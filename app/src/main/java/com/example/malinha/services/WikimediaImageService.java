package com.example.malinha.services;

import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.text.Html;
import android.util.LruCache;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.json.JSONArray;
import org.json.JSONObject;

/** Looks up reusable Commons thumbnails. All network work runs off the UI thread. */
public final class WikimediaImageService {
    public static final String USER_AGENT = "Malinha/1.0 (Android travel planner; com.example.malinha)";
    private static final WikimediaImageService INSTANCE = new WikimediaImageService();
    private static final long HIT_TTL = 24 * 60 * 60 * 1000L;
    private static final long MISS_TTL = 15 * 60 * 1000L;
    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private final Handler main = new Handler(Looper.getMainLooper());
    private final LruCache<String, Cached> cache = new LruCache<>(100);
    private final LruCache<String, String> translatedNames = new LruCache<>(200);
    private final HashMap<String, ArrayList<Callback>> pending = new HashMap<>();

    public interface Callback {
        void onResult(Photo photo);
    }

    public static final class Photo {
        public final String thumbnail, source, title, author, license, licenseUrl;
        Photo(String thumbnail, String source, String title, String author, String license, String licenseUrl) {
            this.thumbnail = thumbnail;
            this.source = source;
            this.title = title;
            this.author = author;
            this.license = license;
            this.licenseUrl = licenseUrl;
        }
    }

    private static final class Cached {
        final Photo photo;
        final long expires;
        Cached(Photo photo, long ttl) {
            this.photo = photo;
            this.expires = android.os.SystemClock.elapsedRealtime() + ttl;
        }
    }

    private WikimediaImageService() {}

    public static WikimediaImageService get() { return INSTANCE; }

    /** Callback runs on the main thread. A null photo means keep the local illustration. */
    public void find(String name, String destination, Callback callback) {
        String subject = name == null ? "" : name.trim();
        String context = destination == null ? "" : destination.trim();
        String key = normalized(subject) + "|" + normalized(context);
        if (normalized(subject).length() < 3) {
            main.post(() -> callback.onResult(null));
            return;
        }
        synchronized (pending) {
            Cached cached = cache.get(key);
            if (cached != null && cached.expires > android.os.SystemClock.elapsedRealtime()) {
                main.post(() -> callback.onResult(cached.photo));
                return;
            }
            ArrayList<Callback> callbacks = pending.get(key);
            if (callbacks != null) {
                callbacks.add(callback);
                return;
            }
            callbacks = new ArrayList<>();
            callbacks.add(callback);
            pending.put(key, callbacks);
        }
        worker.execute(() -> {
            Photo found = null;
            long ttl = 60_000L;
            try {
                found = lookup(subject, context);
                ttl = found == null ? MISS_TTL : HIT_TTL;
            } catch (Exception error) {
                android.util.Log.w("MalinhaPhotos", "Falha na consulta ao Commons: " + error.getClass().getSimpleName());
            }
            Photo result = found;
            ArrayList<Callback> callbacks;
            synchronized (pending) {
                cache.put(key, new Cached(result, ttl));
                callbacks = pending.remove(key);
            }
            main.post(() -> {
                for (Callback listener : callbacks) listener.onResult(result);
            });
        });
    }

    private Photo lookup(String subject, String destination) throws Exception {
        try {
            Photo articlePhoto = lookupArticlePhoto(subject, destination);
            if (articlePhoto != null) return articlePhoto;
        } catch (IOException | org.json.JSONException error) {
            android.util.Log.d("MalinhaPhotos", "PageImages indisponível; buscando no Commons.");
        }
        resolveEnglishName(subject);
        for (String part : destination.split(",")) resolveEnglishName(part.trim());
        String translated = queryTerms(subject, destination, true);
        Photo photo = search(translated, subject, destination);
        String original = queryTerms(subject, destination, false);
        return photo == null && !translated.equals(original)
            ? search(original, subject, destination) : photo;
    }

    private Photo lookupArticlePhoto(String subject, String destination)
            throws IOException, org.json.JSONException {
        if (subject.contains("|")) return null;
        Uri article = Uri.parse("https://pt.wikipedia.org/w/api.php").buildUpon()
            .appendQueryParameter("action", "query")
            .appendQueryParameter("format", "json")
            .appendQueryParameter("formatversion", "2")
            .appendQueryParameter("redirects", "1")
            .appendQueryParameter("titles", subject)
            .appendQueryParameter("prop", "pageimages|pageprops")
            .appendQueryParameter("ppprop", "disambiguation")
            .appendQueryParameter("piprop", "name")
            .appendQueryParameter("pilicense", "free")
            .build();
        JSONArray articles = responsePages(article);
        JSONObject page = articles == null ? null : articles.optJSONObject(0);
        if (page == null || page.optInt("ns", -1) != 0 || page.has("missing") || page.has("pageprops")) return null;
        String filename = page.optString("pageimage").trim();
        if (filename.isEmpty() || filename.contains("|")) return null;

        Uri file = commonsImageQuery().appendQueryParameter("titles", "File:" + filename).build();
        JSONArray files = responsePages(file);
        JSONObject imagePage = files == null ? null : files.optJSONObject(0);
        return imagePage == null ? null : parse(imagePage, subject, destination, true);
    }

    private String queryTerms(String subject, String destination, boolean translated) {
        StringBuilder terms = new StringBuilder();
        terms.append('"').append((translated ? englishName(subject) : subject).replace('"', ' ')).append('"');
        for (String part : destination.split(",")) {
            String place = part.trim();
            if (!place.isEmpty() && !normalized(place).equals(normalized(subject))) {
                terms.append(" \"").append((translated ? englishName(place) : place).replace('"', ' ')).append('"');
            }
        }
        return terms.toString();
    }

    private Photo search(String terms, String subject, String destination) throws Exception {
        Uri endpoint = commonsImageQuery()
            .appendQueryParameter("generator", "search")
            .appendQueryParameter("gsrnamespace", "6")
            .appendQueryParameter("gsrsearch", terms)
            .appendQueryParameter("gsrlimit", "18")
            .build();
        JSONArray pages = responsePages(endpoint);
        Photo best = null;
        int bestIndex = Integer.MAX_VALUE;
        if (pages == null) return null;
        for (int i = 0; i < pages.length(); i++) {
            JSONObject page = pages.optJSONObject(i);
            if (page == null) continue;
            Photo photo = parse(page, subject, destination, false);
            int index = page.optInt("index", i + 1);
            if (photo != null && index < bestIndex) {
                best = photo;
                bestIndex = index;
            }
        }
        return best;
    }

    private static Uri.Builder commonsImageQuery() {
        return Uri.parse("https://commons.wikimedia.org/w/api.php").buildUpon()
            .appendQueryParameter("action", "query")
            .appendQueryParameter("format", "json")
            .appendQueryParameter("formatversion", "2")
            .appendQueryParameter("prop", "imageinfo|categories")
            .appendQueryParameter("cllimit", "25")
            .appendQueryParameter("iiprop", "url|mime|size|extmetadata")
            .appendQueryParameter("iiurlwidth", "960")
            .appendQueryParameter("iiextmetadatafilter", "Artist|Attribution|LicenseShortName|LicenseUrl|ImageDescription");
    }

    private static JSONArray responsePages(Uri endpoint) throws IOException, org.json.JSONException {
        JSONObject response = new JSONObject(read(endpoint));
        if (response.has("error")) throw new IOException("MediaWiki API error");
        JSONObject query = response.optJSONObject("query");
        return query == null ? null : query.optJSONArray("pages");
    }

    private static String read(Uri endpoint) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL(endpoint.toString()).openConnection();
        connection.setConnectTimeout(8_000);
        connection.setReadTimeout(10_000);
        connection.setInstanceFollowRedirects(false);
        connection.setRequestProperty("User-Agent", USER_AGENT);
        connection.setRequestProperty("Accept", "application/json");
        try {
            if (connection.getResponseCode() != 200) throw new IOException("Commons HTTP error");
            StringBuilder body = new StringBuilder();
            try (InputStreamReader reader = new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8)) {
                char[] buffer = new char[4096];
                int count;
                while ((count = reader.read(buffer)) != -1) {
                    body.append(buffer, 0, count);
                    if (body.length() > 2_000_000) throw new IOException("Commons response too large");
                }
            }
            return body.toString();
        } finally {
            connection.disconnect();
        }
    }

    private Photo parse(JSONObject page, String subject, String destination, boolean fromArticle) {
        JSONArray images = page.optJSONArray("imageinfo");
        JSONObject image = images == null ? null : images.optJSONObject(0);
        if (image == null || !"image/jpeg".equals(image.optString("mime"))
                || image.optInt("width") < 480 || image.optInt("height") < 320) return null;
        String title = page.optString("title");
        String titleWords = normalized(title);
        if (titleWords.matches(".*\\b(map|logo|flag|icon|diagram|illustration|poster|screenshot|drawing|painting|collage)\\b.*")) return null;
        JSONObject metadata = image.optJSONObject("extmetadata");
        if (metadata == null) return null;
        String description = value(metadata, "ImageDescription");
        if (!fromArticle && !contains(titleWords, subject) && !contains(normalized(description), subject)) return null;
        StringBuilder evidence = new StringBuilder(title).append(' ').append(description);
        JSONArray categories = page.optJSONArray("categories");
        if (categories != null) for (int i = 0; i < categories.length(); i++) {
            JSONObject category = categories.optJSONObject(i);
            if (category != null) evidence.append(' ').append(category.optString("title"));
        }
        String location = normalized(evidence.toString());
        for (String part : destination.split(",")) {
            if (fromArticle && normalized(part).equals(normalized(subject))) continue;
            if (!normalized(part).isEmpty() && !contains(location, part)) return null;
        }

        String artist = value(metadata, "Artist");
        String attribution = value(metadata, "Attribution");
        String license = value(metadata, "LicenseShortName");
        String licenseUrl = httpsUrl(metadataValue(metadata, "LicenseUrl"));
        String thumbnail = httpsUrl(image.optString("thumburl"));
        String source = httpsUrl(image.optString("descriptionurl"));
        boolean ccZero = trusted(licenseUrl, "creativecommons.org")
            && "/publicdomain/zero/1.0/".equals(Uri.parse(licenseUrl).getPath());
        if (ccZero) license = "CC0 1.0";
        if (artist.isEmpty() || !(ccZero || license.matches("CC BY(?:-SA)? [0-9.]+"))
                || !trusted(licenseUrl, "creativecommons.org")
                || !(trusted(thumbnail, "upload.wikimedia.org") || trusted(thumbnail, "thumb.wikimedia.org"))
                || !trusted(source, "commons.wikimedia.org")) return null;
        String author = attribution.isEmpty() || attribution.equals(artist) ? artist : artist + " · " + attribution;
        return new Photo(thumbnail, source, title.replaceFirst("^File:", ""), author, license, licenseUrl);
    }

    private static String metadataValue(JSONObject metadata, String name) {
        JSONObject field = metadata.optJSONObject(name);
        return field == null ? "" : field.optString("value", "");
    }

    private static String value(JSONObject metadata, String name) {
        return Html.fromHtml(metadataValue(metadata, name), Html.FROM_HTML_MODE_LEGACY).toString().trim();
    }

    private static boolean trusted(String value, String host) {
        Uri uri = Uri.parse(value);
        return "https".equals(uri.getScheme()) && host.equals(uri.getHost()) && uri.getUserInfo() == null;
    }

    private static String httpsUrl(String value) {
        String trimmed = value.trim();
        if (trimmed.startsWith("//")) return "https:" + trimmed;
        return trimmed.replaceFirst("^http://", "https://");
    }

    private boolean contains(String normalizedText, String phrase) {
        String words = " " + normalizedText + " ";
        return words.contains(" " + normalized(phrase) + " ")
            || words.contains(" " + normalized(englishName(phrase)) + " ");
    }

    private String englishName(String name) {
        String translated = translatedNames.get(normalized(name));
        return translated == null ? searchName(name) : translated;
    }

    private void resolveEnglishName(String name) {
        String key = normalized(name);
        if (key.length() < 3 || name.contains("|") || translatedNames.get(key) != null) return;
        String alias = searchName(name);
        if (!normalized(alias).equals(key)) {
            translatedNames.put(key, alias);
            return;
        }
        Uri endpoint = Uri.parse("https://pt.wikipedia.org/w/api.php").buildUpon()
            .appendQueryParameter("action", "query")
            .appendQueryParameter("format", "json")
            .appendQueryParameter("formatversion", "2")
            .appendQueryParameter("redirects", "1")
            .appendQueryParameter("titles", name)
            .appendQueryParameter("prop", "langlinks|pageprops")
            .appendQueryParameter("ppprop", "disambiguation")
            .appendQueryParameter("lllang", "en")
            .build();
        try {
            JSONObject result = new JSONObject(read(endpoint));
            if (result.has("error")) return;
            JSONObject query = result.optJSONObject("query");
            JSONArray pages = query == null ? null : query.optJSONArray("pages");
            JSONObject page = pages == null ? null : pages.optJSONObject(0);
            if (page != null && page.optInt("ns", -1) == 0 && !page.has("missing")
                    && !page.has("pageprops")) {
                JSONArray links = page.optJSONArray("langlinks");
                JSONObject link = links == null ? null : links.optJSONObject(0);
                if (link != null && "en".equals(link.optString("lang"))) {
                    String title = link.optString("title", link.optString("*", "")).trim();
                    if (!title.isEmpty()) alias = title;
                }
            }
            translatedNames.put(key, alias);
        } catch (Exception error) {
            android.util.Log.d("MalinhaPhotos", "Nome em inglês indisponível; usando o nome original.");
        }
    }

    private static String searchName(String value) {
        String name = normalized(value);
        switch (name) {
            case "toquio": return "Tokyo";
            case "pequim": return "Beijing";
            case "seul": return "Seoul";
            case "londres": return "London";
            case "nova york": return "New York";
            case "cidade do cabo": return "Cape Town";
            case "cidade do mexico": return "Mexico City";
            default:
                Locale portuguese = new Locale("pt", "BR");
                for (String code : Locale.getISOCountries()) {
                    Locale country = new Locale("", code);
                    if (name.equals(normalized(country.getDisplayCountry(portuguese)))) {
                        return country.getDisplayCountry(Locale.ENGLISH);
                    }
                }
                return value;
        }
    }

    private static String normalized(String value) {
        return Normalizer.normalize(value, Normalizer.Form.NFD)
            .replaceAll("\\p{M}+", "").toLowerCase(Locale.ROOT)
            .replaceAll("[^\\p{L}\\p{N}]+", " ").trim();
    }
}

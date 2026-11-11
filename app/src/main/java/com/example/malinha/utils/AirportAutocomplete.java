package com.example.malinha.utils;

import android.content.Context;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Filter;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Local OpenFlights suggestions. Android's Filter runs matching and asset I/O off the UI thread. */
public final class AirportAutocomplete {
    private static final Pattern ACCENTS = Pattern.compile("\\p{M}+");
    private static final Pattern DISPLAY_CODE = Pattern.compile(" \\(([A-Z]{3})\\) · ");
    private static final String HUBS = " GRU GIG BSB CGH SDU CNF VCP REC SSA FOR POA ATL DXB LHR HND CDG AMS FRA MAD LIS JFK LAX MIA DOH SIN IST ICN SYD EZE SCL MEX ";
    private static List<Airport> catalog;

    private AirportAutocomplete() {}

    public static void bind(AutoCompleteTextView input) {
        input.setThreshold(2);
        input.setAdapter(new Suggestions(input.getContext()));
        input.setOnItemClickListener((parent, view, position, id) -> input.setError(null));
    }

    public static String searchValue(String text) {
        Matcher code = DISPLAY_CODE.matcher(text);
        return code.find() ? code.group(1) : text.trim();
    }

    private static String normalize(String value) {
        return ACCENTS.matcher(Normalizer.normalize(value, Normalizer.Form.NFD))
                .replaceAll("").toLowerCase(Locale.ROOT).trim();
    }

    private static String cityLabel(String original) {
        switch (normalize(original)) {
            case "sao paulo": return "São Paulo";
            case "rio de janeiro": return "Rio de Janeiro";
            case "brasilia": return "Brasília";
            case "lisbon": return "Lisboa";
            case "london": return "Londres";
            case "new york": return "Nova York";
            case "rome": return "Roma";
            case "tokyo": return "Tóquio";
            case "seoul": return "Seul";
            case "beijing": return "Pequim";
            case "cape town": return "Cidade do Cabo";
            case "mexico city": return "Cidade do México";
            default: return original;
        }
    }

    private static synchronized List<Airport> airports(Context context) throws IOException {
        if (catalog != null) return catalog;
        List<Airport> loaded = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                context.getAssets().open("airports.tsv"), StandardCharsets.UTF_8))) {
            for (String line; (line = reader.readLine()) != null;) {
                String[] columns = line.split("\\t", -1);
                if (columns.length == 7) loaded.add(new Airport(columns));
            }
        }
        catalog = loaded;
        return catalog;
    }

    private static final class Airport {
        final String code, name, city, originalCity, country, label;
        final int priority;

        Airport(String[] columns) {
            code = columns[0].toLowerCase(Locale.ROOT);
            name = normalize(columns[1]);
            originalCity = normalize(columns[2]);
            String displayCity = cityLabel(columns[2].isEmpty() ? columns[1] : columns[2]);
            city = normalize(displayCity);
            boolean brazilian = columns[3].equals("Brazil");
            String countryLabel = brazilian ? "Brasil" : columns[3];
            country = normalize(countryLabel);
            label = displayCity + " (" + columns[0] + ") · " + countryLabel;
            boolean hub = HUBS.contains(" " + columns[0] + " ");
            priority = (brazilian ? 0 : 2) + (hub ? 0 : 1);
        }

        int match(String query) {
            if (code.equals(query)) return 0;
            if (city.equals(query) || originalCity.equals(query)) return 1;
            if (city.startsWith(query) || originalCity.startsWith(query) || code.startsWith(query)) return 2;
            return city.contains(query) || originalCity.contains(query) || name.contains(query)
                    || country.contains(query) ? 3 : 4;
        }
    }

    private static final class Suggestions extends ArrayAdapter<String> {
        Suggestions(Context context) { super(context, android.R.layout.simple_dropdown_item_1line); }

        private final Filter filter = new Filter() {
            @Override protected FilterResults performFiltering(CharSequence constraint) {
                FilterResults result = new FilterResults();
                List<String> labels = new ArrayList<>();
                String query = normalize(constraint == null ? "" : constraint.toString());
                if (query.length() >= 2) {
                    try {
                        List<Airport> matches = new ArrayList<>();
                        for (Airport airport : airports(getContext().getApplicationContext())) {
                            if (airport.match(query) < 4) matches.add(airport);
                        }
                        matches.sort((a, b) -> {
                            int byMatch = Integer.compare(a.match(query), b.match(query));
                            if (byMatch != 0) return byMatch;
                            int byPriority = Integer.compare(a.priority, b.priority);
                            return byPriority != 0 ? byPriority : a.label.compareTo(b.label);
                        });
                        for (int i = 0; i < Math.min(matches.size(), 8); i++) labels.add(matches.get(i).label);
                    } catch (IOException error) {
                        android.util.Log.w("MalinhaAirports", "Catálogo offline indisponível");
                    }
                }
                result.values = labels;
                result.count = labels.size();
                return result;
            }

            @Override protected void publishResults(CharSequence constraint, FilterResults results) {
                setNotifyOnChange(false);
                clear();
                List<?> values = results.values instanceof List<?> ? (List<?>) results.values : Collections.emptyList();
                for (Object value : values) add((String) value);
                notifyDataSetChanged();
            }
        };

        @Override public Filter getFilter() { return filter; }
    }
}

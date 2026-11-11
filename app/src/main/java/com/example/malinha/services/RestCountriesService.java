package com.example.malinha.services;

import com.example.malinha.models.CountryTravelData;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.IOException;
import java.util.List;

/** REST Countries v5 requires authentication; the retired v3.1 is intentionally not used. */
public final class RestCountriesService {
    public CountryTravelData fetch(String code, String token) throws Exception {
        CountryHttp.validateCountry(code);
        if (token == null || token.trim().isEmpty()) throw new IllegalStateException("REST Countries key missing");
        String address = "https://api.restcountries.com/countries/v5/codes.alpha_2/" + code
            + "?response_fields=names.common,codes.alpha_2,languages,currencies,timezones,calling_codes";
        return parse(CountryHttp.get(address, token.trim()), code);
    }
    public static CountryTravelData parse(String json, String code) throws Exception {
        CountryHttp.validateCountry(code);
        JSONObject data = new JSONObject(json).getJSONObject("data");
        // The provider's public demo token returns samples, not a complete country lookup.
        if (data.has("_demo")) throw new IOException("Demo response is not a country lookup");
        JSONArray objects = data.getJSONArray("objects");
        if (objects.length() != 1) throw new IOException("Country not found");
        JSONObject object = objects.getJSONObject(0);
        if (!code.equals(object.getJSONObject("codes").getString("alpha_2"))) throw new IOException("Unexpected country");
        CountryTravelData result = new CountryTravelData();
        result.name = object.getJSONObject("names").optString("common", "");
        JSONArray languages = object.optJSONArray("languages");
        if (languages != null) for (int i = 0; i < languages.length(); i++) {
            JSONObject language = languages.optJSONObject(i);
            if (language != null) add(result.languages, language.optString("name", ""));
        }
        JSONArray currencies = object.optJSONArray("currencies");
        if (currencies != null) for (int i = 0; i < currencies.length(); i++) {
            JSONObject currency = currencies.optJSONObject(i);
            if (currency == null) continue;
            String currencyCode = currency.optString("code", "");
            String name = currency.optString("name", "");
            add(result.currencies, currencyCode + (name.isEmpty() ? "" : " — " + name));
        }
        strings(object.optJSONArray("timezones"), result.timezones);
        strings(object.optJSONArray("calling_codes"), result.callingCodes);
        return result;
    }
    private static void strings(JSONArray array, List<String> target) {
        if (array == null) return;
        for (int i = 0; i < array.length(); i++) if (array.opt(i) instanceof String) add(target, array.optString(i));
    }
    private static void add(List<String> target, String text) {
        if (!text.trim().isEmpty() && !target.contains(text.trim())) target.add(text.trim());
    }
}

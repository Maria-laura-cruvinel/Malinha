package com.example.malinha.services;

import com.example.malinha.models.CountryTravelData.Holiday;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Locale;
import java.util.TimeZone;

public final class NagerDateService {
    public ArrayList<Holiday> fetch(String country, int year) throws Exception {
        CountryHttp.validateCountry(country);
        if (year < 1900 || year > 2100) throw new IllegalArgumentException("Invalid year");
        return parse(CountryHttp.get("https://nagerholidays.com/api/v4/Holidays/" + country + "/" + year, null), country, year);
    }
    public static ArrayList<Holiday> parse(String json, String country, int year) throws Exception {
        JSONArray items = new JSONArray(json);
        ArrayList<Holiday> result = new ArrayList<>();
        SimpleDateFormat dates = new SimpleDateFormat("yyyy-MM-dd", Locale.ROOT);
        dates.setTimeZone(TimeZone.getTimeZone("UTC")); dates.setLenient(false);
        for (int i = 0; i < items.length(); i++) {
            JSONObject item = items.getJSONObject(i);
            Holiday holiday = new Holiday(); holiday.date = item.getString("date");
            if (!country.equals(item.getString("countryCode")) || !holiday.date.matches(year + "-\\d{2}-\\d{2}")
                    || !holiday.date.equals(dates.format(dates.parse(holiday.date)))) throw new IOException("Unexpected holiday date or country");
            holiday.name = item.optString("localName", "");
            if (holiday.name.isEmpty()) holiday.name = item.getString("name");
            holiday.national = item.getBoolean("nationalHoliday");
            copy(item.optJSONArray("subdivisionCodes"), holiday.subdivisions);
            copy(item.optJSONArray("holidayTypes"), holiday.types);
            result.add(holiday);
        }
        Collections.sort(result, (a, b) -> a.date.compareTo(b.date));
        return result;
    }
    private static void copy(JSONArray values, ArrayList<String> output) throws Exception {
        if (values != null) for (int i = 0; i < values.length(); i++) output.add(values.getString(i));
    }
}

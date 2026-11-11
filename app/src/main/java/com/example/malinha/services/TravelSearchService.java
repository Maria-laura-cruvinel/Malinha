package com.example.malinha.services;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.widget.Toast;
import com.example.malinha.R;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/** External search boundary; can later be replaced by hotel/flight API clients. */
public final class TravelSearchService {
    private TravelSearchService() {}
    public static String date(long value) {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.ROOT).format(new Date(value));
    }
    public static Uri hotels(String destination, long start, long end, int guests) {
        return Uri.parse("https://www.booking.com/searchresults.pt-br.html").buildUpon()
                .appendQueryParameter("ss", destination).appendQueryParameter("checkin", date(start))
                .appendQueryParameter("checkout", date(end)).appendQueryParameter("group_adults", Integer.toString(guests))
                .appendQueryParameter("no_rooms", "1").appendQueryParameter("group_children", "0").build();
    }
    public static Uri flights(String origin, String destination, long start, long end, int passengers, String cabin) {
        String query = "Voos de " + origin + " para " + destination + ", ida " + date(start)
                + ", volta " + date(end) + ", " + passengers + " adultos, classe " + cabin;
        return Uri.parse("https://www.google.com/travel/flights").buildUpon().appendQueryParameter("q", query).appendQueryParameter("hl", "pt-BR").build();
    }
    public static Uri maps(String place) {
        return Uri.parse("https://www.google.com/maps/search/").buildUpon()
                .appendQueryParameter("api", "1").appendQueryParameter("query", place).build();
    }
    public static void open(Context context, Uri uri) {
        try { context.startActivity(new Intent(Intent.ACTION_VIEW, uri)); }
        catch (ActivityNotFoundException | SecurityException error) {
            Toast.makeText(context, R.string.no_browser, Toast.LENGTH_LONG).show();
        }
    }
}

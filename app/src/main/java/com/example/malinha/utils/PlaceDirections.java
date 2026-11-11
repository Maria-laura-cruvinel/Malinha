package com.example.malinha.utils;

import android.app.Activity;
import android.net.Uri;
import androidx.appcompat.app.AlertDialog;
import com.example.malinha.R;
import com.example.malinha.services.TravelSearchService;

/** Google Maps handles origin selection, current traffic and route calculation. */
public final class PlaceDirections {
    private PlaceDirections() {}
    public static void show(Activity activity, String destination, String placeId) {
        if (activity.isFinishing() || activity.isDestroyed() || destination == null || destination.trim().isEmpty()) return;
        final String[] modes = {"walking", "driving", "transit"};
        new AlertDialog.Builder(activity).setTitle(R.string.local_mode)
            .setItems(R.array.local_modes, (dialog, which) -> {
                Uri.Builder uri = Uri.parse("https://www.google.com/maps/dir/").buildUpon()
                    .appendQueryParameter("api", "1").appendQueryParameter("destination", destination)
                    .appendQueryParameter("travelmode", modes[which]);
                if (placeId != null && !placeId.isEmpty()) uri.appendQueryParameter("destination_place_id", placeId);
                TravelSearchService.open(activity, uri.build());
            }).setNegativeButton(R.string.cancel_action, null).show();
    }
}

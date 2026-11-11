package com.example.malinha.services;

import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Bundle;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.net.PlacesClient;
import java.util.Locale;

/** Shared initialization for city autocomplete and lodging searches. */
public final class PlacesClientProvider {
    private PlacesClientProvider() {}

    public static synchronized PlacesClient get(Context context) throws PackageManager.NameNotFoundException {
        Context app = context.getApplicationContext();
        if (!Places.isInitialized()) {
            Bundle metadata = app.getPackageManager().getApplicationInfo(app.getPackageName(), PackageManager.GET_META_DATA).metaData;
            String key = metadata == null ? "" : metadata.getString("malinha.PLACES_API_KEY", "");
            if (key.trim().isEmpty() && metadata != null) key = metadata.getString("com.google.android.geo.API_KEY", "");
            if (key.trim().isEmpty()) throw new IllegalStateException("Places key missing");
            Places.initializeWithNewPlacesApiEnabled(app, key, new Locale("pt", "BR"));
        }
        return Places.createClient(app);
    }
}

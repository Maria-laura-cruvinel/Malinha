package com.example.malinha.services;

import android.content.Context;
import android.content.pm.PackageManager;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.net.PlacesClient;
import com.google.android.libraries.places.api.net.SearchByTextRequest;
import com.google.android.libraries.places.api.net.SearchByTextResponse;
import com.google.android.gms.tasks.Task;
import java.util.Arrays;

/** SDK requests are asynchronous. Results are never persisted in the itinerary database. */
public final class HotelPlacesService {
    private final PlacesClient client;
    public HotelPlacesService(Context context) throws PackageManager.NameNotFoundException {
        client = PlacesClientProvider.get(context);
    }
    public Task<SearchByTextResponse> search(String destination) {
        return client.searchByText(SearchByTextRequest.builder("hotéis e pousadas em " + destination,
            Arrays.asList(Place.Field.ID, Place.Field.DISPLAY_NAME, Place.Field.FORMATTED_ADDRESS,
                Place.Field.RATING, Place.Field.USER_RATING_COUNT, Place.Field.PRICE_LEVEL))
            .setIncludedType("lodging").setStrictTypeFiltering(true).setMaxResultCount(20).build());
    }
}

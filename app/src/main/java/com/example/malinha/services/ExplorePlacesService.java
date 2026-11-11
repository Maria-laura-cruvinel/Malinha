package com.example.malinha.services;

import android.content.Context;
import android.content.pm.PackageManager;
import com.google.android.gms.tasks.Task;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.net.FetchPlaceRequest;
import com.google.android.libraries.places.api.net.FetchPlaceResponse;
import com.google.android.libraries.places.api.net.PlacesClient;
import com.google.android.libraries.places.api.net.SearchByTextRequest;
import com.google.android.libraries.places.api.net.SearchByTextResponse;
import java.util.Arrays;

/** User-triggered requests; Places content stays in memory only. */
public final class ExplorePlacesService {
    private final PlacesClient client;
    public ExplorePlacesService(Context context) throws PackageManager.NameNotFoundException {
        client = PlacesClientProvider.get(context);
    }
    public Task<SearchByTextResponse> search(String destination, int category) {
        String[] queries = {"pontos turísticos", "restaurantes", "museus", "parques", "cafés"};
        String[] types = {"tourist_attraction", "restaurant", "museum", "park", "cafe"};
        int index = category >= 0 && category < queries.length ? category : 0;
        return client.searchByText(SearchByTextRequest.builder(
            queries[index] + " em " + destination,
            Arrays.asList(Place.Field.ID, Place.Field.DISPLAY_NAME, Place.Field.FORMATTED_ADDRESS,
                Place.Field.LOCATION, Place.Field.RATING, Place.Field.USER_RATING_COUNT))
            .setIncludedType(types[index])
            .setStrictTypeFiltering(index != 0).build());
    }
    public Task<FetchPlaceResponse> hours(String id) {
        return client.fetchPlace(FetchPlaceRequest.builder(id,
            Arrays.asList(Place.Field.DISPLAY_NAME, Place.Field.CURRENT_OPENING_HOURS,
                Place.Field.OPENING_HOURS)).build());
    }
}

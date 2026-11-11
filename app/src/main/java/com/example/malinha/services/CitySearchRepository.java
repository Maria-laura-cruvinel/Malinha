package com.example.malinha.services;

import android.content.Context;
import android.content.pm.PackageManager;
import com.example.malinha.models.SelectedCity;
import com.google.android.libraries.places.api.model.AutocompletePrediction;
import com.google.android.libraries.places.api.model.AutocompleteSessionToken;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.model.PlaceTypes;
import com.google.android.libraries.places.api.net.FetchPlaceRequest;
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest;
import com.google.android.libraries.places.api.net.PlacesClient;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public final class CitySearchRepository {
    public interface Callback<T> {
        void onSuccess(T value);
        void onError(Exception error);
    }
    private final PlacesClient client;
    private AutocompleteSessionToken token = AutocompleteSessionToken.newInstance();

    public CitySearchRepository(Context context) throws PackageManager.NameNotFoundException {
        client = PlacesClientProvider.get(context);
    }
    public void search(String text, Callback<List<AutocompletePrediction>> callback) {
        if (text.trim().length() < 3) { callback.onSuccess(Collections.emptyList()); return; }
        FindAutocompletePredictionsRequest request = FindAutocompletePredictionsRequest.builder()
            .setQuery(text.trim()).setTypesFilter(Collections.singletonList(PlaceTypes.CITIES))
            .setSessionToken(token).build();
        client.findAutocompletePredictions(request)
            .addOnSuccessListener(response -> callback.onSuccess(response.getAutocompletePredictions()))
            .addOnFailureListener(callback::onError);
    }
    public void select(String placeId, Callback<SelectedCity> callback) {
        AutocompleteSessionToken selectionToken = token;
        FetchPlaceRequest request = FetchPlaceRequest.builder(placeId,
            Arrays.asList(Place.Field.ID, Place.Field.DISPLAY_NAME, Place.Field.LOCATION))
            .setSessionToken(selectionToken).build();
        client.fetchPlace(request).addOnSuccessListener(response -> {
            if (token == selectionToken) token = AutocompleteSessionToken.newInstance();
            Place place = response.getPlace();
            if (place.getId() == null || place.getDisplayName() == null || place.getDisplayName().trim().isEmpty() || place.getLocation() == null) {
                callback.onError(new IllegalStateException("City details incomplete")); return;
            }
            callback.onSuccess(new SelectedCity(place.getId(), place.getDisplayName(),
                place.getLocation().latitude, place.getLocation().longitude));
        }).addOnFailureListener(error -> {
            if (token == selectionToken) token = AutocompleteSessionToken.newInstance();
            callback.onError(error);
        });
    }
    public void resetSession() { token = AutocompleteSessionToken.newInstance(); }
}

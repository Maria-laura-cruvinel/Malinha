package com.example.malinha.utils;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.malinha.R;
import com.example.malinha.adapters.CitySuggestionAdapter;
import com.example.malinha.models.SelectedCity;
import com.example.malinha.services.CitySearchRepository;
import com.google.android.gms.common.api.ApiException;
import com.google.android.libraries.places.api.model.AutocompletePrediction;
import java.util.Collections;
import java.util.List;

/** View-bound debounce and lifecycle handling; networking stays in the repository. */
public final class CityAutocompleteController {
    private final Activity activity;
    private final EditText input;
    private final View container;
    private final RecyclerView list;
    private final TextView status;
    private final CitySuggestionAdapter adapter;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private CitySearchRepository repository;
    private Runnable pending;
    private int generation;
    private boolean suppress, closed, selecting;
    private SelectedCity selected;
    private final TextWatcher watcher = new TextWatcher() {
        @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
        @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
            if (suppress || closed) return;
            if (selecting && repository != null) repository.resetSession();
            selected = null; selecting = false;
            cancelPending(); final int request = ++generation;
            adapter.replace(Collections.emptyList()); container.setVisibility(View.GONE);
            String query = s.toString().trim();
            if (query.length() < 3) {
                if (query.isEmpty() && repository != null) repository.resetSession();
                return;
            }
            pending = () -> { pending = null; search(query, request); };
            handler.postDelayed(pending, 300);
        }
        @Override public void afterTextChanged(Editable s) {}
    };

    public CityAutocompleteController(Activity activity, EditText input) {
        this(activity, activity.getWindow().getDecorView(), input);
    }

    public CityAutocompleteController(Activity activity, View scope, EditText input) {
        this.activity = activity; this.input = input;
        container = scope.findViewById(R.id.city_suggestions_container);
        list = scope.findViewById(R.id.city_suggestions);
        status = scope.findViewById(R.id.city_search_status);
        adapter = new CitySuggestionAdapter(this::select);
        list.setLayoutManager(new LinearLayoutManager(activity)); list.setAdapter(adapter);
        input.addTextChangedListener(watcher);
    }
    private boolean active(int request) {
        return !closed && request == generation && !activity.isFinishing() && !activity.isDestroyed();
    }
    private CitySearchRepository repository() throws android.content.pm.PackageManager.NameNotFoundException {
        if (repository == null) repository = new CitySearchRepository(activity);
        return repository;
    }
    private void search(String query, int request) {
        if (!active(request)) return;
        showStatus(R.string.city_loading);
        try {
            repository().search(query, new CitySearchRepository.Callback<List<AutocompletePrediction>>() {
                @Override public void onSuccess(List<AutocompletePrediction> predictions) {
                    if (!active(request)) return;
                    adapter.replace(predictions);
                    status.setText(R.string.city_empty);
                    status.setVisibility(predictions.isEmpty() ? View.VISIBLE : View.GONE);
                    list.setVisibility(predictions.isEmpty() ? View.GONE : View.VISIBLE);
                }
                @Override public void onError(Exception error) { fail(request, error); }
            });
        } catch (android.content.pm.PackageManager.NameNotFoundException | IllegalStateException error) { fail(request, error); }
    }
    private void select(AutocompletePrediction prediction) {
        if (closed || selecting) return;
        selecting = true; cancelPending(); final int request = ++generation;
        showStatus(R.string.city_selecting);
        try {
            repository().select(prediction.getPlaceId(), new CitySearchRepository.Callback<SelectedCity>() {
                @Override public void onSuccess(SelectedCity city) {
                    if (!active(request)) return;
                    selecting = false; selected = city; suppress = true;
                    try { input.setText(city.name); input.setSelection(input.length()); }
                    finally { suppress = false; }
                    input.setError(null); adapter.replace(Collections.emptyList()); container.setVisibility(View.GONE);
                }
                @Override public void onError(Exception error) { fail(request, error); }
            });
        } catch (android.content.pm.PackageManager.NameNotFoundException | IllegalStateException error) { fail(request, error); }
    }
    private void showStatus(int text) {
        container.setVisibility(View.VISIBLE); status.setVisibility(View.VISIBLE); status.setText(text); list.setVisibility(View.GONE);
    }
    private void fail(int request, Exception error) {
        if (!active(request)) return;
        selecting = false; adapter.replace(Collections.emptyList()); showStatus(R.string.city_error);
        // Status and class identify the failure without logging server text or credentials.
        Log.e("CityAutocomplete", "Places failure: " + error.getClass().getSimpleName()
            + (error instanceof ApiException ? " code=" + ((ApiException)error).getStatusCode() : ""));
        Toast.makeText(activity, R.string.city_error, Toast.LENGTH_SHORT).show();
    }
    public SelectedCity getSelectedCity() { return selected; }
    public void save(Bundle state) {
        if (selected == null) return;
        state.putString("city_id", selected.id); state.putString("city_name", selected.name);
        state.putDouble("city_latitude", selected.latitude); state.putDouble("city_longitude", selected.longitude);
    }
    public void restore(Bundle state) {
        cancelPending(); ++generation; selecting = false; container.setVisibility(View.GONE);
        String id = state.getString("city_id"), name = state.getString("city_name");
        if (id != null && name != null && name.equals(input.getText().toString())
                && state.containsKey("city_latitude") && state.containsKey("city_longitude")) {
            selected = new SelectedCity(id, name, state.getDouble("city_latitude"), state.getDouble("city_longitude"));
        }
    }
    public void close() {
        closed = true; ++generation; cancelPending(); input.removeTextChangedListener(watcher);
        list.setAdapter(null); selected = null;
    }
    private void cancelPending() { if (pending != null) { handler.removeCallbacks(pending); pending = null; } }
}

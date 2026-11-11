package com.example.malinha.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.Html;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.malinha.R;
import com.example.malinha.adapters.ExplorePlaceAdapter;
import com.example.malinha.services.ExplorePlacesService;
import com.example.malinha.services.TravelSearchService;
import com.example.malinha.utils.CityAutocompleteController;
import com.example.malinha.utils.PlaceDirections;
import com.google.android.gms.common.api.ApiException;
import com.google.android.libraries.places.api.model.OpeningHours;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.net.Pagination;
import com.google.android.material.appbar.MaterialToolbar;
import java.util.Collections;

public final class ExplorePlacesActivity extends BaseActivity implements ExplorePlaceAdapter.Listener {
    private ExplorePlacesService service;
    private CityAutocompleteController cities;
    private EditText destination;
    private Spinner category;
    private Button search;
    private Pagination pagination;
    private boolean loadingMore;
    private TextView status;
    private ExplorePlaceAdapter adapter;
    private AlertDialog hoursDialog;
    private int generation, hoursGeneration;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state); setContentView(R.layout.activity_explore_places); applyInsets();
        MaterialToolbar toolbar = findViewById(R.id.planning_toolbar);
        toolbar.setTitle(R.string.local_explore);
        toolbar.setNavigationIcon(androidx.appcompat.R.drawable.abc_ic_ab_back_material);
        toolbar.setNavigationContentDescription(R.string.back); toolbar.setNavigationOnClickListener(v -> finish());
        destination = findViewById(R.id.local_destination); category = findViewById(R.id.local_category);
        search = findViewById(R.id.local_search); status = findViewById(R.id.local_status);
        if (state == null) destination.setText(getIntent().getStringExtra("destination"));
        cities = new CityAutocompleteController(this, destination);
        adapter = new ExplorePlaceAdapter(this);
        RecyclerView list = findViewById(R.id.local_results);
        list.setLayoutManager(new LinearLayoutManager(this)); list.setAdapter(adapter);
        search.setOnClickListener(v -> search());
        findViewById(R.id.local_first).setOnClickListener(v -> scrollToResults());
        destination.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { clearResults(); }
            @Override public void afterTextChanged(Editable s) {}
        });
        category.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(android.widget.AdapterView<?> parent, android.view.View view, int position, long id) { clearResults(); }
            @Override public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });
    }
    @Override public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.explore_places_actions, menu);
        return true;
    }
    @Override public boolean onPrepareOptionsMenu(Menu menu) {
        menu.findItem(R.id.local_more).setVisible(pagination != null && pagination.hasNextPage())
            .setEnabled(!loadingMore);
        menu.findItem(R.id.local_edit_search).setVisible(adapter != null && adapter.getItemCount() > 0);
        return super.onPrepareOptionsMenu(menu);
    }
    @Override public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.local_more) {
            loadMore();
            return true;
        }
        if (item.getItemId() == R.id.local_edit_search) {
            clearResults();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    private void clearResults() {
        generation++; hoursGeneration++; adapter.replace(Collections.emptyList());
        pagination = null; loadingMore = false;
        status.setText(""); search.setEnabled(true);
        findViewById(R.id.local_first).setVisibility(View.GONE);
        showResults(false);
        if (hoursDialog != null) { hoursDialog.dismiss(); hoursDialog = null; }
    }
    private ExplorePlacesService service() throws android.content.pm.PackageManager.NameNotFoundException {
        if (service == null) service = new ExplorePlacesService(this);
        return service;
    }
    private boolean active(int request) { return request == generation && !isDestroyed() && !isFinishing(); }
    private void search() {
        String city = destination.getText().toString().trim();
        if (city.isEmpty()) { destination.setError(getString(R.string.error_destination)); return; }
        clearResults(); final int request = generation;
        status.setText(R.string.local_loading); search.setEnabled(false);
        try {
            service().search(city, category.getSelectedItemPosition()).addOnSuccessListener(response -> {
                if (!active(request)) return;
                search.setEnabled(true); adapter.replace(response.getPlaces());
                pagination = response.getPagination(); showPageState();
                if (adapter.getItemCount() > 0) scrollToResults();
            }).addOnFailureListener(error -> { if (active(request)) { search.setEnabled(true); status.setText(R.string.local_error); error(error); } });
        } catch (android.content.pm.PackageManager.NameNotFoundException | IllegalStateException error) {
            search.setEnabled(true); status.setText(R.string.local_error); error(error);
        }
    }
    private void showPageState() {
        showResults(adapter.getItemCount() > 0);
        findViewById(R.id.local_first).setVisibility(adapter.getItemCount() > 0 ? View.VISIBLE : View.GONE);
        boolean hasMore = pagination != null && pagination.hasNextPage();
        String message = adapter.getItemCount() == 0 ? getString(R.string.local_empty) : getString(R.string.local_count, adapter.getItemCount());
        if (!hasMore && adapter.getItemCount() > 0) message += "\n" + getString(R.string.local_results_end);
        status.setText(message);
    }
    private void scrollToResults() {
        RecyclerView list = findViewById(R.id.local_results);
        list.scrollToPosition(0);
    }
    private void showResults(boolean visible) {
        findViewById(R.id.local_scroll).setVisibility(visible ? View.GONE : View.VISIBLE);
        findViewById(R.id.local_results).setVisibility(visible ? View.VISIBLE : View.GONE);
        invalidateOptionsMenu();
    }
    private void loadMore() {
        if (loadingMore || pagination == null || !pagination.hasNextPage()) return;
        final int request = generation;
        final Pagination page = pagination;
        loadingMore = true;
        invalidateOptionsMenu();
        status.setText(R.string.local_more_loading);
        try {
            page.setPageSize(20);
            page.fetchNextPage().addOnSuccessListener(response -> {
                if (!active(request)) return;
                int firstNew = adapter.getItemCount();
                loadingMore = false; adapter.append(response.getPlaces());
                pagination = response.getPagination(); showPageState();
                if (adapter.getItemCount() > firstNew) {
                    RecyclerView list = findViewById(R.id.local_results);
                    ((LinearLayoutManager) list.getLayoutManager()).scrollToPositionWithOffset(firstNew, 0);
                }
            }).addOnFailureListener(error -> { if (active(request)) moreFailed(error); });
        } catch (IllegalStateException error) { if (active(request)) moreFailed(error); }
    }
    private void moreFailed(Exception error) {
        loadingMore = false; showPageState(); status.append("\n" + getString(R.string.local_more_error));
        Log.e("ExplorePlaces", "Pagination failure: " + error.getClass().getSimpleName());
        Toast.makeText(this, R.string.local_more_error, Toast.LENGTH_SHORT).show();
    }
    private String label(Place place) { return place.getDisplayName() == null ? getString(R.string.local_place) : place.getDisplayName(); }
    private String query(Place place) {
        if (place.getLocation() != null) return place.getLocation().latitude + "," + place.getLocation().longitude;
        return label(place) + " " + (place.getFormattedAddress() == null ? "" : place.getFormattedAddress());
    }
    @Override public void onMap(Place place) {
        if (place.getLocation() == null) {
            android.net.Uri.Builder uri = TravelSearchService.maps(query(place)).buildUpon();
            if (place.getId() != null) uri.appendQueryParameter("query_place_id", place.getId());
            TravelSearchService.open(this, uri.build()); return;
        }
        startActivity(new Intent(this, MapActivity.class).putExtra("query", label(place))
            .putExtra("place_id", place.getId()).putExtra("latitude", place.getLocation().latitude)
            .putExtra("longitude", place.getLocation().longitude).putExtra("address", place.getFormattedAddress()));
    }
    @Override public void onDirections(Place place) { PlaceDirections.show(this, query(place), place.getId()); }
    @Override public void onHours(Place place) {
        if (place.getId() == null) return;
        if (hoursDialog != null) hoursDialog.dismiss();
        final int request = ++hoursGeneration;
        final AlertDialog dialog = new AlertDialog.Builder(this).setTitle(label(place))
            .setMessage(R.string.local_hours_loading).setPositiveButton(R.string.close_dialog, null).create();
        hoursDialog = dialog;
        dialog.setOnDismissListener(ignored -> { if (hoursDialog == dialog) { hoursGeneration++; hoursDialog = null; } });
        dialog.show();
        try {
            service().hours(place.getId()).addOnSuccessListener(response -> {
                if (!activeHours(request, dialog)) return;
                Place details = response.getPlace();
                OpeningHours hours = details.getCurrentOpeningHours();
                boolean current = hours != null && !hours.getWeekdayText().isEmpty();
                if (!current) hours = details.getOpeningHours();
                String text = hours == null || hours.getWeekdayText().isEmpty() ? getString(R.string.local_hours_missing)
                    : getString(current ? R.string.local_hours_current : R.string.local_hours_regular) + "\n\n" + TextUtils.join("\n", hours.getWeekdayText());
                String html = Html.escapeHtml(text).replace("\n", "<br>") + "<br><br>Google Maps";
                if (details.getAttributions() != null) html += "<br>" + TextUtils.join("<br>", details.getAttributions());
                dialog.setMessage(Html.fromHtml(html, Html.FROM_HTML_MODE_LEGACY));
                TextView message = dialog.findViewById(android.R.id.message);
                if (message != null) message.setMovementMethod(LinkMovementMethod.getInstance());
            }).addOnFailureListener(error -> {
                if (activeHours(request, dialog)) { dialog.setMessage(getString(R.string.local_error)); error(error); }
            });
        } catch (android.content.pm.PackageManager.NameNotFoundException | IllegalStateException error) {
            dialog.setMessage(getString(R.string.local_error)); error(error);
        }
    }
    private boolean activeHours(int request, AlertDialog dialog) {
        return request == hoursGeneration && !isDestroyed() && !isFinishing() && dialog.isShowing();
    }
    private void error(Exception error) {
        Log.e("ExplorePlaces", "Places failure: " + error.getClass().getSimpleName()
            + (error instanceof ApiException ? " code=" + ((ApiException)error).getStatusCode() : ""));
        Toast.makeText(this, R.string.local_error, Toast.LENGTH_LONG).show();
    }
    @Override protected void onSaveInstanceState(Bundle state) { cities.save(state); super.onSaveInstanceState(state); }
    @Override protected void onRestoreInstanceState(Bundle state) { super.onRestoreInstanceState(state); cities.restore(state); }
    @Override protected void onDestroy() {
        generation++; hoursGeneration++;
        if (hoursDialog != null) hoursDialog.dismiss();
        if (cities != null) cities.close(); super.onDestroy();
    }
}

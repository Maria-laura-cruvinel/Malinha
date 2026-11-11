package com.example.malinha.activities;

import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.TextView;
import androidx.appcompat.app.AlertDialog;
import com.example.malinha.R;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.appbar.MaterialToolbar;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Maps SDK view with a system geocoder; no device-location permission is needed. */
public class MapActivity extends BaseActivity {
    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private final Handler handler = new Handler(Looper.getMainLooper());
    private GoogleMap map;
    private String query;
    private int request;
    private boolean hasPoint;
    private double latitude, longitude;
    private String address;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state); setContentView(R.layout.activity_map); applyInsets();
        query = getIntent().getStringExtra("query");
        if (query == null || query.trim().isEmpty()) { finish(); return; }
        MaterialToolbar toolbar = findViewById(R.id.planning_toolbar);
        toolbar.setTitle(R.string.internal_map_title);
        toolbar.setNavigationIcon(androidx.appcompat.R.drawable.abc_ic_ab_back_material);
        toolbar.setNavigationContentDescription(R.string.back);
        toolbar.setNavigationOnClickListener(v -> finish());
        ((TextView)findViewById(R.id.map_query)).setText(query);
        findViewById(R.id.map_directions).setOnClickListener(v -> com.example.malinha.utils.PlaceDirections.show(this,
            hasPoint ? latitude + "," + longitude : query, getIntent().getStringExtra("place_id")));
        findViewById(R.id.map_retry).setOnClickListener(v -> locate());
        if (state != null) {
            hasPoint = state.getBoolean("point"); latitude = state.getDouble("lat");
            longitude = state.getDouble("lng"); address = state.getString("address");
        } else if (getIntent().hasExtra("latitude") && getIntent().hasExtra("longitude")) {
            latitude = getIntent().getDoubleExtra("latitude", Double.NaN);
            longitude = getIntent().getDoubleExtra("longitude", Double.NaN);
            hasPoint = !Double.isNaN(latitude) && !Double.isNaN(longitude)
                && Math.abs(latitude) <= 90 && Math.abs(longitude) <= 180;
            address = getIntent().getStringExtra("address");
            if (address == null) address = query;
        }
        if (!hasApiKey()) { unavailable(R.string.map_not_configured); return; }
        if (GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(this) != ConnectionResult.SUCCESS) {
            unavailable(R.string.map_play_services); return;
        }
        SupportMapFragment fragment = (SupportMapFragment)getSupportFragmentManager().findFragmentById(R.id.map_container);
        if (fragment == null) {
            fragment = SupportMapFragment.newInstance();
            getSupportFragmentManager().beginTransaction().replace(R.id.map_container, fragment).commitNow();
        }
        fragment.getMapAsync(googleMap -> {
            if (isDestroyed() || isFinishing()) return;
            map = googleMap; map.getUiSettings().setZoomControlsEnabled(true);
            map.getUiSettings().setMapToolbarEnabled(true);
            if (hasPoint) showPoint(); else locate();
        });
    }
    private boolean hasApiKey() {
        try {
            ApplicationInfo info = getPackageManager().getApplicationInfo(getPackageName(), PackageManager.GET_META_DATA);
            String key = info.metaData == null ? null : info.metaData.getString("com.google.android.geo.API_KEY");
            return key != null && !key.trim().isEmpty() && !key.equals("YOUR_API_KEY");
        } catch (PackageManager.NameNotFoundException error) { return false; }
    }
    private void unavailable(int message) {
        status(message); findViewById(R.id.map_container).setVisibility(View.GONE);
        findViewById(R.id.map_retry).setVisibility(View.GONE);
    }
    private void status(int resource) { ((TextView)findViewById(R.id.map_status)).setText(resource); }
    private void locate() {
        if (map == null) return;
        if (getIntent().hasExtra("place_id") && hasPoint) { showPoint(); return; }
        if (!Geocoder.isPresent()) { status(R.string.map_not_found); return; }
        final int token = ++request;
        status(R.string.map_searching); findViewById(R.id.map_retry).setEnabled(false);
        // The platform geocoder has no configurable timeout; stop waiting in the UI after 15 seconds.
        Runnable timeout = () -> {
            if (token != request || isDestroyed()) return;
            request++; status(R.string.map_not_found); findViewById(R.id.map_retry).setEnabled(true);
        };
        handler.postDelayed(timeout, 15000);
        worker.execute(() -> {
            try {
                List<Address> matches = new Geocoder(getApplicationContext(), new Locale("pt", "BR")).getFromLocationName(query, 5);
                handler.post(() -> {
                    if (token != request || isDestroyed() || isFinishing()) return;
                    handler.removeCallbacks(timeout); findViewById(R.id.map_retry).setEnabled(true);
                    if (matches == null || matches.isEmpty()) { status(R.string.map_not_found); return; }
                    String[] labels = new String[matches.size()];
                    for (int i = 0; i < matches.size(); i++) {
                        String line = matches.get(i).getAddressLine(0);
                        labels[i] = line == null ? matches.get(i).getLatitude() + ", " + matches.get(i).getLongitude() : line;
                    }
                    status(R.string.map_choose_result);
                    new AlertDialog.Builder(this).setTitle(R.string.map_choose_result).setItems(labels, (dialog, which) -> {
                        Address selected = matches.get(which); latitude = selected.getLatitude(); longitude = selected.getLongitude();
                        address = labels[which]; hasPoint = true; showPoint();
                    }).setNegativeButton(R.string.cancel_action, null).show();
                });
            } catch (Exception error) {
                handler.post(() -> {
                    if (token != request || isDestroyed() || isFinishing()) return;
                    handler.removeCallbacks(timeout); status(R.string.map_not_found); findViewById(R.id.map_retry).setEnabled(true);
                });
            }
        });
    }
    private void showPoint() {
        map.clear(); LatLng point = new LatLng(latitude, longitude);
        map.addMarker(new MarkerOptions().position(point).title(query).snippet(address));
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(point, 15));
        ((TextView)findViewById(R.id.map_status)).setText(getString(R.string.map_result, address));
    }
    @Override protected void onSaveInstanceState(Bundle state) {
        state.putBoolean("point", hasPoint); state.putDouble("lat", latitude); state.putDouble("lng", longitude); state.putString("address", address);
        super.onSaveInstanceState(state);
    }
    @Override protected void onDestroy() { request++; handler.removeCallbacksAndMessages(null); worker.shutdownNow(); super.onDestroy(); }
}

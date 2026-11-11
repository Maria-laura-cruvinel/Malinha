package com.example.malinha.activities;

import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.malinha.R;
import com.example.malinha.adapters.HotelAdapter;
import com.example.malinha.services.HotelPlacesService;
import com.google.android.gms.common.api.ApiException;
import com.google.android.material.appbar.MaterialToolbar;
import java.util.Collections;

public final class HotelResultsActivity extends BaseActivity {
    private final HotelAdapter adapter = new HotelAdapter();
    private TextView status;
    private Button retry;
    private String destination;
    private int generation;
    @Override protected void onCreate(Bundle state) {
        super.onCreate(state); setContentView(R.layout.activity_hotel_results); applyInsets();
        destination = getIntent().getStringExtra("destination");
        if (destination == null || destination.trim().isEmpty()) { finish(); return; }
        MaterialToolbar toolbar = findViewById(R.id.planning_toolbar);
        toolbar.setTitle(destination);
        toolbar.setNavigationIcon(androidx.appcompat.R.drawable.abc_ic_ab_back_material);
        toolbar.setNavigationContentDescription(R.string.back); toolbar.setNavigationOnClickListener(v -> finish());
        status = findViewById(R.id.hotel_status); retry = findViewById(R.id.hotel_retry);
        RecyclerView list = findViewById(R.id.hotel_results);
        list.setLayoutManager(new LinearLayoutManager(this)); list.setAdapter(adapter);
        retry.setOnClickListener(v -> search()); search();
    }
    private void search() {
        final int request = ++generation;
        retry.setEnabled(false); status.setText(R.string.places_loading); adapter.replace(Collections.emptyList());
        try {
            new HotelPlacesService(this).search(destination).addOnSuccessListener(response -> {
                if (!active(request)) return;
                adapter.replace(response.getPlaces()); retry.setEnabled(true);
                status.setText(response.getPlaces().isEmpty() ? getString(R.string.places_empty) : getString(R.string.places_count, response.getPlaces().size()));
            }).addOnFailureListener(error -> {
                if (!active(request)) return;
                retry.setEnabled(true);
                String message = getString(R.string.places_error, error instanceof ApiException ? ((ApiException)error).getStatusCode() : -1);
                status.setText(message); Toast.makeText(this, message, Toast.LENGTH_LONG).show();
            });
        } catch (android.content.pm.PackageManager.NameNotFoundException | IllegalStateException error) {
            retry.setEnabled(true); status.setText(R.string.places_missing);
        }
    }
    private boolean active(int request) { return request == generation && !isDestroyed() && !isFinishing(); }
    @Override protected void onDestroy() { generation++; super.onDestroy(); }
}

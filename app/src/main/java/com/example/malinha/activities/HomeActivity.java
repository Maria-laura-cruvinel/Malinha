package com.example.malinha.activities;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.example.malinha.utils.CommonsPhotoView;
import com.example.malinha.R;
import com.example.malinha.database.TripDatabase;
import com.example.malinha.models.Destino;
import com.example.malinha.models.Viagem;
import com.example.malinha.utils.DestinationCatalog;
import com.example.malinha.utils.PlanningUtils;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Comparator;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class HomeActivity extends BaseActivity {
    private static final int UPCOMING_LIMIT = 3;
    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private int tripRequest;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_home);
        applyInsets();
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayShowTitleEnabled(false);

        findViewById(R.id.shortcut_plan).setOnClickListener(v -> open(PlanTripActivity.class));
        findViewById(R.id.shortcut_trips).setOnClickListener(v -> open(TripsActivity.class));
        findViewById(R.id.shortcut_hotels).setOnClickListener(v -> open(HotelsActivity.class));
        findViewById(R.id.shortcut_exchange).setOnClickListener(v -> open(CurrencyActivity.class));
        findViewById(R.id.shortcut_flights).setOnClickListener(v -> open(FlightsActivity.class));
        findViewById(R.id.shortcut_explore).setOnClickListener(v -> open(ExploreActivity.class));
        findViewById(R.id.home_empty_plan).setOnClickListener(v -> open(PlanTripActivity.class));

        BottomNavigationView navigation = findViewById(R.id.bottom_nav);
        ColorStateList tint = new ColorStateList(
            new int[][] { { android.R.attr.state_checked }, {} },
            new int[] { getColor(R.color.petrol), 0xff526970 });
        navigation.setItemIconTintList(tint);
        navigation.setItemTextColor(tint);
        navigation.setItemActiveIndicatorEnabled(true);
        navigation.setItemActiveIndicatorColor(ColorStateList.valueOf(getColor(R.color.coral)));
        navigation.setOnItemSelectedListener(item -> {
            if (item.getItemId() == R.id.nav_trips) open(TripsActivity.class);
            else if (item.getItemId() == R.id.nav_explore) open(ExploreActivity.class);
            return item.getItemId() == R.id.nav_home;
        });
    }

    private void open(Class<?> activity) {
        startActivity(new Intent(this, activity));
    }

    @Override protected void onResume() {
        super.onResume();
        int request = ++tripRequest;
        Calendar today = Calendar.getInstance();
        today.set(Calendar.HOUR_OF_DAY, 0);
        today.set(Calendar.MINUTE, 0);
        today.set(Calendar.SECOND, 0);
        today.set(Calendar.MILLISECOND, 0);
        long startOfDay = today.getTimeInMillis();
        worker.execute(() -> {
            try (TripDatabase database = new TripDatabase(getApplicationContext())) {
                ArrayList<Viagem> trips = database.list();
                trips.removeIf(trip -> trip.end < startOfDay);
                trips.sort(Comparator.comparingLong(trip -> trip.start));
                runOnUiThread(() -> {
                    if (active(request)) showTrips(trips);
                });
            } catch (RuntimeException error) {
                runOnUiThread(() -> {
                    if (!active(request)) return;
                    findViewById(R.id.home_empty).setVisibility(View.GONE);
                    findViewById(R.id.home_trip_scroll).setVisibility(View.GONE);
                    TextView status = findViewById(R.id.home_trip_status);
                    status.setText(R.string.storage_error);
                    status.setVisibility(View.VISIBLE);
                });
            }
        });
    }

    private boolean active(int request) {
        return request == tripRequest && !isFinishing() && !isDestroyed();
    }

    private void showTrips(ArrayList<Viagem> trips) {
        findViewById(R.id.home_trip_status).setVisibility(View.GONE);
        findViewById(R.id.home_empty).setVisibility(trips.isEmpty() ? View.VISIBLE : View.GONE);
        findViewById(R.id.home_trip_scroll).setVisibility(trips.isEmpty() ? View.GONE : View.VISIBLE);
        LinearLayout cards = findViewById(R.id.home_trip_cards);
        for (int index = 0; index < cards.getChildCount(); index++) {
            CommonsPhotoView.clear(cards.getChildAt(index));
        }
        cards.removeAllViews();
        ArrayList<Destino> catalog = DestinationCatalog.all();
        for (int index = 0; index < Math.min(UPCOMING_LIMIT, trips.size()); index++) {
            Viagem trip = trips.get(index);
            View card = getLayoutInflater().inflate(R.layout.item_home_trip, cards, false);
            ((TextView) card.findViewById(R.id.home_trip_name)).setText(trip.destination);
            ((TextView) card.findViewById(R.id.home_trip_dates)).setText(getString(
                R.string.home_trip_dates, PlanningUtils.date(trip.start), PlanningUtils.date(trip.end)));
            int fallback = R.drawable.destination_generic;
            for (Destino destination : catalog) {
                String fullName = getString(destination.name);
                String city = fullName.split(",", 2)[0];
                if (fullName.equalsIgnoreCase(trip.destination) || city.equalsIgnoreCase(trip.destination)) {
                    fallback = destination.image;
                    break;
                }
            }
            card.findViewById(R.id.home_trip_continue).setOnClickListener(v -> startActivity(
                new Intent(this, ItineraryDraftActivity.class).putExtra("trip_id", trip.id)));
            cards.addView(card);
            CommonsPhotoView.bindDestination(card, trip.destination, fallback);
        }
    }

    @Override protected void onDestroy() {
        tripRequest++;
        worker.shutdownNow();
        super.onDestroy();
    }
}

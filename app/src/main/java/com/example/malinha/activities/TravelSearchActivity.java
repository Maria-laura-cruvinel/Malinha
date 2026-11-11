package com.example.malinha.activities;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.*;
import com.example.malinha.R;
import com.example.malinha.services.TravelSearchService;
import com.example.malinha.utils.PlanningUtils;
import com.google.android.material.appbar.MaterialToolbar;
import java.util.Calendar;

/** Shared input validation, with distinct hotel and flight screens. */
public abstract class TravelSearchActivity extends BaseActivity {
    protected abstract boolean isFlight();
    private long start = -1, end = -1;
    private EditText origin, destination, count;
    private com.example.malinha.utils.CityAutocompleteController cityAutocomplete;
    @Override protected void onCreate(Bundle state) {
        super.onCreate(state); setContentView(R.layout.activity_travel_search); applyInsets();
        MaterialToolbar toolbar = findViewById(R.id.planning_toolbar);
        toolbar.setTitle(isFlight() ? R.string.label_flights : R.string.label_hotels);
        toolbar.setNavigationIcon(androidx.appcompat.R.drawable.abc_ic_ab_back_material);
        toolbar.setNavigationContentDescription(R.string.back); toolbar.setNavigationOnClickListener(v -> finish());
        origin = findViewById(R.id.search_origin); destination = findViewById(R.id.search_destination); count = findViewById(R.id.passengers);
        if (!isFlight()) {
            origin.setVisibility(View.GONE); findViewById(R.id.label_search_origin).setVisibility(View.GONE);
            findViewById(R.id.cabin).setVisibility(View.GONE); findViewById(R.id.label_cabin).setVisibility(View.GONE);
            ((TextView)findViewById(R.id.label_search_destination)).setText(R.string.hotel_destination);
            ((TextView)findViewById(R.id.label_outbound)).setText(R.string.check_in);
            ((TextView)findViewById(R.id.label_inbound)).setText(R.string.check_out);
            ((TextView)findViewById(R.id.label_passengers)).setText(R.string.guests);
            cityAutocomplete = new com.example.malinha.utils.CityAutocompleteController(this, destination);
        }
        if (state == null) count.setText("1");
        else { start = state.getLong("start", -1); end = state.getLong("end", -1); }
        refreshDates();
        findViewById(R.id.outbound).setOnClickListener(v -> chooseDate(true));
        findViewById(R.id.inbound).setOnClickListener(v -> chooseDate(false));
        Button search = findViewById(R.id.search_travel); search.setText(isFlight() ? R.string.search_flights : R.string.search_hotels);
        search.setOnClickListener(v -> search(false));
        findViewById(R.id.places_hotels).setVisibility(isFlight() ? View.GONE : View.VISIBLE);
        findViewById(R.id.places_hotels).setOnClickListener(v -> {
            String city = destination.getText().toString().trim();
            if (city.isEmpty()) { destination.setError(getString(R.string.error_destination)); return; }
            startActivity(new android.content.Intent(this, HotelResultsActivity.class).putExtra("destination", city));
        });
        findViewById(R.id.cached_flights).setVisibility(isFlight()?View.VISIBLE:View.GONE);
        findViewById(R.id.cached_flights).setOnClickListener(v -> search(true));
    }
    private long today() { Calendar date = Calendar.getInstance(); date.set(Calendar.HOUR_OF_DAY, 0); date.set(Calendar.MINUTE, 0); date.set(Calendar.SECOND, 0); date.set(Calendar.MILLISECOND, 0); return date.getTimeInMillis(); }
    private void chooseDate(boolean first) {
        Calendar date = Calendar.getInstance();
        long selected = first ? start : end;
        if (selected >= 0) date.setTimeInMillis(selected);
        else if (!first && start >= 0) { date.setTimeInMillis(start); if (!isFlight()) date.add(Calendar.DAY_OF_MONTH, 1); }
        DatePickerDialog dialog = new DatePickerDialog(this, (view, year, month, day) -> {
            Calendar picked = Calendar.getInstance(); picked.clear(); picked.set(year, month, day, 12, 0);
            if (first) { start = picked.getTimeInMillis(); if (end < start || (!isFlight() && end == start)) end = -1; }
            else end = picked.getTimeInMillis();
            refreshDates();
        }, date.get(Calendar.YEAR), date.get(Calendar.MONTH), date.get(Calendar.DAY_OF_MONTH));
        Calendar minimum = Calendar.getInstance(); minimum.setTimeInMillis(today());
        if (!first && start >= 0) { minimum.setTimeInMillis(start); if (!isFlight()) minimum.add(Calendar.DAY_OF_MONTH, 1); }
        dialog.getDatePicker().setMinDate(Math.max(today(), minimum.getTimeInMillis())); dialog.show();
    }
    private void refreshDates() {
        Button first = findViewById(R.id.outbound), last = findViewById(R.id.inbound);
        first.setText(start < 0 ? getString(R.string.pick_date) : PlanningUtils.date(start));
        last.setText(end < 0 ? getString(R.string.pick_date) : PlanningUtils.date(end));
        first.setContentDescription(getString(isFlight() ? R.string.outbound : R.string.check_in) + ": " + first.getText());
        last.setContentDescription(getString(isFlight() ? R.string.inbound : R.string.check_out) + ": " + last.getText());
    }
    private void search(boolean cached) {
        String from = origin.getText().toString().trim(), to = destination.getText().toString().trim();
        if (to.isEmpty()) { destination.setError(getString(R.string.error_destination)); destination.requestFocus(); message(R.string.error_destination); return; }
        if (isFlight() && (from.isEmpty() || from.equalsIgnoreCase(to))) { origin.setError(getString(R.string.invalid_route)); origin.requestFocus(); message(R.string.invalid_route); return; }
        if (start < today() || end < start || (!isFlight() && end == start)) { message(isFlight() ? R.string.invalid_flight_dates : R.string.invalid_stay); return; }
        int adults;
        try { adults = Integer.parseInt(count.getText().toString()); if (adults < 1 || adults > 9) throw new NumberFormatException(); }
        catch (NumberFormatException error) { count.setError(getString(R.string.travel_count_error)); message(R.string.travel_count_error); return; }
        if(cached){
            if(adults!=1 || ((Spinner)findViewById(R.id.cabin)).getSelectedItemPosition()!=0){message(R.string.flight_scope);return;}
            String iataFrom=from.toUpperCase(java.util.Locale.ROOT),iataTo=to.toUpperCase(java.util.Locale.ROOT);
            if(!iataFrom.matches("[A-Z]{3}") || !iataTo.matches("[A-Z]{3}")){message(R.string.flight_iata_error);return;}
            startActivity(new android.content.Intent(this,FlightPricesActivity.class).putExtra("origin",iataFrom).putExtra("destination",iataTo).putExtra("start",start).putExtra("end",end));return;
        }
        if (isFlight()) TravelSearchService.open(this, TravelSearchService.flights(from, to, start, end, adults, ((Spinner)findViewById(R.id.cabin)).getSelectedItem().toString()));
        else TravelSearchService.open(this, TravelSearchService.hotels(to, start, end, adults));
    }
    private void message(int resource) { Toast.makeText(this, resource, Toast.LENGTH_LONG).show(); }
    @Override protected void onSaveInstanceState(Bundle state) { if(cityAutocomplete!=null)cityAutocomplete.save(state); state.putLong("start", start); state.putLong("end", end); super.onSaveInstanceState(state); }
    @Override protected void onRestoreInstanceState(Bundle state) { super.onRestoreInstanceState(state); if(cityAutocomplete!=null)cityAutocomplete.restore(state); }
    @Override protected void onDestroy() { if(cityAutocomplete!=null)cityAutocomplete.close(); super.onDestroy(); }
}

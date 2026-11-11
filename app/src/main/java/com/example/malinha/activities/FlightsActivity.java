package com.example.malinha.activities;

import android.app.DatePickerDialog;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.AutoCompleteTextView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import com.example.malinha.R;
import com.example.malinha.services.FlightSearchLinks;
import com.example.malinha.services.FlightSearchLinks.Cabin;
import com.example.malinha.services.FlightSearchLinks.Provider;
import com.example.malinha.services.TravelSearchService;
import com.example.malinha.utils.PlanningUtils;
import com.example.malinha.utils.AirportAutocomplete;
import com.google.android.material.appbar.MaterialToolbar;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.Calendar;

public final class FlightsActivity extends BaseActivity {
    private static final int HISTORY_LIMIT = 5;
    private AutoCompleteTextView origin, destination;
    private CheckBox oneWay;
    private Spinner cabin;
    private SharedPreferences preferences;
    private long departure = -1, returnDate = -1;
    private int adults = 1;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_flights);
        applyInsets();
        MaterialToolbar toolbar = findViewById(R.id.planning_toolbar);
        toolbar.setTitle(R.string.label_flights);
        toolbar.setNavigationIcon(androidx.appcompat.R.drawable.abc_ic_ab_back_material);
        toolbar.setNavigationContentDescription(R.string.back);
        toolbar.setNavigationOnClickListener(v -> finish());
        preferences = getSharedPreferences("flight_search", MODE_PRIVATE);
        origin = findViewById(R.id.flight_origin);
        destination = findViewById(R.id.flight_destination);
        oneWay = findViewById(R.id.flight_one_way);
        cabin = findViewById(R.id.flight_cabin);
        AirportAutocomplete.bind(origin);
        AirportAutocomplete.bind(destination);
        if (state == null) origin.setText(preferences.getString("origin", ""), false);
        else {
            departure = state.getLong("departure", -1);
            returnDate = state.getLong("return", -1);
            adults = state.getInt("adults", 1);
        }
        oneWay.setOnCheckedChangeListener((button, checked) -> refresh());
        findViewById(R.id.flight_swap).setOnClickListener(v -> {
            String from = origin.getText().toString();
            origin.setText(destination.getText().toString(), false);
            destination.setText(from, false);
            origin.setError(null);
            destination.setError(null);
        });
        findViewById(R.id.flight_departure).setOnClickListener(v -> chooseDate(true));
        findViewById(R.id.flight_return).setOnClickListener(v -> chooseDate(false));
        findViewById(R.id.flight_less).setOnClickListener(v -> { adults--; refresh(); });
        findViewById(R.id.flight_more).setOnClickListener(v -> { adults++; refresh(); });
        FlightReservations reservations = new FlightReservations(this);
        findViewById(R.id.flight_reservations).setOnClickListener(v -> reservations.show());
        findViewById(R.id.flight_airport_credits).setOnClickListener(v ->
                new androidx.appcompat.app.AlertDialog.Builder(this)
                        .setTitle(R.string.flight_airport_credits)
                        .setMessage(R.string.flight_airport_credit_text)
                        .setNegativeButton(R.string.close_dialog, null)
                        .setPositiveButton(R.string.flight_airport_credit_source, (d, w) ->
                                TravelSearchService.open(this, Uri.parse("https://openflights.org/data.php")))
                        .show());
        findViewById(R.id.flight_clear_recent).setOnClickListener(v -> {
            preferences.edit().remove("recent").remove("origin").apply();
            renderHistory();
        });
        LinearLayout buttons = findViewById(R.id.flight_providers);
        String[] labels = getResources().getStringArray(R.array.flight_providers);
        for (Provider provider : Provider.values()) {
            Button button = new Button(this);
            button.setText(labels[provider.ordinal()]);
            button.setMinHeight((int) (48 * getResources().getDisplayMetrics().density));
            button.setOnClickListener(v -> compare(provider));
            buttons.addView(button);
        }
        refresh();
        renderHistory();
    }

    private static Calendar today() {
        Calendar date = Calendar.getInstance();
        date.set(Calendar.HOUR_OF_DAY, 0);
        date.set(Calendar.MINUTE, 0);
        date.set(Calendar.SECOND, 0);
        date.set(Calendar.MILLISECOND, 0);
        return date;
    }

    private void chooseDate(boolean outbound) {
        Calendar initial = today();
        long minimum = outbound ? initial.getTimeInMillis() : Math.max(departure, initial.getTimeInMillis());
        initial.setTimeInMillis(Math.max(minimum, outbound ? departure : returnDate));
        DatePickerDialog picker = new DatePickerDialog(this, (view, year, month, day) -> {
            Calendar selected = Calendar.getInstance();
            selected.clear();
            selected.set(year, month, day, 12, 0);
            if (outbound) {
                departure = selected.getTimeInMillis();
                if (returnDate < departure) returnDate = -1;
            } else returnDate = selected.getTimeInMillis();
            refresh();
        }, initial.get(Calendar.YEAR), initial.get(Calendar.MONTH), initial.get(Calendar.DAY_OF_MONTH));
        picker.getDatePicker().setMinDate(minimum);
        picker.show();
    }

    private void refresh() {
        setDate(R.id.flight_departure, R.string.outbound, departure);
        setDate(R.id.flight_return, R.string.inbound, returnDate);
        findViewById(R.id.flight_return).setVisibility(oneWay.isChecked() ? View.GONE : View.VISIBLE);
        ((TextView) findViewById(R.id.flight_adults)).setText(Integer.toString(adults));
        findViewById(R.id.flight_less).setEnabled(adults > 1);
        findViewById(R.id.flight_more).setEnabled(adults < 9);
    }

    private void setDate(int viewId, int label, long date) {
        ((Button) findViewById(viewId)).setText(getString(R.string.flight_date_label,
                getString(label), date < 0 ? getString(R.string.pick_date) : PlanningUtils.date(date)));
    }

    private void compare(Provider provider) {
        String from = origin.getText().toString().trim();
        String to = destination.getText().toString().trim();
        String fromCode = AirportAutocomplete.searchValue(from);
        String toCode = AirportAutocomplete.searchValue(to);
        if (from.isEmpty() || to.isEmpty() || fromCode.equalsIgnoreCase(toCode)) {
            (to.isEmpty() ? destination : origin).setError(getString(R.string.invalid_route));
            return;
        }
        boolean single = oneWay.isChecked();
        if (departure < today().getTimeInMillis() || (!single && returnDate < departure)) {
            Toast.makeText(this, R.string.flight_dates_error, Toast.LENGTH_LONG).show();
            return;
        }
        int cabinIndex = cabin.getSelectedItemPosition();
        java.net.URI link = FlightSearchLinks.build(provider, fromCode, toCode, TravelSearchService.date(departure),
                single ? null : TravelSearchService.date(returnDate), adults, Cabin.values()[cabinIndex]);
        saveSearch(from, to, single ? -1 : returnDate, cabinIndex);
        if (link.toString().equals(provider.landingPage)) {
            Toast.makeText(this, R.string.flight_manual_filters, Toast.LENGTH_LONG).show();
        }
        TravelSearchService.open(this, Uri.parse(link.toString()));
    }

    private JSONArray history() {
        try { return new JSONArray(preferences.getString("recent", "[]")); }
        catch (JSONException error) { return new JSONArray(); }
    }

    private void saveSearch(String from, String to, long inbound, int cabinIndex) {
        try {
            JSONObject entry = new JSONObject();
            entry.put("from", from).put("to", to).put("departure", departure).put("return", inbound)
                    .put("adults", adults).put("cabin", cabinIndex);
            JSONArray old = history();
            JSONArray updated = new JSONArray().put(entry);
            for (int index = 0; index < old.length() && updated.length() < HISTORY_LIMIT; index++) {
                JSONObject item = old.optJSONObject(index);
                if (item != null && !item.toString().equals(entry.toString())) updated.put(item);
            }
            preferences.edit().putString("origin", from).putString("recent", updated.toString()).apply();
            renderHistory();
        } catch (JSONException error) {
            android.util.Log.w("MalinhaFlights", "Falha ao salvar pesquisa recente");
        }
    }

    private void renderHistory() {
        LinearLayout rows = findViewById(R.id.flight_history);
        rows.removeAllViews();
        JSONArray entries = history();
        for (int index = 0; index < entries.length(); index++) {
            JSONObject item = entries.optJSONObject(index);
            if (item == null) continue;
            Button row = new Button(this);
            row.setText(getString(R.string.flight_history_route, item.optString("from"),
                    item.optString("to"), PlanningUtils.date(item.optLong("departure"))));
            row.setOnClickListener(v -> {
                origin.setText(item.optString("from"), false);
                destination.setText(item.optString("to"), false);
                departure = item.optLong("departure", -1);
                returnDate = item.optLong("return", -1);
                adults = item.optInt("adults", 1);
                cabin.setSelection(item.optInt("cabin", 0));
                oneWay.setChecked(returnDate < 0);
                refresh();
            });
            rows.addView(row);
        }
        findViewById(R.id.flight_clear_recent).setEnabled(rows.getChildCount() > 0);
    }

    @Override protected void onSaveInstanceState(Bundle state) {
        state.putLong("departure", departure);
        state.putLong("return", returnDate);
        state.putInt("adults", adults);
        super.onSaveInstanceState(state);
    }

    @Override protected void onRestoreInstanceState(Bundle state) {
        super.onRestoreInstanceState(state);
        refresh();
    }
}

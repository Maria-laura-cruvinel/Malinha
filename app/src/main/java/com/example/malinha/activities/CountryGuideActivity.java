package com.example.malinha.activities;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.malinha.R;
import com.example.malinha.adapters.CountryCalendarAdapter;
import com.example.malinha.models.CountryTravelData;
import com.example.malinha.models.CountryTravelData.Holiday;
import com.example.malinha.models.CountryTravelData.LongWeekend;
import com.example.malinha.services.NagerDateService;
import com.example.malinha.services.RestCountriesService;
import com.example.malinha.utils.HolidayWeekends;
import com.google.android.material.appbar.MaterialToolbar;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class CountryGuideActivity extends BaseActivity {
    private final ExecutorService worker = Executors.newFixedThreadPool(2);
    private final CountryCalendarAdapter adapter = new CountryCalendarAdapter();
    private final ArrayList<CountryOption> countries = new ArrayList<>();
    private Spinner country;
    private EditText year;
    private Button consult;
    private TextView details, calendarStatus;
    private int generation, pending;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state); setContentView(R.layout.activity_country_guide); applyInsets();
        MaterialToolbar toolbar = findViewById(R.id.planning_toolbar); toolbar.setTitle(R.string.country_guide);
        toolbar.setNavigationIcon(androidx.appcompat.R.drawable.abc_ic_ab_back_material);
        toolbar.setNavigationContentDescription(R.string.back); toolbar.setNavigationOnClickListener(v -> finish());
        country = findViewById(R.id.guide_country); year = findViewById(R.id.guide_year);
        consult = findViewById(R.id.guide_consult); details = findViewById(R.id.guide_details);
        calendarStatus = findViewById(R.id.guide_calendar_status);
        Locale portuguese = new Locale("pt", "BR");
        for (String code : Locale.getISOCountries()) countries.add(new CountryOption(code, new Locale("", code).getDisplayCountry(portuguese)));
        Collections.sort(countries, (a, b) -> a.name.compareToIgnoreCase(b.name));
        ArrayAdapter<CountryOption> choices = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, countries);
        choices.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item); country.setAdapter(choices);
        if (state == null) {
            year.setText(Integer.toString(Calendar.getInstance().get(Calendar.YEAR)));
            String initialCountry = getIntent().getStringExtra("country_code");
            if (initialCountry == null) initialCountry = "BR";
            for (int i = 0; i < countries.size(); i++) if (countries.get(i).code.equals(initialCountry)) { country.setSelection(i); break; }
        }
        RecyclerView list = findViewById(R.id.guide_calendar);
        list.setLayoutManager(new LinearLayoutManager(this)); list.setAdapter(adapter);
        consult.setOnClickListener(v -> consult());
        findViewById(R.id.guide_toggle_calendar).setOnClickListener(v ->
            showCalendar(list.getVisibility() != View.VISIBLE));
        country.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View view, int position, long id) { clear(); }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });
        year.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { clear(); }
            @Override public void afterTextChanged(Editable s) {}
        });
    }
    private void clear() {
        generation++; pending = 0; busy(false); adapter.replace(Collections.emptyList());
        showCalendar(false);
        details.setText(R.string.guide_initial); calendarStatus.setText("");
    }
    private void busy(boolean value) { consult.setEnabled(!value); country.setEnabled(!value); year.setEnabled(!value); }
    private boolean active(int request) { return generation == request && !isDestroyed() && !isFinishing(); }
    private void complete() {
        if (--pending != 0) return;
        busy(false);
        if (adapter.getItemCount() == 0) return;
        showCalendar(true);
        RecyclerView calendar = findViewById(R.id.guide_calendar);
        calendar.scrollToPosition(0);
    }
    private void showCalendar(boolean visible) {
        findViewById(R.id.guide_form).setVisibility(visible ? View.GONE : View.VISIBLE);
        findViewById(R.id.guide_calendar).setVisibility(visible ? View.VISIBLE : View.GONE);
        Button toggle = findViewById(R.id.guide_toggle_calendar);
        toggle.setVisibility(adapter.getItemCount() > 0 ? View.VISIBLE : View.GONE);
        toggle.setText(visible ? R.string.guide_show_country : R.string.guide_show_calendar);
    }
    private void consult() {
        final int selectedYear;
        try { selectedYear = Integer.parseInt(year.getText().toString()); if (selectedYear < 1900 || selectedYear > 2100) throw new NumberFormatException(); }
        catch (NumberFormatException error) { year.setError(getString(R.string.guide_year_error)); return; }
        CountryOption selected = (CountryOption)country.getSelectedItem();
        if (selected == null) return;
        final String code = selected.code;
        android.view.inputmethod.InputMethodManager keyboard =
            getSystemService(android.view.inputmethod.InputMethodManager.class);
        if (keyboard != null) keyboard.hideSoftInputFromWindow(year.getWindowToken(), 0);
        clear(); final int request = generation; pending = 2; busy(true);
        details.setText(R.string.guide_loading_country); calendarStatus.setText(R.string.guide_loading_holidays);
        final String token = getString(R.string.rest_countries_api_key).trim();
        worker.execute(() -> {
            try {
                CountryTravelData data = new RestCountriesService().fetch(code, token);
                runOnUiThread(() -> { if (!active(request)) return; details.setText(countryText(data)); complete(); });
            } catch (Exception error) {
                runOnUiThread(() -> {
                    if (!active(request)) return;
                    details.setText(token.isEmpty() ? R.string.guide_key_missing : R.string.guide_country_error);
                    if (!token.isEmpty()) Toast.makeText(this, R.string.guide_country_error, Toast.LENGTH_SHORT).show();
                    complete();
                });
            }
        });
        worker.execute(() -> {
            try {
                ArrayList<Holiday> holidays = new NagerDateService().fetch(code, selectedYear);
                ArrayList<LongWeekend> weekends = HolidayWeekends.calculate(holidays, selectedYear);
                runOnUiThread(() -> {
                    if (!active(request)) return;
                    calendarStatus.setText(holidays.isEmpty() ? getString(R.string.guide_no_holidays) : getString(R.string.guide_holiday_count, holidays.size()));
                    adapter.replace(calendarRows(holidays, weekends)); complete();
                });
            } catch (Exception error) {
                runOnUiThread(() -> {
                    if (!active(request)) return; calendarStatus.setText(R.string.guide_holiday_error);
                    Toast.makeText(this, R.string.guide_holiday_error, Toast.LENGTH_LONG).show(); complete();
                });
            }
        });
    }
    private String values(List<String> values) { return values.isEmpty() ? getString(R.string.guide_not_informed) : TextUtils.join(", ", values); }
    private String countryText(CountryTravelData data) {
        return data.name + "\n\n" + getString(R.string.guide_languages, values(data.languages)) + "\n\n"
            + getString(R.string.guide_currencies, values(data.currencies)) + "\n\n"
            + getString(R.string.guide_timezones, values(data.timezones)) + "\n\n"
            + getString(R.string.guide_calling_codes, values(data.callingCodes));
    }
    private ArrayList<CountryCalendarAdapter.Entry> calendarRows(List<Holiday> holidays, List<LongWeekend> weekends) {
        ArrayList<CountryCalendarAdapter.Entry> rows = new ArrayList<>();
        for (Holiday holiday : holidays) {
            String scope = holiday.national ? getString(R.string.guide_national)
                : getString(R.string.guide_regional, values(holiday.subdivisions));
            rows.add(new CountryCalendarAdapter.Entry(date(holiday.date) + " · " + holiday.name,
                scope + "\n" + getString(R.string.guide_holiday_types, values(holiday.types))));
        }
        rows.add(new CountryCalendarAdapter.Entry(getString(R.string.guide_weekends), getString(R.string.guide_weekend_note)));
        if (weekends.isEmpty()) rows.add(new CountryCalendarAdapter.Entry(getString(R.string.guide_no_weekends), ""));
        for (LongWeekend weekend : weekends) rows.add(new CountryCalendarAdapter.Entry(date(weekend.start) + " – " + date(weekend.end), getString(R.string.guide_weekend_days, weekend.days)));
        return rows;
    }
    private String date(String iso) { return iso.substring(8, 10) + "/" + iso.substring(5, 7) + "/" + iso.substring(0, 4); }
    @Override protected void onDestroy() { generation++; worker.shutdownNow(); super.onDestroy(); }
    private static final class CountryOption {
        final String code, name;
        CountryOption(String code, String name) { this.code = code; this.name = name; }
        @Override public String toString() { return name + " (" + code + ")"; }
    }
}

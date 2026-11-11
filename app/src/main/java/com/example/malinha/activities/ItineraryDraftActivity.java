package com.example.malinha.activities;

import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.SparseArray;
import android.view.View;
import android.widget.*;
import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.viewpager2.widget.ViewPager2;
import com.example.malinha.R;
import com.example.malinha.adapters.ActivityAdapter;
import com.example.malinha.adapters.ItineraryDayAdapter;
import com.example.malinha.models.Atividade;
import com.example.malinha.utils.ItineraryUtils;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/** Session editor. Local database persistence is implemented separately in stage 5. */
public class ItineraryDraftActivity extends BaseActivity implements ActivityAdapter.Actions {
    private ArrayList<Atividade> activities = new ArrayList<>();
    private int selectedDay; private com.example.malinha.models.Viagem trip; private boolean dirty = true;
    private ItineraryDayAdapter dayAdapter;
    private ViewPager2 pager;
    private TabLayoutMediator dayTabs;
    private final ViewPager2.OnPageChangeCallback dayChange = new ViewPager2.OnPageChangeCallback() {
        @Override public void onPageSelected(int position) {
            selectedDay = position;
            updateDayIndicator();
        }
    };
    private AiSuggestionsSheet suggestions;
    private final ActivityResultLauncher<Intent> editor = registerForActivityResult(
        new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() != RESULT_OK || result.getData() == null) return;
            Atividade updated = (Atividade)result.getData().getSerializableExtra("activity");
            if (updated == null) return;
            boolean replaced = false;
            for (int i = 0; i < activities.size(); i++) {
                if (activities.get(i).id.equals(updated.id)) { activities.set(i, updated); replaced = true; break; }
            }
            if (!replaced) activities.add(updated); dirty = true;
            refresh();
            Toast.makeText(this, R.string.activity_updated, Toast.LENGTH_SHORT).show();
        });

    private void suggestActivities() {
        if (suggestions != null && suggestions.isShowing()) return;
        suggestions = new AiSuggestionsSheet(this, trip.destination, selectedDay, this::addSuggestions);
        suggestions.show();
    }

    private int addSuggestions(int day, ArrayList<AiSuggestionsSheet.Suggestion> selected) {
        ArrayList<Atividade> staged = new ArrayList<>();
        ArrayList<Atividade> occupied = ItineraryUtils.forDay(activities, day);
        java.util.HashSet<String> names = new java.util.HashSet<>();
        for (Atividade activity : occupied) names.add(AiSuggestionsSheet.key(activity.name));
        String[] categories = getResources().getStringArray(R.array.activity_categories);

        for (AiSuggestionsSheet.Suggestion suggestion : selected) {
            if (!names.add(AiSuggestionsSheet.key(suggestion.name))) return R.string.draft_ai_duplicate;
            int start = findFreeTime(occupied, suggestion.suggestedTime, suggestion.duration);
            if (start < 0) return R.string.draft_ai_no_slot;

            Atividade activity = new Atividade();
            activity.day = day;
            activity.name = suggestion.name;
            activity.description = suggestion.description;
            activity.startMinutes = start;
            activity.endMinutes = start + suggestion.duration;
            activity.category = categories.length - 1;
            for (int index = 0; index < categories.length; index++) {
                if (AiSuggestionsSheet.key(categories[index]).equals(AiSuggestionsSheet.key(suggestion.category))) {
                    activity.category = index;
                    break;
                }
            }
            staged.add(activity);
            occupied.add(activity);
        }
        activities.addAll(staged);
        dirty = true;
        refresh();
        Toast.makeText(this, getString(R.string.ai_added_batch, staged.size()), Toast.LENGTH_LONG).show();
        return 0;
    }

    private int findFreeTime(ArrayList<Atividade> occupied, int preferred, int duration) {
        final int opening = 9 * 60;
        final int closing = 22 * 60;
        if (preferred >= opening && preferred + duration <= closing
                && nextAvailable(occupied, preferred, duration) == preferred) return preferred;

        int start = opening;
        while (start + duration <= closing) {
            int next = nextAvailable(occupied, start, duration);
            if (next == start) return start;
            start = next;
        }
        return -1;
    }

    private int nextAvailable(ArrayList<Atividade> occupied, int start, int duration) {
        int next = start;
        for (Atividade activity : occupied) {
            if (activity.startMinutes < start + duration && activity.endMinutes > start) {
                next = Math.max(next, activity.endMinutes);
            }
        }
        return next;
    }

    @Override protected void onDestroy() {
        if (suggestions != null) suggestions.dismiss();
        if (dayTabs != null) dayTabs.detach();
        if (pager != null) {
            pager.unregisterOnPageChangeCallback(dayChange);
            pager.setAdapter(null);
        }
        super.onDestroy();
    }

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state); setContentView(R.layout.activity_itinerary_draft); applyInsets();
        try (com.example.malinha.database.TripDatabase db = new com.example.malinha.database.TripDatabase(this)) {
            if (state != null) { trip = (com.example.malinha.models.Viagem)state.getSerializable("trip"); dirty = state.getBoolean("dirty", true); }
            else if (getIntent().hasExtra("trip")) { trip = (com.example.malinha.models.Viagem)getIntent().getSerializableExtra("trip"); }
            else if (getIntent().hasExtra("trip_id")) { trip = db.find(getIntent().getLongExtra("trip_id", -1)); dirty = false; }
            else {
                trip = new com.example.malinha.models.Viagem();
                trip.destination = getIntent().getStringExtra("destination"); trip.start = getIntent().getLongExtra("start", 0); trip.end = getIntent().getLongExtra("end", 0);
                trip.budget = getIntent().getStringExtra("budget"); trip.interests = getIntent().getStringArrayListExtra("interests");
            }
            if (trip == null) throw new IllegalStateException("Missing trip");
            activities = trip.activities;
        } catch (RuntimeException error) { Toast.makeText(this, R.string.storage_error, Toast.LENGTH_LONG).show(); finish(); return; }
        if (state != null) {
            ArrayList<Atividade> restored = (ArrayList<Atividade>)state.getSerializable("activities");
            if (restored != null) activities = restored;
            selectedDay = state.getInt("day", 0);
        }
        MaterialToolbar toolbar = findViewById(R.id.planning_toolbar);
        boolean wizard = getIntent().getBooleanExtra("wizard", false);
        if (wizard) {
            toolbar.setTitle(getString(R.string.wizard_progress, 4, getString(R.string.wizard_route)));
            toolbar.setBackgroundColor(getColor(R.color.petrol));
            toolbar.setTitleTextColor(getColor(R.color.cream));
        } else toolbar.setTitle(R.string.manual_title);
        toolbar.setNavigationIcon(androidx.appcompat.R.drawable.abc_ic_ab_back_material);
        if (wizard) toolbar.setNavigationIconTint(getColor(R.color.cream));
        toolbar.setNavigationContentDescription(R.string.back);
        toolbar.setNavigationOnClickListener(v -> requestExit());
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override public void handleOnBackPressed() { requestExit(); }
        });
        View aiButton = findViewById(R.id.draft_ai_suggest);
        if ((getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0) {
            aiButton.setVisibility(View.VISIBLE);
            aiButton.setOnClickListener(v -> suggestActivities());
        }
        ArrayList<Long> days = ItineraryUtils.days(trip.start, trip.end);
        if (days.isEmpty()) {
            Toast.makeText(this, R.string.error_date_order, Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        selectedDay = state == null ? todayIndex(days) : Math.max(0, Math.min(selectedDay, days.size() - 1));
        SparseArray<Parcelable> scroll = state == null ? null : state.getSparseParcelableArray("day_scroll");
        dayAdapter = new ItineraryDayAdapter(days.size(), trip.destination, !wizard, this,
                day -> ItineraryUtils.forDay(activities, day), this::addActivity, scroll);
        pager = findViewById(R.id.itinerary_pager);
        pager.setAdapter(dayAdapter);
        pager.setCurrentItem(selectedDay, false);
        TabLayout tabs = findViewById(R.id.itinerary_days);
        SimpleDateFormat dates = new SimpleDateFormat("EEE, dd/MM", new Locale("pt", "BR"));
        dayTabs = new TabLayoutMediator(tabs, pager, (tab, position) -> {
            String label = getString(R.string.day_label, position + 1, dates.format(new Date(days.get(position))));
            tab.setText(label);
            tab.setContentDescription(getString(R.string.itinerary_day_accessibility, label, position + 1, days.size()));
        });
        dayTabs.attach();
        pager.registerOnPageChangeCallback(dayChange);
        findViewById(R.id.add_activity).setOnClickListener(v -> addActivity(selectedDay));
        findViewById(R.id.save_trip).setOnClickListener(v -> saveTrip());
        refresh();
    }
    private static int todayIndex(ArrayList<Long> days) {
        Calendar today = Calendar.getInstance();
        Calendar date = Calendar.getInstance();
        for (int i = 0; i < days.size(); i++) {
            date.setTimeInMillis(days.get(i));
            if (today.get(Calendar.YEAR) == date.get(Calendar.YEAR)
                    && today.get(Calendar.DAY_OF_YEAR) == date.get(Calendar.DAY_OF_YEAR)) return i;
        }
        return 0;
    }
    private void addActivity(int day) {
        editor.launch(new Intent(this, EditActivityActivity.class).putExtra("day", day));
    }
    private void refresh() {
        if (dayAdapter == null) return;
        dayAdapter.refresh();
        updateDayIndicator();
    }
    private void updateDayIndicator() {
        TextView counter = findViewById(R.id.itinerary_day_count);
        counter.setText(getString(R.string.itinerary_day_count, selectedDay + 1, dayAdapter.getItemCount()));
        boolean empty = ItineraryUtils.forDay(activities, selectedDay).isEmpty();
        findViewById(R.id.add_activity).setVisibility(empty ? View.GONE : View.VISIBLE);
    }
    @Override public void edit(Atividade item) { editor.launch(new Intent(this, EditActivityActivity.class).putExtra("activity", item)); }
    @Override public void map(Atividade item) {
        startActivity(new Intent(this, MapActivity.class).putExtra("query", item.name + ", " + trip.destination));
    }
    @Override public void delete(Atividade item) {
        new AlertDialog.Builder(this).setMessage(getString(R.string.delete_question, item.name))
            .setNegativeButton(R.string.cancel_action, null)
            .setPositiveButton(R.string.delete_action, (dialog, which) -> { activities.remove(item); dirty = true; refresh(); }).show();
    }
    @Override public void move(Atividade item, int direction) { if (ItineraryUtils.move(activities, item.id, direction)) dirty = true; refresh(); }
    private void saveTrip() {
        trip.activities = activities;
        try (com.example.malinha.database.TripDatabase db = new com.example.malinha.database.TripDatabase(this)) {
            trip.id = db.save(trip); dirty = false;
            if (getIntent().getBooleanExtra("wizard", false)) setResult(RESULT_OK);
            Toast.makeText(this, R.string.trip_saved, Toast.LENGTH_LONG).show();
        } catch (RuntimeException error) { Toast.makeText(this, R.string.storage_error, Toast.LENGTH_LONG).show(); }
    }
    private void requestExit() {
        if (!dirty) { finish(); return; }
        new AlertDialog.Builder(this).setTitle(R.string.exit_draft_title).setMessage(R.string.unsaved_changes)
            .setNegativeButton(R.string.cancel_action, null).setPositiveButton(R.string.exit_draft, (dialog, which) -> finish()).show();
    }
    @Override protected void onSaveInstanceState(Bundle state) {
        if (dayAdapter != null) state.putSparseParcelableArray("day_scroll", dayAdapter.saveScrollPositions());
        trip.activities = activities; state.putSerializable("trip", trip); state.putBoolean("dirty", dirty); state.putSerializable("activities", activities); state.putInt("day", selectedDay); super.onSaveInstanceState(state);
    }
}

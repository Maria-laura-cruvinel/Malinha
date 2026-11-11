package com.example.malinha.activities;

import android.app.TimePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.widget.*;
import com.example.malinha.R;
import com.example.malinha.models.Atividade;
import com.example.malinha.utils.ItineraryUtils;
import com.example.malinha.utils.PlanningUtils;
import com.google.android.material.appbar.MaterialToolbar;

/** Uses an independent copy: cancelling an edit never changes the original activity. */
public class EditActivityActivity extends BaseActivity {
    private Atividade activity;
    private int start = -1, end = -1;
    private EditText name, description, cost;
    private Spinner category;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_edit_activity);
        applyInsets();
        activity = (Atividade)getIntent().getSerializableExtra("activity");
        boolean editing = activity != null;
        if (!editing) { activity = new Atividade(); activity.day = getIntent().getIntExtra("day", 0); }
        MaterialToolbar toolbar = findViewById(R.id.planning_toolbar);
        toolbar.setTitle(editing ? R.string.edit_activity : R.string.add_activity);
        toolbar.setNavigationIcon(androidx.appcompat.R.drawable.abc_ic_ab_back_material);
        toolbar.setNavigationContentDescription(R.string.back);
        toolbar.setNavigationOnClickListener(v -> finish());
        name = findViewById(R.id.input_activity_place);
        description = findViewById(R.id.input_activity_description_label);
        cost = findViewById(R.id.input_activity_cost);
        category = findViewById(R.id.input_activity_category);
        cost.setKeyListener(android.text.method.DigitsKeyListener.getInstance("0123456789.,"));
        if (state == null) {
            name.setText(activity.name); description.setText(activity.description);
            cost.setText(activity.cost); category.setSelection(activity.category);
            if (editing) { start = activity.startMinutes; end = activity.endMinutes; }
        } else { start = state.getInt("start", -1); end = state.getInt("end", -1); }
        refreshTimes();
        findViewById(R.id.input_activity_start).setOnClickListener(v -> chooseTime(true));
        findViewById(R.id.input_activity_end).setOnClickListener(v -> chooseTime(false));
        findViewById(R.id.confirm_activity).setOnClickListener(v -> confirm());
    }

    private void chooseTime(boolean first) {
        int selected = first ? start : end;
        if (selected < 0) selected = first ? 540 : (start >= 0 ? Math.min(start + 60, 1439) : 600);
        new TimePickerDialog(this, (picker, hour, minute) -> {
            if (first) start = hour * 60 + minute; else end = hour * 60 + minute;
            refreshTimes();
        }, selected / 60, selected % 60, true).show();
    }
    private void refreshTimes() {
        Button first = findViewById(R.id.input_activity_start), last = findViewById(R.id.input_activity_end);
        first.setText(start < 0 ? getString(R.string.select_time) : ItineraryUtils.time(start));
        last.setText(end < 0 ? getString(R.string.select_time) : ItineraryUtils.time(end));
        first.setContentDescription(getString(R.string.activity_start) + ": " + first.getText());
        last.setContentDescription(getString(R.string.activity_end) + ": " + last.getText());
    }
    private void confirm() {
        String place = name.getText().toString().trim();
        if (place.isEmpty()) { name.setError(getString(R.string.error_place)); name.requestFocus(); message(R.string.error_place); return; }
        if (start < 0 || end <= start) { message(R.string.error_times); return; }
        String amount;
        try { amount = PlanningUtils.parseBudget(cost.getText().toString()).toPlainString(); }
        catch (IllegalArgumentException error) { cost.setError(getString(R.string.error_budget)); cost.requestFocus(); message(R.string.error_budget); return; }
        activity.name = place; activity.description = description.getText().toString().trim();
        activity.startMinutes = start; activity.endMinutes = end; activity.cost = amount;
        activity.category = category.getSelectedItemPosition();
        setResult(RESULT_OK, new Intent().putExtra("activity", activity));
        finish();
    }
    private void message(int resource) { Toast.makeText(this, resource, Toast.LENGTH_LONG).show(); }
    @Override protected void onSaveInstanceState(Bundle state) {
        state.putInt("start", start); state.putInt("end", end); super.onSaveInstanceState(state);
    }
}

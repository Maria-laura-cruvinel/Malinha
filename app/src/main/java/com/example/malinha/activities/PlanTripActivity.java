package com.example.malinha.activities;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.*;
import com.example.malinha.R;
import com.example.malinha.utils.PlanningUtils;
import com.example.malinha.utils.ItineraryUtils;
import com.example.malinha.models.Viagem;
import com.example.malinha.models.CountryTravelData.Holiday;
import com.example.malinha.services.NagerDateService;
import com.google.android.material.appbar.MaterialToolbar;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Calendar;

/** Collects trip preferences before opening the shared itinerary editor. */
public class PlanTripActivity extends BaseActivity {
 private long start = -1, end = -1; private com.example.malinha.models.Viagem editing;
 private EditText destination, budget;
 private int step, travelers = 1, dateRequest;
 private final int[] panels = {R.id.wizard_destination_panel, R.id.wizard_dates_panel, R.id.wizard_style_panel};
 private final int[] stepLabels = {R.string.destination, R.string.wizard_dates, R.string.wizard_style};
 private final androidx.activity.result.ActivityResultLauncher<Intent> itineraryEditor =
     registerForActivityResult(new androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult(),
         result -> { if (result.getResultCode() == RESULT_OK) finish(); });
 private com.example.malinha.utils.CityAutocompleteController cityAutocomplete;
 private final int[] interestIds = {R.id.interest_culture,R.id.interest_food,R.id.interest_nature,R.id.interest_beach,R.id.interest_shopping,R.id.interest_fun};
 @Override protected void onCreate(Bundle state) {
  super.onCreate(state); setContentView(R.layout.activity_plan_trip); applyInsets();
  MaterialToolbar toolbar=findViewById(R.id.planning_toolbar);
  toolbar.setTitle(R.string.wizard_title);
  toolbar.setNavigationIcon(androidx.appcompat.R.drawable.abc_ic_ab_back_material);
  toolbar.setNavigationContentDescription(R.string.back);
  toolbar.setNavigationOnClickListener(v -> goBack());
  destination=findViewById(R.id.input_destination); budget=findViewById(R.id.input_budget);
  if(state==null && getIntent().hasExtra("prefill_destination")) destination.setText(getIntent().getStringExtra("prefill_destination"));
  budget.setKeyListener(android.text.method.DigitsKeyListener.getInstance("0123456789.,"));
  ((Spinner)findViewById(R.id.wizard_pace)).setSelection(1);
  if(state!=null){
   start=state.getLong("start",-1);end=state.getLong("end",-1);
   step=state.getInt("wizard_step");travelers=state.getInt("wizard_travelers",1);
  }
  if (getIntent().hasExtra("trip_id")) {
   try (com.example.malinha.database.TripDatabase db = new com.example.malinha.database.TripDatabase(this)) {
    editing = db.find(getIntent().getLongExtra("trip_id", -1));
    if (editing == null) throw new IllegalStateException("Missing trip");
    toolbar.setTitle(R.string.edit_trip);
    if (state == null) {
     start=editing.start;end=editing.end;destination.setText(editing.destination);budget.setText(editing.budget);
     travelers=editing.travelers;
     ((Spinner)findViewById(R.id.wizard_pace)).setSelection(editing.pace);
     for(int id:interestIds){CheckBox option=findViewById(id);option.setChecked(editing.interests.contains(option.getText().toString()));}
     message(R.string.edit_date_notice);
    }
   } catch (RuntimeException error) {message(R.string.storage_error);finish();return;}
  }
  cityAutocomplete = new com.example.malinha.utils.CityAutocompleteController(this, destination);
  refreshDates();
  findViewById(R.id.input_start_date).setOnClickListener(v -> chooseDate(true));
  findViewById(R.id.input_end_date).setOnClickListener(v -> chooseDate(false));
  findViewById(R.id.wizard_weekend).setOnClickListener(v -> shortcut(true));
  findViewById(R.id.wizard_later).setOnClickListener(v -> shortcut(false));
  findViewById(R.id.wizard_holiday).setOnClickListener(v -> nextHoliday());
  findViewById(R.id.wizard_less).setOnClickListener(v -> { travelers--; refreshTravelers(); });
  findViewById(R.id.wizard_more).setOnClickListener(v -> { travelers++; refreshTravelers(); });
  findViewById(R.id.wizard_back).setOnClickListener(v -> goBack());
  findViewById(R.id.create_itinerary).setOnClickListener(v -> advance());
  getOnBackPressedDispatcher().addCallback(this, new androidx.activity.OnBackPressedCallback(true) {
   @Override public void handleOnBackPressed() { goBack(); }
  });
  refreshTravelers();
  showStep();
 }
 private void goBack() {
  if (step == 0) finish();
  else { step--; showStep(); }
 }
 private void showStep() {
  for (int i = 0; i < panels.length; i++) findViewById(panels[i]).setVisibility(i == step ? View.VISIBLE : View.GONE);
  ((TextView)findViewById(R.id.wizard_progress_label)).setText(getString(R.string.wizard_progress, step + 1, getString(stepLabels[step])));
  ((ProgressBar)findViewById(R.id.wizard_progress_bar)).setProgress(step + 1);
  ((Button)findViewById(R.id.create_itinerary)).setText(step == 2 ? R.string.wizard_route : R.string.wizard_continue);
  ((ScrollView)findViewById(R.id.wizard_scroll)).scrollTo(0, 0);
 }
 private void advance() {
  if (step == 0 && destination.getText().toString().trim().isEmpty()) {
   destination.setError(getString(R.string.error_destination)); destination.requestFocus(); return;
  }
  if (step == 1) {
   if (!validDates()) return;
   dateRequest++;
   findViewById(R.id.wizard_holiday).setEnabled(true);
  }
  if (step == 2) { createDraft(); return; }
  View focus = getCurrentFocus();
  if (focus != null) {
   android.view.inputmethod.InputMethodManager keyboard = getSystemService(android.view.inputmethod.InputMethodManager.class);
   if (keyboard != null) keyboard.hideSoftInputFromWindow(focus.getWindowToken(), 0);
   focus.clearFocus();
  }
  step++;
  showStep();
 }
 private boolean validDates() {
  if (start < 0 || end < 0) { message(R.string.error_dates); return false; }
  if (end < start) { message(R.string.error_date_order); return false; }
  return true;
 }
 private void refreshTravelers() {
  ((TextView)findViewById(R.id.wizard_count)).setText(Integer.toString(travelers));
  findViewById(R.id.wizard_less).setEnabled(travelers > 1);
  findViewById(R.id.wizard_more).setEnabled(travelers < 99);
 }
 private Calendar today() {
  Calendar day = Calendar.getInstance();
  day.set(Calendar.HOUR_OF_DAY, 12);
  day.set(Calendar.MINUTE, 0);
  day.set(Calendar.SECOND, 0);
  day.set(Calendar.MILLISECOND, 0);
  return day;
 }
 private void shortcut(boolean weekend) {
  Calendar date = today();
  int days = (Calendar.SATURDAY - date.get(Calendar.DAY_OF_WEEK) + 7) % 7;
  date.add(Calendar.DAY_OF_MONTH, weekend ? days : 30);
  setRange(date, weekend ? 1 : 2);
 }
 private void setRange(Calendar date, int nights) {
  dateRequest++;
  findViewById(R.id.wizard_holiday).setEnabled(true);
  start = date.getTimeInMillis();
  date.add(Calendar.DAY_OF_MONTH, nights);
  end = date.getTimeInMillis();
  refreshDates();
 }
 private void nextHoliday() {
  int request = ++dateRequest;
  findViewById(R.id.wizard_holiday).setEnabled(false);
  new Thread(() -> {
   Calendar today = today();
   int year = today.get(Calendar.YEAR);
   java.text.SimpleDateFormat format = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.ROOT);
   String first = format.format(today.getTime());
   Calendar next = null;
   try {
    NagerDateService service = new NagerDateService();
    for (int offset = 0; offset <= 1 && next == null; offset++) {
     for (Holiday holiday : service.fetch("BR", year + offset)) {
      if (!holiday.national || holiday.date.compareTo(first) <= 0) continue;
      String[] fields = holiday.date.split("-");
      next = today();
      next.set(Integer.parseInt(fields[0]), Integer.parseInt(fields[1]) - 1, Integer.parseInt(fields[2]));
      break;
     }
    }
   } catch (Exception error) { android.util.Log.w("MalinhaPlanning", "Falha ao consultar feriado nacional"); }
   Calendar result = next;
   runOnUiThread(() -> {
    if (request != dateRequest || isFinishing() || isDestroyed()) return;
    findViewById(R.id.wizard_holiday).setEnabled(true);
    if (result == null) message(R.string.wizard_holiday_error);
    else setRange(result, 2);
   });
  }, "malinha-holiday").start();
 }
 private void chooseDate(boolean isStart) {
  Calendar initial=Calendar.getInstance(); long selected=isStart?start:end;
  if(selected>=0) initial.setTimeInMillis(selected);
  else if(!isStart && start>=0) initial.setTimeInMillis(start);
  DatePickerDialog dialog=new DatePickerDialog(this,(view,year,month,day)->{
   dateRequest++;
   findViewById(R.id.wizard_holiday).setEnabled(true);
   Calendar date=Calendar.getInstance(); date.clear(); date.set(year,month,day,12,0,0);
   if(isStart){start=date.getTimeInMillis();if(end>=0 && end<start)end=-1;}
   else end=date.getTimeInMillis();
   refreshDates();
  },initial.get(Calendar.YEAR),initial.get(Calendar.MONTH),initial.get(Calendar.DAY_OF_MONTH));
  if(!isStart && start>=0) dialog.getDatePicker().setMinDate(start);
  dialog.show();
 }
 private void refreshDates(){
  Button first=findViewById(R.id.input_start_date),last=findViewById(R.id.input_end_date);
  first.setText(start<0?getString(R.string.pick_date):PlanningUtils.date(start));
  last.setText(end<0?getString(R.string.pick_date):PlanningUtils.date(end));
  first.setContentDescription(getString(R.string.start_date)+": "+first.getText());
  last.setContentDescription(getString(R.string.end_date)+": "+last.getText());
  TextView duration = findViewById(R.id.wizard_nights);
  if (start < 0 || end < start) duration.setText(R.string.wizard_nights_pending);
  else {
   int nights = ItineraryUtils.days(start,end).size() - 1;
   duration.setText(getResources().getQuantityString(R.plurals.wizard_nights, nights, nights));
  }
 }
 private void createDraft(){
  String place=destination.getText().toString().trim();
  if(place.isEmpty()){destination.setError(getString(R.string.error_destination));destination.requestFocus();message(R.string.error_destination);return;}
  if(start<0 || end<0){message(R.string.error_dates);return;}
  if(end<start){message(R.string.error_date_order);return;}
  BigDecimal amount;
  try{amount=PlanningUtils.parseBudget(budget.getText().toString());}
  catch(IllegalArgumentException exception){budget.setError(getString(R.string.error_budget));budget.requestFocus();message(R.string.error_budget);return;}
  ArrayList<String> interests=new ArrayList<>();
  for(int id:interestIds){CheckBox option=findViewById(id);if(option.isChecked())interests.add(option.getText().toString());}
  if(interests.isEmpty()){message(R.string.error_interests);return;}
  Intent intent=new Intent(this,ItineraryDraftActivity.class).putExtra("wizard", true);
  Viagem trip = editing == null ? new Viagem() : editing;
  if (editing != null) {
   int count=com.example.malinha.utils.ItineraryUtils.days(start,end).size();
   for(com.example.malinha.models.Atividade activity:editing.activities){if(activity.day>=count){message(R.string.short_trip_error);return;}}
  }
  trip.destination=place;trip.start=start;trip.end=end;trip.budget=amount.toPlainString();trip.interests=interests;
  trip.travelers=travelers;
  trip.pace=((Spinner)findViewById(R.id.wizard_pace)).getSelectedItemPosition();
  intent.putExtra("trip",trip);
  itineraryEditor.launch(intent);
 }
 private void message(int text){Toast.makeText(this,text,Toast.LENGTH_LONG).show();}
 @Override protected void onSaveInstanceState(Bundle state){
  if(cityAutocomplete!=null)cityAutocomplete.save(state);
  state.putInt("wizard_step",step);state.putInt("wizard_travelers",travelers);
  state.putLong("start",start);state.putLong("end",end);super.onSaveInstanceState(state);
 }
 @Override protected void onRestoreInstanceState(Bundle state){super.onRestoreInstanceState(state);if(cityAutocomplete!=null)cityAutocomplete.restore(state);}
 @Override protected void onDestroy(){if(cityAutocomplete!=null)cityAutocomplete.close();super.onDestroy();}
}

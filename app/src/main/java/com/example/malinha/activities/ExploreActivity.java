package com.example.malinha.activities;
import android.os.Bundle;
import android.content.Intent;
import androidx.recyclerview.widget.*;
import androidx.appcompat.app.AlertDialog;
import com.google.android.material.appbar.MaterialToolbar;
import com.example.malinha.R;
import com.example.malinha.models.Destino;
import com.example.malinha.adapters.DestinationAdapter;
import java.util.ArrayList;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.AdapterView;
import android.view.View;
import com.example.malinha.utils.CommonsPhotoView;
import com.example.malinha.utils.DestinationCatalog;
import com.example.malinha.utils.ItineraryUtils;
import com.example.malinha.utils.ReadyItineraries;
import com.example.malinha.models.Atividade;
import com.example.malinha.models.Viagem;
import java.util.Calendar;
import java.text.Normalizer;
import java.util.Locale;
public class ExploreActivity extends BaseActivity {
 private final ArrayList<Destino> catalog = DestinationCatalog.all();
 private final ArrayList<Section> sections = new ArrayList<>();
 private RecyclerView.Adapter<RecyclerView.ViewHolder> sectionAdapter;
 private int category;
 private static final class Section {
  final int title;
  final ArrayList<Destino> items = new ArrayList<>();
  Section(int title) { this.title = title; }
 }
 private EditText query;
 private Spinner region;
 private TextView catalogStatus;
 @Override protected void onCreate(Bundle state){
  super.onCreate(state);setContentView(R.layout.activity_explore);applyInsets();
  MaterialToolbar toolbar=findViewById(R.id.planning_toolbar);setTitle(R.string.discover_title);
  toolbar.setNavigationIcon(androidx.appcompat.R.drawable.abc_ic_ab_back_material);toolbar.setNavigationContentDescription(R.string.back);toolbar.setNavigationOnClickListener(v->finish());
  RecyclerView list = findViewById(R.id.destinations_list);
  LinearLayoutManager layoutManager = new LinearLayoutManager(this);
  list.setLayoutManager(layoutManager);
  View header = getLayoutInflater().inflate(R.layout.explore_catalog_header, list, false);
  header.findViewById(R.id.explore_places).setOnClickListener(v -> startActivity(new Intent(this, ExplorePlacesActivity.class)));
  header.findViewById(R.id.explore_country_guide).setOnClickListener(v -> startActivity(new Intent(this, CountryGuideActivity.class)));
  query = header.findViewById(R.id.catalog_query);
  region = header.findViewById(R.id.catalog_region);
  catalogStatus = header.findViewById(R.id.catalog_status);
  if (state != null) {
   query.setText(state.getString("catalog_query", ""));
   region.setSelection(state.getInt("catalog_region", 0));
  }
  category = state == null ? 0 : state.getInt("catalog_category");
  com.google.android.material.chip.ChipGroup chips = header.findViewById(R.id.discovery_categories);
  for (String label : getResources().getStringArray(R.array.discovery_categories)) {
   com.google.android.material.chip.Chip chip = new com.google.android.material.chip.Chip(this);
   chip.setId(View.generateViewId());
   chip.setText(label);
   chip.setCheckable(true);
   chips.addView(chip);
  }
  chips.check(chips.getChildAt(category).getId());
  chips.setOnCheckedStateChangeListener((group, ids) -> {
   if (ids.isEmpty()) return;
   category = group.indexOfChild(group.findViewById(ids.get(0)));
   filter();
  });
  android.content.SharedPreferences favorites = getSharedPreferences("destination_favorites", MODE_PRIVATE);
  sectionAdapter = new RecyclerView.Adapter<RecyclerView.ViewHolder>() {
   @Override public RecyclerView.ViewHolder onCreateViewHolder(android.view.ViewGroup parent, int type) {
    View row = getLayoutInflater().inflate(R.layout.item_destination_section, parent, false);
    RecyclerView cards = row.findViewById(R.id.section_cards);
    cards.setLayoutManager(new LinearLayoutManager(ExploreActivity.this, RecyclerView.HORIZONTAL, false));
    return new RecyclerView.ViewHolder(row) {};
   }
   @Override public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
    Section section = sections.get(position);
    ((TextView) holder.itemView.findViewById(R.id.section_title)).setText(section.title);
    RecyclerView cards = holder.itemView.findViewById(R.id.section_cards);
    cards.setAdapter(new DestinationAdapter(section.items, ExploreActivity.this::details, favorites));
    cards.scrollToPosition(0);
   }
   @Override public int getItemCount() { return sections.size(); }
  };
  RecyclerView.Adapter<RecyclerView.ViewHolder> headerAdapter = new RecyclerView.Adapter<RecyclerView.ViewHolder>() {
   @Override public RecyclerView.ViewHolder onCreateViewHolder(android.view.ViewGroup parent, int viewType) {
    return new RecyclerView.ViewHolder(header) {};
   }
   @Override public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {}
   @Override public int getItemCount() { return 1; }
  };
  list.setAdapter(new ConcatAdapter(headerAdapter, sectionAdapter));
  query.addTextChangedListener(new TextWatcher(){
   @Override public void beforeTextChanged(CharSequence s,int start,int count,int after){}
   @Override public void onTextChanged(CharSequence s,int start,int before,int count){filter();}
   @Override public void afterTextChanged(Editable s){}
  });
  region.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){
   @Override public void onItemSelected(AdapterView<?> parent,View view,int position,long id){filter();}
   @Override public void onNothingSelected(AdapterView<?> parent){}
  });
  filter();
 }
 private void details(Destino destination) {
  View content = getLayoutInflater().inflate(R.layout.dialog_destination_details, null);
  String name = getString(destination.name);
  ((TextView) content.findViewById(R.id.destination_summary)).setText(destination.description);
  ((TextView) content.findViewById(R.id.destination_season)).setText(DestinationCatalog.season(destination));
  ArrayList<Atividade> activities = ReadyItineraries.create(destination.name);
  TextView highlights = content.findViewById(R.id.destination_highlights);
  if (!activities.isEmpty()) {
   StringBuilder names = new StringBuilder();
   for (Atividade activity : activities) {
    if (activity.day != 0) break;
    if (names.length() > 0) names.append('\n');
    names.append("• ").append(activity.name);
   }
   highlights.setText(names);
  } else {
   String localHighlights = DestinationCatalog.highlights(destination);
   if (localHighlights != null) highlights.setText(localHighlights);
  }
  AlertDialog dialog = new AlertDialog.Builder(this).setTitle(name).setView(content)
      .setPositiveButton(R.string.discover_use, (d, which) -> plan(name))
      .setNegativeButton(R.string.close_dialog, null).create();
  View ready = content.findViewById(R.id.destination_template);
  ready.setVisibility(activities.isEmpty() ? View.GONE : View.VISIBLE);
  ready.setOnClickListener(v -> { dialog.dismiss(); previewReady(destination); });
  content.findViewById(R.id.destination_holidays).setOnClickListener(v ->
      startActivity(new Intent(this, CountryGuideActivity.class).putExtra("country_code", DestinationCatalog.countryCode(destination))));
  content.findViewById(R.id.destination_places).setOnClickListener(v ->
      startActivity(new Intent(this, ExplorePlacesActivity.class).putExtra("destination", name)));
  dialog.setOnDismissListener(d -> CommonsPhotoView.clear(content));
  dialog.show();
  CommonsPhotoView.bindDestination(content, name, destination.image);
 }
 private void previewReady(Destino destination) {
  ArrayList<Atividade> activities = ReadyItineraries.create(destination.name);
  if (activities.isEmpty()) return;
  StringBuilder preview = new StringBuilder();
  int day = -1;
  for (Atividade activity : activities) {
   if (day != activity.day) {
    if (day >= 0) preview.append("\n");
    day = activity.day;
    preview.append(getString(R.string.ready_day, day + 1)).append('\n');
   }
   preview.append(ItineraryUtils.time(activity.startMinutes)).append(" · ")
       .append(activity.name).append('\n');
  }
  preview.append('\n').append(getString(R.string.ready_notice));
  new AlertDialog.Builder(this)
      .setTitle(getString(R.string.ready_title, day + 1, getString(destination.name)))
      .setMessage(preview)
      .setNegativeButton(R.string.cancel_action, null)
      .setPositiveButton(R.string.ready_pick_date, (dialog, which) -> chooseReadyStart(destination))
      .show();
 }

 private void chooseReadyStart(Destino destination) {
  Calendar today = Calendar.getInstance();
  android.app.DatePickerDialog picker = new android.app.DatePickerDialog(this, (view, year, month, day) -> {
   Calendar date = Calendar.getInstance();
   date.clear();
   date.set(year, month, day, 12, 0, 0);
   Viagem trip = new Viagem();
   trip.destination = getString(destination.name);
   trip.activities = ReadyItineraries.create(destination.name);
   trip.start = date.getTimeInMillis();
   int lastDay = 0;
   for (Atividade activity : trip.activities) lastDay = Math.max(lastDay, activity.day);
   date.add(Calendar.DAY_OF_MONTH, lastDay);
   trip.end = date.getTimeInMillis();
   startActivity(new Intent(this, ItineraryDraftActivity.class).putExtra("trip", trip));
  }, today.get(Calendar.YEAR), today.get(Calendar.MONTH), today.get(Calendar.DAY_OF_MONTH));
  today.set(Calendar.HOUR_OF_DAY, 0);
  today.set(Calendar.MINUTE, 0);
  today.set(Calendar.SECOND, 0);
  today.set(Calendar.MILLISECOND, 0);
  picker.getDatePicker().setMinDate(today.getTimeInMillis());
  picker.show();
 }

 private void plan(String destination){startActivity(new Intent(this,PlanTripActivity.class).putExtra("prefill_destination",destination));}
 private void filter(){
  String text=normalize(query.getText().toString().trim());int selectedRegion=region.getSelectedItemPosition();
  ArrayList<Destino> visible=new ArrayList<>();
  Section ready = new Section(R.string.discover_ready);
  Section nearby = new Section(R.string.discover_nearby);
  Section outdoor = new Section(R.string.discover_outdoor);
  Section cities = new Section(R.string.discover_cities);
  for (Destino destination : catalog) {
   if ((selectedRegion > 0 && selectedRegion != destination.region)
       || !normalize(getString(destination.name)).contains(text)
       || !DestinationCatalog.matchesCategory(destination, category)) continue;
   visible.add(destination);
   if (ReadyItineraries.hasTemplate(destination.name)) ready.items.add(destination);
   else if (destination.region == 1) nearby.items.add(destination);
   else if (DestinationCatalog.matchesCategory(destination, 3)) outdoor.items.add(destination);
   else cities.items.add(destination);
  }
  sections.clear();
  for (Section section : new Section[]{ready, nearby, outdoor, cities}) {
   if (!section.items.isEmpty()) sections.add(section);
  }
  sectionAdapter.notifyDataSetChanged();
  catalogStatus.setText(getString(R.string.catalog_count,visible.size())
   + (visible.isEmpty() ? "\n" + getString(R.string.catalog_empty) : ""));
 }
 private String normalize(String value){return Normalizer.normalize(value,Normalizer.Form.NFD).replaceAll("\\p{M}+","").toLowerCase(Locale.ROOT);}
 @Override protected void onSaveInstanceState(Bundle state) {
  state.putString("catalog_query", query.getText().toString());
  state.putInt("catalog_region", region.getSelectedItemPosition());
  state.putInt("catalog_category", category);
  super.onSaveInstanceState(state);
 }

}

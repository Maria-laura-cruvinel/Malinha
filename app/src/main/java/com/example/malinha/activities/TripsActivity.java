package com.example.malinha.activities;

import android.os.Bundle;
import android.content.Intent;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.*;
import com.google.android.material.appbar.MaterialToolbar;
import com.example.malinha.R;
import com.example.malinha.models.Viagem;
import com.example.malinha.adapters.TripAdapter;
import com.example.malinha.database.TripDatabase;
import java.util.ArrayList;

public class TripsActivity extends BaseActivity implements TripAdapter.Actions {
 private TripDatabase database;
 private TripAdapter adapter;
 @Override protected void onCreate(Bundle state){
  super.onCreate(state);setContentView(R.layout.activity_trips);applyInsets();
  database=new TripDatabase(this);adapter=new TripAdapter(this);
  MaterialToolbar toolbar=findViewById(R.id.planning_toolbar);toolbar.setTitle(R.string.label_trips);
  toolbar.setNavigationIcon(androidx.appcompat.R.drawable.abc_ic_ab_back_material);
  toolbar.setNavigationContentDescription(R.string.back);toolbar.setNavigationOnClickListener(v->finish());
  RecyclerView list=findViewById(R.id.trips_list);list.setLayoutManager(new LinearLayoutManager(this));list.setAdapter(adapter);
  findViewById(R.id.new_trip).setOnClickListener(v->startActivity(new Intent(this,PlanTripActivity.class)));
 }
 @Override protected void onResume(){super.onResume();reload();}
 private void reload(){
  try{ArrayList<Viagem> trips=database.list();adapter.submit(trips);findViewById(R.id.no_trips).setVisibility(trips.isEmpty()?View.VISIBLE:View.GONE);}
  catch(RuntimeException error){Toast.makeText(this,R.string.storage_error,Toast.LENGTH_LONG).show();}
 }
 @Override public void open(Viagem trip){startActivity(new Intent(this,ItineraryDraftActivity.class).putExtra("trip_id",trip.id));}
 @Override public void edit(Viagem trip){startActivity(new Intent(this,PlanTripActivity.class).putExtra("trip_id",trip.id));}
 @Override public void delete(Viagem trip){
  new AlertDialog.Builder(this).setMessage(getString(R.string.delete_trip_question,trip.destination)).setNegativeButton(R.string.cancel_action,null)
   .setPositiveButton(R.string.delete_action,(dialog,which)->{
    try{database.delete(trip.id);reload();}catch(RuntimeException error){Toast.makeText(this,R.string.storage_error,Toast.LENGTH_LONG).show();}
   }).show();
 }
 @Override protected void onDestroy(){if(database!=null)database.close();super.onDestroy();}
}

package com.example.malinha.activities;
import android.os.Bundle;
import android.net.Uri;
import android.view.*;
import android.widget.*;
import androidx.recyclerview.widget.*;
import com.example.malinha.R;
import com.example.malinha.services.*;
import com.google.android.material.appbar.MaterialToolbar;
import java.util.*;
import java.text.NumberFormat;
import java.util.concurrent.*;

public class FlightPricesActivity extends BaseActivity {
 private final ExecutorService worker=Executors.newSingleThreadExecutor();
 private String origin,destination;private long start,end;private int request;
 private final ArrayList<FlightPriceService.Offer> offers=new ArrayList<>();
 private RecyclerView.Adapter<Row> adapter;
 @Override protected void onCreate(Bundle state){
  super.onCreate(state);setContentView(R.layout.activity_flight_prices);applyInsets();
  origin=getIntent().getStringExtra("origin");destination=getIntent().getStringExtra("destination");
  start=getIntent().getLongExtra("start",0);end=getIntent().getLongExtra("end",0);
  if(origin==null || destination==null){finish();return;}
  MaterialToolbar toolbar=findViewById(R.id.planning_toolbar);toolbar.setTitle(origin+" → "+destination);
  toolbar.setNavigationIcon(androidx.appcompat.R.drawable.abc_ic_ab_back_material);toolbar.setNavigationContentDescription(R.string.back);toolbar.setNavigationOnClickListener(v->finish());
  adapter=new RecyclerView.Adapter<Row>(){
   @Override public Row onCreateViewHolder(ViewGroup parent,int type){return new Row(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_flight_price,parent,false));}
   @Override public int getItemCount(){return offers.size();}
   @Override public void onBindViewHolder(Row holder,int position){
    FlightPriceService.Offer fare=offers.get(position);View view=holder.itemView;
    ((TextView)view.findViewById(R.id.fare_price)).setText(getString(R.string.flight_from,NumberFormat.getCurrencyInstance(new Locale("pt","BR")).format(fare.price)));
    ((TextView)view.findViewById(R.id.fare_details)).setText(getString(R.string.flight_details,fare.departure,fare.returning,fare.airline));
    view.findViewById(R.id.fare_link).setVisibility(fare.link==null?View.GONE:View.VISIBLE);
    view.findViewById(R.id.fare_link).setOnClickListener(v->TravelSearchService.open(FlightPricesActivity.this,Uri.parse(fare.link)));
   }
  };
  RecyclerView list=findViewById(R.id.flight_results);list.setLayoutManager(new LinearLayoutManager(this));list.setAdapter(adapter);
  findViewById(R.id.flight_retry).setOnClickListener(v->load());
  findViewById(R.id.flight_external).setOnClickListener(v->TravelSearchService.open(this,TravelSearchService.flights(origin,destination,start,end,1,"Econômica")));
  load();
 }
 private void load(){
  String endpoint=FlightPriceService.endpoint(this);
  if(endpoint==null || endpoint.isEmpty()){status(R.string.flight_unconfigured);findViewById(R.id.flight_retry).setEnabled(false);return;}
  int token=++request;offers.clear();adapter.notifyDataSetChanged();status(R.string.flight_loading);findViewById(R.id.flight_retry).setEnabled(false);
  worker.execute(()->{
   try{
    FlightPriceService.Result result=new FlightPriceService().fetch(endpoint,origin,destination,TravelSearchService.date(start),TravelSearchService.date(end));
    runOnUiThread(()->{if(isDestroyed() || isFinishing() || token!=request)return;findViewById(R.id.flight_retry).setEnabled(true);
     offers.addAll(result.offers);adapter.notifyDataSetChanged();
     if(offers.isEmpty())status(R.string.flight_empty);else ((TextView)findViewById(R.id.flight_status)).setText(getString(R.string.flight_queried,result.queriedAt));
    });
   }catch(Exception error){runOnUiThread(()->{if(isDestroyed() || isFinishing() || token!=request)return;findViewById(R.id.flight_retry).setEnabled(true);status(R.string.flight_error);Toast.makeText(this,R.string.flight_error,Toast.LENGTH_LONG).show();});}
  });
 }
 private void status(int id){((TextView)findViewById(R.id.flight_status)).setText(id);}
 @Override protected void onDestroy(){request++;worker.shutdownNow();super.onDestroy();}
 static class Row extends RecyclerView.ViewHolder{Row(View view){super(view);}}
}

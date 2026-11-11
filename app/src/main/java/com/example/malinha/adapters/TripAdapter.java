package com.example.malinha.adapters;

import android.view.*;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.malinha.utils.CommonsPhotoView;
import com.example.malinha.R;
import com.example.malinha.models.Viagem;
import com.example.malinha.utils.ItineraryUtils;
import com.example.malinha.utils.PlanningUtils;
import java.util.ArrayList;
import java.util.Locale;
import java.text.NumberFormat;
import java.math.BigDecimal;

public class TripAdapter extends RecyclerView.Adapter<TripAdapter.Holder> {
 public interface Actions { void open(Viagem trip); void edit(Viagem trip); void delete(Viagem trip); }
 private final Actions actions;
 private ArrayList<Viagem> trips=new ArrayList<>();
 public TripAdapter(Actions actions){this.actions=actions;}
 public void submit(ArrayList<Viagem> value){trips=value;notifyDataSetChanged();}
 @NonNull @Override public Holder onCreateViewHolder(@NonNull ViewGroup parent,int type){return new Holder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_trip,parent,false));}
 @Override public void onBindViewHolder(@NonNull Holder holder,int position){
  Viagem trip=trips.get(position);View view=holder.itemView;
  CommonsPhotoView.bindDestination(view, trip.destination, R.drawable.destination_generic);
  ((TextView)view.findViewById(R.id.trip_name)).setText(trip.destination);
  ((TextView)view.findViewById(R.id.trip_period)).setText(view.getContext().getString(R.string.trip_dates,PlanningUtils.date(trip.start),PlanningUtils.date(trip.end)));
  ((TextView)view.findViewById(R.id.trip_amount)).setText(view.getContext().getString(R.string.trip_budget,NumberFormat.getCurrencyInstance(new Locale("pt","BR")).format(new BigDecimal(trip.budget))));
  ((TextView)view.findViewById(R.id.trip_length)).setText(view.getResources().getQuantityString(R.plurals.trip_day_count,ItineraryUtils.days(trip.start,trip.end).size(),ItineraryUtils.days(trip.start,trip.end).size()));
  view.findViewById(R.id.open_trip).setOnClickListener(v->actions.open(trip));
  view.findViewById(R.id.edit_trip).setOnClickListener(v->actions.edit(trip));
  view.findViewById(R.id.delete_action).setOnClickListener(v->actions.delete(trip));
 }
 @Override public void onViewRecycled(@NonNull Holder holder) {
  CommonsPhotoView.clear(holder.itemView);
  super.onViewRecycled(holder);
 }
 @Override public int getItemCount(){return trips.size();}
 static class Holder extends RecyclerView.ViewHolder{Holder(View view){super(view);}}
}

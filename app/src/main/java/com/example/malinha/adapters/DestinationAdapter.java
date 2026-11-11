package com.example.malinha.adapters;
import android.view.*;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.malinha.R;
import com.example.malinha.utils.CommonsPhotoView;
import com.example.malinha.models.Destino;
import java.util.ArrayList;
public class DestinationAdapter extends RecyclerView.Adapter<DestinationAdapter.Holder> {
 public interface Select {void select(Destino destination);}
 private final ArrayList<Destino> items;
 private final Select select;
 private final android.content.SharedPreferences favorites;
 public DestinationAdapter(ArrayList<Destino> items, Select select, android.content.SharedPreferences favorites) {
  this.items = new ArrayList<>(items);
  this.select = select;
  this.favorites = favorites;
 }
 public void replace(ArrayList<Destino> values){items.clear();items.addAll(values);notifyDataSetChanged();}
 @NonNull @Override public Holder onCreateViewHolder(@NonNull ViewGroup parent,int type){return new Holder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_destination,parent,false));}
 @Override public void onBindViewHolder(@NonNull Holder holder,int position){
  Destino item=items.get(position);View view=holder.itemView;
  ((TextView)view.findViewById(R.id.destination_name)).setText(item.name);
  ((TextView)view.findViewById(R.id.destination_description)).setText(item.description);
  CommonsPhotoView.bindDestination(view, view.getContext().getString(item.name), item.image);
  Button favorite = view.findViewById(R.id.destination_favorite);
  String key = view.getResources().getResourceEntryName(item.name);
  showFavorite(favorite, favorites.getBoolean(key, false));
  favorite.setOnClickListener(v -> {
   boolean selected = !favorites.getBoolean(key, false);
   favorites.edit().putBoolean(key, selected).apply();
   showFavorite(favorite, selected);
  });
  view.setOnClickListener(v->select.select(item));
 }
 private void showFavorite(Button button, boolean selected) {
  button.setText(selected ? "♥" : "♡");
  button.setContentDescription(button.getContext().getString(selected ? R.string.remove_favorite : R.string.add_favorite));
 }
 @Override public void onViewRecycled(@NonNull Holder holder) {
  CommonsPhotoView.clear(holder.itemView);
  super.onViewRecycled(holder);
 }
 @Override public int getItemCount(){return items.size();}
 static class Holder extends RecyclerView.ViewHolder{Holder(View view){super(view);}}
}

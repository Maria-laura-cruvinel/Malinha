package com.example.malinha.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.malinha.utils.CommonsPhotoView;
import com.example.malinha.R;
import com.example.malinha.models.Atividade;
import com.example.malinha.utils.ItineraryUtils;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Locale;

public class ActivityAdapter extends RecyclerView.Adapter<ActivityAdapter.Holder> {
    public interface Actions {
        void edit(Atividade item);
        void delete(Atividade item);
        void move(Atividade item, int direction);
        void map(Atividade item);
    }
    private ArrayList<Atividade> items = new ArrayList<>();
    private final Actions actions;
    private final String destination;
    public ActivityAdapter(Actions actions, String destination) {
        this.actions = actions;
        this.destination = destination;
    }
    public void submit(ArrayList<Atividade> value) { items = value; notifyDataSetChanged(); }
    @NonNull @Override public Holder onCreateViewHolder(@NonNull ViewGroup parent, int type) {
        return new Holder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_activity, parent, false));
    }
    @Override public void onBindViewHolder(@NonNull Holder holder, int position) {
        Atividade item = items.get(position);
        View view = holder.itemView;
        CommonsPhotoView.bind(view, item.name, destination, R.drawable.destination_generic);
        ((TextView)view.findViewById(R.id.activity_name)).setText(item.name);
        ((TextView)view.findViewById(R.id.activity_time)).setText(ItineraryUtils.time(item.startMinutes) + " – " + ItineraryUtils.time(item.endMinutes));
        ((TextView)view.findViewById(R.id.activity_description)).setText(item.description);
        view.findViewById(R.id.activity_description).setVisibility(item.description.isEmpty() ? View.GONE : View.VISIBLE);
        String category = view.getResources().getStringArray(R.array.activity_categories)[item.category];
        String cost = NumberFormat.getCurrencyInstance(new Locale("pt", "BR")).format(new BigDecimal(item.cost));
        ((TextView)view.findViewById(R.id.activity_details)).setText(category + " · " + cost);
        view.findViewById(R.id.activity_edit).setOnClickListener(v -> actions.edit(item));
        view.findViewById(R.id.activity_delete).setOnClickListener(v -> actions.delete(item));
        view.findViewById(R.id.activity_map).setOnClickListener(v -> actions.map(item));
        view.findViewById(R.id.activity_up).setEnabled(position > 0);
        view.findViewById(R.id.activity_down).setEnabled(position < items.size() - 1);
        view.findViewById(R.id.activity_up).setOnClickListener(v -> actions.move(item, -1));
        view.findViewById(R.id.activity_down).setOnClickListener(v -> actions.move(item, 1));
    }
    @Override public void onViewRecycled(@NonNull Holder holder) {
        CommonsPhotoView.clear(holder.itemView);
        super.onViewRecycled(holder);
    }
    @Override public int getItemCount() { return items.size(); }
    static class Holder extends RecyclerView.ViewHolder { Holder(View view) { super(view); } }
}

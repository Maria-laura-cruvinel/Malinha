package com.example.malinha.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.recyclerview.widget.RecyclerView;
import com.example.malinha.R;
import com.google.android.libraries.places.api.model.AutocompletePrediction;
import java.util.ArrayList;
import java.util.List;

public final class CitySuggestionAdapter extends RecyclerView.Adapter<CitySuggestionAdapter.Row> {
    public interface Listener { void onSelect(AutocompletePrediction prediction); }
    private final ArrayList<AutocompletePrediction> items = new ArrayList<>();
    private final Listener listener;
    public CitySuggestionAdapter(Listener listener) { this.listener = listener; }
    public void replace(List<AutocompletePrediction> values) { items.clear(); items.addAll(values); notifyDataSetChanged(); }
    @Override public int getItemCount() { return items.size(); }
    @Override public Row onCreateViewHolder(ViewGroup parent, int type) {
        return new Row(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_city_suggestion, parent, false));
    }
    @Override public void onBindViewHolder(Row row, int position) {
        AutocompletePrediction prediction = items.get(position);
        row.primary.setText(prediction.getPrimaryText(null));
        row.secondary.setText(prediction.getSecondaryText(null));
        row.itemView.setOnClickListener(v -> {
            int current = row.getBindingAdapterPosition();
            if (current != RecyclerView.NO_POSITION) listener.onSelect(items.get(current));
        });
    }
    static final class Row extends RecyclerView.ViewHolder {
        final TextView primary, secondary;
        Row(View view) { super(view); primary = view.findViewById(R.id.city_primary); secondary = view.findViewById(R.id.city_secondary); }
    }
}

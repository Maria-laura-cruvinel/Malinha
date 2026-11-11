package com.example.malinha.adapters;

import android.text.Html;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.recyclerview.widget.RecyclerView;
import com.example.malinha.R;
import com.google.android.libraries.places.api.model.Place;
import java.util.ArrayList;
import java.util.List;
import java.util.HashSet;
import java.util.Set;

public final class ExplorePlaceAdapter extends RecyclerView.Adapter<ExplorePlaceAdapter.Row> {
    public interface Listener {
        void onMap(Place place);
        void onDirections(Place place);
        void onHours(Place place);
    }
    private final ArrayList<Place> items = new ArrayList<>();
    private final Listener listener;
    public ExplorePlaceAdapter(Listener listener) { this.listener = listener; }
    public void replace(List<Place> places) { items.clear(); append(places); }
    public void append(List<Place> places) {
        Set<String> ids = new HashSet<>();
        for (Place item : items) if (item.getId() != null) ids.add(item.getId());
        for (Place place : places) if (place.getId() == null || ids.add(place.getId())) items.add(place);
        notifyDataSetChanged();
    }
    @Override public int getItemCount() { return items.size(); }
    @Override public Row onCreateViewHolder(ViewGroup parent, int type) {
        return new Row(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_explore_place, parent, false));
    }
    @Override public void onBindViewHolder(Row row, int position) {
        Place place = items.get(position);
        android.content.Context context = row.itemView.getContext();
        row.name.setText(place.getDisplayName() == null ? context.getString(R.string.local_place) : place.getDisplayName());
        String rating = place.getRating() == null ? context.getString(R.string.places_no_rating)
            : context.getString(R.string.places_rating, place.getRating(), place.getUserRatingCount() == null ? 0 : place.getUserRatingCount());
        row.details.setText(rating + "\n\n" + (place.getFormattedAddress() == null ? context.getString(R.string.places_no_address) : place.getFormattedAddress()));
        row.attributions.setText(place.getAttributions() == null ? "" : Html.fromHtml(TextUtils.join("<br>", place.getAttributions()), Html.FROM_HTML_MODE_LEGACY));
        row.attributions.setMovementMethod(LinkMovementMethod.getInstance());
        row.itemView.findViewById(R.id.local_map).setOnClickListener(v -> listener.onMap(place));
        row.itemView.findViewById(R.id.local_directions).setOnClickListener(v -> listener.onDirections(place));
        View hours = row.itemView.findViewById(R.id.local_hours);
        hours.setEnabled(place.getId() != null);
        hours.setOnClickListener(v -> listener.onHours(place));
    }
    static final class Row extends RecyclerView.ViewHolder {
        final TextView name, details, attributions;
        Row(View view) { super(view); name = view.findViewById(R.id.local_name); details = view.findViewById(R.id.local_details); attributions = view.findViewById(R.id.local_attributions); }
    }
}

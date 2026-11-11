package com.example.malinha.adapters;

import android.net.Uri;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.recyclerview.widget.RecyclerView;
import com.example.malinha.R;
import com.example.malinha.services.TravelSearchService;
import com.google.android.libraries.places.api.model.Place;
import java.util.ArrayList;
import java.util.List;

public final class HotelAdapter extends RecyclerView.Adapter<HotelAdapter.Row> {
    private final ArrayList<Place> places = new ArrayList<>();
    public void replace(List<Place> values) { places.clear(); places.addAll(values); notifyDataSetChanged(); }
    @Override public Row onCreateViewHolder(ViewGroup parent, int type) {
        return new Row(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_hotel, parent, false));
    }
    @Override public int getItemCount() { return places.size(); }
    @Override public void onBindViewHolder(Row row, int index) {
        Place place = places.get(index);
        android.content.Context context = row.itemView.getContext();
        String name = place.getDisplayName() == null ? context.getString(R.string.places_unnamed) : place.getDisplayName();
        row.name.setText(name);
        String rating = place.getRating() == null ? context.getString(R.string.places_no_rating)
            : context.getString(R.string.places_rating, place.getRating(), place.getUserRatingCount() == null ? 0 : place.getUserRatingCount());
        Integer level = place.getPriceLevel();
        String price = context.getString(R.string.places_no_price);
        if (level != null && level > 0 && level <= 4) {
            StringBuilder dollars = new StringBuilder(); for (int i=0; i<level; i++) dollars.append('$');
            price = context.getString(R.string.places_price, dollars.toString());
        }
        row.details.setText(rating + "\n\n" + (place.getFormattedAddress() == null ? context.getString(R.string.places_no_address) : place.getFormattedAddress()) + "\n\n" + price);
        List<String> attributions = place.getAttributions();
        row.attributions.setText(attributions == null ? "" : Html.fromHtml(android.text.TextUtils.join("<br>", attributions), Html.FROM_HTML_MODE_LEGACY));
        row.attributions.setMovementMethod(LinkMovementMethod.getInstance());
        row.itemView.findViewById(R.id.hotel_map).setOnClickListener(v -> {
            Uri.Builder uri = Uri.parse("https://www.google.com/maps/search/").buildUpon()
                .appendQueryParameter("api", "1").appendQueryParameter("query", name + " " + (place.getFormattedAddress() == null ? "" : place.getFormattedAddress()));
            if (place.getId() != null) uri.appendQueryParameter("query_place_id", place.getId());
            TravelSearchService.open(context, uri.build());
        });
    }
    static final class Row extends RecyclerView.ViewHolder {
        final TextView name, details, attributions;
        Row(View view) { super(view); name=view.findViewById(R.id.hotel_name); details=view.findViewById(R.id.hotel_details); attributions=view.findViewById(R.id.hotel_attributions); }
    }
}

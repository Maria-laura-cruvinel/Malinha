package com.example.malinha.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.recyclerview.widget.RecyclerView;
import com.example.malinha.R;
import java.util.ArrayList;
import java.util.List;

public final class CountryCalendarAdapter extends RecyclerView.Adapter<CountryCalendarAdapter.Row> {
    public static final class Entry {
        public final String title, details;
        public Entry(String title, String details) { this.title = title; this.details = details; }
    }
    private final ArrayList<Entry> entries = new ArrayList<>();
    public void replace(List<Entry> values) { entries.clear(); entries.addAll(values); notifyDataSetChanged(); }
    @Override public int getItemCount() { return entries.size(); }
    @Override public Row onCreateViewHolder(ViewGroup parent, int type) {
        return new Row(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_country_calendar, parent, false));
    }
    @Override public void onBindViewHolder(Row row, int position) {
        Entry entry = entries.get(position); row.title.setText(entry.title); row.details.setText(entry.details);
    }
    static final class Row extends RecyclerView.ViewHolder {
        final TextView title, details;
        Row(View view) { super(view); title = view.findViewById(R.id.calendar_title); details = view.findViewById(R.id.calendar_details); }
    }
}

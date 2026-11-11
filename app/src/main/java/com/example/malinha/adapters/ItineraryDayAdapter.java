package com.example.malinha.adapters;

import android.os.Parcelable;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.ConcatAdapter;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.malinha.R;
import com.example.malinha.models.Atividade;
import com.example.malinha.utils.CommonsPhotoView;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.function.IntConsumer;
import java.util.function.IntFunction;

/** Each pager page has its own activity list and saved vertical scroll position. */
public final class ItineraryDayAdapter extends RecyclerView.Adapter<ItineraryDayAdapter.Page> {
    private final int dayCount;
    private final String destination;
    private final boolean showPhoto;
    private final ActivityAdapter.Actions actions;
    private final IntFunction<ArrayList<Atividade>> activitiesForDay;
    private final IntConsumer addActivity;
    private final SparseArray<Parcelable> scrollPositions;
    private final Set<Page> boundPages = new HashSet<>();

    public ItineraryDayAdapter(int dayCount, String destination, boolean showPhoto,
            ActivityAdapter.Actions actions, IntFunction<ArrayList<Atividade>> activitiesForDay,
            IntConsumer addActivity, SparseArray<Parcelable> savedScroll) {
        this.dayCount = dayCount;
        this.destination = destination;
        this.showPhoto = showPhoto;
        this.actions = actions;
        this.activitiesForDay = activitiesForDay;
        this.addActivity = addActivity;
        scrollPositions = savedScroll == null ? new SparseArray<>() : savedScroll.clone();
    }

    @NonNull @Override public Page onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        RecyclerView list = (RecyclerView) LayoutInflater.from(parent.getContext())
                .inflate(R.layout.page_itinerary_day, parent, false);
        return new Page(list);
    }

    @Override public void onBindViewHolder(@NonNull Page page, int position) {
        if (page.day == position) {
            refreshPage(page);
            return;
        }
        release(page);
        page.day = position;
        boundPages.add(page);
        LayoutInflater inflater = LayoutInflater.from(page.list.getContext());
        View header = inflater.inflate(R.layout.item_itinerary_day_header, page.list, false);
        ((TextView) header.findViewById(R.id.draft_destination)).setText(destination);
        page.photo = header.findViewById(R.id.commons_photo_panel);
        if (showPhoto) CommonsPhotoView.bindDestination(page.photo, destination, R.drawable.destination_generic);
        else page.photo.setVisibility(View.GONE);

        View empty = inflater.inflate(R.layout.item_itinerary_day_empty, page.list, false);
        empty.findViewById(R.id.add_empty_day_activity).setOnClickListener(v -> addActivity.accept(page.day));
        page.activities = new ActivityAdapter(actions, destination);
        page.empty = new RowAdapter(empty, false);
        refreshPage(page);
        page.list.setAdapter(new ConcatAdapter(new RowAdapter(header, true), page.activities, page.empty));
        Parcelable scroll = scrollPositions.get(position);
        if (scroll != null) page.layout.onRestoreInstanceState(scroll);
        else page.layout.scrollToPosition(0);
    }

    @Override public void onViewRecycled(@NonNull Page page) {
        release(page);
        super.onViewRecycled(page);
    }

    @Override public int getItemCount() { return dayCount; }

    public void refresh() {
        for (Page page : boundPages) refreshPage(page);
    }

    public SparseArray<Parcelable> saveScrollPositions() {
        for (Page page : boundPages) rememberScroll(page);
        return scrollPositions.clone();
    }

    private void refreshPage(Page page) {
        ArrayList<Atividade> activities = activitiesForDay.apply(page.day);
        page.activities.submit(activities);
        page.empty.setVisible(activities.isEmpty());
    }

    private void rememberScroll(Page page) {
        Parcelable scroll = page.layout.onSaveInstanceState();
        if (scroll != null) scrollPositions.put(page.day, scroll);
    }

    private void release(Page page) {
        if (page.day < 0) return;
        rememberScroll(page);
        boundPages.remove(page);
        if (showPhoto) CommonsPhotoView.clear(page.photo);
        page.list.setAdapter(null);
        page.day = -1;
    }

    static final class Page extends RecyclerView.ViewHolder {
        final RecyclerView list;
        final LinearLayoutManager layout;
        ActivityAdapter activities;
        RowAdapter empty;
        View photo;
        int day = -1;

        Page(RecyclerView list) {
            super(list);
            this.list = list;
            layout = new LinearLayoutManager(list.getContext());
            list.setLayoutManager(layout);
        }
    }

    private static final class RowAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
        private final View row;
        private boolean visible;

        RowAdapter(View row, boolean visible) {
            this.row = row;
            this.visible = visible;
        }

        void setVisible(boolean value) {
            if (visible == value) return;
            visible = value;
            if (value) notifyItemInserted(0);
            else notifyItemRemoved(0);
        }

        @NonNull @Override public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int type) {
            return new RecyclerView.ViewHolder(row) {};
        }

        @Override public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {}
        @Override public int getItemCount() { return visible ? 1 : 0; }
    }
}

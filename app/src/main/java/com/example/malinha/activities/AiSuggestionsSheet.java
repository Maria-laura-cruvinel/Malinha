package com.example.malinha.activities;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.malinha.FirebaseAIService;
import com.example.malinha.R;
import com.example.malinha.utils.CommonsPhotoView;
import com.example.malinha.utils.ItineraryUtils;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.firebase.ai.type.GenerateContentResponse;
import com.google.firebase.appcheck.FirebaseAppCheck;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Locale;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

final class AiSuggestionsSheet {
    interface AddSelected {
        int add(int day, ArrayList<Suggestion> selected);
    }

    static final class Suggestion {
        final String name, city, country, category, description, cost;
        final int duration, suggestedTime;
        boolean selected;

        Suggestion(JSONObject data) {
            name = field(data, "name", 120);
            city = field(data, "city", 100);
            country = field(data, "country", 100);
            category = field(data, "category", 60);
            description = field(data, "description", 500);
            cost = field(data, "cost", 100);
            int minutes = data.optInt("duration_minutes", 60);
            duration = minutes >= 15 && minutes <= 360 ? minutes : 60;
            String time = field(data, "suggested_time", 5);
            suggestedTime = time.matches("(?:[01]\\d|2[0-3]):[0-5]\\d")
                ? Integer.parseInt(time.substring(0, 2)) * 60 + Integer.parseInt(time.substring(3))
                : -1;
        }

        private static String field(JSONObject data, String key, int max) {
            if (data.isNull(key)) return "";
            String text = data.optString(key, "").trim().replace("**", "").replace("`", "");
            return text.length() <= max ? text : "";
        }
    }

    private final ItineraryDraftActivity activity;
    private final String destination;
    private final int day;
    private final AddSelected addSelected;
    private final ArrayList<Suggestion> items = new ArrayList<>();
    private final HashSet<String> seen = new HashSet<>();
    private BottomSheetDialog dialog;
    private RecyclerView list;
    private TextView status;
    private Button more, add;
    private View progress, hero;
    private ListenableFuture<GenerateContentResponse> response;
    private boolean busy, closed;

    AiSuggestionsSheet(ItineraryDraftActivity activity, String destination, int day, AddSelected addSelected) {
        this.activity = activity;
        this.destination = destination;
        this.day = day;
        this.addSelected = addSelected;
    }

    void show() {
        View view = activity.getLayoutInflater().inflate(R.layout.sheet_ai_suggestions, null);
        dialog = new BottomSheetDialog(activity);
        int screenHeight = activity.getWindow().getDecorView().getHeight();
        if (screenHeight <= 0) screenHeight = activity.getResources().getDisplayMetrics().heightPixels;
        int height = Math.round(screenHeight * 0.92f);
        dialog.setContentView(view, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, height));
        ((TextView) view.findViewById(R.id.ai_city)).setText(activity.getString(R.string.ai_city_day, destination, day + 1));
        hero = view.findViewById(R.id.ai_hero);
        float density = activity.getResources().getDisplayMetrics().density;
        if (height / density < 510) hero.setVisibility(View.GONE);
        else CommonsPhotoView.bindDestination(hero, destination, R.drawable.destination_generic);

        progress = view.findViewById(R.id.ai_progress);
        status = view.findViewById(R.id.ai_status);
        more = view.findViewById(R.id.ai_more);
        add = view.findViewById(R.id.ai_add_selected);
        list = view.findViewById(R.id.ai_list);
        list.setLayoutManager(new LinearLayoutManager(activity));
        list.setAdapter(new Cards());
        view.findViewById(R.id.ai_close).setOnClickListener(v -> dismiss());
        more.setOnClickListener(v -> request());
        add.setOnClickListener(v -> addToDraft());
        dialog.setOnDismissListener(ignored -> {
            closed = true;
            if (response != null) response.cancel(true);
            CommonsPhotoView.clear(hero);
            list.setAdapter(null);
        });
        dialog.show();
        dialog.getBehavior().setState(BottomSheetBehavior.STATE_EXPANDED);
        updateActions();
        request();
    }

    boolean isShowing() { return dialog != null && dialog.isShowing(); }
    void dismiss() { if (isShowing()) dialog.dismiss(); }

    private void request() {
        if (busy || closed) return;
        busy = true;
        setStatus(R.string.draft_ai_loading);
        updateActions();
        try {
            FirebaseAppCheck.getInstance().getAppCheckToken(false)
                .addOnSuccessListener(token -> { if (!closed && isShowing()) requestGemini(); })
                .addOnFailureListener(error -> finishRequest(FirebaseAIService.failureMessage(error, true)));
        } catch (RuntimeException error) {
            finishRequest(FirebaseAIService.failureMessage(error, true));
        }
    }

    private void requestGemini() {
        JSONArray excluded = new JSONArray();
        for (Suggestion item : items) excluded.put(item.name);
        String categories = String.join(", ", activity.getResources().getStringArray(R.array.activity_categories));
        try {
            response = new FirebaseAIService().suggest(activity.getString(R.string.ai_structured_prompt,
                destination, categories, excluded.toString()));
            ListenableFuture<GenerateContentResponse> current = response;
            current.addListener(() -> {
                if (closed || !isShowing()) return;
                try {
                    int added = append(current.get().getText());
                    finishRequest(added == 0 ? R.string.ai_no_new : 0);
                } catch (JSONException error) {
                    finishRequest(R.string.draft_ai_empty);
                } catch (InterruptedException error) {
                    Thread.currentThread().interrupt();
                    finishRequest(R.string.draft_ai_error);
                } catch (java.util.concurrent.ExecutionException | RuntimeException error) {
                    finishRequest(FirebaseAIService.failureMessage(error, false));
                }
            }, ContextCompat.getMainExecutor(activity));
        } catch (RuntimeException error) {
            finishRequest(FirebaseAIService.failureMessage(error, false));
        }
    }

    private int append(String answer) throws JSONException {
        if (answer == null || answer.trim().isEmpty()) return 0;
        JSONArray results = new JSONArray(answer);
        int start = items.size();
        for (int index = 0; index < results.length() && items.size() < start + 3; index++) {
            JSONObject data = results.optJSONObject(index);
            if (data == null) continue;
            Suggestion item = new Suggestion(data);
            if (item.name.isEmpty() || item.city.isEmpty() || item.country.isEmpty()
                    || item.description.isEmpty() || !seen.add(key(item.name))) continue;
            items.add(item);
        }
        int count = items.size() - start;
        if (count > 0 && list.getAdapter() != null) list.getAdapter().notifyItemRangeInserted(start, count);
        return count;
    }

    static String key(String name) {
        return Normalizer.normalize(name, Normalizer.Form.NFD).replaceAll("\\p{M}+", "")
            .toLowerCase(Locale.ROOT).replaceAll("[^\\p{L}\\p{N}]+", " ").trim();
    }

    private void finishRequest(int message) {
        if (closed || !isShowing()) return;
        busy = false;
        response = null;
        setStatus(message);
        updateActions();
    }

    private void setStatus(int message) {
        status.setVisibility(message == 0 ? View.GONE : View.VISIBLE);
        if (message != 0) status.setText(message);
    }

    private void updateActions() {
        int count = 0;
        for (Suggestion item : items) if (item.selected) count++;
        add.setText(activity.getResources().getQuantityString(R.plurals.ai_add_count, count, count));
        add.setEnabled(count > 0 && !busy);
        more.setEnabled(!busy);
        progress.setVisibility(busy ? View.VISIBLE : View.GONE);
    }

    private void addToDraft() {
        ArrayList<Suggestion> selected = new ArrayList<>();
        for (Suggestion item : items) if (item.selected) selected.add(item);
        if (selected.isEmpty()) return;
        int error = addSelected.add(day, selected);
        if (error == 0) dismiss();
        else setStatus(error);
    }

    private void details(Suggestion item) {
        String time = item.suggestedTime < 0 ? activity.getString(R.string.ai_not_informed)
            : ItineraryUtils.time(item.suggestedTime);
        String cost = item.cost.isEmpty() ? activity.getString(R.string.ai_not_informed) : item.cost;
        new AlertDialog.Builder(activity).setTitle(item.name)
            .setMessage(activity.getString(R.string.ai_detail_text, item.city, item.country,
                item.description, item.duration, cost, time))
            .setPositiveButton(R.string.close_dialog, null).show();
    }

    private final class Cards extends RecyclerView.Adapter<Holder> {
        @NonNull @Override public Holder onCreateViewHolder(@NonNull ViewGroup parent, int type) {
            return new Holder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_ai_suggestion, parent, false));
        }

        @Override public void onBindViewHolder(@NonNull Holder holder, int position) {
            Suggestion item = items.get(position);
            ((TextView) holder.itemView.findViewById(R.id.ai_name)).setText(item.name);
            ((TextView) holder.itemView.findViewById(R.id.ai_category)).setText(item.category);
            ((TextView) holder.itemView.findViewById(R.id.ai_description)).setText(item.description);
            CheckBox select = holder.itemView.findViewById(R.id.ai_select);
            select.setOnCheckedChangeListener(null);
            select.setChecked(item.selected);
            select.setContentDescription(activity.getString(R.string.ai_select_named, item.name));
            select.setOnCheckedChangeListener((button, checked) -> {
                item.selected = checked;
                updateActions();
            });
            holder.itemView.findViewById(R.id.ai_details).setOnClickListener(v -> details(item));
            String context = destination.contains(",") ? destination : destination + ", " + item.country;
            CommonsPhotoView.bind(holder.itemView, item.name, context, R.drawable.destination_generic);
        }

        @Override public void onViewRecycled(@NonNull Holder holder) {
            CommonsPhotoView.clear(holder.itemView);
            super.onViewRecycled(holder);
        }

        @Override public int getItemCount() { return items.size(); }
    }

    private static final class Holder extends RecyclerView.ViewHolder {
        Holder(View view) { super(view); }
    }
}

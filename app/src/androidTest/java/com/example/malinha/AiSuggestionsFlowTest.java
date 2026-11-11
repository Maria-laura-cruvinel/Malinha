package com.example.malinha;

import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;
import android.view.View;
import androidx.recyclerview.widget.RecyclerView;
import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.espresso.UiController;
import androidx.test.espresso.ViewAction;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import com.example.malinha.activities.ItineraryDraftActivity;
import com.example.malinha.database.TripDatabase;
import com.example.malinha.models.Viagem;
import org.hamcrest.Matcher;
import org.junit.Assume;
import org.junit.Test;
import org.junit.runner.RunWith;

import static androidx.test.espresso.Espresso.closeSoftKeyboard;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.action.ViewActions.scrollTo;
import static androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
public final class AiSuggestionsFlowTest {
    @Test public void selectsTwoSuggestionsEditsAndReopensSavedTrip() {
        Assume.assumeTrue("true".equals(InstrumentationRegistry.getArguments().getString("liveGeminiUi")));
        Context context = ApplicationProvider.getApplicationContext();
        Viagem trip = new Viagem();
        trip.destination = "Osaka, Japão";
        trip.start = trip.end = 1794398400000L;
        long id;
        try (TripDatabase database = new TripDatabase(context)) {
            id = database.save(trip);
        }
        Intent launch = new Intent(context, ItineraryDraftActivity.class).putExtra("trip_id", id);
        try {
            try (ActivityScenario<ItineraryDraftActivity> scenario = ActivityScenario.launch(launch)) {
                onView(withId(R.id.draft_ai_suggest)).perform(click());
                onView(withId(R.id.ai_list)).perform(selectSuggestion(0), selectSuggestion(1));
                onView(withId(R.id.ai_add_selected)).perform(click());
                scenario.onActivity(activity -> {
                    RecyclerView list = activity.findViewById(R.id.activities_list);
                    assertEquals(3, list.getAdapter().getItemCount()); // Header and two activities.
                    RecyclerView.ViewHolder first = list.findViewHolderForAdapterPosition(1);
                    assertNotNull(first);
                    first.itemView.findViewById(R.id.activity_edit).performClick();
                });
                onView(withId(R.id.input_activity_place)).perform(replaceText("Atração editada no teste"));
                closeSoftKeyboard();
                onView(withId(R.id.confirm_activity)).perform(scrollTo(), click());
                onView(withId(R.id.save_trip)).perform(click());
            }
            try (TripDatabase database = new TripDatabase(context)) {
                Viagem saved = database.find(id);
                assertNotNull(saved);
                assertEquals(2, saved.activities.size());
                assertTrue(saved.activities.stream().anyMatch(a -> a.name.equals("Atração editada no teste")));
            }
            try (ActivityScenario<ItineraryDraftActivity> scenario = ActivityScenario.launch(launch)) {
                scenario.onActivity(activity -> {
                    RecyclerView list = activity.findViewById(R.id.activities_list);
                    assertEquals(3, list.getAdapter().getItemCount()); // Header and two activities.
                });
            }
        } finally {
            try (TripDatabase database = new TripDatabase(context)) {
                database.delete(id);
            }
        }
    }

    private static ViewAction selectSuggestion(int position) {
        return new ViewAction() {
            @Override public Matcher<View> getConstraints() { return isAssignableFrom(RecyclerView.class); }
            @Override public String getDescription() { return "selecionar sugestão " + position; }
            @Override public void perform(UiController ui, View view) {
                RecyclerView list = (RecyclerView) view;
                long deadline = SystemClock.elapsedRealtime() + 75_000;
                while (list.getAdapter().getItemCount() <= position
                        && SystemClock.elapsedRealtime() < deadline) ui.loopMainThreadForAtLeast(250);
                assertTrue("O Gemini não retornou sugestões suficientes.", list.getAdapter().getItemCount() > position);
                list.scrollToPosition(position);
                ui.loopMainThreadUntilIdle();
                RecyclerView.ViewHolder holder = list.findViewHolderForAdapterPosition(position);
                assertNotNull(holder);
                holder.itemView.findViewById(R.id.ai_select).performClick();
            }
        };
    }
}

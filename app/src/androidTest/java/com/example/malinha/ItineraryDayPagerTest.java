package com.example.malinha;

import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;
import android.view.InputDevice;
import android.view.MotionEvent;
import android.widget.TextView;
import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.viewpager2.widget.ViewPager2;
import com.example.malinha.activities.ItineraryDraftActivity;
import com.example.malinha.models.Viagem;
import com.google.android.material.tabs.TabLayout;
import java.util.Calendar;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
public class ItineraryDayPagerTest {
    @Test public void opensTodayAndKeepsSwipeSelectionOnRecreation() {
        Context context = ApplicationProvider.getApplicationContext();
        Calendar date = Calendar.getInstance();
        date.set(Calendar.HOUR_OF_DAY, 12);
        date.add(Calendar.DAY_OF_MONTH, -1);
        Viagem trip = new Viagem();
        trip.destination = "Lisboa, Portugal";
        trip.start = date.getTimeInMillis();
        date.add(Calendar.DAY_OF_MONTH, 2);
        trip.end = date.getTimeInMillis();

        Intent intent = new Intent(context, ItineraryDraftActivity.class)
                .putExtra("trip", trip).putExtra("wizard", true);
        try (ActivityScenario<ItineraryDraftActivity> scenario = ActivityScenario.launch(intent)) {
            assertDay(scenario, 1);
            swipeLeft(scenario);
            awaitDay(scenario, 2);
            scenario.recreate();
            assertDay(scenario, 2);
            scenario.onActivity(activity -> {
                TabLayout tabs = activity.findViewById(R.id.itinerary_days);
                tabs.selectTab(tabs.getTabAt(0));
            });
            awaitDay(scenario, 0);
        }
    }

    private static void swipeLeft(ActivityScenario<ItineraryDraftActivity> scenario) {
        float[] points = new float[3];
        scenario.onActivity(activity -> {
            ViewPager2 pager = activity.findViewById(R.id.itinerary_pager);
            int[] location = new int[2];
            pager.getLocationOnScreen(location);
            points[0] = location[0] + pager.getWidth() * 0.85f;
            points[1] = location[0] + pager.getWidth() * 0.15f;
            points[2] = location[1] + pager.getHeight() * 0.5f;
        });
        long started = SystemClock.uptimeMillis();
        touch(started, MotionEvent.ACTION_DOWN, points[0], points[2]);
        for (int step = 1; step <= 10; step++) {
            SystemClock.sleep(18);
            float x = points[0] + (points[1] - points[0]) * step / 10;
            touch(started, MotionEvent.ACTION_MOVE, x, points[2]);
        }
        touch(started, MotionEvent.ACTION_UP, points[1], points[2]);
    }

    private static void touch(long started, int action, float x, float y) {
        MotionEvent event = MotionEvent.obtain(started, SystemClock.uptimeMillis(), action, x, y, 0);
        event.setSource(InputDevice.SOURCE_TOUCHSCREEN);
        try {
            assertTrue(InstrumentationRegistry.getInstrumentation().getUiAutomation().injectInputEvent(event, true));
        } finally {
            event.recycle();
        }
    }

    private static void awaitDay(ActivityScenario<ItineraryDraftActivity> scenario, int expected) {
        boolean[] settled = {false};
        long deadline = SystemClock.uptimeMillis() + 5000;
        do {
            scenario.onActivity(activity -> {
                ViewPager2 pager = activity.findViewById(R.id.itinerary_pager);
                settled[0] = pager.getCurrentItem() == expected && pager.getScrollState() == ViewPager2.SCROLL_STATE_IDLE;
            });
            if (settled[0]) break;
            SystemClock.sleep(50);
        } while (SystemClock.uptimeMillis() < deadline);
        assertTrue("O pager não concluiu a troca de dia.", settled[0]);
        assertDay(scenario, expected);
    }

    private static void assertDay(ActivityScenario<ItineraryDraftActivity> scenario, int expected) {
        scenario.onActivity(activity -> {
            ViewPager2 pager = activity.findViewById(R.id.itinerary_pager);
            TabLayout tabs = activity.findViewById(R.id.itinerary_days);
            TextView counter = activity.findViewById(R.id.itinerary_day_count);
            assertEquals(expected, pager.getCurrentItem());
            assertEquals(expected, tabs.getSelectedTabPosition());
            assertEquals((expected + 1) + " de 3", counter.getText().toString());
        });
    }
}

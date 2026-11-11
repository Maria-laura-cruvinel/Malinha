package com.example.malinha;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.widget.EditText;
import android.widget.TextView;
import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import com.example.malinha.activities.*;
import com.example.malinha.models.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.io.File;
import java.io.FileOutputStream;
import static org.junit.Assert.*;

/** Final device checks. The live exchange test intentionally needs internet. */
@RunWith(AndroidJUnit4.class)
public class ReleaseReviewTest {
    private void screenshot(String name) throws Exception {
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();
        Context context = ApplicationProvider.getApplicationContext();
        File folder = new File(context.getExternalFilesDir(null), "review");
        assertTrue(folder.isDirectory() || folder.mkdirs());
        Bitmap bitmap = InstrumentationRegistry.getInstrumentation().getUiAutomation().takeScreenshot();
        assertNotNull(bitmap);
        try (FileOutputStream out = new FileOutputStream(new File(folder, name + ".png"))) {
            assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG, 100, out));
        } finally { bitmap.recycle(); }
    }
    @Test public void captureMainScreens() throws Exception {
        try (ActivityScenario<HomeActivity> ignored = ActivityScenario.launch(HomeActivity.class)) { screenshot("01-inicio"); }
        try (ActivityScenario<HotelsActivity> ignored = ActivityScenario.launch(HotelsActivity.class)) { screenshot("02-hoteis"); }
        try (ActivityScenario<FlightsActivity> ignored = ActivityScenario.launch(FlightsActivity.class)) { screenshot("03-voos"); }
        try (ActivityScenario<ExploreActivity> ignored = ActivityScenario.launch(ExploreActivity.class)) { screenshot("04-destinos"); }
        Context context = ApplicationProvider.getApplicationContext();
        Viagem trip = new Viagem(); trip.destination = "Paris, França"; trip.budget = "3000";
        trip.start = 1798804800000L; trip.end = trip.start + 86400000;
        Atividade item = new Atividade(); item.name = "Museu do Louvre"; item.description = "Exemplo de atividade manual para demonstração.";
        item.startMinutes = 540; item.endMinutes = 660; trip.activities.add(item);
        try (ActivityScenario<ItineraryDraftActivity> ignored = ActivityScenario.launch(new Intent(context, ItineraryDraftActivity.class).putExtra("trip", trip))) {
            screenshot("05-roteiro-exemplo-nao-salvo");
        }
    }
    @Test public void liveConversionReturnsReferenceRateOnDevice() throws Exception {
        try (ActivityScenario<CurrencyActivity> scenario = ActivityScenario.launch(CurrencyActivity.class)) {
            scenario.onActivity(activity -> {
                ((EditText)activity.findViewById(R.id.currency_amount)).setText("100");
                activity.findViewById(R.id.convert).performClick();
            });
            long deadline = android.os.SystemClock.elapsedRealtime() + 25000;
            boolean[] finished = {false}; String[] result = {""};
            while (!finished[0] && android.os.SystemClock.elapsedRealtime() < deadline) {
                Thread.sleep(250);
                scenario.onActivity(activity -> {
                    finished[0] = activity.findViewById(R.id.convert).isEnabled();
                    result[0] = ((TextView)activity.findViewById(R.id.conversion_result)).getText().toString();
                });
            }
            screenshot("06-cambio-real");
            assertTrue("Conversion did not finish", finished[0]);
            assertTrue(result[0], result[0].contains("Referência da cotação:"));
            assertTrue(result[0], result[0].contains("1 BRL ="));
        }
    }
}

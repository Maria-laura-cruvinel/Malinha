package com.example.malinha;

import android.content.Context;
import android.content.Intent;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import com.example.malinha.activities.CurrencyActivity;
import com.example.malinha.activities.ItineraryDraftActivity;
import com.example.malinha.database.TripDatabase;
import com.example.malinha.models.Atividade;
import com.example.malinha.models.Viagem;
import com.example.malinha.services.CurrencyService;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.math.BigDecimal;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class StorageCurrencyTest {
    @Test public void databaseSurvivesReopeningAndUpdatesWithoutDuplicates() {
        Context context = ApplicationProvider.getApplicationContext();
        String file = "test-" + java.util.UUID.randomUUID() + ".db";
        try {
            long id;
            Viagem trip = new Viagem(); trip.destination = "São Paulo"; trip.budget = "1234.56";
            trip.start = 1798804800000L; trip.end = trip.start + 86400000;
            trip.interests.add("Cultura e história");
            Atividade a = new Atividade(); a.name = "Museu"; a.description = "Exposição";
            a.startMinutes = 540; a.endMinutes = 600; a.cost = "12.50";
            Atividade b = new Atividade(); b.name = "Parque"; b.day = 1;
            trip.activities.add(a); trip.activities.add(b);
            try (TripDatabase db = new TripDatabase(context, file)) { id = db.save(trip); }
            try (TripDatabase db = new TripDatabase(context, file)) {
                Viagem saved = db.find(id);
                assertEquals("São Paulo", saved.destination); assertEquals("1234.56", saved.budget);
                assertEquals(2, saved.activities.size()); assertEquals("Exposição", saved.activities.get(0).description);
                assertEquals(1, saved.activities.get(1).day); assertEquals("12.50", saved.activities.get(0).cost);
                java.util.Collections.swap(saved.activities, 0, 1); saved.destination = "Santos";
                assertEquals(id, db.save(saved)); assertEquals(1, db.list().size());
            }
            try (TripDatabase db = new TripDatabase(context, file)) {
                Viagem saved = db.find(id); assertEquals("Parque", saved.activities.get(0).name);
                assertEquals("Santos", saved.destination); saved.activities.remove(0); db.save(saved);
                assertEquals(1, db.find(id).activities.size()); db.delete(id);
                assertNull(db.find(id)); assertTrue(db.list().isEmpty());
            }
        } finally { context.deleteDatabase(file); }
    }

    @Test public void parserRejectsWrongPairAndInvalidRates() throws Exception {
        // Fixture values only; no network request is made by this parser check.
        String quote = "{\"USDBRL\":{\"code\":\"USD\",\"codein\":\"BRL\",\"bid\":\"5\",\"timestamp\":\"1793613600\"}}";
        CurrencyService.Rate rate = CurrencyService.parse(quote, "BRL", "USD");
        assertEquals(0, new BigDecimal("0.2").compareTo(rate.value));
        assertTrue(rate.date.endsWith("UTC")); assertFalse(rate.derived);
        assertEquals(0, new BigDecimal("5").compareTo(CurrencyService.parse(quote, "USD", "BRL").value));
        String cross = quote.replace("}}", "},\"EURBRL\":{\"code\":\"EUR\",\"codein\":\"BRL\",\"bid\":\"10\",\"timestamp\":\"1793613500\"}}");
        CurrencyService.Rate crossRate = CurrencyService.parse(cross, "USD", "EUR");
        assertEquals(0, new BigDecimal("0.5").compareTo(crossRate.value)); assertTrue(crossRate.derived);
        for (String json : new String[]{"{}", quote.replace("\"code\":\"USD\"", "\"code\":\"EUR\""), quote.replace("\"5\"", "\"-1\""), quote.replace("1793613600", "0")}) {
            try { CurrencyService.parse(json, "BRL", "USD"); fail("Invalid response accepted"); }
            catch (Exception expected) { }
        }
    }

    @Test public void saveButtonPersistsAndRecreationDoesNotDuplicateTrip() {
        Context context = ApplicationProvider.getApplicationContext();
        Viagem trip = new Viagem(); trip.destination = "Teste " + java.util.UUID.randomUUID();
        trip.start = 1798804800000L; trip.end = trip.start;
        Intent intent = new Intent(context, ItineraryDraftActivity.class).putExtra("trip", trip);
        long[] id = {-1};
        try (ActivityScenario<ItineraryDraftActivity> scenario = ActivityScenario.launch(intent)) {
            scenario.onActivity(activity -> activity.findViewById(R.id.save_trip).performClick());
            try (TripDatabase db = new TripDatabase(context)) {
                for (Viagem saved : db.list()) if (saved.destination.equals(trip.destination)) id[0] = saved.id;
                assertTrue(id[0] > 0);
            }
            scenario.recreate();
            scenario.onActivity(activity -> activity.findViewById(R.id.save_trip).performClick());
            try (TripDatabase db = new TripDatabase(context)) {
                int count = 0; for (Viagem saved : db.list()) if (saved.destination.equals(trip.destination)) count++;
                assertEquals(1, count);
            }
        } finally { if (id[0] > 0) try (TripDatabase db = new TripDatabase(context)) { db.delete(id[0]); } }
    }

    @Test public void sameCurrencyConvertsWithoutNetwork() {
        try (ActivityScenario<CurrencyActivity> scenario = ActivityScenario.launch(CurrencyActivity.class)) {
            scenario.onActivity(activity -> {
                ((Spinner)activity.findViewById(R.id.currency_to)).setSelection(0);
                ((EditText)activity.findViewById(R.id.currency_amount)).setText("100,50");
            });
            scenario.onActivity(activity -> {
                activity.findViewById(R.id.convert).performClick();
                String result = ((TextView)activity.findViewById(R.id.conversion_result)).getText().toString();
                assertTrue(result.contains("100,50")); assertTrue(result.contains("taxa 1"));
            });
        }
    }
}

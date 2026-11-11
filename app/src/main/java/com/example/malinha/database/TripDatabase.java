package com.example.malinha.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import com.example.malinha.models.Atividade;
import com.example.malinha.models.Viagem;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.ArrayList;

/** A trip and its ordered activities are saved atomically in a single SQLite row.
 * JSON is the local representation of the variable-length lists, not a remote service. */
public class TripDatabase extends SQLiteOpenHelper {
    public TripDatabase(Context context) { this(context, "malinha.db"); }
    public TripDatabase(Context context, String name) { super(context.getApplicationContext(), name, null, 2); }
    @Override public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE trips (id INTEGER PRIMARY KEY AUTOINCREMENT, destination TEXT NOT NULL, start INTEGER NOT NULL, end INTEGER NOT NULL, budget TEXT NOT NULL, interests TEXT NOT NULL, activities TEXT NOT NULL, travelers INTEGER NOT NULL DEFAULT 1, pace INTEGER NOT NULL DEFAULT 1)");
    }
    @Override public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            db.execSQL("ALTER TABLE trips ADD COLUMN travelers INTEGER NOT NULL DEFAULT 1");
            db.execSQL("ALTER TABLE trips ADD COLUMN pace INTEGER NOT NULL DEFAULT 1");
        }
    }
    public long save(Viagem trip) {
        ContentValues values = new ContentValues();
        values.put("destination", trip.destination); values.put("start", trip.start);
        values.put("end", trip.end); values.put("budget", trip.budget);
        values.put("travelers", trip.travelers); values.put("pace", trip.pace);
        values.put("interests", new JSONArray(trip.interests).toString());
        JSONArray activities = new JSONArray();
        try {
            for (Atividade item : trip.activities) {
                JSONObject json = new JSONObject();
                json.put("id", item.id); json.put("day", item.day); json.put("name", item.name);
                json.put("start", item.startMinutes); json.put("end", item.endMinutes);
                json.put("description", item.description); json.put("cost", item.cost);
                json.put("category", item.category); activities.put(json);
            }
        } catch (JSONException error) { throw new IllegalStateException("Invalid activity", error); }
        values.put("activities", activities.toString());
        SQLiteDatabase db = getWritableDatabase();
        if (trip.id < 0) return db.insertOrThrow("trips", null, values);
        if (db.update("trips", values, "id = ?", new String[]{Long.toString(trip.id)}) != 1)
            throw new IllegalStateException("Trip no longer exists");
        return trip.id;
    }
    public ArrayList<Viagem> list() {
        ArrayList<Viagem> trips = new ArrayList<>();
        try (Cursor cursor = getReadableDatabase().query("trips", null, null, null, null, null, "start ASC, id DESC")) {
            while (cursor.moveToNext()) trips.add(read(cursor));
        }
        return trips;
    }
    public Viagem find(long id) {
        try (Cursor cursor = getReadableDatabase().query("trips", null, "id = ?", new String[]{Long.toString(id)}, null, null, null)) {
            return cursor.moveToFirst() ? read(cursor) : null;
        }
    }
    public void delete(long id) { getWritableDatabase().delete("trips", "id = ?", new String[]{Long.toString(id)}); }
    private static Viagem read(Cursor cursor) {
        Viagem trip = new Viagem();
        trip.id = cursor.getLong(cursor.getColumnIndexOrThrow("id"));
        trip.destination = cursor.getString(cursor.getColumnIndexOrThrow("destination"));
        trip.start = cursor.getLong(cursor.getColumnIndexOrThrow("start"));
        trip.end = cursor.getLong(cursor.getColumnIndexOrThrow("end"));
        trip.budget = cursor.getString(cursor.getColumnIndexOrThrow("budget"));
        trip.travelers = cursor.getInt(cursor.getColumnIndexOrThrow("travelers"));
        trip.pace = cursor.getInt(cursor.getColumnIndexOrThrow("pace"));
        try {
            JSONArray interests = new JSONArray(cursor.getString(cursor.getColumnIndexOrThrow("interests")));
            for (int i = 0; i < interests.length(); i++) trip.interests.add(interests.getString(i));
            JSONArray activities = new JSONArray(cursor.getString(cursor.getColumnIndexOrThrow("activities")));
            for (int i = 0; i < activities.length(); i++) {
                JSONObject json = activities.getJSONObject(i); Atividade item = new Atividade();
                item.id = json.getString("id"); item.day = json.getInt("day"); item.name = json.getString("name");
                item.startMinutes = json.getInt("start"); item.endMinutes = json.getInt("end");
                item.description = json.getString("description"); item.cost = json.getString("cost");
                item.category = json.getInt("category"); trip.activities.add(item);
            }
        } catch (JSONException error) { throw new IllegalStateException("Invalid saved trip", error); }
        return trip;
    }
}

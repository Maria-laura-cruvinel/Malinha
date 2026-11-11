package com.example.malinha.activities;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.text.method.PasswordTransformationMethod;
import android.util.AtomicFile;
import android.view.View;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import com.example.malinha.R;
import com.example.malinha.utils.AirportAutocomplete;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Calendar;
import java.util.Locale;
import java.util.UUID;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/** Private reservation notes. The file is deliberately excluded from Android backups. */
final class FlightReservations {
    private final FlightsActivity activity;
    private final AtomicFile storage;

    FlightReservations(FlightsActivity activity) {
        this.activity = activity;
        storage = new AtomicFile(new File(activity.getNoBackupFilesDir(), "flight-reservations.json"));
    }

    void show() {
        try {
            JSONArray rows = read();
            String[] labels = new String[rows.length()];
            for (int i = 0; i < labels.length; i++) {
                JSONObject row = rows.getJSONObject(i);
                labels[i] = activity.getString(R.string.flight_history_route, row.getString("origin"),
                        row.getString("destination"), row.getString("departure"));
            }
            AlertDialog.Builder dialog = new AlertDialog.Builder(activity)
                    .setTitle(R.string.flight_reservations)
                    .setPositiveButton(R.string.flight_add_reservation, (d, w) -> edit(new JSONObject()))
                    .setNegativeButton(R.string.close_dialog, null);
            if (labels.length == 0) dialog.setMessage(R.string.flight_no_reservations);
            else dialog.setItems(labels, (d, index) -> edit(rows.optJSONObject(index)));
            dialog.show();
        } catch (IOException | JSONException error) {
            showStorageError();
        }
    }

    private void edit(JSONObject row) {
        View form = activity.getLayoutInflater().inflate(R.layout.dialog_flight_reservation, null);
        EditText airline = form.findViewById(R.id.reservation_airline);
        EditText number = form.findViewById(R.id.reservation_number);
        AutoCompleteTextView origin = form.findViewById(R.id.reservation_origin);
        AutoCompleteTextView destination = form.findViewById(R.id.reservation_destination);
        EditText code = form.findViewById(R.id.reservation_code);
        EditText notes = form.findViewById(R.id.reservation_notes);
        AirportAutocomplete.bind(origin);
        AirportAutocomplete.bind(destination);
        boolean existing = row.has("id");
        airline.setText(row.optString("airline"));
        number.setText(row.optString("number"));
        origin.setText(row.optString("origin"), false);
        destination.setText(row.optString("destination"), false);
        code.setText(row.optString("code"));
        notes.setText(row.optString("notes"));
        CheckBox reveal = form.findViewById(R.id.reservation_reveal);
        reveal.setOnCheckedChangeListener((button, checked) -> {
            code.setTransformationMethod(checked ? null : PasswordTransformationMethod.getInstance());
            code.setSelection(code.length());
        });

        Button departure = form.findViewById(R.id.reservation_departure);
        Button arrival = form.findViewById(R.id.reservation_arrival);
        configureDate(departure, R.string.reservation_departure_label, row.optString("departure"));
        configureDate(arrival, R.string.reservation_arrival_label, row.optString("arrival"));

        AlertDialog.Builder builder = new AlertDialog.Builder(activity).setView(form)
                .setTitle(existing ? R.string.flight_edit_reservation : R.string.flight_add_reservation)
                .setNegativeButton(R.string.cancel_action, (d, w) -> show())
                .setPositiveButton(android.R.string.ok, null);
        if (existing) builder.setNeutralButton(R.string.delete_action, null);
        AlertDialog dialog = builder.create();
        dialog.show();
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String from = value(origin), to = value(destination);
            if (from.isEmpty() || to.isEmpty() || departure.getTag() == null) {
                Toast.makeText(activity, R.string.flight_reservation_required, Toast.LENGTH_LONG).show();
                return;
            }
            try {
                JSONObject updated = new JSONObject();
                updated.put("id", existing ? row.getString("id") : UUID.randomUUID().toString());
                updated.put("airline", value(airline)).put("number", value(number));
                updated.put("origin", from).put("destination", to);
                updated.put("departure", departure.getTag()).put("arrival", arrival.getTag());
                updated.put("code", value(code)).put("notes", value(notes));
                saveChange(updated.getString("id"), updated);
                dialog.dismiss();
                show();
            } catch (IOException | JSONException error) { showStorageError(); }
        });
        if (existing) dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener(v ->
                new AlertDialog.Builder(activity).setMessage(R.string.flight_reservation_delete)
                        .setNegativeButton(R.string.cancel_action, null)
                        .setPositiveButton(R.string.delete_action, (d, w) -> {
                            try {
                                saveChange(row.getString("id"), null);
                                dialog.dismiss();
                                show();
                            } catch (IOException | JSONException error) { showStorageError(); }
                        }).show());
    }

    private void configureDate(Button button, int label, String saved) {
        if (!saved.isEmpty()) showDate(button, label, saved);
        button.setOnClickListener(v -> {
            Calendar now = Calendar.getInstance();
            new DatePickerDialog(activity, (picker, year, month, day) ->
                new TimePickerDialog(activity, (time, hour, minute) -> {
                    String value = String.format(Locale.ROOT, "%02d/%02d/%04d %02d:%02d",
                            day, month + 1, year, hour, minute);
                    showDate(button, label, value);
                }, now.get(Calendar.HOUR_OF_DAY), now.get(Calendar.MINUTE), true).show(),
                now.get(Calendar.YEAR), now.get(Calendar.MONTH), now.get(Calendar.DAY_OF_MONTH)).show();
        });
    }

    private void showDate(Button button, int label, String value) {
        button.setTag(value);
        button.setText(activity.getString(R.string.flight_date_label, activity.getString(label), value));
    }

    private static String value(EditText field) { return field.getText().toString().trim(); }

    private JSONArray read() throws IOException, JSONException {
        try { return new JSONArray(new String(storage.readFully(), StandardCharsets.UTF_8)); }
        catch (FileNotFoundException missing) { return new JSONArray(); }
    }

    private void saveChange(String id, JSONObject replacement) throws IOException, JSONException {
        JSONArray current = read();
        JSONArray updated = new JSONArray();
        boolean found = false;
        for (int i = 0; i < current.length(); i++) {
            JSONObject row = current.getJSONObject(i);
            if (!id.equals(row.getString("id"))) updated.put(row);
            else {
                found = true;
                if (replacement != null) updated.put(replacement);
            }
        }
        if (!found && replacement != null) updated.put(replacement);
        FileOutputStream stream = storage.startWrite();
        try {
            stream.write(updated.toString().getBytes(StandardCharsets.UTF_8));
            storage.finishWrite(stream);
        } catch (IOException | RuntimeException error) {
            storage.failWrite(stream);
            throw error;
        }
    }

    private void showStorageError() {
        Toast.makeText(activity, R.string.storage_error, Toast.LENGTH_LONG).show();
    }
}

package com.example.malinha.utils;

import com.example.malinha.models.Atividade;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Locale;

public final class ItineraryUtils {
    private ItineraryUtils() {}

    public static String time(int minutes) {
        return String.format(Locale.ROOT, "%02d:%02d", minutes / 60, minutes % 60);
    }

    /** Calendar arithmetic preserves local dates across daylight-saving transitions. */
    public static ArrayList<Long> days(long start, long end) {
        ArrayList<Long> dates = new ArrayList<>();
        Calendar date = Calendar.getInstance();
        date.setTimeInMillis(start);
        while (date.getTimeInMillis() <= end) {
            dates.add(date.getTimeInMillis());
            date.add(Calendar.DAY_OF_MONTH, 1);
        }
        return dates;
    }

    public static ArrayList<Atividade> forDay(ArrayList<Atividade> all, int day) {
        ArrayList<Atividade> result = new ArrayList<>();
        for (Atividade activity : all) if (activity.day == day) result.add(activity);
        return result;
    }

    /** Move only within the selected day, leaving every other day untouched. */
    public static boolean move(ArrayList<Atividade> all, String id, int direction) {
        if (direction != -1 && direction != 1) return false;
        for (int i = 0; i < all.size(); i++) {
            if (!all.get(i).id.equals(id)) continue;
            for (int j = i + direction; j >= 0 && j < all.size(); j += direction) {
                if (all.get(j).day == all.get(i).day) {
                    Collections.swap(all, i, j);
                    return true;
                }
            }
            return false;
        }
        return false;
    }
}

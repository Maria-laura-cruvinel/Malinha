package com.example.malinha.utils;

import com.example.malinha.models.CountryTravelData.Holiday;
import com.example.malinha.models.CountryTravelData.LongWeekend;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TimeZone;

/** Local suggestions under an explicit Saturday/Sunday assumption, not official days off. */
public final class HolidayWeekends {
    private HolidayWeekends() {}
    public static ArrayList<LongWeekend> calculate(List<Holiday> holidays, int year) {
        Set<String> publicDates = new HashSet<>();
        for (Holiday holiday : holidays) if (holiday.national && holiday.types.contains("Public")) publicDates.add(holiday.date);
        SimpleDateFormat dates = new SimpleDateFormat("yyyy-MM-dd", Locale.ROOT);
        TimeZone utc = TimeZone.getTimeZone("UTC"); dates.setTimeZone(utc);
        Calendar day = Calendar.getInstance(utc, Locale.ROOT); day.clear(); day.set(year, Calendar.JANUARY, 1);
        ArrayList<LongWeekend> result = new ArrayList<>();
        LongWeekend run = null; boolean hasHoliday = false;
        while (day.get(Calendar.YEAR) == year) {
            String date = dates.format(day.getTime());
            int weekday = day.get(Calendar.DAY_OF_WEEK);
            boolean holiday = publicDates.contains(date);
            boolean free = holiday || weekday == Calendar.SATURDAY || weekday == Calendar.SUNDAY;
            if (free) {
                if (run == null) { run = new LongWeekend(); run.start = date; }
                run.end = date; run.days++; hasHoliday |= holiday;
            } else {
                if (run != null && run.days >= 3 && hasHoliday) result.add(run);
                run = null; hasHoliday = false;
            }
            day.add(Calendar.DAY_OF_MONTH, 1);
        }
        if (run != null && run.days >= 3 && hasHoliday) result.add(run);
        return result;
    }
}

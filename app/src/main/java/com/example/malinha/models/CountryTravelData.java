package com.example.malinha.models;

import java.util.ArrayList;

public final class CountryTravelData {
    public String name = "";
    public final ArrayList<String> languages = new ArrayList<>();
    public final ArrayList<String> currencies = new ArrayList<>();
    public final ArrayList<String> timezones = new ArrayList<>();
    public final ArrayList<String> callingCodes = new ArrayList<>();

    public static final class Holiday {
        public String date, name;
        public boolean national;
        public final ArrayList<String> subdivisions = new ArrayList<>();
        public final ArrayList<String> types = new ArrayList<>();
    }
    public static final class LongWeekend {
        public String start, end;
        public int days;
    }
}

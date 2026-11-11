package com.example.malinha.models;

/** Coordinates selected from Places, independent of the editable destination text. */
public final class SelectedCity {
    public final String id, name;
    public final double latitude, longitude;

    public SelectedCity(String id, String name, double latitude, double longitude) {
        this.id = id; this.name = name; this.latitude = latitude; this.longitude = longitude;
    }
}

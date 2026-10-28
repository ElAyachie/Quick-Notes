package com.myapp.quicknotes.data;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

import java.util.Objects;

// A place the user saved under a name of their own, such as Home or Work, so a location reminder
// can be set there with one tap. Only the spot is kept: how close counts as being there belongs to
// each reminder.
@Entity(tableName = "saved_places")
public class SavedPlace {
    @PrimaryKey(autoGenerate = true)
    private final long id;
    @NonNull
    private final String name;
    private final double latitude;
    private final double longitude;

    public SavedPlace(long id, @NonNull String name, double latitude, double longitude) {
        this.id = id;
        this.name = name;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    @Ignore
    public SavedPlace(@NonNull String name, double latitude, double longitude) {
        this(0, name, latitude, longitude);
    }

    public SavedPlace renamed(@NonNull String newName) {
        return new SavedPlace(id, newName, latitude, longitude);
    }

    public long getId() {
        return id;
    }

    @NonNull
    public String getName() {
        return name;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SavedPlace)) return false;
        SavedPlace other = (SavedPlace) o;
        return id == other.id && name.equals(other.name) && latitude == other.latitude
                && longitude == other.longitude;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, latitude, longitude);
    }
}

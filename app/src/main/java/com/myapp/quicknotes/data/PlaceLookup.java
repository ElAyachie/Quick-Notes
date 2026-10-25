package com.myapp.quicknotes.data;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import java.util.Locale;

// A circle on the map whose surroundings have been looked up (see NearbyPlace). Kept by place, not
// by reminder: a repeating reminder continues as new rows at the same place, and two notes can
// share one. A lookup that found nothing still counts, so it isn't repeated.
@Entity(tableName = "place_lookups")
public class PlaceLookup {
    @PrimaryKey
    @ColumnInfo(name = "place_key")
    @NonNull
    private final String placeKey;
    @ColumnInfo(name = "looked_up_at")
    private final long lookedUpAt;

    public PlaceLookup(@NonNull String placeKey, long lookedUpAt) {
        this.placeKey = placeKey;
        this.lookedUpAt = lookedUpAt;
    }

    // Identifies a circle: its centre to about a metre, and its radius.
    @NonNull
    public static String keyOf(double latitude, double longitude, float radiusMeters) {
        return String.format(Locale.US, "%.5f,%.5f,%.0f", latitude, longitude, radiusMeters);
    }

    @NonNull
    public String getPlaceKey() {
        return placeKey;
    }

    public long getLookedUpAt() {
        return lookedUpAt;
    }
}

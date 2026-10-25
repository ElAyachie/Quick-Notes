package com.myapp.quicknotes.data;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import java.util.Objects;

// Something with a name near a location reminder's place: a shop, a school, a landmark, a street,
// the neighbourhood. Looked up from OpenStreetMap when the reminder is set, so a search for
// "Walgreens" or "pharmacy" finds a reminder whose place the user never named.
@Entity(
        tableName = "nearby_places",
        foreignKeys = @ForeignKey(
                entity = PlaceLookup.class,
                parentColumns = "place_key",
                childColumns = "place_key",
                onDelete = ForeignKey.CASCADE),
        indices = @Index("place_key"))
public class NearbyPlace {
    @PrimaryKey(autoGenerate = true)
    private final long id;
    @ColumnInfo(name = "place_key")
    @NonNull
    private final String placeKey;
    @NonNull
    private final String name;
    // What it is, in everyday words: "pharmacy", "school", "street", "neighbourhood".
    @NonNull
    private final String kind;
    // From the centre of the reminder's circle; 0 for an area the centre lies in.
    @ColumnInfo(name = "distance_meters")
    private final int distanceMeters;

    public NearbyPlace(long id, @NonNull String placeKey, @NonNull String name,
                       @NonNull String kind, int distanceMeters) {
        this.id = id;
        this.placeKey = placeKey;
        this.name = name;
        this.kind = kind;
        this.distanceMeters = distanceMeters;
    }

    public long getId() {
        return id;
    }

    @NonNull
    public String getPlaceKey() {
        return placeKey;
    }

    @NonNull
    public String getName() {
        return name;
    }

    @NonNull
    public String getKind() {
        return kind;
    }

    public int getDistanceMeters() {
        return distanceMeters;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof NearbyPlace)) return false;
        NearbyPlace other = (NearbyPlace) o;
        return id == other.id
                && distanceMeters == other.distanceMeters
                && placeKey.equals(other.placeKey)
                && name.equals(other.name)
                && kind.equals(other.kind);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, placeKey, name, kind, distanceMeters);
    }
}

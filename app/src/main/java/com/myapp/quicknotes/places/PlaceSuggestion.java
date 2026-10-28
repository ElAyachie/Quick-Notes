package com.myapp.quicknotes.places;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Objects;

// One place offered in the place picker: a search result, or one of the user's saved or recently
// used places.
public final class PlaceSuggestion {
    public enum Kind {
        // A place the user saved by name (data.SavedPlace).
        SAVED,
        // The place of a recent location reminder.
        RECENT,
        // A result of searching by name or address.
        FOUND,
    }

    @NonNull
    private final Kind kind;
    @NonNull
    private final String name;
    // The address or area under the name, when known.
    @Nullable
    private final String address;
    // Google sends a place's id with the suggestion and its coordinates only when asked for them
    // (PlaceSearchRepository.locate). Other suggestions come with coordinates.
    @Nullable
    private final String googlePlaceId;
    @Nullable
    private final Double latitude;
    @Nullable
    private final Double longitude;
    // From the user's position, when known.
    @Nullable
    private final Integer distanceMeters;
    // The saved place's id, for a SAVED suggestion; otherwise 0.
    private final long savedPlaceId;

    private PlaceSuggestion(@NonNull Kind kind, @NonNull String name, @Nullable String address,
                            @Nullable String googlePlaceId, @Nullable Double latitude,
                            @Nullable Double longitude, @Nullable Integer distanceMeters,
                            long savedPlaceId) {
        this.kind = kind;
        this.name = name;
        this.address = address;
        this.googlePlaceId = googlePlaceId;
        this.latitude = latitude;
        this.longitude = longitude;
        this.distanceMeters = distanceMeters;
        this.savedPlaceId = savedPlaceId;
    }

    // A Google result, to be located when chosen.
    public static PlaceSuggestion foundByGoogle(@NonNull String placeId, @NonNull String name,
                                                @Nullable String address,
                                                @Nullable Integer distanceMeters) {
        return new PlaceSuggestion(Kind.FOUND, name, address, placeId, null, null,
                distanceMeters, 0);
    }

    // A result that came with its coordinates.
    public static PlaceSuggestion found(@NonNull String name, @Nullable String address,
                                        double latitude, double longitude,
                                        @Nullable Integer distanceMeters) {
        return new PlaceSuggestion(Kind.FOUND, name, address, null, latitude, longitude,
                distanceMeters, 0);
    }

    public static PlaceSuggestion saved(long savedPlaceId, @NonNull String name, double latitude,
                                        double longitude, @Nullable Integer distanceMeters) {
        return new PlaceSuggestion(Kind.SAVED, name, null, null, latitude, longitude,
                distanceMeters, savedPlaceId);
    }

    public static PlaceSuggestion recent(@NonNull String name, double latitude, double longitude,
                                         @Nullable Integer distanceMeters) {
        return new PlaceSuggestion(Kind.RECENT, name, null, null, latitude, longitude,
                distanceMeters, 0);
    }

    public PlaceSuggestion withDistance(@Nullable Integer meters) {
        return new PlaceSuggestion(kind, name, address, googlePlaceId, latitude, longitude, meters,
                savedPlaceId);
    }

    @NonNull
    public Kind getKind() {
        return kind;
    }

    @NonNull
    public String getName() {
        return name;
    }

    @Nullable
    public String getAddress() {
        return address;
    }

    @Nullable
    public String getGooglePlaceId() {
        return googlePlaceId;
    }

    public boolean hasLocation() {
        return latitude != null && longitude != null;
    }

    @Nullable
    public Double getLatitude() {
        return latitude;
    }

    @Nullable
    public Double getLongitude() {
        return longitude;
    }

    @Nullable
    public Integer getDistanceMeters() {
        return distanceMeters;
    }

    public long getSavedPlaceId() {
        return savedPlaceId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PlaceSuggestion)) return false;
        PlaceSuggestion other = (PlaceSuggestion) o;
        return kind == other.kind && savedPlaceId == other.savedPlaceId
                && name.equals(other.name) && Objects.equals(address, other.address)
                && Objects.equals(googlePlaceId, other.googlePlaceId)
                && Objects.equals(latitude, other.latitude)
                && Objects.equals(longitude, other.longitude)
                && Objects.equals(distanceMeters, other.distanceMeters);
    }

    @Override
    public int hashCode() {
        return Objects.hash(kind, name, address, googlePlaceId, latitude, longitude,
                distanceMeters, savedPlaceId);
    }

    @NonNull
    @Override
    public String toString() {
        return kind + " " + name + (address != null ? ", " + address : "");
    }
}

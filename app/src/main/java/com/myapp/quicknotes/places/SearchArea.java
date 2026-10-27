package com.myapp.quicknotes.places;

import androidx.annotation.Nullable;

// Where a place search looks first, and where distances are measured from. Results inside the
// circle are preferred, not required, so a well-known place further away is still found.
public final class SearchArea {
    // Google takes no larger circle.
    public static final double MAX_RADIUS_METERS = 50_000;

    @Nullable
    private final Double latitude;
    @Nullable
    private final Double longitude;
    private final double radiusMeters;
    @Nullable
    private final Double originLatitude;
    @Nullable
    private final Double originLongitude;

    // The circle is usually what the map shows, and null when the map shows half the world; the
    // origin is the user's own position, or null when it isn't known.
    public SearchArea(@Nullable Double latitude, @Nullable Double longitude, double radiusMeters,
                      @Nullable Double originLatitude, @Nullable Double originLongitude) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.radiusMeters = Math.min(radiusMeters, MAX_RADIUS_METERS);
        this.originLatitude = originLatitude;
        this.originLongitude = originLongitude;
    }

    public boolean hasCenter() {
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

    public double getRadiusMeters() {
        return radiusMeters;
    }

    public boolean hasOrigin() {
        return originLatitude != null && originLongitude != null;
    }

    @Nullable
    public Double getOriginLatitude() {
        return originLatitude;
    }

    @Nullable
    public Double getOriginLongitude() {
        return originLongitude;
    }

    // Metres from the origin to a place, or null when the origin isn't known.
    @Nullable
    public Integer distanceTo(double latitude, double longitude) {
        if (!hasOrigin()) {
            return null;
        }
        return (int) Math.round(OverpassParser.distanceMeters(
                originLatitude, originLongitude, latitude, longitude));
    }
}

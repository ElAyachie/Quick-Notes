package com.myapp.quicknotes.data;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;

// A named place a location reminder was set at (ReminderDao.getRecentPlaces).
public class RecentPlace {
    @NonNull
    private final String name;
    private final double latitude;
    private final double longitude;
    // The newest reminder there, which orders the places.
    @ColumnInfo(name = "last_reminder_id")
    private final long lastReminderId;

    public RecentPlace(@NonNull String name, double latitude, double longitude,
                       long lastReminderId) {
        this.name = name;
        this.latitude = latitude;
        this.longitude = longitude;
        this.lastReminderId = lastReminderId;
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

    public long getLastReminderId() {
        return lastReminderId;
    }
}

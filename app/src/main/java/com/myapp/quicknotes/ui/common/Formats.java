package com.myapp.quicknotes.ui.common;

import android.content.Context;

import com.myapp.quicknotes.R;

import java.text.DateFormat;
import java.text.DecimalFormat;
import java.util.Date;

// Dates and times as shown to the user, following the device's locale.
public final class Formats {
    private Formats() {
    }

    public static String date(long millis) {
        return DateFormat.getDateInstance(DateFormat.MEDIUM).format(new Date(millis));
    }

    public static String dateTime(long millis) {
        return DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)
                .format(new Date(millis));
    }

    public static String time(long millis) {
        return DateFormat.getTimeInstance(DateFormat.SHORT).format(new Date(millis));
    }

    // "250 m" below a kilometre, "1.5 km" from there on.
    public static String distance(Context context, float meters) {
        if (meters < 1000) {
            return context.getString(R.string.distance_meters, Math.round(meters));
        }
        return context.getString(R.string.distance_kilometers,
                new DecimalFormat("0.#").format(meters / 1000));
    }
}

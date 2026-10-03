package com.myapp.quicknotes.ui.common;

import java.text.DateFormat;
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
}

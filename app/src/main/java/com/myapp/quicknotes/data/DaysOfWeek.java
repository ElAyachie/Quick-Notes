package com.myapp.quicknotes.data;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

// A set of weekdays packed into one number, the way it is stored with a reminder: Monday is the
// lowest bit, Sunday the seventh.
public final class DaysOfWeek {
    public static final int NONE = 0;
    public static final int EVERY_DAY = 0b1111111;

    private DaysOfWeek() {
    }

    public static int of(DayOfWeek... days) {
        int set = NONE;
        for (DayOfWeek day : days) {
            set = with(set, day, true);
        }
        return set;
    }

    public static boolean contains(int days, DayOfWeek day) {
        return (days & bit(day)) != 0;
    }

    // The same set with one day added or removed.
    public static int with(int days, DayOfWeek day, boolean included) {
        return included ? days | bit(day) : days & ~bit(day);
    }

    // The first moment after `after` that falls on one of the days, at the time of day of
    // `timeOfDaySource`. The time of day is read in local time, so a 9:00 reminder stays at 9:00
    // across a clock change.
    public static long nextAfter(int days, long timeOfDaySource, long after, ZoneId zone) {
        if ((days & EVERY_DAY) == NONE) {
            throw new IllegalArgumentException("No day of the week chosen");
        }
        LocalTime timeOfDay = Instant.ofEpochMilli(timeOfDaySource).atZone(zone).toLocalTime();
        LocalDate date = Instant.ofEpochMilli(after).atZone(zone).toLocalDate();
        // Today may still be ahead; otherwise one of the next seven days is.
        while (true) {
            if (contains(days, date.getDayOfWeek())) {
                long candidate = ZonedDateTime.of(date, timeOfDay, zone).toInstant().toEpochMilli();
                if (candidate > after) {
                    return candidate;
                }
            }
            date = date.plusDays(1);
        }
    }

    // The given moment if it falls on one of the days, otherwise the same time of day on the next
    // day that does.
    public static long firstOnOrAfter(int days, long moment, ZoneId zone) {
        return nextAfter(days, moment, moment - 1, zone);
    }

    private static int bit(DayOfWeek day) {
        return 1 << (day.getValue() - 1);
    }
}

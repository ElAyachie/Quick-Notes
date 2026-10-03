package com.myapp.quicknotes.data;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;

// Whether a reminder comes back after it fires, and how often.
public enum Repeat {
    NONE(null),
    // For time reminders.
    DAILY(ChronoUnit.DAYS),
    WEEKLY(ChronoUnit.WEEKS),
    MONTHLY(ChronoUnit.MONTHS),
    YEARLY(ChronoUnit.YEARS),
    // For location reminders: again on every later arrival at the place.
    EVERY_ARRIVAL(null);

    private final ChronoUnit period;

    Repeat(ChronoUnit period) {
        this.period = period;
    }

    // True for the kinds that repeat on the calendar.
    public boolean isTimeBased() {
        return period != null;
    }

    // The next moment a time reminder is due: the first step of the series that lies after both
    // the occurrence that just fired and the present. Stepping from the first occurrence rather
    // than the previous one keeps "monthly on the 31st" on the 31st after passing through a
    // shorter month. Occurrences missed while the device was off are skipped, not replayed.
    //
    // Steps are taken in local time, so a daily 9:00 reminder stays at 9:00 across a clock change.
    public long nextAfter(long firstTriggerAt, long previousTriggerAt, long now, ZoneId zone) {
        if (period == null) {
            throw new IllegalStateException(this + " does not repeat on the calendar");
        }
        ZonedDateTime first = Instant.ofEpochMilli(firstTriggerAt).atZone(zone);
        long after = Math.max(previousTriggerAt, now);
        // Jump close to the answer, then walk the last few steps.
        long steps = Math.max(1, period.between(first, Instant.ofEpochMilli(after).atZone(zone)));
        while (true) {
            long candidate = first.plus(steps, period).toInstant().toEpochMilli();
            if (candidate > after) {
                return candidate;
            }
            steps++;
        }
    }
}

package com.myapp.quicknotes.ui.common;

import androidx.annotation.Nullable;

import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;

// The ready-made times offered when a time reminder is set, so the common cases take one tap
// instead of the date and time pickers.
public enum ReminderPreset {
    // Two to three hours from now, on the hour.
    LATER_TODAY,
    // 9:00 tomorrow.
    TOMORROW_MORNING,
    // 9:00 a week from today.
    NEXT_WEEK;

    private static final int LATER_TODAY_HOURS = 3;
    private static final LocalTime MORNING = LocalTime.of(9, 0);

    // When a reminder set from this preset at `now` fires, in milliseconds since the epoch. Null
    // when the preset isn't on offer: "later today" late in the evening, when it would fall on
    // tomorrow.
    @Nullable
    public Long timeFor(long now, ZoneId zone) {
        ZonedDateTime current = Instant.ofEpochMilli(now).atZone(zone);
        ZonedDateTime time;
        switch (this) {
            case LATER_TODAY:
                time = current.plusHours(LATER_TODAY_HOURS).truncatedTo(ChronoUnit.HOURS);
                if (!time.toLocalDate().equals(current.toLocalDate())) {
                    return null;
                }
                break;
            case TOMORROW_MORNING:
                time = current.toLocalDate().plusDays(1).atTime(MORNING).atZone(zone);
                break;
            default:
                time = current.toLocalDate().plusWeeks(1).atTime(MORNING).atZone(zone);
                break;
        }
        return time.toInstant().toEpochMilli();
    }
}

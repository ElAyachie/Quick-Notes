package com.myapp.quicknotes.ui.common;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

import java.time.ZoneId;
import java.time.ZonedDateTime;

public class ReminderPresetTest {
    private static final ZoneId NEW_YORK = ZoneId.of("America/New_York");

    private static long at(int year, int month, int day, int hour, int minute) {
        return ZonedDateTime.of(year, month, day, hour, minute, 0, 0, NEW_YORK)
                .toInstant().toEpochMilli();
    }

    @Test
    public void laterTodayIsOnTheHourTwoToThreeHoursAhead() {
        long now = at(2026, 10, 3, 14, 40);

        assertEquals(Long.valueOf(at(2026, 10, 3, 17, 0)),
                ReminderPreset.LATER_TODAY.timeFor(now, NEW_YORK));
    }

    @Test
    public void laterTodayOnTheHourIsThreeHoursAhead() {
        long now = at(2026, 10, 3, 14, 0);

        assertEquals(Long.valueOf(at(2026, 10, 3, 17, 0)),
                ReminderPreset.LATER_TODAY.timeFor(now, NEW_YORK));
    }

    @Test
    public void laterTodayIsStillOfferedWhenItFallsInTheLastHourOfTheDay() {
        long now = at(2026, 10, 3, 20, 59);

        assertEquals(Long.valueOf(at(2026, 10, 3, 23, 0)),
                ReminderPreset.LATER_TODAY.timeFor(now, NEW_YORK));
    }

    @Test
    public void laterTodayIsNotOfferedWhenItWouldBeTomorrow() {
        long now = at(2026, 10, 3, 21, 0);

        assertNull(ReminderPreset.LATER_TODAY.timeFor(now, NEW_YORK));
    }

    @Test
    public void tomorrowMorningIsNineTheNextDay() {
        long now = at(2026, 10, 3, 23, 30);

        assertEquals(Long.valueOf(at(2026, 10, 4, 9, 0)),
                ReminderPreset.TOMORROW_MORNING.timeFor(now, NEW_YORK));
    }

    @Test
    public void tomorrowMorningJustAfterMidnightIsStillTheNextDay() {
        long now = at(2026, 10, 3, 0, 5);

        assertEquals(Long.valueOf(at(2026, 10, 4, 9, 0)),
                ReminderPreset.TOMORROW_MORNING.timeFor(now, NEW_YORK));
    }

    @Test
    public void tomorrowMorningIsNineLocalTimeAcrossAClockChange() {
        // Clocks go back one hour on 1 November 2026.
        long now = at(2026, 10, 31, 15, 0);

        assertEquals(Long.valueOf(at(2026, 11, 1, 9, 0)),
                ReminderPreset.TOMORROW_MORNING.timeFor(now, NEW_YORK));
    }

    @Test
    public void nextWeekIsNineOnTheSameWeekday() {
        long now = at(2026, 10, 3, 14, 40);

        assertEquals(Long.valueOf(at(2026, 10, 10, 9, 0)),
                ReminderPreset.NEXT_WEEK.timeFor(now, NEW_YORK));
    }

    @Test
    public void everyOfferedTimeIsInTheFuture() {
        for (int hour = 0; hour < 24; hour++) {
            long now = at(2026, 10, 3, hour, 59);
            for (ReminderPreset preset : ReminderPreset.values()) {
                Long time = preset.timeFor(now, NEW_YORK);
                if (time != null && time <= now) {
                    throw new AssertionError(preset + " at hour " + hour + " is not in the future");
                }
            }
        }
    }
}

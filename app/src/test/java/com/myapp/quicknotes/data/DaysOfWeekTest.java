package com.myapp.quicknotes.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.time.DayOfWeek;
import java.time.ZoneId;
import java.time.ZonedDateTime;

public class DaysOfWeekTest {
    private static final ZoneId NEW_YORK = ZoneId.of("America/New_York");
    private static final int MON_WED_FRI =
            DaysOfWeek.of(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY);

    // 5 October 2026 is a Monday.
    private static long at(int year, int month, int day, int hour, int minute) {
        return ZonedDateTime.of(year, month, day, hour, minute, 0, 0, NEW_YORK)
                .toInstant().toEpochMilli();
    }

    @Test
    public void setRemembersWhichDaysWereAddedAndRemoved() {
        int days = DaysOfWeek.with(MON_WED_FRI, DayOfWeek.WEDNESDAY, false);

        assertTrue(DaysOfWeek.contains(days, DayOfWeek.MONDAY));
        assertFalse(DaysOfWeek.contains(days, DayOfWeek.WEDNESDAY));
        assertTrue(DaysOfWeek.contains(days, DayOfWeek.FRIDAY));
        assertFalse(DaysOfWeek.contains(days, DayOfWeek.SUNDAY));
    }

    @Test
    public void allSevenDaysMakeEveryDay() {
        assertEquals(DaysOfWeek.EVERY_DAY, DaysOfWeek.of(DayOfWeek.values()));
    }

    @Test
    public void afterMondayComesWednesdayAtTheSameTime() {
        long monday = at(2026, 10, 5, 9, 0);

        long next = DaysOfWeek.nextAfter(MON_WED_FRI, monday, monday, NEW_YORK);

        assertEquals(at(2026, 10, 7, 9, 0), next);
    }

    @Test
    public void afterFridayComesMondayOfTheNextWeek() {
        long monday = at(2026, 10, 5, 9, 0);
        long friday = at(2026, 10, 9, 9, 0);

        long next = DaysOfWeek.nextAfter(MON_WED_FRI, monday, friday, NEW_YORK);

        assertEquals(at(2026, 10, 12, 9, 0), next);
    }

    @Test
    public void aSingleDayComesBackAWeekLater() {
        long monday = at(2026, 10, 5, 9, 0);

        long next = DaysOfWeek.nextAfter(DaysOfWeek.of(DayOfWeek.MONDAY), monday, monday, NEW_YORK);

        assertEquals(at(2026, 10, 12, 9, 0), next);
    }

    @Test
    public void laterTheSameDayStillCountsAsToday() {
        long timeOfDay = at(2026, 10, 5, 18, 30);
        long mondayMorning = at(2026, 10, 5, 7, 0);

        long next = DaysOfWeek.nextAfter(MON_WED_FRI, timeOfDay, mondayMorning, NEW_YORK);

        assertEquals(at(2026, 10, 5, 18, 30), next);
    }

    @Test
    public void timeOfDayIsKeptAcrossAClockChange() {
        // Clocks go back on Sunday 1 November 2026.
        int weekend = DaysOfWeek.of(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY, DayOfWeek.MONDAY);
        long saturday = at(2026, 10, 31, 9, 0);

        long sunday = DaysOfWeek.nextAfter(weekend, saturday, saturday, NEW_YORK);
        long monday = DaysOfWeek.nextAfter(weekend, saturday, sunday, NEW_YORK);

        assertEquals(at(2026, 11, 1, 9, 0), sunday);
        assertEquals(at(2026, 11, 2, 9, 0), monday);
    }

    @Test
    public void firstOccurrenceIsTheChosenMomentWhenItsDayIsIncluded() {
        long monday = at(2026, 10, 5, 9, 0);

        assertEquals(monday, DaysOfWeek.firstOnOrAfter(MON_WED_FRI, monday, NEW_YORK));
    }

    @Test
    public void firstOccurrenceMovesToTheNextIncludedDay() {
        long tuesday = at(2026, 10, 6, 9, 0);

        assertEquals(at(2026, 10, 7, 9, 0),
                DaysOfWeek.firstOnOrAfter(MON_WED_FRI, tuesday, NEW_YORK));
    }

    @Test(expected = IllegalArgumentException.class)
    public void anEmptySetHasNoNextDay() {
        DaysOfWeek.nextAfter(DaysOfWeek.NONE, 0, 0, NEW_YORK);
    }

    @Test
    public void reminderOnChosenDaysMovesToTheNextChosenDayWhenItFires() {
        long monday = at(2026, 10, 5, 9, 0);
        Reminder reminder = Reminder.atTime(3, monday, Repeat.DAYS_OF_WEEK, MON_WED_FRI).withId(8);

        Reminder next = reminder.nextOccurrence(monday + 1_000, NEW_YORK);

        assertNotNull(next);
        assertEquals(Long.valueOf(at(2026, 10, 7, 9, 0)), next.getTriggerAt());
        assertEquals(Repeat.DAYS_OF_WEEK, next.getRepeat());
        assertEquals(MON_WED_FRI, next.getRepeatDays());
    }

    @Test
    public void chosenDaysMissedWhileTheDeviceWasOffAreSkipped() {
        long monday = at(2026, 10, 5, 9, 0);
        long thursdayAfternoon = at(2026, 10, 8, 15, 0);
        Reminder reminder = Reminder.atTime(3, monday, Repeat.DAYS_OF_WEEK, MON_WED_FRI);

        Reminder next = reminder.nextOccurrence(thursdayAfternoon, NEW_YORK);

        assertNotNull(next);
        assertEquals(Long.valueOf(at(2026, 10, 9, 9, 0)), next.getTriggerAt());
    }
}

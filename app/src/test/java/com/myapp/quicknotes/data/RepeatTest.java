package com.myapp.quicknotes.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.junit.Test;

import java.time.ZoneId;
import java.time.ZonedDateTime;

public class RepeatTest {
    private static final ZoneId NEW_YORK = ZoneId.of("America/New_York");

    private static long at(int year, int month, int day, int hour, int minute) {
        return ZonedDateTime.of(year, month, day, hour, minute, 0, 0, NEW_YORK)
                .toInstant().toEpochMilli();
    }

    @Test
    public void dailyMovesToTheSameTimeNextDay() {
        long first = at(2026, 10, 3, 9, 0);

        long next = Repeat.DAILY.nextAfter(first, first, first + 1_000, NEW_YORK);

        assertEquals(at(2026, 10, 4, 9, 0), next);
    }

    @Test
    public void dailyKeepsItsLocalTimeAcrossAClockChange() {
        // Clocks go back one hour on 1 November 2026, making that day 25 hours long.
        long first = at(2026, 10, 31, 9, 0);

        long next = Repeat.DAILY.nextAfter(first, first, first + 1_000, NEW_YORK);

        assertEquals(at(2026, 11, 1, 9, 0), next);
    }

    @Test
    public void weeklyMovesSevenDays() {
        long first = at(2026, 10, 3, 18, 30);

        long next = Repeat.WEEKLY.nextAfter(first, first, first + 1_000, NEW_YORK);

        assertEquals(at(2026, 10, 10, 18, 30), next);
    }

    @Test
    public void monthlyOnThe31stReturnsToThe31stAfterAShorterMonth() {
        long first = at(2026, 1, 31, 8, 0);
        long february = Repeat.MONTHLY.nextAfter(first, first, first + 1_000, NEW_YORK);
        assertEquals(at(2026, 2, 28, 8, 0), february);

        long march = Repeat.MONTHLY.nextAfter(first, february, february + 1_000, NEW_YORK);

        assertEquals(at(2026, 3, 31, 8, 0), march);
    }

    @Test
    public void yearlyOnLeapDayFallsBackToFebruary28InOtherYears() {
        long first = at(2028, 2, 29, 12, 0);

        long next = Repeat.YEARLY.nextAfter(first, first, first + 1_000, NEW_YORK);

        assertEquals(at(2029, 2, 28, 12, 0), next);
    }

    @Test
    public void occurrencesMissedWhileTheDeviceWasOffAreSkipped() {
        long first = at(2026, 10, 3, 9, 0);
        long tenDaysLater = at(2026, 10, 13, 15, 0);

        long next = Repeat.DAILY.nextAfter(first, first, tenDaysLater, NEW_YORK);

        assertEquals(at(2026, 10, 14, 9, 0), next);
    }

    @Test
    public void aReminderFiredEarlyDoesNotRepeatTheSameOccurrence() {
        // An inexact alarm can go off slightly before its time.
        long first = at(2026, 10, 3, 9, 0);

        long next = Repeat.DAILY.nextAfter(first, first, first - 30_000, NEW_YORK);

        assertEquals(at(2026, 10, 4, 9, 0), next);
    }

    @Test
    public void nonRepeatingReminderHasNoNextOccurrence() {
        Reminder once = Reminder.atTime(1, at(2026, 10, 3, 9, 0), Repeat.NONE);

        assertNull(once.nextOccurrence(at(2026, 10, 3, 9, 0), NEW_YORK));
    }

    @Test
    public void nextOccurrenceOfATimeReminderIsANewUnfiredReminderInTheSameSeries() {
        long first = at(2026, 10, 3, 9, 0);
        Reminder weekly = Reminder.atTime(5, first, Repeat.WEEKLY).withId(12);

        Reminder next = weekly.nextOccurrence(first + 1_000, NEW_YORK);

        assertNotNull(next);
        assertEquals(0, next.getId());
        assertEquals(5, next.getNoteId());
        assertEquals(Repeat.WEEKLY, next.getRepeat());
        assertEquals(Long.valueOf(at(2026, 10, 10, 9, 0)), next.getTriggerAt());
        assertEquals(Long.valueOf(first), next.getFirstTriggerAt());
        assertNull(next.getFiredAt());
    }

    @Test
    public void nextOccurrenceOfALocationReminderWatchesTheSamePlace() {
        Reminder everyVisit = Reminder.atPlace(5, 42.4, -71.0, 300, "Shop", Repeat.EVERY_ARRIVAL)
                .withId(8);

        Reminder next = everyVisit.nextOccurrence(1_000, NEW_YORK);

        assertNotNull(next);
        assertEquals(0, next.getId());
        assertEquals(ReminderType.LOCATION, next.getType());
        assertEquals(Double.valueOf(42.4), next.getLatitude());
        assertEquals(Double.valueOf(-71.0), next.getLongitude());
        assertEquals(Float.valueOf(300), next.getRadiusMeters());
        assertEquals("Shop", next.getPlaceName());
        assertEquals(Repeat.EVERY_ARRIVAL, next.getRepeat());
        assertNull(next.getTriggerAt());
    }

    @Test
    public void nextOccurrenceOfALeavingReminderWaitsForLeavingAgain() {
        Reminder everyDeparture = Reminder.atPlace(5, 42.4, -71.0, 300, "Work",
                PlaceTrigger.LEAVING, Repeat.EVERY_ARRIVAL).withId(8);

        Reminder next = everyDeparture.nextOccurrence(1_000, NEW_YORK);

        assertNotNull(next);
        assertEquals(PlaceTrigger.LEAVING, next.getPlaceTrigger());
    }

    @Test
    public void reschedulingARepeatingReminderStartsItsSeriesOver() {
        long first = at(2026, 1, 31, 8, 0);
        long moved = at(2026, 3, 15, 10, 0);
        Reminder monthly = Reminder.atTime(1, first, Repeat.MONTHLY).withId(4);

        Reminder rescheduled = monthly.rescheduled(moved, Repeat.MONTHLY, DaysOfWeek.NONE);
        Reminder next = rescheduled.nextOccurrence(moved + 1_000, NEW_YORK);

        assertEquals(4, rescheduled.getId());
        assertNotNull(next);
        assertEquals(Long.valueOf(at(2026, 4, 15, 10, 0)), next.getTriggerAt());
    }
}

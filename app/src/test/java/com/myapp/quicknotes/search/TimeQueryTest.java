package com.myapp.quicknotes.search;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.time.DayOfWeek;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.List;

public class TimeQueryTest {
    private static final ZoneId NEW_YORK = ZoneId.of("America/New_York");
    // Wednesday 28 October 2026, 3 PM. Weeks start on Sunday, as in the United States.
    private static final long NOW = at(2026, 10, 28, 15);

    private static long at(int year, int month, int day, int hour) {
        return ZonedDateTime.of(year, month, day, hour, 0, 0, 0, NEW_YORK)
                .toInstant().toEpochMilli();
    }

    private static TimeQuery.Range range(long start, long end) {
        return new TimeQuery.Range(start, end);
    }

    private static TimeQuery.Range day(int year, int month, int day) {
        return range(at(year, month, day, 0),
                ZonedDateTime.of(year, month, day, 0, 0, 0, 0, NEW_YORK).plusDays(1)
                        .toInstant().toEpochMilli());
    }

    private static List<TimeQuery.Range> ranges(String query) {
        return TimeQuery.parse(query, NOW, NEW_YORK, DayOfWeek.SUNDAY).ranges;
    }

    @Test
    public void todayAndTomorrow() {
        assertEquals(Collections.singletonList(day(2026, 10, 28)), ranges("today"));
        assertEquals(Collections.singletonList(day(2026, 10, 29)), ranges("Tomorrow"));
    }

    @Test
    public void tonightAndPartsOfTheDay() {
        assertEquals(Collections.singletonList(range(at(2026, 10, 28, 18), at(2026, 10, 29, 0))),
                ranges("tonight"));
        assertEquals(Collections.singletonList(range(at(2026, 10, 29, 5), at(2026, 10, 29, 12))),
                ranges("tomorrow morning"));
        assertEquals(Collections.singletonList(range(at(2026, 10, 28, 17), at(2026, 10, 29, 0))),
                ranges("this evening"));
        assertEquals(Collections.singletonList(range(at(2026, 10, 30, 12), at(2026, 10, 30, 17))),
                ranges("Friday afternoon"));
    }

    @Test
    public void weekdaysAreTheComingOneTodayIncluded() {
        assertEquals(Collections.singletonList(day(2026, 10, 30)), ranges("fri"));
        assertEquals(Collections.singletonList(day(2026, 10, 28)), ranges("wednesday"));
        assertEquals(Collections.singletonList(day(2026, 11, 4)), ranges("next wednesday"));
        assertEquals(Collections.singletonList(day(2026, 11, 2)), ranges("monday"));
    }

    @Test
    public void weeksWeekendsAndMonths() {
        assertEquals(Collections.singletonList(range(at(2026, 10, 28, 0), at(2026, 11, 1, 0))),
                ranges("this week"));
        assertEquals(Collections.singletonList(range(at(2026, 11, 1, 0), at(2026, 11, 8, 0))),
                ranges("next week"));
        assertEquals(Collections.singletonList(range(at(2026, 10, 31, 0), at(2026, 11, 2, 0))),
                ranges("weekend"));
        assertEquals(Collections.singletonList(range(at(2026, 11, 1, 0), at(2026, 12, 1, 0))),
                ranges("next month"));
    }

    @Test
    public void monthsAndDates() {
        assertEquals(Collections.singletonList(range(at(2026, 11, 1, 0), at(2026, 12, 1, 0))),
                ranges("november"));
        // The current month counts from today; a month already over is next year's.
        assertEquals(Collections.singletonList(range(at(2026, 10, 28, 0), at(2026, 11, 1, 0))),
                ranges("October"));
        assertEquals(Collections.singletonList(range(at(2027, 9, 1, 0), at(2027, 10, 1, 0))),
                ranges("sept"));
        assertEquals(Collections.singletonList(day(2026, 10, 30)), ranges("Oct 30"));
        assertEquals(Collections.singletonList(day(2026, 10, 30)), ranges("30th october"));
        assertEquals(Collections.singletonList(day(2027, 5, 3)), ranges("may 3"));
    }

    @Test
    public void mayAsAWordIsNotAMonth() {
        assertEquals(Collections.emptyList(), ranges("you may need milk"));
        assertEquals(1, ranges("May").size());
    }

    @Test
    public void searchesMadeOnlyOfTimesAreRecognised() {
        assertTrue(TimeQuery.parse("tomorrow", NOW, NEW_YORK, DayOfWeek.SUNDAY).onlyTime);
        assertTrue(TimeQuery.parse("this week", NOW, NEW_YORK, DayOfWeek.SUNDAY).onlyTime);
        assertTrue(TimeQuery.parse("on friday", NOW, NEW_YORK, DayOfWeek.SUNDAY).onlyTime);
        assertFalse(TimeQuery.parse("dentist tomorrow", NOW, NEW_YORK, DayOfWeek.SUNDAY).onlyTime);
        assertFalse(TimeQuery.parse("milk", NOW, NEW_YORK, DayOfWeek.SUNDAY).onlyTime);
        assertEquals(Collections.emptyList(), ranges("milk"));
    }

    @Test
    public void theWordsThatNamedATimeAreReported() {
        TimeQuery.Parsed parsed =
                TimeQuery.parse("pharmacy next week", NOW, NEW_YORK, DayOfWeek.SUNDAY);

        assertTrue(parsed.timeWords.contains("next"));
        assertTrue(parsed.timeWords.contains("week"));
        assertFalse(parsed.timeWords.contains("pharmacy"));
    }
}

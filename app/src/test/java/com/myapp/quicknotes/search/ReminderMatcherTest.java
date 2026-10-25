package com.myapp.quicknotes.search;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.myapp.quicknotes.data.DaysOfWeek;
import com.myapp.quicknotes.data.NearbyPlace;
import com.myapp.quicknotes.data.Reminder;
import com.myapp.quicknotes.data.Repeat;

import org.junit.Test;

import java.time.DayOfWeek;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ReminderMatcherTest {
    private static final ZoneId NEW_YORK = ZoneId.of("America/New_York");
    // Wednesday 28 October 2026, 3 PM.
    private static final long NOW = at(2026, 10, 28, 15);

    private static long at(int year, int month, int day, int hour) {
        return ZonedDateTime.of(year, month, day, hour, 0, 0, 0, NEW_YORK)
                .toInstant().toEpochMilli();
    }

    private static List<TimeQuery.Range> when(String query) {
        return TimeQuery.parse(query, NOW, NEW_YORK, DayOfWeek.SUNDAY).ranges;
    }

    @Test
    public void aOneOffReminderMatchesTheDayItIsDue() {
        Reminder dentist = Reminder.atTime(1, at(2026, 10, 29, 9), Repeat.NONE).withId(10);

        List<ReminderMatcher.Match> tomorrow =
                ReminderMatcher.byTime(Collections.singletonList(dentist), when("tomorrow"),
                        NEW_YORK);
        List<ReminderMatcher.Match> friday =
                ReminderMatcher.byTime(Collections.singletonList(dentist), when("friday"),
                        NEW_YORK);

        assertEquals(1, tomorrow.size());
        assertEquals(Long.valueOf(at(2026, 10, 29, 9)), tomorrow.get(0).occurrence);
        assertTrue(tomorrow.get(0).isByTime());
        assertEquals(0, friday.size());
    }

    @Test
    public void aRepeatingReminderMatchesItsLaterOccurrences() {
        Reminder pills = Reminder.atTime(1, at(2026, 10, 28, 20), Repeat.DAILY).withId(10);

        List<ReminderMatcher.Match> friday = ReminderMatcher.byTime(
                Collections.singletonList(pills), when("friday"), NEW_YORK);

        assertEquals(Long.valueOf(at(2026, 10, 30, 20)), friday.get(0).occurrence);
    }

    @Test
    public void weekdayRemindersSkipTheWeekend() {
        int weekdays = DaysOfWeek.of(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
                DayOfWeek.THURSDAY, DayOfWeek.FRIDAY);
        Reminder standup = Reminder.atTime(1, at(2026, 10, 29, 9), Repeat.DAYS_OF_WEEK, weekdays)
                .withId(10);

        assertEquals(0, ReminderMatcher.byTime(Collections.singletonList(standup),
                when("weekend"), NEW_YORK).size());
        assertEquals(Long.valueOf(at(2026, 11, 2, 9)), ReminderMatcher.byTime(
                Collections.singletonList(standup), when("monday"), NEW_YORK).get(0).occurrence);
    }

    @Test
    public void notesComeOnceSoonestFirst() {
        List<Reminder> reminders = Arrays.asList(
                Reminder.atTime(1, at(2026, 11, 3, 9), Repeat.NONE).withId(10),
                Reminder.atTime(2, at(2026, 11, 2, 9), Repeat.NONE).withId(11),
                Reminder.atTime(1, at(2026, 11, 2, 8), Repeat.NONE).withId(12));

        List<ReminderMatcher.Match> nextWeek =
                ReminderMatcher.byTime(reminders, when("next week"), NEW_YORK);

        assertEquals(2, nextWeek.size());
        assertEquals(1, nextWeek.get(0).noteId);
        assertEquals(12, nextWeek.get(0).reminder.getId());
        assertEquals(2, nextWeek.get(1).noteId);
    }

    private static Map<String, List<NearbyPlace>> nearby(Reminder reminder, NearbyPlace... places) {
        Map<String, List<NearbyPlace>> map = new HashMap<>();
        map.put(reminder.placeKey(), Arrays.asList(places));
        return map;
    }

    private static NearbyPlace place(Reminder reminder, String name, String kind, int distance) {
        return new NearbyPlace(0, reminder.placeKey(), name, kind, distance);
    }

    @Test
    public void theNameTheUserGaveMatches() {
        Reminder reminder = Reminder.atPlace(1, 42.35, -71.07, 200, "Walgreens", Repeat.NONE)
                .withId(10);

        List<ReminderMatcher.Match> matches = ReminderMatcher.byPlace(
                Collections.singletonList(reminder), new HashMap<>(),
                KeywordScorer.terms("walgreens"));

        assertEquals(1, matches.size());
        assertNull(matches.get(0).nearbyName);
        assertFalse(matches.get(0).byKindOnly);
    }

    @Test
    public void anUnnamedPlaceIsFoundByWhatIsNearIt() {
        Reminder reminder = Reminder.atPlace(1, 42.35, -71.07, 200, null, Repeat.NONE).withId(10);
        Map<String, List<NearbyPlace>> around = nearby(reminder,
                place(reminder, "Back Bay", "neighbourhood", 0),
                place(reminder, "Boylston Street", "street", 20),
                place(reminder, "Walgreens", "pharmacy", 80),
                place(reminder, "CVS", "pharmacy", 150));
        List<Reminder> reminders = Collections.singletonList(reminder);

        assertEquals("Walgreens", ReminderMatcher.byPlace(reminders, around,
                KeywordScorer.terms("Walgreens")).get(0).nearbyName);
        assertFalse(ReminderMatcher.byPlace(reminders, around,
                KeywordScorer.terms("Walgreens")).get(0).byKindOnly);
        assertTrue(ReminderMatcher.byPlace(reminders, around,
                KeywordScorer.terms("pharmacy")).get(0).byKindOnly);
        // By kind, the nearest of that kind.
        assertEquals("Walgreens", ReminderMatcher.byPlace(reminders, around,
                KeywordScorer.terms("pharmacy")).get(0).nearbyName);
        assertEquals("Walgreens", ReminderMatcher.byPlace(reminders, around,
                KeywordScorer.terms("drugstore")).get(0).nearbyName);
        assertEquals("CVS", ReminderMatcher.byPlace(reminders, around,
                KeywordScorer.terms("cvs")).get(0).nearbyName);
        assertEquals("Boylston Street", ReminderMatcher.byPlace(reminders, around,
                KeywordScorer.terms("boylston")).get(0).nearbyName);
        assertEquals(0, ReminderMatcher.byPlace(reminders, around,
                KeywordScorer.terms("milk")).size());
    }

    @Test
    public void whatAPlaceIsMustAccountForEveryWord() {
        Reminder reminder = Reminder.atPlace(1, 42.35, -71.07, 200, null, Repeat.NONE).withId(10);
        Map<String, List<NearbyPlace>> around = nearby(reminder,
                place(reminder, "Red Sox Team Store", "gift shop", 40),
                place(reminder, "Walgreens", "pharmacy", 80));
        List<Reminder> reminders = Collections.singletonList(reminder);

        assertEquals(0, ReminderMatcher.byPlace(reminders, around,
                KeywordScorer.terms("gift for Mom")).size());
        assertEquals(1, ReminderMatcher.byPlace(reminders, around,
                KeywordScorer.terms("gift shop")).size());
        // A name counts on its own.
        assertEquals(1, ReminderMatcher.byPlace(reminders, around,
                KeywordScorer.terms("walgreens shampoo")).size());
    }

    @Test
    public void whatAPlaceIsOnlyCountsCloseToThePin() {
        Reminder reminder = Reminder.atPlace(1, 42.35, -71.07, 300, null, Repeat.NONE).withId(10);
        Map<String, List<NearbyPlace>> around = nearby(reminder,
                place(reminder, "Citizens Bank", "bank", 250),
                place(reminder, "Boylston Street", "street", 20),
                place(reminder, "Back Bay", "neighbourhood", 0));
        List<Reminder> reminders = Collections.singletonList(reminder);

        // Too far out to be the place the reminder is about...
        assertEquals(0, ReminderMatcher.byPlace(reminders, around,
                KeywordScorer.terms("bank")).size());
        // ...but its name still counts.
        assertEquals(1, ReminderMatcher.byPlace(reminders, around,
                KeywordScorer.terms("citizens")).size());
        // Streets and areas are found by name, not by "street" or "neighbourhood".
        assertEquals(0, ReminderMatcher.byPlace(reminders, around,
                KeywordScorer.terms("street")).size());
        assertEquals(0, ReminderMatcher.byPlace(reminders, around,
                KeywordScorer.terms("neighbourhood")).size());
    }

    @Test
    public void veryShortWordsDontMatchPlaces() {
        Reminder reminder = Reminder.atPlace(1, 42.35, -71.07, 200, null, Repeat.NONE).withId(10);
        Map<String, List<NearbyPlace>> around =
                nearby(reminder, place(reminder, "Back Bay", "neighbourhood", 0));

        assertEquals(0, ReminderMatcher.byPlace(Collections.singletonList(reminder), around,
                KeywordScorer.terms("ba")).size());
    }

    @Test
    public void aNamedPlaceOutranksSomethingNearAnother() {
        Reminder named = Reminder.atPlace(1, 42.35, -71.07, 200, "Pharmacy", Repeat.NONE)
                .withId(10);
        Reminder unnamed = Reminder.atPlace(2, 42.36, -71.06, 200, null, Repeat.NONE).withId(11);
        Map<String, List<NearbyPlace>> around =
                nearby(unnamed, place(unnamed, "Walgreens", "pharmacy", 10));

        List<ReminderMatcher.Match> matches = ReminderMatcher.byPlace(
                Arrays.asList(unnamed, named), around, KeywordScorer.terms("pharmacy"));

        assertEquals(1, matches.get(0).noteId);
        assertEquals(2, matches.get(1).noteId);
    }

    @Test
    public void timeRemindersDontMatchPlacesAndPlaceRemindersDontMatchTimes() {
        Reminder time = Reminder.atTime(1, at(2026, 10, 29, 9), Repeat.NONE).withId(10);
        Reminder place = Reminder.atPlace(2, 42.35, -71.07, 200, "Tomorrow's market",
                Repeat.NONE).withId(11);
        List<Reminder> both = Arrays.asList(time, place);

        assertEquals(1, ReminderMatcher.byTime(both, when("tomorrow"), NEW_YORK).get(0).noteId);
        assertEquals(2, ReminderMatcher.byPlace(both, new HashMap<>(),
                KeywordScorer.terms("market")).get(0).noteId);
    }
}

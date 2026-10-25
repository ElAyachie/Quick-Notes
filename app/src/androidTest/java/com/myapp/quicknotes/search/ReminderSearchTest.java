package com.myapp.quicknotes.search;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.myapp.quicknotes.AppExecutors;
import com.myapp.quicknotes.data.AppDatabase;
import com.myapp.quicknotes.data.Folder;
import com.myapp.quicknotes.data.NearbyPlace;
import com.myapp.quicknotes.data.Note;
import com.myapp.quicknotes.data.PlaceLookup;
import com.myapp.quicknotes.data.Reminder;
import com.myapp.quicknotes.data.Repeat;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Arrays;

// Search finding notes through their reminders, end to end through the database. The model is
// left out (an embedder that always fails), so only words and reminders are at work.
@RunWith(AndroidJUnit4.class)
public class ReminderSearchTest {
    private static final ZoneId NEW_YORK = ZoneId.of("America/New_York");
    // Wednesday 28 October 2026, 3 PM.
    private static final long NOW = at(28, 15);

    private AppDatabase database;
    private SearchRepository search;
    private long prescription;
    private long dentist;
    private long groceries;

    private static long at(int day, int hour) {
        return ZonedDateTime.of(2026, 10, day, hour, 0, 0, 0, NEW_YORK).toInstant().toEpochMilli();
    }

    @Before
    public void setUp() {
        database = AppDatabase.createInMemory(ApplicationProvider.getApplicationContext());
        search = new SearchRepository(database.searchDao(), new Embedder() {
            @Override
            public String modelId() {
                return "none";
            }

            @Override
            public float[] embedQuery(String query) {
                throw new IllegalStateException("No model in this test");
            }

            @Override
            public float[] embedPassage(String passage) {
                throw new IllegalStateException("No model in this test");
            }
        }, new AppExecutors());

        prescription = note("Pick up prescription", "The blood pressure one");
        dentist = note("Dentist", "Cleaning");
        groceries = note("Groceries", "Milk, eggs");
        // An unnamed place, recognised by what is near it.
        Reminder atPharmacy = Reminder.atPlace(prescription, 42.35, -71.07, 200, null,
                Repeat.NONE);
        database.reminderDao().insert(atPharmacy);
        database.placeDao().saveLookup(new PlaceLookup(atPharmacy.placeKey(), NOW),
                Arrays.asList(
                        new NearbyPlace(0, atPharmacy.placeKey(), "Back Bay", "neighbourhood", 0),
                        new NearbyPlace(0, atPharmacy.placeKey(), "Walgreens", "pharmacy", 80)));
        database.reminderDao().insert(Reminder.atTime(dentist, at(29, 9), Repeat.NONE));
        // A named place.
        database.reminderDao().insert(
                Reminder.atPlace(groceries, 42.36, -71.06, 300, "Trader Joe's", Repeat.NONE));
    }

    @After
    public void closeDatabase() {
        database.close();
    }

    private long note(String title, String content) {
        return database.noteDao().insert(
                Note.blank(Folder.DEFAULT_ID).edited(title, content, Folder.DEFAULT_ID));
    }

    @Test
    public void aShopNearTheReminderFindsTheNote() {
        SearchRepository.Ranking ranking = search.rank("walgreens", NOW, NEW_YORK);

        assertEquals(Long.valueOf(prescription), ranking.combined.get(0));
        ReminderMatcher.Match match = ranking.reminderMatchFor(prescription);
        assertNotNull(match);
        assertEquals("Walgreens", match.nearbyName);
    }

    @Test
    public void whatTheShopIsFindsTheNoteToo() {
        assertEquals(Long.valueOf(prescription),
                search.rank("drugstore", NOW, NEW_YORK).combined.get(0));
        assertEquals(Long.valueOf(prescription),
                search.rank("back bay", NOW, NEW_YORK).combined.get(0));
    }

    @Test
    public void theNameGivenToAPlaceFindsTheNote() {
        SearchRepository.Ranking ranking = search.rank("trader joes", NOW, NEW_YORK);

        assertEquals(Long.valueOf(groceries), ranking.combined.get(0));
        assertNull(ranking.reminderMatchFor(groceries).nearbyName);
    }

    @Test
    public void aTimeFindsTheNoteDueThen() {
        SearchRepository.Ranking ranking = search.rank("tomorrow", NOW, NEW_YORK);

        assertEquals(Arrays.asList(dentist), ranking.combined);
        assertEquals(Long.valueOf(at(29, 9)), ranking.reminderMatchFor(dentist).occurrence);
        assertTrue(search.rank("friday", NOW, NEW_YORK).combined.isEmpty());
    }

    @Test
    public void wordsAndReminderTogetherComeFirst() {
        // "Cleaning" is in the dentist note; "tomorrow" is when its reminder is due.
        SearchRepository.Ranking ranking = search.rank("cleaning tomorrow", NOW, NEW_YORK);

        assertEquals(Long.valueOf(dentist), ranking.combined.get(0));
    }

    @Test
    public void aKindOfPlaceAndATimeTogether() {
        // The prescription reminder is near a pharmacy; "tomorrow" is about time, not the place.
        SearchRepository.Ranking ranking = search.rank("pharmacy tomorrow", NOW, NEW_YORK);

        assertTrue(ranking.combined.contains(prescription));
        assertTrue(ranking.combined.contains(dentist));
    }

    @Test
    public void aFiredReminderIsNotMatched() {
        Reminder fired = Reminder.atTime(groceries, at(29, 18), Repeat.NONE);
        long id = database.reminderDao().insert(fired);
        database.reminderDao().markFired(id, NOW);

        SearchRepository.Ranking ranking = search.rank("tomorrow", NOW, NEW_YORK);

        assertEquals(Arrays.asList(dentist), ranking.combined);
    }
}

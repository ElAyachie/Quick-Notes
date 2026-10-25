package com.myapp.quicknotes.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.time.DayOfWeek;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@RunWith(AndroidJUnit4.class)
public class AppDatabaseTest {
    private AppDatabase database;
    private FolderDao folders;
    private NoteDao notes;
    private ReminderDao reminders;

    @Before
    public void createDatabase() {
        database = AppDatabase.createInMemory(ApplicationProvider.getApplicationContext());
        folders = database.folderDao();
        notes = database.noteDao();
        reminders = database.reminderDao();
    }

    @After
    public void closeDatabase() {
        database.close();
    }

    @Test
    public void newDatabaseContainsTheDefaultFolder() {
        assertEquals(1, folders.countByName(Folder.DEFAULT_NAME));
    }

    @Test
    public void noteKeepsItsIdWhenRenamedAndMoved() {
        long folderId = folders.insert(new Folder("Work"));
        long noteId = notes.insert(Note.blank(Folder.DEFAULT_ID).edited("Draft", "text", Folder.DEFAULT_ID));

        Note stored = notes.getById(noteId);
        assertNotNull(stored);
        notes.update(stored.edited("Final", "text", folderId));

        Note updated = notes.getById(noteId);
        assertNotNull(updated);
        assertEquals("Final", updated.getTitle());
        assertEquals(folderId, updated.getFolderId());
    }

    @Test
    public void twoNotesInOneFolderMayShareATitle() {
        long first = notes.insert(Note.blank(Folder.DEFAULT_ID).edited("Shopping", "milk", Folder.DEFAULT_ID));
        long second = notes.insert(Note.blank(Folder.DEFAULT_ID).edited("Shopping", "eggs", Folder.DEFAULT_ID));

        assertTrue(first != second);
        assertNotNull(notes.getById(first));
        assertNotNull(notes.getById(second));
    }

    @Test
    public void deletingANoteDeletesItsReminders() {
        long noteId = notes.insert(Note.blank(Folder.DEFAULT_ID).edited("Call", "", Folder.DEFAULT_ID));
        long reminderId = reminders.insert(Reminder.atTime(noteId, 1_000, Repeat.NONE));

        notes.deleteById(noteId);

        assertNull(reminders.getById(reminderId));
    }

    @Test
    public void deletingAFolderDeletesItsNotesAndTheirReminders() {
        long folderId = folders.insert(new Folder("Trips"));
        long noteId = notes.insert(Note.blank(folderId).edited("Pack", "", folderId));
        long reminderId = reminders.insert(Reminder.atTime(noteId, 1_000, Repeat.NONE));

        folders.deleteById(folderId);

        assertNull(notes.getById(noteId));
        assertNull(reminders.getById(reminderId));
    }

    @Test
    public void remindersForAFolderCoverOnlyThatFoldersNotes() {
        long folderId = folders.insert(new Folder("Trips"));
        long inFolder = notes.insert(Note.blank(folderId).edited("Pack", "", folderId));
        long elsewhere = notes.insert(Note.blank(Folder.DEFAULT_ID).edited("Call", "", Folder.DEFAULT_ID));
        long first = reminders.insert(Reminder.atTime(inFolder, 1_000, Repeat.NONE));
        long second = reminders.insert(Reminder.atPlace(inFolder, 48.8584, 2.2945, 200, "Tower", Repeat.NONE));
        reminders.insert(Reminder.atTime(elsewhere, 3_000, Repeat.NONE));

        List<Long> ids = new ArrayList<>();
        for (Reminder reminder : reminders.getForFolder(folderId)) {
            ids.add(reminder.getId());
        }

        assertEquals(Arrays.asList(first, second), ids);
    }

    @Test
    public void locationReminderKeepsItsPlace() {
        long noteId = notes.insert(Note.blank(Folder.DEFAULT_ID).edited("Milk", "", Folder.DEFAULT_ID));
        long reminderId = reminders.insert(Reminder.atPlace(noteId, 48.8584, 2.2945, 350, "Shop", Repeat.EVERY_ARRIVAL));

        Reminder stored = reminders.getById(reminderId);

        assertNotNull(stored);
        assertEquals(ReminderType.LOCATION, stored.getType());
        assertNull(stored.getTriggerAt());
        assertEquals(48.8584, stored.getLatitude(), 0.0);
        assertEquals(2.2945, stored.getLongitude(), 0.0);
        assertEquals(350f, stored.getRadiusMeters(), 0f);
        assertEquals("Shop", stored.getPlaceName());
        assertEquals(PlaceTrigger.ARRIVING, stored.getPlaceTrigger());
        assertEquals(Repeat.EVERY_ARRIVAL, stored.getRepeat());
    }

    @Test
    public void leavingReminderStaysALeavingReminder() {
        long noteId = notes.insert(Note.blank(Folder.DEFAULT_ID).edited("Badge", "", Folder.DEFAULT_ID));
        long reminderId = reminders.insert(Reminder.atPlace(noteId, 48.8584, 2.2945, 350, "Work",
                PlaceTrigger.LEAVING, Repeat.NONE));

        Reminder stored = reminders.getById(reminderId);

        assertNotNull(stored);
        assertEquals(PlaceTrigger.LEAVING, stored.getPlaceTrigger());
    }

    @Test
    public void firedReminderLeavesTheUpcomingList() {
        long noteId = notes.insert(Note.blank(Folder.DEFAULT_ID).edited("Call", "", Folder.DEFAULT_ID));
        long firedId = reminders.insert(Reminder.atTime(noteId, 1_000, Repeat.NONE));
        long waitingId = reminders.insert(Reminder.atTime(noteId, 2_000, Repeat.NONE));

        reminders.markFired(firedId, 1_500);

        List<Reminder> upcoming = reminders.getUpcoming();
        assertEquals(1, upcoming.size());
        assertEquals(waitingId, upcoming.get(0).getId());
        Reminder fired = reminders.getById(firedId);
        assertNotNull(fired);
        assertEquals(Long.valueOf(1_500), fired.getFiredAt());
    }

    @Test
    public void firingARepeatingReminderKeepsItAsHistoryAndStoresTheNextOccurrence() {
        long noteId = notes.insert(Note.blank(Folder.DEFAULT_ID).edited("Pills", "", Folder.DEFAULT_ID));
        Reminder daily = Reminder.atTime(noteId, 1_000, Repeat.DAILY);
        long firedId = reminders.insert(daily);
        Reminder next = daily.withId(firedId).nextOccurrence(1_500, java.time.ZoneOffset.UTC);
        assertNotNull(next);

        long nextId = reminders.markFiredAndInsertNext(firedId, 1_500, next);

        Reminder fired = reminders.getById(firedId);
        assertNotNull(fired);
        assertTrue(fired.hasFired());
        List<Reminder> upcoming = reminders.getUpcoming();
        assertEquals(1, upcoming.size());
        assertEquals(nextId, upcoming.get(0).getId());
        assertEquals(Long.valueOf(1_000 + 24 * 60 * 60 * 1_000), upcoming.get(0).getTriggerAt());
    }

    @Test
    public void editedReminderKeepsItsId() {
        long noteId = notes.insert(Note.blank(Folder.DEFAULT_ID).edited("Call", "", Folder.DEFAULT_ID));
        long reminderId = reminders.insert(Reminder.atTime(noteId, 1_000, Repeat.NONE));
        Reminder stored = reminders.getById(reminderId);
        assertNotNull(stored);

        int tuesdayAndThursday = DaysOfWeek.of(DayOfWeek.TUESDAY, DayOfWeek.THURSDAY);
        reminders.update(stored.rescheduled(9_000, Repeat.DAYS_OF_WEEK, tuesdayAndThursday));

        Reminder edited = reminders.getById(reminderId);
        assertNotNull(edited);
        assertEquals(Long.valueOf(9_000), edited.getTriggerAt());
        assertEquals(Repeat.DAYS_OF_WEEK, edited.getRepeat());
        assertEquals(tuesdayAndThursday, edited.getRepeatDays());
        assertEquals(1, reminders.getUpcoming().size());
    }
}

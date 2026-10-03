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
        long reminderId = reminders.insert(Reminder.atTime(noteId, 1_000));

        notes.deleteById(noteId);

        assertNull(reminders.getById(reminderId));
    }

    @Test
    public void deletingAFolderDeletesItsNotesAndTheirReminders() {
        long folderId = folders.insert(new Folder("Trips"));
        long noteId = notes.insert(Note.blank(folderId).edited("Pack", "", folderId));
        long reminderId = reminders.insert(Reminder.atTime(noteId, 1_000));

        folders.deleteById(folderId);

        assertNull(notes.getById(noteId));
        assertNull(reminders.getById(reminderId));
    }

    @Test
    public void reminderIdsForAFolderCoverOnlyThatFoldersNotes() {
        long folderId = folders.insert(new Folder("Trips"));
        long inFolder = notes.insert(Note.blank(folderId).edited("Pack", "", folderId));
        long elsewhere = notes.insert(Note.blank(Folder.DEFAULT_ID).edited("Call", "", Folder.DEFAULT_ID));
        long first = reminders.insert(Reminder.atTime(inFolder, 1_000));
        long second = reminders.insert(Reminder.atTime(inFolder, 2_000));
        reminders.insert(Reminder.atTime(elsewhere, 3_000));

        List<Long> ids = reminders.getIdsForFolder(folderId);

        assertEquals(Arrays.asList(first, second), ids);
    }
}

package com.myapp.quicknotes.data;

import androidx.lifecycle.LiveData;

import com.myapp.quicknotes.AppExecutors;
import com.myapp.quicknotes.reminders.ReminderScheduler;

import java.util.List;
import java.util.function.Consumer;

public class NoteRepository {
    private final NoteDao noteDao;
    private final ReminderDao reminderDao;
    private final ReminderScheduler scheduler;
    private final AppExecutors executors;

    public NoteRepository(NoteDao noteDao, ReminderDao reminderDao,
                          ReminderScheduler scheduler, AppExecutors executors) {
        this.noteDao = noteDao;
        this.reminderDao = reminderDao;
        this.scheduler = scheduler;
        this.executors = executors;
    }

    public LiveData<List<Note>> observeNotesInFolder(long folderId) {
        return noteDao.observeInFolder(folderId);
    }

    // Reports null when the note no longer exists.
    public void loadNote(long noteId, Consumer<Note> onLoaded) {
        executors.io().execute(() -> {
            Note note = noteDao.getById(noteId);
            executors.runOnMain(() -> onLoaded.accept(note));
        });
    }

    // Inserts a new note or updates an existing one, and reports the note as stored.
    public void saveNote(Note note, Consumer<Note> onSaved) {
        executors.io().execute(() -> {
            Note saved;
            if (note.isNew()) {
                saved = note.withId(noteDao.insert(note));
            } else {
                noteDao.update(note);
                saved = note;
            }
            executors.runOnMain(() -> onSaved.accept(saved));
        });
    }

    public void deleteNote(long noteId) {
        executors.io().execute(() -> {
            for (Reminder reminder : reminderDao.getForNote(noteId)) {
                scheduler.cancel(reminder);
            }
            noteDao.deleteById(noteId);
        });
    }
}

package com.myapp.quicknotes;

import android.content.Context;

import com.myapp.quicknotes.data.AppDatabase;
import com.myapp.quicknotes.data.FolderRepository;
import com.myapp.quicknotes.data.NoteRepository;
import com.myapp.quicknotes.data.ReminderRepository;
import com.myapp.quicknotes.reminders.ReminderNotifier;
import com.myapp.quicknotes.reminders.ReminderScheduler;

// Builds the app's long-lived objects once and hands them to whoever needs them.
public class AppContainer {
    private final AppExecutors executors = new AppExecutors();
    private final ReminderScheduler reminderScheduler;
    private final ReminderNotifier reminderNotifier;
    private final FolderRepository folderRepository;
    private final NoteRepository noteRepository;
    private final ReminderRepository reminderRepository;

    AppContainer(Context appContext) {
        AppDatabase database = AppDatabase.create(appContext);
        reminderScheduler = new ReminderScheduler(appContext);
        reminderNotifier = new ReminderNotifier(appContext);
        folderRepository = new FolderRepository(
                database.folderDao(), database.reminderDao(), reminderScheduler, executors);
        noteRepository = new NoteRepository(
                database.noteDao(), database.reminderDao(), reminderScheduler, executors);
        reminderRepository = new ReminderRepository(
                database.reminderDao(), database.noteDao(), reminderScheduler, reminderNotifier,
                executors);
    }

    public AppExecutors executors() {
        return executors;
    }

    public ReminderScheduler reminderScheduler() {
        return reminderScheduler;
    }

    public ReminderNotifier reminderNotifier() {
        return reminderNotifier;
    }

    public FolderRepository folderRepository() {
        return folderRepository;
    }

    public NoteRepository noteRepository() {
        return noteRepository;
    }

    public ReminderRepository reminderRepository() {
        return reminderRepository;
    }
}

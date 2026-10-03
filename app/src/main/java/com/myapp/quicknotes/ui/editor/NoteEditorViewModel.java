package com.myapp.quicknotes.ui.editor;

import android.app.Application;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.SavedStateHandle;

import com.myapp.quicknotes.AppContainer;
import com.myapp.quicknotes.QuickNotesApp;
import com.myapp.quicknotes.data.Folder;
import com.myapp.quicknotes.data.Note;
import com.myapp.quicknotes.data.NoteRepository;
import com.myapp.quicknotes.data.ReminderRepository;
import com.myapp.quicknotes.data.ReminderType;
import com.myapp.quicknotes.data.Repeat;
import com.myapp.quicknotes.reminders.ReminderNotifier;
import com.myapp.quicknotes.reminders.ReminderScheduler;

import java.util.List;

public class NoteEditorViewModel extends AndroidViewModel {
    // Navigation arguments; the names match nav_graph.xml. A note id of 0 means a new note.
    public static final String ARG_NOTE_ID = "noteId";
    public static final String ARG_FOLDER_ID = "folderId";
    // Editor state that has to survive the app being killed in the background.
    private static final String STATE_SELECTED_FOLDER_ID = "selectedFolderId";
    private static final String STATE_FORM_FILLED = "formFilled";
    private static final String STATE_PENDING_REMINDER_TYPE = "pendingReminderType";

    private final NoteRepository notes;
    private final ReminderRepository reminders;
    private final ReminderScheduler scheduler;
    private final ReminderNotifier notifier;
    private final SavedStateHandle state;
    private final LiveData<List<Folder>> folders;
    // The note as it is stored. For a new note, an empty note that has not been saved yet.
    private final MutableLiveData<Note> note = new MutableLiveData<>();
    private final MutableLiveData<Boolean> noteMissing = new MutableLiveData<>(false);

    public NoteEditorViewModel(@NonNull Application application, @NonNull SavedStateHandle state) {
        super(application);
        AppContainer container = QuickNotesApp.container(application);
        this.notes = container.noteRepository();
        this.reminders = container.reminderRepository();
        this.scheduler = container.reminderScheduler();
        this.notifier = container.reminderNotifier();
        this.folders = container.folderRepository().observeFolders();
        this.state = state;

        long noteId = longState(ARG_NOTE_ID, 0);
        if (noteId == 0) {
            long folderId = longState(ARG_FOLDER_ID, Folder.DEFAULT_ID);
            if (!state.contains(STATE_SELECTED_FOLDER_ID)) {
                state.set(STATE_SELECTED_FOLDER_ID, folderId);
            }
            note.setValue(Note.blank(folderId));
        } else {
            notes.loadNote(noteId, loaded -> {
                if (loaded == null) {
                    noteMissing.setValue(true);
                    return;
                }
                if (!isFormFilled()) {
                    state.set(STATE_SELECTED_FOLDER_ID, loaded.getFolderId());
                }
                note.setValue(loaded);
            });
        }
    }

    public static Bundle args(long noteId, long folderId) {
        Bundle args = new Bundle();
        args.putLong(ARG_NOTE_ID, noteId);
        args.putLong(ARG_FOLDER_ID, folderId);
        return args;
    }

    public LiveData<Note> getNote() {
        return note;
    }

    // True when the editor was opened on a note that has since been deleted.
    public LiveData<Boolean> isNoteMissing() {
        return noteMissing;
    }

    public LiveData<List<Folder>> getFolders() {
        return folders;
    }

    public boolean isNewNote() {
        Note current = note.getValue();
        return current == null || current.isNew();
    }

    public long getSelectedFolderId() {
        return longState(STATE_SELECTED_FOLDER_ID, Folder.DEFAULT_ID);
    }

    public void setSelectedFolderId(long folderId) {
        state.set(STATE_SELECTED_FOLDER_ID, folderId);
    }

    // The form is filled from the stored note only once; after that the text fields hold what the
    // user typed, and refilling them (after a rotation, say) would throw that away.
    public boolean isFormFilled() {
        return Boolean.TRUE.equals(state.get(STATE_FORM_FILLED));
    }

    public void markFormFilled() {
        state.set(STATE_FORM_FILLED, true);
    }

    public boolean hasUnsavedChanges(String title, String content) {
        Note stored = note.getValue();
        return stored != null
                && (!stored.getTitle().equals(title)
                || !stored.getContent().equals(content)
                || stored.getFolderId() != getSelectedFolderId());
    }

    public void save(String title, String content, Runnable onSaved) {
        Note stored = note.getValue();
        if (stored == null) {
            return;
        }
        notes.saveNote(stored.edited(title, content, getSelectedFolderId()), saved -> {
            // A new note has an id now; remember it so a restored editor reopens this note.
            state.set(ARG_NOTE_ID, saved.getId());
            note.setValue(saved);
            onSaved.run();
        });
    }

    public void delete() {
        Note stored = note.getValue();
        if (stored != null && !stored.isNew()) {
            notes.deleteNote(stored.getId());
        }
    }

    // Only a saved note can have a reminder, because the reminder refers to the note by id.
    public void addTimeReminder(long triggerAt, Repeat repeat) {
        Note stored = note.getValue();
        if (stored != null && !stored.isNew()) {
            reminders.addTimeReminder(stored.getId(), triggerAt, repeat);
        }
    }

    // Setting a reminder can take several steps (saving the note, permission prompts). This is
    // the kind of reminder those steps are leading up to.
    public ReminderType getPendingReminderType() {
        ReminderType type = state.get(STATE_PENDING_REMINDER_TYPE);
        return type != null ? type : ReminderType.TIME;
    }

    public void setPendingReminderType(ReminderType type) {
        state.set(STATE_PENDING_REMINDER_TYPE, type);
    }

    public long getNoteId() {
        Note stored = note.getValue();
        return stored != null ? stored.getId() : 0;
    }

    public boolean canShowNotifications() {
        return notifier.canNotify();
    }

    public boolean canScheduleExactAlarms() {
        return scheduler.canScheduleExactAlarms();
    }

    private long longState(String key, long fallback) {
        Long value = state.get(key);
        return value != null ? value : fallback;
    }
}

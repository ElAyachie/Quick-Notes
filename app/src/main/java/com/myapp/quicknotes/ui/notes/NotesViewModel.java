package com.myapp.quicknotes.ui.notes;

import android.app.Application;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.SavedStateHandle;

import com.myapp.quicknotes.QuickNotesApp;
import com.myapp.quicknotes.data.Note;
import com.myapp.quicknotes.data.NoteRepository;

import java.util.List;

public class NotesViewModel extends AndroidViewModel {
    // Navigation arguments; the names match nav_graph.xml.
    private static final String ARG_FOLDER_ID = "folderId";
    private static final String ARG_FOLDER_NAME = "folderName";

    private final NoteRepository notes;
    private final long folderId;
    private final LiveData<List<Note>> notesInFolder;

    public NotesViewModel(@NonNull Application application, @NonNull SavedStateHandle state) {
        super(application);
        notes = QuickNotesApp.container(application).noteRepository();
        Long folderIdArg = state.get(ARG_FOLDER_ID);
        folderId = folderIdArg != null ? folderIdArg : 0;
        notesInFolder = notes.observeNotesInFolder(folderId);
    }

    public static Bundle args(long folderId, String folderName) {
        Bundle args = new Bundle();
        args.putLong(ARG_FOLDER_ID, folderId);
        args.putString(ARG_FOLDER_NAME, folderName);
        return args;
    }

    public long getFolderId() {
        return folderId;
    }

    public LiveData<List<Note>> getNotes() {
        return notesInFolder;
    }

    public void deleteNote(Note note) {
        notes.deleteNote(note.getId());
    }
}

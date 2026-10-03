package com.myapp.quicknotes.ui.home;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.myapp.quicknotes.AppContainer;
import com.myapp.quicknotes.QuickNotesApp;
import com.myapp.quicknotes.data.Folder;
import com.myapp.quicknotes.data.Note;
import com.myapp.quicknotes.data.NoteRepository;

import java.util.List;

public class QuickNoteViewModel extends AndroidViewModel {
    private final NoteRepository notes;
    private final LiveData<List<Folder>> folders;
    private long selectedFolderId = Folder.DEFAULT_ID;

    public QuickNoteViewModel(@NonNull Application application) {
        super(application);
        AppContainer container = QuickNotesApp.container(application);
        notes = container.noteRepository();
        folders = container.folderRepository().observeFolders();
    }

    public LiveData<List<Folder>> getFolders() {
        return folders;
    }

    public long getSelectedFolderId() {
        return selectedFolderId;
    }

    public void setSelectedFolderId(long folderId) {
        selectedFolderId = folderId;
    }

    public void saveNote(String title, String content, Runnable onSaved) {
        Note note = Note.blank(selectedFolderId).edited(title, content, selectedFolderId);
        notes.saveNote(note, saved -> onSaved.run());
    }
}

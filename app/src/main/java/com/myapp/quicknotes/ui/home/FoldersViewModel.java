package com.myapp.quicknotes.ui.home;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.myapp.quicknotes.QuickNotesApp;
import com.myapp.quicknotes.data.Folder;
import com.myapp.quicknotes.data.FolderRepository;

import java.util.List;
import java.util.function.Consumer;

public class FoldersViewModel extends AndroidViewModel {
    private final FolderRepository folders;

    public FoldersViewModel(@NonNull Application application) {
        super(application);
        folders = QuickNotesApp.container(application).folderRepository();
    }

    public LiveData<List<Folder>> getFolders() {
        return folders.observeFolders();
    }

    public void createFolder(String name, Consumer<Boolean> onResult) {
        folders.createFolder(name, onResult);
    }

    public void deleteFolder(Folder folder) {
        folders.deleteFolder(folder);
    }
}

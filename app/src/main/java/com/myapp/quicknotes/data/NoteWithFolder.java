package com.myapp.quicknotes.data;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Embedded;

// A note together with the name of its folder, for search results, which mix folders.
public class NoteWithFolder {
    @Embedded
    @NonNull
    private final Note note;
    @ColumnInfo(name = "folder_name")
    @NonNull
    private final String folderName;

    public NoteWithFolder(@NonNull Note note, @NonNull String folderName) {
        this.note = note;
        this.folderName = folderName;
    }

    @NonNull
    public Note getNote() {
        return note;
    }

    @NonNull
    public String getFolderName() {
        return folderName;
    }
}

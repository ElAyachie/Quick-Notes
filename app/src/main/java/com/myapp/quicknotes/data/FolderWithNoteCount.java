package com.myapp.quicknotes.data;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Embedded;

import java.util.Objects;

// A folder together with how many notes it holds, for the folder list.
public class FolderWithNoteCount {
    @Embedded
    @NonNull
    private final Folder folder;
    @ColumnInfo(name = "note_count")
    private final int noteCount;

    public FolderWithNoteCount(@NonNull Folder folder, int noteCount) {
        this.folder = folder;
        this.noteCount = noteCount;
    }

    @NonNull
    public Folder getFolder() {
        return folder;
    }

    public int getNoteCount() {
        return noteCount;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof FolderWithNoteCount)) return false;
        FolderWithNoteCount other = (FolderWithNoteCount) o;
        return noteCount == other.noteCount && folder.equals(other.folder);
    }

    @Override
    public int hashCode() {
        return Objects.hash(folder, noteCount);
    }
}

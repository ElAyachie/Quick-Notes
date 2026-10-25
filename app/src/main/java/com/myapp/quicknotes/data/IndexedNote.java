package com.myapp.quicknotes.data;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;

// What a note's passages were made from: by which model, from which version of its text.
public class IndexedNote {
    @ColumnInfo(name = "note_id")
    private final long noteId;
    @ColumnInfo(name = "model_id")
    @NonNull
    private final String modelId;
    @ColumnInfo(name = "source_hash")
    @NonNull
    private final String sourceHash;

    public IndexedNote(long noteId, @NonNull String modelId, @NonNull String sourceHash) {
        this.noteId = noteId;
        this.modelId = modelId;
        this.sourceHash = sourceHash;
    }

    public long getNoteId() {
        return noteId;
    }

    @NonNull
    public String getModelId() {
        return modelId;
    }

    @NonNull
    public String getSourceHash() {
        return sourceHash;
    }
}

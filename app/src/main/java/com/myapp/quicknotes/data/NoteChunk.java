package com.myapp.quicknotes.data;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

// One passage of a note and its embedding, for search by meaning. The passage is stored as a
// position in the note's content rather than a copy of its text, so it can't drift from the note.
// Deleting the note deletes its passages.
@Entity(
        tableName = "note_chunks",
        foreignKeys = @ForeignKey(
                entity = Note.class,
                parentColumns = "id",
                childColumns = "note_id",
                onDelete = ForeignKey.CASCADE),
        indices = @Index("note_id"))
public class NoteChunk {
    @PrimaryKey(autoGenerate = true)
    private final long id;
    @ColumnInfo(name = "note_id")
    private final long noteId;
    // Where the passage is in the note's content. A note with no content is embedded by its
    // title alone, as one passage with both positions 0.
    @ColumnInfo(name = "start_offset")
    private final int startOffset;
    @ColumnInfo(name = "end_offset")
    private final int endOffset;
    // Which model made the vector. Vectors from different models can't be compared, so a note
    // indexed by another model is indexed again.
    @ColumnInfo(name = "model_id")
    @NonNull
    private final String modelId;
    // A fingerprint of the title and content that were embedded, the same in all of a note's
    // passages. When it no longer matches the note, the note is indexed again.
    @ColumnInfo(name = "source_hash")
    @NonNull
    private final String sourceHash;
    // The embedding, of length 1, as little-endian floats (search.Vectors.toBytes).
    @NonNull
    private final byte[] vector;

    public NoteChunk(long id, long noteId, int startOffset, int endOffset, @NonNull String modelId,
                     @NonNull String sourceHash, @NonNull byte[] vector) {
        this.id = id;
        this.noteId = noteId;
        this.startOffset = startOffset;
        this.endOffset = endOffset;
        this.modelId = modelId;
        this.sourceHash = sourceHash;
        this.vector = vector;
    }

    public long getId() {
        return id;
    }

    public long getNoteId() {
        return noteId;
    }

    public int getStartOffset() {
        return startOffset;
    }

    public int getEndOffset() {
        return endOffset;
    }

    @NonNull
    public String getModelId() {
        return modelId;
    }

    @NonNull
    public String getSourceHash() {
        return sourceHash;
    }

    @NonNull
    public byte[] getVector() {
        return vector;
    }
}

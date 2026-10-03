package com.myapp.quicknotes.data;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Ignore;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import java.util.Objects;

// A reminder points at a note instead of copying its text, so the notification always shows the
// note as it is when the reminder fires. Deleting the note deletes its reminders.
@Entity(
        tableName = "reminders",
        foreignKeys = @ForeignKey(
                entity = Note.class,
                parentColumns = "id",
                childColumns = "note_id",
                onDelete = ForeignKey.CASCADE),
        indices = @Index("note_id"))
public class Reminder {
    @PrimaryKey(autoGenerate = true)
    private final long id;
    @ColumnInfo(name = "note_id")
    private final long noteId;
    @ColumnInfo(name = "trigger_at")
    private final long triggerAt;

    public Reminder(long id, long noteId, long triggerAt) {
        this.id = id;
        this.noteId = noteId;
        this.triggerAt = triggerAt;
    }

    @Ignore
    public static Reminder atTime(long noteId, long triggerAt) {
        return new Reminder(0, noteId, triggerAt);
    }

    public Reminder withId(long id) {
        return new Reminder(id, noteId, triggerAt);
    }

    public long getId() {
        return id;
    }

    public long getNoteId() {
        return noteId;
    }

    // When the reminder fires, in milliseconds since the epoch.
    public long getTriggerAt() {
        return triggerAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Reminder)) return false;
        Reminder other = (Reminder) o;
        return id == other.id && noteId == other.noteId && triggerAt == other.triggerAt;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, noteId, triggerAt);
    }
}

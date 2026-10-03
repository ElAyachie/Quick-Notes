package com.myapp.quicknotes.data;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Embedded;

import java.util.Objects;

// A reminder together with the title of its note, for the reminders list.
public class ReminderWithNote {
    @Embedded
    @NonNull
    private final Reminder reminder;
    @ColumnInfo(name = "note_title")
    @NonNull
    private final String noteTitle;

    public ReminderWithNote(@NonNull Reminder reminder, @NonNull String noteTitle) {
        this.reminder = reminder;
        this.noteTitle = noteTitle;
    }

    @NonNull
    public Reminder getReminder() {
        return reminder;
    }

    @NonNull
    public String getNoteTitle() {
        return noteTitle;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ReminderWithNote)) return false;
        ReminderWithNote other = (ReminderWithNote) o;
        return reminder.equals(other.reminder) && noteTitle.equals(other.noteTitle);
    }

    @Override
    public int hashCode() {
        return Objects.hash(reminder, noteTitle);
    }
}

package com.myapp.quicknotes.data;

import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface ReminderDao {
    @Query("SELECT reminders.*, notes.title AS note_title FROM reminders "
            + "INNER JOIN notes ON notes.id = reminders.note_id "
            + "ORDER BY reminders.trigger_at")
    LiveData<List<ReminderWithNote>> observeAll();

    @Query("SELECT * FROM reminders")
    List<Reminder> getAll();

    @Nullable
    @Query("SELECT * FROM reminders WHERE id = :id")
    Reminder getById(long id);

    @Query("SELECT id FROM reminders WHERE note_id = :noteId")
    List<Long> getIdsForNote(long noteId);

    @Query("SELECT reminders.id FROM reminders "
            + "INNER JOIN notes ON notes.id = reminders.note_id "
            + "WHERE notes.folder_id = :folderId")
    List<Long> getIdsForFolder(long folderId);

    @Insert
    long insert(Reminder reminder);

    @Query("DELETE FROM reminders WHERE id = :id")
    void deleteById(long id);
}

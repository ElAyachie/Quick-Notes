package com.myapp.quicknotes.data;

import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Transaction;
import androidx.room.Update;

import java.util.List;

@Dao
public interface ReminderDao {
    // Reminders still waiting to fire: time reminders first, soonest at the top; then location
    // reminders, oldest first.
    @Query("SELECT reminders.*, notes.title AS note_title FROM reminders "
            + "INNER JOIN notes ON notes.id = reminders.note_id "
            + "WHERE reminders.fired_at IS NULL "
            + "ORDER BY reminders.trigger_at IS NULL, reminders.trigger_at, reminders.id")
    LiveData<List<ReminderWithNote>> observeUpcoming();

    // Reminders that have fired, most recent first.
    @Query("SELECT reminders.*, notes.title AS note_title FROM reminders "
            + "INNER JOIN notes ON notes.id = reminders.note_id "
            + "WHERE reminders.fired_at IS NOT NULL "
            + "ORDER BY reminders.fired_at DESC, reminders.id DESC")
    LiveData<List<ReminderWithNote>> observeHistory();

    @Query("SELECT * FROM reminders WHERE fired_at IS NULL")
    List<Reminder> getUpcoming();

    @Nullable
    @Query("SELECT * FROM reminders WHERE id = :id")
    Reminder getById(long id);

    @Query("SELECT * FROM reminders WHERE note_id = :noteId ORDER BY id")
    List<Reminder> getForNote(long noteId);

    @Query("SELECT reminders.* FROM reminders "
            + "INNER JOIN notes ON notes.id = reminders.note_id "
            + "WHERE notes.folder_id = :folderId ORDER BY reminders.id")
    List<Reminder> getForFolder(long folderId);

    @Insert
    long insert(Reminder reminder);

    @Update
    void update(Reminder reminder);

    @Query("UPDATE reminders SET fired_at = :firedAt WHERE id = :id")
    void markFired(long id, long firedAt);

    // Moves a repeating reminder to history and stores its next occurrence in one step, so the
    // series can't be cut short by the app being killed in between. Returns the new row's id.
    @Transaction
    default long markFiredAndInsertNext(long firedId, long firedAt, Reminder next) {
        markFired(firedId, firedAt);
        return insert(next);
    }

    @Query("DELETE FROM reminders WHERE id = :id")
    void deleteById(long id);
}

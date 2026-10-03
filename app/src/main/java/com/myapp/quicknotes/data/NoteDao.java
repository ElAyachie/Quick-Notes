package com.myapp.quicknotes.data;

import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface NoteDao {
    @Query("SELECT * FROM notes WHERE folder_id = :folderId ORDER BY updated_at DESC")
    LiveData<List<Note>> observeInFolder(long folderId);

    @Nullable
    @Query("SELECT * FROM notes WHERE id = :id")
    Note getById(long id);

    @Insert
    long insert(Note note);

    @Update
    void update(Note note);

    @Query("DELETE FROM notes WHERE id = :id")
    void deleteById(long id);
}

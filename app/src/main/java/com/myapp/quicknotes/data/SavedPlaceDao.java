package com.myapp.quicknotes.data;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface SavedPlaceDao {
    // In the order they were saved, so the list doesn't shuffle as places are used.
    @Query("SELECT * FROM saved_places ORDER BY id")
    LiveData<List<SavedPlace>> observeAll();

    @Query("SELECT * FROM saved_places ORDER BY id")
    List<SavedPlace> getAll();

    @Insert
    long insert(SavedPlace place);

    @Update
    void update(SavedPlace place);

    @Query("DELETE FROM saved_places WHERE id = :id")
    void deleteById(long id);
}

package com.myapp.quicknotes.data;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface FolderDao {
    // Oldest first, which keeps the default folder at the top.
    @Query("SELECT * FROM folders ORDER BY id")
    LiveData<List<Folder>> observeAll();

    @Query("SELECT COUNT(*) FROM folders WHERE name = :name")
    int countByName(String name);

    @Insert
    long insert(Folder folder);

    @Query("DELETE FROM folders WHERE id = :id")
    void deleteById(long id);
}

package com.myapp.quicknotes.data;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import java.util.Objects;

// A folder groups notes. Deleting a folder deletes the notes inside it.
@Entity(tableName = "folders", indices = @Index(value = "name", unique = true))
public class Folder {
    // The folder every install starts with. It can't be deleted, so a note always has somewhere to go.
    public static final long DEFAULT_ID = 1;
    public static final String DEFAULT_NAME = "Unclassified";

    @PrimaryKey(autoGenerate = true)
    private final long id;
    @NonNull
    private final String name;

    public Folder(long id, @NonNull String name) {
        this.id = id;
        this.name = name;
    }

    @Ignore
    public Folder(@NonNull String name) {
        this(0, name);
    }

    public long getId() {
        return id;
    }

    @NonNull
    public String getName() {
        return name;
    }

    public boolean isDefault() {
        return id == DEFAULT_ID;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Folder)) return false;
        Folder other = (Folder) o;
        return id == other.id && name.equals(other.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name);
    }

    // Shown as the row label when folders are listed in a spinner.
    @NonNull
    @Override
    public String toString() {
        return name;
    }
}

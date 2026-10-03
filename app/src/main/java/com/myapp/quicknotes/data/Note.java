package com.myapp.quicknotes.data;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Ignore;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import java.util.Objects;

// A note is identified by its id, so its title can change (or repeat) freely.
@Entity(
        tableName = "notes",
        foreignKeys = @ForeignKey(
                entity = Folder.class,
                parentColumns = "id",
                childColumns = "folder_id",
                onDelete = ForeignKey.CASCADE),
        indices = @Index("folder_id"))
public class Note {
    @PrimaryKey(autoGenerate = true)
    private final long id;
    @ColumnInfo(name = "folder_id")
    private final long folderId;
    @NonNull
    private final String title;
    @NonNull
    private final String content;
    @ColumnInfo(name = "created_at")
    private final long createdAt;
    @ColumnInfo(name = "updated_at")
    private final long updatedAt;

    public Note(long id, long folderId, @NonNull String title, @NonNull String content,
                long createdAt, long updatedAt) {
        this.id = id;
        this.folderId = folderId;
        this.title = title;
        this.content = content;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    // An empty note that has not been saved yet.
    @Ignore
    public static Note blank(long folderId) {
        long now = System.currentTimeMillis();
        return new Note(0, folderId, "", "", now, now);
    }

    public Note edited(@NonNull String title, @NonNull String content, long folderId) {
        return new Note(id, folderId, title, content, createdAt, System.currentTimeMillis());
    }

    public Note withId(long id) {
        return new Note(id, folderId, title, content, createdAt, updatedAt);
    }

    public long getId() {
        return id;
    }

    public long getFolderId() {
        return folderId;
    }

    @NonNull
    public String getTitle() {
        return title;
    }

    @NonNull
    public String getContent() {
        return content;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public long getUpdatedAt() {
        return updatedAt;
    }

    public boolean isNew() {
        return id == 0;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Note)) return false;
        Note other = (Note) o;
        return id == other.id
                && folderId == other.folderId
                && createdAt == other.createdAt
                && updatedAt == other.updatedAt
                && title.equals(other.title)
                && content.equals(other.content);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, folderId, title, content, createdAt, updatedAt);
    }
}

package com.myapp.quicknotes.search;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Objects;

// A note found by a search, with the passage that best explains why, and the reminder if it was
// found through one.
public class SearchResult {
    private final long noteId;
    private final long folderId;
    @NonNull
    private final String title;
    @NonNull
    private final String folderName;
    @NonNull
    private final String snippet;
    private final long updatedAt;
    @Nullable
    private final ReminderMatcher.Match reminderMatch;

    public SearchResult(long noteId, long folderId, @NonNull String title,
                        @NonNull String folderName, @NonNull String snippet, long updatedAt,
                        @Nullable ReminderMatcher.Match reminderMatch) {
        this.noteId = noteId;
        this.folderId = folderId;
        this.title = title;
        this.folderName = folderName;
        this.snippet = snippet;
        this.updatedAt = updatedAt;
        this.reminderMatch = reminderMatch;
    }

    public long getNoteId() {
        return noteId;
    }

    public long getFolderId() {
        return folderId;
    }

    @NonNull
    public String getTitle() {
        return title;
    }

    @NonNull
    public String getFolderName() {
        return folderName;
    }

    @NonNull
    public String getSnippet() {
        return snippet;
    }

    public long getUpdatedAt() {
        return updatedAt;
    }

    @Nullable
    public ReminderMatcher.Match getReminderMatch() {
        return reminderMatch;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SearchResult)) return false;
        SearchResult other = (SearchResult) o;
        return noteId == other.noteId
                && folderId == other.folderId
                && updatedAt == other.updatedAt
                && title.equals(other.title)
                && folderName.equals(other.folderName)
                && snippet.equals(other.snippet)
                && Objects.equals(reminderMatch, other.reminderMatch);
    }

    @Override
    public int hashCode() {
        return Objects.hash(noteId, folderId, title, folderName, snippet, updatedAt,
                reminderMatch);
    }
}

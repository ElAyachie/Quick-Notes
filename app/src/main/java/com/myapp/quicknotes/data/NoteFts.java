package com.myapp.quicknotes.data;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Fts4;
import androidx.room.FtsOptions;

// SQLite's full-text index over note titles and content, for search by words. It holds no text
// of its own: it reads from the notes table, and triggers Room creates keep it up to date as notes
// are added, changed and deleted. The unicode61 tokenizer ignores case and accents in any language.
@Fts4(contentEntity = Note.class, tokenizer = FtsOptions.TOKENIZER_UNICODE61)
@Entity(tableName = "notes_fts")
public class NoteFts {
    @NonNull
    private final String title;
    @NonNull
    private final String content;

    public NoteFts(@NonNull String title, @NonNull String content) {
        this.title = title;
        this.content = content;
    }

    @NonNull
    public String getTitle() {
        return title;
    }

    @NonNull
    public String getContent() {
        return content;
    }
}

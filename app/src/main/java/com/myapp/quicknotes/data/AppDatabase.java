package com.myapp.quicknotes.data;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.sqlite.db.SupportSQLiteDatabase;

@Database(entities = {Folder.class, Note.class, Reminder.class}, version = 1)
public abstract class AppDatabase extends RoomDatabase {
    private static final String FILE_NAME = "quicknotes.db";

    public abstract FolderDao folderDao();

    public abstract NoteDao noteDao();

    public abstract ReminderDao reminderDao();

    public static AppDatabase create(Context context) {
        return Room.databaseBuilder(context, AppDatabase.class, FILE_NAME)
                .addCallback(SEED_DEFAULT_FOLDER)
                .build();
    }

    // A database that lives only in memory, for tests.
    public static AppDatabase createInMemory(Context context) {
        return Room.inMemoryDatabaseBuilder(context, AppDatabase.class)
                .addCallback(SEED_DEFAULT_FOLDER)
                .build();
    }

    private static final Callback SEED_DEFAULT_FOLDER = new Callback() {
        @Override
        public void onCreate(@NonNull SupportSQLiteDatabase db) {
            db.execSQL("INSERT INTO folders (id, name) VALUES (?, ?)",
                    new Object[]{Folder.DEFAULT_ID, Folder.DEFAULT_NAME});
        }
    };
}

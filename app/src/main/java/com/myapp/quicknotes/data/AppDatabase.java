package com.myapp.quicknotes.data;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

@Database(entities = {Folder.class, Note.class, Reminder.class}, version = 3)
public abstract class AppDatabase extends RoomDatabase {
    private static final String FILE_NAME = "quicknotes.db";

    public abstract FolderDao folderDao();

    public abstract NoteDao noteDao();

    public abstract ReminderDao reminderDao();

    public static AppDatabase create(Context context) {
        return Room.databaseBuilder(context, AppDatabase.class, FILE_NAME)
                .addCallback(SEED_DEFAULT_FOLDER)
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
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

    // Version 2 adds location reminders: a reminder now says what kind it is, and carries either
    // a trigger time or a place. SQLite can't make trigger_at nullable in place, so the table is
    // rebuilt; every existing reminder is a time reminder.
    public static final Migration MIGRATION_1_2 = new Migration(1, 2) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase db) {
            db.execSQL("CREATE TABLE reminders_new ("
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, "
                    + "note_id INTEGER NOT NULL, "
                    + "type TEXT NOT NULL, "
                    + "trigger_at INTEGER, "
                    + "latitude REAL, "
                    + "longitude REAL, "
                    + "radius_meters REAL, "
                    + "place_name TEXT, "
                    + "FOREIGN KEY(note_id) REFERENCES notes(id) "
                    + "ON UPDATE NO ACTION ON DELETE CASCADE)");
            db.execSQL("INSERT INTO reminders_new (id, note_id, type, trigger_at) "
                    + "SELECT id, note_id, 'TIME', trigger_at FROM reminders");
            db.execSQL("DROP TABLE reminders");
            db.execSQL("ALTER TABLE reminders_new RENAME TO reminders");
            db.execSQL("CREATE INDEX index_reminders_note_id ON reminders (note_id)");
        }
    };

    // Version 3 keeps reminders after they fire, as history, and lets them repeat. Reminders
    // stored before this are all still waiting and none of them repeats.
    public static final Migration MIGRATION_2_3 = new Migration(2, 3) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase db) {
            db.execSQL("ALTER TABLE reminders ADD COLUMN repeat TEXT NOT NULL DEFAULT 'NONE'");
            db.execSQL("ALTER TABLE reminders ADD COLUMN first_trigger_at INTEGER");
            db.execSQL("ALTER TABLE reminders ADD COLUMN fired_at INTEGER");
            db.execSQL("UPDATE reminders SET first_trigger_at = trigger_at");
        }
    };
}

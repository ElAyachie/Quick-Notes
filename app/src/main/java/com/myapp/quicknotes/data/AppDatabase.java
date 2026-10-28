package com.myapp.quicknotes.data;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

@Database(
        entities = {Folder.class, Note.class, Reminder.class, NoteChunk.class, NoteFts.class,
                PlaceLookup.class, NearbyPlace.class, SavedPlace.class},
        version = 8)
public abstract class AppDatabase extends RoomDatabase {
    private static final String FILE_NAME = "quicknotes.db";

    public abstract FolderDao folderDao();

    public abstract NoteDao noteDao();

    public abstract ReminderDao reminderDao();

    public abstract SearchDao searchDao();

    public abstract PlaceDao placeDao();

    public abstract SavedPlaceDao savedPlaceDao();

    public static AppDatabase create(Context context) {
        return Room.databaseBuilder(context, AppDatabase.class, FILE_NAME)
                .addCallback(SEED_DEFAULT_FOLDER)
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5,
                        MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8)
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

    // Version 4 lets a time reminder repeat on chosen days of the week. No stored reminder does
    // yet, so the set of days starts out empty.
    public static final Migration MIGRATION_3_4 = new Migration(3, 4) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase db) {
            db.execSQL("ALTER TABLE reminders ADD COLUMN repeat_days INTEGER NOT NULL DEFAULT 0");
        }
    };

    // Version 5 lets a location reminder fire on leaving its place. Every stored one fires on
    // arriving.
    public static final Migration MIGRATION_4_5 = new Migration(4, 5) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase db) {
            db.execSQL("ALTER TABLE reminders "
                    + "ADD COLUMN place_trigger TEXT NOT NULL DEFAULT 'ARRIVING'");
        }
    };

    // Version 6 adds search: a full-text index of the notes, filled here from the notes already
    // stored, and a table of embedded passages, filled by search.SearchRepository when the app
    // starts. The statements are Room's own (app/schemas/.../6.json), so the result matches what
    // Room creates for a new database.
    public static final Migration MIGRATION_5_6 = new Migration(5, 6) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase db) {
            db.execSQL("CREATE TABLE IF NOT EXISTS `note_chunks` ("
                    + "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, "
                    + "`note_id` INTEGER NOT NULL, "
                    + "`start_offset` INTEGER NOT NULL, "
                    + "`end_offset` INTEGER NOT NULL, "
                    + "`model_id` TEXT NOT NULL, "
                    + "`source_hash` TEXT NOT NULL, "
                    + "`vector` BLOB NOT NULL, "
                    + "FOREIGN KEY(`note_id`) REFERENCES `notes`(`id`) "
                    + "ON UPDATE NO ACTION ON DELETE CASCADE )");
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_note_chunks_note_id` "
                    + "ON `note_chunks` (`note_id`)");
            db.execSQL("CREATE VIRTUAL TABLE IF NOT EXISTS `notes_fts` USING FTS4("
                    + "`title` TEXT NOT NULL, `content` TEXT NOT NULL, "
                    + "tokenize=unicode61, content=`notes`)");
            for (String sql : FTS_TRIGGERS) {
                db.execSQL(sql);
            }
            db.execSQL("INSERT INTO notes_fts(notes_fts) VALUES ('rebuild')");
        }
    };

    // Version 7 keeps what is around each location reminder's place, looked up from
    // OpenStreetMap, so search can find a reminder by a shop or street near it. Existing reminders
    // are looked up when the app starts.
    public static final Migration MIGRATION_6_7 = new Migration(6, 7) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase db) {
            db.execSQL("CREATE TABLE IF NOT EXISTS `place_lookups` ("
                    + "`place_key` TEXT NOT NULL, `looked_up_at` INTEGER NOT NULL, "
                    + "PRIMARY KEY(`place_key`))");
            db.execSQL("CREATE TABLE IF NOT EXISTS `nearby_places` ("
                    + "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, "
                    + "`place_key` TEXT NOT NULL, `name` TEXT NOT NULL, `kind` TEXT NOT NULL, "
                    + "`distance_meters` INTEGER NOT NULL, "
                    + "FOREIGN KEY(`place_key`) REFERENCES `place_lookups`(`place_key`) "
                    + "ON UPDATE NO ACTION ON DELETE CASCADE )");
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_nearby_places_place_key` "
                    + "ON `nearby_places` (`place_key`)");
        }
    };

    // Version 8 lets the user save places by name for location reminders. None are saved yet.
    public static final Migration MIGRATION_7_8 = new Migration(7, 8) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase db) {
            db.execSQL("CREATE TABLE IF NOT EXISTS `saved_places` ("
                    + "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, "
                    + "`latitude` REAL NOT NULL, `longitude` REAL NOT NULL)");
        }
    };

    // Keep the full-text index in step with the notes table.
    private static final String[] FTS_TRIGGERS = {
            "CREATE TRIGGER IF NOT EXISTS room_fts_content_sync_notes_fts_BEFORE_UPDATE "
                    + "BEFORE UPDATE ON `notes` BEGIN "
                    + "DELETE FROM `notes_fts` WHERE `docid`=OLD.`rowid`; END",
            "CREATE TRIGGER IF NOT EXISTS room_fts_content_sync_notes_fts_BEFORE_DELETE "
                    + "BEFORE DELETE ON `notes` BEGIN "
                    + "DELETE FROM `notes_fts` WHERE `docid`=OLD.`rowid`; END",
            "CREATE TRIGGER IF NOT EXISTS room_fts_content_sync_notes_fts_AFTER_UPDATE "
                    + "AFTER UPDATE ON `notes` BEGIN "
                    + "INSERT INTO `notes_fts`(`docid`, `title`, `content`) "
                    + "VALUES (NEW.`rowid`, NEW.`title`, NEW.`content`); END",
            "CREATE TRIGGER IF NOT EXISTS room_fts_content_sync_notes_fts_AFTER_INSERT "
                    + "AFTER INSERT ON `notes` BEGIN "
                    + "INSERT INTO `notes_fts`(`docid`, `title`, `content`) "
                    + "VALUES (NEW.`rowid`, NEW.`title`, NEW.`content`); END",
    };
}

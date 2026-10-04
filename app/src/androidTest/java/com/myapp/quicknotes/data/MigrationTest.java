package com.myapp.quicknotes.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

import androidx.room.Room;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

@RunWith(AndroidJUnit4.class)
public class MigrationTest {
    private static final String TEST_DB = "migration-test.db";

    private final Context context = ApplicationProvider.getApplicationContext();
    private AppDatabase migrated;

    @Before
    public void deleteLeftovers() {
        context.deleteDatabase(TEST_DB);
    }

    @After
    public void cleanUp() {
        if (migrated != null) {
            migrated.close();
        }
        context.deleteDatabase(TEST_DB);
    }

    @Test
    public void version1TimeReminderSurvivesMigrationToTheCurrentVersion() throws Exception {
        SQLiteDatabase v1 = createDatabaseAtVersion(1);
        v1.execSQL("INSERT INTO folders (id, name) VALUES (1, 'Unclassified')");
        v1.execSQL("INSERT INTO notes (id, folder_id, title, content, created_at, updated_at) "
                + "VALUES (7, 1, 'Dentist', 'Bring card', 100, 200)");
        v1.execSQL("INSERT INTO reminders (id, note_id, trigger_at) VALUES (3, 7, 123456789)");
        v1.close();

        Reminder reminder = openMigrated().reminderDao().getById(3);

        assertNotNull(reminder);
        assertEquals(7, reminder.getNoteId());
        assertEquals(ReminderType.TIME, reminder.getType());
        assertEquals(Long.valueOf(123456789), reminder.getTriggerAt());
        assertNull(reminder.getLatitude());
        assertEquals(Repeat.NONE, reminder.getRepeat());
        assertEquals(Long.valueOf(123456789), reminder.getFirstTriggerAt());
        assertNull(reminder.getFiredAt());

        // The link to the note still cascades after the table was rebuilt.
        migrated.noteDao().deleteById(7);
        assertNull(migrated.reminderDao().getById(3));
    }

    @Test
    public void version2LocationReminderSurvivesMigrationToTheCurrentVersion() throws Exception {
        SQLiteDatabase v2 = createDatabaseAtVersion(2);
        v2.execSQL("INSERT INTO folders (id, name) VALUES (1, 'Unclassified')");
        v2.execSQL("INSERT INTO notes (id, folder_id, title, content, created_at, updated_at) "
                + "VALUES (7, 1, 'Milk', '', 100, 200)");
        v2.execSQL("INSERT INTO reminders "
                + "(id, note_id, type, latitude, longitude, radius_meters, place_name) "
                + "VALUES (4, 7, 'LOCATION', 42.4, -71.0, 250, 'Shop')");
        v2.close();

        AppDatabase database = openMigrated();
        Reminder reminder = database.reminderDao().getById(4);

        assertNotNull(reminder);
        assertEquals(ReminderType.LOCATION, reminder.getType());
        assertEquals(Double.valueOf(42.4), reminder.getLatitude());
        assertEquals("Shop", reminder.getPlaceName());
        assertEquals(PlaceTrigger.ARRIVING, reminder.getPlaceTrigger());
        assertEquals(Repeat.NONE, reminder.getRepeat());
        assertNull(reminder.getFiredAt());
        // Still waiting, so it is among the reminders that get scheduled.
        assertEquals(1, database.reminderDao().getUpcoming().size());
    }

    @Test
    public void version3RepeatingReminderAndHistorySurviveMigrationToTheCurrentVersion()
            throws Exception {
        SQLiteDatabase v3 = createDatabaseAtVersion(3);
        v3.execSQL("INSERT INTO folders (id, name) VALUES (1, 'Unclassified')");
        v3.execSQL("INSERT INTO notes (id, folder_id, title, content, created_at, updated_at) "
                + "VALUES (7, 1, 'Pills', '', 100, 200)");
        v3.execSQL("INSERT INTO reminders "
                + "(id, note_id, type, trigger_at, repeat, first_trigger_at, fired_at) "
                + "VALUES (5, 7, 'TIME', 1000, 'DAILY', 1000, 1500)");
        v3.execSQL("INSERT INTO reminders "
                + "(id, note_id, type, trigger_at, repeat, first_trigger_at) "
                + "VALUES (6, 7, 'TIME', 86401000, 'DAILY', 1000)");
        v3.close();

        AppDatabase database = openMigrated();
        Reminder fired = database.reminderDao().getById(5);
        Reminder waiting = database.reminderDao().getById(6);

        assertNotNull(fired);
        assertEquals(Long.valueOf(1500), fired.getFiredAt());
        assertNotNull(waiting);
        assertEquals(Repeat.DAILY, waiting.getRepeat());
        assertEquals(DaysOfWeek.NONE, waiting.getRepeatDays());
        assertEquals(Long.valueOf(1000), waiting.getFirstTriggerAt());
        assertEquals(1, database.reminderDao().getUpcoming().size());
    }

    @Test
    public void version4RemindersSurviveMigrationToTheCurrentVersion() throws Exception {
        SQLiteDatabase v4 = createDatabaseAtVersion(4);
        v4.execSQL("INSERT INTO folders (id, name) VALUES (1, 'Unclassified')");
        v4.execSQL("INSERT INTO notes (id, folder_id, title, content, created_at, updated_at) "
                + "VALUES (7, 1, 'Gym', '', 100, 200)");
        v4.execSQL("INSERT INTO reminders "
                + "(id, note_id, type, trigger_at, repeat, repeat_days, first_trigger_at) "
                + "VALUES (8, 7, 'TIME', 1000, 'DAYS_OF_WEEK', 21, 1000)");
        v4.execSQL("INSERT INTO reminders "
                + "(id, note_id, type, latitude, longitude, radius_meters, place_name, repeat, "
                + "repeat_days) "
                + "VALUES (9, 7, 'LOCATION', 42.4, -71.0, 250, 'Gym', 'EVERY_ARRIVAL', 0)");
        v4.close();

        AppDatabase database = openMigrated();
        Reminder weekdays = database.reminderDao().getById(8);
        Reminder place = database.reminderDao().getById(9);

        assertNotNull(weekdays);
        assertEquals(Repeat.DAYS_OF_WEEK, weekdays.getRepeat());
        assertEquals(21, weekdays.getRepeatDays());
        // Every location reminder stored before version 5 fires on arriving.
        assertNotNull(place);
        assertEquals(PlaceTrigger.ARRIVING, place.getPlaceTrigger());
        assertEquals(Repeat.EVERY_ARRIVAL, place.getRepeat());
        assertEquals("Gym", place.getPlaceName());
        assertEquals(2, database.reminderDao().getUpcoming().size());
    }

    // Opening the database runs the migrations. Room then compares the resulting tables with the
    // current entities and throws if they differ.
    private AppDatabase openMigrated() {
        migrated = Room.databaseBuilder(context, AppDatabase.class, TEST_DB)
                .addMigrations(AppDatabase.MIGRATION_1_2, AppDatabase.MIGRATION_2_3,
                        AppDatabase.MIGRATION_3_4, AppDatabase.MIGRATION_4_5)
                .build();
        return migrated;
    }

    // Builds an empty database exactly as Room created it at the given version, from the schema
    // Room exported at the time (app/schemas, packaged into the test APK as assets).
    private SQLiteDatabase createDatabaseAtVersion(int version) throws Exception {
        JSONObject schema = new JSONObject(readTestAsset(
                AppDatabase.class.getName() + "/" + version + ".json")).getJSONObject("database");
        SQLiteDatabase db = SQLiteDatabase.openOrCreateDatabase(
                context.getDatabasePath(TEST_DB), null);
        JSONArray entities = schema.getJSONArray("entities");
        for (int i = 0; i < entities.length(); i++) {
            JSONObject entity = entities.getJSONObject(i);
            String table = entity.getString("tableName");
            db.execSQL(entity.getString("createSql").replace("${TABLE_NAME}", table));
            JSONArray indices = entity.optJSONArray("indices");
            for (int j = 0; indices != null && j < indices.length(); j++) {
                db.execSQL(indices.getJSONObject(j).getString("createSql")
                        .replace("${TABLE_NAME}", table));
            }
        }
        JSONArray setupQueries = schema.getJSONArray("setupQueries");
        for (int i = 0; i < setupQueries.length(); i++) {
            db.execSQL(setupQueries.getString(i));
        }
        db.setVersion(version);
        return db;
    }

    private static String readTestAsset(String path) throws Exception {
        try (InputStream in = InstrumentationRegistry.getInstrumentation().getContext()
                .getAssets().open(path)) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buffer = new byte[8192];
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
            return out.toString(StandardCharsets.UTF_8.name());
        }
    }
}

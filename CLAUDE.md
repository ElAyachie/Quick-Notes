# Quick Notes

An Android note-taking app whose notes can remind you of themselves: at a time, on a
schedule, or when you arrive at a place. Published on Google Play as
`com.myapp.QuickNotes1`. The owner has further ambitions for it that are not yet specified.

For what was built, when and why, read `docs/HISTORY.md`. For this computer's paths,
emulators and build commands, read `CLAUDE.local.md` (not committed; may not exist on
another machine).

## Fixed decisions

- **Java and XML layouts.** The owner declined a move to Kotlin or Compose. Do not propose it again.
- **Single activity**, one fragment and view model per screen, wired in `res/navigation/nav_graph.xml`.
- **Material 3**, with the brand red (`@color/app_bar`) kept for the app bar, tabs and buttons in both themes.
- **The old text-file storage is gone** and is not migrated.
- **The applicationId must stay `com.myapp.QuickNotes1`** so Play Store updates apply. The Java package is `com.myapp.quicknotes`.

## Where things are

Everything is under `app/src/main/java/com/myapp/quicknotes`:

| Package | Contents |
|---|---|
| (root) | `QuickNotesApp`, `AppContainer` (builds the database, repositories and reminder classes once), `AppExecutors` (one background thread for database work, results posted to the main thread) |
| `data` | Room: `Folder`, `Note`, `Reminder` entities, DAOs, one repository per table. `Repeat` and `DaysOfWeek` hold the recurrence arithmetic. Repositories are the only way the UI and receivers touch data. |
| `reminders` | `ReminderScheduler` (alarms and geofences), `ReminderAlarmReceiver`, `GeofenceReceiver`, `ReminderRestoreReceiver` (reboot, app update, exact-alarm permission change), `ReminderNotifier`, `ReminderActionReceiver` (the notification's Done and snooze buttons), `LocationAccess` |
| `ui` | `MainActivity` plus `home`, `notes`, `editor`, `placepicker`, `reminders`, `settings`, and shared helpers in `common` |

## How reminders work

These are the parts that are easy to break without knowing them:

- **A `reminders` row is one occurrence.** When it fires, `fired_at` is set and the row stays as history. A repeating reminder continues as a *new* row for its next occurrence (`Reminder.nextOccurrence`). "Upcoming" is `fired_at IS NULL`; "History" is the rest.
- **A reminder stores its note's id, not the note's text.** The notification reads the note when it fires. Deleting a note or folder cascades to its reminders, and the repositories cancel the alarms and geofences first.
- **Time reminders** are AlarmManager alarms, exact when the user has allowed it and otherwise inexact (the system may deliver up to an hour late).
- **Location reminders** are Google Play services geofences. Each is watched for one crossing only (`Reminder.getPlaceTrigger`): *entering* the circle for an arriving reminder, *exiting* it for a leaving one. An arriving reminder set while already inside waits for the next arrival.
- **Snoozing** a notification adds a new one-off time reminder for the same note (`ReminderRepository.snooze`). The reminder that fired stays in history, and a repeating series carries on untouched.
- **`ReminderScheduler` blocks until Play services answers**, so call it from the background thread only. This keeps a registration made inside a broadcast from being lost when the app is frozen.
- **Alarms and geofences do not survive a reboot or app update.** `ReminderRestoreReceiver` and app start both call `ReminderRepository.rescheduleAll()`.
- **Repeat kinds:** `DAILY`/`WEEKLY`/`MONTHLY`/`YEARLY` step from the *first* occurrence in local time (so "monthly on the 31st" returns to the 31st); `DAYS_OF_WEEK` uses the `repeat_days` set; `EVERY_ARRIVAL` is for location reminders, arriving or leaving.

## Database changes

The database is at **version 5**. Any schema change needs all of:

1. a bumped `version` in `AppDatabase` and a `Migration` added to `create()`;
2. the exported schema file in `app/schemas/` committed (Room writes it on build);
3. a test in `MigrationTest` that builds the previous version from its schema file and opens it with the migrations.

`MigrationTest` builds old databases by hand because Room's `MigrationTestHelper` crashes in this project (a `kotlinx.serialization` version clash).

## Build and test

```
gradlew assembleDebug              # debug APK
gradlew testDebugUnitTest          # JVM tests: recurrence and preset-time arithmetic
gradlew connectedDebugAndroidTest  # device tests: database and migrations
gradlew lintDebug
```

- JDK 17 or newer. AGP 9.4.1, Gradle 9.8.0, compile/target SDK 36, min SDK 26.
- Dependency versions live in `gradle/libs.versions.toml`.
- The map needs `MAP_API_KEY=...` in `local.properties` (not committed).
- `connectedDebugAndroidTest` runs on **every** connected device and uninstalls the app afterwards. Set `ANDROID_SERIAL` to the device you mean.
- Geofences cannot be tested by setting the emulator's location in its control panel. Feed fixes from a shell; the commands are in `README.md`.

## Working agreements

- **Do not push to `master` directly.** GitHub has a rule that changes go through a pull request. Work on a branch, push it, and open or hand over a pull request.
- The owner has given standing permission to commit and push finished, verified work to branches. Merging into `master` needs to be asked for.
- **Verify in the running app**, not only with tests: build, install on an emulator, and drive the affected screens in light and dark theme.
- Comments say what the code is for or why it is the way it is, in plain words. Match the surrounding style.
- When a piece of work is finished, add a dated entry to `docs/HISTORY.md` so the next session starts from the same picture.

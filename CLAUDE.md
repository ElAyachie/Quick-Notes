# Quick Notes

An Android note-taking app whose notes can remind you of themselves: at a time, on a
schedule, or when you arrive at a place, and that searches notes by meaning on the phone. Formerly on Google Play as `com.myapp.QuickNotes1`;
now distributed as signed APKs on GitHub releases (see "Releases" in `README.md`). The owner has further ambitions for it that are not yet specified.

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
| (root) | `QuickNotesApp`, `AppContainer` (builds the database, repositories and reminder classes once), `AppExecutors` (one background thread for database work, one for search, one for the network, results posted to the main thread) |
| `data` | Room: `Folder`, `Note`, `Reminder` entities, the search tables `NoteChunk` and `NoteFts` with `SearchDao`, the nearby-place tables `PlaceLookup` and `NearbyPlace` with `PlaceDao`, DAOs, one repository per table. `Repeat` and `DaysOfWeek` hold the recurrence arithmetic. Repositories are the only way the UI and receivers touch data. |
| `reminders` | `ReminderScheduler` (alarms and geofences), `ReminderAlarmReceiver`, `GeofenceReceiver`, `ReminderRestoreReceiver` (reboot, app update, exact-alarm permission change), `ReminderNotifier`, `ReminderActionReceiver` (the notification's Done and snooze buttons), `LocationAccess` |
| `search` | `SearchRepository` (indexing and searching), `TextChunker`, `UniversalSentenceEncoder` (MediaPipe) behind `Embedder`, `CrashGuardedEmbedder`, `SemanticRanker`, `KeywordScorer`, `TimeQuery` (times in a query), `ReminderMatcher` (reminders by time and place), `RankFusion`, `Vectors` |
| `places` | `NearbyPlacesRepository` (what is near a location reminder's place), `OverpassClient` and `OverpassParser` (OpenStreetMap), `PlaceKinds` (map tags to everyday words) |
| `ui` | `MainActivity` plus `home`, `notes`, `editor`, `placepicker`, `reminders`, `search`, `settings`, and shared helpers in `common` |

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

## How search works

`README.md` ("How search works") explains the design and the measured quality. The parts that are easy to break:

- **Saving a note indexes it.** `NoteRepository.saveNote` hands the saved note to `SearchRepository.indexNote`, which runs on the search thread. App start calls `indexAllNotes()` for notes whose passages are missing or were made from other text or another model.
- **A stored vector is only comparable with vectors from the same `Embedder.modelId()`.** Change the model id whenever the vectors would change (another model, other centering vectors); every note is then embedded again at the next start.
- **Queries and passages are embedded differently** (`embedQuery`, `embedPassage`). The Universal Sentence Encoder needs a centering vector for each, in `assets/universal_sentence_encoder_centering.json`. They come from `androidTest/assets/search_calibration.txt`, never from the evaluation set; `CenteringVectorsTest` checks them and logs new ones when they go stale.
- **Measure before changing ranking.** `SearchQualityTest` logs hit@1, hit@3 and MRR (tag `SearchQuality`) and fails below floors. `SearchRepository.MIN_SIMILARITY` was chosen from its sweep.
- **The full-text index `notes_fts` is kept up to date by triggers** Room declares; a migration that rebuilds `notes` must recreate them (they are in `AppDatabase.FTS_TRIGGERS`).
- **Search also matches waiting reminders** (`ReminderMatcher`): by the times a query names (`TimeQuery`) and by their places. A place matches by the name the user gave it, a nearby place's name, or a nearby place's kind; a kind only counts within 100 m of the pin and when it accounts for every word of the query (time words aside), and such a match ranks below meaning (`ReminderMatcher.Match.byKindOnly`). `PlaceKinds` synonyms must be other names for the same kind of place, never related topics ("money" for a bank matched every reminder in a city).
- **Nearby places are stored by place, not by reminder** (`PlaceLookup.keyOf`: coordinates and radius), so a repeating reminder's next occurrence keeps them. They are looked up when a location reminder is added or moved (`ReminderRepository`) and, for any missed, at app start, which also drops lookups no waiting reminder uses. Lookups go to OpenStreetMap's Overpass servers on the network thread, one at a time; the main server can take a minute. OpenStreetMap's licence needs the credit shown on the search screen.
- **MediaPipe runs native code.** A crash there can't be caught; `CrashGuardedEmbedder` turns search by meaning off on a device where the first attempt never finished. On the x86_64 test emulator the ARM release build crashes this way under ARM translation; the debug build (x86_64 libraries) works, and the 3.1 release works on a real phone.

## Database changes

The database is at **version 7**. Any schema change needs all of:

1. a bumped `version` in `AppDatabase` and a `Migration` added to `create()`;
2. the exported schema file in `app/schemas/` committed (Room writes it on build);
3. a test in `MigrationTest` that builds the previous version from its schema file and opens it with the migrations.

`MigrationTest` builds old databases by hand because Room's `MigrationTestHelper` crashes in this project (a `kotlinx.serialization` version clash).

## Build and test

```
gradlew assembleDebug              # debug APK
gradlew testDebugUnitTest          # JVM tests: recurrence, preset times, search ranking
gradlew connectedDebugAndroidTest  # device tests: database, migrations, search quality
gradlew lintDebug
```

- JDK 17 or newer. AGP 9.4.1, Gradle 9.8.0, compile/target SDK 36, min SDK 26.
- Dependency versions live in `gradle/libs.versions.toml`. MediaPipe stays on 0.10.35: 1.x bundles generative-AI code (about 25 MB of native library per processor type instead of 10), and 0.10.26.1 has no x86_64 build for the emulator.
- Backing up the emulator's database before device tests (which uninstall the app): run `PRAGMA wal_checkpoint(TRUNCATE)` through `run-as ... sqlite3` first, or recent changes stay in the `-wal` file and are lost.
- Release builds include only ARM native libraries (`abiFilters` in `app/build.gradle`), so a release APK can't be checked on the x86_64 emulator except through its ARM translation, where MediaPipe crashes.
- The map needs `MAP_API_KEY=...` in `local.properties` (not committed).
- `connectedDebugAndroidTest` runs on **every** connected device and uninstalls the app afterwards. Set `ANDROID_SERIAL` to the device you mean.
- Geofences cannot be tested by setting the emulator's location in its control panel. Feed fixes from a shell; the commands are in `README.md`.

## Secrets

The repository is public. Never commit keystores, API keys or `local.properties`. Release
signing happens in `.github/workflows/release.yml` from repository secrets.

## Working agreements

- **Do not push to `master` directly.** GitHub has a rule that changes go through a pull request. Work on a branch, push it, and open or hand over a pull request.
- The owner has given standing permission to commit and push finished, verified work to branches. Merging into `master` needs to be asked for.
- **Verify in the running app**, not only with tests: build, install on an emulator, and drive the affected screens in light and dark theme.
- Comments say what the code is for or why it is the way it is, in plain words. Match the surrounding style.
- When a piece of work is finished, add a dated entry to `docs/HISTORY.md` so the next session starts from the same picture.

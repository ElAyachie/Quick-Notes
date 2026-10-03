# Quick Notes

A note-taking app for Android whose notes can remind you of themselves.

## What it does

- **Notes** — create, edit, delete, and move between folders.
- **Folders** — create and delete. Deleting a folder deletes the notes in it. The
  "Unclassified" folder always exists.
- **Time reminders** — attach a date and time to a note; a notification shows the note
  when the time comes, and tapping it opens the note. A time reminder can repeat every
  day, week, month or year.
- **Location reminders** — pick a place on a map and how close counts as arriving
  (100 m to 2 km); the note is shown when you get there, even if the app is closed. The
  reminder fires on arriving, so one set while you are already at the place waits for
  your next visit. It can fire once, or on every visit.
- **Reminders screen** — "Upcoming" lists the reminders still waiting; tap one to edit
  it, open its note or delete it. "History" lists the ones that have fired.
- Reminders survive a reboot.
- **Light and dark theme**, or follow the system.

## How the code is organised

Java, XML layouts, a single activity. Everything lives under
`app/src/main/java/com/myapp/quicknotes`:

| Package | What's in it |
|---|---|
| `data` | The Room database: `Folder`, `Note` and `Reminder` tables, their DAOs, and one repository per table. Repositories are the only way the rest of the app touches data. |
| `reminders` | `ReminderScheduler` hands a reminder to the system: an alarm for a time, a geofence (Google Play services) for a place. `ReminderAlarmReceiver` and `GeofenceReceiver` run when one comes due, `ReminderNotifier` shows the notification, `ReminderRestoreReceiver` re-schedules everything after a reboot or app update. |
| `ui` | `MainActivity` owns the toolbar; each screen is a fragment with a view model (`home`, `notes`, `editor`, `placepicker`, `reminders`, `settings`). Screens are wired together in `res/navigation/nav_graph.xml`. |
| (root) | `QuickNotesApp` and `AppContainer` build the database, repositories and reminder classes once and share them. |

A reminder stores the id of its note, not a copy of the text, so the notification always
shows the note as it is when the reminder fires.

A row in the `reminders` table is one occurrence. When it fires it is stamped with the
time and stays as history; a repeating reminder continues as a new row for its next
occurrence (`Reminder.nextOccurrence`, `Repeat.nextAfter`).

## Building

Open the project in Android Studio and press Run, or from a terminal:

```
gradlew assembleDebug              # build the debug APK
gradlew testDebugUnitTest          # run the recurrence tests on your computer
gradlew connectedDebugAndroidTest  # run the database tests on a connected device or emulator
```

Requires JDK 17 or newer (Android Studio's bundled JDK works). `local.properties` must
point `sdk.dir` at your Android SDK; Android Studio writes it for you.

The place picker uses Google Maps. Put a Maps SDK for Android key in `local.properties`
as `MAP_API_KEY=...`; without it the app still runs but the map shows no tiles.

## Location reminders and permissions

A location reminder needs precise location and, on Android 10+, location access "all the
time", because the place is watched while the app is closed. The app explains this before
asking. To publish on Google Play, background location has to be declared and justified in
the Play Console.

Testing on an emulator: Google Play services only checks geofences when it receives new
location fixes, and the emulator's GPS reports none unless an app is asking. Feed fixes
from a shell instead:

```
adb shell appops set com.android.shell android:mock_location allow
adb shell cmd location providers add-test-provider gps
adb shell cmd location providers set-test-provider-enabled gps true
adb shell cmd location providers set-test-provider-location gps --location <lat>,<lng> --accuracy 5
```

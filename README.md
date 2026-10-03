# Quick Notes

A note-taking app for Android whose notes can remind you of themselves.

## What it does

- **Notes** — create, edit, delete, and move between folders.
- **Folders** — create and delete. Deleting a folder deletes the notes in it. The
  "Unclassified" folder always exists.
- **Time reminders** — attach a date and time to a note; a notification shows the note
  when the time comes, and tapping it opens the note. Reminders survive a reboot.
- **Light and dark theme**, or follow the system.

**Planned:** location reminders — be reminded of a note when you get close to a place.

## How the code is organised

Java, XML layouts, a single activity. Everything lives under
`app/src/main/java/com/myapp/quicknotes`:

| Package | What's in it |
|---|---|
| `data` | The Room database: `Folder`, `Note` and `Reminder` tables, their DAOs, and one repository per table. Repositories are the only way the rest of the app touches data. |
| `reminders` | `ReminderScheduler` hands a reminder to the system, `ReminderAlarmReceiver` runs when it comes due, `ReminderNotifier` shows the notification, `ReminderRestoreReceiver` re-schedules everything after a reboot or app update. |
| `ui` | `MainActivity` owns the toolbar; each screen is a fragment with a view model (`home`, `notes`, `editor`, `reminders`, `settings`). Screens are wired together in `res/navigation/nav_graph.xml`. |
| (root) | `QuickNotesApp` and `AppContainer` build the database, repositories and reminder classes once and share them. |

A reminder stores the id of its note, not a copy of the text, so the notification always
shows the note as it is when the reminder fires.

## Building

Open the project in Android Studio and press Run, or from a terminal:

```
gradlew assembleDebug              # build the debug APK
gradlew connectedDebugAndroidTest  # run the database tests on a connected device or emulator
```

Requires JDK 17 or newer (Android Studio's bundled JDK works). `local.properties` must
point `sdk.dir` at your Android SDK; Android Studio writes it for you.

# Project history

A running record of what was done to Quick Notes, the decisions behind it, and what is
still open. Newest work is at the bottom of the timeline. Add an entry when a piece of
work is finished.

## Where things stand (2026-10-03)

| Branch | Commit | Contains | State |
|---|---|---|---|
| `master` | `6586c3f` | Restructure, location reminders, repeating reminders, history, editing | On GitHub |
| `ui-polish` | `29c12e5` | `master` plus the Material 3 polish | Pushed, no pull request yet |
| `weekday-reminders` | `f31826d` | `ui-polish` plus repeating on chosen days of the week | Pushed, no pull request yet. One pull request from this branch covers both. |
| `restructure`, `location-reminders` | | Fully contained in `master` | Safe to delete |

The app has about 3,800 lines of Java. 36 automated tests pass: 23 JVM tests for the
recurrence arithmetic and 13 device tests for the database and its migrations.
Nothing has been tested on a physical phone yet; all verification was on emulators.

## What the app does today

- Notes in folders: create, edit, delete, move between folders. The "Unclassified" folder always exists.
- Time reminders that fire once or repeat every day, week, month or year, or on chosen days of the week.
- Location reminders set on a map (pin, radius 100 m to 2 km, optional name) that fire once or on every visit, with the app closed.
- A Reminders screen with Upcoming and History tabs. Upcoming reminders can be edited or deleted.
- Light, dark and system theme.

## Timeline

### Before October 2026

The app was written between 2018 and 2021 in Java: notes stored as text files, time
reminders through AlarmManager, and a location reminder that was started but not
finished. The last commit was in November 2021. In 2023 the project was upgraded to
Android Gradle Plugin 8 and some buttons restyled, but those changes were never
committed.

### 2026-10-02: taking stock

- The uncommitted 2023 changes were buried under a repository-wide change of line
  endings (99 files touched, 22 really changed). They were committed as a baseline with
  line endings normalised and a `.gitattributes` added.
- IDE settings and built `.aab` bundles were removed from version control.

### 2026-10-03: build modernisation (`4c5f15e`)

AGP 9.4.1, Gradle 9.8, compile and target SDK 36, min SDK 26, a version catalog. Target
SDK 31 was too old to publish updates on Google Play.

### 2026-10-03: restructure (`4b8dd41`)

The owner asked for the app to be made clean before adding location reminders.

- Storage moved from text files to a Room database. A note is identified by an id
  instead of its file name, so renaming or moving it is an update and titles may repeat.
- Code split into `data`, `reminders` and `ui`, with a single activity and a navigation
  graph. Dialogs and screens had been reaching into each other through static fields.
- Bugs fixed along the way: reminder times were parsed out of `Date.toString()` and
  broke in some time zones; reminders did not survive a reboot; every notification
  reused one id; the alarm receiver was exported; the location permission was requested
  but never declared.

### 2026-10-03: location reminders, repeats, history, editing (`6586c3f`)

Built together because they share the reminder model.

- Location reminders through Google Play services geofencing, with a Google Maps place
  picker.
- Repeating reminders. Each firing is kept as a history row and the next occurrence
  becomes a new row.
- Editing an upcoming reminder: the time questions again for a time reminder, the map
  again for a location reminder.
- Database versions 2 and 3.

### 2026-10-03: UI polish (`29c12e5`, branch `ui-polish`)

The owner asked for a more modern, clean look.

- Lists became flat tinted cards on white pages, with icons; folders show their note count.
- "New folder", "New note" and "Save" became extended floating action buttons.
- The note form got a large borderless title and an outlined folder selector.
- Empty states, a rounded panel over the map, vector icons throughout.

### 2026-10-03: repeat on chosen days of the week (`f31826d`, branch `weekday-reminders`)

"Like an alarm": a time plus ticked weekdays. Stored as `Repeat.DAYS_OF_WEEK` with a
`repeat_days` set. Database version 4.

## Decisions and their reasons

| Decision | Reason |
|---|---|
| Stay in Java with XML layouts | The owner judged a move to Kotlin "a bit much without much gain". Room, ViewModel, Navigation and geofencing all work from Java. |
| No import of the old text-file notes | The owner confirmed a fresh start was fine. |
| A map picker for location reminders | The owner's choice over "remind me when I'm back here". |
| A location reminder fires on entering only | So that "remind me when I get home", set at home, waits for the next arrival instead of firing at once. |
| One table for upcoming and fired reminders | A repeating reminder fires many times; one row per occurrence gives history and the next occurrence without a second table. |
| Repeats step from the first occurrence | Keeps "monthly on the 31st" on the 31st after a shorter month, and a daily time fixed across clock changes. |
| Inexact alarm when exact alarms are not allowed | The reminder still fires. The app offers the permission when a reminder is set. |
| The scheduler waits for Play services | A geofence registered from a broadcast could otherwise be lost when the app is frozen. |
| Red app bar, tabs and buttons kept in the polish | They are the app's identity; the polish changed what sits under them. |
| White pages with tinted cards | Off-white pages left a visible seam under the white editor and map panel. |
| Pull requests for `master` | GitHub has a rule requiring them. One direct push bypassed it by accident on 2026-10-03. |

## Open items

- **Pull request #1** for `weekday-reminders` (includes `ui-polish`) was opened on
  2026-10-03 and is waiting for the owner to merge it.
- **Google Play:** background location must be declared and justified in the Play
  Console before an update with location reminders is approved.
- **Not tested on a real phone.** On a device, a geofence notification can lag a few
  minutes behind arriving.
- No address search on the map; unnamed places show as coordinates.
- No "clear history"; entries are removed one at a time. Deleting a note removes its
  history entries.
- If location permission is revoked later, location reminders stop firing with no
  warning in the app. They are retried on each app start.
- Target SDK is 36; SDK 37 exists.

## Ideas discussed but not built

- **An iOS version.** Feasible, but a rewrite in Swift, not a port. iOS shows a local
  notification without running app code, so history and "next occurrence" would need a
  different design. It needs a Mac to build; free options are GitHub's Mac runners
  (build and simulator only). Publishing needs the Apple Developer Program.
- Zoom buttons on the map (offered; the owner was fine without).
- A lighter, white app bar with red as an accent only (offered, not requested).
- Re-activating a fired reminder from History (the owner's wording was unclear; ask).

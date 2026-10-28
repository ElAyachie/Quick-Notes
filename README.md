# Quick Notes

A note-taking app for Android whose notes can remind you of themselves.

## What it does

- **Notes** — create, edit, delete, and move between folders.
- **Folders** — create and delete. Deleting a folder deletes the notes in it. The
  "Unclassified" folder always exists.
- **Time reminders** — attach a date and time to a note; a notification shows the note
  when the time comes, and tapping it opens the note. "Later today", "Tomorrow morning"
  and "Next week" set one in a single tap. A time reminder can repeat every day, week,
  month or year, or on chosen days of the week (say Monday, Wednesday and Friday), like
  an alarm clock.
- **Location reminders** — pick a place on a map and how close counts as being there
  (100 m to 2 km); the note is shown when you arrive, or when you leave, even if the app
  is closed. An arriving reminder set while you are already at the place waits for your
  next visit. It can fire once, or every time.
- **Snooze** — a reminder's notification has "Done", "In 10 min" and "In 1 hour" buttons,
  so it can be put off without opening the app.
- **Search** — across all folders, by words and by meaning: "doctor" finds a note about a
  cardiology appointment that never uses the word. It also searches reminders: "tomorrow"
  finds what is due tomorrow, and "Target" or "pharmacy" finds a note whose location reminder
  is near one, even if you never named the place. Each result says why it was found. See
  [How search works](#how-search-works).
- **Place search** — the place picker has a search box like a map app's: type an address or a
  name such as "Walgreens" and it suggests matching places, nearest first, with how far away
  each is. Results come from Google Places, or from OpenStreetMap when Google can't be reached.
- **Saved places** — the bookmark in the place name field saves a place under a name (Home,
  Work). Saved places, and the named places of recent reminders, sit as chips under the search
  box and at the top of the search list, so a common place is one tap. Long press one to
  rename or remove it.
- **Quick add, by voice or typing** — the microphone in the toolbar takes a whole request:
  "Set me a location reminder for Walgreens to grab medicine and milk", "Remind me tomorrow at
  9am to call the dentist", "Every Monday at 7pm take out the trash". The app shows what it
  understood (the note, and when or where), finds the place nearest you, and saves nothing until
  you confirm. It reads common English phrasings on the phone; anything it can't place or time
  can be picked on the map or the clock.
- **Place picker shortcuts** — tapping a shop or landmark's icon on the map chooses it and
  fills in its name.
- **Reminders screen** — "Upcoming" lists the reminders still waiting; tap one to edit
  it, open its note or delete it. "History" lists the ones that have fired.
- Reminders survive a reboot.
- **Light and dark theme**, or follow the system.

## Download

**[Download the latest APK](https://github.com/ElAyachie/Quick-Notes/releases/latest/download/QuickNotes.apk)**
(Android 8.0 or newer), or see every version on the
[Releases page](https://github.com/ElAyachie/Quick-Notes/releases).

Installing from here rather than Google Play, Android will ask you to allow installs from your
browser the first time. Each release also has `QuickNotes.apk.sha256`, the file's SHA-256
checksum, if you want to check the download.

## How the code is organised

Java, XML layouts, a single activity. Everything lives under
`app/src/main/java/com/myapp/quicknotes`:

| Package | What's in it |
|---|---|
| `data` | The Room database: `Folder`, `Note` and `Reminder` tables, the search tables (`NoteChunk`, `NoteFts`), the nearby-place tables (`PlaceLookup`, `NearbyPlace`), the user's `SavedPlace`s, their DAOs, and one repository per table. Repositories are the only way the rest of the app touches data. |
| `reminders` | `ReminderScheduler` hands a reminder to the system: an alarm for a time, a geofence (Google Play services) for a place. `ReminderAlarmReceiver` and `GeofenceReceiver` run when one comes due, `ReminderNotifier` shows the notification, `ReminderActionReceiver` handles the notification's buttons, `ReminderRestoreReceiver` re-schedules everything after a reboot or app update. |
| `search` | Search by meaning, by words and by reminders: splitting notes into passages, the on-device embedding model, reading times in a query, matching reminders, ranking and merging. See [How search works](#how-search-works). |
| `places` | Looking up what is near a location reminder's place on OpenStreetMap, and searching for a place by name or address for the place picker (`PlaceSearchRepository`: Google Places, with OpenStreetMap's Photon when Google fails). |
| `command` | Reading a spoken or typed request into a note and reminder: `RuleCommandReader` (patterns, English) behind the `CommandReader` interface, with `SpokenTime` for times. |
| `ui` | `MainActivity` owns the toolbar; each screen is a fragment with a view model (`home`, `notes`, `editor`, `placepicker`, `quickadd`, `reminders`, `search`, `settings`). Screens are wired together in `res/navigation/nav_graph.xml`. |
| (root) | `QuickNotesApp` and `AppContainer` build the database, repositories and reminder classes once and share them. |

A reminder stores the id of its note, not a copy of the text, so the notification always
shows the note as it is when the reminder fires.

A row in the `reminders` table is one occurrence. When it fires it is stamped with the
time and stays as history; a repeating reminder continues as a new row for its next
occurrence (`Reminder.nextOccurrence`, `Repeat.nextAfter`, `DaysOfWeek.nextAfter`).

## How search works

Search combines two rankings, both computed on the phone with no network call and no cost.

**By meaning.** When a note is saved, its text is split into passages at blank lines (long
paragraphs are cut at a sentence end), and each passage, with the note's title in front, is
turned into a vector of 100 numbers by Google's
[Universal Sentence Encoder](https://developers.google.com/edge/mediapipe/solutions/text/text_embedder),
run by MediaPipe from a 6 MB model inside the app. Vectors are stored in the `note_chunks`
table with the model that made them and a fingerprint of the text, so a note is embedded again
only when its text or the model changes. A search embeds the query and compares it with every
passage; a note scores as well as its best passage, and that passage is shown in the results.

**By words.** SQLite's full-text index (FTS4, kept up to date by triggers) finds notes
containing the query's words, ignoring case and accents and leaving out common words like
"the". This catches what a language model can't know: names, ticket numbers, codes.

**By reminders.** Waiting reminders are a third and fourth ranking:

- *Times.* The query is read for times: today, tonight, tomorrow, a weekday, this or next week,
  the weekend, a month, a date like "Oct 30", optionally with morning, afternoon, evening or
  night. A time reminder due within one matches, and a repeating one counts on every day it
  repeats, so "Friday" finds a reminder set for Mon, Wed, Fri.
- *Places.* A location reminder matches by the name you gave its place, or by what is near it.
  When a location reminder is set, the app asks OpenStreetMap what is within its circle (up to
  500 m from the pin): the nearest 40 named shops, schools, landmarks and the like, 8 streets,
  and the neighbourhood. "Target" finds a reminder next to a Target; "pharmacy" or "drugstore"
  finds one near a CVS, because each kind of place carries its other names. What a place is
  counts only within 100 m of the pin, only when it accounts for the whole query ("gift for Mom"
  doesn't find a reminder near a gift shop), and with other names for the same kind of place,
  never related topics: a city has a bank and a cleaner near everything, so "money" or
  "laundry" would otherwise find every location reminder. Streets and areas match by name only.

The result then shows the reminder as its reason: "Reminder when you arrive at Grocery store,
near Target", or "Reminder Nov 2, 2026 9:00 AM · Mon, Wed, Fri".

**Merged** by [reciprocal rank fusion](https://plg.uwaterloo.ca/~gvcormac/cormacksigir09-rrf.pdf):
each note scores 1/(60 + its position) in each ranking. Positions rather than scores, so a
cosine similarity never has to be weighed against a word count, and a note both rankings found
rises to the top. On a tie the word match goes first, then reminders, then meaning. A reminder
found only by what is near it ("doctor" for a place next to a clinic) ranks last, below notes
that are about the search. A query
made only of numbers, codes or times skips the meaning ranking.

**No vector database.** The query is compared with every stored vector. For a few thousand
passages of 100 numbers that is a few hundred thousand multiplications, a fraction of the time
the model takes to embed the query. An approximate index would add a dependency and complexity
for no visible gain at the size of one person's notes.

### Measured quality

`SearchQualityTest` runs the real model over 30 everyday notes and 34 labelled queries: 24
that share no word with their answer ("childcare" for a note about a babysitter), 6 exact
strings ("QX7P2L", "617-555-0142"), and 4 with no answer. Results on an emulator:

| Ranking | Right note first | Right note in first three | Mean reciprocal rank |
|---|---|---|---|
| By meaning, the model's vectors as they come | 20% | 57% | 0.43 |
| By meaning, centered (below); numbers and codes skip it | 63% | 80% | 0.71 |
| By words | 37% | 40% | 0.38 |
| **Both merged (what the app does)** | **70%** | **90%** | **0.79** |

Queries with no answer bring back 1.3 notes on average. A search takes about 6 ms.

**Centering.** MediaPipe's version of the model gives every text a vector pointing mostly the
same way: any two texts came out 0.7 to 0.9 similar, and "doctor" was closer to "Lisbon
itinerary" than to "Cardiology follow-up". Subtracting the average vector of typical texts
removes that shared part. Searches and note passages each get their own average, because a
few words and a paragraph sit in different places. The averages come from a separate set of
calibration texts (`androidTest/assets/search_calibration.txt`), not from the evaluation set,
and `CenteringVectorsTest` checks the shipped ones still match. The similarity a passage needs
to count as a match (0.25) was chosen on the evaluation set itself, so the numbers above are
a little flattering; with 34 queries, one query is 3 points.

**What it doesn't do.** The model understands English only. "Heart appointment" doesn't find
the cardiology note, and a search with no good answer can still show a loosely related note or
two.

### Privacy and size

Note text never leaves the phone. Two things do go over the network:

- When a location reminder is set, its place's coordinates and radius are sent to
  OpenStreetMap's public [Overpass](https://wiki.openstreetmap.org/wiki/Overpass_API) servers
  to find what is nearby: once per place, and again only if the place moves. The servers are
  free and need no key, but can be slow; a lookup that fails is tried again at the next start.
  Place data is © OpenStreetMap contributors, credited on the search screen when it is shown.
- What is typed into the place picker's search box is sent, with the area the map shows and
  the phone's position (for distances), to Google's Places API, or to komoot's
  [Photon](https://photon.komoot.io) server when Google can't answer. Each list of results
  credits where it came from.
- Quick add's microphone uses the phone's speech recognizer (on most phones Google's), which may
  send the recording to its maker to turn it into text, as the keyboard's microphone does. The
  text is then read on the phone. Typing sends nothing; a place named in it is searched for as
  above.
- MediaPipe sends Google anonymous usage statistics about
the model (how often it runs, how long it takes), as its
[privacy notice](https://developers.google.com/edge/mediapipe/solutions/tasks#mediapipe_tasks_privacy_notice)
describes; the library offers no way to turn this off.

The release APK is 31 MB, of which 17 MB is MediaPipe's native library (for 64-bit and 32-bit
ARM) and 6 MB the model. Release builds leave out the x86 libraries, which only emulators use;
with them it was 58 MB.

If the model ever crashes the app (native code can't be caught), the next start notices that
the first attempt never finished, and search falls back to words only on that device instead
of crashing again (`CrashGuardedEmbedder`).

## Building

Open the project in Android Studio and press Run, or from a terminal:

```
gradlew assembleDebug              # build the debug APK
gradlew testDebugUnitTest          # run the recurrence and search ranking tests on your computer
gradlew connectedDebugAndroidTest  # run the database, migration and search quality tests on a device
```

Requires JDK 17 or newer (Android Studio's bundled JDK works). `local.properties` must
point `sdk.dir` at your Android SDK; Android Studio writes it for you.

The place picker uses Google Maps. Put a Maps SDK for Android key in `local.properties`
as `MAP_API_KEY=...`; without it the app still runs but the map shows no tiles. Place search
uses the same key with **Places API (New)** enabled in its Google Cloud project; without that
it searches OpenStreetMap instead. Google bills place search per session (the suggestions
while typing and the chosen place's location count as one); its monthly free allowance
covers personal use.

## Releases

Releases are built and signed by GitHub Actions (`.github/workflows/release.yml`); no signing
key is kept in the repository. To publish a version, bump `versionCode` and `versionName` in
`app/build.gradle` (Android only installs an update over an older version code), then push a
tag such as `git tag v3.1 && git push origin v3.1`, or run **Release APK** from the Actions tab.

The workflow needs these repository secrets (Settings > Secrets and variables > Actions):

| Secret | |
|---|---|
| `ANDROID_KEYSTORE_BASE64` | The release keystore, base64-encoded |
| `ANDROID_KEYSTORE_PASSWORD` | Its password |
| `ANDROID_KEY_ALIAS` | Optional, defaults to `release` |
| `MAP_API_KEY` | Optional. Without it the map shows no tiles. |

The Maps key ends up inside the public APK, which is normal for Android, so restrict it in the
Google Cloud console: under **Application restrictions** choose Android apps and add
`com.myapp.QuickNotes1` with the release certificate's SHA-1 (and the debug certificate's, to
use it from debug builds), and under **API restrictions** allow only Maps SDK for Android and
Places API (New). The app sends its package and certificate with each place search, so a key
restricted this way works for search too.

To build a signed release on your own computer, set `ANDROID_KEYSTORE_PATH` and
`ANDROID_KEYSTORE_PASSWORD` and run `gradlew assembleRelease`. Without them the release build
is unsigned.

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

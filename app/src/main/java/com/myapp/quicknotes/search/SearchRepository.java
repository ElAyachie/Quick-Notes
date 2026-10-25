package com.myapp.quicknotes.search;

import android.database.sqlite.SQLiteConstraintException;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.myapp.quicknotes.AppExecutors;
import com.myapp.quicknotes.data.IndexedNote;
import com.myapp.quicknotes.data.NearbyPlace;
import com.myapp.quicknotes.data.Note;
import com.myapp.quicknotes.data.NoteChunk;
import com.myapp.quicknotes.data.NoteWithFolder;
import com.myapp.quicknotes.data.Reminder;
import com.myapp.quicknotes.data.ReminderType;
import com.myapp.quicknotes.data.SearchDao;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.ZoneId;
import java.time.temporal.WeekFields;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;

// Search over all notes, by meaning, by words and by their reminders at once.
//
// Indexing: when a note is saved, its text is split into passages (TextChunker) and each passage
// is embedded and stored (note_chunks). The words are indexed by SQLite's full-text index, which
// keeps itself up to date. Location reminders' surroundings are looked up separately
// (places.NearbyPlacesRepository).
//
// Searching: the query is embedded and compared with every passage (SemanticRanker), the
// full-text index is asked for notes containing its words (KeywordScorer), waiting reminders are
// matched by the times the query names (TimeQuery) and by their places (ReminderMatcher), and the
// rankings are merged (RankFusion).
//
// Both run on the search thread, one task at a time, so a note's passages are never written by
// two tasks at once and the passages kept in memory need no locking.
public class SearchRepository {
    private static final String TAG = "SearchRepository";

    // How close in meaning (cosine similarity, -1 to 1) a passage must be to a query to count as
    // a match. Below it the model mostly reports loose associations. Chosen with the labelled
    // queries in SearchQualityTest: at 0.20 nearly every query finds its note but brings three
    // wrong ones along; at 0.30 one wrong note comes along but more queries find nothing. Results
    // are ranked, so a wrong note at the bottom costs less than a right one missing.
    static final float MIN_SIMILARITY = 0.25f;
    static final int MAX_RESULTS = 50;
    // How much of a note's start to show when no passage explains the match.
    private static final int SNIPPET_CHARS = 200;

    private final SearchDao dao;
    private final Embedder embedder;
    private final AppExecutors executors;
    private final MutableLiveData<Boolean> indexing = new MutableLiveData<>(false);
    // Becomes false if the model can't be loaded or run on this device. Search then finds notes by
    // their words only.
    private volatile boolean embedderWorks = true;
    // The stored passages, read once and kept for the next search. Cleared when passages are
    // written. A deleted note's passages are deleted with it (by the database) and stay here until
    // the next write, but its id no longer finds a note, so it never shows in results.
    @Nullable
    private List<SemanticRanker.Passage> passages;

    public SearchRepository(SearchDao dao, Embedder embedder, AppExecutors executors) {
        this.dao = dao;
        this.embedder = embedder;
        this.executors = executors;
    }

    // True while notes saved before are being indexed, at the first start after an update.
    public LiveData<Boolean> isIndexing() {
        return indexing;
    }

    public boolean canSearchByMeaning() {
        return embedderWorks;
    }

    // Indexes a note that was just saved, in the background.
    public void indexNote(Note note) {
        executors.search().execute(() -> indexNow(note));
    }

    // Indexes every note whose passages are missing or out of date: notes written before search
    // existed, notes indexed by another model, and notes saved while the app was closed before
    // their turn came. Called at app start.
    public void indexAllNotes() {
        executors.search().execute(() -> {
            List<Note> outdated = findOutdated();
            if (outdated.isEmpty()) {
                return;
            }
            indexing.postValue(true);
            for (Note note : outdated) {
                if (!embedderWorks) {
                    break;
                }
                indexNow(note);
            }
            indexing.postValue(false);
        });
    }

    // Finds notes for a query and reports them, best first, on the main thread.
    public void search(String query, Consumer<List<SearchResult>> onResults) {
        executors.search().execute(() -> {
            List<SearchResult> results = resultsFor(rank(query));
            executors.runOnMain(() -> onResults.accept(results));
        });
    }

    // The rankings a search is built from, kept apart so their quality can be measured
    // separately (SearchQualityTest).
    public static final class Ranking {
        public final List<String> terms;
        public final List<SemanticRanker.Match> byMeaning;
        public final List<Long> byWords;
        public final List<ReminderMatcher.Match> byPlace;
        public final List<ReminderMatcher.Match> byTime;
        public final List<Long> combined;

        Ranking(List<String> terms, List<SemanticRanker.Match> byMeaning, List<Long> byWords,
                List<ReminderMatcher.Match> byPlace, List<ReminderMatcher.Match> byTime) {
            this.terms = terms;
            this.byMeaning = byMeaning;
            this.byWords = byWords;
            this.byPlace = byPlace;
            this.byTime = byTime;
            List<Long> meaningIds = new ArrayList<>();
            for (SemanticRanker.Match match : byMeaning) {
                meaningIds.add(match.noteId);
            }
            // Words go first, so they win ties: a note containing the very word searched for is a
            // surer answer than the model's best guess. "4471" should find "Ticket #4471" first.
            // Reminders come next: a reminder at the very place or time asked about is as sure.
            // A reminder matched only by what is near it ("doctor" for a place by a clinic) comes
            // last: the search was more likely about a note than about the neighbourhood.
            List<ReminderMatcher.Match> byName = new ArrayList<>();
            List<ReminderMatcher.Match> byKind = new ArrayList<>();
            for (ReminderMatcher.Match match : byPlace) {
                (match.byKindOnly ? byKind : byName).add(match);
            }
            this.combined = RankFusion.fuse(Arrays.asList(
                    byWords, noteIds(byName), noteIds(byTime), meaningIds, noteIds(byKind)));
        }

        // Why a note was found through one of its reminders, if it was.
        @Nullable
        public ReminderMatcher.Match reminderMatchFor(long noteId) {
            for (List<ReminderMatcher.Match> matches : Arrays.asList(byPlace, byTime)) {
                for (ReminderMatcher.Match match : matches) {
                    if (match.noteId == noteId) {
                        return match;
                    }
                }
            }
            return null;
        }

        private static List<Long> noteIds(List<ReminderMatcher.Match> matches) {
            List<Long> ids = new ArrayList<>();
            for (ReminderMatcher.Match match : matches) {
                ids.add(match.noteId);
            }
            return ids;
        }
    }

    @WorkerThread
    public Ranking rank(String query) {
        return rank(query, System.currentTimeMillis(), ZoneId.systemDefault());
    }

    @WorkerThread
    Ranking rank(String query, long now, ZoneId zone) {
        List<String> terms = KeywordScorer.terms(query);
        if (terms.isEmpty()) {
            return new Ranking(terms, new ArrayList<>(), new ArrayList<>(), new ArrayList<>(),
                    new ArrayList<>());
        }
        TimeQuery.Parsed time = TimeQuery.parse(query, now, zone,
                WeekFields.of(Locale.getDefault()).getFirstDayOfWeek());
        List<Reminder> reminders = dao.getUpcomingReminders();
        List<String> placeTerms = new ArrayList<>(terms);
        placeTerms.removeAll(time.timeWords);
        List<ReminderMatcher.Match> byPlace =
                ReminderMatcher.byPlace(reminders, nearbyPlaces(reminders), placeTerms);
        List<ReminderMatcher.Match> byTime =
                ReminderMatcher.byTime(reminders, time.ranges, zone);

        List<SemanticRanker.Match> byMeaning = new ArrayList<>();
        // Numbers and codes ("4471", "QX7P2L") mean nothing to the model, which would still
        // answer with whatever notes its vector happens to land near; nor do times ("tomorrow"),
        // which are matched against reminders instead.
        if (embedderWorks && !KeywordScorer.allContainDigits(terms) && !time.onlyTime) {
            float[] queryVector = embedOrGiveUp(() -> embedder.embedQuery(query.trim()));
            if (queryVector != null) {
                byMeaning = SemanticRanker.rank(queryVector, loadPassages(), MIN_SIMILARITY);
            }
        }
        List<Long> byWords = KeywordScorer.rank(
                dao.findNotesMatching(KeywordScorer.matchQuery(terms)), terms);
        return new Ranking(terms, byMeaning, byWords, byPlace, byTime);
    }

    // The surroundings of the location reminders' places, by place key.
    @WorkerThread
    private Map<String, List<NearbyPlace>> nearbyPlaces(List<Reminder> reminders) {
        Set<String> keys = new HashSet<>();
        for (Reminder reminder : reminders) {
            if (reminder.getType() == ReminderType.LOCATION) {
                keys.add(reminder.placeKey());
            }
        }
        Map<String, List<NearbyPlace>> byKey = new HashMap<>();
        if (keys.isEmpty()) {
            return byKey;
        }
        for (NearbyPlace place : dao.getNearbyPlaces(keys)) {
            byKey.computeIfAbsent(place.getPlaceKey(), key -> new ArrayList<>()).add(place);
        }
        // Nearest first, which ReminderMatcher relies on.
        for (List<NearbyPlace> places : byKey.values()) {
            places.sort((a, b) -> Integer.compare(a.getDistanceMeters(), b.getDistanceMeters()));
        }
        return byKey;
    }

    @WorkerThread
    private List<SearchResult> resultsFor(Ranking ranking) {
        List<Long> ids = ranking.combined.subList(
                0, Math.min(ranking.combined.size(), MAX_RESULTS));
        Map<Long, NoteWithFolder> found = new HashMap<>();
        for (NoteWithFolder note : dao.getNotesWithFolder(ids)) {
            found.put(note.getNote().getId(), note);
        }
        Map<Long, SemanticRanker.Match> matches = new HashMap<>();
        for (SemanticRanker.Match match : ranking.byMeaning) {
            matches.put(match.noteId, match);
        }
        List<SearchResult> results = new ArrayList<>();
        for (long id : ids) {
            NoteWithFolder item = found.get(id);
            if (item == null) {
                continue;
            }
            Note note = item.getNote();
            results.add(new SearchResult(note.getId(), note.getFolderId(), note.getTitle(),
                    item.getFolderName(), snippet(note, matches.get(id), ranking.terms),
                    note.getUpdatedAt(), ranking.reminderMatchFor(id)));
        }
        return results;
    }

    // The part of the note to show under its title: the passage closest in meaning, or else the
    // first line containing a search word, or else the note's beginning.
    private static String snippet(Note note, @Nullable SemanticRanker.Match match,
                                  List<String> terms) {
        String content = note.getContent();
        // A passage's positions are from when it was indexed; if the note has changed since and
        // not been indexed again, they may not fit.
        if (match != null && match.start < match.end && match.end <= content.length()) {
            return content.substring(match.start, match.end);
        }
        TextChunker.Span line = KeywordScorer.firstMatchingLine(content, terms);
        if (line != null) {
            return content.substring(line.start, line.end).trim();
        }
        return content.substring(0, Math.min(content.length(), SNIPPET_CHARS)).trim();
    }

    // Package-private so SearchQualityTest can index without waiting on the search thread.
    @WorkerThread
    void indexNow(Note note) {
        if (!embedderWorks) {
            return;
        }
        String hash = sourceHash(note);
        List<NoteChunk> chunks = new ArrayList<>();
        List<TextChunker.Span> spans = TextChunker.split(note.getContent());
        if (spans.isEmpty()) {
            float[] vector = embedOrGiveUp(() -> embedder.embedPassage(note.getTitle()));
            if (vector == null) {
                return;
            }
            chunks.add(chunk(note, 0, 0, hash, vector));
        }
        for (TextChunker.Span span : spans) {
            // The title goes with every passage: "Tuesday 3 PM" means more under "Cardiologist".
            String passage = note.getTitle() + "\n"
                    + note.getContent().substring(span.start, span.end);
            float[] vector = embedOrGiveUp(() -> embedder.embedPassage(passage));
            if (vector == null) {
                return;
            }
            chunks.add(chunk(note, span.start, span.end, hash, vector));
        }
        try {
            dao.replaceChunksOfNote(note.getId(), chunks);
        } catch (SQLiteConstraintException deletedMeanwhile) {
            // The note was deleted while it was being embedded; there is nothing to index.
        }
        passages = null;
    }

    private NoteChunk chunk(Note note, int start, int end, String hash, float[] vector) {
        return new NoteChunk(0, note.getId(), start, end, embedder.modelId(), hash,
                Vectors.toBytes(vector));
    }

    @WorkerThread
    private List<Note> findOutdated() {
        Map<Long, IndexedNote> indexed = new HashMap<>();
        for (IndexedNote note : dao.getIndexedNotes()) {
            indexed.put(note.getNoteId(), note);
        }
        List<Note> outdated = new ArrayList<>();
        for (Note note : dao.getAllNotes()) {
            IndexedNote current = indexed.get(note.getId());
            if (current == null
                    || !current.getModelId().equals(embedder.modelId())
                    || !current.getSourceHash().equals(sourceHash(note))) {
                outdated.add(note);
            }
        }
        return outdated;
    }

    @WorkerThread
    private List<SemanticRanker.Passage> loadPassages() {
        if (passages == null) {
            List<SemanticRanker.Passage> loaded = new ArrayList<>();
            for (NoteChunk chunk : dao.getAllChunks()) {
                if (chunk.getModelId().equals(embedder.modelId())) {
                    loaded.add(new SemanticRanker.Passage(chunk.getNoteId(),
                            chunk.getStartOffset(), chunk.getEndOffset(),
                            Vectors.fromBytes(chunk.getVector())));
                }
            }
            passages = loaded;
        }
        return passages;
    }

    // The vector, or null if the model failed. After one failure (no native library for this
    // processor, a damaged model file) it isn't tried again until the app restarts.
    @Nullable
    private float[] embedOrGiveUp(Supplier<float[]> embedding) {
        try {
            return embedding.get();
        } catch (RuntimeException | LinkageError e) {
            Log.w(TAG, "Search by meaning is unavailable", e);
            embedderWorks = false;
            return null;
        }
    }

    // A fingerprint of what gets embedded, to tell when a note has changed since it was indexed.
    // Moving a note to another folder doesn't change it.
    static String sourceHash(Note note) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest((note.getTitle() + "\u0000" + note.getContent())
                    .getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is always available on Android", e);
        }
    }
}

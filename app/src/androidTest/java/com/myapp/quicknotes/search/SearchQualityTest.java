package com.myapp.quicknotes.search;

import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.util.Log;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.myapp.quicknotes.AppExecutors;
import com.myapp.quicknotes.data.AppDatabase;
import com.myapp.quicknotes.data.Folder;
import com.myapp.quicknotes.data.Note;
import com.myapp.quicknotes.data.NoteChunk;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

// Measures how well search finds the notes a person would expect, with the real model, on the
// labelled set in SearchEvalSet. The numbers are written to the log (tag SearchQuality):
//
//   adb logcat -s SearchQuality
//
// For each way of ranking (by meaning only, by words only, and both combined as the app does):
//   hit@1  share of queries whose first result is a right answer
//   hit@3  share of queries with a right answer among the first three
//   MRR    mean reciprocal rank: 1 for a right answer first, 1/2 second, ... 0 if not found
// and, for the queries with no answer, how many results come back anyway (fewer is better).
//
// A sweep over the minimum similarity shows the trade-off behind SearchRepository.MIN_SIMILARITY.
@RunWith(AndroidJUnit4.class)
public class SearchQualityTest {
    private static final String TAG = "SearchQuality";

    private static AppDatabase database;
    private static SearchRepository search;
    private static final Map<Long, String> titles = new HashMap<>();

    @BeforeClass
    public static void indexTheNotes() {
        Context context = ApplicationProvider.getApplicationContext();
        database = AppDatabase.createInMemory(context);
        search = new SearchRepository(database.searchDao(), new UniversalSentenceEncoder(context),
                new AppExecutors());
        long start = System.nanoTime();
        for (SearchEvalSet.EvalNote evalNote : SearchEvalSet.NOTES) {
            Note note = Note.blank(Folder.DEFAULT_ID)
                    .edited(evalNote.title, evalNote.content, Folder.DEFAULT_ID);
            note = note.withId(database.noteDao().insert(note));
            titles.put(note.getId(), note.getTitle());
            search.indexNow(note);
        }
        Log.i(TAG, String.format(Locale.US, "Indexed %d notes in %d ms (model load included)",
                SearchEvalSet.NOTES.size(), (System.nanoTime() - start) / 1_000_000));
    }

    @AfterClass
    public static void closeDatabase() {
        database.close();
    }

    @Test
    public void combinedSearchFindsWhatEitherSearchAloneFinds() {
        Scores meaning = new Scores();
        Scores words = new Scores();
        Scores combined = new Scores();
        int noAnswerResults = 0;
        int noAnswerQueries = 0;
        long totalMillis = 0;
        for (SearchEvalSet.EvalQuery query : SearchEvalSet.QUERIES) {
            long start = System.nanoTime();
            SearchRepository.Ranking ranking = search.rank(query.text);
            totalMillis += (System.nanoTime() - start) / 1_000_000;
            List<Long> byMeaning = new ArrayList<>();
            for (SemanticRanker.Match match : ranking.byMeaning) {
                byMeaning.add(match.noteId);
            }
            if (query.relevant.isEmpty()) {
                noAnswerQueries++;
                noAnswerResults += ranking.combined.size();
                Log.i(TAG, "No answer expected for \"" + query.text + "\"; got "
                        + titlesOf(ranking.combined));
                continue;
            }
            meaning.add(byMeaning, query.relevant);
            words.add(ranking.byWords, query.relevant);
            combined.add(ranking.combined, query.relevant);
            if (rankOfFirstRelevant(ranking.combined, query.relevant) != 1) {
                Log.i(TAG, "\"" + query.text + "\" expected " + query.relevant + "; got "
                        + titlesOf(ranking.combined.subList(0,
                        Math.min(3, ranking.combined.size()))));
            }
        }
        Log.i(TAG, String.format(Locale.US, "Minimum similarity %.2f, %d queries, %.1f ms each",
                SearchRepository.MIN_SIMILARITY, SearchEvalSet.QUERIES.size(),
                (double) totalMillis / SearchEvalSet.QUERIES.size()));
        Log.i(TAG, "             hit@1  hit@3  MRR");
        Log.i(TAG, "by meaning   " + meaning);
        Log.i(TAG, "by words     " + words);
        Log.i(TAG, "combined     " + combined);
        Log.i(TAG, String.format(Locale.US, "Results for queries with no answer: %.1f on average",
                (double) noAnswerResults / noAnswerQueries));

        // Floors a little under what the current model and settings reach, so a change that
        // makes search clearly worse fails here.
        assertTrue("combined hit@3 " + combined, combined.hitAt3() >= 0.85);
        assertTrue("combined MRR " + combined, combined.mrr() >= 0.70);
        assertTrue("combined should beat words alone", combined.mrr() > words.mrr());
        assertTrue("combined should beat meaning alone", combined.mrr() >= meaning.mrr());
    }

    // Logs, for each minimum similarity, how many queries still find an answer by meaning, and
    // how many wrong notes come along. Informational; it asserts nothing.
    @Test
    public void sweepMinimumSimilarity() {
        List<SemanticRanker.Passage> passages = new ArrayList<>();
        for (NoteChunk chunk : database.searchDao().getAllChunks()) {
            passages.add(new SemanticRanker.Passage(chunk.getNoteId(), chunk.getStartOffset(),
                    chunk.getEndOffset(), Vectors.fromBytes(chunk.getVector())));
        }
        UniversalSentenceEncoder model =
                new UniversalSentenceEncoder(ApplicationProvider.getApplicationContext());
        Map<String, float[]> queryVectors = new HashMap<>();
        for (SearchEvalSet.EvalQuery query : SearchEvalSet.QUERIES) {
            queryVectors.put(query.text, model.embedQuery(query.text));
        }
        Log.i(TAG, "min    meaning hit@3  combined hit@3  wrong notes/query  no-answer results");
        for (float min = 0.10f; min <= 0.651f; min += 0.05f) {
            Scores meaning = new Scores();
            Scores combined = new Scores();
            int wrong = 0;
            int answerable = 0;
            int noAnswerResults = 0;
            int noAnswerQueries = 0;
            for (SearchEvalSet.EvalQuery query : SearchEvalSet.QUERIES) {
                // The same steps as SearchRepository.rank, with this minimum.
                List<Long> byMeaning = new ArrayList<>();
                if (!KeywordScorer.allContainDigits(KeywordScorer.terms(query.text))) {
                    for (SemanticRanker.Match match :
                            SemanticRanker.rank(queryVectors.get(query.text), passages, min)) {
                        byMeaning.add(match.noteId);
                    }
                }
                List<Long> byWords = search.rank(query.text).byWords;
                List<Long> fused = RankFusion.fuse(Arrays.asList(byWords, byMeaning));
                if (query.relevant.isEmpty()) {
                    noAnswerQueries++;
                    noAnswerResults += fused.size();
                    continue;
                }
                answerable++;
                meaning.add(byMeaning, query.relevant);
                combined.add(fused, query.relevant);
                for (long id : byMeaning) {
                    if (!query.relevant.contains(titles.get(id))) {
                        wrong++;
                    }
                }
            }
            Log.i(TAG, String.format(Locale.US, "%.2f   %.2f           %.2f            %.1f"
                            + "                %.1f", min, meaning.hitAt3(), combined.hitAt3(),
                    (double) wrong / answerable, (double) noAnswerResults / noAnswerQueries));
        }
    }

    private static int rankOfFirstRelevant(List<Long> ranking, List<String> relevant) {
        for (int i = 0; i < ranking.size(); i++) {
            if (relevant.contains(titles.get(ranking.get(i)))) {
                return i + 1;
            }
        }
        return 0;
    }

    private static List<String> titlesOf(List<Long> ids) {
        List<String> names = new ArrayList<>();
        for (long id : ids) {
            names.add(titles.get(id));
        }
        return names;
    }

    private static final class Scores {
        private int queries;
        private int hitsAt1;
        private int hitsAt3;
        private double reciprocalRanks;

        void add(List<Long> ranking, List<String> relevant) {
            queries++;
            int rank = rankOfFirstRelevant(ranking, relevant);
            if (rank == 1) {
                hitsAt1++;
            }
            if (rank >= 1 && rank <= 3) {
                hitsAt3++;
            }
            if (rank >= 1) {
                reciprocalRanks += 1.0 / rank;
            }
        }

        double hitAt3() {
            return (double) hitsAt3 / queries;
        }

        double mrr() {
            return reciprocalRanks / queries;
        }

        @Override
        public String toString() {
            return String.format(Locale.US, "%.2f   %.2f   %.2f",
                    (double) hitsAt1 / queries, hitAt3(), mrr());
        }
    }

    // The scoring tells notes apart by title, so the set's titles must be unique.
    static {
        Set<String> seen = new HashSet<>();
        for (SearchEvalSet.EvalNote note : SearchEvalSet.NOTES) {
            if (!seen.add(note.title)) {
                throw new IllegalStateException("Duplicate title in SearchEvalSet: " + note.title);
            }
        }
    }
}

package com.myapp.quicknotes.search;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class SemanticRankerTest {
    private static SemanticRanker.Passage passage(long noteId, int start, float... vector) {
        return new SemanticRanker.Passage(noteId, start, start + 10, Vectors.normalized(vector));
    }

    @Test
    public void notesAreRankedByTheirClosestPassage() {
        float[] query = {1, 0};
        List<SemanticRanker.Passage> passages = Arrays.asList(
                passage(1, 0, 1, 1),     // 45 degrees off
                passage(2, 0, 0, 1),     // unrelated
                passage(2, 50, 1, 0.1f), // nearly the query
                passage(3, 0, 1, 0.5f));

        List<SemanticRanker.Match> ranked = SemanticRanker.rank(query, passages, 0.5f);

        assertEquals(3, ranked.size());
        assertEquals(2, ranked.get(0).noteId);
        // The passage that matched is the one reported.
        assertEquals(50, ranked.get(0).start);
        assertEquals(3, ranked.get(1).noteId);
        assertEquals(1, ranked.get(2).noteId);
    }

    @Test
    public void passagesBelowTheMinimumAreLeftOut() {
        List<SemanticRanker.Match> ranked = SemanticRanker.rank(new float[]{1, 0},
                Arrays.asList(passage(1, 0, 0, 1), passage(2, 0, -1, 0)), 0.1f);

        assertEquals(Collections.emptyList(), ranked);
    }

    @Test
    public void equalScoresAreOrderedByNoteId() {
        List<SemanticRanker.Match> ranked = SemanticRanker.rank(new float[]{1, 0},
                Arrays.asList(passage(9, 0, 1, 0), passage(4, 0, 1, 0)), 0f);

        assertEquals(4, ranked.get(0).noteId);
        assertEquals(9, ranked.get(1).noteId);
    }
}

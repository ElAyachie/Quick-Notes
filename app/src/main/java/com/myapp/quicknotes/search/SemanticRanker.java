package com.myapp.quicknotes.search;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// Ranks notes by how close in meaning their passages are to a query. Every stored passage is
// compared with the query in turn. At the size of one person's notes (thousands of passages of
// a hundred or so numbers each) that takes milliseconds, so an approximate nearest-neighbour index
// would add complexity without making search noticeably faster.
public final class SemanticRanker {
    private SemanticRanker() {
    }

    // One embedded passage of a note. Its text runs from start to end in the note's content; a
    // note with no content is embedded by its title alone, with start and end both 0.
    public static final class Passage {
        final long noteId;
        final int start;
        final int end;
        final float[] vector;

        public Passage(long noteId, int start, int end, float[] vector) {
            this.noteId = noteId;
            this.start = start;
            this.end = end;
            this.vector = vector;
        }
    }

    // A note and its passage closest to the query.
    public static final class Match {
        public final long noteId;
        public final float score;
        public final int start;
        public final int end;

        Match(long noteId, float score, int start, int end) {
            this.noteId = noteId;
            this.score = score;
            this.start = start;
            this.end = end;
        }
    }

    // Notes whose best passage scores at least minScore, best first. A note counts as close as
    // its closest passage, so one relevant paragraph in a long note is enough to find it. The
    // query and passage vectors must have length 1 (Vectors.normalized).
    public static List<Match> rank(float[] query, List<Passage> passages, float minScore) {
        Map<Long, Match> best = new HashMap<>();
        for (Passage passage : passages) {
            float score = Vectors.dot(query, passage.vector);
            if (score < minScore) {
                continue;
            }
            Match current = best.get(passage.noteId);
            if (current == null || score > current.score) {
                best.put(passage.noteId,
                        new Match(passage.noteId, score, passage.start, passage.end));
            }
        }
        List<Match> ranked = new ArrayList<>(best.values());
        ranked.sort((a, b) -> a.score != b.score
                ? Float.compare(b.score, a.score)
                : Long.compare(a.noteId, b.noteId));
        return ranked;
    }
}

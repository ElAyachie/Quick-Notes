package com.myapp.quicknotes.search;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// Merges rankings from different searches into one, by reciprocal rank fusion: each note scores
// 1 / (K + its position) in every ranking it appears in, and the sums decide the order. It needs
// only positions, not scores, so a similarity of 0.6 and a count of matched words never have to
// be weighed against each other, and a note that both searches found rises above either alone.
public final class RankFusion {
    // The usual constant from the original paper (Cormack et al., 2009). A larger K flattens the
    // difference between the first few places.
    static final int K = 60;

    private RankFusion() {
    }

    // Note ids from best to worst. Notes with equal scores keep the order they were first seen in,
    // so the earlier ranking given wins a tie.
    public static List<Long> fuse(List<List<Long>> rankings) {
        Map<Long, Double> scores = new LinkedHashMap<>();
        for (List<Long> ranking : rankings) {
            for (int position = 0; position < ranking.size(); position++) {
                scores.merge(ranking.get(position), 1.0 / (K + position + 1), Double::sum);
            }
        }
        List<Long> fused = new ArrayList<>(scores.keySet());
        // A stable sort, so ties stay in first-seen order.
        fused.sort((a, b) -> Double.compare(scores.get(b), scores.get(a)));
        return fused;
    }
}

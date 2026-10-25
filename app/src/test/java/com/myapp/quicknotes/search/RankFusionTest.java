package com.myapp.quicknotes.search;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class RankFusionTest {
    @Test
    public void noRankingsGiveNothing() {
        assertEquals(Collections.emptyList(), RankFusion.fuse(Collections.emptyList()));
    }

    @Test
    public void oneRankingKeepsItsOrder() {
        assertEquals(Arrays.asList(3L, 1L, 2L),
                RankFusion.fuse(Collections.singletonList(Arrays.asList(3L, 1L, 2L))));
    }

    @Test
    public void aNoteFoundByBothRisesAboveNotesFoundByOne() {
        List<Long> byMeaning = Arrays.asList(1L, 2L, 3L);
        List<Long> byWords = Arrays.asList(4L, 3L);

        List<Long> fused = RankFusion.fuse(Arrays.asList(byMeaning, byWords));

        assertEquals(Long.valueOf(3), fused.get(0));
        assertEquals(4, fused.size());
    }

    @Test
    public void tiesGoToTheFirstRanking() {
        // 1 and 2 are both first in one ranking; the first ranking given wins.
        assertEquals(Arrays.asList(1L, 2L),
                RankFusion.fuse(Arrays.asList(Collections.singletonList(1L),
                        Collections.singletonList(2L))));
    }

    @Test
    public void positionsCountAsReciprocalRanks() {
        // 5 is second in both (2 / 62); 6 is first in one only (1 / 61).
        List<Long> fused = RankFusion.fuse(Arrays.asList(
                Arrays.asList(6L, 5L),
                Arrays.asList(7L, 5L)));

        assertEquals(Arrays.asList(5L, 6L, 7L), fused);
    }
}

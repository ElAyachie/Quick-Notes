package com.myapp.quicknotes.search;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.myapp.quicknotes.data.Note;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class KeywordScorerTest {
    private static Note note(long id, String title, String content, long updatedAt) {
        return new Note(id, 1, title, content, 0, updatedAt);
    }

    @Test
    public void termsAreLowerCaseWithoutAccentsOrRepeats() {
        assertEquals(Arrays.asList("cafe", "creme", "brulee"),
                KeywordScorer.terms("Café crème BRÛLÉE café"));
    }

    @Test
    public void punctuationAndOperatorsSeparateTerms() {
        // Nothing of the full-text query syntax survives: quotes, stars, minus, parentheses.
        assertEquals(Arrays.asList("dr", "smith", "s", "1", "30pm"),
                KeywordScorer.terms("\"Dr. Smith's\" (1:30pm) -OR*"));
    }

    @Test
    public void commonWordsAreLeftOut() {
        assertEquals(Arrays.asList("skiing", "alps"), KeywordScorer.terms("Skiing in the Alps"));
    }

    @Test
    public void aQueryOfOnlyCommonWordsKeepsThem() {
        assertEquals(Arrays.asList("the", "who"), KeywordScorer.terms("The Who"));
    }

    @Test
    public void numbersAndCodesAreRecognised() {
        assertTrue(KeywordScorer.allContainDigits(KeywordScorer.terms("617-555-0142")));
        assertTrue(KeywordScorer.allContainDigits(KeywordScorer.terms("QX7P2L")));
        assertFalse(KeywordScorer.allContainDigits(KeywordScorer.terms("DS-82")));
        assertFalse(KeywordScorer.allContainDigits(KeywordScorer.terms("dentist 3pm")));
        assertFalse(KeywordScorer.allContainDigits(KeywordScorer.terms("")));
    }

    @Test
    public void blankQueryHasNoTerms() {
        assertEquals(Collections.emptyList(), KeywordScorer.terms("  ...  "));
    }

    @Test
    public void matchQueryLooksForAnyTermAsAWordStart() {
        assertEquals("dentist* OR tuesday*",
                KeywordScorer.matchQuery(Arrays.asList("dentist", "tuesday")));
    }

    @Test
    public void titleMatchesCountDouble() {
        List<String> terms = Arrays.asList("dentist");

        assertEquals(2, KeywordScorer.score(note(1, "Dentist", "", 0), terms));
        assertEquals(1, KeywordScorer.score(note(1, "Monday", "dentist at 3", 0), terms));
        assertEquals(3, KeywordScorer.score(note(1, "Dentist", "dentist at 3", 0), terms));
    }

    @Test
    public void termsMatchTheStartOfWordsOnly() {
        List<String> terms = Arrays.asList("dent");

        assertEquals(1, KeywordScorer.score(note(1, "", "Dentist", 0), terms));
        assertEquals(0, KeywordScorer.score(note(1, "", "Accident", 0), terms));
    }

    @Test
    public void rankPutsMoreMatchesFirstThenRecentlyEdited() {
        List<String> terms = Arrays.asList("milk", "eggs");
        List<Note> candidates = Arrays.asList(
                note(1, "Groceries", "milk", 100),
                note(2, "Groceries", "milk and eggs", 50),
                note(3, "Shopping", "milk", 300),
                note(4, "Nothing here", "", 400));

        assertEquals(Arrays.asList(2L, 3L, 1L), KeywordScorer.rank(candidates, terms));
    }

    @Test
    public void firstMatchingLineIsFound() {
        String text = "Groceries\nCall Dr. Smith\nPay rent";

        TextChunker.Span line = KeywordScorer.firstMatchingLine(text, Arrays.asList("smith"));

        assertEquals("Call Dr. Smith", text.substring(line.start, line.end));
        assertNull(KeywordScorer.firstMatchingLine(text, Arrays.asList("dentist")));
    }
}

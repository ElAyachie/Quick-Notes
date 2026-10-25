package com.myapp.quicknotes.search;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class TextChunkerTest {
    private static List<String> passages(String text) {
        List<String> passages = new ArrayList<>();
        for (TextChunker.Span span : TextChunker.split(text)) {
            passages.add(text.substring(span.start, span.end));
        }
        return passages;
    }

    @Test
    public void blankTextHasNoPassages() {
        assertEquals(Collections.emptyList(), TextChunker.split(""));
        assertEquals(Collections.emptyList(), TextChunker.split("  \n\n \t"));
    }

    @Test
    public void shortTextIsOnePassageWithoutSurroundingSpace() {
        assertEquals(Collections.singletonList("Buy milk"), passages("\n  Buy milk  \n"));
    }

    @Test
    public void singleLineBreaksStayInOnePassage() {
        // A list of lines is one thought, like a shopping list.
        assertEquals(Collections.singletonList("eggs\nbread\nmilk"), passages("eggs\nbread\nmilk"));
    }

    @Test
    public void blankLinesSeparateParagraphs() {
        assertEquals(Arrays.asList("Dentist on Monday.", "Call the plumber."),
                passages("Dentist on Monday.\n\nCall the plumber."));
    }

    @Test
    public void aLineOfOnlySpacesSeparatesParagraphs() {
        assertEquals(Arrays.asList("one", "two"), passages("one\n   \ntwo"));
    }

    @Test
    public void positionsPointIntoTheOriginalText() {
        String text = "  first\n\n\nsecond  ";

        List<TextChunker.Span> spans = TextChunker.split(text);

        assertEquals(Arrays.asList(new TextChunker.Span(2, 7), new TextChunker.Span(10, 16)),
                spans);
    }

    @Test
    public void longParagraphIsCutAfterASentence() {
        String sentence = "This sentence is about forty characters. ";
        StringBuilder text = new StringBuilder();
        while (text.length() < TextChunker.MAX_CHARS * 2) {
            text.append(sentence);
        }

        List<String> passages = passages(text.toString());

        assertTrue(passages.size() >= 2);
        for (String passage : passages) {
            assertTrue(passage.length() <= TextChunker.MAX_CHARS);
            assertTrue(passage, passage.endsWith("."));
        }
    }

    @Test
    public void longTextWithoutSentencesIsCutBetweenWords() {
        StringBuilder text = new StringBuilder();
        while (text.length() < TextChunker.MAX_CHARS * 2) {
            text.append("word ");
        }

        for (String passage : passages(text.toString())) {
            assertTrue(passage.length() <= TextChunker.MAX_CHARS);
            assertTrue(passage, passage.endsWith("word"));
        }
    }

    @Test
    public void longTextWithoutSpacesIsCutAtTheLimit() {
        String text = new String(new char[TextChunker.MAX_CHARS * 2 + 10]).replace('\0', 'x');

        List<TextChunker.Span> spans = TextChunker.split(text);

        assertEquals(3, spans.size());
        assertEquals(TextChunker.MAX_CHARS, spans.get(0).end);
        assertEquals(text.length(), spans.get(2).end);
    }

    @Test
    public void noTextIsLostOrRepeated() {
        String text = "Intro line.\n\n" + "Long part. ".repeat(80) + "\n\nOutro.";

        StringBuilder rejoined = new StringBuilder();
        int previousEnd = 0;
        for (TextChunker.Span span : TextChunker.split(text)) {
            assertTrue(span.start >= previousEnd);
            assertTrue(text.substring(previousEnd, span.start).isBlank());
            rejoined.append(text, span.start, span.end);
            previousEnd = span.end;
        }
        assertEquals(text.replaceAll("\\s", ""), rejoined.toString().replaceAll("\\s", ""));
    }
}

package com.myapp.quicknotes.search;

import java.util.ArrayList;
import java.util.List;

// Splits a note's text into passages to embed one by one. A single vector for a long note blurs
// its topics together, so a note about groceries and a doctor's visit would match neither well.
// Paragraphs are the natural unit; a paragraph too long for the model is cut at a sentence end.
public final class TextChunker {
    // The embedding model reads a limited number of tokens; this keeps a passage well inside it.
    static final int MAX_CHARS = 500;

    private TextChunker() {
    }

    // A passage of the note's text, from start (inclusive) to end (exclusive).
    public static final class Span {
        public final int start;
        public final int end;

        Span(int start, int end) {
            this.start = start;
            this.end = end;
        }

        @Override
        public boolean equals(Object o) {
            return o instanceof Span && ((Span) o).start == start && ((Span) o).end == end;
        }

        @Override
        public int hashCode() {
            return 31 * start + end;
        }

        @Override
        public String toString() {
            return "[" + start + ", " + end + ")";
        }
    }

    // The passages of the text in order, with surrounding blank space left out. Empty when the
    // text has nothing but blank space.
    public static List<Span> split(String text) {
        List<Span> spans = new ArrayList<>();
        int paragraphStart = 0;
        while (paragraphStart < text.length()) {
            int paragraphEnd = endOfParagraph(text, paragraphStart);
            addParagraph(text, paragraphStart, paragraphEnd, spans);
            paragraphStart = paragraphEnd;
        }
        return spans;
    }

    // A paragraph ends at a blank line: a line break followed by another with only spaces between.
    private static int endOfParagraph(String text, int from) {
        int lineBreak = text.indexOf('\n', from);
        while (lineBreak >= 0) {
            int next = lineBreak + 1;
            while (next < text.length() && text.charAt(next) != '\n'
                    && Character.isWhitespace(text.charAt(next))) {
                next++;
            }
            if (next < text.length() && text.charAt(next) == '\n') {
                return next;
            }
            lineBreak = text.indexOf('\n', next);
        }
        return text.length();
    }

    private static void addParagraph(String text, int start, int end, List<Span> spans) {
        while (true) {
            start = skipBlank(text, start, end);
            end = trimEnd(text, start, end);
            if (start >= end) {
                return;
            }
            if (end - start <= MAX_CHARS) {
                spans.add(new Span(start, end));
                return;
            }
            int cut = cutPoint(text, start, start + MAX_CHARS);
            spans.add(new Span(start, trimEnd(text, start, cut)));
            start = cut;
        }
    }

    // Where to end a passage that would otherwise run past `limit`: after the last sentence end
    // or line break before it, failing that at the last space, failing that at the limit itself.
    private static int cutPoint(String text, int start, int limit) {
        // A cut in the first half would leave a fragment; only look in the second half.
        int earliest = start + (limit - start) / 2;
        for (int i = limit - 1; i > earliest; i--) {
            char c = text.charAt(i);
            if (c == '\n' || ((c == '.' || c == '!' || c == '?')
                    && Character.isWhitespace(text.charAt(i + 1)))) {
                return i + 1;
            }
        }
        for (int i = limit - 1; i > earliest; i--) {
            if (Character.isWhitespace(text.charAt(i))) {
                return i + 1;
            }
        }
        return limit;
    }

    private static int skipBlank(String text, int start, int end) {
        while (start < end && Character.isWhitespace(text.charAt(start))) {
            start++;
        }
        return start;
    }

    private static int trimEnd(String text, int start, int end) {
        while (end > start && Character.isWhitespace(text.charAt(end - 1))) {
            end--;
        }
        return end;
    }
}

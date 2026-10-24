package com.myapp.quicknotes.search;

import androidx.annotation.Nullable;

import com.myapp.quicknotes.data.Note;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

// The word-matching half of search. Embeddings are good at meaning and poor at exact strings: a
// name, a number, a code. The full-text index finds notes containing the query's words, and
// this ranks them.
public final class KeywordScorer {
    private static final Pattern NOT_A_WORD = Pattern.compile("[^\\p{L}\\p{N}]+");
    private static final Pattern ACCENTS = Pattern.compile("\\p{M}+");
    // Words too common to say what a note is about. Matching them would bring up nearly every
    // note: "skiing in the Alps" would find everything containing "in" or "the".
    private static final Set<String> COMMON_WORDS = new HashSet<>(Arrays.asList(
            "a", "about", "after", "all", "an", "and", "any", "are", "as", "at", "be", "before",
            "but", "by", "can", "do", "for", "from", "get", "had", "has", "have", "how", "i",
            "if", "in", "into", "is", "it", "its", "me", "my", "need", "next", "of", "on", "or",
            "our", "out", "so", "some", "that", "the", "their", "them", "then", "there", "these",
            "this", "to", "up", "was", "we", "were", "what", "when", "where", "which", "who",
            "will", "with", "you", "your"));

    private KeywordScorer() {
    }

    // The query's words, lower case and without accents, each once, in the order typed. Anything
    // other than letters and digits separates words, which also keeps the full-text query free
    // of its operator characters. Common words are left out, unless the query has nothing else.
    public static List<String> terms(String query) {
        Set<String> all = new LinkedHashSet<>(words(query));
        Set<String> telling = new LinkedHashSet<>();
        for (String word : all) {
            if (!COMMON_WORDS.contains(word)) {
                telling.add(word);
            }
        }
        return new ArrayList<>(telling.isEmpty() ? all : telling);
    }

    // True when every term has a digit in it: a number, a date, a code like "QX7P2L".
    public static boolean allContainDigits(List<String> terms) {
        for (String term : terms) {
            if (!term.matches(".*\\p{N}.*")) {
                return false;
            }
        }
        return !terms.isEmpty();
    }

    // A full-text query for notes containing any of the terms, each as a word or the start of
    // one, so "dent" finds "dentist" while typing.
    public static String matchQuery(List<String> terms) {
        StringBuilder query = new StringBuilder();
        for (String term : terms) {
            if (query.length() > 0) {
                query.append(" OR ");
            }
            query.append(term).append('*');
        }
        return query.toString();
    }

    // How well a note matches: per term, 2 if a word of the title starts with it and 1 if a word
    // of the content does. The title says what a note is about, so it counts for more.
    public static int score(Note note, List<String> terms) {
        List<String> titleWords = words(note.getTitle());
        List<String> contentWords = words(note.getContent());
        int score = 0;
        for (String term : terms) {
            if (anyStartsWith(titleWords, term)) {
                score += 2;
            }
            if (anyStartsWith(contentWords, term)) {
                score += 1;
            }
        }
        return score;
    }

    // Note ids from best match to worst; recently edited notes first among equals.
    public static List<Long> rank(List<Note> candidates, List<String> terms) {
        Map<Long, Integer> scores = new HashMap<>();
        List<Note> matching = new ArrayList<>();
        for (Note note : candidates) {
            int score = score(note, terms);
            if (score > 0) {
                scores.put(note.getId(), score);
                matching.add(note);
            }
        }
        matching.sort(Comparator.comparingInt((Note note) -> scores.get(note.getId())).reversed()
                .thenComparing(Comparator.comparingLong(Note::getUpdatedAt).reversed()));
        List<Long> ids = new ArrayList<>();
        for (Note note : matching) {
            ids.add(note.getId());
        }
        return ids;
    }

    // The first line of the text that contains one of the terms, to show under the note's title;
    // null when no line does.
    @Nullable
    public static TextChunker.Span firstMatchingLine(String text, List<String> terms) {
        int lineStart = 0;
        while (lineStart < text.length()) {
            int lineEnd = text.indexOf('\n', lineStart);
            if (lineEnd < 0) {
                lineEnd = text.length();
            }
            List<String> lineWords = words(text.substring(lineStart, lineEnd));
            for (String term : terms) {
                if (anyStartsWith(lineWords, term)) {
                    return new TextChunker.Span(lineStart, lineEnd);
                }
            }
            lineStart = lineEnd + 1;
        }
        return null;
    }

    private static List<String> words(String text) {
        String plain = ACCENTS.matcher(Normalizer.normalize(text, Normalizer.Form.NFD))
                .replaceAll("").toLowerCase(Locale.ROOT);
        List<String> words = new ArrayList<>();
        for (String word : NOT_A_WORD.split(plain)) {
            if (!word.isEmpty()) {
                words.add(word);
            }
        }
        return words;
    }

    private static boolean anyStartsWith(List<String> words, String prefix) {
        for (String word : words) {
            if (word.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }
}

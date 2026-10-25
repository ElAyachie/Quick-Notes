package com.myapp.quicknotes.search;

import androidx.annotation.Nullable;

import com.myapp.quicknotes.data.DaysOfWeek;
import com.myapp.quicknotes.data.NearbyPlace;
import com.myapp.quicknotes.data.Reminder;
import com.myapp.quicknotes.data.ReminderType;
import com.myapp.quicknotes.data.Repeat;
import com.myapp.quicknotes.places.PlaceKinds;

import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

// Finds notes by their waiting reminders: by when they are due ("tomorrow") and by where they
// fire ("Walgreens", "pharmacy", a street near the place). Each match carries the reminder and
// what matched, so the result can say why the note was found.
public final class ReminderMatcher {
    // How many occurrences of a repeating reminder are looked through for one in a time range.
    // A daily reminder reaches a year ahead, which covers any range a search can name.
    private static final int MAX_OCCURRENCES = 400;

    private ReminderMatcher() {
    }

    public static final class Match {
        public final long noteId;
        public final Reminder reminder;
        // For a match by time: the occurrence in the range, which for a repeating reminder can be
        // later than the one stored.
        @Nullable
        public final Long occurrence;
        // For a match by place: the nearby place that matched, or null when the name the user
        // gave the place did.
        @Nullable
        public final String nearbyName;
        // For a match by place: true when no name matched, only what a nearby place is
        // ("pharmacy" for a Walgreens). Weaker evidence, ranked below the rest.
        public final boolean byKindOnly;
        final int score;
        final int distance;

        Match(Reminder reminder, @Nullable Long occurrence, @Nullable String nearbyName,
              boolean byKindOnly, int score, int distance) {
            this.noteId = reminder.getNoteId();
            this.reminder = reminder;
            this.occurrence = occurrence;
            this.nearbyName = nearbyName;
            this.byKindOnly = byKindOnly;
            this.score = score;
            this.distance = distance;
        }

        public boolean isByTime() {
            return occurrence != null;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Match)) return false;
            Match other = (Match) o;
            return reminder.equals(other.reminder)
                    && Objects.equals(occurrence, other.occurrence)
                    && Objects.equals(nearbyName, other.nearbyName);
        }

        @Override
        public int hashCode() {
            return Objects.hash(reminder, occurrence, nearbyName);
        }
    }

    // Notes with a time reminder due within one of the ranges, soonest first, each once.
    public static List<Match> byTime(List<Reminder> reminders, List<TimeQuery.Range> ranges,
                                     ZoneId zone) {
        List<Match> matches = new ArrayList<>();
        if (ranges.isEmpty()) {
            return matches;
        }
        for (Reminder reminder : reminders) {
            if (reminder.getType() != ReminderType.TIME) {
                continue;
            }
            Long earliest = null;
            for (TimeQuery.Range range : ranges) {
                Long occurrence = firstOccurrenceIn(reminder, range, zone);
                if (occurrence != null && (earliest == null || occurrence < earliest)) {
                    earliest = occurrence;
                }
            }
            if (earliest != null) {
                matches.add(new Match(reminder, earliest, null, false, 0, 0));
            }
        }
        matches.sort((a, b) -> Long.compare(a.occurrence, b.occurrence));
        return bestPerNote(matches);
    }

    // Notes with a location reminder whose place matches the terms, best first, each once. Per
    // term, the name the user gave the place counts most, then the name of something nearby,
    // then what something nearby is ("pharmacy" for a Walgreens). Among equals, the reminder whose
    // matching place is nearest comes first.
    //
    // What a place is only counts when it accounts for every term: "pharmacy" finds a reminder
    // near a Walgreens, but "gift for Mom" doesn't find one near a gift shop. Words that are about
    // time ("tomorrow") are left out of the terms by the caller, as they say nothing of places.
    public static List<Match> byPlace(List<Reminder> reminders,
                                      Map<String, List<NearbyPlace>> nearbyByPlace,
                                      List<String> terms) {
        List<Match> matches = new ArrayList<>();
        for (Reminder reminder : reminders) {
            if (reminder.getType() != ReminderType.LOCATION) {
                continue;
            }
            List<String> named = reminder.getPlaceName() != null
                    ? KeywordScorer.words(reminder.getPlaceName()) : Collections.emptyList();
            List<NearbyPlace> nearby = nearbyByPlace.get(reminder.placeKey());
            if (nearby == null) {
                nearby = Collections.emptyList();
            }
            int score = 0;
            NearbyPlace matched = null;
            int matchedPoints = 0;
            boolean byName = false;
            boolean everyTerm = true;
            for (String term : terms) {
                // A letter or two would match the start of half the names around.
                if (term.length() < 3 && !term.chars().allMatch(Character::isDigit)) {
                    continue;
                }
                if (startsAny(named, term)) {
                    score += 3;
                    byName = true;
                    continue;
                }
                int best = 0;
                NearbyPlace bestPlace = null;
                for (NearbyPlace place : nearby) {
                    int points = startsAny(KeywordScorer.words(place.getName()), term) ? 2
                            : matchesKind(place.getKind(), term) ? 1 : 0;
                    // Places are kept nearest first, so the first with the most points is nearest.
                    if (points > best) {
                        best = points;
                        bestPlace = place;
                    }
                }
                score += best;
                byName |= best == 2;
                everyTerm &= best > 0;
                if (bestPlace != null && (matched == null || best > matchedPoints)) {
                    matched = bestPlace;
                    matchedPoints = best;
                }
            }
            if (score > 0 && (byName || everyTerm)) {
                matches.add(new Match(reminder, null, matched != null ? matched.getName() : null,
                        !byName, score, matched != null ? matched.getDistanceMeters() : 0));
            }
        }
        matches.sort((a, b) -> a.score != b.score
                ? Integer.compare(b.score, a.score)
                : Integer.compare(a.distance, b.distance));
        return bestPerNote(matches);
    }

    // The first time the reminder is due within the range, or null. For a repeating reminder,
    // its later occurrences count too: "Friday" finds a reminder that repeats every weekday.
    @Nullable
    static Long firstOccurrenceIn(Reminder reminder, TimeQuery.Range range, ZoneId zone) {
        Long stored = reminder.getTriggerAt();
        if (stored == null) {
            return null;
        }
        long first = reminder.getFirstTriggerAt() != null ? reminder.getFirstTriggerAt() : stored;
        long time = stored;
        for (int i = 0; i < MAX_OCCURRENCES; i++) {
            if (time >= range.end) {
                return null;
            }
            if (time >= range.start) {
                return time;
            }
            Repeat repeat = reminder.getRepeat();
            if (repeat == Repeat.NONE || repeat == Repeat.EVERY_ARRIVAL) {
                return null;
            }
            time = repeat == Repeat.DAYS_OF_WEEK
                    ? DaysOfWeek.nextAfter(reminder.getRepeatDays(), first, time, zone)
                    : repeat.nextAfter(first, time, time, zone);
        }
        return null;
    }

    private static boolean matchesKind(String kind, String term) {
        if (startsAny(KeywordScorer.words(kind), term)) {
            return true;
        }
        for (String synonym : PlaceKinds.wordsFor(kind)) {
            if (startsAny(KeywordScorer.words(synonym), term)) {
                return true;
            }
        }
        return false;
    }

    private static boolean startsAny(List<String> words, String term) {
        for (String word : words) {
            if (word.startsWith(term)) {
                return true;
            }
        }
        return false;
    }

    // A note can have several reminders; it appears once, with its best.
    private static List<Match> bestPerNote(List<Match> sorted) {
        Map<Long, Match> best = new LinkedHashMap<>();
        for (Match match : sorted) {
            best.putIfAbsent(match.noteId, match);
        }
        return new ArrayList<>(best.values());
    }
}

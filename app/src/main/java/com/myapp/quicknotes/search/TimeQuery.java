package com.myapp.quicknotes.search;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.Month;
import java.time.Year;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// Reads the times in a search, so "dentist tomorrow" or "what's on Friday" can find notes by when
// their reminders are due. English only, like the rest of search: today, tonight, tomorrow, a
// weekday, this or next week, the weekend, this or next month, a month, a date like "Oct 30",
// each optionally with morning, afternoon, evening or night.
public final class TimeQuery {
    private static final Pattern NUMBER = Pattern.compile("(\\d{1,2})(st|nd|rd|th)?");
    private static final Map<String, DayOfWeek> WEEKDAYS = new HashMap<>();
    private static final Map<String, Month> MONTHS = new HashMap<>();

    static {
        for (DayOfWeek day : DayOfWeek.values()) {
            String name = day.name().toLowerCase(Locale.ROOT);
            WEEKDAYS.put(name, day);
            WEEKDAYS.put(name.substring(0, 3), day);
        }
        WEEKDAYS.put("tues", DayOfWeek.TUESDAY);
        WEEKDAYS.put("weds", DayOfWeek.WEDNESDAY);
        WEEKDAYS.put("thur", DayOfWeek.THURSDAY);
        WEEKDAYS.put("thurs", DayOfWeek.THURSDAY);
        for (Month month : Month.values()) {
            String name = month.name().toLowerCase(Locale.ROOT);
            MONTHS.put(name, month);
            MONTHS.put(name.substring(0, 3), month);
        }
        MONTHS.put("sept", Month.SEPTEMBER);
    }

    private TimeQuery() {
    }

    // A stretch of time, from start (inclusive) to end (exclusive), in milliseconds.
    public static final class Range {
        public final long start;
        public final long end;

        Range(long start, long end) {
            this.start = start;
            this.end = end;
        }

        @Override
        public boolean equals(Object o) {
            return o instanceof Range && ((Range) o).start == start && ((Range) o).end == end;
        }

        @Override
        public int hashCode() {
            return Long.hashCode(start) * 31 + Long.hashCode(end);
        }

        @Override
        public String toString() {
            return "[" + start + ", " + end + ")";
        }
    }

    // What a search says about time: the stretches it names, the words that named them, and
    // whether the search says nothing else (then searching by meaning is pointless).
    public static final class Parsed {
        public final List<Range> ranges;
        public final Set<String> timeWords;
        public final boolean onlyTime;

        Parsed(List<Range> ranges, Set<String> timeWords, boolean onlyTime) {
            this.ranges = ranges;
            this.timeWords = timeWords;
            this.onlyTime = onlyTime;
        }
    }

    public static Parsed parse(String query, long now, ZoneId zone, DayOfWeek firstDayOfWeek) {
        List<String> words = KeywordScorer.words(query);
        boolean[] used = new boolean[words.size()];
        List<Range> ranges = new ArrayList<>();
        ZonedDateTime current = ZonedDateTime.ofInstant(Instant.ofEpochMilli(now), zone);
        LocalDate today = current.toLocalDate();

        for (int i = 0; i < words.size(); i++) {
            String word = words.get(i);
            String previous = i > 0 ? words.get(i - 1) : "";
            String next = i + 1 < words.size() ? words.get(i + 1) : "";
            boolean afterNext = previous.equals("next");
            boolean afterThis = previous.equals("this");
            LocalDate day = null;
            Range range = null;

            if (word.equals("today")) {
                day = today;
            } else if (word.equals("tonight")) {
                range = between(today, LocalTime.of(18, 0), today.plusDays(1), LocalTime.MIDNIGHT,
                        zone);
            } else if (word.equals("tomorrow")) {
                day = today.plusDays(1);
            } else if (afterThis && partOfDay(word) != null) {
                // "this morning", "this evening".
                LocalTime[] hours = partOfDay(word);
                range = between(today, hours[0], hours[1].equals(LocalTime.MIDNIGHT)
                        ? today.plusDays(1) : today, hours[1], zone);
            } else if (WEEKDAYS.containsKey(word)) {
                DayOfWeek weekday = WEEKDAYS.get(word);
                day = today.with(TemporalAdjusters.nextOrSame(weekday));
                if (afterNext && day.equals(today)) {
                    day = day.plusWeeks(1);
                }
            } else if (word.equals("week") && (afterThis || afterNext)) {
                LocalDate weekStart = today.with(TemporalAdjusters.previousOrSame(firstDayOfWeek));
                LocalDate from = afterNext ? weekStart.plusWeeks(1) : today;
                range = between(from, LocalTime.MIDNIGHT, weekStart.plusWeeks(afterNext ? 2 : 1),
                        LocalTime.MIDNIGHT, zone);
            } else if (word.equals("weekend")) {
                LocalDate saturday = today.getDayOfWeek() == DayOfWeek.SUNDAY
                        ? today.minusDays(1)
                        : today.with(TemporalAdjusters.nextOrSame(DayOfWeek.SATURDAY));
                if (afterNext) {
                    saturday = saturday.plusWeeks(1);
                }
                LocalDate from = saturday.isBefore(today) ? today : saturday;
                range = between(from, LocalTime.MIDNIGHT, saturday.plusDays(2),
                        LocalTime.MIDNIGHT, zone);
            } else if (word.equals("month") && (afterThis || afterNext)) {
                LocalDate first = today.withDayOfMonth(1);
                if (afterNext) {
                    first = first.plusMonths(1);
                }
                LocalDate from = afterNext ? first : today;
                range = between(from, LocalTime.MIDNIGHT, first.plusMonths(1),
                        LocalTime.MIDNIGHT, zone);
            } else if (MONTHS.containsKey(word)) {
                Month month = MONTHS.get(word);
                Integer dayOfMonth = number(next);
                boolean dayAfter = dayOfMonth != null;
                if (dayOfMonth == null) {
                    dayOfMonth = number(previous);
                }
                // "may" is a month only next to a day ("May 3"), or as the whole search.
                if (word.equals("may") && dayOfMonth == null && words.size() > 1) {
                    continue;
                }
                if (dayOfMonth != null && dayOfMonth >= 1
                        && dayOfMonth <= month.length(true)) {
                    LocalDate date = dateOnOrAfter(today, month, dayOfMonth);
                    if (date != null) {
                        day = date;
                        used[dayAfter ? i + 1 : i - 1] = true;
                    }
                } else {
                    LocalDate first = LocalDate.of(today.getYear(), month, 1);
                    if (first.plusMonths(1).isBefore(today)
                            || first.plusMonths(1).isEqual(today)) {
                        first = first.plusYears(1);
                    }
                    LocalDate from = first.isBefore(today) ? today : first;
                    range = between(from, LocalTime.MIDNIGHT, first.plusMonths(1),
                            LocalTime.MIDNIGHT, zone);
                }
            } else {
                continue;
            }

            used[i] = true;
            if (afterNext || afterThis) {
                used[i - 1] = true;
            }
            if (day != null) {
                // "tomorrow morning", "Friday evening".
                String part = i + 1 < words.size() ? words.get(i + 1) : "";
                LocalTime[] hours = partOfDay(part);
                if (hours != null) {
                    used[i + 1] = true;
                    i++;
                    range = between(day, hours[0], hours[1].equals(LocalTime.MIDNIGHT)
                            ? day.plusDays(1) : day, hours[1], zone);
                } else {
                    range = between(day, LocalTime.MIDNIGHT, day.plusDays(1), LocalTime.MIDNIGHT,
                            zone);
                }
            }
            if (range != null) {
                ranges.add(range);
            }
        }

        Set<String> timeWords = new HashSet<>();
        for (int i = 0; i < words.size(); i++) {
            if (used[i]) {
                timeWords.add(words.get(i));
            }
        }
        boolean onlyTime = !ranges.isEmpty();
        for (String term : KeywordScorer.terms(query)) {
            if (!timeWords.contains(term)) {
                onlyTime = false;
            }
        }
        return new Parsed(ranges, timeWords, onlyTime);
    }

    // The hours a part of the day covers, or null if the word isn't one.
    private static LocalTime[] partOfDay(String word) {
        switch (word) {
            case "morning":
                return new LocalTime[]{LocalTime.of(5, 0), LocalTime.NOON};
            case "afternoon":
                return new LocalTime[]{LocalTime.NOON, LocalTime.of(17, 0)};
            case "evening":
                return new LocalTime[]{LocalTime.of(17, 0), LocalTime.MIDNIGHT};
            case "night":
                return new LocalTime[]{LocalTime.of(20, 0), LocalTime.MIDNIGHT};
            default:
                return null;
        }
    }

    private static Integer number(String word) {
        Matcher matcher = NUMBER.matcher(word);
        return matcher.matches() ? Integer.parseInt(matcher.group(1)) : null;
    }

    // The next date with this month and day, today included; null for a day the month never has
    // in the coming year (February 30th).
    private static LocalDate dateOnOrAfter(LocalDate today, Month month, int dayOfMonth) {
        for (int year = today.getYear(); year <= today.getYear() + 4; year++) {
            if (dayOfMonth <= month.length(Year.isLeap(year))) {
                LocalDate date = LocalDate.of(year, month, dayOfMonth);
                if (!date.isBefore(today)) {
                    return date;
                }
            }
        }
        return null;
    }

    private static Range between(LocalDate fromDay, LocalTime fromTime, LocalDate toDay,
                                 LocalTime toTime, ZoneId zone) {
        return new Range(ZonedDateTime.of(fromDay, fromTime, zone).toInstant().toEpochMilli(),
                ZonedDateTime.of(toDay, toTime, zone).toInstant().toEpochMilli());
    }
}

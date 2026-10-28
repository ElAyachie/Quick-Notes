package com.myapp.quicknotes.command;

import androidx.annotation.Nullable;

import com.myapp.quicknotes.data.DaysOfWeek;
import com.myapp.quicknotes.data.Repeat;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.Month;
import java.time.Year;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// Reads when a spoken or typed reminder should go off: "tomorrow at 9am", "in 20 minutes",
// "Friday at 3", "tonight", "on October 30", "every Monday at 7pm", "every day at 8". English
// only. Unlike search.TimeQuery, which reads stretches of time to search in, this settles on one
// moment, and removes the words it used so what is left is the thing to be reminded of.
final class SpokenTime {
    // A day named without an hour, and an hour with no part of the day, fall at these times.
    private static final LocalTime DEFAULT_TIME = LocalTime.of(9, 0);
    private static final Map<String, DayOfWeek> WEEKDAYS = new HashMap<>();
    private static final Map<String, Month> MONTHS = new HashMap<>();
    private static final Map<String, Integer> NUMBERS = new HashMap<>();

    static {
        for (DayOfWeek day : DayOfWeek.values()) {
            WEEKDAYS.put(day.name().toLowerCase(Locale.ROOT), day);
        }
        WEEKDAYS.put("tues", DayOfWeek.TUESDAY);
        WEEKDAYS.put("weds", DayOfWeek.WEDNESDAY);
        WEEKDAYS.put("thurs", DayOfWeek.THURSDAY);
        for (Month month : Month.values()) {
            String name = month.name().toLowerCase(Locale.ROOT);
            MONTHS.put(name, month);
            MONTHS.put(name.substring(0, 3), month);
        }
        MONTHS.put("sept", Month.SEPTEMBER);
        String[] words = {"zero", "one", "two", "three", "four", "five", "six", "seven", "eight",
                "nine", "ten", "eleven", "twelve"};
        for (int i = 0; i < words.length; i++) {
            NUMBERS.put(words[i], i);
        }
        NUMBERS.put("a", 1);
        NUMBERS.put("an", 1);
        NUMBERS.put("fifteen", 15);
        NUMBERS.put("twenty", 20);
        NUMBERS.put("thirty", 30);
        NUMBERS.put("forty-five", 45);
        NUMBERS.put("forty five", 45);
        NUMBERS.put("a couple of", 2);
        NUMBERS.put("a couple", 2);
        NUMBERS.put("a few", 3);
    }

    private static final String WEEKDAY = "(monday|tuesday|wednesday|thursday|friday|saturday|"
            + "sunday|tues|weds|thurs)";
    private static final String MONTH = "(january|february|march|april|may|june|july|august|"
            + "september|october|november|december|jan|feb|mar|apr|jun|jul|aug|sep|sept|oct|nov|"
            + "dec)\\.?";
    private static final String ORDINAL = "(\\d{1,2})(?:st|nd|rd|th)?";
    private static final int FLAGS = Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE;

    private static final Pattern EVERY_WEEKDAY = Pattern.compile(
            "\\b(?:every|each)\\s+weekday\\b|\\b(?:on\\s+)?weekdays\\b", FLAGS);
    // "every Monday and Thursday", "on Mondays and Wednesdays".
    private static final Pattern EVERY_DAYS = Pattern.compile(
            "\\b(?:(?:every|each)\\s+" + WEEKDAY + "s?|on\\s+" + WEEKDAY + "s)"
                    + "(?:\\s*(?:,\\s*and|,|and|&)\\s*" + WEEKDAY + "s?)*\\b", FLAGS);
    private static final Pattern EVERY_PART = Pattern.compile(
            "\\b(?:every|each)\\s+(morning|afternoon|evening|night)\\b", FLAGS);
    private static final Pattern EVERY = Pattern.compile(
            "\\b(?:(?:every|each)\\s+(day|week|month|year)|(everyday|daily|weekly|monthly|"
                    + "yearly|annually))\\b", FLAGS);
    private static final Pattern RELATIVE = Pattern.compile(
            "\\bin\\s+(\\d+|half\\s+an|an|a\\s+couple(?:\\s+of)?|a\\s+few|a|one|two|three|four|"
                    + "five|six|seven|eight|nine|ten|eleven|twelve|fifteen|twenty|thirty|"
                    + "forty[- ]five)\\s+(minutes?|mins?|hours?|hrs?|days?|weeks?)\\b", FLAGS);
    private static final Pattern NAMED_DAY = Pattern.compile(
            "\\b(?:the\\s+)?(day\\s+after\\s+tomorrow|today|tonight|tomorrow|tmrw)\\b", FLAGS);
    private static final Pattern NEXT_WEEK = Pattern.compile("\\bnext\\s+week\\b", FLAGS);
    private static final Pattern ON_WEEKDAY = Pattern.compile(
            "\\b(?:(?:on|for|by|this|until)\\s+)?(next\\s+)?" + WEEKDAY + "\\b", FLAGS);
    private static final Pattern MONTH_DAY = Pattern.compile(
            "\\b(?:(?:on|for|by)\\s+)?" + MONTH + "\\s+" + ORDINAL + "\\b", FLAGS);
    private static final Pattern DAY_OF_MONTH = Pattern.compile(
            "\\b(?:(?:on|for|by)\\s+)?(?:the\\s+)?" + ORDINAL + "\\s+of\\s+" + MONTH, FLAGS);
    private static final Pattern THE_NTH = Pattern.compile(
            "\\b(?:(?:on|for|by)\\s+)?the\\s+(\\d{1,2})(?:st|nd|rd|th)\\b", FLAGS);
    // "5pm", "5:30 p.m.", "at 9 AM".
    private static final Pattern CLOCK_MERIDIEM = Pattern.compile(
            "\\b(?:(?:at|by|around)\\s+)?(\\d{1,2})(?::(\\d{2}))?\\s*([ap])\\.?\\s?m\\b\\.?",
            FLAGS);
    // "at 5", "at 17:30", "at 6 o'clock".
    private static final Pattern CLOCK_AT = Pattern.compile(
            "\\b(?:at|by|around)\\s+(\\d{1,2})(?::(\\d{2}))?(?:\\s*o'?\\s?clock)?\\b", FLAGS);
    private static final Pattern CLOCK_BARE = Pattern.compile("\\b(\\d{1,2}):(\\d{2})\\b",
            FLAGS);
    private static final Pattern NOON = Pattern.compile(
            "\\b(?:(?:at|by|around)\\s+)?(noon|midday|midnight)\\b", FLAGS);
    private static final Pattern PART_OF_DAY = Pattern.compile(
            "\\b(?:(?:in\\s+the|this|at)\\s+)?(morning|afternoon|evening|night)\\b", FLAGS);

    private SpokenTime() {
    }

    // When a reminder goes off and how it repeats, and the text with those words taken out.
    static final class Found {
        // Null when the text names no time.
        @Nullable
        final Long triggerAt;
        final Repeat repeat;
        final int repeatDays;
        final String rest;

        Found(@Nullable Long triggerAt, Repeat repeat, int repeatDays, String rest) {
            this.triggerAt = triggerAt;
            this.repeat = repeat;
            this.repeatDays = repeatDays;
            this.rest = rest;
        }
    }

    // The words that were read are removed from the text one pattern at a time, so later
    // patterns don't read them again.
    private static final class Text {
        String value;

        Text(String value) {
            this.value = value;
        }

        @Nullable
        Matcher take(Pattern pattern) {
            Matcher matcher = pattern.matcher(value);
            if (!matcher.find()) {
                return null;
            }
            value = value.substring(0, matcher.start()) + " " + value.substring(matcher.end());
            return matcher;
        }
    }

    static Found find(String input, long now, ZoneId zone) {
        Text text = new Text(input);
        ZonedDateTime current = Instant.ofEpochMilli(now).atZone(zone);
        LocalDate today = current.toLocalDate();

        Repeat repeat = Repeat.NONE;
        int repeatDays = DaysOfWeek.NONE;
        String partOfDay = null;
        Matcher m;
        if (text.take(EVERY_WEEKDAY) != null) {
            repeat = Repeat.DAYS_OF_WEEK;
            repeatDays = DaysOfWeek.of(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
                    DayOfWeek.THURSDAY, DayOfWeek.FRIDAY);
        } else if ((m = text.take(EVERY_DAYS)) != null) {
            repeat = Repeat.DAYS_OF_WEEK;
            Matcher day = Pattern.compile(WEEKDAY, FLAGS).matcher(m.group());
            while (day.find()) {
                repeatDays = DaysOfWeek.with(repeatDays,
                        WEEKDAYS.get(day.group(1).toLowerCase(Locale.ROOT)), true);
            }
        } else if ((m = text.take(EVERY_PART)) != null) {
            repeat = Repeat.DAILY;
            partOfDay = m.group(1).toLowerCase(Locale.ROOT);
        } else if ((m = text.take(EVERY)) != null) {
            String unit = (m.group(1) != null ? m.group(1) : m.group(2)).toLowerCase(Locale.ROOT);
            if (unit.startsWith("da") || unit.equals("everyday")) {
                repeat = Repeat.DAILY;
            } else if (unit.startsWith("week")) {
                repeat = Repeat.WEEKLY;
            } else if (unit.startsWith("month")) {
                repeat = Repeat.MONTHLY;
            } else {
                repeat = Repeat.YEARLY;
            }
        }

        // "In 20 minutes" is a moment of its own; nothing else needs reading.
        if (repeat == Repeat.NONE && (m = text.take(RELATIVE)) != null) {
            long amount = amountOf(m.group(1));
            String unit = m.group(2).toLowerCase(Locale.ROOT);
            ZonedDateTime at;
            if (unit.startsWith("m")) {
                at = current.plusMinutes(amount == -1 ? 30 : amount);
            } else if (unit.startsWith("h")) {
                at = amount == -1 ? current.plusMinutes(30) : current.plusHours(amount);
            } else if (unit.startsWith("d")) {
                at = current.plusDays(Math.max(amount, 1));
            } else {
                at = current.plusWeeks(Math.max(amount, 1));
            }
            return new Found(at.withNano(0).toInstant().toEpochMilli(), Repeat.NONE,
                    DaysOfWeek.NONE, clean(text.value));
        }

        // The day.
        LocalDate date = null;
        boolean weekday = false;
        if ((m = text.take(NAMED_DAY)) != null) {
            String word = m.group(1).toLowerCase(Locale.ROOT);
            if (word.startsWith("day")) {
                date = today.plusDays(2);
            } else if (word.equals("today")) {
                date = today;
            } else if (word.equals("tonight")) {
                date = today;
                partOfDay = "night";
            } else {
                date = today.plusDays(1);
            }
        } else if (text.take(NEXT_WEEK) != null) {
            date = today.plusWeeks(1);
        } else if ((m = text.take(MONTH_DAY)) != null) {
            date = dateOnOrAfter(today, MONTHS.get(m.group(1).toLowerCase(Locale.ROOT)),
                    Integer.parseInt(m.group(2)));
        } else if ((m = text.take(DAY_OF_MONTH)) != null) {
            date = dateOnOrAfter(today, MONTHS.get(m.group(2).toLowerCase(Locale.ROOT)),
                    Integer.parseInt(m.group(1)));
        } else if (repeat == Repeat.NONE && (m = text.take(ON_WEEKDAY)) != null) {
            DayOfWeek day = WEEKDAYS.get(m.group(2).toLowerCase(Locale.ROOT));
            date = m.group(1) != null
                    ? today.with(TemporalAdjusters.next(day))
                    : today.with(TemporalAdjusters.nextOrSame(day));
            weekday = true;
        } else if ((m = text.take(THE_NTH)) != null) {
            int dayOfMonth = Integer.parseInt(m.group(1));
            LocalDate candidate = null;
            for (int months = 0; months < 3 && candidate == null; months++) {
                LocalDate month = today.withDayOfMonth(1).plusMonths(months);
                if (dayOfMonth >= 1 && dayOfMonth <= month.lengthOfMonth()) {
                    LocalDate day = month.withDayOfMonth(dayOfMonth);
                    candidate = day.isBefore(today) ? null : day;
                }
            }
            date = candidate;
        }

        // The time of day.
        if (partOfDay == null && (m = text.take(PART_OF_DAY)) != null) {
            partOfDay = m.group(1).toLowerCase(Locale.ROOT);
        }
        LocalTime time = null;
        boolean meridiemGiven = false;
        if ((m = text.take(CLOCK_MERIDIEM)) != null) {
            int hour = Integer.parseInt(m.group(1)) % 12;
            if (m.group(3).equalsIgnoreCase("p")) {
                hour += 12;
            }
            time = clock(hour, m.group(2));
            meridiemGiven = true;
        } else if ((m = text.take(NOON)) != null) {
            time = m.group(1).equalsIgnoreCase("midnight") ? LocalTime.of(23, 59)
                    : LocalTime.NOON;
            meridiemGiven = true;
        } else if ((m = text.take(CLOCK_AT)) != null || (m = text.take(CLOCK_BARE)) != null) {
            time = clock(Integer.parseInt(m.group(1)), m.group(2));
        }
        if (time != null && !meridiemGiven && time.getHour() >= 1 && time.getHour() <= 12) {
            time = guessHalfOfDay(time, partOfDay, date, current);
        }
        if (time == null && partOfDay != null) {
            time = timeOf(partOfDay);
        }

        if (date == null && time == null && repeat == Repeat.NONE) {
            return new Found(null, Repeat.NONE, DaysOfWeek.NONE, clean(text.value));
        }
        if (time == null) {
            time = DEFAULT_TIME;
        }
        long triggerAt;
        if (repeat == Repeat.DAYS_OF_WEEK) {
            long timeOfDay = ZonedDateTime.of(today, time, zone).toInstant().toEpochMilli();
            triggerAt = DaysOfWeek.nextAfter(repeatDays, timeOfDay, now, zone);
        } else {
            ZonedDateTime at = ZonedDateTime.of(date != null ? date : today, time, zone);
            // A time already past today means the next one; a weekday already past, next week's.
            if (!at.isAfter(current)) {
                if (date == null) {
                    at = at.plusDays(1);
                } else if (weekday) {
                    at = at.plusWeeks(1);
                }
            }
            triggerAt = at.toInstant().toEpochMilli();
        }
        return new Found(triggerAt, repeat, repeatDays, clean(text.value));
    }

    // -1 means half an hour.
    private static long amountOf(String words) {
        String key = words.toLowerCase(Locale.ROOT).replaceAll("\\s+", " ");
        if (key.equals("half an")) {
            return -1;
        }
        Integer number = NUMBERS.get(key);
        return number != null ? number : Long.parseLong(key);
    }

    private static LocalTime clock(int hour, @Nullable String minutes) {
        int minute = minutes != null ? Integer.parseInt(minutes) : 0;
        return LocalTime.of(Math.min(hour, 23), Math.min(minute, 59));
    }

    // "At 5" without am or pm: the part of the day decides if one was given; otherwise 1 to 6
    // mean the afternoon, and later hours the next time the clock shows them.
    private static LocalTime guessHalfOfDay(LocalTime time, @Nullable String partOfDay,
                                            @Nullable LocalDate date, ZonedDateTime now) {
        int hour = time.getHour() % 12;
        if (partOfDay != null) {
            return partOfDay.equals("morning") ? time.withHour(hour)
                    : time.withHour(hour + 12);
        }
        if (hour >= 1 && hour <= 6) {
            return time.withHour(hour + 12);
        }
        if (time.getHour() == 12) {
            return time;
        }
        if (date == null) {
            LocalTime morning = time.withHour(hour);
            return morning.isAfter(now.toLocalTime()) ? morning : time.withHour(hour + 12);
        }
        return time.withHour(hour);
    }

    private static LocalTime timeOf(String partOfDay) {
        switch (partOfDay) {
            case "morning":
                return DEFAULT_TIME;
            case "afternoon":
                return LocalTime.of(14, 0);
            case "evening":
                return LocalTime.of(18, 0);
            default:
                return LocalTime.of(20, 0);
        }
    }

    // The next date with this month and day, today included.
    @Nullable
    private static LocalDate dateOnOrAfter(LocalDate today, Month month, int dayOfMonth) {
        for (int year = today.getYear(); year <= today.getYear() + 4; year++) {
            if (dayOfMonth >= 1 && dayOfMonth <= month.length(Year.isLeap(year))) {
                LocalDate date = LocalDate.of(year, month, dayOfMonth);
                if (!date.isBefore(today)) {
                    return date;
                }
            }
        }
        return null;
    }

    private static String clean(String text) {
        return text.replaceAll("\\s+", " ").trim();
    }
}

package com.myapp.quicknotes.command;

import androidx.annotation.Nullable;

import com.myapp.quicknotes.data.DaysOfWeek;
import com.myapp.quicknotes.data.PlaceTrigger;
import com.myapp.quicknotes.data.Repeat;

import java.time.ZoneId;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// Reads a request with patterns, on the phone, in English: how it opens ("set me a location
// reminder", "remind me", "note to self"), then when (SpokenTime), then where ("when I get to
// Walgreens", "when I leave work", "at Target"). What is left is the note. A place said with an
// arriving or leaving phrase wins over a time, since a reminder has one or the other.
//
// It reads common phrasings, not every one; the screen shows what it understood before anything
// is saved.
public class RuleCommandReader implements CommandReader {
    private static final int FLAGS = Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE;
    private static final String POLITE = "^(?:(?:hey|hi|ok|okay|so)[,!]?\\s+)?"
            + "(?:(?:please|can\\s+you|could\\s+you|would\\s+you|will\\s+you|i\\s+want\\s+you\\s+to|"
            + "i'?d\\s+like\\s+you\\s+to)\\s+)*(?:please\\s+)?";
    private static final Pattern SET_REMINDER = Pattern.compile(POLITE
            + "(?:set|create|make|add|give|schedule|put\\s+in|put)\\s+(?:(?:me|up|myself)\\s+)*"
            + "(?:an?\\s+|another\\s+)?(?:new\\s+)?(?:(location|place|time)(?:[- ]based)?\\s+)?"
            + "reminder(?:\\s+for\\s+me)?\\b[:,]?", FLAGS);
    private static final Pattern REMIND_ME = Pattern.compile(POLITE
            + "remind\\s+(?:me|us)\\b[:,]?", FLAGS);
    private static final Pattern NOTE = Pattern.compile(POLITE
            + "(?:(?:take|make|add|create|write|jot)\\s+(?:down\\s+)?(?:an?\\s+)?"
            + "(?:new\\s+|quick\\s+)?note|note\\s+to\\s+self|new\\s+note|note)\\b[:,]?", FLAGS);

    // A place runs up to whatever introduces the note ("to", "that", "remind me") or to the end.
    private static final String PLACE = "(.+?)(?=\\s+(?:to|so|that|because|about|remind|"
            + "and\\s+(?:remind|tell))\\b|\\s*[,;.!?]|$)";
    private static final String WHEN = "(when(?:ever)?|once|as\\s+soon\\s+as|every\\s+time|"
            + "each\\s+time|after|(?:the\\s+)?next\\s+time)\\s+(?:i|we)";
    private static final Pattern LEAVING = Pattern.compile("\\b" + WHEN
            + "(?:'m|'re|\\s+am|\\s+are)?\\s+(?:leave|leaving|exit|exiting|get\\s+out\\s+of|"
            + "head\\s+out\\s+of|walk\\s+out\\s+of|depart(?:\\s+from)?)\\s+(?:the\\s+|my\\s+)?"
            + PLACE, FLAGS);
    // "When I'm at", "when I get to", "once I arrive at", "when I get home". Being somewhere
    // needs a preposition ("when I'm hungry" is no place); getting somewhere doesn't, except
    // after words that make it something else ("get up", "get ready").
    private static final Pattern ARRIVING = Pattern.compile("\\b" + WHEN
            + "(?:(?:'m|'re|\\s+am|\\s+are)(?:\\s+back)?\\s+(?:at|in|near|by|close\\s+to|around)"
            + "|\\s+(?:get|arrive|reach|go|stop|pass|come|walk|drive|head|swing|make\\s+it|"
            + "pull\\s+up)(?!\\s+(?:up|off|out|ready|done|a\\s+chance|the\\s+chance|bored|tired|"
            + "hungry|sick|time|paid|there|a\\s+minute)\\b)(?:\\s+by)?(?:\\s+back)?"
            + "(?:\\s+(?:to|at|in|near|into|over\\s+to|past|up\\s+to|by))?)"
            + "\\s+(?:the\\s+|my\\s+)?" + PLACE, FLAGS);
    // After "location reminder": "for Walgreens to ...".
    private static final Pattern FOR_PLACE = Pattern.compile(
            "^\\s*(?:for|at|near|by)\\s+(?:the\\s+|my\\s+)?" + PLACE, FLAGS);
    // "Buy milk at Target". Only taken as a place when nothing else is: after "location
    // reminder", or when the place has a capital letter, as names do ("look at the report" is
    // not a place).
    private static final Pattern AT_PLACE = Pattern.compile(
            "\\b(?:at|near)\\s+(?:the\\s+)?" + PLACE, FLAGS);
    private static final Pattern EVERY_VISIT = Pattern.compile(
            "\\b(?:every\\s+time|each\\s+time|every\\s+visit|whenever)\\b", FLAGS);

    private static final Pattern LEADING = Pattern.compile("^(?:\\s|[,:;-]|(?:to|that|about|"
            + "for|and|then|please|me\\s+to|me|remind\\s+me\\s+to|remind\\s+me|tell\\s+me\\s+to|"
            + "i\\s+(?:need|have|want|should|must|gotta|got)\\s+to|i\\s+need|"
            + "(?:we\\s+)?need\\s+to|have\\s+to)\\b)+", FLAGS);
    private static final Pattern TRAILING = Pattern.compile("(?:\\s|[,:;.!?-]|\\b(?:please|"
            + "thanks|thank\\s+you|for\\s+me|to|and|remind\\s+me)\\b)+$", FLAGS);

    private enum Hint {
        NONE,
        REMINDER,
        TIME,
        LOCATION,
        NOTE,
    }

    @Override
    public Command read(String request, long now, ZoneId zone) {
        String body = request.replaceAll("\\s+", " ").trim();
        Hint hint = Hint.NONE;
        Matcher head;
        if ((head = SET_REMINDER.matcher(body)).find()) {
            String kind = head.group(1);
            if (kind == null) {
                hint = Hint.REMINDER;
            } else {
                hint = kind.equalsIgnoreCase("time") ? Hint.TIME : Hint.LOCATION;
            }
        } else if ((head = REMIND_ME.matcher(body)).find()) {
            hint = Hint.REMINDER;
        } else if ((head = NOTE.matcher(body)).find()) {
            hint = Hint.NOTE;
        } else {
            head = null;
        }
        if (head != null) {
            body = body.substring(head.end());
        }
        if (hint == Hint.NOTE) {
            return new Command(Command.Kind.NOTE, task(body), null, null, PlaceTrigger.ARRIVING,
                    Repeat.NONE, DaysOfWeek.NONE);
        }

        SpokenTime.Found time = SpokenTime.find(body, now, zone);
        body = time.rest;

        String place = null;
        PlaceTrigger trigger = PlaceTrigger.ARRIVING;
        boolean everyVisit = false;
        Matcher m = find(LEAVING, body);
        if (m != null) {
            trigger = PlaceTrigger.LEAVING;
        } else {
            m = find(ARRIVING, body);
        }
        if (m != null) {
            String starter = m.group(1).toLowerCase(Locale.ROOT);
            everyVisit = starter.startsWith("every") || starter.startsWith("each")
                    || starter.equals("whenever");
        } else if (hint == Hint.LOCATION) {
            m = find(FOR_PLACE, body);
        }
        if (m == null && time.triggerAt == null) {
            Matcher at = find(AT_PLACE, body);
            if (at != null && (hint == Hint.LOCATION || looksLikeAName(at.group(1)))) {
                m = at;
            }
        }
        if (m != null) {
            place = placeName(m.group(m.groupCount()));
            body = body.substring(0, m.start()) + " " + body.substring(m.end());
        }
        Matcher visit = EVERY_VISIT.matcher(body);
        if (visit.find()) {
            everyVisit = true;
            body = body.substring(0, visit.start()) + " " + body.substring(visit.end());
        }
        String text = task(body);

        if (place != null || hint == Hint.LOCATION) {
            return new Command(Command.Kind.LOCATION, text, null, place, trigger,
                    everyVisit ? Repeat.EVERY_ARRIVAL : Repeat.NONE, DaysOfWeek.NONE);
        }
        if (time.triggerAt != null || hint == Hint.TIME || hint == Hint.REMINDER) {
            return new Command(Command.Kind.TIME, text, time.triggerAt, null,
                    PlaceTrigger.ARRIVING, time.repeat, time.repeatDays);
        }
        return new Command(Command.Kind.NOTE, text, null, null, PlaceTrigger.ARRIVING,
                Repeat.NONE, DaysOfWeek.NONE);
    }

    // The note's text: the words left over, without the joining words around them, starting
    // with a capital.
    static String task(String words) {
        String text = words.replaceAll("\\s+", " ").trim();
        text = LEADING.matcher(text).replaceFirst("");
        text = TRAILING.matcher(text).replaceFirst("").trim();
        if (text.isEmpty()) {
            return text;
        }
        return text.substring(0, 1).toUpperCase(Locale.getDefault()) + text.substring(1);
    }

    @Nullable
    private static Matcher find(Pattern pattern, String text) {
        Matcher matcher = pattern.matcher(text);
        return matcher.find() ? matcher : null;
    }

    @Nullable
    private static String placeName(String said) {
        String name = TRAILING.matcher(said.trim()).replaceFirst("").trim();
        return name.isEmpty() ? null : name;
    }

    private static boolean looksLikeAName(String said) {
        for (String word : said.trim().split("\\s+")) {
            if (!word.isEmpty() && (Character.isUpperCase(word.charAt(0))
                    || Character.isDigit(word.charAt(0)))) {
                return true;
            }
        }
        return false;
    }
}

package com.myapp.quicknotes.ui.common;

import android.content.Context;

import androidx.annotation.StringRes;

import com.myapp.quicknotes.R;
import com.myapp.quicknotes.data.DaysOfWeek;
import com.myapp.quicknotes.data.PlaceTrigger;
import com.myapp.quicknotes.data.Reminder;
import com.myapp.quicknotes.data.ReminderType;
import com.myapp.quicknotes.data.Repeat;
import com.myapp.quicknotes.search.ReminderMatcher;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.TextStyle;
import java.time.temporal.WeekFields;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

// How reminders are described to the user.
public final class ReminderText {
    private ReminderText() {
    }

    // When or where the reminder fires and whether it repeats, e.g. "Oct 5, 2026 4:55 PM",
    // "Oct 5, 2026 4:55 PM · Every week", "Oct 5, 2026 4:55 PM · Mon, Wed, Fri",
    // "Within 200 m of Home · Every visit" or, for one that fires on leaving,
    // "More than 200 m from Work".
    public static String trigger(Context context, Reminder reminder) {
        String trigger;
        if (reminder.getType() == ReminderType.TIME) {
            trigger = Formats.dateTime(Objects.requireNonNull(reminder.getTriggerAt()));
        } else {
            String place = reminder.getPlaceName();
            if (place == null) {
                place = context.getString(R.string.coordinates,
                        reminder.getLatitude(), reminder.getLongitude());
            }
            trigger = context.getString(
                    reminder.getPlaceTrigger() == PlaceTrigger.LEAVING
                            ? R.string.beyond_distance_of_place
                            : R.string.within_distance_of_place,
                    Formats.distance(context, Objects.requireNonNull(reminder.getRadiusMeters())),
                    place);
        }
        if (reminder.getRepeat() == Repeat.NONE) {
            return trigger;
        }
        return context.getString(R.string.trigger_with_repeat, trigger,
                repeatLabel(context, reminder.getRepeat(), reminder.getRepeatDays()));
    }

    // Why a search found a note through its reminder: "Reminder Oct 30, 2026 9:00 AM · Every day",
    // "Reminder when you arrive at Walgreens", "Reminder when you arrive near Walgreens" (a place
    // the user didn't name, recognised by what is near it), "Reminder when you leave Home, near
    // Elm Street".
    public static String searchReason(Context context, ReminderMatcher.Match match) {
        Reminder reminder = match.reminder;
        if (match.isByTime()) {
            String when = context.getString(R.string.search_reason_time,
                    Formats.dateTime(Objects.requireNonNull(match.occurrence)));
            return reminder.getRepeat() == Repeat.NONE ? when
                    : context.getString(R.string.trigger_with_repeat, when,
                    repeatLabel(context, reminder.getRepeat(), reminder.getRepeatDays()));
        }
        boolean leaving = reminder.getPlaceTrigger() == PlaceTrigger.LEAVING;
        String named = reminder.getPlaceName();
        String nearby = match.nearbyName;
        if (nearby != null && named != null && !nearby.equalsIgnoreCase(named)) {
            return context.getString(leaving ? R.string.search_reason_leave_named_near
                    : R.string.search_reason_arrive_named_near, named, nearby);
        }
        if (named == null && nearby != null) {
            return context.getString(leaving ? R.string.search_reason_leave_near
                    : R.string.search_reason_arrive_near, nearby);
        }
        String place = named != null ? named : context.getString(R.string.coordinates,
                reminder.getLatitude(), reminder.getLongitude());
        return context.getString(leaving ? R.string.search_reason_leave
                : R.string.search_reason_arrive, place);
    }

    // How often a reminder repeats, in words. For chosen days of the week, the days themselves:
    // "Mon, Wed, Fri".
    public static String repeatLabel(Context context, Repeat repeat, int repeatDays) {
        if (repeat != Repeat.DAYS_OF_WEEK) {
            return context.getString(repeat(repeat));
        }
        StringBuilder days = new StringBuilder();
        for (DayOfWeek day : weekInLocalOrder()) {
            if (DaysOfWeek.contains(repeatDays, day)) {
                if (days.length() > 0) {
                    days.append(", ");
                }
                days.append(day.getDisplayName(TextStyle.SHORT, Locale.getDefault()));
            }
        }
        return days.toString();
    }

    // A ready-made time as offered to the user, with the moment it stands for:
    // "Later today · 6:00 PM", "Tomorrow morning · 9:00 AM", "Next week · Sat 9:00 AM".
    public static String presetLabel(Context context, ReminderPreset preset, long time) {
        switch (preset) {
            case LATER_TODAY:
                return context.getString(R.string.preset_later_today, Formats.time(time));
            case TOMORROW_MORNING:
                return context.getString(R.string.preset_tomorrow_morning, Formats.time(time));
            default:
                String day = Instant.ofEpochMilli(time).atZone(ZoneId.systemDefault())
                        .getDayOfWeek().getDisplayName(TextStyle.SHORT, Locale.getDefault());
                return context.getString(R.string.preset_next_week, day, Formats.time(time));
        }
    }

    // The seven days starting from the one the user's region starts its week on.
    public static List<DayOfWeek> weekInLocalOrder() {
        DayOfWeek first = WeekFields.of(Locale.getDefault()).getFirstDayOfWeek();
        List<DayOfWeek> week = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            week.add(first.plus(i));
        }
        return week;
    }

    @StringRes
    public static int repeat(Repeat repeat) {
        switch (repeat) {
            case DAILY:
                return R.string.repeat_daily;
            case WEEKLY:
                return R.string.repeat_weekly;
            case MONTHLY:
                return R.string.repeat_monthly;
            case YEARLY:
                return R.string.repeat_yearly;
            case DAYS_OF_WEEK:
                return R.string.repeat_days_of_week;
            case EVERY_ARRIVAL:
                return R.string.repeat_every_arrival;
            default:
                return R.string.repeat_none;
        }
    }
}

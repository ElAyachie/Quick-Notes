package com.myapp.quicknotes.ui.common;

import android.content.Context;

import androidx.annotation.StringRes;

import com.myapp.quicknotes.R;
import com.myapp.quicknotes.data.Reminder;
import com.myapp.quicknotes.data.ReminderType;
import com.myapp.quicknotes.data.Repeat;

import java.util.Objects;

// How reminders are described to the user.
public final class ReminderText {
    private ReminderText() {
    }

    // When or where the reminder fires and whether it repeats, e.g. "Oct 5, 2026 4:55 PM",
    // "Oct 5, 2026 4:55 PM · Every week" or "Within 200 m of Home · Every visit".
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
            trigger = context.getString(R.string.within_distance_of_place,
                    Formats.distance(context, Objects.requireNonNull(reminder.getRadiusMeters())),
                    place);
        }
        if (reminder.getRepeat() == Repeat.NONE) {
            return trigger;
        }
        return context.getString(R.string.trigger_with_repeat, trigger,
                context.getString(repeat(reminder.getRepeat())));
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
            case EVERY_ARRIVAL:
                return R.string.repeat_every_arrival;
            default:
                return R.string.repeat_none;
        }
    }
}

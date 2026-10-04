package com.myapp.quicknotes.ui.common;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.text.format.DateFormat;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.myapp.quicknotes.R;
import com.myapp.quicknotes.data.DaysOfWeek;
import com.myapp.quicknotes.data.Repeat;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

// Asks when a time reminder should fire: the date, the time of day, and whether to repeat. A
// reminder repeating on specific days of the week gets a fourth question, which days. Used both
// to set a new reminder and to change an existing one. A new reminder is first offered a few
// ready-made times, which skip those questions.
public final class ReminderTimePicker {
    public interface OnPicked {
        // repeatDays is a DaysOfWeek set when repeat is DAYS_OF_WEEK, and empty otherwise.
        void onPicked(long triggerAt, Repeat repeat, int repeatDays);
    }

    private static final Repeat[] REPEAT_CHOICES = {
            Repeat.NONE, Repeat.DAILY, Repeat.WEEKLY, Repeat.DAYS_OF_WEEK, Repeat.MONTHLY,
            Repeat.YEARLY};

    private ReminderTimePicker() {
    }

    // For a new reminder: offers the ready-made times, with the full questions as the last
    // choice. A ready-made time sets a reminder that doesn't repeat.
    public static void showForNewReminder(Context context, OnPicked onPicked) {
        long now = System.currentTimeMillis();
        List<String> labels = new ArrayList<>();
        List<Long> times = new ArrayList<>();
        for (ReminderPreset preset : ReminderPreset.values()) {
            Long time = preset.timeFor(now, ZoneId.systemDefault());
            if (time != null) {
                labels.add(ReminderText.presetLabel(context, preset, time));
                times.add(time);
            }
        }
        labels.add(context.getString(R.string.pick_date_and_time));
        new MaterialAlertDialogBuilder(context)
                .setTitle(R.string.remind_me)
                .setItems(labels.toArray(new String[0]), (dialog, which) -> {
                    if (which < times.size()) {
                        onPicked.onPicked(times.get(which), Repeat.NONE, DaysOfWeek.NONE);
                    } else {
                        show(context, System.currentTimeMillis(), Repeat.NONE, DaysOfWeek.NONE,
                                onPicked);
                    }
                })
                .show();
    }

    // Each step starts out at the given time and repeat setting. Nothing is reported if the user
    // backs out of a step or picks a moment that has already passed.
    public static void show(Context context, long initialTime, Repeat initialRepeat,
                            int initialRepeatDays, OnPicked onPicked) {
        Calendar initial = Calendar.getInstance();
        initial.setTimeInMillis(initialTime);
        DatePickerDialog datePicker = new DatePickerDialog(context,
                (picker, year, month, day) -> new TimePickerDialog(context,
                        (timePicker, hour, minute) -> {
                            Calendar chosen = Calendar.getInstance();
                            chosen.set(year, month, day, hour, minute, 0);
                            chosen.set(Calendar.MILLISECOND, 0);
                            pickRepeat(context, chosen.getTimeInMillis(), initialRepeat,
                                    initialRepeatDays, onPicked);
                        },
                        initial.get(Calendar.HOUR_OF_DAY), initial.get(Calendar.MINUTE),
                        DateFormat.is24HourFormat(context)).show(),
                initial.get(Calendar.YEAR), initial.get(Calendar.MONTH),
                initial.get(Calendar.DAY_OF_MONTH));
        datePicker.getDatePicker().setMinDate(System.currentTimeMillis());
        datePicker.show();
    }

    private static void pickRepeat(Context context, long triggerAt, Repeat initialRepeat,
                                   int initialRepeatDays, OnPicked onPicked) {
        if (triggerAt <= System.currentTimeMillis()) {
            Toast.makeText(context, R.string.error_reminder_in_past, Toast.LENGTH_LONG).show();
            return;
        }
        String[] labels = new String[REPEAT_CHOICES.length];
        int current = 0;
        for (int i = 0; i < REPEAT_CHOICES.length; i++) {
            labels[i] = context.getString(ReminderText.repeat(REPEAT_CHOICES[i]));
            if (REPEAT_CHOICES[i] == initialRepeat) {
                current = i;
            }
        }
        new MaterialAlertDialogBuilder(context)
                .setTitle(R.string.repeat_title)
                .setSingleChoiceItems(labels, current, (dialog, which) -> {
                    dialog.dismiss();
                    if (REPEAT_CHOICES[which] == Repeat.DAYS_OF_WEEK) {
                        pickDays(context, triggerAt, initialRepeatDays, onPicked);
                    } else {
                        onPicked.onPicked(triggerAt, REPEAT_CHOICES[which], DaysOfWeek.NONE);
                    }
                })
                .show();
    }

    // Which days of the week the reminder comes back on, like the day buttons of an alarm clock.
    // Starts from the days it already repeats on, or else from the weekday of the chosen date.
    private static void pickDays(Context context, long triggerAt, int initialRepeatDays,
                                 OnPicked onPicked) {
        ZoneId zone = ZoneId.systemDefault();
        List<DayOfWeek> week = ReminderText.weekInLocalOrder();
        int[] chosen = {initialRepeatDays != DaysOfWeek.NONE
                ? initialRepeatDays
                : DaysOfWeek.of(Instant.ofEpochMilli(triggerAt).atZone(zone).getDayOfWeek())};
        String[] labels = new String[week.size()];
        boolean[] checked = new boolean[week.size()];
        for (int i = 0; i < week.size(); i++) {
            labels[i] = week.get(i).getDisplayName(TextStyle.FULL, Locale.getDefault());
            checked[i] = DaysOfWeek.contains(chosen[0], week.get(i));
        }
        new MaterialAlertDialogBuilder(context)
                .setTitle(R.string.repeat_on)
                .setMultiChoiceItems(labels, checked, (dialog, which, isChecked) -> {
                    chosen[0] = DaysOfWeek.with(chosen[0], week.get(which), isChecked);
                    // A weekly schedule needs at least one day.
                    ((AlertDialog) dialog).getButton(DialogInterface.BUTTON_POSITIVE)
                            .setEnabled(chosen[0] != DaysOfWeek.NONE);
                })
                .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                    if (chosen[0] == DaysOfWeek.EVERY_DAY) {
                        onPicked.onPicked(triggerAt, Repeat.DAILY, DaysOfWeek.NONE);
                    } else {
                        // The chosen date only says where to start; the first reminder is on the
                        // first chosen day from there.
                        onPicked.onPicked(DaysOfWeek.firstOnOrAfter(chosen[0], triggerAt, zone),
                                Repeat.DAYS_OF_WEEK, chosen[0]);
                    }
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }
}

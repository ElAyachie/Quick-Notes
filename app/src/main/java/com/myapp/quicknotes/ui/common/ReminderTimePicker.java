package com.myapp.quicknotes.ui.common;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.text.format.DateFormat;
import android.widget.Toast;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.myapp.quicknotes.R;
import com.myapp.quicknotes.data.Repeat;

import java.util.Calendar;

// Asks when a time reminder should fire, in three steps: the date, the time of day, and whether
// to repeat. Used both to set a new reminder and to change an existing one.
public final class ReminderTimePicker {
    public interface OnPicked {
        void onPicked(long triggerAt, Repeat repeat);
    }

    private static final Repeat[] REPEAT_CHOICES = {
            Repeat.NONE, Repeat.DAILY, Repeat.WEEKLY, Repeat.MONTHLY, Repeat.YEARLY};

    private ReminderTimePicker() {
    }

    // Each step starts out at the given time and repeat setting. Nothing is reported if the user
    // backs out of a step or picks a moment that has already passed.
    public static void show(Context context, long initialTime, Repeat initialRepeat,
                            OnPicked onPicked) {
        Calendar initial = Calendar.getInstance();
        initial.setTimeInMillis(initialTime);
        DatePickerDialog datePicker = new DatePickerDialog(context,
                (picker, year, month, day) -> new TimePickerDialog(context,
                        (timePicker, hour, minute) -> {
                            Calendar chosen = Calendar.getInstance();
                            chosen.set(year, month, day, hour, minute, 0);
                            chosen.set(Calendar.MILLISECOND, 0);
                            pickRepeat(context, chosen.getTimeInMillis(), initialRepeat, onPicked);
                        },
                        initial.get(Calendar.HOUR_OF_DAY), initial.get(Calendar.MINUTE),
                        DateFormat.is24HourFormat(context)).show(),
                initial.get(Calendar.YEAR), initial.get(Calendar.MONTH),
                initial.get(Calendar.DAY_OF_MONTH));
        datePicker.getDatePicker().setMinDate(System.currentTimeMillis());
        datePicker.show();
    }

    private static void pickRepeat(Context context, long triggerAt, Repeat initialRepeat,
                                   OnPicked onPicked) {
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
                    onPicked.onPicked(triggerAt, REPEAT_CHOICES[which]);
                })
                .show();
    }
}

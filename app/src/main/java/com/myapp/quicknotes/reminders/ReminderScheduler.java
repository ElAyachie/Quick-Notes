package com.myapp.quicknotes.reminders;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import com.myapp.quicknotes.data.Reminder;

// Hands reminders to the system so they fire even when the app isn't running.
public class ReminderScheduler {
    private final Context context;
    private final AlarmManager alarmManager;

    public ReminderScheduler(Context context) {
        this.context = context.getApplicationContext();
        this.alarmManager = context.getSystemService(AlarmManager.class);
    }

    // Scheduling a reminder that is already scheduled replaces its alarm.
    public void schedule(Reminder reminder) {
        PendingIntent alarm = alarmFor(reminder.getId());
        if (canScheduleExactAlarms()) {
            try {
                alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP, reminder.getTriggerAt(), alarm);
                return;
            } catch (SecurityException e) {
                // The permission was revoked between the check and the call.
            }
        }
        // Without the exact-alarm permission the system picks the moment, up to an hour late.
        alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, reminder.getTriggerAt(), alarm);
    }

    public void cancel(long reminderId) {
        alarmManager.cancel(alarmFor(reminderId));
    }

    public boolean canScheduleExactAlarms() {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.S
                || alarmManager.canScheduleExactAlarms();
    }

    private PendingIntent alarmFor(long reminderId) {
        Intent intent = new Intent(context, ReminderAlarmReceiver.class)
                .putExtra(ReminderAlarmReceiver.EXTRA_REMINDER_ID, reminderId);
        // The request code tells one reminder's alarm apart from another's.
        return PendingIntent.getBroadcast(context, (int) reminderId, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }
}

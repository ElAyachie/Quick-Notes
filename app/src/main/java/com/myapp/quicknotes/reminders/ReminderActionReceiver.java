package com.myapp.quicknotes.reminders;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.myapp.quicknotes.AppContainer;
import com.myapp.quicknotes.QuickNotesApp;

import java.util.concurrent.TimeUnit;

// Runs when a button on a reminder's notification is pressed. Every button puts the notification
// away; the snooze buttons also set a new reminder for the same note a little later.
public class ReminderActionReceiver extends BroadcastReceiver {
    // The buttons differ in their action, which also keeps their pending intents apart.
    private static final String ACTION_DONE = "com.myapp.quicknotes.reminders.DONE";
    private static final String ACTION_SNOOZE_SHORT = "com.myapp.quicknotes.reminders.SNOOZE_SHORT";
    private static final String ACTION_SNOOZE_LONG = "com.myapp.quicknotes.reminders.SNOOZE_LONG";
    private static final String EXTRA_REMINDER_ID = "reminderId";
    private static final String EXTRA_NOTE_ID = "noteId";

    // The button labels (snooze_short, snooze_long in strings.xml) state these lengths.
    private static final long SHORT_SNOOZE_MILLIS = TimeUnit.MINUTES.toMillis(10);
    private static final long LONG_SNOOZE_MILLIS = TimeUnit.HOURS.toMillis(1);

    static PendingIntent done(Context context, long reminderId, long noteId) {
        return button(context, ACTION_DONE, reminderId, noteId);
    }

    static PendingIntent snoozeShort(Context context, long reminderId, long noteId) {
        return button(context, ACTION_SNOOZE_SHORT, reminderId, noteId);
    }

    static PendingIntent snoozeLong(Context context, long reminderId, long noteId) {
        return button(context, ACTION_SNOOZE_LONG, reminderId, noteId);
    }

    private static PendingIntent button(Context context, String action, long reminderId,
                                        long noteId) {
        Intent intent = new Intent(context, ReminderActionReceiver.class)
                .setAction(action)
                .putExtra(EXTRA_REMINDER_ID, reminderId)
                .putExtra(EXTRA_NOTE_ID, noteId);
        // The request code tells one reminder's buttons apart from another's.
        return PendingIntent.getBroadcast(context, (int) reminderId, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        long reminderId = intent.getLongExtra(EXTRA_REMINDER_ID, -1);
        long noteId = intent.getLongExtra(EXTRA_NOTE_ID, -1);
        if (reminderId == -1 || noteId == -1) {
            return;
        }
        AppContainer container = QuickNotesApp.container(context);
        container.reminderNotifier().dismiss(reminderId);

        long snoozeMillis;
        if (ACTION_SNOOZE_SHORT.equals(intent.getAction())) {
            snoozeMillis = SHORT_SNOOZE_MILLIS;
        } else if (ACTION_SNOOZE_LONG.equals(intent.getAction())) {
            snoozeMillis = LONG_SNOOZE_MILLIS;
        } else {
            return;
        }
        long triggerAt = System.currentTimeMillis() + snoozeMillis;
        // The database can't be used on the main thread; keep the broadcast alive until it's done.
        PendingResult result = goAsync();
        container.executors().io().execute(() -> {
            try {
                container.reminderRepository().snooze(noteId, triggerAt);
            } finally {
                result.finish();
            }
        });
    }
}

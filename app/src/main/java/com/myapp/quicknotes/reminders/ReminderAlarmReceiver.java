package com.myapp.quicknotes.reminders;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.myapp.quicknotes.AppContainer;
import com.myapp.quicknotes.QuickNotesApp;

// Runs when a time reminder's alarm goes off.
public class ReminderAlarmReceiver extends BroadcastReceiver {
    static final String EXTRA_REMINDER_ID = "reminderId";

    @Override
    public void onReceive(Context context, Intent intent) {
        long reminderId = intent.getLongExtra(EXTRA_REMINDER_ID, -1);
        if (reminderId == -1) {
            return;
        }
        AppContainer container = QuickNotesApp.container(context);
        // The database can't be read on the main thread; keep the broadcast alive until it's done.
        PendingResult result = goAsync();
        container.executors().io().execute(() -> {
            try {
                container.reminderRepository().fire(reminderId);
            } finally {
                result.finish();
            }
        });
    }
}

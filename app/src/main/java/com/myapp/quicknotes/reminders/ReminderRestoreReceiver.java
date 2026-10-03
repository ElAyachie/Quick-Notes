package com.myapp.quicknotes.reminders;

import android.app.AlarmManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.myapp.quicknotes.AppContainer;
import com.myapp.quicknotes.QuickNotesApp;

// The system forgets alarms and watched places on reboot and on app update, and switches between
// exact and inexact alarm delivery when the exact-alarm permission changes. In each case the
// reminders are scheduled again.
public class ReminderRestoreReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (!Intent.ACTION_BOOT_COMPLETED.equals(action)
                && !Intent.ACTION_MY_PACKAGE_REPLACED.equals(action)
                && !AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED.equals(action)) {
            return;
        }
        AppContainer container = QuickNotesApp.container(context);
        PendingResult result = goAsync();
        container.executors().io().execute(() -> {
            try {
                container.reminderRepository().rescheduleAll();
            } finally {
                result.finish();
            }
        });
    }
}

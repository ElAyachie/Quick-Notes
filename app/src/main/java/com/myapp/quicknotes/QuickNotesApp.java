package com.myapp.quicknotes;

import android.app.Application;
import android.content.Context;

import com.myapp.quicknotes.ui.settings.ThemeSettings;

public class QuickNotesApp extends Application {
    private AppContainer container;

    @Override
    public void onCreate() {
        super.onCreate();
        container = new AppContainer(this);
        ThemeSettings.apply(this);
        container.reminderNotifier().createChannel();
        // Covers reminders dropped while the app was force-stopped, restored from a backup, or
        // (for places) while location was switched off.
        container.executors().io().execute(container.reminderRepository()::rescheduleAll);
    }

    public static AppContainer container(Context context) {
        return ((QuickNotesApp) context.getApplicationContext()).container;
    }
}

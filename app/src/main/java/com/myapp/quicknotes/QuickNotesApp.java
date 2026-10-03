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
        // Covers alarms dropped while the app was force-stopped or restored from a backup.
        container.executors().io().execute(container.reminderRepository()::rescheduleAll);
    }

    public static AppContainer container(Context context) {
        return ((QuickNotesApp) context.getApplicationContext()).container;
    }
}

package com.myapp.quicknotes.reminders;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofenceStatusCodes;
import com.google.android.gms.location.GeofencingEvent;
import com.myapp.quicknotes.AppContainer;
import com.myapp.quicknotes.QuickNotesApp;

import java.util.List;

// Runs when the device arrives at, or leaves, the place of one or more location reminders.
public class GeofenceReceiver extends BroadcastReceiver {
    private static final String TAG = "GeofenceReceiver";

    // A geofence is registered under the id of its reminder.
    static String geofenceId(long reminderId) {
        return String.valueOf(reminderId);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        GeofencingEvent event = GeofencingEvent.fromIntent(intent);
        if (event == null) {
            return;
        }
        if (event.hasError()) {
            // Typically location was switched off, which also drops every watched place. They are
            // registered again the next time the app starts.
            Log.w(TAG, "Geofence error: "
                    + GeofenceStatusCodes.getStatusCodeString(event.getErrorCode()));
            return;
        }
        // Each place is watched only for the crossing its reminder waits for (see
        // ReminderScheduler), so every geofence reported here is a reminder that has come due.
        List<Geofence> crossed = event.getTriggeringGeofences();
        int transition = event.getGeofenceTransition();
        if (crossed == null || (transition != Geofence.GEOFENCE_TRANSITION_ENTER
                && transition != Geofence.GEOFENCE_TRANSITION_EXIT)) {
            return;
        }
        AppContainer container = QuickNotesApp.container(context);
        PendingResult result = goAsync();
        container.executors().io().execute(() -> {
            try {
                for (Geofence geofence : crossed) {
                    container.reminderRepository().fire(Long.parseLong(geofence.getRequestId()));
                }
            } finally {
                result.finish();
            }
        });
    }
}

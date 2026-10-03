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

// Runs when the device arrives at the place of one or more location reminders.
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
        List<Geofence> entered = event.getTriggeringGeofences();
        if (event.getGeofenceTransition() != Geofence.GEOFENCE_TRANSITION_ENTER || entered == null) {
            return;
        }
        AppContainer container = QuickNotesApp.container(context);
        PendingResult result = goAsync();
        container.executors().io().execute(() -> {
            try {
                for (Geofence geofence : entered) {
                    container.reminderRepository().fire(Long.parseLong(geofence.getRequestId()));
                }
            } finally {
                result.finish();
            }
        });
    }
}

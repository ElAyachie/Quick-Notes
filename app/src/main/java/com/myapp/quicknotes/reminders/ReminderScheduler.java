package com.myapp.quicknotes.reminders;

import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import androidx.annotation.WorkerThread;

import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingClient;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.myapp.quicknotes.data.Reminder;
import com.myapp.quicknotes.data.ReminderType;

import java.util.Collections;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

// Hands reminders to the system so they fire even when the app isn't running: a time reminder
// becomes an alarm, a location reminder becomes a geofence watched by Google Play services.
//
// Call from a background thread. Talking to Play services is asynchronous, and these methods wait
// for its answer: a caller running inside a broadcast (a reminder firing, the device booting)
// could otherwise finish, and the app be frozen, before the place is actually being watched.
public class ReminderScheduler {
    private static final String TAG = "ReminderScheduler";
    private static final long PLAY_SERVICES_TIMEOUT_SECONDS = 10;

    private final Context context;
    private final AlarmManager alarmManager;
    private final GeofencingClient geofencingClient;

    public ReminderScheduler(Context context) {
        this.context = context.getApplicationContext();
        this.alarmManager = context.getSystemService(AlarmManager.class);
        this.geofencingClient = LocationServices.getGeofencingClient(this.context);
    }

    // Scheduling a reminder that is already scheduled replaces it.
    @WorkerThread
    public void schedule(Reminder reminder) {
        if (reminder.getType() == ReminderType.LOCATION) {
            watchPlace(reminder);
        } else {
            setAlarm(reminder);
        }
    }

    @WorkerThread
    public void cancel(Reminder reminder) {
        if (reminder.getType() == ReminderType.LOCATION) {
            waitFor(geofencingClient.removeGeofences(
                            Collections.singletonList(GeofenceReceiver.geofenceId(reminder.getId()))),
                    "stop watching", reminder);
        } else {
            alarmManager.cancel(alarmFor(reminder.getId()));
        }
    }

    public boolean canScheduleExactAlarms() {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.S
                || alarmManager.canScheduleExactAlarms();
    }

    private void setAlarm(Reminder reminder) {
        long triggerAt = Objects.requireNonNull(reminder.getTriggerAt());
        PendingIntent alarm = alarmFor(reminder.getId());
        if (canScheduleExactAlarms()) {
            try {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, alarm);
                return;
            } catch (SecurityException e) {
                // The permission was revoked between the check and the call.
            }
        }
        // Without the exact-alarm permission the system picks the moment, up to an hour late.
        alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, alarm);
    }

    private PendingIntent alarmFor(long reminderId) {
        Intent intent = new Intent(context, ReminderAlarmReceiver.class)
                .putExtra(ReminderAlarmReceiver.EXTRA_REMINDER_ID, reminderId);
        // The request code tells one reminder's alarm apart from another's.
        return PendingIntent.getBroadcast(context, (int) reminderId, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    // The reminder fires on arriving: on crossing into the circle from outside. Being inside it
    // already when the reminder is set does not count, so "remind me when I get home", set at
    // home, waits for the next time.
    //
    // Without the location permissions the place can't be watched. The reminder stays stored and
    // is tried again the next time the app starts.
    @SuppressLint("MissingPermission")
    private void watchPlace(Reminder reminder) {
        if (!LocationAccess.canWatchPlaces(context)) {
            Log.w(TAG, "No location permission; not watching the place of reminder "
                    + reminder.getId());
            return;
        }
        Geofence geofence = new Geofence.Builder()
                .setRequestId(GeofenceReceiver.geofenceId(reminder.getId()))
                .setCircularRegion(
                        Objects.requireNonNull(reminder.getLatitude()),
                        Objects.requireNonNull(reminder.getLongitude()),
                        Objects.requireNonNull(reminder.getRadiusMeters()))
                .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER)
                .setExpirationDuration(Geofence.NEVER_EXPIRE)
                .build();
        GeofencingRequest request = new GeofencingRequest.Builder()
                .setInitialTrigger(0)
                .addGeofence(geofence)
                .build();
        // This fails when location is switched off on the device, among other reasons.
        waitFor(geofencingClient.addGeofences(request, geofenceIntent()), "watch", reminder);
    }

    private void waitFor(Task<Void> request, String action, Reminder reminder) {
        try {
            Tasks.await(request, PLAY_SERVICES_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (ExecutionException | TimeoutException e) {
            Log.w(TAG, "Could not " + action + " the place of reminder " + reminder.getId(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    // One intent serves every geofence; the event it delivers says which ones were entered.
    // Play services writes that event into the intent, so the intent has to be mutable.
    private PendingIntent geofenceIntent() {
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            flags |= PendingIntent.FLAG_MUTABLE;
        }
        return PendingIntent.getBroadcast(context, 0,
                new Intent(context, GeofenceReceiver.class), flags);
    }
}

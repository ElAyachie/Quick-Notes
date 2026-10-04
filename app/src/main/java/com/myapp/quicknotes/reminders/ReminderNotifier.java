package com.myapp.quicknotes.reminders;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import androidx.navigation.NavDeepLinkBuilder;

import com.myapp.quicknotes.R;
import com.myapp.quicknotes.data.Note;
import com.myapp.quicknotes.ui.MainActivity;
import com.myapp.quicknotes.ui.editor.NoteEditorViewModel;

// Shows the notification for a reminder that has come due.
public class ReminderNotifier {
    private static final String CHANNEL_ID = "note_reminders";

    private final Context context;

    public ReminderNotifier(Context context) {
        this.context = context.getApplicationContext();
    }

    public void createChannel() {
        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.reminder_channel_name),
                NotificationManager.IMPORTANCE_HIGH);
        channel.enableVibration(true);
        context.getSystemService(NotificationManager.class).createNotificationChannel(channel);
    }

    public boolean canNotify() {
        boolean permitted = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU
                || ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED;
        return permitted && NotificationManagerCompat.from(context).areNotificationsEnabled();
    }

    // Tapping the notification opens the note. Each reminder gets its own notification.
    public void show(long reminderId, Note note) {
        if (!canNotify()) {
            return;
        }
        Bundle editorArgs = new Bundle();
        editorArgs.putLong(NoteEditorViewModel.ARG_NOTE_ID, note.getId());
        editorArgs.putLong(NoteEditorViewModel.ARG_FOLDER_ID, note.getFolderId());
        PendingIntent openNote = new NavDeepLinkBuilder(context)
                .setComponentName(MainActivity.class)
                .setGraph(R.navigation.nav_graph)
                .setDestination(R.id.noteEditorFragment)
                .setArguments(editorArgs)
                .createPendingIntent();

        NotificationCompat.Builder notification = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(context.getString(R.string.reminder_notification_title, note.getTitle()))
                .setContentText(note.getContent())
                .setStyle(new NotificationCompat.BigTextStyle().bigText(note.getContent()))
                .setCategory(NotificationCompat.CATEGORY_REMINDER)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(openNote)
                .setAutoCancel(true);
        try {
            NotificationManagerCompat.from(context).notify((int) reminderId, notification.build());
        } catch (SecurityException e) {
            // The notification permission was revoked between the check and the call.
        }
    }
}

package QuickNotes.Services;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Build;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import QuickNotes.HomePage;
import QuickNotes.Note;
import QuickNotes.R;
import QuickNotes.Reminder;
import QuickNotes.ReminderFileOperations;
import QuickNotes.RemindersPage;

public class ReminderBroadcast extends BroadcastReceiver {
    SharedPreferences pref;
    NotificationManager notificationManager;
    int notificationIDCounter;

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d("Intent action: ", "" + intent.getAction());
        pref = context.getSharedPreferences("MyPref", 0);
        notificationIDCounter = pref.getInt("notificationID", 0);
        final int NOTIFY_ID = notificationIDCounter - 1;// ID of notification
        String id = "Note Reminder"; // default_channel_id
        String title = "title"; // Default Channel
        Reminder reminder = (Reminder) intent.getSerializableExtra("reminder");
        assert reminder != null;
        PendingIntent pendingIntent = null;
        NotificationCompat.Builder builder;
        if (notificationManager == null) {
            notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel mChannel = notificationManager.getNotificationChannel(id);
            if (mChannel == null) {
                mChannel = new NotificationChannel(id, title, importance);
                mChannel.enableVibration(true);
                mChannel.setVibrationPattern(new long[]{Notification.DEFAULT_VIBRATE});
                notificationManager.createNotificationChannel(mChannel);
            }
            builder = new NotificationCompat.Builder(context, id);
            intent = new Intent(context, HomePage.class);

            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE);
            builder.setContentTitle("Reminder: " + reminder.getNoteName())
                    .setSmallIcon(R.drawable.outline_notifications_black)
                    .setContentText(reminder.getNoteContent())
                    .setDefaults(Notification.DEFAULT_ALL)
                    .setAutoCancel(true)
                    .setContentIntent(pendingIntent)
                    .setTicker(reminder.getNoteName())
                    .setDefaults(Notification.DEFAULT_VIBRATE)
                    .setLights(Color.BLUE, 3000, 3000)
                    .setStyle(new NotificationCompat.BigTextStyle()
                            .bigText(reminder.getNoteContent()));
        } else {
            builder = new NotificationCompat.Builder(context, id);
            intent = new Intent(context, HomePage.class);

            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE);
            }
            builder.setContentTitle("Reminder: " + reminder.getNoteName())
                    .setSmallIcon(R.drawable.outline_notifications_black)
                    .setContentText(reminder.getNoteContent())
                    .setDefaults(Notification.DEFAULT_ALL)
                    .setAutoCancel(true)
                    .setContentIntent(pendingIntent)
                    .setTicker(reminder.getNoteName())
                    .setDefaults(Notification.DEFAULT_VIBRATE)
                    .setLights(Color.BLUE, 3000, 3000)
                    .setPriority(Notification.PRIORITY_HIGH)
                    .setStyle(new NotificationCompat.BigTextStyle()
                            .bigText(reminder.getNoteContent()));
        }
        Notification notification = builder.build();
        notificationManager.notify(NOTIFY_ID, notification);
        ReminderFileOperations.deleteReminder(context, reminder);
    }
}

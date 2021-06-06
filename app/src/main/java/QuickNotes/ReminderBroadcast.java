package QuickNotes;

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
import androidx.core.app.NotificationCompat;


// idk what im doing because i want to make this have specific title and content for this to show as a notification.
public class ReminderBroadcast extends BroadcastReceiver {
    SharedPreferences pref;
    NotificationManager notificationManager;
    int notificationIDCounter;
    SharedPreferences.Editor editor;
    String noteName;
    String noteContent;
    String currentFolder;
    String noteDate;

    @Override
    public void onReceive(Context context, Intent intent) {
        pref = context.getSharedPreferences("MyPref", 0);
        notificationIDCounter = pref.getInt("notificationID", 0);
        final int NOTIFY_ID =  notificationIDCounter - 1;// ID of notification
        String id = "Note Reminder"; // default_channel_id
        String title = "title"; // Default Channel
        currentFolder = pref.getString("Note folder", "Default");
        noteName = pref.getString("Note name", "Default");
        noteDate = pref.getString("Note date", "Default");
        noteContent = pref.getString("Note content", "Default");
        PendingIntent pendingIntent;
        NotificationCompat.Builder builder;
        if (notificationManager == null) {
            notificationManager = (NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);
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
            pendingIntent = PendingIntent.getActivity(context, 0, intent, 0);
            builder.setContentTitle("Reminder")
                    .setSmallIcon(R.drawable.outline_notifications_black)
                    .setContentText(noteName)
                    .setDefaults(Notification.DEFAULT_ALL)
                    .setAutoCancel(true)
                    .setContentIntent(pendingIntent)
                    .setTicker(noteName)
                    .setDefaults(Notification.DEFAULT_VIBRATE)
                    .setLights(Color.BLUE, 3000, 3000)
                    .setStyle(new NotificationCompat.BigTextStyle()
                            .bigText(noteContent));
        }
        else {
            builder = new NotificationCompat.Builder(context, id);
            intent = new Intent(context, HomePage.class);

            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            pendingIntent = PendingIntent.getActivity(context, 0, intent, 0);
            builder.setContentTitle("Reminder")
                    .setSmallIcon(R.drawable.outline_notifications_black)
                    .setContentText(noteName)
                    .setDefaults(Notification.DEFAULT_ALL)
                    .setAutoCancel(true)
                    .setContentIntent(pendingIntent)
                    .setTicker(noteName)
                    .setDefaults(Notification.DEFAULT_VIBRATE)
                    .setLights(Color.BLUE, 3000, 3000)
                    .setPriority(Notification.PRIORITY_HIGH)
                    .setStyle(new NotificationCompat.BigTextStyle()
                            .bigText(noteContent));
        }
        Notification notification = builder.build();
        notificationManager.notify(NOTIFY_ID, notification);
        Reminder.deleteReminder(context, noteName, noteDate);
        new RemindersPage().refreshReminderListView(context);
    }
}

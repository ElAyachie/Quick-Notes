package QuickNotes.Dialogs;

import static android.content.Context.ALARM_SERVICE;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.view.View;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.TimePicker;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import java.util.Calendar;
import java.util.GregorianCalendar;

import QuickNotes.R;
import QuickNotes.Reminder;
import QuickNotes.Services.ReminderBroadcast;

public class DateTimePickerDialog extends AlertDialog.Builder {
    AlertDialog optionDialog;

    public DateTimePickerDialog(Context context, String currentFolder, String currentNoteName, String currentNoteContent) {
        super(context);
        View dialogView = View.inflate(context, R.layout.date_time_picker, null);
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        DatePicker datePicker = dialogView.findViewById(R.id.date_picker);
        TimePicker timePicker = dialogView.findViewById(R.id.time_picker);
        Button cancelButton = dialogView.findViewById(R.id.cancelButton);
        datePicker.setMinDate(System.currentTimeMillis() - 1000);
        builder.setView(dialogView);
        optionDialog = builder.create();
        dialogView.findViewById(R.id.date_time_set).setOnClickListener(view -> {
            Calendar calendar = new GregorianCalendar(datePicker.getYear(),
                    datePicker.getMonth(),
                    datePicker.getDayOfMonth(),
                    timePicker.getCurrentHour(),
                    timePicker.getCurrentMinute());
            String dateTime = calendar.getTime().toString();
            String formattedReminderDateTime = (dateTime.substring(0, 16) + dateTime.substring(19, 28)).replaceAll(" ", "");
            SharedPreferences pref = context.getSharedPreferences("MyPref", 0);
            SharedPreferences.Editor editor = pref.edit();
            int notificationID = pref.getInt("notificationID", 0);
            editor.putInt("notificationID", notificationID + 1);
            editor.putString("Note folder", currentFolder);
            editor.putString("Note name", currentNoteName);
            editor.putString("Note content", currentNoteContent);
            editor.putString("Note date", formattedReminderDateTime);
            notificationID = pref.getInt("notificationID", 0);
            editor.apply();
            Reminder.saveReminder(context, currentNoteName, currentNoteContent, formattedReminderDateTime, String.valueOf(notificationID));
            Intent intent1 = new Intent(context, ReminderBroadcast.class);
            PendingIntent pendingIntent = null;
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                pendingIntent = PendingIntent.getBroadcast(context, notificationID, intent1, PendingIntent.FLAG_IMMUTABLE);
            }
            AlarmManager alarmManager = (AlarmManager) context.getSystemService(ALARM_SERVICE);
            alarmManager.set(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingIntent);
            Toast.makeText(context, "Reminder set.", Toast.LENGTH_LONG).show();
            optionDialog.dismiss();
        });
        cancelButton.setOnClickListener(view -> optionDialog.dismiss());
        builder.setView(dialogView);
        optionDialog = builder.create();
    }

    public AlertDialog show() {
        optionDialog.show();
        return null;
    }
}

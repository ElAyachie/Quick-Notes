package QuickNotes.Dialogs;

import static android.content.Context.ALARM_SERVICE;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Parcelable;
import android.view.View;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.TimePicker;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import java.util.Calendar;
import java.util.GregorianCalendar;

import QuickNotes.Note;
import QuickNotes.R;
import QuickNotes.Reminder;
import QuickNotes.ReminderFileOperations;
import QuickNotes.Services.ReminderBroadcast;

// Dialog that allows the user to choose a date and time to trigger a reminder.
// This dialog is used in the specific note edit.
public class DateTimePickerDialog extends AlertDialog.Builder {
    AlertDialog optionDialog;

    public DateTimePickerDialog(Context context, Note note) {
        super(context);
        View dialogView = View.inflate(context, R.layout.date_time_picker, null);
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        DatePicker datePicker = dialogView.findViewById(R.id.date_picker);
        TimePicker timePicker = dialogView.findViewById(R.id.time_picker);
        Button cancelButton = dialogView.findViewById(R.id.cancelButton);
        datePicker.setMinDate(System.currentTimeMillis() - 1000);
        builder.setView(dialogView);
        optionDialog = builder.create();
        dialogView.findViewById(R.id.setLocationButton).setOnClickListener(view -> {
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
            editor.apply();
            String type = "Time";
            Reminder reminder = new Reminder(type, note.getFolderName(), note.getNoteName(), note.getNoteContent(), notificationID);
            reminder.setReminderDate(formattedReminderDateTime);
            ReminderFileOperations.saveReminder(context, reminder);
            Intent intent = new Intent(context, ReminderBroadcast.class);
            intent.putExtra("reminder", reminder);
            PendingIntent pendingIntent = null;
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                pendingIntent = PendingIntent.getBroadcast(context, notificationID, intent, PendingIntent.FLAG_IMMUTABLE);
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

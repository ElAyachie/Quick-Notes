package QuickNotes.Dialogs;

import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import org.w3c.dom.Text;

import QuickNotes.R;
import QuickNotes.Reminder;
import QuickNotes.ReminderFileOperations;
import QuickNotes.Services.ReminderBroadcast;
import QuickNotes.SpecificNoteEdit;

// Dialog asks the user to confirm deleting a reminder they set.
// This dialog is used in the reminders page.
public class DeleteReminderDialog extends AlertDialog.Builder {
    AlertDialog optionDialog;

    public DeleteReminderDialog(Context context, Reminder reminder) {
        super(context);
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        View alertView = View.inflate(context, R.layout.alertdialog_delete_reminder, null);
        Button positiveButton = alertView.findViewById(R.id.positiveButton);
        Button cancelButton = alertView.findViewById(R.id.cancelButton);
        positiveButton.setOnClickListener(view -> {
            ReminderFileOperations.deleteReminder(context, reminder);
            Intent intent = new Intent(context, ReminderBroadcast.class);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                PendingIntent pendingIntent = PendingIntent.getActivity(context, reminder.getNotificationID(), intent, PendingIntent.FLAG_IMMUTABLE);
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                new SpecificNoteEdit().cancelReminder(context, String.valueOf(reminder.getNotificationID()));
            }
            Toast.makeText(context, "Reminder deleted.", Toast.LENGTH_LONG).show();
            optionDialog.dismiss();
        });

        cancelButton.setOnClickListener(view -> optionDialog.dismiss());
        builder.setView(alertView);
        optionDialog = builder.create();
    }

    public AlertDialog show() {
        optionDialog.show();
        return optionDialog;
    }
}

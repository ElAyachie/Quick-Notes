package QuickNotes.Dialogs;

import android.app.AlertDialog;
import android.content.Context;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import QuickNotes.Note;
import QuickNotes.R;
import QuickNotes.Tab2Fragment;

public class CheckNoteDialog extends AlertDialog.Builder {
    AlertDialog optionDialog;

    public CheckNoteDialog(Context context, String folderName, String noteNameString, String noteString) {
        super(context);
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        View alertView = View.inflate(context, R.layout.alertdialog_overwrite_note, null);
        Button positiveButton = alertView.findViewById(R.id.positiveButton);
        Button cancelButton = alertView.findViewById(R.id.cancelButton);
        builder.setView(alertView);
        positiveButton.setOnClickListener(view -> {
            Date dateCreated = Calendar.getInstance().getTime();
            SimpleDateFormat formatter = new SimpleDateFormat("MM/dd/yyyy", Locale.US);
            String dateString = formatter.format(dateCreated);
            Note.saveNote(context, folderName, noteNameString, noteString, dateString);
            Toast.makeText(getContext(), "Note overwritten.", Toast.LENGTH_LONG).show();
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

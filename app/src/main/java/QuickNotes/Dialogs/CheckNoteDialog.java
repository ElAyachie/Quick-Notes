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
import QuickNotes.NoteFileOperations;
import QuickNotes.R;
import QuickNotes.Tab2Fragment;

// Dialog that allows the user check whether the user wants to overwrite an already existing note with the same name.
// This dialog is used in the specific note edit and fragment 2 of the home page.
public class CheckNoteDialog extends AlertDialog.Builder {
    AlertDialog optionDialog;

    public CheckNoteDialog(Context context, Note note) {
        super(context);
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        View alertView = View.inflate(context, R.layout.alertdialog_overwrite_note, null);
        Button positiveButton = alertView.findViewById(R.id.positiveButton);
        Button cancelButton = alertView.findViewById(R.id.cancelButton);
        builder.setView(alertView);
        positiveButton.setOnClickListener(view -> {
            NoteFileOperations.saveNote(context, note);
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

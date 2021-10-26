package QuickNotes.Dialogs;

import android.app.AlertDialog;
import android.content.Context;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import QuickNotes.Note;
import QuickNotes.NoteFileOperations;
import QuickNotes.R;

// Dialog asks the user to confirm overwriting a note that is already saved.
// This dialog is used in the specific notes edit page.
public class SaveNoteDialog extends AlertDialog.Builder {
    AlertDialog optionDialog;

    public SaveNoteDialog(Context context, Note oldNote, Note newNote) {
        super(context);
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        View alertView = View.inflate(context, R.layout.alertdialog_save_note, null);
        Button positiveButton = alertView.findViewById(R.id.positiveButton);
        Button cancelButton = alertView.findViewById(R.id.cancelButton);
        positiveButton.setOnClickListener(view -> {
            NoteFileOperations.deleteNote(context, oldNote);
            NoteFileOperations.saveNote(context, newNote);
            Toast.makeText(context, "Note saved.", Toast.LENGTH_LONG).show();
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

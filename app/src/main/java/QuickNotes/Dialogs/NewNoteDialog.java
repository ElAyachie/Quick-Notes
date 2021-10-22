package QuickNotes.Dialogs;

import android.content.Context;
import android.app.AlertDialog;
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
import QuickNotes.NotesInFolderPage;
import QuickNotes.R;

public class NewNoteDialog extends AlertDialog.Builder {
    AlertDialog optionDialog;

    public NewNoteDialog(Context context, String currentFolder) {
        super(context);
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        View alertView = View.inflate(context, R.layout.alertdialog_new_note, null);
        Button positiveButton = alertView.findViewById(R.id.positiveButton);
        Button cancelButton = alertView.findViewById(R.id.cancelButton);
        EditText noteNameText = alertView.findViewById(R.id.noteNameText);
        noteNameText.requestFocus();
        // Force a keyboard for the edit text.
        InputMethodManager inputMethodManager = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
        inputMethodManager.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);
        EditText noteContentText = alertView.findViewById(R.id.reminderNameText);
        positiveButton.setOnClickListener(view -> {
            String noteNameString = noteNameText.getText().toString();
            String noteContentString = noteContentText.getText().toString();
            if (noteNameString.equals("")) {
                noteNameText.setError("Please enter a name.");
            }
            else if (NotesInFolderPage.allNoteNames.contains(noteNameString)){
                noteNameText.setError("This note already exists.");
            }
            else {
                Date dateCreated = Calendar.getInstance().getTime();
                SimpleDateFormat formatter = new SimpleDateFormat("MM/dd/yyyy", Locale.US);
                String dateString = formatter.format(dateCreated);
                Note.saveNote(context, currentFolder, noteNameString, noteContentString, dateString);
                NotesInFolderPage.allNoteNames.add(0, noteNameString);
                NotesInFolderPage.allNotesContent.add(0, noteContentString);
                NotesInFolderPage.allNoteDates.add(0, dateString);
                optionDialog.dismiss();
                Toast.makeText(context, "Note saved.", Toast.LENGTH_LONG).show();
            }
        });
        cancelButton.setOnClickListener(view -> optionDialog.dismiss());
        builder.setView(alertView);
        optionDialog = builder.create();
    }

    public AlertDialog show() {
        optionDialog.show();
        return null;
    }
}

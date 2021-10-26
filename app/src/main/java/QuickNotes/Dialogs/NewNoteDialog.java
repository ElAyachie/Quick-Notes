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
import QuickNotes.NoteFileOperations;
import QuickNotes.NotesInFolderPage;
import QuickNotes.R;

// Dialog allows the user to create a note (user can enter a note name and content) in the folder they are in.
// This dialog is used in the notes in folder page.
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
            String noteName = noteNameText.getText().toString();
            String noteContent = noteContentText.getText().toString();
            Date dateCreated = Calendar.getInstance().getTime();
            SimpleDateFormat formatter = new SimpleDateFormat("MM/dd/yyyy", Locale.US);
            String dateString = formatter.format(dateCreated);
            if (noteName.equals("")) {
                noteNameText.setError("Please enter a name.");
            } else if (!checkIfValid(noteName)) {
                noteNameText.setError("This note already exists.");
            } else {
                Note note = new Note(noteName, noteContent, currentFolder, dateString);
                NoteFileOperations.saveNote(context, note);
                Toast.makeText(context, "Note saved.", Toast.LENGTH_LONG).show();
                optionDialog.dismiss();
            }
        });
        cancelButton.setOnClickListener(view -> optionDialog.dismiss());
        builder.setView(alertView);
        optionDialog = builder.create();
    }

    public AlertDialog show() {
        optionDialog.show();
        return optionDialog;
    }

    private static boolean checkIfValid(String noteName) {
        // Check if a note in the allNotes list already has the same name.
        boolean valid = true;
        for (Note n : NotesInFolderPage.allNotes) {
            if (n.getNoteName().equals(noteName)) {
                valid = false;
                break;
            }
        }
        return valid;
    }
}

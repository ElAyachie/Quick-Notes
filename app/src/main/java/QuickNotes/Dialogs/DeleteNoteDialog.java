package QuickNotes.Dialogs;

import android.app.AlertDialog;
import android.content.Context;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import java.util.ArrayList;

import QuickNotes.Note;
import QuickNotes.NoteFileOperations;
import QuickNotes.NotesInFolderPage;
import QuickNotes.R;

// Dialog asks the user to confirm deleting the note they selected.
// This dialog is used in the notes in folder page.
public class DeleteNoteDialog extends AlertDialog.Builder {
    AlertDialog optionDialog;

    public DeleteNoteDialog(Context context, Note note) {
        super(context);
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        View alertView = View.inflate(context, R.layout.alertdialog_delete_note, null);
        Button positiveButton = alertView.findViewById(R.id.positiveButton);
        Button cancelButton = alertView.findViewById(R.id.cancelButton);
        builder.setView(alertView);
        optionDialog = builder.create();
        positiveButton.setOnClickListener(view -> {
            NoteFileOperations.deleteNote(context, note);
            Toast.makeText(context, "Note deleted.", Toast.LENGTH_LONG).show();
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

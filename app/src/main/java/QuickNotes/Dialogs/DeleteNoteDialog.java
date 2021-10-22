package QuickNotes.Dialogs;

import android.app.AlertDialog;
import android.content.Context;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import QuickNotes.Note;
import QuickNotes.NotesInFolderPage;
import QuickNotes.R;

public class DeleteNoteDialog extends AlertDialog.Builder {
    AlertDialog optionDialog;

    public DeleteNoteDialog(Context context, String currentFolder, String noteName) {
        super(context);
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        View alertView = View.inflate(context, R.layout.alertdialog_delete_note, null);
        Button positiveButton = alertView.findViewById(R.id.positiveButton);
        Button cancelButton = alertView.findViewById(R.id.cancelButton);
        builder.setView(alertView);
        optionDialog = builder.create();
        positiveButton.setOnClickListener(view -> {
            Note.deleteNote(context, noteName, currentFolder);
            NotesInFolderPage.allNoteNames.remove(noteName);
            NotesInFolderPage.allNotesContent.remove(Note.loadNote(context, noteName, currentFolder));
            NotesInFolderPage.allNoteDates.remove(Note.loadDate(context, noteName, currentFolder));
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

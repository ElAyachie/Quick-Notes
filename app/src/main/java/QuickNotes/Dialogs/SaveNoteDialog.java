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
import QuickNotes.R;

public class SaveNoteDialog extends AlertDialog.Builder {
    AlertDialog optionDialog;

    public SaveNoteDialog(Context context, String folderNameString, String currentNoteName, String currentFolder, String noteNameString, String noteContentString) {
        super(context);
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        View alertView = View.inflate(context, R.layout.alertdialog_save_note, null);
        Button positiveButton = alertView.findViewById(R.id.positiveButton);
        Button cancelButton = alertView.findViewById(R.id.cancelButton);
        positiveButton.setOnClickListener(view -> {
            Date dateEdited = Calendar.getInstance().getTime();
            Note.deleteNote(context, currentNoteName, currentFolder);
            SimpleDateFormat formatter = new SimpleDateFormat("MM/dd/yyyy", Locale.US);
            String dateString = formatter.format(dateEdited);
            Note.saveNote(context, folderNameString, noteNameString, noteContentString, dateString);
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

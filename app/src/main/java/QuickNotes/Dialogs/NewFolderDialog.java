package QuickNotes.Dialogs;

import android.app.AlertDialog;
import android.content.Context;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import QuickNotes.NoteFileOperations;
import QuickNotes.R;
import QuickNotes.Tab1HomeFragment;
import QuickNotes.Tab2HomeFragment;

// Dialog allows the user to create a new folder.
// This dialog is used in fragment 1 of the home page.
public class NewFolderDialog extends AlertDialog.Builder {
    AlertDialog optionDialog;

    public NewFolderDialog(Context context) {
        super(context);
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        View alertView = View.inflate(context, R.layout.alertdialog_new_folder, null);
        EditText folderName = alertView.findViewById(R.id.folderName);
        folderName.requestFocus();
        // Force a keyboard for the edit text.
        InputMethodManager inputMethodManager = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
        inputMethodManager.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);
        Button positiveButton = alertView.findViewById(R.id.positiveButton);
        Button cancelButton = alertView.findViewById(R.id.cancelButton);
        positiveButton.setOnClickListener(view -> {
            String folderNameString = folderName.getText().toString();
            if (!folderNameString.equals("")) {
                //creating a folder to store notes
                if (Tab1HomeFragment.folderNamesList.contains(folderNameString)) {
                    folderName.setError("Folder already exists.");
                } else {
                    NoteFileOperations.saveFolderName(context, folderNameString);
                    Tab1HomeFragment.folderNamesList.add(folderNameString);
                    Tab1HomeFragment.folderNamesListAdapter.notifyDataSetChanged();
                    new Tab2HomeFragment().refreshFolderSpinner(folderNameString, true);
                    Toast.makeText(context, "Folder made.", Toast.LENGTH_LONG).show();
                    optionDialog.dismiss();
                }
            } else {
                folderName.setError("Enter name.");
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

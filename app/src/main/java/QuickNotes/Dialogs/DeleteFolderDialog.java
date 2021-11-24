package QuickNotes.Dialogs;

import android.app.AlertDialog;
import android.content.Context;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import QuickNotes.NoteFileOperations;
import QuickNotes.R;
import QuickNotes.Tab1HomeFragment;
import QuickNotes.Tab2HomeFragment;

// Dialog that confirms whether the user is sure they want to delete a folder.
// This dialog is used in fragment 1 of the home page.
public class DeleteFolderDialog extends AlertDialog.Builder {
    AlertDialog optionDialog;

    public DeleteFolderDialog(Context context, String folderNameString) {
        super(context);
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        View alertView = View.inflate(context, R.layout.alertdialog_delete_folder, null);
        Button positiveButton = alertView.findViewById(R.id.positiveButton);
        Button cancelButton = alertView.findViewById(R.id.cancelButton);
        positiveButton.setOnClickListener(view -> {
            NoteFileOperations.deleteFolder(context, folderNameString);
            Tab1HomeFragment.folderNamesList.remove(folderNameString);
            Tab1HomeFragment.folderNamesListAdapter.notifyDataSetChanged();
            new Tab2HomeFragment().refreshFolderSpinner(folderNameString, false);
            Toast.makeText(context, "Folder deleted.", Toast.LENGTH_LONG).show();
            optionDialog.dismiss();
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

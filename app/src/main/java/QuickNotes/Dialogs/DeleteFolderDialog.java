package QuickNotes.Dialogs;

import android.app.AlertDialog;
import android.content.Context;
import android.view.View;
import android.widget.Button;

import QuickNotes.Note;
import QuickNotes.R;
import QuickNotes.Tab1Fragment;
import QuickNotes.Tab2Fragment;

public class DeleteFolderDialog extends AlertDialog.Builder {
    AlertDialog optionDialog;

    public DeleteFolderDialog(Context context, String folderNameString) {
        super(context);
            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            View alertView = View.inflate(context, R.layout.alertdialog_delete_folder, null);
            Button positiveButton = alertView.findViewById(R.id.positiveButton);
            Button cancelButton = alertView.findViewById(R.id.cancelButton);
            positiveButton.setOnClickListener(view -> {
                Note.deleteFolder(context, folderNameString);
                Tab1Fragment.folderNamesList.remove(folderNameString);
                Tab1Fragment.folderNamesListAdapter.notifyDataSetChanged();
                new Tab2Fragment().refreshFolderSpinner(folderNameString, false);
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

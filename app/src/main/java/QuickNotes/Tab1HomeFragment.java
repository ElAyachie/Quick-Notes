package QuickNotes;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import java.util.ArrayList;

import QuickNotes.Dialogs.DeleteFolderDialog;
import QuickNotes.Dialogs.NewFolderDialog;

public class Tab1HomeFragment extends Fragment {
    ListView folderNamesListView;
    TextView folderNameView;
    Button newFolderBtn;
    String filePath;
    Context context;
    View mainView;
    Activity activity;
    public static ArrayList<String> folderNamesList;
    Spinner folderSpin;
    public static ArrayAdapter<String> folderNamesListAdapter;
    boolean folderIntentIsClicked;

    @Override
    public View onCreateView(LayoutInflater inflater, final ViewGroup container, Bundle savedInstanceState) {
        mainView = inflater.inflate(R.layout.tab1_fragment, container, false);
        context = getContext();
        activity = getActivity();
        folderNamesListView = mainView.findViewById(R.id.notesFolderListView);
        folderNameView = mainView.findViewById(R.id.noteNameView);
        newFolderBtn = mainView.findViewById(R.id.newFolderBtn);
        folderNamesList = NoteFileOperations.loadFolderNames(context);
        folderNamesListAdapter = new ArrayAdapter<>(context, R.layout.folders_list_view, folderNamesList);
        folderNamesListView.setAdapter(folderNamesListAdapter);
        filePath = context.getFilesDir().getAbsolutePath();
        folderSpin = mainView.findViewById(R.id.folderSpin);

        // if a user presses the new folder button they can enter a name for folder and list view
        newFolderBtn.setOnClickListener(view -> {
            NewFolderDialog newFolderDialog = new NewFolderDialog(context);
            newFolderDialog.show();
        });

        //on item long click, the user will be asked if they want to delete the folder.
        folderNamesListView.setOnItemLongClickListener((parent, view, position, id) -> {
            String folderNameString = folderNamesListView.getItemAtPosition(position).toString();
            if (folderNameString.equals("Unclassified")) {
                Toast.makeText(getContext(), "Unclassified folder can't be deleted.", Toast.LENGTH_LONG).show();
            } else {
                DeleteFolderDialog deleteFolderDialog = new DeleteFolderDialog(context, folderNameString);
                deleteFolderDialog.show();
            }
            return true;
        });

        // when a folder is pressed the user is taken to a page where user can view all of the
        // notes in that folder
        folderNamesListView.setOnItemClickListener((parent, view, position, id) -> {
            // Check if a folder intent has not been clicked yet in the listview.
            if (!folderIntentIsClicked) {
                folderIntentIsClicked = true;
                Intent intent = new Intent(activity, NotesInFolderPage.class);
                String clickedFolderName = folderNamesListView.getItemAtPosition(position).toString();
                intent.putExtra("Folder Name", clickedFolderName);
                startActivity(intent);
            }
        });
        return mainView;
    }

    @Override
    public void onResume() {
        super.onResume();
        // This boolean is used to help prevent multiple clicks to occur on the same list view item.
        folderIntentIsClicked = false;
    }
}
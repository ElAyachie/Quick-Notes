package QuickNotes;

import android.content.Context;
import android.content.Intent;
import androidx.fragment.app.Fragment;
import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;

public class Tab1Fragment extends Fragment {
    ListView folderNamesListView;
    TextView folderNameView;
    Button newFolderBtn;
    String filePath;
    Context context;
    ArrayList<String> folderNamesList;
    Spinner folderSpin;
    ArrayAdapter folderNamesListAdapter;

    @Override
    public View onCreateView(LayoutInflater inflater, final ViewGroup container, Bundle savedInstanceState) {
        View mainView = inflater.inflate(R.layout.tab1_fragment, container, false);
        context = getContext();
        folderNamesListView = mainView.findViewById(R.id.notesFolderListView);
        folderNameView = mainView.findViewById(R.id.folderNameView);
        newFolderBtn = mainView.findViewById(R.id.newFolderBtn);
        folderNamesList = Note.loadFolderNames(context);
        folderNamesListAdapter = new ArrayAdapter<>(context, R.layout.folders_list_view, folderNamesList);
        folderNamesListView.setAdapter(folderNamesListAdapter);
        filePath = context.getFilesDir().getAbsolutePath();
        folderSpin = mainView.findViewById(R.id.folderSpin);

        //if a user presses the new folder button they can enter a name for folder and list view
        // will be updated.
        newFolderBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                AlertDialog.Builder builder = new AlertDialog.Builder(context);
                View alertView = getLayoutInflater().inflate(R.layout.alertdialog_new_folder, container, false);
                final EditText folderName = alertView.findViewById(R.id.folderName);
                final Button positiveButton = alertView.findViewById(R.id.positiveButton);
                final Button cancelButton = alertView.findViewById(R.id.cancelButton);
                builder.setView(alertView);
                final AlertDialog optionDialog = builder.show();
                positiveButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        final String folderNameString = folderName.getText().toString();
                        if (!folderNameString.equals("")) {
                            //creating a folder to store notes
                            if (folderNamesList.contains(folderNameString)) {
                                folderName.setError("Folder already exists.");
                            } else {
                                Note.saveFolderName(context, folderNameString);
                                folderNamesList.add(folderNameString);
                                folderNamesListAdapter.notifyDataSetChanged();
                                new Tab2Fragment().refreshFolderSpinner(folderNameString, true);
                                optionDialog.dismiss();
                                Toast.makeText(context, "Folder made.", Toast.LENGTH_LONG).show();
                            }
                        } else {
                            folderName.setError("Enter name.");
                        }
                    }
                });

                cancelButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        optionDialog.dismiss();
                    }
                });
            }
        });

        //on item long click, the user will be asked if user wants to delete the folder.
        folderNamesListView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                final String folderNameString = folderNamesListView.getItemAtPosition(position).toString();
                if (folderNameString.equals("Unclassified")) {
                    Toast.makeText(getContext(), "Unclassified folder can't be deleted.", Toast.LENGTH_LONG).show();
                } else {
                    AlertDialog.Builder builder = new AlertDialog.Builder(context);
                    View alertView = View.inflate(context, R.layout.alertdialog_delete_folder, null);
                    final Button positiveButton = alertView.findViewById(R.id.positiveButton);
                    final Button cancelButton = alertView.findViewById(R.id.cancelButton);
                    builder.setView(alertView);
                    final AlertDialog optionDialog = builder.show();
                    positiveButton.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            Note.deleteFolder(context, folderNameString);
                            folderNamesList.remove(folderNameString);
                            folderNamesListAdapter.notifyDataSetChanged();
                            new Tab2Fragment().refreshFolderSpinner(folderNameString, false);
                            optionDialog.dismiss();
                        }
                    });
                    cancelButton.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            optionDialog.dismiss();
                        }
                    });
                }
                return true;
            }
        });

        //when a folder is pressed the user is taken to a page where user can view all of the
        // notes in that folder
        folderNamesListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent intent = new Intent(getActivity(), NotesInFolderPage.class);
                String clickedFolderName = folderNamesListView.getItemAtPosition(position).toString();
                intent.putExtra("Folder Name", clickedFolderName);
                startActivity(intent);
            }
        });
        return mainView;
    }
}
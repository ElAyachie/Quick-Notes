package QuickNotes;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.google.android.material.textfield.TextInputEditText;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;


public class Tab2Fragment extends Fragment {
    static ArrayAdapter<String> folderNamesListAdapter;
    ImageButton addNoteButton;
    TextInputEditText noteContentText;
    EditText noteNameText;
    Spinner folderSpin;
    static ArrayList<String> folderNamesList;
    Context context;
    Date dateCreated;
    String noteNameString;
    String noteString;
    RelativeLayout myLayout;

    @Override
    public View onCreateView(LayoutInflater inflater, final ViewGroup container, Bundle savedInstanceState) {
        super.onCreate(null);
        View mainView = inflater.inflate(R.layout.tab2_fragment, container, false);
        context = getContext();
        noteNameText = mainView.findViewById(R.id.noteNameText);
        folderSpin = mainView.findViewById(R.id.folderSpin);
        noteContentText = mainView.findViewById(R.id.reminderNameText);
        // Loading all folder names in the spinner.
        folderNamesList = Note.loadFolderNames(context);
        folderNamesListAdapter = new ArrayAdapter<>(context, R.layout.custom_spinner, folderNamesList);
        folderNamesListAdapter.setDropDownViewResource(R.layout.custom_spinner_dropdown);
        folderSpin.setAdapter(folderNamesListAdapter);
        myLayout = mainView.findViewById(R.id.relativeLayout);
        myLayout.requestFocus();

        // Button used for creating notes.
        addNoteButton = mainView.findViewById(R.id.addNoteButton);
        addNoteButton.setOnClickListener(view -> {
            noteNameString = noteNameText.getText().toString();
            if(noteContentText.getText() != null){
                noteString = noteContentText.getText().toString();
            }
            else {
                noteString = "";
            }
            // Inform the user that they need a name for the note to be made.
            if (noteNameString.equals("")) {
                noteNameText.setError("Please enter a name.");
            } else {
                dateCreated = Calendar.getInstance().getTime();
                ArrayList<String> allNotesNames = Note.loadAllNoteNamesInFolder(context, folderSpin.getSelectedItem().toString());
                if (allNotesNames.contains(noteNameString)) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(context);
                    View alertView = getLayoutInflater().inflate(R.layout.alertdialog_overwrite_note, container);
                    final Button positiveButton = alertView.findViewById(R.id.positiveButton);
                    final Button cancelButton = alertView.findViewById(R.id.cancelButton);
                    builder.setView(alertView);
                    final AlertDialog optionDialog = builder.show();
                    positiveButton.setOnClickListener(view1 -> {
                        dateCreated = Calendar.getInstance().getTime();
                        SimpleDateFormat formatter = new SimpleDateFormat("MM/dd/yyyy", Locale.US);
                        String dateString = formatter.format(dateCreated);
                        Note.saveNote(context, folderSpin.getSelectedItem().toString(), noteNameString, noteString, dateString);
                        updateTextFields();
                        Toast.makeText(getContext(), "Note overwritten.", Toast.LENGTH_LONG).show();
                        optionDialog.dismiss();
                    });
                    cancelButton.setOnClickListener(view12 -> optionDialog.dismiss());
                } else {
                    dateCreated = Calendar.getInstance().getTime();
                    SimpleDateFormat formatter = new SimpleDateFormat("MM/dd/yyyy", Locale.US);
                    String dateString = formatter.format(dateCreated);
                    Note.saveNote(context, folderSpin.getSelectedItem().toString(), noteNameString, noteString, dateString);
                    updateTextFields();
                    Toast.makeText(getContext(), "Note saved.", Toast.LENGTH_LONG).show();
                }
            }
        });
        return mainView;
    }

    // Helps reload the fragment if there are any new
    // folders added.
    public void refreshFolderSpinner(String folderName, boolean add) {
        if (add){
            folderNamesList.add(folderName);
        }
        else {
            folderNamesList.remove(folderName);
        }
        folderNamesListAdapter.notifyDataSetChanged();
    }


    // Created for when switching between fragment 1 and 2,
    // so that there is not any information left behind.
    public void updateTextFields() {
        noteNameText.setText("");
        noteContentText.setText("");
    }
}

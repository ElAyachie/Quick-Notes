package QuickNotes;

import android.app.AlertDialog;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import com.google.android.material.textfield.TextInputEditText;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import QuickNotes.Dialogs.CheckNoteDialog;


public class Tab2HomeFragment extends Fragment {
    public static ArrayAdapter<String> folderNamesListAdapter;
    ImageButton addNoteButton;
    TextInputEditText noteContentText;
    EditText noteNameText;
    Spinner folderSpin;
    public static ArrayList<String> folderNamesList;
    Context context;
    Date dateCreated;
    String noteName;
    String noteContent;
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
        folderNamesList = NoteFileOperations.loadFolderNames(context);
        folderNamesListAdapter = new ArrayAdapter<>(context, R.layout.custom_spinner, folderNamesList);
        folderNamesListAdapter.setDropDownViewResource(R.layout.custom_spinner_dropdown);
        folderSpin.setAdapter(folderNamesListAdapter);
        myLayout = mainView.findViewById(R.id.relativeLayout);
        myLayout.requestFocus();

        // Button used for creating notes.
        addNoteButton = mainView.findViewById(R.id.addNoteButton);
        addNoteButton.setOnClickListener(view -> {
            noteName = noteNameText.getText().toString();
            if (noteContentText.getText() != null) {
                noteContent = noteContentText.getText().toString();
            } else {
                noteString = "";
            }
            // Inform the user that they need a name for the note to be made.
            if (noteName.equals("")) {
                noteNameText.setError("Please enter a name.");
            } else {
                dateCreated = Calendar.getInstance().getTime();
                SimpleDateFormat formatter = new SimpleDateFormat("MM/dd/yyyy", Locale.US);
                String dateString = formatter.format(dateCreated);
                String folderName = folderSpin.getSelectedItem().toString();
                List<Note> allNotes = NoteFileOperations.loadAllNotesInFolder(context, folderName);
                Note note = new Note(noteName, noteContent, folderName, dateString);
                boolean found = false;
                for (Note n : allNotes) {
                    if (n.getNoteName().equals(noteName)) {
                        found = true;
                        CheckNoteDialog checkNoteDialog = new CheckNoteDialog(context, note);
                        AlertDialog optionDialog = checkNoteDialog.show();
                        optionDialog.setOnDismissListener(dialogInterface -> updateTextFields());
                    }
                }
                if (!found) {
                    NoteFileOperations.saveNote(context, note);
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
        if (add) {
            folderNamesList.add(folderName);
        } else {
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

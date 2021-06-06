package QuickNotes;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;


import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

public class NotesInFolderPage extends AppCompatActivity {
    TextView folderNameView;
    static ListView notesFolderListView;
    Button newNotesBtn;
    String currentFolder;
    static ArrayList<String> allNotesContent = new ArrayList<>();
    static ArrayList<String> allNoteNames = new ArrayList<>();
    String filePath;
    Date dateCreated;
    static NotesCustomAdapter notesAdapter;
    SharedPreferences pref;
    private Boolean nightTheme;
    Context context;

    protected void onCreate(Bundle savedInstanceState) {
        pref = getSharedPreferences("MyPref", 0);
        if (pref.getBoolean("NIGHT MODE", false)) {
            setTheme(R.style.DarkTheme);
        }
        else if (! pref.getBoolean("NIGHT MODE", false)) {
            setTheme(R.style.AppTheme);
        }

        nightTheme = pref.getBoolean("NIGHT MODE", false);
        setContentView(R.layout.notes_in_folder_view);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        assert actionBar != null;
        actionBar.setHomeButtonEnabled(true);
        actionBar.setDisplayHomeAsUpEnabled(true);
        super.onCreate(savedInstanceState);
        folderNameView = findViewById(R.id.folderNameView);
        notesFolderListView = findViewById(R.id.notesFolderListView);
        newNotesBtn = findViewById(R.id.newNoteBtn);
        filePath = getFilesDir().getAbsolutePath();
        currentFolder = getIntent().getStringExtra("Folder Name");
        folderNameView.setText(currentFolder);
        allNoteNames = Note.loadAllNoteNamesInFolder(this, currentFolder);
        allNotesContent = Note.loadAllNoteContentInFolder(this, currentFolder);
        notesAdapter = new NotesCustomAdapter(this, allNoteNames, allNotesContent);
        notesFolderListView.setAdapter(notesAdapter);
        context = getBaseContext();

        notesFolderListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent intent = new Intent(view.getContext(), SpecificNoteEdit.class);
                String clickedNoteName = notesFolderListView.getItemAtPosition(position).toString();
                String clickedNoteContent = allNotesContent.get(position);
                intent.putExtra("Folder name", currentFolder);
                intent.putExtra("Note name", clickedNoteName);
                intent.putExtra("Note content", clickedNoteContent);
                startActivity(intent);
            }
        });

        newNotesBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View view) {
                AlertDialog.Builder builder = new AlertDialog.Builder(view.getContext());
                View alertView = View.inflate(context, R.layout.alertdialog_new_note, null);
                final Button positiveButton = alertView.findViewById(R.id.positiveButton);
                final Button cancelButton = alertView.findViewById(R.id.cancelButton);
                final EditText noteNameText = alertView.findViewById(R.id.noteNameText);
                final EditText noteContentText = alertView.findViewById(R.id.reminderNameText);
                builder.setView(alertView);
                final AlertDialog optionDialog = builder.show();
                positiveButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        final String noteNameString = noteNameText.getText().toString();
                        final String noteContentString = noteContentText.getText().toString();
                        if (noteNameString.equals("")) {
                            noteNameText.setError("Please enter a name.");
                        }
                        else if (allNoteNames.contains(noteNameString)){
                            noteNameText.setError("This note already exists.");
                        }
                        else {
                            dateCreated = Calendar.getInstance().getTime();
                            Note.saveNote(context, currentFolder, noteNameString, noteContentString);
                            allNoteNames.add(noteNameString);
                            allNotesContent.add(noteContentString);
                            notesAdapter.notifyDataSetChanged();
                            optionDialog.dismiss();
                            Toast.makeText(context, "Note saved.", Toast.LENGTH_LONG).show();
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

        notesFolderListView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @SuppressLint("SetTextI18n")
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, final int position, long id) {
                final String noteName = notesFolderListView.getItemAtPosition(position).toString();
                AlertDialog.Builder builder = new AlertDialog.Builder(NotesInFolderPage.this);
                View alertView = View.inflate(context, R.layout.alertdialog_delete_note, null);
                final Button positiveButton = alertView.findViewById(R.id.positiveButton);
                final Button cancelButton = alertView.findViewById(R.id.cancelButton);
                builder.setView(alertView);
                final AlertDialog optionDialog = builder.show();
                positiveButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Note.deleteNote(context, noteName, currentFolder);
                        allNoteNames.remove(noteName);
                        allNotesContent.remove(Note.loadNote(context, noteName, currentFolder));
                        notesAdapter.notifyDataSetChanged();
                        Toast.makeText(context, "Note deleted.", Toast.LENGTH_LONG).show();
                        optionDialog.dismiss();
                    }
                });
                cancelButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        optionDialog.dismiss();
                    }
                });
                return true;
            }
        });

    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    public void hideKeyboard(Activity activity) {
        View view = activity.getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    public void refreshListView(Context context, String currentFolder) {
            allNoteNames = Note.loadAllNoteNamesInFolder(context, currentFolder);
            allNotesContent = Note.loadAllNoteContentInFolder(context, currentFolder);
            notesAdapter = new NotesCustomAdapter(context, allNoteNames, allNotesContent);
            notesFolderListView.setAdapter(notesAdapter);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            Intent intent = new Intent(this, SettingsPage.class);
            startActivity(intent);
        }
        if (id == R.id.action_reminder_page) {
            Intent intent = new Intent(this, RemindersPage.class);
            startActivity(intent);
        }

        return super.onOptionsItemSelected(item);
    }

    public void onResume(){
        if (nightTheme != pref.getBoolean("NIGHT MODE", false)){
            recreate();
        }
        super.onResume();
    }

}

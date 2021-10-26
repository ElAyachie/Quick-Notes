package QuickNotes;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import java.util.List;

import QuickNotes.Adapters.NotesCustomAdapter;
import QuickNotes.Dialogs.DeleteNoteDialog;
import QuickNotes.Dialogs.NewNoteDialog;

public class NotesInFolderPage extends AppCompatActivity {
    TextView folderNameView;
    ListView notesFolderListView;
    Button newNotesBtn;
    String currentFolder;
    public static List<Note> allNotes;
    String filePath;
    NotesCustomAdapter notesAdapter;
    SharedPreferences pref;
    private Boolean nightTheme;
    Context context;
    Boolean noteIsClicked;

    protected void onCreate(Bundle savedInstanceState) {
        pref = getSharedPreferences("MyPref", 0);
        if (pref.getBoolean("NIGHT MODE", false)) {
            setTheme(R.style.DarkTheme);
        } else if (!pref.getBoolean("NIGHT MODE", false)) {
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
        folderNameView = findViewById(R.id.noteNameView);
        notesFolderListView = findViewById(R.id.notesFolderListView);
        newNotesBtn = findViewById(R.id.newNoteBtn);
        filePath = getFilesDir().getAbsolutePath();
        currentFolder = getIntent().getStringExtra("Folder Name");
        folderNameView.setText(currentFolder);
        allNotes = NoteFileOperations.loadAllNotesInFolder(this, currentFolder);
        notesAdapter = new NotesCustomAdapter(this, allNotes);
        notesFolderListView.setAdapter(notesAdapter);
        context = this;

        notesFolderListView.setOnItemClickListener((AdapterView<?> parent, View view, int position, long id) -> {
            // Check if a folder intent has not been clicked yet in the listview.
            if (!noteIsClicked) {
                noteIsClicked = true;
                Intent intent = new Intent(view.getContext(), SpecificNoteEdit.class);
                Note selectedNote = (Note) notesFolderListView.getItemAtPosition(position);
                intent.putExtra("note", selectedNote);
                startActivity(intent);
            }
        });

        newNotesBtn.setOnClickListener((View view) -> {
            NewNoteDialog newNoteDialog = new NewNoteDialog(this, currentFolder);
            AlertDialog optionDialog = newNoteDialog.show();
            optionDialog.setOnDismissListener(dialogInterface -> refreshListView());
        });

        notesFolderListView.setOnItemLongClickListener((parent, view, position, id) -> {
            Note selectedNote = (Note) notesFolderListView.getItemAtPosition(position);
            DeleteNoteDialog deleteNoteDialog = new DeleteNoteDialog(this, selectedNote);
            AlertDialog optionDialog = deleteNoteDialog.show();
            optionDialog.setOnDismissListener(dialogInterface -> refreshListView());
            return true;
        });
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    public void refreshListView() {
        allNotes = NoteFileOperations.loadAllNotesInFolder(context, currentFolder);
        notesAdapter = new NotesCustomAdapter(context, allNotes);
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

    public void onResume() {
        if (nightTheme != pref.getBoolean("NIGHT MODE", false)) {
            recreate();
        }
        refreshListView();
        noteIsClicked = false;
        super.onResume();
    }
}

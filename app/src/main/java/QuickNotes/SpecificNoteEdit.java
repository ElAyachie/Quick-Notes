package QuickNotes;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;

import com.google.android.material.textfield.TextInputEditText;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import QuickNotes.Dialogs.DateTimePickerDialog;
import QuickNotes.Dialogs.LocationReminderDialog;
import QuickNotes.Dialogs.SaveNoteDialog;
import QuickNotes.Services.ReminderBroadcast;

public class SpecificNoteEdit extends AppCompatActivity {
    String currentFolder;
    String previousFolder;
    Note note = new Note();
    String currentNoteName;
    String currentNoteContent;
    EditText noteNameText;
    TextInputEditText noteContentText;
    TextView noteDateText;
    ImageButton confirmChangesBtn;
    Date dateEdited;
    Context context;
    Spinner folderSpin;
    static ArrayList<String> folderNamesList = new ArrayList<>();
    SharedPreferences pref;
    private Boolean nightTheme;
    NotificationManager notificationManager;

    protected void onCreate(Bundle savedInstanceState) {
        pref = getSharedPreferences("MyPref", 0);
        if (pref.getBoolean("NIGHT MODE", false)) {
            setTheme(R.style.DarkTheme);
        } else if (!pref.getBoolean("NIGHT MODE", false)) {
            setTheme(R.style.AppTheme);
        }
        nightTheme = pref.getBoolean("NIGHT MODE", false);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.specific_note_edit);
        context = getBaseContext();
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        assert actionBar != null;
        actionBar.setHomeButtonEnabled(true);
        actionBar.setDisplayHomeAsUpEnabled(true);

        confirmChangesBtn = findViewById(R.id.addNoteButton);
        noteNameText = findViewById(R.id.noteNameText);
        noteDateText = findViewById(R.id.noteDateText);
        folderSpin = findViewById(R.id.folderSpin);
        noteContentText = findViewById(R.id.reminderNameText);
        note = (Note) getIntent().getSerializableExtra("note");
        assert note != null;
        noteNameText.setText(note.getNoteName());
        noteContentText.setText(note.getNoteContent());
        noteDateText.setText(note.getDateCreated());

        //setting up the spinner with folder names
        folderNamesList = NoteFileOperations.loadFolderNames(context);
        ArrayAdapter<String> folderNamesListAdapter = new ArrayAdapter<>(context, R.layout.custom_spinner, folderNamesList);
        folderNamesListAdapter.setDropDownViewResource(R.layout.custom_spinner_dropdown);
        folderSpin.setAdapter(folderNamesListAdapter);
        refreshFolderSpinner(context, folderNamesListAdapter);
        folderSpin.setSelection(folderNamesListAdapter.getPosition(note.getFolderName()));

        confirmChangesBtn.setOnClickListener(view -> {
            previousFolder = currentFolder;
            currentFolder = folderSpin.getSelectedItem().toString();
            currentNoteName = noteNameText.getText().toString();
            assert noteContentText.getText() != null;
            currentNoteContent = noteContentText.getText().toString();
            if (currentNoteName.equals("")) {
                noteNameText.setError("Please enter a name.");
            } else {
                dateEdited = Calendar.getInstance().getTime();
                NoteFileOperations.deleteNote(context, note);
                Date dateEdited = Calendar.getInstance().getTime();
                SimpleDateFormat formatter = new SimpleDateFormat("MM/dd/yyyy", Locale.US);
                String dateString = formatter.format(dateEdited);
                Note newNote = new Note(currentNoteName, currentNoteContent, currentFolder, dateString);
                NoteFileOperations.saveNote(context, newNote);
                note = newNote;
                Toast.makeText(context, "Note saved.", Toast.LENGTH_LONG).show();
            }
        });
    }

    public void refreshFolderSpinner(Context context, ArrayAdapter<String> aa) {
        folderNamesList = NoteFileOperations.loadFolderNames(context);
        aa.notifyDataSetChanged();
    }

    @Override
    public void onBackPressed() {
        String newNoteName = noteNameText.getText().toString();
        assert noteContentText.getText() != null;
        String newNoteContent = noteContentText.getText().toString();
        String newNoteFolder = folderSpin.getSelectedItem().toString();
        if (newNoteName.equals("")) {
            noteNameText.setError("Please enter a name.");
        } else if (!note.getNoteContent().equals(newNoteContent) || !note.getFolderName().equals(newNoteFolder)
                || !note.getNoteName().equals(newNoteName)) {
            Date dateEdited = Calendar.getInstance().getTime();
            SimpleDateFormat formatter = new SimpleDateFormat("MM/dd/yyyy", Locale.US);
            String dateString = formatter.format(dateEdited);
            Note newNote = new Note(newNoteName, newNoteContent, newNoteFolder, dateString);
            SaveNoteDialog saveNoteDialog = new SaveNoteDialog(this, note, newNote);
            AlertDialog optionDialog = saveNoteDialog.show();
            optionDialog.setOnDismissListener(dialogInterface -> SpecificNoteEdit.super.onBackPressed());
        } else {
            super.onBackPressed();
        }
    }

    public void hideKeyboard(Activity activity) {
        View view = activity.getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_specific_note, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        Intent intent;
        if (id == R.id.action_settings) {
            intent = new Intent(this, SettingsPage.class);
            startActivity(intent);
        } else if (id == R.id.action_reminder_page) {
            intent = new Intent(this, RemindersPage.class);
            startActivity(intent);
        } else if (id == R.id.action_set_time_reminder) {
            DateTimePickerDialog dateTimePickerDialog = new DateTimePickerDialog(this, note);
            dateTimePickerDialog.show();
        } else if (id == R.id.action_set_location_reminder) {
            LocationReminderDialog locationReminderDialog = new LocationReminderDialog(this, getSupportFragmentManager(), note);
            locationReminderDialog.show();
        }
        return super.onOptionsItemSelected(item);
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    public void cancelReminder(Context context, String notificationReminderID) {
        if (notificationManager == null) {
            Intent intent = new Intent(context, ReminderBroadcast.class);
            PendingIntent pendingIntent = PendingIntent.getBroadcast(context, Integer.parseInt(notificationReminderID), intent, PendingIntent.FLAG_IMMUTABLE);
            AlarmManager alarmManager = (AlarmManager) context.getSystemService(ALARM_SERVICE);
            alarmManager.cancel(pendingIntent);
            pendingIntent.cancel();
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        hideKeyboard(this);
        onBackPressed();
        return true;
    }

    public void onResume() {
        if (nightTheme != pref.getBoolean("NIGHT MODE", false)) {
            recreate();
        }
        super.onResume();
    }
}
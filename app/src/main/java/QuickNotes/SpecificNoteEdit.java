package QuickNotes;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import com.google.android.material.textfield.TextInputEditText;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;


import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;


public class SpecificNoteEdit extends AppCompatActivity {
    String currentFolder;
    String currentNoteName;
    String currentNoteContent;
    String currentNoteDate;
    EditText noteNameText;
    TextInputEditText noteContentText;
    TextView noteDateText;
    ImageButton confirmChangesBtn;
    Date dateEdited;
    Context context;
    static private ArrayAdapter<String> folderNamesListAdapter;
    Spinner folderSpin;
    static ArrayList<String> folderNamesList = new ArrayList<>();
    SharedPreferences pref;
    private Boolean nightTheme;
    private NotificationManager notificationManager;
    SharedPreferences.Editor editor;
    int notificationID;

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
        currentFolder = getIntent().getStringExtra("Folder name");
        currentNoteName = getIntent().getStringExtra("Note name");
        currentNoteContent = getIntent().getStringExtra("Note content");
        currentNoteDate = getIntent().getStringExtra("Note date");
        noteNameText.setText(currentNoteName);
        noteContentText.setText(currentNoteContent);
        noteDateText.setText(currentNoteDate);
        //setting up the spinner with folder names
        folderNamesList = Note.loadFolderNames(context);
        folderNamesListAdapter = new ArrayAdapter<>(context, R.layout.custom_spinner, folderNamesList);
        folderNamesListAdapter.setDropDownViewResource(R.layout.custom_spinner_dropdown);
        folderSpin.setAdapter(folderNamesListAdapter);
        refreshFolderSpinner(context);
        folderSpin.setSelection(folderNamesListAdapter.getPosition(currentFolder));

        confirmChangesBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                currentFolder = folderSpin.getSelectedItem().toString();
                currentNoteName = noteNameText.getText().toString();
                assert noteContentText.getText() != null;
                currentNoteContent = noteContentText.getText().toString();
                if (currentNoteName.equals("")) {
                    noteNameText.setError("Please enter a name.");
                }
                dateEdited = Calendar.getInstance().getTime();
                Note.deleteNote(context, currentNoteName, currentFolder);
                SimpleDateFormat formatter = new SimpleDateFormat("MM/dd/yyyy");
                String dateString = formatter.format(dateEdited);
                Note.saveNote(context, currentFolder, currentNoteName, currentNoteContent, dateString);
                new NotesInFolderPage().refreshListView(context, currentFolder);
                Toast.makeText(context, "Note saved.", Toast.LENGTH_LONG).show();
            }
        });
    }


    public void refreshFolderSpinner(Context context) {
        folderNamesList = Note.loadFolderNames(context);
        folderNamesListAdapter.notifyDataSetChanged();
    }

    @Override
    public void onBackPressed() {
        final String noteNameString = noteNameText.getText().toString();
        assert noteContentText.getText() != null;
        final String noteContentString = noteContentText.getText().toString();
        final String folderNameString = folderSpin.getSelectedItem().toString();
        if (noteNameString.equals("")) {
            noteNameText.setError("Please enter a name.");
        }
        else if (!currentNoteContent.equals(noteContentString) || !currentFolder.equals(folderNameString)
                || !currentNoteName.equals(noteNameString)) {
            View alertView = View.inflate(this, R.layout.alertdialog_save_note, null);
            final Button positiveButton = alertView.findViewById(R.id.positiveButton);
            final Button cancelButton = alertView.findViewById(R.id.cancelButton);
            final AlertDialog optionDialog = new AlertDialog.Builder(this).create();
            positiveButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    dateEdited = Calendar.getInstance().getTime();
                    Note.deleteNote(context, currentNoteName, currentFolder);
                    SimpleDateFormat formatter = new SimpleDateFormat("MM/dd/yyyy");
                    String dateString = formatter.format(dateEdited);
                    Note.deleteNote(context, currentNoteName, currentFolder);
                    Note.saveNote(context, folderNameString, noteNameString, noteContentString, dateString);
                    new NotesInFolderPage().refreshListView(context, currentFolder);
                    Toast.makeText(context, "Note saved.", Toast.LENGTH_LONG).show();
                    SpecificNoteEdit.super.onBackPressed();
                }
            });
            cancelButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    optionDialog.dismiss();
                    SpecificNoteEdit.super.onBackPressed();
                }
            });
            optionDialog.setView(alertView);
            optionDialog.show();
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
        switch (id) {
            //noinspection SimplifiableIfStatement
            case R.id.action_settings:
                intent = new Intent(this, SettingsPage.class);
                startActivity(intent);
                break;

            case R.id.action_reminder_page:
                intent = new Intent(this, RemindersPage.class);
                startActivity(intent);
                break;

            case R.id.action_set_reminder:
                final View dialogView = View.inflate(this, R.layout.date_time_picker, null);
                final AlertDialog alertDialog = new AlertDialog.Builder(this).create();
                final DatePicker datePicker = dialogView.findViewById(R.id.date_picker);
                final TimePicker timePicker = dialogView.findViewById(R.id.time_picker);
                datePicker.setMinDate(System.currentTimeMillis() - 1000);

                dialogView.findViewById(R.id.date_time_set).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Calendar calendar = new GregorianCalendar(datePicker.getYear(),
                                datePicker.getMonth(),
                                datePicker.getDayOfMonth(),
                                timePicker.getCurrentHour(),
                                timePicker.getCurrentMinute());
                        final String dateTime = calendar.getTime().toString();
                        String formattedReminderDateTime = (dateTime.substring(0, 16) + dateTime.substring(19, 28)).replaceAll(" ", "");

                        editor = pref.edit();
                        notificationID = pref.getInt("notificationID", 0);
                        editor.putInt("notificationID", notificationID + 1);
                        editor.putString("Note folder", currentFolder);
                        editor.putString("Note name", currentNoteName);
                        editor.putString("Note content", currentNoteContent);
                        editor.putString("Note date", formattedReminderDateTime);
                        notificationID = pref.getInt("notificationID", 0);
                        editor.apply();
                        Reminder.saveReminder(context, currentNoteName, currentNoteContent, formattedReminderDateTime, String.valueOf(notificationID));
                        Intent intent = new Intent(context, ReminderBroadcast.class);
                        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, notificationID, intent, 0);
                        AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
                        alarmManager.set(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingIntent);
                        Toast.makeText(context, "Reminder set.", Toast.LENGTH_LONG).show();
                        alertDialog.dismiss();
                    }
                });
                dialogView.findViewById(R.id.cancelButton).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        alertDialog.dismiss();
                    }
                });
                alertDialog.setView(dialogView);
                alertDialog.show();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    public void cancelReminder(Context context, String notificationReminderID) {
        if (notificationManager == null) {
            Intent intent = new Intent(context, ReminderBroadcast.class);
            PendingIntent pendingIntent = PendingIntent.getBroadcast(context, Integer.parseInt(notificationReminderID), intent, 0);
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

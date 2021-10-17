package QuickNotes;

import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;

public class RemindersPage extends AppCompatActivity {
    static ListView reminderListView;
    Button newNotesBtn;
    static ArrayList<String> allReminderNames = new ArrayList<>();
    static ArrayList<String> allNotesContent = new ArrayList<>();
    TextView reminderNameText;
    TextView reminderDateText;
    Context context;
    String reminderNotificationID;
    Intent intent;
    PendingIntent pendingIntent;
    static RemindersCustomAdapter notesAdapter;
    SharedPreferences pref;
    private Boolean nightTheme;

    protected void onCreate(Bundle savedInstanceState) {
        pref = getSharedPreferences("MyPref", 0);
        if (pref.getBoolean("NIGHT MODE", false)) {
            setTheme(R.style.DarkTheme);
        } else if (!pref.getBoolean("NIGHT MODE", false)) {
            setTheme(R.style.AppTheme);
        }
        nightTheme = pref.getBoolean("NIGHT MODE", false);
        setContentView(R.layout.reminder_page_view);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        assert actionBar != null;
        actionBar.setHomeButtonEnabled(true);
        actionBar.setDisplayHomeAsUpEnabled(true);
        super.onCreate(savedInstanceState);
        reminderListView = findViewById(R.id.noteReminderListView);
        newNotesBtn = findViewById(R.id.newNoteBtn);
        allReminderNames = Reminder.loadReminderNames(this);
        allNotesContent = Reminder.loadAllNoteContentInReminderFolder(this);
        notesAdapter = new RemindersCustomAdapter(this, allReminderNames, allNotesContent);
        reminderListView.setAdapter(notesAdapter);
        context = getBaseContext();


        reminderListView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @SuppressLint("SetTextI18n")
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, final int position, long id) {
                final String longClickedNoteName = reminderListView.getItemAtPosition(position).toString();
                AlertDialog.Builder builder = new AlertDialog.Builder(RemindersPage.this);
                View alertView = View.inflate(context, R.layout.alertdialog_delete_reminder, null);
                final Button positiveButton = alertView.findViewById(R.id.positiveButton);
                final Button cancelButton = alertView.findViewById(R.id.cancelButton);
                builder.setView(alertView);
                final AlertDialog optionDialog = builder.show();
                reminderNameText = findViewById(R.id.noteNameText);
                reminderDateText = findViewById(R.id.reminderDateText);
                StringBuilder usingJustToReverse = new StringBuilder(longClickedNoteName);
                String reversedLongClickedName = usingJustToReverse.reverse().toString();
                int startNotificationID = longClickedNoteName.length() + 1;
                for (int i = 0; i < reversedLongClickedName.length(); i++){
                    startNotificationID--;
                    if (reversedLongClickedName.charAt(i) == '_'){
                        break;
                    }
                }
                reminderNotificationID = longClickedNoteName.substring(startNotificationID);
                positiveButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Reminder.deleteReminder(context, longClickedNoteName, "");
                        intent = new Intent(context, ReminderBroadcast.class);
                        pendingIntent = PendingIntent.getActivity(context, Integer.parseInt(reminderNotificationID), intent, 0);
                        new SpecificNoteEdit().cancelReminder(context, reminderNotificationID);
                        refreshReminderListView(context);
                        reminderListView.setAdapter(notesAdapter);
                        Toast.makeText(getBaseContext(), "Reminder deleted.", Toast.LENGTH_LONG).show();
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

    public void refreshReminderListView(Context context) {
        if (reminderListView != null) {
        allReminderNames = Reminder.loadReminderNames(context);
        allNotesContent = Reminder.loadAllNoteContentInReminderFolder(context);
        notesAdapter = new RemindersCustomAdapter(context, allReminderNames, allNotesContent);
        reminderListView = findViewById(R.id.noteReminderListView);
        reminderListView.setAdapter(notesAdapter);
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
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

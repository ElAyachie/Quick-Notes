package QuickNotes;

import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.widget.Button;
import android.widget.ListView;

import java.util.ArrayList;

import QuickNotes.Adapters.RemindersCustomAdapter;
import QuickNotes.Dialogs.DeleteReminderDialog;

public class RemindersPage extends AppCompatActivity {
    ListView reminderListView;
    Button newNotesBtn;
    static ArrayList<Reminder> allReminders = new ArrayList<>();
    Context context;
    RemindersCustomAdapter reminderAdapter;
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
        context = getBaseContext();
        reminderListView = findViewById(R.id.noteReminderListView);
        newNotesBtn = findViewById(R.id.newNoteBtn);
        allReminders = ReminderFileOperations.loadAllReminders(context);
        reminderAdapter = new RemindersCustomAdapter(context, allReminders);
        reminderListView.setAdapter(reminderAdapter);


        reminderListView.setOnItemLongClickListener((parent, view, position, id) -> {
            Reminder selectedReminder = (Reminder) reminderListView.getItemAtPosition(position);
            DeleteReminderDialog deleteReminderDialog = new DeleteReminderDialog(this, selectedReminder);
            AlertDialog optionDialog = deleteReminderDialog.show();
            optionDialog.setOnDismissListener(dialogInterface -> refreshReminderListView());
            return true;
        });
    }

    // The app will update this from anywhere on the app
    public void refreshReminderListView() {
        allReminders = ReminderFileOperations.loadAllReminders(context);
        reminderAdapter = new RemindersCustomAdapter(context, allReminders);
        reminderListView.setAdapter(reminderAdapter);
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
        refreshReminderListView();
        super.onResume();
    }
}


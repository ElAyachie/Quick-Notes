package QuickNotes.Adapters;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.ArrayList;

import QuickNotes.R;
import QuickNotes.Reminder;

public class RemindersCustomAdapter extends BaseAdapter {
    private final Context context;
    private final ArrayList<Reminder> allReminders;

    public RemindersCustomAdapter(Context context, ArrayList<Reminder> allReminders) {
        this.context = context;
        this.allReminders = allReminders;
    }

    @Override
    public int getCount() {
        return allReminders.size();
    }

    @Override
    public Reminder getItem(int position) {
        return this.allReminders.get(position);
    }

    @Override
    public long getItemId(int position) {
        return this.allReminders.size();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            Reminder reminder = allReminders.get(position);
            convertView = View.inflate(this.context, R.layout.reminders_list_view, null);
            TextView noteNamesText = convertView.findViewById(R.id.noteNameText);
            TextView reminderDateText = convertView.findViewById(R.id.reminderDateText);

            noteNamesText.setText(reminder.getNoteName());
            String line2;
            if (reminder.getType().equals("Time")) {
                line2 = "Time: " + reminder.getReminderDate();
            } else {
                line2 = "Location";
            }
            reminderDateText.setText(line2);
            if (noteNamesText.getText().length() > 40) {
                noteNamesText.setText(String.format("%s...", reminder.getNoteName().substring(0, 40)));
            }
        }
        return convertView;
    }

    @Override
    public int getViewTypeCount() {
        if (getCount() > 0) {
            return getCount();
        } else {
            return super.getViewTypeCount();
        }
    }

    @Override
    public int getItemViewType(int position) {
        return position;
    }
}

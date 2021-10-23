package QuickNotes.Adapters;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.ArrayList;

import QuickNotes.R;

public class RemindersCustomAdapter extends BaseAdapter {
    private final Context context;
    private final ArrayList<String> noteNamesList;

    public RemindersCustomAdapter(Context context, ArrayList<String> noteNames) {
        this.context = context;
        this.noteNamesList = noteNames;
    }

    @Override
    public int getCount() {
        return noteNamesList.size();
    }

    @Override
    public Object getItem(int position) {
        return this.noteNamesList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return this.noteNamesList.size();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = View.inflate(this.context, R.layout.reminders_list_view, null);
            TextView noteNamesText = convertView.findViewById(R.id.noteNameText);
            TextView reminderDateText = convertView.findViewById(R.id.reminderDateText);

            String noteNameWDateAndNotification = noteNamesList.get(position);
            StringBuilder sb = new StringBuilder(noteNameWDateAndNotification);
            String reversedNoteNameWDateAndNotification = sb.reverse().toString();
            int startNotificationID = 0;
            for (int i = 0; i < reversedNoteNameWDateAndNotification.length(); i++) {
                startNotificationID++;
                if (reversedNoteNameWDateAndNotification.charAt(i) == '_') {
                    break;
                }
            }
            int endOfString = noteNameWDateAndNotification.length() - startNotificationID;
            String noteNameWDate = noteNameWDateAndNotification.substring(0, endOfString);
            String displayNoteName = noteNameWDate.substring(0, noteNameWDate.length() - 20);
            String date = noteNameWDate.substring(noteNameWDate.length() - 20, endOfString);
            String month = date.substring(3, 6);
            String monthDay = date.substring(6, 8);
            String time = date.substring(8, 13);

            int americanTime = Integer.parseInt(time.substring(0, 2)) % 12;
            if (americanTime == 0) {
                time = 12 + time.substring(2, 5) + "PM";
            } else if (Integer.parseInt(time.substring(0, 2)) > 12) {
                time = americanTime + time.substring(2, 5) + "PM";
            } else {
                time = time + "AM";
            }
            String year = date.substring(16, 20);

            String displayDate = month + " " + monthDay + ", " + year + ", " + time;
            noteNamesText.setText(displayNoteName);
            reminderDateText.setText(displayDate);
            if (noteNamesText.getText().length() > 40) {
                noteNamesText.setText(String.format("%s...", displayNoteName.substring(0, 40)));
            }
        }
        return convertView;
    }
}

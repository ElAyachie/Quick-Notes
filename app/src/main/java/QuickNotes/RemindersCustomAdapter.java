package QuickNotes;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.lang.reflect.Array;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class RemindersCustomAdapter extends BaseAdapter {
    private Context context;
    private ArrayList<String> noteNamesList;
    private ArrayList<String> noteContentList;

    RemindersCustomAdapter(Context context, ArrayList<String> noteNames, ArrayList<String> noteContent) {
        this.context = context;
        this.noteContentList = noteContent;
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
        @SuppressLint("ViewHolder") View view = View.inflate(this.context, R.layout.reminders_list_view, null);
        TextView noteNamesText = view.findViewById(R.id.noteNameText);
        TextView reminderDateText = view.findViewById(R.id.reminderDateText);

        // formatting
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
        String formattedDate = date.substring(0,3) + " " + date.substring(3,6) + " " + date.substring(6,8) + " " + date.substring(8,13) + ":00 " + date.substring(13,16) + " " + date.substring(16,20);


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
        return view;
    }
}

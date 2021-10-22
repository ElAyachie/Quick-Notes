package QuickNotes.Adapters;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.ArrayList;

import QuickNotes.R;

public class NotesCustomAdapter extends BaseAdapter {
    private final Context context;
    private final ArrayList<String> noteNamesList;
    private final ArrayList<String> noteContentList;
    private final ArrayList<String> noteDateList;

    public NotesCustomAdapter(Context context, ArrayList<String> noteNames, ArrayList<String> noteContent, ArrayList<String> noteDate) {
        this.context = context;
        this.noteContentList = noteContent;
        this.noteNamesList = noteNames;
        this.noteDateList = noteDate;
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
            convertView = View.inflate(this.context, R.layout.notes_list_view, null);
            TextView noteNamesText = convertView.findViewById(R.id.noteNameText);
            TextView noteContentText = convertView.findViewById(R.id.noteContentText);
            TextView noteDateText = convertView.findViewById(R.id.noteDateText);

            noteNamesText.setText(noteNamesList.get(position));
            noteContentText.setText(noteContentList.get(position));
            noteDateText.setText(noteDateList.get(position));
            System.out.println("Something date: " + noteDateText.getText().toString());
            if (noteNamesText.getText().length() > 40) {
                noteNamesText.setText(String.format("%s...", noteNamesList.get(position).substring(0, 40)));
            }
            if (noteContentText.getText().length() > 100) {
                noteContentText.setText(String.format("%s...", noteContentList.get(position).substring(0, 100)));
            }
        }
        return convertView;
    }
}

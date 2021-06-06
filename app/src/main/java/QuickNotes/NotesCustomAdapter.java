package QuickNotes;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;


import java.util.ArrayList;

public class NotesCustomAdapter extends BaseAdapter {
    private Context context;
    private ArrayList<String> noteNamesList;
    private ArrayList<String> noteContentList;

    NotesCustomAdapter(Context context, ArrayList<String> noteNames, ArrayList<String> noteContent) {
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
        @SuppressLint("ViewHolder") View view = View.inflate(this.context, R.layout.notes_list_view, null);
        TextView noteNamesText = view.findViewById(R.id.noteNameText);
        TextView noteContentText = view.findViewById(R.id.noteContentText);

        noteNamesText.setText(noteNamesList.get(position));
        noteContentText.setText(noteContentList.get(position));
        if (noteNamesText.getText().length() > 40) {
            noteNamesText.setText(String.format("%s...", noteNamesList.get(position).substring(0, 40)));
        }
        if (noteContentText.getText().length() > 100) {
            noteContentText.setText(String.format("%s...", noteContentList.get(position).substring(0, 100)));
        }
        return view;
    }
}

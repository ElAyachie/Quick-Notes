package QuickNotes.Adapters;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import QuickNotes.Note;
import QuickNotes.R;

public class NotesCustomAdapter extends BaseAdapter {
    private final Context context;
    private List<Note> allNotes;

    public NotesCustomAdapter(Context context, List<Note> allNotes) {
        this.context = context;
        this.allNotes = allNotes;
    }

    @Override
    public int getCount() {
        return allNotes.size();
    }

    @Override
    public Note getItem(int position) {
        return this.allNotes.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            Note note = allNotes.get(position);
            convertView = View.inflate(this.context, R.layout.notes_list_view, null);
            TextView noteNamesText = convertView.findViewById(R.id.noteNameText);
            TextView noteContentText = convertView.findViewById(R.id.noteContentText);
            TextView noteDateText = convertView.findViewById(R.id.noteDateText);

            String noteName = note.getNoteName();
            String noteContent = note.getNoteContent();
            String noteDate = note.getDateCreated();

            noteNamesText.setText(noteName);
            noteContentText.setText(noteContent);
            noteDateText.setText(noteDate);
            if (noteNamesText.getText().length() > 40) {
                noteNamesText.setText(String.format("%s...", noteName.substring(0, 40)));
            }
            if (noteContentText.getText().length() > 100) {
                noteContentText.setText(String.format("%s...", noteContent.substring(0, 100)));
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

package com.myapp.quicknotes.ui.notes;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.myapp.quicknotes.data.Note;
import com.myapp.quicknotes.databinding.ItemNoteBinding;
import com.myapp.quicknotes.ui.common.Formats;

import java.util.function.Consumer;

public class NoteAdapter extends ListAdapter<Note, NoteAdapter.NoteViewHolder> {
    private final Consumer<Note> onClick;
    private final Consumer<Note> onLongClick;

    public NoteAdapter(Consumer<Note> onClick, Consumer<Note> onLongClick) {
        super(DIFF);
        this.onClick = onClick;
        this.onLongClick = onLongClick;
    }

    @NonNull
    @Override
    public NoteViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new NoteViewHolder(ItemNoteBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull NoteViewHolder holder, int position) {
        Note note = getItem(position);
        holder.binding.noteTitle.setText(note.getTitle());
        holder.binding.noteContent.setText(note.getContent());
        holder.binding.noteContent.setVisibility(
                note.getContent().isEmpty() ? View.GONE : View.VISIBLE);
        holder.binding.noteDate.setText(Formats.date(note.getUpdatedAt()));
        holder.itemView.setOnClickListener(view -> onClick.accept(note));
        holder.itemView.setOnLongClickListener(view -> {
            onLongClick.accept(note);
            return true;
        });
    }

    static class NoteViewHolder extends RecyclerView.ViewHolder {
        final ItemNoteBinding binding;

        NoteViewHolder(ItemNoteBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }

    private static final DiffUtil.ItemCallback<Note> DIFF = new DiffUtil.ItemCallback<>() {
        @Override
        public boolean areItemsTheSame(@NonNull Note oldItem, @NonNull Note newItem) {
            return oldItem.getId() == newItem.getId();
        }

        @Override
        public boolean areContentsTheSame(@NonNull Note oldItem, @NonNull Note newItem) {
            return oldItem.equals(newItem);
        }
    };
}

package com.myapp.quicknotes.ui.notes;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.myapp.quicknotes.R;
import com.myapp.quicknotes.data.Note;
import com.myapp.quicknotes.databinding.FragmentNotesBinding;
import com.myapp.quicknotes.ui.common.Screens;
import com.myapp.quicknotes.ui.editor.NoteEditorViewModel;

// The notes inside one folder. Tap opens a note, long press offers to delete it.
public class NotesFragment extends Fragment {
    private FragmentNotesBinding binding;
    private NotesViewModel viewModel;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentNotesBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        viewModel = new ViewModelProvider(this).get(NotesViewModel.class);
        NoteAdapter adapter = new NoteAdapter(note -> openEditor(note.getId()), this::confirmDelete);
        binding.noteList.setAdapter(adapter);
        viewModel.getNotes().observe(getViewLifecycleOwner(), notes -> {
            adapter.submitList(notes);
            binding.emptyState.setVisibility(notes.isEmpty() ? View.VISIBLE : View.GONE);
        });
        binding.newNoteButton.setOnClickListener(button -> openEditor(0));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    // A note id of 0 opens the editor on a new note in this folder.
    private void openEditor(long noteId) {
        Screens.open(this, R.id.notesFragment, R.id.noteEditorFragment,
                NoteEditorViewModel.args(noteId, viewModel.getFolderId()));
    }

    private void confirmDelete(Note note) {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.delete_note_title)
                .setMessage(R.string.delete_note_message)
                .setPositiveButton(R.string.delete, (dialog, which) -> viewModel.deleteNote(note))
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }
}

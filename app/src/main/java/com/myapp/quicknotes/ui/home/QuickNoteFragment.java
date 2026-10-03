package com.myapp.quicknotes.ui.home;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.myapp.quicknotes.R;
import com.myapp.quicknotes.databinding.NoteFormBinding;
import com.myapp.quicknotes.ui.common.FolderPicker;

// The "Make Note" tab: write a note and save it without leaving the start screen. Saving clears
// the form, ready for the next note.
public class QuickNoteFragment extends Fragment {
    private NoteFormBinding form;
    private QuickNoteViewModel viewModel;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        form = NoteFormBinding.inflate(inflater, container, false);
        return form.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        viewModel = new ViewModelProvider(this).get(QuickNoteViewModel.class);
        FolderPicker folderPicker = new FolderPicker(form.folderSpinner,
                viewModel.getSelectedFolderId(), viewModel::setSelectedFolderId);
        viewModel.getFolders().observe(getViewLifecycleOwner(), folderPicker::setFolders);
        form.saveButton.setOnClickListener(button -> save());
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        form = null;
    }

    private void save() {
        String title = String.valueOf(form.titleInput.getText()).trim();
        if (title.isEmpty()) {
            form.titleInput.setError(getString(R.string.error_enter_name));
            return;
        }
        viewModel.saveNote(title, String.valueOf(form.contentInput.getText()), () -> {
            if (form == null) {
                return;
            }
            form.titleInput.setText("");
            form.contentInput.setText("");
            Toast.makeText(requireContext(), R.string.note_saved, Toast.LENGTH_SHORT).show();
        });
    }
}

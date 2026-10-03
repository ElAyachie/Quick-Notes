package com.myapp.quicknotes.ui.home;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.myapp.quicknotes.R;
import com.myapp.quicknotes.data.Folder;
import com.myapp.quicknotes.databinding.DialogNewFolderBinding;
import com.myapp.quicknotes.databinding.FragmentFoldersBinding;
import com.myapp.quicknotes.ui.common.Screens;
import com.myapp.quicknotes.ui.notes.NotesViewModel;

// The "Collection" tab: every folder with its number of notes, and a button to add one. Tap opens a folder, long press
// offers to delete it.
public class FoldersFragment extends Fragment {
    private FragmentFoldersBinding binding;
    private FoldersViewModel viewModel;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentFoldersBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        viewModel = new ViewModelProvider(this).get(FoldersViewModel.class);
        FolderAdapter adapter = new FolderAdapter(this::openFolder, this::confirmDelete);
        binding.folderList.setAdapter(adapter);
        viewModel.getFolders().observe(getViewLifecycleOwner(), adapter::submitList);
        binding.newFolderButton.setOnClickListener(button -> askForNewFolderName());
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    private void openFolder(Folder folder) {
        Screens.open(this, R.id.homeFragment, R.id.notesFragment,
                NotesViewModel.args(folder.getId(), folder.getName()));
    }

    private void confirmDelete(Folder folder) {
        if (folder.isDefault()) {
            Toast.makeText(requireContext(), R.string.default_folder_cannot_be_deleted,
                    Toast.LENGTH_LONG).show();
            return;
        }
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(getString(R.string.delete_folder_title, folder.getName()))
                .setMessage(R.string.delete_folder_message)
                .setPositiveButton(R.string.delete, (dialog, which) -> viewModel.deleteFolder(folder))
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void askForNewFolderName() {
        DialogNewFolderBinding form = DialogNewFolderBinding.inflate(getLayoutInflater());
        AlertDialog dialog = new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.new_folder_title)
                .setView(form.getRoot())
                .setPositiveButton(R.string.create, null)
                .setNegativeButton(android.R.string.cancel, null)
                .create();
        // The button is wired up after the dialog is shown so that a rejected name keeps it open.
        dialog.setOnShowListener(shown ->
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(button -> {
                    String name = String.valueOf(form.folderNameInput.getText()).trim();
                    if (name.isEmpty()) {
                        form.folderNameLayout.setError(getString(R.string.error_enter_name));
                        return;
                    }
                    viewModel.createFolder(name, created -> {
                        if (created) {
                            dialog.dismiss();
                        } else {
                            form.folderNameLayout.setError(getString(R.string.error_folder_exists));
                        }
                    });
                }));
        if (dialog.getWindow() != null) {
            dialog.getWindow().setSoftInputMode(
                    WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
        }
        form.folderNameInput.requestFocus();
        dialog.show();
    }
}

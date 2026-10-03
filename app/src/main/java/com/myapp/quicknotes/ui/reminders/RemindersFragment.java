package com.myapp.quicknotes.ui.reminders;

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
import com.myapp.quicknotes.data.Folder;
import com.myapp.quicknotes.data.ReminderWithNote;
import com.myapp.quicknotes.databinding.FragmentRemindersBinding;
import com.myapp.quicknotes.ui.common.Screens;
import com.myapp.quicknotes.ui.editor.NoteEditorViewModel;

// The reminders that have not fired yet, soonest first. Tap opens the note, long press offers to
// delete the reminder.
public class RemindersFragment extends Fragment {
    private FragmentRemindersBinding binding;
    private RemindersViewModel viewModel;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentRemindersBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        viewModel = new ViewModelProvider(this).get(RemindersViewModel.class);
        ReminderAdapter adapter = new ReminderAdapter(this::openNote, this::confirmDelete);
        binding.reminderList.setAdapter(adapter);
        viewModel.getReminders().observe(getViewLifecycleOwner(), reminders -> {
            adapter.submitList(reminders);
            binding.emptyText.setVisibility(reminders.isEmpty() ? View.VISIBLE : View.GONE);
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    private void openNote(ReminderWithNote item) {
        // The folder argument only matters for new notes; the editor reads this note's own folder.
        Screens.open(this, R.id.remindersFragment, R.id.noteEditorFragment,
                NoteEditorViewModel.args(item.getReminder().getNoteId(), Folder.DEFAULT_ID));
    }

    private void confirmDelete(ReminderWithNote item) {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.delete_reminder_title)
                .setMessage(getString(R.string.delete_reminder_message, item.getNoteTitle()))
                .setPositiveButton(R.string.delete,
                        (dialog, which) -> viewModel.deleteReminder(item.getReminder()))
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }
}

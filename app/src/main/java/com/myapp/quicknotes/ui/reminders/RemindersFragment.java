package com.myapp.quicknotes.ui.reminders;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.tabs.TabLayout;
import com.myapp.quicknotes.R;
import com.myapp.quicknotes.data.Folder;
import com.myapp.quicknotes.data.Reminder;
import com.myapp.quicknotes.data.ReminderType;
import com.myapp.quicknotes.data.ReminderWithNote;
import com.myapp.quicknotes.databinding.FragmentRemindersBinding;
import com.myapp.quicknotes.ui.common.ReminderTimePicker;
import com.myapp.quicknotes.ui.common.Screens;
import com.myapp.quicknotes.ui.editor.NoteEditorViewModel;
import com.myapp.quicknotes.ui.placepicker.PlacePickerViewModel;

import java.util.Objects;

// Two lists: "Upcoming" holds the reminders still waiting to fire (time reminders soonest first,
// then location reminders); "History" holds the ones that have fired, most recent first.
// Tapping a waiting reminder offers to edit it, open its note or delete it; tapping a history
// entry opens its note. Long press offers to delete either.
public class RemindersFragment extends Fragment {
    private static final int TAB_UPCOMING = 0;
    private static final int TAB_HISTORY = 1;

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

        binding.tabs.addTab(binding.tabs.newTab().setText(R.string.tab_upcoming));
        binding.tabs.addTab(binding.tabs.newTab().setText(R.string.tab_history));
        binding.tabs.selectTab(binding.tabs.getTabAt(
                viewModel.isShowingHistory() ? TAB_HISTORY : TAB_UPCOMING));
        binding.tabs.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                viewModel.setShowingHistory(tab.getPosition() == TAB_HISTORY);
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
            }
        });

        ReminderAdapter adapter = new ReminderAdapter(this::onReminderTapped, this::confirmDelete);
        binding.reminderList.setAdapter(adapter);
        viewModel.getReminders().observe(getViewLifecycleOwner(), reminders -> {
            adapter.submitList(reminders);
            binding.emptyText.setText(viewModel.isShowingHistory()
                    ? R.string.no_history : R.string.no_reminders);
            binding.emptyText.setVisibility(reminders.isEmpty() ? View.VISIBLE : View.GONE);
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    private void onReminderTapped(ReminderWithNote item) {
        if (item.getReminder().hasFired()) {
            openNote(item);
            return;
        }
        String[] actions = {
                getString(R.string.edit_reminder),
                getString(R.string.open_note),
                getString(R.string.delete)};
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(item.getNoteTitle())
                .setItems(actions, (dialog, which) -> {
                    if (which == 0) {
                        edit(item.getReminder());
                    } else if (which == 1) {
                        openNote(item);
                    } else {
                        confirmDelete(item);
                    }
                })
                .show();
    }

    // A time reminder is changed through the same three questions that set it; a location
    // reminder goes back to the map.
    private void edit(Reminder reminder) {
        if (reminder.getType() == ReminderType.TIME) {
            ReminderTimePicker.show(requireContext(),
                    Objects.requireNonNull(reminder.getTriggerAt()), reminder.getRepeat(),
                    (triggerAt, repeat) -> {
                        viewModel.rescheduleReminder(reminder, triggerAt, repeat);
                        Toast.makeText(requireContext(), R.string.reminder_updated,
                                Toast.LENGTH_SHORT).show();
                    });
        } else {
            Screens.open(this, R.id.remindersFragment, R.id.placePickerFragment,
                    PlacePickerViewModel.argsForEditing(reminder));
        }
    }

    private void openNote(ReminderWithNote item) {
        // The folder argument only matters for new notes; the editor reads this note's own folder.
        Screens.open(this, R.id.remindersFragment, R.id.noteEditorFragment,
                NoteEditorViewModel.args(item.getReminder().getNoteId(), Folder.DEFAULT_ID));
    }

    private void confirmDelete(ReminderWithNote item) {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(item.getReminder().hasFired()
                        ? R.string.remove_from_history_title : R.string.delete_reminder_title)
                .setMessage(getString(R.string.delete_reminder_message, item.getNoteTitle()))
                .setPositiveButton(R.string.delete,
                        (dialog, which) -> viewModel.deleteReminder(item.getReminder()))
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }
}

package com.myapp.quicknotes.ui.editor;

import android.Manifest;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.MenuProvider;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.myapp.quicknotes.R;
import com.myapp.quicknotes.data.Note;
import com.myapp.quicknotes.data.ReminderType;
import com.myapp.quicknotes.data.Repeat;
import com.myapp.quicknotes.databinding.NoteFormBinding;
import com.myapp.quicknotes.ui.common.FolderPicker;
import com.myapp.quicknotes.ui.common.Formats;
import com.myapp.quicknotes.ui.common.ReminderText;
import com.myapp.quicknotes.ui.common.ReminderTimePicker;
import com.myapp.quicknotes.ui.common.Screens;
import com.myapp.quicknotes.ui.placepicker.LocationPermissionRequest;
import com.myapp.quicknotes.ui.placepicker.PlacePickerViewModel;

// Edits one note: its title, folder and text. Also where a reminder is attached to the note.
public class NoteEditorFragment extends Fragment {
    private NoteFormBinding form;
    private NoteEditorViewModel viewModel;
    private FolderPicker folderPicker;

    // Asked just before the first reminder is set. The reminder is set either way; without the
    // permission it simply stays silent, and the user is told so.
    private final ActivityResultLauncher<String> notificationPermission =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                if (!granted) {
                    Toast.makeText(requireContext(), R.string.notifications_denied,
                            Toast.LENGTH_LONG).show();
                }
                chooseWhenOrWhere();
            });

    private final LocationPermissionRequest locationPermission =
            new LocationPermissionRequest(this, this::openPlacePicker);

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        form = NoteFormBinding.inflate(inflater, container, false);
        return form.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        viewModel = new ViewModelProvider(this).get(NoteEditorViewModel.class);
        folderPicker = new FolderPicker(form.folderSpinner, viewModel.getSelectedFolderId(),
                viewModel::setSelectedFolderId);
        viewModel.getFolders().observe(getViewLifecycleOwner(), folderPicker::setFolders);
        viewModel.getNote().observe(getViewLifecycleOwner(), this::showNote);
        viewModel.isNoteMissing().observe(getViewLifecycleOwner(), missing -> {
            if (missing) {
                Toast.makeText(requireContext(), R.string.note_no_longer_exists,
                        Toast.LENGTH_LONG).show();
                close();
            }
        });
        form.saveButton.setOnClickListener(button -> save(null));

        requireActivity().addMenuProvider(new EditorMenu(), getViewLifecycleOwner(),
                Lifecycle.State.RESUMED);
        requireActivity().getOnBackPressedDispatcher().addCallback(getViewLifecycleOwner(),
                new OnBackPressedCallback(true) {
                    @Override
                    public void handleOnBackPressed() {
                        leave();
                    }
                });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        form = null;
        folderPicker = null;
    }

    private void showNote(Note note) {
        if (!viewModel.isFormFilled()) {
            form.titleInput.setText(note.getTitle());
            form.contentInput.setText(note.getContent());
            folderPicker.select(viewModel.getSelectedFolderId());
            viewModel.markFormFilled();
        }
        if (note.isNew()) {
            form.dateText.setVisibility(View.GONE);
        } else {
            form.dateText.setText(getString(R.string.last_edited, Formats.date(note.getUpdatedAt())));
            form.dateText.setVisibility(View.VISIBLE);
        }
        // "Delete note" only makes sense once the note is stored.
        requireActivity().invalidateMenu();
    }

    private String title() {
        return String.valueOf(form.titleInput.getText()).trim();
    }

    private String content() {
        return String.valueOf(form.contentInput.getText());
    }

    // Saves the note and then runs `afterwards`, unless the title is missing.
    private void save(@Nullable Runnable afterwards) {
        if (title().isEmpty()) {
            form.titleInput.setError(getString(R.string.error_enter_name));
            return;
        }
        viewModel.save(title(), content(), () -> {
            if (form == null) {
                return;
            }
            Toast.makeText(requireContext(), R.string.note_saved, Toast.LENGTH_SHORT).show();
            if (afterwards != null) {
                afterwards.run();
            }
        });
    }

    // Back and the toolbar arrow both end up here.
    private void leave() {
        if (!viewModel.hasUnsavedChanges(title(), content())) {
            close();
            return;
        }
        new MaterialAlertDialogBuilder(requireContext())
                .setMessage(R.string.save_changes_question)
                .setPositiveButton(R.string.save, (dialog, which) -> save(this::close))
                .setNegativeButton(R.string.discard, (dialog, which) -> close())
                .setNeutralButton(android.R.string.cancel, null)
                .show();
    }

    private void close() {
        Screens.hideKeyboard(form.getRoot());
        NavHostFragment.findNavController(this).popBackStack();
    }

    private void confirmDelete() {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.delete_note_title)
                .setMessage(R.string.delete_note_message)
                .setPositiveButton(R.string.delete, (dialog, which) -> {
                    viewModel.delete();
                    close();
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    // A reminder belongs to a stored note, so the note is saved first when it needs to be.
    private void startSettingReminder(ReminderType type) {
        viewModel.setPendingReminderType(type);
        if (viewModel.isNewNote() || viewModel.hasUnsavedChanges(title(), content())) {
            save(this::askForNotificationPermission);
        } else {
            askForNotificationPermission();
        }
    }

    private void askForNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && !viewModel.canShowNotifications()) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS);
        } else {
            chooseWhenOrWhere();
        }
    }

    private void chooseWhenOrWhere() {
        if (viewModel.getPendingReminderType() == ReminderType.LOCATION) {
            locationPermission.start();
        } else {
            ReminderTimePicker.show(requireContext(), System.currentTimeMillis(), Repeat.NONE,
                    this::setReminder);
        }
    }

    private void openPlacePicker() {
        Screens.open(this, R.id.noteEditorFragment, R.id.placePickerFragment,
                PlacePickerViewModel.argsForNewReminder(viewModel.getNoteId()));
    }

    private void setReminder(long triggerAt, Repeat repeat) {
        viewModel.addTimeReminder(triggerAt, repeat);
        String message = repeat == Repeat.NONE
                ? getString(R.string.reminder_set, Formats.dateTime(triggerAt))
                : getString(R.string.reminder_set_repeating, Formats.dateTime(triggerAt),
                        getString(ReminderText.repeat(repeat)));
        if (viewModel.canScheduleExactAlarms()) {
            Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show();
        } else {
            offerExactAlarms(message);
        }
    }

    // Without the "Alarms & reminders" permission the system may deliver a reminder up to an hour
    // late. The reminder is set regardless; this offers the way to make it punctual.
    private void offerExactAlarms(String message) {
        Snackbar.make(form.getRoot(),
                        message + " " + getString(R.string.reminder_may_be_late),
                        Snackbar.LENGTH_LONG)
                .setAction(R.string.allow, button -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        startActivity(new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                                Uri.parse("package:" + requireContext().getPackageName())));
                    }
                })
                .show();
    }

    private class EditorMenu implements MenuProvider {
        @Override
        public void onCreateMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
            inflater.inflate(R.menu.note_editor, menu);
        }

        @Override
        public void onPrepareMenu(@NonNull Menu menu) {
            menu.findItem(R.id.action_delete_note).setVisible(!viewModel.isNewNote());
        }

        @Override
        public boolean onMenuItemSelected(@NonNull MenuItem item) {
            if (item.getItemId() == R.id.action_set_time_reminder) {
                startSettingReminder(ReminderType.TIME);
                return true;
            }
            if (item.getItemId() == R.id.action_set_location_reminder) {
                startSettingReminder(ReminderType.LOCATION);
                return true;
            }
            if (item.getItemId() == R.id.action_delete_note) {
                confirmDelete();
                return true;
            }
            return false;
        }
    }
}

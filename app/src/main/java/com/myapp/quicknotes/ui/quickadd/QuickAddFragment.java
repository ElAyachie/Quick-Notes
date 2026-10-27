package com.myapp.quicknotes.ui.quickadd;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavOptions;
import androidx.navigation.fragment.NavHostFragment;

import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.model.LatLng;
import com.myapp.quicknotes.R;
import com.myapp.quicknotes.command.Command;
import com.myapp.quicknotes.data.PlaceTrigger;
import com.myapp.quicknotes.data.Repeat;
import com.myapp.quicknotes.databinding.FragmentQuickAddBinding;
import com.myapp.quicknotes.places.PlaceSuggestion;
import com.myapp.quicknotes.places.SearchArea;
import com.myapp.quicknotes.reminders.LocationAccess;
import com.myapp.quicknotes.ui.common.Formats;
import com.myapp.quicknotes.ui.common.ReminderText;
import com.myapp.quicknotes.ui.common.ReminderTimePicker;
import com.myapp.quicknotes.ui.common.Screens;
import com.myapp.quicknotes.ui.placepicker.LocationPermissionRequest;
import com.myapp.quicknotes.ui.placepicker.PlacePickerViewModel;

import java.util.ArrayList;

// "Remind me to buy milk when I get to Walgreens": the user says or types what to remember, the
// screen shows what it understood (the note, and when or where it will be reminded of), and
// nothing is saved until they confirm. A place is found the way the place picker's search finds
// it; when it can't be, or the user wants another, the place picker opens for the new note.
// Opened from the microphone in the toolbar, it starts listening straight away.
public class QuickAddFragment extends Fragment {
    private static final String ARG_START_VOICE = "startVoice";
    // A place search prefers this much around the user.
    private static final double NEARBY_METERS = 20_000;

    private FragmentQuickAddBinding binding;
    private QuickAddViewModel viewModel;
    @Nullable
    private LatLng userLocation;
    // The understood request whose text is in the note field, so finding its place later
    // doesn't overwrite what the user corrected.
    @Nullable
    private Command shownCommand;
    // What to do once notifications have been allowed or refused.
    @Nullable
    private Runnable afterNotificationPermission;

    private final ActivityResultLauncher<Intent> speech = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(), result -> {
                if (binding == null || result.getResultCode() != Activity.RESULT_OK
                        || result.getData() == null) {
                    return;
                }
                ArrayList<String> heard = result.getData()
                        .getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                if (heard != null && !heard.isEmpty()) {
                    binding.requestInput.setText(heard.get(0));
                    read();
                }
            });
    private final ActivityResultLauncher<String> notificationPermission =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                if (!granted) {
                    Toast.makeText(requireContext(), R.string.notifications_denied,
                            Toast.LENGTH_LONG).show();
                }
                Runnable next = afterNotificationPermission;
                afterNotificationPermission = null;
                if (next != null) {
                    next.run();
                }
            });
    private final LocationPermissionRequest locationForReminder =
            new LocationPermissionRequest(this, this::setLocationReminder);
    private final LocationPermissionRequest locationForMap =
            new LocationPermissionRequest(this, this::openPlacePicker);

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentQuickAddBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        viewModel = new ViewModelProvider(this).get(QuickAddViewModel.class);
        binding.requestLayout.setEndIconOnClickListener(button -> listen());
        // One line as far as the keyboard is concerned, so Enter reads the request, but long
        // requests wrap.
        binding.requestInput.setHorizontallyScrolling(false);
        binding.requestInput.setMaxLines(4);
        binding.readButton.setOnClickListener(button -> read());
        binding.requestInput.setOnEditorActionListener((input, action, event) -> {
            if (action == EditorInfo.IME_ACTION_GO
                    || (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER
                    && event.getAction() == KeyEvent.ACTION_DOWN)) {
                read();
                return true;
            }
            return false;
        });
        viewModel.getState().observe(getViewLifecycleOwner(), this::show);
        findUser();
        if (savedInstanceState == null) {
            if (requireArguments().getBoolean(ARG_START_VOICE, true)) {
                listen();
            } else {
                showKeyboard();
            }
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    private void listen() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
                .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                        RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                .putExtra(RecognizerIntent.EXTRA_PROMPT, getString(R.string.quick_add_listening));
        try {
            speech.launch(intent);
        } catch (ActivityNotFoundException e) {
            // No speech recognizer on this device; typing still works.
            Toast.makeText(requireContext(), R.string.voice_unavailable, Toast.LENGTH_LONG)
                    .show();
            showKeyboard();
        }
    }

    private void showKeyboard() {
        binding.requestInput.requestFocus();
        binding.requestInput.post(() -> {
            if (binding != null) {
                InputMethodManager keyboard = (InputMethodManager) requireContext()
                        .getSystemService(Context.INPUT_METHOD_SERVICE);
                keyboard.showSoftInput(binding.requestInput, InputMethodManager.SHOW_IMPLICIT);
            }
        });
    }

    // Places are searched for near the user, and distances measured from them.
    @SuppressLint("MissingPermission")
    private void findUser() {
        if (!LocationAccess.hasPreciseLocation(requireContext())) {
            return;
        }
        LocationServices.getFusedLocationProviderClient(requireContext()).getLastLocation()
                .addOnSuccessListener(location -> {
                    if (location != null) {
                        userLocation = new LatLng(location.getLatitude(),
                                location.getLongitude());
                    }
                });
    }

    private SearchArea searchArea() {
        if (userLocation == null) {
            return new SearchArea(null, null, SearchArea.MAX_RADIUS_METERS, null, null);
        }
        return new SearchArea(userLocation.latitude, userLocation.longitude, NEARBY_METERS,
                userLocation.latitude, userLocation.longitude);
    }

    private void read() {
        String request = String.valueOf(binding.requestInput.getText()).trim();
        if (request.isEmpty()) {
            return;
        }
        Screens.hideKeyboard(binding.getRoot());
        viewModel.interpret(request, searchArea());
    }

    // Shows what was understood, and offers what can be done with it.
    private void show(QuickAddViewModel.State state) {
        Command command = state.command;
        binding.resultCard.setVisibility(command != null ? View.VISIBLE : View.GONE);
        binding.examples.setVisibility(command != null ? View.GONE : View.VISIBLE);
        if (command == null) {
            return;
        }
        if (command != shownCommand) {
            shownCommand = command;
            binding.noteInput.setText(command.text);
        }
        binding.locating.setVisibility(state.locating ? View.VISIBLE : View.GONE);
        binding.confirmButton.setEnabled(!state.locating);
        String detail = null;
        switch (command.kind) {
            case NOTE:
                binding.triggerIcon.setImageResource(R.drawable.ic_note);
                binding.triggerText.setText(R.string.quick_add_note_only);
                binding.confirmButton.setText(R.string.quick_add_save_note);
                binding.confirmButton.setOnClickListener(button -> saveNote());
                showChange(R.string.quick_add_add_time, this::pickTime);
                break;
            case TIME:
                binding.triggerIcon.setImageResource(R.drawable.ic_schedule);
                if (command.triggerAt == null) {
                    binding.triggerText.setText(R.string.quick_add_no_time);
                    binding.confirmButton.setText(R.string.quick_add_pick_time);
                    binding.confirmButton.setOnClickListener(button -> pickTime());
                    showChange(0, null);
                } else {
                    long triggerAt = command.triggerAt;
                    binding.triggerText.setText(Formats.dateTime(triggerAt));
                    if (command.repeat != Repeat.NONE) {
                        detail = getString(R.string.quick_add_repeats, ReminderText.repeatLabel(
                                requireContext(), command.repeat, command.repeatDays));
                    }
                    binding.confirmButton.setText(R.string.set_reminder);
                    binding.confirmButton.setOnClickListener(button -> setTimeReminder(
                            triggerAt, command.repeat, command.repeatDays));
                    showChange(R.string.quick_add_change_time, () -> ReminderTimePicker.show(
                            requireContext(), triggerAt, command.repeat, command.repeatDays,
                            this::setTimeReminder));
                }
                break;
            default:
                binding.triggerIcon.setImageResource(R.drawable.ic_place);
                showPlace(state, command);
                detail = placeDetail(state.place, command);
                if (state.placeNotFound && state.personal) {
                    detail = getString(R.string.quick_add_personal_place_hint);
                }
                break;
        }
        binding.triggerDetail.setText(detail);
        binding.triggerDetail.setVisibility(detail != null ? View.VISIBLE : View.GONE);
    }

    private void showPlace(QuickAddViewModel.State state, Command command) {
        boolean leaving = command.placeTrigger == PlaceTrigger.LEAVING;
        if (state.place != null) {
            binding.triggerText.setText(getString(leaving ? R.string.quick_add_when_leaving
                    : R.string.quick_add_when_arriving, state.place.getName()));
            binding.confirmButton.setText(R.string.set_reminder);
            binding.confirmButton.setOnClickListener(button -> askThenSetLocationReminder());
            showChange(R.string.quick_add_change_place, this::askThenOpenPlacePicker);
            return;
        }
        if (state.locating) {
            binding.triggerText.setText(getString(R.string.quick_add_finding, command.place));
        } else if (state.placeNotFound && state.personal) {
            binding.triggerText.setText(getString(R.string.quick_add_place_not_saved,
                    command.place));
        } else if (state.placeNotFound) {
            binding.triggerText.setText(getString(R.string.quick_add_place_not_found,
                    command.place));
        } else {
            binding.triggerText.setText(R.string.quick_add_no_place);
        }
        binding.confirmButton.setText(R.string.quick_add_pick_on_map);
        binding.confirmButton.setOnClickListener(button -> askThenOpenPlacePicker());
        showChange(0, null);
    }

    // The address and distance of the place found, and whether it reminds every time.
    @Nullable
    private String placeDetail(@Nullable PlaceSuggestion place, Command command) {
        if (place == null) {
            return null;
        }
        StringBuilder detail = new StringBuilder();
        if (place.getAddress() != null) {
            detail.append(place.getAddress());
        }
        if (place.getDistanceMeters() != null) {
            if (detail.length() > 0) {
                detail.append(" · ");
            }
            detail.append(Formats.distance(requireContext(), place.getDistanceMeters()));
        }
        if (command.repeat == Repeat.EVERY_ARRIVAL) {
            if (detail.length() > 0) {
                detail.append(" · ");
            }
            detail.append(getString(R.string.quick_add_every_time));
        }
        return detail.length() > 0 ? detail.toString() : null;
    }

    private void showChange(int label, @Nullable Runnable action) {
        binding.changeButton.setVisibility(action != null ? View.VISIBLE : View.GONE);
        if (action != null) {
            binding.changeButton.setText(label);
            binding.changeButton.setOnClickListener(button -> action.run());
        }
    }

    // The note's text as corrected; something to show in the list if it was left empty.
    private String noteText() {
        String text = String.valueOf(binding.noteInput.getText()).trim();
        if (!text.isEmpty()) {
            return text;
        }
        QuickAddViewModel.State state = viewModel.getState().getValue();
        if (state != null && state.place != null) {
            return state.place.getName();
        }
        return getString(R.string.quick_add_untitled);
    }

    private void saveNote() {
        viewModel.saveNote(noteText(), noteId -> finish(getString(R.string.note_saved)));
    }

    private void pickTime() {
        ReminderTimePicker.showForNewReminder(requireContext(), this::setTimeReminder);
    }

    private void setTimeReminder(long triggerAt, Repeat repeat, int repeatDays) {
        String text = noteText();
        whenNotificationsAsked(() -> viewModel.setTimeReminder(text, triggerAt, repeat,
                repeatDays, () -> {
                    String message = repeat == Repeat.NONE
                            ? getString(R.string.reminder_set, Formats.dateTime(triggerAt))
                            : getString(R.string.reminder_set_repeating,
                            Formats.dateTime(triggerAt),
                            ReminderText.repeatLabel(requireContext(), repeat, repeatDays));
                    if (!viewModel.canScheduleExactAlarms()) {
                        message += " " + getString(R.string.reminder_may_be_late);
                    }
                    finish(message);
                }));
    }

    private void askThenSetLocationReminder() {
        whenNotificationsAsked(locationForReminder::start);
    }

    private void setLocationReminder() {
        QuickAddViewModel.State state = viewModel.getState().getValue();
        if (binding == null || state == null || state.command == null) {
            return;
        }
        boolean leaving = state.command.placeTrigger == PlaceTrigger.LEAVING;
        viewModel.setLocationReminder(noteText(), () -> {
            int message;
            if (!LocationAccess.isLocationOn(requireContext())) {
                message = R.string.location_reminder_set_but_location_off;
            } else if (leaving) {
                message = R.string.location_reminder_set_leaving;
            } else {
                message = R.string.location_reminder_set;
            }
            finish(getString(message));
        });
    }

    private void askThenOpenPlacePicker() {
        whenNotificationsAsked(locationForMap::start);
    }

    // Saves the note, then lets the user choose its place on the map. This screen is done then:
    // going back from the map returns to where the user was before.
    private void openPlacePicker() {
        if (binding == null) {
            return;
        }
        viewModel.saveNote(noteText(), noteId -> {
            if (binding == null) {
                return;
            }
            NavHostFragment.findNavController(this).navigate(R.id.placePickerFragment,
                    PlacePickerViewModel.argsForNewReminder(noteId),
                    new NavOptions.Builder()
                            .setPopUpTo(R.id.quickAddFragment, true)
                            .build());
        });
    }

    // A reminder can't be shown without notifications; Android 13 and later ask first.
    private void whenNotificationsAsked(Runnable next) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && !viewModel.canShowNotifications()) {
            afterNotificationPermission = next;
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS);
        } else {
            next.run();
        }
    }

    private void finish(String message) {
        Context context = getContext();
        if (context == null) {
            return;
        }
        Toast.makeText(context, message, Toast.LENGTH_LONG).show();
        if (binding != null) {
            Screens.hideKeyboard(binding.getRoot());
            NavHostFragment.findNavController(this).popBackStack();
        }
    }
}

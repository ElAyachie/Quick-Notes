package com.myapp.quicknotes.ui.placepicker;

import android.app.Application;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.SavedStateHandle;

import com.google.android.gms.maps.model.LatLng;
import com.myapp.quicknotes.QuickNotesApp;
import com.myapp.quicknotes.data.Reminder;
import com.myapp.quicknotes.data.ReminderRepository;
import com.myapp.quicknotes.data.ReminderType;
import com.myapp.quicknotes.data.Repeat;

public class PlacePickerViewModel extends AndroidViewModel {
    // Navigation arguments; the names match nav_graph.xml. With a reminder id the picker changes
    // that reminder; without one it adds a new reminder to the note.
    private static final String ARG_NOTE_ID = "noteId";
    private static final String ARG_REMINDER_ID = "reminderId";
    private static final String STATE_PLACE = "place";
    private static final String STATE_RADIUS = "radiusMeters";
    private static final float DEFAULT_RADIUS_METERS = 200;

    private final ReminderRepository reminders;
    private final SavedStateHandle state;
    // The reminder being changed, once loaded. Stays empty when adding a new one.
    private final MutableLiveData<Reminder> editedReminder = new MutableLiveData<>();

    public PlacePickerViewModel(@NonNull Application application,
                                @NonNull SavedStateHandle state) {
        super(application);
        this.reminders = QuickNotesApp.container(application).reminderRepository();
        this.state = state;
        if (isEditing()) {
            reminders.loadReminder(longArg(ARG_REMINDER_ID), reminder -> {
                if (reminder == null || reminder.getType() != ReminderType.LOCATION
                        || reminder.hasFired()) {
                    return;
                }
                // Start from the stored place, unless the user already moved the pin before the
                // app was killed in the background and restored.
                if (getPlace() == null) {
                    setPlace(new LatLng(reminder.getLatitude(), reminder.getLongitude()));
                    setRadiusMeters(reminder.getRadiusMeters());
                }
                editedReminder.setValue(reminder);
            });
        }
    }

    public static Bundle argsForNewReminder(long noteId) {
        Bundle args = new Bundle();
        args.putLong(ARG_NOTE_ID, noteId);
        return args;
    }

    public static Bundle argsForEditing(Reminder reminder) {
        Bundle args = argsForNewReminder(reminder.getNoteId());
        args.putLong(ARG_REMINDER_ID, reminder.getId());
        return args;
    }

    public boolean isEditing() {
        return longArg(ARG_REMINDER_ID) != 0;
    }

    public LiveData<Reminder> getEditedReminder() {
        return editedReminder;
    }

    // The place tapped on the map, or null while none has been chosen.
    @Nullable
    public LatLng getPlace() {
        return state.get(STATE_PLACE);
    }

    public void setPlace(LatLng place) {
        state.set(STATE_PLACE, place);
    }

    public float getRadiusMeters() {
        Float radius = state.get(STATE_RADIUS);
        return radius != null ? radius : DEFAULT_RADIUS_METERS;
    }

    public void setRadiusMeters(float radiusMeters) {
        state.set(STATE_RADIUS, radiusMeters);
    }

    // Stores the chosen place: as a change to the reminder being edited, or as a new reminder.
    // Returns false when there is nothing to store yet.
    public boolean save(@Nullable String placeName, boolean everyArrival) {
        LatLng place = getPlace();
        if (place == null) {
            return false;
        }
        Repeat repeat = everyArrival ? Repeat.EVERY_ARRIVAL : Repeat.NONE;
        if (!isEditing()) {
            reminders.addLocationReminder(longArg(ARG_NOTE_ID), place.latitude, place.longitude,
                    getRadiusMeters(), placeName, repeat);
            return true;
        }
        Reminder edited = editedReminder.getValue();
        if (edited == null) {
            return false;
        }
        reminders.updateReminder(edited.moved(place.latitude, place.longitude,
                getRadiusMeters(), placeName, repeat));
        return true;
    }

    private long longArg(String key) {
        Long value = state.get(key);
        return value != null ? value : 0;
    }
}

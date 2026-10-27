package com.myapp.quicknotes.ui.quickadd;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.myapp.quicknotes.AppContainer;
import com.myapp.quicknotes.QuickNotesApp;
import com.myapp.quicknotes.command.Command;
import com.myapp.quicknotes.command.CommandReader;
import com.myapp.quicknotes.command.RuleCommandReader;
import com.myapp.quicknotes.data.Folder;
import com.myapp.quicknotes.data.Note;
import com.myapp.quicknotes.data.NoteRepository;
import com.myapp.quicknotes.data.RecentPlace;
import com.myapp.quicknotes.data.ReminderRepository;
import com.myapp.quicknotes.data.Repeat;
import com.myapp.quicknotes.data.SavedPlace;
import com.myapp.quicknotes.data.SavedPlaceRepository;
import com.myapp.quicknotes.places.PlaceSearchRepository;
import com.myapp.quicknotes.places.PlaceSuggestion;
import com.myapp.quicknotes.places.SearchArea;
import com.myapp.quicknotes.reminders.ReminderNotifier;
import com.myapp.quicknotes.reminders.ReminderScheduler;

import java.time.ZoneId;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Consumer;

public class QuickAddViewModel extends AndroidViewModel {
    // A new location reminder covers this much, as one set on the map does unless changed.
    private static final float RADIUS_METERS = 200;
    private static final int RECENT_PLACES = 20;
    // Places only the user can say where they are (as said, without "the" or "my"). Searching
    // for "home" finds someone else's.
    private static final Set<String> PERSONAL_PLACES = new HashSet<>(Arrays.asList(
            "home", "work", "office", "school", "place", "house"));

    // What the screen shows: what was understood and, for a place, where it was found.
    public static final class State {
        @Nullable
        public final Command command;
        // The place the reminder will be set at, once found.
        @Nullable
        public final PlaceSuggestion place;
        public final boolean locating;
        // The place was said but couldn't be found; `personal` when it is one the user has to
        // show on the map (home, work) because it isn't saved.
        public final boolean placeNotFound;
        public final boolean personal;

        State(@Nullable Command command, @Nullable PlaceSuggestion place, boolean locating,
              boolean placeNotFound, boolean personal) {
            this.command = command;
            this.place = place;
            this.locating = locating;
            this.placeNotFound = placeNotFound;
            this.personal = personal;
        }
    }

    private final CommandReader reader = new RuleCommandReader();
    private final NoteRepository notes;
    private final ReminderRepository reminders;
    private final SavedPlaceRepository savedPlaces;
    private final PlaceSearchRepository placeSearch;
    private final ReminderScheduler scheduler;
    private final ReminderNotifier notifier;
    private final MutableLiveData<State> state =
            new MutableLiveData<>(new State(null, null, false, false, false));
    // Bumped for every request, so the answer to an earlier one is dropped.
    private int generation;

    public QuickAddViewModel(@NonNull Application application) {
        super(application);
        AppContainer container = QuickNotesApp.container(application);
        notes = container.noteRepository();
        reminders = container.reminderRepository();
        savedPlaces = container.savedPlaceRepository();
        placeSearch = container.placeSearchRepository();
        scheduler = container.reminderScheduler();
        notifier = container.reminderNotifier();
    }

    public LiveData<State> getState() {
        return state;
    }

    // Reads the request and, if it names a place, finds it: a saved place by its name, then a
    // place a reminder was set at before, then a search nearest the user.
    public void interpret(String request, SearchArea area) {
        int current = ++generation;
        Command command = reader.read(request, System.currentTimeMillis(), ZoneId.systemDefault());
        if (command.kind != Command.Kind.LOCATION || command.place == null) {
            state.setValue(new State(command, null, false, false, false));
            return;
        }
        state.setValue(new State(command, null, true, false, false));
        String said = normalized(command.place);
        savedPlaces.loadAll(saved -> {
            for (SavedPlace place : saved) {
                if (normalized(place.getName()).equals(said)) {
                    found(current, command, PlaceSuggestion.saved(place.getId(), place.getName(),
                            place.getLatitude(), place.getLongitude(),
                            area.distanceTo(place.getLatitude(), place.getLongitude())));
                    return;
                }
            }
            reminders.loadRecentPlaces(RECENT_PLACES, recent -> {
                for (RecentPlace place : recent) {
                    if (normalized(place.getName()).equals(said)) {
                        found(current, command, PlaceSuggestion.recent(place.getName(),
                                place.getLatitude(), place.getLongitude(),
                                area.distanceTo(place.getLatitude(), place.getLongitude())));
                        return;
                    }
                }
                if (PERSONAL_PLACES.contains(said)) {
                    notFound(current, command, true);
                } else {
                    search(current, command, area);
                }
            });
        });
    }

    private void search(int current, Command command, SearchArea area) {
        placeSearch.search(command.place, area, results -> {
            if (current != generation) {
                return;
            }
            if (results.places.isEmpty()) {
                notFound(current, command, false);
                return;
            }
            PlaceSuggestion first = results.places.get(0);
            placeSearch.locate(first, location -> {
                if (location == null) {
                    notFound(current, command, false);
                    return;
                }
                found(current, command, PlaceSuggestion.found(first.getName(),
                        first.getAddress(), location.latitude, location.longitude,
                        first.getDistanceMeters() != null ? first.getDistanceMeters()
                                : area.distanceTo(location.latitude, location.longitude)));
            });
        });
    }

    private void found(int current, Command command, PlaceSuggestion place) {
        if (current == generation) {
            state.setValue(new State(command, place, false, false, false));
        }
    }

    private void notFound(int current, Command command, boolean personal) {
        if (current == generation) {
            state.setValue(new State(command, null, false, true, personal));
        }
    }

    // Stores the request as a note and reports its id.
    public void saveNote(String text, Consumer<Long> onSaved) {
        Note note = Note.blank(Folder.DEFAULT_ID).edited(text, "", Folder.DEFAULT_ID);
        notes.saveNote(note, saved -> onSaved.accept(saved.getId()));
    }

    public void setTimeReminder(String text, long triggerAt, Repeat repeat, int repeatDays,
                                Runnable onDone) {
        saveNote(text, noteId -> {
            reminders.addTimeReminder(noteId, triggerAt, repeat, repeatDays);
            onDone.run();
        });
    }

    // Sets the reminder at the place that was found; the place keeps the name it was found by.
    public void setLocationReminder(String text, Runnable onDone) {
        State current = state.getValue();
        if (current == null || current.command == null || current.place == null) {
            return;
        }
        Command command = current.command;
        PlaceSuggestion place = current.place;
        saveNote(text, noteId -> {
            reminders.addLocationReminder(noteId, place.getLatitude(), place.getLongitude(),
                    RADIUS_METERS, place.getName(), command.placeTrigger, command.repeat);
            onDone.run();
        });
    }

    public boolean canShowNotifications() {
        return notifier.canNotify();
    }

    public boolean canScheduleExactAlarms() {
        return scheduler.canScheduleExactAlarms();
    }

    // "The Gym" and "gym" are the same place to say.
    private static String normalized(String name) {
        return name.toLowerCase(Locale.ROOT).replaceAll("^(?:the|my)\\s+", "").trim();
    }
}

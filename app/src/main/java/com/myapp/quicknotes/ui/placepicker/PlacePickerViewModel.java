package com.myapp.quicknotes.ui.placepicker;

import android.app.Application;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.SavedStateHandle;

import com.google.android.gms.maps.model.LatLng;
import com.myapp.quicknotes.AppContainer;
import com.myapp.quicknotes.QuickNotesApp;
import com.myapp.quicknotes.data.PlaceTrigger;
import com.myapp.quicknotes.data.RecentPlace;
import com.myapp.quicknotes.data.Reminder;
import com.myapp.quicknotes.data.ReminderRepository;
import com.myapp.quicknotes.data.ReminderType;
import com.myapp.quicknotes.data.Repeat;
import com.myapp.quicknotes.data.SavedPlace;
import com.myapp.quicknotes.data.SavedPlaceRepository;
import com.myapp.quicknotes.places.OverpassParser;
import com.myapp.quicknotes.places.PlaceSearchRepository;
import com.myapp.quicknotes.places.PlaceSuggestion;
import com.myapp.quicknotes.places.SearchArea;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

public class PlacePickerViewModel extends AndroidViewModel {
    // Navigation arguments; the names match nav_graph.xml. With a reminder id the picker changes
    // that reminder; without one it adds a new reminder to the note.
    private static final String ARG_NOTE_ID = "noteId";
    private static final String ARG_REMINDER_ID = "reminderId";
    private static final String STATE_PLACE = "place";
    private static final String STATE_RADIUS = "radiusMeters";
    private static final String STATE_PLACE_TRIGGER = "placeTrigger";
    private static final float DEFAULT_RADIUS_METERS = 200;
    // Search waits for a pause in typing, and for enough letters to mean something.
    private static final long TYPING_PAUSE_MS = 300;
    private static final int MIN_QUERY_LENGTH = 2;
    private static final int RECENT_PLACES = 5;
    // A recent place this close to a saved one is the same place, and is offered once.
    private static final double SAME_PLACE_METERS = 30;

    // What the search list shows: the saved and recent places that match what was typed, then
    // what the search found.
    public static final class SearchState {
        @NonNull
        public final List<PlaceSuggestion> places;
        public final boolean searching;
        public final boolean failed;
        // The search finished, and neither it nor the saved places had anything.
        public final boolean nothingFound;
        // Who the found places came from, to be credited; null when none are shown.
        @Nullable
        public final PlaceSearchRepository.Source source;

        SearchState(@NonNull List<PlaceSuggestion> places, boolean searching, boolean failed,
                    boolean nothingFound, @Nullable PlaceSearchRepository.Source source) {
            this.places = places;
            this.searching = searching;
            this.failed = failed;
            this.nothingFound = nothingFound;
            this.source = source;
        }
    }

    private final ReminderRepository reminders;
    private final SavedPlaceRepository savedPlaces;
    private final PlaceSearchRepository placeSearch;
    private final SavedStateHandle state;
    // The reminder being changed, once loaded. Stays empty when adding a new one.
    private final MutableLiveData<Reminder> editedReminder = new MutableLiveData<>();
    private final LiveData<List<SavedPlace>> saved;
    // Loaded once: no reminder is set while the picker is open.
    private final MutableLiveData<List<RecentPlace>> recent = new MutableLiveData<>();
    // Saved places, then recent ones, as chips over the map and at the top of the search list.
    private final MediatorLiveData<List<PlaceSuggestion>> quickPlaces = new MediatorLiveData<>();
    private final MediatorLiveData<SearchState> searchState = new MediatorLiveData<>();
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable sendSearch = this::sendSearch;
    // What is being searched for, the area, and what the search found for it (null until then).
    private String query = "";
    @Nullable
    private SearchArea area;
    @Nullable
    private PlaceSearchRepository.Results found;
    private boolean searching;

    public PlacePickerViewModel(@NonNull Application application,
                                @NonNull SavedStateHandle state) {
        super(application);
        AppContainer container = QuickNotesApp.container(application);
        this.reminders = container.reminderRepository();
        this.savedPlaces = container.savedPlaceRepository();
        this.placeSearch = container.placeSearchRepository();
        this.state = state;
        saved = savedPlaces.observeAll();
        reminders.loadRecentPlaces(RECENT_PLACES, recent::setValue);
        quickPlaces.addSource(saved, places -> quickPlaces.setValue(buildQuickPlaces()));
        quickPlaces.addSource(recent, places -> quickPlaces.setValue(buildQuickPlaces()));
        searchState.addSource(quickPlaces, places -> publishSearchState());
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
                    setPlaceTrigger(reminder.getPlaceTrigger());
                }
                editedReminder.setValue(reminder);
            });
        }
    }

    @Override
    protected void onCleared() {
        handler.removeCallbacks(sendSearch);
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

    // Whether the reminder is for arriving at the place or for leaving it.
    @NonNull
    public PlaceTrigger getPlaceTrigger() {
        PlaceTrigger trigger = state.get(STATE_PLACE_TRIGGER);
        return trigger != null ? trigger : PlaceTrigger.ARRIVING;
    }

    public void setPlaceTrigger(PlaceTrigger placeTrigger) {
        state.set(STATE_PLACE_TRIGGER, placeTrigger);
    }

    public LiveData<List<SavedPlace>> getSavedPlaces() {
        return saved;
    }

    public LiveData<List<PlaceSuggestion>> getQuickPlaces() {
        return quickPlaces;
    }

    public LiveData<SearchState> getSearchState() {
        return searchState;
    }

    // Called as the user types, and when the search opens. The search itself waits for a pause.
    public void setQuery(String text, SearchArea searchArea) {
        area = searchArea;
        String trimmed = text.trim();
        if (!trimmed.equals(query) || (found == null && !searching)) {
            query = trimmed;
            found = null;
            handler.removeCallbacks(sendSearch);
            searching = query.length() >= MIN_QUERY_LENGTH;
            if (searching) {
                handler.postDelayed(sendSearch, TYPING_PAUSE_MS);
            }
        }
        publishSearchState();
    }

    private void sendSearch() {
        if (area == null) {
            return;
        }
        placeSearch.search(query, area, results -> {
            // An answer to something since typed over is dropped.
            if (!results.query.equals(query)) {
                return;
            }
            found = results;
            searching = false;
            publishSearchState();
        });
    }

    // Reports where the place is, or null when that couldn't be found out.
    public void locate(PlaceSuggestion place, Consumer<LatLng> onLocated) {
        placeSearch.locate(place, onLocated);
    }

    // The saved place at this spot, if there is one.
    @Nullable
    public SavedPlace savedPlaceAt(LatLng place) {
        for (SavedPlace savedPlace : listOf(saved)) {
            if (OverpassParser.distanceMeters(place.latitude, place.longitude,
                    savedPlace.getLatitude(), savedPlace.getLongitude()) < 1) {
                return savedPlace;
            }
        }
        return null;
    }

    @Nullable
    public SavedPlace savedPlaceWithId(long id) {
        for (SavedPlace savedPlace : listOf(saved)) {
            if (savedPlace.getId() == id) {
                return savedPlace;
            }
        }
        return null;
    }

    public void savePlace(String name, LatLng place) {
        savedPlaces.save(name, place.latitude, place.longitude);
    }

    public void renamePlace(SavedPlace place, String newName) {
        savedPlaces.rename(place, newName);
    }

    public void removePlace(SavedPlace place) {
        savedPlaces.delete(place);
    }

    // Stores the chosen place: as a change to the reminder being edited, or as a new reminder.
    // Returns false when there is nothing to store yet.
    public boolean save(@Nullable String placeName, boolean everyVisit) {
        LatLng place = getPlace();
        if (place == null) {
            return false;
        }
        Repeat repeat = everyVisit ? Repeat.EVERY_ARRIVAL : Repeat.NONE;
        if (!isEditing()) {
            reminders.addLocationReminder(longArg(ARG_NOTE_ID), place.latitude, place.longitude,
                    getRadiusMeters(), placeName, getPlaceTrigger(), repeat);
            return true;
        }
        Reminder edited = editedReminder.getValue();
        if (edited == null) {
            return false;
        }
        reminders.updateReminder(edited.moved(place.latitude, place.longitude,
                getRadiusMeters(), placeName, getPlaceTrigger(), repeat));
        return true;
    }

    private List<PlaceSuggestion> buildQuickPlaces() {
        List<SavedPlace> savedList = listOf(saved);
        List<PlaceSuggestion> places = new ArrayList<>();
        for (SavedPlace savedPlace : savedList) {
            places.add(PlaceSuggestion.saved(savedPlace.getId(), savedPlace.getName(),
                    savedPlace.getLatitude(), savedPlace.getLongitude(), null));
        }
        for (RecentPlace recentPlace : listOf(recent)) {
            if (!isNearSavedPlace(recentPlace, savedList)) {
                places.add(PlaceSuggestion.recent(recentPlace.getName(),
                        recentPlace.getLatitude(), recentPlace.getLongitude(), null));
            }
        }
        return places;
    }

    private static boolean isNearSavedPlace(RecentPlace place, List<SavedPlace> savedList) {
        for (SavedPlace savedPlace : savedList) {
            if (OverpassParser.distanceMeters(place.getLatitude(), place.getLongitude(),
                    savedPlace.getLatitude(), savedPlace.getLongitude()) < SAME_PLACE_METERS) {
                return true;
            }
        }
        return false;
    }

    private void publishSearchState() {
        List<PlaceSuggestion> places = new ArrayList<>();
        String lowerQuery = query.toLowerCase(Locale.ROOT);
        for (PlaceSuggestion place : listOf(quickPlaces)) {
            if (place.getName().toLowerCase(Locale.ROOT).contains(lowerQuery)) {
                places.add(area == null ? place : place.withDistance(
                        area.distanceTo(place.getLatitude(), place.getLongitude())));
            }
        }
        PlaceSearchRepository.Source source = null;
        if (found != null && !found.places.isEmpty()) {
            places.addAll(found.places);
            source = found.source;
        }
        boolean failed = found != null && found.failed;
        boolean nothingFound = found != null && !found.failed && places.isEmpty();
        searchState.setValue(new SearchState(places, searching, failed, nothingFound, source));
    }

    private static <T> List<T> listOf(LiveData<List<T>> live) {
        List<T> value = live.getValue();
        return value != null ? value : Collections.emptyList();
    }

    private long longArg(String key) {
        Long value = state.get(key);
        return value != null ? value : 0;
    }
}

package com.myapp.quicknotes;

import android.content.Context;
import android.content.pm.PackageManager;

import androidx.core.content.pm.PackageInfoCompat;

import com.myapp.quicknotes.data.AppDatabase;
import com.myapp.quicknotes.data.FolderRepository;
import com.myapp.quicknotes.data.NoteRepository;
import com.myapp.quicknotes.data.ReminderRepository;
import com.myapp.quicknotes.data.SavedPlaceRepository;
import com.myapp.quicknotes.places.GooglePlacesClient;
import com.myapp.quicknotes.places.NearbyPlacesRepository;
import com.myapp.quicknotes.places.OverpassClient;
import com.myapp.quicknotes.places.PhotonClient;
import com.myapp.quicknotes.places.PlaceSearchRepository;
import com.myapp.quicknotes.reminders.ReminderNotifier;
import com.myapp.quicknotes.reminders.ReminderScheduler;
import com.myapp.quicknotes.search.CrashGuardedEmbedder;
import com.myapp.quicknotes.search.SearchRepository;
import com.myapp.quicknotes.search.UniversalSentenceEncoder;

// Builds the app's long-lived objects once and hands them to whoever needs them.
public class AppContainer {
    private final AppExecutors executors = new AppExecutors();
    private final ReminderScheduler reminderScheduler;
    private final ReminderNotifier reminderNotifier;
    private final FolderRepository folderRepository;
    private final NoteRepository noteRepository;
    private final ReminderRepository reminderRepository;
    private final SearchRepository searchRepository;
    private final NearbyPlacesRepository nearbyPlacesRepository;
    private final SavedPlaceRepository savedPlaceRepository;
    private final PlaceSearchRepository placeSearchRepository;

    AppContainer(Context appContext) {
        AppDatabase database = AppDatabase.create(appContext);
        reminderScheduler = new ReminderScheduler(appContext);
        reminderNotifier = new ReminderNotifier(appContext);
        folderRepository = new FolderRepository(
                database.folderDao(), database.reminderDao(), reminderScheduler, executors);
        searchRepository = new SearchRepository(database.searchDao(),
                new CrashGuardedEmbedder(new UniversalSentenceEncoder(appContext), appContext,
                        appVersion(appContext)),
                executors);
        noteRepository = new NoteRepository(database.noteDao(), database.reminderDao(),
                reminderScheduler, searchRepository, executors);
        nearbyPlacesRepository = new NearbyPlacesRepository(
                database.placeDao(), database.reminderDao(), new OverpassClient(), executors);
        reminderRepository = new ReminderRepository(
                database.reminderDao(), database.noteDao(), reminderScheduler, reminderNotifier,
                nearbyPlacesRepository, executors);
        savedPlaceRepository = new SavedPlaceRepository(database.savedPlaceDao(), executors);
        placeSearchRepository = new PlaceSearchRepository(
                new GooglePlacesClient(appContext), new PhotonClient(), executors);
    }

    private static long appVersion(Context context) {
        try {
            return PackageInfoCompat.getLongVersionCode(
                    context.getPackageManager().getPackageInfo(context.getPackageName(), 0));
        } catch (PackageManager.NameNotFoundException e) {
            throw new IllegalStateException("The app can always find itself", e);
        }
    }

    public AppExecutors executors() {
        return executors;
    }

    public ReminderScheduler reminderScheduler() {
        return reminderScheduler;
    }

    public ReminderNotifier reminderNotifier() {
        return reminderNotifier;
    }

    public FolderRepository folderRepository() {
        return folderRepository;
    }

    public NoteRepository noteRepository() {
        return noteRepository;
    }

    public ReminderRepository reminderRepository() {
        return reminderRepository;
    }

    public SearchRepository searchRepository() {
        return searchRepository;
    }

    public NearbyPlacesRepository nearbyPlacesRepository() {
        return nearbyPlacesRepository;
    }

    public SavedPlaceRepository savedPlaceRepository() {
        return savedPlaceRepository;
    }

    public PlaceSearchRepository placeSearchRepository() {
        return placeSearchRepository;
    }
}

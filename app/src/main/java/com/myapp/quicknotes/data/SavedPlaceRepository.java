package com.myapp.quicknotes.data;

import androidx.lifecycle.LiveData;

import com.myapp.quicknotes.AppExecutors;

import java.util.List;

// The places the user saved by name. Reminders copy a saved place's position and name when they
// are set, so renaming or removing one leaves existing reminders as they are.
public class SavedPlaceRepository {
    private final SavedPlaceDao savedPlaceDao;
    private final AppExecutors executors;

    public SavedPlaceRepository(SavedPlaceDao savedPlaceDao, AppExecutors executors) {
        this.savedPlaceDao = savedPlaceDao;
        this.executors = executors;
    }

    public LiveData<List<SavedPlace>> observeAll() {
        return savedPlaceDao.observeAll();
    }

    public void save(String name, double latitude, double longitude) {
        executors.io().execute(() ->
                savedPlaceDao.insert(new SavedPlace(name, latitude, longitude)));
    }

    public void rename(SavedPlace place, String newName) {
        executors.io().execute(() -> savedPlaceDao.update(place.renamed(newName)));
    }

    public void delete(SavedPlace place) {
        executors.io().execute(() -> savedPlaceDao.deleteById(place.getId()));
    }
}

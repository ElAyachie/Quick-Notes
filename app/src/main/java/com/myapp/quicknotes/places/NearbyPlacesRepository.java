package com.myapp.quicknotes.places;

import androidx.annotation.WorkerThread;

import com.myapp.quicknotes.AppExecutors;
import com.myapp.quicknotes.data.NearbyPlace;
import com.myapp.quicknotes.data.PlaceDao;
import com.myapp.quicknotes.data.PlaceLookup;
import com.myapp.quicknotes.data.Reminder;
import com.myapp.quicknotes.data.ReminderDao;
import com.myapp.quicknotes.data.ReminderType;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

// Finds out what is around each location reminder's place, so search can find the reminder by a
// shop, school, landmark or street near it. Lookups run on the network thread, one at a time. A
// lookup that fails (no connection, servers busy) is tried again at the next app start.
public class NearbyPlacesRepository {
    private final PlaceDao placeDao;
    private final ReminderDao reminderDao;
    private final OverpassClient client;
    private final AppExecutors executors;

    public NearbyPlacesRepository(PlaceDao placeDao, ReminderDao reminderDao,
                                  OverpassClient client, AppExecutors executors) {
        this.placeDao = placeDao;
        this.reminderDao = reminderDao;
        this.client = client;
        this.executors = executors;
    }

    // Looks up the surroundings of a location reminder that was just set or moved.
    public void lookUpAround(Reminder reminder) {
        if (reminder.getType() == ReminderType.LOCATION) {
            executors.network().execute(() -> lookUpNow(reminder));
        }
    }

    // Looks up the places of waiting reminders that haven't been looked up yet, and forgets the
    // surroundings of places no waiting reminder uses any more. Called at app start.
    public void lookUpMissing() {
        executors.network().execute(() -> {
            Set<String> inUse = new HashSet<>();
            for (Reminder reminder : reminderDao.getUpcoming()) {
                if (reminder.getType() == ReminderType.LOCATION) {
                    inUse.add(reminder.placeKey());
                    lookUpNow(reminder);
                }
            }
            placeDao.deleteLookupsExcept(inUse);
        });
    }

    @WorkerThread
    private void lookUpNow(Reminder reminder) {
        String key = reminder.placeKey();
        if (placeDao.isLookedUp(key)) {
            return;
        }
        List<NearbyPlace> places = client.lookUp(key, reminder.getLatitude(),
                reminder.getLongitude(), reminder.getRadiusMeters());
        if (places != null) {
            placeDao.saveLookup(new PlaceLookup(key, System.currentTimeMillis()), places);
        }
    }
}

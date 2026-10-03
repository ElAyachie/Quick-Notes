package com.myapp.quicknotes.data;

import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;
import androidx.lifecycle.LiveData;

import com.myapp.quicknotes.AppExecutors;
import com.myapp.quicknotes.reminders.ReminderNotifier;
import com.myapp.quicknotes.reminders.ReminderScheduler;

import java.time.ZoneId;
import java.util.List;
import java.util.function.Consumer;

public class ReminderRepository {
    private final ReminderDao reminderDao;
    private final NoteDao noteDao;
    private final ReminderScheduler scheduler;
    private final ReminderNotifier notifier;
    private final AppExecutors executors;

    public ReminderRepository(ReminderDao reminderDao, NoteDao noteDao, ReminderScheduler scheduler,
                              ReminderNotifier notifier, AppExecutors executors) {
        this.reminderDao = reminderDao;
        this.noteDao = noteDao;
        this.scheduler = scheduler;
        this.notifier = notifier;
        this.executors = executors;
    }

    public LiveData<List<ReminderWithNote>> observeUpcoming() {
        return reminderDao.observeUpcoming();
    }

    public LiveData<List<ReminderWithNote>> observeHistory() {
        return reminderDao.observeHistory();
    }

    public void addTimeReminder(long noteId, long triggerAt, Repeat repeat) {
        add(Reminder.atTime(noteId, triggerAt, repeat));
    }

    public void addLocationReminder(long noteId, double latitude, double longitude,
                                    float radiusMeters, @Nullable String placeName,
                                    Repeat repeat) {
        add(Reminder.atPlace(noteId, latitude, longitude, radiusMeters, placeName, repeat));
    }

    private void add(Reminder reminder) {
        executors.io().execute(() ->
                scheduler.schedule(reminder.withId(reminderDao.insert(reminder))));
    }

    // Reports null when the reminder no longer exists.
    public void loadReminder(long reminderId, Consumer<Reminder> onLoaded) {
        executors.io().execute(() -> {
            Reminder reminder = reminderDao.getById(reminderId);
            executors.runOnMain(() -> onLoaded.accept(reminder));
        });
    }

    // Stores a waiting reminder's new time or place. Scheduling it again under the same id
    // replaces the alarm or watched place it had before.
    public void updateReminder(Reminder updated) {
        executors.io().execute(() -> {
            reminderDao.update(updated);
            scheduler.schedule(updated);
        });
    }

    // Deleting a waiting reminder stops it (and, if it repeats, the whole series). Deleting a
    // fired one only removes it from history.
    public void deleteReminder(Reminder reminder) {
        executors.io().execute(() -> {
            scheduler.cancel(reminder);
            reminderDao.deleteById(reminder.getId());
        });
    }

    // Shows the notification for a reminder that has come due, moves the reminder to history
    // and, if it repeats, schedules its next occurrence.
    @WorkerThread
    public void fire(long reminderId) {
        Reminder reminder = reminderDao.getById(reminderId);
        if (reminder == null || reminder.hasFired()) {
            return;
        }
        Note note = noteDao.getById(reminder.getNoteId());
        // An occurrence fires once. A place keeps being watched until told otherwise.
        scheduler.cancel(reminder);
        long now = System.currentTimeMillis();
        Reminder next = reminder.nextOccurrence(now, ZoneId.systemDefault());
        if (next == null) {
            reminderDao.markFired(reminderId, now);
        } else {
            scheduler.schedule(next.withId(
                    reminderDao.markFiredAndInsertNext(reminderId, now, next)));
        }
        if (note != null) {
            notifier.show(reminderId, note);
        }
    }

    // Alarms and watched places don't survive a reboot or an app update, so every reminder that
    // is still waiting is scheduled again. A time reminder that came due in the meantime fires
    // straight away.
    @WorkerThread
    public void rescheduleAll() {
        for (Reminder reminder : reminderDao.getUpcoming()) {
            scheduler.schedule(reminder);
        }
    }
}

package com.myapp.quicknotes.data;

import androidx.annotation.WorkerThread;
import androidx.lifecycle.LiveData;

import com.myapp.quicknotes.AppExecutors;
import com.myapp.quicknotes.reminders.ReminderNotifier;
import com.myapp.quicknotes.reminders.ReminderScheduler;

import java.util.List;

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

    public LiveData<List<ReminderWithNote>> observeReminders() {
        return reminderDao.observeAll();
    }

    public void addTimeReminder(long noteId, long triggerAt) {
        executors.io().execute(() -> {
            Reminder reminder = Reminder.atTime(noteId, triggerAt);
            scheduler.schedule(reminder.withId(reminderDao.insert(reminder)));
        });
    }

    public void deleteReminder(long reminderId) {
        executors.io().execute(() -> {
            scheduler.cancel(reminderId);
            reminderDao.deleteById(reminderId);
        });
    }

    // Shows the notification for a reminder that has come due and removes the reminder.
    @WorkerThread
    public void fire(long reminderId) {
        Reminder reminder = reminderDao.getById(reminderId);
        if (reminder == null) {
            return;
        }
        Note note = noteDao.getById(reminder.getNoteId());
        reminderDao.deleteById(reminderId);
        if (note != null) {
            notifier.show(reminderId, note);
        }
    }

    // Alarms don't survive a reboot or an app update, so every stored reminder is scheduled again.
    // A reminder that came due in the meantime fires straight away.
    @WorkerThread
    public void rescheduleAll() {
        for (Reminder reminder : reminderDao.getAll()) {
            scheduler.schedule(reminder);
        }
    }
}

package com.myapp.quicknotes.ui.reminders;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.myapp.quicknotes.QuickNotesApp;
import com.myapp.quicknotes.data.Reminder;
import com.myapp.quicknotes.data.ReminderRepository;
import com.myapp.quicknotes.data.ReminderWithNote;

import java.util.List;

public class RemindersViewModel extends AndroidViewModel {
    private final ReminderRepository reminders;

    public RemindersViewModel(@NonNull Application application) {
        super(application);
        reminders = QuickNotesApp.container(application).reminderRepository();
    }

    public LiveData<List<ReminderWithNote>> getReminders() {
        return reminders.observeReminders();
    }

    public void deleteReminder(Reminder reminder) {
        reminders.deleteReminder(reminder.getId());
    }
}

package com.myapp.quicknotes.ui.reminders;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.SavedStateHandle;
import androidx.lifecycle.Transformations;

import com.myapp.quicknotes.QuickNotesApp;
import com.myapp.quicknotes.data.Reminder;
import com.myapp.quicknotes.data.ReminderRepository;
import com.myapp.quicknotes.data.ReminderWithNote;
import com.myapp.quicknotes.data.Repeat;

import java.util.List;

public class RemindersViewModel extends AndroidViewModel {
    private static final String STATE_SHOWING_HISTORY = "showingHistory";

    private final ReminderRepository reminders;
    private final SavedStateHandle state;
    private final LiveData<List<ReminderWithNote>> shownReminders;

    public RemindersViewModel(@NonNull Application application, @NonNull SavedStateHandle state) {
        super(application);
        this.reminders = QuickNotesApp.container(application).reminderRepository();
        this.state = state;
        this.shownReminders = Transformations.switchMap(
                state.getLiveData(STATE_SHOWING_HISTORY, false),
                history -> history ? reminders.observeHistory() : reminders.observeUpcoming());
    }

    // The reminders of the list being shown: those still waiting, or those that have fired.
    public LiveData<List<ReminderWithNote>> getReminders() {
        return shownReminders;
    }

    public boolean isShowingHistory() {
        return Boolean.TRUE.equals(state.get(STATE_SHOWING_HISTORY));
    }

    public void setShowingHistory(boolean showingHistory) {
        state.set(STATE_SHOWING_HISTORY, showingHistory);
    }

    public void rescheduleReminder(Reminder reminder, long triggerAt, Repeat repeat,
                                   int repeatDays) {
        reminders.updateReminder(reminder.rescheduled(triggerAt, repeat, repeatDays));
    }

    public void deleteReminder(Reminder reminder) {
        reminders.deleteReminder(reminder);
    }
}

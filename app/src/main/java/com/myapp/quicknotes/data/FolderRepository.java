package com.myapp.quicknotes.data;

import androidx.lifecycle.LiveData;

import com.myapp.quicknotes.AppExecutors;
import com.myapp.quicknotes.reminders.ReminderScheduler;

import java.util.List;
import java.util.function.Consumer;

public class FolderRepository {
    private final FolderDao folderDao;
    private final ReminderDao reminderDao;
    private final ReminderScheduler scheduler;
    private final AppExecutors executors;

    public FolderRepository(FolderDao folderDao, ReminderDao reminderDao,
                            ReminderScheduler scheduler, AppExecutors executors) {
        this.folderDao = folderDao;
        this.reminderDao = reminderDao;
        this.scheduler = scheduler;
        this.executors = executors;
    }

    public LiveData<List<Folder>> observeFolders() {
        return folderDao.observeAll();
    }

    // Reports false, without creating anything, when a folder with this name already exists.
    public void createFolder(String name, Consumer<Boolean> onResult) {
        executors.io().execute(() -> {
            boolean available = folderDao.countByName(name) == 0;
            if (available) {
                folderDao.insert(new Folder(name));
            }
            executors.runOnMain(() -> onResult.accept(available));
        });
    }

    // Deletes the folder and everything in it. The default folder is never deleted.
    public void deleteFolder(Folder folder) {
        if (folder.isDefault()) {
            return;
        }
        executors.io().execute(() -> {
            for (long reminderId : reminderDao.getIdsForFolder(folder.getId())) {
                scheduler.cancel(reminderId);
            }
            folderDao.deleteById(folder.getId());
        });
    }
}

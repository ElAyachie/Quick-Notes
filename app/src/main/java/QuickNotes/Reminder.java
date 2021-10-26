package QuickNotes;

import java.io.Serializable;

// The Reminder class keeps track of reminder information.
// The class extends serializable so that we can pass the objects
// through intents.
public class Reminder implements Serializable {
    private String noteName;
    private String noteContent;
    private String reminderDate;
    private String type;
    private int notificationID;
    private String folderName;

    public Reminder() {
    }

    public Reminder(String type, String folderName, String noteName, String noteContent, int notificationID) {
        this.type = type;
        this.folderName = folderName;
        this.noteName = noteName;
        this.noteContent = noteContent;
        this.notificationID = notificationID;
    }

    public String getNoteContent() {
        return noteContent;
    }

    public void setNoteContent(String noteContent) {
        this.noteContent = noteContent;
    }

    public String getReminderDate() {
        return reminderDate;
    }

    public void setReminderDate(String dateCreated) {
        this.reminderDate = dateCreated;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getNoteName() {
        return noteName;
    }

    public void setNoteName(String noteName) {
        this.noteName = noteName;
    }

    public int getNotificationID() {
        return notificationID;
    }

    public void setNotificationID(int notificationID) {
        this.notificationID = notificationID;
    }

    public String getFolderName() {
        return folderName;
    }

    public void setFolderName(String folderName) {
        this.folderName = folderName;
    }
}

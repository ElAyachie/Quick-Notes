package QuickNotes;

import androidx.annotation.NonNull;

import java.io.Serializable;

// The Note class keeps track of note information.
// The class extends serializable so that we can pass the objects
// through intents.
public class Note implements Serializable {
    private String noteName;
    private String noteContent;
    private String folderName;
    private String dateCreated;

    public Note() {
    }

    public Note(String noteName, String noteContent, String folderName, String dateCreated) {
        this.noteName = noteName;
        this.noteContent = noteContent;
        this.folderName = folderName;
        this.dateCreated = dateCreated;
    }

    public String getNoteName() {
        return noteName;
    }

    public String getNoteContent() {
        return noteContent;
    }

    public String getFolderName() {
        return folderName;
    }

    public String getDateCreated() {
        return dateCreated;
    }

    public void setNoteName(String noteName) {
        this.noteName = noteName;
    }

    public void setNoteContent(String noteContent) {
        this.noteContent = noteContent;
    }

    public void setFolderName(String folderName) {
        this.folderName = folderName;
    }

    public void setDateCreated(String dateCreated) {
        this.dateCreated = dateCreated;
    }

    @NonNull
    public String toString() {
        return this.getFolderName() + "/" + this.getNoteName() + ".txt";
    }
}
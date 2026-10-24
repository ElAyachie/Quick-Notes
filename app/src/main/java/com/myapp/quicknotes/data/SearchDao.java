package com.myapp.quicknotes.data;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Transaction;

import java.util.Collection;
import java.util.List;

@Dao
public interface SearchDao {
    // Notes containing the words of a full-text query (see search.KeywordScorer.matchQuery).
    @Query("SELECT notes.* FROM notes JOIN notes_fts ON notes.id = notes_fts.rowid "
            + "WHERE notes_fts MATCH :matchQuery")
    List<Note> findNotesMatching(String matchQuery);

    @Query("SELECT notes.*, folders.name AS folder_name FROM notes "
            + "JOIN folders ON folders.id = notes.folder_id WHERE notes.id IN (:noteIds)")
    List<NoteWithFolder> getNotesWithFolder(Collection<Long> noteIds);

    @Query("SELECT * FROM notes")
    List<Note> getAllNotes();

    @Query("SELECT * FROM note_chunks")
    List<NoteChunk> getAllChunks();

    // One row per note that has passages. A note's passages all share a model and source hash.
    @Query("SELECT note_id, model_id, source_hash FROM note_chunks GROUP BY note_id")
    List<IndexedNote> getIndexedNotes();

    @Insert
    void insertChunks(List<NoteChunk> chunks);

    @Query("DELETE FROM note_chunks WHERE note_id = :noteId")
    void deleteChunksOfNote(long noteId);

    // A note's passages are swapped all at once, so a search never sees half of the old and half
    // of the new.
    @Transaction
    default void replaceChunksOfNote(long noteId, List<NoteChunk> chunks) {
        deleteChunksOfNote(noteId);
        insertChunks(chunks);
    }
}

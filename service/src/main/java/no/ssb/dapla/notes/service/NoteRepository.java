package no.ssb.dapla.notes.service;

import no.ssb.dapla.notes.protobuf.Note;

import java.util.List;
import java.util.UUID;

public interface NoteRepository {

    void saveNote(Note note) throws NoteRepositoryException;

    /**
     * This method finds a note with the SSB specific UUID
     *
     * The uuid is
     * @param uuid
     * @return
     * @throws NoteRepositoryException
     * @throws NoteNotFound
     */
    Note getNote(String uuid) throws NoteRepositoryException, NoteNotFound;

    /**
     * This method finds a note by its original id
     * <p>
     * The original id is the ID that is given by the note system when it is not possible
     * to override it in the integration.
     *
     * @param original the original id of the note.
     * @return the note with original id.
     * @throws NoteRepositoryException
     * @throws NoteNotFound            if no note with this original id exists.
     */
    Note getNoteByOriginalId(String original) throws NoteRepositoryException, NoteNotFound;

    List<Note> listNotes(List<String> namespace, int count, int offset) throws NoteRepositoryException;

    class NoteRepositoryException extends Exception {
        public NoteRepositoryException(String msg, IllegalArgumentException ex) {
            super(msg, ex);
        }

        public NoteRepositoryException(String msg) {
            super(msg);
        }
    }

    class NoteNotFound extends NoteRepositoryException {
        public NoteNotFound(String msg, IllegalArgumentException ex) {
            super(msg, ex);
        }

        public NoteNotFound(String originalId) {
            super("no note with original id: " + originalId);
        }

        public NoteNotFound(UUID noteUUID) {
            super("no note with uuid: " + noteUUID);
        }
    }
}

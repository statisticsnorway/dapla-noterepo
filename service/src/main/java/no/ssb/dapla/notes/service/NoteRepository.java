package no.ssb.dapla.notes.service;

import no.ssb.dapla.notes.protobuf.Note;

import java.util.List;
import java.util.UUID;

public interface NoteRepository {

    void saveNote(Note note) throws NoteRepositoryException;

    Note getNote(String uuid) throws NoteRepositoryException, NoteNotFound;

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

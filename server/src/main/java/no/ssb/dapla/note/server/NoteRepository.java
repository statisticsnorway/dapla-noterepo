package no.ssb.dapla.note.server;

import no.ssb.dapla.note.api.Note;

import java.util.List;

public interface NoteRepository {

    void saveNote(Note note) throws NoteRepositoryException;

    Note getNote(String uuid) throws NoteNotFound;

    List<Note> listNotes(List<String> namespace, int count, int offset) throws NoteRepositoryException;

    class NoteRepositoryException extends Exception {
    }

    class NoteNotFound extends NoteRepositoryException {
    }
}

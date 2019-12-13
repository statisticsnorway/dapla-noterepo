package no.ssb.dapla.notes.service.memory;

import no.ssb.dapla.notes.protobuf.Note;
import no.ssb.dapla.notes.service.NoteRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A simple in-memory repository.
 */
public class MemoryRepository implements NoteRepository {

    private ConcurrentHashMap<UUID, Note> notes = new ConcurrentHashMap<>();
    private ConcurrentHashMap<String, UUID> aliases = new ConcurrentHashMap<>();

    private static UUID parseUUID(String uuid) throws NoteRepositoryException {
        try {
            return UUID.fromString(uuid);
        } catch (IllegalArgumentException iar) {
            throw new NoteRepositoryException("invalid uuid", iar);
        }
    }

    private static UUID parseUUID(Note note) throws NoteRepositoryException {
        return parseUUID(note.getUuid());
    }

    @Override
    public void saveNote(Note note) throws NoteRepositoryException {
        // Check if the note exists.
        if (hasNote(note)) {
            updateNote(note);
        } else {
            persistNote(note);
        }
    }

    private void persistNote(Note note) throws NoteRepositoryException {
        UUID uuid = parseUUID(note);
        notes.put(uuid, note);
        for (String alias : note.getAliasIdentifiersList()) {
            aliases.putIfAbsent(alias, uuid);
        }
    }

    private void updateNote(Note note) throws NoteRepositoryException {
        // Always override for now.
        persistNote(note);
    }

    private boolean hasNote(Note note) throws NoteRepositoryException {
        UUID uuid = parseUUID(note);
        if (!notes.containsKey(uuid)) {
            return false;
        }
        for (String aliasIdentifier : note.getAliasIdentifiersList()) {
            if (aliases.containsKey(aliasIdentifier)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public Note getNote(String noteUUID) throws NoteRepositoryException {
        UUID uuid = parseUUID(noteUUID);
        if (!notes.containsKey(uuid)) {
            throw new NoteNotFound(uuid);
        } else {
            return notes.get(uuid);
        }
    }

    @Override
    public Note getNoteByOriginalId(String originalId) throws NoteRepositoryException, NoteNotFound {
        if (!aliases.containsKey(originalId)) {
            throw new NoteNotFound(originalId);
        } else {
            return notes.get(aliases.get(originalId));
        }
    }

    @Override
    public List<Note> listNotes(List<String> namespace, int count, int offset) throws NoteRepositoryException {
        return new ArrayList<>(notes.values());
    }
}

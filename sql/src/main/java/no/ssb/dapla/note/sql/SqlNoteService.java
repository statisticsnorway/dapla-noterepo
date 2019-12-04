package no.ssb.dapla.note.sql;

import lombok.extern.slf4j.Slf4j;
import no.ssb.dapla.note.api.NamedDataset;
import no.ssb.dapla.note.api.NoteIdentifier;
import no.ssb.dapla.note.server.NoteRepository;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Slf4j
@Singleton
public class SqlNoteService implements NoteRepository {

    @Inject
    private SqlNoteRepository noteRepository;

    @Inject
    private SqlNamespaceRepository nsRepository;

    @Override
    public void saveNote(no.ssb.dapla.note.api.Note note) throws NoteRepositoryException {

        NoteIdentifier identifier = note.getIdentifier();

        // So we validate that it is indeed a UUID.
        UUID uuid = UUID.fromString(identifier.getUuid());
        String pathString = String.join("/", identifier.getNamespace().getNamespaceList());

        // Create the namespace if it does not exist.
        SqlNamespace existingPath = nsRepository.findByPath(pathString).orElseGet(() -> {
            SqlNamespace namespace = new SqlNamespace();
            namespace.setPath(pathString);
            return nsRepository.save(namespace);
        });

        // Check if note exists.
        SqlNote existingNote = noteRepository.findById(uuid).orElseGet(() -> {
            SqlNote newNote = new SqlNote();
            newNote.setName(note.getIdentifier().getName());
            newNote.setNamespace(existingPath);
            newNote.setId(uuid);
            return noteRepository.save(newNote);
        });

        if (!existingNote.getNamespace().equals(existingPath)) {
            log.debug("moving note {} from {} to {}", existingNote.getId(), existingNote.getNamespace(), existingPath);
        } else {
            log.debug("saving new note {} in {}", existingNote.getId(), existingNote.getNamespace());
        }

        List<SqlNoteDataset> inputs = new ArrayList<>();
        for (NamedDataset input : note.getInputList()) {
            SqlNoteDataset dataset = new SqlNoteDataset();
            dataset.setName(input.getName());
            //dataset.setSourceNote(existingNote);
            inputs.add(dataset);
        }
        existingNote.setInputs(inputs);

        List<SqlNoteDataset> outputs = new ArrayList<>();
        for (NamedDataset output : note.getOutputList()) {
            SqlNoteDataset dataset = new SqlNoteDataset();
            dataset.setName(output.getName());
            //dataset.setSourceNote(existingNote);
            outputs.add(dataset);
        }
        //existingNote.setOutputs(outputs);

        noteRepository.update(existingNote);
    }

    @Override
    public no.ssb.dapla.note.api.Note getNote(String uuid) throws NoteNotFound {
        Optional<SqlNote> note = noteRepository.findById(UUID.fromString(uuid));
        return note.map(SqlNote::toGrpc).orElseThrow(NoteNotFound::new);
    }

    @Override
    public List<no.ssb.dapla.note.api.Note> listNotes(List<String> namespace, int count, int offset) throws NoteRepositoryException {
        Iterable<SqlNote> all = noteRepository.findAll();
        return StreamSupport.stream(all.spliterator(), false).map(SqlNote::toGrpc).collect(Collectors.toList());
    }
}

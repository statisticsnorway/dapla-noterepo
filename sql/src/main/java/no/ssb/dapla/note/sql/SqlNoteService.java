package no.ssb.dapla.note.sql;

import io.micronaut.context.annotation.Requires;
import lombok.extern.slf4j.Slf4j;
import no.ssb.dapla.note.api.Dataset;
import no.ssb.dapla.note.api.Note;
import no.ssb.dapla.note.server.NoteRepository;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.transaction.Transactional;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Slf4j
@Singleton
@Named("sql")
@Requires(property="backend", value="sql")
public class SqlNoteService implements NoteRepository {

    @Inject
    private SqlNoteRepository noteRepository;

    @Inject
    private SqlNamespaceRepository nsRepository;

    @Inject
    private SqlDatasetRepository datasetRepository;

    @Override
    public void saveNote(Note note) throws NoteRepositoryException {

        // So we validate that it is indeed a UUID.
        UUID uuid = UUID.fromString(note.getUuid());
        String pathString = String.join("/", note.getNamespaceList());

        // Create the namespace if it does not exist.
        SqlNamespace existingPath = nsRepository.findByPath(pathString).orElseGet(() -> {
            SqlNamespace namespace = new SqlNamespace();
            namespace.setPath(pathString);
            return nsRepository.save(namespace);
        });

        // Check if note exists.
        SqlNote existingNote = noteRepository.findById(uuid).orElseGet(() -> {
            SqlNote newNote = new SqlNote();
            newNote.setName(note.getName());
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
        for (Dataset input : note.getInputsList()) {
            SqlNoteDataset dataset = new SqlNoteDataset();
            dataset.setName(input.getName());
            //dataset.setSourceNote(existingNote);
            inputs.add(datasetRepository.save(dataset));
        }
        existingNote.setInputs(inputs);

        List<SqlNoteDataset> outputs = new ArrayList<>();
        for (Dataset output : note.getOutputsList()) {
            SqlNoteDataset dataset = new SqlNoteDataset();
            dataset.setName(output.getName());
            //dataset.setSourceNote(existingNote);
            outputs.add(dataset);
        }
       //existingNote(outputs);

        noteRepository.update(existingNote);
    }

    @Transactional
    @Override
    public no.ssb.dapla.note.api.Note getNote(String uuid) throws NoteNotFound {
        UUID noteUUID = UUID.fromString(uuid);
        Optional<SqlNote> note = noteRepository.findById(noteUUID);
        return note.map(SqlNote::toGrpc).orElseThrow(() -> new NoteNotFound(noteUUID));
    }

    @Override
    public Note getNoteByOriginalId(String original) throws NoteRepositoryException, NoteNotFound {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<no.ssb.dapla.note.api.Note> listNotes(List<String> namespace, int count, int offset) throws NoteRepositoryException {
        Iterable<SqlNote> all = noteRepository.findAll();
        return StreamSupport.stream(all.spliterator(), false).map(SqlNote::toGrpc).collect(Collectors.toList());
    }
}

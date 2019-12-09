package no.ssb.dapla.note.sql;

import io.micronaut.test.annotation.MicronautTest;
import no.ssb.dapla.note.api.Dataset;
import no.ssb.dapla.note.api.Note;
import no.ssb.dapla.note.server.NoteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.inject.Inject;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@MicronautTest(transactional = false)
class SqlNoteServiceTest {

    @Inject
    SqlNoteService service;

    @BeforeEach
    void setUp() {

    }

    @Test
    void canSaveNotes() throws NoteRepository.NoteRepositoryException {

        UUID uuid = UUID.randomUUID();

        Note note = Note.newBuilder()
                .setUuid(uuid.toString())
                .setName("Some name")
                .addNamespace("some").addNamespace("long").addNamespace("path")

                .addInputs(Dataset.newBuilder().setName("input1").setUuid("uuid1").build())
                .addInputs(Dataset.newBuilder().setName("input2").setUuid("uuid2").build())

                .addOutputs(Dataset.newBuilder().setName("output1").setUuid("uuid1").build())
                .addOutputs(Dataset.newBuilder().setName("output2").setUuid("uuid2").build())

                //.addAliasIdentifiers("anAliasId")

                .build();

        service.saveNote(note);

        assertThat(service.getNote(note.getUuid()))
                .isEqualToComparingFieldByField(note);

    }

    @Test
    void testUUIDIsValidated() throws NoteRepository.NoteRepositoryException {

        Note note = Note.newBuilder()
                .setUuid("NotAnUUID")
                .setName("Name")
                .addNamespace("foo").addNamespace("bar")
                .build();

        assertThatThrownBy(() -> {
            service.saveNote(note);
        });
    }

    @Test
    void testUUIDIsRequired() throws NoteRepository.NoteRepositoryException {

        Note note = Note.newBuilder()
                .setName("Name")
                .addNamespace("foo").addNamespace("bar")
                .build();

        assertThatThrownBy(() -> {
            service.saveNote(note);
        });
    }
}
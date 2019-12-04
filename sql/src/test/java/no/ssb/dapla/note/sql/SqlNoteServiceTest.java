package no.ssb.dapla.note.sql;

import io.micronaut.test.annotation.MicronautTest;
import no.ssb.dapla.note.api.NameSpace;
import no.ssb.dapla.note.api.Note;
import no.ssb.dapla.note.api.NoteIdentifier;
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

        Note note = Note.newBuilder().setIdentifier(
                NoteIdentifier.newBuilder()
                .setUuid(uuid.toString())
                .setName("Some name")
                .setNamespace(
                        NameSpace.newBuilder().addNamespace("some").addNamespace("long").addNamespace("path")
                )
        ).build();

        service.saveNote(note);

        assertThat(service.getNote(note.getIdentifier().getUuid()))
                .isEqualToComparingFieldByField(note);

    }

    @Test
    void testUUIDIsValidated() throws NoteRepository.NoteRepositoryException {

        Note note = Note.newBuilder().setIdentifier(NoteIdentifier.newBuilder()
                .setUuid("NotAnUUID")
                .setName("Name")
                .setNamespace(NameSpace.newBuilder().addNamespace("foo").addNamespace("bar")
                )
        ).build();

        assertThatThrownBy(() -> {
            service.saveNote(note);
        });
    }
}
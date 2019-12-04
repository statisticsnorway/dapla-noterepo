package no.ssb.dapla.note.sql;

import io.micronaut.test.annotation.MicronautTest;
import org.junit.jupiter.api.Test;

import javax.inject.Inject;
import java.util.List;
import java.util.UUID;

@MicronautTest
class SqlNoteRepositoryTest {

    @Inject
    SqlNoteRepository noteRepo;

    @Inject
    SqlNamespaceRepository nsRepo;

    @Test
    void test() {

        SqlNamespace namespace = new SqlNamespace();
        namespace.setId(UUID.randomUUID());
        namespace.setPaths("/test/hadrien".split("/"));
        //namespace = nsRepo.save(namespace);

        SqlNote aNote = new SqlNote();
        aNote.setNamespace(namespace);
        aNote = noteRepo.save(aNote);

        System.out.println(noteRepo.findById(aNote.getId()));
    }
}
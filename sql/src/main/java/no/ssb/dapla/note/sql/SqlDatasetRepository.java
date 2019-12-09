package no.ssb.dapla.note.sql;

import io.micronaut.data.annotation.Repository;
import io.micronaut.data.repository.CrudRepository;

import java.util.UUID;

@Repository
public interface SqlDatasetRepository extends CrudRepository<SqlNoteDataset, UUID> {
}

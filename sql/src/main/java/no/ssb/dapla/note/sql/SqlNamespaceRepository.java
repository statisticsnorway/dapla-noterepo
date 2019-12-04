package no.ssb.dapla.note.sql;

import io.micronaut.data.annotation.Repository;
import io.micronaut.data.repository.CrudRepository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface SqlNamespaceRepository extends CrudRepository<SqlNamespace, UUID> {

    Optional<SqlNamespace> findByPath(String path);

}

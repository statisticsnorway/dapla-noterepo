package no.ssb.dapla.note.sql;

import edu.umd.cs.findbugs.annotations.NonNull;
import io.micronaut.data.annotation.Join;
import io.micronaut.data.annotation.Repository;
import io.micronaut.data.repository.CrudRepository;

import javax.validation.constraints.NotNull;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SqlNoteRepository extends CrudRepository<SqlNote, UUID> {

    @NonNull
    @Override
    //@Join(value = "namespace", type = Join.Type.FETCH)
    //@Join(value = "inputs", type = Join.Type.LEFT)
    //@Join(value = "outputs", type = Join.Type.LEFT)
    Optional<SqlNote> findById(@NonNull @NotNull UUID uuid);

    @NonNull
    @Override
    //@Join(value = "namespace", type = Join.Type.FETCH)
    //@Join(value = "inputs", type = Join.Type.LEFT)
    //@Join(value = "outputs", type = Join.Type.LEFT)
    Iterable<SqlNote> findAll();
}

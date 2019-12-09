package no.ssb.dapla.note.sql;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import no.ssb.dapla.note.api.Dataset;
import no.ssb.dapla.note.api.Note;

import javax.persistence.*;
import java.util.List;
import java.util.UUID;

@ToString
@Getter
@Setter
@Entity
@Table(name = "note_datasets")
public class SqlNoteDataset {

    @Id
    @GeneratedValue
    private UUID id;

    private String name;

    @ManyToOne
    private SqlNote sourceNote;

    @ManyToMany
    private List<SqlNote> targetNotes;

    @Transient
    public Dataset toGrpc() {
        return Dataset.newBuilder()
                .setUuid(getId().toString())
                .setName(getName())
                .setUri("todo")
                .build();
    }
}

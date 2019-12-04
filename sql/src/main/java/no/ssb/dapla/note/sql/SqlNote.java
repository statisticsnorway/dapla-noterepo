package no.ssb.dapla.note.sql;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import no.ssb.dapla.note.api.NameSpace;
import no.ssb.dapla.note.api.Note;
import no.ssb.dapla.note.api.NoteIdentifier;

import javax.persistence.*;
import javax.validation.constraints.NotBlank;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@ToString
@Getter
@Setter
@Entity
@Table(name = "notes")
public class SqlNote {

    @Id
    private UUID id;

    @NotBlank
    private String name;

    @ManyToOne
    private SqlNamespace namespace;

    @OneToMany(targetEntity = SqlNoteDataset.class, mappedBy = "sourceNote")
    private List<SqlNoteDataset> inputs = new ArrayList<>();
//
    //@OneToMany(targetEntity = SqlNoteDataset.class)
    //@JoinTable
    //private List<SqlNoteDataset> outputs = new ArrayList<>();

    @Transient
    public Note toGrpc() {
        return Note.newBuilder().setIdentifier(
                NoteIdentifier.newBuilder()
                        .setNamespace(
                                NameSpace.newBuilder().addAllNamespace(getNamespace().getPaths())
                        )
                        .setName(getName())
                .setUuid(getId().toString())
        ).build();
    }
}

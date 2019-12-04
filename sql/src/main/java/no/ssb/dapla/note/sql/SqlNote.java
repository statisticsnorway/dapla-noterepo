package no.ssb.dapla.note.sql;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import no.ssb.dapla.note.api.Note;

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
        return Note.newBuilder()
                .setUuid(getId().toString())
                .setName(getName())
                .addAllNamespace(getNamespace().getPaths())
                .build();
    }
}

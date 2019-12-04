package no.ssb.dapla.note.sql;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

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

    //@ManyToMany
    //private List<SqlNote> targetNotes;
}

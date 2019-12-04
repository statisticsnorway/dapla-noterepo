package no.ssb.dapla.note.sql;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import javax.persistence.*;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@ToString
@Getter
@Setter
@Entity
@Table(name = "namespaces")
public class SqlNamespace {

    private static final String DELIMITER = "/";

    @Id
    @GeneratedValue
    private UUID id;

    private String path;

    @Transient
    public List<String> getPaths() {
        return Arrays.asList(path.split(DELIMITER));
    }

    public void setPaths(List<String> paths) {
        setPath(String.join(DELIMITER, paths));
    }

    public void setPaths(String... paths) {
        setPaths(Arrays.asList(paths));
    }

}

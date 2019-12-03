package no.ssb.dapla.note.server;

import no.ssb.dapla.note.api.Note;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Record;
import org.jooq.Table;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.sql.DataSource;
import java.util.List;
import java.util.UUID;

import static org.jooq.impl.DSL.field;
import static org.jooq.impl.DSL.table;

@Singleton
public class PostgresRepository implements NoteRepository {

    private static Table<Record> NOTES = table("notes");
    private static Field<UUID> NOTES_ID = field("id", UUID.class);

    @Inject
    private DataSource dataSource;

    @Inject
    private DSLContext jooq;

    @Override
    public void saveNote(String uuid, Note note) {
        jooq.insertInto(NOTES).columns(NOTES_ID);
    }

    @Override
    public Note getNote(String uuid) {
        return null;
    }

    @Override
    public List<Note> listNotes(List<String> namespace, int count, int offset) {
        return List.of();
    }
}

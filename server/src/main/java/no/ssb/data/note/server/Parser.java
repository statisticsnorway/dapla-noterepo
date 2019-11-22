package no.ssb.data.note.server;

import no.ssb.data.note.api.Paragraph;

public interface Parser {

    Boolean canParse(Paragraph paragraph);
}

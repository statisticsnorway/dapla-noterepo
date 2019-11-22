package no.ssb.data.note.server.parsing;

import no.ssb.data.note.api.NamedDataset;
import no.ssb.data.note.api.Paragraph;

import java.util.Iterator;
import java.util.Optional;

/**
 * Implement this method to handle NamedDataset/Paragraph convertion.
 */
public interface ParagraphConverter {

    /**
     * Return true if this instance supports parsing the paragraph.
     */
    boolean canHandle(Paragraph paragraph);

    /**
     * Try to generate a paragraph for the inputs
     */
    Optional<Paragraph> generateInput(Iterable<NamedDataset> input);

    /**
     * Try to generate a paragraph for the outputs
     */
    Optional<Paragraph> generateOutput(Iterable<NamedDataset> output);

    /**
     * Parse the inputs from a paragraph.
     *
     * @return an iterator over the parsed inputs
     */
    Iterator<NamedDataset> parseInput(Paragraph paragraph);

    /**
     * Parse the outputs from a paragraph.
     *
     * @return an iterator over the parsed outputs
     */
    Iterator<NamedDataset> parseOutput(Paragraph paragraph);

}

package no.ssb.dapla.notes.service.parsing;

import no.ssb.dapla.notes.protobuf.Dataset;
import no.ssb.dapla.notes.protobuf.Paragraph;

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
    Optional<Paragraph> generateInput(Iterable<Dataset> input);

    /**
     * Try to generate a paragraph for the outputs
     */
    Optional<Paragraph> generateOutput(Iterable<Dataset> output);

    /**
     * Parse the inputs from a paragraph.
     *
     * @return an iterator over the parsed inputs
     */
    Iterator<Dataset> parseInput(Paragraph paragraph);

    /**
     * Parse the outputs from a paragraph.
     *
     * @return an iterator over the parsed outputs
     */
    Iterator<Dataset> parseOutput(Paragraph paragraph);

}

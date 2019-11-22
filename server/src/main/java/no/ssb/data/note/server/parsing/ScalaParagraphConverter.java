package no.ssb.data.note.server.parsing;

import no.ssb.data.note.api.NamedDataset;
import no.ssb.data.note.api.Paragraph;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ScalaParagraphConverter implements ParagraphConverter {

    private static Pattern INPUT_PATTERN = Pattern.compile("val\\s+(?<inputName>\\w\\w*)\\s+=\\s+spark\\s*\\.read\\s*" +
            "\\.format\\(\"(gsim|no\\.ssb\\.gsim\\.spark)\"\\)\\s*\\.load\\(\"(?<namespace>.+?)\"\\)");

    private static Pattern OUTPUT_PATTERN = Pattern.compile("(?<outputName>\\w\\w*)\\s*\\.spark\\s*\\.write" +
            "\\s*(:?\\.mode\\(\"\\w*\"\\))?\\s*\\.format\\(\"(gsim|no\\.ssb\\.gsim\\.spark)\"\\)\\s*" +
            "\\.save\\(\"(?<namespace>.+?)\"\\)"
    );

    @Override
    public boolean canHandle(Paragraph paragraph) {
        // TODO: Check language
        return true;
    }

    @Override
    public Optional<Paragraph> generateInput(Iterable<NamedDataset> input) {
        return Optional.empty();
    }

    @Override
    public Optional<Paragraph> generateOutput(Iterable<NamedDataset> output) {
        return Optional.empty();
    }

    @Override
    public Iterator<NamedDataset> parseInput(Paragraph paragraph) {
        Matcher matcher = INPUT_PATTERN.matcher(paragraph.getCode());
        return new Iterator<>() {

            boolean hasNext = matcher.find();

            @Override
            public boolean hasNext() {
                return hasNext;
            }

            @Override
            public NamedDataset next() {
                if (!hasNext) {
                    throw new NoSuchElementException();
                }
                String inputName = matcher.group("inputName");
                String namespace = matcher.group("namespace");
                hasNext = matcher.find();
                return NamedDataset.newBuilder().setName(inputName).setUri(namespace)
                        .build();
            }
        };
    }

    @Override
    public Iterator<NamedDataset> parseOutput(Paragraph paragraph) {
        Matcher matcher = OUTPUT_PATTERN.matcher(paragraph.getCode());
        return new Iterator<>() {

            boolean hasNext = matcher.find();

            @Override
            public boolean hasNext() {
                return hasNext;
            }

            @Override
            public NamedDataset next() {
                if (!hasNext) {
                    throw new NoSuchElementException();
                }
                String inputName = matcher.group("outputName");
                String namespace = matcher.group("namespace");
                hasNext = matcher.find();
                return NamedDataset.newBuilder().setName(inputName).setUri(namespace)
                        .build();
            }
        };
    }
}

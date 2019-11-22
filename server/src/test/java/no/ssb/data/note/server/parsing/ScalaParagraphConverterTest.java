package no.ssb.data.note.server.parsing;

import no.ssb.data.note.api.NamedDataset;
import no.ssb.data.note.api.Paragraph;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;


class ScalaParagraphConverterTest {

    private final ScalaParagraphConverter converter = new ScalaParagraphConverter();

    @Test
    void testParseInputs() {

        List<String> inputs = Arrays.asList(
                // Simple input
                "val someInput = spark.read.format(\"gsim\").load(\"someUri\")",
                // Input with spaces here and there.
                "val someInput = spark\t\n.read\t\t.format(\"gsim\")\n.load(\"someUri\")"
        );

        for (String input : inputs) {
            Paragraph paragraph = Paragraph.newBuilder().setCode(input).build();
            assertThat(converter.parseInput(paragraph)).extracting(NamedDataset::getName)
                    .containsExactly("someInput");

            assertThat(converter.parseInput(paragraph)).extracting(NamedDataset::getUri)
                    .containsExactly("someUri");
        }
    }

    @Test
    void testParseOutputs() {

        List<String> outputs = Arrays.asList(
                // Simple output
                "someOutputVariable.spark.write.format(\"gsim\").save(\"someUri\")",
                // Output with spaces here and there.
                "someOutputVariable.spark\t\n.write\t\n.format(\"gsim\")\n.save(\"someUri\")"
        );

        for (String input : outputs) {
            Paragraph paragraph = Paragraph.newBuilder().setCode(input).build();
            assertThat(converter.parseOutput(paragraph)).extracting(NamedDataset::getName)
                    .containsExactly("someOutputVariable");

            assertThat(converter.parseOutput(paragraph)).extracting(NamedDataset::getUri)
                    .containsExactly("someUri");
        }
    }
}
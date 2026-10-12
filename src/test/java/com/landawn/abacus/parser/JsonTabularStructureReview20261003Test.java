package com.landawn.abacus.parser;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.StringReader;
import java.util.List;

import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.exception.ParsingException;
import com.landawn.abacus.util.Dataset;
import com.landawn.abacus.util.Sheet;

public class JsonTabularStructureReview20261003Test extends TestBase {
    private static <T> T read(final boolean reader, final String json, final Class<T> type) {
        final JsonParser parser = ParserFactory.createJsonParser();
        return reader ? parser.deserialize(new StringReader(json), type) : parser.deserialize(json, type);
    }

    private static void rejected(final boolean reader, final Class<?> type, final String... documents) {
        assertAll(
                java.util.Arrays.stream(documents).map(json -> (Executable) () -> assertThrows(ParsingException.class, () -> read(reader, json, type), json)));
    }

    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    public void datasetColumnObjectsRequireSeparators(final boolean reader) {
        for (final String columns : List.of("{\"a\" [1]}", "{\"a\":[1][2]}", "{\"a\":[1],}")) {
            rejected(reader, Dataset.class, "{\"columnNames\":[\"a\"],\"columns\":" + columns + "}");
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    public void sheetColumnObjectsRequireSeparators(final boolean reader) {
        for (final String columns : List.of("{\"a\" [1]}", "{\"a\":[1][2]}", "{\"a\":[1],}")) {
            rejected(reader, Sheet.class, "{\"rowKeySet\":[\"r\"],\"columnKeySet\":[\"a\"],\"columns\":" + columns + "}");
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    public void datasetMetadataRequiresCompleteSeparatedValues(final boolean reader) {
        rejected(reader, Dataset.class, "{\"columnNames\":[\"a\"][\"a\"],\"columns\":{\"a\":[1]}}",
                "{\"columnNames\":[\"a\"],\"columns\":{\"a\":[1]},\"isFrozen\":}",
                "{\"columnNames\":[\"a\"],\"columns\":{\"a\":[1]},\"isFrozen\":\"true\"\"false\"}", "{\"columnNames\":[\"a\"],\"columns\":{\"a\":[1]},}");
    }

    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    public void sheetMetadataRequiresCompleteSeparatedValues(final boolean reader) {
        rejected(reader, Sheet.class, "{\"rowKeySet\":[\"r\"],\"columnKeySet\":[\"a\"][\"a\"],\"columns\":{\"a\":[1]}}",
                "{\"rowKeySet\":[\"r\"],\"columnKeySet\":[\"a\"],\"columns\":{\"a\":[1]},\"isFrozen\":}",
                "{\"rowKeySet\":[\"r\"],\"columnKeySet\":[\"a\"],\"columns\":{\"a\":[1]},\"isFrozen\":\"true\"\"false\"}",
                "{\"rowKeySet\":[\"r\"],\"columnKeySet\":[\"a\"],\"columns\":{\"a\":[1]},}");
    }

    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    public void supportedObjectDialectsAndTrailingMetadataStillWork(final boolean reader) {
        for (final boolean wrapped : new boolean[] { false, true }) {
            final String datasetText = "columnNames:['a'],columns:{a:[1]},properties:{label:'ok'},isFrozen:true";
            final Dataset dataset = read(reader, wrapped ? "{" + datasetText + "}" : datasetText, Dataset.class);
            assertEquals(List.of(1), dataset.getColumn("a"));
            assertEquals("ok", dataset.getProperties().get("label"));
            assertTrue(dataset.isFrozen());

            final String sheetText = "rowKeySet:['r'],columnKeySet:['a'],columns:{a:[1]},isFrozen:true";
            final Sheet<String, String, Integer> sheet = read(reader, wrapped ? "{" + sheetText + "}" : sheetText, Sheet.class);
            assertEquals(Integer.valueOf(1), sheet.get("r", "a"));
            assertTrue(sheet.isFrozen());
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    public void emptyColumnsAndQuotedMetadataRemainSupported(final boolean reader) {
        for (final String flag : List.of("true", "'true'", "\"true\"")) {
            for (final boolean first : new boolean[] { false, true }) {
                final String metadata = "'isFrozen':" + flag;
                final String body = "'columns':{}";
                final String json = "{" + (first ? metadata + "," + body : body + "," + metadata) + "}";
                final Dataset dataset = read(reader, json, Dataset.class);
                assertEquals(0, dataset.columnCount());
                assertTrue(dataset.isFrozen());
                final Sheet<?, ?, ?> sheet = read(reader, json, Sheet.class);
                assertEquals(0, sheet.columnCount());
                assertTrue(sheet.isFrozen());
            }
        }
        assertEquals(0, read(reader, "{}", Dataset.class).columnCount());
        assertEquals(0, read(reader, "{}", Sheet.class).columnCount());
    }
}

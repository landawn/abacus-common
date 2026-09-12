package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.type.Type;
import com.landawn.abacus.util.NoCachingNoUpdating.DisposableArray;
import com.landawn.abacus.util.function.TriConsumer;
import com.landawn.abacus.util.stream.Stream;

public class CsvUtilCsvTest extends CsvUtilTestSupport {

    @Test
    public void testCsvToJson() throws IOException {
        File jsonFile = tempDir.resolve("output.json").toFile();
        assertEquals(5, CsvUtil.csvToJson(testCsvFile, jsonFile));
        String content = Files.readString(jsonFile.toPath());
        assertTrue(content.startsWith("[") && content.endsWith("]"));
        assertTrue(content.contains("\"John\""));
        assertTrue(content.contains("\"name\""));

        File selected = tempDir.resolve("selected.json").toFile();
        assertEquals(5, CsvUtil.csvToJson(testCsvFile, List.of("name", "age"), selected));
        String selectedContent = Files.readString(selected.toPath());
        assertTrue(selectedContent.contains("name") && selectedContent.contains("age"));

        File bean = tempDir.resolve("bean.json").toFile();
        assertEquals(5, CsvUtil.csvToJson(testCsvFile, List.of("id", "name", "age"), bean, Person.class));
        assertTrue(bean.exists());

        StringWriter writer = new StringWriter();
        assertEquals(5, CsvUtil.csvToJson(new StringReader(testCsvContent), null, writer, null));
        assertTrue(writer.toString().contains("John"));

        StringWriter typed = new StringWriter();
        assertEquals(5, CsvUtil.csvToJson(new StringReader(testCsvContent), null, typed, Person.class));
        assertTrue(typed.toString().contains("\"age\":25"));

        StringWriter cols = new StringWriter();
        assertEquals(5, CsvUtil.csvToJson(new StringReader(testCsvContent), List.of("name", "age"), cols, null));
        assertTrue(cols.toString().contains("\"name\""));

        StringWriter empty = new StringWriter();
        assertEquals(0, CsvUtil.csvToJson(new StringReader(""), null, empty, null));
        assertEquals("[]", empty.toString());

        File nullCols = tempDir.resolve("null_cols.json").toFile();
        assertEquals(5, CsvUtil.csvToJson(testCsvFile, null, nullCols));
        String all = Files.readString(nullCols.toPath());
        assertTrue(all.contains("\"id\"") && all.contains("\"active\""));
    }

    @Test
    public void testCsvToJson_EdgeCase() {
        assertThrows(IllegalArgumentException.class,
                () -> CsvUtil.csvToJson(new StringReader("id,name,age\n1,John,25"), List.of("nonexistent"), new StringWriter(), null));

        TrackingReader reader = new TrackingReader(new StringReader("name,age\nJohn,30\nJane,25\n"));
        TrackingWriter writer = new TrackingWriter(new StringWriter());
        assertEquals(2, CsvUtil.csvToJson(reader, null, writer, null));
        assertFalse(reader.closed);
        assertFalse(writer.closed);
        assertTrue(writer.delegate.toString().contains("\"name\":\"John\""));

        StringWriter escaped = new StringWriter();
        CsvUtil.csvToJson(new StringReader("\"a\"\"b\",x\n1,2\n"), null, escaped, null);
        String json = escaped.toString();
        assertTrue(json.contains("\"a\\\"b\""), json);
        List<Map<String, Object>> parsed = N.fromJson(json, List.class);
        assertEquals(1, parsed.size());
        assertEquals("1", parsed.get(0).get("a\"b"));
        assertEquals("2", parsed.get(0).get("x"));

        StringWriter emptySelect = new StringWriter();
        assertEquals(5, CsvUtil.csvToJson(new StringReader(testCsvContent), List.of(), emptySelect, null));
    }

    @Test
    public void testCsvLoader() {
        Dataset fromReader = CsvUtil.loader().source(new StringReader("id,name\n1,John\n2,Jane")).load();
        assertEquals(2, fromReader.columnCount());
        assertEquals(2, fromReader.size());

        Dataset typed = CsvUtil.loader().source(testCsvFile).beanClassForColumnTypeInference(Person.class).load();
        assertEquals(5, typed.size());
        Dataset typeMap = CsvUtil.loader().source(testCsvFile).columnTypeMap(Map.of("id", Type.of(String.class), "age", Type.of(Integer.class))).load();
        assertEquals(5, typeMap.size());
        Dataset readerTyped = CsvUtil.loader()
                .source(new StringReader(testCsvContent))
                .columnTypeMap(Map.of("age", Type.of(Integer.class), "active", Type.of(Boolean.class)))
                .load();
        assertTrue(readerTyped.get(0, 2) instanceof Integer);

        Dataset chained = CsvUtil.loader()
                .source(testCsvFile)
                .selectColumns(List.of("name", "age"))
                .offset(1)
                .count(3)
                .rowFilter(row -> Integer.parseInt(row[2]) > 25)
                .beanClassForColumnTypeInference(Person.class)
                .load();
        assertEquals(2, chained.columnCount());
        assertEquals(2,
                CsvUtil.loader()
                        .source(testCsvFile)
                        .selectColumns(List.of("id", "name"))
                        .offset(1)
                        .count(2)
                        .beanClassForColumnTypeInference(Person.class)
                        .load()
                        .size());
        assertEquals(3,
                CsvUtil.loader()
                        .source(testCsvFile)
                        .beanClassForColumnTypeInference(Person.class)
                        .rowFilter(row -> Integer.parseInt(row[2]) >= 30)
                        .load()
                        .size());
        assertEquals(3,
                CsvUtil.loader().source(testCsvFile).beanClassForColumnTypeInference(Person.class).rowFilter(row -> "true".equals(row[3])).load().size());
        assertTrue(CsvUtil.loader()
                .source(new StringReader(testCsvContent))
                .beanClassForColumnTypeInference(Person.class)
                .rowFilter(row -> Integer.parseInt(row[2]) > 28)
                .load()
                .size() >= 2);
        assertEquals(2,
                CsvUtil.loader()
                        .source(testCsvFile)
                        .columnTypeMap(Map.of("age", Type.of(Integer.class)))
                        .selectColumns(List.of("name", "age"))
                        .load()
                        .columnCount());
        assertEquals(5, CsvUtil.loader().source(testCsvFile).load().size());
        assertEquals(5, CsvUtil.loader().source(new StringReader(testCsvContent)).beanClassForColumnTypeInference(Person.class).load().size());
    }

    @Test
    public void testCsvLoader_RowExtractorAndStream() {
        Dataset extracted = CsvUtil.loader().source(testCsvFile).load((columns, row, output) -> {
            output[0] = row.get(0);
            output[1] = row.get(1).toUpperCase();
            output[2] = Integer.parseInt(row.get(2)) * 2;
            output[3] = Boolean.parseBoolean(row.get(3));
        });
        assertEquals("JOHN", extracted.get(0, 1));
        assertEquals(Integer.valueOf(50), extracted.get(0, 2));
        assertEquals(5, CsvUtil.loader().source(new StringReader(testCsvContent)).load((columns, row, output) -> {
            for (int i = 0; i < row.length(); i++) {
                output[i] = row.get(i);
            }
        }).size());

        BiFunction<List<String>, DisposableArray<String>, String> mapper = (columns, row) -> row.get(1);
        try (Stream<String> stream = CsvUtil.loader().source(testCsvFile).stream((columns, row) -> row.get(1) + ":" + row.get(2))) {
            List<String> results = stream.toList();
            assertEquals(5, results.size());
            assertEquals("John:25", results.get(0));
        }
        try (Stream<String> stream = CsvUtil.loader().source(new StringReader(testCsvContent)).stream(mapper, true)) {
            assertEquals("John", stream.toList().get(0));
        }
        try (Stream<String> stream = CsvUtil.loader()
                .source(testCsvFile)
                .selectColumns(List.of("name", "active"))
                .offset(0)
                .count(10)
                .rowFilter(row -> "true".equals(row[3]))
                .stream((columns, row) -> row.get(1))) {
            assertEquals(3, stream.toList().size());
        }
    }

    @Test
    public void testCsvLoader_CustomParserAndEscape() {
        Function<String, String[]> headerParser = line -> line.split(",");
        BiConsumer<String, String[]> lineParser = (line, output) -> {
            String[] parts = line.split(",");
            System.arraycopy(parts, 0, output, 0, Math.min(parts.length, output.length));
        };
        assertEquals(4, CsvUtil.loader().source(testCsvFile).setHeaderParser(headerParser).load().columnCount());
        assertEquals(5, CsvUtil.loader().source(testCsvFile).setLineParser(lineParser).load().size());
        Dataset escaped = CsvUtil.loader().source(new StringReader("id,name\n1,John")).setEscapeCharToBackSlashForWrite().load();
        assertEquals(2, escaped.columnCount());
        assertFalse(CsvUtil.isBackSlashEscapeCharForWrite());
        assertNotNull(CsvUtil.loader().source(testCsvFile).setEscapeCharToBackSlashForWrite().load());

        try (Stream<String> stream = CsvUtil.loader()
                .source(testCsvFile)
                .setHeaderParser(headerParser)
                .setLineParser(lineParser)
                .stream((columns, row) -> row.get(1))) {
            assertEquals("John", stream.toList().get(0));
        }

        Function<String, String[]> originalHeader = CsvUtil.getCurrentHeaderParser();
        assertThrows(IllegalArgumentException.class, () -> CsvUtil.loader().setHeaderParser(line -> line.split(";")).stream((columns, row) -> row.get(0)));
        assertSame(originalHeader, CsvUtil.getCurrentHeaderParser());

        Function<String, String[]> origHeader = CsvUtil.getCurrentHeaderParser();
        BiConsumer<String, String[]> origLine = CsvUtil.getCurrentLineParser();
        Stream<String> stream = CsvUtil.loader()
                .source(new StringReader("id;name\n1;Ada\n"))
                .setHeaderParser(line -> line.split(";"))
                .setLineParser((line, output) -> {
                    String[] values = line.split(";");
                    System.arraycopy(values, 0, output, 0, values.length);
                })
                .stream((columns, row) -> row.get(1));
        assertSame(origHeader, CsvUtil.getCurrentHeaderParser());
        assertSame(origLine, CsvUtil.getCurrentLineParser());
        try (stream) {
            assertEquals(List.of("Ada"), stream.toList());
        }
    }

    @Test
    public void testCsvLoader_EdgeCase() {
        assertThrows(IllegalArgumentException.class, () -> CsvUtil.loader().load((columns, row, output) -> {
        }));
        assertThrows(IllegalArgumentException.class, () -> CsvUtil.loader().stream((columns, row) -> row.get(0)));
        IllegalArgumentException loadEx = assertThrows(IllegalArgumentException.class, () -> CsvUtil.loader().load());
        assertTrue(loadEx.getMessage().contains("sourceFile"));
        IllegalArgumentException streamEx = assertThrows(IllegalArgumentException.class, () -> CsvUtil.loader().stream((columns, row) -> row.get(0)));
        assertTrue(streamEx.getMessage().contains("sourceReader"));
        assertThrows(IllegalArgumentException.class, () -> CsvUtil.loader().beanClassForColumnTypeInference(Person.class).load());

        Map<String, Type<?>> typeMap = Map.of("id", Type.of(String.class));
        assertThrows(IllegalArgumentException.class,
                () -> CsvUtil.loader().source(testCsvFile).columnTypeMap(typeMap).beanClassForColumnTypeInference(Person.class));
        assertThrows(IllegalArgumentException.class,
                () -> CsvUtil.loader().source(testCsvFile).beanClassForColumnTypeInference(Person.class).columnTypeMap(typeMap));
        assertThrows(IllegalArgumentException.class, () -> CsvUtil.loader().source(testCsvFile).source(new StringReader(testCsvContent)));
        assertThrows(IllegalArgumentException.class, () -> CsvUtil.loader().source(new StringReader(testCsvContent)).source(testCsvFile));
    }

    @Test
    public void testCsvConverter() throws IOException {
        File jsonFile = tempDir.resolve("input.json").toFile();
        CsvUtil.csvToJson(testCsvFile, jsonFile);

        File csvOut = tempDir.resolve("output.csv").toFile();
        assertEquals(5, CsvUtil.converter().source(jsonFile).jsonToCsv(csvOut));
        assertTrue(csvOut.exists());
        StringWriter writer = new StringWriter();
        assertEquals(5, CsvUtil.converter().source(jsonFile).jsonToCsv(writer));
        assertTrue(writer.toString().contains("name"));
        File offsetCsv = tempDir.resolve("offset.csv").toFile();
        assertEquals(2, CsvUtil.converter().source(jsonFile).offset(1).count(2).jsonToCsv(offsetCsv));
        File selectedCsv = tempDir.resolve("selected.csv").toFile();
        assertTrue(CsvUtil.converter().source(jsonFile).selectColumns(List.of("name", "age")).jsonToCsv(selectedCsv) > 0);
        assertTrue(Files.readString(selectedCsv.toPath()).contains("name"));

        StringWriter jsonWriter = new StringWriter();
        assertEquals(5, CsvUtil.converter().source(testCsvFile).csvToJson(jsonWriter));
        assertTrue(jsonWriter.toString().contains("John"));
        assertEquals(5, CsvUtil.converter().source(new StringReader(testCsvContent)).csvToJson(new StringWriter()));
        assertEquals(2, CsvUtil.converter().source(new StringReader(testCsvContent)).offset(1).count(2).csvToJson(new StringWriter()));

        File fileJson = tempDir.resolve("conv.json").toFile();
        assertEquals(5, CsvUtil.converter().source(testCsvFile).csvToJson(fileJson));
        assertTrue(Files.readString(fileJson.toPath()).contains("John"));

        File typed = tempDir.resolve("typed.json").toFile();
        assertEquals(5,
                CsvUtil.converter()
                        .source(testCsvFile)
                        .selectColumns(List.of("id", "name", "age", "active"))
                        .beanClassForColumnTypeInference(Person.class)
                        .csvToJson(typed));
        assertTrue(typed.exists());
        File partial = tempDir.resolve("partial.json").toFile();
        assertEquals(2, CsvUtil.converter().source(testCsvFile).selectColumns(List.of("name", "age")).offset(1).count(2).csvToJson(partial));
        File beanSelect = tempDir.resolve("bean_select.json").toFile();
        assertEquals(5,
                CsvUtil.converter()
                        .source(testCsvFile)
                        .selectColumns(List.of("name", "age"))
                        .beanClassForColumnTypeInference(Person.class)
                        .csvToJson(beanSelect));
        assertTrue(Files.readString(beanSelect.toPath()).contains("\"age\":"));
        assertEquals(5, CsvUtil.converter().source(testCsvFile).selectColumns(List.of("id", "name")).csvToJson(tempDir.resolve("select.json").toFile()));
        assertTrue(CsvUtil.converter().source(testCsvFile).setHeaderParser(line -> line.split(",")).csvToJson(tempDir.resolve("hdr.json").toFile()) > 0);
        BiConsumer<String, String[]> lineParser = (line, output) -> {
            String[] parts = line.split(",");
            System.arraycopy(parts, 0, output, 0, Math.min(parts.length, output.length));
        };
        assertTrue(CsvUtil.converter().source(testCsvFile).setLineParser(lineParser).csvToJson(tempDir.resolve("line.json").toFile()) > 0);
        assertEquals(5, CsvUtil.converter().source(testCsvFile).setEscapeCharToBackSlashForWrite().csvToJson(tempDir.resolve("esc.json").toFile()));
    }

    @Test
    public void testCsvConverter_EdgeCase() throws IOException {
        assertThrows(IllegalArgumentException.class,
                () -> CsvUtil.converter().source(new StringReader("id,name\n1,John")).selectColumns(List.of("nonexistent")).csvToJson(new StringWriter()));
        assertThrows(IllegalArgumentException.class, () -> CsvUtil.converter().csvToJson(tempDir.resolve("no_source.json").toFile()));
        assertThrows(IllegalArgumentException.class, () -> CsvUtil.converter().jsonToCsv(tempDir.resolve("no_source.csv").toFile()));
        assertThrows(IllegalArgumentException.class, () -> CsvUtil.converter().jsonToCsv(new StringWriter()));
        assertThrows(IllegalArgumentException.class, () -> CsvUtil.converter().csvToJson(new StringWriter()));
        assertThrows(IllegalArgumentException.class, () -> CsvUtil.converter().source(testCsvFile).source(new StringReader(testCsvContent)));
        assertThrows(IllegalArgumentException.class, () -> CsvUtil.converter().source(new StringReader(testCsvContent)).source(testCsvFile));
    }

    @Test
    public void testCsvParserBySplitter() {
        assertEquals(List.of("Name", "Age", "City"), List.of(CsvUtil.CSV_HEADER_PARSER_BY_SPLITTER.apply("\"Name\",\"Age\",\"City\"")));
        String[] row = new String[3];
        CsvUtil.CSV_LINE_PARSER_BY_SPLITTER.accept("\"John\",\"30\",\"NYC\"", row);
        assertEquals(List.of("John", "30", "NYC"), List.of(row));
    }

    @Test
    public void testCsvToJson_NullWriterRecyclesBorrowedBufferedReader() {
        List<java.io.BufferedReader> drainedReaders = new ArrayList<>();
        java.io.BufferedReader markerReader = null;
        java.io.BufferedReader nextReader = null;
        try {
            for (int i = 0; i < 64; i++) {
                drainedReaders.add(Objectory.createBufferedReader("drain-" + i));
            }
            markerReader = Objectory.createBufferedReader("marker");
            Objectory.recycle(markerReader);
            assertThrows(IllegalArgumentException.class, () -> CsvUtil.csvToJson(new StringReader("a\n1\n"), null, (java.io.Writer) null, null));
            nextReader = Objectory.createBufferedReader("next");
            assertSame(markerReader, nextReader);
        } finally {
            if (nextReader != null && nextReader != markerReader) {
                Objectory.recycle(nextReader);
            }
            if (markerReader != null) {
                Objectory.recycle(markerReader);
            }
            for (java.io.BufferedReader reader : drainedReaders) {
                Objectory.recycle(reader);
            }
        }
    }

    @Test
    public void testCsvToJson_NullReaderRecyclesBorrowedJsonWriter() {
        List<BufferedJsonWriter> drainedWriters = new ArrayList<>();
        BufferedJsonWriter markerWriter = null;
        BufferedJsonWriter nextWriter = null;
        try {
            for (int i = 0; i < 64; i++) {
                drainedWriters.add(Objectory.createBufferedJsonWriter(new StringWriter()));
            }
            markerWriter = Objectory.createBufferedJsonWriter(new StringWriter());
            Objectory.recycle(markerWriter);
            assertThrows(IllegalArgumentException.class, () -> CsvUtil.csvToJson((java.io.Reader) null, null, new StringWriter(), null));
            nextWriter = Objectory.createBufferedJsonWriter(new StringWriter());
            assertSame(markerWriter, nextWriter);
        } finally {
            if (nextWriter != null && nextWriter != markerWriter) {
                Objectory.recycle(nextWriter);
            }
            if (markerWriter != null) {
                Objectory.recycle(markerWriter);
            }
            for (BufferedJsonWriter writer : drainedWriters) {
                Objectory.recycle(writer);
            }
        }
    }

    @Test
    public void testCsvToJson_FailedConversionLeavesExistingDestinationIntact() throws IOException {
        File jsonFile = tempDir.resolve("destination-kept.json").toFile();
        Files.writeString(jsonFile.toPath(), "PREVIOUS CONTENT");
        assertThrows(IllegalArgumentException.class, () -> CsvUtil.csvToJson(testCsvFile, List.of("nonexistent"), jsonFile));
        assertEquals("PREVIOUS CONTENT", Files.readString(jsonFile.toPath()));
        assertEquals(5, CsvUtil.csvToJson(testCsvFile, List.of("name"), jsonFile));
        assertTrue(Files.readString(jsonFile.toPath()).contains("John"));
    }

    @Test
    public void testCsvLoader_StreamRejectsColumnTypeConfiguration() {
        Map<String, Type<?>> typeMap = Map.of("age", Type.of(Integer.class));
        BiFunction<List<String>, DisposableArray<String>, String> mapper = (columns, row) -> row.get(2);

        IllegalArgumentException beanEx = assertThrows(IllegalArgumentException.class,
                () -> CsvUtil.loader().source(testCsvFile).beanClassForColumnTypeInference(Person.class).stream(mapper));
        assertTrue(beanEx.getMessage().contains("rowMapper"));
        IllegalArgumentException mapEx = assertThrows(IllegalArgumentException.class,
                () -> CsvUtil.loader().source(testCsvFile).columnTypeMap(typeMap).stream(mapper, true));
        assertTrue(mapEx.getMessage().contains("rowMapper"));
        assertThrows(IllegalArgumentException.class,
                () -> CsvUtil.loader().source(new StringReader(testCsvContent)).beanClassForColumnTypeInference(Person.class).stream(mapper, false));

        try (Stream<String> stream = CsvUtil.loader().source(testCsvFile).stream(mapper)) {
            assertEquals(List.of("25", "30", "35", "28", "40"), stream.toList());
        }
    }

    @Test
    public void testCsvLoader_LoadWithRowExtractorRejectsColumnTypeConfiguration() {
        TriConsumer<List<String>, DisposableArray<String>, Object[]> extractor = (columns, row, output) -> {
            for (int i = 0; i < output.length; i++) {
                output[i] = row.get(i);
            }
        };
        Map<String, Type<?>> typeMap = Map.of("age", Type.of(Integer.class));

        IllegalArgumentException beanEx = assertThrows(IllegalArgumentException.class,
                () -> CsvUtil.loader().source(testCsvFile).beanClassForColumnTypeInference(Person.class).load(extractor));
        assertTrue(beanEx.getMessage().contains("rowExtractor"));
        IllegalArgumentException mapEx = assertThrows(IllegalArgumentException.class,
                () -> CsvUtil.loader().source(testCsvFile).columnTypeMap(typeMap).load(extractor));
        assertTrue(mapEx.getMessage().contains("rowExtractor"));
        assertEquals(5, CsvUtil.loader().source(testCsvFile).load(extractor).size());
    }
}

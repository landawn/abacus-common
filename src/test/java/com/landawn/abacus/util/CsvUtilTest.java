package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.exception.ParsingException;
import com.landawn.abacus.type.Type;
import com.landawn.abacus.util.NoCachingNoUpdating.DisposableArray;
import com.landawn.abacus.util.stream.Stream;

public class CsvUtilTest extends CsvUtilTestSupport {

    @Test
    public void testSetHeaderParser() {
        Function<String, String[]> defaultParser = CsvUtil.getCurrentHeaderParser();
        assertNotNull(defaultParser);
        Function<String, String[]> customParser = line -> line.split(";");
        CsvUtil.setHeaderParser(customParser);
        assertSame(customParser, CsvUtil.getCurrentHeaderParser());
        CsvUtil.resetHeaderParser();
        assertEquals(defaultParser, CsvUtil.getCurrentHeaderParser());
        assertThrows(IllegalArgumentException.class, () -> CsvUtil.setHeaderParser(null));
    }

    @Test
    public void testSetLineParser() {
        BiConsumer<String, String[]> defaultParser = CsvUtil.getCurrentLineParser();
        assertNotNull(defaultParser);
        BiConsumer<String, String[]> customParser = (line, output) -> {
            String[] parts = line.split(";");
            System.arraycopy(parts, 0, output, 0, Math.min(parts.length, output.length));
        };
        CsvUtil.setLineParser(customParser);
        assertSame(customParser, CsvUtil.getCurrentLineParser());
        CsvUtil.resetLineParser();
        assertEquals(defaultParser, CsvUtil.getCurrentLineParser());
        assertThrows(IllegalArgumentException.class, () -> CsvUtil.setLineParser(null));
    }

    @Test
    public void testEscapeCharForWrite() {
        assertFalse(CsvUtil.isBackSlashEscapeCharForWrite());
        CsvUtil.setEscapeCharToBackSlashForWrite();
        assertTrue(CsvUtil.isBackSlashEscapeCharForWrite());
        CsvUtil.resetEscapeCharForWrite();
        assertFalse(CsvUtil.isBackSlashEscapeCharForWrite());
    }

    @Test
    public void testWriteField() throws IOException {
        StringWriter sw = new StringWriter();
        BufferedCsvWriter writer = new BufferedCsvWriter(sw);
        CsvUtil.writeField(writer, Type.of(String.class), "test");
        writer.flush();
        assertTrue(sw.toString().contains("test"));

        sw = new StringWriter();
        writer = new BufferedCsvWriter(sw);
        CsvUtil.writeField(writer, Type.of(Integer.class), 42);
        CsvUtil.writeField(writer, Type.of(Long.class), 12345678901L);
        CsvUtil.writeField(writer, Type.of(Double.class), 3.14);
        CsvUtil.writeField(writer, Type.of(Boolean.class), true);
        CsvUtil.writeField(writer, Type.of(String.class), "hello,world");
        CsvUtil.writeField(writer, Type.of(String.class), null);
        CsvUtil.writeField(writer, null, null);
        CsvUtil.writeField(writer, null, "hello");
        writer.flush();
        String out = sw.toString();
        assertTrue(out.contains("42"));
        assertTrue(out.contains("12345678901"));
        assertTrue(out.contains("3.14"));
        assertTrue(out.contains("true"));
        assertTrue(out.contains("hello,world"));
        assertTrue(out.contains("null"));
        assertTrue(out.contains("hello"));
    }

    @Test
    public void testWriteField_BackSlashEscape() throws IOException {
        CsvUtil.setEscapeCharToBackSlashForWrite();
        try {
            StringWriter sw = new StringWriter();
            BufferedCsvWriter writer = new BufferedCsvWriter(sw);
            CsvUtil.writeField(writer, Type.of(String.class), "test\"value");
            writer.flush();
            assertTrue(sw.toString().length() > 0);
        } finally {
            CsvUtil.resetEscapeCharForWrite();
        }

        CsvUtil.setEscapeCharToBackSlashForWrite();
        Function<String, String[]> oldHeader = CsvUtil.getCurrentHeaderParser();
        BiConsumer<String, String[]> oldLine = CsvUtil.getCurrentLineParser();
        CsvParser parser = new CsvParser(',', '"', '\\');
        CsvUtil.setHeaderParser(parser::parseLineToArray);
        CsvUtil.setLineParser(parser::parseLineInto);
        try {
            String value = "c:\\temp\\\"x\".txt";
            StringWriter sw = new StringWriter();
            BufferedCsvWriter w = new BufferedCsvWriter(sw);
            CsvUtil.writeField(w, Type.of(String.class), "path");
            w.write(',');
            CsvUtil.writeField(w, Type.of(String.class), value);
            w.flush();
            Dataset ds = CsvUtil.load(new StringReader("name,value\n" + sw + "\n"));
            assertEquals(1, ds.size());
            assertEquals("path", ds.get(0, 0));
            assertEquals(value, ds.get(0, 1));
        } finally {
            CsvUtil.resetEscapeCharForWrite();
            CsvUtil.setHeaderParser(oldHeader);
            CsvUtil.setLineParser(oldLine);
        }
    }

    @Test
    public void testJsonToCsv() throws IOException {
        File jsonFile = tempDir.resolve("input.json").toFile();
        CsvUtil.csvToJson(testCsvFile, jsonFile);

        File csvFile = tempDir.resolve("output.csv").toFile();
        assertEquals(5, CsvUtil.jsonToCsv(jsonFile, csvFile));
        assertTrue(csvFile.exists());
        String content = Files.readString(csvFile.toPath());
        assertTrue(content.contains("John"));
        assertTrue(content.contains("name"));

        File selected = tempDir.resolve("selected.csv").toFile();
        assertEquals(5, CsvUtil.jsonToCsv(jsonFile, List.of("name", "age"), selected));
        String selectedContent = Files.readString(selected.toPath());
        assertTrue(selectedContent.contains("name"));
        assertTrue(selectedContent.contains("age"));

        StringWriter writer = new StringWriter();
        try (Reader reader = IOUtil.newFileReader(jsonFile)) {
            assertEquals(5, CsvUtil.jsonToCsv(reader, null, writer));
            assertTrue(writer.toString().contains("name"));
        }

        StringWriter selectedWriter = new StringWriter();
        try (Reader reader = IOUtil.newFileReader(jsonFile)) {
            assertTrue(CsvUtil.jsonToCsv(reader, List.of("id", "name"), selectedWriter) > 0);
            assertTrue(selectedWriter.toString().contains("name"));
        }

        StringWriter basic = new StringWriter();
        CsvUtil.jsonToCsv(new StringReader("[{\"id\":\"1\",\"name\":\"John\",\"age\":25},{\"id\":\"2\",\"name\":\"Jane\",\"age\":30}]"),
                List.of("id", "name", "age"), basic);
        assertTrue(basic.toString().contains("John"));
    }

    @Test
    public void testJsonToCsv_EdgeCase() {
        String json = "[{\"id\":\"1\",\"name\":\"John\"},{\"id\":\"2\",\"name\":\"Jane\"}]";
        StringWriter all = new StringWriter();
        assertEquals(2, CsvUtil.jsonToCsv(new StringReader(json), null, all));
        assertTrue(all.toString().contains("id"));
        assertTrue(all.toString().contains("name"));

        StringWriter empty = new StringWriter();
        assertEquals(1, CsvUtil.jsonToCsv(new StringReader("[{\"id\":\"1\",\"name\":\"John\"}]"), List.of(), empty));
        assertEquals("", empty.toString());

        TrackingReader reader = new TrackingReader(new StringReader("[{\"name\":\"John\",\"age\":30},{\"name\":\"Jane\",\"age\":25}]"));
        TrackingWriter writer = new TrackingWriter(new StringWriter());
        assertTrue(CsvUtil.jsonToCsv(reader, null, writer) >= 1);
        assertFalse(reader.closed);
        assertFalse(writer.closed);
        assertTrue(writer.delegate.toString().length() > 0);
    }

    @Test
    public void testLoader() {
        CsvUtil.CsvLoader loader1 = CsvUtil.loader();
        CsvUtil.CsvLoader loader2 = CsvUtil.loader();
        assertNotNull(loader1);
        assertTrue(loader1 != loader2);

        Dataset ds = CsvUtil.loader()
                .source(testCsvFile)
                .selectColumns(List.of("name", "age"))
                .offset(1)
                .count(2)
                .beanClassForColumnTypeInference(Person.class)
                .load();
        assertEquals(2, ds.columnCount());
        assertEquals(2, ds.size());

        Dataset emptySelect = CsvUtil.loader().source(testCsvFile).selectColumns(List.of()).load();
        assertEquals(0, emptySelect.columnCount());

        assertThrows(IllegalArgumentException.class, () -> CsvUtil.loader().offset(-1));
        assertThrows(IllegalArgumentException.class, () -> CsvUtil.loader().count(-1));
    }

    @Test
    public void testLoader_CustomParser() throws IOException {
        File customFile = tempDir.resolve("custom.csv").toFile();
        Files.writeString(customFile.toPath(), "id;name;age;active\n1;John;25;true\n2;Jane;30;false");
        Function<String, String[]> customHeaderParser = line -> line.split(";");
        BiConsumer<String, String[]> customLineParser = (line, output) -> {
            String[] parts = line.split(";");
            System.arraycopy(parts, 0, output, 0, Math.min(parts.length, output.length));
        };
        Dataset ds = CsvUtil.loader()
                .source(customFile)
                .setHeaderParser(customHeaderParser)
                .setLineParser(customLineParser)
                .beanClassForColumnTypeInference(Person.class)
                .load();
        assertEquals(4, ds.columnCount());
        assertEquals(2, ds.size());
    }

    @Test
    public void testConverter() {
        CsvUtil.CsvConverter conv1 = CsvUtil.converter();
        CsvUtil.CsvConverter conv2 = CsvUtil.converter();
        assertNotNull(conv1);
        assertTrue(conv1 != conv2);
        assertThrows(IllegalArgumentException.class, () -> CsvUtil.converter().offset(-1));
        assertThrows(IllegalArgumentException.class, () -> CsvUtil.converter().count(-1));
    }

    @Test
    public void testJsonParser() {
        assertNotNull(CsvUtil.jsonParser);
        Map<String, Object> result = CsvUtil.jsonParser.parse("{\"name\":\"John\",\"age\":30}", Map.class);
        assertEquals("John", result.get("name"));
        assertEquals(30, result.get("age"));
    }

    @Test
    public void testCsvHeaderParser() {
        assertEquals(List.of("a", "b", "c"), List.of(CsvUtil.CSV_HEADER_PARSER.apply("a,b,c")));
        assertEquals(List.of("a", "b,c", "d"), List.of(CsvUtil.CSV_HEADER_PARSER.apply("\"a\",\"b,c\",\"d\"")));
        assertNotNull(CsvUtil.CSV_HEADER_PARSER.apply(""));
        assertEquals(List.of("name"), List.of(CsvUtil.CSV_HEADER_PARSER.apply("name")));
    }

    @Test
    public void testCsvLineParser() {
        String[] simple = new String[3];
        CsvUtil.CSV_LINE_PARSER.accept("John,30,NYC", simple);
        assertEquals(List.of("John", "30", "NYC"), List.of(simple));
        String[] quoted = new String[3];
        CsvUtil.CSV_LINE_PARSER.accept("\"John Doe\",30,\"New York, NY\"", quoted);
        assertEquals("John Doe", quoted[0]);
        assertEquals("New York, NY", quoted[2]);
        String[] empty = new String[3];
        CsvUtil.CSV_LINE_PARSER.accept("John,,NYC", empty);
        assertEquals("", empty[1]);
        String[] trailing = new String[4];
        CsvUtil.CSV_LINE_PARSER.accept("a,b,c,", trailing);
        assertEquals("", trailing[3]);
    }

    @Test
    public void testCsvHeaderParserBySplitter() {
        assertEquals(List.of("a", "b", "c"), List.of(CsvUtil.CSV_HEADER_PARSER_BY_SPLITTER.apply("a,b,c")));
        assertEquals(List.of("name", "age", "city"), List.of(CsvUtil.CSV_HEADER_PARSER_BY_SPLITTER.apply("\"name\",\"age\",\"city\"")));
        assertEquals(List.of("name"), List.of(CsvUtil.CSV_HEADER_PARSER_BY_SPLITTER.apply("name")));
    }

    @Test
    public void testCsvLineParserBySplitter() {
        String[] simple = new String[3];
        CsvUtil.CSV_LINE_PARSER_BY_SPLITTER.accept("John,30,NYC", simple);
        assertEquals(List.of("John", "30", "NYC"), List.of(simple));
        String[] quoted = new String[3];
        CsvUtil.CSV_LINE_PARSER_BY_SPLITTER.accept("\"John\",\"30\",\"NYC\"", quoted);
        assertEquals(List.of("John", "30", "NYC"), List.of(quoted));
        String[] empty = new String[3];
        CsvUtil.CSV_LINE_PARSER_BY_SPLITTER.accept("John,,NYC", empty);
        assertEquals("NYC", empty[2]);

        String[] tooShort = { "P", "Q" };
        CsvUtil.CSV_LINE_PARSER_BY_SPLITTER.accept("1,2,3", tooShort);
        assertEquals(List.of("1", "2"), List.of(tooShort));
        String[] tooLong = { "P", "Q", "R" };
        CsvUtil.CSV_LINE_PARSER_BY_SPLITTER.accept("1,2", tooLong);
        assertEquals(List.of("1", "2", "R"), List.of(tooLong));

        CsvUtil.setLineParser(CsvUtil.CSV_LINE_PARSER_BY_SPLITTER);
        Dataset viaSplitter = CsvUtil.load(new StringReader("a,b\n1,2,3\n"));
        assertEquals(1, viaSplitter.size());
        assertEquals("1", viaSplitter.get(0, 0));
        assertEquals("2", viaSplitter.get(0, 1));
        CsvUtil.resetLineParser();
        ParsingException ex = assertThrows(ParsingException.class, () -> CsvUtil.load(new StringReader("a,b\n1,2,3\n")));
        assertTrue(ex.getMessage().contains("more fields than the expected 2 column(s)"));
    }

    @Test
    public void testCsvHeaderParserInJson() {
        assertEquals(List.of("Name", "Age", "City"), List.of(CsvUtil.CSV_HEADER_PARSER_IN_JSON.apply("[\"Name\",\"Age\",\"City\"]")));
        assertEquals(List.of("Name"), List.of(CsvUtil.CSV_HEADER_PARSER_IN_JSON.apply("[\"Name\"]")));
        String[] line = new String[3];
        CsvUtil.CSV_LINE_PARSER_IN_JSON.accept("[\"John\",\"30\",\"NYC\"]", line);
        assertEquals(List.of("John", "30", "NYC"), List.of(line));
        String[] single = new String[1];
        CsvUtil.CSV_LINE_PARSER_IN_JSON.accept("[\"value\"]", single);
        assertEquals("value", single[0]);
    }

    @Test
    public void testJsonToCsv_FileDestinationCreatesParentDirectory() throws Exception {
        File root = new File(System.getProperty("java.io.tmpdir"), "abacus-csv-parent-" + System.nanoTime());
        try {
            IOUtil.mkdirsIfNotExists(root);
            File src = new File(root, "in.csv");
            IOUtil.write("a,b\n1,2\n", src);

            File jsonOut = new File(new File(root, "json-out"), "out.json");
            assertFalse(jsonOut.getParentFile().exists());
            CsvUtil.csvToJson(src, jsonOut);
            assertTrue(jsonOut.exists());
            assertTrue(IOUtil.readAllToString(jsonOut).contains("\"a\""));

            File csvOut = new File(new File(root, "csv-out"), "out.csv");
            assertFalse(csvOut.getParentFile().exists());
            CsvUtil.jsonToCsv(jsonOut, csvOut);
            assertTrue(csvOut.exists());
            CsvUtil.csvToJson(src, jsonOut);
            assertTrue(jsonOut.exists());
        } finally {
            IOUtil.deleteIfExists(root);
        }
    }

    @Test
    public void testLoad_SelectionAgainstDuplicatedHeader() {
        assertEquals("Column(s) [zzz] not found in CSV header: [a, a]",
                assertThrows(IllegalArgumentException.class, () -> CsvUtil.load(new StringReader("a,a\n1,2\n"), (List<String>) CommonUtil.asList("a", "zzz")))
                        .getMessage());
        assertEquals("Duplicated column names found in: [a, a]",
                assertThrows(IllegalArgumentException.class, () -> CsvUtil.load(new StringReader("a,a\n1,2\n"), (List<String>) CommonUtil.asList("a", "a")))
                        .getMessage());
        assertThrows(IllegalArgumentException.class, () -> CsvUtil.load(new StringReader("a,b\n1,2\n"), (List<String>) CommonUtil.asList("a", "zzz")));
        assertEquals(CommonUtil.asList("a", "b"), CsvUtil.load(new StringReader("a,b\n1,2\n"), (List<String>) CommonUtil.asList("b", "a")).columnNames());
        assertEquals(CommonUtil.asList("a", "b"), CsvUtil.load(new StringReader("a,b\n1,2\n")).columnNames());
        assertEquals(CommonUtil.asList("a"), CsvUtil.load(new StringReader("a,b\n1,2\n"), (List<String>) CommonUtil.asList("a")).columnNames());
    }

    @Test
    public void testLoad_UnterminatedQuoteMessageIsBounded() {
        StringBuilder huge = new StringBuilder("h\n\"");
        for (int i = 0; i < 5_000; i++) {
            huge.append("xxxxxxxxxx");
        }
        Throwable big = assertThrows(Throwable.class, () -> CsvUtil.load(new StringReader(huge.toString())));
        assertTrue(big.getMessage().length() < 1_000, "message length was " + big.getMessage().length());
        assertTrue(big.getMessage().contains("Un-terminated quoted field"));
        assertTrue(big.getMessage().endsWith("..."));
        Throwable small = assertThrows(Throwable.class, () -> CsvUtil.load(new StringReader("h\n\"abc")));
        assertTrue(small.getMessage().endsWith("\"abc"), small.getMessage());
    }

    @Test
    public void testJsonToCsv_JsonNullDoesNotSurviveRoundTrip() {
        StringWriter csv = new StringWriter();
        CsvUtil.jsonToCsv(new StringReader("[{\"a\":null,\"b\":\"x\",\"c\":\"null\"}]"), null, csv);
        String[] lines = csv.toString().split("\\R");
        assertEquals(2, lines.length, csv.toString());
        assertEquals("\"a\",\"b\",\"c\"", lines[0]);
        assertEquals("null,\"x\",\"null\"", lines[1]);

        StringWriter json = new StringWriter();
        CsvUtil.csvToJson(new StringReader(csv.toString()), null, json, null);
        assertEquals("[{\"a\":\"null\",\"b\":\"x\",\"c\":\"null\"}]", json.toString().replaceAll("\\s", ""));

        Dataset both = CsvUtil.load(new StringReader("a,b\nnull,\"null\"\n"));
        assertEquals("null", both.get(0, 0));
        assertEquals("null", both.get(0, 1));
    }

    @Test
    public void testLoaderStream_AlwaysClosesFileSource() throws IOException {
        File owned = tempDir.resolve("owned.csv").toFile();
        Files.writeString(owned.toPath(), "a,b\n1,2\n");
        BiFunction<List<String>, DisposableArray<String>, String> mapper = (columns, row) -> row.get(0);
        try (Stream<String> fileStream = CsvUtil.loader().source(owned).stream(mapper, false)) {
            assertEquals(CommonUtil.asList("1"), fileStream.toList());
        }
        assertTrue(owned.delete(), "the File source's own reader must be closed even when false is passed");

        TrackingReader kept = new TrackingReader(new StringReader("a,b\n1,2\n"));
        try (Stream<String> keptStream = CsvUtil.loader().source(kept).stream(mapper, false)) {
            keptStream.toList();
        }
        assertFalse(kept.closed);

        TrackingReader shut = new TrackingReader(new StringReader("a,b\n1,2\n"));
        try (Stream<String> shutStream = CsvUtil.loader().source(shut).stream(mapper, true)) {
            shutStream.toList();
        }
        assertTrue(shut.closed);
    }
}

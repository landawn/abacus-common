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
    public void testConverter() {
        CsvUtil.CsvConverter conv1 = CsvUtil.converter();
        CsvUtil.CsvConverter conv2 = CsvUtil.converter();
        assertNotNull(conv1);
        assertTrue(conv1 != conv2);
        assertThrows(IllegalArgumentException.class, () -> CsvUtil.converter().offset(-1));
        assertThrows(IllegalArgumentException.class, () -> CsvUtil.converter().count(-1));
    }

    @Test
    public void testCsvHeaderParser() {
        assertEquals(List.of("a", "b", "c"), List.of(CsvUtil.CSV_HEADER_PARSER.apply("a,b,c")));
        assertEquals(List.of("a", "b,c", "d"), List.of(CsvUtil.CSV_HEADER_PARSER.apply("\"a\",\"b,c\",\"d\"")));
        assertNotNull(CsvUtil.CSV_HEADER_PARSER.apply(""));
        assertEquals(List.of("name"), List.of(CsvUtil.CSV_HEADER_PARSER.apply("name")));
    }

    @Test
    public void testCsvHeaderParserBySplitter() {
        assertEquals(List.of("a", "b", "c"), List.of(CsvUtil.CSV_HEADER_PARSER_BY_SPLITTER.apply("a,b,c")));
        assertEquals(List.of("name", "age", "city"), List.of(CsvUtil.CSV_HEADER_PARSER_BY_SPLITTER.apply("\"name\",\"age\",\"city\"")));
        assertEquals(List.of("name"), List.of(CsvUtil.CSV_HEADER_PARSER_BY_SPLITTER.apply("name")));
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
    public void testEscapeCharForWrite() {
        assertFalse(CsvUtil.isBackSlashEscapeCharForWrite());
        CsvUtil.setEscapeCharToBackSlashForWrite();
        assertTrue(CsvUtil.isBackSlashEscapeCharForWrite());
        CsvUtil.resetEscapeCharForWrite();
        assertFalse(CsvUtil.isBackSlashEscapeCharForWrite());
    }

    @Test
    public void testJsonParser() {
        assertNotNull(CsvUtil.jsonParser);
        Map<String, Object> result = CsvUtil.jsonParser.parse("{\"name\":\"John\",\"age\":30}", Map.class);
        assertEquals("John", result.get("name"));
        assertEquals(30, result.get("age"));
    }

    // jsonToCsv

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

    // loader

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

    // ---- deep review 2026-11-02b G035 begin ----
    // Unselected fields hold doubled quotes, text after a closing quote, a quoted line break, padding and a short row.
    private static final String G035_TRICKY_CSV = "id,note,name,memo\r\n" //
            + "1,\"a \"\"q\"\" b\",John,\"multi\r\nline\"\n" //
            + "2,  \"x\" tail ,Jane,   spaced   \n" //
            + "3,,Bob,\"c, d\"  \n" //
            + "4,\"only\"\n";

    private static List<Object> g035Column(final Dataset ds, final String columnName) {
        return new java.util.ArrayList<>(ds.getColumn(columnName));
    }

    // G035-01 (pin): the load engines build only the selected fields when there is no row filter - same values as a full load
    @Test
    public void testLoadSelectedColumns_unselectedFieldsSkipped_r116() {
        final Dataset full = CsvUtil.load(new StringReader(G035_TRICKY_CSV));
        assertEquals(List.of("id", "note", "name", "memo"), full.columnNames());
        assertEquals("a \"q\" b", full.get(0, 1));
        assertEquals("multi\r\nline", full.get(0, 3));
        assertEquals("x tail ", full.get(1, 1));
        assertEquals("spaced", full.get(1, 3));
        assertEquals("c, d", full.get(2, 3));
        assertEquals(null, (Object) full.get(3, 2));

        final Dataset names = CsvUtil.load(new StringReader(G035_TRICKY_CSV), List.of("name"));
        assertEquals(List.of("name"), names.columnNames());
        assertEquals(java.util.Arrays.asList("John", "Jane", "Bob", null), g035Column(names, "name"));

        final Dataset memoId = CsvUtil.load(new StringReader(G035_TRICKY_CSV), List.of("memo", "id"));
        assertEquals(List.of("id", "memo"), memoId.columnNames());
        assertEquals(g035Column(full, "id"), g035Column(memoId, "id"));
        assertEquals(g035Column(full, "memo"), g035Column(memoId, "memo"));

        final Dataset explicitAlwaysTrue = CsvUtil.load(new StringReader(G035_TRICKY_CSV), List.of("memo", "id"), 0, Long.MAX_VALUE, Fn.alwaysTrue());
        assertEquals(g035Column(memoId, "memo"), g035Column(explicitAlwaysTrue, "memo"));

        final Map<String, Type<?>> typeMap = new java.util.HashMap<>();
        typeMap.put("id", Type.of(Integer.class));
        final Dataset typed = CsvUtil.load(new StringReader(G035_TRICKY_CSV), List.of("id", "note"), 1, 2, typeMap);
        assertEquals(List.of(2, 3), g035Column(typed, "id"));
        assertEquals(java.util.Arrays.asList("x tail ", ""), g035Column(typed, "note"));

        final Dataset beanTyped = CsvUtil.load(new StringReader(G035_TRICKY_CSV), List.of("name", "id"), Person.class);
        assertEquals(List.of("id", "name"), beanTyped.columnNames());
        assertEquals(g035Column(full, "name"), g035Column(beanTyped, "name"));

        final Dataset extracted = CsvUtil.load(new StringReader(G035_TRICKY_CSV), List.of("memo", "name"), (columns, row, output) -> {
            output[0] = columns.get(0) + "=" + row.get(0);
            output[1] = columns.get(1) + "=" + row.get(1);
        });
        assertEquals(List.of("name=John", "name=Jane", "name=Bob", "name=null"), g035Column(extracted, "name"));
        assertEquals(List.of("memo=multi\r\nline", "memo=spaced", "memo=c, d", "memo=null"), g035Column(extracted, "memo"));

        final Dataset loaded = CsvUtil.loader().source(new StringReader(G035_TRICKY_CSV)).selectColumns(List.of("note")).load();
        assertEquals(g035Column(full, "note"), g035Column(loaded, "note"));
    }

    // G035-01 (pin): the stream engines and csvToJson build only the selected fields when there is no row filter
    @Test
    public void testStreamAndCsvToJsonSelectedColumns_unselectedFieldsSkipped_r116() {
        assertEquals(java.util.Arrays.asList("John", "Jane", "Bob", null),
                CsvUtil.stream(new StringReader(G035_TRICKY_CSV), List.of("name"), String.class, false).toList());

        final List<Object[]> idMemo = CsvUtil.stream(new StringReader(G035_TRICKY_CSV), List.of("memo", "id"), Object[].class, false).toList();
        assertEquals("[[1, multi\r\nline], [2, spaced], [3, c, d], [4, null]]", java.util.Arrays.deepToString(idMemo.toArray()));

        final List<Map<String, Object>> notes = CsvUtil.stream(new StringReader(G035_TRICKY_CSV), List.of("note"), Clazz.<String, Object> ofMap(), false)
                .toList();
        assertEquals("[{note=a \"q\" b}, {note=x tail }, {note=}, {note=only}]", notes.toString());

        // A null selection with a bean target reads only the columns that map to a property (id, name).
        assertEquals(List.of(new Person("1", "John", null, null), new Person("2", "Jane", null, null), new Person("3", "Bob", null, null),
                new Person("4", null, null, null)), CsvUtil.stream(new StringReader(G035_TRICKY_CSV), null, Person.class, false).toList());

        assertEquals(List.of("[name]=John", "[name]=Jane", "[name]=Bob", "[name]=null"),
                CsvUtil.stream(new StringReader(G035_TRICKY_CSV), List.of("name"), (columns, row) -> columns + "=" + row.get(0), false).toList());

        final StringWriter json = new StringWriter();
        assertEquals(4, CsvUtil.csvToJson(new StringReader(G035_TRICKY_CSV), List.of("memo", "id"), json, null));
        assertEquals("[\n{\"id\":\"1\",\"memo\":\"multi\\r\\nline\"},\n{\"id\":\"2\",\"memo\":\"spaced\"},\n{\"id\":\"3\",\"memo\":\"c, d\"},\n"
                + "{\"id\":\"4\",\"memo\":null}\n]", json.toString());
    }

    // G035-01 (pin): an unselected field is still validated - the same ParsingException as a full read
    @Test
    public void testSelectedColumns_unselectedFieldFailuresUnchanged_r116() {
        // An extra field after the last header column, holding a doubled quote.
        final String overflow = "a,b\n1,2\n3,4,\"x\"\"y\"\n";
        final ParsingException fullFailure = assertThrows(ParsingException.class, () -> CsvUtil.load(new StringReader(overflow)));
        assertEquals("CSV data row has more fields than the expected 2 column(s): 3,4,\"x\"\"y\"", fullFailure.getMessage());

        final ParsingException loadFailure = assertThrows(ParsingException.class, () -> CsvUtil.load(new StringReader(overflow), List.of("a")));
        assertEquals(fullFailure.getMessage(), loadFailure.getMessage());
        assertEquals(fullFailure.getCause().getMessage(), loadFailure.getCause().getMessage());

        final ParsingException streamFailure = assertThrows(ParsingException.class,
                () -> CsvUtil.stream(new StringReader(overflow), List.of("b"), Object[].class, false).toList());
        assertEquals(fullFailure.getMessage(), streamFailure.getMessage());

        final ParsingException jsonFailure = assertThrows(ParsingException.class,
                () -> CsvUtil.csvToJson(new StringReader(overflow), List.of("a"), new StringWriter(), null));
        assertEquals(fullFailure.getMessage(), jsonFailure.getMessage());

        // An unselected quoted field left open at the end of the input.
        final String unterminated = "a,b\n1,\"open\n";
        final ParsingException openFull = assertThrows(ParsingException.class, () -> CsvUtil.load(new StringReader(unterminated)));
        final ParsingException openSelected = assertThrows(ParsingException.class, () -> CsvUtil.load(new StringReader(unterminated), List.of("a")));
        assertEquals(openFull.getMessage(), openSelected.getMessage());

        // count stops before the bad record, with or without a selection.
        assertEquals(1, CsvUtil.load(new StringReader(overflow), List.of("a"), 0, 1).size());
    }

    // G035-01 (pin): a row filter still sees every raw field, unselected ones included
    @Test
    public void testSelectedColumns_rowFilterSeesWholeRawRow_r116() {
        final String csv = "id,name,age\n1,John,25\n2,\"J\"\"ane\",30\n";
        final List<String> seen = new java.util.ArrayList<>();
        final java.util.function.Predicate<String[]> filter = row -> {
            seen.add(java.util.Arrays.toString(row));
            return !"1".equals(row[0]);
        };

        final Dataset ds = CsvUtil.load(new StringReader(csv), List.of("name"), 0, Long.MAX_VALUE, filter);
        assertEquals(List.of("[1, John, 25]", "[2, J\"ane, 30]"), seen);
        assertEquals(List.of("J\"ane"), g035Column(ds, "name"));

        seen.clear();
        assertEquals(List.of("J\"ane"), CsvUtil.stream(new StringReader(csv), List.of("name"), 0, Long.MAX_VALUE, filter, String.class, false).toList());
        assertEquals(List.of("[1, John, 25]", "[2, J\"ane, 30]"), seen);

        seen.clear();
        assertEquals(List.of("[J\"ane]"), CsvUtil
                .stream(new StringReader(csv), List.of("name"), 0, Long.MAX_VALUE, filter, (columns, row) -> java.util.Arrays.toString(row.copy()), false)
                .toList());
        assertEquals(List.of("[1, John, 25]", "[2, J\"ane, 30]"), seen);

        // The public CsvRecordReader API still parses every field.
        final CsvUtil.CsvRecordReader records = new CsvUtil.CsvRecordReader(new StringReader(csv));
        try {
            assertEquals(List.of("id", "name", "age"), List.of(records.readHeader(CsvUtil.CSV_HEADER_PARSER)));
            assertEquals(List.of("[1, John, 25]", "[2, J\"ane, 30]"),
                    records.stream(CsvUtil.CSV_LINE_PARSER, 0, new String[3]).map(java.util.Arrays::toString).toList());
        } catch (final IOException e) {
            throw new java.io.UncheckedIOException(e);
        }
    }
    // ---- deep review 2026-11-02b G035 end ----

    // ---- re-review 2026-11-03 R03 begin ----
    // R03 (pin): an unselected quoted field is scanned without copying its content into the shared field builder (a doubled
    // quote or text after the closing quote no longer appends), so a selected quoted field after it - in the same record or
    // a later one - must still get exactly its own content. Every column subset of a table that mixes doubled quotes, text
    // after the closing quote and plain quoted fields must match a full load, through the load and the stream engines.
    @Test
    public void testSelectedColumns_unselectedQuotedFieldsDoNotTouchFieldBuilder_r116() {
        final String csv = "a,b,c,d\n" //
                + "\"1\"\"x\",\"2\"\"y\"\"\",\"3\" tail,\"\"\"4\"\n" //
                + "\"p\" q,\"r\",\"s\"\"\"\"t\",u\n" //
                + "\"\",\"\"\"\",\"v\" w ,\"x\"\"\" y\n";
        final List<String> columns = List.of("a", "b", "c", "d");
        final Dataset full = CsvUtil.load(new StringReader(csv));

        assertEquals(columns, full.columnNames());
        assertEquals("1\"x", full.get(0, 0));
        assertEquals("2\"y\"", full.get(0, 1));
        assertEquals("3 tail", full.get(0, 2));
        assertEquals("\"4", full.get(0, 3));
        assertEquals("p q", full.get(1, 0));
        assertEquals("s\"\"t", full.get(1, 2));
        assertEquals("", full.get(2, 0));
        assertEquals("\"", full.get(2, 1));
        assertEquals("x\" y", full.get(2, 3));

        for (int mask = 1; mask < 16; mask++) {
            final List<String> selected = new java.util.ArrayList<>();

            for (int i = 0; i < columns.size(); i++) {
                if ((mask & (1 << i)) != 0) {
                    selected.add(columns.get(i));
                }
            }

            final Dataset loaded = CsvUtil.load(new StringReader(csv), selected);
            assertEquals(selected, loaded.columnNames());

            final List<Object[]> streamed = CsvUtil.stream(new StringReader(csv), selected, Object[].class, false).toList();
            assertEquals(full.size(), streamed.size());

            for (int j = 0; j < selected.size(); j++) {
                final String column = selected.get(j);
                final List<Object> expected = g035Column(full, column);
                assertEquals(expected, g035Column(loaded, column), column + " of " + selected);

                for (int row = 0; row < full.size(); row++) {
                    assertEquals(expected.get(row), streamed.get(row)[j], column + " of " + selected + ", row " + row);
                }
            }
        }
    }
    // ---- re-review 2026-11-03 R03 end ----

}

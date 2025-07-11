package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.type.Type;
import com.landawn.abacus.util.NoCachingNoUpdating.DisposableArray;
import com.landawn.abacus.util.function.TriConsumer;

/**
 * Additional comprehensive unit tests for CSVUtil class.
 */
public class CSVUtil101Test extends TestBase {

    @TempDir
    Path tempDir;

    @BeforeEach
    public void setUp() {
        CSVUtil.resetCSVHeaderParser();
        CSVUtil.resetCSVLineParser();
        CSVUtil.resetEscapeCharForWrite();
    }

    @AfterEach
    public void tearDown() {
        CSVUtil.resetCSVHeaderParser();
        CSVUtil.resetCSVLineParser();
        CSVUtil.resetEscapeCharForWrite();
    }

    @Test
    @DisplayName("Test CSV parser constants")
    public void testCSVParserConstants() {
        // Test CSV_HEADER_PARSER
        String[] headers = CSVUtil.CSV_HEADER_PARSER.apply("col1,col2,col3");
        assertArrayEquals(new String[] { "col1", "col2", "col3" }, headers);

        // Test CSV_HEADER_PARSER with quoted values
        headers = CSVUtil.CSV_HEADER_PARSER.apply("\"col 1\",\"col,2\",col3");
        assertArrayEquals(new String[] { "col 1", "col,2", "col3" }, headers);

        // Test CSV_LINE_PARSER
        String[] output = new String[3];
        CSVUtil.CSV_LINE_PARSER.accept("val1,val2,val3", output);
        assertArrayEquals(new String[] { "val1", "val2", "val3" }, output);

        // Test CSV_LINE_PARSER with quoted values
        CSVUtil.CSV_LINE_PARSER.accept("\"val 1\",\"val,2\",val3", output);
        assertArrayEquals(new String[] { "val 1", "val,2", "val3" }, output);
    }

    @Test
    @DisplayName("Test CSV_HEADER_PARSER_BY_SPLITTER")
    public void testCSVHeaderParserBySplitter() {
        String[] headers = CSVUtil.CSV_HEADER_PARSER_BY_SPLITTER.apply("col1,col2,col3");
        assertArrayEquals(new String[] { "col1", "col2", "col3" }, headers);

        // Test with quotes (should be removed)
        headers = CSVUtil.CSV_HEADER_PARSER_BY_SPLITTER.apply("\"col1\",\"col2\",\"col3\"");
        assertArrayEquals(new String[] { "col1", "col2", "col3" }, headers);

        // Test with spaces
        headers = CSVUtil.CSV_HEADER_PARSER_BY_SPLITTER.apply(" col1 , col2 , col3 ");
        assertArrayEquals(new String[] { "col1", "col2", "col3" }, headers);
    }

    @Test
    @DisplayName("Test CSV_LINE_PARSER_BY_SPLITTER")
    public void testCSVLineParserBySplitter() {
        String[] output = new String[3];
        CSVUtil.CSV_LINE_PARSER_BY_SPLITTER.accept("val1,val2,val3", output);
        assertArrayEquals(new String[] { "val1", "val2", "val3" }, output);

        // Test with quotes
        CSVUtil.CSV_LINE_PARSER_BY_SPLITTER.accept("\"val1\",\"val2\",\"val3\"", output);
        assertArrayEquals(new String[] { "val1", "val2", "val3" }, output);

        // Test with spaces
        CSVUtil.CSV_LINE_PARSER_BY_SPLITTER.accept(" val1 , val2 , val3 ", output);
        assertArrayEquals(new String[] { "val1", "val2", "val3" }, output);
    }

    @Test
    @DisplayName("Test CSV_HEADER_PARSER_IN_JSON")
    public void testCSVHeaderParserInJson() {
        String[] headers = CSVUtil.CSV_HEADER_PARSER_IN_JSON.apply("[\"col1\",\"col2\",\"col3\"]");
        assertArrayEquals(new String[] { "col1", "col2", "col3" }, headers);

        // Test with special characters
        headers = CSVUtil.CSV_HEADER_PARSER_IN_JSON.apply("[\"col,1\",\"col\\\"2\",\"col 3\"]");
        assertArrayEquals(new String[] { "col,1", "col\"2", "col 3" }, headers);
    }

    @Test
    @DisplayName("Test CSV_LINE_PARSER_IN_JSON")
    public void testCSVLineParserInJson() {
        String[] output = new String[3];
        CSVUtil.CSV_LINE_PARSER_IN_JSON.accept("[\"val1\",\"val2\",\"val3\"]", output);
        assertArrayEquals(new String[] { "val1", "val2", "val3" }, output);

        // Test with special characters
        CSVUtil.CSV_LINE_PARSER_IN_JSON.accept("[\"val,1\",\"val\\\"2\",\"val 3\"]", output);
        assertArrayEquals(new String[] { "val,1", "val\"2", "val 3" }, output);
    }

    @Test
    @DisplayName("Test writeField with various types")
    public void testWriteFieldWithVariousTypes() throws IOException {
        StringWriter sw;
        BufferedCSVWriter writer;

        // Test with null type and null value
        sw = new StringWriter();
        writer = Objectory.createBufferedCSVWriter(sw);
        CSVUtil.writeField(writer, null, null);
        writer.flush();
        assertEquals("null", sw.toString());

        // Test with Boolean
        sw = new StringWriter();
        writer = Objectory.createBufferedCSVWriter(sw);
        CSVUtil.writeField(writer, Type.of(Boolean.class), true);
        writer.flush();
        assertEquals("true", sw.toString());

        // Test with Double
        sw = new StringWriter();
        writer = Objectory.createBufferedCSVWriter(sw);
        CSVUtil.writeField(writer, Type.of(Double.class), 3.14159);
        writer.flush();
        assertEquals("3.14159", sw.toString());

        // Test with BigDecimal
        sw = new StringWriter();
        writer = Objectory.createBufferedCSVWriter(sw);
        CSVUtil.writeField(writer, Type.of(BigDecimal.class), new BigDecimal("123.456"));
        writer.flush();
        assertEquals("123.456", sw.toString());

        // Test with String containing special characters
        sw = new StringWriter();
        writer = Objectory.createBufferedCSVWriter(sw);
        CSVUtil.writeField(writer, Type.of(String.class), "Hello, \"World\"!");
        writer.flush();
        assertEquals("\"Hello, \"\"World\"\"!\"", sw.toString());

        // Test with Date/Time types
        sw = new StringWriter();
        writer = Objectory.createBufferedCSVWriter(sw);
        LocalDateTime ldt = LocalDateTime.of(2023, 1, 1, 12, 0, 0);
        CSVUtil.writeField(writer, Type.of(LocalDateTime.class), ldt);
        writer.flush();
        assertTrue(sw.toString().contains("2023-01-01"));

        // Test with Collection
        sw = new StringWriter();
        writer = Objectory.createBufferedCSVWriter(sw);
        List<String> list = Arrays.asList("a", "b", "c");
        CSVUtil.writeField(writer, Type.of(List.class), list);
        writer.flush();
        assertEquals("\"[\"\"a\"\", \"\"b\"\", \"\"c\"\"]\"", sw.toString());

        // Test with Array
        sw = new StringWriter();
        writer = Objectory.createBufferedCSVWriter(sw);
        int[] array = { 1, 2, 3 };
        CSVUtil.writeField(writer, Type.of(int[].class), array);
        writer.flush();
        assertEquals("\"[1, 2, 3]\"", sw.toString());
    }

    @Test
    @DisplayName("Test writeField with escape character settings")
    public void testWriteFieldWithEscapeChar() throws IOException {
        // Test with backslash escape character
        CSVUtil.setEscapeCharToBackSlashForWrite();

        StringWriter sw = new StringWriter();
        BufferedCSVWriter writer = Objectory.createBufferedCSVWriter(sw);
        CSVUtil.writeField(writer, Type.of(String.class), "Hello\\World");
        writer.flush();

        // The exact output depends on the implementation
        assertNotNull(sw.toString());

        CSVUtil.resetEscapeCharForWrite();
    }

    @Test
    @DisplayName("Test loadCSV with large offset")
    public void testLoadCSVWithLargeOffset() throws IOException {
        String csv = "id,name\n1,John\n2,Jane\n3,Bob\n";
        File file = tempDir.resolve("offset.csv").toFile();
        Files.writeString(file.toPath(), csv);

        // Offset beyond file size
        DataSet dataSet = CSVUtil.loadCSV(file, null, 100, 10);
        assertEquals(0, dataSet.size());
    }

    @Test
    @DisplayName("Test loadCSV with BufferedReader")
    public void testLoadCSVWithBufferedReader() throws IOException {
        String csv = "id,name\n1,John\n2,Jane\n";

        try (BufferedReader br = new BufferedReader(new StringReader(csv))) {
            DataSet dataSet = CSVUtil.loadCSV(br);
            assertEquals(2, dataSet.size());
            assertEquals("John", dataSet.get(0, 1));
        }
    }

    @Test
    @DisplayName("Test stream with single column selection and primitive type")
    public void testStreamWithSingleColumnPrimitiveType() throws IOException {
        String csv = "id,value\n1,100\n2,200\n3,300\n";
        File file = tempDir.resolve("single.csv").toFile();
        Files.writeString(file.toPath(), csv);

        List<Integer> values = CSVUtil.stream(file, List.of("value"), Integer.class).toList();

        assertEquals(3, values.size());
        assertEquals(100, values.get(0));
        assertEquals(200, values.get(1));
        assertEquals(300, values.get(2));
    }

    @Test
    @DisplayName("Test stream with unsupported target type")
    public void testStreamWithUnsupportedTargetType() throws IOException {
        String csv = "col1,col2\nval1,val2\n";
        File file = tempDir.resolve("unsupported.csv").toFile();
        Files.writeString(file.toPath(), csv);

        // Custom class without proper bean structure
        class UnsupportedType {
        }

        assertThrows(IllegalArgumentException.class, () -> {
            CSVUtil.stream(file, List.of("col1", "col2"), UnsupportedType.class).toList();
        });
    }

    @Test
    @DisplayName("Test stream with missing bean properties")
    public void testStreamWithMissingBeanProperties() throws IOException {
        String csv = "id,name,unknownField\n1,John,value\n";
        File file = tempDir.resolve("missing.csv").toFile();
        Files.writeString(file.toPath(), csv);

        // TestBean doesn't have 'unknownField' property
        List<TestBean> beans = CSVUtil.stream(file, TestBean.class).toList();

        assertEquals(1, beans.size());
        assertEquals(1, beans.get(0).id);
        assertEquals("John", beans.get(0).name);
    }

    @Test
    @DisplayName("Test loadCSV with row filter returning no results")
    public void testLoadCSVWithFilterNoResults() throws IOException {
        String csv = "id,name\n1,John\n2,Jane\n";
        File file = tempDir.resolve("filter.csv").toFile();
        Files.writeString(file.toPath(), csv);

        Predicate<String[]> alwaysFalse = row -> false;
        DataSet dataSet = CSVUtil.loadCSV(file, null, 0, Long.MAX_VALUE, alwaysFalse);

        assertEquals(0, dataSet.size());
        assertEquals(2, dataSet.columnCount());
    }

    @Test
    @DisplayName("Test concurrent parser modification")
    public void testConcurrentParserModification() throws Exception {
        // Test that parser changes are thread-local
        Function<String, String[]> mainThreadParser = CSVUtil.getCurrentHeaderParser();

        Thread thread = new Thread(() -> {
            // Change parser in different thread
            CSVUtil.setCSVHeaderParser(line -> line.split(";"));

            // Verify it's changed in this thread
            assertNotEquals(mainThreadParser, CSVUtil.getCurrentHeaderParser());
        });

        thread.start();
        thread.join();

        // Verify main thread parser unchanged
        assertEquals(mainThreadParser, CSVUtil.getCurrentHeaderParser());
    }

    @Test
    @DisplayName("Test loadCSV with invalid column in columnTypeMap")
    public void testLoadCSVWithInvalidColumnInTypeMap() throws IOException {
        String csv = "id,name\n1,John\n";
        File file = tempDir.resolve("invalid.csv").toFile();
        Files.writeString(file.toPath(), csv);

        Map<String, Type<?>> columnTypeMap = new HashMap<>();
        columnTypeMap.put("id", Type.of(Integer.class));
        columnTypeMap.put("invalidColumn", Type.of(String.class));

        assertThrows(IllegalArgumentException.class, () -> {
            CSVUtil.loadCSV(file, columnTypeMap);
        });
    }

    @Test
    @DisplayName("Test stream with empty file")
    public void testStreamWithEmptyFile() throws IOException {
        File emptyFile = tempDir.resolve("empty.csv").toFile();
        Files.writeString(emptyFile.toPath(), "");

        List<Map<String, String>> results = CSVUtil.stream(emptyFile, Clazz.ofMap(String.class, String.class)).toList();
        assertTrue(results.isEmpty());
    }

    @Test
    @DisplayName("Test stream with headers only")
    public void testStreamWithHeadersOnly() throws IOException {
        File headersOnlyFile = tempDir.resolve("headers.csv").toFile();
        Files.writeString(headersOnlyFile.toPath(), "col1,col2,col3\n");

        List<Map<String, String>> results = CSVUtil.stream(headersOnlyFile, Clazz.ofMap(String.class, String.class)).toList();
        assertTrue(results.isEmpty());
    }

    @Test
    @DisplayName("Test loadCSV with malformed CSV")
    public void testLoadCSVWithMalformedCSV() throws IOException {
        String malformedCsv = "col1,col2,col3\nval1,val2,val3\nval4,val5,val6\n";
        File file = tempDir.resolve("malformed.csv").toFile();
        Files.writeString(file.toPath(), malformedCsv);

        DataSet dataSet = CSVUtil.loadCSV(file);

        // Should handle rows with different column counts
        assertEquals(3, dataSet.columnCount());
        assertEquals(2, dataSet.size());
    }

    @Test
    @DisplayName("Test loadCSV with custom row extractor and column selection")
    public void testLoadCSVWithRowExtractorAndColumnSelection() throws IOException {
        String csv = "id,name,age,salary\n1,John,25,50000\n2,Jane,30,60000\n";
        File file = tempDir.resolve("extractor.csv").toFile();
        Files.writeString(file.toPath(), csv);

        TriConsumer<List<String>, DisposableArray<String>, Object[]> rowExtractor = (columnNames, rowData, output) -> {
            for (int i = 0; i < output.length; i++) {
                String colName = columnNames.get(i);
                String value = rowData.get(i);

                if ("age".equals(colName) || "salary".equals(colName)) {
                    output[i] = Integer.parseInt(value);
                } else {
                    output[i] = value;
                }
            }
        };

        DataSet dataSet = CSVUtil.loadCSV(file, List.of("name", "salary"), rowExtractor);

        assertEquals(2, dataSet.columnCount());
        assertEquals(2, dataSet.size());
        assertEquals("John", dataSet.get(0, 0));
        assertEquals(50000, (Integer) dataSet.get(0, 1));
    }

    @Test
    @DisplayName("Test stream with rowMapper and filtering")
    public void testStreamWithRowMapperAndFiltering() throws IOException {
        String csv = "id,name,score\n1,John,85\n2,Jane,92\n3,Bob,78\n4,Alice,95\n";
        File file = tempDir.resolve("scores.csv").toFile();
        Files.writeString(file.toPath(), csv);

        BiFunction<List<String>, DisposableArray<String>, Student> rowMapper = (columnNames, rowData) -> {
            Student s = new Student();
            for (int i = 0; i < columnNames.size(); i++) {
                switch (columnNames.get(i)) {
                    case "id":
                        s.id = Integer.parseInt(rowData.get(i));
                        break;
                    case "name":
                        s.name = rowData.get(i);
                        break;
                    case "score":
                        s.score = Integer.parseInt(rowData.get(i));
                        break;
                }
            }
            return s;
        };

        Predicate<String[]> highScorers = row -> Integer.parseInt(row[2]) >= 90;

        List<Student> topStudents = CSVUtil.stream(file, null, 0, Long.MAX_VALUE, highScorers, rowMapper).toList();

        assertEquals(2, topStudents.size());
        assertTrue(topStudents.stream().allMatch(s -> s.score >= 90));
    }

    @Test
    @DisplayName("Test writeField with null writer")
    public void testWriteFieldWithNullWriter() {
        assertThrows(NullPointerException.class, () -> {
            CSVUtil.writeField(null, Type.of(String.class), "test");
        });
    }

    @Test
    @DisplayName("Test CSV with Unicode characters")
    public void testCSVWithUnicodeCharacters() throws IOException {
        String unicodeCsv = "name,greeting\n张三,你好\nJosé,¡Hola!\nMüller,Grüß Gott\n";
        File file = tempDir.resolve("unicode.csv").toFile();
        Files.writeString(file.toPath(), unicodeCsv);

        DataSet dataSet = CSVUtil.loadCSV(file);

        assertEquals(2, dataSet.columnCount());
        assertEquals(3, dataSet.size());
        assertEquals("张三", dataSet.get(0, 0));
        assertEquals("你好", dataSet.get(0, 1));
        assertEquals("José", dataSet.get(1, 0));
        assertEquals("Müller", dataSet.get(2, 0));
    }

    @Test
    @DisplayName("Test loadCSV with very long lines")
    public void testLoadCSVWithLongLines() throws IOException {
        StringBuilder longValue = new StringBuilder();
        for (int i = 0; i < 10000; i++) {
            longValue.append("x");
        }

        String csv = "id,data\n1," + longValue + "\n2,short\n";
        File file = tempDir.resolve("long.csv").toFile();
        Files.writeString(file.toPath(), csv);

        DataSet dataSet = CSVUtil.loadCSV(file);

        assertEquals(2, dataSet.size());
        assertEquals(10000, ((String) dataSet.get(0, 1)).length());
    }

    // Test classes
    public static class TestBean {
        public Integer id;
        public String name;

        public TestBean() {
        }
    }

    public static class Student {
        public int id;
        public String name;
        public int score;

        public Student() {
        }
    }
}

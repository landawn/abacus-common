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
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.type.Type;
import com.landawn.abacus.util.NoCachingNoUpdating.DisposableArray;
import com.landawn.abacus.util.function.TriConsumer;
import com.landawn.abacus.util.stream.Stream;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Tag("2025")
public class CSVUtil2025Test extends TestBase {

    @TempDir
    Path tempDir;

    private File testCsvFile;
    private String testCsvContent;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Person {
        private String id;
        private String name;
        private Integer age;
        private Boolean active;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Product {
        private String id;
        private String name;
        private Double price;
    }

    @BeforeEach
    public void setUp() throws IOException {
        testCsvContent = "id,name,age,active\n" + "1,John,25,true\n" + "2,Jane,30,true\n" + "3,Bob,35,false\n" + "4,Alice,28,true\n" + "5,Charlie,40,false\n";

        testCsvFile = tempDir.resolve("test.csv").toFile();
        Files.writeString(testCsvFile.toPath(), testCsvContent);
    }

    @AfterEach
    public void tearDown() {
        CSVUtil.resetHeaderParser();
        CSVUtil.resetLineParser();
        CSVUtil.resetEscapeCharForWrite();
    }

    @Test
    @DisplayName("Test setHeaderParser")
    public void testSetHeaderParser() {
        Function<String, String[]> customParser = line -> line.split(";");
        CSVUtil.setHeaderParser(customParser);
        assertSame(customParser, CSVUtil.getCurrentHeaderParser());
        assertThrows(IllegalArgumentException.class, () -> CSVUtil.setHeaderParser(null));
    }

    @Test
    @DisplayName("Test setLineParser")
    public void testSetLineParser() {
        BiConsumer<String, String[]> customParser = (line, output) -> {
            String[] parts = line.split(";");
            System.arraycopy(parts, 0, output, 0, Math.min(parts.length, output.length));
        };
        CSVUtil.setLineParser(customParser);
        assertSame(customParser, CSVUtil.getCurrentLineParser());
        assertThrows(IllegalArgumentException.class, () -> CSVUtil.setLineParser(null));
    }

    @Test
    @DisplayName("Test resetHeaderParser")
    public void testResetHeaderParser() {
        Function<String, String[]> defaultParser = CSVUtil.getCurrentHeaderParser();
        Function<String, String[]> customParser = line -> line.split(";");
        CSVUtil.setHeaderParser(customParser);
        CSVUtil.resetHeaderParser();
        assertEquals(defaultParser, CSVUtil.getCurrentHeaderParser());
    }

    @Test
    @DisplayName("Test resetLineParser")
    public void testResetLineParser() {
        BiConsumer<String, String[]> defaultParser = CSVUtil.getCurrentLineParser();
        BiConsumer<String, String[]> customParser = (line, output) -> {
        };
        CSVUtil.setLineParser(customParser);
        CSVUtil.resetLineParser();
        assertEquals(defaultParser, CSVUtil.getCurrentLineParser());
    }

    @Test
    @DisplayName("Test getCurrentHeaderParser")
    public void testGetCurrentHeaderParser() {
        Function<String, String[]> parser = CSVUtil.getCurrentHeaderParser();
        assertNotNull(parser);
    }

    @Test
    @DisplayName("Test getCurrentLineParser")
    public void testGetCurrentLineParser() {
        BiConsumer<String, String[]> parser = CSVUtil.getCurrentLineParser();
        assertNotNull(parser);
    }

    @Test
    @DisplayName("Test setEscapeCharToBackSlashForWrite")
    public void testSetEscapeCharToBackSlashForWrite() {
        CSVUtil.setEscapeCharToBackSlashForWrite();
        assertTrue(CSVUtil.isBackSlashEscapeCharForWrite());
    }

    @Test
    @DisplayName("Test resetEscapeCharForWrite")
    public void testResetEscapeCharForWrite() {
        CSVUtil.setEscapeCharToBackSlashForWrite();
        CSVUtil.resetEscapeCharForWrite();
        assertFalse(CSVUtil.isBackSlashEscapeCharForWrite());
    }

    @Test
    @DisplayName("Test writeField")
    public void testWriteField() throws IOException {
        StringWriter sw = new StringWriter();
        BufferedCSVWriter writer = new BufferedCSVWriter(sw);

        CSVUtil.writeField(writer, Type.of(String.class), "test");
        writer.flush();
        assertTrue(sw.toString().contains("test"));

        sw = new StringWriter();
        writer = new BufferedCSVWriter(sw);
        CSVUtil.writeField(writer, Type.of(Integer.class), 42);
        writer.flush();
        assertTrue(sw.toString().contains("42"));

        sw = new StringWriter();
        writer = new BufferedCSVWriter(sw);
        CSVUtil.writeField(writer, null, null);
        writer.flush();
        assertTrue(sw.toString().contains("null"));
    }

    @Test
    @DisplayName("Test loadCSV(File)")
    public void testLoadCSVFromFile() {
        Dataset ds = CSVUtil.loadCSV(testCsvFile);
        assertNotNull(ds);
        assertEquals(4, ds.columnCount());
        assertEquals(5, ds.size());
        assertEquals(List.of("id", "name", "age", "active"), ds.columnNameList());
    }

    @Test
    @DisplayName("Test loadCSV(File, Collection<String>)")
    public void testLoadCSVFromFileWithSelectColumns() {
        List<String> selectColumns = List.of("name", "age");
        Dataset ds = CSVUtil.loadCSV(testCsvFile, selectColumns);
        assertNotNull(ds);
        assertEquals(2, ds.columnCount());
        assertEquals(5, ds.size());
        assertEquals(selectColumns, ds.columnNameList());
    }

    @Test
    @DisplayName("Test loadCSV(File, Collection<String>, long, long)")
    public void testLoadCSVFromFileWithOffsetAndCount() {
        List<String> selectColumns = List.of("name", "age");
        Dataset ds = CSVUtil.loadCSV(testCsvFile, selectColumns, 1, 2);
        assertNotNull(ds);
        assertEquals(2, ds.columnCount());
        assertEquals(2, ds.size());
        assertEquals("Jane", ds.get(0, 0));
        assertEquals("Bob", ds.get(1, 0));
    }

    @Test
    @DisplayName("Test loadCSV(File, Collection<String>, long, long, Predicate)")
    public void testLoadCSVFromFileWithFilter() {
        Predicate<String[]> filter = row -> "true".equals(row[3]);
        Dataset ds = CSVUtil.loadCSV(testCsvFile, null, 0, Long.MAX_VALUE, filter);
        assertNotNull(ds);
        assertEquals(3, ds.size());
    }

    @Test
    @DisplayName("Test loadCSV(Reader)")
    public void testLoadCSVFromReader() {
        Reader reader = new StringReader(testCsvContent);
        Dataset ds = CSVUtil.loadCSV(reader);
        assertNotNull(ds);
        assertEquals(4, ds.columnCount());
        assertEquals(5, ds.size());
    }

    @Test
    @DisplayName("Test loadCSV(Reader, Collection<String>)")
    public void testLoadCSVFromReaderWithSelectColumns() {
        Reader reader = new StringReader(testCsvContent);
        List<String> selectColumns = List.of("name", "age");
        Dataset ds = CSVUtil.loadCSV(reader, selectColumns);
        assertNotNull(ds);
        assertEquals(2, ds.columnCount());
        assertEquals(5, ds.size());
    }

    @Test
    @DisplayName("Test loadCSV(Reader, Collection<String>, long, long)")
    public void testLoadCSVFromReaderWithOffsetAndCount() {
        Reader reader = new StringReader(testCsvContent);
        Dataset ds = CSVUtil.loadCSV(reader, null, 1, 2);
        assertNotNull(ds);
        assertEquals(4, ds.columnCount());
        assertEquals(2, ds.size());
    }

    @Test
    @DisplayName("Test loadCSV(Reader, Collection<String>, long, long, Predicate)")
    public void testLoadCSVFromReaderWithFilter() {
        Reader reader = new StringReader(testCsvContent);
        Predicate<String[]> filter = row -> Integer.parseInt(row[2]) > 30;
        Dataset ds = CSVUtil.loadCSV(reader, null, 0, Long.MAX_VALUE, filter);
        assertNotNull(ds);
        assertEquals(2, ds.size());
    }

    @Test
    @DisplayName("Test loadCSV(File, Class)")
    public void testLoadCSVFromFileWithBeanClass() {
        Dataset ds = CSVUtil.loadCSV(testCsvFile, Person.class);
        assertNotNull(ds);
        assertEquals(4, ds.columnCount());
        assertEquals(5, ds.size());
    }

    @Test
    @DisplayName("Test loadCSV(File, Collection<String>, Class)")
    public void testLoadCSVFromFileWithSelectColumnsAndBeanClass() {
        List<String> selectColumns = List.of("id", "name", "age");
        Dataset ds = CSVUtil.loadCSV(testCsvFile, selectColumns, Person.class);
        assertNotNull(ds);
        assertEquals(3, ds.columnCount());
        assertEquals(5, ds.size());
    }

    @Test
    @DisplayName("Test loadCSV(File, Collection<String>, long, long, Class)")
    public void testLoadCSVFromFileWithOffsetCountAndBeanClass() {
        Dataset ds = CSVUtil.loadCSV(testCsvFile, null, 1, 2, Person.class);
        assertNotNull(ds);
        assertEquals(4, ds.columnCount());
        assertEquals(2, ds.size());
    }

    @Test
    @DisplayName("Test loadCSV(File, Collection<String>, long, long, Predicate, Class)")
    public void testLoadCSVFromFileWithFilterAndBeanClass() {
        Predicate<String[]> filter = row -> "true".equals(row[3]);
        Dataset ds = CSVUtil.loadCSV(testCsvFile, null, 0, Long.MAX_VALUE, filter, Person.class);
        assertNotNull(ds);
        assertEquals(3, ds.size());
    }

    @Test
    @DisplayName("Test loadCSV(Reader, Class)")
    public void testLoadCSVFromReaderWithBeanClass() {
        Reader reader = new StringReader(testCsvContent);
        Dataset ds = CSVUtil.loadCSV(reader, Person.class);
        assertNotNull(ds);
        assertEquals(4, ds.columnCount());
        assertEquals(5, ds.size());
    }

    @Test
    @DisplayName("Test loadCSV(Reader, Collection<String>, Class)")
    public void testLoadCSVFromReaderWithSelectColumnsAndBeanClass() {
        Reader reader = new StringReader(testCsvContent);
        List<String> selectColumns = List.of("id", "name");
        Dataset ds = CSVUtil.loadCSV(reader, selectColumns, Person.class);
        assertNotNull(ds);
        assertEquals(2, ds.columnCount());
        assertEquals(5, ds.size());
    }

    @Test
    @DisplayName("Test loadCSV(Reader, Collection<String>, long, long, Class)")
    public void testLoadCSVFromReaderWithOffsetCountAndBeanClass() {
        Reader reader = new StringReader(testCsvContent);
        Dataset ds = CSVUtil.loadCSV(reader, null, 1, 3, Person.class);
        assertNotNull(ds);
        assertEquals(4, ds.columnCount());
        assertEquals(3, ds.size());
    }

    @Test
    @DisplayName("Test loadCSV(Reader, Collection<String>, long, long, Predicate, Class)")
    public void testLoadCSVFromReaderWithFilterAndBeanClass() {
        Reader reader = new StringReader(testCsvContent);
        Predicate<String[]> filter = row -> Integer.parseInt(row[2]) >= 30;
        Dataset ds = CSVUtil.loadCSV(reader, null, 0, Long.MAX_VALUE, filter, Person.class);
        assertNotNull(ds);
        assertEquals(3, ds.size());
    }

    @Test
    @DisplayName("Test loadCSV(File, Map<String, Type>)")
    public void testLoadCSVFromFileWithTypeMap() {
        Map<String, Type<?>> typeMap = new HashMap<>();
        typeMap.put("id", Type.of(String.class));
        typeMap.put("name", Type.of(String.class));
        typeMap.put("age", Type.of(Integer.class));

        Dataset ds = CSVUtil.loadCSV(testCsvFile, typeMap);
        assertNotNull(ds);
        assertEquals(3, ds.columnCount());
        assertEquals(5, ds.size());
    }

    @Test
    @DisplayName("Test loadCSV(File, long, long, Map<String, Type>)")
    public void testLoadCSVFromFileWithOffsetCountAndTypeMap() {
        Map<String, Type<?>> typeMap = new HashMap<>();
        typeMap.put("id", Type.of(String.class));
        typeMap.put("name", Type.of(String.class));

        Dataset ds = CSVUtil.loadCSV(testCsvFile, 1, 2, typeMap);
        assertNotNull(ds);
        assertEquals(2, ds.columnCount());
        assertEquals(2, ds.size());
    }

    @Test
    @DisplayName("Test loadCSV(File, long, long, Predicate, Map<String, Type>)")
    public void testLoadCSVFromFileWithFilterAndTypeMap() {
        Map<String, Type<?>> typeMap = new HashMap<>();
        typeMap.put("id", Type.of(String.class));
        typeMap.put("name", Type.of(String.class));

        Predicate<String[]> filter = row -> Integer.parseInt(row[0]) > 2;
        Dataset ds = CSVUtil.loadCSV(testCsvFile, 0, Long.MAX_VALUE, filter, typeMap);
        assertNotNull(ds);
        assertEquals(2, ds.columnCount());
        assertEquals(3, ds.size());
    }

    @Test
    @DisplayName("Test loadCSV(Reader, Map<String, Type>)")
    public void testLoadCSVFromReaderWithTypeMap() {
        Reader reader = new StringReader(testCsvContent);
        Map<String, Type<?>> typeMap = new HashMap<>();
        typeMap.put("id", Type.of(String.class));
        typeMap.put("name", Type.of(String.class));

        Dataset ds = CSVUtil.loadCSV(reader, typeMap);
        assertNotNull(ds);
        assertEquals(2, ds.columnCount());
        assertEquals(5, ds.size());
    }

    @Test
    @DisplayName("Test loadCSV(Reader, long, long, Map<String, Type>)")
    public void testLoadCSVFromReaderWithOffsetCountAndTypeMap() {
        Reader reader = new StringReader(testCsvContent);
        Map<String, Type<?>> typeMap = new HashMap<>();
        typeMap.put("id", Type.of(String.class));
        typeMap.put("name", Type.of(String.class));

        Dataset ds = CSVUtil.loadCSV(reader, 0, 3, typeMap);
        assertNotNull(ds);
        assertEquals(2, ds.columnCount());
        assertEquals(3, ds.size());
    }

    @Test
    @DisplayName("Test loadCSV(Reader, long, long, Predicate, Map<String, Type>)")
    public void testLoadCSVFromReaderWithFilterAndTypeMap() {
        Reader reader = new StringReader(testCsvContent);
        Map<String, Type<?>> typeMap = new HashMap<>();
        typeMap.put("id", Type.of(String.class));
        typeMap.put("age", Type.of(Integer.class));

        Predicate<String[]> filter = row -> Integer.parseInt(row[2]) < 35;
        Dataset ds = CSVUtil.loadCSV(reader, 0, Long.MAX_VALUE, filter, typeMap);
        assertNotNull(ds);
        assertEquals(2, ds.columnCount());
        assertEquals(3, ds.size());
    }

    @Test
    @DisplayName("Test loadCSV(File, TriConsumer)")
    public void testLoadCSVFromFileWithRowExtractor() {
        TriConsumer<List<String>, DisposableArray<String>, Object[]> extractor = (columns, row, output) -> {
            output[0] = row.get(0);
            output[1] = row.get(1);
            output[2] = Integer.parseInt(row.get(2));
            output[3] = Boolean.parseBoolean(row.get(3));
        };

        Dataset ds = CSVUtil.loadCSV(testCsvFile, extractor);
        assertNotNull(ds);
        assertEquals(4, ds.columnCount());
        assertEquals(5, ds.size());
    }

    @Test
    @DisplayName("Test loadCSV(File, Collection<String>, TriConsumer)")
    public void testLoadCSVFromFileWithSelectColumnsAndRowExtractor() {
        List<String> selectColumns = List.of("name", "age");
        TriConsumer<List<String>, DisposableArray<String>, Object[]> extractor = (columns, row, output) -> {
            output[0] = row.get(0);
            output[1] = Integer.parseInt(row.get(1));
        };

        Dataset ds = CSVUtil.loadCSV(testCsvFile, selectColumns, extractor);
        assertNotNull(ds);
        assertEquals(2, ds.columnCount());
        assertEquals(5, ds.size());
    }

    @Test
    @DisplayName("Test loadCSV(File, long, long, TriConsumer)")
    public void testLoadCSVFromFileWithOffsetCountAndRowExtractor() {
        TriConsumer<List<String>, DisposableArray<String>, Object[]> extractor = (columns, row, output) -> {
            for (int i = 0; i < row.length(); i++) {
                output[i] = row.get(i);
            }
        };

        Dataset ds = CSVUtil.loadCSV(testCsvFile, 1, 2, extractor);
        assertNotNull(ds);
        assertEquals(4, ds.columnCount());
        assertEquals(2, ds.size());
    }

    @Test
    @DisplayName("Test loadCSV(File, Collection<String>, long, long, Predicate, TriConsumer)")
    public void testLoadCSVFromFileWithAllParamsAndRowExtractor() {
        List<String> selectColumns = List.of("id", "name");
        Predicate<String[]> filter = row -> Integer.parseInt(row[0]) <= 3;
        TriConsumer<List<String>, DisposableArray<String>, Object[]> extractor = (columns, row, output) -> {
            output[0] = row.get(0);
            output[1] = row.get(1);
        };

        Dataset ds = CSVUtil.loadCSV(testCsvFile, selectColumns, 0, Long.MAX_VALUE, filter, extractor);
        assertNotNull(ds);
        assertEquals(2, ds.columnCount());
        assertEquals(3, ds.size());
    }

    @Test
    @DisplayName("Test loadCSV(Reader, TriConsumer)")
    public void testLoadCSVFromReaderWithRowExtractor() {
        Reader reader = new StringReader(testCsvContent);
        TriConsumer<List<String>, DisposableArray<String>, Object[]> extractor = (columns, row, output) -> {
            for (int i = 0; i < row.length(); i++) {
                output[i] = row.get(i);
            }
        };

        Dataset ds = CSVUtil.loadCSV(reader, extractor);
        assertNotNull(ds);
        assertEquals(4, ds.columnCount());
        assertEquals(5, ds.size());
    }

    @Test
    @DisplayName("Test loadCSV(Reader, Collection<String>, TriConsumer)")
    public void testLoadCSVFromReaderWithSelectColumnsAndRowExtractor() {
        Reader reader = new StringReader(testCsvContent);
        List<String> selectColumns = List.of("name", "age");
        TriConsumer<List<String>, DisposableArray<String>, Object[]> extractor = (columns, row, output) -> {
            output[0] = row.get(0);
            output[1] = row.get(1);
        };

        Dataset ds = CSVUtil.loadCSV(reader, selectColumns, extractor);
        assertNotNull(ds);
        assertEquals(2, ds.columnCount());
        assertEquals(5, ds.size());
    }

    @Test
    @DisplayName("Test loadCSV(Reader, long, long, TriConsumer)")
    public void testLoadCSVFromReaderWithOffsetCountAndRowExtractor() {
        Reader reader = new StringReader(testCsvContent);
        TriConsumer<List<String>, DisposableArray<String>, Object[]> extractor = (columns, row, output) -> {
            for (int i = 0; i < row.length(); i++) {
                output[i] = row.get(i);
            }
        };

        Dataset ds = CSVUtil.loadCSV(reader, 1, 3, extractor);
        assertNotNull(ds);
        assertEquals(4, ds.columnCount());
        assertEquals(3, ds.size());
    }

    @Test
    @DisplayName("Test loadCSV(Reader, Collection<String>, long, long, Predicate, TriConsumer)")
    public void testLoadCSVFromReaderWithAllParamsAndRowExtractor() {
        Reader reader = new StringReader(testCsvContent);
        List<String> selectColumns = List.of("id", "age");
        Predicate<String[]> filter = row -> Integer.parseInt(row[2]) >= 30;
        TriConsumer<List<String>, DisposableArray<String>, Object[]> extractor = (columns, row, output) -> {
            output[0] = row.get(0);
            output[1] = Integer.parseInt(row.get(1));
        };

        Dataset ds = CSVUtil.loadCSV(reader, selectColumns, 0, Long.MAX_VALUE, filter, extractor);
        assertNotNull(ds);
        assertEquals(2, ds.columnCount());
        assertEquals(3, ds.size());
    }

    @Test
    @DisplayName("Test stream(File, Class)")
    public void testStreamFromFileWithClass() {
        try (Stream<Person> stream = CSVUtil.stream(testCsvFile, Person.class)) {
            List<Person> persons = stream.toList();
            assertEquals(5, persons.size());
            assertEquals("John", persons.get(0).getName());
        }
    }

    @Test
    @DisplayName("Test stream(File, Collection<String>, Class)")
    public void testStreamFromFileWithSelectColumnsAndClass() {
        List<String> selectColumns = List.of("id", "name", "age");
        try (Stream<Person> stream = CSVUtil.stream(testCsvFile, selectColumns, Person.class)) {
            List<Person> persons = stream.toList();
            assertEquals(5, persons.size());
        }
    }

    @Test
    @DisplayName("Test stream(File, Collection<String>, long, long, Predicate, Class)")
    public void testStreamFromFileWithAllParamsAndClass() {
        Predicate<String[]> filter = row -> Integer.parseInt(row[2]) > 25;
        try (Stream<Person> stream = CSVUtil.stream(testCsvFile, null, 0, Long.MAX_VALUE, filter, Person.class)) {
            List<Person> persons = stream.toList();
            assertEquals(4, persons.size());
        }
    }

    @Test
    @DisplayName("Test stream(Reader, Class, boolean)")
    public void testStreamFromReaderWithClass() {
        Reader reader = new StringReader(testCsvContent);
        try (Stream<Person> stream = CSVUtil.stream(reader, Person.class, true)) {
            List<Person> persons = stream.toList();
            assertEquals(5, persons.size());
        }
    }

    @Test
    @DisplayName("Test stream(Reader, Collection<String>, Class, boolean)")
    public void testStreamFromReaderWithSelectColumnsAndClass() {
        Reader reader = new StringReader(testCsvContent);
        List<String> selectColumns = List.of("id", "name");
        try (Stream<Person> stream = CSVUtil.stream(reader, selectColumns, Person.class, true)) {
            List<Person> persons = stream.toList();
            assertEquals(5, persons.size());
        }
    }

    @Test
    @DisplayName("Test stream(Reader, Collection<String>, long, long, Predicate, Class, boolean)")
    public void testStreamFromReaderWithAllParamsAndClass() {
        Reader reader = new StringReader(testCsvContent);
        Predicate<String[]> filter = row -> "true".equals(row[3]);
        try (Stream<Person> stream = CSVUtil.stream(reader, null, 0, Long.MAX_VALUE, filter, Person.class, true)) {
            List<Person> persons = stream.toList();
            assertEquals(3, persons.size());
        }
    }

    @Test
    @DisplayName("Test stream(File, BiFunction)")
    public void testStreamFromFileWithBiFunction() {
        BiFunction<List<String>, DisposableArray<String>, Person> mapper = (columns, row) -> new Person(row.get(0), row.get(1), Integer.parseInt(row.get(2)),
                Boolean.parseBoolean(row.get(3)));

        try (Stream<Person> stream = CSVUtil.stream(testCsvFile, mapper)) {
            List<Person> persons = stream.toList();
            assertEquals(5, persons.size());
            assertEquals("John", persons.get(0).getName());
        }
    }

    @Test
    @DisplayName("Test stream(File, Collection<String>, BiFunction)")
    public void testStreamFromFileWithSelectColumnsAndBiFunction() {
        List<String> selectColumns = List.of("id", "name", "age");
        BiFunction<List<String>, DisposableArray<String>, Person> mapper = (columns, row) -> {
            Person p = new Person();
            p.setId(row.get(0));
            p.setName(row.get(1));
            p.setAge(Integer.parseInt(row.get(2)));
            return p;
        };

        try (Stream<Person> stream = CSVUtil.stream(testCsvFile, selectColumns, mapper)) {
            List<Person> persons = stream.toList();
            assertEquals(5, persons.size());
        }
    }

    @Test
    @DisplayName("Test stream(File, Collection<String>, long, long, Predicate, BiFunction)")
    public void testStreamFromFileWithAllParamsAndBiFunction() {
        Predicate<String[]> filter = row -> Integer.parseInt(row[0]) <= 3;
        BiFunction<List<String>, DisposableArray<String>, String> mapper = (columns, row) -> row.get(1);

        try (Stream<String> stream = CSVUtil.stream(testCsvFile, null, 0, Long.MAX_VALUE, filter, mapper)) {
            List<String> names = stream.toList();
            assertEquals(3, names.size());
            assertTrue(names.contains("John"));
            assertTrue(names.contains("Jane"));
            assertTrue(names.contains("Bob"));
        }
    }

    @Test
    @DisplayName("Test stream(Reader, BiFunction, boolean)")
    public void testStreamFromReaderWithBiFunction() {
        Reader reader = new StringReader(testCsvContent);
        BiFunction<List<String>, DisposableArray<String>, String> mapper = (columns, row) -> row.get(1);

        try (Stream<String> stream = CSVUtil.stream(reader, mapper, true)) {
            List<String> names = stream.toList();
            assertEquals(5, names.size());
        }
    }

    @Test
    @DisplayName("Test stream(Reader, Collection<String>, BiFunction, boolean)")
    public void testStreamFromReaderWithSelectColumnsAndBiFunction() {
        Reader reader = new StringReader(testCsvContent);
        List<String> selectColumns = List.of("name", "age");
        BiFunction<List<String>, DisposableArray<String>, String> mapper = (columns, row) -> row.get(0) + ":" + row.get(1);

        try (Stream<String> stream = CSVUtil.stream(reader, selectColumns, mapper, true)) {
            List<String> results = stream.toList();
            assertEquals(5, results.size());
            assertEquals("John:25", results.get(0));
        }
    }

    @Test
    @DisplayName("Test stream(Reader, Collection<String>, long, long, Predicate, BiFunction, boolean)")
    public void testStreamFromReaderWithAllParamsAndBiFunction() {
        Reader reader = new StringReader(testCsvContent);
        List<String> selectColumns = List.of("name", "age");
        Predicate<String[]> filter = row -> Integer.parseInt(row[2]) >= 30;
        BiFunction<List<String>, DisposableArray<String>, String> mapper = (columns, row) -> row.get(0);

        try (Stream<String> stream = CSVUtil.stream(reader, selectColumns, 0, Long.MAX_VALUE, filter, mapper, true)) {
            List<String> names = stream.toList();
            assertEquals(3, names.size());
            assertTrue(names.contains("Jane"));
            assertTrue(names.contains("Bob"));
            assertTrue(names.contains("Charlie"));
        }
    }

    @Test
    @DisplayName("Test csv2json(File, File)")
    public void testCsv2json() throws IOException {
        File jsonFile = tempDir.resolve("output.json").toFile();
        long count = CSVUtil.csv2json(testCsvFile, jsonFile);

        assertEquals(5, count);
        assertTrue(jsonFile.exists());
        assertTrue(jsonFile.length() > 0);
    }

    @Test
    @DisplayName("Test csv2json(File, Collection<String>, File)")
    public void testCsv2jsonWithSelectColumns() throws IOException {
        File jsonFile = tempDir.resolve("output.json").toFile();
        List<String> selectColumns = List.of("name", "age");
        long count = CSVUtil.csv2json(testCsvFile, selectColumns, jsonFile);

        assertEquals(5, count);
        assertTrue(jsonFile.exists());
        String content = Files.readString(jsonFile.toPath());
        assertTrue(content.contains("name"));
        assertTrue(content.contains("age"));
    }

    @Test
    @DisplayName("Test csv2json(File, Collection<String>, File, Class)")
    public void testCsv2jsonWithBeanClass() throws IOException {
        File jsonFile = tempDir.resolve("output.json").toFile();
        List<String> selectColumns = List.of("id", "name", "age");
        long count = CSVUtil.csv2json(testCsvFile, selectColumns, jsonFile, Person.class);

        assertEquals(5, count);
        assertTrue(jsonFile.exists());
    }

    @Test
    @DisplayName("Test json2csv(File, File)")
    public void testJson2csv() throws IOException {
        File jsonFile = tempDir.resolve("input.json").toFile();
        CSVUtil.csv2json(testCsvFile, jsonFile);

        File csvFile = tempDir.resolve("output.csv").toFile();
        long count = CSVUtil.json2csv(jsonFile, csvFile);

        assertEquals(5, count);
        assertTrue(csvFile.exists());
        assertTrue(csvFile.length() > 0);
    }

    @Test
    @DisplayName("Test json2csv(File, Collection<String>, File)")
    public void testJson2csvWithSelectHeaders() throws IOException {
        File jsonFile = tempDir.resolve("input.json").toFile();
        CSVUtil.csv2json(testCsvFile, jsonFile);

        File csvFile = tempDir.resolve("output.csv").toFile();
        List<String> selectHeaders = List.of("name", "age");
        long count = CSVUtil.json2csv(jsonFile, selectHeaders, csvFile);

        assertEquals(5, count);
        assertTrue(csvFile.exists());
        String content = Files.readString(csvFile.toPath());
        assertTrue(content.contains("name"));
        assertTrue(content.contains("age"));
    }

    @Test
    @DisplayName("Test loadCSV with empty file")
    public void testLoadCSVWithEmptyFile() throws IOException {
        File emptyFile = tempDir.resolve("empty.csv").toFile();
        Files.writeString(emptyFile.toPath(), "");

        Dataset ds = CSVUtil.loadCSV(emptyFile);
        assertNotNull(ds);
        assertEquals(0, ds.size());
    }

    @Test
    @DisplayName("Test loadCSV with header only")
    public void testLoadCSVWithHeaderOnly() throws IOException {
        File headerOnlyFile = tempDir.resolve("header.csv").toFile();
        Files.writeString(headerOnlyFile.toPath(), "id,name,age\n");

        Dataset ds = CSVUtil.loadCSV(headerOnlyFile);
        assertNotNull(ds);
        assertEquals(3, ds.columnCount());
        assertEquals(0, ds.size());
    }

    @Test
    @DisplayName("Test loadCSV with negative offset throws exception")
    public void testLoadCSVWithNegativeOffset() {
        assertThrows(IllegalArgumentException.class, () -> CSVUtil.loadCSV(testCsvFile, null, -1, 10));
    }

    @Test
    @DisplayName("Test loadCSV with negative count throws exception")
    public void testLoadCSVWithNegativeCount() {
        assertThrows(IllegalArgumentException.class, () -> CSVUtil.loadCSV(testCsvFile, null, 0, -1));
    }

    @Test
    @DisplayName("Test loadCSV with invalid column names throws exception")
    public void testLoadCSVWithInvalidColumnNames() {
        List<String> invalidColumns = List.of("nonexistent");
        assertThrows(IllegalArgumentException.class, () -> CSVUtil.loadCSV(testCsvFile, invalidColumns));
    }

    @Test
    @DisplayName("Test stream with Map type")
    public void testStreamWithMapType() {
        try (Stream<Map> stream = CSVUtil.stream(testCsvFile, Map.class)) {
            List<Map> maps = stream.toList();
            assertEquals(5, maps.size());
            assertTrue(maps.get(0).containsKey("name"));
        }
    }

    @Test
    @DisplayName("Test stream with List type")
    public void testStreamWithListType() {
        try (Stream<List> stream = CSVUtil.stream(testCsvFile, List.class)) {
            List<List> lists = stream.toList();
            assertEquals(5, lists.size());
            assertEquals(4, lists.get(0).size());
        }
    }

    @Test
    @DisplayName("Test stream with Object[] type")
    public void testStreamWithObjectArrayType() {
        try (Stream<Object[]> stream = CSVUtil.stream(testCsvFile, Object[].class)) {
            List<Object[]> arrays = stream.toList();
            assertEquals(5, arrays.size());
            assertEquals(4, arrays.get(0).length);
        }
    }

    @Test
    @DisplayName("Test stream with single column String type")
    public void testStreamWithSingleColumnStringType() {
        List<String> selectColumns = List.of("name");
        try (Stream<String> stream = CSVUtil.stream(testCsvFile, selectColumns, String.class)) {
            List<String> names = stream.toList();
            assertEquals(5, names.size());
            assertEquals("John", names.get(0));
        }
    }
}

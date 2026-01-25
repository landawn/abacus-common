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
public class CsvUtil2025Test extends TestBase {

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
        CsvUtil.resetHeaderParser();
        CsvUtil.resetLineParser();
        CsvUtil.resetEscapeCharForWrite();
    }

    @Test
    @DisplayName("Test setHeaderParser")
    public void testSetHeaderParser() {
        Function<String, String[]> customParser = line -> line.split(";");
        CsvUtil.setHeaderParser(customParser);
        assertSame(customParser, CsvUtil.getCurrentHeaderParser());
        assertThrows(IllegalArgumentException.class, () -> CsvUtil.setHeaderParser(null));
    }

    @Test
    @DisplayName("Test setLineParser")
    public void testSetLineParser() {
        BiConsumer<String, String[]> customParser = (line, output) -> {
            String[] parts = line.split(";");
            System.arraycopy(parts, 0, output, 0, Math.min(parts.length, output.length));
        };
        CsvUtil.setLineParser(customParser);
        assertSame(customParser, CsvUtil.getCurrentLineParser());
        assertThrows(IllegalArgumentException.class, () -> CsvUtil.setLineParser(null));
    }

    @Test
    @DisplayName("Test resetHeaderParser")
    public void testResetHeaderParser() {
        Function<String, String[]> defaultParser = CsvUtil.getCurrentHeaderParser();
        Function<String, String[]> customParser = line -> line.split(";");
        CsvUtil.setHeaderParser(customParser);
        CsvUtil.resetHeaderParser();
        assertEquals(defaultParser, CsvUtil.getCurrentHeaderParser());
    }

    @Test
    @DisplayName("Test resetLineParser")
    public void testResetLineParser() {
        BiConsumer<String, String[]> defaultParser = CsvUtil.getCurrentLineParser();
        BiConsumer<String, String[]> customParser = (line, output) -> {
        };
        CsvUtil.setLineParser(customParser);
        CsvUtil.resetLineParser();
        assertEquals(defaultParser, CsvUtil.getCurrentLineParser());
    }

    @Test
    @DisplayName("Test getCurrentHeaderParser")
    public void testGetCurrentHeaderParser() {
        Function<String, String[]> parser = CsvUtil.getCurrentHeaderParser();
        assertNotNull(parser);
    }

    @Test
    @DisplayName("Test getCurrentLineParser")
    public void testGetCurrentLineParser() {
        BiConsumer<String, String[]> parser = CsvUtil.getCurrentLineParser();
        assertNotNull(parser);
    }

    @Test
    @DisplayName("Test setEscapeCharToBackSlashForWrite")
    public void testSetEscapeCharToBackSlashForWrite() {
        CsvUtil.setEscapeCharToBackSlashForWrite();
        assertTrue(CsvUtil.isBackSlashEscapeCharForWrite());
    }

    @Test
    @DisplayName("Test resetEscapeCharForWrite")
    public void testResetEscapeCharForWrite() {
        CsvUtil.setEscapeCharToBackSlashForWrite();
        CsvUtil.resetEscapeCharForWrite();
        assertFalse(CsvUtil.isBackSlashEscapeCharForWrite());
    }

    @Test
    @DisplayName("Test writeField")
    public void testWriteField() throws IOException {
        StringWriter sw = new StringWriter();
        BufferedCsvWriter writer = new BufferedCsvWriter(sw);

        CsvUtil.writeField(writer, Type.of(String.class), "test");
        writer.flush();
        assertTrue(sw.toString().contains("test"));

        sw = new StringWriter();
        writer = new BufferedCsvWriter(sw);
        CsvUtil.writeField(writer, Type.of(Integer.class), 42);
        writer.flush();
        assertTrue(sw.toString().contains("42"));

        sw = new StringWriter();
        writer = new BufferedCsvWriter(sw);
        CsvUtil.writeField(writer, null, null);
        writer.flush();
        assertTrue(sw.toString().contains("null"));
    }

    @Test
    @DisplayName("Test load(File)")
    public void testLoadCSVFromFile() {
        Dataset ds = CsvUtil.load(testCsvFile);
        assertNotNull(ds);
        assertEquals(4, ds.columnCount());
        assertEquals(5, ds.size());
        assertEquals(List.of("id", "name", "age", "active"), ds.columnNames());
    }

    @Test
    @DisplayName("Test load(File, Collection<String>)")
    public void testLoadCSVFromFileWithSelectColumns() {
        List<String> selectColumns = List.of("name", "age");
        Dataset ds = CsvUtil.load(testCsvFile, selectColumns);
        assertNotNull(ds);
        assertEquals(2, ds.columnCount());
        assertEquals(5, ds.size());
        assertEquals(selectColumns, ds.columnNames());
    }

    @Test
    @DisplayName("Test load(File, Collection<String>, long, long)")
    public void testLoadCSVFromFileWithOffsetAndCount() {
        List<String> selectColumns = List.of("name", "age");
        Dataset ds = CsvUtil.load(testCsvFile, selectColumns, 1, 2);
        assertNotNull(ds);
        assertEquals(2, ds.columnCount());
        assertEquals(2, ds.size());
        assertEquals("Jane", ds.get(0, 0));
        assertEquals("Bob", ds.get(1, 0));
    }

    @Test
    @DisplayName("Test load(File, Collection<String>, long, long, Predicate)")
    public void testLoadCSVFromFileWithFilter() {
        Predicate<String[]> filter = row -> "true".equals(row[3]);
        Dataset ds = CsvUtil.load(testCsvFile, null, 0, Long.MAX_VALUE, filter);
        assertNotNull(ds);
        assertEquals(3, ds.size());
    }

    @Test
    @DisplayName("Test load(Reader)")
    public void testLoadCSVFromReader() {
        Reader reader = new StringReader(testCsvContent);
        Dataset ds = CsvUtil.load(reader);
        assertNotNull(ds);
        assertEquals(4, ds.columnCount());
        assertEquals(5, ds.size());
    }

    @Test
    @DisplayName("Test load(Reader, Collection<String>)")
    public void testLoadCSVFromReaderWithSelectColumns() {
        Reader reader = new StringReader(testCsvContent);
        List<String> selectColumns = List.of("name", "age");
        Dataset ds = CsvUtil.load(reader, selectColumns);
        assertNotNull(ds);
        assertEquals(2, ds.columnCount());
        assertEquals(5, ds.size());
    }

    @Test
    @DisplayName("Test load(Reader, Collection<String>, long, long)")
    public void testLoadCSVFromReaderWithOffsetAndCount() {
        Reader reader = new StringReader(testCsvContent);
        Dataset ds = CsvUtil.load(reader, null, 1, 2);
        assertNotNull(ds);
        assertEquals(4, ds.columnCount());
        assertEquals(2, ds.size());
    }

    @Test
    @DisplayName("Test load(Reader, Collection<String>, long, long, Predicate)")
    public void testLoadCSVFromReaderWithFilter() {
        Reader reader = new StringReader(testCsvContent);
        Predicate<String[]> filter = row -> Integer.parseInt(row[2]) > 30;
        Dataset ds = CsvUtil.load(reader, null, 0, Long.MAX_VALUE, filter);
        assertNotNull(ds);
        assertEquals(2, ds.size());
    }

    @Test
    @DisplayName("Test load(File, Class)")
    public void testLoadCSVFromFileWithBeanClass() {
        Dataset ds = CsvUtil.load(testCsvFile, Person.class);
        assertNotNull(ds);
        assertEquals(4, ds.columnCount());
        assertEquals(5, ds.size());
    }

    @Test
    @DisplayName("Test load(File, Collection<String>, Class)")
    public void testLoadCSVFromFileWithSelectColumnsAndBeanClass() {
        List<String> selectColumns = List.of("id", "name", "age");
        Dataset ds = CsvUtil.load(testCsvFile, selectColumns, Person.class);
        assertNotNull(ds);
        assertEquals(3, ds.columnCount());
        assertEquals(5, ds.size());
    }

    @Test
    @DisplayName("Test load(File, Collection<String>, long, long, Class)")
    public void testLoadCSVFromFileWithOffsetCountAndBeanClass() {
        Dataset ds = CsvUtil.load(testCsvFile, null, 1, 2, Person.class);
        assertNotNull(ds);
        assertEquals(4, ds.columnCount());
        assertEquals(2, ds.size());
    }

    @Test
    @DisplayName("Test load(File, Collection<String>, long, long, Predicate, Class)")
    public void testLoadCSVFromFileWithFilterAndBeanClass() {
        Predicate<String[]> filter = row -> "true".equals(row[3]);
        Dataset ds = CsvUtil.load(testCsvFile, null, 0, Long.MAX_VALUE, filter, Person.class);
        assertNotNull(ds);
        assertEquals(3, ds.size());
    }

    @Test
    @DisplayName("Test load(Reader, Class)")
    public void testLoadCSVFromReaderWithBeanClass() {
        Reader reader = new StringReader(testCsvContent);
        Dataset ds = CsvUtil.load(reader, Person.class);
        assertNotNull(ds);
        assertEquals(4, ds.columnCount());
        assertEquals(5, ds.size());
    }

    @Test
    @DisplayName("Test load(Reader, Collection<String>, Class)")
    public void testLoadCSVFromReaderWithSelectColumnsAndBeanClass() {
        Reader reader = new StringReader(testCsvContent);
        List<String> selectColumns = List.of("id", "name");
        Dataset ds = CsvUtil.load(reader, selectColumns, Person.class);
        assertNotNull(ds);
        assertEquals(2, ds.columnCount());
        assertEquals(5, ds.size());
    }

    @Test
    @DisplayName("Test load(Reader, Collection<String>, long, long, Class)")
    public void testLoadCSVFromReaderWithOffsetCountAndBeanClass() {
        Reader reader = new StringReader(testCsvContent);
        Dataset ds = CsvUtil.load(reader, null, 1, 3, Person.class);
        assertNotNull(ds);
        assertEquals(4, ds.columnCount());
        assertEquals(3, ds.size());
    }

    @Test
    @DisplayName("Test load(Reader, Collection<String>, long, long, Predicate, Class)")
    public void testLoadCSVFromReaderWithFilterAndBeanClass() {
        Reader reader = new StringReader(testCsvContent);
        Predicate<String[]> filter = row -> Integer.parseInt(row[2]) >= 30;
        Dataset ds = CsvUtil.load(reader, null, 0, Long.MAX_VALUE, filter, Person.class);
        assertNotNull(ds);
        assertEquals(3, ds.size());
    }

    @Test
    @DisplayName("Test load(File, Map<String, Type>)")
    public void testLoadCSVFromFileWithTypeMap() {
        Map<String, Type<?>> typeMap = new HashMap<>();
        typeMap.put("id", Type.of(String.class));
        // typeMap.put("name", Type.of(String.class));
        typeMap.put("age", Type.of(Integer.class));

        Dataset ds = CsvUtil.load(testCsvFile, N.asList("id", "name", "age"), 0, Long.MAX_VALUE, typeMap);
        assertNotNull(ds);
        assertEquals(3, ds.columnCount());
        assertEquals(5, ds.size());
    }

    @Test
    @DisplayName("Test load(File, Collection, long, long, Map<String, Type>)")
    public void testLoadCSVFromFileWithOffsetCountAndTypeMap() {
        Map<String, Type<?>> typeMap = new HashMap<>();
        typeMap.put("id", Type.of(String.class));
        typeMap.put("name", Type.of(String.class));

        Dataset ds = CsvUtil.load(testCsvFile, N.asList("id", "name"), 1, 2, typeMap);
        assertNotNull(ds);
        assertEquals(2, ds.columnCount());
        assertEquals(2, ds.size());
    }

    @Test
    @DisplayName("Test load(File, Collection, long, long, Predicate, Map<String, Type>)")
    public void testLoadCSVFromFileWithFilterAndTypeMap() {
        Map<String, Type<?>> typeMap = new HashMap<>();
        typeMap.put("id", Type.of(String.class));
        typeMap.put("name", Type.of(String.class));

        Predicate<String[]> filter = row -> Integer.parseInt(row[0]) > 2;
        Dataset ds = CsvUtil.load(testCsvFile, null, 0, Long.MAX_VALUE, filter, typeMap);
        assertNotNull(ds);
        assertEquals(4, ds.columnCount());
        assertEquals(3, ds.size());
    }

    @Test
    @DisplayName("Test load(Reader, Map<String, Type>)")
    public void testLoadCSVFromReaderWithTypeMap() {
        Reader reader = new StringReader(testCsvContent);
        Map<String, Type<?>> typeMap = new HashMap<>();
        typeMap.put("id", Type.of(String.class));
        typeMap.put("name", Type.of(String.class));

        Dataset ds = CsvUtil.load(reader, typeMap);
        assertNotNull(ds);
        assertEquals(4, ds.columnCount());
        assertEquals(5, ds.size());
    }

    @Test
    @DisplayName("Test load(Reader, Collection, long, long, Map<String, Type>)")
    public void testLoadCSVFromReaderWithOffsetCountAndTypeMap() {
        Reader reader = new StringReader(testCsvContent);
        Map<String, Type<?>> typeMap = new HashMap<>();
        typeMap.put("id", Type.of(String.class));
        typeMap.put("name", Type.of(String.class));

        Dataset ds = CsvUtil.load(reader, N.asList("id", "name", "age"), 0, 3, typeMap);
        assertNotNull(ds);
        assertEquals(3, ds.columnCount());
        assertEquals(3, ds.size());
    }

    @Test
    @DisplayName("Test load(Reader, Collection, long, long, Predicate, Map<String, Type>)")
    public void testLoadCSVFromReaderWithFilterAndTypeMap() {
        Reader reader = new StringReader(testCsvContent);
        Map<String, Type<?>> typeMap = new HashMap<>();
        typeMap.put("id", Type.of(String.class));
        typeMap.put("age", Type.of(Integer.class));

        Predicate<String[]> filter = row -> Integer.parseInt(row[2]) < 35;
        Dataset ds = CsvUtil.load(reader, null, 0, Long.MAX_VALUE, filter, typeMap);
        assertNotNull(ds);
        assertEquals(4, ds.columnCount());
        assertEquals(3, ds.size());
    }

    @Test
    @DisplayName("Test load(File, TriConsumer)")
    public void testLoadCSVFromFileWithRowExtractor() {
        TriConsumer<List<String>, DisposableArray<String>, Object[]> extractor = (columns, row, output) -> {
            output[0] = row.get(0);
            output[1] = row.get(1);
            output[2] = Integer.parseInt(row.get(2));
            output[3] = Boolean.parseBoolean(row.get(3));
        };

        Dataset ds = CsvUtil.load(testCsvFile, extractor);
        assertNotNull(ds);
        assertEquals(4, ds.columnCount());
        assertEquals(5, ds.size());
    }

    @Test
    @DisplayName("Test load(File, Collection<String>, TriConsumer)")
    public void testLoadCSVFromFileWithSelectColumnsAndRowExtractor() {
        List<String> selectColumns = List.of("name", "age");
        TriConsumer<List<String>, DisposableArray<String>, Object[]> extractor = (columns, row, output) -> {
            output[0] = row.get(0);
            output[1] = Integer.parseInt(row.get(1));
        };

        Dataset ds = CsvUtil.load(testCsvFile, selectColumns, extractor);
        assertNotNull(ds);
        assertEquals(2, ds.columnCount());
        assertEquals(5, ds.size());
    }

    @Test
    @DisplayName("Test load(File, long, long, TriConsumer)")
    public void testLoadCSVFromFileWithOffsetCountAndRowExtractor() {
        TriConsumer<List<String>, DisposableArray<String>, Object[]> extractor = (columns, row, output) -> {
            for (int i = 0; i < row.length(); i++) {
                output[i] = row.get(i);
            }
        };

        Dataset ds = CsvUtil.load(testCsvFile, 1, 2, extractor);
        assertNotNull(ds);
        assertEquals(4, ds.columnCount());
        assertEquals(2, ds.size());
    }

    @Test
    @DisplayName("Test load(File, Collection<String>, long, long, Predicate, TriConsumer)")
    public void testLoadCSVFromFileWithAllParamsAndRowExtractor() {
        List<String> selectColumns = List.of("id", "name");
        Predicate<String[]> filter = row -> Integer.parseInt(row[0]) <= 3;
        TriConsumer<List<String>, DisposableArray<String>, Object[]> extractor = (columns, row, output) -> {
            output[0] = row.get(0);
            output[1] = row.get(1);
        };

        Dataset ds = CsvUtil.load(testCsvFile, selectColumns, 0, Long.MAX_VALUE, filter, extractor);
        assertNotNull(ds);
        assertEquals(2, ds.columnCount());
        assertEquals(3, ds.size());
    }

    @Test
    @DisplayName("Test load(Reader, TriConsumer)")
    public void testLoadCSVFromReaderWithRowExtractor() {
        Reader reader = new StringReader(testCsvContent);
        TriConsumer<List<String>, DisposableArray<String>, Object[]> extractor = (columns, row, output) -> {
            for (int i = 0; i < row.length(); i++) {
                output[i] = row.get(i);
            }
        };

        Dataset ds = CsvUtil.load(reader, extractor);
        assertNotNull(ds);
        assertEquals(4, ds.columnCount());
        assertEquals(5, ds.size());
    }

    @Test
    @DisplayName("Test load(Reader, Collection<String>, TriConsumer)")
    public void testLoadCSVFromReaderWithSelectColumnsAndRowExtractor() {
        Reader reader = new StringReader(testCsvContent);
        List<String> selectColumns = List.of("name", "age");
        TriConsumer<List<String>, DisposableArray<String>, Object[]> extractor = (columns, row, output) -> {
            output[0] = row.get(0);
            output[1] = row.get(1);
        };

        Dataset ds = CsvUtil.load(reader, selectColumns, extractor);
        assertNotNull(ds);
        assertEquals(2, ds.columnCount());
        assertEquals(5, ds.size());
    }

    @Test
    @DisplayName("Test load(Reader, long, long, TriConsumer)")
    public void testLoadCSVFromReaderWithOffsetCountAndRowExtractor() {
        Reader reader = new StringReader(testCsvContent);
        TriConsumer<List<String>, DisposableArray<String>, Object[]> extractor = (columns, row, output) -> {
            for (int i = 0; i < row.length(); i++) {
                output[i] = row.get(i);
            }
        };

        Dataset ds = CsvUtil.load(reader, 1, 3, extractor);
        assertNotNull(ds);
        assertEquals(4, ds.columnCount());
        assertEquals(3, ds.size());
    }

    @Test
    @DisplayName("Test load(Reader, Collection<String>, long, long, Predicate, TriConsumer)")
    public void testLoadCSVFromReaderWithAllParamsAndRowExtractor() {
        Reader reader = new StringReader(testCsvContent);
        List<String> selectColumns = List.of("id", "age");
        Predicate<String[]> filter = row -> Integer.parseInt(row[2]) >= 30;
        TriConsumer<List<String>, DisposableArray<String>, Object[]> extractor = (columns, row, output) -> {
            output[0] = row.get(0);
            output[1] = Integer.parseInt(row.get(1));
        };

        Dataset ds = CsvUtil.load(reader, selectColumns, 0, Long.MAX_VALUE, filter, extractor);
        assertNotNull(ds);
        assertEquals(2, ds.columnCount());
        assertEquals(3, ds.size());
    }

    @Test
    @DisplayName("Test stream(File, Class)")
    public void testStreamFromFileWithClass() {
        try (Stream<Person> stream = CsvUtil.stream(testCsvFile, Person.class)) {
            List<Person> persons = stream.toList();
            assertEquals(5, persons.size());
            assertEquals("John", persons.get(0).getName());
        }
    }

    @Test
    @DisplayName("Test stream(File, Collection<String>, Class)")
    public void testStreamFromFileWithSelectColumnsAndClass() {
        List<String> selectColumns = List.of("id", "name", "age");
        try (Stream<Person> stream = CsvUtil.stream(testCsvFile, selectColumns, Person.class)) {
            List<Person> persons = stream.toList();
            assertEquals(5, persons.size());
        }
    }

    @Test
    @DisplayName("Test stream(File, Collection<String>, long, long, Predicate, Class)")
    public void testStreamFromFileWithAllParamsAndClass() {
        Predicate<String[]> filter = row -> Integer.parseInt(row[2]) > 25;
        try (Stream<Person> stream = CsvUtil.stream(testCsvFile, null, 0, Long.MAX_VALUE, filter, Person.class)) {
            List<Person> persons = stream.toList();
            assertEquals(4, persons.size());
        }
    }

    @Test
    @DisplayName("Test stream(Reader, Class, boolean)")
    public void testStreamFromReaderWithClass() {
        Reader reader = new StringReader(testCsvContent);
        try (Stream<Person> stream = CsvUtil.stream(reader, Person.class, true)) {
            List<Person> persons = stream.toList();
            assertEquals(5, persons.size());
        }
    }

    @Test
    @DisplayName("Test stream(Reader, Collection<String>, Class, boolean)")
    public void testStreamFromReaderWithSelectColumnsAndClass() {
        Reader reader = new StringReader(testCsvContent);
        List<String> selectColumns = List.of("id", "name");
        try (Stream<Person> stream = CsvUtil.stream(reader, selectColumns, Person.class, true)) {
            List<Person> persons = stream.toList();
            assertEquals(5, persons.size());
        }
    }

    @Test
    @DisplayName("Test stream(Reader, Collection<String>, long, long, Predicate, Class, boolean)")
    public void testStreamFromReaderWithAllParamsAndClass() {
        Reader reader = new StringReader(testCsvContent);
        Predicate<String[]> filter = row -> "true".equals(row[3]);
        try (Stream<Person> stream = CsvUtil.stream(reader, null, 0, Long.MAX_VALUE, filter, Person.class, true)) {
            List<Person> persons = stream.toList();
            assertEquals(3, persons.size());
        }
    }

    @Test
    @DisplayName("Test stream(File, BiFunction)")
    public void testStreamFromFileWithBiFunction() {
        BiFunction<List<String>, DisposableArray<String>, Person> mapper = (columns, row) -> new Person(row.get(0), row.get(1), Integer.parseInt(row.get(2)),
                Boolean.parseBoolean(row.get(3)));

        try (Stream<Person> stream = CsvUtil.stream(testCsvFile, mapper)) {
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

        try (Stream<Person> stream = CsvUtil.stream(testCsvFile, selectColumns, mapper)) {
            List<Person> persons = stream.toList();
            assertEquals(5, persons.size());
        }
    }

    @Test
    @DisplayName("Test stream(File, Collection<String>, long, long, Predicate, BiFunction)")
    public void testStreamFromFileWithAllParamsAndBiFunction() {
        Predicate<String[]> filter = row -> Integer.parseInt(row[0]) <= 3;
        BiFunction<List<String>, DisposableArray<String>, String> mapper = (columns, row) -> row.get(1);

        try (Stream<String> stream = CsvUtil.stream(testCsvFile, null, 0, Long.MAX_VALUE, filter, mapper)) {
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

        try (Stream<String> stream = CsvUtil.stream(reader, mapper, true)) {
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

        try (Stream<String> stream = CsvUtil.stream(reader, selectColumns, mapper, true)) {
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

        try (Stream<String> stream = CsvUtil.stream(reader, selectColumns, 0, Long.MAX_VALUE, filter, mapper, true)) {
            List<String> names = stream.toList();
            assertEquals(3, names.size());
            assertTrue(names.contains("Jane"));
            assertTrue(names.contains("Bob"));
            assertTrue(names.contains("Charlie"));
        }
    }

    @Test
    @DisplayName("Test csvToJson(File, File)")
    public void testCsv2json() throws IOException {
        File jsonFile = tempDir.resolve("output.json").toFile();
        long count = CsvUtil.csvToJson(testCsvFile, jsonFile);

        assertEquals(5, count);
        assertTrue(jsonFile.exists());
        assertTrue(jsonFile.length() > 0);
    }

    @Test
    @DisplayName("Test csvToJson(File, Collection<String>, File)")
    public void testCsv2jsonWithSelectColumns() throws IOException {
        File jsonFile = tempDir.resolve("output.json").toFile();
        List<String> selectColumns = List.of("name", "age");
        long count = CsvUtil.csvToJson(testCsvFile, selectColumns, jsonFile);

        assertEquals(5, count);
        assertTrue(jsonFile.exists());
        String content = Files.readString(jsonFile.toPath());
        assertTrue(content.contains("name"));
        assertTrue(content.contains("age"));
    }

    @Test
    @DisplayName("Test csvToJson(File, Collection<String>, File, Class)")
    public void testCsv2jsonWithBeanClass() throws IOException {
        File jsonFile = tempDir.resolve("output.json").toFile();
        List<String> selectColumns = List.of("id", "name", "age");
        long count = CsvUtil.csvToJson(testCsvFile, selectColumns, jsonFile, Person.class);

        assertEquals(5, count);
        assertTrue(jsonFile.exists());
    }

    @Test
    @DisplayName("Test jsonToCsv(File, File)")
    public void testJson2csv() throws IOException {
        File jsonFile = tempDir.resolve("input.json").toFile();
        CsvUtil.csvToJson(testCsvFile, jsonFile);

        File csvFile = tempDir.resolve("output.csv").toFile();
        long count = CsvUtil.jsonToCsv(jsonFile, csvFile);

        assertEquals(5, count);
        assertTrue(csvFile.exists());
        assertTrue(csvFile.length() > 0);
    }

    @Test
    @DisplayName("Test jsonToCsv(File, Collection<String>, File)")
    public void testJson2csvWithSelectHeaders() throws IOException {
        File jsonFile = tempDir.resolve("input.json").toFile();
        CsvUtil.csvToJson(testCsvFile, jsonFile);

        File csvFile = tempDir.resolve("output.csv").toFile();
        List<String> selectHeaders = List.of("name", "age");
        long count = CsvUtil.jsonToCsv(jsonFile, selectHeaders, csvFile);

        assertEquals(5, count);
        assertTrue(csvFile.exists());
        String content = Files.readString(csvFile.toPath());
        assertTrue(content.contains("name"));
        assertTrue(content.contains("age"));
    }

    @Test
    @DisplayName("Test load with empty file")
    public void testLoadCSVWithEmptyFile() throws IOException {
        File emptyFile = tempDir.resolve("empty.csv").toFile();
        Files.writeString(emptyFile.toPath(), "");

        Dataset ds = CsvUtil.load(emptyFile);
        assertNotNull(ds);
        assertEquals(0, ds.size());
    }

    @Test
    @DisplayName("Test load with header only")
    public void testLoadCSVWithHeaderOnly() throws IOException {
        File headerOnlyFile = tempDir.resolve("header.csv").toFile();
        Files.writeString(headerOnlyFile.toPath(), "id,name,age\n");

        Dataset ds = CsvUtil.load(headerOnlyFile);
        assertNotNull(ds);
        assertEquals(3, ds.columnCount());
        assertEquals(0, ds.size());
    }

    @Test
    @DisplayName("Test load with negative offset throws exception")
    public void testLoadCSVWithNegativeOffset() {
        assertThrows(IllegalArgumentException.class, () -> CsvUtil.load(testCsvFile, null, -1, 10));
    }

    @Test
    @DisplayName("Test load with negative count throws exception")
    public void testLoadCSVWithNegativeCount() {
        assertThrows(IllegalArgumentException.class, () -> CsvUtil.load(testCsvFile, null, 0, -1));
    }

    @Test
    @DisplayName("Test load with invalid column names throws exception")
    public void testLoadCSVWithInvalidColumnNames() {
        List<String> invalidColumns = List.of("nonexistent");
        assertThrows(IllegalArgumentException.class, () -> CsvUtil.load(testCsvFile, invalidColumns));
    }

    @Test
    @DisplayName("Test stream with Map type")
    public void testStreamWithMapType() {
        try (Stream<Map> stream = CsvUtil.stream(testCsvFile, Map.class)) {
            List<Map> maps = stream.toList();
            assertEquals(5, maps.size());
            assertTrue(maps.get(0).containsKey("name"));
        }
    }

    @Test
    @DisplayName("Test stream with List type")
    public void testStreamWithListType() {
        try (Stream<List> stream = CsvUtil.stream(testCsvFile, List.class)) {
            List<List> lists = stream.toList();
            assertEquals(5, lists.size());
            assertEquals(4, lists.get(0).size());
        }
    }

    @Test
    @DisplayName("Test stream with Object[] type")
    public void testStreamWithObjectArrayType() {
        try (Stream<Object[]> stream = CsvUtil.stream(testCsvFile, Object[].class)) {
            List<Object[]> arrays = stream.toList();
            assertEquals(5, arrays.size());
            assertEquals(4, arrays.get(0).length);
        }
    }

    @Test
    @DisplayName("Test stream with single column String type")
    public void testStreamWithSingleColumnStringType() {
        List<String> selectColumns = List.of("name");
        try (Stream<String> stream = CsvUtil.stream(testCsvFile, selectColumns, String.class)) {
            List<String> names = stream.toList();
            assertEquals(5, names.size());
            assertEquals("John", names.get(0));
        }
    }

    // ===================== Tests for loader() and CSVLoader =====================

    @Test
    @DisplayName("Test loader() returns CSVLoader instance")
    public void testLoader() {
        CsvUtil.CSVLoader loader = CsvUtil.loader();
        assertNotNull(loader);
    }

    @Test
    @DisplayName("Test CSVLoader with columnTypeMap")
    public void testCSVLoaderWithColumnTypeMap() {
        Map<String, Type<?>> typeMap = new HashMap<>();
        typeMap.put("id", Type.of(String.class));
        typeMap.put("age", Type.of(Integer.class));

        Dataset ds = CsvUtil.loader().source(testCsvFile).columnTypeMap(typeMap).load();

        assertNotNull(ds);
        assertEquals(4, ds.columnCount());
        assertEquals(5, ds.size());
    }

    @Test
    @DisplayName("Test CSVLoader with beanClassForTypeReading")
    public void testCSVLoaderWithBeanClass() {
        Dataset ds = CsvUtil.loader().source(testCsvFile).beanClassForColumnTypeInference(Person.class).load();

        assertNotNull(ds);
        assertEquals(4, ds.columnCount());
        assertEquals(5, ds.size());
    }

    @Test
    @DisplayName("Test CSVLoader with rowFilter")
    public void testCSVLoaderWithRowFilter() {
        Predicate<String[]> filter = row -> Integer.parseInt(row[2]) >= 30;

        Dataset ds = CsvUtil.loader().source(testCsvFile).beanClassForColumnTypeInference(Person.class).rowFilter(filter).load();

        assertNotNull(ds);
        assertEquals(3, ds.size());
    }

    @Test
    @DisplayName("Test CSVLoader with rowExtractor")
    public void testCSVLoaderWithRowExtractor() {
        TriConsumer<List<String>, DisposableArray<String>, Object[]> extractor = (columns, row, output) -> {
            output[0] = row.get(0);
            output[1] = row.get(1).toUpperCase();
            output[2] = Integer.parseInt(row.get(2)) * 2;
            output[3] = Boolean.parseBoolean(row.get(3));
        };

        Dataset ds = CsvUtil.loader().source(testCsvFile).load(extractor);

        assertNotNull(ds);
        assertEquals(4, ds.columnCount());
        assertEquals(5, ds.size());
        assertEquals("JOHN", ds.get(0, 1));
        assertEquals(Integer.valueOf(50), ds.get(0, 2));
    }

    @Test
    @DisplayName("Test CSVLoader chained methods")
    public void testCSVLoaderChainedMethods() {
        List<String> selectColumns = List.of("name", "age");
        Predicate<String[]> filter = row -> Integer.parseInt(row[2]) > 25;

        Dataset ds = CsvUtil.loader()
                .source(testCsvFile)
                .selectColumns(selectColumns)
                .offset(1)
                .count(3)
                .rowFilter(filter)
                .beanClassForColumnTypeInference(Person.class)
                .load();

        assertNotNull(ds);
        assertEquals(2, ds.columnCount());
    }

    @Test
    @DisplayName("Test CSVLoader from Reader source")
    public void testCSVLoaderFromReader() {
        Reader reader = new StringReader(testCsvContent);

        Dataset ds = CsvUtil.loader().source(reader).beanClassForColumnTypeInference(Person.class).load();

        assertNotNull(ds);
        assertEquals(4, ds.columnCount());
        assertEquals(5, ds.size());
    }

    @Test
    @DisplayName("Test CSVLoader selectColumns, offset, count")
    public void testLoaderSelectColumnsOffsetCount() {
        Dataset ds = CsvUtil.loader()
                .source(testCsvFile)
                .selectColumns(List.of("name", "age"))
                .offset(1)
                .count(2)
                .beanClassForColumnTypeInference(Person.class)
                .load();

        assertNotNull(ds);
        assertEquals(2, ds.columnCount());
        assertEquals(2, ds.size());
    }

    @Test
    @DisplayName("Test CSVLoader with custom header parser")
    public void testLoaderWithCustomHeaderParser() {
        String customCsv = "id;name;age;active\n1;John;25;true\n2;Jane;30;false";
        File customFile;
        try {
            customFile = tempDir.resolve("custom.csv").toFile();
            Files.writeString(customFile.toPath(), customCsv);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

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

        assertNotNull(ds);
        assertEquals(4, ds.columnCount());
        assertEquals(2, ds.size());
    }

    @Test
    @DisplayName("Test CSVLoader stream method with rowMapper")
    public void testCSVLoaderStreamWithRowMapper() {
        BiFunction<List<String>, DisposableArray<String>, String> mapper = (columns, row) -> row.get(1) + ":" + row.get(2);

        try (Stream<String> stream = CsvUtil.loader().source(testCsvFile).stream(mapper)) {
            List<String> results = stream.toList();
            assertEquals(5, results.size());
            assertEquals("John:25", results.get(0));
        }
    }

    @Test
    @DisplayName("Test CSVLoader stream with closeReaderWhenStreamIsClosed")
    public void testCSVLoaderStreamCloseReader() {
        Reader reader = new StringReader(testCsvContent);
        BiFunction<List<String>, DisposableArray<String>, String> mapper = (columns, row) -> row.get(1);

        try (Stream<String> stream = CsvUtil.loader().source(reader).stream(mapper, true)) {
            List<String> names = stream.toList();
            assertEquals(5, names.size());
        }
    }

    @Test
    @DisplayName("Test CSVLoader throws exception without source")
    public void testCSVLoaderWithoutSource() {
        assertThrows(IllegalArgumentException.class, () -> CsvUtil.loader().beanClassForColumnTypeInference(Person.class).load());
    }

    @Test
    @DisplayName("Test CSVLoader throws exception without type config")
    public void testCSVLoaderWithoutTypeConfig() {
        assertThrows(IllegalArgumentException.class, () -> CsvUtil.loader().source(testCsvFile).load());
    }

    @Test
    @DisplayName("Test CSVLoader cannot set both columnTypeMap and beanClass")
    public void testCSVLoaderMutuallyExclusiveTypeConfig() {
        Map<String, Type<?>> typeMap = new HashMap<>();
        typeMap.put("id", Type.of(String.class));

        assertThrows(IllegalArgumentException.class,
                () -> CsvUtil.loader().source(testCsvFile).columnTypeMap(typeMap).beanClassForColumnTypeInference(Person.class));
    }

    @Test
    @DisplayName("Test CSVLoader cannot set both file and reader source")
    public void testCSVLoaderCannotSetBothSources() {
        Reader reader = new StringReader(testCsvContent);

        assertThrows(IllegalArgumentException.class, () -> CsvUtil.loader().source(testCsvFile).source(reader));
    }

    // ===================== Tests for converter() and CSVConverter =====================

    @Test
    @DisplayName("Test converter() returns CSVConverter instance")
    public void testConverter() {
        CsvUtil.CSVConverter converter = CsvUtil.converter();
        assertNotNull(converter);
    }

    @Test
    @DisplayName("Test CSVConverter csvToJson with File output")
    public void testCSVConverterCsv2JsonFile() throws IOException {
        File jsonFile = tempDir.resolve("output.json").toFile();

        long count = CsvUtil.converter().source(testCsvFile).csvToJson(jsonFile);

        assertEquals(5, count);
        assertTrue(jsonFile.exists());
        String content = Files.readString(jsonFile.toPath());
        assertTrue(content.contains("John"));

        N.println(IOUtil.readAllToString(testCsvFile));
        N.println(Strings.repeat("=", 80));
        N.println(IOUtil.readAllToString(jsonFile));
    }

    @Test
    @DisplayName("Test CSVConverter csvToJson with Writer output")
    public void testCSVConverterCsv2JsonWriter() {
        StringWriter writer = new StringWriter();

        long count = CsvUtil.converter().source(testCsvFile).csvToJson(writer);

        assertEquals(5, count);
        String content = writer.toString();
        assertTrue(content.contains("John"));
        assertTrue(content.contains("["));
        assertTrue(content.contains("]"));
    }

    @Test
    @DisplayName("Test CSVConverter jsonToCsv with File output")
    public void testCSVConverterJson2CsvFile() throws IOException {
        File jsonFile = tempDir.resolve("input.json").toFile();
        CsvUtil.csvToJson(testCsvFile, jsonFile);

        File csvOutput = tempDir.resolve("output.csv").toFile();

        long count = CsvUtil.converter().source(jsonFile).jsonToCsv(csvOutput);

        assertEquals(5, count);
        assertTrue(csvOutput.exists());
    }

    @Test
    @DisplayName("Test CSVConverter jsonToCsv with Writer output")
    public void testCSVConverterJson2CsvWriter() throws IOException {
        File jsonFile = tempDir.resolve("input.json").toFile();
        CsvUtil.csvToJson(testCsvFile, jsonFile);

        StringWriter writer = new StringWriter();

        long count = CsvUtil.converter().source(jsonFile).jsonToCsv(writer);

        assertEquals(5, count);
        String content = writer.toString();
        assertTrue(content.contains("name"));
    }

    @Test
    @DisplayName("Test CSVConverter with beanClassForTypeWriting")
    public void testCSVConverterWithBeanClassForTypeWriting() throws IOException {
        File jsonFile = tempDir.resolve("typed_output.json").toFile();

        long count = CsvUtil.converter()
                .source(testCsvFile)
                .selectColumns(List.of("id", "name", "age", "active"))
                .beanClassForColumnTypeInference(Person.class)
                .csvToJson(jsonFile);

        assertEquals(5, count);
        assertTrue(jsonFile.exists());

        N.println(IOUtil.readAllToString(testCsvFile));
        N.println(Strings.repeat("=", 80));
        N.println(IOUtil.readAllToString(jsonFile));
    }

    @Test
    @DisplayName("Test CSVConverter with selectColumns and offset/count")
    public void testCSVConverterWithSelectColumnsOffsetCount() throws IOException {
        File jsonFile = tempDir.resolve("partial.json").toFile();

        long count = CsvUtil.converter().source(testCsvFile).selectColumns(List.of("name", "age")).offset(1).count(2).csvToJson(jsonFile);

        assertEquals(2, count);
    }

    @Test
    @DisplayName("Test CSVConverter with Reader source")
    public void testCSVConverterWithReaderSource() {
        Reader reader = new StringReader(testCsvContent);
        StringWriter writer = new StringWriter();

        long count = CsvUtil.converter().source(reader).csvToJson(writer);

        assertEquals(5, count);
    }

    @Test
    @DisplayName("Test CSVConverter throws exception without source")
    public void testCSVConverterWithoutSource() throws IOException {
        File jsonFile = tempDir.resolve("output.json").toFile();

        assertThrows(IllegalArgumentException.class, () -> CsvUtil.converter().csvToJson(jsonFile));
    }

    @Test
    @DisplayName("Test CSVConverter setEscapeCharToBackSlashForWrite")
    public void testCSVConverterSetEscapeChar() throws IOException {
        File jsonFile = tempDir.resolve("escaped.json").toFile();

        long count = CsvUtil.converter().source(testCsvFile).setEscapeCharToBackSlashForWrite().csvToJson(jsonFile);

        assertEquals(5, count);
    }

    // ===================== Tests for public static parser constants =====================

    @Test
    @DisplayName("Test jsonParser is not null")
    public void testJsonParserNotNull() {
        assertNotNull(CsvUtil.jsonParser);
    }

    @Test
    @DisplayName("Test jsonParser can parse JSON")
    @SuppressWarnings("unchecked")
    public void testJsonParserParseJson() {
        String json = "{\"name\":\"John\",\"age\":30}";
        Map<String, Object> result = CsvUtil.jsonParser.readString(json, Map.class);
        assertNotNull(result);
        assertEquals("John", result.get("name"));
        assertEquals(30, result.get("age"));
    }

    @Test
    @DisplayName("Test CSV_HEADER_PARSER with simple header")
    public void testCSV_HEADER_PARSER_SimpleHeader() {
        String header = "a,b,c";
        String[] result = CsvUtil.CSV_HEADER_PARSER.apply(header);
        assertEquals(3, result.length);
        assertEquals("a", result[0]);
        assertEquals("b", result[1]);
        assertEquals("c", result[2]);
    }

    @Test
    @DisplayName("Test CSV_HEADER_PARSER with quoted fields")
    public void testCSV_HEADER_PARSER_QuotedHeader() {
        String header = "\"a\",\"b,c\",\"d\"";
        String[] result = CsvUtil.CSV_HEADER_PARSER.apply(header);
        assertEquals(3, result.length);
        assertEquals("a", result[0]);
        assertEquals("b,c", result[1]);
        assertEquals("d", result[2]);
    }

    @Test
    @DisplayName("Test CSV_LINE_PARSER with simple line")
    public void testCSV_LINE_PARSER_SimpleLine() {
        String line = "John,30,NYC";
        String[] output = new String[3];
        CsvUtil.CSV_LINE_PARSER.accept(line, output);
        assertEquals("John", output[0]);
        assertEquals("30", output[1]);
        assertEquals("NYC", output[2]);
    }

    @Test
    @DisplayName("Test CSV_LINE_PARSER with quoted fields")
    public void testCSV_LINE_PARSER_QuotedLine() {
        String line = "\"John Doe\",30,\"New York, NY\"";
        String[] output = new String[3];
        CsvUtil.CSV_LINE_PARSER.accept(line, output);
        assertEquals("John Doe", output[0]);
        assertEquals("30", output[1]);
        assertEquals("New York, NY", output[2]);
    }

    @Test
    @DisplayName("Test CSV_HEADER_PARSER_BY_SPLITTER simple")
    public void testCSV_HEADER_PARSER_BY_SPLITTER_Simple() {
        String header = "a,b,c";
        String[] result = CsvUtil.CSV_HEADER_PARSER_BY_SPLITTER.apply(header);
        assertEquals(3, result.length);
        assertEquals("a", result[0]);
        assertEquals("b", result[1]);
        assertEquals("c", result[2]);
    }

    @Test
    @DisplayName("Test CSV_HEADER_PARSER_BY_SPLITTER with quotes")
    public void testCSV_HEADER_PARSER_BY_SPLITTER_QuotedFields() {
        String header = "\"name\",\"age\",\"city\"";
        String[] result = CsvUtil.CSV_HEADER_PARSER_BY_SPLITTER.apply(header);
        assertEquals(3, result.length);
        assertEquals("name", result[0]);
        assertEquals("age", result[1]);
        assertEquals("city", result[2]);
    }

    @Test
    @DisplayName("Test CSV_LINE_PARSER_BY_SPLITTER simple")
    public void testCSV_LINE_PARSER_BY_SPLITTER_Simple() {
        String line = "John,30,NYC";
        String[] output = new String[3];
        CsvUtil.CSV_LINE_PARSER_BY_SPLITTER.accept(line, output);
        assertEquals("John", output[0]);
        assertEquals("30", output[1]);
        assertEquals("NYC", output[2]);
    }

    @Test
    @DisplayName("Test CSV_LINE_PARSER_BY_SPLITTER with quotes")
    public void testCSV_LINE_PARSER_BY_SPLITTER_QuotedFields() {
        String line = "\"John\",\"30\",\"NYC\"";
        String[] output = new String[3];
        CsvUtil.CSV_LINE_PARSER_BY_SPLITTER.accept(line, output);
        assertEquals("John", output[0]);
        assertEquals("30", output[1]);
        assertEquals("NYC", output[2]);
    }

    @Test
    @DisplayName("Test CSV_HEADER_PARSER_IN_JSON parse")
    public void testCSV_HEADER_PARSER_IN_JSON_Parse() {
        String header = "[\"Name\",\"Age\",\"City\"]";
        String[] result = CsvUtil.CSV_HEADER_PARSER_IN_JSON.apply(header);
        assertEquals(3, result.length);
        assertEquals("Name", result[0]);
        assertEquals("Age", result[1]);
        assertEquals("City", result[2]);
    }

    @Test
    @DisplayName("Test CSV_LINE_PARSER_IN_JSON parse")
    public void testCSV_LINE_PARSER_IN_JSON_Parse() {
        String line = "[\"John\",\"30\",\"NYC\"]";
        String[] output = new String[3];
        CsvUtil.CSV_LINE_PARSER_IN_JSON.accept(line, output);
        assertEquals("John", output[0]);
        assertEquals("30", output[1]);
        assertEquals("NYC", output[2]);
    }

    // ===================== Additional edge case tests =====================

    @Test
    @DisplayName("Test stream with offset greater than row count")
    public void testStreamWithLargeOffset() {
        try (Stream<Person> stream = CsvUtil.stream(testCsvFile, null, 100, Long.MAX_VALUE, Fn.alwaysTrue(), Person.class)) {
            List<Person> persons = stream.toList();
            assertEquals(0, persons.size());
        }
    }

    @Test
    @DisplayName("Test stream with count of zero")
    public void testStreamWithZeroCount() {
        try (Stream<Person> stream = CsvUtil.stream(testCsvFile, null, 0, 0, Fn.alwaysTrue(), Person.class)) {
            List<Person> persons = stream.toList();
            assertEquals(0, persons.size());
        }
    }

    @Test
    @DisplayName("Test stream from empty file")
    public void testStreamEmptyFile() throws IOException {
        File emptyFile = tempDir.resolve("empty.csv").toFile();
        Files.writeString(emptyFile.toPath(), "");

        try (Stream<Map> stream = CsvUtil.stream(emptyFile, Map.class)) {
            List<Map> results = stream.toList();
            assertEquals(0, results.size());
        }
    }

    @Test
    @DisplayName("Test stream header only file")
    public void testStreamHeaderOnlyFile() throws IOException {
        File headerOnlyFile = tempDir.resolve("header_only.csv").toFile();
        Files.writeString(headerOnlyFile.toPath(), "id,name,age\n");

        try (Stream<Map> stream = CsvUtil.stream(headerOnlyFile, Map.class)) {
            List<Map> results = stream.toList();
            assertEquals(0, results.size());
        }
    }

    @Test
    @DisplayName("Test stream Reader not closed when closeReaderWhenStreamIsClosed is false")
    public void testStreamReaderNotClosed() throws IOException {
        StringReader reader = new StringReader(testCsvContent);

        try (Stream<Person> stream = CsvUtil.stream(reader, Person.class, false)) {
            List<Person> persons = stream.toList();
            assertEquals(5, persons.size());
        }
        // Reader should still be usable after stream is closed (though exhausted)
    }

    @Test
    @DisplayName("Test csvToJson with Reader and Writer")
    public void testCsv2JsonReaderWriter() {
        Reader reader = new StringReader(testCsvContent);
        StringWriter writer = new StringWriter();

        long count = CsvUtil.csvToJson(reader, null, writer, null);

        assertEquals(5, count);
        String json = writer.toString();
        assertTrue(json.contains("["));
        assertTrue(json.contains("John"));
    }

    @Test
    @DisplayName("Test jsonToCsv with Reader and Writer")
    public void testJson2CsvReaderWriter() throws IOException {
        File jsonFile = tempDir.resolve("test_jsonToCsv.json").toFile();
        CsvUtil.csvToJson(testCsvFile, jsonFile);

        StringWriter writer = new StringWriter();
        try (Reader reader = IOUtil.newFileReader(jsonFile)) {
            long count = CsvUtil.jsonToCsv(reader, null, writer);

            assertEquals(5, count);
            String csv = writer.toString();
            assertTrue(csv.contains("name"));
        }
    }

    @Test
    @DisplayName("Test load with all columns in selectColumnNames")
    public void testLoadCSVWithAllColumnsSelected() {
        List<String> allColumns = List.of("id", "name", "age", "active");
        Dataset ds = CsvUtil.load(testCsvFile, allColumns);

        assertNotNull(ds);
        assertEquals(4, ds.columnCount());
        assertEquals(5, ds.size());
    }

    @Test
    @DisplayName("Test isBackSlashEscapeCharForWrite")
    public void testIsBackSlashEscapeCharForWrite() {
        assertFalse(CsvUtil.isBackSlashEscapeCharForWrite());
        CsvUtil.setEscapeCharToBackSlashForWrite();
        assertTrue(CsvUtil.isBackSlashEscapeCharForWrite());
        CsvUtil.resetEscapeCharForWrite();
        assertFalse(CsvUtil.isBackSlashEscapeCharForWrite());
    }

    @Test
    @DisplayName("Test stream with single column Integer type")
    public void testStreamWithSingleColumnIntegerType() {
        List<String> selectColumns = List.of("age");
        try (Stream<Integer> stream = CsvUtil.stream(testCsvFile, selectColumns, Integer.class)) {
            List<Integer> ages = stream.toList();
            assertEquals(5, ages.size());
            assertEquals(25, ages.get(0));
            assertEquals(30, ages.get(1));
        }
    }

    @Test
    @DisplayName("Test writeField with various types")
    public void testWriteFieldWithVariousTypes() throws IOException {
        // Test with Double
        StringWriter sw = new StringWriter();
        BufferedCsvWriter writer = new BufferedCsvWriter(sw);
        CsvUtil.writeField(writer, Type.of(Double.class), 3.14);
        writer.flush();
        assertTrue(sw.toString().contains("3.14"));

        // Test with Boolean
        sw = new StringWriter();
        writer = new BufferedCsvWriter(sw);
        CsvUtil.writeField(writer, Type.of(Boolean.class), true);
        writer.flush();
        assertTrue(sw.toString().contains("true"));

        // Test with String containing special characters
        sw = new StringWriter();
        writer = new BufferedCsvWriter(sw);
        CsvUtil.writeField(writer, Type.of(String.class), "hello,world");
        writer.flush();
        assertTrue(sw.toString().contains("hello,world"));
    }

    @Test
    @DisplayName("Test load with columnTypeMap for specific columns")
    public void testLoadCSVWithPartialColumnTypeMap() {
        Map<String, Type<?>> typeMap = new HashMap<>();
        typeMap.put("age", Type.of(Integer.class));
        typeMap.put("active", Type.of(Boolean.class));

        Dataset ds = CsvUtil.load(testCsvFile, null, 0, Long.MAX_VALUE, typeMap);
        assertNotNull(ds);
        assertEquals(5, ds.size());

        ds.println();

    }
}

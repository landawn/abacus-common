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
import java.util.ArrayList;
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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.exception.ParsingException;
import com.landawn.abacus.type.Type;
import com.landawn.abacus.util.NoCachingNoUpdating.DisposableArray;
import com.landawn.abacus.util.function.TriConsumer;
import com.landawn.abacus.util.stream.Stream;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

public class CsvUtilTest extends TestBase {

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
    @DisplayName("Ragged row (more fields than header) throws a clear ParsingException")
    public void testLoadRaggedRowThrowsParsingException() throws IOException {
        final File raggedFile = tempDir.resolve("ragged.csv").toFile();
        // header has 3 columns; the second data row has 4 fields
        Files.writeString(raggedFile.toPath(), "a,b,c\n1,2,3\n4,5,6,7\n");

        final ParsingException ex = assertThrows(ParsingException.class, () -> CsvUtil.load(raggedFile));
        assertTrue(ex.getMessage().contains("3 column"), "message should name the expected column count: " + ex.getMessage());
    }

    @Test
    @DisplayName("Test setHeaderParser")
    public void testSetHeaderParser() {
        Function<String, String[]> customParser = line -> line.split(";");
        CsvUtil.setHeaderParser(customParser);
        assertSame(customParser, CsvUtil.getCurrentHeaderParser());
        assertThrows(IllegalArgumentException.class, () -> CsvUtil.setHeaderParser(null));
    }

    // ===================== Additional tests for untested methods =====================

    @Test
    @DisplayName("Test setHeaderParser with null throws exception")
    public void testSetHeaderParser_Null() {
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
    @DisplayName("Test setLineParser with null throws exception")
    public void testSetLineParser_Null() {
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
    @DisplayName("Test isBackSlashEscapeCharForWrite")
    public void testIsBackSlashEscapeCharForWrite() {
        assertFalse(CsvUtil.isBackSlashEscapeCharForWrite());
        CsvUtil.setEscapeCharToBackSlashForWrite();
        assertTrue(CsvUtil.isBackSlashEscapeCharForWrite());
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
    @DisplayName("Test writeField with null type infers String type")
    public void testWriteField_NullType() throws IOException {
        StringWriter sw = new StringWriter();
        BufferedCsvWriter writer = new BufferedCsvWriter(sw);
        CsvUtil.writeField(writer, null, "hello");
        writer.flush();
        assertTrue(sw.toString().contains("hello"));
    }

    @Test
    @DisplayName("Test writeField with backslash escape char enabled")
    public void testWriteField_BackSlashEscape() throws IOException {
        CsvUtil.setEscapeCharToBackSlashForWrite();
        try {
            StringWriter sw = new StringWriter();
            BufferedCsvWriter writer = new BufferedCsvWriter(sw);
            CsvUtil.writeField(writer, Type.of(String.class), "test\"value");
            writer.flush();
            assertNotNull(sw.toString());
            assertTrue(sw.toString().length() > 0);
        } finally {
            CsvUtil.resetEscapeCharForWrite();
        }
    }

    @Test
    @DisplayName("Test writeField with null value writes null")
    public void testWriteField_NullValue() throws IOException {
        StringWriter sw = new StringWriter();
        BufferedCsvWriter writer = new BufferedCsvWriter(sw);
        CsvUtil.writeField(writer, Type.of(String.class), null);
        writer.flush();
        assertTrue(sw.toString().contains("null"));
    }

    @Test
    @DisplayName("Test writeField with Long type")
    public void testWriteField_LongType() throws IOException {
        StringWriter sw = new StringWriter();
        BufferedCsvWriter writer = new BufferedCsvWriter(sw);
        CsvUtil.writeField(writer, Type.of(Long.class), 12345678901L);
        writer.flush();
        assertTrue(sw.toString().contains("12345678901"));
    }

    @Test
    @DisplayName("Test load(File) verifies data content")
    public void testLoadCSVFromFile_DataContent() {
        Dataset ds = CsvUtil.load(testCsvFile);
        assertEquals("1", ds.get(0, 0));
        assertEquals("John", ds.get(0, 1));
        assertEquals("25", ds.get(0, 2));
    }

    @Test
    @DisplayName("Test load(Reader) verifies data content")
    public void testLoadCSVFromReader_DataContent() {
        Reader reader = new StringReader(testCsvContent);
        Dataset ds = CsvUtil.load(reader);
        assertEquals("1", ds.get(0, 0));
        assertEquals("John", ds.get(0, 1));
    }

    @Test
    @DisplayName("Test load(File, Class) verifies typed data")
    public void testLoadCSVFromFileWithBeanClass_TypedData() {
        Dataset ds = CsvUtil.load(testCsvFile, Person.class);
        // age should be Integer type from Person class
        Object ageValue = ds.get(0, 2);
        assertTrue(ageValue instanceof Integer);
        assertEquals(Integer.valueOf(25), ageValue);
    }

    @Test
    @DisplayName("Test load(Reader, Class) verifies typed data")
    public void testLoadCSVFromReaderWithBeanClass_TypedData() {
        Reader reader = new StringReader(testCsvContent);
        Dataset ds = CsvUtil.load(reader, Person.class);
        Object ageValue = ds.get(0, 2);
        assertTrue(ageValue instanceof Integer);
    }

    // CsvLoader.load() with reader, no beanClass, no columnTypeMap -> L3403
    @Test
    public void testCsvLoader_ReaderLoadWithoutBeanClassOrColumnTypeMap() {
        Reader reader = new StringReader("id,name\n1,John\n2,Jane");
        Dataset ds = CsvUtil.loader().source(reader).load();
        assertEquals(2, ds.columnCount());
        assertEquals(2, ds.size());
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

        Dataset ds = CsvUtil.load(testCsvFile, N.toList("id", "name", "age"), 0, Long.MAX_VALUE, typeMap);
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

        Dataset ds = CsvUtil.load(testCsvFile, N.toList("id", "name"), 1, 2, typeMap);
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
    @DisplayName("A null column type defaults to String without dropping or shifting the column")
    public void testLoadCSVFromReaderWithNullTypeMapping() {
        Map<String, Type<?>> typeMap = new HashMap<>();
        typeMap.put("id", null);

        Dataset ds = CsvUtil.load(new StringReader(testCsvContent), List.of("id", "name"), 0, Long.MAX_VALUE, typeMap);

        assertEquals(2, ds.columnCount());
        assertEquals(5, ds.size());
        assertEquals("1", ds.get(0, 0));
        assertEquals("John", ds.get(0, 1));

        Dataset allColumns = CsvUtil.load(new StringReader(testCsvContent), typeMap);
        assertEquals(4, allColumns.columnCount());
        assertEquals("1", allColumns.get(0, 0));
    }

    @Test
    @DisplayName("Test load(Reader, Collection, long, long, Map<String, Type>)")
    public void testLoadCSVFromReaderWithOffsetCountAndTypeMap() {
        Reader reader = new StringReader(testCsvContent);
        Map<String, Type<?>> typeMap = new HashMap<>();
        typeMap.put("id", Type.of(String.class));
        typeMap.put("name", Type.of(String.class));

        Dataset ds = CsvUtil.load(reader, N.toList("id", "name", "age"), 0, 3, typeMap);
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
    @DisplayName("Test load with all columns in selectColumnNames")
    public void testLoadCSVWithAllColumnsSelected() {
        List<String> allColumns = List.of("id", "name", "age", "active");
        Dataset ds = CsvUtil.load(testCsvFile, allColumns);

        assertNotNull(ds);
        assertEquals(4, ds.columnCount());
        assertEquals(5, ds.size());
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

    // --- Additional missing tests ---

    @Test
    @DisplayName("Test load(File, Map<String, Type>) - 2 arg typeMap overload")
    public void testLoadCSVFromFileWithTypeMapOnly() {
        Map<String, Type<?>> typeMap = new HashMap<>();
        typeMap.put("age", Type.of(Integer.class));
        typeMap.put("active", Type.of(Boolean.class));

        Dataset ds = CsvUtil.load(testCsvFile, typeMap);
        assertNotNull(ds);
        assertEquals(4, ds.columnCount());
        assertEquals(5, ds.size());
    }

    @Test
    @DisplayName("Test load(File, selectColumns, offset, count, Predicate, typeMap) with filter")
    public void testLoadCSVFromFileWithFilterAndTypeMapFull() {
        Map<String, Type<?>> typeMap = new HashMap<>();
        typeMap.put("age", Type.of(Integer.class));

        Predicate<String[]> filter = row -> Integer.parseInt(row[2]) > 28;
        Dataset ds = CsvUtil.load(testCsvFile, List.of("name", "age"), 0, Long.MAX_VALUE, filter, typeMap);
        assertNotNull(ds);
        assertEquals(2, ds.columnCount());
        assertTrue(ds.size() >= 2);
    }

    @Test
    @DisplayName("Test load(Reader, selectColumns, offset, count, Predicate, typeMap)")
    public void testLoadCSVFromReaderWithFilterAndTypeMapFull() {
        Reader reader = new StringReader(testCsvContent);
        Map<String, Type<?>> typeMap = new HashMap<>();
        typeMap.put("age", Type.of(Integer.class));

        Predicate<String[]> filter = row -> Integer.parseInt(row[2]) > 28;
        Dataset ds = CsvUtil.load(reader, List.of("name", "age"), 0, Long.MAX_VALUE, filter, typeMap);
        assertNotNull(ds);
        assertEquals(2, ds.columnCount());
        assertTrue(ds.size() >= 2);
    }

    @Test
    @DisplayName("Test load(File, selectColumns, long, long, Predicate) with filter only")
    public void testLoadCSVFromFileWithFilterOnly() {
        Predicate<String[]> filter = row -> Integer.parseInt(row[2]) > 30;
        Dataset ds = CsvUtil.load(testCsvFile, null, 0, Long.MAX_VALUE, filter);
        assertNotNull(ds);
        assertEquals(4, ds.columnCount());
        assertTrue(ds.size() >= 2);
    }

    @Test
    @DisplayName("Test load(Reader, selectColumns, long, long, Predicate) with filter only")
    public void testLoadCSVFromReaderWithFilterOnly() {
        Reader reader = new StringReader(testCsvContent);
        Predicate<String[]> filter = row -> Integer.parseInt(row[2]) <= 30;
        Dataset ds = CsvUtil.load(reader, null, 0, Long.MAX_VALUE, filter);
        assertNotNull(ds);
        assertEquals(4, ds.columnCount());
        assertTrue(ds.size() >= 3);
    }

    @Test
    @DisplayName("Test load(File, selectColumns, long, long, Predicate, beanClass) - all params")
    public void testLoadCSVFromFileWithAllParamsAndBeanClass() {
        List<String> selectColumns = List.of("name", "age");
        Predicate<String[]> filter = row -> Integer.parseInt(row[2]) > 25;
        Dataset ds = CsvUtil.load(testCsvFile, selectColumns, 0, 3, filter, Person.class);
        assertNotNull(ds);
        assertEquals(2, ds.columnCount());
        assertTrue(ds.size() <= 3);
    }

    @Test
    @DisplayName("Test load(Reader, selectColumns, long, long, Predicate, beanClass) - all params")
    public void testLoadCSVFromReaderWithAllParamsAndBeanClass() {
        Reader reader = new StringReader(testCsvContent);
        Predicate<String[]> filter = row -> "true".equals(row[3]);
        Dataset ds = CsvUtil.load(reader, null, 0, Long.MAX_VALUE, filter, Person.class);
        assertNotNull(ds);
        assertEquals(3, ds.size());
    }

    @Test
    @DisplayName("Test load(Reader, selectColumns, beanClass) with select")
    public void testLoadCSVFromReaderWithSelectColumnsAndBeanClassDirect() {
        Reader reader = new StringReader(testCsvContent);
        List<String> selectColumns = List.of("id", "name");
        Dataset ds = CsvUtil.load(reader, selectColumns, Person.class);
        assertNotNull(ds);
        assertEquals(2, ds.columnCount());
    }

    @Test
    @DisplayName("Test load(Reader, selectColumns, offset, count, beanClass)")
    public void testLoadCSVFromReaderWithOffsetCountAndBeanClassDirect() {
        Reader reader = new StringReader(testCsvContent);
        Dataset ds = CsvUtil.load(reader, null, 1, 2, Person.class);
        assertNotNull(ds);
        assertEquals(2, ds.size());
    }

    @Test
    @DisplayName("Test load(Reader, selectColumns, offset, count, Predicate, beanClass)")
    public void testLoadCSVFromReaderWithFilterAndBeanClassDirect() {
        Reader reader = new StringReader(testCsvContent);
        Predicate<String[]> filter = row -> Integer.parseInt(row[2]) >= 30;
        Dataset ds = CsvUtil.load(reader, null, 0, Long.MAX_VALUE, filter, Person.class);
        assertNotNull(ds);
        assertTrue(ds.size() >= 3);
    }

    @Test
    @DisplayName("Test load(Reader, columnTypeMap, selectColumns, offset, count)")
    public void testLoadCSVFromReaderWithTypeMapAndOffsetCount() {
        Reader reader = new StringReader(testCsvContent);
        Map<String, Type<?>> typeMap = new HashMap<>();
        typeMap.put("age", Type.of(Integer.class));

        Dataset ds = CsvUtil.load(reader, List.of("name", "age"), 1, 2, typeMap);
        assertNotNull(ds);
        assertEquals(2, ds.columnCount());
        assertEquals(2, ds.size());
    }

    @Test
    @DisplayName("Test load(File, Collection) with single column")
    public void testLoadCSVFromFileWithSingleColumn() {
        Dataset ds = CsvUtil.load(testCsvFile, List.of("name"));
        assertEquals(1, ds.columnCount());
        assertEquals(5, ds.size());
        assertEquals("John", ds.get(0, 0));
    }

    @Test
    @DisplayName("Test load(File, Collection, long, long) with zero count")
    public void testLoadCSVFromFileWithZeroCount() {
        Dataset ds = CsvUtil.load(testCsvFile, null, 0, 0);
        assertNotNull(ds);
        assertEquals(4, ds.columnCount());
        assertEquals(0, ds.size());
    }

    @Test
    @DisplayName("Test load(File, Collection, long, long) with offset beyond rows")
    public void testLoadCSVFromFileWithOffsetBeyondRows() {
        Dataset ds = CsvUtil.load(testCsvFile, null, 100, Long.MAX_VALUE);
        assertNotNull(ds);
        assertEquals(4, ds.columnCount());
        assertEquals(0, ds.size());
    }

    @Test
    @DisplayName("Test load(Reader, Collection) with single column")
    public void testLoadCSVFromReaderWithSingleColumn() {
        Reader reader = new StringReader(testCsvContent);
        Dataset ds = CsvUtil.load(reader, List.of("name"));
        assertEquals(1, ds.columnCount());
        assertEquals(5, ds.size());
    }

    @Test
    @DisplayName("Test load(Reader, Collection, long, long) with zero count")
    public void testLoadCSVFromReaderWithZeroCount() {
        Reader reader = new StringReader(testCsvContent);
        Dataset ds = CsvUtil.load(reader, null, 0, 0);
        assertNotNull(ds);
        assertEquals(0, ds.size());
    }

    @Test
    @DisplayName("Test load(Reader, Collection, long, long, Predicate) with no matching filter")
    public void testLoadCSVFromReaderWithNoMatchFilter() {
        Reader reader = new StringReader(testCsvContent);
        Predicate<String[]> filter = row -> Integer.parseInt(row[2]) > 100;
        Dataset ds = CsvUtil.load(reader, null, 0, Long.MAX_VALUE, filter);
        assertNotNull(ds);
        assertEquals(0, ds.size());
    }

    @Test
    @DisplayName("Test load(File, Collection, long, long, Predicate, Map<String, Type>) with offset and count")
    public void testLoadCSVFromFileWithFilterTypeMapOffsetCount() {
        Map<String, Type<?>> typeMap = new HashMap<>();
        typeMap.put("age", Type.of(Integer.class));
        Predicate<String[]> filter = row -> Integer.parseInt(row[2]) > 25;
        Dataset ds = CsvUtil.load(testCsvFile, null, 1, 2, filter, typeMap);
        assertNotNull(ds);
        assertTrue(ds.size() <= 2);
    }

    @Test
    @DisplayName("Test load(Reader, Collection, long, long, Predicate, Map<String, Type>) verifies typed data")
    public void testLoadCSVFromReaderWithFilterTypeMap_TypedData() {
        Reader reader = new StringReader(testCsvContent);
        Map<String, Type<?>> typeMap = new HashMap<>();
        typeMap.put("age", Type.of(Integer.class));
        typeMap.put("active", Type.of(Boolean.class));

        Dataset ds = CsvUtil.load(reader, null, 0, Long.MAX_VALUE, Fn.alwaysTrue(), typeMap);
        assertNotNull(ds);
        assertEquals(5, ds.size());
        assertTrue(ds.get(0, 2) instanceof Integer);
        assertTrue(ds.get(0, 3) instanceof Boolean);
    }

    @Test
    @DisplayName("Test load(Reader, long, long, TriConsumer)")
    public void testLoadCSVFromReaderWithOffsetCountAndRowExtractorDirect() {
        Reader reader = new StringReader(testCsvContent);
        TriConsumer<List<String>, DisposableArray<String>, Object[]> extractor = (columns, row, output) -> {
            for (int i = 0; i < row.length(); i++) {
                output[i] = row.get(i);
            }
        };

        Dataset ds = CsvUtil.load(reader, 2, 2, extractor);
        assertNotNull(ds);
        assertEquals(4, ds.columnCount());
        assertEquals(2, ds.size());
    }

    @Test
    @DisplayName("Test load with Reader from empty content returns empty Dataset")
    public void testLoadCSVFromReader_EmptyContent() {
        Reader reader = new StringReader("");
        Dataset ds = CsvUtil.load(reader);
        assertNotNull(ds);
        assertEquals(0, ds.size());
    }

    @Test
    @DisplayName("Test load with Reader header only returns empty Dataset")
    public void testLoadCSVFromReader_HeaderOnly() {
        Reader reader = new StringReader("id,name,age\n");
        Dataset ds = CsvUtil.load(reader);
        assertNotNull(ds);
        assertEquals(3, ds.columnCount());
        assertEquals(0, ds.size());
    }

    // load(Reader, Collection, long, long, Predicate, Class) with empty reader -> L1001
    @Test
    public void testLoad_ReaderBeanClass_EmptyReader_ReturnsEmptyDataset() {
        Reader reader = new StringReader("");
        Dataset ds = CsvUtil.load(reader, null, 0, Long.MAX_VALUE, null, Person.class);
        assertEquals(0, ds.size());
    }

    // load(Reader, ..., beanClass) where a column has no matching bean property (noSelect path) -> L1059-1060
    @Test
    public void testLoad_ReaderBeanClass_ColumnNotInBean_UsesRawStringValue() {
        // CSV has "unknownColumn" which does not exist in Person bean -> propInfos[i] == null -> uses raw string
        Reader reader = new StringReader("id,name,unknownColumn\n1,John,someRawValue");
        Dataset ds = CsvUtil.load(reader, null, 0, Long.MAX_VALUE, null, Person.class);
        assertEquals(3, ds.columnCount());
        assertEquals("someRawValue", ds.get(0, 2)); // "unknownColumn" is at index 2
    }

    // load(Reader, ..., beanClass) with selected columns where selected column has no bean property -> L1068-1069
    @Test
    public void testLoad_ReaderBeanClass_SelectedColumnNotInBean_UsesRawStringValue() {
        // selectColumnNames contains "unknownColumn" which exists in CSV but not in Person -> raw string stored
        Reader reader = new StringReader("id,name,unknownColumn\n1,John,rawValue");
        Dataset ds = CsvUtil.load(reader, List.of("id", "unknownColumn"), 0, Long.MAX_VALUE, null, Person.class);
        assertEquals(2, ds.columnCount());
        assertEquals("rawValue", ds.get(0, 1)); // "unknownColumn" is at column index 1
    }

    // load(Reader, Collection, long, long, Predicate, Class) with header-only reader (data line null) -> different from empty
    @Test
    public void testLoad_ReaderBeanClass_HeaderOnlyReader_ReturnsEmptyDataset() {
        Reader reader = new StringReader("id,name,age\n");
        Dataset ds = CsvUtil.load(reader, null, 0, Long.MAX_VALUE, null, Person.class);
        assertEquals(0, ds.size());
        assertEquals(3, ds.columnCount());
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
    @DisplayName("Test load(File, Map<String, Type>) with empty map throws exception")
    public void testLoadCSVFromFileWithEmptyTypeMap() {
        Map<String, Type<?>> typeMap = new HashMap<>();
        assertThrows(IllegalArgumentException.class, () -> CsvUtil.load(testCsvFile, typeMap));
    }

    @Test
    @DisplayName("Test load(Reader, Map<String, Type>) with empty map throws exception")
    public void testLoadCSVFromReaderWithEmptyTypeMap() {
        Reader reader = new StringReader(testCsvContent);
        Map<String, Type<?>> typeMap = new HashMap<>();
        assertThrows(IllegalArgumentException.class, () -> CsvUtil.load(reader, typeMap));
    }

    @Test
    @DisplayName("Test load(File, TriConsumer) verifies extracted data")
    public void testLoadCSVFromFileWithRowExtractor_DataCheck() {
        TriConsumer<List<String>, DisposableArray<String>, Object[]> extractor = (columns, row, output) -> {
            output[0] = row.get(0);
            output[1] = row.get(1).toUpperCase();
            output[2] = Integer.parseInt(row.get(2));
            output[3] = Boolean.parseBoolean(row.get(3));
        };

        Dataset ds = CsvUtil.load(testCsvFile, extractor);
        assertEquals("JOHN", ds.get(0, 1));
        assertEquals(Integer.valueOf(25), ds.get(0, 2));
        assertEquals(Boolean.TRUE, ds.get(0, 3));
    }

    @Test
    @DisplayName("Test CsvLoader load(TriConsumer) with Reader source")
    public void testCsvLoaderLoadWithRowExtractorFromReader() {
        Reader reader = new StringReader(testCsvContent);
        TriConsumer<List<String>, DisposableArray<String>, Object[]> extractor = (columns, row, output) -> {
            for (int i = 0; i < row.length(); i++) {
                output[i] = row.get(i);
            }
        };

        Dataset ds = CsvUtil.loader().source(reader).load(extractor);
        assertNotNull(ds);
        assertEquals(4, ds.columnCount());
        assertEquals(5, ds.size());
    }

    @Test
    @DisplayName("Test CsvLoader load(TriConsumer) without source throws exception")
    public void testCsvLoaderLoadRowExtractorWithoutSource() {
        TriConsumer<List<String>, DisposableArray<String>, Object[]> extractor = (columns, row, output) -> {
        };
        assertThrows(IllegalArgumentException.class, () -> CsvUtil.loader().load(extractor));
    }

    @Test
    @DisplayName("Test load(Reader, selectColumns, offset, count, rowFilter, beanClass) with offset and filter")
    public void testLoad_ReaderWithOffsetAndFilterAndBeanClass() throws IOException {
        try (Reader reader = new StringReader(testCsvContent)) {
            Dataset result = CsvUtil.load(reader, null, 1, 3, row -> !"false".equals(row[3]), Person.class);
            assertNotNull(result);
            assertTrue(result.size() <= 3);
        }
    }

    @Test
    @DisplayName("Test load(Reader, selectColumns, offset, count, rowFilter, columnTypeMap)")
    public void testLoad_ReaderWithColumnTypeMap() throws IOException {
        Map<String, Type<?>> typeMap = new HashMap<>();
        typeMap.put("id", Type.of(String.class));
        typeMap.put("name", Type.of(String.class));
        typeMap.put("age", Type.of(Integer.class));

        try (Reader reader = new StringReader(testCsvContent)) {
            Dataset result = CsvUtil.load(reader, null, 0, Long.MAX_VALUE, null, typeMap);
            assertNotNull(result);
            assertEquals(5, result.size());
        }
    }

    @Test
    @DisplayName("Test load(Reader, selectColumns, offset, count, rowFilter, columnTypeMap) with empty map throws exception")
    public void testLoad_ReaderWithEmptyColumnTypeMap() throws IOException {
        try (Reader reader = new StringReader(testCsvContent)) {
            assertThrows(IllegalArgumentException.class, () -> CsvUtil.load(reader, null, 0, Long.MAX_VALUE, null, new HashMap<>()));
        }
    }

    // load(Reader, Collection, long, long, Predicate, Class) with invalid selectColumnNames -> L1030
    @Test
    public void testLoad_ReaderBeanClass_InvalidSelectColumns_ThrowsIllegalArgument() {
        Reader reader = new StringReader("id,name,age\n1,John,25");
        assertThrows(IllegalArgumentException.class, () -> CsvUtil.load(reader, List.of("nonexistent"), 0, Long.MAX_VALUE, null, Person.class));
    }

    // CsvLoader.load() with invalid selectColumnNames from reader -> exercises L1340
    @Test
    public void testLoad_FileWithSelectColumnNamesAndBeanClass_InvalidColumns_ThrowsIllegalArgument() throws IOException {
        assertThrows(IllegalArgumentException.class, () -> CsvUtil.load(testCsvFile, List.of("nonexistent"), 0, Long.MAX_VALUE, null, Person.class));
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
    @DisplayName("Reference-array streams convert fields to the component type")
    public void testStreamWithTypedObjectArray() {
        try (Stream<Integer[]> stream = CsvUtil.stream(new StringReader("a,b\n1,2\n3,4\n"), Integer[].class, true)) {
            List<Integer[]> rows = stream.toList();

            assertEquals(2, rows.size());
            assertEquals(Integer.valueOf(1), rows.get(0)[0]);
            assertEquals(Integer.valueOf(2), rows.get(0)[1]);
            assertEquals(Integer.valueOf(4), rows.get(1)[1]);
        }
    }

    @Test
    @DisplayName("Test stream(File, BiFunction) - no select columns")
    public void testStreamFromFileWithBiFunctionOnly() {
        BiFunction<List<String>, DisposableArray<String>, String> mapper = (headers, row) -> row.get(1);

        try (Stream<String> stream = CsvUtil.stream(testCsvFile, mapper)) {
            List<String> names = stream.toList();
            assertEquals(5, names.size());
            assertEquals("John", names.get(0));
        }
    }

    @Test
    @DisplayName("Test stream(Reader, BiFunction, boolean) - no select columns")
    public void testStreamFromReaderWithBiFunctionOnly() {
        Reader reader = new StringReader(testCsvContent);
        BiFunction<List<String>, DisposableArray<String>, String> mapper = (headers, row) -> row.get(0);

        try (Stream<String> stream = CsvUtil.stream(reader, mapper, true)) {
            List<String> ids = stream.toList();
            assertEquals(5, ids.size());
        }
    }

    @Test
    @DisplayName("Test stream(Reader, selectColumns, BiFunction, boolean) with select")
    public void testStreamFromReaderWithSelectColumnsAndBiFunctionFull() {
        Reader reader = new StringReader(testCsvContent);
        List<String> selectColumns = List.of("name", "age");
        BiFunction<List<String>, DisposableArray<String>, String> mapper = (headers, row) -> row.get(0) + "-" + row.get(1);

        try (Stream<String> stream = CsvUtil.stream(reader, selectColumns, mapper, true)) {
            List<String> results = stream.toList();
            assertEquals(5, results.size());
            assertTrue(results.get(0).contains("John"));
        }
    }

    @Test
    @DisplayName("Test stream(File, Class) with Person verifies all fields")
    public void testStreamFromFileWithClass_AllFields() {
        try (Stream<Person> stream = CsvUtil.stream(testCsvFile, Person.class)) {
            List<Person> persons = stream.toList();
            Person first = persons.get(0);
            assertEquals("1", first.getId());
            assertEquals("John", first.getName());
            assertEquals(Integer.valueOf(25), first.getAge());
            assertEquals(Boolean.TRUE, first.getActive());
        }
    }

    @Test
    @DisplayName("Test stream(Reader, Class, boolean) with false closeReader")
    public void testStreamFromReaderWithClass_NotCloseReader() {
        StringReader reader = new StringReader(testCsvContent);
        try (Stream<Person> stream = CsvUtil.stream(reader, Person.class, false)) {
            List<Person> persons = stream.toList();
            assertEquals(5, persons.size());
        }
        // Reader is not closed
    }

    @Test
    @DisplayName("Test stream(Reader, Collection, Class, boolean) verifies data")
    public void testStreamFromReaderWithSelectColumnsAndClass_DataCheck() {
        Reader reader = new StringReader(testCsvContent);
        try (Stream<Person> stream = CsvUtil.stream(reader, List.of("id", "name", "age", "active"), Person.class, true)) {
            List<Person> persons = stream.toList();
            assertEquals(5, persons.size());
            assertEquals("John", persons.get(0).getName());
        }
    }

    @Test
    @DisplayName("Test stream(File, BiFunction) verifies mapped data")
    public void testStreamFromFileWithBiFunction_DataVerification() {
        BiFunction<List<String>, DisposableArray<String>, String> mapper = (columns, row) -> row.get(1) + "(" + row.get(2) + ")";
        try (Stream<String> stream = CsvUtil.stream(testCsvFile, mapper)) {
            List<String> results = stream.toList();
            assertEquals(5, results.size());
            assertEquals("John(25)", results.get(0));
        }
    }

    @Test
    @DisplayName("Test stream(Reader, BiFunction, boolean) with false closeReader")
    public void testStreamFromReaderWithBiFunction_NotCloseReader() {
        StringReader reader = new StringReader(testCsvContent);
        BiFunction<List<String>, DisposableArray<String>, String> mapper = (columns, row) -> row.get(1);
        try (Stream<String> stream = CsvUtil.stream(reader, mapper, false)) {
            List<String> names = stream.toList();
            assertEquals(5, names.size());
        }
    }

    @Test
    @DisplayName("Test stream(Reader, Collection, BiFunction, boolean) verifies column selection")
    public void testStreamFromReaderWithSelectColumnsAndBiFunction_ColumnCheck() {
        Reader reader = new StringReader(testCsvContent);
        BiFunction<List<String>, DisposableArray<String>, String> mapper = (columns, row) -> {
            assertEquals(2, columns.size());
            return row.get(0);
        };
        try (Stream<String> stream = CsvUtil.stream(reader, List.of("name", "age"), mapper, true)) {
            List<String> names = stream.toList();
            assertEquals(5, names.size());
            assertEquals("John", names.get(0));
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
    @DisplayName("Test stream with single column String type")
    public void testStreamWithSingleColumnStringType() {
        List<String> selectColumns = List.of("name");
        try (Stream<String> stream = CsvUtil.stream(testCsvFile, selectColumns, String.class)) {
            List<String> names = stream.toList();
            assertEquals(5, names.size());
            assertEquals("John", names.get(0));
        }
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
    @DisplayName("Test stream(File, selectColumns, offset, count, Predicate, BiFunction)")
    public void testStreamFromFileWithAllParamsAndBiFunctionFull() {
        Predicate<String[]> filter = row -> "true".equals(row[3]);
        BiFunction<List<String>, DisposableArray<String>, String> mapper = (headers, row) -> row.get(1) + ":" + row.get(2);

        try (Stream<String> stream = CsvUtil.stream(testCsvFile, null, 0, Long.MAX_VALUE, filter, mapper)) {
            List<String> results = stream.toList();
            assertEquals(3, results.size());
            assertTrue(results.get(0).contains(":"));
        }
    }

    @Test
    @DisplayName("Test stream(Reader, selectColumns, offset, count, Predicate, BiFunction, closeReader)")
    public void testStreamFromReaderWithAllParamsAndBiFunctionFull() {
        Reader reader = new StringReader(testCsvContent);
        Predicate<String[]> filter = row -> Integer.parseInt(row[2]) >= 30;
        BiFunction<List<String>, DisposableArray<String>, String> mapper = (headers, row) -> row.get(1);

        try (Stream<String> stream = CsvUtil.stream(reader, null, 0, Long.MAX_VALUE, filter, mapper, true)) {
            List<String> results = stream.toList();
            assertEquals(3, results.size());
        }
    }

    @Test
    @DisplayName("Test stream from File with selectColumns, offset, count, Predicate, and Class")
    public void testStreamFromFileWithSelectColumnsOffsetCountFilterClass() {
        List<String> selectColumns = List.of("id", "name", "age");
        Predicate<String[]> filter = row -> Integer.parseInt(row[2]) > 25;

        try (Stream<Person> stream = CsvUtil.stream(testCsvFile, selectColumns, 0, Long.MAX_VALUE, filter, Person.class)) {
            List<Person> persons = stream.toList();
            assertTrue(persons.size() >= 3);
        }
    }

    @Test
    @DisplayName("Test stream from Reader with selectColumns, offset, count, Predicate, Class, closeReader")
    public void testStreamFromReaderWithSelectColumnsOffsetCountFilterClassClose() {
        Reader reader = new StringReader(testCsvContent);
        Predicate<String[]> filter = row -> "true".equals(row[3]);

        try (Stream<Person> stream = CsvUtil.stream(reader, null, 0, Long.MAX_VALUE, filter, Person.class, true)) {
            List<Person> persons = stream.toList();
            assertEquals(3, persons.size());
        }
    }

    @Test
    @DisplayName("Test stream(File, Collection, Class) with subset of fields")
    public void testStreamFromFileWithSelectColumnsAndClass_PartialFields() {
        try (Stream<Person> stream = CsvUtil.stream(testCsvFile, List.of("name", "age"), Person.class)) {
            List<Person> persons = stream.toList();
            assertEquals(5, persons.size());
            assertNotNull(persons.get(0).getName());
            assertNotNull(persons.get(0).getAge());
        }
    }

    @Test
    @DisplayName("Test stream(File, Collection, long, long, Predicate, Class) with offset and count")
    public void testStreamFromFileWithClassOffsetCount() {
        try (Stream<Person> stream = CsvUtil.stream(testCsvFile, null, 1, 2, Fn.alwaysTrue(), Person.class)) {
            List<Person> persons = stream.toList();
            assertEquals(2, persons.size());
            assertEquals("Jane", persons.get(0).getName());
        }
    }

    @Test
    @DisplayName("Test stream(Reader, Collection, long, long, Predicate, Class, boolean) with offset and count")
    public void testStreamFromReaderWithClassOffsetCount() {
        Reader reader = new StringReader(testCsvContent);
        try (Stream<Person> stream = CsvUtil.stream(reader, null, 2, 2, Fn.alwaysTrue(), Person.class, true)) {
            List<Person> persons = stream.toList();
            assertEquals(2, persons.size());
        }
    }

    @Test
    @DisplayName("Test stream(File, Collection, BiFunction) with single column")
    public void testStreamFromFileWithSelectColumnsAndBiFunction_SingleColumn() {
        BiFunction<List<String>, DisposableArray<String>, String> mapper = (columns, row) -> row.get(0);
        try (Stream<String> stream = CsvUtil.stream(testCsvFile, List.of("name"), mapper)) {
            List<String> names = stream.toList();
            assertEquals(5, names.size());
            assertEquals("John", names.get(0));
        }
    }

    @Test
    @DisplayName("Test stream(File, Collection, long, long, Predicate, BiFunction) with offset/count")
    public void testStreamFromFileWithBiFunctionOffsetCount() {
        BiFunction<List<String>, DisposableArray<String>, String> mapper = (columns, row) -> row.get(1);
        try (Stream<String> stream = CsvUtil.stream(testCsvFile, null, 1, 2, Fn.alwaysTrue(), mapper)) {
            List<String> names = stream.toList();
            assertEquals(2, names.size());
            assertEquals("Jane", names.get(0));
        }
    }

    @Test
    @DisplayName("Test stream(Reader, Collection, long, long, Predicate, BiFunction, boolean) with offset/count")
    public void testStreamFromReaderWithBiFunctionOffsetCount() {
        Reader reader = new StringReader(testCsvContent);
        BiFunction<List<String>, DisposableArray<String>, String> mapper = (columns, row) -> row.get(1);
        try (Stream<String> stream = CsvUtil.stream(reader, null, 1, 2, Fn.alwaysTrue(), mapper, true)) {
            List<String> names = stream.toList();
            assertEquals(2, names.size());
        }
    }

    @Test
    @DisplayName("Test stream from empty Reader returns empty stream")
    public void testStreamFromReader_EmptyContent() {
        Reader reader = new StringReader("");
        try (Stream<Person> stream = CsvUtil.stream(reader, Person.class, true)) {
            List<Person> persons = stream.toList();
            assertEquals(0, persons.size());
        }
    }

    @Test
    @DisplayName("Test stream BiFunction from empty Reader returns empty stream")
    public void testStreamBiFunctionFromReader_EmptyContent() {
        Reader reader = new StringReader("");
        BiFunction<List<String>, DisposableArray<String>, String> mapper = (columns, row) -> row.get(0);
        try (Stream<String> stream = CsvUtil.stream(reader, mapper, true)) {
            List<String> results = stream.toList();
            assertEquals(0, results.size());
        }
    }

    @Test
    @DisplayName("Test stream(File, selectColumns, offset, count, rowFilter, targetClass)")
    public void testStream_FileWithOffsetAndFilter() {
        try (Stream<Person> stream = CsvUtil.stream(testCsvFile, null, 0, Long.MAX_VALUE, null, Person.class)) {
            List<Person> result = stream.toList();
            assertEquals(5, result.size());
        }
    }

    @Test
    @DisplayName("Test stream(File, selectColumns, offset, count, rowFilter, biFunction)")
    public void testStream_FileWithBiFunctionMapper() {
        BiFunction<List<String>, DisposableArray<String>, String> mapper = (columns, row) -> row.get(0) + ":" + row.get(1);
        try (Stream<String> stream = CsvUtil.stream(testCsvFile, null, 0, Long.MAX_VALUE, null, mapper)) {
            List<String> result = stream.toList();
            assertEquals(5, result.size());
            assertEquals("1:John", result.get(0));
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
    @DisplayName("Test stream with negative offset throws exception")
    public void testStream_NegativeOffset() {
        assertThrows(IllegalArgumentException.class, () -> CsvUtil.stream(testCsvFile, null, -1, 10, Fn.alwaysTrue(), Person.class));
    }

    @Test
    @DisplayName("Test stream with negative count throws exception")
    public void testStream_NegativeCount() {
        assertThrows(IllegalArgumentException.class, () -> CsvUtil.stream(testCsvFile, null, 0, -1, Fn.alwaysTrue(), Person.class));
    }

    @Test
    @DisplayName("Test stream BiFunction with negative offset throws exception")
    public void testStreamBiFunction_NegativeOffset() {
        BiFunction<List<String>, DisposableArray<String>, String> mapper = (columns, row) -> row.get(0);
        assertThrows(IllegalArgumentException.class, () -> CsvUtil.stream(testCsvFile, null, -1, 10, Fn.alwaysTrue(), mapper));
    }

    @Test
    @DisplayName("Test CsvLoader stream() without source throws exception")
    public void testCsvLoaderStreamWithoutSource() {
        BiFunction<List<String>, DisposableArray<String>, String> mapper = (columns, row) -> row.get(0);
        assertThrows(IllegalArgumentException.class, () -> CsvUtil.loader().stream(mapper));
    }

    @Test
    @DisplayName("Test stream(Reader, selectColumns, offset, count, rowFilter, biFunction, closeReader)")
    public void testStream_ReaderWithBiFunctionAndFilter() throws IOException {
        BiFunction<List<String>, DisposableArray<String>, String> mapper = (columns, row) -> row.get(1);
        try (Reader reader = new StringReader(testCsvContent)) {
            List<String> result = CsvUtil.stream(reader, null, 0, Long.MAX_VALUE, row -> !"false".equals(row[3]), mapper, false).toList();
            assertEquals(3, result.size());
        }
    }

    // stream(Reader, ..., Class) with selectColumnNames having property not in bean -> L2012
    @Test
    public void testStream_ReaderBeanClass_SelectedColumnNotABeanProperty_ThrowsIllegalArgument() {
        Reader reader = new StringReader("id,name,unknownColumn\n1,John,val");
        // "unknownColumn" is in CSV headers but not a Person property -> throws when stream is consumed
        try (Stream<Person> stream = CsvUtil.stream(reader, List.of("unknownColumn"), Person.class, false)) {
            assertThrows(IllegalArgumentException.class, stream::toList);
        }
    }

    // stream(Reader, ..., Class) with invalid selectColumnNames -> L2021
    @Test
    public void testStream_ReaderBeanClass_InvalidSelectColumns_ThrowsIllegalArgument() {
        Reader reader = new StringReader("id,name,age\n1,John,25");
        try (Stream<Person> stream = CsvUtil.stream(reader, List.of("nonexistent"), Person.class, false)) {
            assertThrows(IllegalArgumentException.class, stream::toList);
        }
    }

    // stream(Reader, ..., Class) with unsupported target type (not array/collection/map/bean), multiple columns -> L2100
    @Test
    public void testStream_ReaderUnsupportedTargetType_MultipleColumns_ThrowsIllegalArgument() {
        Reader reader = new StringReader("id,name\n1,John");
        // String is not array/collection/map/bean, and we select 2 columns -> L2100
        try (Stream<String> stream = CsvUtil.stream(reader, List.of("id", "name"), 0, Long.MAX_VALUE, null, String.class, false)) {
            assertThrows(IllegalArgumentException.class, stream::toList);
        }
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
    @DisplayName("Test csvToJson(Reader, selectColumns, Writer, beanClass)")
    public void testCsv2jsonReaderWriterWithBeanClass() {
        Reader reader = new StringReader(testCsvContent);
        StringWriter writer = new StringWriter();
        long count = CsvUtil.csvToJson(reader, null, writer, Person.class);
        assertEquals(5, count);
        assertTrue(writer.toString().contains("John"));
    }

    @Test
    @DisplayName("Test csvToJson(Reader, Collection, Writer, Class) with select columns")
    public void testCsvToJsonReaderWriter_SelectColumns() {
        Reader reader = new StringReader(testCsvContent);
        StringWriter writer = new StringWriter();
        long count = CsvUtil.csvToJson(reader, List.of("name", "age"), writer, null);
        assertEquals(5, count);
        String json = writer.toString();
        assertTrue(json.contains("\"name\""));
        assertTrue(json.contains("\"age\""));
    }

    @Test
    @DisplayName("Test csvToJson(Reader, Collection, Writer, Class) with bean class for typed JSON")
    public void testCsvToJsonReaderWriter_TypedWithBeanClass() {
        Reader reader = new StringReader(testCsvContent);
        StringWriter writer = new StringWriter();
        long count = CsvUtil.csvToJson(reader, null, writer, Person.class);
        assertEquals(5, count);
        String json = writer.toString();
        // age should be numeric (not quoted) when using Person class
        assertTrue(json.contains("\"age\":25"));
    }

    @Test
    @DisplayName("Test csvToJson with empty CSV Reader")
    public void testCsvToJson_EmptyReader() {
        Reader reader = new StringReader("");
        StringWriter writer = new StringWriter();
        long count = CsvUtil.csvToJson(reader, null, writer, null);
        assertEquals(0, count);
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
    @DisplayName("Test CsvConverter jsonToCsv with File output")
    public void testCsvConverterJson2CsvFile() throws IOException {
        File jsonFile = tempDir.resolve("input.json").toFile();
        CsvUtil.csvToJson(testCsvFile, jsonFile);

        File csvOutput = tempDir.resolve("output.csv").toFile();

        long count = CsvUtil.converter().source(jsonFile).jsonToCsv(csvOutput);

        assertEquals(5, count);
        assertTrue(csvOutput.exists());
    }

    @Test
    @DisplayName("Test CsvConverter jsonToCsv with Writer output")
    public void testCsvConverterJson2CsvWriter() throws IOException {
        File jsonFile = tempDir.resolve("input.json").toFile();
        CsvUtil.csvToJson(testCsvFile, jsonFile);

        StringWriter writer = new StringWriter();

        long count = CsvUtil.converter().source(jsonFile).jsonToCsv(writer);

        assertEquals(5, count);
        String content = writer.toString();
        assertTrue(content.contains("name"));
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
    @DisplayName("Test csvToJson(File, selectColumns, File, beanClass)")
    public void testCsv2jsonWithSelectColumnsAndBeanClass() throws IOException {
        File jsonFile = tempDir.resolve("test_select_bean.json").toFile();
        List<String> selectColumns = List.of("id", "name", "age");
        long count = CsvUtil.csvToJson(testCsvFile, selectColumns, jsonFile, Person.class);
        assertEquals(5, count);
        assertTrue(jsonFile.exists());
        assertTrue(jsonFile.length() > 0);
    }

    @Test
    @DisplayName("Test jsonToCsv(File, selectHeaders, File)")
    public void testJson2csvWithSelectHeadersFile() throws IOException {
        File jsonFile = tempDir.resolve("test_j2c_select.json").toFile();
        CsvUtil.csvToJson(testCsvFile, jsonFile);

        File csvFile = tempDir.resolve("test_j2c_select.csv").toFile();
        List<String> selectHeaders = List.of("name", "age");
        long count = CsvUtil.jsonToCsv(jsonFile, selectHeaders, csvFile);
        assertTrue(count > 0);
        assertTrue(csvFile.exists());
    }

    @Test
    @DisplayName("Test jsonToCsv(Reader, selectHeaders, Writer)")
    public void testJson2csvReaderWriterWithSelectHeaders() throws IOException {
        File jsonFile = tempDir.resolve("test_j2c_rw_select.json").toFile();
        CsvUtil.csvToJson(testCsvFile, jsonFile);

        StringWriter writer = new StringWriter();
        try (Reader reader = IOUtil.newFileReader(jsonFile)) {
            List<String> selectHeaders = List.of("id", "name");
            long count = CsvUtil.jsonToCsv(reader, selectHeaders, writer);
            assertTrue(count > 0);
            String csv = writer.toString();
            assertTrue(csv.contains("name"));
        }
    }

    @Test
    @DisplayName("Test CsvConverter jsonToCsv with Writer")
    public void testCsvConverterJson2CsvWriterDirect() throws IOException {
        File jsonFile = tempDir.resolve("test_converter_direct.json").toFile();
        CsvUtil.csvToJson(testCsvFile, jsonFile);

        StringWriter writer = new StringWriter();
        long count = CsvUtil.converter().source(jsonFile).jsonToCsv(writer);
        assertTrue(count > 0);
        assertTrue(writer.toString().contains("name"));
    }

    @Test
    @DisplayName("Test csvToJson(File, File) verifies JSON content")
    public void testCsvToJson_VerifyContent() throws IOException {
        File jsonFile = tempDir.resolve("verify_content.json").toFile();
        long count = CsvUtil.csvToJson(testCsvFile, jsonFile);
        assertEquals(5, count);
        String content = Files.readString(jsonFile.toPath());
        assertTrue(content.startsWith("["));
        assertTrue(content.endsWith("]"));
        assertTrue(content.contains("\"name\""));
        assertTrue(content.contains("\"John\""));
    }

    @Test
    @DisplayName("Test csvToJson(File, Collection, File) with null columns includes all")
    public void testCsvToJson_NullColumns() throws IOException {
        File jsonFile = tempDir.resolve("null_cols.json").toFile();
        long count = CsvUtil.csvToJson(testCsvFile, null, jsonFile);
        assertEquals(5, count);
        String content = Files.readString(jsonFile.toPath());
        assertTrue(content.contains("\"id\""));
        assertTrue(content.contains("\"name\""));
        assertTrue(content.contains("\"age\""));
        assertTrue(content.contains("\"active\""));
    }

    @Test
    @DisplayName("Test CsvConverter with offset and count for jsonToCsv")
    public void testCsvConverterJsonToCsvWithOffsetCount() throws IOException {
        File jsonFile = tempDir.resolve("test_j2c_offset.json").toFile();
        CsvUtil.csvToJson(testCsvFile, jsonFile);

        File csvFile = tempDir.resolve("test_j2c_offset.csv").toFile();
        long count = CsvUtil.converter().source(jsonFile).offset(1).count(2).jsonToCsv(csvFile);
        assertEquals(2, count);
    }

    @Test
    @DisplayName("Test CsvConverter jsonToCsv with selectColumns via builder")
    public void testCsvConverterJsonToCsvWithSelectColumns() throws IOException {
        File jsonFile = tempDir.resolve("test_conv_j2c_select.json").toFile();
        CsvUtil.csvToJson(testCsvFile, jsonFile);

        File csvFile = tempDir.resolve("test_conv_j2c_select.csv").toFile();
        long count = CsvUtil.converter().source(jsonFile).selectColumns(List.of("name", "age")).jsonToCsv(csvFile);
        assertTrue(count > 0);
        String content = Files.readString(csvFile.toPath());
        assertTrue(content.contains("name"));
    }

    @Test
    @DisplayName("Test csvToJson basic conversion")
    public void testCsvToJson_Basic() throws IOException {
        StringWriter jsonWriter = new StringWriter();
        CsvUtil.csvToJson(new StringReader(testCsvContent), null, jsonWriter, Person.class);
        String json = jsonWriter.toString();
        assertNotNull(json);
        assertTrue(json.contains("John") || json.length() > 0);
    }

    // csvToJson with invalid selectColumnNames -> L2775
    @Test
    public void testCsvToJson_InvalidSelectColumns_ThrowsIllegalArgument() {
        Reader csvReader = new StringReader("id,name,age\n1,John,25");
        StringWriter writer = new StringWriter();
        assertThrows(IllegalArgumentException.class, () -> CsvUtil.csvToJson(csvReader, List.of("nonexistent"), writer, null));
    }

    // CsvConverter.csvToJson with invalid selectColumns -> exercises builder path
    @Test
    public void testCsvConverter_CsvToJson_WithSelectColumnNames_InvalidColumns_ThrowsIllegalArgument() {
        StringWriter writer = new StringWriter();
        assertThrows(IllegalArgumentException.class,
                () -> CsvUtil.converter().source(new StringReader("id,name\n1,John")).selectColumns(List.of("nonexistent")).csvToJson(writer));
    }

    @Test
    @DisplayName("Test jsonToCsv(File, File) verifies CSV content")
    public void testJsonToCsv_VerifyContent() throws IOException {
        File jsonFile = tempDir.resolve("verify_j2c.json").toFile();
        CsvUtil.csvToJson(testCsvFile, jsonFile);

        File csvFile = tempDir.resolve("verify_j2c.csv").toFile();
        long count = CsvUtil.jsonToCsv(jsonFile, csvFile);
        assertEquals(5, count);
        String content = Files.readString(csvFile.toPath());
        assertTrue(content.contains("John"));
    }

    @Test
    @DisplayName("Test jsonToCsv(Reader, Collection, Writer) with null headers includes all")
    public void testJsonToCsvReaderWriter_NullHeaders() throws IOException {
        File jsonFile = tempDir.resolve("test_j2c_null.json").toFile();
        CsvUtil.csvToJson(testCsvFile, jsonFile);

        StringWriter writer = new StringWriter();
        try (Reader reader = IOUtil.newFileReader(jsonFile)) {
            long count = CsvUtil.jsonToCsv(reader, null, writer);
            assertEquals(5, count);
            String csv = writer.toString();
            assertTrue(csv.contains("id"));
            assertTrue(csv.contains("name"));
        }
    }

    @Test
    @DisplayName("Test jsonToCsv basic conversion")
    public void testJsonToCsv_Basic() throws IOException {
        String json = "[{\"id\":\"1\",\"name\":\"John\",\"age\":25},{\"id\":\"2\",\"name\":\"Jane\",\"age\":30}]";
        java.util.List<String> headers = java.util.Arrays.asList("id", "name", "age");
        StringWriter csvWriter = new StringWriter();
        CsvUtil.jsonToCsv(new StringReader(json), headers, csvWriter);
        String csv = csvWriter.toString();
        assertNotNull(csv);
        assertTrue(csv.contains("id"));
        assertTrue(csv.contains("John"));
    }

    @Test
    @DisplayName("Test CsvLoader stream with closeReaderWhenStreamIsClosed")
    public void testCsvLoaderStreamCloseReader() {
        Reader reader = new StringReader(testCsvContent);
        BiFunction<List<String>, DisposableArray<String>, String> mapper = (columns, row) -> row.get(1);

        try (Stream<String> stream = CsvUtil.loader().source(reader).stream(mapper, true)) {
            List<String> names = stream.toList();
            assertEquals(5, names.size());
        }
    }

    @Test
    @DisplayName("Test CsvLoader stream from Reader source")
    public void testCsvLoaderStreamFromReader() {
        Reader reader = new StringReader(testCsvContent);
        BiFunction<List<String>, DisposableArray<String>, String> mapper = (columns, row) -> row.get(1);

        try (Stream<String> stream = CsvUtil.loader().source(reader).stream(mapper, true)) {
            List<String> names = stream.toList();
            assertEquals(5, names.size());
            assertEquals("John", names.get(0));
        }
    }

    @Test
    @DisplayName("Test CsvLoader stream with selectColumns, offset, count, rowFilter from File")
    public void testCsvLoaderStreamWithAllOptions() {
        Predicate<String[]> filter = row -> "true".equals(row[3]);
        BiFunction<List<String>, DisposableArray<String>, String> mapper = (columns, row) -> row.get(1);

        try (Stream<String> stream = CsvUtil.loader()
                .source(testCsvFile)
                .selectColumns(List.of("name", "active"))
                .offset(0)
                .count(10)
                .rowFilter(filter)
                .stream(mapper)) {
            List<String> names = stream.toList();
            assertEquals(3, names.size());
        }
    }

    @Test
    @DisplayName("CSV builders reject negative pagination at configuration time")
    public void testBuildersRejectNegativePaginationImmediately() {
        assertThrows(IllegalArgumentException.class, () -> CsvUtil.loader().offset(-1));
        assertThrows(IllegalArgumentException.class, () -> CsvUtil.loader().count(-1));
        assertThrows(IllegalArgumentException.class, () -> CsvUtil.converter().offset(-1));
        assertThrows(IllegalArgumentException.class, () -> CsvUtil.converter().count(-1));
    }

    @Test
    public void testLoadRejectsNullRowExtractorImmediately() {
        assertThrows(IllegalArgumentException.class, () -> CsvUtil.load(new StringReader(testCsvContent), null, 0, Long.MAX_VALUE, null,
                (TriConsumer<List<String>, DisposableArray<String>, Object[]>) null));
    }

    // CSVCommon.apply() with escapeCharToBackSlashForWrite=true, previous state=false -> L3294-3295 (finally restores to non-backslash)
    @Test
    public void testCsvLoader_EscapeCharBackSlash_RestoredAfterLoad() {
        // Default escape is false. Set to backslash via builder -> in finally, currentBackSlash=false -> else branch L3295-3296
        Dataset ds = CsvUtil.loader().source(new StringReader("id,name\n1,John")).setEscapeCharToBackSlashForWrite().load();
        assertEquals(2, ds.columnCount());
        // After load, escape char should be restored to default (false)
        assertFalse(CsvUtil.isBackSlashEscapeCharForWrite());
    }

    // ===================== Tests for loader() and CsvLoader =====================

    @Test
    @DisplayName("Test loader() returns CsvLoader instance")
    public void testLoader() {
        CsvUtil.CsvLoader loader = CsvUtil.loader();
        assertNotNull(loader);
    }

    @Test
    @DisplayName("Test CsvLoader with columnTypeMap")
    public void testCsvLoaderWithColumnTypeMap() {
        Map<String, Type<?>> typeMap = new HashMap<>();
        typeMap.put("id", Type.of(String.class));
        typeMap.put("age", Type.of(Integer.class));

        Dataset ds = CsvUtil.loader().source(testCsvFile).columnTypeMap(typeMap).load();

        assertNotNull(ds);
        assertEquals(4, ds.columnCount());
        assertEquals(5, ds.size());
    }

    @Test
    @DisplayName("Test CsvLoader with beanClassForTypeReading")
    public void testCsvLoaderWithBeanClass() {
        Dataset ds = CsvUtil.loader().source(testCsvFile).beanClassForColumnTypeInference(Person.class).load();

        assertNotNull(ds);
        assertEquals(4, ds.columnCount());
        assertEquals(5, ds.size());
    }

    @Test
    @DisplayName("Test CsvLoader chained methods")
    public void testCsvLoaderChainedMethods() {
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
    @DisplayName("Test CsvLoader from Reader source")
    public void testCsvLoaderFromReader() {
        Reader reader = new StringReader(testCsvContent);

        Dataset ds = CsvUtil.loader().source(reader).beanClassForColumnTypeInference(Person.class).load();

        assertNotNull(ds);
        assertEquals(4, ds.columnCount());
        assertEquals(5, ds.size());
    }

    @Test
    @DisplayName("Test CsvLoader selectColumns, offset, count")
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
    @DisplayName("Test CsvLoader supports loading without type config")
    public void testCsvLoaderWithoutTypeConfig() {
        Dataset ds = CsvUtil.loader().source(testCsvFile).load();
        assertNotNull(ds);
        assertEquals(5, ds.size());
        assertEquals(4, ds.columnCount());
    }

    @Test
    @DisplayName("Test CsvLoader with selectColumns and offset/count and beanClass")
    public void testCsvLoaderWithSelectColumnsOffsetCountBeanClass() {
        Dataset ds = CsvUtil.loader()
                .source(testCsvFile)
                .selectColumns(List.of("id", "name"))
                .offset(1)
                .count(2)
                .beanClassForColumnTypeInference(Person.class)
                .load();
        assertNotNull(ds);
        assertEquals(2, ds.columnCount());
        assertEquals(2, ds.size());
    }

    @Test
    @DisplayName("Test CsvLoader with custom line parser")
    public void testCsvLoaderWithCustomLineParser() {
        BiConsumer<String, String[]> customLineParser = (line, output) -> {
            String[] parts = line.split(",");
            System.arraycopy(parts, 0, output, 0, Math.min(parts.length, output.length));
        };

        Dataset ds = CsvUtil.loader().source(testCsvFile).setLineParser(customLineParser).load();
        assertNotNull(ds);
        assertEquals(5, ds.size());
    }

    @Test
    @DisplayName("Test CsvLoader with custom header parser")
    public void testCsvLoaderWithCustomHeaderParserDirect() {
        Function<String, String[]> customHeaderParser = line -> line.split(",");

        Dataset ds = CsvUtil.loader().source(testCsvFile).setHeaderParser(customHeaderParser).load();
        assertNotNull(ds);
        assertEquals(4, ds.columnCount());
    }

    @Test
    @DisplayName("Test CsvLoader setEscapeCharToBackSlashForWrite")
    public void testCsvLoaderSetEscapeChar() {
        CsvUtil.CsvLoader loader = CsvUtil.loader().source(testCsvFile).setEscapeCharToBackSlashForWrite();
        assertNotNull(loader);
        Dataset ds = loader.load();
        assertNotNull(ds);
    }

    @Test
    @DisplayName("Test loader() returns new instance each time")
    public void testLoader_NewInstance() {
        CsvUtil.CsvLoader loader1 = CsvUtil.loader();
        CsvUtil.CsvLoader loader2 = CsvUtil.loader();
        assertNotNull(loader1);
        assertNotNull(loader2);
        assertTrue(loader1 != loader2);
    }

    @Test
    @DisplayName("Test CsvLoader with columnTypeMap from Reader source")
    public void testCsvLoaderWithColumnTypeMapFromReader() {
        Reader reader = new StringReader(testCsvContent);
        Map<String, Type<?>> typeMap = new HashMap<>();
        typeMap.put("age", Type.of(Integer.class));
        typeMap.put("active", Type.of(Boolean.class));

        Dataset ds = CsvUtil.loader().source(reader).columnTypeMap(typeMap).load();
        assertNotNull(ds);
        assertEquals(4, ds.columnCount());
        assertEquals(5, ds.size());
        assertTrue(ds.get(0, 2) instanceof Integer);
    }

    @Test
    @DisplayName("Test CsvLoader with columnTypeMap and selectColumns")
    public void testCsvLoaderWithColumnTypeMapAndSelectColumns() {
        Map<String, Type<?>> typeMap = new HashMap<>();
        typeMap.put("age", Type.of(Integer.class));

        Dataset ds = CsvUtil.loader().source(testCsvFile).columnTypeMap(typeMap).selectColumns(List.of("name", "age")).load();
        assertNotNull(ds);
        assertEquals(2, ds.columnCount());
    }

    @Test
    @DisplayName("Test CsvLoader stream with custom parsers from File")
    public void testCsvLoaderStreamWithCustomParsersFromFile() {
        Function<String, String[]> customHeaderParser = line -> line.split(",");
        BiConsumer<String, String[]> customLineParser = (line, output) -> {
            String[] parts = line.split(",");
            System.arraycopy(parts, 0, output, 0, Math.min(parts.length, output.length));
        };
        BiFunction<List<String>, DisposableArray<String>, String> mapper = (columns, row) -> row.get(1);

        try (Stream<String> stream = CsvUtil.loader().source(testCsvFile).setHeaderParser(customHeaderParser).setLineParser(customLineParser).stream(mapper)) {
            List<String> names = stream.toList();
            assertEquals(5, names.size());
            assertEquals("John", names.get(0));
        }
    }

    @Test
    @DisplayName("CsvLoader stream restores custom parsers if lazy creation fails")
    public void testCsvLoaderStreamRestoresParsersWhenLazyCreationFails() {
        Function<String, String[]> defaultHeaderParser = CsvUtil.getCurrentHeaderParser();
        Function<String, String[]> customHeaderParser = line -> line.split(";");
        BiFunction<List<String>, DisposableArray<String>, String> mapper = (columns, row) -> row.get(0);

        try (Stream<String> stream = CsvUtil.loader().setHeaderParser(customHeaderParser).stream(mapper)) {
            assertThrows(IllegalArgumentException.class, stream::toList);
        }

        assertSame(defaultHeaderParser, CsvUtil.getCurrentHeaderParser());
    }

    @Test
    @DisplayName("Test CsvLoader with beanClassForColumnTypeInference from Reader and rowFilter")
    public void testCsvLoaderBeanClassReaderWithFilter() {
        Reader reader = new StringReader(testCsvContent);
        Predicate<String[]> filter = row -> Integer.parseInt(row[2]) > 28;

        Dataset ds = CsvUtil.loader().source(reader).beanClassForColumnTypeInference(Person.class).rowFilter(filter).load();
        assertNotNull(ds);
        assertTrue(ds.size() >= 2);
    }

    @Test
    @DisplayName("Test CsvLoader with rowFilter")
    public void testCsvLoaderWithRowFilter() {
        Predicate<String[]> filter = row -> Integer.parseInt(row[2]) >= 30;

        Dataset ds = CsvUtil.loader().source(testCsvFile).beanClassForColumnTypeInference(Person.class).rowFilter(filter).load();

        assertNotNull(ds);
        assertEquals(3, ds.size());
    }

    @Test
    @DisplayName("Test CsvLoader with rowExtractor")
    public void testCsvLoaderWithRowExtractor() {
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
    @DisplayName("Test CsvLoader with custom header parser")
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
    @DisplayName("Test CsvLoader stream method with rowMapper")
    public void testCsvLoaderStreamWithRowMapper() {
        BiFunction<List<String>, DisposableArray<String>, String> mapper = (columns, row) -> row.get(1) + ":" + row.get(2);

        try (Stream<String> stream = CsvUtil.loader().source(testCsvFile).stream(mapper)) {
            List<String> results = stream.toList();
            assertEquals(5, results.size());
            assertEquals("John:25", results.get(0));
        }
    }

    @Test
    @DisplayName("Test CsvLoader throws exception without source")
    public void testCsvLoaderWithoutSource() {
        assertThrows(IllegalArgumentException.class, () -> CsvUtil.loader().beanClassForColumnTypeInference(Person.class).load());
    }

    @Test
    @DisplayName("Test CsvLoader cannot set both columnTypeMap and beanClass")
    public void testCsvLoaderMutuallyExclusiveTypeConfig() {
        Map<String, Type<?>> typeMap = new HashMap<>();
        typeMap.put("id", Type.of(String.class));

        assertThrows(IllegalArgumentException.class,
                () -> CsvUtil.loader().source(testCsvFile).columnTypeMap(typeMap).beanClassForColumnTypeInference(Person.class));
    }

    @Test
    @DisplayName("Test CsvLoader cannot set both file and reader source")
    public void testCsvLoaderCannotSetBothSources() {
        Reader reader = new StringReader(testCsvContent);

        assertThrows(IllegalArgumentException.class, () -> CsvUtil.loader().source(testCsvFile).source(reader));
    }

    @Test
    @DisplayName("Test CsvLoader load with TriConsumer rowExtractor")
    public void testCsvLoaderWithRowExtractorLoad() {
        TriConsumer<List<String>, DisposableArray<String>, Object[]> extractor = (columns, row, output) -> {
            for (int i = 0; i < row.length(); i++) {
                output[i] = row.get(i);
            }
        };

        Dataset ds = CsvUtil.loader().source(testCsvFile).load(extractor);
        assertNotNull(ds);
        assertEquals(4, ds.columnCount());
        assertEquals(5, ds.size());
    }

    @Test
    @DisplayName("Test CsvLoader with rowFilter")
    public void testCsvLoaderWithRowFilterOnly() {
        Predicate<String[]> filter = row -> "true".equals(row[3]);

        Dataset ds = CsvUtil.loader().source(testCsvFile).beanClassForColumnTypeInference(Person.class).rowFilter(filter).load();
        assertNotNull(ds);
        assertEquals(3, ds.size());
    }

    @Test
    @DisplayName("Test CsvLoader cannot set both beanClass and columnTypeMap (reverse order)")
    public void testCsvLoaderMutuallyExclusiveTypeConfig_ReverseOrder() {
        Map<String, Type<?>> typeMap = new HashMap<>();
        typeMap.put("id", Type.of(String.class));

        assertThrows(IllegalArgumentException.class,
                () -> CsvUtil.loader().source(testCsvFile).beanClassForColumnTypeInference(Person.class).columnTypeMap(typeMap));
    }

    @Test
    @DisplayName("Test CsvLoader cannot set reader then file source")
    public void testCsvLoaderCannotSetReaderThenFile() {
        Reader reader = new StringReader(testCsvContent);
        assertThrows(IllegalArgumentException.class, () -> CsvUtil.loader().source(reader).source(testCsvFile));
    }

    @Test
    @DisplayName("Test CsvLoader load without source throws exception")
    public void testCsvLoaderLoad_WithoutSource() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> CsvUtil.loader().load());
        assertTrue(exception.getMessage().contains("sourceFile"));
    }

    @Test
    @DisplayName("Test CsvLoader stream without source throws exception")
    public void testCsvLoaderStream_WithoutSource() {
        BiFunction<List<String>, DisposableArray<String>, String> mapper = (columns, row) -> row.get(0);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> CsvUtil.loader().stream(mapper));
        assertTrue(exception.getMessage().contains("sourceReader"));
    }

    @Test
    @DisplayName("Test CsvConverter csvToJson with Writer output")
    public void testCsvConverterCsv2JsonWriter() {
        StringWriter writer = new StringWriter();

        long count = CsvUtil.converter().source(testCsvFile).csvToJson(writer);

        assertEquals(5, count);
        String content = writer.toString();
        assertTrue(content.contains("John"));
        assertTrue(content.contains("["));
        assertTrue(content.contains("]"));
    }

    @Test
    @DisplayName("Test CsvConverter with Reader source")
    public void testCsvConverterWithReaderSource() {
        Reader reader = new StringReader(testCsvContent);
        StringWriter writer = new StringWriter();

        long count = CsvUtil.converter().source(reader).csvToJson(writer);

        assertEquals(5, count);
    }

    @Test
    @DisplayName("Test CsvConverter csvToJson with Writer")
    public void testCsvConverterCsv2JsonWriterDirect() {
        StringWriter writer = new StringWriter();
        long count = CsvUtil.converter().source(testCsvFile).csvToJson(writer);
        assertTrue(count > 0);
        assertTrue(writer.toString().contains("John"));
    }

    @Test
    @DisplayName("Test CsvConverter csvToJson from Reader with Writer")
    public void testCsvConverterCsvToJsonFromReaderWithWriter() {
        Reader reader = new StringReader(testCsvContent);
        StringWriter writer = new StringWriter();

        long count = CsvUtil.converter().source(reader).csvToJson(writer);
        assertEquals(5, count);
        assertTrue(writer.toString().contains("John"));
    }

    @Test
    @DisplayName("Test CsvConverter csvToJson with offset and count from Reader")
    public void testCsvConverterCsvToJsonFromReaderWithOffsetCount() {
        Reader reader = new StringReader(testCsvContent);
        StringWriter writer = new StringWriter();

        long count = CsvUtil.converter().source(reader).offset(1).count(2).csvToJson(writer);
        assertEquals(2, count);
    }

    // ===================== Tests for converter() and CsvConverter =====================

    @Test
    @DisplayName("Test converter() returns CsvConverter instance")
    public void testConverter() {
        CsvUtil.CsvConverter converter = CsvUtil.converter();
        assertNotNull(converter);
    }

    @Test
    @DisplayName("Test converter() returns new instance each time")
    public void testConverter_NewInstance() {
        CsvUtil.CsvConverter conv1 = CsvUtil.converter();
        CsvUtil.CsvConverter conv2 = CsvUtil.converter();
        assertNotNull(conv1);
        assertNotNull(conv2);
        assertTrue(conv1 != conv2);
    }

    @Test
    @DisplayName("Test CsvConverter csvToJson with File output")
    public void testCsvConverterCsv2JsonFile() throws IOException {
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
    @DisplayName("Test CsvConverter with beanClassForTypeWriting")
    public void testCsvConverterWithBeanClassForTypeWriting() throws IOException {
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
    @DisplayName("Test CsvConverter with selectColumns and offset/count")
    public void testCsvConverterWithSelectColumnsOffsetCount() throws IOException {
        File jsonFile = tempDir.resolve("partial.json").toFile();

        long count = CsvUtil.converter().source(testCsvFile).selectColumns(List.of("name", "age")).offset(1).count(2).csvToJson(jsonFile);

        assertEquals(2, count);
    }

    @Test
    @DisplayName("Test CsvConverter throws exception without source")
    public void testCsvConverterWithoutSource() throws IOException {
        File jsonFile = tempDir.resolve("output.json").toFile();

        assertThrows(IllegalArgumentException.class, () -> CsvUtil.converter().csvToJson(jsonFile));
    }

    @Test
    @DisplayName("Test CsvConverter setEscapeCharToBackSlashForWrite")
    public void testCsvConverterSetEscapeChar() throws IOException {
        File jsonFile = tempDir.resolve("escaped.json").toFile();

        long count = CsvUtil.converter().source(testCsvFile).setEscapeCharToBackSlashForWrite().csvToJson(jsonFile);

        assertEquals(5, count);
    }

    @Test
    @DisplayName("Test CsvConverter with selectColumns")
    public void testCsvConverterWithSelectColumns() throws IOException {
        File jsonFile = tempDir.resolve("test_conv_select.json").toFile();
        long count = CsvUtil.converter().source(testCsvFile).selectColumns(List.of("id", "name")).csvToJson(jsonFile);
        assertTrue(count > 0);
        assertTrue(jsonFile.exists());
    }

    @Test
    @DisplayName("Test CsvConverter with custom header parser")
    public void testCsvConverterWithCustomHeaderParser() throws IOException {
        Function<String, String[]> customHeaderParser = line -> line.split(",");

        File jsonFile = tempDir.resolve("test_conv_custom_parser.json").toFile();
        long count = CsvUtil.converter().source(testCsvFile).setHeaderParser(customHeaderParser).csvToJson(jsonFile);
        assertTrue(count > 0);
    }

    @Test
    @DisplayName("Test CsvConverter with custom line parser")
    public void testCsvConverterWithCustomLineParser() throws IOException {
        BiConsumer<String, String[]> customLineParser = (line, output) -> {
            String[] parts = line.split(",");
            System.arraycopy(parts, 0, output, 0, Math.min(parts.length, output.length));
        };

        File jsonFile = tempDir.resolve("test_conv_custom_line_parser.json").toFile();
        long count = CsvUtil.converter().source(testCsvFile).setLineParser(customLineParser).csvToJson(jsonFile);
        assertTrue(count > 0);
    }

    @Test
    @DisplayName("Test CsvConverter setEscapeCharToBackSlashForWrite via builder")
    public void testCsvConverterSetEscapeCharViaBuilder() throws IOException {
        File jsonFile = tempDir.resolve("test_conv_escape.json").toFile();
        long count = CsvUtil.converter().source(testCsvFile).setEscapeCharToBackSlashForWrite().csvToJson(jsonFile);
        assertTrue(count > 0);
    }

    @Test
    @DisplayName("Test CsvConverter csvToJson with beanClass and selectColumns via builder")
    public void testCsvConverterCsvToJsonWithBeanClassSelectColumns() throws IOException {
        File jsonFile = tempDir.resolve("conv_bean_select.json").toFile();
        long count = CsvUtil.converter()
                .source(testCsvFile)
                .selectColumns(List.of("name", "age"))
                .beanClassForColumnTypeInference(Person.class)
                .csvToJson(jsonFile);
        assertEquals(5, count);
        String content = Files.readString(jsonFile.toPath());
        assertTrue(content.contains("\"age\":"));
    }

    @Test
    @DisplayName("Test CsvConverter jsonToCsv throws exception without source")
    public void testCsvConverterJsonToCsvWithoutSource() throws IOException {
        File csvFile = tempDir.resolve("no_source.csv").toFile();
        assertThrows(IllegalArgumentException.class, () -> CsvUtil.converter().jsonToCsv(csvFile));
    }

    @Test
    @DisplayName("Test CsvConverter jsonToCsv with Writer throws exception without source")
    public void testCsvConverterJsonToCsvWriterWithoutSource() {
        StringWriter writer = new StringWriter();
        assertThrows(IllegalArgumentException.class, () -> CsvUtil.converter().jsonToCsv(writer));
    }

    @Test
    @DisplayName("Test CsvConverter csvToJson with Writer throws exception without source")
    public void testCsvConverterCsvToJsonWriterWithoutSource() {
        StringWriter writer = new StringWriter();
        assertThrows(IllegalArgumentException.class, () -> CsvUtil.converter().csvToJson(writer));
    }

    @Test
    @DisplayName("Test CsvConverter cannot set both file and reader source")
    public void testCsvConverterCannotSetBothSources() {
        Reader reader = new StringReader(testCsvContent);
        assertThrows(IllegalArgumentException.class, () -> CsvUtil.converter().source(testCsvFile).source(reader));
    }

    @Test
    @DisplayName("Test CsvConverter cannot set reader then file source")
    public void testCsvConverterCannotSetReaderThenFile() {
        Reader reader = new StringReader(testCsvContent);
        assertThrows(IllegalArgumentException.class, () -> CsvUtil.converter().source(reader).source(testCsvFile));
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
        Map<String, Object> result = CsvUtil.jsonParser.parse(json, Map.class);
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

    @Test
    @DisplayName("Test CSV_HEADER_PARSER with empty string")
    public void testCSV_HEADER_PARSER_EmptyString() {
        String[] result = CsvUtil.CSV_HEADER_PARSER.apply("");
        assertNotNull(result);
    }

    @Test
    @DisplayName("Test CSV_HEADER_PARSER with single field")
    public void testCSV_HEADER_PARSER_SingleField() {
        String[] result = CsvUtil.CSV_HEADER_PARSER.apply("name");
        assertEquals(1, result.length);
        assertEquals("name", result[0]);
    }

    @Test
    @DisplayName("Test CSV_LINE_PARSER with empty fields")
    public void testCSV_LINE_PARSER_EmptyFields() {
        String line = "John,,NYC";
        String[] output = new String[3];
        CsvUtil.CSV_LINE_PARSER.accept(line, output);
        assertEquals("John", output[0]);
        assertEquals("", output[1]);
        assertEquals("NYC", output[2]);
    }

    @Test
    @DisplayName("Test CSV_HEADER_PARSER_BY_SPLITTER with single field")
    public void testCSV_HEADER_PARSER_BY_SPLITTER_SingleField() {
        String[] result = CsvUtil.CSV_HEADER_PARSER_BY_SPLITTER.apply("name");
        assertEquals(1, result.length);
        assertEquals("name", result[0]);
    }

    @Test
    @DisplayName("Test CSV_LINE_PARSER_BY_SPLITTER with empty fields")
    public void testCSV_LINE_PARSER_BY_SPLITTER_EmptyFields() {
        String line = "John,,NYC";
        String[] output = new String[3];
        CsvUtil.CSV_LINE_PARSER_BY_SPLITTER.accept(line, output);
        assertEquals("John", output[0]);
        assertEquals("NYC", output[2]);
    }

    @Test
    @DisplayName("Test CSV_HEADER_PARSER_IN_JSON with single element")
    public void testCSV_HEADER_PARSER_IN_JSON_SingleElement() {
        String header = "[\"Name\"]";
        String[] result = CsvUtil.CSV_HEADER_PARSER_IN_JSON.apply(header);
        assertEquals(1, result.length);
        assertEquals("Name", result[0]);
    }

    @Test
    @DisplayName("Test CSV_LINE_PARSER_IN_JSON with single element")
    public void testCSV_LINE_PARSER_IN_JSON_SingleElement() {
        String line = "[\"value\"]";
        String[] output = new String[1];
        CsvUtil.CSV_LINE_PARSER_IN_JSON.accept(line, output);
        assertEquals("value", output[0]);
    }

    @Test
    @DisplayName("Test CSV_HEADER_PARSER_BY_SPLITTER with quoted values")
    public void testCsvHeaderParserBySplitter() {
        String[] headers = CsvUtil.CSV_HEADER_PARSER_BY_SPLITTER.apply("\"Name\",\"Age\",\"City\"");
        assertEquals(3, headers.length);
        assertEquals("Name", headers[0]);
        assertEquals("Age", headers[1]);
        assertEquals("City", headers[2]);
    }

    @Test
    @DisplayName("Test CSV_LINE_PARSER_BY_SPLITTER with quoted values")
    public void testCsvLineParserBySplitter() {
        String[] row = new String[3];
        CsvUtil.CSV_LINE_PARSER_BY_SPLITTER.accept("\"John\",\"30\",\"NYC\"", row);
        assertEquals("John", row[0]);
        assertEquals("30", row[1]);
        assertEquals("NYC", row[2]);
    }

    // ===================== Bug-fix and RFC 4180 regression tests =====================

    @Test
    @DisplayName("csvToJson must not close caller-provided Reader/Writer")
    public void testCsvToJson_DoesNotCloseCallerStreams() {
        String csv = "name,age\nJohn,30\nJane,25\n";
        TrackingReader reader = new TrackingReader(new StringReader(csv));
        TrackingWriter writer = new TrackingWriter(new StringWriter());

        long count = CsvUtil.csvToJson(reader, null, writer, null);

        assertEquals(2, count);
        assertFalse(reader.closed, "csvToJson must not close the caller-provided Reader");
        assertFalse(writer.closed, "csvToJson must not close the caller-provided Writer");
        assertTrue(writer.delegate.toString().contains("\"name\":\"John\""));
    }

    @Test
    @DisplayName("jsonToCsv must not close caller-provided Reader/Writer")
    public void testJsonToCsv_DoesNotCloseCallerStreams() {
        String json = "[{\"name\":\"John\",\"age\":30},{\"name\":\"Jane\",\"age\":25}]";
        TrackingReader reader = new TrackingReader(new StringReader(json));
        TrackingWriter writer = new TrackingWriter(new StringWriter());

        long count = CsvUtil.jsonToCsv(reader, null, writer);

        assertTrue(count >= 1);
        assertFalse(reader.closed, "jsonToCsv must not close the caller-provided Reader");
        assertFalse(writer.closed, "jsonToCsv must not close the caller-provided Writer");
        assertTrue(writer.delegate.toString().length() > 0);
    }

    @Test
    @DisplayName("load with quoted field containing comma and escaped quote")
    public void testLoad_RFC4180EmbeddedSpecials() throws IOException {
        File f = tempDir.resolve("rfc4180.csv").toFile();
        // Note: BufferedReader.readLine() splits records on \n; embedded literal newlines inside
        // quoted fields are not supported by the line-based loader. We test embedded comma and
        // escaped-double-quote here.
        Files.writeString(f.toPath(), "name,note\n\"Doe, John\",hello\n\"He said \"\"hi\"\"\",end\n");
        Dataset ds = CsvUtil.load(f);
        assertEquals(2, ds.size());
        assertEquals("Doe, John", ds.get(0, 0));
        assertEquals("hello", ds.get(0, 1));
        assertEquals("He said \"hi\"", ds.get(1, 0));
    }

    @Test
    @DisplayName("load with last line missing trailing newline")
    public void testLoad_LastLineNoTrailingNewline() throws IOException {
        File f = tempDir.resolve("noeol.csv").toFile();
        Files.writeString(f.toPath(), "a,b,c\n1,2,3"); // no trailing \n
        Dataset ds = CsvUtil.load(f);
        assertEquals(1, ds.size());
        assertEquals("1", ds.get(0, 0));
        assertEquals("3", ds.get(0, 2));
    }

    @Test
    @DisplayName("load duplicate header names: rejected by Dataset construction")
    public void testLoad_DuplicateHeaderNames() throws IOException {
        File f = tempDir.resolve("dup.csv").toFile();
        Files.writeString(f.toPath(), "a,a,b\n1,2,3\n");
        // Dataset disallows duplicate column names, so the loader surfaces it as IAE.
        assertThrows(IllegalArgumentException.class, () -> CsvUtil.load(f));
    }

    @Test
    @DisplayName("load with BOM in first column header")
    public void testLoad_BomInHeader() throws IOException {
        File f = tempDir.resolve("bom.csv").toFile();
        // UTF-8 BOM + standard content
        byte[] bom = new byte[] { (byte) 0xEF, (byte) 0xBB, (byte) 0xBF };
        byte[] body = "id,name\n1,John\n".getBytes(java.nio.charset.StandardCharsets.UTF_8);
        byte[] all = new byte[bom.length + body.length];
        System.arraycopy(bom, 0, all, 0, bom.length);
        System.arraycopy(body, 0, all, bom.length, body.length);
        Files.write(f.toPath(), all);

        Dataset ds = CsvUtil.load(f);
        // The first column header still includes the BOM character because CsvUtil does not strip it.
        // This documents current behavior so callers know to strip BOM up-front when targeting UTF-8.
        String firstCol = ds.columnNames().get(0);
        assertTrue(firstCol.endsWith("id"), "expected header to end with 'id', was: <" + firstCol + ">");
        // If the header is exactly "id" the BOM has been silently stripped; otherwise it hasn't.
        // Either way assert we can read a row.
        assertEquals(1, ds.size());
    }

    @Test
    @DisplayName("CSV_LINE_PARSER produces empty trailing field for trailing comma")
    public void testTrailingCommaProducesEmptyField() {
        String[] out = new String[4];
        CsvUtil.CSV_LINE_PARSER.accept("a,b,c,", out);
        assertEquals("a", out[0]);
        assertEquals("b", out[1]);
        assertEquals("c", out[2]);
        assertEquals("", out[3]);
    }

    @Test
    @DisplayName("CSV stream() closes the wrapped reader (closeReaderWhenStreamIsClosed=true)")
    public void testStream_ClosesReaderWhenRequested() {
        String csv = "id,name\n1,John\n2,Jane\n";
        TrackingReader reader = new TrackingReader(new StringReader(csv));
        try (Stream<Object[]> s = CsvUtil.stream(reader, Object[].class, true)) {
            s.toList();
        }
        assertTrue(reader.closed, "stream(... closeReaderWhenStreamIsClosed=true) must close caller's Reader");
    }

    @Test
    @DisplayName("CSV stream() does NOT close the reader when closeReaderWhenStreamIsClosed=false")
    public void testStream_DoesNotCloseReaderWhenNotRequested() {
        String csv = "id,name\n1,John\n";
        TrackingReader reader = new TrackingReader(new StringReader(csv));
        try (Stream<Object[]> s = CsvUtil.stream(reader, Object[].class, false)) {
            s.toList();
        }
        assertFalse(reader.closed);
    }

    @Test
    @DisplayName("CSV stream recycles pooled reader for empty source")
    public void testStream_EmptySourceRecyclesBorrowedBufferedReader() {
        List<java.io.BufferedReader> drainedReaders = new ArrayList<>();
        java.io.BufferedReader markerReader = null;
        java.io.BufferedReader nextReader = null;

        try {
            for (int i = 0; i < 64; i++) {
                drainedReaders.add(Objectory.createBufferedReader("drain-" + i));
            }

            markerReader = Objectory.createBufferedReader("marker");
            Objectory.recycle(markerReader);

            BiFunction<List<String>, DisposableArray<String>, String> mapper = (columns, row) -> row.get(0);

            try (Stream<String> stream = CsvUtil.stream(new StringReader(""), null, 0, Long.MAX_VALUE, null, mapper, false)) {
                assertTrue(stream.toList().isEmpty());
            }

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

    // -------------------------------------------------------------------------
    // Backslash round-trip tests (regression for REPLACEMENT_CHARS['\\'] = null bug)
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("backslash-escape mode: writeField output round-trips through load")
    public void testBackslashEscapeMode_WriteFieldRoundTripThroughLoad() throws IOException {
        // regression: in backslash-escape mode literal backslashes were written raw
        // (REPLACEMENT_CHARS_BACK_SLASH['\\'] == null), so the default CsvParser (escape = backslash)
        // collapsed "\\" to "\" and corrupted backslash-before-quote sequences on read-back
        CsvUtil.setEscapeCharToBackSlashForWrite();
        try {
            final String value = "c:\\temp\\\"x\".txt"; // contains backslash and backslash-before-quote
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
        }
    }

    // --- Helpers ---

    /**
     * Reader wrapper that records whether close() was invoked.
     */
    private static final class TrackingReader extends Reader {
        final Reader delegate;
        boolean closed = false;

        TrackingReader(Reader delegate) {
            this.delegate = delegate;
        }

        @Override
        public int read(char[] cbuf, int off, int len) throws IOException {
            return delegate.read(cbuf, off, len);
        }

        @Override
        public void close() throws IOException {
            closed = true;
            delegate.close();
        }
    }

    /**
     * Writer wrapper that records whether close() was invoked.
     */
    private static final class TrackingWriter extends java.io.Writer {
        final StringWriter delegate;
        boolean closed = false;

        TrackingWriter(StringWriter delegate) {
            this.delegate = delegate;
        }

        @Override
        public void write(char[] cbuf, int off, int len) {
            delegate.write(cbuf, off, len);
        }

        @Override
        public void flush() {
            delegate.flush();
        }

        @Override
        public void close() {
            closed = true;
        }
    }

    // --- regression tests for 2026-06-11 deep-review fixes ---

    @org.junit.jupiter.api.Test
    public void testCsvToJsonEscapesHeaderKeys() {
        // regression: JSON keys were written raw, so a header containing '"' produced invalid JSON
        final String csv = "\"a\"\"b\",x\n1,2\n";
        final java.io.StringWriter out = new java.io.StringWriter();

        CsvUtil.csvToJson(new java.io.StringReader(csv), null, out, null);

        final String json = out.toString();
        org.junit.jupiter.api.Assertions.assertTrue(json.contains("\"a\\\"b\""), json);

        // the output must be parseable JSON (one data row with two columns)
        final java.util.List<java.util.Map<String, Object>> parsed = N.fromJson(json, java.util.List.class);
        org.junit.jupiter.api.Assertions.assertEquals(1, parsed.size());
        org.junit.jupiter.api.Assertions.assertEquals("1", parsed.get(0).get("a\"b"));
        org.junit.jupiter.api.Assertions.assertEquals("2", parsed.get(0).get("x"));
    }

    // =========================================================================
    // 2026-06-12: selectColumnNames / selectCsvHeaders null-vs-empty contract.
    // Convention: a null selection means "not specified" -> include ALL columns;
    // an empty (non-null) selection is an explicit selection of NO columns and
    // yields a degenerate-but-constructible zero-column result (NOT an exception,
    // NOT all-columns). For jsonToCsv: null -> all keys, empty -> empty output.
    // =========================================================================

    @Test
    @DisplayName("load(File, null selection) includes ALL columns")
    public void testLoad_File_NullSelectColumns_IncludesAll() {
        Dataset ds = CsvUtil.load(testCsvFile, (List<String>) null);
        assertEquals(4, ds.columnCount());
        assertEquals(5, ds.size());
    }

    @Test
    @DisplayName("load(File, empty selection) selects NO columns (zero-column result) [Family A]")
    public void testLoad_File_EmptySelectColumns_ZeroColumns() {
        Dataset ds = CsvUtil.load(testCsvFile, List.<String> of());
        assertEquals(0, ds.columnCount());
    }

    @Test
    @DisplayName("load(Reader, empty selection) selects NO columns (zero-column result) [Family A]")
    public void testLoad_Reader_EmptySelectColumns_ZeroColumns() {
        Dataset ds = CsvUtil.load(new StringReader(testCsvContent), List.<String> of());
        assertEquals(0, ds.columnCount());
    }

    @Test
    @DisplayName("load(File, empty selection, beanClass) selects NO columns (zero-column result) [Family B]")
    public void testLoad_BeanClass_EmptySelectColumns_ZeroColumns() {
        Dataset ds = CsvUtil.load(testCsvFile, List.<String> of(), Person.class);
        assertEquals(0, ds.columnCount());
    }

    @Test
    @DisplayName("load(Reader, empty selection, columnTypeMap) selects NO columns (zero-column result) [Family C]")
    public void testLoad_ColumnTypeMap_EmptySelectColumns_ZeroColumns() {
        Map<String, Type<?>> typeMap = new HashMap<>();
        typeMap.put("age", Type.of(Integer.class));
        Dataset ds = CsvUtil.load(new StringReader(testCsvContent), List.<String> of(), 0, Long.MAX_VALUE, (Predicate<? super String[]>) null, typeMap);
        assertEquals(0, ds.columnCount());
    }

    @Test
    @DisplayName("load(File, empty selection, rowExtractor) selects NO columns (zero-column result) [Family D]")
    public void testLoad_RowExtractor_EmptySelectColumns_ZeroColumns() {
        TriConsumer<List<String>, DisposableArray<String>, Object[]> extractor = (columns, row, output) -> {
            for (int i = 0; i < output.length; i++) {
                output[i] = row.get(i);
            }
        };
        Dataset ds = CsvUtil.load(testCsvFile, List.<String> of(), extractor);
        assertEquals(0, ds.columnCount());
    }

    @Test
    @DisplayName("stream(File, empty selection, beanClass) is consumable without throwing [Family E]")
    public void testStream_File_BeanClass_EmptySelectColumns_Consumable() {
        try (Stream<Person> stream = CsvUtil.stream(testCsvFile, List.<String> of(), Person.class)) {
            assertNotNull(stream.toList());
        }
    }

    @Test
    @DisplayName("stream(Reader, empty selection, beanClass) is consumable without throwing [Family E]")
    public void testStream_Reader_BeanClass_EmptySelectColumns_Consumable() {
        try (Stream<Person> stream = CsvUtil.stream(new StringReader(testCsvContent), List.<String> of(), Person.class, true)) {
            assertNotNull(stream.toList());
        }
    }

    @Test
    @DisplayName("stream(File, empty selection, rowMapper) is consumable without throwing [Family F]")
    public void testStream_RowMapper_EmptySelectColumns_Consumable() {
        BiFunction<List<String>, DisposableArray<String>, String> mapper = (columns, row) -> columns.toString();
        try (Stream<String> stream = CsvUtil.stream(testCsvFile, List.<String> of(), mapper)) {
            assertNotNull(stream.toList());
        }
    }

    @Test
    @DisplayName("csvToJson(Reader, empty selection, Writer) does not throw and returns a count [Family G]")
    public void testCsvToJson_EmptySelectColumns_NoThrow() {
        StringWriter writer = new StringWriter();
        long count = CsvUtil.csvToJson(new StringReader(testCsvContent), List.<String> of(), writer, null);
        assertEquals(5, count);
    }

    @Test
    @DisplayName("jsonToCsv(Reader, null headers, Writer) includes ALL properties of the first object [Family H]")
    public void testJsonToCsv_NullSelectHeaders_IncludesAll() {
        String json = "[{\"id\":\"1\",\"name\":\"John\"},{\"id\":\"2\",\"name\":\"Jane\"}]";
        StringWriter writer = new StringWriter();
        long count = CsvUtil.jsonToCsv(new StringReader(json), null, writer);
        assertEquals(2, count);
        String csv = writer.toString();
        assertTrue(csv.contains("id"));
        assertTrue(csv.contains("name"));
    }

    @Test
    @DisplayName("jsonToCsv(Reader, empty headers, Writer) does not throw and selects NO columns [Family H]")
    public void testJsonToCsv_EmptySelectHeaders_NoThrow() {
        String json = "[{\"id\":\"1\",\"name\":\"John\"}]";
        StringWriter writer = new StringWriter();
        long count = CsvUtil.jsonToCsv(new StringReader(json), List.<String> of(), writer);
        assertEquals(1, count);
        // empty selection -> zero columns -> the keys are not emitted as headers
        assertFalse(writer.toString().contains("id"));
        assertFalse(writer.toString().contains("name"));
    }

    @Test
    @DisplayName("CsvLoader builder: empty selectColumns selects NO columns (zero-column result)")
    public void testLoaderBuilder_EmptySelectColumns_ZeroColumns() {
        Dataset ds = CsvUtil.loader().source(testCsvFile).selectColumns(List.<String> of()).load();
        assertEquals(0, ds.columnCount());
    }
}

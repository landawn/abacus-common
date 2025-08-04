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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.type.Type;
import com.landawn.abacus.util.function.TriConsumer;

import lombok.Data;

/**
 * Unit tests for CSVUtil class.
 */
public class CSVUtil100Test extends TestBase {

    @TempDir
    Path tempDir;

    private File testCsvFile;
    private String testCsvContent;

    @BeforeEach
    public void setUp() throws IOException {
        // Create test CSV content
        testCsvContent = "id,name,age,active\n" + "1,John,25,true\n" + "2,Jane,30,true\n" + "3,Bob,35,false\n" + "4,Alice,28,true\n" + "5,Charlie,40,false\n";

        // Create test CSV file
        testCsvFile = tempDir.resolve("test.csv").toFile();
        Files.writeString(testCsvFile.toPath(), testCsvContent);
    }

    @AfterEach
    public void tearDown() {
        // Reset parsers to default
        CSVUtil.resetCSVHeaderParser();
        CSVUtil.resetCSVLineParser();
        CSVUtil.resetEscapeCharForWrite();
    }

    @Test
    @DisplayName("Test setCSVHeaderParser and getCurrentHeaderParser")
    public void testSetAndGetCSVHeaderParser() {
        Function<String, String[]> customParser = line -> line.split(";");

        CSVUtil.setCSVHeaderParser(customParser);
        assertSame(customParser, CSVUtil.getCurrentHeaderParser());

        // Test null check
        assertThrows(IllegalArgumentException.class, () -> CSVUtil.setCSVHeaderParser(null));
    }

    @Test
    @DisplayName("Test setCSVLineParser and getCurrentLineParser")
    public void testSetAndGetCSVLineParser() {
        BiConsumer<String, String[]> customParser = (line, output) -> {
            String[] parts = line.split(";");
            System.arraycopy(parts, 0, output, 0, Math.min(parts.length, output.length));
        };

        CSVUtil.setCSVLineParser(customParser);
        assertSame(customParser, CSVUtil.getCurrentLineParser());

        // Test null check
        assertThrows(IllegalArgumentException.class, () -> CSVUtil.setCSVLineParser(null));
    }

    @Test
    @DisplayName("Test resetCSVHeaderParser")
    public void testResetCSVHeaderParser() {
        Function<String, String[]> defaultParser = CSVUtil.getCurrentHeaderParser();
        Function<String, String[]> customParser = line -> line.split(";");

        CSVUtil.setCSVHeaderParser(customParser);
        CSVUtil.resetCSVHeaderParser();

        assertEquals(defaultParser, CSVUtil.getCurrentHeaderParser());
    }

    @Test
    @DisplayName("Test resetCSVLineParser")
    public void testResetCSVLineParser() {
        BiConsumer<String, String[]> defaultParser = CSVUtil.getCurrentLineParser();
        BiConsumer<String, String[]> customParser = (line, output) -> {
        };

        CSVUtil.setCSVLineParser(customParser);
        CSVUtil.resetCSVLineParser();

        assertEquals(defaultParser, CSVUtil.getCurrentLineParser());
    }

    @Test
    @DisplayName("Test loadCSV(File) method")
    public void testLoadCSVFromFile() {
        DataSet dataSet = CSVUtil.loadCSV(testCsvFile);

        assertNotNull(dataSet);
        assertEquals(4, dataSet.columnCount());
        assertEquals(5, dataSet.size());
        assertEquals(List.of("id", "name", "age", "active"), dataSet.columnNameList());
    }

    @Test
    @DisplayName("Test loadCSV(File, Collection<String>) method")
    public void testLoadCSVFromFileWithSelectColumns() {
        List<String> selectColumns = List.of("name", "age");
        DataSet dataSet = CSVUtil.loadCSV(testCsvFile, selectColumns);

        assertNotNull(dataSet);
        assertEquals(2, dataSet.columnCount());
        assertEquals(5, dataSet.size());
        assertEquals(selectColumns, dataSet.columnNameList());
    }

    @Test
    @DisplayName("Test loadCSV(File, Collection<String>, long, long) method")
    public void testLoadCSVFromFileWithOffsetAndCount() {
        List<String> selectColumns = List.of("name", "age");
        DataSet dataSet = CSVUtil.loadCSV(testCsvFile, selectColumns, 1, 2);

        assertNotNull(dataSet);
        assertEquals(2, dataSet.columnCount());
        assertEquals(2, dataSet.size());
        assertEquals("Jane", dataSet.get(0, 0));
        assertEquals("Bob", dataSet.get(1, 0));
    }

    @Test
    @DisplayName("Test loadCSV(File, Collection<String>, long, long, Predicate) method")
    public void testLoadCSVFromFileWithFilter() {
        Predicate<String[]> rowFilter = row -> "true".equals(row[3]);
        DataSet dataSet = CSVUtil.loadCSV(testCsvFile, null, 0, Long.MAX_VALUE, rowFilter);

        assertNotNull(dataSet);
        assertEquals(3, dataSet.size()); // Only rows with active=true
    }

    @Test
    @DisplayName("Test loadCSV(Reader) method")
    public void testLoadCSVFromReader() throws IOException {
        try (Reader reader = new StringReader(testCsvContent)) {
            DataSet dataSet = CSVUtil.loadCSV(reader);

            assertNotNull(dataSet);
            assertEquals(4, dataSet.columnCount());
            assertEquals(5, dataSet.size());
        }
    }

    @Test
    @DisplayName("Test loadCSV(Reader, Collection<String>) method")
    public void testLoadCSVFromReaderWithSelectColumns() throws IOException {
        List<String> selectColumns = List.of("id", "name");

        try (Reader reader = new StringReader(testCsvContent)) {
            DataSet dataSet = CSVUtil.loadCSV(reader, selectColumns);

            assertNotNull(dataSet);
            assertEquals(2, dataSet.columnCount());
            assertEquals(selectColumns, dataSet.columnNameList());
        }
    }

    @Test
    @DisplayName("Test loadCSV(Reader, Collection<String>, long, long) method")
    public void testLoadCSVFromReaderWithOffsetAndCount() throws IOException {
        try (Reader reader = new StringReader(testCsvContent)) {
            DataSet dataSet = CSVUtil.loadCSV(reader, null, 2, 2);

            assertNotNull(dataSet);
            assertEquals(2, dataSet.size());
            assertEquals("Bob", dataSet.get(0, 1));
            assertEquals("Alice", dataSet.get(1, 1));
        }
    }

    @Test
    @DisplayName("Test loadCSV(Reader, Collection<String>, long, long, Predicate) method")
    public void testLoadCSVFromReaderWithFilter() throws IOException {
        Predicate<String[]> rowFilter = row -> Integer.parseInt(row[2]) > 30;

        try (Reader reader = new StringReader(testCsvContent)) {
            DataSet dataSet = CSVUtil.loadCSV(reader, null, 0, Long.MAX_VALUE, rowFilter);

            assertNotNull(dataSet);
            assertEquals(2, dataSet.size()); // Only Bob (35) and Charlie (40)
        }
    }

    @Test
    @DisplayName("Test loadCSV with beanClassForColumnType")
    public void testLoadCSVWithBeanClass() {
        DataSet dataSet = CSVUtil.loadCSV(testCsvFile, TestBean.class);

        assertNotNull(dataSet);
        assertEquals(4, dataSet.columnCount());

        // Check that types are converted properly
        assertEquals(1, (Integer) dataSet.get(0, 0)); // id as Integer
        assertEquals("John", dataSet.get(0, 1)); // name as String
        assertEquals(25, (Integer) dataSet.get(0, 2)); // age as Integer
        assertEquals(true, dataSet.get(0, 3)); // active as Boolean
    }

    @Test
    @DisplayName("Test loadCSV with columnTypeMap")
    public void testLoadCSVWithColumnTypeMap() {
        Map<String, Type<?>> columnTypeMap = new HashMap<>();
        columnTypeMap.put("id", Type.of(Integer.class));
        columnTypeMap.put("age", Type.of(Integer.class));
        columnTypeMap.put("active", Type.of(Boolean.class));

        DataSet dataSet = CSVUtil.loadCSV(testCsvFile, columnTypeMap);

        assertNotNull(dataSet);
        assertEquals(3, dataSet.columnCount()); // Only mapped columns

        // Check types
        assertTrue(dataSet.get(0, 0) instanceof Integer);
        assertTrue(dataSet.get(0, 1) instanceof Integer);
        assertTrue(dataSet.get(0, 2) instanceof Boolean);
    }

    @Test
    @DisplayName("Test loadCSV with rowExtractor")
    public void testLoadCSVWithRowExtractor() {
        TriConsumer<List<String>, NoCachingNoUpdating.DisposableArray<String>, Object[]> rowExtractor = (columnNames, rowData, output) -> {
            for (int i = 0; i < output.length; i++) {
                if ("id".equals(columnNames.get(i)) || "age".equals(columnNames.get(i))) {
                    output[i] = Integer.parseInt(rowData.get(i));
                } else if ("active".equals(columnNames.get(i))) {
                    output[i] = Boolean.parseBoolean(rowData.get(i));
                } else {
                    output[i] = rowData.get(i);
                }
            }
        };

        DataSet dataSet = CSVUtil.loadCSV(testCsvFile, rowExtractor);

        assertNotNull(dataSet);
        assertEquals(4, dataSet.columnCount());

        // Check types
        assertTrue(dataSet.get(0, 0) instanceof Integer);
        assertTrue(dataSet.get(0, 1) instanceof String);
        assertTrue(dataSet.get(0, 2) instanceof Integer);
        assertTrue(dataSet.get(0, 3) instanceof Boolean);
    }

    @Test
    @DisplayName("Test stream(File, Class) method")
    public void testStreamFromFileWithClass() {
        List<TestBean> beans = CSVUtil.stream(testCsvFile, TestBean.class).toList();

        assertEquals(5, beans.size());
        assertEquals("John", beans.get(0).name);
        assertEquals(25, beans.get(0).age);
        assertTrue(beans.get(0).active);
    }

    @Test
    @DisplayName("Test stream with selectColumnNames")
    public void testStreamWithSelectColumnNames() {
        List<Map<String, String>> maps = CSVUtil.stream(testCsvFile, List.of("name", "age"), Clazz.ofMap(String.class, String.class)).toList();

        assertEquals(5, maps.size());
        assertEquals(2, maps.get(0).size());
        assertTrue(maps.get(0).containsKey("name"));
        assertTrue(maps.get(0).containsKey("age"));
        assertFalse(maps.get(0).containsKey("id"));
    }

    @Test
    @DisplayName("Test stream with offset and count")
    public void testStreamWithOffsetAndCount() {
        List<TestBean> beans = CSVUtil.stream(testCsvFile, null, 1, 3, null, TestBean.class).toList();

        assertEquals(3, beans.size());
        assertEquals("Jane", beans.get(0).name);
        assertEquals("Alice", beans.get(2).name);
    }

    @Test
    @DisplayName("Test stream with rowFilter")
    public void testStreamWithRowFilter() {
        Predicate<String[]> rowFilter = row -> Integer.parseInt(row[2]) < 30;

        List<TestBean> beans = CSVUtil.stream(testCsvFile, null, 0, Long.MAX_VALUE, rowFilter, TestBean.class).toList();

        assertEquals(2, beans.size());
        assertTrue(beans.stream().allMatch(b -> b.age < 30));
    }

    @Test
    @DisplayName("Test stream with rowMapper")
    public void testStreamWithRowMapper() {
        BiFunction<List<String>, NoCachingNoUpdating.DisposableArray<String>, Person> rowMapper = (columnNames, rowData) -> {
            Person p = new Person();
            for (int i = 0; i < columnNames.size(); i++) {
                switch (columnNames.get(i)) {
                    case "name":
                        p.name = rowData.get(i);
                        break;
                    case "age":
                        p.age = Integer.parseInt(rowData.get(i));
                        break;
                }
            }
            return p;
        };

        List<Person> persons = CSVUtil.stream(testCsvFile, rowMapper).toList();

        assertEquals(5, persons.size());
        assertEquals("John", persons.get(0).name);
        assertEquals(25, persons.get(0).age);
    }

    @Test
    @DisplayName("Test stream from Reader")
    public void testStreamFromReader() throws IOException {
        try (Reader reader = new StringReader(testCsvContent)) {
            List<TestBean> beans = CSVUtil.stream(reader, TestBean.class, true).toList();

            assertEquals(5, beans.size());
            assertEquals("John", beans.get(0).name);
        }
    }

    @Test
    @DisplayName("Test stream with array target type")
    public void testStreamWithArrayType() {
        List<Object[]> arrays = CSVUtil.stream(testCsvFile, Object[].class).toList();

        assertEquals(5, arrays.size());
        assertEquals(4, arrays.get(0).length);
        assertEquals("1", arrays.get(0)[0]);
        assertEquals("John", arrays.get(0)[1]);
    }

    @Test
    @DisplayName("Test stream with Collection target type")
    public void testStreamWithCollectionType() {
        List<List<String>> lists = CSVUtil.stream(testCsvFile, Clazz.ofList(String.class)).toList();

        assertEquals(5, lists.size());
        assertEquals(4, lists.get(0).size());
        assertEquals("1", lists.get(0).get(0));
        assertEquals("John", lists.get(0).get(1));
    }

    @Test
    @DisplayName("Test edge cases and error conditions")
    public void testEdgeCases() throws IOException {
        // Test empty file
        File emptyFile = tempDir.resolve("empty.csv").toFile();
        Files.writeString(emptyFile.toPath(), "");
        DataSet emptyDataSet = CSVUtil.loadCSV(emptyFile);
        assertTrue(emptyDataSet.isEmpty());

        // Test file with only headers
        File headerOnlyFile = tempDir.resolve("headerOnly.csv").toFile();
        Files.writeString(headerOnlyFile.toPath(), "col1,col2,col3\n");
        DataSet headerOnlyDataSet = CSVUtil.loadCSV(headerOnlyFile);
        assertEquals(3, headerOnlyDataSet.columnCount());
        assertEquals(0, headerOnlyDataSet.size());

        // Test invalid column names
        assertThrows(IllegalArgumentException.class, () -> {
            CSVUtil.loadCSV(testCsvFile, List.of("invalid_column"));
        });

        // Test negative offset/count
        assertThrows(IllegalArgumentException.class, () -> {
            CSVUtil.loadCSV(testCsvFile, null, -1, 10);
        });

        assertThrows(IllegalArgumentException.class, () -> {
            CSVUtil.loadCSV(testCsvFile, null, 0, -1);
        });

        // Test null bean class
        assertThrows(IllegalArgumentException.class, () -> {
            CSVUtil.loadCSV(testCsvFile, (Class<?>) null);
        });

        // Test empty column type map
        assertThrows(IllegalArgumentException.class, () -> {
            CSVUtil.loadCSV(testCsvFile, new HashMap<String, Type<?>>());
        });
    }

    @Test
    @DisplayName("Test CSV with special characters")
    public void testCSVWithSpecialCharacters() throws IOException {
        String csvWithQuotes = "name,description\n" + "\"John Doe\",\"Says \"\"Hello\"\" to everyone\"\n" + "\"Jane, Smith\",\"Has, commas, in, text\"\n";

        File specialFile = tempDir.resolve("special.csv").toFile();
        Files.writeString(specialFile.toPath(), csvWithQuotes);

        DataSet dataSet = CSVUtil.loadCSV(specialFile);

        assertEquals(2, dataSet.columnCount());
        assertEquals(2, dataSet.size());
        assertEquals("John Doe", dataSet.get(0, 0));
        assertEquals("Says \"Hello\" to everyone", dataSet.get(0, 1));
        assertEquals("Jane, Smith", dataSet.get(1, 0));
    }

    @Test
    @DisplayName("Test custom CSV parsers")
    public void testCustomParsers() throws IOException {
        // Create semicolon-delimited CSV
        String semicolonCsv = "id;name;age\n1;John;25\n2;Jane;30\n";
        File semicolonFile = tempDir.resolve("semicolon.csv").toFile();
        Files.writeString(semicolonFile.toPath(), semicolonCsv);

        // Set custom parsers
        CSVUtil.setCSVHeaderParser(line -> line.split(";"));
        CSVUtil.setCSVLineParser((line, output) -> {
            String[] parts = line.split(";");
            System.arraycopy(parts, 0, output, 0, Math.min(parts.length, output.length));
        });

        DataSet dataSet = CSVUtil.loadCSV(semicolonFile);

        assertEquals(3, dataSet.columnCount());
        assertEquals(2, dataSet.size());
        assertEquals("John", dataSet.get(0, 1));

        // Reset parsers
        CSVUtil.resetCSVHeaderParser();
        CSVUtil.resetCSVLineParser();
    }

    @Test
    @DisplayName("Test csv2json")
    public void test_csv2json() {
        N.println(IOUtil.readAllToString(testCsvFile));

        {
            DataSet dataSet = CSVUtil.loadCSV(testCsvFile, TestBean.class);
            File jsonFile = tempDir.resolve("test.json").toFile();
            CSVUtil.csv2json(testCsvFile, jsonFile);
            assertTrue(jsonFile.exists());
            assertTrue(jsonFile.length() > 0);

            List<TestBean> records = N.fromJson(jsonFile, Type.ofList(TestBean.class));
            assertEquals(5, records.size());
            assertEquals(dataSet.toList(TestBean.class), records);

            N.println(IOUtil.readAllToString(jsonFile));
        }
        {
            DataSet dataSet = CSVUtil.loadCSV(testCsvFile, TestBean.class);
            File jsonFile = tempDir.resolve("test.json").toFile();
            CSVUtil.csv2json(testCsvFile, null, jsonFile, TestBean.class);
            assertTrue(jsonFile.exists());
            assertTrue(jsonFile.length() > 0);

            List<TestBean> records = N.fromJson(jsonFile, Type.ofList(TestBean.class));
            assertEquals(5, records.size());
            assertEquals(dataSet.toList(TestBean.class), records);

            N.println(IOUtil.readAllToString(jsonFile));
        }
    }

    // Test bean classes
    @Data
    public static class TestBean {
        public Integer id;
        public String name;
        public Integer age;
        public Boolean active;

        public TestBean() {
        }
    }

    @Data
    public static class Person {
        public String name;
        public int age;

        public Person() {
        }
    }
}

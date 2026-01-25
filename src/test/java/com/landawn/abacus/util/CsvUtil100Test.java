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
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.type.Type;
import com.landawn.abacus.util.function.TriConsumer;

import lombok.Data;

@Tag("new-test")
public class CsvUtil100Test extends TestBase {

    @TempDir
    Path tempDir;

    private File testCsvFile;
    private String testCsvContent;

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
    @DisplayName("Test setCSVHeaderParser and getCurrentHeaderParser")
    public void testSetAndGetCSVHeaderParser() {
        Function<String, String[]> customParser = line -> line.split(";");

        CsvUtil.setHeaderParser(customParser);
        assertSame(customParser, CsvUtil.getCurrentHeaderParser());

        assertThrows(IllegalArgumentException.class, () -> CsvUtil.setHeaderParser(null));
    }

    @Test
    @DisplayName("Test setCSVLineParser and getCurrentLineParser")
    public void testSetAndGetCSVLineParser() {
        BiConsumer<String, String[]> customParser = (line, output) -> {
            String[] parts = line.split(";");
            System.arraycopy(parts, 0, output, 0, Math.min(parts.length, output.length));
        };

        CsvUtil.setLineParser(customParser);
        assertSame(customParser, CsvUtil.getCurrentLineParser());

        assertThrows(IllegalArgumentException.class, () -> CsvUtil.setLineParser(null));
    }

    @Test
    @DisplayName("Test resetCSVHeaderParser")
    public void testResetCSVHeaderParser() {
        Function<String, String[]> defaultParser = CsvUtil.getCurrentHeaderParser();
        Function<String, String[]> customParser = line -> line.split(";");

        CsvUtil.setHeaderParser(customParser);
        CsvUtil.resetHeaderParser();

        assertEquals(defaultParser, CsvUtil.getCurrentHeaderParser());
    }

    @Test
    @DisplayName("Test resetCSVLineParser")
    public void testResetCSVLineParser() {
        BiConsumer<String, String[]> defaultParser = CsvUtil.getCurrentLineParser();
        BiConsumer<String, String[]> customParser = (line, output) -> {
        };

        CsvUtil.setLineParser(customParser);
        CsvUtil.resetLineParser();

        assertEquals(defaultParser, CsvUtil.getCurrentLineParser());
    }

    @Test
    @DisplayName("Test load(File) method")
    public void testLoadCSVFromFile() {
        Dataset dataset = CsvUtil.load(testCsvFile);

        assertNotNull(dataset);
        assertEquals(4, dataset.columnCount());
        assertEquals(5, dataset.size());
        assertEquals(List.of("id", "name", "age", "active"), dataset.columnNames());
    }

    @Test
    @DisplayName("Test load(File, Collection<String>) method")
    public void testLoadCSVFromFileWithSelectColumns() {
        List<String> selectColumns = List.of("name", "age");
        Dataset dataset = CsvUtil.load(testCsvFile, selectColumns);

        assertNotNull(dataset);
        assertEquals(2, dataset.columnCount());
        assertEquals(5, dataset.size());
        assertEquals(selectColumns, dataset.columnNames());
    }

    @Test
    @DisplayName("Test load(File, Collection<String>, long, long) method")
    public void testLoadCSVFromFileWithOffsetAndCount() {
        List<String> selectColumns = List.of("name", "age");
        Dataset dataset = CsvUtil.load(testCsvFile, selectColumns, 1, 2);

        assertNotNull(dataset);
        assertEquals(2, dataset.columnCount());
        assertEquals(2, dataset.size());
        assertEquals("Jane", dataset.get(0, 0));
        assertEquals("Bob", dataset.get(1, 0));
    }

    @Test
    @DisplayName("Test load(File, Collection<String>, long, long, Predicate) method")
    public void testLoadCSVFromFileWithFilter() {
        Predicate<String[]> rowFilter = row -> "true".equals(row[3]);
        Dataset dataset = CsvUtil.load(testCsvFile, null, 0, Long.MAX_VALUE, rowFilter);

        assertNotNull(dataset);
        assertEquals(3, dataset.size());
    }

    @Test
    @DisplayName("Test load(Reader) method")
    public void testLoadCSVFromReader() throws IOException {
        try (Reader reader = new StringReader(testCsvContent)) {
            Dataset dataset = CsvUtil.load(reader);

            assertNotNull(dataset);
            assertEquals(4, dataset.columnCount());
            assertEquals(5, dataset.size());
        }
    }

    @Test
    @DisplayName("Test load(Reader, Collection<String>) method")
    public void testLoadCSVFromReaderWithSelectColumns() throws IOException {
        List<String> selectColumns = List.of("id", "name");

        try (Reader reader = new StringReader(testCsvContent)) {
            Dataset dataset = CsvUtil.load(reader, selectColumns);

            assertNotNull(dataset);
            assertEquals(2, dataset.columnCount());
            assertEquals(selectColumns, dataset.columnNames());
        }
    }

    @Test
    @DisplayName("Test load(Reader, Collection<String>, long, long) method")
    public void testLoadCSVFromReaderWithOffsetAndCount() throws IOException {
        try (Reader reader = new StringReader(testCsvContent)) {
            Dataset dataset = CsvUtil.load(reader, null, 2, 2);

            assertNotNull(dataset);
            assertEquals(2, dataset.size());
            assertEquals("Bob", dataset.get(0, 1));
            assertEquals("Alice", dataset.get(1, 1));
        }
    }

    @Test
    @DisplayName("Test load(Reader, Collection<String>, long, long, Predicate) method")
    public void testLoadCSVFromReaderWithFilter() throws IOException {
        Predicate<String[]> rowFilter = row -> Integer.parseInt(row[2]) > 30;

        try (Reader reader = new StringReader(testCsvContent)) {
            Dataset dataset = CsvUtil.load(reader, null, 0, Long.MAX_VALUE, rowFilter);

            assertNotNull(dataset);
            assertEquals(2, dataset.size());
        }
    }

    @Test
    @DisplayName("Test load with beanClassForColumnType")
    public void testLoadCSVWithBeanClass() {
        Dataset dataset = CsvUtil.load(testCsvFile, TestBean.class);

        assertNotNull(dataset);
        assertEquals(4, dataset.columnCount());

        assertEquals(1, (Integer) dataset.get(0, 0));
        assertEquals("John", dataset.get(0, 1));
        assertEquals(25, (Integer) dataset.get(0, 2));
        assertEquals(true, dataset.get(0, 3));
    }

    @Test
    @DisplayName("Test load with columnTypeMap")
    public void testLoadCSVWithColumnTypeMap() {
        Map<String, Type<?>> columnTypeMap = new HashMap<>();
        columnTypeMap.put("id", Type.of(Integer.class));
        columnTypeMap.put("age", Type.of(Integer.class));
        columnTypeMap.put("active", Type.of(Boolean.class));

        Dataset dataset = CsvUtil.load(testCsvFile, columnTypeMap);

        assertNotNull(dataset);
        assertEquals(4, dataset.columnCount());

        assertTrue(dataset.get(0, 0) instanceof Integer);
        assertTrue(dataset.get(0, 2) instanceof Integer);
        assertTrue(dataset.get(0, 3) instanceof Boolean);
    }

    @Test
    @DisplayName("Test load with rowExtractor")
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

        Dataset dataset = CsvUtil.load(testCsvFile, rowExtractor);

        assertNotNull(dataset);
        assertEquals(4, dataset.columnCount());

        assertTrue(dataset.get(0, 0) instanceof Integer);
        assertTrue(dataset.get(0, 1) instanceof String);
        assertTrue(dataset.get(0, 2) instanceof Integer);
        assertTrue(dataset.get(0, 3) instanceof Boolean);
    }

    @Test
    @DisplayName("Test stream(File, Class) method")
    public void testStreamFromFileWithClass() {
        List<TestBean> beans = CsvUtil.stream(testCsvFile, TestBean.class).toList();

        assertEquals(5, beans.size());
        assertEquals("John", beans.get(0).name);
        assertEquals(25, beans.get(0).age);
        assertTrue(beans.get(0).active);
    }

    @Test
    @DisplayName("Test stream with selectColumnNames")
    public void testStreamWithSelectColumnNames() {
        List<Map<String, String>> maps = CsvUtil.stream(testCsvFile, List.of("name", "age"), Clazz.ofMap(String.class, String.class)).toList();

        assertEquals(5, maps.size());
        assertEquals(2, maps.get(0).size());
        assertTrue(maps.get(0).containsKey("name"));
        assertTrue(maps.get(0).containsKey("age"));
        assertFalse(maps.get(0).containsKey("id"));
    }

    @Test
    @DisplayName("Test stream with offset and count")
    public void testStreamWithOffsetAndCount() {
        List<TestBean> beans = CsvUtil.stream(testCsvFile, null, 1, 3, null, TestBean.class).toList();

        assertEquals(3, beans.size());
        assertEquals("Jane", beans.get(0).name);
        assertEquals("Alice", beans.get(2).name);
    }

    @Test
    @DisplayName("Test stream with rowFilter")
    public void testStreamWithRowFilter() {
        Predicate<String[]> rowFilter = row -> Integer.parseInt(row[2]) < 30;

        List<TestBean> beans = CsvUtil.stream(testCsvFile, null, 0, Long.MAX_VALUE, rowFilter, TestBean.class).toList();

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

        List<Person> persons = CsvUtil.stream(testCsvFile, rowMapper).toList();

        assertEquals(5, persons.size());
        assertEquals("John", persons.get(0).name);
        assertEquals(25, persons.get(0).age);
    }

    @Test
    @DisplayName("Test stream from Reader")
    public void testStreamFromReader() throws IOException {
        try (Reader reader = new StringReader(testCsvContent)) {
            List<TestBean> beans = CsvUtil.stream(reader, TestBean.class, true).toList();

            assertEquals(5, beans.size());
            assertEquals("John", beans.get(0).name);
        }
    }

    @Test
    @DisplayName("Test stream with array target type")
    public void testStreamWithArrayType() {
        List<Object[]> arrays = CsvUtil.stream(testCsvFile, Object[].class).toList();

        assertEquals(5, arrays.size());
        assertEquals(4, arrays.get(0).length);
        assertEquals("1", arrays.get(0)[0]);
        assertEquals("John", arrays.get(0)[1]);
    }

    @Test
    @DisplayName("Test stream with Collection target type")
    public void testStreamWithCollectionType() {
        List<List<String>> lists = CsvUtil.stream(testCsvFile, Clazz.ofList(String.class)).toList();

        assertEquals(5, lists.size());
        assertEquals(4, lists.get(0).size());
        assertEquals("1", lists.get(0).get(0));
        assertEquals("John", lists.get(0).get(1));
    }

    @Test
    @DisplayName("Test edge cases and error conditions")
    public void testEdgeCases() throws IOException {
        File emptyFile = tempDir.resolve("empty.csv").toFile();
        Files.writeString(emptyFile.toPath(), "");
        Dataset emptyDataset = CsvUtil.load(emptyFile);
        assertTrue(emptyDataset.isEmpty());

        File headerOnlyFile = tempDir.resolve("headerOnly.csv").toFile();
        Files.writeString(headerOnlyFile.toPath(), "col1,col2,col3\n");
        Dataset headerOnlyDataset = CsvUtil.load(headerOnlyFile);
        assertEquals(3, headerOnlyDataset.columnCount());
        assertEquals(0, headerOnlyDataset.size());

        assertThrows(IllegalArgumentException.class, () -> {
            CsvUtil.load(testCsvFile, List.of("invalid_column"));
        });

        assertThrows(IllegalArgumentException.class, () -> {
            CsvUtil.load(testCsvFile, null, -1, 10);
        });

        assertThrows(IllegalArgumentException.class, () -> {
            CsvUtil.load(testCsvFile, null, 0, -1);
        });

        assertThrows(IllegalArgumentException.class, () -> {
            CsvUtil.load(testCsvFile, (Class<?>) null);
        });

        assertThrows(IllegalArgumentException.class, () -> {
            CsvUtil.load(testCsvFile, new HashMap<>());
        });
    }

    @Test
    @DisplayName("Test CSV with special characters")
    public void testCSVWithSpecialCharacters() throws IOException {
        String csvWithQuotes = "name,description\n" + "\"John Doe\",\"Says \"\"Hello\"\" to everyone\"\n" + "\"Jane, Smith\",\"Has, commas, in, text\"\n";

        File specialFile = tempDir.resolve("special.csv").toFile();
        Files.writeString(specialFile.toPath(), csvWithQuotes);

        Dataset dataset = CsvUtil.load(specialFile);

        assertEquals(2, dataset.columnCount());
        assertEquals(2, dataset.size());
        assertEquals("John Doe", dataset.get(0, 0));
        assertEquals("Says \"Hello\" to everyone", dataset.get(0, 1));
        assertEquals("Jane, Smith", dataset.get(1, 0));
    }

    @Test
    @DisplayName("Test custom CSV parsers")
    public void testCustomParsers() throws IOException {
        String semicolonCsv = "id;name;age\n1;John;25\n2;Jane;30\n";
        File semicolonFile = tempDir.resolve("semicolon.csv").toFile();
        Files.writeString(semicolonFile.toPath(), semicolonCsv);

        CsvUtil.setHeaderParser(line -> line.split(";"));
        CsvUtil.setLineParser((line, output) -> {
            String[] parts = line.split(";");
            System.arraycopy(parts, 0, output, 0, Math.min(parts.length, output.length));
        });

        Dataset dataset = CsvUtil.load(semicolonFile);

        assertEquals(3, dataset.columnCount());
        assertEquals(2, dataset.size());
        assertEquals("John", dataset.get(0, 1));

        CsvUtil.resetHeaderParser();
        CsvUtil.resetLineParser();
    }

    @Test
    @DisplayName("Test csvToJson")
    public void test_csvToJson() {
        N.println(IOUtil.readAllToString(testCsvFile));

        {
            Dataset dataset = CsvUtil.load(testCsvFile, TestBean.class);
            File jsonFile = tempDir.resolve("test.json").toFile();
            CsvUtil.csvToJson(testCsvFile, jsonFile);
            assertTrue(jsonFile.exists());
            assertTrue(jsonFile.length() > 0);

            List<TestBean> records = N.fromJson(jsonFile, Type.ofList(TestBean.class));
            assertEquals(5, records.size());
            assertEquals(dataset.toList(TestBean.class), records);

            N.println(IOUtil.readAllToString(jsonFile));
        }
        {
            Dataset dataset = CsvUtil.load(testCsvFile, TestBean.class);
            File jsonFile = tempDir.resolve("test.json").toFile();
            CsvUtil.csvToJson(testCsvFile, null, jsonFile, TestBean.class);
            assertTrue(jsonFile.exists());
            assertTrue(jsonFile.length() > 0);

            List<TestBean> records = N.fromJson(jsonFile, Type.ofList(TestBean.class));
            assertEquals(5, records.size());
            assertEquals(dataset.toList(TestBean.class), records);

            N.println(IOUtil.readAllToString(jsonFile));
        }
    }

    @Test
    @DisplayName("Test jsonToCsv")
    public void test_jsonToCsv() {
        List<TestBean> testBeans = Beans.fill(TestBean.class, 999999);
        File jsonFile = tempDir.resolve("test001.json").toFile();

        N.toJson(testBeans, jsonFile);
        List<TestBean> beans2 = N.fromJson(jsonFile, Type.ofList(TestBean.class));
        assertEquals(testBeans, beans2);

        N.println("============================================================");

        beans2 = N.streamJson(jsonFile, Type.ofMap(String.class, Object.class)).map(it -> Beans.mapToBean(it, TestBean.class)).toList();
        assertEquals(testBeans, beans2);

        N.println("============================================================");

        {

            File testCsvFile = tempDir.resolve("test001.csv").toFile();
            CsvUtil.jsonToCsv(jsonFile, testCsvFile);
            assertTrue(jsonFile.exists());
            assertTrue(jsonFile.length() > 0);

            N.println(IOUtil.readAllToString(testCsvFile));
        }
    }

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

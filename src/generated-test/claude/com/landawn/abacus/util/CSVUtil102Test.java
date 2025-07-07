package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Predicate;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.type.Type;
import com.landawn.abacus.util.function.TriConsumer;
import com.landawn.abacus.util.stream.Stream;

public class CSVUtil102Test extends TestBase {

    private File tempFile;

    @BeforeEach
    public void setUp() throws IOException {
        tempFile = File.createTempFile("test", ".csv");
        tempFile.deleteOnExit();
    }

    @AfterEach
    public void tearDown() {
        CSVUtil.resetCSVHeaderParser();
        CSVUtil.resetCSVLineParser();
        CSVUtil.resetEscapeCharForWrite();
        if (tempFile != null && tempFile.exists()) {
            tempFile.delete();
        }
    }

    @Test
    public void testSetAndGetCSVHeaderParser() {
        Function<String, String[]> customParser = line -> line.split("\\|");
        CSVUtil.setCSVHeaderParser(customParser);
        assertEquals(customParser, CSVUtil.getCurrentHeaderParser());
    }

    @Test
    public void testSetAndGetCSVLineParser() {
        BiConsumer<String, String[]> customParser = (line, output) -> {
            String[] parts = line.split("\\|");
            System.arraycopy(parts, 0, output, 0, Math.min(parts.length, output.length));
        };
        CSVUtil.setCSVLineParser(customParser);
        assertEquals(customParser, CSVUtil.getCurrentLineParser());
    }

    @Test
    public void testResetCSVHeaderParser() {
        Function<String, String[]> customParser = line -> line.split("\\|");
        CSVUtil.setCSVHeaderParser(customParser);
        CSVUtil.resetCSVHeaderParser();
        assertNotEquals(customParser, CSVUtil.getCurrentHeaderParser());
    }

    @Test
    public void testResetCSVLineParser() {
        BiConsumer<String, String[]> customParser = (line, output) -> {};
        CSVUtil.setCSVLineParser(customParser);
        CSVUtil.resetCSVLineParser();
        assertNotEquals(customParser, CSVUtil.getCurrentLineParser());
    }

    @Test
    public void testSetAndResetEscapeCharForWrite() {
        assertFalse(CSVUtil.isBackSlashEscapeCharForWrite());
        CSVUtil.setEscapeCharToBackSlashForWrite();
        assertTrue(CSVUtil.isBackSlashEscapeCharForWrite());
        CSVUtil.resetEscapeCharForWrite();
        assertFalse(CSVUtil.isBackSlashEscapeCharForWrite());
    }

    @Test
    public void testLoadCSVFromFile() throws IOException {
        try (FileWriter fw = new FileWriter(tempFile)) {
            fw.write("Name,Age,City\n");
            fw.write("John,30,NYC\n");
            fw.write("Jane,25,LA\n");
        }

        DataSet ds = CSVUtil.loadCSV(tempFile);
        assertEquals(3, ds.columnNameList().size());
        assertEquals(2, ds.size());
        assertEquals("John", ds.get(0, 0));
        assertEquals("30", ds.get(0, 1));
        assertEquals("NYC", ds.get(0, 2));
    }

    @Test
    public void testLoadCSVFromFileWithSelectedColumns() throws IOException {
        try (FileWriter fw = new FileWriter(tempFile)) {
            fw.write("Name,Age,City\n");
            fw.write("John,30,NYC\n");
            fw.write("Jane,25,LA\n");
        }

        List<String> selectedColumns = Arrays.asList("Name", "City");
        DataSet ds = CSVUtil.loadCSV(tempFile, selectedColumns);
        assertEquals(2, ds.columnNameList().size());
        assertEquals(2, ds.size());
        assertEquals("John", ds.get(0, 0));
        assertEquals("NYC", ds.get(0, 1));
    }

    @Test
    public void testLoadCSVFromFileWithOffsetAndCount() throws IOException {
        try (FileWriter fw = new FileWriter(tempFile)) {
            fw.write("Name,Age,City\n");
            fw.write("John,30,NYC\n");
            fw.write("Jane,25,LA\n");
            fw.write("Bob,35,SF\n");
            fw.write("Alice,28,Boston\n");
        }

        DataSet ds = CSVUtil.loadCSV(tempFile, null, 1, 2);
        assertEquals(3, ds.columnNameList().size());
        assertEquals(2, ds.size());
        assertEquals("Jane", ds.get(0, 0));
        assertEquals("Bob", ds.get(1, 0));
    }

    @Test
    public void testLoadCSVFromFileWithRowFilter() throws IOException {
        try (FileWriter fw = new FileWriter(tempFile)) {
            fw.write("Name,Age,City\n");
            fw.write("John,30,NYC\n");
            fw.write("Jane,25,LA\n");
            fw.write("Bob,35,SF\n");
        }

        Predicate<String[]> rowFilter = row -> Integer.parseInt(row[1]) > 28;
        DataSet ds = CSVUtil.loadCSV(tempFile, null, 0, Long.MAX_VALUE, rowFilter);
        assertEquals(2, ds.size());
        assertEquals("John", ds.get(0, 0));
        assertEquals("Bob", ds.get(1, 0));
    }

    @Test
    public void testLoadCSVFromReader() throws IOException {
        String csvContent = "Name,Age,City\nJohn,30,NYC\nJane,25,LA\n";
        Reader reader = new StringReader(csvContent);
        
        DataSet ds = CSVUtil.loadCSV(reader);
        assertEquals(3, ds.columnNameList().size());
        assertEquals(2, ds.size());
        assertEquals("John", ds.get(0, 0));
    }

    @Test
    public void testLoadCSVFromReaderEmpty() throws IOException {
        Reader reader = new StringReader("");
        DataSet ds = CSVUtil.loadCSV(reader);
        assertTrue(ds.isEmpty());
    }

    @Test
    public void testLoadCSVWithQuotedFields() throws IOException {
        try (FileWriter fw = new FileWriter(tempFile)) {
            fw.write("Name,Address,Age\n");
            fw.write("John,\"123 Main St, Apt 4\",30\n");
            fw.write("Jane,\"456 Oak Ave\",25\n");
        }

        DataSet ds = CSVUtil.loadCSV(tempFile);
        assertEquals("123 Main St, Apt 4", ds.get(0, 1));
        assertEquals("456 Oak Ave", ds.get(1, 1));
    }

    @Test
    public void testCSVHeaderParserByDefault() {
        String[] result = CSVUtil.CSV_HEADER_PARSER.apply("Name,Age,\"Address, City\"");
        assertArrayEquals(new String[]{"Name", "Age", "Address, City"}, result);
    }

    @Test
    public void testCSVHeaderParserBySplitter() {
        String[] result = CSVUtil.CSV_HEADER_PARSER_BY_SPLITTER.apply("\"Name\",\"Age\",\"City\"");
        assertArrayEquals(new String[]{"Name", "Age", "City"}, result);
    }

    @Test
    public void testCSVHeaderParserInJSON() {
        String[] result = CSVUtil.CSV_HEADER_PARSER_IN_JSON.apply("[\"Name\",\"Age\",\"City\"]");
        assertArrayEquals(new String[]{"Name", "Age", "City"}, result);
    }

    @Test
    public void testCSVLineParserByDefault() {
        String[] output = new String[3];
        CSVUtil.CSV_LINE_PARSER.accept("John,30,\"New York, NY\"", output);
        assertArrayEquals(new String[]{"John", "30", "New York, NY"}, output);
    }

    @Test
    public void testCSVLineParserBySplitter() {
        String[] output = new String[3];
        CSVUtil.CSV_LINE_PARSER_BY_SPLITTER.accept("\"John\",\"30\",\"NYC\"", output);
        assertArrayEquals(new String[]{"John", "30", "NYC"}, output);
    }

    @Test
    public void testCSVLineParserInJSON() {
        String[] output = new String[3];
        CSVUtil.CSV_LINE_PARSER_IN_JSON.accept("[\"John\",\"30\",\"NYC\"]", output);
        assertArrayEquals(new String[]{"John", "30", "NYC"}, output);
    }

    // Test with bean class
    public static class Person {
        private String name;
        private int age;
        private String city;

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        
        public int getAge() { return age; }
        public void setAge(int age) { this.age = age; }
        
        public String getCity() { return city; }
        public void setCity(String city) { this.city = city; }
    }

    @Test
    public void testLoadCSVWithBeanClass() throws IOException {
        try (FileWriter fw = new FileWriter(tempFile)) {
            fw.write("name,age,city\n");
            fw.write("John,30,NYC\n");
            fw.write("Jane,25,LA\n");
        }

        DataSet ds = CSVUtil.loadCSV(tempFile, Person.class);
        assertEquals(3, ds.columnNameList().size());
        assertEquals(2, ds.size());
        assertEquals("John", ds.get(0, 0));
        assertEquals(30, (Integer) ds.get(0, 1));
        assertEquals("NYC", ds.get(0, 2));
    }

    @Test
    public void testLoadCSVWithColumnTypeMap() throws IOException {
        try (FileWriter fw = new FileWriter(tempFile)) {
            fw.write("name,age,salary\n");
            fw.write("John,30,50000.50\n");
            fw.write("Jane,25,45000.75\n");
        }

        Map<String, Type<?>> columnTypeMap = new HashMap<>();
        columnTypeMap.put("name", N.typeOf(String.class));
        columnTypeMap.put("age", N.typeOf(Integer.class));
        columnTypeMap.put("salary", N.typeOf(Double.class));
        
        DataSet ds = CSVUtil.loadCSV(tempFile, columnTypeMap);
        assertEquals("John", ds.get(0, 0));
        assertEquals(30, (Integer) ds.get(0, 1));
        assertEquals(50000.50, ds.get(0, 2));
    }

    @Test
    public void testLoadCSVWithRowExtractor() throws IOException {
        try (FileWriter fw = new FileWriter(tempFile)) {
            fw.write("name,age,city\n");
            fw.write("John,30,NYC\n");
            fw.write("Jane,25,LA\n");
        }

        TriConsumer<List<String>, NoCachingNoUpdating.DisposableArray<String>, Object[]> rowExtractor = 
            (columns, row, output) -> {
                output[0] = row.get(0).toUpperCase();
                output[1] = Integer.parseInt(row.get(1)) * 2;
                output[2] = row.get(2).toLowerCase();
            };
        
        DataSet ds = CSVUtil.loadCSV(tempFile, rowExtractor);
        assertEquals("JOHN", ds.get(0, 0));
        assertEquals(60, (Integer) ds.get(0, 1));
        assertEquals("nyc", ds.get(0, 2));
    }

    @Test
    public void testStreamCSVWithTargetType() throws IOException {
        try (FileWriter fw = new FileWriter(tempFile)) {
            fw.write("name,age,city\n");
            fw.write("John,30,NYC\n");
            fw.write("Jane,25,LA\n");
        }

        try (Stream<Person> stream = CSVUtil.stream(tempFile, Person.class)) {
            List<Person> persons = stream.toList();
            assertEquals(2, persons.size());
            assertEquals("John", persons.get(0).getName());
            assertEquals(30, persons.get(0).getAge());
            assertEquals("NYC", persons.get(0).getCity());
        }
    }

    @Test
    public void testStreamCSVWithObjectArray() throws IOException {
        try (FileWriter fw = new FileWriter(tempFile)) {
            fw.write("name,age,city\n");
            fw.write("John,30,NYC\n");
            fw.write("Jane,25,LA\n");
        }

        try (Stream<Object[]> stream = CSVUtil.stream(tempFile, Object[].class)) {
            List<Object[]> rows = stream.toList();
            assertEquals(2, rows.size());
            assertEquals("John", rows.get(0)[0]);
            assertEquals("30", rows.get(0)[1]);
            assertEquals("NYC", rows.get(0)[2]);
        }
    }

    @Test
    public void testStreamCSVWithCollection() throws IOException {
        try (FileWriter fw = new FileWriter(tempFile)) {
            fw.write("name,age,city\n");
            fw.write("John,30,NYC\n");
            fw.write("Jane,25,LA\n");
        }

        try (Stream<List> stream = CSVUtil.stream(tempFile, List.class)) {
            List<List> rows = stream.toList();
            assertEquals(2, rows.size());
            assertEquals("John", rows.get(0).get(0));
            assertEquals("30", rows.get(0).get(1));
            assertEquals("NYC", rows.get(0).get(2));
        }
    }

    @Test
    public void testStreamCSVWithMap() throws IOException {
        try (FileWriter fw = new FileWriter(tempFile)) {
            fw.write("name,age,city\n");
            fw.write("John,30,NYC\n");
            fw.write("Jane,25,LA\n");
        }

        try (Stream<Map> stream = CSVUtil.stream(tempFile, Map.class)) {
            List<Map> rows = stream.toList();
            assertEquals(2, rows.size());
            assertEquals("John", rows.get(0).get("name"));
            assertEquals("30", rows.get(0).get("age"));
            assertEquals("NYC", rows.get(0).get("city"));
        }
    }

    @Test
    public void testWriteField() throws IOException {
        StringWriter sw = new StringWriter();
        BufferedCSVWriter writer = new BufferedCSVWriter(sw);
        
        CSVUtil.writeField(writer, N.typeOf(String.class), "Hello");
        CSVUtil.writeField(writer, N.typeOf(Integer.class), 42);
        CSVUtil.writeField(writer, null, null);
        
        writer.flush();
        String result = sw.toString();
        assertTrue(result.contains("Hello"));
        assertTrue(result.contains("42"));
        assertTrue(result.contains("null"));
    }

    @Test
    public void testStreamWithOffsetAndLimit() throws IOException {
        try (FileWriter fw = new FileWriter(tempFile)) {
            fw.write("num\n");
            for (int i = 0; i < 10; i++) {
                fw.write(i + "\n");
            }
        }

        try (Stream<String[]> stream = CSVUtil.stream(tempFile, null, 2, 3, null, String[].class)) {
            List<String[]> rows = stream.toList();
            assertEquals(3, rows.size());
            assertEquals("2", rows.get(0)[0]);
            assertEquals("3", rows.get(1)[0]);
            assertEquals("4", rows.get(2)[0]);
        }
    }

    @Test
    public void testStreamWithRowFilter() throws IOException {
        try (FileWriter fw = new FileWriter(tempFile)) {
            fw.write("name,age\n");
            fw.write("John,30\n");
            fw.write("Jane,25\n");
            fw.write("Bob,35\n");
        }

        Predicate<String[]> filter = row -> Integer.parseInt(row[1]) > 28;
        try (Stream<String[]> stream = CSVUtil.stream(tempFile, null, 0, Long.MAX_VALUE, filter, String[].class)) {
            List<String[]> rows = stream.toList();
            assertEquals(2, rows.size());
            assertEquals("John", rows.get(0)[0]);
            assertEquals("Bob", rows.get(1)[0]);
        }
    }
}
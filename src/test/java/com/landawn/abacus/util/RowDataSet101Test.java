package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.NoCachingNoUpdating.DisposableObjArray;
import com.landawn.abacus.util.function.BiFunction;
import com.landawn.abacus.util.function.BiPredicate;
import com.landawn.abacus.util.function.Function;
import com.landawn.abacus.util.function.IntFunction;
import com.landawn.abacus.util.function.Predicate;
import com.landawn.abacus.util.function.TriFunction;
import com.landawn.abacus.util.function.TriPredicate;

public class RowDataSet101Test extends TestBase {

    private DataSet dataSet;
    private List<String> columnNames;
    private List<List<Object>> columnValues;

    @BeforeEach
    public void setUp() {
        columnNames = N.asList("id", "name", "age", "city");
        columnValues = new ArrayList<>();

        columnValues.add(N.asList(1, 2, 3, 4, 5));
        columnValues.add(N.asList("John", "Jane", "Bob", "Alice", "Tom"));
        columnValues.add(N.asList(25, 30, 35, 28, 32));
        columnValues.add(N.asList("NYC", "LA", "NYC", "LA", "Chicago"));

        dataSet = new RowDataSet(columnNames, columnValues);
    }

    // XML Export Tests

    @Test
    public void testToXml() {
        String xml = dataSet.toXml();
        assertNotNull(xml);
        assertTrue(xml.contains("<dataSet>"));
        assertTrue(xml.contains("</dataSet>"));
        assertTrue(xml.contains("<row>"));
        assertTrue(xml.contains("<id>"));
        assertTrue(xml.contains("<name>"));
    }

    @Test
    public void testToXmlWithCustomRowElementName() {
        String xml = dataSet.toXml("record");
        assertNotNull(xml);
        assertTrue(xml.contains("<record>"));
        assertTrue(xml.contains("</record>"));
    }

    @Test
    public void testToXmlWithRowRange() {
        String xml = dataSet.toXml(1, 3);
        assertNotNull(xml);
        assertTrue(xml.contains("Jane"));
        assertTrue(xml.contains("Bob"));
        assertFalse(xml.contains("John"));
        assertFalse(xml.contains("Alice"));
    }

    @Test
    public void testToXmlWithRowRangeAndCustomElementName() {
        String xml = dataSet.toXml(1, 3, "person");
        assertNotNull(xml);
        assertTrue(xml.contains("<person>"));
        assertTrue(xml.contains("Jane"));
        assertTrue(xml.contains("Bob"));
    }

    @Test
    public void testToXmlWithRowRangeAndColumnNames() {
        Collection<String> columns = N.asList("name", "age");
        String xml = dataSet.toXml(1, 3, columns);
        assertNotNull(xml);
        assertTrue(xml.contains("Jane"));
        assertTrue(xml.contains("30"));
        assertFalse(xml.contains("<id>"));
        assertFalse(xml.contains("<city>"));
    }

    @Test
    public void testToXmlWithAllParameters() {
        Collection<String> columns = N.asList("name", "age");
        String xml = dataSet.toXml(1, 3, columns, "employee");
        assertNotNull(xml);
        assertTrue(xml.contains("<employee>"));
        assertTrue(xml.contains("Jane"));
        assertTrue(xml.contains("30"));
    }

    @Test
    public void testToXmlToFile(@TempDir Path tempDir) throws IOException {
        File outputFile = tempDir.resolve("test.xml").toFile();
        dataSet.toXml(outputFile);
        assertTrue(outputFile.exists());

        String content = IOUtil.readAllToString(outputFile);
        assertTrue(content.contains("<dataSet>"));
        assertTrue(content.contains("John"));
    }

    @Test
    public void testToXmlToFileWithCustomElementName(@TempDir Path tempDir) throws IOException {
        File outputFile = tempDir.resolve("test2.xml").toFile();
        dataSet.toXml("item", outputFile);
        assertTrue(outputFile.exists());

        String content = IOUtil.readAllToString(outputFile);
        assertTrue(content.contains("<item>"));
    }

    @Test
    public void testToXmlToFileWithRowRange(@TempDir Path tempDir) throws IOException {
        File outputFile = tempDir.resolve("test3.xml").toFile();
        dataSet.toXml(1, 3, outputFile);
        assertTrue(outputFile.exists());

        String content = IOUtil.readAllToString(outputFile);
        assertTrue(content.contains("Jane"));
        assertFalse(content.contains("John"));
    }

    @Test
    public void testToXmlToFileWithAllParams(@TempDir Path tempDir) throws IOException {
        File outputFile = tempDir.resolve("test4.xml").toFile();
        Collection<String> columns = N.asList("name", "age");
        dataSet.toXml(0, 2, columns, "record", outputFile);
        assertTrue(outputFile.exists());

        String content = IOUtil.readAllToString(outputFile);
        assertTrue(content.contains("<record>"));
        assertTrue(content.contains("John"));
        assertFalse(content.contains("<city>"));
    }

    @Test
    public void testToXmlToOutputStream() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        dataSet.toXml(baos);

        String xml = baos.toString();
        assertTrue(xml.contains("<dataSet>"));
        assertTrue(xml.contains("John"));
    }

    @Test
    public void testToXmlToOutputStreamWithElementName() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        dataSet.toXml("data", baos);

        String xml = baos.toString();
        assertTrue(xml.contains("<data>"));
    }

    @Test
    public void testToXmlToOutputStreamWithRowRange() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        dataSet.toXml(1, 3, baos);

        String xml = baos.toString();
        assertTrue(xml.contains("Jane"));
        assertFalse(xml.contains("John"));
    }

    @Test
    public void testToXmlToOutputStreamWithAllParams() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Collection<String> columns = N.asList("name");
        dataSet.toXml(0, 2, columns, "person", baos);

        String xml = baos.toString();
        assertTrue(xml.contains("<person>"));
        assertTrue(xml.contains("John"));
        assertFalse(xml.contains("<age>"));
    }

    @Test
    public void testToXmlToWriter() throws IOException {
        StringWriter writer = new StringWriter();
        dataSet.toXml(writer);

        String xml = writer.toString();
        assertTrue(xml.contains("<dataSet>"));
        assertTrue(xml.contains("John"));
    }

    @Test
    public void testToXmlToWriterWithElementName() throws IOException {
        StringWriter writer = new StringWriter();
        dataSet.toXml("entry", writer);

        String xml = writer.toString();
        assertTrue(xml.contains("<entry>"));
    }

    @Test
    public void testToXmlToWriterWithRowRange() throws IOException {
        StringWriter writer = new StringWriter();
        dataSet.toXml(2, 4, writer);

        String xml = writer.toString();
        assertTrue(xml.contains("Bob"));
        assertTrue(xml.contains("Alice"));
        assertFalse(xml.contains("John"));
    }

    @Test
    public void testToXmlToWriterWithAllParams() throws IOException {
        StringWriter writer = new StringWriter();
        Collection<String> columns = N.asList("id", "name");
        dataSet.toXml(0, 2, columns, "user", writer);

        String xml = writer.toString();
        assertTrue(xml.contains("<user>"));
        assertTrue(xml.contains("<id>1</id>"));
        assertTrue(xml.contains("John"));
        assertFalse(xml.contains("<age>"));
    }

    // CSV Export Tests

    @Test
    public void testToCsv() {
        String csv = dataSet.toCsv();
        assertNotNull(csv);
        assertTrue(csv.contains("\"id\",\"name\",\"age\",\"city\""));
        assertTrue(csv.contains("1,\"John\",25,\"NYC\""));
    }

    @Test
    public void testToCsvWithRowRangeAndColumns() {
        Collection<String> columns = N.asList("name", "age");
        String csv = dataSet.toCsv(1, 3, columns);
        assertNotNull(csv);
        assertTrue(csv.contains("\"name\",\"age\""));
        assertTrue(csv.contains("\"Jane\",30"));
        assertTrue(csv.contains("\"Bob\",35"));
        assertFalse(csv.contains("John"));
    }

    @Test
    public void testToCsvToFile(@TempDir Path tempDir) throws IOException {
        File outputFile = tempDir.resolve("test.csv").toFile();
        dataSet.toCsv(outputFile);
        assertTrue(outputFile.exists());

        String content = IOUtil.readAllToString(outputFile);
        assertTrue(content.contains("\"id\",\"name\",\"age\",\"city\""));
        assertTrue(content.contains("\"John\""));
    }

    @Test
    public void testToCsvToFileWithParams(@TempDir Path tempDir) throws IOException {
        File outputFile = tempDir.resolve("test2.csv").toFile();
        Collection<String> columns = N.asList("name", "city");
        dataSet.toCsv(1, 3, columns, outputFile);
        assertTrue(outputFile.exists());

        String content = IOUtil.readAllToString(outputFile);
        assertTrue(content.contains("\"name\",\"city\""));
        assertTrue(content.contains("\"Jane\",\"LA\""));
        assertFalse(content.contains("John"));
    }

    @Test
    public void testToCsvToOutputStream() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        dataSet.toCsv(baos);

        String csv = baos.toString();
        assertTrue(csv.contains("\"id\",\"name\",\"age\",\"city\""));
        assertTrue(csv.contains("John"));
    }

    @Test
    public void testToCsvToOutputStreamWithParams() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Collection<String> columns = N.asList("id", "name");
        dataSet.toCsv(0, 2, columns, baos);

        String csv = baos.toString();
        assertTrue(csv.contains("\"id\",\"name\""));
        assertTrue(csv.contains("1,\"John\""));
        assertFalse(csv.contains("age"));
    }

    @Test
    public void testToCsvToWriter() throws IOException {
        StringWriter writer = new StringWriter();
        dataSet.toCsv(writer);

        String csv = writer.toString();
        assertTrue(csv.contains("\"id\",\"name\",\"age\",\"city\""));
        assertTrue(csv.contains("\"John\""));
    }

    @Test
    public void testToCsvToWriterWithParams() throws IOException {
        StringWriter writer = new StringWriter();
        Collection<String> columns = N.asList("name");
        dataSet.toCsv(2, 4, columns, writer);

        String csv = writer.toString();
        assertTrue(csv.contains("name"));
        assertTrue(csv.contains("Bob"));
        assertTrue(csv.contains("Alice"));
        assertFalse(csv.contains("John"));
    }

    // GroupBy Tests

    @Test
    public void testGroupByWithSingleKeyAndCollector() {
        Collector<Object, ?, List<Object>> listCollector = Collectors.toList();
        DataSet grouped = dataSet.groupBy("city", "name", "names", listCollector);

        assertNotNull(grouped);
        assertEquals(3, grouped.size()); // NYC, LA, Chicago
        assertTrue(grouped.containsColumn("city"));
        assertTrue(grouped.containsColumn("names"));
    }

    @Test
    public void testGroupByWithSingleKeyAndRowType() {
        DataSet grouped = dataSet.groupBy("city", N.asList("name", "age"), "people", Object[].class);

        assertNotNull(grouped);
        assertEquals(3, grouped.size());
        assertTrue(grouped.containsColumn("city"));
        assertTrue(grouped.containsColumn("people"));
    }

    @Test
    public void testGroupByWithSingleKeyAndArrayCollector() {
        Collector<Object[], ?, List<Object[]>> collector = Collectors.toList();
        DataSet grouped = dataSet.groupBy("city", N.asList("name", "age"), "data", collector);

        assertNotNull(grouped);
        assertEquals(3, grouped.size());
        assertTrue(grouped.containsColumn("city"));
        assertTrue(grouped.containsColumn("data"));
    }

    @Test
    public void testGroupByWithSingleKeyAndRowMapper() {
        Function<DisposableObjArray, String> rowMapper = arr -> arr.get(0) + "-" + arr.get(1);
        Collector<String, ?, List<String>> collector = Collectors.toList();
        DataSet grouped = dataSet.groupBy("city", N.asList("name", "age"), "info", rowMapper, collector);

        assertNotNull(grouped);
        assertEquals(3, grouped.size());
        assertTrue(grouped.containsColumn("city"));
        assertTrue(grouped.containsColumn("info"));
    }

    @Test
    public void testGroupByWithKeyExtractor() {
        Function<Object, String> keyExtractor = obj -> obj.toString().toUpperCase();
        Collector<Object, ?, List<Object>> collector = Collectors.toList();
        DataSet grouped = dataSet.groupBy("city", keyExtractor, "name", "names", collector);

        assertNotNull(grouped);
        assertTrue(grouped.containsColumn("city"));
        assertTrue(grouped.containsColumn("names"));
    }

    @Test
    public void testGroupByWithMultipleKeys() {
        DataSet grouped = dataSet.groupBy(N.asList("city"));

        assertNotNull(grouped);
        assertEquals(3, grouped.size());
        assertTrue(grouped.containsColumn("city"));
    }

    @Test
    public void testGroupByWithMultipleKeysAndCollector() {
        Collector<Object, ?, Long> countCollector = Collectors.counting();
        DataSet grouped = dataSet.groupBy(N.asList("city"), "name", "count", countCollector);

        assertNotNull(grouped);
        assertEquals(3, grouped.size());
        assertTrue(grouped.containsColumn("city"));
        assertTrue(grouped.containsColumn("count"));
    }

    @Test
    public void testGroupByWithMultipleKeysAndRowType() {
        List<String> columnNames = N.asList("id", "name");
        List<List<Object>> columnValues = new ArrayList<>();
        columnValues.add(N.asList(1, 2, 3, 4));
        columnValues.add(N.asList("A", "B", "A", "B"));

        RowDataSet ds = new RowDataSet(columnNames, columnValues);
        DataSet grouped = ds.groupBy(N.asList("name"), N.asList("id"), "ids", List.class);

        assertNotNull(grouped);
        assertEquals(2, grouped.size());
        assertTrue(grouped.containsColumn("name"));
        assertTrue(grouped.containsColumn("ids"));
    }

    @Test
    public void testGroupByWithMultipleKeysAndArrayCollector() {
        Collector<Object[], ?, Long> countCollector = Collectors.counting();
        DataSet grouped = dataSet.groupBy(N.asList("city"), N.asList("name", "age"), "count", countCollector);

        assertNotNull(grouped);
        assertEquals(3, grouped.size());
        assertTrue(grouped.containsColumn("city"));
        assertTrue(grouped.containsColumn("count"));
    }

    @Test
    public void testGroupByWithMultipleKeysAndRowMapper() {
        Function<DisposableObjArray, Integer> rowMapper = arr -> (Integer) arr.get(0);
        Collector<Integer, ?, Integer> sumCollector = Collectors.summingInt(Integer::intValue);
        DataSet grouped = dataSet.groupBy(N.asList("city"), N.asList("age"), "totalAge", rowMapper, sumCollector);

        assertNotNull(grouped);
        assertEquals(3, grouped.size());
        assertTrue(grouped.containsColumn("city"));
        assertTrue(grouped.containsColumn("totalAge"));
    }

    @Test
    public void testGroupByWithMultipleKeysAndKeyExtractor() {
        Function<DisposableObjArray, String> keyExtractor = arr -> arr.get(0).toString();
        DataSet grouped = dataSet.groupBy(N.asList("city"), keyExtractor);

        assertNotNull(grouped);
        assertTrue(grouped.containsColumn("city"));
    }

    @Test
    public void testGroupByWithMultipleKeysKeyExtractorAndCollector() {
        Function<DisposableObjArray, String> keyExtractor = arr -> arr.get(0).toString();
        Collector<Object, ?, Long> countCollector = Collectors.counting();
        DataSet grouped = dataSet.groupBy(N.asList("city"), keyExtractor, "name", "count", countCollector);

        assertNotNull(grouped);
        assertTrue(grouped.containsColumn("city"));
        assertTrue(grouped.containsColumn("count"));
    }

    @Test
    public void testGroupByWithMultipleKeysKeyExtractorAndRowType() {
        Function<DisposableObjArray, String> keyExtractor = arr -> arr.get(0).toString();
        DataSet grouped = dataSet.groupBy(N.asList("city"), keyExtractor, N.asList("name"), "names", List.class);
        dataSet.println();

        grouped.println();

        assertNotNull(grouped);
        assertTrue(grouped.containsColumn("city"));
        assertTrue(grouped.containsColumn("names"));
    }

    @Test
    public void testGroupByWithMultipleKeysKeyExtractorAndArrayCollector() {
        Function<DisposableObjArray, String> keyExtractor = arr -> arr.get(0).toString();
        Collector<Object[], ?, Long> countCollector = Collectors.counting();
        DataSet grouped = dataSet.groupBy(N.asList("city"), keyExtractor, N.asList("name"), "count", countCollector);

        grouped.println();

        assertNotNull(grouped);
        assertTrue(grouped.containsColumn("city"));
        assertTrue(grouped.containsColumn("count"));
        assertEquals(3, grouped.size()); // NYC, LA, Chicago
    }

    @Test
    public void testGroupByWithMultipleKeysKeyExtractorAndRowMapper() {
        Function<DisposableObjArray, String> keyExtractor = arr -> arr.get(0).toString();
        Function<DisposableObjArray, String> rowMapper = arr -> arr.get(0).toString();
        Collector<String, ?, List<String>> collector = Collectors.toList();
        DataSet grouped = dataSet.groupBy(N.asList("city"), keyExtractor, N.asList("name"), "names", rowMapper, collector);

        assertNotNull(grouped);
        assertTrue(grouped.containsColumn("city"));
        assertTrue(grouped.containsColumn("names"));
    }

    // Pivot Tests

    @Test
    public void testPivotWithSingleAggregateColumn() {
        // Create a dataset suitable for pivoting
        List<String> columnNames = N.asList("row", "col", "value");
        List<List<Object>> columnValues = new ArrayList<>();
        columnValues.add(N.asList("A", "A", "B", "B"));
        columnValues.add(N.asList("X", "Y", "X", "Y"));
        columnValues.add(N.asList(1, 2, 3, 4));

        RowDataSet ds = new RowDataSet(columnNames, columnValues);
        Collector<Object, ?, Integer> sumCollector = Collectors.summingInt(o -> (Integer) o);

        Sheet<String, String, Integer> pivot = ds.pivot("row", "value", "col", sumCollector);

        assertNotNull(pivot);
        assertEquals(2, pivot.rowKeySet().size());
        assertEquals(2, pivot.columnKeySet().size());
    }

    @Test
    public void testPivotWithMultipleAggregateColumns() {
        List<String> columnNames = N.asList("row", "col", "val1", "val2");
        List<List<Object>> columnValues = new ArrayList<>();
        columnValues.add(N.asList("A", "A", "B", "B"));
        columnValues.add(N.asList("X", "Y", "X", "Y"));
        columnValues.add(N.asList(1, 2, 3, 4));
        columnValues.add(N.asList(5, 6, 7, 8));

        RowDataSet ds = new RowDataSet(columnNames, columnValues);
        Collector<Object[], ?, String> joiningCollector = Collectors.mapping(arr -> arr[0] + "-" + arr[1], Collectors.joining(","));

        Sheet<String, String, String> pivot = ds.pivot("row", N.asList("val1", "val2"), "col", joiningCollector);

        assertNotNull(pivot);
        assertEquals(2, pivot.rowKeySet().size());
        assertEquals(2, pivot.columnKeySet().size());
    }

    @Test
    public void testPivotWithRowMapper() {
        List<String> columnNames = N.asList("row", "col", "val1", "val2");
        List<List<Object>> columnValues = new ArrayList<>();
        columnValues.add(N.asList("A", "A", "B", "B"));
        columnValues.add(N.asList("X", "Y", "X", "Y"));
        columnValues.add(N.asList(1, 2, 3, 4));
        columnValues.add(N.asList(5, 6, 7, 8));

        RowDataSet ds = new RowDataSet(columnNames, columnValues);
        Function<DisposableObjArray, Integer> rowMapper = arr -> (Integer) arr.get(0) + (Integer) arr.get(1);
        Collector<Integer, ?, Integer> sumCollector = Collectors.summingInt(Integer::intValue);

        Sheet<String, String, Integer> pivot = ds.pivot("row", N.asList("val1", "val2"), "col", rowMapper, sumCollector);

        assertNotNull(pivot);
        assertEquals(2, pivot.rowKeySet().size());
        assertEquals(2, pivot.columnKeySet().size());
    }

    // Rollup Tests

    @Test
    public void testRollup() {
        List<DataSet> rollups = dataSet.rollup(N.asList("city", "name")).toList();

        assertNotNull(rollups);
        assertTrue(rollups.size() > 0);
        // Should contain groupings for: (city, name), (city), ()
    }

    @Test
    public void testRollupWithCollector() {
        Collector<Object, ?, Long> countCollector = Collectors.counting();
        List<DataSet> rollups = dataSet.rollup(N.asList("city"), "name", "count", countCollector).toList();

        assertNotNull(rollups);
        assertTrue(rollups.size() > 0);
        for (DataSet ds : rollups) {
            assertTrue(ds.containsColumn("count"));
        }
    }

    @Test
    public void testRollupWithRowType() {
        List<DataSet> rollups = dataSet.rollup(N.asList("city"), N.asList("name"), "names", List.class).toList();

        assertNotNull(rollups);
        assertTrue(rollups.size() > 0);
        for (DataSet ds : rollups) {
            if (ds.columnCount() > 1) {
                assertTrue(ds.containsColumn("names"));
            }
        }
    }

    @Test
    public void testRollupWithArrayCollector() {
        Collector<Object[], ?, Long> countCollector = Collectors.counting();
        List<DataSet> rollups = dataSet.rollup(N.asList("city"), N.asList("name", "age"), "count", countCollector).toList();

        assertNotNull(rollups);
        assertTrue(rollups.size() > 0);
        for (DataSet ds : rollups) {
            if (ds.columnCount() > 1) {
                assertTrue(ds.containsColumn("count"));
            }
        }
    }

    @Test
    public void testRollupWithRowMapper() {
        Function<DisposableObjArray, String> rowMapper = arr -> arr.get(0).toString();
        Collector<String, ?, List<String>> collector = Collectors.toList();
        List<DataSet> rollups = dataSet.rollup(N.asList("city"), N.asList("name"), "names", rowMapper, collector).toList();

        assertNotNull(rollups);
        assertTrue(rollups.size() > 0);
        for (DataSet ds : rollups) {
            if (ds.columnCount() > 1) {
                assertTrue(ds.containsColumn("names"));
            }
        }
    }

    @Test
    public void testRollupWithKeyExtractor() {
        Function<DisposableObjArray, String> keyExtractor = arr -> arr.get(0).toString();
        List<DataSet> rollups = dataSet.rollup(N.asList("city"), keyExtractor).toList();

        assertNotNull(rollups);
        assertTrue(rollups.size() > 0);
    }

    @Test
    public void testRollupWithKeyExtractorAndCollector() {
        Function<DisposableObjArray, String> keyExtractor = arr -> arr.get(0).toString();
        Collector<Object, ?, Long> countCollector = Collectors.counting();
        List<DataSet> rollups = dataSet.rollup(N.asList("city"), keyExtractor, "name", "count", countCollector).toList();

        assertNotNull(rollups);
        assertTrue(rollups.size() > 0);
    }

    @Test
    public void testRollupWithKeyExtractorAndRowType() {
        Function<DisposableObjArray, String> keyExtractor = arr -> arr.get(0).toString();
        List<DataSet> rollups = dataSet.rollup(N.asList("city"), keyExtractor, N.asList("name"), "names", List.class).toList();

        assertNotNull(rollups);
        assertTrue(rollups.size() > 0);
    }

    @Test
    public void testRollupWithKeyExtractorAndArrayCollector() {
        Function<DisposableObjArray, String> keyExtractor = arr -> arr.get(0).toString();
        Collector<Object[], ?, Long> countCollector = Collectors.counting();
        List<DataSet> rollups = dataSet.rollup(N.asList("city"), keyExtractor, N.asList("name"), "count", countCollector).toList();

        assertNotNull(rollups);
        assertTrue(rollups.size() > 0);
    }

    @Test
    public void testRollupWithKeyExtractorAndRowMapper() {
        Function<DisposableObjArray, String> keyExtractor = arr -> arr.get(0).toString();
        Function<DisposableObjArray, String> rowMapper = arr -> arr.get(0).toString();
        Collector<String, ?, List<String>> collector = Collectors.toList();
        List<DataSet> rollups = dataSet.rollup(N.asList("city"), keyExtractor, N.asList("name"), "names", rowMapper, collector).toList();

        assertNotNull(rollups);
        assertTrue(rollups.size() > 0);
    }

    // Cube Tests

    @Test
    public void testCube() {
        List<DataSet> cubes = dataSet.cube(N.asList("city", "name")).toList();

        assertNotNull(cubes);
        assertTrue(cubes.size() > 0);
        // Should contain all possible grouping combinations
    }

    @Test
    public void testCubeWithCollector() {
        Collector<Object, ?, Long> countCollector = Collectors.counting();
        List<DataSet> cubes = dataSet.cube(N.asList("city"), "name", "count", countCollector).toList();

        assertNotNull(cubes);
        assertTrue(cubes.size() > 0);
    }

    @Test
    public void testCubeWithRowType() {
        List<DataSet> cubes = dataSet.cube(N.asList("city"), N.asList("name"), "names", List.class).toList();

        assertNotNull(cubes);
        assertTrue(cubes.size() > 0);
    }

    @Test
    public void testCubeWithArrayCollector() {
        Collector<Object[], ?, Long> countCollector = Collectors.counting();
        List<DataSet> cubes = dataSet.cube(N.asList("city"), N.asList("name"), "count", countCollector).toList();

        assertNotNull(cubes);
        assertTrue(cubes.size() > 0);
    }

    @Test
    public void testCubeWithRowMapper() {
        Function<DisposableObjArray, String> rowMapper = arr -> arr.get(0).toString();
        Collector<String, ?, List<String>> collector = Collectors.toList();
        List<DataSet> cubes = dataSet.cube(N.asList("city"), N.asList("name"), "names", rowMapper, collector).toList();

        assertNotNull(cubes);
        assertTrue(cubes.size() > 0);
    }

    @Test
    public void testCubeWithKeyExtractor() {
        Function<DisposableObjArray, String> keyExtractor = arr -> arr.get(0).toString();
        List<DataSet> cubes = dataSet.cube(N.asList("city"), keyExtractor).toList();

        assertNotNull(cubes);
        assertTrue(cubes.size() > 0);
    }

    @Test
    public void testCubeWithKeyExtractorAndCollector() {
        Function<DisposableObjArray, String> keyExtractor = arr -> arr.get(0).toString();
        Collector<Object, ?, Long> countCollector = Collectors.counting();
        List<DataSet> cubes = dataSet.cube(N.asList("city"), keyExtractor, "name", "count", countCollector).toList();

        assertNotNull(cubes);
        assertTrue(cubes.size() > 0);
    }

    @Test
    public void testCubeWithKeyExtractorAndRowType() {
        Function<DisposableObjArray, String> keyExtractor = arr -> arr.join(", ");
        List<DataSet> cubes = dataSet.cube(N.asList("city"), keyExtractor, N.asList("name"), "names", List.class).toList();
        dataSet.println();

        cubes.forEach(DataSet::println);

        assertNotNull(cubes);
        assertEquals(1, cubes.size());

        N.println(Strings.repeat("=", 80));

        cubes = dataSet.cube(N.asList("city", "age"), keyExtractor, N.asList("name"), "names", List.class).toList();

        cubes.forEach(DataSet::println);

        assertNotNull(cubes);
        assertEquals(3, cubes.size());

        dataSet.groupBy(N.asList("city", "age"), keyExtractor, N.asList("name"), "names", List.class).println();
    }

    @Test
    public void testCubeWithKeyExtractorAndArrayCollector() {
        Function<DisposableObjArray, String> keyExtractor = arr -> arr.get(0).toString();
        Collector<Object[], ?, Long> countCollector = Collectors.counting();
        List<DataSet> cubes = dataSet.cube(N.asList("city"), keyExtractor, N.asList("name"), "count", countCollector).toList();

        assertNotNull(cubes);
        assertTrue(cubes.size() > 0);
    }

    @Test
    public void testCubeWithKeyExtractorAndRowMapper() {
        Function<DisposableObjArray, String> keyExtractor = arr -> arr.get(0).toString();
        Function<DisposableObjArray, String> rowMapper = arr -> arr.get(0).toString();
        Collector<String, ?, List<String>> collector = Collectors.toList();
        List<DataSet> cubes = dataSet.cube(N.asList("city"), keyExtractor, N.asList("name"), "names", rowMapper, collector).toList();

        assertNotNull(cubes);
        assertTrue(cubes.size() > 0);
    }

    // Sort Tests

    @Test
    public void testSortBy() {
        DataSet copy = dataSet.copy();
        copy.sortBy("age");

        List<Object> ages = copy.getColumn("age");
        for (int i = 1; i < ages.size(); i++) {
            assertTrue(((Integer) ages.get(i - 1)) <= ((Integer) ages.get(i)));
        }
    }

    @Test
    public void testSortByWithComparator() {
        DataSet copy = dataSet.copy();
        copy.sortBy("age", Comparator.reverseOrder());

        List<Object> ages = copy.getColumn("age");
        for (int i = 1; i < ages.size(); i++) {
            assertTrue(((Integer) ages.get(i - 1)) >= ((Integer) ages.get(i)));
        }
    }

    @Test
    public void testSortByMultipleColumns() {
        DataSet copy = dataSet.copy();
        copy.sortBy(N.asList("city", "age"));

        // Verify sorting
        List<Object> cities = copy.getColumn("city");
        List<Object> ages = copy.getColumn("age");

        for (int i = 1; i < cities.size(); i++) {
            int cityCompare = cities.get(i - 1).toString().compareTo(cities.get(i).toString());
            assertTrue(cityCompare <= 0);
            if (cityCompare == 0) {
                assertTrue(((Integer) ages.get(i - 1)) <= ((Integer) ages.get(i)));
            }
        }
    }

    @Test
    public void testSortByMultipleColumnsWithComparator() {
        DataSet copy = dataSet.copy();
        Comparator<Object[]> comp = (a, b) -> {
            int result = ((String) a[0]).compareTo((String) b[0]);
            if (result == 0) {
                result = ((Integer) b[1]).compareTo((Integer) a[1]); // reverse order for age
            }
            return result;
        };
        copy.sortBy(N.asList("city", "age"), comp);

        // Verify custom sorting
        assertNotNull(copy);
        assertEquals(dataSet.size(), copy.size());
    }

    @Test
    public void testSortByWithKeyExtractor() {
        DataSet copy = dataSet.copy();
        Function<DisposableObjArray, String> keyExtractor = arr -> arr.get(0) + "-" + arr.get(1);
        copy.sortBy(N.asList("name", "age"), keyExtractor);

        assertNotNull(copy);
        assertEquals(dataSet.size(), copy.size());
    }

    @Test
    public void testParallelSortBy() {
        DataSet copy = dataSet.copy();
        copy.parallelSortBy("age");

        List<Object> ages = copy.getColumn("age");
        for (int i = 1; i < ages.size(); i++) {
            assertTrue(((Integer) ages.get(i - 1)) <= ((Integer) ages.get(i)));
        }
    }

    @Test
    public void testParallelSortByWithComparator() {
        DataSet copy = dataSet.copy();
        copy.parallelSortBy("name", String.CASE_INSENSITIVE_ORDER);

        List<Object> names = copy.getColumn("name");
        for (int i = 1; i < names.size(); i++) {
            assertTrue(names.get(i - 1).toString().compareToIgnoreCase(names.get(i).toString()) <= 0);
        }
    }

    @Test
    public void testParallelSortByMultipleColumns() {
        DataSet copy = dataSet.copy();
        copy.parallelSortBy(N.asList("city", "name"));

        assertNotNull(copy);
        assertEquals(dataSet.size(), copy.size());
    }

    @Test
    public void testParallelSortByMultipleColumnsWithComparator() {
        DataSet copy = dataSet.copy();
        Comparator<Object[]> comp = Comparator.comparing((Object[] a) -> (String) a[0]).thenComparing(a -> (String) a[1]);
        copy.parallelSortBy(N.asList("city", "name"), comp);

        assertNotNull(copy);
        assertEquals(dataSet.size(), copy.size());
    }

    @Test
    public void testParallelSortByWithKeyExtractor() {
        DataSet copy = dataSet.copy();
        Function<DisposableObjArray, Integer> keyExtractor = arr -> (Integer) arr.get(0);
        copy.parallelSortBy(N.asList("age"), keyExtractor);

        List<Object> ages = copy.getColumn("age");
        for (int i = 1; i < ages.size(); i++) {
            assertTrue(((Integer) ages.get(i - 1)) <= ((Integer) ages.get(i)));
        }
    }

    // TopBy Tests

    @Test
    public void testTopBy() {
        DataSet top = dataSet.topBy("age", 3);

        assertNotNull(top);
        assertEquals(3, top.size());
    }

    @Test
    public void testTopByWithComparator() {
        DataSet top = dataSet.topBy("age", 2, Comparator.reverseOrder());

        assertNotNull(top);
        assertEquals(2, top.size());

        // Should contain the 2 youngest (lowest ages)
        List<Object> ages = top.getColumn("age");
        assertTrue(ages.contains(25));
        assertTrue(ages.contains(28));
    }

    @Test
    public void testTopByMultipleColumns() {
        DataSet top = dataSet.topBy(N.asList("city", "age"), 3);

        assertNotNull(top);
        assertEquals(3, top.size());
    }

    @Test
    public void testTopByMultipleColumnsWithComparator() {
        Comparator<Object[]> comp = (a, b) -> ((String) a[0]).compareTo((String) b[0]);
        DataSet top = dataSet.topBy(N.asList("city"), 2, comp);

        assertNotNull(top);
        assertEquals(2, top.size());
    }

    @Test
    public void testTopByWithKeyExtractor() {
        Function<DisposableObjArray, Integer> keyExtractor = arr -> (Integer) arr.get(0);
        DataSet top = dataSet.topBy(N.asList("age"), 3, keyExtractor);

        assertNotNull(top);
        assertEquals(3, top.size());
    }

    // Distinct Tests

    @Test
    public void testDistinct() {
        // Add duplicate rows
        List<String> columnNames = N.asList("col1", "col2");
        List<List<Object>> columnValues = new ArrayList<>();
        columnValues.add(N.asList("A", "A", "B", "B", "A"));
        columnValues.add(N.asList(1, 1, 2, 2, 1));

        RowDataSet ds = new RowDataSet(columnNames, columnValues);
        DataSet distinct = ds.distinct();

        assertNotNull(distinct);
        assertEquals(2, distinct.size()); // Only unique rows
    }

    @Test
    public void testDistinctBy() {
        DataSet distinct = dataSet.distinctBy("city");

        assertNotNull(distinct);
        assertEquals(3, distinct.size()); // NYC, LA, Chicago
    }

    @Test
    public void testDistinctByWithKeyExtractor() {
        Function<Object, String> keyExtractor = obj -> obj.toString().substring(0, 1);
        DataSet distinct = dataSet.distinctBy("name", keyExtractor);

        assertNotNull(distinct);
        assertTrue(distinct.size() <= dataSet.size());
    }

    @Test
    public void testDistinctByMultipleColumns() {
        // Create dataset with duplicates
        List<String> columnNames = N.asList("col1", "col2", "col3");
        List<List<Object>> columnValues = new ArrayList<>();
        columnValues.add(N.asList("A", "A", "B", "B", "A"));
        columnValues.add(N.asList(1, 2, 1, 1, 1));
        columnValues.add(N.asList("X", "Y", "Z", "W", "V"));

        RowDataSet ds = new RowDataSet(columnNames, columnValues);
        DataSet distinct = ds.distinctBy(N.asList("col1", "col2"));

        assertNotNull(distinct);
        assertEquals(3, distinct.size()); // (A,1), (A,2), (B,1)
    }

    @Test
    public void testDistinctByMultipleColumnsWithKeyExtractor() {
        Function<DisposableObjArray, String> keyExtractor = arr -> arr.get(0).toString();
        DataSet distinct = dataSet.distinctBy(N.asList("city", "age"), keyExtractor);

        assertNotNull(distinct);
        assertTrue(distinct.size() <= dataSet.size());
    }

    // Filter Tests

    @Test
    public void testFilterWithPredicate() {
        Predicate<DisposableObjArray> filter = arr -> ((Integer) arr.get(2)) > 30;
        DataSet filtered = dataSet.filter(filter);

        assertNotNull(filtered);
        assertEquals(2, filtered.size()); // Bob (35) and Tom (32)
    }

    @Test
    public void testFilterWithPredicateAndMax() {
        Predicate<DisposableObjArray> filter = arr -> ((Integer) arr.get(2)) > 25;
        DataSet filtered = dataSet.filter(filter, 2);

        assertNotNull(filtered);
        assertEquals(2, filtered.size());
    }

    @Test
    public void testFilterWithRowRange() {
        Predicate<DisposableObjArray> filter = arr -> true; // accept all
        DataSet filtered = dataSet.filter(1, 4, filter);

        assertNotNull(filtered);
        assertEquals(3, filtered.size());
    }

    @Test
    public void testFilterWithRowRangeAndMax() {
        Predicate<DisposableObjArray> filter = arr -> true;
        DataSet filtered = dataSet.filter(0, 5, filter, 3);

        assertNotNull(filtered);
        assertEquals(3, filtered.size());
    }

    @Test
    public void testFilterByColumn() {
        Predicate<Object> filter = age -> ((Integer) age) >= 30;
        DataSet filtered = dataSet.filter("age", filter);

        assertNotNull(filtered);
        assertEquals(3, filtered.size());
    }

    @Test
    public void testFilterByColumnWithMax() {
        Predicate<Object> filter = name -> ((String) name).startsWith("J");
        DataSet filtered = dataSet.filter("name", filter, 1);

        assertNotNull(filtered);
        assertEquals(1, filtered.size());
    }

    @Test
    public void testFilterByColumnWithRowRange() {
        Predicate<Object> filter = city -> "NYC".equals(city);
        DataSet filtered = dataSet.filter(0, 5, "city", filter);

        assertNotNull(filtered);
        assertEquals(2, filtered.size());
    }

    @Test
    public void testFilterByColumnWithRowRangeAndMax() {
        Predicate<Object> filter = city -> city != null;
        DataSet filtered = dataSet.filter(1, 4, "city", filter, 2);

        assertNotNull(filtered);
        assertEquals(2, filtered.size());
    }

    @Test
    public void testFilterByMultipleColumns() {
        Predicate<DisposableObjArray> filter = arr -> "NYC".equals(arr.get(0)) && ((Integer) arr.get(1)) > 30;
        DataSet filtered = dataSet.filter(N.asList("city", "age"), filter);

        assertNotNull(filtered);
        assertEquals(1, filtered.size()); // Only Bob
    }

    @Test
    public void testFilterByMultipleColumnsWithMax() {
        Predicate<DisposableObjArray> filter = arr -> arr.get(0) != null;
        DataSet filtered = dataSet.filter(N.asList("name", "city"), filter, 3);

        assertNotNull(filtered);
        assertEquals(3, filtered.size());
    }

    @Test
    public void testFilterByMultipleColumnsWithRowRange() {
        Predicate<DisposableObjArray> filter = arr -> true;
        DataSet filtered = dataSet.filter(1, 3, N.asList("name", "age"), filter);

        assertNotNull(filtered);
        assertEquals(2, filtered.size());
    }

    @Test
    public void testFilterByMultipleColumnsWithRowRangeAndMax() {
        Predicate<DisposableObjArray> filter = arr -> true;
        DataSet filtered = dataSet.filter(0, 5, N.asList("id", "name"), filter, 4);

        assertNotNull(filtered);
        assertEquals(4, filtered.size());
    }

    @Test
    public void testFilterByTuple2() {
        BiPredicate<Object, Object> filter = (name, age) -> ((Integer) age) > 30;
        DataSet filtered = dataSet.filter(Tuple.of("name", "age"), filter);

        assertNotNull(filtered);
        assertEquals(2, filtered.size());
    }

    @Test
    public void testFilterByTuple2WithMax() {
        BiPredicate<Object, Object> filter = (id, name) -> ((Integer) id) <= 3;
        DataSet filtered = dataSet.filter(Tuple.of("id", "name"), filter, 2);

        assertNotNull(filtered);
        assertEquals(2, filtered.size());
    }

    @Test
    public void testFilterByTuple2WithRowRange() {
        BiPredicate<Object, Object> filter = (name, city) -> "LA".equals(city);
        DataSet filtered = dataSet.filter(1, 5, Tuple.of("name", "city"), filter);

        assertNotNull(filtered);
        assertEquals(2, filtered.size());
    }

    @Test
    public void testFilterByTuple2WithRowRangeAndMax() {
        BiPredicate<Object, Object> filter = (id, age) -> true;
        DataSet filtered = dataSet.filter(0, 5, Tuple.of("id", "age"), filter, 3);

        assertNotNull(filtered);
        assertEquals(3, filtered.size());
    }

    @Test
    public void testFilterByTuple3() {
        TriPredicate<Object, Object, Object> filter = (id, name, age) -> ((Integer) age) < 30;
        DataSet filtered = dataSet.filter(Tuple.of("id", "name", "age"), filter);

        assertNotNull(filtered);
        assertEquals(2, filtered.size());
    }

    @Test
    public void testFilterByTuple3WithMax() {
        TriPredicate<Object, Object, Object> filter = (name, age, city) -> true;
        DataSet filtered = dataSet.filter(Tuple.of("name", "age", "city"), filter, 2);

        assertNotNull(filtered);
        assertEquals(2, filtered.size());
    }

    @Test
    public void testFilterByTuple3WithRowRange() {
        TriPredicate<Object, Object, Object> filter = (id, name, age) -> ((Integer) id) > 2;
        DataSet filtered = dataSet.filter(1, 5, Tuple.of("id", "name", "age"), filter);
        filtered.println();

        assertNotNull(filtered);
        assertEquals(3, filtered.size());
    }

    @Test
    public void testFilterByTuple3WithRowRangeAndMax() {
        TriPredicate<Object, Object, Object> filter = (id, age, city) -> true;
        DataSet filtered = dataSet.filter(0, 4, Tuple.of("id", "age", "city"), filter, 3);

        assertNotNull(filtered);
        assertEquals(3, filtered.size());
    }

    // Map Tests

    @Test
    public void testMapSingleColumn() {
        Function<Object, String> mapper = name -> ((String) name).toUpperCase();
        DataSet mapped = dataSet.map("name", "NAME", "id", mapper);

        assertNotNull(mapped);
        assertEquals(5, mapped.size());
        assertTrue(mapped.containsColumn("NAME"));
        assertTrue(mapped.containsColumn("id"));
        assertEquals("JOHN", mapped.absolute(0).get("NAME"));
    }

    @Test
    public void testMapSingleColumnWithMultipleCopying() {
        Function<Object, Integer> mapper = age -> ((Integer) age) * 2;
        DataSet mapped = dataSet.map("age", "doubleAge", N.asList("id", "name"), mapper);

        assertNotNull(mapped);
        assertEquals(5, mapped.size());
        assertTrue(mapped.containsColumn("doubleAge"));
        assertTrue(mapped.containsColumn("id"));
        assertTrue(mapped.containsColumn("name"));
        assertEquals(50, (Integer) mapped.absolute(0).get("doubleAge"));
    }

    @Test
    public void testMapTuple2() {
        BiFunction<Object, Object, String> mapper = (name, age) -> name + ":" + age;
        DataSet mapped = dataSet.map(Tuple.of("name", "age"), "info", N.asList("id"), mapper);

        assertNotNull(mapped);
        assertEquals(5, mapped.size());
        assertTrue(mapped.containsColumn("info"));
        assertTrue(mapped.containsColumn("id"));
        assertEquals("John:25", mapped.absolute(0).get("info"));
    }

    @Test
    public void testMapTuple3() {
        TriFunction<Object, Object, Object, String> mapper = (id, name, age) -> id + "-" + name + "-" + age;
        DataSet mapped = dataSet.map(Tuple.of("id", "name", "age"), "combined", N.asList("city"), mapper);

        assertNotNull(mapped);
        assertEquals(5, mapped.size());
        assertTrue(mapped.containsColumn("combined"));
        assertTrue(mapped.containsColumn("city"));
        assertEquals("1-John-25", mapped.absolute(0).get("combined"));
    }

    @Test
    public void testMapMultipleColumns() {
        Function<DisposableObjArray, String> mapper = arr -> arr.get(0) + ":" + arr.get(1);
        DataSet mapped = dataSet.map(N.asList("name", "city"), "location", N.asList("id"), mapper);

        assertNotNull(mapped);
        assertEquals(5, mapped.size());
        assertTrue(mapped.containsColumn("location"));
        assertTrue(mapped.containsColumn("id"));
        assertEquals("John:NYC", mapped.absolute(0).get("location"));
    }

    // FlatMap Tests

    @Test
    public void testFlatMapSingleColumn() {
        Function<Object, Collection<String>> mapper = name -> N.asList(((String) name).toLowerCase(), ((String) name).toUpperCase());
        DataSet flatMapped = dataSet.flatMap("name", "variations", "id", mapper);

        assertNotNull(flatMapped);
        assertEquals(10, flatMapped.size()); // 5 * 2
        assertTrue(flatMapped.containsColumn("variations"));
        assertTrue(flatMapped.containsColumn("id"));
    }

    @Test
    public void testFlatMapSingleColumnWithMultipleCopying() {
        Function<Object, Collection<Integer>> mapper = age -> N.asList((Integer) age, (Integer) age + 10);
        DataSet flatMapped = dataSet.flatMap("age", "ages", N.asList("id", "name"), mapper);

        assertNotNull(flatMapped);
        assertEquals(10, flatMapped.size());
        assertTrue(flatMapped.containsColumn("ages"));
        assertTrue(flatMapped.containsColumn("id"));
        assertTrue(flatMapped.containsColumn("name"));
    }

    @Test
    public void testFlatMapTuple2() {
        BiFunction<Object, Object, Collection<String>> mapper = (name, age) -> N.asList(name + "-young", name + "-old");
        DataSet flatMapped = dataSet.flatMap(Tuple.of("name", "age"), "status", N.asList("id"), mapper);

        assertNotNull(flatMapped);
        assertEquals(10, flatMapped.size());
        assertTrue(flatMapped.containsColumn("status"));
        assertTrue(flatMapped.containsColumn("id"));
    }

    @Test
    public void testFlatMapTuple3() {
        TriFunction<Object, Object, Object, Collection<String>> mapper = (id, name, age) -> N.asList("ID" + id, "NAME" + name, "AGE" + age);
        DataSet flatMapped = dataSet.flatMap(Tuple.of("id", "name", "age"), "tags", N.asList("city"), mapper);

        assertNotNull(flatMapped);
        assertEquals(15, flatMapped.size()); // 5 * 3
        assertTrue(flatMapped.containsColumn("tags"));
        assertTrue(flatMapped.containsColumn("city"));
    }

    @Test
    public void testFlatMapMultipleColumns() {
        Function<DisposableObjArray, Collection<String>> mapper = arr -> N.asList(arr.get(0).toString(), arr.get(1).toString());
        DataSet flatMapped = dataSet.flatMap(N.asList("name", "city"), "values", N.asList("id"), mapper);

        assertNotNull(flatMapped);
        assertEquals(10, flatMapped.size());
        assertTrue(flatMapped.containsColumn("values"));
        assertTrue(flatMapped.containsColumn("id"));
    }

    // Copy Tests

    @Test
    public void testCopy() {
        DataSet copy = dataSet.copy();

        assertNotNull(copy);
        assertEquals(dataSet.size(), copy.size());
        assertEquals(dataSet.columnCount(), copy.columnCount());
        assertNotSame(dataSet, copy);
    }

    @Test
    public void testCopyWithColumns() {
        DataSet copy = dataSet.copy(N.asList("id", "name"));

        assertNotNull(copy);
        assertEquals(dataSet.size(), copy.size());
        assertEquals(2, copy.columnCount());
        assertTrue(copy.containsColumn("id"));
        assertTrue(copy.containsColumn("name"));
        assertFalse(copy.containsColumn("age"));
    }

    @Test
    public void testCopyWithRowRange() {
        DataSet copy = dataSet.copy(1, 4);

        assertNotNull(copy);
        assertEquals(3, copy.size());
        assertEquals(dataSet.columnCount(), copy.columnCount());
    }

    @Test
    public void testCopyWithRowRangeAndColumns() {
        DataSet copy = dataSet.copy(1, 3, N.asList("name", "age"));

        assertNotNull(copy);
        assertEquals(2, copy.size());
        assertEquals(2, copy.columnCount());
        assertTrue(copy.containsColumn("name"));
        assertTrue(copy.containsColumn("age"));
    }

    @Test
    public void testClone() {
        DataSet cloned = dataSet.clone();

        assertNotNull(cloned);
        assertEquals(dataSet.size(), cloned.size());
        assertEquals(dataSet.columnCount(), cloned.columnCount());
        assertNotSame(dataSet, cloned);
    }

    @Test
    public void testCloneWithFreeze() {
        DataSet cloned = dataSet.clone(true);

        assertNotNull(cloned);
        assertEquals(dataSet.size(), cloned.size());
        assertEquals(dataSet.columnCount(), cloned.columnCount());
        assertTrue(cloned.isFrozen());
    }

    // Join Tests

    @Test
    public void testInnerJoinSingleColumn() {
        // Create right dataset
        List<String> rightColumns = N.asList("city", "country");
        List<List<Object>> rightValues = new ArrayList<>();
        rightValues.add(N.asList("NYC", "LA", "Chicago"));
        rightValues.add(N.asList("USA", "USA", "USA"));

        RowDataSet right = new RowDataSet(rightColumns, rightValues);

        DataSet joined = dataSet.innerJoin(right, "city", "city");

        joined.println();

        assertNotNull(joined);
        assertEquals(5, joined.size());
        assertTrue(joined.containsColumn("country"));
    }

    @Test
    public void testInnerJoinMultipleColumns() {
        // Create right dataset
        List<String> rightColumns = N.asList("city", "age", "salary");
        List<List<Object>> rightValues = new ArrayList<>();
        rightValues.add(N.asList("NYC", "LA", "NYC"));
        rightValues.add(N.asList(25, 30, 35));
        rightValues.add(N.asList(50000, 60000, 70000));

        RowDataSet right = new RowDataSet(rightColumns, rightValues);

        Map<String, String> onColumns = new HashMap<>();
        onColumns.put("city", "city");
        onColumns.put("age", "age");

        DataSet joined = dataSet.innerJoin(right, onColumns);

        assertNotNull(joined);
        assertTrue(joined.containsColumn("salary"));
    }

    @Test
    public void testInnerJoinWithNewColumn() {
        // Create right dataset
        List<String> rightColumns = N.asList("city", "info");
        List<List<Object>> rightValues = new ArrayList<>();
        rightValues.add(N.asList("NYC", "LA", "Chicago"));
        rightValues.add(N.asList("Big Apple", "City of Angels", "Windy City"));

        RowDataSet right = new RowDataSet(rightColumns, rightValues);

        Map<String, String> onColumns = new HashMap<>();
        onColumns.put("city", "city");

        DataSet joined = dataSet.innerJoin(right, onColumns, "cityInfo", Object[].class);

        assertNotNull(joined);
        assertTrue(joined.containsColumn("cityInfo"));
    }

    @Test
    public void testInnerJoinWithCollectionSupplier() {
        List<String> rightColumns = N.asList("city", "tag");
        List<List<Object>> rightValues = new ArrayList<>();
        rightValues.add(N.asList("NYC", "NYC", "LA"));
        rightValues.add(N.asList("tag1", "tag2", "tag3"));

        RowDataSet right = new RowDataSet(rightColumns, rightValues);

        Map<String, String> onColumns = new HashMap<>();
        onColumns.put("city", "city");

        IntFunction<List<Object>> collSupplier = size -> new ArrayList<>(size);
        DataSet joined = dataSet.innerJoin(right, onColumns, "tags", Object[].class, collSupplier);

        assertNotNull(joined);
        assertTrue(joined.containsColumn("tags"));
    }

    @Test
    public void testLeftJoinSingleColumn() {
        // Create right dataset with some non-matching values
        List<String> rightColumns = N.asList("city", "country");
        List<List<Object>> rightValues = new ArrayList<>();
        rightValues.add(N.asList("NYC", "LA"));
        rightValues.add(N.asList("USA", "USA"));

        RowDataSet right = new RowDataSet(rightColumns, rightValues);

        DataSet joined = dataSet.leftJoin(right, "city", "city");

        assertNotNull(joined);
        assertEquals(5, joined.size()); // All left rows preserved
        assertTrue(joined.containsColumn("country"));
    }

    @Test
    public void testLeftJoinMultipleColumns() {
        List<String> rightColumns = N.asList("city", "age", "bonus");
        List<List<Object>> rightValues = new ArrayList<>();
        rightValues.add(N.asList("NYC", "LA"));
        rightValues.add(N.asList(25, 30));
        rightValues.add(N.asList(1000, 2000));

        RowDataSet right = new RowDataSet(rightColumns, rightValues);

        Map<String, String> onColumns = new HashMap<>();
        onColumns.put("city", "city");
        onColumns.put("age", "age");

        DataSet joined = dataSet.leftJoin(right, onColumns);

        assertNotNull(joined);
        assertEquals(5, joined.size()); // All left rows preserved
    }

    @Test
    public void testLeftJoinWithNewColumn() {
        List<String> rightColumns = N.asList("city", "population");
        List<List<Object>> rightValues = new ArrayList<>();
        rightValues.add(N.asList("NYC", "LA"));
        rightValues.add(N.asList(8000000, 4000000));

        RowDataSet right = new RowDataSet(rightColumns, rightValues);

        Map<String, String> onColumns = new HashMap<>();
        onColumns.put("city", "city");

        DataSet joined = dataSet.leftJoin(right, onColumns, "cityData", Object[].class);

        assertNotNull(joined);
        assertEquals(5, joined.size());
        assertTrue(joined.containsColumn("cityData"));
    }

    @Test
    public void testLeftJoinWithCollectionSupplier() {
        List<String> rightColumns = N.asList("city", "feature");
        List<List<Object>> rightValues = new ArrayList<>();
        rightValues.add(N.asList("NYC", "NYC", "LA"));
        rightValues.add(N.asList("feature1", "feature2", "feature3"));

        RowDataSet right = new RowDataSet(rightColumns, rightValues);

        Map<String, String> onColumns = new HashMap<>();
        onColumns.put("city", "city");

        IntFunction<Set<Object>> collSupplier = size -> new HashSet<>(size);
        DataSet joined = dataSet.leftJoin(right, onColumns, "features", Object[].class, collSupplier);

        assertNotNull(joined);
        assertEquals(5, joined.size());
        assertTrue(joined.containsColumn("features"));
    }

    // Edge Cases and Error Tests

    @Test
    public void testEmptyDataSetOperations() {
        RowDataSet emptyDataSet = new RowDataSet(N.asList("col1", "col2"), N.asList(new ArrayList<>(), new ArrayList<>()));

        // Test XML export
        String xml = emptyDataSet.toXml();
        assertNotNull(xml);
        assertTrue(xml.contains("<dataSet>"));
        assertTrue(xml.contains("</dataSet>"));

        // Test CSV export
        String csv = emptyDataSet.toCsv();
        assertNotNull(csv);
        assertTrue(csv.contains("\"col1\",\"col2\""));

        // Test groupBy
        DataSet grouped = emptyDataSet.groupBy("col1", "col2", "result", Collectors.toList());
        assertNotNull(grouped);
        assertEquals(0, grouped.size());

        // Test filter
        DataSet filtered = emptyDataSet.filter(arr -> true);
        assertNotNull(filtered);
        assertEquals(0, filtered.size());

        // Test distinct
        DataSet distinct = emptyDataSet.distinct();
        assertNotNull(distinct);
        assertEquals(0, distinct.size());

        // Test sort
        emptyDataSet.sortBy("col1");
        assertEquals(0, emptyDataSet.size());

        // Test copy
        DataSet copy = emptyDataSet.copy();
        assertNotNull(copy);
        assertEquals(0, copy.size());
    }

    @Test
    public void testNullHandling() {
        // Create dataset with null values
        List<String> columnNames = N.asList("col1", "col2");
        List<List<Object>> columnValues = new ArrayList<>();
        columnValues.add(N.asList("A", null, "B"));
        columnValues.add(N.asList(1, 2, null));

        RowDataSet ds = new RowDataSet(columnNames, columnValues);

        // Test XML with nulls
        String xml = ds.toXml();
        assertNotNull(xml);
        assertTrue(xml.contains("null"));

        // Test CSV with nulls
        String csv = ds.toCsv();
        assertNotNull(csv);

        // Test filter with nulls
        DataSet filtered = ds.filter("col1", obj -> obj != null);
        assertEquals(2, filtered.size());

        // Test groupBy with nulls
        DataSet grouped = ds.groupBy("col1", "col2", "values", Collectors.toList());
        assertNotNull(grouped);

        // Test sort with nulls
        ds.sortBy("col1");
        assertEquals(3, ds.size());
    }

    @Test
    public void testLargeDataSetOperations() {
        // Create a larger dataset for performance-related tests
        int size = 1000;
        List<String> columnNames = N.asList("id", "value", "category");
        List<List<Object>> columnValues = new ArrayList<>();

        List<Object> ids = new ArrayList<>();
        List<Object> values = new ArrayList<>();
        List<Object> categories = new ArrayList<>();

        for (int i = 0; i < size; i++) {
            ids.add(i);
            values.add(i % 100);
            categories.add("CAT" + (i % 10));
        }

        columnValues.add(ids);
        columnValues.add(values);
        columnValues.add(categories);

        RowDataSet largeDs = new RowDataSet(columnNames, columnValues);

        // Test parallel sort
        largeDs.parallelSortBy("value");
        List<Object> sortedValues = largeDs.getColumn("value");
        for (int i = 1; i < sortedValues.size(); i++) {
            assertTrue(((Integer) sortedValues.get(i - 1)) <= ((Integer) sortedValues.get(i)));
        }

        // Test groupBy on large dataset
        DataSet grouped = largeDs.groupBy("category", "value", "sum", Collectors.summingInt(o -> (Integer) o));
        assertEquals(10, grouped.size());

        // Test topBy on large dataset
        DataSet top = largeDs.topBy("value", 10);
        assertEquals(10, top.size());
    }

    @Test
    public void testComplexJoinScenarios() {
        // Test self-join
        Map<String, String> onColumns = new HashMap<>();
        onColumns.put("city", "city");

        DataSet selfJoined = dataSet.innerJoin(dataSet, onColumns);
        assertNotNull(selfJoined);
        assertTrue(selfJoined.size() > 0);

        // Test join with no matches
        List<String> rightColumns = N.asList("city", "data");
        List<List<Object>> rightValues = new ArrayList<>();
        rightValues.add(N.asList("Paris", "London"));
        rightValues.add(N.asList("data1", "data2"));

        RowDataSet noMatchRight = new RowDataSet(rightColumns, rightValues);
        DataSet noMatchJoined = dataSet.innerJoin(noMatchRight, "city", "city");
        assertEquals(0, noMatchJoined.size());

        // Left join should preserve all left rows
        DataSet leftJoinNoMatch = dataSet.leftJoin(noMatchRight, "city", "city");
        assertEquals(5, leftJoinNoMatch.size());
    }

    @Test
    public void testColumnNameValidation() {
        // Test operations with invalid column names
        assertThrows(IllegalArgumentException.class, () -> {
            dataSet.sortBy("nonexistent");
        });

        assertThrows(IllegalArgumentException.class, () -> {
            dataSet.filter("nonexistent", obj -> true);
        });

        assertThrows(IllegalArgumentException.class, () -> {
            dataSet.groupBy("nonexistent", "name", "result", Collectors.toList());
        });

        assertThrows(IllegalArgumentException.class, () -> {
            dataSet.distinctBy("nonexistent");
        });

        assertThrows(IllegalArgumentException.class, () -> {
            dataSet.map("nonexistent", "new", "id", obj -> obj);
        });
    }

    @Test
    public void testRowIndexValidation() {
        // Test operations with invalid row indices
        assertThrows(IndexOutOfBoundsException.class, () -> {
            dataSet.toXml(-1, 3);
        });

        assertThrows(IndexOutOfBoundsException.class, () -> {
            dataSet.toXml(2, 10);
        });

        assertThrows(IndexOutOfBoundsException.class, () -> {
            dataSet.toCsv(5, 3, N.asList("name"));
        });

        assertThrows(IndexOutOfBoundsException.class, () -> {
            dataSet.filter(-1, 5, arr -> true);
        });

        assertThrows(IndexOutOfBoundsException.class, () -> {
            dataSet.copy(0, 10);
        });
    }

    @Test
    public void testSpecialCharactersInData() {
        // Test with special characters that might affect XML/CSV
        List<String> columnNames = N.asList("text", "value");
        List<List<Object>> columnValues = new ArrayList<>();
        columnValues.add(N.asList("Hello, World", "Test\"Quote", "Line\nBreak", "<tag>"));
        columnValues.add(N.asList(1, 2, 3, 4));

        RowDataSet specialDs = new RowDataSet(columnNames, columnValues);

        // XML should escape special characters
        String xml = specialDs.toXml();
        assertNotNull(xml);
        assertTrue(xml.contains("&lt;tag&gt;") || xml.contains("&lt;"));

        // CSV should handle commas and quotes
        String csv = specialDs.toCsv();
        assertNotNull(csv);
        assertTrue(csv.contains("\"Hello, World\"") || csv.contains("Hello, World"));
    }

    @Test
    public void testMultipleGroupByScenarios() {
        // Test groupBy with different aggregation types

        // Count
        DataSet countGrouped = dataSet.groupBy("city", "name", "count", Collectors.counting());
        assertNotNull(countGrouped);

        // List collection
        DataSet listGrouped = dataSet.groupBy("city", "age", "ages", Collectors.toList());
        assertNotNull(listGrouped);

        // Average
        DataSet avgGrouped = dataSet.groupBy("city", "age", "avgAge", Collectors.averagingInt(o -> (Integer) o));
        assertNotNull(avgGrouped);

        // Custom collector
        Collector<Object, ?, String> joiningCollector = Collectors.mapping(Object::toString, Collectors.joining(","));
        DataSet joinedGrouped = dataSet.groupBy("city", "name", "names", joiningCollector);
        assertNotNull(joinedGrouped);
        assertTrue(joinedGrouped.containsColumn("names"));
    }

    @Test
    public void testComplexFilterPredicates() {
        // Test compound predicates
        Predicate<DisposableObjArray> complexPredicate = arr -> {
            Integer age = (Integer) arr.get(2);
            String city = (String) arr.get(3);
            return age > 25 && age < 35 && ("NYC".equals(city) || "LA".equals(city));
        };

        DataSet filtered = dataSet.filter(complexPredicate);
        assertNotNull(filtered);
        assertTrue(filtered.size() > 0);

        // Test with column subset
        Predicate<DisposableObjArray> subsetPredicate = arr -> {
            String name = (String) arr.get(0);
            Integer age = (Integer) arr.get(1);
            return name.length() > 3 && age > 30;
        };

        DataSet subsetFiltered = dataSet.filter(N.asList("name", "age"), subsetPredicate);
        assertNotNull(subsetFiltered);
    }

    @Test
    public void testMapWithDifferentDataTypes() {
        // Test type conversions
        Function<Object, String> toStringMapper = obj -> "ID:" + obj;
        DataSet stringMapped = dataSet.map("id", "stringId", N.asList("name"), toStringMapper);
        assertEquals("ID:1", stringMapped.absolute(0).get("stringId"));

        // Test calculations
        Function<Object, Double> doubleMapper = obj -> ((Integer) obj) * 1.5;
        DataSet doubleMapped = dataSet.map("age", "adjustedAge", N.asList("name"), doubleMapper);
        assertEquals(37.5, doubleMapped.absolute(0).get("adjustedAge"));

        // Test object creation
        Function<DisposableObjArray, Map<String, Object>> mapMapper = arr -> {
            Map<String, Object> map = new HashMap<>();
            map.put("name", arr.get(0));
            map.put("age", arr.get(1));
            return map;
        };
        DataSet objectMapped = dataSet.map(N.asList("name", "age"), "info", N.asList("id"), mapMapper);
        assertNotNull(objectMapped.absolute(0).get("info"));
        assertTrue(objectMapped.absolute(0).get("info") instanceof Map);
    }

    @Test
    public void testFlatMapEdgeCases() {
        // Test empty collection returned
        Function<Object, Collection<String>> emptyMapper = obj -> Collections.emptyList();
        DataSet emptyFlatMapped = dataSet.flatMap("name", "empty", N.asList("id"), emptyMapper);
        assertEquals(0, emptyFlatMapped.size());

        // Test variable size collections
        Function<Object, Collection<Integer>> variableMapper = obj -> {
            int count = ((Integer) obj) % 3;
            List<Integer> result = new ArrayList<>();
            for (int i = 0; i < count; i++) {
                result.add(i);
            }
            return result;
        };
        DataSet variableFlatMapped = dataSet.flatMap("id", "values", N.asList("name"), variableMapper);
        assertNotNull(variableFlatMapped);

        // Test with null collection
        Function<Object, Collection<String>> nullSafeMapper = obj -> obj == null ? Collections.emptyList() : N.asList(obj.toString());
        DataSet nullSafeFlatMapped = dataSet.flatMap("name", "safe", N.asList("id"), nullSafeMapper);
        assertEquals(5, nullSafeFlatMapped.size());
    }

    @Test
    public void testTopByEdgeCases() {
        // Test n greater than size
        DataSet allTop = dataSet.topBy("age", 10);
        assertEquals(5, allTop.size());

        // Test n = 1
        DataSet singleTop = dataSet.topBy("age", 1);
        assertEquals(1, singleTop.size());

        // Test with ties
        List<String> columnNames = N.asList("id", "value");
        List<List<Object>> columnValues = new ArrayList<>();
        columnValues.add(N.asList(1, 2, 3, 4, 5));
        columnValues.add(N.asList(10, 10, 20, 20, 30));

        RowDataSet tieDs = new RowDataSet(columnNames, columnValues);
        DataSet topWithTies = tieDs.topBy("value", 3);
        assertEquals(3, topWithTies.size());
    }

    @Test
    public void testDistinctComplexKeys() {
        // Create dataset with complex duplicate patterns
        List<String> columnNames = N.asList("a", "b", "c");
        List<List<Object>> columnValues = new ArrayList<>();
        columnValues.add(N.asList(1, 1, 2, 2, 3));
        columnValues.add(N.asList("X", "X", "Y", "Y", "Z"));
        columnValues.add(N.asList(true, false, true, true, false));

        RowDataSet complexDs = new RowDataSet(columnNames, columnValues);

        // Distinct by single column
        DataSet distinctA = complexDs.distinctBy("a");
        assertEquals(3, distinctA.size());

        // Distinct by multiple columns
        DataSet distinctAB = complexDs.distinctBy(N.asList("a", "b"));
        assertEquals(3, distinctAB.size());

        // Distinct by all columns
        DataSet distinctAll = complexDs.distinct();
        assertEquals(4, distinctAll.size()); // One duplicate row (2, Y, true)

        // Distinct with key extractor
        Function<DisposableObjArray, String> compositeKeyExtractor = arr -> arr.get(0) + "-" + arr.get(1);
        DataSet distinctComposite = complexDs.distinctBy(N.asList("a", "b"), compositeKeyExtractor);
        assertEquals(3, distinctComposite.size());
    }

    @Test
    public void testXmlAndCsvWithEmptyColumns() {
        // Test with empty column selection
        Collection<String> emptyColumns = Collections.emptyList();

        String xml = dataSet.toXml(0, 2, emptyColumns);
        assertNotNull(xml);
        assertTrue(xml.contains("<dataSet>"));
        assertTrue(xml.contains("</dataSet>"));

        String csv = dataSet.toCsv(0, 2, emptyColumns);
        assertNotNull(csv);
        assertEquals("", csv.trim());
    }

    @Test
    public void testParallelOperationsConsistency() {
        // Create larger dataset for parallel operations
        int size = 100;
        List<Object> values = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            values.add(i);
        }

        List<String> columnNames = N.asList("value");
        List<List<Object>> columnValues = N.asList(values);
        RowDataSet largeDs = new RowDataSet(columnNames, columnValues);

        // Compare parallel vs sequential sort
        DataSet seqCopy = largeDs.copy();
        DataSet parCopy = largeDs.copy();

        seqCopy.sortBy("value", Comparator.reverseOrder());
        parCopy.parallelSortBy("value", Comparator.reverseOrder());

        // Results should be identical
        for (int i = 0; i < size; i++) {
            assertEquals((Object) seqCopy.absolute(i).get("value"), (Object) parCopy.absolute(i).get("value"));
        }
    }

}
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
import org.junit.jupiter.api.Tag;
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

@Tag("new-test")
public class RowDataset101Test extends TestBase {

    private Dataset dataset;
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

        dataset = new RowDataset(columnNames, columnValues);
    }

    @Test
    public void testToXml() {
        String xml = dataset.toXml();
        assertNotNull(xml);
        assertTrue(xml.contains("<dataset>"));
        assertTrue(xml.contains("</dataset>"));
        assertTrue(xml.contains("<row>"));
        assertTrue(xml.contains("<id>"));
        assertTrue(xml.contains("<name>"));
    }

    @Test
    public void testToXmlWithCustomRowElementName() {
        String xml = dataset.toXml("record");
        assertNotNull(xml);
        assertTrue(xml.contains("<record>"));
        assertTrue(xml.contains("</record>"));
    }

    @Test
    public void testToXmlWithRowRange() {
        String xml = dataset.toXml(1, 3);
        assertNotNull(xml);
        assertTrue(xml.contains("Jane"));
        assertTrue(xml.contains("Bob"));
        assertFalse(xml.contains("John"));
        assertFalse(xml.contains("Alice"));
    }

    @Test
    public void testToXmlWithRowRangeAndCustomElementName() {
        String xml = dataset.toXml(1, 3, "person");
        assertNotNull(xml);
        assertTrue(xml.contains("<person>"));
        assertTrue(xml.contains("Jane"));
        assertTrue(xml.contains("Bob"));
    }

    @Test
    public void testToXmlWithRowRangeAndColumnNames() {
        Collection<String> columns = N.asList("name", "age");
        String xml = dataset.toXml(1, 3, columns);
        assertNotNull(xml);
        assertTrue(xml.contains("Jane"));
        assertTrue(xml.contains("30"));
        assertFalse(xml.contains("<id>"));
        assertFalse(xml.contains("<city>"));
    }

    @Test
    public void testToXmlWithAllParameters() {
        Collection<String> columns = N.asList("name", "age");
        String xml = dataset.toXml(1, 3, columns, "employee");
        assertNotNull(xml);
        assertTrue(xml.contains("<employee>"));
        assertTrue(xml.contains("Jane"));
        assertTrue(xml.contains("30"));
    }

    @Test
    public void testToXmlToFile(@TempDir Path tempDir) throws IOException {
        File outputFile = tempDir.resolve("test.xml").toFile();
        dataset.toXml(outputFile);
        assertTrue(outputFile.exists());

        String content = IOUtil.readAllToString(outputFile);
        assertTrue(content.contains("<dataset>"));
        assertTrue(content.contains("John"));
    }

    @Test
    public void testToXmlToFileWithCustomElementName(@TempDir Path tempDir) throws IOException {
        File outputFile = tempDir.resolve("test2.xml").toFile();
        dataset.toXml("item", outputFile);
        assertTrue(outputFile.exists());

        String content = IOUtil.readAllToString(outputFile);
        assertTrue(content.contains("<item>"));
    }

    @Test
    public void testToXmlToFileWithRowRange(@TempDir Path tempDir) throws IOException {
        File outputFile = tempDir.resolve("test3.xml").toFile();
        dataset.toXml(1, 3, outputFile);
        assertTrue(outputFile.exists());

        String content = IOUtil.readAllToString(outputFile);
        assertTrue(content.contains("Jane"));
        assertFalse(content.contains("John"));
    }

    @Test
    public void testToXmlToFileWithAllParams(@TempDir Path tempDir) throws IOException {
        File outputFile = tempDir.resolve("test4.xml").toFile();
        Collection<String> columns = N.asList("name", "age");
        dataset.toXml(0, 2, columns, "record", outputFile);
        assertTrue(outputFile.exists());

        String content = IOUtil.readAllToString(outputFile);
        assertTrue(content.contains("<record>"));
        assertTrue(content.contains("John"));
        assertFalse(content.contains("<city>"));
    }

    @Test
    public void testToXmlToOutputStream() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        dataset.toXml(baos);

        String xml = baos.toString();
        assertTrue(xml.contains("<dataset>"));
        assertTrue(xml.contains("John"));
    }

    @Test
    public void testToXmlToOutputStreamWithElementName() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        dataset.toXml("data", baos);

        String xml = baos.toString();
        assertTrue(xml.contains("<data>"));
    }

    @Test
    public void testToXmlToOutputStreamWithRowRange() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        dataset.toXml(1, 3, baos);

        String xml = baos.toString();
        assertTrue(xml.contains("Jane"));
        assertFalse(xml.contains("John"));
    }

    @Test
    public void testToXmlToOutputStreamWithAllParams() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Collection<String> columns = N.asList("name");
        dataset.toXml(0, 2, columns, "person", baos);

        String xml = baos.toString();
        assertTrue(xml.contains("<person>"));
        assertTrue(xml.contains("John"));
        assertFalse(xml.contains("<age>"));
    }

    @Test
    public void testToXmlToWriter() throws IOException {
        StringWriter writer = new StringWriter();
        dataset.toXml(writer);

        String xml = writer.toString();
        assertTrue(xml.contains("<dataset>"));
        assertTrue(xml.contains("John"));
    }

    @Test
    public void testToXmlToWriterWithElementName() throws IOException {
        StringWriter writer = new StringWriter();
        dataset.toXml("entry", writer);

        String xml = writer.toString();
        assertTrue(xml.contains("<entry>"));
    }

    @Test
    public void testToXmlToWriterWithRowRange() throws IOException {
        StringWriter writer = new StringWriter();
        dataset.toXml(2, 4, writer);

        String xml = writer.toString();
        assertTrue(xml.contains("Bob"));
        assertTrue(xml.contains("Alice"));
        assertFalse(xml.contains("John"));
    }

    @Test
    public void testToXmlToWriterWithAllParams() throws IOException {
        StringWriter writer = new StringWriter();
        Collection<String> columns = N.asList("id", "name");
        dataset.toXml(0, 2, columns, "user", writer);

        String xml = writer.toString();
        assertTrue(xml.contains("<user>"));
        assertTrue(xml.contains("<id>1</id>"));
        assertTrue(xml.contains("John"));
        assertFalse(xml.contains("<age>"));
    }

    @Test
    public void testToCsv() {
        String csv = dataset.toCsv();
        assertNotNull(csv);
        assertTrue(csv.contains("\"id\",\"name\",\"age\",\"city\""));
        assertTrue(csv.contains("1,\"John\",25,\"NYC\""));
    }

    @Test
    public void testToCsvWithRowRangeAndColumns() {
        Collection<String> columns = N.asList("name", "age");
        String csv = dataset.toCsv(1, 3, columns);
        assertNotNull(csv);
        assertTrue(csv.contains("\"name\",\"age\""));
        assertTrue(csv.contains("\"Jane\",30"));
        assertTrue(csv.contains("\"Bob\",35"));
        assertFalse(csv.contains("John"));
    }

    @Test
    public void testToCsvToFile(@TempDir Path tempDir) throws IOException {
        File outputFile = tempDir.resolve("test.csv").toFile();
        dataset.toCsv(outputFile);
        assertTrue(outputFile.exists());

        String content = IOUtil.readAllToString(outputFile);
        assertTrue(content.contains("\"id\",\"name\",\"age\",\"city\""));
        assertTrue(content.contains("\"John\""));
    }

    @Test
    public void testToCsvToFileWithParams(@TempDir Path tempDir) throws IOException {
        File outputFile = tempDir.resolve("test2.csv").toFile();
        Collection<String> columns = N.asList("name", "city");
        dataset.toCsv(1, 3, columns, outputFile);
        assertTrue(outputFile.exists());

        String content = IOUtil.readAllToString(outputFile);
        assertTrue(content.contains("\"name\",\"city\""));
        assertTrue(content.contains("\"Jane\",\"LA\""));
        assertFalse(content.contains("John"));
    }

    @Test
    public void testToCsvToOutputStream() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        dataset.toCsv(baos);

        String csv = baos.toString();
        assertTrue(csv.contains("\"id\",\"name\",\"age\",\"city\""));
        assertTrue(csv.contains("John"));
    }

    @Test
    public void testToCsvToOutputStreamWithParams() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Collection<String> columns = N.asList("id", "name");
        dataset.toCsv(0, 2, columns, baos);

        String csv = baos.toString();
        assertTrue(csv.contains("\"id\",\"name\""));
        assertTrue(csv.contains("1,\"John\""));
        assertFalse(csv.contains("age"));
    }

    @Test
    public void testToCsvToWriter() throws IOException {
        StringWriter writer = new StringWriter();
        dataset.toCsv(writer);

        String csv = writer.toString();
        assertTrue(csv.contains("\"id\",\"name\",\"age\",\"city\""));
        assertTrue(csv.contains("\"John\""));
    }

    @Test
    public void testToCsvToWriterWithParams() throws IOException {
        StringWriter writer = new StringWriter();
        Collection<String> columns = N.asList("name");
        dataset.toCsv(2, 4, columns, writer);

        String csv = writer.toString();
        assertTrue(csv.contains("name"));
        assertTrue(csv.contains("Bob"));
        assertTrue(csv.contains("Alice"));
        assertFalse(csv.contains("John"));
    }

    @Test
    public void testGroupByWithSingleKeyAndCollector() {
        Collector<Object, ?, List<Object>> listCollector = Collectors.toList();
        Dataset grouped = dataset.groupBy("city", "name", "names", listCollector);

        assertNotNull(grouped);
        assertEquals(3, grouped.size());
        assertTrue(grouped.containsColumn("city"));
        assertTrue(grouped.containsColumn("names"));
    }

    @Test
    public void testGroupByWithSingleKeyAndRowType() {
        Dataset grouped = dataset.groupBy("city", N.asList("name", "age"), "people", Object[].class);

        assertNotNull(grouped);
        assertEquals(3, grouped.size());
        assertTrue(grouped.containsColumn("city"));
        assertTrue(grouped.containsColumn("people"));
    }

    @Test
    public void testGroupByWithSingleKeyAndArrayCollector() {
        Collector<Object[], ?, List<Object[]>> collector = Collectors.toList();
        Dataset grouped = dataset.groupBy("city", N.asList("name", "age"), "data", collector);

        assertNotNull(grouped);
        assertEquals(3, grouped.size());
        assertTrue(grouped.containsColumn("city"));
        assertTrue(grouped.containsColumn("data"));
    }

    @Test
    public void testGroupByWithSingleKeyAndRowMapper() {
        Function<DisposableObjArray, String> rowMapper = arr -> arr.get(0) + "-" + arr.get(1);
        Collector<String, ?, List<String>> collector = Collectors.toList();
        Dataset grouped = dataset.groupBy("city", N.asList("name", "age"), "info", rowMapper, collector);

        assertNotNull(grouped);
        assertEquals(3, grouped.size());
        assertTrue(grouped.containsColumn("city"));
        assertTrue(grouped.containsColumn("info"));
    }

    @Test
    public void testGroupByWithKeyExtractor() {
        Function<Object, String> keyExtractor = obj -> obj.toString().toUpperCase();
        Collector<Object, ?, List<Object>> collector = Collectors.toList();
        Dataset grouped = dataset.groupBy("city", keyExtractor, "name", "names", collector);

        assertNotNull(grouped);
        assertTrue(grouped.containsColumn("city"));
        assertTrue(grouped.containsColumn("names"));
    }

    @Test
    public void testGroupByWithMultipleKeys() {
        Dataset grouped = dataset.groupBy(N.asList("city"));

        assertNotNull(grouped);
        assertEquals(3, grouped.size());
        assertTrue(grouped.containsColumn("city"));
    }

    @Test
    public void testGroupByWithMultipleKeysAndCollector() {
        Collector<Object, ?, Long> countCollector = Collectors.counting();
        Dataset grouped = dataset.groupBy(N.asList("city"), "name", "count", countCollector);

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

        RowDataset ds = new RowDataset(columnNames, columnValues);
        Dataset grouped = ds.groupBy(N.asList("name"), N.asList("id"), "ids", List.class);

        assertNotNull(grouped);
        assertEquals(2, grouped.size());
        assertTrue(grouped.containsColumn("name"));
        assertTrue(grouped.containsColumn("ids"));
    }

    @Test
    public void testGroupByWithMultipleKeysAndArrayCollector() {
        Collector<Object[], ?, Long> countCollector = Collectors.counting();
        Dataset grouped = dataset.groupBy(N.asList("city"), N.asList("name", "age"), "count", countCollector);

        assertNotNull(grouped);
        assertEquals(3, grouped.size());
        assertTrue(grouped.containsColumn("city"));
        assertTrue(grouped.containsColumn("count"));
    }

    @Test
    public void testGroupByWithMultipleKeysAndRowMapper() {
        Function<DisposableObjArray, Integer> rowMapper = arr -> (Integer) arr.get(0);
        Collector<Integer, ?, Integer> sumCollector = Collectors.summingInt(Integer::intValue);
        Dataset grouped = dataset.groupBy(N.asList("city"), N.asList("age"), "totalAge", rowMapper, sumCollector);

        assertNotNull(grouped);
        assertEquals(3, grouped.size());
        assertTrue(grouped.containsColumn("city"));
        assertTrue(grouped.containsColumn("totalAge"));
    }

    @Test
    public void testGroupByWithMultipleKeysAndKeyExtractor() {
        Function<DisposableObjArray, String> keyExtractor = arr -> arr.get(0).toString();
        Dataset grouped = dataset.groupBy(N.asList("city"), keyExtractor);

        assertNotNull(grouped);
        assertTrue(grouped.containsColumn("city"));
    }

    @Test
    public void testGroupByWithMultipleKeysKeyExtractorAndCollector() {
        Function<DisposableObjArray, String> keyExtractor = arr -> arr.get(0).toString();
        Collector<Object, ?, Long> countCollector = Collectors.counting();
        Dataset grouped = dataset.groupBy(N.asList("city"), keyExtractor, "name", "count", countCollector);

        assertNotNull(grouped);
        assertTrue(grouped.containsColumn("city"));
        assertTrue(grouped.containsColumn("count"));
    }

    @Test
    public void testGroupByWithMultipleKeysKeyExtractorAndRowType() {
        Function<DisposableObjArray, String> keyExtractor = arr -> arr.get(0).toString();
        Dataset grouped = dataset.groupBy(N.asList("city"), keyExtractor, N.asList("name"), "names", List.class);
        dataset.println();

        grouped.println();

        assertNotNull(grouped);
        assertTrue(grouped.containsColumn("city"));
        assertTrue(grouped.containsColumn("names"));
    }

    @Test
    public void testGroupByWithMultipleKeysKeyExtractorAndArrayCollector() {
        Function<DisposableObjArray, String> keyExtractor = arr -> arr.get(0).toString();
        Collector<Object[], ?, Long> countCollector = Collectors.counting();
        Dataset grouped = dataset.groupBy(N.asList("city"), keyExtractor, N.asList("name"), "count", countCollector);

        grouped.println();

        assertNotNull(grouped);
        assertTrue(grouped.containsColumn("city"));
        assertTrue(grouped.containsColumn("count"));
        assertEquals(3, grouped.size());
    }

    @Test
    public void testGroupByWithMultipleKeysKeyExtractorAndRowMapper() {
        Function<DisposableObjArray, String> keyExtractor = arr -> arr.get(0).toString();
        Function<DisposableObjArray, String> rowMapper = arr -> arr.get(0).toString();
        Collector<String, ?, List<String>> collector = Collectors.toList();
        Dataset grouped = dataset.groupBy(N.asList("city"), keyExtractor, N.asList("name"), "names", rowMapper, collector);

        assertNotNull(grouped);
        assertTrue(grouped.containsColumn("city"));
        assertTrue(grouped.containsColumn("names"));
    }

    @Test
    public void testPivotWithSingleAggregateColumn() {
        List<String> columnNames = N.asList("row", "col", "value");
        List<List<Object>> columnValues = new ArrayList<>();
        columnValues.add(N.asList("A", "A", "B", "B"));
        columnValues.add(N.asList("X", "Y", "X", "Y"));
        columnValues.add(N.asList(1, 2, 3, 4));

        RowDataset ds = new RowDataset(columnNames, columnValues);
        Collector<Object, ?, Integer> sumCollector = Collectors.summingInt(o -> (Integer) o);

        Sheet<String, String, Integer> pivot = ds.pivot("row", "col", "value", sumCollector);

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

        RowDataset ds = new RowDataset(columnNames, columnValues);
        Collector<Object[], ?, String> joiningCollector = Collectors.mapping(arr -> arr[0] + "-" + arr[1], Collectors.joining(","));

        Sheet<String, String, String> pivot = ds.pivot("row", "col", N.asList("val1", "val2"), joiningCollector);

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

        RowDataset ds = new RowDataset(columnNames, columnValues);
        Function<DisposableObjArray, Integer> rowMapper = arr -> (Integer) arr.get(0) + (Integer) arr.get(1);
        Collector<Integer, ?, Integer> sumCollector = Collectors.summingInt(Integer::intValue);

        Sheet<String, String, Integer> pivot = ds.pivot("row", "col", N.asList("val1", "val2"), rowMapper, sumCollector);

        assertNotNull(pivot);
        assertEquals(2, pivot.rowKeySet().size());
        assertEquals(2, pivot.columnKeySet().size());
    }

    @Test
    public void testRollup() {
        List<Dataset> rollups = dataset.rollup(N.asList("city", "name")).toList();

        assertNotNull(rollups);
        assertTrue(rollups.size() > 0);
    }

    @Test
    public void testRollupWithCollector() {
        Collector<Object, ?, Long> countCollector = Collectors.counting();
        List<Dataset> rollups = dataset.rollup(N.asList("city"), "name", "count", countCollector).toList();

        assertNotNull(rollups);
        assertTrue(rollups.size() > 0);
        for (Dataset ds : rollups) {
            assertTrue(ds.containsColumn("count"));
        }
    }

    @Test
    public void testRollupWithRowType() {
        List<Dataset> rollups = dataset.rollup(N.asList("city"), N.asList("name"), "names", List.class).toList();

        assertNotNull(rollups);
        assertTrue(rollups.size() > 0);
        for (Dataset ds : rollups) {
            if (ds.columnCount() > 1) {
                assertTrue(ds.containsColumn("names"));
            }
        }
    }

    @Test
    public void testRollupWithArrayCollector() {
        Collector<Object[], ?, Long> countCollector = Collectors.counting();
        List<Dataset> rollups = dataset.rollup(N.asList("city"), N.asList("name", "age"), "count", countCollector).toList();

        assertNotNull(rollups);
        assertTrue(rollups.size() > 0);
        for (Dataset ds : rollups) {
            if (ds.columnCount() > 1) {
                assertTrue(ds.containsColumn("count"));
            }
        }
    }

    @Test
    public void testRollupWithRowMapper() {
        Function<DisposableObjArray, String> rowMapper = arr -> arr.get(0).toString();
        Collector<String, ?, List<String>> collector = Collectors.toList();
        List<Dataset> rollups = dataset.rollup(N.asList("city"), N.asList("name"), "names", rowMapper, collector).toList();

        assertNotNull(rollups);
        assertTrue(rollups.size() > 0);
        for (Dataset ds : rollups) {
            if (ds.columnCount() > 1) {
                assertTrue(ds.containsColumn("names"));
            }
        }
    }

    @Test
    public void testRollupWithKeyExtractor() {
        Function<DisposableObjArray, String> keyExtractor = arr -> arr.get(0).toString();
        List<Dataset> rollups = dataset.rollup(N.asList("city"), keyExtractor).toList();

        assertNotNull(rollups);
        assertTrue(rollups.size() > 0);
    }

    @Test
    public void testRollupWithKeyExtractorAndCollector() {
        Function<DisposableObjArray, String> keyExtractor = arr -> arr.get(0).toString();
        Collector<Object, ?, Long> countCollector = Collectors.counting();
        List<Dataset> rollups = dataset.rollup(N.asList("city"), keyExtractor, "name", "count", countCollector).toList();

        assertNotNull(rollups);
        assertTrue(rollups.size() > 0);
    }

    @Test
    public void testRollupWithKeyExtractorAndRowType() {
        Function<DisposableObjArray, String> keyExtractor = arr -> arr.get(0).toString();
        List<Dataset> rollups = dataset.rollup(N.asList("city"), keyExtractor, N.asList("name"), "names", List.class).toList();

        assertNotNull(rollups);
        assertTrue(rollups.size() > 0);
    }

    @Test
    public void testRollupWithKeyExtractorAndArrayCollector() {
        Function<DisposableObjArray, String> keyExtractor = arr -> arr.get(0).toString();
        Collector<Object[], ?, Long> countCollector = Collectors.counting();
        List<Dataset> rollups = dataset.rollup(N.asList("city"), keyExtractor, N.asList("name"), "count", countCollector).toList();

        assertNotNull(rollups);
        assertTrue(rollups.size() > 0);
    }

    @Test
    public void testRollupWithKeyExtractorAndRowMapper() {
        Function<DisposableObjArray, String> keyExtractor = arr -> arr.get(0).toString();
        Function<DisposableObjArray, String> rowMapper = arr -> arr.get(0).toString();
        Collector<String, ?, List<String>> collector = Collectors.toList();
        List<Dataset> rollups = dataset.rollup(N.asList("city"), keyExtractor, N.asList("name"), "names", rowMapper, collector).toList();

        assertNotNull(rollups);
        assertTrue(rollups.size() > 0);
    }

    @Test
    public void testCube() {
        List<Dataset> cubes = dataset.cube(N.asList("city", "name")).toList();

        assertNotNull(cubes);
        assertTrue(cubes.size() > 0);
    }

    @Test
    public void testCubeWithCollector() {
        Collector<Object, ?, Long> countCollector = Collectors.counting();
        List<Dataset> cubes = dataset.cube(N.asList("city"), "name", "count", countCollector).toList();

        assertNotNull(cubes);
        assertTrue(cubes.size() > 0);
    }

    @Test
    public void testCubeWithRowType() {
        List<Dataset> cubes = dataset.cube(N.asList("city"), N.asList("name"), "names", List.class).toList();

        assertNotNull(cubes);
        assertTrue(cubes.size() > 0);
    }

    @Test
    public void testCubeWithArrayCollector() {
        Collector<Object[], ?, Long> countCollector = Collectors.counting();
        List<Dataset> cubes = dataset.cube(N.asList("city"), N.asList("name"), "count", countCollector).toList();

        assertNotNull(cubes);
        assertTrue(cubes.size() > 0);
    }

    @Test
    public void testCubeWithRowMapper() {
        Function<DisposableObjArray, String> rowMapper = arr -> arr.get(0).toString();
        Collector<String, ?, List<String>> collector = Collectors.toList();
        List<Dataset> cubes = dataset.cube(N.asList("city"), N.asList("name"), "names", rowMapper, collector).toList();

        assertNotNull(cubes);
        assertTrue(cubes.size() > 0);
    }

    @Test
    public void testCubeWithKeyExtractor() {
        Function<DisposableObjArray, String> keyExtractor = arr -> arr.get(0).toString();
        List<Dataset> cubes = dataset.cube(N.asList("city"), keyExtractor).toList();

        assertNotNull(cubes);
        assertTrue(cubes.size() > 0);
    }

    @Test
    public void testCubeWithKeyExtractorAndCollector() {
        Function<DisposableObjArray, String> keyExtractor = arr -> arr.get(0).toString();
        Collector<Object, ?, Long> countCollector = Collectors.counting();
        List<Dataset> cubes = dataset.cube(N.asList("city"), keyExtractor, "name", "count", countCollector).toList();

        assertNotNull(cubes);
        assertTrue(cubes.size() > 0);
    }

    @Test
    public void testCubeWithKeyExtractorAndRowType() {
        Function<DisposableObjArray, String> keyExtractor = arr -> arr.join(", ");
        List<Dataset> cubes = dataset.cube(N.asList("city"), keyExtractor, N.asList("name"), "names", List.class).toList();
        dataset.println();

        cubes.forEach(Dataset::println);

        assertNotNull(cubes);
        assertEquals(2, cubes.size());

        N.println(Strings.repeat("=", 80));

        cubes = dataset.cube(N.asList("city", "age"), keyExtractor, N.asList("name"), "names", List.class).toList();

        cubes.forEach(Dataset::println);

        assertNotNull(cubes);
        assertEquals(4, cubes.size());

        dataset.groupBy(N.asList("city", "age"), keyExtractor, N.asList("name"), "names", List.class).println();
    }

    @Test
    public void testCubeWithKeyExtractorAndArrayCollector() {
        Function<DisposableObjArray, String> keyExtractor = arr -> arr.get(0).toString();
        Collector<Object[], ?, Long> countCollector = Collectors.counting();
        List<Dataset> cubes = dataset.cube(N.asList("city"), keyExtractor, N.asList("name"), "count", countCollector).toList();

        assertNotNull(cubes);
        assertTrue(cubes.size() > 0);
    }

    @Test
    public void testCubeWithKeyExtractorAndRowMapper() {
        Function<DisposableObjArray, String> keyExtractor = arr -> arr.get(0).toString();
        Function<DisposableObjArray, String> rowMapper = arr -> arr.get(0).toString();
        Collector<String, ?, List<String>> collector = Collectors.toList();
        List<Dataset> cubes = dataset.cube(N.asList("city"), keyExtractor, N.asList("name"), "names", rowMapper, collector).toList();

        assertNotNull(cubes);
        assertTrue(cubes.size() > 0);
    }

    @Test
    public void testSortBy() {
        Dataset copy = dataset.copy();
        copy.sortBy("age");

        List<Object> ages = copy.getColumn("age");
        for (int i = 1; i < ages.size(); i++) {
            assertTrue(((Integer) ages.get(i - 1)) <= ((Integer) ages.get(i)));
        }
    }

    @Test
    public void testSortByWithComparator() {
        Dataset copy = dataset.copy();
        copy.sortBy("age", Comparator.reverseOrder());

        List<Object> ages = copy.getColumn("age");
        for (int i = 1; i < ages.size(); i++) {
            assertTrue(((Integer) ages.get(i - 1)) >= ((Integer) ages.get(i)));
        }
    }

    @Test
    public void testSortByMultipleColumns() {
        Dataset copy = dataset.copy();
        copy.sortBy(N.asList("city", "age"));

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
        Dataset copy = dataset.copy();
        Comparator<Object[]> comp = (a, b) -> {
            int result = ((String) a[0]).compareTo((String) b[0]);
            if (result == 0) {
                result = ((Integer) b[1]).compareTo((Integer) a[1]);
            }
            return result;
        };
        copy.sortBy(N.asList("city", "age"), comp);

        assertNotNull(copy);
        assertEquals(dataset.size(), copy.size());
    }

    @Test
    public void testSortByWithKeyExtractor() {
        Dataset copy = dataset.copy();
        Function<DisposableObjArray, String> keyExtractor = arr -> arr.get(0) + "-" + arr.get(1);
        copy.sortBy(N.asList("name", "age"), keyExtractor);

        assertNotNull(copy);
        assertEquals(dataset.size(), copy.size());
    }

    @Test
    public void testParallelSortBy() {
        Dataset copy = dataset.copy();
        copy.parallelSortBy("age");

        List<Object> ages = copy.getColumn("age");
        for (int i = 1; i < ages.size(); i++) {
            assertTrue(((Integer) ages.get(i - 1)) <= ((Integer) ages.get(i)));
        }
    }

    @Test
    public void testParallelSortByWithComparator() {
        Dataset copy = dataset.copy();
        copy.parallelSortBy("name", String.CASE_INSENSITIVE_ORDER);

        List<Object> names = copy.getColumn("name");
        for (int i = 1; i < names.size(); i++) {
            assertTrue(names.get(i - 1).toString().compareToIgnoreCase(names.get(i).toString()) <= 0);
        }
    }

    @Test
    public void testParallelSortByMultipleColumns() {
        Dataset copy = dataset.copy();
        copy.parallelSortBy(N.asList("city", "name"));

        assertNotNull(copy);
        assertEquals(dataset.size(), copy.size());
    }

    @Test
    public void testParallelSortByMultipleColumnsWithComparator() {
        Dataset copy = dataset.copy();
        Comparator<Object[]> comp = Comparator.comparing((Object[] a) -> (String) a[0]).thenComparing(a -> (String) a[1]);
        copy.parallelSortBy(N.asList("city", "name"), comp);

        assertNotNull(copy);
        assertEquals(dataset.size(), copy.size());
    }

    @Test
    public void testParallelSortByWithKeyExtractor() {
        Dataset copy = dataset.copy();
        Function<DisposableObjArray, Integer> keyExtractor = arr -> (Integer) arr.get(0);
        copy.parallelSortBy(N.asList("age"), keyExtractor);

        List<Object> ages = copy.getColumn("age");
        for (int i = 1; i < ages.size(); i++) {
            assertTrue(((Integer) ages.get(i - 1)) <= ((Integer) ages.get(i)));
        }
    }

    @Test
    public void testTopBy() {
        Dataset top = dataset.topBy("age", 3);

        assertNotNull(top);
        assertEquals(3, top.size());
    }

    @Test
    public void testTopByWithComparator() {
        Dataset top = dataset.topBy("age", 2, Comparator.reverseOrder());

        assertNotNull(top);
        assertEquals(2, top.size());

        List<Object> ages = top.getColumn("age");
        assertTrue(ages.contains(25));
        assertTrue(ages.contains(28));
    }

    @Test
    public void testTopByMultipleColumns() {
        Dataset top = dataset.topBy(N.asList("city", "age"), 3);

        assertNotNull(top);
        assertEquals(3, top.size());
    }

    @Test
    public void testTopByMultipleColumnsWithComparator() {
        Comparator<Object[]> comp = (a, b) -> ((String) a[0]).compareTo((String) b[0]);
        Dataset top = dataset.topBy(N.asList("city"), 2, comp);

        assertNotNull(top);
        assertEquals(2, top.size());
    }

    @Test
    public void testTopByWithKeyExtractor() {
        Function<DisposableObjArray, Integer> keyExtractor = arr -> (Integer) arr.get(0);
        Dataset top = dataset.topBy(N.asList("age"), 3, keyExtractor);

        assertNotNull(top);
        assertEquals(3, top.size());
    }

    @Test
    public void testDistinct() {
        List<String> columnNames = N.asList("col1", "col2");
        List<List<Object>> columnValues = new ArrayList<>();
        columnValues.add(N.asList("A", "A", "B", "B", "A"));
        columnValues.add(N.asList(1, 1, 2, 2, 1));

        RowDataset ds = new RowDataset(columnNames, columnValues);
        Dataset distinct = ds.distinct();

        assertNotNull(distinct);
        assertEquals(2, distinct.size());
    }

    @Test
    public void testDistinctBy() {
        Dataset distinct = dataset.distinctBy("city");

        assertNotNull(distinct);
        assertEquals(3, distinct.size());
    }

    @Test
    public void testDistinctByWithKeyExtractor() {
        Function<Object, String> keyExtractor = obj -> obj.toString().substring(0, 1);
        Dataset distinct = dataset.distinctBy("name", keyExtractor);

        assertNotNull(distinct);
        assertTrue(distinct.size() <= dataset.size());
    }

    @Test
    public void testDistinctByMultipleColumns() {
        List<String> columnNames = N.asList("col1", "col2", "col3");
        List<List<Object>> columnValues = new ArrayList<>();
        columnValues.add(N.asList("A", "A", "B", "B", "A"));
        columnValues.add(N.asList(1, 2, 1, 1, 1));
        columnValues.add(N.asList("X", "Y", "Z", "W", "V"));

        RowDataset ds = new RowDataset(columnNames, columnValues);
        Dataset distinct = ds.distinctBy(N.asList("col1", "col2"));

        assertNotNull(distinct);
        assertEquals(3, distinct.size());
    }

    @Test
    public void testDistinctByMultipleColumnsWithKeyExtractor() {
        Function<DisposableObjArray, String> keyExtractor = arr -> arr.get(0).toString();
        Dataset distinct = dataset.distinctBy(N.asList("city", "age"), keyExtractor);

        assertNotNull(distinct);
        assertTrue(distinct.size() <= dataset.size());
    }

    @Test
    public void testFilterWithPredicate() {
        Predicate<DisposableObjArray> filter = arr -> ((Integer) arr.get(2)) > 30;
        Dataset filtered = dataset.filter(filter);

        assertNotNull(filtered);
        assertEquals(2, filtered.size());
    }

    @Test
    public void testFilterWithPredicateAndMax() {
        Predicate<DisposableObjArray> filter = arr -> ((Integer) arr.get(2)) > 25;
        Dataset filtered = dataset.filter(filter, 2);

        assertNotNull(filtered);
        assertEquals(2, filtered.size());
    }

    @Test
    public void testFilterWithRowRange() {
        Predicate<DisposableObjArray> filter = arr -> true;
        Dataset filtered = dataset.filter(1, 4, filter);

        assertNotNull(filtered);
        assertEquals(3, filtered.size());
    }

    @Test
    public void testFilterWithRowRangeAndMax() {
        Predicate<DisposableObjArray> filter = arr -> true;
        Dataset filtered = dataset.filter(0, 5, filter, 3);

        assertNotNull(filtered);
        assertEquals(3, filtered.size());
    }

    @Test
    public void testFilterByColumn() {
        Predicate<Object> filter = age -> ((Integer) age) >= 30;
        Dataset filtered = dataset.filter("age", filter);

        assertNotNull(filtered);
        assertEquals(3, filtered.size());
    }

    @Test
    public void testFilterByColumnWithMax() {
        Predicate<Object> filter = name -> ((String) name).startsWith("J");
        Dataset filtered = dataset.filter("name", filter, 1);

        assertNotNull(filtered);
        assertEquals(1, filtered.size());
    }

    @Test
    public void testFilterByColumnWithRowRange() {
        Predicate<Object> filter = city -> "NYC".equals(city);
        Dataset filtered = dataset.filter(0, 5, "city", filter);

        assertNotNull(filtered);
        assertEquals(2, filtered.size());
    }

    @Test
    public void testFilterByColumnWithRowRangeAndMax() {
        Predicate<Object> filter = city -> city != null;
        Dataset filtered = dataset.filter(1, 4, "city", filter, 2);

        assertNotNull(filtered);
        assertEquals(2, filtered.size());
    }

    @Test
    public void testFilterByMultipleColumns() {
        Predicate<DisposableObjArray> filter = arr -> "NYC".equals(arr.get(0)) && ((Integer) arr.get(1)) > 30;
        Dataset filtered = dataset.filter(N.asList("city", "age"), filter);

        assertNotNull(filtered);
        assertEquals(1, filtered.size());
    }

    @Test
    public void testFilterByMultipleColumnsWithMax() {
        Predicate<DisposableObjArray> filter = arr -> arr.get(0) != null;
        Dataset filtered = dataset.filter(N.asList("name", "city"), filter, 3);

        assertNotNull(filtered);
        assertEquals(3, filtered.size());
    }

    @Test
    public void testFilterByMultipleColumnsWithRowRange() {
        Predicate<DisposableObjArray> filter = arr -> true;
        Dataset filtered = dataset.filter(1, 3, N.asList("name", "age"), filter);

        assertNotNull(filtered);
        assertEquals(2, filtered.size());
    }

    @Test
    public void testFilterByMultipleColumnsWithRowRangeAndMax() {
        Predicate<DisposableObjArray> filter = arr -> true;
        Dataset filtered = dataset.filter(0, 5, N.asList("id", "name"), filter, 4);

        assertNotNull(filtered);
        assertEquals(4, filtered.size());
    }

    @Test
    public void testFilterByTuple2() {
        BiPredicate<Object, Object> filter = (name, age) -> ((Integer) age) > 30;
        Dataset filtered = dataset.filter(Tuple.of("name", "age"), filter);

        assertNotNull(filtered);
        assertEquals(2, filtered.size());
    }

    @Test
    public void testFilterByTuple2WithMax() {
        BiPredicate<Object, Object> filter = (id, name) -> ((Integer) id) <= 3;
        Dataset filtered = dataset.filter(Tuple.of("id", "name"), filter, 2);

        assertNotNull(filtered);
        assertEquals(2, filtered.size());
    }

    @Test
    public void testFilterByTuple2WithRowRange() {
        BiPredicate<Object, Object> filter = (name, city) -> "LA".equals(city);
        Dataset filtered = dataset.filter(1, 5, Tuple.of("name", "city"), filter);

        assertNotNull(filtered);
        assertEquals(2, filtered.size());
    }

    @Test
    public void testFilterByTuple2WithRowRangeAndMax() {
        BiPredicate<Object, Object> filter = (id, age) -> true;
        Dataset filtered = dataset.filter(0, 5, Tuple.of("id", "age"), filter, 3);

        assertNotNull(filtered);
        assertEquals(3, filtered.size());
    }

    @Test
    public void testFilterByTuple3() {
        TriPredicate<Object, Object, Object> filter = (id, name, age) -> ((Integer) age) < 30;
        Dataset filtered = dataset.filter(Tuple.of("id", "name", "age"), filter);

        assertNotNull(filtered);
        assertEquals(2, filtered.size());
    }

    @Test
    public void testFilterByTuple3WithMax() {
        TriPredicate<Object, Object, Object> filter = (name, age, city) -> true;
        Dataset filtered = dataset.filter(Tuple.of("name", "age", "city"), filter, 2);

        assertNotNull(filtered);
        assertEquals(2, filtered.size());
    }

    @Test
    public void testFilterByTuple3WithRowRange() {
        TriPredicate<Object, Object, Object> filter = (id, name, age) -> ((Integer) id) > 2;
        Dataset filtered = dataset.filter(1, 5, Tuple.of("id", "name", "age"), filter);
        filtered.println();

        assertNotNull(filtered);
        assertEquals(3, filtered.size());
    }

    @Test
    public void testFilterByTuple3WithRowRangeAndMax() {
        TriPredicate<Object, Object, Object> filter = (id, age, city) -> true;
        Dataset filtered = dataset.filter(0, 4, Tuple.of("id", "age", "city"), filter, 3);

        assertNotNull(filtered);
        assertEquals(3, filtered.size());
    }

    @Test
    public void testMapSingleColumn() {
        Function<Object, String> mapper = name -> ((String) name).toUpperCase();
        Dataset mapped = dataset.mapColumn("name", "NAME", "id", mapper);

        assertNotNull(mapped);
        assertEquals(5, mapped.size());
        assertTrue(mapped.containsColumn("NAME"));
        assertTrue(mapped.containsColumn("id"));
        assertEquals("JOHN", mapped.moveToRow(0).get("NAME"));
    }

    @Test
    public void testMapSingleColumnWithMultipleCopying() {
        Function<Object, Integer> mapper = age -> ((Integer) age) * 2;
        Dataset mapped = dataset.mapColumn("age", "doubleAge", N.asList("id", "name"), mapper);

        assertNotNull(mapped);
        assertEquals(5, mapped.size());
        assertTrue(mapped.containsColumn("doubleAge"));
        assertTrue(mapped.containsColumn("id"));
        assertTrue(mapped.containsColumn("name"));
        assertEquals(50, (Integer) mapped.moveToRow(0).get("doubleAge"));
    }

    @Test
    public void testMapTuple2() {
        BiFunction<Object, Object, String> mapper = (name, age) -> name + ":" + age;
        Dataset mapped = dataset.mapColumns(Tuple.of("name", "age"), "info", N.asList("id"), mapper);

        assertNotNull(mapped);
        assertEquals(5, mapped.size());
        assertTrue(mapped.containsColumn("info"));
        assertTrue(mapped.containsColumn("id"));
        assertEquals("John:25", mapped.moveToRow(0).get("info"));
    }

    @Test
    public void testMapTuple3() {
        TriFunction<Object, Object, Object, String> mapper = (id, name, age) -> id + "-" + name + "-" + age;
        Dataset mapped = dataset.mapColumns(Tuple.of("id", "name", "age"), "combined", N.asList("city"), mapper);

        assertNotNull(mapped);
        assertEquals(5, mapped.size());
        assertTrue(mapped.containsColumn("combined"));
        assertTrue(mapped.containsColumn("city"));
        assertEquals("1-John-25", mapped.moveToRow(0).get("combined"));
    }

    @Test
    public void testMapMultipleColumns() {
        Function<DisposableObjArray, String> mapper = arr -> arr.get(0) + ":" + arr.get(1);
        Dataset mapped = dataset.mapColumns(N.asList("name", "city"), "location", N.asList("id"), mapper);

        assertNotNull(mapped);
        assertEquals(5, mapped.size());
        assertTrue(mapped.containsColumn("location"));
        assertTrue(mapped.containsColumn("id"));
        assertEquals("John:NYC", mapped.moveToRow(0).get("location"));
    }

    @Test
    public void testFlatMapSingleColumn() {
        Function<Object, Collection<String>> mapper = name -> N.asList(((String) name).toLowerCase(), ((String) name).toUpperCase());
        Dataset flatMapped = dataset.flatMapColumn("name", "variations", "id", mapper);

        assertNotNull(flatMapped);
        assertEquals(10, flatMapped.size());
        assertTrue(flatMapped.containsColumn("variations"));
        assertTrue(flatMapped.containsColumn("id"));
    }

    @Test
    public void testFlatMapSingleColumnWithMultipleCopying() {
        Function<Object, Collection<Integer>> mapper = age -> N.asList((Integer) age, (Integer) age + 10);
        Dataset flatMapped = dataset.flatMapColumn("age", "ages", N.asList("id", "name"), mapper);

        assertNotNull(flatMapped);
        assertEquals(10, flatMapped.size());
        assertTrue(flatMapped.containsColumn("ages"));
        assertTrue(flatMapped.containsColumn("id"));
        assertTrue(flatMapped.containsColumn("name"));
    }

    @Test
    public void testFlatMapTuple2() {
        BiFunction<Object, Object, Collection<String>> mapper = (name, age) -> N.asList(name + "-young", name + "-old");
        Dataset flatMapped = dataset.flatMapColumns(Tuple.of("name", "age"), "status", N.asList("id"), mapper);

        assertNotNull(flatMapped);
        assertEquals(10, flatMapped.size());
        assertTrue(flatMapped.containsColumn("status"));
        assertTrue(flatMapped.containsColumn("id"));
    }

    @Test
    public void testFlatMapTuple3() {
        TriFunction<Object, Object, Object, Collection<String>> mapper = (id, name, age) -> N.asList("ID" + id, "NAME" + name, "AGE" + age);
        Dataset flatMapped = dataset.flatMapColumns(Tuple.of("id", "name", "age"), "tags", N.asList("city"), mapper);

        assertNotNull(flatMapped);
        assertEquals(15, flatMapped.size());
        assertTrue(flatMapped.containsColumn("tags"));
        assertTrue(flatMapped.containsColumn("city"));
    }

    @Test
    public void testFlatMapMultipleColumns() {
        Function<DisposableObjArray, Collection<String>> mapper = arr -> N.asList(arr.get(0).toString(), arr.get(1).toString());
        Dataset flatMapped = dataset.flatMapColumns(N.asList("name", "city"), "values", N.asList("id"), mapper);

        assertNotNull(flatMapped);
        assertEquals(10, flatMapped.size());
        assertTrue(flatMapped.containsColumn("values"));
        assertTrue(flatMapped.containsColumn("id"));
    }

    @Test
    public void testCopy() {
        Dataset copy = dataset.copy();

        assertNotNull(copy);
        assertEquals(dataset.size(), copy.size());
        assertEquals(dataset.columnCount(), copy.columnCount());
        assertNotSame(dataset, copy);
    }

    @Test
    public void testCopyWithColumns() {
        Dataset copy = dataset.copy(N.asList("id", "name"));

        assertNotNull(copy);
        assertEquals(dataset.size(), copy.size());
        assertEquals(2, copy.columnCount());
        assertTrue(copy.containsColumn("id"));
        assertTrue(copy.containsColumn("name"));
        assertFalse(copy.containsColumn("age"));
    }

    @Test
    public void testCopyWithRowRange() {
        Dataset copy = dataset.copy(1, 4);

        assertNotNull(copy);
        assertEquals(3, copy.size());
        assertEquals(dataset.columnCount(), copy.columnCount());
    }

    @Test
    public void testCopyWithRowRangeAndColumns() {
        Dataset copy = dataset.copy(1, 3, N.asList("name", "age"));

        assertNotNull(copy);
        assertEquals(2, copy.size());
        assertEquals(2, copy.columnCount());
        assertTrue(copy.containsColumn("name"));
        assertTrue(copy.containsColumn("age"));
    }

    @Test
    public void testClone() {
        Dataset cloned = dataset.clone();

        assertNotNull(cloned);
        assertEquals(dataset.size(), cloned.size());
        assertEquals(dataset.columnCount(), cloned.columnCount());
        assertNotSame(dataset, cloned);
    }

    @Test
    public void testCloneWithFreeze() {
        Dataset cloned = dataset.clone(true);

        assertNotNull(cloned);
        assertEquals(dataset.size(), cloned.size());
        assertEquals(dataset.columnCount(), cloned.columnCount());
        assertTrue(cloned.isFrozen());
    }

    @Test
    public void testInnerJoinSingleColumn() {
        List<String> rightColumns = N.asList("city", "country");
        List<List<Object>> rightValues = new ArrayList<>();
        rightValues.add(N.asList("NYC", "LA", "Chicago"));
        rightValues.add(N.asList("USA", "USA", "USA"));

        RowDataset right = new RowDataset(rightColumns, rightValues);

        Dataset joined = dataset.innerJoin(right, "city", "city");

        joined.println();

        assertNotNull(joined);
        assertEquals(5, joined.size());
        assertTrue(joined.containsColumn("country"));
    }

    @Test
    public void testInnerJoinMultipleColumns() {
        List<String> rightColumns = N.asList("city", "age", "salary");
        List<List<Object>> rightValues = new ArrayList<>();
        rightValues.add(N.asList("NYC", "LA", "NYC"));
        rightValues.add(N.asList(25, 30, 35));
        rightValues.add(N.asList(50000, 60000, 70000));

        RowDataset right = new RowDataset(rightColumns, rightValues);

        Map<String, String> onColumns = new HashMap<>();
        onColumns.put("city", "city");
        onColumns.put("age", "age");

        Dataset joined = dataset.innerJoin(right, onColumns);

        assertNotNull(joined);
        assertTrue(joined.containsColumn("salary"));
    }

    @Test
    public void testInnerJoinWithNewColumn() {
        List<String> rightColumns = N.asList("city", "info");
        List<List<Object>> rightValues = new ArrayList<>();
        rightValues.add(N.asList("NYC", "LA", "Chicago"));
        rightValues.add(N.asList("Big Apple", "City of Angels", "Windy City"));

        RowDataset right = new RowDataset(rightColumns, rightValues);

        Map<String, String> onColumns = new HashMap<>();
        onColumns.put("city", "city");

        Dataset joined = dataset.innerJoin(right, onColumns, "cityInfo", Object[].class);

        assertNotNull(joined);
        assertTrue(joined.containsColumn("cityInfo"));
    }

    @Test
    public void testInnerJoinWithCollectionSupplier() {
        List<String> rightColumns = N.asList("city", "tag");
        List<List<Object>> rightValues = new ArrayList<>();
        rightValues.add(N.asList("NYC", "NYC", "LA"));
        rightValues.add(N.asList("tag1", "tag2", "tag3"));

        RowDataset right = new RowDataset(rightColumns, rightValues);

        Map<String, String> onColumns = new HashMap<>();
        onColumns.put("city", "city");

        IntFunction<List<Object>> collSupplier = size -> new ArrayList<>(size);
        Dataset joined = dataset.innerJoin(right, onColumns, "tags", Object[].class, collSupplier);

        assertNotNull(joined);
        assertTrue(joined.containsColumn("tags"));
    }

    @Test
    public void testLeftJoinSingleColumn() {
        List<String> rightColumns = N.asList("city", "country");
        List<List<Object>> rightValues = new ArrayList<>();
        rightValues.add(N.asList("NYC", "LA"));
        rightValues.add(N.asList("USA", "USA"));

        RowDataset right = new RowDataset(rightColumns, rightValues);

        Dataset joined = dataset.leftJoin(right, "city", "city");

        assertNotNull(joined);
        assertEquals(5, joined.size());
        assertTrue(joined.containsColumn("country"));
    }

    @Test
    public void testLeftJoinMultipleColumns() {
        List<String> rightColumns = N.asList("city", "age", "bonus");
        List<List<Object>> rightValues = new ArrayList<>();
        rightValues.add(N.asList("NYC", "LA"));
        rightValues.add(N.asList(25, 30));
        rightValues.add(N.asList(1000, 2000));

        RowDataset right = new RowDataset(rightColumns, rightValues);

        Map<String, String> onColumns = new HashMap<>();
        onColumns.put("city", "city");
        onColumns.put("age", "age");

        Dataset joined = dataset.leftJoin(right, onColumns);

        assertNotNull(joined);
        assertEquals(5, joined.size());
    }

    @Test
    public void testLeftJoinWithNewColumn() {
        List<String> rightColumns = N.asList("city", "population");
        List<List<Object>> rightValues = new ArrayList<>();
        rightValues.add(N.asList("NYC", "LA"));
        rightValues.add(N.asList(8000000, 4000000));

        RowDataset right = new RowDataset(rightColumns, rightValues);

        Map<String, String> onColumns = new HashMap<>();
        onColumns.put("city", "city");

        Dataset joined = dataset.leftJoin(right, onColumns, "cityData", Object[].class);

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

        RowDataset right = new RowDataset(rightColumns, rightValues);

        Map<String, String> onColumns = new HashMap<>();
        onColumns.put("city", "city");

        IntFunction<Set<Object>> collSupplier = size -> new HashSet<>(size);
        Dataset joined = dataset.leftJoin(right, onColumns, "features", Object[].class, collSupplier);

        assertNotNull(joined);
        assertEquals(5, joined.size());
        assertTrue(joined.containsColumn("features"));
    }

    @Test
    public void testEmptyDatasetOperations() {
        RowDataset emptyDataset = new RowDataset(N.asList("col1", "col2"), N.asList(new ArrayList<>(), new ArrayList<>()));

        String xml = emptyDataset.toXml();
        assertNotNull(xml);
        assertTrue(xml.contains("<dataset>"));
        assertTrue(xml.contains("</dataset>"));

        String csv = emptyDataset.toCsv();
        assertNotNull(csv);
        assertTrue(csv.contains("\"col1\",\"col2\""));

        Dataset grouped = emptyDataset.groupBy("col1", "col2", "result", Collectors.toList());
        assertNotNull(grouped);
        assertEquals(0, grouped.size());

        Dataset filtered = emptyDataset.filter(arr -> true);
        assertNotNull(filtered);
        assertEquals(0, filtered.size());

        Dataset distinct = emptyDataset.distinct();
        assertNotNull(distinct);
        assertEquals(0, distinct.size());

        emptyDataset.sortBy("col1");
        assertEquals(0, emptyDataset.size());

        Dataset copy = emptyDataset.copy();
        assertNotNull(copy);
        assertEquals(0, copy.size());
    }

    @Test
    public void testNullHandling() {
        List<String> columnNames = N.asList("col1", "col2");
        List<List<Object>> columnValues = new ArrayList<>();
        columnValues.add(N.asList("A", null, "B"));
        columnValues.add(N.asList(1, 2, null));

        RowDataset ds = new RowDataset(columnNames, columnValues);

        String xml = ds.toXml();
        assertNotNull(xml);
        assertTrue(xml.contains("null"));

        String csv = ds.toCsv();
        assertNotNull(csv);

        Dataset filtered = ds.filter("col1", obj -> obj != null);
        assertEquals(2, filtered.size());

        Dataset grouped = ds.groupBy("col1", "col2", "values", Collectors.toList());
        assertNotNull(grouped);

        ds.sortBy("col1");
        assertEquals(3, ds.size());
    }

    @Test
    public void testLargeDatasetOperations() {
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

        RowDataset largeDs = new RowDataset(columnNames, columnValues);

        largeDs.parallelSortBy("value");
        List<Object> sortedValues = largeDs.getColumn("value");
        for (int i = 1; i < sortedValues.size(); i++) {
            assertTrue(((Integer) sortedValues.get(i - 1)) <= ((Integer) sortedValues.get(i)));
        }

        Dataset grouped = largeDs.groupBy("category", "value", "sum", Collectors.summingInt(o -> (Integer) o));
        assertEquals(10, grouped.size());

        Dataset top = largeDs.topBy("value", 10);
        assertEquals(10, top.size());
    }

    @Test
    public void testComplexJoinScenarios() {
        Map<String, String> onColumns = new HashMap<>();
        onColumns.put("city", "city");

        Dataset selfJoined = dataset.innerJoin(dataset, onColumns);
        assertNotNull(selfJoined);
        assertTrue(selfJoined.size() > 0);

        List<String> rightColumns = N.asList("city", "data");
        List<List<Object>> rightValues = new ArrayList<>();
        rightValues.add(N.asList("Paris", "London"));
        rightValues.add(N.asList("data1", "data2"));

        RowDataset noMatchRight = new RowDataset(rightColumns, rightValues);
        Dataset noMatchJoined = dataset.innerJoin(noMatchRight, "city", "city");
        assertEquals(0, noMatchJoined.size());

        Dataset leftJoinNoMatch = dataset.leftJoin(noMatchRight, "city", "city");
        assertEquals(5, leftJoinNoMatch.size());
    }

    @Test
    public void testColumnNameValidation() {
        assertThrows(IllegalArgumentException.class, () -> {
            dataset.sortBy("nonexistent");
        });

        assertThrows(IllegalArgumentException.class, () -> {
            dataset.filter("nonexistent", obj -> true);
        });

        assertThrows(IllegalArgumentException.class, () -> {
            dataset.groupBy("nonexistent", "name", "result", Collectors.toList());
        });

        assertThrows(IllegalArgumentException.class, () -> {
            dataset.distinctBy("nonexistent");
        });

        assertThrows(IllegalArgumentException.class, () -> {
            dataset.mapColumn("nonexistent", "new", "id", obj -> obj);
        });
    }

    @Test
    public void testRowIndexValidation() {
        assertThrows(IndexOutOfBoundsException.class, () -> {
            dataset.toXml(-1, 3);
        });

        assertThrows(IndexOutOfBoundsException.class, () -> {
            dataset.toXml(2, 10);
        });

        assertThrows(IndexOutOfBoundsException.class, () -> {
            dataset.toCsv(5, 3, N.asList("name"));
        });

        assertThrows(IndexOutOfBoundsException.class, () -> {
            dataset.filter(-1, 5, arr -> true);
        });

        assertThrows(IndexOutOfBoundsException.class, () -> {
            dataset.copy(0, 10);
        });
    }

    @Test
    public void testSpecialCharactersInData() {
        List<String> columnNames = N.asList("text", "value");
        List<List<Object>> columnValues = new ArrayList<>();
        columnValues.add(N.asList("Hello, World", "Test\"Quote", "Line\nBreak", "<tag>"));
        columnValues.add(N.asList(1, 2, 3, 4));

        RowDataset specialDs = new RowDataset(columnNames, columnValues);

        String xml = specialDs.toXml();
        assertNotNull(xml);
        assertTrue(xml.contains("&lt;tag&gt;") || xml.contains("&lt;"));

        String csv = specialDs.toCsv();
        assertNotNull(csv);
        assertTrue(csv.contains("\"Hello, World\"") || csv.contains("Hello, World"));
    }

    @Test
    public void testMultipleGroupByScenarios() {

        Dataset countGrouped = dataset.groupBy("city", "name", "count", Collectors.counting());
        assertNotNull(countGrouped);

        Dataset listGrouped = dataset.groupBy("city", "age", "ages", Collectors.toList());
        assertNotNull(listGrouped);

        Dataset avgGrouped = dataset.groupBy("city", "age", "avgAge", Collectors.averagingInt(o -> (Integer) o));
        assertNotNull(avgGrouped);

        Collector<Object, ?, String> joiningCollector = Collectors.mapping(Object::toString, Collectors.joining(","));
        Dataset joinedGrouped = dataset.groupBy("city", "name", "names", joiningCollector);
        assertNotNull(joinedGrouped);
        assertTrue(joinedGrouped.containsColumn("names"));
    }

    @Test
    public void testComplexFilterPredicates() {
        Predicate<DisposableObjArray> complexPredicate = arr -> {
            Integer age = (Integer) arr.get(2);
            String city = (String) arr.get(3);
            return age > 25 && age < 35 && ("NYC".equals(city) || "LA".equals(city));
        };

        Dataset filtered = dataset.filter(complexPredicate);
        assertNotNull(filtered);
        assertTrue(filtered.size() > 0);

        Predicate<DisposableObjArray> subsetPredicate = arr -> {
            String name = (String) arr.get(0);
            Integer age = (Integer) arr.get(1);
            return name.length() > 3 && age > 30;
        };

        Dataset subsetFiltered = dataset.filter(N.asList("name", "age"), subsetPredicate);
        assertNotNull(subsetFiltered);
    }

    @Test
    public void testMapWithDifferentDataTypes() {
        Function<Object, String> toStringMapper = obj -> "ID:" + obj;
        Dataset stringMapped = dataset.mapColumn("id", "stringId", N.asList("name"), toStringMapper);
        assertEquals("ID:1", stringMapped.moveToRow(0).get("stringId"));

        Function<Object, Double> doubleMapper = obj -> ((Integer) obj) * 1.5;
        Dataset doubleMapped = dataset.mapColumn("age", "adjustedAge", N.asList("name"), doubleMapper);
        assertEquals(37.5, doubleMapped.moveToRow(0).get("adjustedAge"));

        Function<DisposableObjArray, Map<String, Object>> mapMapper = arr -> {
            Map<String, Object> map = new HashMap<>();
            map.put("name", arr.get(0));
            map.put("age", arr.get(1));
            return map;
        };
        Dataset objectMapped = dataset.mapColumns(N.asList("name", "age"), "info", N.asList("id"), mapMapper);
        assertNotNull(objectMapped.moveToRow(0).get("info"));
        assertTrue(objectMapped.moveToRow(0).get("info") instanceof Map);
    }

    @Test
    public void testFlatMapEdgeCases() {
        Function<Object, Collection<String>> emptyMapper = obj -> Collections.emptyList();
        Dataset emptyFlatMapped = dataset.flatMapColumn("name", "empty", N.asList("id"), emptyMapper);
        assertEquals(0, emptyFlatMapped.size());

        Function<Object, Collection<Integer>> variableMapper = obj -> {
            int count = ((Integer) obj) % 3;
            List<Integer> result = new ArrayList<>();
            for (int i = 0; i < count; i++) {
                result.add(i);
            }
            return result;
        };
        Dataset variableFlatMapped = dataset.flatMapColumn("id", "values", N.asList("name"), variableMapper);
        assertNotNull(variableFlatMapped);

        Function<Object, Collection<String>> nullSafeMapper = obj -> obj == null ? Collections.emptyList() : N.asList(obj.toString());
        Dataset nullSafeFlatMapped = dataset.flatMapColumn("name", "safe", N.asList("id"), nullSafeMapper);
        assertEquals(5, nullSafeFlatMapped.size());
    }

    @Test
    public void testTopByEdgeCases() {
        Dataset allTop = dataset.topBy("age", 10);
        assertEquals(5, allTop.size());

        Dataset singleTop = dataset.topBy("age", 1);
        assertEquals(1, singleTop.size());

        List<String> columnNames = N.asList("id", "value");
        List<List<Object>> columnValues = new ArrayList<>();
        columnValues.add(N.asList(1, 2, 3, 4, 5));
        columnValues.add(N.asList(10, 10, 20, 20, 30));

        RowDataset tieDs = new RowDataset(columnNames, columnValues);
        Dataset topWithTies = tieDs.topBy("value", 3);
        assertEquals(3, topWithTies.size());
    }

    @Test
    public void testDistinctComplexKeys() {
        List<String> columnNames = N.asList("a", "b", "c");
        List<List<Object>> columnValues = new ArrayList<>();
        columnValues.add(N.asList(1, 1, 2, 2, 3));
        columnValues.add(N.asList("X", "X", "Y", "Y", "Z"));
        columnValues.add(N.asList(true, false, true, true, false));

        RowDataset complexDs = new RowDataset(columnNames, columnValues);

        Dataset distinctA = complexDs.distinctBy("a");
        assertEquals(3, distinctA.size());

        Dataset distinctAB = complexDs.distinctBy(N.asList("a", "b"));
        assertEquals(3, distinctAB.size());

        Dataset distinctAll = complexDs.distinct();
        assertEquals(4, distinctAll.size());

        Function<DisposableObjArray, String> compositeKeyExtractor = arr -> arr.get(0) + "-" + arr.get(1);
        Dataset distinctComposite = complexDs.distinctBy(N.asList("a", "b"), compositeKeyExtractor);
        assertEquals(3, distinctComposite.size());
    }

    @Test
    public void testXmlAndCsvWithEmptyColumns() {
        Collection<String> emptyColumns = Collections.emptyList();

        String xml = dataset.toXml(0, 2, emptyColumns);
        assertNotNull(xml);
        assertTrue(xml.contains("<dataset>"));
        assertTrue(xml.contains("</dataset>"));

        String csv = dataset.toCsv(0, 2, emptyColumns);
        assertNotNull(csv);
        assertEquals("", csv.trim());
    }

    @Test
    public void testParallelOperationsConsistency() {
        int size = 100;
        List<Object> values = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            values.add(i);
        }

        List<String> columnNames = N.asList("value");
        List<List<Object>> columnValues = N.asList(values);
        RowDataset largeDs = new RowDataset(columnNames, columnValues);

        Dataset seqCopy = largeDs.copy();
        Dataset parCopy = largeDs.copy();

        seqCopy.sortBy("value", Comparator.reverseOrder());
        parCopy.parallelSortBy("value", Comparator.reverseOrder());

        for (int i = 0; i < size; i++) {
            assertEquals((Object) seqCopy.moveToRow(i).get("value"), (Object) parCopy.moveToRow(i).get("value"));
        }
    }

}

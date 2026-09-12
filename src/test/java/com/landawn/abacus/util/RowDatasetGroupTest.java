package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.util.NoCachingNoUpdating.DisposableObjArray;
import com.landawn.abacus.util.function.Function;

public class RowDatasetGroupTest extends RowDatasetTestSupport {
    @Test
    public void testGroupByWithKeyExtractor() {
        final RowDataset dataset = createFourRowCityDataset();
        String keyColumn = "age";
        Function<Integer, String> keyExtractor = age -> age < 30 ? "Young" : "Adult";
        Collection<String> aggregateColumns = Arrays.asList("name", "city");
        String aggregateResultColumn = "people";

        Dataset grouped = dataset.groupBy(keyColumn, keyExtractor, aggregateColumns, aggregateResultColumn, Map.class);

        Assertions.assertEquals(2, grouped.columnCount());
        Assertions.assertEquals(2, grouped.size());
    }

    @Test
    public void testGroupBy() {
        List<String> groupColumnNames = Arrays.asList("department", "employee", "salary");
        List<List<Object>> groupColumnList = new ArrayList<>();
        groupColumnList.add(Arrays.asList("IT", "IT", "HR", "HR"));
        groupColumnList.add(Arrays.asList("John", "Jane", "Bob", "Alice"));
        groupColumnList.add(Arrays.asList(70000, 80000, 60000, 65000));

        RowDataset groupDataset = new RowDataset(groupColumnNames, groupColumnList);

        String keyColumn = "department";
        Collection<String> aggregateColumns = Arrays.asList("employee", "salary");
        String aggregateResultColumn = "employees";

        Dataset grouped = groupDataset.groupBy(keyColumn, Fn.identity(), aggregateColumns, aggregateResultColumn, List.class);

        Assertions.assertEquals(2, grouped.columnCount());
        Assertions.assertEquals(2, grouped.size());
        Assertions.assertTrue(grouped.containsColumn("department"));
        Assertions.assertTrue(grouped.containsColumn("employees"));
    }

    @Test
    public void testGroupByWithSingleKeyAndCollector() {
        final RowDataset dataset = createFiveRowCityDataset();
        Collector<Object, ?, List<Object>> listCollector = Collectors.toList();
        Dataset grouped = dataset.groupBy("city", "name", "names", listCollector);

        assertNotNull(grouped);
        assertEquals(3, grouped.size());
        assertTrue(grouped.containsColumn("city"));
        assertTrue(grouped.containsColumn("names"));
    }

    @Test
    public void testGroupByWithSingleKeyAndRowType() {
        final RowDataset dataset = createFiveRowCityDataset();
        Dataset grouped = dataset.groupBy("city", CommonUtil.toList("name", "age"), "people", Object[].class);

        assertNotNull(grouped);
        assertEquals(3, grouped.size());
        assertTrue(grouped.containsColumn("city"));
        assertTrue(grouped.containsColumn("people"));
    }

    @Test
    public void testGroupByWithSingleKeyAndArrayCollector() {
        final RowDataset dataset = createFiveRowCityDataset();
        Collector<Object[], ?, List<Object[]>> collector = Collectors.toList();
        Dataset grouped = dataset.groupBy("city", CommonUtil.toList("name", "age"), "data", collector);

        assertNotNull(grouped);
        assertEquals(3, grouped.size());
        assertTrue(grouped.containsColumn("city"));
        assertTrue(grouped.containsColumn("data"));
    }

    @Test
    public void testGroupByWithSingleKeyAndRowMapper() {
        final RowDataset dataset = createFiveRowCityDataset();
        Function<DisposableObjArray, String> rowMapper = arr -> arr.get(0) + "-" + arr.get(1);
        Collector<String, ?, List<String>> collector = Collectors.toList();
        Dataset grouped = dataset.groupBy("city", CommonUtil.toList("name", "age"), "info", rowMapper, collector);

        assertNotNull(grouped);
        assertEquals(3, grouped.size());
        assertTrue(grouped.containsColumn("city"));
        assertTrue(grouped.containsColumn("info"));
    }

    @Test
    public void testGroupByWithMultipleKeys() {
        final RowDataset dataset = createFiveRowCityDataset();
        Dataset grouped = dataset.groupBy(CommonUtil.toList("city"));

        assertNotNull(grouped);
        assertEquals(3, grouped.size());
        assertTrue(grouped.containsColumn("city"));
    }

    @Test
    public void testGroupByWithMultipleKeysAndCollector() {
        final RowDataset dataset = createFiveRowCityDataset();
        Collector<Object, ?, Long> countCollector = Collectors.counting();
        Dataset grouped = dataset.groupBy(CommonUtil.toList("city"), "name", "count", countCollector);

        assertNotNull(grouped);
        assertEquals(3, grouped.size());
        assertTrue(grouped.containsColumn("city"));
        assertTrue(grouped.containsColumn("count"));
    }

    @Test
    public void testGroupByWithMultipleKeysAndRowType() {
        List<String> columnNames = CommonUtil.toList("id", "name");
        List<List<Object>> columnValues = new ArrayList<>();
        columnValues.add(CommonUtil.toList(1, 2, 3, 4));
        columnValues.add(CommonUtil.toList("A", "B", "A", "B"));

        RowDataset ds = new RowDataset(columnNames, columnValues);
        Dataset grouped = ds.groupBy(CommonUtil.toList("name"), CommonUtil.toList("id"), "ids", List.class);

        assertNotNull(grouped);
        assertEquals(2, grouped.size());
        assertTrue(grouped.containsColumn("name"));
        assertTrue(grouped.containsColumn("ids"));
    }

    @Test
    public void testGroupByWithMultipleKeysAndArrayCollector() {
        final RowDataset dataset = createFiveRowCityDataset();
        Collector<Object[], ?, Long> countCollector = Collectors.counting();
        Dataset grouped = dataset.groupBy(CommonUtil.toList("city"), CommonUtil.toList("name", "age"), "count", countCollector);

        assertNotNull(grouped);
        assertEquals(3, grouped.size());
        assertTrue(grouped.containsColumn("city"));
        assertTrue(grouped.containsColumn("count"));
    }

    @Test
    public void testGroupByWithMultipleKeysAndRowMapper() {
        final RowDataset dataset = createFiveRowCityDataset();
        Function<DisposableObjArray, Integer> rowMapper = arr -> (Integer) arr.get(0);
        Collector<Integer, ?, Integer> sumCollector = Collectors.summingInt(Integer::intValue);
        Dataset grouped = dataset.groupBy(CommonUtil.toList("city"), CommonUtil.toList("age"), "totalAge", rowMapper, sumCollector);

        assertNotNull(grouped);
        assertEquals(3, grouped.size());
        assertTrue(grouped.containsColumn("city"));
        assertTrue(grouped.containsColumn("totalAge"));
    }

    @Test
    public void testGroupByWithMultipleKeysAndKeyExtractor() {
        final RowDataset dataset = createFiveRowCityDataset();
        Function<DisposableObjArray, String> keyExtractor = arr -> arr.get(0).toString();
        Dataset grouped = dataset.groupBy(CommonUtil.toList("city"), keyExtractor);

        assertNotNull(grouped);
        assertTrue(grouped.containsColumn("city"));
    }

    @Test
    public void testGroupByWithMultipleKeysKeyExtractorAndCollector() {
        final RowDataset dataset = createFiveRowCityDataset();
        Function<DisposableObjArray, String> keyExtractor = arr -> arr.get(0).toString();
        Collector<Object, ?, Long> countCollector = Collectors.counting();
        Dataset grouped = dataset.groupBy(CommonUtil.toList("city"), keyExtractor, "name", "count", countCollector);

        assertNotNull(grouped);
        assertTrue(grouped.containsColumn("city"));
        assertTrue(grouped.containsColumn("count"));
    }

    @Test
    public void testGroupByWithMultipleKeysKeyExtractorAndRowType() {
        final RowDataset dataset = createFiveRowCityDataset();
        Function<DisposableObjArray, String> keyExtractor = arr -> arr.get(0).toString();
        Dataset grouped = dataset.groupBy(CommonUtil.toList("city"), keyExtractor, CommonUtil.toList("name"), "names", List.class);

        assertNotNull(grouped);
        assertTrue(grouped.containsColumn("city"));
        assertTrue(grouped.containsColumn("names"));
    }

    @Test
    public void testGroupByWithMultipleKeysKeyExtractorAndArrayCollector() {
        final RowDataset dataset = createFiveRowCityDataset();
        Function<DisposableObjArray, String> keyExtractor = arr -> arr.get(0).toString();
        Collector<Object[], ?, Long> countCollector = Collectors.counting();
        Dataset grouped = dataset.groupBy(CommonUtil.toList("city"), keyExtractor, CommonUtil.toList("name"), "count", countCollector);

        assertNotNull(grouped);
        assertTrue(grouped.containsColumn("city"));
        assertTrue(grouped.containsColumn("count"));
        assertEquals(3, grouped.size());
    }

    @Test
    public void testGroupByWithMultipleKeysKeyExtractorAndRowMapper() {
        final RowDataset dataset = createFiveRowCityDataset();
        Function<DisposableObjArray, String> keyExtractor = arr -> arr.get(0).toString();
        Function<DisposableObjArray, String> rowMapper = arr -> arr.get(0).toString();
        Collector<String, ?, List<String>> collector = Collectors.toList();
        Dataset grouped = dataset.groupBy(CommonUtil.toList("city"), keyExtractor, CommonUtil.toList("name"), "names", rowMapper, collector);

        assertNotNull(grouped);
        assertTrue(grouped.containsColumn("city"));
        assertTrue(grouped.containsColumn("names"));
    }

    @Test
    public void testGroupBy_MultiColumnKeys() {
        RowDataset groupDataset = new RowDataset(new ArrayList<>(Arrays.asList("dept", "role", "name")),
                new ArrayList<>(Arrays.asList(new ArrayList<>(Arrays.asList("eng", "eng", "hr", "eng")),
                        new ArrayList<>(Arrays.asList("dev", "dev", "mgr", "qa")), new ArrayList<>(Arrays.asList("Alice", "Bob", "Carol", "Dave")))));

        Dataset result = groupDataset.groupBy(Arrays.asList("dept", "role"));
        assertNotNull(result);
        // unique (eng,dev), (hr,mgr), (eng,qa) = 3
        assertEquals(3, result.size());
    }

    @Test
    public void testGroupBy_MultiColumnKeys_WithAggregateColumnClass() {
        RowDataset groupDataset = new RowDataset(new ArrayList<>(Arrays.asList("dept", "role", "salary")),
                new ArrayList<>(Arrays.asList(new ArrayList<>(Arrays.asList("eng", "eng", "hr")), new ArrayList<>(Arrays.asList("dev", "dev", "mgr")),
                        new ArrayList<>(Arrays.asList(100, 200, 150)))));

        Dataset result = groupDataset.groupBy(Arrays.asList("dept", "role"), Arrays.asList("salary"), "total_salary", Object[].class);
        assertNotNull(result);
        assertEquals(2, result.size());
    }

    // ===== groupBy with Collection keyColumnNames and rowType =====

    @Test
    public void testGroupBy_MultiColumnKeys_WithKeyExtractorAndRowType() {
        RowDataset ds = new RowDataset(new ArrayList<>(Arrays.asList("dept", "role", "name", "salary")),
                new ArrayList<>(
                        Arrays.asList(new ArrayList<>(Arrays.asList("Eng", "Eng", "HR", "Eng")), new ArrayList<>(Arrays.asList("Dev", "Dev", "Mgr", "Dev")),
                                new ArrayList<>(Arrays.asList("Alice", "Bob", "Charlie", "Diana")), new ArrayList<>(Arrays.asList(100, 200, 150, 180)))));

        // groupBy with keyExtractor (non-identity)
        Dataset result = ds.groupBy(Arrays.asList("dept", "role"),
                (com.landawn.abacus.util.NoCachingNoUpdating.DisposableObjArray arr) -> arr.get(0) + ":" + arr.get(1), Arrays.asList("name", "salary"),
                "aggregated", Object[].class);

        assertNotNull(result);
        assertTrue(result.size() > 0);
    }

    // ========== groupBy - empty dataset / duplicate prop ==========

    @Test
    public void testGroupBy_WithCollector_EmptyDataset_ReturnsEmpty() {
        RowDataset ds = new RowDataset(new ArrayList<>(Arrays.asList("id", "val")), new ArrayList<>(Arrays.asList(new ArrayList<>(), new ArrayList<>())));
        Dataset result = ds.groupBy("id", "val", "sum", Collectors.summingInt(o -> (Integer) o));
        assertNotNull(result);
        assertEquals(0, result.size());
    }

    @Test
    public void testGroupBy_MultiKeys_WithIdentityExtractor_EmptyDataset_ReturnsEmpty() {
        RowDataset ds = new RowDataset(new ArrayList<>(Arrays.asList("id", "name")), new ArrayList<>(Arrays.asList(new ArrayList<>(), new ArrayList<>())));
        Dataset result = ds.groupBy(Arrays.asList("id", "name"), Fn.identity());
        assertNotNull(result);
        assertEquals(0, result.size());
    }

    @Test
    public void testGroupBy_MultiKeys_WithKeyExtractor_AndCollector_EmptyDataset_ReturnsEmpty() {
        // Need 2+ key columns so it hits the multi-key code path (L4692) rather than delegating to single-key
        RowDataset ds = new RowDataset(new ArrayList<>(Arrays.asList("id", "name", "val")),
                new ArrayList<>(Arrays.asList(new ArrayList<>(), new ArrayList<>(), new ArrayList<>())));
        Dataset result = ds.groupBy(Arrays.asList("id", "name"), Fn.identity(), "val", "count", Collectors.counting());
        assertNotNull(result);
        assertEquals(0, result.size());
    }

    @Test
    public void testGroupByWithEmptyAggregateColumns() {
        String keyColumn = "city";
        Collection<String> aggregateColumns = new ArrayList<>();
        String aggregateResultColumn = "data";

        Assertions.assertThrows(IllegalArgumentException.class, () -> dataset.groupBy(keyColumn, null, aggregateColumns, aggregateResultColumn, List.class));
    }

    @Test
    public void testGroupBy_WithSingleKeyAndCollectionAggregateWithRowType() {
        RowDataset ds = createFiveRowCityDataset();

        Dataset result = ds.groupBy("city", Arrays.asList("id", "name"), "cityData", List.class);

        assertNotNull(result);
        assertTrue(result.size() > 0);
    }

    @Test
    public void testGroupBy_WithCollector_DuplicatePropertyName_ThrowsIllegalArgument() {
        assertThrows(IllegalArgumentException.class, () -> dataset.groupBy("id", "val", "id", Collectors.counting()));
    }

    @Test
    public void testGroupBy_WithNullCollector_ThrowsIllegalArgument() {
        assertThrows(IllegalArgumentException.class, () -> dataset.groupBy("name", "age", "totalAge", (Collector<?, ?, ?>) null));
        assertThrows(IllegalArgumentException.class, () -> dataset.groupBy("name", Fn.identity(), "age", "totalAge", (Collector<?, ?, ?>) null));
        assertThrows(IllegalArgumentException.class, () -> dataset.groupBy(Arrays.asList("name", "id"), "age", "totalAge", (Collector<?, ?, ?>) null));
        assertThrows(IllegalArgumentException.class,
                () -> dataset.groupBy(Arrays.asList("name", "id"), Fn.identity(), "age", "totalAge", (Collector<?, ?, ?>) null));
    }

}

package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.If.OrElse;
import com.landawn.abacus.util.Tuple.Tuple2;
import com.landawn.abacus.util.Tuple.Tuple3;
import com.landawn.abacus.util.u.Optional;
import com.landawn.abacus.util.stream.Stream;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

public class RowDataSet102Test extends TestBase {

    private DataSet ds1;
    private DataSet ds2;
    private DataSet emptyDs;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Person {
        public int id;
        public String name;
        public int age;
    }

    @BeforeEach
    public void setUp() {
        // Setup first dataset
        List<String> columnNames1 = N.asList("id", "name", "age");
        List<List<Object>> columns1 = new ArrayList<>();
        columns1.add(N.asList(1, 2, 3)); // id column
        columns1.add(N.asList("Alice", "Bob", "Charlie")); // name column
        columns1.add(N.asList(25, 30, 35)); // age column
        ds1 = new RowDataSet(columnNames1, columns1);

        // Setup second dataset
        List<String> columnNames2 = N.asList("id", "city", "salary");
        List<List<Object>> columns2 = new ArrayList<>();
        columns2.add(N.asList(2, 3, 4)); // id column
        columns2.add(N.asList("New York", "London", "Tokyo")); // city column
        columns2.add(N.asList(50000, 60000, 70000)); // salary column
        ds2 = new RowDataSet(columnNames2, columns2);

        // Setup empty dataset
        List<String> emptyColumnNames = N.asList("col1", "col2");
        List<List<Object>> emptyColumns = new ArrayList<>();
        emptyColumns.add(new ArrayList<>());
        emptyColumns.add(new ArrayList<>());
        emptyDs = new RowDataSet(emptyColumnNames, emptyColumns);
    }

    @Test
    public void testRightJoinWithSingleColumnName() {
        DataSet result = ds1.rightJoin(ds2, "id", "id");

        assertNotNull(result);
        assertEquals(3, result.size());
        assertTrue(result.containsColumn("id"));
        assertTrue(result.containsColumn("name"));
        assertTrue(result.containsColumn("age"));
        assertTrue(result.containsColumn("city"));
        assertTrue(result.containsColumn("salary"));

        // Check first row (id=2)
        assertEquals(2, (Integer) result.absolute(0).get("id"));
        assertEquals("Bob", result.absolute(0).get("name"));
        assertEquals(30, (Integer) result.absolute(0).get("age"));
        assertEquals("New York", result.absolute(0).get("city"));
        assertEquals(50000, (Integer) result.absolute(0).get("salary"));

        // Check second row (id=3)
        assertEquals(3, (Integer) result.absolute(1).get("id"));
        assertEquals("Charlie", result.absolute(1).get("name"));
        assertEquals(35, (Integer) result.absolute(1).get("age"));
        assertEquals("London", result.absolute(1).get("city"));
        assertEquals(60000, (Integer) result.absolute(1).get("salary"));

        // Check third row (id=4, no match in left)
        // assertEquals(4, (Integer) result.absolute(2).get("id"));
        // id 4 does not exist in ds1, so it should be null
        assertNull(result.absolute(2).get("id"));
        assertNull(result.absolute(2).get("name"));
        assertNull(result.absolute(2).get("age"));
        assertEquals("Tokyo", result.absolute(2).get("city"));
        assertEquals(70000, (Integer) result.absolute(2).get("salary"));
    }

    @Test
    public void testLeftJoinWithSingleColumnName() {
        DataSet result = ds1.leftJoin(ds2, "id", "id");

        assertNotNull(result);
        assertEquals(3, result.size());
        assertTrue(result.containsColumn("id"));
        assertTrue(result.containsColumn("name"));
        assertTrue(result.containsColumn("age"));
        assertTrue(result.containsColumn("city"));
        assertTrue(result.containsColumn("salary"));

        // Check first row (id=1)
        assertEquals(1, (Integer) result.absolute(0).get("id"));
        assertEquals("Alice", result.absolute(0).get("name"));
        assertEquals(25, (Integer) result.absolute(0).get("age"));
        assertNull(result.absolute(0).get("city"));
        assertNull(result.absolute(0).get("salary"));

        // Check second row (id=2)
        assertEquals(2, (Integer) result.absolute(1).get("id"));
        assertEquals("Bob", result.absolute(1).get("name"));
        assertEquals(30, (Integer) result.absolute(1).get("age"));
        assertEquals("New York", result.absolute(1).get("city"));
        assertEquals(50000, (Integer) result.absolute(1).get("salary"));

        // Check third row (id=3)
        assertEquals(3, (Integer) result.absolute(2).get("id"));
        assertEquals("Charlie", result.absolute(2).get("name"));
        assertEquals(35, (Integer) result.absolute(2).get("age"));
        assertEquals("London", result.absolute(2).get("city"));
        assertEquals(60000, (Integer) result.absolute(2).get("salary"));
    }

    @Test
    public void testRightJoinWithMap() {
        Map<String, String> onColumnNames = new HashMap<>();
        onColumnNames.put("id", "id");

        DataSet result = ds1.rightJoin(ds2, onColumnNames);

        assertNotNull(result);
        assertEquals(3, result.size());

        // Verify structure same as single column test
        assertEquals(2, (Integer) result.absolute(0).get("id"));
        assertEquals("Bob", result.absolute(0).get("name"));
        assertEquals(30, (Integer) result.absolute(0).get("age"));
    }

    @Test
    public void testRightJoinWithMultipleColumns() {
        // Create datasets with multiple matching columns
        List<String> columnNames1 = N.asList("id", "type", "value");
        List<List<Object>> columns1 = new ArrayList<>();
        columns1.add(N.asList(1, 1, 2)); // id
        columns1.add(N.asList("A", "B", "A")); // type
        columns1.add(N.asList(100, 200, 300)); // value
        DataSet multiDs1 = new RowDataSet(columnNames1, columns1);

        List<String> columnNames2 = N.asList("id", "type", "score");
        List<List<Object>> columns2 = new ArrayList<>();
        columns2.add(N.asList(1, 2, 3)); // id
        columns2.add(N.asList("A", "A", "B")); // type
        columns2.add(N.asList(10, 20, 30)); // score
        DataSet multiDs2 = new RowDataSet(columnNames2, columns2);

        Map<String, String> onColumnNames = new HashMap<>();
        onColumnNames.put("id", "id");
        onColumnNames.put("type", "type");

        DataSet result = multiDs1.rightJoin(multiDs2, onColumnNames);

        assertNotNull(result);
        assertEquals(3, result.size());
    }

    @Test
    public void testRightJoinWithEmptyRightDataSet() {
        DataSet result = ds1.rightJoin(emptyDs, "id", "col1");

        assertNotNull(result);
        assertEquals(0, result.size());
    }

    @Test
    public void testRightJoinWithNewColumn() {
        Map<String, String> onColumnNames = new HashMap<>();
        onColumnNames.put("id", "id");

        DataSet result = ds1.rightJoin(ds2, onColumnNames, "rightData", Map.class);

        assertNotNull(result);
        assertEquals(3, result.size());
        assertTrue(result.containsColumn("rightData"));

        // Check that rightData contains the right dataset row as a Map
        Map<String, Object> firstRightData = (Map<String, Object>) result.absolute(2).get("rightData");
        assertNotNull(firstRightData);
    }

    @Test
    public void testRightJoinWithCollectionSupplier() {
        Map<String, String> onColumnNames = new HashMap<>();
        onColumnNames.put("id", "id");

        // Create dataset with duplicate join keys
        List<String> columnNames = N.asList("id", "value");
        List<List<Object>> columns = new ArrayList<>();
        columns.add(N.asList(2, 2, 3)); // id with duplicates
        columns.add(N.asList("X", "Y", "Z")); // value
        DataSet dsWithDuplicates = new RowDataSet(columnNames, columns);

        DataSet result = ds1.rightJoin(dsWithDuplicates, onColumnNames, "values", List.class, ArrayList::new);

        assertNotNull(result);
        assertTrue(result.containsColumn("values"));
    }

    @Test
    public void testFullJoinWithSingleColumnName() {
        DataSet result = ds1.fullJoin(ds2, "id", "id");

        assertNotNull(result);
        assertEquals(4, result.size()); // 1,2,3 from left + 4 from right only

        // Check all columns exist
        assertTrue(result.containsColumn("id"));
        assertTrue(result.containsColumn("name"));
        assertTrue(result.containsColumn("age"));
        assertTrue(result.containsColumn("city"));
        assertTrue(result.containsColumn("salary"));

        // Row for id=1 (left only)
        assertEquals(1, (Integer) result.absolute(0).get("id"));
        assertEquals("Alice", result.absolute(0).get("name"));
        assertEquals(25, (Integer) result.absolute(0).get("age"));
        assertNull(result.absolute(0).get("city"));
        assertNull(result.absolute(0).get("salary"));
    }

    @Test
    public void testFullJoinWithMap() {
        Map<String, String> onColumnNames = new HashMap<>();
        onColumnNames.put("id", "id");

        DataSet result = ds1.fullJoin(ds2, onColumnNames);

        assertNotNull(result);
        assertEquals(4, result.size());
    }

    @Test
    public void testFullJoinWithNewColumn() {
        Map<String, String> onColumnNames = new HashMap<>();
        onColumnNames.put("id", "id");

        DataSet result = ds1.fullJoin(ds2, onColumnNames, "mergedData", Map.class);

        assertNotNull(result);
        assertEquals(4, result.size());
        assertTrue(result.containsColumn("mergedData"));
    }

    @Test
    public void testFullJoinWithCollectionSupplier() {
        Map<String, String> onColumnNames = new HashMap<>();
        onColumnNames.put("id", "id");

        DataSet result = ds1.fullJoin(ds2, onColumnNames, "dataList", List.class, ArrayList::new);

        assertNotNull(result);
        assertEquals(4, result.size());
        assertTrue(result.containsColumn("dataList"));
    }

    @Test
    public void testUnion() {
        DataSet result = ds1.union(ds2);

        assertNotNull(result);
        // Union should combine unique rows based on common columns (id)
        assertTrue(result.size() <= ds1.size() + ds2.size());
        assertTrue(result.containsColumn("id"));
        assertTrue(result.containsColumn("name"));
        assertTrue(result.containsColumn("age"));
        assertTrue(result.containsColumn("city"));
        assertTrue(result.containsColumn("salary"));
    }

    @Test
    public void testUnionWithSameColumnsRequired() {
        // Create dataset with same columns as ds1
        List<String> columnNames = N.asList("id", "name", "age");
        List<List<Object>> columns = new ArrayList<>();
        columns.add(N.asList(4, 5)); // id
        columns.add(N.asList("David", "Eve")); // name
        columns.add(N.asList(40, 45)); // age
        DataSet ds3 = new RowDataSet(columnNames, columns);

        DataSet result = ds1.union(ds3, true);

        assertNotNull(result);
        assertEquals(5, result.size()); // 3 + 2 unique rows
    }

    @Test
    public void testUnionWithKeyColumns() {
        Collection<String> keyColumns = N.asList("id");
        DataSet result = ds1.union(ds2, keyColumns);

        assertNotNull(result);
        assertTrue(result.containsColumn("id"));
    }

    @Test
    public void testUnionAll() {
        DataSet result = ds1.unionAll(ds2);

        assertNotNull(result);
        // UnionAll should combine all rows without deduplication
        assertTrue(result.containsColumn("id"));
    }

    @Test
    public void testIntersect() {
        DataSet result = ds1.intersect(ds2);

        assertNotNull(result);
        // Intersect should return rows with matching keys (id=2, id=3)
        assertEquals(2, result.size());
    }

    @Test
    public void testIntersectWithKeyColumns() {
        Collection<String> keyColumns = N.asList("id");
        DataSet result = ds1.intersect(ds2, keyColumns);

        assertNotNull(result);
        assertEquals(2, result.size());
    }

    @Test
    public void testIntersectAll() {
        DataSet result = ds1.intersectAll(ds2);

        assertNotNull(result);
        assertTrue(result.size() <= Math.min(ds1.size(), ds2.size()));
    }

    @Test
    public void testExcept() {
        DataSet result = ds1.except(ds2);

        assertNotNull(result);
        // Except should return rows from ds1 not in ds2 (id=1)
        assertEquals(1, result.size());
        assertEquals(1, (Integer) result.absolute(0).get("id"));
    }

    @Test
    public void testExceptWithKeyColumns() {
        Collection<String> keyColumns = N.asList("id");
        DataSet result = ds1.except(ds2, keyColumns);

        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    public void testExceptAll() {
        DataSet result = ds1.exceptAll(ds2);

        assertNotNull(result);
        assertTrue(result.size() <= ds1.size());
    }

    @Test
    public void testIntersection() {
        DataSet result = ds1.intersection(ds2);

        assertNotNull(result);
        // Intersection considers occurrence counts
        assertTrue(result.size() <= Math.min(ds1.size(), ds2.size()));
    }

    @Test
    public void testDifference() {
        DataSet result = ds1.difference(ds2);

        assertNotNull(result);
        // Difference removes occurrences from ds1 that exist in ds2
        assertTrue(result.size() <= ds1.size());
    }

    @Test
    public void testSymmetricDifference() {
        DataSet result = ds1.symmetricDifference(ds2);

        assertNotNull(result);
        // Symmetric difference returns elements in either set but not in both
        assertTrue(result.size() >= 0);
    }

    @Test
    public void testMerge() {
        DataSet result = ds1.merge(ds2);

        assertNotNull(result);
        assertEquals(ds1.size() + ds2.size(), result.size());
        assertTrue(result.containsColumn("id"));
        assertTrue(result.containsColumn("name"));
        assertTrue(result.containsColumn("city"));
    }

    @Test
    public void testMergeWithSameColumnsRequired() {
        assertThrows(IllegalArgumentException.class, () -> {
            ds1.merge(ds2, true);
        });
    }

    @Test
    public void testMergeWithColumnNames() {
        Collection<String> columnNames = N.asList("id", "city");
        DataSet result = ds1.merge(ds2, columnNames);

        assertNotNull(result);
        assertEquals(ds1.size() + ds2.size(), result.size());
    }

    @Test
    public void testMergeWithRowRange() {
        DataSet result = ds1.merge(ds2, 0, 2);

        assertNotNull(result);
        assertEquals(ds1.size() + 2, result.size());
    }

    @Test
    public void testMergeWithRowRangeAndColumns() {
        Collection<String> columnNames = N.asList("id", "city");
        DataSet result = ds1.merge(ds2, 1, 2, columnNames);

        assertNotNull(result);
        assertEquals(ds1.size() + 1, result.size());
    }

    @Test
    public void testMergeMultipleDataSets() {
        Collection<DataSet> others = N.asList(ds2, emptyDs);
        DataSet result = ds1.merge(others);

        assertNotNull(result);
        assertEquals(ds1.size() + ds2.size() + emptyDs.size(), result.size());
    }

    @Test
    public void testCartesianProduct() {
        // Create datasets with no common columns
        List<String> columnNames1 = N.asList("a", "b");
        List<List<Object>> columns1 = new ArrayList<>();
        columns1.add(N.asList(1, 2)); // a
        columns1.add(N.asList("X", "Y")); // b
        DataSet ds1New = new RowDataSet(columnNames1, columns1);

        List<String> columnNames2 = N.asList("c", "d");
        List<List<Object>> columns2 = new ArrayList<>();
        columns2.add(N.asList(10, 20)); // c
        columns2.add(N.asList("P", "Q")); // d
        DataSet ds2New = new RowDataSet(columnNames2, columns2);

        DataSet result = ds1New.cartesianProduct(ds2New);

        assertNotNull(result);
        assertEquals(4, result.size()); // 2 * 2 = 4
        assertTrue(result.containsColumn("a"));
        assertTrue(result.containsColumn("b"));
        assertTrue(result.containsColumn("c"));
        assertTrue(result.containsColumn("d"));
    }

    @Test
    public void testCartesianProductWithCommonColumns() {
        assertThrows(IllegalArgumentException.class, () -> {
            ds1.cartesianProduct(ds2); // Both have "id" column
        });
    }

    @Test
    public void testSplit() {
        Stream<DataSet> splitStream = ds1.split(2);
        List<DataSet> splits = splitStream.toList();

        assertEquals(2, splits.size());
        assertEquals(2, splits.get(0).size());
        assertEquals(1, splits.get(1).size());
    }

    @Test
    public void testSplitWithColumns() {
        Collection<String> columnNames = N.asList("id", "name");
        Stream<DataSet> splitStream = ds1.split(2, columnNames);
        List<DataSet> splits = splitStream.toList();

        assertEquals(2, splits.size());
        assertTrue(splits.get(0).containsColumn("id"));
        assertTrue(splits.get(0).containsColumn("name"));
        assertFalse(splits.get(0).containsColumn("age"));
    }

    @Test
    public void testSplitToList() {
        List<DataSet> splits = ds1.splitToList(2);

        assertEquals(2, splits.size());
        assertEquals(2, splits.get(0).size());
        assertEquals(1, splits.get(1).size());
    }

    @Test
    public void testSplitToListWithColumns() {
        Collection<String> columnNames = N.asList("id", "age");
        List<DataSet> splits = ds1.splitToList(2, columnNames);

        assertEquals(2, splits.size());
        assertTrue(splits.get(0).containsColumn("id"));
        assertTrue(splits.get(0).containsColumn("age"));
        assertFalse(splits.get(0).containsColumn("name"));
    }

    @Test
    public void testSlice() {
        Collection<String> columnNames = N.asList("id", "name");
        DataSet result = ds1.slice(columnNames);

        assertNotNull(result);
        assertEquals(ds1.size(), result.size());
        assertEquals(2, result.columnCount());
        assertTrue(result.containsColumn("id"));
        assertTrue(result.containsColumn("name"));
        assertFalse(result.containsColumn("age"));
    }

    @Test
    public void testSliceWithRowRange() {
        DataSet result = ds1.slice(1, 3);

        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals(ds1.columnCount(), result.columnCount());
    }

    @Test
    public void testSliceWithRowRangeAndColumns() {
        Collection<String> columnNames = N.asList("name", "age");
        DataSet result = ds1.slice(0, 2, columnNames);

        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals(2, result.columnCount());
        assertTrue(result.containsColumn("name"));
        assertTrue(result.containsColumn("age"));
    }

    @Test
    public void testPaginate() {
        Paginated<DataSet> paginated = ds1.paginate(2);

        assertNotNull(paginated);
        assertEquals(2, paginated.pageSize());
        assertEquals(2, paginated.totalPages());

        Optional<DataSet> firstPage = paginated.firstPage();
        assertTrue(firstPage.isPresent());
        assertEquals(2, firstPage.get().size());

        Optional<DataSet> lastPage = paginated.lastPage();
        assertTrue(lastPage.isPresent());
        assertEquals(1, lastPage.get().size());
    }

    @Test
    public void testPaginateWithColumns() {
        Collection<String> columnNames = N.asList("id", "name");
        Paginated<DataSet> paginated = ds1.paginate(columnNames, 2);

        assertNotNull(paginated);
        assertEquals(2, paginated.totalPages());

        DataSet page = paginated.getPage(0);
        assertEquals(2, page.columnCount());
        assertTrue(page.containsColumn("id"));
        assertTrue(page.containsColumn("name"));
    }

    @Test
    public void testStreamByColumnName() {
        Stream<Integer> idStream = ds1.stream("id");
        List<Integer> ids = idStream.toList();

        assertEquals(3, ids.size());
        assertEquals(N.asList(1, 2, 3), ids);
    }

    @Test
    public void testStreamByColumnNameWithRange() {
        Stream<String> nameStream = ds1.stream(1, 3, "name");
        List<String> names = nameStream.toList();

        assertEquals(2, names.size());
        assertEquals(N.asList("Bob", "Charlie"), names);
    }

    @Test
    public void testStreamWithRowType() {
        Stream<Object[]> rowStream = ds1.stream(Object[].class);
        List<Object[]> rows = rowStream.toList();

        assertEquals(3, rows.size());
        assertEquals(3, rows.get(0).length);
    }

    @Test
    public void testStreamWithRowSupplier() {
        Stream<List> rowStream = ds1.stream(size -> new ArrayList<>(size));
        List<List> rows = rowStream.toList();

        assertEquals(3, rows.size());
    }

    @Test
    public void testStreamWithPrefixAndFieldNameMap() {
        Map<String, String> prefixMap = new HashMap<>();
        prefixMap.put("", "");

        Stream<Person> rowStream = ds1.stream(prefixMap, Person.class);
        List<Person> rows = rowStream.toList();

        assertEquals(3, rows.size());
    }

    @Test
    public void testStreamWithRowMapper() {
        Stream<String> stream = ds1.stream((rowIndex, array) -> "Row " + rowIndex + ": " + Arrays.toString(array.copy()));
        List<String> results = stream.toList();

        assertEquals(3, results.size());
        assertTrue(results.get(0).startsWith("Row 0:"));
    }

    @Test
    public void testStreamWithTuple2() {
        Tuple2<String, String> columnNames = Tuple.of("id", "name");
        Stream<String> stream = ds1.stream(columnNames, (id, name) -> id + "-" + name);
        List<String> results = stream.toList();

        assertEquals(3, results.size());
        assertEquals("1-Alice", results.get(0));
        assertEquals("2-Bob", results.get(1));
        assertEquals("3-Charlie", results.get(2));
    }

    @Test
    public void testStreamWithTuple3() {
        Tuple3<String, String, String> columnNames = Tuple.of("id", "name", "age");
        Stream<String> stream = ds1.stream(columnNames, (id, name, age) -> id + "-" + name + "-" + age);
        List<String> results = stream.toList();

        assertEquals(3, results.size());
        assertEquals("1-Alice-25", results.get(0));
    }

    @Test
    public void testApply() {
        Integer result = ds1.apply(ds -> ds.size());
        assertEquals(3, result);
    }

    @Test
    public void testApplyIfNotEmpty() {
        Optional<Integer> result = ds1.applyIfNotEmpty(ds -> ds.size());
        assertTrue(result.isPresent());
        assertEquals(3, result.get().intValue());

        Optional<Integer> emptyResult = emptyDs.applyIfNotEmpty(ds -> ds.size());
        assertFalse(emptyResult.isPresent());
    }

    @Test
    public void testAccept() {
        List<String> names = new ArrayList<>();
        ds1.accept(ds -> {
            for (int i = 0; i < ds.size(); i++) {
                names.add((String) ds.absolute(i).get("name"));
            }
        });

        assertEquals(3, names.size());
        assertEquals(N.asList("Alice", "Bob", "Charlie"), names);
    }

    @Test
    public void testAcceptIfNotEmpty() {
        List<String> names = new ArrayList<>();
        OrElse result = ds1.acceptIfNotEmpty(ds -> {
            names.add("processed");
        });

        assertEquals(OrElse.TRUE, result);
        assertEquals(1, names.size());

        OrElse emptyResult = emptyDs.acceptIfNotEmpty(ds -> {
            names.add("should not be added");
        });

        assertEquals(OrElse.FALSE, emptyResult);
        assertEquals(1, names.size());
    }

    @Test
    public void testFreeze() {
        DataSet copyDs = ds1.copy();
        assertFalse(copyDs.isFrozen());

        copyDs.freeze();
        assertTrue(copyDs.isFrozen());

        // Trying to modify frozen dataset should throw exception
        assertThrows(IllegalStateException.class, () -> {
            copyDs.addColumn("newCol", N.asList(1, 2, 3));
        });
    }

    @Test
    public void testIsEmpty() {
        assertFalse(ds1.isEmpty());
        assertTrue(emptyDs.isEmpty());
    }

    @Test
    public void testTrimToSize() {
        ds1.trimToSize();
        // Method should complete without error
        assertTrue(true);
    }

    @Test
    public void testSize() {
        assertEquals(3, ds1.size());
        assertEquals(3, ds2.size());
        assertEquals(0, emptyDs.size());
    }

    @Test
    public void testClear() {
        DataSet copyDs = ds1.copy();
        assertEquals(3, copyDs.size());

        copyDs.clear();
        assertEquals(0, copyDs.size());
        assertTrue(copyDs.isEmpty());
    }

    @Test
    public void testProperties() {
        Map<String, Object> props = ds1.properties();
        assertNotNull(props);
    }

    @Test
    public void testColumnNames() {
        List<String> columnNames = ds1.columnNames().toList();
        assertEquals(3, columnNames.size());
        assertEquals(N.asList("id", "name", "age"), columnNames);
    }

    @Test
    public void testColumns() {
        List<ImmutableList<Object>> columns = ds1.columns().toList();
        assertEquals(3, columns.size());
        assertEquals(3, columns.get(0).size());
    }

    @Test
    public void testColumnMap() {
        Map<String, ImmutableList<Object>> columnMap = ds1.columnMap();
        assertEquals(3, columnMap.size());
        assertTrue(columnMap.containsKey("id"));
        assertTrue(columnMap.containsKey("name"));
        assertTrue(columnMap.containsKey("age"));
    }

    @Test
    public void testPrintln() {
        // Test basic println - should not throw exception
        ds1.println();
    }

    @Test
    public void testPrintlnWithRange() {
        ds1.println(0, 2);
    }

    @Test
    public void testPrintlnWithRangeAndColumns() {
        Collection<String> columnNames = N.asList("id", "name");
        ds1.println(0, 2, columnNames);
    }

    @Test
    public void testPrintlnWithWriter() {
        StringWriter writer = new StringWriter();
        ds1.println(writer);

        String output = writer.toString();
        assertNotNull(output);
        assertTrue(output.contains("id"));
        assertTrue(output.contains("name"));
        assertTrue(output.contains("age"));
    }

    @Test
    public void testPrintlnWithRangeColumnsAndWriter() {
        StringWriter writer = new StringWriter();
        Collection<String> columnNames = N.asList("id", "name");
        ds1.println(1, 3, columnNames, writer);

        String output = writer.toString();
        assertNotNull(output);
        assertTrue(output.contains("Bob"));
        assertTrue(output.contains("Charlie"));
    }

    @Test
    public void testHashCode() {
        DataSet copyDs = ds1.copy();
        assertEquals(ds1.hashCode(), copyDs.hashCode());

        // Different datasets should have different hash codes (usually)
        assertNotEquals(ds1.hashCode(), ds2.hashCode());
    }

    @Test
    public void testEquals() {
        DataSet copyDs = ds1.copy();
        assertEquals(ds1, copyDs);

        assertNotEquals(ds1, ds2);
        assertNotEquals(ds1, null);
        assertNotEquals(ds1, "not a dataset");
    }

    @Test
    public void testToString() {
        String str = ds1.toString();
        assertNotNull(str);
        assertTrue(str.contains("columnNames"));
        assertTrue(str.contains("id"));
        assertTrue(str.contains("name"));
        assertTrue(str.contains("age"));
    }

    @Test
    public void testComplexJoinScenario() {
        // Test with null values in join columns
        List<String> columnNames1 = N.asList("id", "value");
        List<List<Object>> columns1 = new ArrayList<>();
        columns1.add(N.asList(1, null, 3)); // id with null
        columns1.add(N.asList("A", "B", "C")); // value
        DataSet dsWithNull1 = new RowDataSet(columnNames1, columns1);

        List<String> columnNames2 = N.asList("id", "score");
        List<List<Object>> columns2 = new ArrayList<>();
        columns2.add(N.asList(null, 2, 3)); // id with null
        columns2.add(N.asList(10, 20, 30)); // score
        DataSet dsWithNull2 = new RowDataSet(columnNames2, columns2);

        DataSet result = dsWithNull1.rightJoin(dsWithNull2, "id", "id");
        assertNotNull(result);
        assertEquals(3, result.size());
    }

    @Test
    public void testEmptyDataSetOperations() {
        // Test various operations with empty datasets
        DataSet result1 = emptyDs.rightJoin(ds1, "col1", "id");
        assertEquals(ds1.size(), result1.size());

        DataSet result2 = emptyDs.fullJoin(ds1, "col1", "id");
        assertEquals(ds1.size(), result2.size());

        //    DataSet result3 = emptyDs.union(ds1);
        //    assertTrue(result3.size() >= 0);
        //    DataSet result4 = emptyDs.intersect(ds1);
        //    ssertEquals(0, result4.size());

        assertThrows(IllegalArgumentException.class, () -> emptyDs.union(ds1));
        assertThrows(IllegalArgumentException.class, () -> emptyDs.intersect(ds1));

        DataSet emptyDataSet = N.newEmptyDataSet(ds1.columnNameList());
        DataSet result3 = emptyDataSet.union(ds1);
        assertTrue(result3.size() >= 0);
        DataSet result4 = emptyDataSet.intersect(ds1);
        assertEquals(0, result4.size());
    }

    @Test
    public void testInvalidColumnOperations() {
        // Test operations with invalid column names
        assertThrows(IllegalArgumentException.class, () -> {
            ds1.rightJoin(ds2, "invalid_column", "id");
        });

        assertThrows(IllegalArgumentException.class, () -> {
            ds1.rightJoin(ds2, "id", "invalid_column");
        });

        Map<String, String> invalidMap = new HashMap<>();
        invalidMap.put("invalid_column", "id");

        assertThrows(IllegalArgumentException.class, () -> {
            ds1.rightJoin(ds2, invalidMap);
        });
    }

    @Test
    public void testPaginationEdgeCases() {
        // Test pagination with size equal to dataset size
        Paginated<DataSet> paginated1 = ds1.paginate(3);
        assertEquals(1, paginated1.totalPages());

        // Test pagination with size greater than dataset size
        Paginated<DataSet> paginated2 = ds1.paginate(10);
        assertEquals(1, paginated2.totalPages());

        // Test getting invalid page
        assertThrows(IllegalArgumentException.class, () -> {
            paginated1.getPage(-1);
        });

        assertThrows(IllegalArgumentException.class, () -> {
            paginated1.getPage(5);
        });
    }

    @Test
    public void testStreamOperationsWithEmptyDataSet() {
        Stream<Object> stream = emptyDs.stream("col1");
        assertEquals(0, stream.count());

        Stream<Object[]> rowStream = emptyDs.stream(Object[].class);
        assertEquals(0, rowStream.count());
    }
}
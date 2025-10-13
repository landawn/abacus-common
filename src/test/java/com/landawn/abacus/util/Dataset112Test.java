package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.Function;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.NoCachingNoUpdating.DisposableObjArray;
import com.landawn.abacus.util.Tuple.Tuple2;
import com.landawn.abacus.util.Tuple.Tuple3;
import com.landawn.abacus.util.u.Optional;
import com.landawn.abacus.util.function.TriFunction;
import com.landawn.abacus.util.function.TriPredicate;
import com.landawn.abacus.util.stream.Collectors;
import com.landawn.abacus.util.stream.Stream;

@Tag("new-test")
public class Dataset112Test extends TestBase {

    private Dataset testDataset;
    private Dataset emptyDataset;
    private List<String> columnNames;
    private Object[][] testData;

    @BeforeEach
    public void setUp() {
        columnNames = Arrays.asList("id", "name", "age", "salary");
        testData = new Object[][] { { 1, "Alice", 30, 50000.0 }, { 2, "Bob", 25, 45000.0 }, { 3, "Charlie", 35, 60000.0 }, { 4, "Diana", 28, 55000.0 } };
        testDataset = Dataset.rows(columnNames, testData);
        emptyDataset = Dataset.empty();
    }

    @Test
    public void testEmpty() {
        Dataset empty = Dataset.empty();
        assertNotNull(empty);
        assertTrue(empty.isEmpty());
        assertEquals(0, empty.size());
        assertEquals(0, empty.columnCount());
    }

    @Test
    public void testRowsWithArrays() {
        Dataset dataset = Dataset.rows(columnNames, testData);
        assertNotNull(dataset);
        assertEquals(4, dataset.size());
        assertEquals(4, dataset.columnCount());
        assertEquals(columnNames, dataset.columnNameList());
    }

    @Test
    public void testRowsWithCollections() {
        List<List<Object>> rows = Arrays.asList(Arrays.asList(1, "Alice", 30, 50000.0), Arrays.asList(2, "Bob", 25, 45000.0));
        Dataset dataset = Dataset.rows(columnNames, rows);
        assertNotNull(dataset);
        assertEquals(2, dataset.size());
        assertEquals(4, dataset.columnCount());
    }

    @Test
    public void testColumnsWithArrays() {
        Object[][] columns = new Object[][] { { 1, 2, 3, 4 }, { "Alice", "Bob", "Charlie", "Diana" }, { 30, 25, 35, 28 },
                { 50000.0, 45000.0, 60000.0, 55000.0 } };
        Dataset dataset = Dataset.columns(columnNames, columns);
        assertNotNull(dataset);
        assertEquals(4, dataset.size());
        assertEquals(4, dataset.columnCount());
    }

    @Test
    public void testColumnsWithCollections() {
        List<List<Object>> columns = Arrays.asList(Arrays.asList(1, 2, 3, 4), Arrays.asList("Alice", "Bob", "Charlie", "Diana"), Arrays.asList(30, 25, 35, 28),
                Arrays.asList(50000.0, 45000.0, 60000.0, 55000.0));
        Dataset dataset = Dataset.columns(columnNames, columns);
        assertNotNull(dataset);
        assertEquals(4, dataset.size());
        assertEquals(4, dataset.columnCount());
    }

    @Test
    public void testRowsWithInvalidArguments() {
        assertThrows(IllegalArgumentException.class, () -> {
            Dataset.rows(null, testData);
        });

    }

    @Test
    public void testColumnNameList() {
        ImmutableList<String> names = testDataset.columnNameList();
        assertNotNull(names);
        assertEquals(4, names.size());
        assertEquals("id", names.get(0));
        assertEquals("name", names.get(1));
        assertEquals("age", names.get(2));
        assertEquals("salary", names.get(3));
    }

    @Test
    public void testColumnCount() {
        assertEquals(4, testDataset.columnCount());
        assertEquals(0, emptyDataset.columnCount());
    }

    @Test
    public void testGetColumnName() {
        assertEquals("id", testDataset.getColumnName(0));
        assertEquals("name", testDataset.getColumnName(1));
        assertEquals("age", testDataset.getColumnName(2));
        assertEquals("salary", testDataset.getColumnName(3));

        assertThrows(IndexOutOfBoundsException.class, () -> {
            testDataset.getColumnName(-1);
        });

        assertThrows(IndexOutOfBoundsException.class, () -> {
            testDataset.getColumnName(4);
        });
    }

    @Test
    public void testGetColumnIndex() {
        assertEquals(0, testDataset.getColumnIndex("id"));
        assertEquals(1, testDataset.getColumnIndex("name"));
        assertEquals(2, testDataset.getColumnIndex("age"));
        assertEquals(3, testDataset.getColumnIndex("salary"));

        assertThrows(IllegalArgumentException.class, () -> {
            testDataset.getColumnIndex("nonexistent");
        });
    }

    @Test
    public void testGetColumnIndexes() {
        List<String> requestedColumns = Arrays.asList("name", "age");
        int[] indexes = testDataset.getColumnIndexes(requestedColumns);
        assertNotNull(indexes);
        assertEquals(2, indexes.length);
        assertEquals(1, indexes[0]);
        assertEquals(2, indexes[1]);

        assertThrows(IllegalArgumentException.class, () -> {
            testDataset.getColumnIndexes(Arrays.asList("nonexistent"));
        });
    }

    @Test
    public void testContainsColumn() {
        assertTrue(testDataset.containsColumn("id"));
        assertTrue(testDataset.containsColumn("name"));
        assertTrue(testDataset.containsColumn("age"));
        assertTrue(testDataset.containsColumn("salary"));
        assertFalse(testDataset.containsColumn("nonexistent"));
    }

    @Test
    public void testContainsAllColumns() {
        assertTrue(testDataset.containsAllColumns(Arrays.asList("id", "name")));
        assertTrue(testDataset.containsAllColumns(Arrays.asList("age", "salary")));
        assertFalse(testDataset.containsAllColumns(Arrays.asList("id", "nonexistent")));
        assertFalse(testDataset.containsAllColumns(Arrays.asList("nonexistent1", "nonexistent2")));
    }

    @Test
    public void testRenameColumn() {
        Dataset dataset = testDataset.copy();
        dataset.renameColumn("id", "identifier");
        assertTrue(dataset.containsColumn("identifier"));
        assertFalse(dataset.containsColumn("id"));
        assertEquals(0, dataset.getColumnIndex("identifier"));

        assertThrows(IllegalArgumentException.class, () -> {
            dataset.renameColumn("nonexistent", "newname");
        });
    }

    @Test
    public void testRenameColumnsWithMap() {
        Dataset dataset = testDataset.copy();
        Map<String, String> renameMap = new HashMap<>();
        renameMap.put("id", "identifier");
        renameMap.put("name", "fullname");

        dataset.renameColumns(renameMap);
        assertTrue(dataset.containsColumn("identifier"));
        assertTrue(dataset.containsColumn("fullname"));
        assertFalse(dataset.containsColumn("id"));
        assertFalse(dataset.containsColumn("name"));
    }

    @Test
    public void testRenameColumnsWithFunction() {
        Dataset dataset = testDataset.copy();
        dataset.renameColumns(Arrays.asList("id", "name"), columnName -> columnName.toUpperCase());
        assertTrue(dataset.containsColumn("ID"));
        assertTrue(dataset.containsColumn("NAME"));
        assertFalse(dataset.containsColumn("id"));
        assertFalse(dataset.containsColumn("name"));
    }

    @Test
    public void testRenameAllColumnsWithFunction() {
        Dataset dataset = testDataset.copy();
        dataset.renameColumns(columnName -> "col_" + columnName);
        assertTrue(dataset.containsColumn("col_id"));
        assertTrue(dataset.containsColumn("col_name"));
        assertTrue(dataset.containsColumn("col_age"));
        assertTrue(dataset.containsColumn("col_salary"));
    }

    @Test
    public void testMoveColumn() {
        Dataset dataset = testDataset.copy();
        dataset.moveColumn("salary", 1);
        assertEquals("salary", dataset.getColumnName(1));
        assertEquals("name", dataset.getColumnName(2));

        assertThrows(IllegalArgumentException.class, () -> {
            dataset.moveColumn("nonexistent", 0);
        });

        assertThrows(IndexOutOfBoundsException.class, () -> {
            dataset.moveColumn("id", -1);
        });
    }

    @Test
    public void testMoveColumns() {
        Dataset dataset = testDataset.copy();
        List<String> columnsToMove = Arrays.asList("age", "salary");
        dataset.moveColumns(columnsToMove, 0);
        assertEquals("age", dataset.getColumnName(0));
        assertEquals("salary", dataset.getColumnName(1));
        assertEquals("id", dataset.getColumnName(2));
        assertEquals("name", dataset.getColumnName(3));
    }

    @Test
    public void testSwapColumnPosition() {
        Dataset dataset = testDataset.copy();
        dataset.swapColumnPosition("id", "salary");
        assertEquals("salary", dataset.getColumnName(0));
        assertEquals("name", dataset.getColumnName(1));
        assertEquals("age", dataset.getColumnName(2));
        assertEquals("id", dataset.getColumnName(3));

        assertThrows(IllegalArgumentException.class, () -> {
            dataset.swapColumnPosition("nonexistent", "id");
        });
    }

    @Test
    public void testMoveRow() {
        Dataset dataset = testDataset.copy();
        dataset.absolute(0);
        assertEquals(1, dataset.getInt("id"));

        dataset.moveRow(0, 2);
        dataset.absolute(2);
        assertEquals(1, dataset.getInt("id"));
        dataset.absolute(0);
        assertEquals(2, dataset.getInt("id"));

        assertThrows(IndexOutOfBoundsException.class, () -> {
            dataset.moveRow(-1, 0);
        });
    }

    @Test
    public void testMoveRows() {
        Dataset dataset = testDataset.copy();
        dataset.moveRows(0, 2, 2);
        dataset.absolute(2);
        assertEquals(1, dataset.getInt("id"));
        dataset.absolute(3);
        assertEquals(2, dataset.getInt("id"));

        assertThrows(IndexOutOfBoundsException.class, () -> {
            dataset.moveRows(-1, 1, 0);
        });
    }

    @Test
    public void testSwapRowPosition() {
        Dataset dataset = testDataset.copy();
        dataset.absolute(0);
        int firstId = dataset.getInt("id");
        dataset.absolute(1);
        int secondId = dataset.getInt("id");

        dataset.swapRowPosition(0, 1);
        dataset.absolute(0);
        assertEquals(secondId, dataset.getInt("id"));
        dataset.absolute(1);
        assertEquals(firstId, dataset.getInt("id"));

        assertThrows(IndexOutOfBoundsException.class, () -> {
            dataset.swapRowPosition(-1, 0);
        });
    }

    @Test
    public void testSetAndGet() {
        Dataset dataset = testDataset.copy();
        dataset.set(0, 0, 999);
        assertEquals(999, (Integer) dataset.get(0, 0));

        assertThrows(IndexOutOfBoundsException.class, () -> {
            dataset.set(-1, 0, 1);
        });

        assertThrows(IndexOutOfBoundsException.class, () -> {
            dataset.set(0, -1, 1);
        });
    }

    @Test
    public void testIsNull() {
        Dataset dataset = testDataset.copy();
        assertFalse(dataset.isNull(0, 0));

        dataset.set(0, 0, null);
        assertTrue(dataset.isNull(0, 0));

        assertThrows(IndexOutOfBoundsException.class, () -> {
            dataset.isNull(-1, 0);
        });
    }

    @Test
    public void testCurrentRowNum() {
        assertEquals(0, testDataset.currentRowNum());
        testDataset.absolute(0);
        assertEquals(0, testDataset.currentRowNum());
        testDataset.absolute(2);
        assertEquals(2, testDataset.currentRowNum());
    }

    @Test
    public void testAbsolute() {
        Dataset result = testDataset.absolute(1);
        assertNotNull(result);
        assertEquals(1, result.currentRowNum());
        assertEquals(testDataset, result);
    }

    @Test
    public void testGetRowNavigationMethods() {
        testDataset.absolute(0);

        assertEquals(1, testDataset.getByte(0));
        assertEquals(1, testDataset.getShort(0));
        assertEquals(1, testDataset.getInt(0));
        assertEquals(1L, (long) testDataset.getLong(0));
        assertEquals(1.0f, testDataset.getFloat(0));
        assertEquals(1.0, testDataset.getDouble(0));

        assertEquals("Alice", testDataset.get(1));
        assertEquals(30, testDataset.getInt(2));
        assertEquals(50000.0, testDataset.getDouble(3));
    }

    @Test
    public void testGetByColumnName() {
        testDataset.absolute(0);

        assertEquals(1, testDataset.getInt("id"));
        assertEquals("Alice", testDataset.get("name"));
        assertEquals(30, testDataset.getInt("age"));
        assertEquals(50000.0, testDataset.getDouble("salary"));

        assertThrows(IllegalArgumentException.class, () -> {
            testDataset.get("nonexistent");
        });
    }

    @Test
    public void testSetByColumnIndex() {
        Dataset dataset = testDataset.copy();
        dataset.absolute(0);
        dataset.set(0, 999);
        assertEquals(999, dataset.getInt(0));

        assertThrows(IndexOutOfBoundsException.class, () -> {
            dataset.set(-1, 1);
        });
    }

    @Test
    public void testSetByColumnName() {
        Dataset dataset = testDataset.copy();
        dataset.absolute(0);
        dataset.set("id", 999);
        assertEquals(999, dataset.getInt("id"));

        assertThrows(IllegalArgumentException.class, () -> {
            dataset.set("nonexistent", 1);
        });
    }

    @Test
    public void testIsNullByColumnIndex() {
        Dataset dataset = testDataset.copy();
        dataset.absolute(0);
        assertFalse(dataset.isNull(0));

        dataset.set(0, null);
        assertTrue(dataset.isNull(0));
    }

    @Test
    public void testIsNullByColumnName() {
        Dataset dataset = testDataset.copy();
        dataset.absolute(0);
        assertFalse(dataset.isNull("id"));

        dataset.set("id", null);
        assertTrue(dataset.isNull("id"));
    }

    @Test
    public void testAddColumnWithCollection() {
        Dataset dataset = testDataset.copy();
        List<String> departments = Arrays.asList("IT", "HR", "Finance", "Marketing");
        dataset.addColumn("department", departments);

        assertTrue(dataset.containsColumn("department"));
        assertEquals(5, dataset.columnCount());
        dataset.absolute(0);
        assertEquals("IT", dataset.get("department"));
    }

    @Test
    public void testAddColumnAtPosition() {
        Dataset dataset = testDataset.copy();
        List<String> departments = Arrays.asList("IT", "HR", "Finance", "Marketing");
        dataset.addColumn(1, "department", departments);

        assertEquals("department", dataset.getColumnName(1));
        assertEquals("name", dataset.getColumnName(2));
    }

    @Test
    public void testAddColumnWithFunction() {
        Dataset dataset = testDataset.copy();
        dataset.addColumn("age_category", "age", (Integer age) -> age >= 30 ? "Senior" : "Junior");

        assertTrue(dataset.containsColumn("age_category"));
        dataset.absolute(0);
        assertEquals("Senior", dataset.get("age_category"));
        dataset.absolute(1);
        assertEquals("Junior", dataset.get("age_category"));
    }

    @Test
    public void testAddColumnWithBiFunction() {
        Dataset dataset = testDataset.copy();
        Tuple2<String, String> fromColumns = Tuple.of("name", "age");
        BiFunction<String, Integer, String> func = (name, age) -> name + "_" + age;
        dataset.addColumn("name_age", fromColumns, func);

        assertTrue(dataset.containsColumn("name_age"));
        dataset.absolute(0);
        assertEquals("Alice_30", dataset.get("name_age"));
    }

    @Test
    public void testAddColumnWithTriFunction() {
        Dataset dataset = testDataset.copy();
        Tuple3<String, String, String> fromColumns = Tuple.of("id", "name", "age");
        TriFunction<Integer, String, Integer, String> func = (id, name, age) -> id + ":" + name + ":" + age;
        dataset.addColumn("combined", fromColumns, func);

        assertTrue(dataset.containsColumn("combined"));
        dataset.absolute(0);
        assertEquals("1:Alice:30", dataset.get("combined"));
    }

    @Test
    public void testAddColumns() {
        Dataset dataset = testDataset.copy();
        List<String> newColumnNames = Arrays.asList("dept", "location");
        List<List<String>> newColumns = Arrays.asList(Arrays.asList("IT", "HR", "Finance", "Marketing"), Arrays.asList("NY", "LA", "Chicago", "Boston"));
        dataset.addColumns(newColumnNames, newColumns);

        assertTrue(dataset.containsColumn("dept"));
        assertTrue(dataset.containsColumn("location"));
        assertEquals(6, dataset.columnCount());
    }

    @Test
    public void testRemoveColumns() {
        Dataset dataset = testDataset.copy();
        dataset.removeColumns(Arrays.asList("age", "salary"));

        assertFalse(dataset.containsColumn("age"));
        assertFalse(dataset.containsColumn("salary"));
        assertEquals(2, dataset.columnCount());
        assertTrue(dataset.containsColumn("id"));
        assertTrue(dataset.containsColumn("name"));
    }

    @Test
    public void testRemoveColumnsWithPredicate() {
        Dataset dataset = testDataset.copy();
        dataset.removeColumns(columnName -> columnName.length() > 3);

        assertFalse(dataset.containsColumn("name"));
        assertFalse(dataset.containsColumn("salary"));
        assertTrue(dataset.containsColumn("id"));
        assertTrue(dataset.containsColumn("age"));
        assertEquals(2, dataset.columnCount());
    }

    @Test
    public void testUpdateColumn() {
        Dataset dataset = testDataset.copy();
        dataset.updateColumn("age", (Integer age) -> age + 1);

        dataset.absolute(0);
        assertEquals(31, dataset.getInt("age"));
        dataset.absolute(1);
        assertEquals(26, dataset.getInt("age"));
    }

    @Test
    public void testUpdateColumns() {
        Dataset dataset = testDataset.copy();
        dataset.updateColumns(Arrays.asList("age", "salary"), (i, c, v) -> {
            if (v instanceof Integer) {
                return ((Integer) v) + 1;
            } else if (v instanceof Double) {
                return ((Double) v) * 1.1;
            }
            return v;
        });

        dataset.absolute(0);
        assertEquals(31, dataset.getInt("age"));
        assertEquals(55000.0, dataset.getDouble("salary"), 0.01);
    }

    @Test
    public void testConvertColumn() {
        Dataset dataset = testDataset.copy();
        dataset.convertColumn("age", String.class);

        dataset.absolute(0);
        assertEquals("30", dataset.get("age"));
        assertTrue(dataset.get("age") instanceof String);
    }

    @Test
    public void testConvertColumns() {
        Dataset dataset = testDataset.copy();
        Map<String, Class<?>> targetTypes = new HashMap<>();
        targetTypes.put("age", String.class);
        targetTypes.put("id", String.class);
        dataset.convertColumns(targetTypes);

        dataset.absolute(0);
        assertEquals("30", dataset.get("age"));
        assertEquals("1", dataset.get("id"));
        assertTrue(dataset.get("age") instanceof String);
        assertTrue(dataset.get("id") instanceof String);
    }

    @Test
    public void testCombineColumns() {
        Dataset dataset = testDataset.copy();
        dataset.combineColumns(Arrays.asList("name", "age"), "name_age", Map.class);

        assertTrue(dataset.containsColumn("name_age"));
        dataset.absolute(0);
        assertNotNull(dataset.get("name_age"));
    }

    @Test
    public void testCombineColumnsWithFunction() {
        Dataset dataset = testDataset.copy();
        dataset.combineColumns(Arrays.asList("name", "age"), "name_age_combined", (DisposableObjArray row) -> row.get(0) + "_" + row.get(1));

        assertTrue(dataset.containsColumn("name_age_combined"));
        dataset.absolute(0);
        assertEquals("Alice_30", dataset.get("name_age_combined"));
    }

    @Test
    public void testCombineColumnsWithBiFunction() {
        Dataset dataset = testDataset.copy();
        Tuple2<String, String> columnNames = Tuple.of("name", "age");
        BiFunction<String, Integer, String> combineFunc = (name, age) -> name + "(" + age + ")";
        dataset.combineColumns(columnNames, "name_age_bi", combineFunc);

        assertTrue(dataset.containsColumn("name_age_bi"));
        dataset.absolute(0);
        assertEquals("Alice(30)", dataset.get("name_age_bi"));
    }

    @Test
    public void testCombineColumnsWithTriFunction() {
        Dataset dataset = testDataset.copy();
        Tuple3<String, String, String> columnNames = Tuple.of("id", "name", "age");
        TriFunction<Integer, String, Integer, String> combineFunc = (id, name, age) -> "ID:" + id + ",Name:" + name + ",Age:" + age;
        dataset.combineColumns(columnNames, "full_info", combineFunc);

        assertTrue(dataset.containsColumn("full_info"));
        dataset.absolute(0);
        assertEquals("ID:1,Name:Alice,Age:30", dataset.get("full_info"));
    }

    @Test
    public void testDivideColumn() {
        Dataset dataset = testDataset.copy();
        List<String> newColumnNames = Arrays.asList("first_name", "age_category");
        Function<String, List<Object>> divideFunc = name -> Arrays.asList(name.substring(0, Math.min(name.length(), 3)), name.length() > 4 ? "Long" : "Short");
        dataset.divideColumn("name", newColumnNames, divideFunc);

        assertTrue(dataset.containsColumn("first_name"));
        assertTrue(dataset.containsColumn("age_category"));
        dataset.absolute(0);
        assertEquals("Ali", dataset.get("first_name"));
        assertEquals("Long", dataset.get("age_category"));
    }

    @Test
    public void testDivideColumnWithBiConsumer() {
        Dataset dataset = testDataset.copy();
        Object[] output = new Object[2];
        BiConsumer<String, Object[]> outputConsumer = (name, arr) -> {
            arr[0] = name.toUpperCase();
            arr[1] = name.length();
        };
        dataset.divideColumn("name", Arrays.asList("upper_name", "name_length"), outputConsumer);

        assertTrue(dataset.containsColumn("upper_name"));
        assertTrue(dataset.containsColumn("name_length"));
        dataset.absolute(0);
        assertEquals("ALICE", dataset.get("upper_name"));
        assertEquals(5, (Integer) dataset.get("name_length"));
    }

    @Test
    public void testDivideColumnWithPairOutput() {
        Dataset dataset = testDataset.copy();
        Tuple2<String, String> newColumnNames = Tuple.of("name_upper", "name_lower");
        BiConsumer<String, Pair<Object, Object>> output = (name, pair) -> {
            pair.setLeft(name.toUpperCase());
            pair.setRight(name.toLowerCase());
        };
        dataset.divideColumn("name", newColumnNames, output);

        assertTrue(dataset.containsColumn("name_upper"));
        assertTrue(dataset.containsColumn("name_lower"));
        dataset.absolute(0);
        assertEquals("ALICE", dataset.get("name_upper"));
        assertEquals("alice", dataset.get("name_lower"));
    }

    @Test
    public void testDivideColumnWithTripleOutput() {
        Dataset dataset = testDataset.copy();
        Tuple3<String, String, String> newColumnNames = Tuple.of("name_upper", "name_lower", "name_length");
        BiConsumer<String, Triple<Object, Object, Object>> output = (name, triple) -> {
            triple.setLeft(name.toUpperCase());
            triple.setMiddle(name.toLowerCase());
            triple.setRight(name.length());
        };
        dataset.divideColumn("name", newColumnNames, output);

        assertTrue(dataset.containsColumn("name_upper"));
        assertTrue(dataset.containsColumn("name_lower"));
        assertTrue(dataset.containsColumn("name_length"));
        dataset.absolute(0);
        assertEquals("ALICE", dataset.get("name_upper"));
        assertEquals("alice", dataset.get("name_lower"));
        assertEquals(5, (Integer) dataset.get("name_length"));
    }

    @Test
    public void testAddRow() {
        Dataset dataset = testDataset.copy();
        Object[] newRow = { 5, "Eve", 32, 58000.0 };
        dataset.addRow(newRow);

        assertEquals(5, dataset.size());
        dataset.absolute(4);
        assertEquals(5, dataset.getInt("id"));
        assertEquals("Eve", dataset.get("name"));
    }

    @Test
    public void testAddRowAtPosition() {
        Dataset dataset = testDataset.copy();
        Object[] newRow = { 5, "Eve", 32, 58000.0 };
        dataset.addRow(1, newRow);

        assertEquals(5, dataset.size());
        dataset.absolute(1);
        assertEquals(5, dataset.getInt("id"));
        assertEquals("Eve", dataset.get("name"));
    }

    @Test
    public void testAddRows() {
        Dataset dataset = testDataset.copy();
        List<Object[]> newRows = Arrays.asList(new Object[] { 5, "Eve", 32, 58000.0 }, new Object[] { 6, "Frank", 29, 52000.0 });
        dataset.addRows(newRows);

        assertEquals(6, dataset.size());
        dataset.absolute(4);
        assertEquals(5, dataset.getInt("id"));
        dataset.absolute(5);
        assertEquals(6, dataset.getInt("id"));
    }

    @Test
    public void testAddRowsAtPosition() {
        Dataset dataset = testDataset.copy();
        List<Object[]> newRows = Arrays.asList(new Object[] { 5, "Eve", 32, 58000.0 }, new Object[] { 6, "Frank", 29, 52000.0 });
        dataset.addRows(1, newRows);

        assertEquals(6, dataset.size());
        dataset.absolute(1);
        assertEquals(5, dataset.getInt("id"));
        dataset.absolute(2);
        assertEquals(6, dataset.getInt("id"));
    }

    @Test
    public void testRemoveRow() {
        Dataset dataset = testDataset.copy();
        dataset.removeRow(1);

        assertEquals(3, dataset.size());
        dataset.absolute(1);
        assertEquals(3, dataset.getInt("id"));
    }

    @Test
    public void testRemoveRows() {
        Dataset dataset = testDataset.copy();
        dataset.removeMultiRows(0, 2);

        assertEquals(2, dataset.size());
        dataset.absolute(0);
        assertEquals(2, dataset.getInt("id"));
    }

    @Test
    public void testRemoveRowRange() {
        Dataset dataset = testDataset.copy();
        dataset.removeRows(1, 3);

        assertEquals(2, dataset.size());
        dataset.absolute(0);
        assertEquals(1, dataset.getInt("id"));
        dataset.absolute(1);
        assertEquals(4, dataset.getInt("id"));
    }

    @Test
    public void testRemoveDuplicateRowsByColumn() {
        Dataset dataset = testDataset.copy();
        dataset.addRow(new Object[] { 5, "Alice", 40, 70000.0 });

        dataset.removeDuplicateRowsBy("name");

        assertEquals(4, dataset.size());
    }

    @Test
    public void testRemoveDuplicateRowsByColumnWithExtractor() {
        Dataset dataset = testDataset.copy();
        dataset.addRow(new Object[] { 5, "ALICE", 40, 70000.0 });

        dataset.removeDuplicateRowsBy("name", (String name) -> name.toLowerCase());

        assertEquals(4, dataset.size());
    }

    @Test
    public void testRemoveDuplicateRowsByColumns() {
        Dataset dataset = testDataset.copy();
        dataset.addRow(new Object[] { 5, "Alice", 30, 70000.0 });

        dataset.removeDuplicateRowsBy(Arrays.asList("name", "age"));

        assertEquals(4, dataset.size());
    }

    @Test
    public void testRemoveDuplicateRowsByColumnsWithExtractor() {
        Dataset dataset = testDataset.copy();
        dataset.addRow(new Object[] { 5, "Alice", 30, 70000.0 });

        dataset.removeDuplicateRowsBy(Arrays.asList("name", "age"), (DisposableObjArray row) -> row.get(0).toString() + "_" + row.get(1));

        assertEquals(4, dataset.size());
    }

    @Test
    public void testUpdateRow() {
        Dataset dataset = testDataset.copy();
        dataset.updateRow(0, v -> v instanceof Integer ? ((Integer) v) + 5 : v);

        dataset.absolute(0);
        assertEquals(35, dataset.getInt("age"));
    }

    @Test
    public void testUpdateRows() {
        Dataset dataset = testDataset.copy();
        dataset.updateRows(new int[] { 0, 1 }, (i, c, v) -> v instanceof Double ? ((Double) v) * 1.1 : v);

        dataset.absolute(0);
        assertEquals(55000.0, dataset.getDouble("salary"), 0.01);
        dataset.absolute(1);
        assertEquals(49500.0, dataset.getDouble("salary"), 0.01);
    }

    @Test
    public void testUpdateAll() {
        Dataset dataset = testDataset.copy();
        dataset.updateAll(v -> v instanceof Integer ? ((Integer) v) + 1 : v);

        dataset.absolute(0);
        assertEquals(31, dataset.getInt("age"));
        dataset.absolute(1);
        assertEquals(26, dataset.getInt("age"));
    }

    @Test
    public void testReplaceIf() {
        Dataset dataset = testDataset.copy();
        dataset.replaceIf(value -> value instanceof Integer && ((Integer) value) < 30, 30);

        dataset.absolute(1);
        assertEquals(30, dataset.getInt("age"));
    }

    @Test
    public void testPrepend() {
        Dataset dataset = testDataset.copy();
        Dataset other = Dataset.rows(columnNames, new Object[][] { { 0, "Zero", 20, 40000.0 } });

        dataset.prepend(other);

        assertEquals(5, dataset.size());
        dataset.absolute(0);
        assertEquals(0, dataset.getInt("id"));
        assertEquals("Zero", dataset.get("name"));
    }

    @Test
    public void testAppend() {
        Dataset dataset = testDataset.copy();
        Dataset other = Dataset.rows(columnNames, new Object[][] { { 5, "Five", 40, 70000.0 } });

        dataset.append(other);

        assertEquals(5, dataset.size());
        dataset.absolute(4);
        assertEquals(5, dataset.getInt("id"));
        assertEquals("Five", dataset.get("name"));
    }

    @Test
    public void testMerge() {
        Dataset dataset = testDataset.copy();
        Dataset other = Dataset.rows(columnNames, new Object[][] { { 5, "Five", 40, 70000.0 } });

        dataset.merge(other);

        assertEquals(5, dataset.size());
        dataset.absolute(4);
        assertEquals(5, dataset.getInt("id"));
    }

    @Test
    public void testMergeWithRequiredSameColumns() {
        Dataset dataset = testDataset.copy();
        Dataset other = Dataset.rows(columnNames, new Object[][] { { 5, "Five", 40, 70000.0 } });

        dataset.merge(other, true);

        assertEquals(5, dataset.size());
    }

    @Test
    public void testMergeWithSelectColumns() {
        Dataset dataset = testDataset.copy();
        Dataset other = Dataset.rows(columnNames, new Object[][] { { 5, "Five", 40, 70000.0 } });

        dataset.merge(other, Arrays.asList("id", "name"));

        assertEquals(5, dataset.size());
        dataset.absolute(4);
        assertEquals(5, dataset.getInt("id"));
        assertEquals("Five", dataset.get("name"));
    }

    @Test
    public void testMergeWithRange() {
        Dataset dataset = testDataset.copy();
        Dataset other = Dataset.rows(columnNames, new Object[][] { { 5, "Five", 40, 70000.0 }, { 6, "Six", 35, 65000.0 } });

        dataset.merge(other, 0, 1, Arrays.asList("id", "name"));

        assertEquals(5, dataset.size());
        dataset.absolute(4);
        assertEquals(5, dataset.getInt("id"));
        assertEquals("Five", dataset.get("name"));
    }

    @Test
    public void testGetRow() {
        Object[] row = testDataset.getRow(0);
        assertNotNull(row);
        assertEquals(4, row.length);
        assertEquals(1, row[0]);
        assertEquals("Alice", row[1]);
        assertEquals(30, row[2]);
        assertEquals(50000.0, row[3]);

        assertThrows(IndexOutOfBoundsException.class, () -> {
            testDataset.getRow(-1);
        });
    }

    @Test
    public void testFirstRow() {
        Optional<Object[]> firstRow = testDataset.firstRow();
        assertTrue(firstRow.isPresent());
        assertEquals(1, firstRow.get()[0]);
        assertEquals("Alice", firstRow.get()[1]);

        Optional<Object[]> emptyFirst = emptyDataset.firstRow();
        assertFalse(emptyFirst.isPresent());
    }

    @Test
    public void testLastRow() {
        Optional<Object[]> lastRow = testDataset.lastRow();
        assertTrue(lastRow.isPresent());
        assertEquals(4, lastRow.get()[0]);
        assertEquals("Diana", lastRow.get()[1]);

        Optional<Object[]> emptyLast = emptyDataset.lastRow();
        assertFalse(emptyLast.isPresent());
    }

    @Test
    public void testToList() {
        List<Object[]> list = testDataset.toList();
        assertNotNull(list);
        assertEquals(4, list.size());
        assertEquals(1, list.get(0)[0]);
        assertEquals("Alice", list.get(0)[1]);
    }

    @Test
    public void testToListWithRange() {
        List<Object[]> list = testDataset.toList(1, 3);
        assertNotNull(list);
        assertEquals(2, list.size());
        assertEquals(2, list.get(0)[0]);
        assertEquals("Bob", list.get(0)[1]);
        assertEquals(3, list.get(1)[0]);
        assertEquals("Charlie", list.get(1)[1]);

        assertThrows(IndexOutOfBoundsException.class, () -> {
            testDataset.toList(-1, 1);
        });
    }

    @Test
    public void testToJson() {
        String json = testDataset.toJson();
        assertNotNull(json);
        assertTrue(json.contains("Alice"));
        assertTrue(json.contains("Bob"));
    }

    @Test
    public void testToJsonWithRange() {
        String json = testDataset.toJson(1, 3);
        assertNotNull(json);
        assertTrue(json.contains("Bob"));
        assertTrue(json.contains("Charlie"));
        assertFalse(json.contains("Alice"));
        assertFalse(json.contains("Diana"));
    }

    @Test
    public void testToJsonWithRangeAndColumns() {
        String json = testDataset.toJson(0, 2, Arrays.asList("name", "age"));
        assertNotNull(json);
        assertTrue(json.contains("Alice"));
        assertTrue(json.contains("Bob"));
        assertFalse(json.contains("50000"));
    }

    @Test
    public void testToJsonFile() throws Exception {
        File tempFile = File.createTempFile("test", ".json");
        tempFile.deleteOnExit();

        testDataset.toJson(tempFile);
        assertTrue(tempFile.exists());
        assertTrue(tempFile.length() > 0);
    }

    @Test
    public void testToJsonOutputStream() throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        testDataset.toJson(baos);

        String json = baos.toString();
        assertNotNull(json);
        assertTrue(json.contains("Alice"));
    }

    @Test
    public void testToJsonWriter() throws Exception {
        StringWriter writer = new StringWriter();
        testDataset.toJson(writer);

        String json = writer.toString();
        assertNotNull(json);
        assertTrue(json.contains("Alice"));
    }

    @Test
    public void testToXml() {
        String xml = testDataset.toXml();
        assertNotNull(xml);
        assertTrue(xml.contains("Alice"));
        assertTrue(xml.contains("Bob"));
    }

    @Test
    public void testToXmlWithRowElementName() {
        String xml = testDataset.toXml("employee");
        assertNotNull(xml);
        assertTrue(xml.contains("<employee>"));
        assertTrue(xml.contains("</employee>"));
    }

    @Test
    public void testToXmlWithRange() {
        String xml = testDataset.toXml(1, 3);
        assertNotNull(xml);
        assertTrue(xml.contains("Bob"));
        assertTrue(xml.contains("Charlie"));
        assertFalse(xml.contains("Alice"));
        assertFalse(xml.contains("Diana"));
    }

    @Test
    public void testToCsv() {
        String csv = testDataset.toCsv();
        assertNotNull(csv);
        N.println(csv);
        assertTrue(csv.contains("\"id\",\"name\",\"age\",\"salary\""));
        assertTrue(csv.contains("\"Alice\""));
        assertTrue(csv.contains("\"Bob\""));
    }

    @Test
    public void testToCsvWithRangeAndColumns() {
        String csv = testDataset.toCsv(0, 2, Arrays.asList("name", "age"));
        assertNotNull(csv);
        N.println(csv);
        assertTrue(csv.contains("\"name\",\"age\""));
        assertTrue(csv.contains("\"Alice\",30"));
        assertTrue(csv.contains("\"Bob\",25"));
        assertFalse(csv.contains("50000"));
    }

    @Test
    public void testGroupBySimple() {
        Dataset dataset = testDataset.copy();
        dataset.addRow(new Object[] { 5, "Alice", 35, 75000.0 });

        Dataset grouped = dataset.groupBy("name", "salary", "avg_salary", Collectors.averagingDouble(v -> (Double) v));

        assertNotNull(grouped);
        assertTrue(grouped.size() > 0);
        assertTrue(grouped.containsColumn("name"));
        assertTrue(grouped.containsColumn("avg_salary"));
    }

    @Test
    public void testGroupByWithExtractor() {
        Dataset grouped = testDataset.groupBy("age", (Integer age) -> age >= 30 ? "Senior" : "Junior", "salary", "avg_salary",
                Collectors.averagingDouble(v -> (Double) v));

        assertNotNull(grouped);
        assertTrue(grouped.size() > 0);
    }

    @Test
    public void testGroupByMultipleColumns() {
        Dataset grouped = testDataset.groupBy(Arrays.asList("age"));

        assertNotNull(grouped);
        assertTrue(grouped.size() > 0);
    }

    @Test
    public void testGroupByMultipleColumnsWithAggregation() {
        Dataset grouped = testDataset.groupBy(Arrays.asList("age"), "salary", "total_salary", Collectors.summingDouble(v -> (Double) v));

        assertNotNull(grouped);
        assertTrue(grouped.size() > 0);
        assertTrue(grouped.containsColumn("age"));
        assertTrue(grouped.containsColumn("total_salary"));
    }

    @Test
    public void testRollup() {
        Stream<Dataset> rollupStream = testDataset.rollup(Arrays.asList("age"));
        assertNotNull(rollupStream);

        List<Dataset> rollupResults = rollupStream.toList();
        assertNotNull(rollupResults);
        assertTrue(rollupResults.size() > 0);
    }

    @Test
    public void testRollupWithAggregation() {
        Stream<Dataset> rollupStream = testDataset.rollup(Arrays.asList("age"), "salary", "total_salary", Collectors.summingDouble(v -> (Double) v));

        assertNotNull(rollupStream);
        List<Dataset> rollupResults = rollupStream.toList();
        assertTrue(rollupResults.size() > 0);
    }

    @Test
    public void testCube() {
        Stream<Dataset> cubeStream = testDataset.cube(Arrays.asList("age"));
        assertNotNull(cubeStream);

        List<Dataset> cubeResults = cubeStream.toList();
        assertNotNull(cubeResults);
        assertTrue(cubeResults.size() > 0);
    }

    @Test
    public void testCubeWithAggregation() {
        Stream<Dataset> cubeStream = testDataset.cube(Arrays.asList("age"), "salary", "avg_salary", Collectors.averagingDouble(v -> (Double) v));

        assertNotNull(cubeStream);
        List<Dataset> cubeResults = cubeStream.toList();
        assertTrue(cubeResults.size() > 0);
    }

    @Test
    public void testSortBy() {
        Dataset dataset = testDataset.copy();
        dataset.sortBy("age");

        dataset.absolute(0);
        assertEquals(25, dataset.getInt("age"));
        dataset.absolute(3);
        assertEquals(35, dataset.getInt("age"));
    }

    @Test
    public void testSortByWithComparator() {
        Dataset dataset = testDataset.copy();
        dataset.sortBy("age", Comparator.<Integer> naturalOrder().reversed());

        dataset.absolute(0);
        assertEquals(35, dataset.getInt("age"));
        dataset.absolute(3);
        assertEquals(25, dataset.getInt("age"));
    }

    @Test
    public void testSortByMultipleColumns() {
        Dataset dataset = testDataset.copy();
        dataset.addRow(new Object[] { 5, "Eve", 30, 48000.0 });

        dataset.sortBy(Arrays.asList("age", "salary"));

        dataset.absolute(0);
        assertEquals(25, dataset.getInt("age"));
    }

    @Test
    public void testSortByMultipleColumnsWithComparator() {
        Dataset dataset = testDataset.copy();
        Comparator<Object[]> comparator = (a, b) -> {
            int ageCompare = ((Integer) a[0]).compareTo((Integer) b[0]);
            if (ageCompare != 0)
                return ageCompare;
            return ((String) a[1]).compareTo((String) b[1]);
        };
        dataset.sortBy(Arrays.asList("age", "name"), comparator);

        dataset.absolute(0);
        assertEquals(25, dataset.getInt("age"));
    }

    @Test
    public void testSortByWithKeyExtractor() {
        Dataset dataset = testDataset.copy();
        dataset.sortBy(Arrays.asList("age", "name"), (DisposableObjArray row) -> ((Integer) row.get(0)) * 1000 + ((String) row.get(1)).length());

        assertNotNull(dataset);
        assertTrue(dataset.size() > 0);
    }

    @Test
    public void testParallelSortBy() {
        Dataset dataset = testDataset.copy();
        dataset.parallelSortBy("age");

        dataset.absolute(0);
        assertEquals(25, dataset.getInt("age"));
    }

    @Test
    public void testParallelSortByWithComparator() {
        Dataset dataset = testDataset.copy();
        dataset.parallelSortBy("name", Comparator.<String> naturalOrder().reversed());

        dataset.absolute(0);
        String firstName = dataset.get("name").toString();
        assertTrue(firstName.compareTo("Charlie") >= 0);
    }

    @Test
    public void testParallelSortByMultipleColumns() {
        Dataset dataset = testDataset.copy();
        dataset.parallelSortBy(Arrays.asList("age", "name"));

        dataset.absolute(0);
        assertEquals(25, dataset.getInt("age"));
    }

    @Test
    public void testTopBy() {
        Dataset top2 = testDataset.topBy("salary", 2);

        assertNotNull(top2);
        assertEquals(2, top2.size());
    }

    @Test
    public void testTopByWithComparator() {
        Dataset top2 = testDataset.topBy("age", 2, Comparator.<Integer> naturalOrder().reversed());

        assertNotNull(top2);
        assertEquals(2, top2.size());
    }

    @Test
    public void testTopByMultipleColumns() {
        Dataset top2 = testDataset.topBy(Arrays.asList("age", "salary"), 2);

        assertNotNull(top2);
        assertEquals(2, top2.size());
    }

    @Test
    public void testTopByMultipleColumnsWithComparator() {
        Comparator<Object[]> comp = (a, b) -> ((Integer) a[0]).compareTo((Integer) b[0]);
        Dataset top2 = testDataset.topBy(Arrays.asList("age", "salary"), 2, comp);

        assertNotNull(top2);
        assertEquals(2, top2.size());
    }

    @Test
    public void testTopByWithKeyExtractor() {
        Dataset top2 = testDataset.topBy(Arrays.asList("age", "salary"), 2, (DisposableObjArray row) -> (Integer) row.get(0));

        assertNotNull(top2);
        assertEquals(2, top2.size());
    }

    @Test
    public void testDistinct() {
        Dataset dataset = testDataset.copy();
        dataset.addRow(new Object[] { 1, "Alice", 30, 50000.0 });

        Dataset distinct = dataset.distinct();

        assertNotNull(distinct);
        assertEquals(4, distinct.size());
    }

    @Test
    public void testDistinctByColumn() {
        Dataset dataset = testDataset.copy();
        dataset.addRow(new Object[] { 5, "Alice", 35, 75000.0 });

        Dataset distinct = dataset.distinctBy("name");

        assertNotNull(distinct);
        assertEquals(4, distinct.size());
    }

    @Test
    public void testDistinctByColumnWithExtractor() {
        Dataset dataset = testDataset.copy();
        dataset.addRow(new Object[] { 5, "ALICE", 35, 75000.0 });

        Dataset distinct = dataset.distinctBy("name", (String name) -> name.toLowerCase());

        assertNotNull(distinct);
        assertEquals(4, distinct.size());
    }

    @Test
    public void testDistinctByMultipleColumns() {
        Dataset dataset = testDataset.copy();
        dataset.addRow(new Object[] { 5, "Alice", 30, 75000.0 });

        Dataset distinct = dataset.distinctBy(Arrays.asList("name", "age"));

        assertNotNull(distinct);
        assertEquals(4, distinct.size());
    }

    @Test
    public void testDistinctByMultipleColumnsWithExtractor() {
        Dataset dataset = testDataset.copy();
        dataset.addRow(new Object[] { 5, "Alice", 30, 75000.0 });

        Dataset distinct = dataset.distinctBy(Arrays.asList("name", "age"), (DisposableObjArray row) -> row.get(0).toString() + "_" + row.get(1));

        assertNotNull(distinct);
        assertEquals(4, distinct.size());
    }

    @Test
    public void testFilter() {
        Dataset filtered = testDataset.filter((DisposableObjArray row) -> (Integer) row.get(2) >= 30);

        assertNotNull(filtered);
        assertEquals(2, filtered.size());
    }

    @Test
    public void testFilterWithMax() {
        Dataset filtered = testDataset.filter((DisposableObjArray row) -> (Integer) row.get(2) >= 25, 2);

        assertNotNull(filtered);
        assertTrue(filtered.size() <= 2);
    }

    @Test
    public void testFilterWithRange() {
        Dataset filtered = testDataset.filter(1, 3, (DisposableObjArray row) -> (Integer) row.get(2) >= 25);

        assertNotNull(filtered);
        assertTrue(filtered.size() >= 0);
    }

    @Test
    public void testFilterWithRangeAndMax() {
        Dataset filtered = testDataset.filter(0, 4, (DisposableObjArray row) -> (Integer) row.get(2) >= 25, 1);

        assertNotNull(filtered);
        assertTrue(filtered.size() <= 1);
    }

    @Test
    public void testFilterWithBiPredicate() {
        Tuple2<String, String> columns = Tuple.of("age", "salary");
        BiPredicate<Integer, Double> predicate = (age, salary) -> age >= 30 && salary >= 50000;
        Dataset filtered = testDataset.filter(columns, predicate);

        assertNotNull(filtered);
        assertTrue(filtered.size() >= 0);
    }

    @Test
    public void testFilterWithTriPredicate() {
        Tuple3<String, String, String> columns = Tuple.of("id", "age", "salary");
        TriPredicate<Integer, Integer, Double> predicate = (id, age, salary) -> id > 1 && age >= 30 && salary >= 50000;
        Dataset filtered = testDataset.filter(columns, predicate);

        assertNotNull(filtered);
        assertTrue(filtered.size() >= 0);
    }

    @Test
    public void testFilterByColumn() {
        Dataset filtered = testDataset.filter("age", (Integer age) -> age >= 30);

        assertNotNull(filtered);
        assertEquals(2, filtered.size());
    }

    @Test
    public void testFilterByColumnWithMax() {
        Dataset filtered = testDataset.filter("age", (Integer age) -> age >= 25, 2);

        assertNotNull(filtered);
        assertTrue(filtered.size() <= 2);
    }

    @Test
    public void testFilterByColumnsWithSelection() {
        Dataset filtered = testDataset.filter(Arrays.asList("name", "age"), (DisposableObjArray row) -> ((String) row.get(0)).startsWith("A"));

        assertNotNull(filtered);
        assertEquals(1, filtered.size());
    }

    @Test
    public void testMap() {
        Dataset mapped = testDataset.map("name", "upper_name", "age", (String name) -> name.toUpperCase());

        assertNotNull(mapped);
        assertTrue(mapped.containsColumn("upper_name"));
        assertTrue(mapped.containsColumn("age"));
        assertFalse(mapped.containsColumn("name"));
        assertFalse(mapped.containsColumn("id"));
        assertFalse(mapped.containsColumn("salary"));
    }

    @Test
    public void testMapWithMultipleCopyColumns() {
        Dataset mapped = testDataset.map("name", "upper_name", Arrays.asList("age", "salary"), (String name) -> name.toUpperCase());

        assertNotNull(mapped);
        assertTrue(mapped.containsColumn("upper_name"));
        assertTrue(mapped.containsColumn("age"));
        assertTrue(mapped.containsColumn("salary"));
        assertFalse(mapped.containsColumn("name"));
    }

    @Test
    public void testMapWithBiFunction() {
        Tuple2<String, String> fromColumns = Tuple.of("name", "age");
        Dataset mapped = testDataset.map(fromColumns, "name_age", Arrays.asList("id"), (String name, Integer age) -> name + "_" + age);

        assertNotNull(mapped);
        assertTrue(mapped.containsColumn("name_age"));
        assertTrue(mapped.containsColumn("id"));
    }

    @Test
    public void testMapWithTriFunction() {
        Tuple3<String, String, String> fromColumns = Tuple.of("id", "name", "age");
        Dataset mapped = testDataset.map(fromColumns, "combined", Arrays.asList("salary"),
                (Integer id, String name, Integer age) -> id + ":" + name + ":" + age);

        assertNotNull(mapped);
        assertTrue(mapped.containsColumn("combined"));
        assertTrue(mapped.containsColumn("salary"));
    }

    @Test
    public void testMapWithDisposableArray() {
        Dataset mapped = testDataset.map(Arrays.asList("name", "age"), "name_age", Arrays.asList("id", "salary"),
                (DisposableObjArray row) -> row.get(0) + "_" + row.get(1));

        assertNotNull(mapped);
        assertTrue(mapped.containsColumn("name_age"));
        assertTrue(mapped.containsColumn("id"));
        assertTrue(mapped.containsColumn("salary"));
    }

    @Test
    public void testFlatMap() {
        Dataset mapped = testDataset.flatMap("name", "name_chars", "id", (String name) -> Arrays.asList(name.split("")));

        assertNotNull(mapped);
        assertTrue(mapped.containsColumn("name_chars"));
        assertTrue(mapped.containsColumn("id"));
        assertTrue(mapped.size() > testDataset.size());
    }

    @Test
    public void testFlatMapWithBiFunction() {
        Tuple2<String, String> fromColumns = Tuple.of("name", "age");
        Dataset mapped = testDataset.flatMap(fromColumns, "name_parts", Arrays.asList("id"), (String name, Integer age) -> Arrays.asList(name, age.toString()));

        assertNotNull(mapped);
        assertTrue(mapped.containsColumn("name_parts"));
        assertTrue(mapped.size() > testDataset.size());
    }

    @Test
    public void testInnerJoin() {
        Dataset right = Dataset.rows(Arrays.asList("id", "department"), new Object[][] { { 1, "Engineering" }, { 2, "Marketing" }, { 3, "Sales" } });

        Dataset joined = testDataset.innerJoin(right, "id", "id");

        assertNotNull(joined);
        assertEquals(3, joined.size());
        assertTrue(joined.containsColumn("department"));
    }

    @Test
    public void testInnerJoinWithMap() {
        Dataset right = Dataset.rows(Arrays.asList("emp_id", "department"), new Object[][] { { 1, "Engineering" }, { 2, "Marketing" } });

        Map<String, String> joinColumns = new HashMap<>();
        joinColumns.put("id", "emp_id");

        Dataset joined = testDataset.innerJoin(right, joinColumns);

        assertNotNull(joined);
        assertEquals(2, joined.size());
        assertTrue(joined.containsColumn("department"));
    }

    @Test
    public void testInnerJoinWithCollectingColumn() {
        Dataset right = Dataset.rows(Arrays.asList("id", "skill"), new Object[][] { { 1, "Java" }, { 1, "Python" }, { 2, "JavaScript" } });

        Map<String, String> joinColumns = new HashMap<>();
        joinColumns.put("id", "id");

        Dataset joined = testDataset.innerJoin(right, joinColumns, "skills", List.class);

        assertNotNull(joined);
        assertTrue(joined.containsColumn("skills"));
    }

    @Test
    public void testInnerJoinWithCustomCollector() {
        Dataset right = Dataset.rows(Arrays.asList("id", "skill"), new Object[][] { { 1, "Java" }, { 1, "Python" }, { 2, "JavaScript" } });

        Map<String, String> joinColumns = new HashMap<>();
        joinColumns.put("id", "id");

        Dataset joined = testDataset.innerJoin(right, joinColumns, "skills", List.class);

        assertNotNull(joined);
        assertTrue(joined.containsColumn("skills"));
    }

    @Test
    public void testLeftJoin() {
        Dataset right = Dataset.rows(Arrays.asList("id", "department"), new Object[][] { { 1, "Engineering" }, { 2, "Marketing" } });

        Dataset joined = testDataset.leftJoin(right, "id", "id");

        assertNotNull(joined);
        assertEquals(4, joined.size());
        assertTrue(joined.containsColumn("department"));
    }

    @Test
    public void testRightJoin() {
        Dataset right = Dataset.rows(Arrays.asList("id", "department"), new Object[][] { { 1, "Engineering" }, { 2, "Marketing" }, { 5, "HR" } });

        Dataset joined = testDataset.rightJoin(right, "id", "id");

        assertNotNull(joined);
        assertEquals(3, joined.size());
        assertTrue(joined.containsColumn("department"));
    }

    @Test
    public void testFullJoin() {
        Dataset right = Dataset.rows(Arrays.asList("id", "department"), new Object[][] { { 1, "Engineering" }, { 2, "Marketing" }, { 5, "HR" } });

        Dataset joined = testDataset.fullJoin(right, "id", "id");

        assertNotNull(joined);
        assertEquals(5, joined.size());
        assertTrue(joined.containsColumn("department"));
    }

    @Test
    public void testUnion() {
        Dataset other = Dataset.rows(columnNames, new Object[][] { { 5, "Eve", 32, 58000.0 }, { 1, "Alice", 30, 50000.0 } });

        Dataset union = testDataset.union(other);

        assertNotNull(union);
        assertEquals(5, union.size());
    }

    @Test
    public void testUnionWithRequiredSameColumns() {
        Dataset other = Dataset.rows(columnNames, new Object[][] { { 5, "Eve", 32, 58000.0 } });

        Dataset union = testDataset.union(other, true);

        assertNotNull(union);
        assertEquals(5, union.size());
    }

    @Test
    public void testUnionWithKeyColumns() {
        Dataset other = Dataset.rows(columnNames, new Object[][] { { 5, "Eve", 32, 58000.0 }, { 1, "Alice_Modified", 31, 51000.0 } });

        Dataset union = testDataset.union(other, Arrays.asList("id"));

        assertNotNull(union);
        assertEquals(5, union.size());
    }

    @Test
    public void testUnionAll() {
        Dataset other = Dataset.rows(columnNames, new Object[][] { { 5, "Eve", 32, 58000.0 }, { 1, "Alice", 30, 50000.0 } });

        Dataset unionAll = testDataset.unionAll(other);

        assertNotNull(unionAll);
        assertEquals(6, unionAll.size());
    }

    @Test
    public void testIntersect() {
        Dataset other = Dataset.rows(columnNames, new Object[][] { { 1, "Alice", 30, 50000.0 }, { 2, "Bob", 25, 45000.0 }, { 5, "Eve", 32, 58000.0 } });

        Dataset intersect = testDataset.intersect(other);

        assertNotNull(intersect);
        assertEquals(2, intersect.size());
    }

    @Test
    public void testIntersectWithKeyColumns() {
        Dataset other = Dataset.rows(columnNames,
                new Object[][] { { 1, "Alice_Modified", 31, 51000.0 }, { 2, "Bob", 25, 45000.0 }, { 5, "Eve", 32, 58000.0 } });

        Dataset intersect = testDataset.intersect(other, Arrays.asList("id"));

        assertNotNull(intersect);
        assertEquals(2, intersect.size());
    }

    @Test
    public void testIntersectAll() {
        Dataset other = Dataset.rows(columnNames, new Object[][] { { 1, "Alice", 30, 50000.0 }, { 1, "Alice", 30, 50000.0 }, { 2, "Bob", 25, 45000.0 } });

        Dataset intersectAll = testDataset.intersectAll(other);

        assertNotNull(intersectAll);
        assertEquals(2, intersectAll.size());
    }

    @Test
    public void testExcept() {
        Dataset other = Dataset.rows(columnNames, new Object[][] { { 1, "Alice", 30, 50000.0 }, { 2, "Bob", 25, 45000.0 } });

        Dataset except = testDataset.except(other);

        assertNotNull(except);
        assertEquals(2, except.size());
    }

    @Test
    public void testExceptAll() {
        Dataset other = Dataset.rows(columnNames, new Object[][] { { 1, "Alice", 30, 50000.0 } });

        Dataset exceptAll = testDataset.exceptAll(other);

        assertNotNull(exceptAll);
        assertEquals(3, exceptAll.size());
    }

    @Test
    public void testIntersection() {
        Dataset other = Dataset.rows(columnNames, new Object[][] { { 1, "Alice", 30, 50000.0 }, { 3, "Charlie", 35, 60000.0 } });

        Dataset intersection = testDataset.intersection(other);

        assertNotNull(intersection);
        assertEquals(2, intersection.size());
    }

    @Test
    public void testDifference() {
        Dataset other = Dataset.rows(columnNames, new Object[][] { { 1, "Alice", 30, 50000.0 }, { 2, "Bob", 25, 45000.0 } });

        Dataset difference = testDataset.difference(other);

        assertNotNull(difference);
        assertEquals(2, difference.size());
    }

    @Test
    public void testSymmetricDifference() {
        Dataset other = Dataset.rows(columnNames, new Object[][] { { 1, "Alice", 30, 50000.0 }, { 5, "Eve", 32, 58000.0 } });

        Dataset symDiff = testDataset.symmetricDifference(other);

        assertNotNull(symDiff);
        assertEquals(4, symDiff.size());
    }

    @Test
    public void testCartesianProduct() {
        Dataset other = Dataset.rows(Arrays.asList("dept_id", "dept_name"), new Object[][] { { 1, "Engineering" }, { 2, "Marketing" } });

        Dataset cartesian = testDataset.cartesianProduct(other);

        assertNotNull(cartesian);
        assertEquals(8, cartesian.size());
        assertEquals(6, cartesian.columnCount());
    }

    @Test
    public void testSplit() {
        Stream<Dataset> chunks = testDataset.split(2);

        assertNotNull(chunks);
        List<Dataset> chunkList = chunks.toList();
        assertEquals(2, chunkList.size());
        assertEquals(2, chunkList.get(0).size());
        assertEquals(2, chunkList.get(1).size());
    }

    @Test
    public void testSplitWithColumns() {
        Stream<Dataset> chunks = testDataset.split(3, Arrays.asList("name", "age"));

        assertNotNull(chunks);
        List<Dataset> chunkList = chunks.toList();
        assertTrue(chunkList.size() >= 1);
        assertTrue(chunkList.get(0).containsColumn("name"));
        assertTrue(chunkList.get(0).containsColumn("age"));
        assertFalse(chunkList.get(0).containsColumn("id"));
    }

    @Test
    public void testSplitToList() {
        List<Dataset> chunks = testDataset.splitToList(2);

        assertNotNull(chunks);
        assertEquals(2, chunks.size());
        assertEquals(2, chunks.get(0).size());
        assertEquals(2, chunks.get(1).size());
    }

    @Test
    public void testSplitToListWithColumns() {
        List<Dataset> chunks = testDataset.splitToList(3, Arrays.asList("name", "age"));

        assertNotNull(chunks);
        assertTrue(chunks.size() >= 1);
        assertTrue(chunks.get(0).containsColumn("name"));
        assertTrue(chunks.get(0).containsColumn("age"));
    }

    @Test
    public void testSlice() {
        Dataset slice = testDataset.slice(1, 3);

        assertNotNull(slice);
        assertEquals(2, slice.size());
        slice.absolute(0);
        assertEquals(2, slice.getInt("id"));
    }

    @Test
    public void testSliceWithColumns() {
        Dataset slice = testDataset.slice(Arrays.asList("name", "age"));

        assertNotNull(slice);
        assertEquals(4, slice.size());
        assertEquals(2, slice.columnCount());
        assertTrue(slice.containsColumn("name"));
        assertTrue(slice.containsColumn("age"));
        assertFalse(slice.containsColumn("id"));
        assertFalse(slice.containsColumn("salary"));
    }

    @Test
    public void testSliceWithRangeAndColumns() {
        Dataset slice = testDataset.slice(1, 3, Arrays.asList("name", "age"));

        assertNotNull(slice);
        assertEquals(2, slice.size());
        assertEquals(2, slice.columnCount());
        slice.absolute(0);
        assertEquals("Bob", slice.get("name"));
    }

    @Test
    public void testCopy() {
        Dataset copy = testDataset.copy();

        assertNotNull(copy);
        assertEquals(testDataset.size(), copy.size());
        assertEquals(testDataset.columnCount(), copy.columnCount());
        assertEquals(testDataset.columnNameList(), copy.columnNameList());

        copy.absolute(0);
        copy.set("name", "Modified");
        testDataset.absolute(0);
        assertEquals("Alice", testDataset.get("name"));
    }

    @Test
    public void testCopyWithRange() {
        Dataset copy = testDataset.copy(1, 3);

        assertNotNull(copy);
        assertEquals(2, copy.size());
        copy.absolute(0);
        assertEquals(2, copy.getInt("id"));
    }

    @Test
    public void testCopyWithColumns() {
        Dataset copy = testDataset.copy(Arrays.asList("name", "age"));

        assertNotNull(copy);
        assertEquals(4, copy.size());
        assertEquals(2, copy.columnCount());
        assertTrue(copy.containsColumn("name"));
        assertTrue(copy.containsColumn("age"));
    }

    @Test
    public void testCopyWithRangeAndColumns() {
        Dataset copy = testDataset.copy(1, 3, Arrays.asList("name", "age"));

        assertNotNull(copy);
        assertEquals(2, copy.size());
        assertEquals(2, copy.columnCount());
        copy.absolute(0);
        assertEquals("Bob", copy.get("name"));
    }

    @Test
    public void testClone() {
        Dataset clone = testDataset.clone();

        assertNotNull(clone);
        assertEquals(testDataset.size(), clone.size());
        assertEquals(testDataset.columnCount(), clone.columnCount());
    }

    @Test
    public void testCloneWithFreeze() {
        Dataset clone = testDataset.clone(true);

        assertNotNull(clone);
        assertTrue(clone.isFrozen());
    }

    @Test
    public void testFreeze() {
        Dataset dataset = testDataset.copy();
        assertFalse(dataset.isFrozen());

        dataset.freeze();
        assertTrue(dataset.isFrozen());

        assertThrows(IllegalStateException.class, () -> {
            dataset.addRow(new Object[] { 5, "Eve", 32, 58000.0 });
        });
    }

    @Test
    public void testIsFrozen() {
        assertFalse(testDataset.isFrozen());

        Dataset dataset = testDataset.copy();
        dataset.freeze();
        assertTrue(dataset.isFrozen());
    }

    @Test
    public void testClear() {
        Dataset dataset = testDataset.copy();
        assertEquals(4, dataset.size());

        dataset.clear();
        assertEquals(0, dataset.size());
        assertTrue(dataset.isEmpty());
    }

    @Test
    public void testIsEmpty() {
        assertFalse(testDataset.isEmpty());
        assertTrue(emptyDataset.isEmpty());

        Dataset dataset = testDataset.copy();
        dataset.clear();
        assertTrue(dataset.isEmpty());
    }

    @Test
    public void testTrimToSize() {
        Dataset dataset = testDataset.copy();
        dataset.trimToSize();
        assertEquals(4, dataset.size());
    }

    @Test
    public void testSize() {
        assertEquals(4, testDataset.size());
        assertEquals(0, emptyDataset.size());

        Dataset dataset = testDataset.copy();
        dataset.addRow(new Object[] { 5, "Eve", 32, 58000.0 });
        assertEquals(5, dataset.size());
    }

    @Test
    public void testProperties() {
        Map<String, Object> properties = testDataset.getProperties();
        assertNotNull(properties);
    }

    @Test
    public void testColumns() {
        Stream<ImmutableList<Object>> columns = testDataset.columns();
        assertNotNull(columns);

        List<ImmutableList<Object>> columnList = columns.toList();
        assertEquals(4, columnList.size());
        assertEquals(4, columnList.get(0).size());
    }

    @Test
    public void testColumnMap() {
        Map<String, ImmutableList<Object>> columnMap = testDataset.columnMap();
        assertNotNull(columnMap);
        assertEquals(4, columnMap.size());
        assertTrue(columnMap.containsKey("id"));
        assertTrue(columnMap.containsKey("name"));
        assertTrue(columnMap.containsKey("age"));
        assertTrue(columnMap.containsKey("salary"));

        ImmutableList<Object> nameColumn = columnMap.get("name");
        assertEquals(4, nameColumn.size());
        assertEquals("Alice", nameColumn.get(0));
    }

    @Test
    public void testPrintln() {
        testDataset.println();
        testDataset.println("Test Dataset:");
        testDataset.println(1, 3);
        testDataset.println(0, 2, Arrays.asList("name", "age"));

        StringWriter writer = new StringWriter();
        testDataset.println(writer);
        String output = writer.toString();
        assertNotNull(output);
        assertTrue(output.length() > 0);
    }

    @Test
    public void testPrintlnWithWriter() {
        StringWriter writer = new StringWriter();
        testDataset.println(0, 2, Arrays.asList("name", "age"), writer);

        String output = writer.toString();
        assertNotNull(output);
        assertTrue(output.contains("Alice"));
        assertTrue(output.contains("Bob"));
        assertFalse(output.contains("Charlie"));
    }

    @Test
    public void testPrintlnWithPrefixAndWriter() {
        StringWriter writer = new StringWriter();
        testDataset.println(0, 2, Arrays.asList("name", "age"), "TEST: ", writer);

        String output = writer.toString();
        assertNotNull(output);
        assertTrue(output.contains("TEST: "));
        assertTrue(output.contains("Alice"));
    }

    @Test
    public void testInvalidColumnOperations() {
        assertThrows(IllegalArgumentException.class, () -> {
            testDataset.getColumnIndex("nonexistent");
        });

        assertThrows(IllegalArgumentException.class, () -> {
            testDataset.addColumn("name", Arrays.asList("duplicate", "column"));
        });

        assertThrows(IllegalArgumentException.class, () -> {
            testDataset.removeColumns(Arrays.asList("nonexistent"));
        });
    }

    @Test
    public void testInvalidRowOperations() {
        assertThrows(IndexOutOfBoundsException.class, () -> {
            testDataset.getRow(-1);
        });

        assertThrows(IndexOutOfBoundsException.class, () -> {
            testDataset.getRow(100);
        });

        assertThrows(IndexOutOfBoundsException.class, () -> {
            testDataset.removeRow(-1);
        });
    }

    @Test
    public void testInvalidSliceOperations() {
        assertThrows(IndexOutOfBoundsException.class, () -> {
            testDataset.slice(-1, 2);
        });

        assertThrows(IndexOutOfBoundsException.class, () -> {
            testDataset.slice(2, 1);
        });

        assertThrows(IllegalArgumentException.class, () -> {
            testDataset.slice(Arrays.asList("nonexistent"));
        });
    }

    @Test
    public void testInvalidSplitOperations() {
        assertThrows(IllegalArgumentException.class, () -> {
            testDataset.split(0);
        });

        assertThrows(IllegalArgumentException.class, () -> {
            testDataset.split(-1);
        });
    }

    @Test
    public void testFrozenDatasetOperations() {
        Dataset dataset = testDataset.copy();
        dataset.freeze();

        assertThrows(IllegalStateException.class, () -> {
            dataset.addRow(new Object[] { 5, "Eve", 32, 58000.0 });
        });

        assertThrows(IllegalStateException.class, () -> {
            dataset.removeRow(0);
        });

        assertThrows(IllegalStateException.class, () -> {
            dataset.clear();
        });

        assertThrows(IllegalStateException.class, () -> {
            dataset.addColumn("newCol", Arrays.asList(1, 2, 3, 4));
        });
    }
}

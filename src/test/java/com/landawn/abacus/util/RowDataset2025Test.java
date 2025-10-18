package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.u.Optional;
import com.landawn.abacus.util.stream.Stream;

@Tag("2025")
public class RowDataset2025Test extends TestBase {

    private RowDataset dataset;
    private RowDataset emptyDataset;
    private List<String> columnNames;
    private List<List<Object>> columnList;

    @BeforeEach
    public void setUp() {
        columnNames = new ArrayList<>(Arrays.asList("id", "name", "age", "salary"));

        columnList = new ArrayList<>();
        columnList.add(new ArrayList<>(Arrays.asList(1, 2, 3, 4, 5)));
        columnList.add(new ArrayList<>(Arrays.asList("Alice", "Bob", "Charlie", "Diana", "Eve")));
        columnList.add(new ArrayList<>(Arrays.asList(25, 30, 35, 28, 22)));
        columnList.add(new ArrayList<>(Arrays.asList(50000.0, 60000.0, 70000.0, 55000.0, 45000.0)));

        dataset = new RowDataset(columnNames, columnList);
        emptyDataset = new RowDataset(new ArrayList<>(), new ArrayList<>());
    }

    @Test
    public void testConstructorBasic() {
        RowDataset ds = new RowDataset(columnNames, columnList);
        assertNotNull(ds);
        assertEquals(5, ds.size());
        assertEquals(4, ds.columnCount());
    }

    @Test
    public void testConstructorWithProperties() {
        Map<String, Object> props = new LinkedHashMap<>();
        props.put("source", "test");
        props.put("version", 1);
        RowDataset ds = new RowDataset(columnNames, columnList, props);
        assertNotNull(ds);
        assertEquals("test", ds.getProperties().get("source"));
        assertEquals(1, ds.getProperties().get("version"));
    }

    @Test
    public void testConstructorWithNullProperties() {
        RowDataset ds = new RowDataset(columnNames, columnList, null);
        assertNotNull(ds);
        assertNotNull(ds.getProperties());
        assertTrue(ds.getProperties().isEmpty());
    }

    @Test
    public void testConstructorWithNullColumnNames() {
        assertThrows(Exception.class, () -> {
            new RowDataset(null, columnList);
        });
    }

    @Test
    public void testConstructorWithNullColumnList() {
        assertThrows(Exception.class, () -> {
            new RowDataset(columnNames, null);
        });
    }

    @Test
    public void testConstructorWithEmptyColumnName() {
        List<String> badNames = Arrays.asList("id", "", "age");
        List<List<Object>> cols = Arrays.asList(Arrays.asList(1, 2), Arrays.asList("A", "B"), Arrays.asList(10, 20));
        assertThrows(IllegalArgumentException.class, () -> {
            new RowDataset(badNames, cols);
        });
    }

    @Test
    public void testConstructorWithDuplicateColumnNames() {
        List<String> dupNames = Arrays.asList("id", "name", "id");
        List<List<Object>> cols = Arrays.asList(Arrays.asList(1, 2), Arrays.asList("A", "B"), Arrays.asList(10, 20));
        assertThrows(IllegalArgumentException.class, () -> {
            new RowDataset(dupNames, cols);
        });
    }

    @Test
    public void testConstructorWithMismatchedColumnSizes() {
        List<List<Object>> badCols = new ArrayList<>();
        badCols.add(new ArrayList<>(Arrays.asList(1, 2, 3)));
        badCols.add(new ArrayList<>(Arrays.asList("A", "B")));
        badCols.add(new ArrayList<>(Arrays.asList(10, 20, 30)));

        assertThrows(IllegalArgumentException.class, () -> {
            new RowDataset(columnNames, badCols);
        });
    }

    @Test
    public void testConstructorWithSingleRow() {
        List<List<Object>> singleRow = new ArrayList<>();
        singleRow.add(new ArrayList<>(Arrays.asList(1)));
        singleRow.add(new ArrayList<>(Arrays.asList("Alice")));
        singleRow.add(new ArrayList<>(Arrays.asList(25)));
        singleRow.add(new ArrayList<>(Arrays.asList(50000.0)));

        RowDataset ds = new RowDataset(columnNames, singleRow);
        assertEquals(1, ds.size());
        assertEquals(4, ds.columnCount());
    }

    @Test
    public void testColumnNameList() {
        ImmutableList<String> names = dataset.columnNameList();
        assertNotNull(names);
        assertEquals(4, names.size());
        assertEquals("id", names.get(0));
        assertEquals("name", names.get(1));
        assertEquals("age", names.get(2));
        assertEquals("salary", names.get(3));
    }

    @Test
    public void testGetColumnName() {
        assertEquals("id", dataset.getColumnName(0));
        assertEquals("name", dataset.getColumnName(1));
        assertEquals("age", dataset.getColumnName(2));
        assertEquals("salary", dataset.getColumnName(3));
    }

    @Test
    public void testGetColumnNameOutOfBounds() {
        assertThrows(IndexOutOfBoundsException.class, () -> dataset.getColumnName(-1));
        assertThrows(IndexOutOfBoundsException.class, () -> dataset.getColumnName(10));
    }

    @Test
    public void testGetColumnIndex() {
        assertEquals(0, dataset.getColumnIndex("id"));
        assertEquals(1, dataset.getColumnIndex("name"));
        assertEquals(2, dataset.getColumnIndex("age"));
        assertEquals(3, dataset.getColumnIndex("salary"));
    }

    @Test
    public void testGetColumnIndexNonExistent() {
        assertThrows(IllegalArgumentException.class, () -> {
            dataset.getColumnIndex("nonexistent");
        });
    }

    @Test
    public void testGetColumnIndexes() {
        int[] indexes = dataset.getColumnIndexes(Arrays.asList("name", "age"));
        assertEquals(2, indexes.length);
        assertEquals(1, indexes[0]);
        assertEquals(2, indexes[1]);
    }

    @Test
    public void testGetColumnIndexesEmpty() {
        int[] indexes = dataset.getColumnIndexes(Arrays.asList());
        assertEquals(0, indexes.length);
    }

    @Test
    public void testGetColumnIndexesSameAsColumnNameList() {
        int[] indexes = dataset.getColumnIndexes(dataset.columnNameList());
        assertEquals(4, indexes.length);
        assertEquals(0, indexes[0]);
        assertEquals(1, indexes[1]);
        assertEquals(2, indexes[2]);
        assertEquals(3, indexes[3]);
    }

    @Test
    public void testContainsColumn() {
        assertTrue(dataset.containsColumn("id"));
        assertTrue(dataset.containsColumn("name"));
        assertFalse(dataset.containsColumn("nonexistent"));
        assertFalse(dataset.containsColumn("ID"));
    }

    @Test
    public void testContainsAllColumns() {
        assertTrue(dataset.containsAllColumns(Arrays.asList("id", "name")));
        assertTrue(dataset.containsAllColumns(Arrays.asList()));
        assertFalse(dataset.containsAllColumns(Arrays.asList("id", "nonexistent")));
    }

    @Test
    public void testRenameColumn() {
        RowDataset ds = new RowDataset(columnNames, copyColumnList());
        ds.renameColumn("id", "identifier");
        assertEquals("identifier", ds.getColumnName(0));
        assertTrue(ds.containsColumn("identifier"));
        assertFalse(ds.containsColumn("id"));
    }

    @Test
    public void testRenameColumnToSameName() {
        RowDataset ds = new RowDataset(columnNames, copyColumnList());
        ds.renameColumn("id", "id");
        assertEquals("id", ds.getColumnName(0));
    }

    @Test
    public void testRenameColumnToExistingName() {
        RowDataset ds = new RowDataset(columnNames, copyColumnList());
        assertThrows(IllegalArgumentException.class, () -> {
            ds.renameColumn("id", "name");
        });
    }

    @Test
    public void testRenameColumnNonExistent() {
        RowDataset ds = new RowDataset(columnNames, copyColumnList());
        assertThrows(IllegalArgumentException.class, () -> {
            ds.renameColumn("nonexistent", "newName");
        });
    }

    @Test
    public void testRenameColumnsFrozen() {
        RowDataset ds = new RowDataset(columnNames, copyColumnList());
        ds.freeze();
        assertThrows(IllegalStateException.class, () -> {
            ds.renameColumn("id", "identifier");
        });
    }

    @Test
    public void testRenameColumnsWithMap() {
        RowDataset ds = new RowDataset(columnNames, copyColumnList());
        Map<String, String> renameMap = new LinkedHashMap<>();
        renameMap.put("id", "identifier");
        renameMap.put("name", "fullName");
        ds.renameColumns(renameMap);
        assertEquals("identifier", ds.getColumnName(0));
        assertEquals("fullName", ds.getColumnName(1));
    }

    @Test
    public void testRenameColumnsWithEmptyMap() {
        RowDataset ds = new RowDataset(columnNames, copyColumnList());
        ds.renameColumns(new HashMap<>());
        assertEquals("id", ds.getColumnName(0));
    }

    @Test
    public void testRenameColumnsWithDuplicateNewNames() {
        RowDataset ds = new RowDataset(columnNames, copyColumnList());
        Map<String, String> renameMap = new HashMap<>();
        renameMap.put("id", "sameName");
        renameMap.put("name", "sameName");
        assertThrows(IllegalArgumentException.class, () -> {
            ds.renameColumns(renameMap);
        });
    }

    @Test
    public void testRenameColumnsWithFunction() {
        RowDataset ds = new RowDataset(columnNames, copyColumnList());
        ds.renameColumns(Arrays.asList("id", "name"), name -> name.toUpperCase());
        assertEquals("ID", ds.getColumnName(0));
        assertEquals("NAME", ds.getColumnName(1));
        assertEquals("age", ds.getColumnName(2));
    }

    @Test
    public void testRenameAllColumnsWithFunction() {
        RowDataset ds = new RowDataset(columnNames, copyColumnList());
        ds.renameColumns(name -> "col_" + name);
        assertEquals("col_id", ds.getColumnName(0));
        assertEquals("col_name", ds.getColumnName(1));
        assertEquals("col_age", ds.getColumnName(2));
        assertEquals("col_salary", ds.getColumnName(3));
    }

    @Test
    public void testMoveColumn() {
        RowDataset ds = new RowDataset(columnNames, copyColumnList());
        ds.moveColumn("salary", 0);
        assertEquals("salary", ds.getColumnName(0));
        assertEquals("id", ds.getColumnName(1));
        assertEquals("name", ds.getColumnName(2));
        assertEquals("age", ds.getColumnName(3));
    }

    @Test
    public void testMoveColumnSamePosition() {
        RowDataset ds = new RowDataset(columnNames, copyColumnList());
        ds.moveColumn("id", 0);
        assertEquals("id", ds.getColumnName(0));
    }

    @Test
    public void testMoveColumnToEnd() {
        RowDataset ds = new RowDataset(columnNames, copyColumnList());
        ds.moveColumn("id", 3);
        assertEquals("name", ds.getColumnName(0));
        assertEquals("id", ds.getColumnName(3));
    }

    @Test
    public void testMoveColumnInvalidPosition() {
        RowDataset ds = new RowDataset(columnNames, copyColumnList());
        assertThrows(IndexOutOfBoundsException.class, () -> {
            ds.moveColumn("id", 10);
        });
    }

    @Test
    public void testMoveColumns() {
        RowDataset ds = new RowDataset(columnNames, copyColumnList());
        ds.moveColumns(Arrays.asList("age", "salary"), 0);
        assertEquals("age", ds.getColumnName(0));
        assertEquals("salary", ds.getColumnName(1));
        assertEquals("id", ds.getColumnName(2));
        assertEquals("name", ds.getColumnName(3));
    }

    @Test
    public void testMoveColumnsEmpty() {
        RowDataset ds = new RowDataset(columnNames, copyColumnList());
        ds.moveColumns(Arrays.asList(), 0);
        assertEquals("id", ds.getColumnName(0));
    }

    @Test
    public void testMoveColumnsSingleColumn() {
        RowDataset ds = new RowDataset(columnNames, copyColumnList());
        ds.moveColumns(Arrays.asList("salary"), 0);
        assertEquals("salary", ds.getColumnName(0));
    }

    @Test
    public void testMoveColumnsContiguous() {
        RowDataset ds = new RowDataset(columnNames, copyColumnList());
        ds.moveColumns(Arrays.asList("name", "age"), 0);
        assertEquals("name", ds.getColumnName(0));
        assertEquals("age", ds.getColumnName(1));
    }

    @Test
    public void testSwapColumnPosition() {
        RowDataset ds = new RowDataset(columnNames, copyColumnList());
        ds.swapColumnPosition("id", "salary");
        assertEquals("salary", ds.getColumnName(0));
        assertEquals("name", ds.getColumnName(1));
        assertEquals("age", ds.getColumnName(2));
        assertEquals("id", ds.getColumnName(3));
    }

    @Test
    public void testSwapColumnPositionSame() {
        RowDataset ds = new RowDataset(columnNames, copyColumnList());
        ds.swapColumnPosition("id", "id");
        assertEquals("id", ds.getColumnName(0));
    }

    @Test
    public void testSwapColumnPositionFrozen() {
        RowDataset ds = new RowDataset(columnNames, copyColumnList());
        ds.freeze();
        assertThrows(IllegalStateException.class, () -> {
            ds.swapColumnPosition("id", "name");
        });
    }

    @Test
    public void testMoveRow() {
        RowDataset ds = new RowDataset(columnNames, copyColumnList());
        Object firstValue = ds.get(0, 1);
        ds.moveRow(0, 2);
        assertEquals(firstValue, ds.get(2, 1));
    }

    @Test
    public void testMoveRowSamePosition() {
        RowDataset ds = new RowDataset(columnNames, copyColumnList());
        Object value = ds.get(0, 1);
        ds.moveRow(0, 0);
        assertEquals(value, ds.get(0, 1));
    }

    @Test
    public void testMoveRowToEnd() {
        RowDataset ds = new RowDataset(columnNames, copyColumnList());
        Object firstValue = ds.get(0, 1);
        ds.moveRow(0, 4);
        assertEquals(firstValue, ds.get(4, 1));
    }

    @Test
    public void testMoveRows() {
        RowDataset ds = new RowDataset(columnNames, copyColumnList());
        Object firstValue = ds.get(0, 1);
        ds.moveRows(0, 2, 3);
        assertEquals(firstValue, ds.get(3, 1));
    }

    @Test
    public void testMoveRowsInvalidRange() {
        RowDataset ds = new RowDataset(columnNames, copyColumnList());
        assertThrows(IndexOutOfBoundsException.class, () -> {
            ds.moveRows(3, 2, 0);
        });
    }

    @Test
    public void testSwapRowPosition() {
        RowDataset ds = new RowDataset(columnNames, copyColumnList());
        Object firstValue = ds.get(0, 1);
        Object lastValue = ds.get(4, 1);
        ds.swapRowPosition(0, 4);
        assertEquals(lastValue, ds.get(0, 1));
        assertEquals(firstValue, ds.get(4, 1));
    }

    @Test
    public void testSwapRowPositionSame() {
        RowDataset ds = new RowDataset(columnNames, copyColumnList());
        Object value = ds.get(0, 1);
        ds.swapRowPosition(0, 0);
        assertEquals(value, ds.get(0, 1));
    }

    @Test
    public void testGetByRowAndColumnIndex() {
        assertEquals(1, (Integer) dataset.get(0, 0));
        assertEquals("Alice", dataset.get(0, 1));
        assertEquals(25, (Integer) dataset.get(0, 2));
        assertEquals(50000.0, dataset.get(0, 3));
    }

    @Test
    public void testGetOutOfBounds() {
        assertThrows(IndexOutOfBoundsException.class, () -> dataset.get(10, 0));
        assertThrows(IndexOutOfBoundsException.class, () -> dataset.get(0, 10));
    }

    @Test
    public void testSetByRowAndColumn() {
        RowDataset ds = new RowDataset(columnNames, copyColumnList());
        ds.set(0, 1, "NewName");
        assertEquals("NewName", ds.get(0, 1));
    }

    @Test
    public void testSetNull() {
        RowDataset ds = new RowDataset(columnNames, copyColumnList());
        ds.set(0, 1, null);
        assertNull(ds.get(0, 1));
        assertTrue(ds.isNull(0, 1));
    }

    @Test
    public void testSetFrozen() {
        RowDataset ds = new RowDataset(columnNames, copyColumnList());
        ds.freeze();
        assertThrows(IllegalStateException.class, () -> {
            ds.set(0, 1, "NewName");
        });
    }

    @Test
    public void testIsNullByRowAndColumn() {
        List<List<Object>> cols = copyColumnList();
        cols.get(1).set(0, null);
        RowDataset ds = new RowDataset(columnNames, cols);
        assertTrue(ds.isNull(0, 1));
        assertFalse(ds.isNull(1, 1));
    }

    @Test
    public void testCurrentRowNum() {
        assertEquals(0, dataset.currentRowNum());
        dataset.absolute(2);
        assertEquals(2, dataset.currentRowNum());
    }

    @Test
    public void testAbsolute() {
        Dataset ds = dataset.absolute(3);
        assertNotNull(ds);
        assertEquals(3, ds.currentRowNum());
        assertEquals("Diana", ds.get("name"));
    }

    @Test
    public void testAbsoluteInvalid() {
        assertThrows(IndexOutOfBoundsException.class, () -> {
            dataset.absolute(10);
        });
        assertThrows(IndexOutOfBoundsException.class, () -> {
            dataset.absolute(-1);
        });
    }

    @Test
    public void testGetByColumnIndex() {
        dataset.absolute(0);
        assertEquals(1, (Integer) dataset.get(0));
        assertEquals("Alice", dataset.get(1));
        assertEquals(25, (Integer) dataset.get(2));
    }

    @Test
    public void testGetByColumnName() {
        dataset.absolute(1);
        assertEquals(2, (Integer) dataset.get("id"));
        assertEquals("Bob", dataset.get("name"));
        assertEquals(30, (Integer) dataset.get("age"));
    }

    @Test
    public void testGetPrimitiveTypes() {
        dataset.absolute(0);
        assertEquals(25, dataset.getInt(2));
        assertEquals(25, dataset.getInt("age"));
        assertEquals(50000.0, dataset.getDouble(3), 0.001);
        assertEquals(50000.0, dataset.getDouble("salary"), 0.001);
    }

    @Test
    public void testIsNullByColumnIndex() {
        List<List<Object>> cols = copyColumnList();
        cols.get(1).set(0, null);
        RowDataset ds = new RowDataset(columnNames, cols);
        ds.absolute(0);
        assertTrue(ds.isNull(1));
        ds.absolute(1);
        assertFalse(ds.isNull(1));
    }

    @Test
    public void testIsNullByColumnName() {
        List<List<Object>> cols = copyColumnList();
        cols.get(1).set(0, null);
        RowDataset ds = new RowDataset(columnNames, cols);
        ds.absolute(0);
        assertTrue(ds.isNull("name"));
        ds.absolute(1);
        assertFalse(ds.isNull("name"));
    }

    @Test
    public void testSetByColumnIndex() {
        RowDataset ds = new RowDataset(columnNames, copyColumnList());
        ds.absolute(0);
        ds.set(1, "NewName");
        assertEquals("NewName", ds.get(0, 1));
    }

    @Test
    public void testSetByColumnName() {
        RowDataset ds = new RowDataset(columnNames, copyColumnList());
        ds.absolute(0);
        ds.set("name", "NewName");
        assertEquals("NewName", ds.get(0, 1));
    }

    @Test
    public void testGetColumnByIndex() {
        ImmutableList<Object> names = dataset.getColumn(1);
        assertNotNull(names);
        assertEquals(5, names.size());
        assertEquals("Alice", names.get(0));
        assertEquals("Bob", names.get(1));
    }

    @Test
    public void testGetColumnByName() {
        ImmutableList<Object> ages = dataset.getColumn("age");
        assertNotNull(ages);
        assertEquals(5, ages.size());
        assertEquals(25, ages.get(0));
        assertEquals(30, ages.get(1));
    }

    @Test
    public void testCopyColumn() {
        List<Object> names = dataset.copyColumn("name");
        assertNotNull(names);
        assertEquals(5, names.size());
        names.set(0, "Modified");
        assertEquals("Modified", names.get(0));
        assertEquals("Alice", dataset.get(0, 1));
    }

    @Test
    public void testColumns() {
        Stream<ImmutableList<Object>> columnStream = dataset.columns();
        assertNotNull(columnStream);
        assertEquals(4, columnStream.count());
    }

    @Test
    public void testColumnMap() {
        Map<String, ImmutableList<Object>> colMap = dataset.columnMap();
        assertNotNull(colMap);
        assertEquals(4, colMap.size());
        assertTrue(colMap.containsKey("id"));
        assertEquals(5, colMap.get("id").size());
    }

    @Test
    public void testAddColumn() {
        RowDataset ds = new RowDataset(columnNames, copyColumnList());
        List<String> dept = Arrays.asList("IT", "HR", "Finance", "Sales", "IT");
        ds.addColumn("department", dept);
        assertEquals(5, ds.columnCount());
        assertTrue(ds.containsColumn("department"));
        assertEquals("IT", ds.get(0, 4));
    }

    @Test
    public void testAddColumnEmpty() {
        RowDataset ds = new RowDataset(columnNames, copyColumnList());
        ds.addColumn("newCol", new ArrayList<>());
        assertEquals(5, ds.columnCount());
        assertNull(ds.get(0, 4));
    }

    @Test
    public void testAddColumnWrongSize() {
        RowDataset ds = new RowDataset(columnNames, copyColumnList());
        List<String> dept = Arrays.asList("IT", "HR");
        assertThrows(IllegalArgumentException.class, () -> {
            ds.addColumn("department", dept);
        });
    }

    @Test
    public void testAddColumnDuplicateName() {
        RowDataset ds = new RowDataset(columnNames, copyColumnList());
        List<Integer> values = Arrays.asList(1, 2, 3, 4, 5);
        assertThrows(IllegalArgumentException.class, () -> {
            ds.addColumn("id", values);
        });
    }

    @Test
    public void testAddColumnAtPosition() {
        RowDataset ds = new RowDataset(columnNames, copyColumnList());
        List<String> dept = Arrays.asList("IT", "HR", "Finance", "Sales", "IT");
        ds.addColumn(1, "department", dept);
        assertEquals(5, ds.columnCount());
        assertEquals("department", ds.getColumnName(1));
        assertEquals("IT", ds.get(0, 1));
    }

    @Test
    public void testAddColumnWithFunction() {
        RowDataset ds = new RowDataset(columnNames, copyColumnList());
        ds.addColumn("age_plus_10", "age", (Integer age) -> age + 10);
        assertEquals(5, ds.columnCount());
        assertEquals(35, (Integer) ds.get(0, 4));
    }

    @Test
    public void testAddColumnWithBiFunction() {
        RowDataset ds = new RowDataset(columnNames, copyColumnList());
        ds.addColumn("name_age", Tuple.of("name", "age"), (String name, Integer age) -> name + ":" + age);
        assertEquals(5, ds.columnCount());
        assertEquals("Alice:25", ds.get(0, 4));
    }

    @Test
    public void testAddColumnWithTriFunction() {
        RowDataset ds = new RowDataset(columnNames, copyColumnList());
        ds.addColumn("combined", Tuple.of("id", "name", "age"), (Integer id, String name, Integer age) -> id + "-" + name + "-" + age);
        assertEquals(5, ds.columnCount());
        assertEquals("1-Alice-25", ds.get(0, 4));
    }

    @Test
    public void testAddColumns() {
        RowDataset ds = new RowDataset(columnNames, copyColumnList());
        List<String> depts = Arrays.asList("IT", "HR", "Finance", "Sales", "IT");
        List<Boolean> active = Arrays.asList(true, true, false, true, false);
        ds.addColumns(Arrays.asList("department", "active"), Arrays.asList(depts, active));
        assertEquals(6, ds.columnCount());
        assertEquals("IT", ds.get(0, 4));
        assertEquals(true, ds.get(0, 5));
    }

    @Test
    public void testRemoveColumn() {
        RowDataset ds = new RowDataset(columnNames, copyColumnList());
        List<Object> removed = ds.removeColumn("age");
        assertEquals(3, ds.columnCount());
        assertFalse(ds.containsColumn("age"));
        assertEquals(5, removed.size());
        assertEquals(25, removed.get(0));
    }

    @Test
    public void testRemoveColumnNonExistent() {
        RowDataset ds = new RowDataset(columnNames, copyColumnList());
        assertThrows(IllegalArgumentException.class, () -> {
            ds.removeColumn("nonexistent");
        });
    }

    @Test
    public void testRemoveColumns() {
        RowDataset ds = new RowDataset(columnNames, copyColumnList());
        ds.removeColumns(Arrays.asList("age", "salary"));
        assertEquals(2, ds.columnCount());
        assertTrue(ds.containsColumn("id"));
        assertTrue(ds.containsColumn("name"));
    }

    @Test
    public void testRemoveColumnsWithFilter() {
        RowDataset ds = new RowDataset(columnNames, copyColumnList());
        ds.removeColumns(name -> name.startsWith("a"));
        assertEquals(3, ds.columnCount());
        assertFalse(ds.containsColumn("age"));
    }

    @Test
    public void testUpdateColumn() {
        RowDataset ds = new RowDataset(columnNames, copyColumnList());
        ds.updateColumn("age", (Integer age) -> age + 1);
        assertEquals(26, (Integer) ds.get(0, 2));
        assertEquals(31, (Integer) ds.get(1, 2));
    }

    @Test
    public void testUpdateColumnWithNull() {
        List<List<Object>> cols = copyColumnList();
        cols.get(2).set(0, null);
        RowDataset ds = new RowDataset(columnNames, cols);
        ds.updateColumn("age", (Integer age) -> age == null ? 0 : age + 1);
        assertEquals(0, (Integer) ds.get(0, 2));
        assertEquals(31, (Integer) ds.get(1, 2));
    }

    @Test
    public void testUpdateColumns() {
        RowDataset ds = new RowDataset(columnNames, copyColumnList());
        ds.updateColumns(Arrays.asList("age", "salary"), (i, c, v) -> {
            if (c.equals("age")) {
                return ((Integer) v) + 1;
            } else {
                return ((Double) v) * 1.1;
            }
        });
        assertEquals(26, (Integer) ds.get(0, 2));
        assertEquals(55000.0, (Double) ds.get(0, 3), 1.0);
    }

    @Test
    public void testConvertColumn() {
        RowDataset ds = new RowDataset(columnNames, copyColumnList());
        ds.convertColumn("id", String.class);
        assertEquals("1", ds.get(0, 0));
    }

    @Test
    public void testConvertColumns() {
        RowDataset ds = new RowDataset(columnNames, copyColumnList());
        Map<String, Class<?>> typeMap = new HashMap<>();
        typeMap.put("id", String.class);
        typeMap.put("age", String.class);
        ds.convertColumns(typeMap);
        assertEquals("1", ds.get(0, 0));
        assertEquals("25", ds.get(0, 2));
    }

    @Test
    public void testCombineColumnsWithFunction() {
        RowDataset ds = new RowDataset(columnNames, copyColumnList());
        ds.combineColumns(Arrays.asList("name", "age"), "info", row -> row.get(0) + " is " + row.get(1));
        assertTrue(ds.containsColumn("info"));
        assertEquals("Alice is 25", ds.get(0, ds.getColumnIndex("info")));
    }

    @Test
    public void testCombineColumnsWithBiFunction() {
        RowDataset ds = new RowDataset(columnNames, copyColumnList());
        ds.combineColumns(Tuple.of("name", "age"), "combined", (String name, Integer age) -> name + ":" + age);
        assertTrue(ds.containsColumn("combined"));
        assertFalse(ds.containsColumn("name"));
    }

    @Test
    public void testDivideColumnWithFunction() {
        List<String> names = CommonUtil.asList("fullName");
        List<List<Object>> cols = new ArrayList<>();
        cols.add(new ArrayList<>(CommonUtil.asList("John Doe", "Jane Smith")));
        RowDataset ds = new RowDataset(names, cols);
        ds.divideColumn("fullName", CommonUtil.asList("firstName", "lastName"), (String fullName) -> CommonUtil.asList(fullName.split(" ")));
        assertTrue(ds.containsColumn("firstName"));
        assertTrue(ds.containsColumn("lastName"));
        assertFalse(ds.containsColumn("fullName"));
        assertEquals("John", ds.get(0, 0));
        assertEquals("Doe", ds.get(0, 1));
    }

    @Test
    public void testDivideColumnWithBiConsumer() {
        List<String> names = CommonUtil.asList("fullName");
        List<List<Object>> cols = new ArrayList<>();
        cols.add(new ArrayList<>(CommonUtil.asList("John Doe", "Jane Smith")));
        RowDataset ds = new RowDataset(names, cols);
        ds.divideColumn("fullName", Arrays.asList("firstName", "lastName"), (String fullName, Object[] output) -> {
            String[] parts = fullName.split(" ");
            output[0] = parts[0];
            output[1] = parts[1];
        });
        assertEquals("John", ds.get(0, 0));
        assertEquals("Doe", ds.get(0, 1));
    }

    @Test
    public void testAddRowAsArray() {
        RowDataset ds = new RowDataset(columnNames, copyColumnList());
        Object[] newRow = new Object[] { 6, "Frank", 40, 80000.0 };
        ds.addRow(newRow);
        assertEquals(6, ds.size());
        assertEquals(6, (Integer) ds.get(5, 0));
        assertEquals("Frank", ds.get(5, 1));
    }

    @Test
    public void testAddRowAsList() {
        RowDataset ds = new RowDataset(columnNames, copyColumnList());
        List<Object> newRow = Arrays.asList(6, "Frank", 40, 80000.0);
        ds.addRow(newRow);
        assertEquals(6, ds.size());
        assertEquals(6, (Integer) ds.get(5, 0));
    }

    @Test
    public void testAddRowAsMap() {
        RowDataset ds = new RowDataset(columnNames, copyColumnList());
        Map<String, Object> newRow = new LinkedHashMap<>();
        newRow.put("id", 6);
        newRow.put("name", "Frank");
        newRow.put("age", 40);
        newRow.put("salary", 80000.0);
        ds.addRow(newRow);
        assertEquals(6, ds.size());
        assertEquals(6, (Integer) ds.get(5, 0));
    }

    @Test
    public void testAddRowAtPosition() {
        RowDataset ds = new RowDataset(columnNames, copyColumnList());
        Object[] newRow = new Object[] { 6, "Frank", 40, 80000.0 };
        ds.addRow(0, newRow);
        assertEquals(6, ds.size());
        assertEquals(6, (Integer) ds.get(0, 0));
        assertEquals("Frank", ds.get(0, 1));
    }

    @Test
    public void testAddRowWrongSize() {
        RowDataset ds = new RowDataset(columnNames, copyColumnList());
        Object[] newRow = new Object[] { 6, "Frank" };
        assertThrows(IllegalArgumentException.class, () -> {
            ds.addRow(newRow);
        });
    }

    @Test
    public void testAddRows() {
        RowDataset ds = new RowDataset(columnNames, copyColumnList());
        List<Object[]> newRows = Arrays.asList(new Object[] { 6, "Frank", 40, 80000.0 }, new Object[] { 7, "Grace", 45, 90000.0 });
        ds.addRows(newRows);
        assertEquals(7, ds.size());
        assertEquals(6, (Integer) ds.get(5, 0));
        assertEquals(7, (Integer) ds.get(6, 0));
    }

    @Test
    public void testRemoveRow() {
        RowDataset ds = new RowDataset(columnNames, copyColumnList());
        ds.removeRow(0);
        assertEquals(4, ds.size());
        assertEquals(2, (Integer) ds.get(0, 0));
    }

    @Test
    public void testRemoveRowOutOfBounds() {
        RowDataset ds = new RowDataset(columnNames, copyColumnList());
        assertThrows(IndexOutOfBoundsException.class, () -> {
            ds.removeRow(10);
        });
    }

    @Test
    public void testRemoveRows() {
        RowDataset ds = new RowDataset(columnNames, copyColumnList());
        ds.removeMultiRows(0, 2);
        assertEquals(3, ds.size());
        assertEquals(2, (Integer) ds.get(0, 0));
    }

    @Test
    public void testRemoveRowRange() {
        RowDataset ds = new RowDataset(columnNames, copyColumnList());
        ds.removeRows(0, 2);
        assertEquals(3, ds.size());
        assertEquals(3, (Integer) ds.get(0, 0));
    }

    @Test
    public void testRemoveDuplicateRowsBy() {
        List<List<Object>> cols = new ArrayList<>();
        cols.add(new ArrayList<>(Arrays.asList(1, 1, 2, 3)));
        cols.add(new ArrayList<>(Arrays.asList("A", "B", "C", "D")));
        List<String> names = Arrays.asList("id", "name");
        RowDataset ds = new RowDataset(names, cols);

        ds.removeDuplicateRowsBy("id");
        assertEquals(3, ds.size());
    }

    @Test
    public void testUpdateRow() {
        RowDataset ds = new RowDataset(columnNames, copyColumnList());
        ds.updateRow(0, (Object val) -> {
            if (val instanceof String) {
                return ((String) val).toUpperCase();
            }
            return val;
        });
        assertEquals("ALICE", ds.get(0, 1));
    }

    @Test
    public void testUpdateAll() {
        RowDataset ds = new RowDataset(columnNames, copyColumnList());
        ds.updateAll((Object val) -> {
            if (val instanceof Integer) {
                return ((Integer) val) + 1;
            }
            return val;
        });
        assertEquals(2, (Integer) ds.get(0, 0));
        assertEquals(26, (Integer) ds.get(0, 2));
    }

    @Test
    public void testReplaceIf() {
        RowDataset ds = new RowDataset(columnNames, copyColumnList());
        ds.replaceIf(val -> val instanceof Integer && (Integer) val > 30, 999);
        assertEquals(25, (Integer) ds.get(0, 2));
        assertEquals(999, (Integer) ds.get(2, 2));
    }

    @Test
    public void testMerge() {
        RowDataset ds1 = new RowDataset(columnNames, copyColumnList());
        RowDataset ds2 = new RowDataset(columnNames, copyColumnList());
        int originalSize = ds1.size();
        ds1.merge(ds2);
        assertEquals(originalSize * 2, ds1.size());
    }

    @Test
    public void testMergeWithDifferentColumns() {
        RowDataset ds1 = new RowDataset(columnNames, copyColumnList());
        List<String> otherNames = Arrays.asList("id", "name", "department");
        List<List<Object>> otherCols = new ArrayList<>();
        otherCols.add(new ArrayList<>(Arrays.asList(6, 7)));
        otherCols.add(new ArrayList<>(Arrays.asList("Frank", "Grace")));
        otherCols.add(new ArrayList<>(Arrays.asList("IT", "HR")));
        RowDataset ds2 = new RowDataset(otherNames, otherCols);

        ds1.merge(ds2, Arrays.asList("id", "name"));
        assertEquals(7, ds1.size());
        assertTrue(ds1.containsColumn("id"));
        assertTrue(ds1.containsColumn("name"));
    }

    @Test
    public void testPrepend() {
        RowDataset ds1 = new RowDataset(columnNames, copyColumnList());
        RowDataset ds2 = new RowDataset(columnNames, copyColumnList());
        Object firstValue = ds1.get(0, 0);
        ds1.prepend(ds2);
        assertNotNull(ds1.get(ds2.size(), 0));
    }

    @Test
    public void testAppend() {
        RowDataset ds1 = new RowDataset(columnNames, copyColumnList());
        RowDataset ds2 = new RowDataset(columnNames, copyColumnList());
        int originalSize = ds1.size();
        ds1.append(ds2);
        assertEquals(originalSize * 2, ds1.size());
    }

    @Test
    public void testGetRow() {
        Object[] row = dataset.getRow(0);
        assertNotNull(row);
        assertEquals(4, row.length);
        assertEquals(1, row[0]);
        assertEquals("Alice", row[1]);
    }

    @Test
    public void testGetRowAsMap() {
        Map<String, Object> row = dataset.getRow(0, Map.class);
        assertNotNull(row);
        assertEquals(4, row.size());
        assertEquals(1, row.get("id"));
        assertEquals("Alice", row.get("name"));
    }

    @Test
    public void testGetRowAsList() {
        List<Object> row = dataset.getRow(0, List.class);
        assertNotNull(row);
        assertEquals(4, row.size());
        assertEquals(1, row.get(0));
        assertEquals("Alice", row.get(1));
    }

    @Test
    public void testFirstRow() {
        Optional<Object[]> firstRow = dataset.firstRow();
        assertTrue(firstRow.isPresent());
        assertEquals(1, firstRow.get()[0]);
    }

    @Test
    public void testFirstRowEmpty() {
        Optional<Object[]> firstRow = emptyDataset.firstRow();
        assertFalse(firstRow.isPresent());
    }

    @Test
    public void testLastRow() {
        Optional<Object[]> lastRow = dataset.lastRow();
        assertTrue(lastRow.isPresent());
        assertEquals(5, lastRow.get()[0]);
    }

    @Test
    public void testLastRowEmpty() {
        Optional<Object[]> lastRow = emptyDataset.lastRow();
        assertFalse(lastRow.isPresent());
    }

    @Test
    public void testForEach() {
        RowDataset ds = new RowDataset(columnNames, copyColumnList());
        final int[] count = { 0 };
        ds.forEach(row -> {
            assertNotNull(row);
            assertEquals(4, row.length());
            count[0]++;
        });
        assertEquals(5, count[0]);
    }

    @Test
    public void testForEachWithColumnNames() {
        RowDataset ds = new RowDataset(columnNames, copyColumnList());
        final int[] count = { 0 };
        ds.forEach(Arrays.asList("id", "name"), row -> {
            assertNotNull(row);
            assertEquals(2, row.length());
            count[0]++;
        });
        assertEquals(5, count[0]);
    }

    @Test
    public void testIterator() {
        BiIterator<Integer, String> iter = dataset.iterator("id", "name");
        assertNotNull(iter);
        assertTrue(iter.hasNext());
        Pair<Integer, String> first = iter.next();
        assertEquals(1, first.left().intValue());
        assertEquals("Alice", first.right());
    }

    @Test
    public void testTriIterator() {
        TriIterator<Integer, String, Integer> iter = dataset.iterator("id", "name", "age");
        assertNotNull(iter);
        assertTrue(iter.hasNext());
        Triple<Integer, String, Integer> first = iter.next();
        assertEquals(1, first.left().intValue());
        assertEquals("Alice", first.middle());
        assertEquals(25, first.right().intValue());
    }

    @Test
    public void testFreeze() {
        RowDataset ds = new RowDataset(columnNames, copyColumnList());
        assertFalse(ds.isFrozen());
        ds.freeze();
        assertTrue(ds.isFrozen());
    }

    @Test
    public void testFreezeIdempotent() {
        RowDataset ds = new RowDataset(columnNames, copyColumnList());
        ds.freeze();
        ds.freeze();
        assertTrue(ds.isFrozen());
    }

    @Test
    public void testClear() {
        RowDataset ds = new RowDataset(columnNames, copyColumnList());
        assertFalse(ds.isEmpty());
        ds.clear();
        assertTrue(ds.isEmpty());
        assertEquals(0, ds.size());
        assertEquals(4, ds.columnCount());
    }

    @Test
    public void testClearFrozen() {
        RowDataset ds = new RowDataset(columnNames, copyColumnList());
        ds.freeze();
        assertThrows(IllegalStateException.class, () -> {
            ds.clear();
        });
    }

    @Test
    public void testIsEmpty() {
        assertFalse(dataset.isEmpty());
        assertTrue(emptyDataset.isEmpty());
    }

    @Test
    public void testSize() {
        assertEquals(5, dataset.size());
        assertEquals(0, emptyDataset.size());
    }

    @Test
    public void testTrimToSize() {
        RowDataset ds = new RowDataset(columnNames, copyColumnList());
        ds.removeRow(0);
        ds.trimToSize();
        assertEquals(4, ds.size());
    }

    @Test
    public void testGetProperties() {
        Map<String, Object> props = dataset.getProperties();
        assertNotNull(props);
    }

    @Test
    public void testSetProperties() {
        RowDataset ds = new RowDataset(columnNames, copyColumnList());
        Map<String, Object> props = new HashMap<>();
        props.put("key1", "value1");
        props.put("key2", 123);
        ds.setProperties(props);
        assertEquals("value1", ds.getProperties().get("key1"));
        assertEquals(123, ds.getProperties().get("key2"));
    }

    @Test
    public void testSetPropertiesNull() {
        RowDataset ds = new RowDataset(columnNames, copyColumnList());
        ds.setProperties(null);
        assertNotNull(ds.getProperties());
        assertTrue(ds.getProperties().isEmpty());
    }

    @Test
    public void testGetProperty() {
        Map<String, Object> props = new HashMap<>();
        props.put("key1", "value1");
        RowDataset ds = new RowDataset(columnNames, copyColumnList(), props);
        assertEquals("value1", ds.getProperties().get("key1"));
    }

    @Test
    public void testCopy() {
        Dataset copy = dataset.copy();
        assertNotNull(copy);
        assertEquals(dataset.size(), copy.size());
        assertEquals(dataset.columnCount(), copy.columnCount());

        if (copy instanceof RowDataset) {
            ((RowDataset) copy).set(0, 0, 999);
            assertNotNull(dataset.get(0, 0));
            assertTrue(!dataset.get(0, 0).equals(999));
        }
    }

    @Test
    public void testCopyWithRowRange() {
        Dataset copy = dataset.copy(1, 3);
        assertNotNull(copy);
        assertEquals(2, copy.size());
        assertEquals(dataset.columnCount(), copy.columnCount());
    }

    @Test
    public void testCopyWithColumnNames() {
        Dataset copy = dataset.copy(Arrays.asList("id", "name"));
        assertNotNull(copy);
        assertEquals(dataset.size(), copy.size());
        assertEquals(2, copy.columnCount());
    }

    @Test
    public void testClone() {
        Dataset clone = dataset.clone();
        assertNotNull(clone);
        assertEquals(dataset.size(), clone.size());
        assertEquals(dataset.columnCount(), clone.columnCount());
    }

    @Test
    public void testToString() {
        String str = dataset.toString();
        assertNotNull(str);
        assertTrue(str.length() > 0);
    }

    @Test
    public void testHashCode() {
        int hash1 = dataset.hashCode();
        int hash2 = dataset.hashCode();
        assertEquals(hash1, hash2);
    }

    @Test
    public void testEquals() {
        Dataset copy = dataset.copy();
        boolean result = dataset.equals(copy);
    }

    private List<List<Object>> copyColumnList() {
        List<List<Object>> copy = new ArrayList<>();
        for (List<Object> column : columnList) {
            copy.add(new ArrayList<>(column));
        }
        return copy;
    }
}

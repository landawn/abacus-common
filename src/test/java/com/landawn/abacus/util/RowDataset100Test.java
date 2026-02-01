package com.landawn.abacus.util;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.Predicate;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.NoCachingNoUpdating.DisposableObjArray;
import com.landawn.abacus.util.Tuple.Tuple2;
import com.landawn.abacus.util.Tuple.Tuple3;
import com.landawn.abacus.util.u.Optional;
import com.landawn.abacus.util.function.TriConsumer;
import com.landawn.abacus.util.function.TriFunction;

@Tag("new-test")
public class RowDataset100Test extends TestBase {

    private RowDataset dataset;
    private List<String> columnNames;
    private List<List<Object>> columns;

    @BeforeEach
    public void setUp() {
        columnNames = CommonUtil.asList("id", "name", "age", "score");
        columns = new ArrayList<>();
        columns.add(CommonUtil.asList(1, 2, 3));
        columns.add(CommonUtil.asList("John", "Jane", "Bob"));
        columns.add(CommonUtil.asList(25, 30, 35));
        columns.add(CommonUtil.asList(85.5, 90.0, 88.5));
        dataset = new RowDataset(columnNames, columns);
    }

    @Test
    public void testConstructorWithColumnNamesAndColumns() {
        RowDataset ds = new RowDataset(columnNames, columns);
        Assertions.assertNotNull(ds);
        Assertions.assertEquals(4, ds.columnCount());
        Assertions.assertEquals(3, ds.size());
    }

    @Test
    public void testConstructorWithProperties() {
        Map<String, Object> properties = new HashMap<>();
        properties.put("key1", "value1");
        properties.put("key2", 123);

        RowDataset ds = new RowDataset(columnNames, columns, properties);
        Assertions.assertNotNull(ds);
        Assertions.assertEquals(4, ds.columnCount());
        Assertions.assertEquals(3, ds.size());
    }

    @Test
    public void testConstructorWithNullColumnNames() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            new RowDataset(null, columns);
        });
    }

    @Test
    public void testConstructorWithNullColumns() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            new RowDataset(columnNames, null);
        });
    }

    @Test
    public void testConstructorWithEmptyColumnName() {
        List<String> badColumnNames = Arrays.asList("id", "", "age");
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            new RowDataset(badColumnNames, columns);
        });
    }

    @Test
    public void testConstructorWithDuplicateColumnNames() {
        List<String> duplicateNames = Arrays.asList("id", "name", "id");
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            new RowDataset(duplicateNames, columns);
        });
    }

    @Test
    public void testConstructorWithMismatchedSizes() {
        List<String> shortColumnNames = Arrays.asList("id", "name");
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            new RowDataset(shortColumnNames, columns);
        });
    }

    @Test
    public void testConstructorWithInconsistentColumnSizes() {
        List<List<Object>> badColumns = new ArrayList<>();
        badColumns.add(Arrays.asList(1, 2));
        badColumns.add(Arrays.asList("John", "Jane", "Bob"));

        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            new RowDataset(Arrays.asList("id", "name"), badColumns);
        });
    }

    @Test
    public void testColumnNameList() {
        ImmutableList<String> names = dataset.columnNames();
        Assertions.assertEquals(4, names.size());
        Assertions.assertEquals("id", names.get(0));
        Assertions.assertEquals("name", names.get(1));
        Assertions.assertEquals("age", names.get(2));
        Assertions.assertEquals("score", names.get(3));
    }

    @Test
    public void testColumnCount() {
        Assertions.assertEquals(4, dataset.columnCount());
    }

    @Test
    public void testGetColumnName() {
        Assertions.assertEquals("id", dataset.getColumnName(0));
        Assertions.assertEquals("name", dataset.getColumnName(1));
        Assertions.assertEquals("age", dataset.getColumnName(2));
        Assertions.assertEquals("score", dataset.getColumnName(3));
    }

    @Test
    public void testGetColumnNameWithInvalidIndex() {
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> {
            dataset.getColumnName(-1);
        });
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> {
            dataset.getColumnName(4);
        });
    }

    @Test
    public void testGetColumnIndex() {
        Assertions.assertEquals(0, dataset.getColumnIndex("id"));
        Assertions.assertEquals(1, dataset.getColumnIndex("name"));
        Assertions.assertEquals(2, dataset.getColumnIndex("age"));
        Assertions.assertEquals(3, dataset.getColumnIndex("score"));
    }

    @Test
    public void testGetColumnIndexWithInvalidName() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            dataset.getColumnIndex("invalid");
        });
    }

    @Test
    public void testGetColumnIndexes() {
        int[] indexes = dataset.getColumnIndexes(Arrays.asList("id", "age"));
        Assertions.assertArrayEquals(new int[] { 0, 2 }, indexes);

        indexes = dataset.getColumnIndexes(Arrays.asList("name", "score"));
        Assertions.assertArrayEquals(new int[] { 1, 3 }, indexes);

        indexes = dataset.getColumnIndexes(Collections.emptyList());
        Assertions.assertArrayEquals(new int[] {}, indexes);
    }

    @Test
    public void testGetColumnIndexesWithInvalidName() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            dataset.getColumnIndexes(Arrays.asList("id", "invalid"));
        });
    }

    @Test
    public void testContainsColumn() {
        Assertions.assertTrue(dataset.containsColumn("id"));
        Assertions.assertTrue(dataset.containsColumn("name"));
        Assertions.assertTrue(dataset.containsColumn("age"));
        Assertions.assertTrue(dataset.containsColumn("score"));
        Assertions.assertFalse(dataset.containsColumn("invalid"));
    }

    @Test
    public void testContainsAllColumns() {
        Assertions.assertTrue(dataset.containsAllColumns(Arrays.asList("id", "name")));
        Assertions.assertTrue(dataset.containsAllColumns(Arrays.asList("age", "score")));
        Assertions.assertTrue(dataset.containsAllColumns(columnNames));
        Assertions.assertFalse(dataset.containsAllColumns(Arrays.asList("id", "invalid")));
        Assertions.assertTrue(dataset.containsAllColumns(Collections.emptyList()));
    }

    @Test
    public void testRenameColumn() {
        dataset.renameColumn("id", "user_id");
        Assertions.assertTrue(dataset.containsColumn("user_id"));
        Assertions.assertFalse(dataset.containsColumn("id"));
        Assertions.assertEquals(0, dataset.getColumnIndex("user_id"));
    }

    @Test
    public void testRenameColumnWithSameName() {
        dataset.renameColumn("id", "id");
        Assertions.assertTrue(dataset.containsColumn("id"));
    }

    @Test
    public void testRenameColumnWithExistingName() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            dataset.renameColumn("id", "name");
        });
    }

    @Test
    public void testRenameColumnWithInvalidName() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            dataset.renameColumn("invalid", "new_name");
        });
    }

    @Test
    public void testRenameColumns() {
        Map<String, String> oldNewNames = new HashMap<>();
        oldNewNames.put("id", "user_id");
        oldNewNames.put("name", "user_name");

        dataset.renameColumns(oldNewNames);
        Assertions.assertTrue(dataset.containsColumn("user_id"));
        Assertions.assertTrue(dataset.containsColumn("user_name"));
        Assertions.assertFalse(dataset.containsColumn("id"));
        Assertions.assertFalse(dataset.containsColumn("name"));
    }

    @Test
    public void testRenameColumnsWithDuplicateNewNames() {
        Map<String, String> oldNewNames = new HashMap<>();
        oldNewNames.put("id", "same_name");
        oldNewNames.put("name", "same_name");

        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            dataset.renameColumns(oldNewNames);
        });
    }

    @Test
    public void testRenameColumnsWithFunction() {
        dataset.renameColumns(Arrays.asList("id", "name"), col -> col.toUpperCase());
        Assertions.assertTrue(dataset.containsColumn("ID"));
        Assertions.assertTrue(dataset.containsColumn("NAME"));
        Assertions.assertFalse(dataset.containsColumn("id"));
        Assertions.assertFalse(dataset.containsColumn("name"));
    }

    @Test
    public void testRenameAllColumnsWithFunction() {
        dataset.renameColumns(col -> col + "_renamed");
        Assertions.assertTrue(dataset.containsColumn("id_renamed"));
        Assertions.assertTrue(dataset.containsColumn("name_renamed"));
        Assertions.assertTrue(dataset.containsColumn("age_renamed"));
        Assertions.assertTrue(dataset.containsColumn("score_renamed"));
    }

    @Test
    public void testMoveColumn() {
        dataset.moveColumn("id", 2);
        List<String> names = dataset.columnNames();
        Assertions.assertEquals("name", names.get(0));
        Assertions.assertEquals("age", names.get(1));
        Assertions.assertEquals("id", names.get(2));
        Assertions.assertEquals("score", names.get(3));
    }

    @Test
    public void testMoveColumnToSamePosition() {
        dataset.moveColumn("id", 0);
        List<String> names = dataset.columnNames();
        Assertions.assertEquals("id", names.get(0));
    }

    @Test
    public void testMoveColumnWithInvalidPosition() {
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> {
            dataset.moveColumn("id", -1);
        });
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> {
            dataset.moveColumn("id", 5);
        });
    }

    @Test
    public void testSwapColumnPosition() {
        dataset.swapColumns("id", "score");
        List<String> names = dataset.columnNames();
        Assertions.assertEquals("score", names.get(0));
        Assertions.assertEquals("name", names.get(1));
        Assertions.assertEquals("age", names.get(2));
        Assertions.assertEquals("id", names.get(3));
    }

    @Test
    public void testSwapColumnPositionWithSameColumn() {
        dataset.swapColumns("id", "id");
        List<String> names = dataset.columnNames();
        Assertions.assertEquals("id", names.get(0));
    }

    @Test
    public void testMoveRow() {
        Object originalRow1Name = dataset.get(1, 1);
        dataset.moveRow(1, 0);
        Assertions.assertEquals(originalRow1Name, dataset.get(0, 1));
    }

    @Test
    public void testMoveRowToSamePosition() {
        Object originalRow1Name = dataset.get(1, 1);
        dataset.moveRow(1, 1);
        Assertions.assertEquals(originalRow1Name, dataset.get(1, 1));
    }

    @Test
    public void testSwapRowPosition() {
        Object row0Name = dataset.get(0, 1);
        Object row2Name = dataset.get(2, 1);

        dataset.swapRows(0, 2);
        Assertions.assertEquals(row2Name, dataset.get(0, 1));
        Assertions.assertEquals(row0Name, dataset.get(2, 1));
    }

    @Test
    public void testSwapRowPositionWithSameRow() {
        Object row0Name = dataset.get(0, 1);
        dataset.swapRows(0, 0);
        Assertions.assertEquals(row0Name, dataset.get(0, 1));
    }

    @Test
    public void testGet() {
        Assertions.assertEquals(1, (Integer) dataset.get(0, 0));
        Assertions.assertEquals("John", dataset.get(0, 1));
        Assertions.assertEquals(25, (Integer) dataset.get(0, 2));
        Assertions.assertEquals(85.5, dataset.get(0, 3));
    }

    @Test
    public void testSet() {
        dataset.set(0, 0, 100);
        Assertions.assertEquals(100, (Integer) dataset.get(0, 0));

        dataset.set(1, 1, "Updated");
        Assertions.assertEquals("Updated", dataset.get(1, 1));
    }

    @Test
    public void testIsNullWithRowAndColumn() {
        dataset.set(0, 0, null);
        Assertions.assertTrue(dataset.isNull(0, 0));
        Assertions.assertFalse(dataset.isNull(0, 1));
    }

    @Test
    public void testGetByColumnIndex() {
        dataset.moveToRow(0);
        Assertions.assertEquals(1, (Integer) dataset.get(0));
        Assertions.assertEquals("John", dataset.get(1));
        Assertions.assertEquals(25, (Integer) dataset.get(2));
        Assertions.assertEquals(85.5, dataset.get(3));
    }

    @Test
    public void testGetByColumnName() {
        dataset.moveToRow(0);
        Assertions.assertEquals(1, (Integer) dataset.get("id"));
        Assertions.assertEquals("John", dataset.get("name"));
        Assertions.assertEquals(25, (Integer) dataset.get("age"));
        Assertions.assertEquals(85.5, dataset.get("score"));
    }

    @Test
    public void testGetBoolean() {
        List<List<Object>> boolColumns = new ArrayList<>();
        boolColumns.add(Arrays.asList(true, false, true));
        RowDataset boolDataset = new RowDataset(Arrays.asList("flag"), boolColumns);

        boolDataset.moveToRow(0);
        Assertions.assertTrue(boolDataset.getBoolean(0));
        Assertions.assertTrue(boolDataset.getBoolean("flag"));

        boolDataset.moveToRow(1);
        Assertions.assertFalse(boolDataset.getBoolean(0));
        Assertions.assertFalse(boolDataset.getBoolean("flag"));
    }

    @Test
    public void testGetChar() {
        List<List<Object>> charColumns = new ArrayList<>();
        charColumns.add(Arrays.asList('A', 'B', 'C'));
        RowDataset charDataset = new RowDataset(Arrays.asList("letter"), charColumns);

        charDataset.moveToRow(0);
        Assertions.assertEquals('A', charDataset.getChar(0));
        Assertions.assertEquals('A', charDataset.getChar("letter"));
    }

    @Test
    public void testGetByte() {
        dataset.moveToRow(0);
        Assertions.assertEquals((byte) 1, dataset.getByte(0));
        Assertions.assertEquals((byte) 1, dataset.getByte("id"));
    }

    @Test
    public void testGetShort() {
        dataset.moveToRow(0);
        Assertions.assertEquals((short) 1, dataset.getShort(0));
        Assertions.assertEquals((short) 1, dataset.getShort("id"));
    }

    @Test
    public void testGetInt() {
        dataset.moveToRow(0);
        Assertions.assertEquals(1, dataset.getInt(0));
        Assertions.assertEquals(1, dataset.getInt("id"));
        Assertions.assertEquals(25, dataset.getInt(2));
        Assertions.assertEquals(25, dataset.getInt("age"));
    }

    @Test
    public void testGetLong() {
        dataset.moveToRow(0);
        Assertions.assertEquals(1L, dataset.getLong(0));
        Assertions.assertEquals(1L, dataset.getLong("id"));
    }

    @Test
    public void testGetFloat() {
        dataset.moveToRow(0);
        Assertions.assertEquals(85.5f, dataset.getFloat(3), 0.01f);
        Assertions.assertEquals(85.5f, dataset.getFloat("score"), 0.01f);
    }

    @Test
    public void testGetDouble() {
        dataset.moveToRow(0);
        Assertions.assertEquals(85.5, dataset.getDouble(3), 0.01);
        Assertions.assertEquals(85.5, dataset.getDouble("score"), 0.01);
    }

    @Test
    public void testIsNull() {
        dataset.moveToRow(0);
        dataset.set(0, null);
        Assertions.assertTrue(dataset.isNull(0));
        Assertions.assertTrue(dataset.isNull("id"));
        Assertions.assertFalse(dataset.isNull(1));
        Assertions.assertFalse(dataset.isNull("name"));
    }

    @Test
    public void testSetByColumnIndex() {
        dataset.moveToRow(0);
        dataset.set(0, 999);
        Assertions.assertEquals(999, (Integer) dataset.get(0));
    }

    @Test
    public void testSetByColumnName() {
        dataset.moveToRow(0);
        dataset.set("name", "Updated Name");
        Assertions.assertEquals("Updated Name", dataset.get("name"));
    }

    @Test
    public void testGetColumn() {
        ImmutableList<Object> idColumn = dataset.getColumn(0);
        Assertions.assertEquals(3, idColumn.size());
        Assertions.assertEquals(1, idColumn.get(0));
        Assertions.assertEquals(2, idColumn.get(1));
        Assertions.assertEquals(3, idColumn.get(2));

        ImmutableList<Object> nameColumn = dataset.getColumn("name");
        Assertions.assertEquals(3, nameColumn.size());
        Assertions.assertEquals("John", nameColumn.get(0));
        Assertions.assertEquals("Jane", nameColumn.get(1));
        Assertions.assertEquals("Bob", nameColumn.get(2));
    }

    @Test
    public void testCopyColumn() {
        List<Object> copiedColumn = dataset.copyColumn("name");
        Assertions.assertEquals(3, copiedColumn.size());
        Assertions.assertEquals("John", copiedColumn.get(0));
        Assertions.assertEquals("Jane", copiedColumn.get(1));
        Assertions.assertEquals("Bob", copiedColumn.get(2));

        copiedColumn.set(0, "Modified");
        Assertions.assertEquals("John", dataset.get(0, 1));
    }

    @Test
    public void testAddColumn() {
        dataset.addColumn("status", Arrays.asList("Active", "Inactive", "Active"));
        Assertions.assertEquals(5, dataset.columnCount());
        Assertions.assertTrue(dataset.containsColumn("status"));
        Assertions.assertEquals("Active", dataset.get(0, 4));
    }

    @Test
    public void testAddColumnAtPosition() {
        dataset.addColumn(2, "status", Arrays.asList("Active", "Inactive", "Active"));
        Assertions.assertEquals(5, dataset.columnCount());
        Assertions.assertEquals("status", dataset.getColumnName(2));
        Assertions.assertEquals("Active", dataset.get(0, 2));
    }

    @Test
    public void testAddColumnWithEmptyCollection() {
        dataset.addColumn("empty", Collections.emptyList());
        Assertions.assertEquals(5, dataset.columnCount());
        Assertions.assertTrue(dataset.containsColumn("empty"));
        Assertions.assertNull(dataset.get(0, 4));
    }

    @Test
    public void testAddColumnWithWrongSize() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            dataset.addColumn("bad", Arrays.asList("One", "Two"));
        });
    }

    @Test
    public void testAddColumnWithFunction() {
        dataset.addColumn("double_age", "age", (Function<Integer, Integer>) age -> age * 2);
        Assertions.assertEquals(5, dataset.columnCount());
        Assertions.assertTrue(dataset.containsColumn("double_age"));
        Assertions.assertEquals(50, (Integer) dataset.get(0, 4));
        Assertions.assertEquals(60, (Integer) dataset.get(1, 4));
        Assertions.assertEquals(70, (Integer) dataset.get(2, 4));
    }

    @Test
    public void testAddColumnWithMultipleColumnsAndFunction() {
        dataset.addColumn("full_info", Arrays.asList("name", "age"), (Function<DisposableObjArray, String>) arr -> arr.get(0) + " (" + arr.get(1) + ")");
        Assertions.assertEquals(5, dataset.columnCount());
        Assertions.assertTrue(dataset.containsColumn("full_info"));
        Assertions.assertEquals("John (25)", dataset.get(0, 4));
    }

    @Test
    public void testAddColumnWithTuple2() {
        dataset.addColumn("name_age", new Tuple2<>("name", "age"), (BiFunction<String, Integer, String>) (name, age) -> name + "-" + age);
        Assertions.assertEquals(5, dataset.columnCount());
        Assertions.assertEquals("John-25", dataset.get(0, 4));
    }

    @Test
    public void testAddColumnWithTuple3() {
        dataset.addColumn("summary", new Tuple3<>("id", "name", "age"),
                (TriFunction<Integer, String, Integer, String>) (id, name, age) -> "ID:" + id + ",Name:" + name + ",Age:" + age);
        Assertions.assertEquals(5, dataset.columnCount());
        Assertions.assertEquals("ID:1,Name:John,Age:25", dataset.get(0, 4));
    }

    @Test
    public void testRemoveColumn() {
        List<Object> removedColumn = dataset.removeColumn("age");
        Assertions.assertEquals(3, dataset.columnCount());
        Assertions.assertFalse(dataset.containsColumn("age"));
        Assertions.assertEquals(3, removedColumn.size());
        Assertions.assertEquals(25, removedColumn.get(0));
    }

    @Test
    public void testRemoveColumns() {
        dataset.removeColumns(Arrays.asList("age", "score"));
        Assertions.assertEquals(2, dataset.columnCount());
        Assertions.assertFalse(dataset.containsColumn("age"));
        Assertions.assertFalse(dataset.containsColumn("score"));
    }

    @Test
    public void testRemoveColumnsWithEmptyList() {
        dataset.removeColumns(Collections.emptyList());
        Assertions.assertEquals(4, dataset.columnCount());
    }

    @Test
    public void testRemoveColumnsWithPredicate() {
        dataset.removeColumns(col -> col.startsWith("a"));
        Assertions.assertEquals(3, dataset.columnCount());
        Assertions.assertFalse(dataset.containsColumn("age"));
        Assertions.assertTrue(dataset.containsColumn("id"));
        Assertions.assertTrue(dataset.containsColumn("name"));
        Assertions.assertTrue(dataset.containsColumn("score"));
    }

    @Test
    public void testConvertColumn() {
        dataset.convertColumn("id", String.class);
        Assertions.assertEquals("1", dataset.get(0, 0));
        Assertions.assertEquals("2", dataset.get(1, 0));
        Assertions.assertEquals("3", dataset.get(2, 0));
    }

    @Test
    public void testConvertColumns() {
        Map<String, Class<?>> conversions = new HashMap<>();
        conversions.put("id", String.class);
        conversions.put("age", Double.class);

        dataset.convertColumns(conversions);
        Assertions.assertEquals("1", dataset.get(0, 0));
        Assertions.assertEquals(25.0, dataset.get(0, 2));
    }

    @Test
    public void testUpdateColumn() {
        dataset.updateColumn("age", (Function<Integer, Integer>) age -> age + 5);
        Assertions.assertEquals(30, (Integer) dataset.get(0, 2));
        Assertions.assertEquals(35, (Integer) dataset.get(1, 2));
        Assertions.assertEquals(40, (Integer) dataset.get(2, 2));
    }

    @Test
    public void testUpdateColumns() {
        dataset.updateColumns(Arrays.asList("id", "age"), (i, c, v) -> (Integer) v * 10);
        Assertions.assertEquals(10, (Integer) dataset.get(0, 0));
        Assertions.assertEquals(250, (Integer) dataset.get(0, 2));
    }

    @Test
    public void testCombineColumnsWithClass() {
        dataset.combineColumns(Arrays.asList("name", "age"), "nameAge", a -> a.join(""));
        Assertions.assertEquals(3, dataset.columnCount());
        Assertions.assertTrue(dataset.containsColumn("nameAge"));
        Assertions.assertFalse(dataset.containsColumn("name"));
        Assertions.assertFalse(dataset.containsColumn("age"));
    }

    @Test
    public void testCombineColumnsWithFunction() {
        dataset.combineColumns(Arrays.asList("name", "age"), "info", (Function<DisposableObjArray, String>) arr -> arr.get(0) + ":" + arr.get(1));
        Assertions.assertEquals(3, dataset.columnCount());
        Assertions.assertEquals("John:25", dataset.get(0, dataset.getColumnIndex("info")));
    }

    @Test
    public void testCombineColumnsWithTuple2() {
        dataset.combineColumns(new Tuple2<>("name", "age"), "combined", (BiFunction<String, Integer, String>) (name, age) -> name + "_" + age);
        Assertions.assertEquals(3, dataset.columnCount());
        Assertions.assertEquals("John_25", dataset.get(0, dataset.getColumnIndex("combined")));
    }

    @Test
    public void testCombineColumnsWithTuple3() {
        dataset.combineColumns(new Tuple3<>("id", "name", "age"), "combined",
                (TriFunction<Integer, String, Integer, String>) (id, name, age) -> id + ":" + name + ":" + age);
        Assertions.assertEquals(2, dataset.columnCount());
        Assertions.assertEquals("1:John:25", dataset.get(0, dataset.getColumnIndex("combined")));
    }

    @Test
    public void testDivideColumnWithFunction() {
        dataset.addColumn("full_name", Arrays.asList("John Doe", "Jane Smith", "Bob Johnson"));
        dataset.divideColumn("full_name", Arrays.asList("first_name", "last_name"), (Function<String, List<String>>) name -> Arrays.asList(name.split(" ")));

        Assertions.assertTrue(dataset.containsColumn("first_name"));
        Assertions.assertTrue(dataset.containsColumn("last_name"));
        Assertions.assertFalse(dataset.containsColumn("full_name"));
        Assertions.assertEquals("John", dataset.get(0, dataset.getColumnIndex("first_name")));
        Assertions.assertEquals("Doe", dataset.get(0, dataset.getColumnIndex("last_name")));
    }

    @Test
    public void testDivideColumnWithBiConsumer() {
        dataset.addColumn("composite", Arrays.asList("A-1", "B-2", "C-3"));
        dataset.divideColumn("composite", Arrays.asList("letter", "number"), (BiConsumer<String, Object[]>) (val, output) -> {
            String[] parts = val.split("-");
            output[0] = parts[0];
            output[1] = Integer.parseInt(parts[1]);
        });

        Assertions.assertEquals("A", dataset.get(0, dataset.getColumnIndex("letter")));
        Assertions.assertEquals(1, (Integer) dataset.get(0, dataset.getColumnIndex("number")));
    }

    @Test
    public void testDivideColumnWithTuple2() {
        dataset.addColumn("pair", Arrays.asList("X|Y", "A|B", "M|N"));
        dataset.divideColumn("pair", new Tuple2<>("first", "second"), (BiConsumer<String, Pair<Object, Object>>) (val, output) -> {
            String[] parts = val.split("\\|");
            output.setLeft(parts[0]);
            output.setRight(parts[1]);
        });

        Assertions.assertEquals("X", dataset.get(0, dataset.getColumnIndex("first")));
        Assertions.assertEquals("Y", dataset.get(0, dataset.getColumnIndex("second")));
    }

    @Test
    public void testDivideColumnWithTuple3() {
        dataset.addColumn("triple", Arrays.asList("A-B-C", "X-Y-Z", "1-2-3"));
        dataset.divideColumn("triple", new Tuple3<>("p1", "p2", "p3"), (BiConsumer<String, Triple<Object, Object, Object>>) (val, output) -> {
            String[] parts = val.split("-");
            output.setLeft(parts[0]);
            output.setMiddle(parts[1]);
            output.setRight(parts[2]);
        });

        Assertions.assertEquals("A", dataset.get(0, dataset.getColumnIndex("p1")));
        Assertions.assertEquals("B", dataset.get(0, dataset.getColumnIndex("p2")));
        Assertions.assertEquals("C", dataset.get(0, dataset.getColumnIndex("p3")));
    }

    @Test
    public void testAddRowWithArray() {
        dataset.addRow(new Object[] { 4, "Alice", 28, 92.0 });
        Assertions.assertEquals(4, dataset.size());
        Assertions.assertEquals(4, (Integer) dataset.get(3, 0));
        Assertions.assertEquals("Alice", dataset.get(3, 1));
    }

    @Test
    public void testAddRowAtPosition() {
        dataset.addRow(1, new Object[] { 99, "Insert", 50, 75.0 });
        Assertions.assertEquals(4, dataset.size());
        Assertions.assertEquals(99, (Integer) dataset.get(1, 0));
        Assertions.assertEquals("Insert", dataset.get(1, 1));
        Assertions.assertEquals("Jane", dataset.get(2, 1));
    }

    @Test
    public void testAddRowWithList() {
        dataset.addRow(Arrays.asList(4, "Alice", 28, 92.0));
        Assertions.assertEquals(4, dataset.size());
        Assertions.assertEquals("Alice", dataset.get(3, 1));
    }

    @Test
    public void testAddRowWithMap() {
        Map<String, Object> row = new HashMap<>();
        row.put("id", 4);
        row.put("name", "Alice");
        row.put("age", 28);
        row.put("score", 92.0);

        dataset.addRow(row);
        Assertions.assertEquals(4, dataset.size());
        Assertions.assertEquals("Alice", dataset.get(3, 1));
    }

    @Test
    public void testAddRowWithBean() {
        TestBean bean = new TestBean();
        bean.id = 4;
        bean.name = "Alice";
        bean.age = 28;
        bean.score = 92.0;

        dataset.addRow(bean);
        Assertions.assertEquals(4, dataset.size());
        Assertions.assertEquals("Alice", dataset.get(3, 1));
    }

    @Test
    public void testRemoveRow() {
        dataset.removeRow(1);
        Assertions.assertEquals(2, dataset.size());
        Assertions.assertEquals("John", dataset.get(0, 1));
        Assertions.assertEquals("Bob", dataset.get(1, 1));
    }

    @Test
    public void testRemoveRows() {
        dataset.removeRowsAt(0, 2);
        Assertions.assertEquals(1, dataset.size());
        Assertions.assertEquals("Jane", dataset.get(0, 1));
    }

    @Test
    public void testRemoveRowRange() {
        dataset.removeRows(0, 2);
        Assertions.assertEquals(1, dataset.size());
        Assertions.assertEquals("Bob", dataset.get(0, 1));
    }

    @Test
    public void testUpdateRow() {
        dataset.updateRow(0, (Function<Object, Object>) val -> val instanceof Integer ? (Integer) val * 2 : val);
        Assertions.assertEquals(2, (Integer) dataset.get(0, 0));
        Assertions.assertEquals(50, (Integer) dataset.get(0, 2));
    }

    @Test
    public void testUpdateRows() {
        dataset.updateRows(new int[] { 0, 2 }, (i, c, v) -> v instanceof Integer ? (Integer) v * 2 : v);
        Assertions.assertEquals(2, (Integer) dataset.get(0, 0));
        Assertions.assertEquals(50, (Integer) dataset.get(0, 2));
        Assertions.assertEquals(2, (Integer) dataset.get(1, 0));
        Assertions.assertEquals(6, (Integer) dataset.get(2, 0));
        Assertions.assertEquals(70, (Integer) dataset.get(2, 2));
    }

    @Test
    public void testUpdateAll() {
        dataset.updateAll((Function<Object, Object>) val -> val instanceof Integer ? (Integer) val * 10 : val);
        Assertions.assertEquals(10, (Integer) dataset.get(0, 0));
        Assertions.assertEquals(20, (Integer) dataset.get(1, 0));
        Assertions.assertEquals(30, (Integer) dataset.get(2, 0));
        Assertions.assertEquals(250, (Integer) dataset.get(0, 2));
    }

    @Test
    public void testReplaceIf() {
        dataset.replaceIf((Predicate<Object>) val -> val instanceof Integer && (Integer) val > 25, 999);
        Assertions.assertEquals(1, (Integer) dataset.get(0, 0));
        Assertions.assertEquals(999, (Integer) dataset.get(1, 2));
        Assertions.assertEquals(999, (Integer) dataset.get(2, 2));
    }

    @Test
    public void testPrepend() {
        List<List<Object>> newColumns = new ArrayList<>();
        newColumns.add(Arrays.asList(10, 11));
        newColumns.add(Arrays.asList("PrependA", "PrependB"));
        newColumns.add(Arrays.asList(100, 101));
        newColumns.add(Arrays.asList(50.0, 60.0));

        RowDataset otherDataset = new RowDataset(columnNames, newColumns);
        dataset.prepend(otherDataset);

        Assertions.assertEquals(5, dataset.size());
        Assertions.assertEquals(10, (Integer) dataset.get(0, 0));
        Assertions.assertEquals("PrependA", dataset.get(0, 1));
        Assertions.assertEquals(1, (Integer) dataset.get(2, 0));
    }

    @Test
    public void testAppend() {
        List<List<Object>> newColumns = new ArrayList<>();
        newColumns.add(Arrays.asList(10, 11));
        newColumns.add(Arrays.asList("AppendA", "AppendB"));
        newColumns.add(Arrays.asList(100, 101));
        newColumns.add(Arrays.asList(50.0, 60.0));

        RowDataset otherDataset = new RowDataset(columnNames, newColumns);
        dataset.append(otherDataset);

        Assertions.assertEquals(5, dataset.size());
        Assertions.assertEquals(1, (Integer) dataset.get(0, 0));
        Assertions.assertEquals(10, (Integer) dataset.get(3, 0));
        Assertions.assertEquals("AppendA", dataset.get(3, 1));
    }

    @Test
    public void testCurrentRowNum() {
        Assertions.assertEquals(0, dataset.currentRowIndex());
        dataset.moveToRow(2);
        Assertions.assertEquals(2, dataset.currentRowIndex());
    }

    @Test
    public void testAbsolute() {
        Dataset result = dataset.moveToRow(1);
        Assertions.assertSame(dataset, result);
        Assertions.assertEquals(1, dataset.currentRowIndex());
        Assertions.assertEquals("Jane", dataset.get("name"));
    }

    @Test
    public void testGetRowAsArray() {
        Object[] row = dataset.getRow(0);
        Assertions.assertEquals(4, row.length);
        Assertions.assertEquals(1, row[0]);
        Assertions.assertEquals("John", row[1]);
        Assertions.assertEquals(25, row[2]);
        Assertions.assertEquals(85.5, row[3]);
    }

    @Test
    public void testGetRowAsClass() {
        TestBean bean = dataset.getRow(0, TestBean.class);
        Assertions.assertEquals(1, bean.id);
        Assertions.assertEquals("John", bean.name);
        Assertions.assertEquals(25, bean.age);
        Assertions.assertEquals(85.5, bean.score, 0.01);
    }

    @Test
    public void testGetRowWithSelectedColumns() {
        TestBean bean = dataset.getRow(0, Arrays.asList("name", "age"), TestBean.class);
        Assertions.assertEquals("John", bean.name);
        Assertions.assertEquals(25, bean.age);
    }

    @Test
    public void testGetRowWithSupplier() {
        List<Object> row = dataset.getRow(0, (IntFunction<List<Object>>) size -> new ArrayList<>(size));
        Assertions.assertEquals(4, row.size());
        Assertions.assertEquals(1, row.get(0));
        Assertions.assertEquals("John", row.get(1));
    }

    @Test
    public void testFirstRow() {
        Optional<Object[]> firstRow = dataset.firstRow();
        Assertions.assertTrue(firstRow.isPresent());
        Assertions.assertEquals(1, firstRow.get()[0]);
        Assertions.assertEquals("John", firstRow.get()[1]);
    }

    @Test
    public void testFirstRowAsClass() {
        Optional<TestBean> firstRow = dataset.firstRow(TestBean.class);
        Assertions.assertTrue(firstRow.isPresent());
        Assertions.assertEquals("John", firstRow.get().name);
    }

    @Test
    public void testFirstRowWithColumns() {
        Optional<TestBean> firstRow = dataset.firstRow(Arrays.asList("name", "age"), TestBean.class);
        Assertions.assertTrue(firstRow.isPresent());
        Assertions.assertEquals("John", firstRow.get().name);
        Assertions.assertEquals(25, firstRow.get().age);
    }

    @Test
    public void testFirstRowWithSupplier() {
        Optional<List<Object>> firstRow = dataset.firstRow((IntFunction<List<Object>>) ArrayList::new);
        Assertions.assertTrue(firstRow.isPresent());
        Assertions.assertEquals(4, firstRow.get().size());
    }

    @Test
    public void testLastRow() {
        Optional<Object[]> lastRow = dataset.lastRow();
        Assertions.assertTrue(lastRow.isPresent());
        Assertions.assertEquals(3, lastRow.get()[0]);
        Assertions.assertEquals("Bob", lastRow.get()[1]);
    }

    @Test
    public void testLastRowAsClass() {
        Optional<TestBean> lastRow = dataset.lastRow(TestBean.class);
        Assertions.assertTrue(lastRow.isPresent());
        Assertions.assertEquals("Bob", lastRow.get().name);
    }

    @Test
    public void testLastRowWithColumns() {
        Optional<TestBean> lastRow = dataset.lastRow(Arrays.asList("name", "age"), TestBean.class);
        Assertions.assertTrue(lastRow.isPresent());
        Assertions.assertEquals("Bob", lastRow.get().name);
        Assertions.assertEquals(35, lastRow.get().age);
    }

    @Test
    public void testLastRowWithSupplier() {
        Optional<List<Object>> lastRow = dataset.lastRow((IntFunction<List<Object>>) ArrayList::new);
        Assertions.assertTrue(lastRow.isPresent());
        Assertions.assertEquals(4, lastRow.get().size());
    }

    @Test
    public void testBiIterator() {
        BiIterator<String, Integer> iter = dataset.iterator("name", "age");
        Assertions.assertTrue(iter.hasNext());

        Pair<String, Integer> pair = iter.next();
        Assertions.assertEquals("John", pair.left());
        Assertions.assertEquals(25, pair.right());
    }

    @Test
    public void testBiIteratorWithRange() {
        BiIterator<String, Integer> iter = dataset.iterator(1, 3, "name", "age");
        Assertions.assertTrue(iter.hasNext());

        Pair<String, Integer> pair = iter.next();
        Assertions.assertEquals("Jane", pair.left());
        Assertions.assertEquals(30, pair.right());
    }

    @Test
    public void testTriIterator() {
        TriIterator<Integer, String, Integer> iter = dataset.iterator("id", "name", "age");
        Assertions.assertTrue(iter.hasNext());

        Triple<Integer, String, Integer> triple = iter.next();
        Assertions.assertEquals(1, triple.left());
        Assertions.assertEquals("John", triple.middle());
        Assertions.assertEquals(25, triple.right());
    }

    @Test
    public void testTriIteratorWithRange() {
        TriIterator<Integer, String, Integer> iter = dataset.iterator(0, 2, "id", "name", "age");

        int count = 0;
        while (iter.hasNext()) {
            iter.next();
            count++;
        }
        Assertions.assertEquals(2, count);
    }

    @Test
    public void testForEach() {
        List<String> results = new ArrayList<>();
        dataset.forEach((DisposableObjArray arr) -> {
            results.add(arr.get(1) + ":" + arr.get(2));
        });

        Assertions.assertEquals(3, results.size());
        Assertions.assertEquals("John:25", results.get(0));
        Assertions.assertEquals("Jane:30", results.get(1));
        Assertions.assertEquals("Bob:35", results.get(2));
    }

    @Test
    public void testForEachWithColumns() {
        List<String> results = new ArrayList<>();
        dataset.forEach(Arrays.asList("name", "age"), (DisposableObjArray arr) -> {
            results.add(arr.get(0) + "-" + arr.get(1));
        });

        Assertions.assertEquals(3, results.size());
        Assertions.assertEquals("John-25", results.get(0));
    }

    @Test
    public void testForEachWithRange() {
        List<String> results = new ArrayList<>();
        dataset.forEach(1, 3, (DisposableObjArray arr) -> {
            results.add(arr.get(1).toString());
        });

        Assertions.assertEquals(2, results.size());
        Assertions.assertEquals("Jane", results.get(0));
        Assertions.assertEquals("Bob", results.get(1));
    }

    @Test
    public void testForEachWithTuple2() {
        List<String> results = new ArrayList<>();
        dataset.forEach(Tuple.of("name", "age"), (name, age) -> results.add(name + " is " + age));

        Assertions.assertEquals(3, results.size());
        Assertions.assertEquals("John is 25", results.get(0));
    }

    @Test
    public void testForEachWithTuple3() {
        List<String> results = new ArrayList<>();
        dataset.forEach(new Tuple3<>("id", "name", "age"), (TriConsumer<Integer, String, Integer>) (id, name, age) -> results.add(id + ":" + name + ":" + age));

        Assertions.assertEquals(3, results.size());
        Assertions.assertEquals("1:John:25", results.get(0));
    }

    @Test
    public void testToList() {
        List<Object[]> list = dataset.toList();
        Assertions.assertEquals(3, list.size());
        Assertions.assertEquals(1, list.get(0)[0]);
        Assertions.assertEquals("John", list.get(0)[1]);
    }

    @Test
    public void testToListWithRange() {
        List<Object[]> list = dataset.toList(1, 3);
        Assertions.assertEquals(2, list.size());
        Assertions.assertEquals(2, list.get(0)[0]);
        Assertions.assertEquals("Jane", list.get(0)[1]);
    }

    @Test
    public void testToListAsClass() {
        List<TestBean> list = dataset.toList(TestBean.class);
        Assertions.assertEquals(3, list.size());
        Assertions.assertEquals("John", list.get(0).name);
        Assertions.assertEquals(25, list.get(0).age);
    }

    @Test
    public void testToListWithRangeAsClass() {
        List<TestBean> list = dataset.toList(0, 2, TestBean.class);
        Assertions.assertEquals(2, list.size());
        Assertions.assertEquals("John", list.get(0).name);
        Assertions.assertEquals("Jane", list.get(1).name);
    }

    @Test
    public void testToListWithColumnsAsClass() {
        List<TestBean> list = dataset.toList(Arrays.asList("name", "age"), TestBean.class);
        Assertions.assertEquals(3, list.size());
        Assertions.assertEquals("John", list.get(0).name);
        Assertions.assertEquals(25, list.get(0).age);
    }

    @Test
    public void testToListWithSupplier() {
        List<Map<String, Object>> list = dataset.toList((IntFunction<Map<String, Object>>) size -> new HashMap<>());
        Assertions.assertEquals(3, list.size());
        Assertions.assertEquals(1, list.get(0).get("id"));
        Assertions.assertEquals("John", list.get(0).get("name"));
    }

    @Test
    public void testToListWithFilters() {
        List<TestBean> list = dataset.toList(col -> col.equals("name") || col.equals("age"), col -> col.toUpperCase(), TestBean.class);
        Assertions.assertEquals(3, list.size());
    }

    @Test
    public void testToEntities() {
        List<TestBean> entities = dataset.toEntities(null, TestBean.class);
        Assertions.assertEquals(3, entities.size());
        Assertions.assertEquals("John", entities.get(0).name);
    }

    @Test
    public void testToEntitiesWithPrefixMap() {
        Map<String, String> prefixMap = new HashMap<>();
        prefixMap.put("n", "name");
        List<TestBean> entities = dataset.toEntities(prefixMap, TestBean.class);
        Assertions.assertEquals(3, entities.size());
    }

    @Test
    public void testToMergedEntities() {
        List<List<Object>> mergeColumns = new ArrayList<>();
        mergeColumns.add(Arrays.asList(1, 1, 2));
        mergeColumns.add(Arrays.asList("John", "John", "Jane"));
        mergeColumns.add(Arrays.asList(25, 25, 30));
        mergeColumns.add(Arrays.asList(85.5, 90.0, 88.5));

        RowDataset mergeDataset = new RowDataset(columnNames, mergeColumns);
        List<TestBean> merged = mergeDataset.toMergedEntities("id", TestBean.class);

        Assertions.assertEquals(2, merged.size());
        Assertions.assertEquals(1, merged.get(0).id);
        Assertions.assertEquals("John", merged.get(0).name);
    }

    @Test
    public void testToMapWithKeyValueColumns() {
        Map<Integer, String> map = dataset.toMap("id", "name");
        Assertions.assertEquals(3, map.size());
        Assertions.assertEquals("John", map.get(1));
        Assertions.assertEquals("Jane", map.get(2));
        Assertions.assertEquals("Bob", map.get(3));
    }

    @Test
    public void testToMapWithSupplier() {
        Map<Integer, String> map = dataset.toMap("id", "name", (IntFunction<LinkedHashMap<Integer, String>>) size -> new LinkedHashMap<>());
        Assertions.assertEquals(3, map.size());
        Assertions.assertEquals("John", map.get(1));
    }

    @Test
    public void testToMapWithRange() {
        Map<Integer, String> map = dataset.toMap(0, 2, "id", "name");
        Assertions.assertEquals(2, map.size());
        Assertions.assertEquals("John", map.get(1));
        Assertions.assertEquals("Jane", map.get(2));
        Assertions.assertNull(map.get(3));
    }

    @Test
    public void testToMapWithMultipleValueColumns() {
        Map<Integer, TestBean> map = dataset.toMap("id", Arrays.asList("name", "age"), TestBean.class);
        Assertions.assertEquals(3, map.size());
        Assertions.assertEquals("John", map.get(1).name);
        Assertions.assertEquals(25, map.get(1).age);
    }

    @Test
    public void testToMapWithValueSupplier() {
        Map<Integer, List<Object>> map = dataset.toMap("id", Arrays.asList("name", "age"), (IntFunction<List<Object>>) ArrayList::new);
        Assertions.assertEquals(3, map.size());
        Assertions.assertEquals("John", map.get(1).get(0));
        Assertions.assertEquals(25, map.get(1).get(1));
    }

    @Test
    public void testToMultimap() {
        List<List<Object>> dupColumns = new ArrayList<>();
        dupColumns.add(Arrays.asList(1, 1, 2));
        dupColumns.add(Arrays.asList("A", "B", "C"));

        RowDataset dupDataset = new RowDataset(Arrays.asList("id", "value"), dupColumns);
        ListMultimap<Integer, String> multimap = dupDataset.toMultimap("id", "value");

        Assertions.assertEquals(2, multimap.get(1).size());
        Assertions.assertTrue(multimap.get(1).contains("A"));
        Assertions.assertTrue(multimap.get(1).contains("B"));
    }

    @Test
    public void testToMultimapWithClass() {
        ListMultimap<Integer, TestBean> multimap = dataset.toMultimap("id", Arrays.asList("name", "age"), TestBean.class);
        Assertions.assertEquals(1, multimap.get(1).size());
        Assertions.assertEquals("John", multimap.get(1).get(0).name);
    }

    @Test
    public void testToJson() {
        String json = dataset.toJson();
        Assertions.assertTrue(json.contains("\"id\":1"));
        Assertions.assertTrue(json.contains("\"name\":\"John\""));
        Assertions.assertTrue(json.contains("\"age\":25"));
    }

    @Test
    public void testToJsonWithRange() {
        String json = dataset.toJson(0, 1);
        Assertions.assertTrue(json.contains("\"name\":\"John\""));
        Assertions.assertFalse(json.contains("\"name\":\"Jane\""));
    }

    @Test
    public void testToJsonWithColumns() {
        String json = dataset.toJson(0, 3, Arrays.asList("name", "age"));
        Assertions.assertTrue(json.contains("\"name\":\"John\""));
        Assertions.assertTrue(json.contains("\"age\":25"));
        Assertions.assertFalse(json.contains("\"id\""));
        Assertions.assertFalse(json.contains("\"score\""));
    }

    @Test
    public void testToJsonToFile() throws IOException {
        File tempFile = File.createTempFile("dataset", ".json");
        tempFile.deleteOnExit();

        dataset.toJson(tempFile);
        String content = new String(java.nio.file.Files.readAllBytes(tempFile.toPath()));
        Assertions.assertTrue(content.contains("\"name\":\"John\""));
    }

    @Test
    public void testToJsonToOutputStream() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        dataset.toJson(baos);
        String json = baos.toString();
        Assertions.assertTrue(json.contains("\"name\":\"John\""));
    }

    @Test
    public void testToJsonToWriter() throws IOException {
        StringWriter writer = new StringWriter();
        dataset.toJson(writer);
        String json = writer.toString();
        Assertions.assertTrue(json.contains("\"name\":\"John\""));
    }

    public static class TestBean {
        public int id;
        public String name;
        public int age;
        public double score;

        public TestBean() {
        }
    }
}

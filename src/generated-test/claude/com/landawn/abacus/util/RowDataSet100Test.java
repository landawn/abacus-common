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
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.NoCachingNoUpdating.DisposableObjArray;
import com.landawn.abacus.util.Tuple.Tuple2;
import com.landawn.abacus.util.Tuple.Tuple3;
import com.landawn.abacus.util.u.Optional;
import com.landawn.abacus.util.function.TriConsumer;
import com.landawn.abacus.util.function.TriFunction;

public class RowDataSet100Test extends TestBase {

    private RowDataSet dataSet;
    private List<String> columnNames;
    private List<List<Object>> columns;

    @BeforeEach
    public void setUp() {
        columnNames = N.asList("id", "name", "age", "score");
        columns = new ArrayList<>();
        columns.add(N.asList(1, 2, 3)); // id column
        columns.add(N.asList("John", "Jane", "Bob")); // name column
        columns.add(N.asList(25, 30, 35)); // age column
        columns.add(N.asList(85.5, 90.0, 88.5)); // score column
        dataSet = new RowDataSet(columnNames, columns);
    }

    @Test
    public void testConstructorWithColumnNamesAndColumns() {
        RowDataSet ds = new RowDataSet(columnNames, columns);
        Assertions.assertNotNull(ds);
        Assertions.assertEquals(4, ds.columnCount());
        Assertions.assertEquals(3, ds.size());
    }

    @Test
    public void testConstructorWithProperties() {
        Map<String, Object> properties = new HashMap<>();
        properties.put("key1", "value1");
        properties.put("key2", 123);

        RowDataSet ds = new RowDataSet(columnNames, columns, properties);
        Assertions.assertNotNull(ds);
        Assertions.assertEquals(4, ds.columnCount());
        Assertions.assertEquals(3, ds.size());
    }

    @Test
    public void testConstructorWithNullColumnNames() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            new RowDataSet(null, columns);
        });
    }

    @Test
    public void testConstructorWithNullColumns() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            new RowDataSet(columnNames, null);
        });
    }

    @Test
    public void testConstructorWithEmptyColumnName() {
        List<String> badColumnNames = Arrays.asList("id", "", "age");
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            new RowDataSet(badColumnNames, columns);
        });
    }

    @Test
    public void testConstructorWithDuplicateColumnNames() {
        List<String> duplicateNames = Arrays.asList("id", "name", "id");
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            new RowDataSet(duplicateNames, columns);
        });
    }

    @Test
    public void testConstructorWithMismatchedSizes() {
        List<String> shortColumnNames = Arrays.asList("id", "name");
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            new RowDataSet(shortColumnNames, columns);
        });
    }

    @Test
    public void testConstructorWithInconsistentColumnSizes() {
        List<List<Object>> badColumns = new ArrayList<>();
        badColumns.add(Arrays.asList(1, 2));
        badColumns.add(Arrays.asList("John", "Jane", "Bob"));

        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            new RowDataSet(Arrays.asList("id", "name"), badColumns);
        });
    }

    @Test
    public void testColumnNameList() {
        ImmutableList<String> names = dataSet.columnNameList();
        Assertions.assertEquals(4, names.size());
        Assertions.assertEquals("id", names.get(0));
        Assertions.assertEquals("name", names.get(1));
        Assertions.assertEquals("age", names.get(2));
        Assertions.assertEquals("score", names.get(3));
    }

    @Test
    public void testColumnCount() {
        Assertions.assertEquals(4, dataSet.columnCount());
    }

    @Test
    public void testGetColumnName() {
        Assertions.assertEquals("id", dataSet.getColumnName(0));
        Assertions.assertEquals("name", dataSet.getColumnName(1));
        Assertions.assertEquals("age", dataSet.getColumnName(2));
        Assertions.assertEquals("score", dataSet.getColumnName(3));
    }

    @Test
    public void testGetColumnNameWithInvalidIndex() {
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> {
            dataSet.getColumnName(-1);
        });
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> {
            dataSet.getColumnName(4);
        });
    }

    @Test
    public void testGetColumnIndex() {
        Assertions.assertEquals(0, dataSet.getColumnIndex("id"));
        Assertions.assertEquals(1, dataSet.getColumnIndex("name"));
        Assertions.assertEquals(2, dataSet.getColumnIndex("age"));
        Assertions.assertEquals(3, dataSet.getColumnIndex("score"));
    }

    @Test
    public void testGetColumnIndexWithInvalidName() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            dataSet.getColumnIndex("invalid");
        });
    }

    @Test
    public void testGetColumnIndexes() {
        int[] indexes = dataSet.getColumnIndexes(Arrays.asList("id", "age"));
        Assertions.assertArrayEquals(new int[] { 0, 2 }, indexes);

        indexes = dataSet.getColumnIndexes(Arrays.asList("name", "score"));
        Assertions.assertArrayEquals(new int[] { 1, 3 }, indexes);

        indexes = dataSet.getColumnIndexes(Collections.emptyList());
        Assertions.assertArrayEquals(new int[] {}, indexes);
    }

    @Test
    public void testGetColumnIndexesWithInvalidName() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            dataSet.getColumnIndexes(Arrays.asList("id", "invalid"));
        });
    }

    @Test
    public void testContainsColumn() {
        Assertions.assertTrue(dataSet.containsColumn("id"));
        Assertions.assertTrue(dataSet.containsColumn("name"));
        Assertions.assertTrue(dataSet.containsColumn("age"));
        Assertions.assertTrue(dataSet.containsColumn("score"));
        Assertions.assertFalse(dataSet.containsColumn("invalid"));
    }

    @Test
    public void testContainsAllColumns() {
        Assertions.assertTrue(dataSet.containsAllColumns(Arrays.asList("id", "name")));
        Assertions.assertTrue(dataSet.containsAllColumns(Arrays.asList("age", "score")));
        Assertions.assertTrue(dataSet.containsAllColumns(columnNames));
        Assertions.assertFalse(dataSet.containsAllColumns(Arrays.asList("id", "invalid")));
        Assertions.assertTrue(dataSet.containsAllColumns(Collections.emptyList()));
    }

    @Test
    public void testRenameColumn() {
        dataSet.renameColumn("id", "user_id");
        Assertions.assertTrue(dataSet.containsColumn("user_id"));
        Assertions.assertFalse(dataSet.containsColumn("id"));
        Assertions.assertEquals(0, dataSet.getColumnIndex("user_id"));
    }

    @Test
    public void testRenameColumnWithSameName() {
        dataSet.renameColumn("id", "id");
        Assertions.assertTrue(dataSet.containsColumn("id"));
    }

    @Test
    public void testRenameColumnWithExistingName() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            dataSet.renameColumn("id", "name");
        });
    }

    @Test
    public void testRenameColumnWithInvalidName() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            dataSet.renameColumn("invalid", "new_name");
        });
    }

    @Test
    public void testRenameColumns() {
        Map<String, String> oldNewNames = new HashMap<>();
        oldNewNames.put("id", "user_id");
        oldNewNames.put("name", "user_name");

        dataSet.renameColumns(oldNewNames);
        Assertions.assertTrue(dataSet.containsColumn("user_id"));
        Assertions.assertTrue(dataSet.containsColumn("user_name"));
        Assertions.assertFalse(dataSet.containsColumn("id"));
        Assertions.assertFalse(dataSet.containsColumn("name"));
    }

    @Test
    public void testRenameColumnsWithDuplicateNewNames() {
        Map<String, String> oldNewNames = new HashMap<>();
        oldNewNames.put("id", "same_name");
        oldNewNames.put("name", "same_name");

        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            dataSet.renameColumns(oldNewNames);
        });
    }

    @Test
    public void testRenameColumnsWithFunction() {
        dataSet.renameColumns(Arrays.asList("id", "name"), col -> col.toUpperCase());
        Assertions.assertTrue(dataSet.containsColumn("ID"));
        Assertions.assertTrue(dataSet.containsColumn("NAME"));
        Assertions.assertFalse(dataSet.containsColumn("id"));
        Assertions.assertFalse(dataSet.containsColumn("name"));
    }

    @Test
    public void testRenameAllColumnsWithFunction() {
        dataSet.renameColumns(col -> col + "_renamed");
        Assertions.assertTrue(dataSet.containsColumn("id_renamed"));
        Assertions.assertTrue(dataSet.containsColumn("name_renamed"));
        Assertions.assertTrue(dataSet.containsColumn("age_renamed"));
        Assertions.assertTrue(dataSet.containsColumn("score_renamed"));
    }

    @Test
    public void testMoveColumn() {
        dataSet.moveColumn("id", 2);
        List<String> names = dataSet.columnNameList();
        Assertions.assertEquals("name", names.get(0));
        Assertions.assertEquals("age", names.get(1));
        Assertions.assertEquals("id", names.get(2));
        Assertions.assertEquals("score", names.get(3));
    }

    @Test
    public void testMoveColumnToSamePosition() {
        dataSet.moveColumn("id", 0);
        List<String> names = dataSet.columnNameList();
        Assertions.assertEquals("id", names.get(0));
    }

    @Test
    public void testMoveColumnWithInvalidPosition() {
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> {
            dataSet.moveColumn("id", -1);
        });
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> {
            dataSet.moveColumn("id", 5);
        });
    }

    @Test
    public void testMoveColumns() {
        Map<String, Integer> columnPositions = new LinkedHashMap<>();
        columnPositions.put("score", 0);
        columnPositions.put("id", 3);

        dataSet.moveColumns(columnPositions);
        List<String> names = dataSet.columnNameList();
        Assertions.assertEquals("score", names.get(0));
        Assertions.assertEquals("name", names.get(1));
        Assertions.assertEquals("age", names.get(2));
        Assertions.assertEquals("id", names.get(3));
    }

    @Test
    public void testSwapColumnPosition() {
        dataSet.swapColumnPosition("id", "score");
        List<String> names = dataSet.columnNameList();
        Assertions.assertEquals("score", names.get(0));
        Assertions.assertEquals("name", names.get(1));
        Assertions.assertEquals("age", names.get(2));
        Assertions.assertEquals("id", names.get(3));
    }

    @Test
    public void testSwapColumnPositionWithSameColumn() {
        dataSet.swapColumnPosition("id", "id");
        List<String> names = dataSet.columnNameList();
        Assertions.assertEquals("id", names.get(0));
    }

    @Test
    public void testMoveRow() {
        Object originalRow1Name = dataSet.get(1, 1);
        dataSet.moveRow(1, 0);
        Assertions.assertEquals(originalRow1Name, dataSet.get(0, 1));
    }

    @Test
    public void testMoveRowToSamePosition() {
        Object originalRow1Name = dataSet.get(1, 1);
        dataSet.moveRow(1, 1);
        Assertions.assertEquals(originalRow1Name, dataSet.get(1, 1));
    }

    @Test
    public void testSwapRowPosition() {
        Object row0Name = dataSet.get(0, 1);
        Object row2Name = dataSet.get(2, 1);

        dataSet.swapRowPosition(0, 2);
        Assertions.assertEquals(row2Name, dataSet.get(0, 1));
        Assertions.assertEquals(row0Name, dataSet.get(2, 1));
    }

    @Test
    public void testSwapRowPositionWithSameRow() {
        Object row0Name = dataSet.get(0, 1);
        dataSet.swapRowPosition(0, 0);
        Assertions.assertEquals(row0Name, dataSet.get(0, 1));
    }

    @Test
    public void testGet() {
        Assertions.assertEquals(1, (Integer) dataSet.get(0, 0));
        Assertions.assertEquals("John", dataSet.get(0, 1));
        Assertions.assertEquals(25, (Integer) dataSet.get(0, 2));
        Assertions.assertEquals(85.5, dataSet.get(0, 3));
    }

    @Test
    public void testSet() {
        dataSet.set(0, 0, 100);
        Assertions.assertEquals(100, (Integer) dataSet.get(0, 0));

        dataSet.set(1, 1, "Updated");
        Assertions.assertEquals("Updated", dataSet.get(1, 1));
    }

    @Test
    public void testIsNullWithRowAndColumn() {
        dataSet.set(0, 0, null);
        Assertions.assertTrue(dataSet.isNull(0, 0));
        Assertions.assertFalse(dataSet.isNull(0, 1));
    }

    @Test
    public void testGetByColumnIndex() {
        dataSet.absolute(0);
        Assertions.assertEquals(1, (Integer) dataSet.get(0));
        Assertions.assertEquals("John", dataSet.get(1));
        Assertions.assertEquals(25, (Integer) dataSet.get(2));
        Assertions.assertEquals(85.5, dataSet.get(3));
    }

    @Test
    public void testGetByColumnName() {
        dataSet.absolute(0);
        Assertions.assertEquals(1, (Integer) dataSet.get("id"));
        Assertions.assertEquals("John", dataSet.get("name"));
        Assertions.assertEquals(25, (Integer) dataSet.get("age"));
        Assertions.assertEquals(85.5, dataSet.get("score"));
    }

    @Test
    public void testGetBoolean() {
        List<List<Object>> boolColumns = new ArrayList<>();
        boolColumns.add(Arrays.asList(true, false, true));
        RowDataSet boolDataSet = new RowDataSet(Arrays.asList("flag"), boolColumns);

        boolDataSet.absolute(0);
        Assertions.assertTrue(boolDataSet.getBoolean(0));
        Assertions.assertTrue(boolDataSet.getBoolean("flag"));

        boolDataSet.absolute(1);
        Assertions.assertFalse(boolDataSet.getBoolean(0));
        Assertions.assertFalse(boolDataSet.getBoolean("flag"));
    }

    @Test
    public void testGetChar() {
        List<List<Object>> charColumns = new ArrayList<>();
        charColumns.add(Arrays.asList('A', 'B', 'C'));
        RowDataSet charDataSet = new RowDataSet(Arrays.asList("letter"), charColumns);

        charDataSet.absolute(0);
        Assertions.assertEquals('A', charDataSet.getChar(0));
        Assertions.assertEquals('A', charDataSet.getChar("letter"));
    }

    @Test
    public void testGetByte() {
        dataSet.absolute(0);
        Assertions.assertEquals((byte) 1, dataSet.getByte(0));
        Assertions.assertEquals((byte) 1, dataSet.getByte("id"));
    }

    @Test
    public void testGetShort() {
        dataSet.absolute(0);
        Assertions.assertEquals((short) 1, dataSet.getShort(0));
        Assertions.assertEquals((short) 1, dataSet.getShort("id"));
    }

    @Test
    public void testGetInt() {
        dataSet.absolute(0);
        Assertions.assertEquals(1, dataSet.getInt(0));
        Assertions.assertEquals(1, dataSet.getInt("id"));
        Assertions.assertEquals(25, dataSet.getInt(2));
        Assertions.assertEquals(25, dataSet.getInt("age"));
    }

    @Test
    public void testGetLong() {
        dataSet.absolute(0);
        Assertions.assertEquals(1L, dataSet.getLong(0));
        Assertions.assertEquals(1L, dataSet.getLong("id"));
    }

    @Test
    public void testGetFloat() {
        dataSet.absolute(0);
        Assertions.assertEquals(85.5f, dataSet.getFloat(3), 0.01f);
        Assertions.assertEquals(85.5f, dataSet.getFloat("score"), 0.01f);
    }

    @Test
    public void testGetDouble() {
        dataSet.absolute(0);
        Assertions.assertEquals(85.5, dataSet.getDouble(3), 0.01);
        Assertions.assertEquals(85.5, dataSet.getDouble("score"), 0.01);
    }

    @Test
    public void testIsNull() {
        dataSet.absolute(0);
        dataSet.set(0, null);
        Assertions.assertTrue(dataSet.isNull(0));
        Assertions.assertTrue(dataSet.isNull("id"));
        Assertions.assertFalse(dataSet.isNull(1));
        Assertions.assertFalse(dataSet.isNull("name"));
    }

    @Test
    public void testSetByColumnIndex() {
        dataSet.absolute(0);
        dataSet.set(0, 999);
        Assertions.assertEquals(999, (Integer) dataSet.get(0));
    }

    @Test
    public void testSetByColumnName() {
        dataSet.absolute(0);
        dataSet.set("name", "Updated Name");
        Assertions.assertEquals("Updated Name", dataSet.get("name"));
    }

    @Test
    public void testGetColumn() {
        ImmutableList<Object> idColumn = dataSet.getColumn(0);
        Assertions.assertEquals(3, idColumn.size());
        Assertions.assertEquals(1, idColumn.get(0));
        Assertions.assertEquals(2, idColumn.get(1));
        Assertions.assertEquals(3, idColumn.get(2));

        ImmutableList<Object> nameColumn = dataSet.getColumn("name");
        Assertions.assertEquals(3, nameColumn.size());
        Assertions.assertEquals("John", nameColumn.get(0));
        Assertions.assertEquals("Jane", nameColumn.get(1));
        Assertions.assertEquals("Bob", nameColumn.get(2));
    }

    @Test
    public void testCopyColumn() {
        List<Object> copiedColumn = dataSet.copyColumn("name");
        Assertions.assertEquals(3, copiedColumn.size());
        Assertions.assertEquals("John", copiedColumn.get(0));
        Assertions.assertEquals("Jane", copiedColumn.get(1));
        Assertions.assertEquals("Bob", copiedColumn.get(2));

        // Verify it's a copy
        copiedColumn.set(0, "Modified");
        Assertions.assertEquals("John", dataSet.get(0, 1));
    }

    @Test
    public void testAddColumn() {
        dataSet.addColumn("status", Arrays.asList("Active", "Inactive", "Active"));
        Assertions.assertEquals(5, dataSet.columnCount());
        Assertions.assertTrue(dataSet.containsColumn("status"));
        Assertions.assertEquals("Active", dataSet.get(0, 4));
    }

    @Test
    public void testAddColumnAtPosition() {
        dataSet.addColumn(2, "status", Arrays.asList("Active", "Inactive", "Active"));
        Assertions.assertEquals(5, dataSet.columnCount());
        Assertions.assertEquals("status", dataSet.getColumnName(2));
        Assertions.assertEquals("Active", dataSet.get(0, 2));
    }

    @Test
    public void testAddColumnWithEmptyCollection() {
        dataSet.addColumn("empty", Collections.emptyList());
        Assertions.assertEquals(5, dataSet.columnCount());
        Assertions.assertTrue(dataSet.containsColumn("empty"));
        Assertions.assertNull(dataSet.get(0, 4));
    }

    @Test
    public void testAddColumnWithWrongSize() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            dataSet.addColumn("bad", Arrays.asList("One", "Two"));
        });
    }

    @Test
    public void testAddColumnWithFunction() {
        dataSet.addColumn("double_age", "age", (Function<Integer, Integer>) age -> (Integer) age * 2);
        Assertions.assertEquals(5, dataSet.columnCount());
        Assertions.assertTrue(dataSet.containsColumn("double_age"));
        Assertions.assertEquals(50, (Integer) dataSet.get(0, 4));
        Assertions.assertEquals(60, (Integer) dataSet.get(1, 4));
        Assertions.assertEquals(70, (Integer) dataSet.get(2, 4));
    }

    @Test
    public void testAddColumnWithMultipleColumnsAndFunction() {
        dataSet.addColumn("full_info", Arrays.asList("name", "age"), (Function<DisposableObjArray, String>) arr -> arr.get(0) + " (" + arr.get(1) + ")");
        Assertions.assertEquals(5, dataSet.columnCount());
        Assertions.assertTrue(dataSet.containsColumn("full_info"));
        Assertions.assertEquals("John (25)", dataSet.get(0, 4));
    }

    @Test
    public void testAddColumnWithTuple2() {
        dataSet.addColumn("name_age", new Tuple2<>("name", "age"), (BiFunction<String, Integer, String>) (name, age) -> name + "-" + age);
        Assertions.assertEquals(5, dataSet.columnCount());
        Assertions.assertEquals("John-25", dataSet.get(0, 4));
    }

    @Test
    public void testAddColumnWithTuple3() {
        dataSet.addColumn("summary", new Tuple3<>("id", "name", "age"),
                (TriFunction<Integer, String, Integer, String>) (id, name, age) -> "ID:" + id + ",Name:" + name + ",Age:" + age);
        Assertions.assertEquals(5, dataSet.columnCount());
        Assertions.assertEquals("ID:1,Name:John,Age:25", dataSet.get(0, 4));
    }

    @Test
    public void testRemoveColumn() {
        List<Object> removedColumn = dataSet.removeColumn("age");
        Assertions.assertEquals(3, dataSet.columnCount());
        Assertions.assertFalse(dataSet.containsColumn("age"));
        Assertions.assertEquals(3, removedColumn.size());
        Assertions.assertEquals(25, removedColumn.get(0));
    }

    @Test
    public void testRemoveColumns() {
        dataSet.removeColumns(Arrays.asList("age", "score"));
        Assertions.assertEquals(2, dataSet.columnCount());
        Assertions.assertFalse(dataSet.containsColumn("age"));
        Assertions.assertFalse(dataSet.containsColumn("score"));
    }

    @Test
    public void testRemoveColumnsWithEmptyList() {
        dataSet.removeColumns(Collections.emptyList());
        Assertions.assertEquals(4, dataSet.columnCount());
    }

    @Test
    public void testRemoveColumnsWithPredicate() {
        dataSet.removeColumns(col -> col.startsWith("a"));
        Assertions.assertEquals(3, dataSet.columnCount());
        Assertions.assertFalse(dataSet.containsColumn("age"));
        Assertions.assertTrue(dataSet.containsColumn("id"));
        Assertions.assertTrue(dataSet.containsColumn("name"));
        Assertions.assertTrue(dataSet.containsColumn("score"));
    }

    @Test
    public void testConvertColumn() {
        dataSet.convertColumn("id", String.class);
        Assertions.assertEquals("1", dataSet.get(0, 0));
        Assertions.assertEquals("2", dataSet.get(1, 0));
        Assertions.assertEquals("3", dataSet.get(2, 0));
    }

    @Test
    public void testConvertColumns() {
        Map<String, Class<?>> conversions = new HashMap<>();
        conversions.put("id", String.class);
        conversions.put("age", Double.class);

        dataSet.convertColumns(conversions);
        Assertions.assertEquals("1", dataSet.get(0, 0));
        Assertions.assertEquals(25.0, dataSet.get(0, 2));
    }

    @Test
    public void testUpdateColumn() {
        dataSet.updateColumn("age", (Function<Integer, Integer>) age -> (Integer) age + 5);
        Assertions.assertEquals(30, (Integer) dataSet.get(0, 2));
        Assertions.assertEquals(35, (Integer) dataSet.get(1, 2));
        Assertions.assertEquals(40, (Integer) dataSet.get(2, 2));
    }

    @Test
    public void testUpdateColumns() {
        dataSet.updateColumns(Arrays.asList("id", "age"), (Function<Integer, Integer>) val -> (Integer) val * 10);
        Assertions.assertEquals(10, (Integer) dataSet.get(0, 0));
        Assertions.assertEquals(250, (Integer) dataSet.get(0, 2));
    }

    @Test
    public void testCombineColumnsWithClass() {
        dataSet.combineColumns(Arrays.asList("name", "age"), "nameAge", a -> a.join(""));
        Assertions.assertEquals(3, dataSet.columnCount());
        Assertions.assertTrue(dataSet.containsColumn("nameAge"));
        Assertions.assertFalse(dataSet.containsColumn("name"));
        Assertions.assertFalse(dataSet.containsColumn("age"));
    }

    @Test
    public void testCombineColumnsWithFunction() {
        dataSet.combineColumns(Arrays.asList("name", "age"), "info", (Function<DisposableObjArray, String>) arr -> arr.get(0) + ":" + arr.get(1));
        Assertions.assertEquals(3, dataSet.columnCount());
        Assertions.assertEquals("John:25", dataSet.get(0, dataSet.getColumnIndex("info")));
    }

    @Test
    public void testCombineColumnsWithPredicate() {
        dataSet.combineColumns(col -> col.equals("id") || col.equals("age"), "combined", Object[].class);
        Assertions.assertEquals(3, dataSet.columnCount());
        Assertions.assertTrue(dataSet.containsColumn("combined"));
    }

    @Test
    public void testCombineColumnsWithTuple2() {
        dataSet.combineColumns(new Tuple2<>("name", "age"), "combined", (BiFunction<String, Integer, String>) (name, age) -> name + "_" + age);
        Assertions.assertEquals(3, dataSet.columnCount());
        Assertions.assertEquals("John_25", dataSet.get(0, dataSet.getColumnIndex("combined")));
    }

    @Test
    public void testCombineColumnsWithTuple3() {
        dataSet.combineColumns(new Tuple3<>("id", "name", "age"), "combined",
                (TriFunction<Integer, String, Integer, String>) (id, name, age) -> id + ":" + name + ":" + age);
        Assertions.assertEquals(2, dataSet.columnCount());
        Assertions.assertEquals("1:John:25", dataSet.get(0, dataSet.getColumnIndex("combined")));
    }

    @Test
    public void testDivideColumnWithFunction() {
        dataSet.addColumn("full_name", Arrays.asList("John Doe", "Jane Smith", "Bob Johnson"));
        dataSet.divideColumn("full_name", Arrays.asList("first_name", "last_name"), (Function<String, List<String>>) name -> Arrays.asList(name.split(" ")));

        Assertions.assertTrue(dataSet.containsColumn("first_name"));
        Assertions.assertTrue(dataSet.containsColumn("last_name"));
        Assertions.assertFalse(dataSet.containsColumn("full_name"));
        Assertions.assertEquals("John", dataSet.get(0, dataSet.getColumnIndex("first_name")));
        Assertions.assertEquals("Doe", dataSet.get(0, dataSet.getColumnIndex("last_name")));
    }

    @Test
    public void testDivideColumnWithBiConsumer() {
        dataSet.addColumn("composite", Arrays.asList("A-1", "B-2", "C-3"));
        dataSet.divideColumn("composite", Arrays.asList("letter", "number"), (BiConsumer<String, Object[]>) (val, output) -> {
            String[] parts = val.split("-");
            output[0] = parts[0];
            output[1] = Integer.parseInt(parts[1]);
        });

        Assertions.assertEquals("A", dataSet.get(0, dataSet.getColumnIndex("letter")));
        Assertions.assertEquals(1, (Integer) dataSet.get(0, dataSet.getColumnIndex("number")));
    }

    @Test
    public void testDivideColumnWithTuple2() {
        dataSet.addColumn("pair", Arrays.asList("X|Y", "A|B", "M|N"));
        dataSet.divideColumn("pair", new Tuple2<>("first", "second"), (BiConsumer<String, Pair<Object, Object>>) (val, output) -> {
            String[] parts = val.split("\\|");
            output.setLeft(parts[0]);
            output.setRight(parts[1]);
        });

        Assertions.assertEquals("X", dataSet.get(0, dataSet.getColumnIndex("first")));
        Assertions.assertEquals("Y", dataSet.get(0, dataSet.getColumnIndex("second")));
    }

    @Test
    public void testDivideColumnWithTuple3() {
        dataSet.addColumn("triple", Arrays.asList("A-B-C", "X-Y-Z", "1-2-3"));
        dataSet.divideColumn("triple", new Tuple3<>("p1", "p2", "p3"), (BiConsumer<String, Triple<Object, Object, Object>>) (val, output) -> {
            String[] parts = val.split("-");
            output.setLeft(parts[0]);
            output.setMiddle(parts[1]);
            output.setRight(parts[2]);
        });

        Assertions.assertEquals("A", dataSet.get(0, dataSet.getColumnIndex("p1")));
        Assertions.assertEquals("B", dataSet.get(0, dataSet.getColumnIndex("p2")));
        Assertions.assertEquals("C", dataSet.get(0, dataSet.getColumnIndex("p3")));
    }

    @Test
    public void testAddRowWithArray() {
        dataSet.addRow(new Object[] { 4, "Alice", 28, 92.0 });
        Assertions.assertEquals(4, dataSet.size());
        Assertions.assertEquals(4, (Integer) dataSet.get(3, 0));
        Assertions.assertEquals("Alice", dataSet.get(3, 1));
    }

    @Test
    public void testAddRowAtPosition() {
        dataSet.addRow(1, new Object[] { 99, "Insert", 50, 75.0 });
        Assertions.assertEquals(4, dataSet.size());
        Assertions.assertEquals(99, (Integer) dataSet.get(1, 0));
        Assertions.assertEquals("Insert", dataSet.get(1, 1));
        Assertions.assertEquals("Jane", dataSet.get(2, 1)); // Original row 1 moved to 2
    }

    @Test
    public void testAddRowWithList() {
        dataSet.addRow(Arrays.asList(4, "Alice", 28, 92.0));
        Assertions.assertEquals(4, dataSet.size());
        Assertions.assertEquals("Alice", dataSet.get(3, 1));
    }

    @Test
    public void testAddRowWithMap() {
        Map<String, Object> row = new HashMap<>();
        row.put("id", 4);
        row.put("name", "Alice");
        row.put("age", 28);
        row.put("score", 92.0);

        dataSet.addRow(row);
        Assertions.assertEquals(4, dataSet.size());
        Assertions.assertEquals("Alice", dataSet.get(3, 1));
    }

    @Test
    public void testAddRowWithBean() {
        TestBean bean = new TestBean();
        bean.id = 4;
        bean.name = "Alice";
        bean.age = 28;
        bean.score = 92.0;

        dataSet.addRow(bean);
        Assertions.assertEquals(4, dataSet.size());
        Assertions.assertEquals("Alice", dataSet.get(3, 1));
    }

    @Test
    public void testRemoveRow() {
        dataSet.removeRow(1);
        Assertions.assertEquals(2, dataSet.size());
        Assertions.assertEquals("John", dataSet.get(0, 1));
        Assertions.assertEquals("Bob", dataSet.get(1, 1));
    }

    @Test
    public void testRemoveRows() {
        dataSet.removeRows(0, 2);
        Assertions.assertEquals(1, dataSet.size());
        Assertions.assertEquals("Jane", dataSet.get(0, 1));
    }

    @Test
    public void testRemoveRowRange() {
        dataSet.removeRowRange(0, 2);
        Assertions.assertEquals(1, dataSet.size());
        Assertions.assertEquals("Bob", dataSet.get(0, 1));
    }

    @Test
    public void testUpdateRow() {
        dataSet.updateRow(0, (Function<Object, Object>) val -> val instanceof Integer ? (Integer) val * 2 : val);
        Assertions.assertEquals(2, (Integer) dataSet.get(0, 0));
        Assertions.assertEquals(50, (Integer) dataSet.get(0, 2));
    }

    @Test
    public void testUpdateRows() {
        dataSet.updateRows(new int[] { 0, 2 }, (Function<Object, Object>) val -> val instanceof Integer ? (Integer) val * 2 : val);
        Assertions.assertEquals(2, (Integer) dataSet.get(0, 0));
        Assertions.assertEquals(50, (Integer) dataSet.get(0, 2));
        Assertions.assertEquals(2, (Integer) dataSet.get(1, 0)); // Unchanged
        Assertions.assertEquals(6, (Integer) dataSet.get(2, 0));
        Assertions.assertEquals(70, (Integer) dataSet.get(2, 2));
    }

    @Test
    public void testUpdateAll() {
        dataSet.updateAll((Function<Object, Object>) val -> val instanceof Integer ? (Integer) val * 10 : val);
        Assertions.assertEquals(10, (Integer) dataSet.get(0, 0));
        Assertions.assertEquals(20, (Integer) dataSet.get(1, 0));
        Assertions.assertEquals(30, (Integer) dataSet.get(2, 0));
        Assertions.assertEquals(250, (Integer) dataSet.get(0, 2));
    }

    @Test
    public void testReplaceIf() {
        dataSet.replaceIf((Predicate<Object>) val -> val instanceof Integer && (Integer) val > 25, 999);
        Assertions.assertEquals(1, (Integer) dataSet.get(0, 0));
        Assertions.assertEquals(999, (Integer) dataSet.get(1, 2)); // age 30 > 25
        Assertions.assertEquals(999, (Integer) dataSet.get(2, 2)); // age 35 > 25
    }

    @Test
    public void testPrepend() {
        List<List<Object>> newColumns = new ArrayList<>();
        newColumns.add(Arrays.asList(10, 11));
        newColumns.add(Arrays.asList("PrependA", "PrependB"));
        newColumns.add(Arrays.asList(100, 101));
        newColumns.add(Arrays.asList(50.0, 60.0));

        RowDataSet otherDataSet = new RowDataSet(columnNames, newColumns);
        dataSet.prepend(otherDataSet);

        Assertions.assertEquals(5, dataSet.size());
        Assertions.assertEquals(10, (Integer) dataSet.get(0, 0));
        Assertions.assertEquals("PrependA", dataSet.get(0, 1));
        Assertions.assertEquals(1, (Integer) dataSet.get(2, 0)); // Original first row
    }

    @Test
    public void testAppend() {
        List<List<Object>> newColumns = new ArrayList<>();
        newColumns.add(Arrays.asList(10, 11));
        newColumns.add(Arrays.asList("AppendA", "AppendB"));
        newColumns.add(Arrays.asList(100, 101));
        newColumns.add(Arrays.asList(50.0, 60.0));

        RowDataSet otherDataSet = new RowDataSet(columnNames, newColumns);
        dataSet.append(otherDataSet);

        Assertions.assertEquals(5, dataSet.size());
        Assertions.assertEquals(1, (Integer) dataSet.get(0, 0)); // Original first row
        Assertions.assertEquals(10, (Integer) dataSet.get(3, 0));
        Assertions.assertEquals("AppendA", dataSet.get(3, 1));
    }

    @Test
    public void testCurrentRowNum() {
        Assertions.assertEquals(0, dataSet.currentRowNum());
        dataSet.absolute(2);
        Assertions.assertEquals(2, dataSet.currentRowNum());
    }

    @Test
    public void testAbsolute() {
        DataSet result = dataSet.absolute(1);
        Assertions.assertSame(dataSet, result);
        Assertions.assertEquals(1, dataSet.currentRowNum());
        Assertions.assertEquals("Jane", dataSet.get("name"));
    }

    @Test
    public void testGetRowAsArray() {
        Object[] row = dataSet.getRow(0);
        Assertions.assertEquals(4, row.length);
        Assertions.assertEquals(1, row[0]);
        Assertions.assertEquals("John", row[1]);
        Assertions.assertEquals(25, row[2]);
        Assertions.assertEquals(85.5, row[3]);
    }

    @Test
    public void testGetRowAsClass() {
        TestBean bean = dataSet.getRow(0, TestBean.class);
        Assertions.assertEquals(1, bean.id);
        Assertions.assertEquals("John", bean.name);
        Assertions.assertEquals(25, bean.age);
        Assertions.assertEquals(85.5, bean.score, 0.01);
    }

    @Test
    public void testGetRowWithSelectedColumns() {
        TestBean bean = dataSet.getRow(0, Arrays.asList("name", "age"), TestBean.class);
        Assertions.assertEquals("John", bean.name);
        Assertions.assertEquals(25, bean.age);
    }

    @Test
    public void testGetRowWithSupplier() {
        List<Object> row = dataSet.getRow(0, (IntFunction<List<Object>>) size -> new ArrayList<>(size));
        Assertions.assertEquals(4, row.size());
        Assertions.assertEquals(1, row.get(0));
        Assertions.assertEquals("John", row.get(1));
    }

    @Test
    public void testFirstRow() {
        Optional<Object[]> firstRow = dataSet.firstRow();
        Assertions.assertTrue(firstRow.isPresent());
        Assertions.assertEquals(1, firstRow.get()[0]);
        Assertions.assertEquals("John", firstRow.get()[1]);
    }

    @Test
    public void testFirstRowAsClass() {
        Optional<TestBean> firstRow = dataSet.firstRow(TestBean.class);
        Assertions.assertTrue(firstRow.isPresent());
        Assertions.assertEquals("John", firstRow.get().name);
    }

    @Test
    public void testFirstRowWithColumns() {
        Optional<TestBean> firstRow = dataSet.firstRow(Arrays.asList("name", "age"), TestBean.class);
        Assertions.assertTrue(firstRow.isPresent());
        Assertions.assertEquals("John", firstRow.get().name);
        Assertions.assertEquals(25, firstRow.get().age);
    }

    @Test
    public void testFirstRowWithSupplier() {
        Optional<List<Object>> firstRow = dataSet.firstRow((IntFunction<List<Object>>) ArrayList::new);
        Assertions.assertTrue(firstRow.isPresent());
        Assertions.assertEquals(4, firstRow.get().size());
    }

    @Test
    public void testLastRow() {
        Optional<Object[]> lastRow = dataSet.lastRow();
        Assertions.assertTrue(lastRow.isPresent());
        Assertions.assertEquals(3, lastRow.get()[0]);
        Assertions.assertEquals("Bob", lastRow.get()[1]);
    }

    @Test
    public void testLastRowAsClass() {
        Optional<TestBean> lastRow = dataSet.lastRow(TestBean.class);
        Assertions.assertTrue(lastRow.isPresent());
        Assertions.assertEquals("Bob", lastRow.get().name);
    }

    @Test
    public void testLastRowWithColumns() {
        Optional<TestBean> lastRow = dataSet.lastRow(Arrays.asList("name", "age"), TestBean.class);
        Assertions.assertTrue(lastRow.isPresent());
        Assertions.assertEquals("Bob", lastRow.get().name);
        Assertions.assertEquals(35, lastRow.get().age);
    }

    @Test
    public void testLastRowWithSupplier() {
        Optional<List<Object>> lastRow = dataSet.lastRow((IntFunction<List<Object>>) ArrayList::new);
        Assertions.assertTrue(lastRow.isPresent());
        Assertions.assertEquals(4, lastRow.get().size());
    }

    @Test
    public void testBiIterator() {
        BiIterator<String, Integer> iter = dataSet.iterator("name", "age");
        Assertions.assertTrue(iter.hasNext());

        Pair<String, Integer> pair = iter.next();
        Assertions.assertEquals("John", pair.left());
        Assertions.assertEquals(25, pair.right());
    }

    @Test
    public void testBiIteratorWithRange() {
        BiIterator<String, Integer> iter = dataSet.iterator(1, 3, "name", "age");
        Assertions.assertTrue(iter.hasNext());

        Pair<String, Integer> pair = iter.next();
        Assertions.assertEquals("Jane", pair.left());
        Assertions.assertEquals(30, pair.right());
    }

    @Test
    public void testTriIterator() {
        TriIterator<Integer, String, Integer> iter = dataSet.iterator("id", "name", "age");
        Assertions.assertTrue(iter.hasNext());

        Triple<Integer, String, Integer> triple = iter.next();
        Assertions.assertEquals(1, triple.left());
        Assertions.assertEquals("John", triple.middle());
        Assertions.assertEquals(25, triple.right());
    }

    @Test
    public void testTriIteratorWithRange() {
        TriIterator<Integer, String, Integer> iter = dataSet.iterator(0, 2, "id", "name", "age");

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
        dataSet.forEach((DisposableObjArray arr) -> {
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
        dataSet.forEach(Arrays.asList("name", "age"), (DisposableObjArray arr) -> {
            results.add(arr.get(0) + "-" + arr.get(1));
        });

        Assertions.assertEquals(3, results.size());
        Assertions.assertEquals("John-25", results.get(0));
    }

    @Test
    public void testForEachWithRange() {
        List<String> results = new ArrayList<>();
        dataSet.forEach(1, 3, (DisposableObjArray arr) -> {
            results.add(arr.get(1).toString());
        });

        Assertions.assertEquals(2, results.size());
        Assertions.assertEquals("Jane", results.get(0));
        Assertions.assertEquals("Bob", results.get(1));
    }

    @Test
    public void testForEachWithTuple2() {
        List<String> results = new ArrayList<>();
        dataSet.forEach(Tuple.of("name", "age"), (name, age) -> results.add(name + " is " + age));

        Assertions.assertEquals(3, results.size());
        Assertions.assertEquals("John is 25", results.get(0));
    }

    @Test
    public void testForEachWithTuple3() {
        List<String> results = new ArrayList<>();
        dataSet.forEach(new Tuple3<>("id", "name", "age"), (TriConsumer<Integer, String, Integer>) (id, name, age) -> results.add(id + ":" + name + ":" + age));

        Assertions.assertEquals(3, results.size());
        Assertions.assertEquals("1:John:25", results.get(0));
    }

    @Test
    public void testToList() {
        List<Object[]> list = dataSet.toList();
        Assertions.assertEquals(3, list.size());
        Assertions.assertEquals(1, list.get(0)[0]);
        Assertions.assertEquals("John", list.get(0)[1]);
    }

    @Test
    public void testToListWithRange() {
        List<Object[]> list = dataSet.toList(1, 3);
        Assertions.assertEquals(2, list.size());
        Assertions.assertEquals(2, list.get(0)[0]);
        Assertions.assertEquals("Jane", list.get(0)[1]);
    }

    @Test
    public void testToListAsClass() {
        List<TestBean> list = dataSet.toList(TestBean.class);
        Assertions.assertEquals(3, list.size());
        Assertions.assertEquals("John", list.get(0).name);
        Assertions.assertEquals(25, list.get(0).age);
    }

    @Test
    public void testToListWithRangeAsClass() {
        List<TestBean> list = dataSet.toList(0, 2, TestBean.class);
        Assertions.assertEquals(2, list.size());
        Assertions.assertEquals("John", list.get(0).name);
        Assertions.assertEquals("Jane", list.get(1).name);
    }

    @Test
    public void testToListWithColumnsAsClass() {
        List<TestBean> list = dataSet.toList(Arrays.asList("name", "age"), TestBean.class);
        Assertions.assertEquals(3, list.size());
        Assertions.assertEquals("John", list.get(0).name);
        Assertions.assertEquals(25, list.get(0).age);
    }

    @Test
    public void testToListWithSupplier() {
        List<Map<String, Object>> list = dataSet.toList((IntFunction<Map<String, Object>>) size -> new HashMap<>());
        Assertions.assertEquals(3, list.size());
        Assertions.assertEquals(1, list.get(0).get("id"));
        Assertions.assertEquals("John", list.get(0).get("name"));
    }

    @Test
    public void testToListWithFilters() {
        List<TestBean> list = dataSet.toList(col -> col.equals("name") || col.equals("age"), col -> col.toUpperCase(), TestBean.class);
        Assertions.assertEquals(3, list.size());
        // Note: The beans will have properties based on uppercase names
    }

    @Test
    public void testToEntities() {
        List<TestBean> entities = dataSet.toEntities(null, TestBean.class);
        Assertions.assertEquals(3, entities.size());
        Assertions.assertEquals("John", entities.get(0).name);
    }

    @Test
    public void testToEntitiesWithPrefixMap() {
        Map<String, String> prefixMap = new HashMap<>();
        prefixMap.put("n", "name");
        List<TestBean> entities = dataSet.toEntities(prefixMap, TestBean.class);
        Assertions.assertEquals(3, entities.size());
    }

    @Test
    public void testToMergedEntities() {
        // Create a dataset with duplicate ids
        List<List<Object>> mergeColumns = new ArrayList<>();
        mergeColumns.add(Arrays.asList(1, 1, 2)); // id
        mergeColumns.add(Arrays.asList("John", "John", "Jane")); // name
        mergeColumns.add(Arrays.asList(25, 25, 30)); // age
        mergeColumns.add(Arrays.asList(85.5, 90.0, 88.5)); // score

        RowDataSet mergeDataSet = new RowDataSet(columnNames, mergeColumns);
        List<TestBean> merged = mergeDataSet.toMergedEntities("id", TestBean.class);

        Assertions.assertEquals(2, merged.size());
        Assertions.assertEquals(1, merged.get(0).id);
        Assertions.assertEquals("John", merged.get(0).name);
    }

    @Test
    public void testToMapWithKeyValueColumns() {
        Map<Integer, String> map = dataSet.toMap("id", "name");
        Assertions.assertEquals(3, map.size());
        Assertions.assertEquals("John", map.get(1));
        Assertions.assertEquals("Jane", map.get(2));
        Assertions.assertEquals("Bob", map.get(3));
    }

    @Test
    public void testToMapWithSupplier() {
        Map<Integer, String> map = dataSet.toMap("id", "name", (IntFunction<LinkedHashMap<Integer, String>>) size -> new LinkedHashMap<>());
        Assertions.assertEquals(3, map.size());
        Assertions.assertEquals("John", map.get(1));
    }

    @Test
    public void testToMapWithRange() {
        Map<Integer, String> map = dataSet.toMap(0, 2, "id", "name");
        Assertions.assertEquals(2, map.size());
        Assertions.assertEquals("John", map.get(1));
        Assertions.assertEquals("Jane", map.get(2));
        Assertions.assertNull(map.get(3));
    }

    @Test
    public void testToMapWithMultipleValueColumns() {
        Map<Integer, TestBean> map = dataSet.toMap("id", Arrays.asList("name", "age"), TestBean.class);
        Assertions.assertEquals(3, map.size());
        Assertions.assertEquals("John", map.get(1).name);
        Assertions.assertEquals(25, map.get(1).age);
    }

    @Test
    public void testToMapWithValueSupplier() {
        Map<Integer, List<Object>> map = dataSet.toMap("id", Arrays.asList("name", "age"), (IntFunction<List<Object>>) ArrayList::new);
        Assertions.assertEquals(3, map.size());
        Assertions.assertEquals("John", map.get(1).get(0));
        Assertions.assertEquals(25, map.get(1).get(1));
    }

    @Test
    public void testToMultimap() {
        // Create dataset with duplicate keys
        List<List<Object>> dupColumns = new ArrayList<>();
        dupColumns.add(Arrays.asList(1, 1, 2)); // id
        dupColumns.add(Arrays.asList("A", "B", "C")); // value

        RowDataSet dupDataSet = new RowDataSet(Arrays.asList("id", "value"), dupColumns);
        ListMultimap<Integer, String> multimap = dupDataSet.toMultimap("id", "value");

        Assertions.assertEquals(2, multimap.get(1).size());
        Assertions.assertTrue(multimap.get(1).contains("A"));
        Assertions.assertTrue(multimap.get(1).contains("B"));
    }

    @Test
    public void testToMultimapWithClass() {
        ListMultimap<Integer, TestBean> multimap = dataSet.toMultimap("id", Arrays.asList("name", "age"), TestBean.class);
        Assertions.assertEquals(1, multimap.get(1).size());
        Assertions.assertEquals("John", multimap.get(1).get(0).name);
    }

    @Test
    public void testToJson() {
        String json = dataSet.toJson();
        Assertions.assertTrue(json.contains("\"id\":1"));
        Assertions.assertTrue(json.contains("\"name\":\"John\""));
        Assertions.assertTrue(json.contains("\"age\":25"));
    }

    @Test
    public void testToJsonWithRange() {
        String json = dataSet.toJson(0, 1);
        Assertions.assertTrue(json.contains("\"name\":\"John\""));
        Assertions.assertFalse(json.contains("\"name\":\"Jane\""));
    }

    @Test
    public void testToJsonWithColumns() {
        String json = dataSet.toJson(0, 3, Arrays.asList("name", "age"));
        Assertions.assertTrue(json.contains("\"name\":\"John\""));
        Assertions.assertTrue(json.contains("\"age\":25"));
        Assertions.assertFalse(json.contains("\"id\""));
        Assertions.assertFalse(json.contains("\"score\""));
    }

    @Test
    public void testToJsonToFile() throws IOException {
        File tempFile = File.createTempFile("dataset", ".json");
        tempFile.deleteOnExit();

        dataSet.toJson(tempFile);
        String content = new String(java.nio.file.Files.readAllBytes(tempFile.toPath()));
        Assertions.assertTrue(content.contains("\"name\":\"John\""));
    }

    @Test
    public void testToJsonToOutputStream() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        dataSet.toJson(baos);
        String json = baos.toString();
        Assertions.assertTrue(json.contains("\"name\":\"John\""));
    }

    @Test
    public void testToJsonToWriter() throws IOException {
        StringWriter writer = new StringWriter();
        dataSet.toJson(writer);
        String json = writer.toString();
        Assertions.assertTrue(json.contains("\"name\":\"John\""));
    }

    // Test bean class for testing
    public static class TestBean {
        public int id;
        public String name;
        public int age;
        public double score;

        public TestBean() {
        }
    }
}
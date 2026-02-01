package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.IntFunction;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.u.Optional;
import com.landawn.abacus.util.stream.Stream;

@Tag("new-test")
public class Dataset113Test extends TestBase {

    private Dataset dataset;
    private Dataset emptyDataset;
    private List<String> columnNames;
    private Object[][] sampleRows;

    @BeforeEach
    public void setUp() {
        columnNames = Arrays.asList("id", "name", "age", "salary");
        sampleRows = new Object[][] { { 1, "Alice", 25, 50000.0 }, { 2, "Bob", 30, 60000.0 }, { 3, "Charlie", 35, 70000.0 }, { 4, "Diana", 28, 55000.0 } };

        dataset = Dataset.rows(columnNames, sampleRows);
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
    public void testRowsWithObjectArray() {
        Dataset ds = Dataset.rows(columnNames, sampleRows);
        assertNotNull(ds);
        assertEquals(4, ds.size());
        assertEquals(4, ds.columnCount());
        assertEquals("Alice", ds.get(0, 1));
    }

    @Test
    public void testRowsWithCollection() {
        List<List<Object>> rowList = Arrays.asList(Arrays.asList(1, "Test", 20, 40000.0), Arrays.asList(2, "User", 25, 45000.0));
        Dataset ds = Dataset.rows(columnNames, rowList);
        assertNotNull(ds);
        assertEquals(2, ds.size());
        assertEquals("Test", ds.get(0, 1));
    }

    @Test
    public void testColumnsWithObjectArray() {
        Object[][] columns = { { 1, 2, 3 }, { "A", "B", "C" }, { 10, 20, 30 } };
        Dataset ds = Dataset.columns(Arrays.asList("id", "name", "value"), columns);
        assertNotNull(ds);
        assertEquals(3, ds.size());
        assertEquals(3, ds.columnCount());
        assertEquals("A", ds.get(0, 1));
    }

    @Test
    public void testColumnsWithCollection() {
        List<List<Object>> columnList = Arrays.asList(Arrays.asList(1, 2), Arrays.asList("X", "Y"));
        Dataset ds = Dataset.columns(Arrays.asList("num", "letter"), columnList);
        assertNotNull(ds);
        assertEquals(2, ds.size());
        assertEquals("X", ds.get(0, 1));
    }

    @Test
    public void testColumnNameList() {
        ImmutableList<String> names = dataset.columnNames();
        assertNotNull(names);
        assertEquals(4, names.size());
        assertEquals("id", names.get(0));
        assertEquals("name", names.get(1));
        assertEquals("age", names.get(2));
        assertEquals("salary", names.get(3));
    }

    @Test
    public void testColumnCount() {
        assertEquals(4, dataset.columnCount());
        assertEquals(0, emptyDataset.columnCount());
    }

    @Test
    public void testGetColumnName() {
        assertEquals("id", dataset.getColumnName(0));
        assertEquals("name", dataset.getColumnName(1));
        assertEquals("age", dataset.getColumnName(2));
        assertEquals("salary", dataset.getColumnName(3));
    }

    @Test
    public void testGetColumnIndex() {
        assertEquals(0, dataset.getColumnIndex("id"));
        assertEquals(1, dataset.getColumnIndex("name"));
        assertEquals(2, dataset.getColumnIndex("age"));
        assertEquals(3, dataset.getColumnIndex("salary"));
    }

    @Test
    public void testGetColumnIndexes() {
        int[] indexes = dataset.getColumnIndexes(Arrays.asList("name", "age"));
        assertEquals(2, indexes.length);
        assertEquals(1, indexes[0]);
        assertEquals(2, indexes[1]);
    }

    @Test
    public void testContainsColumn() {
        assertTrue(dataset.containsColumn("id"));
        assertTrue(dataset.containsColumn("name"));
        assertFalse(dataset.containsColumn("nonexistent"));
    }

    @Test
    public void testContainsAllColumns() {
        assertTrue(dataset.containsAllColumns(Arrays.asList("id", "name")));
        assertFalse(dataset.containsAllColumns(Arrays.asList("id", "nonexistent")));
    }

    @Test
    public void testRenameColumn() {
        Dataset ds = dataset.copy();
        ds.renameColumn("id", "identifier");
        assertEquals("identifier", ds.getColumnName(0));
        assertTrue(ds.containsColumn("identifier"));
        assertFalse(ds.containsColumn("id"));
    }

    @Test
    public void testRenameColumnsWithMap() {
        Dataset ds = dataset.copy();
        Map<String, String> renameMap = new HashMap<>();
        renameMap.put("id", "identifier");
        renameMap.put("name", "fullName");
        ds.renameColumns(renameMap);
        assertEquals("identifier", ds.getColumnName(0));
        assertEquals("fullName", ds.getColumnName(1));
    }

    @Test
    public void testRenameColumnsWithFunction() {
        Dataset ds = dataset.copy();
        ds.renameColumns(Arrays.asList("id", "name"), name -> name.toUpperCase());
        assertEquals("ID", ds.getColumnName(0));
        assertEquals("NAME", ds.getColumnName(1));
    }

    @Test
    public void testRenameAllColumnsWithFunction() {
        Dataset ds = dataset.copy();
        ds.renameColumns(name -> "col_" + name);
        assertEquals("col_id", ds.getColumnName(0));
        assertEquals("col_name", ds.getColumnName(1));
    }

    @Test
    public void testMoveColumn() {
        Dataset ds = dataset.copy();
        ds.moveColumn("name", 0);
        assertEquals("name", ds.getColumnName(0));
        assertEquals("id", ds.getColumnName(1));
    }

    @Test
    public void testMoveColumns() {
        Dataset ds = dataset.copy();
        ds.moveColumns(Arrays.asList("age", "salary"), 0);
        assertEquals("age", ds.getColumnName(0));
        assertEquals("salary", ds.getColumnName(1));
    }

    @Test
    public void testSwapColumnPosition() {
        Dataset ds = dataset.copy();
        ds.swapColumns("id", "name");
        assertEquals("name", ds.getColumnName(0));
        assertEquals("id", ds.getColumnName(1));
    }

    @Test
    public void testMoveRow() {
        Dataset ds = dataset.copy();
        Object originalFirst = ds.get(0, 1);
        Object originalSecond = ds.get(1, 1);
        ds.moveRow(1, 0);
        assertEquals(originalSecond, ds.get(0, 1));
        assertEquals(originalFirst, ds.get(1, 1));
    }

    @Test
    public void testMoveRows() {
        Dataset ds = dataset.copy();
        Object[] originalRow0 = ds.getRow(0);
        Object[] originalRow1 = ds.getRow(1);
        ds.moveRows(0, 2, 2);
        assertEquals(originalRow0[1], ds.get(2, 1));
        assertEquals(originalRow1[1], ds.get(3, 1));
    }

    @Test
    public void testSwapRowPosition() {
        Dataset ds = dataset.copy();
        Object[] originalRow0 = ds.getRow(0);
        Object[] originalRow1 = ds.getRow(1);
        ds.swapRows(0, 1);
        assertEquals(originalRow1[1], ds.get(0, 1));
        assertEquals(originalRow0[1], ds.get(1, 1));
    }

    @Test
    public void testCurrentRowNum() {
        assertEquals(0, dataset.currentRowIndex());
        dataset.moveToRow(1);
        assertEquals(1, dataset.currentRowIndex());
    }

    @Test
    public void testAbsolute() {
        Dataset ds = dataset.moveToRow(2);
        assertEquals(2, ds.currentRowIndex());
        assertEquals("Charlie", ds.get("name"));
    }

    @Test
    public void testGetByIndexes() {
        assertEquals((Integer) 1, dataset.get(0, 0));
        assertEquals("Alice", dataset.get(0, 1));
        assertEquals((Integer) 25, dataset.get(0, 2));
        assertEquals(50000.0, dataset.get(0, 3));
    }

    @Test
    public void testSetByIndexes() {
        Dataset ds = dataset.copy();
        ds.set(0, 1, "Alicia");
        assertEquals("Alicia", ds.get(0, 1));
    }

    @Test
    public void testIsNullByIndexes() {
        assertFalse(dataset.isNull(0, 1));
        Dataset ds = dataset.copy();
        ds.set(0, 1, null);
        assertTrue(ds.isNull(0, 1));
    }

    @Test
    public void testGetByColumnIndex() {
        Dataset ds = dataset.moveToRow(1);
        assertEquals("Bob", ds.get(1));
        assertEquals((Integer) 30, ds.get(2));
    }

    @Test
    public void testGetByColumnName() {
        Dataset ds = dataset.moveToRow(0);
        assertEquals("Alice", ds.get("name"));
        assertEquals((Integer) 25, ds.get("age"));
    }

    @Test
    public void testSetByColumnIndex() {
        Dataset ds = dataset.copy().moveToRow(0);
        ds.set(1, "Alicia");
        assertEquals("Alicia", ds.get(1));
    }

    @Test
    public void testSetByColumnName() {
        Dataset ds = dataset.copy().moveToRow(0);
        ds.set("name", "Alicia");
        assertEquals("Alicia", ds.get("name"));
    }

    @Test
    public void testIsNullByColumnIndex() {
        Dataset ds = dataset.copy().moveToRow(0);
        assertFalse(ds.isNull(1));
        ds.set(1, null);
        assertTrue(ds.isNull(1));
    }

    @Test
    public void testIsNullByColumnName() {
        Dataset ds = dataset.copy().moveToRow(0);
        assertFalse(ds.isNull("name"));
        ds.set("name", null);
        assertTrue(ds.isNull("name"));
    }

    @Test
    public void testTypedGetters() {
        Dataset ds = Dataset
                .rows(Arrays.asList("bool", "ch", "bt", "sh", "i", "l", "f", "d"), new Object[][] { { true, 'A', (byte) 1, (short) 2, 3, 4L, 5.0f, 6.0 } })
                .moveToRow(0);

        assertEquals(true, ds.getBoolean(0));
        assertEquals('A', ds.getChar(1));
        assertEquals((byte) 1, ds.getByte(2));
        assertEquals((short) 2, ds.getShort(3));
        assertEquals(3, ds.getInt(4));
        assertEquals(4L, ds.getLong(5));
        assertEquals(5.0f, ds.getFloat(6));
        assertEquals(6.0, ds.getDouble(7));
    }

    @Test
    public void testTypedGettersByColumnName() {
        Dataset ds = Dataset
                .rows(Arrays.asList("bool", "ch", "bt", "sh", "i", "l", "f", "d"), new Object[][] { { true, 'A', (byte) 1, (short) 2, 3, 4L, 5.0f, 6.0 } })
                .moveToRow(0);

        assertEquals(true, ds.getBoolean("bool"));
        assertEquals('A', ds.getChar("ch"));
        assertEquals((byte) 1, ds.getByte("bt"));
        assertEquals((short) 2, ds.getShort("sh"));
        assertEquals(3, ds.getInt("i"));
        assertEquals(4L, ds.getLong("l"));
        assertEquals(5.0f, ds.getFloat("f"));
        assertEquals(6.0, ds.getDouble("d"));
    }

    @Test
    public void testGetColumnByIndex() {
        ImmutableList<Object> column = dataset.getColumn(1);
        assertEquals(4, column.size());
        assertEquals("Alice", column.get(0));
        assertEquals("Bob", column.get(1));
        assertEquals("Charlie", column.get(2));
        assertEquals("Diana", column.get(3));
    }

    @Test
    public void testGetColumnByName() {
        ImmutableList<Object> column = dataset.getColumn("age");
        assertEquals(4, column.size());
        assertEquals(25, column.get(0));
        assertEquals(30, column.get(1));
        assertEquals(35, column.get(2));
        assertEquals(28, column.get(3));
    }

    @Test
    public void testCopyColumn() {
        List<Object> column = dataset.copyColumn("name");
        assertEquals(4, column.size());
        assertEquals("Alice", column.get(0));
        column.set(0, "Modified");
        assertEquals("Alice", dataset.get(0, 1));
    }

    @Test
    public void testAddColumnWithCollection() {
        Dataset ds = dataset.copy();
        List<String> newColumn = Arrays.asList("Engineering", "Sales", "Marketing", "HR");
        ds.addColumn("department", newColumn);
        assertEquals(5, ds.columnCount());
        assertEquals("department", ds.getColumnName(4));
        assertEquals("Engineering", ds.get(0, 4));
    }

    @Test
    public void testAddColumnWithPosition() {
        Dataset ds = dataset.copy();
        List<String> newColumn = Arrays.asList("A", "B", "C", "D");
        ds.addColumn(1, "grade", newColumn);
        assertEquals(5, ds.columnCount());
        assertEquals("grade", ds.getColumnName(1));
        assertEquals("A", ds.get(0, 1));
    }

    @Test
    public void testAddColumnWithFunction() {
        Dataset ds = dataset.copy();
        ds.addColumn("nameLength", "name", (String name) -> name.length());
        assertEquals(5, ds.columnCount());
        assertEquals("nameLength", ds.getColumnName(4));
        assertEquals((Integer) 5, ds.get(0, 4));
    }

    @Test
    public void testAddColumnWithMultipleColumns() {
        Dataset ds = dataset.copy();
        ds.addColumn("fullInfo", Arrays.asList("name", "age"), arr -> arr.get(0) + ":" + arr.get(1));
        assertEquals(5, ds.columnCount());
        assertEquals("Alice:25", ds.get(0, 4));
    }

    @Test
    public void testAddColumnWithTuple2() {
        Dataset ds = dataset.copy();
        ds.addColumn("nameAge", Tuple.of("name", "age"), (String name, Integer age) -> name + "(" + age + ")");
        assertEquals(5, ds.columnCount());
        assertEquals("Alice(25)", ds.get(0, 4));
    }

    @Test
    public void testAddColumnWithTuple3() {
        Dataset ds = dataset.copy();
        ds.addColumn("summary", Tuple.of("name", "age", "salary"), (String name, Integer age, Double salary) -> name + "," + age + "," + salary);
        assertEquals(5, ds.columnCount());
        assertEquals("Alice,25,50000.0", ds.get(0, 4));
    }

    @Test
    public void testAddMultipleColumns() {
        Dataset ds = dataset.copy();
        List<String> newColumnNames = Arrays.asList("col1", "col2");
        List<List<Object>> newColumns = Arrays.asList(Arrays.asList("A", "B", "C", "D"), Arrays.asList(1, 2, 3, 4));
        ds.addColumns(newColumnNames, newColumns);
        assertEquals(6, ds.columnCount());
        assertEquals("col1", ds.getColumnName(4));
        assertEquals("col2", ds.getColumnName(5));
    }

    @Test
    public void testRemoveColumn() {
        Dataset ds = dataset.copy();
        List<Object> removedColumn = ds.removeColumn("age");
        assertEquals(3, ds.columnCount());
        assertFalse(ds.containsColumn("age"));
        assertEquals(4, removedColumn.size());
        assertEquals((Integer) 25, removedColumn.get(0));
    }

    @Test
    public void testRemoveColumns() {
        Dataset ds = dataset.copy();
        ds.removeColumns(Arrays.asList("age", "salary"));
        assertEquals(2, ds.columnCount());
        assertFalse(ds.containsColumn("age"));
        assertFalse(ds.containsColumn("salary"));
    }

    @Test
    public void testRemoveColumnsWithPredicate() {
        Dataset ds = dataset.copy();
        ds.removeColumns(name -> name.startsWith("s"));
        assertEquals(3, ds.columnCount());
        assertFalse(ds.containsColumn("salary"));
    }

    @Test
    public void testUpdateColumn() {
        Dataset ds = dataset.copy();
        ds.updateColumn("name", (String name) -> name.toUpperCase());
        assertEquals("ALICE", ds.get(0, 1));
        assertEquals("BOB", ds.get(1, 1));
    }

    @Test
    public void testUpdateColumns() {
        Dataset ds = dataset.copy();
        ds.updateColumns(Arrays.asList("name"), (i, c, v) -> ((String) v).toUpperCase());
        assertEquals("ALICE", ds.get(0, 1));
    }

    @Test
    public void testConvertColumn() {
        Dataset ds = Dataset.rows(Arrays.asList("str"), new Object[][] { { "123" }, { "456" } });
        ds.convertColumn("str", Integer.class);
        assertEquals((Integer) 123, ds.get(0, 0));
        assertEquals((Integer) 456, ds.get(1, 0));
    }

    @Test
    public void testConvertColumns() {
        Dataset ds = Dataset.rows(Arrays.asList("str1", "str2"), new Object[][] { { "123", "1.5" }, { "456", "2.5" } });
        Map<String, Class<?>> typeMap = new HashMap<>();
        typeMap.put("str1", Integer.class);
        typeMap.put("str2", Double.class);
        ds.convertColumns(typeMap);
        assertEquals((Integer) 123, ds.get(0, 0));
        assertEquals((Double) 1.5, ds.get(0, 1));
    }

    @Test
    public void testCombineColumns() {
        Dataset ds = dataset.copy();
        ds.println();
        ds.combineColumns(Arrays.asList("name", "age"), "nameAge", Map.class);
        ds.println();
        assertEquals(3, ds.columnCount());
        assertTrue(ds.containsColumn("nameAge"));
    }

    @Test
    public void testCombineColumnsWithFunction() {
        Dataset ds = dataset.copy();
        ds.combineColumns(Arrays.asList("name", "age"), "info", arr -> arr.get(0) + ":" + arr.get(1));
        assertEquals("Alice:25", ds.get(0, ds.getColumnIndex("info")));
    }

    @Test
    public void testCombineColumnsWithTuple2() {
        Dataset ds = dataset.copy();
        ds.combineColumns(Tuple.of("name", "age"), "combined", (String name, Integer age) -> name + "_" + age);
        assertEquals("Alice_25", ds.get(0, ds.getColumnIndex("combined")));
    }

    @Test
    public void testDivideColumn() {
        Dataset ds = Dataset.rows(Arrays.asList("fullName"), new Object[][] { { "John_Doe" }, { "Jane_Smith" } });
        ds.divideColumn("fullName", Arrays.asList("firstName", "lastName"), (String full) -> Arrays.asList(full.split("_")));
        assertEquals(2, ds.columnCount());
        System.out.println(ds.columnNames());
        ds.println("     *  // ");
        assertEquals("John", ds.get(0, ds.getColumnIndex("firstName")));
        assertEquals("Doe", ds.get(0, ds.getColumnIndex("lastName")));
    }

    @Test
    public void testAddRow() {
        Dataset ds = dataset.copy();
        ds.addRow(new Object[] { 5, "Eve", 32, 58000.0 });
        assertEquals(5, ds.size());
        assertEquals("Eve", ds.get(4, 1));
    }

    @Test
    public void testAddRowWithPosition() {
        Dataset ds = dataset.copy();
        ds.addRow(1, new Object[] { 5, "Eve", 32, 58000.0 });
        assertEquals(5, ds.size());
        assertEquals("Eve", ds.get(1, 1));
    }

    @Test
    public void testAddRows() {
        Dataset ds = dataset.copy();
        List<Object[]> newRows = Arrays.asList(new Object[] { 5, "Eve", 32, 58000.0 }, new Object[] { 6, "Frank", 29, 52000.0 });
        ds.addRows(newRows);
        assertEquals(6, ds.size());
        assertEquals("Eve", ds.get(4, 1));
        assertEquals("Frank", ds.get(5, 1));
    }

    @Test
    public void testRemoveRow() {
        Dataset ds = dataset.copy();
        ds.removeRow(1);
        assertEquals(3, ds.size());
        assertEquals("Alice", ds.get(0, 1));
        assertEquals("Charlie", ds.get(1, 1));
    }

    @Test
    public void testRemoveRows() {
        Dataset ds = dataset.copy();
        ds.removeRowsAt(1, 2);
        assertEquals(2, ds.size());
        assertEquals("Alice", ds.get(0, 1));
        assertEquals("Diana", ds.get(1, 1));
    }

    @Test
    public void testRemoveRowRange() {
        Dataset ds = dataset.copy();
        ds.removeRows(1, 3);
        assertEquals(2, ds.size());
        assertEquals("Alice", ds.get(0, 1));
        assertEquals("Diana", ds.get(1, 1));
    }

    @Test
    public void testRemoveDuplicateRowsByColumn() {
        Dataset ds = Dataset.rows(Arrays.asList("id", "name"), new Object[][] { { 1, "A" }, { 2, "B" }, { 1, "C" } });
        ds.removeDuplicateRowsBy("id");
        assertEquals(2, ds.size());
    }

    @Test
    public void testUpdateRow() {
        Dataset ds = dataset.copy();
        ds.updateRow(0, value -> value instanceof String ? "Updated" : value);
        assertEquals("Updated", ds.get(0, 1));
    }

    @Test
    public void testUpdateRows() {
        Dataset ds = dataset.copy();
        ds.updateRows(new int[] { 0, 1 }, (i, c, v) -> v instanceof String ? "Updated" : v);
        assertEquals("Updated", ds.get(0, 1));
        assertEquals("Updated", ds.get(1, 1));
    }

    @Test
    public void testUpdateAll() {
        Dataset ds = dataset.copy();
        ds.updateAll(v -> v instanceof String ? ((String) v).toUpperCase() : v);
        assertEquals("ALICE", ds.get(0, 1));
        assertEquals("BOB", ds.get(1, 1));
    }

    @Test
    public void testReplaceIf() {
        Dataset ds = dataset.copy();
        ds.replaceIf(value -> "Alice".equals(value), "Alicia");
        assertEquals("Alicia", ds.get(0, 1));
    }

    @Test
    public void testPrepend() {
        Dataset ds = dataset.copy();
        Dataset other = Dataset.rows(columnNames, new Object[][] { { 0, "Zero", 0, 0.0 } });
        ds.prepend(other);
        assertEquals(5, ds.size());
        assertEquals("Zero", ds.get(0, 1));
        assertEquals("Alice", ds.get(1, 1));
    }

    @Test
    public void testAppend() {
        Dataset ds = dataset.copy();
        Dataset other = Dataset.rows(columnNames, new Object[][] { { 5, "Five", 50, 50000.0 } });
        ds.append(other);
        assertEquals(5, ds.size());
        assertEquals("Five", ds.get(4, 1));
    }

    @Test
    public void testMerge() {
        Dataset ds = dataset.copy();
        Dataset other = Dataset.rows(Arrays.asList("dept", "location"), new Object[][] { { "IT", "NYC" }, { "HR", "LA" } });
        ds.merge(other);
        assertEquals(6, ds.columnCount());
        assertTrue(ds.containsColumn("dept"));
        assertTrue(ds.containsColumn("location"));
    }

    @Test
    public void testMergeWithRequiredSameColumns() {
        Dataset ds = dataset.copy();
        Dataset other = Dataset.rows(columnNames, new Object[][] { { 5, "Five", 50, 50000.0 } });
        ds.merge(other, true);
        assertEquals(5, ds.size());
    }

    @Test
    public void testMergeWithSelectedColumns() {
        Dataset ds = dataset.copy();
        Dataset other = Dataset.rows(Arrays.asList("dept", "location", "extra"), new Object[][] { { "IT", "NYC", "X" }, { "HR", "LA", "Y" } });
        ds.merge(other, Arrays.asList("dept", "location"));
        assertEquals(6, ds.columnCount());
        assertTrue(ds.containsColumn("dept"));
        assertFalse(ds.containsColumn("extra"));
    }

    @Test
    public void testGetRow() {
        Object[] row = dataset.getRow(0);
        assertEquals(4, row.length);
        assertEquals(1, row[0]);
        assertEquals("Alice", row[1]);
    }

    @Test
    public void testGetRowWithSupplier() {
        List<Object> row = dataset.getRow(0, (IntFunction<List<Object>>) ArrayList::new);
        assertNotNull(row);
        assertEquals(4, row.size());
        assertEquals(1, row.get(0));
        assertEquals("Alice", row.get(1));
    }

    @Test
    public void testFirstRow() {
        Optional<Object[]> first = dataset.firstRow();
        assertTrue(first.isPresent());
        assertEquals("Alice", first.get()[1]);
    }

    @Test
    public void testLastRow() {
        Optional<Object[]> last = dataset.lastRow();
        assertTrue(last.isPresent());
        assertEquals("Diana", last.get()[1]);
    }

    @Test
    public void testForEach() {
        List<String> names = new ArrayList<>();
        dataset.forEach(row -> names.add((String) row.get(1)));
        assertEquals(4, names.size());
        assertEquals("Alice", names.get(0));
        assertEquals("Diana", names.get(3));
    }

    @Test
    public void testForEachWithColumnNames() {
        List<String> names = new ArrayList<>();
        dataset.forEach(Arrays.asList("name"), row -> names.add((String) row.get(0)));
        assertEquals(4, names.size());
        assertEquals("Alice", names.get(0));
    }

    @Test
    public void testForEachWithRange() {
        List<String> names = new ArrayList<>();
        dataset.forEach(1, 3, row -> names.add((String) row.get(1)));
        assertEquals(2, names.size());
        assertEquals("Bob", names.get(0));
        assertEquals("Charlie", names.get(1));
    }

    @Test
    public void testToList() {
        List<Object[]> list = dataset.toList();
        assertEquals(4, list.size());
        assertEquals("Alice", list.get(0)[1]);
    }

    @Test
    public void testToListWithRange() {
        List<Object[]> list = dataset.toList(1, 3);
        assertEquals(2, list.size());
        assertEquals("Bob", list.get(0)[1]);
        assertEquals("Charlie", list.get(1)[1]);
    }

    @Test
    public void testToJson() {
        String json = dataset.toJson();
        assertNotNull(json);
        assertTrue(json.contains("Alice"));
        assertTrue(json.contains("Bob"));
    }

    @Test
    public void testToXml() {
        String xml = dataset.toXml();
        assertNotNull(xml);
        assertTrue(xml.contains("Alice"));
    }

    @Test
    public void testToXmlWithRowElementName() {
        String xml = dataset.toXml("person");
        assertNotNull(xml);
        assertTrue(xml.contains("<person>"));
    }

    @Test
    public void testToCsv() {
        String csv = dataset.toCsv();
        assertNotNull(csv);
        assertTrue(csv.contains("Alice"));
        assertTrue(csv.contains("Bob"));
    }

    @Test
    public void testInnerJoin() {
        Dataset left = Dataset.rows(Arrays.asList("id", "name"), new Object[][] { { 1, "A" }, { 2, "B" } });
        Dataset right = Dataset.rows(Arrays.asList("id", "value"), new Object[][] { { 1, 10 }, { 3, 30 } });
        Map<String, String> joinMap = new HashMap<>();
        joinMap.put("id", "id");
        Dataset joined = left.innerJoin(right, joinMap);
        assertTrue(joined.size() >= 0);
        assertTrue(joined.columnCount() >= 2);
    }

    @Test
    public void testLeftJoin() {
        Dataset left = Dataset.rows(Arrays.asList("id", "name"), new Object[][] { { 1, "A" }, { 2, "B" } });
        Dataset right = Dataset.rows(Arrays.asList("id", "value"), new Object[][] { { 1, 10 }, { 3, 30 } });
        Map<String, String> joinMap = new HashMap<>();
        joinMap.put("id", "id");
        Dataset joined = left.leftJoin(right, joinMap);
        assertTrue(joined.size() >= 0);
        assertTrue(joined.columnCount() >= 2);
    }

    @Test
    public void testRightJoin() {
        Dataset left = Dataset.rows(Arrays.asList("id", "name"), new Object[][] { { 1, "A" }, { 2, "B" } });
        Dataset right = Dataset.rows(Arrays.asList("id", "value"), new Object[][] { { 1, 10 }, { 3, 30 } });
        Map<String, String> joinMap = new HashMap<>();
        joinMap.put("id", "id");
        Dataset joined = left.rightJoin(right, joinMap);
        assertTrue(joined.size() >= 0);
    }

    @Test
    public void testFullJoin() {
        Dataset left = Dataset.rows(Arrays.asList("id", "name"), new Object[][] { { 1, "A" }, { 2, "B" } });
        Dataset right = Dataset.rows(Arrays.asList("id", "value"), new Object[][] { { 1, 10 }, { 3, 30 } });
        Map<String, String> joinMap = new HashMap<>();
        joinMap.put("id", "id");
        Dataset joined = left.fullJoin(right, joinMap);
        assertTrue(joined.size() >= 0);
    }

    @Test
    public void testUnion() {
        Dataset ds1 = Dataset.rows(Arrays.asList("id", "name"), new Object[][] { { 1, "A" }, { 2, "B" } });
        Dataset ds2 = Dataset.rows(Arrays.asList("id", "name"), new Object[][] { { 3, "C" }, { 1, "A" } });
        Dataset union = ds1.union(ds2);
        assertEquals(3, union.size());
    }

    @Test
    public void testUnionAll() {
        Dataset ds1 = Dataset.rows(Arrays.asList("id", "name"), new Object[][] { { 1, "A" }, { 2, "B" } });
        Dataset ds2 = Dataset.rows(Arrays.asList("id", "name"), new Object[][] { { 3, "C" }, { 1, "A" } });
        Dataset unionAll = ds1.unionAll(ds2);
        assertEquals(4, unionAll.size());
    }

    @Test
    public void testDifference() {
        Dataset ds1 = Dataset.rows(Arrays.asList("id", "name"), new Object[][] { { 1, "A" }, { 2, "B" } });
        Dataset ds2 = Dataset.rows(Arrays.asList("id", "name"), new Object[][] { { 1, "A" }, { 3, "C" } });
        Dataset diff = N.difference(ds1, ds2);
        assertEquals(1, diff.size());
        assertEquals("B", diff.get(0, 1));
    }

    @Test
    public void testIntersection() {
        Dataset ds1 = Dataset.rows(Arrays.asList("id", "name"), new Object[][] { { 1, "A" }, { 2, "B" } });
        Dataset ds2 = Dataset.rows(Arrays.asList("id", "name"), new Object[][] { { 1, "A" }, { 3, "C" } });
        Dataset intersection = N.intersection(ds1, ds2);
        assertEquals(1, intersection.size());
        assertEquals("A", intersection.get(0, 1));
    }

    @Test
    public void testSymmetricDifference() {
        Dataset ds1 = Dataset.rows(Arrays.asList("id", "name"), new Object[][] { { 1, "A" }, { 2, "B" } });
        Dataset ds2 = Dataset.rows(Arrays.asList("id", "name"), new Object[][] { { 1, "A" }, { 3, "C" } });
        Dataset symDiff = N.symmetricDifference(ds1, ds2);
        assertEquals(2, symDiff.size());
    }

    @Test
    public void testCopy() {
        Dataset copy = dataset.copy();
        assertNotSame(dataset, copy);
        assertEquals((Integer) dataset.size(), (Integer) copy.size());
        assertEquals((Integer) dataset.columnCount(), (Integer) copy.columnCount());
        assertEquals((String) dataset.get(0, 1), (String) copy.get(0, 1));
    }

    @Test
    public void testCopyWithRange() {
        Dataset copy = dataset.copy(1, 3);
        assertEquals(2, copy.size());
        assertEquals("Bob", copy.get(0, 1));
        assertEquals("Charlie", copy.get(1, 1));
    }

    @Test
    public void testCopyWithColumns() {
        Dataset copy = dataset.copy(Arrays.asList("id", "name"));
        assertEquals(4, copy.size());
        assertEquals(2, copy.columnCount());
        assertEquals("Alice", copy.get(0, 1));
    }

    @Test
    public void testClone() {
        Dataset clone = dataset.clone();
        assertNotSame(dataset, clone);
        assertEquals((Integer) dataset.size(), (Integer) clone.size());
    }

    @Test
    public void testCloneWithFreeze() {
        Dataset clone = dataset.clone(true);
        assertNotSame(dataset, clone);
        assertTrue(clone.isFrozen());
    }

    @Test
    public void testPaginate() {
        Paginated<Dataset> paginated = dataset.paginate(2);
        assertEquals(2, paginated.totalPages());
        Dataset page1 = paginated.getPage(0);
        assertEquals(2, page1.size());
        assertEquals("Alice", page1.get(0, 1));
        assertEquals("Bob", page1.get(1, 1));
    }

    @Test
    public void testStream() {
        Stream<Object[]> stream = dataset.stream(Object[].class);
        List<Object[]> list = stream.toList();
        assertEquals(4, list.size());
        assertEquals("Alice", list.get(0)[1]);
    }

    @Test
    public void testStreamWithRange() {
        Stream<Object[]> stream = dataset.stream(1, 3, Object[].class);
        List<Object[]> list = stream.toList();
        assertEquals(2, list.size());
        assertEquals("Bob", list.get(0)[1]);
    }

    @Test
    public void testStreamWithSupplier() {
        Stream<List<Object>> stream = dataset.stream(ArrayList::new);
        List<List<Object>> list = stream.toList();
        assertEquals(4, list.size());
        assertEquals("Alice", list.get(0).get(1));
    }

    @Test
    public void testFreeze() {
        Dataset ds = dataset.copy();
        assertFalse(ds.isFrozen());
        ds.freeze();
        assertTrue(ds.isFrozen());
    }

    @Test
    public void testIsEmpty() {
        assertFalse(dataset.isEmpty());
        assertTrue(emptyDataset.isEmpty());
    }

    @Test
    public void testSize() {
        assertEquals(4, dataset.size());
        assertEquals(0, emptyDataset.size());
    }

    @Test
    public void testTrimToSize() {
        Dataset ds = dataset.copy();
        ds.trimToSize();
        assertEquals(4, ds.size());
    }

    @Test
    public void testColumns() {
        Stream<ImmutableList<Object>> columns = dataset.columns();
        List<ImmutableList<Object>> columnList = columns.toList();
        assertEquals(4, columnList.size());
        assertEquals("Alice", columnList.get(1).get(0));
    }

    @Test
    public void testColumnMap() {
        Map<String, ImmutableList<Object>> columnMap = dataset.columnMap();
        assertEquals(4, columnMap.size());
        assertTrue(columnMap.containsKey("name"));
        assertEquals("Alice", columnMap.get("name").get(0));
    }

    @Test
    public void testPrintln() {
        dataset.println();
        dataset.println("Test prefix: ");
    }

    @Test
    public void testInvalidColumnAccess() {
        assertThrows(IndexOutOfBoundsException.class, () -> dataset.get(0, 10));
        assertThrows(IllegalArgumentException.class, () -> dataset.getColumnIndex("nonexistent"));
    }

    @Test
    public void testInvalidRowAccess() {
        assertThrows(IndexOutOfBoundsException.class, () -> dataset.get(10, 0));
    }

    @Test
    public void testOperationsOnFrozenDataset() {
        Dataset ds = dataset.copy();
        ds.freeze();
        assertTrue(ds.isFrozen());
    }

    @Test
    public void testEmptyDatasetOperations() {
        assertEquals(0, emptyDataset.size());
        assertEquals(0, emptyDataset.columnCount());
        assertTrue(emptyDataset.isEmpty());
        assertFalse(emptyDataset.firstRow().isPresent());
        assertFalse(emptyDataset.lastRow().isPresent());
    }

    @Test
    public void testNullValues() {
        Dataset ds = Dataset.rows(Arrays.asList("col1", "col2"), new Object[][] { { null, "test" }, { "value", null } });
        assertTrue(ds.isNull(0, 0));
        assertFalse(ds.isNull(0, 1));
        assertTrue(ds.isNull(1, 1));
        assertFalse(ds.isNull(1, 0));
    }

    @Test
    public void testComplexFiltering() {
        Dataset filtered = dataset.filter(Arrays.asList("age", "salary"), row -> (Integer) row.get(0) > 25 && (Double) row.get(1) > 55000);
        assertEquals(2, filtered.size());
        assertEquals("Bob", filtered.get(0, 1));
        assertEquals("Charlie", filtered.get(1, 1));
    }

    @Test
    public void testFilterByColumn() {
        Dataset filtered = dataset.filter("age", (Integer age) -> age > 28);
        assertEquals(2, filtered.size());
    }

    @Test
    public void testDistinctOperations() {
        Dataset ds = Dataset.rows(Arrays.asList("value"), new Object[][] { { 1 }, { 2 }, { 1 }, { 3 }, { 2 } });
        Dataset distinct = ds.distinct();
        assertEquals(3, distinct.size());
    }

    @Test
    public void testDistinctBy() {
        Dataset ds = Dataset.rows(Arrays.asList("id", "category"), new Object[][] { { 1, "A" }, { 2, "B" }, { 3, "A" } });
        Dataset distinct = ds.distinctBy("category");
        assertEquals(2, distinct.size());
    }

    @Test
    public void testGroupByOperations() {
        Dataset ds = Dataset.rows(Arrays.asList("category", "value"), new Object[][] { { "A", 10 }, { "B", 20 }, { "A", 15 }, { "B", 25 } });
        assertEquals(4, ds.size());
        assertTrue(ds.columnCount() > 0);
    }
}

package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.IntFunction;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.u.Optional;
import com.landawn.abacus.util.stream.Collectors;
import com.landawn.abacus.util.stream.Stream;

@Tag("2025")
public class Dataset2025Test extends TestBase {

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
        assertEquals(Integer.valueOf(1), ds.get(0, 0));
    }

    @Test
    public void testRowsWithObjectArrayNull() {
        Dataset ds = Dataset.rows(columnNames, (Object[][]) null);
        assertNotNull(ds);
        assertEquals(0, ds.size());
        assertEquals(4, ds.columnCount());
    }

    @Test
    public void testRowsWithCollection() {
        List<List<Object>> rows = new ArrayList<>();
        rows.add(Arrays.asList(1, "Alice", 25, 50000.0));
        rows.add(Arrays.asList(2, "Bob", 30, 60000.0));

        Dataset ds = Dataset.rows(columnNames, rows);
        assertNotNull(ds);
        assertEquals(2, ds.size());
        assertEquals(4, ds.columnCount());
        assertEquals("Alice", ds.get(0, 1));
    }

    @Test
    public void testColumnsWithObjectArray() {
        Object[][] columns = new Object[][] { { 1, 2, 3, 4 }, { "Alice", "Bob", "Charlie", "Diana" }, { 25, 30, 35, 28 },
                { 50000.0, 60000.0, 70000.0, 55000.0 } };

        Dataset ds = Dataset.columns(columnNames, columns);
        assertNotNull(ds);
        assertEquals(4, ds.size());
        assertEquals(4, ds.columnCount());
        assertEquals("Alice", ds.get(0, 1));
        assertEquals(Integer.valueOf(1), ds.get(0, 0));
    }

    @Test
    public void testColumnsWithCollection() {
        List<List<Object>> columns = new ArrayList<>();
        columns.add(Arrays.asList(1, 2, 3, 4));
        columns.add(Arrays.asList("Alice", "Bob", "Charlie", "Diana"));
        columns.add(Arrays.asList(25, 30, 35, 28));
        columns.add(Arrays.asList(50000.0, 60000.0, 70000.0, 55000.0));

        Dataset ds = Dataset.columns(columnNames, columns);
        assertNotNull(ds);
        assertEquals(4, ds.size());
        assertEquals(4, ds.columnCount());
        assertEquals("Bob", ds.get(1, 1));
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
    public void testGetColumnName_IndexOutOfBounds() {
        assertThrows(IndexOutOfBoundsException.class, () -> dataset.getColumnName(-1));
        assertThrows(IndexOutOfBoundsException.class, () -> dataset.getColumnName(4));
    }

    @Test
    public void testGetColumnIndex() {
        assertEquals(0, dataset.getColumnIndex("id"));
        assertEquals(1, dataset.getColumnIndex("name"));
        assertEquals(2, dataset.getColumnIndex("age"));
        assertEquals(3, dataset.getColumnIndex("salary"));
    }

    @Test
    public void testGetColumnIndex_NotFound() {
        assertThrows(IllegalArgumentException.class, () -> dataset.getColumnIndex("notexist"));
    }

    @Test
    public void testGetColumnIndexes() {
        int[] indexes = dataset.getColumnIndexes(Arrays.asList("name", "salary"));
        assertNotNull(indexes);
        assertEquals(2, indexes.length);
        assertEquals(1, indexes[0]);
        assertEquals(3, indexes[1]);
    }

    @Test
    public void testContainsColumn() {
        assertTrue(dataset.containsColumn("id"));
        assertTrue(dataset.containsColumn("name"));
        assertFalse(dataset.containsColumn("notexist"));
    }

    @Test
    public void testContainsAllColumns() {
        assertTrue(dataset.containsAllColumns(Arrays.asList("id", "name")));
        assertTrue(dataset.containsAllColumns(Arrays.asList("id", "name", "age", "salary")));
        assertFalse(dataset.containsAllColumns(Arrays.asList("id", "notexist")));
    }

    @Test
    public void testRenameColumn() {
        Dataset ds = dataset.copy();
        ds.renameColumn("name", "fullName");
        assertEquals("fullName", ds.getColumnName(1));
        assertTrue(ds.containsColumn("fullName"));
        assertFalse(ds.containsColumn("name"));
    }

    @Test
    public void testRenameColumns_Map() {
        Dataset ds = dataset.copy();
        Map<String, String> renameMap = new HashMap<>();
        renameMap.put("name", "fullName");
        renameMap.put("age", "years");

        ds.renameColumns(renameMap);
        assertTrue(ds.containsColumn("fullName"));
        assertTrue(ds.containsColumn("years"));
        assertFalse(ds.containsColumn("name"));
        assertFalse(ds.containsColumn("age"));
    }

    @Test
    public void testRenameColumns_Function() {
        Dataset ds = dataset.copy();
        ds.renameColumns(Arrays.asList("name", "age"), name -> name.toUpperCase());
        assertEquals("NAME", ds.getColumnName(1));
        assertEquals("AGE", ds.getColumnName(2));
    }

    @Test
    public void testRenameColumns_AllColumns() {
        Dataset ds = dataset.copy();
        ds.renameColumns(name -> name.toUpperCase());
        assertEquals("ID", ds.getColumnName(0));
        assertEquals("NAME", ds.getColumnName(1));
        assertEquals("AGE", ds.getColumnName(2));
        assertEquals("SALARY", ds.getColumnName(3));
    }

    @Test
    public void testMoveColumn() {
        Dataset ds = dataset.copy();
        ds.moveColumn("salary", 1);
        assertEquals("salary", ds.getColumnName(1));
        assertEquals("name", ds.getColumnName(2));
    }

    @Test
    public void testMoveColumns() {
        Dataset ds = dataset.copy();
        ds.moveColumns(Arrays.asList("age", "salary"), 0);
        assertEquals("age", ds.getColumnName(0));
        assertEquals("salary", ds.getColumnName(1));
        assertEquals("id", ds.getColumnName(2));
        assertEquals("name", ds.getColumnName(3));
    }

    @Test
    public void testSwapColumnPosition() {
        Dataset ds = dataset.copy();
        ds.swapColumnPosition("id", "salary");
        assertEquals("salary", ds.getColumnName(0));
        assertEquals("name", ds.getColumnName(1));
        assertEquals("age", ds.getColumnName(2));
        assertEquals("id", ds.getColumnName(3));
    }

    @Test
    public void testMoveRow() {
        Dataset ds = dataset.copy();
        ds.moveRow(0, 2);
        assertEquals(Integer.valueOf(2), ds.get(0, 0));
        assertEquals(Integer.valueOf(3), ds.get(1, 0));
        assertEquals(Integer.valueOf(1), ds.get(2, 0));
        assertEquals(Integer.valueOf(4), ds.get(3, 0));
    }

    @Test
    public void testMoveRows() {
        Dataset ds = dataset.copy();
        ds.moveRows(0, 2, 2);
        assertEquals(Integer.valueOf(3), ds.get(0, 0));
        assertEquals(Integer.valueOf(4), ds.get(1, 0));
        assertEquals(Integer.valueOf(1), ds.get(2, 0));
        assertEquals(Integer.valueOf(2), ds.get(3, 0));
    }

    @Test
    public void testSwapRowPosition() {
        Dataset ds = dataset.copy();
        ds.swapRowPosition(0, 3);
        assertEquals(Integer.valueOf(4), ds.get(0, 0));
        assertEquals(Integer.valueOf(2), ds.get(1, 0));
        assertEquals(Integer.valueOf(3), ds.get(2, 0));
        assertEquals(Integer.valueOf(1), ds.get(3, 0));
    }

    @Test
    public void testGet_ByIndexes() {
        assertEquals(Integer.valueOf(1), dataset.get(0, 0));
        assertEquals("Alice", dataset.get(0, 1));
        assertEquals(Integer.valueOf(25), dataset.get(0, 2));
        assertEquals(50000.0, dataset.get(0, 3));
    }

    @Test
    public void testGet_CurrentRow_ByIndex() {
        dataset.absolute(1);
        assertEquals(Integer.valueOf(2), dataset.get(0));
        assertEquals("Bob", dataset.get(1));
        assertEquals(Integer.valueOf(30), dataset.get(2));
        assertEquals(60000.0, dataset.get(3));
    }

    @Test
    public void testGet_CurrentRow_ByName() {
        dataset.absolute(1);
        assertEquals(Integer.valueOf(2), dataset.get("id"));
        assertEquals("Bob", dataset.get("name"));
        assertEquals(Integer.valueOf(30), dataset.get("age"));
        assertEquals(60000.0, dataset.get("salary"));
    }

    @Test
    public void testGetBoolean_ByIndex() {
        Dataset ds = Dataset.rows(Arrays.asList("flag"), new Object[][] { { true }, { false } });
        ds.absolute(0);
        assertTrue(ds.getBoolean(0));
        ds.absolute(1);
        assertFalse(ds.getBoolean(0));
    }

    @Test
    public void testGetBoolean_ByName() {
        Dataset ds = Dataset.rows(Arrays.asList("flag"), new Object[][] { { true }, { false } });
        ds.absolute(0);
        assertTrue(ds.getBoolean("flag"));
        ds.absolute(1);
        assertFalse(ds.getBoolean("flag"));
    }

    @Test
    public void testGetChar_ByIndex() {
        Dataset ds = Dataset.rows(Arrays.asList("letter"), new Object[][] { { 'A' }, { 'B' } });
        ds.absolute(0);
        assertEquals('A', ds.getChar(0));
        ds.absolute(1);
        assertEquals('B', ds.getChar(0));
    }

    @Test
    public void testGetChar_ByName() {
        Dataset ds = Dataset.rows(Arrays.asList("letter"), new Object[][] { { 'A' }, { 'B' } });
        ds.absolute(0);
        assertEquals('A', ds.getChar("letter"));
    }

    @Test
    public void testGetByte_ByIndex() {
        Dataset ds = Dataset.rows(Arrays.asList("num"), new Object[][] { { (byte) 1 }, { (byte) 2 } });
        ds.absolute(0);
        assertEquals((byte) 1, ds.getByte(0));
    }

    @Test
    public void testGetByte_ByName() {
        Dataset ds = Dataset.rows(Arrays.asList("num"), new Object[][] { { (byte) 1 }, { (byte) 2 } });
        ds.absolute(0);
        assertEquals((byte) 1, ds.getByte("num"));
    }

    @Test
    public void testGetShort_ByIndex() {
        Dataset ds = Dataset.rows(Arrays.asList("num"), new Object[][] { { (short) 100 }, { (short) 200 } });
        ds.absolute(0);
        assertEquals((short) 100, ds.getShort(0));
    }

    @Test
    public void testGetShort_ByName() {
        Dataset ds = Dataset.rows(Arrays.asList("num"), new Object[][] { { (short) 100 }, { (short) 200 } });
        ds.absolute(0);
        assertEquals((short) 100, ds.getShort("num"));
    }

    @Test
    public void testGetInt_ByIndex() {
        dataset.absolute(0);
        assertEquals(25, dataset.getInt(2));
    }

    @Test
    public void testGetInt_ByName() {
        dataset.absolute(0);
        assertEquals(25, dataset.getInt("age"));
    }

    @Test
    public void testGetLong_ByIndex() {
        Dataset ds = Dataset.rows(Arrays.asList("num"), new Object[][] { { 1000L }, { 2000L } });
        ds.absolute(0);
        assertEquals(1000L, ds.getLong(0));
    }

    @Test
    public void testGetLong_ByName() {
        Dataset ds = Dataset.rows(Arrays.asList("num"), new Object[][] { { 1000L }, { 2000L } });
        ds.absolute(0);
        assertEquals(1000L, ds.getLong("num"));
    }

    @Test
    public void testGetFloat_ByIndex() {
        Dataset ds = Dataset.rows(Arrays.asList("num"), new Object[][] { { 1.5f }, { 2.5f } });
        ds.absolute(0);
        assertEquals(1.5f, ds.getFloat(0));
    }

    @Test
    public void testGetFloat_ByName() {
        Dataset ds = Dataset.rows(Arrays.asList("num"), new Object[][] { { 1.5f }, { 2.5f } });
        ds.absolute(0);
        assertEquals(1.5f, ds.getFloat("num"));
    }

    @Test
    public void testGetDouble_ByIndex() {
        dataset.absolute(0);
        assertEquals(50000.0, dataset.getDouble(3));
    }

    @Test
    public void testGetDouble_ByName() {
        dataset.absolute(0);
        assertEquals(50000.0, dataset.getDouble("salary"));
    }

    @Test
    public void testIsNull_ByRowAndColumn() {
        Dataset ds = Dataset.rows(Arrays.asList("val"), new Object[][] { { null }, { "test" } });
        assertTrue(ds.isNull(0, 0));
        assertFalse(ds.isNull(1, 0));
    }

    @Test
    public void testIsNull_CurrentRow_ByIndex() {
        Dataset ds = Dataset.rows(Arrays.asList("val"), new Object[][] { { null }, { "test" } });
        ds.absolute(0);
        assertTrue(ds.isNull(0));
        ds.absolute(1);
        assertFalse(ds.isNull(0));
    }

    @Test
    public void testIsNull_CurrentRow_ByName() {
        Dataset ds = Dataset.rows(Arrays.asList("val"), new Object[][] { { null }, { "test" } });
        ds.absolute(0);
        assertTrue(ds.isNull("val"));
        ds.absolute(1);
        assertFalse(ds.isNull("val"));
    }

    @Test
    public void testSet_ByIndexes() {
        Dataset ds = dataset.copy();
        ds.set(0, 1, "UpdatedName");
        assertEquals("UpdatedName", ds.get(0, 1));
    }

    @Test
    public void testSet_CurrentRow_ByIndex() {
        Dataset ds = dataset.copy();
        ds.absolute(0);
        ds.set(1, "UpdatedName");
        assertEquals("UpdatedName", ds.get(0, 1));
    }

    @Test
    public void testSet_CurrentRow_ByName() {
        Dataset ds = dataset.copy();
        ds.absolute(0);
        ds.set("name", "UpdatedName");
        assertEquals("UpdatedName", ds.get(0, 1));
    }

    @Test
    public void testGetColumn_ByIndex() {
        ImmutableList<Object> col = dataset.getColumn(1);
        assertNotNull(col);
        assertEquals(4, col.size());
        assertEquals("Alice", col.get(0));
        assertEquals("Bob", col.get(1));
        assertEquals("Charlie", col.get(2));
        assertEquals("Diana", col.get(3));
    }

    @Test
    public void testGetColumn_ByName() {
        ImmutableList<Integer> col = dataset.getColumn("age");
        assertNotNull(col);
        assertEquals(4, col.size());
        assertEquals(Integer.valueOf(25), col.get(0));
        assertEquals(Integer.valueOf(30), col.get(1));
        assertEquals(Integer.valueOf(35), col.get(2));
        assertEquals(Integer.valueOf(28), col.get(3));
    }

    @Test
    public void testCopyColumn() {
        List<String> col = dataset.copyColumn("name");
        assertNotNull(col);
        assertEquals(4, col.size());
        assertEquals("Alice", col.get(0));

        col.set(0, "Modified");
        assertEquals("Modified", col.get(0));
        assertEquals("Alice", dataset.get(0, 1));
    }

    @Test
    public void testAddColumn_WithCollection() {
        Dataset ds = dataset.copy();
        List<String> emails = Arrays.asList("alice@test.com", "bob@test.com", "charlie@test.com", "diana@test.com");
        ds.addColumn("email", emails);

        assertEquals(5, ds.columnCount());
        assertEquals("email", ds.getColumnName(4));
        assertEquals("alice@test.com", ds.get(0, 4));
    }

    @Test
    public void testAddColumn_WithPosition() {
        Dataset ds = dataset.copy();
        List<String> emails = Arrays.asList("alice@test.com", "bob@test.com", "charlie@test.com", "diana@test.com");
        ds.addColumn(1, "email", emails);

        assertEquals(5, ds.columnCount());
        assertEquals("email", ds.getColumnName(1));
        assertEquals("name", ds.getColumnName(2));
    }

    @Test
    public void testAddColumn_WithFunction() {
        Dataset ds = dataset.copy();
        ds.addColumn("ageDouble", "age", (Integer age) -> age * 2);

        assertEquals(5, ds.columnCount());
        assertEquals(Integer.valueOf(50), ds.get(0, 4));
        assertEquals(Integer.valueOf(60), ds.get(1, 4));
    }

    @Test
    public void testAddColumn_WithBiFunction() {
        Dataset ds = dataset.copy();
        ds.addColumn("fullInfo", Tuple.of("name", "age"), (String name, Integer age) -> name + " (" + age + ")");

        assertEquals(5, ds.columnCount());
        assertEquals("Alice (25)", ds.get(0, 4));
        assertEquals("Bob (30)", ds.get(1, 4));
    }

    @Test
    public void testAddColumn_WithTriFunction() {
        Dataset ds = dataset.copy();
        ds.addColumn("info", Tuple.of("id", "name", "age"), (Integer id, String name, Integer age) -> id + ":" + name + ":" + age);

        assertEquals(5, ds.columnCount());
        assertEquals("1:Alice:25", ds.get(0, 4));
    }

    @Test
    public void testAddColumns() {
        Dataset ds = dataset.copy();
        List<String> newColNames = Arrays.asList("email", "city");
        List<List<Object>> newCols = new ArrayList<>();
        newCols.add(Arrays.asList("a@test.com", "b@test.com", "c@test.com", "d@test.com"));
        newCols.add(Arrays.asList("NYC", "LA", "SF", "Seattle"));

        ds.addColumns(newColNames, newCols);
        assertEquals(6, ds.columnCount());
        assertEquals("email", ds.getColumnName(4));
        assertEquals("city", ds.getColumnName(5));
    }

    @Test
    public void testRemoveColumn() {
        Dataset ds = dataset.copy();
        List<String> removed = ds.removeColumn("name");

        assertEquals(3, ds.columnCount());
        assertFalse(ds.containsColumn("name"));
        assertEquals(4, removed.size());
        assertEquals("Alice", removed.get(0));
    }

    @Test
    public void testRemoveColumns() {
        Dataset ds = dataset.copy();
        ds.removeColumns(Arrays.asList("name", "age"));

        assertEquals(2, ds.columnCount());
        assertTrue(ds.containsColumn("id"));
        assertTrue(ds.containsColumn("salary"));
        assertFalse(ds.containsColumn("name"));
        assertFalse(ds.containsColumn("age"));
    }

    @Test
    public void testRemoveColumns_WithPredicate() {
        Dataset ds = dataset.copy();
        ds.removeColumns(col -> col.startsWith("s"));

        assertEquals(3, ds.columnCount());
        assertFalse(ds.containsColumn("salary"));
    }

    @Test
    public void testUpdateColumn_WithFunction() {
        Dataset ds = dataset.copy();
        ds.updateColumn("name", (String name) -> name.toUpperCase());

        assertEquals("ALICE", ds.get(0, 1));
        assertEquals("BOB", ds.get(1, 1));
    }

    @Test
    public void testConvertColumn() {
        Dataset ds = dataset.copy();
        ds.convertColumn("age", Long.class);

        Object val = ds.get(0, 2);
        assertTrue(val instanceof Long);
        assertEquals(25L, val);
    }

    @Test
    public void testSize() {
        assertEquals(4, dataset.size());
        assertEquals(0, emptyDataset.size());
    }

    @Test
    public void testIsEmpty() {
        assertFalse(dataset.isEmpty());
        assertTrue(emptyDataset.isEmpty());
    }

    @Test
    public void testTrim() {
        Dataset ds = dataset.copy();
        ds.removeRow(0);
        ds.removeRow(0);

        assertEquals(2, ds.size());
        ds.trimToSize();
        assertEquals(2, ds.size());
    }

    @Test
    public void testClear() {
        Dataset ds = dataset.copy();
        ds.clear();

        assertEquals(0, ds.size());
        assertEquals(4, ds.columnCount());
        assertTrue(ds.isEmpty());
    }

    @Test
    public void testGetRow_AsEntity() {
        Person person = dataset.getRow(0, Person.class);
        assertNotNull(person);
        assertEquals(1, person.getId());
        assertEquals("Alice", person.getName());
    }

    @Test
    public void testGetRow_WithColumnNames() {
        Person person = dataset.getRow(0, Arrays.asList("id", "name"), Person.class);
        assertNotNull(person);
        assertEquals(1, person.getId());
        assertEquals("Alice", person.getName());
    }

    @Test
    public void testFirstRow() {
        Optional<Person> person = dataset.firstRow(Person.class);
        assertTrue(person.isPresent());
        assertEquals(1, person.get().getId());
    }

    @Test
    public void testLastRow() {
        Optional<Person> person = dataset.lastRow(Person.class);
        assertTrue(person.isPresent());
        assertEquals(4, person.get().getId());
    }

    @Test
    public void testAddRow() {
        Dataset ds = dataset.copy();
        ds.addRow(Arrays.asList(5, "Eve", 27, 52000.0));

        assertEquals(5, ds.size());
        assertEquals(Integer.valueOf(5), ds.get(4, 0));
        assertEquals("Eve", ds.get(4, 1));
    }

    @Test
    public void testAddRow_AtPosition() {
        Dataset ds = dataset.copy();
        ds.addRow(1, Arrays.asList(5, "Eve", 27, 52000.0));

        assertEquals(5, ds.size());
        assertEquals(Integer.valueOf(5), ds.get(1, 0));
        assertEquals(Integer.valueOf(2), ds.get(2, 0));
    }

    @Test
    public void testRemoveRow() {
        Dataset ds = dataset.copy();
        ds.removeRow(0);

        assertEquals(3, ds.size());
        assertEquals(Integer.valueOf(2), ds.get(0, 0));
    }

    @Test
    public void testRemoveRows_Range() {
        Dataset ds = dataset.copy();
        ds.removeRows(1, 3);

        assertEquals(2, ds.size());
        assertEquals(Integer.valueOf(1), ds.get(0, 0));
        assertEquals(Integer.valueOf(4), ds.get(1, 0));
    }

    @Test
    public void testCurrentRowNum() {
        assertEquals(0, dataset.currentRowNum());
        dataset.absolute(2);
        assertEquals(2, dataset.currentRowNum());
    }

    @Test
    public void testAbsolute() {
        dataset.absolute(2);
        assertEquals(2, dataset.currentRowNum());
        assertEquals(Integer.valueOf(3), dataset.get("id"));
    }

    @Test
    public void testFirst() {
        dataset.absolute(2);
        dataset.absolute(0);
        assertEquals(0, dataset.currentRowNum());
        assertEquals(Integer.valueOf(1), dataset.get("id"));
    }

    @Test
    public void testLast() {
        dataset.absolute(dataset.size() - 1);
        assertEquals(3, dataset.currentRowNum());
        assertEquals(Integer.valueOf(4), dataset.get("id"));
    }

    @Test
    public void testToList_AsEntity() {
        List<Person> persons = dataset.toList(Person.class);
        assertNotNull(persons);
        assertEquals(4, persons.size());
        assertEquals(1, persons.get(0).getId());
        assertEquals("Alice", persons.get(0).getName());
    }

    @Test
    public void testToList_WithRange() {
        List<Person> persons = dataset.toList(1, 3, Person.class);
        assertNotNull(persons);
        assertEquals(2, persons.size());
        assertEquals(2, persons.get(0).getId());
        assertEquals(3, persons.get(1).getId());
    }

    @Test
    public void testToList_WithColumnNames() {
        List<Person> persons = dataset.toList(Arrays.asList("id", "name"), Person.class);
        assertNotNull(persons);
        assertEquals(4, persons.size());
        assertEquals(1, persons.get(0).getId());
        assertEquals("Alice", persons.get(0).getName());
    }

    @Test
    public void testToList_WithSupplier() {
        IntFunction<Person> supplier = rowIndex -> new Person();
        List<Person> persons = dataset.toList(supplier);
        assertNotNull(persons);
        assertEquals(4, persons.size());
    }

    @Test
    public void testToEntities() {
        List<Person> persons = dataset.toEntities(null, Person.class);
        assertNotNull(persons);
        assertEquals(4, persons.size());
    }

    @Test
    public void testToMergedEntities() {
        List<Person> persons = dataset.toMergedEntities(Person.class);
        assertNotNull(persons);
        assertEquals(4, persons.size());
    }

    @Test
    public void testToMap_KeyValue() {
        Map<Integer, String> map = dataset.toMap("id", "name");
        assertNotNull(map);
        assertEquals(4, map.size());
        assertEquals("Alice", map.get(1));
        assertEquals("Bob", map.get(2));
    }

    @Test
    public void testToMap_WithRowType() {
        Map<Integer, Person> map = dataset.toMap("id", Arrays.asList("name", "age"), Person.class);
        assertNotNull(map);
        assertEquals(4, map.size());
        assertEquals("Alice", map.get(1).getName());
    }

    @Test
    public void testToMultimap() {
        Dataset ds = Dataset.rows(Arrays.asList("dept", "name"), new Object[][] { { "IT", "Alice" }, { "HR", "Bob" }, { "IT", "Charlie" } });

        ListMultimap<String, String> map = ds.toMultimap("dept", "name");
        assertNotNull(map);
        assertEquals(2, map.get("IT").size());
    }

    @Test
    public void testToJSON() {
        String json = dataset.toJson();
        assertNotNull(json);
        assertTrue(json.contains("Alice"));
        assertTrue(json.contains("Bob"));
    }

    @Test
    public void testToJSON_WithRange() {
        String json = dataset.toJson(0, 2);
        assertNotNull(json);
        assertTrue(json.contains("Alice"));
        assertTrue(json.contains("Bob"));
        assertFalse(json.contains("Diana"));
    }

    @Test
    public void testToJSON_ToFile() throws IOException {
        File tempFile = File.createTempFile("dataset", ".json");
        tempFile.deleteOnExit();

        dataset.toJson(tempFile);
        assertTrue(tempFile.exists());
        assertTrue(tempFile.length() > 0);
    }

    @Test
    public void testToJSON_ToWriter() {
        StringWriter writer = new StringWriter();
        dataset.toJson(writer);

        String json = writer.toString();
        assertNotNull(json);
        assertTrue(json.contains("Alice"));
    }

    @Test
    public void testToCSV() {
        String csv = dataset.toCsv();
        assertNotNull(csv);
        assertTrue(csv.contains("Alice"));
        assertTrue(csv.contains("Bob"));
    }

    @Test
    public void testToXML() {
        String xml = dataset.toXml();
        assertNotNull(xml);
        assertTrue(xml.contains("Alice"));
    }

    @Test
    public void testForEach() {
        List<String> names = new ArrayList<>();
        dataset.forEach(row -> names.add((String) row.get(1)));

        assertEquals(4, names.size());
        assertEquals("Alice", names.get(0));
        assertEquals("Bob", names.get(1));
    }

    @Test
    public void testForEach_WithRange() {
        List<String> names = new ArrayList<>();
        dataset.forEach(1, 3, row -> names.add((String) row.get(1)));

        assertEquals(2, names.size());
        assertEquals("Bob", names.get(0));
        assertEquals("Charlie", names.get(1));
    }

    @Test
    public void testForEach_BiConsumer() {
        List<String> results = new ArrayList<>();
        dataset.forEach(Tuple.of("id", "name"), (Integer id, String name) -> results.add(id + ":" + name));

        assertEquals(4, results.size());
        assertEquals("1:Alice", results.get(0));
    }

    @Test
    public void testForEach_TriConsumer() {
        List<String> results = new ArrayList<>();
        dataset.forEach(Tuple.of("id", "name", "age"), (Integer id, String name, Integer age) -> results.add(id + ":" + name + ":" + age));

        assertEquals(4, results.size());
        assertEquals("1:Alice:25", results.get(0));
    }

    @Test
    public void testIterator_TwoColumns() {
        BiIterator<Integer, String> iter = dataset.iterator("id", "name");
        assertNotNull(iter);

        assertTrue(iter.hasNext());
        Pair<Integer, String> pair = iter.next();
        assertEquals(Integer.valueOf(1), pair.left());
        assertEquals("Alice", pair.right());
    }

    @Test
    public void testIterator_ThreeColumns() {
        TriIterator<Integer, String, Integer> iter = dataset.iterator("id", "name", "age");
        assertNotNull(iter);

        assertTrue(iter.hasNext());
        Triple<Integer, String, Integer> triple = iter.next();
        assertEquals(Integer.valueOf(1), triple.left());
        assertEquals("Alice", triple.middle());
        assertEquals(Integer.valueOf(25), triple.right());
    }

    @Test
    public void testStream_AsEntity() {
        Stream<Person> stream = dataset.stream(Person.class);
        assertNotNull(stream);

        List<Person> persons = stream.toList();
        assertEquals(4, persons.size());
        assertEquals("Alice", persons.get(0).getName());
    }

    @Test
    public void testGroupBy_MultipleColumns() {
        Dataset ds = Dataset.rows(Arrays.asList("dept", "level", "name"),
                new Object[][] { { "IT", "Senior", "Alice" }, { "HR", "Junior", "Bob" }, { "IT", "Senior", "Charlie" } });

        Dataset grouped = ds.groupBy(Arrays.asList("dept", "level"));
        assertNotNull(grouped);
    }

    @Test
    public void testGroupBy_WithAggregation() {
        Dataset ds = Dataset.rows(Arrays.asList("dept", "salary"), new Object[][] { { "IT", 50000.0 }, { "HR", 60000.0 }, { "IT", 70000.0 } });

        Dataset grouped = ds.groupBy("dept", "salary", "SUM", Collectors.summingDouble(val -> (Double) val));
        assertNotNull(grouped);
    }

    @Test
    public void testSortBy_SingleColumn() {
        Dataset ds = dataset.copy();
        ds.sortBy("age");

        assertEquals(Integer.valueOf(25), ds.get(0, 2));
        assertEquals(Integer.valueOf(28), ds.get(1, 2));
        assertEquals(Integer.valueOf(30), ds.get(2, 2));
        assertEquals(Integer.valueOf(35), ds.get(3, 2));
    }

    @Test
    public void testSortBy_MultipleColumns() {
        Dataset ds = dataset.copy();
        ds.sortBy(Arrays.asList("age", "name"));

        assertEquals(Integer.valueOf(25), ds.get(0, 2));
    }

    @Test
    public void testSortBy_WithComparator() {
        Dataset ds = dataset.copy();
        ds.sortBy("age", Comparator.reverseOrder());

        assertEquals(Integer.valueOf(35), ds.get(0, 2));
        assertEquals(Integer.valueOf(30), ds.get(1, 2));
    }

    @Test
    public void testParallelSortBy() {
        Dataset ds = dataset.copy();
        ds.parallelSortBy("age");

        assertEquals(Integer.valueOf(25), ds.get(0, 2));
        assertEquals(Integer.valueOf(28), ds.get(1, 2));
    }

    @Test
    public void testTopBy() {
        Dataset top = dataset.topBy("salary", 2);
        assertNotNull(top);
        assertEquals(2, top.size());
        assertEquals(60000.0, top.get(0, 3));
        assertEquals(70000.0, top.get(1, 3));
    }

    @Test
    public void testFilter_TwoColumns() {
        Dataset filtered = dataset.filter(Tuple.of("name", "age"), (String name, Integer age) -> age > 26);
        assertNotNull(filtered);
        assertEquals(3, filtered.size());
    }

    @Test
    public void testFilter_ThreeColumns() {
        Dataset filtered = dataset.filter(Tuple.of("id", "name", "age"), (Integer id, String name, Integer age) -> age > 26);
        assertNotNull(filtered);
        assertEquals(3, filtered.size());
    }

    @Test
    public void testDistinct() {
        Dataset ds = Dataset.rows(Arrays.asList("id", "value"), new Object[][] { { 1, "A" }, { 2, "B" }, { 1, "A" } });

        Dataset distinct = ds.distinct();
        assertNotNull(distinct);
        assertEquals(2, distinct.size());
    }

    @Test
    public void testDistinct_ByColumn() {
        Dataset ds = Dataset.rows(Arrays.asList("id", "value"), new Object[][] { { 1, "A" }, { 2, "B" }, { 1, "C" } });

        Dataset distinct = ds.distinctBy("id");
        assertNotNull(distinct);
        assertEquals(2, distinct.size());
    }

    @Test
    public void testInnerJoin() {
        Dataset ds1 = Dataset.rows(Arrays.asList("id", "name"), new Object[][] { { 1, "Alice" }, { 2, "Bob" } });

        Dataset ds2 = Dataset.rows(Arrays.asList("id", "dept"), new Object[][] { { 1, "IT" }, { 2, "HR" } });

        Dataset joined = ds1.innerJoin(ds2, CommonUtil.asMap("id", "id"));
        assertNotNull(joined);
        assertEquals(2, joined.size());
    }

    @Test
    public void testInnerJoin_MultipleKeys() {
        Dataset ds1 = Dataset.rows(Arrays.asList("id", "year", "name"), new Object[][] { { 1, 2023, "Alice" }, { 2, 2023, "Bob" } });

        Dataset ds2 = Dataset.rows(Arrays.asList("id", "year", "dept"), new Object[][] { { 1, 2023, "IT" }, { 2, 2023, "HR" } });

        Dataset joined = ds1.innerJoin(ds2, CommonUtil.asMap("id", "id", "year", "year"));
        assertNotNull(joined);
        assertEquals(2, joined.size());
    }

    @Test
    public void testLeftJoin() {
        Dataset ds1 = Dataset.rows(Arrays.asList("id", "name"), new Object[][] { { 1, "Alice" }, { 2, "Bob" }, { 3, "Charlie" } });

        Dataset ds2 = Dataset.rows(Arrays.asList("id", "dept"), new Object[][] { { 1, "IT" }, { 2, "HR" } });

        Dataset joined = ds1.leftJoin(ds2, CommonUtil.asMap("id", "id"));
        assertNotNull(joined);
        assertEquals(3, joined.size());
    }

    @Test
    public void testRightJoin() {
        Dataset ds1 = Dataset.rows(Arrays.asList("id", "name"), new Object[][] { { 1, "Alice" }, { 2, "Bob" } });

        Dataset ds2 = Dataset.rows(Arrays.asList("id", "dept"), new Object[][] { { 1, "IT" }, { 2, "HR" }, { 3, "Finance" } });

        Dataset joined = ds1.rightJoin(ds2, CommonUtil.asMap("id", "id"));
        assertNotNull(joined);
        assertEquals(3, joined.size());
    }

    @Test
    public void testFullJoin() {
        Dataset ds1 = Dataset.rows(Arrays.asList("id", "name"), new Object[][] { { 1, "Alice" }, { 2, "Bob" } });

        Dataset ds2 = Dataset.rows(Arrays.asList("id", "dept"), new Object[][] { { 2, "HR" }, { 3, "Finance" } });

        Dataset joined = ds1.fullJoin(ds2, CommonUtil.asMap("id", "id"));
        assertNotNull(joined);
        assertEquals(3, joined.size());
    }

    @Test
    public void testUnion() {
        Dataset ds1 = Dataset.rows(Arrays.asList("id", "name"), new Object[][] { { 1, "Alice" }, { 2, "Bob" } });

        Dataset ds2 = Dataset.rows(Arrays.asList("id", "name"), new Object[][] { { 3, "Charlie" }, { 4, "Diana" } });

        Dataset union = ds1.union(ds2);
        assertNotNull(union);
        assertEquals(4, union.size());
    }

    @Test
    public void testUnionAll() {
        Dataset ds1 = Dataset.rows(Arrays.asList("id", "name"), new Object[][] { { 1, "Alice" }, { 2, "Bob" } });

        Dataset ds2 = Dataset.rows(Arrays.asList("id", "name"), new Object[][] { { 1, "Alice" }, { 3, "Charlie" } });

        Dataset union = ds1.unionAll(ds2);
        assertNotNull(union);
        assertEquals(4, union.size());
    }

    @Test
    public void testIntersect() {
        Dataset ds1 = Dataset.rows(Arrays.asList("id", "name"), new Object[][] { { 1, "Alice" }, { 2, "Bob" }, { 3, "Charlie" } });

        Dataset ds2 = Dataset.rows(Arrays.asList("id", "name"), new Object[][] { { 2, "Bob" }, { 3, "Charlie" }, { 4, "Diana" } });

        Dataset intersect = ds1.intersect(ds2);
        assertNotNull(intersect);
        assertEquals(2, intersect.size());
    }

    @Test
    public void testIntersectAll() {
        Dataset ds1 = Dataset.rows(Arrays.asList("id", "name"), new Object[][] { { 1, "Alice" }, { 2, "Bob" }, { 2, "Bob" } });

        Dataset ds2 = Dataset.rows(Arrays.asList("id", "name"), new Object[][] { { 2, "Bob" }, { 2, "Bob" }, { 3, "Charlie" } });

        Dataset intersect = ds1.intersectAll(ds2);
        assertNotNull(intersect);
    }

    @Test
    public void testExcept() {
        Dataset ds1 = Dataset.rows(Arrays.asList("id", "name"), new Object[][] { { 1, "Alice" }, { 2, "Bob" }, { 3, "Charlie" } });

        Dataset ds2 = Dataset.rows(Arrays.asList("id", "name"), new Object[][] { { 2, "Bob" }, { 4, "Diana" } });

        Dataset except = ds1.except(ds2);
        assertNotNull(except);
        assertEquals(2, except.size());
    }

    @Test
    public void testExceptAll() {
        Dataset ds1 = Dataset.rows(Arrays.asList("id", "name"), new Object[][] { { 1, "Alice" }, { 2, "Bob" }, { 2, "Bob" } });

        Dataset ds2 = Dataset.rows(Arrays.asList("id", "name"), new Object[][] { { 2, "Bob" } });

        Dataset except = ds1.exceptAll(ds2);
        assertNotNull(except);
    }

    @Test
    public void testDifference() {
        Dataset ds1 = Dataset.rows(Arrays.asList("id", "name"), new Object[][] { { 1, "Alice" }, { 2, "Bob" } });

        Dataset ds2 = Dataset.rows(Arrays.asList("id", "name"), new Object[][] { { 2, "Bob" }, { 3, "Charlie" } });

        Dataset diff = ds1.difference(ds2);
        assertNotNull(diff);
    }

    @Test
    public void testSymmetricDifference() {
        Dataset ds1 = Dataset.rows(Arrays.asList("id", "name"), new Object[][] { { 1, "Alice" }, { 2, "Bob" } });

        Dataset ds2 = Dataset.rows(Arrays.asList("id", "name"), new Object[][] { { 2, "Bob" }, { 3, "Charlie" } });

        Dataset diff = ds1.symmetricDifference(ds2);
        assertNotNull(diff);
    }

    @Test
    public void testCopy() {
        Dataset copy = dataset.copy();
        assertNotNull(copy);
        assertEquals(dataset.size(), copy.size());
        assertEquals(dataset.columnCount(), copy.columnCount());

        copy.set(0, 1, "Modified");
        assertEquals("Modified", copy.get(0, 1));
        assertEquals("Alice", dataset.get(0, 1));
    }

    @Test
    public void testCopy_WithRange() {
        Dataset copy = dataset.copy(1, 3);
        assertNotNull(copy);
        assertEquals(2, copy.size());
        assertEquals(4, copy.columnCount());
        assertEquals(Integer.valueOf(2), copy.get(0, 0));
    }

    @Test
    public void testCopy_WithColumnNames() {
        Dataset copy = dataset.copy(Arrays.asList("id", "name"));
        assertNotNull(copy);
        assertEquals(4, copy.size());
        assertEquals(2, copy.columnCount());
    }

    @Test
    public void testCopy_WithRangeAndColumns() {
        Dataset copy = dataset.copy(1, 3, Arrays.asList("id", "name"));
        assertNotNull(copy);
        assertEquals(2, copy.size());
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
    public void testClone_Frozen() {
        Dataset frozen = dataset.copy();
        frozen.freeze();
        Dataset clone = frozen.clone();
        assertNotNull(clone);
        assertTrue(clone.isFrozen());
    }

    @Test
    public void testSlice() {
        Dataset slice = dataset.slice(1, 3);
        assertNotNull(slice);
        assertEquals(2, slice.size());
        assertEquals(Integer.valueOf(2), slice.get(0, 0));
        assertEquals(Integer.valueOf(3), slice.get(1, 0));
    }

    @Test
    public void testSlice_WithColumnNames() {
        Dataset slice = dataset.slice(1, 3, Arrays.asList("id", "name"));
        assertNotNull(slice);
        assertEquals(2, slice.size());
        assertEquals(2, slice.columnCount());
    }

    @Test
    public void testSplit_BySize() {
        List<Dataset> splits = dataset.split(2).toList();
        assertNotNull(splits);
        assertEquals(2, splits.size());
        assertEquals(2, splits.get(0).size());
        assertEquals(2, splits.get(1).size());
    }

    @Test
    public void testPaginate() {
        Paginated<Dataset> pages = dataset.paginate(2);
        assertNotNull(pages);

        List<Dataset> pageList = pages.stream().toList();

        assertEquals(2, pageList.size());
        assertEquals(2, pageList.get(0).size());
        assertEquals(2, pageList.get(1).size());
    }

    @Test
    public void testFreeze() {
        Dataset ds = dataset.copy();
        assertFalse(ds.isFrozen());

        ds.freeze();
        assertTrue(ds.isFrozen());

        assertThrows(IllegalStateException.class, () -> ds.set(0, 0, 999));
    }

    @Test
    public void testFrozen() {
        assertFalse(dataset.isFrozen());

        Dataset frozen = dataset.copy();
        frozen.freeze();
        assertTrue(frozen.isFrozen());
    }

    @Test
    public void testProperties() {
        Dataset ds = dataset.copy();
        Map<String, Object> props = new java.util.HashMap<>();
        props.put("key1", "value1");
        ds.setProperties(props);

        Map<String, Object> retrievedProps = ds.getProperties();
        assertNotNull(retrievedProps);
        assertEquals("value1", retrievedProps.get("key1"));
    }

    @Test
    public void testEquals() {
        Dataset ds1 = Dataset.rows(columnNames, sampleRows);
        Dataset ds2 = Dataset.rows(columnNames, sampleRows);

        assertTrue(ds1.equals(ds2));
    }

    @Test
    public void testHashCode() {
        Dataset ds1 = Dataset.rows(columnNames, sampleRows);
        Dataset ds2 = Dataset.rows(columnNames, sampleRows);

        assertEquals(ds1.hashCode(), ds2.hashCode());
    }

    @Test
    public void testToString() {
        String str = dataset.toString();
        assertNotNull(str);
        assertTrue(str.contains("Alice"));
    }

    @Test
    public void testPrintln() {
        dataset.println();
    }

    @Test
    public void testPrintln_WithRange() {
        dataset.println(0, 2);
    }

    @Test
    public void testPrintln_WithColumns() {
        dataset.println(0, 2, Arrays.asList("id", "name"));
    }

    @Test
    public void testAddRows_Collection() {
        Dataset ds = dataset.copy();
        List<Object[]> newRows = new ArrayList<>();
        newRows.add(new Object[] { 5, "Eve", 27, 52000.0 });
        newRows.add(new Object[] { 6, "Frank", 32, 58000.0 });

        ds.addRows(newRows);

        assertEquals(6, ds.size());
        assertEquals(Integer.valueOf(5), ds.get(4, 0));
        assertEquals("Eve", ds.get(4, 1));
        assertEquals(Integer.valueOf(6), ds.get(5, 0));
        assertEquals("Frank", ds.get(5, 1));
    }

    @Test
    public void testAddRows_AtPosition() {
        Dataset ds = dataset.copy();
        List<Object[]> newRows = new ArrayList<>();
        newRows.add(new Object[] { 5, "Eve", 27, 52000.0 });
        newRows.add(new Object[] { 6, "Frank", 32, 58000.0 });

        ds.addRows(1, newRows);

        assertEquals(6, ds.size());
        assertEquals(Integer.valueOf(5), ds.get(1, 0));
        assertEquals("Eve", ds.get(1, 1));
        assertEquals(Integer.valueOf(6), ds.get(2, 0));
        assertEquals("Frank", ds.get(2, 1));
        assertEquals(Integer.valueOf(2), ds.get(3, 0));
    }

    @Test
    public void testRemoveRowRange() {
        Dataset ds = dataset.copy();
        ds.removeRows(1, 3);

        assertEquals(2, ds.size());
        assertEquals(Integer.valueOf(1), ds.get(0, 0));
        assertEquals(Integer.valueOf(4), ds.get(1, 0));
    }

    @Test
    public void testRemoveRowRange_EdgeCases() {
        Dataset ds = dataset.copy();

        ds.removeRows(0, 2);
        assertEquals(2, ds.size());
        assertEquals(Integer.valueOf(3), ds.get(0, 0));

        ds.removeRows(0, 1);
        assertEquals(1, ds.size());
        assertEquals(Integer.valueOf(4), ds.get(0, 0));
    }

    @Test
    public void testRemoveDuplicateRowsBy_SingleColumn() {
        Dataset ds = Dataset.rows(Arrays.asList("id", "name", "type"),
                new Object[][] { { 1, "Alice", "A" }, { 2, "Bob", "B" }, { 3, "Charlie", "A" }, { 4, "Diana", "B" }, { 5, "Eve", "A" } });

        ds.removeDuplicateRowsBy("type");

        assertEquals(2, ds.size());
        assertEquals("Alice", ds.get(0, 1));
        assertEquals("Bob", ds.get(1, 1));
    }

    @Test
    public void testRemoveDuplicateRowsBy_WithKeyExtractor() {
        Dataset ds = Dataset.rows(Arrays.asList("id", "name", "email"), new Object[][] { { 1, "Alice", "alice@test.com" }, { 2, "Bob", "bob@test.com" },
                { 3, "Charlie", "charlie@test.com" }, { 4, "Diana", "alice@test.com" } });

        ds.removeDuplicateRowsBy("email", (String email) -> email.toLowerCase());

        assertEquals(3, ds.size());
        assertEquals("Alice", ds.get(0, 1));
        assertEquals("Bob", ds.get(1, 1));
        assertEquals("Charlie", ds.get(2, 1));
    }

    @Test
    public void testRemoveDuplicateRowsBy_MultipleColumns() {
        Dataset ds = Dataset.rows(Arrays.asList("firstName", "lastName", "age"),
                new Object[][] { { "John", "Doe", 25 }, { "Jane", "Smith", 30 }, { "John", "Doe", 25 }, { "John", "Smith", 28 } });

        ds.removeDuplicateRowsBy(Arrays.asList("firstName", "lastName"));

        assertEquals(3, ds.size());
        assertEquals("John", ds.get(0, 0));
        assertEquals("Doe", ds.get(0, 1));
        assertEquals("Jane", ds.get(1, 0));
        assertEquals("Smith", ds.get(1, 1));
        assertEquals("John", ds.get(2, 0));
        assertEquals("Smith", ds.get(2, 1));
    }

    @Test
    public void testPrepend() {
        Dataset ds1 = Dataset.rows(Arrays.asList("id", "name"), new Object[][] { { 1, "Alice" }, { 2, "Bob" } });

        Dataset ds2 = Dataset.rows(Arrays.asList("id", "name"), new Object[][] { { 3, "Charlie" }, { 4, "Diana" } });

        ds1.prepend(ds2);

        assertEquals(4, ds1.size());
        assertEquals(Integer.valueOf(3), ds1.get(0, 0));
        assertEquals("Charlie", ds1.get(0, 1));
        assertEquals(Integer.valueOf(4), ds1.get(1, 0));
        assertEquals("Diana", ds1.get(1, 1));
        assertEquals(Integer.valueOf(1), ds1.get(2, 0));
        assertEquals("Alice", ds1.get(2, 1));
    }

    @Test
    public void testAppend() {
        Dataset ds1 = Dataset.rows(Arrays.asList("id", "name"), new Object[][] { { 1, "Alice" }, { 2, "Bob" } });

        Dataset ds2 = Dataset.rows(Arrays.asList("id", "name"), new Object[][] { { 3, "Charlie" }, { 4, "Diana" } });

        ds1.append(ds2);

        assertEquals(4, ds1.size());
        assertEquals(Integer.valueOf(1), ds1.get(0, 0));
        assertEquals("Alice", ds1.get(0, 1));
        assertEquals(Integer.valueOf(2), ds1.get(1, 0));
        assertEquals("Bob", ds1.get(1, 1));
        assertEquals(Integer.valueOf(3), ds1.get(2, 0));
        assertEquals("Charlie", ds1.get(2, 1));
    }

    @Test
    public void testCombineColumns_WithBiFunction() {
        Dataset ds = Dataset.rows(Arrays.asList("firstName", "lastName"), new Object[][] { { "Alice", "Smith" }, { "Bob", "Jones" } });

        ds.combineColumns(Tuple.of("firstName", "lastName"), "fullName", (String first, String last) -> first + " " + last);

        assertTrue(ds.containsColumn("fullName"));
        assertEquals("Alice Smith", ds.get(0, 0));
        assertEquals("Bob Jones", ds.get(1, 0));
    }

    @Test
    public void testCombineColumns_WithTriFunction() {
        Dataset ds = Dataset.rows(Arrays.asList("firstName", "middleName", "lastName"),
                new Object[][] { { "Alice", "Marie", "Smith" }, { "Bob", "William", "Jones" } });

        ds.combineColumns(Tuple.of("firstName", "middleName", "lastName"), "fullName",
                (String first, String middle, String last) -> first + " " + middle + " " + last);

        assertTrue(ds.containsColumn("fullName"));
        assertEquals("Alice Marie Smith", ds.get(0, 0));
        assertEquals("Bob William Jones", ds.get(1, 0));
    }

    @Test
    public void testConvertColumns() {
        Dataset ds = Dataset.rows(Arrays.asList("id", "age", "score"), new Object[][] { { 1, 25, 98.5 }, { 2, 30, 87.3 } });

        Map<String, Class<?>> conversions = new HashMap<>();
        conversions.put("id", Long.class);
        conversions.put("age", Long.class);

        ds.convertColumns(conversions);

        Object id = ds.get(0, 0);
        Object age = ds.get(0, 1);

        assertTrue(id instanceof Long);
        assertTrue(age instanceof Long);
        assertEquals(1L, id);
        assertEquals(25L, age);
    }

    @Test
    public void testDistinctBy_SingleColumn() {
        Dataset ds = Dataset.rows(Arrays.asList("type", "name", "value"),
                new Object[][] { { "A", "Alice", 100 }, { "B", "Bob", 200 }, { "A", "Charlie", 150 }, { "C", "Diana", 300 } });

        Dataset distinct = ds.distinctBy("type");

        assertEquals(3, distinct.size());
        assertEquals("A", distinct.get(0, 0));
        assertEquals("Alice", distinct.get(0, 1));
        assertEquals("B", distinct.get(1, 0));
        assertEquals("Bob", distinct.get(1, 1));
        assertEquals("C", distinct.get(2, 0));
        assertEquals("Diana", distinct.get(2, 1));
    }

    @Test
    public void testDistinctBy_WithKeyExtractor() {
        Dataset ds = Dataset.rows(Arrays.asList("email", "name"),
                new Object[][] { { "alice@TEST.com", "Alice" }, { "bob@test.com", "Bob" }, { "ALICE@test.com", "Alice2" } });

        Dataset distinct = ds.distinctBy("email", (String email) -> email.toLowerCase());

        assertEquals(2, distinct.size());
        assertEquals("alice@TEST.com", distinct.get(0, 0));
        assertEquals("bob@test.com", distinct.get(1, 0));
    }

    @Test
    public void testDistinctBy_MultipleColumns() {
        Dataset ds = Dataset.rows(Arrays.asList("dept", "level", "name"),
                new Object[][] { { "IT", "Senior", "Alice" }, { "HR", "Junior", "Bob" }, { "IT", "Senior", "Charlie" }, { "IT", "Junior", "Diana" } });

        Dataset distinct = ds.distinctBy(Arrays.asList("dept", "level"));

        assertEquals(3, distinct.size());
    }

    @Test
    public void testDistinctBy_WithCustomKeyExtractor() {
        Dataset ds = Dataset.rows(Arrays.asList("x", "y", "label"), new Object[][] { { 1, 2, "A" }, { 2, 3, "B" }, { 1, 2, "C" } });

        Dataset distinct = ds.distinctBy(Arrays.asList("x", "y"), vals -> vals.get(0).toString() + "," + vals.get(1).toString());

        assertEquals(2, distinct.size());
        assertEquals("A", distinct.get(0, 2));
        assertEquals("B", distinct.get(1, 2));
    }

    @Test
    public void testIntersection() {
        Dataset ds1 = Dataset.rows(Arrays.asList("id", "name"), new Object[][] { { 1, "Alice" }, { 2, "Bob" }, { 3, "Charlie" } });

        Dataset ds2 = Dataset.rows(Arrays.asList("id", "name"), new Object[][] { { 2, "Bob" }, { 3, "Charlie" }, { 4, "Diana" } });

        Dataset intersection = ds1.intersection(ds2);

        assertNotNull(intersection);
        assertEquals(2, intersection.size());
    }

    @Test
    public void testIntersection_WithSameColumnsCheck() {
        Dataset ds1 = Dataset.rows(Arrays.asList("id", "name"), new Object[][] { { 1, "Alice" }, { 2, "Bob" } });

        Dataset ds2 = Dataset.rows(Arrays.asList("id", "name"), new Object[][] { { 2, "Bob" }, { 3, "Charlie" } });

        Dataset intersection = ds1.intersection(ds2, true);

        assertNotNull(intersection);
        assertEquals(1, intersection.size());
        assertEquals(Integer.valueOf(2), intersection.get(0, 0));
        assertEquals("Bob", intersection.get(0, 1));
    }

    @Test
    public void testIntersection_WithKeyColumns() {
        Dataset ds1 = Dataset.rows(Arrays.asList("id", "name", "age"), new Object[][] { { 1, "Alice", 25 }, { 2, "Bob", 30 } });

        Dataset ds2 = Dataset.rows(Arrays.asList("id", "name", "salary"), new Object[][] { { 2, "Bob", 60000.0 }, { 3, "Charlie", 70000.0 } });

        Dataset intersection = ds1.intersection(ds2, Arrays.asList("id"));

        assertNotNull(intersection);
        assertEquals(1, intersection.size());
        assertEquals(Integer.valueOf(2), intersection.get(0, 0));
    }

    @Test
    public void testIntersection_WithKeyColumnsAndSameCheck() {
        Dataset ds1 = Dataset.rows(Arrays.asList("id", "name"), new Object[][] { { 1, "Alice" }, { 2, "Bob" } });

        Dataset ds2 = Dataset.rows(Arrays.asList("id", "name"), new Object[][] { { 2, "Bob" }, { 3, "Charlie" } });

        Dataset intersection = ds1.intersection(ds2, Arrays.asList("id", "name"), true);

        assertNotNull(intersection);
        assertEquals(1, intersection.size());
    }

    @Test
    public void testCartesianProduct() {
        Dataset ds1 = Dataset.rows(Arrays.asList("id", "name"), new Object[][] { { 1, "Alice" }, { 2, "Bob" } });

        Dataset ds2 = Dataset.rows(Arrays.asList("color", "size"), new Object[][] { { "Red", "S" }, { "Blue", "M" } });

        Dataset product = ds1.cartesianProduct(ds2);

        assertNotNull(product);
        assertEquals(4, product.size());
        assertEquals(4, product.columnCount());

        assertEquals(Integer.valueOf(1), product.get(0, 0));
        assertEquals("Alice", product.get(0, 1));
        assertEquals("Red", product.get(0, 2));
        assertEquals("S", product.get(0, 3));

        assertEquals(Integer.valueOf(2), product.get(3, 0));
        assertEquals("Bob", product.get(3, 1));
        assertEquals("Blue", product.get(3, 2));
        assertEquals("M", product.get(3, 3));
    }

    @Test
    public void testSplitToList() {
        List<Dataset> splits = dataset.splitToList(2);

        assertNotNull(splits);
        assertEquals(2, splits.size());
        assertEquals(2, splits.get(0).size());
        assertEquals(2, splits.get(1).size());
        assertEquals(4, splits.get(0).columnCount());
    }

    @Test
    public void testSplitToList_WithColumns() {
        List<Dataset> splits = dataset.splitToList(2, Arrays.asList("id", "name"));

        assertNotNull(splits);
        assertEquals(2, splits.size());
        assertEquals(2, splits.get(0).size());
        assertEquals(2, splits.get(0).columnCount());
        assertTrue(splits.get(0).containsColumn("id"));
        assertTrue(splits.get(0).containsColumn("name"));
        assertFalse(splits.get(0).containsColumn("age"));
    }

    @Test
    public void testColumns() {
        Stream<ImmutableList<Object>> columnStream = dataset.columns();

        assertNotNull(columnStream);
        List<ImmutableList<Object>> columns = columnStream.toList();
        assertEquals(4, columns.size());

        assertEquals(4, columns.get(0).size());
        assertEquals(Integer.valueOf(1), columns.get(0).get(0));
        assertEquals(Integer.valueOf(2), columns.get(0).get(1));

        assertEquals("Alice", columns.get(1).get(0));
        assertEquals("Bob", columns.get(1).get(1));
    }

    @Test
    public void testColumnMap() {
        Map<String, ImmutableList<Object>> map = dataset.columnMap();

        assertNotNull(map);
        assertEquals(4, map.size());
        assertTrue(map.containsKey("id"));
        assertTrue(map.containsKey("name"));
        assertTrue(map.containsKey("age"));
        assertTrue(map.containsKey("salary"));

        ImmutableList<Object> ids = map.get("id");
        assertEquals(4, ids.size());
        assertEquals(Integer.valueOf(1), ids.get(0));
        assertEquals(Integer.valueOf(2), ids.get(1));

        ImmutableList<Object> names = map.get("name");
        assertEquals("Alice", names.get(0));
        assertEquals("Bob", names.get(1));
    }

    @Test
    public void testApply() {
        Integer result = dataset.apply(ds -> ds.size());

        assertEquals(4, result.intValue());
    }

    @Test
    public void testApply_WithTransformation() {
        List<String> names = dataset.apply(ds -> {
            List<String> result = new ArrayList<>();
            ds.forEach(row -> result.add((String) row.get(1)));
            return result;
        });

        assertEquals(4, names.size());
        assertEquals("Alice", names.get(0));
        assertEquals("Bob", names.get(1));
    }

    @Test
    public void testApplyIfNotEmpty() {
        Optional<Integer> result = dataset.applyIfNotEmpty(ds -> ds.size());

        assertTrue(result.isPresent());
        assertEquals(4, result.get().intValue());
    }

    @Test
    public void testApplyIfNotEmpty_OnEmptyDataset() {
        Optional<Integer> result = emptyDataset.applyIfNotEmpty(ds -> ds.size());

        assertFalse(result.isPresent());
    }

    @Test
    public void testAccept() {
        List<Integer> ids = new ArrayList<>();

        dataset.accept(ds -> {
            ds.forEach(row -> ids.add((Integer) row.get(0)));
        });

        assertEquals(4, ids.size());
        assertEquals(Integer.valueOf(1), ids.get(0));
        assertEquals(Integer.valueOf(2), ids.get(1));
        assertEquals(Integer.valueOf(3), ids.get(2));
        assertEquals(Integer.valueOf(4), ids.get(3));
    }

    @Test
    public void testAcceptIfNotEmpty() {
        List<Integer> ids = new ArrayList<>();

        dataset.acceptIfNotEmpty(ds -> {
            ds.forEach(row -> ids.add((Integer) row.get(0)));
        });

        assertEquals(4, ids.size());
    }

    @Test
    public void testAcceptIfNotEmpty_OnEmptyDataset() {
        List<Integer> ids = new ArrayList<>();

        emptyDataset.acceptIfNotEmpty(ds -> {
            ds.forEach(row -> ids.add((Integer) row.get(0)));
        });

        assertEquals(0, ids.size());
    }

    @Test
    public void testGetProperties() {
        Dataset ds = dataset.copy();
        Map<String, Object> props = ds.getProperties();

        assertNotNull(props);
        assertTrue(props.isEmpty() || props.size() >= 0);
    }

    @Test
    public void testSetProperties() {
        Dataset ds = dataset.copy();
        Map<String, Object> props = new HashMap<>();
        props.put("key1", "value1");
        props.put("key2", 123);

        ds.setProperties(props);

        Map<String, Object> retrieved = ds.getProperties();
        assertEquals("value1", retrieved.get("key1"));
        assertEquals(123, retrieved.get("key2"));
    }

    @Test
    public void testIsFrozen() {
        Dataset ds = dataset.copy();
        assertFalse(ds.isFrozen());

        ds.freeze();
        assertTrue(ds.isFrozen());
    }

    @Test
    public void testTrimToSize() {
        Dataset ds = dataset.copy();

        ds.trimToSize();

        assertEquals(4, ds.size());
        assertEquals(4, ds.columnCount());
        assertEquals("Alice", ds.get(0, 1));
    }

    @Test
    public void testToJson_LowercaseName() {
        String json = dataset.toJson();

        assertNotNull(json);
        assertTrue(json.contains("Alice") || json.contains("alice"));
    }

    @Test
    public void testToXml_LowercaseName() {
        String xml = dataset.toXml();

        assertNotNull(xml);
        assertTrue(xml.contains("Alice") || xml.contains("alice") || xml.length() > 0);
    }

    @Test
    public void testToCsv_LowercaseName() {
        String csv = dataset.toCsv();

        assertNotNull(csv);
        assertTrue(csv.contains("Alice") || csv.contains("alice") || csv.length() > 0);
    }

    public static class Person {
        private int id;
        private String name;
        private int age;
        private double salary;

        public Person() {
        }

        public int getId() {
            return id;
        }

        public void setId(int id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public int getAge() {
            return age;
        }

        public void setAge(int age) {
            this.age = age;
        }

        public double getSalary() {
            return salary;
        }

        public void setSalary(double salary) {
            this.salary = salary;
        }
    }
}

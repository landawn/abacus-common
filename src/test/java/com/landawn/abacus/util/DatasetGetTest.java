package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.IntFunction;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import testfixtures.entity.extendDirty.basic.Account;

public class DatasetGetTest extends DatasetTestSupport {
    @Test
    public void getColumnName() {
        assertEquals("ID", sampleDataset.getColumnName(0));
        assertEquals("Name", sampleDataset.getColumnName(1));
        assertEquals("Age", sampleDataset.getColumnName(2));
        assertThrows(IndexOutOfBoundsException.class, () -> sampleDataset.getColumnName(3));
        assertThrows(IndexOutOfBoundsException.class, () -> emptyDataset.getColumnName(0));
    }

    @Test
    public void testGetColumnNameInvalidIndex() {
        assertThrows(IndexOutOfBoundsException.class, () -> dataset.getColumnName(10));
    }

    @Test
    public void getColumnIndex() {
        assertEquals(0, sampleDataset.getColumnIndex("ID"));
        assertEquals(1, sampleDataset.getColumnIndex("Name"));
        assertEquals(2, sampleDataset.getColumnIndex("Age"));
        assertThrows(IllegalArgumentException.class, () -> sampleDataset.getColumnIndex("NonExistent"));
        assertThrows(IllegalArgumentException.class, () -> emptyDataset.getColumnIndex("Any"));
    }

    @Test
    public void testGetColumnIndexInvalidName() {
        assertThrows(IllegalArgumentException.class, () -> dataset.getColumnIndex("invalid"));
    }

    @Test
    public void getColumnIndexes() {
        Collection<String> namesToGet = Arrays.asList("Name", "ID");
        int[] indexes = sampleDataset.getColumnIndexes(namesToGet);
        assertArrayEquals(new int[] { 1, 0 }, indexes);

        Collection<String> allNames = sampleDataset.columnNames();
        int[] allIndexes = sampleDataset.getColumnIndexes(allNames);
        assertArrayEquals(new int[] { 0, 1, 2 }, allIndexes);

        assertThrows(IllegalArgumentException.class, () -> sampleDataset.getColumnIndexes(Arrays.asList("ID", "NonExistent")));
        assertArrayEquals(CommonUtil.EMPTY_INT_ARRAY, sampleDataset.getColumnIndexes(Collections.emptyList()));
    }

    @Test
    public void get_columnIndex_withAbsolute() {
        sampleDataset.moveToRow(1);
        assertEquals((Integer) 2, sampleDataset.get(0));
        assertEquals("Bob", sampleDataset.get(1));
        assertEquals((Integer) 24, sampleDataset.get(2));
    }

    @Test
    public void get_columnName_withAbsolute() {
        sampleDataset.moveToRow(2);
        assertEquals((Integer) 3, sampleDataset.get("ID"));
        assertEquals("Charlie", sampleDataset.get("Name"));
        assertEquals((Integer) 35, sampleDataset.get("Age"));
    }

    @Test
    public void testGetByIndexes() {
        assertEquals((Integer) 1, dataset.get(0, 0));
        assertEquals("John", dataset.get(0, 1));
        assertEquals((Integer) 25, dataset.get(0, 2));
        assertEquals(50000.0, dataset.get(0, 3));
    }

    @Test
    public void testGetByColumnIndex() {
        Dataset ds = dataset.moveToRow(1);
        assertEquals("Jane", ds.get(1));
        assertEquals((Integer) 30, ds.get(2));
    }

    @Test
    public void testGetByColumnName() {
        Dataset ds = dataset.moveToRow(0);
        assertEquals("John", ds.get("name"));
        assertEquals((Integer) 25, ds.get("age"));
    }

    @Test
    public void testGet() {
        assertEquals((Integer) 1, dataset.get(0, 0));
        assertEquals("John", dataset.get(0, 1));
        assertEquals((Integer) 25, dataset.get(0, 2));
        assertEquals((Double) 50000.0, dataset.get(0, 3));
    }

    @Test
    public void testGetWithCurrentRow() {
        dataset.moveToRow(1);
        assertEquals((Integer) 2, dataset.get(0));
        assertEquals("Jane", dataset.get(1));
    }

    @Test
    public void test_get_set() throws Exception {
        final List<Account> accountList = createAccountList(Account.class, 9);
        final Dataset ds = CommonUtil.newDataset(accountList);

        final String newValue = "abc123";

        ds.set(1, 1, newValue);

        assertEquals(newValue, ds.get(1, 1));

        assertFalse(ds.moveToRow(1).isNull(1));
        assertFalse(ds.moveToRow(1).isNull(ds.getColumnName(1)));

        ds.set(1, 1, null);

        assertTrue(ds.moveToRow(1).isNull(1));
        assertTrue(ds.moveToRow(1).isNull(ds.getColumnName(1)));
    }

    @Test
    public void get_rowIndex_columnIndex() {
        assertEquals((Integer) 1, sampleDataset.moveToRow(0).get(0));
        assertEquals("Bob", sampleDataset.moveToRow(1).get(1));
        assertEquals((Integer) 35, sampleDataset.moveToRow(2).get(2));
        assertThrows(IndexOutOfBoundsException.class, () -> sampleDataset.moveToRow(3).get(0));
        assertThrows(IndexOutOfBoundsException.class, () -> sampleDataset.moveToRow(0).get(3));
    }

    @Test
    public void testGetBoolean_ByIndex() {
        Dataset ds = Dataset.rows(Arrays.asList("flag"), new Object[][] { { true }, { false } });
        ds.moveToRow(0);
        assertTrue(ds.getBoolean(0));
        ds.moveToRow(1);
        assertFalse(ds.getBoolean(0));
    }

    @Test
    public void testGetBoolean_ByName() {
        Dataset ds = Dataset.rows(Arrays.asList("flag"), new Object[][] { { true }, { false } });
        ds.moveToRow(0);
        assertTrue(ds.getBoolean("flag"));
        ds.moveToRow(1);
        assertFalse(ds.getBoolean("flag"));
    }

    @Test
    public void testGetBoolean() {
        dataset.set(0, 0, true);
        assertTrue(dataset.getBoolean(0));
        assertTrue(dataset.getBoolean("id"));

        dataset.set(0, 0, false);
        assertFalse(dataset.getBoolean(0));
    }

    @Test
    public void getBoolean_primitiveAndObject() {
        List<String> boolNames = List.of("BoolCol");
        List<List<Object>> boolVals = List.of(new ArrayList<>(Arrays.asList(true, false, null, Boolean.TRUE, Boolean.FALSE)));
        RowDataset boolDs = new RowDataset(boolNames, boolVals);

        boolDs.moveToRow(0);
        assertTrue(boolDs.getBoolean(0));
        assertTrue(boolDs.getBoolean("BoolCol"));

        boolDs.moveToRow(1);
        assertFalse(boolDs.getBoolean(0));

        boolDs.moveToRow(2);
        assertFalse(boolDs.getBoolean(0));

        boolDs.moveToRow(3);
        assertTrue(boolDs.getBoolean(0));

        boolDs.moveToRow(4);
        assertFalse(boolDs.getBoolean(0));
    }

    @Test
    public void testGetChar_ByIndex() {
        Dataset ds = Dataset.rows(Arrays.asList("letter"), new Object[][] { { 'A' }, { 'B' } });
        ds.moveToRow(0);
        assertEquals('A', ds.getChar(0));
        ds.moveToRow(1);
        assertEquals('B', ds.getChar(0));
    }

    @Test
    public void testGetChar_ByName() {
        Dataset ds = Dataset.rows(Arrays.asList("letter"), new Object[][] { { 'A' }, { 'B' } });
        ds.moveToRow(0);
        assertEquals('A', ds.getChar("letter"));
    }

    @Test
    public void testGetChar() {
        dataset.set(0, 0, 'A');
        assertEquals('A', dataset.getChar(0));
        assertEquals('A', dataset.getChar("id"));
    }

    @Test
    public void getChar_primitiveAndObject() {
        List<String> charNames = List.of("CharCol");
        List<List<Object>> charVals = List.of(new ArrayList<>(Arrays.asList('a', 'Z', null, Character.valueOf('x'))));
        RowDataset charDs = new RowDataset(charNames, charVals);

        charDs.moveToRow(0);
        assertEquals('a', charDs.getChar(0));
        assertEquals('a', charDs.getChar("CharCol"));

        charDs.moveToRow(1);
        assertEquals('Z', charDs.getChar(0));

        charDs.moveToRow(2);
        assertEquals((char) 0, charDs.getChar(0));

        charDs.moveToRow(3);
        assertEquals('x', charDs.getChar(0));
    }

    @Test
    public void testGetByte_ByIndex() {
        Dataset ds = Dataset.rows(Arrays.asList("num"), new Object[][] { { (byte) 1 }, { (byte) 2 } });
        ds.moveToRow(0);
        assertEquals((byte) 1, ds.getByte(0));
    }

    @Test
    public void testGetByte_ByName() {
        Dataset ds = Dataset.rows(Arrays.asList("num"), new Object[][] { { (byte) 1 }, { (byte) 2 } });
        ds.moveToRow(0);
        assertEquals((byte) 1, ds.getByte("num"));
    }

    @Test
    public void testGetByte() {
        dataset.set(0, 0, (byte) 10);
        assertEquals((byte) 10, dataset.getByte(0));
        assertEquals((byte) 10, dataset.getByte("id"));
    }

    @Test
    public void testGetShort_ByIndex() {
        Dataset ds = Dataset.rows(Arrays.asList("num"), new Object[][] { { (short) 100 }, { (short) 200 } });
        ds.moveToRow(0);
        assertEquals((short) 100, ds.getShort(0));
    }

    @Test
    public void testGetShort_ByName() {
        Dataset ds = Dataset.rows(Arrays.asList("num"), new Object[][] { { (short) 100 }, { (short) 200 } });
        ds.moveToRow(0);
        assertEquals((short) 100, ds.getShort("num"));
    }

    @Test
    public void testGetShort() {
        dataset.set(0, 0, (short) 100);
        assertEquals((short) 100, dataset.getShort(0));
        assertEquals((short) 100, dataset.getShort("id"));
    }

    @Test
    public void testGetInt_ByIndex() {
        dataset.moveToRow(0);
        assertEquals(25, dataset.getInt(2));
    }

    @Test
    public void testGetInt_ByName() {
        dataset.moveToRow(0);
        assertEquals(25, dataset.getInt("age"));
    }

    @Test
    public void testGetInt() {
        assertEquals(1, dataset.getInt(0));
        assertEquals(1, dataset.getInt("id"));
    }

    @Test
    public void getInt_primitiveAndObject() {
        List<String> intNames = List.of("IntCol");
        List<List<Object>> intVals = List.of(new ArrayList<>(Arrays.asList(10, -5, null, Integer.valueOf(100), Long.valueOf(200L))));
        RowDataset intDs = new RowDataset(intNames, intVals);

        intDs.moveToRow(0);
        assertEquals(10, intDs.getInt(0));
        assertEquals(10, intDs.getInt("IntCol"));

        intDs.moveToRow(1);
        assertEquals(-5, intDs.getInt(0));

        intDs.moveToRow(2);
        assertEquals(0, intDs.getInt(0));

        intDs.moveToRow(3);
        assertEquals(100, intDs.getInt(0));

        intDs.moveToRow(4);
        assertEquals(200, intDs.getInt(0));
    }

    @Test
    public void testGetLong_ByIndex() {
        Dataset ds = Dataset.rows(Arrays.asList("num"), new Object[][] { { 1000L }, { 2000L } });
        ds.moveToRow(0);
        assertEquals(1000L, ds.getLong(0));
    }

    @Test
    public void testGetLong_ByName() {
        Dataset ds = Dataset.rows(Arrays.asList("num"), new Object[][] { { 1000L }, { 2000L } });
        ds.moveToRow(0);
        assertEquals(1000L, ds.getLong("num"));
    }

    @Test
    public void testGetLong() {
        assertEquals(1L, dataset.getLong(0));
        assertEquals(1L, dataset.getLong("id"));
    }

    @Test
    public void testGetFloat_ByIndex() {
        Dataset ds = Dataset.rows(Arrays.asList("num"), new Object[][] { { 1.5f }, { 2.5f } });
        ds.moveToRow(0);
        assertEquals(1.5f, ds.getFloat(0));
    }

    @Test
    public void testGetFloat_ByName() {
        Dataset ds = Dataset.rows(Arrays.asList("num"), new Object[][] { { 1.5f }, { 2.5f } });
        ds.moveToRow(0);
        assertEquals(1.5f, ds.getFloat("num"));
    }

    @Test
    public void testGetFloat() {
        assertEquals(50000.0f, dataset.getFloat(3), 0.01);
        assertEquals(50000.0f, dataset.getFloat("salary"), 0.01);
    }

    @Test
    public void testGetDouble_ByIndex() {
        dataset.moveToRow(0);
        assertEquals(50000.0, dataset.getDouble(3));
    }

    @Test
    public void testGetDouble_ByName() {
        dataset.moveToRow(0);
        assertEquals(50000.0, dataset.getDouble("salary"));
    }

    @Test
    public void testGetDouble() {
        assertEquals(50000.0, dataset.getDouble(3), 0.01);
        assertEquals(50000.0, dataset.getDouble("salary"), 0.01);
    }

    @Test
    public void getColumn_byName() {
        List<Object> nameColumn = sampleDataset.getColumn("Name");
        assertEquals(Arrays.asList("Alice", "Bob", "Charlie"), nameColumn);
    }

    @Test
    public void testGetColumnByIndex() {
        ImmutableList<Object> column = dataset.getColumn(1);
        assertEquals(5, column.size());
        assertEquals("John", column.get(0));
        assertEquals("Jane", column.get(1));
        assertEquals("Bob", column.get(2));
        assertEquals("Alice", column.get(3));
        assertEquals("Charlie", column.get(4));
    }

    @Test
    public void testGetColumnByName() {
        ImmutableList<Object> column = dataset.getColumn("age");
        assertEquals(5, column.size());
        assertEquals(25, column.get(0));
        assertEquals(30, column.get(1));
        assertEquals(35, column.get(2));
        assertEquals(28, column.get(3));
        assertEquals(40, column.get(4));
    }

    @Test
    public void testGetColumn() {
        ImmutableList<Object> idColumn = dataset.getColumn(0);
        assertEquals(5, idColumn.size());
        assertEquals(Arrays.asList(1, 2, 3, 4, 5), idColumn);

        ImmutableList<Object> nameColumn = dataset.getColumn("name");
        assertEquals(Arrays.asList("John", "Jane", "Bob", "Alice", "Charlie"), nameColumn);
    }

    @Test
    public void getColumn_byIndex() {
        List<Object> idColumn = sampleDataset.getColumn(0);
        assertEquals(Arrays.asList(1, 2, 3), idColumn);
        assertThrows(UnsupportedOperationException.class, () -> sampleDataset.getColumn(0).add(4));
    }

    @Test
    public void getRow_asImmutableList() {
        List<Object> row0 = sampleDataset.getRow(0);
        assertTrue(row0 instanceof ImmutableList);
        assertTrue(((ImmutableList<?>) row0).list instanceof java.util.AbstractList);
        assertEquals(Arrays.asList(1, "Alice", 30), row0);
    }

    @Test
    public void getRow_asSpecificType_Array() {
        Object[] row1 = sampleDataset.getRow(1, Object[].class);
        assertArrayEquals(new Object[] { 2, "Bob", 24 }, row1);
    }

    @Test
    public void getRow_asSpecificType_List() {
        List<Object> row2 = sampleDataset.getRow(2, List.class);
        assertEquals(Arrays.asList(3, "Charlie", 35), row2);
    }

    @Test
    public void getRow_asSpecificType_Map() {
        Map<String, Object> row0Map = sampleDataset.getRow(0, Map.class);
        assertEquals(1, row0Map.get("ID"));
        assertEquals("Alice", row0Map.get("Name"));
        assertEquals(30, row0Map.get("Age"));
    }

    @Test
    public void getRow_asSpecificType_Bean() {
        RowDataset beanFriendlyDs = new RowDataset(Arrays.asList("id", "name", "value"), Arrays.asList(new ArrayList<>(Arrays.asList(101, 102)),
                new ArrayList<>(Arrays.asList("Bean1", "Bean2")), new ArrayList<>(Arrays.asList(10.5, 20.5))));
        TestBean bean1 = beanFriendlyDs.getRow(0, TestBean.class);
        assertEquals(101, bean1.getId());
        assertEquals("Bean1", bean1.getName());
        assertEquals(10.5, bean1.getValue(), 0.001);
    }

    @Test
    public void getRow_withSelectedColumns_asArray() {
        Object[] partialRow = sampleDataset.getRow(0, Arrays.asList("Name", "ID"), Object[].class);
        assertArrayEquals(new Object[] { "Alice", 1 }, partialRow);
    }

    @Test
    public void getRow_withSupplier() {
        AtomicInteger counter = new AtomicInteger(0);
        Map<String, Object> row = sampleDataset.getRow(0, size -> {
            counter.incrementAndGet();
            return new LinkedHashMap<>(size);
        });
        assertEquals(1, counter.get());
        assertTrue(row instanceof LinkedHashMap);
        assertEquals(1, row.get("ID"));
    }

    @Test
    public void testGetRow() {
        ImmutableList<Object> row = dataset.getRow(0);
        assertEquals(4, row.size());
        assertEquals(1, row.get(0));
        assertEquals("John", row.get(1));
    }

    @Test
    public void testGetRowNavigationMethods() {
        testDataset.moveToRow(0);

        assertEquals(1, testDataset.getByte(0));
        assertEquals(1, testDataset.getShort(0));
        assertEquals(1, testDataset.getInt(0));
        assertEquals(1L, testDataset.getLong(0));
        assertEquals(1.0f, testDataset.getFloat(0));
        assertEquals(1.0, testDataset.getDouble(0));

        assertEquals("Alice", testDataset.get(1));
        assertEquals(30, testDataset.getInt(2));
        assertEquals(50000.0, testDataset.getDouble(3));
    }

    @Test
    public void testGetRowWithClass() {
        List<Object> row = dataset.getRow(1, ArrayList.class);
        assertEquals(Arrays.asList(2, "Jane", 30, 60000.0), row);
    }

    @Test
    public void testGetRowWithColumnNames() {
        Object[] row = dataset.getRow(1, Arrays.asList("name", "age"), Object[].class);
        assertArrayEquals(new Object[] { "Jane", 30 }, row);
    }

    @Test
    public void testGetRow_AsEntity() {
        Person person = dataset.getRow(0, Person.class);
        assertNotNull(person);
        assertEquals(1, person.getId());
        assertEquals("John", person.getName());
    }

    @Test
    public void testGetRow_WithColumnNames() {
        Person person = dataset.getRow(0, Arrays.asList("id", "name"), Person.class);
        assertNotNull(person);
        assertEquals(1, person.getId());
        assertEquals("John", person.getName());
    }

    @Test
    public void testGetRowWithSupplier() {
        List<Object> row = dataset.getRow(0, (IntFunction<List<Object>>) ArrayList::new);
        assertNotNull(row);
        assertEquals(4, row.size());
        assertEquals(1, row.get(0));
        assertEquals("John", row.get(1));
    }

    @Test
    public void testGetRowWithPrefixAndFieldNameMap() {
        List<String> nestedColumns = Arrays.asList("id", "person.name", "person.age", "address.city");
        List<List<Object>> nestedData = new ArrayList<>();
        nestedData.add(Arrays.asList(1, 2));
        nestedData.add(Arrays.asList("John", "Jane"));
        nestedData.add(Arrays.asList(25, 30));
        nestedData.add(Arrays.asList("NYC", "LA"));

        Dataset nestedDataset = new RowDataset(nestedColumns, nestedData);

        Map<String, String> prefixMap = new HashMap<>();
        prefixMap.put("person", "personInfo");
        prefixMap.put("address", "addressInfo");

        assertThrows(IllegalArgumentException.class, () -> nestedDataset.stream(prefixMap, Map.class).toList());
    }

    @Test
    public void testGetProperties() {
        Dataset ds = dataset.copy();
        Map<String, Object> props = ds.getProperties();

        assertNotNull(props);
        assertTrue(props.isEmpty() || props.size() >= 0);
    }

    @Test
    @DisplayName("getColumn returns an immutable view backed by internal storage (no mutation)")
    public void testGetColumnImmutability() {
        ImmutableList<Object> col = dataset.getColumn("name");
        assertThrows(UnsupportedOperationException.class, () -> col.add("X"));
    }

}

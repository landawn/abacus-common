package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.NoCachingNoUpdating.DisposableObjArray;
import com.landawn.abacus.util.Tuple.Tuple2;
import com.landawn.abacus.util.stream.Stream;

@SuppressWarnings({ "unchecked", "rawtypes", "deprecation" })
@Tag("new-test")
public class Dataset200Test extends TestBase {

    private RowDataset emptyDataset;
    private RowDataset sampleDataset;
    private List<String> columnNames;
    private List<List<Object>> columnValues;

    @BeforeEach
    public void setUp() {
        emptyDataset = new RowDataset(new ArrayList<>(), new ArrayList<>());

        columnNames = new ArrayList<>(Arrays.asList("ID", "Name", "Age"));
        columnValues = new ArrayList<>();
        columnValues.add(new ArrayList<>(Arrays.asList(1, 2, 3)));
        columnValues.add(new ArrayList<>(Arrays.asList("Alice", "Bob", "Charlie")));
        columnValues.add(new ArrayList<>(Arrays.asList(30, 24, 35)));
        sampleDataset = new RowDataset(columnNames, columnValues);
    }

    private RowDataset createCustomDataset(List<String> names, List<List<Object>> values) {
        return new RowDataset(new ArrayList<>(names), new ArrayList<>(values.stream().map(ArrayList::new).collect(Collectors.toList())));
    }

    @Test
    public void constructor_emptyLists() {
        RowDataset ds = new RowDataset(Collections.emptyList(), Collections.emptyList());
        assertNotNull(ds);
        assertTrue(ds.isEmpty());
        assertEquals(0, ds.columnCount());
    }

    @Test
    public void constructor_withData() {
        assertEquals(3, sampleDataset.columnCount());
        assertEquals(3, sampleDataset.size());
        assertFalse(sampleDataset.isFrozen());
        assertEquals(columnNames, sampleDataset.columnNames());
    }

    @Test
    public void constructor_withProperties() {
        Map<String, Object> props = new HashMap<>();
        props.put("key1", "value1");
        RowDataset ds = new RowDataset(columnNames, columnValues, props);
        assertEquals("value1", ds.getProperties().get("key1"));
    }

    @Test
    public void constructor_mismatchedColumnNameAndListSize_throwsIllegalArgumentException() {
        List<String> names = Arrays.asList("A", "B");
        List<List<Object>> values = List.of(Arrays.asList(1, 2));
        assertThrows(IllegalArgumentException.class, () -> new RowDataset(names, values));
    }

    @Test
    public void constructor_mismatchedColumnSizes_throwsIllegalArgumentException() {
        List<String> names = Arrays.asList("A", "B");
        List<List<Object>> values = List.of(Arrays.asList(1, 2), Arrays.asList(3));
        assertThrows(IllegalArgumentException.class, () -> new RowDataset(names, values));
    }

    @Test
    public void constructor_duplicateColumnNames_throwsIllegalArgumentException() {
        List<String> duplicateNames = Arrays.asList("ID", "Name", "ID");
        assertThrows(IllegalArgumentException.class, () -> new RowDataset(duplicateNames, columnValues));
    }

    @Test
    public void columnNameList() {
        assertEquals(columnNames, sampleDataset.columnNames());
        assertTrue(emptyDataset.columnNames().isEmpty());
    }

    @Test
    public void columnCount() {
        assertEquals(3, sampleDataset.columnCount());
        assertEquals(0, emptyDataset.columnCount());
    }

    @Test
    public void getColumnName() {
        assertEquals("ID", sampleDataset.getColumnName(0));
        assertEquals("Name", sampleDataset.getColumnName(1));
        assertEquals("Age", sampleDataset.getColumnName(2));
        assertThrows(IndexOutOfBoundsException.class, () -> sampleDataset.getColumnName(3));
        assertThrows(IndexOutOfBoundsException.class, () -> emptyDataset.getColumnName(0));
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
    public void containsColumn() {
        assertTrue(sampleDataset.containsColumn("ID"));
        assertFalse(sampleDataset.containsColumn("NonExistent"));
        assertFalse(emptyDataset.containsColumn("Any"));
    }

    @Test
    public void containsAllColumns() {
        assertTrue(sampleDataset.containsAllColumns(Arrays.asList("ID", "Age")));
        assertFalse(sampleDataset.containsAllColumns(Arrays.asList("ID", "NonExistent")));
        assertTrue(sampleDataset.containsAllColumns(Collections.emptyList()));
        assertTrue(emptyDataset.containsAllColumns(Collections.emptyList()));
        assertFalse(emptyDataset.containsAllColumns(Arrays.asList("ID")));
    }

    @Test
    public void renameColumn() {
        sampleDataset.renameColumn("Age", "Years");
        assertEquals(2, sampleDataset.getColumnIndex("Years"));
        assertTrue(sampleDataset.columnNames().contains("Years"));
        assertFalse(sampleDataset.columnNames().contains("Age"));
        assertThrows(IllegalArgumentException.class, () -> sampleDataset.renameColumn("NonExistent", "NewName"));
        assertThrows(IllegalArgumentException.class, () -> sampleDataset.renameColumn("ID", "Name"));

        sampleDataset.renameColumn("Years", "Years");
        assertEquals(2, sampleDataset.getColumnIndex("Years"));

        RowDataset ds = createSimpleDataset();
        ds.freeze();
        assertThrows(IllegalStateException.class, () -> ds.renameColumn("ID", "NewID"));
    }

    @Test
    public void renameColumns_map() {
        Map<String, String> renames = new HashMap<>();
        renames.put("ID", "Identifier");
        renames.put("Age", "Years");
        sampleDataset.renameColumns(renames);
        assertTrue(sampleDataset.columnNames().contains("Identifier"));
        assertTrue(sampleDataset.columnNames().contains("Years"));
        assertFalse(sampleDataset.columnNames().contains("ID"));
        assertFalse(sampleDataset.columnNames().contains("Age"));

        Map<String, String> invalidRenames = new HashMap<>();
        invalidRenames.put("NonExistent", "NewName");
        assertThrows(IllegalArgumentException.class, () -> sampleDataset.renameColumns(invalidRenames));

        Map<String, String> duplicateNewNames = new HashMap<>();
        duplicateNewNames.put("Name", "Duplicate");
        duplicateNewNames.put("Years", "Duplicate");
        assertThrows(IllegalArgumentException.class, () -> sampleDataset.renameColumns(duplicateNewNames));
    }

    @Test
    public void renameColumns_collectionAndFunction() {
        sampleDataset.renameColumns(Arrays.asList("ID", "Age"), name -> name + "_new");
        assertTrue(sampleDataset.columnNames().contains("ID_new"));
        assertTrue(sampleDataset.columnNames().contains("Age_new"));
        assertFalse(sampleDataset.columnNames().contains("ID"));
        assertFalse(sampleDataset.columnNames().contains("Age"));

        assertThrows(IllegalArgumentException.class, () -> sampleDataset.renameColumns(Arrays.asList("Name", "NonExistent"), String::toUpperCase));
    }

    @Test
    public void renameColumns_functionForAll() {
        sampleDataset.renameColumns(name -> "col_" + name);
        assertTrue(sampleDataset.columnNames().contains("col_ID"));
        assertTrue(sampleDataset.columnNames().contains("col_Name"));
        assertTrue(sampleDataset.columnNames().contains("col_Age"));
    }

    @Test
    public void moveColumn() {
        sampleDataset.moveColumn("Age", 0);
        assertEquals(Arrays.asList("Age", "ID", "Name"), sampleDataset.columnNames());
        assertEquals((Integer) 35, sampleDataset.moveToRow(2).get("Age"));

        assertThrows(IndexOutOfBoundsException.class, () -> sampleDataset.moveColumn("ID", 5));
        assertThrows(IllegalArgumentException.class, () -> sampleDataset.moveColumn("NonExistent", 0));
    }

    @Test
    public void swapColumns() {
        sampleDataset.swapColumns("ID", "Age");
        assertEquals(Arrays.asList("Age", "Name", "ID"), sampleDataset.columnNames());
        assertEquals((Integer) 30, sampleDataset.moveToRow(0).get("Age"));
        assertEquals((Integer) 1, sampleDataset.moveToRow(0).get("ID"));

        sampleDataset.swapColumns("Name", "Name");
        assertEquals(Arrays.asList("Age", "Name", "ID"), sampleDataset.columnNames());

        assertThrows(IllegalArgumentException.class, () -> sampleDataset.swapColumns("ID", "NonExistent"));
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
    public void set_rowIndex_columnIndex_value() {
        sampleDataset.set(0, 0, 100);
        assertEquals((Integer) 100, sampleDataset.moveToRow(0).get(0));

        RowDataset ds = createSimpleDataset();
        ds.freeze();
        assertThrows(IllegalStateException.class, () -> ds.set(0, 0, 99));
    }

    @Test
    public void isNull_rowIndex_columnIndex() {
        assertFalse(sampleDataset.isNull(0, 0));
        sampleDataset.set(0, 0, null);
        assertTrue(sampleDataset.isNull(0, 0));
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
    public void isNull_columnIndex_withAbsolute() {
        sampleDataset.moveToRow(0);
        assertFalse(sampleDataset.isNull(0));
        sampleDataset.set(0, null);
        assertTrue(sampleDataset.isNull(0));
        assertTrue(sampleDataset.isNull("ID"));
    }

    @Test
    public void set_columnIndex_value_withAbsolute() {
        sampleDataset.moveToRow(1);
        sampleDataset.set(0, 200);
        assertEquals((Integer) 200, sampleDataset.moveToRow(1).get(0));
    }

    @Test
    public void set_columnName_value_withAbsolute() {
        sampleDataset.moveToRow(2);
        sampleDataset.set("Age", 40);
        assertEquals((Integer) 40, sampleDataset.moveToRow(2).get(2));
    }

    @Test
    public void getColumn_byIndex() {
        List<Object> idColumn = sampleDataset.getColumn(0);
        assertEquals(Arrays.asList(1, 2, 3), idColumn);
        assertThrows(UnsupportedOperationException.class, () -> sampleDataset.getColumn(0).add(4));
    }

    @Test
    public void getColumn_byName() {
        List<Object> nameColumn = sampleDataset.getColumn("Name");
        assertEquals(Arrays.asList("Alice", "Bob", "Charlie"), nameColumn);
    }

    @Test
    public void copyColumn() {
        List<Object> ageColumnCopy = sampleDataset.copyColumn("Age");
        assertEquals(Arrays.asList(30, 24, 35), ageColumnCopy);
        ageColumnCopy.add(40);
        assertEquals(Arrays.asList(30, 24, 35), sampleDataset.getColumn("Age"));
    }

    @Test
    public void addColumn_newColumnName_collection() {
        List<String> newColData = Arrays.asList("X", "Y", "Z");
        sampleDataset.addColumn("Grade", newColData);
        assertEquals(4, sampleDataset.columnCount());
        assertTrue(sampleDataset.columnNames().contains("Grade"));
        assertEquals(newColData, sampleDataset.getColumn("Grade"));
        assertEquals("X", sampleDataset.moveToRow(0).get("Grade"));

        sampleDataset.addColumn("EmptyGrade", null);
        assertEquals(5, sampleDataset.columnCount());
        assertEquals(Arrays.asList(null, null, null), sampleDataset.getColumn("EmptyGrade"));

        assertThrows(IllegalArgumentException.class, () -> sampleDataset.addColumn("ID", Arrays.asList("A")));
        assertThrows(IllegalArgumentException.class, () -> sampleDataset.addColumn("Score", Arrays.asList(10, 20)));
    }

    @Test
    public void addColumn_newColumnPosition_newColumnName_collection() {
        List<Double> scores = Arrays.asList(90.5, 88.0, 92.0);
        sampleDataset.addColumn(1, "Score", scores);
        assertEquals(4, sampleDataset.columnCount());
        assertEquals(Arrays.asList("ID", "Score", "Name", "Age"), sampleDataset.columnNames());
        assertEquals(scores, sampleDataset.getColumn("Score"));
        assertEquals(90.5, sampleDataset.moveToRow(0).get("Score"));

        assertThrows(IllegalArgumentException.class, () -> sampleDataset.addColumn(0, "ID", scores));
    }

    @Test
    public void addColumn_fromAnotherColumn_withFunction() {
        sampleDataset.addColumn("AgePlus5", "Age", (Integer age) -> age + 5);
        assertEquals(4, sampleDataset.columnCount());
        assertTrue(sampleDataset.columnNames().contains("AgePlus5"));
        assertEquals(Arrays.asList(35, 29, 40), sampleDataset.getColumn("AgePlus5"));
    }

    @Test
    public void addColumn_fromMultipleColumns_withFunction() {
        sampleDataset.addColumn("ID_Name", Arrays.asList("ID", "Name"), (DisposableObjArray row) -> row.get(0) + "_" + row.get(1));
        assertEquals(4, sampleDataset.columnCount());
        assertTrue(sampleDataset.columnNames().contains("ID_Name"));
        assertEquals(Arrays.asList("1_Alice", "2_Bob", "3_Charlie"), sampleDataset.getColumn("ID_Name"));
    }

    @Test
    public void addColumn_fromTuple2_withBiFunction() {
        sampleDataset.addColumn("Name_Age_Str", Tuple.of("Name", "Age"), (String name, Integer age) -> name + ":" + age);
        assertEquals(4, sampleDataset.columnCount());
        assertTrue(sampleDataset.columnNames().contains("Name_Age_Str"));
        assertEquals(Arrays.asList("Alice:30", "Bob:24", "Charlie:35"), sampleDataset.getColumn("Name_Age_Str"));
    }

    @Test
    public void addColumn_fromTuple3_withTriFunction() {
        List<String> t3ColNames = new ArrayList<>(Arrays.asList("C1", "C2", "C3", "C4"));
        List<List<Object>> t3ColValues = new ArrayList<>();
        t3ColValues.add(new ArrayList<>(Arrays.asList(1, 2)));
        t3ColValues.add(new ArrayList<>(Arrays.asList("A", "B")));
        t3ColValues.add(new ArrayList<>(Arrays.asList(true, false)));
        t3ColValues.add(new ArrayList<>(Arrays.asList(1.1, 2.2)));
        RowDataset t3ds = new RowDataset(t3ColNames, t3ColValues);

        t3ds.addColumn("Combined", Tuple.of("C1", "C2", "C3"), (Integer c1, String c2, Boolean c3) -> c1 + "_" + c2 + "_" + c3);
        assertEquals(5, t3ds.columnCount());
        assertEquals(Arrays.asList("1_A_true", "2_B_false"), t3ds.getColumn("Combined"));
    }

    @Test
    public void removeColumn() {
        List<Object> removedCol = sampleDataset.removeColumn("Name");
        assertEquals(Arrays.asList("Alice", "Bob", "Charlie"), removedCol);
        assertEquals(2, sampleDataset.columnCount());
        assertFalse(sampleDataset.columnNames().contains("Name"));
        assertThrows(IllegalArgumentException.class, () -> sampleDataset.removeColumn("NonExistent"));
    }

    @Test
    public void removeColumns_collection() {
        sampleDataset.removeColumns(Arrays.asList("ID", "Age"));
        assertEquals(1, sampleDataset.columnCount());
        assertEquals(Collections.singletonList("Name"), sampleDataset.columnNames());
    }

    @Test
    public void removeColumns_predicate() {
        sampleDataset.removeColumns(name -> name.equals("ID") || name.endsWith("e"));
        assertEquals(0, sampleDataset.columnCount());
        assertTrue(sampleDataset.columnNames().isEmpty());
    }

    @Test
    public void convertColumn() {
        sampleDataset.convertColumn("Age", String.class);
        assertEquals("30", sampleDataset.moveToRow(0).get("Age"));
        assertTrue(sampleDataset.getColumn("Age").get(0) instanceof String);

        sampleDataset.convertColumn("ID", Double.class);
        assertEquals(1.0, sampleDataset.moveToRow(0).get("ID"));
        assertTrue(sampleDataset.getColumn("ID").get(0) instanceof Double);
    }

    @Test
    public void convertColumns_map() {
        Map<String, Class<?>> conversions = new HashMap<>();
        conversions.put("ID", String.class);
        conversions.put("Age", Double.class);
        sampleDataset.convertColumns(conversions);

        assertTrue(sampleDataset.getColumn("ID").get(0) instanceof String);
        assertEquals("1", sampleDataset.moveToRow(0).get("ID"));
        assertTrue(sampleDataset.getColumn("Age").get(0) instanceof Double);
        assertEquals(30.0, sampleDataset.moveToRow(0).get("Age"));
    }

    @Test
    public void updateColumn() {
        sampleDataset.updateColumn("Age", (Integer age) -> age * 2);
        assertEquals(Arrays.asList(60, 48, 70), sampleDataset.getColumn("Age"));
    }

    @Test
    public void updateColumns_collection() {
        sampleDataset.updateColumns(Arrays.asList("ID", "Age"), (i, c, v) -> ((Number) v).intValue() + 100);
        assertEquals(Arrays.asList(101, 102, 103), sampleDataset.getColumn("ID"));
        assertEquals(Arrays.asList(130, 124, 135), sampleDataset.getColumn("Age"));
    }

    @Test
    public void combineColumns_toNewType() {
        sampleDataset.combineColumns(Arrays.asList("ID", "Age"), "ID_Age_Combined", Map.class);
        assertEquals(2, sampleDataset.columnCount());
        assertTrue(sampleDataset.columnNames().contains("ID_Age_Combined"));
        assertTrue(sampleDataset.columnNames().contains("Name"));
        assertFalse(sampleDataset.columnNames().contains("ID"));
        assertFalse(sampleDataset.columnNames().contains("Age"));

        assertEquals(Map.of("ID", 1, "Age", 30), sampleDataset.moveToRow(0).get("ID_Age_Combined"));
        assertEquals(Map.of("ID", 2, "Age", 24), sampleDataset.moveToRow(1).get("ID_Age_Combined"));
    }

    @Test
    public void combineColumns_withFunction() {
        sampleDataset.combineColumns(Arrays.asList("Name", "Age"), "NameAndAge", (DisposableObjArray row) -> row.get(0) + " is " + row.get(1));
        assertEquals(2, sampleDataset.columnCount());
        assertTrue(sampleDataset.columnNames().contains("NameAndAge"));
        assertEquals(Arrays.asList("Alice is 30", "Bob is 24", "Charlie is 35"), sampleDataset.getColumn("NameAndAge"));
    }

    @Test
    public void divideColumn_withFunctionToList() {
        sampleDataset.addColumn("FullName", Arrays.asList("Alice Wonderland", "Bob TheBuilder", "Charlie Brown"));
        sampleDataset.divideColumn("FullName", Arrays.asList("FirstName", "LastName"), (String fullName) -> {
            if (fullName == null)
                return Arrays.asList(null, null);
            String[] parts = fullName.split(" ", 2);
            return Arrays.asList(parts[0], parts.length > 1 ? parts[1] : null);
        });

        assertEquals(5, sampleDataset.columnCount());
        assertTrue(sampleDataset.columnNames().contains("FirstName"));
        assertTrue(sampleDataset.columnNames().contains("LastName"));
        assertEquals(Arrays.asList("Alice", "Bob", "Charlie"), sampleDataset.getColumn("FirstName"));
        assertEquals(Arrays.asList("Wonderland", "TheBuilder", "Brown"), sampleDataset.getColumn("LastName"));
    }

    @Test
    public void divideColumn_withBiConsumerObjectArray() {
        sampleDataset.addColumn("Coords", Arrays.asList("10,20", "30,40", "50,60"));
        sampleDataset.divideColumn("Coords", Arrays.asList("X", "Y"), (String coords, Object[] output) -> {
            if (coords == null) {
                output[0] = null;
                output[1] = null;
                return;
            }
            String[] parts = coords.split(",");
            output[0] = Integer.parseInt(parts[0]);
            output[1] = Integer.parseInt(parts[1]);
        });
        assertEquals(5, sampleDataset.columnCount());
        assertEquals(Arrays.asList(10, 30, 50), sampleDataset.getColumn("X"));
        assertEquals(Arrays.asList(20, 40, 60), sampleDataset.getColumn("Y"));
    }

    @Test
    public void divideColumn_withBiConsumerPair() {
        sampleDataset.addColumn("NameAndInitial", Arrays.asList("Alice A", "Bob B", "Charlie C"));
        sampleDataset.divideColumn("NameAndInitial", Tuple.of("DerivedName", "Initial"), (String ni, Pair<Object, Object> output) -> {
            if (ni == null) {
                output.set(null, null);
                return;
            }
            String[] parts = ni.split(" ");
            output.setLeft(parts[0]);
            output.setRight(parts[1].charAt(0));
        });
        assertEquals(Arrays.asList("Alice", "Bob", "Charlie"), sampleDataset.getColumn("DerivedName"));
        assertEquals(Arrays.asList('A', 'B', 'C'), sampleDataset.getColumn("Initial"));
    }

    @Test
    public void addRow_fromArray() {
        sampleDataset.addRow(new Object[] { 4, "David", 28 });
        assertEquals(4, sampleDataset.size());
        assertEquals("David", sampleDataset.moveToRow(3).get("Name"));
    }

    @Test
    public void addRow_fromList() {
        sampleDataset.addRow(Arrays.asList(4, "Eve", 40));
        assertEquals(4, sampleDataset.size());
        assertEquals((Integer) 40, sampleDataset.moveToRow(3).get("Age"));
    }

    @Test
    public void addRow_fromMap() {
        Map<String, Object> newRowMap = new LinkedHashMap<>();
        newRowMap.put("ID", 4);
        newRowMap.put("Name", "Frank");
        newRowMap.put("Age", 33);
        sampleDataset.addRow(newRowMap);
        assertEquals(4, sampleDataset.size());
        assertEquals("Frank", sampleDataset.moveToRow(3).get("Name"));

        Map<String, Object> incompleteMap = Map.of("ID", 5, "Name", "Grace");
        assertThrows(IllegalArgumentException.class, () -> sampleDataset.addRow(incompleteMap));
    }

    @Test
    public void addRow_fromBean() {
        TestBean bean = new TestBean(4, "Ivy", 22);
        RowDataset beanDs = new RowDataset(Arrays.asList("id", "name", "value"), Arrays.asList(new ArrayList<>(), new ArrayList<>(), new ArrayList<>()));
        beanDs.addRow(bean);
        assertEquals(1, beanDs.size());
        assertEquals((Integer) 4, beanDs.moveToRow(0).get("id"));
        assertEquals("Ivy", beanDs.moveToRow(0).get("name"));
        assertEquals(22.0, (double) beanDs.moveToRow(0).get("value"), 0.001);
    }

    @Test
    public void addRow_atPosition() {
        sampleDataset.addRow(1, new Object[] { 0, "Zero", 0 });
        assertEquals(4, sampleDataset.size());
        assertEquals("Zero", sampleDataset.moveToRow(1).get("Name"));
        assertEquals("Bob", sampleDataset.moveToRow(2).get("Name"));
    }

    @Test
    public void removeRow() {
        sampleDataset.removeRow(1);
        assertEquals(2, sampleDataset.size());
        assertEquals("Charlie", sampleDataset.moveToRow(1).get("Name"));
        assertThrows(IndexOutOfBoundsException.class, () -> sampleDataset.removeRow(5));
    }

    @Test
    public void removeRows_indices() {
        sampleDataset.removeRowsAt(0, 2);
        assertEquals(1, sampleDataset.size());
        assertEquals("Bob", sampleDataset.moveToRow(0).get("Name"));
    }

    @Test
    public void removeRowRange() {
        sampleDataset.removeRows(0, 2);
        assertEquals(1, sampleDataset.size());
        assertEquals("Charlie", sampleDataset.moveToRow(0).get("Name"));
        assertThrows(IndexOutOfBoundsException.class, () -> sampleDataset.removeRows(0, 5));
    }

    @Test
    public void updateRow() {
        sampleDataset.updateRow(0, val -> {
            if (val instanceof String)
                return ((String) val).toUpperCase();
            if (val instanceof Integer && ((Integer) val) == 30)
                return 31;
            return val;
        });
        assertEquals("ALICE", sampleDataset.moveToRow(0).get("Name"));
        assertEquals((Integer) 31, sampleDataset.moveToRow(0).get("Age"));
    }

    @Test
    public void updateAll() {
        sampleDataset.updateAll(val -> {
            if (val instanceof String)
                return "Name_" + val;
            if (val instanceof Integer)
                return ((Integer) val) + 10;
            return val;
        });
        assertEquals(Arrays.asList(11, 12, 13), sampleDataset.getColumn("ID"));
        assertEquals(Arrays.asList("Name_Alice", "Name_Bob", "Name_Charlie"), sampleDataset.getColumn("Name"));
        assertEquals(Arrays.asList(40, 34, 45), sampleDataset.getColumn("Age"));
    }

    @Test
    public void replaceIf() {
        sampleDataset.replaceIf(val -> val instanceof String && "Bob".equals(val), "Robert");
        assertEquals("Robert", sampleDataset.moveToRow(1).get("Name"));

        sampleDataset.replaceIf(val -> val instanceof Integer && (Integer) val > 30, 0);
        assertEquals((Integer) 0, sampleDataset.moveToRow(2).get("Age"));
    }

    @Test
    public void getRow_asArray() {
        Object[] row0 = sampleDataset.getRow(0);
        assertArrayEquals(new Object[] { 1, "Alice", 30 }, row0);
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
    public void firstRow_Optional() {
        com.landawn.abacus.util.u.Optional<Object[]> first = sampleDataset.firstRow();
        assertTrue(first.isPresent());
        assertArrayEquals(new Object[] { 1, "Alice", 30 }, first.get());

        com.landawn.abacus.util.u.Optional<Object[]> emptyFirst = emptyDataset.firstRow();
        assertFalse(emptyFirst.isPresent());
    }

    @Test
    public void lastRow_Optional() {
        com.landawn.abacus.util.u.Optional<Object[]> last = sampleDataset.lastRow();
        assertTrue(last.isPresent());
        assertArrayEquals(new Object[] { 3, "Charlie", 35 }, last.get());
    }

    @Test
    public void prepend() {
        List<String> otherNames = Arrays.asList("ID", "Name", "Age");
        List<List<Object>> otherValues = Arrays.asList(new ArrayList<>(Arrays.asList(0)), new ArrayList<>(Arrays.asList("Zero")),
                new ArrayList<>(Arrays.asList(20)));
        RowDataset otherDs = new RowDataset(otherNames, otherValues);

        sampleDataset.prepend(otherDs);
        assertEquals(4, sampleDataset.size());
        assertEquals((Integer) 0, sampleDataset.moveToRow(0).get("ID"));
        assertEquals((Integer) 1, sampleDataset.moveToRow(1).get("ID"));
    }

    @Test
    public void append() {
        List<String> otherNames = Arrays.asList("ID", "Name", "Age");
        List<List<Object>> otherValues = Arrays.asList(new ArrayList<>(Arrays.asList(4)), new ArrayList<>(Arrays.asList("David")),
                new ArrayList<>(Arrays.asList(28)));
        RowDataset otherDs = new RowDataset(otherNames, otherValues);

        sampleDataset.append(otherDs);
        assertEquals(4, sampleDataset.size());
        assertEquals((Integer) 4, sampleDataset.moveToRow(3).get("ID"));
    }

    @Test
    public void currentRowIndex_and_absolute() {
        assertEquals(0, sampleDataset.currentRowIndex());
        sampleDataset.moveToRow(1);
        assertEquals(1, sampleDataset.currentRowIndex());
        assertThrows(IndexOutOfBoundsException.class, () -> sampleDataset.moveToRow(10));
    }

    @Test
    public void iterator_Tuple2() {
        BiIterator<String, Integer> iter = sampleDataset.iterator("Name", "Age");
        assertTrue(iter.hasNext());
        Pair<String, Integer> p = iter.next();
        assertEquals("Alice", p.left());
        assertEquals(30, p.right());
        iter.next();
        iter.next();
        assertFalse(iter.hasNext());
    }

    @Test
    public void iterator_Tuple3() {
        TriIterator<Integer, String, Integer> iter = sampleDataset.iterator("ID", "Name", "Age");
        assertTrue(iter.hasNext());
        Triple<Integer, String, Integer> t = iter.next();
        assertEquals(1, t.left());
        assertEquals("Alice", t.middle());
        assertEquals(30, t.right());
    }

    @Test
    public void forEach_DisposableObjArray() {
        AtomicInteger sumOfIds = new AtomicInteger(0);
        sampleDataset.forEach(row -> sumOfIds.addAndGet((Integer) row.get(0)));
        assertEquals(1 + 2 + 3, sumOfIds.get());
    }

    @Test
    public void forEach_Tuple2() {
        List<String> combined = new ArrayList<>();
        sampleDataset.forEach(Tuple.of("Name", "Age"), (String name, Integer age) -> combined.add(name + ":" + age));
        assertEquals(Arrays.asList("Alice:30", "Bob:24", "Charlie:35"), combined);
    }

    @Test
    public void toList_defaultArray() {
        List<Object[]> list = sampleDataset.toList();
        assertEquals(3, list.size());
        assertArrayEquals(new Object[] { 1, "Alice", 30 }, list.get(0));
    }

    @Test
    public void toList_specificType_Map() {
        List<Map> mapList = sampleDataset.toList(Map.class);
        assertEquals(3, mapList.size());
        assertEquals("Alice", mapList.get(0).get("Name"));
    }

    @Test
    public void toList_specificType_Bean() {
        RowDataset beanFriendlyDs = new RowDataset(Arrays.asList("id", "name", "value"), Arrays.asList(new ArrayList<>(Arrays.asList(101, 102, 103)),
                new ArrayList<>(Arrays.asList("BeanA", "BeanB", "BeanC")), new ArrayList<>(Arrays.asList(1.1, 2.2, 3.3))));
        List<TestBean> beanList = beanFriendlyDs.toList(TestBean.class);
        assertEquals(3, beanList.size());
        assertEquals("BeanA", beanList.get(0).getName());
        assertEquals(2.2, beanList.get(1).getValue(), 0.001);
    }

    @Test
    public void toEntities_simpleBean() {
        RowDataset beanDs = new RowDataset(Arrays.asList("id", "name", "value"), Arrays.asList(new ArrayList<>(Arrays.asList(1, 2)),
                new ArrayList<>(Arrays.asList("obj1", "obj2")), new ArrayList<>(Arrays.asList(10.0, 20.0))));
        List<TestBean> entities = beanDs.toEntities(Collections.emptyMap(), TestBean.class);
        assertEquals(2, entities.size());
        assertEquals(new TestBean(1, "obj1", 10.0), entities.get(0));
        assertEquals(new TestBean(2, "obj2", 20.0), entities.get(1));
    }

    @Test
    public void toMergedEntities_singleId() {
        List<String> names = Arrays.asList("id", "name", "value", "detail");
        List<List<Object>> values = Arrays.asList(new ArrayList<>(Arrays.asList(1, 1, 2)), new ArrayList<>(Arrays.asList("A", "A", "B")),
                new ArrayList<>(Arrays.asList(10.0, 10.0, 20.0)), new ArrayList<>(Arrays.asList("d1", "d2", "d3")));
        RowDataset ds = new RowDataset(names, values);

        List<String> simpleNames = Arrays.asList("id", "name", "value");
        List<List<Object>> simpleValues = Arrays.asList(new ArrayList<>(Arrays.asList(1, 1, 2)), new ArrayList<>(Arrays.asList("Alice", "Alice", "Bob")),
                new ArrayList<>(Arrays.asList(10.0, 11.0, 20.0)));
        RowDataset simpleDs = new RowDataset(simpleNames, simpleValues);

        List<TestBean> merged = simpleDs.toMergedEntities("id", TestBean.class);
        assertEquals(2, merged.size());

        TestBean bean1 = merged.stream().filter(b -> b.getId() == 1).findFirst().orElse(null);
        TestBean bean2 = merged.stream().filter(b -> b.getId() == 2).findFirst().orElse(null);

        assertNotNull(bean1);
        assertEquals("Alice", bean1.getName());
        assertEquals(11.0, bean1.getValue(), 0.001);

        assertNotNull(bean2);
        assertEquals("Bob", bean2.getName());
        assertEquals(20.0, bean2.getValue(), 0.001);
    }

    @Test
    public void toMap_keyValue() {
        Map<Integer, String> idToNameMap = sampleDataset.toMap("ID", "Name");
        assertEquals(3, idToNameMap.size());
        assertEquals("Alice", idToNameMap.get(1));
        assertEquals("Bob", idToNameMap.get(2));
        assertEquals("Charlie", idToNameMap.get(3));
    }

    @Test
    public void toMap_keyRowAsBean() {
        RowDataset ds = new RowDataset(Arrays.asList("key", "id", "name", "value"), Arrays.asList(new ArrayList<>(Arrays.asList("k1", "k2")),
                new ArrayList<>(Arrays.asList(1, 2)), new ArrayList<>(Arrays.asList("Alice", "Bob")), new ArrayList<>(Arrays.asList(10.0, 20.0))));

        Map<String, TestBean> map = ds.toMap("key", Arrays.asList("id", "name", "value"), TestBean.class);
        assertEquals(2, map.size());
        assertEquals(new TestBean(1, "Alice", 10.0), map.get("k1"));
        assertEquals(new TestBean(2, "Bob", 20.0), map.get("k2"));
    }

    @Test
    public void toJson_writer() throws IOException {
        StringWriter sw = new StringWriter();
        sampleDataset.toJson(sw);
        String json = sw.toString();
        assertTrue(json.startsWith("["));
        assertTrue(json.endsWith("]"));
        assertTrue(json.contains("\"ID\":1"));
        assertTrue(json.contains("\"Name\":\"Alice\""));
        assertTrue(json.contains("\"Age\":30"));
    }

    @Test
    public void toJson_file(@TempDir File tempDir) throws IOException {
        File tempFile = new File(tempDir, "test.json");
        sampleDataset.toJson(tempFile);
        assertTrue(tempFile.exists());
        String jsonContent = Files.readString(tempFile.toPath());
        assertTrue(jsonContent.contains("\"Name\":\"Bob\""));

        IOUtil.deleteIfExists(tempFile);
    }

    @Test
    public void toJson_outputStream() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        sampleDataset.toJson(baos);
        String json = baos.toString();
        assertTrue(json.contains("\"Name\":\"Charlie\""));
    }

    @Test
    public void toXml_writer() throws IOException {
        StringWriter sw = new StringWriter();
        sampleDataset.toXml(sw);
        String xml = sw.toString();
        assertTrue(xml.startsWith("<dataset>"));
        assertTrue(xml.endsWith("</dataset>"));
        assertTrue(xml.contains("<row>"));
        assertTrue(xml.contains("<ID>1</ID>"));
        assertTrue(xml.contains("<Name>Alice</Name>"));
        assertTrue(xml.contains("<Age>30</Age>"));
    }

    @Test
    public void toCsv_writer() throws IOException {
        StringWriter sw = new StringWriter();
        sampleDataset.toCsv(sw);
        String csv = sw.toString();
        String[] lines = csv.split(IOUtil.LINE_SEPARATOR_UNIX);
        assertEquals("\"ID\",\"Name\",\"Age\"", lines[0]);
        assertEquals("1,\"Alice\",30", lines[1]);
    }

    @Test
    public void groupBy_singleKey_singleAggregate_collectorSum() {
        List<String> gNames = Arrays.asList("Category", "Value");
        List<List<Object>> gValues = Arrays.asList(new ArrayList<>(Arrays.asList("A", "B", "A", "B", "A")), new ArrayList<>(Arrays.asList(10, 20, 5, 15, 2)));
        RowDataset dsToGroup = new RowDataset(gNames, gValues);

        Dataset grouped = dsToGroup.groupBy("Category", "Value", "TotalValue", Collectors.summingInt(val -> (Integer) val));

        assertEquals(2, grouped.columnCount());
        assertEquals(2, grouped.size());

        grouped.moveToRow(0);
        assertEquals("A", grouped.get("Category"));
        assertEquals((Integer) (10 + 5 + 2), grouped.get("TotalValue"));

        grouped.moveToRow(1);
        assertEquals("B", grouped.get("Category"));
        assertEquals((Integer) (20 + 15), grouped.get("TotalValue"));
    }

    @Test
    public void groupBy_multipleKeys_collectorToList() {
        List<String> gNames = Arrays.asList("Group", "SubGroup", "Data");
        List<List<Object>> gValues = Arrays.asList(new ArrayList<>(Arrays.asList("G1", "G1", "G2", "G1")),
                new ArrayList<>(Arrays.asList("S1", "S2", "S1", "S1")), new ArrayList<>(Arrays.asList(10, 20, 30, 40)));
        RowDataset dsToGroup = new RowDataset(gNames, gValues);

        Dataset grouped = dsToGroup.groupBy(Arrays.asList("Group", "SubGroup"), "Data", "CollectedData", Collectors.toList());

        assertEquals(3, grouped.columnCount());
        assertEquals(3, grouped.size());

        Map<Tuple2<String, String>, List<Integer>> resultMap = new HashMap<>();
        for (int i = 0; i < grouped.size(); i++) {
            grouped.moveToRow(i);
            resultMap.put(Tuple.of(grouped.get("Group"), grouped.get("SubGroup")), (List<Integer>) grouped.get("CollectedData"));
        }

        assertEquals(Arrays.asList(10, 40), resultMap.get(Tuple.of("G1", "S1")));
        assertEquals(Collections.singletonList(20), resultMap.get(Tuple.of("G1", "S2")));
        assertEquals(Collections.singletonList(30), resultMap.get(Tuple.of("G2", "S1")));
    }

    @Test
    public void sortBy_singleColumn_defaultOrder() {
        sampleDataset.sortBy("Age");
        assertEquals((Integer) 2, sampleDataset.moveToRow(0).get("ID"));
        assertEquals((Integer) 1, sampleDataset.moveToRow(1).get("ID"));
        assertEquals((Integer) 3, sampleDataset.moveToRow(2).get("ID"));
    }

    @Test
    public void sortBy_singleColumn_customComparator() {
        sampleDataset.sortBy("Name", Comparator.reverseOrder());
        assertEquals("Charlie", sampleDataset.moveToRow(0).get("Name"));
        assertEquals("Bob", sampleDataset.moveToRow(1).get("Name"));
        assertEquals("Alice", sampleDataset.moveToRow(2).get("Name"));
    }

    @Test
    public void sortBy_multipleColumns_defaultComparator() {
        sampleDataset.addRow(new Object[] { 4, "Alice", 25 });
        sampleDataset.sortBy(Arrays.asList("Name", "Age"));
        assertEquals((Integer) 4, sampleDataset.moveToRow(0).get("ID"));
        assertEquals((Integer) 1, sampleDataset.moveToRow(1).get("ID"));
        assertEquals((Integer) 2, sampleDataset.moveToRow(2).get("ID"));
        assertEquals((Integer) 3, sampleDataset.moveToRow(3).get("ID"));
    }

    @Test
    public void distinct() {
        sampleDataset.addRow(new Object[] { 1, "Alice", 30 });
        assertEquals(4, sampleDataset.size());
        Dataset distinctDs = sampleDataset.distinct();
        assertEquals(3, distinctDs.size());
    }

    @Test
    public void distinctBy_singleColumn() {
        sampleDataset.addRow(new Object[] { 4, "Alice", 28 });
        Dataset distinctByName = sampleDataset.distinctBy("Name");
        assertEquals(3, distinctByName.size());
        List<Object> names = distinctByName.getColumn("Name");
        assertTrue(names.contains("Alice") && names.contains("Bob") && names.contains("Charlie"));
    }

    @Test
    public void filter_byPredicateOnDisposableObjArray() {
        Dataset filtered = sampleDataset.filter(row -> (Integer) row.get(2) > 30);
        assertEquals(1, filtered.size());
        assertEquals("Charlie", filtered.getRow(0, Map.class).get("Name"));
    }

    @Test
    public void filter_byPredicateOnSingleColumn() {
        Dataset filtered = sampleDataset.filter("Name", (String name) -> name.startsWith("A"));
        assertEquals(1, filtered.size());
        assertEquals("Alice", filtered.getRow(0, Map.class).get("Name"));
    }

    @Test
    public void map_singleColumnToNew() {
        Dataset mapped = sampleDataset.mapColumn("Age", "AgeInMonths", "ID", (Integer age) -> age * 12);
        assertEquals(2, mapped.columnCount());
        assertTrue(mapped.columnNames().contains("AgeInMonths"));
        assertTrue(mapped.columnNames().contains("ID"));
        assertEquals(30 * 12, mapped.getRow(0, Map.class).get("AgeInMonths"));
    }

    @Test
    public void flatMap_singleColumnToNewCollection() {
        sampleDataset.addColumn("Hobbies", Arrays.asList("Reading,Hiking", "Gaming", "Cooking,Swimming"));
        Dataset flatMapped = sampleDataset.flatMapColumn("Hobbies", "Hobby", "Name", (String hobbies) -> Arrays.asList(hobbies.split(",")));

        assertEquals(2, flatMapped.columnCount());
        assertEquals(5, flatMapped.size());
        assertEquals("Reading", flatMapped.moveToRow(0).get("Hobby"));
        assertEquals("Alice", flatMapped.moveToRow(0).get("Name"));
        assertEquals("Gaming", flatMapped.moveToRow(2).get("Hobby"));
        assertEquals("Bob", flatMapped.moveToRow(2).get("Name"));
    }

    @Test
    public void copy_full() {
        Dataset copied = sampleDataset.copy();
        assertNotSame(sampleDataset, copied);
        assertEquals(sampleDataset, copied);
        copied.set(0, 0, 100);
        assertEquals((Integer) 1, sampleDataset.moveToRow(0).get(0));
    }

    @Test
    public void copy_selectedColumns() {
        Dataset copied = sampleDataset.copy(Arrays.asList("Name", "Age"));
        assertEquals(2, copied.columnCount());
        assertTrue(copied.columnNames().contains("Name"));
        assertTrue(copied.columnNames().contains("Age"));
        assertEquals("Alice", copied.get(0, 0));
    }

    @Test
    public void copy_range() {
        Dataset copied = sampleDataset.copy(1, 3);
        assertEquals(2, copied.size());
        assertEquals("Bob", copied.moveToRow(0).get("Name"));
        assertEquals("Charlie", copied.moveToRow(1).get("Name"));
    }

    @Test
    public void clone_method() {

        Dataset copy = sampleDataset.clone();
        assertFalse(copy == sampleDataset);
        assertEquals(sampleDataset, copy);

    }

    @Test
    public void split_intoChunks() {
        Stream<Dataset> stream = sampleDataset.split(2);
        List<Dataset> chunks = stream.toList();
        assertEquals(2, chunks.size());
        assertEquals(2, chunks.get(0).size());
        assertEquals(1, chunks.get(1).size());
        assertEquals("Alice", chunks.get(0).moveToRow(0).get("Name"));
        assertEquals("Charlie", chunks.get(1).moveToRow(0).get("Name"));
    }

    @Test
    public void slice_dataset() {
        Dataset sliced = sampleDataset.slice(1, 2, Arrays.asList("Name"));
        assertEquals(1, sliced.size());
        assertEquals(1, sliced.columnCount());
        assertEquals("Bob", sliced.get(0, 0));
        assertTrue(sliced.isFrozen());
    }

    @Test
    public void stream_singleColumn() {
        List<String> names = sampleDataset.stream("Name").map(s -> ((String) s).toUpperCase()).toList();
        assertEquals(Arrays.asList("ALICE", "BOB", "CHARLIE"), names);
    }

    @Test
    public void stream_asBean() {
        RowDataset beanDs = new RowDataset(Arrays.asList("id", "name", "value"), Arrays.asList(new ArrayList<>(Arrays.asList(1, 2)),
                new ArrayList<>(Arrays.asList("obj1", "obj2")), new ArrayList<>(Arrays.asList(10.0, 20.0))));
        List<Integer> ids = beanDs.stream(TestBean.class).map(TestBean::getId).toList();
        assertEquals(Arrays.asList(1, 2), ids);
    }

    @Test
    public void apply_function() {
        Integer totalAge = sampleDataset.apply(ds -> {
            int sum = 0;
            for (int i = 0; i < ds.size(); i++) {
                sum += (Integer) ds.moveToRow(i).get("Age");
            }
            return sum;
        });
        assertEquals(30 + 24 + 35, totalAge.intValue());
    }

    @Test
    public void accept_consumer() {
        AtomicInteger count = new AtomicInteger(0);
        sampleDataset.accept(ds -> {
            count.set(ds.size());
        });
        assertEquals(3, count.get());
    }

    @Test
    public void freeze_and_isFrozen() {
        assertFalse(sampleDataset.isFrozen());
        sampleDataset.freeze();
        assertTrue(sampleDataset.isFrozen());
        assertThrows(IllegalStateException.class, () -> sampleDataset.set(0, 0, 100));
    }

    @Test
    public void isEmpty_size_clear() {
        assertFalse(sampleDataset.isEmpty());
        assertEquals(3, sampleDataset.size());

        sampleDataset.clear();
        assertTrue(sampleDataset.isEmpty());
        assertEquals(0, sampleDataset.size());
        assertEquals(3, sampleDataset.columnCount());
        assertTrue(sampleDataset.getColumn(0).isEmpty());

        RowDataset frozenDs = createSimpleDataset();
        frozenDs.freeze();
        assertThrows(IllegalStateException.class, frozenDs::clear);
    }

    @Test
    public void properties_access() {
        assertTrue(sampleDataset.getProperties().isEmpty());
        Map<String, Object> props = new HashMap<>();
        props.put("version", 1.2);
        RowDataset dsWithProps = new RowDataset(columnNames, columnValues, props);
        assertEquals(1.2, (Double) dsWithProps.getProperties().get("version"), 0.001);
        assertThrows(UnsupportedOperationException.class, () -> dsWithProps.getProperties().put("newKey", "newVal"));
    }

    @Test
    public void columns_streamOfImmutableLists() {
        List<ArrayList<Object>> streamedCols = sampleDataset.columns().map(ArrayList::new).toList();
        assertEquals(columnValues, streamedCols);
    }

    @Test
    public void columnMap() {
        Map<String, com.landawn.abacus.util.ImmutableList<Object>> map = sampleDataset.columnMap();
        assertEquals(3, map.size());
        assertEquals(sampleDataset.getColumn("ID"), map.get("ID"));
        assertEquals(sampleDataset.getColumn("Name"), map.get("Name"));
    }

    @Test
    public void println_toWriter() throws IOException {
        StringWriter stringWriter = new StringWriter();
        sampleDataset.println(stringWriter);
        String output = stringWriter.toString();

        assertTrue(output.contains("+----+---------+-----+"));
        assertTrue(output.contains("| ID | Name    | Age |"));
        assertTrue(output.contains("| 1  | Alice   | 30  |"));
        assertTrue(output.contains("| 2  | Bob     | 24  |"));
        assertTrue(output.contains("| 3  | Charlie | 35  |"));
    }

    @Test
    public void hashCode_equals_toString() {
        RowDataset ds1 = createSimpleDataset();
        RowDataset ds2 = createSimpleDataset();
        RowDataset ds3 = new RowDataset(Arrays.asList("ID", "Name", "Value"), Arrays.asList(new ArrayList<>(Arrays.asList(1, 2, 3)),
                new ArrayList<>(Arrays.asList("Alice", "Bob", "Charlie")), new ArrayList<>(Arrays.asList(30.0, 24.0, 35.0))));
        RowDataset ds4 = new RowDataset(columnNames, Arrays.asList(new ArrayList<>(Arrays.asList(1, 2, 99)),
                new ArrayList<>(Arrays.asList("Alice", "Bob", "David")), new ArrayList<>(Arrays.asList(30, 24, 40))));

        assertEquals(ds1.hashCode(), ds2.hashCode());
        assertTrue(ds1.equals(ds2));
        assertTrue(ds2.equals(ds1));

        assertFalse(ds1.equals(ds3));
        assertFalse(ds1.equals(ds4));
        assertFalse(ds1.equals(null));
        assertFalse(ds1.equals("a string"));

        assertNotNull(ds1.toString());
        assertTrue(ds1.toString().contains("columnNames=[ID, Name, Age]"));
    }

    @Test
    public void innerJoin_singleColumn() {
        List<String> orderCN = Arrays.asList("OrderID", "ID", "Product");
        List<List<Object>> orderCV = Arrays.asList(new ArrayList<>(Arrays.asList(101, 102, 103, 104)), new ArrayList<>(Arrays.asList(1, 1, 2, 4)),
                new ArrayList<>(Arrays.asList("Book", "Pen", "Paper", "Clip")));
        RowDataset orders = new RowDataset(orderCN, orderCV);

        Dataset joined = sampleDataset.innerJoin(orders, "ID", "ID");
        assertEquals(3, joined.size());
        assertEquals(sampleDataset.columnCount() + orders.columnCount(), joined.columnCount());
        assertTrue(joined.columnNames().contains("ID_2"));

        joined.moveToRow(0);
        assertEquals((Integer) 1, joined.get("ID"));
        assertEquals("Alice", joined.get("Name"));
        assertEquals((Integer) 101, joined.get("OrderID"));
        assertEquals("Book", joined.get("Product"));
    }

    @Test
    public void leftJoin_singleColumn() {
        List<String> orderCN = Arrays.asList("OrderID", "ID", "Product");
        List<List<Object>> orderCV = Arrays.asList(new ArrayList<>(Arrays.asList(101, 102)), new ArrayList<>(Arrays.asList(1, 1)),
                new ArrayList<>(Arrays.asList("Book", "Pen")));
        RowDataset orders = new RowDataset(orderCN, orderCV);

        Dataset joined = sampleDataset.leftJoin(orders, "ID", "ID");
        assertEquals(4, joined.size());

        joined.moveToRow(2);
        assertEquals((Integer) 2, joined.get("ID"));
        assertNull(joined.get("OrderID"));
    }

    @Test
    public void union_datasets() {
        List<String> otherNames = Arrays.asList("ID", "Name", "Salary");
        List<List<Object>> otherValues = Arrays.asList(new ArrayList<>(Arrays.asList(3, 4)), new ArrayList<>(Arrays.asList("Charlie", "David")),
                new ArrayList<>(Arrays.asList(70000, 80000)));
        RowDataset otherDs = new RowDataset(otherNames, otherValues);

        Dataset unionResult = sampleDataset.union(otherDs);
        assertEquals(4, unionResult.columnCount());
        assertEquals(4, unionResult.size());

        sampleDataset.println();
        otherDs.println();
        unionResult.println();

        Map<Integer, Map<String, Object>> resultMap = new HashMap<>();
        for (int i = 0; i < unionResult.size(); i++) {
            resultMap.put((Integer) unionResult.moveToRow(i).get("ID"), unionResult.getRow(i, Map.class));
        }

        assertEquals("Alice", resultMap.get(1).get("Name"));
        assertEquals(30, resultMap.get(1).get("Age"));
        assertNull(resultMap.get(1).get("Salary"));

        assertEquals("Charlie", resultMap.get(3).get("Name"));
        assertEquals(35, resultMap.get(3).get("Age"));
        assertNull(resultMap.get(3).get("Salary"));

        assertEquals("David", resultMap.get(4).get("Name"));
        assertNull(resultMap.get(4).get("Age"));
        assertEquals(80000, resultMap.get(4).get("Salary"));
    }

    @Test
    public void intersect_datasets() {
        List<String> otherNames = Arrays.asList("ID", "Name", "Age");
        List<List<Object>> otherValues = Arrays.asList(new ArrayList<>(Arrays.asList(2, 3, 4)), new ArrayList<>(Arrays.asList("Bob", "Charlie", "David")),
                new ArrayList<>(Arrays.asList(24, 35, 28)));
        RowDataset otherDs = new RowDataset(otherNames, otherValues);

        Dataset intersectResult = sampleDataset.intersect(otherDs);
        assertEquals(2, intersectResult.size());
        List<String> names = intersectResult.toList(Map.class).stream().map(m -> (String) m.get("Name")).sorted().toList();
        assertEquals(Arrays.asList("Bob", "Charlie"), names);
    }

    @Test
    public void except_datasets() {
        List<String> otherNames = Arrays.asList("ID", "Name", "Age");
        List<List<Object>> otherValues = Arrays.asList(new ArrayList<>(Arrays.asList(2, 3)), new ArrayList<>(Arrays.asList("Bob", "Charlie")),
                new ArrayList<>(Arrays.asList(24, 35)));
        RowDataset otherDs = new RowDataset(otherNames, otherValues);

        Dataset exceptResult = sampleDataset.except(otherDs);
        assertEquals(1, exceptResult.size());
        assertEquals("Alice", exceptResult.moveToRow(0).get("Name"));
    }

    public static class TestBean {
        private int id;
        private String name;
        private double value;
        private TestNestedBean nested;
        private List<TestNestedBean> nestedList;

        public TestBean() {
        }

        public TestBean(int id, String name, double value) {
            this.id = id;
            this.name = name;
            this.value = value;
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

        public double getValue() {
            return value;
        }

        public void setValue(double value) {
            this.value = value;
        }

        public TestNestedBean getNested() {
            return nested;
        }

        public void setNested(TestNestedBean nested) {
            this.nested = nested;
        }

        public List<TestNestedBean> getNestedList() {
            return nestedList;
        }

        public void setNestedList(List<TestNestedBean> nestedList) {
            this.nestedList = nestedList;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (o == null || getClass() != o.getClass())
                return false;
            TestBean testBean = (TestBean) o;
            return id == testBean.id && Double.compare(testBean.value, value) == 0 && Objects.equals(name, testBean.name)
                    && Objects.equals(nested, testBean.nested) && Objects.equals(nestedList, testBean.nestedList);
        }

        @Override
        public int hashCode() {
            return Objects.hash(id, name, value, nested, nestedList);
        }

        @Override
        public String toString() {
            return "TestBean{id=" + id + ", name='" + name + '\'' + ", value=" + value + ", nested=" + nested + ", nestedList=" + nestedList + "}";
        }
    }

    public static class TestNestedBean {
        private String detail;

        public TestNestedBean() {
        }

        public TestNestedBean(String detail) {
            this.detail = detail;
        }

        public String getDetail() {
            return detail;
        }

        public void setDetail(String detail) {
            this.detail = detail;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (o == null || getClass() != o.getClass())
                return false;
            TestNestedBean that = (TestNestedBean) o;
            return Objects.equals(detail, that.detail);
        }

        @Override
        public int hashCode() {
            return Objects.hash(detail);
        }

        @Override
        public String toString() {
            return "TestNestedBean{detail='" + detail + "'}";
        }
    }

    private RowDataset createSimpleDataset() {
        List<String> names = new ArrayList<>(Arrays.asList("ID", "Name", "Age"));
        List<List<Object>> values = new ArrayList<>();
        values.add(new ArrayList<>(Arrays.asList(1, 2, 3)));
        values.add(new ArrayList<>(Arrays.asList("Alice", "Bob", "Charlie")));
        values.add(new ArrayList<>(Arrays.asList(30, 24, 35)));
        return new RowDataset(names, values);
    }
}

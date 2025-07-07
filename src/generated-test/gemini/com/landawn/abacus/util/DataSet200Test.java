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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.NoCachingNoUpdating.DisposableObjArray;
import com.landawn.abacus.util.Tuple.Tuple2;
import com.landawn.abacus.util.stream.Stream;

@SuppressWarnings({ "unchecked", "rawtypes", "deprecation" })
public class DataSet200Test extends TestBase {

    private RowDataSet emptyDataSet;
    private RowDataSet sampleDataSet;
    private List<String> columnNames;
    private List<List<Object>> columnValues;

    @BeforeEach
    public void setUp() {
        emptyDataSet = new RowDataSet(new ArrayList<>(), new ArrayList<>());

        columnNames = new ArrayList<>(Arrays.asList("ID", "Name", "Age"));
        columnValues = new ArrayList<>();
        columnValues.add(new ArrayList<>(Arrays.asList(1, 2, 3))); // ID
        columnValues.add(new ArrayList<>(Arrays.asList("Alice", "Bob", "Charlie"))); // Name
        columnValues.add(new ArrayList<>(Arrays.asList(30, 24, 35))); // Age
        sampleDataSet = new RowDataSet(columnNames, columnValues);
    }

    private RowDataSet createCustomDataSet(List<String> names, List<List<Object>> values) {
        return new RowDataSet(new ArrayList<>(names), new ArrayList<>(values.stream().map(ArrayList::new).collect(Collectors.toList())));
    }

    @Test
    public void constructor_emptyLists() {
        RowDataSet ds = new RowDataSet(Collections.emptyList(), Collections.emptyList());
        assertNotNull(ds);
        assertTrue(ds.isEmpty());
        assertEquals(0, ds.columnCount());
    }

    @Test
    public void constructor_withData() {
        assertEquals(3, sampleDataSet.columnCount());
        assertEquals(3, sampleDataSet.size());
        assertFalse(sampleDataSet.isFrozen());
        assertEquals(columnNames, sampleDataSet.columnNameList());
    }

    @Test
    public void constructor_withProperties() {
        Map<String, Object> props = new HashMap<>();
        props.put("key1", "value1");
        RowDataSet ds = new RowDataSet(columnNames, columnValues, props);
        assertEquals("value1", ds.properties().get("key1"));
    }

    @Test
    public void constructor_mismatchedColumnNameAndListSize_throwsIllegalArgumentException() {
        List<String> names = Arrays.asList("A", "B");
        List<List<Object>> values = List.of(Arrays.asList(1, 2)); // Only one column of data
        assertThrows(IllegalArgumentException.class, () -> new RowDataSet(names, values));
    }

    @Test
    public void constructor_mismatchedColumnSizes_throwsIllegalArgumentException() {
        List<String> names = Arrays.asList("A", "B");
        List<List<Object>> values = List.of(Arrays.asList(1, 2), Arrays.asList(3)); // Col B has fewer rows
        assertThrows(IllegalArgumentException.class, () -> new RowDataSet(names, values));
    }

    @Test
    public void constructor_duplicateColumnNames_throwsIllegalArgumentException() {
        List<String> duplicateNames = Arrays.asList("ID", "Name", "ID");
        assertThrows(IllegalArgumentException.class, () -> new RowDataSet(duplicateNames, columnValues));
    }

    @Test
    public void columnNameList() {
        assertEquals(columnNames, sampleDataSet.columnNameList());
        assertTrue(emptyDataSet.columnNameList().isEmpty());
    }

    @Test
    public void columnCount() {
        assertEquals(3, sampleDataSet.columnCount());
        assertEquals(0, emptyDataSet.columnCount());
    }

    @Test
    public void getColumnName() {
        assertEquals("ID", sampleDataSet.getColumnName(0));
        assertEquals("Name", sampleDataSet.getColumnName(1));
        assertEquals("Age", sampleDataSet.getColumnName(2));
        assertThrows(IndexOutOfBoundsException.class, () -> sampleDataSet.getColumnName(3));
        assertThrows(IndexOutOfBoundsException.class, () -> emptyDataSet.getColumnName(0));
    }

    @Test
    public void getColumnIndex() {
        assertEquals(0, sampleDataSet.getColumnIndex("ID"));
        assertEquals(1, sampleDataSet.getColumnIndex("Name"));
        assertEquals(2, sampleDataSet.getColumnIndex("Age"));
        assertThrows(IllegalArgumentException.class, () -> sampleDataSet.getColumnIndex("NonExistent"));
        assertThrows(IllegalArgumentException.class, () -> emptyDataSet.getColumnIndex("Any"));
    }

    @Test
    public void getColumnIndexes() {
        Collection<String> namesToGet = Arrays.asList("Name", "ID");
        int[] indexes = sampleDataSet.getColumnIndexes(namesToGet);
        assertArrayEquals(new int[] { 1, 0 }, indexes);

        Collection<String> allNames = sampleDataSet.columnNameList();
        int[] allIndexes = sampleDataSet.getColumnIndexes(allNames);
        assertArrayEquals(new int[] { 0, 1, 2 }, allIndexes); // Should use cached _columnIndexes

        assertThrows(IllegalArgumentException.class, () -> sampleDataSet.getColumnIndexes(Arrays.asList("ID", "NonExistent")));
        assertArrayEquals(N.EMPTY_INT_ARRAY, sampleDataSet.getColumnIndexes(Collections.emptyList()));
    }

    @Test
    public void containsColumn() {
        assertTrue(sampleDataSet.containsColumn("ID"));
        assertFalse(sampleDataSet.containsColumn("NonExistent"));
        assertFalse(emptyDataSet.containsColumn("Any"));
    }

    @Test
    public void containsAllColumns() {
        assertTrue(sampleDataSet.containsAllColumns(Arrays.asList("ID", "Age")));
        assertFalse(sampleDataSet.containsAllColumns(Arrays.asList("ID", "NonExistent")));
        assertTrue(sampleDataSet.containsAllColumns(Collections.emptyList())); // Empty set should be true
        assertTrue(emptyDataSet.containsAllColumns(Collections.emptyList()));
        assertFalse(emptyDataSet.containsAllColumns(Arrays.asList("ID")));
    }

    @Test
    public void renameColumn() {
        sampleDataSet.renameColumn("Age", "Years");
        assertEquals(2, sampleDataSet.getColumnIndex("Years"));
        assertTrue(sampleDataSet.columnNameList().contains("Years"));
        assertFalse(sampleDataSet.columnNameList().contains("Age"));
        assertThrows(IllegalArgumentException.class, () -> sampleDataSet.renameColumn("NonExistent", "NewName"));
        assertThrows(IllegalArgumentException.class, () -> sampleDataSet.renameColumn("ID", "Name")); // Duplicate

        sampleDataSet.renameColumn("Years", "Years"); // Rename to self
        assertEquals(2, sampleDataSet.getColumnIndex("Years"));

        RowDataSet ds = createSimpleDataSet();
        ds.freeze();
        assertThrows(IllegalStateException.class, () -> ds.renameColumn("ID", "NewID"));
    }

    @Test
    public void renameColumns_map() {
        Map<String, String> renames = new HashMap<>();
        renames.put("ID", "Identifier");
        renames.put("Age", "Years");
        sampleDataSet.renameColumns(renames);
        assertTrue(sampleDataSet.columnNameList().contains("Identifier"));
        assertTrue(sampleDataSet.columnNameList().contains("Years"));
        assertFalse(sampleDataSet.columnNameList().contains("ID"));
        assertFalse(sampleDataSet.columnNameList().contains("Age"));

        Map<String, String> invalidRenames = new HashMap<>();
        invalidRenames.put("NonExistent", "NewName");
        assertThrows(IllegalArgumentException.class, () -> sampleDataSet.renameColumns(invalidRenames));

        Map<String, String> duplicateNewNames = new HashMap<>();
        duplicateNewNames.put("Name", "Duplicate");
        duplicateNewNames.put("Years", "Duplicate"); // Years was Age
        assertThrows(IllegalArgumentException.class, () -> sampleDataSet.renameColumns(duplicateNewNames));
    }

    @Test
    public void renameColumns_collectionAndFunction() {
        sampleDataSet.renameColumns(Arrays.asList("ID", "Age"), name -> name + "_new");
        assertTrue(sampleDataSet.columnNameList().contains("ID_new"));
        assertTrue(sampleDataSet.columnNameList().contains("Age_new"));
        assertFalse(sampleDataSet.columnNameList().contains("ID"));
        assertFalse(sampleDataSet.columnNameList().contains("Age"));

        assertThrows(IllegalArgumentException.class, () -> sampleDataSet.renameColumns(Arrays.asList("Name", "NonExistent"), String::toUpperCase));
    }

    @Test
    public void renameColumns_functionForAll() {
        sampleDataSet.renameColumns(name -> "col_" + name);
        assertTrue(sampleDataSet.columnNameList().contains("col_ID"));
        assertTrue(sampleDataSet.columnNameList().contains("col_Name"));
        assertTrue(sampleDataSet.columnNameList().contains("col_Age"));
    }

    @Test
    public void moveColumn() {
        sampleDataSet.moveColumn("Age", 0); // Move Age to the first position
        assertEquals(Arrays.asList("Age", "ID", "Name"), sampleDataSet.columnNameList());
        assertEquals((Integer) 35, sampleDataSet.absolute(2).get("Age")); // Charlie's age

        assertThrows(IndexOutOfBoundsException.class, () -> sampleDataSet.moveColumn("ID", 5)); // Invalid new position
        assertThrows(IllegalArgumentException.class, () -> sampleDataSet.moveColumn("NonExistent", 0));
    }

    @Test
    public void moveColumns_map() {
        Map<String, Integer> moves = new HashMap<>();
        moves.put("Age", 0); // Age to first
        moves.put("ID", 2); // ID to last (after Age is moved)
        sampleDataSet.moveColumns(moves);
        assertEquals(Arrays.asList("Age", "Name", "ID"), sampleDataSet.columnNameList());
    }

    @Test
    public void swapColumnPosition() {
        sampleDataSet.swapColumnPosition("ID", "Age");
        assertEquals(Arrays.asList("Age", "Name", "ID"), sampleDataSet.columnNameList());
        assertEquals((Integer) 30, sampleDataSet.absolute(0).get("Age")); // Alice's ID is now in Age column
        assertEquals((Integer) 1, sampleDataSet.absolute(0).get("ID")); // Alice's Age is now in ID column

        sampleDataSet.swapColumnPosition("Name", "Name"); // Swap with self
        assertEquals(Arrays.asList("Age", "Name", "ID"), sampleDataSet.columnNameList());

        assertThrows(IllegalArgumentException.class, () -> sampleDataSet.swapColumnPosition("ID", "NonExistent"));
    }

    @Test
    public void get_rowIndex_columnIndex() {
        assertEquals((Integer) 1, sampleDataSet.absolute(0).get(0));
        assertEquals("Bob", sampleDataSet.absolute(1).get(1));
        assertEquals((Integer) 35, sampleDataSet.absolute(2).get(2));
        assertThrows(IndexOutOfBoundsException.class, () -> sampleDataSet.absolute(3).get(0));
        assertThrows(IndexOutOfBoundsException.class, () -> sampleDataSet.absolute(0).get(3));
    }

    @Test
    public void set_rowIndex_columnIndex_value() {
        sampleDataSet.set(0, 0, 100);
        assertEquals((Integer) 100, sampleDataSet.absolute(0).get(0));

        RowDataSet ds = createSimpleDataSet();
        ds.freeze();
        assertThrows(IllegalStateException.class, () -> ds.set(0, 0, 99));
    }

    @Test
    public void isNull_rowIndex_columnIndex() {
        assertFalse(sampleDataSet.isNull(0, 0));
        sampleDataSet.set(0, 0, null);
        assertTrue(sampleDataSet.isNull(0, 0));
    }

    // Test for get(columnIndex) - relies on currentRowNum
    @Test
    public void get_columnIndex_withAbsolute() {
        sampleDataSet.absolute(1); // Bob's row
        assertEquals((Integer) 2, sampleDataSet.get(0)); // Bob's ID
        assertEquals("Bob", sampleDataSet.get(1));
        assertEquals((Integer) 24, sampleDataSet.get(2));
    }

    // Test for get(columnName) - relies on currentRowNum
    @Test
    public void get_columnName_withAbsolute() {
        sampleDataSet.absolute(2); // Charlie's row
        assertEquals((Integer) 3, sampleDataSet.get("ID"));
        assertEquals("Charlie", sampleDataSet.get("Name"));
        assertEquals((Integer) 35, sampleDataSet.get("Age"));
    }

    @Test
    public void getBoolean_primitiveAndObject() {
        List<String> boolNames = List.of("BoolCol");
        List<List<Object>> boolVals = List.of(new ArrayList<>(Arrays.asList(true, false, null, Boolean.TRUE, Boolean.FALSE)));
        RowDataSet boolDs = new RowDataSet(boolNames, boolVals);

        boolDs.absolute(0);
        assertTrue(boolDs.getBoolean(0));
        assertTrue(boolDs.getBoolean("BoolCol"));

        boolDs.absolute(1);
        assertFalse(boolDs.getBoolean(0));

        boolDs.absolute(2); // null
        assertFalse(boolDs.getBoolean(0)); // null becomes false for primitive

        boolDs.absolute(3);
        assertTrue(boolDs.getBoolean(0));

        boolDs.absolute(4);
        assertFalse(boolDs.getBoolean(0));
    }

    @Test
    public void getChar_primitiveAndObject() {
        List<String> charNames = List.of("CharCol");
        List<List<Object>> charVals = List.of(new ArrayList<>(Arrays.asList('a', 'Z', null, Character.valueOf('x'))));
        RowDataSet charDs = new RowDataSet(charNames, charVals);

        charDs.absolute(0);
        assertEquals('a', charDs.getChar(0));
        assertEquals('a', charDs.getChar("CharCol"));

        charDs.absolute(1);
        assertEquals('Z', charDs.getChar(0));

        charDs.absolute(2); // null
        assertEquals((char) 0, charDs.getChar(0)); // null becomes (char)0 for primitive

        charDs.absolute(3);
        assertEquals('x', charDs.getChar(0));
    }

    // Similar tests for getByte, getShort, getInt, getLong, getFloat, getDouble
    @Test
    public void getInt_primitiveAndObject() {
        List<String> intNames = List.of("IntCol");
        List<List<Object>> intVals = List.of(new ArrayList<>(Arrays.asList(10, -5, null, Integer.valueOf(100), Long.valueOf(200L))));
        RowDataSet intDs = new RowDataSet(intNames, intVals);

        intDs.absolute(0);
        assertEquals(10, intDs.getInt(0));
        assertEquals(10, intDs.getInt("IntCol"));

        intDs.absolute(1);
        assertEquals(-5, intDs.getInt(0));

        intDs.absolute(2); // null
        assertEquals(0, intDs.getInt(0)); // null becomes 0 for primitive

        intDs.absolute(3);
        assertEquals(100, intDs.getInt(0));

        intDs.absolute(4); // Long value
        assertEquals(200, intDs.getInt(0));
    }

    @Test
    public void isNull_columnIndex_withAbsolute() {
        sampleDataSet.absolute(0);
        assertFalse(sampleDataSet.isNull(0));
        sampleDataSet.set(0, null); // set ID of current row (Alice) to null
        assertTrue(sampleDataSet.isNull(0));
        assertTrue(sampleDataSet.isNull("ID"));
    }

    @Test
    public void set_columnIndex_value_withAbsolute() {
        sampleDataSet.absolute(1); // Bob's row
        sampleDataSet.set(0, 200); // Set Bob's ID
        assertEquals((Integer) 200, sampleDataSet.absolute(1).get(0)); // Verify Bob's ID in the underlying list
    }

    @Test
    public void set_columnName_value_withAbsolute() {
        sampleDataSet.absolute(2); // Charlie's row
        sampleDataSet.set("Age", 40);
        assertEquals((Integer) 40, sampleDataSet.absolute(2).get(2)); // Verify Charlie's Age
    }

    @Test
    public void getColumn_byIndex() {
        List<Object> idColumn = sampleDataSet.getColumn(0);
        assertEquals(Arrays.asList(1, 2, 3), idColumn);
        // Check immutability
        assertThrows(UnsupportedOperationException.class, () -> sampleDataSet.getColumn(0).add(4));
    }

    @Test
    public void getColumn_byName() {
        List<Object> nameColumn = sampleDataSet.getColumn("Name");
        assertEquals(Arrays.asList("Alice", "Bob", "Charlie"), nameColumn);
    }

    @Test
    public void copyColumn() {
        List<Object> ageColumnCopy = sampleDataSet.copyColumn("Age");
        assertEquals(Arrays.asList(30, 24, 35), ageColumnCopy);
        ageColumnCopy.add(40); // Should not affect original
        assertEquals(Arrays.asList(30, 24, 35), sampleDataSet.getColumn("Age"));
    }

    @Test
    public void addColumn_newColumnName_collection() {
        List<String> newColData = Arrays.asList("X", "Y", "Z");
        sampleDataSet.addColumn("Grade", newColData);
        assertEquals(4, sampleDataSet.columnCount());
        assertTrue(sampleDataSet.columnNameList().contains("Grade"));
        assertEquals(newColData, sampleDataSet.getColumn("Grade"));
        assertEquals("X", sampleDataSet.absolute(0).get("Grade"));

        // Test adding null column
        sampleDataSet.addColumn("EmptyGrade", null);
        assertEquals(5, sampleDataSet.columnCount());
        assertEquals(Arrays.asList(null, null, null), sampleDataSet.getColumn("EmptyGrade"));

        assertThrows(IllegalArgumentException.class, () -> sampleDataSet.addColumn("ID", Arrays.asList("A"))); // duplicate name
        assertThrows(IllegalArgumentException.class, () -> sampleDataSet.addColumn("Score", Arrays.asList(10, 20))); // wrong size
    }

    @Test
    public void addColumn_newColumnPosition_newColumnName_collection() {
        List<Double> scores = Arrays.asList(90.5, 88.0, 92.0);
        sampleDataSet.addColumn(1, "Score", scores); // Add at index 1
        assertEquals(4, sampleDataSet.columnCount());
        assertEquals(Arrays.asList("ID", "Score", "Name", "Age"), sampleDataSet.columnNameList());
        assertEquals(scores, sampleDataSet.getColumn("Score"));
        assertEquals(90.5, sampleDataSet.absolute(0).get("Score"));

        assertThrows(IllegalArgumentException.class, () -> sampleDataSet.addColumn(0, "ID", scores)); // duplicate name
    }

    @Test
    public void addColumn_fromAnotherColumn_withFunction() {
        sampleDataSet.addColumn("AgePlus5", "Age", (Integer age) -> age + 5);
        assertEquals(4, sampleDataSet.columnCount());
        assertTrue(sampleDataSet.columnNameList().contains("AgePlus5"));
        assertEquals(Arrays.asList(35, 29, 40), sampleDataSet.getColumn("AgePlus5"));
    }

    @Test
    public void addColumn_fromMultipleColumns_withFunction() {
        sampleDataSet.addColumn("ID_Name", Arrays.asList("ID", "Name"), (DisposableObjArray row) -> row.get(0) + "_" + row.get(1));
        assertEquals(4, sampleDataSet.columnCount());
        assertTrue(sampleDataSet.columnNameList().contains("ID_Name"));
        assertEquals(Arrays.asList("1_Alice", "2_Bob", "3_Charlie"), sampleDataSet.getColumn("ID_Name"));
    }

    @Test
    public void addColumn_fromTuple2_withBiFunction() {
        sampleDataSet.addColumn("Name_Age_Str", Tuple.of("Name", "Age"), (String name, Integer age) -> name + ":" + age);
        assertEquals(4, sampleDataSet.columnCount());
        assertTrue(sampleDataSet.columnNameList().contains("Name_Age_Str"));
        assertEquals(Arrays.asList("Alice:30", "Bob:24", "Charlie:35"), sampleDataSet.getColumn("Name_Age_Str"));
    }

    @Test
    public void addColumn_fromTuple3_withTriFunction() {
        List<String> t3ColNames = new ArrayList<>(Arrays.asList("C1", "C2", "C3", "C4"));
        List<List<Object>> t3ColValues = new ArrayList<>();
        t3ColValues.add(new ArrayList<>(Arrays.asList(1, 2)));
        t3ColValues.add(new ArrayList<>(Arrays.asList("A", "B")));
        t3ColValues.add(new ArrayList<>(Arrays.asList(true, false)));
        t3ColValues.add(new ArrayList<>(Arrays.asList(1.1, 2.2)));
        RowDataSet t3ds = new RowDataSet(t3ColNames, t3ColValues);

        t3ds.addColumn("Combined", Tuple.of("C1", "C2", "C3"), (Integer c1, String c2, Boolean c3) -> c1 + "_" + c2 + "_" + c3);
        assertEquals(5, t3ds.columnCount());
        assertEquals(Arrays.asList("1_A_true", "2_B_false"), t3ds.getColumn("Combined"));
    }

    @Test
    public void removeColumn() {
        List<Object> removedCol = sampleDataSet.removeColumn("Name");
        assertEquals(Arrays.asList("Alice", "Bob", "Charlie"), removedCol);
        assertEquals(2, sampleDataSet.columnCount());
        assertFalse(sampleDataSet.columnNameList().contains("Name"));
        assertThrows(IllegalArgumentException.class, () -> sampleDataSet.removeColumn("NonExistent"));
    }

    @Test
    public void removeColumns_collection() {
        sampleDataSet.removeColumns(Arrays.asList("ID", "Age"));
        assertEquals(1, sampleDataSet.columnCount());
        assertEquals(Collections.singletonList("Name"), sampleDataSet.columnNameList());
    }

    @Test
    public void removeColumns_predicate() {
        sampleDataSet.removeColumns(name -> name.equals("ID") || name.endsWith("e")); // Removes ID, Name, Age
        assertEquals(0, sampleDataSet.columnCount());
        assertTrue(sampleDataSet.columnNameList().isEmpty());
    }

    @Test
    public void convertColumn() {
        // Age is Integer, convert to String
        sampleDataSet.convertColumn("Age", String.class);
        assertEquals("30", sampleDataSet.absolute(0).get("Age"));
        assertTrue(sampleDataSet.getColumn("Age").get(0) instanceof String);

        // ID is Integer, convert to Double
        sampleDataSet.convertColumn("ID", Double.class);
        assertEquals(1.0, sampleDataSet.absolute(0).get("ID"));
        assertTrue(sampleDataSet.getColumn("ID").get(0) instanceof Double);
    }

    @Test
    public void convertColumns_map() {
        Map<String, Class<?>> conversions = new HashMap<>();
        conversions.put("ID", String.class);
        conversions.put("Age", Double.class);
        sampleDataSet.convertColumns(conversions);

        assertTrue(sampleDataSet.getColumn("ID").get(0) instanceof String);
        assertEquals("1", sampleDataSet.absolute(0).get("ID"));
        assertTrue(sampleDataSet.getColumn("Age").get(0) instanceof Double);
        assertEquals(30.0, sampleDataSet.absolute(0).get("Age"));
    }

    @Test
    public void updateColumn() {
        sampleDataSet.updateColumn("Age", (Integer age) -> age * 2);
        assertEquals(Arrays.asList(60, 48, 70), sampleDataSet.getColumn("Age"));
    }

    @Test
    public void updateColumns_collection() {
        sampleDataSet.updateColumns(Arrays.asList("ID", "Age"), val -> ((Number) val).intValue() + 100);
        assertEquals(Arrays.asList(101, 102, 103), sampleDataSet.getColumn("ID"));
        assertEquals(Arrays.asList(130, 124, 135), sampleDataSet.getColumn("Age"));
    }

    @Test
    public void combineColumns_toNewType() {
        // Combine ID (Integer) and Age (Integer) into a new String column "ID_Age"
        sampleDataSet.combineColumns(Arrays.asList("ID", "Age"), "ID_Age_Combined", Map.class);
        assertEquals(2, sampleDataSet.columnCount()); // Name, ID_Age_Combined
        assertTrue(sampleDataSet.columnNameList().contains("ID_Age_Combined"));
        assertTrue(sampleDataSet.columnNameList().contains("Name"));
        assertFalse(sampleDataSet.columnNameList().contains("ID"));
        assertFalse(sampleDataSet.columnNameList().contains("Age"));

        // Check combined values (assuming default conversion combines them as a list string)
        // The default behavior if newColumnType is specified is to create a List<Map<String, Object>>
        // and then convert that list to the target type. For String.class, it's List.toString().
        // This might not be the most intuitive for simple combination, function version is better.
        // For direct control, use the function-based combine.
        // For example, row 0: ID=1, Age=30. Becomes [{ID=1, Age=30}].toString()
        assertEquals(Map.of("ID", 1, "Age", 30), sampleDataSet.absolute(0).get("ID_Age_Combined"));
        assertEquals(Map.of("ID", 2, "Age", 24), sampleDataSet.absolute(1).get("ID_Age_Combined"));
    }

    @Test
    public void combineColumns_withFunction() {
        sampleDataSet.combineColumns(Arrays.asList("Name", "Age"), "NameAndAge", (DisposableObjArray row) -> row.get(0) + " is " + row.get(1));
        assertEquals(2, sampleDataSet.columnCount()); // ID, NameAndAge
        assertTrue(sampleDataSet.columnNameList().contains("NameAndAge"));
        assertEquals(Arrays.asList("Alice is 30", "Bob is 24", "Charlie is 35"), sampleDataSet.getColumn("NameAndAge"));
    }

    @Test
    public void divideColumn_withFunctionToList() {
        // Add a column "FullName" like "Alice Wonderland"
        sampleDataSet.addColumn("FullName", Arrays.asList("Alice Wonderland", "Bob TheBuilder", "Charlie Brown"));
        sampleDataSet.divideColumn("FullName", Arrays.asList("FirstName", "LastName"), (String fullName) -> {
            if (fullName == null)
                return Arrays.asList(null, null);
            String[] parts = ((String) fullName).split(" ", 2);
            return Arrays.asList(parts[0], parts.length > 1 ? parts[1] : null);
        });

        assertEquals(5, sampleDataSet.columnCount()); // ID, Name, Age, FirstName, LastName
        assertTrue(sampleDataSet.columnNameList().contains("FirstName"));
        assertTrue(sampleDataSet.columnNameList().contains("LastName"));
        assertEquals(Arrays.asList("Alice", "Bob", "Charlie"), sampleDataSet.getColumn("FirstName"));
        assertEquals(Arrays.asList("Wonderland", "TheBuilder", "Brown"), sampleDataSet.getColumn("LastName"));
    }

    @Test
    public void divideColumn_withBiConsumerObjectArray() {
        sampleDataSet.addColumn("Coords", Arrays.asList("10,20", "30,40", "50,60"));
        sampleDataSet.divideColumn("Coords", Arrays.asList("X", "Y"), (String coords, Object[] output) -> {
            if (coords == null) {
                output[0] = null;
                output[1] = null;
                return;
            }
            String[] parts = ((String) coords).split(",");
            output[0] = Integer.parseInt(parts[0]);
            output[1] = Integer.parseInt(parts[1]);
        });
        assertEquals(5, sampleDataSet.columnCount());
        assertEquals(Arrays.asList(10, 30, 50), sampleDataSet.getColumn("X"));
        assertEquals(Arrays.asList(20, 40, 60), sampleDataSet.getColumn("Y"));
    }

    @Test
    public void divideColumn_withBiConsumerPair() {
        sampleDataSet.addColumn("NameAndInitial", Arrays.asList("Alice A", "Bob B", "Charlie C"));
        sampleDataSet.divideColumn("NameAndInitial", Tuple.of("DerivedName", "Initial"), (String ni, Pair<Object, Object> output) -> {
            if (ni == null) {
                output.set(null, null);
                return;
            }
            String[] parts = ((String) ni).split(" ");
            output.setLeft(parts[0]);
            output.setRight(parts[1].charAt(0));
        });
        assertEquals(Arrays.asList("Alice", "Bob", "Charlie"), sampleDataSet.getColumn("DerivedName"));
        assertEquals(Arrays.asList('A', 'B', 'C'), sampleDataSet.getColumn("Initial"));
    }

    @Test
    public void addRow_fromArray() {
        sampleDataSet.addRow(new Object[] { 4, "David", 28 });
        assertEquals(4, sampleDataSet.size());
        assertEquals("David", sampleDataSet.absolute(3).get("Name"));
    }

    @Test
    public void addRow_fromList() {
        sampleDataSet.addRow(Arrays.asList(4, "Eve", 40));
        assertEquals(4, sampleDataSet.size());
        assertEquals((Integer) 40, sampleDataSet.absolute(3).get("Age"));
    }

    @Test
    public void addRow_fromMap() {
        Map<String, Object> newRowMap = new LinkedHashMap<>();
        newRowMap.put("ID", 4);
        newRowMap.put("Name", "Frank");
        newRowMap.put("Age", 33);
        sampleDataSet.addRow(newRowMap);
        assertEquals(4, sampleDataSet.size());
        assertEquals("Frank", sampleDataSet.absolute(3).get("Name"));

        Map<String, Object> incompleteMap = Map.of("ID", 5, "Name", "Grace");
        assertThrows(IllegalArgumentException.class, () -> sampleDataSet.addRow(incompleteMap)); // Missing Age
    }

    @Test
    public void addRow_fromBean() {
        TestBean bean = new TestBean(4, "Ivy", 22);
        // To add from bean, column names must match bean property names (case-sensitive default)
        // Or, use a custom RowDataSet where column names are "id", "name", "value"
        RowDataSet beanDs = new RowDataSet(Arrays.asList("id", "name", "value"), Arrays.asList(new ArrayList<>(), new ArrayList<>(), new ArrayList<>()));
        beanDs.addRow(bean);
        assertEquals(1, beanDs.size());
        assertEquals((Integer) 4, beanDs.absolute(0).get("id"));
        assertEquals("Ivy", beanDs.absolute(0).get("name"));
        assertEquals(22.0, (double) beanDs.absolute(0).get("value"), 0.001);
    }

    @Test
    public void addRow_atPosition() {
        sampleDataSet.addRow(1, new Object[] { 0, "Zero", 0 }); // Add at index 1
        assertEquals(4, sampleDataSet.size());
        assertEquals("Zero", sampleDataSet.absolute(1).get("Name"));
        assertEquals("Bob", sampleDataSet.absolute(2).get("Name")); // Bob is shifted
    }

    @Test
    public void removeRow() {
        sampleDataSet.removeRow(1); // Remove Bob
        assertEquals(2, sampleDataSet.size());
        assertEquals("Charlie", sampleDataSet.absolute(1).get("Name")); // Charlie is now at index 1
        assertThrows(IndexOutOfBoundsException.class, () -> sampleDataSet.removeRow(5));
    }

    @Test
    public void removeRows_indices() {
        sampleDataSet.removeRows(0, 2); // Remove Alice and Charlie
        assertEquals(1, sampleDataSet.size());
        assertEquals("Bob", sampleDataSet.absolute(0).get("Name"));
    }

    @Test
    public void removeRowRange() {
        sampleDataSet.removeRowRange(0, 2); // Remove Alice and Bob
        assertEquals(1, sampleDataSet.size());
        assertEquals("Charlie", sampleDataSet.absolute(0).get("Name"));
        assertThrows(IndexOutOfBoundsException.class, () -> sampleDataSet.removeRowRange(0, 5));
    }

    @Test
    public void updateRow() {
        sampleDataSet.updateRow(0, val -> {
            if (val instanceof String)
                return ((String) val).toUpperCase();
            if (val instanceof Integer && ((Integer) val) == 30)
                return 31; // Alice's age
            return val;
        });
        assertEquals("ALICE", sampleDataSet.absolute(0).get("Name"));
        assertEquals((Integer) 31, sampleDataSet.absolute(0).get("Age"));
    }

    @Test
    public void updateAll() {
        sampleDataSet.updateAll(val -> {
            if (val instanceof String)
                return "Name_" + val;
            if (val instanceof Integer)
                return ((Integer) val) + 10;
            return val;
        });
        assertEquals(Arrays.asList(11, 12, 13), sampleDataSet.getColumn("ID"));
        assertEquals(Arrays.asList("Name_Alice", "Name_Bob", "Name_Charlie"), sampleDataSet.getColumn("Name"));
        assertEquals(Arrays.asList(40, 34, 45), sampleDataSet.getColumn("Age"));
    }

    @Test
    public void replaceIf() {
        sampleDataSet.replaceIf(val -> val instanceof String && "Bob".equals(val), "Robert");
        assertEquals("Robert", sampleDataSet.absolute(1).get("Name"));

        sampleDataSet.replaceIf(val -> val instanceof Integer && (Integer) val > 30, 0);
        assertEquals((Integer) 0, sampleDataSet.absolute(2).get("Age")); // Charlie's age was 35
    }

    @Test
    public void getRow_asArray() {
        Object[] row0 = sampleDataSet.getRow(0);
        assertArrayEquals(new Object[] { 1, "Alice", 30 }, row0);
    }

    @Test
    public void getRow_asSpecificType_Array() {
        Object[] row1 = sampleDataSet.getRow(1, Object[].class);
        assertArrayEquals(new Object[] { 2, "Bob", 24 }, row1);
    }

    @Test
    public void getRow_asSpecificType_List() {
        List<Object> row2 = sampleDataSet.getRow(2, List.class);
        assertEquals(Arrays.asList(3, "Charlie", 35), row2);
    }

    @Test
    public void getRow_asSpecificType_Map() {
        Map<String, Object> row0Map = sampleDataSet.getRow(0, Map.class);
        assertEquals(1, row0Map.get("ID"));
        assertEquals("Alice", row0Map.get("Name"));
        assertEquals(30, row0Map.get("Age"));
    }

    @Test
    public void getRow_asSpecificType_Bean() {
        // Need column names to match bean properties for direct mapping
        RowDataSet beanFriendlyDs = new RowDataSet(Arrays.asList("id", "name", "value"), Arrays.asList(new ArrayList<>(Arrays.asList(101, 102)),
                new ArrayList<>(Arrays.asList("Bean1", "Bean2")), new ArrayList<>(Arrays.asList(10.5, 20.5))));
        TestBean bean1 = beanFriendlyDs.getRow(0, TestBean.class);
        assertEquals(101, bean1.getId());
        assertEquals("Bean1", bean1.getName());
        assertEquals(10.5, bean1.getValue(), 0.001);
    }

    @Test
    public void getRow_withSelectedColumns_asArray() {
        Object[] partialRow = sampleDataSet.getRow(0, Arrays.asList("Name", "ID"), Object[].class);
        assertArrayEquals(new Object[] { "Alice", 1 }, partialRow);
    }

    @Test
    public void getRow_withSupplier() {
        AtomicInteger counter = new AtomicInteger(0);
        // Supplier creates a new Map for each row, good for custom map types
        Map<String, Object> row = sampleDataSet.getRow(0, size -> {
            counter.incrementAndGet();
            return new LinkedHashMap<>(size); // Use LinkedHashMap
        });
        assertEquals(1, counter.get());
        assertTrue(row instanceof LinkedHashMap);
        assertEquals(1, row.get("ID"));
    }

    @Test
    public void firstRow_Optional() {
        com.landawn.abacus.util.u.Optional<Object[]> first = sampleDataSet.firstRow();
        assertTrue(first.isPresent());
        assertArrayEquals(new Object[] { 1, "Alice", 30 }, first.get());

        com.landawn.abacus.util.u.Optional<Object[]> emptyFirst = emptyDataSet.firstRow();
        assertFalse(emptyFirst.isPresent());
    }

    @Test
    public void lastRow_Optional() {
        com.landawn.abacus.util.u.Optional<Object[]> last = sampleDataSet.lastRow();
        assertTrue(last.isPresent());
        assertArrayEquals(new Object[] { 3, "Charlie", 35 }, last.get());
    }

    @Test
    public void prepend() {
        List<String> otherNames = Arrays.asList("ID", "Name", "Age");
        List<List<Object>> otherValues = Arrays.asList(new ArrayList<>(Arrays.asList(0)), new ArrayList<>(Arrays.asList("Zero")),
                new ArrayList<>(Arrays.asList(20)));
        RowDataSet otherDs = new RowDataSet(otherNames, otherValues);

        sampleDataSet.prepend(otherDs);
        assertEquals(4, sampleDataSet.size());
        assertEquals((Integer) 0, sampleDataSet.absolute(0).get("ID"));
        assertEquals((Integer) 1, sampleDataSet.absolute(1).get("ID"));
    }

    @Test
    public void append() {
        List<String> otherNames = Arrays.asList("ID", "Name", "Age");
        List<List<Object>> otherValues = Arrays.asList(new ArrayList<>(Arrays.asList(4)), new ArrayList<>(Arrays.asList("David")),
                new ArrayList<>(Arrays.asList(28)));
        RowDataSet otherDs = new RowDataSet(otherNames, otherValues);

        sampleDataSet.append(otherDs);
        assertEquals(4, sampleDataSet.size());
        assertEquals((Integer) 4, sampleDataSet.absolute(3).get("ID"));
    }

    @Test
    public void currentRowNum_and_absolute() {
        assertEquals(0, sampleDataSet.currentRowNum());
        sampleDataSet.absolute(1);
        assertEquals(1, sampleDataSet.currentRowNum());
        assertThrows(IndexOutOfBoundsException.class, () -> sampleDataSet.absolute(10));
    }

    @Test
    public void iterator_Tuple2() {
        BiIterator<String, Integer> iter = sampleDataSet.iterator("Name", "Age");
        assertTrue(iter.hasNext());
        Pair<String, Integer> p = iter.next();
        assertEquals("Alice", p.left());
        assertEquals(30, p.right());
        iter.next(); // Bob
        iter.next(); // Charlie
        assertFalse(iter.hasNext());
    }

    @Test
    public void iterator_Tuple3() {
        TriIterator<Integer, String, Integer> iter = sampleDataSet.iterator("ID", "Name", "Age");
        assertTrue(iter.hasNext());
        Triple<Integer, String, Integer> t = iter.next();
        assertEquals(1, t.left());
        assertEquals("Alice", t.middle());
        assertEquals(30, t.right());
    }

    @Test
    public void forEach_DisposableObjArray() {
        AtomicInteger sumOfIds = new AtomicInteger(0);
        sampleDataSet.forEach(row -> sumOfIds.addAndGet((Integer) row.get(0)));
        assertEquals(1 + 2 + 3, sumOfIds.get());
    }

    @Test
    public void forEach_Tuple2() {
        List<String> combined = new ArrayList<>();
        sampleDataSet.forEach(Tuple.of("Name", "Age"), (String name, Integer age) -> combined.add(name + ":" + age));
        assertEquals(Arrays.asList("Alice:30", "Bob:24", "Charlie:35"), combined);
    }

    @Test
    public void toList_defaultArray() {
        List<Object[]> list = sampleDataSet.toList();
        assertEquals(3, list.size());
        assertArrayEquals(new Object[] { 1, "Alice", 30 }, list.get(0));
    }

    @Test
    public void toList_specificType_Map() {
        List<Map> mapList = sampleDataSet.toList(Map.class);
        assertEquals(3, mapList.size());
        assertEquals("Alice", mapList.get(0).get("Name"));
    }

    @Test
    public void toList_specificType_Bean() {
        RowDataSet beanFriendlyDs = new RowDataSet(Arrays.asList("id", "name", "value"), Arrays.asList(new ArrayList<>(Arrays.asList(101, 102, 103)),
                new ArrayList<>(Arrays.asList("BeanA", "BeanB", "BeanC")), new ArrayList<>(Arrays.asList(1.1, 2.2, 3.3))));
        List<TestBean> beanList = beanFriendlyDs.toList(TestBean.class);
        assertEquals(3, beanList.size());
        assertEquals("BeanA", beanList.get(0).getName());
        assertEquals(2.2, beanList.get(1).getValue(), 0.001);
    }

    @Test
    public void toEntities_simpleBean() {
        RowDataSet beanDs = new RowDataSet(Arrays.asList("id", "name", "value"), Arrays.asList(new ArrayList<>(Arrays.asList(1, 2)),
                new ArrayList<>(Arrays.asList("obj1", "obj2")), new ArrayList<>(Arrays.asList(10.0, 20.0))));
        List<TestBean> entities = beanDs.toEntities(Collections.emptyMap(), TestBean.class);
        assertEquals(2, entities.size());
        assertEquals(new TestBean(1, "obj1", 10.0), entities.get(0));
        assertEquals(new TestBean(2, "obj2", 20.0), entities.get(1));
    }

    @Test
    public void toMergedEntities_singleId() {
        List<String> names = Arrays.asList("id", "name", "value", "detail");
        List<List<Object>> values = Arrays.asList(new ArrayList<>(Arrays.asList(1, 1, 2)), // id
                new ArrayList<>(Arrays.asList("A", "A", "B")), // name
                new ArrayList<>(Arrays.asList(10.0, 10.0, 20.0)), // value
                new ArrayList<>(Arrays.asList("d1", "d2", "d3")) // detail (will be part of nested TestNestedBean)
        );
        RowDataSet ds = new RowDataSet(names, values);

        // This test is complex due to the merging logic with nested beans.
        // A simpler version without nested beans:
        List<String> simpleNames = Arrays.asList("id", "name", "value");
        List<List<Object>> simpleValues = Arrays.asList(new ArrayList<>(Arrays.asList(1, 1, 2)), // id
                new ArrayList<>(Arrays.asList("Alice", "Alice", "Bob")), // name
                new ArrayList<>(Arrays.asList(10.0, 11.0, 20.0)) // value
        );
        RowDataSet simpleDs = new RowDataSet(simpleNames, simpleValues);

        // Assuming the last value for a given ID is taken for non-id fields or that they are compatible for merging.
        // The actual merging logic for non-ID properties depends on how PropInfo.setPropValue handles multiple sets.
        // Typically, for primitive/String types, the last one wins. For collections, it might add.
        // Let's test for distinct IDs first.

        List<TestBean> merged = simpleDs.toMergedEntities("id", TestBean.class);
        assertEquals(2, merged.size()); // 2 distinct IDs

        TestBean bean1 = merged.stream().filter(b -> b.getId() == 1).findFirst().orElse(null);
        TestBean bean2 = merged.stream().filter(b -> b.getId() == 2).findFirst().orElse(null);

        assertNotNull(bean1);
        // The value for name/value for ID 1 will be from the *last* row with ID=1 in the source,
        // if not handled as a collection.
        assertEquals("Alice", bean1.getName()); // From second row for ID=1
        assertEquals(11.0, bean1.getValue(), 0.001); // From second row for ID=1

        assertNotNull(bean2);
        assertEquals("Bob", bean2.getName());
        assertEquals(20.0, bean2.getValue(), 0.001);
    }

    @Test
    public void toMap_keyValue() {
        Map<Integer, String> idToNameMap = sampleDataSet.toMap("ID", "Name");
        assertEquals(3, idToNameMap.size());
        assertEquals("Alice", idToNameMap.get(1));
        assertEquals("Bob", idToNameMap.get(2));
        assertEquals("Charlie", idToNameMap.get(3));
    }

    @Test
    public void toMap_keyRowAsBean() {
        RowDataSet ds = new RowDataSet(Arrays.asList("key", "id", "name", "value"), Arrays.asList(new ArrayList<>(Arrays.asList("k1", "k2")),
                new ArrayList<>(Arrays.asList(1, 2)), new ArrayList<>(Arrays.asList("Alice", "Bob")), new ArrayList<>(Arrays.asList(10.0, 20.0))));

        Map<String, TestBean> map = ds.toMap("key", Arrays.asList("id", "name", "value"), TestBean.class);
        assertEquals(2, map.size());
        assertEquals(new TestBean(1, "Alice", 10.0), map.get("k1"));
        assertEquals(new TestBean(2, "Bob", 20.0), map.get("k2"));
    }

    @Test
    public void toJson_writer() throws IOException {
        StringWriter sw = new StringWriter();
        sampleDataSet.toJson(sw);
        String json = sw.toString();
        // Basic check, actual JSON structure might be complex to assert precisely without a JSON lib
        assertTrue(json.startsWith("["));
        assertTrue(json.endsWith("]"));
        assertTrue(json.contains("\"ID\":1"));
        assertTrue(json.contains("\"Name\":\"Alice\""));
        assertTrue(json.contains("\"Age\":30"));
    }

    @Test
    public void toJson_file(@TempDir File tempDir) throws IOException {
        File tempFile = new File(tempDir, "test.json");
        sampleDataSet.toJson(tempFile);
        assertTrue(tempFile.exists());
        String jsonContent = Files.readString(tempFile.toPath());
        assertTrue(jsonContent.contains("\"Name\":\"Bob\""));

        IOUtil.deleteIfExists(tempFile);
    }

    @Test
    public void toJson_outputStream() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        sampleDataSet.toJson(baos);
        String json = baos.toString();
        assertTrue(json.contains("\"Name\":\"Charlie\""));
    }

    @Test
    public void toXml_writer() throws IOException {
        StringWriter sw = new StringWriter();
        sampleDataSet.toXml(sw);
        String xml = sw.toString();
        assertTrue(xml.startsWith("<dataSet>"));
        assertTrue(xml.endsWith("</dataSet>"));
        assertTrue(xml.contains("<row>"));
        assertTrue(xml.contains("<ID>1</ID>"));
        assertTrue(xml.contains("<Name>Alice</Name>"));
        assertTrue(xml.contains("<Age>30</Age>"));
    }

    @Test
    public void toCsv_writer() throws IOException {
        StringWriter sw = new StringWriter();
        sampleDataSet.toCsv(sw);
        String csv = sw.toString();
        String[] lines = csv.split(System.lineSeparator());
        assertEquals("\"ID\",\"Name\",\"Age\"", lines[0]);
        assertEquals("1,\"Alice\",30", lines[1]);
    }

    @Test
    public void groupBy_singleKey_singleAggregate_collectorSum() {
        List<String> gNames = Arrays.asList("Category", "Value");
        List<List<Object>> gValues = Arrays.asList(new ArrayList<>(Arrays.asList("A", "B", "A", "B", "A")), new ArrayList<>(Arrays.asList(10, 20, 5, 15, 2)));
        RowDataSet dsToGroup = new RowDataSet(gNames, gValues);

        DataSet grouped = dsToGroup.groupBy("Category", "Value", "TotalValue", Collectors.summingInt(val -> (Integer) val));

        assertEquals(2, grouped.columnCount());
        assertEquals(2, grouped.size()); // A and B

        grouped.absolute(0); // Assuming "A" comes first (LinkedHashMap behavior)
        assertEquals("A", grouped.get("Category"));
        assertEquals((Integer) (10 + 5 + 2), grouped.get("TotalValue"));

        grouped.absolute(1);
        assertEquals("B", grouped.get("Category"));
        assertEquals((Integer) (20 + 15), grouped.get("TotalValue"));
    }

    @Test
    public void groupBy_multipleKeys_collectorToList() {
        List<String> gNames = Arrays.asList("Group", "SubGroup", "Data");
        List<List<Object>> gValues = Arrays.asList(new ArrayList<>(Arrays.asList("G1", "G1", "G2", "G1")),
                new ArrayList<>(Arrays.asList("S1", "S2", "S1", "S1")), new ArrayList<>(Arrays.asList(10, 20, 30, 40)));
        RowDataSet dsToGroup = new RowDataSet(gNames, gValues);

        DataSet grouped = dsToGroup.groupBy(Arrays.asList("Group", "SubGroup"), "Data", "CollectedData", Collectors.toList()); // Collector collects Integers into a List<Integer>

        // Expected: (G1, S1, [10, 40]), (G1, S2, [20]), (G2, S1, [30])
        assertEquals(3, grouped.columnCount()); // Group, SubGroup, CollectedData
        assertEquals(3, grouped.size());

        // This requires checking specific rows, which depends on the order from groupBy
        // For simplicity, convert to a known structure like a map for assertion.
        Map<Tuple2<String, String>, List<Integer>> resultMap = new HashMap<>();
        for (int i = 0; i < grouped.size(); i++) {
            grouped.absolute(i);
            resultMap.put(Tuple.of(grouped.get("Group"), grouped.get("SubGroup")), (List<Integer>) grouped.get("CollectedData"));
        }

        assertEquals(Arrays.asList(10, 40), resultMap.get(Tuple.of("G1", "S1")));
        assertEquals(Collections.singletonList(20), resultMap.get(Tuple.of("G1", "S2")));
        assertEquals(Collections.singletonList(30), resultMap.get(Tuple.of("G2", "S1")));
    }

    @Test
    public void sortBy_singleColumn_defaultOrder() {
        // Original: (1,A,30), (2,B,24), (3,C,35)
        // Sort by Age: (2,B,24), (1,A,30), (3,C,35)
        sampleDataSet.sortBy("Age");
        assertEquals((Integer) 2, sampleDataSet.absolute(0).get("ID")); // Bob
        assertEquals((Integer) 1, sampleDataSet.absolute(1).get("ID")); // Alice
        assertEquals((Integer) 3, sampleDataSet.absolute(2).get("ID")); // Charlie
    }

    @Test
    public void sortBy_singleColumn_customComparator() {
        // Sort by Name descending
        sampleDataSet.sortBy("Name", Comparator.reverseOrder());
        assertEquals("Charlie", sampleDataSet.absolute(0).get("Name"));
        assertEquals("Bob", sampleDataSet.absolute(1).get("Name"));
        assertEquals("Alice", sampleDataSet.absolute(2).get("Name"));
    }

    @Test
    public void sortBy_multipleColumns_defaultComparator() {
        // Add a row to make multi-column sort meaningful
        sampleDataSet.addRow(new Object[] { 4, "Alice", 25 }); // (4,Alice,25)
        // Now: (1,A,30), (2,B,24), (3,C,35), (4,A,25)
        // Sort by Name, then Age
        sampleDataSet.sortBy(Arrays.asList("Name", "Age"));
        // Expected: (4,A,25), (1,A,30), (2,B,24), (3,C,35)
        assertEquals((Integer) 4, sampleDataSet.absolute(0).get("ID")); // Alice, 25
        assertEquals((Integer) 1, sampleDataSet.absolute(1).get("ID")); // Alice, 30
        assertEquals((Integer) 2, sampleDataSet.absolute(2).get("ID")); // Bob, 24
        assertEquals((Integer) 3, sampleDataSet.absolute(3).get("ID")); // Charlie, 35
    }

    @Test
    public void distinct() {
        sampleDataSet.addRow(new Object[] { 1, "Alice", 30 }); // Add duplicate row
        assertEquals(4, sampleDataSet.size());
        DataSet distinctDs = sampleDataSet.distinct();
        assertEquals(3, distinctDs.size());
    }

    @Test
    public void distinctBy_singleColumn() {
        sampleDataSet.addRow(new Object[] { 4, "Alice", 28 }); // Another Alice
        DataSet distinctByName = sampleDataSet.distinctBy("Name");
        // Should keep first occurrence of each name
        assertEquals(3, distinctByName.size());
        List<Object> names = distinctByName.getColumn("Name");
        assertTrue(names.contains("Alice") && names.contains("Bob") && names.contains("Charlie"));
    }

    @Test
    public void filter_byPredicateOnDisposableObjArray() {
        DataSet filtered = sampleDataSet.filter(row -> (Integer) row.get(2) > 30);
        assertEquals(1, filtered.size());
        assertEquals("Charlie", filtered.getRow(0, Map.class).get("Name"));
    }

    @Test
    public void filter_byPredicateOnSingleColumn() {
        DataSet filtered = sampleDataSet.filter("Name", (String name) -> name.startsWith("A"));
        assertEquals(1, filtered.size());
        assertEquals("Alice", filtered.getRow(0, Map.class).get("Name"));
    }

    @Test
    public void map_singleColumnToNew() {
        DataSet mapped = sampleDataSet.map("Age", "AgeInMonths", "ID", (Integer age) -> age * 12);
        assertEquals(2, mapped.columnCount()); // AgeInMonths, ID
        assertTrue(mapped.columnNameList().contains("AgeInMonths"));
        assertTrue(mapped.columnNameList().contains("ID"));
        assertEquals(30 * 12, mapped.getRow(0, Map.class).get("AgeInMonths"));
    }

    @Test
    public void flatMap_singleColumnToNewCollection() {
        // Add a column with list-like strings
        sampleDataSet.addColumn("Hobbies", Arrays.asList("Reading,Hiking", "Gaming", "Cooking,Swimming"));
        DataSet flatMapped = sampleDataSet.flatMap("Hobbies", "Hobby", "Name", (String hobbies) -> Arrays.asList(hobbies.split(",")));

        // Expected rows: (Alice,Reading), (Alice,Hiking), (Bob,Gaming), (Charlie,Cooking), (Charlie,Swimming)
        assertEquals(2, flatMapped.columnCount()); // Hobby, Name
        assertEquals(5, flatMapped.size());
        assertEquals("Reading", flatMapped.absolute(0).get("Hobby"));
        assertEquals("Alice", flatMapped.absolute(0).get("Name"));
        assertEquals("Gaming", flatMapped.absolute(2).get("Hobby"));
        assertEquals("Bob", flatMapped.absolute(2).get("Name"));
    }

    @Test
    public void copy_full() {
        DataSet copied = sampleDataSet.copy();
        assertNotSame(sampleDataSet, copied);
        assertEquals(sampleDataSet, copied); // Content equality
        // Modify copy, original should not change
        copied.set(0, 0, 100);
        assertEquals((Integer) 1, sampleDataSet.absolute(0).get(0));
    }

    @Test
    public void copy_selectedColumns() {
        DataSet copied = sampleDataSet.copy(Arrays.asList("Name", "Age"));
        assertEquals(2, copied.columnCount());
        assertTrue(copied.columnNameList().contains("Name"));
        assertTrue(copied.columnNameList().contains("Age"));
        assertEquals("Alice", copied.get(0, 0)); // Name of first row
    }

    @Test
    public void copy_range() {
        DataSet copied = sampleDataSet.copy(1, 3); // Bob and Charlie
        assertEquals(2, copied.size());
        assertEquals("Bob", copied.absolute(0).get("Name"));
        assertEquals("Charlie", copied.absolute(1).get("Name"));
    }

    @Test
    public void clone_method() {
        // The actual clone uses Kryo. If Kryo is not set up, it throws RuntimeException.
        // Here, we test that behavior, or if Kryo was mocked/available, we'd test content.
        // For now, assume Kryo is not available for this basic test setup.
        // To properly test clone, you'd need Kryo in the test classpath or mock ParserFactory.
        // assertThrows(RuntimeException.class, () -> sampleDataSet.clone(), "Kryo is required");

        DataSet copy = sampleDataSet.clone();
        assertFalse(copy == sampleDataSet);
        assertEquals(sampleDataSet, copy); // Content equality

        // If Kryo were available, the test would be more like:
        // RowDataSet clonedDs = (RowDataSet) sampleDataSet.clone();
        // assertNotSame(sampleDataSet, clonedDs);
        // assertEquals(sampleDataSet, clonedDs);
        // assertFalse(clonedDs.isFrozen()); // Default clone is not frozen
        //
        // RowDataSet frozenClone = (RowDataSet) sampleDataSet.clone(true);
        // assertTrue(frozenClone.isFrozen());
    }

    @Test
    public void split_intoChunks() {
        Stream<DataSet> stream = sampleDataSet.split(2);
        List<DataSet> chunks = stream.toList();
        assertEquals(2, chunks.size());
        assertEquals(2, chunks.get(0).size()); // First chunk: Alice, Bob
        assertEquals(1, chunks.get(1).size()); // Second chunk: Charlie
        assertEquals("Alice", chunks.get(0).absolute(0).get("Name"));
        assertEquals("Charlie", chunks.get(1).absolute(0).get("Name"));
    }

    @Test
    public void slice_dataSet() {
        DataSet sliced = sampleDataSet.slice(1, 2, Arrays.asList("Name")); // Bob's Name
        assertEquals(1, sliced.size());
        assertEquals(1, sliced.columnCount());
        assertEquals("Bob", sliced.get(0, 0)); // Only one column "Name"
        assertTrue(sliced.isFrozen()); // Slice result is frozen
    }

    @Test
    public void stream_singleColumn() {
        List<String> names = sampleDataSet.stream("Name").map(s -> ((String) s).toUpperCase()).toList();
        assertEquals(Arrays.asList("ALICE", "BOB", "CHARLIE"), names);
    }

    @Test
    public void stream_asBean() {
        RowDataSet beanDs = new RowDataSet(Arrays.asList("id", "name", "value"), Arrays.asList(new ArrayList<>(Arrays.asList(1, 2)),
                new ArrayList<>(Arrays.asList("obj1", "obj2")), new ArrayList<>(Arrays.asList(10.0, 20.0))));
        List<Integer> ids = beanDs.stream(TestBean.class).map(TestBean::getId).toList();
        assertEquals(Arrays.asList(1, 2), ids);
    }

    @Test
    public void apply_function() {
        Integer totalAge = sampleDataSet.apply(ds -> {
            int sum = 0;
            for (int i = 0; i < ds.size(); i++) {
                sum += (Integer) ds.absolute(i).get("Age");
            }
            return sum;
        });
        assertEquals(30 + 24 + 35, totalAge.intValue());
    }

    @Test
    public void accept_consumer() {
        AtomicInteger count = new AtomicInteger(0);
        sampleDataSet.accept(ds -> {
            count.set(ds.size());
        });
        assertEquals(3, count.get());
    }

    @Test
    public void freeze_and_isFrozen() {
        assertFalse(sampleDataSet.isFrozen());
        sampleDataSet.freeze();
        assertTrue(sampleDataSet.isFrozen());
        // Try an operation that modifies
        assertThrows(IllegalStateException.class, () -> sampleDataSet.set(0, 0, 100));
    }

    @Test
    public void isEmpty_size_clear() {
        assertFalse(sampleDataSet.isEmpty());
        assertEquals(3, sampleDataSet.size());

        sampleDataSet.clear();
        assertTrue(sampleDataSet.isEmpty());
        assertEquals(0, sampleDataSet.size());
        // Columns should still exist, but be empty
        assertEquals(3, sampleDataSet.columnCount());
        assertTrue(sampleDataSet.getColumn(0).isEmpty());

        RowDataSet frozenDs = createSimpleDataSet();
        frozenDs.freeze();
        assertThrows(IllegalStateException.class, frozenDs::clear);
    }

    @Test
    public void properties_access() {
        assertTrue(sampleDataSet.properties().isEmpty());
        Map<String, Object> props = new HashMap<>();
        props.put("version", 1.2);
        RowDataSet dsWithProps = new RowDataSet(columnNames, columnValues, props);
        assertEquals(1.2, (Double) dsWithProps.properties().get("version"), 0.001);
        // Test immutability of returned properties map
        assertThrows(UnsupportedOperationException.class, () -> dsWithProps.properties().put("newKey", "newVal"));
    }

    @Test
    public void columnNames_stream() {
        List<String> streamedNames = sampleDataSet.columnNames().toList();
        assertEquals(columnNames, streamedNames);
    }

    @Test
    public void columns_streamOfImmutableLists() {
        List<ArrayList<Object>> streamedCols = sampleDataSet.columns().map(ArrayList::new).toList();
        assertEquals(columnValues, streamedCols);
    }

    @Test
    public void columnMap() {
        Map<String, com.landawn.abacus.util.ImmutableList<Object>> map = sampleDataSet.columnMap();
        assertEquals(3, map.size());
        assertEquals(sampleDataSet.getColumn("ID"), map.get("ID"));
        assertEquals(sampleDataSet.getColumn("Name"), map.get("Name"));
    }

    @Test
    public void println_toWriter() throws IOException {
        StringWriter stringWriter = new StringWriter();
        sampleDataSet.println(stringWriter);
        String output = stringWriter.toString();

        assertTrue(output.contains("+----+---------+-----+")); // Header separator
        assertTrue(output.contains("| ID | Name    | Age |")); // Header
        assertTrue(output.contains("| 1  | Alice   | 30  |")); // Row 1
        assertTrue(output.contains("| 2  | Bob     | 24  |")); // Row 2
        assertTrue(output.contains("| 3  | Charlie | 35  |")); // Row 3
    }

    @Test
    public void hashCode_equals_toString() {
        RowDataSet ds1 = createSimpleDataSet();
        RowDataSet ds2 = createSimpleDataSet(); // Same content
        RowDataSet ds3 = new RowDataSet(Arrays.asList("ID", "Name", "Value"), // Different column name
                Arrays.asList(new ArrayList<>(Arrays.asList(1, 2, 3)), new ArrayList<>(Arrays.asList("Alice", "Bob", "Charlie")),
                        new ArrayList<>(Arrays.asList(30.0, 24.0, 35.0))));
        RowDataSet ds4 = new RowDataSet(columnNames, Arrays.asList(new ArrayList<>(Arrays.asList(1, 2, 99)), // Different data
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
        List<String> orderCN = Arrays.asList("OrderID", "ID", "Product"); // ID is foreign key
        List<List<Object>> orderCV = Arrays.asList(new ArrayList<>(Arrays.asList(101, 102, 103, 104)), new ArrayList<>(Arrays.asList(1, 1, 2, 4)), // User IDs
                new ArrayList<>(Arrays.asList("Book", "Pen", "Paper", "Clip")));
        RowDataSet orders = new RowDataSet(orderCN, orderCV);

        DataSet joined = sampleDataSet.innerJoin(orders, "ID", "ID");
        // Expected: (1,Alice,30,101,Book), (1,Alice,30,102,Pen), (2,Bob,24,103,Paper)
        assertEquals(3, joined.size());
        assertEquals(sampleDataSet.columnCount() + orders.columnCount(), joined.columnCount()); // Names might be adjusted for duplicates
        assertTrue(joined.columnNameList().contains("ID_2")); // Due to duplicate ID column name

        // Check a merged row
        joined.absolute(0);
        assertEquals((Integer) 1, joined.get("ID"));
        assertEquals("Alice", joined.get("Name"));
        assertEquals((Integer) 101, joined.get("OrderID"));
        assertEquals("Book", joined.get("Product"));
    }

    @Test
    public void leftJoin_singleColumn() {
        List<String> orderCN = Arrays.asList("OrderID", "ID", "Product");
        List<List<Object>> orderCV = Arrays.asList(new ArrayList<>(Arrays.asList(101, 102)), // Only for ID 1
                new ArrayList<>(Arrays.asList(1, 1)), new ArrayList<>(Arrays.asList("Book", "Pen")));
        RowDataSet orders = new RowDataSet(orderCN, orderCV);

        DataSet joined = sampleDataSet.leftJoin(orders, "ID", "ID");
        // Expected: (1,A,30,101,Book), (1,A,30,102,Pen), (2,B,24,null,null), (3,C,35,null,null)
        assertEquals(4, joined.size());

        joined.absolute(2); // Bob's row
        assertEquals((Integer) 2, joined.get("ID"));
        assertNull(joined.get("OrderID")); // No matching order for Bob
    }

    @Test
    public void union_dataSets() {
        List<String> otherNames = Arrays.asList("ID", "Name", "Salary"); // Age vs Salary
        List<List<Object>> otherValues = Arrays.asList(new ArrayList<>(Arrays.asList(3, 4)), // Charlie (duplicate ID), David
                new ArrayList<>(Arrays.asList("Charlie", "David")), new ArrayList<>(Arrays.asList(70000, 80000)));
        RowDataSet otherDs = new RowDataSet(otherNames, otherValues);

        DataSet unionResult = sampleDataSet.union(otherDs);
        // Columns: ID, Name, Age, Salary
        // Rows: (1,A,30,null), (2,B,24,null), (3,C,35,70000 -- Age from sample, Salary from other), (4,D,null,80000)
        assertEquals(4, unionResult.columnCount());
        assertEquals(4, unionResult.size());

        sampleDataSet.println();
        otherDs.println();
        unionResult.println();

        Map<Integer, Map<String, Object>> resultMap = new HashMap<>();
        for (int i = 0; i < unionResult.size(); i++) {
            resultMap.put((Integer) unionResult.absolute(i).get("ID"), unionResult.getRow(i, Map.class));
        }

        assertEquals("Alice", resultMap.get(1).get("Name"));
        assertEquals(30, resultMap.get(1).get("Age"));
        assertNull(resultMap.get(1).get("Salary"));

        assertEquals("Charlie", resultMap.get(3).get("Name"));
        assertEquals(35, resultMap.get(3).get("Age")); // From sampleDataSet (first occurrence)
        assertEquals(70000, resultMap.get(3).get("Salary")); // From otherDs

        assertEquals("David", resultMap.get(4).get("Name"));
        assertNull(resultMap.get(4).get("Age"));
        assertEquals(80000, resultMap.get(4).get("Salary"));
    }

    @Test
    public void intersect_dataSets() {
        List<String> otherNames = Arrays.asList("ID", "Name", "Age");
        List<List<Object>> otherValues = Arrays.asList(new ArrayList<>(Arrays.asList(2, 3, 4)), // Bob, Charlie, David
                new ArrayList<>(Arrays.asList("Bob", "Charlie", "David")), new ArrayList<>(Arrays.asList(24, 35, 28)));
        RowDataSet otherDs = new RowDataSet(otherNames, otherValues);

        DataSet intersectResult = sampleDataSet.intersect(otherDs);
        // Common rows: Bob, Charlie
        assertEquals(2, intersectResult.size());
        List<String> names = intersectResult.toList(Map.class).stream().map(m -> (String) m.get("Name")).sorted().toList();
        assertEquals(Arrays.asList("Bob", "Charlie"), names);
    }

    @Test
    public void except_dataSets() {
        List<String> otherNames = Arrays.asList("ID", "Name", "Age");
        List<List<Object>> otherValues = Arrays.asList(new ArrayList<>(Arrays.asList(2, 3)), // Bob, Charlie
                new ArrayList<>(Arrays.asList("Bob", "Charlie")), new ArrayList<>(Arrays.asList(24, 35)));
        RowDataSet otherDs = new RowDataSet(otherNames, otherValues);

        DataSet exceptResult = sampleDataSet.except(otherDs);
        // Rows in sampleDataSet but not in otherDs: Alice
        assertEquals(1, exceptResult.size());
        assertEquals("Alice", exceptResult.absolute(0).get("Name"));
    }

    // A simple bean for testing toEntities, etc.
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

    private RowDataSet createSimpleDataSet() {
        List<String> names = new ArrayList<>(Arrays.asList("ID", "Name", "Age"));
        List<List<Object>> values = new ArrayList<>();
        values.add(new ArrayList<>(Arrays.asList(1, 2, 3)));
        values.add(new ArrayList<>(Arrays.asList("Alice", "Bob", "Charlie")));
        values.add(new ArrayList<>(Arrays.asList(30, 24, 35)));
        return new RowDataSet(names, values);
    }
}

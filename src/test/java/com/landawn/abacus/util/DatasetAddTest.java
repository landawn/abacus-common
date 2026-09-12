package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.util.NoCachingNoUpdating.DisposableObjArray;
import com.landawn.abacus.util.Tuple.Tuple2;
import com.landawn.abacus.util.Tuple.Tuple3;
import com.landawn.abacus.util.function.Function;
import com.landawn.abacus.util.function.TriFunction;

import testfixtures.entity.extendDirty.basic.Account;

public class DatasetAddTest extends DatasetTestSupport {
    @Test
    public void testAddColumn_WithCollection() {
        Dataset ds = dataset.copy();
        List<String> emails = Arrays.asList("john@test.com", "jane@test.com", "bob@test.com", "alice@test.com", "charlie@test.com");
        ds.addColumn("email", emails);

        assertEquals(5, ds.columnCount());
        assertEquals("email", ds.getColumnName(4));
        assertEquals("john@test.com", ds.get(0, 4));
    }

    @Test
    public void testAddColumn_WithPosition() {
        Dataset ds = dataset.copy();
        List<String> emails = Arrays.asList("john@test.com", "jane@test.com", "bob@test.com", "alice@test.com", "charlie@test.com");
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
        assertEquals("John (25)", ds.get(0, 4));
        assertEquals("Jane (30)", ds.get(1, 4));
    }

    @Test
    public void testAddColumn_WithTriFunction() {
        Dataset ds = dataset.copy();
        ds.addColumn("info", Tuple.of("id", "name", "age"), (Integer id, String name, Integer age) -> id + ":" + name + ":" + age);

        assertEquals(5, ds.columnCount());
        assertEquals("1:John:25", ds.get(0, 4));
    }

    @Test
    public void addColumn_fromAnotherColumn_withFunction() {
        sampleDataset.addColumn("AgePlus5", "Age", (Integer age) -> age + 5);
        assertEquals(4, sampleDataset.columnCount());
        assertTrue(sampleDataset.columnNames().contains("AgePlus5"));
        assertEquals(Arrays.asList(35, 29, 40), sampleDataset.getColumn("AgePlus5"));
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
    public void testAddColumnWithCollection() {
        Dataset ds = dataset.copy();
        List<String> newColumn = Arrays.asList("Engineering", "Sales", "Marketing", "HR", "Finance");
        ds.addColumn("department", newColumn);
        assertEquals(5, ds.columnCount());
        assertEquals("department", ds.getColumnName(4));
        assertEquals("Engineering", ds.get(0, 4));
    }

    @Test
    public void testAddColumnWithPosition() {
        Dataset ds = dataset.copy();
        List<String> newColumn = Arrays.asList("A", "B", "C", "D", "E");
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
        assertEquals((Integer) 4, ds.get(0, 4));
    }

    @Test
    public void testAddColumnWithTuple2() {
        Dataset ds = dataset.copy();
        ds.addColumn("nameAge", Tuple.of("name", "age"), (String name, Integer age) -> name + "(" + age + ")");
        assertEquals(5, ds.columnCount());
        assertEquals("John(25)", ds.get(0, 4));
    }

    @Test
    public void testAddColumnWithTuple3() {
        Dataset ds = dataset.copy();
        ds.addColumn("summary", Tuple.of("name", "age", "salary"), (String name, Integer age, Double salary) -> name + "," + age + "," + salary);
        assertEquals(5, ds.columnCount());
        assertEquals("John,25,50000.0", ds.get(0, 4));
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
    public void testAddColumnWithBiFunction() {
        Dataset dataset = testDataset.copy();
        Tuple2<String, String> fromColumns = Tuple.of("name", "age");
        BiFunction<String, Integer, String> func = (name, age) -> name + "_" + age;
        dataset.addColumn("name_age", fromColumns, func);

        assertTrue(dataset.containsColumn("name_age"));
        dataset.moveToRow(0);
        assertEquals("Alice_30", dataset.get("name_age"));
    }

    @Test
    public void testAddColumnWithTriFunction() {
        Dataset dataset = testDataset.copy();
        Tuple3<String, String, String> fromColumns = Tuple.of("id", "name", "age");
        TriFunction<Integer, String, Integer, String> func = (id, name, age) -> id + ":" + name + ":" + age;
        dataset.addColumn("combined", fromColumns, func);

        assertTrue(dataset.containsColumn("combined"));
        dataset.moveToRow(0);
        assertEquals("1:Alice:30", dataset.get("combined"));
    }

    @Test
    public void testAddColumn() {
        List<Object> newColumn = Arrays.asList("A", "B", "C", "D", "E");
        dataset.addColumn("grade", newColumn);

        assertEquals(5, dataset.columnCount());
        assertTrue(dataset.containsColumn("grade"));
        assertEquals("A", dataset.get(0, 4));
    }

    @Test
    public void testAddColumnWithDisposableObjArray() {
        dataset.addColumn("combined", Arrays.asList("id", "name"), (DisposableObjArray arr) -> arr.get(0) + ":" + arr.get(1));

        assertTrue(dataset.containsColumn("combined"));
        assertEquals("1:John", dataset.get(0, 4));
    }

    @Test
    public void addColumn_fromMultipleColumns_withFunction() {
        sampleDataset.addColumn("ID_Name", Arrays.asList("ID", "Name"), (DisposableObjArray row) -> row.get(0) + "_" + row.get(1));
        assertEquals(4, sampleDataset.columnCount());
        assertTrue(sampleDataset.columnNames().contains("ID_Name"));
        assertEquals(Arrays.asList("1_Alice", "2_Bob", "3_Charlie"), sampleDataset.getColumn("ID_Name"));
    }

    @Test
    public void testAddColumnWithMultipleColumns() {
        Dataset ds = dataset.copy();
        ds.addColumn("fullInfo", Arrays.asList("name", "age"), arr -> arr.get(0) + ":" + arr.get(1));
        assertEquals(5, ds.columnCount());
        assertEquals("John:25", ds.get(0, 4));
    }

    @Test
    public void test_addColumn() throws Exception {
        final Account account = createAccount(Account.class);
        final Dataset ds1 = CommonUtil.newDataset(CommonUtil.toList(account, account));

        ds1.addColumn("firstName2", "firstName", (Function<String, String>) t -> "**********" + t);

        ds1.addColumn(0, "firstName3", CommonUtil.toList("firstName", "lastName"),
                (Function<DisposableObjArray, String>) a -> a.get(0) + "**********" + a.get(1));

        assertNotNull(ds1);
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
    public void testAddColumns() {
        Dataset ds = dataset.copy();
        List<String> newColNames = Arrays.asList("email", "city");
        List<List<Object>> newCols = new ArrayList<>();
        newCols.add(Arrays.asList("a@test.com", "b@test.com", "c@test.com", "d@test.com", "e@test.com"));
        newCols.add(Arrays.asList("NYC", "LA", "SF", "Seattle", "Chicago"));

        ds.addColumns(newColNames, newCols);
        assertEquals(6, ds.columnCount());
        assertEquals("email", ds.getColumnName(4));
        assertEquals("city", ds.getColumnName(5));
    }

    @Test
    @DisplayName("Should add columns at beginning of dataset")
    public void testAddColumnsAtBeginning() {
        List<String> newColumnNames = Arrays.asList("prefix1", "prefix2");
        List<Collection<Object>> newColumns = Arrays.asList(Arrays.asList("P1-1", "P1-2", "P1-3", "P1-4", "P1-5"),
                Arrays.asList("P2-1", "P2-2", "P2-3", "P2-4", "P2-5"));

        dataset.addColumns(0, newColumnNames, newColumns);

        assertEquals(6, dataset.columnCount());
        assertEquals("prefix1", dataset.getColumnName(0));
        assertEquals("prefix2", dataset.getColumnName(1));
        assertEquals("id", dataset.getColumnName(2));
        assertEquals("P1-1", dataset.get(0, 0));
        assertEquals("P2-1", dataset.get(0, 1));
        assertEquals((Integer) 1, dataset.get(0, 2));
    }

    @Test
    @DisplayName("Should preserve original data when adding columns")
    public void testAddColumnsPreservesOriginalData() {
        Object[][] originalData = new Object[dataset.size()][dataset.columnCount()];
        for (int i = 0; i < dataset.size(); i++) {
            for (int j = 0; j < dataset.columnCount(); j++) {
                originalData[i][j] = dataset.get(i, j);
            }
        }

        List<String> newColumnNames = Arrays.asList("new1", "new2");
        List<Collection<Object>> newColumns = Arrays.asList(Arrays.asList("N1", "N2", "N3", "N4", "N5"), Arrays.asList(10.1, 20.2, 30.3, 40.4, 50.5));

        dataset.addColumns(2, newColumnNames, newColumns);

        assertEquals(originalData[0][0], dataset.get(0, 0));
        assertEquals(originalData[0][1], dataset.get(0, 1));
        assertEquals(originalData[0][2], dataset.get(0, 4));
        assertEquals(originalData[0][3], dataset.get(0, 5));
    }

    @Test
    @DisplayName("Should add multiple columns at specific position")
    public void testAddColumnsAtPosition() {
        List<String> newColumnNames = Arrays.asList("grade", "status");
        List<Collection<Object>> newColumns = Arrays.asList(Arrays.asList("A", "B", "C", "B", "A"),
                Arrays.asList("Active", "Active", "Inactive", "Active", "Active"));

        dataset.addColumns(2, newColumnNames, newColumns);

        assertEquals(6, dataset.columnCount());
        assertEquals("grade", dataset.getColumnName(2));
        assertEquals("status", dataset.getColumnName(3));
        assertEquals("age", dataset.getColumnName(4));
        assertEquals("A", dataset.get(0, 2));
        assertEquals("Active", dataset.get(0, 3));
    }

    @Test
    @DisplayName("Should handle empty columns by filling with nulls")
    public void testAddColumnsWithEmptyColumns() {
        List<String> newColumnNames = Arrays.asList("empty1", "empty2");
        List<Collection<Object>> newColumns = Arrays.asList(Collections.emptyList(), Collections.emptyList());

        dataset.addColumns(newColumnNames, newColumns);

        assertEquals(6, dataset.columnCount());
        for (int i = 0; i < dataset.size(); i++) {
            assertNull(dataset.get(i, dataset.getColumnIndex("empty1")));
            assertNull(dataset.get(i, dataset.getColumnIndex("empty2")));
        }
    }

    @Test
    @DisplayName("Should handle mixed empty and non-empty columns")
    public void testAddColumnsMixedEmptyAndNonEmpty() {
        List<String> newColumnNames = Arrays.asList("data", "empty", "moreData");
        List<Collection<Object>> newColumns = Arrays.asList(Arrays.asList("D1", "D2", "D3", "D4", "D5"), Collections.emptyList(),
                Arrays.asList(100, 200, 300, 400, 500));

        dataset.addColumns(newColumnNames, newColumns);

        assertEquals(7, dataset.columnCount());
        assertEquals("D1", dataset.get(0, dataset.getColumnIndex("data")));
        assertNull(dataset.get(0, dataset.getColumnIndex("empty")));
        assertEquals(100, (Integer) dataset.get(0, dataset.getColumnIndex("moreData")));
    }

    @Test
    @DisplayName("Should handle columns with null values")
    public void testAddColumnsWithNullValues() {
        List<String> newColumnNames = Arrays.asList("nullable1", "nullable2");
        List<Collection<Object>> newColumns = Arrays.asList(Arrays.asList("A", null, "C", null, "E"), Arrays.asList(null, 2, null, 4, null));

        dataset.addColumns(newColumnNames, newColumns);

        assertEquals(6, dataset.columnCount());
        assertEquals("A", dataset.get(0, dataset.getColumnIndex("nullable1")));
        assertNull(dataset.get(1, dataset.getColumnIndex("nullable1")));
        assertNull(dataset.get(0, dataset.getColumnIndex("nullable2")));
        assertEquals(2, (Integer) dataset.get(1, dataset.getColumnIndex("nullable2")));
    }

    @Test
    @DisplayName("Should handle empty column names list")
    public void testAddColumnsWithEmptyNamesList() {
        List<String> newColumnNames = Collections.emptyList();
        List<Collection<Object>> newColumns = Collections.emptyList();

        int originalColumnCount = dataset.columnCount();
        dataset.addColumns(newColumnNames, newColumns);

        assertEquals(originalColumnCount, dataset.columnCount());
    }

    @Test
    @DisplayName("Should add multiple columns with different collection types")
    public void testAddColumnsWithDifferentCollectionTypes() {
        List<String> newColumnNames = Arrays.asList("list", "set", "arrayList");
        List<Collection<Object>> newColumns = Arrays.asList(Arrays.asList(1, 2, 3, 4, 5), new HashSet<>(Arrays.asList("A", "B", "C", "D", "E")),
                new ArrayList<>(Arrays.asList(true, false, true, false, true)));

        dataset.addColumns(newColumnNames, newColumns);

        assertEquals(7, dataset.columnCount());
        assertTrue(dataset.containsColumn("list"));
        assertTrue(dataset.containsColumn("set"));
        assertTrue(dataset.containsColumn("arrayList"));
    }

    @Test
    @DisplayName("Should throw exception when column names and columns size mismatch")
    public void testAddColumnsWithSizeMismatch() {
        List<String> newColumnNames = Arrays.asList("col1", "col2", "col3");
        List<Collection<Object>> newColumns = Arrays.asList(Arrays.asList(1, 2, 3, 4, 5), Arrays.asList(6, 7, 8, 9, 10));

        assertThrows(IllegalArgumentException.class, () -> dataset.addColumns(newColumnNames, newColumns));
    }

    @Test
    @DisplayName("Should throw exception when column size doesn't match dataset size")
    public void testAddColumnsWithIncorrectColumnSize() {
        List<String> newColumnNames = Arrays.asList("col1", "col2");
        List<Collection<Object>> newColumns = Arrays.asList(Arrays.asList(1, 2, 3), Arrays.asList(4, 5, 6, 7, 8, 9));

        assertThrows(IllegalArgumentException.class, () -> dataset.addColumns(newColumnNames, newColumns));
    }

    @Test
    @DisplayName("Should throw exception for duplicate column names")
    public void testAddColumnsWithDuplicateNames() {
        List<String> newColumnNames = Arrays.asList("id", "newCol");
        List<Collection<Object>> newColumns = Arrays.asList(Arrays.asList(10, 20, 30, 40, 50), Arrays.asList("A", "B", "C", "D", "E"));

        assertThrows(IllegalArgumentException.class, () -> dataset.addColumns(newColumnNames, newColumns));
    }

    @Test
    @DisplayName("Should throw exception for empty column names")
    public void testAddColumnsWithEmptyColumnName() {
        List<String> newColumnNames = Arrays.asList("valid", "", "alsoValid");
        List<Collection<Object>> newColumns = Arrays.asList(Arrays.asList(1, 2, 3, 4, 5), Arrays.asList(6, 7, 8, 9, 10), Arrays.asList(11, 12, 13, 14, 15));

        assertThrows(IllegalArgumentException.class, () -> dataset.addColumns(newColumnNames, newColumns));
    }

    @Test
    @DisplayName("Should throw exception when adding to frozen dataset")
    public void testAddColumnsOnFrozenDataset() {
        dataset.freeze();

        List<String> newColumnNames = Arrays.asList("col1");
        List<Collection<Object>> newColumns = Arrays.asList(Arrays.asList(1, 2, 3, 4, 5));

        assertThrows(IllegalStateException.class, () -> dataset.addColumns(newColumnNames, newColumns));
    }

    @Test
    @DisplayName("Should throw exception for invalid position")
    public void testAddColumnsWithInvalidPosition() {
        List<String> newColumnNames = Arrays.asList("col1");
        List<Collection<Object>> newColumns = Arrays.asList(Arrays.asList(1, 2, 3, 4, 5));

        assertThrows(IndexOutOfBoundsException.class, () -> dataset.addColumns(100, newColumnNames, newColumns));

        assertThrows(IndexOutOfBoundsException.class, () -> dataset.addColumns(-1, newColumnNames, newColumns));
    }

    @Test
    public void testAddRow() {
        Dataset ds = dataset.copy();
        ds.addRow(Arrays.asList(5, "Eve", 27, 52000.0));

        assertEquals(6, ds.size());
        assertEquals(Integer.valueOf(5), ds.get(5, 0));
        assertEquals("Eve", ds.get(5, 1));
    }

    @Test
    public void testAddRow_AtPosition() {
        Dataset ds = dataset.copy();
        ds.addRow(1, Arrays.asList(5, "Eve", 27, 52000.0));

        assertEquals(6, ds.size());
        assertEquals(Integer.valueOf(5), ds.get(1, 0));
        assertEquals(Integer.valueOf(2), ds.get(2, 0));
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
    public void testAddRowWithPosition() {
        Dataset ds = dataset.copy();
        ds.addRow(1, new Object[] { 5, "Eve", 32, 58000.0 });
        assertEquals(6, ds.size());
        assertEquals("Eve", ds.get(1, 1));
    }

    @Test
    public void testAddRowAtPosition() {
        Dataset dataset = testDataset.copy();
        Object[] newRow = { 5, "Eve", 32, 58000.0 };
        dataset.addRow(1, newRow);

        assertEquals(5, dataset.size());
        dataset.moveToRow(1);
        assertEquals(5, dataset.getInt("id"));
        assertEquals("Eve", dataset.get("name"));
    }

    @Test
    public void testAddRowWithCollection() {
        List<Object> newRow = Arrays.asList(6, "Frank", 45, 90000.0);
        dataset.addRow(newRow);

        assertEquals(6, dataset.size());
        assertEquals((Integer) 6, dataset.get(5, 0));
        assertEquals("Frank", dataset.get(5, 1));
    }

    @Test
    public void testAddRowWithMap() {
        Map<String, Object> newRow = new HashMap<>();
        newRow.put("id", 6);
        newRow.put("name", "Frank");
        newRow.put("age", 45);
        newRow.put("salary", 90000.0);

        dataset.addRow(newRow);

        assertEquals(6, dataset.size());
        assertEquals((Integer) 6, dataset.get(5, 0));
        assertEquals("Frank", dataset.get(5, 1));
    }

    @Test
    public void testAddRowWithBean() {
        Person person = new Person(6, "Frank", 45, 90000.0);
        dataset.addRow(person);

        assertEquals(6, dataset.size());
        assertEquals((Integer) 6, dataset.get(5, 0));
        assertEquals("Frank", dataset.get(5, 1));
    }

    @Test
    public void addRow_atPosition() {
        sampleDataset.addRow(1, new Object[] { 0, "Zero", 0 });
        assertEquals(4, sampleDataset.size());
        assertEquals("Zero", sampleDataset.moveToRow(1).get("Name"));
        assertEquals("Bob", sampleDataset.moveToRow(2).get("Name"));
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
    public void testAddRows_Collection() {
        Dataset ds = dataset.copy();
        List<Object[]> newRows = new ArrayList<>();
        newRows.add(new Object[] { 5, "Eve", 27, 52000.0 });
        newRows.add(new Object[] { 6, "Frank", 32, 58000.0 });

        ds.addRows(newRows);

        assertEquals(7, ds.size());
        assertEquals(Integer.valueOf(5), ds.get(5, 0));
        assertEquals("Eve", ds.get(5, 1));
        assertEquals(Integer.valueOf(6), ds.get(6, 0));
        assertEquals("Frank", ds.get(6, 1));
    }

    @Test
    public void testAddRows_AtPosition() {
        Dataset ds = dataset.copy();
        List<Object[]> newRows = new ArrayList<>();
        newRows.add(new Object[] { 5, "Eve", 27, 52000.0 });
        newRows.add(new Object[] { 6, "Frank", 32, 58000.0 });

        ds.addRows(1, newRows);

        assertEquals(7, ds.size());
        assertEquals(Integer.valueOf(5), ds.get(1, 0));
        assertEquals("Eve", ds.get(1, 1));
        assertEquals(Integer.valueOf(6), ds.get(2, 0));
        assertEquals("Frank", ds.get(2, 1));
        assertEquals(Integer.valueOf(2), ds.get(3, 0));
    }

    @Test
    public void testAddRows() {
        Dataset ds = dataset.copy();
        List<Object[]> newRows = Arrays.asList(new Object[] { 5, "Eve", 32, 58000.0 }, new Object[] { 6, "Frank", 29, 52000.0 });
        ds.addRows(newRows);
        assertEquals(7, ds.size());
        assertEquals("Eve", ds.get(5, 1));
        assertEquals("Frank", ds.get(6, 1));
    }

    @Test
    public void testAddRowsAtPosition() {
        Dataset dataset = testDataset.copy();
        List<Object[]> newRows = Arrays.asList(new Object[] { 5, "Eve", 32, 58000.0 }, new Object[] { 6, "Frank", 29, 52000.0 });
        dataset.addRows(1, newRows);

        assertEquals(6, dataset.size());
        dataset.moveToRow(1);
        assertEquals(5, dataset.getInt("id"));
        dataset.moveToRow(2);
        assertEquals(6, dataset.getInt("id"));
    }

    @Test
    @DisplayName("Should add rows at beginning of dataset")
    public void testAddRowsAtBeginning() {
        Collection<Object[]> rows = Arrays.asList(new Object[] { 0, "First", 20, 40000.0 }, new Object[] { -1, "Second", 22, 42000.0 });

        dataset.addRows(0, rows);

        assertEquals(7, dataset.size());
        assertEquals((Integer) 0, dataset.get(0, 0));
        assertEquals("First", dataset.get(0, 1));
        assertEquals((Integer) (-1), dataset.get(1, 0));
        assertEquals("Second", dataset.get(1, 1));
        assertEquals((Integer) 1, dataset.get(2, 0));
        assertEquals("John", dataset.get(2, 1));
    }

    @Test
    @DisplayName("Should add rows at end of dataset")
    public void testAddRowsAtEnd() {

        Collection<Object[]> rows = new ArrayList<>();
        rows.add(new Object[] { 6, "Last", 60, 120000.0 });

        int originalSize = dataset.size();
        dataset.addRows(originalSize, rows);

        assertEquals(originalSize + 1, dataset.size());
        assertEquals((Integer) 6, dataset.get(originalSize, 0));
        assertEquals("Last", dataset.get(originalSize, 1));
    }

    @Test
    public void testAddRowsModCountIncrement() {
        Object[] row = { 4, "David", 40, 100.0 };
        Collection<Object[]> rows = Collections.singletonList(row);

        int sizeBefore = dataset.size();
        dataset.addRows(rows);

        assertEquals(sizeBefore + 1, dataset.size());
    }

    @Test
    @DisplayName("Should add multiple rows with Object arrays")
    public void testAddRowsWithObjectArrays() {
        Collection<Object[]> rows = Arrays.asList(new Object[] { 6, "Frank", 45, 90000.0 }, new Object[] { 7, "Grace", 32, 75000.0 },
                new Object[] { 8, "Henry", 28, 65000.0 });

        int originalSize = dataset.size();
        dataset.addRows(rows);

        assertEquals(originalSize + 3, dataset.size());
        assertEquals((Integer) 6, dataset.get(5, 0));
        assertEquals("Frank", dataset.get(5, 1));
        assertEquals((Integer) 7, dataset.get(6, 0));
        assertEquals("Grace", dataset.get(6, 1));
        assertEquals((Integer) 8, dataset.get(7, 0));
        assertEquals("Henry", dataset.get(7, 1));
    }

    @Test
    @DisplayName("Should add multiple rows at specific position with Object arrays")
    public void testAddRowsAtPositionWithObjectArrays() {
        Collection<Object[]> rows = Arrays.asList(new Object[] { 10, "Insert1", 50, 95000.0 }, new Object[] { 11, "Insert2", 55, 100000.0 });

        int originalSize = dataset.size();
        dataset.addRows(2, rows);

        assertEquals(originalSize + 2, dataset.size());
        assertEquals("Bob", dataset.get(4, 1));
        assertEquals((Integer) 10, dataset.get(2, 0));
        assertEquals("Insert1", dataset.get(2, 1));
        assertEquals((Integer) 11, dataset.get(3, 0));
        assertEquals("Insert2", dataset.get(3, 1));
    }

    @Test
    @DisplayName("Should add multiple rows with Lists")
    public void testAddRowsWithLists() {
        Collection<List<Object>> rows = Arrays.asList(Arrays.asList(6, "Frank", 45, 90000.0), Arrays.asList(7, "Grace", 32, 75000.0),
                Arrays.asList(8, "Henry", 28, 65000.0));

        int originalSize = dataset.size();
        dataset.addRows(rows);

        assertEquals(originalSize + 3, dataset.size());
        assertEquals((Integer) 6, dataset.get(5, 0));
        assertEquals("Frank", dataset.get(5, 1));
        assertEquals((Integer) 7, dataset.get(6, 0));
        assertEquals("Grace", dataset.get(6, 1));
    }

    @Test
    @DisplayName("Should add multiple rows with Maps")
    public void testAddRowsWithMaps() {
        Map<String, Object> row1 = new HashMap<>();
        row1.put("id", 6);
        row1.put("name", "Frank");
        row1.put("age", 45);
        row1.put("salary", 90000.0);

        Map<String, Object> row2 = new HashMap<>();
        row2.put("id", 7);
        row2.put("name", "Grace");
        row2.put("age", 32);
        row2.put("salary", 75000.0);

        Collection<Map<String, Object>> rows = Arrays.asList(row1, row2);

        int originalSize = dataset.size();
        dataset.addRows(rows);

        assertEquals(originalSize + 2, dataset.size());
        assertEquals((Integer) 6, dataset.get(5, 0));
        assertEquals("Frank", dataset.get(5, 1));
        assertEquals((Integer) 7, dataset.get(6, 0));
        assertEquals("Grace", dataset.get(6, 1));
    }

    @Test
    @DisplayName("Should add multiple rows with Bean objects")
    public void testAddRowsWithBeans() {
        Collection<Person> rows = Arrays.asList(new Person(6, "Frank", 45, 90000.0), new Person(7, "Grace", 32, 75000.0), new Person(8, "Henry", 28, 65000.0));

        int originalSize = dataset.size();
        dataset.addRows(rows);

        assertEquals(originalSize + 3, dataset.size());
        assertEquals((Integer) 6, dataset.get(5, 0));
        assertEquals("Frank", dataset.get(5, 1));
        assertEquals((Integer) 45, dataset.get(5, 2));
        assertEquals((Double) 90000.0, dataset.get(5, 3));
    }

    @Test
    @DisplayName("Should handle empty collection in addRows")
    public void testAddRowsWithEmptyCollection() {
        Collection<Object[]> rows = Collections.emptyList();

        int originalSize = dataset.size();
        dataset.addRows(rows);

        assertEquals(originalSize, dataset.size());
    }

    @Test
    @DisplayName("Should handle null values in rows")
    public void testAddRowsWithNullValues() {
        Collection<Object[]> rows = Arrays.asList(new Object[] { 6, null, 45, 90000.0 }, new Object[] { 7, "Grace", null, 75000.0 },
                new Object[] { 8, "Henry", 28, null });

        int originalSize = dataset.size();
        dataset.addRows(rows);

        assertEquals(originalSize + 3, dataset.size());
        assertNull(dataset.get(5, 1));
        assertNull(dataset.get(6, 2));
        assertNull(dataset.get(7, 3));
    }

    @Test
    @DisplayName("Should add single row using addRows")
    public void testAddRowsWithSingleRow() {
        Collection<Object[]> rows = Collections.singletonList(new Object[] { 6, "Single", 45, 90000.0 });

        int originalSize = dataset.size();
        dataset.addRows(rows);

        assertEquals(originalSize + 1, dataset.size());
        assertEquals((Integer) 6, dataset.get(originalSize, 0));
        assertEquals("Single", dataset.get(originalSize, 1));
    }

    @Test
    @DisplayName("Should preserve row order when adding multiple rows")
    public void testAddRowsPreservesOrder() {
        Collection<Object[]> rows = new ArrayList<>();
        for (int i = 10; i <= 15; i++) {
            rows.add(new Object[] { i, "Person" + i, 20 + i, 50000.0 + i * 1000 });
        }

        int originalSize = dataset.size();
        dataset.addRows(rows);

        assertEquals(originalSize + 6, dataset.size());

        for (int i = 0; i < 6; i++) {
            assertEquals((Integer) (10 + i), dataset.get(originalSize + i, 0));
            assertEquals("Person" + (10 + i), dataset.get(originalSize + i, 1));
        }
    }

    @Test
    public void testAddRowsOnFrozenDataset() {
        dataset.freeze();

        Object[] row = { 4, "David", 40 };
        Collection<Object[]> rows = Collections.singletonList(row);

        assertThrows(IllegalStateException.class, () -> dataset.addRows(rows));
    }

    @Test
    @DisplayName("Should throw exception when row has fewer columns than dataset")
    public void testAddRowsWithInsufficientColumns() {
        Collection<Object[]> rows = new ArrayList<>();
        rows.add(new Object[] { 6, "Frank" });

        assertThrows(IllegalArgumentException.class, () -> dataset.addRows(rows));
    }

    @Test
    @DisplayName("Should throw exception for invalid position in addRows")
    public void testAddRowsWithInvalidPosition() {
        Collection<Object[]> rows = new ArrayList<>();
        rows.add(new Object[] { 6, "Test", 45, 90000.0 });

        assertThrows(IndexOutOfBoundsException.class, () -> dataset.addRows(100, rows));

        assertThrows(IndexOutOfBoundsException.class, () -> dataset.addRows(-1, rows));
    }

    @Test
    @DisplayName("Should add rows with mixed supported representations")
    public void testAddRowsWithMixedTypes() {
        Collection<Object> mixedRows = Arrays.asList(new Object[] { 6, "Array", 45, 90000.0 }, Arrays.asList(7, "List", 32, 75000.0));

        final int originalSize = dataset.size();
        dataset.addRows(mixedRows);

        assertEquals(originalSize + 2, dataset.size());
        assertEquals("Array", dataset.get(originalSize, 1));
        assertEquals("List", dataset.get(originalSize + 1, 1));
    }

    @Test
    @DisplayName("Should throw exception for unsupported row type")
    public void testAddRowsWithUnsupportedType() {
        Collection<String> rows = Arrays.asList("row1", "row2");

        assertThrows(IllegalArgumentException.class, () -> dataset.addRows(rows));
    }

    @Test
    public void testAddMultipleColumns() {
        Dataset ds = dataset.copy();
        List<String> newColumnNames = Arrays.asList("col1", "col2");
        List<List<Object>> newColumns = Arrays.asList(Arrays.asList("A", "B", "C", "D", "E"), Arrays.asList(1, 2, 3, 4, 5));
        ds.addColumns(newColumnNames, newColumns);
        assertEquals(6, ds.columnCount());
        assertEquals("col1", ds.getColumnName(4));
        assertEquals("col2", ds.getColumnName(5));
    }

    @Test
    @DisplayName("Should add large number of columns")
    public void testAddManyColumns() {
        List<String> newColumnNames = new ArrayList<>();
        List<Collection<Object>> newColumns = new ArrayList<>();

        for (int i = 0; i < 10; i++) {
            newColumnNames.add("col" + i);
            List<Object> column = new ArrayList<>();
            for (int j = 0; j < dataset.size(); j++) {
                column.add("R" + j + "C" + i);
            }
            newColumns.add(column);
        }

        int originalColumnCount = dataset.columnCount();
        dataset.addColumns(newColumnNames, newColumns);

        assertEquals(originalColumnCount + 10, dataset.columnCount());

        assertEquals("R0C0", dataset.get(0, dataset.getColumnIndex("col0")));
        assertEquals("R4C9", dataset.get(4, dataset.getColumnIndex("col9")));
    }

    @Test
    @DisplayName("addRow(null) must throw IllegalArgumentException, not NullPointerException")
    public void testAddRowNullThrowsIAE() {
        Dataset ds = new RowDataset(new ArrayList<>(Arrays.asList("a", "b")),
                new ArrayList<>(Arrays.asList(new ArrayList<>(Arrays.asList(1)), new ArrayList<>(Arrays.asList(2)))));
        assertThrows(IllegalArgumentException.class, () -> ds.addRow(null));
        assertThrows(IllegalArgumentException.class, () -> ds.addRow(0, null));
    }

}

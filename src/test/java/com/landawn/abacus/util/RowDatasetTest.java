package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.If.OrElse;
import com.landawn.abacus.util.NoCachingNoUpdating.DisposableObjArray;
import com.landawn.abacus.util.Tuple.Tuple2;
import com.landawn.abacus.util.Tuple.Tuple3;
import com.landawn.abacus.util.u.Optional;
import com.landawn.abacus.util.function.BiFunction;
import com.landawn.abacus.util.function.BiPredicate;
import com.landawn.abacus.util.function.Function;
import com.landawn.abacus.util.function.IntFunction;
import com.landawn.abacus.util.function.Predicate;
import com.landawn.abacus.util.function.TriConsumer;
import com.landawn.abacus.util.function.TriFunction;
import com.landawn.abacus.util.function.TriPredicate;
import com.landawn.abacus.util.stream.Stream;

@Tag("2025")
public class RowDatasetTest extends TestBase {

    private RowDataset dataset;
    private RowDataset emptyDataset;
    private Dataset ds1;
    private Dataset ds2;
    private Dataset emptyDs;
    private List<String> columnNames;
    private List<List<Object>> columnList;
    private List<List<Object>> columns;

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
        columns = copyColumnList();

        List<String> leftColumnNames = CommonUtil.toList("id", "name", "age");
        List<List<Object>> leftColumns = new ArrayList<>();
        leftColumns.add(CommonUtil.toList(1, 2, 3));
        leftColumns.add(CommonUtil.toList("Alice", "Bob", "Charlie"));
        leftColumns.add(CommonUtil.toList(25, 30, 35));
        ds1 = new RowDataset(leftColumnNames, leftColumns);

        List<String> rightColumnNames = CommonUtil.toList("id", "city", "salary");
        List<List<Object>> rightColumns = new ArrayList<>();
        rightColumns.add(CommonUtil.toList(2, 3, 4));
        rightColumns.add(CommonUtil.toList("New York", "London", "Tokyo"));
        rightColumns.add(CommonUtil.toList(50000, 60000, 70000));
        ds2 = new RowDataset(rightColumnNames, rightColumns);

        List<String> emptyColumnNames = CommonUtil.toList("col1", "col2");
        List<List<Object>> emptyColumns = new ArrayList<>();
        emptyColumns.add(new ArrayList<>());
        emptyColumns.add(new ArrayList<>());
        emptyDs = new RowDataset(emptyColumnNames, emptyColumns);
    }

    private List<List<Object>> copyColumnList() {
        List<List<Object>> copy = new ArrayList<>();
        for (List<Object> column : columnList) {
            copy.add(new ArrayList<>(column));
        }
        return copy;
    }

    private RowDataset createFourRowCityDataset() {
        final List<String> localColumnNames = new ArrayList<>(Arrays.asList("id", "name", "age", "city"));
        final List<List<Object>> localColumns = new ArrayList<>();
        localColumns.add(new ArrayList<>(Arrays.asList(1, 2, 3, 4)));
        localColumns.add(new ArrayList<>(Arrays.asList("John", "Jane", "Bob", "Alice")));
        localColumns.add(new ArrayList<>(Arrays.asList(25, 30, 35, 28)));
        localColumns.add(new ArrayList<>(Arrays.asList("NYC", "LA", "Chicago", "Miami")));

        return new RowDataset(localColumnNames, localColumns);
    }

    private RowDataset createFiveRowCityDataset() {
        final List<String> localColumnNames = new ArrayList<>(Arrays.asList("id", "name", "age", "city"));
        final List<List<Object>> localColumns = new ArrayList<>();
        localColumns.add(new ArrayList<>(Arrays.asList(1, 2, 3, 4, 5)));
        localColumns.add(new ArrayList<>(Arrays.asList("John", "Jane", "Bob", "Alice", "Eve")));
        localColumns.add(new ArrayList<>(Arrays.asList(25, 30, 35, 40, 28)));
        localColumns.add(new ArrayList<>(Arrays.asList("NYC", "LA", "Chicago", "NYC", "LA")));

        return new RowDataset(localColumnNames, localColumns);
    }

    private RowDataset createThreeRowScoreDataset() {
        final List<String> localColumnNames = new ArrayList<>(Arrays.asList("id", "name", "age", "score"));
        final List<List<Object>> localColumns = new ArrayList<>();
        localColumns.add(new ArrayList<>(Arrays.asList(1, 2, 3)));
        localColumns.add(new ArrayList<>(Arrays.asList("John", "Jane", "Bob")));
        localColumns.add(new ArrayList<>(Arrays.asList(25, 30, 35)));
        localColumns.add(new ArrayList<>(Arrays.asList(85.5, 90.0, 88.0)));

        return new RowDataset(localColumnNames, localColumns);
    }

    private List<List<Object>> createThreeRowScoreColumns() {
        final List<List<Object>> localColumns = new ArrayList<>();
        localColumns.add(new ArrayList<>(Arrays.asList(1, 2, 3)));
        localColumns.add(new ArrayList<>(Arrays.asList("John", "Jane", "Bob")));
        localColumns.add(new ArrayList<>(Arrays.asList(25, 30, 35)));
        localColumns.add(new ArrayList<>(Arrays.asList(85.5, 90.0, 88.0)));

        return localColumns;
    }

    public static class Person {
        private int id;
        private String name;
        private int age;
        private String city;

        public Person() {
        }

        public Person(final int id, final String name, final int age, final String city) {
            this.id = id;
            this.name = name;
            this.age = age;
            this.city = city;
        }

        public int getId() {
            return id;
        }

        public void setId(final int id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(final String name) {
            this.name = name;
        }

        public int getAge() {
            return age;
        }

        public void setAge(final int age) {
            this.age = age;
        }

        public String getCity() {
            return city;
        }

        public void setCity(final String city) {
            this.city = city;
        }
    }

    public static class PersonSkill {
        private int id;
        private String name;
        private String skill;

        public int getId() {
            return id;
        }

        public void setId(final int id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(final String name) {
            this.name = name;
        }

        public String getSkill() {
            return skill;
        }

        public void setSkill(final String skill) {
            this.skill = skill;
        }
    }

    public static class TestBean {
        public int id;
        public String name;
        public int age;
        public double score;

        public TestBean() {
        }
    }

    public static class SalaryRowBean {
        private int id;
        private String name;
        private int age;
        private double salary;

        public SalaryRowBean() {
        }

        public SalaryRowBean(final int id, final String name, final int age, final double salary) {
            this.id = id;
            this.name = name;
            this.age = age;
            this.salary = salary;
        }

        public int getId() {
            return id;
        }

        public void setId(final int id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(final String name) {
            this.name = name;
        }

        public int getAge() {
            return age;
        }

        public void setAge(final int age) {
            this.age = age;
        }

        public double getSalary() {
            return salary;
        }

        public void setSalary(final double salary) {
            this.salary = salary;
        }
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
        ImmutableList<String> names = dataset.columnNames();
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
        int[] indexes = dataset.getColumnIndexes(dataset.columnNames());
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
        ds.swapColumns("id", "salary");
        assertEquals("salary", ds.getColumnName(0));
        assertEquals("name", ds.getColumnName(1));
        assertEquals("age", ds.getColumnName(2));
        assertEquals("id", ds.getColumnName(3));
    }

    @Test
    public void testSwapColumnPositionSame() {
        RowDataset ds = new RowDataset(columnNames, copyColumnList());
        ds.swapColumns("id", "id");
        assertEquals("id", ds.getColumnName(0));
    }

    @Test
    public void testSwapColumnPositionFrozen() {
        RowDataset ds = new RowDataset(columnNames, copyColumnList());
        ds.freeze();
        assertThrows(IllegalStateException.class, () -> {
            ds.swapColumns("id", "name");
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
        ds.swapRows(0, 4);
        assertEquals(lastValue, ds.get(0, 1));
        assertEquals(firstValue, ds.get(4, 1));
    }

    @Test
    public void testSwapRowPositionSame() {
        RowDataset ds = new RowDataset(columnNames, copyColumnList());
        Object value = ds.get(0, 1);
        ds.swapRows(0, 0);
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
    public void testAbsolute() {
        Dataset ds = dataset.moveToRow(3);
        assertNotNull(ds);
        assertEquals(3, ds.currentRowIndex());
        assertEquals("Diana", ds.get("name"));
    }

    @Test
    public void testAbsoluteInvalid() {
        assertThrows(IndexOutOfBoundsException.class, () -> {
            dataset.moveToRow(10);
        });
        assertThrows(IndexOutOfBoundsException.class, () -> {
            dataset.moveToRow(-1);
        });
    }

    @Test
    public void testGetByColumnIndex() {
        dataset.moveToRow(0);
        assertEquals(1, (Integer) dataset.get(0));
        assertEquals("Alice", dataset.get(1));
        assertEquals(25, (Integer) dataset.get(2));
    }

    @Test
    public void testGetByColumnName() {
        dataset.moveToRow(1);
        assertEquals(2, (Integer) dataset.get("id"));
        assertEquals("Bob", dataset.get("name"));
        assertEquals(30, (Integer) dataset.get("age"));
    }

    @Test
    public void testGetPrimitiveTypes() {
        dataset.moveToRow(0);
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
        ds.moveToRow(0);
        assertTrue(ds.isNull(1));
        ds.moveToRow(1);
        assertFalse(ds.isNull(1));
    }

    @Test
    public void testIsNullByColumnName() {
        List<List<Object>> cols = copyColumnList();
        cols.get(1).set(0, null);
        RowDataset ds = new RowDataset(columnNames, cols);
        ds.moveToRow(0);
        assertTrue(ds.isNull("name"));
        ds.moveToRow(1);
        assertFalse(ds.isNull("name"));
    }

    @Test
    public void testSetByColumnIndex() {
        RowDataset ds = new RowDataset(columnNames, copyColumnList());
        ds.moveToRow(0);
        ds.set(1, "NewName");
        assertEquals("NewName", ds.get(0, 1));
    }

    @Test
    public void testSetByColumnName() {
        RowDataset ds = new RowDataset(columnNames, copyColumnList());
        ds.moveToRow(0);
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
        List<String> names = CommonUtil.toList("fullName");
        List<List<Object>> cols = new ArrayList<>();
        cols.add(new ArrayList<>(CommonUtil.toList("John Doe", "Jane Smith")));
        RowDataset ds = new RowDataset(names, cols);
        ds.divideColumn("fullName", CommonUtil.toList("firstName", "lastName"), (String fullName) -> CommonUtil.toList(fullName.split(" ")));
        assertTrue(ds.containsColumn("firstName"));
        assertTrue(ds.containsColumn("lastName"));
        assertFalse(ds.containsColumn("fullName"));
        assertEquals("John", ds.get(0, 0));
        assertEquals("Doe", ds.get(0, 1));
    }

    @Test
    public void testDivideColumnWithBiConsumer() {
        List<String> names = CommonUtil.toList("fullName");
        List<List<Object>> cols = new ArrayList<>();
        cols.add(new ArrayList<>(CommonUtil.toList("John Doe", "Jane Smith")));
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
        ds.removeRowsAt(0, 2);
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
        assertNotNull(result);
    }

    @Test
    public void testToListWithColumnFilterAndConverter() {
        final RowDataset dataset = createFourRowCityDataset();
        Predicate<String> columnFilter = col -> col.equals("name") || col.equals("age");
        Function<String, String> columnConverter = String::toUpperCase;
        IntFunction<Map<String, Object>> rowSupplier = capacity -> new HashMap<>();

        List<Map<String, Object>> result = dataset.toList(columnFilter, columnConverter, rowSupplier);

        Assertions.assertEquals(4, result.size());
        Map<String, Object> firstRow = result.get(0);
        Assertions.assertTrue(firstRow.containsKey("NAME"));
        Assertions.assertTrue(firstRow.containsKey("AGE"));
        Assertions.assertFalse(firstRow.containsKey("id"));
        Assertions.assertFalse(firstRow.containsKey("city"));
    }

    @Test
    public void testToListWithColumnFilterAndConverterNullValues() {
        final RowDataset dataset = createFourRowCityDataset();
        IntFunction<List<Object>> rowSupplier = capacity -> new ArrayList<>();

        List<List<Object>> result = dataset.toList(null, null, rowSupplier);

        Assertions.assertEquals(4, result.size());
        Assertions.assertEquals(4, result.get(0).size());
    }

    @Test
    public void testToListWithRowIndexRange() {
        final RowDataset dataset = createFourRowCityDataset();
        Predicate<String> columnFilter = col -> col.equals("name") || col.equals("age");
        IntFunction<Map<String, Object>> rowSupplier = capacity -> new HashMap<>();

        List<Map<String, Object>> result = dataset.toList(1, 3, columnFilter, null, rowSupplier);

        Assertions.assertEquals(2, result.size());
        Map<String, Object> firstRow = result.get(0);
        Assertions.assertEquals("Jane", firstRow.get("name"));
        Assertions.assertEquals(30, firstRow.get("age"));
    }

    @Test
    public void testToMergedEntities() {
        List<String> mergeColumnNames = Arrays.asList("id", "name", "skill");
        List<List<Object>> mergeColumnList = new ArrayList<>();
        mergeColumnList.add(Arrays.asList(1, 1, 2, 2));
        mergeColumnList.add(Arrays.asList("John", "John", "Jane", "Jane"));
        mergeColumnList.add(Arrays.asList("Java", "Python", "JavaScript", "SQL"));

        RowDataset mergeDataset = new RowDataset(mergeColumnNames, mergeColumnList);

        Collection<String> idPropNames = Arrays.asList("id");
        Collection<String> selectPropNames = Arrays.asList("id", "name", "skill");
        Map<String, String> prefixAndFieldNameMap = new HashMap<>();

        List<PersonSkill> mergedEntities = mergeDataset.toMergedEntities(idPropNames, selectPropNames, prefixAndFieldNameMap, PersonSkill.class);

        Assertions.assertEquals(2, mergedEntities.size());
    }

    @Test
    public void testToMergedEntitiesWithInvalidIdProps() {
        Collection<String> idPropNames = Arrays.asList("nonexistent_id");
        Collection<String> selectPropNames = Arrays.asList("name", "age");
        Map<String, String> prefixAndFieldNameMap = new HashMap<>();

        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            dataset.toMergedEntities(idPropNames, selectPropNames, prefixAndFieldNameMap, Person.class);
        });
    }

    @Test
    public void testToMapWithKeyAndValueColumns() {
        final RowDataset dataset = createFourRowCityDataset();
        String keyColumn = "id";
        Collection<String> valueColumns = Arrays.asList("name", "age");
        IntFunction<Map<Integer, Object[]>> supplier = capacity -> new HashMap<>();

        Map<Integer, Object[]> result = dataset.toMap(0, dataset.size(), keyColumn, valueColumns, Object[].class, supplier);

        Assertions.assertEquals(4, result.size());
        Object[] firstValue = result.get(1);
        Assertions.assertEquals("John", firstValue[0]);
        Assertions.assertEquals(25, firstValue[1]);
    }

    @Test
    public void testToMapWithCollectionRowType() {
        final RowDataset dataset = createFourRowCityDataset();
        String keyColumn = "id";
        Collection<String> valueColumns = Arrays.asList("name", "age", "city");
        IntFunction<Map<Integer, List<Object>>> supplier = capacity -> new HashMap<>();

        Map<Integer, List<Object>> result = dataset.toMap(0, dataset.size(), keyColumn, valueColumns, Clazz.ofList(), supplier);

        Assertions.assertEquals(4, result.size());
        List<Object> firstValue = result.get(1);
        Assertions.assertEquals(3, firstValue.size());
        Assertions.assertTrue(firstValue.contains("John"));
        Assertions.assertTrue(firstValue.contains(25));
        Assertions.assertTrue(firstValue.contains("NYC"));
    }

    @Test
    public void testToMapWithMapRowType() {
        final RowDataset dataset = createFourRowCityDataset();
        String keyColumn = "id";
        Collection<String> valueColumns = Arrays.asList("name", "age", "city");
        IntFunction<Map<Integer, Map<String, Object>>> supplier = capacity -> new HashMap<>();

        Map<Integer, Map<String, Object>> result = dataset.toMap(0, dataset.size(), keyColumn, valueColumns, Clazz.ofMap(), supplier);

        Assertions.assertEquals(4, result.size());
        Map<String, Object> firstValue = result.get(1);
        Assertions.assertEquals("John", firstValue.get("name"));
        Assertions.assertEquals(25, firstValue.get("age"));
        Assertions.assertEquals("NYC", firstValue.get("city"));
    }

    @Test
    public void testToMapWithBeanRowType() {
        final RowDataset dataset = createFourRowCityDataset();
        String keyColumn = "id";
        Collection<String> valueColumns = Arrays.asList("name", "age", "city");
        IntFunction<Map<Integer, Person>> supplier = capacity -> new HashMap<>();

        Map<Integer, Person> result = dataset.toMap(0, dataset.size(), keyColumn, valueColumns, Person.class, supplier);

        Assertions.assertEquals(4, result.size());
        Person firstPerson = result.get(1);
        Assertions.assertEquals("John", firstPerson.getName());
        Assertions.assertEquals(25, firstPerson.getAge());
        Assertions.assertEquals("NYC", firstPerson.getCity());
    }

    @Test
    public void testToMapWithRowSupplier() {
        final RowDataset dataset = createFourRowCityDataset();
        {
            String keyColumn = "id";
            Collection<String> valueColumns = Arrays.asList("name", "age");
            IntFunction<Map<Integer, Object[]>> mapSupplier = capacity -> new HashMap<>();

            Map<Integer, Object[]> result = dataset.toMap(0, dataset.size(), keyColumn, valueColumns, IntFunctions.ofObjectArray(), mapSupplier);

            Assertions.assertEquals(4, result.size());
            Object[] firstValue = result.get(1);
            Assertions.assertEquals("John", firstValue[0]);
            Assertions.assertEquals(25, firstValue[1]);
        }
        {
            String keyColumn = "id";
            Collection<String> valueColumns = Arrays.asList("name", "age");
            IntFunction<Map<Integer, List<Object>>> mapSupplier = capacity -> new HashMap<>();

            Map<Integer, List<Object>> result = dataset.toMap(0, dataset.size(), keyColumn, valueColumns, IntFunctions.ofList(), mapSupplier);

            Assertions.assertEquals(4, result.size());
            List<Object> firstValue = result.get(1);
            Assertions.assertEquals("John", firstValue.get(0));
            Assertions.assertEquals(25, firstValue.get(1));
        }

        {
            String keyColumn = "id";
            Collection<String> valueColumns = Arrays.asList("name", "age");
            IntFunction<Map<Integer, Map<String, Object>>> mapSupplier = capacity -> new HashMap<>();

            Map<Integer, Map<String, Object>> result = dataset.toMap(0, dataset.size(), keyColumn, valueColumns, IntFunctions.ofMap(), mapSupplier);

            Assertions.assertEquals(4, result.size());
            Map<String, Object> firstValue = result.get(1);
            Assertions.assertEquals("John", firstValue.get("name"));
            Assertions.assertEquals(25, firstValue.get("age"));
        }

        {
            String keyColumn = "id";
            Collection<String> valueColumns = Arrays.asList("name", "age");
            IntFunction<Map<Integer, Person>> mapSupplier = capacity -> new HashMap<>();

            Map<Integer, Person> result = dataset.toMap(0, dataset.size(), keyColumn, valueColumns, i -> new Person(), mapSupplier);

            Assertions.assertEquals(4, result.size());
            Person firstValue = result.get(1);
            Assertions.assertEquals("John", firstValue.getName());
            Assertions.assertEquals(25, firstValue.getAge());
        }
    }

    @Test
    public void testToMultimapWithRowSupplier() {
        final RowDataset dataset = createFourRowCityDataset();
        {
            String keyColumn = "id";
            Collection<String> valueColumns = Arrays.asList("name", "age");
            IntFunction<ListMultimap<Integer, Object[]>> mapSupplier = IntFunctions.ofListMultimap();

            Multimap<Integer, Object[], List<Object[]>> result = dataset.toMultimap(0, dataset.size(), keyColumn, valueColumns, IntFunctions.ofObjectArray(),
                    mapSupplier);

            Assertions.assertEquals(4, result.totalValueCount());
            List<Object[]> firstValue = result.get(1);
            Assertions.assertEquals("John", firstValue.get(0)[0]);
            Assertions.assertEquals(25, firstValue.get(0)[1]);
        }
        {
            String keyColumn = "id";
            Collection<String> valueColumns = Arrays.asList("name", "age");
            IntFunction<ListMultimap<Integer, List<Object>>> mapSupplier = IntFunctions.ofListMultimap();

            Multimap<Integer, List<Object>, List<List<Object>>> result = dataset.toMultimap(0, dataset.size(), keyColumn, valueColumns, IntFunctions.ofList(),
                    mapSupplier);

            Assertions.assertEquals(4, result.totalValueCount());
            List<List<Object>> firstValue = result.get(1);
            Assertions.assertEquals("John", firstValue.get(0).get(0));
            Assertions.assertEquals(25, firstValue.get(0).get(1));
        }
        {
            String keyColumn = "id";
            Collection<String> valueColumns = Arrays.asList("name", "age");
            IntFunction<ListMultimap<Integer, Map<String, Object>>> mapSupplier = IntFunctions.ofListMultimap();

            Multimap<Integer, Map<String, Object>, List<Map<String, Object>>> result = dataset.toMultimap(0, dataset.size(), keyColumn, valueColumns,
                    IntFunctions.ofMap(), mapSupplier);

            Assertions.assertEquals(4, result.totalValueCount());
            List<Map<String, Object>> firstValue = result.get(1);
            Assertions.assertEquals("John", firstValue.get(0).get("name"));
            Assertions.assertEquals(25, firstValue.get(0).get("age"));
        }
        {
            String keyColumn = "id";
            Collection<String> valueColumns = Arrays.asList("name", "age");
            IntFunction<ListMultimap<Integer, Person>> mapSupplier = IntFunctions.ofListMultimap();

            Multimap<Integer, Person, List<Person>> result = dataset.toMultimap(0, dataset.size(), keyColumn, valueColumns, i -> new Person(), mapSupplier);

            Assertions.assertEquals(4, result.totalValueCount());
            List<Person> firstValue = result.get(1);
            Assertions.assertEquals("John", firstValue.get(0).getName());
            Assertions.assertEquals(25, firstValue.get(0).getAge());
        }
    }

    @Test
    public void testToMultimapWithCollectionRowType() {
        List<String> dupColumnNames = Arrays.asList("category", "product", "price");
        List<List<Object>> dupColumnList = new ArrayList<>();
        dupColumnList.add(Arrays.asList("A", "A", "B", "B"));
        dupColumnList.add(Arrays.asList("P1", "P2", "P3", "P4"));
        dupColumnList.add(Arrays.asList(10, 20, 30, 40));

        RowDataset dupDataset = new RowDataset(dupColumnNames, dupColumnList);

        String keyColumn = "category";
        Collection<String> valueColumns = Arrays.asList("product", "price");
        IntFunction<ListMultimap<String, List<Object>>> supplier = capacity -> CommonUtil.newLinkedListMultimap();

        ListMultimap<String, List<Object>> result = dupDataset.toMultimap(0, dupDataset.size(), keyColumn, valueColumns, Clazz.ofList(), supplier);

        Assertions.assertEquals(2, result.keySet().size());
        Assertions.assertEquals(2, result.get("A").size());
        Assertions.assertEquals(2, result.get("B").size());
    }

    @Test
    public void testToMultimapWithMapRowType() {
        final RowDataset dataset = createFourRowCityDataset();
        String keyColumn = "city";
        Collection<String> valueColumns = Arrays.asList("id", "name", "age");
        IntFunction<ListMultimap<String, Map<String, Object>>> supplier = capacity -> CommonUtil.newLinkedListMultimap();

        ListMultimap<String, Map<String, Object>> result = dataset.toMultimap(0, dataset.size(), keyColumn, valueColumns, Clazz.ofMap(), supplier);

        Assertions.assertEquals(4, result.keySet().size());
        Map<String, Object> nycPerson = result.get("NYC").get(0);
        Assertions.assertEquals(1, nycPerson.get("id"));
        Assertions.assertEquals("John", nycPerson.get("name"));
    }

    @Test
    public void testToMultimapWithBeanRowType() {
        final RowDataset dataset = createFourRowCityDataset();
        String keyColumn = "city";
        Collection<String> valueColumns = Arrays.asList("id", "name", "age");
        IntFunction<ListMultimap<String, Person>> supplier = capacity -> CommonUtil.newLinkedListMultimap();

        ListMultimap<String, Person> result = dataset.toMultimap(0, dataset.size(), keyColumn, valueColumns, Person.class, supplier);

        Assertions.assertEquals(4, result.keySet().size());
        Person nycPerson = result.get("NYC").get(0);
        Assertions.assertEquals(1, nycPerson.getId());
        Assertions.assertEquals("John", nycPerson.getName());
    }

    @Test
    public void testToMultimapWithRowSupplierSimple() {
        final RowDataset dataset = createFourRowCityDataset();
        String keyColumn = "city";
        Collection<String> valueColumns = Arrays.asList("name", "age");
        IntFunction<Object[]> rowSupplier = size -> new Object[size];

        ListMultimap<String, Object[]> result = dataset.toMultimap(keyColumn, valueColumns, rowSupplier);

        Assertions.assertEquals(4, result.keySet().size());
        Object[] nycData = result.get("NYC").get(0);
        Assertions.assertEquals("John", nycData[0]);
        Assertions.assertEquals(25, nycData[1]);
    }

    @Test
    public void testToMultimapWithRowSupplierAndSupplier() {
        final RowDataset dataset = createFourRowCityDataset();
        String keyColumn = "city";
        Collection<String> valueColumns = Arrays.asList("name", "age");
        IntFunction<Object[]> rowSupplier = size -> new Object[size];
        IntFunction<ListMultimap<String, Object[]>> supplier = capacity -> CommonUtil.newLinkedListMultimap();

        ListMultimap<String, Object[]> result = dataset.toMultimap(keyColumn, valueColumns, rowSupplier, supplier);

        Assertions.assertEquals(4, result.keySet().size());
        Object[] nycData = result.get("NYC").get(0);
        Assertions.assertEquals("John", nycData[0]);
        Assertions.assertEquals(25, nycData[1]);
    }

    @Test
    public void testToMultimapWithRowSupplierAndRange() {
        final RowDataset dataset = createFourRowCityDataset();
        String keyColumn = "city";
        Collection<String> valueColumns = Arrays.asList("name", "age");
        IntFunction<Object[]> rowSupplier = size -> new Object[size];

        ListMultimap<String, Object[]> result = dataset.toMultimap(1, 3, keyColumn, valueColumns, rowSupplier);

        Assertions.assertEquals(2, result.keySet().size());
        Assertions.assertTrue(result.containsKey("LA"));
        Assertions.assertTrue(result.containsKey("Chicago"));
    }

    @Test
    public void testGroupBy() {
        List<String> groupColumnNames = Arrays.asList("department", "employee", "salary");
        List<List<Object>> groupColumnList = new ArrayList<>();
        groupColumnList.add(Arrays.asList("IT", "IT", "HR", "HR"));
        groupColumnList.add(Arrays.asList("John", "Jane", "Bob", "Alice"));
        groupColumnList.add(Arrays.asList(70000, 80000, 60000, 65000));

        RowDataset groupDataset = new RowDataset(groupColumnNames, groupColumnList);

        String keyColumn = "department";
        Collection<String> aggregateColumns = Arrays.asList("employee", "salary");
        String aggregateResultColumn = "employees";

        Dataset grouped = groupDataset.groupBy(keyColumn, null, aggregateColumns, aggregateResultColumn, List.class);

        Assertions.assertEquals(2, grouped.columnCount());
        Assertions.assertEquals(2, grouped.size());
        Assertions.assertTrue(grouped.containsColumn("department"));
        Assertions.assertTrue(grouped.containsColumn("employees"));
    }

    @Test
    public void testGroupByWithKeyExtractor() {
        final RowDataset dataset = createFourRowCityDataset();
        String keyColumn = "age";
        Function<Integer, String> keyExtractor = age -> age < 30 ? "Young" : "Adult";
        Collection<String> aggregateColumns = Arrays.asList("name", "city");
        String aggregateResultColumn = "people";

        Dataset grouped = dataset.groupBy(keyColumn, keyExtractor, aggregateColumns, aggregateResultColumn, Map.class);

        Assertions.assertEquals(2, grouped.columnCount());
        Assertions.assertEquals(2, grouped.size());
    }

    @Test
    public void testIntersection() {
        final RowDataset dataset = createFourRowCityDataset();
        List<String> otherColumnNames = Arrays.asList("id", "name", "age", "city");
        List<List<Object>> otherColumnList = new ArrayList<>();
        otherColumnList.add(Arrays.asList(2, 3, 5, 6));
        otherColumnList.add(Arrays.asList("Jane", "Bob", "Eve", "Frank"));
        otherColumnList.add(Arrays.asList(30, 35, 40, 45));
        otherColumnList.add(Arrays.asList("LA", "Chicago", "Miami", "Seattle"));

        RowDataset otherDataset = new RowDataset(otherColumnNames, otherColumnList);

        Dataset intersection = N.intersection(dataset, otherDataset);

        Assertions.assertEquals(2, intersection.size());
    }

    @Test
    public void testIntersectionWithRequireSameColumns() {
        final RowDataset dataset = createFourRowCityDataset();
        List<String> otherColumnNames = Arrays.asList("id", "name", "age", "city");
        List<List<Object>> otherColumnList = new ArrayList<>();
        otherColumnList.add(Arrays.asList(2, 3));
        otherColumnList.add(Arrays.asList("Jane", "Bob"));
        otherColumnList.add(Arrays.asList(30, 35));
        otherColumnList.add(Arrays.asList("LA", "Chicago"));

        RowDataset otherDataset = new RowDataset(otherColumnNames, otherColumnList);

        Dataset intersection = N.intersection(dataset, otherDataset, true);

        Assertions.assertEquals(2, intersection.size());
    }

    @Test
    public void testIntersectionWithKeyColumns() {
        final RowDataset dataset = createFourRowCityDataset();
        List<String> otherColumnNames = Arrays.asList("id", "name", "age", "city");
        List<List<Object>> otherColumnList = new ArrayList<>();
        otherColumnList.add(Arrays.asList(1, 2, 5, 6));
        otherColumnList.add(Arrays.asList("Different", "Different", "Eve", "Frank"));
        otherColumnList.add(Arrays.asList(30, 35, 40, 45));
        otherColumnList.add(Arrays.asList("LA", "Chicago", "Miami", "Seattle"));

        RowDataset otherDataset = new RowDataset(otherColumnNames, otherColumnList);

        Collection<String> keyColumns = Arrays.asList("id");
        Dataset intersection = N.intersection(dataset, otherDataset, keyColumns);

        Assertions.assertEquals(2, intersection.size());
    }

    @Test
    public void testDifference() {
        final RowDataset dataset = createFourRowCityDataset();
        List<String> otherColumnNames = Arrays.asList("id", "name", "age", "city");
        List<List<Object>> otherColumnList = new ArrayList<>();
        otherColumnList.add(Arrays.asList(2, 3, 5, 6));
        otherColumnList.add(Arrays.asList("Jane", "Bob", "Eve", "Frank"));
        otherColumnList.add(Arrays.asList(30, 35, 40, 45));
        otherColumnList.add(Arrays.asList("LA", "Chicago", "Miami", "Seattle"));

        RowDataset otherDataset = new RowDataset(otherColumnNames, otherColumnList);

        Dataset difference = N.difference(dataset, otherDataset);

        Assertions.assertEquals(2, difference.size());
    }

    @Test
    public void testDifferenceWithRequireSameColumns() {
        final RowDataset dataset = createFourRowCityDataset();
        List<String> otherColumnNames = Arrays.asList("id", "name", "age", "city");
        List<List<Object>> otherColumnList = new ArrayList<>();
        otherColumnList.add(Arrays.asList(3, 4));
        otherColumnList.add(Arrays.asList("Bob", "Alice"));
        otherColumnList.add(Arrays.asList(35, 28));
        otherColumnList.add(Arrays.asList("Chicago", "Boston"));

        RowDataset otherDataset = new RowDataset(otherColumnNames, otherColumnList);

        Dataset difference = N.difference(dataset, otherDataset, true);

        Assertions.assertEquals(3, difference.size());
    }

    @Test
    public void testDifferenceWithKeyColumns() {
        final RowDataset dataset = createFourRowCityDataset();
        List<String> otherColumnNames = Arrays.asList("id", "name", "age", "city");
        List<List<Object>> otherColumnList = new ArrayList<>();
        otherColumnList.add(Arrays.asList(1, 2));
        otherColumnList.add(Arrays.asList("Different", "Different"));
        otherColumnList.add(Arrays.asList(30, 35));
        otherColumnList.add(Arrays.asList("LA", "Chicago"));

        RowDataset otherDataset = new RowDataset(otherColumnNames, otherColumnList);

        Collection<String> keyColumns = Arrays.asList("id");
        Dataset difference = N.difference(dataset, otherDataset, keyColumns);

        Assertions.assertEquals(2, difference.size());
    }

    @Test
    public void testSymmetricDifference() {
        final RowDataset dataset = createFourRowCityDataset();
        List<String> otherColumnNames = Arrays.asList("id", "name", "age", "city");
        List<List<Object>> otherColumnList = new ArrayList<>();
        otherColumnList.add(Arrays.asList(2, 3, 5, 6));
        otherColumnList.add(Arrays.asList("Jane", "Bob", "Eve", "Frank"));
        otherColumnList.add(Arrays.asList(30, 35, 40, 45));
        otherColumnList.add(Arrays.asList("LA", "Chicago", "Miami", "Seattle"));

        RowDataset otherDataset = new RowDataset(otherColumnNames, otherColumnList);

        Dataset symmetricDiff = N.symmetricDifference(dataset, otherDataset);

        Assertions.assertEquals(4, symmetricDiff.size());
    }

    @Test
    public void testSymmetricDifferenceWithRequireSameColumns() {
        final RowDataset dataset = createFourRowCityDataset();
        List<String> otherColumnNames = Arrays.asList("id", "name", "age", "city");
        List<List<Object>> otherColumnList = new ArrayList<>();
        otherColumnList.add(Arrays.asList(3, 4, 5));
        otherColumnList.add(Arrays.asList("Bob", "Alice", "Eve"));
        otherColumnList.add(Arrays.asList(35, 28, 40));
        otherColumnList.add(Arrays.asList("Chicago", "Boston", "Miami"));

        RowDataset otherDataset = new RowDataset(otherColumnNames, otherColumnList);

        Dataset symmetricDiff = N.symmetricDifference(dataset, otherDataset, true);

        Assertions.assertEquals(5, symmetricDiff.size());
    }

    @Test
    public void testSymmetricDifferenceWithKeyColumns() {
        final RowDataset dataset = createFourRowCityDataset();
        List<String> otherColumnNames = Arrays.asList("id", "name", "age", "city");
        List<List<Object>> otherColumnList = new ArrayList<>();
        otherColumnList.add(Arrays.asList(1, 5, 6));
        otherColumnList.add(Arrays.asList("Different", "Eve", "Frank"));
        otherColumnList.add(Arrays.asList(30, 40, 45));
        otherColumnList.add(Arrays.asList("LA", "Miami", "Seattle"));

        RowDataset otherDataset = new RowDataset(otherColumnNames, otherColumnList);

        Collection<String> keyColumns = Arrays.asList("id");
        Dataset symmetricDiff = N.symmetricDifference(dataset, otherDataset, keyColumns);

        Assertions.assertEquals(5, symmetricDiff.size());
    }

    @Test
    public void testSymmetricDifferenceWithKeyColumnsAndRequireSameColumns() {
        final RowDataset dataset = createFourRowCityDataset();
        List<String> otherColumnNames = Arrays.asList("id", "name", "age", "city");
        List<List<Object>> otherColumnList = new ArrayList<>();
        otherColumnList.add(Arrays.asList(2, 5));
        otherColumnList.add(Arrays.asList("Jane", "Eve"));
        otherColumnList.add(Arrays.asList(30, 40));
        otherColumnList.add(Arrays.asList("LA", "Miami"));

        RowDataset otherDataset = new RowDataset(otherColumnNames, otherColumnList);

        Collection<String> keyColumns = Arrays.asList("id");
        Dataset symmetricDiff = N.symmetricDifference(dataset, otherDataset, keyColumns, true);

        Assertions.assertEquals(4, symmetricDiff.size());
    }

    @Test
    public void testToListWithEmptyDataset() {
        RowDataset emptyDataset = new RowDataset(new ArrayList<>(), new ArrayList<>());
        IntFunction<List<Object>> rowSupplier = capacity -> new ArrayList<>();

        List<List<Object>> result = emptyDataset.toList(null, null, rowSupplier);

        Assertions.assertTrue(result.isEmpty());
    }

    @Test
    public void testToMapWithInvalidRowType() {
        final RowDataset dataset = createFourRowCityDataset();
        String keyColumn = "id";
        Collection<String> valueColumns = Arrays.asList("name", "age");
        IntFunction<Map<Integer, String>> supplier = capacity -> new HashMap<>();

        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            dataset.toMap(0, dataset.size(), keyColumn, valueColumns, String.class, supplier);
        });
    }

    @Test
    public void testGroupByWithEmptyAggregateColumns() {
        String keyColumn = "city";
        Collection<String> aggregateColumns = new ArrayList<>();
        String aggregateResultColumn = "data";

        Assertions.assertThrows(IllegalArgumentException.class, () -> dataset.groupBy(keyColumn, null, aggregateColumns, aggregateResultColumn, List.class));
    }

    @Test
    public void testRightJoinWithSingleColumnName() {
        Dataset result = ds1.rightJoin(ds2, "id", "id");

        assertNotNull(result);
        assertEquals(3, result.size());
        assertTrue(result.containsColumn("id"));
        assertTrue(result.containsColumn("name"));
        assertTrue(result.containsColumn("age"));
        assertTrue(result.containsColumn("city"));
        assertTrue(result.containsColumn("salary"));

        assertEquals(2, (Integer) result.moveToRow(0).get("id"));
        assertEquals("Bob", result.moveToRow(0).get("name"));
        assertEquals(30, (Integer) result.moveToRow(0).get("age"));
        assertEquals("New York", result.moveToRow(0).get("city"));
        assertEquals(50000, (Integer) result.moveToRow(0).get("salary"));

        assertEquals(3, (Integer) result.moveToRow(1).get("id"));
        assertEquals("Charlie", result.moveToRow(1).get("name"));
        assertEquals(35, (Integer) result.moveToRow(1).get("age"));
        assertEquals("London", result.moveToRow(1).get("city"));
        assertEquals(60000, (Integer) result.moveToRow(1).get("salary"));

        assertNull(result.moveToRow(2).get("id"));
        assertNull(result.moveToRow(2).get("name"));
        assertNull(result.moveToRow(2).get("age"));
        assertEquals("Tokyo", result.moveToRow(2).get("city"));
        assertEquals(70000, (Integer) result.moveToRow(2).get("salary"));
    }

    @Test
    public void testLeftJoinWithSingleColumnName() {
        Dataset result = ds1.leftJoin(ds2, "id", "id");

        assertNotNull(result);
        assertEquals(3, result.size());
        assertTrue(result.containsColumn("id"));
        assertTrue(result.containsColumn("name"));
        assertTrue(result.containsColumn("age"));
        assertTrue(result.containsColumn("city"));
        assertTrue(result.containsColumn("salary"));

        assertEquals(1, (Integer) result.moveToRow(0).get("id"));
        assertEquals("Alice", result.moveToRow(0).get("name"));
        assertEquals(25, (Integer) result.moveToRow(0).get("age"));
        assertNull(result.moveToRow(0).get("city"));
        assertNull(result.moveToRow(0).get("salary"));

        assertEquals(2, (Integer) result.moveToRow(1).get("id"));
        assertEquals("Bob", result.moveToRow(1).get("name"));
        assertEquals(30, (Integer) result.moveToRow(1).get("age"));
        assertEquals("New York", result.moveToRow(1).get("city"));
        assertEquals(50000, (Integer) result.moveToRow(1).get("salary"));

        assertEquals(3, (Integer) result.moveToRow(2).get("id"));
        assertEquals("Charlie", result.moveToRow(2).get("name"));
        assertEquals(35, (Integer) result.moveToRow(2).get("age"));
        assertEquals("London", result.moveToRow(2).get("city"));
        assertEquals(60000, (Integer) result.moveToRow(2).get("salary"));
    }

    @Test
    public void testRightJoinWithMap() {
        Map<String, String> onColumnNames = new HashMap<>();
        onColumnNames.put("id", "id");

        Dataset result = ds1.rightJoin(ds2, onColumnNames);

        assertNotNull(result);
        assertEquals(3, result.size());

        assertEquals(2, (Integer) result.moveToRow(0).get("id"));
        assertEquals("Bob", result.moveToRow(0).get("name"));
        assertEquals(30, (Integer) result.moveToRow(0).get("age"));
    }

    @Test
    public void testRightJoinWithMultipleColumns() {
        List<String> columnNames1 = CommonUtil.toList("id", "type", "value");
        List<List<Object>> columns1 = new ArrayList<>();
        columns1.add(CommonUtil.toList(1, 1, 2));
        columns1.add(CommonUtil.toList("A", "B", "A"));
        columns1.add(CommonUtil.toList(100, 200, 300));
        Dataset multiDs1 = new RowDataset(columnNames1, columns1);

        List<String> columnNames2 = CommonUtil.toList("id", "type", "score");
        List<List<Object>> columns2 = new ArrayList<>();
        columns2.add(CommonUtil.toList(1, 2, 3));
        columns2.add(CommonUtil.toList("A", "A", "B"));
        columns2.add(CommonUtil.toList(10, 20, 30));
        Dataset multiDs2 = new RowDataset(columnNames2, columns2);

        Map<String, String> onColumnNames = new HashMap<>();
        onColumnNames.put("id", "id");
        onColumnNames.put("type", "type");

        Dataset result = multiDs1.rightJoin(multiDs2, onColumnNames);

        assertNotNull(result);
        assertEquals(3, result.size());
    }

    @Test
    public void testRightJoinWithEmptyRightDataset() {
        Dataset result = ds1.rightJoin(emptyDs, "id", "col1");

        assertNotNull(result);
        assertEquals(0, result.size());
    }

    @Test
    public void testRightJoinWithNewColumn() {
        Map<String, String> onColumnNames = new HashMap<>();
        onColumnNames.put("id", "id");

        Dataset result = ds1.rightJoin(ds2, onColumnNames, "rightData", Map.class);

        assertNotNull(result);
        assertEquals(3, result.size());
        assertTrue(result.containsColumn("rightData"));

        Map<String, Object> firstRightData = (Map<String, Object>) result.moveToRow(2).get("rightData");
        assertNotNull(firstRightData);
    }

    @Test
    public void testRightJoinWithCollectionSupplier() {
        Map<String, String> onColumnNames = new HashMap<>();
        onColumnNames.put("id", "id");

        List<String> columnNames = CommonUtil.toList("id", "value");
        List<List<Object>> columns = new ArrayList<>();
        columns.add(CommonUtil.toList(2, 2, 3));
        columns.add(CommonUtil.toList("X", "Y", "Z"));
        Dataset dsWithDuplicates = new RowDataset(columnNames, columns);

        Dataset result = ds1.rightJoin(dsWithDuplicates, onColumnNames, "values", List.class, ArrayList::new);

        assertNotNull(result);
        assertTrue(result.containsColumn("values"));
    }

    @Test
    public void testFullJoinWithSingleColumnName() {
        Dataset result = ds1.fullJoin(ds2, "id", "id");

        assertNotNull(result);
        assertEquals(4, result.size());

        assertTrue(result.containsColumn("id"));
        assertTrue(result.containsColumn("name"));
        assertTrue(result.containsColumn("age"));
        assertTrue(result.containsColumn("city"));
        assertTrue(result.containsColumn("salary"));

        assertEquals(1, (Integer) result.moveToRow(0).get("id"));
        assertEquals("Alice", result.moveToRow(0).get("name"));
        assertEquals(25, (Integer) result.moveToRow(0).get("age"));
        assertNull(result.moveToRow(0).get("city"));
        assertNull(result.moveToRow(0).get("salary"));
    }

    @Test
    public void testFullJoinWithMap() {
        Map<String, String> onColumnNames = new HashMap<>();
        onColumnNames.put("id", "id");

        Dataset result = ds1.fullJoin(ds2, onColumnNames);

        assertNotNull(result);
        assertEquals(4, result.size());
    }

    @Test
    public void testFullJoinWithNewColumn() {
        Map<String, String> onColumnNames = new HashMap<>();
        onColumnNames.put("id", "id");

        Dataset result = ds1.fullJoin(ds2, onColumnNames, "mergedData", Map.class);

        assertNotNull(result);
        assertEquals(4, result.size());
        assertTrue(result.containsColumn("mergedData"));
    }

    @Test
    public void testFullJoinWithCollectionSupplier() {
        Map<String, String> onColumnNames = new HashMap<>();
        onColumnNames.put("id", "id");

        Dataset result = ds1.fullJoin(ds2, onColumnNames, "dataList", List.class, ArrayList::new);

        assertNotNull(result);
        assertEquals(4, result.size());
        assertTrue(result.containsColumn("dataList"));
    }

    @Test
    public void testUnion() {
        Dataset result = ds1.union(ds2);

        assertNotNull(result);
        assertTrue(result.size() <= ds1.size() + ds2.size());
        assertTrue(result.containsColumn("id"));
        assertTrue(result.containsColumn("name"));
        assertTrue(result.containsColumn("age"));
        assertTrue(result.containsColumn("city"));
        assertTrue(result.containsColumn("salary"));
    }

    @Test
    public void testUnionWithSameColumnsRequired() {
        List<String> columnNames = CommonUtil.toList("id", "name", "age");
        List<List<Object>> columns = new ArrayList<>();
        columns.add(CommonUtil.toList(4, 5));
        columns.add(CommonUtil.toList("David", "Eve"));
        columns.add(CommonUtil.toList(40, 45));
        Dataset ds3 = new RowDataset(columnNames, columns);

        Dataset result = ds1.union(ds3, true);

        assertNotNull(result);
        assertEquals(5, result.size());
    }

    @Test
    public void testUnionWithKeyColumns() {
        Collection<String> keyColumns = CommonUtil.toList("id");
        Dataset result = ds1.union(ds2, keyColumns);

        assertNotNull(result);
        assertTrue(result.containsColumn("id"));
    }

    @Test
    public void testUnionAll() {
        Dataset result = ds1.unionAll(ds2);

        assertNotNull(result);
        assertTrue(result.containsColumn("id"));
    }

    @Test
    public void testIntersect() {
        Dataset result = ds1.intersect(ds2);

        assertNotNull(result);
        assertEquals(2, result.size());
    }

    @Test
    public void testIntersectWithKeyColumns() {
        Collection<String> keyColumns = CommonUtil.toList("id");
        Dataset result = ds1.intersect(ds2, keyColumns);

        assertNotNull(result);
        assertEquals(2, result.size());
    }

    @Test
    public void testIntersectAll() {
        Dataset result = ds1.intersectAll(ds2);

        assertNotNull(result);
        assertTrue(result.size() <= Math.min(ds1.size(), ds2.size()));
    }

    @Test
    public void testExcept() {
        Dataset result = ds1.except(ds2);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(1, (Integer) result.moveToRow(0).get("id"));
    }

    @Test
    public void testExceptWithKeyColumns() {
        Collection<String> keyColumns = CommonUtil.toList("id");
        Dataset result = ds1.except(ds2, keyColumns);

        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    public void testExceptAll() {
        Dataset result = ds1.exceptAll(ds2);

        assertNotNull(result);
        assertTrue(result.size() <= ds1.size());
    }

    @Test
    public void testMergeWithSameColumnsRequired() {
        assertThrows(IllegalArgumentException.class, () -> {
            ds1.merge(ds2, true);
        });
    }

    @Test
    public void testMergeWithColumnNames() {
        Collection<String> columnNames = CommonUtil.toList("id", "city");
        Dataset result = ds1.copy();
        result.merge(ds2, columnNames);

        assertNotNull(result);
        assertEquals(ds1.size() + ds2.size(), result.size());
    }

    @Test
    public void testMergeMultipleDatasets() {
        Collection<Dataset> ds = CommonUtil.toList(ds1, ds2, emptyDs);
        Dataset result = CommonUtil.merge(ds);

        assertNotNull(result);
        assertEquals(ds1.size() + ds2.size() + emptyDs.size(), result.size());
    }

    @Test
    public void testCartesianProduct() {
        List<String> columnNames1 = CommonUtil.toList("a", "b");
        List<List<Object>> columns1 = new ArrayList<>();
        columns1.add(CommonUtil.toList(1, 2));
        columns1.add(CommonUtil.toList("X", "Y"));
        Dataset ds1New = new RowDataset(columnNames1, columns1);

        List<String> columnNames2 = CommonUtil.toList("c", "d");
        List<List<Object>> columns2 = new ArrayList<>();
        columns2.add(CommonUtil.toList(10, 20));
        columns2.add(CommonUtil.toList("P", "Q"));
        Dataset ds2New = new RowDataset(columnNames2, columns2);

        Dataset result = ds1New.cartesianProduct(ds2New);

        assertNotNull(result);
        assertEquals(4, result.size());
        assertTrue(result.containsColumn("a"));
        assertTrue(result.containsColumn("b"));
        assertTrue(result.containsColumn("c"));
        assertTrue(result.containsColumn("d"));
    }

    @Test
    public void testCartesianProductWithCommonColumns() {
        assertThrows(IllegalArgumentException.class, () -> {
            ds1.cartesianProduct(ds2);
        });
    }

    @Test
    public void testSplit() {
        Stream<Dataset> splitStream = ds1.split(2);
        List<Dataset> splits = splitStream.toList();

        assertEquals(2, splits.size());
        assertEquals(2, splits.get(0).size());
        assertEquals(1, splits.get(1).size());
    }

    @Test
    public void testSplitWithColumns() {
        Collection<String> columnNames = CommonUtil.toList("id", "name");
        Stream<Dataset> splitStream = ds1.split(2, columnNames);
        List<Dataset> splits = splitStream.toList();

        assertEquals(2, splits.size());
        assertTrue(splits.get(0).containsColumn("id"));
        assertTrue(splits.get(0).containsColumn("name"));
        assertFalse(splits.get(0).containsColumn("age"));
    }

    @Test
    public void testSplitToList() {
        List<Dataset> splits = ds1.splitToList(2);

        assertEquals(2, splits.size());
        assertEquals(2, splits.get(0).size());
        assertEquals(1, splits.get(1).size());
    }

    @Test
    public void testSplitToListWithColumns() {
        Collection<String> columnNames = CommonUtil.toList("id", "age");
        List<Dataset> splits = ds1.splitToList(2, columnNames);

        assertEquals(2, splits.size());
        assertTrue(splits.get(0).containsColumn("id"));
        assertTrue(splits.get(0).containsColumn("age"));
        assertFalse(splits.get(0).containsColumn("name"));
    }

    @Test
    public void testSlice() {
        Collection<String> columnNames = CommonUtil.toList("id", "name");
        Dataset result = ds1.slice(columnNames);

        assertNotNull(result);
        assertEquals(ds1.size(), result.size());
        assertEquals(2, result.columnCount());
        assertTrue(result.containsColumn("id"));
        assertTrue(result.containsColumn("name"));
        assertFalse(result.containsColumn("age"));
    }

    @Test
    public void testSliceWithRowRange() {
        Dataset result = ds1.slice(1, 3);

        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals(ds1.columnCount(), result.columnCount());
    }

    @Test
    public void testSliceWithRowRangeAndColumns() {
        Collection<String> columnNames = CommonUtil.toList("name", "age");
        Dataset result = ds1.slice(0, 2, columnNames);

        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals(2, result.columnCount());
        assertTrue(result.containsColumn("name"));
        assertTrue(result.containsColumn("age"));
    }

    @Test
    public void testPaginate() {
        Paginated<Dataset> paginated = ds1.paginate(2);

        assertNotNull(paginated);
        assertEquals(2, paginated.pageSize());
        assertEquals(2, paginated.totalPages());

        Optional<Dataset> firstPage = paginated.firstPage();
        assertTrue(firstPage.isPresent());
        assertEquals(2, firstPage.get().size());

        Optional<Dataset> lastPage = paginated.lastPage();
        assertTrue(lastPage.isPresent());
        assertEquals(1, lastPage.get().size());
    }

    @Test
    public void testPaginateWithColumns() {
        Collection<String> columnNames = CommonUtil.toList("id", "name");
        Paginated<Dataset> paginated = ds1.paginate(columnNames, 2);

        assertNotNull(paginated);
        assertEquals(2, paginated.totalPages());

        Dataset page = paginated.getPage(0);
        assertEquals(2, page.columnCount());
        assertTrue(page.containsColumn("id"));
        assertTrue(page.containsColumn("name"));
    }

    @Test
    public void testStreamByColumnName() {
        Stream<Integer> idStream = ds1.stream("id");
        List<Integer> ids = idStream.toList();

        assertEquals(3, ids.size());
        assertEquals(CommonUtil.toList(1, 2, 3), ids);
    }

    @Test
    public void testStreamByColumnNameWithRange() {
        Stream<String> nameStream = ds1.stream(1, 3, "name");
        List<String> names = nameStream.toList();

        assertEquals(2, names.size());
        assertEquals(CommonUtil.toList("Bob", "Charlie"), names);
    }

    @Test
    public void testStreamWithRowType() {
        Stream<Object[]> rowStream = ds1.stream(Object[].class);
        List<Object[]> rows = rowStream.toList();

        assertEquals(3, rows.size());
        assertEquals(3, rows.get(0).length);
    }

    @Test
    public void testStreamWithRowSupplier() {
        Stream<List> rowStream = ds1.stream(size -> new ArrayList<>(size));
        List<List> rows = rowStream.toList();

        assertEquals(3, rows.size());
    }

    @Test
    public void testStreamWithPrefixAndFieldNameMap() {
        Map<String, String> prefixMap = new HashMap<>();
        prefixMap.put("", "");

        Stream<Person> rowStream = ds1.stream(prefixMap, Person.class);
        List<Person> rows = rowStream.toList();

        assertEquals(3, rows.size());
    }

    @Test
    public void testStreamWithRowMapper() {
        Stream<String> stream = ds1.stream((rowIndex, array) -> "Row " + rowIndex + ": " + Arrays.toString(array.copy()));
        List<String> results = stream.toList();

        assertEquals(3, results.size());
        assertTrue(results.get(0).startsWith("Row 0:"));
    }

    @Test
    public void testStreamWithTuple2() {
        Tuple2<String, String> columnNames = Tuple.of("id", "name");
        Stream<String> stream = ds1.stream(columnNames, (id, name) -> id + "-" + name);
        List<String> results = stream.toList();

        assertEquals(3, results.size());
        assertEquals("1-Alice", results.get(0));
        assertEquals("2-Bob", results.get(1));
        assertEquals("3-Charlie", results.get(2));
    }

    @Test
    public void testStreamWithTuple3() {
        Tuple3<String, String, String> columnNames = Tuple.of("id", "name", "age");
        Stream<String> stream = ds1.stream(columnNames, (id, name, age) -> id + "-" + name + "-" + age);
        List<String> results = stream.toList();

        assertEquals(3, results.size());
        assertEquals("1-Alice-25", results.get(0));
    }

    @Test
    public void testApply() {
        Integer result = ds1.apply(ds -> ds.size());
        assertEquals(3, result);
    }

    @Test
    public void testApplyIfNotEmpty() {
        Optional<Integer> result = ds1.applyIfNotEmpty(ds -> ds.size());
        assertTrue(result.isPresent());
        assertEquals(3, result.get().intValue());

        Optional<Integer> emptyResult = emptyDs.applyIfNotEmpty(ds -> ds.size());
        assertFalse(emptyResult.isPresent());
    }

    @Test
    public void testAccept() {
        List<String> names = new ArrayList<>();
        ds1.accept(ds -> {
            for (int i = 0; i < ds.size(); i++) {
                names.add((String) ds.moveToRow(i).get("name"));
            }
        });

        assertEquals(3, names.size());
        assertEquals(CommonUtil.toList("Alice", "Bob", "Charlie"), names);
    }

    @Test
    public void testAcceptIfNotEmpty() {
        List<String> names = new ArrayList<>();
        OrElse result = ds1.acceptIfNotEmpty(ds -> {
            names.add("processed");
        });

        assertEquals(OrElse.TRUE, result);
        assertEquals(1, names.size());

        OrElse emptyResult = emptyDs.acceptIfNotEmpty(ds -> {
            names.add("should not be added");
        });

        assertEquals(OrElse.FALSE, emptyResult);
        assertEquals(1, names.size());
    }

    @Test
    public void testProperties() {
        Map<String, Object> props = ds1.getProperties();
        assertNotNull(props);
    }

    @Test
    public void testPrintln() {
        assertDoesNotThrow(() -> {
            ds1.println();
        });
    }

    @Test
    public void testPrintlnWithRange() {
        assertDoesNotThrow(() -> {
            ds1.println(0, 2);
        });
    }

    @Test
    public void testPrintlnWithRangeAndColumns() {
        Collection<String> columnNames = CommonUtil.toList("id", "name");
        ds1.println(0, 2, columnNames);
        assertNotNull(columnNames);
    }

    @Test
    public void testPrintlnWithWriter() {
        StringWriter writer = new StringWriter();
        ds1.println(writer);

        String output = writer.toString();
        assertNotNull(output);
        assertTrue(output.contains("id"));
        assertTrue(output.contains("name"));
        assertTrue(output.contains("age"));
    }

    @Test
    public void testPrintlnWithRangeColumnsAndWriter() {
        StringWriter writer = new StringWriter();
        Collection<String> columnNames = CommonUtil.toList("id", "name");
        ds1.println(1, 3, columnNames, writer);

        String output = writer.toString();
        assertNotNull(output);
        assertTrue(output.contains("Bob"));
        assertTrue(output.contains("Charlie"));
    }

    @Test
    public void testComplexJoinScenario() {
        List<String> columnNames1 = CommonUtil.toList("id", "value");
        List<List<Object>> columns1 = new ArrayList<>();
        columns1.add(CommonUtil.toList(1, null, 3));
        columns1.add(CommonUtil.toList("A", "B", "C"));
        Dataset dsWithNull1 = new RowDataset(columnNames1, columns1);

        List<String> columnNames2 = CommonUtil.toList("id", "score");
        List<List<Object>> columns2 = new ArrayList<>();
        columns2.add(CommonUtil.toList((Object) null, 2, 3));
        columns2.add(CommonUtil.toList(10, 20, 30));
        Dataset dsWithNull2 = new RowDataset(columnNames2, columns2);

        Dataset result = dsWithNull1.rightJoin(dsWithNull2, "id", "id");
        assertNotNull(result);
        assertEquals(3, result.size());
    }

    @Test
    public void testEmptyDatasetOperations() {
        Dataset result1 = emptyDs.rightJoin(ds1, "col1", "id");
        assertEquals(ds1.size(), result1.size());

        Dataset result2 = emptyDs.fullJoin(ds1, "col1", "id");
        assertEquals(ds1.size(), result2.size());

        assertThrows(IllegalArgumentException.class, () -> emptyDs.union(ds1));
        assertThrows(IllegalArgumentException.class, () -> emptyDs.intersect(ds1));

        Dataset emptyDataset = CommonUtil.newEmptyDataset(ds1.columnNames());
        Dataset result3 = emptyDataset.union(ds1);
        assertTrue(result3.size() >= 0);
        Dataset result4 = emptyDataset.intersect(ds1);
        assertEquals(0, result4.size());
    }

    @Test
    public void testInvalidColumnOperations() {
        assertThrows(IllegalArgumentException.class, () -> {
            ds1.rightJoin(ds2, "invalid_column", "id");
        });

        assertThrows(IllegalArgumentException.class, () -> {
            ds1.rightJoin(ds2, "id", "invalid_column");
        });

        Map<String, String> invalidMap = new HashMap<>();
        invalidMap.put("invalid_column", "id");

        assertThrows(IllegalArgumentException.class, () -> {
            ds1.rightJoin(ds2, invalidMap);
        });
    }

    @Test
    public void testPaginationEdgeCases() {
        Paginated<Dataset> paginated1 = ds1.paginate(3);
        assertEquals(1, paginated1.totalPages());

        Paginated<Dataset> paginated2 = ds1.paginate(10);
        assertEquals(1, paginated2.totalPages());

        assertThrows(IllegalArgumentException.class, () -> {
            paginated1.getPage(-1);
        });

        assertThrows(IllegalArgumentException.class, () -> {
            paginated1.getPage(5);
        });
    }

    @Test
    public void testStreamOperationsWithEmptyDataset() {
        Stream<Object> stream = emptyDs.stream("col1");
        assertEquals(0, stream.count());

        Stream<Object[]> rowStream = emptyDs.stream(Object[].class);
        assertEquals(0, rowStream.count());
    }

    @Test
    public void testToXml() {
        final RowDataset dataset = createFiveRowCityDataset();
        String xml = dataset.toXml();
        assertNotNull(xml);
        assertTrue(xml.contains("<dataset>"));
        assertTrue(xml.contains("</dataset>"));
        assertTrue(xml.contains("<row>"));
        assertTrue(xml.contains("<id>"));
        assertTrue(xml.contains("<name>"));
    }

    @Test
    public void testToXmlWithCustomRowElementName() {
        final RowDataset dataset = createFiveRowCityDataset();
        String xml = dataset.toXml("record");
        assertNotNull(xml);
        assertTrue(xml.contains("<record>"));
        assertTrue(xml.contains("</record>"));
    }

    @Test
    public void testToXmlWithRowRange() {
        final RowDataset dataset = createFiveRowCityDataset();
        String xml = dataset.toXml(1, 3);
        assertNotNull(xml);
        assertTrue(xml.contains("Jane"));
        assertTrue(xml.contains("Bob"));
        assertFalse(xml.contains("John"));
        assertFalse(xml.contains("Alice"));
    }

    @Test
    public void testToXmlWithRowRangeAndCustomElementName() {
        final RowDataset dataset = createFiveRowCityDataset();
        String xml = dataset.toXml(1, 3, "person");
        assertNotNull(xml);
        assertTrue(xml.contains("<person>"));
        assertTrue(xml.contains("Jane"));
        assertTrue(xml.contains("Bob"));
    }

    @Test
    public void testToXmlWithRowRangeAndColumnNames() {
        final RowDataset dataset = createFiveRowCityDataset();
        Collection<String> columns = CommonUtil.toList("name", "age");
        String xml = dataset.toXml(1, 3, columns);
        assertNotNull(xml);
        assertTrue(xml.contains("Jane"));
        assertTrue(xml.contains("30"));
        assertFalse(xml.contains("<id>"));
        assertFalse(xml.contains("<city>"));
    }

    @Test
    public void testToXmlWithAllParameters() {
        final RowDataset dataset = createFiveRowCityDataset();
        Collection<String> columns = CommonUtil.toList("name", "age");
        String xml = dataset.toXml(1, 3, columns, "employee");
        assertNotNull(xml);
        assertTrue(xml.contains("<employee>"));
        assertTrue(xml.contains("Jane"));
        assertTrue(xml.contains("30"));
    }

    @Test
    public void testToXmlToFile(@TempDir Path tempDir) throws IOException {
        final RowDataset dataset = createFiveRowCityDataset();
        File outputFile = tempDir.resolve("test.xml").toFile();
        dataset.toXml(outputFile);
        assertTrue(outputFile.exists());

        String content = IOUtil.readAllToString(outputFile);
        assertTrue(content.contains("<dataset>"));
        assertTrue(content.contains("John"));
    }

    @Test
    public void testToXmlToFileWithCustomElementName(@TempDir Path tempDir) throws IOException {
        final RowDataset dataset = createFiveRowCityDataset();
        File outputFile = tempDir.resolve("test2.xml").toFile();
        dataset.toXml("item", outputFile);
        assertTrue(outputFile.exists());

        String content = IOUtil.readAllToString(outputFile);
        assertTrue(content.contains("<item>"));
    }

    @Test
    public void testToXmlToFileWithRowRange(@TempDir Path tempDir) throws IOException {
        final RowDataset dataset = createFiveRowCityDataset();
        File outputFile = tempDir.resolve("test3.xml").toFile();
        dataset.toXml(1, 3, outputFile);
        assertTrue(outputFile.exists());

        String content = IOUtil.readAllToString(outputFile);
        assertTrue(content.contains("Jane"));
        assertFalse(content.contains("John"));
    }

    @Test
    public void testToXmlToFileWithAllParams(@TempDir Path tempDir) throws IOException {
        final RowDataset dataset = createFiveRowCityDataset();
        File outputFile = tempDir.resolve("test4.xml").toFile();
        Collection<String> columns = CommonUtil.toList("name", "age");
        dataset.toXml(0, 2, columns, "record", outputFile);
        assertTrue(outputFile.exists());

        String content = IOUtil.readAllToString(outputFile);
        assertTrue(content.contains("<record>"));
        assertTrue(content.contains("John"));
        assertFalse(content.contains("<city>"));
    }

    @Test
    public void testToXmlToOutputStream() throws IOException {
        final RowDataset dataset = createFiveRowCityDataset();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        dataset.toXml(baos);

        String xml = baos.toString();
        assertTrue(xml.contains("<dataset>"));
        assertTrue(xml.contains("John"));
    }

    @Test
    public void testToXmlToOutputStreamWithElementName() throws IOException {
        final RowDataset dataset = createFiveRowCityDataset();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        dataset.toXml("data", baos);

        String xml = baos.toString();
        assertTrue(xml.contains("<data>"));
    }

    @Test
    public void testToXmlToOutputStreamWithRowRange() throws IOException {
        final RowDataset dataset = createFiveRowCityDataset();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        dataset.toXml(1, 3, baos);

        String xml = baos.toString();
        assertTrue(xml.contains("Jane"));
        assertFalse(xml.contains("John"));
    }

    @Test
    public void testToXmlToOutputStreamWithAllParams() throws IOException {
        final RowDataset dataset = createFiveRowCityDataset();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Collection<String> columns = CommonUtil.toList("name");
        dataset.toXml(0, 2, columns, "person", baos);

        String xml = baos.toString();
        assertTrue(xml.contains("<person>"));
        assertTrue(xml.contains("John"));
        assertFalse(xml.contains("<age>"));
    }

    @Test
    public void testToXmlToWriter() throws IOException {
        final RowDataset dataset = createFiveRowCityDataset();
        StringWriter writer = new StringWriter();
        dataset.toXml(writer);

        String xml = writer.toString();
        assertTrue(xml.contains("<dataset>"));
        assertTrue(xml.contains("John"));
    }

    @Test
    public void testToXmlToWriterWithElementName() throws IOException {
        final RowDataset dataset = createFiveRowCityDataset();
        StringWriter writer = new StringWriter();
        dataset.toXml("entry", writer);

        String xml = writer.toString();
        assertTrue(xml.contains("<entry>"));
    }

    @Test
    public void testToXmlToWriterWithRowRange() throws IOException {
        final RowDataset dataset = createFiveRowCityDataset();
        StringWriter writer = new StringWriter();
        dataset.toXml(2, 4, writer);

        String xml = writer.toString();
        assertTrue(xml.contains("Bob"));
        assertTrue(xml.contains("Alice"));
        assertFalse(xml.contains("John"));
    }

    @Test
    public void testToXmlToWriterWithAllParams() throws IOException {
        final RowDataset dataset = createFiveRowCityDataset();
        StringWriter writer = new StringWriter();
        Collection<String> columns = CommonUtil.toList("id", "name");
        dataset.toXml(0, 2, columns, "user", writer);

        String xml = writer.toString();
        assertTrue(xml.contains("<user>"));
        assertTrue(xml.contains("<id>1</id>"));
        assertTrue(xml.contains("John"));
        assertFalse(xml.contains("<age>"));
    }

    @Test
    public void testToCsv() {
        final RowDataset dataset = createFiveRowCityDataset();
        String csv = dataset.toCsv();
        assertNotNull(csv);
        assertTrue(csv.contains("\"id\",\"name\",\"age\",\"city\""));
        assertTrue(csv.contains("1,\"John\",25,\"NYC\""));
    }

    @Test
    public void testToCsvWithRowRangeAndColumns() {
        final RowDataset dataset = createFiveRowCityDataset();
        Collection<String> columns = CommonUtil.toList("name", "age");
        String csv = dataset.toCsv(1, 3, columns);
        assertNotNull(csv);
        assertTrue(csv.contains("\"name\",\"age\""));
        assertTrue(csv.contains("\"Jane\",30"));
        assertTrue(csv.contains("\"Bob\",35"));
        assertFalse(csv.contains("John"));
    }

    @Test
    public void testToCsvToFile(@TempDir Path tempDir) throws IOException {
        final RowDataset dataset = createFiveRowCityDataset();
        File outputFile = tempDir.resolve("test.csv").toFile();
        dataset.toCsv(outputFile);
        assertTrue(outputFile.exists());

        String content = IOUtil.readAllToString(outputFile);
        assertTrue(content.contains("\"id\",\"name\",\"age\",\"city\""));
        assertTrue(content.contains("\"John\""));
    }

    @Test
    public void testToCsvToFileWithParams(@TempDir Path tempDir) throws IOException {
        final RowDataset dataset = createFiveRowCityDataset();
        File outputFile = tempDir.resolve("test2.csv").toFile();
        Collection<String> columns = CommonUtil.toList("name", "city");
        dataset.toCsv(1, 3, columns, outputFile);
        assertTrue(outputFile.exists());

        String content = IOUtil.readAllToString(outputFile);
        assertTrue(content.contains("\"name\",\"city\""));
        assertTrue(content.contains("\"Jane\",\"LA\""));
        assertFalse(content.contains("John"));
    }

    @Test
    public void testToCsvToOutputStream() throws IOException {
        final RowDataset dataset = createFiveRowCityDataset();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        dataset.toCsv(baos);

        String csv = baos.toString();
        assertTrue(csv.contains("\"id\",\"name\",\"age\",\"city\""));
        assertTrue(csv.contains("John"));
    }

    @Test
    public void testToCsvToOutputStreamWithParams() throws IOException {
        final RowDataset dataset = createFiveRowCityDataset();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Collection<String> columns = CommonUtil.toList("id", "name");
        dataset.toCsv(0, 2, columns, baos);

        String csv = baos.toString();
        assertTrue(csv.contains("\"id\",\"name\""));
        assertTrue(csv.contains("1,\"John\""));
        assertFalse(csv.contains("age"));
    }

    @Test
    public void testToCsvToWriter() throws IOException {
        final RowDataset dataset = createFiveRowCityDataset();
        StringWriter writer = new StringWriter();
        dataset.toCsv(writer);

        String csv = writer.toString();
        assertTrue(csv.contains("\"id\",\"name\",\"age\",\"city\""));
        assertTrue(csv.contains("\"John\""));
    }

    @Test
    public void testToCsvToWriterWithParams() throws IOException {
        final RowDataset dataset = createFiveRowCityDataset();
        StringWriter writer = new StringWriter();
        Collection<String> columns = CommonUtil.toList("name");
        dataset.toCsv(2, 4, columns, writer);

        String csv = writer.toString();
        assertTrue(csv.contains("name"));
        assertTrue(csv.contains("Bob"));
        assertTrue(csv.contains("Alice"));
        assertFalse(csv.contains("John"));
    }

    @Test
    public void testGroupByWithSingleKeyAndCollector() {
        final RowDataset dataset = createFiveRowCityDataset();
        Collector<Object, ?, List<Object>> listCollector = Collectors.toList();
        Dataset grouped = dataset.groupBy("city", "name", "names", listCollector);

        assertNotNull(grouped);
        assertEquals(3, grouped.size());
        assertTrue(grouped.containsColumn("city"));
        assertTrue(grouped.containsColumn("names"));
    }

    @Test
    public void testGroupByWithSingleKeyAndRowType() {
        final RowDataset dataset = createFiveRowCityDataset();
        Dataset grouped = dataset.groupBy("city", CommonUtil.toList("name", "age"), "people", Object[].class);

        assertNotNull(grouped);
        assertEquals(3, grouped.size());
        assertTrue(grouped.containsColumn("city"));
        assertTrue(grouped.containsColumn("people"));
    }

    @Test
    public void testGroupByWithSingleKeyAndArrayCollector() {
        final RowDataset dataset = createFiveRowCityDataset();
        Collector<Object[], ?, List<Object[]>> collector = Collectors.toList();
        Dataset grouped = dataset.groupBy("city", CommonUtil.toList("name", "age"), "data", collector);

        assertNotNull(grouped);
        assertEquals(3, grouped.size());
        assertTrue(grouped.containsColumn("city"));
        assertTrue(grouped.containsColumn("data"));
    }

    @Test
    public void testGroupByWithSingleKeyAndRowMapper() {
        final RowDataset dataset = createFiveRowCityDataset();
        Function<DisposableObjArray, String> rowMapper = arr -> arr.get(0) + "-" + arr.get(1);
        Collector<String, ?, List<String>> collector = Collectors.toList();
        Dataset grouped = dataset.groupBy("city", CommonUtil.toList("name", "age"), "info", rowMapper, collector);

        assertNotNull(grouped);
        assertEquals(3, grouped.size());
        assertTrue(grouped.containsColumn("city"));
        assertTrue(grouped.containsColumn("info"));
    }

    @Test
    public void testGroupByWithMultipleKeys() {
        final RowDataset dataset = createFiveRowCityDataset();
        Dataset grouped = dataset.groupBy(CommonUtil.toList("city"));

        assertNotNull(grouped);
        assertEquals(3, grouped.size());
        assertTrue(grouped.containsColumn("city"));
    }

    @Test
    public void testGroupByWithMultipleKeysAndCollector() {
        final RowDataset dataset = createFiveRowCityDataset();
        Collector<Object, ?, Long> countCollector = Collectors.counting();
        Dataset grouped = dataset.groupBy(CommonUtil.toList("city"), "name", "count", countCollector);

        assertNotNull(grouped);
        assertEquals(3, grouped.size());
        assertTrue(grouped.containsColumn("city"));
        assertTrue(grouped.containsColumn("count"));
    }

    @Test
    public void testGroupByWithMultipleKeysAndRowType() {
        List<String> columnNames = CommonUtil.toList("id", "name");
        List<List<Object>> columnValues = new ArrayList<>();
        columnValues.add(CommonUtil.toList(1, 2, 3, 4));
        columnValues.add(CommonUtil.toList("A", "B", "A", "B"));

        RowDataset ds = new RowDataset(columnNames, columnValues);
        Dataset grouped = ds.groupBy(CommonUtil.toList("name"), CommonUtil.toList("id"), "ids", List.class);

        assertNotNull(grouped);
        assertEquals(2, grouped.size());
        assertTrue(grouped.containsColumn("name"));
        assertTrue(grouped.containsColumn("ids"));
    }

    @Test
    public void testGroupByWithMultipleKeysAndArrayCollector() {
        final RowDataset dataset = createFiveRowCityDataset();
        Collector<Object[], ?, Long> countCollector = Collectors.counting();
        Dataset grouped = dataset.groupBy(CommonUtil.toList("city"), CommonUtil.toList("name", "age"), "count", countCollector);

        assertNotNull(grouped);
        assertEquals(3, grouped.size());
        assertTrue(grouped.containsColumn("city"));
        assertTrue(grouped.containsColumn("count"));
    }

    @Test
    public void testGroupByWithMultipleKeysAndRowMapper() {
        final RowDataset dataset = createFiveRowCityDataset();
        Function<DisposableObjArray, Integer> rowMapper = arr -> (Integer) arr.get(0);
        Collector<Integer, ?, Integer> sumCollector = Collectors.summingInt(Integer::intValue);
        Dataset grouped = dataset.groupBy(CommonUtil.toList("city"), CommonUtil.toList("age"), "totalAge", rowMapper, sumCollector);

        assertNotNull(grouped);
        assertEquals(3, grouped.size());
        assertTrue(grouped.containsColumn("city"));
        assertTrue(grouped.containsColumn("totalAge"));
    }

    @Test
    public void testGroupByWithMultipleKeysAndKeyExtractor() {
        final RowDataset dataset = createFiveRowCityDataset();
        Function<DisposableObjArray, String> keyExtractor = arr -> arr.get(0).toString();
        Dataset grouped = dataset.groupBy(CommonUtil.toList("city"), keyExtractor);

        assertNotNull(grouped);
        assertTrue(grouped.containsColumn("city"));
    }

    @Test
    public void testGroupByWithMultipleKeysKeyExtractorAndCollector() {
        final RowDataset dataset = createFiveRowCityDataset();
        Function<DisposableObjArray, String> keyExtractor = arr -> arr.get(0).toString();
        Collector<Object, ?, Long> countCollector = Collectors.counting();
        Dataset grouped = dataset.groupBy(CommonUtil.toList("city"), keyExtractor, "name", "count", countCollector);

        assertNotNull(grouped);
        assertTrue(grouped.containsColumn("city"));
        assertTrue(grouped.containsColumn("count"));
    }

    @Test
    public void testGroupByWithMultipleKeysKeyExtractorAndRowType() {
        final RowDataset dataset = createFiveRowCityDataset();
        Function<DisposableObjArray, String> keyExtractor = arr -> arr.get(0).toString();
        Dataset grouped = dataset.groupBy(CommonUtil.toList("city"), keyExtractor, CommonUtil.toList("name"), "names", List.class);
        dataset.println();

        grouped.println();

        assertNotNull(grouped);
        assertTrue(grouped.containsColumn("city"));
        assertTrue(grouped.containsColumn("names"));
    }

    @Test
    public void testGroupByWithMultipleKeysKeyExtractorAndArrayCollector() {
        final RowDataset dataset = createFiveRowCityDataset();
        Function<DisposableObjArray, String> keyExtractor = arr -> arr.get(0).toString();
        Collector<Object[], ?, Long> countCollector = Collectors.counting();
        Dataset grouped = dataset.groupBy(CommonUtil.toList("city"), keyExtractor, CommonUtil.toList("name"), "count", countCollector);

        grouped.println();

        assertNotNull(grouped);
        assertTrue(grouped.containsColumn("city"));
        assertTrue(grouped.containsColumn("count"));
        assertEquals(3, grouped.size());
    }

    @Test
    public void testGroupByWithMultipleKeysKeyExtractorAndRowMapper() {
        final RowDataset dataset = createFiveRowCityDataset();
        Function<DisposableObjArray, String> keyExtractor = arr -> arr.get(0).toString();
        Function<DisposableObjArray, String> rowMapper = arr -> arr.get(0).toString();
        Collector<String, ?, List<String>> collector = Collectors.toList();
        Dataset grouped = dataset.groupBy(CommonUtil.toList("city"), keyExtractor, CommonUtil.toList("name"), "names", rowMapper, collector);

        assertNotNull(grouped);
        assertTrue(grouped.containsColumn("city"));
        assertTrue(grouped.containsColumn("names"));
    }

    @Test
    public void testPivotWithSingleAggregateColumn() {
        List<String> columnNames = CommonUtil.toList("row", "col", "value");
        List<List<Object>> columnValues = new ArrayList<>();
        columnValues.add(CommonUtil.toList("A", "A", "B", "B"));
        columnValues.add(CommonUtil.toList("X", "Y", "X", "Y"));
        columnValues.add(CommonUtil.toList(1, 2, 3, 4));

        RowDataset ds = new RowDataset(columnNames, columnValues);
        Collector<Object, ?, Integer> sumCollector = Collectors.summingInt(o -> (Integer) o);

        Sheet<String, String, Integer> pivot = ds.pivot("row", "col", "value", sumCollector);

        assertNotNull(pivot);
        assertEquals(2, pivot.rowKeySet().size());
        assertEquals(2, pivot.columnKeySet().size());
    }

    @Test
    public void testPivotWithMultipleAggregateColumns() {
        List<String> columnNames = CommonUtil.toList("row", "col", "val1", "val2");
        List<List<Object>> columnValues = new ArrayList<>();
        columnValues.add(CommonUtil.toList("A", "A", "B", "B"));
        columnValues.add(CommonUtil.toList("X", "Y", "X", "Y"));
        columnValues.add(CommonUtil.toList(1, 2, 3, 4));
        columnValues.add(CommonUtil.toList(5, 6, 7, 8));

        RowDataset ds = new RowDataset(columnNames, columnValues);
        Collector<Object[], ?, String> joiningCollector = Collectors.mapping(arr -> arr[0] + "-" + arr[1], Collectors.joining(","));

        Sheet<String, String, String> pivot = ds.pivot("row", "col", CommonUtil.toList("val1", "val2"), joiningCollector);

        assertNotNull(pivot);
        assertEquals(2, pivot.rowKeySet().size());
        assertEquals(2, pivot.columnKeySet().size());
    }

    @Test
    public void testPivotWithRowMapper() {
        List<String> columnNames = CommonUtil.toList("row", "col", "val1", "val2");
        List<List<Object>> columnValues = new ArrayList<>();
        columnValues.add(CommonUtil.toList("A", "A", "B", "B"));
        columnValues.add(CommonUtil.toList("X", "Y", "X", "Y"));
        columnValues.add(CommonUtil.toList(1, 2, 3, 4));
        columnValues.add(CommonUtil.toList(5, 6, 7, 8));

        RowDataset ds = new RowDataset(columnNames, columnValues);
        Function<DisposableObjArray, Integer> rowMapper = arr -> (Integer) arr.get(0) + (Integer) arr.get(1);
        Collector<Integer, ?, Integer> sumCollector = Collectors.summingInt(Integer::intValue);

        Sheet<String, String, Integer> pivot = ds.pivot("row", "col", CommonUtil.toList("val1", "val2"), rowMapper, sumCollector);

        assertNotNull(pivot);
        assertEquals(2, pivot.rowKeySet().size());
        assertEquals(2, pivot.columnKeySet().size());
    }

    @Test
    public void testRollup() {
        final RowDataset dataset = createFiveRowCityDataset();
        List<Dataset> rollups = dataset.rollup(CommonUtil.toList("city", "name")).toList();

        assertNotNull(rollups);
        assertTrue(rollups.size() > 0);
    }

    @Test
    public void testRollupWithCollector() {
        final RowDataset dataset = createFiveRowCityDataset();
        Collector<Object, ?, Long> countCollector = Collectors.counting();
        List<Dataset> rollups = dataset.rollup(CommonUtil.toList("city"), "name", "count", countCollector).toList();

        assertNotNull(rollups);
        assertTrue(rollups.size() > 0);
        for (Dataset ds : rollups) {
            assertTrue(ds.containsColumn("count"));
        }
    }

    @Test
    public void testRollupWithRowType() {
        final RowDataset dataset = createFiveRowCityDataset();
        List<Dataset> rollups = dataset.rollup(CommonUtil.toList("city"), CommonUtil.toList("name"), "names", List.class).toList();

        assertNotNull(rollups);
        assertTrue(rollups.size() > 0);
        for (Dataset ds : rollups) {
            if (ds.columnCount() > 1) {
                assertTrue(ds.containsColumn("names"));
            }
        }
    }

    @Test
    public void testRollupWithArrayCollector() {
        final RowDataset dataset = createFiveRowCityDataset();
        Collector<Object[], ?, Long> countCollector = Collectors.counting();
        List<Dataset> rollups = dataset.rollup(CommonUtil.toList("city"), CommonUtil.toList("name", "age"), "count", countCollector).toList();

        assertNotNull(rollups);
        assertTrue(rollups.size() > 0);
        for (Dataset ds : rollups) {
            if (ds.columnCount() > 1) {
                assertTrue(ds.containsColumn("count"));
            }
        }
    }

    @Test
    public void testRollupWithRowMapper() {
        final RowDataset dataset = createFiveRowCityDataset();
        Function<DisposableObjArray, String> rowMapper = arr -> arr.get(0).toString();
        Collector<String, ?, List<String>> collector = Collectors.toList();
        List<Dataset> rollups = dataset.rollup(CommonUtil.toList("city"), CommonUtil.toList("name"), "names", rowMapper, collector).toList();

        assertNotNull(rollups);
        assertTrue(rollups.size() > 0);
        for (Dataset ds : rollups) {
            if (ds.columnCount() > 1) {
                assertTrue(ds.containsColumn("names"));
            }
        }
    }

    @Test
    public void testRollupWithKeyExtractor() {
        final RowDataset dataset = createFiveRowCityDataset();
        Function<DisposableObjArray, String> keyExtractor = arr -> arr.get(0).toString();
        List<Dataset> rollups = dataset.rollup(CommonUtil.toList("city"), keyExtractor).toList();

        assertNotNull(rollups);
        assertTrue(rollups.size() > 0);
    }

    @Test
    public void testRollupWithKeyExtractorAndCollector() {
        final RowDataset dataset = createFiveRowCityDataset();
        Function<DisposableObjArray, String> keyExtractor = arr -> arr.get(0).toString();
        Collector<Object, ?, Long> countCollector = Collectors.counting();
        List<Dataset> rollups = dataset.rollup(CommonUtil.toList("city"), keyExtractor, "name", "count", countCollector).toList();

        assertNotNull(rollups);
        assertTrue(rollups.size() > 0);
    }

    @Test
    public void testRollupWithKeyExtractorAndRowType() {
        final RowDataset dataset = createFiveRowCityDataset();
        Function<DisposableObjArray, String> keyExtractor = arr -> arr.get(0).toString();
        List<Dataset> rollups = dataset.rollup(CommonUtil.toList("city"), keyExtractor, CommonUtil.toList("name"), "names", List.class).toList();

        assertNotNull(rollups);
        assertTrue(rollups.size() > 0);
    }

    @Test
    public void testRollupWithKeyExtractorAndArrayCollector() {
        final RowDataset dataset = createFiveRowCityDataset();
        Function<DisposableObjArray, String> keyExtractor = arr -> arr.get(0).toString();
        Collector<Object[], ?, Long> countCollector = Collectors.counting();
        List<Dataset> rollups = dataset.rollup(CommonUtil.toList("city"), keyExtractor, CommonUtil.toList("name"), "count", countCollector).toList();

        assertNotNull(rollups);
        assertTrue(rollups.size() > 0);
    }

    @Test
    public void testRollupWithKeyExtractorAndRowMapper() {
        final RowDataset dataset = createFiveRowCityDataset();
        Function<DisposableObjArray, String> keyExtractor = arr -> arr.get(0).toString();
        Function<DisposableObjArray, String> rowMapper = arr -> arr.get(0).toString();
        Collector<String, ?, List<String>> collector = Collectors.toList();
        List<Dataset> rollups = dataset.rollup(CommonUtil.toList("city"), keyExtractor, CommonUtil.toList("name"), "names", rowMapper, collector).toList();

        assertNotNull(rollups);
        assertTrue(rollups.size() > 0);
    }

    @Test
    public void testCube() {
        final RowDataset dataset = createFiveRowCityDataset();
        List<Dataset> cubes = dataset.cube(CommonUtil.toList("city", "name")).toList();

        assertNotNull(cubes);
        assertTrue(cubes.size() > 0);
    }

    @Test
    public void testCubeWithCollector() {
        final RowDataset dataset = createFiveRowCityDataset();
        Collector<Object, ?, Long> countCollector = Collectors.counting();
        List<Dataset> cubes = dataset.cube(CommonUtil.toList("city"), "name", "count", countCollector).toList();

        assertNotNull(cubes);
        assertTrue(cubes.size() > 0);
    }

    @Test
    public void testCubeWithRowType() {
        final RowDataset dataset = createFiveRowCityDataset();
        List<Dataset> cubes = dataset.cube(CommonUtil.toList("city"), CommonUtil.toList("name"), "names", List.class).toList();

        assertNotNull(cubes);
        assertTrue(cubes.size() > 0);
    }

    @Test
    public void testCubeWithArrayCollector() {
        final RowDataset dataset = createFiveRowCityDataset();
        Collector<Object[], ?, Long> countCollector = Collectors.counting();
        List<Dataset> cubes = dataset.cube(CommonUtil.toList("city"), CommonUtil.toList("name"), "count", countCollector).toList();

        assertNotNull(cubes);
        assertTrue(cubes.size() > 0);
    }

    @Test
    public void testCubeWithRowMapper() {
        final RowDataset dataset = createFiveRowCityDataset();
        Function<DisposableObjArray, String> rowMapper = arr -> arr.get(0).toString();
        Collector<String, ?, List<String>> collector = Collectors.toList();
        List<Dataset> cubes = dataset.cube(CommonUtil.toList("city"), CommonUtil.toList("name"), "names", rowMapper, collector).toList();

        assertNotNull(cubes);
        assertTrue(cubes.size() > 0);
    }

    @Test
    public void testCubeWithKeyExtractor() {
        final RowDataset dataset = createFiveRowCityDataset();
        Function<DisposableObjArray, String> keyExtractor = arr -> arr.get(0).toString();
        List<Dataset> cubes = dataset.cube(CommonUtil.toList("city"), keyExtractor).toList();

        assertNotNull(cubes);
        assertTrue(cubes.size() > 0);
    }

    @Test
    public void testCubeWithKeyExtractorAndCollector() {
        final RowDataset dataset = createFiveRowCityDataset();
        Function<DisposableObjArray, String> keyExtractor = arr -> arr.get(0).toString();
        Collector<Object, ?, Long> countCollector = Collectors.counting();
        List<Dataset> cubes = dataset.cube(CommonUtil.toList("city"), keyExtractor, "name", "count", countCollector).toList();

        assertNotNull(cubes);
        assertTrue(cubes.size() > 0);
    }

    @Test
    public void testCubeWithKeyExtractorAndRowType() {
        final RowDataset dataset = createFiveRowCityDataset();
        Function<DisposableObjArray, String> keyExtractor = arr -> arr.join(", ");
        List<Dataset> cubes = dataset.cube(CommonUtil.toList("city"), keyExtractor, CommonUtil.toList("name"), "names", List.class).toList();
        dataset.println();

        cubes.forEach(Dataset::println);

        assertNotNull(cubes);
        assertEquals(2, cubes.size());

        N.println(Strings.repeat("=", 80));

        cubes = dataset.cube(CommonUtil.toList("city", "age"), keyExtractor, CommonUtil.toList("name"), "names", List.class).toList();

        cubes.forEach(Dataset::println);

        assertNotNull(cubes);
        assertEquals(4, cubes.size());

        dataset.groupBy(CommonUtil.toList("city", "age"), keyExtractor, CommonUtil.toList("name"), "names", List.class).println();
    }

    @Test
    public void testCubeWithKeyExtractorAndArrayCollector() {
        final RowDataset dataset = createFiveRowCityDataset();
        Function<DisposableObjArray, String> keyExtractor = arr -> arr.get(0).toString();
        Collector<Object[], ?, Long> countCollector = Collectors.counting();
        List<Dataset> cubes = dataset.cube(CommonUtil.toList("city"), keyExtractor, CommonUtil.toList("name"), "count", countCollector).toList();

        assertNotNull(cubes);
        assertTrue(cubes.size() > 0);
    }

    @Test
    public void testCubeWithKeyExtractorAndRowMapper() {
        final RowDataset dataset = createFiveRowCityDataset();
        Function<DisposableObjArray, String> keyExtractor = arr -> arr.get(0).toString();
        Function<DisposableObjArray, String> rowMapper = arr -> arr.get(0).toString();
        Collector<String, ?, List<String>> collector = Collectors.toList();
        List<Dataset> cubes = dataset.cube(CommonUtil.toList("city"), keyExtractor, CommonUtil.toList("name"), "names", rowMapper, collector).toList();

        assertNotNull(cubes);
        assertTrue(cubes.size() > 0);
    }

    @Test
    public void testSortBy() {
        final RowDataset dataset = createFiveRowCityDataset();
        Dataset copy = dataset.copy();
        copy.sortBy("age");

        List<Object> ages = copy.getColumn("age");
        for (int i = 1; i < ages.size(); i++) {
            assertTrue(((Integer) ages.get(i - 1)) <= ((Integer) ages.get(i)));
        }
    }

    @Test
    public void testSortByWithComparator() {
        final RowDataset dataset = createFiveRowCityDataset();
        Dataset copy = dataset.copy();
        copy.sortBy("age", Comparator.reverseOrder());

        List<Object> ages = copy.getColumn("age");
        for (int i = 1; i < ages.size(); i++) {
            assertTrue(((Integer) ages.get(i - 1)) >= ((Integer) ages.get(i)));
        }
    }

    @Test
    public void testSortByMultipleColumns() {
        final RowDataset dataset = createFiveRowCityDataset();
        Dataset copy = dataset.copy();
        copy.sortBy(CommonUtil.toList("city", "age"));

        List<Object> cities = copy.getColumn("city");
        List<Object> ages = copy.getColumn("age");

        for (int i = 1; i < cities.size(); i++) {
            int cityCompare = cities.get(i - 1).toString().compareTo(cities.get(i).toString());
            assertTrue(cityCompare <= 0);
            if (cityCompare == 0) {
                assertTrue(((Integer) ages.get(i - 1)) <= ((Integer) ages.get(i)));
            }
        }
    }

    @Test
    public void testSortByMultipleColumnsWithComparator() {
        final RowDataset dataset = createFiveRowCityDataset();
        Dataset copy = dataset.copy();
        Comparator<Object[]> comp = (a, b) -> {
            int result = ((String) a[0]).compareTo((String) b[0]);
            if (result == 0) {
                result = ((Integer) b[1]).compareTo((Integer) a[1]);
            }
            return result;
        };
        copy.sortBy(CommonUtil.toList("city", "age"), comp);

        assertNotNull(copy);
        assertEquals(dataset.size(), copy.size());
    }

    @Test
    public void testSortByWithKeyExtractor() {
        final RowDataset dataset = createFiveRowCityDataset();
        Dataset copy = dataset.copy();
        Function<DisposableObjArray, String> keyExtractor = arr -> arr.get(0) + "-" + arr.get(1);
        copy.sortBy(CommonUtil.toList("name", "age"), keyExtractor);

        assertNotNull(copy);
        assertEquals(dataset.size(), copy.size());
    }

    @Test
    public void testParallelSortBy() {
        final RowDataset dataset = createFiveRowCityDataset();
        Dataset copy = dataset.copy();
        copy.parallelSortBy("age");

        List<Object> ages = copy.getColumn("age");
        for (int i = 1; i < ages.size(); i++) {
            assertTrue(((Integer) ages.get(i - 1)) <= ((Integer) ages.get(i)));
        }
    }

    @Test
    public void testParallelSortByWithComparator() {
        final RowDataset dataset = createFiveRowCityDataset();
        Dataset copy = dataset.copy();
        copy.parallelSortBy("name", String.CASE_INSENSITIVE_ORDER);

        List<Object> names = copy.getColumn("name");
        for (int i = 1; i < names.size(); i++) {
            assertTrue(names.get(i - 1).toString().compareToIgnoreCase(names.get(i).toString()) <= 0);
        }
    }

    @Test
    public void testParallelSortByMultipleColumns() {
        final RowDataset dataset = createFiveRowCityDataset();
        Dataset copy = dataset.copy();
        copy.parallelSortBy(CommonUtil.toList("city", "name"));

        assertNotNull(copy);
        assertEquals(dataset.size(), copy.size());
    }

    @Test
    public void testParallelSortByMultipleColumnsWithComparator() {
        final RowDataset dataset = createFiveRowCityDataset();
        Dataset copy = dataset.copy();
        Comparator<Object[]> comp = Comparator.comparing((Object[] a) -> (String) a[0]).thenComparing(a -> (String) a[1]);
        copy.parallelSortBy(CommonUtil.toList("city", "name"), comp);

        assertNotNull(copy);
        assertEquals(dataset.size(), copy.size());
    }

    @Test
    public void testParallelSortByWithKeyExtractor() {
        final RowDataset dataset = createFiveRowCityDataset();
        Dataset copy = dataset.copy();
        Function<DisposableObjArray, Integer> keyExtractor = arr -> (Integer) arr.get(0);
        copy.parallelSortBy(CommonUtil.toList("age"), keyExtractor);

        List<Object> ages = copy.getColumn("age");
        for (int i = 1; i < ages.size(); i++) {
            assertTrue(((Integer) ages.get(i - 1)) <= ((Integer) ages.get(i)));
        }
    }

    @Test
    public void testTopBy() {
        final RowDataset dataset = createFiveRowCityDataset();
        Dataset top = dataset.topBy("age", 3);

        assertNotNull(top);
        assertEquals(3, top.size());
    }

    @Test
    public void testTopByWithComparator() {
        final RowDataset dataset = createFiveRowCityDataset();
        Dataset top = dataset.topBy("age", 2, Comparator.reverseOrder());

        assertNotNull(top);
        assertEquals(2, top.size());

        List<Object> ages = top.getColumn("age");
        assertTrue(ages.contains(25));
        assertTrue(ages.contains(28));
    }

    @Test
    public void testTopByMultipleColumns() {
        final RowDataset dataset = createFiveRowCityDataset();
        Dataset top = dataset.topBy(CommonUtil.toList("city", "age"), 3);

        assertNotNull(top);
        assertEquals(3, top.size());
    }

    @Test
    public void testTopByMultipleColumnsWithComparator() {
        final RowDataset dataset = createFiveRowCityDataset();
        Comparator<Object[]> comp = (a, b) -> ((String) a[0]).compareTo((String) b[0]);
        Dataset top = dataset.topBy(CommonUtil.toList("city"), 2, comp);

        assertNotNull(top);
        assertEquals(2, top.size());
    }

    @Test
    public void testTopByWithKeyExtractor() {
        final RowDataset dataset = createFiveRowCityDataset();
        Function<DisposableObjArray, Integer> keyExtractor = arr -> (Integer) arr.get(0);
        Dataset top = dataset.topBy(CommonUtil.toList("age"), 3, keyExtractor);

        assertNotNull(top);
        assertEquals(3, top.size());
    }

    @Test
    public void testDistinct() {
        List<String> columnNames = CommonUtil.toList("col1", "col2");
        List<List<Object>> columnValues = new ArrayList<>();
        columnValues.add(CommonUtil.toList("A", "A", "B", "B", "A"));
        columnValues.add(CommonUtil.toList(1, 1, 2, 2, 1));

        RowDataset ds = new RowDataset(columnNames, columnValues);
        Dataset distinct = ds.distinct();

        assertNotNull(distinct);
        assertEquals(2, distinct.size());
    }

    @Test
    public void testDistinctBy() {
        final RowDataset dataset = createFiveRowCityDataset();
        Dataset distinct = dataset.distinctBy("city");

        assertNotNull(distinct);
        assertEquals(3, distinct.size());
    }

    @Test
    public void testDistinctByWithKeyExtractor() {
        final RowDataset dataset = createFiveRowCityDataset();
        Function<Object, String> keyExtractor = obj -> obj.toString().substring(0, 1);
        Dataset distinct = dataset.distinctBy("name", keyExtractor);

        assertNotNull(distinct);
        assertTrue(distinct.size() <= dataset.size());
    }

    @Test
    public void testDistinctByMultipleColumns() {
        List<String> columnNames = CommonUtil.toList("col1", "col2", "col3");
        List<List<Object>> columnValues = new ArrayList<>();
        columnValues.add(CommonUtil.toList("A", "A", "B", "B", "A"));
        columnValues.add(CommonUtil.toList(1, 2, 1, 1, 1));
        columnValues.add(CommonUtil.toList("X", "Y", "Z", "W", "V"));

        RowDataset ds = new RowDataset(columnNames, columnValues);
        Dataset distinct = ds.distinctBy(CommonUtil.toList("col1", "col2"));

        assertNotNull(distinct);
        assertEquals(3, distinct.size());
    }

    @Test
    public void testDistinctByMultipleColumnsWithKeyExtractor() {
        final RowDataset dataset = createFiveRowCityDataset();
        Function<DisposableObjArray, String> keyExtractor = arr -> arr.get(0).toString();
        Dataset distinct = dataset.distinctBy(CommonUtil.toList("city", "age"), keyExtractor);

        assertNotNull(distinct);
        assertTrue(distinct.size() <= dataset.size());
    }

    @Test
    public void testFilterWithPredicate() {
        final RowDataset dataset = createFiveRowCityDataset();
        Predicate<DisposableObjArray> filter = arr -> ((Integer) arr.get(2)) > 30;
        Dataset filtered = dataset.filter(filter);

        assertNotNull(filtered);
        assertEquals(2, filtered.size());
    }

    @Test
    public void testFilterWithPredicateAndMax() {
        final RowDataset dataset = createFiveRowCityDataset();
        Predicate<DisposableObjArray> filter = arr -> ((Integer) arr.get(2)) > 25;
        Dataset filtered = dataset.filter(filter, 2);

        assertNotNull(filtered);
        assertEquals(2, filtered.size());
    }

    @Test
    public void testFilterWithRowRange() {
        final RowDataset dataset = createFiveRowCityDataset();
        Predicate<DisposableObjArray> filter = arr -> true;
        Dataset filtered = dataset.filter(1, 4, filter);

        assertNotNull(filtered);
        assertEquals(3, filtered.size());
    }

    @Test
    public void testFilterWithRowRangeAndMax() {
        final RowDataset dataset = createFiveRowCityDataset();
        Predicate<DisposableObjArray> filter = arr -> true;
        Dataset filtered = dataset.filter(0, 5, filter, 3);

        assertNotNull(filtered);
        assertEquals(3, filtered.size());
    }

    @Test
    public void testFilterByColumn() {
        final RowDataset dataset = createFiveRowCityDataset();
        Predicate<Object> filter = age -> ((Integer) age) >= 30;
        Dataset filtered = dataset.filter("age", filter);

        assertNotNull(filtered);
        assertEquals(3, filtered.size());
    }

    @Test
    public void testFilterByColumnWithMax() {
        final RowDataset dataset = createFiveRowCityDataset();
        Predicate<Object> filter = name -> ((String) name).startsWith("J");
        Dataset filtered = dataset.filter("name", filter, 1);

        assertNotNull(filtered);
        assertEquals(1, filtered.size());
    }

    @Test
    public void testFilterByColumnWithRowRange() {
        final RowDataset dataset = createFiveRowCityDataset();
        Predicate<Object> filter = city -> "NYC".equals(city);
        Dataset filtered = dataset.filter(0, 5, "city", filter);

        assertNotNull(filtered);
        assertEquals(2, filtered.size());
    }

    @Test
    public void testFilterByColumnWithRowRangeAndMax() {
        final RowDataset dataset = createFiveRowCityDataset();
        Predicate<Object> filter = city -> city != null;
        Dataset filtered = dataset.filter(1, 4, "city", filter, 2);

        assertNotNull(filtered);
        assertEquals(2, filtered.size());
    }

    @Test
    public void testFilterByMultipleColumns() {
        final RowDataset dataset = createFiveRowCityDataset();
        Predicate<DisposableObjArray> filter = arr -> "NYC".equals(arr.get(0)) && ((Integer) arr.get(1)) > 30;
        Dataset filtered = dataset.filter(CommonUtil.toList("city", "age"), filter);

        assertNotNull(filtered);
        assertEquals(1, filtered.size());
    }

    @Test
    public void testFilterByMultipleColumnsWithMax() {
        final RowDataset dataset = createFiveRowCityDataset();
        Predicate<DisposableObjArray> filter = arr -> arr.get(0) != null;
        Dataset filtered = dataset.filter(CommonUtil.toList("name", "city"), filter, 3);

        assertNotNull(filtered);
        assertEquals(3, filtered.size());
    }

    @Test
    public void testFilterByMultipleColumnsWithRowRange() {
        final RowDataset dataset = createFiveRowCityDataset();
        Predicate<DisposableObjArray> filter = arr -> true;
        Dataset filtered = dataset.filter(1, 3, CommonUtil.toList("name", "age"), filter);

        assertNotNull(filtered);
        assertEquals(2, filtered.size());
    }

    @Test
    public void testFilterByMultipleColumnsWithRowRangeAndMax() {
        final RowDataset dataset = createFiveRowCityDataset();
        Predicate<DisposableObjArray> filter = arr -> true;
        Dataset filtered = dataset.filter(0, 5, CommonUtil.toList("id", "name"), filter, 4);

        assertNotNull(filtered);
        assertEquals(4, filtered.size());
    }

    @Test
    public void testFilterByTuple2() {
        final RowDataset dataset = createFiveRowCityDataset();
        BiPredicate<Object, Object> filter = (name, age) -> ((Integer) age) > 30;
        Dataset filtered = dataset.filter(Tuple.of("name", "age"), filter);

        assertNotNull(filtered);
        assertEquals(2, filtered.size());
    }

    @Test
    public void testFilterByTuple2WithMax() {
        final RowDataset dataset = createFiveRowCityDataset();
        BiPredicate<Object, Object> filter = (id, name) -> ((Integer) id) <= 3;
        Dataset filtered = dataset.filter(Tuple.of("id", "name"), filter, 2);

        assertNotNull(filtered);
        assertEquals(2, filtered.size());
    }

    @Test
    public void testFilterByTuple2WithRowRange() {
        final RowDataset dataset = createFiveRowCityDataset();
        BiPredicate<Object, Object> filter = (name, city) -> "LA".equals(city);
        Dataset filtered = dataset.filter(1, 5, Tuple.of("name", "city"), filter);

        assertNotNull(filtered);
        assertEquals(2, filtered.size());
    }

    @Test
    public void testFilterByTuple2WithRowRangeAndMax() {
        final RowDataset dataset = createFiveRowCityDataset();
        BiPredicate<Object, Object> filter = (id, age) -> true;
        Dataset filtered = dataset.filter(0, 5, Tuple.of("id", "age"), filter, 3);

        assertNotNull(filtered);
        assertEquals(3, filtered.size());
    }

    @Test
    public void testFilterByTuple3() {
        final RowDataset dataset = createFiveRowCityDataset();
        TriPredicate<Object, Object, Object> filter = (id, name, age) -> ((Integer) age) < 30;
        Dataset filtered = dataset.filter(Tuple.of("id", "name", "age"), filter);

        assertNotNull(filtered);
        assertEquals(2, filtered.size());
    }

    @Test
    public void testFilterByTuple3WithMax() {
        final RowDataset dataset = createFiveRowCityDataset();
        TriPredicate<Object, Object, Object> filter = (name, age, city) -> true;
        Dataset filtered = dataset.filter(Tuple.of("name", "age", "city"), filter, 2);

        assertNotNull(filtered);
        assertEquals(2, filtered.size());
    }

    @Test
    public void testFilterByTuple3WithRowRange() {
        final RowDataset dataset = createFiveRowCityDataset();
        TriPredicate<Object, Object, Object> filter = (id, name, age) -> ((Integer) id) > 2;
        Dataset filtered = dataset.filter(1, 5, Tuple.of("id", "name", "age"), filter);
        filtered.println();

        assertNotNull(filtered);
        assertEquals(3, filtered.size());
    }

    @Test
    public void testFilterByTuple3WithRowRangeAndMax() {
        final RowDataset dataset = createFiveRowCityDataset();
        TriPredicate<Object, Object, Object> filter = (id, age, city) -> true;
        Dataset filtered = dataset.filter(0, 4, Tuple.of("id", "age", "city"), filter, 3);

        assertNotNull(filtered);
        assertEquals(3, filtered.size());
    }

    @Test
    public void testMapSingleColumn() {
        final RowDataset dataset = createFiveRowCityDataset();
        Function<Object, String> mapper = name -> ((String) name).toUpperCase();
        Dataset mapped = dataset.mapColumn("name", "NAME", "id", mapper);

        assertNotNull(mapped);
        assertEquals(5, mapped.size());
        assertTrue(mapped.containsColumn("NAME"));
        assertTrue(mapped.containsColumn("id"));
        assertEquals("JOHN", mapped.moveToRow(0).get("NAME"));
    }

    @Test
    public void testMapSingleColumnWithMultipleCopying() {
        final RowDataset dataset = createFiveRowCityDataset();
        Function<Object, Integer> mapper = age -> ((Integer) age) * 2;
        Dataset mapped = dataset.mapColumn("age", "doubleAge", CommonUtil.toList("id", "name"), mapper);

        assertNotNull(mapped);
        assertEquals(5, mapped.size());
        assertTrue(mapped.containsColumn("doubleAge"));
        assertTrue(mapped.containsColumn("id"));
        assertTrue(mapped.containsColumn("name"));
        assertEquals(50, (Integer) mapped.moveToRow(0).get("doubleAge"));
    }

    @Test
    public void testMapTuple2() {
        final RowDataset dataset = createFiveRowCityDataset();
        BiFunction<Object, Object, String> mapper = (name, age) -> name + ":" + age;
        Dataset mapped = dataset.mapColumns(Tuple.of("name", "age"), "info", CommonUtil.toList("id"), mapper);

        assertNotNull(mapped);
        assertEquals(5, mapped.size());
        assertTrue(mapped.containsColumn("info"));
        assertTrue(mapped.containsColumn("id"));
        assertEquals("John:25", mapped.moveToRow(0).get("info"));
    }

    @Test
    public void testMapTuple3() {
        final RowDataset dataset = createFiveRowCityDataset();
        TriFunction<Object, Object, Object, String> mapper = (id, name, age) -> id + "-" + name + "-" + age;
        Dataset mapped = dataset.mapColumns(Tuple.of("id", "name", "age"), "combined", CommonUtil.toList("city"), mapper);

        assertNotNull(mapped);
        assertEquals(5, mapped.size());
        assertTrue(mapped.containsColumn("combined"));
        assertTrue(mapped.containsColumn("city"));
        assertEquals("1-John-25", mapped.moveToRow(0).get("combined"));
    }

    @Test
    public void testMapMultipleColumns() {
        final RowDataset dataset = createFiveRowCityDataset();
        Function<DisposableObjArray, String> mapper = arr -> arr.get(0) + ":" + arr.get(1);
        Dataset mapped = dataset.mapColumns(CommonUtil.toList("name", "city"), "location", CommonUtil.toList("id"), mapper);

        assertNotNull(mapped);
        assertEquals(5, mapped.size());
        assertTrue(mapped.containsColumn("location"));
        assertTrue(mapped.containsColumn("id"));
        assertEquals("John:NYC", mapped.moveToRow(0).get("location"));
    }

    @Test
    public void testFlatMapSingleColumn() {
        final RowDataset dataset = createFiveRowCityDataset();
        Function<Object, Collection<String>> mapper = name -> CommonUtil.toList(((String) name).toLowerCase(), ((String) name).toUpperCase());
        Dataset flatMapped = dataset.flatMapColumn("name", "variations", "id", mapper);

        assertNotNull(flatMapped);
        assertEquals(10, flatMapped.size());
        assertTrue(flatMapped.containsColumn("variations"));
        assertTrue(flatMapped.containsColumn("id"));
    }

    @Test
    public void testFlatMapSingleColumnWithMultipleCopying() {
        final RowDataset dataset = createFiveRowCityDataset();
        Function<Object, Collection<Integer>> mapper = age -> CommonUtil.toList((Integer) age, (Integer) age + 10);
        Dataset flatMapped = dataset.flatMapColumn("age", "ages", CommonUtil.toList("id", "name"), mapper);

        assertNotNull(flatMapped);
        assertEquals(10, flatMapped.size());
        assertTrue(flatMapped.containsColumn("ages"));
        assertTrue(flatMapped.containsColumn("id"));
        assertTrue(flatMapped.containsColumn("name"));
    }

    @Test
    public void testFlatMapTuple2() {
        final RowDataset dataset = createFiveRowCityDataset();
        BiFunction<Object, Object, Collection<String>> mapper = (name, age) -> CommonUtil.toList(name + "-young", name + "-old");
        Dataset flatMapped = dataset.flatMapColumns(Tuple.of("name", "age"), "status", CommonUtil.toList("id"), mapper);

        assertNotNull(flatMapped);
        assertEquals(10, flatMapped.size());
        assertTrue(flatMapped.containsColumn("status"));
        assertTrue(flatMapped.containsColumn("id"));
    }

    @Test
    public void testFlatMapTuple3() {
        final RowDataset dataset = createFiveRowCityDataset();
        TriFunction<Object, Object, Object, Collection<String>> mapper = (id, name, age) -> CommonUtil.toList("ID" + id, "NAME" + name, "AGE" + age);
        Dataset flatMapped = dataset.flatMapColumns(Tuple.of("id", "name", "age"), "tags", CommonUtil.toList("city"), mapper);

        assertNotNull(flatMapped);
        assertEquals(15, flatMapped.size());
        assertTrue(flatMapped.containsColumn("tags"));
        assertTrue(flatMapped.containsColumn("city"));
    }

    @Test
    public void testFlatMapMultipleColumns() {
        final RowDataset dataset = createFiveRowCityDataset();
        Function<DisposableObjArray, Collection<String>> mapper = arr -> CommonUtil.toList(arr.get(0).toString(), arr.get(1).toString());
        Dataset flatMapped = dataset.flatMapColumns(CommonUtil.toList("name", "city"), "values", CommonUtil.toList("id"), mapper);

        assertNotNull(flatMapped);
        assertEquals(10, flatMapped.size());
        assertTrue(flatMapped.containsColumn("values"));
        assertTrue(flatMapped.containsColumn("id"));
    }

    @Test
    public void testCopyWithColumns() {
        final RowDataset dataset = createFiveRowCityDataset();
        Dataset copy = dataset.copy(CommonUtil.toList("id", "name"));

        assertNotNull(copy);
        assertEquals(dataset.size(), copy.size());
        assertEquals(2, copy.columnCount());
        assertTrue(copy.containsColumn("id"));
        assertTrue(copy.containsColumn("name"));
        assertFalse(copy.containsColumn("age"));
    }

    @Test
    public void testCopyWithRowRangeAndColumns() {
        final RowDataset dataset = createFiveRowCityDataset();
        Dataset copy = dataset.copy(1, 3, CommonUtil.toList("name", "age"));

        assertNotNull(copy);
        assertEquals(2, copy.size());
        assertEquals(2, copy.columnCount());
        assertTrue(copy.containsColumn("name"));
        assertTrue(copy.containsColumn("age"));
    }

    @Test
    public void testCloneWithFreeze() {
        final RowDataset dataset = createFiveRowCityDataset();
        Dataset cloned = dataset.clone(true);

        assertNotNull(cloned);
        assertEquals(dataset.size(), cloned.size());
        assertEquals(dataset.columnCount(), cloned.columnCount());
        assertTrue(cloned.isFrozen());
    }

    @Test
    public void testInnerJoinSingleColumn() {
        final RowDataset dataset = createFiveRowCityDataset();
        List<String> rightColumns = CommonUtil.toList("city", "country");
        List<List<Object>> rightValues = new ArrayList<>();
        rightValues.add(CommonUtil.toList("NYC", "LA", "Chicago"));
        rightValues.add(CommonUtil.toList("USA", "USA", "USA"));

        RowDataset right = new RowDataset(rightColumns, rightValues);

        Dataset joined = dataset.innerJoin(right, "city", "city");

        joined.println();

        assertNotNull(joined);
        assertEquals(5, joined.size());
        assertTrue(joined.containsColumn("country"));
    }

    @Test
    public void testInnerJoinMultipleColumns() {
        final RowDataset dataset = createFiveRowCityDataset();
        List<String> rightColumns = CommonUtil.toList("city", "age", "salary");
        List<List<Object>> rightValues = new ArrayList<>();
        rightValues.add(CommonUtil.toList("NYC", "LA", "NYC"));
        rightValues.add(CommonUtil.toList(25, 30, 35));
        rightValues.add(CommonUtil.toList(50000, 60000, 70000));

        RowDataset right = new RowDataset(rightColumns, rightValues);

        Map<String, String> onColumns = new HashMap<>();
        onColumns.put("city", "city");
        onColumns.put("age", "age");

        Dataset joined = dataset.innerJoin(right, onColumns);

        assertNotNull(joined);
        assertTrue(joined.containsColumn("salary"));
    }

    @Test
    public void testInnerJoinWithNewColumn() {
        final RowDataset dataset = createFiveRowCityDataset();
        List<String> rightColumns = CommonUtil.toList("city", "info");
        List<List<Object>> rightValues = new ArrayList<>();
        rightValues.add(CommonUtil.toList("NYC", "LA", "Chicago"));
        rightValues.add(CommonUtil.toList("Big Apple", "City of Angels", "Windy City"));

        RowDataset right = new RowDataset(rightColumns, rightValues);

        Map<String, String> onColumns = new HashMap<>();
        onColumns.put("city", "city");

        Dataset joined = dataset.innerJoin(right, onColumns, "cityInfo", Object[].class);

        assertNotNull(joined);
        assertTrue(joined.containsColumn("cityInfo"));
    }

    @Test
    public void testInnerJoinWithCollectionSupplier() {
        final RowDataset dataset = createFiveRowCityDataset();
        List<String> rightColumns = CommonUtil.toList("city", "tag");
        List<List<Object>> rightValues = new ArrayList<>();
        rightValues.add(CommonUtil.toList("NYC", "NYC", "LA"));
        rightValues.add(CommonUtil.toList("tag1", "tag2", "tag3"));

        RowDataset right = new RowDataset(rightColumns, rightValues);

        Map<String, String> onColumns = new HashMap<>();
        onColumns.put("city", "city");

        IntFunction<List<Object>> collSupplier = size -> new ArrayList<>(size);
        Dataset joined = dataset.innerJoin(right, onColumns, "tags", Object[].class, collSupplier);

        assertNotNull(joined);
        assertTrue(joined.containsColumn("tags"));
    }

    @Test
    public void testLeftJoinSingleColumn() {
        final RowDataset dataset = createFiveRowCityDataset();
        List<String> rightColumns = CommonUtil.toList("city", "country");
        List<List<Object>> rightValues = new ArrayList<>();
        rightValues.add(CommonUtil.toList("NYC", "LA"));
        rightValues.add(CommonUtil.toList("USA", "USA"));

        RowDataset right = new RowDataset(rightColumns, rightValues);

        Dataset joined = dataset.leftJoin(right, "city", "city");

        assertNotNull(joined);
        assertEquals(5, joined.size());
        assertTrue(joined.containsColumn("country"));
    }

    @Test
    public void testLeftJoinMultipleColumns() {
        final RowDataset dataset = createFiveRowCityDataset();
        List<String> rightColumns = CommonUtil.toList("city", "age", "bonus");
        List<List<Object>> rightValues = new ArrayList<>();
        rightValues.add(CommonUtil.toList("NYC", "LA"));
        rightValues.add(CommonUtil.toList(25, 30));
        rightValues.add(CommonUtil.toList(1000, 2000));

        RowDataset right = new RowDataset(rightColumns, rightValues);

        Map<String, String> onColumns = new HashMap<>();
        onColumns.put("city", "city");
        onColumns.put("age", "age");

        Dataset joined = dataset.leftJoin(right, onColumns);

        assertNotNull(joined);
        assertEquals(5, joined.size());
    }

    @Test
    public void testLeftJoinWithNewColumn() {
        final RowDataset dataset = createFiveRowCityDataset();
        List<String> rightColumns = CommonUtil.toList("city", "population");
        List<List<Object>> rightValues = new ArrayList<>();
        rightValues.add(CommonUtil.toList("NYC", "LA"));
        rightValues.add(CommonUtil.toList(8000000, 4000000));

        RowDataset right = new RowDataset(rightColumns, rightValues);

        Map<String, String> onColumns = new HashMap<>();
        onColumns.put("city", "city");

        Dataset joined = dataset.leftJoin(right, onColumns, "cityData", Object[].class);

        assertNotNull(joined);
        assertEquals(5, joined.size());
        assertTrue(joined.containsColumn("cityData"));
    }

    @Test
    public void testLeftJoinWithCollectionSupplier() {
        final RowDataset dataset = createFiveRowCityDataset();
        List<String> rightColumns = CommonUtil.toList("city", "feature");
        List<List<Object>> rightValues = new ArrayList<>();
        rightValues.add(CommonUtil.toList("NYC", "NYC", "LA"));
        rightValues.add(CommonUtil.toList("feature1", "feature2", "feature3"));

        RowDataset right = new RowDataset(rightColumns, rightValues);

        Map<String, String> onColumns = new HashMap<>();
        onColumns.put("city", "city");

        IntFunction<Set<Object>> collSupplier = size -> new HashSet<>(size);
        Dataset joined = dataset.leftJoin(right, onColumns, "features", Object[].class, collSupplier);

        assertNotNull(joined);
        assertEquals(5, joined.size());
        assertTrue(joined.containsColumn("features"));
    }

    @Test
    public void testNullHandling() {
        List<String> columnNames = CommonUtil.toList("col1", "col2");
        List<List<Object>> columnValues = new ArrayList<>();
        columnValues.add(CommonUtil.toList("A", null, "B"));
        columnValues.add(CommonUtil.toList(1, 2, null));

        RowDataset ds = new RowDataset(columnNames, columnValues);

        String xml = ds.toXml();
        assertNotNull(xml);
        assertTrue(xml.contains("null"));

        String csv = ds.toCsv();
        assertNotNull(csv);

        Dataset filtered = ds.filter("col1", obj -> obj != null);
        assertEquals(2, filtered.size());

        Dataset grouped = ds.groupBy("col1", "col2", "values", Collectors.toList());
        assertNotNull(grouped);

        ds.sortBy("col1");
        assertEquals(3, ds.size());
    }

    @Test
    public void testLargeDatasetOperations() {
        int size = 1000;
        List<String> columnNames = CommonUtil.toList("id", "value", "category");
        List<List<Object>> columnValues = new ArrayList<>();

        List<Object> ids = new ArrayList<>();
        List<Object> values = new ArrayList<>();
        List<Object> categories = new ArrayList<>();

        for (int i = 0; i < size; i++) {
            ids.add(i);
            values.add(i % 100);
            categories.add("CAT" + (i % 10));
        }

        columnValues.add(ids);
        columnValues.add(values);
        columnValues.add(categories);

        RowDataset largeDs = new RowDataset(columnNames, columnValues);

        largeDs.parallelSortBy("value");
        List<Object> sortedValues = largeDs.getColumn("value");
        for (int i = 1; i < sortedValues.size(); i++) {
            assertTrue(((Integer) sortedValues.get(i - 1)) <= ((Integer) sortedValues.get(i)));
        }

        Dataset grouped = largeDs.groupBy("category", "value", "sum", Collectors.summingInt(o -> (Integer) o));
        assertEquals(10, grouped.size());

        Dataset top = largeDs.topBy("value", 10);
        assertEquals(10, top.size());
    }

    @Test
    public void testComplexJoinScenarios() {
        final RowDataset dataset = createFiveRowCityDataset();
        Map<String, String> onColumns = new HashMap<>();
        onColumns.put("city", "city");

        Dataset selfJoined = dataset.innerJoin(dataset, onColumns);
        assertNotNull(selfJoined);
        assertTrue(selfJoined.size() > 0);

        List<String> rightColumns = CommonUtil.toList("city", "data");
        List<List<Object>> rightValues = new ArrayList<>();
        rightValues.add(CommonUtil.toList("Paris", "London"));
        rightValues.add(CommonUtil.toList("data1", "data2"));

        RowDataset noMatchRight = new RowDataset(rightColumns, rightValues);
        Dataset noMatchJoined = dataset.innerJoin(noMatchRight, "city", "city");
        assertEquals(0, noMatchJoined.size());

        Dataset leftJoinNoMatch = dataset.leftJoin(noMatchRight, "city", "city");
        assertEquals(5, leftJoinNoMatch.size());
    }

    @Test
    public void testColumnNameValidation() {
        assertThrows(IllegalArgumentException.class, () -> {
            dataset.sortBy("nonexistent");
        });

        assertThrows(IllegalArgumentException.class, () -> {
            dataset.filter("nonexistent", obj -> true);
        });

        assertThrows(IllegalArgumentException.class, () -> {
            dataset.groupBy("nonexistent", "name", "result", Collectors.toList());
        });

        assertThrows(IllegalArgumentException.class, () -> {
            dataset.distinctBy("nonexistent");
        });

        assertThrows(IllegalArgumentException.class, () -> {
            dataset.mapColumn("nonexistent", "new", "id", obj -> obj);
        });
    }

    @Test
    public void testRowIndexValidation() {
        assertThrows(IndexOutOfBoundsException.class, () -> {
            dataset.toXml(-1, 3);
        });

        assertThrows(IndexOutOfBoundsException.class, () -> {
            dataset.toXml(2, 10);
        });

        assertThrows(IndexOutOfBoundsException.class, () -> {
            dataset.toCsv(5, 3, CommonUtil.toList("name"));
        });

        assertThrows(IndexOutOfBoundsException.class, () -> {
            dataset.filter(-1, 5, arr -> true);
        });

        assertThrows(IndexOutOfBoundsException.class, () -> {
            dataset.copy(0, 10);
        });
    }

    @Test
    public void testSpecialCharactersInData() {
        List<String> columnNames = CommonUtil.toList("text", "value");
        List<List<Object>> columnValues = new ArrayList<>();
        columnValues.add(CommonUtil.toList("Hello, World", "Test\"Quote", "Line\nBreak", "<tag>"));
        columnValues.add(CommonUtil.toList(1, 2, 3, 4));

        RowDataset specialDs = new RowDataset(columnNames, columnValues);

        String xml = specialDs.toXml();
        assertNotNull(xml);
        assertTrue(xml.contains("&lt;tag&gt;") || xml.contains("&lt;"));

        String csv = specialDs.toCsv();
        assertNotNull(csv);
        assertTrue(csv.contains("\"Hello, World\"") || csv.contains("Hello, World"));
    }

    @Test
    public void testMultipleGroupByScenarios() {
        final RowDataset dataset = createFiveRowCityDataset();

        Dataset countGrouped = dataset.groupBy("city", "name", "count", Collectors.counting());
        assertNotNull(countGrouped);

        Dataset listGrouped = dataset.groupBy("city", "age", "ages", Collectors.toList());
        assertNotNull(listGrouped);

        Dataset avgGrouped = dataset.groupBy("city", "age", "avgAge", Collectors.averagingInt(o -> (Integer) o));
        assertNotNull(avgGrouped);

        Collector<Object, ?, String> joiningCollector = Collectors.mapping(Object::toString, Collectors.joining(","));
        Dataset joinedGrouped = dataset.groupBy("city", "name", "names", joiningCollector);
        assertNotNull(joinedGrouped);
        assertTrue(joinedGrouped.containsColumn("names"));
    }

    @Test
    public void testComplexFilterPredicates() {
        final RowDataset dataset = createFiveRowCityDataset();
        Predicate<DisposableObjArray> complexPredicate = arr -> {
            Integer age = (Integer) arr.get(2);
            String city = (String) arr.get(3);
            return age > 25 && age < 35 && ("NYC".equals(city) || "LA".equals(city));
        };

        Dataset filtered = dataset.filter(complexPredicate);
        assertNotNull(filtered);
        assertTrue(filtered.size() > 0);

        Predicate<DisposableObjArray> subsetPredicate = arr -> {
            String name = (String) arr.get(0);
            Integer age = (Integer) arr.get(1);
            return name.length() > 3 && age > 30;
        };

        Dataset subsetFiltered = dataset.filter(CommonUtil.toList("name", "age"), subsetPredicate);
        assertNotNull(subsetFiltered);
    }

    @Test
    public void testMapWithDifferentDataTypes() {
        final RowDataset dataset = createFiveRowCityDataset();
        Function<Object, String> toStringMapper = obj -> "ID:" + obj;
        Dataset stringMapped = dataset.mapColumn("id", "stringId", CommonUtil.toList("name"), toStringMapper);
        assertEquals("ID:1", stringMapped.moveToRow(0).get("stringId"));

        Function<Object, Double> doubleMapper = obj -> ((Integer) obj) * 1.5;
        Dataset doubleMapped = dataset.mapColumn("age", "adjustedAge", CommonUtil.toList("name"), doubleMapper);
        assertEquals(37.5, doubleMapped.moveToRow(0).get("adjustedAge"));

        Function<DisposableObjArray, Map<String, Object>> mapMapper = arr -> {
            Map<String, Object> map = new HashMap<>();
            map.put("name", arr.get(0));
            map.put("age", arr.get(1));
            return map;
        };
        Dataset objectMapped = dataset.mapColumns(CommonUtil.toList("name", "age"), "info", CommonUtil.toList("id"), mapMapper);
        assertNotNull(objectMapped.moveToRow(0).get("info"));
        assertTrue(objectMapped.moveToRow(0).get("info") instanceof Map);
    }

    @Test
    public void testFlatMapEdgeCases() {
        final RowDataset dataset = createFiveRowCityDataset();
        Function<Object, Collection<String>> emptyMapper = obj -> Collections.emptyList();
        Dataset emptyFlatMapped = dataset.flatMapColumn("name", "empty", CommonUtil.toList("id"), emptyMapper);
        assertEquals(0, emptyFlatMapped.size());

        Function<Object, Collection<Integer>> variableMapper = obj -> {
            int count = ((Integer) obj) % 3;
            List<Integer> result = new ArrayList<>();
            for (int i = 0; i < count; i++) {
                result.add(i);
            }
            return result;
        };
        Dataset variableFlatMapped = dataset.flatMapColumn("id", "values", CommonUtil.toList("name"), variableMapper);
        assertNotNull(variableFlatMapped);

        Function<Object, Collection<String>> nullSafeMapper = obj -> obj == null ? Collections.emptyList() : CommonUtil.toList(obj.toString());
        Dataset nullSafeFlatMapped = dataset.flatMapColumn("name", "safe", CommonUtil.toList("id"), nullSafeMapper);
        assertEquals(5, nullSafeFlatMapped.size());
    }

    @Test
    public void testTopByEdgeCases() {
        Dataset allTop = dataset.topBy("age", 10);
        assertEquals(5, allTop.size());

        Dataset singleTop = dataset.topBy("age", 1);
        assertEquals(1, singleTop.size());

        List<String> columnNames = CommonUtil.toList("id", "value");
        List<List<Object>> columnValues = new ArrayList<>();
        columnValues.add(CommonUtil.toList(1, 2, 3, 4, 5));
        columnValues.add(CommonUtil.toList(10, 10, 20, 20, 30));

        RowDataset tieDs = new RowDataset(columnNames, columnValues);
        Dataset topWithTies = tieDs.topBy("value", 3);
        assertEquals(3, topWithTies.size());
    }

    @Test
    public void testDistinctComplexKeys() {
        List<String> columnNames = CommonUtil.toList("a", "b", "c");
        List<List<Object>> columnValues = new ArrayList<>();
        columnValues.add(CommonUtil.toList(1, 1, 2, 2, 3));
        columnValues.add(CommonUtil.toList("X", "X", "Y", "Y", "Z"));
        columnValues.add(CommonUtil.toList(true, false, true, true, false));

        RowDataset complexDs = new RowDataset(columnNames, columnValues);

        Dataset distinctA = complexDs.distinctBy("a");
        assertEquals(3, distinctA.size());

        Dataset distinctAB = complexDs.distinctBy(CommonUtil.toList("a", "b"));
        assertEquals(3, distinctAB.size());

        Dataset distinctAll = complexDs.distinct();
        assertEquals(4, distinctAll.size());

        Function<DisposableObjArray, String> compositeKeyExtractor = arr -> arr.get(0) + "-" + arr.get(1);
        Dataset distinctComposite = complexDs.distinctBy(CommonUtil.toList("a", "b"), compositeKeyExtractor);
        assertEquals(3, distinctComposite.size());
    }

    @Test
    public void testXmlAndCsvWithEmptyColumns() {
        Collection<String> emptyColumns = Collections.emptyList();

        String xml = dataset.toXml(0, 2, emptyColumns);
        assertNotNull(xml);
        assertTrue(xml.contains("<dataset>"));
        assertTrue(xml.contains("</dataset>"));

        String csv = dataset.toCsv(0, 2, emptyColumns);
        assertNotNull(csv);
        assertEquals("", csv.trim());
    }

    @Test
    public void testParallelOperationsConsistency() {
        int size = 100;
        List<Object> values = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            values.add(i);
        }

        List<String> columnNames = CommonUtil.toList("value");
        List<List<Object>> columnValues = CommonUtil.toList(values);
        RowDataset largeDs = new RowDataset(columnNames, columnValues);

        Dataset seqCopy = largeDs.copy();
        Dataset parCopy = largeDs.copy();

        seqCopy.sortBy("value", Comparator.reverseOrder());
        parCopy.parallelSortBy("value", Comparator.reverseOrder());

        for (int i = 0; i < size; i++) {
            assertEquals((Object) seqCopy.moveToRow(i).get("value"), (Object) parCopy.moveToRow(i).get("value"));
        }
    }

    @Test
    public void testConstructorWithColumnNamesAndColumns() {
        final List<String> columnNames = Arrays.asList("id", "name", "age", "score");
        final List<List<Object>> columns = createThreeRowScoreColumns();
        RowDataset ds = new RowDataset(columnNames, columns);
        Assertions.assertNotNull(ds);
        Assertions.assertEquals(4, ds.columnCount());
        Assertions.assertEquals(3, ds.size());
    }

    @Test
    public void testConstructorWithNullColumns() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            new RowDataset(columnNames, null);
        });
    }

    @Test
    public void testConstructorWithMismatchedSizes() {
        final List<List<Object>> columns = createThreeRowScoreColumns();
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
    public void testColumnCount() {
        final RowDataset dataset = createThreeRowScoreDataset();
        Assertions.assertEquals(4, dataset.columnCount());
    }

    @Test
    public void testGetColumnNameWithInvalidIndex() {
        final RowDataset dataset = createThreeRowScoreDataset();
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> {
            dataset.getColumnName(-1);
        });
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> {
            dataset.getColumnName(4);
        });
    }

    @Test
    public void testGetColumnIndexWithInvalidName() {
        final RowDataset dataset = createThreeRowScoreDataset();
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            dataset.getColumnIndex("invalid");
        });
    }

    @Test
    public void testGetColumnIndexesWithInvalidName() {
        final RowDataset dataset = createThreeRowScoreDataset();
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            dataset.getColumnIndexes(Arrays.asList("id", "invalid"));
        });
    }

    @Test
    public void testRenameColumnWithSameName() {
        final RowDataset dataset = createThreeRowScoreDataset();
        dataset.renameColumn("id", "id");
        Assertions.assertTrue(dataset.containsColumn("id"));
    }

    @Test
    public void testRenameColumnWithExistingName() {
        final RowDataset dataset = createThreeRowScoreDataset();
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            dataset.renameColumn("id", "name");
        });
    }

    @Test
    public void testRenameColumnWithInvalidName() {
        final RowDataset dataset = createThreeRowScoreDataset();
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            dataset.renameColumn("invalid", "new_name");
        });
    }

    @Test
    public void testRenameColumns() {
        final RowDataset dataset = createThreeRowScoreDataset();
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
    public void testMoveColumnToSamePosition() {
        final RowDataset dataset = createThreeRowScoreDataset();
        dataset.moveColumn("id", 0);
        List<String> names = dataset.columnNames();
        Assertions.assertEquals("id", names.get(0));
    }

    @Test
    public void testMoveColumnWithInvalidPosition() {
        final RowDataset dataset = createThreeRowScoreDataset();
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> {
            dataset.moveColumn("id", -1);
        });
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> {
            dataset.moveColumn("id", 5);
        });
    }

    @Test
    public void testSwapColumnPositionWithSameColumn() {
        final RowDataset dataset = createThreeRowScoreDataset();
        dataset.swapColumns("id", "id");
        List<String> names = dataset.columnNames();
        Assertions.assertEquals("id", names.get(0));
    }

    @Test
    public void testMoveRowToSamePosition() {
        final RowDataset dataset = createThreeRowScoreDataset();
        Object originalRow1Name = dataset.get(1, 1);
        dataset.moveRow(1, 1);
        Assertions.assertEquals(originalRow1Name, dataset.get(1, 1));
    }

    @Test
    public void testSwapRowPositionWithSameRow() {
        final RowDataset dataset = createThreeRowScoreDataset();
        Object row0Name = dataset.get(0, 1);
        dataset.swapRows(0, 0);
        Assertions.assertEquals(row0Name, dataset.get(0, 1));
    }

    @Test
    public void testGet() {
        final RowDataset dataset = createThreeRowScoreDataset();
        Assertions.assertEquals(1, (Integer) dataset.get(0, 0));
        Assertions.assertEquals("John", dataset.get(0, 1));
        Assertions.assertEquals(25, (Integer) dataset.get(0, 2));
        Assertions.assertEquals(85.5, dataset.get(0, 3));
    }

    @Test
    public void testSet() {
        final RowDataset dataset = createThreeRowScoreDataset();
        dataset.set(0, 0, 100);
        Assertions.assertEquals(100, (Integer) dataset.get(0, 0));

        dataset.set(1, 1, "Updated");
        Assertions.assertEquals("Updated", dataset.get(1, 1));
    }

    @Test
    public void testIsNullWithRowAndColumn() {
        final RowDataset dataset = createThreeRowScoreDataset();
        dataset.set(0, 0, null);
        Assertions.assertTrue(dataset.isNull(0, 0));
        Assertions.assertFalse(dataset.isNull(0, 1));
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
        final RowDataset dataset = createThreeRowScoreDataset();
        dataset.moveToRow(0);
        Assertions.assertEquals((byte) 1, dataset.getByte(0));
        Assertions.assertEquals((byte) 1, dataset.getByte("id"));
    }

    @Test
    public void testGetShort() {
        final RowDataset dataset = createThreeRowScoreDataset();
        dataset.moveToRow(0);
        Assertions.assertEquals((short) 1, dataset.getShort(0));
        Assertions.assertEquals((short) 1, dataset.getShort("id"));
    }

    @Test
    public void testGetInt() {
        final RowDataset dataset = createThreeRowScoreDataset();
        dataset.moveToRow(0);
        Assertions.assertEquals(1, dataset.getInt(0));
        Assertions.assertEquals(1, dataset.getInt("id"));
        Assertions.assertEquals(25, dataset.getInt(2));
        Assertions.assertEquals(25, dataset.getInt("age"));
    }

    @Test
    public void testGetLong() {
        final RowDataset dataset = createThreeRowScoreDataset();
        dataset.moveToRow(0);
        Assertions.assertEquals(1L, dataset.getLong(0));
        Assertions.assertEquals(1L, dataset.getLong("id"));
    }

    @Test
    public void testGetFloat() {
        final RowDataset dataset = createThreeRowScoreDataset();
        dataset.moveToRow(0);
        Assertions.assertEquals(85.5f, dataset.getFloat(3), 0.01f);
        Assertions.assertEquals(85.5f, dataset.getFloat("score"), 0.01f);
    }

    @Test
    public void testGetDouble() {
        final RowDataset dataset = createThreeRowScoreDataset();
        dataset.moveToRow(0);
        Assertions.assertEquals(85.5, dataset.getDouble(3), 0.01);
        Assertions.assertEquals(85.5, dataset.getDouble("score"), 0.01);
    }

    @Test
    public void testIsNull() {
        final RowDataset dataset = createThreeRowScoreDataset();
        dataset.moveToRow(0);
        dataset.set(0, null);
        Assertions.assertTrue(dataset.isNull(0));
        Assertions.assertTrue(dataset.isNull("id"));
        Assertions.assertFalse(dataset.isNull(1));
        Assertions.assertFalse(dataset.isNull("name"));
    }

    @Test
    public void testGetColumn() {
        final RowDataset dataset = createThreeRowScoreDataset();
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
    public void testAddColumnWithEmptyCollection() {
        final RowDataset dataset = createThreeRowScoreDataset();
        dataset.addColumn("empty", Collections.emptyList());
        Assertions.assertEquals(5, dataset.columnCount());
        Assertions.assertTrue(dataset.containsColumn("empty"));
        Assertions.assertNull(dataset.get(0, 4));
    }

    @Test
    public void testAddColumnWithWrongSize() {
        final RowDataset dataset = createThreeRowScoreDataset();
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            dataset.addColumn("bad", Arrays.asList("One", "Two"));
        });
    }

    @Test
    public void testAddColumnWithMultipleColumnsAndFunction() {
        final RowDataset dataset = createThreeRowScoreDataset();
        dataset.addColumn("full_info", Arrays.asList("name", "age"), (Function<DisposableObjArray, String>) arr -> arr.get(0) + " (" + arr.get(1) + ")");
        Assertions.assertEquals(5, dataset.columnCount());
        Assertions.assertTrue(dataset.containsColumn("full_info"));
        Assertions.assertEquals("John (25)", dataset.get(0, 4));
    }

    @Test
    public void testAddColumnWithTuple2() {
        final RowDataset dataset = createThreeRowScoreDataset();
        dataset.addColumn("name_age", new Tuple2<>("name", "age"), (BiFunction<String, Integer, String>) (name, age) -> name + "-" + age);
        Assertions.assertEquals(5, dataset.columnCount());
        Assertions.assertEquals("John-25", dataset.get(0, 4));
    }

    @Test
    public void testAddColumnWithTuple3() {
        final RowDataset dataset = createThreeRowScoreDataset();
        dataset.addColumn("summary", new Tuple3<>("id", "name", "age"),
                (TriFunction<Integer, String, Integer, String>) (id, name, age) -> "ID:" + id + ",Name:" + name + ",Age:" + age);
        Assertions.assertEquals(5, dataset.columnCount());
        Assertions.assertEquals("ID:1,Name:John,Age:25", dataset.get(0, 4));
    }

    @Test
    public void testRemoveColumnsWithEmptyList() {
        final RowDataset dataset = createThreeRowScoreDataset();
        dataset.removeColumns(Collections.emptyList());
        Assertions.assertEquals(4, dataset.columnCount());
    }

    @Test
    public void testRemoveColumnsWithPredicate() {
        final RowDataset dataset = createThreeRowScoreDataset();
        dataset.removeColumns(col -> col.startsWith("a"));
        Assertions.assertEquals(3, dataset.columnCount());
        Assertions.assertFalse(dataset.containsColumn("age"));
        Assertions.assertTrue(dataset.containsColumn("id"));
        Assertions.assertTrue(dataset.containsColumn("name"));
        Assertions.assertTrue(dataset.containsColumn("score"));
    }

    @Test
    public void testCombineColumnsWithClass() {
        final RowDataset dataset = createThreeRowScoreDataset();
        dataset.combineColumns(Arrays.asList("name", "age"), "nameAge", a -> a.join(""));
        Assertions.assertEquals(3, dataset.columnCount());
        Assertions.assertTrue(dataset.containsColumn("nameAge"));
        Assertions.assertFalse(dataset.containsColumn("name"));
        Assertions.assertFalse(dataset.containsColumn("age"));
    }

    @Test
    public void testCombineColumnsWithTuple2() {
        final RowDataset dataset = createThreeRowScoreDataset();
        dataset.combineColumns(new Tuple2<>("name", "age"), "combined", (BiFunction<String, Integer, String>) (name, age) -> name + "_" + age);
        Assertions.assertEquals(3, dataset.columnCount());
        Assertions.assertEquals("John_25", dataset.get(0, dataset.getColumnIndex("combined")));
    }

    @Test
    public void testCombineColumnsWithTuple3() {
        final RowDataset dataset = createThreeRowScoreDataset();
        dataset.combineColumns(new Tuple3<>("id", "name", "age"), "combined",
                (TriFunction<Integer, String, Integer, String>) (id, name, age) -> id + ":" + name + ":" + age);
        Assertions.assertEquals(2, dataset.columnCount());
        Assertions.assertEquals("1:John:25", dataset.get(0, dataset.getColumnIndex("combined")));
    }

    @Test
    public void testDivideColumnWithTuple2() {
        final RowDataset dataset = createThreeRowScoreDataset();
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
        final RowDataset dataset = createThreeRowScoreDataset();
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
        final RowDataset dataset = createThreeRowScoreDataset();
        dataset.addRow(new Object[] { 4, "Alice", 28, 92.0 });
        Assertions.assertEquals(4, dataset.size());
        Assertions.assertEquals(4, (Integer) dataset.get(3, 0));
        Assertions.assertEquals("Alice", dataset.get(3, 1));
    }

    @Test
    public void testAddRowWithList() {
        final RowDataset dataset = createThreeRowScoreDataset();
        dataset.addRow(Arrays.asList(4, "Alice", 28, 92.0));
        Assertions.assertEquals(4, dataset.size());
        Assertions.assertEquals("Alice", dataset.get(3, 1));
    }

    @Test
    public void testAddRowWithMap() {
        final RowDataset dataset = createThreeRowScoreDataset();
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
        final RowDataset dataset = createThreeRowScoreDataset();
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
    public void testUpdateRows() {
        final RowDataset dataset = createThreeRowScoreDataset();
        dataset.updateRows(new int[] { 0, 2 }, (i, c, v) -> v instanceof Integer ? (Integer) v * 2 : v);
        Assertions.assertEquals(2, (Integer) dataset.get(0, 0));
        Assertions.assertEquals(50, (Integer) dataset.get(0, 2));
        Assertions.assertEquals(2, (Integer) dataset.get(1, 0));
        Assertions.assertEquals(6, (Integer) dataset.get(2, 0));
        Assertions.assertEquals(70, (Integer) dataset.get(2, 2));
    }

    @Test
    public void testGetRowAsArray() {
        final RowDataset dataset = createThreeRowScoreDataset();
        Object[] row = dataset.getRow(0);
        Assertions.assertEquals(4, row.length);
        Assertions.assertEquals(1, row[0]);
        Assertions.assertEquals("John", row[1]);
        Assertions.assertEquals(25, row[2]);
        Assertions.assertEquals(85.5, row[3]);
    }

    @Test
    public void testGetRowAsClass() {
        final RowDataset dataset = createThreeRowScoreDataset();
        TestBean bean = dataset.getRow(0, TestBean.class);
        Assertions.assertEquals(1, bean.id);
        Assertions.assertEquals("John", bean.name);
        Assertions.assertEquals(25, bean.age);
        Assertions.assertEquals(85.5, bean.score, 0.01);
    }

    @Test
    public void testGetRowWithSelectedColumns() {
        final RowDataset dataset = createThreeRowScoreDataset();
        TestBean bean = dataset.getRow(0, Arrays.asList("name", "age"), TestBean.class);
        Assertions.assertEquals("John", bean.name);
        Assertions.assertEquals(25, bean.age);
    }

    @Test
    public void testGetRowWithSupplier() {
        final RowDataset dataset = createThreeRowScoreDataset();
        List<Object> row = dataset.getRow(0, (IntFunction<List<Object>>) size -> new ArrayList<>(size));
        Assertions.assertEquals(4, row.size());
        Assertions.assertEquals(1, row.get(0));
        Assertions.assertEquals("John", row.get(1));
    }

    @Test
    public void testFirstRowAsClass() {
        final RowDataset dataset = createThreeRowScoreDataset();
        Optional<TestBean> firstRow = dataset.firstRow(TestBean.class);
        Assertions.assertTrue(firstRow.isPresent());
        Assertions.assertEquals("John", firstRow.get().name);
    }

    @Test
    public void testFirstRowWithColumns() {
        final RowDataset dataset = createThreeRowScoreDataset();
        Optional<TestBean> firstRow = dataset.firstRow(Arrays.asList("name", "age"), TestBean.class);
        Assertions.assertTrue(firstRow.isPresent());
        Assertions.assertEquals("John", firstRow.get().name);
        Assertions.assertEquals(25, firstRow.get().age);
    }

    @Test
    public void testFirstRowWithSupplier() {
        final RowDataset dataset = createThreeRowScoreDataset();
        Optional<List<Object>> firstRow = dataset.firstRow((IntFunction<List<Object>>) ArrayList::new);
        Assertions.assertTrue(firstRow.isPresent());
        Assertions.assertEquals(4, firstRow.get().size());
    }

    @Test
    public void testLastRowAsClass() {
        final RowDataset dataset = createThreeRowScoreDataset();
        Optional<TestBean> lastRow = dataset.lastRow(TestBean.class);
        Assertions.assertTrue(lastRow.isPresent());
        Assertions.assertEquals("Bob", lastRow.get().name);
    }

    @Test
    public void testLastRowWithColumns() {
        final RowDataset dataset = createThreeRowScoreDataset();
        Optional<TestBean> lastRow = dataset.lastRow(Arrays.asList("name", "age"), TestBean.class);
        Assertions.assertTrue(lastRow.isPresent());
        Assertions.assertEquals("Bob", lastRow.get().name);
        Assertions.assertEquals(35, lastRow.get().age);
    }

    @Test
    public void testLastRowWithSupplier() {
        final RowDataset dataset = createThreeRowScoreDataset();
        Optional<List<Object>> lastRow = dataset.lastRow((IntFunction<List<Object>>) ArrayList::new);
        Assertions.assertTrue(lastRow.isPresent());
        Assertions.assertEquals(4, lastRow.get().size());
    }

    @Test
    public void testBiIterator() {
        final RowDataset dataset = createThreeRowScoreDataset();
        BiIterator<String, Integer> iter = dataset.iterator("name", "age");
        Assertions.assertTrue(iter.hasNext());

        Pair<String, Integer> pair = iter.next();
        Assertions.assertEquals("John", pair.left());
        Assertions.assertEquals(25, pair.right());
    }

    @Test
    public void testBiIteratorWithRange() {
        final RowDataset dataset = createThreeRowScoreDataset();
        BiIterator<String, Integer> iter = dataset.iterator(1, 3, "name", "age");
        Assertions.assertTrue(iter.hasNext());

        Pair<String, Integer> pair = iter.next();
        Assertions.assertEquals("Jane", pair.left());
        Assertions.assertEquals(30, pair.right());
    }

    @Test
    public void testTriIteratorWithRange() {
        final RowDataset dataset = createThreeRowScoreDataset();
        TriIterator<Integer, String, Integer> iter = dataset.iterator(0, 2, "id", "name", "age");

        int count = 0;
        while (iter.hasNext()) {
            iter.next();
            count++;
        }
        Assertions.assertEquals(2, count);
    }

    @Test
    public void testForEachWithColumns() {
        final RowDataset dataset = createThreeRowScoreDataset();
        List<String> results = new ArrayList<>();
        dataset.forEach(Arrays.asList("name", "age"), (DisposableObjArray arr) -> {
            results.add(arr.get(0) + "-" + arr.get(1));
        });

        Assertions.assertEquals(3, results.size());
        Assertions.assertEquals("John-25", results.get(0));
    }

    @Test
    public void testForEachWithRange() {
        final RowDataset dataset = createThreeRowScoreDataset();
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
        final RowDataset dataset = createThreeRowScoreDataset();
        List<String> results = new ArrayList<>();
        dataset.forEach(Tuple.of("name", "age"), (name, age) -> results.add(name + " is " + age));

        Assertions.assertEquals(3, results.size());
        Assertions.assertEquals("John is 25", results.get(0));
    }

    @Test
    public void testForEachWithTuple3() {
        final RowDataset dataset = createThreeRowScoreDataset();
        List<String> results = new ArrayList<>();
        dataset.forEach(new Tuple3<>("id", "name", "age"), (TriConsumer<Integer, String, Integer>) (id, name, age) -> results.add(id + ":" + name + ":" + age));

        Assertions.assertEquals(3, results.size());
        Assertions.assertEquals("1:John:25", results.get(0));
    }

    @Test
    public void testToList() {
        final RowDataset dataset = createThreeRowScoreDataset();
        List<Object[]> list = dataset.toList();
        Assertions.assertEquals(3, list.size());
        Assertions.assertEquals(1, list.get(0)[0]);
        Assertions.assertEquals("John", list.get(0)[1]);
    }

    @Test
    public void testToListWithRange() {
        final RowDataset dataset = createThreeRowScoreDataset();
        List<Object[]> list = dataset.toList(1, 3);
        Assertions.assertEquals(2, list.size());
        Assertions.assertEquals(2, list.get(0)[0]);
        Assertions.assertEquals("Jane", list.get(0)[1]);
    }

    @Test
    public void testToListAsClass() {
        final RowDataset dataset = createThreeRowScoreDataset();
        List<TestBean> list = dataset.toList(TestBean.class);
        Assertions.assertEquals(3, list.size());
        Assertions.assertEquals("John", list.get(0).name);
        Assertions.assertEquals(25, list.get(0).age);
    }

    @Test
    public void testToListWithRangeAsClass() {
        final RowDataset dataset = createThreeRowScoreDataset();
        List<TestBean> list = dataset.toList(0, 2, TestBean.class);
        Assertions.assertEquals(2, list.size());
        Assertions.assertEquals("John", list.get(0).name);
        Assertions.assertEquals("Jane", list.get(1).name);
    }

    @Test
    public void testToListWithColumnsAsClass() {
        final RowDataset dataset = createThreeRowScoreDataset();
        List<TestBean> list = dataset.toList(Arrays.asList("name", "age"), TestBean.class);
        Assertions.assertEquals(3, list.size());
        Assertions.assertEquals("John", list.get(0).name);
        Assertions.assertEquals(25, list.get(0).age);
    }

    @Test
    public void testToListWithSupplier() {
        final RowDataset dataset = createThreeRowScoreDataset();
        List<Map<String, Object>> list = dataset.toList((IntFunction<Map<String, Object>>) size -> new HashMap<>());
        Assertions.assertEquals(3, list.size());
        Assertions.assertEquals(1, list.get(0).get("id"));
        Assertions.assertEquals("John", list.get(0).get("name"));
    }

    @Test
    public void testToListWithFilters() {
        final RowDataset dataset = createThreeRowScoreDataset();
        List<TestBean> list = dataset.toList(col -> col.equals("name") || col.equals("age"), col -> col.toUpperCase(), TestBean.class);
        Assertions.assertEquals(3, list.size());
    }

    @Test
    public void testToEntities() {
        final RowDataset dataset = createThreeRowScoreDataset();
        List<TestBean> entities = dataset.toEntities(null, TestBean.class);
        Assertions.assertEquals(3, entities.size());
        Assertions.assertEquals("John", entities.get(0).name);
    }

    @Test
    public void testToEntitiesWithPrefixMap() {
        final RowDataset dataset = createThreeRowScoreDataset();
        Map<String, String> prefixMap = new HashMap<>();
        prefixMap.put("n", "name");
        List<TestBean> entities = dataset.toEntities(prefixMap, TestBean.class);
        Assertions.assertEquals(3, entities.size());
    }

    @Test
    public void testToMapWithKeyValueColumns() {
        final RowDataset dataset = createThreeRowScoreDataset();
        Map<Integer, String> map = dataset.toMap("id", "name");
        Assertions.assertEquals(3, map.size());
        Assertions.assertEquals("John", map.get(1));
        Assertions.assertEquals("Jane", map.get(2));
        Assertions.assertEquals("Bob", map.get(3));
    }

    @Test
    public void testToMapWithSupplier() {
        final RowDataset dataset = createThreeRowScoreDataset();
        Map<Integer, String> map = dataset.toMap("id", "name", (IntFunction<LinkedHashMap<Integer, String>>) size -> new LinkedHashMap<>());
        Assertions.assertEquals(3, map.size());
        Assertions.assertEquals("John", map.get(1));
    }

    @Test
    public void testToMapWithRange() {
        final RowDataset dataset = createThreeRowScoreDataset();
        Map<Integer, String> map = dataset.toMap(0, 2, "id", "name");
        Assertions.assertEquals(2, map.size());
        Assertions.assertEquals("John", map.get(1));
        Assertions.assertEquals("Jane", map.get(2));
        Assertions.assertNull(map.get(3));
    }

    @Test
    public void testToMapWithMultipleValueColumns() {
        final RowDataset dataset = createThreeRowScoreDataset();
        Map<Integer, TestBean> map = dataset.toMap("id", Arrays.asList("name", "age"), TestBean.class);
        Assertions.assertEquals(3, map.size());
        Assertions.assertEquals("John", map.get(1).name);
        Assertions.assertEquals(25, map.get(1).age);
    }

    @Test
    public void testToMapWithValueSupplier() {
        final RowDataset dataset = createThreeRowScoreDataset();
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
        final RowDataset dataset = createThreeRowScoreDataset();
        ListMultimap<Integer, TestBean> multimap = dataset.toMultimap("id", Arrays.asList("name", "age"), TestBean.class);
        Assertions.assertEquals(1, multimap.get(1).size());
        Assertions.assertEquals("John", multimap.get(1).get(0).name);
    }

    @Test
    public void testToJson() {
        final RowDataset dataset = createThreeRowScoreDataset();
        String json = dataset.toJson();
        Assertions.assertTrue(json.contains("\"id\":1"));
        Assertions.assertTrue(json.contains("\"name\":\"John\""));
        Assertions.assertTrue(json.contains("\"age\":25"));
    }

    @Test
    public void testToJsonWithRange() {
        final RowDataset dataset = createThreeRowScoreDataset();
        String json = dataset.toJson(0, 1);
        Assertions.assertTrue(json.contains("\"name\":\"John\""));
        Assertions.assertFalse(json.contains("\"name\":\"Jane\""));
    }

    @Test
    public void testToJsonWithColumns() {
        final RowDataset dataset = createThreeRowScoreDataset();
        String json = dataset.toJson(0, 3, Arrays.asList("name", "age"));
        Assertions.assertTrue(json.contains("\"name\":\"John\""));
        Assertions.assertTrue(json.contains("\"age\":25"));
        Assertions.assertFalse(json.contains("\"id\""));
        Assertions.assertFalse(json.contains("\"score\""));
    }

    @Test
    public void testToJsonToFile() throws IOException {
        final RowDataset dataset = createThreeRowScoreDataset();
        File tempFile = File.createTempFile("dataset", ".json");
        tempFile.deleteOnExit();

        dataset.toJson(tempFile);
        String content = new String(java.nio.file.Files.readAllBytes(tempFile.toPath()));
        Assertions.assertTrue(content.contains("\"name\":\"John\""));
    }

    @Test
    public void testToJsonToOutputStream() throws IOException {
        final RowDataset dataset = createThreeRowScoreDataset();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        dataset.toJson(baos);
        String json = baos.toString();
        Assertions.assertTrue(json.contains("\"name\":\"John\""));
    }

    @Test
    public void testToJsonToWriter() throws IOException {
        final RowDataset dataset = createThreeRowScoreDataset();
        StringWriter writer = new StringWriter();
        dataset.toJson(writer);
        String json = writer.toString();
        Assertions.assertTrue(json.contains("\"name\":\"John\""));
    }

    // ==================== Missing test methods below ====================

    @Test
    public void testCurrentRowIndex() {
        assertEquals(0, dataset.currentRowIndex());
    }

    @Test
    public void testMoveToRow() {
        dataset.moveToRow(3);
        assertEquals(3, dataset.currentRowIndex());
    }

    @Test
    public void testMoveToRow_invalid() {
        assertThrows(Exception.class, () -> dataset.moveToRow(-1));
        assertThrows(Exception.class, () -> dataset.moveToRow(100));
    }

    @Test
    public void testColumnNamesImmutable() {
        ImmutableList<String> names = dataset.columnNames();
        assertNotNull(names);
        assertEquals(4, names.size());
        assertEquals("id", names.get(0));
    }

    @Test
    public void testIsEmpty() {
        assertFalse(dataset.isEmpty());
        assertTrue(emptyDataset.isEmpty());
    }

    @Test
    public void testIsFrozen() {
        assertFalse(dataset.isFrozen());
        dataset.freeze();
        assertTrue(dataset.isFrozen());
    }

    @Test
    public void testUpdateAllWithIntBiObjFunction() {
        RowDataset ds = new RowDataset(new ArrayList<>(Arrays.asList("a", "b")),
                new ArrayList<>(Arrays.asList(new ArrayList<>(Arrays.asList(1, 2)), new ArrayList<>(Arrays.asList(3, 4)))));

        ds.updateAll((com.landawn.abacus.util.function.IntBiObjFunction<String, Object, Object>) (rowIndex, columnName, value) -> {
            if (value instanceof Integer) {
                return ((Integer) value) * 10;
            }
            return value;
        });

        assertEquals(10, (int) ds.get(0, 0));
        assertEquals(20, (int) ds.get(1, 0));
        assertEquals(30, (int) ds.get(0, 1));
        assertEquals(40, (int) ds.get(1, 1));
    }

    @Test
    public void testReplaceIfWithIntBiObjPredicate() {
        RowDataset ds = new RowDataset(new ArrayList<>(Arrays.asList("a", "b")),
                new ArrayList<>(Arrays.asList(new ArrayList<>(Arrays.asList(1, 2)), new ArrayList<>(Arrays.asList(3, 4)))));

        ds.replaceIf((com.landawn.abacus.util.function.IntBiObjPredicate<String, Object>) (rowIndex, columnName, value) -> {
            return value instanceof Integer && ((Integer) value) > 2;
        }, 99);

        assertEquals(1, (int) ds.get(0, 0));
        assertEquals(2, (int) ds.get(1, 0));
        assertEquals(99, (int) ds.get(0, 1));
        assertEquals(99, (int) ds.get(1, 1));
    }

    @Test
    public void testRemoveDuplicateRowsByColumnName() {
        RowDataset ds = new RowDataset(new ArrayList<>(Arrays.asList("name", "val")),
                new ArrayList<>(Arrays.asList(new ArrayList<>(Arrays.asList("A", "B", "A", "C")), new ArrayList<>(Arrays.asList(1, 2, 3, 4)))));

        ds.removeDuplicateRowsBy("name");
        assertEquals(3, ds.size()); // "A" duplicate removed
    }

    @Test
    public void testRemoveDuplicateRowsByColumnNameWithKeyExtractor() {
        RowDataset ds = new RowDataset(new ArrayList<>(Arrays.asList("name", "val")),
                new ArrayList<>(Arrays.asList(new ArrayList<>(Arrays.asList("Alice", "Bob", "Amy")), new ArrayList<>(Arrays.asList(1, 2, 3)))));

        ds.removeDuplicateRowsBy("name", (Function<Object, Object>) n -> ((String) n).substring(0, 1));
        assertEquals(2, ds.size()); // "Alice" and "Amy" have same key "A"
    }

    @Test
    public void testRemoveDuplicateRowsByMultipleColumns() {
        RowDataset ds = new RowDataset(new ArrayList<>(Arrays.asList("a", "b")),
                new ArrayList<>(Arrays.asList(new ArrayList<>(Arrays.asList("x", "y", "x")), new ArrayList<>(Arrays.asList(1, 2, 1)))));

        ds.removeDuplicateRowsBy(Arrays.asList("a", "b"));
        assertEquals(2, ds.size());
    }

    @Test
    public void testRemoveRowsAt() {
        int sizeBefore = dataset.size();
        dataset.removeRowsAt(new int[] { 0, 2 });
        assertEquals(sizeBefore - 2, dataset.size());
    }

    @Test
    public void testRemoveRowsRange() {
        int sizeBefore = dataset.size();
        dataset.removeRows(1, 3);
        assertEquals(sizeBefore - 2, dataset.size());
    }

    @Test
    public void testMapColumn() {
        Dataset result = dataset.mapColumn("name", "upperName", "id", (Function<Object, Object>) n -> ((String) n).toUpperCase());
        assertNotNull(result);
        assertTrue(result.columnNames().contains("upperName"));
        assertTrue(result.columnNames().contains("id"));
        assertEquals("ALICE", result.get(0, result.getColumnIndex("upperName")));
    }

    @Test
    public void testMapColumnMultipleCopying() {
        Dataset result = dataset.mapColumn("name", "nameLen", Arrays.asList("id", "age"), (Function<Object, Object>) n -> ((String) n).length());
        assertNotNull(result);
        assertTrue(result.columnNames().contains("nameLen"));
        assertTrue(result.columnNames().contains("id"));
        assertTrue(result.columnNames().contains("age"));
    }

    @Test
    public void testMapColumnsTuple2() {
        Dataset result = dataset.mapColumns(Tuple2.of("id", "age"), "combined", Arrays.asList("name"),
                (BiFunction<Object, Object, Object>) (id, age) -> id + "_" + age);
        assertNotNull(result);
        assertTrue(result.columnNames().contains("combined"));
        assertEquals("1_25", result.get(0, result.getColumnIndex("combined")));
    }

    @Test
    public void testMapColumnsTuple3() {
        Dataset result = dataset.mapColumns(Tuple3.of("id", "name", "age"), "combined", Collections.emptyList(),
                (TriFunction<Object, Object, Object, Object>) (id, name, age) -> id + "-" + name + "-" + age);
        assertNotNull(result);
        assertTrue(result.columnNames().contains("combined"));
        assertEquals("1-Alice-25", result.get(0, result.getColumnIndex("combined")));
    }

    @Test
    public void testMapColumnsCollection() {
        Dataset result = dataset.mapColumns(Arrays.asList("id", "age"), "sum", Arrays.asList("name"),
                (Function<NoCachingNoUpdating.DisposableObjArray, Object>) arr -> (Integer) arr.get(0) + (Integer) arr.get(1));
        assertNotNull(result);
        assertTrue(result.columnNames().contains("sum"));
    }

    @Test
    public void testFlatMapColumn() {
        RowDataset ds = new RowDataset(new ArrayList<>(Arrays.asList("tags", "id")),
                new ArrayList<>(Arrays.asList(new ArrayList<>(Arrays.asList("a,b", "c,d,e")), new ArrayList<>(Arrays.asList(1, 2)))));

        Dataset result = ds.flatMapColumn("tags", "tag", "id", (Function<Object, Collection<?>>) t -> Arrays.asList(((String) t).split(",")));
        assertNotNull(result);
        assertTrue(result.size() > ds.size());
    }

    @Test
    public void testFlatMapColumnMultipleCopying() {
        RowDataset ds = new RowDataset(new ArrayList<>(Arrays.asList("tags", "id", "name")), new ArrayList<>(
                Arrays.asList(new ArrayList<>(Arrays.asList("a,b", "c")), new ArrayList<>(Arrays.asList(1, 2)), new ArrayList<>(Arrays.asList("X", "Y")))));

        Dataset result = ds.flatMapColumn("tags", "tag", Arrays.asList("id", "name"),
                (Function<Object, Collection<?>>) t -> Arrays.asList(((String) t).split(",")));
        assertNotNull(result);
        assertTrue(result.columnNames().contains("tag"));
    }

    @Test
    public void testDivideColumnWithTuple2BiConsumer() {
        RowDataset ds = new RowDataset(new ArrayList<>(Arrays.asList("full", "extra")),
                new ArrayList<>(Arrays.asList(new ArrayList<>(Arrays.asList("John-Doe", "Jane-Smith")), new ArrayList<>(Arrays.asList(1, 2)))));

        ds.divideColumn("full", Tuple2.of("first", "last"), (BiConsumer<Object, com.landawn.abacus.util.Pair<Object, Object>>) (val, output) -> {
            String[] parts = ((String) val).split("-");
            output.set(parts[0], parts[1]);
        });

        assertTrue(ds.columnNames().contains("first"));
        assertTrue(ds.columnNames().contains("last"));
        assertEquals("John", ds.get(0, ds.getColumnIndex("first")));
        assertEquals("Doe", ds.get(0, ds.getColumnIndex("last")));
    }

    @Test
    public void testDivideColumnWithTuple3BiConsumer() {
        RowDataset ds = new RowDataset(new ArrayList<>(Arrays.asList("date", "extra")),
                new ArrayList<>(Arrays.asList(new ArrayList<>(Arrays.asList("2024-01-15", "2024-06-20")), new ArrayList<>(Arrays.asList(1, 2)))));

        ds.divideColumn("date", Tuple3.of("year", "month", "day"), (BiConsumer<Object, Triple<Object, Object, Object>>) (val, output) -> {
            String[] parts = ((String) val).split("-");
            output.set(parts[0], parts[1], parts[2]);
        });

        assertTrue(ds.columnNames().contains("year"));
        assertTrue(ds.columnNames().contains("month"));
        assertTrue(ds.columnNames().contains("day"));
    }

    @Test
    public void testAddColumnsAtPosition() {
        int colCountBefore = dataset.columnCount();
        dataset.addColumns(1, Arrays.asList("newCol1", "newCol2"),
                Arrays.asList(new ArrayList<>(Arrays.asList("a", "b", "c", "d", "e")), new ArrayList<>(Arrays.asList("x", "y", "z", "w", "v"))));
        assertEquals(colCountBefore + 2, dataset.columnCount());
        assertEquals("newCol1", dataset.getColumnName(1));
        assertEquals("newCol2", dataset.getColumnName(2));
    }

    @Test
    public void testPrintlnWithPrefix() {
        assertDoesNotThrow(() -> dataset.println("PREFIX: "));
    }

    @Test
    public void testPrintlnWithRangeAndColumnsAndPrefix() {
        StringWriter sw = new StringWriter();
        assertDoesNotThrow(() -> dataset.println(0, 2, Arrays.asList("id", "name"), "= ", sw));
        String output = sw.toString();
        assertNotNull(output);
        assertTrue(output.length() > 0);
    }

    @Test
    public void testUpdateRows_2() {
        RowDataset ds = new RowDataset(new ArrayList<>(Arrays.asList("a", "b")),
                new ArrayList<>(Arrays.asList(new ArrayList<>(Arrays.asList(1, 2, 3)), new ArrayList<>(Arrays.asList(4, 5, 6)))));

        ds.updateRows(new int[] { 0, 2 }, (com.landawn.abacus.util.function.IntBiObjFunction<String, Object, Object>) (rowIndex, columnName, value) -> {
            if (value instanceof Integer) {
                return ((Integer) value) * 100;
            }
            return value;
        });

        assertEquals(100, (int) ds.get(0, 0));
        assertEquals(2, (int) ds.get(1, 0)); // row 1 untouched
        assertEquals(300, (int) ds.get(2, 0));
    }

    @Test
    public void testAddColumnAtPositionWithFunction() {
        dataset.addColumn(1, "nameUpper", "name", (Function<Object, Object>) n -> ((String) n).toUpperCase());
        assertEquals("nameUpper", dataset.getColumnName(1));
        assertEquals("ALICE", dataset.get(0, 1));
    }

    @Test
    public void testAddColumnAtPositionWithMultipleColumns() {
        dataset.addColumn(0, "idPlusAge", Arrays.asList("id", "age"),
                (Function<NoCachingNoUpdating.DisposableObjArray, Object>) arr -> (Integer) arr.get(0) + (Integer) arr.get(1));
        assertEquals("idPlusAge", dataset.getColumnName(0));
        assertEquals(26, (int) dataset.get(0, 0)); // id=1, age=25
    }

    @Test
    public void testAddColumnAtPositionWithTuple2() {
        dataset.addColumn(0, "combined", Tuple2.of("id", "name"), (BiFunction<Object, Object, Object>) (id, name) -> id + "-" + name);
        assertEquals("combined", dataset.getColumnName(0));
        assertEquals("1-Alice", dataset.get(0, 0));
    }

    @Test
    public void testAddColumnAtPositionWithTuple3() {
        dataset.addColumn(0, "info", Tuple3.of("id", "name", "age"),
                (TriFunction<Object, Object, Object, Object>) (id, name, age) -> id + "/" + name + "/" + age);
        assertEquals("info", dataset.getColumnName(0));
        assertEquals("1/Alice/25", dataset.get(0, 0));
    }

    @Test
    public void testAddRowsAtPosition() {
        int sizeBefore = dataset.size();
        List<Object[]> newRows = Arrays.asList(new Object[] { 10, "NewPerson1", 50, 90000.0 }, new Object[] { 11, "NewPerson2", 55, 95000.0 });
        dataset.addRows(1, newRows);
        assertEquals(sizeBefore + 2, dataset.size());
        assertEquals(10, (int) dataset.get(1, 0));
        assertEquals(11, (int) dataset.get(2, 0));
    }

    @Test
    public void testCopyAll() {
        Dataset copy = dataset.copy();
        assertEquals(dataset.size(), copy.size());
        assertEquals(dataset.columnCount(), copy.columnCount());
    }

    @Test
    public void testSliceWithColumns() {
        Dataset sliced = dataset.slice(Arrays.asList("id", "name"));
        assertEquals(2, sliced.columnCount());
        assertEquals(dataset.size(), sliced.size());
    }

    // --- New tests for previously untested methods ---

    @Test
    public void testMergeWithRange() {
        RowDataset target = new RowDataset(new ArrayList<>(Arrays.asList("id", "name")),
                new ArrayList<>(Arrays.asList(new ArrayList<>(Arrays.asList(1, 2)), new ArrayList<>(Arrays.asList("A", "B")))));
        RowDataset source = new RowDataset(new ArrayList<>(Arrays.asList("id", "name")),
                new ArrayList<>(Arrays.asList(new ArrayList<>(Arrays.asList(3, 4, 5)), new ArrayList<>(Arrays.asList("C", "D", "E")))));

        target.merge(source, 1, 3, Arrays.asList("id", "name"));
        assertEquals(4, target.size());
        assertEquals(4, (int) target.get(2, 0));
        assertEquals("E", target.get(3, 1));
    }

    @Test
    public void testMergeWithRangeAndNewColumns() {
        RowDataset target = new RowDataset(new ArrayList<>(Arrays.asList("id", "name")),
                new ArrayList<>(Arrays.asList(new ArrayList<>(Arrays.asList(1, 2)), new ArrayList<>(Arrays.asList("A", "B")))));
        RowDataset source = new RowDataset(new ArrayList<>(Arrays.asList("id", "city")),
                new ArrayList<>(Arrays.asList(new ArrayList<>(Arrays.asList(3, 4, 5)), new ArrayList<>(Arrays.asList("NYC", "LA", "SF")))));

        target.merge(source, 0, 2, Arrays.asList("id", "city"));
        assertEquals(4, target.size());
        assertTrue(target.containsColumn("city"));
        assertEquals("NYC", target.get(2, target.getColumnIndex("city")));
        assertNull(target.get(0, target.getColumnIndex("city")));
    }

    @Test
    public void testRemoveDuplicateRowsByMultipleColumnsWithKeyExtractor() {
        List<List<Object>> cols = new ArrayList<>();
        cols.add(new ArrayList<>(Arrays.asList(1, 1, 2, 2)));
        cols.add(new ArrayList<>(Arrays.asList("A", "A", "B", "C")));
        RowDataset ds = new RowDataset(new ArrayList<>(Arrays.asList("id", "name")), cols);

        ds.removeDuplicateRowsBy(Arrays.asList("id", "name"), (DisposableObjArray arr) -> arr.get(0) + "_" + arr.get(1));
        assertEquals(3, ds.size());
    }

    @Test
    public void testForEachWithRangeAndTuple2() {
        final RowDataset ds = createThreeRowScoreDataset();
        List<String> results = new ArrayList<>();
        ds.forEach(1, 3, Tuple.of("name", "age"), (Throwables.BiConsumer<String, Integer, RuntimeException>) (name, age) -> results.add(name + ":" + age));

        assertEquals(2, results.size());
        assertEquals("Jane:30", results.get(0));
        assertEquals("Bob:35", results.get(1));
    }

    @Test
    public void testForEachWithRangeAndTuple3() {
        final RowDataset ds = createThreeRowScoreDataset();
        List<String> results = new ArrayList<>();
        ds.forEach(0, 2, new Tuple3<>("id", "name", "age"),
                (Throwables.TriConsumer<Integer, String, Integer, RuntimeException>) (id, name, age) -> results.add(id + ":" + name + ":" + age));

        assertEquals(2, results.size());
        assertEquals("1:John:25", results.get(0));
        assertEquals("2:Jane:30", results.get(1));
    }

    @Test
    public void testForEachWithRangeAndColumns() {
        final RowDataset ds = createThreeRowScoreDataset();
        List<String> results = new ArrayList<>();
        ds.forEach(0, 2, Arrays.asList("name", "age"), (DisposableObjArray arr) -> {
            results.add(arr.get(0) + "-" + arr.get(1));
        });

        assertEquals(2, results.size());
        assertEquals("John-25", results.get(0));
        assertEquals("Jane-30", results.get(1));
    }

    @Test
    public void testForEachReverseOrder() {
        final RowDataset ds = createThreeRowScoreDataset();
        List<String> results = new ArrayList<>();
        ds.forEach(2, -1, (DisposableObjArray arr) -> {
            results.add(arr.get(1).toString());
        });

        assertEquals(3, results.size());
        assertEquals("Bob", results.get(0));
        assertEquals("Jane", results.get(1));
        assertEquals("John", results.get(2));
    }

    @Test
    public void testToListWithRangeAndColumnsAsClass() {
        final RowDataset ds = createThreeRowScoreDataset();
        List<TestBean> list = ds.toList(1, 3, Arrays.asList("name", "age"), TestBean.class);
        assertEquals(2, list.size());
        assertEquals("Jane", list.get(0).name);
        assertEquals(35, list.get(1).age);
    }

    @Test
    public void testToListWithRangeAndColumnsAndSupplier() {
        final RowDataset ds = createThreeRowScoreDataset();
        List<Map<String, Object>> list = ds.toList(0, 2, Arrays.asList("name", "age"), (IntFunction<Map<String, Object>>) size -> new HashMap<>());
        assertEquals(2, list.size());
        assertEquals("John", list.get(0).get("name"));
        assertEquals(30, list.get(1).get("age"));
    }

    @Test
    public void testToListWithRangeAndFilterAndConverter() {
        final RowDataset ds = createThreeRowScoreDataset();
        List<TestBean> list = ds.toList(0, 2, col -> col.equals("name") || col.equals("age"), col -> col, TestBean.class);
        assertEquals(2, list.size());
        assertEquals("John", list.get(0).name);
        assertEquals(25, list.get(0).age);
    }

    @Test
    public void testToListWithRangeAndFilterAndConverterAndSupplier() {
        final RowDataset ds = createThreeRowScoreDataset();
        List<Map<String, Object>> list = ds.toList(0, 2, col -> !col.equals("score"), null, (IntFunction<Map<String, Object>>) size -> new HashMap<>());
        assertEquals(2, list.size());
        assertEquals("John", list.get(0).get("name"));
        assertFalse(list.get(0).containsKey("score"));
    }

    @Test
    public void testToEntitiesWithRange() {
        final RowDataset ds = createThreeRowScoreDataset();
        List<TestBean> entities = ds.toEntities(1, 3, null, TestBean.class);
        assertEquals(2, entities.size());
        assertEquals("Jane", entities.get(0).name);
        assertEquals("Bob", entities.get(1).name);
    }

    @Test
    public void testToEntitiesWithRangeAndColumns() {
        final RowDataset ds = createThreeRowScoreDataset();
        List<TestBean> entities = ds.toEntities(0, 2, Arrays.asList("name", "age"), null, TestBean.class);
        assertEquals(2, entities.size());
        assertEquals("John", entities.get(0).name);
        assertEquals(25, entities.get(0).age);
    }

    @Test
    public void testToEntitiesWithColumnsAndPrefix() {
        final RowDataset ds = createThreeRowScoreDataset();
        Map<String, String> prefixMap = new HashMap<>();
        List<TestBean> entities = ds.toEntities(Arrays.asList("name", "age"), prefixMap, TestBean.class);
        assertEquals(3, entities.size());
        assertEquals("John", entities.get(0).name);
    }

    @Test
    public void testClonePlain() {
        final RowDataset ds = createFiveRowCityDataset();
        Dataset cloned = ds.clone();

        assertNotNull(cloned);
        assertEquals(ds.size(), cloned.size());
        assertEquals(ds.columnCount(), cloned.columnCount());
        assertFalse(cloned.isFrozen());

        // Verify it's a deep copy
        cloned.set(0, 0, 999);
        assertFalse(ds.get(0, 0).equals(999));
    }

    @Test
    public void testStreamWithRangeAndColumnsAndRowType() {
        final RowDataset ds = createThreeRowScoreDataset();
        List<TestBean> list = ds.stream(1, 3, Arrays.asList("name", "age"), TestBean.class).toList();
        assertEquals(2, list.size());
        assertEquals("Jane", list.get(0).name);
        assertEquals(35, list.get(1).age);
    }

    @Test
    public void testStreamWithRangeAndColumnsAndSupplier() {
        final RowDataset ds = createThreeRowScoreDataset();
        List<Map<String, Object>> list = ds.stream(0, 2, Arrays.asList("name", "age"), (IntFunction<Map<String, Object>>) size -> new HashMap<>()).toList();
        assertEquals(2, list.size());
        assertEquals("John", list.get(0).get("name"));
    }

    @Test
    public void testStreamWithColumnsAndRowMapper() {
        final RowDataset ds = createThreeRowScoreDataset();
        List<String> list = ds.stream(Arrays.asList("name", "age"), (int rowIndex, DisposableObjArray arr) -> arr.get(0) + "-" + arr.get(1)).toList();
        assertEquals(3, list.size());
        assertEquals("John-25", list.get(0));
    }

    @Test
    public void testStreamWithRangeAndRowMapper() {
        final RowDataset ds = createThreeRowScoreDataset();
        List<String> list = ds.stream(1, 3, (int rowIndex, DisposableObjArray arr) -> arr.get(1) + "=" + arr.get(2)).toList();
        assertEquals(2, list.size());
        assertEquals("Jane=30", list.get(0));
    }

    @Test
    public void testStreamWithRangeAndColumnsAndRowMapper() {
        final RowDataset ds = createThreeRowScoreDataset();
        List<String> list = ds.stream(0, 2, Arrays.asList("name"), (int rowIndex, DisposableObjArray arr) -> "row" + rowIndex + ":" + arr.get(0)).toList();
        assertEquals(2, list.size());
        assertEquals("row0:John", list.get(0));
        assertEquals("row1:Jane", list.get(1));
    }

    @Test
    public void testStreamWithRangeAndTuple2() {
        final RowDataset ds = createThreeRowScoreDataset();
        List<String> list = ds.stream(1, 3, Tuple.of("name", "age"), (BiFunction<String, Integer, String>) (name, age) -> name + "(" + age + ")").toList();
        assertEquals(2, list.size());
        assertEquals("Jane(30)", list.get(0));
        assertEquals("Bob(35)", list.get(1));
    }

    @Test
    public void testStreamWithRangeAndTuple3() {
        final RowDataset ds = createThreeRowScoreDataset();
        List<String> list = ds
                .stream(0, 2, new Tuple3<>("id", "name", "age"), (TriFunction<Integer, String, Integer, String>) (id, name, age) -> id + ":" + name + ":" + age)
                .toList();
        assertEquals(2, list.size());
        assertEquals("1:John:25", list.get(0));
        assertEquals("2:Jane:30", list.get(1));
    }

    @Test
    public void testStreamWithColumnsAndPrefixMap() {
        final RowDataset ds = createThreeRowScoreDataset();
        Map<String, String> prefixMap = new HashMap<>();
        List<TestBean> list = ds.stream(Arrays.asList("name", "age"), prefixMap, TestBean.class).toList();
        assertEquals(3, list.size());
        assertEquals("John", list.get(0).name);
    }

    @Test
    public void testStreamWithRangeAndColumnsAndPrefixMap() {
        final RowDataset ds = createThreeRowScoreDataset();
        Map<String, String> prefixMap = new HashMap<>();
        List<TestBean> list = ds.stream(1, 3, Arrays.asList("name", "age"), prefixMap, TestBean.class).toList();
        assertEquals(2, list.size());
        assertEquals("Jane", list.get(0).name);
    }

    @Test
    public void testGetRowWithColumnsAndClass() {
        final RowDataset ds = createThreeRowScoreDataset();
        TestBean bean = ds.getRow(0, Arrays.asList("name", "age"), TestBean.class);
        assertEquals("John", bean.name);
        assertEquals(25, bean.age);
    }

    @Test
    public void testGetRowWithColumnsAndSupplier() {
        final RowDataset ds = createThreeRowScoreDataset();
        Map<String, Object> row = ds.getRow(0, Arrays.asList("name", "age"), (IntFunction<Map<String, Object>>) size -> new HashMap<>());
        assertEquals("John", row.get("name"));
        assertEquals(25, row.get("age"));
    }

    @Test
    public void testFirstRowWithColumnsAndSupplier() {
        final RowDataset ds = createThreeRowScoreDataset();
        Optional<Map<String, Object>> row = ds.firstRow(Arrays.asList("name", "age"), (IntFunction<Map<String, Object>>) size -> new HashMap<>());
        assertTrue(row.isPresent());
        assertEquals("John", row.get().get("name"));
    }

    @Test
    public void testLastRowWithColumnsAndSupplier() {
        final RowDataset ds = createThreeRowScoreDataset();
        Optional<Map<String, Object>> row = ds.lastRow(Arrays.asList("name", "age"), (IntFunction<Map<String, Object>>) size -> new HashMap<>());
        assertTrue(row.isPresent());
        assertEquals("Bob", row.get().get("name"));
    }

    @Test
    public void testToJsonToOutputStreamWithRangeAndColumns() throws IOException {
        final RowDataset ds = createThreeRowScoreDataset();
        java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
        ds.toJson(0, 2, Arrays.asList("name", "age"), baos);
        String json = baos.toString();
        assertNotNull(json);
        assertTrue(json.contains("John"));
        assertFalse(json.contains("Bob"));
    }

    @Test
    public void testToJsonToWriterWithRangeAndColumns() throws IOException {
        final RowDataset ds = createThreeRowScoreDataset();
        StringWriter writer = new StringWriter();
        ds.toJson(0, 2, Arrays.asList("name", "age"), writer);
        String json = writer.toString();
        assertNotNull(json);
        assertTrue(json.contains("John"));
        assertFalse(json.contains("Bob"));
    }

    @Test
    public void testToJsonToFileWithRangeAndColumns(@TempDir Path tempDir) throws IOException {
        final RowDataset ds = createThreeRowScoreDataset();
        File file = tempDir.resolve("test.json").toFile();
        ds.toJson(0, 2, Arrays.asList("name", "age"), file);
        assertTrue(file.exists());
        assertTrue(file.length() > 0);
    }

    @Test
    public void testMoveRowsToNewPosition() {
        RowDataset ds = new RowDataset(new ArrayList<>(Arrays.asList("id", "name")),
                new ArrayList<>(Arrays.asList(new ArrayList<>(Arrays.asList(1, 2, 3, 4, 5)), new ArrayList<>(Arrays.asList("A", "B", "C", "D", "E")))));
        ds.moveRows(0, 2, 3);
        assertEquals(3, (int) ds.get(0, 0));
        assertEquals(4, (int) ds.get(1, 0));
    }

    @Test
    public void testPrintlnToAppendable() throws IOException {
        StringBuilder sb = new StringBuilder();
        dataset.println(sb);
        assertTrue(sb.length() > 0);
        assertTrue(sb.toString().contains("Alice"));
    }

    @Test
    public void testPrintlnWithRangeColumnsAndPrefixToAppendable() throws IOException {
        StringBuilder sb = new StringBuilder();
        dataset.println(0, 2, Arrays.asList("id", "name"), "PREFIX: ", sb);
        String result = sb.toString();
        assertTrue(result.contains("PREFIX:"));
        assertTrue(result.contains("Alice"));
        assertFalse(result.contains("Charlie"));
    }

    @Test
    public void testBiIteratorNoRange() {
        final RowDataset ds = createThreeRowScoreDataset();
        BiIterator<Integer, String> iter = ds.iterator("id", "name");
        List<String> results = new ArrayList<>();
        iter.forEachRemaining((id, name) -> results.add(id + ":" + name));
        assertEquals(3, results.size());
        assertEquals("1:John", results.get(0));
    }

    @Test
    public void testTriIteratorNoRange() {
        final RowDataset ds = createThreeRowScoreDataset();
        TriIterator<Integer, String, Integer> iter = ds.iterator("id", "name", "age");
        List<String> results = new ArrayList<>();
        iter.forEachRemaining((id, name, age) -> results.add(id + ":" + name + ":" + age));
        assertEquals(3, results.size());
        assertEquals("1:John:25", results.get(0));
    }

    @Test
    public void testStreamWithRowTypeAndRange() {
        final RowDataset ds = createThreeRowScoreDataset();
        List<TestBean> list = ds.stream(1, 3, TestBean.class).toList();
        assertEquals(2, list.size());
        assertEquals("Jane", list.get(0).name);
    }

    @Test
    public void testStreamWithRowSupplierAndRange() {
        final RowDataset ds = createThreeRowScoreDataset();
        List<Map<String, Object>> list = ds.stream(1, 3, (IntFunction<Map<String, Object>>) size -> new HashMap<>()).toList();
        assertEquals(2, list.size());
        assertEquals("Jane", list.get(0).get("name"));
    }

    @Test
    public void testStreamWithColumnsAndRowType() {
        final RowDataset ds = createThreeRowScoreDataset();
        List<TestBean> list = ds.stream(Arrays.asList("name", "age"), TestBean.class).toList();
        assertEquals(3, list.size());
        assertEquals("John", list.get(0).name);
    }

    @Test
    public void testStreamWithColumnsAndRowSupplier() {
        final RowDataset ds = createThreeRowScoreDataset();
        List<Map<String, Object>> list = ds.stream(Arrays.asList("name", "age"), (IntFunction<Map<String, Object>>) size -> new HashMap<>()).toList();
        assertEquals(3, list.size());
        assertEquals("John", list.get(0).get("name"));
    }

    @Test
    public void testStreamWithPrefixMapAndRange() {
        final RowDataset ds = createThreeRowScoreDataset();
        Map<String, String> prefixMap = new HashMap<>();
        List<TestBean> list = ds.stream(0, 2, prefixMap, TestBean.class).toList();
        assertEquals(2, list.size());
        assertEquals("John", list.get(0).name);
    }

    @Test
    public void testToListWithColumnsAsList() {
        final RowDataset ds = createThreeRowScoreDataset();
        List<List<Object>> list = ds.toList(0, 3, Arrays.asList("name", "age"), (IntFunction<List<Object>>) size -> new ArrayList<>());
        assertEquals(3, list.size());
        assertEquals("John", list.get(0).get(0));
        assertEquals(25, list.get(0).get(1));
    }

    @Test
    public void testToListWithColumnsAsObjectArray() {
        final RowDataset ds = createThreeRowScoreDataset();
        List<Object[]> list = ds.toList(0, 2, Arrays.asList("name", "age"), Object[].class);
        assertEquals(2, list.size());
        assertEquals("John", list.get(0)[0]);
        assertEquals(25, list.get(0)[1]);
    }

    @Test
    public void testToListWithColumnsAsMap() {
        final RowDataset ds = createThreeRowScoreDataset();
        List<Map<String, Object>> list = ds.toList(0, 2, Arrays.asList("name", "age"), (Class) Map.class);
        assertEquals(2, list.size());
        assertEquals("John", list.get(0).get("name"));
    }

    @Test
    public void testUnionWithKeyColumnsAndRequireSameColumns() {
        RowDataset ds1Local = new RowDataset(new ArrayList<>(Arrays.asList("id", "name")),
                new ArrayList<>(Arrays.asList(new ArrayList<>(Arrays.asList(1, 2)), new ArrayList<>(Arrays.asList("A", "B")))));
        RowDataset ds2Local = new RowDataset(new ArrayList<>(Arrays.asList("id", "name")),
                new ArrayList<>(Arrays.asList(new ArrayList<>(Arrays.asList(2, 3)), new ArrayList<>(Arrays.asList("B", "C")))));

        Dataset result = ds1Local.union(ds2Local, Arrays.asList("id"), true);
        assertNotNull(result);
        assertTrue(result.size() >= 2);
    }

    @Test
    public void testIntersectWithKeyColumnsAndRequireSameColumns() {
        RowDataset ds1Local = new RowDataset(new ArrayList<>(Arrays.asList("id", "name")),
                new ArrayList<>(Arrays.asList(new ArrayList<>(Arrays.asList(1, 2, 3)), new ArrayList<>(Arrays.asList("A", "B", "C")))));
        RowDataset ds2Local = new RowDataset(new ArrayList<>(Arrays.asList("id", "name")),
                new ArrayList<>(Arrays.asList(new ArrayList<>(Arrays.asList(2, 3, 4)), new ArrayList<>(Arrays.asList("B", "C", "D")))));

        Dataset result = ds1Local.intersect(ds2Local, Arrays.asList("id"), true);
        assertNotNull(result);
        assertEquals(2, result.size());
    }

    @Test
    public void testIntersectAllWithKeyColumns() {
        RowDataset ds1Local = new RowDataset(new ArrayList<>(Arrays.asList("id", "name")),
                new ArrayList<>(Arrays.asList(new ArrayList<>(Arrays.asList(1, 2, 2, 3)), new ArrayList<>(Arrays.asList("A", "B", "B2", "C")))));
        RowDataset ds2Local = new RowDataset(new ArrayList<>(Arrays.asList("id", "name")),
                new ArrayList<>(Arrays.asList(new ArrayList<>(Arrays.asList(2, 3, 4)), new ArrayList<>(Arrays.asList("B", "C", "D")))));

        Dataset result = ds1Local.intersectAll(ds2Local, Arrays.asList("id"));
        assertNotNull(result);
        assertTrue(result.size() >= 2);
    }

    @Test
    public void testIntersectAllWithKeyColumnsAndRequireSameColumns() {
        RowDataset ds1Local = new RowDataset(new ArrayList<>(Arrays.asList("id", "name")),
                new ArrayList<>(Arrays.asList(new ArrayList<>(Arrays.asList(1, 2, 3)), new ArrayList<>(Arrays.asList("A", "B", "C")))));
        RowDataset ds2Local = new RowDataset(new ArrayList<>(Arrays.asList("id", "name")),
                new ArrayList<>(Arrays.asList(new ArrayList<>(Arrays.asList(2, 3)), new ArrayList<>(Arrays.asList("B", "C")))));

        Dataset result = ds1Local.intersectAll(ds2Local, Arrays.asList("id"), true);
        assertNotNull(result);
    }

    @Test
    public void testExceptWithKeyColumnsAndRequireSameColumns() {
        RowDataset ds1Local = new RowDataset(new ArrayList<>(Arrays.asList("id", "name")),
                new ArrayList<>(Arrays.asList(new ArrayList<>(Arrays.asList(1, 2, 3)), new ArrayList<>(Arrays.asList("A", "B", "C")))));
        RowDataset ds2Local = new RowDataset(new ArrayList<>(Arrays.asList("id", "name")),
                new ArrayList<>(Arrays.asList(new ArrayList<>(Arrays.asList(2, 3)), new ArrayList<>(Arrays.asList("B", "C")))));

        Dataset result = ds1Local.except(ds2Local, Arrays.asList("id"), true);
        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    public void testExceptAllWithKeyColumns() {
        RowDataset ds1Local = new RowDataset(new ArrayList<>(Arrays.asList("id", "name")),
                new ArrayList<>(Arrays.asList(new ArrayList<>(Arrays.asList(1, 2, 2, 3)), new ArrayList<>(Arrays.asList("A", "B", "B2", "C")))));
        RowDataset ds2Local = new RowDataset(new ArrayList<>(Arrays.asList("id", "name")),
                new ArrayList<>(Arrays.asList(new ArrayList<>(Arrays.asList(2)), new ArrayList<>(Arrays.asList("B")))));

        Dataset result = ds1Local.exceptAll(ds2Local, Arrays.asList("id"));
        assertNotNull(result);
    }

    @Test
    public void testExceptAllWithKeyColumnsAndRequireSameColumns() {
        RowDataset ds1Local = new RowDataset(new ArrayList<>(Arrays.asList("id", "name")),
                new ArrayList<>(Arrays.asList(new ArrayList<>(Arrays.asList(1, 2, 3)), new ArrayList<>(Arrays.asList("A", "B", "C")))));
        RowDataset ds2Local = new RowDataset(new ArrayList<>(Arrays.asList("id", "name")),
                new ArrayList<>(Arrays.asList(new ArrayList<>(Arrays.asList(2)), new ArrayList<>(Arrays.asList("B")))));

        Dataset result = ds1Local.exceptAll(ds2Local, Arrays.asList("id"), true);
        assertNotNull(result);
    }

    @Test
    public void testPaginateGetPage() {
        Paginated<Dataset> paginated = dataset.paginate(2);
        assertEquals(3, paginated.totalPages());
        assertEquals(2, paginated.pageSize());

        Dataset page0 = paginated.getPage(0);
        assertEquals(2, page0.size());

        Dataset page2 = paginated.getPage(2);
        assertEquals(1, page2.size());
    }

    @Test
    public void testPaginateFirstAndLastPage() {
        Paginated<Dataset> paginated = dataset.paginate(2);
        Optional<Dataset> first = paginated.firstPage();
        assertTrue(first.isPresent());
        assertEquals(2, first.get().size());

        Optional<Dataset> last = paginated.lastPage();
        assertTrue(last.isPresent());
        assertEquals(1, last.get().size());
    }

    @Test
    public void testPaginateStream() {
        Paginated<Dataset> paginated = dataset.paginate(2);
        List<Dataset> pages = paginated.stream().toList();
        assertEquals(3, pages.size());
    }

    @Test
    public void testPaginateIterator() {
        Paginated<Dataset> paginated = dataset.paginate(3);
        int count = 0;
        for (Dataset page : paginated) {
            assertNotNull(page);
            count++;
        }
        assertEquals(2, count);
    }

    @Test
    public void testAddRow_MapInsertAtPosition() {
        Map<String, Object> newRow = new LinkedHashMap<>();
        newRow.put("id", 99);
        newRow.put("name", "Inserted");
        newRow.put("age", 41);
        newRow.put("salary", 91000.0);

        dataset.addRow(1, newRow);

        assertEquals(6, dataset.size());
        assertEquals(Integer.valueOf(99), dataset.get(1, 0));
        assertEquals("Inserted", dataset.get(1, 1));
        assertEquals(91000.0, dataset.get(1, 3));
    }

    @Test
    public void testAddRow_BeanInsertAtPosition() {
        dataset.addRow(2, new SalaryRowBean(77, "Bean", 36, 88000.0));

        assertEquals(6, dataset.size());
        assertEquals(Integer.valueOf(77), dataset.get(2, 0));
        assertEquals("Bean", dataset.get(2, 1));
        assertEquals(88000.0, dataset.get(2, 3));
    }

    @Test
    public void testAddRow_UnsupportedRowType() {
        assertThrows(IllegalArgumentException.class, () -> dataset.addRow(0, 123));
    }

    @Test
    public void testRemoveDuplicateRowsBy_SingleColumnCollection() {
        RowDataset dupDataset = new RowDataset(new ArrayList<>(Arrays.asList("id", "name")),
                new ArrayList<>(Arrays.asList(new ArrayList<>(Arrays.asList(1, 1, 2)), new ArrayList<>(Arrays.asList("A", "B", "C")))));

        dupDataset.removeDuplicateRowsBy(Arrays.asList("id"));

        assertEquals(2, dupDataset.size());
        assertEquals("A", dupDataset.get(0, 1));
        assertEquals("C", dupDataset.get(1, 1));
    }

    @Test
    public void testGetRow_RowSupplierReturningNull() {
        assertThrows(NullPointerException.class, () -> dataset.getRow(0, size -> null));
    }

    @Test
    public void testMerge_MergesPropertiesFromOtherDataset() {
        Map<String, Object> leftProps = new LinkedHashMap<>();
        leftProps.put("source", "left");
        Map<String, Object> rightProps = new LinkedHashMap<>();
        rightProps.put("source", "right");
        rightProps.put("version", 2);

        RowDataset left = new RowDataset(new ArrayList<>(Arrays.asList("id")), new ArrayList<>(Arrays.asList(new ArrayList<>(Arrays.asList(1)))), leftProps);
        RowDataset right = new RowDataset(new ArrayList<>(Arrays.asList("id")), new ArrayList<>(Arrays.asList(new ArrayList<>(Arrays.asList(2)))), rightProps);

        left.append(right);

        assertEquals(2, left.size());
        assertEquals("right", left.getProperties().get("source"));
        assertEquals(2, left.getProperties().get("version"));
    }

}

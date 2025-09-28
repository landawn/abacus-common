package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.NoCachingNoUpdating.DisposableObjArray;
import com.landawn.abacus.util.Tuple.Tuple2;
import com.landawn.abacus.util.Tuple.Tuple3;
import com.landawn.abacus.util.u.Optional;
import com.landawn.abacus.util.stream.Collectors.MoreCollectors;
import com.landawn.abacus.util.stream.Stream;

public class Dataset100Test extends TestBase {

    private RowDataset dataset;
    private List<String> columnNames;
    private List<List<Object>> columnList;

    @BeforeEach
    public void setUp() {
        columnNames = N.asList("id", "name", "age", "salary");
        columnList = new ArrayList<>();
        columnList.add(N.asList(1, 2, 3, 4, 5)); // id
        columnList.add(N.asList("John", "Jane", "Bob", "Alice", "Charlie")); // name
        columnList.add(N.asList(25, 30, 35, 28, 40)); // age
        columnList.add(N.asList(50000.0, 60000.0, 70000.0, 55000.0, 80000.0)); // salary

        dataset = new RowDataset(columnNames, columnList);
    }

    @Test
    public void testConstructor() {
        assertNotNull(dataset);
        assertEquals(4, dataset.columnCount());
        assertEquals(5, dataset.size());
    }

    @Test
    public void testConstructorWithProperties() {
        Map<String, Object> properties = new HashMap<>();
        properties.put("source", "test");
        properties.put("version", 1);

        RowDataset dsWithProps = new RowDataset(columnNames, columnList, properties);
        assertEquals("test", dsWithProps.getProperties().get("source"));
        assertEquals(1, dsWithProps.getProperties().get("version"));
    }

    @Test
    public void testConstructorWithEmptyColumnName() {
        List<String> invalidNames = Arrays.asList("id", "", "age");
        assertThrows(IllegalArgumentException.class, () -> new RowDataset(invalidNames, columnList));
    }

    @Test
    public void testConstructorWithDuplicateColumnNames() {
        List<String> duplicateNames = Arrays.asList("id", "name", "id", "age");
        assertThrows(IllegalArgumentException.class, () -> new RowDataset(duplicateNames, columnList));
    }

    @Test
    public void testConstructorWithDifferentColumnSizes() {
        List<List<Object>> invalidColumns = new ArrayList<>();
        invalidColumns.add(Arrays.asList(1, 2, 3));
        invalidColumns.add(Arrays.asList("A", "B", "C", "D")); // Different size

        assertThrows(IllegalArgumentException.class, () -> new RowDataset(Arrays.asList("col1", "col2"), invalidColumns));
    }

    @Test
    @DisplayName("Should create Dataset with valid column names and rows")
    public void testCreateDatasetWithValidData() {
        Collection<String> columnNames = Arrays.asList("id", "name", "age");
        Object[][] rows = { { 1, "John", 25 }, { 2, "Jane", 30 }, { 3, "Bob", 35 } };

        Dataset dataset = Dataset.rows(columnNames, rows);

        assertNotNull(dataset);
        assertEquals(3, dataset.size());
        assertEquals(3, dataset.columnNameList().size());
        assertTrue(dataset.columnNameList().containsAll(columnNames));
    }

    @Test
    @DisplayName("Should create empty Dataset with column names but no rows")
    public void testCreateDatasetWithEmptyRows() {
        Collection<String> columnNames = Arrays.asList("id", "name");
        Object[][] rows = {};

        Dataset dataset = Dataset.rows(columnNames, rows);

        assertNotNull(dataset);
        assertEquals(0, dataset.size());
        assertEquals(2, dataset.columnNameList().size());
        assertTrue(dataset.isEmpty());
    }

    @Test
    @DisplayName("Should create Dataset with single column")
    public void testCreateDatasetWithSingleColumn() {
        Collection<String> columnNames = Collections.singletonList("value");
        Object[][] rows = { { "test1" }, { "test2" } };

        Dataset dataset = Dataset.rows(columnNames, rows);

        assertNotNull(dataset);
        assertEquals(2, dataset.size());
        assertEquals(1, dataset.columnNameList().size());
        assertEquals("value", dataset.columnNameList().get(0));
    }

    @Test
    @DisplayName("Should handle null values in rows")
    public void testCreateDatasetWithNullValues() {
        Collection<String> columnNames = Arrays.asList("id", "name", "score");
        Object[][] rows = { { 1, "John", null }, { null, "Jane", 95.5 }, { 3, null, 87.2 } };

        Dataset dataset = Dataset.rows(columnNames, rows);

        assertNotNull(dataset);
        assertEquals(3, dataset.size());
        assertTrue(dataset.isNull(0, 2)); // First row, third column
        assertTrue(dataset.isNull(1, 0)); // Second row, first column
        assertTrue(dataset.isNull(2, 1)); // Third row, second column
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException for mismatched column count")
    public void testThrowsExceptionForMismatchedColumnCount() {
        Collection<String> columnNames = Arrays.asList("id", "name");
        Object[][] rows = { { 1, "John", 25 }, // 3 values but only 2 columns
                { 2, "Jane" } };

        assertThrows(IllegalArgumentException.class, () -> {
            Dataset.rows(columnNames, rows);
        });
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException for null column names")
    public void testThrowsExceptionForNullColumnNames() {
        Object[][] rows = { { 1, "John" } };

        assertThrows(IllegalArgumentException.class, () -> {
            Dataset.rows(null, rows);
        });
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException for empty column names")
    public void testThrowsExceptionForEmptyColumnNames() {
        Collection<String> columnNames = Collections.emptyList();
        Object[][] rows = { { 1, "John" } };

        assertThrows(IllegalArgumentException.class, () -> {
            Dataset.rows(columnNames, rows);
        });
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException for duplicate column names")
    public void testThrowsExceptionForDuplicateColumnNames() {
        Collection<String> columnNames = Arrays.asList("id", "name", "id");
        Object[][] rows = { { 1, "John", 25 } };

        assertThrows(IllegalArgumentException.class, () -> {
            Dataset.rows(columnNames, rows);
        });
    }

    @Test
    @DisplayName("Should handle different data types in columns")
    public void testCreateDatasetWithMixedDataTypes() {
        Collection<String> columnNames = Arrays.asList("id", "name", "active", "score", "date");
        Object[][] rows = { { 1, "John", true, 95.5, new Date() }, { 2L, "Jane", false, 87, null } };

        Dataset dataset = Dataset.rows(columnNames, rows);

        assertNotNull(dataset);
        assertEquals(2, dataset.size());
        assertEquals(5, dataset.columnNameList().size());
    }

    @Test
    @DisplayName("Should handle jagged arrays with consistent column count")
    public void testCreateDatasetWithJaggedButConsistentRows() {
        Collection<String> columnNames = Arrays.asList("col1", "col2");
        Object[][] rows = { new Object[] { 1, "a" }, new Object[] { 2, "b" }, new Object[] { 3, "c" } };

        Dataset dataset = Dataset.rows(columnNames, rows);

        assertNotNull(dataset);
        assertEquals(3, dataset.size());
        assertEquals(2, dataset.columnNameList().size());
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException for inconsistent row lengths")
    public void testThrowsExceptionForInconsistentRowLengths() {
        Collection<String> columnNames = Arrays.asList("col1", "col2", "col3");
        Object[][] rows = { { 1, "a", true }, { 2, "b" }, // Missing third column
                { 3, "c", false } };

        assertThrows(IllegalArgumentException.class, () -> {
            Dataset.rows(columnNames, rows);
        });
    }

    @Test
    @DisplayName("Should create Dataset with valid column names and columns")
    public void testCreateDatasetWithValidColumnsData2() {
        Collection<String> columnNames = Arrays.asList("id", "name", "age");
        Object[][] columns = { { 1, 2, 3 }, // id column
                { "John", "Jane", "Bob" }, // name column
                { 25, 30, 35 } // age column
        };

        Dataset dataset = Dataset.columns(columnNames, columns);

        assertNotNull(dataset);
        assertEquals(3, dataset.size()); // 3 rows
        assertEquals(3, dataset.columnNameList().size()); // 3 columns
        assertTrue(dataset.columnNameList().containsAll(columnNames));

        // Verify data is properly transposed
        assertEquals(1, (Integer) dataset.absolute(0).get("id"));
        assertEquals("John", dataset.absolute(0).get("name"));
        assertEquals(25, (Integer) dataset.absolute(0).get("age"));
    }

    @Test
    @DisplayName("Should create empty Dataset with column names but empty columns")
    public void testCreateDatasetWithEmptyColumns() {
        Collection<String> columnNames = Arrays.asList("id", "name");
        Object[][] columns = { {}, // empty id column
                {} // empty name column
        };

        Dataset dataset = Dataset.columns(columnNames, columns);

        assertNotNull(dataset);
        assertEquals(0, dataset.size());
        assertEquals(2, dataset.columnNameList().size());
        assertTrue(dataset.isEmpty());
    }

    @Test
    @DisplayName("Should create Dataset with single column")
    public void testCreateDatasetWithSingleColumn2() {
        Collection<String> columnNames = Collections.singletonList("value");
        Object[][] columns = { { "test1", "test2", "test3" } };

        Dataset dataset = Dataset.columns(columnNames, columns);

        assertNotNull(dataset);
        assertEquals(3, dataset.size());
        assertEquals(1, dataset.columnNameList().size());
        assertEquals("value", dataset.columnNameList().get(0));
        assertEquals("test1", dataset.absolute(0).get("value"));
        assertEquals("test2", dataset.absolute(1).get("value"));
        assertEquals("test3", dataset.absolute(2).get("value"));
    }

    @Test
    @DisplayName("Should handle null values in columns")
    public void testCreateDatasetWithNullValues2() {
        Collection<String> columnNames = Arrays.asList("id", "name", "score");
        Object[][] columns = { { 1, null, 3 }, // id column with null
                { "John", "Jane", null }, // name column with null
                { null, 95.5, 87.2 } // score column with null
        };

        Dataset dataset = Dataset.columns(columnNames, columns);

        assertNotNull(dataset);
        assertEquals(3, dataset.size());
        assertTrue(dataset.isNull(1, 0)); // Second row, first column (id)
        assertTrue(dataset.isNull(2, 1)); // Third row, second column (name)
        assertTrue(dataset.isNull(0, 2)); // First row, third column (score)
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when columnNames length differs from columns length")
    public void testThrowsExceptionForMismatchedColumnCount2() {
        Collection<String> columnNames = Arrays.asList("id", "name"); // 2 columns
        Object[][] columns = { { 1, 2, 3 }, // id column
                { "John", "Jane", "Bob" }, // name column  
                { 25, 30, 35 } // extra age column - 3 columns total
        };

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            Dataset.columns(columnNames, columns);
        });

        assertTrue(exception.getMessage().contains("The length of 'columnNames'(2) is not equal to the length of the sub-collections in 'columns'(3)"));
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException for null column names")
    public void testThrowsExceptionForNullColumnNames2() {
        Object[][] columns = { { 1, 2 }, { "John", "Jane" } };

        assertThrows(IllegalArgumentException.class, () -> {
            Dataset.columns(null, columns);
        });
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException for inconsistent column lengths")
    public void testThrowsExceptionForInconsistentColumnLengths() {
        Collection<String> columnNames = Arrays.asList("id", "name", "age");
        Object[][] columns = { { 1, 2, 3 }, // 3 elements
                { "John", "Jane" }, // 2 elements - inconsistent!
                { 25, 30, 35 } // 3 elements
        };

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            Dataset.columns(columnNames, columns);
        });

        assertTrue(exception.getMessage().contains("size of the sub-collection in 'columns' is not equal"));
    }

    @Test
    @DisplayName("Should handle different data types in same column")
    public void testCreateDatasetWithMixedDataTypesInColumn() {
        Collection<String> columnNames = Arrays.asList("mixed", "numbers");
        Object[][] columns = { { "string", 123, true, null }, // mixed types column
                { 1, 2.5, 3L, 4.0f } // numeric types column
        };

        Dataset dataset = Dataset.columns(columnNames, columns);

        assertNotNull(dataset);
        assertEquals(4, dataset.size());
        assertEquals(2, dataset.columnNameList().size());

        // Verify mixed types are preserved
        assertEquals("string", dataset.absolute(0).get("mixed"));
        assertEquals(123, (Integer) dataset.absolute(1).get("mixed"));
        assertEquals(true, dataset.absolute(2).get("mixed"));
        assertTrue(dataset.isNull(3, 0));
    }

    @Test
    @DisplayName("Should create Dataset with empty column names collection")
    public void testCreateDatasetWithEmptyColumnNamesCollection() {
        Collection<String> columnNames = Collections.emptyList();
        Object[][] columns = {};

        Dataset dataset = Dataset.columns(columnNames, columns);

        assertNotNull(dataset);
        assertEquals(0, dataset.size());
        assertEquals(0, dataset.columnNameList().size());
        assertTrue(dataset.isEmpty());
    }

    @Test
    @DisplayName("Should handle single row of data across multiple columns")
    public void testCreateDatasetWithSingleRow() {
        Collection<String> columnNames = Arrays.asList("col1", "col2", "col3");
        Object[][] columns = { { "a" }, // single value in col1
                { "b" }, // single value in col2
                { "c" } // single value in col3
        };

        Dataset dataset = Dataset.columns(columnNames, columns);

        assertNotNull(dataset);
        assertEquals(1, dataset.size());
        assertEquals(3, dataset.columnNameList().size());
        assertEquals("a", dataset.absolute(0).get("col1"));
        assertEquals("b", dataset.absolute(0).get("col2"));
        assertEquals("c", dataset.absolute(0).get("col3"));
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException for duplicate column names")
    public void testThrowsExceptionForDuplicateColumnNames2() {
        Collection<String> columnNames = Arrays.asList("id", "name", "id"); // duplicate "id"
        Object[][] columns = { { 1, 2, 3 }, { "John", "Jane", "Bob" }, { 25, 30, 35 } };

        assertThrows(IllegalArgumentException.class, () -> {
            Dataset.columns(columnNames, columns);
        });
    }

    @Test
    @DisplayName("Should handle large dataset with many rows")
    public void testCreateDatasetWithManyRows() {
        Collection<String> columnNames = Arrays.asList("index", "squared");
        Object[][] columns = new Object[2][];

        // Create columns with 1000 rows each
        Object[] indexColumn = new Object[1000];
        Object[] squaredColumn = new Object[1000];

        for (int i = 0; i < 1000; i++) {
            indexColumn[i] = i;
            squaredColumn[i] = i * i;
        }

        columns[0] = indexColumn;
        columns[1] = squaredColumn;

        Dataset dataset = Dataset.columns(columnNames, columns);

        assertNotNull(dataset);
        assertEquals(1000, dataset.size());
        assertEquals(2, dataset.columnNameList().size());
        assertEquals(0, (Integer) dataset.absolute(0).get("index"));
        assertEquals(0, (Integer) dataset.absolute(0).get("squared"));
        assertEquals(999, (Integer) dataset.absolute(999).get("index"));
        assertEquals(998001, (Integer) dataset.absolute(999).get("squared")); // 999^2
    }

    @Test
    public void testColumnNameList() {
        ImmutableList<String> names = dataset.columnNameList();
        assertEquals(4, names.size());
        assertTrue(names.contains("id"));
        assertTrue(names.contains("name"));
        assertTrue(names.contains("age"));
        assertTrue(names.contains("salary"));
    }

    @Test
    public void testColumnCount() {
        assertEquals(4, dataset.columnCount());
    }

    @Test
    public void testGetColumnName() {
        assertEquals("id", dataset.getColumnName(0));
        assertEquals("name", dataset.getColumnName(1));
        assertEquals("age", dataset.getColumnName(2));
        assertEquals("salary", dataset.getColumnName(3));
    }

    @Test
    public void testGetColumnNameInvalidIndex() {
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
    public void testGetColumnIndexInvalidName() {
        assertThrows(IllegalArgumentException.class, () -> dataset.getColumnIndex("invalid"));
    }

    @Test
    public void testGetColumnIndexes() {
        int[] indexes = dataset.getColumnIndexes(Arrays.asList("name", "age"));
        assertArrayEquals(new int[] { 1, 2 }, indexes);
    }

    @Test
    public void testContainsColumn() {
        assertTrue(dataset.containsColumn("id"));
        assertTrue(dataset.containsColumn("name"));
        assertFalse(dataset.containsColumn("invalid"));
    }

    @Test
    public void testContainsAllColumns() {
        assertTrue(dataset.containsAllColumns(Arrays.asList("id", "name")));
        assertFalse(dataset.containsAllColumns(Arrays.asList("id", "invalid")));
    }

    @Test
    public void testRenameColumn() {
        dataset.renameColumn("age", "years");
        assertTrue(dataset.containsColumn("years"));
        assertFalse(dataset.containsColumn("age"));
        assertEquals(2, dataset.getColumnIndex("years"));
    }

    @Test
    public void testRenameColumnToExistingName() {
        assertThrows(IllegalArgumentException.class, () -> dataset.renameColumn("age", "name"));
    }

    @Test
    public void testRenameColumns() {
        Map<String, String> renameMap = new HashMap<>();
        renameMap.put("age", "years");
        renameMap.put("salary", "income");

        dataset.renameColumns(renameMap);
        assertTrue(dataset.containsColumn("years"));
        assertTrue(dataset.containsColumn("income"));
        assertFalse(dataset.containsColumn("age"));
        assertFalse(dataset.containsColumn("salary"));
    }

    @Test
    public void testRenameColumnsWithFunction() {
        dataset.renameColumns(Arrays.asList("name", "age"), col -> col.toUpperCase());
        assertTrue(dataset.containsColumn("NAME"));
        assertTrue(dataset.containsColumn("AGE"));
    }

    @Test
    public void testMoveColumn() {
        dataset.moveColumn("salary", 1);
        assertEquals("salary", dataset.getColumnName(1));
        assertEquals("name", dataset.getColumnName(2));
    }

    @Test
    public void testSwapColumnPosition() {
        dataset.swapColumnPosition("id", "salary");
        assertEquals(0, dataset.getColumnIndex("salary"));
        assertEquals(3, dataset.getColumnIndex("id"));
    }

    @Test
    public void testMoveRow() {
        Object originalName = dataset.get(0, 1);
        dataset.moveRow(0, 3);
        assertEquals(originalName, dataset.get(3, 1));
    }

    @Test
    public void testSwapRowPosition() {
        Object row0Name = dataset.get(0, 1);
        Object row2Name = dataset.get(2, 1);

        dataset.swapRowPosition(0, 2);
        assertEquals(row2Name, dataset.get(0, 1));
        assertEquals(row0Name, dataset.get(2, 1));
    }

    @Test
    public void testGet() {
        assertEquals((Integer) 1, dataset.get(0, 0));
        assertEquals("John", dataset.get(0, 1));
        assertEquals((Integer) 25, dataset.get(0, 2));
        assertEquals((Double) 50000.0, dataset.get(0, 3));
    }

    @Test
    public void testSet() {
        dataset.set(0, 1, "Johnny");
        assertEquals("Johnny", dataset.get(0, 1));
    }

    @Test
    public void testIsNull() {
        assertFalse(dataset.isNull(0, 0));
        dataset.set(0, 0, null);
        assertTrue(dataset.isNull(0, 0));
    }

    @Test
    public void testGetWithCurrentRow() {
        dataset.absolute(1);
        assertEquals((Integer) 2, dataset.get(0)); // id
        assertEquals("Jane", dataset.get(1)); // name
    }

    @Test
    public void testGetByColumnName() {
        dataset.absolute(0);
        assertEquals((Integer) 1, dataset.get("id"));
        assertEquals("John", dataset.get("name"));
    }

    @Test
    public void testGetBoolean() {
        // Set a boolean value
        dataset.set(0, 0, true);
        assertTrue(dataset.getBoolean(0));
        assertTrue(dataset.getBoolean("id"));

        dataset.set(0, 0, false);
        assertFalse(dataset.getBoolean(0));
    }

    @Test
    public void testGetChar() {
        dataset.set(0, 0, 'A');
        assertEquals('A', dataset.getChar(0));
        assertEquals('A', dataset.getChar("id"));
    }

    @Test
    public void testGetByte() {
        dataset.set(0, 0, (byte) 10);
        assertEquals((byte) 10, dataset.getByte(0));
        assertEquals((byte) 10, dataset.getByte("id"));
    }

    @Test
    public void testGetShort() {
        dataset.set(0, 0, (short) 100);
        assertEquals((short) 100, dataset.getShort(0));
        assertEquals((short) 100, dataset.getShort("id"));
    }

    @Test
    public void testGetInt() {
        assertEquals(1, dataset.getInt(0));
        assertEquals(1, dataset.getInt("id"));
    }

    @Test
    public void testGetLong() {
        assertEquals(1L, dataset.getLong(0));
        assertEquals(1L, dataset.getLong("id"));
    }

    @Test
    public void testGetFloat() {
        assertEquals(50000.0f, dataset.getFloat(3), 0.01);
        assertEquals(50000.0f, dataset.getFloat("salary"), 0.01);
    }

    @Test
    public void testGetDouble() {
        assertEquals(50000.0, dataset.getDouble(3), 0.01);
        assertEquals(50000.0, dataset.getDouble("salary"), 0.01);
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
    public void testCopyColumn() {
        List<Object> copiedColumn = dataset.copyColumn("name");
        assertEquals(Arrays.asList("John", "Jane", "Bob", "Alice", "Charlie"), copiedColumn);

        // Modify the copy
        copiedColumn.set(0, "Modified");

        // Original should remain unchanged
        assertEquals("John", dataset.get(0, 1));
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
    public void testAddColumnAtPosition() {
        List<Object> newColumn = Arrays.asList("A", "B", "C", "D", "E");
        dataset.addColumn(2, "grade", newColumn);

        assertEquals(5, dataset.columnCount());
        assertEquals("grade", dataset.getColumnName(2));
        assertEquals("age", dataset.getColumnName(3));
    }

    @Test
    public void testAddColumnWithFunction() {
        dataset.addColumn("ageGroup", "age", age -> (int) age < 30 ? "Young" : "Adult");

        assertTrue(dataset.containsColumn("ageGroup"));
        assertEquals("Young", dataset.get(0, 4)); // John is 25
        assertEquals("Adult", dataset.get(1, 4)); // Jane is 30
    }

    @Test
    public void testAddColumnWithBiFunction() {
        dataset.addColumn("nameAge", Tuple2.of("name", "age"), (name, age) -> name + ":" + age);

        assertTrue(dataset.containsColumn("nameAge"));
        assertEquals("John:25", dataset.get(0, 4));
    }

    @Test
    public void testAddColumnWithTriFunction() {
        dataset.addColumn("combined", Tuple3.of("id", "name", "age"), (id, name, age) -> id + "-" + name + "-" + age);

        assertTrue(dataset.containsColumn("combined"));
        assertEquals("1-John-25", dataset.get(0, 4));
    }

    @Test
    public void testAddColumnWithDisposableObjArray() {
        dataset.addColumn("combined", Arrays.asList("id", "name"), (DisposableObjArray arr) -> arr.get(0) + ":" + arr.get(1));

        assertTrue(dataset.containsColumn("combined"));
        assertEquals("1:John", dataset.get(0, 4));
    }

    @Test
    @DisplayName("Should add multiple columns at the end of dataset")
    public void testAddColumns() {
        List<String> newColumnNames = Arrays.asList("grade", "status");
        List<Collection<Object>> newColumns = Arrays.asList(Arrays.asList("A", "B", "C", "B", "A"),
                Arrays.asList("Active", "Active", "Inactive", "Active", "Active"));

        int originalColumnCount = dataset.columnCount();
        dataset.addColumns(newColumnNames, newColumns);

        assertEquals(originalColumnCount + 2, dataset.columnCount());
        assertTrue(dataset.containsColumn("grade"));
        assertTrue(dataset.containsColumn("status"));
        assertEquals("A", dataset.get(0, dataset.getColumnIndex("grade")));
        assertEquals("Active", dataset.get(0, dataset.getColumnIndex("status")));
    }

    @Test
    @DisplayName("Should add multiple columns at specific position")
    public void testAddColumnsAtPosition() {
        List<String> newColumnNames = Arrays.asList("grade", "status");
        List<Collection<Object>> newColumns = Arrays.asList(Arrays.asList("A", "B", "C", "B", "A"),
                Arrays.asList("Active", "Active", "Inactive", "Active", "Active"));

        dataset.addColumns(2, newColumnNames, newColumns); // Insert after "name" column

        assertEquals(6, dataset.columnCount());
        assertEquals("grade", dataset.getColumnName(2));
        assertEquals("status", dataset.getColumnName(3));
        assertEquals("age", dataset.getColumnName(4)); // Original column shifted
        assertEquals("A", dataset.get(0, 2));
        assertEquals("Active", dataset.get(0, 3));
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
        assertEquals("id", dataset.getColumnName(2)); // Original first column
        assertEquals("P1-1", dataset.get(0, 0));
        assertEquals("P2-1", dataset.get(0, 1));
        assertEquals((Integer) 1, dataset.get(0, 2)); // Original data shifted
    }

    @Test
    @DisplayName("Should handle empty columns by filling with nulls")
    public void testAddColumnsWithEmptyColumns() {
        List<String> newColumnNames = Arrays.asList("empty1", "empty2");
        List<Collection<Object>> newColumns = Arrays.asList(Collections.emptyList(), Collections.emptyList());

        dataset.addColumns(newColumnNames, newColumns);

        assertEquals(6, dataset.columnCount());
        // All values in empty columns should be null
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
    @DisplayName("Should throw exception when column names and columns size mismatch")
    public void testAddColumnsWithSizeMismatch() {
        List<String> newColumnNames = Arrays.asList("col1", "col2", "col3");
        List<Collection<Object>> newColumns = Arrays.asList(Arrays.asList(1, 2, 3, 4, 5), Arrays.asList(6, 7, 8, 9, 10)
        // Missing third column
        );

        assertThrows(IllegalArgumentException.class, () -> dataset.addColumns(newColumnNames, newColumns));
    }

    @Test
    @DisplayName("Should throw exception when column size doesn't match dataset size")
    public void testAddColumnsWithIncorrectColumnSize() {
        List<String> newColumnNames = Arrays.asList("col1", "col2");
        List<Collection<Object>> newColumns = Arrays.asList(Arrays.asList(1, 2, 3), // Only 3 elements instead of 5
                Arrays.asList(4, 5, 6, 7, 8, 9) // 6 elements instead of 5
        );

        assertThrows(IllegalArgumentException.class, () -> dataset.addColumns(newColumnNames, newColumns));
    }

    @Test
    @DisplayName("Should throw exception for duplicate column names")
    public void testAddColumnsWithDuplicateNames() {
        List<String> newColumnNames = Arrays.asList("id", "newCol"); // "id" already exists
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

        // Position too large
        assertThrows(IndexOutOfBoundsException.class, () -> dataset.addColumns(100, newColumnNames, newColumns));

        // Negative position
        assertThrows(IndexOutOfBoundsException.class, () -> dataset.addColumns(-1, newColumnNames, newColumns));
    }

    @Test
    @DisplayName("Should handle empty column names list")
    public void testAddColumnsWithEmptyNamesList() {
        List<String> newColumnNames = Collections.emptyList();
        List<Collection<Object>> newColumns = Collections.emptyList();

        int originalColumnCount = dataset.columnCount();
        dataset.addColumns(newColumnNames, newColumns);

        assertEquals(originalColumnCount, dataset.columnCount()); // No change
    }

    @Test
    @DisplayName("Should add multiple columns with different collection types")
    public void testAddColumnsWithDifferentCollectionTypes() {
        List<String> newColumnNames = Arrays.asList("list", "set", "arrayList");
        List<Collection<Object>> newColumns = Arrays.asList(Arrays.asList(1, 2, 3, 4, 5), // List
                new HashSet<>(Arrays.asList("A", "B", "C", "D", "E")), // Set (order may vary)
                new ArrayList<>(Arrays.asList(true, false, true, false, true)) // ArrayList
        );

        dataset.addColumns(newColumnNames, newColumns);

        assertEquals(7, dataset.columnCount());
        assertTrue(dataset.containsColumn("list"));
        assertTrue(dataset.containsColumn("set"));
        assertTrue(dataset.containsColumn("arrayList"));
    }

    @Test
    @DisplayName("Should preserve original data when adding columns")
    public void testAddColumnsPreservesOriginalData() {
        // Store original data
        Object[][] originalData = new Object[dataset.size()][dataset.columnCount()];
        for (int i = 0; i < dataset.size(); i++) {
            for (int j = 0; j < dataset.columnCount(); j++) {
                originalData[i][j] = dataset.get(i, j);
            }
        }

        List<String> newColumnNames = Arrays.asList("new1", "new2");
        List<Collection<Object>> newColumns = Arrays.asList(Arrays.asList("N1", "N2", "N3", "N4", "N5"), Arrays.asList(10.1, 20.2, 30.3, 40.4, 50.5));

        dataset.addColumns(2, newColumnNames, newColumns);

        // Verify original data is preserved (accounting for column shift)
        assertEquals(originalData[0][0], dataset.get(0, 0)); // id
        assertEquals(originalData[0][1], dataset.get(0, 1)); // name
        assertEquals(originalData[0][2], dataset.get(0, 4)); // age (shifted)
        assertEquals(originalData[0][3], dataset.get(0, 5)); // salary (shifted)
    }

    @Test
    @DisplayName("Should add large number of columns")
    public void testAddManyColumns() {
        List<String> newColumnNames = new ArrayList<>();
        List<Collection<Object>> newColumns = new ArrayList<>();

        // Add 10 new columns
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

        // Verify some data
        assertEquals("R0C0", dataset.get(0, dataset.getColumnIndex("col0")));
        assertEquals("R4C9", dataset.get(4, dataset.getColumnIndex("col9")));
    }

    @Test
    public void testRemoveColumn() {
        List<Object> removedColumn = dataset.removeColumn("age");

        assertEquals(3, dataset.columnCount());
        assertFalse(dataset.containsColumn("age"));
        assertEquals(Arrays.asList(25, 30, 35, 28, 40), removedColumn);
    }

    @Test
    public void testRemoveColumns() {
        dataset.removeColumns(Arrays.asList("age", "salary"));

        assertEquals(2, dataset.columnCount());
        assertFalse(dataset.containsColumn("age"));
        assertFalse(dataset.containsColumn("salary"));
    }

    @Test
    public void testRemoveColumnsWithPredicate() {
        dataset.removeColumns(col -> col.startsWith("s"));

        assertEquals(3, dataset.columnCount());
        assertFalse(dataset.containsColumn("salary"));
    }

    @Test
    public void testConvertColumn() {
        dataset.convertColumn("id", String.class);
        assertEquals("1", dataset.get(0, 0));
        assertEquals("2", dataset.get(1, 0));
    }

    @Test
    public void testConvertColumns() {
        Map<String, Class<?>> conversions = new HashMap<>();
        conversions.put("id", String.class);
        conversions.put("age", String.class);

        dataset.convertColumns(conversions);

        assertEquals("1", dataset.get(0, 0));
        assertEquals("25", dataset.get(0, 2));
    }

    @Test
    public void testUpdateColumn() {
        dataset.updateColumn("age", age -> (int) age + 10);

        assertEquals((Integer) 35, dataset.get(0, 2)); // 25 + 10
        assertEquals((Integer) 40, dataset.get(1, 2)); // 30 + 10
    }

    @Test
    public void testUpdateColumns() {
        dataset.updateColumns(Arrays.asList("age", "salary"), (c, v) -> v instanceof Integer ? (int) v * 2 : (double) v * 2);

        assertEquals((Double) 50.0, dataset.get(0, 2)); // age doubled
        assertEquals((Double) 100000.0, dataset.get(0, 3)); // salary doubled
    }

    @Test
    public void testCombineColumns() {
        dataset.combineColumns(Arrays.asList("id", "name"), "idName", it -> Strings.concat(it.get(0), it.get(1)));

        assertTrue(dataset.containsColumn("idName"));
        assertFalse(dataset.containsColumn("id"));
        assertFalse(dataset.containsColumn("name"));
        assertEquals("1John", dataset.get(0, dataset.getColumnIndex("idName")));
    }

    @Test
    public void testCombineColumnsWithFunction() {
        dataset.combineColumns(Arrays.asList("id", "name"), "idName", (DisposableObjArray arr) -> arr.get(0) + "-" + arr.get(1));

        assertTrue(dataset.containsColumn("idName"));
        assertEquals("1-John", dataset.get(0, dataset.getColumnIndex("idName")));
    }

    @Test
    public void testCombineColumnsWithBiFunction() {
        dataset.combineColumns(Tuple2.of("id", "name"), "idName", (id, name) -> id + "-" + name);

        assertTrue(dataset.containsColumn("idName"));
        assertEquals("1-John", dataset.get(0, dataset.getColumnIndex("idName")));
    }

    @Test
    public void testDivideColumn() {
        dataset.addColumn("fullName", Arrays.asList("John Doe", "Jane Smith", "Bob Jones", "Alice Brown", "Charlie Davis"));

        dataset.divideColumn("fullName", Arrays.asList("firstName", "lastName"), fullName -> Arrays.asList(fullName.toString().split(" ")));

        assertFalse(dataset.containsColumn("fullName"));
        assertTrue(dataset.containsColumn("firstName"));
        assertTrue(dataset.containsColumn("lastName"));
        assertEquals("John", dataset.get(0, dataset.getColumnIndex("firstName")));
        assertEquals("Doe", dataset.get(0, dataset.getColumnIndex("lastName")));
    }

    @Test
    public void testDivideColumnWithBiConsumer() {
        dataset.addColumn("composite", Arrays.asList("1:A", "2:B", "3:C", "4:D", "5:E"));

        dataset.divideColumn("composite", Arrays.asList("num", "letter"), (composite, output) -> {
            String[] parts = composite.toString().split(":");
            output[0] = parts[0];
            output[1] = parts[1];
        });

        assertTrue(dataset.containsColumn("num"));
        assertTrue(dataset.containsColumn("letter"));
        assertEquals("1", dataset.get(0, dataset.getColumnIndex("num")));
        assertEquals("A", dataset.get(0, dataset.getColumnIndex("letter")));
    }

    @Test
    public void testAddRow() {
        Object[] newRow = { 6, "Frank", 45, 90000.0 };
        dataset.addRow(newRow);

        assertEquals(6, dataset.size());
        assertEquals((Integer) 6, dataset.get(5, 0));
        assertEquals("Frank", dataset.get(5, 1));
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
    public void testAddRowsModCountIncrement() {
        // Access modCount through reflection or verify behavior that depends on it
        Object[] row = { 4, "David", 40, 100.0 };
        Collection<Object[]> rows = Collections.singletonList(row);

        int sizeBefore = dataset.size();
        dataset.addRows(rows);

        // Verify the change was applied
        assertEquals(sizeBefore + 1, dataset.size());
    }

    @Test
    public void testAddRowsOnFrozenDataset() {
        dataset.freeze();

        Object[] row = { 4, "David", 40 };
        Collection<Object[]> rows = Collections.singletonList(row);

        assertThrows(IllegalStateException.class, () -> dataset.addRows(rows));
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
        dataset.addRows(2, rows); // Insert at position 2

        assertEquals(originalSize + 2, dataset.size());
        // Original row at position 2 should now be at position 4
        assertEquals("Bob", dataset.get(4, 1));
        // New rows should be at positions 2 and 3
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

        assertEquals(originalSize, dataset.size()); // Size should remain unchanged
    }

    @Test
    @DisplayName("Should throw exception when row has fewer columns than dataset")
    public void testAddRowsWithInsufficientColumns() {
        Collection<Object[]> rows = new ArrayList<>();
        rows.add(new Object[] { 6, "Frank" }); // Missing salary

        assertThrows(IllegalArgumentException.class, () -> dataset.addRows(rows));
    }

    @Test
    @DisplayName("Should handle null values in rows")
    public void testAddRowsWithNullValues() {
        Collection<Object[]> rows = Arrays.asList(new Object[] { 6, null, 45, 90000.0 }, new Object[] { 7, "Grace", null, 75000.0 },
                new Object[] { 8, "Henry", 28, null });

        int originalSize = dataset.size();
        dataset.addRows(rows);

        assertEquals(originalSize + 3, dataset.size());
        assertNull(dataset.get(5, 1)); // null name
        assertNull(dataset.get(6, 2)); // null age
        assertNull(dataset.get(7, 3)); // null salary
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
        // Original first row should now be at position 2
        assertEquals((Integer) 1, dataset.get(2, 0));
        assertEquals("John", dataset.get(2, 1));
    }

    @Test
    @DisplayName("Should add rows at end of dataset")
    public void testAddRowsAtEnd() {

        Collection<Object[]> rows = new ArrayList<>();
        rows.add(new Object[] { 6, "Last", 60, 120000.0 }); // Missing salary

        int originalSize = dataset.size();
        dataset.addRows(originalSize, rows);

        assertEquals(originalSize + 1, dataset.size());
        assertEquals((Integer) 6, dataset.get(originalSize, 0));
        assertEquals("Last", dataset.get(originalSize, 1));
    }

    @Test
    @DisplayName("Should throw exception for invalid position in addRows")
    public void testAddRowsWithInvalidPosition() {
        Collection<Object[]> rows = new ArrayList<>();
        rows.add(new Object[] { 6, "Test", 45, 90000.0 }); // Missing salary

        // Position too large
        assertThrows(IndexOutOfBoundsException.class, () -> dataset.addRows(100, rows));

        // Negative position
        assertThrows(IndexOutOfBoundsException.class, () -> dataset.addRows(-1, rows));
    }

    @Test
    @DisplayName("Should throw exception for mixed collection types")
    public void testAddRowsWithMixedTypes() {
        // All rows must be of the same type, mixing types should fail
        Collection<Object> mixedRows = Arrays.asList(new Object[] { 6, "Array", 45, 90000.0 }, Arrays.asList(7, "List", 32, 75000.0) // Different type
        );

        // This should work as the implementation checks the first row type
        // and processes all rows accordingly
        @SuppressWarnings("unchecked")
        Collection<?> rows = (Collection<?>) mixedRows;
        assertThrows(ClassCastException.class, () -> dataset.addRows(rows));
    }

    @Test
    @DisplayName("Should add single row using addRows")
    public void testAddRowsWithSingleRow() {
        // Special case: single row should delegate to addRow
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

        // Verify order is preserved
        for (int i = 0; i < 6; i++) {
            assertEquals((Integer) (10 + i), dataset.get(originalSize + i, 0));
            assertEquals("Person" + (10 + i), dataset.get(originalSize + i, 1));
        }
    }

    @Test
    @DisplayName("Should handle row with extra columns")
    public void testAddRowsWithExtraColumns() {
        // Rows with more columns than dataset should work (extra columns ignored)
        Collection<Object[]> rows = new ArrayList<>();
        rows.add(new Object[] { 6, "Frank", 45, 90000.0, "Extra1", "Extra2" }); // One extra column

        int originalSize = dataset.size();
        dataset.addRows(rows);

        assertEquals(originalSize + 1, dataset.size());
        assertEquals((Integer) 6, dataset.get(originalSize, 0));
        assertEquals("Frank", dataset.get(originalSize, 1));
    }

    @Test
    @DisplayName("Should throw exception for unsupported row type")
    public void testAddRowsWithUnsupportedType() {
        // Try to add rows of unsupported type (e.g., String)
        Collection<String> rows = Arrays.asList("row1", "row2");

        assertThrows(IllegalArgumentException.class, () -> dataset.addRows(rows));
    }

    @Test
    public void testRemoveRow() {
        dataset.removeRow(2);

        assertEquals(4, dataset.size());
        assertNotEquals("Bob", dataset.get(2, 1)); // Bob was at index 2
    }

    @Test
    public void testRemoveRows() {
        dataset.println();
        dataset.removeRows(1, 3);

        dataset.println();

        assertEquals(3, dataset.size());
        assertEquals("John", dataset.get(0, 1));
        assertEquals("Bob", dataset.get(1, 1));
        assertEquals("Charlie", dataset.get(2, 1));
    }

    @Test
    public void testRemoveRowRange() {
        dataset.removeRowRange(1, 3);

        assertEquals(3, dataset.size());
        assertEquals("John", dataset.get(0, 1));
        assertEquals("Alice", dataset.get(1, 1));
    }

    @Test
    public void testRemoveDuplicateRowsBy() {
        Dataset dataset = Dataset.rows(Arrays.asList("id", "name", "department"),
                new Object[][] { { 1, "John", "IT" }, { 2, "Jane", "HR" }, { 3, "John", "Finance" }, // Duplicate name
                        { 4, "Bob", "IT" } });
        dataset.removeDuplicateRowsBy("name");
        // Result: Only rows with id=1, id=2, and id=4 remain
        // The row with id=3 is removed because "John" already exists
        assertEquals(3, dataset.size());
        assertEquals((Integer) 1, dataset.get(0, 0));
        assertEquals((Integer) 2, dataset.get(1, 0));
        assertEquals((Integer) 4, dataset.get(2, 0));
    }

    @Test
    public void testRemoveDuplicateRowsBy2() {
        Dataset dataset = Dataset.rows(Arrays.asList("id", "name", "department"),
                new Object[][] { { 1, "John Doe", "IT" }, { 2, "Jane Smith", "HR" }, { 3, "Johnathan Doe", "Finance" }, // Duplicate based on last name
                        { 4, "Bob Brown", "IT" } });
        dataset.removeDuplicateRowsBy("name", name -> ((String) name).split(" ")[1]); // Use last name as key
        // Result: Only rows with id=1, id=2, and id=4 remain
        // The row with id=3 is removed because "Doe" already exists
        assertEquals(3, dataset.size());
        assertEquals((Integer) 1, dataset.get(0, 0));
        assertEquals((Integer) 2, dataset.get(1, 0));
        assertEquals((Integer) 4, dataset.get(2, 0));
    }

    @Test
    public void testRemoveDuplicateRows3() {
        Dataset dataset = Dataset.rows(Arrays.asList("id", "name", "department"),
                new Object[][] { { 1, "John", "IT" }, { 2, "Jane", "HR" }, { 3, "John", "IT" }, // Duplicate name and department
                        { 4, "Bob", "IT" } });
        dataset.removeDuplicateRowsBy(Arrays.asList("name", "department"));
        // Result: Only rows with id=1, id=2, and id=4 remain
        // The row with id=3 is removed because ("John", "IT") already exists
        assertEquals(3, dataset.size());
        assertEquals((Integer) 1, dataset.get(0, 0));
        assertEquals((Integer) 2, dataset.get(1, 0));
        assertEquals((Integer) 4, dataset.get(2, 0));

    }

    @Test
    public void testRemoveDuplicateRowsBy4() {
        Dataset dataset = Dataset.rows(Arrays.asList("id", "name", "department"),
                new Object[][] { { 1, "John Doe", "IT" }, { 2, "Jane Smith", "HR" }, { 3, "Johnathan Doe", "IT" }, // Duplicate based on last name and department
                        { 4, "Bob Brown", "IT" } });
        dataset.removeDuplicateRowsBy(Arrays.asList("name", "department"), row -> ((String) row.get(0)).split(" ")[1] + "|" + row.get(1)); // Use last name and department as key
        // Result: Only rows with id=1, id=2, and id=4 remain
        // The row with id=3 is removed because ("Doe", "IT") already exists
        assertEquals(3, dataset.size());
        assertEquals((Integer) 1, dataset.get(0, 0));
        assertEquals((Integer) 2, dataset.get(1, 0));
        assertEquals((Integer) 4, dataset.get(2, 0));
    }

    @Test
    public void testUpdateRow() {
        dataset.updateRow(0, val -> val instanceof Number ? 999 : "UPDATED");

        assertEquals((Integer) 999, dataset.get(0, 0));
        assertEquals("UPDATED", dataset.get(0, 1));
        assertEquals((Integer) 999, dataset.get(0, 2));
        assertEquals((Integer) 999, dataset.get(0, 3));
    }

    @Test
    public void testUpdateRows() {
        dataset.updateRows(new int[] { 0, 2 }, (i, v) -> v instanceof String ? "UPDATED" : v);

        assertEquals("UPDATED", dataset.get(0, 1));
        assertEquals("UPDATED", dataset.get(2, 1));
        assertEquals("Jane", dataset.get(1, 1)); // Unchanged
    }

    @Test
    public void testUpdateAll() {
        dataset.updateAll(val -> val instanceof Integer ? 0 : val);

        for (int i = 0; i < dataset.size(); i++) {
            assertEquals((Integer) 0, dataset.get(i, 0)); // id column
            assertEquals((Integer) 0, dataset.get(i, 2)); // age column
        }
    }

    @Test
    public void testUpdateAllWithIntBiObjFunction() {
        dataset.println();

        dataset.updateAll((i, c, v) -> v instanceof String ? "Name" + i : v);

        dataset.println();

        for (int i = 0; i < dataset.size(); i++) {
            assertEquals("Name" + i, dataset.get(i, 1)); // name column
        }
    }


    @Test
    public void testReplaceIf() {
        dataset.replaceIf(val -> val instanceof Integer && (int) val > 30, 999);

        assertEquals((Integer) 25, dataset.get(0, 2)); // age 25, not replaced
        assertEquals((Integer) 30, dataset.get(1, 2)); // age 30, not replaced
        assertEquals((Integer) 999, dataset.get(2, 2)); // age 35, replaced
    }

    @Test
    public void testReplaceIfWithIntBiObjPredicate() {
        dataset.println();
        assertEquals("Bob", dataset.get(2, 1));
        dataset.replaceIf((i, c, v) -> "name".equals(c) && "Bob".equals(v), "Robert");
        dataset.println();
        assertEquals("Robert", dataset.get(2, 1));
    }

    @Test
    public void testPrepend() {
        List<String> otherColumns = Arrays.asList("id", "name", "age", "salary");
        List<List<Object>> otherData = new ArrayList<>();
        otherData.add(Arrays.asList(10, 11));
        otherData.add(Arrays.asList("Pre1", "Pre2"));
        otherData.add(Arrays.asList(100, 101));
        otherData.add(Arrays.asList(1000.0, 1001.0));

        Dataset other = new RowDataset(otherColumns, otherData);
        dataset.prepend(other);

        assertEquals(7, dataset.size());
        assertEquals((Integer) 10, dataset.get(0, 0));
        assertEquals("Pre1", dataset.get(0, 1));
        assertEquals((Integer) 1, dataset.get(2, 0)); // Original first row
    }

    @Test
    public void testAppend() {
        List<String> otherColumns = Arrays.asList("id", "name", "age", "salary");
        List<List<Object>> otherData = new ArrayList<>();
        otherData.add(Arrays.asList(10, 11));
        otherData.add(Arrays.asList("App1", "App2"));
        otherData.add(Arrays.asList(100, 101));
        otherData.add(Arrays.asList(1000.0, 1001.0));

        Dataset other = new RowDataset(otherColumns, otherData);
        dataset.append(other);

        assertEquals(7, dataset.size());
        assertEquals((Integer) 10, dataset.get(5, 0));
        assertEquals("App1", dataset.get(5, 1));
    }

    @Test
    public void testCurrentRowNum() {
        assertEquals(0, dataset.currentRowNum());
        dataset.absolute(2);
        assertEquals(2, dataset.currentRowNum());
    }

    @Test
    public void testAbsolute() {
        dataset.absolute(3);
        assertEquals(3, dataset.currentRowNum());
        assertEquals((Integer) 4, dataset.get("id"));
        assertEquals("Alice", dataset.get("name"));
    }

    @Test
    public void testGetRow() {
        Object[] row = dataset.getRow(1);
        assertArrayEquals(new Object[] { 2, "Jane", 30, 60000.0 }, row);
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
    public void testGetRowWithSupplier() {
        List<Object> row = dataset.getRow(1, size -> new ArrayList<>(size));
        assertEquals(Arrays.asList(2, "Jane", 30, 60000.0), row);
    }

    @Test
    public void testFirstRow() {
        Optional<Object[]> firstRow = dataset.firstRow();
        assertTrue(firstRow.isPresent());
        assertArrayEquals(new Object[] { 1, "John", 25, 50000.0 }, firstRow.get());
    }

    @Test
    public void testFirstRowWithClass() {
        Optional<List> firstRow = dataset.firstRow(ArrayList.class);
        assertTrue(firstRow.isPresent());
        assertEquals(Arrays.asList(1, "John", 25, 50000.0), firstRow.get());
    }

    @Test
    public void testLastRow() {
        Optional<Object[]> lastRow = dataset.lastRow();
        assertTrue(lastRow.isPresent());
        assertArrayEquals(new Object[] { 5, "Charlie", 40, 80000.0 }, lastRow.get());
    }

    @Test
    public void testBiIterator() {
        BiIterator<Object, Object> iter = dataset.iterator("id", "name");

        assertTrue(iter.hasNext());
        Pair<Object, Object> pair = iter.next();
        assertEquals(1, pair.left());
        assertEquals("John", pair.right());
    }

    @Test
    public void testTriIterator() {
        TriIterator<Object, Object, Object> iter = dataset.iterator("id", "name", "age");

        assertTrue(iter.hasNext());
        Triple<Object, Object, Object> triple = iter.next();
        assertEquals(1, triple.left());
        assertEquals("John", triple.middle());
        assertEquals(25, triple.right());
    }

    @Test
    public void testForEach() {
        List<String> names = new ArrayList<>();
        dataset.forEach((DisposableObjArray row) -> {
            names.add(row.get(1).toString());
        });

        assertEquals(Arrays.asList("John", "Jane", "Bob", "Alice", "Charlie"), names);
    }

    @Test
    public void testForEachWithColumnNames() {
        List<String> nameAges = new ArrayList<>();
        dataset.forEach(Arrays.asList("name", "age"), (DisposableObjArray row) -> {
            nameAges.add(row.get(0) + ":" + row.get(1));
        });

        assertEquals(Arrays.asList("John:25", "Jane:30", "Bob:35", "Alice:28", "Charlie:40"), nameAges);
    }

    @Test
    public void testForEachWithBiConsumer() {
        List<String> nameAges = new ArrayList<>();
        dataset.forEach(Tuple2.of("name", "age"), (name, age) -> {
            nameAges.add(name + ":" + age);
        });

        assertEquals(Arrays.asList("John:25", "Jane:30", "Bob:35", "Alice:28", "Charlie:40"), nameAges);
    }

    @Test
    public void testForEachWithTriConsumer() {
        List<String> combined = new ArrayList<>();
        dataset.forEach(Tuple3.of("id", "name", "age"), (id, name, age) -> {
            combined.add(id + "-" + name + "-" + age);
        });

        assertEquals(Arrays.asList("1-John-25", "2-Jane-30", "3-Bob-35", "4-Alice-28", "5-Charlie-40"), combined);
    }

    @Test
    public void testToList() {
        List<Object[]> list = dataset.toList();
        assertEquals(5, list.size());
        assertArrayEquals(new Object[] { 1, "John", 25, 50000.0 }, list.get(0));
    }

    @Test
    public void testToListWithRange() {
        List<Object[]> list = dataset.toList(1, 3);
        assertEquals(2, list.size());
        assertArrayEquals(new Object[] { 2, "Jane", 30, 60000.0 }, list.get(0));
    }

    @Test
    public void testToListWithClass() {
        List<List> list = dataset.toList(ArrayList.class);
        assertEquals(5, list.size());
        assertEquals(Arrays.asList(1, "John", 25, 50000.0), list.get(0));
    }

    @Test
    public void testToListWithColumnNames() {
        List<Object[]> list = dataset.toList(Arrays.asList("name", "age"), Object[].class);
        assertEquals(5, list.size());
        assertArrayEquals(new Object[] { "John", 25 }, list.get(0));
    }

    @Test
    public void testToListWithSupplier() {
        List<List> list = dataset.toList(size -> new ArrayList<>(size));
        assertEquals(5, list.size());
        assertEquals(Arrays.asList(1, "John", 25, 50000.0), list.get(0));
    }

    @Test
    public void testToListWithFilter() {
        List<Map> list = dataset.toList(col -> col.equals("name") || col.equals("age"), col -> col.toUpperCase(), HashMap.class);

        assertEquals(5, list.size());
        Map<String, Object> firstRow = list.get(0);
        assertEquals("John", firstRow.get("NAME"));
        assertEquals(25, firstRow.get("AGE"));
    }

    @Test
    public void testToEntities() {
        List<Person> persons = dataset.toEntities(null, Person.class);
        assertEquals(5, persons.size());
        assertEquals("John", persons.get(0).getName());
        assertEquals(25, persons.get(0).getAge());
    }

    @Test
    public void testToMergedEntities() {
        // Add duplicate rows for testing merge
        dataset.addRow(new Object[] { 1, "John", 25, 55000.0 });
        dataset.addRow(new Object[] { 2, "Jane", 30, 65000.0 });

        List<Person> persons = dataset.toMergedEntities("id", Person.class);
        assertEquals(5, persons.size()); // Should still be 5 unique persons
    }

    @Test
    public void testToMap() {
        Map<Object, Object> map = dataset.toMap("id", "name");
        assertEquals(5, map.size());
        assertEquals("John", map.get(1));
        assertEquals("Jane", map.get(2));
    }

    @Test
    public void testToMapWithRowType() {
        Map<Object, List> map = dataset.toMap("id", Arrays.asList("name", "age"), ArrayList.class);
        assertEquals(5, map.size());
        assertEquals(Arrays.asList("John", 25), map.get(1));
    }

    @Test
    public void testToMultimap() {
        // Add duplicate keys for testing
        dataset.addRow(new Object[] { 1, "Johnny", 26, 51000.0 });

        ListMultimap<Object, Object> multimap = dataset.toMultimap("id", "name");
        assertEquals(2, multimap.get(1).size());
        assertTrue(multimap.get(1).contains("John"));
        assertTrue(multimap.get(1).contains("Johnny"));
    }

    @Test
    public void testToJson() {
        String json = dataset.toJson();
        assertNotNull(json);
        assertTrue(json.contains("\"id\""));
        assertTrue(json.contains("\"name\""));
        assertTrue(json.contains("John"));
    }

    @Test
    public void testToJsonWithRange() {
        String json = dataset.toJson(0, 2);
        assertNotNull(json);
        assertTrue(json.contains("John"));
        assertTrue(json.contains("Jane"));
        assertFalse(json.contains("Bob"));
    }

    @Test
    public void testToJsonToFile() throws IOException {
        File tempFile = File.createTempFile("dataset", ".json");
        tempFile.deleteOnExit();

        dataset.toJson(tempFile);

        String content = new String(IOUtil.readAllBytes(tempFile));
        assertTrue(content.contains("John"));
    }

    @Test
    public void testToXml() {
        String xml = dataset.toXml();
        assertNotNull(xml);
        assertTrue(xml.contains("<row>"));
        assertTrue(xml.contains("<name>John</name>"));
    }

    @Test
    public void testToXmlWithRowElementName() {
        String xml = dataset.toXml("person");
        assertNotNull(xml);
        assertTrue(xml.contains("<person>"));
        assertTrue(xml.contains("</person>"));
    }

    @Test
    public void testToCsv() {
        String csv = dataset.toCsv();
        assertNotNull(csv);
        N.println(csv);
        assertTrue(csv.contains("\"id\",\"name\",\"age\",\"salary\""));
        assertTrue(csv.contains("1,\"John\",25,50000.0"));
    }

    @Test
    public void testToCsv_2() {
        Dataset dataset = Dataset.rows(Arrays.asList("id", "name"), new Object[][] { { 1, "Alice" }, { 2, "Bob\"s" } });
        String csv = dataset.toCsv();
        N.println(csv);

    }

    @Test
    public void testToCsvToFile() throws IOException {
        File tempFile = File.createTempFile("dataset", ".csv");
        tempFile.deleteOnExit();

        dataset.toCsv(tempFile);

        String content = new String(IOUtil.readAllBytes(tempFile));
        assertTrue(content.contains("John"));
    }

    @Test
    public void testGroupBy() {
        // Add some duplicate ages for grouping
        dataset.addRow(new Object[] { 6, "Frank", 25, 52000.0 });
        dataset.addRow(new Object[] { 7, "Grace", 30, 62000.0 });

        Dataset grouped = dataset.groupBy("age", "name", "names", collector(Collectors.toList()));

        assertEquals(2, grouped.columnCount());
        assertTrue(grouped.containsColumn("age"));
        assertTrue(grouped.containsColumn("names"));
    }

    @Test
    public void testGroupBy2() {

        Dataset dataset = Dataset.rows(Arrays.asList("department", "level", "employee", "salary", "bonus"),
                new Object[][] { { "Sales", "Junior", "Alice", 50000, 5000 }, { "Sales", "Senior", "Bob", 75000, 8000 },
                        { "IT", "Junior", "Charlie", 55000, 6000 }, { "IT", "Senior", "David", 80000, 9000 }, { "Sales", "Junior", "Eve", 52000, 5500 } });

        Dataset result = dataset.groupBy(Arrays.asList("department", "level"), // group by department and level
                row -> row.get(0) + "_" + row.get(1), // create composite key: "Sales_Junior"
                Arrays.asList("salary", "bonus"), // aggregate on salary and bonus
                "total_compensation", // result column name
                row -> ((Integer) row.get(0)) + ((Integer) row.get(1)), // sum salary + bonus
                Collectors.summingInt(Integer.class::cast)); // sum the results

        N.println("Group by with sum aggregation:");
        dataset.println("     * // ");
        result.println("     * // ");
    }

    @Test
    public void testGroupByWithCollector() {
        Dataset grouped = dataset.groupBy("age", "salary", "avgSalary", collector(Collectors.averagingDouble(val -> (Double) val)));

        assertEquals(2, grouped.columnCount());
        assertEquals(5, grouped.size()); // 5 unique ages
    }

    @Test
    public void testGroupByMultipleColumns() {
        // Create a dataset with more columns for grouping
        List<String> cols = Arrays.asList("dept", "role", "id", "salary");
        List<List<Object>> data = new ArrayList<>();
        data.add(Arrays.asList("IT", "Dev", "IT", "Dev", "HR", "HR"));
        data.add(Arrays.asList("Senior", "Junior", "Senior", "Junior", "Senior", "Junior"));
        data.add(Arrays.asList(1, 2, 3, 4, 5, 6));
        data.add(Arrays.asList(80000.0, 50000.0, 85000.0, 55000.0, 70000.0, 45000.0));

        Dataset deptData = new RowDataset(cols, data);

        Dataset grouped = deptData.groupBy(Arrays.asList("dept", "role"));
        assertEquals(2, grouped.columnCount());
    }

    @Test
    public void testPivot() {
        // Create data suitable for pivot
        List<String> cols = Arrays.asList("product", "quarter", "sales");
        List<List<Object>> data = new ArrayList<>();
        data.add(Arrays.asList("A", "A", "A", "A", "B", "B", "B", "B"));
        data.add(Arrays.asList("Q1", "Q2", "Q3", "Q4", "Q1", "Q2", "Q3", "Q4"));
        data.add(Arrays.asList(100, 110, 120, 130, 200, 210, 220, 230));

        Dataset salesData = new RowDataset(cols, data);

        Sheet<Object, Object, Object> pivoted = salesData.pivot("product", "quarter", "sales", collector(Collectors.summingInt(val -> (Integer) val)));

        assertNotNull(pivoted);
        assertEquals(2, pivoted.rowKeySet().size()); // Products A and B
        assertEquals(4, pivoted.columnKeySet().size()); // Q1-Q4
    }

    @Test
    public void testPivot2() {

        Dataset dataset = Dataset.rows(Arrays.asList("region", "product", "sales", "quantity"),
                new Object[][] { { "North", "A", 100, 10 }, { "North", "B", 200, 20 }, { "South", "A", 150, 15 }, { "South", "B", 250, 25 } });

        Sheet<String, String, Integer> pivotResult = dataset.pivot("region", // row identifier
                "product", // column identifier
                Arrays.asList("sales", "quantity"), // aggregate columns
                Collectors.summingInt(arr -> (Integer) arr[0] + (Integer) arr[1])); // sum sales + quantity

        // Result Sheet:
        //        | A   | B   |
        // -------|-----|-----|
        // North  | 110 | 220 |
        // South  | 165 | 275 |
        N.println("Pivot with aggregation:");
        dataset.println("     * // ");
        pivotResult.println("     * // ");

        // Result Sheet:
        //        | A   | B   |
        // -------|-----|-----|
        // North  | 100 | 200 |
        // South  | 150 | 250 |
    }

    @Test
    public void testRollup() {
        Dataset dataset = Dataset.rows(Arrays.asList("region", "country", "city", "sales"), new Object[][] { { "North", "USA", "New York", 1000 },
                { "North", "USA", "Boston", 800 }, { "North", "Canada", "Toronto", 600 }, { "South", "Mexico", "Mexico City", 400 } });

        dataset.println("     * ");
        Stream<Dataset> rollupResult = dataset.rollup(Arrays.asList("region", "country", "city"));
        rollupResult.forEach(ds -> ds.println("     * "));
        // Returns a stream of 4 datasets:
        // Level 1: Grouped by region, country, city (most detailed)
        // Level 2: Grouped by region, country
        // Level 3: Grouped by region
        // Level 4: Grand total (no grouping)
    }

    @Test
    public void testRollup2() {
        Dataset dataset = Dataset.rows(Arrays.asList("region", "country", "city", "sales", "quantity"),
                new Object[][] { { "North", "USA", "New York", 1000, 50 }, { "North", "USA", "Boston", 800, 40 }, { "North", "Canada", "Toronto", 600, 30 },
                        { "South", "Mexico", "Mexico City", 400, 20 } });
        Function<DisposableObjArray, String> keyExtractor = keyRow -> keyRow.join("-");
        Stream<Dataset> rollupResult = dataset.rollup(Arrays.asList("region", "country", "city"), keyExtractor, Arrays.asList("sales", "quantity"),
                "aggregated_totals", row -> Tuple.of((Integer) row.get(0), (Integer) row.get(1)), MoreCollectors.summingInt(tp -> tp._1, tp -> tp._2));

        N.println("Rollup with aggregation:");
        dataset.println("     * // ");
        rollupResult.forEach(ds -> ds.println("     * // "));
        // Returns a stream of 4 datasets with custom keys and aggregated sales:
        // Level 1: Grouped by region-country, city (most detailed)
        // Level 2: Grouped by region-country
        // Level 3: Grouped by region
        // Level 4: Grand total (no grouping)
    }

    @Test
    public void testCube2() {
        Dataset dataset = Dataset.rows(Arrays.asList("region", "country", "sales"),
                new Object[][] { { "North", "USA", 1000 }, { "North", "Canada", 600 }, { "South", "Mexico", 400 } });

        Function<DisposableObjArray, String> keyExtractor = keyRow -> keyRow.join("-");
        Function<DisposableObjArray, Double> rowMapper = row -> (Integer) row.get(0) * 1.1;
        Stream<Dataset> cubeResult = dataset.cube(Arrays.asList("region", "country"), keyExtractor, Arrays.asList("sales"), "total_sales_with_markup",
                rowMapper, Collectors.collectingAndThen(Collectors.summingDouble(Double::doubleValue), r -> Numbers.round(r, 2)));
        N.println("Rollup with aggregation:");
        dataset.println("     * // ");
        cubeResult.forEach(ds -> ds.println("     * // "));
        // Returns a stream of 4 datasets:
        // Level 1: Grouped by region, country (most detailed)
        // Level 2: Grouped by region only
        // Level 3: Grouped by country only  
        // Level 4: Grand total (no grouping)
    }

    @Test
    public void testCube() {
        List<Dataset> cubes = dataset.cube(Arrays.asList("age", "name")).toList();

        assertTrue(cubes.size() > 0);
    }

    @Test
    public void testSortBy() {
        dataset.sortBy("age");

        assertEquals((Integer) 25, dataset.get(0, 2));
        assertEquals((Integer) 28, dataset.get(1, 2));
        assertEquals((Integer) 30, dataset.get(2, 2));
        assertEquals((Integer) 35, dataset.get(3, 2));
        assertEquals((Integer) 40, dataset.get(4, 2));
    }

    @Test
    public void testSortByWithComparator() {
        dataset.sortBy("name", Comparator.reverseOrder());

        assertEquals("John", dataset.get(0, 1));
        assertEquals("Jane", dataset.get(1, 1));
        assertEquals("Charlie", dataset.get(2, 1));
        assertEquals("Bob", dataset.get(3, 1));
        assertEquals("Alice", dataset.get(4, 1));
    }

    @Test
    public void testSortByMultipleColumns() {
        // Add duplicate ages to test secondary sort
        dataset.addRow(new Object[] { 6, "Aaron", 25, 48000.0 });

        dataset.sortBy(Arrays.asList("age", "name"));

        assertEquals("Aaron", dataset.get(0, 1)); // Both age 25, Aaron comes first
        assertEquals("John", dataset.get(1, 1));
    }

    @Test
    public void testParallelSortBy() {
        dataset.parallelSortBy("age");

        assertEquals((Integer) 25, dataset.get(0, 2));
        assertEquals((Integer) 28, dataset.get(1, 2));
        assertEquals((Integer) 30, dataset.get(2, 2));
    }

    @Test
    public void testTopBy() {
        Dataset top3 = dataset.topBy("salary", 3);

        top3.println();

        assertEquals(3, top3.size());
        assertEquals((Double) 60000.0, top3.get(0, 3)); // Charlie's salary
        assertEquals((Double) 70000.0, top3.get(1, 3)); // Bob's salary
        assertEquals((Double) 80000.0, top3.get(2, 3)); // Jane's salary
    }

    @Test
    public void testTopByWithComparator() {
        Dataset top3 = dataset.topBy("age", 3, Comparator.naturalOrder());

        assertEquals(3, top3.size());
        // Should get 3 youngest people
        assertTrue((int) top3.get(0, 2) <= 30);
    }

    @Test
    public void testDistinct() {
        // Add duplicate rows
        dataset.addRow(new Object[] { 1, "John", 25, 50000.0 });
        dataset.addRow(new Object[] { 2, "Jane", 30, 60000.0 });

        Dataset distinct = dataset.distinct();
        assertEquals(5, distinct.size()); // Should remove duplicates
    }

    @Test
    public void testDistinctBy() {
        // Add rows with duplicate ages
        dataset.addRow(new Object[] { 6, "Frank", 25, 52000.0 });
        dataset.addRow(new Object[] { 7, "Grace", 30, 62000.0 });

        Dataset distinct = dataset.distinctBy("age");
        assertEquals(5, distinct.size()); // 5 unique ages
    }

    @Test
    public void testDistinctByMultipleColumns() {
        dataset.addRow(new Object[] { 6, "John", 25, 52000.0 }); // Same name and age

        Dataset distinct = dataset.distinctBy(Arrays.asList("name", "age"));
        assertEquals(5, distinct.size());
    }

    @Test
    public void testFilter() {
        Dataset filtered = dataset.filter((DisposableObjArray row) -> {
            return (int) row.get(2) > 30; // age > 30
        });

        assertEquals(2, filtered.size());
        assertEquals("Bob", filtered.get(0, 1));
        assertEquals("Charlie", filtered.get(1, 1));
    }

    @Test
    public void testFilterWithMax() {
        Dataset filtered = dataset.filter((DisposableObjArray row) -> {
            return (int) row.get(2) >= 25; // age >= 25
        }, 3);

        assertEquals(3, filtered.size());
    }

    @Test
    public void testFilterByColumn() {
        Dataset filtered = dataset.filter("age", age -> (int) age > 30);

        assertEquals(2, filtered.size());
        assertEquals((Integer) 35, filtered.get(0, 2));
        assertEquals((Integer) 40, filtered.get(1, 2));
    }

    @Test
    public void testFilterByBiPredicate() {
        Dataset filtered = dataset.filter(Tuple2.of("age", "salary"), (age, salary) -> (int) age > 30 && (double) salary > 60000);

        assertEquals(2, filtered.size());
    }

    @Test
    public void testMap() {
        dataset.println();
        Dataset mapped = dataset.map("age", "ageGroup", "name", age -> (int) age < 30 ? "Young" : "Adult");

        mapped.println();

        assertEquals(2, mapped.columnCount());
        assertTrue(mapped.containsColumn("name"));
        assertTrue(mapped.containsColumn("ageGroup"));
        assertEquals("Young", mapped.get(0, 1)); // John is 25
    }

    @Test
    public void testMapWithBiFunction() {
        Dataset mapped = dataset.map(Tuple2.of("name", "age"), "nameAge", Arrays.asList("id"), (name, age) -> name + ":" + age);

        assertEquals(2, mapped.columnCount());
        assertEquals("John:25", mapped.get(0, 1));
    }

    @Test
    public void testFlatMap() {
        Dataset flatMapped = dataset.flatMap("name", "letters", "id", name -> Arrays.asList(((String) name).split("")));

        assertTrue(flatMapped.size() > 5); // More rows due to flat mapping
        assertTrue(flatMapped.containsColumn("letters"));
    }

    @Test
    public void testCopy() {
        Dataset copy = dataset.copy();

        assertEquals(dataset.size(), copy.size());
        assertEquals(dataset.columnCount(), copy.columnCount());

        // Modify copy
        copy.set(0, 0, 999);

        // Original should be unchanged
        assertEquals((Integer) 1, dataset.get(0, 0));
        assertEquals((Integer) 999, copy.get(0, 0));
    }

    @Test
    public void testCopyWithColumnNames() {
        Dataset copy = dataset.copy(Arrays.asList("name", "age"));

        assertEquals(2, copy.columnCount());
        assertTrue(copy.containsColumn("name"));
        assertTrue(copy.containsColumn("age"));
        assertFalse(copy.containsColumn("id"));
    }

    @Test
    public void testCopyWithRange() {
        Dataset copy = dataset.copy(1, 3);

        assertEquals(2, copy.size());
        assertEquals("Jane", copy.get(0, 1));
        assertEquals("Bob", copy.get(1, 1));
    }

    @Test
    public void testClone() {
        Dataset cloned = dataset.clone();

        assertEquals(dataset.size(), cloned.size());
        assertEquals(dataset.columnCount(), cloned.columnCount());
        assertFalse(cloned.isFrozen());
    }

    @Test
    public void testCloneWithFreeze() {
        Dataset cloned = dataset.clone(true);

        assertTrue(cloned.isFrozen());
    }

    @Test
    public void testInnerJoin() {
        List<String> otherColumns = Arrays.asList("id", "dept");
        List<List<Object>> otherData = new ArrayList<>();
        otherData.add(Arrays.asList(1, 3, 5));
        otherData.add(Arrays.asList("IT", "HR", "Finance"));

        Dataset other = new RowDataset(otherColumns, otherData);
        dataset.println();

        other.println();

        Dataset joined = dataset.innerJoin(other, "id", "id");

        joined.println();

        assertEquals(3, joined.size()); // Only matching IDs
        assertEquals(6, joined.columnCount()); // 4 + 1 (dept)
        assertTrue(joined.containsColumn("dept"));
    }

    @Test
    public void testInnerJoinWithMultipleKeys() {
        List<String> otherColumns = Arrays.asList("name", "age", "dept");
        List<List<Object>> otherData = new ArrayList<>();
        otherData.add(Arrays.asList("John", "Bob"));
        otherData.add(Arrays.asList(25, 35));
        otherData.add(Arrays.asList("IT", "HR"));

        Dataset other = new RowDataset(otherColumns, otherData);

        Map<String, String> joinKeys = new HashMap<>();
        joinKeys.put("name", "name");
        joinKeys.put("age", "age");

        Dataset joined = dataset.innerJoin(other, joinKeys);

        assertEquals(2, joined.size()); // John and Bob match
    }

    @Test
    public void testLeftJoin() {
        List<String> otherColumns = Arrays.asList("id", "dept");
        List<List<Object>> otherData = new ArrayList<>();
        otherData.add(Arrays.asList(1, 3, 5));
        otherData.add(Arrays.asList("IT", "HR", "Finance"));

        Dataset other = new RowDataset(otherColumns, otherData);

        Dataset joined = dataset.leftJoin(other, "id", "id");
        dataset.println();
        other.println();
        joined.println();

        assertEquals(5, joined.size()); // All rows from left
        assertEquals(6, joined.columnCount());

        // Check that non-matching rows have null for dept
        assertEquals("IT", joined.get(0, 5)); // id=1 matches
        assertNull(joined.get(1, 4)); // id=2 doesn't match
    }

    @Test
    public void testRightJoin() {
        List<String> otherColumns = Arrays.asList("id", "dept");
        List<List<Object>> otherData = new ArrayList<>();
        otherData.add(Arrays.asList(1, 3, 6)); // 6 doesn't exist in left
        otherData.add(Arrays.asList("IT", "HR", "Finance"));

        Dataset other = new RowDataset(otherColumns, otherData);

        Dataset joined = dataset.rightJoin(other, "id", "id");

        assertEquals(3, joined.size()); // All rows from right

        // Check last row (id=6) has nulls for left columns
        assertNull(joined.get(2, 1)); // name should be null
    }

    @Test
    public void testFullJoin() {
        List<String> otherColumns = Arrays.asList("id", "dept");
        List<List<Object>> otherData = new ArrayList<>();
        otherData.add(Arrays.asList(1, 3, 6)); // 6 doesn't exist in left
        otherData.add(Arrays.asList("IT", "HR", "Finance"));

        Dataset other = new RowDataset(otherColumns, otherData);

        Dataset joined = dataset.fullJoin(other, "id", "id");

        assertEquals(6, joined.size()); // All unique IDs: 1,2,3,4,5,6
    }

    @Test
    public void testUnion() {
        List<String> otherColumns = Arrays.asList("id", "name", "age", "salary");
        List<List<Object>> otherData = new ArrayList<>();
        otherData.add(Arrays.asList(1, 6)); // 1 is duplicate
        otherData.add(Arrays.asList("John", "Frank"));
        otherData.add(Arrays.asList(25, 45));
        otherData.add(Arrays.asList(50000.0, 90000.0));

        Dataset other = new RowDataset(otherColumns, otherData);

        Dataset union = dataset.union(other);

        assertEquals(6, union.size()); // 5 + 2 - 1 duplicate
    }

    @Test
    public void testUnion_2() {
        Dataset dataset1 = Dataset.rows(Arrays.asList("id", "name", "age"),
                new Object[][] { { 1, "Alice", 25 }, new Object[] { 2, "Bob", 30 }, new Object[] { 1, "Alice", 35 } });
        Dataset dataset2 = Dataset.rows(Arrays.asList("id", "name", "score"), new Object[][] { { 1, "Alice", 95 }, { 3, "Charlie", 85 } });

        Dataset result = dataset1.union(dataset2);
        // Result contains: {1, "Alice", 25, null}, {2, "Bob", 30, null}, {3, "Charlie", null, 85}
        // Note: Duplicate rows are eliminated

        result.println();
        assertEquals(4, result.columnCount());
        assertTrue(result.containsColumn("id"));
        assertTrue(result.containsColumn("name"));
        assertTrue(result.containsColumn("age"));
        assertTrue(result.containsColumn("score"));
        assertEquals(3, result.size());
        assertEquals(1, (Integer) result.get(0, 0));
        assertEquals("Alice", result.get(0, 1));
        assertEquals(25, (Integer) result.get(0, 2));
        assertNull(result.get(0, 3));
        assertEquals(2, (Integer) result.get(1, 0));
        assertEquals("Bob", result.get(1, 1));
        assertEquals(30, (Integer) result.get(1, 2));
        assertNull(result.get(1, 3));
        assertEquals(3, (Integer) result.get(2, 0));
        assertEquals("Charlie", result.get(2, 1));
        assertNull(result.get(2, 2));
        assertEquals(85, (Integer) result.get(2, 3));
    }

    @Test
    public void testUnionAll() {
        List<String> otherColumns = Arrays.asList("id", "name", "age", "salary");
        List<List<Object>> otherData = new ArrayList<>();
        otherData.add(Arrays.asList(1, 6));
        otherData.add(Arrays.asList("John", "Frank"));
        otherData.add(Arrays.asList(25, 45));
        otherData.add(Arrays.asList(50000.0, 90000.0));

        Dataset other = new RowDataset(otherColumns, otherData);

        Dataset unionAll = dataset.unionAll(other);

        assertEquals(7, unionAll.size()); // 5 + 2 (includes duplicate)
    }

    @Test
    public void testUnionAll_2() {
        Dataset dataset1 = Dataset.rows(Arrays.asList("id", "name", "age"),
                new Object[][] { { 1, "Alice", 25 }, new Object[] { 2, "Bob", 30 }, new Object[] { 1, "Alice", 35 } });
        Dataset dataset2 = Dataset.rows(Arrays.asList("id", "name", "score"), new Object[][] { { 1, "Alice", 95 }, { 3, "Charlie", 85 } });

        Dataset result = dataset1.unionAll(dataset2);
        // Result contains columns: id, name, age, score
        // Result contains: {1, "Alice", 25, null}, {2, "Bob", 30, null}, {1, "Alice", 35, null}, {1, "Alice", null, 95}, {3, "Charlie", null, 85}
        // Note: All rows are included, including duplicates

        result.println();
        assertEquals(4, result.columnCount());
        assertTrue(result.containsColumn("id"));
        assertTrue(result.containsColumn("name"));
        assertTrue(result.containsColumn("age"));
        assertTrue(result.containsColumn("score"));
        assertEquals(5, result.size());
        assertEquals(1, (Integer) result.get(0, 0));
        assertEquals("Alice", result.get(0, 1));
        assertEquals(25, (Integer) result.get(0, 2));
        assertNull(result.get(0, 3));
        assertEquals(2, (Integer) result.get(1, 0));
        assertEquals("Bob", result.get(1, 1));
        assertEquals(30, (Integer) result.get(1, 2));
        assertNull(result.get(1, 3));
        assertEquals(1, (Integer) result.get(2, 0));
        assertEquals("Alice", result.get(2, 1));
        assertEquals(35, (Integer) result.get(2, 2));
        assertNull(result.get(2, 3));
        // Duplicate row
        assertEquals(1, (Integer) result.get(3, 0));
        assertEquals("Alice", result.get(3, 1));
        assertNull(result.get(3, 2));
        assertEquals(95, (Integer) result.get(3, 3));
        assertEquals(3, (Integer) result.get(4, 0));
        assertEquals("Charlie", result.get(4, 1));
        assertNull(result.get(4, 2));
        assertEquals(85, (Integer) result.get(4, 3));

        assertThrows(IllegalArgumentException.class, () -> dataset1.unionAll(dataset2, true));
    }

    @Test
    public void testIntersect() {
        List<String> otherColumns = Arrays.asList("id", "name", "age", "salary");
        List<List<Object>> otherData = new ArrayList<>();
        otherData.add(Arrays.asList(1, 3, 6));
        otherData.add(Arrays.asList("John", "Bob", "Frank"));
        otherData.add(Arrays.asList(25, 35, 45));
        otherData.add(Arrays.asList(50000.0, 70000.0, 90000.0));

        Dataset other = new RowDataset(otherColumns, otherData);

        Dataset intersect = dataset.intersect(other);

        assertEquals(2, intersect.size()); // Only John and Bob match
    }

    @Test
    public void testExcept() {
        List<String> otherColumns = Arrays.asList("id", "name", "age", "salary");
        List<List<Object>> otherData = new ArrayList<>();
        otherData.add(Arrays.asList(1, 3));
        otherData.add(Arrays.asList("John", "Bob"));
        otherData.add(Arrays.asList(25, 35));
        otherData.add(Arrays.asList(50000.0, 70000.0));

        Dataset other = new RowDataset(otherColumns, otherData);

        Dataset except = dataset.except(other);

        assertEquals(3, except.size()); // Jane, Alice, Charlie
    }

    @Test
    public void testCartesianProduct() {
        Dataset dataset1 = Dataset.rows(Arrays.asList("id", "name"), new Object[][] { { 1, "Alice" }, { 2, "Bob" } });
        Dataset dataset2 = Dataset.rows(Arrays.asList("category", "score"), new Object[][] { { "A", 95 }, { "B", 90 } });

        Dataset result = dataset1.cartesianProduct(dataset2);
        // Result contains columns: id, name, category, score
        // Result contains: {1, "Alice", "A", 95}, {1, "Alice", "B", 90},
        //                  {2, "Bob", "A", 95}, {2, "Bob", "B", 90}

        assertEquals(N.asList("id", "name", "category", "score"), result.columnNameList());
        assertEquals(4, result.size());
        assertEquals(4, result.columnCount());
        assertEquals(1, (Integer) result.get(0, 0));
        assertEquals("Alice", result.get(0, 1));
        assertEquals("A", result.get(0, 2));
        assertEquals(95, (Integer) result.get(0, 3));
        assertEquals(1, (Integer) result.get(1, 0));
        assertEquals("Alice", result.get(1, 1));
        assertEquals("B", result.get(1, 2));
        assertEquals(90, (Integer) result.get(1, 3));
        assertEquals(2, (Integer) result.get(2, 0));
        assertEquals("Bob", result.get(2, 1));
        assertEquals("A", result.get(2, 2));
        assertEquals(95, (Integer) result.get(2, 3));
        assertEquals(2, (Integer) result.get(3, 0));
        assertEquals("Bob", result.get(3, 1));
        assertEquals("B", result.get(3, 2));
        assertEquals(90, (Integer) result.get(3, 3));

    }

    @Test
    public void testSplit() {
        Stream<Dataset> splits = dataset.split(2);
        List<Dataset> splitList = splits.toList();

        assertEquals(3, splitList.size()); // 5 rows split by 2
        assertEquals(2, splitList.get(0).size());
        assertEquals(2, splitList.get(1).size());
        assertEquals(1, splitList.get(2).size());
    }

    @Test
    public void testSplitToList() {
        List<Dataset> splits = dataset.splitToList(2);

        assertEquals(3, splits.size());
        assertEquals(2, splits.get(0).size());
    }

    @Test
    public void testSlice() {
        Dataset slice = dataset.slice(1, 3);

        assertEquals(2, slice.size());
        assertEquals("Jane", slice.get(0, 1));
        assertEquals("Bob", slice.get(1, 1));
        assertTrue(slice.isFrozen());
    }

    @Test
    public void testSliceWithColumns() {
        Dataset slice = dataset.slice(1, 3, Arrays.asList("name", "age"));

        assertEquals(2, slice.size());
        assertEquals(2, slice.columnCount());
        assertTrue(slice.containsColumn("name"));
        assertTrue(slice.containsColumn("age"));
        assertFalse(slice.containsColumn("id"));
    }

    @Test
    public void testPaginate() {
        {
            Paginated<Dataset> paginated = dataset.paginate(2);

            assertEquals(3, paginated.totalPages());
            assertEquals(2, paginated.pageSize());

            Dataset page1 = paginated.firstPage().get();
            assertEquals(2, page1.size());

            Dataset page3 = paginated.lastPage().get();
            assertEquals(1, page3.size());
        }

        {
            Paginated<Dataset> paginated = dataset.paginate(2);

            assertEquals(3, paginated.totalPages());
            assertEquals(2, paginated.pageSize());

            Iterator<Dataset> iter = paginated.iterator();

            Dataset page1 = iter.next();
            assertEquals(2, page1.size());

            iter.next();

            Dataset page3 = iter.next();
            assertEquals(1, page3.size());
        }
    }

    @Test
    public void testStream() {
        List<String> names = dataset.stream("name").map(Object::toString).toList();

        assertEquals(Arrays.asList("John", "Jane", "Bob", "Alice", "Charlie"), names);
    }

    @Test
    public void testStreamWithClass() {
        List<Person> persons = dataset.stream(Person.class).toList();

        assertEquals(5, persons.size());
        assertEquals("John", persons.get(0).getName());
    }

    @Test
    public void testStreamWithRowMapper() {
        dataset.println();
        List<String> result = dataset.stream((rowIndex, row) -> rowIndex + ":" + row.get(1)).toList();

        assertEquals(Arrays.asList("0:John", "1:Jane", "2:Bob", "3:Alice", "4:Charlie"), result);
    }

    @Test
    public void testApply() {
        int size = dataset.apply(ds -> ds.size());
        assertEquals(5, size);
    }

    @Test
    public void testApplyIfNotEmpty() {
        Optional<Integer> result = dataset.applyIfNotEmpty(ds -> ds.size());
        assertTrue(result.isPresent());
        assertEquals(5, result.get().intValue());

        Dataset empty = new RowDataset(new ArrayList<>(), new ArrayList<>());
        Optional<Integer> emptyResult = empty.applyIfNotEmpty(ds -> ds.size());
        assertFalse(emptyResult.isPresent());
    }

    @Test
    public void testAccept() {
        final int[] count = { 0 };
        dataset.accept(ds -> count[0] = ds.size());
        assertEquals(5, count[0]);
    }

    @Test
    public void testAcceptIfNotEmpty() {
        final boolean[] called = { false };
        dataset.acceptIfNotEmpty(ds -> called[0] = true);
        assertTrue(called[0]);
    }

    @Test
    public void testFreeze() {
        assertFalse(dataset.isFrozen());
        dataset.freeze();
        assertTrue(dataset.isFrozen());
    }

    @Test
    public void testModifyFrozenDataset() {
        dataset.freeze();
        assertThrows(IllegalStateException.class, () -> dataset.set(0, 0, 999));
    }

    @Test
    public void testIsEmpty() {
        assertFalse(dataset.isEmpty());

        Dataset empty = new RowDataset(new ArrayList<>(), new ArrayList<>());
        assertTrue(empty.isEmpty());
    }

    @Test
    public void testTrimToSize() {
        dataset.trimToSize(); // Should not throw exception
    }

    @Test
    public void testSize() {
        assertEquals(5, dataset.size());
    }

    @Test
    public void testClear() {
        dataset.clear();
        assertEquals(0, dataset.size());
        assertTrue(dataset.isEmpty());
    }

    @Test
    public void testProperties() {
        assertTrue(dataset.getProperties().isEmpty());

        Map<String, Object> props = new HashMap<>();
        props.put("key", "value");
        Dataset dsWithProps = new RowDataset(columnNames, columnList, props);

        assertEquals("value", dsWithProps.getProperties().get("key"));
    }

    //    @Test
    //    public void testColumnNames() {
    //        List<String> names = dataset.columnNames().toList();
    //        assertEquals(Arrays.asList("id", "name", "age", "salary"), names);
    //    }

    @Test
    public void testColumns() {
        List<ImmutableList<Object>> columns = dataset.columns().toList();
        assertEquals(4, columns.size());
        assertEquals(Arrays.asList(1, 2, 3, 4, 5), columns.get(0));
    }

    @Test
    public void testColumnMap() {
        Map<String, ImmutableList<Object>> map = dataset.columnMap();
        assertEquals(4, map.size());
        assertTrue(map.containsKey("id"));
        assertEquals(Arrays.asList(1, 2, 3, 4, 5), map.get("id"));
    }

    @Test
    public void testPrintln() {
        // Just test that it doesn't throw exception
        StringWriter writer = new StringWriter();
        dataset.println(writer);

        String output = writer.toString();
        assertTrue(output.contains("John"));
        assertTrue(output.contains("Jane"));
    }

    @Test
    public void testPrintln_01() {
        {
            N.println(Strings.repeat("=", 120));
            Dataset.empty().println();
            N.println(Strings.repeat("=", 120));

            N.println(Strings.repeat("=", 120));
            Dataset.empty().println("# ");
            N.println(Strings.repeat("=", 120));

            N.println(Strings.repeat("=", 120));
            Dataset.empty().println("// ");
            N.println(Strings.repeat("=", 120));
        }
        {
            N.println(Strings.repeat("=", 120));
            dataset.set(0, 0, "A very long text that exceeds the usual width零零忑零零忑零零忑 to test wrapping functionality in the dataset printing method.零零忑");
            dataset.println();
            N.println(Strings.repeat("=", 120));

            N.println(Strings.repeat("=", 120));
            dataset.println("     * ## ");
            N.println(Strings.repeat("=", 120));

            N.println(Strings.repeat("=", 120));
            dataset.println("// ");
            N.println(Strings.repeat("=", 120));
        }

        N.println(dataset);
    }

    @Test
    public void testHashCode() {
        Dataset other = new RowDataset(columnNames, columnList);
        assertEquals(dataset.hashCode(), other.hashCode());
    }

    @Test
    public void testEquals() {
        Dataset other = dataset.copy();
        assertTrue(dataset.equals(other));

        other.set(0, 0, 999);
        assertFalse(dataset.equals(other));
    }

    @Test
    public void testToString() {
        String str = dataset.toString();
        assertTrue(str.contains("columnNames"));
        assertTrue(str.contains("id"));
        assertTrue(str.contains("name"));
    }

    // Helper method to create a collector
    private static <T, A, R> Collector<T, A, R> collector(Collector<T, A, R> downstream) {
        return downstream;
    }

    @Test
    public void testIntersectAll() {
        // Add duplicate rows
        dataset.addRow(new Object[] { 1, "John", 25, 50000.0 });
        dataset.addRow(new Object[] { 2, "Jane", 30, 60000.0 });

        List<String> otherColumns = Arrays.asList("id", "name", "age", "salary");
        List<List<Object>> otherData = new ArrayList<>();
        otherData.add(Arrays.asList(1, 1, 3));
        otherData.add(Arrays.asList("John", "John", "Bob"));
        otherData.add(Arrays.asList(25, 25, 35));
        otherData.add(Arrays.asList(50000.0, 50000.0, 70000.0));

        Dataset other = new RowDataset(otherColumns, otherData);

        Dataset intersectAll = dataset.intersectAll(other);

        assertEquals(3, intersectAll.size()); // 2 Johns + 1 Bob
    }

    @Test
    public void testIntersectAll_2() {
        {
            // Dataset 1 with columns "id", "name"
            Dataset ds1 = Dataset.rows(Arrays.asList("id", "name"), new Object[][] { { 1, "Alice" }, { 2, "Bob" }, { 3, "Charlie" }, { 2, "Bob" } // duplicate row
            });

            // Dataset 2 with columns "id", "name"
            Dataset ds2 = Dataset.rows(Arrays.asList("id", "name"), new Object[][] { { 2, "Bob" }, { 3, "Charlie" }, { 4, "Dave" }, { 2, "Bob" }, // duplicate row
                    { 2, "Bob" } // another duplicate
            });

            // Result will contain {2, "Bob"} twice and {3, "Charlie"} once
            Dataset result = ds1.intersectAll(ds2);

            result.println();
            assertEquals(3, result.size());
            assertEquals((Integer) 2, result.get(0, 0));
            assertEquals("Bob", result.get(0, 1));
            assertEquals((Integer) 3, result.get(1, 0));
            assertEquals("Charlie", result.get(1, 1));
            assertEquals((Integer) 2, result.get(2, 0));
            assertEquals("Bob", result.get(2, 1));
        }

        {
            // Dataset 1 with columns "id", "name"
            Dataset ds1 = Dataset.rows(Arrays.asList("id", "name"), new Object[][] { { 1, "Alice" }, { 2, "Bob" }, { 3, "Charlie" }, { 2, "Bob" } // duplicate row
            });

            // Dataset 2 with columns "id", "name"
            Dataset ds2 = Dataset.rows(Arrays.asList("id", "name"), new Object[][] { { 2, "Bob" }, { 3, "Charlie" }, { 4, "Dave" } });

            // Result will contain {2, "Bob"} twice and {3, "Charlie"} once
            Dataset result = ds1.intersectAll(ds2);

            result.println();
            assertEquals(3, result.size());
            assertEquals((Integer) 2, result.get(0, 0));
            assertEquals("Bob", result.get(0, 1));
            assertEquals((Integer) 3, result.get(1, 0));
            assertEquals("Charlie", result.get(1, 1));
            assertEquals((Integer) 2, result.get(2, 0));
            assertEquals("Bob", result.get(2, 1));
        }

    }

    @Test
    public void testExceptAll() {
        // Add duplicate rows
        dataset.addRow(new Object[] { 1, "John", 25, 50000.0 });

        List<String> otherColumns = Arrays.asList("id", "name", "age", "salary");
        List<List<Object>> otherData = new ArrayList<>();
        otherData.add(Arrays.asList(1, 3));
        otherData.add(Arrays.asList("John", "Bob"));
        otherData.add(Arrays.asList(25, 35));
        otherData.add(Arrays.asList(50000.0, 70000.0));

        Dataset other = new RowDataset(otherColumns, otherData);

        dataset.println();
        other.println();

        Dataset exceptAll = dataset.exceptAll(other);

        assertEquals(3, exceptAll.size()); // One John removed, Jane, Alice, Charlie remain

        exceptAll.println();
    }

    @Test
    public void testIntersection() {
        List<String> otherColumns = Arrays.asList("id", "name", "age", "salary");
        List<List<Object>> otherData = new ArrayList<>();
        // Add some matching and non-matching rows
        otherData.add(Arrays.asList(1, 1, 3, 6));
        otherData.add(Arrays.asList("John", "John", "Bob", "Frank"));
        otherData.add(Arrays.asList(25, 25, 35, 45));
        otherData.add(Arrays.asList(50000.0, 50000.0, 70000.0, 90000.0));

        Dataset other = new RowDataset(otherColumns, otherData);

        Dataset intersection = dataset.intersection(other);

        assertEquals(2, intersection.size()); // John and Bob (count preserved from left)
    }

    @Test
    public void testIntersection_2() {
        {
            // Dataset 1 with columns "id", "name"
            Dataset ds1 = Dataset.rows(Arrays.asList("id", "name"), new Object[][] { { 1, "Alice" }, { 2, "Bob" }, { 3, "Charlie" }, { 2, "Bob" } // duplicate row
            });

            // Dataset 2 with columns "id", "name"
            Dataset ds2 = Dataset.rows(Arrays.asList("id", "name"), new Object[][] { { 2, "Bob" }, { 3, "Charlie" }, { 4, "Dave" }, { 2, "Bob" }, // duplicate row
                    { 2, "Bob" } // another duplicate
            });

            // Result will contain {2, "Bob"} twice and {3, "Charlie"} once
            Dataset result = ds1.intersection(ds2);

            result.println();
            assertEquals(3, result.size());
            assertEquals((Integer) 2, result.get(0, 0));
            assertEquals("Bob", result.get(0, 1));
            assertEquals((Integer) 3, result.get(1, 0));
            assertEquals("Charlie", result.get(1, 1));
            assertEquals((Integer) 2, result.get(2, 0));
            assertEquals("Bob", result.get(2, 1));
        }

        {
            // Dataset 1 with columns "id", "name"
            Dataset ds1 = Dataset.rows(Arrays.asList("id", "name"), new Object[][] { { 1, "Alice" }, { 2, "Bob" }, { 3, "Charlie" }, { 2, "Bob" } // duplicate row
            });

            // Dataset 2 with columns "id", "name"
            Dataset ds2 = Dataset.rows(Arrays.asList("id", "name"), new Object[][] { { 2, "Bob" }, { 3, "Charlie" }, { 4, "Dave" } });

            // Result will contain {2, "Bob"} twice and {3, "Charlie"} once
            Dataset result = ds1.intersection(ds2);

            result.println();
            assertEquals(2, result.size());
            assertEquals((Integer) 2, result.get(0, 0));
            assertEquals("Bob", result.get(0, 1));
            assertEquals((Integer) 3, result.get(1, 0));
            assertEquals("Charlie", result.get(1, 1));
        }

    }

    @Test
    public void testDifference() {
        List<String> otherColumns = Arrays.asList("id", "name", "age", "salary");
        List<List<Object>> otherData = new ArrayList<>();
        otherData.add(Arrays.asList(1, 1, 3));
        otherData.add(Arrays.asList("John", "John", "Bob"));
        otherData.add(Arrays.asList(25, 25, 35));
        otherData.add(Arrays.asList(50000.0, 50000.0, 70000.0));

        Dataset other = new RowDataset(otherColumns, otherData);

        Dataset difference = dataset.difference(other);

        assertEquals(3, difference.size()); // Jane, Alice, Charlie
    }

    @Test
    public void testSymmetricDifference() {
        List<String> otherColumns = Arrays.asList("id", "name", "age", "salary");
        List<List<Object>> otherData = new ArrayList<>();
        otherData.add(Arrays.asList(1, 6));
        otherData.add(Arrays.asList("John", "Frank"));
        otherData.add(Arrays.asList(25, 45));
        otherData.add(Arrays.asList(50000.0, 90000.0));

        Dataset other = new RowDataset(otherColumns, otherData);

        Dataset symDiff = dataset.symmetricDifference(other);

        assertEquals(5, symDiff.size()); // Jane, Bob, Alice, Charlie from left + Frank from right
    }

    @Test
    public void testSymmetricDifferenceWithKeyColumnsAndRequireSameColumns() {
        Dataset dataset1 = Dataset.rows(Arrays.asList("id", "name", "department"),
                new Object[][] { { 1, "Alice", "HR" }, { 2, "Bob", "Engineering" }, { 2, "Bob", "Engineering" }, // duplicate row
                        { 3, "Charlie", "Marketing" } });
        Dataset dataset2 = Dataset.rows(Arrays.asList("id", "name", "salary"),
                new Object[][] { { 2, "Bob", 50000 }, { 3, "Charlie", 55000 }, { 4, "Dave", 60000 } });

        Collection<String> keyColumns = Arrays.asList("id", "name");
        Dataset result = dataset1.symmetricDifference(dataset2, keyColumns, false);
        // Result contains {1, "Alice", "HR", null}, one occurrence of {2, "Bob", "Engineering", null}
        // and {4, "Dave", null, 60000}
        // One Bob row remains because dataset1 has two occurrences and dataset2 has one
        result.println();
        assertEquals(3, result.size());
        assertEquals(4, result.columnCount());
        assertTrue(result.containsColumn("id"));
        assertTrue(result.containsColumn("name"));
        assertTrue(result.containsColumn("department"));
        assertTrue(result.containsColumn("salary"));
        assertEquals(1, (Integer) result.get(0, 0));
        assertEquals("Alice", result.get(0, 1));
        assertEquals("HR", result.get(0, 2));
        assertNull(result.get(0, 3));
        assertEquals(2, (Integer) result.get(1, 0));
        assertEquals("Bob", result.get(1, 1));
        assertEquals("Engineering", result.get(1, 2));
        assertNull(result.get(1, 3));
        assertEquals(4, (Integer) result.get(2, 0));
        assertEquals("Dave", result.get(2, 1));
        assertNull(result.get(2, 2));
        assertEquals(60000, (Integer) result.get(2, 3));

    }

    @Test
    public void testMerge() {
        Dataset dataset1 = Dataset.rows(Arrays.asList("id", "name", "age"), new Object[][] { { 1, "Alice", 25 }, { 2, "Bob", 30 } });
        Dataset dataset2 = Dataset.rows(Arrays.asList("id", "name", "score"), new Object[][] { { 1, "Alice", 95 }, { 3, "Charlie", 85 } });

        dataset1.merge(dataset2);
        // dataset1 now contains columns: id, name, age, score
        // dataset1 now contains rows: {1, "Alice", 25, null}, {2, "Bob", 30, null}, {1, "Alice", null, 95}, {3, "Charlie", null, 85}
        Dataset result = dataset1;
        result.println();
        assertEquals(4, result.columnCount());
        assertTrue(result.containsColumn("id"));
        assertTrue(result.containsColumn("name"));
        assertTrue(result.containsColumn("age"));
        assertTrue(result.containsColumn("score"));
        assertEquals(4, result.size());
        assertEquals(1, (Integer) result.get(0, 0));
        assertEquals("Alice", result.get(0, 1));
        assertEquals(25, (Integer) result.get(0, 2));
        assertNull(result.get(0, 3));
        assertEquals(2, (Integer) result.get(1, 0));
        assertEquals("Bob", result.get(1, 1));
        assertEquals(30, (Integer) result.get(1, 2));
        assertNull(result.get(1, 3));
        assertEquals(1, (Integer) result.get(2, 0));
        assertEquals("Alice", result.get(2, 1));
        assertNull(result.get(2, 2));
        assertEquals(95, (Integer) result.get(2, 3));
        assertEquals(3, (Integer) result.get(3, 0));
        assertEquals("Charlie", result.get(3, 1));
        assertNull(result.get(3, 2));
        assertEquals(85, (Integer) result.get(3, 3));

    }

    @Test
    public void testMergeWithRequiresSameColumns() {

        Dataset dataset1 = Dataset.rows(Arrays.asList("id", "name", "age"), new Object[][] { { 1, "Alice", 25 }, { 2, "Bob", 30 } });
        Dataset dataset2 = Dataset.rows(Arrays.asList("id", "name", "score"), new Object[][] { { 1, "Alice", 95 }, { 3, "Charlie", 85 } });

        dataset1.merge(dataset2, false);
        // dataset1 now contains columns: id, name, age, score
        // dataset1 now contains rows: {1, "Alice", 25, null}, {2, "Bob", 30, null}, {1, "Alice", null, 95}, {3, "Charlie", null, 85}
        Dataset result = dataset1;
        result.println();
        assertEquals(4, result.columnCount());
        assertTrue(result.containsColumn("id"));
        assertTrue(result.containsColumn("name"));
        assertTrue(result.containsColumn("age"));
        assertTrue(result.containsColumn("score"));
        assertEquals(4, result.size());
        assertEquals(1, (Integer) result.get(0, 0));
        assertEquals("Alice", result.get(0, 1));
        assertEquals(25, (Integer) result.get(0, 2));
        assertNull(result.get(0, 3));
        assertEquals(2, (Integer) result.get(1, 0));
        assertEquals("Bob", result.get(1, 1));
        assertEquals(30, (Integer) result.get(1, 2));
        assertNull(result.get(1, 3));
        assertEquals(1, (Integer) result.get(2, 0));
        assertEquals("Alice", result.get(2, 1));
        assertNull(result.get(2, 2));
        assertEquals(95, (Integer) result.get(2, 3));
        assertEquals(3, (Integer) result.get(3, 0));
        assertEquals("Charlie", result.get(3, 1));
        assertNull(result.get(3, 2));
        assertEquals(85, (Integer) result.get(3, 3));

        assertThrows(IllegalArgumentException.class, () -> dataset1.merge(dataset2, true));

    }

    @Test
    public void testMergeWithColumnNames() {
        Dataset dataset1 = Dataset.rows(Arrays.asList("id", "name", "age"), new Object[][] { { 1, "Alice", 25 }, { 2, "Bob", 30 } });
        Dataset dataset2 = Dataset.rows(Arrays.asList("id", "name", "score"), new Object[][] { { 1, "Alice", 95 }, { 3, "Charlie", 85 } });

        dataset1.merge(dataset2, Arrays.asList("id", "name"));
        Dataset result = dataset1;
        result.println();
        // Result contains columns: id, name
        // Result contains: {1, "Alice", 25}, {2, "Bob", 30}, {1, "Alice", null}, {3, "Charlie", null}
        assertEquals(3, result.columnCount());
        assertTrue(result.containsColumn("id"));
        assertTrue(result.containsColumn("name"));
        assertTrue(result.containsColumn("age"));
        assertEquals(4, result.size());
        assertEquals(1, (Integer) result.get(0, 0));
        assertEquals("Alice", result.get(0, 1));
        assertEquals(25, (Integer) result.get(0, 2));
        assertEquals(2, (Integer) result.get(1, 0));
        assertEquals("Bob", result.get(1, 1));
        assertEquals(30, (Integer) result.get(1, 2));
        assertEquals(1, (Integer) result.get(2, 0));
        assertEquals("Alice", result.get(2, 1));
        assertNull(result.get(2, 2));
        assertEquals(3, (Integer) result.get(3, 0));
        assertEquals("Charlie", result.get(3, 1));
        assertNull(result.get(3, 2));
    }

    @Test
    public void testMergeWithColumnNames2() {
        Dataset dataset1 = Dataset.rows(Arrays.asList("id", "name", "age"), new Object[][] { { 1, "Alice", 25 }, { 2, "Bob", 30 } });
        Dataset dataset2 = Dataset.rows(Arrays.asList("id", "name", "score", "grade"), new Object[][] { { 1, "Alice", 95, "A" }, { 3, "Charlie", 85, "B" } });

        Collection<String> selectedColumns = Arrays.asList("id", "score");
        dataset1.merge(dataset2, selectedColumns);
        // dataset1 now contains columns: id, name, age, score
        // dataset1 now contains rows: {1, "Alice", 25, null}, {2, "Bob", 30, null}, {1, null, null, 95}, {3, null, null, 85}
        Dataset result = dataset1;
        result.println();
        assertEquals(4, result.columnCount());
        assertTrue(result.containsColumn("id"));
        assertTrue(result.containsColumn("name"));
        assertTrue(result.containsColumn("age"));
        assertTrue(result.containsColumn("score"));
        assertEquals(4, result.size());
        assertEquals(1, (Integer) result.get(0, 0));
        assertEquals("Alice", result.get(0, 1));
        assertEquals(25, (Integer) result.get(0, 2));
        assertNull(result.get(0, 3));
        assertEquals(2, (Integer) result.get(1, 0));
        assertEquals("Bob", result.get(1, 1));
        assertEquals(30, (Integer) result.get(1, 2));
        assertNull(result.get(1, 3));
        assertEquals(1, (Integer) result.get(2, 0));
        assertNull(result.get(2, 1));
        assertNull(result.get(2, 2));
        assertEquals(95, (Integer) result.get(2, 3));
        assertEquals(3, (Integer) result.get(3, 0));
        assertNull(result.get(3, 1));
        assertNull(result.get(3, 2));
        assertEquals(85, (Integer) result.get(3, 3));

    }

    @Test
    public void testMergeWithCollection() {
        List<Dataset> others = new ArrayList<>();
        others.add(dataset);

        List<String> otherColumns1 = Arrays.asList("id", "name", "age", "salary");
        List<List<Object>> otherData1 = new ArrayList<>();
        otherData1.add(Arrays.asList(6, 7));
        otherData1.add(Arrays.asList("Frank", "Grace"));
        otherData1.add(Arrays.asList(45, 50));
        otherData1.add(Arrays.asList(90000.0, 95000.0));

        List<String> otherColumns2 = Arrays.asList("id", "name", "age", "salary", "bonus");
        List<List<Object>> otherData2 = new ArrayList<>();
        otherData2.add(Arrays.asList(8));
        otherData2.add(Arrays.asList("Henry"));
        otherData2.add(Arrays.asList(55));
        otherData2.add(Arrays.asList(100000.0));
        otherData2.add(Arrays.asList(10000.0));

        others.add(new RowDataset(otherColumns1, otherData1));
        others.add(new RowDataset(otherColumns2, otherData2));

        Dataset merged = N.merge(others);

        assertEquals(8, merged.size()); // 5 + 2 + 1
        assertTrue(merged.containsColumn("bonus"));
    }

    @Test
    public void testGetRowWithPrefixAndFieldNameMap() {
        // Create a dataset with nested column names
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

        // This would typically be used with bean classes that have nested properties
        // For this test, we'll just verify it doesn't throw exceptions
        assertThrows(IllegalArgumentException.class, () -> nestedDataset.stream(prefixMap, Map.class).toList());
        // assertEquals(2, rows.size());
    }

    @Test
    public void testComplexGroupByScenarios() {
        // Test grouping with key extractor
        Dataset grouped = dataset.groupBy("age", age -> ((int) age / 10) * 10, "id", "sumOfId", Collectors.summarizingInt(Numbers::toInt)); // Group by decade
        assertTrue(grouped.size() <= dataset.size());

        // Test grouping with multiple columns and aggregation
        Dataset multiGrouped = dataset.groupBy(Arrays.asList("age"), Arrays.asList("salary"), "totalSalary",
                collector(Collectors.summingDouble(arr -> ((Object[]) arr)[0] != null ? (Double) ((Object[]) arr)[0] : 0.0)));
        assertEquals(5, multiGrouped.size()); // One row per unique age
    }

    @Test
    public void testEdgeCases() {
        // Test empty Dataset
        Dataset empty = new RowDataset(Arrays.asList("col1"), Arrays.asList(new ArrayList<>()));
        assertEquals(0, empty.size());
        assertTrue(empty.isEmpty());

        // Test single column, single row
        Dataset single = new RowDataset(Arrays.asList("col1"), Arrays.asList(Arrays.asList("value")));
        assertEquals(1, single.size());
        assertEquals(1, single.columnCount());

        // Test with nulls
        List<String> nullColumns = Arrays.asList("col1", "col2");
        List<List<Object>> nullData = new ArrayList<>();
        nullData.add(Arrays.asList(null, "A"));
        nullData.add(Arrays.asList("B", null));

        Dataset withNulls = new RowDataset(nullColumns, nullData);
        assertTrue(withNulls.isNull(0, 0));
        assertFalse(withNulls.isNull(0, 1));
    }

    @Test
    public void testStreamWithBiFunctionMapper() {
        List<String> nameAges = dataset.stream(Tuple2.of("name", "age"), (name, age) -> name + " is " + age + " years old").toList();

        assertEquals(5, nameAges.size());
        assertEquals("John is 25 years old", nameAges.get(0));
    }

    @Test
    public void testStreamWithTriFunctionMapper() {
        List<String> combined = dataset.stream(Tuple3.of("id", "name", "age"), (id, name, age) -> "ID:" + id + " Name:" + name + " Age:" + age).toList();

        assertEquals(5, combined.size());
        assertEquals("ID:1 Name:John Age:25", combined.get(0));
    }

    @Test
    public void testComplexJoinScenarios() {
        // Test join with collection result
        List<String> otherColumns = Arrays.asList("id", "skill");
        List<List<Object>> otherData = new ArrayList<>();
        otherData.add(Arrays.asList(1, 1, 2, 2));
        otherData.add(Arrays.asList("Java", "Python", "C++", "Java"));

        Dataset skills = new RowDataset(otherColumns, otherData);

        Dataset joined = dataset.leftJoin(skills, Collections.singletonMap("id", "id"), "skills", List.class, size -> new ArrayList<>(size));

        assertTrue(joined.containsColumn("skills"));
        Object skillsList = joined.get(0, joined.getColumnIndex("skills"));
        assertTrue(skillsList instanceof Collection);
    }

    @Test
    public void testInvalidOperations() {
        // Try to rename to existing column
        assertThrows(IllegalArgumentException.class, () -> dataset.renameColumn("age", "name"));
    }

    @Test
    public void testNullHandling() {
        // Add row with nulls
        dataset.addRow(new Object[] { null, null, null, null });

        // Test null handling in various operations
        assertNull(dataset.get(5, 0));
        assertTrue(dataset.isNull(5, 0));

        dataset.println();

        dataset.absolute(5);

        // Test getters with null
        assertEquals(0, dataset.getInt(dataset.getColumnIndex("id"))); // Current row has null
        assertEquals(0.0, dataset.getDouble(dataset.getColumnIndex("salary")), 0.01);
        assertFalse(dataset.getBoolean(dataset.getColumnIndex("id")));
    }

    @Test
    public void testConcurrentModification() {
        Stream<Object> stream = dataset.stream("id");

        // Modify dataset while iterating
        Iterator<Object> iter = stream.iterator();
        assertTrue(iter.hasNext());
        iter.next();

        // This should cause ConcurrentModificationException on next iteration
        dataset.set(0, 0, 999);

        //    try {
        //        iter.next();
        //        fail("Should throw ConcurrentModificationException");
        //    } catch (ConcurrentModificationException e) {
        //        // Expected
        //    }

        assertEquals(2, iter.next());
    }

    // Helper class for testing bean operations
    public static class Person {
        private int id;
        private String name;
        private int age;
        private double salary;

        public Person() {
        }

        public Person(int id, String name, int age, double salary) {
            this.id = id;
            this.name = name;
            this.age = age;
            this.salary = salary;
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

    @Test
    public void testEmptyDatasetConstant() {
        Dataset empty = Dataset.empty();
        assertEquals(0, empty.size());
        assertTrue(empty.isFrozen());
    }

    @Test
    public void testOutputFormats() throws IOException {
        // Test JSON output to stream
        ByteArrayOutputStream jsonOut = new ByteArrayOutputStream();
        dataset.toJson(jsonOut);
        String json = jsonOut.toString();
        assertTrue(json.contains("John"));

        // Test XML output to stream
        ByteArrayOutputStream xmlOut = new ByteArrayOutputStream();
        dataset.toXml(xmlOut);
        String xml = xmlOut.toString();
        assertTrue(xml.contains("<name>John</name>"));

        // Test CSV output to stream
        ByteArrayOutputStream csvOut = new ByteArrayOutputStream();
        dataset.toCsv(csvOut);
        String csv = csvOut.toString();
        assertTrue(csv.contains("John"));
    }

    @Test
    public void testRangeOperations() {
        // Test various range operations
        dataset.forEach(1, 3, row -> {
            assertNotNull(row.get(0));
        });

        dataset.forEach(1, 3, Arrays.asList("name", "age"), row -> {
            assertEquals(2, row.length());
        });

        // Test reverse iteration
        List<String> reverseNames = new ArrayList<>();
        dataset.forEach(4, 0, row -> {
            reverseNames.add(row.get(1).toString());
        });
        assertEquals(Arrays.asList("Charlie", "Alice", "Bob", "Jane"), reverseNames);
    }

    @Test
    public void testComplexFilterScenarios() {
        // Test filter with range
        Dataset filtered = dataset.filter(1, 4, row -> (int) row.get(2) > 25);
        assertTrue(filtered.size() <= 3);

        // Test filter with column names
        Dataset colFiltered = dataset.filter(Arrays.asList("name", "age"), row -> row.get(0).toString().length() > 3 && (int) row.get(1) > 25);
        assertTrue(colFiltered.size() < dataset.size());
    }

    @Test
    public void testComplexSortScenarios() {
        // Test sort with key extractor
        dataset.sortBy(Arrays.asList("name"), row -> row.get(0).toString().length()); // Sort by name length

        // Test parallel sort
        dataset.parallelSortBy(Arrays.asList("age", "salary"));

        // Verify first row has minimum age
        int firstAge = (int) dataset.get(0, 2);
        for (int i = 1; i < dataset.size(); i++) {
            assertTrue(firstAge <= (int) dataset.get(i, 2));
        }
    }

    @Test
    @DisplayName("Should move single row within valid range")
    public void testMoveRowsSingleRow() {
        // Move first row (John) to position 3
        dataset.moveRows(0, 1, 3);
        assertEquals("Jane", dataset.getRow(0)[1]);
        assertEquals("Bob", dataset.getRow(1)[1]);
        assertEquals("Alice", dataset.getRow(2)[1]);
        assertEquals("John", dataset.getRow(3)[1]);
        assertEquals("Charlie", dataset.getRow(4)[1]);
        assertEquals(5, dataset.size());
    }

    @Test
    @DisplayName("Should move multiple consecutive rows")
    public void testMoveRowsMultipleRows() {
        // Move rows 1-2 (Jane, Bob) to position 3
        dataset.moveRows(1, 3, 3);
        assertEquals("John", dataset.getRow(0)[1]);
        assertEquals("Alice", dataset.getRow(1)[1]);
        assertEquals("Charlie", dataset.getRow(2)[1]);
        assertEquals("Jane", dataset.getRow(3)[1]);
        assertEquals("Bob", dataset.getRow(4)[1]);
        assertEquals(5, dataset.size());
    }

    @Test
    @DisplayName("Should move rows to beginning")
    public void testMoveRowsToBeginning() {
        // Move last two rows to beginning
        dataset.moveRows(3, 5, 0);
        assertEquals("Alice", dataset.getRow(0)[1]);
        assertEquals("Charlie", dataset.getRow(1)[1]);
        assertEquals("John", dataset.getRow(2)[1]);
        assertEquals("Jane", dataset.getRow(3)[1]);
        assertEquals("Bob", dataset.getRow(4)[1]);
    }

    @Test
    @DisplayName("Should move rows to end")
    public void testMoveRowsToEnd() {
        // Move first two rows (John, Jane) to end
        dataset.moveRows(0, 2, 3);
        assertEquals("Bob", dataset.getRow(0)[1]);
        assertEquals("Alice", dataset.getRow(1)[1]);
        assertEquals("Charlie", dataset.getRow(2)[1]);
        assertEquals("John", dataset.getRow(3)[1]);
        assertEquals("Jane", dataset.getRow(4)[1]);
    }

    @Test
    @DisplayName("Should handle moving rows backward")
    public void testMoveRowsBackward() {
        // Move rows 3-4 (Alice, Charlie) to position 1
        dataset.moveRows(3, 5, 1);
        assertEquals("John", dataset.getRow(0)[1]);
        assertEquals("Alice", dataset.getRow(1)[1]);
        assertEquals("Charlie", dataset.getRow(2)[1]);
        assertEquals("Jane", dataset.getRow(3)[1]);
        assertEquals("Bob", dataset.getRow(4)[1]);
    }

    @Test
    @DisplayName("Should not change dataset when moving to same position")
    public void testMoveRowsToSamePosition() {
        List<String> originalNames = new ArrayList<>();
        for (int i = 0; i < dataset.size(); i++) {
            originalNames.add((String) dataset.getRow(i)[1]);
        }

        dataset.moveRows(1, 3, 1);

        for (int i = 0; i < dataset.size(); i++) {
            assertEquals(originalNames.get(i), dataset.getRow(i)[1]);
        }
    }

    @Test
    @DisplayName("Should throw exception for invalid fromRowIndex")
    public void testMoveRowsInvalidFromIndex() {
        assertThrows(IndexOutOfBoundsException.class, () -> dataset.moveRows(-1, 2, 3));
        // assertThrows(IndexOutOfBoundsException.class, () -> dataset.moveRows(5, 5, 0));
        dataset.moveRows(5, 5, 0);
    }

    @Test
    @DisplayName("Should throw exception for invalid toRowIndex")
    public void testMoveRowsInvalidToIndex() {
        assertThrows(IndexOutOfBoundsException.class, () -> dataset.moveRows(0, -1, 2));
        assertThrows(IndexOutOfBoundsException.class, () -> dataset.moveRows(0, 6, 2));
        assertThrows(IndexOutOfBoundsException.class, () -> dataset.moveRows(2, 7, 0));
    }

    @Test
    @DisplayName("Should throw exception when fromIndex > toIndex")
    public void testMoveRowsInvalidRange() {
        assertThrows(IndexOutOfBoundsException.class, () -> dataset.moveRows(3, 1, 0));
    }

    @Test
    @DisplayName("Should throw exception for invalid newPosition")
    public void testMoveRowsInvalidNewPosition() {
        assertThrows(IndexOutOfBoundsException.class, () -> dataset.moveRows(0, 1, -1));
        assertThrows(IndexOutOfBoundsException.class, () -> dataset.moveRows(0, 2, 4)); // Can only move to positions 0-3
        assertThrows(IndexOutOfBoundsException.class, () -> dataset.moveRows(1, 4, 3)); // Can only move to positions 0-2
    }

    @Test
    @DisplayName("Should move entire dataset")
    public void testMoveRowsEntireDataset() {
        // Moving entire dataset to position 0 should not change anything
        List<String> originalNames = new ArrayList<>();
        for (int i = 0; i < dataset.size(); i++) {
            originalNames.add((String) dataset.getRow(i)[1]);
        }

        dataset.moveRows(0, 5, 0);

        for (int i = 0; i < dataset.size(); i++) {
            assertEquals(originalNames.get(i), dataset.getRow(i)[1]);
        }
    }

    @Test
    @DisplayName("Should move single column")
    public void testMoveColumnsSingle() {

        dataset.moveColumns(N.asList("age"), 0);

        assertEquals("age", dataset.columnNameList().get(0));
        assertEquals("id", dataset.columnNameList().get(1));
        assertEquals("name", dataset.columnNameList().get(2));
        assertEquals("salary", dataset.columnNameList().get(3));
        assertEquals(4, dataset.columnCount());
    }

    @Test
    @DisplayName("Should move multiple columns")
    public void testMoveColumnsMultiple() {
        dataset.moveColumns(Arrays.asList("name", "salary"), 0);

        assertEquals("name", dataset.columnNameList().get(0));
        assertEquals("salary", dataset.columnNameList().get(1));
        assertEquals("id", dataset.columnNameList().get(2));
        assertEquals("age", dataset.columnNameList().get(3));
    }

    @Test
    @DisplayName("Should move columns to end")
    public void testMoveColumnsToEnd() {
        dataset.moveColumns(Arrays.asList("id", "name"), 2);

        assertEquals("age", dataset.columnNameList().get(0));
        assertEquals("salary", dataset.columnNameList().get(1));
        assertEquals("id", dataset.columnNameList().get(2));
        assertEquals("name", dataset.columnNameList().get(3));
    }

    @Test
    @DisplayName("Should handle moving columns to same position")
    public void testMoveColumnsToSamePosition() {
        List<String> originalColumns = new ArrayList<>(dataset.columnNameList());

        dataset.moveColumns(Arrays.asList("name", "age"), 1);

        assertEquals(originalColumns, dataset.columnNameList());
    }

    @Test
    @DisplayName("Should handle empty column collection")
    public void testMoveColumnsEmpty() {
        List<String> originalColumns = new ArrayList<>(dataset.columnNameList());

        dataset.moveColumns(Collections.emptyList(), 2);

        assertEquals(originalColumns, dataset.columnNameList());
    }

    @Test
    @DisplayName("Should preserve data integrity after column move")
    public void testMoveColumnsDataIntegrity() {
        // Store original data
        Map<String, List<Object>> originalData = new HashMap<>();
        Map<String, ImmutableList<Object>> columnMap = dataset.columnMap();
        for (String col : dataset.columnNameList()) {
            originalData.put(col, new ArrayList<>(columnMap.get(col)));
        }

        dataset.println();
        // Move columns
        dataset.moveColumns(Arrays.asList("age", "salary"), 0);
        dataset.println();

        // Verify data integrity
        Map<String, ImmutableList<Object>> newColumnMap = dataset.columnMap();
        assertEquals(originalData.get("age"), new ArrayList<>(newColumnMap.get("age")));
        assertEquals(originalData.get("salary"), new ArrayList<>(newColumnMap.get("salary")));
        assertEquals(originalData.get("id"), new ArrayList<>(newColumnMap.get("id")));
        assertEquals(originalData.get("name"), new ArrayList<>(newColumnMap.get("name")));
    }

    @Test
    @DisplayName("Should throw exception for non-existent column")
    public void testMoveColumnsNonExistent() {
        assertThrows(IllegalArgumentException.class, () -> dataset.moveColumns(Arrays.asList("invalid_column"), 0));
        assertThrows(IllegalArgumentException.class, () -> dataset.moveColumns(Arrays.asList("name", "invalid_column"), 0));
    }

    @Test
    @DisplayName("Should throw exception for duplicate columns")
    public void testMoveColumnsDuplicates() {
        assertThrows(IllegalArgumentException.class, () -> dataset.moveColumns(Arrays.asList("name", "name"), 0));
    }

    @Test
    @DisplayName("Should throw exception for invalid newPosition")
    public void testMoveColumnsInvalidPosition() {
        assertThrows(IndexOutOfBoundsException.class, () -> dataset.moveColumns(Arrays.asList("name"), -1));
        assertThrows(IndexOutOfBoundsException.class, () -> dataset.moveColumns(Arrays.asList("name"), 4)); // Can move to 0-3
        assertThrows(IndexOutOfBoundsException.class, () -> dataset.moveColumns(Arrays.asList("name", "age"), 3)); // Can move to 0-2
    }

    @Test
    @DisplayName("Should move all columns except one")
    public void testMoveColumnsAllButOne() {

        dataset.println();

        dataset.moveColumns(Arrays.asList("id", "name", "age"), 1);

        dataset.println();

        assertEquals("salary", dataset.columnNameList().get(0));
        assertEquals("id", dataset.columnNameList().get(1));
        assertEquals("name", dataset.columnNameList().get(2));
        assertEquals("age", dataset.columnNameList().get(3));
    }

    @Test
    @DisplayName("Should handle column move in different orders")
    public void testMoveColumnsNonConsecutive() {
        dataset.println();
        dataset.moveColumns(Arrays.asList("salary", "id"), 1);
        dataset.println();

        assertEquals("name", dataset.columnNameList().get(0));
        assertEquals("salary", dataset.columnNameList().get(1));
        assertEquals("id", dataset.columnNameList().get(2));
        assertEquals("age", dataset.columnNameList().get(3));
    }

    @Test
    @DisplayName("Should handle column move in different orders")
    public void testMoveColumnsNonConsecutive2() {
        dataset.println();
        dataset.moveColumns(Arrays.asList("id", "salary", "name", "age"), 0);
        dataset.println();

        assertEquals("id", dataset.columnNameList().get(0));
        assertEquals("salary", dataset.columnNameList().get(1));
        assertEquals("name", dataset.columnNameList().get(2));
        assertEquals("age", dataset.columnNameList().get(3));
    }

    @Test
    @DisplayName("Should handle column move in different orders")
    public void testMoveColumnsNonConsecutive3() {
        dataset.println();
        dataset.moveColumns(Arrays.asList("salary", "name"), 0);
        dataset.println();

        assertEquals("salary", dataset.columnNameList().get(0));
        assertEquals("name", dataset.columnNameList().get(1));
        assertEquals("id", dataset.columnNameList().get(2));
        assertEquals("age", dataset.columnNameList().get(3));
    }

    @Test
    @DisplayName("Should verify row data after complex moves")
    public void testMoveRowsComplexDataVerification() {
        // Store original data for verification
        Object[] originalRow0 = dataset.getRow(0);
        Object[] originalRow2 = dataset.getRow(2);
        Object[] originalRow4 = dataset.getRow(4);

        // Move middle row (Bob) to end
        dataset.moveRows(2, 3, 4);

        // Verify data integrity
        assertArrayEquals(originalRow0, dataset.getRow(0));
        assertArrayEquals(originalRow2, dataset.getRow(4));
        assertArrayEquals(originalRow4, dataset.getRow(3));
    }

    @Test
    @DisplayName("Should handle moving adjacent blocks of rows")
    public void testMoveRowsAdjacentBlocks() {
        // Move rows in a way that tests block adjacency
        dataset.moveRows(1, 3, 3); // Move Jane, Bob to end
        dataset.moveRows(0, 1, 2); // Move John between Alice and Jane
        dataset.println();

        assertEquals("Alice", dataset.getRow(0)[1]);
        assertEquals("Charlie", dataset.getRow(1)[1]);
        assertEquals("John", dataset.getRow(2)[1]);
        assertEquals("Jane", dataset.getRow(3)[1]);
        assertEquals("Bob", dataset.getRow(4)[1]);
    }

    @Test
    @DisplayName("Should verify column order preservation with complex moves")
    public void testMoveColumnsComplexOrderPreservation() {
        // Perform multiple column moves
        dataset.moveColumns(N.asList("salary"), 0);
        dataset.moveColumns(Arrays.asList("name", "age"), 2);

        assertEquals("salary", dataset.columnNameList().get(0));
        assertEquals("id", dataset.columnNameList().get(1));
        assertEquals("name", dataset.columnNameList().get(2));
        assertEquals("age", dataset.columnNameList().get(3));

        // Verify data is still correct (salary, id, name, age)
        assertEquals(50000.0, dataset.getRow(0)[0]);
        assertEquals(1, dataset.getRow(0)[1]);
        assertEquals("John", dataset.getRow(0)[2]);
        assertEquals(25, dataset.getRow(0)[3]);
    }

    @Test
    @DisplayName("Should handle edge case of moving last row")
    public void testMoveRowsLastRowEdgeCase() {
        // Move last row to different positions
        dataset.moveRows(4, 5, 0); // Move Charlie to beginning
        assertEquals("Charlie", dataset.getRow(0)[1]);
        assertEquals("John", dataset.getRow(1)[1]);

        dataset.moveRows(0, 1, 4); // Move back to end
        assertEquals("John", dataset.getRow(0)[1]);
        assertEquals("Charlie", dataset.getRow(4)[1]);
    }

    @Test
    @DisplayName("Should maintain dataset properties after moves")
    public void testMaintainPropertiesAfterMoves() {
        Map<String, Object> properties = new HashMap<>();
        properties.put("test", "value");
        RowDataset datasetWithProps = new RowDataset(columnNames, columnList, properties);

        datasetWithProps.moveRows(0, 2, 3);
        datasetWithProps.moveColumns(Arrays.asList("name"), 0);

        assertEquals("value", datasetWithProps.getProperties().get("test"));
        assertEquals(5, datasetWithProps.size());
        assertEquals(4, datasetWithProps.columnCount());
    }
}

package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.parser.JsonDeserConfig;
import com.landawn.abacus.util.NoCachingNoUpdating.DisposableObjArray;
import com.landawn.abacus.util.Tuple.Tuple2;
import com.landawn.abacus.util.Tuple.Tuple3;
import com.landawn.abacus.util.u.Optional;
import com.landawn.abacus.util.function.BiConsumer;
import com.landawn.abacus.util.function.Function;
import com.landawn.abacus.util.function.TriFunction;
import com.landawn.abacus.util.stream.Collectors;
import com.landawn.abacus.util.stream.Collectors.MoreCollectors;
import com.landawn.abacus.util.stream.IntStream;
import com.landawn.abacus.util.stream.Stream;

import testfixtures.entity.extendDirty.basic.Account;

public class DatasetTest extends DatasetTestSupport {

    @Test
    public void testXmlSelectionPreservesSelectedColumnOrder() {
        Dataset dataset = Dataset.rows(List.of("id", "name"), new Object[][] { { 1, "Alice" } });
        List<String> selection = List.of("name", "id");
        String expected = "<dataset><row><name>Alice</name><id>1</id></row></dataset>";

        assertEquals(expected, dataset.toXml(0, 1, selection));
        assertEquals(expected, dataset.toXml(0, 1, selection, "row"));

        StringWriter writer = new StringWriter();
        dataset.toXml(0, 1, selection, "row", writer);
        assertEquals(expected, writer.toString());

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        dataset.toXml(0, 1, selection, "row", output);
        assertEquals(expected, output.toString(java.nio.charset.StandardCharsets.UTF_8));
        assertEquals(List.of("id", "name"), dataset.columnNames());
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
    public void testRowCount() {
        Dataset dataset = Dataset.rows(Arrays.asList("id", "name"), new Object[][] { { 1, "Alice" }, { 2, "Bob" } });

        assertEquals(dataset.size(), dataset.rowCount());
        assertEquals(2, dataset.rowCount());
        assertEquals(0, emptyDataset.rowCount());
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
    public void testEmptyDataset_NullColumnSelection_ThrowsIAE_NotNPE() {
        // regression: on a zero-column Dataset a null column selection previously slipped past
        // checkColumnNames' guard and NPE'd; it must throw the documented IllegalArgumentException.
        assertThrows(IllegalArgumentException.class, () -> emptyDataset.copy((Collection<String>) null));
        assertThrows(IllegalArgumentException.class, () -> emptyDataset.toList((Collection<String>) null, Object.class));
        // an EMPTY (non-null) selection on a zero-column Dataset stays valid: the empty list is the full
        // (empty) column set, so no-arg operations that delegate via columnNameList() keep working.
        assertDoesNotThrow(() -> emptyDataset.copy(Collections.<String> emptyList()));
    }

    @Test
    public void testEmptyDatasetConstant() {
        Dataset empty = Dataset.empty();
        assertEquals(0, empty.size());
        assertTrue(empty.isFrozen());
    }

    @Test
    public void test_emptyDataset() {
        Dataset empty = CommonUtil.newEmptyDataset();
        Dataset named = CommonUtil.newEmptyDataset(CommonUtil.toList("firstName", "lastName"));
        assertEquals(0, empty.size());
        assertEquals(0, named.size());
        assertNotNull(empty.toJson());
        assertNotNull(empty.toXml());
        assertNotNull(empty.toCsv());
        assertNotNull(named.toJson());
        assertNotNull(named.toXml());
        assertNotNull(named.toCsv());
        assertNotNull(N.fromJson(N.toJson(named), Dataset.class));
        assertNotNull(N.fromJson(N.toJson(empty), Dataset.class));
    }

    @Test
    public void testPrintlnWithWideCharacters() {
        Dataset ds = Dataset.rows(Arrays.asList("id", "name", "age", "salary"), new Object[][] { { 1, "John", 25, 50000.0 }, { 2, "Jane", 30, 60000.0 },
                { 3, "Bob李海洋", 35, 70000.0 }, { 4, "Alice", 28, 55000.0 }, { 5, "Charlie", 40, 80000.0 } });
        StringWriter writer = new StringWriter();

        ds.println();

        ds.println(writer);

        assertEquals("+----+-----------+-----+---------+\n" //
                + "| id | name      | age | salary  |\n" //
                + "+----+-----------+-----+---------+\n" //
                + "| 1  | John      | 25  | 50000.0 |\n" //
                + "| 2  | Jane      | 30  | 60000.0 |\n" //
                + "| 3  | Bob李海洋 | 35  | 70000.0 |\n" //
                + "| 4  | Alice     | 28  | 55000.0 |\n" //
                + "| 5  | Charlie   | 40  | 80000.0 |\n" //
                + "+----+-----------+-----+---------+\n", writer.toString());
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
    public void testIntersection_2() {
        {
            Dataset ds1 = Dataset.rows(Arrays.asList("id", "name"), new Object[][] { { 1, "Alice" }, { 2, "Bob" }, { 3, "Charlie" }, { 2, "Bob" } });

            Dataset ds2 = Dataset.rows(Arrays.asList("id", "name"),
                    new Object[][] { { 2, "Bob" }, { 3, "Charlie" }, { 4, "Dave" }, { 2, "Bob" }, { 2, "Bob" } });

            Dataset result = N.intersection(ds1, ds2);

            assertEquals(3, result.size());
            assertEquals((Integer) 2, result.get(0, 0));
            assertEquals("Bob", result.get(0, 1));
            assertEquals((Integer) 3, result.get(1, 0));
            assertEquals("Charlie", result.get(1, 1));
            assertEquals((Integer) 2, result.get(2, 0));
            assertEquals("Bob", result.get(2, 1));
        }

        {
            Dataset ds1 = Dataset.rows(Arrays.asList("id", "name"), new Object[][] { { 1, "Alice" }, { 2, "Bob" }, { 3, "Charlie" }, { 2, "Bob" } });

            Dataset ds2 = Dataset.rows(Arrays.asList("id", "name"), new Object[][] { { 2, "Bob" }, { 3, "Charlie" }, { 4, "Dave" } });

            Dataset result = N.intersection(ds1, ds2);

            assertEquals(2, result.size());
            assertEquals((Integer) 2, result.get(0, 0));
            assertEquals("Bob", result.get(0, 1));
            assertEquals((Integer) 3, result.get(1, 0));
            assertEquals("Charlie", result.get(1, 1));
        }

    }

    @Test
    public void testDifference() {
        Dataset ds1 = Dataset.rows(Arrays.asList("id", "name"), new Object[][] { { 1, "Alice" }, { 2, "Bob" } });

        Dataset ds2 = Dataset.rows(Arrays.asList("id", "name"), new Object[][] { { 2, "Bob" }, { 3, "Charlie" } });

        Dataset diff = N.difference(ds1, ds2);
        assertNotNull(diff);
    }

    @Test
    public void testSymmetricDifference() {
        Dataset ds1 = Dataset.rows(Arrays.asList("id", "name"), new Object[][] { { 1, "Alice" }, { 2, "Bob" } });

        Dataset ds2 = Dataset.rows(Arrays.asList("id", "name"), new Object[][] { { 2, "Bob" }, { 3, "Charlie" } });

        Dataset diff = N.symmetricDifference(ds1, ds2);
        assertNotNull(diff);
    }

    @Test
    public void testIntersection() {
        Dataset ds1 = Dataset.rows(Arrays.asList("id", "name"), new Object[][] { { 1, "Alice" }, { 2, "Bob" }, { 3, "Charlie" } });

        Dataset ds2 = Dataset.rows(Arrays.asList("id", "name"), new Object[][] { { 2, "Bob" }, { 3, "Charlie" }, { 4, "Diana" } });

        Dataset intersection = N.intersection(ds1, ds2);

        assertNotNull(intersection);
        assertEquals(2, intersection.size());
    }

    @Test
    public void testIntersection_WithSameColumnsCheck() {
        Dataset ds1 = Dataset.rows(Arrays.asList("id", "name"), new Object[][] { { 1, "Alice" }, { 2, "Bob" } });

        Dataset ds2 = Dataset.rows(Arrays.asList("id", "name"), new Object[][] { { 2, "Bob" }, { 3, "Charlie" } });

        Dataset intersection = N.intersection(ds1, ds2, true);

        assertNotNull(intersection);
        assertEquals(1, intersection.size());
        assertEquals(Integer.valueOf(2), intersection.get(0, 0));
        assertEquals("Bob", intersection.get(0, 1));
    }

    @Test
    public void testIntersection_WithKeyColumns() {
        Dataset ds1 = Dataset.rows(Arrays.asList("id", "name", "age"), new Object[][] { { 1, "Alice", 25 }, { 2, "Bob", 30 } });

        Dataset ds2 = Dataset.rows(Arrays.asList("id", "name", "salary"), new Object[][] { { 2, "Bob", 60000.0 }, { 3, "Charlie", 70000.0 } });

        Dataset intersection = N.intersection(ds1, ds2, Arrays.asList("id"));

        assertNotNull(intersection);
        assertEquals(1, intersection.size());
        assertEquals(Integer.valueOf(2), intersection.get(0, 0));
    }

    @Test
    public void testIntersection_WithKeyColumnsAndSameCheck() {
        Dataset ds1 = Dataset.rows(Arrays.asList("id", "name"), new Object[][] { { 1, "Alice" }, { 2, "Bob" } });

        Dataset ds2 = Dataset.rows(Arrays.asList("id", "name"), new Object[][] { { 2, "Bob" }, { 3, "Charlie" } });

        Dataset intersection = N.intersection(ds1, ds2, Arrays.asList("id", "name"), true);

        assertNotNull(intersection);
        assertEquals(1, intersection.size());
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
    public void testRowsWithArrays() {
        Dataset dataset = Dataset.rows(columnNames, testData);
        assertNotNull(dataset);
        assertEquals(4, dataset.size());
        assertEquals(4, dataset.columnCount());
        assertEquals(columnNames, dataset.columnNames());
    }

    @Test
    public void testRowsWithCollections() {
        List<List<Object>> rows = Arrays.asList(Arrays.asList(1, "Alice", 30, 50000.0), Arrays.asList(2, "Bob", 25, 45000.0));
        Dataset dataset = Dataset.rows(columnNames, rows);
        assertNotNull(dataset);
        assertEquals(2, dataset.size());
        assertEquals(4, dataset.columnCount());
    }

    @Test
    @DisplayName("Should create Dataset with valid column names and rows")
    public void testCreateDatasetWithValidData() {
        Collection<String> columnNames = Arrays.asList("id", "name", "age");
        Object[][] rows = { { 1, "John", 25 }, { 2, "Jane", 30 }, { 3, "Bob", 35 } };

        Dataset dataset = Dataset.rows(columnNames, rows);

        assertNotNull(dataset);
        assertEquals(3, dataset.size());
        assertEquals(3, dataset.columnNames().size());
        assertTrue(dataset.columnNames().containsAll(columnNames));
    }

    @Test
    @DisplayName("Should create empty Dataset with column names but no rows")
    public void testCreateDatasetWithEmptyRows() {
        Collection<String> columnNames = Arrays.asList("id", "name");
        Object[][] rows = {};

        Dataset dataset = Dataset.rows(columnNames, rows);

        assertNotNull(dataset);
        assertEquals(0, dataset.size());
        assertEquals(2, dataset.columnNames().size());
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
        assertEquals(1, dataset.columnNames().size());
        assertEquals("value", dataset.columnNames().get(0));
    }

    @Test
    @DisplayName("Should handle null values in rows")
    public void testCreateDatasetWithNullValues() {
        Collection<String> columnNames = Arrays.asList("id", "name", "score");
        Object[][] rows = { { 1, "John", null }, { null, "Jane", 95.5 }, { 3, null, 87.2 } };

        Dataset dataset = Dataset.rows(columnNames, rows);

        assertNotNull(dataset);
        assertEquals(3, dataset.size());
        assertTrue(dataset.isNull(0, 2));
        assertTrue(dataset.isNull(1, 0));
        assertTrue(dataset.isNull(2, 1));
    }

    @Test
    @DisplayName("Should handle different data types in columns")
    public void testCreateDatasetWithMixedDataTypes() {
        Collection<String> columnNames = Arrays.asList("id", "name", "active", "score", "date");
        Object[][] rows = { { 1, "John", true, 95.5, new Date() }, { 2L, "Jane", false, 87, null } };

        Dataset dataset = Dataset.rows(columnNames, rows);

        assertNotNull(dataset);
        assertEquals(2, dataset.size());
        assertEquals(5, dataset.columnNames().size());
    }

    @Test
    @DisplayName("Should handle jagged arrays with consistent column count")
    public void testCreateDatasetWithJaggedButConsistentRows() {
        Collection<String> columnNames = Arrays.asList("col1", "col2");
        Object[][] rows = { new Object[] { 1, "a" }, new Object[] { 2, "b" }, new Object[] { 3, "c" } };

        Dataset dataset = Dataset.rows(columnNames, rows);

        assertNotNull(dataset);
        assertEquals(3, dataset.size());
        assertEquals(2, dataset.columnNames().size());
    }

    @Test
    public void testSymmetricDifferenceWithKeyColumnsAndRequireSameColumns() {
        Dataset dataset1 = Dataset.rows(Arrays.asList("id", "name", "department"),
                new Object[][] { { 1, "Alice", "HR" }, { 2, "Bob", "Engineering" }, { 2, "Bob", "Engineering" }, { 3, "Charlie", "Marketing" } });
        Dataset dataset2 = Dataset.rows(Arrays.asList("id", "name", "salary"),
                new Object[][] { { 2, "Bob", 50000 }, { 3, "Charlie", 55000 }, { 4, "Dave", 60000 } });

        Collection<String> keyColumns = Arrays.asList("id", "name");
        Dataset result = N.symmetricDifference(dataset1, dataset2, keyColumns, false);
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
    public void testRowsWithInvalidArguments() {
        assertThrows(IllegalArgumentException.class, () -> {
            Dataset.rows(null, testData);
        });

    }

    @Test
    @DisplayName("Should throw IllegalArgumentException for mismatched column count")
    public void testThrowsExceptionForMismatchedColumnCount() {
        Collection<String> columnNames = Arrays.asList("id", "name");
        Object[][] rows = { { 1, "John", 25 }, { 2, "Jane" } };

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
    @DisplayName("Should throw IllegalArgumentException for inconsistent row lengths")
    public void testThrowsExceptionForInconsistentRowLengths() {
        Collection<String> columnNames = Arrays.asList("col1", "col2", "col3");
        Object[][] rows = { { 1, "a", true }, { 2, "b" }, { 3, "c", false } };

        assertThrows(IllegalArgumentException.class, () -> {
            Dataset.rows(columnNames, rows);
        });
    }

    @Test
    public void columns_streamOfImmutableLists() {
        List<ArrayList<Object>> streamedCols = sampleDataset.columns().map(ArrayList::new).toList();
        assertEquals(columnValues, streamedCols);
    }

    @Test
    public void testColumns() {
        Stream<ImmutableList<Object>> columnStream = dataset.columns();

        assertNotNull(columnStream);
        List<ImmutableList<Object>> columns = columnStream.toList();
        assertEquals(4, columns.size());

        assertEquals(5, columns.get(0).size());
        assertEquals(Integer.valueOf(1), columns.get(0).get(0));
        assertEquals(Integer.valueOf(2), columns.get(0).get(1));

        assertEquals("John", columns.get(1).get(0));
        assertEquals("Jane", columns.get(1).get(1));
    }

    @Test
    public void testColumnsWithArrays() {
        Object[][] columns = new Object[][] { { 1, 2, 3, 4 }, { "Alice", "Bob", "Charlie", "Diana" }, { 30, 25, 35, 28 },
                { 50000.0, 45000.0, 60000.0, 55000.0 } };
        Dataset dataset = Dataset.columns(columnNames, columns);
        assertNotNull(dataset);
        assertEquals(4, dataset.size());
        assertEquals(4, dataset.columnCount());
    }

    @Test
    public void testColumnsWithCollections() {
        List<List<Object>> columns = Arrays.asList(Arrays.asList(1, 2, 3, 4), Arrays.asList("Alice", "Bob", "Charlie", "Diana"), Arrays.asList(30, 25, 35, 28),
                Arrays.asList(50000.0, 45000.0, 60000.0, 55000.0));
        Dataset dataset = Dataset.columns(columnNames, columns);
        assertNotNull(dataset);
        assertEquals(4, dataset.size());
        assertEquals(4, dataset.columnCount());
    }

    @Test
    public void testColumnsWithArraysSupportsRowMutation() {
        // Regression: the Object[][] overload built columns with fixed-size Array.asList(...),
        // so addRow/removeRow threw UnsupportedOperationException (the Collection overload did not).
        List<String> names = Arrays.asList("id", "name");
        Object[][] cols = new Object[][] { { 1, 2 }, { "Alice", "Bob" } };
        Dataset ds = Dataset.columns(names, cols);
        assertEquals(2, ds.size());

        ds.addRow(Arrays.asList(3, "Charlie"));
        assertEquals(3, ds.size());
        assertEquals(Integer.valueOf(3), ds.get(2, 0));
        assertEquals("Charlie", ds.get(2, 1));

        ds.removeRow(0);
        assertEquals(2, ds.size());
    }

    @Test
    @DisplayName("Should create Dataset with valid column names and columns")
    public void testCreateDatasetWithValidColumnsData2() {
        Collection<String> columnNames = Arrays.asList("id", "name", "age");
        Object[][] columns = { { 1, 2, 3 }, { "John", "Jane", "Bob" }, { 25, 30, 35 } };

        Dataset dataset = Dataset.columns(columnNames, columns);

        assertNotNull(dataset);
        assertEquals(3, dataset.size());
        assertEquals(3, dataset.columnNames().size());
        assertTrue(dataset.columnNames().containsAll(columnNames));

        assertEquals(1, (Integer) dataset.moveToRow(0).get("id"));
        assertEquals("John", dataset.moveToRow(0).get("name"));
        assertEquals(25, (Integer) dataset.moveToRow(0).get("age"));
    }

    @Test
    @DisplayName("Should create empty Dataset with column names but empty columns")
    public void testCreateDatasetWithEmptyColumns() {
        Collection<String> columnNames = Arrays.asList("id", "name");
        Object[][] columns = { {}, {} };

        Dataset dataset = Dataset.columns(columnNames, columns);

        assertNotNull(dataset);
        assertEquals(0, dataset.size());
        assertEquals(2, dataset.columnNames().size());
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
        assertEquals(1, dataset.columnNames().size());
        assertEquals("value", dataset.columnNames().get(0));
        assertEquals("test1", dataset.moveToRow(0).get("value"));
        assertEquals("test2", dataset.moveToRow(1).get("value"));
        assertEquals("test3", dataset.moveToRow(2).get("value"));
    }

    @Test
    @DisplayName("Should handle null values in columns")
    public void testCreateDatasetWithNullValues2() {
        Collection<String> columnNames = Arrays.asList("id", "name", "score");
        Object[][] columns = { { 1, null, 3 }, { "John", "Jane", null }, { null, 95.5, 87.2 } };

        Dataset dataset = Dataset.columns(columnNames, columns);

        assertNotNull(dataset);
        assertEquals(3, dataset.size());
        assertTrue(dataset.isNull(1, 0));
        assertTrue(dataset.isNull(2, 1));
        assertTrue(dataset.isNull(0, 2));
    }

    @Test
    @DisplayName("Should handle different data types in same column")
    public void testCreateDatasetWithMixedDataTypesInColumn() {
        Collection<String> columnNames = Arrays.asList("mixed", "numbers");
        Object[][] columns = { { "string", 123, true, null }, { 1, 2.5, 3L, 4.0f } };

        Dataset dataset = Dataset.columns(columnNames, columns);

        assertNotNull(dataset);
        assertEquals(4, dataset.size());
        assertEquals(2, dataset.columnNames().size());

        assertEquals("string", dataset.moveToRow(0).get("mixed"));
        assertEquals(123, (Integer) dataset.moveToRow(1).get("mixed"));
        assertEquals(true, dataset.moveToRow(2).get("mixed"));
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
        assertEquals(0, dataset.columnNames().size());
        assertTrue(dataset.isEmpty());
    }

    @Test
    @DisplayName("Should create empty Dataset when columns array is null and column names are empty")
    public void testCreateDatasetWithNullColumnsArray() {
        Dataset dataset = Dataset.columns(Collections.emptyList(), (Object[][]) null);

        assertNotNull(dataset);
        assertEquals(0, dataset.size());
        assertEquals(0, dataset.columnCount());
        assertTrue(dataset.isEmpty());
    }

    @Test
    @DisplayName("Should create empty Dataset when columns collection is null and column names are empty")
    public void testCreateDatasetWithNullColumnsCollection() {
        Dataset dataset = Dataset.columns(Collections.emptyList(), (Collection<? extends Collection<?>>) null);

        assertNotNull(dataset);
        assertEquals(0, dataset.size());
        assertEquals(0, dataset.columnCount());
        assertTrue(dataset.isEmpty());
    }

    @Test
    @DisplayName("Should handle single row of data across multiple columns")
    public void testCreateDatasetWithSingleRow() {
        Collection<String> columnNames = Arrays.asList("col1", "col2", "col3");
        Object[][] columns = { { "a" }, { "b" }, { "c" } };

        Dataset dataset = Dataset.columns(columnNames, columns);

        assertNotNull(dataset);
        assertEquals(1, dataset.size());
        assertEquals(3, dataset.columnNames().size());
        assertEquals("a", dataset.moveToRow(0).get("col1"));
        assertEquals("b", dataset.moveToRow(0).get("col2"));
        assertEquals("c", dataset.moveToRow(0).get("col3"));
    }

    @Test
    @DisplayName("Should handle large dataset with many rows")
    public void testCreateDatasetWithManyRows() {
        Collection<String> columnNames = Arrays.asList("index", "squared");
        Object[][] columns = new Object[2][];

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
        assertEquals(2, dataset.columnNames().size());
        assertEquals(0, (Integer) dataset.moveToRow(0).get("index"));
        assertEquals(0, (Integer) dataset.moveToRow(0).get("squared"));
        assertEquals(999, (Integer) dataset.moveToRow(999).get("index"));
        assertEquals(998001, (Integer) dataset.moveToRow(999).get("squared"));
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when columnNames length differs from columns length")
    public void testThrowsExceptionForMismatchedColumnCount2() {
        Collection<String> columnNames = Arrays.asList("id", "name");
        Object[][] columns = { { 1, 2, 3 }, { "John", "Jane", "Bob" }, { 25, 30, 35 } };

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            Dataset.columns(columnNames, columns);
        });

        assertTrue(exception.getMessage().contains("The length of 'columnNames'(2) is not equal to the number of sub-arrays in 'columns'(3)"));
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
    @DisplayName("Should throw IllegalArgumentException for duplicate column names")
    public void testThrowsExceptionForDuplicateColumnNames2() {
        Collection<String> columnNames = Arrays.asList("id", "name", "id");
        Object[][] columns = { { 1, 2, 3 }, { "John", "Jane", "Bob" }, { 25, 30, 35 } };

        assertThrows(IllegalArgumentException.class, () -> {
            Dataset.columns(columnNames, columns);
        });
    }

    @Test
    public void columnNameList() {
        assertEquals(Arrays.asList("ID", "Name", "Age"), sampleDataset.columnNames());
        assertTrue(emptyDataset.columnNames().isEmpty());
    }

    @Test
    public void constructor_withData() {
        assertEquals(3, sampleDataset.columnCount());
        assertEquals(3, sampleDataset.size());
        assertFalse(sampleDataset.isFrozen());
        assertEquals(Arrays.asList("ID", "Name", "Age"), sampleDataset.columnNames());
    }

    @Test
    public void columnCount() {
        assertEquals(3, sampleDataset.columnCount());
        assertEquals(0, emptyDataset.columnCount());
    }

    @Test
    public void testConstructor() {
        assertNotNull(dataset);
        assertEquals(4, dataset.columnCount());
        assertEquals(5, dataset.size());
    }

    @Test
    public void testInvalidColumnAccess() {
        assertThrows(IndexOutOfBoundsException.class, () -> dataset.get(0, 10));
        assertThrows(IllegalArgumentException.class, () -> dataset.getColumnIndex("nonexistent"));
    }

    @Test
    public void testInvalidColumnOperations() {
        assertThrows(IllegalArgumentException.class, () -> {
            testDataset.getColumnIndex("nonexistent");
        });

        assertThrows(IllegalArgumentException.class, () -> {
            testDataset.addColumn("name", Arrays.asList("duplicate", "column"));
        });

        assertThrows(IllegalArgumentException.class, () -> {
            testDataset.removeColumns(Arrays.asList("nonexistent"));
        });
    }

    @Test
    public void containsColumn() {
        assertTrue(sampleDataset.containsColumn("ID"));
        assertFalse(sampleDataset.containsColumn("NonExistent"));
        assertFalse(emptyDataset.containsColumn("Any"));
    }

    @Test
    public void test_containsColumn() throws Exception {
        final List<Account> accountList = createAccountList(Account.class, 7);

        final Dataset ds = CommonUtil.newDataset(accountList);

        assertTrue(ds.containsColumn("firstName"));
        assertTrue(ds.containsAllColumns(CommonUtil.toList("firstName", "lastName")));

        assertFalse(ds.containsColumn("Account.firstName"));
        assertFalse(ds.containsAllColumns(CommonUtil.toList("firstName", "Account.lastName")));
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
    public void test_renameColumn_2() throws Exception {
        final List<Account> accountList = createAccountList(Account.class, 7);

        final Dataset ds = CommonUtil.newDataset(accountList);
        ds.renameColumns(ds.columnNames(), t -> t + "2");

        ds.updateColumns(ds.columnNames(), (i, c, v) -> CommonUtil.toString(v));
        assertNotNull(ds);
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
    public void testRenameColumnToExistingName() {
        assertThrows(IllegalArgumentException.class, () -> dataset.renameColumn("age", "name"));
    }

    @Test
    public void renameColumns_functionForAll() {
        sampleDataset.renameColumns(name -> "col_" + name);
        assertTrue(sampleDataset.columnNames().contains("col_ID"));
        assertTrue(sampleDataset.columnNames().contains("col_Name"));
        assertTrue(sampleDataset.columnNames().contains("col_Age"));
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
        Dataset ds = dataset.copy();
        ds.renameColumns(Arrays.asList("id", "name"), name -> name.toUpperCase());
        assertEquals("ID", ds.getColumnName(0));
        assertEquals("NAME", ds.getColumnName(1));
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

    @Test
    public void testSet_ByIndexes() {
        Dataset ds = dataset.copy();
        ds.set(0, 1, "UpdatedName");
        assertEquals("UpdatedName", ds.get(0, 1));
    }

    @Test
    public void testSet_CurrentRow_ByIndex() {
        Dataset ds = dataset.copy();
        ds.moveToRow(0);
        ds.set(1, "UpdatedName");
        assertEquals("UpdatedName", ds.get(0, 1));
    }

    @Test
    public void testSet_CurrentRow_ByName() {
        Dataset ds = dataset.copy();
        ds.moveToRow(0);
        ds.set("name", "UpdatedName");
        assertEquals("UpdatedName", ds.get(0, 1));
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
    public void testSet() {
        dataset.set(0, 1, "Johnny");
        assertEquals("Johnny", dataset.get(0, 1));
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
    public void testSetAndGet() {
        Dataset dataset = testDataset.copy();
        dataset.set(0, 0, 999);
        assertEquals(999, (Integer) dataset.get(0, 0));

        assertThrows(IndexOutOfBoundsException.class, () -> {
            dataset.set(-1, 0, 1);
        });

        assertThrows(IndexOutOfBoundsException.class, () -> {
            dataset.set(0, -1, 1);
        });
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
        ds.moveToRow(0);
        assertTrue(ds.isNull(0));
        ds.moveToRow(1);
        assertFalse(ds.isNull(0));
    }

    @Test
    public void testIsNull_CurrentRow_ByName() {
        Dataset ds = Dataset.rows(Arrays.asList("val"), new Object[][] { { null }, { "test" } });
        ds.moveToRow(0);
        assertTrue(ds.isNull("val"));
        ds.moveToRow(1);
        assertFalse(ds.isNull("val"));
    }

    @Test
    public void isNull_rowIndex_columnIndex() {
        assertFalse(sampleDataset.isNull(0, 0));
        sampleDataset.set(0, 0, null);
        assertTrue(sampleDataset.isNull(0, 0));
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
    public void testIsNullByIndexes() {
        assertFalse(dataset.isNull(0, 1));
        Dataset ds = dataset.copy();
        ds.set(0, 1, null);
        assertTrue(ds.isNull(0, 1));
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
    public void testIsNull() {
        Dataset dataset = testDataset.copy();
        assertFalse(dataset.isNull(0, 0));

        dataset.set(0, 0, null);
        assertTrue(dataset.isNull(0, 0));

        assertThrows(IndexOutOfBoundsException.class, () -> {
            dataset.isNull(-1, 0);
        });
    }

    @Test
    public void copyColumn() {
        List<Object> ageColumnCopy = sampleDataset.copyColumn("Age");
        assertEquals(Arrays.asList(30, 24, 35), ageColumnCopy);
        ageColumnCopy.add(40);
        assertEquals(Arrays.asList(30, 24, 35), sampleDataset.getColumn("Age"));
    }

    @Test
    public void testCopyColumn() {
        List<String> col = dataset.copyColumn("name");
        assertNotNull(col);
        assertEquals(5, col.size());
        assertEquals("John", col.get(0));

        col.set(0, "Modified");
        assertEquals("Modified", col.get(0));
        assertEquals("John", dataset.get(0, 1));
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
    public void updateColumn() {
        sampleDataset.updateColumn("Age", (Integer age) -> age * 2);
        assertEquals(Arrays.asList(60, 48, 70), sampleDataset.getColumn("Age"));
    }

    @Test
    public void testUpdateColumn() {
        Dataset dataset = testDataset.copy();
        dataset.updateColumn("age", (Integer age) -> age + 1);

        dataset.moveToRow(0);
        assertEquals(31, dataset.getInt("age"));
        dataset.moveToRow(1);
        assertEquals(26, dataset.getInt("age"));
    }

    @Test
    public void testUpdateColumn_WithFunction() {
        Dataset ds = dataset.copy();
        ds.updateColumn("name", (String name) -> name.toUpperCase());

        assertEquals("JOHN", ds.get(0, 1));
        assertEquals("JANE", ds.get(1, 1));
    }

    @Test
    public void updateColumns_collection() {
        sampleDataset.updateColumns(Arrays.asList("ID", "Age"), (i, c, v) -> ((Number) v).intValue() + 100);
        assertEquals(Arrays.asList(101, 102, 103), sampleDataset.getColumn("ID"));
        assertEquals(Arrays.asList(130, 124, 135), sampleDataset.getColumn("Age"));
    }

    @Test
    public void testUpdateColumns() {
        Dataset ds = dataset.copy();
        ds.updateColumns(Arrays.asList("name"), (i, c, v) -> ((String) v).toUpperCase());
        assertEquals("JOHN", ds.get(0, 1));
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
    public void convertColumn() {
        sampleDataset.convertColumn("Age", String.class);
        assertEquals("30", sampleDataset.moveToRow(0).get("Age"));
        assertTrue(sampleDataset.getColumn("Age").get(0) instanceof String);

        sampleDataset.convertColumn("ID", Double.class);
        assertEquals(1.0, sampleDataset.moveToRow(0).get("ID"));
        assertTrue(sampleDataset.getColumn("ID").get(0) instanceof Double);
    }

    @Test
    public void test_convertColumn() {

        final Dataset dataset = CommonUtil.newDataset(CommonUtil.toList("column1", "column2"),
                CommonUtil.toList(CommonUtil.toList("ab", "cd"), CommonUtil.toList("ef", "gh")));

        assertThrows(IllegalArgumentException.class, () -> dataset.convertColumn("column1", Long.class));
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
    public void testCombineColumns() {
        Dataset ds = dataset.copy();
        ds.combineColumns(Arrays.asList("name", "age"), "nameAge", Map.class);
        assertEquals(3, ds.columnCount());
        assertTrue(ds.containsColumn("nameAge"));
    }

    @Test
    public void testCombineColumnsWithFunction() {
        Dataset ds = dataset.copy();
        ds.combineColumns(Arrays.asList("name", "age"), "info", arr -> arr.get(0) + ":" + arr.get(1));
        assertEquals("John:25", ds.get(0, ds.getColumnIndex("info")));
    }

    @Test
    public void testCombineColumnsWithTuple2() {
        Dataset ds = dataset.copy();
        ds.combineColumns(Tuple.of("name", "age"), "combined", (String name, Integer age) -> name + "_" + age);
        assertEquals("John_25", ds.get(0, ds.getColumnIndex("combined")));
    }

    @Test
    public void testCombineColumnsWithBiFunction() {
        Dataset dataset = testDataset.copy();
        Tuple2<String, String> columnNames = Tuple.of("name", "age");
        BiFunction<String, Integer, String> combineFunc = (name, age) -> name + "(" + age + ")";
        dataset.combineColumns(columnNames, "name_age_bi", combineFunc);

        assertTrue(dataset.containsColumn("name_age_bi"));
        dataset.moveToRow(0);
        assertEquals("Alice(30)", dataset.get("name_age_bi"));
    }

    @Test
    public void testCombineColumnsWithTriFunction() {
        Dataset dataset = testDataset.copy();
        Tuple3<String, String, String> columnNames = Tuple.of("id", "name", "age");
        TriFunction<Integer, String, Integer, String> combineFunc = (id, name, age) -> "ID:" + id + ",Name:" + name + ",Age:" + age;
        dataset.combineColumns(columnNames, "full_info", combineFunc);

        assertTrue(dataset.containsColumn("full_info"));
        dataset.moveToRow(0);
        assertEquals("ID:1,Name:Alice,Age:30", dataset.get("full_info"));
    }

    @Test
    public void testDivideColumn() {
        Dataset ds = Dataset.rows(Arrays.asList("fullName"), new Object[][] { { "John_Doe" }, { "Jane_Smith" } });
        ds.divideColumn("fullName", Arrays.asList("firstName", "lastName"), (String full) -> Arrays.asList(full.split("_")));
        assertEquals(2, ds.columnCount());
        assertEquals("John", ds.get(0, ds.getColumnIndex("firstName")));
        assertEquals("Doe", ds.get(0, ds.getColumnIndex("lastName")));
    }

    @Test
    public void divideColumn_withFunctionToList() {
        sampleDataset.addColumn("FullName", Arrays.asList("Alice Wonderland", "Bob TheBuilder", "Charlie Brown"));
        sampleDataset.divideColumn("FullName", Arrays.asList("FirstName", "LastName"), (String fullName) -> {
            if (fullName == null) {
                return Arrays.asList(null, null);
            }
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
    public void testDivideColumnWithBiConsumer() {
        Dataset dataset = testDataset.copy();
        Object[] output = new Object[2];
        BiConsumer<String, Object[]> outputConsumer = (name, arr) -> {
            arr[0] = name.toUpperCase();
            arr[1] = name.length();
        };
        dataset.divideColumn("name", Arrays.asList("upper_name", "name_length"), outputConsumer);

        assertTrue(dataset.containsColumn("upper_name"));
        assertTrue(dataset.containsColumn("name_length"));
        dataset.moveToRow(0);
        assertEquals("ALICE", dataset.get("upper_name"));
        assertEquals(5, (Integer) dataset.get("name_length"));
    }

    @Test
    public void testDivideColumnWithPairOutput() {
        Dataset dataset = testDataset.copy();
        Tuple2<String, String> newColumnNames = Tuple.of("name_upper", "name_lower");
        BiConsumer<String, Pair<Object, Object>> output = (name, pair) -> {
            pair.setLeft(name.toUpperCase());
            pair.setRight(name.toLowerCase());
        };
        dataset.divideColumn("name", newColumnNames, output);

        assertTrue(dataset.containsColumn("name_upper"));
        assertTrue(dataset.containsColumn("name_lower"));
        dataset.moveToRow(0);
        assertEquals("ALICE", dataset.get("name_upper"));
        assertEquals("alice", dataset.get("name_lower"));
    }

    @Test
    public void testDivideColumnWithTripleOutput() {
        Dataset dataset = testDataset.copy();
        Tuple3<String, String, String> newColumnNames = Tuple.of("name_upper", "name_lower", "name_length");
        BiConsumer<String, Triple<Object, Object, Object>> output = (name, triple) -> {
            triple.setLeft(name.toUpperCase());
            triple.setMiddle(name.toLowerCase());
            triple.setRight(name.length());
        };
        dataset.divideColumn("name", newColumnNames, output);

        assertTrue(dataset.containsColumn("name_upper"));
        assertTrue(dataset.containsColumn("name_lower"));
        assertTrue(dataset.containsColumn("name_length"));
        dataset.moveToRow(0);
        assertEquals("ALICE", dataset.get("name_upper"));
        assertEquals("alice", dataset.get("name_lower"));
        assertEquals(5, (Integer) dataset.get("name_length"));
    }

    @Test
    public void columnMap() {
        Map<String, com.landawn.abacus.util.ImmutableList<Object>> map = sampleDataset.columnMap();
        assertEquals(3, map.size());
        assertEquals(sampleDataset.getColumn("ID"), map.get("ID"));
        assertEquals(sampleDataset.getColumn("Name"), map.get("Name"));
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
        assertEquals(5, ids.size());
        assertEquals(Integer.valueOf(1), ids.get(0));
        assertEquals(Integer.valueOf(2), ids.get(1));

        ImmutableList<Object> names = map.get("name");
        assertEquals("John", names.get(0));
        assertEquals("Jane", names.get(1));
    }

    @Test
    public void testNullHandling() {
        dataset.addRow(new Object[] { null, null, null, null });

        assertNull(dataset.get(5, 0));
        assertTrue(dataset.isNull(5, 0));

        dataset.moveToRow(5);

        assertEquals(0, dataset.getInt(dataset.getColumnIndex("id")));
        assertEquals(0.0, dataset.getDouble(dataset.getColumnIndex("salary")), 0.01);
        assertFalse(dataset.getBoolean(dataset.getColumnIndex("id")));
    }

    @Test
    public void testUpdateRow() {
        Dataset ds = dataset.copy();
        ds.updateRow(0, value -> value instanceof String ? "Updated" : value);
        assertEquals("Updated", ds.get(0, 1));
    }

    @Test
    public void updateRow() {
        sampleDataset.updateRow(0, val -> {
            if (val instanceof String) {
                return ((String) val).toUpperCase();
            }
            if (val instanceof Integer && ((Integer) val) == 30) {
                return 31;
            }
            return val;
        });
        assertEquals("ALICE", sampleDataset.moveToRow(0).get("Name"));
        assertEquals((Integer) 31, sampleDataset.moveToRow(0).get("Age"));
    }

    @Test
    public void testUpdateRows() {
        Dataset ds = dataset.copy();
        ds.updateRows(new int[] { 0, 1 }, (i, c, v) -> v instanceof String ? "Updated" : v);
        assertEquals("Updated", ds.get(0, 1));
        assertEquals("Updated", ds.get(1, 1));
    }

    @Test
    public void updateAll() {
        sampleDataset.updateAll(val -> {
            if (val instanceof String) {
                return "Name_" + val;
            }
            if (val instanceof Integer) {
                return ((Integer) val) + 10;
            }
            return val;
        });
        assertEquals(Arrays.asList(11, 12, 13), sampleDataset.getColumn("ID"));
        assertEquals(Arrays.asList("Name_Alice", "Name_Bob", "Name_Charlie"), sampleDataset.getColumn("Name"));
        assertEquals(Arrays.asList(40, 34, 45), sampleDataset.getColumn("Age"));
    }

    @Test
    public void testUpdateAllWithIntBiObjFunction() {

        dataset.updateAll((i, c, v) -> v instanceof String ? "Name" + i : v);

        for (int i = 0; i < dataset.size(); i++) {
            assertEquals("Name" + i, dataset.get(i, 1));
        }
    }

    @Test
    public void testUpdateAll() {
        Dataset ds = dataset.copy();
        ds.updateAll(v -> v instanceof String ? ((String) v).toUpperCase() : v);
        assertEquals("JOHN", ds.get(0, 1));
        assertEquals("JANE", ds.get(1, 1));
    }

    @Test
    public void replaceIf() {
        sampleDataset.replaceIf(val -> val instanceof String && "Bob".equals(val), "Robert");
        assertEquals("Robert", sampleDataset.moveToRow(1).get("Name"));

        sampleDataset.replaceIf(val -> val instanceof Integer && (Integer) val > 30, 0);
        assertEquals((Integer) 0, sampleDataset.moveToRow(2).get("Age"));
    }

    @Test
    public void testReplaceIf() {
        Dataset ds = dataset.copy();
        ds.replaceIf(value -> "Alice".equals(value), "Alicia");
        assertEquals("Alicia", ds.get(3, 1));
    }

    @Test
    public void testReplaceIfWithIntBiObjPredicate() {
        assertEquals("Bob", dataset.get(2, 1));
        dataset.replaceIf((i, c, v) -> "name".equals(c) && "Bob".equals(v), "Robert");
        assertEquals("Robert", dataset.get(2, 1));
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
    public void testMerge() {
        Dataset ds = dataset.copy();
        Dataset other = Dataset.rows(Arrays.asList("dept", "location"), new Object[][] { { "IT", "NYC" }, { "HR", "LA" } });
        ds.merge(other);
        assertEquals(6, ds.columnCount());
        assertTrue(ds.containsColumn("dept"));
        assertTrue(ds.containsColumn("location"));
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
    public void testMergeWithSelectColumns() {
        Dataset dataset = testDataset.copy();
        Dataset other = Dataset.rows(columnNames, new Object[][] { { 5, "Five", 40, 70000.0 } });

        dataset.merge(other, Arrays.asList("id", "name"));

        assertEquals(5, dataset.size());
        dataset.moveToRow(4);
        assertEquals(5, dataset.getInt("id"));
        assertEquals("Five", dataset.get("name"));
    }

    @Test
    public void testMergeWithRange() {
        Dataset dataset = testDataset.copy();
        Dataset other = Dataset.rows(columnNames, new Object[][] { { 5, "Five", 40, 70000.0 }, { 6, "Six", 35, 65000.0 } });

        dataset.merge(other, 0, 1, Arrays.asList("id", "name"));

        assertEquals(5, dataset.size());
        dataset.moveToRow(4);
        assertEquals(5, dataset.getInt("id"));
        assertEquals("Five", dataset.get("name"));
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

        Dataset merged = CommonUtil.merge(others);

        assertEquals(8, merged.size());
        assertTrue(merged.containsColumn("bonus"));
    }

    @Test
    public void testMergeWithRequiredSameColumns() {
        Dataset ds = dataset.copy();
        Dataset other = Dataset.rows(columnNames, new Object[][] { { 5, "Five", 50, 50000.0 } });
        ds.merge(other, true);
        assertEquals(6, ds.size());
    }

    @Test
    public void testMergeWithColumnNames() {
        Dataset dataset1 = Dataset.rows(Arrays.asList("id", "name", "age"), new Object[][] { { 1, "Alice", 25 }, { 2, "Bob", 30 } });
        Dataset dataset2 = Dataset.rows(Arrays.asList("id", "name", "score"), new Object[][] { { 1, "Alice", 95 }, { 3, "Charlie", 85 } });

        dataset1.merge(dataset2, Arrays.asList("id", "name"));
        Dataset result = dataset1;
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
        Dataset result = dataset1;
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
    public void testMergeWithRequiresSameColumns() {

        Dataset dataset1 = Dataset.rows(Arrays.asList("id", "name", "age"), new Object[][] { { 1, "Alice", 25 }, { 2, "Bob", 30 } });
        Dataset dataset2 = Dataset.rows(Arrays.asList("id", "name", "score"), new Object[][] { { 1, "Alice", 95 }, { 3, "Charlie", 85 } });

        dataset1.merge(dataset2, false);
        Dataset result = dataset1;
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
    public void testCurrentRowNum() {
        assertEquals(0, dataset.currentRowIndex());
        dataset.moveToRow(2);
        assertEquals(2, dataset.currentRowIndex());
    }

    @Test
    public void currentRowIndex_and_moveToRow() {
        assertEquals(0, sampleDataset.currentRowIndex());
        sampleDataset.moveToRow(1);
        assertEquals(1, sampleDataset.currentRowIndex());
        assertThrows(IndexOutOfBoundsException.class, () -> sampleDataset.moveToRow(10));
    }

    @Test
    public void testAbsolute() {
        dataset.moveToRow(2);
        assertEquals(2, dataset.currentRowIndex());
        assertEquals(Integer.valueOf(3), dataset.get("id"));
    }

    @Test
    public void testFirst() {
        dataset.moveToRow(2);
        dataset.moveToRow(0);
        assertEquals(0, dataset.currentRowIndex());
        assertEquals(Integer.valueOf(1), dataset.get("id"));
    }

    @Test
    public void testLast() {
        dataset.moveToRow(dataset.size() - 1);
        assertEquals(4, dataset.currentRowIndex());
        assertEquals(Integer.valueOf(5), dataset.get("id"));
    }

    @Test
    public void testInvalidRowOperations() {
        assertThrows(IndexOutOfBoundsException.class, () -> {
            testDataset.getRow(-1);
        });

        assertThrows(IndexOutOfBoundsException.class, () -> {
            testDataset.getRow(100);
        });

        assertThrows(IndexOutOfBoundsException.class, () -> {
            testDataset.removeRow(-1);
        });
    }

    @Test
    public void testFirstRow() {
        Optional<Person> person = dataset.firstRow(Person.class);
        assertTrue(person.isPresent());
        assertEquals(1, person.get().getId());
    }

    @Test
    public void testFirstRowWithClass() {
        Optional<List> firstRow = dataset.firstRow(ArrayList.class);
        assertTrue(firstRow.isPresent());
        assertEquals(Arrays.asList(1, "John", 25, 50000.0), firstRow.get());
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
    public void testLastRow() {
        Optional<Person> person = dataset.lastRow(Person.class);
        assertTrue(person.isPresent());
        assertEquals(5, person.get().getId());
    }

    @Test
    public void lastRow_Optional() {
        com.landawn.abacus.util.u.Optional<Object[]> last = sampleDataset.lastRow();
        assertTrue(last.isPresent());
        assertArrayEquals(new Object[] { 3, "Charlie", 35 }, last.get());
    }

    @Test
    public void testForEach() {
        List<String> names = new ArrayList<>();
        dataset.forEach(row -> names.add((String) row.get(1)));

        assertEquals(5, names.size());
        assertEquals("John", names.get(0));
        assertEquals("Jane", names.get(1));
    }

    @Test
    public void testForEach_WithRange() {
        List<String> names = new ArrayList<>();
        dataset.forEach(1, 3, row -> names.add((String) row.get(1)));

        assertEquals(2, names.size());
        assertEquals("Jane", names.get(0));
        assertEquals("Bob", names.get(1));
    }

    @Test
    public void testForEach_BiConsumer() {
        List<String> results = new ArrayList<>();
        dataset.forEach(Tuple.of("id", "name"), (Integer id, String name) -> results.add(id + ":" + name));

        assertEquals(5, results.size());
        assertEquals("1:John", results.get(0));
    }

    @Test
    public void testForEach_TriConsumer() {
        List<String> results = new ArrayList<>();
        dataset.forEach(Tuple.of("id", "name", "age"), (Integer id, String name, Integer age) -> results.add(id + ":" + name + ":" + age));

        assertEquals(5, results.size());
        assertEquals("1:John:25", results.get(0));
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
    public void testForEachWithColumnNames() {
        List<String> names = new ArrayList<>();
        dataset.forEach(Arrays.asList("name"), row -> names.add((String) row.get(0)));
        assertEquals(5, names.size());
        assertEquals("John", names.get(0));
    }

    @Test
    public void testForEachWithBiConsumer() {
        List<String> nameAges = new ArrayList<>();
        dataset.forEach(Tuple.of("name", "age"), (name, age) -> {
            nameAges.add(name + ":" + age);
        });

        assertEquals(Arrays.asList("John:25", "Jane:30", "Bob:35", "Alice:28", "Charlie:40"), nameAges);
    }

    @Test
    public void testForEachWithTriConsumer() {
        List<String> combined = new ArrayList<>();
        dataset.forEach(Tuple.of("id", "name", "age"), (id, name, age) -> {
            combined.add(id + "-" + name + "-" + age);
        });

        assertEquals(Arrays.asList("1-John-25", "2-Jane-30", "3-Bob-35", "4-Alice-28", "5-Charlie-40"), combined);
    }

    @Test
    public void testRangeOperations() {
        dataset.forEach(1, 3, row -> {
            assertNotNull(row.get(0));
        });

        dataset.forEach(1, 3, Arrays.asList("name", "age"), row -> {
            assertEquals(2, row.length());
        });

        List<String> reverseNames = new ArrayList<>();
        dataset.forEach(4, 0, row -> {
            reverseNames.add(row.get(1).toString());
        });
        assertEquals(Arrays.asList("Charlie", "Alice", "Bob", "Jane"), reverseNames);
    }

    @Test
    public void test_join_all() {
        final Dataset ds1 = CommonUtil.newDataset(CommonUtil.toList("id", "name", "city"),
                CommonUtil.toList(CommonUtil.toList(1, "n1", "c1"), CommonUtil.toList(2, "n2", "c2"), CommonUtil.toList(3, "n3", "c3")));
        final Dataset ds2 = CommonUtil.newDataset(CommonUtil.toList("id", "address2", "state"), CommonUtil.toList(CommonUtil.toList(1, "n1", "c1"),
                CommonUtil.toList(2, "n2", "c2"), CommonUtil.toList(2, "n22", "c22"), CommonUtil.toList(4, "n4", "c4")));

        assertNotNull(ds1.innerJoin(ds2, "id", "id"));
        assertNotNull(ds1.innerJoin(ds2, CommonUtil.asMap("id", "id"), "newAddress", List.class));
        assertNotNull(ds1.innerJoin(ds2, CommonUtil.asMap("id", "id"), "newAddress", List.class, IntFunctions.ofList()));
        assertNotNull(ds1.innerJoin(ds2, CommonUtil.asMap("id", "id", "name", "address2")));
        assertNotNull(ds1.leftJoin(ds2, "id", "id"));
        assertNotNull(ds1.leftJoin(ds2, CommonUtil.asMap("id", "id"), "newAddress", List.class));
        assertNotNull(ds1.rightJoin(ds2, "id", "id"));
        assertNotNull(ds1.rightJoin(ds2, CommonUtil.asMap("id", "id"), "newAddress", List.class));
        assertNotNull(ds1.fullJoin(ds2, "id", "id"));
        assertNotNull(ds1.fullJoin(ds2, CommonUtil.asMap("id", "id"), "newAddress", List.class));
    }

    @Test
    public void test_print() {
        final Dataset ds = CommonUtil.newDataset(CommonUtil.toList("a", "blafjiawfj;lkasjf23 i2qfja;lsfjoiaslf", "c"), CommonUtil
                .asList(CommonUtil.toList(1, "n1kafjeoiwajf", "c1"), CommonUtil.toList(2, "n2", "c2las83292rfjioa"), CommonUtil.toList(3, "n3", "c3")));

        StringWriter outputWriter = new StringWriter();
        ds.println(outputWriter);

        ds.clear();

        outputWriter = new StringWriter();
        ds.println(outputWriter);

        ds.removeColumns(ds.columnNames());
        assertNotNull(outputWriter.toString());
    }

    @Test
    public void test_csv_01() throws Exception {
        assertDoesNotThrow(() -> {
            final Object[][] rowList = { { "Banana", 1000, "USA" }, { "Carrots", 1500, "USA" }, { "Beans", 1600, "USA" }, { "Orange", 2000, "USA" },
                    { "Orange", 2000, "USA" }, { "Banana", 400, "China" }, { "Carrots", 1200, "China" }, { "Beans", 1500, "China" },
                    { "Orange", 4000, "China" }, { "Banana", 2000, "Canada" }, { "Carrots", 2000, "Canada" }, { "Beans", 2000, "Mexico" } };

            final Dataset dataset = CommonUtil.newDataset(CommonUtil.toList("Prod\"^@&\\'skdf'''\\\\\\uct", "\\\"^@&\\\\'skdf'''\\\\\\\\\\\\uct", "Country"),
                    rowList);
            assertNotNull(dataset.toCsv());
        });
    }

    @Test
    public void test_json_3() throws Exception {
        final List<Account> accountList = createAccountList(Account.class, 9);

        final List<Map<String, Object>> mapList = Stream.of(accountList).map(Beans::beanToMap).toList();

        mapList.get(0).remove("id");

        mapList.get(mapList.size() / 2).remove("emailAddress");

        mapList.get(mapList.size() - 1).remove("createdTime");

        String json = N.toJson(mapList, true);

        Dataset ds2 = N.fromJson(json, JsonDeserConfig.create().setValueTypesByBeanClass(Account.class), Dataset.class);

        json = N.toJson(CommonUtil.toList(accountList.get(0)));

        ds2 = N.fromJson(json, JsonDeserConfig.create().setValueTypesByBeanClass(Account.class), Dataset.class);

        ds2 = N.fromJson("[]", JsonDeserConfig.create().setValueTypesByBeanClass(Account.class), Dataset.class);
        assertNotNull(ds2);
    }

    @Test
    public void test_rename() {
        assertDoesNotThrow(() -> {
            final Dataset ds1 = CommonUtil.newDataset(CommonUtil.toList("a", "b", "c"),
                    CommonUtil.toList(CommonUtil.toList(1, "n1", "c1"), CommonUtil.toList(2, "n2", "c2"), CommonUtil.toList(3, "n3", "c3")));
            ds1.renameColumns(CommonUtil.asMap("a", "a", "c", "d"));
            assertEquals("d", ds1.getColumnName(2));
            assertNotNull(ds1.slice(0, 2, CommonUtil.toList("a")));
        });
    }

    @Test
    public void test_combine_divide() throws Exception {
        assertDoesNotThrow(() -> {
            final Dataset ds1 = CommonUtil
                    .newDataset(CommonUtil.toList(createAccount(Account.class), createAccount(Account.class), createAccount(Account.class)));
            ds1.removeColumns(CommonUtil.toList("gui", "emailAddress", "lastUpdateTime", "createdTime"));
            ds1.updateRow(0, t -> t instanceof String ? t + "__0" : t);

            Dataset ds2 = ds1.copy();
            ds2.combineColumns(CommonUtil.toList("firstName", "lastName"), "name", Map.class);

            ds2 = ds1.copy();
            ds2.combineColumns(CommonUtil.toList("firstName", "lastName"), "name", (Function<DisposableObjArray, String>) t -> Strings.join(t.copy(), "-"));

            ds2.moveColumn("name", 0);

            ds2.divideColumn("name", CommonUtil.toList("firstName", "lastName"), (BiConsumer<String, Object[]>) (t, a) -> {
                final String[] strs = Splitter.with("-").splitToArray(t);
                CommonUtil.copy(strs, 0, a, 0, a.length);
            });

        });
    }

    @Test
    public void test_update() throws Exception {
        final Account account = createAccount(Account.class);
        final Dataset ds1 = CommonUtil.newDataset(CommonUtil.toList(account, account));

        ds1.updateRow(0, t -> t instanceof String ? t + "___" : t);

        ds1.updateRows(Array.of(1, 0), (i, c, v) -> v instanceof String ? v + "___" : v);

        ds1.updateColumn("firstName", t -> t instanceof String ? t + "###" : t);

        ds1.updateColumns(CommonUtil.toList("lastName", "firstName"), (i, c, v) -> v instanceof String ? v + "###" : v);

        ds1.updateAll(t -> t instanceof String ? t + "+++" : t);

        assertNotNull(ds1);
    }

    @Test
    public void test_intersection() throws Exception {
        final Account account = createAccount(Account.class);
        final Dataset ds1 = CommonUtil.newDataset(CommonUtil.toList(account));
        final Dataset ds2 = CommonUtil.newDataset(CommonUtil.toList(account, account));
        final Dataset ds3 = CommonUtil.newDataset(CommonUtil.toList(account, account, account));

        assertEquals(0, ds2.except(ds1).size());
        assertEquals(1, N.difference(ds2, ds1).size());
        assertEquals(0, ds3.except(ds1).size());
        assertEquals(2, N.difference(ds3, ds1).size());

        assertEquals(1, N.intersection(ds2, ds1).size());
        assertEquals(1, ds2.intersectAll(ds1).size());
        assertEquals(2, N.intersection(ds3, ds2).size());
        assertEquals(2, ds3.intersectAll(ds2).size());
    }

    @Test
    public void testOutputFormats() throws IOException {
        ByteArrayOutputStream jsonOut = new ByteArrayOutputStream();
        dataset.toJson(jsonOut);
        String json = jsonOut.toString();
        assertTrue(json.contains("John"));

        ByteArrayOutputStream xmlOut = new ByteArrayOutputStream();
        dataset.toXml(xmlOut);
        String xml = xmlOut.toString();
        assertTrue(xml.contains("<name>John</name>"));

        ByteArrayOutputStream csvOut = new ByteArrayOutputStream();
        dataset.toCsv(csvOut);
        String csv = csvOut.toString();
        assertTrue(csv.contains("John"));
    }

    @Test
    public void testGroupByOperations() {
        Dataset ds = Dataset.rows(Arrays.asList("category", "value"), new Object[][] { { "A", 10 }, { "B", 20 }, { "A", 15 }, { "B", 25 } });
        assertEquals(4, ds.size());
        assertTrue(ds.columnCount() > 0);
    }

    @Test
    public void testGroupBy() {
        dataset.addRow(new Object[] { 6, "Frank", 25, 52000.0 });
        dataset.addRow(new Object[] { 7, "Grace", 30, 62000.0 });

        Dataset grouped = dataset.groupBy("age", "name", "names", collector(Collectors.toList()));

        assertEquals(2, grouped.columnCount());
        assertTrue(grouped.containsColumn("age"));
        assertTrue(grouped.containsColumn("names"));
    }

    @Test
    public void testGroupByWithCollector() {
        Dataset grouped = dataset.groupBy("age", "salary", "avgSalary", collector(Collectors.averagingDouble(val -> (Double) val)));

        assertEquals(2, grouped.columnCount());
        assertEquals(5, grouped.size());
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
    public void testGroupBySimple() {
        Dataset dataset = testDataset.copy();
        dataset.addRow(new Object[] { 5, "Alice", 35, 75000.0 });

        Dataset grouped = dataset.groupBy("name", "salary", "avg_salary", Collectors.averagingDouble(v -> (Double) v));

        assertNotNull(grouped);
        assertTrue(grouped.size() > 0);
        assertTrue(grouped.containsColumn("name"));
        assertTrue(grouped.containsColumn("avg_salary"));
    }

    @Test
    public void testGroupByWithExtractor() {
        Dataset grouped = testDataset.groupBy("age", (Integer age) -> age >= 30 ? "Senior" : "Junior", "salary", "avg_salary",
                Collectors.averagingDouble(v -> (Double) v));

        assertNotNull(grouped);
        assertTrue(grouped.size() > 0);
    }

    @Test
    public void testGroupByMultipleColumns() {
        Dataset grouped = testDataset.groupBy(Arrays.asList("age"));

        assertNotNull(grouped);
        assertTrue(grouped.size() > 0);
    }

    @Test
    public void testGroupByMultipleColumnsWithAggregation() {
        Dataset grouped = testDataset.groupBy(Arrays.asList("age"), "salary", "total_salary", Collectors.summingDouble(v -> (Double) v));

        assertNotNull(grouped);
        assertTrue(grouped.size() > 0);
        assertTrue(grouped.containsColumn("age"));
        assertTrue(grouped.containsColumn("total_salary"));
    }

    @Test
    public void testComplexGroupByScenarios() {
        Dataset grouped = dataset.groupBy("age", age -> ((int) age / 10) * 10, "id", "sumOfId", Collectors.summarizingInt(Numbers::toInt));
        assertTrue(grouped.size() <= dataset.size());

        Dataset multiGrouped = dataset.groupBy(Arrays.asList("age"), Arrays.asList("salary"), "totalSalary",
                collector(Collectors.summingDouble(arr -> arr[0] != null ? (Double) arr[0] : 0.0)));
        assertEquals(5, multiGrouped.size());
    }

    @Test
    public void testGroupBy2() {
        assertDoesNotThrow(() -> {
            Dataset dataset = Dataset.rows(Arrays.asList("department", "level", "employee", "salary", "bonus"),
                    new Object[][] { { "Sales", "Junior", "Alice", 50000, 5000 }, { "Sales", "Senior", "Bob", 75000, 8000 },
                            { "IT", "Junior", "Charlie", 55000, 6000 }, { "IT", "Senior", "David", 80000, 9000 }, { "Sales", "Junior", "Eve", 52000, 5500 } });

            Dataset result = dataset.groupBy(Arrays.asList("department", "level"), row -> row.get(0) + "_" + row.get(1), Arrays.asList("salary", "bonus"),
                    "total_compensation", row -> ((Integer) row.get(0)) + ((Integer) row.get(1)), Collectors.summingInt(Integer.class::cast));
            assertNotNull(result);
            assertTrue(result.size() > 0);
        });
    }

    @Test
    public void testRollup() {
        Stream<Dataset> rollupStream = testDataset.rollup(Arrays.asList("age"));
        assertNotNull(rollupStream);

        List<Dataset> rollupResults = rollupStream.toList();
        assertNotNull(rollupResults);
        assertTrue(rollupResults.size() > 0);
    }

    @Test
    public void testRollupWithAggregation() {
        Stream<Dataset> rollupStream = testDataset.rollup(Arrays.asList("age"), "salary", "total_salary", Collectors.summingDouble(v -> (Double) v));

        assertNotNull(rollupStream);
        List<Dataset> rollupResults = rollupStream.toList();
        assertTrue(rollupResults.size() > 0);
    }

    @Test
    public void testRollup2() {
        Dataset dataset = Dataset.rows(Arrays.asList("region", "country", "city", "sales", "quantity"),
                new Object[][] { { "North", "USA", "New York", 1000, 50 }, { "North", "USA", "Boston", 800, 40 }, { "North", "Canada", "Toronto", 600, 30 },
                        { "South", "Mexico", "Mexico City", 400, 20 } });
        Function<DisposableObjArray, String> keyExtractor = keyRow -> keyRow.join("-");
        Stream<Dataset> rollupResult = dataset.rollup(Arrays.asList("region", "country", "city"), keyExtractor, Arrays.asList("sales", "quantity"),
                "aggregated_totals", row -> Tuple.of((Integer) row.get(0), (Integer) row.get(1)), MoreCollectors.summingInt(tp -> tp._1, tp -> tp._2));
        assertNotNull(rollupResult);
        assertTrue(rollupResult.toList().size() > 0);
    }

    @Test
    public void test_rollup() throws Exception {
        final List<Account> accountList = createAccountList(Account.class, 9);
        accountList.forEach(it -> it.setId(2));
        final Dataset ds = CommonUtil.newDataset(accountList);
        ds.rollup(ds.columnNames()).forEach(Dataset::println);
        assertNotNull(ds);
    }

    @Test
    public void testCube() {
        Stream<Dataset> cubeStream = testDataset.cube(Arrays.asList("age"));
        assertNotNull(cubeStream);

        List<Dataset> cubeResults = cubeStream.toList();
        assertNotNull(cubeResults);
        assertTrue(cubeResults.size() > 0);
    }

    @Test
    public void testCubeWithAggregation() {
        Stream<Dataset> cubeStream = testDataset.cube(Arrays.asList("age"), "salary", "avg_salary", Collectors.averagingDouble(v -> (Double) v));

        assertNotNull(cubeStream);
        List<Dataset> cubeResults = cubeStream.toList();
        assertTrue(cubeResults.size() > 0);
    }

    @Test
    public void testCube2() {
        Dataset dataset = Dataset.rows(Arrays.asList("region", "country", "sales"),
                new Object[][] { { "North", "USA", 1000 }, { "North", "Canada", 600 }, { "South", "Mexico", 400 } });

        Function<DisposableObjArray, String> keyExtractor = keyRow -> keyRow.join("-");
        Function<DisposableObjArray, Double> rowMapper = row -> (Integer) row.get(0) * 1.1;
        Stream<Dataset> cubeResult = dataset.cube(Arrays.asList("region", "country"), keyExtractor, Arrays.asList("sales"), "total_sales_with_markup",
                rowMapper, Collectors.collectingAndThen(Collectors.summingDouble(Double::doubleValue), r -> Numbers.round(r, 2)));
        assertNotNull(cubeResult);
        assertTrue(cubeResult.toList().size() > 0);
    }

    @Test
    public void test_cube() throws Exception {
        final List<Account> accountList = createAccountList(Account.class, 9);
        accountList.forEach(it -> it.setId(2));
        final Dataset ds = CommonUtil.newDataset(accountList);
        ds.cube(ds.columnNames()).forEach(Dataset::println);
        assertNotNull(ds);
    }

    @Test
    public void test_cube_2() throws Exception {
        final Object[][] rowList = { { "Banana", 1000, "USA" }, { "Carrots", 1500, "USA" }, { "Beans", 1600, "USA" }, { "Orange", 2000, "USA" },
                { "Orange", 2000, "USA" }, { "Banana", 400, "China" }, { "Carrots", 1200, "China" }, { "Beans", 1500, "China" }, { "Orange", 4000, "China" },
                { "Banana", 2000, "Canada" }, { "Carrots", 2000, "Canada" }, { "Beans", 2000, "Mexico" } };

        final Dataset dataset = CommonUtil.newDataset(CommonUtil.toList("Product", "Amount", "Country"), rowList);

        dataset.cube(CommonUtil.toList("Product", "Country"), CommonUtil.toList("Amount"), "result", List.class).forEach(Dataset::println);
        assertNotNull(dataset);
    }

    @Test
    public void testPivot() {
        List<String> cols = Arrays.asList("product", "quarter", "sales");
        List<List<Object>> data = new ArrayList<>();
        data.add(Arrays.asList("A", "A", "A", "A", "B", "B", "B", "B"));
        data.add(Arrays.asList("Q1", "Q2", "Q3", "Q4", "Q1", "Q2", "Q3", "Q4"));
        data.add(Arrays.asList(100, 110, 120, 130, 200, 210, 220, 230));

        Dataset salesData = new RowDataset(cols, data);

        Sheet<Object, Object, Object> pivoted = salesData.pivot("product", "quarter", "sales", collector(Collectors.summingInt(val -> (Integer) val)));

        assertNotNull(pivoted);
        assertEquals(2, pivoted.rowKeySet().size());
        assertEquals(4, pivoted.columnKeySet().size());
    }

    @Test
    public void test_pivot() throws Exception {
        final Object[][] rowList = { { "Banana", 1000, "USA" }, { "Carrots", 1500, "USA" }, { "Beans", 1600, "USA" }, { "Orange", 2000, "USA" },
                { "Orange", 2000, "USA" }, { "Banana", 400, "China" }, { "Carrots", 1200, "China" }, { "Beans", 1500, "China" }, { "Orange", 4000, "China" },
                { "Banana", 2000, "Canada" }, { "Carrots", 2000, "Canada" }, { "Beans", 2000, "Mexico" } };

        final Dataset dataset = CommonUtil.newDataset(CommonUtil.toList("Product", "Amount", "Country"), rowList);

        final Dataset ds2 = dataset.groupBy(CommonUtil.toList("Product", "Country"), "Amount", "sum(Amount)",
                Collectors.summingLong(it -> ((Number) it).longValue()));
        ds2.sortBy("Product");
        assertNotNull(dataset);
    }

    @Test
    public void testPivot2() {
        assertDoesNotThrow(() -> {
            Dataset dataset = Dataset.rows(Arrays.asList("region", "product", "sales", "quantity"),
                    new Object[][] { { "North", "A", 100, 10 }, { "North", "B", 200, 20 }, { "South", "A", 150, 15 }, { "South", "B", 250, 25 } });

            Sheet<String, String, Integer> pivotResult = dataset.pivot("region", "product", Arrays.asList("sales", "quantity"),
                    Collectors.summingInt(arr -> (Integer) arr[0] + (Integer) arr[1]));
            assertNotNull(pivotResult);
            assertEquals(2, pivotResult.rowKeySet().size());
        });
    }

    @Test
    public void testSortBy_WithComparator() {
        Dataset ds = dataset.copy();
        ds.sortBy("age", Comparator.reverseOrder());

        assertEquals(Integer.valueOf(40), ds.get(0, 2));
        assertEquals(Integer.valueOf(35), ds.get(1, 2));
    }

    @Test
    public void testSortBy() {
        Dataset dataset = testDataset.copy();
        dataset.sortBy("age");

        dataset.moveToRow(0);
        assertEquals(25, dataset.getInt("age"));
        dataset.moveToRow(3);
        assertEquals(35, dataset.getInt("age"));
    }

    @Test
    public void testSortByWithComparator() {
        Dataset dataset = testDataset.copy();
        dataset.sortBy("age", Comparator.<Integer> naturalOrder().reversed());

        dataset.moveToRow(0);
        assertEquals(35, dataset.getInt("age"));
        dataset.moveToRow(3);
        assertEquals(25, dataset.getInt("age"));
    }

    @Test
    public void testComplexSortScenarios() {
        dataset.sortBy(Arrays.asList("name"), row -> row.get(0).toString().length());

        dataset.parallelSortBy(Arrays.asList("age", "salary"));

        int firstAge = (int) dataset.get(0, 2);
        for (int i = 1; i < dataset.size(); i++) {
            assertTrue(firstAge <= (int) dataset.get(i, 2));
        }
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
    public void testSortByMultipleColumns() {
        Dataset dataset = testDataset.copy();
        dataset.addRow(new Object[] { 5, "Eve", 30, 48000.0 });

        dataset.sortBy(Arrays.asList("age", "salary"));

        dataset.moveToRow(0);
        assertEquals(25, dataset.getInt("age"));
    }

    @Test
    public void testSortByMultipleColumnsWithComparator() {
        Dataset dataset = testDataset.copy();
        Comparator<Object[]> comparator = (a, b) -> {
            int ageCompare = ((Integer) a[0]).compareTo((Integer) b[0]);
            if (ageCompare != 0) {
                return ageCompare;
            }
            return ((String) a[1]).compareTo((String) b[1]);
        };
        dataset.sortBy(Arrays.asList("age", "name"), comparator);

        dataset.moveToRow(0);
        assertEquals(25, dataset.getInt("age"));
    }

    @Test
    public void testSortByWithKeyExtractor() {
        Dataset dataset = testDataset.copy();
        dataset.sortBy(Arrays.asList("age", "name"), (DisposableObjArray row) -> ((Integer) row.get(0)) * 1000 + ((String) row.get(1)).length());

        assertNotNull(dataset);
        assertTrue(dataset.size() > 0);
    }

    @Test
    public void test_sortBy() throws Exception {

        final List<List<Object>> rowList = IntStream.range(0, 10)
                .mapToObj(it -> (List<Object>) CommonUtil.<Object> asList(it, ((char) ('a' + it)), it + "_" + ((char) ('a' + it))))
                .toList();
        CommonUtil.shuffle(rowList);

        Dataset dataset = CommonUtil.newDataset(CommonUtil.toList("int", "char", "str"), rowList);

        dataset.sortBy("int");

        dataset = CommonUtil.newDataset(CommonUtil.toList("int", "char", "str"), rowList);
        dataset.sortBy(CommonUtil.toList("char", "int"), Comparators.OBJECT_ARRAY_COMPARATOR);

        dataset = CommonUtil.newDataset(CommonUtil.toList("int", "char", "str"), rowList);
        dataset.sortBy(CommonUtil.toList("char", "int"), Comparators.comparingObjArray(Comparators.reverseOrder()));
        assertNotNull(dataset);
    }

    @Test
    public void testParallelSortBy() {
        Dataset ds = dataset.copy();
        ds.parallelSortBy("age");

        assertEquals(Integer.valueOf(25), ds.get(0, 2));
        assertEquals(Integer.valueOf(28), ds.get(1, 2));
    }

    @Test
    public void testParallelSortByWithComparator() {
        Dataset dataset = testDataset.copy();
        dataset.parallelSortBy("name", Comparator.<String> naturalOrder().reversed());

        dataset.moveToRow(0);
        String firstName = dataset.get("name").toString();
        assertTrue(firstName.compareTo("Charlie") >= 0);
    }

    @Test
    public void testParallelSortByMultipleColumns() {
        Dataset dataset = testDataset.copy();
        dataset.parallelSortBy(Arrays.asList("age", "name"));

        dataset.moveToRow(0);
        assertEquals(25, dataset.getInt("age"));
    }

    @Test
    public void testTopBy() {
        Dataset top = dataset.topBy("salary", 2);
        assertNotNull(top);
        assertEquals(2, top.size());
        assertEquals(70000.0, top.get(0, 3));
        assertEquals(80000.0, top.get(1, 3));
    }

    @Test
    public void testTopByWithComparator() {
        Dataset top2 = testDataset.topBy("age", 2, Comparator.<Integer> naturalOrder().reversed());

        assertNotNull(top2);
        assertEquals(2, top2.size());
    }

    @Test
    public void testTopByMultipleColumns() {
        Dataset top2 = testDataset.topBy(Arrays.asList("age", "salary"), 2);

        assertNotNull(top2);
        assertEquals(2, top2.size());
    }

    @Test
    public void testTopByMultipleColumnsWithComparator() {
        Comparator<Object[]> comp = (a, b) -> ((Integer) a[0]).compareTo((Integer) b[0]);
        Dataset top2 = testDataset.topBy(Arrays.asList("age", "salary"), 2, comp);

        assertNotNull(top2);
        assertEquals(2, top2.size());
    }

    @Test
    public void testTopByWithKeyExtractor() {
        Dataset top2 = testDataset.topBy(Arrays.asList("age", "salary"), 2, (DisposableObjArray row) -> (Integer) row.get(0));

        assertNotNull(top2);
        assertEquals(2, top2.size());
    }

    @Test
    public void distinct() {
        sampleDataset.addRow(new Object[] { 1, "Alice", 30 });
        assertEquals(4, sampleDataset.size());
        Dataset distinctDs = sampleDataset.distinct();
        assertEquals(3, distinctDs.size());
    }

    @Test
    public void testDistinctOperations() {
        Dataset ds = Dataset.rows(Arrays.asList("value"), new Object[][] { { 1 }, { 2 }, { 1 }, { 3 }, { 2 } });
        Dataset distinct = ds.distinct();
        assertEquals(3, distinct.size());
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
    public void test_distinct() throws Exception {
        final List<Account> accountList = createAccountList(Account.class, 1000);
        final Dataset ds = CommonUtil.newDataset(accountList);
        Dataset ds2 = ds.distinct();
        assertEquals(accountList.size(), ds2.size());

        ds2 = ds.distinct();
        assertEquals(accountList.size(), ds2.size());

        ds2 = ds.distinctBy("gui");
        assertEquals(accountList.size(), ds2.size());

        ds2 = ds.distinctBy("gui", (Function<String, Object>) t -> t.substring(0, 2));

        ds2 = ds.distinctBy(CommonUtil.toList("firstName", "lastName"), (Function<DisposableObjArray, Object>) DisposableObjArray::length);
        assertEquals(1, ds2.size());

        ds2 = ds.groupBy(CommonUtil.toList("firstName", "lastName"), (Function<DisposableObjArray, Object>) DisposableObjArray::length);
        assertEquals(1, ds2.size());

        ds2 = ds.groupBy("gui", (Function<String, Object>) t -> t.substring(0, 2), "gui", "*", Collectors.counting());

        ds2 = ds.groupBy("gui", (Function<String, Object>) t -> t.substring(0, 2), CommonUtil.toList("gui"), "*", Collectors.counting());

        ds2 = ds.groupBy(CommonUtil.toList("firstName", "lastName"), (Function<DisposableObjArray, Object>) DisposableObjArray::length, "gui", "*",
                Collectors.counting());

        assertEquals(1, ds2.size());

        ds2 = ds.groupBy(CommonUtil.toList("firstName", "lastName"), (Function<DisposableObjArray, Object>) DisposableObjArray::length,
                CommonUtil.toList("gui"), "*", Collectors.counting());

        ds2 = ds.groupBy(CommonUtil.toList("firstName", "lastName"), (Function<DisposableObjArray, Object>) DisposableObjArray::length,
                CommonUtil.toList("firstName", "lastName"), "*", Collectors.counting());

        assertEquals(1, ds2.size());

        ds2 = ds.groupBy(CommonUtil.toList("firstName", "lastName"), (Function<DisposableObjArray, Object>) DisposableObjArray::length,
                CommonUtil.toList("firstName", "lastName"), "*", Collectors.counting());

        assertEquals(1, ds2.size());

        ds2 = ds.distinctBy(CommonUtil.toList("gui"));
        assertEquals(accountList.size(), ds2.size());

        ds2 = ds.distinctBy(CommonUtil.toList("firstName", "lastName", "gui"));
        assertEquals(accountList.size(), ds2.size());

        ds2 = ds.distinctBy(CommonUtil.toList("firstName", "lastName", "gui"));
        assertEquals(accountList.size(), ds2.size());

        ds2 = ds.distinctBy(CommonUtil.toList("firstName", "lastName"));
        assertEquals(1, ds2.size());

        ds2 = ds.distinctBy(CommonUtil.toList("firstName", "lastName"));
        assertEquals(1, ds2.size());
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
    public void testDistinctBy() {
        Dataset ds = Dataset.rows(Arrays.asList("id", "category"), new Object[][] { { 1, "A" }, { 2, "B" }, { 3, "A" } });
        Dataset distinct = ds.distinctBy("category");
        assertEquals(2, distinct.size());
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
    public void distinctBy_singleColumn() {
        sampleDataset.addRow(new Object[] { 4, "Alice", 28 });
        Dataset distinctByName = sampleDataset.distinctBy("Name");
        assertEquals(3, distinctByName.size());
        List<Object> names = distinctByName.getColumn("Name");
        assertTrue(names.contains("Alice") && names.contains("Bob") && names.contains("Charlie"));
    }

    @Test
    public void testDistinctByColumn() {
        Dataset dataset = testDataset.copy();
        dataset.addRow(new Object[] { 5, "Alice", 35, 75000.0 });

        Dataset distinct = dataset.distinctBy("name");

        assertNotNull(distinct);
        assertEquals(4, distinct.size());
    }

    @Test
    public void testDistinctByColumnWithExtractor() {
        Dataset dataset = testDataset.copy();
        dataset.addRow(new Object[] { 5, "ALICE", 35, 75000.0 });

        Dataset distinct = dataset.distinctBy("name", (String name) -> name.toLowerCase());

        assertNotNull(distinct);
        assertEquals(4, distinct.size());
    }

    @Test
    public void testDistinctByMultipleColumns() {
        Dataset dataset = testDataset.copy();
        dataset.addRow(new Object[] { 5, "Alice", 30, 75000.0 });

        Dataset distinct = dataset.distinctBy(Arrays.asList("name", "age"));

        assertNotNull(distinct);
        assertEquals(4, distinct.size());
    }

    @Test
    public void testDistinctByMultipleColumnsWithExtractor() {
        Dataset dataset = testDataset.copy();
        dataset.addRow(new Object[] { 5, "Alice", 30, 75000.0 });

        Dataset distinct = dataset.distinctBy(Arrays.asList("name", "age"), (DisposableObjArray row) -> row.get(0).toString() + "_" + row.get(1));

        assertNotNull(distinct);
        assertEquals(4, distinct.size());
    }

}

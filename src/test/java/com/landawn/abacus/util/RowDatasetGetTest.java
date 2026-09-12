package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.util.function.IntFunction;

public class RowDatasetGetTest extends RowDatasetTestSupport {
    @Test
    public void testGetByRowAndColumnIndex() {
        assertEquals(1, (Integer) dataset.get(0, 0));
        assertEquals("Alice", dataset.get(0, 1));
        assertEquals(25, (Integer) dataset.get(0, 2));
        assertEquals(50000.0, dataset.get(0, 3));
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
    public void testGetProperty() {
        Map<String, Object> props = new HashMap<>();
        props.put("key1", "value1");
        RowDataset ds = new RowDataset(columnNames, copyColumnList(), props);
        assertEquals("value1", ds.getProperties().get("key1"));
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
    public void testGetOutOfBounds() {
        assertThrows(IndexOutOfBoundsException.class, () -> dataset.get(10, 0));
        assertThrows(IndexOutOfBoundsException.class, () -> dataset.get(0, 10));
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
    public void testGetColumnIndexWithInvalidName() {
        final RowDataset dataset = createThreeRowScoreDataset();
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            dataset.getColumnIndex("invalid");
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
    public void testGetColumnIndexes_MultipleColumns() {
        int[] indexes = dataset.getColumnIndexes(Arrays.asList("id", "name", "age"));
        assertEquals(3, indexes.length);
        assertEquals(0, indexes[0]);
        assertEquals(1, indexes[1]);
        assertEquals(2, indexes[2]);
    }

    // ===== getColumnIndexes with all column names =====

    @Test
    public void testGetColumnIndexes_AllColumns_CacheHit() {
        // Call twice to test the cache path (_columnIndexes != null)
        int[] indexes1 = dataset.getColumnIndexes(dataset.columnNames());
        int[] indexes2 = dataset.getColumnIndexes(dataset.columnNames());

        assertEquals(4, indexes1.length);
        assertEquals(4, indexes2.length);
        for (int i = 0; i < 4; i++) {
            assertEquals(i, indexes1[i]);
            assertEquals(i, indexes2[i]);
        }
    }

    @Test
    public void testGetColumnIndexesWithInvalidName() {
        final RowDataset dataset = createThreeRowScoreDataset();
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            dataset.getColumnIndexes(Arrays.asList("id", "invalid"));
        });
    }

    @Test
    public void testGetColumnIndexes_InvalidColumn() {
        assertThrows(IllegalArgumentException.class, () -> dataset.getColumnIndexes(Arrays.asList("nonexistent")));
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
    public void testGetColumnViewDetachesWhenItsColumnIsRemoved() {
        final Dataset ds = twoColumnDataset();
        final ImmutableList<Object> name = ds.getColumn("name");

        assertEquals(Arrays.asList("a", "b", "c"), name);

        ds.set(0, 1, "A");
        assertEquals("A", name.get(0));

        ds.removeColumn("name");
        assertFalse(ds.containsColumn("name"));
        // Documented behaviour: the view is bound to the column list it resolved, so it stays readable.
        assertEquals(Arrays.asList("A", "b", "c"), name);
    }

    @Test
    public void testGetRowAsImmutableList() {
        final RowDataset dataset = createThreeRowScoreDataset();
        List<Object> row = dataset.getRow(0);
        Assertions.assertTrue(row instanceof ImmutableList);
        Assertions.assertTrue(((ImmutableList<?>) row).list instanceof java.util.AbstractList);
        Assertions.assertEquals(4, row.size());
        Assertions.assertEquals(1, row.get(0));
        Assertions.assertEquals("John", row.get(1));
        Assertions.assertEquals(25, row.get(2));
        Assertions.assertEquals(85.5, row.get(3));
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
    public void testGetRow() {
        ImmutableList<Object> row = dataset.getRow(0);
        assertNotNull(row);
        assertEquals(4, row.size());
        assertEquals(1, row.get(0));
        assertEquals("Alice", row.get(1));
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
    public void testGetRow_RowSupplierReturningNull() {
        assertThrows(IllegalArgumentException.class, () -> dataset.getRow(0, size -> null));
    }

    @Test
    public void testGetRowViewIsPositional() {
        final Dataset ds = twoColumnDataset();
        final ImmutableList<Object> firstRow = ds.getRow(0);

        assertEquals(Arrays.asList(1, "a"), firstRow);

        ds.sortBy("id", Comparator.reverseOrder());
        // Documented behaviour: an indexed view follows the position.
        assertEquals(Arrays.asList(3, "c"), firstRow);
    }

    @Test
    public void testGetProperties() {
        Map<String, Object> props = dataset.getProperties();
        assertNotNull(props);
    }

}

package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.util.NoCachingNoUpdating.DisposableObjArray;
import com.landawn.abacus.util.function.Function;

public class RowDatasetRemoveTest extends RowDatasetTestSupport {
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
    public void testRemoveColumnsWithEmptyList() {
        final RowDataset dataset = createThreeRowScoreDataset();
        dataset.removeColumns(Collections.emptyList());
        Assertions.assertEquals(4, dataset.columnCount());
    }

    @Test
    public void testRemoveColumns_DuplicateNamesInInput() {
        final List<String> cols = new ArrayList<>(Arrays.asList("a", "b", "c", "d"));
        final List<List<Object>> data = new ArrayList<>();
        data.add(new ArrayList<>(Arrays.asList(1, 2)));
        data.add(new ArrayList<>(Arrays.asList(3, 4)));
        data.add(new ArrayList<>(Arrays.asList(5, 6)));
        data.add(new ArrayList<>(Arrays.asList(7, 8)));
        final RowDataset ds = new RowDataset(cols, data);

        // duplicate "c" in input should not corrupt the dataset by removing wrong columns
        ds.removeColumns(Arrays.asList("c", "c"));

        // expect remaining columns to be [a, b, d] in order
        assertEquals(3, ds.columnCount());
        assertTrue(ds.containsColumn("a"));
        assertTrue(ds.containsColumn("b"));
        assertTrue(ds.containsColumn("d"));
        assertFalse(ds.containsColumn("c"));
        // verify data alignment is preserved
        assertEquals((Integer) 1, ds.get(0, ds.getColumnIndex("a")));
        assertEquals((Integer) 3, ds.get(0, ds.getColumnIndex("b")));
        assertEquals((Integer) 7, ds.get(0, ds.getColumnIndex("d")));
    }

    @Test
    public void testRemoveRow() {
        RowDataset ds = new RowDataset(columnNames, copyColumnList());
        ds.removeRow(0);
        assertEquals(4, ds.size());
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
    public void testRemoveRowOutOfBounds() {
        RowDataset ds = new RowDataset(columnNames, copyColumnList());
        assertThrows(IndexOutOfBoundsException.class, () -> {
            ds.removeRow(10);
        });
    }

    @Test
    public void testRemoveRowsAt() {
        int sizeBefore = dataset.size();
        dataset.removeRowsAt(new int[] { 0, 2 });
        assertEquals(sizeBefore - 2, dataset.size());
    }

    @Test
    public void testRemoveRows() {
        RowDataset ds = new RowDataset(columnNames, copyColumnList());
        ds.removeRowsAt(0, 2);
        assertEquals(3, ds.size());
        assertEquals(2, (Integer) ds.get(0, 0));
    }

    @Test
    public void testRemoveRowsRange() {
        int sizeBefore = dataset.size();
        dataset.removeRows(1, 3);
        assertEquals(sizeBefore - 2, dataset.size());
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
    public void testRemoveDuplicateRowsByColumnName() {
        RowDataset ds = new RowDataset(new ArrayList<>(Arrays.asList("name", "val")),
                new ArrayList<>(Arrays.asList(new ArrayList<>(Arrays.asList("A", "B", "A", "C")), new ArrayList<>(Arrays.asList(1, 2, 3, 4)))));

        ds.removeDuplicateRowsBy("name");
        assertEquals(3, ds.size()); // "A" duplicate removed
    }

    @Test
    public void testRemoveDuplicateRowsBy_MultiColumnWithKeyExtractor() {
        RowDataset dupDataset = new RowDataset(new ArrayList<>(Arrays.asList("id", "name", "dept")),
                new ArrayList<>(Arrays.asList(new ArrayList<>(Arrays.asList(1, 1, 2, 2)), new ArrayList<>(Arrays.asList("Alice", "Alice", "Bob", "Bob")),
                        new ArrayList<>(Arrays.asList("eng", "eng", "hr", "mkt")))));

        // use keyExtractor based on id+dept
        dupDataset.removeDuplicateRowsBy(Arrays.asList("id", "dept"),
                (Function<? super NoCachingNoUpdating.DisposableObjArray, ?>) arr -> arr.get(0).toString() + "-" + arr.get(1).toString());

        assertEquals(3, dupDataset.size());
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
    public void testRemoveDuplicateRowsByMultipleColumnsWithKeyExtractor() {
        List<List<Object>> cols = new ArrayList<>();
        cols.add(new ArrayList<>(Arrays.asList(1, 1, 2, 2)));
        cols.add(new ArrayList<>(Arrays.asList("A", "A", "B", "C")));
        RowDataset ds = new RowDataset(new ArrayList<>(Arrays.asList("id", "name")), cols);

        ds.removeDuplicateRowsBy(Arrays.asList("id", "name"), (DisposableObjArray arr) -> arr.get(0) + "_" + arr.get(1));
        assertEquals(3, ds.size());
    }

    @Test
    public void testRemoveDuplicateRowsBy_NullOrEmptyKeyColumnNames_ThrowsIllegalArgument() {
        RowDataset ds = new RowDataset(columnNames, copyColumnList());

        assertThrows(IllegalArgumentException.class, () -> ds.removeDuplicateRowsBy((Collection<String>) null));
        assertThrows(IllegalArgumentException.class, () -> ds.removeDuplicateRowsBy(new ArrayList<>()));
        assertThrows(IllegalArgumentException.class, () -> ds.removeDuplicateRowsBy((Collection<String>) null, (DisposableObjArray arr) -> arr.get(0)));
        assertThrows(IllegalArgumentException.class, () -> ds.removeDuplicateRowsBy(new ArrayList<>(), (DisposableObjArray arr) -> arr.get(0)));
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
    public void testRemoveDuplicateRowsBy_MultiColumnNullExtractor() {
        RowDataset dupDataset = new RowDataset(new ArrayList<>(Arrays.asList("id", "name")),
                new ArrayList<>(Arrays.asList(new ArrayList<>(Arrays.asList(1, 1, 2)), new ArrayList<>(Arrays.asList("A", "A", "B")))));

        dupDataset.removeDuplicateRowsBy(Arrays.asList("id", "name"));
        assertEquals(2, dupDataset.size());
    }

    // ========== removeDuplicateRowsBy - size <= 1 ==========

    @Test
    public void testRemoveDuplicateRowsBy_WithExtractor_SingleRow_ReturnsEarly() {
        RowDataset ds = new RowDataset(new ArrayList<>(Arrays.asList("id")), new ArrayList<>(Arrays.asList(new ArrayList<>(Arrays.asList(1)))));
        int sizeBefore = ds.size();
        ds.removeDuplicateRowsBy("id", (Function<Object, Object>) v -> v);
        assertEquals(sizeBefore, ds.size());
    }

    @Test
    public void testRemoveDuplicateRowsBy_MultiColumn_SingleRow_ReturnsEarly() {
        RowDataset ds = new RowDataset(new ArrayList<>(Arrays.asList("id", "name")),
                new ArrayList<>(Arrays.asList(new ArrayList<>(Arrays.asList(1)), new ArrayList<>(Arrays.asList("A")))));
        int sizeBefore = ds.size();
        ds.removeDuplicateRowsBy(Arrays.asList("id", "name"));
        assertEquals(sizeBefore, ds.size());
    }

    @Test
    public void testRemoveDuplicateRowsClampsCurrentRowIndex() {
        final RowDataset ds = new RowDataset(new ArrayList<>(Arrays.asList("id")), new ArrayList<>(Arrays.asList(new ArrayList<>(Arrays.asList(1, 1, 2)))));
        ds.moveToRow(2);

        ds.removeDuplicateRowsBy("id");

        assertEquals(2, ds.size());
        assertEquals(1, ds.currentRowIndex());
        assertEquals(2, ds.<Integer> get("id"));

        final RowDataset multiKey = new RowDataset(new ArrayList<>(Arrays.asList("id", "name")),
                new ArrayList<>(Arrays.asList(new ArrayList<>(Arrays.asList(1, 1, 2)), new ArrayList<>(Arrays.asList("a", "a", "b")))));
        multiKey.moveToRow(2);
        multiKey.removeDuplicateRowsBy(Arrays.asList("id", "name"));
        assertEquals(1, multiKey.currentRowIndex());
        assertEquals("b", multiKey.<String> get("name"));
    }

}

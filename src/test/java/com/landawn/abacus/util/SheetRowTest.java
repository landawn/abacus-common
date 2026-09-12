package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.util.stream.Stream;

public class SheetRowTest extends SheetTestSupport {
    @Test
    public void testRowOrientedStreamsPreserveRowsWhenThereAreNoColumns() {
        final Sheet<String, String, Integer> zeroColumnSheet = new Sheet<>(Arrays.asList("r1", "r2"), Collections.emptyList());

        assertEquals(2, zeroColumnSheet.rowCells().count());
        assertEquals(2, zeroColumnSheet.rowStreams().count());

        final List<Pair<String, Stream<Integer>>> rows = zeroColumnSheet.rows().toList();
        assertEquals(Arrays.asList("r1", "r2"), rows.stream().map(Pair::left).toList());
        assertTrue(rows.get(0).right().toList().isEmpty());
        assertTrue(rows.get(1).right().toList().isEmpty());

        final List<Integer> mappedRowSizes = zeroColumnSheet.rows((rowIndex, row) -> row.length()).map(Pair::right).toList();
        assertEquals(Arrays.asList(0, 0), mappedRowSizes);
    }

    @Test
    public void testRowKeySet() {
        ImmutableSet<String> keys = sheet.rowKeySet();
        assertNotNull(keys);
        assertEquals(3, keys.size());
        assertTrue(keys.contains("row1"));
        assertTrue(keys.contains("row2"));
        assertTrue(keys.contains("row3"));
    }

    @Test
    public void testRowKeySetImmutable() {
        ImmutableSet<String> keys = sheet.rowKeySet();
        assertThrows(UnsupportedOperationException.class, () -> {
            keys.add("row4");
        });
    }

    @Test
    public void testRowValues_AllRows() {
        ImmutableList<Integer> row2 = sheet.rowValues("row2");
        assertEquals(Arrays.asList(4, 5, 6), new ArrayList<>(row2));

        ImmutableList<Integer> row3 = sheet.rowValues("row3");
        assertEquals(Arrays.asList(7, 8, 9), new ArrayList<>(row3));
    }

    // ==================== Additional tests matching source method names ====================

    @Test
    public void testRowValues() {
        List<Integer> row = sheet.rowValues("row1");
        assertNotNull(row);
        assertTrue(row instanceof ImmutableList);
        assertTrue(((ImmutableList<?>) row).list instanceof java.util.AbstractList);
        assertFalse(((ImmutableList<?>) row).list instanceof ArrayList);
        assertEquals(3, row.size());
        assertEquals(Integer.valueOf(1), row.get(0));
        assertEquals(Integer.valueOf(2), row.get(1));
        assertEquals(Integer.valueOf(3), row.get(2));

        sheet.set("row1", "col3", 30);
        assertEquals(Integer.valueOf(30), row.get(2));
    }

    @Test
    public void testRowValues_UninitializedSheet() {
        Sheet<String, String, Integer> uninitSheet = new Sheet<>(rowKeys, columnKeys);
        List<Integer> row = uninitSheet.rowValues("row1");
        assertTrue(row instanceof ImmutableList);
        assertTrue(((ImmutableList<?>) row).list instanceof java.util.AbstractList);
        assertFalse(((ImmutableList<?>) row).list instanceof ArrayList);
        assertEquals(3, row.size());
        assertNull(row.get(0));
        assertNull(row.get(1));
        assertNull(row.get(2));

        uninitSheet.set("row1", "col2", 20);
        // the lazy view now reflects writes that initialize the sheet (it previously captured
        // rowIndex = -1 and crashed with IndexOutOfBoundsException)
        assertEquals(Integer.valueOf(20), row.get(1));
    }

    @Test
    public void testRowValues_InvalidKey() {
        assertThrows(IllegalArgumentException.class, () -> sheet.rowValues("invalidRow"));
    }

    @Test
    public void testRowValuesViewFollowsRowKeyThroughReordering() {
        // rowValues is a KEYED live view: it resolves the row key on every access, so it keeps showing the
        // same logical row after moveRow/swapRows/sortByRowKey. It used to capture the numeric position at
        // creation time and silently start reporting a different row's values.
        final Sheet<String, String, Integer> s = Sheet.rows(Arrays.asList("R1", "R2", "R3"), Arrays.asList("C1"), new Integer[][] { { 1 }, { 2 }, { 3 } });
        final ImmutableList<Integer> r2 = s.rowValues("R2");
        assertEquals(2, r2.get(0).intValue());

        s.moveRow("R2", 2);
        assertEquals(Arrays.asList("R1", "R3", "R2"), new ArrayList<>(s.rowKeySet()));
        assertEquals(2, r2.get(0).intValue());

        s.swapRows("R2", "R1");
        assertEquals(2, r2.get(0).intValue());

        s.sortByRowKey();
        assertEquals(Arrays.asList("R1", "R2", "R3"), new ArrayList<>(s.rowKeySet()));
        assertEquals(2, r2.get(0).intValue());

        // Still live for value updates at whatever position the key now occupies.
        s.set("R2", "C1", 99);
        assertEquals(99, r2.get(0).intValue());
    }

    @Test
    public void testRowValuesViewTracksColumnCount() {
        // The view's length is the live column count, not a snapshot taken at creation.
        final Sheet<String, String, Integer> s = Sheet.rows(Arrays.asList("R1"), Arrays.asList("C1"), new Integer[][] { { 1 } });
        final ImmutableList<Integer> r1 = s.rowValues("R1");
        assertEquals(1, r1.size());

        s.addColumn("C2", Arrays.asList(9));
        assertEquals(2, r1.size());
        assertEquals(Arrays.asList(1, 9), new ArrayList<>(r1));

        s.removeColumn("C1");
        assertEquals(1, r1.size());
        assertEquals(Arrays.asList(9), new ArrayList<>(r1));
    }

    @Test
    public void testRowValuesViewThrowsAfterItsRowIsRemoved() {
        final Sheet<String, String, Integer> s = Sheet.rows(Arrays.asList("R1", "R2"), Arrays.asList("C1"), new Integer[][] { { 1 }, { 2 } });
        final ImmutableList<Integer> r1 = s.rowValues("R1");
        assertEquals(1, r1.get(0).intValue());

        // Removing an unrelated row must not disturb the view.
        s.removeRow("R2");
        assertEquals(1, r1.get(0).intValue());

        // Removing the view's own row makes it report the loss explicitly rather than
        // silently reading whatever row slid into that position.
        s.removeRow("R1");
        assertThrows(IllegalArgumentException.class, () -> r1.get(0));
    }

    @Test
    public void testRowValuesViewOnUninitializedSheet() {
        final Sheet<String, String, Integer> s = new Sheet<>(Arrays.asList("R1"), Arrays.asList("C1", "C2"));
        final ImmutableList<Integer> r1 = s.rowValues("R1");
        assertEquals(2, r1.size());
        assertNull(r1.get(0));
        assertNull(r1.get(1));

        // A later write initializes the sheet and must be visible through the already-created view.
        s.set("R1", "C2", 7);
        assertNull(r1.get(0));
        assertEquals(7, r1.get(1).intValue());
    }

    @Test
    public void testRowValuesViewIndexOutOfBounds() {
        final Sheet<String, String, Integer> s = Sheet.rows(Arrays.asList("R1"), Arrays.asList("C1"), new Integer[][] { { 1 } });
        final ImmutableList<Integer> r1 = s.rowValues("R1");

        assertThrows(IndexOutOfBoundsException.class, () -> r1.get(-1));
        assertThrows(IndexOutOfBoundsException.class, () -> r1.get(1));
    }

    @Test
    public void testRowCellsColumnCellsCount_drainsIterator() {
        // Regression: rowCells/columnCells iterators' count() must exhaust the iterator
        // (IteratorEx.count() contract), matching the rowStreams/rows/columns siblings fixed earlier.
        final Sheet<String, String, Integer> sheet = Sheet.rows(java.util.Arrays.asList("R1", "R2"), java.util.Arrays.asList("C1", "C2", "C3"),
                new Integer[][] { { 1, 2, 3 }, { 4, 5, 6 } });

        final com.landawn.abacus.util.stream.ObjIteratorEx<com.landawn.abacus.util.stream.Stream<Sheet.Cell<String, String, Integer>>> outer = (com.landawn.abacus.util.stream.ObjIteratorEx<com.landawn.abacus.util.stream.Stream<Sheet.Cell<String, String, Integer>>>) sheet
                .rowCells(0, 2)
                .iterator();
        final com.landawn.abacus.util.stream.ObjIteratorEx<Sheet.Cell<String, String, Integer>> inner = (com.landawn.abacus.util.stream.ObjIteratorEx<Sheet.Cell<String, String, Integer>>) outer
                .next()
                .iterator();
        org.junit.jupiter.api.Assertions.assertEquals(3L, inner.count());
        org.junit.jupiter.api.Assertions.assertFalse(inner.hasNext());
        org.junit.jupiter.api.Assertions.assertEquals(1L, outer.count());
        org.junit.jupiter.api.Assertions.assertFalse(outer.hasNext());

        final com.landawn.abacus.util.stream.ObjIteratorEx<com.landawn.abacus.util.stream.Stream<Sheet.Cell<String, String, Integer>>> colOuter = (com.landawn.abacus.util.stream.ObjIteratorEx<com.landawn.abacus.util.stream.Stream<Sheet.Cell<String, String, Integer>>>) sheet
                .columnCells(0, 3)
                .iterator();
        org.junit.jupiter.api.Assertions.assertEquals(3L, colOuter.count());
        org.junit.jupiter.api.Assertions.assertFalse(colOuter.hasNext());
    }

    @Test
    public void testRowValuesViewCreatedBeforeInitialization() {
        // regression: the lazy view captured rowIndex = -1 on an uninitialized sheet and crashed
        // with IndexOutOfBoundsException once a later write initialized the sheet
        final Sheet<String, String, Integer> sheet = new Sheet<>(CommonUtil.asList("r1"), CommonUtil.asList("c1", "c2"));
        final ImmutableList<Integer> row = sheet.rowValues("r1");

        org.junit.jupiter.api.Assertions.assertNull(row.get(0));

        sheet.set("r1", "c1", 42);

        org.junit.jupiter.api.Assertions.assertEquals(Integer.valueOf(42), row.get(0));
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class, () -> sheet.rowValues("missing"));
    }

    @Test
    public void testRow_asMap() {
        Map<String, Object> row1Map = objectSheet.rowAsMap("R1");
        assertEquals("V11", row1Map.get("C1"));
        assertEquals("V12", row1Map.get("C2"));
        assertNull(row1Map.get("C3"));
        assertEquals(3, row1Map.size());
        assertTrue(row1Map instanceof LinkedHashMap);
    }

    @Test
    public void testRow_asMap_uninitializedSheet() {
        Sheet<String, String, String> uninitializedSheet = new Sheet<>(upperRowKeys, colKeys);
        Map<String, String> row1Map = uninitializedSheet.rowAsMap("R1");
        assertNull(row1Map.get("C1"));
        assertEquals(3, row1Map.size());
    }

    @Test
    public void testRowAsMap() {
        Map<String, Integer> rowMap = sheet.rowAsMap("row1");
        assertNotNull(rowMap);
        assertEquals(3, rowMap.size());
        assertEquals(Integer.valueOf(1), rowMap.get("col1"));
        assertEquals(Integer.valueOf(2), rowMap.get("col2"));
        assertEquals(Integer.valueOf(3), rowMap.get("col3"));
    }

    @Test
    public void testRowAsMap_UninitializedSheet() {
        Sheet<String, String, Integer> uninitSheet = new Sheet<>(rowKeys, columnKeys);
        Map<String, Integer> rowMap = uninitSheet.rowAsMap("row1");
        assertEquals(3, rowMap.size());
        assertNull(rowMap.get("col1"));
    }

    @Test
    public void testRowAsMap_InvalidKey() {
        assertThrows(IllegalArgumentException.class, () -> sheet.rowAsMap("invalidRow"));
    }

    @Test
    public void testRow() {
        Map<String, Integer> row = sheet.rowAsMap("row1");
        assertNotNull(row);
        assertEquals(3, row.size());
        assertEquals(Integer.valueOf(1), row.get("col1"));
        assertEquals(Integer.valueOf(2), row.get("col2"));
        assertEquals(Integer.valueOf(3), row.get("col3"));
    }

    @Test
    public void testRowInvalidKey() {
        assertThrows(IllegalArgumentException.class, () -> {
            sheet.rowAsMap("invalidRow");
        });
    }

    @Test
    public void testRowMap() {
        Map<String, Map<String, Integer>> rowMap = sheet.rowsMap();
        assertNotNull(rowMap);
        assertEquals(3, rowMap.size());
        assertEquals(Integer.valueOf(1), rowMap.get("row1").get("col1"));
        assertEquals(Integer.valueOf(5), rowMap.get("row2").get("col2"));
        assertEquals(Integer.valueOf(9), rowMap.get("row3").get("col3"));
    }

    @Test
    public void testRowLength() {
        assertEquals(3, sheet.rowCount());
        assertEquals(0, emptySheet.rowCount());
    }

    @Test
    public void testRowValuesViewSizeFailsOnceItsRowIsGone() {
        // size() used to be the one accessor that still answered on a dead view: it reported the live column
        // count while every element read threw, so `for (int i = 0; i < v.size(); i++) v.get(i)` blew up on a
        // list that had just claimed three elements.
        final Sheet<String, String, Integer> s = Sheet.rows(Arrays.asList("R1", "R2"), Arrays.asList("C1", "C2", "C3"),
                new Integer[][] { { 1, 2, 3 }, { 4, 5, 6 } });
        final ImmutableList<Integer> removed = s.rowValues("R1");
        assertEquals(3, removed.size());

        s.removeRow("R1");
        assertThrows(IllegalArgumentException.class, removed::size);
        assertThrows(IllegalArgumentException.class, removed::isEmpty);
        assertThrows(IllegalArgumentException.class, () -> removed.stream().count());
        assertThrows(IllegalArgumentException.class, () -> removed.get(0));
        // An out-of-range index is still reported as such, ahead of the missing key.
        assertThrows(IndexOutOfBoundsException.class, () -> removed.get(5));

        // Renaming the key away is the same loss.
        final Sheet<String, String, Integer> renamed = Sheet.rows(Arrays.asList("R1"), Arrays.asList("C1", "C2", "C3"), new Integer[][] { { 1, 2, 3 } });
        final ImmutableList<Integer> byOldName = renamed.rowValues("R1");
        renamed.renameRow("R1", "RZ");
        assertThrows(IllegalArgumentException.class, byOldName::size);
        assertThrows(IllegalArgumentException.class, () -> byOldName.get(0));

        // With no columns the dead view used to be silently wrong rather than loudly wrong: it answered
        // 0 / true / "[]" / equals(emptyList) while a fresh rowValues for the same key threw.
        final Sheet<String, String, Integer> noColumns = new Sheet<>(Arrays.asList("R1", "R2"), Collections.<String> emptyList());
        final ImmutableList<Integer> noColumnsView = noColumns.rowValues("R1");
        assertEquals(0, noColumnsView.size());

        noColumns.removeRow("R1");
        assertThrows(IllegalArgumentException.class, noColumnsView::size);
        assertThrows(IllegalArgumentException.class, noColumnsView::isEmpty);
        assertThrows(IllegalArgumentException.class, noColumnsView::toString);
        assertThrows(IllegalArgumentException.class, () -> noColumnsView.equals(Collections.emptyList()));
        assertThrows(IllegalArgumentException.class, () -> noColumns.rowValues("R1"));

        // The two accessors the class doc exempts keep their plain List behaviour even here: index 0 is out of
        // range for a zero-column Sheet whether or not the row is still present, and containsAll of nothing is
        // vacuously true without ever touching the view.
        assertThrows(IndexOutOfBoundsException.class, () -> noColumnsView.get(0));
        assertTrue(noColumnsView.containsAll(Collections.emptyList()));

        // A live key is untouched: the view still tracks the other axis.
        final Sheet<String, String, Integer> live = Sheet.rows(Arrays.asList("R1", "R2"), Arrays.asList("C1", "C2"),
                new Integer[][] { { 1, 2 }, { 3, 4 } });
        final ImmutableList<Integer> liveView = live.rowValues("R1");
        live.removeRow("R2");
        assertEquals(2, liveView.size());
        assertFalse(liveView.isEmpty());
        assertEquals(Arrays.asList(1, 2), new ArrayList<>(liveView));
    }

    @Test
    public void testRowValuesViewIteratorResolvesItsRowKeyOnce() {
        // The view overrides iterator() so that iterating it costs one index-map lookup per element instead of
        // three: AbstractList's own iterator asks size() on every hasNext() - and size() now resolves the row
        // key - while ImmutableCollection wraps it in an ObjIterator whose next() calls hasNext() again. The
        // visible consequence of resolving the key once up front is that a dead view fails at iterator()
        // creation rather than at the first hasNext().
        final Sheet<String, String, Integer> s = Sheet.rows(Arrays.asList("R1", "R2"), Arrays.asList("C1", "C2", "C3"),
                new Integer[][] { { 1, 2, 3 }, { 4, 5, 6 } });
        final ImmutableList<Integer> view = s.rowValues("R1");

        assertEquals(Arrays.asList(1, 2, 3), new ArrayList<>(view));

        final Iterator<Integer> iter = view.iterator();
        assertTrue(iter.hasNext());
        assertEquals(Integer.valueOf(1), iter.next());
        assertEquals(Integer.valueOf(2), iter.next());
        assertEquals(Integer.valueOf(3), iter.next());
        assertFalse(iter.hasNext());
        assertThrows(NoSuchElementException.class, iter::next);

        // The iterator reads the live column count, not one captured when the view was made.
        s.addColumn("C4", Arrays.asList(7, 8));
        assertEquals(Arrays.asList(1, 2, 3, 7), new ArrayList<>(view));
        s.removeColumn("C1");
        assertEquals(Arrays.asList(2, 3, 7), new ArrayList<>(view));

        // Reordering still does not move the view off its key.
        s.moveRow("R1", 1);
        assertEquals(Arrays.asList(2, 3, 7), new ArrayList<>(view));

        // Once the row is gone, creating the iterator is itself the failure.
        s.removeRow("R1");
        assertThrows(IllegalArgumentException.class, view::iterator);
        assertThrows(IllegalArgumentException.class, () -> new ArrayList<>(view));
    }
}

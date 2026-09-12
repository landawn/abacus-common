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

public class SheetColumnTest extends SheetTestSupport {
    @Test
    public void testColumnOrientedStreamsPreserveColumnsWhenThereAreNoRows() {
        final Sheet<String, String, Integer> zeroRowSheet = new Sheet<>(Collections.emptyList(), Arrays.asList("c1", "c2"));

        assertEquals(2, zeroRowSheet.columnCells().count());
        assertEquals(2, zeroRowSheet.columnStreams().count());

        final List<Pair<String, Stream<Integer>>> columns = zeroRowSheet.columns().toList();
        assertEquals(Arrays.asList("c1", "c2"), columns.stream().map(Pair::left).toList());
        assertTrue(columns.get(0).right().toList().isEmpty());
        assertTrue(columns.get(1).right().toList().isEmpty());

        final List<Integer> mappedColumnSizes = zeroRowSheet.columns((columnIndex, column) -> column.length()).map(Pair::right).toList();
        assertEquals(Arrays.asList(0, 0), mappedColumnSizes);
    }

    @Test
    public void testColumnKeySet() {
        ImmutableSet<String> keys = sheet.columnKeySet();
        assertNotNull(keys);
        assertEquals(3, keys.size());
        assertTrue(keys.contains("col1"));
        assertTrue(keys.contains("col2"));
        assertTrue(keys.contains("col3"));
    }

    @Test
    public void testColumnKeySetImmutable() {
        ImmutableSet<String> keys = sheet.columnKeySet();
        assertThrows(UnsupportedOperationException.class, () -> {
            keys.add("col4");
        });
    }

    @Test
    public void testColumnValues_AllColumns() {
        ImmutableList<Integer> col2 = sheet.columnValues("col2");
        assertEquals(Arrays.asList(2, 5, 8), new ArrayList<>(col2));

        ImmutableList<Integer> col3 = sheet.columnValues("col3");
        assertEquals(Arrays.asList(3, 6, 9), new ArrayList<>(col3));
    }

    @Test
    public void testColumnValues() {
        ImmutableList<Integer> col = sheet.columnValues("col1");
        assertNotNull(col);
        assertEquals(3, col.size());
        assertEquals(Integer.valueOf(1), col.get(0));
        assertEquals(Integer.valueOf(4), col.get(1));
        assertEquals(Integer.valueOf(7), col.get(2));
    }

    @Test
    public void testColumnValues_UninitializedSheet() {
        Sheet<String, String, Integer> uninitSheet = new Sheet<>(rowKeys, columnKeys);
        ImmutableList<Integer> col = uninitSheet.columnValues("col1");
        assertEquals(3, col.size());
        assertNull(col.get(0));
    }

    @Test
    public void testColumnValues_frozenSheet_notMutated() {
        // Regression: columnValues is a read accessor and must not materialize (init()) a frozen Sheet,
        // which would mutate a supposedly-immutable, possibly-shared instance (observable via toString()).
        Sheet<String, String, Integer> frozen = new Sheet<>(rowKeys, columnKeys);
        frozen.freeze();
        final String before = frozen.toString();
        ImmutableList<Integer> col = frozen.columnValues("col1");
        assertEquals(rowKeys.size(), col.size());
        assertNull(col.get(0));
        assertEquals(before, frozen.toString(), "columnValues must not mutate a frozen Sheet");
    }

    @Test
    public void testColumnValues_InvalidKey() {
        assertThrows(IllegalArgumentException.class, () -> sheet.columnValues("invalidCol"));
    }

    @Test
    public void testColumnValuesViewFollowsColumnKeyThroughReordering() {
        // columnValues is a KEYED live view - the mirror of testRowValuesViewFollowsRowKeyThroughReordering.
        final Sheet<String, String, Integer> s = Sheet.rows(Arrays.asList("r1"), Arrays.asList("c1", "c2", "c3"), new Integer[][] { { 1, 2, 3 } });
        final ImmutableList<Integer> c2 = s.columnValues("c2");

        assertEquals(Integer.valueOf(2), c2.get(0));

        s.moveColumn("c2", 2);
        assertEquals(Arrays.asList("c1", "c3", "c2"), new ArrayList<>(s.columnKeySet()));
        assertEquals(Integer.valueOf(2), c2.get(0));

        s.swapColumns("c2", "c1");
        assertEquals(Integer.valueOf(2), c2.get(0));

        s.sortByColumnKey();
        assertEquals(Arrays.asList("c1", "c2", "c3"), new ArrayList<>(s.columnKeySet()));
        assertEquals(Integer.valueOf(2), c2.get(0));

        s.set("r1", "c2", 42);
        assertEquals(Integer.valueOf(42), c2.get(0));
    }

    @Test
    public void testColumnValuesViewThrowsAfterItsColumnIsRemoved() {
        final Sheet<String, String, Integer> s = Sheet.rows(Arrays.asList("r1"), Arrays.asList("c1", "c2"), new Integer[][] { { 1, 2 } });
        final ImmutableList<Integer> c1 = s.columnValues("c1");
        assertEquals(Integer.valueOf(1), c1.get(0));

        s.removeColumn("c2");
        assertEquals(Integer.valueOf(1), c1.get(0));

        s.removeColumn("c1");
        assertThrows(IllegalArgumentException.class, () -> c1.get(0));
    }

    @Test
    public void testColumnValuesViewOnUninitializedSheet() {
        final Sheet<String, String, Integer> s = new Sheet<>(Arrays.asList("r1", "r2"), Arrays.asList("c1"));
        final ImmutableList<Integer> c1 = s.columnValues("c1");
        assertEquals(2, c1.size());
        assertNull(c1.get(0));

        s.set("r2", "c1", 5);
        assertNull(c1.get(0));
        assertEquals(Integer.valueOf(5), c1.get(1));
    }

    @Test
    public void testColumn_asMap() {
        Map<String, Object> col1Map = objectSheet.columnAsMap("C1");
        assertEquals("V11", col1Map.get("R1"));
        assertEquals(100, col1Map.get("R2"));
        assertNull(col1Map.get("R3"));
        assertEquals(3, col1Map.size());
        assertTrue(col1Map instanceof LinkedHashMap);
    }

    @Test
    public void testColumn_asMap_uninitializedSheet() {
        Sheet<String, String, String> uninitializedSheet = new Sheet<>(rowKeys, colKeys);
        Map<String, String> col1Map = uninitializedSheet.columnAsMap("C1");
        assertNull(col1Map.get("R1"));
        assertEquals(3, col1Map.size());
    }

    @Test
    public void testColumnAsMap() {
        Map<String, Integer> colMap = sheet.columnAsMap("col1");
        assertNotNull(colMap);
        assertEquals(3, colMap.size());
        assertEquals(Integer.valueOf(1), colMap.get("row1"));
        assertEquals(Integer.valueOf(4), colMap.get("row2"));
        assertEquals(Integer.valueOf(7), colMap.get("row3"));
    }

    @Test
    public void testColumnAsMap_UninitializedSheet() {
        Sheet<String, String, Integer> uninitSheet = new Sheet<>(rowKeys, columnKeys);
        Map<String, Integer> colMap = uninitSheet.columnAsMap("col1");
        assertEquals(3, colMap.size());
        assertNull(colMap.get("row1"));
    }

    @Test
    public void testColumnAsMap_InvalidKey() {
        assertThrows(IllegalArgumentException.class, () -> sheet.columnAsMap("invalidCol"));
    }

    @Test
    public void testColumn() {
        Map<String, Integer> column = sheet.columnAsMap("col1");
        assertNotNull(column);
        assertEquals(3, column.size());
        assertEquals(Integer.valueOf(1), column.get("row1"));
        assertEquals(Integer.valueOf(4), column.get("row2"));
        assertEquals(Integer.valueOf(7), column.get("row3"));
    }

    @Test
    public void testColumnInvalidKey() {
        assertThrows(IllegalArgumentException.class, () -> {
            sheet.columnAsMap("invalidCol");
        });
    }

    @Test
    public void testColumnMap() {
        Map<String, Map<String, Integer>> columnMap = sheet.columnsMap();
        assertNotNull(columnMap);
        assertEquals(3, columnMap.size());
        assertEquals(Integer.valueOf(1), columnMap.get("col1").get("row1"));
        assertEquals(Integer.valueOf(5), columnMap.get("col2").get("row2"));
        assertEquals(Integer.valueOf(9), columnMap.get("col3").get("row3"));
    }

    @Test
    public void testColumnLength() {
        assertEquals(3, sheet.columnCount());
        assertEquals(0, emptySheet.columnCount());
    }

    @Test
    public void testColumnValuesViewSizeFailsOnceItsColumnIsGone() {
        // The mirror of testRowValuesViewSizeFailsOnceItsRowIsGone: size() must not keep answering for a
        // column key that is gone while every element read throws.
        final Sheet<String, String, Integer> s = Sheet.rows(Arrays.asList("r1", "r2"), Arrays.asList("c1", "c2"),
                new Integer[][] { { 1, 2 }, { 3, 4 } });
        final ImmutableList<Integer> removed = s.columnValues("c1");
        assertEquals(2, removed.size());

        s.removeColumn("c1");
        assertThrows(IllegalArgumentException.class, removed::size);
        assertThrows(IllegalArgumentException.class, removed::isEmpty);
        assertThrows(IllegalArgumentException.class, () -> removed.stream().count());
        assertThrows(IllegalArgumentException.class, () -> removed.get(0));
        // An out-of-range index is still reported as such, ahead of the missing key.
        assertThrows(IndexOutOfBoundsException.class, () -> removed.get(5));

        // Renaming the key away is the same loss.
        final Sheet<String, String, Integer> renamed = Sheet.rows(Arrays.asList("r1", "r2"), Arrays.asList("c1"), new Integer[][] { { 1 }, { 2 } });
        final ImmutableList<Integer> byOldName = renamed.columnValues("c1");
        renamed.renameColumn("c1", "cZ");
        assertThrows(IllegalArgumentException.class, byOldName::size);
        assertThrows(IllegalArgumentException.class, () -> byOldName.get(0));

        // With no rows the dead view used to be silently wrong rather than loudly wrong.
        final Sheet<String, String, Integer> noRows = new Sheet<>(Collections.<String> emptyList(), Arrays.asList("c1", "c2"));
        final ImmutableList<Integer> noRowsView = noRows.columnValues("c1");
        assertEquals(0, noRowsView.size());

        noRows.removeColumn("c1");
        assertThrows(IllegalArgumentException.class, noRowsView::size);
        assertThrows(IllegalArgumentException.class, noRowsView::isEmpty);
        assertThrows(IllegalArgumentException.class, noRowsView::toString);
        assertThrows(IllegalArgumentException.class, () -> noRowsView.equals(Collections.emptyList()));
        assertThrows(IllegalArgumentException.class, () -> noRows.columnValues("c1"));

        // The two accessors the class doc exempts keep their plain List behaviour even here - see the row twin.
        assertThrows(IndexOutOfBoundsException.class, () -> noRowsView.get(0));
        assertTrue(noRowsView.containsAll(Collections.emptyList()));

        // A live key is untouched: the view still tracks the other axis.
        final Sheet<String, String, Integer> live = Sheet.rows(Arrays.asList("r1", "r2"), Arrays.asList("c1", "c2"),
                new Integer[][] { { 1, 2 }, { 3, 4 } });
        final ImmutableList<Integer> liveView = live.columnValues("c1");
        live.removeColumn("c2");
        assertEquals(2, liveView.size());
        assertEquals(Arrays.asList(1, 3), new ArrayList<>(liveView));
    }

    @Test
    public void testColumnValuesViewIteratorResolvesItsColumnKeyOnce() {
        // The mirror of testRowValuesViewIteratorResolvesItsRowKeyOnce: iterator() is overridden so that the
        // column key is resolved once per iteration instead of three times per element, which also means a dead
        // view fails when the iterator is created.
        final Sheet<String, String, Integer> s = Sheet.rows(Arrays.asList("r1", "r2", "r3"), Arrays.asList("c1", "c2"),
                new Integer[][] { { 1, 2 }, { 3, 4 }, { 5, 6 } });
        final ImmutableList<Integer> view = s.columnValues("c1");

        assertEquals(Arrays.asList(1, 3, 5), new ArrayList<>(view));

        final Iterator<Integer> iter = view.iterator();
        assertTrue(iter.hasNext());
        assertEquals(Integer.valueOf(1), iter.next());
        assertEquals(Integer.valueOf(3), iter.next());
        assertEquals(Integer.valueOf(5), iter.next());
        assertFalse(iter.hasNext());
        assertThrows(NoSuchElementException.class, iter::next);

        // The iterator reads the live row count, not one captured when the view was made.
        s.addRow("r4", Arrays.asList(7, 8));
        assertEquals(Arrays.asList(1, 3, 5, 7), new ArrayList<>(view));
        s.removeRow("r1");
        assertEquals(Arrays.asList(3, 5, 7), new ArrayList<>(view));

        // Reordering still does not move the view off its key.
        s.moveColumn("c1", 1);
        assertEquals(Arrays.asList(3, 5, 7), new ArrayList<>(view));

        // Once the column is gone, creating the iterator is itself the failure.
        s.removeColumn("c1");
        assertThrows(IllegalArgumentException.class, view::iterator);
        assertThrows(IllegalArgumentException.class, () -> new ArrayList<>(view));
    }
}

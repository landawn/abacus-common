package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

/**
 * Regressions for the 2026-09-21 Dataset/RowDataset review: duplicate {@code merge} selections (C-019)
 * and slice schema/cursor fail-fast (C-020).
 */
public class DatasetSheetIterativeReview20260921Test extends TestBase {

    private static final String EMOJI = "\uD83D\uDE00";

    @Test
    void merge_duplicateSelection_isRejectedAndLeavesTheReceiverUntouched() {
        final Dataset left = Dataset.rows(List.of("a", "b"), new Object[][] { { 1, "x" }, { 2, "y" } });
        final Dataset right = Dataset.rows(List.of("a", "c"), new Object[][] { { 3, "z" }, { 4, "w" } });

        final IllegalArgumentException error = assertThrows(IllegalArgumentException.class, () -> left.merge(right, List.of("a", "a")));

        assertTrue(error.getMessage().contains("Duplicated column names"), error.getMessage());
        assertTrue(error.getMessage().contains("a is listed more than once"), error.getMessage());
        assertEquals(List.of("a", "b"), left.columnNames());
        assertEquals(2, left.size());
        assertEquals(1, (Integer) left.get(0, 0));
        assertEquals("y", left.get(1, 1));
    }

    @Test
    void merge_duplicateSelectionOnTheRangeOverload_isRejected() {
        final Dataset left = Dataset.rows(List.of("a"), new Object[][] { { 1 } });
        final Dataset right = Dataset.rows(List.of("a"), new Object[][] { { 2 }, { 3 } });

        final IllegalArgumentException error = assertThrows(IllegalArgumentException.class, () -> left.merge(right, 0, 1, List.of("a", "a")));

        assertTrue(error.getMessage().contains("Duplicated column names"), error.getMessage());
        assertEquals(1, left.size());
        assertEquals(1, (Integer) left.get(0, 0));
    }

    @Test
    void merge_duplicateOfAColumnThatExistsOnlyOnTheSource_isRejected() {
        final Dataset left = Dataset.rows(List.of("a"), new Object[][] { { 1 } });
        final Dataset right = Dataset.rows(List.of("c"), new Object[][] { { 9 } });

        assertThrows(IllegalArgumentException.class, () -> left.merge(right, List.of("c", "c")));
        assertEquals(List.of("a"), left.columnNames());
        assertEquals(1, left.size());
    }

    @Test
    void merge_unicodeDuplicateName_namesTheColumnAndDoesNotAppend() {
        final Dataset left = Dataset.rows(List.of(EMOJI), new Object[][] { { "keep" } });
        final Dataset right = Dataset.rows(List.of(EMOJI, "名前"), new Object[][] { { "new", "列" } });

        final IllegalArgumentException error = assertThrows(IllegalArgumentException.class, () -> left.merge(right, List.of(EMOJI, EMOJI)));

        assertTrue(error.getMessage().contains(EMOJI), error.getMessage());
        assertEquals(List.of(EMOJI), left.columnNames());
        assertEquals("keep", left.get(0, 0));
    }

    @Test
    void merge_nullEmptyAndUnknownSelections_areRejected() {
        final Dataset left = Dataset.rows(List.of("a"), new Object[][] { { 1 } });
        final Dataset right = Dataset.rows(List.of("a"), new Object[][] { { 2 } });

        assertThrows(IllegalArgumentException.class, () -> left.merge(right, (List<String>) null));
        assertThrows(IllegalArgumentException.class, () -> left.merge(right, List.of()));
        assertThrows(IllegalArgumentException.class, () -> left.merge(right, Arrays.asList("a", null)));
        assertThrows(IllegalArgumentException.class, () -> left.merge(right, 0, 0, List.of("a", "a")));
        assertEquals(1, left.size());
        assertEquals(1, (Integer) left.get(0, 0));
    }

    @Test
    void merge_emptyDatasetsAndADistinctSelection_stillMerge() {
        final Dataset emptyLeft = Dataset.rows(List.of("a", "b"), new Object[][] {});
        final Dataset emptyRight = Dataset.rows(List.of("a"), new Object[][] {});
        assertThrows(IllegalArgumentException.class, () -> emptyLeft.merge(emptyRight, List.of("a", "a")));
        assertEquals(0, emptyLeft.size());

        final Dataset left = Dataset.rows(List.of("a", "b"), new Object[][] { { 1, "héllo" }, { null, "" } });
        final Dataset right = Dataset.rows(List.of("a", "c"), new Object[][] { { 8, EMOJI }, { 9, "名前" } });
        left.merge(right, 1, 2, List.of("c", "a"));

        assertEquals(List.of("a", "b", "c"), left.columnNames());
        assertEquals(3, left.size());
        assertEquals(1, (Integer) left.get(0, 0));
        assertEquals("héllo", left.get(0, 1));
        assertNull(left.get(0, 2));
        assertNull(left.get(1, 0));
        assertEquals("", left.get(1, 1));
        assertEquals(9, (Integer) left.get(2, 0));
        assertNull(left.get(2, 1));
        assertEquals("名前", left.get(2, 2));
    }

    @Test
    void sliceLookupAndCursor_failAfterAParentRowChange() {
        final Dataset root = Dataset.rows(List.of("a", "b", EMOJI), new Object[][] { { 1, "x", "é" }, { 2, "y", "ñ" } });
        final Dataset slice = root.slice(0, 1);
        final Dataset nested = slice.slice(0, 1);
        final Dataset emptyWindow = root.slice(0, 0, List.of("a"));

        assertTrue(slice.containsColumn("a"));
        assertTrue(slice.containsColumn(EMOJI));
        assertFalse(slice.containsColumn("missing"));
        assertFalse(slice.containsColumn(null));
        assertFalse(slice.containsColumn(""));
        assertTrue(slice.containsAllColumns(List.of("b", "a", "b")));
        assertTrue(slice.containsAllColumns(List.of()));
        assertEquals(0, slice.getColumnIndex("a"));
        assertEquals(2, slice.getColumnIndex(EMOJI));
        assertArrayEquals(new int[] { 1, 0 }, slice.getColumnIndexes(List.of("b", "a")));
        assertArrayEquals(new int[] { 0, 0 }, slice.getColumnIndexes(List.of("a", "a")));
        assertEquals(0, slice.getColumnIndexes(null).length);
        assertEquals(0, slice.getColumnIndexes(List.of()).length);
        assertEquals(0, slice.currentRowIndex());
        slice.moveToRow(0);
        assertEquals(0, slice.currentRowIndex());
        assertThrows(IllegalArgumentException.class, () -> slice.getColumnIndex("missing"));
        assertThrows(IllegalArgumentException.class, () -> slice.containsAllColumns(null));

        root.removeRow(0);

        assertThrows(java.util.ConcurrentModificationException.class, () -> slice.containsColumn("a"));
        assertThrows(java.util.ConcurrentModificationException.class, () -> slice.containsColumn(null));
        assertThrows(java.util.ConcurrentModificationException.class, () -> slice.containsAllColumns(List.of("a")));
        assertThrows(java.util.ConcurrentModificationException.class, () -> slice.containsAllColumns(List.of()));
        assertThrows(java.util.ConcurrentModificationException.class, () -> slice.containsAllColumns(null));
        assertThrows(java.util.ConcurrentModificationException.class, () -> slice.getColumnIndex("a"));
        assertThrows(java.util.ConcurrentModificationException.class, () -> slice.getColumnIndexes(List.of("b", "a")));
        assertThrows(java.util.ConcurrentModificationException.class, () -> slice.getColumnIndexes(null));
        assertThrows(java.util.ConcurrentModificationException.class, slice::currentRowIndex);
        assertThrows(java.util.ConcurrentModificationException.class, () -> nested.containsColumn(EMOJI));
        assertThrows(java.util.ConcurrentModificationException.class, nested::currentRowIndex);
        assertThrows(java.util.ConcurrentModificationException.class, () -> emptyWindow.getColumnIndexes(List.of()));
        assertThrows(java.util.ConcurrentModificationException.class, slice::size);
    }

    @Test
    void sliceLookup_staysValidAcrossColumnRenameCellEditsAndTrim() {
        final Dataset root = Dataset.rows(List.of("a", "b"), new Object[][] { { 1, 2 }, { 3, 4 } });
        final Dataset slice = root.slice(0, 2);
        root.renameColumn("a", "renamed");
        root.set(0, 0, 99);
        root.trimToSize();

        assertTrue(slice.containsColumn("a"));
        assertFalse(slice.containsColumn("renamed"));
        assertEquals(0, slice.getColumnIndex("a"));
        assertEquals(99, (Integer) slice.get(0, 0));
        assertEquals(0, slice.currentRowIndex());
        assertEquals(2, slice.size());
    }

    @Test
    void rootCursorAndLookup_areUnchangedByTheSliceCheck() {
        final Dataset root = Dataset.rows(List.of("a"), new Object[][] { { 1 }, { 2 }, { 3 } });
        root.moveToRow(2);
        root.removeRow(2);

        assertEquals(1, root.currentRowIndex());
        assertTrue(root.containsColumn("a"));
        assertFalse(root.containsColumn(null));
        assertArrayEquals(new int[] { 0, 0 }, root.getColumnIndexes(List.of("a", "a")));
        assertEquals(0, root.getColumnIndexes(null).length);
        final AtomicReference<IllegalArgumentException> missing = new AtomicReference<>();
        missing.set(assertThrows(IllegalArgumentException.class, () -> root.getColumnIndex("nope")));
        assertTrue(missing.get().getMessage().contains("nope"), missing.get().getMessage());
    }
}

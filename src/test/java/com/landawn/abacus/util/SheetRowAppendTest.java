package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.lang.management.ManagementFactory;
import java.util.AbstractCollection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("unit")
class SheetRowAppendTest {
    @Test
    @Tag("slow-test")
    void indexedAppendDoesNotAllocateAnotherCopyOfTheRow() {
        final java.lang.management.ThreadMXBean bean = ManagementFactory.getThreadMXBean();
        assumeTrue(bean instanceof com.sun.management.ThreadMXBean, "per-thread allocation counters unavailable");
        final com.sun.management.ThreadMXBean allocations = (com.sun.management.ThreadMXBean) bean;
        assumeTrue(allocations.isThreadAllocatedMemorySupported() && allocations.isThreadAllocatedMemoryEnabled(),
                "per-thread allocation counters unavailable");

        for (int i = 0; i < 10; i++) {
            preparedSheet(8).addRow("new", Collections.nCopies(8, 7));
            preparedSheet(8).addRow(1, "new", Collections.nCopies(8, 7));
        }

        final int width = 10_000;
        final Sheet<String, Integer, Integer> appended = preparedSheet(width);
        final Sheet<String, Integer, Integer> indexed = preparedSheet(width);
        final List<Integer> row = Collections.nCopies(width, 7);

        final long beforeAppend = allocations.getCurrentThreadAllocatedBytes();
        appended.addRow("new", row);
        final long appendBytes = allocations.getCurrentThreadAllocatedBytes() - beforeAppend;
        final long beforeIndexed = allocations.getCurrentThreadAllocatedBytes();
        indexed.addRow(1, "new", row);
        final long indexedBytes = allocations.getCurrentThreadAllocatedBytes() - beforeIndexed;

        assertEquals(appended, indexed);
        // Both calls append the same data; allow bookkeeping noise but not another width-sized array.
        assertTrue(indexedBytes - appendBytes < width * 2L, "Indexed append allocated " + indexedBytes + " bytes; ordinary append allocated " + appendBytes);
    }

    @Test
    void indexedAppendSnapshotsLiveRowValuesAndCallerOwnedValues() {
        final Sheet<String, String, Integer> sheet = Sheet.rows(List.of("first"), List.of("a", "b"), new Integer[][] { { 1, 2 } });
        sheet.addRow(sheet.rowCount(), "second", sheet.rowValues("first"));
        sheet.set("first", "a", 9);
        assertEquals(List.of(1, 2), sheet.rowValues("second"));

        final List<Integer> values = new ArrayList<>(List.of(3, 4));
        sheet.addRow(sheet.rowCount(), "third", values);
        values.set(0, 8);
        assertEquals(List.of(3, 4), sheet.rowValues("third"));
        assertEquals(List.of("first", "second", "third"), new ArrayList<>(sheet.rowKeySet()));
    }

    @Test
    void indexedAppendAcceptsNullAndEmptyRows() {
        final Sheet<String, String, Integer> sheet = Sheet.rows(List.of("first"), List.of("a", "b"), new Integer[][] { { 1, 2 } });
        sheet.addRow(sheet.rowCount(), "null", null);
        sheet.addRow(sheet.rowCount(), "empty", List.of());

        assertEquals(Arrays.asList(null, null), sheet.rowValues("null"));
        assertEquals(Arrays.asList(null, null), sheet.rowValues("empty"));
        assertEquals(List.of(1, 2), sheet.rowValues("first"));
        assertEquals(List.of("first", "null", "empty"), new ArrayList<>(sheet.rowKeySet()));
    }

    @Test
    void indexedAppendSupportsInitiallyEmptyAxes() {
        final Sheet<String, String, Integer> noRows = new Sheet<>(List.of(), List.of("a", "b"));
        noRows.addRow(0, "first", List.of(1, 2));
        assertEquals(List.of(1, 2), noRows.rowValues("first"));

        final Sheet<String, String, Integer> noColumns = new Sheet<>(List.of(), List.of());
        noColumns.addRow(0, "first", List.of());
        noColumns.addRow(1, "second", null);
        assertEquals(List.of("first", "second"), new ArrayList<>(noColumns.rowKeySet()));
        assertEquals(0, noColumns.columnCount());
        assertTrue(noColumns.rowValues("first").isEmpty());
        assertTrue(noColumns.rowValues("second").isEmpty());
    }

    @Test
    void indexedAppendDoesNotMutateSheetWhenSnapshotFails() {
        final Sheet<String, String, Integer> sheet = Sheet.rows(List.of("first"), List.of("a", "b"), new Integer[][] { { 1, 2 } });
        final IllegalStateException failure = new IllegalStateException("snapshot failed");
        final Collection<Integer> values = new AbstractCollection<>() {
            @Override
            public Iterator<Integer> iterator() {
                throw failure;
            }

            @Override
            public int size() {
                return 2;
            }
        };

        assertSame(failure, assertThrows(IllegalStateException.class, () -> sheet.addRow(sheet.rowCount(), "second", values)));
        assertEquals(List.of("first"), new ArrayList<>(sheet.rowKeySet()));
        assertEquals(List.of(1, 2), sheet.rowValues("first"));
        sheet.addRow(sheet.rowCount(), "second", List.of(3, 4));
        assertEquals(List.of(3, 4), sheet.rowValues("second"));
    }

    @Test
    void indexedAppendValidatesBeforeChangingTheSheet() {
        final Sheet<String, String, Integer> sheet = Sheet.rows(List.of("first"), List.of("a", "b"), new Integer[][] { { 1, 2 } });
        assertThrows(IllegalArgumentException.class, () -> sheet.addRow(sheet.rowCount(), "second", List.of(3)));
        assertThrows(IllegalArgumentException.class, () -> sheet.addRow(sheet.rowCount(), "first", List.of(3, 4)));
        assertThrows(IllegalArgumentException.class, () -> sheet.addRow(sheet.rowCount(), null, List.of(3, 4)));
        assertEquals(List.of("first"), new ArrayList<>(sheet.rowKeySet()));
        assertEquals(List.of(1, 2), sheet.rowValues("first"));
        sheet.freeze();
        assertThrows(IllegalStateException.class, () -> sheet.addRow(sheet.rowCount(), "second", List.of(3, 4)));
        assertEquals(List.of("first"), new ArrayList<>(sheet.rowKeySet()));
    }

    private static Sheet<String, Integer, Integer> preparedSheet(final int width) {
        final List<Integer> columns = new ArrayList<>(width);
        for (int i = 0; i < width; i++) {
            columns.add(i);
        }
        final List<Integer> values = Collections.nCopies(width, 1);
        final Sheet<String, Integer, Integer> sheet = Sheet.rows(List.of("base", "spare"), columns, List.of(values, values));
        // Leave room in each column so resizing does not obscure the row snapshot allocation.
        sheet.removeRow("spare");
        return sheet;
    }
}

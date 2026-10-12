package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.AbstractCollection;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

@Tag("unit")
class SheetColumnSnapshotAuditTest extends TestBase {
    @Test
    void columnSnapshotsOwnTheCollectionButShareMutableCellValues() {
        for (final int position : new int[] { -1, 0, 1, 2 }) {
            final Sheet<String, String, StringBuilder> sheet = new Sheet<>(List.of("r1", "r2"), List.of("c1", "c2"));
            final StringBuilder first = new StringBuilder("first");
            final StringBuilder second = new StringBuilder("second");
            final List<StringBuilder> input = new ArrayList<>(List.of(first, second));

            addColumn(sheet, position, input);
            input.clear();
            first.append(" changed");

            assertSame(first, sheet.get("r1", "new"));
            assertSame(second, sheet.get("r2", "new"));
            assertEquals("first changed", sheet.get("r1", "new").toString());
            sheet.set("r2", "new", new StringBuilder("replacement"));
            assertEquals("second", second.toString());
            assertEquals(List.of(), input);
            assertEquals("new", new ArrayList<>(sheet.columnKeySet()).get(position < 0 ? 2 : position));
        }
    }

    @Test
    void failingInputSnapshotLeavesKeysCellsAndLiveViewsUnchanged() {
        for (final int position : new int[] { -1, 0, 1, 2 }) {
            final Sheet<String, String, Integer> sheet = Sheet.rows(List.of("r1", "r2"), List.of("c1", "c2"), new Integer[][] { { 1, 2 }, { 3, 4 } });
            final Sheet<String, String, Integer> before = sheet.copy();
            final List<Integer> liveColumn = sheet.columnValues("c2");
            final IllegalStateException failure = new IllegalStateException("source failed after its first value");
            final Collection<Integer> input = new AbstractCollection<>() {
                @Override
                public int size() {
                    return 2;
                }

                @Override
                public Iterator<Integer> iterator() {
                    return new Iterator<>() {
                        private int index;

                        @Override
                        public boolean hasNext() {
                            return index < 2;
                        }

                        @Override
                        public Integer next() {
                            if (index++ == 0) {
                                return 10;
                            }
                            throw failure;
                        }
                    };
                }
            };

            // Snapshotting must finish before insertion shifts the key indexes or appends any cells.
            assertSame(failure, assertThrows(IllegalStateException.class, () -> addColumn(sheet, position, input)));
            assertEquals(before, sheet);
            assertEquals(List.of(2, 4), liveColumn);
            sheet.set("r2", "c2", 40);
            assertEquals(List.of(2, 40), liveColumn);
        }
    }

    private static <V> void addColumn(final Sheet<String, String, V> sheet, final int position, final Collection<V> values) {
        if (position < 0) {
            sheet.addColumn("new", values);
        } else {
            sheet.addColumn(position, "new", values);
        }
    }
}

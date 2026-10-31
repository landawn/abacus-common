package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

/**
 * Correctness regressions for the column-wise Sheet.putAll(Sheet) of the 2026-10-19 performance review (restored
 * 2026-10-29). They pin the cell mapping and the failures, so they belong to the unit suite rather than to the
 * slow-test SheetPerformanceMemoryTest they were first restored into.
 */
public class SheetReviewFollowupTest extends TestBase {
    // ---- perf review 2026-10-19 G205 begin ----
    @Nested
    class R1019G205 {

        private Sheet<String, String, Integer> keyed(final int rowCount, final int columnCount, final boolean reverse, final int seed, final boolean fill) {
            final List<String> rowKeys = new ArrayList<>();
            final List<String> columnKeys = new ArrayList<>();

            for (int i = 0; i < rowCount; i++) {
                rowKeys.add("r" + i);
            }

            for (int j = 0; j < columnCount; j++) {
                columnKeys.add("c" + j);
            }

            if (reverse) {
                java.util.Collections.reverse(rowKeys);
                java.util.Collections.rotate(columnKeys, 1);
            }

            final Sheet<String, String, Integer> result = new Sheet<>(rowKeys, columnKeys);

            if (fill) {
                for (int i = 0; i < rowCount; i++) {
                    for (int j = 0; j < columnCount; j++) {
                        result.setAt(i, j, (i * 7 + j * 3 + seed) % 5 == 0 ? null : seed * 1000 + i * 37 + j);
                    }
                }
            }

            return result;
        }

        private List<List<Integer>> expectedPutAll(final Sheet<String, String, Integer> target, final Sheet<String, String, Integer> source) {
            final List<List<Integer>> expected = new ArrayList<>();

            for (final String rowKey : target.rowKeySet()) {
                final List<Integer> row = new ArrayList<>();

                for (final String columnKey : target.columnKeySet()) {
                    row.add(source.containsCell(rowKey, columnKey) ? source.get(rowKey, columnKey) : target.get(rowKey, columnKey));
                }

                expected.add(row);
            }

            return expected;
        }

        private List<List<Integer>> grid(final Sheet<String, String, Integer> sheet) {
            final List<List<Integer>> result = new ArrayList<>();

            for (final String rowKey : sheet.rowKeySet()) {
                result.add(new ArrayList<>(sheet.rowValues(rowKey)));
            }

            return result;
        }

        // G205-01: putAll(Sheet) now copies column by column; pins the cell mapping for permuted/subset keys,
        // uninitialized sources and targets, empty sources, self-putAll and the frozen/missing-key failures.
        @Test
        public void testPutAll_columnWiseCopyKeepsCellMapping() {
            final int[][] shapes = { { 1, 1 }, { 3, 2 }, { 2, 5 }, { 7, 4 }, { 40, 3 }, { 3, 40 } };

            for (final int[] shape : shapes) {
                for (int sourceRows = 0; sourceRows <= shape[0]; sourceRows += Math.max(1, shape[0] / 2)) {
                    for (int sourceColumns = 0; sourceColumns <= shape[1]; sourceColumns += Math.max(1, shape[1] / 2)) {
                        for (final boolean targetInit : new boolean[] { true, false }) {
                            for (final boolean sourceInit : new boolean[] { true, false }) {
                                final Sheet<String, String, Integer> target = keyed(shape[0], shape[1], false, 1, targetInit);
                                final Sheet<String, String, Integer> full = keyed(shape[0], shape[1], true, 2, true);
                                final List<String> rowKeys = new ArrayList<>(full.rowKeySet()).subList(0, sourceRows);
                                final List<String> columnKeys = new ArrayList<>(full.columnKeySet()).subList(0, sourceColumns);
                                final Sheet<String, String, Integer> source = new Sheet<>(rowKeys, columnKeys);

                                if (sourceInit && sourceRows > 0 && sourceColumns > 0) {
                                    for (final String r : rowKeys) {
                                        for (final String c : columnKeys) {
                                            source.set(r, c, full.get(r, c));
                                        }
                                    }
                                }

                                final List<List<Integer>> expected = expectedPutAll(target, source);
                                target.putAll(source);
                                assertEquals(expected, grid(target), java.util.Arrays.toString(shape) + " " + sourceRows + "x" + sourceColumns);
                            }
                        }
                    }
                }
            }

            final Sheet<String, String, Integer> self = keyed(6, 4, false, 3, true);
            final List<List<Integer>> before = grid(self);
            self.putAll(self);
            assertEquals(before, grid(self));

            final Sheet<String, String, Integer> uninitializedSelf = keyed(3, 3, false, 3, false);
            uninitializedSelf.putAll(uninitializedSelf);
            assertEquals(grid(keyed(3, 3, false, 3, false)), grid(uninitializedSelf));

            final Sheet<String, String, Integer> frozen = keyed(2, 2, false, 1, true);
            frozen.freeze();
            assertThrows(IllegalStateException.class, () -> frozen.putAll(keyed(2, 2, false, 2, true)));

            final Sheet<String, String, Integer> small = keyed(2, 2, false, 1, true);
            final List<List<Integer>> smallBefore = grid(small);
            assertThrows(IllegalArgumentException.class, () -> small.putAll(keyed(3, 2, false, 2, true)));
            assertThrows(IllegalArgumentException.class, () -> small.putAll(keyed(2, 3, false, 2, true)));
            assertEquals(smallBefore, grid(small));
        }
    }
    // ---- perf review 2026-10-19 G205 end ----
}

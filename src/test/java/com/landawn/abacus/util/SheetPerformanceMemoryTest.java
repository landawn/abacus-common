package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

/** Performance and memory regression contracts for Sheet. */
@Tag("slow-test")
public class SheetPerformanceMemoryTest extends TestBase {

    // ---- perf review 2026-10-08 G065 end ----
    // ---- perf review 2026-10-08 G066 begin ----
    @Nested
    class G066 {

        private Sheet<String, String, Integer> sheet(final int rowCount, final int columnCount) {
            final List<String> rowKeys = new ArrayList<>();
            final List<String> columnKeys = new ArrayList<>();
            final Integer[][] rows = new Integer[rowCount][columnCount];

            for (int i = 0; i < rowCount; i++) {
                rowKeys.add("r" + i);

                for (int j = 0; j < columnCount; j++) {
                    rows[i][j] = (i + j) % 4 == 3 ? null : i * 10 + j;
                }
            }

            for (int j = 0; j < columnCount; j++) {
                columnKeys.add("c" + j);
            }

            return Sheet.rows(rowKeys, columnKeys, rows);
        }

        private List<Integer> expectedRowMajor(final Sheet<String, String, Integer> sheet, final int fromRowIndex, final int toRowIndex) {
            final List<Integer> expected = new ArrayList<>();

            for (int i = fromRowIndex; i < toRowIndex; i++) {
                for (int j = 0; j < sheet.columnCount(); j++) {
                    expected.add(sheet.getAt(i, j));
                }
            }

            return expected;
        }

        private List<Integer> expectedColumnMajor(final Sheet<String, String, Integer> sheet, final int fromColumnIndex, final int toColumnIndex) {
            final List<Integer> expected = new ArrayList<>();

            for (int j = fromColumnIndex; j < toColumnIndex; j++) {
                for (int i = 0; i < sheet.rowCount(); i++) {
                    expected.add(sheet.getAt(i, j));
                }
            }

            return expected;
        }

        private List<Integer> drain(final Iterator<Integer> iter) {
            final List<Integer> result = new ArrayList<>();

            while (iter.hasNext()) {
                result.add(iter.next());
            }

            return result;
        }

        private com.landawn.abacus.util.stream.ObjIteratorEx<Integer> iteratorEx(final com.landawn.abacus.util.stream.Stream<Integer> stream) {
            return (com.landawn.abacus.util.stream.ObjIteratorEx<Integer>) stream.iterator();
        }

        private void assertAdvanceAndCount(final java.util.function.Supplier<com.landawn.abacus.util.stream.Stream<Integer>> streamSupplier,
                final List<Integer> expected) {
            final long[] advances = { -1, 0, 1, 2, 3, 4, 5, 7, 11, 100, Long.MAX_VALUE };

            for (int consumed = 0; consumed <= expected.size(); consumed++) {
                for (final long n : advances) {
                    final com.landawn.abacus.util.stream.ObjIteratorEx<Integer> iter = iteratorEx(streamSupplier.get());

                    for (int k = 0; k < consumed; k++) {
                        assertEquals(expected.get(k), iter.next());
                    }

                    iter.advance(n);

                    final int skipped = (int) Math.min(expected.size() - consumed, Math.max(0, n));
                    assertEquals(expected.subList(consumed + skipped, expected.size()), drain(iter), "consumed=" + consumed + ", n=" + n);
                    assertFalse(iter.hasNext());
                    assertThrows(java.util.NoSuchElementException.class, iter::next);
                }

                final com.landawn.abacus.util.stream.ObjIteratorEx<Integer> iter = iteratorEx(streamSupplier.get());

                for (int k = 0; k < consumed; k++) {
                    iter.next();
                }

                assertEquals(expected.size() - consumed, iter.count());
                assertFalse(iter.hasNext());
                assertThrows(java.util.NoSuchElementException.class, iter::next);
                assertEquals(0, iter.count());
                iter.advance(3);
                assertFalse(iter.hasNext());
            }
        }

        // G066-01: rowMajorStream steps a (row, column) pair along with its cursor instead of dividing the cursor per element.
        @Test
        public void testRowMajorStream_everyRangeOfEveryShape() {
            final int[][] shapes = { { 1, 1 }, { 1, 5 }, { 5, 1 }, { 3, 4 }, { 4, 3 }, { 2, 7 } };

            for (final int[] shape : shapes) {
                final Sheet<String, String, Integer> sheet = sheet(shape[0], shape[1]);

                for (int from = 0; from <= shape[0]; from++) {
                    for (int to = from; to <= shape[0]; to++) {
                        assertEquals(expectedRowMajor(sheet, from, to), sheet.rowMajorStream(from, to).toList(),
                                shape[0] + "x" + shape[1] + " " + from + "-" + to);
                    }
                }

                assertEquals(expectedRowMajor(sheet, 0, shape[0]), sheet.rowMajorStream().toList());
            }
        }

        // G066-01: columnMajorStream steps a (column, row) pair along with its cursor instead of dividing the cursor per element.
        @Test
        public void testColumnMajorStream_everyRangeOfEveryShape() {
            final int[][] shapes = { { 1, 1 }, { 1, 5 }, { 5, 1 }, { 3, 4 }, { 4, 3 }, { 7, 2 } };

            for (final int[] shape : shapes) {
                final Sheet<String, String, Integer> sheet = sheet(shape[0], shape[1]);

                for (int from = 0; from <= shape[1]; from++) {
                    for (int to = from; to <= shape[1]; to++) {
                        assertEquals(expectedColumnMajor(sheet, from, to), sheet.columnMajorStream(from, to).toList(),
                                shape[0] + "x" + shape[1] + " " + from + "-" + to);
                    }
                }

                assertEquals(expectedColumnMajor(sheet, 0, shape[1]), sheet.columnMajorStream().toList());
            }
        }

        // G066-01: advance(n) moves only the cursor (the cell position is re-derived on the next read); count() consumes the rest.
        @Test
        public void testRowMajorStream_advanceAndCountFromEveryPosition() {
            final Sheet<String, String, Integer> sheet = sheet(4, 3);
            final Sheet<String, String, Integer> narrow = sheet(3, 1);

            assertAdvanceAndCount(() -> sheet.rowMajorStream(), expectedRowMajor(sheet, 0, 4));
            assertAdvanceAndCount(() -> sheet.rowMajorStream(1, 3), expectedRowMajor(sheet, 1, 3));
            assertAdvanceAndCount(() -> narrow.rowMajorStream(), expectedRowMajor(narrow, 0, 3));
            assertEquals(expectedRowMajor(sheet, 0, 4).subList(5, 12), sheet.rowMajorStream().skip(5).toList());
            assertEquals(expectedRowMajor(sheet, 0, 4).subList(2, 9), sheet.rowMajorStream().skip(2).limit(7).toList());
        }

        // G066-01: advance(n) moves only the cursor (the cell position is re-derived on the next read); count() consumes the rest.
        @Test
        public void testColumnMajorStream_advanceAndCountFromEveryPosition() {
            final Sheet<String, String, Integer> sheet = sheet(3, 4);
            final Sheet<String, String, Integer> flat = sheet(1, 3);

            assertAdvanceAndCount(() -> sheet.columnMajorStream(), expectedColumnMajor(sheet, 0, 4));
            assertAdvanceAndCount(() -> sheet.columnMajorStream(1, 3), expectedColumnMajor(sheet, 1, 3));
            assertAdvanceAndCount(() -> flat.columnMajorStream(), expectedColumnMajor(flat, 0, 3));
            assertEquals(expectedColumnMajor(sheet, 0, 4).subList(5, 12), sheet.columnMajorStream().skip(5).toList());
            assertEquals(expectedColumnMajor(sheet, 0, 4).subList(2, 9), sheet.columnMajorStream().skip(2).limit(7).toList());
        }

        // G066-01: a stream created before the Sheet had storage returns nulls, and after the first write the live values, also
        // when its position moved on (next/advance) while there was no storage.
        @Test
        public void testRowMajorStream_firstWriteAfterPartialConsumption() {
            final Sheet<String, String, Integer> sheet = new Sheet<>(Arrays.asList("r0", "r1", "r2"), Arrays.asList("c0", "c1"));
            final com.landawn.abacus.util.stream.ObjIteratorEx<Integer> iter = iteratorEx(sheet.rowMajorStream());

            assertNull(iter.next()); // r0,c0
            iter.advance(1); // skips r0,c1
            assertNull(iter.next()); // r1,c0

            sheet.setAt(0, 0, 1);
            sheet.setAt(1, 1, 11);
            sheet.setAt(2, 0, 20);
            sheet.setAt(2, 1, 21);

            assertEquals(Integer.valueOf(11), iter.next()); // r1,c1
            assertEquals(Integer.valueOf(20), iter.next()); // r2,c0
            sheet.setAt(2, 1, 210);
            assertEquals(Integer.valueOf(210), iter.next()); // r2,c1
            assertFalse(iter.hasNext());

            final Sheet<String, String, Integer> other = new Sheet<>(Arrays.asList("r0", "r1"), Arrays.asList("c0", "c1", "c2"));
            final com.landawn.abacus.util.stream.ObjIteratorEx<Integer> iter2 = iteratorEx(other.rowMajorStream(1, 2));
            assertNull(iter2.next()); // r1,c0
            other.setAt(1, 2, 12);
            other.setAt(1, 1, 11);
            assertEquals(Arrays.asList(11, 12), drain(iter2));
            assertEquals(Arrays.asList(null, null, null, null, 11, 12), other.rowMajorStream().toList());
        }

        // G066-01: as above, column-major.
        @Test
        public void testColumnMajorStream_firstWriteAfterPartialConsumption() {
            final Sheet<String, String, Integer> sheet = new Sheet<>(Arrays.asList("r0", "r1"), Arrays.asList("c0", "c1", "c2"));
            final com.landawn.abacus.util.stream.ObjIteratorEx<Integer> iter = iteratorEx(sheet.columnMajorStream());

            assertNull(iter.next()); // r0,c0
            iter.advance(1); // skips r1,c0
            assertNull(iter.next()); // r0,c1

            sheet.setAt(1, 1, 11);
            sheet.setAt(0, 2, 2);
            sheet.setAt(1, 2, 12);

            assertEquals(Integer.valueOf(11), iter.next()); // r1,c1
            assertEquals(Integer.valueOf(2), iter.next()); // r0,c2
            sheet.setAt(1, 2, 120);
            assertEquals(Integer.valueOf(120), iter.next()); // r1,c2
            assertFalse(iter.hasNext());
            assertEquals(Arrays.asList(null, null, null, 11, 2, 120), sheet.columnMajorStream().toList());
        }

        // G066-01: (documented as undefined, pinned here as deterministic) a failed column read leaves the position unchanged and
        // a failed cell read has already moved past the cell, exactly as when the position was divided out of the cursor.
        @Test
        public void testRowMajorStream_failedReadsAfterStructuralChange() {
            final Sheet<String, String, Integer> columnRemoved = sheet(2, 3); // r0: 0 1 2, r1: 10 11 null
            final com.landawn.abacus.util.stream.ObjIteratorEx<Integer> iter = iteratorEx(columnRemoved.rowMajorStream());
            assertEquals(Integer.valueOf(0), iter.next());
            assertEquals(Integer.valueOf(1), iter.next());
            columnRemoved.removeColumn("c0"); // two columns left: c1, c2
            assertThrows(IndexOutOfBoundsException.class, iter::next); // column index 2 is gone; position unchanged
            assertThrows(IndexOutOfBoundsException.class, iter::next);
            assertTrue(iter.hasNext());
            iter.advance(1); // now at row 1, column index 0 (c1 after the removal)
            assertEquals(Integer.valueOf(11), iter.next());
            assertNull(iter.next());
            assertThrows(IndexOutOfBoundsException.class, iter::next);

            final Sheet<String, String, Integer> rowRemoved = sheet(3, 2); // r0: 0 1, r1: 10 11, r2: 20 null
            final com.landawn.abacus.util.stream.ObjIteratorEx<Integer> iter2 = iteratorEx(rowRemoved.rowMajorStream());
            assertEquals(Arrays.asList(0, 1, 10, 11), Arrays.asList(iter2.next(), iter2.next(), iter2.next(), iter2.next()));
            rowRemoved.removeRow("r0"); // rows left: r1, r2 at indexes 0, 1
            assertThrows(IndexOutOfBoundsException.class, iter2::next); // row index 2 is gone; position moved on
            assertThrows(IndexOutOfBoundsException.class, iter2::next);
            assertFalse(iter2.hasNext());
        }

        // G066-01: as above, column-major.
        @Test
        public void testColumnMajorStream_failedReadsAfterStructuralChange() {
            final Sheet<String, String, Integer> columnRemoved = sheet(2, 3); // c0: 0 10, c1: 1 11, c2: 2 null
            final com.landawn.abacus.util.stream.ObjIteratorEx<Integer> iter = iteratorEx(columnRemoved.columnMajorStream());
            assertEquals(Arrays.asList(0, 10, 1, 11), Arrays.asList(iter.next(), iter.next(), iter.next(), iter.next()));
            columnRemoved.removeColumn("c0"); // two columns left: c1, c2
            assertThrows(IndexOutOfBoundsException.class, iter::next); // column index 2 is gone; position unchanged
            assertTrue(iter.hasNext());
            columnRemoved.addColumn("c3", Arrays.asList(3, 13));
            assertEquals(Integer.valueOf(3), iter.next());
            assertEquals(Integer.valueOf(13), iter.next());
            assertFalse(iter.hasNext());

            final Sheet<String, String, Integer> rowRemoved = sheet(3, 2); // c0: 0 10 20, c1: 1 11 null
            final com.landawn.abacus.util.stream.ObjIteratorEx<Integer> iter2 = iteratorEx(rowRemoved.columnMajorStream());
            assertEquals(Arrays.asList(0, 10), Arrays.asList(iter2.next(), iter2.next()));
            rowRemoved.removeRow("r0"); // rows left: r1, r2 at indexes 0, 1
            assertThrows(IndexOutOfBoundsException.class, iter2::next); // row index 2 is gone; position moved on
            assertEquals(Integer.valueOf(11), iter2.next()); // column index 1, row index 0 (r1)
            assertNull(iter2.next());
            assertThrows(IndexOutOfBoundsException.class, iter2::next);
            assertFalse(iter2.hasNext());
        }
    }
}

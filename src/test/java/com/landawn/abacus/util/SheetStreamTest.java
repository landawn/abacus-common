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
import java.util.NoSuchElementException;
import java.util.function.Supplier;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.util.stream.ObjIteratorEx;
import com.landawn.abacus.util.stream.Stream;

public class SheetStreamTest extends SheetTestSupport {

    @Test
    public void testRowAxisInnerStreamsReadCellValuesLazily() {
        // Contract pin for the paragraph the six row-axis nested-stream accessors now document (their six
        // column-axis twins already did): the inner stream reads cell values when it is consumed, so the
        // Sheet's first value write is visible even when it happens after the inner stream was created.
        final List<Integer> expected = Arrays.asList(null, 42);

        Sheet<String, String, Integer> s = new Sheet<>(Arrays.asList("r1"), Arrays.asList("c1", "c2"));
        Stream<Integer> inner = s.rowStreams().toList().get(0);
        s.set("r1", "c2", 42);
        assertEquals(expected, inner.toList());

        s = new Sheet<>(Arrays.asList("r1"), Arrays.asList("c1", "c2"));
        inner = s.rowStreams(0, 1).toList().get(0);
        s.set("r1", "c2", 42);
        assertEquals(expected, inner.toList());

        s = new Sheet<>(Arrays.asList("r1"), Arrays.asList("c1", "c2"));
        inner = s.rows().toList().get(0).right();
        s.set("r1", "c2", 42);
        assertEquals(expected, inner.toList());

        s = new Sheet<>(Arrays.asList("r1"), Arrays.asList("c1", "c2"));
        inner = s.rows(0, 1).toList().get(0).right();
        s.set("r1", "c2", 42);
        assertEquals(expected, inner.toList());

        s = new Sheet<>(Arrays.asList("r1"), Arrays.asList("c1", "c2"));
        Stream<Sheet.Cell<String, String, Integer>> cellInner = s.rowCells().toList().get(0);
        s.set("r1", "c2", 42);
        assertEquals(expected, cellInner.map(Sheet.Cell::value).toList());

        s = new Sheet<>(Arrays.asList("r1"), Arrays.asList("c1", "c2"));
        cellInner = s.rowCells(0, 1).toList().get(0);
        s.set("r1", "c2", 42);
        assertEquals(expected, cellInner.map(Sheet.Cell::value).toList());

        // The already-documented column-axis behaviour, for the symmetry the javadoc now claims.
        s = new Sheet<>(Arrays.asList("r1", "r2"), Arrays.asList("c1"));
        inner = s.columnStreams().toList().get(0);
        s.set("r2", "c1", 42);
        assertEquals(expected, inner.toList());
    }

    // columnMajorStream

    @Test
    public void testColumnMajorStream_advanceAndCountFromEveryPosition() {
        final Sheet<String, String, Integer> s = numberedSheet(3, 4);

        assertAdvanceAndCount(() -> s.columnMajorStream(), expectedColumnMajor(s, 0, 4));
        assertAdvanceAndCount(() -> s.columnMajorStream(1, 3), expectedColumnMajor(s, 1, 3));
        assertAdvanceAndCount(() -> numberedSheet(1, 3).columnMajorStream(), Arrays.asList(0, 1, 2));
        assertEquals(expectedColumnMajor(s, 0, 4).subList(5, 12), s.columnMajorStream().skip(5).toList());
        assertEquals(expectedColumnMajor(s, 0, 4).subList(2, 9), s.columnMajorStream().skip(2).limit(7).toList());
    }

    @Test
    public void testColumnMajorStream_everyRangeOfEveryShape() {
        for (final int[] shape : new int[][] { { 1, 1 }, { 1, 5 }, { 5, 1 }, { 3, 4 }, { 4, 3 }, { 7, 2 } }) {
            final Sheet<String, String, Integer> s = numberedSheet(shape[0], shape[1]);

            for (int from = 0; from <= shape[1]; from++) {
                for (int to = from; to <= shape[1]; to++) {
                    assertEquals(expectedColumnMajor(s, from, to), s.columnMajorStream(from, to).toList(), shape[0] + "x" + shape[1] + " " + from + "-" + to);
                }
            }
        }
    }

    @Test
    public void testColumnMajorStream_firstWriteAfterPartialConsumption() {
        final Sheet<String, String, Integer> s = new Sheet<>(Arrays.asList("r0", "r1"), Arrays.asList("c0", "c1", "c2"));
        final ObjIteratorEx<Integer> iter = iteratorEx(s.columnMajorStream());

        assertNull(iter.next()); // r0,c0 - no storage yet
        iter.advance(1); // skips r1,c0
        assertNull(iter.next()); // r0,c1

        s.setAt(1, 1, 11);
        s.setAt(0, 2, 2);
        s.setAt(1, 2, 12);

        assertEquals(Integer.valueOf(11), iter.next()); // r1,c1 - first read with storage
        assertEquals(Integer.valueOf(2), iter.next()); // r0,c2
        s.setAt(1, 2, 120);
        assertEquals(Integer.valueOf(120), iter.next()); // r1,c2 - read live
        assertFalse(iter.hasNext());
    }

    @Test
    public void testColumnMajorStream_rangeFromLaterColumnWithFirstWriteMidTraversal() {
        // Starts past column 0, so the stepped position begins at (row 0, fromColumnIndex); storage appears only after
        // two cells of the first column in the range were returned as null.
        final Sheet<String, String, Integer> s = new Sheet<>(Arrays.asList("r0", "r1", "r2"), Arrays.asList("c0", "c1", "c2", "c3"));
        final ObjIteratorEx<Integer> iter = iteratorEx(s.columnMajorStream(1, 3));

        assertNull(iter.next()); // r0,c1
        assertNull(iter.next()); // r1,c1

        s.setAt(2, 1, 21);
        s.setAt(0, 2, 2);
        s.setAt(1, 2, 12);
        s.setAt(2, 2, 22);
        s.setAt(0, 3, 3); // outside the range

        assertEquals(Arrays.asList(21, 2, 12, 22), drain(iter));

        // The same range read again, fully initialized, and after an advance across the column boundary.
        assertEquals(Arrays.asList(null, null, 21, 2, 12, 22), s.columnMajorStream(1, 3).toList());
        final ObjIteratorEx<Integer> advanced = iteratorEx(s.columnMajorStream(1, 3));
        advanced.advance(4); // past r0..r2 of c1 and r0 of c2
        assertEquals(Arrays.asList(12, 22), drain(advanced));
    }

    // rowMajorStream

    @Test
    public void testRowMajorStream_advanceAndCountFromEveryPosition() {
        final Sheet<String, String, Integer> s = numberedSheet(4, 3);

        assertAdvanceAndCount(() -> s.rowMajorStream(), expectedRowMajor(s, 0, 4));
        assertAdvanceAndCount(() -> s.rowMajorStream(1, 3), expectedRowMajor(s, 1, 3));
        assertAdvanceAndCount(() -> numberedSheet(3, 1).rowMajorStream(), Arrays.asList(0, 10, 20));
        assertEquals(expectedRowMajor(s, 0, 4).subList(5, 12), s.rowMajorStream().skip(5).toList());
        assertEquals(expectedRowMajor(s, 0, 4).subList(2, 9), s.rowMajorStream().skip(2).limit(7).toList());
    }



    // rowMajorStream/columnMajorStream step a (row, column) pair along with the cursor and re-derive it only after
    // advance(..), count() or values returned while the Sheet had no storage. These pin every path of that bookkeeping.
    @Test
    public void testRowMajorStream_everyRangeOfEveryShape() {
        for (final int[] shape : new int[][] { { 1, 1 }, { 1, 5 }, { 5, 1 }, { 3, 4 }, { 4, 3 }, { 2, 7 } }) {
            final Sheet<String, String, Integer> s = numberedSheet(shape[0], shape[1]);

            for (int from = 0; from <= shape[0]; from++) {
                for (int to = from; to <= shape[0]; to++) {
                    assertEquals(expectedRowMajor(s, from, to), s.rowMajorStream(from, to).toList(), shape[0] + "x" + shape[1] + " " + from + "-" + to);
                }
            }
        }
    }

    @Test
    public void testRowMajorStream_firstWriteAfterPartialConsumption() {
        final Sheet<String, String, Integer> s = new Sheet<>(Arrays.asList("r0", "r1", "r2"), Arrays.asList("c0", "c1"));
        final ObjIteratorEx<Integer> iter = iteratorEx(s.rowMajorStream());

        assertNull(iter.next()); // r0,c0 - no storage yet
        iter.advance(1); // skips r0,c1
        assertNull(iter.next()); // r1,c0

        s.setAt(0, 0, 1);
        s.setAt(1, 1, 11);
        s.setAt(2, 0, 20);
        s.setAt(2, 1, 21);

        assertEquals(Integer.valueOf(11), iter.next()); // r1,c1 - first read with storage
        assertEquals(Integer.valueOf(20), iter.next()); // r2,c0
        s.setAt(2, 1, 210);
        assertEquals(Integer.valueOf(210), iter.next()); // r2,c1 - read live
        assertFalse(iter.hasNext());

        // A range that does not start at row 0.
        final Sheet<String, String, Integer> other = new Sheet<>(Arrays.asList("r0", "r1"), Arrays.asList("c0", "c1", "c2"));
        final ObjIteratorEx<Integer> iter2 = iteratorEx(other.rowMajorStream(1, 2));
        assertNull(iter2.next()); // r1,c0
        other.setAt(1, 2, 12);
        other.setAt(1, 1, 11);
        assertEquals(Arrays.asList(11, 12), drain(iter2));
    }

    @Test
    public void testStreamC() {
        Stream<Stream<Integer>> columnStreams = sheet.columnStreams();
        List<List<Integer>> result = columnStreams.map(Stream::toList).toList();
        assertEquals(3, result.size());
        assertEquals(3, result.get(0).size());
        assertEquals(Integer.valueOf(1), result.get(0).get(0));
    }

    @Test
    public void testStreamC_emptyRange() {
        assertTrue(objectSheet.columnStreams(1, 1).toList().isEmpty());
    }

    @Test
    public void testStreamC_InvalidRange() {
        assertThrows(IndexOutOfBoundsException.class, () -> sheet.columnStreams(-1, 2));
        assertThrows(IndexOutOfBoundsException.class, () -> sheet.columnStreams(0, 10));
    }

    @Test
    public void testStreamC_ValueOrder() {
        List<List<Integer>> cols = sheet.columnStreams().map(Stream::toList).toList();
        assertEquals(3, cols.size());
        assertEquals(Arrays.asList(1, 4, 7), cols.get(0));
        assertEquals(Arrays.asList(2, 5, 8), cols.get(1));
        assertEquals(Arrays.asList(3, 6, 9), cols.get(2));
    }

    @Test
    public void testStreamCWithRange() {
        Stream<Stream<Integer>> columnStreams = sheet.columnStreams(0, 2);
        List<List<Integer>> result = columnStreams.map(Stream::toList).toList();
        assertEquals(2, result.size());
    }

    @Test
    public void testStreamH() {
        Stream<Integer> stream = sheet.rowMajorStream();
        List<Integer> values = stream.toList();
        assertEquals(9, values.size());
        assertEquals(Integer.valueOf(1), values.get(0));
    }

    @Test
    public void testStreamH_emptyRange() {
        assertTrue(objectSheet.rowMajorStream(1, 1).toList().isEmpty());
    }

    @Test
    public void testStreamH_InvalidRange() {
        assertThrows(IndexOutOfBoundsException.class, () -> sheet.rowMajorStream(-1, 2));
        assertThrows(IndexOutOfBoundsException.class, () -> sheet.rowMajorStream(0, 10));
    }

    @Test
    public void testStreamH_ValueOrder() {
        List<Integer> values = sheet.rowMajorStream().toList();
        assertEquals(Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9), values);
    }

    @Test
    public void testStreamHWithRange() {
        Stream<Integer> stream = sheet.rowMajorStream(0, 2);
        List<Integer> values = stream.toList();
        assertEquals(6, values.size());
    }

    @Test
    public void testStreamHWithSingleRow() {
        Sheet<String, String, Integer> uninitSheet = new Sheet<>(upperRowKeys, colKeys);
        uninitSheet.set("R2", "C1", 21);
        uninitSheet.set("R2", "C2", 22);
        uninitSheet.set("R2", "C3", 23);

        List<Integer> values = uninitSheet.rowMajorStream(1, 2).toList();
        assertEquals(3, values.size());
        assertEquals(Integer.valueOf(21), values.get(0));
        assertEquals(Integer.valueOf(22), values.get(1));
        assertEquals(Integer.valueOf(23), values.get(2));
    }

    @Test
    public void testStreamOperationsOnEmptySheet() {
        assertEquals(0, emptySheet.rowMajorStream().count());
        assertEquals(0, emptySheet.columnMajorStream().count());
        assertEquals(0, emptySheet.rowMajorCells().count());
        assertEquals(0, emptySheet.rowMajorPoints().count());
    }

    @Test
    public void testStreamR() {
        Stream<Stream<Integer>> rowStreams = sheet.rowStreams();
        List<List<Integer>> result = rowStreams.map(Stream::toList).toList();
        assertEquals(3, result.size());
        assertEquals(3, result.get(0).size());
        assertEquals(Integer.valueOf(1), result.get(0).get(0));
    }

    @Test
    public void testStreamR_emptyRange() {
        assertTrue(objectSheet.rowStreams(1, 1).toList().isEmpty());
    }

    @Test
    public void testStreamR_InvalidRange() {
        assertThrows(IndexOutOfBoundsException.class, () -> sheet.rowStreams(-1, 2));
        assertThrows(IndexOutOfBoundsException.class, () -> sheet.rowStreams(0, 10));
    }

    @Test
    public void testStreamR_ValueOrder() {
        List<List<Integer>> rows = sheet.rowStreams().map(Stream::toList).toList();
        assertEquals(3, rows.size());
        assertEquals(Arrays.asList(1, 2, 3), rows.get(0));
        assertEquals(Arrays.asList(4, 5, 6), rows.get(1));
        assertEquals(Arrays.asList(7, 8, 9), rows.get(2));
    }

    @Test
    public void testStreamRWithRange() {
        Stream<Stream<Integer>> rowStreams = sheet.rowStreams(0, 2);
        List<List<Integer>> result = rowStreams.map(Stream::toList).toList();
        assertEquals(2, result.size());
    }

    @Test
    public void testStreamV() {
        Stream<Integer> stream = sheet.columnMajorStream();
        List<Integer> values = stream.toList();
        assertEquals(9, values.size());
        assertEquals(Integer.valueOf(1), values.get(0));
    }

    @Test
    public void testStreamV_emptyRange() {
        assertTrue(objectSheet.columnMajorStream(1, 1).toList().isEmpty());
    }

    @Test
    public void testStreamV_InvalidRange() {
        assertThrows(IndexOutOfBoundsException.class, () -> sheet.columnMajorStream(-1, 2));
        assertThrows(IndexOutOfBoundsException.class, () -> sheet.columnMajorStream(0, 10));
    }

    @Test
    public void testStreamV_ValueOrder() {
        List<Integer> values = sheet.columnMajorStream().toList();
        // column by column: col1(1,4,7), col2(2,5,8), col3(3,6,9)
        assertEquals(Arrays.asList(1, 4, 7, 2, 5, 8, 3, 6, 9), values);
    }

    @Test
    public void testStreamVWithInvalidRange() {
        assertThrows(IndexOutOfBoundsException.class, () -> sheet.columnMajorStream(-1, 2));
    }

    @Test
    public void testStreamVWithRange() {
        Stream<Integer> stream = sheet.columnMajorStream(0, 2);
        List<Integer> values = stream.toList();
        assertEquals(6, values.size());
    }

    @Test
    public void testStreamVWithSingleColumn() {
        Sheet<String, String, Integer> uninitSheet = new Sheet<>(upperRowKeys, colKeys);
        uninitSheet.set("R1", "C2", 12);
        uninitSheet.set("R2", "C2", 22);
        uninitSheet.set("R3", "C2", 32);

        List<Integer> values = uninitSheet.columnMajorStream(1, 2).toList();
        assertEquals(3, values.size());
        assertEquals(Integer.valueOf(12), values.get(0));
        assertEquals(Integer.valueOf(22), values.get(1));
        assertEquals(Integer.valueOf(32), values.get(2));
    }

    private static Sheet<String, String, Integer> numberedSheet(final int rowCount, final int columnCount) {
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

    private static List<Integer> expectedRowMajor(final Sheet<String, String, Integer> s, final int fromRowIndex, final int toRowIndex) {
        final List<Integer> expected = new ArrayList<>();

        for (int i = fromRowIndex; i < toRowIndex; i++) {
            for (int j = 0; j < s.columnCount(); j++) {
                expected.add(s.getAt(i, j));
            }
        }

        return expected;
    }

    private static List<Integer> expectedColumnMajor(final Sheet<String, String, Integer> s, final int fromColumnIndex, final int toColumnIndex) {
        final List<Integer> expected = new ArrayList<>();

        for (int j = fromColumnIndex; j < toColumnIndex; j++) {
            for (int i = 0; i < s.rowCount(); i++) {
                expected.add(s.getAt(i, j));
            }
        }

        return expected;
    }

    private static List<Integer> drain(final Iterator<Integer> iter) {
        final List<Integer> result = new ArrayList<>();

        while (iter.hasNext()) {
            result.add(iter.next());
        }

        return result;
    }

    private static ObjIteratorEx<Integer> iteratorEx(final Stream<Integer> stream) {
        return (ObjIteratorEx<Integer>) stream.iterator();
    }

    private static void assertAdvanceAndCount(final Supplier<Stream<Integer>> streamSupplier, final List<Integer> expected) {
        final long[] advances = { -1, 0, 1, 2, 3, 4, 5, 7, 11, 100, Long.MAX_VALUE };

        for (int consumed = 0; consumed <= expected.size(); consumed++) {
            for (final long n : advances) {
                final ObjIteratorEx<Integer> iter = iteratorEx(streamSupplier.get());

                for (int k = 0; k < consumed; k++) {
                    assertEquals(expected.get(k), iter.next());
                }

                iter.advance(n);

                final int skipped = (int) Math.min(expected.size() - consumed, Math.max(0, n));
                assertEquals(expected.subList(consumed + skipped, expected.size()), drain(iter), "consumed=" + consumed + ", n=" + n);
                assertFalse(iter.hasNext());
                assertThrows(NoSuchElementException.class, iter::next);
            }

            final ObjIteratorEx<Integer> iter = iteratorEx(streamSupplier.get());

            for (int k = 0; k < consumed; k++) {
                iter.next();
            }

            assertEquals(expected.size() - consumed, iter.count());
            assertFalse(iter.hasNext());
            assertThrows(NoSuchElementException.class, iter::next);
        }
    }

}

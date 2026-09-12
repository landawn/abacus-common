package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.util.stream.Stream;

public class SheetStreamTest extends SheetTestSupport {
    @Test
    public void testStreamOperationsOnEmptySheet() {
        assertEquals(0, emptySheet.rowMajorStream().count());
        assertEquals(0, emptySheet.columnMajorStream().count());
        assertEquals(0, emptySheet.rowMajorCells().count());
        assertEquals(0, emptySheet.rowMajorPoints().count());
    }

    @Test
    public void testStreamH_emptyRange() {
        assertTrue(objectSheet.rowMajorStream(1, 1).toList().isEmpty());
    }

    @Test
    public void testStreamV_emptyRange() {
        assertTrue(objectSheet.columnMajorStream(1, 1).toList().isEmpty());
    }

    @Test
    public void testStreamR_emptyRange() {
        assertTrue(objectSheet.rowStreams(1, 1).toList().isEmpty());
    }

    @Test
    public void testStreamC_emptyRange() {
        assertTrue(objectSheet.columnStreams(1, 1).toList().isEmpty());
    }

    @Test
    public void testStreamH() {
        Stream<Integer> stream = sheet.rowMajorStream();
        List<Integer> values = stream.toList();
        assertEquals(9, values.size());
        assertEquals(Integer.valueOf(1), values.get(0));
    }

    @Test
    public void testStreamHWithRange() {
        Stream<Integer> stream = sheet.rowMajorStream(0, 2);
        List<Integer> values = stream.toList();
        assertEquals(6, values.size());
    }

    @Test
    public void testStreamH_ValueOrder() {
        List<Integer> values = sheet.rowMajorStream().toList();
        assertEquals(Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9), values);
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
    public void testStreamH_InvalidRange() {
        assertThrows(IndexOutOfBoundsException.class, () -> sheet.rowMajorStream(-1, 2));
        assertThrows(IndexOutOfBoundsException.class, () -> sheet.rowMajorStream(0, 10));
    }

    @Test
    public void testStreamV() {
        Stream<Integer> stream = sheet.columnMajorStream();
        List<Integer> values = stream.toList();
        assertEquals(9, values.size());
        assertEquals(Integer.valueOf(1), values.get(0));
    }

    @Test
    public void testStreamVWithRange() {
        Stream<Integer> stream = sheet.columnMajorStream(0, 2);
        List<Integer> values = stream.toList();
        assertEquals(6, values.size());
    }

    @Test
    public void testStreamV_ValueOrder() {
        List<Integer> values = sheet.columnMajorStream().toList();
        // column by column: col1(1,4,7), col2(2,5,8), col3(3,6,9)
        assertEquals(Arrays.asList(1, 4, 7, 2, 5, 8, 3, 6, 9), values);
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

    @Test
    public void testStreamVWithInvalidRange() {
        assertThrows(IndexOutOfBoundsException.class, () -> sheet.columnMajorStream(-1, 2));
    }

    @Test
    public void testStreamV_InvalidRange() {
        assertThrows(IndexOutOfBoundsException.class, () -> sheet.columnMajorStream(-1, 2));
        assertThrows(IndexOutOfBoundsException.class, () -> sheet.columnMajorStream(0, 10));
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
    public void testStreamRWithRange() {
        Stream<Stream<Integer>> rowStreams = sheet.rowStreams(0, 2);
        List<List<Integer>> result = rowStreams.map(Stream::toList).toList();
        assertEquals(2, result.size());
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
    public void testStreamR_InvalidRange() {
        assertThrows(IndexOutOfBoundsException.class, () -> sheet.rowStreams(-1, 2));
        assertThrows(IndexOutOfBoundsException.class, () -> sheet.rowStreams(0, 10));
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
    public void testStreamCWithRange() {
        Stream<Stream<Integer>> columnStreams = sheet.columnStreams(0, 2);
        List<List<Integer>> result = columnStreams.map(Stream::toList).toList();
        assertEquals(2, result.size());
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
    public void testStreamC_InvalidRange() {
        assertThrows(IndexOutOfBoundsException.class, () -> sheet.columnStreams(-1, 2));
        assertThrows(IndexOutOfBoundsException.class, () -> sheet.columnStreams(0, 10));
    }

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
}

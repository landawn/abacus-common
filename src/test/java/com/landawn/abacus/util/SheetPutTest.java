package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Arrays;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.util.Sheet.Point;

public class SheetPutTest extends SheetTestSupport {
    @Test
    public void testPutOnFrozenSheet() {
        sheet.freeze();
        assertThrows(IllegalStateException.class, () -> {
            sheet.set("row1", "col1", 100);
        });
    }

    @Test
    public void testPutByKeys_frozenSheet() {
        objectSheet.freeze();
        assertThrows(IllegalStateException.class, () -> objectSheet.set("R1", "C1", "Val"));
    }

    @Test
    public void testPutInitializesUninitializedSheet() {
        Sheet<String, String, Integer> s = new Sheet<>(rowKeys, columnKeys);
        assertNull(s.set("row1", "col1", 42));
        assertEquals(Integer.valueOf(42), s.get("row1", "col1"));
    }

    @Test
    public void testPutAll() {
        Sheet<String, String, Integer> source = Sheet.rows(Arrays.asList("row1", "row2"), Arrays.asList("col1", "col2"),
                new Integer[][] { { 100, 200 }, { 300, 400 } });

        sheet.putAll(source);

        assertEquals(Integer.valueOf(100), sheet.get("row1", "col1"));
        assertEquals(Integer.valueOf(200), sheet.get("row1", "col2"));
        assertEquals(Integer.valueOf(300), sheet.get("row2", "col1"));
        assertEquals(Integer.valueOf(400), sheet.get("row2", "col2"));
        assertEquals(Integer.valueOf(7), sheet.get("row3", "col1"));
    }

    @Test
    public void testPutAllWithMergeFunction() {
        Sheet<String, String, Integer> source = Sheet.rows(Arrays.asList("row1", "row2"), Arrays.asList("col1", "col2"),
                new Integer[][] { { 10, 20 }, { 30, 40 } });

        sheet.putAll(source, (target, src) -> target + src);

        assertEquals(Integer.valueOf(11), sheet.get("row1", "col1"));
        assertEquals(Integer.valueOf(22), sheet.get("row1", "col2"));
        assertEquals(Integer.valueOf(34), sheet.get("row2", "col1"));
        assertEquals(Integer.valueOf(45), sheet.get("row2", "col2"));
    }

    @Test
    public void testPutAll_2() {
        Sheet<String, String, Object> sourceSheet = new Sheet<>(Arrays.asList("R1", "R2"), Arrays.asList("C1", "C2"));
        sourceSheet.set("R1", "C1", "SourceV11");
        sourceSheet.set("R2", "C2", "SourceV22");

        objectSheet.putAll(sourceSheet, (a, b) -> Nulls.firstNonNull(b, a));
        assertEquals("SourceV11", objectSheet.get("R1", "C1"));
        assertEquals("V12", objectSheet.get("R1", "C2"));
        assertEquals("SourceV22", objectSheet.get("R2", "C2"));
    }

    @Test
    public void testPutAllWithOverlap() {
        Sheet<String, String, Integer> uninitSheet = new Sheet<>(upperRowKeys, colKeys);
        uninitSheet.set("R1", "C1", 1);
        uninitSheet.set("R2", "C2", 2);

        Sheet<String, String, Integer> source = new Sheet<>(Arrays.asList("R1", "R2"), Arrays.asList("C1", "C2"));
        source.set("R1", "C1", 10);
        source.set("R1", "C2", 20);

        uninitSheet.putAll(source);

        assertEquals(Integer.valueOf(10), uninitSheet.get("R1", "C1"));
        assertEquals(Integer.valueOf(20), uninitSheet.get("R1", "C2"));
        assertNull(uninitSheet.get("R2", "C2"));
    }

    @Test
    public void testPutAllWithInvalidRowKeys() {
        Sheet<String, String, Integer> source = Sheet.rows(Arrays.asList("invalidRow"), Arrays.asList("col1"), new Integer[][] { { 100 } });

        assertThrows(IllegalArgumentException.class, () -> {
            sheet.putAll(source);
        });
    }

    @Test
    public void testPutAllWithInvalidColumnKeys() {
        Sheet<String, String, Integer> source = Sheet.rows(Arrays.asList("row1"), Arrays.asList("invalidCol"), new Integer[][] { { 100 } });

        assertThrows(IllegalArgumentException.class, () -> {
            sheet.putAll(source);
        });
    }

    @Test
    public void testPutAllOnFrozenSheet() {
        sheet.freeze();
        Sheet<String, String, Integer> source = Sheet.rows(Arrays.asList("row1"), Arrays.asList("col1"), new Integer[][] { { 100 } });

        assertThrows(IllegalStateException.class, () -> {
            sheet.putAll(source);
        });
    }

    @Test
    public void testPutAll_keyMismatch() {
        Sheet<String, String, Object> sourceSheetBadRow = new Sheet<>(Arrays.asList("R1", "RX"), Arrays.asList("C1", "C2"));
        sourceSheetBadRow.set("R1", "C1", "V");
        sourceSheetBadRow.set("RX", "C1", "V");
        assertThrows(IllegalArgumentException.class, () -> objectSheet.putAll(sourceSheetBadRow));

        Sheet<String, String, Object> sourceSheetBadCol = new Sheet<>(Arrays.asList("R1", "R2"), Arrays.asList("C1", "CX"));
        sourceSheetBadCol.set("R1", "C1", "V");
        sourceSheetBadCol.set("R1", "CX", "V");
        assertThrows(IllegalArgumentException.class, () -> objectSheet.putAll(sourceSheetBadCol));
    }

    @Test
    public void testPutAll_frozenSheet() {
        objectSheet.freeze();
        Sheet<String, String, Object> sourceSheet = new Sheet<>(Arrays.asList("R1"), Arrays.asList("C1"));
        sourceSheet.set("R1", "C1", "V");
        assertThrows(IllegalStateException.class, () -> objectSheet.putAll(sourceSheet));
    }

    @Test
    public void testPutAll_OverwritesMatchingCells() {
        Sheet<String, String, Integer> target = Sheet.rows(rowKeys, columnKeys, sampleData);
        Sheet<String, String, Integer> source = Sheet.rows(Arrays.asList("row1"), Arrays.asList("col1", "col3"), new Integer[][] { { 100, 300 } });
        target.putAll(source);
        assertEquals(Integer.valueOf(100), target.get("row1", "col1"));
        assertEquals(Integer.valueOf(2), target.get("row1", "col2"));
        assertEquals(Integer.valueOf(300), target.get("row1", "col3"));
        // row2/row3 untouched
        assertEquals(Integer.valueOf(4), target.get("row2", "col1"));
    }

    @Test
    public void testPutAll_RejectsUnknownKeys() {
        Sheet<String, String, Integer> target = Sheet.rows(rowKeys, columnKeys, sampleData);
        Sheet<String, String, Integer> source = Sheet.rows(Arrays.asList("rowZ"), Arrays.asList("col1"), new Integer[][] { { 9 } });
        assertThrows(IllegalArgumentException.class, () -> target.putAll(source));
    }

    @Test
    public void testPutByIndices_outOfBounds() {
        assertThrows(IndexOutOfBoundsException.class, () -> objectSheet.setAt(5, 0, "Val"));
    }

    @Test
    public void testPutWithKeys() {
        Integer oldValue = sheet.set("row1", "col1", 100);
        assertEquals(Integer.valueOf(1), oldValue);
        assertEquals(Integer.valueOf(100), sheet.get("row1", "col1"));
    }

    @Test
    public void testPutWithNullValue() {
        Integer oldValue = sheet.set("row1", "col1", null);
        assertEquals(Integer.valueOf(1), oldValue);
        assertNull(sheet.get("row1", "col1"));
    }

    @Test
    public void testPutWithInvalidKeys() {
        assertThrows(IllegalArgumentException.class, () -> {
            sheet.set("invalidRow", "col1", 100);
        });
    }

    @Test
    public void testPutWithIndices() {
        Integer oldValue = sheet.setAt(0, 0, 100);
        assertEquals(Integer.valueOf(1), oldValue);
        assertEquals(Integer.valueOf(100), sheet.getAt(0, 0));
    }

    @Test
    public void testPutWithInvalidIndices() {
        assertThrows(IndexOutOfBoundsException.class, () -> {
            sheet.setAt(-1, 0, 100);
        });
        assertThrows(IndexOutOfBoundsException.class, () -> {
            sheet.setAt(0, 10, 100);
        });
    }

    @Test
    public void testPutWithPoint() {
        Point point = Point.of(0, 0);
        Integer oldValue = sheet.set(point, 100);
        assertEquals(Integer.valueOf(1), oldValue);
        assertEquals(Integer.valueOf(100), sheet.get(point));
    }

    @Test
    public void testPutByKeys() {
        Object prev = objectSheet.set("R3", "C3", "V33");
        assertNull(prev);
        assertEquals("V33", objectSheet.get("R3", "C3"));

        Object prevUpdate = objectSheet.set("R1", "C1", "NewV11");
        assertEquals("V11", prevUpdate);
        assertEquals("NewV11", objectSheet.get("R1", "C1"));
    }

    @Test
    public void testPutByKeys_invalidKeys() {
        assertThrows(IllegalArgumentException.class, () -> objectSheet.set("RX", "C1", "Val"));
        assertThrows(IllegalArgumentException.class, () -> objectSheet.set("R1", "CX", "Val"));
    }

    @Test
    public void testPutByIndices() {
        Object prev = objectSheet.setAt(2, 2, "V33");
        assertNull(prev);
        assertEquals("V33", objectSheet.getAt(2, 2));

        Object prevUpdate = objectSheet.setAt(0, 0, "NewV11");
        assertEquals("V11", prevUpdate);
        assertEquals("NewV11", objectSheet.getAt(0, 0));
    }

    @Test
    public void testPutByPoint() {
        Object prev = objectSheet.set(Sheet.Point.of(2, 2), "V33");
        assertNull(prev);
        assertEquals("V33", objectSheet.getAt(2, 2));
    }

}

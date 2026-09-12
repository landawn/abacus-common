package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.util.Sheet.Point;

public class SheetRemoveTest extends SheetTestSupport {
    @Test
    public void testRemoveWithKeys() {
        Integer removed = sheet.remove("row1", "col1");
        assertEquals(Integer.valueOf(1), removed);
        assertNull(sheet.get("row1", "col1"));
    }

    @Test
    public void testRemoveWithIndices() {
        Integer removed = sheet.removeAt(0, 0);
        assertEquals(Integer.valueOf(1), removed);
        assertNull(sheet.getAt(0, 0));
    }

    @Test
    public void testRemoveWithPoint() {
        Point point = Point.of(0, 0);
        Integer removed = sheet.remove(point);
        assertEquals(Integer.valueOf(1), removed);
        assertNull(sheet.get(point));
    }

    @Test
    public void testRemoveUninitializedSheet() {
        Sheet<String, String, Integer> s = new Sheet<>(rowKeys, columnKeys);
        assertNull(s.remove("row1", "col1"));
    }

    @Test
    public void testRemoveByKeys() {
        Object removed = objectSheet.remove("R1", "C1");
        assertEquals("V11", removed);
        assertNull(objectSheet.get("R1", "C1"));
    }

    @Test
    public void testRemoveByKeys_nonExistentValueWasNull() {
        Object removed = objectSheet.remove("R1", "C3");
        assertNull(removed);
        assertNull(objectSheet.get("R1", "C3"));
    }

    @Test
    public void testRemoveByKeys_uninitializedSheet() {
        Sheet<String, String, String> uninitializedSheet = new Sheet<>(upperRowKeys, colKeys);
        assertNull(uninitializedSheet.remove("R1", "C1"));
    }

    @Test
    public void testRemoveByIndices() {
        Object removed = objectSheet.removeAt(0, 1);
        assertEquals("V12", removed);
        assertNull(objectSheet.getAt(0, 1));
    }

    @Test
    public void testRemoveByIndices_uninitializedSheet() {
        Sheet<String, String, String> uninitializedSheet = new Sheet<>(upperRowKeys, colKeys);
        assertNull(uninitializedSheet.removeAt(0, 0));
    }

    @Test
    public void testRemoveByPoint() {
        Object removed = objectSheet.remove(Sheet.Point.of(1, 0));
        assertEquals(100, removed);
        assertNull(objectSheet.getAt(1, 0));
    }

    @Test
    public void testRemove() {
        Sheet<String, String, Integer> uninitSheet = new Sheet<>(upperRowKeys, colKeys);
        uninitSheet.set("R1", "C1", 100);
        assertEquals(Integer.valueOf(100), uninitSheet.remove("R1", "C1"));
        assertNull(uninitSheet.get("R1", "C1"));
    }

    @Test
    public void testRemoveByIndex() {
        sheet.setAt(0, 0, 100);
        assertEquals(Integer.valueOf(100), sheet.removeAt(0, 0));
        assertNull(sheet.getAt(0, 0));
    }

    @Test
    public void testRemoveWithInvalidKeys() {
        assertThrows(IllegalArgumentException.class, () -> {
            sheet.remove("invalidRow", "col1");
        });
    }

    @Test
    public void testRemoveWithInvalidIndices() {
        assertThrows(IndexOutOfBoundsException.class, () -> {
            sheet.removeAt(-1, 0);
        });
        assertThrows(IndexOutOfBoundsException.class, () -> {
            sheet.removeAt(0, 10);
        });
    }

    @Test
    public void testRemoveOnFrozenSheet() {
        sheet.freeze();
        assertThrows(IllegalStateException.class, () -> {
            sheet.remove("row1", "col1");
        });
    }

    @Test
    public void testRemoveByKeys_invalidKeys() {
        assertThrows(IllegalArgumentException.class, () -> objectSheet.remove("RX", "C1"));
    }

    @Test
    public void testRemove_frozenSheet() {
        objectSheet.freeze();
        assertThrows(IllegalStateException.class, () -> objectSheet.remove("R1", "C1"));
        assertThrows(IllegalStateException.class, () -> objectSheet.removeAt(0, 0));
    }

    @Test
    public void testRemove_DoesNotShrinkSheet() {
        Sheet<String, String, Integer> s = Sheet.rows(rowKeys, columnKeys, sampleData);
        int rows = s.rowCount();
        int cols = s.columnCount();
        s.remove("row1", "col1");
        assertEquals(rows, s.rowCount());
        assertEquals(cols, s.columnCount());
        assertNull(s.get("row1", "col1"));
        assertTrue(s.containsCell("row1", "col1"));
    }

    @Test
    public void testRemoveRow_DataIntegrity() {
        sheet.removeRow("row2");
        assertEquals(2, sheet.rowCount());
        // Verify remaining rows maintain correct data
        assertEquals(Integer.valueOf(1), sheet.get("row1", "col1"));
        assertEquals(Integer.valueOf(7), sheet.get("row3", "col1"));
        assertEquals(Integer.valueOf(9), sheet.get("row3", "col3"));
    }

    @Test
    public void testRemoveRow_UninitializedSheet() {
        Sheet<String, String, Integer> uninitSheet = new Sheet<>(rowKeys, columnKeys);
        uninitSheet.removeRow("row2");
        assertEquals(2, uninitSheet.rowCount());
        assertFalse(uninitSheet.containsRow("row2"));
    }

    @Test
    public void testRemoveRow() {
        sheet.removeRow("row2");
        assertEquals(2, sheet.rowCount());
        assertFalse(sheet.containsRow("row2"));
        assertThrows(IllegalArgumentException.class, () -> {
            sheet.get("row2", "col1");
        });
    }

    @Test
    public void testRemoveRowInvalidKey() {
        assertThrows(IllegalArgumentException.class, () -> {
            sheet.removeRow("invalidRow");
        });
    }

    @Test
    public void testRemoveRowOnFrozenSheet() {
        sheet.freeze();
        assertThrows(IllegalStateException.class, () -> {
            sheet.removeRow("row1");
        });
    }

    @Test
    public void testRemoveRow_frozen() {
        objectSheet.freeze();
        assertThrows(IllegalStateException.class, () -> objectSheet.removeRow("R1"));
    }

    @Test
    public void testRemoveColumn_DataIntegrity() {
        sheet.removeColumn("col2");
        assertEquals(2, sheet.columnCount());
        assertEquals(Integer.valueOf(1), sheet.get("row1", "col1"));
        assertEquals(Integer.valueOf(3), sheet.get("row1", "col3"));
        assertEquals(Integer.valueOf(7), sheet.get("row3", "col1"));
    }

    @Test
    public void testRemoveColumn_UninitializedSheet() {
        Sheet<String, String, Integer> uninitSheet = new Sheet<>(rowKeys, columnKeys);
        uninitSheet.removeColumn("col2");
        assertEquals(2, uninitSheet.columnCount());
        assertFalse(uninitSheet.containsColumn("col2"));
    }

    @Test
    public void testRemoveColumn_New() {
        sheet.removeColumn("col2");
        assertEquals(2, sheet.columnCount());
        assertFalse(sheet.containsColumn("col2"));
        assertEquals(Integer.valueOf(1), sheet.get("row1", "col1"));
        assertEquals(Integer.valueOf(3), sheet.get("row1", "col3"));
    }

    @Test
    public void testRemoveColumn() {
        sheet.removeColumn("col2");
        assertEquals(2, sheet.columnCount());
        assertFalse(sheet.containsColumn("col2"));
        assertThrows(IllegalArgumentException.class, () -> {
            sheet.get("row1", "col2");
        });
    }

    @Test
    public void testRemoveColumnInvalidKey() {
        assertThrows(IllegalArgumentException.class, () -> {
            sheet.removeColumn("invalidCol");
        });
    }

    @Test
    public void testRemoveColumnOnFrozenSheet() {
        sheet.freeze();
        assertThrows(IllegalStateException.class, () -> {
            sheet.removeColumn("col1");
        });
    }

    @Test
    public void testRemoveColumn_frozen() {
        objectSheet.freeze();
        assertThrows(IllegalStateException.class, () -> objectSheet.removeColumn("C1"));
    }

}

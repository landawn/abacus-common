package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;

import org.junit.jupiter.api.Test;

public class SheetCopyTest extends SheetTestSupport {
    @Test
    public void testCopyWithSubset() {
        Sheet<String, String, Integer> uninitSheet = new Sheet<>(upperRowKeys, colKeys);
        uninitSheet.set("R1", "C1", 1);
        uninitSheet.set("R2", "C2", 2);
        uninitSheet.set("R3", "C3", 3);

        Sheet<String, String, Integer> copy = uninitSheet.copy(Arrays.asList("R1", "R2"), Arrays.asList("C1", "C2"));

        assertEquals(2, copy.rowCount());
        assertEquals(2, copy.columnCount());
        assertEquals(Integer.valueOf(1), copy.get("R1", "C1"));
        assertEquals(Integer.valueOf(2), copy.get("R2", "C2"));
        assertFalse(copy.containsRow("R3"));
        assertFalse(copy.containsColumn("C3"));
    }

    @Test
    public void testCopy() {
        Sheet<String, String, Integer> copy = sheet.copy();
        assertNotNull(copy);
        assertEquals(sheet.rowCount(), copy.rowCount());
        assertEquals(sheet.columnCount(), copy.columnCount());
        assertEquals(sheet.get("row1", "col1"), copy.get("row1", "col1"));

        copy.set("row1", "col1", 999);
        assertEquals(Integer.valueOf(1), sheet.get("row1", "col1"));
        assertEquals(Integer.valueOf(999), copy.get("row1", "col1"));
    }

    @Test
    public void testCopySubset() {
        Sheet<String, String, Integer> copy = sheet.copy(Arrays.asList("row1", "row2"), Arrays.asList("col1", "col2"));
        assertNotNull(copy);
        assertEquals(2, copy.rowCount());
        assertEquals(2, copy.columnCount());
        assertEquals(Integer.valueOf(1), copy.get("row1", "col1"));
        assertEquals(Integer.valueOf(5), copy.get("row2", "col2"));
        assertFalse(copy.containsRow("row3"));
        assertFalse(copy.containsColumn("col3"));
    }

    @Test
    public void testCopyEmptySheet() {
        Sheet<String, String, Integer> emptyCopy = emptySheet.copy();
        assertTrue(emptyCopy.isEmpty());
        assertFalse(emptyCopy.isFrozen());
    }

    @Test
    public void testCopy_UninitializedSheet() {
        Sheet<String, String, Integer> uninitSheet = new Sheet<>(rowKeys, columnKeys);
        Sheet<String, String, Integer> copy = uninitSheet.copy();
        assertEquals(3, copy.rowCount());
        assertEquals(3, copy.columnCount());
        assertNull(copy.get("row1", "col1"));
    }

    @Test
    public void testCopy_SubsetUninitializedSheet() {
        Sheet<String, String, Integer> uninitSheet = new Sheet<>(rowKeys, columnKeys);
        Sheet<String, String, Integer> copy = uninitSheet.copy(Arrays.asList("row1"), Arrays.asList("col1"));
        assertEquals(1, copy.rowCount());
        assertEquals(1, copy.columnCount());
        assertNull(copy.get("row1", "col1"));
    }

    @Test
    public void testCopySubset_EmptySubset() {
        Sheet<String, String, Integer> copy = sheet.copy(Arrays.asList("row1"), Arrays.asList("col1"));
        assertEquals(1, copy.rowCount());
        assertEquals(1, copy.columnCount());
        assertEquals(Integer.valueOf(1), copy.get("row1", "col1"));
    }

    @Test
    public void testCopy_withSpecificKeys() {
        List<String> subRowKeys = Arrays.asList("R1", "R2");
        List<String> subColKeys = Arrays.asList("C1", "C2");
        Sheet<String, String, Object> subCopy = objectSheet.copy(subRowKeys, subColKeys);

        assertEquals(2, subCopy.rowCount());
        assertEquals(2, subCopy.columnCount());
        assertTrue(new LinkedHashSet<>(subCopy.rowKeySet()).containsAll(subRowKeys));
        assertTrue(new LinkedHashSet<>(subCopy.columnKeySet()).containsAll(subColKeys));

        assertEquals("V11", subCopy.get("R1", "C1"));
        assertEquals("V12", subCopy.get("R1", "C2"));
        assertEquals(100, subCopy.get("R2", "C1"));
        assertNull(subCopy.get("R2", "C2"));

        assertThrows(IllegalArgumentException.class, () -> subCopy.get("R3", "C1"));
    }

    @Test
    public void testCopy_withSpecificKeys_invalidSubset() {
        assertThrows(IllegalArgumentException.class, () -> objectSheet.copy(Arrays.asList("R1", "RX"), colKeys));
        assertThrows(IllegalArgumentException.class, () -> objectSheet.copy(rowKeys, Arrays.asList("C1", "CX")));
    }

    @Test
    public void testCopyWithInvalidRowKeys() {
        assertThrows(IllegalArgumentException.class, () -> sheet.copy(Arrays.asList("R1", "InvalidRow"), columnKeys));
    }

    @Test
    public void testCopyWithInvalidColumnKeys() {
        assertThrows(IllegalArgumentException.class, () -> sheet.copy(rowKeys, Arrays.asList("C1", "InvalidColumn")));
    }

    @Test
    public void testCopy_StructuralIndependence() {
        Sheet<String, String, Integer> orig = Sheet.rows(rowKeys, columnKeys, sampleData);
        Sheet<String, String, Integer> copy = orig.copy();
        copy.set("row1", "col1", 999);
        assertEquals(Integer.valueOf(1), orig.get("row1", "col1"));
        assertEquals(Integer.valueOf(999), copy.get("row1", "col1"));
    }

    @Test
    public void testCopy_NullRowKeySet_ThrowsIAE() {
        assertThrows(IllegalArgumentException.class, () -> sheet.copy((List<String>) null, columnKeys));
    }

    @Test
    public void testCopy_NullColumnKeySet_ThrowsIAE() {
        assertThrows(IllegalArgumentException.class, () -> sheet.copy(rowKeys, (List<String>) null));
    }

    @Test
    public void testCopy_EmptyRowKeySetOnPopulatedSheet_ThrowsIAE() {
        assertThrows(IllegalArgumentException.class, () -> sheet.copy(Collections.<String> emptyList(), columnKeys));
    }

    @Test
    public void testCopy_EmptyColumnKeySetOnPopulatedSheet_ThrowsIAE() {
        assertThrows(IllegalArgumentException.class, () -> sheet.copy(rowKeys, Collections.<String> emptyList()));
    }

    @Test
    public void testCopy_ValidSubset_StillWorks() {
        // regression: a non-empty valid subset still works
        Sheet<String, String, Integer> sub = sheet.copy(Arrays.asList("row1"), Arrays.asList("col1", "col2"));
        assertEquals(1, sub.rowCount());
        assertEquals(2, sub.columnCount());
        assertEquals(Integer.valueOf(1), sub.get("row1", "col1"));
        assertEquals(Integer.valueOf(2), sub.get("row1", "col2"));
    }

    @Test
    public void testCopy_UnknownKey_StillThrowsIAE() {
        assertThrows(IllegalArgumentException.class, () -> sheet.copy(Arrays.asList("rowX"), columnKeys));
        assertThrows(IllegalArgumentException.class, () -> sheet.copy(rowKeys, Arrays.asList("colX")));
    }

    @Test
    public void testCopy_EmptyKeysOnEmptyAxis_Allowed() {
        // carve-out (matches Dataset): empty is accepted on the corresponding EMPTY axis.
        // zero-row Sheet: empty rowKeySet is the full (empty) row set.
        Sheet<String, String, Integer> zeroRow = new Sheet<>(Collections.<String> emptyList(), Arrays.asList("c1", "c2"));
        Sheet<String, String, Integer> sub1 = assertDoesNotThrow(() -> zeroRow.copy(Collections.<String> emptyList(), Arrays.asList("c1")));
        assertEquals(0, sub1.rowCount());
        assertEquals(1, sub1.columnCount());

        // zero-column Sheet: empty columnKeySet is the full (empty) column set.
        Sheet<String, String, Integer> zeroCol = new Sheet<>(Arrays.asList("r1", "r2"), Collections.<String> emptyList());
        Sheet<String, String, Integer> sub2 = assertDoesNotThrow(() -> zeroCol.copy(Arrays.asList("r1"), Collections.<String> emptyList()));
        assertEquals(1, sub2.rowCount());
        assertEquals(0, sub2.columnCount());

        // fully-empty Sheet: copy(empty, empty) is allowed (mirrors Dataset emptyDataset.copy(columnNameList()))
        Sheet<String, String, Integer> empty = new Sheet<>(Collections.<String> emptyList(), Collections.<String> emptyList());
        assertDoesNotThrow(() -> empty.copy(Collections.<String> emptyList(), Collections.<String> emptyList()));
    }

}

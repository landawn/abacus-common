package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.util.Sheet.Point;

public class SheetSetTest extends SheetTestSupport {
    @Test
    public void testSetWithKeys() {
        Integer old = sheet.set("row1", "col1", 100);
        assertEquals(Integer.valueOf(1), old);
        assertEquals(Integer.valueOf(100), sheet.get("row1", "col1"));
    }

    @Test
    public void testSetWithIndices() {
        Integer old = sheet.setAt(0, 0, 100);
        assertEquals(Integer.valueOf(1), old);
        assertEquals(Integer.valueOf(100), sheet.getAt(0, 0));
    }

    @Test
    public void testSetWithPoint() {
        Point p = Point.of(1, 2);
        Integer old = sheet.set(p, 100);
        assertEquals(Integer.valueOf(6), old);
        assertEquals(Integer.valueOf(100), sheet.get(p));
    }

    @Test
    public void testSetWithKeys_NullValue() {
        Integer old = sheet.set("row1", "col1", null);
        assertEquals(Integer.valueOf(1), old);
        assertNull(sheet.get("row1", "col1"));
    }

    @Test
    public void testSetWithKeys_InvalidKey() {
        assertThrows(IllegalArgumentException.class, () -> sheet.set("invalidRow", "col1", 1));
        assertThrows(IllegalArgumentException.class, () -> sheet.set("row1", "invalidCol", 1));
    }

    @Test
    public void testSetWithKeys_FrozenSheet() {
        sheet.freeze();
        assertThrows(IllegalStateException.class, () -> sheet.set("row1", "col1", 100));
    }

    @Test
    public void testSetWithIndices_InvalidIndex() {
        assertThrows(IndexOutOfBoundsException.class, () -> sheet.setAt(-1, 0, 1));
        assertThrows(IndexOutOfBoundsException.class, () -> sheet.setAt(0, -1, 1));
        assertThrows(IndexOutOfBoundsException.class, () -> sheet.setAt(10, 0, 1));
        assertThrows(IndexOutOfBoundsException.class, () -> sheet.setAt(0, 10, 1));
    }

    @Test
    public void testSetWithPoint_FrozenSheet() {
        sheet.freeze();
        assertThrows(IllegalStateException.class, () -> sheet.set(Point.of(0, 0), 100));
    }

    @Test
    public void testSet_NullValueAllowed() {
        Sheet<String, String, Integer> s = Sheet.rows(rowKeys, columnKeys, sampleData);
        Integer prev = s.set("row1", "col1", null);
        assertEquals(Integer.valueOf(1), prev);
        assertNull(s.get("row1", "col1"));
        assertTrue(s.isNull("row1", "col1"));
    }

    @Test
    public void testSetReturnsPreviousValueThroughBothCoordinateForms() {
        final Sheet<String, String, Integer> s = Sheet.rows(Arrays.asList("r1"), Arrays.asList("c1", "c2"), new Integer[][] { { 1, null } });

        assertEquals(Integer.valueOf(1), s.set("r1", "c1", 10));
        assertEquals(Integer.valueOf(10), s.get("r1", "c1"));
        assertNull(s.set("r1", "c2", 20));
        assertEquals(Integer.valueOf(20), s.get("r1", "c2"));

        assertEquals(Integer.valueOf(10), s.setAt(0, 0, 100));
        assertEquals(Integer.valueOf(100), s.getAt(0, 0));

        // Writing to an uninitialized Sheet must still allocate storage and report null as the old value.
        final Sheet<String, String, Integer> lazy = new Sheet<>(Arrays.asList("r1"), Arrays.asList("c1"));
        assertNull(lazy.set("r1", "c1", 7));
        assertEquals(Integer.valueOf(7), lazy.get("r1", "c1"));

        // Validation is unchanged by the shared setValue path.
        assertThrows(IllegalArgumentException.class, () -> s.set("nope", "c1", 1));
        assertThrows(IllegalArgumentException.class, () -> s.set("r1", "nope", 1));
        assertThrows(IndexOutOfBoundsException.class, () -> s.setAt(5, 0, 1));
        assertThrows(IndexOutOfBoundsException.class, () -> s.setAt(0, 5, 1));
    }

    @Test
    public void testSetRow() {
        sheet.setRow("row1", Arrays.asList(10, 20, 30));
        assertEquals(Integer.valueOf(10), sheet.get("row1", "col1"));
        assertEquals(Integer.valueOf(20), sheet.get("row1", "col2"));
        assertEquals(Integer.valueOf(30), sheet.get("row1", "col3"));
    }

    @Test
    public void testSetRow_UninitializedSheet() {
        Sheet<String, String, Integer> uninitSheet = new Sheet<>(rowKeys, columnKeys);
        uninitSheet.setRow("row1", Arrays.asList(10, 20, 30));
        assertEquals(Integer.valueOf(10), uninitSheet.get("row1", "col1"));
        assertEquals(Integer.valueOf(20), uninitSheet.get("row1", "col2"));
        assertEquals(Integer.valueOf(30), uninitSheet.get("row1", "col3"));
    }

    @Test
    public void testSetRowEmptyCollection() {
        sheet.setRow("row1", Arrays.asList());
        assertNull(sheet.get("row1", "col1"));
        assertNull(sheet.get("row1", "col2"));
        assertNull(sheet.get("row1", "col3"));
    }

    @Test
    public void testSetRow_emptyCollectionToSetNulls() {
        objectSheet.setRow("R1", Collections.emptyList());
        assertEquals(Arrays.asList(null, null, null), new ArrayList<>(objectSheet.rowValues("R1")));
    }

    @Test
    public void testSetRowSizeMismatch() {
        assertThrows(IllegalArgumentException.class, () -> {
            sheet.setRow("row1", Arrays.asList(10, 20));
        });
    }

    @Test
    public void testSetRowOnFrozenSheet() {
        sheet.freeze();
        assertThrows(IllegalStateException.class, () -> {
            sheet.setRow("row1", Arrays.asList(10, 20, 30));
        });
    }

    @Test
    public void testSetRow_sizeMismatch() {
        List<Object> newRowDataShort = Arrays.asList("New1", "New2");
        assertThrows(IllegalArgumentException.class, () -> objectSheet.setRow("R1", newRowDataShort));
    }

    @Test
    public void testSetRow_frozen() {
        objectSheet.freeze();
        assertThrows(IllegalStateException.class, () -> objectSheet.setRow("R1", Arrays.asList("a", "b", "c")));
    }

    @Test
    public void testSetRow_InvalidKey() {
        assertThrows(IllegalArgumentException.class, () -> sheet.setRow("invalidRow", Arrays.asList(1, 2, 3)));
    }

    @Test
    public void testSetColumn() {
        sheet.setColumn("col1", Arrays.asList(10, 20, 30));
        assertEquals(Integer.valueOf(10), sheet.get("row1", "col1"));
        assertEquals(Integer.valueOf(20), sheet.get("row2", "col1"));
        assertEquals(Integer.valueOf(30), sheet.get("row3", "col1"));
    }

    @Test
    public void testSetColumn_UninitializedSheet() {
        Sheet<String, String, Integer> uninitSheet = new Sheet<>(rowKeys, columnKeys);
        uninitSheet.setColumn("col1", Arrays.asList(10, 20, 30));
        assertEquals(Integer.valueOf(10), uninitSheet.get("row1", "col1"));
        assertEquals(Integer.valueOf(20), uninitSheet.get("row2", "col1"));
        assertEquals(Integer.valueOf(30), uninitSheet.get("row3", "col1"));
    }

    @Test
    public void testSetColumnEmptyCollection() {
        sheet.setColumn("col1", Arrays.asList());
        assertNull(sheet.get("row1", "col1"));
        assertNull(sheet.get("row2", "col1"));
        assertNull(sheet.get("row3", "col1"));
    }

    @Test
    public void testSetColumn_emptyCollectionToSetNulls() {
        objectSheet.setColumn("C1", Collections.emptyList());
        assertEquals(Arrays.asList(null, null, null), new ArrayList<>(objectSheet.columnValues("C1")));
    }

    @Test
    public void testSetColumnSizeMismatch() {
        assertThrows(IllegalArgumentException.class, () -> {
            sheet.setColumn("col1", Arrays.asList(10, 20));
        });
    }

    @Test
    public void testSetColumnOnFrozenSheet() {
        sheet.freeze();
        assertThrows(IllegalStateException.class, () -> {
            sheet.setColumn("col1", Arrays.asList(10, 20, 30));
        });
    }

    @Test
    public void testSetColumn_sizeMismatch() {
        List<Object> newColDataShort = Arrays.asList("New1", "New2");
        assertThrows(IllegalArgumentException.class, () -> objectSheet.setColumn("C1", newColDataShort));
    }

    @Test
    public void testSetColumn_frozen() {
        objectSheet.freeze();
        assertThrows(IllegalStateException.class, () -> objectSheet.setColumn("C1", Arrays.asList("a", "b", "c")));
    }

    @Test
    public void testSetColumn_InvalidKey() {
        assertThrows(IllegalArgumentException.class, () -> sheet.setColumn("invalidCol", Arrays.asList(1, 2, 3)));
    }

    @Test
    public void testSetColumn_KeepsLiveViewConsistent_FixedBug() {
        Sheet<String, String, Integer> s = Sheet.rows(rowKeys, columnKeys, sampleData);
        ImmutableList<Integer> view = s.columnValues("col1");
        assertEquals(Integer.valueOf(1), view.get(0));
        s.setColumn("col1", Arrays.asList(100, 200, 300));
        // After in-place setColumn, the previously obtained view should reflect new values.
        assertEquals(Integer.valueOf(100), view.get(0));
        assertEquals(Integer.valueOf(200), view.get(1));
        assertEquals(Integer.valueOf(300), view.get(2));
    }

    @Test
    public void testSetColumn_EmptyKeepsLiveViewConsistent() {
        Sheet<String, String, Integer> s = Sheet.rows(rowKeys, columnKeys, sampleData);
        ImmutableList<Integer> view = s.columnValues("col1");
        s.setColumn("col1", Collections.emptyList());
        assertNull(view.get(0));
        assertNull(view.get(1));
        assertNull(view.get(2));
    }

}

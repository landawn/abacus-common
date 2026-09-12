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

public class SheetAddTest extends SheetTestSupport {
    @Test
    public void testAddExistingRow() {
        Sheet<String, String, Integer> uninitSheet = new Sheet<>(upperRowKeys, colKeys);
        assertThrows(IllegalArgumentException.class, () -> uninitSheet.addRow("R1", Arrays.asList(1, 2, 3)));
    }

    @Test
    public void testAddExistingColumn() {
        Sheet<String, String, Integer> uninitSheet = new Sheet<>(upperRowKeys, colKeys);
        assertThrows(IllegalArgumentException.class, () -> uninitSheet.addColumn("C1", Arrays.asList(1, 2, 3)));
    }

    @Test
    public void testAddRow() {
        sheet.addRow("row4", Arrays.asList(10, 11, 12));
        assertEquals(4, sheet.rowCount());
        assertEquals(Integer.valueOf(10), sheet.get("row4", "col1"));
        assertEquals(Integer.valueOf(11), sheet.get("row4", "col2"));
        assertEquals(Integer.valueOf(12), sheet.get("row4", "col3"));
    }

    @Test
    public void testAddRowAtIndex() {
        sheet.addRow(1, "row1.5", Arrays.asList(10, 11, 12));
        assertEquals(4, sheet.rowCount());

        assertEquals(Integer.valueOf(10), sheet.get("row1.5", "col1"));

        List<String> expectedOrder = Arrays.asList("row1", "row1.5", "row2", "row3");
        List<String> actualOrder = new ArrayList<>(sheet.rowKeySet());
        assertEquals(expectedOrder, actualOrder);
    }

    @Test
    public void testAddRowAtIndexAtEnd() {
        sheet.addRow(3, "row4", Arrays.asList(10, 11, 12));
        assertEquals(4, sheet.rowCount());
        assertEquals(Integer.valueOf(10), sheet.get("row4", "col1"));
    }

    @Test
    public void testAddRow_atIndex() {
        List<Object> newRowData = Arrays.asList("VNew1", "VNew2", "VNew3");
        objectSheet.addRow(1, "RNew", newRowData);

        assertEquals(4, objectSheet.rowCount());
        assertTrue(objectSheet.containsRow("RNew"));
        assertEquals(Arrays.asList("R1", "RNew", "R2", "R3"), new ArrayList<>(objectSheet.rowKeySet()));
        assertEquals(newRowData, new ArrayList<>(objectSheet.rowValues("RNew")));
        assertEquals("V11", objectSheet.get("R1", "C1"));
        assertEquals(100, objectSheet.get("R2", "C1"));
    }

    @Test
    public void testAddRow_atIndex_end() {
        List<Object> newRowData = Arrays.asList("V41", "V42", "V43");
        objectSheet.addRow(3, "R4", newRowData);
        assertEquals(Arrays.asList("R1", "R2", "R3", "R4"), new ArrayList<>(objectSheet.rowKeySet()));
        assertEquals(newRowData, new ArrayList<>(objectSheet.rowValues("R4")));
    }

    @Test
    public void testAddRowAtBeginning() {
        Sheet<String, String, Integer> uninitSheet = new Sheet<>(upperRowKeys, colKeys);
        uninitSheet.set("R1", "C1", 1);
        uninitSheet.set("R2", "C1", 2);

        uninitSheet.addRow(0, "R0", Arrays.asList(0, 0, 0));

        assertEquals(4, uninitSheet.rowCount());
        assertEquals(Integer.valueOf(0), uninitSheet.get("R0", "C1"));
        assertEquals(Integer.valueOf(0), uninitSheet.getAt(0, 0));
        assertEquals(Integer.valueOf(1), uninitSheet.getAt(1, 0));
    }

    @Test
    public void testAddRowEmptyCollection() {
        sheet.addRow("row4", Arrays.asList());
        assertEquals(4, sheet.rowCount());
        assertNull(sheet.get("row4", "col1"));
        assertNull(sheet.get("row4", "col2"));
        assertNull(sheet.get("row4", "col3"));
    }

    @Test
    public void testAddRow_emptyData() {
        objectSheet.addRow("R4", Collections.emptyList());
        assertTrue(objectSheet.containsRow("R4"));
        assertEquals(4, objectSheet.rowCount());
        assertEquals(Arrays.asList(null, null, null), new ArrayList<>(objectSheet.rowValues("R4")));
    }

    @Test
    public void testAddRowDuplicateKey() {
        assertThrows(IllegalArgumentException.class, () -> {
            sheet.addRow("row1", Arrays.asList(10, 11, 12));
        });
    }

    @Test
    public void testAddRowSizeMismatch() {
        assertThrows(IllegalArgumentException.class, () -> {
            sheet.addRow("row4", Arrays.asList(10, 11));
        });
    }

    @Test
    public void testAddRowAtIndexInvalid() {
        assertThrows(IndexOutOfBoundsException.class, () -> {
            sheet.addRow(-1, "row0", Arrays.asList(10, 11, 12));
        });
        assertThrows(IndexOutOfBoundsException.class, () -> {
            sheet.addRow(10, "row10", Arrays.asList(10, 11, 12));
        });
    }

    @Test
    public void testAddRow_frozen() {
        objectSheet.freeze();
        assertThrows(IllegalStateException.class, () -> objectSheet.addRow("R4", Arrays.asList("a", "b", "c")));
    }

    @Test
    public void testAddRow_atIndex_outOfBounds() {
        assertThrows(IndexOutOfBoundsException.class, () -> objectSheet.addRow(4, "R5", Arrays.asList("a", "b", "c")));
        assertThrows(IndexOutOfBoundsException.class, () -> objectSheet.addRow(-1, "R0", Arrays.asList("a", "b", "c")));
    }

    @Test
    public void testAddRow_OnFrozenSheetAtIndex() {
        sheet.freeze();
        assertThrows(IllegalStateException.class, () -> sheet.addRow(0, "newRow", Arrays.asList(1, 2, 3)));
    }

    @Test
    public void testAddRow_DuplicateKeyAtIndex() {
        assertThrows(IllegalArgumentException.class, () -> sheet.addRow(0, "row1", Arrays.asList(1, 2, 3)));
    }

    @Test
    public void testAddRow_SizeMismatchAtIndex() {
        assertThrows(IllegalArgumentException.class, () -> sheet.addRow(0, "newRow", Arrays.asList(1, 2)));
    }

    @Test
    public void testAddRowAfterMoveRow_indexMapResetRegression() {
        // moveRow() sets _rowKeyIndexMap = null while the sheet stays initialized.
        // Before the fix, addRow() relied on init() (a no-op when already initialized)
        // and threw NullPointerException because the index map was still null.
        Sheet<String, String, Integer> s = Sheet.rows(Arrays.asList("r1", "r2"), Arrays.asList("c1"), new Integer[][] { { 1 }, { 2 } });

        s.moveRow("r1", 1);
        assertEquals(Arrays.asList("r2", "r1"), new ArrayList<>(s.rowKeySet()));

        s.addRow("r3", Arrays.asList(3));

        assertEquals(Arrays.asList("r2", "r1", "r3"), new ArrayList<>(s.rowKeySet()));
        assertEquals(Integer.valueOf(2), s.get("r2", "c1"));
        assertEquals(Integer.valueOf(1), s.get("r1", "c1"));
        assertEquals(Integer.valueOf(3), s.get("r3", "c1"));
        assertEquals(Integer.valueOf(3), s.getAt(2, 0));
    }

    @Test
    public void testAddRowAtIndexAfterMoveRow_indexMapResetRegression() {
        Sheet<String, String, Integer> s = Sheet.rows(Arrays.asList("r1", "r2", "r3"), Arrays.asList("c1"), new Integer[][] { { 1 }, { 2 }, { 3 } });

        s.moveRow("r3", 0); // resets _rowKeyIndexMap to null; order -> r3, r1, r2

        s.addRow(1, "rX", Arrays.asList(99)); // before fix: NPE at _rowKeyIndexMap.size()

        assertEquals(Arrays.asList("r3", "rX", "r1", "r2"), new ArrayList<>(s.rowKeySet()));
        assertEquals(Integer.valueOf(3), s.get("r3", "c1"));
        assertEquals(Integer.valueOf(99), s.get("rX", "c1"));
        assertEquals(Integer.valueOf(1), s.get("r1", "c1"));
        assertEquals(Integer.valueOf(2), s.get("r2", "c1"));
    }

    @Test
    public void testAddColumn() {
        sheet.addColumn("col4", Arrays.asList(10, 11, 12));
        assertEquals(4, sheet.columnCount());
        assertEquals(Integer.valueOf(10), sheet.get("row1", "col4"));
        assertEquals(Integer.valueOf(11), sheet.get("row2", "col4"));
        assertEquals(Integer.valueOf(12), sheet.get("row3", "col4"));
    }

    @Test
    public void testAddColumnAtIndex() {
        sheet.addColumn(1, "col1.5", Arrays.asList(10, 11, 12));
        assertEquals(4, sheet.columnCount());

        assertEquals(Integer.valueOf(10), sheet.get("row1", "col1.5"));

        List<String> expectedOrder = Arrays.asList("col1", "col1.5", "col2", "col3");
        List<String> actualOrder = new ArrayList<>(sheet.columnKeySet());
        assertEquals(expectedOrder, actualOrder);
    }

    @Test
    public void testAddColumnAtIndexAtEnd() {
        sheet.addColumn(3, "col4", Arrays.asList(10, 11, 12));
        assertEquals(4, sheet.columnCount());
        assertEquals(Integer.valueOf(10), sheet.get("row1", "col4"));
    }

    @Test
    public void testAddColumn_atIndex() {
        List<Object> newColData = Arrays.asList("NR1New", "NR2New", "NR3New");
        objectSheet.addColumn(1, "CNew", newColData);

        assertEquals(4, objectSheet.columnCount());
        assertTrue(objectSheet.containsColumn("CNew"));
        assertEquals(Arrays.asList("C1", "CNew", "C2", "C3"), new ArrayList<>(objectSheet.columnKeySet()));
        assertEquals(newColData, new ArrayList<>(objectSheet.columnValues("CNew")));
        assertEquals("V11", objectSheet.get("R1", "C1"));
        assertEquals("V12", objectSheet.get("R1", "C2"));
    }

    @Test
    public void testAddColumn_atIndex_end() {
        List<Object> newColData = Arrays.asList("V14", "V24", "V34");
        objectSheet.addColumn(3, "C4", newColData);
        assertEquals(Arrays.asList("C1", "C2", "C3", "C4"), new ArrayList<>(objectSheet.columnKeySet()));
        assertEquals(newColData, new ArrayList<>(objectSheet.columnValues("C4")));
    }

    @Test
    public void testAddColumnAtEnd() {
        Sheet<String, String, Integer> uninitSheet = new Sheet<>(upperRowKeys, colKeys);
        uninitSheet.set("R1", "C1", 1);

        uninitSheet.addColumn(uninitSheet.columnCount(), "C4", Arrays.asList(4, 5, 6));

        assertEquals(4, uninitSheet.columnCount());
        assertEquals(Integer.valueOf(4), uninitSheet.get("R1", "C4"));
    }

    @Test
    public void testAddColumnEmptyCollection() {
        sheet.addColumn("col4", Arrays.asList());
        assertEquals(4, sheet.columnCount());
        assertNull(sheet.get("row1", "col4"));
        assertNull(sheet.get("row2", "col4"));
        assertNull(sheet.get("row3", "col4"));
    }

    @Test
    public void testAddColumn_emptyData() {
        objectSheet.addColumn("C4", Collections.emptyList());
        assertTrue(objectSheet.containsColumn("C4"));
        assertEquals(4, objectSheet.columnCount());
        assertEquals(Arrays.asList(null, null, null), new ArrayList<>(objectSheet.columnValues("C4")));
    }

    @Test
    public void testAddColumnDuplicateKey() {
        assertThrows(IllegalArgumentException.class, () -> {
            sheet.addColumn("col1", Arrays.asList(10, 11, 12));
        });
    }

    @Test
    public void testAddColumnSizeMismatch() {
        assertThrows(IllegalArgumentException.class, () -> {
            sheet.addColumn("col4", Arrays.asList(10, 11));
        });
    }

    @Test
    public void testAddColumnAtIndexInvalid() {
        assertThrows(IndexOutOfBoundsException.class, () -> {
            sheet.addColumn(-1, "col0", Arrays.asList(10, 11, 12));
        });
        assertThrows(IndexOutOfBoundsException.class, () -> {
            sheet.addColumn(10, "col10", Arrays.asList(10, 11, 12));
        });
    }

    @Test
    public void testAddColumn_frozen() {
        objectSheet.freeze();
        assertThrows(IllegalStateException.class, () -> objectSheet.addColumn("C4", Arrays.asList("a", "b", "c")));
    }

    @Test
    public void testAddColumn_atIndex_outOfBounds() {
        assertThrows(IndexOutOfBoundsException.class, () -> objectSheet.addColumn(4, "C5", Arrays.asList("a", "b", "c")));
        assertThrows(IndexOutOfBoundsException.class, () -> objectSheet.addColumn(-1, "C0", Arrays.asList("a", "b", "c")));
    }

    @Test
    public void testAddColumn_OnFrozenSheetAtIndex() {
        sheet.freeze();
        assertThrows(IllegalStateException.class, () -> sheet.addColumn(0, "newCol", Arrays.asList(1, 2, 3)));
    }

    @Test
    public void testAddColumn_DuplicateKeyAtIndex() {
        assertThrows(IllegalArgumentException.class, () -> sheet.addColumn(0, "col1", Arrays.asList(1, 2, 3)));
    }

    @Test
    public void testAddColumn_SizeMismatchAtIndex() {
        assertThrows(IllegalArgumentException.class, () -> sheet.addColumn(0, "newCol", Arrays.asList(1, 2)));
    }

    @Test
    public void testAddColumnAfterMoveColumn_indexMapResetRegression() {
        // moveColumn() sets _columnKeyIndexMap = null while the sheet stays initialized.
        // Before the fix, addColumn() threw NullPointerException for the same reason.
        Sheet<String, String, Integer> s = Sheet.rows(Arrays.asList("r1"), Arrays.asList("c1", "c2"), new Integer[][] { { 10, 20 } });

        s.moveColumn("c1", 1);
        assertEquals(Arrays.asList("c2", "c1"), new ArrayList<>(s.columnKeySet()));

        s.addColumn("c3", Arrays.asList(30));

        assertEquals(Arrays.asList("c2", "c1", "c3"), new ArrayList<>(s.columnKeySet()));
        assertEquals(Integer.valueOf(20), s.get("r1", "c2"));
        assertEquals(Integer.valueOf(10), s.get("r1", "c1"));
        assertEquals(Integer.valueOf(30), s.get("r1", "c3"));
        assertEquals(Integer.valueOf(30), s.getAt(0, 2));
    }

    @Test
    public void testDuplicateKeyMessageRendersArrayKeys() {
        // Array keys are a documented first-class key type, but the duplicate diagnostics concatenated the key
        // with '+' and printed "[I@c65a5ef" - the sibling renameRow/renameColumn messages already used N.toString.
        final Sheet<int[], String, Integer> rowKeyed = Sheet.rows(Arrays.asList(new int[] { 1, 2 }), Arrays.asList("c1"), new Integer[][] { { 1 } });
        assertEquals("Row '[1, 2]' already exists",
                assertThrows(IllegalArgumentException.class, () -> rowKeyed.addRow(new int[] { 1, 2 }, null)).getMessage());
        assertEquals("Row '[1, 2]' already exists",
                assertThrows(IllegalArgumentException.class, () -> rowKeyed.addRow(0, new int[] { 1, 2 }, null)).getMessage());

        final Sheet<String, int[], Integer> columnKeyed = Sheet.rows(Arrays.asList("r1"), Arrays.asList(new int[] { 7, 8 }), new Integer[][] { { 1 } });
        assertEquals("Column '[7, 8]' already exists",
                assertThrows(IllegalArgumentException.class, () -> columnKeyed.addColumn(new int[] { 7, 8 }, null)).getMessage());
        assertEquals("Column '[7, 8]' already exists",
                assertThrows(IllegalArgumentException.class, () -> columnKeyed.addColumn(0, new int[] { 7, 8 }, null)).getMessage());

        // A plain key still renders exactly as it did.
        assertEquals("Row 'row1' already exists",
                assertThrows(IllegalArgumentException.class, () -> sheet.addRow("row1", Arrays.asList(1, 2, 3))).getMessage());
        assertEquals("Column 'col1' already exists",
                assertThrows(IllegalArgumentException.class, () -> sheet.addColumn("col1", Arrays.asList(1, 2, 3))).getMessage());
    }

    @Test
    public void testPutAllForeignKeyMessageRendersArrayKeys() {
        // Companion to testDuplicateKeyMessageRendersArrayKeys: putAll names whole key *sets* in its message and
        // concatenated them straight in, so an array key came out as "[[I@67389cb8]".
        final int[] rowKey = { 1, 2 };
        final int[] columnKey = { 7, 8 };
        final int[] absent = { 9, 9 };
        final Sheet<int[], int[], Integer> target = Sheet.rows(Arrays.asList(rowKey), Arrays.asList(columnKey), new Integer[][] { { 1 } });

        final Sheet<int[], int[], Integer> foreignRow = Sheet.rows(Arrays.asList(absent), Arrays.asList(columnKey), new Integer[][] { { 2 } });
        assertEquals("[[9, 9]] are not all included in this sheet with row key set: [[1, 2]]",
                assertThrows(IllegalArgumentException.class, () -> target.putAll(foreignRow)).getMessage());
        assertEquals("[[9, 9]] are not all included in this sheet with row key set: [[1, 2]]",
                assertThrows(IllegalArgumentException.class, () -> target.putAll(foreignRow, (a, b) -> a)).getMessage());

        final Sheet<int[], int[], Integer> foreignColumn = Sheet.rows(Arrays.asList(rowKey), Arrays.asList(absent), new Integer[][] { { 2 } });
        assertEquals("[[9, 9]] are not all included in this sheet with column key set: [[7, 8]]",
                assertThrows(IllegalArgumentException.class, () -> target.putAll(foreignColumn)).getMessage());
        assertEquals("[[9, 9]] are not all included in this sheet with column key set: [[7, 8]]",
                assertThrows(IllegalArgumentException.class, () -> target.putAll(foreignColumn, (a, b) -> a)).getMessage());

        // A plain key set still renders exactly as it did.
        final Sheet<String, String, Integer> foreignPlain = Sheet.rows(Arrays.asList("nope"), columnKeys, new Integer[][] { { 1, 2, 3 } });
        assertEquals("[nope] are not all included in this sheet with row key set: [row1, row2, row3]",
                assertThrows(IllegalArgumentException.class, () -> sheet.putAll(foreignPlain)).getMessage());
    }
}

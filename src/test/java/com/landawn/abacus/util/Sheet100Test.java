package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.If.OrElse;
import com.landawn.abacus.util.Sheet.Cell;
import com.landawn.abacus.util.Sheet.Point;
import com.landawn.abacus.util.stream.Stream;

public class Sheet100Test extends TestBase {

    private Sheet<String, String, Integer> sheet;
    private List<String> rowKeys;
    private List<String> columnKeys;

    @BeforeEach
    public void setUp() {
        rowKeys = Arrays.asList("R1", "R2", "R3");
        columnKeys = Arrays.asList("C1", "C2", "C3");
        sheet = new Sheet<>(rowKeys, columnKeys);
    }

    // Constructor tests
    @Test
    public void testDefaultConstructor() {
        Sheet<String, String, Integer> emptySheet = new Sheet<>();
        assertTrue(emptySheet.isEmpty());
        assertEquals(0, emptySheet.rowLength());
        assertEquals(0, emptySheet.columnLength());
    }

    @Test
    public void testConstructorWithKeySets() {
        assertEquals(3, sheet.rowLength());
        assertEquals(3, sheet.columnLength());
        assertTrue(sheet.rowKeySet().containsAll(rowKeys));
        assertTrue(sheet.columnKeySet().containsAll(columnKeys));
    }

    @Test
    public void testConstructorWithData() {
        Object[][] data = { { 1, 2, 3 }, { 4, 5, 6 }, { 7, 8, 9 } };
        Sheet<String, String, Integer> dataSheet = new Sheet<>(rowKeys, columnKeys, data);
        assertEquals(1, dataSheet.get("R1", "C1"));
        assertEquals(5, dataSheet.get("R2", "C2"));
        assertEquals(9, dataSheet.get("R3", "C3"));
    }

    @Test
    public void testConstructorWithNullRowKey() {
        List<String> invalidRowKeys = Arrays.asList("R1", null, "R3");
        assertThrows(IllegalArgumentException.class, () -> new Sheet<>(invalidRowKeys, columnKeys));
    }

    @Test
    public void testConstructorWithNullColumnKey() {
        List<String> invalidColumnKeys = Arrays.asList("C1", null, "C3");
        assertThrows(IllegalArgumentException.class, () -> new Sheet<>(rowKeys, invalidColumnKeys));
    }

    // Static factory method tests
    @Test
    public void testEmpty() {
        Sheet<String, String, Integer> emptySheet = Sheet.empty();
        assertTrue(emptySheet.isEmpty());
        assertTrue(emptySheet.isFrozen());
    }

    @Test
    public void testRows() {
        {
            Object[][] data = { { 1, 2, 3 }, { 4, 5, 6 }, { 7, 8, 9 } };
            Sheet<String, String, Integer> sheetLocal = Sheet.rows(rowKeys, columnKeys, data);
            assertEquals(5, sheetLocal.get("R2", "C2"));
        }
        {

            sheet.put("R1", "C1", 1);
            sheet.put("R1", "C2", 2);

            List<Pair<String, Stream<Integer>>> rows = sheet.rows()
                    .map(pair -> Pair.of(pair.left(), pair.right().toList()))
                    .map(pair -> Pair.of(pair.left(), Stream.of(pair.right())))
                    .toList();

            assertEquals(3, rows.size());
            assertEquals("R1", rows.get(0).left());
            List<Integer> firstRow = rows.get(0).right().toList();
            assertEquals(Integer.valueOf(1), firstRow.get(0));
            assertEquals(Integer.valueOf(2), firstRow.get(1));

        }
    }

    @Test
    public void testRowsWithCollection() {
        List<List<Integer>> rows = Arrays.asList(Arrays.asList(1, 2, 3), Arrays.asList(4, 5, 6), Arrays.asList(7, 8, 9));
        Sheet<String, String, Integer> sheetLocal = Sheet.rows(rowKeys, columnKeys, rows);
        assertEquals(5, sheetLocal.get("R2", "C2"));
    }

    @Test
    public void testColumns() {

        {
            Object[][] columns = { { 1, 4, 7 }, { 2, 5, 8 }, { 3, 6, 9 } };
            Sheet<String, String, Integer> sheetLocal = Sheet.columns(rowKeys, columnKeys, columns);
            assertEquals(5, sheetLocal.get("R2", "C2"));
        }
        {

            sheet.put("R1", "C1", 1);
            sheet.put("R2", "C1", 2);

            List<Pair<String, Stream<Integer>>> columns = sheet.columns()
                    .map(pair -> Pair.of(pair.left(), pair.right().toList()))
                    .map(pair -> Pair.of(pair.left(), Stream.of(pair.right())))
                    .toList();

            assertEquals(3, columns.size());
            assertEquals("C1", columns.get(0).left());
            List<Integer> firstColumn = columns.get(0).right().toList();
            assertEquals(Integer.valueOf(1), firstColumn.get(0));
            assertEquals(Integer.valueOf(2), firstColumn.get(1));

        }
    }

    @Test
    public void testColumnsWithCollection() {
        List<List<Integer>> columns = Arrays.asList(Arrays.asList(1, 4, 7), Arrays.asList(2, 5, 8), Arrays.asList(3, 6, 9));
        Sheet<String, String, Integer> sheetLocal = Sheet.columns(rowKeys, columnKeys, columns);
        assertEquals(5, sheetLocal.get("R2", "C2"));
    }

    // Get and Put tests
    @Test
    public void testGetPut() {
        assertNull(sheet.get("R1", "C1"));
        sheet.put("R1", "C1", 100);
        assertEquals(Integer.valueOf(100), sheet.get("R1", "C1"));
    }

    @Test
    public void testGetPutByIndex() {
        assertNull(sheet.get(0, 0));
        sheet.put(0, 0, 100);
        assertEquals(Integer.valueOf(100), sheet.get(0, 0));
    }

    @Test
    public void testGetPutByPoint() {
        Point point = Point.of(1, 1);
        assertNull(sheet.get(point));
        sheet.put(point, 200);
        assertEquals(Integer.valueOf(200), sheet.get(point));
    }

    @Test
    public void testGetWithInvalidRowKey() {
        assertThrows(IllegalArgumentException.class, () -> sheet.get("InvalidRow", "C1"));
    }

    @Test
    public void testGetWithInvalidColumnKey() {
        assertThrows(IllegalArgumentException.class, () -> sheet.get("R1", "InvalidColumn"));
    }

    @Test
    public void testGetWithInvalidRowIndex() {
        assertThrows(IndexOutOfBoundsException.class, () -> sheet.get(10, 0));
    }

    @Test
    public void testGetWithInvalidColumnIndex() {
        assertThrows(IndexOutOfBoundsException.class, () -> sheet.get(0, 10));
    }

    // PutAll test
    @Test
    public void testPutAll() {
        Sheet<String, String, Integer> source = new Sheet<>(Arrays.asList("R1", "R2"), Arrays.asList("C1", "C2"));
        source.put("R1", "C1", 10);
        source.put("R2", "C2", 20);

        sheet.putAll(source);
        assertEquals(Integer.valueOf(10), sheet.get("R1", "C1"));
        assertEquals(Integer.valueOf(20), sheet.get("R2", "C2"));
    }

    // Remove tests
    @Test
    public void testRemove() {
        sheet.put("R1", "C1", 100);
        assertEquals(Integer.valueOf(100), sheet.remove("R1", "C1"));
        assertNull(sheet.get("R1", "C1"));
    }

    @Test
    public void testRemoveByIndex() {
        sheet.put(0, 0, 100);
        assertEquals(Integer.valueOf(100), sheet.remove(0, 0));
        assertNull(sheet.get(0, 0));
    }

    @Test
    public void testRemoveByPoint() {
        Point point = Point.of(1, 1);
        sheet.put(point, 200);
        assertEquals(Integer.valueOf(200), sheet.remove(point));
        assertNull(sheet.get(point));
    }

    // Contains tests
    @Test
    public void testContains() {
        assertTrue(sheet.contains("R1", "C1"));
        assertFalse(sheet.contains("R1", "InvalidColumn"));
        assertFalse(sheet.contains("InvalidRow", "C1"));
    }

    @Test
    public void testContainsValue() {
        assertFalse(sheet.containsValue(100));
        sheet.put("R1", "C1", 100);
        assertTrue(sheet.containsValue(100));
        assertTrue(sheet.containsValue(null));
    }

    @Test
    public void testContainsWithValue() {
        sheet.put("R1", "C1", 100);
        assertTrue(sheet.contains("R1", "C1", 100));
        assertFalse(sheet.contains("R1", "C1", 200));
    }

    // Row operations tests
    @Test
    public void testGetRow() {
        sheet.put("R1", "C1", 1);
        sheet.put("R1", "C2", 2);
        sheet.put("R1", "C3", 3);

        List<Integer> row = sheet.getRow("R1");
        assertEquals(Arrays.asList(1, 2, 3), row);
    }

    @Test
    public void testSetRow() {
        List<Integer> newRow = Arrays.asList(10, 20, 30);
        sheet.setRow("R1", newRow);

        assertEquals(Integer.valueOf(10), sheet.get("R1", "C1"));
        assertEquals(Integer.valueOf(20), sheet.get("R1", "C2"));
        assertEquals(Integer.valueOf(30), sheet.get("R1", "C3"));
    }

    @Test
    public void testAddRow() {
        sheet.addRow("R4", Arrays.asList(1, 2, 3));
        assertEquals(4, sheet.rowLength());
        assertEquals(Integer.valueOf(2), sheet.get("R4", "C2"));
    }

    @Test
    public void testAddRowAtIndex() {
        sheet.addRow(1, "R_NEW", Arrays.asList(10, 20, 30));
        assertEquals(4, sheet.rowLength());
        assertEquals(Integer.valueOf(20), sheet.get("R_NEW", "C2"));
    }

    @Test
    public void testAddExistingRow() {
        assertThrows(IllegalArgumentException.class, () -> sheet.addRow("R1", Arrays.asList(1, 2, 3)));
    }

    @Test
    public void testUpdateRow() {
        sheet.put("R1", "C1", 1);
        sheet.put("R1", "C2", 2);
        sheet.put("R1", "C3", 3);

        sheet.updateRow("R1", v -> v == null ? 0 : v * 10);

        assertEquals(Integer.valueOf(10), sheet.get("R1", "C1"));
        assertEquals(Integer.valueOf(20), sheet.get("R1", "C2"));
        assertEquals(Integer.valueOf(30), sheet.get("R1", "C3"));
    }

    @Test
    public void testRemoveRow() {
        sheet.put("R2", "C1", 100);
        sheet.removeRow("R2");
        assertEquals(2, sheet.rowLength());
        assertFalse(sheet.containsRow("R2"));
    }

    @Test
    public void testMoveRow() {
        sheet.put("R1", "C1", 1);
        sheet.put("R2", "C1", 2);
        sheet.put("R3", "C1", 3);

        sheet.moveRow("R1", 2);

        assertEquals(Integer.valueOf(2), sheet.get(0, 0));
        assertEquals(Integer.valueOf(3), sheet.get(1, 0));
        assertEquals(Integer.valueOf(1), sheet.get(2, 0));
    }

    @Test
    public void testSwapRowPosition() {
        sheet.put("R1", "C1", 1);
        sheet.put("R2", "C1", 2);

        sheet.swapRowPosition("R1", "R2");

        assertEquals(Integer.valueOf(1), sheet.get("R1", "C1"));
        assertEquals(Integer.valueOf(2), sheet.get("R2", "C1"));
    }

    @Test
    public void testRenameRow() {
        sheet.put("R1", "C1", 100);
        sheet.renameRow("R1", "R1_NEW");

        assertTrue(sheet.containsRow("R1_NEW"));
        assertFalse(sheet.containsRow("R1"));
        assertEquals(Integer.valueOf(100), sheet.get("R1_NEW", "C1"));
    }

    @Test
    public void testContainsRow() {
        assertTrue(sheet.containsRow("R1"));
        assertFalse(sheet.containsRow("R4"));
    }

    @Test
    public void testRow() {
        sheet.put("R1", "C1", 1);
        sheet.put("R1", "C2", 2);
        sheet.put("R1", "C3", 3);

        Map<String, Integer> rowMap = sheet.row("R1");
        assertEquals(3, rowMap.size());
        assertEquals(Integer.valueOf(1), rowMap.get("C1"));
        assertEquals(Integer.valueOf(2), rowMap.get("C2"));
        assertEquals(Integer.valueOf(3), rowMap.get("C3"));
    }

    @Test
    public void testRowMap() {
        sheet.put("R1", "C1", 1);
        sheet.put("R2", "C2", 2);

        Map<String, Map<String, Integer>> allRows = sheet.rowMap();
        assertEquals(3, allRows.size());
        assertEquals(Integer.valueOf(1), allRows.get("R1").get("C1"));
        assertEquals(Integer.valueOf(2), allRows.get("R2").get("C2"));
    }

    // Column operations tests
    @Test
    public void testGetColumn() {
        sheet.put("R1", "C1", 1);
        sheet.put("R2", "C1", 2);
        sheet.put("R3", "C1", 3);

        List<Integer> column = sheet.getColumn("C1");
        assertEquals(Arrays.asList(1, 2, 3), column);
    }

    @Test
    public void testSetColumn() {
        List<Integer> newColumn = Arrays.asList(10, 20, 30);
        sheet.setColumn("C1", newColumn);

        assertEquals(Integer.valueOf(10), sheet.get("R1", "C1"));
        assertEquals(Integer.valueOf(20), sheet.get("R2", "C1"));
        assertEquals(Integer.valueOf(30), sheet.get("R3", "C1"));
    }

    @Test
    public void testAddColumn() {
        sheet.addColumn("C4", Arrays.asList(1, 2, 3));
        assertEquals(4, sheet.columnLength());
        assertEquals(Integer.valueOf(2), sheet.get("R2", "C4"));
    }

    @Test
    public void testAddColumnAtIndex() {
        sheet.addColumn(1, "C_NEW", Arrays.asList(10, 20, 30));
        assertEquals(4, sheet.columnLength());
        assertEquals(Integer.valueOf(20), sheet.get("R2", "C_NEW"));
    }

    @Test
    public void testAddExistingColumn() {
        assertThrows(IllegalArgumentException.class, () -> sheet.addColumn("C1", Arrays.asList(1, 2, 3)));
    }

    @Test
    public void testUpdateColumn() {
        sheet.put("R1", "C1", 1);
        sheet.put("R2", "C1", 2);
        sheet.put("R3", "C1", 3);

        sheet.updateColumn("C1", v -> v == null ? 0 : v * 10);

        assertEquals(Integer.valueOf(10), sheet.get("R1", "C1"));
        assertEquals(Integer.valueOf(20), sheet.get("R2", "C1"));
        assertEquals(Integer.valueOf(30), sheet.get("R3", "C1"));
    }

    @Test
    public void testRemoveColumn() {
        sheet.put("R1", "C2", 100);
        sheet.removeColumn("C2");
        assertEquals(2, sheet.columnLength());
        assertFalse(sheet.containsColumn("C2"));
    }

    @Test
    public void testMoveColumn() {
        sheet.put("R1", "C1", 1);
        sheet.put("R1", "C2", 2);
        sheet.put("R1", "C3", 3);

        sheet.moveColumn("C1", 2);

        assertEquals(Integer.valueOf(2), sheet.get(0, 0));
        assertEquals(Integer.valueOf(3), sheet.get(0, 1));
        assertEquals(Integer.valueOf(1), sheet.get(0, 2));
    }

    @Test
    public void testSwapColumnPosition() {
        sheet.put("R1", "C1", 1);
        sheet.put("R1", "C2", 2);

        sheet.println();

        sheet.swapColumnPosition("C1", "C2");

        sheet.println();

        assertEquals(Integer.valueOf(1), sheet.get("R1", "C1"));
        assertEquals(Integer.valueOf(2), sheet.get("R1", "C2"));

        {
            Sheet<String, String, Object> sheetA = Sheet.rows(List.of("row1", "row2", "row3"), List.of("col1", "col2", "col3"),
                    new Integer[][] { { 1, 2, 3 }, { 4, 5, 6 }, { 7, 8, 9 } });
            sheetA.println();

            sheetA.swapColumnPosition("col1", "col3");

            sheetA.println();
        }
    }

    @Test
    public void testRenameColumn() {
        sheet.put("R1", "C1", 100);
        sheet.renameColumn("C1", "C1_NEW");

        assertTrue(sheet.containsColumn("C1_NEW"));
        assertFalse(sheet.containsColumn("C1"));
        assertEquals(Integer.valueOf(100), sheet.get("R1", "C1_NEW"));
    }

    @Test
    public void testContainsColumn() {
        assertTrue(sheet.containsColumn("C1"));
        assertFalse(sheet.containsColumn("C4"));
    }

    @Test
    public void testColumn() {
        sheet.put("R1", "C1", 1);
        sheet.put("R2", "C1", 2);
        sheet.put("R3", "C1", 3);

        Map<String, Integer> columnMap = sheet.column("C1");
        assertEquals(3, columnMap.size());
        assertEquals(Integer.valueOf(1), columnMap.get("R1"));
        assertEquals(Integer.valueOf(2), columnMap.get("R2"));
        assertEquals(Integer.valueOf(3), columnMap.get("R3"));
    }

    @Test
    public void testColumnMap() {
        sheet.put("R1", "C1", 1);
        sheet.put("R2", "C2", 2);

        Map<String, Map<String, Integer>> allColumns = sheet.columnMap();
        assertEquals(3, allColumns.size());
        assertEquals(Integer.valueOf(1), allColumns.get("C1").get("R1"));
        assertEquals(Integer.valueOf(2), allColumns.get("C2").get("R2"));
    }

    // Length tests
    @Test
    public void testRowLength() {
        assertEquals(3, sheet.rowLength());
    }

    @Test
    public void testColumnLength() {
        assertEquals(3, sheet.columnLength());
    }

    // Update operations tests
    @Test
    public void testUpdateAll() {
        sheet.put("R1", "C1", 1);
        sheet.put("R2", "C2", 2);
        sheet.put("R3", "C3", 3);

        sheet.updateAll(v -> v == null ? 0 : v * 2);

        assertEquals(Integer.valueOf(2), sheet.get("R1", "C1"));
        assertEquals(Integer.valueOf(4), sheet.get("R2", "C2"));
        assertEquals(Integer.valueOf(6), sheet.get("R3", "C3"));
        assertEquals(Integer.valueOf(0), sheet.get("R1", "C2"));
    }

    @Test
    public void testUpdateAllWithIndices() {
        sheet.updateAll((rowIndex, columnIndex) -> rowIndex * 10 + columnIndex);

        assertEquals(Integer.valueOf(0), sheet.get(0, 0));
        assertEquals(Integer.valueOf(11), sheet.get(1, 1));
        assertEquals(Integer.valueOf(22), sheet.get(2, 2));
    }

    @Test
    public void testUpdateAllWithKeys() {
        sheet.updateAll((rowKey, columnKey, value) -> {
            int rowNum = Integer.parseInt(rowKey.substring(1));
            int colNum = Integer.parseInt(columnKey.substring(1));
            return rowNum * colNum;
        });

        assertEquals(Integer.valueOf(1), sheet.get("R1", "C1"));
        assertEquals(Integer.valueOf(4), sheet.get("R2", "C2"));
        assertEquals(Integer.valueOf(9), sheet.get("R3", "C3"));
    }

    @Test
    public void testReplaceIf() {
        sheet.put("R1", "C1", 1);
        sheet.put("R2", "C2", 2);
        sheet.put("R3", "C3", 3);

        sheet.replaceIf(v -> v != null && v > 1, 999);

        assertEquals(Integer.valueOf(1), sheet.get("R1", "C1"));
        assertEquals(Integer.valueOf(999), sheet.get("R2", "C2"));
        assertEquals(Integer.valueOf(999), sheet.get("R3", "C3"));
    }

    @Test
    public void testReplaceIfWithIndices() {
        sheet.replaceIf((rowIndex, columnIndex) -> rowIndex == columnIndex, 100);

        assertEquals(Integer.valueOf(100), sheet.get(0, 0));
        assertEquals(Integer.valueOf(100), sheet.get(1, 1));
        assertEquals(Integer.valueOf(100), sheet.get(2, 2));
        assertNull(sheet.get(0, 1));
    }

    @Test
    public void testReplaceIfWithKeys() {
        sheet.replaceIf((rowKey, columnKey, value) -> "R1".equals(rowKey) && "C1".equals(columnKey), 100);

        assertEquals(Integer.valueOf(100), sheet.get("R1", "C1"));
        assertNull(sheet.get("R1", "C2"));
    }

    // Sort tests
    @Test
    public void testSortByRowKey() {
        Sheet<String, String, Integer> unsortedSheet = new Sheet<>(Arrays.asList("R3", "R1", "R2"), columnKeys);
        unsortedSheet.put("R1", "C1", 1);
        unsortedSheet.put("R2", "C1", 2);
        unsortedSheet.put("R3", "C1", 3);

        unsortedSheet.sortByRowKey();

        List<String> sortedRowKeys = new ArrayList<>(unsortedSheet.rowKeySet());
        assertEquals(Arrays.asList("R1", "R2", "R3"), sortedRowKeys);
    }

    @Test
    public void testSortByRowKeyWithComparator() {
        sheet.put("R1", "C1", 1);
        sheet.put("R2", "C1", 2);
        sheet.put("R3", "C1", 3);

        sheet.sortByRowKey(Comparator.reverseOrder());

        List<String> sortedRowKeys = new ArrayList<>(sheet.rowKeySet());
        assertEquals(Arrays.asList("R3", "R2", "R1"), sortedRowKeys);
    }

    @Test
    public void testSortByRow() {
        sheet.put("R1", "C1", 3);
        sheet.put("R1", "C2", 1);
        sheet.put("R1", "C3", 2);

        sheet.sortByRow("R1", Comparator.naturalOrder());

        List<String> sortedColumnKeys = new ArrayList<>(sheet.columnKeySet());
        assertEquals("C2", sortedColumnKeys.get(0));
        assertEquals("C3", sortedColumnKeys.get(1));
        assertEquals("C1", sortedColumnKeys.get(2));
    }

    @Test
    public void testSortByRows() {
        sheet.put("R1", "C1", 1);
        sheet.put("R1", "C2", 2);
        sheet.put("R1", "C3", 3);
        sheet.put("R2", "C1", 3);
        sheet.put("R2", "C2", 1);
        sheet.put("R2", "C3", 2);

        sheet.println();

        sheet.sortByRows(Arrays.asList("R1", "R2"), (a, b) -> {
            int sum1 = 0, sum2 = 0;
            for (Object o : a)
                if (o != null)
                    sum1 += (Integer) o;
            for (Object o : b)
                if (o != null)
                    sum2 += (Integer) o;
            return Integer.compare(sum1, sum2);
        });

        sheet.println();

        // Columns should be sorted by sum of R1 and R2 values
        List<Integer> row1 = sheet.getRow("R1");
        assertEquals(Integer.valueOf(2), row1.get(0)); // was C2
        assertEquals(Integer.valueOf(1), row1.get(1)); // was C3
        assertEquals(Integer.valueOf(3), row1.get(2)); // was C1
    }

    @Test
    public void testSortByColumnKey() {
        Sheet<String, String, Integer> unsortedSheet = new Sheet<>(rowKeys, Arrays.asList("C3", "C1", "C2"));
        unsortedSheet.put("R1", "C1", 1);
        unsortedSheet.put("R1", "C2", 2);
        unsortedSheet.put("R1", "C3", 3);

        unsortedSheet.sortByColumnKey();

        List<String> sortedColumnKeys = new ArrayList<>(unsortedSheet.columnKeySet());
        assertEquals(Arrays.asList("C1", "C2", "C3"), sortedColumnKeys);
    }

    @Test
    public void testSortByColumn() {
        sheet.put("R1", "C1", 3);
        sheet.put("R2", "C1", 1);
        sheet.put("R3", "C1", 2);

        sheet.sortByColumn("C1", Comparator.naturalOrder());

        List<String> sortedRowKeys = new ArrayList<>(sheet.rowKeySet());
        assertEquals("R2", sortedRowKeys.get(0));
        assertEquals("R3", sortedRowKeys.get(1));
        assertEquals("R1", sortedRowKeys.get(2));
    }

    @Test
    public void testSortByColumns() {
        sheet.put("R1", "C1", 1);
        sheet.put("R2", "C1", 2);
        sheet.put("R3", "C1", 3);
        sheet.put("R1", "C2", 3);
        sheet.put("R2", "C2", 1);
        sheet.put("R3", "C2", 2);

        sheet.println();

        sheet.sortByColumns(Arrays.asList("C1", "C2"), (a, b) -> {
            int sum1 = 0, sum2 = 0;
            for (Object o : a)
                if (o != null)
                    sum1 += (Integer) o;
            for (Object o : b)
                if (o != null)
                    sum2 += (Integer) o;
            return Integer.compare(sum1, sum2);
        });

        sheet.println();

        // Rows should be sorted by sum of C1 and C2 values
        List<Integer> col1 = sheet.getColumn("C1");
        assertEquals(Integer.valueOf(2), col1.get(0)); // was R2
        assertEquals(Integer.valueOf(1), col1.get(1)); // was R3
        assertEquals(Integer.valueOf(3), col1.get(2)); // was R1
    }

    // Copy and Clone tests
    @Test
    public void testCopy() {
        sheet.put("R1", "C1", 100);
        sheet.put("R2", "C2", 200);

        Sheet<String, String, Integer> copy = sheet.copy();

        assertEquals(sheet.rowLength(), copy.rowLength());
        assertEquals(sheet.columnLength(), copy.columnLength());
        assertEquals(Integer.valueOf(100), copy.get("R1", "C1"));
        assertEquals(Integer.valueOf(200), copy.get("R2", "C2"));

        // Modify copy should not affect original
        copy.put("R1", "C1", 999);
        assertEquals(Integer.valueOf(100), sheet.get("R1", "C1"));
    }

    @Test
    public void testCopyWithSubset() {
        sheet.put("R1", "C1", 1);
        sheet.put("R2", "C2", 2);
        sheet.put("R3", "C3", 3);

        Sheet<String, String, Integer> copy = sheet.copy(Arrays.asList("R1", "R2"), Arrays.asList("C1", "C2"));

        assertEquals(2, copy.rowLength());
        assertEquals(2, copy.columnLength());
        assertEquals(Integer.valueOf(1), copy.get("R1", "C1"));
        assertEquals(Integer.valueOf(2), copy.get("R2", "C2"));
        assertFalse(copy.containsRow("R3"));
        assertFalse(copy.containsColumn("C3"));
    }

    @Test
    public void testClone() {
        // Note: This test assumes KryoParser is available
        // If not available, it will throw RuntimeException
        try {
            sheet.put("R1", "C1", 100);
            Sheet<String, String, Integer> cloned = sheet.clone();

            assertEquals(sheet.rowLength(), cloned.rowLength());
            assertEquals(sheet.columnLength(), cloned.columnLength());
            assertEquals(Integer.valueOf(100), cloned.get("R1", "C1"));
        } catch (RuntimeException e) {
            // Expected if Kryo is not available
            assertTrue(e.getMessage().contains("Kryo is required"));
        }
    }

    // Merge test
    @Test
    public void testMerge() {
        sheet.put("R1", "C1", 1);
        sheet.put("R2", "C2", 2);

        Sheet<String, String, Integer> other = new Sheet<>(Arrays.asList("R2", "R3"), Arrays.asList("C2", "C3"));
        other.put("R2", "C2", 20);
        other.put("R3", "C3", 30);

        Sheet<String, String, Integer> merged = sheet.merge(other, (v1, v2) -> {
            if (v1 == null)
                return v2;
            if (v2 == null)
                return v1;
            return v1 + v2;
        });

        assertEquals(Integer.valueOf(1), merged.get("R1", "C1"));
        assertEquals(Integer.valueOf(22), merged.get("R2", "C2")); // 2 + 20
        assertEquals(Integer.valueOf(30), merged.get("R3", "C3"));
    }

    // Transpose test
    @Test
    public void testTranspose() {
        sheet.put("R1", "C1", 1);
        sheet.put("R1", "C2", 2);
        sheet.put("R2", "C1", 3);
        sheet.put("R2", "C2", 4);

        Sheet<String, String, Integer> transposed = sheet.transpose();

        assertEquals(sheet.rowLength(), transposed.columnLength());
        assertEquals(sheet.columnLength(), transposed.rowLength());
        assertEquals(Integer.valueOf(1), transposed.get("C1", "R1"));
        assertEquals(Integer.valueOf(2), transposed.get("C2", "R1"));
        assertEquals(Integer.valueOf(3), transposed.get("C1", "R2"));
        assertEquals(Integer.valueOf(4), transposed.get("C2", "R2"));
    }

    // Freeze tests
    @Test
    public void testFreeze() {
        assertFalse(sheet.isFrozen());
        sheet.freeze();
        assertTrue(sheet.isFrozen());
    }

    @Test
    public void testModifyFrozenSheet() {
        sheet.freeze();
        assertThrows(IllegalStateException.class, () -> sheet.put("R1", "C1", 100));
    }

    // Clear and trim tests
    @Test
    public void testClear() {
        sheet.put("R1", "C1", 1);
        sheet.put("R2", "C2", 2);

        sheet.clear();

        assertNull(sheet.get("R1", "C1"));
        assertNull(sheet.get("R2", "C2"));
        assertEquals(3, sheet.rowLength());
        assertEquals(3, sheet.columnLength());
    }

    @Test
    public void testTrimToSize() {
        sheet.put("R1", "C1", 1);
        sheet.trimToSize(); // Should not throw exception
    }

    // Count and isEmpty tests
    @Test
    public void testCountOfNonNullValue() {
        assertEquals(0, sheet.countOfNonNullValue());

        sheet.put("R1", "C1", 1);
        sheet.put("R2", "C2", 2);
        sheet.put("R3", "C3", 3);

        assertEquals(3, sheet.countOfNonNullValue());
    }

    @Test
    public void testIsEmpty() {
        assertFalse(sheet.isEmpty());

        Sheet<String, String, Integer> emptySheet = new Sheet<>();
        assertTrue(emptySheet.isEmpty());
    }

    // ForEach tests
    @Test
    public void testForEachH() {
        sheet.put("R1", "C1", 1);
        sheet.put("R2", "C2", 2);

        List<String> visited = new ArrayList<>();
        sheet.forEachH((rowKey, columnKey, value) -> {
            visited.add(rowKey + "," + columnKey + "," + value);
        });

        assertEquals(9, visited.size());
        assertTrue(visited.contains("R1,C1,1"));
        assertTrue(visited.contains("R2,C2,2"));
    }

    @Test
    public void testForEachV() {
        sheet.put("R1", "C1", 1);
        sheet.put("R2", "C2", 2);

        List<String> visited = new ArrayList<>();
        sheet.forEachV((rowKey, columnKey, value) -> {
            visited.add(rowKey + "," + columnKey + "," + value);
        });

        assertEquals(9, visited.size());
        // First column should be visited completely before second column
        assertEquals("R1,C1,1", visited.get(0));
        assertEquals("R2,C1,null", visited.get(1));
        assertEquals("R3,C1,null", visited.get(2));
    }

    @Test
    public void testForEachNonNullH() {
        sheet.put("R1", "C1", 1);
        sheet.put("R2", "C2", 2);

        List<String> visited = new ArrayList<>();
        sheet.forEachNonNullH((rowKey, columnKey, value) -> {
            visited.add(rowKey + "," + columnKey + "," + value);
        });

        assertEquals(2, visited.size());
        assertTrue(visited.contains("R1,C1,1"));
        assertTrue(visited.contains("R2,C2,2"));
    }

    @Test
    public void testForEachNonNullV() {
        sheet.put("R1", "C1", 1);
        sheet.put("R2", "C2", 2);

        List<String> visited = new ArrayList<>();
        sheet.forEachNonNullV((rowKey, columnKey, value) -> {
            visited.add(rowKey + "," + columnKey + "," + value);
        });

        assertEquals(2, visited.size());
        assertTrue(visited.contains("R1,C1,1"));
        assertTrue(visited.contains("R2,C2,2"));
    }

    // Stream tests - cells
    @Test
    public void testCellsH() {
        sheet.put("R1", "C1", 1);
        sheet.put("R2", "C2", 2);

        List<Cell<String, String, Integer>> cells = sheet.cellsH().toList();
        assertEquals(9, cells.size());

        Cell<String, String, Integer> firstCell = cells.get(0);
        assertEquals("R1", firstCell.rowKey());
        assertEquals("C1", firstCell.columnKey());
        assertEquals(Integer.valueOf(1), firstCell.value());
    }

    @Test
    public void testCellsHRange() {
        sheet.put("R1", "C1", 1);
        sheet.put("R2", "C2", 2);

        List<Cell<String, String, Integer>> cells = sheet.cellsH(1, 2).toList();
        assertEquals(3, cells.size()); // Only row R2
    }

    @Test
    public void testCellsV() {
        sheet.put("R1", "C1", 1);
        sheet.put("R2", "C2", 2);

        List<Cell<String, String, Integer>> cells = sheet.cellsV().toList();
        assertEquals(9, cells.size());

        // First three should be from column C1
        assertEquals("C1", cells.get(0).columnKey());
        assertEquals("C1", cells.get(1).columnKey());
        assertEquals("C1", cells.get(2).columnKey());
    }

    @Test
    public void testCellsR() {
        sheet.put("R1", "C1", 1);
        sheet.put("R1", "C2", 2);

        List<List<Cell<String, String, Integer>>> rows = sheet.cellsR().map(stream -> stream.toList()).toList();

        assertEquals(3, rows.size());
        assertEquals(3, rows.get(0).size());
        assertEquals("R1", rows.get(0).get(0).rowKey());
    }

    @Test
    public void testCellsC() {
        sheet.put("R1", "C1", 1);
        sheet.put("R2", "C1", 2);

        List<List<Cell<String, String, Integer>>> columns = sheet.cellsC().map(stream -> stream.toList()).toList();

        assertEquals(3, columns.size());
        assertEquals(3, columns.get(0).size());
        assertEquals("C1", columns.get(0).get(0).columnKey());
    }

    // Stream tests - points
    @Test
    public void testPointsH() {
        List<Point> points = sheet.pointsH().toList();
        assertEquals(9, points.size());
        assertEquals(Point.of(0, 0), points.get(0));
        assertEquals(Point.of(0, 1), points.get(1));
        assertEquals(Point.of(0, 2), points.get(2));
        assertEquals(Point.of(1, 0), points.get(3));
    }

    @Test
    public void testPointsV() {
        List<Point> points = sheet.pointsV().toList();
        assertEquals(9, points.size());
        assertEquals(Point.of(0, 0), points.get(0));
        assertEquals(Point.of(1, 0), points.get(1));
        assertEquals(Point.of(2, 0), points.get(2));
        assertEquals(Point.of(0, 1), points.get(3));
    }

    @Test
    public void testPointsR() {
        List<List<Point>> rows = sheet.pointsR().map(stream -> stream.toList()).toList();

        assertEquals(3, rows.size());
        assertEquals(3, rows.get(0).size());
        assertEquals(Point.of(0, 0), rows.get(0).get(0));
    }

    @Test
    public void testPointsC() {
        List<List<Point>> columns = sheet.pointsC().map(stream -> stream.toList()).toList();

        assertEquals(3, columns.size());
        assertEquals(3, columns.get(0).size());
        assertEquals(Point.of(0, 0), columns.get(0).get(0));
    }

    // Stream tests - values
    @Test
    public void testStreamH() {
        sheet.put("R1", "C1", 1);
        sheet.put("R1", "C2", 2);
        sheet.put("R2", "C1", 3);

        List<Integer> values = sheet.streamH().toList();
        assertEquals(9, values.size());
        assertEquals(Integer.valueOf(1), values.get(0));
        assertEquals(Integer.valueOf(2), values.get(1));
    }

    @Test
    public void testStreamV() {
        sheet.put("R1", "C1", 1);
        sheet.put("R1", "C2", 2);
        sheet.put("R2", "C1", 3);

        List<Integer> values = sheet.streamV().toList();
        assertEquals(9, values.size());
        assertEquals(Integer.valueOf(1), values.get(0));
        assertEquals(Integer.valueOf(3), values.get(1)); // Next in same column
    }

    @Test
    public void testStreamR() {
        sheet.put("R1", "C1", 1);
        sheet.put("R1", "C2", 2);

        List<List<Integer>> rows = sheet.streamR().map(stream -> stream.toList()).toList();

        assertEquals(3, rows.size());
        assertEquals(3, rows.get(0).size());
        assertEquals(Integer.valueOf(1), rows.get(0).get(0));
        assertEquals(Integer.valueOf(2), rows.get(0).get(1));
    }

    @Test
    public void testStreamC() {
        sheet.put("R1", "C1", 1);
        sheet.put("R2", "C1", 2);

        List<List<Integer>> columns = sheet.streamC().map(stream -> stream.toList()).toList();

        assertEquals(3, columns.size());
        assertEquals(3, columns.get(0).size());
        assertEquals(Integer.valueOf(1), columns.get(0).get(0));
        assertEquals(Integer.valueOf(2), columns.get(0).get(1));
    }

    // Dataset conversion tests
    @Test
    public void testToDatasetH() {
        sheet.put("R1", "C1", 1);
        sheet.put("R1", "C2", 2);
        sheet.put("R2", "C1", 3);
        sheet.put("R2", "C2", 4);

        Dataset ds = sheet.toDatasetH();

        assertEquals(3, ds.columnCount());
        assertEquals(3, ds.size());
        assertEquals("C1", ds.getColumnName(0));
        assertEquals("C2", ds.getColumnName(1));
        assertEquals("C3", ds.getColumnName(2));
    }

    @Test
    public void testToDatasetV() {
        sheet.put("R1", "C1", 1);
        sheet.put("R1", "C2", 2);
        sheet.put("R2", "C1", 3);
        sheet.put("R2", "C2", 4);

        Dataset ds = sheet.toDatasetV();

        assertEquals(3, ds.columnCount());
        assertEquals(3, ds.size());
        assertEquals("R1", ds.getColumnName(0));
        assertEquals("R2", ds.getColumnName(1));
        assertEquals("R3", ds.getColumnName(2));
    }

    // Array conversion tests
    @Test
    public void testToArrayH() {
        sheet.put("R1", "C1", 1);
        sheet.put("R1", "C2", 2);
        sheet.put("R2", "C1", 3);
        sheet.put("R2", "C2", 4);

        Object[][] array = sheet.toArrayH();

        assertEquals(3, array.length);
        assertEquals(3, array[0].length);
        assertEquals(1, array[0][0]);
        assertEquals(2, array[0][1]);
        assertEquals(3, array[1][0]);
        assertEquals(4, array[1][1]);
    }

    @Test
    public void testToArrayHTyped() {
        sheet.put("R1", "C1", 1);
        sheet.put("R1", "C2", 2);
        sheet.put("R2", "C1", 3);
        sheet.put("R2", "C2", 4);

        Integer[][] array = sheet.toArrayH(Integer.class);

        assertEquals(3, array.length);
        assertEquals(3, array[0].length);
        assertEquals(Integer.valueOf(1), array[0][0]);
        assertEquals(Integer.valueOf(2), array[0][1]);
    }

    @Test
    public void testToArrayV() {
        sheet.put("R1", "C1", 1);
        sheet.put("R1", "C2", 2);
        sheet.put("R2", "C1", 3);
        sheet.put("R2", "C2", 4);

        Object[][] array = sheet.toArrayV();

        assertEquals(3, array.length);
        assertEquals(3, array[0].length);
        assertEquals(1, array[0][0]);
        assertEquals(3, array[0][1]);
        assertEquals(2, array[1][0]);
        assertEquals(4, array[1][1]);
    }

    @Test
    public void testToArrayVTyped() {
        sheet.put("R1", "C1", 1);
        sheet.put("R1", "C2", 2);
        sheet.put("R2", "C1", 3);
        sheet.put("R2", "C2", 4);

        Integer[][] array = sheet.toArrayV(Integer.class);

        assertEquals(3, array.length);
        assertEquals(3, array[0].length);
        assertEquals(Integer.valueOf(1), array[0][0]);
        assertEquals(Integer.valueOf(3), array[0][1]);
    }

    // Functional interface tests
    @Test
    public void testApply() {
        sheet.put("R1", "C1", 1);

        Integer result = sheet.apply(s -> s.get("R1", "C1"));
        assertEquals(Integer.valueOf(1), result);
    }

    @Test
    public void testApplyIfNotEmpty() {
        sheet.put("R1", "C1", 1);

        u.Optional<Integer> result = sheet.applyIfNotEmpty(s -> s.get("R1", "C1"));
        assertTrue(result.isPresent());
        assertEquals(Integer.valueOf(1), result.get());

        Sheet<String, String, Integer> emptySheet = new Sheet<>();
        u.Optional<Integer> emptyResult = emptySheet.applyIfNotEmpty(s -> 100);
        assertFalse(emptyResult.isPresent());
    }

    @Test
    public void testAccept() {
        List<Integer> values = new ArrayList<>();
        sheet.put("R1", "C1", 1);

        sheet.accept(s -> values.add(s.get("R1", "C1")));
        assertEquals(1, values.size());
        assertEquals(Integer.valueOf(1), values.get(0));
    }

    @Test
    public void testAcceptIfNotEmpty() {
        List<Integer> values = new ArrayList<>();
        sheet.put("R1", "C1", 1);

        OrElse result = sheet.acceptIfNotEmpty(s -> values.add(s.get("R1", "C1")));
        assertEquals(OrElse.TRUE, result);
        assertEquals(1, values.size());

        Sheet<String, String, Integer> emptySheet = new Sheet<>();
        OrElse emptyResult = emptySheet.acceptIfNotEmpty(s -> values.add(100));
        assertEquals(OrElse.FALSE, emptyResult);
        assertEquals(1, values.size()); // Should not have added 100
    }

    // Print tests
    @Test
    public void testPrintln() {
        sheet.put("R1", "C1", 1);
        sheet.put("R2", "C2", 2);

        StringWriter writer = new StringWriter();
        sheet.println(writer);

        String output = writer.toString();
        assertTrue(output.contains("R1"));
        assertTrue(output.contains("C1"));
        assertTrue(output.contains("1"));
    }

    @Test
    public void testPrintln_01() {
        {
            N.println(Strings.repeat("=", 120));
            Sheet.empty().println();
            N.println(Strings.repeat("=", 120));

            N.println(Strings.repeat("=", 120));
            Sheet.empty().println("# ");
            N.println(Strings.repeat("=", 120));

            N.println(Strings.repeat("=", 120));
            Sheet.empty().println("// ");
            N.println(Strings.repeat("=", 120));
        }

        Object[][] data = { { 1, 2, 3 }, { 4, 5, 6 }, { 7, 8, 9 } };
        Sheet<String, String, Integer> sheet = Sheet.rows(rowKeys, columnKeys, data);

        {
            N.println(Strings.repeat("=", 120));
            sheet.println();
            N.println(Strings.repeat("=", 120));

            N.println(Strings.repeat("=", 120));
            sheet.println("     * ");
            N.println(Strings.repeat("=", 120));

            N.println(Strings.repeat("=", 120));
            sheet.println("// ");
            N.println(Strings.repeat("=", 120));
        }
    }

    @Test
    public void testPrintlnWithSubset() {
        sheet.put("R1", "C1", 1);
        sheet.put("R2", "C2", 2);

        StringWriter writer = new StringWriter();
        sheet.println(Arrays.asList("R1"), Arrays.asList("C1"), writer);

        String output = writer.toString();
        assertTrue(output.contains("R1"));
        assertTrue(output.contains("C1"));
        assertTrue(output.contains("1"));
        assertFalse(output.contains("R2"));
    }

    // Hash, equals, toString tests
    @Test
    public void testHashCode() {
        sheet.put("R1", "C1", 1);

        Sheet<String, String, Integer> other = new Sheet<>(rowKeys, columnKeys);
        other.put("R1", "C1", 1);

        assertEquals(sheet.hashCode(), other.hashCode());
    }

    @Test
    public void testEquals() {
        sheet.put("R1", "C1", 1);

        Sheet<String, String, Integer> other = new Sheet<>(rowKeys, columnKeys);
        other.put("R1", "C1", 1);

        assertTrue(sheet.equals(other));
        assertTrue(sheet.equals(sheet));
        assertFalse(sheet.equals(null));
        assertFalse(sheet.equals("not a sheet"));

        other.put("R1", "C1", 2);
        assertFalse(sheet.equals(other));
    }

    @Test
    public void testToString() {
        sheet.put("R1", "C1", 1);

        String str = sheet.toString();
        assertTrue(str.contains("rowKeySet"));
        assertTrue(str.contains("columnKeySet"));
        assertTrue(str.contains("R1"));
        assertTrue(str.contains("C1"));
    }

    // Cell and Point tests
    @Test
    public void testCellCreation() {
        Cell<String, String, Integer> cell = Cell.of("R1", "C1", 100);
        assertEquals("R1", cell.rowKey());
        assertEquals("C1", cell.columnKey());
        assertEquals(Integer.valueOf(100), cell.value());
    }

    @Test
    public void testPointCreation() {
        Point point = Point.of(1, 2);
        assertEquals(1, point.rowIndex());
        assertEquals(2, point.columnIndex());

        // Test cached points
        Point cached1 = Point.of(0, 0);
        Point cached2 = Point.of(0, 0);
        assertSame(cached1, cached2);
        assertEquals(Point.ZERO, cached1);
    }

    // Edge cases and special scenarios
    @Test
    public void testEmptySheetOperations() {
        Sheet<String, String, Integer> emptySheet = new Sheet<>();

        assertEquals(0, emptySheet.countOfNonNullValue());
        assertTrue(emptySheet.cellsH().toList().isEmpty());
        assertTrue(emptySheet.streamH().toList().isEmpty());

        Map<String, Map<String, Integer>> rowMap = emptySheet.rowMap();
        assertTrue(rowMap.isEmpty());
    }

    @Test
    public void testNullValueHandling() {
        sheet.put("R1", "C1", null);
        assertNull(sheet.get("R1", "C1"));
        assertTrue(sheet.containsValue(null));

        sheet.updateAll(v -> v == null ? 0 : v);
        assertEquals(Integer.valueOf(0), sheet.get("R1", "C1"));
    }

    @Test
    public void testLargeSheet() {
        // Create a larger sheet for performance/correctness testing
        List<String> largeRowKeys = new ArrayList<>();
        List<String> largeColumnKeys = new ArrayList<>();

        for (int i = 0; i < 100; i++) {
            largeRowKeys.add("R" + i);
            largeColumnKeys.add("C" + i);
        }

        Sheet<String, String, Integer> largeSheet = new Sheet<>(largeRowKeys, largeColumnKeys);

        // Fill diagonal
        for (int i = 0; i < 100; i++) {
            largeSheet.put(i, i, i);
        }

        assertEquals(100, largeSheet.countOfNonNullValue());
        assertEquals(Integer.valueOf(50), largeSheet.get(50, 50));
    }

    // Additional stream method tests with ranges
    @Test
    public void testCellsHWithSingleRow() {
        sheet.put("R2", "C1", 21);
        sheet.put("R2", "C2", 22);
        sheet.put("R2", "C3", 23);

        List<Cell<String, String, Integer>> cells = sheet.cellsH(1).toList();
        assertEquals(3, cells.size());
        assertEquals("R2", cells.get(0).rowKey());
        assertEquals(Integer.valueOf(21), cells.get(0).value());
    }

    @Test
    public void testCellsVWithSingleColumn() {
        sheet.put("R1", "C2", 12);
        sheet.put("R2", "C2", 22);
        sheet.put("R3", "C2", 32);

        List<Cell<String, String, Integer>> cells = sheet.cellsV(1).toList();
        assertEquals(3, cells.size());
        assertEquals("C2", cells.get(0).columnKey());
        assertEquals(Integer.valueOf(12), cells.get(0).value());
    }

    @Test
    public void testPointsHWithSingleRow() {
        List<Point> points = sheet.pointsH(1).toList();
        assertEquals(3, points.size());
        assertEquals(Point.of(1, 0), points.get(0));
        assertEquals(Point.of(1, 1), points.get(1));
        assertEquals(Point.of(1, 2), points.get(2));
    }

    @Test
    public void testPointsVWithSingleColumn() {
        List<Point> points = sheet.pointsV(1).toList();
        assertEquals(3, points.size());
        assertEquals(Point.of(0, 1), points.get(0));
        assertEquals(Point.of(1, 1), points.get(1));
        assertEquals(Point.of(2, 1), points.get(2));
    }

    @Test
    public void testStreamHWithSingleRow() {
        sheet.put("R2", "C1", 21);
        sheet.put("R2", "C2", 22);
        sheet.put("R2", "C3", 23);

        List<Integer> values = sheet.streamH(1).toList();
        assertEquals(3, values.size());
        assertEquals(Integer.valueOf(21), values.get(0));
        assertEquals(Integer.valueOf(22), values.get(1));
        assertEquals(Integer.valueOf(23), values.get(2));
    }

    @Test
    public void testStreamVWithSingleColumn() {
        sheet.put("R1", "C2", 12);
        sheet.put("R2", "C2", 22);
        sheet.put("R3", "C2", 32);

        List<Integer> values = sheet.streamV(1).toList();
        assertEquals(3, values.size());
        assertEquals(Integer.valueOf(12), values.get(0));
        assertEquals(Integer.valueOf(22), values.get(1));
        assertEquals(Integer.valueOf(32), values.get(2));
    }

    // Test stream methods with custom ranges
    @Test
    public void testCellsHWithCustomRange() {
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                sheet.put(i, j, i * 10 + j);
            }
        }

        List<Cell<String, String, Integer>> cells = sheet.cellsH(0, 2).toList();
        assertEquals(6, cells.size()); // 2 rows * 3 columns
        assertEquals(Integer.valueOf(0), cells.get(0).value());
        assertEquals(Integer.valueOf(12), cells.get(5).value());
    }

    @Test
    public void testCellsVWithCustomRange() {
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                sheet.put(i, j, i * 10 + j);
            }
        }

        List<Cell<String, String, Integer>> cells = sheet.cellsV(1, 3).toList();
        assertEquals(6, cells.size()); // 3 rows * 2 columns
        assertEquals(Integer.valueOf(1), cells.get(0).value());
        assertEquals(Integer.valueOf(22), cells.get(5).value());
    }

    @Test
    public void testRowsWithRange() {
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                sheet.put(i, j, i * 10 + j);
            }
        }

        List<Pair<String, List<Integer>>> rows = sheet.rows(1, 3).map(pair -> Pair.of(pair.left(), pair.right().toList())).toList();

        assertEquals(2, rows.size());
        assertEquals("R2", rows.get(0).left());
        assertEquals(Arrays.asList(10, 11, 12), rows.get(0).right());
        assertEquals("R3", rows.get(1).left());
        assertEquals(Arrays.asList(20, 21, 22), rows.get(1).right());
    }

    @Test
    public void testColumnsWithRange() {
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                sheet.put(i, j, i * 10 + j);
            }
        }

        List<Pair<String, List<Integer>>> columns = sheet.columns(0, 2).map(pair -> Pair.of(pair.left(), pair.right().toList())).toList();

        assertEquals(2, columns.size());
        assertEquals("C1", columns.get(0).left());
        assertEquals(Arrays.asList(0, 10, 20), columns.get(0).right());
        assertEquals("C2", columns.get(1).left());
        assertEquals(Arrays.asList(1, 11, 21), columns.get(1).right());
    }

    // Test error cases for ranges
    @Test
    public void testCellsHWithInvalidFromIndex() {
        assertThrows(IndexOutOfBoundsException.class, () -> sheet.cellsH(-1, 2));
    }

    @Test
    public void testCellsHWithInvalidToIndex() {
        assertThrows(IndexOutOfBoundsException.class, () -> sheet.cellsH(0, 5));
    }

    @Test
    public void testCellsHWithFromGreaterThanTo() {
        assertThrows(IndexOutOfBoundsException.class, () -> sheet.cellsH(2, 1));
    }

    @Test
    public void testStreamVWithInvalidRange() {
        assertThrows(IndexOutOfBoundsException.class, () -> sheet.streamV(-1, 2));
    }

    // Test uninitialized sheet behavior
    @Test
    public void testUninitializedSheetOperations() {
        // Sheet is created but no put operations performed
        Sheet<String, String, Integer> uninitSheet = new Sheet<>(rowKeys, columnKeys);

        assertNull(uninitSheet.get("R1", "C1"));
        assertEquals(0, uninitSheet.countOfNonNullValue());

        List<Integer> row = uninitSheet.getRow("R1");
        assertEquals(3, row.size());
        assertTrue(row.stream().allMatch(Fn.isNull()));

        List<Integer> column = uninitSheet.getColumn("C1");
        assertEquals(3, column.size());
        assertTrue(column.stream().allMatch(Fn.isNull()));

        // ForEach on uninitialized sheet
        List<String> visited = new ArrayList<>();
        uninitSheet.forEachH((r, c, v) -> {
            assertNull(v);
            visited.add(r + "," + c);
        });
        assertEquals(9, visited.size());
    }

    // Test frozen sheet comprehensive operations
    @Test
    public void testFrozenSheetComprehensive() {
        sheet.put("R1", "C1", 100);
        sheet.freeze();

        // Try all modifying operations
        try {
            sheet.put("R2", "C2", 200);
            fail("Should throw IllegalStateException");
        } catch (IllegalStateException e) {
            // Expected
        }

        try {
            sheet.putAll(Sheet.empty());
            fail("Should throw IllegalStateException");
        } catch (IllegalStateException e) {
            // Expected
        }

        try {
            sheet.remove("R1", "C1");
            fail("Should throw IllegalStateException");
        } catch (IllegalStateException e) {
            // Expected
        }

        try {
            sheet.setRow("R1", Arrays.asList(1, 2, 3));
            fail("Should throw IllegalStateException");
        } catch (IllegalStateException e) {
            // Expected
        }

        try {
            sheet.addRow("R4", Arrays.asList(1, 2, 3));
            fail("Should throw IllegalStateException");
        } catch (IllegalStateException e) {
            // Expected
        }

        try {
            sheet.updateRow("R1", v -> v);
            fail("Should throw IllegalStateException");
        } catch (IllegalStateException e) {
            // Expected
        }

        try {
            sheet.removeRow("R1");
            fail("Should throw IllegalStateException");
        } catch (IllegalStateException e) {
            // Expected
        }

        try {
            sheet.clear();
            fail("Should throw IllegalStateException");
        } catch (IllegalStateException e) {
            // Expected
        }

        // Read operations should still work
        assertEquals(Integer.valueOf(100), sheet.get("R1", "C1"));
        assertTrue(sheet.contains("R1", "C1"));
    }

    // Test sorting with null values
    @Test
    public void testSortingWithNulls() {
        sheet.put("R1", "C1", null);
        sheet.put("R1", "C2", 3);
        sheet.put("R1", "C3", 1);

        sheet.sortByRow("R1", Comparator.nullsFirst(Comparator.naturalOrder()));

        List<String> sortedColumns = new ArrayList<>(sheet.columnKeySet());
        List<Integer> sortedValues = sheet.getRow("R1");

        assertNull(sortedValues.get(0));
        assertEquals(Integer.valueOf(1), sortedValues.get(1));
        assertEquals(Integer.valueOf(3), sortedValues.get(2));
    }

    // Test with different types
    @Test
    public void testWithStringValues() {
        Sheet<Integer, String, String> stringSheet = new Sheet<>(Arrays.asList(1, 2, 3), Arrays.asList("A", "B", "C"));

        stringSheet.put(1, "A", "Hello");
        stringSheet.put(2, "B", "World");

        assertEquals("Hello", stringSheet.get(1, "A"));
        assertEquals("World", stringSheet.get(2, "B"));

        stringSheet.sortByColumnKey();
        List<String> sortedColumns = new ArrayList<>(stringSheet.columnKeySet());
        assertEquals(Arrays.asList("A", "B", "C"), sortedColumns);
    }

    // Test edge cases for add operations
    @Test
    public void testAddRowAtBeginning() {
        sheet.put("R1", "C1", 1);
        sheet.put("R2", "C1", 2);

        sheet.addRow(0, "R0", Arrays.asList(0, 0, 0));

        assertEquals(4, sheet.rowLength());
        assertEquals(Integer.valueOf(0), sheet.get("R0", "C1"));
        assertEquals(Integer.valueOf(0), sheet.get(0, 0));
        assertEquals(Integer.valueOf(1), sheet.get(1, 0)); // R1 moved to index 1
    }

    @Test
    public void testAddColumnAtEnd() {
        sheet.put("R1", "C1", 1);

        sheet.addColumn(sheet.columnLength(), "C4", Arrays.asList(4, 5, 6));

        assertEquals(4, sheet.columnLength());
        assertEquals(Integer.valueOf(4), sheet.get("R1", "C4"));
    }

    // Test putAll with overlapping data
    @Test
    public void testPutAllWithOverlap() {
        sheet.put("R1", "C1", 1);
        sheet.put("R2", "C2", 2);

        Sheet<String, String, Integer> source = new Sheet<>(Arrays.asList("R1", "R2"), Arrays.asList("C1", "C2"));
        source.put("R1", "C1", 10); // Overlap
        source.put("R1", "C2", 20); // New

        sheet.putAll(source);

        assertEquals(Integer.valueOf(10), sheet.get("R1", "C1")); // Updated
        assertEquals(Integer.valueOf(20), sheet.get("R1", "C2")); // New
        assertEquals(null, sheet.get("R2", "C2")); // Unchanged
    }

    // Test copy with invalid keys
    @Test
    public void testCopyWithInvalidRowKeys() {
        assertThrows(IllegalArgumentException.class, () -> sheet.copy(Arrays.asList("R1", "InvalidRow"), columnKeys));
    }

    @Test
    public void testCopyWithInvalidColumnKeys() {
        assertThrows(IllegalArgumentException.class, () -> sheet.copy(rowKeys, Arrays.asList("C1", "InvalidColumn")));
    }

    // Test println edge cases
    @Test
    public void testPrintlnEmptySheet() {
        Sheet<String, String, Integer> emptySheet = new Sheet<>();
        StringWriter writer = new StringWriter();
        emptySheet.println(writer);

        String output = writer.toString();
        assertTrue(output.contains("+---+"));
        assertTrue(output.contains("|   |"));
    }

    @Test
    public void testPrintlnWithEmptyRowsAndColumns() {
        StringWriter writer = new StringWriter();
        sheet.println(Collections.emptyList(), Collections.emptyList(), writer);

        String output = writer.toString();
        assertTrue(output.contains("+---+"));
    }

    // Test clone with frozen state
    @Test
    public void testCloneWithFrozenState() {
        try {
            sheet.put("R1", "C1", 100);
            sheet.freeze();

            Sheet<String, String, Integer> cloned = sheet.clone(false);
            assertFalse(cloned.isFrozen());

            Sheet<String, String, Integer> clonedFrozen = sheet.clone(true);
            assertTrue(clonedFrozen.isFrozen());
        } catch (RuntimeException e) {
            // Expected if Kryo is not available
            assertTrue(e.getMessage().contains("Kryo is required"));
        }
    }

    // Test Point cache behavior
    @Test
    public void testPointCaching() {
        // Points within cache range should be same instance
        Point p1 = Point.of(10, 20);
        Point p2 = Point.of(10, 20);
        assertSame(p1, p2);

        // Points outside cache range should be different instances
        Point p3 = Point.of(200, 300);
        Point p4 = Point.of(200, 300);
        assertNotSame(p3, p4);
        assertEquals(p3, p4);
    }

    // Test contains with null value
    @Test
    public void testContainsWithNull() {
        assertTrue(sheet.contains("R1", "C1", null));
        assertTrue(sheet.isNull("R1", "C1"));

        sheet.put("R1", "C1", null);
        assertTrue(sheet.contains("R1", "C1", null));

        sheet.put("R1", "C1", 100);
        assertFalse(sheet.contains("R1", "C1", null));
    }

    // Test sorting empty or single element
    @Test
    public void testSortingSingleRow() {
        Sheet<String, String, Integer> singleRowSheet = new Sheet<>(Arrays.asList("R1"), columnKeys);

        singleRowSheet.sortByRowKey();
        assertEquals(1, singleRowSheet.rowLength());
    }

    @Test
    public void testSortingEmptySheet() {
        Sheet<String, String, Integer> emptySheet = new Sheet<>();
        emptySheet.sortByRowKey(); // Should not throw
        emptySheet.sortByColumnKey(); // Should not throw
    }

    // Test update operations on empty sheet
    @Test
    public void testUpdateAllOnEmptySheet() {
        Sheet<String, String, Integer> emptySheet = new Sheet<>();
        emptySheet.updateAll(v -> 100); // Should not throw

        Sheet<String, String, Integer> uninitSheet = new Sheet<>(rowKeys, columnKeys);
        uninitSheet.updateAll(v -> 100);
        assertEquals(Integer.valueOf(100), uninitSheet.get(0, 0));
    }

    // Test exceptional cases in constructors
    @Test
    public void testConstructorWithMismatchedDataRows() {
        Object[][] data = { { 1, 2 }, // Wrong size
                { 4, 5, 6 }, { 7, 8, 9 } };
        assertThrows(IllegalArgumentException.class, () -> new Sheet<>(rowKeys, columnKeys, data));
    }

    @Test
    public void testRowsFactoryWithMismatchedData() {
        List<List<Integer>> rows = Arrays.asList(Arrays.asList(1, 2), // Wrong size
                Arrays.asList(4, 5, 6), Arrays.asList(7, 8, 9));
        assertThrows(IllegalArgumentException.class, () -> Sheet.rows(rowKeys, columnKeys, rows));
    }

    // Test streams with empty ranges
    @Test
    public void testStreamsWithEmptyRange() {
        assertTrue(sheet.cellsH(1, 1).toList().isEmpty());
        assertTrue(sheet.streamV(2, 2).toList().isEmpty());
        assertTrue(sheet.pointsH(0, 0).toList().isEmpty());
    }

    // Final comprehensive integration test
    @Test
    public void testComprehensiveIntegration() {
        // Build a complete sheet
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                sheet.put(i, j, (i + 1) * (j + 1));
            }
        }

        // Test multiple operations in sequence
        sheet.addRow("R4", Arrays.asList(10, 20, 30));
        sheet.addColumn("C4", Arrays.asList(4, 8, 12, 16));
        sheet.swapRowPosition("R1", "R4");

        sheet.println();
        sheet.swapColumnPosition("C1", "C4");

        sheet.println();
        // Verify state
        assertEquals(4, sheet.rowLength());
        assertEquals(4, sheet.columnLength());
        assertEquals(Integer.valueOf(4), sheet.get("R1", "C4"));
        assertEquals(Integer.valueOf(10), sheet.get("R4", "C1"));

        // Sort and verify
        sheet.sortByRowKey();
        List<String> sortedRows = new ArrayList<>(sheet.rowKeySet());
        assertEquals(Arrays.asList("R1", "R2", "R3", "R4"), sortedRows);

        // Test transpose
        Sheet<String, String, Integer> transposed = sheet.transpose();
        assertEquals(4, transposed.rowLength());
        assertEquals(4, transposed.columnLength());
        assertEquals(sheet.get("R1", "C1"), transposed.get("C1", "R1"));
    }

}

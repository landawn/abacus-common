package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.Sheet.Cell;
import com.landawn.abacus.util.Sheet.Point;
import com.landawn.abacus.util.stream.Stream;

@Tag("2025")
public class Sheet2025Test extends TestBase {

    private Sheet<String, String, Integer> sheet;
    private Sheet<String, String, Integer> emptySheet;
    private List<String> rowKeys;
    private List<String> columnKeys;
    private Integer[][] sampleData;

    @BeforeEach
    public void setUp() {
        rowKeys = Arrays.asList("row1", "row2", "row3");
        columnKeys = Arrays.asList("col1", "col2", "col3");
        sampleData = new Integer[][] { { 1, 2, 3 }, { 4, 5, 6 }, { 7, 8, 9 } };

        sheet = Sheet.rows(rowKeys, columnKeys, sampleData);
        emptySheet = Sheet.empty();
    }

    @Test
    public void testDefaultConstructor() {
        Sheet<String, String, Integer> s = new Sheet<>();
        assertNotNull(s);
        assertEquals(0, s.rowCount());
        assertEquals(0, s.columnCount());
        assertTrue(s.isEmpty());
    }

    @Test
    public void testConstructorWithKeySets() {
        Sheet<String, String, Integer> s = new Sheet<>(rowKeys, columnKeys);
        assertNotNull(s);
        assertEquals(3, s.rowCount());
        assertEquals(3, s.columnCount());
        assertFalse(s.isEmpty());
        assertNull(s.get("row1", "col1"));
    }

    @Test
    public void testConstructorWithNullRowKey() {
        List<String> invalidRowKeys = Arrays.asList("row1", null, "row3");
        assertThrows(IllegalArgumentException.class, () -> {
            new Sheet<>(invalidRowKeys, columnKeys);
        });
    }

    @Test
    public void testConstructorWithNullColumnKey() {
        List<String> invalidColumnKeys = Arrays.asList("col1", null, "col3");
        assertThrows(IllegalArgumentException.class, () -> {
            new Sheet<>(rowKeys, invalidColumnKeys);
        });
    }

    @Test
    public void testConstructorWithDuplicateRowKeys() {
        List<String> duplicateRowKeys = Arrays.asList("row1", "row2", "row1");
        assertThrows(IllegalArgumentException.class, () -> {
            new Sheet<>(duplicateRowKeys, columnKeys);
        });
    }

    @Test
    public void testConstructorWithDuplicateColumnKeys() {
        List<String> duplicateColumnKeys = Arrays.asList("col1", "col2", "col1");
        assertThrows(IllegalArgumentException.class, () -> {
            new Sheet<>(rowKeys, duplicateColumnKeys);
        });
    }

    @Test
    public void testConstructorWithData() {
        Sheet<String, String, Integer> s = new Sheet<>(rowKeys, columnKeys, sampleData);
        assertNotNull(s);
        assertEquals(3, s.rowCount());
        assertEquals(3, s.columnCount());
        assertFalse(s.isEmpty());
        assertEquals(Integer.valueOf(1), s.get("row1", "col1"));
        assertEquals(Integer.valueOf(9), s.get("row3", "col3"));
    }

    @Test
    public void testConstructorWithMismatchedRowLength() {
        Integer[][] invalidData = new Integer[][] { { 1, 2, 3 }, { 4, 5, 6 } };
        assertThrows(IllegalArgumentException.class, () -> {
            new Sheet<>(rowKeys, columnKeys, invalidData);
        });
    }

    @Test
    public void testConstructorWithMismatchedColumnLength() {
        Integer[][] invalidData = new Integer[][] { { 1, 2 }, { 4, 5 }, { 7, 8 } };
        assertThrows(IllegalArgumentException.class, () -> {
            new Sheet<>(rowKeys, columnKeys, invalidData);
        });
    }

    @Test
    public void testConstructorWithNullData() {
        Sheet<String, String, Integer> s = new Sheet<>(rowKeys, columnKeys, (Object[][]) null);
        assertNotNull(s);
        assertEquals(3, s.rowCount());
        assertEquals(3, s.columnCount());
        assertFalse(s.isEmpty());
    }

    @Test
    public void testEmpty() {
        Sheet<String, String, Integer> empty = Sheet.empty();
        assertNotNull(empty);
        assertTrue(empty.isEmpty());
        assertEquals(0, empty.rowCount());
        assertEquals(0, empty.columnCount());
        assertTrue(empty.isFrozen());
    }

    @Test
    public void testRowsWithObjectArray() {
        Sheet<String, String, Integer> s = Sheet.rows(rowKeys, columnKeys, sampleData);
        assertNotNull(s);
        assertEquals(3, s.rowCount());
        assertEquals(3, s.columnCount());
        assertEquals(Integer.valueOf(1), s.get("row1", "col1"));
        assertEquals(Integer.valueOf(5), s.get("row2", "col2"));
    }

    @Test
    public void testRowsWithCollections() {
        List<List<Integer>> rowData = Arrays.asList(Arrays.asList(1, 2, 3), Arrays.asList(4, 5, 6), Arrays.asList(7, 8, 9));
        Sheet<String, String, Integer> s = Sheet.rows(rowKeys, columnKeys, rowData);
        assertNotNull(s);
        assertEquals(3, s.rowCount());
        assertEquals(3, s.columnCount());
        assertEquals(Integer.valueOf(1), s.get("row1", "col1"));
        assertEquals(Integer.valueOf(9), s.get("row3", "col3"));
    }

    @Test
    public void testRowsWithCollectionsSizeMismatch() {
        List<List<Integer>> rowData = Arrays.asList(Arrays.asList(1, 2, 3), Arrays.asList(4, 5, 6));
        assertThrows(IllegalArgumentException.class, () -> {
            Sheet.rows(rowKeys, columnKeys, rowData);
        });
    }

    @Test
    public void testColumnsWithObjectArray() {
        Integer[][] columnData = new Integer[][] { { 1, 4, 7 }, { 2, 5, 8 }, { 3, 6, 9 } };
        Sheet<String, String, Integer> s = Sheet.columns(rowKeys, columnKeys, columnData);
        assertNotNull(s);
        assertEquals(3, s.rowCount());
        assertEquals(3, s.columnCount());
        assertEquals(Integer.valueOf(1), s.get("row1", "col1"));
        assertEquals(Integer.valueOf(5), s.get("row2", "col2"));
    }

    @Test
    public void testColumnsWithCollections() {
        List<List<Integer>> columnData = Arrays.asList(Arrays.asList(1, 4, 7), Arrays.asList(2, 5, 8), Arrays.asList(3, 6, 9));
        Sheet<String, String, Integer> s = Sheet.columns(rowKeys, columnKeys, columnData);
        assertNotNull(s);
        assertEquals(3, s.rowCount());
        assertEquals(3, s.columnCount());
        assertEquals(Integer.valueOf(1), s.get("row1", "col1"));
        assertEquals(Integer.valueOf(9), s.get("row3", "col3"));
    }

    @Test
    public void testColumnsWithCollectionsSizeMismatch() {
        List<List<Integer>> columnData = Arrays.asList(Arrays.asList(1, 4, 7), Arrays.asList(2, 5, 8));
        assertThrows(IllegalArgumentException.class, () -> {
            Sheet.columns(rowKeys, columnKeys, columnData);
        });
    }

    @Test
    public void testRowKeySet() {
        ImmutableSet<String> keys = sheet.rowKeySet();
        assertNotNull(keys);
        assertEquals(3, keys.size());
        assertTrue(keys.contains("row1"));
        assertTrue(keys.contains("row2"));
        assertTrue(keys.contains("row3"));
    }

    @Test
    public void testRowKeySetImmutable() {
        ImmutableSet<String> keys = sheet.rowKeySet();
        assertThrows(UnsupportedOperationException.class, () -> {
            keys.add("row4");
        });
    }

    @Test
    public void testColumnKeySet() {
        ImmutableSet<String> keys = sheet.columnKeySet();
        assertNotNull(keys);
        assertEquals(3, keys.size());
        assertTrue(keys.contains("col1"));
        assertTrue(keys.contains("col2"));
        assertTrue(keys.contains("col3"));
    }

    @Test
    public void testColumnKeySetImmutable() {
        ImmutableSet<String> keys = sheet.columnKeySet();
        assertThrows(UnsupportedOperationException.class, () -> {
            keys.add("col4");
        });
    }

    @Test
    public void testIsNullWithKeys() {
        assertFalse(sheet.isNull("row1", "col1"));
        assertFalse(sheet.isNull("row2", "col2"));
    }

    @Test
    public void testIsNullWithNullValue() {
        Sheet<String, String, Integer> s = Sheet.rows(Arrays.asList("r1", "r2"), Arrays.asList("c1", "c2"), new Integer[][] { { 1, null }, { null, 4 } });
        assertTrue(s.isNull("r1", "c2"));
        assertTrue(s.isNull("r2", "c1"));
        assertFalse(s.isNull("r1", "c1"));
    }

    @Test
    public void testIsNullWithInvalidKey() {
        assertThrows(IllegalArgumentException.class, () -> {
            sheet.isNull("invalidRow", "col1");
        });
    }

    @Test
    public void testIsNullWithIndices() {
        assertFalse(sheet.isNull(0, 0));
        assertFalse(sheet.isNull(1, 1));
    }

    @Test
    public void testIsNullWithInvalidIndices() {
        assertThrows(IndexOutOfBoundsException.class, () -> {
            sheet.isNull(-1, 0);
        });
        assertThrows(IndexOutOfBoundsException.class, () -> {
            sheet.isNull(0, 10);
        });
    }

    @Test
    public void testIsNullWithPoint() {
        Point point = Point.of(0, 0);
        assertFalse(sheet.isNull(point));

        Sheet<String, String, Integer> s = Sheet.rows(Arrays.asList("r1"), Arrays.asList("c1"), new Integer[][] { { null } });
        Point nullPoint = Point.of(0, 0);
        assertTrue(s.isNull(nullPoint));
    }

    @Test
    public void testIsNullUninitializedSheet() {
        Sheet<String, String, Integer> s = new Sheet<>(rowKeys, columnKeys);
        assertTrue(s.isNull("row1", "col1"));
        assertTrue(s.isNull(0, 0));
    }

    @Test
    public void testGetWithKeys() {
        assertEquals(Integer.valueOf(1), sheet.get("row1", "col1"));
        assertEquals(Integer.valueOf(5), sheet.get("row2", "col2"));
        assertEquals(Integer.valueOf(9), sheet.get("row3", "col3"));
    }

    @Test
    public void testGetWithInvalidKeys() {
        assertThrows(IllegalArgumentException.class, () -> {
            sheet.get("invalidRow", "col1");
        });
        assertThrows(IllegalArgumentException.class, () -> {
            sheet.get("row1", "invalidCol");
        });
    }

    @Test
    public void testGetWithIndices() {
        assertEquals(Integer.valueOf(1), sheet.get(0, 0));
        assertEquals(Integer.valueOf(5), sheet.get(1, 1));
        assertEquals(Integer.valueOf(9), sheet.get(2, 2));
    }

    @Test
    public void testGetWithInvalidIndices() {
        assertThrows(IndexOutOfBoundsException.class, () -> {
            sheet.get(-1, 0);
        });
        assertThrows(IndexOutOfBoundsException.class, () -> {
            sheet.get(0, 10);
        });
    }

    @Test
    public void testGetWithPoint() {
        Point point = Point.of(0, 0);
        assertEquals(Integer.valueOf(1), sheet.get(point));

        Point point2 = Point.of(1, 1);
        assertEquals(Integer.valueOf(5), sheet.get(point2));
    }

    @Test
    public void testGetUninitializedSheet() {
        Sheet<String, String, Integer> s = new Sheet<>(rowKeys, columnKeys);
        assertNull(s.get("row1", "col1"));
        assertNull(s.get(0, 0));
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
        Integer oldValue = sheet.set(0, 0, 100);
        assertEquals(Integer.valueOf(1), oldValue);
        assertEquals(Integer.valueOf(100), sheet.get(0, 0));
    }

    @Test
    public void testPutWithInvalidIndices() {
        assertThrows(IndexOutOfBoundsException.class, () -> {
            sheet.set(-1, 0, 100);
        });
        assertThrows(IndexOutOfBoundsException.class, () -> {
            sheet.set(0, 10, 100);
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
    public void testPutOnFrozenSheet() {
        sheet.freeze();
        assertThrows(IllegalStateException.class, () -> {
            sheet.set("row1", "col1", 100);
        });
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
    public void testPutAllOnFrozenSheet() {
        sheet.freeze();
        Sheet<String, String, Integer> source = Sheet.rows(Arrays.asList("row1"), Arrays.asList("col1"), new Integer[][] { { 100 } });

        assertThrows(IllegalStateException.class, () -> {
            sheet.putAll(source);
        });
    }

    @Test
    public void testRemoveWithKeys() {
        Integer removed = sheet.remove("row1", "col1");
        assertEquals(Integer.valueOf(1), removed);
        assertNull(sheet.get("row1", "col1"));
    }

    @Test
    public void testRemoveWithInvalidKeys() {
        assertThrows(IllegalArgumentException.class, () -> {
            sheet.remove("invalidRow", "col1");
        });
    }

    @Test
    public void testRemoveWithIndices() {
        Integer removed = sheet.remove(0, 0);
        assertEquals(Integer.valueOf(1), removed);
        assertNull(sheet.get(0, 0));
    }

    @Test
    public void testRemoveWithInvalidIndices() {
        assertThrows(IndexOutOfBoundsException.class, () -> {
            sheet.remove(-1, 0);
        });
        assertThrows(IndexOutOfBoundsException.class, () -> {
            sheet.remove(0, 10);
        });
    }

    @Test
    public void testRemoveWithPoint() {
        Point point = Point.of(0, 0);
        Integer removed = sheet.remove(point);
        assertEquals(Integer.valueOf(1), removed);
        assertNull(sheet.get(point));
    }

    @Test
    public void testRemoveOnFrozenSheet() {
        sheet.freeze();
        assertThrows(IllegalStateException.class, () -> {
            sheet.remove("row1", "col1");
        });
    }

    @Test
    public void testRemoveUninitializedSheet() {
        Sheet<String, String, Integer> s = new Sheet<>(rowKeys, columnKeys);
        assertNull(s.remove("row1", "col1"));
    }

    @Test
    public void testContains() {
        assertTrue(sheet.containsCell("row1", "col1"));
        assertTrue(sheet.containsCell("row2", "col2"));
        assertFalse(sheet.containsCell("invalidRow", "col1"));
        assertFalse(sheet.containsCell("row1", "invalidCol"));
    }

    @Test
    public void testContainsWithValue() {
        assertTrue(sheet.containsValueAt("row1", "col1", 1));
        assertTrue(sheet.containsValueAt("row2", "col2", 5));
        assertFalse(sheet.containsValueAt("row1", "col1", 100));
    }

    @Test
    public void testContainsWithNullValue() {
        Sheet<String, String, Integer> s = Sheet.rows(Arrays.asList("r1"), Arrays.asList("c1"), new Integer[][] { { null } });
        assertTrue(s.containsValueAt("r1", "c1", null));
        assertFalse(s.containsValueAt("r1", "c1", 1));
    }

    @Test
    public void testContainsValue() {
        assertTrue(sheet.containsValue(1));
        assertTrue(sheet.containsValue(5));
        assertTrue(sheet.containsValue(9));
        assertFalse(sheet.containsValue(100));
    }

    @Test
    public void testContainsValueNull() {
        Sheet<String, String, Integer> s = Sheet.rows(Arrays.asList("r1"), Arrays.asList("c1"), new Integer[][] { { null } });
        assertTrue(s.containsValue(null));
    }

    @Test
    public void testContainsValueUninitializedSheet() {
        Sheet<String, String, Integer> s = new Sheet<>(rowKeys, columnKeys);
        assertTrue(s.containsValue(null));
        assertFalse(s.containsValue(1));
    }

    @Test
    public void testGetRow() {
        ImmutableList<Integer> row = sheet.rowValues("row1");
        assertNotNull(row);
        assertEquals(3, row.size());
        assertEquals(Integer.valueOf(1), row.get(0));
        assertEquals(Integer.valueOf(2), row.get(1));
        assertEquals(Integer.valueOf(3), row.get(2));
    }

    @Test
    public void testGetRowInvalidKey() {
        assertThrows(IllegalArgumentException.class, () -> {
            sheet.rowValues("invalidRow");
        });
    }

    @Test
    public void testSetRow() {
        sheet.setRow("row1", Arrays.asList(10, 20, 30));
        assertEquals(Integer.valueOf(10), sheet.get("row1", "col1"));
        assertEquals(Integer.valueOf(20), sheet.get("row1", "col2"));
        assertEquals(Integer.valueOf(30), sheet.get("row1", "col3"));
    }

    @Test
    public void testSetRowEmptyCollection() {
        sheet.setRow("row1", Arrays.asList());
        assertNull(sheet.get("row1", "col1"));
        assertNull(sheet.get("row1", "col2"));
        assertNull(sheet.get("row1", "col3"));
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
    public void testAddRow() {
        sheet.addRow("row4", Arrays.asList(10, 11, 12));
        assertEquals(4, sheet.rowCount());
        assertEquals(Integer.valueOf(10), sheet.get("row4", "col1"));
        assertEquals(Integer.valueOf(11), sheet.get("row4", "col2"));
        assertEquals(Integer.valueOf(12), sheet.get("row4", "col3"));
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
    public void testAddRowEmptyCollection() {
        sheet.addRow("row4", Arrays.asList());
        assertEquals(4, sheet.rowCount());
        assertNull(sheet.get("row4", "col1"));
        assertNull(sheet.get("row4", "col2"));
        assertNull(sheet.get("row4", "col3"));
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
    public void testAddRowAtIndexInvalid() {
        assertThrows(IndexOutOfBoundsException.class, () -> {
            sheet.addRow(-1, "row0", Arrays.asList(10, 11, 12));
        });
        assertThrows(IndexOutOfBoundsException.class, () -> {
            sheet.addRow(10, "row10", Arrays.asList(10, 11, 12));
        });
    }

    @Test
    public void testUpdateRow() {
        sheet.updateRow("row1", v -> v == null ? 0 : v * 10);
        assertEquals(Integer.valueOf(10), sheet.get("row1", "col1"));
        assertEquals(Integer.valueOf(20), sheet.get("row1", "col2"));
        assertEquals(Integer.valueOf(30), sheet.get("row1", "col3"));
    }

    @Test
    public void testUpdateRowInvalidKey() {
        assertThrows(IllegalArgumentException.class, () -> {
            sheet.updateRow("invalidRow", v -> v);
        });
    }

    @Test
    public void testUpdateRowOnFrozenSheet() {
        sheet.freeze();
        assertThrows(IllegalStateException.class, () -> {
            sheet.updateRow("row1", v -> v);
        });
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
    public void testMoveRow() {
        sheet.moveRow("row3", 0);

        List<String> expectedOrder = Arrays.asList("row3", "row1", "row2");
        List<String> actualOrder = new ArrayList<>(sheet.rowKeySet());
        assertEquals(expectedOrder, actualOrder);

        assertEquals(Integer.valueOf(7), sheet.get(0, 0));
        assertEquals(Integer.valueOf(1), sheet.get(1, 0));
    }

    @Test
    public void testMoveRowInvalidIndex() {
        assertThrows(IndexOutOfBoundsException.class, () -> {
            sheet.moveRow("row1", -1);
        });
        assertThrows(IndexOutOfBoundsException.class, () -> {
            sheet.moveRow("row1", 10);
        });
    }

    @Test
    public void testMoveRowOnFrozenSheet() {
        sheet.freeze();
        assertThrows(IllegalStateException.class, () -> {
            sheet.moveRow("row1", 2);
        });
    }

    @Test
    public void testSwapRowPosition() {
        sheet.swapRows("row1", "row3");

        List<String> expectedOrder = Arrays.asList("row3", "row2", "row1");
        List<String> actualOrder = new ArrayList<>(sheet.rowKeySet());
        assertEquals(expectedOrder, actualOrder);

        assertEquals(Integer.valueOf(7), sheet.get(0, 0));
        assertEquals(Integer.valueOf(1), sheet.get(2, 0));
    }

    @Test
    public void testSwapRowPositionInvalidKey() {
        assertThrows(IllegalArgumentException.class, () -> {
            sheet.swapRows("row1", "invalidRow");
        });
    }

    @Test
    public void testSwapRowPositionOnFrozenSheet() {
        sheet.freeze();
        assertThrows(IllegalStateException.class, () -> {
            sheet.swapRows("row1", "row2");
        });
    }

    @Test
    public void testRenameRow() {
        sheet.renameRow("row1", "renamedRow");
        assertTrue(sheet.containsRow("renamedRow"));
        assertFalse(sheet.containsRow("row1"));
        assertEquals(Integer.valueOf(1), sheet.get("renamedRow", "col1"));
    }

    @Test
    public void testRenameRowToExistingKey() {
        assertThrows(IllegalArgumentException.class, () -> {
            sheet.renameRow("row1", "row2");
        });
    }

    @Test
    public void testRenameRowInvalidKey() {
        assertThrows(IllegalArgumentException.class, () -> {
            sheet.renameRow("invalidRow", "newRow");
        });
    }

    @Test
    public void testRenameRowOnFrozenSheet() {
        sheet.freeze();
        assertThrows(IllegalStateException.class, () -> {
            sheet.renameRow("row1", "newRow");
        });
    }

    @Test
    public void testContainsRow() {
        assertTrue(sheet.containsRow("row1"));
        assertTrue(sheet.containsRow("row2"));
        assertFalse(sheet.containsRow("invalidRow"));
    }

    @Test
    public void testRow() {
        Map<String, Integer> row = sheet.rowAsMap("row1");
        assertNotNull(row);
        assertEquals(3, row.size());
        assertEquals(Integer.valueOf(1), row.get("col1"));
        assertEquals(Integer.valueOf(2), row.get("col2"));
        assertEquals(Integer.valueOf(3), row.get("col3"));
    }

    @Test
    public void testRowInvalidKey() {
        assertThrows(IllegalArgumentException.class, () -> {
            sheet.rowAsMap("invalidRow");
        });
    }

    @Test
    public void testRowMap() {
        Map<String, Map<String, Integer>> rowMap = sheet.rowsMap();
        assertNotNull(rowMap);
        assertEquals(3, rowMap.size());
        assertEquals(Integer.valueOf(1), rowMap.get("row1").get("col1"));
        assertEquals(Integer.valueOf(5), rowMap.get("row2").get("col2"));
        assertEquals(Integer.valueOf(9), rowMap.get("row3").get("col3"));
    }

    @Test
    public void testGetColumn() {
        ImmutableList<Integer> column = sheet.columnValues("col1");
        assertNotNull(column);
        assertEquals(3, column.size());
        assertEquals(Integer.valueOf(1), column.get(0));
        assertEquals(Integer.valueOf(4), column.get(1));
        assertEquals(Integer.valueOf(7), column.get(2));
    }

    @Test
    public void testGetColumnInvalidKey() {
        assertThrows(IllegalArgumentException.class, () -> {
            sheet.columnValues("invalidCol");
        });
    }

    @Test
    public void testSetColumn() {
        sheet.setColumn("col1", Arrays.asList(10, 20, 30));
        assertEquals(Integer.valueOf(10), sheet.get("row1", "col1"));
        assertEquals(Integer.valueOf(20), sheet.get("row2", "col1"));
        assertEquals(Integer.valueOf(30), sheet.get("row3", "col1"));
    }

    @Test
    public void testSetColumnEmptyCollection() {
        sheet.setColumn("col1", Arrays.asList());
        assertNull(sheet.get("row1", "col1"));
        assertNull(sheet.get("row2", "col1"));
        assertNull(sheet.get("row3", "col1"));
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
    public void testAddColumn() {
        sheet.addColumn("col4", Arrays.asList(10, 11, 12));
        assertEquals(4, sheet.columnCount());
        assertEquals(Integer.valueOf(10), sheet.get("row1", "col4"));
        assertEquals(Integer.valueOf(11), sheet.get("row2", "col4"));
        assertEquals(Integer.valueOf(12), sheet.get("row3", "col4"));
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
    public void testAddColumnEmptyCollection() {
        sheet.addColumn("col4", Arrays.asList());
        assertEquals(4, sheet.columnCount());
        assertNull(sheet.get("row1", "col4"));
        assertNull(sheet.get("row2", "col4"));
        assertNull(sheet.get("row3", "col4"));
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
    public void testAddColumnAtIndexInvalid() {
        assertThrows(IndexOutOfBoundsException.class, () -> {
            sheet.addColumn(-1, "col0", Arrays.asList(10, 11, 12));
        });
        assertThrows(IndexOutOfBoundsException.class, () -> {
            sheet.addColumn(10, "col10", Arrays.asList(10, 11, 12));
        });
    }

    @Test
    public void testUpdateColumn() {
        sheet.updateColumn("col1", v -> v == null ? 0 : v * 10);
        assertEquals(Integer.valueOf(10), sheet.get("row1", "col1"));
        assertEquals(Integer.valueOf(40), sheet.get("row2", "col1"));
        assertEquals(Integer.valueOf(70), sheet.get("row3", "col1"));
    }

    @Test
    public void testUpdateColumnInvalidKey() {
        assertThrows(IllegalArgumentException.class, () -> {
            sheet.updateColumn("invalidCol", v -> v);
        });
    }

    @Test
    public void testUpdateColumnOnFrozenSheet() {
        sheet.freeze();
        assertThrows(IllegalStateException.class, () -> {
            sheet.updateColumn("col1", v -> v);
        });
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
    public void testMoveColumn() {
        sheet.moveColumn("col3", 0);

        List<String> expectedOrder = Arrays.asList("col3", "col1", "col2");
        List<String> actualOrder = new ArrayList<>(sheet.columnKeySet());
        assertEquals(expectedOrder, actualOrder);

        assertEquals(Integer.valueOf(3), sheet.get(0, 0));
        assertEquals(Integer.valueOf(1), sheet.get(0, 1));
    }

    @Test
    public void testMoveColumnInvalidIndex() {
        assertThrows(IndexOutOfBoundsException.class, () -> {
            sheet.moveColumn("col1", -1);
        });
        assertThrows(IndexOutOfBoundsException.class, () -> {
            sheet.moveColumn("col1", 10);
        });
    }

    @Test
    public void testMoveColumnOnFrozenSheet() {
        sheet.freeze();
        assertThrows(IllegalStateException.class, () -> {
            sheet.moveColumn("col1", 2);
        });
    }

    @Test
    public void testSwapColumnPosition() {
        sheet.swapColumns("col1", "col3");

        List<String> expectedOrder = Arrays.asList("col3", "col2", "col1");
        List<String> actualOrder = new ArrayList<>(sheet.columnKeySet());
        assertEquals(expectedOrder, actualOrder);

        assertEquals(Integer.valueOf(3), sheet.get(0, 0));
        assertEquals(Integer.valueOf(1), sheet.get(0, 2));
    }

    @Test
    public void testSwapColumnPositionInvalidKey() {
        assertThrows(IllegalArgumentException.class, () -> {
            sheet.swapColumns("col1", "invalidCol");
        });
    }

    @Test
    public void testSwapColumnPositionOnFrozenSheet() {
        sheet.freeze();
        assertThrows(IllegalStateException.class, () -> {
            sheet.swapColumns("col1", "col2");
        });
    }

    @Test
    public void testRenameColumn() {
        sheet.renameColumn("col1", "renamedCol");
        assertTrue(sheet.containsColumn("renamedCol"));
        assertFalse(sheet.containsColumn("col1"));
        assertEquals(Integer.valueOf(1), sheet.get("row1", "renamedCol"));
    }

    @Test
    public void testRenameColumnToExistingKey() {
        assertThrows(IllegalArgumentException.class, () -> {
            sheet.renameColumn("col1", "col2");
        });
    }

    @Test
    public void testRenameColumnInvalidKey() {
        assertThrows(IllegalArgumentException.class, () -> {
            sheet.renameColumn("invalidCol", "newCol");
        });
    }

    @Test
    public void testRenameColumnOnFrozenSheet() {
        sheet.freeze();
        assertThrows(IllegalStateException.class, () -> {
            sheet.renameColumn("col1", "newCol");
        });
    }

    @Test
    public void testContainsColumn() {
        assertTrue(sheet.containsColumn("col1"));
        assertTrue(sheet.containsColumn("col2"));
        assertFalse(sheet.containsColumn("invalidCol"));
    }

    @Test
    public void testColumn() {
        Map<String, Integer> column = sheet.columnAsMap("col1");
        assertNotNull(column);
        assertEquals(3, column.size());
        assertEquals(Integer.valueOf(1), column.get("row1"));
        assertEquals(Integer.valueOf(4), column.get("row2"));
        assertEquals(Integer.valueOf(7), column.get("row3"));
    }

    @Test
    public void testColumnInvalidKey() {
        assertThrows(IllegalArgumentException.class, () -> {
            sheet.columnAsMap("invalidCol");
        });
    }

    @Test
    public void testColumnMap() {
        Map<String, Map<String, Integer>> columnMap = sheet.columnsMap();
        assertNotNull(columnMap);
        assertEquals(3, columnMap.size());
        assertEquals(Integer.valueOf(1), columnMap.get("col1").get("row1"));
        assertEquals(Integer.valueOf(5), columnMap.get("col2").get("row2"));
        assertEquals(Integer.valueOf(9), columnMap.get("col3").get("row3"));
    }

    @Test
    public void testRowLength() {
        assertEquals(3, sheet.rowCount());
        assertEquals(0, emptySheet.rowCount());
    }

    @Test
    public void testColumnLength() {
        assertEquals(3, sheet.columnCount());
        assertEquals(0, emptySheet.columnCount());
    }

    @Test
    public void testUpdateAll() {
        sheet.updateAll(v -> v == null ? 0 : v * 10);
        assertEquals(Integer.valueOf(10), sheet.get("row1", "col1"));
        assertEquals(Integer.valueOf(50), sheet.get("row2", "col2"));
        assertEquals(Integer.valueOf(90), sheet.get("row3", "col3"));
    }

    @Test
    public void testUpdateAllOnFrozenSheet() {
        sheet.freeze();
        assertThrows(IllegalStateException.class, () -> {
            sheet.updateAll(v -> v);
        });
    }

    @Test
    public void testUpdateAllWithIntBiFunction() {
        sheet.updateAll((rowIdx, colIdx) -> rowIdx * 10 + colIdx);
        assertEquals(Integer.valueOf(0), sheet.get(0, 0));
        assertEquals(Integer.valueOf(11), sheet.get(1, 1));
        assertEquals(Integer.valueOf(22), sheet.get(2, 2));
    }

    @Test
    public void testUpdateAllWithTriFunction() {
        sheet.updateAll((rowKey, colKey, value) -> {
            if (rowKey.equals("row1")) {
                return value * 100;
            }
            return value;
        });
        assertEquals(Integer.valueOf(100), sheet.get("row1", "col1"));
        assertEquals(Integer.valueOf(200), sheet.get("row1", "col2"));
        assertEquals(Integer.valueOf(300), sheet.get("row1", "col3"));
        assertEquals(Integer.valueOf(4), sheet.get("row2", "col1"));
    }

    @Test
    public void testReplaceIfWithPredicate() {
        sheet.replaceIf(v -> v != null && v > 5, 0);
        assertEquals(Integer.valueOf(1), sheet.get("row1", "col1"));
        assertEquals(Integer.valueOf(0), sheet.get("row3", "col1"));
        assertEquals(Integer.valueOf(0), sheet.get("row3", "col3"));
    }

    @Test
    public void testReplaceIfWithIntBiPredicate() {
        sheet.replaceIf((rowIdx, colIdx) -> rowIdx == colIdx, 0);
        assertEquals(Integer.valueOf(0), sheet.get(0, 0));
        assertEquals(Integer.valueOf(0), sheet.get(1, 1));
        assertEquals(Integer.valueOf(0), sheet.get(2, 2));
        assertEquals(Integer.valueOf(2), sheet.get(0, 1));
    }

    @Test
    public void testReplaceIfWithTriPredicate() {
        sheet.replaceIf((rowKey, colKey, value) -> rowKey.equals("row1") && value != null && value < 3, 100);
        assertEquals(Integer.valueOf(100), sheet.get("row1", "col1"));
        assertEquals(Integer.valueOf(100), sheet.get("row1", "col2"));
        assertEquals(Integer.valueOf(3), sheet.get("row1", "col3"));
        assertEquals(Integer.valueOf(4), sheet.get("row2", "col1"));
    }

    @Test
    public void testReplaceIfOnFrozenSheet() {
        sheet.freeze();
        assertThrows(IllegalStateException.class, () -> {
            sheet.replaceIf(v -> true, 0);
        });
    }

    @Test
    public void testSortByRowKey() {
        Sheet<String, String, Integer> unsorted = Sheet.rows(Arrays.asList("c", "a", "b"), Arrays.asList("col1"), new Integer[][] { { 1 }, { 2 }, { 3 } });

        unsorted.sortByRowKey();

        List<String> expectedOrder = Arrays.asList("a", "b", "c");
        List<String> actualOrder = new ArrayList<>(unsorted.rowKeySet());
        assertEquals(expectedOrder, actualOrder);
    }

    @Test
    public void testSortByRowKeyWithComparator() {
        Sheet<String, String, Integer> unsorted = Sheet.rows(Arrays.asList("c", "a", "b"), Arrays.asList("col1"), new Integer[][] { { 1 }, { 2 }, { 3 } });

        unsorted.sortByRowKey(Comparator.reverseOrder());

        List<String> expectedOrder = Arrays.asList("c", "b", "a");
        List<String> actualOrder = new ArrayList<>(unsorted.rowKeySet());
        assertEquals(expectedOrder, actualOrder);
    }

    @Test
    public void testSortByRowKeyOnFrozenSheet() {
        sheet.freeze();
        assertThrows(IllegalStateException.class, () -> {
            sheet.sortByRowKey();
        });
    }

    @Test
    public void testSortByRow() {
        Sheet<String, String, Integer> s = Sheet.rows(Arrays.asList("row1", "row2", "row3"), Arrays.asList("col1", "col2", "col3"),
                new Integer[][] { { 3, 1, 2 }, { 6, 4, 5 }, { 9, 7, 8 } });

        s.sortColumnsByRowValues("row1", Comparator.naturalOrder());

        assertEquals(Integer.valueOf(1), s.get("row1", "col2"));
        assertEquals(Integer.valueOf(2), s.get("row1", "col3"));
        assertEquals(Integer.valueOf(3), s.get("row1", "col1"));
    }

    @Test
    public void testSortByRowOnFrozenSheet() {
        sheet.freeze();
        assertThrows(IllegalStateException.class, () -> {
            sheet.sortColumnsByRowValues("row1", Comparator.naturalOrder());
        });
    }

    @Test
    public void testSortByRows() {
        Sheet<String, String, Integer> s = Sheet.rows(Arrays.asList("row1", "row2", "row3"), Arrays.asList("col1", "col2", "col3"),
                new Integer[][] { { 3, 1, 2 }, { 6, 4, 5 }, { 9, 7, 8 } });

        s.sortColumnsByRowValues(Arrays.asList("row1", "row2"), Comparator.comparing((Object[] arr) -> (Integer) arr[0]));

        assertNotNull(s.get("row1", "col1"));
    }

    @Test
    public void testSortByColumnKey() {
        Sheet<String, String, Integer> unsorted = Sheet.rows(Arrays.asList("row1"), Arrays.asList("c", "a", "b"), new Integer[][] { { 1, 2, 3 } });

        unsorted.sortByColumnKey();

        List<String> expectedOrder = Arrays.asList("a", "b", "c");
        List<String> actualOrder = new ArrayList<>(unsorted.columnKeySet());
        assertEquals(expectedOrder, actualOrder);
    }

    @Test
    public void testSortByColumnKeyWithComparator() {
        Sheet<String, String, Integer> unsorted = Sheet.rows(Arrays.asList("row1"), Arrays.asList("c", "a", "b"), new Integer[][] { { 1, 2, 3 } });

        unsorted.sortByColumnKey(Comparator.reverseOrder());

        List<String> expectedOrder = Arrays.asList("c", "b", "a");
        List<String> actualOrder = new ArrayList<>(unsorted.columnKeySet());
        assertEquals(expectedOrder, actualOrder);
    }

    @Test
    public void testSortByColumnKeyOnFrozenSheet() {
        sheet.freeze();
        assertThrows(IllegalStateException.class, () -> {
            sheet.sortByColumnKey();
        });
    }

    @Test
    public void testSortByColumn() {
        Sheet<String, String, Integer> s = Sheet.rows(Arrays.asList("row1", "row2", "row3"), Arrays.asList("col1", "col2", "col3"),
                new Integer[][] { { 3, 2, 1 }, { 6, 5, 4 }, { 9, 8, 7 } });

        s.sortRowsByColumnValues("col1", Comparator.naturalOrder());

        assertEquals(Integer.valueOf(3), s.get("row1", "col1"));
    }

    @Test
    public void testSortByColumnOnFrozenSheet() {
        sheet.freeze();
        assertThrows(IllegalStateException.class, () -> {
            sheet.sortRowsByColumnValues("col1", Comparator.naturalOrder());
        });
    }

    @Test
    public void testSortByColumns() {
        Sheet<String, String, Integer> s = Sheet.rows(Arrays.asList("row1", "row2", "row3"), Arrays.asList("col1", "col2", "col3"),
                new Integer[][] { { 3, 2, 1 }, { 6, 5, 4 }, { 9, 8, 7 } });

        s.sortRowsByColumnValues(Arrays.asList("col1", "col2"), Comparator.comparing((Object[] arr) -> (Integer) arr[0]));

        assertNotNull(s.get("row1", "col1"));
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
    public void testClone() {
        Sheet<String, String, Integer> clone = sheet.clone();
        assertNotNull(clone);
        assertEquals(sheet.rowCount(), clone.rowCount());
        assertEquals(sheet.columnCount(), clone.columnCount());
        assertEquals(sheet.get("row1", "col1"), clone.get("row1", "col1"));
        assertFalse(clone.isFrozen());
    }

    @Test
    public void testCloneWithFreeze() {
        Sheet<String, String, Integer> clone = sheet.clone(true);
        assertNotNull(clone);
        assertTrue(clone.isFrozen());
        assertThrows(IllegalStateException.class, () -> {
            clone.set("row1", "col1", 999);
        });
    }

    @Test
    public void testMerge() {
        Sheet<String, String, String> sheet1 = Sheet.rows(Arrays.asList("row1", "row2"), Arrays.asList("col1", "col2"),
                new String[][] { { "a", "b" }, { "c", "d" } });
        Sheet<String, String, Integer> sheet2 = Sheet.rows(Arrays.asList("row1", "row2"), Arrays.asList("col1", "col2"),
                new Integer[][] { { 1, 2 }, { 3, 4 } });

        Sheet<String, String, String> merged = sheet1.merge(sheet2, (s, i) -> s + i);

        assertEquals("a1", merged.get("row1", "col1"));
        assertEquals("b2", merged.get("row1", "col2"));
        assertEquals("c3", merged.get("row2", "col1"));
        assertEquals("d4", merged.get("row2", "col2"));
    }

    @Test
    public void testTranspose() {
        Sheet<String, String, Integer> transposed = sheet.transpose();
        assertNotNull(transposed);
        assertEquals(sheet.columnCount(), transposed.rowCount());
        assertEquals(sheet.rowCount(), transposed.columnCount());

        assertEquals(Integer.valueOf(1), transposed.get("col1", "row1"));
        assertEquals(Integer.valueOf(5), transposed.get("col2", "row2"));
        assertEquals(Integer.valueOf(9), transposed.get("col3", "row3"));
    }

    @Test
    public void testFreeze() {
        assertFalse(sheet.isFrozen());
        sheet.freeze();
        assertTrue(sheet.isFrozen());
    }

    @Test
    public void testIsFrozen() {
        assertFalse(sheet.isFrozen());
        sheet.freeze();
        assertTrue(sheet.isFrozen());
    }

    @Test
    public void testFreezePreventsMutations() {
        sheet.freeze();
        assertThrows(IllegalStateException.class, () -> sheet.set("row1", "col1", 100));
        assertThrows(IllegalStateException.class, () -> sheet.remove("row1", "col1"));
        assertThrows(IllegalStateException.class, () -> sheet.addRow("row4", Arrays.asList(1, 2, 3)));
        assertThrows(IllegalStateException.class, () -> sheet.addColumn("col4", Arrays.asList(1, 2, 3)));
    }

    @Test
    public void testClear() {
        sheet.clear();
        assertEquals(3, sheet.rowCount());
        assertEquals(3, sheet.columnCount());
        assertFalse(sheet.isEmpty());
        assertNull(sheet.get("row1", "col1"));
        assertNull(sheet.get("row2", "col2"));
        assertNull(sheet.get("row3", "col3"));
    }

    @Test
    public void testClearOnFrozenSheet() {
        sheet.freeze();
        assertThrows(IllegalStateException.class, () -> {
            sheet.clear();
        });
    }

    @Test
    public void testTrimToSize() {
        sheet.trimToSize();
        assertEquals(3, sheet.rowCount());
        assertEquals(3, sheet.columnCount());
    }

    @Test
    public void testCountOfNonNullValue() {
        assertEquals(9, sheet.countOfNonNullValues());

        Sheet<String, String, Integer> withNulls = Sheet.rows(Arrays.asList("r1", "r2"), Arrays.asList("c1", "c2"),
                new Integer[][] { { 1, null }, { null, 4 } });
        assertEquals(2, withNulls.countOfNonNullValues());
    }

    @Test
    public void testIsEmpty() {
        assertFalse(sheet.isEmpty());
        assertTrue(emptySheet.isEmpty());

        Sheet<String, String, Integer> s = new Sheet<>(rowKeys, columnKeys);
        assertFalse(s.isEmpty());

        Sheet<String, String, Integer> noRows = new Sheet<>(Arrays.asList(), columnKeys);
        assertTrue(noRows.isEmpty());

        Sheet<String, String, Integer> noCols = new Sheet<>(rowKeys, Arrays.asList());
        assertTrue(noCols.isEmpty());
    }

    @Test
    public void testForEachH() {
        List<Integer> collected = new ArrayList<>();
        sheet.forEachH((r, c, v) -> {
            if (v != null) {
                collected.add(v);
            }
        });
        assertEquals(9, collected.size());
        assertEquals(Integer.valueOf(1), collected.get(0));
    }

    @Test
    public void testForEachV() {
        List<Integer> collected = new ArrayList<>();
        sheet.forEachV((r, c, v) -> {
            if (v != null) {
                collected.add(v);
            }
        });
        assertEquals(9, collected.size());
        assertEquals(Integer.valueOf(1), collected.get(0));
    }

    @Test
    public void testForEachNonNullH() {
        List<Integer> collected = new ArrayList<>();
        sheet.forEachNonNullH((r, c, v) -> collected.add(v));
        assertEquals(9, collected.size());
    }

    @Test
    public void testForEachNonNullV() {
        List<Integer> collected = new ArrayList<>();
        sheet.forEachNonNullV((r, c, v) -> collected.add(v));
        assertEquals(9, collected.size());
    }

    @Test
    public void testCellsH() {
        Stream<Cell<String, String, Integer>> cells = sheet.cellsH();
        List<Cell<String, String, Integer>> cellList = cells.toList();
        assertEquals(9, cellList.size());
        assertEquals(Integer.valueOf(1), cellList.get(0).value());
        assertEquals("row1", cellList.get(0).rowKey());
        assertEquals("col1", cellList.get(0).columnKey());
    }

    @Test
    public void testCellsHWithRange() {
        Stream<Cell<String, String, Integer>> cells = sheet.cellsH(0, 2);
        List<Cell<String, String, Integer>> cellList = cells.toList();
        assertEquals(6, cellList.size());
    }

    @Test
    public void testCellsHWithInvalidRange() {
        assertThrows(IndexOutOfBoundsException.class, () -> {
            sheet.cellsH(-1, 2);
        });
        assertThrows(IndexOutOfBoundsException.class, () -> {
            sheet.cellsH(0, 10);
        });
    }

    @Test
    public void testCellsV() {
        Stream<Cell<String, String, Integer>> cells = sheet.cellsV();
        List<Cell<String, String, Integer>> cellList = cells.toList();
        assertEquals(9, cellList.size());
        assertEquals(Integer.valueOf(1), cellList.get(0).value());
        assertEquals("row1", cellList.get(0).rowKey());
        assertEquals("col1", cellList.get(0).columnKey());
    }

    @Test
    public void testCellsVWithRange() {
        Stream<Cell<String, String, Integer>> cells = sheet.cellsV(0, 2);
        List<Cell<String, String, Integer>> cellList = cells.toList();
        assertEquals(6, cellList.size());
    }

    @Test
    public void testCellsR() {
        Stream<Stream<Cell<String, String, Integer>>> cellsR = sheet.cellsR();
        List<List<Cell<String, String, Integer>>> result = cellsR.map(Stream::toList).toList();
        assertEquals(3, result.size());
        assertEquals(3, result.get(0).size());
    }

    @Test
    public void testCellsRWithRange() {
        Stream<Stream<Cell<String, String, Integer>>> cellsR = sheet.cellsR(0, 2);
        List<List<Cell<String, String, Integer>>> result = cellsR.map(Stream::toList).toList();
        assertEquals(2, result.size());
    }

    @Test
    public void testCellsC() {
        Stream<Stream<Cell<String, String, Integer>>> cellsC = sheet.cellsC();
        List<List<Cell<String, String, Integer>>> result = cellsC.map(Stream::toList).toList();
        assertEquals(3, result.size());
        assertEquals(3, result.get(0).size());
    }

    @Test
    public void testCellsCWithRange() {
        Stream<Stream<Cell<String, String, Integer>>> cellsC = sheet.cellsC(0, 2);
        List<List<Cell<String, String, Integer>>> result = cellsC.map(Stream::toList).toList();
        assertEquals(2, result.size());
    }

    @Test
    public void testPointsH() {
        Stream<Point> points = sheet.pointsH();
        List<Point> pointList = points.toList();
        assertEquals(9, pointList.size());
        assertEquals(0, pointList.get(0).rowIndex());
        assertEquals(0, pointList.get(0).columnIndex());
    }

    @Test
    public void testPointsHWithRange() {
        Stream<Point> points = sheet.pointsH(0, 2);
        List<Point> pointList = points.toList();
        assertEquals(6, pointList.size());
    }

    @Test
    public void testPointsV() {
        Stream<Point> points = sheet.pointsV();
        List<Point> pointList = points.toList();
        assertEquals(9, pointList.size());
        assertEquals(0, pointList.get(0).rowIndex());
        assertEquals(0, pointList.get(0).columnIndex());
    }

    @Test
    public void testPointsVWithRange() {
        Stream<Point> points = sheet.pointsV(0, 2);
        List<Point> pointList = points.toList();
        assertEquals(6, pointList.size());
    }

    @Test
    public void testPointsR() {
        Stream<Stream<Point>> pointsR = sheet.pointsR();
        List<List<Point>> result = pointsR.map(Stream::toList).toList();
        assertEquals(3, result.size());
        assertEquals(3, result.get(0).size());
    }

    @Test
    public void testPointsRWithRange() {
        Stream<Stream<Point>> pointsR = sheet.pointsR(0, 2);
        List<List<Point>> result = pointsR.map(Stream::toList).toList();
        assertEquals(2, result.size());
    }

    @Test
    public void testPointsC() {
        Stream<Stream<Point>> pointsC = sheet.pointsC();
        List<List<Point>> result = pointsC.map(Stream::toList).toList();
        assertEquals(3, result.size());
        assertEquals(3, result.get(0).size());
    }

    @Test
    public void testPointsCWithRange() {
        Stream<Stream<Point>> pointsC = sheet.pointsC(0, 2);
        List<List<Point>> result = pointsC.map(Stream::toList).toList();
        assertEquals(2, result.size());
    }

    @Test
    public void testStreamH() {
        Stream<Integer> stream = sheet.streamH();
        List<Integer> values = stream.toList();
        assertEquals(9, values.size());
        assertEquals(Integer.valueOf(1), values.get(0));
    }

    @Test
    public void testStreamHWithRange() {
        Stream<Integer> stream = sheet.streamH(0, 2);
        List<Integer> values = stream.toList();
        assertEquals(6, values.size());
    }

    @Test
    public void testStreamV() {
        Stream<Integer> stream = sheet.streamV();
        List<Integer> values = stream.toList();
        assertEquals(9, values.size());
        assertEquals(Integer.valueOf(1), values.get(0));
    }

    @Test
    public void testStreamVWithRange() {
        Stream<Integer> stream = sheet.streamV(0, 2);
        List<Integer> values = stream.toList();
        assertEquals(6, values.size());
    }

    @Test
    public void testStreamR() {
        Stream<Stream<Integer>> streamR = sheet.streamR();
        List<List<Integer>> result = streamR.map(Stream::toList).toList();
        assertEquals(3, result.size());
        assertEquals(3, result.get(0).size());
        assertEquals(Integer.valueOf(1), result.get(0).get(0));
    }

    @Test
    public void testStreamRWithRange() {
        Stream<Stream<Integer>> streamR = sheet.streamR(0, 2);
        List<List<Integer>> result = streamR.map(Stream::toList).toList();
        assertEquals(2, result.size());
    }

    @Test
    public void testStreamC() {
        Stream<Stream<Integer>> streamC = sheet.streamC();
        List<List<Integer>> result = streamC.map(Stream::toList).toList();
        assertEquals(3, result.size());
        assertEquals(3, result.get(0).size());
        assertEquals(Integer.valueOf(1), result.get(0).get(0));
    }

    @Test
    public void testStreamCWithRange() {
        Stream<Stream<Integer>> streamC = sheet.streamC(0, 2);
        List<List<Integer>> result = streamC.map(Stream::toList).toList();
        assertEquals(2, result.size());
    }

    @Test
    public void testRows() {
        Stream<Pair<String, Stream<Integer>>> rows = sheet.rows();
        List<Pair<String, Stream<Integer>>> rowList = rows.toList();
        assertEquals(3, rowList.size());
        assertEquals("row1", rowList.get(0).left());
        List<Integer> row1Values = rowList.get(0).right().toList();
        assertEquals(Arrays.asList(1, 2, 3), row1Values);
    }

    @Test
    public void testRowsWithRange() {
        Stream<Pair<String, Stream<Integer>>> rows = sheet.rows(0, 2);
        List<Pair<String, Stream<Integer>>> rowList = rows.toList();
        assertEquals(2, rowList.size());
        assertEquals("row1", rowList.get(0).left());
        assertEquals("row2", rowList.get(1).left());
    }

    @Test
    public void testRowsWithMapper() {
        Stream<Pair<String, Integer>> rows = sheet.rows((idx, arr) -> {
            int sum = 0;
            for (int i = 0; i < arr.length(); i++) {
                sum += (Integer) arr.get(i);
            }
            return sum;
        });
        List<Pair<String, Integer>> rowList = rows.toList();
        assertEquals(3, rowList.size());
        assertEquals("row1", rowList.get(0).left());
        assertEquals(Integer.valueOf(6), rowList.get(0).right());
    }

    @Test
    public void testRowsWithRangeAndMapper() {
        Stream<Pair<String, Integer>> rows = sheet.rows(0, 2, (idx, arr) -> {
            int sum = 0;
            for (int i = 0; i < arr.length(); i++) {
                sum += (Integer) arr.get(i);
            }
            return sum;
        });
        List<Pair<String, Integer>> rowList = rows.toList();
        assertEquals(2, rowList.size());
    }

    @Test
    public void testColumns() {
        Stream<Pair<String, Stream<Integer>>> columns = sheet.columns();
        List<Pair<String, Stream<Integer>>> columnList = columns.toList();
        assertEquals(3, columnList.size());
        assertEquals("col1", columnList.get(0).left());
        List<Integer> col1Values = columnList.get(0).right().toList();
        assertEquals(Arrays.asList(1, 4, 7), col1Values);
    }

    @Test
    public void testColumnsWithRange() {
        Stream<Pair<String, Stream<Integer>>> columns = sheet.columns(0, 2);
        List<Pair<String, Stream<Integer>>> columnList = columns.toList();
        assertEquals(2, columnList.size());
        assertEquals("col1", columnList.get(0).left());
        assertEquals("col2", columnList.get(1).left());
    }

    @Test
    public void testColumnsWithMapper() {
        Stream<Pair<String, Integer>> columns = sheet.columns((idx, arr) -> {
            int sum = 0;
            for (int i = 0; i < arr.length(); i++) {
                sum += (Integer) arr.get(i);
            }
            return sum;
        });
        List<Pair<String, Integer>> columnList = columns.toList();
        assertEquals(3, columnList.size());
        assertEquals("col1", columnList.get(0).left());
        assertEquals(Integer.valueOf(12), columnList.get(0).right());
    }

    @Test
    public void testColumnsWithRangeAndMapper() {
        Stream<Pair<String, Integer>> columns = sheet.columns(0, 2, (idx, arr) -> {
            int sum = 0;
            for (int i = 0; i < arr.length(); i++) {
                sum += (Integer) arr.get(i);
            }
            return sum;
        });
        List<Pair<String, Integer>> columnList = columns.toList();
        assertEquals(2, columnList.size());
    }

    @Test
    public void testToDatasetH() {
        Dataset ds = sheet.toDatasetH();
        assertNotNull(ds);
        assertEquals(3, ds.size());
        assertEquals(3, ds.columnCount());
    }

    @Test
    public void testToDatasetV() {
        Dataset ds = sheet.toDatasetV();
        assertNotNull(ds);
        assertEquals(3, ds.size());
        assertEquals(3, ds.columnCount());
    }

    @Test
    public void testToArrayH() {
        Object[][] array = sheet.toArrayH();
        assertNotNull(array);
        assertEquals(3, array.length);
        assertEquals(3, array[0].length);
        assertEquals(Integer.valueOf(1), array[0][0]);
        assertEquals(Integer.valueOf(9), array[2][2]);
    }

    @Test
    public void testToArrayHWithComponentType() {
        Integer[][] array = sheet.toArrayH(Integer.class);
        assertNotNull(array);
        assertEquals(3, array.length);
        assertEquals(3, array[0].length);
        assertEquals(Integer.valueOf(1), array[0][0]);
        assertEquals(Integer.valueOf(9), array[2][2]);
    }

    @Test
    public void testToArrayV() {
        Object[][] array = sheet.toArrayV();
        assertNotNull(array);
        assertEquals(3, array.length);
        assertEquals(3, array[0].length);
        assertEquals(Integer.valueOf(1), array[0][0]);
        assertEquals(Integer.valueOf(9), array[2][2]);
    }

    @Test
    public void testToArrayVWithComponentType() {
        Integer[][] array = sheet.toArrayV(Integer.class);
        assertNotNull(array);
        assertEquals(3, array.length);
        assertEquals(3, array[0].length);
        assertEquals(Integer.valueOf(1), array[0][0]);
        assertEquals(Integer.valueOf(9), array[2][2]);
    }

    @Test
    public void testApply() {
        Integer sum = sheet.apply(s -> {
            int total = 0;
            for (int i = 0; i < s.rowCount(); i++) {
                for (int j = 0; j < s.columnCount(); j++) {
                    Integer val = s.get(i, j);
                    if (val != null) {
                        total += val;
                    }
                }
            }
            return total;
        });
        assertEquals(Integer.valueOf(45), sum);
    }

    @Test
    public void testApplyIfNotEmpty() {
        u.Optional<Integer> result = sheet.applyIfNotEmpty(s -> {
            return s.get(0, 0);
        });
        assertTrue(result.isPresent());
        assertEquals(Integer.valueOf(1), result.get());
    }

    @Test
    public void testApplyIfNotEmptyOnEmptySheet() {
        u.Optional<Integer> result = emptySheet.applyIfNotEmpty(s -> s.get(0, 0));
        assertFalse(result.isPresent());
    }

    @Test
    public void testAccept() {
        List<Integer> values = new ArrayList<>();
        sheet.accept(s -> {
            for (int i = 0; i < s.rowCount(); i++) {
                for (int j = 0; j < s.columnCount(); j++) {
                    Integer val = s.get(i, j);
                    if (val != null) {
                        values.add(val);
                    }
                }
            }
        });
        assertEquals(9, values.size());
    }

    @Test
    public void testAcceptIfNotEmpty() {
        List<Integer> values = new ArrayList<>();
        sheet.acceptIfNotEmpty(s -> {
            values.add(s.get(0, 0));
        });
        assertEquals(1, values.size());
        assertEquals(Integer.valueOf(1), values.get(0));
    }

    @Test
    public void testAcceptIfNotEmptyOnEmptySheet() {
        List<Integer> values = new ArrayList<>();
        emptySheet.acceptIfNotEmpty(s -> values.add(1));
        assertEquals(0, values.size());
    }

    @Test
    public void testPrintln() {
        sheet.println();
    }

    @Test
    public void testPrintlnWithPrefix() {
        sheet.println("Test Prefix:");
    }

    @Test
    public void testPrintlnWithSubset() {
        sheet.println(Arrays.asList("row1", "row2"), Arrays.asList("col1", "col2"));
    }

    @Test
    public void testPrintlnWithOutput() {
        StringWriter writer = new StringWriter();
        sheet.println(writer);
        String output = writer.toString();
        assertNotNull(output);
        assertTrue(output.length() > 0);
    }

    @Test
    public void testPrintlnWithSubsetAndOutput() {
        StringWriter writer = new StringWriter();
        sheet.println(Arrays.asList("row1", "row2"), Arrays.asList("col1", "col2"), writer);
        String output = writer.toString();
        assertNotNull(output);
        assertTrue(output.length() > 0);
    }

    @Test
    public void testPrintlnWithFullParams() {
        StringWriter writer = new StringWriter();
        sheet.println(Arrays.asList("row1", "row2"), Arrays.asList("col1", "col2"), "Custom Prefix:", writer);
        String output = writer.toString();
        assertNotNull(output);
        assertTrue(output.length() > 0);
        assertTrue(output.contains("Custom Prefix"));
    }

    @Test
    public void testHashCode() {
        Sheet<String, String, Integer> sheet2 = Sheet.rows(rowKeys, columnKeys, sampleData);
        assertEquals(sheet.hashCode(), sheet2.hashCode());
    }

    @Test
    public void testEquals() {
        Sheet<String, String, Integer> sheet2 = Sheet.rows(rowKeys, columnKeys, sampleData);
        assertTrue(sheet.equals(sheet2));
        assertTrue(sheet.equals(sheet));
        assertFalse(sheet.equals(null));
        assertFalse(sheet.equals("not a sheet"));
    }

    @Test
    public void testEqualsDifferentValues() {
        Sheet<String, String, Integer> sheet2 = Sheet.rows(rowKeys, columnKeys, new Integer[][] { { 1, 2, 3 }, { 4, 5, 6 }, { 7, 8, 100 } });
        assertFalse(sheet.equals(sheet2));
    }

    @Test
    public void testToString() {
        String str = sheet.toString();
        assertNotNull(str);
        assertTrue(str.length() > 0);
    }

    @Test
    public void testPointOf() {
        Point point = Point.of(1, 2);
        assertNotNull(point);
        assertEquals(1, point.rowIndex());
        assertEquals(2, point.columnIndex());
    }

    @Test
    public void testPointEquals() {
        Point p1 = Point.of(1, 2);
        Point p2 = Point.of(1, 2);
        Point p3 = Point.of(2, 1);

        assertEquals(p1, p2);
        assertFalse(p1.equals(p3));
    }

    @Test
    public void testPointHashCode() {
        Point p1 = Point.of(1, 2);
        Point p2 = Point.of(1, 2);

        assertEquals(p1.hashCode(), p2.hashCode());
    }

    @Test
    public void testPointZero() {
        Point zero = Point.ZERO;
        assertNotNull(zero);
        assertEquals(0, zero.rowIndex());
        assertEquals(0, zero.columnIndex());
    }

    @Test
    public void testPointToString() {
        Point point = Point.of(1, 2);
        String str = point.toString();
        assertNotNull(str);
        assertTrue(str.contains("1"));
        assertTrue(str.contains("2"));
    }

    @Test
    public void testCellOf() {
        Cell<String, String, Integer> cell = Cell.of("r1", "c1", 42);
        assertNotNull(cell);
        assertEquals("r1", cell.rowKey());
        assertEquals("c1", cell.columnKey());
        assertEquals(Integer.valueOf(42), cell.value());
    }

    @Test
    public void testCellEquals() {
        Cell<String, String, Integer> c1 = Cell.of("r1", "c1", 42);
        Cell<String, String, Integer> c2 = Cell.of("r1", "c1", 42);
        Cell<String, String, Integer> c3 = Cell.of("r1", "c1", 43);

        assertEquals(c1, c2);
        assertFalse(c1.equals(c3));
    }

    @Test
    public void testCellHashCode() {
        Cell<String, String, Integer> c1 = Cell.of("r1", "c1", 42);
        Cell<String, String, Integer> c2 = Cell.of("r1", "c1", 42);

        assertEquals(c1.hashCode(), c2.hashCode());
    }

    @Test
    public void testCellToString() {
        Cell<String, String, Integer> cell = Cell.of("r1", "c1", 42);
        String str = cell.toString();
        assertNotNull(str);
        assertTrue(str.contains("r1"));
        assertTrue(str.contains("c1"));
        assertTrue(str.contains("42"));
    }

    @Test
    public void testSingleCellSheet() {
        Sheet<String, String, Integer> single = Sheet.rows(Arrays.asList("r1"), Arrays.asList("c1"), new Integer[][] { { 42 } });
        assertEquals(1, single.rowCount());
        assertEquals(1, single.columnCount());
        assertEquals(Integer.valueOf(42), single.get("r1", "c1"));
    }

    @Test
    public void testSingleRowSheet() {
        Sheet<String, String, Integer> singleRow = Sheet.rows(Arrays.asList("r1"), Arrays.asList("c1", "c2", "c3"), new Integer[][] { { 1, 2, 3 } });
        assertEquals(1, singleRow.rowCount());
        assertEquals(3, singleRow.columnCount());
        assertEquals(Integer.valueOf(1), singleRow.get("r1", "c1"));
    }

    @Test
    public void testSingleColumnSheet() {
        Sheet<String, String, Integer> singleCol = Sheet.rows(Arrays.asList("r1", "r2", "r3"), Arrays.asList("c1"), new Integer[][] { { 1 }, { 2 }, { 3 } });
        assertEquals(3, singleCol.rowCount());
        assertEquals(1, singleCol.columnCount());
        assertEquals(Integer.valueOf(1), singleCol.get("r1", "c1"));
    }

    @Test
    public void testSheetWithAllNullValues() {
        Sheet<String, String, Integer> allNull = Sheet.rows(Arrays.asList("r1", "r2"), Arrays.asList("c1", "c2"),
                new Integer[][] { { null, null }, { null, null } });
        assertEquals(0, allNull.countOfNonNullValues());
        assertTrue(allNull.containsValue(null));
        assertFalse(allNull.containsValue(1));
    }

    @Test
    public void testCopyEmptySheet() {
        Sheet<String, String, Integer> emptyCopy = emptySheet.copy();
        assertTrue(emptyCopy.isEmpty());
        assertFalse(emptyCopy.isFrozen());
    }

    @Test
    public void testTransposeEmptySheet() {
        Sheet<String, String, Integer> transposed = emptySheet.transpose();
        assertTrue(transposed.isEmpty());
    }

    @Test
    public void testStreamOperationsOnEmptySheet() {
        assertEquals(0, emptySheet.streamH().count());
        assertEquals(0, emptySheet.streamV().count());
        assertEquals(0, emptySheet.cellsH().count());
        assertEquals(0, emptySheet.pointsH().count());
    }
}

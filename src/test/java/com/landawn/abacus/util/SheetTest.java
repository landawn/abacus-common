package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.AbstractTest;
import com.landawn.abacus.util.Sheet.Cell;
import com.landawn.abacus.util.Sheet.Point;
import com.landawn.abacus.util.stream.Stream;

@Tag("old-test")
public class SheetTest extends AbstractTest {

    private Sheet<String, String, Integer> sheet;
    private Sheet<String, String, Integer> emptySheet;
    private Sheet<String, String, Object> objectSheet;
    private Sheet<String, String, Integer> intSheet;
    private Sheet<String, String, Integer> sortSheet;
    private List<String> rowKeys;
    private List<String> columnKeys;
    private List<String> upperRowKeys;
    private List<String> colKeys;
    private Integer[][] sampleData;
    private StringWriter stringWriter;

    @BeforeEach
    public void setUp() {
        rowKeys = Arrays.asList("row1", "row2", "row3");
        columnKeys = Arrays.asList("col1", "col2", "col3");
        upperRowKeys = Arrays.asList("R1", "R2", "R3");
        colKeys = Arrays.asList("C1", "C2", "C3");
        sampleData = new Integer[][] { { 1, 2, 3 }, { 4, 5, 6 }, { 7, 8, 9 } };

        sheet = Sheet.rows(rowKeys, columnKeys, sampleData);
        emptySheet = Sheet.empty();
        objectSheet = Sheet.rows(upperRowKeys, colKeys, new Object[][] { { "V11", "V12", null }, { 100, null, true }, { null, null, null } });
        intSheet = Sheet.rows(upperRowKeys, colKeys, new Integer[][] { { 11, 12, 13 }, { 21, 22, 23 }, { 31, 32, 33 } });
        sortSheet = Sheet.rows(Arrays.asList("B", "C", "A"), Arrays.asList("Y", "Z", "X"), new Integer[][] { { 1, 2, 3 }, { 4, 5, 6 }, { 7, 8, 9 } });
        stringWriter = new StringWriter();
    }

    @Test
    public void test_01() {
        Dataset.empty().println();

        Sheet.empty().println();

        Sheet<String, String, Object> sheet = Sheet.rows(CommonUtil.toList("r1", "r2", "r3"), CommonUtil.toList("c1", "c2"),
                new Object[][] { { 1, "a" }, { null, "b" }, { 5, "c" } });

        sheet.println();
        sheet.toDatasetH().println();
        sheet.toDatasetV().println();

        sheet.rows().map(it -> Pair.of(it.left(), it.right().join(","))).forEach(Fn.println());
        sheet.columns().map(it -> Pair.of(it.left(), it.right().join(","))).forEach(Fn.println());

        N.println(sheet.toString());

        N.println(N.toJson(sheet));
        N.println(N.toJson(sheet, true));

        assertEquals(sheet, N.fromJson(N.toJson(sheet), Sheet.class));

        N.println(sheet.nonNullValueCount());

        sheet = new Sheet<>(CommonUtil.toList("r1", "r2", "r3"), CommonUtil.toList("c1", "c2"));
        sheet.forEachH((r, c, v) -> N.println(r + ": " + c + ": " + v));

        sheet = Sheet.rows(CommonUtil.emptyList(), CommonUtil.emptyList(), CommonUtil.emptyList());
        N.println(sheet);
        N.println(N.toJson(sheet));

        sheet = Sheet.rows((List<String>) null, (List<String>) null, (List<List<Object>>) null);
        sheet.println();
        N.println(sheet);
        N.println(N.toJson(sheet));

        assertEquals(sheet, sheet.clone());

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
        assertEquals(9, sheet.nonNullValueCount());

        Sheet<String, String, Integer> withNulls = Sheet.rows(Arrays.asList("r1", "r2"), Arrays.asList("c1", "c2"),
                new Integer[][] { { 1, null }, { null, 4 } });
        assertEquals(2, withNulls.nonNullValueCount());
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
        assertDoesNotThrow(() -> {
            sheet.println();
        });
    }

    @Test
    public void testPrintlnWithPrefix() {
        assertDoesNotThrow(() -> {
            sheet.println("Test Prefix:");
        });
    }

    @Test
    public void testPrintlnWithSubset() {
        assertDoesNotThrow(() -> {
            sheet.println(Arrays.asList("row1", "row2"), Arrays.asList("col1", "col2"));
        });
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
        assertEquals(0, allNull.nonNullValueCount());
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

    @Test
    public void testConstructorWithKeys() {
        Sheet<String, String, String> newSheet = new Sheet<>(upperRowKeys, colKeys);
        assertFalse(newSheet.isEmpty());
        assertEquals(3, newSheet.rowCount());
        assertEquals(3, newSheet.columnCount());
        assertEquals(new LinkedHashSet<>(upperRowKeys), new LinkedHashSet<>(newSheet.rowKeySet()));
        assertEquals(new LinkedHashSet<>(colKeys), new LinkedHashSet<>(newSheet.columnKeySet()));
        assertNull(newSheet.get("R1", "C1"));
    }

    @Test
    public void testConstructorWithKeys_nullInKeysThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> new Sheet<>(Arrays.asList("R1", null), colKeys));
        assertThrows(IllegalArgumentException.class, () -> new Sheet<>(upperRowKeys, Arrays.asList("C1", null)));
    }

    @Test
    public void testConstructorWithKeysAndDataArray() {
        Object[][] data = { { "V11", "V12" }, { "V21", "V22" } };
        List<String> rk = Arrays.asList("R1", "R2");
        List<String> ck = Arrays.asList("C1", "C2");
        Sheet<String, String, Object> dataSheet = new Sheet<>(rk, ck, data);

        assertEquals(2, dataSheet.rowCount());
        assertEquals(2, dataSheet.columnCount());
        assertEquals("V11", dataSheet.get("R1", "C1"));
        assertEquals("V12", dataSheet.get("R1", "C2"));
        assertEquals("V21", dataSheet.get("R2", "C1"));
        assertEquals("V22", dataSheet.get("R2", "C2"));
    }

    @Test
    public void testConstructorWithKeysAndEmptyDataArray() {
        Object[][] data = {};
        List<String> rk = Arrays.asList("R1", "R2");
        List<String> ck = Arrays.asList("C1", "C2");
        Sheet<String, String, Object> dataSheet = new Sheet<>(rk, ck, data);
        assertEquals(2, dataSheet.rowCount());
        assertEquals(2, dataSheet.columnCount());
        assertNull(dataSheet.get("R1", "C1"));
    }

    @Test
    public void testConstructorWithKeysAndDataArray_mismatchDimensions() {
        Object[][] dataMismatchRow = { { "V11", "V12" } };
        List<String> rk = Arrays.asList("R1", "R2");
        List<String> ck = Arrays.asList("C1", "C2");
        assertThrows(IllegalArgumentException.class, () -> new Sheet<>(rk, ck, dataMismatchRow));

        Object[][] dataMismatchCol = { { "V11" }, { "V21" } };
        assertThrows(IllegalArgumentException.class, () -> new Sheet<>(rk, ck, dataMismatchCol));
    }

    @Test
    public void testEmptyFactory() {
        Sheet<String, String, String> emptySheet = Sheet.empty();
        assertTrue(emptySheet.isEmpty());
        assertTrue(emptySheet.isFrozen());
        assertEquals(0, emptySheet.rowCount());
        assertEquals(0, emptySheet.columnCount());
    }

    @Test
    public void testRowsFactory_fromArray() {
        Object[][] data = { { "V11", "V12" }, { "V21", "V22" } };
        List<String> rk = Arrays.asList("R1", "R2");
        List<String> ck = Arrays.asList("C1", "C2");
        Sheet<String, String, Object> dataSheet = Sheet.rows(rk, ck, data);

        assertEquals("V11", dataSheet.get("R1", "C1"));
        assertEquals("V22", dataSheet.get("R2", "C2"));
    }

    @Test
    public void testRowsFactory_fromCollection() {
        List<List<String>> rowsData = Arrays.asList(Arrays.asList("V11", "V12"), Arrays.asList("V21", "V22"));
        List<String> rk = Arrays.asList("R1", "R2");
        List<String> ck = Arrays.asList("C1", "C2");
        Sheet<String, String, String> dataSheet = Sheet.rows(rk, ck, rowsData);

        assertEquals("V11", dataSheet.get("R1", "C1"));
        assertEquals("V22", dataSheet.get("R2", "C2"));
    }

    @Test
    public void testRowsFactory_fromEmptyCollection() {
        List<List<String>> rowsData = Collections.emptyList();
        List<String> rk = Arrays.asList("R1", "R2");
        List<String> ck = Arrays.asList("C1", "C2");
        Sheet<String, String, String> dataSheet = Sheet.rows(rk, ck, rowsData);
        assertEquals(2, dataSheet.rowCount());
        assertEquals(2, dataSheet.columnCount());
        assertNull(dataSheet.get("R1", "C1"));
    }

    @Test
    public void testRowsFactory_fromCollection_mismatchDimensions() {
        List<List<String>> rowsDataMismatchRow = Arrays.asList(Arrays.asList("V11", "V12"));
        List<String> rk = Arrays.asList("R1", "R2");
        List<String> ck = Arrays.asList("C1", "C2");
        assertThrows(IllegalArgumentException.class, () -> Sheet.rows(rk, ck, rowsDataMismatchRow));

        List<List<String>> rowsDataMismatchCol = Arrays.asList(Arrays.asList("V11"), Arrays.asList("V21"));
        assertThrows(IllegalArgumentException.class, () -> Sheet.rows(rk, ck, rowsDataMismatchCol));
    }

    @Test
    public void testColumnsFactory_fromArray() {
        Object[][] data = { { "V11", "V21" }, { "V12", "V22" } };
        List<String> rk = Arrays.asList("R1", "R2");
        List<String> ck = Arrays.asList("C1", "C2");
        Sheet<String, String, Object> dataSheet = Sheet.columns(rk, ck, data);

        assertEquals("V11", dataSheet.get("R1", "C1"));
        assertEquals("V22", dataSheet.get("R2", "C2"));
    }

    @Test
    public void testColumnsFactory_fromCollection() {
        List<List<String>> colsData = Arrays.asList(Arrays.asList("V11", "V21"), Arrays.asList("V12", "V22"));
        List<String> rk = Arrays.asList("R1", "R2");
        List<String> ck = Arrays.asList("C1", "C2");
        Sheet<String, String, String> dataSheet = Sheet.columns(rk, ck, colsData);

        assertEquals("V11", dataSheet.get("R1", "C1"));
        assertEquals("V22", dataSheet.get("R2", "C2"));
    }

    @Test
    public void testGetByKeys() {
        assertEquals("V11", objectSheet.get("R1", "C1"));
        assertEquals(100, objectSheet.get("R2", "C1"));
        assertNull(objectSheet.get("R1", "C3"));
    }

    @Test
    public void testGetByKeys_uninitializedSheet() {
        Sheet<String, String, String> uninitializedSheet = new Sheet<>(upperRowKeys, colKeys);
        assertNull(uninitializedSheet.get("R1", "C1"));
    }

    @Test
    public void testGetByKeys_invalidKeys() {
        assertThrows(IllegalArgumentException.class, () -> objectSheet.get("RX", "C1"));
        assertThrows(IllegalArgumentException.class, () -> objectSheet.get("R1", "CX"));
    }

    @Test
    public void testGetByIndices() {
        assertEquals("V11", objectSheet.get(0, 0));
        assertEquals(100, objectSheet.get(1, 0));
        assertNull(objectSheet.get(0, 2));
    }

    @Test
    public void testGetByIndices_uninitializedSheet() {
        Sheet<String, String, String> uninitializedSheet = new Sheet<>(upperRowKeys, colKeys);
        assertNull(uninitializedSheet.get(0, 0));
    }

    @Test
    public void testGetByIndices_outOfBounds() {
        assertThrows(IndexOutOfBoundsException.class, () -> objectSheet.get(5, 0));
        assertThrows(IndexOutOfBoundsException.class, () -> objectSheet.get(0, 5));
    }

    @Test
    public void testGetByPoint() {
        assertEquals("V11", objectSheet.get(Sheet.Point.of(0, 0)));
        assertNull(objectSheet.get(Sheet.Point.of(0, 2)));
    }

    @Test
    public void testGetByPoint_outOfBounds() {
        assertThrows(IndexOutOfBoundsException.class, () -> objectSheet.get(Sheet.Point.of(5, 0)));
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
    public void testPutByKeys_frozenSheet() {
        objectSheet.freeze();
        assertThrows(IllegalStateException.class, () -> objectSheet.set("R1", "C1", "Val"));
    }

    @Test
    public void testPutByIndices() {
        Object prev = objectSheet.set(2, 2, "V33");
        assertNull(prev);
        assertEquals("V33", objectSheet.get(2, 2));

        Object prevUpdate = objectSheet.set(0, 0, "NewV11");
        assertEquals("V11", prevUpdate);
        assertEquals("NewV11", objectSheet.get(0, 0));
    }

    @Test
    public void testPutByIndices_outOfBounds() {
        assertThrows(IndexOutOfBoundsException.class, () -> objectSheet.set(5, 0, "Val"));
    }

    @Test
    public void testPutByPoint() {
        Object prev = objectSheet.set(Sheet.Point.of(2, 2), "V33");
        assertNull(prev);
        assertEquals("V33", objectSheet.get(2, 2));
    }

    @Test
    public void testPutAll_2() {
        Sheet<String, String, Object> sourceSheet = new Sheet<>(Arrays.asList("R1", "R2"), Arrays.asList("C1", "C2"));
        sourceSheet.set("R1", "C1", "SourceV11");
        sourceSheet.set("R2", "C2", "SourceV22");

        objectSheet.putAll(sourceSheet, (a, b) -> Iterables.firstNonNull(b, a));
        assertEquals("SourceV11", objectSheet.get("R1", "C1"));
        assertEquals("V12", objectSheet.get("R1", "C2"));
        assertEquals("SourceV22", objectSheet.get("R2", "C2"));
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
    public void testRemoveByKeys_invalidKeys() {
        assertThrows(IllegalArgumentException.class, () -> objectSheet.remove("RX", "C1"));
    }

    @Test
    public void testRemoveByIndices() {
        Object removed = objectSheet.remove(0, 1);
        assertEquals("V12", removed);
        assertNull(objectSheet.get(0, 1));
    }

    @Test
    public void testRemoveByIndices_uninitializedSheet() {
        Sheet<String, String, String> uninitializedSheet = new Sheet<>(upperRowKeys, colKeys);
        assertNull(uninitializedSheet.remove(0, 0));
    }

    @Test
    public void testRemoveByPoint() {
        Object removed = objectSheet.remove(Sheet.Point.of(1, 0));
        assertEquals(100, removed);
        assertNull(objectSheet.get(1, 0));
    }

    @Test
    public void testRemove_frozenSheet() {
        objectSheet.freeze();
        assertThrows(IllegalStateException.class, () -> objectSheet.remove("R1", "C1"));
        assertThrows(IllegalStateException.class, () -> objectSheet.remove(0, 0));
    }

    @Test
    public void testContains_keyPair() {
        assertTrue(objectSheet.containsCell("R1", "C1"));
        assertTrue(objectSheet.containsCell("R3", "C3"));
        assertFalse(objectSheet.containsCell("RX", "C1"));
        assertFalse(objectSheet.containsCell("R1", "CX"));
    }

    @Test
    public void testContains_keyPairAndValue() {
        assertTrue(objectSheet.containsValueAt("R1", "C1", "V11"));
        assertTrue(objectSheet.containsValueAt("R1", "C3", null));
        assertFalse(objectSheet.containsValueAt("R1", "C1", "WrongValue"));
        assertFalse(objectSheet.containsValueAt("R1", "C3", "NotNull"));
    }

    @Test
    public void testGetRow_uninitializedSheet() {
        Sheet<String, String, String> uninitializedSheet = new Sheet<>(upperRowKeys, colKeys);
        assertEquals(Arrays.asList(null, null, null), new ArrayList<>(uninitializedSheet.rowValues("R1")));
    }

    @Test
    public void testGetRow_invalidKey() {
        assertThrows(IllegalArgumentException.class, () -> objectSheet.rowValues("RX"));
    }

    @Test
    public void testSetRow_sizeMismatch() {
        List<Object> newRowDataShort = Arrays.asList("New1", "New2");
        assertThrows(IllegalArgumentException.class, () -> objectSheet.setRow("R1", newRowDataShort));
    }

    @Test
    public void testSetRow_emptyCollectionToSetNulls() {
        objectSheet.setRow("R1", Collections.emptyList());
        assertEquals(Arrays.asList(null, null, null), new ArrayList<>(objectSheet.rowValues("R1")));
    }

    @Test
    public void testSetRow_frozen() {
        objectSheet.freeze();
        assertThrows(IllegalStateException.class, () -> objectSheet.setRow("R1", Arrays.asList("a", "b", "c")));
    }

    @Test
    public void testAddRow_emptyData() {
        objectSheet.addRow("R4", Collections.emptyList());
        assertTrue(objectSheet.containsRow("R4"));
        assertEquals(4, objectSheet.rowCount());
        assertEquals(Arrays.asList(null, null, null), new ArrayList<>(objectSheet.rowValues("R4")));
    }

    @Test
    public void testAddRow_duplicateKey() {
        assertThrows(IllegalArgumentException.class, () -> objectSheet.addRow("R1", Arrays.asList("a", "b", "c")));
    }

    @Test
    public void testAddRow_sizeMismatch() {
        assertThrows(IllegalArgumentException.class, () -> objectSheet.addRow("R4", Arrays.asList("a", "b")));
    }

    @Test
    public void testAddRow_frozen() {
        objectSheet.freeze();
        assertThrows(IllegalStateException.class, () -> objectSheet.addRow("R4", Arrays.asList("a", "b", "c")));
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
    public void testAddRow_atIndex_outOfBounds() {
        assertThrows(IndexOutOfBoundsException.class, () -> objectSheet.addRow(4, "R5", Arrays.asList("a", "b", "c")));
        assertThrows(IndexOutOfBoundsException.class, () -> objectSheet.addRow(-1, "R0", Arrays.asList("a", "b", "c")));
    }

    @Test
    public void testUpdateRow_frozen() {
        intSheet.freeze();
        assertThrows(IllegalStateException.class, () -> intSheet.updateRow("R1", v -> v + 1));
    }

    @Test
    public void testRemoveRow_invalidKey() {
        assertThrows(IllegalArgumentException.class, () -> objectSheet.removeRow("RX"));
    }

    @Test
    public void testRemoveRow_frozen() {
        objectSheet.freeze();
        assertThrows(IllegalStateException.class, () -> objectSheet.removeRow("R1"));
    }

    @Test
    public void testMoveRow_toBeginning() {
        List<Object> r1Data = new ArrayList<>(objectSheet.rowValues("R1"));
        List<Object> r2Data = new ArrayList<>(objectSheet.rowValues("R2"));
        List<Object> r3Data = new ArrayList<>(objectSheet.rowValues("R3"));

        objectSheet.moveRow("R3", 0);
        assertEquals(Arrays.asList("R3", "R1", "R2"), new ArrayList<>(objectSheet.rowKeySet()));
        assertEquals(r3Data, new ArrayList<>(objectSheet.rowValues("R3")));
        assertEquals(r1Data, new ArrayList<>(objectSheet.rowValues("R1")));
        assertEquals(r2Data, new ArrayList<>(objectSheet.rowValues("R2")));
    }

    @Test
    public void testMoveRow_toSamePosition() {
        List<String> initialRowOrder = new ArrayList<>(objectSheet.rowKeySet());
        List<Object> r2Data = new ArrayList<>(objectSheet.rowValues("R2"));
        objectSheet.moveRow("R2", 1);
        assertEquals(initialRowOrder, new ArrayList<>(objectSheet.rowKeySet()));
        assertEquals(r2Data, new ArrayList<>(objectSheet.rowValues("R2")));
    }

    @Test
    public void testMoveRow_frozen() {
        objectSheet.freeze();
        assertThrows(IllegalStateException.class, () -> objectSheet.moveRow("R1", 1));
    }

    @Test
    public void testSwapRowPosition_frozen() {
        objectSheet.freeze();
        assertThrows(IllegalStateException.class, () -> objectSheet.swapRows("R1", "R2"));
    }

    @Test
    public void testRenameRow_newNameExists() {
        assertThrows(IllegalArgumentException.class, () -> objectSheet.renameRow("R1", "R2"));
    }

    @Test
    public void testRenameRow_oldNameNotFound() {
        assertThrows(IllegalArgumentException.class, () -> objectSheet.renameRow("RX_NonExistent", "R_New"));
    }

    @Test
    public void testRenameRow_frozen() {
        objectSheet.freeze();
        assertThrows(IllegalStateException.class, () -> objectSheet.renameRow("R1", "RNew"));
    }

    @Test
    public void testRow_asMap() {
        Map<String, Object> row1Map = objectSheet.rowAsMap("R1");
        assertEquals("V11", row1Map.get("C1"));
        assertEquals("V12", row1Map.get("C2"));
        assertNull(row1Map.get("C3"));
        assertEquals(3, row1Map.size());
        assertTrue(row1Map instanceof LinkedHashMap);
    }

    @Test
    public void testRow_asMap_uninitializedSheet() {
        Sheet<String, String, String> uninitializedSheet = new Sheet<>(upperRowKeys, colKeys);
        Map<String, String> row1Map = uninitializedSheet.rowAsMap("R1");
        assertNull(row1Map.get("C1"));
        assertEquals(3, row1Map.size());
    }

    @Test
    public void testGetColumn_uninitializedSheet() {
        Sheet<String, String, String> uninitializedSheet = new Sheet<>(rowKeys, colKeys);
        assertEquals(Arrays.asList(null, null, null), new ArrayList<>(uninitializedSheet.columnValues("C1")));
    }

    @Test
    public void testGetColumn_invalidKey() {
        assertThrows(IllegalArgumentException.class, () -> objectSheet.columnValues("CX"));
    }

    @Test
    public void testSetColumn_sizeMismatch() {
        List<Object> newColDataShort = Arrays.asList("New1", "New2");
        assertThrows(IllegalArgumentException.class, () -> objectSheet.setColumn("C1", newColDataShort));
    }

    @Test
    public void testSetColumn_emptyCollectionToSetNulls() {
        objectSheet.setColumn("C1", Collections.emptyList());
        assertEquals(Arrays.asList(null, null, null), new ArrayList<>(objectSheet.columnValues("C1")));
    }

    @Test
    public void testSetColumn_frozen() {
        objectSheet.freeze();
        assertThrows(IllegalStateException.class, () -> objectSheet.setColumn("C1", Arrays.asList("a", "b", "c")));
    }

    @Test
    public void testAddColumn_emptyData() {
        objectSheet.addColumn("C4", Collections.emptyList());
        assertTrue(objectSheet.containsColumn("C4"));
        assertEquals(4, objectSheet.columnCount());
        assertEquals(Arrays.asList(null, null, null), new ArrayList<>(objectSheet.columnValues("C4")));
    }

    @Test
    public void testAddColumn_duplicateKey() {
        assertThrows(IllegalArgumentException.class, () -> objectSheet.addColumn("C1", Arrays.asList("a", "b", "c")));
    }

    @Test
    public void testAddColumn_sizeMismatch() {
        assertThrows(IllegalArgumentException.class, () -> objectSheet.addColumn("C4", Arrays.asList("a", "b")));
    }

    @Test
    public void testAddColumn_frozen() {
        objectSheet.freeze();
        assertThrows(IllegalStateException.class, () -> objectSheet.addColumn("C4", Arrays.asList("a", "b", "c")));
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
    public void testAddColumn_atIndex_outOfBounds() {
        assertThrows(IndexOutOfBoundsException.class, () -> objectSheet.addColumn(4, "C5", Arrays.asList("a", "b", "c")));
        assertThrows(IndexOutOfBoundsException.class, () -> objectSheet.addColumn(-1, "C0", Arrays.asList("a", "b", "c")));
    }

    @Test
    public void testUpdateColumn_frozen() {
        intSheet.freeze();
        assertThrows(IllegalStateException.class, () -> intSheet.updateColumn("C1", v -> v + 1));
    }

    @Test
    public void testRemoveColumn_invalidKey() {
        assertThrows(IllegalArgumentException.class, () -> objectSheet.removeColumn("CX"));
    }

    @Test
    public void testRemoveColumn_frozen() {
        objectSheet.freeze();
        assertThrows(IllegalStateException.class, () -> objectSheet.removeColumn("C1"));
    }

    @Test
    public void testMoveColumn_toBeginning() {
        List<Object> c1Data = new ArrayList<>(objectSheet.columnValues("C1"));
        List<Object> c2Data = new ArrayList<>(objectSheet.columnValues("C2"));
        List<Object> c3Data = new ArrayList<>(objectSheet.columnValues("C3"));

        objectSheet.moveColumn("C3", 0);
        assertEquals(Arrays.asList("C3", "C1", "C2"), new ArrayList<>(objectSheet.columnKeySet()));
        assertEquals(c3Data, new ArrayList<>(objectSheet.columnValues("C3")));
        assertEquals(c1Data, new ArrayList<>(objectSheet.columnValues("C1")));
        assertEquals(c2Data, new ArrayList<>(objectSheet.columnValues("C2")));
    }

    @Test
    public void testMoveColumn_frozen() {
        objectSheet.freeze();
        assertThrows(IllegalStateException.class, () -> objectSheet.moveColumn("C1", 1));
    }

    @Test
    public void testSwapColumnPosition_frozen() {
        objectSheet.freeze();
        assertThrows(IllegalStateException.class, () -> objectSheet.swapColumns("C1", "C2"));
    }

    @Test
    public void testRenameColumn_newNameExists() {
        assertThrows(IllegalArgumentException.class, () -> objectSheet.renameColumn("C1", "C2"));
    }

    @Test
    public void testRenameColumn_oldNameNotFound() {
        assertThrows(IllegalArgumentException.class, () -> objectSheet.renameColumn("CX_NonExistent", "C_New"));
    }

    @Test
    public void testRenameColumn_frozen() {
        objectSheet.freeze();
        assertThrows(IllegalStateException.class, () -> objectSheet.renameColumn("C1", "CNew"));
    }

    @Test
    public void testColumn_asMap() {
        Map<String, Object> col1Map = objectSheet.columnAsMap("C1");
        assertEquals("V11", col1Map.get("R1"));
        assertEquals(100, col1Map.get("R2"));
        assertNull(col1Map.get("R3"));
        assertEquals(3, col1Map.size());
        assertTrue(col1Map instanceof LinkedHashMap);
    }

    @Test
    public void testColumn_asMap_uninitializedSheet() {
        Sheet<String, String, String> uninitializedSheet = new Sheet<>(rowKeys, colKeys);
        Map<String, String> col1Map = uninitializedSheet.columnAsMap("C1");
        assertNull(col1Map.get("R1"));
        assertEquals(3, col1Map.size());
    }

    @Test
    public void testUpdateAll_byValue() {
        intSheet.updateAll(val -> val == null ? -1 : val + 10);
        assertEquals(21, intSheet.get("R1", "C1"));
        assertEquals(43, intSheet.get("R3", "C3"));

        Sheet<String, String, Integer> sheetWithNulls = new Sheet<>(Arrays.asList("R1"), Arrays.asList("C1"));
        sheetWithNulls.updateAll(val -> val == null ? -1 : val + 10);
        assertEquals(-1, sheetWithNulls.get("R1", "C1"));
    }

    @Test
    public void testUpdateAll_byIndices() {
        intSheet.updateAll((rIdx, cIdx) -> (rIdx + 1) * 100 + (cIdx + 1) * 10);
        assertEquals(110, intSheet.get(0, 0));
        assertEquals(330, intSheet.get(2, 2));
    }

    @Test
    public void testUpdateAll_byKeysAndValue() {
        intSheet.updateAll((rKey, cKey, val) -> {
            int rNum = Integer.parseInt(rKey.substring(1));
            int cNum = Integer.parseInt(cKey.substring(1));
            return (val == null ? 0 : val) + rNum * 10 + cNum;
        });
        assertEquals(22, intSheet.get("R1", "C1"));
        assertEquals(66, intSheet.get("R3", "C3"));
    }

    @Test
    public void testReplaceIf_byValuePredicate() {
        intSheet.replaceIf(val -> val != null && val > 20 && val < 30, 999);
        assertEquals(11, intSheet.get("R1", "C1"));
        assertEquals(999, intSheet.get("R2", "C1"));
        assertEquals(999, intSheet.get("R2", "C2"));
        assertEquals(999, intSheet.get("R2", "C3"));
        assertEquals(31, intSheet.get("R3", "C1"));
    }

    @Test
    public void testReplaceIf_byValuePredicate_withNulls() {
        objectSheet.replaceIf(Objects::isNull, "REPLACED_NULL");
        assertEquals("V11", objectSheet.get("R1", "C1"));
        assertEquals("REPLACED_NULL", objectSheet.get("R1", "C3"));
        assertEquals("REPLACED_NULL", objectSheet.get("R2", "C2"));
    }

    @Test
    public void testReplaceIf_byIndexPredicate() {
        intSheet.replaceIf((rIdx, cIdx) -> rIdx == 1, 777);
        assertEquals(11, intSheet.get("R1", "C1"));
        assertEquals(777, intSheet.get("R2", "C1"));
        assertEquals(777, intSheet.get("R2", "C2"));
        assertEquals(777, intSheet.get("R2", "C3"));
        assertEquals(31, intSheet.get("R3", "C1"));
    }

    @Test
    public void testReplaceIf_byKeyAndValuePredicate() {
        intSheet.replaceIf((rKey, cKey, val) -> "R2".equals(rKey) && val != null && val > 21, 888);
        assertEquals(21, intSheet.get("R2", "C1"));
        assertEquals(888, intSheet.get("R2", "C2"));
        assertEquals(888, intSheet.get("R2", "C3"));
    }

    @Test
    public void testUpdateAll_frozen() {
        objectSheet.freeze();
        assertThrows(IllegalStateException.class, () -> objectSheet.updateAll(v -> v));
        assertThrows(IllegalStateException.class, () -> objectSheet.updateAll((r, c) -> "v"));
        assertThrows(IllegalStateException.class, () -> objectSheet.updateAll((r, c, v) -> v));
    }

    @Test
    public void testReplaceIf_frozen() {
        objectSheet.freeze();
        assertThrows(IllegalStateException.class, () -> objectSheet.replaceIf(v -> true, "new"));
        assertThrows(IllegalStateException.class, () -> objectSheet.replaceIf((r, c) -> true, "new"));
        assertThrows(IllegalStateException.class, () -> objectSheet.replaceIf((r, c, v) -> true, "new"));
    }

    @Test
    public void testSortByRowKey_natural() {
        sortSheet.sortByRowKey();
        assertEquals(Arrays.asList("A", "B", "C"), new ArrayList<>(sortSheet.rowKeySet()));
        assertEquals(7, sortSheet.get("A", "Y"));
        assertEquals(8, sortSheet.get("A", "Z"));
        assertEquals(9, sortSheet.get("A", "X"));
        assertEquals(1, sortSheet.get("B", "Y"));
    }

    @Test
    public void testSortByRowKey_customComparator() {
        sortSheet.sortByRowKey(Comparator.reverseOrder());
        assertEquals(Arrays.asList("C", "B", "A"), new ArrayList<>(sortSheet.rowKeySet()));
        assertEquals(4, sortSheet.get("C", "Y"));
        assertEquals(1, sortSheet.get("B", "Y"));
        assertEquals(7, sortSheet.get("A", "Y"));
    }

    @Test
    public void testSortByRow_valuesInARow() {
        sortSheet.sortColumnsByRowValues("A", Comparator.naturalOrder());
        assertEquals(Arrays.asList("Y", "Z", "X"), new ArrayList<>(sortSheet.columnKeySet()));
        assertEquals(1, sortSheet.get("B", "Y"));
        assertEquals(2, sortSheet.get("B", "Z"));
        assertEquals(3, sortSheet.get("B", "X"));

        sortSheet.sortColumnsByRowValues("A", Comparator.reverseOrder());
        assertEquals(Arrays.asList("X", "Z", "Y"), new ArrayList<>(sortSheet.columnKeySet()));
        assertEquals(3, sortSheet.get("B", "X"));
        assertEquals(2, sortSheet.get("B", "Z"));
        assertEquals(1, sortSheet.get("B", "Y"));
    }

    @Test
    public void testSortByRows_multipleRowsCriteria() {
        sortSheet.sortColumnsByRowValues(Arrays.asList("B", "A"), (arr1, arr2) -> {
            int cmp = ((Integer) arr1[0]).compareTo((Integer) arr2[0]);
            if (cmp == 0) {
                return ((Integer) arr1[1]).compareTo((Integer) arr2[1]);
            }
            return cmp;
        });
        assertEquals(Arrays.asList("Y", "Z", "X"), new ArrayList<>(sortSheet.columnKeySet()));
    }

    @Test
    public void testSortByColumnKey_natural() {
        sortSheet.sortByColumnKey();
        assertEquals(Arrays.asList("X", "Y", "Z"), new ArrayList<>(sortSheet.columnKeySet()));
        assertEquals(3, sortSheet.get("B", "X"));
        assertEquals(1, sortSheet.get("B", "Y"));
        assertEquals(2, sortSheet.get("B", "Z"));
    }

    @Test
    public void testSortByColumnKey_customComparator() {
        sortSheet.sortByColumnKey(Comparator.reverseOrder());
        assertEquals(Arrays.asList("Z", "Y", "X"), new ArrayList<>(sortSheet.columnKeySet()));
        assertEquals(2, sortSheet.get("B", "Z"));
    }

    @Test
    public void testSortByColumn_valuesInAColumn() {
        sortSheet.sortRowsByColumnValues("X", Comparator.naturalOrder());
        assertEquals(Arrays.asList("B", "C", "A"), new ArrayList<>(sortSheet.rowKeySet()));
        assertEquals(1, sortSheet.get("B", "Y"));
        assertEquals(4, sortSheet.get("C", "Y"));
        assertEquals(7, sortSheet.get("A", "Y"));

        sortSheet.sortRowsByColumnValues("X", Comparator.reverseOrder());
        assertEquals(Arrays.asList("A", "C", "B"), new ArrayList<>(sortSheet.rowKeySet()));
        assertEquals(7, sortSheet.get("A", "Y"));
        assertEquals(4, sortSheet.get("C", "Y"));
        assertEquals(1, sortSheet.get("B", "Y"));
    }

    @Test
    public void testSortByColumns_multipleColsCriteria() {
        sortSheet.sortRowsByColumnValues(Arrays.asList("Y", "X"), (arr1, arr2) -> {
            int cmp = ((Integer) arr1[0]).compareTo((Integer) arr2[0]);
            if (cmp == 0) {
                return ((Integer) arr1[1]).compareTo((Integer) arr2[1]);
            }
            return cmp;
        });
        assertEquals(Arrays.asList("B", "C", "A"), new ArrayList<>(sortSheet.rowKeySet()));
    }

    @Test
    public void testSort_frozenSheet() {
        objectSheet.freeze();
        assertThrows(IllegalStateException.class, () -> objectSheet.sortByRowKey());
        assertThrows(IllegalStateException.class, () -> objectSheet.sortByColumnKey());
        assertThrows(IllegalStateException.class, () -> objectSheet.sortColumnsByRowValues("R1", (Comparator) Comparator.naturalOrder()));
        assertThrows(IllegalStateException.class, () -> objectSheet.sortRowsByColumnValues("C1", (Comparator) Comparator.naturalOrder()));
        assertThrows(IllegalStateException.class,
                () -> objectSheet.sortColumnsByRowValues(Collections.singletonList("R1"), (Comparator) Comparator.naturalOrder()));
        assertThrows(IllegalStateException.class,
                () -> objectSheet.sortRowsByColumnValues(Collections.singletonList("C1"), (Comparator) Comparator.naturalOrder()));
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
    @Disabled("Kryo dependency: Test requires Kryo on classpath and setup. Will throw RuntimeException if Kryo not available.")
    public void testClone_default() {
        try {
            Sheet<String, String, Object> clone = objectSheet.clone();
            assertNotSame(objectSheet, clone);
            assertEquals(objectSheet, clone);
            assertEquals(objectSheet.isFrozen(), clone.isFrozen());

            clone.set("R1", "C1", "ClonedV11");
            assertEquals("ClonedV11", clone.get("R1", "C1"));
            assertEquals("V11", objectSheet.get("R1", "C1"));

        } catch (RuntimeException e) {
            if (e.getMessage() != null && e.getMessage().contains("Kryo is required")) {
                System.err.println("Skipping clone test as Kryo is not available: " + e.getMessage());
            } else {
                throw e;
            }
        }
    }

    @Test
    @Disabled("Kryo dependency: Test requires Kryo on classpath and setup. Will throw RuntimeException if Kryo not available.")
    public void testClone_withFreezeOption() {
        try {
            Sheet<String, String, Object> frozenClone = objectSheet.clone(true);
            assertTrue(frozenClone.isFrozen());
            assertThrows(IllegalStateException.class, () -> frozenClone.set("R1", "C1", "fail"));

            Sheet<String, String, Object> unfrozenClone = objectSheet.clone(false);
            assertFalse(unfrozenClone.isFrozen());
            assertDoesNotThrow(() -> unfrozenClone.set("R1", "C1", "ok"));
        } catch (RuntimeException e) {
            if (e.getMessage() != null && e.getMessage().contains("Kryo is required")) {
                System.err.println("Skipping clone(freeze) test as Kryo is not available: " + e.getMessage());
            } else {
                throw e;
            }
        }
    }

    @Test
    public void testTranspose_emptySheet() {
        Sheet<String, String, String> empty = new Sheet<>();
        Sheet<String, String, String> transposedEmpty = empty.transpose();
        assertTrue(transposedEmpty.isEmpty());
        assertEquals(0, transposedEmpty.rowCount());
        assertEquals(0, transposedEmpty.columnCount());
    }

    @Test
    public void testTranspose_uninitializedSheet() {
        Sheet<String, String, String> uninitialized = new Sheet<>(upperRowKeys, colKeys);
        Sheet<String, String, String> transposed = uninitialized.transpose();
        assertEquals(colKeys, new ArrayList<>(transposed.rowKeySet()));
        assertEquals(upperRowKeys, new ArrayList<>(transposed.columnKeySet()));
        assertNull(transposed.get("C1", "R1"));
    }

    @Test
    public void testFreezeAndIsFrozen() {
        assertFalse(objectSheet.isFrozen());
        objectSheet.freeze();
        assertTrue(objectSheet.isFrozen());
        assertThrows(IllegalStateException.class, () -> objectSheet.set("R1", "C1", "Fail"));
    }

    @Test
    public void testClear_frozenSheet() {
        objectSheet.freeze();
        assertThrows(IllegalStateException.class, () -> objectSheet.clear());
    }

    @Test
    public void testForEachH_exceptionPropagation() {
        IOException thrown = assertThrows(IOException.class, () -> {
            objectSheet.forEachH((r, c, v) -> {
                if (r.equals("R2") && c.equals("C1")) {
                    throw new IOException("Test Exception");
                }
            });
        });
        assertEquals("Test Exception", thrown.getMessage());
    }

    @Test
    public void testForEachV_exceptionPropagation() {
        RuntimeException thrown = assertThrows(RuntimeException.class, () -> {
            objectSheet.forEachV((r, c, v) -> {
                if (c.equals("C2") && r.equals("R1")) {
                    throw new RuntimeException("Test Exception V");
                }
            });
        });
        assertEquals("Test Exception V", thrown.getMessage());
    }

    @Test
    public void testForEachNonNullH_exceptionPropagation() {
        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class, () -> {
            objectSheet.forEachNonNullH((r, c, v) -> {
                if (r.equals("R2") && c.equals("C3")) {
                    throw new IllegalArgumentException("Test Exception NonNullH");
                }
            });
        });
        assertEquals("Test Exception NonNullH", thrown.getMessage());
    }

    @Test
    public void testForEachNonNullV_exceptionPropagation() {
        UnsupportedOperationException thrown = assertThrows(UnsupportedOperationException.class, () -> {
            objectSheet.forEachNonNullV((r, c, v) -> {
                if (c.equals("C1") && r.equals("R1")) {
                    throw new UnsupportedOperationException("Test Exception NonNullV");
                }
            });
        });
        assertEquals("Test Exception NonNullV", thrown.getMessage());
    }

    @Test
    public void testCellsH_emptyRange() {
        assertTrue(objectSheet.cellsH(1, 1).toList().isEmpty());
    }

    @Test
    public void testCellsV_emptyRange() {
        assertTrue(objectSheet.cellsV(1, 1).toList().isEmpty());
    }

    @Test
    public void testCellsR_emptyRange() {
        assertTrue(objectSheet.cellsR(1, 1).toList().isEmpty());
    }

    @Test
    public void testCellsC_emptyRange() {
        assertTrue(objectSheet.cellsC(1, 1).toList().isEmpty());
    }

    @Test
    public void testPointsH_fullRangeAndSubRange() {
        assertEquals(9, objectSheet.pointsH().count());
        assertEquals(3, objectSheet.pointsH(0, 1).count());
        assertEquals(0, objectSheet.pointsH(1, 1).count());
        List<Sheet.Point> r1Points = objectSheet.pointsH(0, 1).toList();
        assertEquals(Sheet.Point.of(0, 0), r1Points.get(0));
        assertEquals(Sheet.Point.of(0, 1), r1Points.get(1));
        assertEquals(Sheet.Point.of(0, 2), r1Points.get(2));
    }

    @Test
    public void testPointsV_fullRangeAndSubRange() {
        assertEquals(9, objectSheet.pointsV().count());
        assertEquals(3, objectSheet.pointsV(0, 1).count());
        assertEquals(0, objectSheet.pointsV(1, 1).count());
        List<Sheet.Point> c1Points = objectSheet.pointsV(0, 1).toList();
        assertEquals(Sheet.Point.of(0, 0), c1Points.get(0));
        assertEquals(Sheet.Point.of(1, 0), c1Points.get(1));
        assertEquals(Sheet.Point.of(2, 0), c1Points.get(2));
    }

    @Test
    public void testPointsR_fullRangeAndSubRange() {
        assertEquals(3, objectSheet.pointsR().count());
        assertEquals(1, objectSheet.pointsR(1, 2).count());
        assertEquals(0, objectSheet.pointsR(1, 1).count());

        List<Sheet.Point> r2Points = objectSheet.pointsR(1, 2).first().get().toList();
        assertEquals(3, r2Points.size());
        assertEquals(Sheet.Point.of(1, 0), r2Points.get(0));
    }

    @Test
    public void testPointsC_fullRangeAndSubRange_ACTUAL_BEHAVIOR() {
        objectSheet.println();
        List<Stream<Sheet.Point>> pointsCStreams = objectSheet.pointsC().toList();
        assertEquals(objectSheet.columnCount(), pointsCStreams.size());

        for (int i = 0; i < objectSheet.columnCount(); i++) {
            List<Sheet.Point> streamContent = pointsCStreams.get(i).toList();
            assertEquals(objectSheet.rowCount(), streamContent.size());
            for (int j = 0; j < objectSheet.rowCount(); j++) {
                assertEquals(Sheet.Point.of(j, i), streamContent.get(j));
            }
        }

        List<Stream<Sheet.Point>> actualPointsCStreams = objectSheet.pointsC(0, objectSheet.columnCount()).toList();
        assertEquals(objectSheet.columnCount(), actualPointsCStreams.size());
        List<Sheet.Point> firstColPoints = actualPointsCStreams.get(0).toList();
        assertEquals(objectSheet.rowCount(), firstColPoints.size());
        assertEquals(Sheet.Point.of(0, 0), firstColPoints.get(0));
        assertEquals(Sheet.Point.of(1, 0), firstColPoints.get(1));
        assertEquals(Sheet.Point.of(2, 0), firstColPoints.get(2));
    }

    @Test
    public void testPointsC_overload_emptyRange() {
        assertTrue(objectSheet.pointsC(1, 1).toList().isEmpty());
    }

    @Test
    public void testStreamH_emptyRange() {
        assertTrue(objectSheet.streamH(1, 1).toList().isEmpty());
    }

    @Test
    public void testStreamV_emptyRange() {
        assertTrue(objectSheet.streamV(1, 1).toList().isEmpty());
    }

    @Test
    public void testStreamR_emptyRange() {
        assertTrue(objectSheet.streamR(1, 1).toList().isEmpty());
    }

    @Test
    public void testStreamC_emptyRange() {
        assertTrue(objectSheet.streamC(1, 1).toList().isEmpty());
    }

    @Test
    public void testRows_pairStream_emptyRange() {
        assertTrue(objectSheet.rows(1, 1).toList().isEmpty());
    }

    @Test
    public void testColumns_pairStream_emptyRange() {
        assertTrue(objectSheet.columns(1, 1).toList().isEmpty());
    }

    @Test
    public void testForEachH_Original() {
        Map<String, Object> collected = new LinkedHashMap<>();
        objectSheet.forEachH((r, c, v) -> collected.put(r + "-" + c, v));

        assertEquals("V11", collected.get("R1-C1"));
        assertEquals(true, collected.get("R2-C3"));
        assertNull(collected.get("R1-C3"));
        assertEquals(9, collected.size());
        List<String> expectedOrder = Arrays.asList("R1-C1", "R1-C2", "R1-C3", "R2-C1", "R2-C2", "R2-C3", "R3-C1", "R3-C2", "R3-C3");
        assertEquals(expectedOrder, new ArrayList<>(collected.keySet()));
    }

    @Test
    public void testToDatasetH_uninitialized() {
        Sheet<String, String, String> uninit = new Sheet<>(rowKeys, colKeys);
        Dataset ds = uninit.toDatasetH();
        assertEquals(CommonUtil.toList("C1", "C2", "C3"), ds.columnNames());
        assertEquals(3, ds.size());
        assertNull(ds.moveToRow(0).get("C1"));
    }

    @Test
    public void testToArrayH_uninitialized() {
        Sheet<String, String, String> uninit = new Sheet<>(rowKeys, colKeys);
        Object[][] arr = uninit.toArrayH();
        assertEquals(3, arr.length);
        assertEquals(3, arr[0].length);
        assertNull(arr[0][0]);
    }

    @Test
    public void testToArrayH_typed() {
        Sheet<String, String, String> stringSheet = new Sheet<>(Arrays.asList("R1"), Arrays.asList("C1", "C2"));
        stringSheet.set("R1", "C1", "S11");
        stringSheet.set("R1", "C2", "S12");

        String[][] arr = stringSheet.toArrayH(String.class);
        assertEquals(1, arr.length);
        assertEquals(2, arr[0].length);
        assertEquals("S11", arr[0][0]);

        assertThrows(ArrayStoreException.class, () -> {
            objectSheet.toArrayH(Integer.class);
        });
    }

    @Test
    public void testToArrayV_uninitialized() {
        Sheet<String, String, String> uninit = new Sheet<>(rowKeys, colKeys);
        Object[][] arr = uninit.toArrayV();
        assertEquals(3, arr.length);
        assertEquals(3, arr[0].length);
        assertNull(arr[0][0]);
    }

    @Test
    public void testToArrayV_typed() {
        Sheet<String, String, String> stringSheet = new Sheet<>(Arrays.asList("R1", "R2"), Arrays.asList("C1"));
        stringSheet.set("R1", "C1", "S11");
        stringSheet.set("R2", "C1", "S21");

        String[][] arr = stringSheet.toArrayV(String.class);
        assertEquals(1, arr.length);
        assertEquals(2, arr[0].length);
        assertEquals("S11", arr[0][0]);
        assertEquals("S21", arr[0][1]);

        assertThrows(ArrayStoreException.class, () -> {
            objectSheet.toArrayV(UUID.class);
        });
    }

    @Test
    public void testApplyIfNotEmpty_nonEmptySheet() {
        u.Optional<Integer> result = objectSheet.applyIfNotEmpty(s -> (int) s.nonNullValueCount());
        assertTrue(result.isPresent());
        assertEquals(4, result.get());
    }

    @Test
    public void testApplyIfNotEmpty_emptySheet() {
        Sheet<String, String, String> emptyS = Sheet.empty();
        u.Optional<Integer> result = emptyS.applyIfNotEmpty(s -> (int) s.nonNullValueCount());
        assertFalse(result.isPresent());
    }

    @Test
    public void testAcceptIfNotEmpty_nonEmptySheet() {
        List<String> temp = new ArrayList<>();
        If.OrElse result = objectSheet.acceptIfNotEmpty(s -> temp.add(s.get("R1", "C1").toString()));
        assertEquals(Arrays.asList("V11"), temp);
        assertSame(If.OrElse.TRUE, result);
    }

    @Test
    public void testAcceptIfNotEmpty_emptySheet() {
        Sheet<String, String, String> emptyS = Sheet.empty();
        List<String> temp = new ArrayList<>();
        If.OrElse result = emptyS.acceptIfNotEmpty(s -> temp.add("should_not_run"));
        assertTrue(temp.isEmpty());
        assertSame(If.OrElse.FALSE, result);
    }

    @Test
    public void testPrintln_defaultToStdout() {
        assertDoesNotThrow(() -> objectSheet.println());
    }

    @Test
    public void testPrintln_keysToStdout() {
        assertDoesNotThrow(() -> objectSheet.println(Arrays.asList("R1"), Arrays.asList("C1")));
    }

    @Test
    public void testPrintln_toWriter_full() {
        assertDoesNotThrow(() -> objectSheet.println(stringWriter));
        String output = stringWriter.toString();
        assertTrue(output.length() > 0);
        assertTrue(output.contains("R1"));
        assertTrue(output.contains("C1"));
        assertTrue(output.contains("V11"));
        assertTrue(output.contains("true"));
        assertTrue(output.contains("null"));
    }

    @Test
    public void testPrintln_toWriter_subset() {
        assertDoesNotThrow(() -> objectSheet.println(Arrays.asList("R1", "R2"), Arrays.asList("C1", "C3"), stringWriter));
        String output = stringWriter.toString();
        assertTrue(output.length() > 0);
        assertTrue(output.contains("R1"));
        assertTrue(output.contains("R2"));
        assertFalse(output.contains("R3"));
        assertTrue(output.contains("C1"));
        assertTrue(output.contains("C3"));
        assertFalse(output.contains("C2"));
        assertTrue(output.contains("V11"));
        assertTrue(output.contains("true"));
        assertFalse(output.contains("V12"));
    }

    @Test
    public void testPrintln_toWriter_emptySheet() {
        Sheet<String, String, String> emptySheet = new Sheet<>();
        assertDoesNotThrow(() -> emptySheet.println(stringWriter));
        String output = stringWriter.toString();
        assertTrue(output.contains("+---+"));
    }

    @Test
    public void testPrintln_toWriter_emptyKeySetsButDataExists() {
        assertDoesNotThrow(() -> objectSheet.println(Collections.emptyList(), Collections.emptyList(), stringWriter));
        String output = stringWriter.toString();
        assertTrue(output.contains("+---+"));
    }

    @Test
    public void testPrintln_withInvalidKeysToWriter() {
        assertThrows(IllegalArgumentException.class, () -> objectSheet.println(Arrays.asList("RX"), colKeys, stringWriter));
        assertThrows(IllegalArgumentException.class, () -> objectSheet.println(rowKeys, Arrays.asList("CX"), stringWriter));
    }

    @Test
    public void testPrintln_nullWriter() {
        assertThrows(IllegalArgumentException.class, () -> objectSheet.println((Writer) null));
        assertThrows(IllegalArgumentException.class, () -> objectSheet.println(rowKeys, colKeys, null));
    }

    @Test
    public void testCellEqualityAndHashCode() {
        Sheet.Cell<String, String, Integer> cell1 = Sheet.Cell.of("R", "C", 123);
        Sheet.Cell<String, String, Integer> cell2 = Sheet.Cell.of("R", "C", 123);
        Sheet.Cell<String, String, Integer> cell3 = Sheet.Cell.of("R", "X", 123);
        Sheet.Cell<String, String, Integer> cell4 = Sheet.Cell.of("R", "C", 456);

        assertEquals(cell1, cell2);
        assertNotEquals(cell1, cell3);
        assertNotEquals(cell1, cell4);
        assertEquals(cell1.hashCode(), cell2.hashCode());
        assertNotEquals(cell1.hashCode(), cell3.hashCode());
    }

    @Test
    public void testPointOf_cached() {
        Sheet.Point p1 = Sheet.Point.of(0, 0);
        Sheet.Point p2 = Sheet.Point.of(0, 0);
        assertSame(p1, p2, "Points (0,0) should be cached and thus the same instance");
        assertEquals(0, p1.rowIndex());
        assertEquals(0, p1.columnIndex());

        Sheet.Point p3 = Sheet.Point.of(Sheet.Point.ZERO.rowIndex(), Sheet.Point.ZERO.columnIndex());
        assertSame(Sheet.Point.ZERO, p3, "Point.ZERO should be cached");

        Sheet.Point p_max_cache = Sheet.Point.of(127, 127);
        Sheet.Point p_max_cache_again = Sheet.Point.of(127, 127);
        assertSame(p_max_cache, p_max_cache_again, "Points at edge of cache should be cached");
    }

    @Test
    public void testPointOf_notCached() {
        Sheet.Point p_outside_cache1 = Sheet.Point.of(128, 128);
        Sheet.Point p_outside_cache2 = Sheet.Point.of(128, 128);
        assertNotSame(p_outside_cache1, p_outside_cache2, "Points outside cache range should be new instances");
        assertEquals(p_outside_cache1, p_outside_cache2, "Points outside cache should still be equal by value");

        Sheet.Point p_mixed_cache = Sheet.Point.of(0, 128);
        Sheet.Point p_mixed_cache_again = Sheet.Point.of(0, 128);
        assertNotSame(p_mixed_cache, p_mixed_cache_again);
        assertEquals(p_mixed_cache, p_mixed_cache_again);

    }

    @Test
    public void testPointEqualityAndHashCode() {
        Sheet.Point p1 = Sheet.Point.of(10, 20);
        Sheet.Point p2 = Sheet.Point.of(10, 20);
        Sheet.Point p3 = Sheet.Point.of(10, 21);
        Sheet.Point p4 = Sheet.Point.of(200, 300);
        Sheet.Point p5 = Sheet.Point.of(200, 300);

        assertEquals(p1, p2);
        assertNotEquals(p1, p3);
        assertEquals(p4, p5);
        assertNotEquals(p1, p4);

        assertEquals(p1.hashCode(), p2.hashCode());
        assertNotEquals(p1.hashCode(), p3.hashCode());
        assertEquals(p4.hashCode(), p5.hashCode());
    }

    @Test
    public void testRowsWithCollection() {
        List<List<Integer>> rows = Arrays.asList(Arrays.asList(1, 2, 3), Arrays.asList(4, 5, 6), Arrays.asList(7, 8, 9));
        Sheet<String, String, Integer> sheetLocal = Sheet.rows(upperRowKeys, colKeys, rows);
        assertEquals(5, sheetLocal.get("R2", "C2"));
    }

    @Test
    public void testColumnsWithCollection() {
        List<List<Integer>> columns = Arrays.asList(Arrays.asList(1, 4, 7), Arrays.asList(2, 5, 8), Arrays.asList(3, 6, 9));
        Sheet<String, String, Integer> sheetLocal = Sheet.columns(upperRowKeys, colKeys, columns);
        assertEquals(5, sheetLocal.get("R2", "C2"));
    }

    @Test
    public void testGetPut() {
        Sheet<String, String, Integer> uninitSheet = new Sheet<>(upperRowKeys, colKeys);
        assertNull(uninitSheet.get("R1", "C1"));
        uninitSheet.set("R1", "C1", 100);
        assertEquals(Integer.valueOf(100), uninitSheet.get("R1", "C1"));
    }

    @Test
    public void testGetPutByIndex() {
        Sheet<String, String, Integer> uninitSheet = new Sheet<>(upperRowKeys, colKeys);
        assertNull(uninitSheet.get(0, 0));
        uninitSheet.set(0, 0, 100);
        assertEquals(Integer.valueOf(100), uninitSheet.get(0, 0));
    }

    @Test
    public void testGetPutByPoint() {
        Sheet<String, String, Integer> uninitSheet = new Sheet<>(upperRowKeys, colKeys);
        Point point = Point.of(1, 1);
        assertNull(uninitSheet.get(point));
        uninitSheet.set(point, 200);
        assertEquals(Integer.valueOf(200), uninitSheet.get(point));
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

    @Test
    public void testRemove() {
        Sheet<String, String, Integer> uninitSheet = new Sheet<>(upperRowKeys, colKeys);
        uninitSheet.set("R1", "C1", 100);
        assertEquals(Integer.valueOf(100), uninitSheet.remove("R1", "C1"));
        assertNull(uninitSheet.get("R1", "C1"));
    }

    @Test
    public void testRemoveByIndex() {
        sheet.set(0, 0, 100);
        assertEquals(Integer.valueOf(100), sheet.remove(0, 0));
        assertNull(sheet.get(0, 0));
    }

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
    public void testUpdateAllWithIndices() {
        sheet.updateAll((rowIndex, columnIndex) -> rowIndex * 10 + columnIndex);

        assertEquals(Integer.valueOf(0), sheet.get(0, 0));
        assertEquals(Integer.valueOf(11), sheet.get(1, 1));
        assertEquals(Integer.valueOf(22), sheet.get(2, 2));
    }

    @Test
    public void testUpdateAllWithKeys() {
        Sheet<String, String, Integer> uninitSheet = new Sheet<>(upperRowKeys, colKeys);
        uninitSheet.updateAll((rowKey, columnKey, value) -> {
            int rowNum = Integer.parseInt(rowKey.substring(1));
            int colNum = Integer.parseInt(columnKey.substring(1));
            return rowNum * colNum;
        });

        assertEquals(Integer.valueOf(1), uninitSheet.get("R1", "C1"));
        assertEquals(Integer.valueOf(4), uninitSheet.get("R2", "C2"));
        assertEquals(Integer.valueOf(9), uninitSheet.get("R3", "C3"));
    }

    @Test
    public void testReplaceIf() {
        Sheet<String, String, Integer> uninitSheet = new Sheet<>(upperRowKeys, colKeys);
        uninitSheet.set("R1", "C1", 1);
        uninitSheet.set("R2", "C2", 2);
        uninitSheet.set("R3", "C3", 3);

        uninitSheet.replaceIf(v -> v != null && v > 1, 999);

        assertEquals(Integer.valueOf(1), uninitSheet.get("R1", "C1"));
        assertEquals(Integer.valueOf(999), uninitSheet.get("R2", "C2"));
        assertEquals(Integer.valueOf(999), uninitSheet.get("R3", "C3"));
    }

    @Test
    public void testReplaceIfWithIndices() {
        Sheet<String, String, Integer> uninitSheet = new Sheet<>(upperRowKeys, colKeys);
        uninitSheet.replaceIf((rowIndex, columnIndex) -> rowIndex == columnIndex, 100);

        assertEquals(Integer.valueOf(100), uninitSheet.get(0, 0));
        assertEquals(Integer.valueOf(100), uninitSheet.get(1, 1));
        assertEquals(Integer.valueOf(100), uninitSheet.get(2, 2));
        assertNull(uninitSheet.get(0, 1));
    }

    @Test
    public void testReplaceIfWithKeys() {
        Sheet<String, String, Integer> uninitSheet = new Sheet<>(upperRowKeys, colKeys);
        uninitSheet.replaceIf((rowKey, columnKey, value) -> "R1".equals(rowKey) && "C1".equals(columnKey), 100);

        assertEquals(Integer.valueOf(100), uninitSheet.get("R1", "C1"));
        assertNull(uninitSheet.get("R1", "C2"));
    }

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
    public void testModifyFrozenSheet() {
        Sheet<String, String, Integer> uninitSheet = new Sheet<>(upperRowKeys, colKeys);
        uninitSheet.freeze();
        assertThrows(IllegalStateException.class, () -> uninitSheet.set("R1", "C1", 100));
    }

    @Test
    public void testCellsHRange() {
        Sheet<String, String, Integer> uninitSheet = new Sheet<>(upperRowKeys, colKeys);
        uninitSheet.set("R1", "C1", 1);
        uninitSheet.set("R2", "C2", 2);

        List<Cell<String, String, Integer>> cells = uninitSheet.cellsH(1, 2).toList();
        assertEquals(3, cells.size());
    }

    @Test
    public void testToArrayHTyped() {
        Sheet<String, String, Integer> uninitSheet = new Sheet<>(upperRowKeys, colKeys);
        uninitSheet.set("R1", "C1", 1);
        uninitSheet.set("R1", "C2", 2);
        uninitSheet.set("R2", "C1", 3);
        uninitSheet.set("R2", "C2", 4);

        Integer[][] array = uninitSheet.toArrayH(Integer.class);

        assertEquals(3, array.length);
        assertEquals(3, array[0].length);
        assertEquals(Integer.valueOf(1), array[0][0]);
        assertEquals(Integer.valueOf(2), array[0][1]);
    }

    @Test
    public void testToArrayVTyped() {
        Sheet<String, String, Integer> uninitSheet = new Sheet<>(upperRowKeys, colKeys);
        uninitSheet.set("R1", "C1", 1);
        uninitSheet.set("R1", "C2", 2);
        uninitSheet.set("R2", "C1", 3);
        uninitSheet.set("R2", "C2", 4);

        Integer[][] array = uninitSheet.toArrayV(Integer.class);

        assertEquals(3, array.length);
        assertEquals(3, array[0].length);
        assertEquals(Integer.valueOf(1), array[0][0]);
        assertEquals(Integer.valueOf(3), array[0][1]);
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
        assertNotNull(sheet);
    }

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

        Point cached1 = Point.of(0, 0);
        Point cached2 = Point.of(0, 0);
        assertSame(cached1, cached2);
        assertEquals(Point.ZERO, cached1);
    }

    @Test
    public void testEmptySheetOperations() {
        Sheet<String, String, Integer> emptySheet = new Sheet<>();

        assertEquals(0, emptySheet.nonNullValueCount());
        assertTrue(emptySheet.cellsH().toList().isEmpty());
        assertTrue(emptySheet.streamH().toList().isEmpty());

        Map<String, Map<String, Integer>> rowMap = emptySheet.rowsMap();
        assertTrue(rowMap.isEmpty());
    }

    @Test
    public void testNullValueHandling() {
        Sheet<String, String, Integer> uninitSheet = new Sheet<>(upperRowKeys, colKeys);
        uninitSheet.set("R1", "C1", null);
        assertNull(uninitSheet.get("R1", "C1"));
        assertTrue(uninitSheet.containsValue(null));

        uninitSheet.updateAll(v -> v == null ? 0 : v);
        assertEquals(Integer.valueOf(0), uninitSheet.get("R1", "C1"));
    }

    @Test
    public void testLargeSheet() {
        List<String> largeRowKeys = new ArrayList<>();
        List<String> largeColumnKeys = new ArrayList<>();

        for (int i = 0; i < 100; i++) {
            largeRowKeys.add("R" + i);
            largeColumnKeys.add("C" + i);
        }

        Sheet<String, String, Integer> largeSheet = new Sheet<>(largeRowKeys, largeColumnKeys);

        for (int i = 0; i < 100; i++) {
            largeSheet.set(i, i, i);
        }

        assertEquals(100, largeSheet.nonNullValueCount());
        assertEquals(Integer.valueOf(50), largeSheet.get(50, 50));
    }

    @Test
    public void testCellsHWithSingleRow() {
        Sheet<String, String, Integer> uninitSheet = new Sheet<>(upperRowKeys, colKeys);
        uninitSheet.set("R2", "C1", 21);
        uninitSheet.set("R2", "C2", 22);
        uninitSheet.set("R2", "C3", 23);

        List<Cell<String, String, Integer>> cells = uninitSheet.cellsH(1, 2).toList();
        assertEquals(3, cells.size());
        assertEquals("R2", cells.get(0).rowKey());
        assertEquals(Integer.valueOf(21), cells.get(0).value());
    }

    @Test
    public void testCellsVWithSingleColumn() {
        Sheet<String, String, Integer> uninitSheet = new Sheet<>(upperRowKeys, colKeys);
        uninitSheet.set("R1", "C2", 12);
        uninitSheet.set("R2", "C2", 22);
        uninitSheet.set("R3", "C2", 32);

        List<Cell<String, String, Integer>> cells = uninitSheet.cellsV(1, 2).toList();
        assertEquals(3, cells.size());
        assertEquals("C2", cells.get(0).columnKey());
        assertEquals(Integer.valueOf(12), cells.get(0).value());
    }

    @Test
    public void testPointsHWithSingleRow() {
        List<Point> points = sheet.pointsH(1, 2).toList();
        assertEquals(3, points.size());
        assertEquals(Point.of(1, 0), points.get(0));
        assertEquals(Point.of(1, 1), points.get(1));
        assertEquals(Point.of(1, 2), points.get(2));
    }

    @Test
    public void testPointsVWithSingleColumn() {
        List<Point> points = sheet.pointsV(1, 2).toList();
        assertEquals(3, points.size());
        assertEquals(Point.of(0, 1), points.get(0));
        assertEquals(Point.of(1, 1), points.get(1));
        assertEquals(Point.of(2, 1), points.get(2));
    }

    @Test
    public void testStreamHWithSingleRow() {
        Sheet<String, String, Integer> uninitSheet = new Sheet<>(upperRowKeys, colKeys);
        uninitSheet.set("R2", "C1", 21);
        uninitSheet.set("R2", "C2", 22);
        uninitSheet.set("R2", "C3", 23);

        List<Integer> values = uninitSheet.streamH(1, 2).toList();
        assertEquals(3, values.size());
        assertEquals(Integer.valueOf(21), values.get(0));
        assertEquals(Integer.valueOf(22), values.get(1));
        assertEquals(Integer.valueOf(23), values.get(2));
    }

    @Test
    public void testStreamVWithSingleColumn() {
        Sheet<String, String, Integer> uninitSheet = new Sheet<>(upperRowKeys, colKeys);
        uninitSheet.set("R1", "C2", 12);
        uninitSheet.set("R2", "C2", 22);
        uninitSheet.set("R3", "C2", 32);

        List<Integer> values = uninitSheet.streamV(1, 2).toList();
        assertEquals(3, values.size());
        assertEquals(Integer.valueOf(12), values.get(0));
        assertEquals(Integer.valueOf(22), values.get(1));
        assertEquals(Integer.valueOf(32), values.get(2));
    }

    @Test
    public void testCellsHWithCustomRange() {
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                sheet.set(i, j, i * 10 + j);
            }
        }

        List<Cell<String, String, Integer>> cells = sheet.cellsH(0, 2).toList();
        assertEquals(6, cells.size());
        assertEquals(Integer.valueOf(0), cells.get(0).value());
        assertEquals(Integer.valueOf(12), cells.get(5).value());
    }

    @Test
    public void testCellsVWithCustomRange() {
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                sheet.set(i, j, i * 10 + j);
            }
        }

        List<Cell<String, String, Integer>> cells = sheet.cellsV(1, 3).toList();
        assertEquals(6, cells.size());
        assertEquals(Integer.valueOf(1), cells.get(0).value());
        assertEquals(Integer.valueOf(22), cells.get(5).value());
    }

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

    @Test
    public void testUninitializedSheetOperations() {
        Sheet<String, String, Integer> uninitSheet = new Sheet<>(upperRowKeys, colKeys);

        assertNull(uninitSheet.get("R1", "C1"));
        assertEquals(0, uninitSheet.nonNullValueCount());

        List<Integer> row = uninitSheet.rowValues("R1");
        assertEquals(3, row.size());
        assertTrue(row.stream().allMatch(Fn.isNull()));

        List<Integer> column = uninitSheet.columnValues("C1");
        assertEquals(3, column.size());
        assertTrue(column.stream().allMatch(Fn.isNull()));

        List<String> visited = new ArrayList<>();
        uninitSheet.forEachH((r, c, v) -> {
            assertNull(v);
            visited.add(r + "," + c);
        });
        assertEquals(9, visited.size());
    }

    @Test
    public void testFrozenSheetComprehensive() {
        Sheet<String, String, Integer> uninitSheet = new Sheet<>(upperRowKeys, colKeys);
        uninitSheet.set("R1", "C1", 100);
        uninitSheet.freeze();

        try {
            uninitSheet.set("R2", "C2", 200);
            fail("Should throw IllegalStateException");
        } catch (IllegalStateException e) {
        }

        try {
            uninitSheet.putAll(Sheet.empty());
            fail("Should throw IllegalStateException");
        } catch (IllegalStateException e) {
        }

        try {
            uninitSheet.remove("R1", "C1");
            fail("Should throw IllegalStateException");
        } catch (IllegalStateException e) {
        }

        try {
            uninitSheet.setRow("R1", Arrays.asList(1, 2, 3));
            fail("Should throw IllegalStateException");
        } catch (IllegalStateException e) {
        }

        try {
            uninitSheet.addRow("R4", Arrays.asList(1, 2, 3));
            fail("Should throw IllegalStateException");
        } catch (IllegalStateException e) {
        }

        try {
            uninitSheet.updateRow("R1", v -> v);
            fail("Should throw IllegalStateException");
        } catch (IllegalStateException e) {
        }

        try {
            uninitSheet.removeRow("R1");
            fail("Should throw IllegalStateException");
        } catch (IllegalStateException e) {
        }

        try {
            uninitSheet.clear();
            fail("Should throw IllegalStateException");
        } catch (IllegalStateException e) {
        }

        assertEquals(Integer.valueOf(100), uninitSheet.get("R1", "C1"));
        assertTrue(uninitSheet.containsCell("R1", "C1"));
    }

    @Test
    public void testSortingWithNulls() {
        Sheet<String, String, Integer> uninitSheet = new Sheet<>(upperRowKeys, colKeys);
        uninitSheet.set("R1", "C1", null);
        uninitSheet.set("R1", "C2", 3);
        uninitSheet.set("R1", "C3", 1);

        uninitSheet.sortColumnsByRowValues("R1", Comparator.nullsFirst(Comparator.naturalOrder()));

        List<String> sortedColumns = new ArrayList<>(uninitSheet.columnKeySet());
        List<Integer> sortedValues = uninitSheet.rowValues("R1");

        assertNull(sortedValues.get(0));
        assertEquals(Integer.valueOf(1), sortedValues.get(1));
        assertEquals(Integer.valueOf(3), sortedValues.get(2));
    }

    @Test
    public void testWithStringValues() {
        Sheet<Integer, String, String> stringSheet = new Sheet<>(Arrays.asList(1, 2, 3), Arrays.asList("A", "B", "C"));

        stringSheet.set(1, "A", "Hello");
        stringSheet.set(2, "B", "World");

        assertEquals("Hello", stringSheet.get(1, "A"));
        assertEquals("World", stringSheet.get(2, "B"));

        stringSheet.sortByColumnKey();
        List<String> sortedColumns = new ArrayList<>(stringSheet.columnKeySet());
        assertEquals(Arrays.asList("A", "B", "C"), sortedColumns);
    }

    @Test
    public void testAddRowAtBeginning() {
        Sheet<String, String, Integer> uninitSheet = new Sheet<>(upperRowKeys, colKeys);
        uninitSheet.set("R1", "C1", 1);
        uninitSheet.set("R2", "C1", 2);

        uninitSheet.addRow(0, "R0", Arrays.asList(0, 0, 0));

        assertEquals(4, uninitSheet.rowCount());
        assertEquals(Integer.valueOf(0), uninitSheet.get("R0", "C1"));
        assertEquals(Integer.valueOf(0), uninitSheet.get(0, 0));
        assertEquals(Integer.valueOf(1), uninitSheet.get(1, 0));
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
    public void testCopyWithInvalidRowKeys() {
        assertThrows(IllegalArgumentException.class, () -> sheet.copy(Arrays.asList("R1", "InvalidRow"), columnKeys));
    }

    @Test
    public void testCopyWithInvalidColumnKeys() {
        assertThrows(IllegalArgumentException.class, () -> sheet.copy(rowKeys, Arrays.asList("C1", "InvalidColumn")));
    }

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

    @Test
    public void testCloneWithFrozenState() {
        try {
            Sheet<String, String, Integer> uninitSheet = new Sheet<>(upperRowKeys, colKeys);
            uninitSheet.set("R1", "C1", 100);
            uninitSheet.freeze();

            Sheet<String, String, Integer> cloned = uninitSheet.clone(false);
            assertFalse(cloned.isFrozen());

            Sheet<String, String, Integer> clonedFrozen = uninitSheet.clone(true);
            assertNotNull(clonedFrozen);
        } catch (RuntimeException e) {
            assertTrue(e.getMessage().contains("Kryo is required"));
        }
    }

    @Test
    public void testPointCaching() {
        Point p1 = Point.of(10, 20);
        Point p2 = Point.of(10, 20);
        assertSame(p1, p2);

        Point p3 = Point.of(200, 300);
        Point p4 = Point.of(200, 300);
        assertNotSame(p3, p4);
        assertEquals(p3, p4);
    }

    @Test
    public void testContainsWithNull() {
        Sheet<String, String, Integer> uninitSheet = new Sheet<>(upperRowKeys, colKeys);
        assertTrue(uninitSheet.containsValueAt("R1", "C1", null));
        assertTrue(uninitSheet.isNull("R1", "C1"));

        uninitSheet.set("R1", "C1", null);
        assertTrue(uninitSheet.containsValueAt("R1", "C1", null));

        uninitSheet.set("R1", "C1", 100);
        assertFalse(uninitSheet.containsValueAt("R1", "C1", null));
    }

    @Test
    public void testSortingSingleRow() {
        Sheet<String, String, Integer> singleRowSheet = new Sheet<>(Arrays.asList("R1"), columnKeys);

        singleRowSheet.sortByRowKey();
        assertEquals(1, singleRowSheet.rowCount());
    }

    @Test
    public void testSortingEmptySheet() {
        Sheet<String, String, Integer> emptySheet = new Sheet<>();
        emptySheet.sortByRowKey();
        emptySheet.sortByColumnKey();
        assertNotNull(emptySheet);
    }

    @Test
    public void testUpdateAllOnEmptySheet() {
        Sheet<String, String, Integer> emptySheet = new Sheet<>();
        emptySheet.updateAll(v -> 100);

        Sheet<String, String, Integer> uninitSheet = new Sheet<>(upperRowKeys, colKeys);
        uninitSheet.updateAll(v -> 100);
        assertEquals(Integer.valueOf(100), uninitSheet.get(0, 0));
    }

    @Test
    public void testConstructorWithMismatchedDataRows() {
        Object[][] data = { { 1, 2 }, { 4, 5, 6 }, { 7, 8, 9 } };
        assertThrows(IllegalArgumentException.class, () -> new Sheet<>(rowKeys, columnKeys, data));
    }

    @Test
    public void testRowsFactoryWithMismatchedData() {
        List<List<Integer>> rows = Arrays.asList(Arrays.asList(1, 2), Arrays.asList(4, 5, 6), Arrays.asList(7, 8, 9));
        assertThrows(IllegalArgumentException.class, () -> Sheet.rows(rowKeys, columnKeys, rows));
    }

    @Test
    public void testStreamsWithEmptyRange() {
        assertTrue(sheet.cellsH(1, 1).toList().isEmpty());
        assertTrue(sheet.streamV(2, 2).toList().isEmpty());
        assertTrue(sheet.pointsH(0, 0).toList().isEmpty());
    }

    @Test
    public void test_tmp() {
        Sheet<String, String, Integer> sheet1 = Sheet.rows(List.of("row1", "row2"), List.of("col1", "col2"), new Integer[][] { { 1, 2 }, { 3, 4 } });

        sheet1.println("     * # ");

        Sheet<String, String, Integer> sheet2 = Sheet.rows(List.of("row2", "row3"), List.of("col2", "col3"), new Integer[][] { { 10, 20 }, { 30, 40 } });

        sheet2.println("     * # ");
        Sheet<String, String, String> merged = sheet1.merge(sheet2, (a, b) -> a + "#" + b);

        merged.println("     * # ");
        assertNotNull(merged);
    }

    // ==================== Additional tests matching source method names ====================

    @Test
    @Tag("new-test")
    public void testRowValues() {
        ImmutableList<Integer> row = sheet.rowValues("row1");
        assertNotNull(row);
        assertEquals(3, row.size());
        assertEquals(Integer.valueOf(1), row.get(0));
        assertEquals(Integer.valueOf(2), row.get(1));
        assertEquals(Integer.valueOf(3), row.get(2));
    }

    @Test
    @Tag("new-test")
    public void testRowValues_AllRows() {
        ImmutableList<Integer> row2 = sheet.rowValues("row2");
        assertEquals(Arrays.asList(4, 5, 6), new ArrayList<>(row2));

        ImmutableList<Integer> row3 = sheet.rowValues("row3");
        assertEquals(Arrays.asList(7, 8, 9), new ArrayList<>(row3));
    }

    @Test
    @Tag("new-test")
    public void testRowAsMap() {
        Map<String, Integer> rowMap = sheet.rowAsMap("row1");
        assertNotNull(rowMap);
        assertEquals(3, rowMap.size());
        assertEquals(Integer.valueOf(1), rowMap.get("col1"));
        assertEquals(Integer.valueOf(2), rowMap.get("col2"));
        assertEquals(Integer.valueOf(3), rowMap.get("col3"));
    }

    @Test
    @Tag("new-test")
    public void testRowAsMap_InvalidKey() {
        assertThrows(IllegalArgumentException.class, () -> sheet.rowAsMap("invalidRow"));
    }

    @Test
    @Tag("new-test")
    public void testRowsMap() {
        Map<String, Map<String, Integer>> rMap = sheet.rowsMap();
        assertNotNull(rMap);
        assertEquals(3, rMap.size());
        assertTrue(rMap.containsKey("row1"));
        assertTrue(rMap.containsKey("row2"));
        assertTrue(rMap.containsKey("row3"));
        assertEquals(Integer.valueOf(1), rMap.get("row1").get("col1"));
        assertEquals(Integer.valueOf(5), rMap.get("row2").get("col2"));
        assertEquals(Integer.valueOf(9), rMap.get("row3").get("col3"));
    }

    @Test
    @Tag("new-test")
    public void testColumnValues() {
        ImmutableList<Integer> col = sheet.columnValues("col1");
        assertNotNull(col);
        assertEquals(3, col.size());
        assertEquals(Integer.valueOf(1), col.get(0));
        assertEquals(Integer.valueOf(4), col.get(1));
        assertEquals(Integer.valueOf(7), col.get(2));
    }

    @Test
    @Tag("new-test")
    public void testColumnValues_AllColumns() {
        ImmutableList<Integer> col2 = sheet.columnValues("col2");
        assertEquals(Arrays.asList(2, 5, 8), new ArrayList<>(col2));

        ImmutableList<Integer> col3 = sheet.columnValues("col3");
        assertEquals(Arrays.asList(3, 6, 9), new ArrayList<>(col3));
    }

    @Test
    @Tag("new-test")
    public void testColumnAsMap() {
        Map<String, Integer> colMap = sheet.columnAsMap("col1");
        assertNotNull(colMap);
        assertEquals(3, colMap.size());
        assertEquals(Integer.valueOf(1), colMap.get("row1"));
        assertEquals(Integer.valueOf(4), colMap.get("row2"));
        assertEquals(Integer.valueOf(7), colMap.get("row3"));
    }

    @Test
    @Tag("new-test")
    public void testColumnAsMap_InvalidKey() {
        assertThrows(IllegalArgumentException.class, () -> sheet.columnAsMap("invalidCol"));
    }

    @Test
    @Tag("new-test")
    public void testColumnsMap() {
        Map<String, Map<String, Integer>> cMap = sheet.columnsMap();
        assertNotNull(cMap);
        assertEquals(3, cMap.size());
        assertTrue(cMap.containsKey("col1"));
        assertTrue(cMap.containsKey("col2"));
        assertTrue(cMap.containsKey("col3"));
        assertEquals(Integer.valueOf(1), cMap.get("col1").get("row1"));
        assertEquals(Integer.valueOf(5), cMap.get("col2").get("row2"));
        assertEquals(Integer.valueOf(9), cMap.get("col3").get("row3"));
    }

    @Test
    @Tag("new-test")
    public void testRowCount() {
        assertEquals(3, sheet.rowCount());
        assertEquals(0, emptySheet.rowCount());
    }

    @Test
    @Tag("new-test")
    public void testColumnCount() {
        assertEquals(3, sheet.columnCount());
        assertEquals(0, emptySheet.columnCount());
    }

    @Test
    @Tag("new-test")
    public void testContainsCell() {
        assertTrue(sheet.containsCell("row1", "col1"));
        assertTrue(sheet.containsCell("row3", "col3"));
        assertFalse(sheet.containsCell("invalidRow", "col1"));
        assertFalse(sheet.containsCell("row1", "invalidCol"));
        assertFalse(sheet.containsCell("invalidRow", "invalidCol"));
    }

    @Test
    @Tag("new-test")
    public void testContainsValueAt() {
        assertTrue(sheet.containsValueAt("row1", "col1", 1));
        assertTrue(sheet.containsValueAt("row2", "col2", 5));
        assertFalse(sheet.containsValueAt("row1", "col1", 999));
        assertFalse(sheet.containsValueAt("row1", "col1", null));
    }

    @Test
    @Tag("new-test")
    public void testContainsValueAt_NullValue() {
        Sheet<String, String, Integer> s = Sheet.rows(List.of("r1"), List.of("c1"), new Integer[][] { { null } });
        assertTrue(s.containsValueAt("r1", "c1", null));
        assertFalse(s.containsValueAt("r1", "c1", 1));
    }

    @Test
    @Tag("new-test")
    public void testSwapRows_SameRow() {
        sheet.swapRows("row1", "row1");
        assertEquals(Arrays.asList(1, 2, 3), new ArrayList<>(sheet.rowValues("row1")));
    }

    @Test
    @Tag("new-test")
    public void testSwapRows_InvalidKey() {
        assertThrows(IllegalArgumentException.class, () -> sheet.swapRows("row1", "invalidRow"));
    }

    @Test
    @Tag("new-test")
    public void testSwapRows_FrozenSheet() {
        sheet.freeze();
        assertThrows(IllegalStateException.class, () -> sheet.swapRows("row1", "row2"));
    }

    @Test
    @Tag("new-test")
    public void testSwapColumns_SameColumn() {
        sheet.swapColumns("col1", "col1");
        assertEquals(Integer.valueOf(1), sheet.get("row1", "col1"));
    }

    @Test
    @Tag("new-test")
    public void testSwapColumns_InvalidKey() {
        assertThrows(IllegalArgumentException.class, () -> sheet.swapColumns("col1", "invalidCol"));
    }

    @Test
    @Tag("new-test")
    public void testSwapColumns_FrozenSheet() {
        sheet.freeze();
        assertThrows(IllegalStateException.class, () -> sheet.swapColumns("col1", "col2"));
    }

    @Test
    @Tag("new-test")
    public void testSortRowsByColumnValues() {
        // sortSheet has rows B,C,A and cols Y,Z,X with data {{1,2,3},{4,5,6},{7,8,9}}
        // Sort rows by column "X" values ascending: A=9, B=3, C=6 -> B(3), C(6), A(9)
        sortSheet.sortRowsByColumnValues("X", Comparator.naturalOrder());
        List<String> rowKeyList = new ArrayList<>(sortSheet.rowKeySet());
        assertEquals("B", rowKeyList.get(0));
        assertEquals("C", rowKeyList.get(1));
        assertEquals("A", rowKeyList.get(2));
    }

    @Test
    @Tag("new-test")
    public void testSortRowsByColumnValues_WithCollection() {
        sortSheet.sortRowsByColumnValues(List.of("Y", "Z"), (a, b) -> {
            int cmp = Integer.compare((Integer) a[0], (Integer) b[0]);
            return cmp != 0 ? cmp : Integer.compare((Integer) a[1], (Integer) b[1]);
        });
        List<String> rowKeyList = new ArrayList<>(sortSheet.rowKeySet());
        assertEquals("B", rowKeyList.get(0));
        assertEquals("C", rowKeyList.get(1));
        assertEquals("A", rowKeyList.get(2));
    }

    @Test
    @Tag("new-test")
    public void testSortColumnsByRowValues() {
        // sortSheet has rows B,C,A and cols Y,Z,X with data {{1,2,3},{4,5,6},{7,8,9}}
        // Sort columns by row "B" values ascending: Y=1, Z=2, X=3 -> Y(1), Z(2), X(3) (already sorted)
        sortSheet.sortColumnsByRowValues("A", Comparator.naturalOrder());
        List<String> colKeyList = new ArrayList<>(sortSheet.columnKeySet());
        assertEquals("A", new ArrayList<>(sortSheet.rowKeySet()).get(2));
        assertNotNull(colKeyList);
        assertEquals(3, colKeyList.size());
    }

    @Test
    @Tag("new-test")
    public void testSortColumnsByRowValues_WithCollection() {
        sortSheet.sortColumnsByRowValues(List.of("B", "C"), (a, b) -> {
            int cmp = Integer.compare((Integer) a[0], (Integer) b[0]);
            return cmp != 0 ? cmp : Integer.compare((Integer) a[1], (Integer) b[1]);
        });
        List<String> colKeyList = new ArrayList<>(sortSheet.columnKeySet());
        assertNotNull(colKeyList);
        assertEquals(3, colKeyList.size());
    }

    @Test
    @Tag("new-test")
    public void testNonNullValueCount() {
        assertEquals(9, sheet.nonNullValueCount());
        assertEquals(0, emptySheet.nonNullValueCount());
    }

    @Test
    @Tag("new-test")
    public void testNonNullValueCount_WithNulls() {
        // objectSheet has some null values: {{"V11","V12",null},{100,null,true},{null,null,null}}
        assertEquals(4, objectSheet.nonNullValueCount());
    }

    @Test
    @Tag("new-test")
    public void testSetWithKeys() {
        Integer old = sheet.set("row1", "col1", 100);
        assertEquals(Integer.valueOf(1), old);
        assertEquals(Integer.valueOf(100), sheet.get("row1", "col1"));
    }

    @Test
    @Tag("new-test")
    public void testSetWithKeys_NullValue() {
        Integer old = sheet.set("row1", "col1", null);
        assertEquals(Integer.valueOf(1), old);
        assertNull(sheet.get("row1", "col1"));
    }

    @Test
    @Tag("new-test")
    public void testSetWithKeys_InvalidKey() {
        assertThrows(IllegalArgumentException.class, () -> sheet.set("invalidRow", "col1", 1));
        assertThrows(IllegalArgumentException.class, () -> sheet.set("row1", "invalidCol", 1));
    }

    @Test
    @Tag("new-test")
    public void testSetWithKeys_FrozenSheet() {
        sheet.freeze();
        assertThrows(IllegalStateException.class, () -> sheet.set("row1", "col1", 100));
    }

    @Test
    @Tag("new-test")
    public void testSetWithIndices() {
        Integer old = sheet.set(0, 0, 100);
        assertEquals(Integer.valueOf(1), old);
        assertEquals(Integer.valueOf(100), sheet.get(0, 0));
    }

    @Test
    @Tag("new-test")
    public void testSetWithIndices_InvalidIndex() {
        assertThrows(IndexOutOfBoundsException.class, () -> sheet.set(-1, 0, 1));
        assertThrows(IndexOutOfBoundsException.class, () -> sheet.set(0, -1, 1));
        assertThrows(IndexOutOfBoundsException.class, () -> sheet.set(10, 0, 1));
        assertThrows(IndexOutOfBoundsException.class, () -> sheet.set(0, 10, 1));
    }

    @Test
    @Tag("new-test")
    public void testSetWithPoint() {
        Point p = Point.of(1, 2);
        Integer old = sheet.set(p, 100);
        assertEquals(Integer.valueOf(6), old);
        assertEquals(Integer.valueOf(100), sheet.get(p));
    }

    @Test
    @Tag("new-test")
    public void testSetWithPoint_FrozenSheet() {
        sheet.freeze();
        assertThrows(IllegalStateException.class, () -> sheet.set(Point.of(0, 0), 100));
    }

    @Test
    @Tag("new-test")
    public void testIsNull_WithKeys() {
        assertFalse(sheet.isNull("row1", "col1"));
        sheet.set("row1", "col1", null);
        assertTrue(sheet.isNull("row1", "col1"));
    }

    @Test
    @Tag("new-test")
    public void testIsNull_WithIndices() {
        assertFalse(sheet.isNull(0, 0));
        sheet.set(0, 0, null);
        assertTrue(sheet.isNull(0, 0));
    }

    @Test
    @Tag("new-test")
    public void testIsNull_WithPoint() {
        Point p = Point.of(0, 0);
        assertFalse(sheet.isNull(p));
        sheet.set(p, null);
        assertTrue(sheet.isNull(p));
    }

    @Test
    @Tag("new-test")
    public void testGetWithPoint_Valid() {
        Point p = Point.of(0, 0);
        assertEquals(Integer.valueOf(1), sheet.get(p));
        assertEquals(Integer.valueOf(5), sheet.get(Point.of(1, 1)));
        assertEquals(Integer.valueOf(9), sheet.get(Point.of(2, 2)));
    }

    @Test
    @Tag("new-test")
    public void testCopy_NoArgs() {
        Sheet<String, String, Integer> copy = sheet.copy();
        assertNotSame(sheet, copy);
        assertEquals(sheet.rowCount(), copy.rowCount());
        assertEquals(sheet.columnCount(), copy.columnCount());
        assertEquals(sheet.get("row1", "col1"), copy.get("row1", "col1"));
        // Verify it's a deep copy
        copy.set("row1", "col1", 999);
        assertEquals(Integer.valueOf(1), sheet.get("row1", "col1"));
    }

    @Test
    @Tag("new-test")
    public void testClone_NoArgs() {
        Sheet<String, String, Integer> cloned = sheet.clone();
        assertNotSame(sheet, cloned);
        assertEquals(sheet.rowCount(), cloned.rowCount());
        assertEquals(sheet.columnCount(), cloned.columnCount());
        assertEquals(sheet.get("row1", "col1"), cloned.get("row1", "col1"));
        assertFalse(cloned.isFrozen());
    }

    @Test
    @Tag("new-test")
    public void testClone_WithFreeze() {
        Sheet<String, String, Integer> cloned = sheet.clone(true);
        assertTrue(cloned.isFrozen());
        assertThrows(IllegalStateException.class, () -> cloned.set("row1", "col1", 100));
    }

    @Test
    @Tag("new-test")
    public void testFreeze_AlreadyFrozen() {
        sheet.freeze();
        assertTrue(sheet.isFrozen());
        // Freezing again should not throw
        sheet.freeze();
        assertTrue(sheet.isFrozen());
    }

    @Test
    @Tag("new-test")
    public void testTrimToSize_AfterRemoval() {
        sheet.removeRow("row3");
        sheet.trimToSize();
        assertEquals(2, sheet.rowCount());
        assertEquals(Integer.valueOf(1), sheet.get("row1", "col1"));
    }

    @Test
    @Tag("new-test")
    public void testIsEmpty_Various() {
        assertTrue(emptySheet.isEmpty());
        assertFalse(sheet.isEmpty());
        Sheet<String, String, Integer> keysOnly = new Sheet<>(List.of("r1"), List.of("c1"));
        assertFalse(keysOnly.isEmpty());
    }

    @Test
    @Tag("new-test")
    public void testCellsV_InvalidRange() {
        assertThrows(IndexOutOfBoundsException.class, () -> sheet.cellsV(-1, 2));
        assertThrows(IndexOutOfBoundsException.class, () -> sheet.cellsV(0, 10));
    }

    @Test
    @Tag("new-test")
    public void testPointsH_InvalidRange() {
        assertThrows(IndexOutOfBoundsException.class, () -> sheet.pointsH(-1, 2));
        assertThrows(IndexOutOfBoundsException.class, () -> sheet.pointsH(0, 10));
    }

    @Test
    @Tag("new-test")
    public void testPointsV_InvalidRange() {
        assertThrows(IndexOutOfBoundsException.class, () -> sheet.pointsV(-1, 2));
        assertThrows(IndexOutOfBoundsException.class, () -> sheet.pointsV(0, 10));
    }

    @Test
    @Tag("new-test")
    public void testStreamH_InvalidRange() {
        assertThrows(IndexOutOfBoundsException.class, () -> sheet.streamH(-1, 2));
        assertThrows(IndexOutOfBoundsException.class, () -> sheet.streamH(0, 10));
    }

    @Test
    @Tag("new-test")
    public void testStreamV_InvalidRange() {
        assertThrows(IndexOutOfBoundsException.class, () -> sheet.streamV(-1, 2));
        assertThrows(IndexOutOfBoundsException.class, () -> sheet.streamV(0, 10));
    }

    @Test
    @Tag("new-test")
    public void testCellsR_InvalidRange() {
        assertThrows(IndexOutOfBoundsException.class, () -> sheet.cellsR(-1, 2));
        assertThrows(IndexOutOfBoundsException.class, () -> sheet.cellsR(0, 10));
    }

    @Test
    @Tag("new-test")
    public void testCellsC_InvalidRange() {
        assertThrows(IndexOutOfBoundsException.class, () -> sheet.cellsC(-1, 2));
        assertThrows(IndexOutOfBoundsException.class, () -> sheet.cellsC(0, 10));
    }

    @Test
    @Tag("new-test")
    public void testPointsR_InvalidRange() {
        assertThrows(IndexOutOfBoundsException.class, () -> sheet.pointsR(-1, 2));
        assertThrows(IndexOutOfBoundsException.class, () -> sheet.pointsR(0, 10));
    }

    @Test
    @Tag("new-test")
    public void testPointsC_InvalidRange() {
        assertThrows(IndexOutOfBoundsException.class, () -> sheet.pointsC(-1, 2));
        assertThrows(IndexOutOfBoundsException.class, () -> sheet.pointsC(0, 10));
    }

    @Test
    @Tag("new-test")
    public void testStreamR_InvalidRange() {
        assertThrows(IndexOutOfBoundsException.class, () -> sheet.streamR(-1, 2));
        assertThrows(IndexOutOfBoundsException.class, () -> sheet.streamR(0, 10));
    }

    @Test
    @Tag("new-test")
    public void testStreamC_InvalidRange() {
        assertThrows(IndexOutOfBoundsException.class, () -> sheet.streamC(-1, 2));
        assertThrows(IndexOutOfBoundsException.class, () -> sheet.streamC(0, 10));
    }

    @Test
    @Tag("new-test")
    public void testRowsStream_InvalidRange() {
        assertThrows(IndexOutOfBoundsException.class, () -> sheet.rows(-1, 2));
        assertThrows(IndexOutOfBoundsException.class, () -> sheet.rows(0, 10));
    }

    @Test
    @Tag("new-test")
    public void testColumnsStream_InvalidRange() {
        assertThrows(IndexOutOfBoundsException.class, () -> sheet.columns(-1, 2));
        assertThrows(IndexOutOfBoundsException.class, () -> sheet.columns(0, 10));
    }

    @Test
    @Tag("new-test")
    public void testPrintln_Appendable() {
        sheet.println(stringWriter);
        String output = stringWriter.toString();
        assertNotNull(output);
        assertTrue(output.length() > 0);
        assertTrue(output.contains("row1"));
        assertTrue(output.contains("col1"));
    }

    @Test
    @Tag("new-test")
    public void testPrintln_PrefixString() {
        sheet.println("PREFIX");
        // Just verify it doesn't throw - output goes to stdout
        assertTrue(true);
    }

    @Test
    @Tag("new-test")
    public void testCellOf_NullValue() {
        Cell<String, String, Integer> cell = Cell.of("r1", "c1", null);
        assertEquals("r1", cell.rowKey());
        assertEquals("c1", cell.columnKey());
        assertNull(cell.value());
    }

    @Test
    @Tag("new-test")
    public void testPointOf_Caching() {
        // Small indices should be cached
        Point p1 = Point.of(0, 0);
        Point p2 = Point.of(0, 0);
        assertSame(p1, p2);
    }

    @Test
    @Tag("new-test")
    public void testPointOf_LargeIndices() {
        Point p = Point.of(1000, 2000);
        assertEquals(1000, p.rowIndex());
        assertEquals(2000, p.columnIndex());
    }

    @Test
    @Tag("new-test")
    public void testHashCode_ConsistentWithEquals() {
        Sheet<String, String, Integer> copy = sheet.copy();
        assertEquals(sheet.hashCode(), copy.hashCode());
        assertEquals(sheet, copy);
    }

    @Test
    @Tag("new-test")
    public void testEquals_DifferentType() {
        assertNotEquals(sheet, "not a sheet");
        assertNotEquals(sheet, null);
    }

    @Test
    @Tag("new-test")
    public void testToString_NotNull() {
        String str = sheet.toString();
        assertNotNull(str);
        assertTrue(str.length() > 0);
    }

    @Test
    @Tag("new-test")
    public void testToDatasetH_WithData() {
        Dataset ds = sheet.toDatasetH();
        assertNotNull(ds);
    }

    @Test
    @Tag("new-test")
    public void testToDatasetV_WithData() {
        Dataset ds = sheet.toDatasetV();
        assertNotNull(ds);
    }

    @Test
    @Tag("new-test")
    public void testApply_TransformSheet() {
        int result = sheet.apply(s -> s.rowCount() * s.columnCount());
        assertEquals(9, result);
    }

    @Test
    @Tag("new-test")
    public void testAccept_ConsumeSheet() {
        final boolean[] called = { false };
        sheet.accept(s -> {
            called[0] = true;
            assertEquals(3, s.rowCount());
        });
        assertTrue(called[0]);
    }

    @Test
    @Tag("new-test")
    public void testAcceptIfNotEmpty_OnEmptySheet() {
        final boolean[] called = { false };
        emptySheet.acceptIfNotEmpty(s -> called[0] = true);
        assertFalse(called[0]);
    }

    @Test
    @Tag("new-test")
    public void testApplyIfNotEmpty_OnEmptySheet() {
        u.Optional<Integer> result = emptySheet.applyIfNotEmpty(s -> s.rowCount());
        assertFalse(result.isPresent());
    }

    @Test
    @Tag("new-test")
    public void testApplyIfNotEmpty_OnNonEmptySheet() {
        u.Optional<Integer> result = sheet.applyIfNotEmpty(s -> s.rowCount());
        assertTrue(result.isPresent());
        assertEquals(Integer.valueOf(3), result.get());
    }

    // ==================== @Tag("2025") tests for untested methods ====================

    @Test
    @Tag("2025")
    public void testRowsFactoryWithNullParams() {
        // rows(null, null, (List)null) should produce empty sheet
        Sheet<String, String, Object> s = Sheet.rows(null, null, (List<List<Object>>) null);
        assertNotNull(s);
        assertEquals(0, s.rowCount());
        assertEquals(0, s.columnCount());
        assertTrue(s.isEmpty());
    }

    @Test
    @Tag("2025")
    public void testRowsFactoryWithEmptyLists() {
        Sheet<String, String, Integer> s = Sheet.rows(Collections.emptyList(), Collections.emptyList(), Collections.emptyList());
        assertNotNull(s);
        assertEquals(0, s.rowCount());
        assertEquals(0, s.columnCount());
        assertTrue(s.isEmpty());
    }

    @Test
    @Tag("2025")
    public void testColumnsFactoryWithCollections() {
        List<List<Integer>> colsData = Arrays.asList(Arrays.asList(1, 4, 7), Arrays.asList(2, 5, 8), Arrays.asList(3, 6, 9));
        Sheet<String, String, Integer> s = Sheet.columns(rowKeys, columnKeys, colsData);
        assertEquals(Integer.valueOf(1), s.get("row1", "col1"));
        assertEquals(Integer.valueOf(5), s.get("row2", "col2"));
        assertEquals(Integer.valueOf(9), s.get("row3", "col3"));
    }

    @Test
    @Tag("2025")
    public void testColumnsFactoryWithCollections_MismatchedColumnSize() {
        List<List<Integer>> colsData = Arrays.asList(Arrays.asList(1, 4), Arrays.asList(2, 5, 8));
        List<String> rk = Arrays.asList("R1", "R2", "R3");
        List<String> ck = Arrays.asList("C1", "C2");
        assertThrows(IllegalArgumentException.class, () -> Sheet.columns(rk, ck, colsData));
    }

    @Test
    @Tag("2025")
    public void testColumnsFactoryWithEmptyCollections() {
        Sheet<String, String, Integer> s = Sheet.columns(Arrays.asList("R1", "R2"), Arrays.asList("C1", "C2"), Collections.emptyList());
        assertNotNull(s);
        assertEquals(2, s.rowCount());
        assertEquals(2, s.columnCount());
        assertNull(s.get("R1", "C1"));
    }

    @Test
    @Tag("2025")
    public void testColumnsFactoryWithNullCollections() {
        Sheet<String, String, Object> s = Sheet.columns(null, null, (List<List<Object>>) null);
        assertNotNull(s);
        assertEquals(0, s.rowCount());
        assertEquals(0, s.columnCount());
    }

    @Test
    @Tag("2025")
    public void testRowValues_UninitializedSheet() {
        Sheet<String, String, Integer> uninitSheet = new Sheet<>(rowKeys, columnKeys);
        ImmutableList<Integer> row = uninitSheet.rowValues("row1");
        assertEquals(3, row.size());
        assertNull(row.get(0));
        assertNull(row.get(1));
        assertNull(row.get(2));
    }

    @Test
    @Tag("2025")
    public void testRowValues_InvalidKey() {
        assertThrows(IllegalArgumentException.class, () -> sheet.rowValues("invalidRow"));
    }

    @Test
    @Tag("2025")
    public void testSetRow_InvalidKey() {
        assertThrows(IllegalArgumentException.class, () -> sheet.setRow("invalidRow", Arrays.asList(1, 2, 3)));
    }

    @Test
    @Tag("2025")
    public void testSetRow_UninitializedSheet() {
        Sheet<String, String, Integer> uninitSheet = new Sheet<>(rowKeys, columnKeys);
        uninitSheet.setRow("row1", Arrays.asList(10, 20, 30));
        assertEquals(Integer.valueOf(10), uninitSheet.get("row1", "col1"));
        assertEquals(Integer.valueOf(20), uninitSheet.get("row1", "col2"));
        assertEquals(Integer.valueOf(30), uninitSheet.get("row1", "col3"));
    }

    @Test
    @Tag("2025")
    public void testAddRow_OnFrozenSheetAtIndex() {
        sheet.freeze();
        assertThrows(IllegalStateException.class, () -> sheet.addRow(0, "newRow", Arrays.asList(1, 2, 3)));
    }

    @Test
    @Tag("2025")
    public void testAddRow_DuplicateKeyAtIndex() {
        assertThrows(IllegalArgumentException.class, () -> sheet.addRow(0, "row1", Arrays.asList(1, 2, 3)));
    }

    @Test
    @Tag("2025")
    public void testAddRow_SizeMismatchAtIndex() {
        assertThrows(IllegalArgumentException.class, () -> sheet.addRow(0, "newRow", Arrays.asList(1, 2)));
    }

    @Test
    @Tag("2025")
    public void testUpdateRow_UninitializedSheet() {
        Sheet<String, String, Integer> uninitSheet = new Sheet<>(rowKeys, columnKeys);
        uninitSheet.updateRow("row1", v -> v == null ? 42 : v);
        assertEquals(Integer.valueOf(42), uninitSheet.get("row1", "col1"));
        assertEquals(Integer.valueOf(42), uninitSheet.get("row1", "col2"));
    }

    @Test
    @Tag("2025")
    public void testRemoveRow_DataIntegrity() {
        sheet.removeRow("row2");
        assertEquals(2, sheet.rowCount());
        // Verify remaining rows maintain correct data
        assertEquals(Integer.valueOf(1), sheet.get("row1", "col1"));
        assertEquals(Integer.valueOf(7), sheet.get("row3", "col1"));
        assertEquals(Integer.valueOf(9), sheet.get("row3", "col3"));
    }

    @Test
    @Tag("2025")
    public void testRemoveRow_UninitializedSheet() {
        Sheet<String, String, Integer> uninitSheet = new Sheet<>(rowKeys, columnKeys);
        uninitSheet.removeRow("row2");
        assertEquals(2, uninitSheet.rowCount());
        assertFalse(uninitSheet.containsRow("row2"));
    }

    @Test
    @Tag("2025")
    public void testMoveRow_InvalidKey() {
        assertThrows(IllegalArgumentException.class, () -> sheet.moveRow("invalidRow", 0));
    }

    @Test
    @Tag("2025")
    public void testMoveRow_ToEnd() {
        sheet.moveRow("row1", 2);
        List<String> expectedOrder = Arrays.asList("row2", "row3", "row1");
        assertEquals(expectedOrder, new ArrayList<>(sheet.rowKeySet()));
        assertEquals(Integer.valueOf(1), sheet.get("row1", "col1"));
    }

    @Test
    @Tag("2025")
    public void testRenameRow_NullKey() {
        assertThrows(IllegalArgumentException.class, () -> sheet.renameRow("row1", null));
    }

    @Test
    @Tag("2025")
    public void testContainsRow_EmptySheet() {
        assertFalse(emptySheet.containsRow("row1"));
    }

    @Test
    @Tag("2025")
    public void testRowAsMap_UninitializedSheet() {
        Sheet<String, String, Integer> uninitSheet = new Sheet<>(rowKeys, columnKeys);
        Map<String, Integer> rowMap = uninitSheet.rowAsMap("row1");
        assertEquals(3, rowMap.size());
        assertNull(rowMap.get("col1"));
    }

    @Test
    @Tag("2025")
    public void testRowsMap_UninitializedSheet() {
        Sheet<String, String, Integer> uninitSheet = new Sheet<>(rowKeys, columnKeys);
        Map<String, Map<String, Integer>> rMap = uninitSheet.rowsMap();
        assertEquals(3, rMap.size());
        assertNull(rMap.get("row1").get("col1"));
    }

    @Test
    @Tag("2025")
    public void testRowsMap_EmptySheet() {
        Map<String, Map<String, Integer>> rMap = emptySheet.rowsMap();
        assertTrue(rMap.isEmpty());
    }

    @Test
    @Tag("2025")
    public void testColumnValues_UninitializedSheet() {
        Sheet<String, String, Integer> uninitSheet = new Sheet<>(rowKeys, columnKeys);
        ImmutableList<Integer> col = uninitSheet.columnValues("col1");
        assertEquals(3, col.size());
        assertNull(col.get(0));
    }

    @Test
    @Tag("2025")
    public void testColumnValues_InvalidKey() {
        assertThrows(IllegalArgumentException.class, () -> sheet.columnValues("invalidCol"));
    }

    @Test
    @Tag("2025")
    public void testSetColumn_InvalidKey() {
        assertThrows(IllegalArgumentException.class, () -> sheet.setColumn("invalidCol", Arrays.asList(1, 2, 3)));
    }

    @Test
    @Tag("2025")
    public void testSetColumn_UninitializedSheet() {
        Sheet<String, String, Integer> uninitSheet = new Sheet<>(rowKeys, columnKeys);
        uninitSheet.setColumn("col1", Arrays.asList(10, 20, 30));
        assertEquals(Integer.valueOf(10), uninitSheet.get("row1", "col1"));
        assertEquals(Integer.valueOf(20), uninitSheet.get("row2", "col1"));
        assertEquals(Integer.valueOf(30), uninitSheet.get("row3", "col1"));
    }

    @Test
    @Tag("2025")
    public void testAddColumn_OnFrozenSheetAtIndex() {
        sheet.freeze();
        assertThrows(IllegalStateException.class, () -> sheet.addColumn(0, "newCol", Arrays.asList(1, 2, 3)));
    }

    @Test
    @Tag("2025")
    public void testAddColumn_DuplicateKeyAtIndex() {
        assertThrows(IllegalArgumentException.class, () -> sheet.addColumn(0, "col1", Arrays.asList(1, 2, 3)));
    }

    @Test
    @Tag("2025")
    public void testAddColumn_SizeMismatchAtIndex() {
        assertThrows(IllegalArgumentException.class, () -> sheet.addColumn(0, "newCol", Arrays.asList(1, 2)));
    }

    @Test
    @Tag("2025")
    public void testUpdateColumn_UninitializedSheet() {
        Sheet<String, String, Integer> uninitSheet = new Sheet<>(rowKeys, columnKeys);
        uninitSheet.updateColumn("col1", v -> v == null ? 42 : v);
        assertEquals(Integer.valueOf(42), uninitSheet.get("row1", "col1"));
        assertEquals(Integer.valueOf(42), uninitSheet.get("row2", "col1"));
    }

    @Test
    @Tag("2025")
    public void testUpdateColumn_InvalidKey() {
        assertThrows(IllegalArgumentException.class, () -> sheet.updateColumn("invalidCol", v -> v));
    }

    @Test
    @Tag("2025")
    public void testRemoveColumn_DataIntegrity() {
        sheet.removeColumn("col2");
        assertEquals(2, sheet.columnCount());
        assertEquals(Integer.valueOf(1), sheet.get("row1", "col1"));
        assertEquals(Integer.valueOf(3), sheet.get("row1", "col3"));
        assertEquals(Integer.valueOf(7), sheet.get("row3", "col1"));
    }

    @Test
    @Tag("2025")
    public void testRemoveColumn_UninitializedSheet() {
        Sheet<String, String, Integer> uninitSheet = new Sheet<>(rowKeys, columnKeys);
        uninitSheet.removeColumn("col2");
        assertEquals(2, uninitSheet.columnCount());
        assertFalse(uninitSheet.containsColumn("col2"));
    }

    @Test
    @Tag("2025")
    public void testMoveColumn_InvalidKey() {
        assertThrows(IllegalArgumentException.class, () -> sheet.moveColumn("invalidCol", 0));
    }

    @Test
    @Tag("2025")
    public void testMoveColumn_ToEnd() {
        sheet.moveColumn("col1", 2);
        List<String> expectedOrder = Arrays.asList("col2", "col3", "col1");
        assertEquals(expectedOrder, new ArrayList<>(sheet.columnKeySet()));
        assertEquals(Integer.valueOf(1), sheet.get("row1", "col1"));
    }

    @Test
    @Tag("2025")
    public void testMoveColumn_ToSamePosition() {
        List<String> initialOrder = new ArrayList<>(sheet.columnKeySet());
        sheet.moveColumn("col2", 1);
        assertEquals(initialOrder, new ArrayList<>(sheet.columnKeySet()));
    }

    @Test
    @Tag("2025")
    public void testRenameColumn_NullKey() {
        assertThrows(IllegalArgumentException.class, () -> sheet.renameColumn("col1", null));
    }

    @Test
    @Tag("2025")
    public void testContainsColumn_EmptySheet() {
        assertFalse(emptySheet.containsColumn("col1"));
    }

    @Test
    @Tag("2025")
    public void testColumnAsMap_UninitializedSheet() {
        Sheet<String, String, Integer> uninitSheet = new Sheet<>(rowKeys, columnKeys);
        Map<String, Integer> colMap = uninitSheet.columnAsMap("col1");
        assertEquals(3, colMap.size());
        assertNull(colMap.get("row1"));
    }

    @Test
    @Tag("2025")
    public void testColumnsMap_UninitializedSheet() {
        Sheet<String, String, Integer> uninitSheet = new Sheet<>(rowKeys, columnKeys);
        Map<String, Map<String, Integer>> cMap = uninitSheet.columnsMap();
        assertEquals(3, cMap.size());
        assertNull(cMap.get("col1").get("row1"));
    }

    @Test
    @Tag("2025")
    public void testColumnsMap_EmptySheet() {
        Map<String, Map<String, Integer>> cMap = emptySheet.columnsMap();
        assertTrue(cMap.isEmpty());
    }

    @Test
    @Tag("2025")
    public void testUpdateAll_UninitializedWithIntBiFunction() {
        Sheet<String, String, Integer> uninitSheet = new Sheet<>(rowKeys, columnKeys);
        uninitSheet.updateAll((rowIdx, colIdx) -> rowIdx * 10 + colIdx);
        assertEquals(Integer.valueOf(0), uninitSheet.get(0, 0));
        assertEquals(Integer.valueOf(11), uninitSheet.get(1, 1));
        assertEquals(Integer.valueOf(22), uninitSheet.get(2, 2));
    }

    @Test
    @Tag("2025")
    public void testReplaceIf_UninitializedWithIntBiPredicate() {
        Sheet<String, String, Integer> uninitSheet = new Sheet<>(rowKeys, columnKeys);
        uninitSheet.set("row1", "col1", 5);
        uninitSheet.replaceIf((rowIdx, colIdx) -> rowIdx == 0 && colIdx == 0, 99);
        assertEquals(Integer.valueOf(99), uninitSheet.get(0, 0));
    }

    @Test
    @Tag("2025")
    public void testReplaceIf_UninitializedWithTriPredicate() {
        Sheet<String, String, Integer> uninitSheet = new Sheet<>(rowKeys, columnKeys);
        uninitSheet.set("row1", "col1", 5);
        uninitSheet.replaceIf((r, c, v) -> "row1".equals(r) && "col1".equals(c), 99);
        assertEquals(Integer.valueOf(99), uninitSheet.get("row1", "col1"));
    }

    @Test
    @Tag("2025")
    public void testSortByRowKey_UninitializedSheet() {
        Sheet<String, String, Integer> uninitSheet = new Sheet<>(Arrays.asList("C", "A", "B"), columnKeys);
        uninitSheet.sortByRowKey();
        assertEquals(Arrays.asList("A", "B", "C"), new ArrayList<>(uninitSheet.rowKeySet()));
    }

    @Test
    @Tag("2025")
    public void testSortByRowKey_DataIntegrity() {
        Sheet<String, String, Integer> s = Sheet.rows(Arrays.asList("C", "A", "B"), Arrays.asList("c1", "c2"),
                new Integer[][] { { 3, 30 }, { 1, 10 }, { 2, 20 } });
        s.sortByRowKey();
        assertEquals(Arrays.asList("A", "B", "C"), new ArrayList<>(s.rowKeySet()));
        assertEquals(Integer.valueOf(1), s.get("A", "c1"));
        assertEquals(Integer.valueOf(10), s.get("A", "c2"));
        assertEquals(Integer.valueOf(2), s.get("B", "c1"));
        assertEquals(Integer.valueOf(3), s.get("C", "c1"));
    }

    @Test
    @Tag("2025")
    public void testSortByColumnKey_DataIntegrity() {
        Sheet<String, String, Integer> s = Sheet.rows(Arrays.asList("r1", "r2"), Arrays.asList("C", "A", "B"), new Integer[][] { { 3, 1, 2 }, { 6, 4, 5 } });
        s.sortByColumnKey();
        assertEquals(Arrays.asList("A", "B", "C"), new ArrayList<>(s.columnKeySet()));
        assertEquals(Integer.valueOf(1), s.get("r1", "A"));
        assertEquals(Integer.valueOf(2), s.get("r1", "B"));
        assertEquals(Integer.valueOf(3), s.get("r1", "C"));
    }

    @Test
    @Tag("2025")
    public void testSortByColumnKey_UninitializedSheet() {
        Sheet<String, String, Integer> uninitSheet = new Sheet<>(rowKeys, Arrays.asList("C", "A", "B"));
        uninitSheet.sortByColumnKey();
        assertEquals(Arrays.asList("A", "B", "C"), new ArrayList<>(uninitSheet.columnKeySet()));
    }

    @Test
    @Tag("2025")
    public void testSortRowsByColumnValues_InvalidColumnKey() {
        assertThrows(IllegalArgumentException.class, () -> sheet.sortRowsByColumnValues("invalidCol", Comparator.naturalOrder()));
    }

    @Test
    @Tag("2025")
    public void testSortColumnsByRowValues_InvalidRowKey() {
        assertThrows(IllegalArgumentException.class, () -> sheet.sortColumnsByRowValues("invalidRow", Comparator.naturalOrder()));
    }

    @Test
    @Tag("2025")
    public void testCopy_UninitializedSheet() {
        Sheet<String, String, Integer> uninitSheet = new Sheet<>(rowKeys, columnKeys);
        Sheet<String, String, Integer> copy = uninitSheet.copy();
        assertEquals(3, copy.rowCount());
        assertEquals(3, copy.columnCount());
        assertNull(copy.get("row1", "col1"));
    }

    @Test
    @Tag("2025")
    public void testCopy_SubsetUninitializedSheet() {
        Sheet<String, String, Integer> uninitSheet = new Sheet<>(rowKeys, columnKeys);
        Sheet<String, String, Integer> copy = uninitSheet.copy(Arrays.asList("row1"), Arrays.asList("col1"));
        assertEquals(1, copy.rowCount());
        assertEquals(1, copy.columnCount());
        assertNull(copy.get("row1", "col1"));
    }

    @Test
    @Tag("2025")
    public void testMerge_OverlappingKeys() {
        Sheet<String, String, Integer> sheet1 = Sheet.rows(Arrays.asList("r1", "r2"), Arrays.asList("c1", "c2"), new Integer[][] { { 1, 2 }, { 3, 4 } });
        Sheet<String, String, Integer> sheet2 = Sheet.rows(Arrays.asList("r2", "r3"), Arrays.asList("c2", "c3"), new Integer[][] { { 10, 20 }, { 30, 40 } });
        Sheet<String, String, Integer> merged = sheet1.merge(sheet2, (a, b) -> {
            if (a == null && b == null)
                return 0;
            if (a == null)
                return b;
            if (b == null)
                return a;
            return a + b;
        });
        assertEquals(3, merged.rowCount());
        assertEquals(3, merged.columnCount());
        // r2,c2 has both values: 4 + 10 = 14
        assertEquals(Integer.valueOf(14), merged.get("r2", "c2"));
        // r1,c1 only from sheet1: 1
        assertEquals(Integer.valueOf(1), merged.get("r1", "c1"));
        // r3,c3 only from sheet2: 40
        assertEquals(Integer.valueOf(40), merged.get("r3", "c3"));
    }

    @Test
    @Tag("2025")
    public void testMerge_EmptySheets() {
        Sheet<String, String, Integer> empty1 = Sheet.empty();
        Sheet<String, String, Integer> empty2 = Sheet.empty();
        Sheet<String, String, Integer> merged = empty1.merge(empty2, (a, b) -> 0);
        assertTrue(merged.isEmpty());
    }

    @Test
    @Tag("2025")
    public void testSortByValues_AllNullData_NoReorder() {
        Sheet<String, String, Integer> allNull = Sheet.rows(Arrays.asList("r2", "r1"), Arrays.asList("c2", "c1"),
                new Integer[][] { { null, null }, { null, null } });

        allNull.sortRowsByColumnValues("c1", Comparator.nullsLast(Comparator.naturalOrder()));
        assertEquals(Arrays.asList("r2", "r1"), new ArrayList<>(allNull.rowKeySet()));

        allNull.sortRowsByColumnValues(Arrays.asList("c1", "c2"), Comparator.comparing(arr -> arr.length));
        assertEquals(Arrays.asList("r2", "r1"), new ArrayList<>(allNull.rowKeySet()));

        allNull.sortColumnsByRowValues("r1", Comparator.nullsLast(Comparator.naturalOrder()));
        assertEquals(Arrays.asList("c2", "c1"), new ArrayList<>(allNull.columnKeySet()));

        allNull.sortColumnsByRowValues(Arrays.asList("r1", "r2"), Comparator.comparing(arr -> arr.length));
        assertEquals(Arrays.asList("c2", "c1"), new ArrayList<>(allNull.columnKeySet()));
    }

    @Test
    @Tag("2025")
    public void testMerge_UninitializedSheetWithSparseValues() {
        Sheet<String, String, Integer> left = new Sheet<>(Arrays.asList("r1", "r2"), Arrays.asList("c1", "c2"));
        left.set("r1", "c1", 1);

        Sheet<String, String, Integer> right = new Sheet<>(Arrays.asList("r2", "r3"), Arrays.asList("c2", "c3"));
        right.set("r3", "c3", 5);

        Sheet<String, String, Integer> merged = left.merge(right, (a, b) -> (a == null ? 0 : a) + (b == null ? 0 : b));

        assertEquals(3, merged.rowCount());
        assertEquals(3, merged.columnCount());
        assertEquals(Integer.valueOf(1), merged.get("r1", "c1"));
        assertEquals(Integer.valueOf(0), merged.get("r2", "c2"));
        assertEquals(Integer.valueOf(5), merged.get("r3", "c3"));
    }

    @Test
    @Tag("2025")
    public void testTranspose_DataIntegrity() {
        Sheet<String, String, Integer> transposed = sheet.transpose();
        // row1: {1,2,3} -> col1 of transposed: get(colKey="row1") should have 1,4,7... no wait.
        // transpose swaps rows/columns
        // original: row1,col1 = 1; row2,col1 = 4; row3,col1 = 7
        // transposed: row=col1, col=row1 -> get("col1","row1") = 1
        assertEquals(Integer.valueOf(1), transposed.get("col1", "row1"));
        assertEquals(Integer.valueOf(4), transposed.get("col1", "row2"));
        assertEquals(Integer.valueOf(7), transposed.get("col1", "row3"));
        assertEquals(Integer.valueOf(2), transposed.get("col2", "row1"));
    }

    @Test
    @Tag("2025")
    public void testFreeze_AllMutationsPrevented() {
        sheet.freeze();
        assertThrows(IllegalStateException.class, () -> sheet.setRow("row1", Arrays.asList(1, 2, 3)));
        assertThrows(IllegalStateException.class, () -> sheet.setColumn("col1", Arrays.asList(1, 2, 3)));
        assertThrows(IllegalStateException.class, () -> sheet.removeRow("row1"));
        assertThrows(IllegalStateException.class, () -> sheet.removeColumn("col1"));
        assertThrows(IllegalStateException.class, () -> sheet.moveRow("row1", 2));
        assertThrows(IllegalStateException.class, () -> sheet.moveColumn("col1", 2));
        assertThrows(IllegalStateException.class, () -> sheet.renameRow("row1", "newRow"));
        assertThrows(IllegalStateException.class, () -> sheet.renameColumn("col1", "newCol"));
        assertThrows(IllegalStateException.class, () -> sheet.updateAll(v -> v));
    }

    @Test
    @Tag("2025")
    public void testClear_DataIntegrity() {
        sheet.clear();
        assertNull(sheet.get("row1", "col1"));
        assertNull(sheet.get("row2", "col2"));
        assertNull(sheet.get("row3", "col3"));
        assertEquals(0, sheet.nonNullValueCount());
        // Keys should still be present
        assertTrue(sheet.containsRow("row1"));
        assertTrue(sheet.containsColumn("col1"));
    }

    @Test
    @Tag("2025")
    public void testClear_UninitializedSheet() {
        Sheet<String, String, Integer> uninitSheet = new Sheet<>(rowKeys, columnKeys);
        uninitSheet.clear(); // should not throw
        assertNull(uninitSheet.get("row1", "col1"));
    }

    @Test
    @Tag("2025")
    public void testNonNullValueCount_UninitializedSheet() {
        Sheet<String, String, Integer> uninitSheet = new Sheet<>(rowKeys, columnKeys);
        assertEquals(0, uninitSheet.nonNullValueCount());
    }

    @Test
    @Tag("2025")
    public void testForEachH_UninitializedSheet() {
        Sheet<String, String, Integer> uninitSheet = new Sheet<>(rowKeys, columnKeys);
        List<String> visited = new ArrayList<>();
        uninitSheet.forEachH((r, c, v) -> {
            assertNull(v);
            visited.add(r + "-" + c);
        });
        assertEquals(9, visited.size());
        // Horizontal order: row1-col1, row1-col2, row1-col3, row2-col1, ...
        assertEquals("row1-col1", visited.get(0));
        assertEquals("row1-col2", visited.get(1));
    }

    @Test
    @Tag("2025")
    public void testForEachV_UninitializedSheet() {
        Sheet<String, String, Integer> uninitSheet = new Sheet<>(rowKeys, columnKeys);
        List<String> visited = new ArrayList<>();
        uninitSheet.forEachV((r, c, v) -> {
            assertNull(v);
            visited.add(r + "-" + c);
        });
        assertEquals(9, visited.size());
        // Vertical order: row1-col1, row2-col1, row3-col1, row1-col2, ...
        assertEquals("row1-col1", visited.get(0));
        assertEquals("row2-col1", visited.get(1));
    }

    @Test
    @Tag("2025")
    public void testForEachNonNullH_WithNulls() {
        Sheet<String, String, Integer> s = Sheet.rows(Arrays.asList("r1", "r2"), Arrays.asList("c1", "c2"), new Integer[][] { { 1, null }, { null, 4 } });
        List<Integer> collected = new ArrayList<>();
        s.forEachNonNullH((r, c, v) -> collected.add(v));
        assertEquals(2, collected.size());
        assertEquals(Integer.valueOf(1), collected.get(0));
        assertEquals(Integer.valueOf(4), collected.get(1));
    }

    @Test
    @Tag("2025")
    public void testForEachNonNullV_WithNulls() {
        Sheet<String, String, Integer> s = Sheet.rows(Arrays.asList("r1", "r2"), Arrays.asList("c1", "c2"), new Integer[][] { { 1, null }, { null, 4 } });
        List<Integer> collected = new ArrayList<>();
        s.forEachNonNullV((r, c, v) -> collected.add(v));
        assertEquals(2, collected.size());
        // Vertical order: c1(r1=1), c1(r2=null skip), c2(r1=null skip), c2(r2=4)
        assertEquals(Integer.valueOf(1), collected.get(0));
        assertEquals(Integer.valueOf(4), collected.get(1));
    }

    @Test
    @Tag("2025")
    public void testForEachNonNullH_UninitializedSheet() {
        Sheet<String, String, Integer> uninitSheet = new Sheet<>(rowKeys, columnKeys);
        List<Integer> collected = new ArrayList<>();
        uninitSheet.forEachNonNullH((r, c, v) -> collected.add(v));
        assertTrue(collected.isEmpty());
    }

    @Test
    @Tag("2025")
    public void testForEachNonNullV_UninitializedSheet() {
        Sheet<String, String, Integer> uninitSheet = new Sheet<>(rowKeys, columnKeys);
        List<Integer> collected = new ArrayList<>();
        uninitSheet.forEachNonNullV((r, c, v) -> collected.add(v));
        assertTrue(collected.isEmpty());
    }

    @Test
    @Tag("2025")
    public void testCellsH_ValueOrder() {
        List<Cell<String, String, Integer>> cells = sheet.cellsH().toList();
        assertEquals(9, cells.size());
        // row1: col1=1, col2=2, col3=3; row2: col1=4, col2=5, col3=6; ...
        assertEquals(Integer.valueOf(1), cells.get(0).value());
        assertEquals("row1", cells.get(0).rowKey());
        assertEquals("col1", cells.get(0).columnKey());
        assertEquals(Integer.valueOf(2), cells.get(1).value());
        assertEquals(Integer.valueOf(4), cells.get(3).value());
        assertEquals("row2", cells.get(3).rowKey());
    }

    @Test
    @Tag("2025")
    public void testCellsV_ValueOrder() {
        List<Cell<String, String, Integer>> cells = sheet.cellsV().toList();
        assertEquals(9, cells.size());
        // col1: row1=1, row2=4, row3=7; col2: row1=2, row2=5, ...
        assertEquals(Integer.valueOf(1), cells.get(0).value());
        assertEquals(Integer.valueOf(4), cells.get(1).value());
        assertEquals(Integer.valueOf(7), cells.get(2).value());
        assertEquals(Integer.valueOf(2), cells.get(3).value());
    }

    @Test
    @Tag("2025")
    public void testCellsR_ValueOrder() {
        List<List<Cell<String, String, Integer>>> cellRows = sheet.cellsR().map(Stream::toList).toList();
        assertEquals(3, cellRows.size());
        assertEquals(3, cellRows.get(0).size());
        assertEquals(Integer.valueOf(1), cellRows.get(0).get(0).value());
        assertEquals(Integer.valueOf(4), cellRows.get(1).get(0).value());
    }

    @Test
    @Tag("2025")
    public void testCellsC_ValueOrder() {
        List<List<Cell<String, String, Integer>>> cellCols = sheet.cellsC().map(Stream::toList).toList();
        assertEquals(3, cellCols.size());
        assertEquals(3, cellCols.get(0).size());
        // First column: row1=1, row2=4, row3=7
        assertEquals(Integer.valueOf(1), cellCols.get(0).get(0).value());
        assertEquals(Integer.valueOf(4), cellCols.get(0).get(1).value());
        assertEquals(Integer.valueOf(7), cellCols.get(0).get(2).value());
    }

    @Test
    @Tag("2025")
    public void testStreamH_ValueOrder() {
        List<Integer> values = sheet.streamH().toList();
        assertEquals(Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9), values);
    }

    @Test
    @Tag("2025")
    public void testStreamV_ValueOrder() {
        List<Integer> values = sheet.streamV().toList();
        // column by column: col1(1,4,7), col2(2,5,8), col3(3,6,9)
        assertEquals(Arrays.asList(1, 4, 7, 2, 5, 8, 3, 6, 9), values);
    }

    @Test
    @Tag("2025")
    public void testStreamR_ValueOrder() {
        List<List<Integer>> rows = sheet.streamR().map(Stream::toList).toList();
        assertEquals(3, rows.size());
        assertEquals(Arrays.asList(1, 2, 3), rows.get(0));
        assertEquals(Arrays.asList(4, 5, 6), rows.get(1));
        assertEquals(Arrays.asList(7, 8, 9), rows.get(2));
    }

    @Test
    @Tag("2025")
    public void testStreamC_ValueOrder() {
        List<List<Integer>> cols = sheet.streamC().map(Stream::toList).toList();
        assertEquals(3, cols.size());
        assertEquals(Arrays.asList(1, 4, 7), cols.get(0));
        assertEquals(Arrays.asList(2, 5, 8), cols.get(1));
        assertEquals(Arrays.asList(3, 6, 9), cols.get(2));
    }

    @Test
    @Tag("2025")
    public void testRows_PairStreamContent() {
        List<Pair<String, Stream<Integer>>> rowPairs = sheet.rows().toList();
        assertEquals(3, rowPairs.size());
        assertEquals("row1", rowPairs.get(0).left());
        assertEquals(Arrays.asList(1, 2, 3), rowPairs.get(0).right().toList());
        assertEquals("row3", rowPairs.get(2).left());
        assertEquals(Arrays.asList(7, 8, 9), rowPairs.get(2).right().toList());
    }

    @Test
    @Tag("2025")
    public void testColumns_PairStreamContent() {
        List<Pair<String, Stream<Integer>>> colPairs = sheet.columns().toList();
        assertEquals(3, colPairs.size());
        assertEquals("col1", colPairs.get(0).left());
        assertEquals(Arrays.asList(1, 4, 7), colPairs.get(0).right().toList());
        assertEquals("col3", colPairs.get(2).left());
        assertEquals(Arrays.asList(3, 6, 9), colPairs.get(2).right().toList());
    }

    @Test
    @Tag("2025")
    public void testRows_WithMapperInvalidRange() {
        assertThrows(IndexOutOfBoundsException.class, () -> sheet.rows(-1, 2, (idx, arr) -> null));
        assertThrows(IndexOutOfBoundsException.class, () -> sheet.rows(0, 10, (idx, arr) -> null));
    }

    @Test
    @Tag("2025")
    public void testColumns_WithMapperInvalidRange() {
        assertThrows(IndexOutOfBoundsException.class, () -> sheet.columns(-1, 2, (idx, arr) -> null));
        assertThrows(IndexOutOfBoundsException.class, () -> sheet.columns(0, 10, (idx, arr) -> null));
    }

    @Test
    @Tag("2025")
    public void testToDatasetH_ColumnNames() {
        Dataset ds = sheet.toDatasetH();
        assertNotNull(ds);
        assertEquals(3, ds.size());
        assertEquals(Arrays.asList("col1", "col2", "col3"), ds.columnNames());
    }

    @Test
    @Tag("2025")
    public void testToDatasetV_ColumnNames() {
        Dataset ds = sheet.toDatasetV();
        assertNotNull(ds);
        assertEquals(3, ds.size());
        assertEquals(Arrays.asList("row1", "row2", "row3"), ds.columnNames());
    }

    @Test
    @Tag("2025")
    public void testToDatasetV_UninitializedSheet() {
        Sheet<String, String, Integer> uninitSheet = new Sheet<>(rowKeys, columnKeys);
        Dataset ds = uninitSheet.toDatasetV();
        assertEquals(Arrays.asList("row1", "row2", "row3"), ds.columnNames());
        assertEquals(3, ds.size());
    }

    @Test
    @Tag("2025")
    public void testToArrayH_EmptySheet() {
        Sheet<String, String, Integer> empty = new Sheet<>();
        Object[][] arr = empty.toArrayH();
        assertEquals(0, arr.length);
    }

    @Test
    @Tag("2025")
    public void testToArrayV_EmptySheet() {
        Sheet<String, String, Integer> empty = new Sheet<>();
        Object[][] arr = empty.toArrayV();
        assertEquals(0, arr.length);
    }

    @Test
    @Tag("2025")
    public void testPrintln_WithPrefixAndAppendable() {
        StringWriter writer = new StringWriter();
        sheet.println(rowKeys, columnKeys, ">> ", writer);
        String output = writer.toString();
        assertTrue(output.contains(">> "));
        assertTrue(output.contains("row1"));
        assertTrue(output.contains("col1"));
    }

    @Test
    @Tag("2025")
    public void testPrintln_SubsetWithPrefix() {
        StringWriter writer = new StringWriter();
        sheet.println(Arrays.asList("row1"), Arrays.asList("col1", "col2"), "## ", writer);
        String output = writer.toString();
        assertTrue(output.contains("## "));
        assertTrue(output.contains("row1"));
        assertTrue(output.contains("col1"));
    }

    @Test
    @Tag("2025")
    public void testEquals_SameInstance() {
        assertTrue(sheet.equals(sheet));
    }

    @Test
    @Tag("2025")
    public void testEquals_DifferentRowKeys() {
        Sheet<String, String, Integer> other = Sheet.rows(Arrays.asList("a", "b", "c"), columnKeys, sampleData);
        assertFalse(sheet.equals(other));
    }

    @Test
    @Tag("2025")
    public void testEquals_DifferentColumnKeys() {
        Sheet<String, String, Integer> other = Sheet.rows(rowKeys, Arrays.asList("a", "b", "c"), sampleData);
        assertFalse(sheet.equals(other));
    }

    @Test
    @Tag("2025")
    public void testEquals_UninitializedSheets() {
        Sheet<String, String, Integer> uninit1 = new Sheet<>(rowKeys, columnKeys);
        Sheet<String, String, Integer> uninit2 = new Sheet<>(rowKeys, columnKeys);
        assertEquals(uninit1, uninit2);
    }

    @Test
    @Tag("2025")
    public void testHashCode_UninitializedSheets() {
        Sheet<String, String, Integer> uninit1 = new Sheet<>(rowKeys, columnKeys);
        Sheet<String, String, Integer> uninit2 = new Sheet<>(rowKeys, columnKeys);
        assertEquals(uninit1.hashCode(), uninit2.hashCode());
    }

    @Test
    @Tag("2025")
    public void testToString_EmptySheet() {
        String str = emptySheet.toString();
        assertNotNull(str);
    }

    @Test
    @Tag("2025")
    public void testToString_UninitializedSheet() {
        Sheet<String, String, Integer> uninitSheet = new Sheet<>(rowKeys, columnKeys);
        String str = uninitSheet.toString();
        assertNotNull(str);
        assertTrue(str.length() > 0);
    }

    @Test
    @Tag("2025")
    public void testPointOf_NegativeIndices() {
        // Point.of should allow any int values (no validation in factory)
        Point p = Point.of(-1, -1);
        assertEquals(-1, p.rowIndex());
        assertEquals(-1, p.columnIndex());
    }

    @Test
    @Tag("2025")
    public void testCellOf_AllNullFields() {
        Cell<String, String, Integer> cell = Cell.of(null, null, null);
        assertNull(cell.rowKey());
        assertNull(cell.columnKey());
        assertNull(cell.value());
    }

    @Test
    @Tag("2025")
    public void testCellToString_NullValue() {
        Cell<String, String, Integer> cell = Cell.of("r1", "c1", null);
        String str = cell.toString();
        assertNotNull(str);
        assertTrue(str.contains("r1"));
        assertTrue(str.contains("c1"));
    }

    // ===== Missing happy-path tests =====

    @Test
    @Tag("2025")
    public void testSwapRows_HappyPath() {
        // Before swap: row1=[1,2,3], row2=[4,5,6], row3=[7,8,9]
        sheet.swapRows("row1", "row3");

        // After swap: row3 is first, row1 is last (positions swapped)
        List<String> rowOrder = new ArrayList<>(sheet.rowKeySet());
        assertEquals("row3", rowOrder.get(0));
        assertEquals("row2", rowOrder.get(1));
        assertEquals("row1", rowOrder.get(2));

        // Data follows the keys
        assertEquals(Integer.valueOf(7), sheet.get("row3", "col1"));
        assertEquals(Integer.valueOf(8), sheet.get("row3", "col2"));
        assertEquals(Integer.valueOf(9), sheet.get("row3", "col3"));
        assertEquals(Integer.valueOf(1), sheet.get("row1", "col1"));
        assertEquals(Integer.valueOf(2), sheet.get("row1", "col2"));
        assertEquals(Integer.valueOf(3), sheet.get("row1", "col3"));
        // row2 data unchanged
        assertEquals(Integer.valueOf(4), sheet.get("row2", "col1"));
        assertEquals(Integer.valueOf(5), sheet.get("row2", "col2"));
        assertEquals(Integer.valueOf(6), sheet.get("row2", "col3"));
    }

    @Test
    @Tag("2025")
    public void testSwapRows_AdjacentRows() {
        sheet.swapRows("row1", "row2");

        List<String> rowOrder = new ArrayList<>(sheet.rowKeySet());
        assertEquals("row2", rowOrder.get(0));
        assertEquals("row1", rowOrder.get(1));
        assertEquals("row3", rowOrder.get(2));

        // Verify data integrity
        assertEquals(Integer.valueOf(4), sheet.get("row2", "col1"));
        assertEquals(Integer.valueOf(1), sheet.get("row1", "col1"));
    }

    @Test
    @Tag("2025")
    public void testSwapColumns_HappyPath() {
        // Before swap: col1=[1,4,7], col2=[2,5,8], col3=[3,6,9]
        sheet.swapColumns("col1", "col3");

        // Positions swapped
        List<String> colOrder = new ArrayList<>(sheet.columnKeySet());
        assertEquals("col3", colOrder.get(0));
        assertEquals("col2", colOrder.get(1));
        assertEquals("col1", colOrder.get(2));

        // Data follows the keys
        assertEquals(Integer.valueOf(1), sheet.get("row1", "col1"));
        assertEquals(Integer.valueOf(3), sheet.get("row1", "col3"));
        assertEquals(Integer.valueOf(7), sheet.get("row3", "col1"));
        assertEquals(Integer.valueOf(9), sheet.get("row3", "col3"));
    }

    @Test
    @Tag("2025")
    public void testSwapColumns_AdjacentColumns() {
        sheet.swapColumns("col1", "col2");

        List<String> colOrder = new ArrayList<>(sheet.columnKeySet());
        assertEquals("col2", colOrder.get(0));
        assertEquals("col1", colOrder.get(1));
        assertEquals("col3", colOrder.get(2));

        // Verify data integrity
        assertEquals(Integer.valueOf(1), sheet.get("row1", "col1"));
        assertEquals(Integer.valueOf(2), sheet.get("row1", "col2"));
    }

    @Test
    @Tag("2025")
    public void testMerge_DifferentSizes() {
        Sheet<String, String, Integer> sheet1 = Sheet.rows(Arrays.asList("r1", "r2"), Arrays.asList("c1", "c2"), new Integer[][] { { 1, 2 }, { 3, 4 } });
        Sheet<String, String, Integer> sheet2 = Sheet.rows(Arrays.asList("r1", "r2"), Arrays.asList("c1", "c2"), new Integer[][] { { 10, 20 }, { 30, 40 } });

        Sheet<String, String, Integer> merged = sheet1.merge(sheet2, (a, b) -> (a == null ? 0 : a) + (b == null ? 0 : b));
        assertEquals(Integer.valueOf(11), merged.get("r1", "c1"));
        assertEquals(Integer.valueOf(22), merged.get("r1", "c2"));
        assertEquals(Integer.valueOf(33), merged.get("r2", "c1"));
        assertEquals(Integer.valueOf(44), merged.get("r2", "c2"));
    }

    @Test
    @Tag("2025")
    public void testCopySubset_EmptySubset() {
        Sheet<String, String, Integer> copy = sheet.copy(Arrays.asList("row1"), Arrays.asList("col1"));
        assertEquals(1, copy.rowCount());
        assertEquals(1, copy.columnCount());
        assertEquals(Integer.valueOf(1), copy.get("row1", "col1"));
    }

    @Test
    @Tag("2025")
    public void testForEachNonNullH_WithNullValues() {
        Sheet<String, String, Integer> s = Sheet.rows(Arrays.asList("r1", "r2"), Arrays.asList("c1", "c2"), new Integer[][] { { 1, null }, { null, 4 } });
        List<Integer> collected = new ArrayList<>();
        s.forEachNonNullH((r, c, v) -> collected.add(v));
        assertEquals(2, collected.size());
        assertTrue(collected.contains(1));
        assertTrue(collected.contains(4));
    }

    @Test
    @Tag("2025")
    public void testForEachNonNullV_WithNullValues() {
        Sheet<String, String, Integer> s = Sheet.rows(Arrays.asList("r1", "r2"), Arrays.asList("c1", "c2"), new Integer[][] { { 1, null }, { null, 4 } });
        List<Integer> collected = new ArrayList<>();
        s.forEachNonNullV((r, c, v) -> collected.add(v));
        assertEquals(2, collected.size());
        assertTrue(collected.contains(1));
        assertTrue(collected.contains(4));
    }

    @Test
    @Tag("2025")
    public void testCloneWithFreeze_DataIntegrity() {
        Sheet<String, String, Integer> frozenClone = sheet.clone(true);
        assertTrue(frozenClone.isFrozen());
        // Data should be the same
        assertEquals(Integer.valueOf(1), frozenClone.get("row1", "col1"));
        assertEquals(Integer.valueOf(9), frozenClone.get("row3", "col3"));
        // Original should not be frozen
        assertFalse(sheet.isFrozen());
    }

    @Test
    @Tag("2025")
    public void testCloneWithoutFreeze_IndependentMutation() {
        Sheet<String, String, Integer> clone = sheet.clone(false);
        assertFalse(clone.isFrozen());
        clone.set("row1", "col1", 999);
        assertEquals(Integer.valueOf(999), clone.get("row1", "col1"));
        // Original unaffected
        assertEquals(Integer.valueOf(1), sheet.get("row1", "col1"));
    }

    @Test
    @Tag("2025")
    public void testPointOf_CachedRange() {
        // Points within cached range should be same instance
        Point p1 = Point.of(0, 0);
        Point p2 = Point.of(0, 0);
        assertSame(p1, p2);
        assertSame(Point.ZERO, p1);
    }

    @Test
    @Tag("2025")
    public void testRowsMap_DataIntegrity() {
        Map<String, Map<String, Integer>> rowsMap = sheet.rowsMap();
        assertEquals(3, rowsMap.size());
        // Verify all entries
        assertEquals(Integer.valueOf(1), rowsMap.get("row1").get("col1"));
        assertEquals(Integer.valueOf(2), rowsMap.get("row1").get("col2"));
        assertEquals(Integer.valueOf(3), rowsMap.get("row1").get("col3"));
        assertEquals(Integer.valueOf(4), rowsMap.get("row2").get("col1"));
    }

    @Test
    @Tag("2025")
    public void testColumnsMap_DataIntegrity() {
        Map<String, Map<String, Integer>> columnsMap = sheet.columnsMap();
        assertEquals(3, columnsMap.size());
        // Verify all entries
        assertEquals(Integer.valueOf(1), columnsMap.get("col1").get("row1"));
        assertEquals(Integer.valueOf(4), columnsMap.get("col1").get("row2"));
        assertEquals(Integer.valueOf(7), columnsMap.get("col1").get("row3"));
        assertEquals(Integer.valueOf(2), columnsMap.get("col2").get("row1"));
    }

}

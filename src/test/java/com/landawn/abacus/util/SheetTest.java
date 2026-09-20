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
import java.util.Random;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.util.Sheet.Cell;
import com.landawn.abacus.util.Sheet.Point;
import com.landawn.abacus.util.stream.Stream;

public class SheetTest extends SheetTestSupport {
    @Test
    public void testEqualsDifferentValues() {
        Sheet<String, String, Integer> sheet2 = Sheet.rows(rowKeys, columnKeys, new Integer[][] { { 1, 2, 3 }, { 4, 5, 6 }, { 7, 8, 100 } });
        assertFalse(sheet.equals(sheet2));
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
    public void testCellEquals() {
        Cell<String, String, Integer> c1 = Cell.of("r1", "c1", 42);
        Cell<String, String, Integer> c2 = Cell.of("r1", "c1", 42);
        Cell<String, String, Integer> c3 = Cell.of("r1", "c1", 43);

        assertEquals(c1, c2);
        assertFalse(c1.equals(c3));
    }

    @Test
    public void testEquals_DifferentRowKeys() {
        Sheet<String, String, Integer> other = Sheet.rows(Arrays.asList("a", "b", "c"), columnKeys, sampleData);
        assertFalse(sheet.equals(other));
    }

    @Test
    public void testEquals_DifferentRowKeyOrder() {
        Sheet<String, String, Integer> first = Sheet.rows(Arrays.asList("r1", "r2"), Arrays.asList("c"), new Integer[][] { { 1 }, { 2 } });
        Sheet<String, String, Integer> second = Sheet.rows(Arrays.asList("r2", "r1"), Arrays.asList("c"), new Integer[][] { { 1 }, { 2 } });

        assertEquals(Integer.valueOf(1), first.get("r1", "c"));
        assertEquals(Integer.valueOf(2), second.get("r1", "c"));
        assertNotEquals(first, second);
        assertNotEquals(first.hashCode(), second.hashCode());
    }

    @Test
    public void testEquals_DifferentColumnKeys() {
        Sheet<String, String, Integer> other = Sheet.rows(rowKeys, Arrays.asList("a", "b", "c"), sampleData);
        assertFalse(sheet.equals(other));
    }

    @Test
    public void testEquals_UninitializedSheets() {
        Sheet<String, String, Integer> uninit1 = new Sheet<>(rowKeys, columnKeys);
        Sheet<String, String, Integer> uninit2 = new Sheet<>(rowKeys, columnKeys);
        assertEquals(uninit1, uninit2);
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
    public void testEquals_DifferentType() {
        assertNotEquals(sheet, "not a sheet");
        assertNotEquals(sheet, null);
    }

    @Test
    public void testEquals_SameInstance() {
        assertTrue(sheet.equals(sheet));
    }

    @Test
    public void testEquals_InitializedAllNullVsUninitialized_ShouldBeEqual() {
        Sheet<String, String, Integer> uninit = new Sheet<>(rowKeys, columnKeys);
        Sheet<String, String, Integer> initAllNull = new Sheet<>(rowKeys, columnKeys);
        // Force initialization by setting a cell to null (this triggers init() internally)
        initAllNull.set("row1", "col1", null);
        assertTrue(initAllNull.equals(uninit), "initialized all-null sheet should equal uninitialized sheet with same keys");
        assertTrue(uninit.equals(initAllNull), "equals should be symmetric");
        assertEquals(uninit.hashCode(), initAllNull.hashCode(), "hashCode must be consistent with equals");
    }

    @Test
    public void testEquals_InitializedWithNonNullVsUninitialized_ShouldNotBeEqual() {
        Sheet<String, String, Integer> uninit = new Sheet<>(rowKeys, columnKeys);
        Sheet<String, String, Integer> initWithValue = new Sheet<>(rowKeys, columnKeys);
        initWithValue.set("row1", "col1", 42);
        assertFalse(initWithValue.equals(uninit));
        assertFalse(uninit.equals(initWithValue));
    }

    @Test
    public void testEquals_InitializedAllNullSheets_ShouldBeEqual() {
        Sheet<String, String, Integer> a = new Sheet<>(rowKeys, columnKeys);
        Sheet<String, String, Integer> b = new Sheet<>(rowKeys, columnKeys);
        a.setAt(0, 0, null);
        b.setAt(1, 1, null);
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    public void testEquals_TransposedNotEqual() {
        // A 2x3 sheet and its transposed 3x2 sheet should not be equal as the row/column keys differ.
        Sheet<String, String, Integer> orig = Sheet.rows(Arrays.asList("r1", "r2"), Arrays.asList("c1", "c2", "c3"),
                new Integer[][] { { 1, 2, 3 }, { 4, 5, 6 } });
        Sheet<String, String, Integer> tr = orig.transposed();
        assertNotEquals(orig, tr);
    }

    @Test
    public void testEqualsHandlesNullKeysAndNonSheets() {
        final Sheet<String, String, Integer> s = Sheet.rows(Arrays.asList("r1"), Arrays.asList("c1"), new Integer[][] { { 1 } });

        assertNotEquals(s, null);
        assertNotEquals(s, "not a sheet");
        assertEquals(s, s);
        assertNotEquals(s, Sheet.rows(Arrays.asList("r1", "r2"), Arrays.asList("c1"), new Integer[][] { { 1 }, { 2 } }));
    }

    @Test
    public void testHashCode() {
        Sheet<String, String, Integer> sheet2 = Sheet.rows(rowKeys, columnKeys, sampleData);
        assertEquals(sheet.hashCode(), sheet2.hashCode());
    }

    @Test
    public void testPointHashCode() {
        Point p1 = Point.of(1, 2);
        Point p2 = Point.of(1, 2);

        assertEquals(p1.hashCode(), p2.hashCode());
    }

    @Test
    public void testCellHashCode() {
        Cell<String, String, Integer> c1 = Cell.of("r1", "c1", 42);
        Cell<String, String, Integer> c2 = Cell.of("r1", "c1", 42);

        assertEquals(c1.hashCode(), c2.hashCode());
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
    public void testHashCode_ConsistentWithEquals() {
        Sheet<String, String, Integer> copy = sheet.copy();
        assertEquals(sheet.hashCode(), copy.hashCode());
        assertEquals(sheet, copy);
    }

    @Test
    public void testHashCode_UninitializedSheets() {
        Sheet<String, String, Integer> uninit1 = new Sheet<>(rowKeys, columnKeys);
        Sheet<String, String, Integer> uninit2 = new Sheet<>(rowKeys, columnKeys);
        assertEquals(uninit1.hashCode(), uninit2.hashCode());
    }

    @Test
    public void testHashCodeAndEqualsAreOrderSensitiveOnBothAxes() {
        final Sheet<String, String, Integer> a = Sheet.rows(Arrays.asList("r1", "r2"), Arrays.asList("c1", "c2"), new Integer[][] { { 1, 2 }, { 3, 4 } });
        final Sheet<String, String, Integer> b = Sheet.rows(Arrays.asList("r1", "r2"), Arrays.asList("c1", "c2"), new Integer[][] { { 1, 2 }, { 3, 4 } });

        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());

        // Reordering rows changes both the key order and the data, so the sheets differ.
        b.swapRows("r1", "r2");
        assertNotEquals(a, b);

        b.swapRows("r1", "r2");
        assertEquals(a, b);

        b.swapColumns("c1", "c2");
        assertNotEquals(a, b);
    }

    @Test
    public void testHashCodeAndEqualsTreatUninitializedAsAllNull() {
        final Sheet<String, String, Integer> uninitialized = new Sheet<>(Arrays.asList("r1"), Arrays.asList("c1"));
        final Sheet<String, String, Integer> allNull = Sheet.rows(Arrays.asList("r1"), Arrays.asList("c1"), new Integer[][] { { null } });

        assertEquals(uninitialized, allNull);
        assertEquals(allNull, uninitialized);
        assertEquals(uninitialized.hashCode(), allNull.hashCode());

        allNull.set("r1", "c1", 1);
        assertNotEquals(uninitialized, allNull);
    }

    @Test
    public void testContainsWithNullValue() {
        Sheet<String, String, Integer> s = Sheet.rows(Arrays.asList("r1"), Arrays.asList("c1"), new Integer[][] { { null } });
        assertTrue(s.containsValueAt("r1", "c1", null));
        assertFalse(s.containsValueAt("r1", "c1", 1));
    }

    @Test
    public void testContains_keyPair() {
        assertTrue(objectSheet.containsCell("R1", "C1"));
        assertTrue(objectSheet.containsCell("R3", "C3"));
        assertFalse(objectSheet.containsCell("RX", "C1"));
        assertFalse(objectSheet.containsCell("R1", "CX"));
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
    public void testContains_keyPairAndValue() {
        assertTrue(objectSheet.containsValueAt("R1", "C1", "V11"));
        assertTrue(objectSheet.containsValueAt("R1", "C3", null));
        assertFalse(objectSheet.containsValueAt("R1", "C1", "WrongValue"));
        assertFalse(objectSheet.containsValueAt("R1", "C3", "NotNull"));
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
    public void testKeyedViewOnUninitializedSheetStillReportsARemovedKey() {
        // Key resolution must not be skipped just because the Sheet has no storage yet: an uninitialized
        // Sheet used to return null for a row that had been removed, instead of reporting the missing key.
        final Sheet<String, String, Integer> s = new Sheet<>(Arrays.asList("r1", "r2"), Arrays.asList("c1"));
        final ImmutableList<Integer> rowView = s.rowValues("r1");
        final ImmutableList<Integer> columnView = s.columnValues("c1");

        assertNull(rowView.get(0));
        assertNull(columnView.get(0));

        s.removeRow("r1");
        assertThrows(IllegalArgumentException.class, () -> rowView.get(0));
        // The column view is unaffected by an unrelated row removal, but it did shrink with the Sheet.
        assertEquals(1, columnView.size());
        assertNull(columnView.get(0));

        s.removeColumn("c1");
        assertThrows(IllegalArgumentException.class, () -> columnView.get(0));

        // An out-of-range index is still reported as such, ahead of any key problem.
        final Sheet<String, String, Integer> s2 = new Sheet<>(Arrays.asList("r1"), Arrays.asList("c1"));
        final ImmutableList<Integer> v2 = s2.rowValues("r1");
        s2.removeRow("r1");
        assertThrows(IndexOutOfBoundsException.class, () -> v2.get(5));
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
    public void testClearOnFrozenSheet() {
        sheet.freeze();
        assertThrows(IllegalStateException.class, () -> {
            sheet.clear();
        });
    }

    @Test
    public void testClear_frozenSheet() {
        objectSheet.freeze();
        assertThrows(IllegalStateException.class, () -> objectSheet.clear());
    }

    @Test
    public void testClear_UninitializedSheet() {
        Sheet<String, String, Integer> uninitSheet = new Sheet<>(rowKeys, columnKeys);
        uninitSheet.clear(); // should not throw
        assertNull(uninitSheet.get("row1", "col1"));
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
    public void testSortingEmptySheet() {
        Sheet<String, String, Integer> emptySheet = new Sheet<>();
        emptySheet.sortByRowKey();
        emptySheet.sortByColumnKey();
        assertNotNull(emptySheet);
    }

    @Test
    public void testTransposeEmptySheet() {
        Sheet<String, String, Integer> transposed = emptySheet.transposed();
        assertTrue(transposed.isEmpty());
    }

    @Test
    public void testTranspose_emptySheet() {
        Sheet<String, String, String> empty = new Sheet<>();
        Sheet<String, String, String> transposedEmpty = empty.transposed();
        assertTrue(transposedEmpty.isEmpty());
        assertEquals(0, transposedEmpty.rowCount());
        assertEquals(0, transposedEmpty.columnCount());
    }

    @Test
    public void testTranspose_uninitializedSheet() {
        Sheet<String, String, String> uninitialized = new Sheet<>(upperRowKeys, colKeys);
        Sheet<String, String, String> transposed = uninitialized.transposed();
        assertEquals(colKeys, new ArrayList<>(transposed.rowKeySet()));
        assertEquals(upperRowKeys, new ArrayList<>(transposed.columnKeySet()));
        assertNull(transposed.get("C1", "R1"));
    }

    @Test
    public void testSwapRowPositionOnFrozenSheet() {
        sheet.freeze();
        assertThrows(IllegalStateException.class, () -> {
            sheet.swapRows("row1", "row2");
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
    public void testModifyFrozenSheet() {
        Sheet<String, String, Integer> uninitSheet = new Sheet<>(upperRowKeys, colKeys);
        uninitSheet.freeze();
        assertThrows(IllegalStateException.class, () -> uninitSheet.set("R1", "C1", 100));
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
    public void testLargeSheet() {
        List<String> largeRowKeys = new ArrayList<>();
        List<String> largeColumnKeys = new ArrayList<>();

        for (int i = 0; i < 100; i++) {
            largeRowKeys.add("R" + i);
            largeColumnKeys.add("C" + i);
        }

        Sheet<String, String, Integer> largeSheet = new Sheet<>(largeRowKeys, largeColumnKeys);

        for (int i = 0; i < 100; i++) {
            largeSheet.setAt(i, i, i);
        }

        assertEquals(100, largeSheet.nonNullValueCount());
        assertEquals(Integer.valueOf(50), largeSheet.getAt(50, 50));
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
        uninitSheet.forEachRowMajor((r, c, v) -> {
            assertNull(v);
            visited.add(r + "," + c);
        });
        assertEquals(9, visited.size());
    }

    @Test
    public void testForEachH_UninitializedSheet() {
        Sheet<String, String, Integer> uninitSheet = new Sheet<>(rowKeys, columnKeys);
        List<String> visited = new ArrayList<>();
        uninitSheet.forEachRowMajor((r, c, v) -> {
            assertNull(v);
            visited.add(r + "-" + c);
        });
        assertEquals(9, visited.size());
        // Horizontal order: row1-col1, row1-col2, row1-col3, row2-col1, ...
        assertEquals("row1-col1", visited.get(0));
        assertEquals("row1-col2", visited.get(1));
    }

    @Test
    public void testForEachV_UninitializedSheet() {
        Sheet<String, String, Integer> uninitSheet = new Sheet<>(rowKeys, columnKeys);
        List<String> visited = new ArrayList<>();
        uninitSheet.forEachColumnMajor((r, c, v) -> {
            assertNull(v);
            visited.add(r + "-" + c);
        });
        assertEquals(9, visited.size());
        // Vertical order: row1-col1, row2-col1, row3-col1, row1-col2, ...
        assertEquals("row1-col1", visited.get(0));
        assertEquals("row2-col1", visited.get(1));
    }

    @Test
    public void testForEachNonNullH_UninitializedSheet() {
        Sheet<String, String, Integer> uninitSheet = new Sheet<>(rowKeys, columnKeys);
        List<Integer> collected = new ArrayList<>();
        uninitSheet.forEachNonNullRowMajor((r, c, v) -> collected.add(v));
        assertTrue(collected.isEmpty());
    }

    @Test
    public void testForEachNonNullV_UninitializedSheet() {
        Sheet<String, String, Integer> uninitSheet = new Sheet<>(rowKeys, columnKeys);
        List<Integer> collected = new ArrayList<>();
        uninitSheet.forEachNonNullColumnMajor((r, c, v) -> collected.add(v));
        assertTrue(collected.isEmpty());
    }

    @Test
    public void testFrozenSheet_CannotModifyThroughSet() {
        Sheet<String, String, Integer> s = Sheet.rows(rowKeys, columnKeys, sampleData);
        s.freeze();
        assertThrows(IllegalStateException.class, () -> s.set("row1", "col1", 99));
        assertThrows(IllegalStateException.class, () -> s.remove("row1", "col1"));
        assertThrows(IllegalStateException.class, () -> s.addRow("rowX", Arrays.asList(1, 2, 3)));
        assertThrows(IllegalStateException.class, () -> s.removeRow("row1"));
    }

    @Test
    public void testKeyedViewsOnFrozenAndEmptyAxisSheets() {
        final Sheet<String, String, Integer> frozen = Sheet.rows(Arrays.asList("r1"), Arrays.asList("c1"), new Integer[][] { { 1 } });
        frozen.freeze();

        // A frozen Sheet must still be readable through its views (they allocate nothing and mutate nothing).
        assertEquals(Integer.valueOf(1), frozen.rowValues("r1").get(0));
        assertEquals(Integer.valueOf(1), frozen.columnValues("c1").get(0));

        final Sheet<String, String, Integer> noColumns = new Sheet<>(Arrays.asList("r1"), Collections.<String> emptyList());
        assertEquals(0, noColumns.rowValues("r1").size());
        assertTrue(new ArrayList<>(noColumns.rowValues("r1")).isEmpty());

        final Sheet<String, String, Integer> noRows = new Sheet<>(Collections.<String> emptyList(), Arrays.asList("c1"));
        assertEquals(0, noRows.columnValues("c1").size());
        assertTrue(new ArrayList<>(noRows.columnValues("c1")).isEmpty());
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
    public void testEmptyFactory() {
        Sheet<String, String, String> emptySheet = Sheet.empty();
        assertTrue(emptySheet.isEmpty());
        assertTrue(emptySheet.isFrozen());
        assertEquals(0, emptySheet.rowCount());
        assertEquals(0, emptySheet.columnCount());
    }

    @Test
    public void testEmptySheetOperations() {
        Sheet<String, String, Integer> emptySheet = new Sheet<>();

        assertEquals(0, emptySheet.nonNullValueCount());
        assertTrue(emptySheet.rowMajorCells().toList().isEmpty());
        assertTrue(emptySheet.rowMajorStream().toList().isEmpty());

        Map<String, Map<String, Integer>> rowMap = emptySheet.rowsMap();
        assertTrue(rowMap.isEmpty());
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
    public void testStreamsWithEmptyRange() {
        assertTrue(sheet.rowMajorCells(1, 1).toList().isEmpty());
        assertTrue(sheet.columnMajorStream(2, 2).toList().isEmpty());
        assertTrue(sheet.rowMajorPoints(0, 0).toList().isEmpty());
    }

    @Test
    public void testPointsC_overload_emptyRange() {
        assertTrue(objectSheet.columnPoints(1, 1).toList().isEmpty());
    }

    @Test
    public void testConstructorWithKeys_nullInKeysThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> new Sheet<>(Arrays.asList("R1", null), colKeys));
        assertThrows(IllegalArgumentException.class, () -> new Sheet<>(upperRowKeys, Arrays.asList("C1", null)));
    }

    @Test
    public void testConstructorWithMismatchedDataRows() {
        Object[][] data = { { 1, 2 }, { 4, 5, 6 }, { 7, 8, 9 } };
        assertThrows(IllegalArgumentException.class, () -> new Sheet<>(rowKeys, columnKeys, data));
    }

    @Test
    public void testSingleColumnSheet() {
        Sheet<String, String, Integer> singleCol = Sheet.rows(Arrays.asList("r1", "r2", "r3"), Arrays.asList("c1"), new Integer[][] { { 1 }, { 2 }, { 3 } });
        assertEquals(3, singleCol.rowCount());
        assertEquals(1, singleCol.columnCount());
        assertEquals(Integer.valueOf(1), singleCol.get("r1", "c1"));
    }

    @Test
    public void testNestedNullRowsAndColumnsAreRejectedConsistently() {
        final List<String> oneRow = Arrays.asList("r1");
        final List<String> oneColumn = Arrays.asList("c1");

        assertThrows(IllegalArgumentException.class, () -> new Sheet<String, String, Integer>(oneRow, oneColumn, new Integer[][] { null }));
        assertThrows(IllegalArgumentException.class, () -> Sheet.<String, String, Integer> rows(oneRow, oneColumn, new Integer[][] { null }));

        final List<List<Integer>> rows = new ArrayList<>();
        rows.add(null);
        assertThrows(IllegalArgumentException.class, () -> Sheet.rows(oneRow, oneColumn, rows));

        assertThrows(IllegalArgumentException.class, () -> Sheet.<String, String, Integer> columns(oneRow, oneColumn, new Integer[][] { null }));

        final List<List<Integer>> columns = new ArrayList<>();
        columns.add(null);
        assertThrows(IllegalArgumentException.class, () -> Sheet.columns(oneRow, oneColumn, columns));
    }

    @Test
    public void testFrozenSheet_RowKeySetIsImmutable() {
        Sheet<String, String, Integer> s = Sheet.rows(rowKeys, columnKeys, sampleData);
        s.freeze();
        ImmutableSet<String> rks = s.rowKeySet();
        assertThrows(UnsupportedOperationException.class, () -> rks.add("rowX"));
        ImmutableSet<String> cks = s.columnKeySet();
        assertThrows(UnsupportedOperationException.class, () -> cks.add("colX"));
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
    public void testIsNullWithIndices() {
        assertFalse(sheet.isNullAt(0, 0));
        assertFalse(sheet.isNullAt(1, 1));
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
        assertTrue(s.isNullAt(0, 0));
    }

    @Test
    public void testIsNull_WithKeys() {
        assertFalse(sheet.isNull("row1", "col1"));
        sheet.set("row1", "col1", null);
        assertTrue(sheet.isNull("row1", "col1"));
    }

    @Test
    public void testIsNull_WithIndices() {
        assertFalse(sheet.isNullAt(0, 0));
        sheet.setAt(0, 0, null);
        assertTrue(sheet.isNullAt(0, 0));
    }

    @Test
    public void testIsNull_WithPoint() {
        Point p = Point.of(0, 0);
        assertFalse(sheet.isNull(p));
        sheet.set(p, null);
        assertTrue(sheet.isNull(p));
    }

    @Test
    public void testIsNullWithInvalidKey() {
        assertThrows(IllegalArgumentException.class, () -> {
            sheet.isNull("invalidRow", "col1");
        });
    }

    @Test
    public void testIsNullWithInvalidIndices() {
        assertThrows(IndexOutOfBoundsException.class, () -> {
            sheet.isNullAt(-1, 0);
        });
        assertThrows(IndexOutOfBoundsException.class, () -> {
            sheet.isNullAt(0, 10);
        });
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
    public void testContainsCell() {
        assertTrue(sheet.containsCell("row1", "col1"));
        assertTrue(sheet.containsCell("row3", "col3"));
        assertFalse(sheet.containsCell("invalidRow", "col1"));
        assertFalse(sheet.containsCell("row1", "invalidCol"));
        assertFalse(sheet.containsCell("invalidRow", "invalidCol"));
    }

    @Test
    public void testContainsValueAt() {
        assertTrue(sheet.containsValueAt("row1", "col1", 1));
        assertTrue(sheet.containsValueAt("row2", "col2", 5));
        assertFalse(sheet.containsValueAt("row1", "col1", 999));
        assertFalse(sheet.containsValueAt("row1", "col1", null));
    }

    @Test
    public void testContainsValueAt_NullValue() {
        Sheet<String, String, Integer> s = Sheet.rows(List.of("r1"), List.of("c1"), new Integer[][] { { null } });
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
    public void testMoveRow() {
        sheet.moveRow("row3", 0);

        List<String> expectedOrder = Arrays.asList("row3", "row1", "row2");
        List<String> actualOrder = new ArrayList<>(sheet.rowKeySet());
        assertEquals(expectedOrder, actualOrder);

        assertEquals(Integer.valueOf(7), sheet.getAt(0, 0));
        assertEquals(Integer.valueOf(1), sheet.getAt(1, 0));
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
    public void testMoveRow_ToEnd() {
        sheet.moveRow("row1", 2);
        List<String> expectedOrder = Arrays.asList("row2", "row3", "row1");
        assertEquals(expectedOrder, new ArrayList<>(sheet.rowKeySet()));
        assertEquals(Integer.valueOf(1), sheet.get("row1", "col1"));
    }

    @Test
    public void testMoveRow_New() {
        sheet.moveRow("row1", 2);
        List<String> rowKeys = new ArrayList<>(sheet.rowKeySet());
        assertEquals("row2", rowKeys.get(0));
        assertEquals("row3", rowKeys.get(1));
        assertEquals("row1", rowKeys.get(2));
        assertEquals(Integer.valueOf(1), sheet.get("row1", "col1"));
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
    public void testMoveRow_frozen() {
        objectSheet.freeze();
        assertThrows(IllegalStateException.class, () -> objectSheet.moveRow("R1", 1));
    }

    @Test
    public void testMoveRow_InvalidKey() {
        assertThrows(IllegalArgumentException.class, () -> sheet.moveRow("invalidRow", 0));
    }

    // ===== Missing happy-path tests =====

    @Test
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
    public void testSwapRows_New() {
        sheet.swapRows("row1", "row3");
        List<String> rowKeys = new ArrayList<>(sheet.rowKeySet());
        assertEquals("row3", rowKeys.get(0));
        assertEquals("row2", rowKeys.get(1));
        assertEquals("row1", rowKeys.get(2));
        assertEquals(Integer.valueOf(7), sheet.get("row3", "col1"));
        assertEquals(Integer.valueOf(1), sheet.get("row1", "col1"));
    }

    @Test
    public void testSwapRows_SameRow() {
        sheet.swapRows("row1", "row1");
        assertEquals(Arrays.asList(1, 2, 3), new ArrayList<>(sheet.rowValues("row1")));
    }

    @Test
    public void testSwapRows_InvalidKey() {
        assertThrows(IllegalArgumentException.class, () -> sheet.swapRows("row1", "invalidRow"));
    }

    @Test
    public void testSwapRows_FrozenSheet() {
        sheet.freeze();
        assertThrows(IllegalStateException.class, () -> sheet.swapRows("row1", "row2"));
    }

    @Test
    public void testRenameRow() {
        sheet.renameRow("row1", "renamedRow");
        assertTrue(sheet.containsRow("renamedRow"));
        assertFalse(sheet.containsRow("row1"));
        assertEquals(Integer.valueOf(1), sheet.get("renamedRow", "col1"));
    }

    @Test
    public void testRenameRow_New() {
        sheet.renameRow("row1", "rowA");
        assertTrue(sheet.containsRow("rowA"));
        assertFalse(sheet.containsRow("row1"));
        assertEquals(Integer.valueOf(1), sheet.get("rowA", "col1"));
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
    public void testRenameRow_NullKey() {
        assertThrows(IllegalArgumentException.class, () -> sheet.renameRow("row1", null));
    }

    @Test
    public void testContainsRow_EmptySheet() {
        assertFalse(emptySheet.containsRow("row1"));
    }

    @Test
    public void testContainsRow() {
        assertTrue(sheet.containsRow("row1"));
        assertTrue(sheet.containsRow("row2"));
        assertFalse(sheet.containsRow("invalidRow"));
    }

    @Test
    public void testMoveColumn() {
        sheet.moveColumn("col3", 0);

        List<String> expectedOrder = Arrays.asList("col3", "col1", "col2");
        List<String> actualOrder = new ArrayList<>(sheet.columnKeySet());
        assertEquals(expectedOrder, actualOrder);

        assertEquals(Integer.valueOf(3), sheet.getAt(0, 0));
        assertEquals(Integer.valueOf(1), sheet.getAt(0, 1));
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
    public void testMoveColumn_ToEnd() {
        sheet.moveColumn("col1", 2);
        List<String> expectedOrder = Arrays.asList("col2", "col3", "col1");
        assertEquals(expectedOrder, new ArrayList<>(sheet.columnKeySet()));
        assertEquals(Integer.valueOf(1), sheet.get("row1", "col1"));
    }

    @Test
    public void testMoveColumn_ToSamePosition() {
        List<String> initialOrder = new ArrayList<>(sheet.columnKeySet());
        sheet.moveColumn("col2", 1);
        assertEquals(initialOrder, new ArrayList<>(sheet.columnKeySet()));
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
    public void testMoveColumn_frozen() {
        objectSheet.freeze();
        assertThrows(IllegalStateException.class, () -> objectSheet.moveColumn("C1", 1));
    }

    @Test
    public void testMoveColumn_InvalidKey() {
        assertThrows(IllegalArgumentException.class, () -> sheet.moveColumn("invalidCol", 0));
    }

    @Test
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
    public void testSwapColumns_New() {
        sheet.swapColumns("col1", "col3");
        List<String> colKeys = new ArrayList<>(sheet.columnKeySet());
        assertEquals("col3", colKeys.get(0));
        assertEquals("col2", colKeys.get(1));
        assertEquals("col1", colKeys.get(2));
        assertEquals(Integer.valueOf(3), sheet.get("row1", "col3"));
        assertEquals(Integer.valueOf(1), sheet.get("row1", "col1"));
    }

    @Test
    public void testSwapColumns_SameColumn() {
        sheet.swapColumns("col1", "col1");
        assertEquals(Integer.valueOf(1), sheet.get("row1", "col1"));
    }

    @Test
    public void testSwapColumns_InvalidKey() {
        assertThrows(IllegalArgumentException.class, () -> sheet.swapColumns("col1", "invalidCol"));
    }

    @Test
    public void testSwapColumns_FrozenSheet() {
        sheet.freeze();
        assertThrows(IllegalStateException.class, () -> sheet.swapColumns("col1", "col2"));
    }

    @Test
    public void testRenameColumn() {
        sheet.renameColumn("col1", "renamedCol");
        assertTrue(sheet.containsColumn("renamedCol"));
        assertFalse(sheet.containsColumn("col1"));
        assertEquals(Integer.valueOf(1), sheet.get("row1", "renamedCol"));
    }

    @Test
    public void testRenameColumn_New() {
        sheet.renameColumn("col1", "colA");
        assertTrue(sheet.containsColumn("colA"));
        assertFalse(sheet.containsColumn("col1"));
        assertEquals(Integer.valueOf(1), sheet.get("row1", "colA"));
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
    public void testRenameColumn_NullKey() {
        assertThrows(IllegalArgumentException.class, () -> sheet.renameColumn("col1", null));
    }

    @Test
    public void testContainsColumn_EmptySheet() {
        assertFalse(emptySheet.containsColumn("col1"));
    }

    @Test
    public void testContainsColumn() {
        assertTrue(sheet.containsColumn("col1"));
        assertTrue(sheet.containsColumn("col2"));
        assertFalse(sheet.containsColumn("invalidCol"));
    }

    @Test
    public void testReplaceIfWithIntBiPredicate() {
        sheet.replaceIf((rowIdx, colIdx) -> rowIdx == colIdx, 0);
        assertEquals(Integer.valueOf(0), sheet.getAt(0, 0));
        assertEquals(Integer.valueOf(0), sheet.getAt(1, 1));
        assertEquals(Integer.valueOf(0), sheet.getAt(2, 2));
        assertEquals(Integer.valueOf(2), sheet.getAt(0, 1));
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
    public void testReplaceIf_UninitializedWithIntBiPredicate() {
        Sheet<String, String, Integer> uninitSheet = new Sheet<>(rowKeys, columnKeys);
        uninitSheet.set("row1", "col1", 5);
        uninitSheet.replaceIf((rowIdx, colIdx) -> rowIdx == 0 && colIdx == 0, 99);
        assertEquals(Integer.valueOf(99), uninitSheet.getAt(0, 0));
    }

    @Test
    public void testReplaceIf_UninitializedWithTriPredicate() {
        Sheet<String, String, Integer> uninitSheet = new Sheet<>(rowKeys, columnKeys);
        uninitSheet.set("row1", "col1", 5);
        uninitSheet.replaceIf((r, c, v) -> "row1".equals(r) && "col1".equals(c), 99);
        assertEquals(Integer.valueOf(99), uninitSheet.get("row1", "col1"));
    }

    @Test
    public void testReplaceIfWithPredicate() {
        sheet.replaceIf(v -> v != null && v > 5, 0);
        assertEquals(Integer.valueOf(1), sheet.get("row1", "col1"));
        assertEquals(Integer.valueOf(0), sheet.get("row3", "col1"));
        assertEquals(Integer.valueOf(0), sheet.get("row3", "col3"));
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
    public void testReplaceIf_byKeyAndValuePredicate() {
        intSheet.replaceIf((rKey, cKey, val) -> "R2".equals(rKey) && val != null && val > 21, 888);
        assertEquals(21, intSheet.get("R2", "C1"));
        assertEquals(888, intSheet.get("R2", "C2"));
        assertEquals(888, intSheet.get("R2", "C3"));
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

        assertEquals(Integer.valueOf(100), uninitSheet.getAt(0, 0));
        assertEquals(Integer.valueOf(100), uninitSheet.getAt(1, 1));
        assertEquals(Integer.valueOf(100), uninitSheet.getAt(2, 2));
        assertNull(uninitSheet.getAt(0, 1));
    }

    @Test
    public void testReplaceIfWithKeys() {
        Sheet<String, String, Integer> uninitSheet = new Sheet<>(upperRowKeys, colKeys);
        uninitSheet.replaceIf((rowKey, columnKey, value) -> "R1".equals(rowKey) && "C1".equals(columnKey), 100);

        assertEquals(Integer.valueOf(100), uninitSheet.get("R1", "C1"));
        assertNull(uninitSheet.get("R1", "C2"));
    }

    @Test
    public void testReplaceIf_TriPredicate_New() {
        sheet.replaceIf((rowKey, colKey, val) -> val != null && val > 5, 0);
        assertEquals(Integer.valueOf(1), sheet.get("row1", "col1"));
        assertEquals(Integer.valueOf(0), sheet.get("row2", "col3"));
        assertEquals(Integer.valueOf(0), sheet.get("row3", "col1"));
    }

    @Test
    public void testReplaceIfOnFrozenSheet() {
        sheet.freeze();
        assertThrows(IllegalStateException.class, () -> {
            sheet.replaceIf(v -> true, 0);
        });
    }

    @Test
    public void testReplaceIf_frozen() {
        objectSheet.freeze();
        assertThrows(IllegalStateException.class, () -> objectSheet.replaceIf(v -> true, "new"));
        assertThrows(IllegalStateException.class, () -> objectSheet.replaceIf((r, c) -> true, "new"));
        assertThrows(IllegalStateException.class, () -> objectSheet.replaceIf((r, c, v) -> true, "new"));
    }

    @Test
    public void testCloneWithoutFreeze_IndependentMutation() {
        Sheet<String, String, Integer> clone = sheet.clone(false);
        assertFalse(clone.isFrozen());
        clone.set("row1", "col1", 999);
        assertEquals(Integer.valueOf(999), clone.get("row1", "col1"));
        // Original unaffected
        assertEquals(Integer.valueOf(1), sheet.get("row1", "col1"));
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
    public void testClone_NoArgs() {
        Sheet<String, String, Integer> cloned = sheet.clone();
        assertNotSame(sheet, cloned);
        assertEquals(sheet.rowCount(), cloned.rowCount());
        assertEquals(sheet.columnCount(), cloned.columnCount());
        assertEquals(sheet.get("row1", "col1"), cloned.get("row1", "col1"));
        assertFalse(cloned.isFrozen());
    }

    @Test
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
    public void testCloneWithFreeze() {
        Sheet<String, String, Integer> clone = sheet.clone(true);
        assertNotNull(clone);
        assertTrue(clone.isFrozen());
        assertThrows(IllegalStateException.class, () -> {
            clone.set("row1", "col1", 999);
        });
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
            } else {
                throw e;
            }
        }
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
    public void testClone_WithFreeze() {
        Sheet<String, String, Integer> cloned = sheet.clone(true);
        assertTrue(cloned.isFrozen());
        assertThrows(IllegalStateException.class, () -> cloned.set("row1", "col1", 100));
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
    public void testMerge_OverlappingKeys() {
        Sheet<String, String, Integer> sheet1 = Sheet.rows(Arrays.asList("r1", "r2"), Arrays.asList("c1", "c2"), new Integer[][] { { 1, 2 }, { 3, 4 } });
        Sheet<String, String, Integer> sheet2 = Sheet.rows(Arrays.asList("r2", "r3"), Arrays.asList("c2", "c3"), new Integer[][] { { 10, 20 }, { 30, 40 } });
        Sheet<String, String, Integer> merged = sheet1.merge(sheet2, (a, b) -> {
            if (a == null && b == null) {
                return 0;
            }
            if (a == null) {
                return b;
            }
            if (b == null) {
                return a;
            }
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
    public void testMerge_EmptySheets() {
        Sheet<String, String, Integer> empty1 = Sheet.empty();
        Sheet<String, String, Integer> empty2 = Sheet.empty();
        Sheet<String, String, Integer> merged = empty1.merge(empty2, (a, b) -> 0);
        assertTrue(merged.isEmpty());
    }

    @Test
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
    public void testMergeStillProducesTheDocumentedUnionGrid() {
        final Sheet<String, String, Integer> a = Sheet.rows(Arrays.asList("row1", "row2"), Arrays.asList("col1", "col2"),
                new Integer[][] { { 1, 2 }, { 3, 4 } });
        final Sheet<String, String, Integer> b = Sheet.rows(Arrays.asList("row2", "row3"), Arrays.asList("col2", "col3"),
                new Integer[][] { { 10, 20 }, { 30, 40 } });

        final Sheet<String, String, String> merged = a.merge(b, (x, y) -> x + "#" + y);

        assertEquals(Arrays.asList("row1", "row2", "row3"), new ArrayList<>(merged.rowKeySet()));
        assertEquals(Arrays.asList("col1", "col2", "col3"), new ArrayList<>(merged.columnKeySet()));
        assertEquals("1#null", merged.get("row1", "col1"));
        assertEquals("2#null", merged.get("row1", "col2"));
        assertEquals("null#null", merged.get("row1", "col3"));
        assertEquals("3#null", merged.get("row2", "col1"));
        assertEquals("4#10", merged.get("row2", "col2"));
        assertEquals("null#20", merged.get("row2", "col3"));
        assertEquals("null#null", merged.get("row3", "col1"));
        assertEquals("null#30", merged.get("row3", "col2"));
        assertEquals("null#40", merged.get("row3", "col3"));

        // The merged Sheet must be a genuinely independent, mutable Sheet.
        assertFalse(merged.isFrozen());
        merged.set("row1", "col1", "changed");
        assertEquals(Integer.valueOf(1), a.get("row1", "col1"));
    }

    @Test
    public void testTranspose_DataIntegrity() {
        Sheet<String, String, Integer> transposed = sheet.transposed();
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
    public void testFreezePreventsMutations() {
        sheet.freeze();
        assertThrows(IllegalStateException.class, () -> sheet.set("row1", "col1", 100));
        assertThrows(IllegalStateException.class, () -> sheet.remove("row1", "col1"));
        assertThrows(IllegalStateException.class, () -> sheet.addRow("row4", Arrays.asList(1, 2, 3)));
        assertThrows(IllegalStateException.class, () -> sheet.addColumn("col4", Arrays.asList(1, 2, 3)));
    }

    @Test
    public void testFreezeAndIsFrozen() {
        assertFalse(objectSheet.isFrozen());
        objectSheet.freeze();
        assertTrue(objectSheet.isFrozen());
        assertThrows(IllegalStateException.class, () -> objectSheet.set("R1", "C1", "Fail"));
    }

    @Test
    public void testFreeze_AlreadyFrozen() {
        sheet.freeze();
        assertTrue(sheet.isFrozen());
        // Freezing again should not throw
        sheet.freeze();
        assertTrue(sheet.isFrozen());
    }

    @Test
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
        assertThrows(IllegalStateException.class, () -> sheet.updateAll((java.util.function.Function<Integer, Integer>) null));
    }

    @Test
    public void testIsFrozen() {
        assertFalse(sheet.isFrozen());
        sheet.freeze();
        assertTrue(sheet.isFrozen());
    }

    @Test
    public void testTrimToSize() {
        sheet.trimToSize();
        assertEquals(3, sheet.rowCount());
        assertEquals(3, sheet.columnCount());
    }

    @Test
    public void testTrimToSize_AfterRemoval() {
        sheet.removeRow("row3");
        sheet.trimToSize();
        assertEquals(2, sheet.rowCount());
        assertEquals(Integer.valueOf(1), sheet.get("row1", "col1"));
    }

    @Test
    public void testTrimToSizeIsAllowedOnAFrozenSheet() {
        final Sheet<String, String, Integer> s = Sheet.rows(Arrays.asList("r1", "r2"), Arrays.asList("c1"), new Integer[][] { { 1 }, { 2 } });
        s.removeRow("r2");
        s.freeze();

        // Documented to mirror Dataset.trimToSize(): it changes no content or structure.
        assertDoesNotThrow(s::trimToSize);
        assertEquals(Integer.valueOf(1), s.get("r1", "c1"));
        assertEquals(1, s.rowCount());

        // Everything that does change content or structure is still rejected.
        assertThrows(IllegalStateException.class, () -> s.set("r1", "c1", 9));
        assertThrows(IllegalStateException.class, s::clear);
    }

    @Test
    public void testNonNullValueCount() {
        assertEquals(9, sheet.nonNullValueCount());
        assertEquals(0, emptySheet.nonNullValueCount());
    }

    @Test
    public void testNonNullValueCount_WithNulls() {
        // objectSheet has some null values: {{"V11","V12",null},{100,null,true},{null,null,null}}
        assertEquals(4, objectSheet.nonNullValueCount());
    }

    @Test
    public void testNonNullValueCount_UninitializedSheet() {
        Sheet<String, String, Integer> uninitSheet = new Sheet<>(rowKeys, columnKeys);
        assertEquals(0, uninitSheet.nonNullValueCount());
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
    public void testIsEmpty_Various() {
        assertTrue(emptySheet.isEmpty());
        assertFalse(sheet.isEmpty());
        Sheet<String, String, Integer> keysOnly = new Sheet<>(List.of("r1"), List.of("c1"));
        assertFalse(keysOnly.isEmpty());
    }

    @Test
    public void testIsEmpty_RowsButNoColumns() {
        Sheet<String, String, Integer> s = new Sheet<>(rowKeys, Collections.emptyList());
        assertTrue(s.isEmpty());
    }

    @Test
    public void testIsEmpty_ColumnsButNoRows() {
        Sheet<String, String, Integer> s = new Sheet<>(Collections.emptyList(), columnKeys);
        assertTrue(s.isEmpty());
    }

    @Test
    public void testApply_TransformSheet() {
        int result = sheet.apply(s -> s.rowCount() * s.columnCount());
        assertEquals(9, result);
    }

    @Test
    public void testApply() {
        Integer sum = sheet.apply(s -> {
            int total = 0;
            for (int i = 0; i < s.rowCount(); i++) {
                for (int j = 0; j < s.columnCount(); j++) {
                    Integer val = s.getAt(i, j);
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
            return s.getAt(0, 0);
        });
        assertTrue(result.isPresent());
        assertEquals(Integer.valueOf(1), result.get());
    }

    @Test
    public void testApplyIfNotEmptyOnEmptySheet() {
        u.Optional<Integer> result = emptySheet.applyIfNotEmpty(s -> s.getAt(0, 0));
        assertFalse(result.isPresent());
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
    public void testApplyIfNotEmpty_OnNonEmptySheet() {
        u.Optional<Integer> result = sheet.applyIfNotEmpty(s -> s.rowCount());
        assertTrue(result.isPresent());
        assertEquals(Integer.valueOf(3), result.get());
    }

    @Test
    public void testAccept_ConsumeSheet() {
        final boolean[] called = { false };
        sheet.accept(s -> {
            called[0] = true;
            assertEquals(3, s.rowCount());
        });
        assertTrue(called[0]);
    }

    @Test
    public void testAccept() {
        List<Integer> values = new ArrayList<>();
        sheet.accept(s -> {
            for (int i = 0; i < s.rowCount(); i++) {
                for (int j = 0; j < s.columnCount(); j++) {
                    Integer val = s.getAt(i, j);
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
            values.add(s.getAt(0, 0));
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
    public void testAcceptIfNotEmpty_OnEmptySheet() {
        final boolean[] called = { false };
        emptySheet.acceptIfNotEmpty(s -> called[0] = true);
        assertFalse(called[0]);
    }

    @Test
    public void testPrintln_WithPrefixAndAppendable() {
        StringWriter writer = new StringWriter();
        sheet.println(rowKeys, columnKeys, ">> ", writer);
        String output = writer.toString();
        assertTrue(output.contains(">> "));
        assertTrue(output.contains("row1"));
        assertTrue(output.contains("col1"));
    }

    @Test
    public void testPrintln_SubsetWithPrefix() {
        StringWriter writer = new StringWriter();
        sheet.println(Arrays.asList("row1"), Arrays.asList("col1", "col2"), "## ", writer);
        String output = writer.toString();
        assertTrue(output.contains("## "));
        assertTrue(output.contains("row1"));
        assertTrue(output.contains("col1"));
    }

    @Test
    public void testPrintlnWithWideCharacters() {
        Sheet<String, String, Object> wideSheet = Sheet.rows(Arrays.asList("row1", "行2"), Arrays.asList("name", "城市"),
                new Object[][] { { "Bob李海洋", "LA" }, { "Alice", "上海" } });
        StringWriter writer = new StringWriter();

        wideSheet.println(writer);

        assertEquals("       +-----------+------+\n" //
                + "       | name      | 城市 |\n" //
                + "+------+-----------+------+\n" //
                + "| row1 | Bob李海洋 | LA   |\n" //
                + "| 行2  | Alice     | 上海 |\n" //
                + "+------+-----------+------+\n", writer.toString());
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
    public void testPrintlnWithOneNullKeySetTreatedAsEmpty() {
        // regression: println(keys, null, ...) threw NPE while println(keys, emptyList, ...) worked
        final Sheet<String, String, Integer> sheet = Sheet.rows(CommonUtil.asList("r1", "r2"), CommonUtil.asList("c1", "c2"),
                new Integer[][] { { 1, 2 }, { 3, 4 } });

        final StringBuilder sb1 = new StringBuilder();
        sheet.println(CommonUtil.asList("r1"), null, sb1);

        final StringBuilder sb2 = new StringBuilder();
        sheet.println(CommonUtil.asList("r1"), CommonUtil.emptyList(), sb2);

        org.junit.jupiter.api.Assertions.assertEquals(sb2.toString(), sb1.toString());
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
    public void testCellToString() {
        Cell<String, String, Integer> cell = Cell.of("r1", "c1", 42);
        String str = cell.toString();
        assertNotNull(str);
        assertTrue(str.contains("r1"));
        assertTrue(str.contains("c1"));
        assertTrue(str.contains("42"));
    }

    @Test
    public void testCellToString_NullValue() {
        Cell<String, String, Integer> cell = Cell.of("r1", "c1", null);
        String str = cell.toString();
        assertNotNull(str);
        assertTrue(str.contains("r1"));
        assertTrue(str.contains("c1"));
    }

    @Test
    public void testCountOfNonNullValue() {
        assertEquals(9, sheet.nonNullValueCount());

        Sheet<String, String, Integer> withNulls = Sheet.rows(Arrays.asList("r1", "r2"), Arrays.asList("c1", "c2"),
                new Integer[][] { { 1, null }, { null, 4 } });
        assertEquals(2, withNulls.nonNullValueCount());
    }

    @Test
    public void testPointOf() {
        Point point = Point.of(1, 2);
        assertNotNull(point);
        assertEquals(1, point.rowIndex());
        assertEquals(2, point.columnIndex());
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
    public void testCellOf_NullValue() {
        Cell<String, String, Integer> cell = Cell.of("r1", "c1", null);
        assertEquals("r1", cell.rowKey());
        assertEquals("c1", cell.columnKey());
        assertNull(cell.value());
    }

    @Test
    public void testPointOf_Caching() {
        // Small indices should be cached
        Point p1 = Point.of(0, 0);
        Point p2 = Point.of(0, 0);
        assertSame(p1, p2);
    }

    @Test
    public void testPointOf_LargeIndices() {
        Point p = Point.of(1000, 2000);
        assertEquals(1000, p.rowIndex());
        assertEquals(2000, p.columnIndex());
    }

    @Test
    public void testPointOf_NegativeIndices() {
        // Point.of should allow any int values (no validation in factory)
        Point p = Point.of(-1, -1);
        assertEquals(-1, p.rowIndex());
        assertEquals(-1, p.columnIndex());
    }

    @Test
    public void testCellOf_AllNullFields() {
        Cell<String, String, Integer> cell = Cell.of(null, null, null);
        assertNull(cell.rowKey());
        assertNull(cell.columnKey());
        assertNull(cell.value());
    }

    @Test
    public void testPointOf_CachedRange() {
        // Points within cached range should be same instance
        Point p1 = Point.of(0, 0);
        Point p2 = Point.of(0, 0);
        assertSame(p1, p2);
        assertSame(Point.ZERO, p1);
    }

    @Test
    public void test_tmp() {
        Sheet<String, String, Integer> sheet1 = Sheet.rows(List.of("row1", "row2"), List.of("col1", "col2"), new Integer[][] { { 1, 2 }, { 3, 4 } });

        Sheet<String, String, Integer> sheet2 = Sheet.rows(List.of("row2", "row3"), List.of("col2", "col3"), new Integer[][] { { 10, 20 }, { 30, 40 } });

        Sheet<String, String, String> merged = sheet1.merge(sheet2, (a, b) -> a + "#" + b);

        assertNotNull(merged);
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
    public void testSwapRowPositionInvalidKey() {
        assertThrows(IllegalArgumentException.class, () -> {
            sheet.swapRows("row1", "invalidRow");
        });
    }

    @Test
    public void testSwapColumnPositionInvalidKey() {
        assertThrows(IllegalArgumentException.class, () -> {
            sheet.swapColumns("col1", "invalidCol");
        });
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
    public void testDefaultConstructor() {
        Sheet<String, String, Integer> s = new Sheet<>();
        assertNotNull(s);
        assertEquals(0, s.rowCount());
        assertEquals(0, s.columnCount());
        assertTrue(s.isEmpty());
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
    public void testConstructorWithNullData() {
        Sheet<String, String, Integer> s = new Sheet<>(rowKeys, columnKeys, (Integer[][]) null);
        assertNotNull(s);
        assertEquals(3, s.rowCount());
        assertEquals(3, s.columnCount());
        assertFalse(s.isEmpty());
    }

    @Test
    public void testSortingSingleRow() {
        Sheet<String, String, Integer> singleRowSheet = new Sheet<>(Arrays.asList("R1"), columnKeys);

        singleRowSheet.sortByRowKey();
        assertEquals(1, singleRowSheet.rowCount());
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

        for (boolean multipleKeys : new boolean[] { false, true }) {
            Sheet<String, String, Integer> values = Sheet.rows(List.of("r1", "r2"), List.of("c1", "c2"), new Integer[][] { { 4, 2 }, { 3, 1 } });
            Comparator<Object[]> byFirstValue = (a, b) -> {
                assertNotNull(a);
                assertNotNull(b);
                return Integer.compare((Integer) a[0], (Integer) b[0]);
            };

            if (multipleKeys) {
                values.sortRowsByColumnValues(List.of("c1"), byFirstValue);
                values.sortColumnsByRowValues(List.of("r1"), byFirstValue);
            } else {
                values.sortRowsByColumnValues("c1", Integer::compareTo);
                values.sortColumnsByRowValues("r1", Integer::compareTo);
            }

            assertEquals(List.of("r2", "r1"), new ArrayList<>(values.rowKeySet()));
            assertEquals(List.of("c2", "c1"), new ArrayList<>(values.columnKeySet()));
            values.clear();
            Comparator<Object> unexpectedComparison = (a, b) -> {
                throw new AssertionError("All-null selections must not invoke the comparator");
            };

            if (multipleKeys) {
                values.sortRowsByColumnValues(List.of("c1", "c2"), unexpectedComparison);
                values.sortColumnsByRowValues(List.of("r1", "r2"), unexpectedComparison);
            } else {
                values.sortRowsByColumnValues("c1", unexpectedComparison);
                values.sortColumnsByRowValues("r1", unexpectedComparison);
            }

            assertEquals(List.of("r2", "r1"), new ArrayList<>(values.rowKeySet()));
            assertEquals(List.of("c2", "c1"), new ArrayList<>(values.columnKeySet()));
        }
    }

    @Test
    public void testTranspose() {
        Sheet<String, String, Integer> transposed = sheet.transposed();
        assertNotNull(transposed);
        assertEquals(sheet.columnCount(), transposed.rowCount());
        assertEquals(sheet.rowCount(), transposed.columnCount());

        assertEquals(Integer.valueOf(1), transposed.get("col1", "row1"));
        assertEquals(Integer.valueOf(5), transposed.get("col2", "row2"));
        assertEquals(Integer.valueOf(9), transposed.get("col3", "row3"));
    }

    @Test
    public void testSwapRowPosition_frozen() {
        objectSheet.freeze();
        assertThrows(IllegalStateException.class, () -> objectSheet.swapRows("R1", "R2"));
    }

    @Test
    public void testSwapColumnPosition_frozen() {
        objectSheet.freeze();
        assertThrows(IllegalStateException.class, () -> objectSheet.swapColumns("C1", "C2"));
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
    public void testForEachH() {
        List<Integer> collected = new ArrayList<>();
        sheet.forEachRowMajor((r, c, v) -> {
            if (v != null) {
                collected.add(v);
            }
        });
        assertEquals(9, collected.size());
        assertEquals(Integer.valueOf(1), collected.get(0));
    }

    @Test
    public void testForEachH_Original() {
        Map<String, Object> collected = new LinkedHashMap<>();
        objectSheet.forEachRowMajor((r, c, v) -> collected.put(r + "-" + c, v));

        assertEquals("V11", collected.get("R1-C1"));
        assertEquals(true, collected.get("R2-C3"));
        assertNull(collected.get("R1-C3"));
        assertEquals(9, collected.size());
        List<String> expectedOrder = Arrays.asList("R1-C1", "R1-C2", "R1-C3", "R2-C1", "R2-C2", "R2-C3", "R3-C1", "R3-C2", "R3-C3");
        assertEquals(expectedOrder, new ArrayList<>(collected.keySet()));
    }

    @Test
    public void testForEachH_exceptionPropagation() {
        IOException thrown = assertThrows(IOException.class, () -> {
            objectSheet.forEachRowMajor((r, c, v) -> {
                if (r.equals("R2") && c.equals("C1")) {
                    throw new IOException("Test Exception");
                }
            });
        });
        assertEquals("Test Exception", thrown.getMessage());
    }

    @Test
    public void testForEachV() {
        List<Integer> collected = new ArrayList<>();
        sheet.forEachColumnMajor((r, c, v) -> {
            if (v != null) {
                collected.add(v);
            }
        });
        assertEquals(9, collected.size());
        assertEquals(Integer.valueOf(1), collected.get(0));
    }

    @Test
    public void testForEachV_exceptionPropagation() {
        RuntimeException thrown = assertThrows(RuntimeException.class, () -> {
            objectSheet.forEachColumnMajor((r, c, v) -> {
                if (c.equals("C2") && r.equals("R1")) {
                    throw new RuntimeException("Test Exception V");
                }
            });
        });
        assertEquals("Test Exception V", thrown.getMessage());
    }

    @Test
    public void testForEachNonNullH() {
        List<Integer> collected = new ArrayList<>();
        sheet.forEachNonNullRowMajor((r, c, v) -> collected.add(v));
        assertEquals(9, collected.size());
    }

    @Test
    public void testForEachNonNullH_WithNulls() {
        Sheet<String, String, Integer> s = Sheet.rows(Arrays.asList("r1", "r2"), Arrays.asList("c1", "c2"), new Integer[][] { { 1, null }, { null, 4 } });
        List<Integer> collected = new ArrayList<>();
        s.forEachNonNullRowMajor((r, c, v) -> collected.add(v));
        assertEquals(2, collected.size());
        assertEquals(Integer.valueOf(1), collected.get(0));
        assertEquals(Integer.valueOf(4), collected.get(1));
    }

    @Test
    public void testForEachNonNullH_WithNullValues() {
        Sheet<String, String, Integer> s = Sheet.rows(Arrays.asList("r1", "r2"), Arrays.asList("c1", "c2"), new Integer[][] { { 1, null }, { null, 4 } });
        List<Integer> collected = new ArrayList<>();
        s.forEachNonNullRowMajor((r, c, v) -> collected.add(v));
        assertEquals(2, collected.size());
        assertTrue(collected.contains(1));
        assertTrue(collected.contains(4));
    }

    @Test
    public void testForEachNonNullH_exceptionPropagation() {
        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class, () -> {
            objectSheet.forEachNonNullRowMajor((r, c, v) -> {
                if (r.equals("R2") && c.equals("C3")) {
                    throw new IllegalArgumentException("Test Exception NonNullH");
                }
            });
        });
        assertEquals("Test Exception NonNullH", thrown.getMessage());
    }

    @Test
    public void testForEachNonNullV() {
        List<Integer> collected = new ArrayList<>();
        sheet.forEachNonNullColumnMajor((r, c, v) -> collected.add(v));
        assertEquals(9, collected.size());
    }

    @Test
    public void testForEachNonNullV_WithNulls() {
        Sheet<String, String, Integer> s = Sheet.rows(Arrays.asList("r1", "r2"), Arrays.asList("c1", "c2"), new Integer[][] { { 1, null }, { null, 4 } });
        List<Integer> collected = new ArrayList<>();
        s.forEachNonNullColumnMajor((r, c, v) -> collected.add(v));
        assertEquals(2, collected.size());
        // Vertical order: c1(r1=1), c1(r2=null skip), c2(r1=null skip), c2(r2=4)
        assertEquals(Integer.valueOf(1), collected.get(0));
        assertEquals(Integer.valueOf(4), collected.get(1));
    }

    @Test
    public void testForEachNonNullV_WithNullValues() {
        Sheet<String, String, Integer> s = Sheet.rows(Arrays.asList("r1", "r2"), Arrays.asList("c1", "c2"), new Integer[][] { { 1, null }, { null, 4 } });
        List<Integer> collected = new ArrayList<>();
        s.forEachNonNullColumnMajor((r, c, v) -> collected.add(v));
        assertEquals(2, collected.size());
        assertTrue(collected.contains(1));
        assertTrue(collected.contains(4));
    }

    @Test
    public void testForEachNonNullV_exceptionPropagation() {
        UnsupportedOperationException thrown = assertThrows(UnsupportedOperationException.class, () -> {
            objectSheet.forEachNonNullColumnMajor((r, c, v) -> {
                if (c.equals("C1") && r.equals("R1")) {
                    throw new UnsupportedOperationException("Test Exception NonNullV");
                }
            });
        });
        assertEquals("Test Exception NonNullV", thrown.getMessage());
    }

    @Test
    public void testPointsH() {
        Stream<Point> points = sheet.rowMajorPoints();
        List<Point> pointList = points.toList();
        assertEquals(9, pointList.size());
        assertEquals(0, pointList.get(0).rowIndex());
        assertEquals(0, pointList.get(0).columnIndex());
    }

    @Test
    public void testPointsHWithRange() {
        Stream<Point> points = sheet.rowMajorPoints(0, 2);
        List<Point> pointList = points.toList();
        assertEquals(6, pointList.size());
    }

    @Test
    public void testPointsH_fullRangeAndSubRange() {
        assertEquals(9, objectSheet.rowMajorPoints().count());
        assertEquals(3, objectSheet.rowMajorPoints(0, 1).count());
        assertEquals(0, objectSheet.rowMajorPoints(1, 1).count());
        List<Sheet.Point> r1Points = objectSheet.rowMajorPoints(0, 1).toList();
        assertEquals(Sheet.Point.of(0, 0), r1Points.get(0));
        assertEquals(Sheet.Point.of(0, 1), r1Points.get(1));
        assertEquals(Sheet.Point.of(0, 2), r1Points.get(2));
    }

    @Test
    public void testPointsHWithSingleRow() {
        List<Point> points = sheet.rowMajorPoints(1, 2).toList();
        assertEquals(3, points.size());
        assertEquals(Point.of(1, 0), points.get(0));
        assertEquals(Point.of(1, 1), points.get(1));
        assertEquals(Point.of(1, 2), points.get(2));
    }

    @Test
    public void testPointsH_InvalidRange() {
        assertThrows(IndexOutOfBoundsException.class, () -> sheet.rowMajorPoints(-1, 2));
        assertThrows(IndexOutOfBoundsException.class, () -> sheet.rowMajorPoints(0, 10));
    }

    @Test
    public void testPointsV() {
        Stream<Point> points = sheet.columnMajorPoints();
        List<Point> pointList = points.toList();
        assertEquals(9, pointList.size());
        assertEquals(0, pointList.get(0).rowIndex());
        assertEquals(0, pointList.get(0).columnIndex());
    }

    @Test
    public void testPointsVWithRange() {
        Stream<Point> points = sheet.columnMajorPoints(0, 2);
        List<Point> pointList = points.toList();
        assertEquals(6, pointList.size());
    }

    @Test
    public void testPointsV_fullRangeAndSubRange() {
        assertEquals(9, objectSheet.columnMajorPoints().count());
        assertEquals(3, objectSheet.columnMajorPoints(0, 1).count());
        assertEquals(0, objectSheet.columnMajorPoints(1, 1).count());
        List<Sheet.Point> c1Points = objectSheet.columnMajorPoints(0, 1).toList();
        assertEquals(Sheet.Point.of(0, 0), c1Points.get(0));
        assertEquals(Sheet.Point.of(1, 0), c1Points.get(1));
        assertEquals(Sheet.Point.of(2, 0), c1Points.get(2));
    }

    @Test
    public void testPointsVWithSingleColumn() {
        List<Point> points = sheet.columnMajorPoints(1, 2).toList();
        assertEquals(3, points.size());
        assertEquals(Point.of(0, 1), points.get(0));
        assertEquals(Point.of(1, 1), points.get(1));
        assertEquals(Point.of(2, 1), points.get(2));
    }

    @Test
    public void testPointsV_InvalidRange() {
        assertThrows(IndexOutOfBoundsException.class, () -> sheet.columnMajorPoints(-1, 2));
        assertThrows(IndexOutOfBoundsException.class, () -> sheet.columnMajorPoints(0, 10));
    }

    @Test
    public void testPointsR() {
        Stream<Stream<Point>> rowPoints = sheet.rowPoints();
        List<List<Point>> result = rowPoints.map(Stream::toList).toList();
        assertEquals(3, result.size());
        assertEquals(3, result.get(0).size());
    }

    @Test
    public void testPointsRWithRange() {
        Stream<Stream<Point>> rowPoints = sheet.rowPoints(0, 2);
        List<List<Point>> result = rowPoints.map(Stream::toList).toList();
        assertEquals(2, result.size());
    }

    @Test
    public void testPointsR_fullRangeAndSubRange() {
        assertEquals(3, objectSheet.rowPoints().count());
        assertEquals(1, objectSheet.rowPoints(1, 2).count());
        assertEquals(0, objectSheet.rowPoints(1, 1).count());

        List<Sheet.Point> r2Points = objectSheet.rowPoints(1, 2).first().get().toList();
        assertEquals(3, r2Points.size());
        assertEquals(Sheet.Point.of(1, 0), r2Points.get(0));
    }

    @Test
    public void testPointsR_InvalidRange() {
        assertThrows(IndexOutOfBoundsException.class, () -> sheet.rowPoints(-1, 2));
        assertThrows(IndexOutOfBoundsException.class, () -> sheet.rowPoints(0, 10));
    }

    @Test
    public void testPointsC() {
        Stream<Stream<Point>> columnPoints = sheet.columnPoints();
        List<List<Point>> result = columnPoints.map(Stream::toList).toList();
        assertEquals(3, result.size());
        assertEquals(3, result.get(0).size());
    }

    @Test
    public void testPointsCWithRange() {
        Stream<Stream<Point>> columnPoints = sheet.columnPoints(0, 2);
        List<List<Point>> result = columnPoints.map(Stream::toList).toList();
        assertEquals(2, result.size());
    }

    @Test
    public void testPointsC_fullRangeAndSubRange_ACTUAL_BEHAVIOR() {
        List<Stream<Sheet.Point>> columnPointsStreams = objectSheet.columnPoints().toList();
        assertEquals(objectSheet.columnCount(), columnPointsStreams.size());

        for (int i = 0; i < objectSheet.columnCount(); i++) {
            List<Sheet.Point> columnStreamsontent = columnPointsStreams.get(i).toList();
            assertEquals(objectSheet.rowCount(), columnStreamsontent.size());
            for (int j = 0; j < objectSheet.rowCount(); j++) {
                assertEquals(Sheet.Point.of(j, i), columnStreamsontent.get(j));
            }
        }

        List<Stream<Sheet.Point>> actualPointsCStreams = objectSheet.columnPoints(0, objectSheet.columnCount()).toList();
        assertEquals(objectSheet.columnCount(), actualPointsCStreams.size());
        List<Sheet.Point> firstColPoints = actualPointsCStreams.get(0).toList();
        assertEquals(objectSheet.rowCount(), firstColPoints.size());
        assertEquals(Sheet.Point.of(0, 0), firstColPoints.get(0));
        assertEquals(Sheet.Point.of(1, 0), firstColPoints.get(1));
        assertEquals(Sheet.Point.of(2, 0), firstColPoints.get(2));
    }

    @Test
    public void testPointsC_InvalidRange() {
        assertThrows(IndexOutOfBoundsException.class, () -> sheet.columnPoints(-1, 2));
        assertThrows(IndexOutOfBoundsException.class, () -> sheet.columnPoints(0, 10));
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
    public void testPointZero() {
        Point zero = Point.ZERO;
        assertNotNull(zero);
        assertEquals(0, zero.rowIndex());
        assertEquals(0, zero.columnIndex());
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
    public void testTranspose_RoundTripPreservesData() {
        Sheet<String, String, Integer> orig = Sheet.rows(Arrays.asList("r1", "r2", "r3"), Arrays.asList("c1", "c2"),
                new Integer[][] { { 1, 2 }, { 3, 4 }, { 5, 6 } });
        Sheet<String, String, Integer> roundTrip = orig.transposed().transposed();
        assertEquals(orig, roundTrip);
        // verify each cell
        for (String r : orig.rowKeySet()) {
            for (String c : orig.columnKeySet()) {
                assertEquals(orig.get(r, c), roundTrip.get(r, c));
            }
        }
    }

    @Test
    public void testTranspose_NonSquareDataPreserved() {
        Sheet<String, String, Integer> orig = Sheet.rows(Arrays.asList("r1", "r2"), Arrays.asList("c1", "c2", "c3"),
                new Integer[][] { { 1, 2, 3 }, { 4, 5, 6 } });
        Sheet<String, String, Integer> tr = orig.transposed();
        assertEquals(3, tr.rowCount());
        assertEquals(2, tr.columnCount());
        // Cell at (oldRow=r1, oldCol=c2) == 2; in transposed: (newRow=c2, newCol=r1)
        assertEquals(Integer.valueOf(2), tr.get("c2", "r1"));
        assertEquals(Integer.valueOf(6), tr.get("c3", "r2"));
    }

    @Test
    public void testIterationOrder_PreservesInsertion() {
        Sheet<String, String, Integer> s = new Sheet<>(Arrays.asList("z", "a", "m"), Arrays.asList("y", "b", "n"));
        List<String> rk = new ArrayList<>(s.rowKeySet());
        assertEquals(Arrays.asList("z", "a", "m"), rk);
        List<String> ck = new ArrayList<>(s.columnKeySet());
        assertEquals(Arrays.asList("y", "b", "n"), ck);
    }

    @Test
    public void testNullKeyMutatorsFailBeforeAnyMutation() {
        // regression: a null new key was rejected by BiMap.put only AFTER the key set had been
        // mutated, leaving the sheet permanently inconsistent (phantom rows, unreachable data)
        final Sheet<String, String, Integer> sheet = Sheet.rows(CommonUtil.asList("r1"), CommonUtil.asList("c1"), new Integer[][] { { 1 } });

        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class, () -> sheet.addRow(null, CommonUtil.asList(2)));
        org.junit.jupiter.api.Assertions.assertEquals(1, sheet.rowCount());

        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class, () -> sheet.renameRow("r1", null));
        org.junit.jupiter.api.Assertions.assertEquals(Integer.valueOf(1), sheet.get("r1", "c1"));

        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class, () -> sheet.addColumn(null, CommonUtil.asList(2)));
        org.junit.jupiter.api.Assertions.assertEquals(1, sheet.columnCount());

        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class, () -> sheet.renameColumn("c1", null));
        org.junit.jupiter.api.Assertions.assertEquals(Integer.valueOf(1), sheet.get("r1", "c1"));
    }

    @Test
    public void testKeyedViewsAreUnmodifiable() {
        final Sheet<String, String, Integer> s = Sheet.rows(Arrays.asList("r1"), Arrays.asList("c1"), new Integer[][] { { 1 } });

        assertThrows(UnsupportedOperationException.class, () -> s.rowValues("r1").set(0, 9));
        assertThrows(UnsupportedOperationException.class, () -> s.columnValues("c1").set(0, 9));
    }

    @Test
    public void testKeyedViewsTrackTheirKeyThroughInsertionsAndRenames() {
        final Sheet<String, String, Integer> s = Sheet.rows(Arrays.asList("r1", "r2", "r3"), Arrays.asList("c1", "c2"),
                new Integer[][] { { 1, 2 }, { 3, 4 }, { 5, 6 } });
        final ImmutableList<Integer> rowView = s.rowValues("r2");
        final ImmutableList<Integer> columnView = s.columnValues("c2");

        assertEquals(Arrays.asList(3, 4), new ArrayList<>(rowView));
        assertEquals(Arrays.asList(2, 4, 6), new ArrayList<>(columnView));

        // Inserting a row *before* the tracked one shifts its position but must not shift the view.
        s.addRow(0, "r0", Arrays.asList(9, 10));
        assertEquals(Arrays.asList("r0", "r1", "r2", "r3"), new ArrayList<>(s.rowKeySet()));
        assertEquals(Arrays.asList(3, 4), new ArrayList<>(rowView));
        assertEquals(Arrays.asList(10, 2, 4, 6), new ArrayList<>(columnView));

        // Inserting a column *before* the tracked one likewise.
        s.addColumn(0, "c0", Arrays.asList(100, 200, 300, 400));
        assertEquals(Arrays.asList(300, 3, 4), new ArrayList<>(rowView));
        assertEquals(new ArrayList<>(s.rowValues("r2")), new ArrayList<>(rowView));
        assertEquals(Arrays.asList(10, 2, 4, 6), new ArrayList<>(columnView));

        // Renaming the tracked key away invalidates the view - the old key no longer names anything.
        s.renameRow("r2", "r2x");
        assertThrows(IllegalArgumentException.class, () -> rowView.get(0));
        assertEquals(Arrays.asList(300, 3, 4), new ArrayList<>(s.rowValues("r2x")));
    }

    /**
     * Property test for the keyed-view contract: after any sequence of structural mutations, a view obtained
     * earlier for a key must still report exactly what a freshly obtained view for that same key reports -
     * and must fail loudly once the key is gone. This is the invariant the whole keyed-view design rests on;
     * the worked examples above only cover the handful of sequences someone thought to write down.
     */
    @Test
    public void testKeyedViewsAlwaysAgreeWithAFreshLookup() {
        final Random rnd = new Random(20260831L);

        for (int trial = 0; trial < 200; trial++) {
            final List<String> rowKeys = new ArrayList<>(Arrays.asList("r0", "r1", "r2", "r3"));
            final List<String> columnKeys = new ArrayList<>(Arrays.asList("c0", "c1", "c2"));
            final Integer[][] data = new Integer[rowKeys.size()][columnKeys.size()];

            for (int r = 0; r < rowKeys.size(); r++) {
                for (int c = 0; c < columnKeys.size(); c++) {
                    data[r][c] = (r * 10) + c;
                }
            }

            final Sheet<String, String, Integer> sheet = Sheet.rows(rowKeys, columnKeys, data);

            // Hold a view for every key up front, then mutate underneath them.
            final Map<String, ImmutableList<Integer>> rowViews = new LinkedHashMap<>();
            final Map<String, ImmutableList<Integer>> columnViews = new LinkedHashMap<>();

            for (final String rowKey : rowKeys) {
                rowViews.put(rowKey, sheet.rowValues(rowKey));
            }

            for (final String columnKey : columnKeys) {
                columnViews.put(columnKey, sheet.columnValues(columnKey));
            }

            int nextId = 0;

            for (int step = 0; step < 12; step++) {
                applyRandomStructuralChange(sheet, rnd, nextId++);

                for (final Map.Entry<String, ImmutableList<Integer>> entry : rowViews.entrySet()) {
                    final String rowKey = entry.getKey();
                    final ImmutableList<Integer> view = entry.getValue();

                    if (sheet.containsRow(rowKey)) {
                        assertEquals(new ArrayList<>(sheet.rowValues(rowKey)), new ArrayList<>(view),
                                "row view for " + rowKey + " diverged from a fresh lookup at step " + step);
                    } else {
                        assertThrows(IllegalArgumentException.class, () -> view.get(0), "row view for removed key " + rowKey + " should throw at step " + step);
                    }
                }

                for (final Map.Entry<String, ImmutableList<Integer>> entry : columnViews.entrySet()) {
                    final String columnKey = entry.getKey();
                    final ImmutableList<Integer> view = entry.getValue();

                    if (sheet.containsColumn(columnKey)) {
                        assertEquals(new ArrayList<>(sheet.columnValues(columnKey)), new ArrayList<>(view),
                                "column view for " + columnKey + " diverged from a fresh lookup at step " + step);
                    } else {
                        assertThrows(IllegalArgumentException.class, () -> view.get(0),
                                "column view for removed key " + columnKey + " should throw at step " + step);
                    }
                }
            }
        }
    }

}

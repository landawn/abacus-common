package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.UUID;

import org.junit.jupiter.api.Test;

public class SheetToTest extends SheetTestSupport {
    @Test
    public void testDatasetNameCollisionsExplainArrayIdentityOnce() {
        final int[] firstArray = { 1, 2 };
        final int[] secondArray = { 1, 2 };
        // Cover two arrays and mixed keys with the array on either side of the collision.
        for (final Object[] keys : new Object[][] { { firstArray, secondArray }, { firstArray, "[1, 2]" }, { "[1, 2]", secondArray } }) {
            for (final boolean rows : new boolean[] { true, false }) {
                final Sheet<Object, Object, Integer> target = new Sheet<>(rows ? Arrays.asList(keys) : Arrays.asList("other"),
                        rows ? Arrays.asList("other") : Arrays.asList(keys));
                final String message = assertThrows(IllegalArgumentException.class, () -> {
                    if (rows) {
                        target.toTransposedDataset();
                    } else {
                        target.toDataset();
                    }
                }).getMessage();
                assertTrue(message.contains(rows ? "Row keys" : "Column keys"), message);
                assertTrue(message.contains("both map to the Dataset column name \"[1, 2]\""), message);
                for (final Object key : keys) {
                    if (key.getClass().isArray()) {
                        assertTrue(message.contains("[1, 2]@" + Integer.toHexString(System.identityHashCode(key))), message);
                    }
                }
                final String note = " (array keys match by identity)";
                assertTrue(message.endsWith(note), message);
                assertEquals(message.indexOf(note), message.lastIndexOf(note), message);
            }
        }
    }

    @Test
    public void testToDatasetH() {
        Dataset ds = sheet.toDataset();
        assertNotNull(ds);
        assertEquals(3, ds.size());
        assertEquals(3, ds.columnCount());
    }

    @Test
    public void testToDatasetH_uninitialized() {
        Sheet<String, String, String> uninit = new Sheet<>(rowKeys, colKeys);
        Dataset ds = uninit.toDataset();
        assertEquals(CommonUtil.toList("C1", "C2", "C3"), ds.columnNames());
        assertEquals(3, ds.size());
        assertNull(ds.moveToRow(0).get("C1"));
    }

    @Test
    public void testToDatasetH_WithData() {
        Dataset ds = sheet.toDataset();
        assertNotNull(ds);
    }

    @Test
    public void testToDatasetH_ColumnNames() {
        Dataset ds = sheet.toDataset();
        assertNotNull(ds);
        assertEquals(3, ds.size());
        assertEquals(Arrays.asList("col1", "col2", "col3"), ds.columnNames());
    }

    @Test
    public void testToDatasetV_UninitializedSheet() {
        Sheet<String, String, Integer> uninitSheet = new Sheet<>(rowKeys, columnKeys);
        Dataset ds = uninitSheet.toTransposedDataset();
        assertEquals(Arrays.asList("row1", "row2", "row3"), ds.columnNames());
        assertEquals(3, ds.size());
    }

    @Test
    public void testToDatasetV() {
        Dataset ds = sheet.toTransposedDataset();
        assertNotNull(ds);
        assertEquals(3, ds.size());
        assertEquals(3, ds.columnCount());
    }

    @Test
    public void testToDatasetV_WithData() {
        Dataset ds = sheet.toTransposedDataset();
        assertNotNull(ds);
    }

    @Test
    public void testToDatasetV_ColumnNames() {
        Dataset ds = sheet.toTransposedDataset();
        assertNotNull(ds);
        assertEquals(3, ds.size());
        assertEquals(Arrays.asList("row1", "row2", "row3"), ds.columnNames());
    }

    @Test
    public void testToArrayHTyped() {
        Sheet<String, String, Integer> uninitSheet = new Sheet<>(upperRowKeys, colKeys);
        uninitSheet.set("R1", "C1", 1);
        uninitSheet.set("R1", "C2", 2);
        uninitSheet.set("R2", "C1", 3);
        uninitSheet.set("R2", "C2", 4);

        Integer[][] array = uninitSheet.toArray(Integer.class);

        assertEquals(3, array.length);
        assertEquals(3, array[0].length);
        assertEquals(Integer.valueOf(1), array[0][0]);
        assertEquals(Integer.valueOf(2), array[0][1]);
    }

    @Test
    public void testToArrayH_WithClass_New() {
        Integer[][] arr = intSheet.toArray(Integer.class);
        assertEquals(3, arr.length);
        assertEquals(3, arr[0].length);
        assertEquals(Integer.valueOf(11), arr[0][0]);
        assertEquals(Integer.valueOf(12), arr[0][1]);
        assertEquals(Integer.valueOf(33), arr[2][2]);
    }

    @Test
    public void testToArrayH() {
        Object[][] array = sheet.toArray();
        assertNotNull(array);
        assertEquals(3, array.length);
        assertEquals(3, array[0].length);
        assertEquals(Integer.valueOf(1), array[0][0]);
        assertEquals(Integer.valueOf(9), array[2][2]);
    }

    @Test
    public void testToArrayHWithComponentType() {
        Integer[][] array = sheet.toArray(Integer.class);
        assertNotNull(array);
        assertEquals(3, array.length);
        assertEquals(3, array[0].length);
        assertEquals(Integer.valueOf(1), array[0][0]);
        assertEquals(Integer.valueOf(9), array[2][2]);
    }

    @Test
    public void testToArrayH_uninitialized() {
        Sheet<String, String, String> uninit = new Sheet<>(rowKeys, colKeys);
        Object[][] arr = uninit.toArray();
        assertEquals(3, arr.length);
        assertEquals(3, arr[0].length);
        assertNull(arr[0][0]);
    }

    @Test
    public void testToArrayH_EmptySheet() {
        Sheet<String, String, Integer> empty = new Sheet<>();
        Object[][] arr = empty.toArray();
        assertEquals(0, arr.length);
    }

    @Test
    public void testToArrayH_typed() {
        Sheet<String, String, String> stringSheet = new Sheet<>(Arrays.asList("R1"), Arrays.asList("C1", "C2"));
        stringSheet.set("R1", "C1", "S11");
        stringSheet.set("R1", "C2", "S12");

        String[][] arr = stringSheet.toArray(String.class);
        assertEquals(1, arr.length);
        assertEquals(2, arr[0].length);
        assertEquals("S11", arr[0][0]);

        assertThrows(ArrayStoreException.class, () -> {
            objectSheet.toArray(Integer.class);
        });
    }

    @Test
    public void testToArrayVTyped() {
        Sheet<String, String, Integer> uninitSheet = new Sheet<>(upperRowKeys, colKeys);
        uninitSheet.set("R1", "C1", 1);
        uninitSheet.set("R1", "C2", 2);
        uninitSheet.set("R2", "C1", 3);
        uninitSheet.set("R2", "C2", 4);

        Integer[][] array = uninitSheet.toTransposedArray(Integer.class);

        assertEquals(3, array.length);
        assertEquals(3, array[0].length);
        assertEquals(Integer.valueOf(1), array[0][0]);
        assertEquals(Integer.valueOf(3), array[0][1]);
    }

    @Test
    public void testToArrayV() {
        Object[][] array = sheet.toTransposedArray();
        assertNotNull(array);
        assertEquals(3, array.length);
        assertEquals(3, array[0].length);
        assertEquals(Integer.valueOf(1), array[0][0]);
        assertEquals(Integer.valueOf(9), array[2][2]);
    }

    @Test
    public void testToArrayVWithComponentType() {
        Integer[][] array = sheet.toTransposedArray(Integer.class);
        assertNotNull(array);
        assertEquals(3, array.length);
        assertEquals(3, array[0].length);
        assertEquals(Integer.valueOf(1), array[0][0]);
        assertEquals(Integer.valueOf(9), array[2][2]);
    }

    @Test
    public void testToArrayV_uninitialized() {
        Sheet<String, String, String> uninit = new Sheet<>(rowKeys, colKeys);
        Object[][] arr = uninit.toTransposedArray();
        assertEquals(3, arr.length);
        assertEquals(3, arr[0].length);
        assertNull(arr[0][0]);
    }

    @Test
    public void testToArrayV_EmptySheet() {
        Sheet<String, String, Integer> empty = new Sheet<>();
        Object[][] arr = empty.toTransposedArray();
        assertEquals(0, arr.length);
    }

    @Test
    public void testToArrayV_typed() {
        Sheet<String, String, String> stringSheet = new Sheet<>(Arrays.asList("R1", "R2"), Arrays.asList("C1"));
        stringSheet.set("R1", "C1", "S11");
        stringSheet.set("R2", "C1", "S21");

        String[][] arr = stringSheet.toTransposedArray(String.class);
        assertEquals(1, arr.length);
        assertEquals(2, arr[0].length);
        assertEquals("S11", arr[0][0]);
        assertEquals("S21", arr[0][1]);

        assertThrows(ArrayStoreException.class, () -> {
            objectSheet.toTransposedArray(UUID.class);
        });
    }

    @Test
    public void testToString_NotNull() {
        String str = sheet.toString();
        assertNotNull(str);
        assertTrue(str.length() > 0);
    }

    @Test
    public void testToString_EmptySheet() {
        String str = emptySheet.toString();
        assertNotNull(str);
    }

    @Test
    public void testToString_UninitializedSheet() {
        Sheet<String, String, Integer> uninitSheet = new Sheet<>(rowKeys, columnKeys);
        String str = uninitSheet.toString();
        assertNotNull(str);
        assertTrue(str.length() > 0);
    }

}

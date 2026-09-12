package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.util.stream.Stream;

public class SheetRowsTest extends SheetTestSupport {
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
    public void testRowsWithCollection() {
        List<List<Integer>> rows = Arrays.asList(Arrays.asList(1, 2, 3), Arrays.asList(4, 5, 6), Arrays.asList(7, 8, 9));
        Sheet<String, String, Integer> sheetLocal = Sheet.rows(upperRowKeys, colKeys, rows);
        assertEquals(5, sheetLocal.get("R2", "C2"));
    }

    @Test
    public void testRows_PairStreamContent() {
        List<Pair<String, Stream<Integer>>> rowPairs = sheet.rows().toList();
        assertEquals(3, rowPairs.size());
        assertEquals("row1", rowPairs.get(0).left());
        assertEquals(Arrays.asList(1, 2, 3), rowPairs.get(0).right().toList());
        assertEquals("row3", rowPairs.get(2).left());
        assertEquals(Arrays.asList(7, 8, 9), rowPairs.get(2).right().toList());
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
    public void testRows_pairStream_emptyRange() {
        assertTrue(objectSheet.rows(1, 1).toList().isEmpty());
    }

    // ==================== tests for untested methods ====================

    @Test
    public void testRowsFactoryWithNullParams() {
        // rows(null, null, (List)null) should produce empty sheet
        Sheet<String, String, Object> s = Sheet.rows(null, null, (List<List<Object>>) null);
        assertNotNull(s);
        assertEquals(0, s.rowCount());
        assertEquals(0, s.columnCount());
        assertTrue(s.isEmpty());
    }

    @Test
    public void testRowsFactoryWithEmptyLists() {
        Sheet<String, String, Integer> s = Sheet.rows(Collections.emptyList(), Collections.emptyList(), Collections.emptyList());
        assertNotNull(s);
        assertEquals(0, s.rowCount());
        assertEquals(0, s.columnCount());
        assertTrue(s.isEmpty());
    }

    @Test
    public void testRowsWithCollectionsSizeMismatch() {
        List<List<Integer>> rowData = Arrays.asList(Arrays.asList(1, 2, 3), Arrays.asList(4, 5, 6));
        assertThrows(IllegalArgumentException.class, () -> {
            Sheet.rows(rowKeys, columnKeys, rowData);
        });
    }

    @Test
    public void testRowsFactory_fromCollection_mismatchDimensions() {
        List<List<String>> rowsDataMismatchRow = Arrays.asList(Arrays.asList("V11", "V12"));
        List<String> rk = Arrays.asList("R1", "R2");
        List<String> ck = Arrays.asList("C1", "C2");
        IllegalArgumentException rowMismatch = assertThrows(IllegalArgumentException.class, () -> Sheet.rows(rk, ck, rowsDataMismatchRow));
        assertEquals("The size of row collection is not equal to size of row key set", rowMismatch.getMessage());

        List<List<String>> rowsDataMismatchCol = Arrays.asList(Arrays.asList("V11"), Arrays.asList("V21"));
        IllegalArgumentException colMismatch = assertThrows(IllegalArgumentException.class, () -> Sheet.rows(rk, ck, rowsDataMismatchCol));
        assertEquals("The size of row is not equal to size of column key set", colMismatch.getMessage());
    }

    @Test
    public void testRowsFactoryWithMismatchedData() {
        List<List<Integer>> rows = Arrays.asList(Arrays.asList(1, 2), Arrays.asList(4, 5, 6), Arrays.asList(7, 8, 9));
        assertThrows(IllegalArgumentException.class, () -> Sheet.rows(rowKeys, columnKeys, rows));
    }

    @Test
    public void testRowsStream_InvalidRange() {
        assertThrows(IndexOutOfBoundsException.class, () -> sheet.rows(-1, 2));
        assertThrows(IndexOutOfBoundsException.class, () -> sheet.rows(0, 10));
    }

    @Test
    public void testRows_WithMapperInvalidRange() {
        assertThrows(IndexOutOfBoundsException.class, () -> sheet.rows(-1, 2, (idx, arr) -> null));
        assertThrows(IndexOutOfBoundsException.class, () -> sheet.rows(0, 10, (idx, arr) -> null));
    }

    @Test
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
    public void testRowsMap_UninitializedSheet() {
        Sheet<String, String, Integer> uninitSheet = new Sheet<>(rowKeys, columnKeys);
        Map<String, Map<String, Integer>> rMap = uninitSheet.rowsMap();
        assertEquals(3, rMap.size());
        assertNull(rMap.get("row1").get("col1"));
    }

    @Test
    public void testRowsMap_EmptySheet() {
        Map<String, Map<String, Integer>> rMap = emptySheet.rowsMap();
        assertTrue(rMap.isEmpty());
    }

    @Test
    public void testRowsFactory_fromArray_mismatchDimensions() {
        // The row-array path reported both dimension failures with one identical, axis-less message
        // ("The length of array is not equal to size of row/column key set"); its three siblings - and the
        // assertions in testRowsFactory_fromCollection_mismatchDimensions - each name the axis that is wrong.
        final List<String> rk = Arrays.asList("R1", "R2");
        final List<String> ck = Arrays.asList("C1", "C2");

        final IllegalArgumentException rowMismatch = assertThrows(IllegalArgumentException.class, () -> Sheet.rows(rk, ck, new Integer[][] { { 1, 2 } }));
        assertEquals("The length of row array is not equal to size of row key set", rowMismatch.getMessage());

        final IllegalArgumentException colMismatch = assertThrows(IllegalArgumentException.class,
                () -> Sheet.rows(rk, ck, new Integer[][] { { 1 }, { 2 } }));
        assertEquals("The length of row is not equal to size of column key set", colMismatch.getMessage());

        // The column-array twin already distinguished them; pinned here so the two stay symmetric.
        final IllegalArgumentException columnCountMismatch = assertThrows(IllegalArgumentException.class,
                () -> Sheet.columns(rk, ck, new Integer[][] { { 1, 2 } }));
        assertEquals("The length of column array is not equal to size of column key set", columnCountMismatch.getMessage());
    }
}

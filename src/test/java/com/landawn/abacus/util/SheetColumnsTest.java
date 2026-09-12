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

public class SheetColumnsTest extends SheetTestSupport {
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
    public void testColumnsWithCollection() {
        List<List<Integer>> columns = Arrays.asList(Arrays.asList(1, 4, 7), Arrays.asList(2, 5, 8), Arrays.asList(3, 6, 9));
        Sheet<String, String, Integer> sheetLocal = Sheet.columns(upperRowKeys, colKeys, columns);
        assertEquals(5, sheetLocal.get("R2", "C2"));
    }

    @Test
    public void testColumnsFactoryWithCollections() {
        List<List<Integer>> colsData = Arrays.asList(Arrays.asList(1, 4, 7), Arrays.asList(2, 5, 8), Arrays.asList(3, 6, 9));
        Sheet<String, String, Integer> s = Sheet.columns(rowKeys, columnKeys, colsData);
        assertEquals(Integer.valueOf(1), s.get("row1", "col1"));
        assertEquals(Integer.valueOf(5), s.get("row2", "col2"));
        assertEquals(Integer.valueOf(9), s.get("row3", "col3"));
    }

    @Test
    public void testColumns_PairStreamContent() {
        List<Pair<String, Stream<Integer>>> colPairs = sheet.columns().toList();
        assertEquals(3, colPairs.size());
        assertEquals("col1", colPairs.get(0).left());
        assertEquals(Arrays.asList(1, 4, 7), colPairs.get(0).right().toList());
        assertEquals("col3", colPairs.get(2).left());
        assertEquals(Arrays.asList(3, 6, 9), colPairs.get(2).right().toList());
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
    public void testColumns_pairStream_emptyRange() {
        assertTrue(objectSheet.columns(1, 1).toList().isEmpty());
    }

    @Test
    public void testColumnsFactoryWithEmptyCollections() {
        Sheet<String, String, Integer> s = Sheet.columns(Arrays.asList("R1", "R2"), Arrays.asList("C1", "C2"), Collections.emptyList());
        assertNotNull(s);
        assertEquals(2, s.rowCount());
        assertEquals(2, s.columnCount());
        assertNull(s.get("R1", "C1"));
    }

    @Test
    public void testColumnsFactoryWithNullCollections() {
        Sheet<String, String, Object> s = Sheet.columns(null, null, (List<List<Object>>) null);
        assertNotNull(s);
        assertEquals(0, s.rowCount());
        assertEquals(0, s.columnCount());
    }

    @Test
    public void testColumnsWithCollectionsSizeMismatch() {
        List<List<Integer>> columnData = Arrays.asList(Arrays.asList(1, 4, 7), Arrays.asList(2, 5, 8));
        assertThrows(IllegalArgumentException.class, () -> {
            Sheet.columns(rowKeys, columnKeys, columnData);
        });
    }

    @Test
    public void testColumnsStream_InvalidRange() {
        assertThrows(IndexOutOfBoundsException.class, () -> sheet.columns(-1, 2));
        assertThrows(IndexOutOfBoundsException.class, () -> sheet.columns(0, 10));
    }

    @Test
    public void testColumnsFactoryWithCollections_MismatchedColumnSize() {
        List<List<Integer>> colsData = Arrays.asList(Arrays.asList(1, 4), Arrays.asList(2, 5, 8));
        List<String> rk = Arrays.asList("R1", "R2", "R3");
        List<String> ck = Arrays.asList("C1", "C2");
        assertThrows(IllegalArgumentException.class, () -> Sheet.columns(rk, ck, colsData));
    }

    @Test
    public void testColumns_WithMapperInvalidRange() {
        assertThrows(IndexOutOfBoundsException.class, () -> sheet.columns(-1, 2, (idx, arr) -> null));
        assertThrows(IndexOutOfBoundsException.class, () -> sheet.columns(0, 10, (idx, arr) -> null));
    }

    @Test
    public void testColumnsMap_DataIntegrity() {
        Map<String, Map<String, Integer>> columnsMap = sheet.columnsMap();
        assertEquals(3, columnsMap.size());
        // Verify all entries
        assertEquals(Integer.valueOf(1), columnsMap.get("col1").get("row1"));
        assertEquals(Integer.valueOf(4), columnsMap.get("col1").get("row2"));
        assertEquals(Integer.valueOf(7), columnsMap.get("col1").get("row3"));
        assertEquals(Integer.valueOf(2), columnsMap.get("col2").get("row1"));
    }

    @Test
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
    public void testColumnsMap_UninitializedSheet() {
        Sheet<String, String, Integer> uninitSheet = new Sheet<>(rowKeys, columnKeys);
        Map<String, Map<String, Integer>> cMap = uninitSheet.columnsMap();
        assertEquals(3, cMap.size());
        assertNull(cMap.get("col1").get("row1"));
    }

    @Test
    public void testColumnsMap_EmptySheet() {
        Map<String, Map<String, Integer>> cMap = emptySheet.columnsMap();
        assertTrue(cMap.isEmpty());
    }

}

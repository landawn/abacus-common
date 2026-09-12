package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.util.Sheet.Cell;
import com.landawn.abacus.util.stream.Stream;

public class SheetCellsTest extends SheetTestSupport {
    @Test
    public void testCellsH_emptyRange() {
        assertTrue(objectSheet.rowMajorCells(1, 1).toList().isEmpty());
    }

    @Test
    public void testCellsV_emptyRange() {
        assertTrue(objectSheet.columnMajorCells(1, 1).toList().isEmpty());
    }

    @Test
    public void testCellsR_emptyRange() {
        assertTrue(objectSheet.rowCells(1, 1).toList().isEmpty());
    }

    @Test
    public void testCellsC_emptyRange() {
        assertTrue(objectSheet.columnCells(1, 1).toList().isEmpty());
    }

    @Test
    public void testCellsV_EmptyRowCount_ReturnsEmpty() {
        Sheet<String, String, Integer> s = Sheet.rows(Collections.emptyList(), Arrays.asList("c1", "c2"), new Integer[0][]);
        assertEquals(0, s.columnMajorCells(0, 2).count());
    }

    @Test
    public void testCellsH() {
        Stream<Cell<String, String, Integer>> cells = sheet.rowMajorCells();
        List<Cell<String, String, Integer>> cellList = cells.toList();
        assertEquals(9, cellList.size());
        assertEquals(Integer.valueOf(1), cellList.get(0).value());
        assertEquals("row1", cellList.get(0).rowKey());
        assertEquals("col1", cellList.get(0).columnKey());
    }

    @Test
    public void testCellsHWithRange() {
        Stream<Cell<String, String, Integer>> cells = sheet.rowMajorCells(0, 2);
        List<Cell<String, String, Integer>> cellList = cells.toList();
        assertEquals(6, cellList.size());
    }

    @Test
    public void testCellsHRange() {
        Sheet<String, String, Integer> uninitSheet = new Sheet<>(upperRowKeys, colKeys);
        uninitSheet.set("R1", "C1", 1);
        uninitSheet.set("R2", "C2", 2);

        List<Cell<String, String, Integer>> cells = uninitSheet.rowMajorCells(1, 2).toList();
        assertEquals(3, cells.size());
    }

    @Test
    public void testCellsHWithCustomRange() {
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                sheet.setAt(i, j, i * 10 + j);
            }
        }

        List<Cell<String, String, Integer>> cells = sheet.rowMajorCells(0, 2).toList();
        assertEquals(6, cells.size());
        assertEquals(Integer.valueOf(0), cells.get(0).value());
        assertEquals(Integer.valueOf(12), cells.get(5).value());
    }

    @Test
    public void testCellsH_ValueOrder() {
        List<Cell<String, String, Integer>> cells = sheet.rowMajorCells().toList();
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
    public void testCellsH_CountMethod() {
        Sheet<String, String, Integer> s = Sheet.rows(Arrays.asList("r1", "r2"), Arrays.asList("c1", "c2"), new Integer[][] { { 1, 2 }, { 3, 4 } });
        assertEquals(4, s.rowMajorCells().count());
        assertEquals(2, s.rowMajorCells(0, 1).count()); // one row, two columns
    }

    @Test
    public void testCellsH_SkipMethod_TriggersAdvance() {
        Sheet<String, String, Integer> s = Sheet.rows(Arrays.asList("r1", "r2"), Arrays.asList("c1", "c2"), new Integer[][] { { 1, 2 }, { 3, 4 } });
        assertEquals(3, s.rowMajorCells().skip(1).count());
        assertEquals(1, s.rowMajorCells().skip(3).count());
    }

    @Test
    public void testCellsHWithSingleRow() {
        Sheet<String, String, Integer> uninitSheet = new Sheet<>(upperRowKeys, colKeys);
        uninitSheet.set("R2", "C1", 21);
        uninitSheet.set("R2", "C2", 22);
        uninitSheet.set("R2", "C3", 23);

        List<Cell<String, String, Integer>> cells = uninitSheet.rowMajorCells(1, 2).toList();
        assertEquals(3, cells.size());
        assertEquals("R2", cells.get(0).rowKey());
        assertEquals(Integer.valueOf(21), cells.get(0).value());
    }

    @Test
    public void testCellsHWithInvalidRange() {
        assertThrows(IndexOutOfBoundsException.class, () -> {
            sheet.rowMajorCells(-1, 2);
        });
        assertThrows(IndexOutOfBoundsException.class, () -> {
            sheet.rowMajorCells(0, 10);
        });
    }

    @Test
    public void testCellsHWithInvalidFromIndex() {
        assertThrows(IndexOutOfBoundsException.class, () -> sheet.rowMajorCells(-1, 2));
    }

    @Test
    public void testCellsHWithInvalidToIndex() {
        assertThrows(IndexOutOfBoundsException.class, () -> sheet.rowMajorCells(0, 5));
    }

    @Test
    public void testCellsHWithFromGreaterThanTo() {
        assertThrows(IndexOutOfBoundsException.class, () -> sheet.rowMajorCells(2, 1));
    }

    @Test
    public void testCellsH_NoSuchElementException() {
        Sheet<String, String, Integer> s = Sheet.rows(Arrays.asList("r1"), Arrays.asList("c1"), new Integer[][] { { 1 } });
        com.landawn.abacus.util.ObjIterator<Cell<String, String, Integer>> iter = s.rowMajorCells().iterator();
        while (iter.hasNext()) {
            iter.next();
        }
        assertThrows(java.util.NoSuchElementException.class, iter::next);
    }

    @Test
    public void testCellsV() {
        Stream<Cell<String, String, Integer>> cells = sheet.columnMajorCells();
        List<Cell<String, String, Integer>> cellList = cells.toList();
        assertEquals(9, cellList.size());
        assertEquals(Integer.valueOf(1), cellList.get(0).value());
        assertEquals("row1", cellList.get(0).rowKey());
        assertEquals("col1", cellList.get(0).columnKey());
    }

    @Test
    public void testCellsVWithRange() {
        Stream<Cell<String, String, Integer>> cells = sheet.columnMajorCells(0, 2);
        List<Cell<String, String, Integer>> cellList = cells.toList();
        assertEquals(6, cellList.size());
    }

    @Test
    public void testCellsVWithCustomRange() {
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                sheet.setAt(i, j, i * 10 + j);
            }
        }

        List<Cell<String, String, Integer>> cells = sheet.columnMajorCells(1, 3).toList();
        assertEquals(6, cells.size());
        assertEquals(Integer.valueOf(1), cells.get(0).value());
        assertEquals(Integer.valueOf(22), cells.get(5).value());
    }

    @Test
    public void testCellsV_ValueOrder() {
        List<Cell<String, String, Integer>> cells = sheet.columnMajorCells().toList();
        assertEquals(9, cells.size());
        // col1: row1=1, row2=4, row3=7; col2: row1=2, row2=5, ...
        assertEquals(Integer.valueOf(1), cells.get(0).value());
        assertEquals(Integer.valueOf(4), cells.get(1).value());
        assertEquals(Integer.valueOf(7), cells.get(2).value());
        assertEquals(Integer.valueOf(2), cells.get(3).value());
    }

    @Test
    public void testCellsV_CountMethod() {
        Sheet<String, String, Integer> s = Sheet.rows(Arrays.asList("r1", "r2"), Arrays.asList("c1", "c2"), new Integer[][] { { 1, 2 }, { 3, 4 } });
        assertEquals(4, s.columnMajorCells().count());
        assertEquals(2, s.columnMajorCells(0, 1).count()); // one column, two rows
    }

    @Test
    public void testCellsV_SkipMethod_TriggersAdvance() {
        Sheet<String, String, Integer> s = Sheet.rows(Arrays.asList("r1", "r2"), Arrays.asList("c1", "c2"), new Integer[][] { { 1, 2 }, { 3, 4 } });
        assertEquals(3, s.columnMajorCells().skip(1).count());
        assertEquals(1, s.columnMajorCells().skip(3).count());
    }

    @Test
    public void testCellsVWithSingleColumn() {
        Sheet<String, String, Integer> uninitSheet = new Sheet<>(upperRowKeys, colKeys);
        uninitSheet.set("R1", "C2", 12);
        uninitSheet.set("R2", "C2", 22);
        uninitSheet.set("R3", "C2", 32);

        List<Cell<String, String, Integer>> cells = uninitSheet.columnMajorCells(1, 2).toList();
        assertEquals(3, cells.size());
        assertEquals("C2", cells.get(0).columnKey());
        assertEquals(Integer.valueOf(12), cells.get(0).value());
    }

    @Test
    public void testCellsV_InvalidRange() {
        assertThrows(IndexOutOfBoundsException.class, () -> sheet.columnMajorCells(-1, 2));
        assertThrows(IndexOutOfBoundsException.class, () -> sheet.columnMajorCells(0, 10));
    }

    @Test
    public void testCellsV_NoSuchElementException() {
        Sheet<String, String, Integer> s = Sheet.rows(Arrays.asList("r1"), Arrays.asList("c1"), new Integer[][] { { 1 } });
        com.landawn.abacus.util.ObjIterator<Cell<String, String, Integer>> iter = s.columnMajorCells().iterator();
        while (iter.hasNext()) {
            iter.next();
        }
        assertThrows(java.util.NoSuchElementException.class, iter::next);
    }

    @Test
    public void testCellsR() {
        Stream<Stream<Cell<String, String, Integer>>> rowCells = sheet.rowCells();
        List<List<Cell<String, String, Integer>>> result = rowCells.map(Stream::toList).toList();
        assertEquals(3, result.size());
        assertEquals(3, result.get(0).size());
    }

    @Test
    public void testCellsRWithRange() {
        Stream<Stream<Cell<String, String, Integer>>> rowCells = sheet.rowCells(0, 2);
        List<List<Cell<String, String, Integer>>> result = rowCells.map(Stream::toList).toList();
        assertEquals(2, result.size());
    }

    @Test
    public void testCellsR_ValueOrder() {
        List<List<Cell<String, String, Integer>>> cellRows = sheet.rowCells().map(Stream::toList).toList();
        assertEquals(3, cellRows.size());
        assertEquals(3, cellRows.get(0).size());
        assertEquals(Integer.valueOf(1), cellRows.get(0).get(0).value());
        assertEquals(Integer.valueOf(4), cellRows.get(1).get(0).value());
    }

    @Test
    public void testCellsR_InvalidRange() {
        assertThrows(IndexOutOfBoundsException.class, () -> sheet.rowCells(-1, 2));
        assertThrows(IndexOutOfBoundsException.class, () -> sheet.rowCells(0, 10));
    }

    @Test
    public void testCellsC() {
        Stream<Stream<Cell<String, String, Integer>>> columnCells = sheet.columnCells();
        List<List<Cell<String, String, Integer>>> result = columnCells.map(Stream::toList).toList();
        assertEquals(3, result.size());
        assertEquals(3, result.get(0).size());
    }

    @Test
    public void testCellsCWithRange() {
        Stream<Stream<Cell<String, String, Integer>>> columnCells = sheet.columnCells(0, 2);
        List<List<Cell<String, String, Integer>>> result = columnCells.map(Stream::toList).toList();
        assertEquals(2, result.size());
    }

    @Test
    public void testCellsC_ValueOrder() {
        List<List<Cell<String, String, Integer>>> cellCols = sheet.columnCells().map(Stream::toList).toList();
        assertEquals(3, cellCols.size());
        assertEquals(3, cellCols.get(0).size());
        // First column: row1=1, row2=4, row3=7
        assertEquals(Integer.valueOf(1), cellCols.get(0).get(0).value());
        assertEquals(Integer.valueOf(4), cellCols.get(0).get(1).value());
        assertEquals(Integer.valueOf(7), cellCols.get(0).get(2).value());
    }

    @Test
    public void testCellsC_InvalidRange() {
        assertThrows(IndexOutOfBoundsException.class, () -> sheet.columnCells(-1, 2));
        assertThrows(IndexOutOfBoundsException.class, () -> sheet.columnCells(0, 10));
    }

    @Test
    public void testCellsByRow_YieldsAllTriples() {
        Sheet<String, String, Integer> s = Sheet.rows(Arrays.asList("r1", "r2"), Arrays.asList("c1", "c2"), new Integer[][] { { 1, 2 }, { 3, 4 } });
        List<Sheet.Cell<String, String, Integer>> cells = s.rowMajorCells().toList();
        assertEquals(4, cells.size());
        assertEquals(Sheet.Cell.of("r1", "c1", 1), cells.get(0));
        assertEquals(Sheet.Cell.of("r1", "c2", 2), cells.get(1));
        assertEquals(Sheet.Cell.of("r2", "c1", 3), cells.get(2));
        assertEquals(Sheet.Cell.of("r2", "c2", 4), cells.get(3));
    }

    @Test
    public void testMajorCellsExampleOutputMatchesTheJavadoc() {
        // Pins the "// Prints:" line of the rowMajorCells() javadoc example, which was the only one in the
        // cells/stream family with no stated output, against its columnMajorCells() twin.
        final Sheet<String, String, Integer> example = Sheet.rows(Arrays.asList("row1", "row2"), Arrays.asList("col1", "col2"),
                new Integer[][] { { 1, 2 }, { 3, 4 } });

        assertEquals("row1,col1=1  row1,col2=2  row2,col1=3  row2,col2=4",
                String.join("  ", example.rowMajorCells().map(c -> c.rowKey() + "," + c.columnKey() + "=" + c.value()).toList()));
        assertEquals("row1,col1=1  row2,col1=3  row1,col2=2  row2,col2=4",
                String.join("  ", example.columnMajorCells().map(c -> c.rowKey() + "," + c.columnKey() + "=" + c.value()).toList()));
    }
}

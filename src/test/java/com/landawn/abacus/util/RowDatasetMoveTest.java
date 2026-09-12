package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class RowDatasetMoveTest extends RowDatasetTestSupport {
    @Test
    public void testMoveColumn() {
        RowDataset ds = new RowDataset(columnNames, copyColumnList());
        ds.moveColumn("salary", 0);
        assertEquals("salary", ds.getColumnName(0));
        assertEquals("id", ds.getColumnName(1));
        assertEquals("name", ds.getColumnName(2));
        assertEquals("age", ds.getColumnName(3));
    }

    @Test
    public void testMoveColumnToEnd() {
        RowDataset ds = new RowDataset(columnNames, copyColumnList());
        ds.moveColumn("id", 3);
        assertEquals("name", ds.getColumnName(0));
        assertEquals("id", ds.getColumnName(3));
    }

    @Test
    public void testMoveColumnToSamePosition() {
        final RowDataset dataset = createThreeRowScoreDataset();
        dataset.moveColumn("id", 0);
        List<String> names = dataset.columnNames();
        Assertions.assertEquals("id", names.get(0));
    }

    @Test
    public void testMoveColumnInvalidPosition() {
        RowDataset ds = new RowDataset(columnNames, copyColumnList());
        assertThrows(IndexOutOfBoundsException.class, () -> {
            ds.moveColumn("id", 10);
        });
    }

    @Test
    public void testMoveColumnWithInvalidPosition() {
        final RowDataset dataset = createThreeRowScoreDataset();
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> {
            dataset.moveColumn("id", -1);
        });
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> {
            dataset.moveColumn("id", 5);
        });
    }

    @Test
    public void testMoveColumnSamePosition() {
        RowDataset ds = new RowDataset(columnNames, copyColumnList());
        ds.moveColumn("id", 0);
        assertEquals("id", ds.getColumnName(0));
    }

    @Test
    public void testMoveColumns() {
        RowDataset ds = new RowDataset(columnNames, copyColumnList());
        ds.moveColumns(Arrays.asList("age", "salary"), 0);
        assertEquals("age", ds.getColumnName(0));
        assertEquals("salary", ds.getColumnName(1));
        assertEquals("id", ds.getColumnName(2));
        assertEquals("name", ds.getColumnName(3));
    }

    @Test
    public void testMoveColumnsContiguous() {
        RowDataset ds = new RowDataset(columnNames, copyColumnList());
        ds.moveColumns(Arrays.asList("name", "age"), 0);
        assertEquals("name", ds.getColumnName(0));
        assertEquals("age", ds.getColumnName(1));
    }

    @Test
    public void testMoveColumnsEmpty() {
        RowDataset ds = new RowDataset(columnNames, copyColumnList());
        ds.moveColumns(Arrays.asList(), 0);
        assertEquals("id", ds.getColumnName(0));
    }

    @Test
    public void testMoveColumnsSingleColumn() {
        RowDataset ds = new RowDataset(columnNames, copyColumnList());
        ds.moveColumns(Arrays.asList("salary"), 0);
        assertEquals("salary", ds.getColumnName(0));
    }

    @Test
    public void testMoveRow() {
        RowDataset ds = new RowDataset(columnNames, copyColumnList());
        Object firstValue = ds.get(0, 1);
        ds.moveRow(0, 2);
        assertEquals(firstValue, ds.get(2, 1));
    }

    @Test
    public void testMoveRowToEnd() {
        RowDataset ds = new RowDataset(columnNames, copyColumnList());
        Object firstValue = ds.get(0, 1);
        ds.moveRow(0, 4);
        assertEquals(firstValue, ds.get(4, 1));
    }

    @Test
    public void testMoveRowToSamePosition() {
        final RowDataset dataset = createThreeRowScoreDataset();
        Object originalRow1Name = dataset.get(1, 1);
        dataset.moveRow(1, 1);
        Assertions.assertEquals(originalRow1Name, dataset.get(1, 1));
    }

    // ========== moveRow - out of bounds ==========

    @Test
    public void testMoveRow_InvalidRowIndex_ThrowsIndexOutOfBounds() {
        RowDataset ds = new RowDataset(new ArrayList<>(Arrays.asList("id")), new ArrayList<>(Arrays.asList(new ArrayList<>(Arrays.asList(1, 2, 3)))));
        assertThrows(IndexOutOfBoundsException.class, () -> ds.moveRow(-1, 0));
        assertThrows(IndexOutOfBoundsException.class, () -> ds.moveRow(3, 0));
    }

    @Test
    public void testMoveRow_InvalidNewPosition_ThrowsIndexOutOfBounds() {
        RowDataset ds = new RowDataset(new ArrayList<>(Arrays.asList("id")), new ArrayList<>(Arrays.asList(new ArrayList<>(Arrays.asList(1, 2, 3)))));
        assertThrows(IndexOutOfBoundsException.class, () -> ds.moveRow(0, -1));
        assertThrows(IndexOutOfBoundsException.class, () -> ds.moveRow(0, 3));
    }

    @Test
    public void testMoveRowSamePosition() {
        RowDataset ds = new RowDataset(columnNames, copyColumnList());
        Object value = ds.get(0, 1);
        ds.moveRow(0, 0);
        assertEquals(value, ds.get(0, 1));
    }

    @Test
    public void testMoveRows() {
        RowDataset ds = new RowDataset(columnNames, copyColumnList());
        Object firstValue = ds.get(0, 1);
        ds.moveRows(0, 2, 3);
        assertEquals(firstValue, ds.get(3, 1));
    }

    @Test
    public void testMoveRowsToNewPosition() {
        RowDataset ds = new RowDataset(new ArrayList<>(Arrays.asList("id", "name")),
                new ArrayList<>(Arrays.asList(new ArrayList<>(Arrays.asList(1, 2, 3, 4, 5)), new ArrayList<>(Arrays.asList("A", "B", "C", "D", "E")))));
        ds.moveRows(0, 2, 3);
        assertEquals(3, (int) ds.get(0, 0));
        assertEquals(4, (int) ds.get(1, 0));
    }

    @Test
    public void testMoveRowsInvalidRange() {
        RowDataset ds = new RowDataset(columnNames, copyColumnList());
        assertThrows(IndexOutOfBoundsException.class, () -> {
            ds.moveRows(3, 2, 0);
        });
    }

    @Test
    public void testMoveToRow() {
        dataset.moveToRow(3);
        assertEquals(3, dataset.currentRowIndex());
    }

    @Test
    public void testMoveToRow_invalid() {
        assertThrows(Exception.class, () -> dataset.moveToRow(-1));
        assertThrows(Exception.class, () -> dataset.moveToRow(100));
    }

}

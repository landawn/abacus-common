package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class DatasetMoveTest extends DatasetTestSupport {
    @Test
    public void moveColumn() {
        sampleDataset.moveColumn("Age", 0);
        assertEquals(Arrays.asList("Age", "ID", "Name"), sampleDataset.columnNames());
        assertEquals((Integer) 35, sampleDataset.moveToRow(2).get("Age"));

        assertThrows(IndexOutOfBoundsException.class, () -> sampleDataset.moveColumn("ID", 5));
        assertThrows(IllegalArgumentException.class, () -> sampleDataset.moveColumn("NonExistent", 0));
    }

    @Test
    @DisplayName("Should move columns to end")
    public void testMoveColumnsToEnd() {
        dataset.moveColumns(Arrays.asList("id", "name"), 2);

        assertEquals("age", dataset.columnNames().get(0));
        assertEquals("salary", dataset.columnNames().get(1));
        assertEquals("id", dataset.columnNames().get(2));
        assertEquals("name", dataset.columnNames().get(3));
    }

    @Test
    @DisplayName("Should preserve data integrity after column move")
    public void testMoveColumnsDataIntegrity() {
        Map<String, List<Object>> originalData = new HashMap<>();
        Map<String, ImmutableList<Object>> columnMap = dataset.columnMap();
        for (String col : dataset.columnNames()) {
            originalData.put(col, new ArrayList<>(columnMap.get(col)));
        }

        dataset.moveColumns(Arrays.asList("age", "salary"), 0);

        Map<String, ImmutableList<Object>> newColumnMap = dataset.columnMap();
        assertEquals(originalData.get("age"), new ArrayList<>(newColumnMap.get("age")));
        assertEquals(originalData.get("salary"), new ArrayList<>(newColumnMap.get("salary")));
        assertEquals(originalData.get("id"), new ArrayList<>(newColumnMap.get("id")));
        assertEquals(originalData.get("name"), new ArrayList<>(newColumnMap.get("name")));
    }

    @Test
    @DisplayName("Should move all columns except one")
    public void testMoveColumnsAllButOne() {

        dataset.moveColumns(Arrays.asList("id", "name", "age"), 1);

        assertEquals("salary", dataset.columnNames().get(0));
        assertEquals("id", dataset.columnNames().get(1));
        assertEquals("name", dataset.columnNames().get(2));
        assertEquals("age", dataset.columnNames().get(3));
    }

    @Test
    @DisplayName("Should handle column move in different orders")
    public void testMoveColumnsNonConsecutive() {
        dataset.moveColumns(Arrays.asList("salary", "id"), 1);

        assertEquals("name", dataset.columnNames().get(0));
        assertEquals("salary", dataset.columnNames().get(1));
        assertEquals("id", dataset.columnNames().get(2));
        assertEquals("age", dataset.columnNames().get(3));
    }

    @Test
    @DisplayName("Should handle column move in different orders")
    public void testMoveColumnsNonConsecutive2() {
        dataset.moveColumns(Arrays.asList("id", "salary", "name", "age"), 0);

        assertEquals("id", dataset.columnNames().get(0));
        assertEquals("salary", dataset.columnNames().get(1));
        assertEquals("name", dataset.columnNames().get(2));
        assertEquals("age", dataset.columnNames().get(3));
    }

    @Test
    @DisplayName("Should handle column move in different orders")
    public void testMoveColumnsNonConsecutive3() {
        dataset.moveColumns(Arrays.asList("salary", "name"), 0);

        assertEquals("salary", dataset.columnNames().get(0));
        assertEquals("name", dataset.columnNames().get(1));
        assertEquals("id", dataset.columnNames().get(2));
        assertEquals("age", dataset.columnNames().get(3));
    }

    @Test
    @DisplayName("Should verify column order preservation with complex moves")
    public void testMoveColumnsComplexOrderPreservation() {
        dataset.moveColumns(CommonUtil.toList("salary"), 0);
        dataset.moveColumns(Arrays.asList("name", "age"), 2);

        assertEquals("salary", dataset.columnNames().get(0));
        assertEquals("id", dataset.columnNames().get(1));
        assertEquals("name", dataset.columnNames().get(2));
        assertEquals("age", dataset.columnNames().get(3));

        assertEquals(50000.0, dataset.getRow(0).get(0));
        assertEquals(1, dataset.getRow(0).get(1));
        assertEquals("John", dataset.getRow(0).get(2));
        assertEquals(25, dataset.getRow(0).get(3));
    }

    @Test
    @DisplayName("Should move single column")
    public void testMoveColumnsSingle() {

        dataset.moveColumns(CommonUtil.toList("age"), 0);

        assertEquals("age", dataset.columnNames().get(0));
        assertEquals("id", dataset.columnNames().get(1));
        assertEquals("name", dataset.columnNames().get(2));
        assertEquals("salary", dataset.columnNames().get(3));
        assertEquals(4, dataset.columnCount());
    }

    @Test
    @DisplayName("Should move multiple columns")
    public void testMoveColumnsMultiple() {
        dataset.moveColumns(Arrays.asList("name", "salary"), 0);

        assertEquals("name", dataset.columnNames().get(0));
        assertEquals("salary", dataset.columnNames().get(1));
        assertEquals("id", dataset.columnNames().get(2));
        assertEquals("age", dataset.columnNames().get(3));
    }

    @Test
    @DisplayName("Should handle moving columns to same position")
    public void testMoveColumnsToSamePosition() {
        List<String> originalColumns = new ArrayList<>(dataset.columnNames());

        dataset.moveColumns(Arrays.asList("name", "age"), 1);

        assertEquals(originalColumns, dataset.columnNames());
    }

    @Test
    @DisplayName("Should handle empty column collection")
    public void testMoveColumnsEmpty() {
        List<String> originalColumns = new ArrayList<>(dataset.columnNames());

        dataset.moveColumns(Collections.emptyList(), 2);

        assertEquals(originalColumns, dataset.columnNames());
    }

    @Test
    @DisplayName("Empty column collection is a no-op regardless of newPosition")
    public void testMoveColumnsEmptyOutOfRangePositionIsNoOp() {
        List<String> originalColumns = new ArrayList<>(dataset.columnNames());

        // Nothing to move -> must be a harmless no-op even for an otherwise-out-of-range position,
        // consistent with moveColumns(emptyList, inRangePosition) and the single-column path.
        dataset.moveColumns(Collections.emptyList(), dataset.columnCount() + 5);

        assertEquals(originalColumns, dataset.columnNames());
    }

    @Test
    @DisplayName("Should throw exception for non-existent column")
    public void testMoveColumnsNonExistent() {
        assertThrows(IllegalArgumentException.class, () -> dataset.moveColumns(Arrays.asList("invalid_column"), 0));
        assertThrows(IllegalArgumentException.class, () -> dataset.moveColumns(Arrays.asList("name", "invalid_column"), 0));
    }

    @Test
    @DisplayName("Should throw exception for duplicate columns")
    public void testMoveColumnsDuplicates() {
        assertThrows(IllegalArgumentException.class, () -> dataset.moveColumns(Arrays.asList("name", "name"), 0));
    }

    @Test
    @DisplayName("Should throw exception for invalid newPosition")
    public void testMoveColumnsInvalidPosition() {
        assertThrows(IndexOutOfBoundsException.class, () -> dataset.moveColumns(Arrays.asList("name"), -1));
        assertThrows(IndexOutOfBoundsException.class, () -> dataset.moveColumns(Arrays.asList("name"), 4));
        assertThrows(IndexOutOfBoundsException.class, () -> dataset.moveColumns(Arrays.asList("name", "age"), 3));
    }

    @Test
    @DisplayName("Should move rows to beginning")
    public void testMoveRowsToBeginning() {
        dataset.moveRows(3, 5, 0);
        assertEquals("Alice", dataset.getRow(0).get(1));
        assertEquals("Charlie", dataset.getRow(1).get(1));
        assertEquals("John", dataset.getRow(2).get(1));
        assertEquals("Jane", dataset.getRow(3).get(1));
        assertEquals("Bob", dataset.getRow(4).get(1));
    }

    @Test
    @DisplayName("Should move rows to end")
    public void testMoveRowsToEnd() {
        dataset.moveRows(0, 2, 3);
        assertEquals("Bob", dataset.getRow(0).get(1));
        assertEquals("Alice", dataset.getRow(1).get(1));
        assertEquals("Charlie", dataset.getRow(2).get(1));
        assertEquals("John", dataset.getRow(3).get(1));
        assertEquals("Jane", dataset.getRow(4).get(1));
    }

    @Test
    @DisplayName("Should handle moving rows backward")
    public void testMoveRowsBackward() {
        dataset.moveRows(3, 5, 1);
        assertEquals("John", dataset.getRow(0).get(1));
        assertEquals("Alice", dataset.getRow(1).get(1));
        assertEquals("Charlie", dataset.getRow(2).get(1));
        assertEquals("Jane", dataset.getRow(3).get(1));
        assertEquals("Bob", dataset.getRow(4).get(1));
    }

    @Test
    @DisplayName("Should move entire dataset")
    public void testMoveRowsEntireDataset() {
        List<String> originalNames = new ArrayList<>();
        for (int i = 0; i < dataset.size(); i++) {
            originalNames.add((String) dataset.getRow(i).get(1));
        }

        dataset.moveRows(0, 5, 0);

        for (int i = 0; i < dataset.size(); i++) {
            assertEquals(originalNames.get(i), dataset.getRow(i).get(1));
        }
    }

    @Test
    @DisplayName("Should verify row data after complex moves")
    public void testMoveRowsComplexDataVerification() {
        Object[] originalRow0 = dataset.getRow(0, Object[].class);
        Object[] originalRow2 = dataset.getRow(2, Object[].class);
        Object[] originalRow4 = dataset.getRow(4, Object[].class);

        dataset.moveRows(2, 3, 4);

        assertEquals(Arrays.asList(originalRow0), dataset.getRow(0));
        assertEquals(Arrays.asList(originalRow2), dataset.getRow(4));
        assertEquals(Arrays.asList(originalRow4), dataset.getRow(3));
    }

    @Test
    @DisplayName("Should handle moving adjacent blocks of rows")
    public void testMoveRowsAdjacentBlocks() {
        dataset.moveRows(1, 3, 3);
        dataset.moveRows(0, 1, 2);

        assertEquals("Alice", dataset.getRow(0).get(1));
        assertEquals("Charlie", dataset.getRow(1).get(1));
        assertEquals("John", dataset.getRow(2).get(1));
        assertEquals("Jane", dataset.getRow(3).get(1));
        assertEquals("Bob", dataset.getRow(4).get(1));
    }

    @Test
    @DisplayName("Should move single row within valid range")
    public void testMoveRowsSingleRow() {
        dataset.moveRows(0, 1, 3);
        assertEquals("Jane", dataset.getRow(0).get(1));
        assertEquals("Bob", dataset.getRow(1).get(1));
        assertEquals("Alice", dataset.getRow(2).get(1));
        assertEquals("John", dataset.getRow(3).get(1));
        assertEquals("Charlie", dataset.getRow(4).get(1));
        assertEquals(5, dataset.size());
    }

    @Test
    @DisplayName("Should move multiple consecutive rows")
    public void testMoveRowsMultipleRows() {
        dataset.moveRows(1, 3, 3);
        assertEquals("John", dataset.getRow(0).get(1));
        assertEquals("Alice", dataset.getRow(1).get(1));
        assertEquals("Charlie", dataset.getRow(2).get(1));
        assertEquals("Jane", dataset.getRow(3).get(1));
        assertEquals("Bob", dataset.getRow(4).get(1));
        assertEquals(5, dataset.size());
    }

    @Test
    @DisplayName("Should not change dataset when moving to same position")
    public void testMoveRowsToSamePosition() {
        List<String> originalNames = new ArrayList<>();
        for (int i = 0; i < dataset.size(); i++) {
            originalNames.add((String) dataset.getRow(i).get(1));
        }

        dataset.moveRows(1, 3, 1);

        for (int i = 0; i < dataset.size(); i++) {
            assertEquals(originalNames.get(i), dataset.getRow(i).get(1));
        }
    }

    @Test
    @DisplayName("Should handle edge case of moving last row")
    public void testMoveRowsLastRowEdgeCase() {
        dataset.moveRows(4, 5, 0);
        assertEquals("Charlie", dataset.getRow(0).get(1));
        assertEquals("John", dataset.getRow(1).get(1));

        dataset.moveRows(0, 1, 4);
        assertEquals("John", dataset.getRow(0).get(1));
        assertEquals("Charlie", dataset.getRow(4).get(1));
    }

    @Test
    @DisplayName("Should throw exception for invalid fromRowIndex")
    public void testMoveRowsInvalidFromIndex() {
        assertThrows(IndexOutOfBoundsException.class, () -> dataset.moveRows(-1, 2, 3));
        dataset.moveRows(5, 5, 0);
    }

    @Test
    @DisplayName("Should throw exception for invalid toRowIndex")
    public void testMoveRowsInvalidToIndex() {
        assertThrows(IndexOutOfBoundsException.class, () -> dataset.moveRows(0, -1, 2));
        assertThrows(IndexOutOfBoundsException.class, () -> dataset.moveRows(0, 6, 2));
        assertThrows(IndexOutOfBoundsException.class, () -> dataset.moveRows(2, 7, 0));
    }

    @Test
    @DisplayName("Should throw exception when fromIndex > toIndex")
    public void testMoveRowsInvalidRange() {
        assertThrows(IndexOutOfBoundsException.class, () -> dataset.moveRows(3, 1, 0));
    }

    @Test
    @DisplayName("Should throw exception for invalid newPosition")
    public void testMoveRowsInvalidNewPosition() {
        assertThrows(IndexOutOfBoundsException.class, () -> dataset.moveRows(0, 1, -1));
        assertThrows(IndexOutOfBoundsException.class, () -> dataset.moveRows(0, 2, 4));
        assertThrows(IndexOutOfBoundsException.class, () -> dataset.moveRows(1, 4, 3));
    }

}

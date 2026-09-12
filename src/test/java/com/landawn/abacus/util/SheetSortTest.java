package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.junit.jupiter.api.Test;

public class SheetSortTest extends SheetTestSupport {
    @Test
    public void testSortByRowOnFrozenSheet() {
        sheet.freeze();
        assertThrows(IllegalStateException.class, () -> {
            sheet.sortColumnsByRowValues("row1", Comparator.naturalOrder());
        });
    }

    @Test
    public void testSortByColumnOnFrozenSheet() {
        sheet.freeze();
        assertThrows(IllegalStateException.class, () -> {
            sheet.sortRowsByColumnValues("col1", Comparator.naturalOrder());
        });
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
    public void testSortOnAZeroColumnOrSingleRowSheetIsANoOp() {
        final Sheet<String, String, Integer> noColumns = new Sheet<>(Arrays.asList("r2", "r1"), Collections.<String> emptyList());
        assertDoesNotThrow(() -> noColumns.sortByRowKey());
        assertEquals(Arrays.asList("r1", "r2"), new ArrayList<>(noColumns.rowKeySet()));

        final Sheet<String, String, Integer> single = Sheet.rows(Arrays.asList("r1"), Arrays.asList("c1"), new Integer[][] { { 1 } });
        assertDoesNotThrow(() -> single.sortByRowKey());
        assertEquals(Integer.valueOf(1), single.get("r1", "c1"));
    }

    @Test
    public void testSort_EmptyKeysOnEmptyAxis_Allowed() {
        // carve-out: empty sort keys accepted (no-op) on the corresponding empty axis.
        Sheet<String, String, Integer> zeroCol = new Sheet<>(Arrays.asList("r1", "r2"), Collections.<String> emptyList());
        assertDoesNotThrow(() -> zeroCol.sortRowsByColumnValues(Collections.<String> emptyList(), (Object[] a, Object[] b) -> 0));

        Sheet<String, String, Integer> zeroRow = new Sheet<>(Collections.<String> emptyList(), Arrays.asList("c1", "c2"));
        assertDoesNotThrow(() -> zeroRow.sortColumnsByRowValues(Collections.<String> emptyList(), (Object[] a, Object[] b) -> 0));
    }

    @Test
    public void testSortByRows() {
        Sheet<String, String, Integer> s = Sheet.rows(Arrays.asList("row1", "row2", "row3"), Arrays.asList("col1", "col2", "col3"),
                new Integer[][] { { 3, 1, 2 }, { 6, 4, 5 }, { 9, 7, 8 } });

        s.sortColumnsByRowValues(Arrays.asList("row1", "row2"), Comparator.comparing((Object[] arr) -> (Integer) arr[0]));

        assertNotNull(s.get("row1", "col1"));
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
    public void testSortByColumns() {
        Sheet<String, String, Integer> s = Sheet.rows(Arrays.asList("row1", "row2", "row3"), Arrays.asList("col1", "col2", "col3"),
                new Integer[][] { { 3, 2, 1 }, { 6, 5, 4 }, { 9, 8, 7 } });

        s.sortRowsByColumnValues(Arrays.asList("col1", "col2"), Comparator.comparing((Object[] arr) -> (Integer) arr[0]));

        assertNotNull(s.get("row1", "col1"));
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
    public void testSortRowsByMultipleColumnsRejectsNullComparator() {
        final Sheet<String, String, Integer> s = Sheet.rows(Arrays.asList("r1", "r2", "r3"), Arrays.asList("c1", "c2", "data"),
                new Integer[][] { { 1, 2, 10 }, { 1, 1, 20 }, { null, 9, 30 } });

        assertThrows(IllegalArgumentException.class, () -> s.sortRowsByColumnValues(Arrays.asList("c1", "c2"), null));
    }

    @Test
    public void testSortColumnsByMultipleRowsRejectsNullComparator() {
        final Sheet<String, String, Integer> s = Sheet.rows(Arrays.asList("r1", "r2", "data"), Arrays.asList("c1", "c2", "c3"),
                new Integer[][] { { 1, 1, null }, { 2, 1, 9 }, { 10, 20, 30 } });

        assertThrows(IllegalArgumentException.class, () -> s.sortColumnsByRowValues(Arrays.asList("r1", "r2"), null));
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
    public void testSortByRowKey_UninitializedSheet() {
        Sheet<String, String, Integer> uninitSheet = new Sheet<>(Arrays.asList("C", "A", "B"), columnKeys);
        uninitSheet.sortByRowKey();
        assertEquals(Arrays.asList("A", "B", "C"), new ArrayList<>(uninitSheet.rowKeySet()));
    }

    @Test
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
    public void testSortByRowKeyOnFrozenSheet() {
        sheet.freeze();
        assertThrows(IllegalStateException.class, () -> {
            sheet.sortByRowKey();
        });
    }

    @Test
    public void testSortByRowKeyAppliesMultiCyclePermutationsCorrectly() {
        final Sheet<Integer, String, String> s = Sheet.rows(Arrays.asList(5, 3, 1, 0, 4, 2), Arrays.asList("c1", "c2"),
                new String[][] { { "f1", "f2" }, { "d1", "d2" }, { "b1", "b2" }, { "a1", "a2" }, { "e1", "e2" }, { "c1v", "c2v" } });

        s.sortByRowKey();

        assertEquals(Arrays.asList(0, 1, 2, 3, 4, 5), new ArrayList<>(s.rowKeySet()));
        // Both columns must have moved with their rows.
        assertEquals(Arrays.asList("a1", "b1", "c1v", "d1", "e1", "f1"), new ArrayList<>(s.columnValues("c1")));
        assertEquals(Arrays.asList("a2", "b2", "c2v", "d2", "e2", "f2"), new ArrayList<>(s.columnValues("c2")));
    }

    @Test
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
    public void testSortRowsByColumnValues_SingleKey_New() {
        Sheet<String, String, Integer> s = Sheet.rows(Arrays.asList("b", "a", "c"), Arrays.asList("x", "y"),
                new Integer[][] { { 2, 20 }, { 1, 10 }, { 3, 30 } });
        s.sortRowsByColumnValues("x", Comparator.naturalOrder());
        List<String> rowKeys = new ArrayList<>(s.rowKeySet());
        assertEquals("a", rowKeys.get(0));
        assertEquals("b", rowKeys.get(1));
        assertEquals("c", rowKeys.get(2));
    }

    @Test
    public void testSortRowsByColumnValues_MultipleKeys_New() {
        Sheet<String, String, Integer> s = Sheet.rows(Arrays.asList("b", "a", "c"), Arrays.asList("x", "y"),
                new Integer[][] { { 2, 20 }, { 1, 10 }, { 3, 30 } });
        s.sortRowsByColumnValues(Arrays.asList("x"), (Object[] row1, Object[] row2) -> ((Integer) row1[0]).compareTo((Integer) row2[0]));
        List<String> rowKeys = new ArrayList<>(s.rowKeySet());
        assertEquals("a", rowKeys.get(0));
    }

    @Test
    public void testSortRowsByColumnValues_InvalidColumnKey() {
        assertThrows(IllegalArgumentException.class, () -> sheet.sortRowsByColumnValues("invalidCol", Comparator.naturalOrder()));
    }

    @Test
    public void testSortRowsByColumnValues_UninitializedSheet_SingleKey() {
        Sheet<String, String, Integer> s = new Sheet<>(Arrays.asList("r1", "r2"), Arrays.asList("c1", "c2"));
        assertDoesNotThrow(() -> s.sortRowsByColumnValues("c1", Comparator.naturalOrder()));
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class, () -> s.sortRowsByColumnValues("c1", (Comparator<Integer>) null));
    }

    @Test
    public void testSortRowsByColumnValues_UninitializedSheet_MultipleKeys() {
        Sheet<String, String, Integer> s = new Sheet<>(Arrays.asList("r1", "r2"), Arrays.asList("c1", "c2"));
        assertDoesNotThrow(() -> s.sortRowsByColumnValues(Arrays.asList("c1", "c2"), (Object[] a, Object[] b) -> 0));
    }

    @Test
    public void testSortRowsByColumnValues_RejectsNullComparator() {
        Sheet<String, String, Integer> s = Sheet.rows(Arrays.asList("b", "a"), Arrays.asList("col"), new Integer[][] { { 2 }, { 1 } });
        assertThrows(IllegalArgumentException.class, () -> s.sortRowsByColumnValues("col", (Comparator<Integer>) null));
    }

    @Test
    public void testSortRowsByColumnValues_NullCollection_ThrowsIAE() {
        assertThrows(IllegalArgumentException.class, () -> sheet.sortRowsByColumnValues((List<String>) null, (Object[] a, Object[] b) -> 0));
    }

    @Test
    public void testSortRowsByColumnValues_EmptyOnPopulatedSheet_ThrowsIAE() {
        assertThrows(IllegalArgumentException.class, () -> sheet.sortRowsByColumnValues(Collections.<String> emptyList(), (Object[] a, Object[] b) -> 0));
    }

    @Test
    public void testSortRowsByColumnValuesMovesEveryColumnWithItsRow() {
        final Sheet<String, String, Integer> s = Sheet.rows(Arrays.asList("r1", "r2", "r3", "r4"), Arrays.asList("key", "other"),
                new Integer[][] { { 3, 30 }, { 1, 10 }, { 4, 40 }, { 2, 20 } });

        s.sortRowsByColumnValues("key", Comparator.<Integer> naturalOrder());

        assertEquals(Arrays.asList("r2", "r4", "r1", "r3"), new ArrayList<>(s.rowKeySet()));
        assertEquals(Arrays.asList(1, 2, 3, 4), new ArrayList<>(s.columnValues("key")));
        assertEquals(Arrays.asList(10, 20, 30, 40), new ArrayList<>(s.columnValues("other")));
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
    public void testSortByColumnKey_DataIntegrity() {
        Sheet<String, String, Integer> s = Sheet.rows(Arrays.asList("r1", "r2"), Arrays.asList("C", "A", "B"), new Integer[][] { { 3, 1, 2 }, { 6, 4, 5 } });
        s.sortByColumnKey();
        assertEquals(Arrays.asList("A", "B", "C"), new ArrayList<>(s.columnKeySet()));
        assertEquals(Integer.valueOf(1), s.get("r1", "A"));
        assertEquals(Integer.valueOf(2), s.get("r1", "B"));
        assertEquals(Integer.valueOf(3), s.get("r1", "C"));
    }

    @Test
    public void testSortByColumnKey_UninitializedSheet() {
        Sheet<String, String, Integer> uninitSheet = new Sheet<>(rowKeys, Arrays.asList("C", "A", "B"));
        uninitSheet.sortByColumnKey();
        assertEquals(Arrays.asList("A", "B", "C"), new ArrayList<>(uninitSheet.columnKeySet()));
    }

    @Test
    public void testSortByColumnKeyOnFrozenSheet() {
        sheet.freeze();
        assertThrows(IllegalStateException.class, () -> {
            sheet.sortByColumnKey();
        });
    }

    @Test
    public void testSortByColumnKeyAppliesMultiCyclePermutationsCorrectly() {
        final Sheet<String, Integer, String> s = Sheet.rows(Arrays.asList("r1", "r2"), Arrays.asList(5, 3, 1, 0, 4, 2),
                new String[][] { { "f", "d", "b", "a", "e", "c" }, { "F", "D", "B", "A", "E", "C" } });

        s.sortByColumnKey();

        assertEquals(Arrays.asList(0, 1, 2, 3, 4, 5), new ArrayList<>(s.columnKeySet()));
        assertEquals(Arrays.asList("a", "b", "c", "d", "e", "f"), new ArrayList<>(s.rowValues("r1")));
        assertEquals(Arrays.asList("A", "B", "C", "D", "E", "F"), new ArrayList<>(s.rowValues("r2")));
    }

    @Test
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
    public void testSortColumnsByRowValues_RejectsNullComparator() {
        Sheet<String, String, Integer> s = Sheet.rows(Arrays.asList("row"), Arrays.asList("b", "a"), new Integer[][] { { 2, 1 } });
        assertThrows(IllegalArgumentException.class, () -> s.sortColumnsByRowValues("row", (Comparator<Integer>) null));
    }

    @Test
    public void testSortColumnsByRowValues_InvalidRowKey() {
        assertThrows(IllegalArgumentException.class, () -> sheet.sortColumnsByRowValues("invalidRow", Comparator.naturalOrder()));
    }

    @Test
    public void testSortColumnsByRowValues_UninitializedSheet_SingleKey() {
        Sheet<String, String, Integer> s = new Sheet<>(Arrays.asList("r1", "r2"), Arrays.asList("c1", "c2"));
        assertDoesNotThrow(() -> s.sortColumnsByRowValues("r1", Comparator.naturalOrder()));
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class, () -> s.sortColumnsByRowValues("r1", (Comparator<Integer>) null));
    }

    @Test
    public void testSortColumnsByRowValues_UninitializedSheet_MultipleKeys() {
        Sheet<String, String, Integer> s = new Sheet<>(Arrays.asList("r1", "r2"), Arrays.asList("c1", "c2"));
        assertDoesNotThrow(() -> s.sortColumnsByRowValues(Arrays.asList("r1", "r2"), (Object[] a, Object[] b) -> 0));
    }

    @Test
    public void testSortColumnsByRowValues_NullCollection_ThrowsIAE() {
        assertThrows(IllegalArgumentException.class, () -> sheet.sortColumnsByRowValues((List<String>) null, (Object[] a, Object[] b) -> 0));
    }

    @Test
    public void testSortColumnsByRowValues_EmptyOnPopulatedSheet_ThrowsIAE() {
        assertThrows(IllegalArgumentException.class, () -> sheet.sortColumnsByRowValues(Collections.<String> emptyList(), (Object[] a, Object[] b) -> 0));
    }

    @Test
    public void testSortColumnsByRowValuesMovesEveryRowWithItsColumn() {
        final Sheet<String, String, Integer> s = Sheet.rows(Arrays.asList("key", "other"), Arrays.asList("c1", "c2", "c3", "c4"),
                new Integer[][] { { 3, 1, 4, 2 }, { 30, 10, 40, 20 } });

        s.sortColumnsByRowValues("key", Comparator.<Integer> naturalOrder());

        assertEquals(Arrays.asList("c2", "c4", "c1", "c3"), new ArrayList<>(s.columnKeySet()));
        assertEquals(Arrays.asList(1, 2, 3, 4), new ArrayList<>(s.rowValues("key")));
        assertEquals(Arrays.asList(10, 20, 30, 40), new ArrayList<>(s.rowValues("other")));
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
    public void testSortByColumn() {
        Sheet<String, String, Integer> s = Sheet.rows(Arrays.asList("row1", "row2", "row3"), Arrays.asList("col1", "col2", "col3"),
                new Integer[][] { { 3, 2, 1 }, { 6, 5, 4 }, { 9, 8, 7 } });

        s.sortRowsByColumnValues("col1", Comparator.naturalOrder());

        assertEquals(Integer.valueOf(3), s.get("row1", "col1"));
    }

    @Test
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

}

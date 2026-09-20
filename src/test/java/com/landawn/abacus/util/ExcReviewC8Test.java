package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

/**
 * Group C8 (CommonUtil.java 36180-41309): validation-order changes (rule 3) — the range is validated before the
 * comparator, so a bad range combined with a null comparator now gets IndexOutOfBoundsException (was IAE).
 */
public class ExcReviewC8Test extends TestBase {

    // --- parallelSort(T[], int, int, Comparator): range validated before cmp -------------------------------------

    @Test
    public void parallelSort_array_badRange_and_nullCmp_isIOOBE() {
        final Integer[] a = { 3, 1, 2 };
        assertThrows(IndexOutOfBoundsException.class, () -> CommonUtil.parallelSort(a, 0, 4, (Comparator<Integer>) null));
        assertThrows(IndexOutOfBoundsException.class, () -> CommonUtil.parallelSort(a, -1, 3, (Comparator<Integer>) null));
        assertThrows(IndexOutOfBoundsException.class, () -> CommonUtil.parallelSort((Integer[]) null, 0, 1, (Comparator<Integer>) null));
    }

    @Test
    public void parallelSort_array_validRange_and_nullCmp_stillIAE() {
        final Integer[] a = { 3, 1, 2 };
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.parallelSort(a, 0, 3, (Comparator<Integer>) null));
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.parallelSort(a, (Comparator<Integer>) null));
        CommonUtil.parallelSort(a, 0, 3, Comparator.naturalOrder());
        assertEquals(Arrays.asList(1, 2, 3), Arrays.asList(a));
    }

    // --- parallelSort(List, int, int, Comparator) ----------------------------------------------------------------

    @Test
    public void parallelSort_list_badRange_and_nullCmp_isIOOBE() {
        final List<Integer> list = new ArrayList<>(Arrays.asList(3, 1, 2));
        assertThrows(IndexOutOfBoundsException.class, () -> CommonUtil.parallelSort(list, 0, 4, (Comparator<Integer>) null));
        assertThrows(IndexOutOfBoundsException.class, () -> CommonUtil.parallelSort(list, 2, 1, (Comparator<Integer>) null));
        assertThrows(IndexOutOfBoundsException.class, () -> CommonUtil.parallelSort((List<Integer>) null, 0, 1, (Comparator<Integer>) null));
    }

    @Test
    public void parallelSort_list_validRange_and_nullCmp_stillIAE() {
        final List<Integer> list = new ArrayList<>(Arrays.asList(3, 1, 2));
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.parallelSort(list, 0, 3, (Comparator<Integer>) null));
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.parallelSort(list, (Comparator<Integer>) null));
        CommonUtil.parallelSort(list, 0, 3, Comparator.naturalOrder());
        assertEquals(Arrays.asList(1, 2, 3), list);
    }

    // --- binarySearch(T[], int, int, T, Comparator) --------------------------------------------------------------

    @Test
    public void binarySearch_array_badRange_and_nullCmp_isIOOBE() {
        final Integer[] a = { 1, 2, 3 };
        assertThrows(IndexOutOfBoundsException.class, () -> CommonUtil.binarySearch(a, 0, 4, 2, (Comparator<Integer>) null));
        assertThrows(IndexOutOfBoundsException.class, () -> CommonUtil.binarySearch(a, -1, 3, 2, (Comparator<Integer>) null));
        assertThrows(IndexOutOfBoundsException.class, () -> CommonUtil.binarySearch((Integer[]) null, 0, 1, 2, (Comparator<Integer>) null));
    }

    @Test
    public void binarySearch_array_validRange_and_nullCmp_stillIAE() {
        final Integer[] a = { 1, 2, 3 };
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.binarySearch(a, 0, 3, 2, (Comparator<Integer>) null));
        assertEquals(1, CommonUtil.binarySearch(a, 0, 3, 2, Comparator.naturalOrder()));
    }

    // --- binarySearch(List, int, int, T, Comparator) -------------------------------------------------------------

    @Test
    public void binarySearch_list_badRange_and_nullCmp_isIOOBE() {
        final List<Integer> list = Arrays.asList(1, 2, 3);
        assertThrows(IndexOutOfBoundsException.class, () -> CommonUtil.binarySearch(list, 0, 4, 2, (Comparator<Integer>) null));
        assertThrows(IndexOutOfBoundsException.class, () -> CommonUtil.binarySearch(list, 2, 1, 2, (Comparator<Integer>) null));
        assertThrows(IndexOutOfBoundsException.class, () -> CommonUtil.binarySearch((List<Integer>) null, 0, 1, 2, (Comparator<Integer>) null));
    }

    @Test
    public void binarySearch_list_validRange_and_nullCmp_stillIAE() {
        final List<Integer> list = Arrays.asList(1, 2, 3);
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.binarySearch(list, 0, 3, 2, (Comparator<Integer>) null));
        assertEquals(1, CommonUtil.binarySearch(list, 0, 3, 2, Comparator.naturalOrder()));
    }

    // --- binarySearch(List, T) / (List, int, int, T): newly DECLARED ClassCastException (already thrown before) ---

    @Test
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public void binarySearch_list_natural_incomparableElements_isCCE() {
        final List rawList = new ArrayList(Arrays.asList(1, "two", 3));
        assertThrows(ClassCastException.class, () -> CommonUtil.binarySearch((List<Comparable>) rawList, (Comparable) "x"));
        assertThrows(ClassCastException.class, () -> CommonUtil.binarySearch((List<Comparable>) rawList, 0, 3, (Comparable) "x"));
    }

    // --- indicesOfMin(T[], Comparator): newly DECLARED ClassCastException (already thrown before) -----------------

    @Test
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public void indicesOfMin_array_cmpThrowsCCE_propagates() {
        final Object[] a = { 1, "two", 3 };
        final Comparator rawNatural = Comparator.naturalOrder();
        assertThrows(ClassCastException.class, () -> CommonUtil.indicesOfMin(a, (Comparator<Object>) rawNatural));
    }
}

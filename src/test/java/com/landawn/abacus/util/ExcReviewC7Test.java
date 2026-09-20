package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

import org.junit.jupiter.api.Test;

public class ExcReviewC7Test extends com.landawn.abacus.TestBase {

    // ---- copyOfRange(..., step): range is validated before step (rule 3b) ----
    // invalid range + step == 0 -> IOOBE (baseline: IAE)

    @Test
    public void copyOfRange_step_invalidRange_beforeStep() {
        assertThrows(IndexOutOfBoundsException.class, () -> N.copyOfRange(new boolean[] { true, false }, -1, 2, 0));
        assertThrows(IndexOutOfBoundsException.class, () -> N.copyOfRange(new char[] { 'a', 'b' }, -1, 2, 0));
        assertThrows(IndexOutOfBoundsException.class, () -> N.copyOfRange(new byte[] { 1, 2 }, -1, 2, 0));
        assertThrows(IndexOutOfBoundsException.class, () -> N.copyOfRange(new short[] { 1, 2 }, -1, 2, 0));
        assertThrows(IndexOutOfBoundsException.class, () -> N.copyOfRange(new int[] { 1, 2 }, -1, 2, 0));
        assertThrows(IndexOutOfBoundsException.class, () -> N.copyOfRange(new long[] { 1L, 2L }, -1, 2, 0));
        assertThrows(IndexOutOfBoundsException.class, () -> N.copyOfRange(new float[] { 1f, 2f }, -1, 2, 0));
        assertThrows(IndexOutOfBoundsException.class, () -> N.copyOfRange(new double[] { 1d, 2d }, -1, 2, 0));
        assertThrows(IndexOutOfBoundsException.class, () -> N.copyOfRange(new String[] { "a", "b" }, 0, 3, 0));
        assertThrows(IndexOutOfBoundsException.class, () -> N.copyOfRange(new String[] { "a", "b" }, 0, 3, 0, Object[].class));
        assertThrows(IndexOutOfBoundsException.class, () -> N.copyOfRange(Arrays.asList(1, 2), 0, 3, 0));
        assertThrows(IndexOutOfBoundsException.class, () -> N.copyOfRange("ab", 0, 3, 0));
    }

    // valid range + step == 0 -> still IAE (unchanged)
    @Test
    public void copyOfRange_step_zero_validRange_stillIAE() {
        assertThrows(IllegalArgumentException.class, () -> N.copyOfRange(new int[] { 1, 2 }, 0, 2, 0));
        assertThrows(IllegalArgumentException.class, () -> N.copyOfRange(new String[] { "a", "b" }, 0, 2, 0, Object[].class));
        assertThrows(IllegalArgumentException.class, () -> N.copyOfRange(Arrays.asList(1, 2), 0, 2, 0));
        assertThrows(IllegalArgumentException.class, () -> N.copyOfRange("ab", 0, 2, 0));
        // null original still rejected first
        assertThrows(IllegalArgumentException.class, () -> N.copyOfRange((int[]) null, -1, 2, 0));
        // newType null is checked after range and step
        assertThrows(IndexOutOfBoundsException.class, () -> N.copyOfRange(new String[] { "a" }, 0, 3, 1, (Class<Object[]>) null));
        assertThrows(IllegalArgumentException.class, () -> N.copyOfRange(new String[] { "a" }, 0, 1, 1, (Class<Object[]>) null));
    }

    // ---- (a, fromIndex, toIndex, cmp): range validated before cmp (rule 3b) ----
    // invalid range + null cmp -> IOOBE (baseline: IAE)

    @Test
    public void rangeBeforeCmp_invalidRange_nullCmp() {
        final Integer[] a = { 1, 2, 3 };
        final List<Integer> list = new ArrayList<>(Arrays.asList(1, 2, 3));
        final List<Integer> linked = new LinkedList<>(Arrays.asList(1, 2, 3));
        assertThrows(IndexOutOfBoundsException.class, () -> N.isSorted(a, 0, 5, (Comparator<Integer>) null));
        assertThrows(IndexOutOfBoundsException.class, () -> N.isSorted(list, 0, 5, (Comparator<Integer>) null));
        assertThrows(IndexOutOfBoundsException.class, () -> N.sort(a, 0, 5, (Comparator<Integer>) null));
        assertThrows(IndexOutOfBoundsException.class, () -> N.sort(list, 0, 5, (Comparator<Integer>) null));
        assertThrows(IndexOutOfBoundsException.class, () -> N.sort(linked, 2, 1, (Comparator<Integer>) null));
    }

    // valid range + null cmp -> still IAE (unchanged)
    @Test
    public void rangeBeforeCmp_validRange_nullCmp_stillIAE() {
        final Integer[] a = { 1, 2, 3 };
        final List<Integer> list = new ArrayList<>(Arrays.asList(1, 2, 3));
        assertThrows(IllegalArgumentException.class, () -> N.isSorted(a, 0, 3, (Comparator<Integer>) null));
        assertThrows(IllegalArgumentException.class, () -> N.isSorted(list, 0, 3, (Comparator<Integer>) null));
        assertThrows(IllegalArgumentException.class, () -> N.sort(a, 0, 3, (Comparator<Integer>) null));
        assertThrows(IllegalArgumentException.class, () -> N.sort(list, 0, 3, (Comparator<Integer>) null));
        // null array/list with an empty range is still rejected on cmp only
        assertThrows(IllegalArgumentException.class, () -> N.sort((Integer[]) null, 0, 0, (Comparator<Integer>) null));
    }
}

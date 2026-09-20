package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * Group N6 (N.java 26196-31406): validation-order changes. Every assertion below pins the message an invalid-input
 * COMBINATION gets after the rule-3 reorder (an earlier parameter is now reported before a later one). The exception
 * class is IllegalArgumentException both before and after; only the message (i.e. which check fires first) changed.
 */
public class ExcReviewN6Test extends com.landawn.abacus.TestBase {

    private static final Comparator<Integer> CMP = Comparator.naturalOrder();

    private static String msg(final org.junit.jupiter.api.function.Executable e) {
        return assertThrows(IllegalArgumentException.class, e).getMessage();
    }

    // ---- lowerMedian(Collection, int, int, Comparator): empty range (c/from/to) reported before cmp

    @Test
    public void lowerMedian_range_emptyRangeReportedBeforeNullCmp() {
        assertEquals("The specified collection or range cannot be empty", msg(() -> N.lowerMedian(Arrays.asList(1, 2), 1, 1, null)));
        assertEquals("The specified collection or range cannot be empty", msg(() -> N.lowerMedian(N.<Integer> emptyList(), 0, 0, null)));
        // cmp alone still reported
        assertEquals("'cmp' cannot be null", msg(() -> N.lowerMedian(Arrays.asList(1, 2), 0, 2, null)));
        assertEquals(Integer.valueOf(1), N.lowerMedian(Arrays.asList(1, 2), 0, 2, CMP));
    }

    // ---- kthLargest(T[], int, int, int, Comparator) / kthLargest(Collection, int, int, int, Comparator): k before cmp

    @Test
    public void kthLargest_arrayRange_kReportedBeforeNullCmp() {
        final Integer[] a = { 3, 1, 2 };
        assertTrue(msg(() -> N.kthLargest(a, 0, 3, 5, null)).startsWith("'k' (5) is out of range [1, 3]"));
        assertEquals("The specified array or range cannot be empty", msg(() -> N.kthLargest(a, 1, 1, 1, null)));
        assertEquals("'cmp' cannot be null", msg(() -> N.kthLargest(a, 0, 3, 2, null)));
        assertEquals(Integer.valueOf(2), N.kthLargest(a, 0, 3, 2, CMP));
    }

    @Test
    public void kthLargest_collectionRange_kReportedBeforeNullCmp() {
        final List<Integer> c = Arrays.asList(3, 1, 2);
        assertTrue(msg(() -> N.kthLargest(c, 0, 3, 5, null)).startsWith("'k' (5) is out of range [1, 3]"));
        assertEquals("The specified collection or range cannot be empty", msg(() -> N.kthLargest(c, 1, 1, 1, null)));
        assertEquals("'cmp' cannot be null", msg(() -> N.kthLargest(c, 0, 3, 2, null)));
        assertEquals(Integer.valueOf(2), N.kthLargest(c, 0, 3, 2, CMP));
    }

    // ---- kthLargest(T[], int, Comparator) / kthLargest(Collection, int, Comparator): k (param 2) before cmp (param 3)

    @Test
    public void kthLargest_whole_kReportedBeforeNullCmp() {
        final Integer[] a = { 3, 1, 2 };
        final List<Integer> c = Arrays.asList(3, 1, 2);
        assertTrue(msg(() -> N.kthLargest(a, 0, null)).startsWith("'k' (0) is out of range [1, 3]"));
        assertTrue(msg(() -> N.kthLargest(c, 0, null)).startsWith("'k' (0) is out of range [1, 3]"));
        assertEquals("'cmp' cannot be null", msg(() -> N.kthLargest(a, 2, null)));
        assertEquals("'cmp' cannot be null", msg(() -> N.kthLargest(c, 2, null)));
        assertEquals("The specified array cannot be null or empty", msg(() -> N.kthLargest((Integer[]) null, 0, null)));
        assertEquals("The specified collection cannot be null or empty", msg(() -> N.kthLargest((List<Integer>) null, 0, null)));
        assertEquals(Integer.valueOf(2), N.kthLargest(a, 2, CMP));
        assertEquals(Integer.valueOf(2), N.kthLargest(c, 2, CMP));
    }

    // ---- top(*, n, cmp[, keepEncounterOrder]) delegating overloads: n (param 2) before cmp (param 3)

    @Test
    public void top_negativeNReportedBeforeNullCmp() {
        final String N_NEG = "'n' cannot be negative: -1";
        assertEquals(N_NEG, msg(() -> N.top(new short[] { 1 }, -1, null)));
        assertEquals(N_NEG, msg(() -> N.top(new int[] { 1 }, -1, null)));
        assertEquals(N_NEG, msg(() -> N.top(new long[] { 1 }, -1, null)));
        assertEquals(N_NEG, msg(() -> N.top(new float[] { 1 }, -1, null)));
        assertEquals(N_NEG, msg(() -> N.top(new double[] { 1 }, -1, null)));
        assertEquals(N_NEG, msg(() -> N.top(new Integer[] { 1 }, -1, null)));
        assertEquals(N_NEG, msg(() -> N.top(Arrays.asList(1), -1, null)));
        assertEquals(N_NEG, msg(() -> N.top(new Integer[] { 1 }, -1, null, true)));
        assertEquals(N_NEG, msg(() -> N.top(Arrays.asList(1), -1, null, true)));
        // null cmp alone is still rejected (now by the delegate), also for a null/empty array
        assertEquals("'cmp' cannot be null", msg(() -> N.top(new int[] { 1 }, 1, null)));
        assertEquals("'cmp' cannot be null", msg(() -> N.top((int[]) null, 0, null)));
        assertEquals("'cmp' cannot be null", msg(() -> N.top(new Integer[] { 1 }, 1, null, false)));
        assertEquals("'cmp' cannot be null", msg(() -> N.top(Arrays.asList(1), 1, null, true)));
        assertEquals(0, N.top(new int[] { 1 }, 0, Comparator.naturalOrder()).length);
        assertEquals(Arrays.asList(3), N.top(Arrays.asList(1, 3, 2), 1, CMP, true));
    }
}

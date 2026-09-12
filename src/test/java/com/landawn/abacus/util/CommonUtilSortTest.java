package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.junit.jupiter.api.Test;

public class CommonUtilSortTest extends CommonUtilTestSupport {

    @Test
    public void testSort_arrays() {
        CommonUtil.sort((boolean[]) null);
        CommonUtil.sort(new boolean[0]);
        boolean[] bools = { true, false, true, false, false };
        CommonUtil.sort(bools);
        assertArrayEquals(new boolean[] { false, false, false, true, true }, bools);
        boolean[] already = { false, false, true, true };
        CommonUtil.sort(already);
        assertArrayEquals(new boolean[] { false, false, true, true }, already);

        CommonUtil.sort((char[]) null);
        CommonUtil.sort(new char[0]);
        char[] chars = { 'd', 'b', 'a', 'c' };
        CommonUtil.sort(chars);
        assertArrayEquals(new char[] { 'a', 'b', 'c', 'd' }, chars);

        CommonUtil.sort((byte[]) null);
        byte[] bytes = { 4, 2, 1, 3 };
        CommonUtil.sort(bytes);
        assertArrayEquals(new byte[] { 1, 2, 3, 4 }, bytes);

        CommonUtil.sort((short[]) null);
        short[] shorts = { 4, 2, 1, 3 };
        CommonUtil.sort(shorts);
        assertArrayEquals(new short[] { 1, 2, 3, 4 }, shorts);

        CommonUtil.sort((int[]) null);
        CommonUtil.sort(new int[0]);
        int[] ints = { 3, 1, 4, 1, 5 };
        CommonUtil.sort(ints);
        assertTrue(CommonUtil.isSorted(ints));
        assertTrue(CommonUtil.equals(ints, new int[] { 1, 1, 3, 4, 5 }));

        CommonUtil.sort((long[]) null);
        long[] longs = { 4L, 2L, 1L, 3L };
        CommonUtil.sort(longs);
        assertArrayEquals(new long[] { 1L, 2L, 3L, 4L }, longs);

        CommonUtil.sort((float[]) null);
        float[] floats = { 4.0f, 2.0f, 1.0f, 3.0f };
        CommonUtil.sort(floats);
        assertArrayEquals(new float[] { 1.0f, 2.0f, 3.0f, 4.0f }, floats);

        CommonUtil.sort((double[]) null);
        double[] doubles = { 4.0, 2.0, 1.0, 3.0 };
        CommonUtil.sort(doubles);
        assertArrayEquals(new double[] { 1.0, 2.0, 3.0, 4.0 }, doubles);

        CommonUtil.sort((Object[]) null);
        CommonUtil.sort(new Object[0]);
        String[] strings = { "c", "a", "b" };
        CommonUtil.sort(strings);
        assertTrue(CommonUtil.equals(strings, new String[] { "a", "b", "c" }));
        String[] withNulls = { "b", null, "a", null };
        CommonUtil.sort(withNulls);
        assertArrayEquals(new String[] { null, null, "a", "b" }, withNulls);

        String[] byLength = { "aaa", "b", "cc" };
        CommonUtil.sort(byLength, Comparator.comparing(String::length));
        assertArrayEquals(new String[] { "b", "cc", "aaa" }, byLength);
    }

    @Test
    public void testSort_range() {
        boolean[] bools = { true, true, false, true, false };
        CommonUtil.sort(bools, 1, 4);
        assertArrayEquals(new boolean[] { true, false, true, true, false }, bools);
        boolean[] emptyRange = { true, false };
        CommonUtil.sort(emptyRange, 1, 1);
        assertArrayEquals(new boolean[] { true, false }, emptyRange);
        assertThrows(IndexOutOfBoundsException.class, () -> CommonUtil.sort(bools, -1, 2));

        char[] chars = { 'd', 'c', 'b', 'a', 'e' };
        CommonUtil.sort(chars, 1, 4);
        assertArrayEquals(new char[] { 'd', 'a', 'b', 'c', 'e' }, chars);

        byte[] bytes = { 5, 3, 2, 1, 4 };
        CommonUtil.sort(bytes, 1, 4);
        assertArrayEquals(new byte[] { 5, 1, 2, 3, 4 }, bytes);

        short[] shorts = { 5, 3, 2, 1, 4 };
        CommonUtil.sort(shorts, 1, 4);
        assertArrayEquals(new short[] { 5, 1, 2, 3, 4 }, shorts);

        int[] ints = { 5, 3, 2, 1, 4 };
        CommonUtil.sort(ints, 1, 4);
        assertArrayEquals(new int[] { 5, 1, 2, 3, 4 }, ints);

        long[] longs = { 5L, 3L, 2L, 1L, 4L };
        CommonUtil.sort(longs, 1, 4);
        assertArrayEquals(new long[] { 5L, 1L, 2L, 3L, 4L }, longs);

        float[] floats = { 5.0f, 3.0f, 2.0f, 1.0f, 4.0f };
        CommonUtil.sort(floats, 1, 4);
        assertArrayEquals(new float[] { 5.0f, 1.0f, 2.0f, 3.0f, 4.0f }, floats);

        double[] doubles = { 5.0, 3.0, 2.0, 1.0, 4.0 };
        CommonUtil.sort(doubles, 1, 4);
        assertArrayEquals(new double[] { 5.0, 1.0, 2.0, 3.0, 4.0 }, doubles);

        String[] strings = { "e", "d", "c", "b", "a" };
        CommonUtil.sort(strings, 1, 4);
        assertArrayEquals(new String[] { "e", "b", "c", "d", "a" }, strings);

        String[] byLength = { "e", "aaa", "b", "cc", "d" };
        CommonUtil.sort(byLength, 1, 4, Comparator.comparing(String::length));
        assertArrayEquals(new String[] { "e", "b", "cc", "aaa", "d" }, byLength);
    }

    @Test
    public void testSort_listAndBy() {
        CommonUtil.sort((List<String>) null);
        CommonUtil.sort(new ArrayList<String>());
        List<String> list = new ArrayList<>(Arrays.asList("c", "a", "b"));
        CommonUtil.sort(list);
        assertEquals(Arrays.asList("a", "b", "c"), list);

        List<String> ranged = new ArrayList<>(Arrays.asList("e", "d", "c", "b", "a"));
        CommonUtil.sort(ranged, 1, 4);
        assertEquals(Arrays.asList("e", "b", "c", "d", "a"), ranged);

        List<String> byCmp = new ArrayList<>(Arrays.asList("aaa", "b", "cc"));
        CommonUtil.sort(byCmp, Comparator.comparing(String::length));
        assertEquals(Arrays.asList("b", "cc", "aaa"), byCmp);

        List<String> rangedCmp = new ArrayList<>(Arrays.asList("d", "b", "a", "c", "e"));
        CommonUtil.sort(rangedCmp, 1, 4, Comparator.naturalOrder());
        assertEquals(Arrays.asList("d", "a", "b", "c", "e"), rangedCmp);

        List<Integer> full = new ArrayList<>(Arrays.asList(3, 1, 4, 1, 5));
        CommonUtil.sort(full, 0, full.size(), Comparator.naturalOrder());
        assertEquals(Arrays.asList(1, 1, 3, 4, 5), full);

        List<String> empty = new ArrayList<>();
        CommonUtil.sort(empty, 0, 0, Comparator.naturalOrder());
        assertTrue(empty.isEmpty());

        List<String> rejectNull = new ArrayList<>(Arrays.asList("d", "b", "a", "c", "e"));
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.sort(rejectNull, 1, 4, (Comparator<String>) null));
        assertEquals(Arrays.asList("d", "b", "a", "c", "e"), rejectNull);

        List<String> byLength = new ArrayList<>(Arrays.asList("e", "aaa", "b", "cc", "d"));
        CommonUtil.sort(byLength, 1, 4, Comparator.comparing(String::length));
        assertEquals(Arrays.asList("e", "b", "cc", "aaa", "d"), byLength);

        assertThrows(IndexOutOfBoundsException.class, () -> CommonUtil.sort(new ArrayList<Integer>(), 1, 3));
        assertThrows(IndexOutOfBoundsException.class, () -> CommonUtil.sort((List<Integer>) null, 1, 3));
        CommonUtil.sort(new ArrayList<Integer>(), 0, 0);
        List<Integer> validRange = new ArrayList<>(Arrays.asList(3, 1, 2));
        CommonUtil.sort(validRange, 0, 3);
        assertEquals(Arrays.asList(1, 2, 3), validRange);

        List<String> by = new ArrayList<>(Arrays.asList("apple", "pie", "a", "dog"));
        CommonUtil.sortBy(by, String::length);
        assertEquals("a", by.get(0));
        assertEquals(3, by.get(1).length());
        assertEquals("apple", by.get(3));

        List<String> byInt = new ArrayList<>(Arrays.asList("apple", "zoo", "a", "be"));
        CommonUtil.sortByInt(byInt, String::length);
        assertEquals("a", byInt.get(0));
        assertEquals(2, byInt.get(1).length());
        assertEquals("apple", byInt.get(3));

        List<String> byLong = new ArrayList<>(Arrays.asList("apple", "pie", "a"));
        CommonUtil.sortByLong(byLong, s -> (long) s.length());
        assertEquals(Arrays.asList("a", "pie", "apple"), byLong);

        List<String> byFloat = new ArrayList<>(Arrays.asList("apple", "pie", "a"));
        CommonUtil.sortByFloat(byFloat, s -> (float) s.length());
        assertEquals(Arrays.asList("a", "pie", "apple"), byFloat);

        List<String> byDouble = new ArrayList<>(Arrays.asList("apple", "pie", "a"));
        CommonUtil.sortByDouble(byDouble, s -> (double) s.length());
        assertEquals(Arrays.asList("a", "pie", "apple"), byDouble);
    }

    /**
     * A partial-range sort of a {@code CopyOnWriteArrayList} must not throw.
     *
     * <p>{@code CopyOnWriteArrayList} supports {@code set(int, E)} but not {@code listIterator().set(E)}, so the
     * write-back has to be index-based for a {@code RandomAccess} list. A full-range sort never reached the
     * write-back (it delegates to {@code list.sort}), which is why only the partial range used to fail.</p>
     */
    @Test
    public void testSort_PartialRangeOfCopyOnWriteArrayList() {
        final List<Integer> full = new CopyOnWriteArrayList<>(Arrays.asList(5, 4, 3, 2, 1));
        CommonUtil.sort(full, 0, 5, Comparator.<Integer> naturalOrder());
        assertEquals(Arrays.asList(1, 2, 3, 4, 5), new ArrayList<>(full));

        final List<Integer> partial = new CopyOnWriteArrayList<>(Arrays.asList(5, 4, 3, 2, 1));
        CommonUtil.sort(partial, 1, 4, Comparator.<Integer> naturalOrder());
        assertEquals(Arrays.asList(5, 2, 3, 4, 1), new ArrayList<>(partial));
    }

    /** The non-{@code RandomAccess} write-back branch must still sort a partial range correctly. */
    @Test
    public void testSort_PartialRangeOfLinkedList() {
        final List<Integer> list = new LinkedList<>(Arrays.asList(5, 4, 3, 2, 1));
        CommonUtil.sort(list, 1, 4, Comparator.<Integer> naturalOrder());
        assertEquals(Arrays.asList(5, 2, 3, 4, 1), new ArrayList<>(list));
    }

    /**
     * {@code parallelSort} of a {@code CopyOnWriteArrayList} must not depend on its size.
     *
     * <p>Above {@code DOUBLE_PIPE_SORT_OBJECT_THRESHOLD} (2000) on a multi-core machine the {@code list.sort}
     * fast path is skipped and the array write-back runs, so the same call used to succeed at 2000 elements and
     * throw {@code UnsupportedOperationException} at 2001.</p>
     */
    @Test
    public void testParallelSort_CopyOnWriteArrayListAboveThreshold() {
        for (final int n : new int[] { 2000, 2001, 5000 }) {
            final List<Integer> cow = descendingCopyOnWrite(n);

            CommonUtil.parallelSort(cow, Comparator.<Integer> naturalOrder());

            assertEquals(ascending(1, n), new ArrayList<>(cow));
        }
    }

    /**
     * A partial-range {@code parallelSort} always reaches the array write-back, whatever the core count.
     *
     * <p>The {@code list.sort(cmp)} fast path requires {@code fromIndex == 0 && toIndex == list.size()}, so unlike
     * the full-range case above this does not depend on {@code IOUtil.CPU_CORES} or on the 2000-element
     * threshold.</p>
     */
    @Test
    public void testParallelSort_PartialRangeOfCopyOnWriteArrayList() {
        final List<Integer> cow = descendingCopyOnWrite(10);

        CommonUtil.parallelSort(cow, 1, 9, Comparator.<Integer> naturalOrder());

        assertEquals(Arrays.asList(10, 2, 3, 4, 5, 6, 7, 8, 9, 1), new ArrayList<>(cow));
    }

    /**
     * The two overloads that delegate to {@code sort(List, int, int, Comparator)} inherit the fix.
     *
     * <p>{@code sort(List, int, int)} forwards with the natural comparator and {@code reverseSort(List, int, int)}
     * with the reversed one, so both used to throw {@code UnsupportedOperationException} on a partial range of a
     * copy-on-write list too.</p>
     */
    @Test
    public void testSortAndReverseSort_PartialRangeOfCopyOnWriteArrayList_DelegatingOverloads() {
        final List<Integer> natural = new CopyOnWriteArrayList<>(Arrays.asList(5, 4, 3, 2, 1));
        CommonUtil.sort(natural, 1, 4);
        assertEquals(Arrays.asList(5, 2, 3, 4, 1), new ArrayList<>(natural));

        final List<Integer> reversed = new CopyOnWriteArrayList<>(Arrays.asList(1, 2, 3, 4, 5));
        CommonUtil.reverseSort(reversed, 1, 4);
        assertEquals(Arrays.asList(1, 4, 3, 2, 5), new ArrayList<>(reversed));
    }

    private static List<Integer> descendingCopyOnWrite(final int n) {
        final List<Integer> cow = new CopyOnWriteArrayList<>();

        for (int i = n; i > 0; i--) {
            cow.add(i);
        }

        return cow;
    }

    private static List<Integer> ascending(final int fromInclusive, final int toInclusive) {
        final List<Integer> expected = new ArrayList<>();

        for (int i = fromInclusive; i <= toInclusive; i++) {
            expected.add(i);
        }

        return expected;
    }
}

package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.junit.jupiter.api.Test;

public class NRemoveTest extends NTestSupport {

    @Test
    public void testRemove() {
        assertArrayEquals(new boolean[] { false }, N.remove(new boolean[] { true, false }, true));
        assertArrayEquals(new boolean[0], N.remove(new boolean[0], true));
        assertArrayEquals(new boolean[0], N.remove((boolean[]) null, true));

        assertArrayEquals(new char[] { 'b', 'a', 'c' }, N.remove(new char[] { 'a', 'b', 'a', 'c' }, 'a'));
        assertArrayEquals(new byte[] { 2, 1, 3 }, N.remove(new byte[] { 1, 2, 1, 3 }, (byte) 1));
        assertArrayEquals(new short[] { 2, 1, 3 }, N.remove(new short[] { 1, 2, 1, 3 }, (short) 1));
        assertArrayEquals(new int[] { 2, 3, 1 }, N.remove(new int[] { 1, 2, 3, 1 }, 1));
        assertArrayEquals(new long[] { 2L, 1L, 3L }, N.remove(new long[] { 1L, 2L, 1L, 3L }, 1L));
        assertArrayEquals(new float[] { 2.0f, 1.0f, 3.0f }, N.remove(new float[] { 1.0f, 2.0f, 1.0f, 3.0f }, 1.0f), 0.001f);
        assertArrayEquals(new double[] { 2.0, 1.0, 3.0 }, N.remove(new double[] { 1.0, 2.0, 1.0, 3.0 }, 1.0), 0.001);
        assertArrayEquals(new String[] { "b", "a", "c" }, N.remove(new String[] { "a", "b", "a", "c" }, "a"));
        assertArrayEquals(CommonUtil.EMPTY_STRING_ARRAY, N.remove((String[]) null, "a"));
        assertArrayEquals(CommonUtil.EMPTY_STRING_ARRAY, N.remove(new String[0], "a"));

        List<String> list = new ArrayList<>(Arrays.asList("a", "b", "a", "c"));
        assertTrue(N.remove(list, "a"));
        assertEquals(Arrays.asList("b", "a", "c"), list);
        assertFalse(N.remove(list, "x"));
        assertFalse(N.remove(new ArrayList<String>(), "a"));
        assertFalse(N.remove((Collection<String>) null, "a"));
    }

    @Test
    public void testRemoveAt() {
        assertArrayEquals(new boolean[] { true, true }, N.removeAt(new boolean[] { true, false, true }, 1));
        assertArrayEquals(new boolean[] { true, true, false }, N.removeAt(new boolean[] { true, false, true, false }, new int[] { 1 }));
        assertArrayEquals(new char[] { 'b', 'c' }, N.removeAt(new char[] { 'a', 'b', 'c' }, 0));
        assertArrayEquals(new char[] { 'a', 'c', 'e' }, N.removeAt(new char[] { 'a', 'b', 'c', 'd', 'e' }, 1, 3));
        assertArrayEquals(new char[] { 'a', 'c', 'e' }, N.removeAt(new char[] { 'a', 'b', 'c', 'd', 'e' }, 1, 1, 3));
        char[] chars = { 'a', 'b', 'c' };
        assertArrayEquals(chars.clone(), N.removeAt(chars));
        assertArrayEquals(new byte[] { 1, 2 }, N.removeAt(new byte[] { 1, 2, 3 }, 2));
        assertArrayEquals(new byte[] { 2, 4 }, N.removeAt(new byte[] { 1, 2, 3, 4, 5 }, 0, 2, 4));
        assertArrayEquals(new short[] { 10, 30 }, N.removeAt(new short[] { 10, 20, 30 }, 1));
        assertArrayEquals(new int[] { 1, 2, 4, 5 }, N.removeAt(new int[] { 1, 2, 3, 4, 5 }, 2));
        assertArrayEquals(new int[] { 2, 4 }, N.removeAt(new int[] { 1, 2, 3, 4, 5 }, 0, 2, 4));
        assertArrayEquals(new int[0], N.removeAt(new int[] { 42 }, 0));
        assertArrayEquals(new int[] { 10, 30, 50 }, N.removeAt(new int[] { 10, 20, 30, 40, 50 }, 3, 1, 1));
        assertArrayEquals(new long[] { 200L, 300L }, N.removeAt(new long[] { 100L, 200L, 300L }, 0));
        assertArrayEquals(new float[] { 1.0f, 3.0f }, N.removeAt(new float[] { 1.0f, 2.0f, 3.0f }, 1), DELTAf);
        assertArrayEquals(new double[] { 1.0, 2.0 }, N.removeAt(new double[] { 1.0, 2.0, 3.0 }, 2), DELTA);
        assertArrayEquals(new String[] { "a", "c" }, N.removeAt(new String[] { "a", "b", "c" }, 1));
        String[] generic = { "A", "B", "C", "D", "E" };
        assertArrayEquals(new String[] { "A", "C", "E" }, N.removeAt(generic, 1, 3));
        String[] cloned = N.removeAt(generic, new int[0]);
        assertArrayEquals(generic, cloned);
        assertNotSame(generic, cloned);

        assertThrows(IllegalArgumentException.class, () -> N.removeAt((int[]) null, 0));
        assertThrows(IndexOutOfBoundsException.class, () -> N.removeAt(new int[] { 1, 2, 3 }, 5));
        assertThrows(IndexOutOfBoundsException.class, () -> N.removeAt(new char[] { 'a', 'b', 'c' }, new int[] { 0, 10 }));
        assertThrows(IndexOutOfBoundsException.class, () -> N.removeAt(new int[0], 0));

        final boolean[] ba = { true, false };
        assertArrayEquals(new boolean[] { true, false }, N.removeAt(ba, (int[]) null));
        assertNotSame(ba, N.removeAt(ba, (int[]) null));
        assertArrayEquals(new int[0], N.removeAt((int[]) null, (int[]) null));

        assertThrows(IndexOutOfBoundsException.class, () -> N.removeAt((boolean[]) null, new int[] { 0 }));
        assertThrows(IndexOutOfBoundsException.class, () -> N.removeAt((String[]) null, new int[] { 0 }));
        assertThrows(IndexOutOfBoundsException.class, () -> N.removeAt((int[]) null, new int[] { 0, 1 }));
        assertThrows(IllegalArgumentException.class, () -> N.removeAt((int[]) null, 0));
        assertArrayEquals(new int[] {}, N.removeAt((int[]) null, new int[0]));

        List<String> list = new ArrayList<>(Arrays.asList("a", "b", "c", "d"));
        assertTrue(N.removeAt(list, 1, 3));
        assertEquals(Arrays.asList("a", "c"), list);
        assertFalse(N.removeAt(new ArrayList<>(Arrays.asList("a", "b")), new int[0]));
        List<Integer> ints = new ArrayList<>(Arrays.asList(10, 20, 30));
        assertTrue(N.removeAt(ints, 1));
        assertEquals(Arrays.asList(10, 30), ints);
    }

    @Test
    public void testRemoveAll() {
        assertArrayEquals(new boolean[] { false, false }, N.removeAll(new boolean[] { true, false, true, false, true }, true));
        assertArrayEquals(new boolean[0], N.removeAll(new boolean[] { true, false, true }, true, false));
        assertArrayEquals(new boolean[0], N.removeAll((boolean[]) null, true));
        assertArrayEquals(new char[] { 'b', 'd' }, N.removeAll(new char[] { 'a', 'b', 'c', 'd', 'a' }, 'a', 'c'));
        char[] chars = { 'x', 'y', 'z' };
        char[] charClone = N.removeAll(chars, new char[0]);
        assertArrayEquals(chars, charClone);
        assertNotSame(chars, charClone);
        assertArrayEquals(new char[] { 'b', 'c' }, N.removeAll(new char[] { 'a', 'b', 'a', 'c', 'a' }, 'a'));
        assertArrayEquals(new byte[] { 2, 4 }, N.removeAll(new byte[] { 1, 2, 3, 4, 1 }, (byte) 1, (byte) 3));
        assertArrayEquals(new short[] { 1, 4, 5 }, N.removeAll(new short[] { 1, 2, 3, 2, 4, 3, 5 }, (short) 2, (short) 3));
        int[] ints = { 1, 2, 3, 2, 4, 3, 5 };
        assertArrayEquals(new int[] { 1, 4, 5 }, N.removeAll(ints, 2, 3));
        int[] intClone = N.removeAll(ints, new int[0]);
        assertArrayEquals(ints, intClone);
        assertNotSame(ints, intClone);
        assertArrayEquals(new long[] { 100L, 400L }, N.removeAll(new long[] { 100L, 200L, 300L, 200L, 400L }, 200L, 300L));
        assertArrayEquals(new float[] { 1f, 4.5f }, N.removeAll(new float[] { 1f, 2.5f, 3f, 2.5f, 4.5f }, 2.5f, 3f), 0f);
        assertArrayEquals(new double[] { 1d, 4.5d }, N.removeAll(new double[] { 1d, 2.5d, 3d, 2.5d, 4.5d }, 2.5d, 3d), 0d);
        assertArrayEquals(new String[] { "c" }, N.removeAll(new String[] { "a", "b", "a", "c", "b" }, "a", "b"));
        assertArrayEquals(CommonUtil.EMPTY_STRING_ARRAY, N.removeAll((String[]) null, "a"));

        final int[] first = { 1, 2 };
        final int[][] source = { first, new int[] { 3 } };
        assertEquals(2, N.removeAll(source, new int[] { 1, 2 }, new int[] { 9 }).length);

        Collection<String> coll = new ArrayList<>(Arrays.asList("a", "b", "a", "c", "b", "d"));
        assertTrue(N.removeAll(coll, "a", "b"));
        assertEquals(Arrays.asList("c", "d"), coll);
        assertFalse(N.removeAll(coll));

        final TreeSet<String> caseInsensitive = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        caseInsensitive.add("ABC");
        final List<String> ci = new ArrayList<>(Arrays.asList("abc", "xyz"));
        assertTrue(N.removeAll(ci, caseInsensitive));
        assertEquals(Arrays.asList("xyz"), ci);

        final List<String> selfList = new ArrayList<>(Arrays.asList("a", "b"));
        assertTrue(N.removeAll(selfList, selfList));
        assertTrue(selfList.isEmpty());
        final Set<String> selfSet = new LinkedHashSet<>(Arrays.asList("a", "b"));
        assertTrue(N.removeAll(selfSet, selfSet.iterator()));
        assertTrue(selfSet.isEmpty());

        Collection<String> fromIter = new ArrayList<>(Arrays.asList("a", "b", "a", "c", "b", "d"));
        assertTrue(N.removeAll(fromIter, Arrays.asList("a", "b")));
        assertFalse(N.removeAll(fromIter, (Iterable<String>) null));
        Set<String> hash = new HashSet<>(Arrays.asList("a", "b", "c"));
        assertTrue(N.removeAll(hash, Arrays.asList("a", "x")));
        assertEquals(new HashSet<>(Arrays.asList("b", "c")), hash);
        List<String> viewBacked = new LinkedList<>(Arrays.asList("a", "b", "c", "d"));
        assertTrue(N.removeAll(viewBacked, viewBacked.subList(0, 2)));
        assertEquals(Arrays.asList("c", "d"), viewBacked);

        Collection<String> fromIterator = new ArrayList<>(Arrays.asList("a", "b", "a", "c", "b", "d"));
        assertTrue(N.removeAll(fromIterator, Arrays.asList("a", "b").iterator()));
        assertFalse(N.removeAll(fromIterator, (Iterator<String>) null));
        Set<String> iterSet = new LinkedHashSet<>(Arrays.asList("a", "b", "c"));
        assertTrue(N.removeAll(iterSet, iterSet.iterator()));
        assertTrue(iterSet.isEmpty());
        Set<String> noChange = new HashSet<>(Arrays.asList("a", "b"));
        assertFalse(N.removeAll(noChange, Arrays.asList("q").iterator()));
    }

    @Test
    public void testRemoveAllOccurrences() {
        assertArrayEquals(new boolean[] { false }, N.removeAllOccurrences(new boolean[] { true, false, true }, true));
        assertArrayEquals(new boolean[0], N.removeAllOccurrences((boolean[]) null, true));
        assertArrayEquals(new char[] { 'b', 'c' }, N.removeAllOccurrences(new char[] { 'a', 'b', 'a', 'c', 'a' }, 'a'));
        assertArrayEquals(new byte[] { 2, 3 }, N.removeAllOccurrences(new byte[] { 1, 2, 1, 3, 1 }, (byte) 1));
        assertArrayEquals(new short[] { 2, 3 }, N.removeAllOccurrences(new short[] { 1, 2, 1, 3, 1 }, (short) 1));
        assertArrayEquals(new int[] { 2, 3 }, N.removeAllOccurrences(new int[] { 1, 2, 1, 3, 1 }, 1));
        assertArrayEquals(new long[] { 2L, 3L }, N.removeAllOccurrences(new long[] { 1L, 2L, 1L, 3L, 1L }, 1L));
        assertArrayEquals(new float[] { 2.0f, 3.0f }, N.removeAllOccurrences(new float[] { 1.0f, 2.0f, 1.0f, 3.0f, 1.0f }, 1.0f), 0.001f);
        assertArrayEquals(new float[] { 1.0f, 2.0f }, N.removeAllOccurrences(new float[] { 1.0f, Float.NaN, 2.0f, Float.NaN }, Float.NaN), 0.0f);
        assertArrayEquals(new double[] { 2.0, 3.0 }, N.removeAllOccurrences(new double[] { 1.0, 2.0, 1.0, 3.0, 1.0 }, 1.0), 0.001);
        assertArrayEquals(new String[] { "b", "c" }, N.removeAllOccurrences(new String[] { "a", "b", "a", "c", "a" }, "a"));
        assertArrayEquals(new String[] { "a", "a", "b" }, N.removeAllOccurrences(new String[] { "a", null, "a", null, "b" }, null));
        assertArrayEquals(CommonUtil.EMPTY_STRING_ARRAY, N.removeAllOccurrences((String[]) null, "a"));

        final int[] first = { 1, 2 };
        final int[][] source = { first, new int[] { 3 } };
        assertEquals(2, N.removeAllOccurrences(source, new int[] { 1, 2 }).length);
        assertEquals(1, N.removeAllOccurrences(source, first).length);

        Collection<String> coll = new ArrayList<>(Arrays.asList("a", "b", "a", "c", "a"));
        assertTrue(N.removeAllOccurrences(coll, "a"));
        assertEquals(Arrays.asList("b", "c"), coll);
        assertFalse(N.removeAllOccurrences(coll, "d"));
        assertFalse(N.removeAllOccurrences(new ArrayList<String>(), "a"));
    }

    @Test
    public void testRemoveDuplicates() {
        assertArrayEquals(new boolean[] { true, false }, N.removeDuplicates(new boolean[] { true, false, true }));
        assertArrayEquals(new boolean[0], N.removeDuplicates((boolean[]) null));
        assertArrayEquals(new char[] { 'a', 'b', 'c' }, N.removeDuplicates(new char[] { 'a', 'b', 'a', 'c', 'b' }));
        assertArrayEquals(new char[] { 'a', 'b', 'c' }, N.removeDuplicates(new char[] { 'a', 'a', 'b', 'c', 'c' }, true));
        assertArrayEquals(new char[] { 'b', 'a' }, N.removeDuplicates(new char[] { 'x', 'b', 'b', 'a', 'y' }, 1, 4, false));
        assertArrayEquals(new char[0], N.removeDuplicates(new char[] { 'a', 'b' }, 1, 1, true));
        assertArrayEquals(new byte[] { 1, 2, 3 }, N.removeDuplicates(new byte[] { 1, 2, 1, 3, 2 }));
        assertArrayEquals(new byte[] { 1, 2 }, N.removeDuplicates(new byte[] { 5, 1, 2, 1, 5 }, 1, 4, false));
        short[] shorts = { 1, 2, 3, 2, 1 };
        assertArrayEquals(new short[0], N.removeDuplicates(shorts, 2, 2, false));
        assertArrayEquals(new short[] { 2 }, N.removeDuplicates(shorts, 1, 2, false));
        assertArrayEquals(new short[] { 1, 2, 3 }, N.removeDuplicates(new short[] { 1, 1, 2, 2, 3 }, 0, 5, true));
        assertArrayEquals(new int[] { 1, 2, 3 }, N.removeDuplicates(new int[] { 1, 2, 1, 3, 2 }));
        assertArrayEquals(new int[] { 1, 2, 3 }, N.removeDuplicates(new int[] { 1, 1, 2, 3, 3 }, true));
        assertArrayEquals(new long[] { 1L, 2L, 3L }, N.removeDuplicates(new long[] { 1L, 2L, 1L, 3L, 2L }));
        assertArrayEquals(new float[] { 1.0f, 2.0f, 3.0f }, N.removeDuplicates(new float[] { 1.0f, 2.0f, 1.0f, 3.0f, 2.0f }), 0.001f);
        assertArrayEquals(new float[0], N.removeDuplicates(new float[] { 1.0f, 2.0f }, 1, 1, false), 0.0f);
        assertArrayEquals(new double[] { 1.0, 2.0, 3.0 }, N.removeDuplicates(new double[] { 1.0, 2.0, 1.0, 3.0, 2.0 }), 0.001);
        assertArrayEquals(new String[] { "a", "b", "c" }, N.removeDuplicates(new String[] { "a", "b", "a", "c", "b" }));
        assertArrayEquals(CommonUtil.EMPTY_STRING_ARRAY, N.removeDuplicates((String[]) null));

        Integer[] in = { 1, 2, 1, 3, null, 2, null };
        assertArrayEquals(new Integer[] { 1, 2, 3, null }, N.removeDuplicates(in));
        assertSame(null, N.removeDuplicates((Integer[]) null));

        String[] src = { "a", "b", "c" };
        String[] emptyRange = N.removeDuplicates(src, 0, 0, false);
        assertEquals(0, emptyRange.length);
        assertNotSame(src, emptyRange);
        assertEquals(String.class, emptyRange.getClass().getComponentType());

        final Integer[] empty = new Integer[0];
        assertSame(empty, N.removeDuplicates(empty, 0, 0, false));
        assertNull(N.removeDuplicates((Integer[]) null, 0, 0, false));
        final String[] emptyStr = new String[0];
        assertNotSame(emptyStr, N.removeDuplicates(emptyStr, 0, 0, false));

        final Object first = new Object[] { new int[] { 1, 2 }, null };
        final Object duplicate = new Object[] { new int[] { 1, 2 }, null };
        final Object last = new Object[] { new int[] { 3 }, null };
        Object[] nested = { first, duplicate, last };
        Object[] sortedResult = N.removeDuplicates(nested, true);
        assertEquals(2, sortedResult.length);
        assertSame(first, sortedResult[0]);
        assertSame(last, sortedResult[1]);

        Collection<String> coll = new ArrayList<>(Arrays.asList("a", "b", "a", "c", "b"));
        assertTrue(N.removeDuplicates(coll));
        assertEquals(3, coll.size());
        assertFalse(N.removeDuplicates(new ArrayList<>(Arrays.asList("a", "b", "c"))));
        assertFalse(N.removeDuplicates(new HashSet<>(Arrays.asList("a", "b", "c"))));
        Collection<Integer> sortedList = new LinkedList<>(Arrays.asList(1, 1, 2, 3, 3, 3, 4));
        assertTrue(N.removeDuplicates(sortedList, true));
        assertEquals(Arrays.asList(1, 2, 3, 4), sortedList);
        List<Object> pair = new ArrayList<>(Arrays.asList(first, duplicate));
        assertTrue(N.removeDuplicates(pair, false));
        assertSame(first, pair.get(0));
    }

    @Test
    public void testRemoveRange() {
        assertArrayEquals(new boolean[] { true, false, true }, N.removeRange(new boolean[] { true, false, true, false, true }, 1, 3));
        assertArrayEquals(new char[] { 'a', 'd', 'e' }, N.removeRange(new char[] { 'a', 'b', 'c', 'd', 'e' }, 1, 3));
        assertArrayEquals(new int[] { 3, 4, 5 }, N.removeRange(new int[] { 1, 2, 3, 4, 5 }, 0, 2));
        assertArrayEquals(new int[] { 1, 2, 3 }, N.removeRange(new int[] { 1, 2, 3 }, 1, 1));
        assertArrayEquals(new long[] { 10L, 20L }, N.removeRange(new long[] { 10L, 20L, 30L, 40L }, 2, 4));
        assertEquals(0, N.removeRange(new double[] { 1.0, 2.0, 3.0 }, 0, 3).length);
        assertArrayEquals(new String[] { "a", "d" }, N.removeRange(new String[] { "a", "b", "c", "d" }, 1, 3));
        assertThrows(IndexOutOfBoundsException.class, () -> N.removeRange(new int[] { 1, 2, 3 }, -1, 2));

        List<String> list = new ArrayList<>(Arrays.asList("a", "b", "c", "d", "e"));
        assertTrue(N.removeRange(list, 1, 3));
        assertEquals(Arrays.asList("a", "d", "e"), list);
        assertFalse(N.removeRange(new ArrayList<>(Arrays.asList("a", "b")), 1, 1));

        assertEquals("aef", N.removeRange("abcdef", 1, 4));
        assertEquals("", N.removeRange("abc", 0, 3));
    }

    @Test
    public void testRemoveAll_unmodifiableReceiverThrowsEvenWhenNothingMatches() {
        final List<String> unmodifiable = java.util.Collections.unmodifiableList(new ArrayList<>(Arrays.asList("a")));

        // Both argument shapes behave alike now: the removal is attempted, so an unmodifiable receiver reports
        // that it does not support it - exactly as Collection.removeAll and N.retainAll do.
        assertThrows(UnsupportedOperationException.class, () -> N.removeAll(unmodifiable, java.util.Set.of("z")));
        assertThrows(UnsupportedOperationException.class, () -> N.removeAll(unmodifiable, Arrays.asList("z")));

        // An empty value set still short-circuits before touching the receiver.
        assertFalse(N.removeAll(unmodifiable, java.util.Collections.emptyList()));

        // A modifiable receiver is unaffected by the change.
        final List<String> modifiable = new ArrayList<>(Arrays.asList("a", "b"));
        assertFalse(N.removeAll(modifiable, java.util.Set.of("z")));
        assertTrue(N.removeAll(modifiable, java.util.Set.of("a")));
        assertEquals(Arrays.asList("b"), modifiable);
    }
}

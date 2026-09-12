package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

public class NZipTest extends NTestSupport {

    @Test
    public void testZip_two() {
        String[] a = { "a", "b", "c" };
        Integer[] b = { 1, 2, 3 };
        List<Pair<String, Integer>> result = N.zip(a, b, Pair::of);
        assertEquals(3, result.size());
        assertEquals(Pair.of("a", 1), result.get(0));
        assertEquals(Pair.of("c", 3), result.get(2));

        assertEquals(3, N.zip(new String[] { "a", "b", "c", "d", "e" }, new Integer[] { 1, 2, 3 }, Pair::of).size());
        assertEquals(3, N.zip(Arrays.asList("a", "b", "c"), Arrays.asList(1, 2, 3), Pair::of).size());

        Pair<String, Integer>[] array = N.zip(a, b, Pair::of, Pair.class);
        assertEquals(3, array.length);
        assertEquals("a", array[0].left());
        assertEquals(2, N.zip(new String[] { "a", "b", "c", "d", "e" }, new Integer[] { 1, 2 }, Pair::of, Pair.class).length);

        assertTrue(N.zip((String[]) null, b, Pair::of).isEmpty());
        assertTrue(N.zip(a, (Integer[]) null, Pair::of).isEmpty());
        assertTrue(N.zip((String[]) null, (Integer[]) null, Pair::of).isEmpty());
        assertTrue(N.zip(new String[0], new Integer[0], Pair::of).isEmpty());
        assertTrue(N.zip((List<String>) null, Arrays.asList(1, 2, 3), Pair::of).isEmpty());
    }

    @Test
    public void testZip_three() {
        String[] a = { "a", "b", "c" };
        Integer[] b = { 1, 2, 3 };
        Boolean[] c = { true, false, true };
        List<Triple<String, Integer, Boolean>> result = N.zip(a, b, c, Triple::of);
        assertEquals(3, result.size());
        assertEquals("a", result.get(0).left());
        assertEquals(1, result.get(0).middle());
        assertEquals(true, result.get(0).right());

        assertEquals(2, N.zip(new String[] { "a", "b", "c", "d", "e" }, new Integer[] { 1, 2, 3 }, new Boolean[] { true, false }, Triple::of).size());
        assertEquals(3, N.zip(Arrays.asList("a", "b", "c"), Arrays.asList(1, 2, 3), Arrays.asList(true, false, true), Triple::of).size());

        Triple<String, Integer, Boolean>[] array = N.zip(a, b, c, Triple::of, Triple.class);
        assertEquals(3, array.length);
        assertEquals(1, N.zip(new String[] { "a", "b", "c", "d" }, new Integer[] { 1, 2, 3 }, new Boolean[] { true }, Triple::of, Triple.class).length);

        assertTrue(N.zip(a, (Integer[]) null, c, Triple::of).isEmpty());
        assertTrue(N.zip(Arrays.asList("a", "b"), null, Arrays.asList(true, false), Triple::of).isEmpty());
    }

    @Test
    public void testZip_withDefaults() {
        List<Pair<String, Integer>> two = N.zip(new String[] { "a", "b", "c" }, new Integer[] { 1, 2, 3, 4, 5 }, "default", 0, Pair::of);
        assertEquals(5, two.size());
        assertEquals("default", two.get(3).left());
        assertEquals(4, two.get(3).right());

        List<Pair<String, Integer>> firstLonger = N.zip(new String[] { "a", "b", "c", "d", "e" }, new Integer[] { 1, 2 }, "default", 0, Pair::of);
        assertEquals(5, firstLonger.size());
        assertEquals(0, firstLonger.get(2).right());

        List<Pair<String, Integer>> iterables = N.zip(Arrays.asList("a", "b"), Arrays.asList(1, 2, 3, 4), "default", 0, Pair::of);
        assertEquals(4, iterables.size());
        assertEquals("default", iterables.get(2).left());

        Pair<String, Integer>[] toArray = N.zip(new String[] { "a", "b" }, new Integer[] { 1, 2, 3, 4 }, "default", 0, Pair::of, Pair.class);
        assertEquals(4, toArray.length);
        assertEquals("default", toArray[2].left());

        assertTrue(N.zip((String[]) null, (Integer[]) null, "default", 0, Pair::of).isEmpty());
        List<Pair<String, Integer>> firstNull = N.zip((String[]) null, new Integer[] { 1, 2, 3 }, "default", 0, Pair::of);
        assertEquals(3, firstNull.size());
        assertEquals("default", firstNull.get(0).left());

        Iterable<String> nonCollA = new CustomIterable<>(Arrays.asList("a", "b"));
        Iterable<Integer> nonCollB = new CustomIterable<>(Arrays.asList(1, 2, 3));
        assertEquals(Arrays.asList("a1", "b2", "x3"), N.zip(nonCollA, nonCollB, "x", 0, (s, i) -> s + i));
        assertEquals(Arrays.asList("x1", "y2", "z-1"), N.zip(Arrays.asList("x", "y", "z"), Arrays.asList(1, 2), "DEF", -1, (s, n) -> s + n));
    }

    @Test
    public void testZip_threeWithDefaults() {
        List<Triple<String, Integer, Boolean>> result = N.zip(new String[] { "a", "b" }, new Integer[] { 1, 2, 3 }, new Boolean[] { true }, "default", 0, false,
                Triple::of);
        assertEquals(3, result.size());
        assertEquals("default", result.get(2).left());
        assertEquals(3, result.get(2).middle());
        assertEquals(false, result.get(2).right());

        List<Triple<String, Integer, Boolean>> allDifferent = N.zip(new String[] { "a", "b", "c", "d", "e" }, new Integer[] { 1, 2, 3 }, new Boolean[] { true },
                "default", 0, false, Triple::of);
        assertEquals(5, allDifferent.size());
        assertEquals("d", allDifferent.get(3).left());
        assertEquals(0, allDifferent.get(3).middle());

        Triple<String, Integer, Boolean>[] array = N.zip(new String[] { "a", "b" }, new Integer[] { 1, 2, 3 }, new Boolean[] { true }, "default", 0, false,
                Triple::of, Triple.class);
        assertEquals(3, array.length);
        assertEquals("default", array[2].left());

        Iterable<String> a = new CustomIterable<>(Arrays.asList("a", "b", "c"));
        Iterable<Integer> b = new CustomIterable<>(Arrays.asList(1, 2));
        Iterable<Boolean> c = new CustomIterable<>(Arrays.asList(true));
        assertEquals(Arrays.asList("a1true", "b2false", "c0false"), N.zip(a, b, c, "x", 0, false, (s, i, bool) -> s + i + bool));
    }

    @Test
    public void testZip_nullTargetElementTypeReportsIllegalArgumentException() {
        final String[] a = { "a" };
        final Integer[] b = { 1 };
        final Long[] c = { 1L };

        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class, () -> N.zip(a, b, (s, i) -> s + i, (Class<String>) null));
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class, () -> N.zip(a, b, "", 0, (s, i) -> s + i, (Class<String>) null));
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                () -> N.zip(a, b, c, (s, i, l) -> s + i + l, (Class<String>) null));
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                () -> N.zip(a, b, c, "", 0, 0L, (s, i, l) -> s + i + l, (Class<String>) null));
        // Also on the empty-input path: a zero-length result still reaches the array factory.
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                () -> N.zip(new String[0], new Integer[0], (s, i) -> s + i, (Class<String>) null));
    }
}

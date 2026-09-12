package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

import org.junit.jupiter.api.Test;

public class NMinTest extends NTestSupport {

    @Test
    public void testMinWithOnlyNaNAndNullValues() {
        Double[] values = { null, Double.NaN, Double.NaN };
        List<Double> list = Arrays.asList(values);
        assertTrue(N.min(values).isNaN());
        assertTrue(N.min(list).isNaN());
        assertTrue(N.min(list.iterator()).isNaN());
        assertTrue(N.min((Double) null, Double.valueOf(Double.NaN)).isNaN());
        assertTrue(N.min((Double) null, Double.valueOf(Double.NaN), (Double) null).isNaN());

        for (String[] labels : new String[][] { { "first", "second" }, { "null", "first", "second" } }) {
            java.util.function.Function<String, Double> key = s -> "null".equals(s) ? null : Double.NaN;
            assertEquals("first", N.minBy(labels, key));
            assertEquals("first", N.minBy(Arrays.asList(labels), key));
            assertEquals("first", N.minBy(Arrays.asList(labels).iterator(), key));
        }
    }

    @Test
    public void testMin_twoAndThreeValues() {
        assertEquals('a', N.min('a', 'b'));
        assertEquals((byte) 1, N.min((byte) 1, (byte) 2));
        assertEquals((short) 1, N.min((short) 1, (short) 2));
        assertEquals(1, N.min(1, 2));
        assertEquals(1L, N.min(1L, 2L));
        assertEquals(1.0f, N.min(1.0f, 2.0f), DELTAf);
        assertEquals(1.0, N.min(1.0, 2.0), DELTA);

        assertEquals('a', N.min('a', 'b', 'c'));
        assertEquals((byte) 1, N.min((byte) 1, (byte) 2, (byte) 3));
        assertEquals((short) 1, N.min((short) 1, (short) 2, (short) 3));
        assertEquals(1, N.min(1, 2, 3));
        assertEquals(1L, N.min(1L, 2L, 3L));
        assertEquals(1.0f, N.min(1.0f, 2.0f, 3.0f), DELTAf);
        assertEquals(1.0, N.min(1.0, 2.0, 3.0), DELTA);

        assertEquals("a", N.min("a", "b"));
        assertEquals("a", N.min("a", "b", "c"));
        assertEquals("a", N.min(null, "a"));
        assertEquals("a", N.min("a", null));
        assertEquals(Integer.valueOf(1), N.min(1, 2, Comparator.naturalOrder()));
        assertEquals("b", N.min("a", "b", Comparator.reverseOrder()));
        assertEquals("c", N.min("a", "b", "c", Comparator.reverseOrder()));
    }

    @Test
    public void testMin_arrays() {
        assertEquals('1', N.min(new char[] { '3', '2', '1', '4', '5' }));
        assertEquals((byte) 1, N.min(new byte[] { 3, 2, 1, 4, 5 }));
        assertEquals((short) 1, N.min(new short[] { 3, 2, 1, 4, 5 }));
        assertEquals(1, N.min(new int[] { 3, 2, 1, 4, 5 }));
        assertEquals(1L, N.min(new long[] { 3, 2, 1, 4, 5 }));
        assertEquals(1.0f, N.min(new float[] { 3, 2, 1, 4, 5 }), DELTAf);
        assertEquals(1.0, N.min(new double[] { 3, 2, 1, 4, 5 }), DELTA);

        assertEquals('1', N.min(new Character[] { '3', '2', '1', '4', '5' }).charValue());
        assertEquals(1, N.min(new Integer[] { 3, 2, 1, 4, 5 }).intValue());

        assertEquals('b', N.min(new char[] { 'a', 'b', 'c', 'd' }, 1, 3));
        assertEquals((byte) 2, N.min(new byte[] { 1, 2, 3, 4 }, 1, 3));
        assertEquals(2, N.min(new int[] { 1, 2, 3, 4 }, 1, 3));
        assertEquals("b", N.min(new String[] { "a", "b", "c", "d" }, 1, 3));
        assertEquals("b", N.min(new String[] { "a", "b", "c", "d" }, 1, 3, Comparator.naturalOrder()));

        assertEquals("five", N.min(stringArray));
        assertEquals("four", N.min(stringArray, 1, 4));
        assertEquals("two", N.min(stringArray, Comparator.reverseOrder()));

        Integer[] range = { 10, 5, 8, 3, 15 };
        assertEquals(Integer.valueOf(5), N.min(range, 0, 3, Comparator.naturalOrder()));

        assertThrows(IllegalArgumentException.class, () -> N.min(new int[] {}));
        assertThrows(IllegalArgumentException.class, () -> N.min((int[]) null));
        assertThrows(IllegalArgumentException.class, () -> N.min(new char[] {}));
        assertThrows(IllegalArgumentException.class, () -> N.min(new String[] {}));
    }

    @Test
    public void testMin_collectionIterableIterator() {
        assertEquals(Integer.valueOf(1), N.min(Arrays.asList(1, 2, 3, 4)));
        assertEquals(Integer.valueOf(1), N.min(Arrays.asList(1, 2, 3, 4), Comparator.naturalOrder()));
        assertEquals(Integer.valueOf(1), N.min(Arrays.asList(null, 1, 2, 3)));
        assertNull(N.min(Arrays.asList(null, null, null)));

        assertEquals(Integer.valueOf(1), N.min(Arrays.asList(1, 2, 3, 4), 0, 4));
        assertEquals(Integer.valueOf(2), N.min(Arrays.asList(1, 2, 3, 4), 1, 3));
        assertEquals(Integer.valueOf(2), N.min(Arrays.asList(1, 2, 3, 4), 1, 3, Comparator.naturalOrder()));

        List<Integer> linked = new LinkedList<>(Arrays.asList(10, 5, 8, 3, 15));
        assertEquals(Integer.valueOf(5), N.min(linked, 0, 3));

        Iterable<Integer> notACollection = () -> Arrays.asList(7, 2, 9, 1).iterator();
        assertEquals(Integer.valueOf(1), N.min(notACollection, Comparator.naturalOrder()));

        assertEquals(Integer.valueOf(1), N.min(Arrays.asList(1, 2, 3, 4).iterator()));
        assertEquals(Integer.valueOf(1), N.min(Arrays.asList(1, 2, 3, 4).iterator(), Comparator.naturalOrder()));
        assertEquals("five", N.min(stringList.iterator()));
        assertNull(N.min(Arrays.asList("b", null, "a").iterator(), Comparators.nullsFirst()));

        assertThrows(IllegalArgumentException.class, () -> N.min(Collections.emptyList(), 0, 0));
        assertThrows(IllegalArgumentException.class, () -> N.min(Collections.<String> emptyList()));
        assertThrows(IllegalArgumentException.class, () -> N.min(Collections.<Integer> emptyList().iterator()));
    }

    @Test
    public void testMinBy() {
        Person[] people = { new Person("John", 25), new Person("Jane", 22), new Person("Bob", 30) };
        assertEquals("Jane", N.minBy(people, p -> p.age).name);
        assertEquals("Jane", N.minBy(Arrays.asList(people), p -> p.age).name);
        assertEquals("Jane", N.minBy(Arrays.asList(people).iterator(), p -> p.age).name);

        assertEquals("KIWI", N.minBy(new String[] { "apple", "Banana", "KIWI" }, String::length));
        assertEquals(Pair.of("B", 1), N.minBy(Arrays.asList(Pair.of("A", 3), Pair.of("B", 1), Pair.of("C", 2)), it -> it.right()));
    }

    @Test
    public void testMinAll() {
        assertEquals(Arrays.asList(1, 1), N.minAll(new Integer[] { 1, 2, 3, 1, 4 }));
        assertEquals(Arrays.asList(1, 1), N.minAll(new Integer[] { 1, 2, 3, 1, 4 }, Comparator.naturalOrder()));
        assertEquals(Arrays.asList(1, 1), N.minAll(Arrays.asList(1, 2, 3, 1, 4)));
        assertEquals(Arrays.asList(1, 1), N.minAll(Arrays.asList(1, 2, 3, 1, 4).iterator()));
        assertEquals(Collections.emptyList(), N.minAll(new Integer[] {}));
        assertEquals(Collections.emptyList(), N.minAll(Collections.emptyList()));
    }

    @Test
    public void testMinOrDefaultIfEmpty() {
        assertEquals(Integer.valueOf(1), N.minValueOrDefaultIfEmpty(new Integer[] { 1, 2, 3 }, x -> x, 99));
        assertEquals(Integer.valueOf(99), N.minValueOrDefaultIfEmpty(new Integer[] {}, x -> x, 99));
        assertEquals(Integer.valueOf(1), N.minValueOrDefaultIfEmpty(Arrays.asList(1, 2, 3), x -> x, 99));
        assertEquals(Integer.valueOf(1), N.minValueOrDefaultIfEmpty(Arrays.asList(1, 2, 3).iterator(), x -> x, 99));
        assertEquals(Integer.valueOf(99), N.minValueOrDefaultIfEmpty(Collections.<Integer> emptyIterator(), x -> x, 99));

        assertEquals(1, N.minIntOrDefaultIfEmpty(new Integer[] { 1, 2, 3 }, x -> x, 99));
        assertEquals(99, N.minIntOrDefaultIfEmpty(new Integer[] {}, x -> x, 99));
        assertEquals(1L, N.minLongOrDefaultIfEmpty(new Long[] { 1L, 2L, 3L }, x -> x, 99L));
        assertEquals(99L, N.minLongOrDefaultIfEmpty(Collections.<Long> emptyList(), x -> x, 99L));
        assertEquals(1.0, N.minDoubleOrDefaultIfEmpty(new Double[] { 1.0, 2.0, 3.0 }, x -> x, 99.0), DELTA);
        assertEquals(99.0, N.minDoubleOrDefaultIfEmpty(Collections.<Double> emptyIterator(), x -> x, 99.0), DELTA);

        assertEquals(Integer.valueOf(4), N.minValueOrDefaultIfEmpty(new String[] { "apple", "kiwi", "plum" }, String::length, 100));
        assertEquals(-9, N.minIntOrDefaultIfEmpty(new String[] { "apple", "kiwi" }, s -> s.charAt(0) - 'j', 99));
        assertEquals(0.33333333, N.minDoubleOrDefaultIfEmpty(new Integer[] { 1, 2, 3 }, x -> 1.0 / x, 10.0), DELTA);

        java.util.function.Function<String, Integer> toNull = s -> null;
        assertEquals(Integer.valueOf(99), N.minValueOrDefaultIfEmpty(new String[] { "a", "b" }, toNull, 99));
        assertEquals(Integer.valueOf(99), N.minValueOrDefaultIfEmpty(Arrays.asList("a", "b"), toNull, 99));
        assertEquals(Integer.valueOf(99), N.minValueOrDefaultIfEmpty(Arrays.asList("a", "b").iterator(), toNull, 99));
    }

    @Test
    public void testMinMax() {
        assertEquals(Pair.of(1, 5), N.minMax(new Integer[] { 1, 2, 3, 4, 5 }));
        assertEquals(Pair.of(1, 5), N.minMax(new Integer[] { 1, 2, 3, 4, 5 }, Comparator.naturalOrder()));
        assertEquals(Pair.of(1, 5), N.minMax(Arrays.asList(1, 2, 3, 4, 5)));
        assertEquals(Pair.of(1, 5), N.minMax(Arrays.asList(1, 2, 3, 4, 5).iterator()));
        assertEquals(Pair.of(1, 1), N.minMax(new Integer[] { 1 }));
        assertEquals(Pair.of(1, 3), N.minMax(new Integer[] { null, 3, 1, null, 2 }));
        assertEquals(Pair.of(null, null), N.minMax(new Integer[] { null, null }));
        assertEquals(Pair.of("five", "two"), N.minMax(stringArray));
        assertEquals(Pair.of("a", "c"), N.minMax(Arrays.asList("b", "c", "a"), String::compareTo));

        assertThrows(IllegalArgumentException.class, () -> N.minMax(new Integer[] {}));
        assertThrows(IllegalArgumentException.class, () -> N.minMax(Collections.emptyList()));
        assertThrows(IllegalArgumentException.class, () -> N.minMax(Collections.<String> emptyList().iterator()));
    }

    @Test
    public void testMin_nanAndEmpty() {
        assertEquals(Float.NaN, N.min(Float.NaN, 1.0f), DELTA);
        assertEquals(Float.NaN, N.min(1.0f, Float.NaN), DELTA);
        assertEquals(Float.NaN, N.min(new float[] { 3.0f, 1.0f, Float.NaN, 2.0f }), DELTA);
        assertTrue(Float.isNaN(N.min(new float[] { 1.0f, Float.NaN, 3.0f })));
        assertEquals(1.0f, N.min(new float[] { 1.0f, Float.NaN, 3.0f }, 0, 1), 0.0f);

        assertEquals(-1, N.min(new int[] { 3, 1, 4, 1, 5, 9, -1, 2, 6 }));
        assertEquals(Integer.valueOf(1), N.min(new Integer[] { 3, 1, null, 5 }));
        assertEquals("apple", N.min(Arrays.asList("zebra", "apple", "Banana"), String.CASE_INSENSITIVE_ORDER));
    }

    @Test
    public void testMinBy_keyExtractorIsAppliedToNullElements() {
        // The extractor is applied to every element that takes part in a comparison, a null element included.
        assertThrows(NullPointerException.class, () -> N.minBy(new String[] { null, "x" }, String::length));
        assertThrows(NullPointerException.class, () -> N.minBy(new String[] { null, null }, String::length));
        assertThrows(NullPointerException.class, () -> N.minBy(Arrays.asList(null, "x"), String::length));

        // ... except for a single-element input, where no comparison is made at all.
        final java.util.concurrent.atomic.AtomicInteger calls = new java.util.concurrent.atomic.AtomicInteger();
        assertNull(N.minBy(new String[] { null }, s -> {
            calls.incrementAndGet();
            return s == null ? null : s.length();
        }));
        assertEquals(0, calls.get());

        // A null element ranks last only because of the key it is mapped to: a null key does it, and so does a
        // maximal non-null key.
        assertEquals("x", N.minBy(new String[] { null, "x" }, s -> s == null ? null : s.length()));
        assertEquals("x", N.minBy(new String[] { null, "x" }, s -> s == null ? Integer.MAX_VALUE : s.length()));
    }

    @Test
    public void testMin_nullCollectionRangeCheckRunsFirst() {
        assertThrows(IndexOutOfBoundsException.class, () -> N.min((java.util.Collection<String>) null, 0, 1));
        assertThrows(IndexOutOfBoundsException.class, () -> N.min((java.util.Collection<String>) null, 0, 1, Comparator.naturalOrder()));
        assertThrows(IllegalArgumentException.class, () -> N.min((java.util.Collection<String>) null, 0, 0));
        assertThrows(IllegalArgumentException.class, () -> N.min((java.util.Collection<String>) null, 0, 0, Comparator.naturalOrder()));
    }
}

package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;

import org.junit.jupiter.api.Test;

public class NMaxTest extends NTestSupport {

    @Test
    public void testMax_twoAndThreeValues() {
        assertEquals('b', N.max('a', 'b'));
        assertEquals((byte) 2, N.max((byte) 1, (byte) 2));
        assertEquals((short) 2, N.max((short) 1, (short) 2));
        assertEquals(2, N.max(1, 2));
        assertEquals(2L, N.max(1L, 2L));
        assertEquals(2.0f, N.max(1.0f, 2.0f), DELTAf);
        assertEquals(2.0, N.max(1.0, 2.0), DELTA);

        assertEquals('c', N.max('a', 'b', 'c'));
        assertEquals((byte) 3, N.max((byte) 1, (byte) 2, (byte) 3));
        assertEquals((short) 3, N.max((short) 1, (short) 2, (short) 3));
        assertEquals(3, N.max(1, 2, 3));
        assertEquals(3L, N.max(1L, 2L, 3L));
        assertEquals(3.0f, N.max(1.0f, 2.0f, 3.0f), DELTAf);
        assertEquals(3.0, N.max(1.0, 2.0, 3.0), DELTA);

        assertEquals("b", N.max("a", "b"));
        assertEquals("c", N.max("a", "b", "c"));
        assertEquals("a", N.max(null, "a"));
        assertEquals("a", N.max("a", null));
        assertEquals(Integer.valueOf(2), N.max(1, 2, Comparator.naturalOrder()));
        assertEquals("a", N.max("a", "b", Comparator.reverseOrder()));
        assertEquals("a", N.max("a", "b", "c", Comparator.reverseOrder()));
    }

    @Test
    public void testMax_arrays() {
        assertEquals('5', N.max(new char[] { '3', '2', '1', '4', '5' }));
        assertEquals((byte) 5, N.max(new byte[] { 3, 2, 1, 4, 5 }));
        assertEquals((short) 5, N.max(new short[] { 3, 2, 1, 4, 5 }));
        assertEquals(5, N.max(new int[] { 3, 2, 1, 4, 5 }));
        assertEquals(5L, N.max(new long[] { 3, 2, 1, 4, 5 }));
        assertEquals(5.0f, N.max(new float[] { 3, 2, 1, 4, 5 }), DELTAf);
        assertEquals(5.0, N.max(new double[] { 3, 2, 1, 4, 5 }), DELTA);

        assertEquals('5', N.max(new Character[] { '3', '2', '1', '4', '5' }).charValue());
        assertEquals(5, N.max(new Integer[] { 3, 2, 1, 4, 5 }).intValue());

        assertEquals('c', N.max(new char[] { 'a', 'b', 'c', 'd' }, 1, 3));
        assertEquals((byte) 3, N.max(new byte[] { 1, 2, 3, 4 }, 1, 3));
        assertEquals(3, N.max(new int[] { 1, 2, 3, 4 }, 1, 3));
        assertEquals("c", N.max(new String[] { "a", "b", "c", "d" }, 1, 3));
        assertEquals("c", N.max(new String[] { "a", "b", "c", "d" }, 1, 3, Comparator.naturalOrder()));

        assertEquals("two", N.max(stringArray));
        assertEquals("two", N.max(stringArray, 1, 4));
        assertEquals("five", N.max(stringArray, Comparator.reverseOrder()));
        assertEquals(9, N.max(new int[] { 3, 1, 4, 1, 5, 9, -1, 2, 6 }));

        assertThrows(IllegalArgumentException.class, () -> N.max(new int[] {}));
        assertThrows(IllegalArgumentException.class, () -> N.max((int[]) null));
        assertThrows(IllegalArgumentException.class, () -> N.max(new char[] {}));
        assertThrows(IllegalArgumentException.class, () -> N.max(new String[] {}));
    }

    @Test
    public void testMax_collectionIterableIterator() {
        assertEquals(Integer.valueOf(4), N.max(Arrays.asList(1, 2, 3, 4)));
        assertEquals(Integer.valueOf(4), N.max(Arrays.asList(1, 2, 3, 4), Comparator.naturalOrder()));
        assertEquals(Integer.valueOf(3), N.max(Arrays.asList(1, 2, 3, null)));
        assertNull(N.max(Arrays.asList(null, null, null)));

        assertEquals(Integer.valueOf(4), N.max(Arrays.asList(1, 2, 3, 4), 0, 4));
        assertEquals(Integer.valueOf(3), N.max(Arrays.asList(1, 2, 3, 4), 1, 3, Comparator.naturalOrder()));
        assertEquals("two", N.max(stringList));
        assertEquals("two", N.max(stringList.iterator()));
        assertNull(N.max(Arrays.asList("b", null, "a").iterator(), Comparators.nullsLast()));

        assertThrows(IllegalArgumentException.class, () -> N.max(Collections.emptyList(), 0, 0));
        assertThrows(IllegalArgumentException.class, () -> N.max(Collections.<String> emptyList()));
        assertThrows(IllegalArgumentException.class, () -> N.max(Collections.<Integer> emptyList().iterator()));
    }

    @Test
    public void testMaxBy() {
        Person[] people = { new Person("John", 25), new Person("Jane", 22), new Person("Bob", 30) };
        assertEquals("Bob", N.maxBy(people, p -> p.age).name);
        assertEquals("Bob", N.maxBy(Arrays.asList(people), p -> p.age).name);
        assertEquals("Bob", N.maxBy(Arrays.asList(people).iterator(), p -> p.age).name);
    }

    @Test
    public void testMaxAll() {
        assertEquals(Arrays.asList(4, 4), N.maxAll(new Integer[] { 1, 2, 4, 3, 4 }));
        assertEquals(Arrays.asList(4, 4), N.maxAll(new Integer[] { 1, 2, 4, 3, 4 }, Comparator.naturalOrder()));
        assertEquals(Arrays.asList(4, 4), N.maxAll(Arrays.asList(1, 2, 4, 3, 4)));
        assertEquals(Arrays.asList(4, 4), N.maxAll(Arrays.asList(1, 2, 4, 3, 4).iterator()));
        assertEquals(Collections.emptyList(), N.maxAll(new Integer[] {}));
        assertEquals(Collections.emptyList(), N.maxAll(Collections.emptyList()));
    }

    @Test
    public void testMaxOrDefaultIfEmpty() {
        assertEquals(Integer.valueOf(3), N.maxValueOrDefaultIfEmpty(new Integer[] { 1, 2, 3 }, x -> x, 99));
        assertEquals(Integer.valueOf(99), N.maxValueOrDefaultIfEmpty(new Integer[] {}, x -> x, 99));
        assertEquals(Integer.valueOf(3), N.maxValueOrDefaultIfEmpty(Arrays.asList(1, 2, 3), x -> x, 99));
        assertEquals(Integer.valueOf(3), N.maxValueOrDefaultIfEmpty(Arrays.asList(1, 2, 3).iterator(), x -> x, 99));
        assertEquals(Integer.valueOf(99), N.maxValueOrDefaultIfEmpty(Collections.<Integer> emptyIterator(), x -> x, 99));

        assertEquals(3, N.maxIntOrDefaultIfEmpty(new Integer[] { 1, 2, 3 }, x -> x, 99));
        assertEquals(99, N.maxIntOrDefaultIfEmpty(new Integer[] {}, x -> x, 99));
        assertEquals(3L, N.maxLongOrDefaultIfEmpty(new Long[] { 1L, 2L, 3L }, x -> x, 99L));
        assertEquals(99L, N.maxLongOrDefaultIfEmpty(Collections.<Long> emptyList(), x -> x, 99L));
        assertEquals(3.0, N.maxDoubleOrDefaultIfEmpty(new Double[] { 1.0, 2.0, 3.0 }, x -> x, 99.0), DELTA);
        assertEquals(99.0, N.maxDoubleOrDefaultIfEmpty(Collections.<Double> emptyIterator(), x -> x, 99.0), DELTA);
    }

    @Test
    public void testMax_nanAndEmpty() {
        assertEquals(Double.NaN, N.max(new double[] { 3.0, 1.0, Double.NaN, 2.0 }), DELTA);
        assertEquals(Double.NaN, N.max(new double[] { Double.NaN, 1.0 }), DELTA);
        assertTrue(Float.isNaN(N.max(new float[] { 1.0f, Float.NaN, 3.0f })));
        assertEquals(1.0f, N.max(new float[] { 1.0f, Float.NaN, 3.0f }, 0, 1), 0.0f);
        assertEquals(Integer.valueOf(5), N.max(new Integer[] { 3, 1, null, 5 }));
        assertEquals("zebra", N.max(Arrays.asList("zebra", "apple", "Banana"), String.CASE_INSENSITIVE_ORDER));
    }

    @Test
    public void testMaxBy_returnsNullWhenTheSelectedElementIsNull() {
        // The input is not all-null, yet the selected element is null: the key of the null element wins.
        assertNull(N.maxBy(new String[] { null, "x" }, s -> s == null ? "z" : "a"));
        assertNull(N.maxBy(Arrays.asList(null, "x"), s -> s == null ? "z" : "a"));

        // An all-null input with a null-hostile extractor throws rather than returning null.
        assertThrows(NullPointerException.class, () -> N.maxBy(new String[] { null, null }, String::length));

        // With a null-tolerant extractor a null element ranks first, so the maximum is the non-null one.
        assertEquals("x", N.maxBy(new String[] { null, "x" }, s -> s == null ? null : s.length()));
    }

    @Test
    public void testMaxBy_keyExtractorIsNotAppliedToASingleElement() {
        // Pins the javadoc note on all three maxBy overloads: the extractor is applied to every element that
        // takes part in a comparison - a null element included - "except for a single-element input, where no
        // comparison is made".
        final java.util.concurrent.atomic.AtomicInteger calls = new java.util.concurrent.atomic.AtomicInteger();
        final java.util.function.Function<String, Integer> counting = s -> {
            calls.incrementAndGet();
            return s == null ? null : s.length();
        };

        assertNull(N.maxBy(new String[] { null }, counting));
        assertEquals(0, calls.get());

        assertNull(N.maxBy(Arrays.asList((String) null), counting));
        assertEquals(0, calls.get());

        assertNull(N.maxBy(Arrays.asList((String) null).iterator(), counting));
        assertEquals(0, calls.get());

        assertEquals("ab", N.maxBy(new String[] { "ab" }, counting));
        assertEquals(0, calls.get());

        // Two elements: the extractor is applied once per element.
        assertEquals("abc", N.maxBy(new String[] { "a", "abc" }, counting));
        assertEquals(2, calls.get());

        // ... and a null element in a multi-element input is passed to the extractor too.
        assertThrows(NullPointerException.class, () -> N.maxBy(Arrays.asList(null, "x"), String::length));
        assertThrows(NullPointerException.class, () -> N.maxBy(Arrays.asList(null, "x").iterator(), String::length));
    }
}

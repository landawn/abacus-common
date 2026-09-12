package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.util.function.BooleanPredicate;
import com.landawn.abacus.util.function.BytePredicate;
import com.landawn.abacus.util.function.CharPredicate;
import com.landawn.abacus.util.function.DoublePredicate;
import com.landawn.abacus.util.function.FloatPredicate;
import com.landawn.abacus.util.function.Function;
import com.landawn.abacus.util.function.IntPredicate;
import com.landawn.abacus.util.function.LongPredicate;
import com.landawn.abacus.util.function.Predicate;
import com.landawn.abacus.util.function.ShortPredicate;

public class NFilterTest extends NTestSupport {

    @Test
    public void testFilter_primitiveArrays() {
        assertArrayEquals(new boolean[] { true, true }, N.filter(new boolean[] { true, false, true }, (BooleanPredicate) v -> v));
        assertArrayEquals(new char[] { '2', '3' }, N.filter(new char[] { '1', '2', '3' }, (CharPredicate) v -> v > '1'));
        assertArrayEquals(new byte[] { 2, 3 }, N.filter(new byte[] { 1, 2, 3 }, (BytePredicate) v -> v > 1));
        assertArrayEquals(new short[] { 2, 3 }, N.filter(new short[] { 1, 2, 3 }, (ShortPredicate) v -> v > 1));
        assertArrayEquals(new int[] { 2, 3 }, N.filter(new int[] { 1, 2, 3 }, (IntPredicate) v -> v > 1));
        assertArrayEquals(new long[] { 2L, 3L }, N.filter(new long[] { 1, 2, 3 }, (LongPredicate) v -> v > 1));
        assertArrayEquals(new float[] { 2f, 3f }, N.filter(new float[] { 1, 2, 3 }, (FloatPredicate) v -> v > 1), DELTAf);
        assertArrayEquals(new double[] { 2d, 3d }, N.filter(new double[] { 1, 2, 3 }, (DoublePredicate) v -> v > 1), DELTA);

        assertArrayEquals(CommonUtil.EMPTY_BOOLEAN_ARRAY, N.filter((boolean[]) null, b -> b));
        assertArrayEquals(CommonUtil.EMPTY_CHAR_ARRAY, N.filter((char[]) null, c -> true));
        assertArrayEquals(CommonUtil.EMPTY_BYTE_ARRAY, N.filter((byte[]) null, b -> true));
        assertArrayEquals(CommonUtil.EMPTY_SHORT_ARRAY, N.filter((short[]) null, s -> true));
        assertArrayEquals(CommonUtil.EMPTY_INT_ARRAY, N.filter((int[]) null, IS_EVEN_INT));
        assertArrayEquals(CommonUtil.EMPTY_LONG_ARRAY, N.filter((long[]) null, l -> true));
        assertArrayEquals(CommonUtil.EMPTY_FLOAT_ARRAY, N.filter((float[]) null, f -> true));
        assertArrayEquals(CommonUtil.EMPTY_DOUBLE_ARRAY, N.filter((double[]) null, d -> true));

        assertArrayEquals(new boolean[] { true, true }, N.filter(new boolean[] { true, false, true, true, false }, 0, 3, b -> b));
        assertArrayEquals(new char[] { 'x' }, N.filter(new char[] { 'x', 'y', 'x', 'z', 'x' }, 0, 2, c -> c == 'x'));
        assertArrayEquals(new byte[] { 10, 20 }, N.filter(new byte[] { 10, 20, -5, 30 }, 0, 2, b -> b > 0));
        assertArrayEquals(new short[] { 1, 2 }, N.filter(new short[] { 1, 2, 0, 3 }, 0, 2, s -> s > 0));
        assertArrayEquals(new int[] { 2, 4 }, N.filter(new int[] { 1, 2, 3, 4, 5, 6 }, 0, 4, IS_EVEN_INT));
        assertArrayEquals(new long[] { 10L, 20L }, N.filter(new long[] { 10L, 20L, -5L, 30L }, 0, 2, l -> l > 0));
        assertArrayEquals(new float[] { 1.f, 2.f }, N.filter(new float[] { 1.f, 2.f, 0.f, 3.f }, 0, 2, f -> f > 0));
        assertArrayEquals(new double[] { 1., 2. }, N.filter(new double[] { 1., 2., 0., 3. }, 0, 2, d -> d > 0));
        assertArrayEquals(CommonUtil.EMPTY_INT_ARRAY, N.filter(new int[] { 1, 2, 3 }, 1, 1, IS_EVEN_INT));
        assertThrows(IndexOutOfBoundsException.class, () -> N.filter(new int[] { 1, 2, 3 }, -1, 2, IS_EVEN_INT));
        assertThrows(IndexOutOfBoundsException.class, () -> N.filter(new boolean[] { true }, 0, 2, b -> b));
    }

    @Test
    public void testFilter_objects() {
        assertEquals(Arrays.asList("three", "four", "five"), N.filter(stringArray, s -> s.length() > 3));
        assertEquals(new HashSet<>(Arrays.asList("three", "four", "five")), N.filter(stringArray, s -> s.length() > 3, size -> new HashSet<>()));
        assertEquals(Arrays.asList("three", "four"), N.filter(stringArray, 1, 4, s -> s.length() > 3));
        assertEquals(Set.of(2, 4), N.filter(new Integer[] { 1, 2, 3, 4, 5, 6 }, 0, 4, IS_EVEN_INTEGER, size -> new HashSet<>()));
        assertTrue(N.filter((String[]) null, STRING_NOT_EMPTY).isEmpty());
        assertTrue(N.filter(new String[0], STRING_NOT_EMPTY).isEmpty());

        assertEquals(Arrays.asList("three", "four", "five"), N.filter(stringList, s -> s.length() > 3));
        assertEquals(List.of("aa", "aaa"), N.filter(Arrays.asList("a", "b", "aa", "bb", "aaa"), 1, 5, s -> s.contains("a") && s.length() > 1));
        assertEquals(Set.of("aa", "aaa"), N.filter(Arrays.asList("a", "b", "aa", "bb", "aaa"), 1, 5, s -> s.contains("a") && s.length() > 1, HashSet::new));
        assertEquals(new HashSet<>(Arrays.asList(2, 4)), N.filter(new LinkedHashSet<>(Arrays.asList(1, 2, 3, 4, 5)), 1, 4, x -> x % 2 == 0, HashSet::new));

        Iterable<String> iter = Arrays.asList("apple", "banana", "", "grape");
        assertEquals(List.of("apple", "banana", "grape"), N.filter(iter, STRING_NOT_EMPTY));
        assertEquals(Set.of("apple", "apricot"),
                N.filter((Iterable<String>) Arrays.asList("apple", "banana", "apricot", "grape"), s -> s.startsWith("a"), HashSet::new));
        assertTrue(N.filter((Iterable<String>) null, STRING_NOT_EMPTY).isEmpty());

        assertEquals(List.of("one", "two", "three"), N.filter(Arrays.asList("one", "two", "", "three").iterator(), STRING_NOT_EMPTY));
        assertEquals(Set.of("one", "two"), N.filter(Arrays.asList("one", "two", "three", "onetwo").iterator(), s -> s.length() == 3, HashSet::new));
        assertTrue(N.filter((Iterator<String>) null, STRING_NOT_EMPTY).isEmpty());
        assertTrue(N.filter(Collections.emptyIterator(), STRING_NOT_EMPTY).isEmpty());

        assertEquals(Arrays.asList(1, 3, 5), N.filter(nullContainingArray, i -> i != null));
        assertEquals(Arrays.asList(null, null), N.filter(nullContainingArray, i -> i == null));
        assertEquals(10, N.filter(largeIntArray, i -> i % 1000 == 0).length);
    }

    @Test
    public void testFilterAndMap() {
        assertEquals(Arrays.asList(5, 4, 4), N.filterAndMap(stringList, s -> s.length() > 3, String::length));
        assertEquals(new HashSet<>(Arrays.asList("even:2", "even:4")), N.filterAndMap(integerList, i -> i % 2 == 0, i -> "even:" + i, size -> new HashSet<>()));
        assertEquals(Arrays.asList(5, 7), N.filterAndMap(Arrays.asList("apple", "banana", "kiwi", "avocado"), s -> s.startsWith("a"), String::length));
        assertTrue(N.filterAndMap(null, (Predicate<String>) s -> true, Function.identity()).isEmpty());
        assertEquals(Set.of("APRICOT", "BANANA"),
                N.filterAndMap(Arrays.asList("apple", "apricot", "banana", "apricot"), s -> s.length() > 5, String::toUpperCase, HashSet::new));

        assertEquals(Arrays.asList('a', 'b', 'c', 'f', 'g', 'h', 'i'), N.filterAndFlatMap(Arrays.asList("abc", "de", "fghi"), s -> s.length() > 2,
                s -> s.chars().mapToObj(c -> (char) c).collect(Collectors.toList())));
        assertEquals(Arrays.asList("one", "two", "four", "five", "six"),
                N.filterAndFlatMap(Arrays.asList("one two", "three", "four five six", "seven"), s -> s.contains(" "), s -> Arrays.asList(s.split(" "))));
        assertEquals(Set.of("one", "two", "four", "five", "six"), N.filterAndFlatMap(Arrays.asList("one two one", "three", "four five six four", "seven"),
                s -> s.contains(" "), s -> Arrays.asList(s.split(" ")), HashSet::new));
        assertTrue(N.filterAndFlatMap(null, (Predicate<String>) s -> true, s -> Collections.emptyList()).isEmpty());
    }
}

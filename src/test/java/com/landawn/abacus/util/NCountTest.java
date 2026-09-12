package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.util.function.BooleanPredicate;
import com.landawn.abacus.util.function.BytePredicate;
import com.landawn.abacus.util.function.CharPredicate;
import com.landawn.abacus.util.function.DoublePredicate;
import com.landawn.abacus.util.function.FloatPredicate;
import com.landawn.abacus.util.function.LongPredicate;
import com.landawn.abacus.util.function.Predicate;
import com.landawn.abacus.util.function.ShortPredicate;

public class NCountTest extends NTestSupport {

    @Test
    public void testCount_arrays() {
        assertEquals(2, N.count(new boolean[] { true, false, true }, (BooleanPredicate) v -> v));
        assertEquals(2, N.count(new char[] { '1', '2', '3' }, (CharPredicate) v -> v > '1'));
        assertEquals(2, N.count(new byte[] { 1, 2, 3 }, (BytePredicate) v -> v > 1));
        assertEquals(2, N.count(new short[] { 1, 2, 3 }, (ShortPredicate) v -> v > 1));
        assertEquals(3, N.count(new int[] { 1, 2, 3, 4, 5, 6 }, IS_EVEN_INT));
        assertEquals(2, N.count(new long[] { 1, 2, 3 }, (LongPredicate) v -> v > 1));
        assertEquals(2, N.count(new float[] { 1.5f, 2.5f, 3.5f }, (FloatPredicate) f -> f < 3.0f));
        assertEquals(1, N.count(new double[] { 1.1, 2.2, 3.3 }, (DoublePredicate) d -> d > 3.0));
        assertEquals(3, N.count(stringArray, s -> s.length() > 3));
        assertEquals(0, N.count((boolean[]) null, b -> b));
        assertEquals(0, N.count((int[]) null, IS_EVEN_INT));
        assertEquals(0, N.count(new int[0], IS_EVEN_INT));
        assertEquals(N.count((String[]) null, Fn.isNull()), N.count((String[]) null, Fn.notNull()));

        assertEquals(1, N.count(new boolean[] { true, false, true, false }, 1, 3, b -> b));
        assertEquals(2, N.count(new char[] { 'a', 'b', 'c', 'd', 'e' }, 1, 4, c -> c < 'd'));
        assertEquals(2, N.count(new byte[] { 1, 2, 3, 4, 5 }, 1, 4, b -> b % 2 == 0));
        assertEquals(2, N.count(new short[] { 10, 20, 30, 40, 50 }, 1, 4, s -> s > 25));
        assertEquals(2, N.count(new int[] { 1, 2, 3, 4, 5, 6 }, 0, 4, IS_EVEN_INT));
        assertEquals(2, N.count(new long[] { 1L, 2L, 3L, 4L, 5L }, 1, 4, l -> l % 2 == 0));
        assertEquals(2, N.count(new float[] { 1.0f, 2.0f, 3.0f, 4.0f, 5.0f }, 1, 4, f -> f > 2.5f));
        assertEquals(2, N.count(new double[] { 1.0, 2.0, 3.0, 4.0, 5.0 }, 1, 4, d -> d > 2.5));
        assertEquals(2, N.count(new String[] { "apple", "banana", "cherry", "date", "elderberry" }, 1, 4, s -> s.length() > 5));
        assertEquals(0, N.count(new int[] { 1, 2, 3 }, 2, 2, i -> true));
        assertEquals(0, N.count((char[]) null, 0, 0, c -> true));
    }

    @Test
    public void testCount_iterableIterator() {
        assertEquals(5, N.count(Arrays.asList(1, 2, 3, 4, 5)));
        assertEquals(0, N.count((Iterable<?>) null));
        assertEquals(3, N.count((Iterable<Integer>) () -> Arrays.asList(10, 20, 30).iterator()));
        assertEquals(2, N.count(Arrays.asList("a", "", "c", ""), STRING_NOT_EMPTY));
        assertEquals(3, N.count(stringList, 0, 5, s -> s.length() > 3));
        assertEquals(2, N.count(CommonUtil.newLinkedList(stringList), 1, 4, s -> s.length() > 3));

        assertEquals(3, N.count(Arrays.asList(1, 2, 3).iterator()));
        assertEquals(0, N.count((Iterator<Integer>) null));
        assertEquals(0, N.count(Collections.emptyIterator()));
        assertEquals(2, N.count(Arrays.asList(1, 2, 3, 4, 5).iterator(), IS_EVEN_INTEGER));
        assertEquals(2, N.count(CommonUtil.asMap("a", 1, "b", 2, "c", 3).entrySet(),
                (Predicate<Map.Entry<String, Integer>>) entry -> entry.getKey().equals("a") || entry.getKey().equals("b")));
    }

    @Test
    public void testCountBy() {
        Map<Integer, Integer> byLength = N.countBy(stringList, String::length);
        assertEquals(Integer.valueOf(2), byLength.get(3));
        assertEquals(Integer.valueOf(1), byLength.get(5));
        assertEquals(Integer.valueOf(2), byLength.get(4));

        List<String> words = Arrays.asList("apple", "apricot", "banana", "apricot");
        assertEquals(3, N.countBy(words, s -> s.charAt(0)).get('a'));
        assertEquals(3, N.countBy(words, s -> s.charAt(0), TreeMap::new).get('a'));
        assertEquals(3, N.countBy(words.iterator(), s -> s.charAt(0)).get('a'));
        assertEquals(3, N.countBy(words.iterator(), s -> s.charAt(0), TreeMap::new).get('a'));
        assertTrue(N.countBy((List<String>) null, s -> s).isEmpty());
    }
}

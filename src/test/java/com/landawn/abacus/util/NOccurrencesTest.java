package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.util.function.Supplier;

public class NOccurrencesTest extends NTestSupport {

    @Test
    public void testOccurrencesOf_arrays() {
        assertEquals(3, N.frequency(new boolean[] { true, false, true, true, false }, true));
        assertEquals(2, N.frequency(new boolean[] { true, false, true, true, false }, false));
        assertEquals(0, N.frequency((boolean[]) null, true));
        assertEquals(0, N.frequency(EMPTY_BOOLEAN_ARRAY_CONST, true));
        assertEquals(3, N.frequency(new boolean[] { true, true, true }, true));

        assertEquals(3, N.frequency(new char[] { 'a', 'b', 'c', 'a', 'b', 'a' }, 'a'));
        assertEquals(0, N.frequency(new char[] { 'a', 'b' }, 'z'));
        assertEquals(0, N.frequency((char[]) null, 'a'));
        assertEquals(0, N.frequency(EMPTY_CHAR_ARRAY_CONST, 'a'));
        assertEquals(1, N.frequency(new char[] { 'a' }, 'a'));

        assertEquals(3, N.frequency(new byte[] { 1, 2, 3, 1, 2, 1 }, (byte) 1));
        assertEquals(0, N.frequency((byte[]) null, (byte) 1));
        assertEquals(3, N.frequency(new byte[] { -1, -2, -1, 0, -1 }, (byte) -1));

        assertEquals(3, N.frequency(new short[] { 10, 20, 30, 10, 20, 10 }, (short) 10));
        assertEquals(0, N.frequency((short[]) null, (short) 1));

        assertEquals(3, N.frequency(new int[] { 1, 2, 3, 2, 4, 2, 5 }, 2));
        assertEquals(0, N.frequency((int[]) null, 1));
        assertEquals(2, N.frequency(new int[] { 1000000, 2000000, 1000000 }, 1000000));

        assertEquals(3, N.frequency(new long[] { 1000L, 2000L, 3000L, 1000L, 2000L, 1000L }, 1000L));
        assertEquals(0, N.frequency((long[]) null, 1L));

        assertEquals(3, N.frequency(new float[] { 1.5f, 2.3f, 1.5f, 3.7f, 1.5f }, 1.5f));
        assertEquals(2, N.frequency(new float[] { 1.0f, Float.NaN, 2.0f, Float.NaN }, Float.NaN));
        assertEquals(0, N.frequency((float[]) null, 1.0f));
        assertEquals(2, N.frequency(new float[] { 0.0f, -0.0f, 0.0f }, 0.0f));
        assertEquals(1, N.frequency(new float[] { 0.0f, -0.0f, 0.0f }, -0.0f));

        assertEquals(3, N.frequency(new double[] { 1.5, 2.3, 1.5, 3.7, 1.5 }, 1.5));
        assertEquals(2, N.frequency(new double[] { 1.0, Double.NaN, 2.0, Double.NaN }, Double.NaN));
        assertEquals(0, N.frequency((double[]) null, 1.0));
        assertEquals(2, N.frequency(new double[] { Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY }, Double.POSITIVE_INFINITY));

        Object[] mixed = { 1, "hello", 1, 2.5, "hello", 1 };
        assertEquals(3, N.frequency(mixed, 1));
        assertEquals(2, N.frequency(mixed, "hello"));
        assertEquals(2, N.frequency(new Object[] { "a", null, "b", null, "c" }, null));
        assertEquals(0, N.frequency(new Object[] { "a" }, null));
        assertEquals(0, N.frequency((Object[]) null, "a"));

        Object[] nested = { new int[] { 1, 2 }, new int[] { 3 }, new int[] { 1, 2 } };
        assertEquals(0, N.frequency(nested, new int[] { 1, 2 }));
        assertEquals(1, N.frequency(nested, nested[0]));
    }

    @Test
    public void testOccurrencesOf_collectionIterableIterator() {
        List<String> words = Arrays.asList("hello", "world", "hello", "java");
        assertEquals(2, N.frequency(words, "hello"));
        assertEquals(0, N.frequency(words, "missing"));
        assertEquals(2, N.frequency(Arrays.asList("a", null, "b", null), null));
        assertEquals(0, N.frequency((Iterable<String>) null, "test"));
        assertEquals(0, N.frequency(Collections.emptyList(), "test"));

        Set<Integer> numbers = new HashSet<>(Arrays.asList(1, 2, 3, 4, 5));
        assertEquals(1, N.frequency(numbers, 3));
        assertEquals(0, N.frequency(numbers, 10));

        assertEquals(2, N.frequency(words.iterator(), "hello"));
        assertEquals(0, N.frequency((Iterator<String>) null, "test"));
        assertEquals(0, N.frequency(Collections.emptyIterator(), "test"));
    }

    @Test
    public void testOccurrencesOf_string() {
        assertEquals(3, N.frequency("hello world", 'l'));
        assertEquals(2, N.frequency("hello world", 'o'));
        assertEquals(0, N.frequency("hello world", 'z'));
        assertEquals(0, N.frequency((String) null, 'a'));
        assertEquals(0, N.frequency("", 'a'));
        assertEquals(3, N.frequency("banana", 'a'));

        assertEquals(3, N.frequency("hello hello world hello", "hello"));
        assertEquals(3, N.frequency("ababab", "ab"));
        assertEquals(0, N.frequency("abc", ""));
        assertEquals(0, N.frequency("", ""));
        assertEquals(0, N.frequency((String) null, "a"));
        assertEquals(1, N.frequency("banana", "ana"));
    }

    @Test
    public void testOccurrencesMap() {
        Map<String, Integer> counts = N.frequencyMap(new String[] { "apple", "banana", "apple", "cherry", "banana", "apple" });
        assertEquals(3, counts.size());
        assertEquals(3, counts.get("apple").intValue());
        assertEquals(2, counts.get("banana").intValue());
        assertEquals(1, counts.get("cherry").intValue());
        assertTrue(N.frequencyMap((String[]) null).isEmpty());
        assertTrue(N.frequencyMap(new String[] {}).isEmpty());

        Map<String, Integer> withNulls = N.frequencyMap(new String[] { "a", null, "b", null, "a" });
        assertEquals(2, withNulls.get("a").intValue());
        assertEquals(2, withNulls.get(null).intValue());

        Map<String, Integer> ordered = N.frequencyMap(new String[] { "c", "a", "b", "a", "c" }, LinkedHashMap::new);
        assertTrue(ordered instanceof LinkedHashMap);
        assertEquals(Arrays.asList("c", "a", "b"), new ArrayList<>(ordered.keySet()));

        Map<String, Integer> sorted = N.frequencyMap(new String[] { "c", "a", "b", "a", "c" }, TreeMap::new);
        assertTrue(sorted instanceof TreeMap);
        assertEquals(Arrays.asList("a", "b", "c"), new ArrayList<>(sorted.keySet()));

        List<String> words = Arrays.asList("apple", "banana", "apple", "cherry");
        Map<String, Integer> fromIterable = N.frequencyMap(words);
        assertEquals(2, fromIterable.get("apple").intValue());
        assertTrue(N.frequencyMap((Iterable<String>) null).isEmpty());
        assertTrue(N.frequencyMap(Collections.<String> emptyList()).isEmpty());

        Set<Integer> unique = new HashSet<>(Arrays.asList(1, 2, 3));
        Map<Integer, Integer> setCounts = N.frequencyMap(unique);
        assertEquals(1, setCounts.get(1).intValue());

        Map<String, Integer> fromIterator = N.frequencyMap(words.iterator());
        assertEquals(2, fromIterator.get("apple").intValue());
        assertTrue(N.frequencyMap((Iterator<String>) null).isEmpty());
        assertTrue(N.frequencyMap(Collections.<String> emptyIterator()).isEmpty());

        Supplier<Map<String, Integer>> concurrent = ConcurrentHashMap::new;
        Map<String, Integer> concurrentMap = N.frequencyMap(Arrays.asList("a", "b", "a").iterator(), concurrent);
        assertTrue(concurrentMap instanceof ConcurrentHashMap);
        assertEquals(2, concurrentMap.get("a").intValue());

        java.util.function.Supplier<Map<String, Integer>> ci = () -> new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        assertEquals(CommonUtil.asMap("a", 2), N.frequencyMap(new String[] { "a", "A" }, ci));
        assertEquals(CommonUtil.asMap("a", 1), N.frequencyMap(Arrays.asList("a", "A"), ci));
        assertEquals(N.frequencyMap(Arrays.asList("a", "A"), (java.util.function.Supplier<Map<String, Integer>>) LinkedHashMap::new),
                N.frequencyMap(new String[] { "a", "A" }, (java.util.function.Supplier<Map<String, Integer>>) LinkedHashMap::new));
    }

    @Test
    public void testFrequency_stringOccurrencesDoNotOverlap() {
        assertEquals(2, N.frequency("aaaa", "aa"));
        assertEquals(3, N.frequency("hello hello world hello", "hello"));
        assertEquals(0, N.frequency("aaa", (String) null));
        assertEquals(0, N.frequency("aaa", ""));
        assertEquals(0, N.frequency((String) null, "a"));
    }
}

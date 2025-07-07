package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.Fn.Factory;
import com.landawn.abacus.util.function.IntBiFunction;
import com.landawn.abacus.util.function.IntFunction;

// If N.java is in a different package, you would import pieces.N and call N.method().
// For this test, assuming N's methods are directly callable or via N.method().
// To make N's static methods directly callable, they need to be imported:
// import static your.package.name.N.*;

public class N200Test extends TestBase {

    // Helper for comparing collections of collections
    private <T> void assertListOfListsEquals(List<List<T>> expected, List<List<T>> actual) {
        if (expected == null) {
            assertNull(actual);
            return;
        }
        assertNotNull(actual);
        assertEquals(expected.size(), actual.size(), "Outer list sizes differ");
        for (int i = 0; i < expected.size(); i++) {
            assertIterableEquals(expected.get(i), actual.get(i), "Inner list at index " + i + " differs");
        }
    }

    // Helper for comparing list of arrays
    private <T> void assertListOfArraysEquals(List<T[]> expected, List<T[]> actual) {
        if (expected == null) {
            assertNull(actual);
            return;
        }
        assertNotNull(actual);
        assertEquals(expected.size(), actual.size(), "List sizes differ");
        for (int i = 0; i < expected.size(); i++) {
            assertArrayEquals(expected.get(i), actual.get(i), "Array at index " + i + " differs");
        }
    }

    // Primitive versions of assertListOfArraysEquals
    private void assertListOfBooleanArraysEquals(List<boolean[]> expected, List<boolean[]> actual) {
        assertEquals(expected.size(), actual.size());
        for (int i = 0; i < expected.size(); i++)
            assertArrayEquals(expected.get(i), actual.get(i));
    }

    private void assertListOfCharArraysEquals(List<char[]> expected, List<char[]> actual) {
        assertEquals(expected.size(), actual.size());
        for (int i = 0; i < expected.size(); i++)
            assertArrayEquals(expected.get(i), actual.get(i));
    }

    private void assertListOfByteArraysEquals(List<byte[]> expected, List<byte[]> actual) {
        assertEquals(expected.size(), actual.size());
        for (int i = 0; i < expected.size(); i++)
            assertArrayEquals(expected.get(i), actual.get(i));
    }

    private void assertListOfShortArraysEquals(List<short[]> expected, List<short[]> actual) {
        assertEquals(expected.size(), actual.size());
        for (int i = 0; i < expected.size(); i++)
            assertArrayEquals(expected.get(i), actual.get(i));
    }

    private void assertListOfIntArraysEquals(List<int[]> expected, List<int[]> actual) {
        assertEquals(expected.size(), actual.size());
        for (int i = 0; i < expected.size(); i++)
            assertArrayEquals(expected.get(i), actual.get(i));
    }

    private void assertListOfLongArraysEquals(List<long[]> expected, List<long[]> actual) {
        assertEquals(expected.size(), actual.size());
        for (int i = 0; i < expected.size(); i++)
            assertArrayEquals(expected.get(i), actual.get(i));
    }

    private void assertListOfFloatArraysEquals(List<float[]> expected, List<float[]> actual) {
        assertEquals(expected.size(), actual.size());
        for (int i = 0; i < expected.size(); i++)
            assertArrayEquals(expected.get(i), actual.get(i));
    }

    private void assertListOfDoubleArraysEquals(List<double[]> expected, List<double[]> actual) {
        assertEquals(expected.size(), actual.size());
        for (int i = 0; i < expected.size(); i++)
            assertArrayEquals(expected.get(i), actual.get(i));
    }

    // For methods returning Map<T, Integer> where order doesn't matter for keys
    private <T> void assertMapEquals(Map<T, Integer> expected, Map<T, Integer> actual) {
        if (expected == null) {
            assertNull(actual);
            return;
        }
        assertNotNull(actual);
        assertEquals(expected.size(), actual.size(), "Map sizes differ. Expected: " + expected + ", Actual: " + actual);
        for (Map.Entry<T, Integer> entry : expected.entrySet()) {
            assertTrue(actual.containsKey(entry.getKey()), "Actual map missing key: " + entry.getKey() + ". Actual map: " + actual);
            assertEquals(entry.getValue(), actual.get(entry.getKey()),
                    "Value for key " + entry.getKey() + " differs. Expected: " + entry.getValue() + ", Actual: " + actual.get(entry.getKey()));
        }
    }

    // Constants for use in tests
    private static final boolean[] EMPTY_BOOLEAN_ARRAY_CONST = {}; // Renamed to avoid conflict with N.EMPTY_BOOLEAN_ARRAY
    private static final char[] EMPTY_CHAR_ARRAY_CONST = {};
    private static final byte[] EMPTY_BYTE_ARRAY_CONST = {};
    private static final short[] EMPTY_SHORT_ARRAY_CONST = {};
    private static final int[] EMPTY_INT_ARRAY_CONST = {};
    private static final long[] EMPTY_LONG_ARRAY_CONST = {};
    private static final float[] EMPTY_FLOAT_ARRAY_CONST = {};
    private static final double[] EMPTY_DOUBLE_ARRAY_CONST = {};
    private static final Object[] EMPTY_OBJECT_ARRAY_CONST = {};

    // Helper to convert ObjIterator to List for easier assertion
    private <T> List<T> iteratorToList(Iterator<T> iterator) {
        List<T> list = new ArrayList<>();
        if (iterator != null) {
            iterator.forEachRemaining(list::add);
        }
        return list;
    }

    // INDEX_NOT_FOUND, assuming it's -1 as is common
    private static final int INDEX_NOT_FOUND = -1;

    // --- Test methods for occurrencesOf ---

    @Test
    public void testOccurrencesOf_booleanArray() {
        assertEquals(0, N.occurrencesOf((boolean[]) null, true));
        assertEquals(0, N.occurrencesOf(EMPTY_BOOLEAN_ARRAY_CONST, true));
        assertEquals(2, N.occurrencesOf(new boolean[] { true, false, true }, true));
        assertEquals(1, N.occurrencesOf(new boolean[] { true, false, true }, false));
        assertEquals(0, N.occurrencesOf(new boolean[] { false, false }, true));
        assertEquals(3, N.occurrencesOf(new boolean[] { true, true, true }, true));
    }

    @Test
    public void testOccurrencesOf_charArray() {
        assertEquals(0, N.occurrencesOf((char[]) null, 'a'));
        assertEquals(0, N.occurrencesOf(EMPTY_CHAR_ARRAY_CONST, 'a'));
        assertEquals(2, N.occurrencesOf(new char[] { 'a', 'b', 'a' }, 'a'));
        assertEquals(1, N.occurrencesOf(new char[] { 'a', 'b', 'c' }, 'b'));
        assertEquals(0, N.occurrencesOf(new char[] { 'b', 'c' }, 'a'));
    }

    @Test
    public void testOccurrencesOf_byteArray() {
        assertEquals(0, N.occurrencesOf((byte[]) null, (byte) 1));
        assertEquals(0, N.occurrencesOf(EMPTY_BYTE_ARRAY_CONST, (byte) 1));
        assertEquals(2, N.occurrencesOf(new byte[] { 1, 2, 1 }, (byte) 1));
        assertEquals(1, N.occurrencesOf(new byte[] { 1, 2, 3 }, (byte) 2));
        assertEquals(0, N.occurrencesOf(new byte[] { 2, 3 }, (byte) 1));
    }

    @Test
    public void testOccurrencesOf_shortArray() {
        assertEquals(0, N.occurrencesOf((short[]) null, (short) 1));
        assertEquals(0, N.occurrencesOf(EMPTY_SHORT_ARRAY_CONST, (short) 1));
        assertEquals(2, N.occurrencesOf(new short[] { 1, 2, 1 }, (short) 1));
        assertEquals(1, N.occurrencesOf(new short[] { 1, 2, 3 }, (short) 2));
        assertEquals(0, N.occurrencesOf(new short[] { 2, 3 }, (short) 1));
    }

    @Test
    public void testOccurrencesOf_intArray() {
        assertEquals(0, N.occurrencesOf((int[]) null, 1));
        assertEquals(0, N.occurrencesOf(EMPTY_INT_ARRAY_CONST, 1));
        assertEquals(2, N.occurrencesOf(new int[] { 1, 2, 1 }, 1));
        assertEquals(1, N.occurrencesOf(new int[] { 1, 2, 3 }, 2));
        assertEquals(0, N.occurrencesOf(new int[] { 2, 3 }, 1));
    }

    @Test
    public void testOccurrencesOf_longArray() {
        assertEquals(0, N.occurrencesOf((long[]) null, 1L));
        assertEquals(0, N.occurrencesOf(EMPTY_LONG_ARRAY_CONST, 1L));
        assertEquals(2, N.occurrencesOf(new long[] { 1L, 2L, 1L }, 1L));
        assertEquals(1, N.occurrencesOf(new long[] { 1L, 2L, 3L }, 2L));
        assertEquals(0, N.occurrencesOf(new long[] { 2L, 3L }, 1L));
    }

    @Test
    public void testOccurrencesOf_floatArray() {
        assertEquals(0, N.occurrencesOf((float[]) null, 1.0f));
        assertEquals(0, N.occurrencesOf(EMPTY_FLOAT_ARRAY_CONST, 1.0f));
        assertEquals(2, N.occurrencesOf(new float[] { 1.0f, 2.0f, 1.0f }, 1.0f));
        assertEquals(1, N.occurrencesOf(new float[] { 1.0f, 2.0f, 3.0f }, 2.0f));
        assertEquals(0, N.occurrencesOf(new float[] { 2.0f, 3.0f }, 1.0f));
        assertEquals(1, N.occurrencesOf(new float[] { Float.NaN, 1.0f }, Float.NaN)); // NaN comparison
    }

    @Test
    public void testOccurrencesOf_doubleArray() {
        assertEquals(0, N.occurrencesOf((double[]) null, 1.0));
        assertEquals(0, N.occurrencesOf(EMPTY_DOUBLE_ARRAY_CONST, 1.0));
        assertEquals(2, N.occurrencesOf(new double[] { 1.0, 2.0, 1.0 }, 1.0));
        assertEquals(1, N.occurrencesOf(new double[] { 1.0, 2.0, 3.0 }, 2.0));
        assertEquals(0, N.occurrencesOf(new double[] { 2.0, 3.0 }, 1.0));
        assertEquals(1, N.occurrencesOf(new double[] { Double.NaN, 1.0 }, Double.NaN)); // NaN comparison
    }

    @Test
    public void testOccurrencesOf_objectArray() {
        assertEquals(0, N.occurrencesOf((Object[]) null, "a"));
        assertEquals(0, N.occurrencesOf(EMPTY_OBJECT_ARRAY_CONST, "a"));
        assertEquals(2, N.occurrencesOf(new Object[] { "a", "b", "a" }, "a"));
        assertEquals(1, N.occurrencesOf(new Object[] { "a", "b", "c" }, "b"));
        assertEquals(0, N.occurrencesOf(new Object[] { "b", "c" }, "a"));
        assertEquals(2, N.occurrencesOf(new Object[] { "a", null, "a", null }, null));
        assertEquals(1, N.occurrencesOf(new Object[] { null }, null));
        assertEquals(0, N.occurrencesOf(new Object[] { "a" }, null));
    }

    @Test
    public void testOccurrencesOf_iterable() {
        assertEquals(0, N.occurrencesOf((Iterable<?>) null, "a"));
        assertEquals(0, N.occurrencesOf(Collections.emptyList(), "a"));
        assertEquals(2, N.occurrencesOf(Arrays.asList("a", "b", "a"), "a"));
        assertEquals(1, N.occurrencesOf(Arrays.asList("a", "b", "c"), "b"));
        assertEquals(0, N.occurrencesOf(Arrays.asList("b", "c"), "a"));
        assertEquals(2, N.occurrencesOf(Arrays.asList("a", null, "a", null), null));
        assertEquals(1, N.occurrencesOf(Collections.singletonList(null), null));
    }

    @Test
    public void testOccurrencesOf_iterator() {
        assertEquals(0, N.occurrencesOf((Iterator<?>) null, "a"));
        assertEquals(0, N.occurrencesOf(Collections.emptyIterator(), "a"));
        assertEquals(2, N.occurrencesOf(Arrays.asList("a", "b", "a").iterator(), "a"));
        assertEquals(1, N.occurrencesOf(Arrays.asList("a", "b", "c").iterator(), "b"));
        assertEquals(0, N.occurrencesOf(Arrays.asList("b", "c").iterator(), "a"));
        assertEquals(2, N.occurrencesOf(Arrays.asList("a", null, "a", null).iterator(), null));

        // Test for ArithmeticException - requires a very large iterator, hard to test directly without mocking Iterators.occurrencesOf
        // Assuming Iterators.occurrencesOf handles this correctly.
        // If we want to test N's handling of the result of Iterators.occurrencesOf:
        // We'd need to mock Iterators.occurrencesOf to return a long > Integer.MAX_VALUE
    }

    @Test
    public void testOccurrencesOf_stringChar() {
        assertEquals(0, N.occurrencesOf((String) null, 'a'));
        assertEquals(0, N.occurrencesOf("", 'a'));
        assertEquals(2, N.occurrencesOf("aba", 'a'));
        assertEquals(1, N.occurrencesOf("abc", 'b'));
        assertEquals(0, N.occurrencesOf("bc", 'a'));
    }

    @Test
    public void testOccurrencesOf_stringString() {
        assertEquals(0, N.occurrencesOf((String) null, "a"));
        assertEquals(0, N.occurrencesOf("", "a"));
        assertEquals(2, N.occurrencesOf("ababa", "ab")); // "aba" for "a"
        assertEquals(1, N.occurrencesOf("abcab", "bca")); // "abc" for "b"
        assertEquals(0, N.occurrencesOf("def", "abc")); // "bc" for "a"
        assertEquals(0, N.occurrencesOf("abc", "")); // N.Strings.countMatches might handle this, often returns 0 or throws
        // Behavior for empty valueToFind depends on Strings.countMatches. Assuming it returns 0 for empty valueToFind based on typical implementations.
        // Or if Strings.countMatches("", "") returns 1 and ("a", "") returns length + 1, adjust test.
        // Based on Apache Commons Lang StringUtils.countMatches, countMatches(str, "") returns 0.
        assertEquals(0, N.occurrencesOf("abc", ""));
        assertEquals(0, N.occurrencesOf("", ""));
    }

    // --- Test methods for occurrencesMap ---

    @Test
    public void testOccurrencesMap_array() {
        Map<String, Integer> expected = new HashMap<>();
        assertMapEquals(expected, N.occurrencesMap((String[]) null));
        assertMapEquals(expected, N.occurrencesMap(new String[] {}));

        expected.put("a", 2);
        expected.put("b", 1);
        assertMapEquals(expected, N.occurrencesMap(new String[] { "a", "b", "a" }));

        expected.clear();
        expected.put(null, 2);
        expected.put("a", 1);
        assertMapEquals(expected, N.occurrencesMap(new String[] { null, "a", null }));
    }

    @Test
    public void testOccurrencesMap_arrayWithSupplier() {
        Supplier<Map<String, Integer>> supplier = LinkedHashMap::new; // Test with a different map type

        Map<String, Integer> expected = new LinkedHashMap<>();
        assertMapEquals(expected, N.occurrencesMap((String[]) null, supplier));
        assertMapEquals(expected, N.occurrencesMap(new String[] {}, supplier));

        expected.put("a", 2);
        expected.put("b", 1);
        Map<String, Integer> actual = N.occurrencesMap(new String[] { "a", "b", "a" }, supplier);
        assertMapEquals(expected, actual);
        assertTrue(actual instanceof LinkedHashMap);

        expected.clear();
        expected.put(null, 2);
        expected.put("a", 1);
        actual = N.occurrencesMap(new String[] { null, "a", null }, supplier);
        assertMapEquals(expected, actual);
        assertTrue(actual instanceof LinkedHashMap);
    }

    @Test
    public void testOccurrencesMap_iterable() {
        Map<String, Integer> expected = new HashMap<>();
        assertMapEquals(expected, N.occurrencesMap((Iterable<String>) null));
        assertMapEquals(expected, N.occurrencesMap(Collections.<String> emptyList()));

        expected.put("a", 2);
        expected.put("b", 1);
        assertMapEquals(expected, N.occurrencesMap(Arrays.asList("a", "b", "a")));

        expected.clear();
        expected.put(null, 2);
        expected.put("a", 1);
        assertMapEquals(expected, N.occurrencesMap(Arrays.asList(null, "a", null)));
    }

    @Test
    public void testOccurrencesMap_iterableWithSupplier() {
        Supplier<Map<String, Integer>> supplier = TreeMap::new; // Test with a sorted map

        Map<String, Integer> expected = new TreeMap<>();
        assertMapEquals(expected, N.occurrencesMap((Iterable<String>) null, supplier));
        assertMapEquals(expected, N.occurrencesMap(Collections.<String> emptyList(), supplier));

        expected.put("a", 2);
        expected.put("b", 1);
        Map<String, Integer> actual = N.occurrencesMap(Arrays.asList("a", "b", "a"), supplier);
        assertMapEquals(expected, actual);
        assertTrue(actual instanceof TreeMap);

        expected.clear();
        // TreeMap with null key will throw NullPointerException if comparator doesn't handle it.
        // Using a supplier that can handle nulls or testing non-null elements here.
        // Let's test with non-null elements for TreeMap or ensure supplier handles nulls.
        // N.occurrencesMap uses a Multiset internally which typically handles nulls.
        // The final map.put(e, multiset.getCount(e)) would be subject to the map's behavior.
        // Default HashMap supplier handles nulls. TreeMap does not by default.
        // For simplicity, let's assume supplier can handle nulls if tested with nulls.
        // Or, we can test with a map known to handle null keys with TreeMap by providing a null-safe comparator.
        // For now, let's use a HashMap supplier for the null test.
        Supplier<Map<String, Integer>> hashMapSupplier = HashMap::new;
        Map<String, Integer> expectedNull = new HashMap<>();
        expectedNull.put(null, 2);
        expectedNull.put("a", 1);
        Map<String, Integer> actualNull = N.occurrencesMap(Arrays.asList(null, "a", null), hashMapSupplier);
        assertMapEquals(expectedNull, actualNull);
        assertTrue(actualNull instanceof HashMap);
    }

    @Test
    public void testOccurrencesMap_iterator() {
        Map<String, Integer> expected = new HashMap<>();
        assertMapEquals(expected, N.occurrencesMap((Iterator<String>) null));
        assertMapEquals(expected, N.occurrencesMap(Collections.<String> emptyIterator()));

        expected.put("a", 2);
        expected.put("b", 1);
        assertMapEquals(expected, N.occurrencesMap(Arrays.asList("a", "b", "a").iterator()));

        expected.clear();
        expected.put(null, 2);
        expected.put("a", 1);
        assertMapEquals(expected, N.occurrencesMap(Arrays.asList(null, "a", null).iterator()));
    }

    @Test
    public void testOccurrencesMap_iteratorWithSupplier() {
        Supplier<Map<String, Integer>> supplier = ConcurrentHashMap::new; // Test with a concurrent map

        Map<String, Integer> expected = new ConcurrentHashMap<>();
        assertMapEquals(expected, N.occurrencesMap((Iterator<String>) null, supplier));
        assertMapEquals(expected, N.occurrencesMap(Collections.<String> emptyIterator(), supplier));

        expected.put("a", 2);
        expected.put("b", 1);
        Map<String, Integer> actual = N.occurrencesMap(Arrays.asList("a", "b", "a").iterator(), supplier);
        assertMapEquals(expected, actual);
        assertTrue(actual instanceof ConcurrentHashMap);
    }

    // --- Test methods for contains ---

    @Test
    public void testContains_booleanArray() {
        assertFalse(N.contains((boolean[]) null, true));
        assertFalse(N.contains(EMPTY_BOOLEAN_ARRAY_CONST, true));
        assertTrue(N.contains(new boolean[] { true, false }, true));
        assertFalse(N.contains(new boolean[] { false, false }, true));
    }

    @Test
    public void testContains_charArray() {
        assertFalse(N.contains((char[]) null, 'a'));
        assertFalse(N.contains(EMPTY_CHAR_ARRAY_CONST, 'a'));
        assertTrue(N.contains(new char[] { 'a', 'b' }, 'a'));
        assertFalse(N.contains(new char[] { 'b', 'c' }, 'a'));
    }

    @Test
    public void testContains_byteArray() {
        assertFalse(N.contains((byte[]) null, (byte) 1));
        assertFalse(N.contains(EMPTY_BYTE_ARRAY_CONST, (byte) 1));
        assertTrue(N.contains(new byte[] { 1, 2 }, (byte) 1));
        assertFalse(N.contains(new byte[] { 2, 3 }, (byte) 1));
    }

    @Test
    public void testContains_shortArray() {
        assertFalse(N.contains((short[]) null, (short) 1));
        assertFalse(N.contains(EMPTY_SHORT_ARRAY_CONST, (short) 1));
        assertTrue(N.contains(new short[] { 1, 2 }, (short) 1));
        assertFalse(N.contains(new short[] { 2, 3 }, (short) 1));
    }

    @Test
    public void testContains_intArray() {
        assertFalse(N.contains((int[]) null, 1));
        assertFalse(N.contains(EMPTY_INT_ARRAY_CONST, 1));
        assertTrue(N.contains(new int[] { 1, 2 }, 1));
        assertFalse(N.contains(new int[] { 2, 3 }, 1));
    }

    @Test
    public void testContains_longArray() {
        assertFalse(N.contains((long[]) null, 1L));
        assertFalse(N.contains(EMPTY_LONG_ARRAY_CONST, 1L));
        assertTrue(N.contains(new long[] { 1L, 2L }, 1L));
        assertFalse(N.contains(new long[] { 2L, 3L }, 1L));
    }

    @Test
    public void testContains_floatArray() {
        assertFalse(N.contains((float[]) null, 1.0f));
        assertFalse(N.contains(EMPTY_FLOAT_ARRAY_CONST, 1.0f));
        assertTrue(N.contains(new float[] { 1.0f, 2.0f }, 1.0f));
        assertFalse(N.contains(new float[] { 2.0f, 3.0f }, 1.0f));
        assertTrue(N.contains(new float[] { Float.NaN, 1.0f }, Float.NaN));
    }

    @Test
    public void testContains_doubleArray() {
        assertFalse(N.contains((double[]) null, 1.0));
        assertFalse(N.contains(EMPTY_DOUBLE_ARRAY_CONST, 1.0));
        assertTrue(N.contains(new double[] { 1.0, 2.0 }, 1.0));
        assertFalse(N.contains(new double[] { 2.0, 3.0 }, 1.0));
        assertTrue(N.contains(new double[] { Double.NaN, 1.0 }, Double.NaN));
    }

    @Test
    public void testContains_objectArray() {
        assertFalse(N.contains((Object[]) null, "a"));
        assertFalse(N.contains(EMPTY_OBJECT_ARRAY_CONST, "a"));
        assertTrue(N.contains(new Object[] { "a", "b" }, "a"));
        assertFalse(N.contains(new Object[] { "b", "c" }, "a"));
        assertTrue(N.contains(new Object[] { "a", null }, null));
        assertFalse(N.contains(new Object[] { "a" }, null));
    }

    @Test
    public void testContains_collection() {
        assertFalse(N.contains((Collection<?>) null, "a"));
        assertFalse(N.contains(Collections.emptyList(), "a"));
        assertTrue(N.contains(Arrays.asList("a", "b"), "a"));
        assertFalse(N.contains(Arrays.asList("b", "c"), "a"));
        assertTrue(N.contains(Arrays.asList("a", null), null));
        assertFalse(N.contains(Collections.singletonList("a"), null));
    }

    @Test
    public void testContains_iterable() {
        // Iterable specific test, not just delegating to Collection.contains
        Iterable<String> nullIterable = null;
        assertFalse(N.contains(nullIterable, "a"));
        assertFalse(N.contains(Collections.<String> emptyIterator(), "a")); // Empty iterable
        assertTrue(N.contains(Arrays.asList("a", "b").iterator(), "a"));
        assertFalse(N.contains(Arrays.asList("b", "c").iterator(), "a"));
        assertTrue(N.contains(Arrays.asList("a", null).iterator(), null));
        assertFalse(N.contains(Collections.singleton("a").iterator(), null));
    }

    @Test
    public void testContains_iterator() {
        assertFalse(N.contains((Iterator<?>) null, "a"));
        assertFalse(N.contains(Collections.emptyIterator(), "a"));
        assertTrue(N.contains(Arrays.asList("a", "b").iterator(), "a"));
        assertFalse(N.contains(Arrays.asList("b", "c").iterator(), "a"));
        assertTrue(N.contains(Arrays.asList("a", null).iterator(), null));
        assertFalse(N.contains(Collections.singleton("a").iterator(), null));
    }

    // --- Test methods for containsAll ---

    @Test
    public void testContainsAll_collectionCollection() {
        Collection<String> main = Arrays.asList("a", "b", "c");
        assertTrue(N.containsAll(main, Arrays.asList("a", "b")));
        assertTrue(N.containsAll(main, Collections.emptyList())); // valuesToFind is empty
        assertTrue(N.containsAll(main, (Collection<?>) null)); // valuesToFind is null
        assertFalse(N.containsAll(main, Arrays.asList("a", "d")));
        assertFalse(N.containsAll(Collections.emptyList(), Arrays.asList("a"))); // main is empty
        assertFalse(N.containsAll((Iterator<String>) null, Arrays.asList("a"))); // main is null
        assertTrue(N.containsAll(Arrays.asList("a", "a", "b"), Arrays.asList("a", "a")));
        assertTrue(N.containsAll(Arrays.asList("a", "b"), Arrays.asList("a", "a")));
    }

    @Test
    public void testContainsAll_collectionVarargs() {
        Collection<String> main = Arrays.asList("a", "b", "c");
        assertTrue(N.containsAll(main, "a", "b"));
        assertTrue(N.containsAll(main)); // valuesToFind is empty varargs
        assertTrue(N.containsAll(main, (Object[]) null)); // valuesToFind is null array
        assertFalse(N.containsAll(main, "a", "d"));
        assertFalse(N.containsAll(Collections.emptyList(), "a"));
        assertFalse(N.containsAll(null, "a"));
        assertTrue(N.containsAll(Arrays.asList("a", "a", "b"), "a", "a"));
        assertTrue(N.containsAll(Arrays.asList("a", "b"), "a", "a"));
    }

    @Test
    public void testContainsAll_iterableCollection() {
        Iterable<String> mainIter = Arrays.asList("a", "b", "c");
        assertTrue(N.containsAll(mainIter, Arrays.asList("a", "b")));
        assertTrue(N.containsAll(mainIter, Collections.emptyList()));
        assertTrue(N.containsAll(mainIter, (Collection<?>) null)); // valuesToFind is null
        assertFalse(N.containsAll(mainIter, Arrays.asList("a", "d")));

        Iterable<String> emptyIter = Collections::emptyIterator;
        assertFalse(N.containsAll(emptyIter, Arrays.asList("a")));

        Iterable<String> nullIter = null;
        assertFalse(N.containsAll(nullIter, Arrays.asList("a")));

        // Test with HashSet for valuesToFind for efficiency
        Iterable<String> mainWithDupes = Arrays.asList("a", "a", "b", "c");
        assertTrue(N.containsAll(mainWithDupes, new HashSet<>(Arrays.asList("a", "b"))));
        assertTrue(N.containsAll(mainWithDupes, new HashSet<>(Arrays.asList("a")))); // Test remove from set
        assertTrue(N.containsAll(mainWithDupes, new HashSet<>(Arrays.asList("a", "c", "b"))));
        assertFalse(N.containsAll(mainWithDupes, new HashSet<>(Arrays.asList("a", "d"))));
    }

    @Test
    public void testContainsAll_iteratorCollection() {
        assertTrue(N.containsAll(Arrays.asList("a", "b", "c").iterator(), Arrays.asList("a", "b")));
        assertTrue(N.containsAll(Arrays.asList("a", "b", "c").iterator(), Collections.emptyList()));
        assertTrue(N.containsAll(Arrays.asList("a", "b", "c").iterator(), (Collection<?>) null));

        assertFalse(N.containsAll(Arrays.asList("a", "b", "c").iterator(), Arrays.asList("a", "d")));
        assertFalse(N.containsAll(Collections.emptyIterator(), Arrays.asList("a")));
        assertFalse(N.containsAll((Iterator<String>) null, Arrays.asList("a")));

        // Iterator is consumed, so re-create for each test if needed
        Iterator<String> mainWithDupesIter = Arrays.asList("a", "a", "b", "c").iterator();
        assertTrue(N.containsAll(mainWithDupesIter, new HashSet<>(Arrays.asList("a", "b"))));

        mainWithDupesIter = Arrays.asList("a", "a", "b", "c").iterator(); // re-init
        assertTrue(N.containsAll(mainWithDupesIter, new HashSet<>(Arrays.asList("a", "c", "b"))));

        mainWithDupesIter = Arrays.asList("a", "a", "b", "c").iterator(); // re-init
        assertFalse(N.containsAll(mainWithDupesIter, new HashSet<>(Arrays.asList("a", "d"))));
    }

    // --- Test methods for containsAny ---
    @Test
    public void testContainsAny_collectionCollection() {
        Collection<String> main = Arrays.asList("a", "b", "c");
        assertTrue(N.containsAny(main, Arrays.asList("c", "d")));
        assertFalse(N.containsAny(main, Arrays.asList("d", "e")));
        assertFalse(N.containsAny(main, Collections.emptyList()));
        assertFalse(N.containsAny(main, (Collection<?>) null));
        assertFalse(N.containsAny(Collections.emptyList(), Arrays.asList("a")));
        assertFalse(N.containsAny(null, Arrays.asList("a")));
    }

    @Test
    public void testContainsAny_collectionVarargs() {
        Collection<String> main = Arrays.asList("a", "b", "c");
        assertTrue(N.containsAny(main, "c", "d"));
        assertFalse(N.containsAny(main, "d", "e"));
        assertFalse(N.containsAny(main)); // empty varargs
        assertFalse(N.containsAny(main, (Object[]) null)); // null varargs array
        assertFalse(N.containsAny(Collections.emptyList(), "a"));
        assertFalse(N.containsAny(null, "a"));
    }

    @Test
    public void testContainsAny_iterableSet() {
        Iterable<String> mainIter = Arrays.asList("a", "b", "c");
        assertTrue(N.containsAny(mainIter, new HashSet<>(Arrays.asList("c", "d"))));
        assertFalse(N.containsAny(mainIter, new HashSet<>(Arrays.asList("d", "e"))));
        assertFalse(N.containsAny(mainIter, Collections.emptySet()));
        assertFalse(N.containsAny(mainIter, (Set<?>) null));

        Iterable<String> emptyIter = Collections::emptyIterator;
        assertFalse(N.containsAny(emptyIter, new HashSet<>(Arrays.asList("a"))));

        Iterable<String> nullIter = null;
        assertFalse(N.containsAny(nullIter, new HashSet<>(Arrays.asList("a"))));
    }

    @Test
    public void testContainsAny_iteratorSet() {
        assertTrue(N.containsAny(Arrays.asList("a", "b", "c").iterator(), new HashSet<>(Arrays.asList("c", "d"))));
        assertFalse(N.containsAny(Arrays.asList("a", "b", "c").iterator(), new HashSet<>(Arrays.asList("d", "e")))); // Iterator consumed
        assertFalse(N.containsAny(Arrays.asList("a", "b", "c").iterator(), Collections.emptySet()));
        assertFalse(N.containsAny(Arrays.asList("a", "b", "c").iterator(), (Set<?>) null));
        assertFalse(N.containsAny(Collections.emptyIterator(), new HashSet<>(Arrays.asList("a"))));
        assertFalse(N.containsAny((Iterator<String>) null, new HashSet<>(Arrays.asList("a"))));
    }

    // --- Test methods for containsNone ---
    @Test
    public void testContainsNone_collectionCollection() {
        Collection<String> main = Arrays.asList("a", "b", "c");
        assertFalse(N.containsNone(main, Arrays.asList("c", "d")));
        assertTrue(N.containsNone(main, Arrays.asList("d", "e")));
        assertTrue(N.containsNone(main, Collections.emptyList()));
        assertTrue(N.containsNone(main, (Collection<?>) null));
        assertTrue(N.containsNone(Collections.emptyList(), Arrays.asList("a")));
        assertTrue(N.containsNone(null, Arrays.asList("a")));
    }

    @Test
    public void testContainsNone_collectionVarargs() {
        Collection<String> main = Arrays.asList("a", "b", "c");
        assertFalse(N.containsNone(main, "c", "d"));
        assertTrue(N.containsNone(main, "d", "e"));
        assertTrue(N.containsNone(main)); // empty varargs
        assertTrue(N.containsNone(main, (Object[]) null)); // null varargs array
        assertTrue(N.containsNone(Collections.emptyList(), "a"));
        assertTrue(N.containsNone(null, "a"));
    }

    @Test
    public void testContainsNone_iterableSet() {
        Iterable<String> mainIter = Arrays.asList("a", "b", "c");
        assertFalse(N.containsNone(mainIter, new HashSet<>(Arrays.asList("c", "d"))));
        assertTrue(N.containsNone(mainIter, new HashSet<>(Arrays.asList("d", "e"))));
        assertTrue(N.containsNone(mainIter, Collections.emptySet()));
        assertTrue(N.containsNone(mainIter, (Set<?>) null));

        Iterable<String> emptyIter = Collections::emptyIterator;
        assertTrue(N.containsNone(emptyIter, new HashSet<>(Arrays.asList("a"))));

        Iterable<String> nullIter = null;
        assertTrue(N.containsNone(nullIter, new HashSet<>(Arrays.asList("a"))));
    }

    @Test
    public void testContainsNone_iteratorSet() {
        assertFalse(N.containsNone(Arrays.asList("a", "b", "c").iterator(), new HashSet<>(Arrays.asList("c", "d"))));
        assertTrue(N.containsNone(Arrays.asList("a", "b", "c").iterator(), new HashSet<>(Arrays.asList("d", "e")))); // Iterator consumed
        assertTrue(N.containsNone(Arrays.asList("a", "b", "c").iterator(), Collections.emptySet()));
        assertTrue(N.containsNone(Arrays.asList("a", "b", "c").iterator(), (Set<?>) null));
        assertTrue(N.containsNone(Collections.emptyIterator(), new HashSet<>(Arrays.asList("a"))));
        assertTrue(N.containsNone((Iterator<String>) null, new HashSet<>(Arrays.asList("a"))));
    }

    // --- Test methods for slice ---

    @Test
    public void testSlice_array() {
        String[] arr = { "a", "b", "c", "d" };
        assertIterableEquals(Arrays.asList("b", "c"), N.slice(arr, 1, 3));
        assertIterableEquals(Arrays.asList("a", "b", "c", "d"), N.slice(arr, 0, 4));
        assertTrue(N.slice(arr, 1, 1).isEmpty());
        assertTrue(N.slice(new String[] {}, 0, 0).isEmpty());
        assertTrue(N.slice((String[]) null, 0, 0).isEmpty()); // N.len(null) is 0, checkFromToIndex(0,0,0) is fine.

        assertThrows(IndexOutOfBoundsException.class, () -> N.slice(arr, -1, 2));
        assertThrows(IndexOutOfBoundsException.class, () -> N.slice(arr, 0, 5));
        assertThrows(IndexOutOfBoundsException.class, () -> N.slice(arr, 3, 1));
    }

    @Test
    public void testSlice_list() {
        List<String> list = Arrays.asList("a", "b", "c", "d");
        assertIterableEquals(Arrays.asList("b", "c"), N.slice(list, 1, 3));
        assertIterableEquals(Arrays.asList("a", "b", "c", "d"), N.slice(list, 0, 4)); // whole list
        assertTrue(N.slice(list, 1, 1).isEmpty());
        assertTrue(N.slice(Collections.<String> emptyList(), 0, 0).isEmpty());
        assertTrue(N.slice((List<String>) null, 0, 0).isEmpty());

        assertThrows(IndexOutOfBoundsException.class, () -> N.slice(list, -1, 2));
        assertThrows(IndexOutOfBoundsException.class, () -> N.slice(list, 0, 5));
        assertThrows(IndexOutOfBoundsException.class, () -> N.slice(list, 3, 1));
    }

    @Test
    public void testSlice_collection() {
        Collection<String> coll = new LinkedHashSet<>(Arrays.asList("a", "b", "c", "d")); // Use collection that's not a list
        // Slice for generic collection might not preserve original order if underlying iterator doesn't.
        // Assuming Slice internal implementation iterates and collects for non-List.
        // The test below assumes the iteration order of LinkedHashSet.
        assertIterableEquals(Arrays.asList("b", "c"), N.slice(coll, 1, 3));
        assertIterableEquals(Arrays.asList("a", "b", "c", "d"), N.slice(coll, 0, 4));
        assertTrue(N.slice(coll, 1, 1).isEmpty());
        assertTrue(N.slice(Collections.<String> emptySet(), 0, 0).isEmpty());
        assertTrue(N.slice((Collection<String>) null, 0, 0).isEmpty());

        assertThrows(IndexOutOfBoundsException.class, () -> N.slice(coll, -1, 2));
        assertThrows(IndexOutOfBoundsException.class, () -> N.slice(coll, 0, 5));
        assertThrows(IndexOutOfBoundsException.class, () -> N.slice(coll, 3, 1));

        // Test with a List, should delegate to list slice
        List<String> list = Arrays.asList("a", "b", "c", "d");
        assertTrue(N.slice((Collection<String>) list, 1, 3) instanceof ImmutableList);
        assertIterableEquals(Arrays.asList("b", "c"), N.slice((Collection<String>) list, 1, 3));
    }

    @Test
    public void testSlice_iterator() {
        assertIterableEquals(Arrays.asList("b", "c"), iteratorToList(N.slice(Arrays.asList("a", "b", "c", "d").iterator(), 1, 3)));
        assertIterableEquals(Arrays.asList("a", "b", "c", "d"), iteratorToList(N.slice(Arrays.asList("a", "b", "c", "d").iterator(), 0, 4)));
        assertTrue(iteratorToList(N.slice(Arrays.asList("a", "b", "c", "d").iterator(), 1, 1)).isEmpty()); // fromIndex == toIndex
        assertTrue(iteratorToList(N.slice(Collections.<String> emptyIterator(), 0, 0)).isEmpty());
        assertTrue(iteratorToList(N.slice((Iterator<String>) null, 0, 0)).isEmpty()); // iter is null

        // Iterator is consumed. Re-initialize for each sub-test if the same base data is used.
        assertIterableEquals(Collections.emptyList(), iteratorToList(N.slice(Arrays.asList("a", "b").iterator(), 0, 0)));
        assertIterableEquals(Arrays.asList("a"), iteratorToList(N.slice(Arrays.asList("a", "b").iterator(), 0, 1)));
        assertIterableEquals(Arrays.asList("b"), iteratorToList(N.slice(Arrays.asList("a", "b").iterator(), 1, 2))); // "a" is skipped
        assertIterableEquals(Collections.emptyList(), iteratorToList(N.slice(Arrays.asList("a", "b").iterator(), 2, 2))); // skip all

        assertThrows(IllegalArgumentException.class, () -> N.slice(Arrays.asList("a").iterator(), -1, 0));
        assertThrows(IllegalArgumentException.class, () -> N.slice(Arrays.asList("a").iterator(), 1, 0)); // fromIndex > toIndex
    }

    // --- Test methods for split ---

    // boolean[]
    @Test
    public void testSplit_booleanArray_chunkSize() {
        boolean[] arr = { true, false, true, false, true };
        List<boolean[]> expected = Arrays.asList(new boolean[] { true, false }, new boolean[] { true, false }, new boolean[] { true });
        assertListOfBooleanArraysEquals(expected, N.split(arr, 2));

        assertListOfBooleanArraysEquals(Collections.emptyList(), N.split((boolean[]) null, 2));
        assertListOfBooleanArraysEquals(Collections.emptyList(), N.split(EMPTY_BOOLEAN_ARRAY_CONST, 2));

        List<boolean[]> singleChunks = Arrays.asList(new boolean[] { true }, new boolean[] { false }, new boolean[] { true }, new boolean[] { false },
                new boolean[] { true });
        assertListOfBooleanArraysEquals(singleChunks, N.split(arr, 1));

        List<boolean[]> largeChunk = Collections.singletonList(arr.clone());
        assertListOfBooleanArraysEquals(largeChunk, N.split(arr, 5));
        assertListOfBooleanArraysEquals(largeChunk, N.split(arr, 10));

        assertThrows(IllegalArgumentException.class, () -> N.split(arr, 0));
        assertThrows(IllegalArgumentException.class, () -> N.split(arr, -1));
    }

    @Test
    public void testSplit_booleanArray_fromIndex_toIndex_chunkSize() {
        boolean[] arr = { true, false, true, false, true, false }; // len 6
        // split(arr, 1, 5, 2) -> {false, true}, {false, true} from subarray {false, true, false, true}
        List<boolean[]> expected = Arrays.asList(new boolean[] { false, true }, new boolean[] { false, true });
        assertListOfBooleanArraysEquals(expected, N.split(arr, 1, 5, 2));

        assertListOfBooleanArraysEquals(Collections.emptyList(), N.split(arr, 1, 1, 2)); // fromIndex == toIndex
        assertListOfBooleanArraysEquals(Collections.emptyList(), N.split((boolean[]) null, 0, 0, 1));
        assertListOfBooleanArraysEquals(Collections.emptyList(), N.split(EMPTY_BOOLEAN_ARRAY_CONST, 0, 0, 1));

        assertThrows(IndexOutOfBoundsException.class, () -> N.split(arr, -1, 3, 2));
        assertThrows(IndexOutOfBoundsException.class, () -> N.split(arr, 0, 7, 2));
        assertThrows(IndexOutOfBoundsException.class, () -> N.split(arr, 4, 2, 2));
        assertThrows(IllegalArgumentException.class, () -> N.split(arr, 1, 5, 0));
    }

    // char[] (similar pattern for byte, short, int, long, float, double, T[])
    @Test
    public void testSplit_charArray_chunkSize() {
        char[] arr = { 'a', 'b', 'c', 'd', 'e' };
        List<char[]> expected = Arrays.asList(new char[] { 'a', 'b' }, new char[] { 'c', 'd' }, new char[] { 'e' });
        assertListOfCharArraysEquals(expected, N.split(arr, 2));
        assertListOfCharArraysEquals(Collections.emptyList(), N.split((char[]) null, 2));
    }

    @Test
    public void testSplit_charArray_fromIndex_toIndex_chunkSize() {
        char[] arr = { 'a', 'b', 'c', 'd', 'e', 'f' };
        List<char[]> expected = Arrays.asList(new char[] { 'b', 'c' }, new char[] { 'd', 'e' });
        assertListOfCharArraysEquals(expected, N.split(arr, 1, 5, 2));
        assertThrows(IllegalArgumentException.class, () -> N.split(arr, 1, 5, 0));
    }

    // byte[]
    @Test
    public void testSplit_byteArray_chunkSize() {
        byte[] arr = { 1, 2, 3, 4, 5 };
        List<byte[]> expected = Arrays.asList(new byte[] { 1, 2 }, new byte[] { 3, 4 }, new byte[] { 5 });
        assertListOfByteArraysEquals(expected, N.split(arr, 2));
        assertListOfByteArraysEquals(Collections.emptyList(), N.split((byte[]) null, 2));
    }

    @Test
    public void testSplit_byteArray_fromIndex_toIndex_chunkSize() {
        byte[] arr = { 1, 2, 3, 4, 5, 6 };
        List<byte[]> expected = Arrays.asList(new byte[] { 2, 3 }, new byte[] { 4, 5 });
        assertListOfByteArraysEquals(expected, N.split(arr, 1, 5, 2));
        assertThrows(IllegalArgumentException.class, () -> N.split(arr, 1, 5, 0));
    }

    // short[]
    @Test
    public void testSplit_shortArray_chunkSize() {
        short[] arr = { 1, 2, 3, 4, 5 };
        List<short[]> expected = Arrays.asList(new short[] { 1, 2 }, new short[] { 3, 4 }, new short[] { 5 });
        assertListOfShortArraysEquals(expected, N.split(arr, 2));
        assertListOfShortArraysEquals(Collections.emptyList(), N.split((short[]) null, 2));
    }

    @Test
    public void testSplit_shortArray_fromIndex_toIndex_chunkSize() {
        short[] arr = { 1, 2, 3, 4, 5, 6 };
        List<short[]> expected = Arrays.asList(new short[] { 2, 3 }, new short[] { 4, 5 });
        assertListOfShortArraysEquals(expected, N.split(arr, 1, 5, 2));
        assertThrows(IllegalArgumentException.class, () -> N.split(arr, 1, 5, 0));
    }

    // int[]
    @Test
    public void testSplit_intArray_chunkSize() {
        int[] arr = { 1, 2, 3, 4, 5 };
        List<int[]> expected = Arrays.asList(new int[] { 1, 2 }, new int[] { 3, 4 }, new int[] { 5 });
        assertListOfIntArraysEquals(expected, N.split(arr, 2));
        assertListOfIntArraysEquals(Collections.emptyList(), N.split((int[]) null, 2));
    }

    @Test
    public void testSplit_intArray_fromIndex_toIndex_chunkSize() {
        int[] arr = { 1, 2, 3, 4, 5, 6 };
        List<int[]> expected = Arrays.asList(new int[] { 2, 3 }, new int[] { 4, 5 });
        assertListOfIntArraysEquals(expected, N.split(arr, 1, 5, 2));
        assertThrows(IllegalArgumentException.class, () -> N.split(arr, 1, 5, 0));
    }

    // long[]
    @Test
    public void testSplit_longArray_chunkSize() {
        long[] arr = { 1L, 2L, 3L, 4L, 5L };
        List<long[]> expected = Arrays.asList(new long[] { 1L, 2L }, new long[] { 3L, 4L }, new long[] { 5L });
        assertListOfLongArraysEquals(expected, N.split(arr, 2));
        assertListOfLongArraysEquals(Collections.emptyList(), N.split((long[]) null, 2));
    }

    @Test
    public void testSplit_longArray_fromIndex_toIndex_chunkSize() {
        long[] arr = { 1L, 2L, 3L, 4L, 5L, 6L };
        List<long[]> expected = Arrays.asList(new long[] { 2L, 3L }, new long[] { 4L, 5L });
        assertListOfLongArraysEquals(expected, N.split(arr, 1, 5, 2));
        assertThrows(IllegalArgumentException.class, () -> N.split(arr, 1, 5, 0));
    }

    // float[]
    @Test
    public void testSplit_floatArray_chunkSize() {
        float[] arr = { 1f, 2f, 3f, 4f, 5f };
        List<float[]> expected = Arrays.asList(new float[] { 1f, 2f }, new float[] { 3f, 4f }, new float[] { 5f });
        assertListOfFloatArraysEquals(expected, N.split(arr, 2));
        assertListOfFloatArraysEquals(Collections.emptyList(), N.split((float[]) null, 2));
    }

    @Test
    public void testSplit_floatArray_fromIndex_toIndex_chunkSize() {
        float[] arr = { 1f, 2f, 3f, 4f, 5f, 6f };
        List<float[]> expected = Arrays.asList(new float[] { 2f, 3f }, new float[] { 4f, 5f });
        assertListOfFloatArraysEquals(expected, N.split(arr, 1, 5, 2));
        assertThrows(IllegalArgumentException.class, () -> N.split(arr, 1, 5, 0));
    }

    // double[]
    @Test
    public void testSplit_doubleArray_chunkSize() {
        double[] arr = { 1.0, 2.0, 3.0, 4.0, 5.0 };
        List<double[]> expected = Arrays.asList(new double[] { 1.0, 2.0 }, new double[] { 3.0, 4.0 }, new double[] { 5.0 });
        assertListOfDoubleArraysEquals(expected, N.split(arr, 2));
        assertListOfDoubleArraysEquals(Collections.emptyList(), N.split((double[]) null, 2));
    }

    @Test
    public void testSplit_doubleArray_fromIndex_toIndex_chunkSize() {
        double[] arr = { 1.0, 2.0, 3.0, 4.0, 5.0, 6.0 };
        List<double[]> expected = Arrays.asList(new double[] { 2.0, 3.0 }, new double[] { 4.0, 5.0 });
        assertListOfDoubleArraysEquals(expected, N.split(arr, 1, 5, 2));
        assertThrows(IllegalArgumentException.class, () -> N.split(arr, 1, 5, 0));
    }

    // T[]
    @Test
    public void testSplit_objectArray_chunkSize() {
        String[] arr = { "a", "b", "c", "d", "e" };
        List<String[]> expected = Arrays.asList(new String[] { "a", "b" }, new String[] { "c", "d" }, new String[] { "e" });
        assertListOfArraysEquals(expected, N.split(arr, 2));
        assertListOfArraysEquals(Collections.emptyList(), N.split((String[]) null, 2));
    }

    @Test
    public void testSplit_objectArray_fromIndex_toIndex_chunkSize() {
        String[] arr = { "a", "b", "c", "d", "e", "f" };
        List<String[]> expected = Arrays.asList(new String[] { "b", "c" }, new String[] { "d", "e" });
        assertListOfArraysEquals(expected, N.split(arr, 1, 5, 2));
        assertThrows(IllegalArgumentException.class, () -> N.split(arr, 1, 5, 0));
    }

    // Collection<T>
    @Test
    public void testSplit_collection_chunkSize() {
        Collection<String> coll = Arrays.asList("a", "b", "c", "d", "e");
        List<List<String>> expected = Arrays.asList(Arrays.asList("a", "b"), Arrays.asList("c", "d"), Collections.singletonList("e"));
        List<List<String>> actual = N.split(coll, 2);
        assertListOfListsEquals(expected, actual);

        assertTrue(N.split((Collection<String>) null, 2).isEmpty());
        assertTrue(N.split(Collections.<String> emptyList(), 2).isEmpty());
        assertThrows(IllegalArgumentException.class, () -> N.split(coll, 0));
    }

    @Test
    public void testSplit_collection_fromIndex_toIndex_chunkSize() {
        List<String> list = Arrays.asList("a", "b", "c", "d", "e", "f"); // Use List for subList behavior
        List<List<String>> expected = Arrays.asList(list.subList(1, 3), list.subList(3, 5)); // {b,c}, {d,e}
        List<List<String>> actual = N.split(list, 1, 5, 2);
        assertListOfListsEquals(expected, actual);

        // Test with non-List collection
        Collection<String> coll = new LinkedHashSet<>(Arrays.asList("a", "b", "c", "d", "e", "f"));
        // Expected for non-list from 1 to 5 (b,c,d,e) with chunk 2 => (b,c), (d,e)
        List<List<String>> expectedNonList = Arrays.asList(Arrays.asList("b", "c"), Arrays.asList("d", "e"));
        List<List<String>> actualNonList = N.split(coll, 1, 5, 2);
        assertListOfListsEquals(expectedNonList, actualNonList);

        assertTrue(N.split(list, 1, 1, 2).isEmpty()); // fromIndex == toIndex
        assertTrue(N.split((Collection<String>) null, 0, 0, 1).isEmpty());

        assertThrows(IndexOutOfBoundsException.class, () -> N.split(list, -1, 3, 2));
        assertThrows(IllegalArgumentException.class, () -> N.split(list, 1, 5, 0));
    }

    // Iterable<T>
    @Test
    public void testSplit_iterable_chunkSize() {
        Iterable<String> iter = Arrays.asList("a", "b", "c", "d", "e");
        List<List<String>> expected = Arrays.asList(Arrays.asList("a", "b"), Arrays.asList("c", "d"), Collections.singletonList("e"));
        assertListOfListsEquals(expected, N.split(iter, 2));

        assertTrue(N.split((Iterable<String>) null, 2).isEmpty());
        assertTrue(N.split(Collections::emptyIterator, 2).isEmpty());
        assertThrows(IllegalArgumentException.class, () -> N.split(iter, 0));
    }

    // Iterator<T>
    @Test
    public void testSplit_iterator_chunkSize() {
        ObjIterator<List<String>> objIter = N.split(Arrays.asList("a", "b", "c", "d", "e").iterator(), 2);
        List<List<String>> actual = iteratorToList(objIter);
        List<List<String>> expected = Arrays.asList(Arrays.asList("a", "b"), Arrays.asList("c", "d"), Collections.singletonList("e"));
        assertListOfListsEquals(expected, actual);

        assertTrue(iteratorToList(N.split((Iterator<String>) null, 2)).isEmpty());
        assertTrue(iteratorToList(N.split(Collections.<String> emptyIterator(), 2)).isEmpty());
        assertThrows(IllegalArgumentException.class, () -> N.split(Arrays.asList("a").iterator(), 0));
    }

    // CharSequence
    @Test
    public void testSplit_charSequence_chunkSize() {
        CharSequence str = "abcde";
        List<String> expected = Arrays.asList("ab", "cd", "e");
        assertEquals(expected, N.split(str, 2));

        assertTrue(N.split((CharSequence) null, 2).isEmpty());
        assertTrue(N.split("", 2).isEmpty());
        assertThrows(IllegalArgumentException.class, () -> N.split(str, 0));
    }

    @Test
    public void testSplit_charSequence_fromIndex_toIndex_chunkSize() {
        CharSequence str = "abcdef";
        // Subsequence from 1 to 5 is "bcde"
        List<String> expected = Arrays.asList("bc", "de");
        assertEquals(expected, N.split(str, 1, 5, 2));

        assertTrue(N.split(str, 1, 1, 2).isEmpty());
        assertTrue(N.split((CharSequence) null, 0, 0, 1).isEmpty());
        assertTrue(N.split("", 0, 0, 1).isEmpty());

        assertThrows(IndexOutOfBoundsException.class, () -> N.split(str, -1, 3, 2));
        assertThrows(IllegalArgumentException.class, () -> N.split(str, 1, 5, 0));
    }

    // --- Test methods for splitByChunkCount ---

    @Test
    public void testSplitByChunkCount_totalSizeFunc() {
        IntBiFunction<int[]> copyRangeFunc = (from, to) -> Arrays.copyOfRange(new int[] { 1, 2, 3, 4, 5, 6, 7 }, from, to);

        // sizeSmallerFirst = false (default, larger chunks first)
        List<int[]> result1 = N.splitByChunkCount(7, 5, copyRangeFunc);
        // Expected: [[1,2], [3,4], [5], [6], [7]] (total 7, 5 chunks. 7/5 = 1 remainder 2. So two chunks of size 2, three of size 1)
        List<int[]> expected1 = Arrays.asList(new int[] { 1, 2 }, new int[] { 3, 4 }, new int[] { 5 }, new int[] { 6 }, new int[] { 7 });
        assertListOfIntArraysEquals(expected1, result1);

        // sizeSmallerFirst = true
        List<int[]> result2 = N.splitByChunkCount(7, 5, true, copyRangeFunc);
        // Expected: [[1], [2], [3], [4,5], [6,7]]
        List<int[]> expected2 = Arrays.asList(new int[] { 1 }, new int[] { 2 }, new int[] { 3 }, new int[] { 4, 5 }, new int[] { 6, 7 });
        assertListOfIntArraysEquals(expected2, result2);

        assertEquals(0, N.splitByChunkCount(0, 5, copyRangeFunc).size());

        // totalSize < maxChunkCount
        List<int[]> result3 = N.splitByChunkCount(3, 5, copyRangeFunc); // [[1],[2],[3]]
        List<int[]> expected3 = Arrays.asList(new int[] { 1 }, new int[] { 2 }, new int[] { 3 });
        assertListOfIntArraysEquals(expected3, result3);

        assertThrows(IllegalArgumentException.class, () -> N.splitByChunkCount(-1, 5, copyRangeFunc));
        assertThrows(IllegalArgumentException.class, () -> N.splitByChunkCount(5, 0, copyRangeFunc));
    }

    @Test
    public void testSplitByChunkCount_collection() {
        List<Integer> coll = Arrays.asList(1, 2, 3, 4, 5, 6, 7);

        // sizeSmallerFirst = false (default)
        List<List<Integer>> result1 = N.splitByChunkCount(coll, 5);
        List<List<Integer>> expected1 = Arrays.asList(Arrays.asList(1, 2), Arrays.asList(3, 4), Arrays.asList(5), Arrays.asList(6), Arrays.asList(7));
        assertListOfListsEquals(expected1, result1);

        // sizeSmallerFirst = true
        List<List<Integer>> result2 = N.splitByChunkCount(coll, 5, true);
        List<List<Integer>> expected2 = Arrays.asList(Arrays.asList(1), Arrays.asList(2), Arrays.asList(3), Arrays.asList(4, 5), Arrays.asList(6, 7));
        assertListOfListsEquals(expected2, result2);

        assertTrue(N.splitByChunkCount((Collection<Integer>) null, 5).isEmpty());
        assertTrue(N.splitByChunkCount(Collections.<Integer> emptyList(), 5).isEmpty());

        // totalSize < maxChunkCount
        List<Integer> smallColl = Arrays.asList(1, 2, 3);
        List<List<Integer>> result3 = N.splitByChunkCount(smallColl, 5);
        List<List<Integer>> expected3 = Arrays.asList(Arrays.asList(1), Arrays.asList(2), Arrays.asList(3));
        assertListOfListsEquals(expected3, result3);

        assertThrows(IllegalArgumentException.class, () -> N.splitByChunkCount(coll, 0));

        // Test with non-List Collection
        Collection<Integer> setColl = new LinkedHashSet<>(coll); // Keep order for predictable test
        List<List<Integer>> resultSet = N.splitByChunkCount(setColl, 5);
        assertListOfListsEquals(expected1, resultSet); // Should behave same as list if iteration order is same
    }

    // --- Test methods for concat --- (boolean[], char[], byte[], short[], int[], long[], float[], double[], T[])
    // boolean[]
    @Test
    public void testConcat_booleanArrays() {
        assertArrayEquals(new boolean[] { true, false, true, true }, N.concat(new boolean[] { true, false }, new boolean[] { true, true }));
        assertArrayEquals(new boolean[] { true, false }, N.concat(new boolean[] { true, false }, null));
        assertArrayEquals(new boolean[] { true, false }, N.concat(new boolean[] { true, false }, EMPTY_BOOLEAN_ARRAY_CONST));
        assertArrayEquals(new boolean[] { true, true }, N.concat(null, new boolean[] { true, true }));
        assertArrayEquals(new boolean[] { true, true }, N.concat(EMPTY_BOOLEAN_ARRAY_CONST, new boolean[] { true, true }));
        assertArrayEquals(EMPTY_BOOLEAN_ARRAY_CONST, N.concat((boolean[]) null, (boolean[]) null));
        assertArrayEquals(EMPTY_BOOLEAN_ARRAY_CONST, N.concat(EMPTY_BOOLEAN_ARRAY_CONST, EMPTY_BOOLEAN_ARRAY_CONST));
    }

    @Test
    public void testConcat_booleanArraysVarargs() {
        assertArrayEquals(new boolean[] { true, false, true, true, false },
                N.concat(new boolean[] { true, false }, new boolean[] { true, true }, new boolean[] { false }));
        assertArrayEquals(new boolean[] { true, false }, N.concat(new boolean[] { true, false }));
        assertArrayEquals(EMPTY_BOOLEAN_ARRAY_CONST, N.concat((boolean[][]) null)); // N.concat((boolean[][])null) in N.java, should return EMPTY_BOOLEAN_ARRAY if N.isEmpty(aa) is true
        assertArrayEquals(EMPTY_BOOLEAN_ARRAY_CONST, N.concat(new boolean[0][0])); // Empty varargs
        assertArrayEquals(EMPTY_BOOLEAN_ARRAY_CONST, N.concat(new boolean[][] { null, null }));
        assertArrayEquals(new boolean[] { true }, N.concat(null, new boolean[] { true }, null, EMPTY_BOOLEAN_ARRAY_CONST));
    }

    // char[]
    @Test
    public void testConcat_charArrays() {
        assertArrayEquals(new char[] { 'a', 'b', 'c', 'd' }, N.concat(new char[] { 'a', 'b' }, new char[] { 'c', 'd' }));
        assertArrayEquals(new char[] { 'a', 'b' }, N.concat(new char[] { 'a', 'b' }, null));
        assertArrayEquals(EMPTY_CHAR_ARRAY_CONST, N.concat((char[]) null, (char[]) null));
    }

    @Test
    public void testConcat_charArraysVarargs() {
        assertArrayEquals(new char[] { 'a', 'b', 'c', 'd', 'e' }, N.concat(new char[] { 'a', 'b' }, new char[] { 'c', 'd' }, new char[] { 'e' }));
        assertArrayEquals(EMPTY_CHAR_ARRAY_CONST, N.concat((char[][]) null));
    }

    // byte[]
    @Test
    public void testConcat_byteArrays() {
        assertArrayEquals(new byte[] { 1, 2, 3, 4 }, N.concat(new byte[] { 1, 2 }, new byte[] { 3, 4 }));
        assertArrayEquals(new byte[] { 1, 2 }, N.concat(new byte[] { 1, 2 }, null));
        assertArrayEquals(EMPTY_BYTE_ARRAY_CONST, N.concat((byte[]) null, (byte[]) null));
    }

    @Test
    public void testConcat_byteArraysVarargs() {
        assertArrayEquals(new byte[] { 1, 2, 3, 4, 5 }, N.concat(new byte[] { 1, 2 }, new byte[] { 3, 4 }, new byte[] { 5 }));
        assertArrayEquals(EMPTY_BYTE_ARRAY_CONST, N.concat((byte[][]) null));
    }

    // short[]
    @Test
    public void testConcat_shortArrays() {
        assertArrayEquals(new short[] { 1, 2, 3, 4 }, N.concat(new short[] { 1, 2 }, new short[] { 3, 4 }));
        assertArrayEquals(new short[] { 1, 2 }, N.concat(new short[] { 1, 2 }, null));
        assertArrayEquals(EMPTY_SHORT_ARRAY_CONST, N.concat((short[]) null, (short[]) null));
    }

    @Test
    public void testConcat_shortArraysVarargs() {
        assertArrayEquals(new short[] { 1, 2, 3, 4, 5 }, N.concat(new short[] { 1, 2 }, new short[] { 3, 4 }, new short[] { 5 }));
        assertArrayEquals(EMPTY_SHORT_ARRAY_CONST, N.concat((short[][]) null));
    }

    // int[]
    @Test
    public void testConcat_intArrays() {
        assertArrayEquals(new int[] { 1, 2, 3, 4 }, N.concat(new int[] { 1, 2 }, new int[] { 3, 4 }));
        assertArrayEquals(new int[] { 1, 2 }, N.concat(new int[] { 1, 2 }, null));
        assertArrayEquals(EMPTY_INT_ARRAY_CONST, N.concat((int[]) null, (int[]) null));
    }

    @Test
    public void testConcat_intArraysVarargs() {
        assertArrayEquals(new int[] { 1, 2, 3, 4, 5 }, N.concat(new int[] { 1, 2 }, new int[] { 3, 4 }, new int[] { 5 }));
        assertArrayEquals(EMPTY_INT_ARRAY_CONST, N.concat((int[][]) null));
    }

    // long[]
    @Test
    public void testConcat_longArrays() {
        assertArrayEquals(new long[] { 1, 2, 3, 4 }, N.concat(new long[] { 1, 2 }, new long[] { 3, 4 }));
        assertArrayEquals(new long[] { 1, 2 }, N.concat(new long[] { 1, 2 }, null));
        assertArrayEquals(EMPTY_LONG_ARRAY_CONST, N.concat((long[]) null, (long[]) null));
    }

    @Test
    public void testConcat_longArraysVarargs() {
        assertArrayEquals(new long[] { 1, 2, 3, 4, 5 }, N.concat(new long[] { 1, 2 }, new long[] { 3, 4 }, new long[] { 5 }));
        assertArrayEquals(EMPTY_LONG_ARRAY_CONST, N.concat((long[][]) null));
    }

    // float[]
    @Test
    public void testConcat_floatArrays() {
        assertArrayEquals(new float[] { 1, 2, 3, 4 }, N.concat(new float[] { 1, 2 }, new float[] { 3, 4 }));
        assertArrayEquals(new float[] { 1, 2 }, N.concat(new float[] { 1, 2 }, null));
        assertArrayEquals(EMPTY_FLOAT_ARRAY_CONST, N.concat((float[]) null, (float[]) null));
    }

    @Test
    public void testConcat_floatArraysVarargs() {
        assertArrayEquals(new float[] { 1, 2, 3, 4, 5 }, N.concat(new float[] { 1, 2 }, new float[] { 3, 4 }, new float[] { 5 }));
        assertArrayEquals(EMPTY_FLOAT_ARRAY_CONST, N.concat((float[][]) null));
    }

    // double[]
    @Test
    public void testConcat_doubleArrays() {
        assertArrayEquals(new double[] { 1, 2, 3, 4 }, N.concat(new double[] { 1, 2 }, new double[] { 3, 4 }));
        assertArrayEquals(new double[] { 1, 2 }, N.concat(new double[] { 1, 2 }, null));
        assertArrayEquals(EMPTY_DOUBLE_ARRAY_CONST, N.concat((double[]) null, (double[]) null));
    }

    @Test
    public void testConcat_doubleArraysVarargs() {
        assertArrayEquals(new double[] { 1, 2, 3, 4, 5 }, N.concat(new double[] { 1, 2 }, new double[] { 3, 4 }, new double[] { 5 }));
        assertArrayEquals(EMPTY_DOUBLE_ARRAY_CONST, N.concat((double[][]) null));
    }

    // T[]
    @Test
    public void testConcat_objectArrays() {
        assertArrayEquals(new String[] { "a", "b", "c", "d" }, N.concat(new String[] { "a", "b" }, new String[] { "c", "d" }));
        assertArrayEquals(new String[] { "a", "b" }, N.concat(new String[] { "a", "b" }, null)); // returns clone of a
        assertArrayEquals(new String[] { "c", "d" }, N.concat(null, new String[] { "c", "d" })); // returns clone of b
        assertNull(N.concat((String[]) null, (String[]) null)); // returns a which is null
        assertArrayEquals(EMPTY_OBJECT_ARRAY_CONST, N.concat(new String[] {}, new String[] {})); // returns a which is empty
    }

    @Test
    public void testConcat_objectArraysVarargs() {
        assertArrayEquals(new String[] { "a", "b", "c", "d", "e" }, N.concat(new String[] { "a", "b" }, new String[] { "c", "d" }, new String[] { "e" }));
        assertNull(N.concat((String[][]) null)); // varargs array is null
        assertArrayEquals(new String[0], N.concat(new String[0][0])); // empty varargs
        assertArrayEquals(new String[] { "a" }, N.concat(new String[] { "a" }));
        assertArrayEquals(new String[] { "a" }, N.concat(null, new String[] { "a" }, null));
        assertArrayEquals(new String[0], N.concat(new String[][] { null, null }));
        // Test correct component type for empty varargs from T[]...
        String[][] emptyVarargs = new String[0][0];
        assertEquals(0, N.concat(emptyVarargs).length);
        assertTrue(N.concat(emptyVarargs) instanceof String[]);
    }

    // Iterable<T>
    @Test
    public void testConcat_iterables() {
        Iterable<String> iterA = Arrays.asList("a", "b");
        Iterable<String> iterB = Arrays.asList("c", "d");
        List<String> expected = Arrays.asList("a", "b", "c", "d");
        assertEquals(expected, N.concat(iterA, iterB));

        assertEquals(Arrays.asList("a", "b"), N.concat(iterA, null));
        assertEquals(Arrays.asList("c", "d"), N.concat(null, iterB));
        assertEquals(Collections.emptyList(), N.concat((Iterable<String>) null, (Iterable<String>) null));
        assertEquals(Collections.emptyList(), N.concat(Collections.emptyList(), Collections.emptyList()));
    }

    @Test
    @SuppressWarnings("unchecked") // For varargs with generic types
    public void testConcat_iterablesVarargs() {
        Iterable<String> iterA = Arrays.asList("a", "b");
        Iterable<String> iterB = Arrays.asList("c", "d");
        Iterable<String> iterC = Arrays.asList("e");
        List<String> expected = Arrays.asList("a", "b", "c", "d", "e");
        assertEquals(expected, N.concat(iterA, iterB, iterC));

        assertEquals(Arrays.asList("a", "b"), N.concat(iterA));
        assertEquals(Collections.emptyList(), N.concat((Iterable<String>[]) null));
        assertEquals(Collections.emptyList(), N.concat(new Iterable[0])); // empty varargs
        assertEquals(Arrays.asList("a", "b"), N.concat(null, iterA, null, Collections.emptyList()));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testConcat_collectionOfIterables() {
        Iterable<String> iterA = Arrays.asList("a", "b");
        Iterable<String> iterB = Arrays.asList("c", "d");
        Collection<Iterable<String>> collOfIters = Arrays.asList(iterA, null, iterB, Collections.emptyList());
        List<String> expected = Arrays.asList("a", "b", "c", "d");
        assertEquals(expected, N.concat(collOfIters));

        assertTrue(N.concat((Collection<Iterable<String>>) null).isEmpty());
        assertTrue(N.concat(Collections.<Iterable<String>> emptyList()).isEmpty());
        assertEquals(Arrays.asList("a", "b"), N.concat(Collections.singletonList(iterA)));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testConcat_collectionOfIterablesWithSupplier() {
        Iterable<String> iterA = Arrays.asList("a", "b");
        Iterable<String> iterB = Arrays.asList("c", "d");
        Collection<Iterable<String>> collOfIters = Arrays.asList(iterA, iterB);

        IntFunction<LinkedList<String>> supplier = Factory.ofLinkedList();
        LinkedList<String> result = N.concat(collOfIters, supplier);

        assertEquals(Arrays.asList("a", "b", "c", "d"), result);
        assertTrue(result instanceof LinkedList);

        assertTrue(N.concat((Collection<Iterable<String>>) null, supplier).isEmpty());
    }

    // Iterator<T>
    @Test
    public void testConcat_iterators() {
        Iterator<String> iterA = Arrays.asList("a", "b").iterator();
        Iterator<String> iterB = Arrays.asList("c", "d").iterator();
        List<String> expected = Arrays.asList("a", "b", "c", "d");
        assertEquals(expected, iteratorToList(N.concat(iterA, iterB)));

        assertEquals(Arrays.asList("a", "b"), iteratorToList(N.concat(Arrays.asList("a", "b").iterator(), null)));
        assertEquals(Arrays.asList("c", "d"), iteratorToList(N.concat(null, Arrays.asList("c", "d").iterator())));
        assertEquals(Collections.emptyList(), iteratorToList(N.concat((Iterator<String>) null, (Iterator<String>) null)));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testConcat_iteratorsVarargs() {
        Iterator<String> iterA = Arrays.asList("a", "b").iterator();
        Iterator<String> iterB = Arrays.asList("c", "d").iterator();
        Iterator<String> iterC = Arrays.asList("e").iterator();
        List<String> expected = Arrays.asList("a", "b", "c", "d", "e");
        assertEquals(expected, iteratorToList(N.concat(iterA, iterB, iterC)));

        assertEquals(Arrays.asList("a", "b"), iteratorToList(N.concat(Arrays.asList("a", "b").iterator())));
        assertTrue(iteratorToList(N.concat((Iterator<String>[]) null)).isEmpty());
        assertTrue(iteratorToList(N.concat(new Iterator[0])).isEmpty());
    }

    // --- Test methods for flatten ---
    // boolean[][]
    @Test
    public void testFlatten_boolean2DArray() {
        boolean[][] arr = { { true, false }, { true }, {}, null, { false, false } };
        assertArrayEquals(new boolean[] { true, false, true, false, false }, N.flatten(arr));
        assertArrayEquals(EMPTY_BOOLEAN_ARRAY_CONST, N.flatten((boolean[][]) null));
        assertArrayEquals(EMPTY_BOOLEAN_ARRAY_CONST, N.flatten(new boolean[][] {}));
        assertArrayEquals(EMPTY_BOOLEAN_ARRAY_CONST, N.flatten(new boolean[][] { null, null }));
        assertArrayEquals(EMPTY_BOOLEAN_ARRAY_CONST, N.flatten(new boolean[][] { {}, {} }));
    }

    // char[][] (similar pattern for byte, short, int, long, float, double)
    @Test
    public void testFlatten_char2DArray() {
        char[][] arr = { { 'a', 'b' }, { 'c' }, {}, null, { 'd', 'e' } };
        assertArrayEquals(new char[] { 'a', 'b', 'c', 'd', 'e' }, N.flatten(arr));
        assertArrayEquals(EMPTY_CHAR_ARRAY_CONST, N.flatten((char[][]) null));
    }

    // byte[][]
    @Test
    public void testFlatten_byte2DArray() {
        byte[][] arr = { { 1, 2 }, { 3 }, {}, null, { 4, 5 } };
        assertArrayEquals(new byte[] { 1, 2, 3, 4, 5 }, N.flatten(arr));
        assertArrayEquals(EMPTY_BYTE_ARRAY_CONST, N.flatten((byte[][]) null));
    }

    // short[][]
    @Test
    public void testFlatten_short2DArray() {
        short[][] arr = { { 1, 2 }, { 3 }, {}, null, { 4, 5 } };
        assertArrayEquals(new short[] { 1, 2, 3, 4, 5 }, N.flatten(arr));
        assertArrayEquals(EMPTY_SHORT_ARRAY_CONST, N.flatten((short[][]) null));
    }

    // int[][]
    @Test
    public void testFlatten_int2DArray() {
        int[][] arr = { { 1, 2 }, { 3 }, {}, null, { 4, 5 } };
        assertArrayEquals(new int[] { 1, 2, 3, 4, 5 }, N.flatten(arr));
        assertArrayEquals(EMPTY_INT_ARRAY_CONST, N.flatten((int[][]) null));
    }

    // long[][]
    @Test
    public void testFlatten_long2DArray() {
        long[][] arr = { { 1L, 2L }, { 3L }, {}, null, { 4L, 5L } };
        assertArrayEquals(new long[] { 1L, 2L, 3L, 4L, 5L }, N.flatten(arr));
        assertArrayEquals(EMPTY_LONG_ARRAY_CONST, N.flatten((long[][]) null));
    }

    // float[][]
    @Test
    public void testFlatten_float2DArray() {
        float[][] arr = { { 1f, 2f }, { 3f }, {}, null, { 4f, 5f } };
        assertArrayEquals(new float[] { 1f, 2f, 3f, 4f, 5f }, N.flatten(arr));
        assertArrayEquals(EMPTY_FLOAT_ARRAY_CONST, N.flatten((float[][]) null));
    }

    // double[][]
    @Test
    public void testFlatten_double2DArray() {
        double[][] arr = { { 1.0, 2.0 }, { 3.0 }, {}, null, { 4.0, 5.0 } };
        assertArrayEquals(new double[] { 1.0, 2.0, 3.0, 4.0, 5.0 }, N.flatten(arr));
        assertArrayEquals(EMPTY_DOUBLE_ARRAY_CONST, N.flatten((double[][]) null));
    }

    // T[][]
    @Test
    public void testFlatten_object2DArray() {
        String[][] arr = { { "a", "b" }, { "c" }, {}, null, { "d", "e" } };
        assertArrayEquals(new String[] { "a", "b", "c", "d", "e" }, N.flatten(arr));
        assertNull(N.flatten((String[][]) null)); // Method is @MayReturnNull
        assertArrayEquals(new String[0], N.flatten(new String[][] {}));
    }

    @Test
    public void testFlatten_object2DArrayWithComponentType() {
        String[][] arr = { { "a", "b" }, { "c" }, {}, null, { "d", "e" } };
        assertArrayEquals(new String[] { "a", "b", "c", "d", "e" }, N.flatten(arr, String.class));
        assertArrayEquals(new String[0], N.flatten((String[][]) null, String.class)); // Returns empty array if input is null
        assertArrayEquals(new String[0], N.flatten(new String[][] {}, String.class));
    }

    // Iterable<Iterable<T>>
    @Test
    public void testFlatten_iterableOfIterables() {
        Iterable<Iterable<String>> iterOfIters = Arrays.asList(Arrays.asList("a", "b"), Collections.singletonList("c"), Collections.emptyList(), null, // null inner iterable
                Arrays.asList("d", "e"));
        List<String> expected = Arrays.asList("a", "b", "c", "d", "e");
        assertEquals(expected, N.flatten(iterOfIters));

        assertTrue(N.flatten((Iterable<Iterable<String>>) null).isEmpty());
        assertTrue(N.flatten(Collections.<Iterable<String>> emptyList()).isEmpty());
        assertTrue(N.flatten(Arrays.asList(null, null)).isEmpty()); // All inner iterables are null
    }

    @Test
    public void testFlatten_iterableOfIterablesWithSupplier() {
        Iterable<Iterable<String>> iterOfIters = Arrays.asList(Arrays.asList("a", "b"), Arrays.asList("c"));
        IntFunction<Set<String>> supplier = LinkedHashSet::new; // Keep order for test

        Set<String> result = N.flatten(iterOfIters, supplier);
        assertEquals(new LinkedHashSet<>(Arrays.asList("a", "b", "c")), result);
        assertTrue(result instanceof LinkedHashSet);

        assertTrue(N.flatten((Iterable<Iterable<String>>) null, supplier).isEmpty());
    }

    // Iterator<Iterator<T>>
    @Test
    public void testFlatten_iteratorOfIterators() {
        List<Iterator<String>> listOfIters = Arrays.asList(Arrays.asList("a", "b").iterator(), Collections.singletonList("c").iterator(),
                Collections.<String> emptyIterator(), null, // null inner iterator
                Arrays.asList("d", "e").iterator());
        Iterator<Iterator<String>> iterOfIters = listOfIters.iterator();
        List<String> expected = Arrays.asList("a", "b", "c", "d", "e");
        assertEquals(expected, iteratorToList(N.flatten(iterOfIters)));

        assertTrue(iteratorToList(N.flatten((Iterator<Iterator<String>>) null)).isEmpty());
        assertTrue(iteratorToList(N.flatten(Collections.<Iterator<String>> emptyIterator())).isEmpty());
    }

    // --- Test methods for flattenEachElement ---
    @Test
    @SuppressWarnings("unchecked")
    public void testFlattenEachElement() {
        Iterable<?> mixed = Arrays.asList("a", Arrays.asList("b", "c"), "d", Collections.singletonList("e"));
        List<?> expected = Arrays.asList("a", "b", "c", "d", "e");
        assertEquals(expected, N.flattenEachElement(mixed));

        Iterable<?> deeplyNested = Arrays.asList("a", Arrays.asList("b", Arrays.asList("c1", "c2")), "d");
        List<?> expectedDeep = Arrays.asList("a", "b", "c1", "c2", "d");
        assertEquals(expectedDeep, N.flattenEachElement(deeplyNested));

        assertTrue(N.flattenEachElement(null).isEmpty());
        assertTrue(N.flattenEachElement(Collections.emptyList()).isEmpty());
        assertEquals(Arrays.asList("a", "b"), N.flattenEachElement(Arrays.asList("a", "b"))); // No nesting
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testFlattenEachElementWithSupplier() {
        Iterable<?> mixed = Arrays.asList("a", Arrays.asList("b", "c"), "d");
        Supplier<Set<Object>> supplier = LinkedHashSet::new;
        Set<?> result = N.flattenEachElement(mixed, supplier);

        assertEquals(new LinkedHashSet<>(Arrays.asList("a", "b", "c", "d")), result);
        assertTrue(result instanceof LinkedHashSet);

        assertTrue(N.flattenEachElement(null, supplier).isEmpty());
    }

    // --- Test methods for intersection ---
    // For primitive arrays, the implementation uses BooleanList, CharList etc.
    // Assuming these Abacus utilities work as described for these tests.
    @Test
    public void testIntersection_booleanArrays() {
        assertArrayEquals(new boolean[] { true, false, true }, N.intersection(new boolean[] { true, false, true }, new boolean[] { true, true, false, false }));
        assertArrayEquals(new boolean[] { true, false }, N.intersection(new boolean[] { true, false, true }, new boolean[] { true, false })); // exact same elements
        assertArrayEquals(EMPTY_BOOLEAN_ARRAY_CONST, N.intersection(new boolean[] { true }, new boolean[] { false }));
        assertArrayEquals(EMPTY_BOOLEAN_ARRAY_CONST, N.intersection(null, new boolean[] { true }));
        assertArrayEquals(EMPTY_BOOLEAN_ARRAY_CONST, N.intersection(new boolean[] { true }, null));
        assertArrayEquals(EMPTY_BOOLEAN_ARRAY_CONST, N.intersection(EMPTY_BOOLEAN_ARRAY_CONST, new boolean[] { true }));
    }

    @Test
    public void testIntersection_charArrays() {
        assertArrayEquals(new char[] { 'a', 'b', 'a' }, N.intersection(new char[] { 'a', 'b', 'a', 'c' }, new char[] { 'b', 'a', 'd', 'a' })); // a:2, b:1 common
        assertArrayEquals(EMPTY_CHAR_ARRAY_CONST, N.intersection(new char[] { 'a' }, null));
    }

    // Similar tests for byte, short, int, long, float, double intersections

    @Test
    public void testIntersection_intArrays() {
        // N.intersection([0,1,2,2,3], [2,5,1]) -> [1,2] (not [1,2,2] as in retainAll example)
        // This means it's set-like intersection based on occurrence counts from both.
        // Multiset.of(a) intersect Multiset.of(b)
        // a: {0:1, 1:1, 2:2, 3:1}, b: {1:1, 2:1, 5:1}
        // Common elements: 1 (min(1,1)=1), 2 (min(2,1)=1) -> result [1,2] (order depends on implementation of IntList)
        // Let's assume IntList.intersection behaves as taking min occurrences.
        // If N.java's int[] intersection example `[1,2]` is correct, then IntList must implement it this way.
        // The example `int[] c = retainAll(a, b); // The elements c in a will b: [1, 2, 2].` seems to be a different method.
        // The example `int[] c = intersection(a, b); // The elements c in a will b: [1, 2].`
        // This is consistent with multiset intersection.

        int[] a1 = { 0, 1, 2, 2, 3 };
        int[] b1 = { 2, 5, 1 };
        int[] expected1 = { 1, 2 }; // Assuming order from first list or sorted. IntList might sort.
        int[] actual1 = N.intersection(a1, b1);
        Arrays.sort(actual1); // Sort to make assertion order-independent
        assertArrayEquals(expected1, actual1);

        int[] a2 = { 1, 2, 2, 3, 3, 3 };
        int[] b2 = { 2, 3, 3, 4, 4, 4 };
        int[] expected2 = { 2, 3, 3 }; // 2: min(2,1)=1, 3: min(3,2)=2
        int[] actual2 = N.intersection(a2, b2);
        Arrays.sort(actual2);
        assertArrayEquals(expected2, actual2);

        assertArrayEquals(EMPTY_INT_ARRAY_CONST, N.intersection(new int[] { 1 }, null));
    }

    @Test
    public void testIntersection_objectArrays() {
        String[] a = { "a", "b", "a", "c" };
        Object[] b = { "b", "a", "d", "a" };
        List<String> expected = Arrays.asList("a", "a", "b"); // a: min(2,2)=2, b: min(1,1)=1
        List<String> actual = N.intersection(a, b);
        Collections.sort(actual); // Sort for comparison
        Collections.sort(expected);
        assertEquals(expected, actual);

        assertTrue(N.intersection((String[]) null, b).isEmpty());
        assertTrue(N.intersection(a, (Object[]) null).isEmpty());
        assertTrue(N.intersection(new String[0], b).isEmpty());
    }

    @Test
    public void testIntersection_collections() {
        Collection<String> a = Arrays.asList("a", "b", "a", "c");
        Collection<?> b = Arrays.asList("b", "a", "d", "a");
        List<String> expected = Arrays.asList("a", "a", "b");
        List<String> actual = N.intersection(a, b);
        Collections.sort(actual);
        Collections.sort(expected);
        assertEquals(expected, actual);

        assertTrue(N.intersection((Collection<String>) null, b).isEmpty());
        assertTrue(N.intersection(a, (Collection<?>) null).isEmpty());
    }

    @Test
    public void testIntersection_collectionOfCollections() {
        Collection<String> c1 = Arrays.asList("a", "b", "c", "c");
        Collection<String> c2 = Arrays.asList("b", "c", "d", "c");
        Collection<String> c3 = Arrays.asList("c", "a", "b", "c");

        // c1: a:1, b:1, c:2
        // c2: b:1, c:2, d:1
        // c3: a:1, b:1, c:2
        // Intersection(c1,c2): b:1, c:2
        // Intersection( (b:1,c:2), c3 ): b:min(1,1)=1, c:min(2,2)=2 -> b,c,c
        List<String> expected = Arrays.asList("b", "c", "c");

        List<List<String>> listOfColls = Arrays.asList(new ArrayList<>(c1), // Modifiable list for internal intersection
                new ArrayList<>(c2), new ArrayList<>(c3));
        List<String> actual = N.intersection(listOfColls);
        Collections.sort(actual);
        Collections.sort(expected);
        assertEquals(expected, actual);

        assertTrue(N.intersection((Collection<Collection<String>>) null).isEmpty());
        assertTrue(N.intersection(Collections.<Collection<String>> emptyList()).isEmpty());
        assertEquals(Arrays.asList("a", "b"), N.intersection(Collections.singletonList(Arrays.asList("a", "b")))); // single collection
        assertTrue(N.intersection(Arrays.asList(c1, Collections.emptyList())).isEmpty()); // one is empty
    }

    // --- Test methods for difference ---
    // (a - b)
    @Test
    public void testDifference_intArrays() {
        // a: {0:1, 1:1, 2:2, 3:1}, b: {1:1, 2:1, 5:1}
        // Difference (a-b): 0 (1-0=1), 1 (1-1=0), 2 (2-1=1), 3 (1-0=1) -> result [0,2,3]
        int[] a1 = { 0, 1, 2, 2, 3 };
        int[] b1 = { 2, 5, 1 };
        int[] expected1 = { 0, 2, 3 };
        int[] actual1 = N.difference(a1, b1);
        Arrays.sort(actual1);
        assertArrayEquals(expected1, actual1);

        assertArrayEquals(EMPTY_INT_ARRAY_CONST, N.difference(null, b1));
        assertArrayEquals(a1, N.difference(a1, null)); // clone of a1
        assertArrayEquals(a1, N.difference(a1, EMPTY_INT_ARRAY_CONST));
    }

    @Test
    public void testDifference_objectArrays() {
        String[] a = { "a", "b", "a", "c" }; // a:2, b:1, c:1
        Object[] b = { "b", "a", "d" }; // a:1, b:1, d:1
        // a-b: a (2-1=1), c (1-0=1) -> [a,c]
        List<String> expected = Arrays.asList("a", "c");
        List<String> actual = N.difference(a, b);
        Collections.sort(actual);
        Collections.sort(expected);
        assertEquals(expected, actual);

        assertTrue(N.difference((String[]) null, b).isEmpty());
        assertEquals(Arrays.asList(a), N.difference(a, (Object[]) null));
    }

    @Test
    public void testDifference_collections() {
        Collection<String> a = Arrays.asList("a", "b", "a", "c");
        Collection<?> b = Arrays.asList("b", "a", "d");
        List<String> expected = Arrays.asList("a", "c");
        List<String> actual = N.difference(a, b);
        Collections.sort(actual);
        Collections.sort(expected);
        assertEquals(expected, actual);

        assertTrue(N.difference((Collection<String>) null, b).isEmpty());
        assertEquals(new ArrayList<>(a), N.difference(a, (Collection<?>) null));
    }

    // --- Test methods for symmetricDifference ---
    // (a-b) U (b-a)
    @Test
    public void testSymmetricDifference_intArrays() {
        // a: {0:1, 1:1, 2:2, 3:1}, b: {1:1, 2:1, 5:1}
        // a-b: {0:1, 2:1, 3:1}
        // b-a: {5:1}
        // Union: {0:1, 2:1, 3:1, 5:1} -> [0,2,3,5]
        int[] a1 = { 0, 1, 2, 2, 3 };
        int[] b1 = { 2, 5, 1 };
        int[] expected1 = { 0, 2, 3, 5 };
        int[] actual1 = N.symmetricDifference(a1, b1);
        Arrays.sort(actual1);
        assertArrayEquals(expected1, actual1);

        assertArrayEquals(b1, N.symmetricDifference(null, b1));
        assertArrayEquals(a1, N.symmetricDifference(a1, null));
    }

    @Test
    public void testSymmetricDifference_objectArrays() {
        String[] a = { "a", "b", "a", "c" }; // a:2, b:1, c:1
        String[] b = { "b", "a", "d" }; // a:1, b:1, d:1
        // a-b: a:1, c:1
        // b-a: d:1
        // Union: a:1, c:1, d:1 -> [a,c,d]
        List<String> expected = Arrays.asList("a", "c", "d");
        List<String> actual = N.symmetricDifference(a, b);
        Collections.sort(actual);
        Collections.sort(expected);
        assertEquals(expected, actual);

        assertEquals(Arrays.asList(b), N.symmetricDifference(null, b));
        assertEquals(Arrays.asList(a), N.symmetricDifference(a, null));
    }

    @Test
    public void testSymmetricDifference_collections() {
        Collection<String> a = Arrays.asList("a", "b", "a", "c");
        Collection<String> b = Arrays.asList("b", "a", "d");
        List<String> expected = Arrays.asList("a", "c", "d");
        List<String> actual = N.symmetricDifference(a, b);
        Collections.sort(actual);
        Collections.sort(expected);
        assertEquals(expected, actual);

        assertEquals(new ArrayList<>(b), N.symmetricDifference(null, b));
        assertEquals(new ArrayList<>(a), N.symmetricDifference(a, null));
    }

    // --- Test methods for commonSet ---
    @Test
    public void testCommonSet_twoCollections() {
        Collection<String> a = Arrays.asList("a", "b", "c", "c");
        Collection<?> b = Arrays.asList("b", "c", "d", "c", "e");
        Set<String> expected = new HashSet<>(Arrays.asList("b", "c"));
        assertEquals(expected, N.commonSet(a, b));

        assertTrue(N.commonSet(null, b).isEmpty());
        assertTrue(N.commonSet(a, null).isEmpty());
        assertTrue(N.commonSet(a, Collections.emptyList()).isEmpty());
    }

    @Test
    public void testCommonSet_collectionOfCollections() {
        Collection<String> c1 = Arrays.asList("a", "b", "c", "c");
        Collection<String> c2 = Arrays.asList("b", "c", "d", "c");
        Collection<String> c3 = Arrays.asList("c", "a", "b", "c", "b"); // b:2, c:2, a:1
        // Smallest: c1 or c2 (size 4)
        // map from smallest (c1): {a:1,b:1,c:1} (distinct from smallest for map init)
        // N's commonSet uses distinct elements of the smallest for initial map
        // Let smallest = c1 -> map = {a:1, b:1, c:1} (using distinct values)
        // cnt = 1
        // Iterate c1 (smallest): map becomes {a:2, b:2, c:2}
        // cnt = 2
        // Iterate c2: b in map, val=2 == cnt -> val=3. c in map, val=2 == cnt -> val=3. d not in map.
        // map {a:2, b:3, c:3}
        // cnt = 3
        // Iterate c3: c in map, val=3 == cnt -> val=4. a in map, val=2 < cnt. b in map, val=3==cnt -> val=4.
        // map {a:2, b:4, c:4}
        // Final check: val.value() == cnt (which is 3, because it iterates over 3 collections)
        // So, b and c. Expected {b,c}
        Set<String> expected = new HashSet<>(Arrays.asList("b", "c"));

        List<Collection<String>> listOfColls = Arrays.asList(c1, c2, c3);
        assertEquals(expected, N.commonSet(listOfColls));

        assertTrue(N.commonSet((Collection<Collection<String>>) null).isEmpty());
        assertEquals(new HashSet<>(c1), N.commonSet(Collections.singletonList(c1)));
        assertTrue(N.commonSet(Arrays.asList(c1, Collections.emptyList())).isEmpty());

        // Test with LinkedHashSet preference
        Collection<String> lc1 = new LinkedHashSet<>(Arrays.asList("z", "y", "x"));
        Collection<String> lc2 = new LinkedHashSet<>(Arrays.asList("y", "x", "w"));
        Set<String> expectedLinked = new LinkedHashSet<>(Arrays.asList("y", "x")); // Order from first if Linked
        // The implementation of commonSet iterates smallest, then others. If smallest is LinkedHashSet,
        // and result is LinkedHashSet, order of elements added to result might reflect smallest's distinct iteration.
        // The current N.commonSet creates a new HashSet or LinkedHashSet based on the first collection in the input 'c'.
        // If c.get(0) is a List or LinkedHashSet, it uses newLinkedHashSet.
        List<Collection<String>> listOfLinked = Arrays.asList(lc1, lc2);
        Set<String> actualLinked = N.commonSet(listOfLinked);
        assertEquals(expectedLinked, actualLinked);
        assertTrue(actualLinked instanceof LinkedHashSet);
    }

    // --- Test methods for exclude ---
    @Test
    public void testExclude() {
        Collection<String> coll = Arrays.asList("a", "b", "a", "c");
        assertEquals(Arrays.asList("b", "c"), N.exclude(coll, "a"));
        assertEquals(Arrays.asList("a", "b", "a", "c"), N.exclude(coll, "d")); // not present
        assertEquals(Arrays.asList("a", "a", "c"), N.exclude(coll, "b"));
        assertEquals(Arrays.asList("a", "b", "a", "c"), N.exclude(coll, null)); // exclude null if present

        Collection<String> collWithNull = Arrays.asList("a", null, "b", null);
        assertEquals(Arrays.asList("a", "b"), N.exclude(collWithNull, null));

        assertTrue(N.exclude(null, "a").isEmpty());
        assertTrue(N.exclude(Collections.emptyList(), "a").isEmpty());
    }

    @Test
    public void testExcludeToSet() {
        Collection<String> coll = Arrays.asList("a", "b", "a", "c"); // distinct a,b,c
        assertEquals(new HashSet<>(Arrays.asList("b", "c")), N.excludeToSet(coll, "a"));
        assertEquals(new HashSet<>(Arrays.asList("a", "b", "c")), N.excludeToSet(coll, "d"));

        // Test with LinkedHashSet input to check output type (should be LinkedHashSet)
        Collection<String> linkedColl = new LinkedHashSet<>(Arrays.asList("c", "a", "b"));
        Set<String> resultLinked = N.excludeToSet(linkedColl, "a");
        assertEquals(new LinkedHashSet<>(Arrays.asList("c", "b")), resultLinked);
        assertTrue(resultLinked instanceof LinkedHashSet);

        assertTrue(N.excludeToSet(null, "a").isEmpty());
    }

    // --- Test methods for excludeAll ---
    @Test
    public void testExcludeAll() {
        Collection<String> main = Arrays.asList("a", "b", "c", "a", "d");
        Collection<?> toExclude = Arrays.asList("a", "c", "e");
        assertEquals(Arrays.asList("b", "d"), N.excludeAll(main, toExclude));

        assertEquals(new ArrayList<>(main), N.excludeAll(main, null));
        assertEquals(new ArrayList<>(main), N.excludeAll(main, Collections.emptyList()));
        assertTrue(N.excludeAll(null, toExclude).isEmpty());

        // Exclude single element using excludeAll
        assertEquals(Arrays.asList("b", "c", "d"), N.excludeAll(main, Collections.singletonList("a")));
    }

    @Test
    public void testExcludeAllToSet() {
        Collection<String> main = new LinkedHashSet<>(Arrays.asList("a", "b", "c", "d")); // Use LinkedHashSet for predictable iteration
        Collection<?> toExclude = Arrays.asList("a", "c", "e"); // elements a, c
        // Result should be {b, d}
        assertEquals(new LinkedHashSet<>(Arrays.asList("b", "d")), N.excludeAllToSet(main, toExclude));

        assertEquals(new HashSet<>(main), N.excludeAllToSet(main, null)); // Returns new HashSet of main
        assertTrue(N.excludeAllToSet(null, toExclude).isEmpty());
    }

    // --- Test methods for isSubCollection ---
    @Test
    public void testIsSubCollection() {
        Collection<String> collA = Arrays.asList("a", "b", "c");
        Collection<String> collB = Arrays.asList("a", "b", "c", "d");
        Collection<String> collC = Arrays.asList("a", "b");
        Collection<String> collD = Arrays.asList("a", "x");
        Collection<String> collE = Arrays.asList("a", "a", "b");
        Collection<String> collF = Arrays.asList("a", "b", "a"); // same as E

        assertTrue(N.isSubCollection(collC, collA)); // C is sub of A
        assertTrue(N.isSubCollection(collA, collB)); // A is sub of B
        assertTrue(N.isSubCollection(collA, collA)); // Identical
        assertTrue(N.isSubCollection(Collections.emptyList(), collA)); // Empty is sub of anything

        assertFalse(N.isSubCollection(collA, collC)); // A is not sub of C (larger)
        assertFalse(N.isSubCollection(collD, collA)); // D is not sub of A (contains x)
        assertFalse(N.isSubCollection(collA, Collections.emptyList())); // Non-empty cannot be sub of empty

        // Cardinality
        assertTrue(N.isSubCollection(collE, collF)); // E is sub of F (identical)
        assertTrue(N.isSubCollection(Arrays.asList("a", "b"), collE)); // {a:1,b:1} is sub of {a:2,b:1}
        assertFalse(N.isSubCollection(collE, Arrays.asList("a", "b"))); // {a:2,b:1} is not sub of {a:1,b:1}

        assertThrows(IllegalArgumentException.class, () -> N.isSubCollection(null, collA));
        assertThrows(IllegalArgumentException.class, () -> N.isSubCollection(collA, null));
    }

    // --- Test methods for isProperSubCollection ---
    @Test
    public void testIsProperSubCollection() {
        Collection<String> collA = Arrays.asList("a", "b", "c");
        Collection<String> collB = Arrays.asList("a", "b", "c", "d");
        Collection<String> collC = Arrays.asList("a", "b");

        assertTrue(N.isProperSubCollection(collC, collA));
        assertTrue(N.isProperSubCollection(collA, collB));
        assertFalse(N.isProperSubCollection(collA, collA)); // Not proper if identical
        assertTrue(N.isProperSubCollection(Collections.emptyList(), collA)); // Empty is proper sub of non-empty

        assertFalse(N.isProperSubCollection(collA, collC)); // Larger
        assertFalse(N.isProperSubCollection(collA, Collections.emptyList())); // Non-empty not proper sub of empty

        // Cardinality
        Collection<String> collE = Arrays.asList("a", "b");
        Collection<String> collF = Arrays.asList("a", "a", "b");
        assertTrue(N.isProperSubCollection(collE, collF)); // {a:1,b:1} proper sub of {a:2,b:1}
        assertFalse(N.isProperSubCollection(collF, collE));

        assertThrows(IllegalArgumentException.class, () -> N.isProperSubCollection(null, collA));
        assertThrows(IllegalArgumentException.class, () -> N.isProperSubCollection(collA, null));
    }

    // --- Test methods for isEqualCollection ---
    @Test
    public void testIsEqualCollection() {
        Collection<String> collA1 = Arrays.asList("a", "b", "a");
        Collection<String> collA2 = Arrays.asList("a", "a", "b"); // Same elements, same cardinality
        Collection<String> collB = Arrays.asList("a", "b"); // Different cardinality for 'a'
        Collection<String> collC = Arrays.asList("a", "b", "c"); // Different elements/size

        assertTrue(N.isEqualCollection(collA1, collA2));
        assertTrue(N.isEqualCollection(null, null));
        assertTrue(N.isEqualCollection(Collections.emptyList(), Collections.emptySet()));

        assertFalse(N.isEqualCollection(collA1, collB));
        assertFalse(N.isEqualCollection(collA1, collC));
        assertFalse(N.isEqualCollection(collA1, null));
        assertFalse(N.isEqualCollection(null, collA1));

        Collection<Integer> list1 = Arrays.asList(1, 2, 2, 3);
        Collection<Integer> list2 = new LinkedList<>(Arrays.asList(3, 2, 1, 2)); // Different order, different type, but equal
        assertTrue(N.isEqualCollection(list1, list2));
    }
}

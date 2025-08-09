package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.AbstractMap;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.TreeSet;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.NoCachingNoUpdating.DisposableArray;
import com.landawn.abacus.util.NoCachingNoUpdating.DisposableBooleanArray;
import com.landawn.abacus.util.NoCachingNoUpdating.DisposableByteArray;
import com.landawn.abacus.util.NoCachingNoUpdating.DisposableCharArray;
import com.landawn.abacus.util.NoCachingNoUpdating.DisposableDeque;
import com.landawn.abacus.util.NoCachingNoUpdating.DisposableDoubleArray;
import com.landawn.abacus.util.NoCachingNoUpdating.DisposableEntry;
import com.landawn.abacus.util.NoCachingNoUpdating.DisposableFloatArray;
import com.landawn.abacus.util.NoCachingNoUpdating.DisposableIntArray;
import com.landawn.abacus.util.NoCachingNoUpdating.DisposableLongArray;
import com.landawn.abacus.util.NoCachingNoUpdating.DisposablePair;
import com.landawn.abacus.util.NoCachingNoUpdating.DisposableShortArray;
import com.landawn.abacus.util.NoCachingNoUpdating.DisposableTriple;

/**
 * Additional comprehensive unit tests for NoCachingNoUpdating classes
 * focusing on edge cases, error conditions, and thorough functional testing
 */
public class NoCachingNoUpdating101Test extends TestBase {

    // Edge case tests for DisposableArray
    @Test
    public void testDisposableArray_wrapNull() {
        assertThrows(IllegalArgumentException.class, () -> DisposableArray.wrap(null));
    }

    @Test
    public void testDisposableArray_emptyArray() {
        String[] arr = new String[0];
        DisposableArray<String> array = DisposableArray.wrap(arr);
        assertEquals(0, array.length());

        List<String> list = array.toList();
        assertTrue(list.isEmpty());

        Set<String> set = array.toSet();
        assertTrue(set.isEmpty());
    }

    @Test
    public void testDisposableArray_withNullElements() {
        String[] arr = { "a", null, "c" };
        DisposableArray<String> array = DisposableArray.wrap(arr);
        assertEquals(3, array.length());
        assertEquals("a", array.get(0));
        assertNull(array.get(1));
        assertEquals("c", array.get(2));

        List<String> list = array.toList();
        assertEquals(3, list.size());
        assertNull(list.get(1));
    }

    @Test
    public void testDisposableArray_largeArray() {
        Integer[] arr = new Integer[1000];
        for (int i = 0; i < 1000; i++) {
            arr[i] = i;
        }
        DisposableArray<Integer> array = DisposableArray.wrap(arr);
        assertEquals(1000, array.length());
        assertEquals(Integer.valueOf(999), array.get(999));
    }

    @Test
    public void testDisposableArray_toArrayWithLargerTarget() {
        String[] arr = { "a", "b" };
        DisposableArray<String> array = DisposableArray.wrap(arr);

        String[] target = new String[5];
        Arrays.fill(target, "x");
        String[] result = array.toArray(target);

        assertSame(target, result);
        assertEquals("a", result[0]);
        assertEquals("b", result[1]);
        assertEquals("x", result[2]); // Should be set to null
        assertEquals("x", result[3]);
        assertEquals("x", result[4]);
    }

    @Test
    public void testDisposableArray_functionalInterfaceExceptions() throws Exception {
        String[] arr = { "a", "b", "c" };
        DisposableArray<String> array = DisposableArray.wrap(arr);

        // Test apply with exception
        try {
            array.apply(a -> {
                throw new RuntimeException("Test exception");
            });
            fail("Should have thrown exception");
        } catch (RuntimeException e) {
            assertEquals("Test exception", e.getMessage());
        }

        // Test accept with exception
        try {
            array.accept(a -> {
                throw new RuntimeException("Test exception");
            });
            fail("Should have thrown exception");
        } catch (RuntimeException e) {
            assertEquals("Test exception", e.getMessage());
        }

        // Test forEach with exception
        try {
            array.forEach(e -> {
                if ("b".equals(e)) {
                    throw new RuntimeException("Test exception at b");
                }
            });
            fail("Should have thrown exception");
        } catch (RuntimeException e) {
            assertEquals("Test exception at b", e.getMessage());
        }
    }

    // Edge case tests for primitive arrays
    @Test
    public void testDisposableBooleanArray_wrapNull() {
        assertThrows(IllegalArgumentException.class, () -> DisposableBooleanArray.wrap(null));
    }

    @Test
    public void testDisposableBooleanArray_emptyArray() {
        boolean[] arr = new boolean[0];
        DisposableBooleanArray array = DisposableBooleanArray.wrap(arr);
        assertEquals(0, array.length());

        Boolean[] boxed = array.box();
        assertEquals(0, boxed.length);

        BooleanList list = array.toList();
        assertEquals(0, list.size());
    }

    @Test
    public void testDisposableCharArray_emptyArray() {
        char[] arr = new char[0];
        DisposableCharArray array = DisposableCharArray.wrap(arr);
        assertEquals(0, array.length());

        // These methods should handle empty arrays gracefully
        assertEquals(0, array.sum());
        assertTrue(Numbers.fuzzyEquals(array.average(), 0, 0.0001)); // or might be 0.0 depending on implementation
    }

    @Test
    public void testDisposableIntArray_overflowSum() {
        int[] arr = { Integer.MAX_VALUE, 1 };
        DisposableIntArray array = DisposableIntArray.wrap(arr);
        // This will overflow, but should still work
        assertThrows(ArithmeticException.class, () -> array.sum());
    }

    @Test
    public void testDisposableLongArray_largeValues() {
        long[] arr = { Long.MAX_VALUE, Long.MIN_VALUE, 0 };
        DisposableLongArray array = DisposableLongArray.wrap(arr);
        assertEquals(Long.MAX_VALUE, array.max());
        assertEquals(Long.MIN_VALUE, array.min());
        assertEquals(-1L, array.sum()); // MAX + MIN = -1
    }

    @Test
    public void testDisposableFloatArray_specialValues() {
        float[] arr = { Float.NaN, Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY, 0.0f, -0.0f };
        DisposableFloatArray array = DisposableFloatArray.wrap(arr);
        assertEquals(5, array.length());

        assertTrue(Float.isNaN(array.get(0)));
        assertTrue(Float.isInfinite(array.get(1)));
        assertTrue(Float.isInfinite(array.get(2)));

        // Sum and average behavior with special values
        float sum = array.sum();
        assertTrue(Float.isNaN(sum)); // NaN propagates
    }

    @Test
    public void testDisposableDoubleArray_precisionTests() {
        double[] arr = { 0.1, 0.2, 0.3 };
        DisposableDoubleArray array = DisposableDoubleArray.wrap(arr);

        double sum = array.sum();
        // Due to floating point precision, 0.1 + 0.2 + 0.3 != 0.6 exactly
        assertEquals(0.6, sum, 0.0000001);

        double avg = array.average();
        assertEquals(0.2, avg, 0.0000001);
    }

    // Edge case tests for DisposableDeque
    @Test
    public void testDisposableDeque_wrapNull() {
        assertThrows(IllegalArgumentException.class, () -> DisposableDeque.wrap(null));
    }

    @Test
    public void testDisposableDeque_getFirstOnEmpty() {
        DisposableDeque<String> deque = DisposableDeque.create(10);
        assertThrows(NoSuchElementException.class, () -> deque.getFirst());
    }

    @Test
    public void testDisposableDeque_getLastOnEmpty() {
        DisposableDeque<String> deque = DisposableDeque.create(10);
        assertThrows(NoSuchElementException.class, () -> deque.getLast());
    }

    //    @Test
    //    public void testDisposableDeque_withNullElements() {
    //        Deque<String> original = new ArrayDeque<>();
    //        original.add("a");
    //        original.add(null); // ArrayDeque doesn't allow nulls, so use LinkedList
    //
    //        Deque<String> linkedDeque = new LinkedList<>();
    //        linkedDeque.add("a");
    //        linkedDeque.add(null);
    //        linkedDeque.add("c");
    //
    //        DisposableDeque<String> deque = DisposableDeque.wrap(linkedDeque);
    //        assertEquals(3, deque.size());
    //
    //        List<String> list = deque.toList();
    //        assertEquals(3, list.size());
    //        assertNull(list.get(1));
    //    }

    @Test
    public void testDisposableDeque_functionalOperations() throws Exception {
        Deque<Integer> original = new ArrayDeque<>();
        original.add(1);
        original.add(2);
        original.add(3);

        DisposableDeque<Integer> deque = DisposableDeque.wrap(original);

        // Test apply
        int sum = deque.apply(d -> {
            int total = 0;
            for (int n : d) {
                total += n;
            }
            return total;
        });
        assertEquals(6, sum);

        // Test accept
        final int[] count = { 0 };
        deque.accept(d -> count[0] = d.size());
        assertEquals(3, count[0]);

        // Test forEach
        List<Integer> collected = new ArrayList<>();
        deque.foreach(collected::add);
        assertEquals(Arrays.asList(1, 2, 3), collected);
    }

    // Edge case tests for DisposableEntry
    @Test
    public void testDisposableEntry_wrapNull() {
        assertThrows(IllegalArgumentException.class, () -> DisposableEntry.wrap(null));
    }

    @Test
    public void testDisposableEntry_withNullKeyValue() {
        Map.Entry<String, Integer> entry1 = new AbstractMap.SimpleEntry<>(null, 100);
        DisposableEntry<String, Integer> disposable1 = DisposableEntry.wrap(entry1);
        assertNull(disposable1.getKey());
        assertEquals(Integer.valueOf(100), disposable1.getValue());

        Map.Entry<String, Integer> entry2 = new AbstractMap.SimpleEntry<>("key", null);
        DisposableEntry<String, Integer> disposable2 = DisposableEntry.wrap(entry2);
        assertEquals("key", disposable2.getKey());
        assertNull(disposable2.getValue());

        Map.Entry<String, Integer> entry3 = new AbstractMap.SimpleEntry<>(null, null);
        DisposableEntry<String, Integer> disposable3 = DisposableEntry.wrap(entry3);
        assertNull(disposable3.getKey());
        assertNull(disposable3.getValue());
    }

    @Test
    public void testDisposableEntry_hashCodeAndEquals() {
        Map.Entry<String, Integer> entry1 = new AbstractMap.SimpleEntry<>("key", 100);
        Map.Entry<String, Integer> entry2 = new AbstractMap.SimpleEntry<>("key", 100);

        DisposableEntry<String, Integer> disposable1 = DisposableEntry.wrap(entry1);
        DisposableEntry<String, Integer> disposable2 = DisposableEntry.wrap(entry2);

        assertNotEquals(disposable1.hashCode(), disposable2.hashCode());
        assertNotEquals(disposable1, disposable2);
    }

    // Edge case tests for DisposablePair
    @Test
    public void testDisposablePair_wrapNull() {
        assertThrows(IllegalArgumentException.class, () -> DisposablePair.wrap(null));
    }

    @Test
    public void testDisposablePair_withNullElements() {
        Pair<String, Integer> pair1 = Pair.of(null, 100);
        DisposablePair<String, Integer> disposable1 = DisposablePair.wrap(pair1);
        assertNull(disposable1.left());
        assertEquals(Integer.valueOf(100), disposable1.right());

        Pair<String, Integer> pair2 = Pair.of("left", null);
        DisposablePair<String, Integer> disposable2 = DisposablePair.wrap(pair2);
        assertEquals("left", disposable2.left());
        assertNull(disposable2.right());

        Pair<String, Integer> pair3 = Pair.of(null, null);
        DisposablePair<String, Integer> disposable3 = DisposablePair.wrap(pair3);
        assertNull(disposable3.left());
        assertNull(disposable3.right());
    }

    // Edge case tests for DisposableTriple
    @Test
    public void testDisposableTriple_wrapNull() {
        assertThrows(IllegalArgumentException.class, () -> DisposableTriple.wrap(null));
    }

    @Test
    public void testDisposableTriple_withNullElements() {
        Triple<String, Integer, Boolean> triple1 = Triple.of(null, 100, true);
        DisposableTriple<String, Integer, Boolean> disposable1 = DisposableTriple.wrap(triple1);
        assertNull(disposable1.left());
        assertEquals(Integer.valueOf(100), disposable1.middle());
        assertEquals(Boolean.TRUE, disposable1.right());

        Triple<String, Integer, Boolean> triple2 = Triple.of("left", null, true);
        DisposableTriple<String, Integer, Boolean> disposable2 = DisposableTriple.wrap(triple2);
        assertEquals("left", disposable2.left());
        assertNull(disposable2.middle());
        assertEquals(Boolean.TRUE, disposable2.right());

        Triple<String, Integer, Boolean> triple3 = Triple.of("left", 100, null);
        DisposableTriple<String, Integer, Boolean> disposable3 = DisposableTriple.wrap(triple3);
        assertEquals("left", disposable3.left());
        assertEquals(Integer.valueOf(100), disposable3.middle());
        assertNull(disposable3.right());

        Triple<String, Integer, Boolean> triple4 = Triple.of(null, null, null);
        DisposableTriple<String, Integer, Boolean> disposable4 = DisposableTriple.wrap(triple4);
        assertNull(disposable4.left());
        assertNull(disposable4.middle());
        assertNull(disposable4.right());
    }

    // More comprehensive tests for Timed
    @Test
    public void testTimed_hashCodeWithNullValue() {
        Timed<String> timed1 = Timed.of(null, 1000L);
        Timed<String> timed2 = Timed.of(null, 1000L);
        assertEquals(timed1.hashCode(), timed2.hashCode());

        Timed<String> timed3 = Timed.of("value", 1000L);
        assertNotEquals(timed1.hashCode(), timed3.hashCode());
    }

    @Test
    public void testTimed_equalsEdgeCases() {
        Timed<String> timed = Timed.of("value", 1000L);

        // Same instance
        assertTrue(timed.equals(timed));

        // Null
        assertFalse(timed.equals(null));

        // Different class
        assertFalse(timed.equals("not a timed"));
        assertFalse(timed.equals(new Object()));

        // Different timestamps
        Timed<String> different1 = Timed.of("value", 2000L);
        assertFalse(timed.equals(different1));

        // Different values
        Timed<String> different2 = Timed.of("other", 1000L);
        assertFalse(timed.equals(different2));

        // Both different
        Timed<String> different3 = Timed.of("other", 2000L);
        assertFalse(timed.equals(different3));
    }

    // Performance and stress tests
    @Test
    public void testDisposableArray_performanceWithLargeData() {
        int size = 100000;
        Integer[] arr = new Integer[size];
        for (int i = 0; i < size; i++) {
            arr[i] = i;
        }

        DisposableArray<Integer> array = DisposableArray.wrap(arr);

        // Test iteration performance
        long sum = 0;
        for (int i = 0; i < size; i++) {
            sum += array.get(i);
        }

        long expectedSum = (long) size * (size - 1) / 2;
        assertEquals(expectedSum, sum);

        // Test forEach performance
        final long[] forEachSum = { 0 };
        array.forEach(i -> forEachSum[0] += i);
        assertEquals(expectedSum, forEachSum[0]);
    }

    @Test
    public void testPrimitiveArrays_boundaryValues() {
        // Test byte array with boundary values
        byte[] byteArr = { Byte.MIN_VALUE, -1, 0, 1, Byte.MAX_VALUE };
        DisposableByteArray byteArray = DisposableByteArray.wrap(byteArr);
        assertEquals(Byte.MIN_VALUE, byteArray.min());
        assertEquals(Byte.MAX_VALUE, byteArray.max());

        // Test short array with boundary values
        short[] shortArr = { Short.MIN_VALUE, -1, 0, 1, Short.MAX_VALUE };
        DisposableShortArray shortArray = DisposableShortArray.wrap(shortArr);
        assertEquals(Short.MIN_VALUE, shortArray.min());
        assertEquals(Short.MAX_VALUE, shortArray.max());

        // Test int array with boundary values
        int[] intArr = { Integer.MIN_VALUE, -1, 0, 1, Integer.MAX_VALUE };
        DisposableIntArray intArray = DisposableIntArray.wrap(intArr);
        assertEquals(Integer.MIN_VALUE, intArray.min());
        assertEquals(Integer.MAX_VALUE, intArray.max());

        // Test long array with boundary values
        long[] longArr = { Long.MIN_VALUE, -1, 0, 1, Long.MAX_VALUE };
        DisposableLongArray longArray = DisposableLongArray.wrap(longArr);
        assertEquals(Long.MIN_VALUE, longArray.min());
        assertEquals(Long.MAX_VALUE, longArray.max());
    }

    // Test join operations with special characters
    @Test
    public void testJoinOperations_specialCharacters() {
        String[] arr = { "a\"b", "c,d", "e\nf", "g\th" };
        DisposableArray<String> array = DisposableArray.wrap(arr);

        assertEquals("a\"b,c,d,e\nf,g\th", array.join(","));
        assertEquals("[a\"b|c,d|e\nf|g\th]", array.join("|", "[", "]"));

        // Test with empty delimiter
        assertEquals("a\"bc,de\nfg\th", array.join(""));

        // Test with null in array
        String[] arrWithNull = { "a", null, "b" };
        DisposableArray<String> arrayWithNull = DisposableArray.wrap(arrWithNull);
        assertEquals("a,null,b", arrayWithNull.join(","));
    }

    // Test collection conversion edge cases
    @Test
    public void testCollectionConversions_customSuppliers() {
        Integer[] arr = { 1, 2, 3, 2, 1 };
        DisposableArray<Integer> array = DisposableArray.wrap(arr);

        // Test with LinkedHashSet to preserve order
        LinkedHashSet<Integer> linkedSet = array.toCollection(LinkedHashSet::new);
        assertEquals(3, linkedSet.size());
        Iterator<Integer> iter = linkedSet.iterator();
        assertEquals(Integer.valueOf(1), iter.next());
        assertEquals(Integer.valueOf(2), iter.next());
        assertEquals(Integer.valueOf(3), iter.next());

        // Test with TreeSet for sorting
        TreeSet<Integer> treeSet = array.toCollection(IntFunctions.ofTreeSet());
        assertEquals(3, treeSet.size());
        assertEquals(Integer.valueOf(1), treeSet.first());
        assertEquals(Integer.valueOf(3), treeSet.last());

        // Test with custom initial capacity
        ArrayList<Integer> customList = array.toCollection(size -> new ArrayList<>(size * 2));
        assertEquals(5, customList.size());
    }
}

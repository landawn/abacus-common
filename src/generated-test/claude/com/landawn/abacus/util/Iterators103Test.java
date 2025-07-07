package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.u.Nullable;
import com.landawn.abacus.util.function.TriFunction;

public class Iterators103Test extends TestBase {

    @Test
    public void testGet() {
        // Test with null iterator
        Nullable<String> result = Iterators.get(null, 0);
        assertFalse(result.isPresent());

        // Test with empty iterator
        Iterator<String> emptyIter = Collections.<String> emptyIterator();
        result = Iterators.get(emptyIter, 0);
        assertFalse(result.isPresent());

        // Test with valid index
        List<String> list = Arrays.asList("a", "b", "c", "d");
        result = Iterators.get(list.iterator(), 0);
        assertTrue(result.isPresent());
        assertEquals("a", result.get());

        result = Iterators.get(list.iterator(), 2);
        assertTrue(result.isPresent());
        assertEquals("c", result.get());

        // Test with index out of bounds
        result = Iterators.get(list.iterator(), 4);
        assertFalse(result.isPresent());

        // Test with negative index (should throw exception)
        assertThrows(IllegalArgumentException.class, () -> Iterators.get(list.iterator(), -1));
    }

    @Test
    public void testOccurrencesOf() {
        // Test with null iterator
        assertEquals(0, Iterators.occurrencesOf(null, "test"));

        // Test with empty iterator
        assertEquals(0, Iterators.occurrencesOf(Collections.emptyIterator(), "test"));

        // Test with non-null value
        List<String> list = Arrays.asList("a", "b", "a", "c", "a");
        assertEquals(3, Iterators.occurrencesOf(list.iterator(), "a"));
        assertEquals(1, Iterators.occurrencesOf(list.iterator(), "b"));
        assertEquals(0, Iterators.occurrencesOf(list.iterator(), "d"));

        // Test with null value
        List<String> listWithNulls = Arrays.asList("a", null, "b", null, null);
        assertEquals(3, Iterators.occurrencesOf(listWithNulls.iterator(), null));
        assertEquals(1, Iterators.occurrencesOf(listWithNulls.iterator(), "a"));
    }

    @Test
    public void testCountNoParams() {
        // Test with null iterator
        assertEquals(0, Iterators.count(null));

        // Test with empty iterator
        assertEquals(0, Iterators.count(Collections.emptyIterator()));

        // Test with non-empty iterator
        List<String> list = Arrays.asList("a", "b", "c");
        assertEquals(3, Iterators.count(list.iterator()));

        // Test with larger collection
        List<Integer> numbers = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            numbers.add(i);
        }
        assertEquals(100, Iterators.count(numbers.iterator()));
    }

    @Test
    public void testCountWithPredicate() {
        // Test with null iterator
        assertEquals(0, Iterators.count(null, str -> true));

        // Test with null predicate
        assertThrows(IllegalArgumentException.class, () -> Iterators.count(Arrays.asList("a", "b").iterator(), null));

        // Test with empty iterator
        assertEquals(0, Iterators.count(Collections.emptyIterator(), str -> true));

        // Test with predicate
        List<Integer> numbers = Arrays.asList(1, 2, 3, 4, 5, 6);
        assertEquals(3, Iterators.count(numbers.iterator(), n -> n % 2 == 0));
        assertEquals(3, Iterators.count(numbers.iterator(), n -> n % 2 == 1));
        assertEquals(0, Iterators.count(numbers.iterator(), n -> n > 10));
        assertEquals(6, Iterators.count(numbers.iterator(), n -> n > 0));
    }

    @Test
    public void testIndexOfTwoParams() {
        // Test with null iterator
        assertEquals(N.INDEX_NOT_FOUND, Iterators.indexOf(null, "test"));

        // Test with empty iterator
        assertEquals(N.INDEX_NOT_FOUND, Iterators.indexOf(Collections.emptyIterator(), "test"));

        // Test finding existing element
        List<String> list = Arrays.asList("a", "b", "c", "b", "d");
        assertEquals(0, Iterators.indexOf(list.iterator(), "a"));
        assertEquals(1, Iterators.indexOf(list.iterator(), "b"));
        assertEquals(4, Iterators.indexOf(list.iterator(), "d"));

        // Test finding non-existing element
        assertEquals(N.INDEX_NOT_FOUND, Iterators.indexOf(list.iterator(), "e"));

        // Test with null value
        List<String> listWithNull = Arrays.asList("a", null, "b", null);
        assertEquals(1, Iterators.indexOf(listWithNull.iterator(), null));
    }

    @Test
    public void testIndexOfThreeParams() {
        // Test with null iterator
        assertEquals(N.INDEX_NOT_FOUND, Iterators.indexOf(null, "test", 0));

        // Test with fromIndex = 0
        List<String> list = Arrays.asList("a", "b", "c", "b", "d");
        assertEquals(1, Iterators.indexOf(list.iterator(), "b", 0));

        // Test with fromIndex > 0
        assertEquals(3, Iterators.indexOf(list.iterator(), "b", 2));
        assertEquals(N.INDEX_NOT_FOUND, Iterators.indexOf(list.iterator(), "a", 1));

        // Test with fromIndex >= list size
        assertEquals(N.INDEX_NOT_FOUND, Iterators.indexOf(list.iterator(), "a", 10));
    }

    @Test
    public void testElementsEqual() {
        // Test with both null
        Iterator<String> iter1 = null;
        Iterator<String> iter2 = null;
        assertThrows(NullPointerException.class, () -> Iterators.elementsEqual(iter1, iter2));

        // Test with empty iterators
        assertTrue(Iterators.elementsEqual(Collections.emptyIterator(), Collections.emptyIterator()));

        // Test with equal iterators
        List<String> list1 = Arrays.asList("a", "b", "c");
        List<String> list2 = Arrays.asList("a", "b", "c");
        assertTrue(Iterators.elementsEqual(list1.iterator(), list2.iterator()));

        // Test with different lengths
        List<String> list3 = Arrays.asList("a", "b");
        assertFalse(Iterators.elementsEqual(list1.iterator(), list3.iterator()));

        // Test with different elements
        List<String> list4 = Arrays.asList("a", "b", "d");
        assertFalse(Iterators.elementsEqual(list1.iterator(), list4.iterator()));

        // Test with nulls
        List<String> listWithNull1 = Arrays.asList("a", null, "c");
        List<String> listWithNull2 = Arrays.asList("a", null, "c");
        assertTrue(Iterators.elementsEqual(listWithNull1.iterator(), listWithNull2.iterator()));
    }

    @Test
    public void testRepeatWithInt() {
        {
            // Test with n = 0
            ObjIterator<String> iter = Iterators.repeat("test", 0);
            assertFalse(iter.hasNext());
        }

        {
            // Test with n > 0
            ObjIterator<String> iter = Iterators.repeat("hello", 3);
            assertTrue(iter.hasNext());
            assertEquals("hello", iter.next());
            assertEquals("hello", iter.next());
            assertEquals("hello", iter.next());
            assertFalse(iter.hasNext());

            // Test NoSuchElementException
            assertThrows(NoSuchElementException.class, () -> iter.next());

            // Test with negative n
            assertThrows(IllegalArgumentException.class, () -> Iterators.repeat("test", -1));
        }
    }

    @Test
    public void testRepeatWithLong() {
        // Test with n = 0
        ObjIterator<String> iter = Iterators.repeat("test", 0L);
        assertFalse(iter.hasNext());

        // Test with n > 0
        iter = Iterators.repeat("hello", 3L);
        assertTrue(iter.hasNext());
        assertEquals("hello", iter.next());
        assertEquals("hello", iter.next());
        assertEquals("hello", iter.next());
        assertFalse(iter.hasNext());

        // Test with large n
        iter = Iterators.repeat("x", 1000L);
        int count = 0;
        while (iter.hasNext()) {
            assertEquals("x", iter.next());
            count++;
        }
        assertEquals(1000, count);

        // Test with negative n
        assertThrows(IllegalArgumentException.class, () -> Iterators.repeat("test", -1L));
    }

    @Test
    public void testRepeatElements() {
        // Test with empty collection
        ObjIterator<String> iter = Iterators.repeatElements(Collections.emptyList(), 3L);
        assertFalse(iter.hasNext());

        // Test with n = 0
        iter = Iterators.repeatElements(Arrays.asList("a", "b"), 0L);
        assertFalse(iter.hasNext());

        // Test normal case
        iter = Iterators.repeatElements(Arrays.asList("a", "b", "c"), 2L);
        assertEquals("a", iter.next());
        assertEquals("a", iter.next());
        assertEquals("b", iter.next());
        assertEquals("b", iter.next());
        assertEquals("c", iter.next());
        assertEquals("c", iter.next());
        assertFalse(iter.hasNext());

        // Test with negative n
        assertThrows(IllegalArgumentException.class, () -> Iterators.repeatElements(Arrays.asList("a"), -1L));
    }

    @Test
    public void testRepeatCollection() {
        // Test with empty collection
        ObjIterator<String> iter = Iterators.repeatCollection(Collections.emptyList(), 3L);
        assertFalse(iter.hasNext());

        // Test with n = 0
        iter = Iterators.repeatCollection(Arrays.asList("a", "b"), 0L);
        assertFalse(iter.hasNext());

        // Test normal case
        iter = Iterators.repeatCollection(Arrays.asList("a", "b"), 2L);
        assertEquals("a", iter.next());
        assertEquals("b", iter.next());
        assertEquals("a", iter.next());
        assertEquals("b", iter.next());
        assertFalse(iter.hasNext());

        // Test with negative n
        assertThrows(IllegalArgumentException.class, () -> Iterators.repeatCollection(Arrays.asList("a"), -1L));
    }

    @Test
    public void testRepeatElementsToSize() {
        // Test with empty collection and size > 0
        assertThrows(IllegalArgumentException.class, () -> Iterators.repeatElementsToSize(Collections.emptyList(), 5L));

        // Test with size = 0
        ObjIterator<String> iter = Iterators.repeatElementsToSize(Arrays.asList("a"), 0L);
        assertFalse(iter.hasNext());

        // Test exact division
        iter = Iterators.repeatElementsToSize(Arrays.asList("a", "b"), 4L);
        assertEquals("a", iter.next());
        assertEquals("a", iter.next());
        assertEquals("b", iter.next());
        assertEquals("b", iter.next());
        assertFalse(iter.hasNext());

        // Test with remainder
        iter = Iterators.repeatElementsToSize(Arrays.asList("a", "b", "c"), 7L);
        int count = 0;
        while (iter.hasNext()) {
            iter.next();
            count++;
        }
        assertEquals(7, count);

        // Test with negative size
        assertThrows(IllegalArgumentException.class, () -> Iterators.repeatElementsToSize(Arrays.asList("a"), -1L));
    }

    @Test
    public void testRepeatCollectionToSize() {
        // Test with empty collection and size > 0
        assertThrows(IllegalArgumentException.class, () -> Iterators.repeatCollectionToSize(Collections.emptyList(), 5L));

        // Test with size = 0
        ObjIterator<String> iter = Iterators.repeatCollectionToSize(Arrays.asList("a"), 0L);
        assertFalse(iter.hasNext());

        // Test normal case
        iter = Iterators.repeatCollectionToSize(Arrays.asList("a", "b"), 5L);
        assertEquals("a", iter.next());
        assertEquals("b", iter.next());
        assertEquals("a", iter.next());
        assertEquals("b", iter.next());
        assertEquals("a", iter.next());
        assertFalse(iter.hasNext());

        // Test with negative size
        assertThrows(IllegalArgumentException.class, () -> Iterators.repeatCollectionToSize(Arrays.asList("a"), -1L));
    }

    @Test
    public void testCycleArray() {
        // Test with empty array
        ObjIterator<String> iter = Iterators.cycle();
        assertFalse(iter.hasNext());

        // Test with non-empty array
        iter = Iterators.cycle("a", "b", "c");
        for (int i = 0; i < 10; i++) {
            assertTrue(iter.hasNext());
            assertEquals("a", iter.next());
            assertEquals("b", iter.next());
            assertEquals("c", iter.next());
        }
    }

    @Test
    public void testCycleIterable() {
        // Test with empty iterable
        ObjIterator<String> iter = Iterators.cycle(Collections.emptyList());
        assertFalse(iter.hasNext());

        // Test with non-empty iterable
        iter = Iterators.cycle(Arrays.asList("x", "y"));
        for (int i = 0; i < 10; i++) {
            assertTrue(iter.hasNext());
            assertEquals("x", iter.next());
            assertEquals("y", iter.next());
        }
    }

    @Test
    public void testCycleIterableWithRounds() {
        // Test with empty iterable
        ObjIterator<String> iter = Iterators.cycle(Collections.emptyList(), 5L);
        assertFalse(iter.hasNext());

        // Test with rounds = 0
        iter = Iterators.cycle(Arrays.asList("a", "b"), 0L);
        assertFalse(iter.hasNext());

        // Test with rounds = 1
        iter = Iterators.cycle(Arrays.asList("a", "b"), 1L);
        assertEquals("a", iter.next());
        assertEquals("b", iter.next());
        assertFalse(iter.hasNext());

        // Test with rounds > 1
        iter = Iterators.cycle(Arrays.asList("a", "b"), 3L);
        for (int i = 0; i < 3; i++) {
            assertEquals("a", iter.next());
            assertEquals("b", iter.next());
        }
        assertFalse(iter.hasNext());

        // Test with negative rounds
        assertThrows(IllegalArgumentException.class, () -> Iterators.cycle(Arrays.asList("a"), -1L));
    }

    @Test
    public void testConcatBooleanArrays() {
        // Test with empty arrays
        BooleanIterator iter = Iterators.concat(new boolean[][] {});
        assertFalse(iter.hasNext());

        // Test with multiple arrays
        boolean[] arr1 = { true, false };
        boolean[] arr2 = { false, true, true };
        boolean[] arr3 = {};
        boolean[] arr4 = { false };

        iter = Iterators.concat(arr1, arr2, arr3, arr4);
        assertTrue(iter.hasNext());
        assertTrue(iter.nextBoolean());
        assertFalse(iter.nextBoolean());
        assertFalse(iter.nextBoolean());
        assertTrue(iter.nextBoolean());
        assertTrue(iter.nextBoolean());
        assertFalse(iter.nextBoolean());
        assertFalse(iter.hasNext());
    }

    @Test
    public void testConcatCharArrays() {
        // Test with empty arrays
        CharIterator iter = Iterators.concat(new char[][] {});
        assertFalse(iter.hasNext());

        // Test with multiple arrays
        char[] arr1 = { 'a', 'b' };
        char[] arr2 = { 'c', 'd', 'e' };

        iter = Iterators.concat(arr1, arr2);
        assertEquals('a', iter.nextChar());
        assertEquals('b', iter.nextChar());
        assertEquals('c', iter.nextChar());
        assertEquals('d', iter.nextChar());
        assertEquals('e', iter.nextChar());
        assertFalse(iter.hasNext());
    }

    @Test
    public void testConcatByteArrays() {
        // Test with multiple arrays
        byte[] arr1 = { 1, 2 };
        byte[] arr2 = { 3, 4, 5 };

        ByteIterator iter = Iterators.concat(arr1, arr2);
        assertEquals(1, iter.nextByte());
        assertEquals(2, iter.nextByte());
        assertEquals(3, iter.nextByte());
        assertEquals(4, iter.nextByte());
        assertEquals(5, iter.nextByte());
        assertFalse(iter.hasNext());
    }

    @Test
    public void testConcatShortArrays() {
        // Test with multiple arrays
        short[] arr1 = { 10, 20 };
        short[] arr2 = { 30, 40 };

        ShortIterator iter = Iterators.concat(arr1, arr2);
        assertEquals(10, iter.nextShort());
        assertEquals(20, iter.nextShort());
        assertEquals(30, iter.nextShort());
        assertEquals(40, iter.nextShort());
        assertFalse(iter.hasNext());
    }

    @Test
    public void testConcatIntArrays() {
        // Test with multiple arrays
        int[] arr1 = { 100, 200 };
        int[] arr2 = { 300 };

        IntIterator iter = Iterators.concat(arr1, arr2);
        assertEquals(100, iter.nextInt());
        assertEquals(200, iter.nextInt());
        assertEquals(300, iter.nextInt());
        assertFalse(iter.hasNext());
    }

    @Test
    public void testConcatLongArrays() {
        // Test with multiple arrays
        long[] arr1 = { 1000L, 2000L };
        long[] arr2 = { 3000L };

        LongIterator iter = Iterators.concat(arr1, arr2);
        assertEquals(1000L, iter.nextLong());
        assertEquals(2000L, iter.nextLong());
        assertEquals(3000L, iter.nextLong());
        assertFalse(iter.hasNext());
    }

    @Test
    public void testConcatFloatArrays() {
        // Test with multiple arrays
        float[] arr1 = { 1.1f, 2.2f };
        float[] arr2 = { 3.3f };

        FloatIterator iter = Iterators.concat(arr1, arr2);
        assertEquals(1.1f, iter.nextFloat(), 0.0001);
        assertEquals(2.2f, iter.nextFloat(), 0.0001);
        assertEquals(3.3f, iter.nextFloat(), 0.0001);
        assertFalse(iter.hasNext());
    }

    @Test
    public void testConcatDoubleArrays() {
        // Test with multiple arrays
        double[] arr1 = { 1.11, 2.22 };
        double[] arr2 = { 3.33 };

        DoubleIterator iter = Iterators.concat(arr1, arr2);
        assertEquals(1.11, iter.nextDouble(), 0.0001);
        assertEquals(2.22, iter.nextDouble(), 0.0001);
        assertEquals(3.33, iter.nextDouble(), 0.0001);
        assertFalse(iter.hasNext());
    }

    @Test
    public void testConcatBooleanIterators() {
        // Test with empty iterators
        BooleanIterator iter = Iterators.concat(new BooleanIterator[] {});
        assertFalse(iter.hasNext());

        // Test with multiple iterators
        BooleanIterator iter1 = BooleanIterator.of(true, false);
        BooleanIterator iter2 = BooleanIterator.of(true);

        iter = Iterators.concat(iter1, iter2);
        assertTrue(iter.nextBoolean());
        assertFalse(iter.nextBoolean());
        assertTrue(iter.nextBoolean());
        assertFalse(iter.hasNext());
    }

    @Test
    public void testConcatCharIterators() {
        CharIterator iter1 = CharIterator.of('a', 'b');
        CharIterator iter2 = CharIterator.of('c');

        CharIterator iter = Iterators.concat(iter1, iter2);
        assertEquals('a', iter.nextChar());
        assertEquals('b', iter.nextChar());
        assertEquals('c', iter.nextChar());
        assertFalse(iter.hasNext());
    }

    @Test
    public void testConcatByteIterators() {
        ByteIterator iter1 = ByteIterator.of((byte) 1, (byte) 2);
        ByteIterator iter2 = ByteIterator.of((byte) 3);

        ByteIterator iter = Iterators.concat(iter1, iter2);
        assertEquals(1, iter.nextByte());
        assertEquals(2, iter.nextByte());
        assertEquals(3, iter.nextByte());
        assertFalse(iter.hasNext());
    }

    @Test
    public void testConcatShortIterators() {
        ShortIterator iter1 = ShortIterator.of((short) 10, (short) 20);
        ShortIterator iter2 = ShortIterator.of((short) 30);

        ShortIterator iter = Iterators.concat(iter1, iter2);
        assertEquals(10, iter.nextShort());
        assertEquals(20, iter.nextShort());
        assertEquals(30, iter.nextShort());
        assertFalse(iter.hasNext());
    }

    @Test
    public void testConcatIntIterators() {
        IntIterator iter1 = IntIterator.of(100, 200);
        IntIterator iter2 = IntIterator.of(300);

        IntIterator iter = Iterators.concat(iter1, iter2);
        assertEquals(100, iter.nextInt());
        assertEquals(200, iter.nextInt());
        assertEquals(300, iter.nextInt());
        assertFalse(iter.hasNext());
    }

    @Test
    public void testConcatLongIterators() {
        LongIterator iter1 = LongIterator.of(1000L, 2000L);
        LongIterator iter2 = LongIterator.of(3000L);

        LongIterator iter = Iterators.concat(iter1, iter2);
        assertEquals(1000L, iter.nextLong());
        assertEquals(2000L, iter.nextLong());
        assertEquals(3000L, iter.nextLong());
        assertFalse(iter.hasNext());
    }

    @Test
    public void testConcatFloatIterators() {
        FloatIterator iter1 = FloatIterator.of(1.1f, 2.2f);
        FloatIterator iter2 = FloatIterator.of(3.3f);

        FloatIterator iter = Iterators.concat(iter1, iter2);
        assertEquals(1.1f, iter.nextFloat(), 0.0001);
        assertEquals(2.2f, iter.nextFloat(), 0.0001);
        assertEquals(3.3f, iter.nextFloat(), 0.0001);
        assertFalse(iter.hasNext());
    }

    @Test
    public void testConcatDoubleIterators() {
        DoubleIterator iter1 = DoubleIterator.of(1.11, 2.22);
        DoubleIterator iter2 = DoubleIterator.of(3.33);

        DoubleIterator iter = Iterators.concat(iter1, iter2);
        assertEquals(1.11, iter.nextDouble(), 0.0001);
        assertEquals(2.22, iter.nextDouble(), 0.0001);
        assertEquals(3.33, iter.nextDouble(), 0.0001);
        assertFalse(iter.hasNext());
    }

    @Test
    public void testConcatObjectArrays() {
        // Test with empty arrays
        ObjIterator<String> iter = Iterators.concat(new String[][] {});
        assertFalse(iter.hasNext());

        // Test with multiple arrays
        String[] arr1 = { "a", "b" };
        String[] arr2 = { "c" };
        String[] arr3 = {};
        String[] arr4 = { "d", "e" };

        iter = Iterators.concat(arr1, arr2, arr3, arr4);
        assertEquals("a", iter.next());
        assertEquals("b", iter.next());
        assertEquals("c", iter.next());
        assertEquals("d", iter.next());
        assertEquals("e", iter.next());
        assertFalse(iter.hasNext());
    }

    @Test
    public void testConcatIterators() {
        // Test with empty array
        ObjIterator<String> iter = Iterators.concat(new Iterator[] {});
        assertFalse(iter.hasNext());

        // Test with multiple iterators
        Iterator<String> iter1 = Arrays.asList("a", "b").iterator();
        Iterator<String> iter2 = Arrays.asList("c").iterator();
        Iterator<String> iter3 = Collections.<String> emptyIterator();
        Iterator<String> iter4 = Arrays.asList("d", "e").iterator();

        iter = Iterators.concat(iter1, iter2, iter3, iter4);
        assertEquals("a", iter.next());
        assertEquals("b", iter.next());
        assertEquals("c", iter.next());
        assertEquals("d", iter.next());
        assertEquals("e", iter.next());
        assertFalse(iter.hasNext());
    }

    @Test
    public void testConcatIterables() {
        // Test with multiple iterables
        Iterable<String> iter1 = Arrays.asList("a", "b");
        Iterable<String> iter2 = Arrays.asList("c");
        Iterable<String> iter3 = Collections.emptyList();
        Iterable<String> iter4 = Arrays.asList("d", "e");

        ObjIterator<String> iter = Iterators.concat(iter1, iter2, iter3, iter4);
        assertEquals("a", iter.next());
        assertEquals("b", iter.next());
        assertEquals("c", iter.next());
        assertEquals("d", iter.next());
        assertEquals("e", iter.next());
        assertFalse(iter.hasNext());
    }

    @Test
    public void testConcatMaps() {
        // Test with empty array
        ObjIterator<Map.Entry<String, Integer>> iter = Iterators.concat(new Map[] {});
        assertFalse(iter.hasNext());

        // Test with multiple maps
        Map<String, Integer> map1 = new HashMap<>();
        map1.put("a", 1);
        map1.put("b", 2);

        Map<String, Integer> map2 = new HashMap<>();
        map2.put("c", 3);

        Map<String, Integer> map3 = new HashMap<>(); // empty map

        iter = Iterators.concat(map1, map2, map3);

        Set<String> keys = new HashSet<>();
        Set<Integer> values = new HashSet<>();
        while (iter.hasNext()) {
            Map.Entry<String, Integer> entry = iter.next();
            keys.add(entry.getKey());
            values.add(entry.getValue());
        }

        assertEquals(3, keys.size());
        assertTrue(keys.contains("a"));
        assertTrue(keys.contains("b"));
        assertTrue(keys.contains("c"));
        assertTrue(values.contains(1));
        assertTrue(values.contains(2));
        assertTrue(values.contains(3));
    }

    @Test
    public void testConcatCollectionOfIterators() {
        // Test with empty collection
        Collection<Iterator<String>> coll = Collections.emptyList();
        ObjIterator<String> iter = Iterators.concat(coll);
        assertFalse(iter.hasNext());

        // Test with collection of iterators
        List<Iterator<String>> iterators = new ArrayList<>();
        iterators.add(Arrays.asList("a", "b").iterator());
        iterators.add(Arrays.asList("c").iterator());
        iterators.add(Collections.<String> emptyIterator());
        iterators.add(Arrays.asList("d").iterator());

        iter = Iterators.concat(iterators);
        assertEquals("a", iter.next());
        assertEquals("b", iter.next());
        assertEquals("c", iter.next());
        assertEquals("d", iter.next());
        assertFalse(iter.hasNext());
    }

    @Test
    public void testConcatIterablesCollection() {
        // Test with empty collection
        Collection<Iterable<String>> coll = Collections.emptyList();
        ObjIterator<String> iter = Iterators.concatIterables(coll);
        assertFalse(iter.hasNext());

        // Test with collection of iterables
        List<Iterable<String>> iterables = new ArrayList<>();
        iterables.add(Arrays.asList("a", "b"));
        iterables.add(Arrays.asList("c"));
        iterables.add(Collections.emptyList());
        iterables.add(Arrays.asList("d"));

        iter = Iterators.concatIterables(iterables);
        assertEquals("a", iter.next());
        assertEquals("b", iter.next());
        assertEquals("c", iter.next());
        assertEquals("d", iter.next());
        assertFalse(iter.hasNext());
    }

    @Test
    public void testConcatBiIterators() {
        // Test with empty array
        BiIterator<String, Integer> iter = Iterators.concat(new BiIterator[] {});
        assertFalse(iter.hasNext());

        // Test with multiple BiIterators
        BiIterator<String, Integer> iter1 = BiIterator.of(N.asLinkedHashMap("a", 1, "b", 2));
        BiIterator<String, Integer> iter2 = BiIterator.of(N.asLinkedHashMap("c", 3));

        iter = Iterators.concat(iter1, iter2);

        Pair<String, Integer> pair = iter.next();
        assertEquals("a", pair.left());
        assertEquals(1, pair.right());

        pair = iter.next();
        assertEquals("b", pair.left());
        assertEquals(2, pair.right());

        pair = iter.next();
        assertEquals("c", pair.left());
        assertEquals(3, pair.right());

        assertFalse(iter.hasNext());
    }

    @Test
    public void testConcatTriIterators() {
        // Test with empty array
        TriIterator<String, Integer, Boolean> iter = Iterators.concat(new TriIterator[] {});
        assertFalse(iter.hasNext());

        // Test with multiple TriIterators
        TriIterator<String, Integer, Boolean> iter1 = TriIterator.zip(new String[] { "a", "b" }, new Integer[] { 1, 2 }, new Boolean[] { true, false });
        TriIterator<String, Integer, Boolean> iter2 = TriIterator.zip(new String[] { "c" }, new Integer[] { 3 }, new Boolean[] { true });

        iter = Iterators.concat(iter1, iter2);

        Triple<String, Integer, Boolean> triple = iter.next();
        assertEquals("a", triple.left());
        assertEquals(1, triple.middle());
        assertEquals(true, triple.right());

        triple = iter.next();
        assertEquals("b", triple.left());
        assertEquals(2, triple.middle());
        assertEquals(false, triple.right());

        triple = iter.next();
        assertEquals("c", triple.left());
        assertEquals(3, triple.middle());
        assertEquals(true, triple.right());

        assertFalse(iter.hasNext());
    }

    @Test
    public void testMergeTwoIterators() {
        // Test with null selectors
        assertThrows(IllegalArgumentException.class, () -> Iterators.merge(Arrays.asList(1).iterator(), Arrays.asList(2).iterator(), null));

        // Test with null iterators
        BiFunction<Integer, Integer, MergeResult> selector = (a, b) -> a < b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND;
        ObjIterator<Integer> iter = Iterators.merge((Iterator<Integer>) null, (Iterator<Integer>) null, selector);
        assertFalse(iter.hasNext());

        // Test normal merge
        Iterator<Integer> iter1 = Arrays.asList(1, 3, 5).iterator();
        Iterator<Integer> iter2 = Arrays.asList(2, 4, 6).iterator();

        iter = Iterators.merge(iter1, iter2, selector);
        assertEquals(1, iter.next());
        assertEquals(2, iter.next());
        assertEquals(3, iter.next());
        assertEquals(4, iter.next());
        assertEquals(5, iter.next());
        assertEquals(6, iter.next());
        assertFalse(iter.hasNext());
    }

    @Test
    public void testMergeCollectionOfIterators() {
        // Test with null selector
        assertThrows(IllegalArgumentException.class, () -> Iterators.merge(Arrays.asList(Arrays.asList(1).iterator()), null));

        // Test with empty collection
        BiFunction<Integer, Integer, MergeResult> selector = (a, b) -> a < b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND;
        Collection<Iterator<Integer>> coll = Collections.emptyList();
        ObjIterator<Integer> iter = Iterators.merge(coll, selector);
        assertFalse(iter.hasNext());

        // Test with single iterator
        coll = Arrays.asList(Arrays.asList(1, 2, 3).iterator());
        iter = Iterators.merge(coll, selector);
        assertEquals(1, iter.next());
        assertEquals(2, iter.next());
        assertEquals(3, iter.next());
        assertFalse(iter.hasNext());

        // Test with multiple iterators
        List<Iterator<Integer>> iterators = new ArrayList<>();
        iterators.add(Arrays.asList(1, 4, 7).iterator());
        iterators.add(Arrays.asList(2, 5, 8).iterator());
        iterators.add(Arrays.asList(3, 6, 9).iterator());

        iter = Iterators.merge(iterators, selector);
        for (int i = 1; i <= 9; i++) {
            assertEquals(i, iter.next());
        }
        assertFalse(iter.hasNext());
    }

    @Test
    public void testMergeTwoIterables() {
        BiFunction<Integer, Integer, MergeResult> selector = (a, b) -> a < b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND;

        Iterable<Integer> iter1 = Arrays.asList(1, 3, 5);
        Iterable<Integer> iter2 = Arrays.asList(2, 4, 6);

        ObjIterator<Integer> iter = Iterators.merge(iter1, iter2, selector);
        for (int i = 1; i <= 6; i++) {
            assertEquals(i, iter.next());
        }
        assertFalse(iter.hasNext());
    }

    @Test
    public void testMergeIterables() {
        // Test with null selector
        assertThrows(IllegalArgumentException.class, () -> Iterators.mergeIterables(Arrays.asList(Arrays.asList(1)), null));

        BiFunction<Integer, Integer, MergeResult> selector = (a, b) -> a < b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND;

        // Test with empty collection
        Collection<Iterable<Integer>> coll = Collections.emptyList();
        ObjIterator<Integer> iter = Iterators.mergeIterables(coll, selector);
        assertFalse(iter.hasNext());

        // Test with multiple iterables
        List<Iterable<Integer>> iterables = new ArrayList<>();
        iterables.add(Arrays.asList(1, 4));
        iterables.add(Arrays.asList(2, 5));
        iterables.add(Arrays.asList(3, 6));

        iter = Iterators.mergeIterables(iterables, selector);
        for (int i = 1; i <= 6; i++) {
            assertEquals(i, iter.next());
        }
        assertFalse(iter.hasNext());
    }

    @Test
    public void testMergeSortedIteratorsComparable() {
        Iterator<Integer> iter1 = Arrays.asList(1, 3, 5).iterator();
        Iterator<Integer> iter2 = Arrays.asList(2, 4, 6).iterator();

        ObjIterator<Integer> iter = Iterators.mergeSorted(iter1, iter2);
        for (int i = 1; i <= 6; i++) {
            assertEquals(i, iter.next());
        }
        assertFalse(iter.hasNext());
    }

    @Test
    public void testMergeSortedIteratorsWithComparator() {
        // Test with null comparator
        assertThrows(IllegalArgumentException.class, () -> Iterators.mergeSorted(Arrays.asList(1).iterator(), Arrays.asList(2).iterator(), null));

        // Test with custom comparator
        Comparator<String> cmp = (a, b) -> a.compareTo(b);
        Iterator<String> iter1 = Arrays.asList("a", "c", "e").iterator();
        Iterator<String> iter2 = Arrays.asList("b", "d", "f").iterator();

        ObjIterator<String> iter = Iterators.mergeSorted(iter1, iter2, cmp);
        assertEquals("a", iter.next());
        assertEquals("b", iter.next());
        assertEquals("c", iter.next());
        assertEquals("d", iter.next());
        assertEquals("e", iter.next());
        assertEquals("f", iter.next());
        assertFalse(iter.hasNext());
    }

    @Test
    public void testMergeSortedIterablesComparable() {
        Iterable<Integer> iter1 = Arrays.asList(1, 3, 5);
        Iterable<Integer> iter2 = Arrays.asList(2, 4, 6);

        ObjIterator<Integer> iter = Iterators.mergeSorted(iter1, iter2);
        for (int i = 1; i <= 6; i++) {
            assertEquals(i, iter.next());
        }
        assertFalse(iter.hasNext());
    }

    @Test
    public void testMergeSortedIterablesWithComparator() {
        Comparator<String> cmp = (a, b) -> a.compareTo(b);
        Iterable<String> iter1 = Arrays.asList("a", "c", "e");
        Iterable<String> iter2 = Arrays.asList("b", "d", "f");

        ObjIterator<String> iter = Iterators.mergeSorted(iter1, iter2, cmp);
        assertEquals("a", iter.next());
        assertEquals("b", iter.next());
        assertEquals("c", iter.next());
        assertEquals("d", iter.next());
        assertEquals("e", iter.next());
        assertEquals("f", iter.next());
        assertFalse(iter.hasNext());
    }

    @Test
    public void testZipTwoIterators() {
        // Test with null iterators
        BiFunction<String, Integer, String> zipFunc = (a, b) -> a + b;
        ObjIterator<String> iter = Iterators.zip((Iterator<String>) null, (Iterator<Integer>) null, zipFunc);
        assertFalse(iter.hasNext());

        // Test normal zip
        Iterator<String> iter1 = Arrays.asList("a", "b", "c").iterator();
        Iterator<Integer> iter2 = Arrays.asList(1, 2, 3).iterator();

        iter = Iterators.zip(iter1, iter2, zipFunc);
        assertEquals("a1", iter.next());
        assertEquals("b2", iter.next());
        assertEquals("c3", iter.next());
        assertFalse(iter.hasNext());

        // Test with different lengths
        iter1 = Arrays.asList("a", "b").iterator();
        iter2 = Arrays.asList(1, 2, 3).iterator();

        iter = Iterators.zip(iter1, iter2, zipFunc);
        assertEquals("a1", iter.next());
        assertEquals("b2", iter.next());
        assertFalse(iter.hasNext());
    }

    @Test
    public void testZipTwoIterables() {
        BiFunction<String, Integer, String> zipFunc = (a, b) -> a + b;

        Iterable<String> iter1 = Arrays.asList("a", "b", "c");
        Iterable<Integer> iter2 = Arrays.asList(1, 2, 3);

        ObjIterator<String> iter = Iterators.zip(iter1, iter2, zipFunc);
        assertEquals("a1", iter.next());
        assertEquals("b2", iter.next());
        assertEquals("c3", iter.next());
        assertFalse(iter.hasNext());
    }

    @Test
    public void testZipThreeIterators() {
        TriFunction<String, Integer, Boolean, String> zipFunc = (a, b, c) -> a + b + c;

        Iterator<String> iter1 = Arrays.asList("a", "b").iterator();
        Iterator<Integer> iter2 = Arrays.asList(1, 2).iterator();
        Iterator<Boolean> iter3 = Arrays.asList(true, false).iterator();

        ObjIterator<String> iter = Iterators.zip(iter1, iter2, iter3, zipFunc);
        assertEquals("a1true", iter.next());
        assertEquals("b2false", iter.next());
        assertFalse(iter.hasNext());
    }

    @Test
    public void testZipThreeIterables() {
        TriFunction<String, Integer, Boolean, String> zipFunc = (a, b, c) -> a + b + c;

        Iterable<String> iter1 = Arrays.asList("a", "b");
        Iterable<Integer> iter2 = Arrays.asList(1, 2);
        Iterable<Boolean> iter3 = Arrays.asList(true, false);

        ObjIterator<String> iter = Iterators.zip(iter1, iter2, iter3, zipFunc);
        assertEquals("a1true", iter.next());
        assertEquals("b2false", iter.next());
        assertFalse(iter.hasNext());
    }

    @Test
    public void testZipTwoIteratorsWithDefaults() {
        BiFunction<String, Integer, String> zipFunc = (a, b) -> a + b;

        Iterator<String> iter1 = Arrays.asList("a", "b", "c").iterator();
        Iterator<Integer> iter2 = Arrays.asList(1).iterator();

        ObjIterator<String> iter = Iterators.zip(iter1, iter2, "X", 0, zipFunc);
        assertEquals("a1", iter.next());
        assertEquals("b0", iter.next());
        assertEquals("c0", iter.next());
        assertFalse(iter.hasNext());

        // Test when first iterator is shorter
        iter1 = Arrays.asList("a").iterator();
        iter2 = Arrays.asList(1, 2, 3).iterator();

        iter = Iterators.zip(iter1, iter2, "X", 0, zipFunc);
        assertEquals("a1", iter.next());
        assertEquals("X2", iter.next());
        assertEquals("X3", iter.next());
        assertFalse(iter.hasNext());
    }

    @Test
    public void testZipTwoIterablesWithDefaults() {
        BiFunction<String, Integer, String> zipFunc = (a, b) -> a + b;

        Iterable<String> iter1 = Arrays.asList("a", "b", "c");
        Iterable<Integer> iter2 = Arrays.asList(1);

        ObjIterator<String> iter = Iterators.zip(iter1, iter2, "X", 0, zipFunc);
        assertEquals("a1", iter.next());
        assertEquals("b0", iter.next());
        assertEquals("c0", iter.next());
        assertFalse(iter.hasNext());
    }

    @Test
    public void testZipThreeIteratorsWithDefaults() {
        TriFunction<String, Integer, Boolean, String> zipFunc = (a, b, c) -> a + b + c;

        Iterator<String> iter1 = Arrays.asList("a").iterator();
        Iterator<Integer> iter2 = Arrays.asList(1, 2).iterator();
        Iterator<Boolean> iter3 = Arrays.asList(true, false, true).iterator();

        ObjIterator<String> iter = Iterators.zip(iter1, iter2, iter3, "X", 0, false, zipFunc);
        assertEquals("a1true", iter.next());
        assertEquals("X2false", iter.next());
        assertEquals("X0true", iter.next());
        assertFalse(iter.hasNext());
    }

    @Test
    public void testZipThreeIterablesWithDefaults() {
        TriFunction<String, Integer, Boolean, String> zipFunc = (a, b, c) -> a + b + c;

        Iterable<String> iter1 = Arrays.asList("a");
        Iterable<Integer> iter2 = Arrays.asList(1, 2);
        Iterable<Boolean> iter3 = Arrays.asList(true, false, true);

        ObjIterator<String> iter = Iterators.zip(iter1, iter2, iter3, "X", 0, false, zipFunc);
        assertEquals("a1true", iter.next());
        assertEquals("X2false", iter.next());
        assertEquals("X0true", iter.next());
        assertFalse(iter.hasNext());
    }

    @Test
    public void testUnzipIterator() {
        Iterator<String> inputIter = Arrays.asList("a1", "b2", "c3").iterator();

        BiConsumer<String, Pair<String, Integer>> unzipFunc = (str, pair) -> {
            pair.set(str.substring(0, 1), Integer.parseInt(str.substring(1)));
        };

        BiIterator<String, Integer> iter = Iterators.unzip(inputIter, unzipFunc);

        Pair<String, Integer> pair = iter.next();
        assertEquals("a", pair.left());
        assertEquals(1, pair.right());

        pair = iter.next();
        assertEquals("b", pair.left());
        assertEquals(2, pair.right());

        pair = iter.next();
        assertEquals("c", pair.left());
        assertEquals(3, pair.right());

        assertFalse(iter.hasNext());
    }

    @Test
    public void testUnzipIterable() {
        Iterable<String> input = Arrays.asList("a1", "b2", "c3");

        BiConsumer<String, Pair<String, Integer>> unzipFunc = (str, pair) -> {
            pair.set(str.substring(0, 1), Integer.parseInt(str.substring(1)));
        };

        BiIterator<String, Integer> iter = Iterators.unzip(input, unzipFunc);

        Pair<String, Integer> pair = iter.next();
        assertEquals("a", pair.left());
        assertEquals(1, pair.right());

        pair = iter.next();
        assertEquals("b", pair.left());
        assertEquals(2, pair.right());

        pair = iter.next();
        assertEquals("c", pair.left());
        assertEquals(3, pair.right());

        assertFalse(iter.hasNext());
    }

    @Test
    public void testUnzippIterator() {
        Iterator<String> inputIter = Arrays.asList("a1x", "b2y", "c3z").iterator();

        BiConsumer<String, Triple<String, Integer, String>> unzipFunc = (str, triple) -> {
            triple.set(str.substring(0, 1), Integer.parseInt(str.substring(1, 2)), str.substring(2));
        };

        TriIterator<String, Integer, String> iter = Iterators.unzipp(inputIter, unzipFunc);

        Triple<String, Integer, String> triple = iter.next();
        assertEquals("a", triple.left());
        assertEquals(1, triple.middle());
        assertEquals("x", triple.right());

        triple = iter.next();
        assertEquals("b", triple.left());
        assertEquals(2, triple.middle());
        assertEquals("y", triple.right());

        triple = iter.next();
        assertEquals("c", triple.left());
        assertEquals(3, triple.middle());
        assertEquals("z", triple.right());

        assertFalse(iter.hasNext());
    }

    @Test
    public void testUnzippIterable() {
        Iterable<String> input = Arrays.asList("a1x", "b2y", "c3z");

        BiConsumer<String, Triple<String, Integer, String>> unzipFunc = (str, triple) -> {
            triple.set(str.substring(0, 1), Integer.parseInt(str.substring(1, 2)), str.substring(2));
        };

        TriIterator<String, Integer, String> iter = Iterators.unzipp(input, unzipFunc);

        Triple<String, Integer, String> triple = iter.next();
        assertEquals("a", triple.left());
        assertEquals(1, triple.middle());
        assertEquals("x", triple.right());

        triple = iter.next();
        assertEquals("b", triple.left());
        assertEquals(2, triple.middle());
        assertEquals("y", triple.right());

        triple = iter.next();
        assertEquals("c", triple.left());
        assertEquals(3, triple.middle());
        assertEquals("z", triple.right());

        assertFalse(iter.hasNext());
    }

    @Test
    public void testAdvance() {
        // Test with negative number
        assertThrows(IllegalArgumentException.class, () -> Iterators.advance(Arrays.asList(1, 2, 3).iterator(), -1));

        // Test advance 0
        List<String> list = Arrays.asList("a", "b", "c");
        Iterator<String> iter = list.iterator();
        assertEquals(0, Iterators.advance(iter, 0));
        assertEquals("a", iter.next());

        // Test normal advance
        iter = list.iterator();
        assertEquals(2, Iterators.advance(iter, 2));
        assertEquals("c", iter.next());

        // Test advance beyond size
        iter = list.iterator();
        assertEquals(3, Iterators.advance(iter, 10));
        assertFalse(iter.hasNext());
    }

    @Test
    public void testSkip() {
        // Test with null iterator
        ObjIterator<String> iter = Iterators.skip(null, 5);
        assertFalse(iter.hasNext());

        // Test with negative n
        assertThrows(IllegalArgumentException.class, () -> Iterators.skip(Arrays.asList("a").iterator(), -1));

        // Test skip 0
        List<String> list = Arrays.asList("a", "b", "c");
        iter = Iterators.skip(list.iterator(), 0);
        assertEquals("a", iter.next());
        assertEquals("b", iter.next());
        assertEquals("c", iter.next());
        assertFalse(iter.hasNext());

        // Test normal skip
        iter = Iterators.skip(list.iterator(), 1);
        assertEquals("b", iter.next());
        assertEquals("c", iter.next());
        assertFalse(iter.hasNext());

        // Test skip all
        iter = Iterators.skip(list.iterator(), 3);
        assertFalse(iter.hasNext());

        // Test skip beyond size
        iter = Iterators.skip(list.iterator(), 10);
        assertFalse(iter.hasNext());
    }

    @Test
    public void testLimit() {
        // Test with null iterator
        ObjIterator<String> iter = Iterators.limit(null, 5);
        assertFalse(iter.hasNext());

        // Test with negative count
        assertThrows(IllegalArgumentException.class, () -> Iterators.limit(Arrays.asList("a").iterator(), -1));

        // Test limit 0
        List<String> list = Arrays.asList("a", "b", "c");
        iter = Iterators.limit(list.iterator(), 0);
        assertFalse(iter.hasNext());

        // Test normal limit
        iter = Iterators.limit(list.iterator(), 2);
        assertEquals("a", iter.next());
        assertEquals("b", iter.next());
        assertFalse(iter.hasNext());

        // Test limit beyond size
        iter = Iterators.limit(list.iterator(), 10);
        assertEquals("a", iter.next());
        assertEquals("b", iter.next());
        assertEquals("c", iter.next());
        assertFalse(iter.hasNext());

        // Test limit with Long.MAX_VALUE
        iter = Iterators.limit(list.iterator(), Long.MAX_VALUE);
        assertEquals("a", iter.next());
        assertEquals("b", iter.next());
        assertEquals("c", iter.next());
        assertFalse(iter.hasNext());
    }

    @Test
    public void testSkipAndLimitIterator() {
        // Test with null iterator
        ObjIterator<String> iter = Iterators.skipAndLimit((List<String>) null, 1, 2);
        assertFalse(iter.hasNext());

        // Test with offset 0 and count MAX_VALUE
        List<String> list = Arrays.asList("a", "b", "c", "d", "e");
        iter = Iterators.skipAndLimit(list.iterator(), 0, Long.MAX_VALUE);
        assertEquals("a", iter.next());
        assertEquals("b", iter.next());
        assertEquals("c", iter.next());
        assertEquals("d", iter.next());
        assertEquals("e", iter.next());
        assertFalse(iter.hasNext());

        // Test normal skip and limit
        iter = Iterators.skipAndLimit(list.iterator(), 1, 2);
        assertEquals("b", iter.next());
        assertEquals("c", iter.next());
        assertFalse(iter.hasNext());

        // Test skip beyond size
        iter = Iterators.skipAndLimit(list.iterator(), 10, 2);
        assertFalse(iter.hasNext());
    }

    @Test
    public void testSkipAndLimitIterable() {
        // Test with null iterable
        ObjIterator<String> iter = Iterators.skipAndLimit((List<String>) null, 1, 2);
        assertFalse(iter.hasNext());

        // Test normal skip and limit
        List<String> list = Arrays.asList("a", "b", "c", "d", "e");
        iter = Iterators.skipAndLimit(list, 2, 2);
        assertEquals("c", iter.next());
        assertEquals("d", iter.next());
        assertFalse(iter.hasNext());
    }

    @Test
    public void testSkipNullsIterable() {
        // Test with null iterable
        ObjIterator<String> iter = Iterators.skipNulls((Iterable<String>) null);
        assertFalse(iter.hasNext());

        // Test with nulls
        List<String> list = Arrays.asList("a", null, "b", null, "c");
        iter = Iterators.skipNulls(list);
        assertEquals("a", iter.next());
        assertEquals("b", iter.next());
        assertEquals("c", iter.next());
        assertFalse(iter.hasNext());

        // Test with all nulls
        list = Arrays.asList(null, null, null);
        iter = Iterators.skipNulls(list);
        assertFalse(iter.hasNext());
    }

    @Test
    public void testSkipNullsIterator() {
        // Test with null iterator
        ObjIterator<String> iter = Iterators.skipNulls((Iterator<String>) null);
        assertFalse(iter.hasNext());

        // Test with nulls
        List<String> list = Arrays.asList("a", null, "b", null, "c");
        iter = Iterators.skipNulls(list.iterator());
        assertEquals("a", iter.next());
        assertEquals("b", iter.next());
        assertEquals("c", iter.next());
        assertFalse(iter.hasNext());
    }

    @Test
    public void testDistinctIterable() {
        // Test with null iterable
        ObjIterator<String> iter = Iterators.distinct((Iterable<String>) null);
        assertFalse(iter.hasNext());

        // Test with duplicates
        List<String> list = Arrays.asList("a", "b", "a", "c", "b", "d");
        iter = Iterators.distinct(list);
        assertEquals("a", iter.next());
        assertEquals("b", iter.next());
        assertEquals("c", iter.next());
        assertEquals("d", iter.next());
        assertFalse(iter.hasNext());
    }

    @Test
    public void testDistinctIterator() {
        // Test with null iterator
        ObjIterator<String> iter = Iterators.distinct((Iterator<String>) null);
        assertFalse(iter.hasNext());

        // Test with duplicates
        List<String> list = Arrays.asList("a", "b", "a", "c", "b", "d");
        iter = Iterators.distinct(list.iterator());
        assertEquals("a", iter.next());
        assertEquals("b", iter.next());
        assertEquals("c", iter.next());
        assertEquals("d", iter.next());
        assertFalse(iter.hasNext());

        // Test with all duplicates
        list = Arrays.asList("a", "a", "a");
        iter = Iterators.distinct(list.iterator());
        assertEquals("a", iter.next());
        assertFalse(iter.hasNext());
    }

    @Test
    public void testDistinctByIterable() {
        // Test with null iterable
        ObjIterator<String> iter = Iterators.distinctBy((Iterable<String>) null, String::length);
        assertFalse(iter.hasNext());

        // Test with null key extractor
        assertThrows(IllegalArgumentException.class, () -> Iterators.distinctBy(Arrays.asList("a"), null));

        // Test with key extractor
        List<String> list = Arrays.asList("a", "bb", "ccc", "dd", "e");
        iter = Iterators.distinctBy(list, String::length);
        assertEquals("a", iter.next());
        assertEquals("bb", iter.next());
        assertEquals("ccc", iter.next());
        assertFalse(iter.hasNext());
    }

    @Test
    public void testDistinctByIterator() {
        // Test with null iterator
        ObjIterator<String> iter = Iterators.distinctBy((Iterator<String>) null, String::length);
        assertFalse(iter.hasNext());

        // Test with null key extractor
        assertThrows(IllegalArgumentException.class, () -> Iterators.distinctBy(Arrays.asList("a").iterator(), null));

        // Test with key extractor
        List<String> list = Arrays.asList("a", "bb", "ccc", "dd", "e");
        iter = Iterators.distinctBy(list.iterator(), String::length);
        assertEquals("a", iter.next());
        assertEquals("bb", iter.next());
        assertEquals("ccc", iter.next());
        assertFalse(iter.hasNext());
    }

    @Test
    public void testFilterIterable() {
        // Test with null iterable
        ObjIterator<String> iter = Iterators.filter((Iterable<String>) null, s -> true);
        assertFalse(iter.hasNext());

        // Test with predicate
        List<Integer> list = Arrays.asList(1, 2, 3, 4, 5);
        ObjIterator<Integer> intIter = Iterators.filter(list, n -> n % 2 == 0);
        assertEquals(2, intIter.next());
        assertEquals(4, intIter.next());
        assertFalse(intIter.hasNext());
    }

    @Test
    public void testFilterIterator() {
        // Test with null iterator
        ObjIterator<String> iter = Iterators.filter((Iterator<String>) null, s -> true);
        assertFalse(iter.hasNext());

        // Test with predicate
        List<Integer> list = Arrays.asList(1, 2, 3, 4, 5);
        ObjIterator<Integer> intIter = Iterators.filter(list.iterator(), n -> n % 2 == 0);
        assertEquals(2, intIter.next());
        assertEquals(4, intIter.next());
        assertFalse(intIter.hasNext());

        // Test filter all
        intIter = Iterators.filter(list.iterator(), n -> n > 10);
        assertFalse(intIter.hasNext());

        // Test filter none
        intIter = Iterators.filter(list.iterator(), n -> n > 0);
        assertEquals(1, intIter.next());
        assertEquals(2, intIter.next());
        assertEquals(3, intIter.next());
        assertEquals(4, intIter.next());
        assertEquals(5, intIter.next());
        assertFalse(intIter.hasNext());
    }

    @Test
    public void testTakeWhileIterable() {
        // Test with null iterable
        ObjIterator<String> iter = Iterators.takeWhile((Iterable<String>) null, s -> true);
        assertFalse(iter.hasNext());

        // Test with predicate
        List<Integer> list = Arrays.asList(1, 2, 3, 4, 5);
        ObjIterator<Integer> intIter = Iterators.takeWhile(list, n -> n < 4);
        assertEquals(1, intIter.next());
        assertEquals(2, intIter.next());
        assertEquals(3, intIter.next());
        assertFalse(intIter.hasNext());
    }

    @Test
    public void testTakeWhileIterator() {
        // Test with null iterator
        ObjIterator<String> iter = Iterators.takeWhile((Iterator<String>) null, s -> true);
        assertFalse(iter.hasNext());

        // Test with predicate
        List<Integer> list = Arrays.asList(1, 2, 3, 4, 5);
        ObjIterator<Integer> intIter = Iterators.takeWhile(list.iterator(), n -> n < 4);
        assertEquals(1, intIter.next());
        assertEquals(2, intIter.next());
        assertEquals(3, intIter.next());
        assertFalse(intIter.hasNext());

        // Test take none
        intIter = Iterators.takeWhile(list.iterator(), n -> n < 0);
        assertFalse(intIter.hasNext());

        // Test take all
        intIter = Iterators.takeWhile(list.iterator(), n -> n < 10);
        assertEquals(1, intIter.next());
        assertEquals(2, intIter.next());
        assertEquals(3, intIter.next());
        assertEquals(4, intIter.next());
        assertEquals(5, intIter.next());
        assertFalse(intIter.hasNext());
    }

    @Test
    public void testTakeWhileInclusiveIterable() {
        // Test with null iterable
        ObjIterator<String> iter = Iterators.takeWhileInclusive((Iterable<String>) null, s -> true);
        assertFalse(iter.hasNext());

        // Test with predicate
        List<Integer> list = Arrays.asList(1, 2, 3, 4, 5);
        ObjIterator<Integer> intIter = Iterators.takeWhileInclusive(list, n -> n < 4);
        assertEquals(1, intIter.next());
        assertEquals(2, intIter.next());
        assertEquals(3, intIter.next());
        assertEquals(4, intIter.next()); // includes first false element
        assertFalse(intIter.hasNext());
    }

    @Test
    public void testTakeWhileInclusiveIterator() {
        // Test with null iterator
        ObjIterator<String> iter = Iterators.takeWhileInclusive((Iterator<String>) null, s -> true);
        assertFalse(iter.hasNext());

        // Test with predicate
        List<Integer> list = Arrays.asList(1, 2, 3, 4, 5);
        ObjIterator<Integer> intIter = Iterators.takeWhileInclusive(list.iterator(), n -> n < 4);
        assertEquals(1, intIter.next());
        assertEquals(2, intIter.next());
        assertEquals(3, intIter.next());
        assertEquals(4, intIter.next()); // includes first false element
        assertFalse(intIter.hasNext());

        // Test take none but include first
        intIter = Iterators.takeWhileInclusive(list.iterator(), n -> n < 0);
        assertEquals(1, intIter.next()); // includes first element that fails
        assertFalse(intIter.hasNext());
    }

    @Test
    public void testDropWhileIterable() {
        // Test with null iterable
        ObjIterator<String> iter = Iterators.dropWhile((Iterable<String>) null, s -> true);
        assertFalse(iter.hasNext());

        // Test with predicate
        List<Integer> list = Arrays.asList(1, 2, 3, 4, 5);
        ObjIterator<Integer> intIter = Iterators.dropWhile(list, n -> n < 4);
        assertEquals(4, intIter.next());
        assertEquals(5, intIter.next());
        assertFalse(intIter.hasNext());
    }

    @Test
    public void testDropWhileIterator() {
        // Test with null iterator
        ObjIterator<String> iter = Iterators.dropWhile((Iterator<String>) null, s -> true);
        assertFalse(iter.hasNext());

        // Test with predicate
        List<Integer> list = Arrays.asList(1, 2, 3, 4, 5);
        ObjIterator<Integer> intIter = Iterators.dropWhile(list.iterator(), n -> n < 4);
        assertEquals(4, intIter.next());
        assertEquals(5, intIter.next());
        assertFalse(intIter.hasNext());

        // Test drop all
        intIter = Iterators.dropWhile(list.iterator(), n -> n < 10);
        assertFalse(intIter.hasNext());

        // Test drop none
        intIter = Iterators.dropWhile(list.iterator(), n -> n < 0);
        assertEquals(1, intIter.next());
        assertEquals(2, intIter.next());
        assertEquals(3, intIter.next());
        assertEquals(4, intIter.next());
        assertEquals(5, intIter.next());
        assertFalse(intIter.hasNext());
    }

    @Test
    public void testSkipUntilIterable() {
        // Test with null iterable
        ObjIterator<String> iter = Iterators.skipUntil((Iterable<String>) null, s -> true);
        assertFalse(iter.hasNext());

        // Test with predicate
        List<Integer> list = Arrays.asList(1, 2, 3, 4, 5);
        ObjIterator<Integer> intIter = Iterators.skipUntil(list, n -> n >= 4);
        assertEquals(4, intIter.next());
        assertEquals(5, intIter.next());
        assertFalse(intIter.hasNext());
    }

    @Test
    public void testSkipUntilIterator() {
        // Test with null iterator
        ObjIterator<String> iter = Iterators.skipUntil((Iterator<String>) null, s -> true);
        assertFalse(iter.hasNext());

        // Test with predicate
        List<Integer> list = Arrays.asList(1, 2, 3, 4, 5);
        ObjIterator<Integer> intIter = Iterators.skipUntil(list.iterator(), n -> n >= 4);
        assertEquals(4, intIter.next());
        assertEquals(5, intIter.next());
        assertFalse(intIter.hasNext());

        // Test skip all if predicate never true
        intIter = Iterators.skipUntil(list.iterator(), n -> n > 10);
        assertFalse(intIter.hasNext());

        // Test skip none if predicate immediately true
        intIter = Iterators.skipUntil(list.iterator(), n -> n >= 1);
        assertEquals(1, intIter.next());
        assertEquals(2, intIter.next());
        assertEquals(3, intIter.next());
        assertEquals(4, intIter.next());
        assertEquals(5, intIter.next());
        assertFalse(intIter.hasNext());
    }

    @Test
    public void testMapIterable() {
        // Test with null iterable
        ObjIterator<String> iter = Iterators.map((Iterable<Integer>) null, Object::toString);
        assertFalse(iter.hasNext());

        // Test with mapper
        List<Integer> list = Arrays.asList(1, 2, 3);
        iter = Iterators.map(list, n -> "num" + n);
        assertEquals("num1", iter.next());
        assertEquals("num2", iter.next());
        assertEquals("num3", iter.next());
        assertFalse(iter.hasNext());
    }

    @Test
    public void testMapIterator() {
        // Test with null iterator
        ObjIterator<String> iter = Iterators.map((Iterator<Integer>) null, Object::toString);
        assertFalse(iter.hasNext());

        // Test with mapper
        List<Integer> list = Arrays.asList(1, 2, 3);
        iter = Iterators.map(list.iterator(), n -> "num" + n);
        assertEquals("num1", iter.next());
        assertEquals("num2", iter.next());
        assertEquals("num3", iter.next());
        assertFalse(iter.hasNext());
    }

    @Test
    public void testFlatMapIterable() {
        // Test with null iterable
        ObjIterator<String> iter = Iterators.flatMap((Iterable<String>) null, s -> Arrays.asList(s));
        assertFalse(iter.hasNext());

        // Test with mapper
        List<String> list = Arrays.asList("a", "b", "c");
        iter = Iterators.flatMap(list, s -> Arrays.asList(s + "1", s + "2"));
        assertEquals("a1", iter.next());
        assertEquals("a2", iter.next());
        assertEquals("b1", iter.next());
        assertEquals("b2", iter.next());
        assertEquals("c1", iter.next());
        assertEquals("c2", iter.next());
        assertFalse(iter.hasNext());

        // Test with empty results
        iter = Iterators.flatMap(list, s -> Collections.emptyList());
        assertFalse(iter.hasNext());

        // Test with null results
        iter = Iterators.flatMap(list, s -> null);
        assertFalse(iter.hasNext());
    }

    @Test
    public void testFlatMapIterator() {
        // Test with null iterator
        ObjIterator<String> iter = Iterators.flatMap((Iterator<String>) null, s -> Arrays.asList(s));
        assertFalse(iter.hasNext());

        // Test with mapper
        List<String> list = Arrays.asList("a", "b", "c");
        iter = Iterators.flatMap(list.iterator(), s -> Arrays.asList(s + "1", s + "2"));
        assertEquals("a1", iter.next());
        assertEquals("a2", iter.next());
        assertEquals("b1", iter.next());
        assertEquals("b2", iter.next());
        assertEquals("c1", iter.next());
        assertEquals("c2", iter.next());
        assertFalse(iter.hasNext());
    }

    @Test
    public void testFlatmapIterableArray() {
        // Test with null iterable
        ObjIterator<String> iter = Iterators.flatmap((Iterable<String>) null, s -> new String[] { s });
        assertFalse(iter.hasNext());

        // Test with mapper
        List<String> list = Arrays.asList("a", "b", "c");
        iter = Iterators.flatmap(list, s -> new String[] { s + "1", s + "2" });
        assertEquals("a1", iter.next());
        assertEquals("a2", iter.next());
        assertEquals("b1", iter.next());
        assertEquals("b2", iter.next());
        assertEquals("c1", iter.next());
        assertEquals("c2", iter.next());
        assertFalse(iter.hasNext());

        // Test with empty arrays
        iter = Iterators.flatmap(list, s -> new String[0]);
        assertFalse(iter.hasNext());

        // Test with null arrays
        iter = Iterators.flatmap(list, s -> null);
        assertFalse(iter.hasNext());
    }

    @Test
    public void testFlatmapIteratorArray() {
        // Test with null iterator
        ObjIterator<String> iter = Iterators.flatmap((Iterator<String>) null, s -> new String[] { s });
        assertFalse(iter.hasNext());

        // Test with mapper
        List<String> list = Arrays.asList("a", "b", "c");
        iter = Iterators.flatmap(list.iterator(), s -> new String[] { s + "1", s + "2" });
        assertEquals("a1", iter.next());
        assertEquals("a2", iter.next());
        assertEquals("b1", iter.next());
        assertEquals("b2", iter.next());
        assertEquals("c1", iter.next());
        assertEquals("c2", iter.next());
        assertFalse(iter.hasNext());
    }

    @Test
    public void testForEachOneParam() throws Exception {
        List<String> result = new ArrayList<>();
        List<String> list = Arrays.asList("a", "b", "c");

        Iterators.forEach(list.iterator(), result::add);
        assertEquals(Arrays.asList("a", "b", "c"), result);

        // Test with empty iterator
        result.clear();
        Iterators.forEach(Collections.<String> emptyIterator(), result::add);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testForEachTwoParams() throws Exception {
        List<String> result = new ArrayList<>();
        List<String> list = Arrays.asList("a", "b", "c");
        boolean[] completed = { false };

        Iterators.forEach(list.iterator(), result::add, () -> completed[0] = true);
        assertEquals(Arrays.asList("a", "b", "c"), result);
        assertTrue(completed[0]);

        // Test with empty iterator
        result.clear();
        completed[0] = false;
        Iterators.forEach(Collections.<String> emptyIterator(), result::add, () -> completed[0] = true);
        assertTrue(result.isEmpty());
        assertTrue(completed[0]);
    }

    @Test
    public void testForEachThreeParams() throws Exception {
        List<String> result = new ArrayList<>();
        List<String> list = Arrays.asList("a", "b", "c", "d", "e");

        Iterators.forEach(list.iterator(), 1L, 3L, result::add);
        assertEquals(Arrays.asList("b", "c", "d"), result);

        // Test with offset beyond size
        result.clear();
        Iterators.forEach(list.iterator(), 10L, 3L, result::add);
        assertTrue(result.isEmpty());

        // Test with count 0
        result.clear();
        Iterators.forEach(list.iterator(), 0L, 0L, result::add);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testForEachFourParams() throws Exception {
        List<String> result = new ArrayList<>();
        List<String> list = Arrays.asList("a", "b", "c", "d", "e");
        boolean[] completed = { false };

        Iterators.forEach(list.iterator(), 1L, 3L, result::add, () -> completed[0] = true);
        assertEquals(Arrays.asList("b", "c", "d"), result);
        assertTrue(completed[0]);

        // Test with offset and count that exceed size
        result.clear();
        completed[0] = false;
        Iterators.forEach(list.iterator(), 2L, 10L, result::add, () -> completed[0] = true);
        assertEquals(Arrays.asList("c", "d", "e"), result);
        assertTrue(completed[0]);
    }
}
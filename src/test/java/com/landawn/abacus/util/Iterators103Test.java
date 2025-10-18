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

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.u.Nullable;
import com.landawn.abacus.util.function.TriFunction;

@Tag("new-test")
public class Iterators103Test extends TestBase {

    @Test
    public void testGet() {
        Nullable<String> result = Iterators.get(null, 0);
        assertFalse(result.isPresent());

        Iterator<String> emptyIter = Collections.<String> emptyIterator();
        result = Iterators.get(emptyIter, 0);
        assertFalse(result.isPresent());

        List<String> list = Arrays.asList("a", "b", "c", "d");
        result = Iterators.get(list.iterator(), 0);
        assertTrue(result.isPresent());
        assertEquals("a", result.get());

        result = Iterators.get(list.iterator(), 2);
        assertTrue(result.isPresent());
        assertEquals("c", result.get());

        result = Iterators.get(list.iterator(), 4);
        assertFalse(result.isPresent());

        assertThrows(IllegalArgumentException.class, () -> Iterators.get(list.iterator(), -1));
    }

    @Test
    public void testOccurrencesOf() {
        assertEquals(0, Iterators.occurrencesOf(null, "test"));

        assertEquals(0, Iterators.occurrencesOf(Collections.emptyIterator(), "test"));

        List<String> list = Arrays.asList("a", "b", "a", "c", "a");
        assertEquals(3, Iterators.occurrencesOf(list.iterator(), "a"));
        assertEquals(1, Iterators.occurrencesOf(list.iterator(), "b"));
        assertEquals(0, Iterators.occurrencesOf(list.iterator(), "d"));

        List<String> listWithNulls = Arrays.asList("a", null, "b", null, null);
        assertEquals(3, Iterators.occurrencesOf(listWithNulls.iterator(), null));
        assertEquals(1, Iterators.occurrencesOf(listWithNulls.iterator(), "a"));
    }

    @Test
    public void testCountNoParams() {
        assertEquals(0, Iterators.count(null));

        assertEquals(0, Iterators.count(Collections.emptyIterator()));

        List<String> list = Arrays.asList("a", "b", "c");
        assertEquals(3, Iterators.count(list.iterator()));

        List<Integer> numbers = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            numbers.add(i);
        }
        assertEquals(100, Iterators.count(numbers.iterator()));
    }

    @Test
    public void testCountWithPredicate() {
        assertEquals(0, Iterators.count(null, str -> true));

        assertThrows(IllegalArgumentException.class, () -> Iterators.count(Arrays.asList("a", "b").iterator(), null));

        assertEquals(0, Iterators.count(Collections.emptyIterator(), str -> true));

        List<Integer> numbers = Arrays.asList(1, 2, 3, 4, 5, 6);
        assertEquals(3, Iterators.count(numbers.iterator(), n -> n % 2 == 0));
        assertEquals(3, Iterators.count(numbers.iterator(), n -> n % 2 == 1));
        assertEquals(0, Iterators.count(numbers.iterator(), n -> n > 10));
        assertEquals(6, Iterators.count(numbers.iterator(), n -> n > 0));
    }

    @Test
    public void testIndexOfTwoParams() {
        assertEquals(CommonUtil.INDEX_NOT_FOUND, Iterators.indexOf(null, "test"));

        assertEquals(CommonUtil.INDEX_NOT_FOUND, Iterators.indexOf(Collections.emptyIterator(), "test"));

        List<String> list = Arrays.asList("a", "b", "c", "b", "d");
        assertEquals(0, Iterators.indexOf(list.iterator(), "a"));
        assertEquals(1, Iterators.indexOf(list.iterator(), "b"));
        assertEquals(4, Iterators.indexOf(list.iterator(), "d"));

        assertEquals(CommonUtil.INDEX_NOT_FOUND, Iterators.indexOf(list.iterator(), "e"));

        List<String> listWithNull = Arrays.asList("a", null, "b", null);
        assertEquals(1, Iterators.indexOf(listWithNull.iterator(), null));
    }

    @Test
    public void testIndexOfThreeParams() {
        assertEquals(CommonUtil.INDEX_NOT_FOUND, Iterators.indexOf(null, "test", 0));

        List<String> list = Arrays.asList("a", "b", "c", "b", "d");
        assertEquals(1, Iterators.indexOf(list.iterator(), "b", 0));

        assertEquals(3, Iterators.indexOf(list.iterator(), "b", 2));
        assertEquals(CommonUtil.INDEX_NOT_FOUND, Iterators.indexOf(list.iterator(), "a", 1));

        assertEquals(CommonUtil.INDEX_NOT_FOUND, Iterators.indexOf(list.iterator(), "a", 10));
    }

    @Test
    public void testElementsEqual() {
        Iterator<String> iter1 = null;
        Iterator<String> iter2 = null;
        assertTrue(Iterators.elementsEqual(iter1, iter2));

        assertTrue(Iterators.elementsEqual(Collections.emptyIterator(), Collections.emptyIterator()));

        List<String> list1 = Arrays.asList("a", "b", "c");
        List<String> list2 = Arrays.asList("a", "b", "c");
        assertTrue(Iterators.elementsEqual(list1.iterator(), list2.iterator()));

        List<String> list3 = Arrays.asList("a", "b");
        assertFalse(Iterators.elementsEqual(list1.iterator(), list3.iterator()));

        List<String> list4 = Arrays.asList("a", "b", "d");
        assertFalse(Iterators.elementsEqual(list1.iterator(), list4.iterator()));

        List<String> listWithNull1 = Arrays.asList("a", null, "c");
        List<String> listWithNull2 = Arrays.asList("a", null, "c");
        assertTrue(Iterators.elementsEqual(listWithNull1.iterator(), listWithNull2.iterator()));
    }

    @Test
    public void testRepeatWithInt() {
        {
            ObjIterator<String> iter = Iterators.repeat("test", 0);
            assertFalse(iter.hasNext());
        }

        {
            ObjIterator<String> iter = Iterators.repeat("hello", 3);
            assertTrue(iter.hasNext());
            assertEquals("hello", iter.next());
            assertEquals("hello", iter.next());
            assertEquals("hello", iter.next());
            assertFalse(iter.hasNext());

            assertThrows(NoSuchElementException.class, () -> iter.next());

            assertThrows(IllegalArgumentException.class, () -> Iterators.repeat("test", -1));
        }
    }

    @Test
    public void testRepeatWithLong() {
        ObjIterator<String> iter = Iterators.repeat("test", 0L);
        assertFalse(iter.hasNext());

        iter = Iterators.repeat("hello", 3L);
        assertTrue(iter.hasNext());
        assertEquals("hello", iter.next());
        assertEquals("hello", iter.next());
        assertEquals("hello", iter.next());
        assertFalse(iter.hasNext());

        iter = Iterators.repeat("x", 1000L);
        int count = 0;
        while (iter.hasNext()) {
            assertEquals("x", iter.next());
            count++;
        }
        assertEquals(1000, count);

        assertThrows(IllegalArgumentException.class, () -> Iterators.repeat("test", -1L));
    }

    @Test
    public void testRepeatElements() {
        ObjIterator<String> iter = Iterators.repeatElements(Collections.emptyList(), 3L);
        assertFalse(iter.hasNext());

        iter = Iterators.repeatElements(Arrays.asList("a", "b"), 0L);
        assertFalse(iter.hasNext());

        iter = Iterators.repeatElements(Arrays.asList("a", "b", "c"), 2L);
        assertEquals("a", iter.next());
        assertEquals("a", iter.next());
        assertEquals("b", iter.next());
        assertEquals("b", iter.next());
        assertEquals("c", iter.next());
        assertEquals("c", iter.next());
        assertFalse(iter.hasNext());

        assertThrows(IllegalArgumentException.class, () -> Iterators.repeatElements(Arrays.asList("a"), -1L));
    }

    @Test
    public void testRepeatCollection() {
        ObjIterator<String> iter = Iterators.repeatCollection(Collections.emptyList(), 3L);
        assertFalse(iter.hasNext());

        iter = Iterators.repeatCollection(Arrays.asList("a", "b"), 0L);
        assertFalse(iter.hasNext());

        iter = Iterators.repeatCollection(Arrays.asList("a", "b"), 2L);
        assertEquals("a", iter.next());
        assertEquals("b", iter.next());
        assertEquals("a", iter.next());
        assertEquals("b", iter.next());
        assertFalse(iter.hasNext());

        assertThrows(IllegalArgumentException.class, () -> Iterators.repeatCollection(Arrays.asList("a"), -1L));
    }

    @Test
    public void testRepeatElementsToSize() {
        assertThrows(IllegalArgumentException.class, () -> Iterators.repeatElementsToSize(Collections.emptyList(), 5L));

        ObjIterator<String> iter = Iterators.repeatElementsToSize(Arrays.asList("a"), 0L);
        assertFalse(iter.hasNext());

        iter = Iterators.repeatElementsToSize(Arrays.asList("a", "b"), 4L);
        assertEquals("a", iter.next());
        assertEquals("a", iter.next());
        assertEquals("b", iter.next());
        assertEquals("b", iter.next());
        assertFalse(iter.hasNext());

        iter = Iterators.repeatElementsToSize(Arrays.asList("a", "b", "c"), 7L);
        int count = 0;
        while (iter.hasNext()) {
            iter.next();
            count++;
        }
        assertEquals(7, count);

        assertThrows(IllegalArgumentException.class, () -> Iterators.repeatElementsToSize(Arrays.asList("a"), -1L));
    }

    @Test
    public void testRepeatCollectionToSize() {
        assertThrows(IllegalArgumentException.class, () -> Iterators.repeatCollectionToSize(Collections.emptyList(), 5L));

        ObjIterator<String> iter = Iterators.repeatCollectionToSize(Arrays.asList("a"), 0L);
        assertFalse(iter.hasNext());

        iter = Iterators.repeatCollectionToSize(Arrays.asList("a", "b"), 5L);
        assertEquals("a", iter.next());
        assertEquals("b", iter.next());
        assertEquals("a", iter.next());
        assertEquals("b", iter.next());
        assertEquals("a", iter.next());
        assertFalse(iter.hasNext());

        assertThrows(IllegalArgumentException.class, () -> Iterators.repeatCollectionToSize(Arrays.asList("a"), -1L));
    }

    @Test
    public void testCycleArray() {
        ObjIterator<String> iter = Iterators.cycle();
        assertFalse(iter.hasNext());

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
        ObjIterator<String> iter = Iterators.cycle(Collections.emptyList());
        assertFalse(iter.hasNext());

        iter = Iterators.cycle(Arrays.asList("x", "y"));
        for (int i = 0; i < 10; i++) {
            assertTrue(iter.hasNext());
            assertEquals("x", iter.next());
            assertEquals("y", iter.next());
        }
    }

    @Test
    public void testCycleIterableWithRounds() {
        ObjIterator<String> iter = Iterators.cycle(Collections.emptyList(), 5L);
        assertFalse(iter.hasNext());

        iter = Iterators.cycle(Arrays.asList("a", "b"), 0L);
        assertFalse(iter.hasNext());

        iter = Iterators.cycle(Arrays.asList("a", "b"), 1L);
        assertEquals("a", iter.next());
        assertEquals("b", iter.next());
        assertFalse(iter.hasNext());

        iter = Iterators.cycle(Arrays.asList("a", "b"), 3L);
        for (int i = 0; i < 3; i++) {
            assertEquals("a", iter.next());
            assertEquals("b", iter.next());
        }
        assertFalse(iter.hasNext());

        assertThrows(IllegalArgumentException.class, () -> Iterators.cycle(Arrays.asList("a"), -1L));
    }

    @Test
    public void testConcatBooleanArrays() {
        BooleanIterator iter = Iterators.concat(new boolean[][] {});
        assertFalse(iter.hasNext());

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
        CharIterator iter = Iterators.concat(new char[][] {});
        assertFalse(iter.hasNext());

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
        BooleanIterator iter = Iterators.concat(new BooleanIterator[] {});
        assertFalse(iter.hasNext());

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
        ObjIterator<String> iter = Iterators.concat(new String[][] {});
        assertFalse(iter.hasNext());

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
        ObjIterator<String> iter = Iterators.concat(new Iterator[] {});
        assertFalse(iter.hasNext());

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
        ObjIterator<Map.Entry<String, Integer>> iter = Iterators.concat(new Map[] {});
        assertFalse(iter.hasNext());

        Map<String, Integer> map1 = new HashMap<>();
        map1.put("a", 1);
        map1.put("b", 2);

        Map<String, Integer> map2 = new HashMap<>();
        map2.put("c", 3);

        Map<String, Integer> map3 = new HashMap<>();

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
        Collection<Iterator<String>> coll = Collections.emptyList();
        ObjIterator<String> iter = Iterators.concat(coll);
        assertFalse(iter.hasNext());

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
        Collection<Iterable<String>> coll = Collections.emptyList();
        ObjIterator<String> iter = Iterators.concatIterables(coll);
        assertFalse(iter.hasNext());

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
        BiIterator<String, Integer> iter = Iterators.concat(new BiIterator[] {});
        assertFalse(iter.hasNext());

        BiIterator<String, Integer> iter1 = BiIterator.of(CommonUtil.asLinkedHashMap("a", 1, "b", 2));
        BiIterator<String, Integer> iter2 = BiIterator.of(CommonUtil.asLinkedHashMap("c", 3));

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
        TriIterator<String, Integer, Boolean> iter = Iterators.concat(new TriIterator[] {});
        assertFalse(iter.hasNext());

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
        assertThrows(IllegalArgumentException.class, () -> Iterators.merge(Arrays.asList(1).iterator(), Arrays.asList(2).iterator(), null));

        BiFunction<Integer, Integer, MergeResult> selector = (a, b) -> a < b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND;
        ObjIterator<Integer> iter = Iterators.merge((Iterator<Integer>) null, (Iterator<Integer>) null, selector);
        assertFalse(iter.hasNext());

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
        assertThrows(IllegalArgumentException.class, () -> Iterators.merge(Arrays.asList(Arrays.asList(1).iterator()), null));

        BiFunction<Integer, Integer, MergeResult> selector = (a, b) -> a < b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND;
        Collection<Iterator<Integer>> coll = Collections.emptyList();
        ObjIterator<Integer> iter = Iterators.merge(coll, selector);
        assertFalse(iter.hasNext());

        coll = Arrays.asList(Arrays.asList(1, 2, 3).iterator());
        iter = Iterators.merge(coll, selector);
        assertEquals(1, iter.next());
        assertEquals(2, iter.next());
        assertEquals(3, iter.next());
        assertFalse(iter.hasNext());

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
        assertThrows(IllegalArgumentException.class, () -> Iterators.mergeIterables(Arrays.asList(Arrays.asList(1)), null));

        BiFunction<Integer, Integer, MergeResult> selector = (a, b) -> a < b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND;

        Collection<Iterable<Integer>> coll = Collections.emptyList();
        ObjIterator<Integer> iter = Iterators.mergeIterables(coll, selector);
        assertFalse(iter.hasNext());

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
        assertThrows(IllegalArgumentException.class, () -> Iterators.mergeSorted(Arrays.asList(1).iterator(), Arrays.asList(2).iterator(), null));

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
        BiFunction<String, Integer, String> zipFunc = (a, b) -> a + b;
        ObjIterator<String> iter = Iterators.zip((Iterator<String>) null, (Iterator<Integer>) null, zipFunc);
        assertFalse(iter.hasNext());

        Iterator<String> iter1 = Arrays.asList("a", "b", "c").iterator();
        Iterator<Integer> iter2 = Arrays.asList(1, 2, 3).iterator();

        iter = Iterators.zip(iter1, iter2, zipFunc);
        assertEquals("a1", iter.next());
        assertEquals("b2", iter.next());
        assertEquals("c3", iter.next());
        assertFalse(iter.hasNext());

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

        BiConsumer<String, Pair<String, Integer>> unzipFunction = (str, pair) -> {
            pair.set(str.substring(0, 1), Integer.parseInt(str.substring(1)));
        };

        BiIterator<String, Integer> iter = Iterators.unzip(inputIter, unzipFunction);

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

        BiConsumer<String, Pair<String, Integer>> unzipFunction = (str, pair) -> {
            pair.set(str.substring(0, 1), Integer.parseInt(str.substring(1)));
        };

        BiIterator<String, Integer> iter = Iterators.unzip(input, unzipFunction);

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

        BiConsumer<String, Triple<String, Integer, String>> unzipFunction = (str, triple) -> {
            triple.set(str.substring(0, 1), Integer.parseInt(str.substring(1, 2)), str.substring(2));
        };

        TriIterator<String, Integer, String> iter = Iterators.unzipp(inputIter, unzipFunction);

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

        BiConsumer<String, Triple<String, Integer, String>> unzipFunction = (str, triple) -> {
            triple.set(str.substring(0, 1), Integer.parseInt(str.substring(1, 2)), str.substring(2));
        };

        TriIterator<String, Integer, String> iter = Iterators.unzipp(input, unzipFunction);

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
        assertThrows(IllegalArgumentException.class, () -> Iterators.advance(Arrays.asList(1, 2, 3).iterator(), -1));

        List<String> list = Arrays.asList("a", "b", "c");
        Iterator<String> iter = list.iterator();
        assertEquals(0, Iterators.advance(iter, 0));
        assertEquals("a", iter.next());

        iter = list.iterator();
        assertEquals(2, Iterators.advance(iter, 2));
        assertEquals("c", iter.next());

        iter = list.iterator();
        assertEquals(3, Iterators.advance(iter, 10));
        assertFalse(iter.hasNext());
    }

    @Test
    public void testSkip() {
        ObjIterator<String> iter = Iterators.skip(null, 5);
        assertFalse(iter.hasNext());

        assertThrows(IllegalArgumentException.class, () -> Iterators.skip(Arrays.asList("a").iterator(), -1));

        List<String> list = Arrays.asList("a", "b", "c");
        iter = Iterators.skip(list.iterator(), 0);
        assertEquals("a", iter.next());
        assertEquals("b", iter.next());
        assertEquals("c", iter.next());
        assertFalse(iter.hasNext());

        iter = Iterators.skip(list.iterator(), 1);
        assertEquals("b", iter.next());
        assertEquals("c", iter.next());
        assertFalse(iter.hasNext());

        iter = Iterators.skip(list.iterator(), 3);
        assertFalse(iter.hasNext());

        iter = Iterators.skip(list.iterator(), 10);
        assertFalse(iter.hasNext());
    }

    @Test
    public void testLimit() {
        ObjIterator<String> iter = Iterators.limit(null, 5);
        assertFalse(iter.hasNext());

        assertThrows(IllegalArgumentException.class, () -> Iterators.limit(Arrays.asList("a").iterator(), -1));

        List<String> list = Arrays.asList("a", "b", "c");
        iter = Iterators.limit(list.iterator(), 0);
        assertFalse(iter.hasNext());

        iter = Iterators.limit(list.iterator(), 2);
        assertEquals("a", iter.next());
        assertEquals("b", iter.next());
        assertFalse(iter.hasNext());

        iter = Iterators.limit(list.iterator(), 10);
        assertEquals("a", iter.next());
        assertEquals("b", iter.next());
        assertEquals("c", iter.next());
        assertFalse(iter.hasNext());

        iter = Iterators.limit(list.iterator(), Long.MAX_VALUE);
        assertEquals("a", iter.next());
        assertEquals("b", iter.next());
        assertEquals("c", iter.next());
        assertFalse(iter.hasNext());
    }

    @Test
    public void testSkipAndLimitIterator() {
        ObjIterator<String> iter = Iterators.skipAndLimit((List<String>) null, 1, 2);
        assertFalse(iter.hasNext());

        List<String> list = Arrays.asList("a", "b", "c", "d", "e");
        iter = Iterators.skipAndLimit(list.iterator(), 0, Long.MAX_VALUE);
        assertEquals("a", iter.next());
        assertEquals("b", iter.next());
        assertEquals("c", iter.next());
        assertEquals("d", iter.next());
        assertEquals("e", iter.next());
        assertFalse(iter.hasNext());

        iter = Iterators.skipAndLimit(list.iterator(), 1, 2);
        assertEquals("b", iter.next());
        assertEquals("c", iter.next());
        assertFalse(iter.hasNext());

        iter = Iterators.skipAndLimit(list.iterator(), 10, 2);
        assertFalse(iter.hasNext());
    }

    @Test
    public void testSkipAndLimitIterable() {
        ObjIterator<String> iter = Iterators.skipAndLimit((List<String>) null, 1, 2);
        assertFalse(iter.hasNext());

        List<String> list = Arrays.asList("a", "b", "c", "d", "e");
        iter = Iterators.skipAndLimit(list, 2, 2);
        assertEquals("c", iter.next());
        assertEquals("d", iter.next());
        assertFalse(iter.hasNext());
    }

    @Test
    public void testSkipNullsIterable() {
        ObjIterator<String> iter = Iterators.skipNulls((Iterable<String>) null);
        assertFalse(iter.hasNext());

        List<String> list = Arrays.asList("a", null, "b", null, "c");
        iter = Iterators.skipNulls(list);
        assertEquals("a", iter.next());
        assertEquals("b", iter.next());
        assertEquals("c", iter.next());
        assertFalse(iter.hasNext());

        list = Arrays.asList(null, null, null);
        iter = Iterators.skipNulls(list);
        assertFalse(iter.hasNext());
    }

    @Test
    public void testSkipNullsIterator() {
        ObjIterator<String> iter = Iterators.skipNulls((Iterator<String>) null);
        assertFalse(iter.hasNext());

        List<String> list = Arrays.asList("a", null, "b", null, "c");
        iter = Iterators.skipNulls(list.iterator());
        assertEquals("a", iter.next());
        assertEquals("b", iter.next());
        assertEquals("c", iter.next());
        assertFalse(iter.hasNext());
    }

    @Test
    public void testDistinctIterable() {
        ObjIterator<String> iter = Iterators.distinct((Iterable<String>) null);
        assertFalse(iter.hasNext());

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
        ObjIterator<String> iter = Iterators.distinct((Iterator<String>) null);
        assertFalse(iter.hasNext());

        List<String> list = Arrays.asList("a", "b", "a", "c", "b", "d");
        iter = Iterators.distinct(list.iterator());
        assertEquals("a", iter.next());
        assertEquals("b", iter.next());
        assertEquals("c", iter.next());
        assertEquals("d", iter.next());
        assertFalse(iter.hasNext());

        list = Arrays.asList("a", "a", "a");
        iter = Iterators.distinct(list.iterator());
        assertEquals("a", iter.next());
        assertFalse(iter.hasNext());
    }

    @Test
    public void testDistinctByIterable() {
        ObjIterator<String> iter = Iterators.distinctBy((Iterable<String>) null, String::length);
        assertFalse(iter.hasNext());

        assertThrows(IllegalArgumentException.class, () -> Iterators.distinctBy(Arrays.asList("a"), null));

        List<String> list = Arrays.asList("a", "bb", "ccc", "dd", "e");
        iter = Iterators.distinctBy(list, String::length);
        assertEquals("a", iter.next());
        assertEquals("bb", iter.next());
        assertEquals("ccc", iter.next());
        assertFalse(iter.hasNext());
    }

    @Test
    public void testDistinctByIterator() {
        ObjIterator<String> iter = Iterators.distinctBy((Iterator<String>) null, String::length);
        assertFalse(iter.hasNext());

        assertThrows(IllegalArgumentException.class, () -> Iterators.distinctBy(Arrays.asList("a").iterator(), null));

        List<String> list = Arrays.asList("a", "bb", "ccc", "dd", "e");
        iter = Iterators.distinctBy(list.iterator(), String::length);
        assertEquals("a", iter.next());
        assertEquals("bb", iter.next());
        assertEquals("ccc", iter.next());
        assertFalse(iter.hasNext());
    }

    @Test
    public void testFilterIterable() {
        ObjIterator<String> iter = Iterators.filter((Iterable<String>) null, s -> true);
        assertFalse(iter.hasNext());

        List<Integer> list = Arrays.asList(1, 2, 3, 4, 5);
        ObjIterator<Integer> intIter = Iterators.filter(list, n -> n % 2 == 0);
        assertEquals(2, intIter.next());
        assertEquals(4, intIter.next());
        assertFalse(intIter.hasNext());
    }

    @Test
    public void testFilterIterator() {
        ObjIterator<String> iter = Iterators.filter((Iterator<String>) null, s -> true);
        assertFalse(iter.hasNext());

        List<Integer> list = Arrays.asList(1, 2, 3, 4, 5);
        ObjIterator<Integer> intIter = Iterators.filter(list.iterator(), n -> n % 2 == 0);
        assertEquals(2, intIter.next());
        assertEquals(4, intIter.next());
        assertFalse(intIter.hasNext());

        intIter = Iterators.filter(list.iterator(), n -> n > 10);
        assertFalse(intIter.hasNext());

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
        ObjIterator<String> iter = Iterators.takeWhile((Iterable<String>) null, s -> true);
        assertFalse(iter.hasNext());

        List<Integer> list = Arrays.asList(1, 2, 3, 4, 5);
        ObjIterator<Integer> intIter = Iterators.takeWhile(list, n -> n < 4);
        assertEquals(1, intIter.next());
        assertEquals(2, intIter.next());
        assertEquals(3, intIter.next());
        assertFalse(intIter.hasNext());
    }

    @Test
    public void testTakeWhileIterator() {
        ObjIterator<String> iter = Iterators.takeWhile((Iterator<String>) null, s -> true);
        assertFalse(iter.hasNext());

        List<Integer> list = Arrays.asList(1, 2, 3, 4, 5);
        ObjIterator<Integer> intIter = Iterators.takeWhile(list.iterator(), n -> n < 4);
        assertEquals(1, intIter.next());
        assertEquals(2, intIter.next());
        assertEquals(3, intIter.next());
        assertFalse(intIter.hasNext());

        intIter = Iterators.takeWhile(list.iterator(), n -> n < 0);
        assertFalse(intIter.hasNext());

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
        ObjIterator<String> iter = Iterators.takeWhileInclusive((Iterable<String>) null, s -> true);
        assertFalse(iter.hasNext());

        List<Integer> list = Arrays.asList(1, 2, 3, 4, 5);
        ObjIterator<Integer> intIter = Iterators.takeWhileInclusive(list, n -> n < 4);
        assertEquals(1, intIter.next());
        assertEquals(2, intIter.next());
        assertEquals(3, intIter.next());
        assertEquals(4, intIter.next());
        assertFalse(intIter.hasNext());
    }

    @Test
    public void testTakeWhileInclusiveIterator() {
        ObjIterator<String> iter = Iterators.takeWhileInclusive((Iterator<String>) null, s -> true);
        assertFalse(iter.hasNext());

        List<Integer> list = Arrays.asList(1, 2, 3, 4, 5);
        ObjIterator<Integer> intIter = Iterators.takeWhileInclusive(list.iterator(), n -> n < 4);
        assertEquals(1, intIter.next());
        assertEquals(2, intIter.next());
        assertEquals(3, intIter.next());
        assertEquals(4, intIter.next());
        assertFalse(intIter.hasNext());

        intIter = Iterators.takeWhileInclusive(list.iterator(), n -> n < 0);
        assertEquals(1, intIter.next());
        assertFalse(intIter.hasNext());
    }

    @Test
    public void testDropWhileIterable() {
        ObjIterator<String> iter = Iterators.dropWhile((Iterable<String>) null, s -> true);
        assertFalse(iter.hasNext());

        List<Integer> list = Arrays.asList(1, 2, 3, 4, 5);
        ObjIterator<Integer> intIter = Iterators.dropWhile(list, n -> n < 4);
        assertEquals(4, intIter.next());
        assertEquals(5, intIter.next());
        assertFalse(intIter.hasNext());
    }

    @Test
    public void testDropWhileIterator() {
        ObjIterator<String> iter = Iterators.dropWhile((Iterator<String>) null, s -> true);
        assertFalse(iter.hasNext());

        List<Integer> list = Arrays.asList(1, 2, 3, 4, 5);
        ObjIterator<Integer> intIter = Iterators.dropWhile(list.iterator(), n -> n < 4);
        assertEquals(4, intIter.next());
        assertEquals(5, intIter.next());
        assertFalse(intIter.hasNext());

        intIter = Iterators.dropWhile(list.iterator(), n -> n < 10);
        assertFalse(intIter.hasNext());

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
        ObjIterator<String> iter = Iterators.skipUntil((Iterable<String>) null, s -> true);
        assertFalse(iter.hasNext());

        List<Integer> list = Arrays.asList(1, 2, 3, 4, 5);
        ObjIterator<Integer> intIter = Iterators.skipUntil(list, n -> n >= 4);
        assertEquals(4, intIter.next());
        assertEquals(5, intIter.next());
        assertFalse(intIter.hasNext());
    }

    @Test
    public void testSkipUntilIterator() {
        ObjIterator<String> iter = Iterators.skipUntil((Iterator<String>) null, s -> true);
        assertFalse(iter.hasNext());

        List<Integer> list = Arrays.asList(1, 2, 3, 4, 5);
        ObjIterator<Integer> intIter = Iterators.skipUntil(list.iterator(), n -> n >= 4);
        assertEquals(4, intIter.next());
        assertEquals(5, intIter.next());
        assertFalse(intIter.hasNext());

        intIter = Iterators.skipUntil(list.iterator(), n -> n > 10);
        assertFalse(intIter.hasNext());

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
        ObjIterator<String> iter = Iterators.map((Iterable<Integer>) null, Object::toString);
        assertFalse(iter.hasNext());

        List<Integer> list = Arrays.asList(1, 2, 3);
        iter = Iterators.map(list, n -> "num" + n);
        assertEquals("num1", iter.next());
        assertEquals("num2", iter.next());
        assertEquals("num3", iter.next());
        assertFalse(iter.hasNext());
    }

    @Test
    public void testMapIterator() {
        ObjIterator<String> iter = Iterators.map((Iterator<Integer>) null, Object::toString);
        assertFalse(iter.hasNext());

        List<Integer> list = Arrays.asList(1, 2, 3);
        iter = Iterators.map(list.iterator(), n -> "num" + n);
        assertEquals("num1", iter.next());
        assertEquals("num2", iter.next());
        assertEquals("num3", iter.next());
        assertFalse(iter.hasNext());
    }

    @Test
    public void testFlatMapIterable() {
        ObjIterator<String> iter = Iterators.flatMap((Iterable<String>) null, s -> Arrays.asList(s));
        assertFalse(iter.hasNext());

        List<String> list = Arrays.asList("a", "b", "c");
        iter = Iterators.flatMap(list, s -> Arrays.asList(s + "1", s + "2"));
        assertEquals("a1", iter.next());
        assertEquals("a2", iter.next());
        assertEquals("b1", iter.next());
        assertEquals("b2", iter.next());
        assertEquals("c1", iter.next());
        assertEquals("c2", iter.next());
        assertFalse(iter.hasNext());

        iter = Iterators.flatMap(list, s -> Collections.emptyList());
        assertFalse(iter.hasNext());

        iter = Iterators.flatMap(list, s -> null);
        assertFalse(iter.hasNext());
    }

    @Test
    public void testFlatMapIterator() {
        ObjIterator<String> iter = Iterators.flatMap((Iterator<String>) null, s -> Arrays.asList(s));
        assertFalse(iter.hasNext());

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
        ObjIterator<String> iter = Iterators.flatmap((Iterable<String>) null, s -> new String[] { s });
        assertFalse(iter.hasNext());

        List<String> list = Arrays.asList("a", "b", "c");
        iter = Iterators.flatmap(list, s -> new String[] { s + "1", s + "2" });
        assertEquals("a1", iter.next());
        assertEquals("a2", iter.next());
        assertEquals("b1", iter.next());
        assertEquals("b2", iter.next());
        assertEquals("c1", iter.next());
        assertEquals("c2", iter.next());
        assertFalse(iter.hasNext());

        iter = Iterators.flatmap(list, s -> new String[0]);
        assertFalse(iter.hasNext());

        iter = Iterators.flatmap(list, s -> null);
        assertFalse(iter.hasNext());
    }

    @Test
    public void testFlatmapIteratorArray() {
        ObjIterator<String> iter = Iterators.flatmap((Iterator<String>) null, s -> new String[] { s });
        assertFalse(iter.hasNext());

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

        result.clear();
        Iterators.forEach(list.iterator(), 10L, 3L, result::add);
        assertTrue(result.isEmpty());

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

        result.clear();
        completed[0] = false;
        Iterators.forEach(list.iterator(), 2L, 10L, result::add, () -> completed[0] = true);
        assertEquals(Arrays.asList("c", "d", "e"), result);
        assertTrue(completed[0]);
    }
}

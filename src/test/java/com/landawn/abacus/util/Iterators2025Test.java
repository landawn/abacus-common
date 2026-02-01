package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.u.Nullable;
import com.landawn.abacus.util.function.TriFunction;

@Tag("2025")
public class Iterators2025Test extends TestBase {

    @Test
    public void testGet() {
        Iterator<String> iter = Arrays.asList("a", "b", "c", "d").iterator();

        Nullable<String> result = Iterators.elementAt(iter, 0);
        assertTrue(result.isPresent());
        assertEquals("a", result.get());

        iter = Arrays.asList("a", "b", "c", "d").iterator();
        result = Iterators.elementAt(iter, 2);
        assertTrue(result.isPresent());
        assertEquals("c", result.get());

        iter = Arrays.asList("a", "b", "c").iterator();
        result = Iterators.elementAt(iter, 10);
        assertFalse(result.isPresent());

        result = Iterators.elementAt(null, 0);
        assertFalse(result.isPresent());

        assertThrows(IllegalArgumentException.class, () -> Iterators.elementAt(Arrays.asList("a").iterator(), -1));
    }

    @Test
    public void testOccurrencesOf() {
        List<Integer> numbers = Arrays.asList(1, 2, 3, 2, 4, 2, 5);
        assertEquals(3, Iterators.occurrencesOf(numbers.iterator(), 2));
        assertEquals(1, Iterators.occurrencesOf(numbers.iterator(), 5));
        assertEquals(0, Iterators.occurrencesOf(numbers.iterator(), 6));

        List<String> withNulls = Arrays.asList("a", null, "b", null, "c");
        assertEquals(2, Iterators.occurrencesOf(withNulls.iterator(), null));

        assertEquals(0, Iterators.occurrencesOf(null, "test"));
    }

    @Test
    public void testCount() {
        assertEquals(5, Iterators.count(Arrays.asList(1, 2, 3, 4, 5).iterator()));
        assertEquals(0, Iterators.count(null));
        assertEquals(0, Iterators.count(new ArrayList<>().iterator()));
    }

    @Test
    public void testCountWithPredicate() {
        Predicate<Integer> isEven = n -> n % 2 == 0;
        assertEquals(2, Iterators.count(Arrays.asList(1, 2, 3, 4, 5).iterator(), isEven));
        assertEquals(3, Iterators.count(Arrays.asList(2, 4, 6).iterator(), isEven));
        assertEquals(0, Iterators.count(null, isEven));

        assertThrows(IllegalArgumentException.class, () -> Iterators.count(Arrays.asList(1).iterator(), null));
    }

    @Test
    public void testIndexOf() {
        assertEquals(2, Iterators.indexOf(Arrays.asList("a", "b", "c", "d").iterator(), "c"));
        assertEquals(0, Iterators.indexOf(Arrays.asList("a", "b", "c").iterator(), "a"));
        assertEquals(-1, Iterators.indexOf(Arrays.asList("a", "b", "c").iterator(), "z"));
        assertEquals(-1, Iterators.indexOf(null, "a"));

        List<String> withNulls = Arrays.asList("a", null, "b", null);
        assertEquals(1, Iterators.indexOf(withNulls.iterator(), null));
    }

    @Test
    public void testIndexOfWithFromIndex() {
        assertEquals(3, Iterators.indexOf(Arrays.asList("a", "b", "c", "a", "d").iterator(), "a", 1));
        assertEquals(-1, Iterators.indexOf(Arrays.asList("a", "b", "c").iterator(), "c", 3));
        assertEquals(2, Iterators.indexOf(Arrays.asList("a", "b", "c").iterator(), "c", 0));
        assertEquals(-1, Iterators.indexOf(null, "a", 0));
    }

    @Test
    public void testElementsEqual() {
        assertTrue(Iterators.equalsInOrder(Arrays.asList(1, 2, 3).iterator(), Arrays.asList(1, 2, 3).iterator()));
        assertFalse(Iterators.equalsInOrder(Arrays.asList(1, 2, 3).iterator(), Arrays.asList(1, 2, 4).iterator()));
        assertFalse(Iterators.equalsInOrder(Arrays.asList(1, 2).iterator(), Arrays.asList(1, 2, 3).iterator()));
        assertTrue(Iterators.equalsInOrder(null, null));
        assertFalse(Iterators.equalsInOrder(Arrays.asList(1).iterator(), null));
    }

    @Test
    public void testRepeatInt() {
        ObjIterator<String> iter = Iterators.repeat("a", 3);
        assertEquals(Arrays.asList("a", "a", "a"), iter.toList());

        iter = Iterators.repeat("x", 0);
        assertFalse(iter.hasNext());

        assertThrows(IllegalArgumentException.class, () -> Iterators.repeat("a", -1));
    }

    @Test
    public void testRepeatLong() {
        ObjIterator<String> iter = Iterators.repeat("b", 4L);
        assertEquals(Arrays.asList("b", "b", "b", "b"), iter.toList());

        iter = Iterators.repeat("y", 0L);
        assertFalse(iter.hasNext());

        assertThrows(IllegalArgumentException.class, () -> Iterators.repeat("a", -1L));
    }

    @Test
    public void testRepeatElements() {
        ObjIterator<Integer> iter = Iterators.repeatElements(Arrays.asList(1, 2, 3), 2);
        assertEquals(Arrays.asList(1, 1, 2, 2, 3, 3), iter.toList());

        iter = Iterators.repeatElements(Arrays.asList(1, 2), 0);
        assertFalse(iter.hasNext());

        iter = Iterators.repeatElements(null, 2);
        assertFalse(iter.hasNext());

        assertThrows(IllegalArgumentException.class, () -> Iterators.repeatElements(Arrays.asList(1), -1));
    }

    @Test
    public void testRepeatCollection() {
        ObjIterator<Integer> iter = Iterators.cycle(Arrays.asList(1, 2), 3);
        assertEquals(Arrays.asList(1, 2, 1, 2, 1, 2), iter.toList());

        iter = Iterators.cycle(Arrays.asList(1, 2, 3), 0);
        assertFalse(iter.hasNext());

        iter = Iterators.cycle(null, 2);
        assertFalse(iter.hasNext());

        assertThrows(IllegalArgumentException.class, () -> Iterators.cycle(Arrays.asList(1), -1));
    }

    @Test
    public void testRepeatElementsToSize() {
        ObjIterator<Integer> iter = Iterators.repeatElementsToSize(Arrays.asList(1, 2), 5);
        assertEquals(Arrays.asList(1, 1, 1, 2, 2), iter.toList());

        iter = Iterators.repeatElementsToSize(Arrays.asList(1, 2, 3), 7);
        assertEquals(Arrays.asList(1, 1, 1, 2, 2, 3, 3), iter.toList());

        iter = Iterators.repeatElementsToSize(Arrays.asList(1, 2), 0);
        assertFalse(iter.hasNext());

        assertThrows(IllegalArgumentException.class, () -> Iterators.repeatElementsToSize(null, 5));

        assertThrows(IllegalArgumentException.class, () -> Iterators.repeatElementsToSize(Arrays.asList(1), -1));
    }

    @Test
    public void testRepeatCollectionToSize() {
        ObjIterator<Integer> iter = Iterators.cycleToSize(Arrays.asList(1, 2), 5);
        assertEquals(Arrays.asList(1, 2, 1, 2, 1), iter.toList());

        iter = Iterators.cycleToSize(Arrays.asList(1, 2, 3), 8);
        assertEquals(Arrays.asList(1, 2, 3, 1, 2, 3, 1, 2), iter.toList());

        iter = Iterators.cycleToSize(Arrays.asList(1, 2), 0);
        assertFalse(iter.hasNext());

        assertThrows(IllegalArgumentException.class, () -> Iterators.cycleToSize(null, 5));

        assertThrows(IllegalArgumentException.class, () -> Iterators.cycleToSize(Arrays.asList(1), -1));
    }

    @Test
    public void testCycleVarargs() {
        ObjIterator<String> iter = Iterators.cycle("a", "b", "c");
        List<String> result = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            result.add(iter.next());
        }
        assertEquals(Arrays.asList("a", "b", "c", "a", "b", "c", "a"), result);

        iter = Iterators.cycle();
        assertFalse(iter.hasNext());
    }

    @Test
    public void testCycleIterable() {
        ObjIterator<Integer> iter = Iterators.cycle(Arrays.asList(1, 2));
        List<Integer> result = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            result.add(iter.next());
        }
        assertEquals(Arrays.asList(1, 2, 1, 2, 1), result);

        iter = Iterators.cycle(Collections.emptyList());
        assertFalse(iter.hasNext());

        iter = Iterators.cycle((Iterable<Integer>) null);
        assertFalse(iter.hasNext());
    }

    @Test
    public void testCycleWithRounds() {
        ObjIterator<String> iter = Iterators.cycle(Arrays.asList("x", "y"), 3);
        assertEquals(Arrays.asList("x", "y", "x", "y", "x", "y"), iter.toList());

        iter = Iterators.cycle(Arrays.asList("a", "b"), 0);
        assertFalse(iter.hasNext());

        iter = Iterators.cycle(null, 3);
        assertFalse(iter.hasNext());

        assertThrows(IllegalArgumentException.class, () -> Iterators.cycle(Arrays.asList("a"), -1));
    }

    @Test
    public void testConcatBooleanArrays() {
        BooleanIterator iter = Iterators.concat(new boolean[] { true, false }, new boolean[] { true }, new boolean[] { false, false });
        List<Boolean> result = new ArrayList<>();
        while (iter.hasNext()) {
            result.add(iter.nextBoolean());
        }
        assertEquals(Arrays.asList(true, false, true, false, false), result);

        iter = Iterators.concat(new boolean[0], new boolean[0]);
        assertFalse(iter.hasNext());

        iter = Iterators.concat((boolean[][]) null);
        assertFalse(iter.hasNext());
    }

    @Test
    public void testConcatCharArrays() {
        CharIterator iter = Iterators.concat(new char[] { 'a', 'b' }, new char[] { 'c' });
        List<Character> result = new ArrayList<>();
        while (iter.hasNext()) {
            result.add(iter.nextChar());
        }
        assertEquals(Arrays.asList('a', 'b', 'c'), result);

        iter = Iterators.concat((char[][]) null);
        assertFalse(iter.hasNext());
    }

    @Test
    public void testConcatByteArrays() {
        ByteIterator iter = Iterators.concat(new byte[] { 1, 2 }, new byte[] { 3, 4 });
        List<Byte> result = new ArrayList<>();
        while (iter.hasNext()) {
            result.add(iter.nextByte());
        }
        assertEquals(Arrays.asList((byte) 1, (byte) 2, (byte) 3, (byte) 4), result);

        iter = Iterators.concat((byte[][]) null);
        assertFalse(iter.hasNext());
    }

    @Test
    public void testConcatShortArrays() {
        ShortIterator iter = Iterators.concat(new short[] { 1, 2 }, new short[] { 3 });
        List<Short> result = new ArrayList<>();
        while (iter.hasNext()) {
            result.add(iter.nextShort());
        }
        assertEquals(Arrays.asList((short) 1, (short) 2, (short) 3), result);

        iter = Iterators.concat((short[][]) null);
        assertFalse(iter.hasNext());
    }

    @Test
    public void testConcatIntArrays() {
        IntIterator iter = Iterators.concat(new int[] { 1, 2 }, new int[] { 3, 4, 5 });
        List<Integer> result = new ArrayList<>();
        while (iter.hasNext()) {
            result.add(iter.nextInt());
        }
        assertEquals(Arrays.asList(1, 2, 3, 4, 5), result);

        iter = Iterators.concat((int[][]) null);
        assertFalse(iter.hasNext());
    }

    @Test
    public void testConcatLongArrays() {
        LongIterator iter = Iterators.concat(new long[] { 1L, 2L }, new long[] { 3L });
        List<Long> result = new ArrayList<>();
        while (iter.hasNext()) {
            result.add(iter.nextLong());
        }
        assertEquals(Arrays.asList(1L, 2L, 3L), result);

        iter = Iterators.concat((long[][]) null);
        assertFalse(iter.hasNext());
    }

    @Test
    public void testConcatFloatArrays() {
        FloatIterator iter = Iterators.concat(new float[] { 1.0f, 2.0f }, new float[] { 3.0f });
        List<Float> result = new ArrayList<>();
        while (iter.hasNext()) {
            result.add(iter.nextFloat());
        }
        assertEquals(Arrays.asList(1.0f, 2.0f, 3.0f), result);

        iter = Iterators.concat((float[][]) null);
        assertFalse(iter.hasNext());
    }

    @Test
    public void testConcatDoubleArrays() {
        DoubleIterator iter = Iterators.concat(new double[] { 1.0, 2.0 }, new double[] { 3.0, 4.0 });
        List<Double> result = new ArrayList<>();
        while (iter.hasNext()) {
            result.add(iter.nextDouble());
        }
        assertEquals(Arrays.asList(1.0, 2.0, 3.0, 4.0), result);

        iter = Iterators.concat((double[][]) null);
        assertFalse(iter.hasNext());
    }

    @Test
    public void testConcatBooleanIterators() {
        BooleanIterator iter1 = BooleanIterator.of(true, false);
        BooleanIterator iter2 = BooleanIterator.of(true);
        BooleanIterator result = Iterators.concat(iter1, iter2);

        List<Boolean> list = new ArrayList<>();
        while (result.hasNext()) {
            list.add(result.nextBoolean());
        }
        assertEquals(Arrays.asList(true, false, true), list);

        result = Iterators.concat((BooleanIterator[]) null);
        assertFalse(result.hasNext());
    }

    @Test
    public void testConcatCharIterators() {
        CharIterator iter1 = CharIterator.of('a', 'b');
        CharIterator iter2 = CharIterator.of('c');
        CharIterator result = Iterators.concat(iter1, iter2);

        List<Character> list = new ArrayList<>();
        while (result.hasNext()) {
            list.add(result.nextChar());
        }
        assertEquals(Arrays.asList('a', 'b', 'c'), list);

        result = Iterators.concat((CharIterator[]) null);
        assertFalse(result.hasNext());
    }

    @Test
    public void testConcatByteIterators() {
        ByteIterator iter1 = ByteIterator.of((byte) 1, (byte) 2);
        ByteIterator iter2 = ByteIterator.of((byte) 3);
        ByteIterator result = Iterators.concat(iter1, iter2);

        List<Byte> list = new ArrayList<>();
        while (result.hasNext()) {
            list.add(result.nextByte());
        }
        assertEquals(Arrays.asList((byte) 1, (byte) 2, (byte) 3), list);

        result = Iterators.concat((ByteIterator[]) null);
        assertFalse(result.hasNext());
    }

    @Test
    public void testConcatShortIterators() {
        ShortIterator iter1 = ShortIterator.of((short) 1, (short) 2);
        ShortIterator iter2 = ShortIterator.of((short) 3);
        ShortIterator result = Iterators.concat(iter1, iter2);

        List<Short> list = new ArrayList<>();
        while (result.hasNext()) {
            list.add(result.nextShort());
        }
        assertEquals(Arrays.asList((short) 1, (short) 2, (short) 3), list);

        result = Iterators.concat((ShortIterator[]) null);
        assertFalse(result.hasNext());
    }

    @Test
    public void testConcatIntIterators() {
        IntIterator iter1 = IntIterator.of(1, 2);
        IntIterator iter2 = IntIterator.of(3, 4);
        IntIterator result = Iterators.concat(iter1, iter2);

        List<Integer> list = new ArrayList<>();
        while (result.hasNext()) {
            list.add(result.nextInt());
        }
        assertEquals(Arrays.asList(1, 2, 3, 4), list);

        result = Iterators.concat((IntIterator[]) null);
        assertFalse(result.hasNext());
    }

    @Test
    public void testConcatLongIterators() {
        LongIterator iter1 = LongIterator.of(1L, 2L);
        LongIterator iter2 = LongIterator.of(3L);
        LongIterator result = Iterators.concat(iter1, iter2);

        List<Long> list = new ArrayList<>();
        while (result.hasNext()) {
            list.add(result.nextLong());
        }
        assertEquals(Arrays.asList(1L, 2L, 3L), list);

        result = Iterators.concat((LongIterator[]) null);
        assertFalse(result.hasNext());
    }

    @Test
    public void testConcatFloatIterators() {
        FloatIterator iter1 = FloatIterator.of(1.0f, 2.0f);
        FloatIterator iter2 = FloatIterator.of(3.0f);
        FloatIterator result = Iterators.concat(iter1, iter2);

        List<Float> list = new ArrayList<>();
        while (result.hasNext()) {
            list.add(result.nextFloat());
        }
        assertEquals(Arrays.asList(1.0f, 2.0f, 3.0f), list);

        result = Iterators.concat((FloatIterator[]) null);
        assertFalse(result.hasNext());
    }

    @Test
    public void testConcatDoubleIterators() {
        DoubleIterator iter1 = DoubleIterator.of(1.0, 2.0);
        DoubleIterator iter2 = DoubleIterator.of(3.0);
        DoubleIterator result = Iterators.concat(iter1, iter2);

        List<Double> list = new ArrayList<>();
        while (result.hasNext()) {
            list.add(result.nextDouble());
        }
        assertEquals(Arrays.asList(1.0, 2.0, 3.0), list);

        result = Iterators.concat((DoubleIterator[]) null);
        assertFalse(result.hasNext());
    }

    @Test
    public void testConcatObjectArrays() {
        ObjIterator<String> iter = Iterators.concat(new String[] { "a", "b" }, new String[] { "c" }, new String[] { "d", "e" });
        assertEquals(Arrays.asList("a", "b", "c", "d", "e"), iter.toList());

        iter = Iterators.concat((String[][]) null);
        assertFalse(iter.hasNext());
    }

    @Test
    public void testConcatIterators() {
        ObjIterator<Integer> iter = Iterators.concat(Arrays.asList(1, 2).iterator(), Arrays.asList(3, 4).iterator(), Arrays.asList(5).iterator());
        assertEquals(Arrays.asList(1, 2, 3, 4, 5), iter.toList());

        iter = Iterators.concat((Iterator<Integer>[]) null);
        assertFalse(iter.hasNext());
    }

    @Test
    public void testConcatIterables() {
        ObjIterator<String> iter = Iterators.concat(Arrays.asList("a", "b"), Arrays.asList("c"), Collections.emptyList());
        assertEquals(Arrays.asList("a", "b", "c"), iter.toList());

        iter = Iterators.concat((Iterable<String>[]) null);
        assertFalse(iter.hasNext());
    }

    @Test
    public void testConcatMaps() {
        Map<String, Integer> map1 = new HashMap<>();
        map1.put("a", 1);
        map1.put("b", 2);

        Map<String, Integer> map2 = new HashMap<>();
        map2.put("c", 3);

        ObjIterator<Map.Entry<String, Integer>> iter = Iterators.concat(map1, map2);
        assertEquals(3, iter.toList().size());

        iter = Iterators.concat((Map<String, Integer>[]) null);
        assertFalse(iter.hasNext());
    }

    @Test
    public void testConcatCollectionOfIterators() {
        Collection<Iterator<String>> collection = Arrays.asList(Arrays.asList("a", "b").iterator(), Arrays.asList("c").iterator());
        ObjIterator<String> iter = Iterators.concat(collection);
        assertEquals(Arrays.asList("a", "b", "c"), iter.toList());

        iter = Iterators.concat((Collection<Iterator<String>>) null);
        assertFalse(iter.hasNext());
    }

    @Test
    public void testConcatIterablesCollection() {
        Collection<Iterable<Integer>> collection = Arrays.asList(Arrays.asList(1, 2), Arrays.asList(3, 4));
        ObjIterator<Integer> iter = Iterators.concatIterables(collection);
        assertEquals(Arrays.asList(1, 2, 3, 4), iter.toList());

        iter = Iterators.concatIterables(null);
        assertFalse(iter.hasNext());
    }

    @Test
    public void testConcatBiIterators() {
        BiIterator<String, Integer> iter1 = createBiIterator(Arrays.asList(Pair.of("a", 1), Pair.of("b", 2)));
        BiIterator<String, Integer> iter2 = createBiIterator(Arrays.asList(Pair.of("c", 3)));

        BiIterator<String, Integer> result = Iterators.concat(iter1, iter2);
        List<Pair<String, Integer>> list = new ArrayList<>();
        result.forEachRemaining((l, r) -> list.add(Pair.of(l, r)));

        assertEquals(3, list.size());
        assertEquals("a", list.get(0).left());
        assertEquals(1, list.get(0).right());

        result = Iterators.concat((BiIterator<String, Integer>[]) null);
        assertFalse(result.hasNext());
    }

    @Test
    public void testConcatTriIterators() {
        TriIterator<String, Integer, Boolean> iter1 = createTriIterator(Arrays.asList(Triple.of("a", 1, true), Triple.of("b", 2, false)));
        TriIterator<String, Integer, Boolean> iter2 = createTriIterator(Arrays.asList(Triple.of("c", 3, true)));

        TriIterator<String, Integer, Boolean> result = Iterators.concat(iter1, iter2);
        List<Triple<String, Integer, Boolean>> list = new ArrayList<>();
        result.forEachRemaining((l, m, r) -> list.add(Triple.of(l, m, r)));

        assertEquals(3, list.size());
        assertEquals("a", list.get(0).left());

        result = Iterators.concat((TriIterator<String, Integer, Boolean>[]) null);
        assertFalse(result.hasNext());
    }

    @Test
    public void testMergeTwoIterators() {
        BiFunction<Integer, Integer, MergeResult> selector = (a, b) -> a <= b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND;

        ObjIterator<Integer> result = Iterators.merge(Arrays.asList(1, 3, 5).iterator(), Arrays.asList(2, 4, 6).iterator(), selector);
        assertEquals(Arrays.asList(1, 2, 3, 4, 5, 6), result.toList());

        result = Iterators.merge((Iterator<Integer>) null, Arrays.asList(1).iterator(), selector);
        assertEquals(Arrays.asList(1), result.toList());

        assertThrows(IllegalArgumentException.class, () -> Iterators.merge(Arrays.asList(1).iterator(), Arrays.asList(2).iterator(), null));
    }

    @Test
    public void testMergeCollection() {
        BiFunction<Integer, Integer, MergeResult> selector = (a, b) -> a <= b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND;

        Collection<Iterator<Integer>> collection = Arrays.asList(Arrays.asList(1, 4).iterator(), Arrays.asList(2, 5).iterator(),
                Arrays.asList(3, 6).iterator());

        ObjIterator<Integer> result = Iterators.merge(collection, selector);
        assertNotNull(result);

        result = Iterators.merge((Collection<Iterator<Integer>>) null, selector);
        assertFalse(result.hasNext());

        assertThrows(IllegalArgumentException.class, () -> Iterators.merge(Arrays.asList(Arrays.asList(1).iterator()), null));
    }

    @Test
    public void testMergeTwoIterables() {
        BiFunction<Integer, Integer, MergeResult> selector = (a, b) -> a <= b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND;

        ObjIterator<Integer> result = Iterators.merge(Arrays.asList(1, 3, 5), Arrays.asList(2, 4, 6), selector);
        assertEquals(Arrays.asList(1, 2, 3, 4, 5, 6), result.toList());

        result = Iterators.merge((Iterable<Integer>) null, Arrays.asList(1), selector);
        assertEquals(Arrays.asList(1), result.toList());
    }

    @Test
    public void testMergeIterablesCollection() {
        BiFunction<Integer, Integer, MergeResult> selector = (a, b) -> a <= b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND;

        Collection<Iterable<Integer>> collection = Arrays.asList(Arrays.asList(1, 4), Arrays.asList(2, 5));

        ObjIterator<Integer> result = Iterators.mergeIterables(collection, selector);
        assertNotNull(result);

        result = Iterators.mergeIterables(null, selector);
        assertFalse(result.hasNext());
    }

    @Test
    public void testMergeSortedComparable() {
        ObjIterator<Integer> result = Iterators.mergeSorted(Arrays.asList(1, 3, 5).iterator(), Arrays.asList(2, 4, 6).iterator());
        assertEquals(Arrays.asList(1, 2, 3, 4, 5, 6), result.toList());

        result = Iterators.mergeSorted(Arrays.asList(1).iterator(), (Iterator<Integer>) null);
        assertEquals(Arrays.asList(1), result.toList());
    }

    @Test
    public void testMergeSortedWithComparator() {
        Comparator<Integer> cmp = Integer::compareTo;

        ObjIterator<Integer> result = Iterators.mergeSorted(Arrays.asList(1, 3, 5).iterator(), Arrays.asList(2, 4, 6).iterator(), cmp);
        assertEquals(Arrays.asList(1, 2, 3, 4, 5, 6), result.toList());

        result = Iterators.mergeSorted((Iterator<Integer>) null, Arrays.asList(1).iterator(), cmp);
        assertEquals(Arrays.asList(1), result.toList());
    }

    @Test
    public void testMergeSortedIterablesComparable() {
        ObjIterator<String> result = Iterators.mergeSorted(Arrays.asList("a", "c", "e"), Arrays.asList("b", "d"));
        assertEquals(Arrays.asList("a", "b", "c", "d", "e"), result.toList());

        result = Iterators.mergeSorted(Arrays.asList("x"), (Iterable<String>) null);
        assertEquals(Arrays.asList("x"), result.toList());
    }

    @Test
    public void testMergeSortedIterablesWithComparator() {
        Comparator<Integer> cmp = Integer::compareTo;

        ObjIterator<Integer> result = Iterators.mergeSorted(Arrays.asList(1, 3), Arrays.asList(2, 4), cmp);
        assertEquals(Arrays.asList(1, 2, 3, 4), result.toList());
    }

    @Test
    public void testZipTwoIterators() {
        BiFunction<Integer, String, String> zipFn = (a, b) -> a + b;

        ObjIterator<String> result = Iterators.zip(Arrays.asList(1, 2, 3).iterator(), Arrays.asList("a", "b", "c").iterator(), zipFn);
        assertEquals(Arrays.asList("1a", "2b", "3c"), result.toList());

        result = Iterators.zip(Arrays.asList(1, 2).iterator(), Arrays.asList("a", "b", "c").iterator(), zipFn);
        assertEquals(Arrays.asList("1a", "2b"), result.toList());

        assertThrows(IllegalArgumentException.class, () -> Iterators.zip(Arrays.asList(1).iterator(), Arrays.asList("a").iterator(), null));
    }

    @Test
    public void testZipTwoIterables() {
        BiFunction<Integer, String, String> zipFn = (a, b) -> a + b;

        ObjIterator<String> result = Iterators.zip(Arrays.asList(1, 2), Arrays.asList("a", "b"), zipFn);
        assertEquals(Arrays.asList("1a", "2b"), result.toList());
    }

    @Test
    public void testZipTwoIteratorsWithDefaultValues() {
        BiFunction<Integer, String, String> zipFn = (a, b) -> a + b;

        ObjIterator<String> result = Iterators.zip(Arrays.asList(1, 2).iterator(), Arrays.asList("a", "b", "c").iterator(), 0, "x", zipFn);
        assertEquals(Arrays.asList("1a", "2b", "0c"), result.toList());

        result = Iterators.zip(Arrays.asList(1, 2, 3).iterator(), Arrays.asList("a").iterator(), 9, "z", zipFn);
        assertEquals(Arrays.asList("1a", "2z", "3z"), result.toList());
    }

    @Test
    public void testZipTwoIterablesWithDefaultValues() {
        BiFunction<Integer, String, String> zipFn = (a, b) -> a + b;

        ObjIterator<String> result = Iterators.zip(Arrays.asList(1, 2), Arrays.asList("a", "b", "c"), 0, "x", zipFn);
        assertEquals(Arrays.asList("1a", "2b", "0c"), result.toList());
    }

    @Test
    public void testZipThreeIterators() {
        TriFunction<Integer, String, Boolean, String> zipFn = (a, b, c) -> a + b + c;

        ObjIterator<String> result = Iterators.zip(Arrays.asList(1, 2).iterator(), Arrays.asList("a", "b").iterator(), Arrays.asList(true, false).iterator(),
                zipFn);
        assertEquals(Arrays.asList("1atrue", "2bfalse"), result.toList());

        result = Iterators.zip(Arrays.asList(1).iterator(), Arrays.asList("a", "b").iterator(), Arrays.asList(true).iterator(), zipFn);
        assertEquals(Arrays.asList("1atrue"), result.toList());
    }

    @Test
    public void testZipThreeIterables() {
        TriFunction<Integer, String, Boolean, String> zipFn = (a, b, c) -> a + b + c;

        ObjIterator<String> result = Iterators.zip(Arrays.asList(1, 2), Arrays.asList("a", "b"), Arrays.asList(true, false), zipFn);
        assertEquals(Arrays.asList("1atrue", "2bfalse"), result.toList());
    }

    @Test
    public void testZipThreeIteratorsWithDefaultValues() {
        TriFunction<Integer, String, Boolean, String> zipFn = (a, b, c) -> a + b + c;

        ObjIterator<String> result = Iterators.zip(Arrays.asList(1).iterator(), Arrays.asList("a", "b").iterator(), Arrays.asList(true, false, true).iterator(),
                0, "x", false, zipFn);
        assertEquals(Arrays.asList("1atrue", "0bfalse", "0xtrue"), result.toList());
    }

    @Test
    public void testZipThreeIterablesWithDefaultValues() {
        TriFunction<Integer, String, Boolean, String> zipFn = (a, b, c) -> a + b + c;

        ObjIterator<String> result = Iterators.zip(Arrays.asList(1), Arrays.asList("a", "b"), Arrays.asList(true), 0, "x", false, zipFn);
        assertEquals(Arrays.asList("1atrue", "0bfalse"), result.toList());
    }

    @Test
    public void testUnzipIterator() {
        BiConsumer<String, Pair<String, Integer>> unzipFn = (str, pair) -> {
            pair.setLeft(str.substring(0, 1));
            pair.setRight(Integer.parseInt(str.substring(1)));
        };

        BiIterator<String, Integer> result = Iterators.unzip(Arrays.asList("a1", "b2", "c3").iterator(), unzipFn);

        List<Pair<String, Integer>> list = new ArrayList<>();
        result.forEachRemaining((l, r) -> list.add(Pair.of(l, r)));

        assertEquals(3, list.size());
        assertEquals("a", list.get(0).left());
        assertEquals(1, list.get(0).right());

        assertThrows(IllegalArgumentException.class, () -> Iterators.unzip(Arrays.asList("a1").iterator(), null));
    }

    @Test
    public void testUnzipIterable() {
        BiConsumer<String, Pair<String, Integer>> unzipFn = (str, pair) -> {
            pair.setLeft(str.substring(0, 1));
            pair.setRight(Integer.parseInt(str.substring(1)));
        };

        BiIterator<String, Integer> result = Iterators.unzip(Arrays.asList("x1", "y2"), unzipFn);

        List<Pair<String, Integer>> list = new ArrayList<>();
        result.forEachRemaining((l, r) -> list.add(Pair.of(l, r)));

        assertEquals(2, list.size());
    }

    @Test
    public void testUnzippIterator() {
        BiConsumer<String, Triple<String, Integer, Boolean>> unzipFn = (str, triple) -> {
            triple.setLeft(str.substring(0, 1));
            triple.setMiddle(Integer.parseInt(str.substring(1, 2)));
            triple.setRight(str.charAt(2) == 't');
        };

        TriIterator<String, Integer, Boolean> result = Iterators.unzip3(Arrays.asList("a1t", "b2f").iterator(), unzipFn);

        List<Triple<String, Integer, Boolean>> list = new ArrayList<>();
        result.forEachRemaining((l, m, r) -> list.add(Triple.of(l, m, r)));

        assertEquals(2, list.size());
        assertEquals("a", list.get(0).left());
        assertEquals(1, list.get(0).middle());
        assertTrue(list.get(0).right());
    }

    @Test
    public void testUnzippIterable() {
        BiConsumer<String, Triple<String, Integer, Boolean>> unzipFn = (str, triple) -> {
            triple.setLeft(str.substring(0, 1));
            triple.setMiddle(Integer.parseInt(str.substring(1, 2)));
            triple.setRight(str.charAt(2) == 't');
        };

        TriIterator<String, Integer, Boolean> result = Iterators.unzip3(Arrays.asList("x1t"), unzipFn);

        assertTrue(result.hasNext());
    }

    @Test
    public void testAdvance() {
        Iterator<Integer> iter = Arrays.asList(1, 2, 3, 4, 5).iterator();
        assertEquals(3, Iterators.advance(iter, 3));
        assertTrue(iter.hasNext());
        assertEquals(4, iter.next());

        iter = Arrays.asList(1, 2).iterator();
        assertEquals(2, Iterators.advance(iter, 5));
        assertFalse(iter.hasNext());

        assertEquals(0, Iterators.advance(null, 5));

        assertThrows(IllegalArgumentException.class, () -> Iterators.advance(Arrays.asList(1).iterator(), -1));
    }

    @Test
    public void testSkip() {
        ObjIterator<Integer> result = Iterators.skip(Arrays.asList(1, 2, 3, 4, 5).iterator(), 2);
        assertEquals(Arrays.asList(3, 4, 5), result.toList());

        result = Iterators.skip(Arrays.asList(1, 2).iterator(), 5);
        assertFalse(result.hasNext());

        result = Iterators.skip((Iterator<Integer>) null, 2);
        assertFalse(result.hasNext());

        assertThrows(IllegalArgumentException.class, () -> Iterators.skip(Arrays.asList(1).iterator(), -1));
    }

    @Test
    public void testLimit() {
        ObjIterator<Integer> result = Iterators.limit(Arrays.asList(1, 2, 3, 4, 5).iterator(), 3);
        assertEquals(Arrays.asList(1, 2, 3), result.toList());

        result = Iterators.limit(Arrays.asList(1, 2).iterator(), 5);
        assertEquals(Arrays.asList(1, 2), result.toList());

        result = Iterators.limit((Iterator<Integer>) null, 3);
        assertFalse(result.hasNext());

        assertThrows(IllegalArgumentException.class, () -> Iterators.limit(Arrays.asList(1).iterator(), -1));
    }

    @Test
    public void testSkipAndLimitIterator() {
        ObjIterator<Integer> result = Iterators.skipAndLimit(Arrays.asList(1, 2, 3, 4, 5).iterator(), 1, 3);
        assertEquals(Arrays.asList(2, 3, 4), result.toList());

        result = Iterators.skipAndLimit(Arrays.asList(1, 2).iterator(), 0, 1);
        assertEquals(Arrays.asList(1), result.toList());

        result = Iterators.skipAndLimit((Iterator<Integer>) null, 1, 2);
        assertFalse(result.hasNext());
    }

    @Test
    public void testSkipAndLimitIterable() {
        ObjIterator<Integer> result = Iterators.skipAndLimit(Arrays.asList(1, 2, 3, 4, 5), 2, 2);
        assertEquals(Arrays.asList(3, 4), result.toList());

        result = Iterators.skipAndLimit((Iterable<Integer>) null, 1, 2);
        assertFalse(result.hasNext());
    }

    @Test
    public void testSkipNullsIterable() {
        ObjIterator<String> result = Iterators.skipNulls(Arrays.asList("a", null, "b", null, "c"));
        assertEquals(Arrays.asList("a", "b", "c"), result.toList());

        result = Iterators.skipNulls(Arrays.asList(null, null));
        assertFalse(result.hasNext());

        result = Iterators.skipNulls((Iterable<String>) null);
        assertFalse(result.hasNext());
    }

    @Test
    public void testSkipNullsIterator() {
        ObjIterator<String> result = Iterators.skipNulls(Arrays.asList("a", null, "b", null, "c").iterator());
        assertEquals(Arrays.asList("a", "b", "c"), result.toList());

        result = Iterators.skipNulls((Iterator<String>) null);
        assertFalse(result.hasNext());
    }

    @Test
    public void testDistinctIterable() {
        ObjIterator<Integer> result = Iterators.distinct(Arrays.asList(1, 2, 2, 3, 1, 4));
        assertEquals(Arrays.asList(1, 2, 3, 4), result.toList());

        result = Iterators.distinct(Collections.emptyList());
        assertFalse(result.hasNext());

        result = Iterators.distinct((Iterable<Integer>) null);
        assertFalse(result.hasNext());
    }

    @Test
    public void testDistinctIterator() {
        ObjIterator<Integer> result = Iterators.distinct(Arrays.asList(1, 2, 2, 3, 1, 4).iterator());
        assertEquals(Arrays.asList(1, 2, 3, 4), result.toList());

        result = Iterators.distinct((Iterator<Integer>) null);
        assertFalse(result.hasNext());
    }

    @Test
    public void testDistinctByIterable() {
        ObjIterator<String> result = Iterators.distinctBy(Arrays.asList("a", "ab", "b", "abc", "c"), String::length);
        List<String> list = result.toList();
        assertEquals(3, list.size());

        result = Iterators.distinctBy((Iterable<String>) null, String::length);
        assertFalse(result.hasNext());

        assertThrows(IllegalArgumentException.class, () -> Iterators.distinctBy(Arrays.asList("a"), null));
    }

    @Test
    public void testDistinctByIterator() {
        ObjIterator<String> result = Iterators.distinctBy(Arrays.asList("a", "ab", "b", "abc").iterator(), String::length);
        List<String> list = result.toList();
        assertEquals(3, list.size());

        result = Iterators.distinctBy((Iterator<String>) null, String::length);
        assertFalse(result.hasNext());

        assertThrows(IllegalArgumentException.class, () -> Iterators.distinctBy(Arrays.asList("a").iterator(), null));
    }

    @Test
    public void testFilterIterable() {
        Predicate<Integer> isEven = n -> n % 2 == 0;

        ObjIterator<Integer> result = Iterators.filter(Arrays.asList(1, 2, 3, 4, 5, 6), isEven);
        assertEquals(Arrays.asList(2, 4, 6), result.toList());

        result = Iterators.filter((Iterable<Integer>) null, isEven);
        assertFalse(result.hasNext());

        assertThrows(IllegalArgumentException.class, () -> Iterators.filter(Arrays.asList(1), null));
    }

    @Test
    public void testFilterIterator() {
        Predicate<Integer> isOdd = n -> n % 2 != 0;

        ObjIterator<Integer> result = Iterators.filter(Arrays.asList(1, 2, 3, 4, 5).iterator(), isOdd);
        assertEquals(Arrays.asList(1, 3, 5), result.toList());

        result = Iterators.filter((Iterator<Integer>) null, isOdd);
        assertFalse(result.hasNext());

        assertThrows(IllegalArgumentException.class, () -> Iterators.filter(Arrays.asList(1).iterator(), null));
    }

    @Test
    public void testTakeWhileIterable() {
        Predicate<Integer> lessThan4 = n -> n < 4;

        ObjIterator<Integer> result = Iterators.takeWhile(Arrays.asList(1, 2, 3, 4, 5), lessThan4);
        assertEquals(Arrays.asList(1, 2, 3), result.toList());

        result = Iterators.takeWhile(Arrays.asList(5, 6, 7), lessThan4);
        assertFalse(result.hasNext());

        result = Iterators.takeWhile((Iterable<Integer>) null, lessThan4);
        assertFalse(result.hasNext());

        assertThrows(IllegalArgumentException.class, () -> Iterators.takeWhile(Arrays.asList(1), null));
    }

    @Test
    public void testTakeWhileIterator() {
        Predicate<Integer> lessThan3 = n -> n < 3;

        ObjIterator<Integer> result = Iterators.takeWhile(Arrays.asList(1, 2, 3, 4).iterator(), lessThan3);
        assertEquals(Arrays.asList(1, 2), result.toList());

        result = Iterators.takeWhile((Iterator<Integer>) null, lessThan3);
        assertFalse(result.hasNext());
    }

    @Test
    public void testTakeWhileInclusiveIterable() {
        Predicate<Integer> lessThan4 = n -> n < 4;

        ObjIterator<Integer> result = Iterators.takeWhileInclusive(Arrays.asList(1, 2, 3, 4, 5), lessThan4);
        assertEquals(Arrays.asList(1, 2, 3, 4), result.toList());

        result = Iterators.takeWhileInclusive(Arrays.asList(5, 6), lessThan4);
        assertEquals(Arrays.asList(5), result.toList());

        result = Iterators.takeWhileInclusive((Iterable<Integer>) null, lessThan4);
        assertFalse(result.hasNext());
    }

    @Test
    public void testTakeWhileInclusiveIterator() {
        Predicate<Integer> lessThan3 = n -> n < 3;

        ObjIterator<Integer> result = Iterators.takeWhileInclusive(Arrays.asList(1, 2, 3, 4).iterator(), lessThan3);
        assertEquals(Arrays.asList(1, 2, 3), result.toList());

        result = Iterators.takeWhileInclusive((Iterator<Integer>) null, lessThan3);
        assertFalse(result.hasNext());
    }

    @Test
    public void testDropWhileIterable() {
        Predicate<Integer> lessThan3 = n -> n < 3;

        ObjIterator<Integer> result = Iterators.dropWhile(Arrays.asList(1, 2, 3, 4, 5), lessThan3);
        assertEquals(Arrays.asList(3, 4, 5), result.toList());

        result = Iterators.dropWhile(Arrays.asList(1, 2), lessThan3);
        assertFalse(result.hasNext());

        result = Iterators.dropWhile((Iterable<Integer>) null, lessThan3);
        assertFalse(result.hasNext());
    }

    @Test
    public void testDropWhileIterator() {
        Predicate<Integer> lessThan4 = n -> n < 4;

        ObjIterator<Integer> result = Iterators.dropWhile(Arrays.asList(1, 2, 3, 4, 5).iterator(), lessThan4);
        assertEquals(Arrays.asList(4, 5), result.toList());

        result = Iterators.dropWhile((Iterator<Integer>) null, lessThan4);
        assertFalse(result.hasNext());
    }

    @Test
    public void testSkipUntilIterable() {
        Predicate<Integer> greaterThan2 = n -> n > 2;

        ObjIterator<Integer> result = Iterators.skipUntil(Arrays.asList(1, 2, 3, 4, 5), greaterThan2);
        assertEquals(Arrays.asList(3, 4, 5), result.toList());

        result = Iterators.skipUntil(Arrays.asList(1, 2), greaterThan2);
        assertFalse(result.hasNext());

        result = Iterators.skipUntil((Iterable<Integer>) null, greaterThan2);
        assertFalse(result.hasNext());
    }

    @Test
    public void testSkipUntilIterator() {
        Predicate<Integer> greaterThan3 = n -> n > 3;

        ObjIterator<Integer> result = Iterators.skipUntil(Arrays.asList(1, 2, 3, 4, 5).iterator(), greaterThan3);
        assertEquals(Arrays.asList(4, 5), result.toList());

        result = Iterators.skipUntil((Iterator<Integer>) null, greaterThan3);
        assertFalse(result.hasNext());
    }

    @Test
    public void testMapIterable() {
        Function<Integer, String> mapper = n -> "n" + n;

        ObjIterator<String> result = Iterators.map(Arrays.asList(1, 2, 3), mapper);
        assertEquals(Arrays.asList("n1", "n2", "n3"), result.toList());

        result = Iterators.map((Iterable<Integer>) null, mapper);
        assertFalse(result.hasNext());

        assertThrows(IllegalArgumentException.class, () -> Iterators.map(Arrays.asList(1), null));
    }

    @Test
    public void testMapIterator() {
        Function<String, Integer> mapper = String::length;

        ObjIterator<Integer> result = Iterators.map(Arrays.asList("a", "ab", "abc").iterator(), mapper);
        assertEquals(Arrays.asList(1, 2, 3), result.toList());

        result = Iterators.map((Iterator<String>) null, mapper);
        assertFalse(result.hasNext());
    }

    @Test
    public void testFlatMapIterable() {
        Function<Integer, Iterable<Integer>> mapper = n -> Arrays.asList(n, n * 10);

        ObjIterator<Integer> result = Iterators.flatMap(Arrays.asList(1, 2, 3), mapper);
        assertEquals(Arrays.asList(1, 10, 2, 20, 3, 30), result.toList());

        result = Iterators.flatMap((Iterable<Integer>) null, mapper);
        assertFalse(result.hasNext());

        assertThrows(IllegalArgumentException.class, () -> Iterators.flatMap(Arrays.asList(1), null));
    }

    @Test
    public void testFlatMapIterator() {
        Function<String, Iterable<String>> mapper = s -> Arrays.asList(s, s.toUpperCase());

        ObjIterator<String> result = Iterators.flatMap(Arrays.asList("a", "b").iterator(), mapper);
        assertEquals(Arrays.asList("a", "A", "b", "B"), result.toList());

        result = Iterators.flatMap((Iterator<String>) null, mapper);
        assertFalse(result.hasNext());
    }

    @Test
    public void testFlatmapIterable() {
        Function<Integer, Integer[]> mapper = n -> new Integer[] { n, n * 2 };

        ObjIterator<Integer> result = Iterators.flatmap(Arrays.asList(1, 2), mapper);
        assertEquals(Arrays.asList(1, 2, 2, 4), result.toList());

        result = Iterators.flatmap((Iterable<Integer>) null, mapper);
        assertFalse(result.hasNext());

        assertThrows(IllegalArgumentException.class, () -> Iterators.flatmap(Arrays.asList(1), null));
    }

    @Test
    public void testFlatmapIterator() {
        Function<String, String[]> mapper = s -> new String[] { s, s + s };

        ObjIterator<String> result = Iterators.flatmap(Arrays.asList("a", "b").iterator(), mapper);
        assertEquals(Arrays.asList("a", "aa", "b", "bb"), result.toList());

        result = Iterators.flatmap((Iterator<String>) null, mapper);
        assertFalse(result.hasNext());
    }

    @Test
    public void testForEachIterator() {
        AtomicInteger sum = new AtomicInteger(0);
        Iterators.forEach(Arrays.asList(1, 2, 3, 4, 5).iterator(), sum::addAndGet);
        assertEquals(15, sum.get());

        AtomicInteger count = new AtomicInteger(0);
        Iterators.forEach((Iterator<Integer>) null, n -> count.incrementAndGet());
        assertEquals(0, count.get());
    }

    @Test
    public void testForEachIteratorWithOnComplete() {
        AtomicInteger sum = new AtomicInteger(0);
        AtomicInteger completeCalled = new AtomicInteger(0);

        Iterators.forEach(Arrays.asList(1, 2, 3).iterator(), sum::addAndGet, () -> completeCalled.incrementAndGet());

        assertEquals(6, sum.get());
        assertEquals(1, completeCalled.get());
    }

    @Test
    public void testForEachIteratorWithOffsetAndCount() {
        AtomicInteger sum = new AtomicInteger(0);
        Iterators.forEach(Arrays.asList(1, 2, 3, 4, 5).iterator(), 1, 3, sum::addAndGet);
        assertEquals(9, sum.get());

        sum.set(0);
        Iterators.forEach(Arrays.asList(1, 2).iterator(), 0, 1, sum::addAndGet);
        assertEquals(1, sum.get());
    }

    @Test
    public void testForEachIteratorWithOffsetCountAndOnComplete() {
        AtomicInteger sum = new AtomicInteger(0);
        AtomicInteger completeCalled = new AtomicInteger(0);

        Iterators.forEach(Arrays.asList(1, 2, 3, 4, 5).iterator(), 2, 2, sum::addAndGet, () -> completeCalled.incrementAndGet());

        assertEquals(7, sum.get());
        assertEquals(1, completeCalled.get());
    }

    @Test
    public void testForEachIteratorWithThreads() {
        AtomicInteger sum = new AtomicInteger(0);
        Iterators.forEach(Arrays.asList(1, 2, 3, 4, 5).iterator(), 0, 5, 1, 1, sum::addAndGet);
        assertEquals(15, sum.get());
    }

    @Test
    public void testForEachIteratorWithThreadsAndOnComplete() {
        AtomicInteger sum = new AtomicInteger(0);
        AtomicInteger completeCalled = new AtomicInteger(0);

        Iterators.forEach(Arrays.asList(1, 2, 3).iterator(), 0, 3, 1, 1, sum::addAndGet, () -> completeCalled.incrementAndGet());

        assertEquals(6, sum.get());
        assertEquals(1, completeCalled.get());
    }

    @Test
    public void testForEachCollectionOfIterators() {
        AtomicInteger sum = new AtomicInteger(0);

        Collection<Iterator<Integer>> iterators = Arrays.asList(Arrays.asList(1, 2).iterator(), Arrays.asList(3, 4).iterator());

        Iterators.forEach(iterators, sum::addAndGet);
        assertEquals(10, sum.get());

        sum.set(0);
        Iterators.forEach((Collection<Iterator<Integer>>) null, sum::addAndGet);
        assertEquals(0, sum.get());
    }

    @Test
    public void testForEachCollectionOfIteratorsWithOnComplete() {
        AtomicInteger sum = new AtomicInteger(0);
        AtomicInteger completeCalled = new AtomicInteger(0);

        Collection<Iterator<Integer>> iterators = Arrays.asList(Arrays.asList(1, 2).iterator(), Arrays.asList(3).iterator());

        Iterators.forEach(iterators, sum::addAndGet, () -> completeCalled.incrementAndGet());

        assertEquals(6, sum.get());
        assertEquals(1, completeCalled.get());
    }

    @Test
    public void testForEachCollectionWithOffsetAndCount() {
        AtomicInteger sum = new AtomicInteger(0);

        Collection<Iterator<Integer>> iterators = Arrays.asList(Arrays.asList(1, 2, 3).iterator(), Arrays.asList(4, 5).iterator());

        Iterators.forEach(iterators, 1, 3, sum::addAndGet);
        assertEquals(9, sum.get());
    }

    @Test
    public void testForEachCollectionWithOffsetCountAndOnComplete() {
        AtomicInteger sum = new AtomicInteger(0);
        AtomicInteger completeCalled = new AtomicInteger(0);

        Collection<Iterator<Integer>> iterators = Arrays.asList(Arrays.asList(1, 2).iterator(), Arrays.asList(3, 4).iterator());

        Iterators.forEach(iterators, 0, 3, sum::addAndGet, () -> completeCalled.incrementAndGet());

        assertEquals(6, sum.get());
        assertEquals(1, completeCalled.get());
    }

    @Test
    public void testForEachCollectionWithThreads() {
        AtomicInteger sum = new AtomicInteger(0);

        Collection<Iterator<Integer>> iterators = Arrays.asList(Arrays.asList(1, 2).iterator(), Arrays.asList(3, 4).iterator());

        Iterators.forEach(iterators, 1, 1, 1, sum::addAndGet);
        assertEquals(10, sum.get());
    }

    @Test
    public void testForEachCollectionWithThreadsAndOnComplete() {
        AtomicInteger sum = new AtomicInteger(0);
        AtomicInteger completeCalled = new AtomicInteger(0);

        Collection<Iterator<Integer>> iterators = Arrays.asList(Arrays.asList(1, 2).iterator(), Arrays.asList(3).iterator());

        Iterators.forEach(iterators, 1, 1, 1, sum::addAndGet, () -> completeCalled.incrementAndGet());

        assertEquals(6, sum.get());
        assertEquals(1, completeCalled.get());
    }

    @Test
    public void testForEachCollectionWithOffsetCountAndThreads() {
        AtomicLong sum = new AtomicLong(0);

        Collection<Iterator<Integer>> iterators = Arrays.asList(Arrays.asList(1, 2, 3).iterator(), Arrays.asList(4, 5).iterator());

        Iterators.forEach(iterators, 1, 3, 1, 1, 1, n -> sum.addAndGet(n));
        assertEquals(9, sum.get());
    }

    @Test
    public void testForEachCollectionWithOffsetCountThreadsAndOnComplete() {
        AtomicLong sum = new AtomicLong(0);
        AtomicInteger completeCalled = new AtomicInteger(0);

        Collection<Iterator<Integer>> iterators = Arrays.asList(Arrays.asList(1, 2).iterator(), Arrays.asList(3, 4).iterator());

        Iterators.forEach(iterators, 0, 3, 1, 1, 1, n -> sum.addAndGet(n), () -> completeCalled.incrementAndGet());

        assertEquals(6, sum.get());
        assertEquals(1, completeCalled.get());
    }

    @Test
    public void testForEachIteratorWithOffsetCountThreadsAndQueueSize() {
        AtomicInteger sum = new AtomicInteger(0);

        Iterators.forEach(Arrays.asList(1, 2, 3, 4, 5).iterator(), 1, 3, 1, 1, sum::addAndGet);

        assertEquals(9, sum.get());
    }

    @Test
    public void testForEachIteratorWithOffsetCountThreadsQueueSizeAndOnComplete() {
        AtomicInteger sum = new AtomicInteger(0);
        AtomicInteger completeCalled = new AtomicInteger(0);

        Iterators.forEach(Arrays.asList(1, 2, 3, 4, 5).iterator(), 1, 3, 1, 1, sum::addAndGet, () -> completeCalled.incrementAndGet());

        assertEquals(9, sum.get());
        assertEquals(1, completeCalled.get());
    }

    @Test
    public void testForEachCollectionWithOffsetCountReadThreadsProcessThreadsAndQueueSize() {
        AtomicLong sum = new AtomicLong(0);

        Collection<Iterator<Integer>> iterators = Arrays.asList(Arrays.asList(1, 2, 3).iterator(), Arrays.asList(4, 5).iterator());

        Iterators.forEach(iterators, 1, 3, 1, 1, 1, n -> sum.addAndGet(n));
        assertEquals(9, sum.get());
    }

    @Test
    public void testForEachCollectionWithOffsetCountReadThreadsProcessThreadsQueueSizeAndOnComplete() {
        AtomicLong sum = new AtomicLong(0);
        AtomicInteger completeCalled = new AtomicInteger(0);

        Collection<Iterator<Integer>> iterators = Arrays.asList(Arrays.asList(1, 2).iterator(), Arrays.asList(3, 4).iterator());

        Iterators.forEach(iterators, 0, 3, 1, 1, 1, n -> sum.addAndGet(n), () -> completeCalled.incrementAndGet());

        assertEquals(6, sum.get());
        assertEquals(1, completeCalled.get());
    }
}

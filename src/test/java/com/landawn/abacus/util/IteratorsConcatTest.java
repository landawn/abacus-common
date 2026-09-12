package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.util.function.BiConsumer;
import com.landawn.abacus.util.function.BiFunction;
import com.landawn.abacus.util.function.TriConsumer;
import com.landawn.abacus.util.function.TriFunction;

public class IteratorsConcatTest extends IteratorsTestSupport {
    @Test
    public void testConcatBiIterator_NextWithAction() {
        List<Pair<String, Integer>> pairs = new ArrayList<>();
        pairs.add(Pair.of("a", 1));
        pairs.add(Pair.of("b", 2));

        BiIterator<String, Integer> iter = createBiIterator(pairs);
        BiIterator<String, Integer> result = Iterators.concat(iter);

        List<String> keys = new ArrayList<>();
        List<Integer> values = new ArrayList<>();

        result.next((k, v) -> {
            keys.add(k);
            values.add(v);
        });

        assertEquals(1, keys.size());
        assertEquals("a", keys.get(0));
        assertEquals(1, values.get(0));
    }

    @Test
    public void testConcatBiIterator_ForEachRemaining() {
        List<Pair<String, Integer>> pairs1 = new ArrayList<>();
        pairs1.add(Pair.of("a", 1));
        pairs1.add(Pair.of("b", 2));

        List<Pair<String, Integer>> pairs2 = new ArrayList<>();
        pairs2.add(Pair.of("c", 3));

        BiIterator<String, Integer> iter1 = createBiIterator(pairs1);
        BiIterator<String, Integer> iter2 = createBiIterator(pairs2);
        BiIterator<String, Integer> result = Iterators.concat(iter1, iter2);

        List<String> keys = new ArrayList<>();
        List<Integer> values = new ArrayList<>();

        BiConsumer<String, Integer> action = (k, v) -> {
            keys.add(k);
            values.add(v);
        };

        result.forEachRemaining(action);

        assertEquals(3, keys.size());
        assertEquals(List.of("a", "b", "c"), keys);
        assertEquals(List.of(1, 2, 3), values);
    }

    @Test
    public void testConcatBiIterator_Map() {
        List<Pair<String, Integer>> pairs1 = new ArrayList<>();
        pairs1.add(Pair.of("a", 1));
        pairs1.add(Pair.of("b", 2));

        List<Pair<String, Integer>> pairs2 = new ArrayList<>();
        pairs2.add(Pair.of("c", 3));

        BiIterator<String, Integer> iter1 = createBiIterator(pairs1);
        BiIterator<String, Integer> iter2 = createBiIterator(pairs2);
        BiIterator<String, Integer> result = Iterators.concat(iter1, iter2);

        BiFunction<String, Integer, String> mapper = (k, v) -> k + v;
        ObjIterator<String> mapped = result.map(mapper);

        assertTrue(mapped.hasNext());
        assertEquals("a1", mapped.next());
        assertEquals("b2", mapped.next());
        assertEquals("c3", mapped.next());
        assertFalse(mapped.hasNext());
    }

    @Test
    public void testConcatTriIterator_NextWithAction() {
        List<Triple<String, Integer, Double>> triples = new ArrayList<>();
        triples.add(Triple.of("a", 1, 1.1));
        triples.add(Triple.of("b", 2, 2.2));

        TriIterator<String, Integer, Double> iter = createTriIterator(triples);
        TriIterator<String, Integer, Double> result = Iterators.concat(iter);

        List<String> first = new ArrayList<>();
        List<Integer> second = new ArrayList<>();
        List<Double> third = new ArrayList<>();

        result.next((a, b, c) -> {
            first.add(a);
            second.add(b);
            third.add(c);
        });

        assertEquals(1, first.size());
        assertEquals("a", first.get(0));
        assertEquals(1, second.get(0));
        assertEquals(1.1, third.get(0));
    }

    @Test
    public void testConcatTriIterator_ForEachRemaining() {
        List<Triple<String, Integer, Double>> triples1 = new ArrayList<>();
        triples1.add(Triple.of("a", 1, 1.1));
        triples1.add(Triple.of("b", 2, 2.2));

        List<Triple<String, Integer, Double>> triples2 = new ArrayList<>();
        triples2.add(Triple.of("c", 3, 3.3));

        TriIterator<String, Integer, Double> iter1 = createTriIterator(triples1);
        TriIterator<String, Integer, Double> iter2 = createTriIterator(triples2);
        TriIterator<String, Integer, Double> result = Iterators.concat(iter1, iter2);

        List<String> first = new ArrayList<>();
        List<Integer> second = new ArrayList<>();
        List<Double> third = new ArrayList<>();

        TriConsumer<String, Integer, Double> action = (a, b, c) -> {
            first.add(a);
            second.add(b);
            third.add(c);
        };

        result.forEachRemaining(action);

        assertEquals(3, first.size());
        assertEquals(List.of("a", "b", "c"), first);
        assertEquals(List.of(1, 2, 3), second);
        assertEquals(List.of(1.1, 2.2, 3.3), third);
    }

    @Test
    public void testConcatTriIterator_Map() {
        List<Triple<String, Integer, Double>> triples1 = new ArrayList<>();
        triples1.add(Triple.of("a", 1, 1.1));
        triples1.add(Triple.of("b", 2, 2.2));

        List<Triple<String, Integer, Double>> triples2 = new ArrayList<>();
        triples2.add(Triple.of("c", 3, 3.3));

        TriIterator<String, Integer, Double> iter1 = createTriIterator(triples1);
        TriIterator<String, Integer, Double> iter2 = createTriIterator(triples2);
        TriIterator<String, Integer, Double> result = Iterators.concat(iter1, iter2);

        TriFunction<String, Integer, Double, String> mapper = (a, b, c) -> a + b + c;
        ObjIterator<String> mapped = result.map(mapper);

        assertTrue(mapped.hasNext());
        assertEquals("a11.1", mapped.next());
        assertEquals("b22.2", mapped.next());
        assertEquals("c33.3", mapped.next());
        assertFalse(mapped.hasNext());
    }

    @Test
    public void testConcatArrays() {
        boolean[] arr1 = { true, false };
        boolean[] arr2 = { true };
        BooleanIterator iter = Iterators.concat(arr1, arr2);

        assertTrue(iter.nextBoolean());
        assertFalse(iter.nextBoolean());
        assertTrue(iter.nextBoolean());
        assertFalse(iter.hasNext());
    }

    @Test
    public void testConcatCollection() {
        List<Iterator<String>> iterators = Arrays.asList(Arrays.asList("a", "b").iterator(), Arrays.asList("c", "d").iterator());

        ObjIterator<String> result = Iterators.concat(iterators);

        assertEquals("a", result.next());
        assertEquals("b", result.next());
        assertEquals("c", result.next());
        assertEquals("d", result.next());
        assertFalse(result.hasNext());
    }

    @Test
    public void testConcatIteratorCollection() {
        List<Iterator<String>> iterators = Arrays.asList(Arrays.asList("a", "b").iterator(), Arrays.asList("c").iterator(), Arrays.asList("d", "e").iterator());

        ObjIterator<String> result = Iterators.concat(iterators);

        List<String> collected = new ArrayList<>();
        while (result.hasNext()) {
            collected.add(result.next());
        }
        assertEquals(Arrays.asList("a", "b", "c", "d", "e"), collected);
    }

    @Test
    public void testConcatPrimitiveArrays() {
        char[] charArr1 = { 'a', 'b' };
        char[] charArr2 = { 'c', 'd' };
        CharIterator charIter = Iterators.concat(charArr1, charArr2);
        assertEquals('a', charIter.nextChar());
        assertEquals('b', charIter.nextChar());
        assertEquals('c', charIter.nextChar());
        assertEquals('d', charIter.nextChar());
        assertFalse(charIter.hasNext());

        byte[] byteArr1 = { 1, 2 };
        byte[] byteArr2 = { 3, 4 };
        ByteIterator byteIter = Iterators.concat(byteArr1, byteArr2);
        assertEquals((byte) 1, byteIter.nextByte());
        assertEquals((byte) 2, byteIter.nextByte());
        assertEquals((byte) 3, byteIter.nextByte());
        assertEquals((byte) 4, byteIter.nextByte());
        assertFalse(byteIter.hasNext());

        short[] shortArr1 = { 10, 20 };
        short[] shortArr2 = { 30, 40 };
        ShortIterator shortIter = Iterators.concat(shortArr1, shortArr2);
        assertEquals((short) 10, shortIter.nextShort());
        assertEquals((short) 20, shortIter.nextShort());
        assertEquals((short) 30, shortIter.nextShort());
        assertEquals((short) 40, shortIter.nextShort());
        assertFalse(shortIter.hasNext());

        long[] longArr1 = { 100L, 200L };
        long[] longArr2 = { 300L, 400L };
        LongIterator longIter = Iterators.concat(longArr1, longArr2);
        assertEquals(100L, longIter.nextLong());
        assertEquals(200L, longIter.nextLong());
        assertEquals(300L, longIter.nextLong());
        assertEquals(400L, longIter.nextLong());
        assertFalse(longIter.hasNext());

        float[] floatArr1 = { 1.1f, 2.2f };
        float[] floatArr2 = { 3.3f, 4.4f };
        FloatIterator floatIter = Iterators.concat(floatArr1, floatArr2);
        assertEquals(1.1f, floatIter.nextFloat(), 0.001f);
        assertEquals(2.2f, floatIter.nextFloat(), 0.001f);
        assertEquals(3.3f, floatIter.nextFloat(), 0.001f);
        assertEquals(4.4f, floatIter.nextFloat(), 0.001f);
        assertFalse(floatIter.hasNext());

        double[] doubleArr1 = { 1.11, 2.22 };
        double[] doubleArr2 = { 3.33, 4.44 };
        DoubleIterator doubleIter = Iterators.concat(doubleArr1, doubleArr2);
        assertEquals(1.11, doubleIter.nextDouble(), 0.001);
        assertEquals(2.22, doubleIter.nextDouble(), 0.001);
        assertEquals(3.33, doubleIter.nextDouble(), 0.001);
        assertEquals(4.44, doubleIter.nextDouble(), 0.001);
        assertFalse(doubleIter.hasNext());
    }

    // ===================== concat Map overload =====================

    @Test
    public void testConcatMaps_Dedicated() {
        Map<String, Integer> m1 = new HashMap<>();
        m1.put("a", 1);
        Map<String, Integer> m2 = new HashMap<>();
        m2.put("b", 2);

        @SuppressWarnings("unchecked")
        ObjIterator<Map.Entry<String, Integer>> iter = Iterators.concat(m1, m2);
        List<Map.Entry<String, Integer>> result = iter.toList();
        assertEquals(2, result.size());
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
    public void testConcatBiIterator_SingleIterator() {
        List<Pair<String, Integer>> pairs = new ArrayList<>();
        pairs.add(Pair.of("a", 1));
        pairs.add(Pair.of("b", 2));

        BiIterator<String, Integer> iter1 = createBiIterator(pairs);
        BiIterator<String, Integer> result = Iterators.concat(iter1);

        assertTrue(result.hasNext());
        assertEquals(Pair.of("a", 1), result.next());
        assertTrue(result.hasNext());
        assertEquals(Pair.of("b", 2), result.next());
        assertFalse(result.hasNext());
    }

    @Test
    public void testConcatBiIterator_MultipleIterators() {
        List<Pair<String, Integer>> pairs1 = new ArrayList<>();
        pairs1.add(Pair.of("a", 1));
        pairs1.add(Pair.of("b", 2));

        List<Pair<String, Integer>> pairs2 = new ArrayList<>();
        pairs2.add(Pair.of("c", 3));
        pairs2.add(Pair.of("d", 4));

        BiIterator<String, Integer> iter1 = createBiIterator(pairs1);
        BiIterator<String, Integer> iter2 = createBiIterator(pairs2);
        BiIterator<String, Integer> result = Iterators.concat(iter1, iter2);

        assertTrue(result.hasNext());
        assertEquals(Pair.of("a", 1), result.next());
        assertEquals(Pair.of("b", 2), result.next());
        assertEquals(Pair.of("c", 3), result.next());
        assertEquals(Pair.of("d", 4), result.next());
        assertFalse(result.hasNext());
    }

    @Test
    public void testConcatBiIterator_WithEmptyIterators() {
        List<Pair<String, Integer>> pairs1 = new ArrayList<>();
        pairs1.add(Pair.of("a", 1));

        List<Pair<String, Integer>> emptyPairs = new ArrayList<>();

        List<Pair<String, Integer>> pairs2 = new ArrayList<>();
        pairs2.add(Pair.of("b", 2));

        BiIterator<String, Integer> iter1 = createBiIterator(pairs1);
        BiIterator<String, Integer> emptyIter = createBiIterator(emptyPairs);
        BiIterator<String, Integer> iter2 = createBiIterator(pairs2);
        BiIterator<String, Integer> result = Iterators.concat(iter1, emptyIter, iter2);

        assertTrue(result.hasNext());
        assertEquals(Pair.of("a", 1), result.next());
        assertTrue(result.hasNext());
        assertEquals(Pair.of("b", 2), result.next());
        assertFalse(result.hasNext());
    }

    @Test
    public void testConcatTriIterator_SingleIterator() {
        List<Triple<String, Integer, Double>> triples = new ArrayList<>();
        triples.add(Triple.of("a", 1, 1.1));
        triples.add(Triple.of("b", 2, 2.2));

        TriIterator<String, Integer, Double> iter1 = createTriIterator(triples);
        TriIterator<String, Integer, Double> result = Iterators.concat(iter1);

        assertTrue(result.hasNext());
        assertEquals(Triple.of("a", 1, 1.1), result.next());
        assertTrue(result.hasNext());
        assertEquals(Triple.of("b", 2, 2.2), result.next());
        assertFalse(result.hasNext());
    }

    @Test
    public void testConcatTriIterator_MultipleIterators() {
        List<Triple<String, Integer, Double>> triples1 = new ArrayList<>();
        triples1.add(Triple.of("a", 1, 1.1));
        triples1.add(Triple.of("b", 2, 2.2));

        List<Triple<String, Integer, Double>> triples2 = new ArrayList<>();
        triples2.add(Triple.of("c", 3, 3.3));
        triples2.add(Triple.of("d", 4, 4.4));

        TriIterator<String, Integer, Double> iter1 = createTriIterator(triples1);
        TriIterator<String, Integer, Double> iter2 = createTriIterator(triples2);
        TriIterator<String, Integer, Double> result = Iterators.concat(iter1, iter2);

        assertTrue(result.hasNext());
        assertEquals(Triple.of("a", 1, 1.1), result.next());
        assertEquals(Triple.of("b", 2, 2.2), result.next());
        assertEquals(Triple.of("c", 3, 3.3), result.next());
        assertEquals(Triple.of("d", 4, 4.4), result.next());
        assertFalse(result.hasNext());
    }

    @Test
    public void testConcatTriIterator_WithEmptyIterators() {
        List<Triple<String, Integer, Double>> triples1 = new ArrayList<>();
        triples1.add(Triple.of("a", 1, 1.1));

        List<Triple<String, Integer, Double>> emptyTriples = new ArrayList<>();

        List<Triple<String, Integer, Double>> triples2 = new ArrayList<>();
        triples2.add(Triple.of("b", 2, 2.2));

        TriIterator<String, Integer, Double> iter1 = createTriIterator(triples1);
        TriIterator<String, Integer, Double> emptyIter = createTriIterator(emptyTriples);
        TriIterator<String, Integer, Double> iter2 = createTriIterator(triples2);
        TriIterator<String, Integer, Double> result = Iterators.concat(iter1, emptyIter, iter2);

        assertTrue(result.hasNext());
        assertEquals(Triple.of("a", 1, 1.1), result.next());
        assertTrue(result.hasNext());
        assertEquals(Triple.of("b", 2, 2.2), result.next());
        assertFalse(result.hasNext());
    }

    @Test
    public void testConcatBiIterator_HasNextMultipleCalls() {
        List<Pair<String, Integer>> pairs = new ArrayList<>();
        pairs.add(Pair.of("a", 1));

        BiIterator<String, Integer> iter = createBiIterator(pairs);
        BiIterator<String, Integer> result = Iterators.concat(iter);

        assertTrue(result.hasNext());
        assertTrue(result.hasNext());
        assertTrue(result.hasNext());

        assertEquals(Pair.of("a", 1), result.next());
        assertFalse(result.hasNext());
        assertFalse(result.hasNext());
    }

    @Test
    public void testConcatTriIterator_HasNextMultipleCalls() {
        List<Triple<String, Integer, Double>> triples = new ArrayList<>();
        triples.add(Triple.of("a", 1, 1.1));

        TriIterator<String, Integer, Double> iter = createTriIterator(triples);
        TriIterator<String, Integer, Double> result = Iterators.concat(iter);

        assertTrue(result.hasNext());
        assertTrue(result.hasNext());
        assertTrue(result.hasNext());

        assertEquals(Triple.of("a", 1, 1.1), result.next());
        assertFalse(result.hasNext());
        assertFalse(result.hasNext());
    }

    @Test
    public void testConcatPrimitiveIterators() {
        IntIterator intIter1 = IntIterator.of(1, 2);
        IntIterator intIter2 = IntIterator.of(3, 4);
        IntIterator concatenated = Iterators.concat(intIter1, intIter2);
        assertEquals(1, concatenated.nextInt());
        assertEquals(2, concatenated.nextInt());
        assertEquals(3, concatenated.nextInt());
        assertEquals(4, concatenated.nextInt());
        assertFalse(concatenated.hasNext());

        IntIterator empty = IntIterator.empty();
        IntIterator nonEmpty = IntIterator.of(5, 6);
        concatenated = Iterators.concat(empty, nonEmpty);
        assertEquals(5, concatenated.nextInt());
        assertEquals(6, concatenated.nextInt());
        assertFalse(concatenated.hasNext());
    }

    // ===================== concat Additional Edge Cases =====================

    @Test
    public void testConcatObjectArrays_SingleArray() {
        ObjIterator<String> iter = Iterators.concat(new String[] { "a", "b" });
        assertEquals(Arrays.asList("a", "b"), iter.toList());
    }

    @Test
    public void testConcatObjectArrays_EmptyArrays() {
        ObjIterator<String> iter = Iterators.concat(new String[0], new String[0]);
        assertFalse(iter.hasNext());
    }

    @Test
    public void testConcatIterators_SingleIterator() {
        @SuppressWarnings("unchecked")
        ObjIterator<Integer> iter = Iterators.concat(Arrays.asList(1, 2, 3).iterator());
        assertEquals(Arrays.asList(1, 2, 3), iter.toList());
    }

    @Test
    public void testConcatIterators_EmptyIterators() {
        @SuppressWarnings("unchecked")
        ObjIterator<Integer> iter = Iterators.concat(Collections.<Integer> emptyIterator(), Collections.<Integer> emptyIterator());
        assertFalse(iter.hasNext());
    }

    @Test
    public void testConcatCollection_EmptyCollection() {
        ObjIterator<String> iter = Iterators.concat(Collections.<Iterator<String>> emptyList());
        assertFalse(iter.hasNext());
    }

    // ===================== concat Primitive Iterator Empty Edge Cases =====================

    @Test
    public void testConcatBooleanIterators_Empty() {
        BooleanIterator result = Iterators.concat(BooleanIterator.empty(), BooleanIterator.empty());
        assertFalse(result.hasNext());
    }

    @Test
    public void testConcatCharIterators_Empty() {
        CharIterator result = Iterators.concat(CharIterator.empty(), CharIterator.empty());
        assertFalse(result.hasNext());
    }

    @Test
    public void testConcatByteIterators_Empty() {
        ByteIterator result = Iterators.concat(ByteIterator.empty(), ByteIterator.empty());
        assertFalse(result.hasNext());
    }

    @Test
    public void testConcatShortIterators_Empty() {
        ShortIterator result = Iterators.concat(ShortIterator.empty(), ShortIterator.empty());
        assertFalse(result.hasNext());
    }

    @Test
    public void testConcatIntIterators_Empty() {
        IntIterator result = Iterators.concat(IntIterator.empty(), IntIterator.empty());
        assertFalse(result.hasNext());
    }

    @Test
    public void testConcatLongIterators_Empty() {
        LongIterator result = Iterators.concat(LongIterator.empty(), LongIterator.empty());
        assertFalse(result.hasNext());
    }

    @Test
    public void testConcatFloatIterators_Empty() {
        FloatIterator result = Iterators.concat(FloatIterator.empty(), FloatIterator.empty());
        assertFalse(result.hasNext());
    }

    @Test
    public void testConcatDoubleIterators_Empty() {
        DoubleIterator result = Iterators.concat(DoubleIterator.empty(), DoubleIterator.empty());
        assertFalse(result.hasNext());
    }

    // ===================== concat Primitive Array Empty Edge Cases =====================

    @Test
    public void testConcatBooleanArrays_Empty() {
        BooleanIterator result = Iterators.concat(new boolean[0], new boolean[0]);
        assertFalse(result.hasNext());
    }

    @Test
    public void testConcatCharArrays_Empty() {
        CharIterator result = Iterators.concat(new char[0], new char[0]);
        assertFalse(result.hasNext());
    }

    @Test
    public void testConcatByteArrays_Empty() {
        ByteIterator result = Iterators.concat(new byte[0], new byte[0]);
        assertFalse(result.hasNext());
    }

    @Test
    public void testConcatShortArrays_Empty() {
        ShortIterator result = Iterators.concat(new short[0], new short[0]);
        assertFalse(result.hasNext());
    }

    @Test
    public void testConcatIntArrays_Empty() {
        IntIterator result = Iterators.concat(new int[0], new int[0]);
        assertFalse(result.hasNext());
    }

    @Test
    public void testConcatLongArrays_Empty() {
        LongIterator result = Iterators.concat(new long[0], new long[0]);
        assertFalse(result.hasNext());
    }

    @Test
    public void testConcatFloatArrays_Empty() {
        FloatIterator result = Iterators.concat(new float[0], new float[0]);
        assertFalse(result.hasNext());
    }

    @Test
    public void testConcatDoubleArrays_Empty() {
        DoubleIterator result = Iterators.concat(new double[0], new double[0]);
        assertFalse(result.hasNext());
    }

    @Test
    public void testConcatMaps_MultipleMapsCoverage() {
        Map<String, Integer> map1 = new HashMap<>();
        map1.put("a", 1);
        map1.put("b", 2);
        Map<String, Integer> map2 = new HashMap<>();
        map2.put("c", 3);

        @SuppressWarnings("unchecked")
        ObjIterator<Map.Entry<String, Integer>> result = Iterators.concat(map1, map2);
        List<Map.Entry<String, Integer>> entries = new ArrayList<>();
        result.forEachRemaining(entries::add);
        assertEquals(3, entries.size());
    }

    @Test
    public void testConcatMaps_EmptyMapArrayCoverage() {
        @SuppressWarnings("unchecked")
        ObjIterator<Map.Entry<String, Integer>> result = Iterators.concat(new Map[0]);
        assertFalse(result.hasNext());
    }

    @Test
    public void testConcatMaps_SingleMap() {
        Map<String, Integer> map = new HashMap<>();
        map.put("x", 10);

        @SuppressWarnings("unchecked")
        ObjIterator<Map.Entry<String, Integer>> result = Iterators.concat(map);
        List<Map.Entry<String, Integer>> entries = new ArrayList<>();
        result.forEachRemaining(entries::add);
        assertEquals(1, entries.size());
        assertEquals("x", entries.get(0).getKey());
        assertEquals(10, entries.get(0).getValue());
    }

    @Test
    public void testConcatBiIterator_EmptyArray() {
        BiIterator<String, Integer> result = Iterators.concat(new BiIterator[0]);
        assertFalse(result.hasNext());
        assertThrows(NoSuchElementException.class, () -> result.next());
    }

    @Test
    public void testConcatBiIterator_MapWithEmptyIterators() {
        List<Pair<String, Integer>> pairs = new ArrayList<>();
        pairs.add(Pair.of("a", 1));

        List<Pair<String, Integer>> emptyPairs = new ArrayList<>();

        BiIterator<String, Integer> iter1 = createBiIterator(pairs);
        BiIterator<String, Integer> emptyIter = createBiIterator(emptyPairs);
        BiIterator<String, Integer> result = Iterators.concat(iter1, emptyIter);

        BiFunction<String, Integer, String> mapper = (k, v) -> k + v;
        ObjIterator<String> mapped = result.map(mapper);

        assertTrue(mapped.hasNext());
        assertEquals("a1", mapped.next());
        assertFalse(mapped.hasNext());
        assertThrows(NoSuchElementException.class, () -> mapped.next());
    }

    @Test
    public void testConcatTriIterator_EmptyArray() {
        TriIterator<String, Integer, Double> result = Iterators.concat(new TriIterator[0]);
        assertFalse(result.hasNext());
        assertThrows(NoSuchElementException.class, () -> result.next());
    }

    @Test
    public void testConcatTriIterator_MapWithEmptyIterators() {
        List<Triple<String, Integer, Double>> triples = new ArrayList<>();
        triples.add(Triple.of("a", 1, 1.1));

        List<Triple<String, Integer, Double>> emptyTriples = new ArrayList<>();

        TriIterator<String, Integer, Double> iter1 = createTriIterator(triples);
        TriIterator<String, Integer, Double> emptyIter = createTriIterator(emptyTriples);
        TriIterator<String, Integer, Double> result = Iterators.concat(iter1, emptyIter);

        TriFunction<String, Integer, Double, String> mapper = (a, b, c) -> a + b + c;
        ObjIterator<String> mapped = result.map(mapper);

        assertTrue(mapped.hasNext());
        assertEquals("a11.1", mapped.next());
        assertFalse(mapped.hasNext());
        assertThrows(NoSuchElementException.class, () -> mapped.next());
    }

    @Test
    public void testConcatBiIterator_AllEmptyIterators() {
        BiIterator<String, Integer> empty1 = BiIterator.empty();
        BiIterator<String, Integer> empty2 = BiIterator.empty();
        BiIterator<String, Integer> empty3 = BiIterator.empty();

        BiIterator<String, Integer> result = Iterators.concat(empty1, empty2, empty3);

        assertFalse(result.hasNext());
        assertThrows(NoSuchElementException.class, () -> result.next());
    }

    @Test
    public void testConcatTriIterator_AllEmptyIterators() {
        TriIterator<String, Integer, Double> empty1 = TriIterator.empty();
        TriIterator<String, Integer, Double> empty2 = TriIterator.empty();
        TriIterator<String, Integer, Double> empty3 = TriIterator.empty();

        TriIterator<String, Integer, Double> result = Iterators.concat(empty1, empty2, empty3);

        assertFalse(result.hasNext());
        assertThrows(NoSuchElementException.class, () -> result.next());
    }

    @Test
    public void testConcatIterables_02() {
        List<Iterable<String>> iterables = Arrays.asList(Arrays.asList("a", "b"), Arrays.asList("c", "d"));

        ObjIterator<String> result = Iterators.concatIterables(iterables);

        assertEquals("a", result.next());
        assertEquals("b", result.next());
        assertEquals("c", result.next());
        assertEquals("d", result.next());
        assertFalse(result.hasNext());
    }

    @Test
    public void testConcatIterables() {
        ObjIterator<String> iter = Iterators.concat(Arrays.asList("a", "b"), Arrays.asList("c"), Collections.emptyList());
        assertEquals(Arrays.asList("a", "b", "c"), iter.toList());

        iter = Iterators.concat((Iterable<String>[]) null);
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
    public void testConcatIterablesEdgeCases() {
        List<Iterable<String>> withNull = new ArrayList<>();
        withNull.add(Arrays.asList("a", "b"));
        withNull.add(null);
        withNull.add(Arrays.asList("c", "d"));

        ObjIterator<String> result = Iterators.concatIterables(withNull);
        List<String> collected = new ArrayList<>();
        while (result.hasNext()) {
            collected.add(result.next());
        }
        assertEquals(Arrays.asList("a", "b", "c", "d"), collected);
    }

    @Test
    public void testConcatIterables_EmptyIterables() {
        @SuppressWarnings("unchecked")
        ObjIterator<String> iter = Iterators.concat(Collections.<String> emptyList(), Collections.<String> emptyList());
        assertFalse(iter.hasNext());
    }

    @Test
    public void testConcatIterablesCollection_EmptyCollection() {
        ObjIterator<String> iter = Iterators.concatIterables(Collections.<Iterable<String>> emptyList());
        assertFalse(iter.hasNext());
    }

    @Test
    public void testConcatIterableVarargsObtainsIteratorsLazily() {
        final AtomicInteger firstIteratorCalls = new AtomicInteger();
        final AtomicInteger secondIteratorCalls = new AtomicInteger();
        final Iterable<Integer> first = () -> {
            firstIteratorCalls.incrementAndGet();
            return Collections.singleton(1).iterator();
        };
        final Iterable<Integer> second = () -> {
            secondIteratorCalls.incrementAndGet();
            return Collections.singleton(2).iterator();
        };

        final ObjIterator<Integer> concatenated = Iterators.concat(first, second);
        assertEquals(0, firstIteratorCalls.get());
        assertEquals(0, secondIteratorCalls.get());

        assertTrue(concatenated.hasNext());
        assertEquals(1, firstIteratorCalls.get());
        assertEquals(0, secondIteratorCalls.get());
        assertEquals(Integer.valueOf(1), concatenated.next());

        assertTrue(concatenated.hasNext());
        assertEquals(1, secondIteratorCalls.get());
        assertEquals(Integer.valueOf(2), concatenated.next());
    }

    @Test
    public void testConcatBiAndTriIteratorsValidateCallbacksUpFront() {
        final BiIterator<String, Integer> bi = Iterators.concat(BiIterator.empty());
        assertThrows(IllegalArgumentException.class, () -> bi.map(null));
        assertThrows(IllegalArgumentException.class, () -> bi.forEachRemaining((BiConsumer<String, Integer>) null));
        assertThrows(IllegalArgumentException.class, () -> bi.foreachRemaining((Throwables.BiConsumer<String, Integer, RuntimeException>) null));

        final TriIterator<String, Integer, Double> tri = Iterators.concat(TriIterator.empty());
        assertThrows(IllegalArgumentException.class, () -> tri.map(null));
        assertThrows(IllegalArgumentException.class, () -> tri.forEachRemaining((TriConsumer<String, Integer, Double>) null));
        assertThrows(IllegalArgumentException.class, () -> tri.foreachRemaining((Throwables.TriConsumer<String, Integer, Double, RuntimeException>) null));
    }

    // ===================== concat: null iterators handled / lazy =====================

    @Test
    public void testConcat_NullIteratorEntriesHandled() {
        Iterator<Integer> a = Arrays.asList(1, 2).iterator();
        Iterator<Integer> b = null;
        Iterator<Integer> c = Arrays.asList(3).iterator();
        // Null iterators in the array should be skipped, not produce NPE.
        ObjIterator<Integer> r = Iterators.concat(a, b, c);
        assertEquals(Arrays.asList(1, 2, 3), r.toList());
    }

    @Test
    public void testConcat_AllNulls() {
        @SuppressWarnings("unchecked")
        Iterator<Integer>[] all = new Iterator[] { null, null };
        ObjIterator<Integer> r = Iterators.concat(all);
        assertFalse(r.hasNext());
    }

    @Test
    public void testConcat_EmptyVarargs() {
        @SuppressWarnings("unchecked")
        Iterator<String>[] empty = new Iterator[0];
        ObjIterator<String> r = Iterators.concat(empty);
        assertFalse(r.hasNext());
    }

    @Test
    public void testConcat_LazyDoesNotConsumeFirstUntilNeeded() {
        AtomicBoolean firstAdvanced = new AtomicBoolean(false);
        Iterator<Integer> first = new Iterator<>() {
            final Iterator<Integer> inner = Arrays.asList(1, 2).iterator();

            @Override
            public boolean hasNext() {
                return inner.hasNext();
            }

            @Override
            public Integer next() {
                firstAdvanced.set(true);
                return inner.next();
            }
        };
        Iterators.concat(first, Arrays.asList(3).iterator());
        // Concat should not have advanced the source iterator at construction time.
        assertFalse(firstAdvanced.get());
    }

    /** D6: concat copies its varargs array, so a later write to it cannot change what the iterator yields. */
    @Test
    public void testConcat_varargsArrayIsCopied() {
        @SuppressWarnings("unchecked")
        final Iterator<String>[] iterators = new Iterator[] { Arrays.asList("x").iterator(), Arrays.asList("y").iterator() };
        final ObjIterator<String> objIter = Iterators.concat(iterators);
        iterators[1] = Arrays.asList("MUTATED").iterator();
        assertEquals(Arrays.asList("x", "y"), drainToList(objIter));

        final int[][] arrays = { { 1 }, { 2 } };
        final IntIterator intIter = Iterators.concat(arrays);
        arrays[1] = new int[] { 99 };
        Assertions.assertArrayEquals(new int[] { 1, 2 }, intIter.toArray());

        final IntIterator[] intIterators = { IntIterator.of(1), IntIterator.of(2) };
        final IntIterator concatenated = Iterators.concat(intIterators);
        intIterators[1] = IntIterator.of(99);
        Assertions.assertArrayEquals(new int[] { 1, 2 }, concatenated.toArray());

        @SuppressWarnings("unchecked")
        final Iterable<String>[] iterables = new Iterable[] { Arrays.asList("x"), Arrays.asList("y") };
        final ObjIterator<String> fromIterables = Iterators.concat(iterables);
        iterables[1] = Arrays.asList("MUTATED");
        assertEquals(Arrays.asList("x", "y"), drainToList(fromIterables));
    }

    /**
     * J1: the javadoc claimed "The array is copied, so writing to it afterwards does not change what the returned
     * iterator yields". Only the <i>varargs</i> array is cloned - the supplied arrays are read lazily and are not
     * copied, so writing into one is visible.
     */
    @Test
    public void testConcatPrimitiveArrays_varargsArrayIsCopiedButSuppliedArraysAreLive() {
        final int[] first = { 1, 2 };
        final int[] second = { 3 };

        final IntIterator live = Iterators.concat(first, second);
        first[0] = 99;
        assertEquals(Arrays.asList(99, 2, 3), drainInts(live), "the supplied arrays are not copied");

        final int[][] varargs = { { 1, 2 }, { 3 } };
        final IntIterator fromVarargs = Iterators.concat(varargs);
        varargs[0] = new int[] { 7, 7 };
        assertEquals(Arrays.asList(1, 2, 3), drainInts(fromVarargs), "the varargs array itself is copied");

        // The same split holds for the object overload, whose javadoc now says so too.
        final String[] words = { "a", "b" };
        final ObjIterator<String> objLive = Iterators.concat(words, new String[] { "z" });
        words[0] = "MUTATED";
        assertEquals(Arrays.asList("MUTATED", "b", "z"), drainToList(objLive));
    }

    /**
     * D3: {@code concat(Map...)} grabbed every {@code entrySet().iterator()} up front, so modifying any of the maps
     * afterwards threw {@code ConcurrentModificationException} on the first {@code next()} - even for a map whose
     * turn had not come yet. Its siblings {@code concat(Iterable...)} / {@code concatIterables} are lazy.
     */
    @Test
    public void testConcatMaps_entrySetsAreIteratedLazily() {
        final Map<String, Integer> first = new java.util.LinkedHashMap<>();
        first.put("a", 1);
        final Map<String, Integer> second = new java.util.LinkedHashMap<>();
        second.put("b", 2);

        final ObjIterator<Map.Entry<String, Integer>> iter = Iterators.concat(first, second);
        second.put("c", 3); // second's turn has not come yet

        final List<String> keys = new ArrayList<>();

        while (iter.hasNext()) {
            keys.add(iter.next().getKey());
        }

        assertEquals(Arrays.asList("a", "b", "c"), keys);

        // A map that is empty when concat() is called but filled before its turn still contributes.
        final Map<String, Integer> filledLater = new java.util.LinkedHashMap<>();
        final ObjIterator<Map.Entry<String, Integer>> lazy = Iterators.concat(Collections.singletonMap("x", 1), filledLater);
        filledLater.put("y", 2);
        assertEquals(Arrays.asList("x", "y"), Iterators.map(lazy, Map.Entry::getKey).toList());

        // null maps are still skipped, and an all-null input is still empty.
        @SuppressWarnings("unchecked")
        final Map<String, Integer>[] withNull = new Map[] { Collections.singletonMap("k", 1), null, Collections.emptyMap() };
        assertEquals(Arrays.asList("k"), Iterators.map(Iterators.concat(withNull), Map.Entry::getKey).toList());

        @SuppressWarnings("unchecked")
        final Map<String, Integer>[] allNull = new Map[] { null };
        assertFalse(Iterators.concat(allNull).hasNext());
    }
}

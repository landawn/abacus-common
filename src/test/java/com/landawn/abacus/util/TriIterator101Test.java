package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.u.Optional;
import com.landawn.abacus.util.function.BiConsumer;
import com.landawn.abacus.util.function.BooleanSupplier;
import com.landawn.abacus.util.function.IntObjConsumer;
import com.landawn.abacus.util.function.TriPredicate;
import com.landawn.abacus.util.stream.Stream;

@Tag("new-test")
public class TriIterator101Test extends TestBase {

    @Test
    public void testForEachRemainingConsumerDeprecated() {
        String[] arr1 = { "a", "b", "c" };
        Integer[] arr2 = { 1, 2, 3 };
        Double[] arr3 = { 1.1, 2.2, 3.3 };

        TriIterator<String, Integer, Double> iter = TriIterator.zip(arr1, arr2, arr3);

        List<Triple<String, Integer, Double>> collected = new ArrayList<>();
        Consumer<Triple<String, Integer, Double>> consumer = collected::add;

        iter.next();
        iter.forEachRemaining(consumer);

        assertEquals(2, collected.size());
        assertEquals("b", collected.get(0).left());
        assertEquals("c", collected.get(1).left());
    }

    @Test
    public void testForeachRemainingWithThrowables() throws IOException {
        String[] arr1 = { "a", "b", "c" };
        Integer[] arr2 = { 1, 2, 3 };
        Double[] arr3 = { 1.1, 2.2, 3.3 };

        TriIterator<String, Integer, Double> iter = TriIterator.zip(arr1, arr2, arr3);

        List<String> results = new ArrayList<>();

        Throwables.TriConsumer<String, Integer, Double, IOException> action = (s, i, d) -> {
            if (i == 2) {
                throw new IOException("Test exception at 2");
            }
            results.add(s + i + ":" + d);
        };

        try {
            iter.foreachRemaining(action);
            fail("Should throw IOException");
        } catch (IOException e) {
            assertEquals("Test exception at 2", e.getMessage());
            assertEquals(1, results.size());
            assertEquals("a1:1.1", results.get(0));
        }
    }

    @Test
    public void testEmptyIteratorMethods() {
        TriIterator<String, Integer, Double> empty = TriIterator.empty();

        AtomicInteger count = new AtomicInteger(0);
        empty.forEachRemaining((a, b, c) -> count.incrementAndGet());
        assertEquals(0, count.get());

        ObjIterator<String> mapped = empty.map((a, b, c) -> a + b + c);
        assertFalse(mapped.hasNext());
    }

    @Test
    public void testGenerateEdgeCases() {
        BooleanSupplier alwaysFalse = () -> false;
        Consumer<Triple<String, Integer, Double>> output = triple -> {
            fail("Should never be called");
        };

        TriIterator<String, Integer, Double> iter = TriIterator.generate(alwaysFalse, output);
        assertFalse(iter.hasNext());

        IntObjConsumer<Triple<Integer, Integer, Integer>> indexOutput = (idx, triple) -> {
            triple.set(idx, idx * 2, idx * 3);
        };

        TriIterator<Integer, Integer, Integer> indexIter = TriIterator.generate(0, 1, indexOutput);
        assertTrue(indexIter.hasNext());
        Triple<Integer, Integer, Integer> single = indexIter.next();
        assertEquals(Integer.valueOf(0), single.left());
        assertEquals(Integer.valueOf(0), single.middle());
        assertEquals(Integer.valueOf(0), single.right());
        assertFalse(indexIter.hasNext());
    }

    @Test
    public void testZipWithNullElements() {
        String[] arr1 = { "a", null, "c" };
        Integer[] arr2 = { 1, 2, null };
        Double[] arr3 = { null, 2.2, 3.3 };

        TriIterator<String, Integer, Double> iter = TriIterator.zip(arr1, arr2, arr3);

        List<Triple<String, Integer, Double>> result = iter.toList();
        assertEquals(3, result.size());

        assertEquals("a", result.get(0).left());
        assertEquals(Integer.valueOf(1), result.get(0).middle());
        assertNull(result.get(0).right());

        assertNull(result.get(1).left());
        assertEquals(Integer.valueOf(2), result.get(1).middle());
        assertEquals(2.2, result.get(1).right(), 0.001);

        assertEquals("c", result.get(2).left());
        assertNull(result.get(2).middle());
        assertEquals(3.3, result.get(2).right(), 0.001);
    }

    @Test
    public void testZipEmptyCollections() {
        List<String> empty1 = Collections.emptyList();
        List<Integer> empty2 = Collections.emptyList();
        List<Double> empty3 = Collections.emptyList();

        TriIterator<String, Integer, Double> iter = TriIterator.zip(empty1, empty2, empty3);
        assertFalse(iter.hasNext());

        List<String> list1 = Arrays.asList("a", "b");
        List<Integer> list2 = Collections.emptyList();
        List<Double> list3 = Arrays.asList(1.1, 2.2);

        iter = TriIterator.zip(list1, list2, list3);
        assertFalse(iter.hasNext());
    }

    @Test
    public void testZipWithDefaultsAllEmpty() {
        Iterator<String> iter1 = Collections.<String> emptyList().iterator();
        Iterator<Integer> iter2 = Collections.<Integer> emptyList().iterator();
        Iterator<Double> iter3 = Collections.<Double> emptyList().iterator();

        TriIterator<String, Integer, Double> triIter = TriIterator.zip(iter1, iter2, iter3, "default", -1, -1.0);
        assertFalse(triIter.hasNext());
    }

    @Test
    public void testUnzipWithNullInput() {
        BiConsumer<String, Triple<String, Integer, Double>> unzipFunction = (s, t) -> {
        };

        TriIterator<String, Integer, Double> iter = TriIterator.unzip((Iterable<String>) null, unzipFunction);
        assertFalse(iter.hasNext());

        iter = TriIterator.unzip((Iterator<String>) null, unzipFunction);
        assertFalse(iter.hasNext());
    }

    @Test
    public void testComplexUnzip() {
        List<Map<String, Object>> source = new ArrayList<>();
        Map<String, Object> map1 = new HashMap<>();
        map1.put("name", "Alice");
        map1.put("age", 25);
        map1.put("score", 95.5);
        source.add(map1);

        Map<String, Object> map2 = new HashMap<>();
        map2.put("name", "Bob");
        map2.put("age", 30);
        map2.put("score", 88.0);
        source.add(map2);

        BiConsumer<Map<String, Object>, Triple<String, Integer, Double>> unzipFunction = (map, triple) -> {
            triple.set((String) map.get("name"), (Integer) map.get("age"), (Double) map.get("score"));
        };

        TriIterator<String, Integer, Double> iter = TriIterator.unzip(source, unzipFunction);

        List<Triple<String, Integer, Double>> result = iter.toList();
        assertEquals(2, result.size());
        assertEquals("Alice", result.get(0).left());
        assertEquals(Integer.valueOf(25), result.get(0).middle());
        assertEquals(95.5, result.get(0).right(), 0.001);
    }

    @Test
    public void testSkipAndLimitCombinations() {
        String[] arr1 = { "a", "b", "c", "d", "e" };
        Integer[] arr2 = { 1, 2, 3, 4, 5 };
        Double[] arr3 = { 1.1, 2.2, 3.3, 4.4, 5.5 };

        TriIterator<String, Integer, Double> iter = TriIterator.zip(arr1, arr2, arr3).skip(0);
        assertEquals(5, iter.toList().size());

        iter = TriIterator.zip(arr1, arr2, arr3).limit(10);
        assertEquals(5, iter.toList().size());

        iter = TriIterator.zip(arr1, arr2, arr3).skip(10).limit(5);
        assertEquals(0, iter.toList().size());
    }

    @Test
    public void testFilterAllElementsFiltered() {
        String[] arr1 = { "a", "b", "c" };
        Integer[] arr2 = { 1, 2, 3 };
        Double[] arr3 = { 1.1, 2.2, 3.3 };

        TriPredicate<String, Integer, Double> rejectAll = (s, i, d) -> false;

        TriIterator<String, Integer, Double> iter = TriIterator.zip(arr1, arr2, arr3).filter(rejectAll);
        assertFalse(iter.hasNext());
        assertEquals(0, iter.toList().size());
    }

    @Test
    public void testFilterWithComplexPredicate() {
        String[] arr1 = { "apple", "banana", "cherry", "date", "elderberry" };
        Integer[] arr2 = { 1, 2, 3, 4, 5 };
        Double[] arr3 = { 1.5, 2.5, 3.5, 4.5, 5.5 };

        TriPredicate<String, Integer, Double> complex = (s, i, d) -> (s.length() > 4 && i % 2 == 0) || d > 4.0;

        TriIterator<String, Integer, Double> iter = TriIterator.zip(arr1, arr2, arr3).filter(complex);

        List<Triple<String, Integer, Double>> result = iter.toList();
        assertEquals(3, result.size());
    }

    @Test
    public void testMapChaining() {
        String[] arr1 = { "1", "2", "3" };
        Integer[] arr2 = { 10, 20, 30 };
        Double[] arr3 = { 100.0, 200.0, 300.0 };

        ObjIterator<Double> iter = TriIterator.zip(arr1, arr2, arr3).map((s, i, d) -> Integer.parseInt(s) + i + d).map(sum -> sum * 2);

        List<Double> result = new ArrayList<>();
        iter.forEachRemaining(result::add);

        assertEquals(3, result.size());
        assertEquals(222.0, result.get(0), 0.001);
        assertEquals(444.0, result.get(1), 0.001);
        assertEquals(666.0, result.get(2), 0.001);
    }

    @Test
    public void testFirstAfterPartialConsumption() {
        String[] arr1 = { "a", "b", "c" };
        Integer[] arr2 = { 1, 2, 3 };
        Double[] arr3 = { 1.1, 2.2, 3.3 };

        TriIterator<String, Integer, Double> iter = TriIterator.zip(arr1, arr2, arr3);

        iter.next();

        Optional<Triple<String, Integer, Double>> first = iter.first();
        assertTrue(first.isPresent());
        assertEquals("b", first.get().left());

        iter.next();
        assertFalse(iter.hasNext());
    }

    @Test
    public void testToArrayWithProvidedArray() {
        String[] arr1 = { "a", "b", "c" };
        Integer[] arr2 = { 1, 2, 3 };
        Double[] arr3 = { 1.1, 2.2, 3.3 };

        TriIterator<String, Integer, Double> iter = TriIterator.zip(arr1, arr2, arr3);

        @SuppressWarnings("unchecked")
        Triple<String, Integer, Double>[] smallArray = new Triple[1];
        Triple<String, Integer, Double>[] result = iter.toArray(smallArray);

        assertNotSame(smallArray, result);
        assertEquals(3, result.length);

        iter = TriIterator.zip(arr1, arr2, arr3);
        @SuppressWarnings("unchecked")
        Triple<String, Integer, Double>[] exactArray = new Triple[3];
        result = iter.toArray(exactArray);

        assertSame(exactArray, result);
        assertEquals(3, result.length);

        iter = TriIterator.zip(arr1, arr2, arr3);
        @SuppressWarnings("unchecked")
        Triple<String, Integer, Double>[] largeArray = new Triple[5];
        result = iter.toArray(largeArray);

        assertSame(largeArray, result);
        assertNull(result[3]);
        assertNull(result[4]);
    }

    @Test
    public void testStreamWithEmptyIterator() {
        TriIterator<String, Integer, Double> empty = TriIterator.empty();

        Stream<String> stream = empty.stream((a, b, c) -> a + b + c);
        assertEquals(0, stream.count());
    }

    @Test
    public void testMultipleHasNextCalls() {
        String[] arr1 = { "a" };
        Integer[] arr2 = { 1 };
        Double[] arr3 = { 1.1 };

        TriIterator<String, Integer, Double> iter = TriIterator.zip(arr1, arr2, arr3);

        assertTrue(iter.hasNext());
        assertTrue(iter.hasNext());
        assertTrue(iter.hasNext());

        Triple<String, Integer, Double> element = iter.next();
        assertEquals("a", element.left());

        assertFalse(iter.hasNext());
        assertFalse(iter.hasNext());
    }

    @Test
    public void testGenerateMapFunction() {
        AtomicInteger counter = new AtomicInteger(0);
        BooleanSupplier hasNext = () -> counter.get() < 3;
        Consumer<Triple<Integer, Integer, Integer>> output = triple -> {
            int val = counter.getAndIncrement();
            triple.set(val, val * 10, val * 100);
        };

        TriIterator<Integer, Integer, Integer> iter = TriIterator.generate(hasNext, output);

        ObjIterator<Integer> sumIter = iter.map((a, b, c) -> a + b + c);

        List<Integer> sums = new ArrayList<>();
        sumIter.forEachRemaining(sums::add);

        assertEquals(3, sums.size());
        assertEquals(Integer.valueOf(0), sums.get(0));
        assertEquals(Integer.valueOf(111), sums.get(1));
        assertEquals(Integer.valueOf(222), sums.get(2));
    }

    @Test
    public void testFilterMapCombination() {
        String[] arr1 = { "a", "bb", "ccc", "dddd", "eeeee" };
        Integer[] arr2 = { 1, 2, 3, 4, 5 };
        Double[] arr3 = { 1.0, 2.0, 3.0, 4.0, 5.0 };

        ObjIterator<Integer> iter = TriIterator.zip(arr1, arr2, arr3).filter((s, i, d) -> i % 2 == 0).map((s, i, d) -> s.length());

        List<Integer> lengths = new ArrayList<>();
        iter.forEachRemaining(lengths::add);

        assertEquals(2, lengths.size());
        assertEquals(Integer.valueOf(2), lengths.get(0));
        assertEquals(Integer.valueOf(4), lengths.get(1));
    }

    @Test
    public void testGenerateWithNullHasNext() {
        assertThrows(IllegalArgumentException.class, () -> TriIterator.generate(null, triple -> {
        }));
    }

    @Test
    public void testGenerateWithNullOutput() {
        assertThrows(IllegalArgumentException.class, () -> TriIterator.generate(() -> true, null));
    }

    @Test
    public void testFilterWithNullPredicate() {
        assertThrows(IllegalArgumentException.class, () -> TriIterator.empty().filter(null));
    }

    @Test
    public void testMapWithNullMapper() {

        TriIterator.empty().stream(null).forEach(s -> Assertions.fail("Should not be called"));
    }

    @Test
    public void testForEachRemainingWithNullAction() {

        TriIterator.empty().forEachRemaining(s -> Assertions.fail("Should not be called"));
    }

    @Test
    public void testIteratorExhaustion() {
        String[] arr1 = { "a" };
        Integer[] arr2 = { 1 };
        Double[] arr3 = { 1.1 };

        TriIterator<String, Integer, Double> iter = TriIterator.zip(arr1, arr2, arr3);

        iter.next();

        assertFalse(iter.hasNext());

        try {
            iter.next();
            fail("Should throw NoSuchElementException");
        } catch (NoSuchElementException e) {
        }

        AtomicInteger count = new AtomicInteger(0);
        iter.forEachRemaining((a, b, c) -> count.incrementAndGet());
        assertEquals(0, count.get());
    }
}

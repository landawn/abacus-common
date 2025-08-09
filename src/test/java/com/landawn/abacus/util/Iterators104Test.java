package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.function.TriConsumer;
import com.landawn.abacus.util.function.TriFunction;

public class Iterators104Test extends TestBase {

    private List<Integer> testList;
    private Iterator<Integer> testIterator;

    @BeforeEach
    public void setUp() {
        testList = Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
        testIterator = testList.iterator();
    }

    // Test forEach(Iterator, offset, count, elementConsumer, onComplete)
    @Test
    public void testForEachIteratorWithOffsetCountConsumerAndOnComplete() throws Exception {
        List<Integer> result = new ArrayList<>();
        AtomicBoolean onCompleteCalled = new AtomicBoolean(false);

        Iterators.forEach(testIterator, 2L, 5L, (Throwables.Consumer<Integer, Exception>) result::add,
                (Throwables.Runnable<Exception>) () -> onCompleteCalled.set(true));

        Assertions.assertEquals(Arrays.asList(3, 4, 5, 6, 7), result);
        Assertions.assertTrue(onCompleteCalled.get());
    }

    @Test
    public void testForEachIteratorWithZeroOffset() throws Exception {
        List<Integer> result = new ArrayList<>();

        Iterators.forEach(testIterator, 0L, 3L, (Throwables.Consumer<Integer, Exception>) result::add, (Throwables.Runnable<Exception>) () -> {
        });

        Assertions.assertEquals(Arrays.asList(1, 2, 3), result);
    }

    @Test
    public void testForEachIteratorWithOffsetExceedingSize() throws Exception {
        List<Integer> result = new ArrayList<>();

        Iterators.forEach(testIterator, 15L, 5L, (Throwables.Consumer<Integer, Exception>) result::add, (Throwables.Runnable<Exception>) () -> {
        });

        Assertions.assertTrue(result.isEmpty());
    }

    // Test forEach(Iterator, offset, count, processThreadNum, queueSize, elementConsumer)
    @Test
    public void testForEachIteratorWithThreadsNoOnComplete() throws Exception {
        List<Integer> synchronizedResult = Collections.synchronizedList(new ArrayList<>());

        Iterators.forEach(testIterator, 1L, 4L, 2, 10, (Throwables.Consumer<Integer, Exception>) synchronizedResult::add);

        Collections.sort(synchronizedResult);
        Assertions.assertEquals(Arrays.asList(2, 3, 4, 5), synchronizedResult);
    }

    // Test forEach(Iterator, offset, count, processThreadNum, queueSize, elementConsumer, onComplete)
    @Test
    public void testForEachIteratorWithThreadsAndOnComplete() throws Exception {
        List<Integer> synchronizedResult = Collections.synchronizedList(new ArrayList<>());
        AtomicBoolean onCompleteCalled = new AtomicBoolean(false);

        Iterators.forEach(testIterator, 0L, 5L, 3, 20, (Throwables.Consumer<Integer, Exception>) synchronizedResult::add,
                (Throwables.Runnable<Exception>) () -> onCompleteCalled.set(true));

        Collections.sort(synchronizedResult);
        Assertions.assertEquals(Arrays.asList(1, 2, 3, 4, 5), synchronizedResult);
        Assertions.assertTrue(onCompleteCalled.get());
    }

    @Test
    public void testForEachIteratorWithZeroThreads() throws Exception {
        List<Integer> result = new ArrayList<>();

        Iterators.forEach(testIterator, 0L, 3L, 0, 0, (Throwables.Consumer<Integer, Exception>) result::add, (Throwables.Runnable<Exception>) () -> {
        });

        Assertions.assertEquals(Arrays.asList(1, 2, 3), result);
    }

    // Test forEach(Collection<Iterator>, elementConsumer)
    @Test
    public void testForEachCollectionBasic() throws Exception {
        List<Iterator<Integer>> iterators = Arrays.asList(Arrays.asList(1, 2, 3).iterator(), Arrays.asList(4, 5, 6).iterator(),
                Arrays.asList(7, 8, 9).iterator());

        List<Integer> result = new ArrayList<>();
        Iterators.forEach(iterators, (Throwables.Consumer<Integer, Exception>) result::add);

        Assertions.assertEquals(Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9), result);
    }

    @Test
    public void testForEachEmptyCollection() throws Exception {
        List<Iterator<Integer>> emptyIterators = new ArrayList<>();
        List<Integer> result = new ArrayList<>();

        Iterators.forEach(emptyIterators, (Throwables.Consumer<Integer, Exception>) result::add);

        Assertions.assertTrue(result.isEmpty());
    }

    // Test forEach(Collection<Iterator>, elementConsumer, onComplete)
    @Test
    public void testForEachCollectionWithOnComplete() throws Exception {
        List<Iterator<Integer>> iterators = Arrays.asList(Arrays.asList(1, 2).iterator(), Arrays.asList(3, 4).iterator());

        List<Integer> result = new ArrayList<>();
        AtomicBoolean onCompleteCalled = new AtomicBoolean(false);

        Iterators.forEach(iterators, (Throwables.Consumer<Integer, Exception>) result::add, (Throwables.Runnable<Exception>) () -> onCompleteCalled.set(true));

        Assertions.assertEquals(Arrays.asList(1, 2, 3, 4), result);
        Assertions.assertTrue(onCompleteCalled.get());
    }

    // Test forEach(Collection<Iterator>, offset, count, elementConsumer)
    @Test
    public void testForEachCollectionWithOffsetCount() throws Exception {
        List<Iterator<Integer>> iterators = Arrays.asList(Arrays.asList(1, 2, 3, 4).iterator(), Arrays.asList(5, 6, 7, 8).iterator());

        List<Integer> result = new ArrayList<>();
        Iterators.forEach(iterators, 2L, 4L, (Throwables.Consumer<Integer, Exception>) result::add);

        Assertions.assertEquals(Arrays.asList(3, 4, 5, 6), result);
    }

    // Test forEach(Collection<Iterator>, offset, count, elementConsumer, onComplete)
    @Test
    public void testForEachCollectionWithOffsetCountAndOnComplete() throws Exception {
        List<Iterator<Integer>> iterators = Arrays.asList(Arrays.asList(10, 20, 30).iterator(), Arrays.asList(40, 50, 60).iterator());

        List<Integer> result = new ArrayList<>();
        AtomicBoolean onCompleteCalled = new AtomicBoolean(false);

        Iterators.forEach(iterators, 1L, 3L, (Throwables.Consumer<Integer, Exception>) result::add,
                (Throwables.Runnable<Exception>) () -> onCompleteCalled.set(true));

        Assertions.assertEquals(Arrays.asList(20, 30, 40), result);
        Assertions.assertTrue(onCompleteCalled.get());
    }

    // Test forEach(Collection<Iterator>, readThreadNum, processThreadNum, queueSize, elementConsumer)
    @Test
    public void testForEachCollectionWithThreadsNoOnComplete() throws Exception {
        List<Iterator<Integer>> iterators = Arrays.asList(Arrays.asList(1, 2, 3).iterator(), Arrays.asList(4, 5, 6).iterator());

        List<Integer> synchronizedResult = Collections.synchronizedList(new ArrayList<>());
        Iterators.forEach(iterators, 2, 2, 10, (Throwables.Consumer<Integer, Exception>) synchronizedResult::add);

        Collections.sort(synchronizedResult);
        Assertions.assertEquals(Arrays.asList(1, 2, 3, 4, 5, 6), synchronizedResult);
    }

    // Test forEach(Collection<Iterator>, readThreadNum, processThreadNum, queueSize, elementConsumer, onComplete)
    @Test
    public void testForEachCollectionWithThreadsAndOnComplete() throws Exception {
        List<Iterator<Integer>> iterators = Arrays.asList(Arrays.asList(10, 11).iterator(), Arrays.asList(12, 13).iterator());

        List<Integer> synchronizedResult = Collections.synchronizedList(new ArrayList<>());
        AtomicBoolean onCompleteCalled = new AtomicBoolean(false);

        Iterators.forEach(iterators, 1, 2, 5, (Throwables.Consumer<Integer, Exception>) synchronizedResult::add,
                (Throwables.Runnable<Exception>) () -> onCompleteCalled.set(true));

        Collections.sort(synchronizedResult);
        Assertions.assertEquals(Arrays.asList(10, 11, 12, 13), synchronizedResult);
        Assertions.assertTrue(onCompleteCalled.get());
    }

    // Test forEach(Collection<Iterator>, offset, count, readThreadNum, processThreadNum, queueSize, elementConsumer)
    @Test
    public void testForEachCollectionFullParamsNoOnComplete() throws Exception {
        List<Iterator<Integer>> iterators = Arrays.asList(Arrays.asList(1, 2, 3, 4, 5).iterator(), Arrays.asList(6, 7, 8, 9, 10).iterator());

        List<Integer> synchronizedResult = Collections.synchronizedList(new ArrayList<>());
        Iterators.forEach(iterators, 2L, 5L, 1, 3, 10, (Throwables.Consumer<Integer, Exception>) synchronizedResult::add);

        Collections.sort(synchronizedResult);
        Assertions.assertEquals(Arrays.asList(3, 4, 5, 6, 7), synchronizedResult);
    }

    // Test forEach(Collection<Iterator>, offset, count, readThreadNum, processThreadNum, queueSize, elementConsumer, onComplete)
    @Test
    public void testForEachCollectionFullParams() throws Exception {
        List<Iterator<Integer>> iterators = Arrays.asList(Arrays.asList(100, 101, 102).iterator(), Arrays.asList(103, 104, 105).iterator());

        List<Integer> synchronizedResult = Collections.synchronizedList(new ArrayList<>());
        AtomicBoolean onCompleteCalled = new AtomicBoolean(false);

        Iterators.forEach(iterators, 1L, 4L, 1, 2, 10, (Throwables.Consumer<Integer, Exception>) synchronizedResult::add,
                (Throwables.Runnable<Exception>) () -> onCompleteCalled.set(true));

        Collections.sort(synchronizedResult);
        Assertions.assertEquals(Arrays.asList(101, 102, 103, 104), synchronizedResult);
        Assertions.assertTrue(onCompleteCalled.get());
    }

    // Test edge cases and exceptions
    @Test
    public void testNegativeOffset() {
        List<Iterator<Integer>> iterators = Arrays.asList(testIterator);

        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            Iterators.forEach(iterators, -1L, 5L, 0, 0, 0, (Throwables.Consumer<Integer, Exception>) e -> {
            }, (Throwables.Runnable<Exception>) () -> {
            });
        });
    }

    @Test
    public void testNegativeCount() {
        List<Iterator<Integer>> iterators = Arrays.asList(testIterator);

        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            Iterators.forEach(iterators, 0L, -1L, 0, 0, 0, (Throwables.Consumer<Integer, Exception>) e -> {
            }, (Throwables.Runnable<Exception>) () -> {
            });
        });
    }

    @Test
    public void testConsumerException() {
        List<Iterator<Integer>> iterators = Arrays.asList(Arrays.asList(1, 2, 3).iterator());

        Assertions.assertThrows(RuntimeException.class, () -> {
            Iterators.forEach(iterators, (Throwables.Consumer<Integer, Exception>) e -> {
                throw new RuntimeException("Test exception");
            });
        });
    }

    @Test
    public void testOnCompleteException() {
        List<Iterator<Integer>> iterators = Arrays.asList(Arrays.asList(1).iterator());

        Assertions.assertThrows(RuntimeException.class, () -> {
            Iterators.forEach(iterators, (Throwables.Consumer<Integer, Exception>) e -> {
            }, (Throwables.Runnable<Exception>) () -> {
                throw new RuntimeException("OnComplete exception");
            });
        });
    }

    @Test
    public void testMultiThreadedExceptionHandling() {
        Collection<Iterator<Integer>> iterators = Arrays.asList(Arrays.asList(1, 2, 3, 4, 5).iterator());

        AtomicInteger counter = new AtomicInteger(0);

        Assertions.assertThrows(RuntimeException.class, () -> {
            Iterators.forEach(iterators, 0L, 5L, 1, 2, 10, e -> {
                if (counter.incrementAndGet() == 3) {
                    throw new RuntimeException("Multi-threaded exception");
                }
            }, () -> {
            });
        });
    }

    @Test
    public void testLargeCountValue() throws Exception {
        List<Iterator<Integer>> iterators = Arrays.asList(Arrays.asList(1, 2, 3).iterator());

        List<Integer> result = new ArrayList<>();
        Iterators.forEach(iterators, 0L, Long.MAX_VALUE, (Throwables.Consumer<Integer, Exception>) result::add);

        Assertions.assertEquals(Arrays.asList(1, 2, 3), result);
    }

    @Test
    public void testZeroCountValue() throws Exception {
        List<Iterator<Integer>> iterators = Arrays.asList(Arrays.asList(1, 2, 3).iterator());

        List<Integer> result = new ArrayList<>();
        Iterators.forEach(iterators, 0L, 0L, (Throwables.Consumer<Integer, Exception>) result::add);

        Assertions.assertTrue(result.isEmpty());
    }

    @Test
    public void testEmptyIteratorInCollection() throws Exception {
        List<Iterator<Integer>> iterators = Arrays.asList(Collections.<Integer> emptyIterator(), Arrays.asList(1, 2).iterator(),
                Collections.<Integer> emptyIterator());

        List<Integer> result = new ArrayList<>();
        Iterators.forEach(iterators, (Throwables.Consumer<Integer, Exception>) result::add);

        Assertions.assertEquals(Arrays.asList(1, 2), result);
    }

    @Test
    public void testConcatBiIterator_EmptyArray() {
        BiIterator<String, Integer> result = Iterators.concat(new BiIterator[0]);
        assertFalse(result.hasNext());
        assertThrows(NoSuchElementException.class, () -> result.next());
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
    public void testConcatBiIterator_HasNextMultipleCalls() {
        List<Pair<String, Integer>> pairs = new ArrayList<>();
        pairs.add(Pair.of("a", 1));

        BiIterator<String, Integer> iter = createBiIterator(pairs);
        BiIterator<String, Integer> result = Iterators.concat(iter);

        // Multiple hasNext calls should not consume elements
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

        // Multiple hasNext calls should not consume elements
        assertTrue(result.hasNext());
        assertTrue(result.hasNext());
        assertTrue(result.hasNext());

        assertEquals(Triple.of("a", 1, 1.1), result.next());
        assertFalse(result.hasNext());
        assertFalse(result.hasNext());
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
}
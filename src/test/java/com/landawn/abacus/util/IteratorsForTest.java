package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.util.function.BiFunction;
import com.landawn.abacus.util.function.TriFunction;
import com.landawn.abacus.util.stream.IntStream;
import com.landawn.abacus.util.stream.Stream;

public class IteratorsForTest extends IteratorsTestSupport {
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
        Iterators.forEach(Arrays.asList(1, 2, 3, 4, 5).iterator(), Iterators.IterateOptions.builder().offset(0).count(5).processThreads(1).queueSize(1).build(),
                sum::addAndGet);
        assertEquals(15, sum.get());
    }

    @Test
    public void testForEachIteratorWithThreadsAndOnComplete() {
        AtomicInteger sum = new AtomicInteger(0);
        AtomicInteger completeCalled = new AtomicInteger(0);

        Iterators.forEach(Arrays.asList(1, 2, 3).iterator(), Iterators.IterateOptions.builder().offset(0).count(3).processThreads(1).queueSize(1).build(),
                sum::addAndGet, () -> completeCalled.incrementAndGet());

        assertEquals(6, sum.get());
        assertEquals(1, completeCalled.get());
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

        Iterators.forEach(iterators, Iterators.IterateOptions.builder().readThreads(1).processThreads(1).queueSize(1).build(), sum::addAndGet);
        assertEquals(10, sum.get());
    }

    @Test
    public void testForEachCollectionWithThreadsAndOnComplete() {
        AtomicInteger sum = new AtomicInteger(0);
        AtomicInteger completeCalled = new AtomicInteger(0);

        Collection<Iterator<Integer>> iterators = Arrays.asList(Arrays.asList(1, 2).iterator(), Arrays.asList(3).iterator());

        Iterators.forEach(iterators, Iterators.IterateOptions.builder().readThreads(1).processThreads(1).queueSize(1).build(), sum::addAndGet,
                () -> completeCalled.incrementAndGet());

        assertEquals(6, sum.get());
        assertEquals(1, completeCalled.get());
    }

    @Test
    public void testForEachIteratorWithOffsetCountThreadsAndQueueSize() {
        AtomicInteger sum = new AtomicInteger(0);

        Iterators.forEach(Arrays.asList(1, 2, 3, 4, 5).iterator(), Iterators.IterateOptions.builder().offset(1).count(3).processThreads(1).queueSize(1).build(),
                sum::addAndGet);

        assertEquals(9, sum.get());
    }

    @Test
    public void testForEachIteratorWithOffsetCountThreadsQueueSizeAndOnComplete() {
        AtomicInteger sum = new AtomicInteger(0);
        AtomicInteger completeCalled = new AtomicInteger(0);

        Iterators.forEach(Arrays.asList(1, 2, 3, 4, 5).iterator(), Iterators.IterateOptions.builder().offset(1).count(3).processThreads(1).queueSize(1).build(),
                sum::addAndGet, () -> completeCalled.incrementAndGet());

        assertEquals(9, sum.get());
        assertEquals(1, completeCalled.get());
    }

    @Test
    public void testForEachCollectionWithOffsetCountReadThreadsProcessThreadsAndQueueSize() {
        AtomicLong sum = new AtomicLong(0);

        Collection<Iterator<Integer>> iterators = Arrays.asList(Arrays.asList(1, 2, 3).iterator(), Arrays.asList(4, 5).iterator());

        Iterators.forEach(iterators, Iterators.IterateOptions.builder().offset(1).count(3).readThreads(1).processThreads(1).queueSize(1).build(),
                n -> sum.addAndGet(n));
        assertEquals(9, sum.get());
    }

    @Test
    public void testForEachCollectionWithOffsetCountReadThreadsProcessThreadsQueueSizeAndOnComplete() {
        AtomicLong sum = new AtomicLong(0);
        AtomicInteger completeCalled = new AtomicInteger(0);

        Collection<Iterator<Integer>> iterators = Arrays.asList(Arrays.asList(1, 2).iterator(), Arrays.asList(3, 4).iterator());

        Iterators.forEach(iterators, Iterators.IterateOptions.builder().offset(0).count(3).readThreads(1).processThreads(1).queueSize(1).build(),
                n -> sum.addAndGet(n), () -> completeCalled.incrementAndGet());

        assertEquals(6, sum.get());
        assertEquals(1, completeCalled.get());
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
    public void testForEachPairAndTriple_sequential() {
        assertEquals(List.of("1-2", "2-3", "3-4", "4-5", "5-6", "6-7", "7-8", "8-9"), collectPairs(1));
        assertEquals(collectPairs(1), collectPairsDefault());
        assertEquals(List.of("1-2", "3-4", "5-6", "7-8", "9-null"), collectPairs(2));
        assertEquals(List.of("1-2", "4-5", "7-8"), collectPairs(3));

        assertEquals(List.of("1-2-3", "2-3-4", "3-4-5", "4-5-6", "5-6-7", "6-7-8", "7-8-9"), collectTriples(1));
        assertEquals(List.of("1-2-3", "3-4-5", "5-6-7", "7-8-9"), collectTriples(2));
        assertEquals(List.of("1-2-3", "4-5-6", "7-8-9"), collectTriples(3));
        assertEquals(List.of("1-2-3", "5-6-7", "9-null-null"), collectTriples(4));
    }

    @Test
    public void testForEachPairAndTriple_collectionAndArray() {
        final List<String> pairs = new ArrayList<>();
        Stream.of(IntList.range(1, 10).toList()).forEachPair(2, (a, b) -> pairs.add(a + "-" + b));
        assertEquals(List.of("1-2", "3-4", "5-6", "7-8", "9-null"), pairs);

        final List<String> triples = new ArrayList<>();
        Stream.of(IntList.range(1, 10).toArray()).forEachTriple(4, (a, b, c) -> triples.add(a + "-" + b + "-" + c));
        assertEquals(List.of("1-2-3", "5-6-7", "9-null-null"), triples);
    }

    @Test
    public void testForEachPairAndTriple_parallel() {
        assertEquals(new HashSet<>(collectPairs(1)), new HashSet<>(collectPairsParallel(1)));
        assertEquals(new HashSet<>(collectPairs(2)), new HashSet<>(collectPairsParallel(2)));
        assertEquals(new HashSet<>(collectTriples(4)), new HashSet<>(collectTriplesParallel(4)));
    }

    @Test
    public void testSlidingMap_streamFromIterator() {
        final BiFunction<Integer, Integer, String> pairs = (a, b) -> a + "-" + b;
        final TriFunction<Integer, Integer, Integer, String> triples = (a, b, c) -> a + "-" + b + "-" + c;

        assertEquals(List.of("1-2", "2-3", "3-4", "4-5", "5-6", "6-7", "7-8", "8-9"), slidingPairs(false, 1, false, pairs));
        assertEquals(List.of("1-2", "3-4", "5-6", "7-8", "9-null"), slidingPairs(false, 2, false, pairs));
        assertEquals(List.of("1-2", "3-4", "5-6", "7-8"), slidingPairs(false, 2, true, pairs));
        assertEquals(List.of("1-2-3", "2-3-4", "3-4-5", "4-5-6", "5-6-7", "6-7-8", "7-8-9"), slidingTriples(false, 1, false, triples));
        assertEquals(List.of("1-2-3", "5-6-7", "9-null-null"), slidingTriples(false, 4, false, triples));
        assertEquals(List.of("1-2-3", "5-6-7"), slidingTriples(false, 4, true, triples));
    }

    @Test
    public void testSlidingMap_streamFromCollection() {
        final BiFunction<Integer, Integer, String> pairs = (a, b) -> a + "-" + b;
        assertEquals(List.of("1-2", "3-4", "5-6", "7-8", "9-null"), slidingPairs(true, 2, false, pairs));
        assertEquals(List.of("1-2", "3-4", "5-6", "7-8"), slidingPairs(true, 2, true, pairs));
    }

    @Test
    public void testSlidingMap_seqFromIterator() {
        final BiFunction<Integer, Integer, String> pairs = (a, b) -> a + "-" + b;
        final TriFunction<Integer, Integer, Integer, String> triples = (a, b, c) -> a + "-" + b + "-" + c;

        assertEquals(List.of("1-2", "2-3", "3-4", "4-5", "5-6", "6-7", "7-8", "8-9"),
                Seq.<Integer, RuntimeException> of(IntList.range(1, 10).toList().iterator()).slidingMap(1, pairs).toList());
        assertEquals(List.of("1-2", "3-4", "5-6", "7-8"),
                Seq.<Integer, RuntimeException> of(IntList.range(1, 10).toList().iterator()).slidingMap(2, true, pairs).toList());
        assertEquals(List.of("1-2-3", "5-6-7", "9-null-null"),
                Seq.<Integer, RuntimeException> of(IntList.range(1, 10).toList().iterator()).slidingMap(4, triples).toList());
        assertEquals(List.of("1-2-3", "5-6-7"),
                Seq.<Integer, RuntimeException> of(IntList.range(1, 10).toList().iterator()).slidingMap(4, true, triples).toList());
    }

    @Test
    public void testForEachIteratorConsumer() throws Exception {
        List<String> result = new ArrayList<>();
        Throwables.Consumer<String, Exception> consumer = result::add;
        Iterators.forEach(list("a", "b").iterator(), consumer);
        assertEquals(list("a", "b"), result);
    }

    @Test
    public void testForEachIteratorConsumerOnComplete() throws Exception {
        List<String> result = new ArrayList<>();
        AtomicBoolean completed = new AtomicBoolean(false);
        Throwables.Consumer<String, Exception> consumer = result::add;
        Throwables.Runnable<Exception> onComplete = () -> completed.set(true);

        Iterators.forEach(list("a", "b").iterator(), consumer, onComplete);
        assertEquals(list("a", "b"), result);
        assertTrue(completed.get());
    }

    @Test
    public void testForEachIteratorOffsetCount() throws Exception {
        List<String> result = new ArrayList<>();
        Throwables.Consumer<String, Exception> consumer = result::add;
        Iterators.forEach(list("a", "b", "c", "d").iterator(), 1, 2, consumer);
        assertEquals(list("b", "c"), result);
    }

    @Test
    public void testForEachIteratorParallelSequentialPath() throws Exception {
        List<String> result = new ArrayList<>();
        AtomicBoolean completed = new AtomicBoolean(false);
        Throwables.Consumer<String, Exception> consumer = result::add;
        Throwables.Runnable<Exception> onComplete = () -> completed.set(true);

        Iterators.forEach(list("x", "y", "z").iterator(), Iterators.IterateOptions.builder().offset(0).count(3).processThreads(0).queueSize(0).build(),
                consumer, onComplete);
        assertEquals(list("x", "y", "z"), result);
        assertTrue(completed.get());
    }

    @Test
    public void testForEachCollectionOfIteratorsSequentialPath() throws Exception {
        Collection<Iterator<String>> iterators = list(list("a", "b").iterator(), list("c").iterator());
        List<String> result = new ArrayList<>();
        AtomicBoolean completed = new AtomicBoolean(false);
        Throwables.Consumer<String, Exception> consumer = result::add;
        Throwables.Runnable<Exception> onComplete = () -> completed.set(true);

        Iterators.forEach(iterators, Iterators.IterateOptions.builder().offset(0).count(Long.MAX_VALUE).readThreads(0).processThreads(0).queueSize(0).build(),
                consumer, onComplete);
        assertEquals(list("a", "b", "c"), result);
        assertTrue(completed.get());
    }

    @Disabled("Parallel forEach tests require more setup or a test environment that can handle threads properly.")
    @Test
    public void testForEachIteratorParallelPath() throws Exception {
        List<String> result = Collections.synchronizedList(new ArrayList<>());
        AtomicBoolean completed = new AtomicBoolean(false);
        Throwables.Consumer<String, Exception> consumer = result::add;
        Throwables.Runnable<Exception> onComplete = () -> completed.set(true);

        Iterators.forEach(list("a", "b", "c").iterator(), Iterators.IterateOptions.builder().offset(0).count(3).processThreads(2).queueSize(2).build(),
                consumer, onComplete);
        assertEquals(list("a", "b", "c"), result);
        assertTrue(completed.get());
    }

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

    @Test
    public void testForEachIteratorWithThreadsNoOnComplete() throws Exception {
        List<Integer> synchronizedResult = Collections.synchronizedList(new ArrayList<>());

        Iterators.forEach(testIterator, Iterators.IterateOptions.builder().offset(1L).count(4L).processThreads(2).queueSize(10).build(),
                (Throwables.Consumer<Integer, Exception>) synchronizedResult::add);

        Collections.sort(synchronizedResult);
        Assertions.assertEquals(Arrays.asList(2, 3, 4, 5), synchronizedResult);
    }

    @Test
    public void testForEachIteratorWithZeroThreads() throws Exception {
        List<Integer> result = new ArrayList<>();

        Iterators.forEach(testIterator, Iterators.IterateOptions.builder().offset(0L).count(3L).processThreads(0).queueSize(0).build(),
                (Throwables.Consumer<Integer, Exception>) result::add, (Throwables.Runnable<Exception>) () -> {
                });

        Assertions.assertEquals(Arrays.asList(1, 2, 3), result);
    }

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

    @Test
    public void testForEachCollectionWithOnComplete() throws Exception {
        List<Iterator<Integer>> iterators = Arrays.asList(Arrays.asList(1, 2).iterator(), Arrays.asList(3, 4).iterator());

        List<Integer> result = new ArrayList<>();
        AtomicBoolean onCompleteCalled = new AtomicBoolean(false);

        Iterators.forEach(iterators, (Throwables.Consumer<Integer, Exception>) result::add, (Throwables.Runnable<Exception>) () -> onCompleteCalled.set(true));

        Assertions.assertEquals(Arrays.asList(1, 2, 3, 4), result);
        Assertions.assertTrue(onCompleteCalled.get());
    }

    @Test
    public void testForEachCollectionWithOffsetCount() throws Exception {
        List<Iterator<Integer>> iterators = Arrays.asList(Arrays.asList(1, 2, 3, 4).iterator(), Arrays.asList(5, 6, 7, 8).iterator());

        List<Integer> result = new ArrayList<>();
        Iterators.forEach(iterators, 2L, 4L, (Throwables.Consumer<Integer, Exception>) result::add);

        Assertions.assertEquals(Arrays.asList(3, 4, 5, 6), result);
    }

    @Test
    public void testForEachCollectionWithThreadsNoOnComplete() throws Exception {
        List<Iterator<Integer>> iterators = Arrays.asList(Arrays.asList(1, 2, 3).iterator(), Arrays.asList(4, 5, 6).iterator());

        List<Integer> synchronizedResult = Collections.synchronizedList(new ArrayList<>());
        Iterators.forEach(iterators, Iterators.IterateOptions.builder().readThreads(2).processThreads(2).queueSize(10).build(),
                (Throwables.Consumer<Integer, Exception>) synchronizedResult::add);

        Collections.sort(synchronizedResult);
        Assertions.assertEquals(Arrays.asList(1, 2, 3, 4, 5, 6), synchronizedResult);
    }

    @Test
    public void testForEachCollectionFullParamsNoOnComplete() throws Exception {
        List<Iterator<Integer>> iterators = Arrays.asList(Arrays.asList(1, 2, 3, 4, 5).iterator(), Arrays.asList(6, 7, 8, 9, 10).iterator());

        List<Integer> synchronizedResult = Collections.synchronizedList(new ArrayList<>());
        Iterators.forEach(iterators, Iterators.IterateOptions.builder().offset(2L).count(5L).readThreads(1).processThreads(3).queueSize(10).build(),
                (Throwables.Consumer<Integer, Exception>) synchronizedResult::add);

        Collections.sort(synchronizedResult);
        Assertions.assertEquals(Arrays.asList(3, 4, 5, 6, 7), synchronizedResult);
    }

    @Test
    public void testForEachCollectionFullParams() throws Exception {
        List<Iterator<Integer>> iterators = Arrays.asList(Arrays.asList(100, 101, 102).iterator(), Arrays.asList(103, 104, 105).iterator());

        List<Integer> synchronizedResult = Collections.synchronizedList(new ArrayList<>());
        AtomicBoolean onCompleteCalled = new AtomicBoolean(false);

        Iterators.forEach(iterators, Iterators.IterateOptions.builder().offset(1L).count(4L).readThreads(1).processThreads(2).queueSize(10).build(),
                (Throwables.Consumer<Integer, Exception>) synchronizedResult::add, (Throwables.Runnable<Exception>) () -> onCompleteCalled.set(true));

        Collections.sort(synchronizedResult);
        Assertions.assertEquals(Arrays.asList(101, 102, 103, 104), synchronizedResult);
        Assertions.assertTrue(onCompleteCalled.get());
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

    @Test
    public void testForEachBasic() {
        List<Integer> processed = new ArrayList<>();
        Throwables.Consumer<Integer, RuntimeException> consumer = processed::add;

        Iterators.forEach(testList.iterator(), consumer);

        assertEquals(testList, processed);
    }

    @Test
    public void testForEachWithOnComplete() {
        List<Integer> processed = new ArrayList<>();
        AtomicInteger completeCalled = new AtomicInteger(0);

        Throwables.Consumer<Integer, RuntimeException> consumer = processed::add;
        Throwables.Runnable<RuntimeException> onComplete = () -> completeCalled.incrementAndGet();

        Iterators.forEach(testList.iterator(), consumer, onComplete);

        assertEquals(testList, processed);
        assertEquals(1, completeCalled.get());
    }

    @Test
    public void testForEachWithOffsetAndCount() {
        List<Integer> processed = new ArrayList<>();
        Throwables.Consumer<Integer, RuntimeException> consumer = processed::add;

        Iterators.forEach(testList.iterator(), 1, 3, consumer);

        assertEquals(Arrays.asList(2, 3, 4), processed);
    }

    @Test
    public void testForEachWithThreads() {
        List<Integer> processed = Collections.synchronizedList(new ArrayList<>());
        Throwables.Consumer<Integer, RuntimeException> consumer = processed::add;

        Iterators.forEach(testList.iterator(), Iterators.IterateOptions.builder().offset(0).count(Long.MAX_VALUE).processThreads(2).queueSize(4).build(),
                consumer);

        processed.sort(Integer::compareTo);
        assertEquals(testList, processed);
    }

    @Test
    public void testForEach() throws Exception {
        List<Integer> collected = new ArrayList<>();
        Iterators.forEach(intList.iterator(), collected::add);
        assertEquals(intList, collected);

        collected.clear();
        Iterators.forEach(intList.iterator(), 1, 3, collected::add);
        assertEquals(Arrays.asList(2, 3, 4), collected);

        AtomicInteger completeCount = new AtomicInteger(0);
        collected.clear();
        Iterators.forEach(intList.iterator(), collected::add, completeCount::incrementAndGet);
        assertEquals(intList, collected);
        assertEquals(1, completeCount.get());
    }

    @Test
    public void testForEachMultipleIterators() throws Exception {
        List<Iterator<Integer>> iterators = Arrays.asList(Arrays.asList(1, 2).iterator(), Arrays.asList(3, 4).iterator(), Arrays.asList(5, 6).iterator());

        List<Integer> collected = new ArrayList<>();
        Iterators.forEach(iterators, collected::add);
        assertEquals(Arrays.asList(1, 2, 3, 4, 5, 6), collected);

        iterators = Arrays.asList(Arrays.asList(1, 2, 3).iterator(), Arrays.asList(4, 5, 6).iterator());
        collected.clear();
        Iterators.forEach(iterators, 2, 3, collected::add);
        assertEquals(Arrays.asList(3, 4, 5), collected);
    }

    // ===================== forEach with offset/count/threads edge cases =====================

    @Test
    public void testForEach_EmptyIterator() throws Exception {
        AtomicInteger count = new AtomicInteger(0);
        Iterators.forEach(Collections.<Integer> emptyList().iterator(), v -> count.incrementAndGet());
        assertEquals(0, count.get());
    }

    @Test
    public void testForEach_WithOffset() throws Exception {
        List<Integer> result = new ArrayList<>();
        Iterators.forEach(Arrays.asList(1, 2, 3, 4, 5).iterator(), 2, 2, v -> result.add(v));
        assertEquals(Arrays.asList(3, 4), result);
    }

    @Test
    public void testForEach_ZeroCount() throws Exception {
        AtomicInteger count = new AtomicInteger(0);
        Iterators.forEach(Arrays.asList(1, 2, 3).iterator(), 0, 0, v -> count.incrementAndGet());
        assertEquals(0, count.get());
    }

    // ===================== forEach Additional Edge Cases =====================

    @Test
    public void testForEach_NullIterator() throws Exception {
        AtomicInteger count = new AtomicInteger(0);
        Iterators.forEach((Iterator<Integer>) null, v -> count.incrementAndGet());
        assertEquals(0, count.get());
    }

    @Test
    public void testForEach_NullIteratorWithOnComplete() throws Exception {
        AtomicInteger count = new AtomicInteger(0);
        AtomicBoolean completed = new AtomicBoolean(false);
        Iterators.forEach((Iterator<Integer>) null, v -> count.incrementAndGet(), () -> completed.set(true));
        assertEquals(0, count.get());
        assertTrue(completed.get());
    }

    @Test
    public void testForEach_NullIteratorStillValidatesNumericArguments() {
        final AtomicBoolean completed = new AtomicBoolean(false);

        // A negative setting can no longer reach forEach at all: IterateOptions rejects it at build() time.
        assertThrows(IllegalArgumentException.class, () -> Iterators.IterateOptions.builder().offset(-1).build());
        assertThrows(IllegalArgumentException.class, () -> Iterators.IterateOptions.builder().count(-1).build());
        assertThrows(IllegalArgumentException.class, () -> Iterators.IterateOptions.builder().processThreads(-1).build());
        assertThrows(IllegalArgumentException.class, () -> Iterators.IterateOptions.builder().queueSize(-1).build());

        // The positional overloads take raw numbers, so they must still validate before short-circuiting on a
        // null iterator - and must not run onComplete when they reject the arguments.
        assertThrows(IllegalArgumentException.class, () -> Iterators.forEach((Iterator<Integer>) null, -1L, 1L, value -> {
        }, () -> completed.set(true)));
        assertThrows(IllegalArgumentException.class, () -> Iterators.forEach((Iterator<Integer>) null, 0L, -1L, value -> {
        }, () -> completed.set(true)));
        assertThrows(IllegalArgumentException.class, () -> Iterators.forEach((Collection<Iterator<Integer>>) null, -1L, 1L, value -> {
        }, () -> completed.set(true)));

        assertFalse(completed.get());
    }

    @Test
    public void testForEach_OffsetAndCount_OffsetBeyondSize() throws Exception {
        List<Integer> result = new ArrayList<>();
        Iterators.forEach(Arrays.asList(1, 2, 3).iterator(), 10, 5, v -> result.add(v));
        assertTrue(result.isEmpty());
    }

    @Test
    public void testForEach_OffsetAndCount_CountExceedsRemaining() throws Exception {
        List<Integer> result = new ArrayList<>();
        Iterators.forEach(Arrays.asList(1, 2, 3, 4, 5).iterator(), 3, 10, v -> result.add(v));
        assertEquals(Arrays.asList(4, 5), result);
    }

    @Test
    public void testForEachCollectionOfIterators_EmptyCollection() throws Exception {
        List<Integer> result = new ArrayList<>();
        Iterators.forEach(Collections.<Iterator<Integer>> emptyList(), v -> result.add(v));
        assertTrue(result.isEmpty());
    }

    @Test
    public void testForEachCollectionOfIterators_NullCollection() throws Exception {
        AtomicInteger count = new AtomicInteger(0);
        Iterators.forEach((Collection<Iterator<Integer>>) null, v -> count.incrementAndGet());
        assertEquals(0, count.get());
    }

    @Test
    public void testForEachCollection_WithOffset() throws Exception {
        List<Iterator<? extends Integer>> iterators = Arrays.asList(Arrays.asList(1, 2, 3).iterator(), Arrays.asList(4, 5, 6).iterator());
        List<Integer> collected = new ArrayList<>();
        Iterators.forEach(iterators, 1, 4, collected::add);
        assertEquals(4, collected.size());
    }

    @Test
    public void testForEachCollection_WithThreads() throws Exception {
        List<Iterator<? extends Integer>> iterators = Arrays.asList(Arrays.asList(1, 2, 3, 4, 5).iterator());
        AtomicInteger count = new AtomicInteger(0);
        assertThrows(IllegalArgumentException.class,
                () -> Iterators.forEach(iterators, Iterators.IterateOptions.builder().build(), e -> count.incrementAndGet(), null));
        assertEquals(0, count.get());
    }

    @Test
    public void testForEachEmptyIteratorCollectionRunsOnComplete() throws Exception {
        // regression: an EMPTY collection of iterators returned before running onComplete, while a
        // collection of empty iterators (zero elements either way) did run it
        final java.util.concurrent.atomic.AtomicBoolean done = new java.util.concurrent.atomic.AtomicBoolean(false);
        Iterators.forEach(new java.util.ArrayList<java.util.Iterator<String>>(), s -> {
        }, () -> done.set(true));
        assertTrue(done.get());

        final java.util.concurrent.atomic.AtomicBoolean done2 = new java.util.concurrent.atomic.AtomicBoolean(false);
        Iterators.forEach(java.util.List.of(Collections.<String> emptyIterator()), s -> {
        }, () -> done2.set(true));
        assertTrue(done2.get());
    }

    @Test
    public void testForEachEmptyIteratorCollectionDoesNotEvaluateConsumer() {
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                () -> Iterators.forEach(Collections.<Iterator<Integer>> emptyList(), (Throwables.Consumer<Integer, RuntimeException>) null));
    }

    @Test
    public void testForEachWithIterateOptions_offsetCountSlicing() throws Exception {
        final List<Iterator<Integer>> iters = Arrays.asList(Arrays.asList(1, 2, 3).iterator(), Arrays.asList(4, 5, 6).iterator());
        final List<Integer> result = new ArrayList<>();
        Iterators.forEach(iters, Iterators.IterateOptions.builder().offset(2).count(3).build(), result::add);
        assertEquals(Arrays.asList(3, 4, 5), result);
    }

    @Test
    public void testForEachWithIterateOptions_runsOnComplete() throws Exception {
        final List<Iterator<Integer>> iters = Arrays.asList(Arrays.asList(1, 2).iterator(), Arrays.asList(3, 4).iterator());
        final List<Integer> result = new ArrayList<>();
        final AtomicBoolean done = new AtomicBoolean(false);
        Iterators.forEach(iters, Iterators.IterateOptions.builder().build(), result::add, () -> done.set(true));
        assertEquals(Arrays.asList(1, 2, 3, 4), result);
        assertTrue(done.get());
    }

    @Test
    public void testForEachWithIterateOptions_nullOptionsTreatedAsDefaults() throws Exception {
        final List<Iterator<Integer>> iters = Arrays.asList(Arrays.asList(1, 2, 3).iterator());
        final List<Integer> result = new ArrayList<>();
        Iterators.forEach(iters, (Iterators.IterateOptions) null, result::add);
        assertEquals(Arrays.asList(1, 2, 3), result);
    }

    @Test
    public void testForEachWithIterateOptions_parallelProcessesAllElements() throws Exception {
        final List<Integer> range1 = new ArrayList<>();
        final List<Integer> range2 = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            range1.add(i);
            range2.add(i + 100);
        }
        final List<Iterator<Integer>> iters = Arrays.asList(range1.iterator(), range2.iterator());
        final java.util.Set<Integer> processed = java.util.concurrent.ConcurrentHashMap.newKeySet();
        Iterators.forEach(iters, Iterators.IterateOptions.builder().readThreads(2).processThreads(4).queueSize(50).build(), processed::add);
        assertEquals(200, processed.size());
        assertTrue(processed.contains(0));
        assertTrue(processed.contains(199));
    }

    /** See {@link #testParallelForEachPropagatesCheckedExceptionUnwrapped()} - same rule, consumer-only overload. */
    @Test
    public void testForEachWithIterateOptions_parallelPropagatesCheckedExceptionUnwrapped() {
        final List<Iterator<Integer>> iters = Arrays.asList(Arrays.asList(1).iterator());

        final IOException thrown = assertThrows(IOException.class,
                () -> Iterators.forEach(iters, Iterators.IterateOptions.builder().processThreads(1).build(), (Throwables.Consumer<Integer, IOException>) e -> {
                    throw new IOException("checked");
                }));
        assertEquals("checked", thrown.getMessage());
    }

    @Test
    public void testForEachWithIterateOptions_negativeOffsetOrCountThrows() {
        final List<Iterator<Integer>> iters = Arrays.asList(Arrays.asList(1, 2, 3).iterator());
        assertThrows(IllegalArgumentException.class, () -> Iterators.forEach(iters, Iterators.IterateOptions.builder().offset(-1).build(), x -> {
        }));
        final List<Iterator<Integer>> iters2 = Arrays.asList(Arrays.asList(1, 2, 3).iterator());
        assertThrows(IllegalArgumentException.class, () -> Iterators.forEach(iters2, Iterators.IterateOptions.builder().count(-1).build(), x -> {
        }));
    }

    @Test
    public void testForEachIteratorWithIterateOptions_offsetCountSlicing() throws Exception {
        final Iterator<Integer> iter = Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8).iterator();
        final List<Integer> result = new ArrayList<>();
        Iterators.forEach(iter, Iterators.IterateOptions.builder().offset(2).count(3).build(), result::add);
        assertEquals(Arrays.asList(3, 4, 5), result);
    }

    @Test
    public void testForEachIteratorWithIterateOptions_runsOnComplete() throws Exception {
        final Iterator<Integer> iter = Arrays.asList(1, 2, 3).iterator();
        final List<Integer> result = new ArrayList<>();
        final AtomicBoolean done = new AtomicBoolean(false);
        Iterators.forEach(iter, Iterators.IterateOptions.builder().build(), result::add, () -> done.set(true));
        assertEquals(Arrays.asList(1, 2, 3), result);
        assertTrue(done.get());
    }

    @Test
    public void testForEachIteratorWithIterateOptions_nullOptionsTreatedAsDefaults() throws Exception {
        final Iterator<Integer> iter = Arrays.asList(1, 2, 3).iterator();
        final List<Integer> result = new ArrayList<>();
        Iterators.forEach(iter, (Iterators.IterateOptions) null, result::add);
        assertEquals(Arrays.asList(1, 2, 3), result);
    }

    @Test
    public void testForEachIteratorWithIterateOptions_parallelIgnoresReadThreadsAndProcessesAll() throws Exception {
        final List<Integer> range = new ArrayList<>();
        for (int i = 0; i < 200; i++) {
            range.add(i);
        }
        final java.util.Set<Integer> processed = java.util.concurrent.ConcurrentHashMap.newKeySet();
        // readThreads(2) is silently ignored for a single iterator; processing still uses 4 threads
        Iterators.forEach(range.iterator(), Iterators.IterateOptions.builder().readThreads(2).processThreads(4).queueSize(50).build(), processed::add);
        assertEquals(200, processed.size());
        assertTrue(processed.contains(0));
        assertTrue(processed.contains(199));
    }

    @Test
    public void testForEachSequentialReadDoesNotLogStreamCloseWarning() {
        // Regression: forEach used to build a Stream even for sequential reading and then take
        // stream.skip(..).limit(..).iterator(). Every derived stream carries a parent-link close handler, so
        // Stream.iterator() logged "Remember to close .. because it has close handlers" on every call - even
        // though forEach closes everything it opens. Sequential reading now concatenates and slices the
        // iterators directly, with no Stream involved.
        final StringBuilder captured = new StringBuilder();
        final java.util.logging.Logger root = java.util.logging.Logger.getLogger("");
        final java.util.logging.Handler probe = new java.util.logging.Handler() {
            @Override
            public void publish(final java.util.logging.LogRecord r) {
                captured.append(r.getMessage()).append(System.lineSeparator());
            }

            @Override
            public void flush() {
            }

            @Override
            public void close() {
            }
        };

        root.addHandler(probe);

        final List<Integer> processed = new ArrayList<>();

        try {
            Iterators.forEach(Arrays.asList(Arrays.asList(0, 1, 2).iterator(), Arrays.asList(3, 4, 5).iterator()), 1, 3, processed::add);
        } finally {
            root.removeHandler(probe);
        }

        assertEquals(Arrays.asList(1, 2, 3), processed, "slicing across the iterator boundary must be unchanged");
        assertFalse(captured.toString().contains("Remember to close"), "sequential forEach must not log a stream close-handler warning; captured: " + captured);
    }

    @Test
    public void testForEachSlicingIsIdenticalAcrossReadAndProcessModes() {
        final int[][] modes = { { 0, 0, 0 }, { 0, 2, 0 }, { 0, 4, 8 } };

        for (final int[] mode : modes) {
            final List<Integer> out = Collections.synchronizedList(new ArrayList<Integer>());
            final Iterators.IterateOptions options = Iterators.IterateOptions.builder()
                    .offset(4)
                    .count(3)
                    .readThreads(mode[0])
                    .processThreads(mode[1])
                    .queueSize(mode[2])
                    .build();

            Iterators.forEach(Arrays.asList(Arrays.asList(0, 1, 2, 3, 4).iterator(), Arrays.asList(5, 6, 7, 8, 9).iterator()), options, out::add,
                    Fn.emptyAction());

            final List<Integer> sorted = new ArrayList<>(out);
            Collections.sort(sorted);
            assertEquals(Arrays.asList(4, 5, 6), sorted, "readThreads=" + mode[0] + ", processThreads=" + mode[1] + ", queueSize=" + mode[2]);
        }
    }

    @Test
    public void testForEachParallelReadHonoursTheCountButNotTheSelection() {
        // What a concurrent read DOES guarantee: exactly `count` elements, all distinct, all drawn from the
        // input. Which ones is not stable, so nothing here pins their identity.
        final int[][] modes = { { 2, 0, 8 }, { 2, 2, 8 } };

        for (final int[] mode : modes) {
            for (int repeat = 0; repeat < 20; repeat++) {
                final List<Integer> out = Collections.synchronizedList(new ArrayList<Integer>());
                final Iterators.IterateOptions options = Iterators.IterateOptions.builder()
                        .offset(4)
                        .count(3)
                        .readThreads(mode[0])
                        .processThreads(mode[1])
                        .queueSize(mode[2])
                        .build();

                Iterators.forEach(Arrays.asList(Arrays.asList(0, 1, 2, 3, 4).iterator(), Arrays.asList(5, 6, 7, 8, 9).iterator()), options, out::add,
                        Fn.emptyAction());

                final String where = "readThreads=" + mode[0] + ", processThreads=" + mode[1] + ", repeat=" + repeat;
                assertEquals(3, out.size(), where);
                assertEquals(3, new HashSet<>(out).size(), "no element may be delivered twice: " + where);
                assertTrue(new HashSet<>(Arrays.asList(0, 1, 2, 3, 4, 5, 6, 7, 8, 9)).containsAll(out), "only input elements: " + where);
            }
        }
    }

    /** B1: a {@code null} iterator inside the collection must be skipped, whatever the reading mode is. */
    @Test
    public void testForEachCollection_nullIteratorMemberIsSkipped_inEveryReadingMode() throws Exception {
        for (final int readThreads : new int[] { 0, 1, 2 }) {
            final List<Iterator<Integer>> iters = Arrays.asList(null, Arrays.asList(1, 2).iterator(), null, Arrays.asList(3).iterator(), null);
            final List<Integer> out = Collections.synchronizedList(new ArrayList<>());
            final AtomicBoolean done = new AtomicBoolean(false);

            Iterators.forEach(iters, Iterators.IterateOptions.builder().readThreads(readThreads).build(), out::add, () -> done.set(true));

            assertEquals(new HashSet<>(Arrays.asList(1, 2, 3)), new HashSet<>(out), "readThreads=" + readThreads);
            assertTrue(done.get(), "readThreads=" + readThreads);
        }
    }

    /** B1: a collection made up entirely of {@code null}s behaves exactly like an empty collection. */
    @Test
    public void testForEachCollection_allNullMembersRunsOnCompleteAndProcessesNothing() throws Exception {
        for (final int readThreads : new int[] { 0, 2 }) {
            final List<Iterator<Integer>> iters = Arrays.asList(null, null);
            final List<Integer> out = Collections.synchronizedList(new ArrayList<>());
            final AtomicBoolean done = new AtomicBoolean(false);

            Iterators.forEach(iters, Iterators.IterateOptions.builder().readThreads(readThreads).build(), out::add, () -> done.set(true));

            assertTrue(out.isEmpty(), "readThreads=" + readThreads);
            assertTrue(done.get(), "readThreads=" + readThreads);
        }
    }

    /** B1: the positional (sequential) overload skips {@code null} members too. */
    @Test
    public void testForEachCollection_nullIteratorMemberIsSkipped_positionalOverload() throws Exception {
        final List<Iterator<Integer>> iters = Arrays.asList(null, Arrays.asList(1, 2, 3, 4).iterator(), null);
        final List<Integer> out = new ArrayList<>();

        Iterators.forEach(iters, 1, 2, out::add);

        assertEquals(Arrays.asList(2, 3), out);
    }

    /** B2: {@code queueSize} alone must not move reading off the calling thread. */
    @Test
    public void testForEach_queueSizeAloneDoesNotStartAReaderThread() throws Exception {
        final String caller = Thread.currentThread().getName();

        final ThreadRecordingIterator single = new ThreadRecordingIterator(50);
        Iterators.forEach(single, Iterators.IterateOptions.builder().queueSize(8).build(), x -> {
        });
        assertEquals(Collections.singleton(caller), single.readerThreads, "single-iterator overload must read on the calling thread");

        final ThreadRecordingIterator inCollection = new ThreadRecordingIterator(50);
        Iterators.forEach(Arrays.<Iterator<Integer>> asList(inCollection), Iterators.IterateOptions.builder().queueSize(8).build(), x -> {
        });
        assertEquals(Collections.singleton(caller), inCollection.readerThreads, "collection overload must read on the calling thread when readThreads == 0");

        // ... while readThreads > 0 still does move reading off it.
        final ThreadRecordingIterator concurrent = new ThreadRecordingIterator(50);
        Iterators.forEach(Arrays.<Iterator<Integer>> asList(concurrent), Iterators.IterateOptions.builder().readThreads(1).build(), x -> {
        });
        assertFalse(concurrent.readerThreads.equals(Collections.singleton(caller)), "readThreads > 0 must read on a pool thread");
    }

    /** B2: a queueSize set together with readThreads is still honoured and still delivers every element. */
    @Test
    public void testForEach_queueSizeWithReadThreadsStillDeliversEverything() throws Exception {
        final List<Integer> first = new ArrayList<>();
        final List<Integer> second = new ArrayList<>();

        for (int i = 0; i < 200; i++) {
            first.add(i);
            second.add(200 + i);
        }

        final List<Iterator<Integer>> iters = Arrays.asList(first.iterator(), second.iterator());
        final List<Integer> out = Collections.synchronizedList(new ArrayList<>());

        Iterators.forEach(iters, Iterators.IterateOptions.builder().readThreads(2).queueSize(4).build(), out::add);

        assertEquals(400, out.size());
        assertEquals(400, new HashSet<>(out).size());
    }

    /** D1: the single-iterator fast path must honour offset/count/onComplete exactly as the general path did. */
    @Test
    public void testForEachIterator_sequentialFastPathSlicing() throws Exception {
        assertEquals(Arrays.asList(3, 4, 5), collectForEach(Arrays.asList(1, 2, 3, 4, 5, 6).iterator(), 2, 3));
        assertEquals(Arrays.asList(1, 2, 3), collectForEach(Arrays.asList(1, 2, 3).iterator(), 0, 10));
        assertEquals(Collections.emptyList(), collectForEach(Arrays.asList(1, 2, 3).iterator(), 10, 3));
        assertEquals(Collections.emptyList(), collectForEach(Arrays.asList(1, 2, 3).iterator(), 0, 0));
        assertEquals(Arrays.asList(3), collectForEach(Arrays.asList(1, 2, 3).iterator(), 2, 5));
        assertEquals(Arrays.asList(1, 2, 3), collectForEach(Arrays.asList(1, 2, 3).iterator(), 0, Long.MAX_VALUE));

        // an offset with count == 0 still consumes the offset, exactly as the pre-fast-path implementation did
        final Iterator<Integer> iter = Arrays.asList(1, 2, 3, 4).iterator();
        Iterators.forEach(iter, 2, 0, x -> Assertions.fail("nothing should be processed"));
        assertEquals(Arrays.asList(3, 4), drainToList(iter));

        // onComplete runs on the fast path, and does not run when the consumer fails
        final AtomicBoolean done = new AtomicBoolean(false);
        Iterators.forEach(Arrays.asList(1, 2).iterator(), x -> {
        }, () -> done.set(true));
        assertTrue(done.get());

        done.set(false);
        assertThrows(IllegalStateException.class, () -> Iterators.forEach(Arrays.asList(1, 2).iterator(), x -> {
            throw new IllegalStateException("boom");
        }, () -> done.set(true)));
        assertFalse(done.get());
    }

    /**
     * D1: {@code processThreads} is a tuning knob, but it used to change the exception <i>type</i> a caller saw -
     * a checked exception came back wrapped in a {@code RuntimeException}, so the {@code catch} clause the
     * compiler still demanded (E is inferred from the consumer) silently stopped matching.
     */
    @Test
    public void testForEach_checkedExceptionPropagatesUnwrappedWhateverProcessThreadsIs() {
        for (final int processThreads : new int[] { 0, 1, 3 }) {
            final IOException thrown = assertThrows(IOException.class,
                    () -> Iterators.forEach(Arrays.asList(1, 2, 3).iterator(), Iterators.IterateOptions.builder().processThreads(processThreads).build(), x -> {
                        throw new IOException("boom");
                    }), "processThreads=" + processThreads);

            assertEquals("boom", thrown.getMessage());
        }
    }

    /** D1: the same must hold for {@code onComplete}'s {@code E2}. */
    @Test
    public void testForEach_onCompleteCheckedExceptionPropagatesUnwrappedWhateverProcessThreadsIs() {
        for (final int processThreads : new int[] { 0, 1, 3 }) {
            final AtomicInteger processed = new AtomicInteger();

            final IOException thrown = assertThrows(IOException.class, () -> Iterators.forEach(Arrays.asList(1, 2, 3).iterator(),
                    Iterators.IterateOptions.builder().processThreads(processThreads).build(), x -> processed.incrementAndGet(), () -> {
                        throw new IOException("done-boom");
                    }), "processThreads=" + processThreads);

            assertEquals("done-boom", thrown.getMessage());
            assertEquals(3, processed.get(), "onComplete runs only after every element has been processed");
        }
    }

    /** D1: unchecked failures and {@code Error}s keep passing through untouched on both paths. */
    @Test
    public void testForEach_uncheckedFailuresAndErrorsPassThroughOnBothPaths() {
        for (final int processThreads : new int[] { 0, 2 }) {
            final IllegalStateException unchecked = assertThrows(IllegalStateException.class,
                    () -> Iterators.forEach(Arrays.asList(1, 2).iterator(), Iterators.IterateOptions.builder().processThreads(processThreads).build(), x -> {
                        throw new IllegalStateException("rte");
                    }), "processThreads=" + processThreads);
            assertEquals("rte", unchecked.getMessage());

            final StackOverflowError error = assertThrows(StackOverflowError.class,
                    () -> Iterators.forEach(Arrays.asList(1, 2).iterator(), Iterators.IterateOptions.builder().processThreads(processThreads).build(), x -> {
                        throw new StackOverflowError("err");
                    }), "processThreads=" + processThreads);
            assertEquals("err", error.getMessage());
        }
    }

    /** D1: when several workers fail, the first failure is thrown and the rest are suppressed onto it. */
    @Test
    public void testForEach_laterWorkerFailuresAreSuppressedOntoTheFirst() {
        final int threads = 2;
        final CountDownLatch bothHoldAnElement = new CountDownLatch(threads);

        final IOException thrown = assertThrows(IOException.class,
                () -> Iterators.forEach(Arrays.asList(1, 2).iterator(), Iterators.IterateOptions.builder().processThreads(threads).build(), x -> {
                    // Make both workers take an element before either fails, so that the second failure is
                    // deterministically recorded as suppressed rather than racing the first one out.
                    bothHoldAnElement.countDown();

                    try {
                        bothHoldAnElement.await(5, TimeUnit.SECONDS);
                    } catch (final InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }

                    throw new IOException("boom-" + x);
                }));

        assertEquals(1, thrown.getSuppressed().length, "the second worker's failure must be suppressed onto the first");

        final List<String> messages = new ArrayList<>(Arrays.asList(thrown.getMessage(), thrown.getSuppressed()[0].getMessage()));
        Collections.sort(messages);
        assertEquals(Arrays.asList("boom-1", "boom-2"), messages);
    }

    /**
     * B3: the worker pool was built by {@code Executors.newFixedThreadPool(n)} with no thread factory, so its
     * threads were non-daemon (the interrupt path may abandon them after a bounded wait, and non-daemon threads
     * then keep the JVM alive) and unnamed ({@code pool-7-thread-2} cannot be attributed to a caller).
     */
    @Test
    public void testForEach_processThreadsUseNamedDaemonThreads() throws Exception {
        final java.util.Set<String> observed = Collections.synchronizedSet(new java.util.LinkedHashSet<>());

        Iterators.forEach(IntStream.range(0, 200).boxed().iterator(), Iterators.IterateOptions.builder().processThreads(2).build(), x -> {
            final Thread t = Thread.currentThread();
            observed.add(t.getName() + "|" + t.isDaemon());
        });

        assertFalse(observed.isEmpty());

        for (final String each : observed) {
            assertTrue(each.startsWith("Iterators-forEach-"), "worker threads must be named for thread dumps, was: " + each);
            assertTrue(each.endsWith("|true"), "worker threads must be daemon threads, was: " + each);
        }
    }

    /**
     * B2: the {@code readThreads > 0} path pulled its iterator with {@code Stream.iterator()}, which logs
     * "Remember to close .. because it has close handlers" - telling the caller to close a Stream they never see
     * and which {@code doForEach} always closes in its {@code finally}.
     */
    @Test
    public void testForEach_readThreadsDoesNotLogTheSpuriousCloseWarning() throws Exception {
        final List<String> logged = Collections.synchronizedList(new ArrayList<>());
        final java.util.logging.Handler capture = new java.util.logging.Handler() {
            @Override
            public void publish(final java.util.logging.LogRecord record) {
                if (record != null && record.getMessage() != null) {
                    logged.add(record.getMessage());
                }
            }

            @Override
            public void flush() {
                // nothing to flush - the records are kept in memory
            }

            @Override
            public void close() {
                // nothing to release
            }
        };

        final java.util.logging.Logger root = java.util.logging.LogManager.getLogManager().getLogger("");
        root.addHandler(capture);

        final List<Integer> collected = Collections.synchronizedList(new ArrayList<>());

        try {
            final List<Iterator<? extends Integer>> iterators = Arrays.asList(Arrays.asList(1, 2, 3).iterator(), Arrays.asList(4, 5, 6).iterator());
            Iterators.forEach(iterators, Iterators.IterateOptions.builder().readThreads(2).build(), collected::add);
        } finally {
            root.removeHandler(capture);
        }

        assertEquals(6, collected.size());
        assertTrue(logged.stream().noneMatch(m -> m.contains("Remember to close")),
                "forEach closes the stream it creates, so it must not ask the caller to: " + logged);
    }

    private static List<Integer> rangeOneToNine() {
        return IntList.range(1, 10).toList();
    }

    private static List<String> collectPairsDefault() {
        final List<String> out = new ArrayList<>();
        Stream.of(rangeOneToNine().iterator()).forEachPair((a, b) -> out.add(a + "-" + b));
        return out;
    }

    private static List<String> collectPairs(final int increment) {
        final List<String> out = new ArrayList<>();
        Stream.of(rangeOneToNine().iterator()).forEachPair(increment, (a, b) -> out.add(a + "-" + b));
        return out;
    }

    private static List<String> collectPairsParallel(final int increment) {
        final List<String> out = Collections.synchronizedList(new ArrayList<>());
        Stream.of(rangeOneToNine().iterator()).parallel(2).forEachPair(increment, (a, b) -> out.add(a + "-" + b));
        return out;
    }

    private static List<String> collectTriples(final int increment) {
        final List<String> out = new ArrayList<>();
        Stream.of(rangeOneToNine().iterator()).forEachTriple(increment, (a, b, c) -> out.add(a + "-" + b + "-" + c));
        return out;
    }

    private static List<String> collectTriplesParallel(final int increment) {
        final List<String> out = Collections.synchronizedList(new ArrayList<>());
        Stream.of(rangeOneToNine().iterator()).parallel(2).forEachTriple(increment, (a, b, c) -> out.add(a + "-" + b + "-" + c));
        return out;
    }

    private static List<String> slidingPairs(final boolean fromCollection, final int increment, final boolean ignoreUnpaired,
            final BiFunction<Integer, Integer, String> mapper) {
        final Stream<Integer> stream = fromCollection ? Stream.of(rangeOneToNine()) : Stream.of(rangeOneToNine().iterator());
        return stream.slidingMap(increment, ignoreUnpaired, mapper).toList();
    }

    private static List<String> slidingTriples(final boolean fromCollection, final int increment, final boolean ignoreUnpaired,
            final TriFunction<Integer, Integer, Integer, String> mapper) {
        final Stream<Integer> stream = fromCollection ? Stream.of(rangeOneToNine()) : Stream.of(rangeOneToNine().iterator());
        return stream.slidingMap(increment, ignoreUnpaired, mapper).toList();
    }

    /**
     * The {@code IterateOptions} "Thread budget" note used to say the shared read pool's threads are <i>not</i>
     * daemon threads and so delay JVM exit. Both pools the {@code readThreads} path can reach create daemon
     * threads, so there is nothing to avoid; this pins the corrected claim.
     */
    @Test
    public void testForEach_readThreadsUseDaemonPoolThreads() throws Exception {
        final Thread caller = Thread.currentThread();
        final java.util.Set<String> readers = Collections.synchronizedSet(new java.util.LinkedHashSet<>());
        final List<Iterator<Integer>> iters = new ArrayList<>();

        for (int i = 0; i < 4; i++) {
            final int base = i * 10;

            iters.add(new Iterator<>() {
                private int cursor;

                @Override
                public boolean hasNext() {
                    observe();
                    return cursor < 3;
                }

                @Override
                public Integer next() {
                    observe();
                    return base + cursor++;
                }

                private void observe() {
                    final Thread current = Thread.currentThread();

                    if (current != caller) {
                        readers.add(current.getName() + "|" + current.isDaemon());
                    }
                }
            });
        }

        final AtomicInteger consumed = new AtomicInteger();

        Iterators.forEach(iters, Iterators.IterateOptions.builder().readThreads(2).queueSize(8).build(), value -> consumed.incrementAndGet());

        assertEquals(12, consumed.get());
        assertFalse(readers.isEmpty(), "readThreads > 0 must read on pool threads, not only on the calling thread");

        for (final String each : readers) {
            assertTrue(each.endsWith("|true"), "read pool threads must be daemon threads, so they never hold the JVM open, was: " + each);
        }
    }

    /**
     * The four {@code forEach} overloads that do <i>not</i> declare an {@code onComplete} parameter documented one
     * anyway. This pins what their corrected {@code @param} text claims: a {@code null} source is treated as
     * empty, and a {@code null} member of the collection is skipped.
     */
    @Test
    public void testForEach_nullSourceIsTreatedAsEmptyOnTheOverloadsWithoutOnComplete() throws Exception {
        final AtomicInteger consumed = new AtomicInteger();

        Iterators.forEach((Iterator<Integer>) null, 0L, 10L, value -> consumed.incrementAndGet());
        Iterators.forEach((Iterator<Integer>) null, Iterators.IterateOptions.builder().build(), value -> consumed.incrementAndGet());
        Iterators.forEach((Collection<Iterator<Integer>>) null, value -> consumed.incrementAndGet());
        Iterators.forEach((Collection<Iterator<Integer>>) null, 0L, 10L, value -> consumed.incrementAndGet());

        assertEquals(0, consumed.get());

        Iterators.forEach(Arrays.asList(null, Arrays.asList(1, 2).iterator(), null), value -> consumed.addAndGet((Integer) value));
        assertEquals(3, consumed.get());

        Iterators.forEach(Arrays.asList(null, Arrays.asList(1, 2, 3).iterator(), null), 1L, 10L, value -> consumed.addAndGet((Integer) value));
        assertEquals(8, consumed.get());
    }
}

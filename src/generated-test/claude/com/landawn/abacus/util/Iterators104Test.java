package com.landawn.abacus.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

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
}
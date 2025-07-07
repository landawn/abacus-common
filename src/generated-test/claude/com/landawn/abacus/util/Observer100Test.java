package com.landawn.abacus.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

public class Observer100Test extends TestBase {

    // Tests for complete method
    @Test
    public void testComplete() {
        BlockingQueue<String> queue = new LinkedBlockingQueue<>();
        queue.offer("test");
        Observer.complete(queue);

        // The queue should contain the complete flag
        Assertions.assertEquals(2, queue.size());
    }

    // Tests for of methods
    @Test
    public void testOfBlockingQueue() throws InterruptedException {
        BlockingQueue<String> queue = new LinkedBlockingQueue<>();
        queue.offer("item1");
        queue.offer("item2");
        Observer.complete(queue);

        Observer<String> observer = Observer.of(queue);
        Assertions.assertNotNull(observer);

        List<String> results = new ArrayList<>();
        CountDownLatch latch = new CountDownLatch(1);

        observer.observe(results::add, e -> Assertions.fail("Unexpected error: " + e), latch::countDown);

        boolean completed = latch.await(5, TimeUnit.SECONDS);
        Assertions.assertTrue(completed);
        Assertions.assertEquals(Arrays.asList("item1", "item2"), results);
    }

    @Test
    public void testOfCollection() throws InterruptedException {
        Collection<Integer> collection = Arrays.asList(1, 2, 3);
        Observer<Integer> observer = Observer.of(collection);
        Assertions.assertNotNull(observer);

        List<Integer> results = new ArrayList<>();
        CountDownLatch latch = new CountDownLatch(1);

        observer.observe(results::add, e -> Assertions.fail("Unexpected error: " + e), latch::countDown);

        boolean completed = latch.await(5, TimeUnit.SECONDS);
        Assertions.assertTrue(completed);
        Assertions.assertEquals(Arrays.asList(1, 2, 3), results);
    }

    @Test
    public void testOfEmptyCollection() throws InterruptedException {
        Collection<String> collection = Collections.emptyList();
        Observer<String> observer = Observer.of(collection);
        Assertions.assertNotNull(observer);

        List<String> results = new ArrayList<>();
        CountDownLatch latch = new CountDownLatch(1);

        observer.observe(results::add, e -> Assertions.fail("Unexpected error: " + e), latch::countDown);

        boolean completed = latch.await(5, TimeUnit.SECONDS);
        Assertions.assertTrue(completed);
        Assertions.assertTrue(results.isEmpty());
    }

    @Test
    public void testOfIterator() throws InterruptedException {
        Iterator<String> iterator = Arrays.asList("a", "b", "c").iterator();
        Observer<String> observer = Observer.of(iterator);
        Assertions.assertNotNull(observer);

        List<String> results = new ArrayList<>();
        CountDownLatch latch = new CountDownLatch(1);

        observer.observe(results::add, e -> Assertions.fail("Unexpected error: " + e), latch::countDown);

        boolean completed = latch.await(5, TimeUnit.SECONDS);
        Assertions.assertTrue(completed);
        Assertions.assertEquals(Arrays.asList("a", "b", "c"), results);
    }

    // Tests for timer
    @Test
    public void testTimerMillis() throws InterruptedException {
        Observer<Long> observer = Observer.timer(100);
        Assertions.assertNotNull(observer);

        List<Long> results = new ArrayList<>();
        CountDownLatch latch = new CountDownLatch(1);

        long startTime = System.currentTimeMillis();
        observer.observe(results::add, e -> Assertions.fail("Unexpected error: " + e), latch::countDown);

        boolean completed = latch.await(5, TimeUnit.SECONDS);
        long elapsedTime = System.currentTimeMillis() - startTime;

        Assertions.assertTrue(completed);
        Assertions.assertEquals(1, results.size());
        Assertions.assertEquals(0L, results.get(0));
        Assertions.assertTrue(elapsedTime >= 100);
    }

    @Test
    public void testTimerWithUnit() throws InterruptedException {
        Observer<Long> observer = Observer.timer(200, TimeUnit.MILLISECONDS);
        Assertions.assertNotNull(observer);

        List<Long> results = new ArrayList<>();
        CountDownLatch latch = new CountDownLatch(1);

        long startTime = System.currentTimeMillis();
        observer.observe(results::add, e -> Assertions.fail("Unexpected error: " + e), latch::countDown);

        boolean completed = latch.await(5, TimeUnit.SECONDS);
        long elapsedTime = System.currentTimeMillis() - startTime;

        Assertions.assertTrue(completed);
        Assertions.assertEquals(1, results.size());
        Assertions.assertEquals(0L, results.get(0));
        Assertions.assertTrue(elapsedTime >= 200);
    }

    @Test
    public void testTimerInvalidArgs() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> Observer.timer(-1));
        Assertions.assertThrows(IllegalArgumentException.class, () -> Observer.timer(-1, TimeUnit.MILLISECONDS));
        Assertions.assertThrows(IllegalArgumentException.class, () -> Observer.timer(100, null));
    }

    // Tests for interval
    @Test
    public void testIntervalMillis() throws InterruptedException {
        Observer<Long> observer = Observer.interval(100);
        Assertions.assertNotNull(observer);

        List<Long> results = new ArrayList<>();
        CountDownLatch latch = new CountDownLatch(3);

        observer.limit(3).observe(value -> {
            results.add(value);
            latch.countDown();
        }, e -> Assertions.fail("Unexpected error: " + e), () -> {
        });

        boolean completed = latch.await(5, TimeUnit.SECONDS);
        Assertions.assertTrue(completed);
        Assertions.assertEquals(3, results.size());
        Assertions.assertEquals(Arrays.asList(0L, 1L, 2L), results);
    }

    @Test
    public void testIntervalWithInitialDelay() throws InterruptedException {
        Observer<Long> observer = Observer.interval(50, 100);
        Assertions.assertNotNull(observer);

        List<Long> results = new ArrayList<>();
        CountDownLatch latch = new CountDownLatch(2);

        long startTime = System.currentTimeMillis();
        observer.limit(2).observe(value -> {
            results.add(value);
            latch.countDown();
        }, e -> Assertions.fail("Unexpected error: " + e), () -> {
        });

        boolean completed = latch.await(5, TimeUnit.SECONDS);
        long elapsedTime = System.currentTimeMillis() - startTime;

        Assertions.assertTrue(completed);
        Assertions.assertEquals(2, results.size());
        Assertions.assertTrue(elapsedTime >= 50); // At least initial delay
    }

    @Test
    public void testIntervalWithUnit() throws InterruptedException {
        Observer<Long> observer = Observer.interval(100, TimeUnit.MILLISECONDS);
        Assertions.assertNotNull(observer);

        List<Long> results = new ArrayList<>();
        CountDownLatch latch = new CountDownLatch(2);

        observer.limit(2).observe(value -> {
            results.add(value);
            latch.countDown();
        }, e -> Assertions.fail("Unexpected error: " + e), () -> {
        });

        boolean completed = latch.await(5, TimeUnit.SECONDS);
        Assertions.assertTrue(completed);
        Assertions.assertEquals(2, results.size());
    }

    @Test
    public void testIntervalFullParams() throws InterruptedException {
        Observer<Long> observer = Observer.interval(50, 100, TimeUnit.MILLISECONDS);
        Assertions.assertNotNull(observer);

        List<Long> results = new ArrayList<>();
        CountDownLatch latch = new CountDownLatch(2);

        observer.limit(2).observe(value -> {
            results.add(value);
            latch.countDown();
        }, e -> Assertions.fail("Unexpected error: " + e), () -> {
        });

        boolean completed = latch.await(5, TimeUnit.SECONDS);
        Assertions.assertTrue(completed);
        Assertions.assertEquals(2, results.size());
    }

    @Test
    public void testIntervalInvalidArgs() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> Observer.interval(-1, 100, TimeUnit.MILLISECONDS));
        Assertions.assertThrows(IllegalArgumentException.class, () -> Observer.interval(0, 0, TimeUnit.MILLISECONDS));
        Assertions.assertThrows(IllegalArgumentException.class, () -> Observer.interval(0, -1, TimeUnit.MILLISECONDS));
        Assertions.assertThrows(IllegalArgumentException.class, () -> Observer.interval(0, 100, null));
    }

    // Tests for debounce
    @Test
    public void testDebounceMillis() throws InterruptedException {
        Observer<Integer> observer = Observer.of(Arrays.asList(1, 2, 3, 4, 5));
        Observer<Integer> debounced = observer.debounce(100);
        Assertions.assertSame(observer, debounced); // Should return same instance

        List<Integer> results = new ArrayList<>();
        CountDownLatch latch = new CountDownLatch(1);

        debounced.observe(results::add, e -> Assertions.fail("Unexpected error: " + e), latch::countDown);

        boolean completed = latch.await(5, TimeUnit.SECONDS);
        Assertions.assertTrue(completed);
        // Due to debouncing, we should get fewer results
        Assertions.assertTrue(results.size() <= 5);
    }

    @Test
    public void testDebounceZero() {
        Observer<String> observer = Observer.of(Arrays.asList("a", "b"));
        Observer<String> debounced = observer.debounce(0);
        Assertions.assertSame(observer, debounced); // Should return same instance without modification
    }

    @Test
    public void testDebounceInvalidArgs() {
        Observer<String> observer = Observer.of(Arrays.asList("test"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> observer.debounce(-1));
        Assertions.assertThrows(IllegalArgumentException.class, () -> observer.debounce(100, null));
    }

    // Tests for throttleFirst
    @Test
    public void testThrottleFirstMillis() throws InterruptedException {
        Observer<Integer> observer = Observer.of(Arrays.asList(1, 2, 3, 4, 5));
        Observer<Integer> throttled = observer.throttleFirst(100);
        Assertions.assertSame(observer, throttled);

        List<Integer> results = new ArrayList<>();
        CountDownLatch latch = new CountDownLatch(1);

        throttled.observe(results::add, e -> Assertions.fail("Unexpected error: " + e), latch::countDown);

        boolean completed = latch.await(5, TimeUnit.SECONDS);
        Assertions.assertTrue(completed);
        // Due to throttling, we should get fewer results
        Assertions.assertTrue(results.size() <= 5);
    }

    @Test
    public void testThrottleFirstZero() {
        Observer<String> observer = Observer.of(Arrays.asList("a", "b"));
        Observer<String> throttled = observer.throttleFirst(0);
        Assertions.assertSame(observer, throttled);
    }

    @Test
    public void testThrottleFirstInvalidArgs() {
        Observer<String> observer = Observer.of(Arrays.asList("test"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> observer.throttleFirst(-1));
        Assertions.assertThrows(IllegalArgumentException.class, () -> observer.throttleFirst(100, null));
    }

    // Tests for throttleLast
    @Test
    public void testThrottleLastMillis() throws InterruptedException {
        Observer<Integer> observer = Observer.of(Arrays.asList(1, 2, 3, 4, 5));
        Observer<Integer> throttled = observer.throttleLast(100);
        Assertions.assertSame(observer, throttled);

        List<Integer> results = new ArrayList<>();
        CountDownLatch latch = new CountDownLatch(1);

        throttled.observe(results::add, e -> Assertions.fail("Unexpected error: " + e), latch::countDown);

        boolean completed = latch.await(5, TimeUnit.SECONDS);
        Assertions.assertTrue(completed);
        // Due to throttling, we should get fewer results
        Assertions.assertTrue(results.size() <= 5);
    }

    @Test
    public void testThrottleLastZero() {
        Observer<String> observer = Observer.of(Arrays.asList("a", "b"));
        Observer<String> throttled = observer.throttleLast(0);
        Assertions.assertSame(observer, throttled);
    }

    @Test
    public void testThrottleLastInvalidArgs() {
        Observer<String> observer = Observer.of(Arrays.asList("test"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> observer.throttleLast(-1));
        Assertions.assertThrows(IllegalArgumentException.class, () -> observer.throttleLast(100, null));
    }

    // Tests for delay
    @Test
    public void testDelayMillis() throws InterruptedException {
        Observer<String> observer = Observer.of(Arrays.asList("test"));
        Observer<String> delayed = observer.delay(100);
        Assertions.assertSame(observer, delayed);

        List<String> results = new ArrayList<>();
        CountDownLatch latch = new CountDownLatch(1);

        long startTime = System.currentTimeMillis();
        delayed.observe(results::add, e -> Assertions.fail("Unexpected error: " + e), latch::countDown);

        boolean completed = latch.await(5, TimeUnit.SECONDS);
        long elapsedTime = System.currentTimeMillis() - startTime;

        Assertions.assertTrue(completed);
        Assertions.assertEquals(Arrays.asList("test"), results);
        Assertions.assertTrue(elapsedTime >= 100);
    }

    @Test
    public void testDelayZero() {
        Observer<String> observer = Observer.of(Arrays.asList("a", "b"));
        Observer<String> delayed = observer.delay(0);
        Assertions.assertSame(observer, delayed);
    }

    @Test
    public void testDelayInvalidArgs() {
        Observer<String> observer = Observer.of(Arrays.asList("test"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> observer.delay(-1));
        Assertions.assertThrows(IllegalArgumentException.class, () -> observer.delay(100, null));
    }

    // Tests for timeInterval
    @Test
    public void testTimeInterval() throws InterruptedException {
        Observer<Integer> observer = Observer.of(Arrays.asList(1, 2, 3));
        Observer<Timed<Integer>> timed = observer.timeInterval();
        Assertions.assertNotNull(timed);

        List<Timed<Integer>> results = new ArrayList<>();
        CountDownLatch latch = new CountDownLatch(1);

        timed.observe(results::add, e -> Assertions.fail("Unexpected error: " + e), latch::countDown);

        boolean completed = latch.await(5, TimeUnit.SECONDS);
        Assertions.assertTrue(completed);
        Assertions.assertEquals(3, results.size());

        // Check values
        Assertions.assertEquals(Integer.valueOf(1), results.get(0).value());
        Assertions.assertEquals(Integer.valueOf(2), results.get(1).value());
        Assertions.assertEquals(Integer.valueOf(3), results.get(2).value());

        // Check intervals are positive
        for (Timed<Integer> t : results) {
            Assertions.assertTrue(t.timestamp() >= 0);
        }
    }

    // Tests for timestamp
    @Test
    public void testTimestamp() throws InterruptedException {
        Observer<String> observer = Observer.of(Arrays.asList("a", "b"));
        Observer<Timed<String>> timed = observer.timestamp();
        Assertions.assertNotNull(timed);

        List<Timed<String>> results = new ArrayList<>();
        CountDownLatch latch = new CountDownLatch(1);

        long beforeTime = System.currentTimeMillis();
        timed.observe(results::add, e -> Assertions.fail("Unexpected error: " + e), latch::countDown);

        boolean completed = latch.await(5, TimeUnit.SECONDS);
        long afterTime = System.currentTimeMillis();

        Assertions.assertTrue(completed);
        Assertions.assertEquals(2, results.size());

        // Check values
        Assertions.assertEquals("a", results.get(0).value());
        Assertions.assertEquals("b", results.get(1).value());

        // Check timestamps are in range
        for (Timed<String> t : results) {
            Assertions.assertTrue(t.timestamp() >= beforeTime);
            Assertions.assertTrue(t.timestamp() <= afterTime);
        }
    }

    // Tests for skip
    @Test
    public void testSkip() throws InterruptedException {
        Observer<Integer> observer = Observer.of(Arrays.asList(1, 2, 3, 4, 5));
        Observer<Integer> skipped = observer.skip(2);
        Assertions.assertSame(observer, skipped);

        List<Integer> results = new ArrayList<>();
        CountDownLatch latch = new CountDownLatch(1);

        skipped.observe(results::add, e -> Assertions.fail("Unexpected error: " + e), latch::countDown);

        boolean completed = latch.await(5, TimeUnit.SECONDS);
        Assertions.assertTrue(completed);
        Assertions.assertEquals(Arrays.asList(3, 4, 5), results);
    }

    @Test
    public void testSkipZero() throws InterruptedException {
        Observer<String> observer = Observer.of(Arrays.asList("a", "b"));
        Observer<String> skipped = observer.skip(0);

        List<String> results = new ArrayList<>();
        CountDownLatch latch = new CountDownLatch(1);

        skipped.observe(results::add, e -> Assertions.fail("Unexpected error: " + e), latch::countDown);

        boolean completed = latch.await(5, TimeUnit.SECONDS);
        Assertions.assertTrue(completed);
        Assertions.assertEquals(Arrays.asList("a", "b"), results);
    }

    @Test
    public void testSkipInvalidArgs() {
        Observer<String> observer = Observer.of(Arrays.asList("test"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> observer.skip(-1));
    }

    // Tests for limit
    @Test
    public void testLimit() throws InterruptedException {
        Observer<Integer> observer = Observer.of(Arrays.asList(1, 2, 3, 4, 5));
        Observer<Integer> limited = observer.limit(3);
        Assertions.assertSame(observer, limited);

        List<Integer> results = new ArrayList<>();
        CountDownLatch latch = new CountDownLatch(1);

        limited.observe(results::add, e -> Assertions.fail("Unexpected error: " + e), latch::countDown);

        boolean completed = latch.await(5, TimeUnit.SECONDS);
        Assertions.assertTrue(completed);
        Assertions.assertEquals(Arrays.asList(1, 2, 3), results);
    }

    @Test
    public void testLimitZero() throws InterruptedException {
        Observer<String> observer = Observer.of(Arrays.asList("a", "b"));
        Observer<String> limited = observer.limit(0);

        List<String> results = new ArrayList<>();
        CountDownLatch latch = new CountDownLatch(1);

        limited.observe(results::add, e -> Assertions.fail("Unexpected error: " + e), latch::countDown);

        boolean completed = latch.await(5, TimeUnit.SECONDS);
        Assertions.assertTrue(completed);
        Assertions.assertTrue(results.isEmpty());
    }

    @Test
    public void testLimitInvalidArgs() {
        Observer<String> observer = Observer.of(Arrays.asList("test"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> observer.limit(-1));
    }

    // Tests for distinct
    @Test
    public void testDistinct() throws InterruptedException {
        Observer<Integer> observer = Observer.of(Arrays.asList(1, 2, 2, 3, 3, 3, 4));
        Observer<Integer> distinct = observer.distinct();
        Assertions.assertSame(observer, distinct);

        List<Integer> results = new ArrayList<>();
        CountDownLatch latch = new CountDownLatch(1);

        distinct.observe(results::add, e -> Assertions.fail("Unexpected error: " + e), latch::countDown);

        boolean completed = latch.await(5, TimeUnit.SECONDS);
        Assertions.assertTrue(completed);
        Assertions.assertEquals(Arrays.asList(1, 2, 3, 4), results);
    }

    // Tests for distinctBy
    @Test
    public void testDistinctBy() throws InterruptedException {
        Observer<String> observer = Observer.of(Arrays.asList("a", "bb", "ccc", "dd", "e"));
        Observer<String> distinct = observer.distinctBy(String::length);
        Assertions.assertSame(observer, distinct);

        List<String> results = new ArrayList<>();
        CountDownLatch latch = new CountDownLatch(1);

        distinct.observe(results::add, e -> Assertions.fail("Unexpected error: " + e), latch::countDown);

        boolean completed = latch.await(5, TimeUnit.SECONDS);
        Assertions.assertTrue(completed);
        Assertions.assertEquals(Arrays.asList("a", "bb", "ccc"), results);
    }

    // Tests for filter
    @Test
    public void testFilter() throws InterruptedException {
        Observer<Integer> observer = Observer.of(Arrays.asList(1, 2, 3, 4, 5));
        Observer<Integer> filtered = observer.filter(i -> i % 2 == 0);
        Assertions.assertSame(observer, filtered);

        List<Integer> results = new ArrayList<>();
        CountDownLatch latch = new CountDownLatch(1);

        filtered.observe(results::add, e -> Assertions.fail("Unexpected error: " + e), latch::countDown);

        boolean completed = latch.await(5, TimeUnit.SECONDS);
        Assertions.assertTrue(completed);
        Assertions.assertEquals(Arrays.asList(2, 4), results);
    }

    // Tests for map
    @Test
    public void testMap() throws InterruptedException {
        Observer<Integer> observer = Observer.of(Arrays.asList(1, 2, 3));
        Observer<String> mapped = observer.map(i -> "value-" + i);
        Assertions.assertNotNull(mapped);

        List<String> results = new ArrayList<>();
        CountDownLatch latch = new CountDownLatch(1);

        mapped.observe(results::add, e -> Assertions.fail("Unexpected error: " + e), latch::countDown);

        boolean completed = latch.await(5, TimeUnit.SECONDS);
        Assertions.assertTrue(completed);
        Assertions.assertEquals(Arrays.asList("value-1", "value-2", "value-3"), results);
    }

    // Tests for flatMap
    @Test
    public void testFlatMap() throws InterruptedException {
        Observer<Integer> observer = Observer.of(Arrays.asList(1, 2, 3));
        Observer<String> flatMapped = observer.flatMap(i -> Arrays.asList("a" + i, "b" + i));
        Assertions.assertNotNull(flatMapped);

        List<String> results = new ArrayList<>();
        CountDownLatch latch = new CountDownLatch(1);

        flatMapped.observe(results::add, e -> Assertions.fail("Unexpected error: " + e), latch::countDown);

        boolean completed = latch.await(5, TimeUnit.SECONDS);
        Assertions.assertTrue(completed);
        Assertions.assertEquals(Arrays.asList("a1", "b1", "a2", "b2", "a3", "b3"), results);
    }

    @Test
    public void testFlatMapEmpty() throws InterruptedException {
        Observer<Integer> observer = Observer.of(Arrays.asList(1, 2));
        Observer<String> flatMapped = observer.flatMap(i -> Collections.emptyList());

        List<String> results = new ArrayList<>();
        CountDownLatch latch = new CountDownLatch(1);

        flatMapped.observe(results::add, e -> Assertions.fail("Unexpected error: " + e), latch::countDown);

        boolean completed = latch.await(5, TimeUnit.SECONDS);
        Assertions.assertTrue(completed);
        Assertions.assertTrue(results.isEmpty());
    }

    // Tests for buffer with time
    @Test
    public void testBufferTime() throws InterruptedException {
        Observer<Long> observer = Observer.interval(50, TimeUnit.MILLISECONDS);
        Observer<List<Long>> buffered = observer.buffer(200, TimeUnit.MILLISECONDS);
        Assertions.assertNotNull(buffered);

        List<List<Long>> results = new ArrayList<>();
        CountDownLatch latch = new CountDownLatch(2);

        buffered.limit(2).observe(list -> {
            results.add(new ArrayList<>(list));
            latch.countDown();
        }, e -> Assertions.fail("Unexpected error: " + e), () -> {
        });

        boolean completed = latch.await(5, TimeUnit.SECONDS);
        Assertions.assertTrue(completed);
        Assertions.assertEquals(2, results.size());

        // Each buffer should contain multiple items
        for (List<Long> buffer : results) {
            Assertions.assertTrue(buffer.size() > 0);
        }
    }

    @Test
    public void testBufferTimeAndCount() throws InterruptedException {
        Observer<Integer> observer = Observer.of(Arrays.asList(1, 2, 3, 4, 5, 6));
        Observer<List<Integer>> buffered = observer.buffer(1000, TimeUnit.MILLISECONDS, 3);
        Assertions.assertNotNull(buffered);

        List<List<Integer>> results = new ArrayList<>();
        CountDownLatch latch = new CountDownLatch(1);

        buffered.observe(list -> results.add(new ArrayList<>(list)), e -> Assertions.fail("Unexpected error: " + e), latch::countDown);

        boolean completed = latch.await(5, TimeUnit.SECONDS);
        Assertions.assertTrue(completed);

        // Should have at least 2 buffers (6 items / 3 count = 2)
        Assertions.assertTrue(results.size() >= 2);
    }

    @Test
    public void testBufferInvalidArgs() {
        Observer<String> observer = Observer.of(Arrays.asList("test"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> observer.buffer(0, TimeUnit.MILLISECONDS));
        Assertions.assertThrows(IllegalArgumentException.class, () -> observer.buffer(-1, TimeUnit.MILLISECONDS));
        Assertions.assertThrows(IllegalArgumentException.class, () -> observer.buffer(100, null));
        Assertions.assertThrows(IllegalArgumentException.class, () -> observer.buffer(100, TimeUnit.MILLISECONDS, 0));
        Assertions.assertThrows(IllegalArgumentException.class, () -> observer.buffer(100, TimeUnit.MILLISECONDS, -1));
    }

    // Tests for buffer with time skip
    @Test
    public void testBufferTimeSkip() throws InterruptedException {
        Observer<Long> observer = Observer.interval(50, TimeUnit.MILLISECONDS);
        Observer<List<Long>> buffered = observer.buffer(100, 200, TimeUnit.MILLISECONDS);
        Assertions.assertNotNull(buffered);

        List<List<Long>> results = new ArrayList<>();
        CountDownLatch latch = new CountDownLatch(2);
        AtomicInteger count = new AtomicInteger(0);

        buffered.observe(list -> {
            results.add(new ArrayList<>(list));
            if (count.incrementAndGet() >= 2) {
                latch.countDown();
                latch.countDown();
            }
        }, e -> Assertions.fail("Unexpected error: " + e), () -> {
        });

        boolean completed = latch.await(5, TimeUnit.SECONDS);
        Assertions.assertTrue(completed);
        Assertions.assertTrue(results.size() >= 2);
    }

    @Test
    public void testBufferTimeSkipInvalidArgs() {
        Observer<String> observer = Observer.of(Arrays.asList("test"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> observer.buffer(0, 100, TimeUnit.MILLISECONDS));
        Assertions.assertThrows(IllegalArgumentException.class, () -> observer.buffer(100, 0, TimeUnit.MILLISECONDS));
        Assertions.assertThrows(IllegalArgumentException.class, () -> observer.buffer(-1, 100, TimeUnit.MILLISECONDS));
        Assertions.assertThrows(IllegalArgumentException.class, () -> observer.buffer(100, -1, TimeUnit.MILLISECONDS));
        Assertions.assertThrows(IllegalArgumentException.class, () -> observer.buffer(100, 200, null));
        Assertions.assertThrows(IllegalArgumentException.class, () -> observer.buffer(100, 200, TimeUnit.MILLISECONDS, 0));
    }

    // Tests for observe methods
    @Test
    public void testObserveWithAction() throws InterruptedException {
        Observer<String> observer = Observer.of(Arrays.asList("test"));
        CountDownLatch latch = new CountDownLatch(1);
        AtomicBoolean called = new AtomicBoolean(false);

        observer.observe(s -> {
            called.set(true);
            Assertions.assertEquals("test", s);
            latch.countDown();
        });

        boolean completed = latch.await(5, TimeUnit.SECONDS);
        Assertions.assertTrue(completed);
        Assertions.assertTrue(called.get());
    }

    @Test
    public void testObserveWithActionAndError() throws InterruptedException {
        Observer<String> observer = Observer.of(Arrays.asList("test"));
        observer = observer.map(s -> {
            throw new RuntimeException("Test error");
        });

        CountDownLatch latch = new CountDownLatch(1);
        AtomicBoolean errorCalled = new AtomicBoolean(false);

        observer.observe(s -> Assertions.fail("Should not be called"), e -> {
            errorCalled.set(true);
            Assertions.assertTrue(e instanceof RuntimeException);
            Assertions.assertEquals("Test error", e.getMessage());
            latch.countDown();
        });

        boolean completed = latch.await(5, TimeUnit.SECONDS);
        Assertions.assertTrue(completed);
        Assertions.assertTrue(errorCalled.get());
    }

    @Test
    public void testObserveWithActionErrorComplete() throws InterruptedException {
        Observer<String> observer = Observer.of(Arrays.asList("a", "b", "c"));

        List<String> results = new ArrayList<>();
        CountDownLatch latch = new CountDownLatch(1);
        AtomicBoolean completeCalled = new AtomicBoolean(false);

        observer.observe(results::add, e -> Assertions.fail("Unexpected error: " + e), () -> {
            completeCalled.set(true);
            latch.countDown();
        });

        boolean completed = latch.await(5, TimeUnit.SECONDS);
        Assertions.assertTrue(completed);
        Assertions.assertTrue(completeCalled.get());
        Assertions.assertEquals(Arrays.asList("a", "b", "c"), results);
    }

    // Test chaining multiple operations
    @Test
    public void testChainedOperations() throws InterruptedException {
        Observer<Integer> observer = Observer.of(Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10));

        List<String> results = new ArrayList<>();
        CountDownLatch latch = new CountDownLatch(1);

        observer.skip(2)
                .filter(i -> i % 2 == 0)
                .map(i -> i * 2)
                .limit(3)
                .map(i -> "Result: " + i)
                .observe(results::add, e -> Assertions.fail("Unexpected error: " + e), latch::countDown);

        boolean completed = latch.await(5, TimeUnit.SECONDS);
        Assertions.assertTrue(completed);
        Assertions.assertEquals(Arrays.asList("Result: 8", "Result: 12", "Result: 16"), results);
    }

}
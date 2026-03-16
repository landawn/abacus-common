package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

@Tag("2025")
public class ObserverTest extends TestBase {

    // ==================== complete(BlockingQueue) ====================

    @Test
    public void testComplete() {
        BlockingQueue<String> queue = new LinkedBlockingQueue<>();
        queue.offer("test");
        Observer.complete(queue);

        Assertions.assertEquals(2, queue.size());
    }

    @Test
    public void testComplete_EmptyQueue() {
        BlockingQueue<String> queue = new LinkedBlockingQueue<>();
        Observer.complete(queue);

        Assertions.assertEquals(1, queue.size());
    }

    @Test
    public void testComplete_MultipleTimes() {
        BlockingQueue<String> queue = new LinkedBlockingQueue<>();
        Observer.complete(queue);
        Observer.complete(queue);
        Assertions.assertEquals(2, queue.size());
    }

    // ==================== of(BlockingQueue) ====================

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
    public void testOfBlockingQueue_SingleElement() throws InterruptedException {
        BlockingQueue<Integer> queue = new LinkedBlockingQueue<>();
        queue.offer(42);
        Observer.complete(queue);

        List<Integer> results = new ArrayList<>();
        CountDownLatch latch = new CountDownLatch(1);

        Observer.of(queue).observe(results::add, e -> Assertions.fail("Unexpected error: " + e), latch::countDown);

        boolean completed = latch.await(5, TimeUnit.SECONDS);
        Assertions.assertTrue(completed);
        Assertions.assertEquals(Arrays.asList(42), results);
    }

    @Test
    public void testOfBlockingQueue_NullArg() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> Observer.of((BlockingQueue<String>) null));
    }

    // ==================== of(Collection) ====================

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
    public void testOfCollection_Empty() throws InterruptedException {
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
    public void testOfCollection_Null() throws InterruptedException {
        Observer<String> observer = Observer.of((Collection<String>) null);
        Assertions.assertNotNull(observer);

        List<String> results = new ArrayList<>();
        CountDownLatch latch = new CountDownLatch(1);

        observer.observe(results::add, e -> Assertions.fail("Unexpected error: " + e), latch::countDown);

        boolean completed = latch.await(5, TimeUnit.SECONDS);
        Assertions.assertTrue(completed);
        Assertions.assertTrue(results.isEmpty());
    }

    @Test
    public void testOfCollection_SingleElement() throws InterruptedException {
        Observer<String> observer = Observer.of(Collections.singletonList("only"));
        List<String> results = new ArrayList<>();
        CountDownLatch latch = new CountDownLatch(1);

        observer.observe(results::add, e -> Assertions.fail("Unexpected error: " + e), latch::countDown);

        boolean completed = latch.await(5, TimeUnit.SECONDS);
        Assertions.assertTrue(completed);
        Assertions.assertEquals(Arrays.asList("only"), results);
    }

    // ==================== of(Iterator) ====================

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

    @Test
    public void testOfIterator_Empty() throws InterruptedException {
        Iterator<String> iterator = Collections.<String> emptyList().iterator();
        Observer<String> observer = Observer.of(iterator);
        Assertions.assertNotNull(observer);

        List<String> results = new ArrayList<>();
        CountDownLatch latch = new CountDownLatch(1);

        observer.observe(results::add, e -> Assertions.fail("Unexpected error: " + e), latch::countDown);

        boolean completed = latch.await(5, TimeUnit.SECONDS);
        Assertions.assertTrue(completed);
        Assertions.assertTrue(results.isEmpty());
    }

    @Test
    public void testOfIterator_NullArg() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> Observer.of((Iterator<String>) null));
    }

    // ==================== timer(long) ====================

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
    public void testTimerMillis_ZeroDelay() throws InterruptedException {
        Observer<Long> observer = Observer.timer(0);
        Assertions.assertNotNull(observer);

        List<Long> results = new ArrayList<>();
        CountDownLatch latch = new CountDownLatch(1);

        observer.observe(results::add, e -> Assertions.fail("Unexpected error: " + e), latch::countDown);

        boolean completed = latch.await(5, TimeUnit.SECONDS);
        Assertions.assertTrue(completed);
        Assertions.assertEquals(1, results.size());
        Assertions.assertEquals(0L, results.get(0));
    }

    // ==================== timer(long, TimeUnit) ====================

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
    public void testTimerWithUnit_Seconds() throws InterruptedException {
        Observer<Long> observer = Observer.timer(1, TimeUnit.SECONDS);
        Assertions.assertNotNull(observer);

        List<Long> results = new ArrayList<>();
        CountDownLatch latch = new CountDownLatch(1);

        long startTime = System.currentTimeMillis();
        observer.observe(results::add, e -> Assertions.fail("Unexpected error: " + e), latch::countDown);

        boolean completed = latch.await(5, TimeUnit.SECONDS);
        long elapsedTime = System.currentTimeMillis() - startTime;

        Assertions.assertTrue(completed);
        Assertions.assertEquals(1, results.size());
        Assertions.assertTrue(elapsedTime >= 1000);
    }

    @Test
    public void testTimerInvalidArgs() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> Observer.timer(-1));
        Assertions.assertThrows(IllegalArgumentException.class, () -> Observer.timer(-1, TimeUnit.MILLISECONDS));
        Assertions.assertThrows(IllegalArgumentException.class, () -> Observer.timer(100, null));
    }

    // ==================== interval(long) ====================

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

    // ==================== interval(long, long) ====================

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
        Assertions.assertTrue(elapsedTime >= 50);
    }

    @Test
    public void testIntervalWithInitialDelay_InvalidArgs() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> Observer.interval(-1, 100));
        Assertions.assertThrows(IllegalArgumentException.class, () -> Observer.interval(0, 0));
        Assertions.assertThrows(IllegalArgumentException.class, () -> Observer.interval(0, -1));
    }

    // ==================== interval(long, TimeUnit) ====================

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
    public void testIntervalWithUnit_InvalidArgs() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> Observer.interval(-1, TimeUnit.MILLISECONDS));
        Assertions.assertThrows(IllegalArgumentException.class, () -> Observer.interval(0, TimeUnit.MILLISECONDS));
        Assertions.assertThrows(IllegalArgumentException.class, () -> Observer.interval(100, null));
    }

    // ==================== interval(long, long, TimeUnit) ====================

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
    public void testIntervalFullParams_InvalidArgs() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> Observer.interval(-1, 100, TimeUnit.MILLISECONDS));
        Assertions.assertThrows(IllegalArgumentException.class, () -> Observer.interval(0, 0, TimeUnit.MILLISECONDS));
        Assertions.assertThrows(IllegalArgumentException.class, () -> Observer.interval(0, -1, TimeUnit.MILLISECONDS));
        Assertions.assertThrows(IllegalArgumentException.class, () -> Observer.interval(0, 100, null));
    }

    @Test
    public void testIntervalFullParams_ZeroInitialDelay() throws InterruptedException {
        Observer<Long> observer = Observer.interval(0, 100, TimeUnit.MILLISECONDS);
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
        Assertions.assertEquals(0L, results.get(0));
        Assertions.assertEquals(1L, results.get(1));
    }

    // ==================== debounce(long) ====================

    @Test
    public void testDebounceMillis() throws InterruptedException {
        Observer<Integer> observer = Observer.of(Arrays.asList(1, 2, 3, 4, 5));
        Observer<Integer> debounced = observer.debounce(100);
        Assertions.assertSame(observer, debounced);

        List<Integer> results = new ArrayList<>();
        CountDownLatch latch = new CountDownLatch(1);

        debounced.observe(results::add, e -> Assertions.fail("Unexpected error: " + e), latch::countDown);

        boolean completed = latch.await(5, TimeUnit.SECONDS);
        Assertions.assertTrue(completed);
        Assertions.assertTrue(results.size() <= 5);
    }

    @Test
    public void testDebounceMillis_Zero() {
        Observer<String> observer = Observer.of(Arrays.asList("a", "b"));
        Observer<String> debounced = observer.debounce(0);
        Assertions.assertSame(observer, debounced);
    }

    @Test
    public void testDebounceMillis_InvalidArgs() {
        Observer<String> observer = Observer.of(Arrays.asList("test"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> observer.debounce(-1));
    }

    // ==================== debounce(long, TimeUnit) ====================

    @Test
    public void testDebounceWithUnit() throws InterruptedException {
        Observer<Integer> observer = Observer.of(Arrays.asList(1, 2, 3));
        Observer<Integer> debounced = observer.debounce(100, TimeUnit.MILLISECONDS);
        Assertions.assertSame(observer, debounced);

        List<Integer> results = new ArrayList<>();
        CountDownLatch latch = new CountDownLatch(1);

        debounced.observe(results::add, e -> Assertions.fail("Unexpected error: " + e), latch::countDown);

        boolean completed = latch.await(5, TimeUnit.SECONDS);
        Assertions.assertTrue(completed);
        Assertions.assertTrue(results.size() <= 3);
    }

    @Test
    public void testDebounceWithUnit_Zero() {
        Observer<String> observer = Observer.of(Arrays.asList("a"));
        Observer<String> debounced = observer.debounce(0, TimeUnit.MILLISECONDS);
        Assertions.assertSame(observer, debounced);
    }

    @Test
    public void testDebounceWithUnit_InvalidArgs() {
        Observer<String> observer = Observer.of(Arrays.asList("test"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> observer.debounce(-1, TimeUnit.MILLISECONDS));
        Assertions.assertThrows(IllegalArgumentException.class, () -> observer.debounce(100, null));
    }

    // ==================== throttleFirst(long) ====================

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
        Assertions.assertTrue(results.size() <= 5);
    }

    @Test
    public void testThrottleFirstMillis_Zero() {
        Observer<String> observer = Observer.of(Arrays.asList("a", "b"));
        Observer<String> throttled = observer.throttleFirst(0);
        Assertions.assertSame(observer, throttled);
    }

    @Test
    public void testThrottleFirstMillis_InvalidArgs() {
        Observer<String> observer = Observer.of(Arrays.asList("test"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> observer.throttleFirst(-1));
    }

    // ==================== throttleFirst(long, TimeUnit) ====================

    @Test
    public void testThrottleFirstWithUnit() throws InterruptedException {
        Observer<Integer> observer = Observer.of(Arrays.asList(1, 2, 3));
        Observer<Integer> throttled = observer.throttleFirst(100, TimeUnit.MILLISECONDS);
        Assertions.assertSame(observer, throttled);

        List<Integer> results = new ArrayList<>();
        CountDownLatch latch = new CountDownLatch(1);

        throttled.observe(results::add, e -> Assertions.fail("Unexpected error: " + e), latch::countDown);

        boolean completed = latch.await(5, TimeUnit.SECONDS);
        Assertions.assertTrue(completed);
        Assertions.assertTrue(results.size() <= 3);
    }

    @Test
    public void testThrottleFirstWithUnit_Zero() {
        Observer<String> observer = Observer.of(Arrays.asList("a"));
        Observer<String> throttled = observer.throttleFirst(0, TimeUnit.MILLISECONDS);
        Assertions.assertSame(observer, throttled);
    }

    @Test
    public void testThrottleFirstWithUnit_InvalidArgs() {
        Observer<String> observer = Observer.of(Arrays.asList("test"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> observer.throttleFirst(-1, TimeUnit.MILLISECONDS));
        Assertions.assertThrows(IllegalArgumentException.class, () -> observer.throttleFirst(100, null));
    }

    // ==================== throttleLast(long) ====================

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
        Assertions.assertTrue(results.size() <= 5);
    }

    @Test
    public void testThrottleLastMillis_Zero() {
        Observer<String> observer = Observer.of(Arrays.asList("a", "b"));
        Observer<String> throttled = observer.throttleLast(0);
        Assertions.assertSame(observer, throttled);
    }

    @Test
    public void testThrottleLastMillis_InvalidArgs() {
        Observer<String> observer = Observer.of(Arrays.asList("test"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> observer.throttleLast(-1));
    }

    // ==================== throttleLast(long, TimeUnit) ====================

    @Test
    public void testThrottleLastWithUnit() throws InterruptedException {
        Observer<Integer> observer = Observer.of(Arrays.asList(1, 2, 3));
        Observer<Integer> throttled = observer.throttleLast(100, TimeUnit.MILLISECONDS);
        Assertions.assertSame(observer, throttled);

        List<Integer> results = new ArrayList<>();
        CountDownLatch latch = new CountDownLatch(1);

        throttled.observe(results::add, e -> Assertions.fail("Unexpected error: " + e), latch::countDown);

        boolean completed = latch.await(5, TimeUnit.SECONDS);
        Assertions.assertTrue(completed);
        Assertions.assertTrue(results.size() <= 3);
    }

    @Test
    public void testThrottleLastWithUnit_Zero() {
        Observer<String> observer = Observer.of(Arrays.asList("a"));
        Observer<String> throttled = observer.throttleLast(0, TimeUnit.MILLISECONDS);
        Assertions.assertSame(observer, throttled);
    }

    @Test
    public void testThrottleLastWithUnit_InvalidArgs() {
        Observer<String> observer = Observer.of(Arrays.asList("test"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> observer.throttleLast(-1, TimeUnit.MILLISECONDS));
        Assertions.assertThrows(IllegalArgumentException.class, () -> observer.throttleLast(100, null));
    }

    // ==================== delay(long) ====================

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
    public void testDelayMillis_Zero() {
        Observer<String> observer = Observer.of(Arrays.asList("a", "b"));
        Observer<String> delayed = observer.delay(0);
        Assertions.assertSame(observer, delayed);
    }

    @Test
    public void testDelayMillis_InvalidArgs() {
        Observer<String> observer = Observer.of(Arrays.asList("test"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> observer.delay(-1));
    }

    @Test
    public void testDelayMillis_MultipleItems() throws InterruptedException {
        Observer<Integer> observer = Observer.of(Arrays.asList(1, 2, 3));
        Observer<Integer> delayed = observer.delay(100);

        List<Integer> results = new ArrayList<>();
        CountDownLatch latch = new CountDownLatch(1);

        long startTime = System.currentTimeMillis();
        delayed.observe(results::add, e -> Assertions.fail("Unexpected error: " + e), latch::countDown);

        boolean completed = latch.await(5, TimeUnit.SECONDS);
        long elapsedTime = System.currentTimeMillis() - startTime;

        Assertions.assertTrue(completed);
        Assertions.assertEquals(Arrays.asList(1, 2, 3), results);
        Assertions.assertTrue(elapsedTime >= 100);
    }

    // ==================== delay(long, TimeUnit) ====================

    @Test
    public void testDelayWithUnit() throws InterruptedException {
        Observer<String> observer = Observer.of(Arrays.asList("test"));
        Observer<String> delayed = observer.delay(100, TimeUnit.MILLISECONDS);
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
    public void testDelayWithUnit_Zero() {
        Observer<String> observer = Observer.of(Arrays.asList("a"));
        Observer<String> delayed = observer.delay(0, TimeUnit.MILLISECONDS);
        Assertions.assertSame(observer, delayed);
    }

    @Test
    public void testDelayWithUnit_InvalidArgs() {
        Observer<String> observer = Observer.of(Arrays.asList("test"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> observer.delay(-1, TimeUnit.MILLISECONDS));
        Assertions.assertThrows(IllegalArgumentException.class, () -> observer.delay(100, null));
    }

    // ==================== timeInterval() ====================

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

        Assertions.assertEquals(Integer.valueOf(1), results.get(0).value());
        Assertions.assertEquals(Integer.valueOf(2), results.get(1).value());
        Assertions.assertEquals(Integer.valueOf(3), results.get(2).value());

        for (Timed<Integer> t : results) {
            Assertions.assertTrue(t.timestamp() >= 0);
        }
    }

    @Test
    public void testTimeInterval_SingleElement() throws InterruptedException {
        Observer<String> observer = Observer.of(Arrays.asList("only"));
        Observer<Timed<String>> timed = observer.timeInterval();

        List<Timed<String>> results = new ArrayList<>();
        CountDownLatch latch = new CountDownLatch(1);

        timed.observe(results::add, e -> Assertions.fail("Unexpected error: " + e), latch::countDown);

        boolean completed = latch.await(5, TimeUnit.SECONDS);
        Assertions.assertTrue(completed);
        Assertions.assertEquals(1, results.size());
        Assertions.assertEquals("only", results.get(0).value());
    }

    // ==================== timestamp() ====================

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

        Assertions.assertEquals("a", results.get(0).value());
        Assertions.assertEquals("b", results.get(1).value());

        for (Timed<String> t : results) {
            Assertions.assertTrue(t.timestamp() >= beforeTime);
            Assertions.assertTrue(t.timestamp() <= afterTime);
        }
    }

    @Test
    public void testTimestamp_SingleElement() throws InterruptedException {
        Observer<String> observer = Observer.of(Arrays.asList("single"));
        Observer<Timed<String>> timed = observer.timestamp();

        List<Timed<String>> results = new ArrayList<>();
        CountDownLatch latch = new CountDownLatch(1);

        timed.observe(results::add, e -> Assertions.fail("Unexpected error: " + e), latch::countDown);

        boolean completed = latch.await(5, TimeUnit.SECONDS);
        Assertions.assertTrue(completed);
        Assertions.assertEquals(1, results.size());
        Assertions.assertEquals("single", results.get(0).value());
    }

    @Test
    public void testTimestamp_TimestampsNonDecreasing() throws InterruptedException {
        Observer<Integer> observer = Observer.of(Arrays.asList(1, 2, 3, 4, 5));
        Observer<Timed<Integer>> timed = observer.timestamp();

        List<Timed<Integer>> results = new ArrayList<>();
        CountDownLatch latch = new CountDownLatch(1);

        timed.observe(results::add, e -> Assertions.fail("Unexpected error: " + e), latch::countDown);

        boolean completed = latch.await(5, TimeUnit.SECONDS);
        Assertions.assertTrue(completed);

        for (int i = 1; i < results.size(); i++) {
            Assertions.assertTrue(results.get(i).timestamp() >= results.get(i - 1).timestamp());
        }
    }

    // ==================== skip(long) ====================

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
    public void testSkip_Zero() throws InterruptedException {
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
    public void testSkip_InvalidArgs() {
        Observer<String> observer = Observer.of(Arrays.asList("test"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> observer.skip(-1));
    }

    @Test
    public void testSkip_MoreThanAvailable() throws InterruptedException {
        Observer<Integer> observer = Observer.of(Arrays.asList(1, 2, 3));
        Observer<Integer> skipped = observer.skip(10);

        List<Integer> results = new ArrayList<>();
        CountDownLatch latch = new CountDownLatch(1);

        skipped.observe(results::add, e -> Assertions.fail("Unexpected error: " + e), latch::countDown);

        boolean completed = latch.await(5, TimeUnit.SECONDS);
        Assertions.assertTrue(completed);
        Assertions.assertTrue(results.isEmpty());
    }

    @Test
    public void testSkip_ExactCount() throws InterruptedException {
        Observer<Integer> observer = Observer.of(Arrays.asList(1, 2, 3));
        Observer<Integer> skipped = observer.skip(3);

        List<Integer> results = new ArrayList<>();
        CountDownLatch latch = new CountDownLatch(1);

        skipped.observe(results::add, e -> Assertions.fail("Unexpected error: " + e), latch::countDown);

        boolean completed = latch.await(5, TimeUnit.SECONDS);
        Assertions.assertTrue(completed);
        Assertions.assertTrue(results.isEmpty());
    }

    @Test
    public void testSkip_One() throws InterruptedException {
        Observer<String> observer = Observer.of(Arrays.asList("first", "second", "third"));
        Observer<String> skipped = observer.skip(1);

        List<String> results = new ArrayList<>();
        CountDownLatch latch = new CountDownLatch(1);

        skipped.observe(results::add, e -> Assertions.fail("Unexpected error: " + e), latch::countDown);

        boolean completed = latch.await(5, TimeUnit.SECONDS);
        Assertions.assertTrue(completed);
        Assertions.assertEquals(Arrays.asList("second", "third"), results);
    }

    // ==================== limit(long) ====================

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
    public void testLimit_Zero() throws InterruptedException {
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
    public void testLimit_InvalidArgs() {
        Observer<String> observer = Observer.of(Arrays.asList("test"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> observer.limit(-1));
    }

    @Test
    public void testLimit_MoreThanAvailable() throws InterruptedException {
        Observer<Integer> observer = Observer.of(Arrays.asList(1, 2));
        Observer<Integer> limited = observer.limit(100);

        List<Integer> results = new ArrayList<>();
        CountDownLatch latch = new CountDownLatch(1);

        limited.observe(results::add, e -> Assertions.fail("Unexpected error: " + e), latch::countDown);

        boolean completed = latch.await(5, TimeUnit.SECONDS);
        Assertions.assertTrue(completed);
        Assertions.assertEquals(Arrays.asList(1, 2), results);
    }

    @Test
    public void testLimit_One() throws InterruptedException {
        Observer<String> observer = Observer.of(Arrays.asList("first", "second", "third"));
        Observer<String> limited = observer.limit(1);

        List<String> results = new ArrayList<>();
        CountDownLatch latch = new CountDownLatch(1);

        limited.observe(results::add, e -> Assertions.fail("Unexpected error: " + e), latch::countDown);

        boolean completed = latch.await(5, TimeUnit.SECONDS);
        Assertions.assertTrue(completed);
        Assertions.assertEquals(Arrays.asList("first"), results);
    }

    // ==================== distinct() ====================

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

    @Test
    public void testDistinct_AllSame() throws InterruptedException {
        Observer<Integer> observer = Observer.of(Arrays.asList(1, 1, 1));
        Observer<Integer> distinct = observer.distinct();

        List<Integer> results = new ArrayList<>();
        CountDownLatch latch = new CountDownLatch(1);

        distinct.observe(results::add, e -> Assertions.fail("Unexpected error: " + e), latch::countDown);

        boolean completed = latch.await(5, TimeUnit.SECONDS);
        Assertions.assertTrue(completed);
        Assertions.assertEquals(Arrays.asList(1), results);
    }

    @Test
    public void testDistinct_AlreadyUnique() throws InterruptedException {
        Observer<Integer> observer = Observer.of(Arrays.asList(1, 2, 3, 4));
        Observer<Integer> distinct = observer.distinct();

        List<Integer> results = new ArrayList<>();
        CountDownLatch latch = new CountDownLatch(1);

        distinct.observe(results::add, e -> Assertions.fail("Unexpected error: " + e), latch::countDown);

        boolean completed = latch.await(5, TimeUnit.SECONDS);
        Assertions.assertTrue(completed);
        Assertions.assertEquals(Arrays.asList(1, 2, 3, 4), results);
    }

    @Test
    public void testDistinct_Strings() throws InterruptedException {
        Observer<String> observer = Observer.of(Arrays.asList("a", "b", "a", "c", "b"));
        Observer<String> distinct = observer.distinct();

        List<String> results = new ArrayList<>();
        CountDownLatch latch = new CountDownLatch(1);

        distinct.observe(results::add, e -> Assertions.fail("Unexpected error: " + e), latch::countDown);

        boolean completed = latch.await(5, TimeUnit.SECONDS);
        Assertions.assertTrue(completed);
        Assertions.assertEquals(Arrays.asList("a", "b", "c"), results);
    }

    // ==================== distinctBy(Function) ====================

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

    @Test
    public void testDistinctBy_FirstCharacter() throws InterruptedException {
        Observer<String> observer = Observer.of(Arrays.asList("apple", "banana", "apricot", "blueberry", "cherry"));
        Observer<String> distinct = observer.distinctBy(s -> s.charAt(0));

        List<String> results = new ArrayList<>();
        CountDownLatch latch = new CountDownLatch(1);

        distinct.observe(results::add, e -> Assertions.fail("Unexpected error: " + e), latch::countDown);

        boolean completed = latch.await(5, TimeUnit.SECONDS);
        Assertions.assertTrue(completed);
        Assertions.assertEquals(Arrays.asList("apple", "banana", "cherry"), results);
    }

    // ==================== filter(Predicate) ====================

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

    @Test
    public void testFilter_NoneMatch() throws InterruptedException {
        Observer<Integer> observer = Observer.of(Arrays.asList(1, 3, 5));
        Observer<Integer> filtered = observer.filter(i -> i % 2 == 0);

        List<Integer> results = new ArrayList<>();
        CountDownLatch latch = new CountDownLatch(1);

        filtered.observe(results::add, e -> Assertions.fail("Unexpected error: " + e), latch::countDown);

        boolean completed = latch.await(5, TimeUnit.SECONDS);
        Assertions.assertTrue(completed);
        Assertions.assertTrue(results.isEmpty());
    }

    @Test
    public void testFilter_AllMatch() throws InterruptedException {
        Observer<Integer> observer = Observer.of(Arrays.asList(2, 4, 6));
        Observer<Integer> filtered = observer.filter(i -> i % 2 == 0);

        List<Integer> results = new ArrayList<>();
        CountDownLatch latch = new CountDownLatch(1);

        filtered.observe(results::add, e -> Assertions.fail("Unexpected error: " + e), latch::countDown);

        boolean completed = latch.await(5, TimeUnit.SECONDS);
        Assertions.assertTrue(completed);
        Assertions.assertEquals(Arrays.asList(2, 4, 6), results);
    }

    @Test
    public void testFilter_StringPredicate() throws InterruptedException {
        Observer<String> observer = Observer.of(Arrays.asList("apple", "banana", "cherry", "date"));
        Observer<String> filtered = observer.filter(s -> s.length() > 5);

        List<String> results = new ArrayList<>();
        CountDownLatch latch = new CountDownLatch(1);

        filtered.observe(results::add, e -> Assertions.fail("Unexpected error: " + e), latch::countDown);

        boolean completed = latch.await(5, TimeUnit.SECONDS);
        Assertions.assertTrue(completed);
        Assertions.assertEquals(Arrays.asList("banana", "cherry"), results);
    }

    // ==================== map(Function) ====================

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

    @Test
    public void testMap_TypeConversion() throws InterruptedException {
        Observer<String> observer = Observer.of(Arrays.asList("1", "2", "3"));
        Observer<Integer> mapped = observer.map(Integer::parseInt);

        List<Integer> results = new ArrayList<>();
        CountDownLatch latch = new CountDownLatch(1);

        mapped.observe(results::add, e -> Assertions.fail("Unexpected error: " + e), latch::countDown);

        boolean completed = latch.await(5, TimeUnit.SECONDS);
        Assertions.assertTrue(completed);
        Assertions.assertEquals(Arrays.asList(1, 2, 3), results);
    }

    @Test
    public void testMap_IdentityTransform() throws InterruptedException {
        Observer<String> observer = Observer.of(Arrays.asList("a", "b", "c"));
        Observer<String> mapped = observer.map(s -> s);

        List<String> results = new ArrayList<>();
        CountDownLatch latch = new CountDownLatch(1);

        mapped.observe(results::add, e -> Assertions.fail("Unexpected error: " + e), latch::countDown);

        boolean completed = latch.await(5, TimeUnit.SECONDS);
        Assertions.assertTrue(completed);
        Assertions.assertEquals(Arrays.asList("a", "b", "c"), results);
    }

    @Test
    public void testMap_ToUpperCase() throws InterruptedException {
        Observer<String> observer = Observer.of(Arrays.asList("hello", "world"));
        Observer<String> mapped = observer.map(String::toUpperCase);

        List<String> results = new ArrayList<>();
        CountDownLatch latch = new CountDownLatch(1);

        mapped.observe(results::add, e -> Assertions.fail("Unexpected error: " + e), latch::countDown);

        boolean completed = latch.await(5, TimeUnit.SECONDS);
        Assertions.assertTrue(completed);
        Assertions.assertEquals(Arrays.asList("HELLO", "WORLD"), results);
    }

    // ==================== flatMap(Function) ====================

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
    public void testFlatMap_Empty() throws InterruptedException {
        Observer<Integer> observer = Observer.of(Arrays.asList(1, 2));
        Observer<String> flatMapped = observer.flatMap(i -> Collections.emptyList());

        List<String> results = new ArrayList<>();
        CountDownLatch latch = new CountDownLatch(1);

        flatMapped.observe(results::add, e -> Assertions.fail("Unexpected error: " + e), latch::countDown);

        boolean completed = latch.await(5, TimeUnit.SECONDS);
        Assertions.assertTrue(completed);
        Assertions.assertTrue(results.isEmpty());
    }

    @Test
    public void testFlatMap_SingleElement() throws InterruptedException {
        Observer<Integer> observer = Observer.of(Arrays.asList(1));
        Observer<String> flatMapped = observer.flatMap(i -> Arrays.asList("x", "y", "z"));

        List<String> results = new ArrayList<>();
        CountDownLatch latch = new CountDownLatch(1);

        flatMapped.observe(results::add, e -> Assertions.fail("Unexpected error: " + e), latch::countDown);

        boolean completed = latch.await(5, TimeUnit.SECONDS);
        Assertions.assertTrue(completed);
        Assertions.assertEquals(Arrays.asList("x", "y", "z"), results);
    }

    @Test
    public void testFlatMap_MixedSizes() throws InterruptedException {
        Observer<Integer> observer = Observer.of(Arrays.asList(1, 2, 3));
        Observer<Integer> flatMapped = observer.flatMap(i -> {
            if (i == 1)
                return Arrays.asList(10);
            if (i == 2)
                return Arrays.asList(20, 21, 22);
            return Collections.emptyList();
        });

        List<Integer> results = new ArrayList<>();
        CountDownLatch latch = new CountDownLatch(1);

        flatMapped.observe(results::add, e -> Assertions.fail("Unexpected error: " + e), latch::countDown);

        boolean completed = latch.await(5, TimeUnit.SECONDS);
        Assertions.assertTrue(completed);
        Assertions.assertEquals(Arrays.asList(10, 20, 21, 22), results);
    }

    // ==================== buffer(long, TimeUnit) ====================

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

        for (List<Long> buffer : results) {
            Assertions.assertTrue(buffer.size() > 0);
        }
    }

    @Test
    public void testBufferTime_InvalidArgs() {
        Observer<String> observer = Observer.of(Arrays.asList("test"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> observer.buffer(0, TimeUnit.MILLISECONDS));
        Assertions.assertThrows(IllegalArgumentException.class, () -> observer.buffer(-1, TimeUnit.MILLISECONDS));
        Assertions.assertThrows(IllegalArgumentException.class, () -> observer.buffer(100, null));
    }

    // ==================== buffer(long, TimeUnit, int) ====================

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

        Assertions.assertTrue(results.size() >= 2);
    }

    @Test
    public void testBufferTimeAndCount_InvalidArgs() {
        Observer<String> observer = Observer.of(Arrays.asList("test"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> observer.buffer(100, TimeUnit.MILLISECONDS, 0));
        Assertions.assertThrows(IllegalArgumentException.class, () -> observer.buffer(100, TimeUnit.MILLISECONDS, -1));
    }

    // ==================== buffer(long, long, TimeUnit) ====================

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
    public void testBufferTimeSkip_InvalidArgs() {
        Observer<String> observer = Observer.of(Arrays.asList("test"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> observer.buffer(0, 100, TimeUnit.MILLISECONDS));
        Assertions.assertThrows(IllegalArgumentException.class, () -> observer.buffer(100, 0, TimeUnit.MILLISECONDS));
        Assertions.assertThrows(IllegalArgumentException.class, () -> observer.buffer(-1, 100, TimeUnit.MILLISECONDS));
        Assertions.assertThrows(IllegalArgumentException.class, () -> observer.buffer(100, -1, TimeUnit.MILLISECONDS));
        Assertions.assertThrows(IllegalArgumentException.class, () -> observer.buffer(100, 200, null));
    }

    // ==================== buffer(long, long, TimeUnit, int) ====================

    @Test
    public void testBufferTimeSkipAndCount() throws InterruptedException {
        Observer<Long> observer = Observer.interval(50, TimeUnit.MILLISECONDS);
        Observer<List<Long>> buffered = observer.buffer(100, 200, TimeUnit.MILLISECONDS, 5);
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
    public void testBufferTimeSkipAndCount_InvalidArgs() {
        Observer<String> observer = Observer.of(Arrays.asList("test"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> observer.buffer(100, 200, TimeUnit.MILLISECONDS, 0));
        Assertions.assertThrows(IllegalArgumentException.class, () -> observer.buffer(100, 200, TimeUnit.MILLISECONDS, -1));
    }

    // ==================== observe(Consumer) ====================

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
    public void testObserveWithAction_MultipleItems() throws InterruptedException {
        Observer<Integer> observer = Observer.of(Arrays.asList(1, 2, 3));
        List<Integer> results = new ArrayList<>();
        CountDownLatch latch = new CountDownLatch(3);

        observer.observe(v -> {
            results.add(v);
            latch.countDown();
        });

        boolean completed = latch.await(5, TimeUnit.SECONDS);
        Assertions.assertTrue(completed);
        Assertions.assertEquals(Arrays.asList(1, 2, 3), results);
    }

    // ==================== observe(Consumer, Consumer) ====================

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
    public void testObserveWithActionAndError_NoError() throws InterruptedException {
        Observer<String> observer = Observer.of(Arrays.asList("ok"));
        List<String> results = new ArrayList<>();
        AtomicBoolean errorCalled = new AtomicBoolean(false);
        CountDownLatch latch = new CountDownLatch(1);

        observer.observe(v -> {
            results.add(v);
            latch.countDown();
        }, e -> errorCalled.set(true));

        boolean completed = latch.await(5, TimeUnit.SECONDS);
        Assertions.assertTrue(completed);
        Assertions.assertFalse(errorCalled.get());
        Assertions.assertEquals(Arrays.asList("ok"), results);
    }

    // ==================== observe(Consumer, Consumer, Runnable) ====================

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

    @Test
    public void testObserveWithActionErrorComplete_EmptyCollection() throws InterruptedException {
        Observer<String> observer = Observer.of(Collections.emptyList());

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
        Assertions.assertTrue(results.isEmpty());
    }

    // ==================== chained operations ====================

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

    @Test
    public void testChainedOperations_DistinctAndFilter() throws InterruptedException {
        Observer<Integer> observer = Observer.of(Arrays.asList(1, 2, 2, 3, 3, 4, 5));

        List<Integer> results = new ArrayList<>();
        CountDownLatch latch = new CountDownLatch(1);

        observer.distinct().filter(i -> i > 2).observe(results::add, e -> Assertions.fail("Unexpected error: " + e), latch::countDown);

        boolean completed = latch.await(5, TimeUnit.SECONDS);
        Assertions.assertTrue(completed);
        Assertions.assertEquals(Arrays.asList(3, 4, 5), results);
    }

    @Test
    public void testChainedOperations_SkipAndLimit() throws InterruptedException {
        Observer<Integer> observer = Observer.of(Arrays.asList(1, 2, 3, 4, 5));

        List<Integer> results = new ArrayList<>();
        CountDownLatch latch = new CountDownLatch(1);

        observer.skip(1).limit(2).observe(results::add, e -> Assertions.fail("Unexpected error: " + e), latch::countDown);

        boolean completed = latch.await(5, TimeUnit.SECONDS);
        Assertions.assertTrue(completed);
        Assertions.assertEquals(Arrays.asList(2, 3), results);
    }

    @Test
    public void testChainedOperations_FilterAndMap() throws InterruptedException {
        Observer<Integer> observer = Observer.of(Arrays.asList(1, 2, 3, 4, 5));

        List<String> results = new ArrayList<>();
        CountDownLatch latch = new CountDownLatch(1);

        observer.filter(i -> i > 2).map(i -> "num:" + i).observe(results::add, e -> Assertions.fail("Unexpected error: " + e), latch::countDown);

        boolean completed = latch.await(5, TimeUnit.SECONDS);
        Assertions.assertTrue(completed);
        Assertions.assertEquals(Arrays.asList("num:3", "num:4", "num:5"), results);
    }

    @Test
    public void testChainedOperations_DistinctByAndLimit() throws InterruptedException {
        Observer<String> observer = Observer.of(Arrays.asList("apple", "avocado", "banana", "blueberry", "cherry"));

        List<String> results = new ArrayList<>();
        CountDownLatch latch = new CountDownLatch(1);

        observer.distinctBy(s -> s.charAt(0)).limit(2).observe(results::add, e -> Assertions.fail("Unexpected error: " + e), latch::countDown);

        boolean completed = latch.await(5, TimeUnit.SECONDS);
        Assertions.assertTrue(completed);
        Assertions.assertEquals(Arrays.asList("apple", "banana"), results);
    }

    @Test
    public void testChainedOperations_MapAndFlatMap() throws InterruptedException {
        Observer<Integer> observer = Observer.of(Arrays.asList(1, 2));

        List<String> results = new ArrayList<>();
        CountDownLatch latch = new CountDownLatch(1);

        observer.map(i -> i * 10)
                .flatMap(i -> Arrays.asList(i + "a", i + "b"))
                .observe(results::add, e -> Assertions.fail("Unexpected error: " + e), latch::countDown);

        boolean completed = latch.await(5, TimeUnit.SECONDS);
        Assertions.assertTrue(completed);
        Assertions.assertEquals(Arrays.asList("10a", "10b", "20a", "20b"), results);
    }

    @Test
    public void testChainedOperations_SkipFilterDistinctLimit() throws InterruptedException {
        Observer<Integer> observer = Observer.of(Arrays.asList(1, 2, 3, 3, 4, 5, 5, 6, 7));

        List<Integer> results = new ArrayList<>();
        CountDownLatch latch = new CountDownLatch(1);

        observer.skip(1).filter(i -> i % 2 == 0).distinct().limit(2).observe(results::add, e -> Assertions.fail("Unexpected error: " + e), latch::countDown);

        boolean completed = latch.await(5, TimeUnit.SECONDS);
        Assertions.assertTrue(completed);
        Assertions.assertEquals(Arrays.asList(2, 4), results);
    }

    // ==================== legacy test ====================

    @Test
    public void test_0() {
        BlockingQueue<String> queue = new ArrayBlockingQueue<>(100);
        Observer.of(queue).observe(Fn.println(), Exception::printStackTrace, () -> N.println("completed"));

        queue.add("ab");
        queue.add("cc");
        queue.add("dd");

        Observer.timer(10).observe(Fn.println(), Exception::printStackTrace, () -> N.println("completed"));

        Observer.interval(100).observe(Fn.println(), Exception::printStackTrace, () -> N.println("completed"));

        N.sleep(3000);
        assertNotNull(queue);
    }
}

package com.landawn.abacus.util;

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
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

public class ObserverTest extends TestBase {

    @Test
    public void testSubscriptionActionRunsOnlyAfterSubscription() throws InterruptedException {
        Observer<Integer> observer = Observer.of(List.of(1));
        AtomicInteger initialized = new AtomicInteger();
        CountDownLatch completed = new CountDownLatch(1);
        Assertions.assertThrows(IllegalArgumentException.class, () -> observer.addSubscriptionAction(null));
        observer.addSubscriptionAction(initialized::incrementAndGet);
        Assertions.assertEquals(0, initialized.get());

        observer.observe(value -> Assertions.assertEquals(1, initialized.get()), error -> Assertions.fail(error), completed::countDown);

        Assertions.assertTrue(completed.await(5, TimeUnit.SECONDS));
        Assertions.assertEquals(1, initialized.get());
        Assertions.assertThrows(IllegalStateException.class, () -> observer.addSubscriptionAction(initialized::incrementAndGet));
    }

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
        Assertions.assertEquals(Arrays.asList(1, 2, 3), collect(Observer.of(Arrays.asList(1, 2, 3))));
        Assertions.assertTrue(collect(Observer.of(Collections.emptyList())).isEmpty());
        Assertions.assertTrue(collect(Observer.of((Collection<String>) null)).isEmpty());
        Assertions.assertEquals(Collections.singletonList("only"), collect(Observer.of(Collections.singletonList("only"))));
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
    public void test_0() throws InterruptedException {
        final BlockingQueue<String> queue = new ArrayBlockingQueue<>(100);
        final List<String> queueResults = new ArrayList<>();
        final List<Throwable> errors = Collections.synchronizedList(new ArrayList<>());
        final CountDownLatch queueCompleted = new CountDownLatch(1);
        Observer.of(queue).observe(queueResults::add, e -> {
            errors.add(e);
            queueCompleted.countDown();
        }, queueCompleted::countDown);

        queue.add("ab");
        queue.add("cc");
        queue.add("dd");
        Observer.complete(queue);

        Assertions.assertTrue(queueCompleted.await(5, TimeUnit.SECONDS));
        Assertions.assertEquals(Arrays.asList("ab", "cc", "dd"), queueResults);

        final List<Long> timerResults = new ArrayList<>();
        final CountDownLatch timerCompleted = new CountDownLatch(1);
        Observer.timer(10).observe(timerResults::add, e -> {
            errors.add(e);
            timerCompleted.countDown();
        }, timerCompleted::countDown);

        Assertions.assertTrue(timerCompleted.await(5, TimeUnit.SECONDS));
        Assertions.assertEquals(Collections.singletonList(0L), timerResults);

        final List<Long> intervalResults = new ArrayList<>();
        final CountDownLatch intervalCompleted = new CountDownLatch(1);
        Observer.interval(100).limit(3).observe(intervalResults::add, e -> {
            errors.add(e);
            intervalCompleted.countDown();
        }, intervalCompleted::countDown);

        Assertions.assertTrue(intervalCompleted.await(5, TimeUnit.SECONDS));
        Assertions.assertEquals(Arrays.asList(0L, 1L, 2L), intervalResults);
        Assertions.assertTrue(errors.isEmpty(), errors::toString);
    }

    // ==================== timer(long) / timer(long, TimeUnit) ====================

    @Test
    public void testTimer() throws InterruptedException {
        assertTimerEmitsZeroAfterDelay(Observer.timer(100), 100);
        assertTimerEmitsZeroAfterDelay(Observer.timer(200, TimeUnit.MILLISECONDS), 200);
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

    @Test
    public void testTimerLimitZeroDoesNotEmitOrWaitForDelay() throws InterruptedException {
        List<Long> results = new ArrayList<>();
        CountDownLatch completed = new CountDownLatch(1);

        Observer.timer(1, TimeUnit.DAYS).limit(0).observe(results::add, e -> Assertions.fail("Unexpected error: " + e), completed::countDown);

        Assertions.assertTrue(completed.await(5, TimeUnit.SECONDS));
        Assertions.assertTrue(results.isEmpty());
    }

    @Test
    public void testIntervalLimitZeroDoesNotEmitOrWaitForInitialDelay() throws InterruptedException {
        List<Long> results = new ArrayList<>();
        CountDownLatch completed = new CountDownLatch(1);

        Observer.interval(1, 1, TimeUnit.DAYS).limit(0).observe(results::add, e -> Assertions.fail("Unexpected error: " + e), completed::countDown);

        Assertions.assertTrue(completed.await(5, TimeUnit.SECONDS));
        Assertions.assertTrue(results.isEmpty());
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

    @Test
    public void testDebounceMillis_Zero() {
        Observer<String> observer = Observer.of(Arrays.asList("a", "b"));
        Observer<String> debounced = observer.debounce(0);
        Assertions.assertSame(observer, debounced);
    }

    @Test
    public void testDebounceWithUnit_Zero() {
        Observer<String> observer = Observer.of(Arrays.asList("a"));
        Observer<String> debounced = observer.debounce(0, TimeUnit.MILLISECONDS);
        Assertions.assertSame(observer, debounced);
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
        Assertions.assertEquals(Collections.singletonList(5), results);
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
        Assertions.assertEquals(Collections.singletonList(3), results);
    }

    @Test
    public void testDebounceDoesNotEmitAfterError() throws InterruptedException {
        final List<String> events = Collections.synchronizedList(new ArrayList<>());
        final CountDownLatch error = new CountDownLatch(1);

        Observer.of(Arrays.asList(1, 2)).map(value -> {
            if (value == 2) {
                throw new IllegalStateException("boom");
            }

            return value;
        }).debounce(50, TimeUnit.MILLISECONDS).observe(value -> events.add("next:" + value), ex -> {
            events.add("error");
            error.countDown();
        }, () -> events.add("complete"));

        Assertions.assertTrue(error.await(5, TimeUnit.SECONDS));
        Thread.sleep(100);
        Assertions.assertEquals(Collections.singletonList("error"), events);
    }

    @Test
    public void testDebounceRestartsQuietPeriodForEveryItem() throws InterruptedException {
        final BlockingQueue<Integer> queue = new LinkedBlockingQueue<>();
        final CountDownLatch firstSeen = new CountDownLatch(1);
        final CountDownLatch secondSeen = new CountDownLatch(1);
        final CountDownLatch next = new CountDownLatch(1);
        final CountDownLatch completed = new CountDownLatch(1);
        final List<Integer> results = Collections.synchronizedList(new ArrayList<>());

        Observer.of(queue).map(value -> {
            (value == 1 ? firstSeen : secondSeen).countDown();
            return value;
        }).debounce(200, TimeUnit.MILLISECONDS).observe(value -> {
            results.add(value);
            next.countDown();
        }, e -> Assertions.fail("Unexpected error: " + e), completed::countDown);

        queue.offer(1);
        Assertions.assertTrue(firstSeen.await(5, TimeUnit.SECONDS));
        Thread.sleep(80);
        queue.offer(2);
        Assertions.assertTrue(secondSeen.await(5, TimeUnit.SECONDS));

        Thread.sleep(150);
        Assertions.assertTrue(results.isEmpty(), "The first item's deadline must not emit the second item early");
        Assertions.assertTrue(next.await(5, TimeUnit.SECONDS));

        Observer.complete(queue);
        Assertions.assertTrue(completed.await(5, TimeUnit.SECONDS));
        Assertions.assertEquals(Collections.singletonList(2), results);
    }

    @Test
    public void testDebounceWithUnit_InvalidArgs() {
        Observer<String> observer = Observer.of(Arrays.asList("test"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> observer.debounce(-1, TimeUnit.MILLISECONDS));
        Assertions.assertThrows(IllegalArgumentException.class, () -> observer.debounce(100, null));
    }

    @Test
    public void testThrottleFirstMillis_Zero() {
        Observer<String> observer = Observer.of(Arrays.asList("a", "b"));
        Observer<String> throttled = observer.throttleFirst(0);
        Assertions.assertSame(observer, throttled);
    }

    @Test
    public void testThrottleFirstWithUnit_Zero() {
        Observer<String> observer = Observer.of(Arrays.asList("a"));
        Observer<String> throttled = observer.throttleFirst(0, TimeUnit.MILLISECONDS);
        Assertions.assertSame(observer, throttled);
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
        Assertions.assertEquals(Collections.singletonList(1), results);
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
        Assertions.assertEquals(Collections.singletonList(1), results);
    }

    @Test
    public void testThrottleFirstWithUnit_InvalidArgs() {
        Observer<String> observer = Observer.of(Arrays.asList("test"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> observer.throttleFirst(-1, TimeUnit.MILLISECONDS));
        Assertions.assertThrows(IllegalArgumentException.class, () -> observer.throttleFirst(100, null));
    }

    @Test
    public void testThrottleLastMillis_Zero() {
        Observer<String> observer = Observer.of(Arrays.asList("a", "b"));
        Observer<String> throttled = observer.throttleLast(0);
        Assertions.assertSame(observer, throttled);
    }

    @Test
    public void testThrottleLastWithUnit_Zero() {
        Observer<String> observer = Observer.of(Arrays.asList("a"));
        Observer<String> throttled = observer.throttleLast(0, TimeUnit.MILLISECONDS);
        Assertions.assertSame(observer, throttled);
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
        Assertions.assertTrue(results.isEmpty());
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
        Assertions.assertTrue(results.isEmpty());
    }

    @Test
    public void testThrottleLastWithUnit_InvalidArgs() {
        Observer<String> observer = Observer.of(Arrays.asList("test"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> observer.throttleLast(-1, TimeUnit.MILLISECONDS));
        Assertions.assertThrows(IllegalArgumentException.class, () -> observer.throttleLast(100, null));
    }

    @Test
    public void testDelayMillis_Zero() {
        Observer<String> observer = Observer.of(Arrays.asList("a", "b"));
        Observer<String> delayed = observer.delay(0);
        Assertions.assertSame(observer, delayed);
    }

    @Test
    public void testDelayWithUnit_Zero() {
        Observer<String> observer = Observer.of(Arrays.asList("a"));
        Observer<String> delayed = observer.delay(0, TimeUnit.MILLISECONDS);
        Assertions.assertSame(observer, delayed);
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
    public void testLimitZeroDoesNotConsumeUpstreamElement() throws InterruptedException {
        AtomicInteger nextCalls = new AtomicInteger();
        Iterator<Integer> iterator = new Iterator<>() {
            private int next = 1;

            @Override
            public boolean hasNext() {
                return next <= 3;
            }

            @Override
            public Integer next() {
                nextCalls.incrementAndGet();
                return next++;
            }
        };

        List<Integer> results = new ArrayList<>();
        CountDownLatch latch = new CountDownLatch(1);

        Observer.of(iterator).limit(0).observe(results::add, e -> Assertions.fail("Unexpected error: " + e), latch::countDown);

        Assertions.assertTrue(latch.await(5, TimeUnit.SECONDS));
        Assertions.assertTrue(results.isEmpty());
        Assertions.assertEquals(0, nextCalls.get());
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

    @Test
    public void testLimitDoesNotConsumeElementAfterLimit() throws InterruptedException {
        AtomicInteger nextCalls = new AtomicInteger();
        Iterator<Integer> iterator = new Iterator<>() {
            private int next = 1;

            @Override
            public boolean hasNext() {
                return next <= 5;
            }

            @Override
            public Integer next() {
                nextCalls.incrementAndGet();
                return next++;
            }
        };

        List<Integer> results = new ArrayList<>();
        CountDownLatch latch = new CountDownLatch(1);

        Observer.of(iterator).limit(3).observe(results::add, e -> Assertions.fail("Unexpected error: " + e), latch::countDown);

        Assertions.assertTrue(latch.await(5, TimeUnit.SECONDS));
        Assertions.assertEquals(Arrays.asList(1, 2, 3), results);
        Assertions.assertEquals(3, nextCalls.get());
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
            if (i == 1) {
                return Arrays.asList(10);
            }
            if (i == 2) {
                return Arrays.asList(20, 21, 22);
            }
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

    @Test
    public void testBufferDoesNotScheduleUntilSubscribed() {
        final int scheduledTaskCount = Observer.schedulerForIntermediateOp.getQueue().size();
        final Observer<List<Integer>> fixed = Observer.of(Arrays.asList(1, 2, 3)).buffer(1, TimeUnit.DAYS);
        final Observer<List<Integer>> sliding = Observer.of(Arrays.asList(1, 2, 3)).buffer(1, 1, TimeUnit.DAYS);

        Assertions.assertTrue(fixed.scheduledFutures.isEmpty());
        Assertions.assertTrue(sliding.scheduledFutures.isEmpty());
        Assertions.assertEquals(scheduledTaskCount, Observer.schedulerForIntermediateOp.getQueue().size());
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

    @Test
    public void testBufferFlushesPartialBufferBeforeCompletion() throws InterruptedException {
        final List<List<Integer>> results = new ArrayList<>();
        final CountDownLatch completed = new CountDownLatch(1);

        Observer.of(Arrays.asList(1, 2))
                .buffer(1, TimeUnit.DAYS, 10)
                .observe(list -> results.add(new ArrayList<>(list)), e -> Assertions.fail("Unexpected error: " + e), completed::countDown);

        Assertions.assertTrue(completed.await(5, TimeUnit.SECONDS));
        Assertions.assertEquals(Collections.singletonList(Arrays.asList(1, 2)), results);
    }

    @Test
    public void testBufferCompletionFlushFailureIsDeliveredAsError() throws InterruptedException {
        final List<String> terminalEvents = Collections.synchronizedList(new ArrayList<>());
        final CountDownLatch terminal = new CountDownLatch(1);

        Observer.of(Arrays.asList(1, 2)).buffer(1, TimeUnit.DAYS, 10).observe(list -> {
            throw new IllegalStateException("flush failure");
        }, e -> {
            terminalEvents.add("error:" + e.getMessage());
            terminal.countDown();
        }, () -> {
            terminalEvents.add("complete");
            terminal.countDown();
        });

        Assertions.assertTrue(terminal.await(5, TimeUnit.SECONDS));
        Assertions.assertEquals(Collections.singletonList("error:flush failure"), terminalEvents);
    }

    @Test
    public void testBufferScheduledDeliveryFailureIsDeliveredOnce() throws InterruptedException {
        final AtomicInteger errorCount = new AtomicInteger();
        final AtomicBoolean completed = new AtomicBoolean();
        final CountDownLatch error = new CountDownLatch(1);

        Observer.interval(1, TimeUnit.MILLISECONDS).buffer(20, TimeUnit.MILLISECONDS).observe(list -> {
            throw new IllegalStateException("scheduled failure");
        }, e -> {
            errorCount.incrementAndGet();
            error.countDown();
        }, () -> completed.set(true));

        Assertions.assertTrue(error.await(5, TimeUnit.SECONDS));
        Thread.sleep(100);
        Assertions.assertEquals(1, errorCount.get());
        Assertions.assertFalse(completed.get());
    }

    @Test
    public void testBufferDiscardsPartialBufferOnError() throws InterruptedException {
        final List<String> events = Collections.synchronizedList(new ArrayList<>());
        final CountDownLatch error = new CountDownLatch(1);

        Observer.of(Arrays.asList(1, 2)).map(value -> {
            if (value == 2) {
                throw new IllegalStateException("boom");
            }

            return value;
        }).buffer(1, TimeUnit.DAYS, 10).observe(list -> events.add("next"), e -> {
            events.add("error");
            error.countDown();
        }, () -> events.add("complete"));

        Assertions.assertTrue(error.await(5, TimeUnit.SECONDS));
        Assertions.assertEquals(Collections.singletonList("error"), events);
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

        buffered.limit(2).observe(list -> {
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
    public void testBufferTimeSkipCreatesOverlappingWindows() throws InterruptedException {
        final BlockingQueue<Integer> queue = new LinkedBlockingQueue<>();
        final List<List<Integer>> results = new ArrayList<>();
        final CountDownLatch completed = new CountDownLatch(1);

        Observer.of(queue)
                .buffer(10_000, 50, TimeUnit.MILLISECONDS)
                .observe(list -> results.add(new ArrayList<>(list)), e -> Assertions.fail("Unexpected error: " + e), completed::countDown);

        queue.offer(1);
        Thread.sleep(250);
        queue.offer(2);
        Observer.complete(queue);

        Assertions.assertTrue(completed.await(5, TimeUnit.SECONDS));
        Assertions.assertTrue(results.stream().filter(window -> window.contains(2)).count() >= 2, "An item must be added to every overlapping active window");
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

    @Test
    public void testOverlappingBufferWindowsCompleteByIdentity() throws Exception {
        final Observer<Integer> source = Observer.of(Collections.<Integer> emptyList());
        final Observer<List<Integer>> observer = source.buffer(1, 1, TimeUnit.DAYS);
        final List<List<Integer>> results = new ArrayList<>();
        observer.dispatcher.append(new Observer.Dispatcher<>() {
            @Override
            public void onNext(final Object value) {
                results.add(new ArrayList<>((List<Integer>) value));
            }
        });

        final Object buffer = observer.dispatcher.downDispatcher;
        final java.lang.reflect.Method startWindow = buffer.getClass().getDeclaredMethod("startWindow");
        final java.lang.reflect.Method emitWindow = buffer.getClass().getDeclaredMethod("emitWindow", List.class);
        final java.lang.reflect.Field windowsField = buffer.getClass().getDeclaredField("windows");
        final java.lang.reflect.Field futuresField = buffer.getClass().getDeclaredField("windowFutures");
        startWindow.setAccessible(true);
        emitWindow.setAccessible(true);
        windowsField.setAccessible(true);
        futuresField.setAccessible(true);
        final List<java.util.concurrent.ScheduledFuture<?>> futures = new ArrayList<>();

        try {
            source.startSubscriptionActions();
            startWindow.invoke(buffer);
            final List<List<Integer>> windows = (List<List<Integer>>) windowsField.get(buffer);
            final List<Integer> firstWindow = windows.get(0);
            final List<Integer> secondWindow = windows.get(1);
            futures.addAll(((java.util.Map<List<Integer>, java.util.concurrent.ScheduledFuture<?>>) futuresField.get(buffer)).values());

            observer.dispatcher.onNext(1);
            // Scheduler threads can finish a newer window before an older thread acquires the window lock.
            emitWindow.invoke(buffer, secondWindow);
            // A stale callback must neither emit twice nor remove the still-active equal-content window.
            emitWindow.invoke(buffer, secondWindow);
            observer.dispatcher.onNext(2);
            emitWindow.invoke(buffer, firstWindow);

            Assertions.assertEquals(Arrays.asList(Arrays.asList(1), Arrays.asList(1, 2)), results);
        } finally {
            observer.dispatcher.onComplete();
            source.cancelScheduledFutures();
            for (final java.util.concurrent.ScheduledFuture<?> future : futures) {
                future.cancel(false);
            }
        }
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

        buffered.limit(2).observe(list -> {
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

    @Test
    public void testNullTransformCallbacksAreNotRejectedSynchronously() {
        Observer<String> observer = Observer.of(Arrays.asList("test"));

        Assertions.assertThrows(IllegalArgumentException.class, () -> observer.distinctBy(null));
        Assertions.assertThrows(IllegalArgumentException.class, () -> observer.filter(null));
        Assertions.assertThrows(IllegalArgumentException.class, () -> observer.map(null));
        Assertions.assertThrows(IllegalArgumentException.class, () -> observer.flatMap(null));
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

    @Test
    public void testIntervalLimitCompletesWithoutWaitingForNextPeriod() throws InterruptedException {
        final List<Long> results = new ArrayList<>();
        final CountDownLatch completed = new CountDownLatch(1);
        final AtomicInteger completionCount = new AtomicInteger();

        Observer.interval(0, 1, TimeUnit.DAYS).limit(1).observe(results::add, e -> Assertions.fail("Unexpected error: " + e), () -> {
            completionCount.incrementAndGet();
            completed.countDown();
        });

        Assertions.assertTrue(completed.await(5, TimeUnit.SECONDS));
        Assertions.assertEquals(Collections.singletonList(0L), results);
        Assertions.assertEquals(1, completionCount.get());
    }

    @Test
    public void testSubMillisecondDelayIsNotRoundedDown() {
        long totalDispatchTimeInNanos = 0;

        for (int i = 0; i < 20; i++) {
            final Observer<String> observer = Observer.of(Collections.emptyList());
            observer.delay(999_999, TimeUnit.NANOSECONDS);
            final long startTimeInNanos = System.nanoTime();
            observer.dispatcher.onNext("ignored");
            totalDispatchTimeInNanos += System.nanoTime() - startTimeInNanos;
        }

        Assertions.assertTrue(totalDispatchTimeInNanos >= 10_000_000,
                "Sub-millisecond delays must not be rounded to zero; totalDispatchTimeInNanos=" + totalDispatchTimeInNanos);
    }

    @Test
    public void testSchedulersRemoveCancelledTasksImmediately() {
        Assertions.assertTrue(Observer.schedulerForIntermediateOp.getRemoveOnCancelPolicy());
        Assertions.assertTrue(Observer.schedulerForObserveOp.getRemoveOnCancelPolicy());
    }

    @Test
    public void testLibraryOwnedExecutorsUseDaemonThreads() throws InterruptedException {
        final CountDownLatch completed = new CountDownLatch(3);
        final List<Boolean> daemonFlags = Collections.synchronizedList(new ArrayList<>());

        Observer.asyncExecutor.execute(() -> {
            daemonFlags.add(Thread.currentThread().isDaemon());
            completed.countDown();
        });
        Observer.schedulerForIntermediateOp.execute(() -> {
            daemonFlags.add(Thread.currentThread().isDaemon());
            completed.countDown();
        });
        Observer.schedulerForObserveOp.execute(() -> {
            daemonFlags.add(Thread.currentThread().isDaemon());
            completed.countDown();
        });

        Assertions.assertTrue(completed.await(5, TimeUnit.SECONDS));
        Assertions.assertEquals(3, daemonFlags.size());
        Assertions.assertTrue(daemonFlags.stream().allMatch(Boolean::booleanValue));
    }

    @Test
    public void testCompleteRejectsNullQueueSynchronously() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> Observer.complete(null));
    }

    @Test
    public void testProtectedPipelineHooksRejectNullDispatchers() {
        final Observer.Dispatcher<Object> dispatcher = new Observer.Dispatcher<>();
        Assertions.assertThrows(IllegalArgumentException.class, () -> dispatcher.append(null));

        Assertions.assertThrows(IllegalArgumentException.class, () -> new Observer<Object>((Observer.Dispatcher<Object>) null) {
            @Override
            public void observe(final java.util.function.Consumer<? super Object> action, final java.util.function.Consumer<? super Exception> onError,
                    final Runnable onComplete) {
                // No-op test implementation.
            }
        });
    }

    @Test
    public void testObserverRejectsASecondSubscription() throws InterruptedException {
        final Observer<Integer> observer = Observer.of(Collections.singletonList(1));
        final CountDownLatch completed = new CountDownLatch(1);

        observer.observe(value -> {
        }, Assertions::fail, completed::countDown);
        Assertions.assertTrue(completed.await(5, TimeUnit.SECONDS));

        Assertions.assertThrows(IllegalStateException.class, () -> observer.observe(value -> {
        }, Assertions::fail, () -> {
        }));
    }

    @Test
    public void testFlatMapStopsAccessingMappedIteratorAtDownstreamLimit() throws InterruptedException {
        final Collection<Integer> mapped = singleReadMappedCollection();
        assertFlattenedValues(Observer.of(List.of(1)).flatMap(value -> mapped).limit(1), List.of(10));
    }

    @Test
    public void testFlatMapStopsAtDownstreamLimitDuringBufferCompletion() throws InterruptedException {
        assertFlattenedValues(Observer.of(List.of(1, 2)).limit(1).buffer(1, TimeUnit.DAYS).flatMap(value -> singleReadMappedCollection()).limit(1),
                List.of(10));
    }

    @Test
    public void testFlatMapEmitsAllAcceptedValuesAfterUpstreamLimit() throws InterruptedException {
        assertFlattenedValues(Observer.of(List.of(1, 2)).limit(1).flatMap(value -> List.of(10, 20, 30)), List.of(10, 20, 30));
        assertFlattenedValues(Observer.of(List.of(1, 2, 3)).limit(2).buffer(1, TimeUnit.DAYS).flatMap(value -> value), List.of(1, 2));
    }

    @Test
    public void testFlatMapSkipsPendingWindowMappersAfterDownstreamLimit() throws Exception {
        final AtomicInteger mappedWindows = new AtomicInteger();
        final Observer<Integer> observer = Observer.of(Collections.<Integer> emptyList()).buffer(1, 1, TimeUnit.DAYS).flatMap(window -> {
            if (mappedWindows.incrementAndGet() > 1) {
                throw new IllegalStateException("Mapped a pending window after the downstream limit was reached");
            }
            return window;
        }).limit(1);
        final List<Integer> actual = new ArrayList<>();
        final List<Exception> errors = new ArrayList<>();
        final AtomicInteger completions = new AtomicInteger();
        observer.dispatcher.append(new Observer.Dispatcher<>() {
            @Override
            public void onNext(final Object value) {
                actual.add((Integer) value);
            }

            @Override
            public void onError(final Exception error) {
                errors.add(error);
            }

            @Override
            public void onComplete() {
                completions.incrementAndGet();
            }
        });

        final Object buffer = observer.dispatcher.downDispatcher;
        final java.lang.reflect.Method startWindow = buffer.getClass().getDeclaredMethod("startWindow");
        startWindow.setAccessible(true);

        try {
            observer.startSubscriptionActions();
            startWindow.invoke(buffer);
            observer.dispatcher.onNext(10);
            observer.dispatcher.onComplete();

            Assertions.assertEquals(List.of(10), actual);
            Assertions.assertEquals(1, mappedWindows.get());
            Assertions.assertTrue(errors.isEmpty(), () -> "Unexpected errors: " + errors);
            Assertions.assertEquals(1, completions.get());
        } finally {
            observer.dispatcher.onComplete();
            observer.cancelScheduledFutures();
        }
    }

    private static Collection<Integer> singleReadMappedCollection() {
        return new java.util.AbstractCollection<>() {
            @Override
            public int size() {
                return 2;
            }

            @Override
            public Iterator<Integer> iterator() {
                return new Iterator<>() {
                    private boolean consumed;

                    @Override
                    public boolean hasNext() {
                        if (consumed) {
                            throw new IllegalStateException("Mapped iterator inspected after the downstream limit was reached");
                        }
                        return true;
                    }

                    @Override
                    public Integer next() {
                        if (consumed) {
                            throw new IllegalStateException("Mapped iterator consumed after the downstream limit was reached");
                        }
                        consumed = true;
                        return 10;
                    }
                };
            }
        };
    }

    private static <T> List<T> collect(final Observer<T> observer) throws InterruptedException {
        final List<T> results = new ArrayList<>();
        final CountDownLatch latch = new CountDownLatch(1);
        observer.observe(results::add, e -> Assertions.fail("Unexpected error: " + e), latch::countDown);
        Assertions.assertTrue(latch.await(5, TimeUnit.SECONDS));
        return results;
    }

    private static void assertTimerEmitsZeroAfterDelay(final Observer<Long> observer, final long minElapsedMillis) throws InterruptedException {
        Assertions.assertNotNull(observer);
        final List<Long> results = new ArrayList<>();
        final CountDownLatch latch = new CountDownLatch(1);
        final long startTime = System.currentTimeMillis();
        observer.observe(results::add, e -> Assertions.fail("Unexpected error: " + e), latch::countDown);
        Assertions.assertTrue(latch.await(5, TimeUnit.SECONDS));
        Assertions.assertEquals(Collections.singletonList(0L), results);
        Assertions.assertTrue(System.currentTimeMillis() - startTime >= minElapsedMillis);
    }

    private static void assertFlattenedValues(final Observer<Integer> observer, final List<Integer> expected) throws InterruptedException {
        final List<Integer> actual = new ArrayList<>();
        final List<Exception> errors = new ArrayList<>();
        final CountDownLatch finished = new CountDownLatch(1);
        final AtomicInteger completions = new AtomicInteger();
        observer.observe(actual::add, error -> {
            errors.add(error);
            finished.countDown();
        }, () -> {
            completions.incrementAndGet();
            finished.countDown();
        });

        Assertions.assertTrue(finished.await(5, TimeUnit.SECONDS), "Observer did not terminate");
        Assertions.assertTrue(errors.isEmpty(), () -> "Unexpected errors: " + errors);
        Assertions.assertEquals(1, completions.get());
        Assertions.assertEquals(expected, actual);
    }
}

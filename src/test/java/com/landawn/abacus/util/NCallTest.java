package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import com.landawn.abacus.util.Tuple.Tuple2;
import com.landawn.abacus.util.Tuple.Tuple3;
import com.landawn.abacus.util.Tuple.Tuple4;
import com.landawn.abacus.util.Tuple.Tuple5;

public class NCallTest extends NTestSupport {

    @Test
    @Timeout(15)
    public void testCallAsync() throws InterruptedException {
        List<Callable<Integer>> tasks = new ArrayList<>();
        CountDownLatch latch = new CountDownLatch(2);
        tasks.add(() -> {
            Thread.sleep(20);
            latch.countDown();
            return 1;
        });
        tasks.add(() -> {
            Thread.sleep(10);
            latch.countDown();
            return 2;
        });

        ObjIterator<Integer> iter = N.callAsync(tasks, executorService);
        List<Integer> results = iteratorToList(iter);
        assertTrue(latch.await(500, TimeUnit.MILLISECONDS));
        assertEquals(2, results.size());
        assertTrue(results.containsAll(Arrays.asList(1, 2)));

        List<Callable<Integer>> ordered = new ArrayList<>();
        CountDownLatch[] releases = new CountDownLatch[8];
        CountDownLatch finished = new CountDownLatch(releases.length);
        for (int i = 0; i < releases.length; i++) {
            final int value = (releases.length - i) * 5;
            final CountDownLatch release = new CountDownLatch(1);
            releases[i] = release;
            ordered.add(() -> {
                try {
                    release.await();
                    return value;
                } finally {
                    finished.countDown();
                }
            });
        }
        try {
            ObjIterator<Integer> completionOrder = N.callAsync(ordered);
            for (int i = releases.length - 1; i >= 0; i--) {
                // Observe this completion before allowing the next task to complete.
                releases[i].countDown();
                assertEquals(Integer.valueOf((releases.length - i) * 5), completionOrder.next());
            }
            assertFalse(completionOrder.hasNext());
        } finally {
            for (CountDownLatch release : releases) {
                release.countDown();
            }
            assertTrue(finished.await(5, TimeUnit.SECONDS), "All completion-order tasks must finish");
        }
    }

    @Test
    public void testCallWithRetry() {
        assertEquals("done", N.callWithRetry(() -> "done", 3, 10, (r, e) -> false));

        AtomicInteger counter = new AtomicInteger(0);
        String result = N.callWithRetry(() -> {
            if (counter.incrementAndGet() < 3) {
                throw new RuntimeException("fail");
            }
            return "success";
        }, 3, 10, (r, e) -> e instanceof RuntimeException);
        assertEquals("success", result);
        assertEquals(3, counter.get());
    }

    @Test
    public void testCallInParallel() throws Exception {
        Tuple2<String, Integer> two = N.callInParallel(() -> "hello", () -> 42);
        assertEquals("hello", two._1);
        assertEquals(42, two._2);

        Tuple3<String, Integer, Boolean> three = N.callInParallel(() -> "hello", () -> 42, () -> true);
        assertEquals("hello", three._1);
        assertEquals(true, three._3);

        Tuple4<String, Integer, Boolean, Double> four = N.callInParallel(() -> "hello", () -> 42, () -> true, () -> 3.14);
        assertEquals(3.14, four._4, DELTA);

        Tuple5<String, Integer, Boolean, Double, Character> five = N.callInParallel(() -> "world", () -> 99, () -> false, () -> 2.71, () -> 'z');
        assertEquals('z', five._5);

        Tuple3<String, Integer, Object> withNull = N.callInParallel(() -> "text", () -> 42, () -> null);
        assertNull(withNull._3);

        List<Integer> results = N.callInParallel(Arrays.asList(() -> 1, () -> 2, () -> 3, () -> 4, () -> 5));
        assertEquals(Arrays.asList(1, 2, 3, 4, 5), results);

        ExecutorService exec = Executors.newFixedThreadPool(3);
        try {
            assertEquals(Arrays.asList(1, 2, 3), N.callInParallel(Arrays.asList(() -> 1, () -> 2, () -> 3), exec));
            assertEquals(0, N.callInParallel(Collections.emptyList(), exec).size());
        } finally {
            exec.shutdown();
        }
    }

    @Test
    public void testCallByBatch() throws Exception {
        assertEquals(Arrays.asList(3, 3, 3, 1), N.callByBatch(Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10), 3, List::size));
        assertEquals(Arrays.asList(1, 2, 3, 4, 5), N.callByBatch(Arrays.asList(1, 2, 3, 4, 5), 1, batch -> batch.get(0)));
        assertTrue(N.callByBatch(new String[0], 5, List::size).isEmpty());
        assertEquals(Arrays.asList(2, 2, 1), N.callByBatch(new String[] { "A", "B", "C", "D", "E" }, 2, List::size));
        assertEquals(Arrays.asList(2, 2, 1), N.callByBatch((Iterable<String>) () -> Arrays.asList("A", "B", "C", "D", "E").iterator(), 2, List::size));

        List<Integer> processed = new ArrayList<>();
        List<String> labeled = N.callByBatch(Arrays.asList(1, 2, 3, 4, 5), 2, (idx, element) -> processed.add(element), () -> "batch-" + processed.size());
        assertEquals(Arrays.asList(1, 2, 3, 4, 5), processed);
        assertEquals(Arrays.asList("batch-2", "batch-4", "batch-5"), labeled);

        AtomicInteger sum = new AtomicInteger(0);
        List<Integer> batchSums = N.callByBatch(Arrays.asList(1, 2, 3, 4, 5, 6), 2, (idx, item) -> sum.addAndGet(item), () -> {
            int current = sum.get();
            sum.set(0);
            return current;
        });
        assertEquals(Arrays.asList(3, 7, 11), batchSums);

        assertEquals(Arrays.asList(3, 7, 5),
                N.callByBatch(Arrays.asList("a", "bb", "ccc", "dddd", "eeeee").iterator(), 2, batch -> batch.stream().mapToInt(String::length).sum()));
        assertEquals(Arrays.asList("APPLE-BANANA", "CHERRY-DATE", "ELDERBERRY"), N.callByBatch(Arrays.asList("apple", "banana", "cherry", "date", "elderberry"),
                2, batch -> batch.stream().map(String::toUpperCase).collect(Collectors.joining("-"))));

        List<String> withNull = N.callByBatch(Arrays.asList(1, 2, 3, 4), 2, batch -> batch.contains(3) ? null : batch.toString());
        assertEquals("[1, 2]", withNull.get(0));
        assertNull(withNull.get(1));

        AtomicInteger batchCount = new AtomicInteger(0);
        List<Integer> counted = N.callByBatch(new Integer[] { 1, 2, 3, 4, 5 }, 2, (idx, item) -> batchCount.incrementAndGet(), () -> batchCount.getAndSet(0));
        assertFalse(counted.isEmpty());

        assertThrows(RuntimeException.class, () -> N.callByBatch(Arrays.asList(1, 2, 3, 4, 5), 2, batch -> {
            if (batch.contains(3)) {
                throw new RuntimeException("Batch contains 3");
            }
            return batch.size();
        }));
        assertThrows(IllegalArgumentException.class, () -> N.callByBatch(Arrays.asList(1, 2, 3), 0, List::size));
    }

    @Test
    public void testCallUninterruptibly() {
        assertEquals("done", N.callUninterruptibly(() -> "done"));
        assertEquals("success", N.callUninterruptibly(() -> {
            Thread.sleep(10);
            return "success";
        }));
        assertNull(N.callUninterruptibly(() -> {
            Thread.sleep(10);
            return null;
        }));
        assertEquals("immediate", N.callUninterruptibly(() -> "immediate"));
        assertEquals("zero timeout", N.callUninterruptibly(millis -> "zero timeout", 0));
        assertEquals("negative timeout", N.callUninterruptibly(millis -> "negative timeout", -100));
        assertThrows(IllegalArgumentException.class, () -> N.callUninterruptibly(null));

        String timed = N.callUninterruptibly(remainingMillis -> {
            Thread.sleep(10);
            return "success-" + remainingMillis;
        }, 100);
        assertTrue(timed.startsWith("success-"));

        AtomicReference<TimeUnit> receivedUnit = new AtomicReference<>();
        AtomicLong receivedTime = new AtomicLong();
        Integer converted = N.callUninterruptibly((time, unit) -> {
            receivedUnit.set(unit);
            receivedTime.set(time);
            unit.sleep(1);
            return 42;
        }, 100, TimeUnit.MILLISECONDS);
        assertEquals(42, converted);
        assertEquals(TimeUnit.NANOSECONDS, receivedUnit.get());
        assertTrue(receivedTime.get() > 0);

        Thread currentThread = Thread.currentThread();
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        scheduler.schedule(currentThread::interrupt, 50, TimeUnit.MILLISECONDS);
        try {
            assertEquals("completed", N.callUninterruptibly((remainingNanos, unit) -> {
                TimeUnit.MILLISECONDS.sleep(100);
                return "completed";
            }, 200, TimeUnit.MILLISECONDS));
        } finally {
            // The local timer must finish before clearing the interrupt, so it cannot affect a later test.
            scheduler.shutdownNow();
            Thread.interrupted();
            try {
                assertTrue(N.callUninterruptibly(scheduler::awaitTermination, 1, TimeUnit.SECONDS));
            } finally {
                Thread.interrupted();
            }
        }
    }

    @Test
    public void testCallUninterruptibly_nullTimeUnitReportsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> N.callUninterruptibly((t, u) -> "x", 1, (TimeUnit) null));
        assertThrows(IllegalArgumentException.class,
                () -> N.callUninterruptibly((Throwables.BiFunction<Long, TimeUnit, String, InterruptedException>) null, 1, TimeUnit.MILLISECONDS));
    }
}

package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

public class NRunTest extends NTestSupport {

    @Test
    public void testRunWithRetry() {
        AtomicInteger counter = new AtomicInteger();
        N.runWithRetry(() -> {
            if (counter.incrementAndGet() < 3) {
                throw new RuntimeException("fail");
            }
        }, 3, 10, (java.util.function.Predicate<? super Exception>) e -> e instanceof RuntimeException);
        assertEquals(3, counter.get());

        AtomicInteger failures = new AtomicInteger();
        assertThrows(RuntimeException.class, () -> N.runWithRetry(() -> {
            failures.incrementAndGet();
            throw new RuntimeException("always fail");
        }, 2, 10, (java.util.function.Predicate<? super Exception>) e -> e instanceof RuntimeException));
        assertEquals(3, failures.get());
    }

    @Test
    public void testRunInParallel() throws Exception {
        AtomicInteger c1 = new AtomicInteger();
        AtomicInteger c2 = new AtomicInteger();
        N.runInParallel(c1::incrementAndGet, c2::incrementAndGet);
        assertEquals(1, c1.get());
        assertEquals(1, c2.get());

        AtomicInteger c3 = new AtomicInteger();
        AtomicInteger c4 = new AtomicInteger();
        AtomicInteger c5 = new AtomicInteger();
        N.runInParallel(c1::incrementAndGet, c2::incrementAndGet, c3::incrementAndGet, c4::incrementAndGet, c5::incrementAndGet);
        assertEquals(1, c3.get());

        List<Throwables.Runnable<Exception>> tasks = Arrays.asList(c1::incrementAndGet, c2::incrementAndGet, c3::incrementAndGet);
        N.runInParallel(tasks);
        N.runInParallel(Collections.emptyList());
        N.runInParallel(tasks, executorService);

        RuntimeException thrown = assertThrows(RuntimeException.class, () -> N.runInParallel(() -> {
            throw new IOException("Task 1 failed");
        }, () -> Thread.sleep(10)));
        assertTrue(thrown.getCause() instanceof IOException);

        AtomicInteger second = new AtomicInteger();
        assertThrows(RuntimeException.class, () -> N.runInParallel(second::incrementAndGet, () -> {
            Thread.sleep(20);
            throw new RuntimeException("Second task failed");
        }));
        assertEquals(1, second.get());

        CountDownLatch latch = new CountDownLatch(2);
        ObjIterator<Void> iter = N.runAsync(Arrays.asList(() -> {
            latch.countDown();
        }, () -> {
            latch.countDown();
        }), executorService);
        while (iter.hasNext()) {
            iter.next();
        }
        assertTrue(latch.await(500, TimeUnit.MILLISECONDS));
    }

    @Test
    public void testRunByBatch() {
        List<Integer> sums = new ArrayList<>();
        N.runByBatch(new Integer[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10 }, 3, batch -> sums.add(batch.stream().mapToInt(Integer::intValue).sum()));
        assertEquals(Arrays.asList(6, 15, 24, 10), sums);

        List<Integer> processed = new ArrayList<>();
        AtomicInteger batches = new AtomicInteger();
        N.runByBatch(new Integer[] { 1, 2, 3, 4, 5 }, 2, (idx, element) -> processed.add(element), batches::incrementAndGet);
        assertEquals(Arrays.asList(1, 2, 3, 4, 5), processed);
        assertEquals(3, batches.get());

        List<Integer> iterSums = new ArrayList<>();
        N.runByBatch(Arrays.asList(1, 2, 3, 4, 5, 6, 7).iterator(), 2, batch -> iterSums.add(batch.stream().mapToInt(Integer::intValue).sum()));
        assertEquals(Arrays.asList(3, 7, 11, 7), iterSums);

        List<List<String>> nullBatches = new ArrayList<>();
        N.runByBatch(new String[] { "a", null, "b", null, "c" }, 2, nullBatches::add);
        assertEquals(Arrays.asList("a", null), nullBatches.get(0));

        AtomicInteger emptyCount = new AtomicInteger();
        N.runByBatch(Collections.emptyList(), 10, batch -> emptyCount.incrementAndGet());
        N.runByBatch((Iterator<String>) null, 10, batch -> emptyCount.incrementAndGet());
        assertEquals(0, emptyCount.get());

        assertThrows(IllegalArgumentException.class, () -> N.runByBatch(new Integer[] { 1, 2, 3 }, 0, batch -> {
        }));
        assertThrows(RuntimeException.class, () -> N.runByBatch(Arrays.asList(1, 2, 3, 4, 5), 2, batch -> {
            if (batch.contains(3)) {
                throw new RuntimeException("Found 3!");
            }
        }));

        Iterable<String> nonList = () -> Arrays.asList("A", "B", "C", "D", "E").iterator();
        List<Integer> sizes = new ArrayList<>();
        N.runByBatch(nonList, 2, batch -> sizes.add(batch.size()));
        assertEquals(Arrays.asList(2, 2, 1), sizes);
    }

    @Test
    public void testRunUninterruptibly() {
        AtomicBoolean executed = new AtomicBoolean();
        N.runUninterruptibly(() -> executed.set(true));
        assertTrue(executed.get());

        AtomicInteger counter = new AtomicInteger();
        N.runUninterruptibly(remainingMillis -> {
            counter.incrementAndGet();
            Thread.sleep(10);
        }, 100);
        assertEquals(1, counter.get());

        Thread currentThread = Thread.currentThread();
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        scheduler.schedule(currentThread::interrupt, 50, TimeUnit.MILLISECONDS);
        try {
            N.runUninterruptibly(() -> Thread.sleep(100));
            assertTrue(Thread.interrupted());
        } finally {
            scheduler.shutdown();
            Thread.interrupted();
        }
    }

    @Test
    public void testRunUninterruptibly_nullTimeUnitReportsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> N.runUninterruptibly((t, u) -> {
        }, 1, (TimeUnit) null));
        assertThrows(IllegalArgumentException.class,
                () -> N.runUninterruptibly((Throwables.BiConsumer<Long, TimeUnit, InterruptedException>) null, 1, TimeUnit.MILLISECONDS));
    }
}

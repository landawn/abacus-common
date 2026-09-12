package com.landawn.abacus.pool;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

public class PoolAdmissionSignalTest extends TestBase {
    private static final String FIRST = "\uD83D\uDE80\u4E2D";
    private static final String NEXT = "";

    private static final class Value extends AbstractPoolable {
        final long bytes;
        boolean failMeasure;
        volatile boolean failActivity;
        int destroys;

        Value(final long bytes) {
            super(Long.MAX_VALUE, Long.MAX_VALUE);
            this.bytes = bytes;
        }

        @Override
        public ActivityPrint activityPrint() {
            if (failActivity) {
                throw new IllegalStateException("activity unavailable");
            }
            return super.activityPrint();
        }

        long measure() {
            if (failMeasure) {
                throw new IllegalArgumentException("measurement unavailable");
            }
            return bytes;
        }

        @Override
        public void destroy(final Caller caller) {
            destroys++;
        }
    }

    private static final class Scenario implements AutoCloseable {
        final GenericObjectPool<Value> objects;
        final GenericKeyedObjectPool<String, Value> keyed;
        final Value seed = new Value(1);
        final List<Thread> workers = new ArrayList<>();

        Scenario(final boolean withKeys) {
            objects = withKeys ? null : new GenericObjectPool<>(1, 0, EvictionPolicy.FIFO, false, 0.5f, 10, Value::measure);
            keyed = withKeys ? new GenericKeyedObjectPool<>(1, 0, EvictionPolicy.FIFO, false, 0.5f, 10, (key, value) -> value.measure()) : null;
            assertTrue(withKeys ? keyed.put("seed", seed) : objects.add(seed));
        }

        AbstractPool pool() {
            return objects == null ? keyed : objects;
        }

        CompletableFuture<Boolean> start(final String key, final Value value) {
            final CompletableFuture<Boolean> result = new CompletableFuture<>();
            final Thread worker = new Thread(() -> {
                try {
                    result.complete(objects == null ? keyed.put(key, value, 20, TimeUnit.SECONDS, true) : objects.add(value, 20, TimeUnit.SECONDS, true));
                } catch (final Throwable failure) {
                    result.completeExceptionally(failure);
                }
            });
            workers.add(worker);
            worker.start();
            return result;
        }

        void awaitWaiters(final int count) throws Exception {
            final long start = System.nanoTime();
            while (System.nanoTime() - start < TimeUnit.SECONDS.toNanos(3)) {
                pool().lock.lock();
                try {
                    if (pool().lock.getWaitQueueLength(objects == null ? keyed.notFull : objects.notFull) == count) {
                        return;
                    }
                } finally {
                    pool().lock.unlock();
                }
                Thread.sleep(1);
            }
            fail("producers did not enter the capacity queue in order");
        }

        void releaseSeed(final String operation) throws Exception {
            if (operation.startsWith("expired")) {
                seed.activityPrint().setCreatedTime(Long.MIN_VALUE);
            }
            switch (operation) {
                case "poll" -> assertSame(seed, objects.poll());
                case "timedPoll" -> assertSame(seed, objects.poll(0, TimeUnit.NANOSECONDS));
                case "expiredPoll" -> assertNull(objects.poll());
                case "expiredTimedPoll" -> assertNull(objects.poll(0, TimeUnit.NANOSECONDS));
                case "remove" -> assertSame(seed, keyed.remove("seed"));
                case "expiredGet" -> assertNull(keyed.get("seed"));
                case "expiredTimedGet" -> assertNull(keyed.get("seed", 0, TimeUnit.NANOSECONDS));
                case "expiredPeek" -> assertNull(keyed.peek("seed"));
                default -> throw new AssertionError(operation);
            }
        }

        Value take(final String key) {
            return objects == null ? keyed.remove(key) : objects.poll();
        }

        void assertFinal(final Value expected, final long puts) {
            assertEquals(1, pool().size());
            assertEquals(10, pool().totalDataSize.get());
            assertEquals(puts, pool().putCount.get());
            assertSame(expected, take(NEXT));
            assertEquals(0, pool().totalDataSize.get());
            assertEquals(0, expected.destroys);
        }

        @Override
        public void close() throws Exception {
            try {
                for (final Thread worker : workers) {
                    worker.interrupt();
                }
                for (final Thread worker : workers) {
                    worker.join(3_000);
                }
                for (final Thread worker : workers) {
                    assertFalse(worker.isAlive());
                }
            } finally {
                pool().close();
            }
        }
    }

    @Test
    public void rejectedProducerPassesCapacityToEligibleSuccessor() throws Exception {
        for (final boolean keyed : new boolean[] { false, true }) {
            final String[] releases = keyed ? new String[] { "remove", "expiredGet", "expiredTimedGet", "expiredPeek" }
                    : new String[] { "poll", "timedPoll", "expiredPoll", "expiredTimedPoll" };
            for (final String release : releases) {
                for (final String rejection : new String[] { "negative", "oversized", "measureThrows", "expired" }) {
                    try (Scenario scenario = new Scenario(keyed)) {
                        final Value first = new Value(rejection.equals("negative") ? -1 : rejection.equals("oversized") ? 11 : 1);
                        first.failMeasure = rejection.equals("measureThrows");
                        final Value next = new Value(10);
                        final CompletableFuture<Boolean> firstResult = scenario.start(FIRST, first);
                        scenario.awaitWaiters(1);
                        final CompletableFuture<Boolean> nextResult = scenario.start(NEXT, next);
                        scenario.awaitWaiters(2);
                        if (rejection.equals("expired")) {
                            first.activityPrint().setCreatedTime(Long.MIN_VALUE);
                        }
                        scenario.releaseSeed(release);
                        assertFalse(firstResult.get(2, TimeUnit.SECONDS));
                        assertTrue(nextResult.get(2, TimeUnit.SECONDS), "unused capacity must not wait for the 20-second producer timeout");
                        assertEquals(1, first.destroys);
                        scenario.assertFinal(next, 2);
                    }
                }
            }
        }
    }

    @Test
    public void severalRejectedProducersPassTheSignalAlongTheQueue() throws Exception {
        for (final boolean keyed : new boolean[] { false, true }) {
            try (Scenario scenario = new Scenario(keyed)) {
                final CompletableFuture<Boolean> first = scenario.start(FIRST, new Value(-1));
                scenario.awaitWaiters(1);
                final CompletableFuture<Boolean> middle = scenario.start("middle", new Value(11));
                scenario.awaitWaiters(2);
                final Value next = new Value(10);
                final CompletableFuture<Boolean> last = scenario.start(NEXT, next);
                scenario.awaitWaiters(3);
                scenario.releaseSeed(keyed ? "remove" : "poll");
                assertFalse(first.get(2, TimeUnit.SECONDS));
                assertFalse(middle.get(2, TimeUnit.SECONDS));
                assertTrue(last.get(2, TimeUnit.SECONDS));
                scenario.assertFinal(next, 2);
            }
        }
    }

    @Test
    public void successfulProducerConsumesTheSlotAndSuccessorWaitsForAnother() throws Exception {
        for (final boolean keyed : new boolean[] { false, true }) {
            try (Scenario scenario = new Scenario(keyed)) {
                final Value first = new Value(0);
                final CompletableFuture<Boolean> firstResult = scenario.start(FIRST, first);
                scenario.awaitWaiters(1);
                final Value next = new Value(10);
                final CompletableFuture<Boolean> nextResult = scenario.start(NEXT, next);
                scenario.awaitWaiters(2);
                scenario.releaseSeed(keyed ? "remove" : "timedPoll");
                assertTrue(firstResult.get(2, TimeUnit.SECONDS));
                scenario.awaitWaiters(1);
                assertFalse(nextResult.isDone());
                assertEquals(1, scenario.pool().size());
                assertEquals(0, scenario.pool().totalDataSize.get());
                assertSame(first, scenario.take(FIRST));
                assertTrue(nextResult.get(2, TimeUnit.SECONDS));
                assertEquals(0, first.destroys);
                scenario.assertFinal(next, 3);
            }
        }
    }

    @Test
    public void exceptionalAwakenedProducerDoesNotStrandSuccessor() throws Exception {
        for (final boolean keyed : new boolean[] { false, true }) {
            try (Scenario scenario = new Scenario(keyed)) {
                final Value first = new Value(1);
                final CompletableFuture<Boolean> firstResult = scenario.start(FIRST, first);
                scenario.awaitWaiters(1);
                final Value next = new Value(10);
                final CompletableFuture<Boolean> nextResult = scenario.start(NEXT, next);
                scenario.awaitWaiters(2);
                first.failActivity = true;
                scenario.releaseSeed(keyed ? "remove" : "poll");
                final ExecutionException failure = assertThrows(ExecutionException.class, () -> firstResult.get(2, TimeUnit.SECONDS));
                assertInstanceOf(IllegalStateException.class, failure.getCause());
                assertTrue(nextResult.get(2, TimeUnit.SECONDS));
                assertEquals(1, first.destroys);
                scenario.assertFinal(next, 2);
            }
        }
    }

    @Test
    public void interruptedProducerReleasesOwnershipWithoutStrandingSuccessor() throws Exception {
        for (final boolean keyed : new boolean[] { false, true }) {
            try (Scenario scenario = new Scenario(keyed)) {
                final Value first = new Value(1);
                final CompletableFuture<Boolean> firstResult = scenario.start(FIRST, first);
                scenario.awaitWaiters(1);
                final Value next = new Value(10);
                final CompletableFuture<Boolean> nextResult = scenario.start(NEXT, next);
                scenario.awaitWaiters(2);
                scenario.pool().lock.lock();
                try {
                    final Thread firstWorker = scenario.workers.get(0);
                    firstWorker.interrupt();
                    final long start = System.nanoTime();
                    // Wait for interrupt handling to transfer A from the condition to the lock queue.
                    while (!scenario.pool().lock.hasQueuedThread(firstWorker) && System.nanoTime() - start < TimeUnit.SECONDS.toNanos(2)) {
                        Thread.yield();
                    }
                    assertTrue(scenario.pool().lock.hasQueuedThread(firstWorker));
                    scenario.releaseSeed(keyed ? "remove" : "poll");
                } finally {
                    scenario.pool().lock.unlock();
                }
                final ExecutionException failure = assertThrows(ExecutionException.class, () -> firstResult.get(2, TimeUnit.SECONDS));
                assertInstanceOf(InterruptedException.class, failure.getCause());
                assertTrue(nextResult.get(2, TimeUnit.SECONDS));
                assertEquals(1, first.destroys);
                scenario.assertFinal(next, 2);
            }
        }
    }
}

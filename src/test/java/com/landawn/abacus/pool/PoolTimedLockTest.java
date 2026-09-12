package com.landawn.abacus.pool;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Condition;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

public class PoolTimedLockTest extends TestBase {
    private static final class Resource extends AbstractPoolable {
        private int destroys;

        Resource() {
            super(Long.MAX_VALUE, Long.MAX_VALUE);
        }

        @Override
        public void destroy(final Caller caller) {
            destroys++;
        }
    }

    @Test
    public void lockAcquisitionConsumesTheConditionWaitingBudget() throws Exception {
        try (Scenario scenario = new Scenario("poll")) {
            final AtomicLong conditionBudget = new AtomicLong();
            final Condition condition = org.mockito.Mockito.mock(Condition.class);
            org.mockito.Mockito.when(condition.awaitNanos(org.mockito.ArgumentMatchers.anyLong())).thenAnswer(invocation -> {
                conditionBudget.set(invocation.getArgument(0));
                return 0L;
            });
            scenario.objectPool.notEmpty = condition;
            final CompletableFuture<Object> result = new CompletableFuture<>();
            final Thread worker = new Thread(() -> {
                try {
                    result.complete(scenario.call(5_000));
                } catch (final Throwable failure) {
                    result.completeExceptionally(failure);
                }
            });
            scenario.pool().lock.lock();
            try {
                worker.start();
                final long start = System.nanoTime();
                while (!scenario.pool().lock.hasQueuedThread(worker) && System.nanoTime() - start < TimeUnit.SECONDS.toNanos(2)) {
                    Thread.yield();
                }
                assertTrue(scenario.pool().lock.hasQueuedThread(worker));
                Thread.sleep(100);
            } finally {
                scenario.pool().lock.unlock();
            }
            assertNull(result.get(3, TimeUnit.SECONDS));
            worker.join(2_000);
            assertTrue(conditionBudget.get() > 0);
            assertTrue(conditionBudget.get() <= TimeUnit.MILLISECONDS.toNanos(4_950), "initial contention must reduce the later waiting budget");
        }
    }

    @Test
    public void timedAutoDestroyWaitsForOwnershipCheckAndRetainsExistingInstance() throws Exception {
        final CountDownLatch admissionFinished = new CountDownLatch(1);
        try (GenericKeyedObjectPool<String, Resource> pool = new GenericKeyedObjectPool<>(1, 0, EvictionPolicy.FIFO, false, 0.5f) {
            @Override
            public boolean put(final String key, final Resource value, final long timeout, final TimeUnit unit) throws InterruptedException {
                try {
                    return super.put(key, value, timeout, unit);
                } finally {
                    admissionFinished.countDown();
                }
            }
        }) {
            final Resource value = new Resource();
            assertTrue(pool.put("key", value));
            final CompletableFuture<Boolean> result = new CompletableFuture<>();
            final Thread worker = new Thread(() -> {
                try {
                    result.complete(pool.put("key", value, 25, TimeUnit.MILLISECONDS, true));
                } catch (final Throwable failure) {
                    result.completeExceptionally(failure);
                }
            });
            pool.lock.lock();
            try {
                worker.start();
                assertTrue(admissionFinished.await(2, TimeUnit.SECONDS));
                assertFalse(result.isDone(), "automatic cleanup waits for a safe ownership check after admission times out");
            } finally {
                pool.lock.unlock();
            }
            assertFalse(result.get(2, TimeUnit.SECONDS));
            worker.join(2_000);
            assertSame(value, pool.peek("key"));
            assertEquals(0, value.destroys);
        }
    }

    private static final class Scenario implements AutoCloseable {
        final GenericObjectPool<PoolableAdapter<String>> objectPool = new GenericObjectPool<>(2, 0, EvictionPolicy.FIFO, false, 0.5f);
        final GenericKeyedObjectPool<String, PoolableAdapter<String>> keyedPool = new GenericKeyedObjectPool<>(2, 0, EvictionPolicy.FIFO, false, 0.5f);
        final String operation;

        Scenario(final String operation) {
            this.operation = operation;
        }

        AbstractPool pool() {
            return operation.equals("add") || operation.equals("poll") ? objectPool : keyedPool;
        }

        Object call(final long timeout) throws InterruptedException {
            return switch (operation) {
                case "add" -> objectPool.add(Poolable.wrap(""), timeout, TimeUnit.MILLISECONDS);
                case "poll" -> objectPool.poll(timeout, TimeUnit.MILLISECONDS);
                case "put" -> keyedPool.put("\uD83D\uDE80", Poolable.wrap(""), timeout, TimeUnit.MILLISECONDS);
                case "get" -> keyedPool.get("\uD83D\uDE80", timeout, TimeUnit.MILLISECONDS);
                default -> throw new AssertionError(operation);
            };
        }

        boolean read() {
            return operation.equals("poll") || operation.equals("get");
        }

        @Override
        public void close() {
            objectPool.close();
            keyedPool.close();
        }
    }

    @Test
    public void timedOperationsExpireWhileAnotherThreadStillHoldsLock() throws Exception {
        for (final String operation : new String[] { "add", "poll", "put", "get" }) {
            try (Scenario scenario = new Scenario(operation)) {
                final CompletableFuture<Object> result = new CompletableFuture<>();
                scenario.pool().lock.lock();
                final Thread worker = new Thread(() -> {
                    try {
                        result.complete(scenario.call(25));
                    } catch (final Throwable failure) {
                        result.completeExceptionally(failure);
                    }
                });
                try {
                    worker.start();
                    if (scenario.read()) {
                        assertNull(result.get(2, TimeUnit.SECONDS), operation);
                    } else {
                        assertEquals(false, result.get(2, TimeUnit.SECONDS), operation);
                    }
                    assertEquals(scenario.read() ? 1 : 0, scenario.pool().missCount.get());
                    assertEquals(0, scenario.pool().putCount.get());
                } finally {
                    scenario.pool().lock.unlock();
                    worker.interrupt();
                    worker.join(2_000);
                }
            }
        }
    }

    @Test
    public void interruptionAbortsInitialLockAcquisitionWithoutRecordingAMiss() throws Exception {
        for (final String operation : new String[] { "add", "poll", "put", "get" }) {
            try (Scenario scenario = new Scenario(operation)) {
                final CompletableFuture<Throwable> result = new CompletableFuture<>();
                scenario.pool().lock.lock();
                final Thread worker = new Thread(() -> {
                    try {
                        scenario.call(Long.MAX_VALUE);
                        result.complete(null);
                    } catch (final Throwable failure) {
                        result.complete(failure);
                    }
                });
                try {
                    worker.start();
                    final long start = System.nanoTime();
                    while (!scenario.pool().lock.hasQueuedThread(worker) && System.nanoTime() - start < TimeUnit.SECONDS.toNanos(2)) {
                        Thread.yield();
                    }
                    assertTrue(scenario.pool().lock.hasQueuedThread(worker), operation);
                    worker.interrupt();
                    assertInstanceOf(InterruptedException.class, result.get(2, TimeUnit.SECONDS), operation);
                    assertEquals(0, scenario.pool().missCount.get());
                    assertEquals(0, scenario.pool().putCount.get());
                } finally {
                    scenario.pool().lock.unlock();
                    worker.interrupt();
                    worker.join(2_000);
                }
            }
        }
    }

    @Test
    public void preInterruptedCallsFailEvenWhenLockIsImmediatelyAvailable() throws Exception {
        for (final String operation : new String[] { "add", "poll", "put", "get" }) {
            try (Scenario scenario = new Scenario(operation)) {
                final CompletableFuture<Throwable> result = new CompletableFuture<>();
                final Thread worker = new Thread(() -> {
                    Thread.currentThread().interrupt();
                    try {
                        scenario.call(0);
                        result.complete(null);
                    } catch (final Throwable failure) {
                        result.complete(failure);
                    }
                });
                worker.start();
                assertInstanceOf(InterruptedException.class, result.get(2, TimeUnit.SECONDS), operation);
                worker.join(2_000);
                assertEquals(0, scenario.pool().missCount.get());
                assertEquals(0, scenario.pool().putCount.get());
            }
        }
    }

    @Test
    public void nonpositiveTimeoutsStillAttemptAvailableOperations() throws Exception {
        for (final long timeout : new long[] { 0, -1, Long.MIN_VALUE }) {
            try (Scenario add = new Scenario("add");
                 Scenario put = new Scenario("put")) {
                assertEquals(true, add.call(timeout));
                assertNotNull(add.objectPool.poll(timeout, TimeUnit.MILLISECONDS));
                assertNull(add.objectPool.poll(timeout, TimeUnit.MILLISECONDS));
                assertEquals(true, put.call(timeout));
                assertNotNull(put.keyedPool.get("\uD83D\uDE80", timeout, TimeUnit.MILLISECONDS));
                assertNull(put.keyedPool.get("missing", timeout, TimeUnit.MILLISECONDS));
            }
        }
    }
}

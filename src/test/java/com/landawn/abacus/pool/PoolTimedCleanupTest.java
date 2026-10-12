package com.landawn.abacus.pool;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.LockSupport;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

import com.landawn.abacus.TestBase;

// Extends TestBase and is tagged "slow-test": performance/memory-tuning regressions stay out of
// the regular suite (AbacusCoreTestSuite excludes "slow-test"). Run this class directly when changing that code.
@Tag("slow-test")
public class PoolTimedCleanupTest extends TestBase {
    private static final int PAYLOAD_BYTES = 64 * 1024;

    private static final class Value extends AbstractPoolable {
        final CountDownLatch destroyed = new CountDownLatch(1);
        final AtomicInteger destroyCount = new AtomicInteger();
        final Runnable onDestroy;
        byte[] payload = new byte[PAYLOAD_BYTES];

        Value(final Runnable onDestroy) {
            super(60_000, Long.MAX_VALUE);
            this.onDestroy = onDestroy;
        }

        @Override
        public void destroy(final Caller caller) {
            destroyCount.incrementAndGet();
            try {
                onDestroy.run();
            } finally {
                payload = null;
                destroyed.countDown();
            }
        }
    }

    private static final class Scenario implements AutoCloseable {
        final GenericObjectPool<Value> objectPool;
        final GenericKeyedObjectPool<String, Value> keyedPool;

        Scenario(final boolean keyed) {
            objectPool = keyed ? null : new GenericObjectPool<>(1, 0, EvictionPolicy.FIFO, false, 0.5f, PAYLOAD_BYTES, value -> PAYLOAD_BYTES);
            keyedPool = keyed ? new GenericKeyedObjectPool<>(1, 0, EvictionPolicy.FIFO, false, 0.5f, PAYLOAD_BYTES, (key, value) -> PAYLOAD_BYTES) : null;
        }

        AbstractPool pool() {
            return objectPool == null ? keyedPool : objectPool;
        }

        boolean add(final Value value) {
            return objectPool == null ? keyedPool.put("key", value) : objectPool.add(value);
        }

        void addExpired(final Value value) {
            pool().lock.lock();
            try {
                assertTrue(add(value));
                value.activityPrint().setCreatedTime(System.currentTimeMillis() - 120_000);
            } finally {
                pool().lock.unlock();
            }
        }

        Value read(final long timeoutMillis) throws InterruptedException {
            return objectPool == null ? keyedPool.get("key", timeoutMillis, TimeUnit.MILLISECONDS) : objectPool.poll(timeoutMillis, TimeUnit.MILLISECONDS);
        }

        @Override
        public void close() {
            pool().close();
        }
    }

    private static final class Waiter implements AutoCloseable {
        final CompletableFuture<Value> result = new CompletableFuture<>();
        final Thread thread;

        Waiter(final Scenario scenario, final long timeoutMillis) {
            thread = new Thread(() -> {
                try {
                    result.complete(scenario.read(timeoutMillis));
                } catch (final Throwable failure) {
                    result.completeExceptionally(failure);
                }
            }, "pool-timed-cleanup-test");
            thread.setDaemon(true);
            thread.start();
        }

        @Override
        public void close() throws InterruptedException {
            thread.interrupt();
            thread.join(3_000);
            assertFalse(thread.isAlive(), "the test must not leave a pool waiter running");
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    public void expiredAdmissionsAreReleasedWhileTheSameReaderKeepsWaiting(final boolean keyed) throws Exception {
        try (Scenario scenario = new Scenario(keyed);
             Waiter waiter = new Waiter(scenario, 30_000)) {
            final AtomicBoolean callbackHeldLock = new AtomicBoolean();
            for (int i = 0; i < 16; i++) {
                final Value value = new Value(() -> callbackHeldLock.set(callbackHeldLock.get() || scenario.pool().lock.isHeldByCurrentThread()));
                scenario.addExpired(value);
                assertTrue(value.destroyed.await(3, TimeUnit.SECONDS), "cleanup must run before the long wait ends");
                assertFalse(waiter.result.isDone());
                assertNull(value.payload);
                assertEquals(1, value.destroyCount.get());
                assertEquals(0, scenario.pool().size());
                assertEquals(0, scenario.pool().totalDataSize.get());
            }
            assertFalse(callbackHeldLock.get());
            assertEquals(16, scenario.pool().evictionCount.get());
            assertEquals(0, scenario.pool().hitCount.get());
            assertEquals(0, scenario.pool().missCount.get());
            waiter.thread.interrupt();
            final ExecutionException failure = assertThrows(ExecutionException.class, () -> waiter.result.get(3, TimeUnit.SECONDS));
            assertInstanceOf(InterruptedException.class, failure.getCause());
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    public void callbackAdmissionIsRecheckedWithoutWaitingForAnotherSignal(final boolean keyed) throws Exception {
        try (Scenario scenario = new Scenario(keyed)) {
            final Value replacement = new Value(() -> {
            });
            final AtomicBoolean admitted = new AtomicBoolean();
            final AtomicBoolean callbackHeldLock = new AtomicBoolean();
            final Value expired = new Value(() -> {
                callbackHeldLock.set(scenario.pool().lock.isHeldByCurrentThread());
                admitted.set(scenario.add(replacement));
            });
            scenario.addExpired(expired);
            try (Waiter waiter = new Waiter(scenario, 30_000)) {
                assertSame(replacement, waiter.result.get(3, TimeUnit.SECONDS));
            }
            assertTrue(admitted.get());
            assertFalse(callbackHeldLock.get());
            assertEquals(1, expired.destroyCount.get());
            assertEquals(1, scenario.pool().hitCount.get());
            assertEquals(0, scenario.pool().missCount.get());
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    public void callbackCloseIsObservedWithoutRecordingAMiss(final boolean keyed) throws Exception {
        try (Scenario scenario = new Scenario(keyed)) {
            final Value expired = new Value(scenario::close);
            scenario.addExpired(expired);
            try (Waiter waiter = new Waiter(scenario, 30_000)) {
                final ExecutionException failure = assertThrows(ExecutionException.class, () -> waiter.result.get(3, TimeUnit.SECONDS));
                assertInstanceOf(IllegalStateException.class, failure.getCause());
            }
            assertEquals(1, expired.destroyCount.get());
            assertEquals(0, scenario.pool().hitCount.get());
            assertEquals(0, scenario.pool().missCount.get());
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    public void ordinaryCallbackFailureIsContainedAfterReentrantAdmission(final boolean keyed) throws Exception {
        try (Scenario scenario = new Scenario(keyed)) {
            final Value replacement = new Value(() -> {
            });
            final Value expired = new Value(() -> {
                scenario.add(replacement);
                throw new IllegalStateException("expected callback failure");
            });
            scenario.addExpired(expired);
            try (Waiter waiter = new Waiter(scenario, 30_000)) {
                assertSame(replacement, waiter.result.get(3, TimeUnit.SECONDS));
            }
            assertEquals(1, expired.destroyCount.get());
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    public void fatalCallbackFailureIsPropagatedOnceWithoutUnlockingTwice(final boolean keyed) throws Exception {
        try (Scenario scenario = new Scenario(keyed)) {
            final VirtualMachineError fatal = new VirtualMachineError("test fatal callback") {
            };
            final Value expired = new Value(() -> {
                throw fatal;
            });
            scenario.addExpired(expired);
            try (Waiter waiter = new Waiter(scenario, 30_000)) {
                final ExecutionException failure = assertThrows(ExecutionException.class, () -> waiter.result.get(3, TimeUnit.SECONDS));
                assertSame(fatal, failure.getCause());
            }
            assertEquals(1, expired.destroyCount.get());
            assertEquals(0, scenario.pool().missCount.get());
            assertTrue(scenario.add(new Value(() -> {
            })), "the lock must remain usable after fatal cleanup");
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    public void cleanupTimeConsumesTheRemainingConditionBudget(final boolean keyed) throws Exception {
        try (Scenario scenario = new Scenario(keyed)) {
            final AtomicLong cleanupNanos = new AtomicLong();
            final AtomicLong conditionBudget = new AtomicLong();
            final Value expired = new Value(() -> {
                final long start = System.nanoTime();
                LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(50));
                cleanupNanos.set(System.nanoTime() - start);
            });
            scenario.addExpired(expired);
            final Condition condition = Mockito.mock(Condition.class);
            Mockito.when(condition.awaitNanos(ArgumentMatchers.anyLong())).thenAnswer(invocation -> {
                assertEquals(1, expired.destroyCount.get(), "cleanup must precede the next await");
                conditionBudget.set(invocation.getArgument(0));
                return 0L;
            });
            scenario.pool().notEmpty = condition;
            assertNull(scenario.read(5_000));
            assertTrue(conditionBudget.get() > 0);
            assertTrue(conditionBudget.get() <= TimeUnit.SECONDS.toNanos(5) - cleanupNanos.get());
            assertEquals(1, expired.destroyCount.get());
            assertEquals(1, scenario.pool().missCount.get());
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    public void reacquiringAfterCleanupCannotExceedTheWaitBudget(final boolean keyed) throws Exception {
        try (Scenario scenario = new Scenario(keyed)) {
            final CountDownLatch cleanupEntered = new CountDownLatch(1);
            final CountDownLatch finishCleanup = new CountDownLatch(1);
            final Value expired = new Value(() -> {
                cleanupEntered.countDown();
                await(finishCleanup);
            });
            scenario.addExpired(expired);
            try (Waiter waiter = new Waiter(scenario, 250)) {
                assertTrue(cleanupEntered.await(3, TimeUnit.SECONDS));
                scenario.pool().lock.lock();
                try {
                    finishCleanup.countDown();
                    assertNull(waiter.result.get(3, TimeUnit.SECONDS), "timed reacquisition must finish while another thread still holds the lock");
                    assertEquals(1, scenario.pool().missCount.get());
                    assertEquals(1, expired.destroyCount.get());
                } finally {
                    finishCleanup.countDown();
                    scenario.pool().lock.unlock();
                }
            }
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    public void reacquisitionAfterCleanupRemainsInterruptible(final boolean keyed) throws Exception {
        try (Scenario scenario = new Scenario(keyed)) {
            final CountDownLatch cleanupEntered = new CountDownLatch(1);
            final CountDownLatch finishCleanup = new CountDownLatch(1);
            final Value expired = new Value(() -> {
                cleanupEntered.countDown();
                await(finishCleanup);
            });
            scenario.addExpired(expired);
            try (Waiter waiter = new Waiter(scenario, 30_000)) {
                assertTrue(cleanupEntered.await(3, TimeUnit.SECONDS));
                scenario.pool().lock.lock();
                try {
                    finishCleanup.countDown();
                    final long start = System.nanoTime();
                    while (!scenario.pool().lock.hasQueuedThread(waiter.thread) && System.nanoTime() - start < TimeUnit.SECONDS.toNanos(3)) {
                        Thread.yield();
                    }
                    assertTrue(scenario.pool().lock.hasQueuedThread(waiter.thread));
                    waiter.thread.interrupt();
                    final ExecutionException failure = assertThrows(ExecutionException.class, () -> waiter.result.get(3, TimeUnit.SECONDS));
                    assertInstanceOf(InterruptedException.class, failure.getCause());
                    assertEquals(0, scenario.pool().missCount.get());
                    assertEquals(1, expired.destroyCount.get());
                } finally {
                    finishCleanup.countDown();
                    scenario.pool().lock.unlock();
                }
            }
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    public void zeroTimeoutRetainsCompletionCleanupAndDoesNotConsumeCallbackAdmission(final boolean keyed) throws Exception {
        try (Scenario scenario = new Scenario(keyed)) {
            final Value replacement = new Value(() -> {
            });
            final Value expired = new Value(() -> scenario.add(replacement));
            scenario.addExpired(expired);
            assertNull(scenario.read(0));
            assertEquals(1, expired.destroyCount.get());
            assertEquals(1, scenario.pool().missCount.get());
            assertSame(replacement, scenario.read(0));
        }
    }

    private static void await(final CountDownLatch latch) {
        try {
            if (!latch.await(5, TimeUnit.SECONDS)) {
                throw new IllegalStateException("test cleanup coordination timed out");
            }
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
    }
}

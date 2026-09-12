package com.landawn.abacus.pool;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

public class PoolFailedAdmissionOwnershipTest extends TestBase {
    private static final String INNER = "";
    private static final String OUTER = "\uD83D\uDE80\u4E2D";

    private static final class Value extends AbstractPoolable {
        private final String text;
        private int destroys;
        private Runnable onDestroy = () -> {
        };

        Value(final String text) {
            super(Long.MAX_VALUE, Long.MAX_VALUE);
            this.text = text;
        }

        @Override
        public void destroy(final Caller caller) {
            destroys++;
            onDestroy.run();
        }

        @Override
        public boolean equals(final Object other) {
            return other instanceof Value value && text.equals(value.text);
        }

        @Override
        public int hashCode() {
            return text.hashCode();
        }
    }

    private static final class Scenario implements AutoCloseable {
        private final GenericObjectPool<Value> objects;
        private final GenericKeyedObjectPool<String, Value> keyed;

        Scenario(final boolean withKeys, final int capacity, final ObjectPool.MemoryMeasure<Value> measure) {
            objects = withKeys ? null : new GenericObjectPool<>(capacity, 0, EvictionPolicy.FIFO, false, 0.5f, 100, measure);
            keyed = withKeys ? new GenericKeyedObjectPool<>(capacity, 0, EvictionPolicy.FIFO, false, 0.5f, 100, (key, value) -> measure.sizeOf(value)) : null;
        }

        AbstractPool pool() {
            return objects == null ? keyed : objects;
        }

        boolean admit(final String key, final Value value, final boolean timed, final boolean cleanup) throws InterruptedException {
            if (objects != null) {
                return timed ? objects.add(value, 0, TimeUnit.NANOSECONDS, cleanup) : objects.add(value, cleanup);
            }
            return timed ? keyed.put(key, value, 0, TimeUnit.NANOSECONDS, cleanup) : keyed.put(key, value, cleanup);
        }

        Value take() {
            return objects == null ? keyed.remove(INNER) : objects.poll();
        }

        @Override
        public void close() {
            pool().close();
        }
    }

    @Test
    public void reentrantAdmissionRetainsTheRejectedCandidate() throws Exception {
        for (final boolean keyed : new boolean[] { false, true }) {
            for (final boolean timed : new boolean[] { false, true }) {
                final AtomicBoolean nested = new AtomicBoolean();
                final AtomicReference<Scenario> reference = new AtomicReference<>();
                try (Scenario scenario = new Scenario(keyed, 1, value -> {
                    if (nested.compareAndSet(false, true)) {
                        try {
                            assertTrue(reference.get().admit(INNER, value, false, false));
                        } catch (final InterruptedException e) {
                            throw new AssertionError(e);
                        }
                    }
                    return 7;
                })) {
                    reference.set(scenario);
                    final Value value = new Value(OUTER);
                    assertFalse(scenario.admit(OUTER, value, timed, true));
                    assertEquals(0, value.destroys);
                    assertEquals(1, scenario.pool().size());
                    assertEquals(1, scenario.pool().putCount.get());
                    assertEquals(7, scenario.pool().totalDataSize.get());
                    assertSame(value, scenario.take());
                    assertEquals(0, scenario.pool().totalDataSize.get());
                    assertEquals(0, value.destroys);
                }
            }
        }
    }

    @Test
    public void alreadyRetainedIdentitySurvivesCapacityAndExpiryRejection() throws Exception {
        for (final boolean keyed : new boolean[] { false, true }) {
            for (final boolean timed : new boolean[] { false, true }) {
                try (Scenario scenario = new Scenario(keyed, 1, value -> 7)) {
                    final Value value = new Value(INNER);
                    assertTrue(scenario.admit(INNER, value, false, false));
                    assertFalse(scenario.admit(OUTER, value, timed, true));
                    assertEquals(0, value.destroys);
                    value.activityPrint().setCreatedTime(0).setMaxLiveTime(1);
                    assertFalse(scenario.admit(OUTER, value, timed, true));
                    assertEquals(0, value.destroys, "failure cleanup leaves retained expiry cleanup to the pool");
                    assertEquals(7, scenario.pool().totalDataSize.get());
                    scenario.pool().clear();
                    assertEquals(1, value.destroys);
                    assertEquals(0, scenario.pool().totalDataSize.get());
                }
            }
        }
    }

    @Test
    public void equalButDistinctRejectedValuesAreDestroyedOnlyWhenEnabled() throws Exception {
        for (final boolean keyed : new boolean[] { false, true }) {
            for (final boolean timed : new boolean[] { false, true }) {
                for (final boolean cleanup : new boolean[] { false, true }) {
                    try (Scenario scenario = new Scenario(keyed, 1, value -> 7)) {
                        final Value retained = new Value(OUTER);
                        final Value rejected = new Value(OUTER);
                        assertEquals(retained, rejected);
                        rejected.onDestroy = () -> assertFalse(scenario.pool().lock.isHeldByCurrentThread(), "callback must run outside the pool lock");
                        assertTrue(scenario.admit(INNER, retained, false, false));
                        assertFalse(scenario.admit(OUTER, rejected, timed, cleanup));
                        assertEquals(cleanup ? 1 : 0, rejected.destroys);
                        assertEquals(0, retained.destroys);
                        assertSame(retained, scenario.take());
                        assertEquals(0, scenario.pool().totalDataSize.get());
                    }
                }
            }
        }
    }

    @Test
    public void emptyZeroCapacityAndNullFailuresKeepCleanupSemantics() throws Exception {
        for (final boolean keyed : new boolean[] { false, true }) {
            for (final boolean timed : new boolean[] { false, true }) {
                try (Scenario scenario = new Scenario(keyed, 0, value -> 0)) {
                    final Value rejected = new Value(INNER);
                    assertFalse(scenario.admit(OUTER, rejected, timed, true));
                    assertEquals(1, rejected.destroys);
                    assertThrows(IllegalArgumentException.class, () -> scenario.admit(OUTER, null, timed, true));
                    assertTrue(scenario.pool().isEmpty());
                    assertEquals(0, scenario.pool().putCount.get());
                }
            }
        }
    }

    @Test
    public void argumentFailurePreservesRetainedValuesButCleansAbsentOnes() throws Exception {
        for (final boolean withKeys : new boolean[] { false, true }) {
            try (Scenario scenario = new Scenario(withKeys, 1, value -> 7)) {
                final Value retained = new Value(INNER);
                final Value absent = new Value(OUTER);
                assertTrue(scenario.admit(INNER, retained, false, false));
                for (final Value value : new Value[] { retained, absent }) {
                    if (withKeys) {
                        assertThrows(IllegalArgumentException.class, () -> scenario.keyed.put(OUTER, value, 0, null, true));
                    } else {
                        assertThrows(IllegalArgumentException.class, () -> scenario.objects.add(value, 0, null, true));
                    }
                }
                if (withKeys) {
                    assertThrows(IllegalArgumentException.class, () -> scenario.keyed.put(null, retained, true));
                    assertEquals(0, retained.destroys);
                }
                assertEquals(0, retained.destroys);
                assertEquals(1, absent.destroys);
                assertSame(retained, scenario.take());
            }
        }
    }

    @Test
    public void interruptedAdmissionPreservesRetainedIdentity() throws Exception {
        for (final boolean keyed : new boolean[] { false, true }) {
            try (Scenario scenario = new Scenario(keyed, 1, value -> 7)) {
                final Value value = new Value(OUTER);
                assertTrue(scenario.admit(INNER, value, false, false));
                Thread.currentThread().interrupt();
                try {
                    assertThrows(InterruptedException.class, () -> scenario.admit(OUTER, value, true, true));
                } finally {
                    Thread.interrupted();
                }
                assertEquals(0, value.destroys);
                assertEquals(7, scenario.pool().totalDataSize.get());
                assertSame(value, scenario.take());
            }
        }
    }

    @Test
    public void objectTimedCleanupWaitsForRetentionCheckAfterTimeout() throws Exception {
        final CountDownLatch admissionFinished = new CountDownLatch(1);
        try (GenericObjectPool<Value> pool = new GenericObjectPool<>(1, 0, EvictionPolicy.FIFO, false, 0.5f) {
            @Override
            public boolean add(final Value value, final long timeout, final TimeUnit unit) throws InterruptedException {
                try {
                    return super.add(value, timeout, unit);
                } finally {
                    admissionFinished.countDown();
                }
            }
        }) {
            final Value value = new Value(OUTER);
            assertTrue(pool.add(value));
            final CompletableFuture<Boolean> result = new CompletableFuture<>();
            final Thread worker = new Thread(() -> {
                try {
                    result.complete(pool.add(value, 0, TimeUnit.NANOSECONDS, true));
                } catch (final Throwable failure) {
                    result.completeExceptionally(failure);
                }
            });
            pool.lock.lock();
            try {
                worker.start();
                assertTrue(admissionFinished.await(2, TimeUnit.SECONDS));
                assertFalse(result.isDone(), "cleanup must check retention even after the admission budget expires");
            } finally {
                pool.lock.unlock();
                worker.join(2_000);
            }
            assertFalse(result.get(2, TimeUnit.SECONDS));
            assertEquals(0, value.destroys);
            assertSame(value, pool.poll());
        }
    }
}

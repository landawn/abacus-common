package com.landawn.abacus.pool;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

public class PoolMemoryCallbackTest extends TestBase {
    private static final class Value extends AbstractPoolable {
        volatile long bytes = 10;
        int destroys;

        Value() {
            super(Long.MAX_VALUE, Long.MAX_VALUE);
        }

        @Override
        public void destroy(final Caller caller) {
            destroys++;
        }
    }

    private static long response(final int mode) {
        if (mode == 2) {
            throw new IllegalArgumentException("measurement failed");
        }
        return mode == 1 ? -1 : 10;
    }

    @Test
    public void objectMeasurementCannotReopenPoolAfterReentrantClose() {
        for (final boolean timed : new boolean[] { false, true }) {
            for (int mode = 0; mode < 3; mode++) {
                final int responseMode = mode;
                final AtomicReference<GenericObjectPool<Value>> reference = new AtomicReference<>();
                try (GenericObjectPool<Value> pool = new GenericObjectPool<>(1, 0, EvictionPolicy.FIFO, false, 0.5f, 100, value -> {
                    assertFalse(reference.get().lock.isHeldByCurrentThread());
                    reference.get().close();
                    return response(responseMode);
                })) {
                    reference.set(pool);
                    final Value candidate = new Value();
                    assertThrows(IllegalStateException.class, () -> {
                        if (timed) {
                            pool.add(candidate, 0, TimeUnit.NANOSECONDS);
                        } else {
                            pool.add(candidate);
                        }
                    });
                    assertTrue(pool.isClosed());
                    assertTrue(pool.pool.isEmpty());
                    assertEquals(0, pool.totalDataSize.get());
                    assertEquals(0, candidate.destroys, "failed insertion without auto-destroy leaves ownership with caller");
                }
            }
        }
    }

    @Test
    public void keyedMeasurementCannotInsertOrRestoreAfterReentrantClose() {
        for (final boolean timed : new boolean[] { false, true }) {
            for (final boolean sameInstance : new boolean[] { false, true }) {
                for (int mode = 0; mode < 3; mode++) {
                    final int responseMode = mode;
                    final AtomicBoolean armed = new AtomicBoolean();
                    final AtomicReference<GenericKeyedObjectPool<String, Value>> reference = new AtomicReference<>();
                    try (GenericKeyedObjectPool<String, Value> pool = new GenericKeyedObjectPool<>(1, 0, EvictionPolicy.FIFO, false, 0.5f, 100,
                            (key, value) -> {
                                assertFalse(reference.get().lock.isHeldByCurrentThread());
                                if (armed.get()) {
                                    reference.get().close();
                                    return response(responseMode);
                                }
                                return 10;
                            })) {
                        reference.set(pool);
                        final Value original = new Value();
                        assertTrue(pool.put("\uD83D\uDE80", original));
                        final Value candidate = sameInstance ? original : new Value();
                        armed.set(true);
                        assertThrows(IllegalStateException.class, () -> {
                            if (timed) {
                                pool.put("\uD83D\uDE80", candidate, 0, TimeUnit.NANOSECONDS);
                            } else {
                                pool.put("\uD83D\uDE80", candidate);
                            }
                        });
                        assertTrue(pool.isClosed());
                        assertTrue(pool.pool.isEmpty());
                        assertEquals(0, pool.totalDataSize.get());
                        assertEquals(1, original.destroys);
                        assertEquals(sameInstance ? 1 : 0, candidate.destroys);
                    }
                }
            }
        }
    }

    @Test
    public void nestedObjectAdmissionCannotExceedCapacity() throws Exception {
        for (final boolean timed : new boolean[] { false, true }) {
            final AtomicBoolean nested = new AtomicBoolean();
            final AtomicReference<GenericObjectPool<Value>> reference = new AtomicReference<>();
            final Value inner = new Value();
            try (GenericObjectPool<Value> pool = new GenericObjectPool<>(1, 0, EvictionPolicy.FIFO, false, 0.5f, 100, value -> {
                if (nested.compareAndSet(false, true)) {
                    assertTrue(reference.get().add(inner));
                }
                return 10;
            })) {
                reference.set(pool);
                assertFalse(timed ? pool.add(new Value(), 0, TimeUnit.NANOSECONDS) : pool.add(new Value()));
                assertEquals(1, pool.size());
                assertEquals(10, pool.stats().dataSize());
                assertSame(inner, pool.poll());
            }
        }
    }

    @Test
    public void nestedKeyedAdmissionCannotExceedCapacity() throws Exception {
        for (final boolean timed : new boolean[] { false, true }) {
            final AtomicBoolean nested = new AtomicBoolean();
            final AtomicReference<GenericKeyedObjectPool<String, Value>> reference = new AtomicReference<>();
            final Value inner = new Value();
            try (GenericKeyedObjectPool<String, Value> pool = new GenericKeyedObjectPool<>(1, 0, EvictionPolicy.FIFO, false, 0.5f, 100, (key, value) -> {
                if (nested.compareAndSet(false, true)) {
                    assertTrue(reference.get().put("inner", inner));
                }
                return 10;
            })) {
                reference.set(pool);
                assertFalse(timed ? pool.put("outer", new Value(), 0, TimeUnit.NANOSECONDS) : pool.put("outer", new Value()));
                assertEquals(1, pool.size());
                assertEquals(10, pool.stats().dataSize());
                assertSame(inner, pool.remove("inner"));
            }
        }
    }

    @Test
    public void timedAdmissionSamplesOnceBeforeWaiting() throws Exception {
        final AtomicInteger calls = new AtomicInteger();
        try (GenericObjectPool<Value> pool = new GenericObjectPool<>(1, 0, EvictionPolicy.FIFO, false, 0.5f, 100, value -> {
            calls.incrementAndGet();
            return value.bytes;
        })) {
            assertTrue(pool.add(new Value()));
            final Value candidate = new Value();
            final CompletableFuture<Boolean> result = new CompletableFuture<>();
            final Thread worker = new Thread(() -> {
                try {
                    result.complete(pool.add(candidate, 10, TimeUnit.SECONDS));
                } catch (final Throwable failure) {
                    result.completeExceptionally(failure);
                }
            });
            worker.start();
            try {
                final long start = System.nanoTime();
                boolean waiting = false;
                while (!waiting && System.nanoTime() - start < TimeUnit.SECONDS.toNanos(3)) {
                    pool.lock.lock();
                    try {
                        waiting = pool.lock.hasWaiters(pool.notFull);
                    } finally {
                        pool.lock.unlock();
                    }
                    if (!waiting) {
                        Thread.sleep(1);
                    }
                }
                assertTrue(waiting);
                assertEquals(2, calls.get());
                candidate.bytes = 101;
                assertNotNull(pool.poll());
                assertTrue(result.get(3, TimeUnit.SECONDS));
                assertEquals(2, calls.get());
                assertEquals(10, pool.stats().dataSize(), "the charge is the sample taken before waiting");
            } finally {
                worker.interrupt();
                worker.join(3_000);
                assertFalse(worker.isAlive());
            }
        }
    }

    @Test
    public void expiryDuringMeasurementAndInvalidInputsDoNotAdmit() throws Exception {
        final AtomicInteger calls = new AtomicInteger();
        try (GenericObjectPool<Value> pool = new GenericObjectPool<>(1, 0, EvictionPolicy.FIFO, false, 0.5f, 100, value -> {
            calls.incrementAndGet();
            value.activityPrint().setCreatedTime(Long.MIN_VALUE);
            return 10;
        })) {
            assertThrows(IllegalArgumentException.class, () -> pool.add(null));
            assertThrows(IllegalArgumentException.class, () -> pool.add(new Value(), 0, null));
            assertEquals(0, calls.get());
            assertFalse(pool.add(new Value()));
            assertFalse(pool.add(new Value(), 0, TimeUnit.NANOSECONDS));
            assertEquals(0, pool.size());
            assertEquals(0, pool.stats().dataSize());
        }
        try (GenericKeyedObjectPool<String, Value> pool = new GenericKeyedObjectPool<>(1, 0, EvictionPolicy.FIFO, false, 0.5f, 100, (key, value) -> {
            value.activityPrint().setCreatedTime(Long.MIN_VALUE);
            return 10;
        })) {
            assertThrows(IllegalArgumentException.class, () -> pool.put(null, new Value()));
            assertThrows(IllegalArgumentException.class, () -> pool.put("", null));
            assertFalse(pool.put("", new Value()));
            assertFalse(pool.put("", new Value(), 0, TimeUnit.NANOSECONDS));
            assertEquals(0, pool.size());
            assertEquals(0, pool.stats().dataSize());
        }
    }
}

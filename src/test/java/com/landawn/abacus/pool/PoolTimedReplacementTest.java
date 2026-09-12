package com.landawn.abacus.pool;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

public class PoolTimedReplacementTest extends TestBase {
    private static final class Value extends AbstractPoolable {
        long bytes = 100;
        int destroys;

        Value() {
            super(Long.MAX_VALUE, Long.MAX_VALUE);
        }

        @Override
        public void destroy(final Caller caller) {
            destroys++;
        }
    }

    private static void awaitWaiting(final GenericKeyedObjectPool<String, Value> pool, final int count) throws Exception {
        final long start = System.nanoTime();
        while (System.nanoTime() - start < TimeUnit.SECONDS.toNanos(3)) {
            pool.lock.lock();
            try {
                if (pool.lock.getWaitQueueLength(pool.notFull) >= count) {
                    return;
                }
            } finally {
                pool.lock.unlock();
            }
            Thread.sleep(1);
        }
        fail("producer did not enter capacity wait");
    }

    @Test
    public void arrivingMappingCanBeReplacedAtCapacityWithExactMemoryLimit() throws Exception {
        for (final boolean timedArrival : new boolean[] { false, true }) {
            for (final boolean sameInstance : new boolean[] { false, true }) {
                exerciseArrival(timedArrival, sameInstance, false);
            }
        }
    }

    @Test
    public void failedReplacementOfArrivingSameInstanceRestoresItsCharge() throws Exception {
        exerciseArrival(false, true, true);
        exerciseArrival(true, true, true);
    }

    private static void exerciseArrival(final boolean timedArrival, final boolean sameInstance, final boolean reject) throws Exception {
        try (GenericKeyedObjectPool<String, Value> pool = new GenericKeyedObjectPool<>(1, 0, EvictionPolicy.FIFO, false, 0.5f, 100, (k, v) -> v.bytes)) {
            final Value initial = new Value();
            final Value candidate = new Value();
            if (reject) {
                candidate.bytes = 101;
            }
            final Value arriving = sameInstance ? candidate : new Value();
            assertTrue(pool.put("initial", initial));
            final CompletableFuture<Boolean> result = new CompletableFuture<>();
            final Thread worker = new Thread(() -> {
                try {
                    result.complete(pool.put("\uD83D\uDE80", candidate, 10, TimeUnit.SECONDS, true));
                } catch (final Throwable failure) {
                    result.completeExceptionally(failure);
                }
            });
            worker.start();
            try {
                awaitWaiting(pool, 1);
                pool.lock.lock();
                try {
                    assertSame(initial, pool.remove("initial"));
                    // The waiting admission retains its earlier sample; the competing admission
                    // obtains a fresh charge for this same instance.
                    if (reject) {
                        candidate.bytes = 100;
                    }
                    assertTrue(timedArrival ? pool.put("\uD83D\uDE80", arriving, 0, TimeUnit.SECONDS) : pool.put("\uD83D\uDE80", arriving));
                } finally {
                    pool.lock.unlock();
                }
                assertEquals(!reject, result.get(3, TimeUnit.SECONDS));
                assertSame(candidate, pool.peek("\uD83D\uDE80"));
                assertEquals(100, pool.stats().dataSize());
                assertEquals(0, candidate.destroys);
                assertEquals(sameInstance ? 0 : 1, arriving.destroys);
                assertEquals(0, pool.stats().evictionCount());
            } finally {
                worker.interrupt();
                worker.join(3_000);
                assertFalse(worker.isAlive());
            }
        }
    }

    @Test
    public void insertionWakesSameKeyProducerBehindForeignKeyProducer() throws Exception {
        try (GenericKeyedObjectPool<String, Value> pool = new GenericKeyedObjectPool<>(1, 0, EvictionPolicy.FIFO, false, 0.5f)) {
            assertTrue(pool.put("initial", new Value()));
            final CompletableFuture<Boolean> foreignResult = new CompletableFuture<>();
            final CompletableFuture<Boolean> matchingResult = new CompletableFuture<>();
            final Value replacement = new Value();
            final Thread foreign = new Thread(() -> {
                try {
                    foreignResult.complete(pool.put("foreign", new Value(), 10, TimeUnit.SECONDS));
                } catch (final InterruptedException expected) {
                    foreignResult.complete(false);
                }
            });
            final Thread matching = new Thread(() -> {
                try {
                    matchingResult.complete(pool.put("matching", replacement, 10, TimeUnit.SECONDS));
                } catch (final Throwable failure) {
                    matchingResult.completeExceptionally(failure);
                }
            });
            try {
                foreign.start();
                awaitWaiting(pool, 1);
                matching.start();
                awaitWaiting(pool, 2);
                pool.lock.lock();
                try {
                    assertNotNull(pool.remove("initial"));
                    assertTrue(pool.put("matching", new Value()));
                } finally {
                    pool.lock.unlock();
                }
                assertTrue(matchingResult.get(3, TimeUnit.SECONDS));
                assertSame(replacement, pool.peek("matching"));
                assertFalse(foreignResult.isDone());
            } finally {
                foreign.interrupt();
                matching.interrupt();
                foreign.join(3_000);
                matching.join(3_000);
                assertFalse(foreign.isAlive());
                assertFalse(matching.isAlive());
            }
        }
    }
}

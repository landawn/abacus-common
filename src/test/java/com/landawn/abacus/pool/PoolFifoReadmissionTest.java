package com.landawn.abacus.pool;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.landawn.abacus.TestBase;

public class PoolFifoReadmissionTest extends TestBase {
    private static final class Value extends AbstractPoolable {
        long charge = 10;
        int destroys;

        Value() {
            super(Long.MAX_VALUE, Long.MAX_VALUE);
        }

        @Override
        public void destroy(final Caller caller) {
            destroys++;
        }
    }

    private static boolean put(final GenericKeyedObjectPool<String, Value> pool, final String key, final Value value, final boolean timed)
            throws InterruptedException {
        return timed ? pool.put(key, value, 0, TimeUnit.NANOSECONDS, true) : pool.put(key, value, true);
    }

    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    public void rejectedMeasurementPreservesFifoOrderAndCharge(final boolean timed) throws Exception {
        for (final long rejectedCharge : new long[] { -1, 1_000 }) {
            try (GenericKeyedObjectPool<String, Value> pool = new GenericKeyedObjectPool<>(3, 0, EvictionPolicy.FIFO, true, 0.2f, 100,
                    (key, value) -> value.charge)) {
                final Value first = new Value();
                final Value second = new Value();
                final Value third = new Value();
                assertTrue(pool.put("first", first));
                assertTrue(pool.put("second", second));
                assertTrue(pool.put("third", third));
                second.charge = rejectedCharge;
                assertFalse(put(pool, new String("second"), second, timed));
                assertEquals(30, pool.stats().dataSize());
                assertEquals(3, pool.stats().putCount());
                assertEquals(0, second.destroys);
                assertEquals(List.of(first, second, third), new ArrayList<>(pool.values()));
                pool.evict();
                pool.evict();
                assertEquals(1, first.destroys);
                assertEquals(1, second.destroys);
                assertEquals(0, third.destroys);
                assertEquals(10, pool.stats().dataSize());
            }
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    public void failedBalancingPreservesOldestEntry(final boolean timed) throws Exception {
        try (GenericKeyedObjectPool<String, Value> pool = new GenericKeyedObjectPool<>(5, 0, EvictionPolicy.FIFO, true, 0.2f, 100,
                (key, value) -> value.charge)) {
            final Value first = new Value();
            assertTrue(pool.put("first", first));
            for (int i = 0; i < 4; i++) {
                assertTrue(pool.put("other" + i, new Value()));
            }
            first.charge = 95;
            assertFalse(put(pool, "first", first, timed));
            assertEquals(4, pool.size());
            assertEquals(40, pool.stats().dataSize());
            assertEquals(0, first.destroys);
            assertFalse(pool.containsKey("other0"));
            assertSame(first, pool.values().iterator().next());
            pool.evict();
            assertEquals(1, first.destroys);
            assertEquals(30, pool.stats().dataSize());
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    public void expiryDuringMeasurementKeepsOriginalOrder(final boolean timed) throws Exception {
        try (GenericKeyedObjectPool<String, Value> pool = new GenericKeyedObjectPool<>(3, 0, EvictionPolicy.FIFO, true, 0.2f, 100, (key, value) -> {
            if (value.charge == 11) {
                value.activityPrint().setCreatedTime(0).setMaxLiveTime(1);
            }
            return value.charge;
        })) {
            final Value first = new Value();
            assertTrue(pool.put("first", first));
            assertTrue(pool.put("second", new Value()));
            first.charge = 11;
            assertFalse(put(pool, "first", first, timed));
            assertEquals(20, pool.stats().dataSize());
            assertEquals(0, first.destroys);
            assertSame(first, pool.values().iterator().next());
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    public void successfulReadmissionStillMovesEntryToNewest(final boolean timed) throws Exception {
        try (GenericKeyedObjectPool<String, Value> pool = new GenericKeyedObjectPool<>(2, 0, EvictionPolicy.FIFO, false, 0.2f, 100,
                (key, value) -> value.charge)) {
            final Value first = new Value();
            final Value second = new Value();
            assertTrue(pool.put("first", first));
            assertTrue(pool.put("second", second));
            first.charge = 20;
            assertTrue(put(pool, "first", first, timed));
            assertEquals(List.of(second, first), new ArrayList<>(pool.values()));
            assertEquals(30, pool.stats().dataSize());
            assertEquals(3, pool.stats().putCount());
            pool.evict();
            assertEquals(0, first.destroys);
            assertEquals(1, second.destroys);
            assertEquals(20, pool.stats().dataSize());
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    public void successfulUnmeasuredReadmissionStillMovesEntryToNewest(final boolean timed) throws Exception {
        try (GenericKeyedObjectPool<String, Value> pool = new GenericKeyedObjectPool<>(2, 0, EvictionPolicy.FIFO, false, 0.2f)) {
            final Value first = new Value();
            final Value second = new Value();
            assertTrue(pool.put("first", first));
            assertTrue(pool.put("second", second));
            assertTrue(put(pool, "first", first, timed));
            assertEquals(List.of(second, first), new ArrayList<>(pool.values()));
            assertEquals(-1, pool.stats().dataSize());
            assertEquals(3, pool.stats().putCount());
            pool.evict();
            assertEquals(0, first.destroys);
            assertEquals(1, second.destroys);
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    public void balancingNeverSelectsReadmittedInstance(final boolean timed) throws Exception {
        for (final EvictionPolicy policy : EvictionPolicy.values()) {
            for (final float factor : new float[] { 0.2f, 1f }) {
                try (GenericKeyedObjectPool<String, Value> pool = new GenericKeyedObjectPool<>(3, 0, policy, true, factor, 100, (key, value) -> value.charge)) {
                    final Value first = new Value();
                    assertTrue(pool.put("first", first));
                    assertTrue(pool.put("second", new Value()));
                    assertTrue(pool.put("third", new Value()));
                    first.charge = 90;
                    assertTrue(put(pool, "first", first, timed));
                    assertEquals(0, first.destroys);
                    assertSame(first, pool.peek("first"));
                    assertEquals(factor == 1f ? 90 : 100, pool.stats().dataSize());
                    assertEquals(factor == 1f ? 1 : 2, pool.size());
                }
            }
        }
    }
}

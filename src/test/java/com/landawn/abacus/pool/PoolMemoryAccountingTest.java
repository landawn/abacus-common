package com.landawn.abacus.pool;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

public class PoolMemoryAccountingTest extends TestBase {
    private static final class Value implements Poolable, Serializable {
        private final ActivityPrint activity = new ActivityPrint(Long.MAX_VALUE, Long.MAX_VALUE);
        private final String text;
        private long bytes;
        private boolean failMeasure;
        private int destroyed;

        Value(final String text, final long bytes) {
            this.text = text;
            this.bytes = bytes;
        }

        long measure() {
            if (failMeasure) {
                throw new IllegalStateException("measurement unavailable");
            }
            return bytes;
        }

        @Override
        public ActivityPrint activityPrint() {
            return activity;
        }

        @Override
        public void destroy(final Caller caller) {
            destroyed++;
        }

        @Override
        public boolean equals(final Object other) {
            return other instanceof Value v && text.equals(v.text);
        }

        @Override
        public int hashCode() {
            return text.hashCode();
        }
    }

    private static GenericObjectPool<Value> objects(final EvictionPolicy policy, final long limit) {
        return new GenericObjectPool<>(10, 0, policy, false, 0.5f, limit, (ObjectPool.MemoryMeasure<Value> & Serializable) Value::measure);
    }

    private static GenericKeyedObjectPool<String, Value> keyed(final long limit) {
        return new GenericKeyedObjectPool<>(10, 0, EvictionPolicy.FIFO, false, 0.5f, limit,
                (KeyedObjectPool.MemoryMeasure<String, Value> & Serializable) (key, value) -> value.measure());
    }

    @Test
    public void objectRemovalUsesAdmissionChargeOnEveryPath() throws Exception {
        for (final String operation : new String[] { "poll", "timedPoll", "clear", "close", "evict", "expiredScan", "expiredPoll", "expiredTimedPoll" }) {
            try (GenericObjectPool<Value> pool = objects(EvictionPolicy.FIFO, 100)) {
                final Value value = new Value("\uD83D\uDE80", 10);
                assertTrue(pool.add(value));
                value.bytes = 20;
                value.failMeasure = true;
                if (operation.startsWith("expired")) {
                    value.activity.setCreatedTime(Long.MIN_VALUE);
                }
                switch (operation) {
                    case "poll" -> assertSame(value, pool.poll());
                    case "timedPoll" -> assertSame(value, pool.poll(0, TimeUnit.NANOSECONDS));
                    case "clear" -> pool.clear();
                    case "close" -> pool.close();
                    case "evict" -> pool.evict();
                    case "expiredScan" -> pool.removeExpired();
                    case "expiredPoll" -> assertNull(pool.poll());
                    case "expiredTimedPoll" -> assertNull(pool.poll(0, TimeUnit.NANOSECONDS));
                    default -> throw new AssertionError(operation);
                }
                assertEquals(0, pool.totalDataSize.get(), operation);
                assertEquals(operation.equals("poll") || operation.equals("timedPoll") ? 0 : 1, value.destroyed, operation);
                if (!pool.isClosed()) {
                    assertEquals(0, pool.stats().dataSize());
                    assertTrue(pool.add(new Value("", 100)), "detached charge must not consume capacity");
                }
            }
        }
    }

    @Test
    public void keyedRemovalUsesAdmissionChargeOnEveryPath() throws Exception {
        for (final String operation : new String[] { "remove", "replace", "timedReplace", "clear", "close", "evict", "scan", "get", "timedGet", "peek" }) {
            try (GenericKeyedObjectPool<String, Value> pool = keyed(100)) {
                final Value value = new Value("value", 10);
                final String key = "\uD83D\uDE80";
                assertTrue(pool.put(key, value));
                value.bytes = -1;
                value.failMeasure = true;
                if (operation.equals("scan") || operation.equals("get") || operation.equals("timedGet") || operation.equals("peek")) {
                    value.activity.setCreatedTime(Long.MIN_VALUE);
                }
                switch (operation) {
                    case "remove" -> assertSame(value, pool.remove(key));
                    case "replace" -> assertTrue(pool.put(key, new Value("replacement", 0)));
                    case "timedReplace" -> assertTrue(pool.put(key, new Value("replacement", 0), 0, TimeUnit.NANOSECONDS));
                    case "clear" -> pool.clear();
                    case "close" -> pool.close();
                    case "evict" -> pool.evict();
                    case "scan" -> pool.removeExpired();
                    case "get" -> assertNull(pool.get(key));
                    case "timedGet" -> assertNull(pool.get(key, 0, TimeUnit.NANOSECONDS));
                    case "peek" -> assertNull(pool.peek(key));
                    default -> throw new AssertionError(operation);
                }
                assertEquals(0, pool.totalDataSize.get(), operation);
                assertEquals(operation.equals("remove") ? 0 : 1, value.destroyed, operation);
                if (!pool.isClosed()) {
                    assertEquals(0, pool.stats().dataSize());
                    assertTrue(pool.put("", new Value("", 100)));
                }
            }
        }
    }

    @Test
    public void duplicateIdentityHasSeparateLifoAndFifoCharges() {
        try (GenericObjectPool<Value> pool = objects(EvictionPolicy.FIFO, 100)) {
            final Value value = new Value("same", 10);
            assertTrue(pool.add(value));
            value.bytes = 20;
            assertTrue(pool.add(value));
            pool.evict();
            assertEquals(20, pool.stats().dataSize(), "FIFO must remove the oldest charge");
            assertSame(value, pool.poll());
            assertEquals(0, pool.stats().dataSize());
            value.bytes = 10;
            assertTrue(pool.add(value));
            value.bytes = 20;
            assertTrue(pool.add(value));
            assertSame(value, pool.poll());
            assertEquals(10, pool.stats().dataSize(), "LIFO must remove the newest charge");
        }
    }

    @Test
    public void equalDistinctValuesAndPriorityEvictionRetainCorrectCharges() {
        try (GenericObjectPool<Value> pool = objects(EvictionPolicy.ACCESS_COUNT, 100)) {
            final Value first = new Value("equal", 10);
            final Value second = new Value("equal", 20);
            first.activity.updateAccessCount();
            assertTrue(pool.add(first));
            assertTrue(pool.add(second));
            pool.evict();
            assertEquals(10, pool.stats().dataSize());
            assertEquals(0, first.destroyed);
            assertEquals(1, second.destroyed);
            assertSame(first, pool.poll());
        }
        try (GenericObjectPool<Value> pool = objects(EvictionPolicy.ACCESS_COUNT, 100)) {
            final Value value = new Value("same", 10);
            assertTrue(pool.add(value));
            value.bytes = 20;
            assertTrue(pool.add(value));
            pool.evict();
            assertEquals(10, pool.stats().dataSize(), "priority removal selects the first identity occurrence");
        }
    }

    @Test
    public void sameKeyReadmissionUsesNewChargeAndFailureRestoresOldCharge() throws Exception {
        for (final boolean timed : new boolean[] { false, true }) {
            try (GenericKeyedObjectPool<String, Value> pool = keyed(100)) {
                final Value value = new Value("", 10);
                assertTrue(pool.put("", value));
                value.bytes = 30;
                assertTrue(timed ? pool.put("", value, 0, TimeUnit.NANOSECONDS) : pool.put("", value));
                assertEquals(30, pool.stats().dataSize());
                value.bytes = 101;
                assertFalse(timed ? pool.put("", value, 0, TimeUnit.NANOSECONDS, true) : pool.put("", value, true));
                assertSame(value, pool.peek(""));
                assertEquals(30, pool.stats().dataSize());
                value.failMeasure = true;
                assertFalse(timed ? pool.put("", value, 0, TimeUnit.NANOSECONDS, true) : pool.put("", value, true));
                assertEquals(30, pool.stats().dataSize());
                assertEquals(0, value.destroyed);
                assertSame(value, pool.remove(""));
                assertEquals(0, pool.stats().dataSize());
            }
        }
    }

    @Test
    public void sameValueUnderDifferentKeysHasSeparateCharges() {
        try (GenericKeyedObjectPool<String, Value> pool = keyed(100)) {
            final Value value = new Value("same", 10);
            assertTrue(pool.put("one", value));
            value.bytes = 20;
            assertTrue(pool.put("two", value));
            assertSame(value, pool.remove("one"));
            assertEquals(20, pool.stats().dataSize());
            assertSame(value, pool.remove("two"));
            assertEquals(0, pool.stats().dataSize());
        }
    }

    @Test
    public void unlimitedMeasuredPoolsRejectOverflowAndAcceptZero() throws Exception {
        try (GenericObjectPool<Value> pool = objects(EvictionPolicy.FIFO, 0)) {
            assertTrue(pool.add(new Value("max", Long.MAX_VALUE)));
            assertFalse(pool.add(new Value("overflow", 1)));
            assertFalse(pool.add(new Value("overflow", Long.MAX_VALUE), 0, TimeUnit.NANOSECONDS));
            assertTrue(pool.add(new Value("zero", 0)));
            assertEquals(Long.MAX_VALUE, pool.totalDataSize.get());
            pool.clear();
            assertEquals(0, pool.totalDataSize.get());
        }
        try (GenericKeyedObjectPool<String, Value> pool = keyed(0)) {
            assertTrue(pool.put("max", new Value("max", Long.MAX_VALUE)));
            assertFalse(pool.put("overflow", new Value("overflow", 1)));
            assertFalse(pool.put("overflow", new Value("overflow", Long.MAX_VALUE), 0, TimeUnit.NANOSECONDS));
            assertTrue(pool.put("zero", new Value("zero", 0)));
            assertEquals(Long.MAX_VALUE, pool.totalDataSize.get());
            pool.clear();
            assertEquals(0, pool.totalDataSize.get());
        }
    }

    @Test
    public void serializationRetainsChargesWithoutRemeasurement() throws Exception {
        try (GenericObjectPool<Value> original = objects(EvictionPolicy.FIFO, 100)) {
            final Value value = new Value("same", 10);
            assertTrue(original.add(value));
            value.bytes = 20;
            assertTrue(original.add(value));
            value.failMeasure = true;
            try (GenericObjectPool<Value> copy = copy(original)) {
                assertEquals(30, copy.stats().dataSize());
                copy.evict();
                assertEquals(20, copy.stats().dataSize());
                copy.clear();
                assertEquals(0, copy.stats().dataSize());
            }
        }
        try (GenericKeyedObjectPool<String, Value> original = keyed(100)) {
            final Value value = new Value("same", 10);
            assertTrue(original.put("", value));
            value.failMeasure = true;
            try (GenericKeyedObjectPool<String, Value> copy = copy(original)) {
                assertEquals(10, copy.stats().dataSize());
                assertNotNull(copy.remove(""));
                assertEquals(0, copy.stats().dataSize());
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static <T> T copy(final T value) throws Exception {
        final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ObjectOutputStream out = new ObjectOutputStream(bytes)) {
            out.writeObject(value);
        }
        try (ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray()))) {
            return (T) in.readObject();
        }
    }

    @Test
    public void serializedChargesDetermineUsageInsteadOfEarlierSuperclassCounter() throws Exception {
        try (GenericObjectPool<Value> original = objects(EvictionPolicy.FIFO, 100);
             GenericKeyedObjectPool<String, Value> keyedOriginal = keyed(100)) {
            assertTrue(original.add(new Value("", 10)));
            assertTrue(keyedOriginal.put("", new Value("", 20)));
            // The superclass is written before the subclass's locked snapshot. Simulate the
            // counter having been sampled before a concurrent change to the stored entries.
            original.totalDataSize.set(99);
            keyedOriginal.totalDataSize.set(99);
            try {
                try (GenericObjectPool<Value> restored = copy(original);
                     GenericKeyedObjectPool<String, Value> keyedRestored = copy(keyedOriginal)) {
                    assertEquals(10, restored.stats().dataSize());
                    assertEquals(20, keyedRestored.stats().dataSize());
                }
            } finally {
                original.totalDataSize.set(10);
                keyedOriginal.totalDataSize.set(20);
            }
        }
    }

    @Test
    public void oldMeasuredStreamsWithoutChargesRequireRecreationOnlyWhenNonempty() throws Exception {
        try (GenericObjectPool<Value> original = objects(EvictionPolicy.FIFO, 100);
             GenericKeyedObjectPool<String, Value> keyedOriginal = keyed(100)) {
            for (final AbstractPool pool : new AbstractPool[] { original, keyedOriginal }) {
                final Field charges = pool.getClass().getDeclaredField("memoryCharges");
                charges.setAccessible(true);
                final Object saved = charges.get(pool);
                charges.set(pool, null); // Missing fields in older serialized forms deserialize as null.
                try (AbstractPool restored = copy(pool)) {
                    assertEquals(0, restored.stats().dataSize());
                } finally {
                    charges.set(pool, saved);
                }
            }
            assertTrue(original.add(new Value("", 10)));
            assertTrue(keyedOriginal.put("", new Value("", 20)));
            for (final AbstractPool pool : new AbstractPool[] { original, keyedOriginal }) {
                final Field charges = pool.getClass().getDeclaredField("memoryCharges");
                charges.setAccessible(true);
                final Object saved = charges.get(pool);
                charges.set(pool, null);
                try {
                    assertThrows(InvalidObjectException.class, () -> copy(pool));
                } finally {
                    charges.set(pool, saved);
                }
            }
        }
    }
}

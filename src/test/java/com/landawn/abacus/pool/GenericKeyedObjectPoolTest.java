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
import java.io.IOException;
import java.io.NotSerializableException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Field;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

public class GenericKeyedObjectPoolTest extends TestBase {

    private static final class SpuriousWakeupCondition implements Condition {
        private final int spuriousWakeups;
        private int awaitCount;

        private SpuriousWakeupCondition(final int spuriousWakeups) {
            this.spuriousWakeups = spuriousWakeups;
        }

        @Override
        public long awaitNanos(final long nanosTimeout) {
            return ++awaitCount <= spuriousWakeups ? 1 : 0;
        }

        @Override
        public void await() {
            throw new UnsupportedOperationException();
        }

        @Override
        public void awaitUninterruptibly() {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean await(final long time, final TimeUnit unit) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean awaitUntil(final Date deadline) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void signal() {
        }

        @Override
        public void signalAll() {
        }
    }

    private static final class AtomicEvictPool extends GenericKeyedObjectPool<String, TestPoolable> {
        private final AtomicReference<TestPoolable> removedDuringFormerGap;

        private AtomicEvictPool(final AtomicReference<TestPoolable> removedDuringFormerGap) {
            super(8, 0, EvictionPolicy.FIFO, true, 0.5f);
            this.removedDuringFormerGap = removedDuringFormerGap;
        }

        @Override
        protected void vacate(final int numberToEvict) {
            final Thread interloper = new Thread(() -> removedDuringFormerGap.set(remove("one")), "keyed-evict-gap-interloper");
            interloper.start();
            try {
                interloper.join(2_000);
            } catch (final InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new AssertionError(e);
            }
            super.vacate(numberToEvict);
        }
    }

    private static final class DeserializationEvictionProbePool extends GenericKeyedObjectPool<String, TestPoolable> {
        private static final AtomicBoolean observedPartiallyDeserializedState = new AtomicBoolean();
        private static volatile CountDownLatch evictionRan = new CountDownLatch(1);

        private boolean subclassStateInitialized = true;
        private transient volatile boolean deserializationProbeEnabled;

        private DeserializationEvictionProbePool() {
            super(2, 1, EvictionPolicy.LAST_ACCESS_TIME);
        }

        private void readObject(final ObjectInputStream input) throws IOException, ClassNotFoundException {
            deserializationProbeEnabled = true;
            try {
                Thread.sleep(100);
            } catch (final InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IOException(e);
            }
            input.defaultReadObject();
        }

        @Override
        protected void removeExpired() {
            if (deserializationProbeEnabled) {
                if (!subclassStateInitialized) {
                    observedPartiallyDeserializedState.set(true);
                }
                evictionRan.countDown();
            }
            super.removeExpired();
        }

        private static void resetProbe() {
            observedPartiallyDeserializedState.set(false);
            evictionRan = new CountDownLatch(1);
        }
    }

    private static class TestPoolable extends AbstractPoolable {
        private final String value;
        private boolean destroyed;
        private Poolable.Caller destroyedByCaller;

        TestPoolable(String value) {
            this(value, 10000, 5000);
        }

        TestPoolable(String value, long liveTime, long maxIdleTime) {
            super(liveTime, maxIdleTime);
            this.value = value;
        }

        @Override
        public void destroy(Poolable.Caller caller) {
            destroyed = true;
            destroyedByCaller = caller;
        }

        String getValue() {
            return value;
        }

        boolean isDestroyed() {
            return destroyed;
        }

        Poolable.Caller getDestroyedByCaller() {
            return destroyedByCaller;
        }
    }

    /** Records whether {@code hashCode()} ran while the pool lock was held by this thread. */
    private static final class LockAwareKey {
        private final String id;
        AbstractPool pool;
        int hashCount;
        int hashesWhilePoolLockHeld;

        LockAwareKey(final String id) {
            this.id = id;
        }

        void resetCounts() {
            hashCount = 0;
            hashesWhilePoolLockHeld = 0;
        }

        @Override
        public int hashCode() {
            hashCount++;
            if (pool != null && pool.lock.isHeldByCurrentThread()) {
                hashesWhilePoolLockHeld++;
            }
            return id.hashCode();
        }

        @Override
        public boolean equals(final Object obj) {
            return obj instanceof LockAwareKey other && id.equals(other.id);
        }
    }

    /** Records whether a pooled value is hashed while the pool lock is held by this thread. */
    private static final class LockAwarePoolable extends TestPoolable {
        AbstractPool pool;
        int hashCount;
        int hashesWhilePoolLockHeld;

        LockAwarePoolable(final String value) {
            super(value);
        }

        void resetCounts() {
            hashCount = 0;
            hashesWhilePoolLockHeld = 0;
        }

        @Override
        public int hashCode() {
            hashCount++;
            if (pool != null && pool.lock.isHeldByCurrentThread()) {
                hashesWhilePoolLockHeld++;
            }
            return super.hashCode();
        }
    }

    private GenericKeyedObjectPool<String, TestPoolable> pool;

    @BeforeEach
    public void setUp() {
        pool = new GenericKeyedObjectPool<>(10, 0, EvictionPolicy.LAST_ACCESS_TIME);
    }

    @AfterEach
    public void tearDown() {
        if (pool != null && !pool.isClosed()) {
            pool.close();
        }
    }

    private static GenericKeyedObjectPool<String, TestPoolable> noBalance(int capacity) {
        return new GenericKeyedObjectPool<>(capacity, 0, EvictionPolicy.LAST_ACCESS_TIME, false, 0.2f);
    }

    private static byte[] serialize(final Object obj) throws IOException {
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ObjectOutputStream oos = new ObjectOutputStream(baos)) {
            oos.writeObject(obj);
        }
        return baos.toByteArray();
    }

    @SuppressWarnings("unchecked")
    private static <T> T deserialize(final byte[] bytes) throws IOException, ClassNotFoundException {
        try (ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(bytes))) {
            return (T) ois.readObject();
        }
    }

    private static Object shutdownHookOf(final Object target) throws Exception {
        final Field field = AbstractPool.class.getDeclaredField("shutdownHook");
        field.setAccessible(true);
        return field.get(target);
    }

    private static void awaitCleanupRelease(final CountDownLatch release) {
        try {
            release.await(5, TimeUnit.SECONDS);
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static void assertSameValue(TestPoolable expected, TestPoolable actual) {
        assertNotNull(actual);
        assertEquals(expected.getValue(), actual.getValue());
    }

    @Test
    public void testPut() {
        TestPoolable poolable = new TestPoolable("value1");
        assertTrue(pool.put("key1", poolable));
        assertEquals(1, pool.size());
        assertTrue(pool.containsKey("key1"));
        assertEquals(1, pool.stats().putCount());

        TestPoolable p1 = new TestPoolable("value1");
        TestPoolable p2 = new TestPoolable("value2");
        assertTrue(pool.put("replace", p1));
        assertTrue(pool.put("replace", p2));
        assertEquals(2, pool.size());
        assertTrue(p1.isDestroyed());
        assertEquals(Poolable.Caller.REMOVE_REPLACE_CLEAR, p1.getDestroyedByCaller());
        assertEquals(p2, pool.get("replace"));

        GenericKeyedObjectPool<String, TestPoolable> noBalancePool = noBalance(3);
        try {
            for (int i = 0; i < 3; i++) {
                assertTrue(noBalancePool.put("key" + i, new TestPoolable("value" + i)));
            }
            assertFalse(noBalancePool.put("key3", new TestPoolable("value3")));
            assertEquals(3, noBalancePool.size());
        } finally {
            noBalancePool.close();
        }

        GenericKeyedObjectPool<String, TestPoolable> balancePool = new GenericKeyedObjectPool<>(3, 0, EvictionPolicy.LAST_ACCESS_TIME, true, 0.4f);
        try {
            for (int i = 0; i < 3; i++) {
                assertTrue(balancePool.put("key" + i, new TestPoolable("value" + i)));
            }
            assertTrue(balancePool.put("key3", new TestPoolable("value3")));
            assertEquals(3, balancePool.size());
        } finally {
            balancePool.close();
        }

        GenericKeyedObjectPool<String, TestPoolable> small = new GenericKeyedObjectPool<>(1, 0, EvictionPolicy.LAST_ACCESS_TIME, true, 0.2f);
        try {
            TestPoolable first = new TestPoolable("value1");
            assertTrue(small.put("key1", first));
            assertTrue(small.put("key2", new TestPoolable("value2")));
            assertEquals(1, small.size());
            assertTrue(first.isDestroyed());
        } finally {
            small.close();
        }

        KeyedObjectPool.MemoryMeasure<String, TestPoolable> measure = (k, v) -> k.length() + 100;
        GenericKeyedObjectPool<String, TestPoolable> memPool = new GenericKeyedObjectPool<>(10, 0, EvictionPolicy.LAST_ACCESS_TIME, 250, measure);
        try {
            assertTrue(memPool.put("k1", new TestPoolable("v1")));
            assertTrue(memPool.put("k2", new TestPoolable("v2")));
            assertTrue(memPool.put("k3", new TestPoolable("v3")));
            assertEquals(2, memPool.size());
        } finally {
            memPool.close();
        }

        GenericKeyedObjectPool<String, TestPoolable> unlimited = new GenericKeyedObjectPool<>(10, 0, EvictionPolicy.LAST_ACCESS_TIME, 0, measure);
        try {
            for (int i = 0; i < 5; i++) {
                assertTrue(unlimited.put("key" + i, new TestPoolable("value" + i)));
            }
            assertEquals(5, unlimited.size());
        } finally {
            unlimited.close();
        }

        GenericKeyedObjectPool<String, TestPoolable> memNoBalance = new GenericKeyedObjectPool<>(10, 0, EvictionPolicy.LAST_ACCESS_TIME, false, 0.2f, 250,
                measure);
        try {
            assertTrue(memNoBalance.put("k1", new TestPoolable("v1")));
            assertTrue(memNoBalance.put("k2", new TestPoolable("v2")));
            assertFalse(memNoBalance.put("k3", new TestPoolable("v3")));
            assertEquals(2, memNoBalance.size());
        } finally {
            memNoBalance.close();
        }

        for (int i = 0; i < 10; i++) {
            pool.put("full" + i, new TestPoolable("value" + i));
        }
        TestPoolable extra = new TestPoolable("extra");
        assertTrue(pool.put("full10", extra, true));
        assertFalse(extra.isDestroyed());
    }

    @Test
    public void testPut_EdgeCase() throws InterruptedException {
        assertThrows(IllegalArgumentException.class, () -> pool.put(null, new TestPoolable("value")));
        assertThrows(IllegalArgumentException.class, () -> pool.put("key", null));
        TestPoolable expired = new TestPoolable("expired", 10, 10);
        Thread.sleep(20);
        assertFalse(pool.put("key", expired));
        assertEquals(0, pool.size());

        KeyedObjectPool.MemoryMeasure<String, TestPoolable> negative = (k, v) -> -1;
        GenericKeyedObjectPool<String, TestPoolable> memPool = new GenericKeyedObjectPool<>(10, 0, EvictionPolicy.LAST_ACCESS_TIME, 1000, negative);
        try {
            assertFalse(memPool.put("key1", new TestPoolable("v1")));
            assertEquals(0, memPool.size());
        } finally {
            memPool.close();
        }
    }

    @Test
    public void testPut_Closed() throws InterruptedException {
        GenericKeyedObjectPool<String, TestPoolable> closedPool = new GenericKeyedObjectPool<>(1, 0, EvictionPolicy.LAST_ACCESS_TIME);
        TestPoolable expired = new TestPoolable("expired", 1, 1);
        Thread.sleep(20);
        closedPool.close();
        assertThrows(IllegalStateException.class, () -> closedPool.put("key", expired));

        pool.close();
        assertThrows(IllegalStateException.class, () -> pool.put("key", new TestPoolable("value")));
        assertThrows(IllegalStateException.class, () -> pool.put("k", new TestPoolable("v"), 1, TimeUnit.SECONDS));
    }

    @Test
    public void testPut_AutoDestroyFailure() {
        GenericKeyedObjectPool<String, TestPoolable> noBalancePool = noBalance(2);
        try {
            noBalancePool.put("key1", new TestPoolable("value1"));
            noBalancePool.put("key2", new TestPoolable("value2"));
            TestPoolable keep = new TestPoolable("keep");
            assertFalse(noBalancePool.put("key3", keep, false));
            assertFalse(keep.isDestroyed());
            TestPoolable extra = new TestPoolable("extra");
            assertFalse(noBalancePool.put("key3", extra, true));
            assertTrue(extra.isDestroyed());
            assertEquals(Poolable.Caller.PUT_ADD_FAILURE, extra.getDestroyedByCaller());
        } finally {
            noBalancePool.close();
        }
    }

    @Test
    public void testPut_MemoryMeasureThrows() {
        AtomicBoolean throwOnNext = new AtomicBoolean();
        KeyedObjectPool.MemoryMeasure<String, TestPoolable> measure = (k, v) -> {
            if (throwOnNext.get()) {
                throw new RuntimeException("sizeOf");
            }
            return 100L;
        };
        GenericKeyedObjectPool<String, TestPoolable> memPool = new GenericKeyedObjectPool<>(10, 0, EvictionPolicy.LAST_ACCESS_TIME, true, 0.2f, 1024L * 1024L,
                measure);
        try {
            TestPoolable original = new TestPoolable("memMeasurePut");
            assertTrue(memPool.put("k1", original));
            throwOnNext.set(true);
            assertFalse(memPool.put("k1", new TestPoolable("shouldNotBePut")));
            assertEquals(0, memPool.size());
        } finally {
            memPool.close();
        }
    }

    @Test
    public void testPut_ValueExpiringDuringMeasurement() throws InterruptedException {
        KeyedObjectPool.MemoryMeasure<String, TestPoolable> slow = (k, v) -> {
            try {
                Thread.sleep(200);
            } catch (final InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return 1L;
        };
        GenericKeyedObjectPool<String, TestPoolable> memPool = new GenericKeyedObjectPool<>(10, 0, EvictionPolicy.LAST_ACCESS_TIME, 1_000_000L, slow);
        try {
            TestPoolable shortLived = new TestPoolable("v", 50, 50);
            assertFalse(memPool.put("k", shortLived));
            assertEquals(0, memPool.size());
            assertFalse(memPool.put("k", new TestPoolable("v", 50, 50), 1, TimeUnit.SECONDS));
            assertEquals(0, memPool.size());
        } finally {
            memPool.close();
        }
    }

    @Test
    public void testPut_SameInstance() {
        TestPoolable resource = new TestPoolable("res");
        assertTrue(pool.put("k", resource));
        assertSame(resource, pool.get("k"));
        assertTrue(pool.put("k", resource));
        assertFalse(resource.isDestroyed());
        TestPoolable replacement = new TestPoolable("res2");
        assertTrue(pool.put("k", replacement));
        assertTrue(resource.isDestroyed());

        KeyedObjectPool.MemoryMeasure<String, TestPoolable> measure = (k, v) -> k.length() + 100;
        GenericKeyedObjectPool<String, TestPoolable> memPool = new GenericKeyedObjectPool<>(10, 0, EvictionPolicy.LAST_ACCESS_TIME, 10000, measure);
        try {
            TestPoolable value = new TestPoolable("res");
            assertTrue(memPool.put("k1", value));
            assertEquals(102, memPool.stats().dataSize());
            assertTrue(memPool.put("k1", memPool.get("k1")));
            assertEquals(102, memPool.stats().dataSize());
            memPool.remove("k1");
            assertEquals(0, memPool.stats().dataSize());
        } finally {
            memPool.close();
        }

        AtomicInteger measureCalls = new AtomicInteger();
        KeyedObjectPool.MemoryMeasure<String, TestPoolable> failing = (k, v) -> measureCalls.incrementAndGet() == 1 ? 100 : -1;
        GenericKeyedObjectPool<String, TestPoolable> restore = new GenericKeyedObjectPool<>(10, 0, EvictionPolicy.LAST_ACCESS_TIME, 10_000, failing);
        try {
            TestPoolable value = new TestPoolable("v1");
            assertTrue(restore.put("k", value));
            TestPoolable same = restore.get("k");
            assertFalse(restore.put("k", same, true));
            assertEquals(1, restore.size());
            assertEquals(100L, restore.totalDataSize.get());
            assertFalse(same.isDestroyed());
        } finally {
            restore.close();
        }
    }

    @Test
    public void testPut_CapacityAndMemoryFailurePreserveExisting() {
        GenericKeyedObjectPool<String, TestPoolable> capacityPool = noBalance(1);
        try {
            TestPoolable v1 = new TestPoolable("value1");
            assertTrue(capacityPool.put("K1", v1));
            assertFalse(capacityPool.put("K2", new TestPoolable("value2")));
            assertFalse(v1.isDestroyed());
            assertTrue(capacityPool.containsKey("K1"));
        } finally {
            capacityPool.close();
        }

        KeyedObjectPool.MemoryMeasure<String, TestPoolable> measure = (k, v) -> 100L;
        GenericKeyedObjectPool<String, TestPoolable> memPool = new GenericKeyedObjectPool<>(10, 0, EvictionPolicy.LAST_ACCESS_TIME, false, 0.2f, 150, measure);
        try {
            TestPoolable p1 = new TestPoolable("value1");
            assertTrue(memPool.put("K1", p1));
            assertFalse(memPool.put("K2", new TestPoolable("value2")));
            assertFalse(p1.isDestroyed());
            assertEquals(1, memPool.size());
        } finally {
            memPool.close();
        }
    }

    @Test
    public void testGet() throws InterruptedException {
        TestPoolable poolable = new TestPoolable("value1");
        pool.put("key1", poolable);
        TestPoolable retrieved = pool.get("key1");
        assertEquals("value1", retrieved.getValue());
        assertEquals(1, retrieved.activityPrint().getAccessCount());
        pool.get("key1");
        pool.get("key1");
        assertEquals(3, poolable.activityPrint().getAccessCount());
        assertNull(pool.get("nonexistent"));

        TestPoolable expired = new TestPoolable("expired", 50, 50);
        pool.put("expired", expired);
        Thread.sleep(60);
        assertNull(pool.get("expired"));
        assertTrue(expired.isDestroyed());
        assertEquals(Poolable.Caller.EVICT, expired.getDestroyedByCaller());
        assertFalse(pool.containsKey("expired"));
    }

    @Test
    public void testGet_Closed() {
        pool.close();
        assertThrows(IllegalStateException.class, () -> pool.get("key"));
        assertThrows(IllegalStateException.class, () -> pool.get("k", 1, TimeUnit.SECONDS));
    }

    @Test
    public void testGet_HitMissStats() throws InterruptedException {
        pool.put("key1", new TestPoolable("value1"));
        pool.get("key1");
        pool.get("nonexistent");
        assertEquals(1, pool.stats().hitCount());
        assertEquals(1, pool.stats().missCount());

        GenericKeyedObjectPool<String, TestPoolable> p = new GenericKeyedObjectPool<>(10, 0, EvictionPolicy.LAST_ACCESS_TIME);
        try {
            p.put("k1", new TestPoolable("v1"));
            assertNotNull(p.get("k1"));
            assertNull(p.get("absent"));
            p.put("short", new TestPoolable("s", 1, 1));
            Thread.sleep(20);
            assertNull(p.get("short"));
            assertEquals(1, p.hitCount.get());
            assertEquals(2, p.missCount.get());
            p.close();
            assertThrows(IllegalStateException.class, () -> p.get("k1"));
            assertEquals(1, p.hitCount.get());
            assertEquals(2, p.missCount.get());
        } finally {
            if (!p.isClosed()) {
                p.close();
            }
        }
    }

    @Test
    public void testGet_RaceWithClose() throws Exception {
        assertRaceWithClose(op -> op.get("K"));
        assertRaceWithClose(op -> op.remove("K"));
        assertRaceWithClose(op -> op.peek("K"));
    }

    private static void assertRaceWithClose(java.util.function.Function<GenericKeyedObjectPool<String, TestPoolable>, TestPoolable> op) throws Exception {
        GenericKeyedObjectPool<String, TestPoolable> racing = new GenericKeyedObjectPool<>(10, 0, EvictionPolicy.LAST_ACCESS_TIME);
        racing.put("K", new TestPoolable("racing"));
        ReentrantLock poolLock = racing.lock;
        CountDownLatch holderHasLock = new CountDownLatch(1);
        CountDownLatch mayProceed = new CountDownLatch(1);
        Thread holder = new Thread(() -> {
            poolLock.lock();
            try {
                holderHasLock.countDown();
                mayProceed.await();
                racing.isClosed = true;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                poolLock.unlock();
            }
        });
        holder.start();
        holderHasLock.await();
        AtomicBoolean threwISE = new AtomicBoolean();
        AtomicBoolean returned = new AtomicBoolean();
        Thread actor = new Thread(() -> {
            try {
                if (op.apply(racing) != null) {
                    returned.set(true);
                }
            } catch (IllegalStateException e) {
                threwISE.set(true);
            }
        });
        actor.start();
        Thread.sleep(50);
        mayProceed.countDown();
        holder.join();
        actor.join();
        assertTrue(threwISE.get());
        assertFalse(returned.get());
        racing.isClosed = false;
        racing.close();
    }

    @Test
    public void testRemove() {
        TestPoolable poolable = new TestPoolable("value1");
        pool.put("key1", poolable);
        TestPoolable removed = pool.remove("key1");
        assertSame(poolable, removed);
        assertEquals(0, pool.size());
        assertEquals(1, removed.activityPrint().getAccessCount());
        assertNull(pool.remove("nonexistent"));

        KeyedObjectPool.MemoryMeasure<String, TestPoolable> measure = (k, v) -> k.length() + 100;
        GenericKeyedObjectPool<String, TestPoolable> memPool = new GenericKeyedObjectPool<>(10, 0, EvictionPolicy.LAST_ACCESS_TIME, 1000, measure);
        try {
            memPool.put("k1", new TestPoolable("v1"));
            memPool.put("k2", new TestPoolable("v2"));
            assertEquals(204, memPool.stats().dataSize());
            memPool.remove("k1");
            assertEquals(102, memPool.stats().dataSize());
        } finally {
            memPool.close();
        }
    }

    @Test
    public void testRemove_Closed() {
        pool.close();
        assertThrows(IllegalStateException.class, () -> pool.remove("key"));
    }

    @Test
    public void testRemove_MemoryMeasureThrows() {
        AtomicBoolean throwOnNext = new AtomicBoolean();
        KeyedObjectPool.MemoryMeasure<String, TestPoolable> measure = (k, v) -> {
            if (throwOnNext.get()) {
                throw new RuntimeException("sizeOf");
            }
            return 0L;
        };
        GenericKeyedObjectPool<String, TestPoolable> memPool = new GenericKeyedObjectPool<>(10, 0, EvictionPolicy.LAST_ACCESS_TIME, true, 0.2f, 1024L * 1024L,
                measure);
        try {
            TestPoolable original = new TestPoolable("memMeasureRemove");
            assertTrue(memPool.put("k1", original));
            throwOnNext.set(true);
            TestPoolable removed = memPool.remove("k1");
            assertNotNull(removed);
            assertFalse(removed.isDestroyed());
            assertEquals(0, memPool.size());
        } finally {
            memPool.close();
        }
    }

    @Test
    public void testPeek() throws InterruptedException {
        TestPoolable poolable = new TestPoolable("value1");
        pool.put("key1", poolable);
        assertSame(poolable, pool.peek("key1"));
        assertEquals(0, poolable.activityPrint().getAccessCount());
        assertEquals(1, pool.size());
        assertNull(pool.peek("nonexistent"));

        TestPoolable expired = new TestPoolable("expired", 50, 50);
        pool.put("expired", expired);
        Thread.sleep(60);
        assertNull(pool.peek("expired"));
        assertTrue(expired.isDestroyed());
        assertEquals(Poolable.Caller.EVICT, expired.getDestroyedByCaller());
    }

    @Test
    public void testPeek_Closed() {
        pool.close();
        assertThrows(IllegalStateException.class, () -> pool.peek("key"));
    }

    @Test
    public void testContainsKey() {
        pool.put("key1", new TestPoolable("value1"));
        pool.put("key2", new TestPoolable("value2"));
        assertTrue(pool.containsKey("key1"));
        assertFalse(pool.containsKey("key3"));
        pool.close();
        assertThrows(IllegalStateException.class, () -> pool.containsKey("key"));
    }

    @Test
    public void testKeySet() {
        pool.put("key1", new TestPoolable("value1"));
        pool.put("key2", new TestPoolable("value2"));
        pool.put("key3", new TestPoolable("value3"));
        Set<String> keys = pool.keySet();
        assertEquals(3, keys.size());
        keys.clear();
        assertEquals(3, pool.size());
        pool.close();
        assertThrows(IllegalStateException.class, () -> pool.keySet());
    }

    @Test
    public void testValues() {
        TestPoolable p1 = new TestPoolable("value1");
        TestPoolable p2 = new TestPoolable("value2");
        pool.put("key1", p1);
        pool.put("key2", p2);
        Collection<TestPoolable> values = pool.values();
        assertEquals(2, values.size());
        values.clear();
        assertEquals(2, pool.size());
        pool.close();
        assertThrows(IllegalStateException.class, () -> pool.values());
    }

    @Test
    public void testClear() {
        List<TestPoolable> items = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            TestPoolable item = new TestPoolable("value" + i);
            items.add(item);
            pool.put("key" + i, item);
        }
        pool.clear();
        assertEquals(0, pool.size());
        for (TestPoolable item : items) {
            assertTrue(item.isDestroyed());
            assertEquals(Poolable.Caller.REMOVE_REPLACE_CLEAR, item.getDestroyedByCaller());
        }
        pool.clear();
        assertTrue(pool.isEmpty());
    }

    @Test
    public void testClear_Closed() {
        pool.close();
        assertThrows(IllegalStateException.class, () -> pool.clear());
    }

    @Test
    public void testClear_AccountsBeforeUnlock() throws Exception {
        assertRemovalAccountingVisibleBeforeCallback(true);
        assertRemovalAccountingVisibleBeforeCallback(false);
    }

    private static void assertRemovalAccountingVisibleBeforeCallback(final boolean clear) throws Exception {
        final CountDownLatch destructionPhaseReached = new CountDownLatch(1);
        final CountDownLatch releaseDestruction = new CountDownLatch(1);
        final AtomicReference<Throwable> cleanupFailure = new AtomicReference<>();
        final AtomicReference<Boolean> lockHeldDuringCallback = new AtomicReference<>();

        class AccountingProbePool extends GenericKeyedObjectPool<String, TestPoolable> {
            AccountingProbePool() {
                super(2, 0, EvictionPolicy.LAST_ACCESS_TIME, false, 0.2f, 10L, (key, value) -> 10L);
            }

            @Override
            protected void destroyAll(final Map<String, TestPoolable> values, final Poolable.Caller caller) {
                destructionPhaseReached.countDown();
                awaitCleanupRelease(releaseDestruction);
                super.destroyAll(values, caller);
            }
        }

        final AccountingProbePool p = new AccountingProbePool();
        final TestPoolable victim = new TestPoolable("victim") {
            @Override
            public void destroy(final Poolable.Caller caller) {
                lockHeldDuringCallback.set(p.lock.isHeldByCurrentThread());
                destructionPhaseReached.countDown();
                awaitCleanupRelease(releaseDestruction);
                super.destroy(caller);
            }
        };
        assertTrue(p.put("victim", victim));
        if (!clear) {
            victim.activityPrint().setCreatedTime(System.currentTimeMillis() - 20_000);
        }
        final Thread cleanup = new Thread(() -> {
            try {
                if (clear) {
                    p.clear();
                } else {
                    p.removeExpired();
                }
            } catch (final Throwable e) {
                cleanupFailure.set(e);
            }
        });
        cleanup.start();
        try {
            assertTrue(destructionPhaseReached.await(2, TimeUnit.SECONDS));
            assertTrue(p.put("replacement", new TestPoolable("replacement")));
            assertEquals(10L, p.totalDataSize.get());
        } finally {
            releaseDestruction.countDown();
            cleanup.join(2_000);
            if (!p.isClosed()) {
                p.close();
            }
        }
        assertFalse(cleanup.isAlive());
        assertNull(cleanupFailure.get());
        assertEquals(Boolean.FALSE, lockHeldDuringCallback.get());
        assertEquals(0L, p.totalDataSize.get());
    }

    @Test
    public void testConstructor() {
        GenericKeyedObjectPool<String, TestPoolable> basic = new GenericKeyedObjectPool<>(50, 3000, EvictionPolicy.ACCESS_COUNT);
        try {
            assertEquals(50, basic.capacity());
            assertEquals(0, basic.size());
        } finally {
            basic.close();
        }
        KeyedObjectPool.MemoryMeasure<String, TestPoolable> measure = (k, v) -> k.length() + 100;
        GenericKeyedObjectPool<String, TestPoolable> mem = new GenericKeyedObjectPool<>(20, 2000, EvictionPolicy.EXPIRATION_TIME, 1024, measure);
        try {
            assertEquals(20, mem.capacity());
        } finally {
            mem.close();
        }
        GenericKeyedObjectPool<String, TestPoolable> full = new GenericKeyedObjectPool<>(40, 4000, EvictionPolicy.ACCESS_COUNT, true, 0.4f, 2048, measure);
        try {
            assertEquals(40, full.capacity());
        } finally {
            full.close();
        }
    }

    @Test
    public void testSerialization() throws Exception {
        GenericKeyedObjectPool<String, TestPoolable> evictPool = new GenericKeyedObjectPool<>(10, 100, EvictionPolicy.LAST_ACCESS_TIME);
        GenericKeyedObjectPool<String, TestPoolable> deserialized = deserialize(serialize(evictPool));
        try {
            assertNotNull(deserialized);
            assertFalse(deserialized.isClosed());
            assertNotNull(shutdownHookOf(deserialized));
        } finally {
            evictPool.close();
            deserialized.close();
        }

        TestPoolable value = new TestPoolable("v1", 600_000, 60_000);
        value.activityPrint().updateAccessCount();
        assertTrue(pool.put("k1", value));
        assertThrows(NotSerializableException.class, () -> serialize(pool));

        GenericKeyedObjectPool<String, PoolableAdapter<String>> adapterPool = new GenericKeyedObjectPool<>(10, 0, EvictionPolicy.LAST_ACCESS_TIME);
        try {
            PoolableAdapter<String> adapter = Poolable.wrap("hello", 600_000, 60_000);
            adapter.activityPrint().updateAccessCount();
            assertTrue(adapterPool.put("k1", adapter));
            assertTrue(adapterPool.put("k2", Poolable.wrap("汉字 😀")));
            assertThrows(NotSerializableException.class, () -> serialize(adapterPool));
        } finally {
            adapterPool.close();
        }

        KeyedObjectPool.MemoryMeasure<String, PoolableAdapter<String>> measure = (KeyedObjectPool.MemoryMeasure<String, PoolableAdapter<String>> & java.io.Serializable) (
                k, v) -> v.value().length();
        GenericKeyedObjectPool<String, PoolableAdapter<String>> measured = new GenericKeyedObjectPool<>(10, 0, EvictionPolicy.LAST_ACCESS_TIME, 1000, measure);
        try {
            measured.put("a", Poolable.wrap("abc"));
            measured.put("b", Poolable.wrap("defgh"));
            assertThrows(NotSerializableException.class, () -> serialize(measured));
        } finally {
            measured.close();
        }

        GenericKeyedObjectPool<String, PoolableAdapter<Object>> failPool = new GenericKeyedObjectPool<>(10, 0, EvictionPolicy.LAST_ACCESS_TIME);
        try {
            assertTrue(failPool.put("k", PoolableAdapter.of(new Object())));
            assertThrows(NotSerializableException.class, () -> serialize(failPool));
            assertEquals(1, failPool.size());
        } finally {
            failPool.close();
        }
    }

    @Test
    public void testDeserializationDefersEviction() throws Exception {
        DeserializationEvictionProbePool original = new DeserializationEvictionProbePool();
        byte[] bytes = serialize(original);
        original.close();
        DeserializationEvictionProbePool.resetProbe();
        DeserializationEvictionProbePool deserialized = deserialize(bytes);
        try {
            assertTrue(DeserializationEvictionProbePool.evictionRan.await(1, TimeUnit.SECONDS));
            assertFalse(DeserializationEvictionProbePool.observedPartiallyDeserializedState.get());
        } finally {
            deserialized.close();
        }
    }

    @Test
    public void testClose() {
        TestPoolable p1 = new TestPoolable("value1");
        TestPoolable p2 = new TestPoolable("value2");
        pool.put("key1", p1);
        pool.put("key2", p2);
        pool.close();
        assertTrue(pool.isClosed());
        assertTrue(p1.isDestroyed());
        assertEquals(Poolable.Caller.CLOSE, p1.getDestroyedByCaller());
        assertThrows(IllegalStateException.class, () -> pool.put("key3", new TestPoolable("value3")));
        pool.close();
        assertTrue(pool.isClosed());
    }

    @Test
    public void testClose_ContinuesAfterDestroyError() {
        TestPoolable broken = new TestPoolable("broken") {
            @Override
            public void destroy(final Poolable.Caller caller) {
                throw new AssertionError("simulated callback failure");
            }
        };
        TestPoolable survivor = new TestPoolable("survivor");
        assertTrue(pool.put("broken", broken));
        assertTrue(pool.put("survivor", survivor));
        pool.close();
        assertTrue(survivor.isDestroyed());
    }

    @Test
    public void testConcurrentCloseWaits() throws InterruptedException {
        GenericKeyedObjectPool<String, TestPoolable> p = new GenericKeyedObjectPool<>(1, 0, EvictionPolicy.LAST_ACCESS_TIME);
        CountDownLatch destroyStarted = new CountDownLatch(1);
        CountDownLatch allowDestroyToFinish = new CountDownLatch(1);
        CountDownLatch secondCloseReturned = new CountDownLatch(1);
        TestPoolable element = new TestPoolable("blocking") {
            @Override
            public void destroy(final Poolable.Caller caller) {
                destroyStarted.countDown();
                try {
                    allowDestroyToFinish.await();
                } catch (final InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                super.destroy(caller);
            }
        };
        assertTrue(p.put("key", element));
        Thread first = new Thread(p::close);
        Thread second = new Thread(() -> {
            p.close();
            secondCloseReturned.countDown();
        });
        first.start();
        assertTrue(destroyStarted.await(2, TimeUnit.SECONDS));
        second.start();
        try {
            assertFalse(secondCloseReturned.await(200, TimeUnit.MILLISECONDS));
        } finally {
            allowDestroyToFinish.countDown();
        }
        first.join(2000);
        second.join(2000);
        assertFalse(first.isAlive());
        assertTrue(element.isDestroyed());
    }

    @Test
    public void testEvict() throws InterruptedException {
        for (int i = 0; i < 10; i++) {
            pool.put("key" + i, new TestPoolable("value" + i));
        }
        pool.evict();
        assertEquals(8, pool.size());

        GenericKeyedObjectPool<String, TestPoolable> custom = new GenericKeyedObjectPool<>(10, 0, EvictionPolicy.LAST_ACCESS_TIME, true, 0.5f);
        try {
            for (int i = 0; i < 10; i++) {
                custom.put("key" + i, new TestPoolable("value" + i));
            }
            custom.evict();
            assertEquals(5, custom.size());
        } finally {
            custom.close();
        }

        TestPoolable v1 = new TestPoolable("v1");
        TestPoolable v2 = new TestPoolable("v2");
        TestPoolable v3 = new TestPoolable("v3");
        pool.clear();
        pool.put("key1", v1);
        pool.put("key2", v2);
        pool.put("key3", v3);
        pool.vacate(2);
        int destroyed = (v1.isDestroyed() ? 1 : 0) + (v2.isDestroyed() ? 1 : 0) + (v3.isDestroyed() ? 1 : 0);
        assertEquals(1, pool.size());
        assertEquals(2, destroyed);

        GenericKeyedObjectPool<String, TestPoolable> created = new GenericKeyedObjectPool<>(10, 0, EvictionPolicy.CREATED_TIME, true, 0.5f);
        try {
            TestPoolable c1 = new TestPoolable("v1");
            Thread.sleep(10);
            TestPoolable c2 = new TestPoolable("v2");
            Thread.sleep(10);
            TestPoolable c3 = new TestPoolable("v3");
            created.put("k1", c1);
            created.put("k2", c2);
            created.put("k3", c3);
            created.vacate(2);
            assertTrue(c1.isDestroyed());
            assertTrue(c2.isDestroyed());
            assertFalse(c3.isDestroyed());
        } finally {
            created.close();
        }

        GenericKeyedObjectPool<String, TestPoolable> fifo = new GenericKeyedObjectPool<>(10, 0, EvictionPolicy.FIFO, true, 0.5f);
        try {
            TestPoolable older = new TestPoolable("older");
            Thread.sleep(10);
            TestPoolable newer = new TestPoolable("newer");
            fifo.put("newer", newer);
            fifo.put("older", older);
            fifo.vacate(1);
            assertTrue(newer.isDestroyed());
            assertFalse(older.isDestroyed());
        } finally {
            fifo.close();
        }

        GenericKeyedObjectPool<String, TestPoolable> countPool = new GenericKeyedObjectPool<>(10, 0, EvictionPolicy.ACCESS_COUNT, true, 0.5f);
        try {
            for (int i = 0; i < 10; i++) {
                countPool.put("key" + i, new TestPoolable("value" + i));
            }
            for (int i = 0; i < 5; i++) {
                countPool.get("key" + i);
                countPool.get("key" + i);
            }
            countPool.evict();
            assertTrue(countPool.size() < 10);
            for (int i = 0; i < 5; i++) {
                assertTrue(countPool.containsKey("key" + i));
            }
        } finally {
            countPool.close();
        }

        KeyedObjectPool.MemoryMeasure<String, TestPoolable> measure = (k, v) -> k.length() + 100;
        GenericKeyedObjectPool<String, TestPoolable> memPool = new GenericKeyedObjectPool<>(10, 0, EvictionPolicy.LAST_ACCESS_TIME, true, 0.5f, 5000, measure);
        try {
            for (int i = 0; i < 10; i++) {
                memPool.put("key" + i, new TestPoolable("value" + i));
            }
            long before = memPool.stats().dataSize();
            memPool.evict();
            assertTrue(memPool.stats().dataSize() < before);
        } finally {
            memPool.close();
        }

        GenericKeyedObjectPool<String, TestPoolable> scheduled = new GenericKeyedObjectPool<>(10, 100, EvictionPolicy.LAST_ACCESS_TIME);
        try {
            TestPoolable shortLived = new TestPoolable("short", 50, 50);
            TestPoolable longLived = new TestPoolable("long", 10000, 10000);
            scheduled.put("short", shortLived);
            scheduled.put("long", longLived);
            Thread.sleep(200);
            assertEquals(1, scheduled.size());
            assertTrue(scheduled.containsKey("long"));
            assertTrue(shortLived.isDestroyed());
        } finally {
            scheduled.close();
        }

        TestPoolable expired = new TestPoolable("expired", 10, 10);
        TestPoolable retained = new TestPoolable("retained");
        pool.clear();
        pool.put("expired", expired);
        pool.put("retained", retained);
        Thread.sleep(20);
        pool.removeExpired();
        assertTrue(expired.isDestroyed());
        assertEquals(Poolable.Caller.EVICT, expired.getDestroyedByCaller());
        assertEquals(1, pool.size());
    }

    @Test
    public void testEvict_Closed() {
        pool.evict();
        assertEquals(0, pool.size());
        pool.close();
        assertThrows(IllegalStateException.class, () -> pool.evict());
    }

    @Test
    public void testEvict_RemovesBeforeDestroyCallback() {
        GenericKeyedObjectPool<String, TestPoolable> p = new GenericKeyedObjectPool<>(1, 0, EvictionPolicy.LAST_ACCESS_TIME);
        AtomicReference<TestPoolable> observed = new AtomicReference<>();
        AtomicReference<Boolean> lockHeld = new AtomicReference<>();
        TestPoolable doomed = new TestPoolable("doomed") {
            @Override
            public void destroy(final Poolable.Caller caller) {
                lockHeld.set(p.lock.isHeldByCurrentThread());
                observed.set(p.get("key"));
                super.destroy(caller);
            }
        };
        try {
            assertTrue(p.put("key", doomed));
            p.evict();
            assertNull(observed.get());
            assertEquals(Boolean.FALSE, lockHeld.get());
        } finally {
            p.close();
        }
    }

    @Test
    public void testEvict_SingleCriticalSection() {
        AtomicReference<TestPoolable> removedDuringFormerGap = new AtomicReference<>();
        AtomicEvictPool p = new AtomicEvictPool(removedDuringFormerGap);
        try {
            p.put("one", new TestPoolable("one"));
            p.put("two", new TestPoolable("two"));
            p.put("three", new TestPoolable("three"));
            p.put("four", new TestPoolable("four"));
            p.evict();
            assertNull(removedDuringFormerGap.get());
            assertEquals(2, p.size());
        } finally {
            p.close();
        }
    }

    @Test
    public void testSize() {
        assertEquals(0, pool.size());
        assertTrue(pool.isEmpty());
        assertEquals(10, pool.capacity());
        pool.put("key1", new TestPoolable("value1"));
        assertEquals(1, pool.size());
        pool.remove("key1");
        assertEquals(0, pool.size());
        pool.put("key1", new TestPoolable("value1"));
        pool.close();
        assertThrows(IllegalStateException.class, () -> pool.size());
    }

    @Test
    public void testStats() {
        pool.put("key1", new TestPoolable("value1"));
        pool.put("key2", new TestPoolable("value2"));
        pool.get("key1");
        pool.get("key2");
        pool.get("key3");
        PoolStats stats = pool.stats();
        assertEquals(10, stats.capacity());
        assertEquals(2, stats.size());
        assertEquals(2, stats.putCount());
        assertEquals(3, stats.getCount());
        assertEquals(2, stats.hitCount());
        assertEquals(1, stats.missCount());
        assertFalse(pool.isMemoryTracked());
        assertEquals(-1, stats.dataSize());

        KeyedObjectPool.MemoryMeasure<String, TestPoolable> measure = (k, v) -> 100;
        GenericKeyedObjectPool<String, TestPoolable> unlimited = new GenericKeyedObjectPool<>(10, 0, EvictionPolicy.LAST_ACCESS_TIME, 0, measure);
        try {
            unlimited.put("a", new TestPoolable("a"));
            unlimited.put("b", new TestPoolable("b"));
            assertEquals(-1, unlimited.stats().maxMemory());
            assertEquals(200, unlimited.stats().dataSize());
            unlimited.remove("a");
            assertEquals(100, unlimited.stats().dataSize());
        } finally {
            unlimited.close();
        }

        GenericKeyedObjectPool<String, TestPoolable> limited = new GenericKeyedObjectPool<>(10, 0, EvictionPolicy.LAST_ACCESS_TIME, 1000, measure);
        try {
            limited.put("a", new TestPoolable("a"));
            limited.put("b", new TestPoolable("b"));
            assertEquals(1000, limited.stats().maxMemory());
            assertEquals(200, limited.stats().dataSize());
        } finally {
            limited.close();
        }
    }

    @Test
    public void testLockAndUnlock() {
        pool.lock();
        pool.unlock();
        assertEquals(0, pool.size());
    }

    @Test
    public void testEqualsHashCodeToString() {
        pool.put("key1", new TestPoolable("value1"));
        assertEquals(pool.hashCode(), pool.hashCode());
        assertTrue(pool.toString().contains("GenericKeyedObjectPool"));
        assertFalse(pool.toString().contains("key1"));

        GenericKeyedObjectPool<String, TestPoolable> pool1 = new GenericKeyedObjectPool<>(10, 0, EvictionPolicy.LAST_ACCESS_TIME);
        GenericKeyedObjectPool<String, TestPoolable> pool2 = new GenericKeyedObjectPool<>(10, 0, EvictionPolicy.LAST_ACCESS_TIME);
        try {
            assertTrue(pool1.equals(pool2));
            TestPoolable p = new TestPoolable("value1");
            pool1.put("key1", p);
            assertFalse(pool1.equals(pool2));
            pool2.put("key1", p);
            assertTrue(pool1.equals(pool2));
            assertFalse(pool1.equals(null));
            assertFalse(pool1.equals("not a pool"));
        } finally {
            pool1.close();
            pool2.close();
        }
    }

    @Test
    public void testHashCode_hashesOutsideThePoolLock() {
        GenericKeyedObjectPool<LockAwareKey, TestPoolable> p = new GenericKeyedObjectPool<>(10, 0, EvictionPolicy.LAST_ACCESS_TIME);
        try {
            final LockAwareKey a = new LockAwareKey("a");
            final LockAwareKey b = new LockAwareKey("b");
            final LockAwarePoolable va = new LockAwarePoolable("va");
            final LockAwarePoolable vb = new LockAwarePoolable("vb");
            assertTrue(p.put(a, va));
            assertTrue(p.put(b, vb));
            a.pool = p;
            b.pool = p;
            va.pool = p;
            vb.pool = p;
            a.resetCounts();
            b.resetCounts();
            va.resetCounts();
            vb.resetCounts();

            final int hash = p.hashCode();

            assertEquals(0, a.hashesWhilePoolLockHeld);
            assertEquals(0, b.hashesWhilePoolLockHeld);
            assertEquals(0, va.hashesWhilePoolLockHeld);
            assertEquals(0, vb.hashesWhilePoolLockHeld);
            assertEquals(1, a.hashCount);
            assertEquals(1, b.hashCount);
            assertEquals(1, va.hashCount);
            assertEquals(1, vb.hashCount);
            assertEquals(new AbstractMap.SimpleImmutableEntry<>(a, va).hashCode() + new AbstractMap.SimpleImmutableEntry<>(b, vb).hashCode(), hash);

            GenericKeyedObjectPool<LockAwareKey, TestPoolable> q = new GenericKeyedObjectPool<>(10, 0, EvictionPolicy.LAST_ACCESS_TIME);
            try {
                q.put(a, va);
                q.put(b, vb);
                assertEquals(p, q);
                assertEquals(p.hashCode(), q.hashCode());
            } finally {
                q.close();
            }
        } finally {
            p.close();
        }
    }

    @Test
    public void testEquals_ConcurrentMutation() throws InterruptedException {
        GenericKeyedObjectPool<String, TestPoolable> p1 = new GenericKeyedObjectPool<>(1000, 0, EvictionPolicy.LAST_ACCESS_TIME);
        GenericKeyedObjectPool<String, TestPoolable> p2 = new GenericKeyedObjectPool<>(1000, 0, EvictionPolicy.LAST_ACCESS_TIME);
        for (int i = 0; i < 200; i++) {
            p1.put("k" + i, new TestPoolable("v" + i));
            p2.put("k" + i, new TestPoolable("v" + i));
        }
        AtomicBoolean stop = new AtomicBoolean();
        AtomicReference<Throwable> mutatorFailure = new AtomicReference<>();
        Thread mutator = new Thread(() -> {
            int n = 0;
            try {
                while (!stop.get()) {
                    String key = "k" + (n++ % 200);
                    p2.remove(key);
                    p2.put(key, new TestPoolable("vv" + n));
                }
            } catch (Throwable t) {
                mutatorFailure.set(t);
            }
        });
        mutator.start();
        AtomicReference<Throwable> equalsFailure = new AtomicReference<>();
        try {
            for (int i = 0; i < 500; i++) {
                try {
                    p1.equals(p2);
                } catch (Throwable t) {
                    equalsFailure.set(t);
                    break;
                }
            }
        } finally {
            stop.set(true);
            mutator.join();
        }
        assertNull(equalsFailure.get());
        assertNull(mutatorFailure.get());
        p1.close();
        p2.close();
    }

    @Test
    public void testConcurrent() throws InterruptedException {
        int threads = 10;
        int ops = 100;
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(threads);
        for (int i = 0; i < threads; i++) {
            final int threadId = i;
            new Thread(() -> {
                try {
                    startLatch.await();
                    for (int j = 0; j < ops; j++) {
                        String key = "key" + (j % 20);
                        if (j % 2 == 0) {
                            pool.put(key, new TestPoolable("t" + threadId + "-" + j));
                        } else {
                            pool.get(key);
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    endLatch.countDown();
                }
            }).start();
        }
        startLatch.countDown();
        assertTrue(endLatch.await(5, TimeUnit.SECONDS));
        assertTrue(pool.size() >= 0 && pool.size() <= pool.capacity());
    }

    @Test
    public void testTimedPut() throws InterruptedException {
        TestPoolable v = new TestPoolable("v");
        assertTrue(pool.put("k", v, 1, TimeUnit.SECONDS));
        assertSameValue(v, pool.get("k"));

        GenericKeyedObjectPool<String, TestPoolable> p = noBalance(1);
        try {
            assertTrue(p.put("k1", new TestPoolable("v1")));
            long start = System.nanoTime();
            assertFalse(p.put("k2", new TestPoolable("v2"), 100, TimeUnit.MILLISECONDS));
            assertTrue((System.nanoTime() - start) / 1_000_000 >= 50);
            assertEquals(1, p.size());

            Thread freer = new Thread(() -> {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                p.remove("k1");
            });
            freer.start();
            assertTrue(p.put("k2", new TestPoolable("v2"), 5, TimeUnit.SECONDS));
            freer.join();
            assertTrue(p.containsKey("k2"));

            TestPoolable timeoutDestroy = new TestPoolable("v3");
            assertFalse(p.put("k3", timeoutDestroy, 50, TimeUnit.MILLISECONDS, true));
            assertTrue(timeoutDestroy.isDestroyed());
            assertEquals(Poolable.Caller.PUT_ADD_FAILURE, timeoutDestroy.getDestroyedByCaller());
        } finally {
            p.close();
        }

        TestPoolable expired = new TestPoolable("e", 1, 1);
        Thread.sleep(10);
        assertFalse(pool.put("expired", expired, 1, TimeUnit.SECONDS));
        assertThrows(IllegalArgumentException.class, () -> pool.put(null, new TestPoolable("v"), 1, TimeUnit.SECONDS));
        assertThrows(IllegalArgumentException.class, () -> pool.put("k", null, 1, TimeUnit.SECONDS));

        GenericKeyedObjectPool<String, TestPoolable> auto = new GenericKeyedObjectPool<>(1, 0, EvictionPolicy.LAST_ACCESS_TIME);
        try {
            TestPoolable victim = new TestPoolable("v1");
            TestPoolable newcomer = new TestPoolable("v2");
            assertTrue(auto.put("k1", victim));
            long nano = System.nanoTime();
            assertTrue(auto.put("k2", newcomer, 5, TimeUnit.SECONDS));
            assertTrue((System.nanoTime() - nano) / 1_000_000 < 2_000);
            assertTrue(victim.isDestroyed());
            assertEquals(Poolable.Caller.VACATE, victim.getDestroyedByCaller());
            assertSame(newcomer, auto.peek("k2"));
        } finally {
            auto.close();
        }

        GenericKeyedObjectPool<String, TestPoolable> zero = new GenericKeyedObjectPool<>(0, 0, EvictionPolicy.LAST_ACCESS_TIME);
        try {
            TestPoolable value = new TestPoolable("v");
            long nano = System.nanoTime();
            assertFalse(zero.put("k", value, 50, TimeUnit.MILLISECONDS));
            assertTrue((System.nanoTime() - nano) / 1_000_000 >= 25);
            assertFalse(value.isDestroyed());
        } finally {
            zero.close();
        }
    }

    @Test
    public void testTimedGet() throws InterruptedException {
        TestPoolable v = new TestPoolable("v");
        pool.put("k", v);
        assertSameValue(v, pool.get("k", 1, TimeUnit.SECONDS));
        assertEquals(1, pool.size());

        long start = System.nanoTime();
        assertNull(pool.get("missing", 100, TimeUnit.MILLISECONDS));
        assertTrue((System.nanoTime() - start) / 1_000_000 >= 50);

        CountDownLatch ready = new CountDownLatch(1);
        Thread producer = new Thread(() -> {
            try {
                ready.await();
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            pool.put("later", new TestPoolable("v"));
        });
        producer.start();
        ready.countDown();
        TestPoolable got = pool.get("later", 5, TimeUnit.SECONDS);
        producer.join();
        assertEquals("v", got.getValue());
    }

    @Test
    public void testTimedGet_HeterogeneousWaiters() throws InterruptedException {
        GenericKeyedObjectPool<String, TestPoolable> p = new GenericKeyedObjectPool<>(8, 0, EvictionPolicy.LAST_ACCESS_TIME);
        try {
            CountDownLatch waitersStarted = new CountDownLatch(2);
            AtomicReference<TestPoolable> resultA = new AtomicReference<>();
            AtomicReference<TestPoolable> resultC = new AtomicReference<>();
            AtomicReference<Throwable> failure = new AtomicReference<>();
            Thread waiterA = new Thread(() -> {
                waitersStarted.countDown();
                try {
                    resultA.set(p.get("A", 10_000, TimeUnit.MILLISECONDS));
                } catch (final Throwable t) {
                    failure.set(t);
                }
            });
            Thread waiterC = new Thread(() -> {
                waitersStarted.countDown();
                try {
                    resultC.set(p.get("C", 10_000, TimeUnit.MILLISECONDS));
                } catch (final Throwable t) {
                    failure.set(t);
                }
            });
            waiterA.start();
            waiterC.start();
            assertTrue(waitersStarted.await(2, TimeUnit.SECONDS));
            Thread.sleep(150);
            p.put("B", new TestPoolable("vB"));
            Thread.sleep(50);
            p.put("A", new TestPoolable("vA"));
            p.put("C", new TestPoolable("vC"));
            waiterA.join(4_000);
            waiterC.join(4_000);
            assertNull(failure.get());
            assertEquals("vA", resultA.get().getValue());
            assertEquals("vC", resultC.get().getValue());
        } finally {
            p.close();
        }
    }

    @Test
    public void testPut_FailedReplaceWakesNotFullWaiters() throws Exception {
        KeyedObjectPool.MemoryMeasure<String, TestPoolable> measure = (k, v) -> "reject".equals(v.getValue()) ? -1L : 10L;
        GenericKeyedObjectPool<String, TestPoolable> p = new GenericKeyedObjectPool<>(1, 0, EvictionPolicy.LAST_ACCESS_TIME, false, 0.2f, 10_000L, measure);
        try {
            assertTrue(p.put("A", new TestPoolable("oldA")));
            CountDownLatch waiterStarted = new CountDownLatch(1);
            AtomicReference<Boolean> putResult = new AtomicReference<>();
            AtomicReference<Throwable> failure = new AtomicReference<>();
            Thread waiter = new Thread(() -> {
                try {
                    waiterStarted.countDown();
                    putResult.set(p.put("B", new TestPoolable("vB"), 5, TimeUnit.SECONDS));
                } catch (final Throwable t) {
                    failure.set(t);
                }
            });
            waiter.start();
            assertTrue(waiterStarted.await(2, TimeUnit.SECONDS));
            Thread.sleep(150);
            assertFalse(p.put("A", new TestPoolable("reject")));
            waiter.join(4_000);
            assertNull(failure.get());
            assertEquals(Boolean.TRUE, putResult.get());
            assertNotNull(p.get("B"));
        } finally {
            p.close();
        }
    }

    @Test
    public void testExpiredReadWakesTimedPutWaiter() throws Exception {
        assertExpiredReadWakesTimedPutWaiter(false);
        assertExpiredReadWakesTimedPutWaiter(true);
    }

    private static void assertExpiredReadWakesTimedPutWaiter(final boolean peek) throws Exception {
        GenericKeyedObjectPool<String, TestPoolable> p = noBalance(1);
        TestPoolable old = new TestPoolable("old");
        CountDownLatch waiterStarted = new CountDownLatch(1);
        AtomicReference<Boolean> putResult = new AtomicReference<>();
        AtomicReference<Throwable> failure = new AtomicReference<>();
        Thread waiter = new Thread(() -> {
            try {
                waiterStarted.countDown();
                putResult.set(p.put("new", new TestPoolable("new"), 10, TimeUnit.SECONDS));
            } catch (final Throwable e) {
                failure.set(e);
            }
        });
        try {
            assertTrue(p.put("old", old));
            waiter.start();
            assertTrue(waiterStarted.await(2, TimeUnit.SECONDS));
            Thread.sleep(100);
            old.activityPrint().setCreatedTime(System.currentTimeMillis() - 20_000);
            assertNull(peek ? p.peek("old") : p.get("old"));
            waiter.join(2_000);
            assertFalse(waiter.isAlive());
            assertNull(failure.get());
            assertEquals(Boolean.TRUE, putResult.get());
        } finally {
            p.close();
            waiter.join(1_000);
        }
    }

    @Test
    public void testPutReplacementDestroyCallback() {
        GenericKeyedObjectPool<String, TestPoolable> p = new GenericKeyedObjectPool<>(1, 0, EvictionPolicy.LAST_ACCESS_TIME);
        AtomicReference<Boolean> lockHeld = new AtomicReference<>();
        AtomicReference<TestPoolable> observed = new AtomicReference<>();
        TestPoolable replacement = new TestPoolable("replacement");
        TestPoolable victim = new TestPoolable("victim") {
            @Override
            public void destroy(final Poolable.Caller caller) {
                lockHeld.set(p.lock.isHeldByCurrentThread());
                observed.set(p.peek("key"));
                super.destroy(caller);
            }
        };
        try {
            assertTrue(p.put("key", victim));
            assertTrue(p.put("key", replacement));
            assertEquals(Boolean.FALSE, lockHeld.get());
            assertEquals(replacement, observed.get());
            assertEquals(Poolable.Caller.REMOVE_REPLACE_CLEAR, victim.getDestroyedByCaller());
        } finally {
            p.close();
        }
    }

    @Test
    public void testPutAutoBalanceDestroyCallback() {
        GenericKeyedObjectPool<String, TestPoolable> p = new GenericKeyedObjectPool<>(1, 0, EvictionPolicy.LAST_ACCESS_TIME);
        AtomicReference<Boolean> lockHeld = new AtomicReference<>();
        AtomicReference<Boolean> victimPresent = new AtomicReference<>();
        AtomicReference<TestPoolable> observedReplacement = new AtomicReference<>();
        TestPoolable replacement = new TestPoolable("replacement");
        TestPoolable victim = new TestPoolable("victim") {
            @Override
            public void destroy(final Poolable.Caller caller) {
                lockHeld.set(p.lock.isHeldByCurrentThread());
                victimPresent.set(p.containsKey("victim"));
                observedReplacement.set(p.peek("replacement"));
                super.destroy(caller);
            }
        };
        try {
            assertTrue(p.put("victim", victim));
            assertTrue(p.put("replacement", replacement));
            assertEquals(Boolean.FALSE, lockHeld.get());
            assertEquals(Boolean.FALSE, victimPresent.get());
            assertEquals(replacement, observedReplacement.get());
            assertEquals(Poolable.Caller.VACATE, victim.getDestroyedByCaller());
        } finally {
            p.close();
        }
    }

    @Test
    public void testGet_ExpiredDestroyCallback() {
        GenericKeyedObjectPool<String, TestPoolable> p = new GenericKeyedObjectPool<>(1, 0, EvictionPolicy.LAST_ACCESS_TIME);
        AtomicReference<Boolean> lockHeld = new AtomicReference<>();
        AtomicReference<Boolean> mappingPresent = new AtomicReference<>();
        TestPoolable expired = new TestPoolable("expired") {
            @Override
            public void destroy(final Poolable.Caller caller) {
                lockHeld.set(p.lock.isHeldByCurrentThread());
                mappingPresent.set(p.containsKey("expired"));
                super.destroy(caller);
            }
        };
        try {
            assertTrue(p.put("expired", expired));
            expired.activityPrint().setCreatedTime(System.currentTimeMillis() - 20_000);
            assertNull(p.get("expired"));
            assertEquals(Boolean.FALSE, lockHeld.get());
            assertEquals(Boolean.FALSE, mappingPresent.get());
            assertEquals(Poolable.Caller.EVICT, expired.getDestroyedByCaller());
        } finally {
            p.close();
        }
    }

    @Test
    public void testTimedPut_SpuriousWakeups() throws Exception {
        GenericKeyedObjectPool<String, TestPoolable> p = noBalance(1);
        assertTrue(p.put("existing", new TestPoolable("value")));
        SpuriousWakeupCondition condition = new SpuriousWakeupCondition(3);
        p.notFull = condition;
        try {
            assertFalse(p.put("new", new TestPoolable("new-value"), 1, TimeUnit.SECONDS));
            assertEquals(4, condition.awaitCount);
        } finally {
            p.close();
        }
        assertThrows(IllegalArgumentException.class, () -> pool.put("key", new TestPoolable("value"), 1, null));
        assertThrows(IllegalArgumentException.class, () -> pool.get("key", 1, null));
    }

    @Test
    public void testRemove_ReturnsExpiredUndestroyed() throws Exception {
        KeyedObjectPool.MemoryMeasure<String, TestPoolable> measure = (k, v) -> 100;
        GenericKeyedObjectPool<String, TestPoolable> memPool = new GenericKeyedObjectPool<>(1, 0, EvictionPolicy.LAST_ACCESS_TIME, false, 0.2f, 1000, measure);
        try {
            TestPoolable shortLived = new TestPoolable("s", 20, 20);
            assertTrue(memPool.put("k", shortLived));
            Thread.sleep(50);
            assertTrue(shortLived.activityPrint().isExpired());
            assertTrue(memPool.containsKey("k"));
            TestPoolable removed = memPool.remove("k");
            assertSame(shortLived, removed);
            assertFalse(removed.isDestroyed());
            assertEquals(0, memPool.stats().dataSize());
            assertEquals(0, memPool.stats().evictionCount());
            assertTrue(memPool.put("k2", new TestPoolable("live")));
        } finally {
            memPool.close();
        }
    }

    @Test
    public void testExpiredMappingReportedUntilGetEvicts() throws Exception {
        TestPoolable shortLived = new TestPoolable("s", 20, 20);
        TestPoolable live = new TestPoolable("l");
        assertTrue(pool.put("expired", shortLived));
        assertTrue(pool.put("live", live));
        Thread.sleep(50);
        assertTrue(pool.containsKey("expired"));
        assertEquals(2, pool.size());
        assertTrue(pool.keySet().contains("expired"));
        assertTrue(pool.values().contains(shortLived));
        assertNull(pool.get("expired"));
        assertTrue(shortLived.isDestroyed());
        assertEquals(Poolable.Caller.EVICT, shortLived.getDestroyedByCaller());
        assertFalse(pool.containsKey("expired"));
        assertEquals(1, pool.size());
        assertSame(live, pool.get("live"));

        TestPoolable peekExpired = new TestPoolable("s", 20, 20);
        pool.put("peek", peekExpired);
        Thread.sleep(50);
        assertNull(pool.peek("peek"));
        assertTrue(peekExpired.isDestroyed());
        assertNull(pool.remove("peek"));
    }

    @Test
    public void testNullKeyReadsMatchNoMapping() throws Exception {
        assertTrue(pool.put("k", new TestPoolable("v")));
        long missesBefore = pool.stats().missCount();
        long hitsBefore = pool.stats().hitCount();
        assertNull(pool.get(null));
        assertEquals(missesBefore + 1, pool.stats().missCount());
        assertNull(pool.get(null, 10, TimeUnit.MILLISECONDS));
        assertEquals(missesBefore + 2, pool.stats().missCount());
        assertNull(pool.peek(null));
        assertNull(pool.remove(null));
        assertFalse(pool.containsKey(null));
        assertEquals(hitsBefore, pool.stats().hitCount());
        assertEquals(1, pool.size());
        assertThrows(IllegalArgumentException.class, () -> pool.put(null, new TestPoolable("v2")));
    }
}

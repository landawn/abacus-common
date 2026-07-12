package com.landawn.abacus.pool;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

public class GenericKeyedObjectPoolTest extends TestBase {

    private GenericKeyedObjectPool<String, TestPoolable> pool;

    private static class TestPoolable extends AbstractPoolable {
        private final String value;
        private boolean destroyed = false;
        private Poolable.Caller destroyedByCaller = null;

        TestPoolable(String value) {
            super(10000, 5000);
            this.value = value;
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

    private static byte[] serialize(final Object obj) throws IOException {
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();

        try (ObjectOutputStream oos = new ObjectOutputStream(baos)) {
            oos.writeObject(obj);
        }

        return baos.toByteArray();
    }

    private static <T> T deserialize(final byte[] bytes) throws IOException, ClassNotFoundException {
        try (ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(bytes))) {
            return (T) ois.readObject();
        }
    }

    @Test
    public void testPut() {
        TestPoolable poolable = new TestPoolable("value1");
        assertTrue(pool.put("key1", poolable));
        assertEquals(1, pool.size());
        assertTrue(pool.containsKey("key1"));
    }

    @Test
    public void testPutClosedPoolRejectsExpiredValueWithIllegalState() throws InterruptedException {
        GenericKeyedObjectPool<String, TestPoolable> closedPool = new GenericKeyedObjectPool<>(1, 0, EvictionPolicy.LAST_ACCESS_TIME);
        TestPoolable expired = new TestPoolable("expired", 1, 1);

        Thread.sleep(20);
        closedPool.close();

        assertThrows(IllegalStateException.class, () -> closedPool.put("key", expired));
    }

    @Test
    public void testPutToFullPoolWithoutAutoBalance() {
        GenericKeyedObjectPool<String, TestPoolable> noBalancePool = new GenericKeyedObjectPool<>(3, 0, EvictionPolicy.LAST_ACCESS_TIME, false, 0.2f);

        for (int i = 0; i < 3; i++) {
            assertTrue(noBalancePool.put("key" + i, new TestPoolable("value" + i)));
        }

        assertFalse(noBalancePool.put("key3", new TestPoolable("value3")));
        assertEquals(3, noBalancePool.size());

        noBalancePool.close();
    }

    @Test
    public void testPutToFullPoolWithAutoBalance() {
        GenericKeyedObjectPool<String, TestPoolable> balancePool = new GenericKeyedObjectPool<>(3, 0, EvictionPolicy.LAST_ACCESS_TIME, true, 0.4f);

        for (int i = 0; i < 3; i++) {
            assertTrue(balancePool.put("key" + i, new TestPoolable("value" + i)));
        }

        assertTrue(balancePool.put("key3", new TestPoolable("value3")));
        assertEquals(3, balancePool.size());

        balancePool.close();
    }

    @Test
    public void testPutToSmallFullPoolWithDefaultAutoBalanceFactor() {
        GenericKeyedObjectPool<String, TestPoolable> balancePool = new GenericKeyedObjectPool<>(1, 0, EvictionPolicy.LAST_ACCESS_TIME, true, 0.2f);
        TestPoolable first = new TestPoolable("value1");

        assertTrue(balancePool.put("key1", first));
        assertTrue(balancePool.put("key2", new TestPoolable("value2")));
        assertEquals(1, balancePool.size());
        assertTrue(first.isDestroyed());

        balancePool.close();
    }

    @Test
    public void testPutReplace() {
        TestPoolable p1 = new TestPoolable("value1");
        TestPoolable p2 = new TestPoolable("value2");

        assertTrue(pool.put("key1", p1));
        assertEquals(1, pool.size());

        assertTrue(pool.put("key1", p2));
        assertEquals(1, pool.size());

        assertTrue(p1.isDestroyed());
        assertEquals(Poolable.Caller.REMOVE_REPLACE_CLEAR, p1.getDestroyedByCaller());

        assertEquals(p2, pool.get("key1"));
    }

    @Test
    public void testPutWithMemoryConstraint() {
        KeyedObjectPool.MemoryMeasure<String, TestPoolable> measure = (k, v) -> k.length() + 100;
        GenericKeyedObjectPool<String, TestPoolable> memPool = new GenericKeyedObjectPool<>(10, 0, EvictionPolicy.LAST_ACCESS_TIME, 250, measure);

        assertTrue(memPool.put("k1", new TestPoolable("v1")));
        assertTrue(memPool.put("k2", new TestPoolable("v2")));
        assertTrue(memPool.put("k3", new TestPoolable("v3")));
        assertEquals(2, memPool.size());

        memPool.close();
    }

    @Test
    public void testPut_valueExpiringDuringInLockMeasure_isRejected() {
        // GenericKeyedObjectPool.put(K,E) calls memoryMeasure.sizeOf(...) inside the lock, AFTER the
        // pre-lock expiry check. A value with little life left can therefore expire during that in-lock
        // work and must NOT be pooled (mirrors GenericObjectPool.add and the timed put, both of which
        // re-check expiry in-lock). Make sizeOf() sleep longer than the value's liveTime.
        final KeyedObjectPool.MemoryMeasure<String, TestPoolable> slowMeasure = (k, v) -> {
            try {
                Thread.sleep(200);
            } catch (final InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return 1L;
        };
        final GenericKeyedObjectPool<String, TestPoolable> memPool = new GenericKeyedObjectPool<>(10, 0, EvictionPolicy.LAST_ACCESS_TIME, 1_000_000L,
                slowMeasure);

        try {
            // liveTime 50ms: not expired when put() is called, but expires during the 200ms in-lock sizeOf().
            final TestPoolable shortLived = new TestPoolable("v", 50, 50);

            assertFalse(memPool.put("k", shortLived), "a value that expired during the in-lock measure must be rejected, not pooled");
            assertEquals(0, memPool.size());
            assertNull(memPool.get("k"));
        } finally {
            memPool.close();
        }
    }

    @Test
    public void testTimedPut_valueExpiringDuringInLockMeasure_isRejected() throws InterruptedException {
        final KeyedObjectPool.MemoryMeasure<String, TestPoolable> slowMeasure = (k, v) -> {
            try {
                Thread.sleep(200);
            } catch (final InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return 1L;
        };
        final GenericKeyedObjectPool<String, TestPoolable> memPool = new GenericKeyedObjectPool<>(10, 0, EvictionPolicy.LAST_ACCESS_TIME, 1_000_000L,
                slowMeasure);

        try {
            final TestPoolable shortLived = new TestPoolable("v", 50, 50);

            assertFalse(memPool.put("k", shortLived, 1, TimeUnit.SECONDS), "timed put must reject a value that expires during the in-lock memory measure");
            assertEquals(0, memPool.size());
            assertNull(memPool.get("k"));
        } finally {
            memPool.close();
        }
    }

    @Test
    public void testPutWithMemoryMeasureAndUnlimitedMemory() {
        KeyedObjectPool.MemoryMeasure<String, TestPoolable> measure = (k, v) -> k.length() + 100;
        GenericKeyedObjectPool<String, TestPoolable> memPool = new GenericKeyedObjectPool<>(10, 0, EvictionPolicy.LAST_ACCESS_TIME, 0, measure);

        try {
            for (int i = 0; i < 5; i++) {
                assertTrue(memPool.put("key" + i, new TestPoolable("value" + i)));
            }

            assertEquals(5, memPool.size());
        } finally {
            memPool.close();
        }
    }

    @Test
    public void testPutWithAutoDestroyFalse() {
        GenericKeyedObjectPool<String, TestPoolable> noBalancePool = new GenericKeyedObjectPool<>(2, 0, EvictionPolicy.LAST_ACCESS_TIME, false, 0.2f);
        try {
            noBalancePool.put("key1", new TestPoolable("value1"));
            noBalancePool.put("key2", new TestPoolable("value2"));

            TestPoolable extra = new TestPoolable("extra");
            assertFalse(noBalancePool.put("key3", extra, false));
            assertFalse(extra.isDestroyed());
        } finally {
            noBalancePool.close();
        }
    }

    @Test
    public void testPutWithMemoryConstraintNoAutoBalance() {
        KeyedObjectPool.MemoryMeasure<String, TestPoolable> measure = (k, v) -> k.length() + 100;
        GenericKeyedObjectPool<String, TestPoolable> memPool = new GenericKeyedObjectPool<>(10, 0, EvictionPolicy.LAST_ACCESS_TIME, false, 0.2f, 250, measure);

        assertTrue(memPool.put("k1", new TestPoolable("v1")));
        assertTrue(memPool.put("k2", new TestPoolable("v2")));
        assertFalse(memPool.put("k3", new TestPoolable("v3")));
        assertEquals(2, memPool.size());

        memPool.close();
    }

    @Test
    public void testPutCountIncrementsOnPut() {
        pool.put("key1", new TestPoolable("value1"));
        pool.put("key2", new TestPoolable("value2"));
        pool.put("key3", new TestPoolable("value3"));
        assertEquals(3, pool.stats().putCount());
    }

    @Test
    public void testPutWithAutoDestroy() {
        for (int i = 0; i < 10; i++) {
            pool.put("key" + i, new TestPoolable("value" + i));
        }

        TestPoolable extra = new TestPoolable("extra");
        assertTrue(pool.put("key10", extra, true));
        assertFalse(extra.isDestroyed());
        assertNull(extra.getDestroyedByCaller());
    }

    @Test
    public void testPutNullKey() {
        assertThrows(IllegalArgumentException.class, () -> pool.put(null, new TestPoolable("value")));
    }

    @Test
    public void testPutNullValue() {
        assertThrows(IllegalArgumentException.class, () -> pool.put("key", null));
    }

    @Test
    public void testPutExpired() throws InterruptedException {
        TestPoolable expired = new TestPoolable("expired", 10, 10);
        Thread.sleep(20);
        assertFalse(pool.put("key", expired));
        assertEquals(0, pool.size());
    }

    @Test
    public void testPutWithAutoDestroyFailure() {
        GenericKeyedObjectPool<String, TestPoolable> noBalancePool = new GenericKeyedObjectPool<>(2, 0, EvictionPolicy.LAST_ACCESS_TIME, false, 0.2f);
        try {
            noBalancePool.put("key1", new TestPoolable("value1"));
            noBalancePool.put("key2", new TestPoolable("value2"));

            TestPoolable extra = new TestPoolable("extra");
            assertFalse(noBalancePool.put("key3", extra, true));
            assertTrue(extra.isDestroyed());
            assertEquals(Poolable.Caller.PUT_ADD_FAILURE, extra.getDestroyedByCaller());
        } finally {
            noBalancePool.close();
        }
    }

    @Test
    public void testPutOnClosedPool() {
        pool.close();
        assertThrows(IllegalStateException.class, () -> pool.put("key", new TestPoolable("value")));
    }

    @Test
    public void testGet() {
        TestPoolable poolable = new TestPoolable("value1");
        pool.put("key1", poolable);

        TestPoolable retrieved = pool.get("key1");
        assertNotNull(retrieved);
        assertEquals("value1", retrieved.getValue());
        assertEquals(poolable, retrieved);
        assertEquals(1, retrieved.activityPrint().getAccessCount());
    }

    @Test
    public void testGetNonExistent() {
        assertNull(pool.get("nonexistent"));
    }

    @Test
    public void testGetMultipleTimesUpdatesAccessCount() {
        TestPoolable poolable = new TestPoolable("value1");
        pool.put("key1", poolable);

        pool.get("key1");
        pool.get("key1");
        pool.get("key1");

        assertEquals(3, poolable.activityPrint().getAccessCount());
    }

    @Test
    public void testGetExpired() throws InterruptedException {
        TestPoolable expired = new TestPoolable("expired", 50, 50);
        pool.put("key", expired);

        Thread.sleep(60);

        assertNull(pool.get("key"));
        assertTrue(expired.isDestroyed());
        assertEquals(Poolable.Caller.EVICT, expired.getDestroyedByCaller());
        assertFalse(pool.containsKey("key"));
    }

    @Test
    public void testGetOnClosedPool() {
        pool.close();
        assertThrows(IllegalStateException.class, () -> pool.get("key"));
    }

    @Test
    public void testRemoveWithMemoryMeasure() {
        KeyedObjectPool.MemoryMeasure<String, TestPoolable> measure = (k, v) -> k.length() + 100;
        GenericKeyedObjectPool<String, TestPoolable> memPool = new GenericKeyedObjectPool<>(10, 0, EvictionPolicy.LAST_ACCESS_TIME, 1000, measure);

        memPool.put("k1", new TestPoolable("v1"));
        memPool.put("k2", new TestPoolable("v2"));
        assertEquals(204, memPool.stats().dataSize());

        memPool.remove("k1");
        assertEquals(102, memPool.stats().dataSize());

        memPool.close();
    }

    @Test
    public void testRemove() {
        TestPoolable poolable = new TestPoolable("value1");
        pool.put("key1", poolable);

        TestPoolable removed = pool.remove("key1");
        assertNotNull(removed);
        assertEquals(poolable, removed);
        assertEquals(0, pool.size());
        assertFalse(pool.containsKey("key1"));
        assertEquals(1, removed.activityPrint().getAccessCount());
    }

    @Test
    public void testRemoveNonExistent() {
        assertNull(pool.remove("nonexistent"));
    }

    @Test
    public void testMemoryTracking() {
        KeyedObjectPool.MemoryMeasure<String, TestPoolable> measure = (k, v) -> k.length() + 100;
        GenericKeyedObjectPool<String, TestPoolable> memPool = new GenericKeyedObjectPool<>(10, 0, EvictionPolicy.LAST_ACCESS_TIME, 1000, measure);

        PoolStats stats = memPool.stats();
        assertEquals(1000, stats.maxMemory());
        assertEquals(0, stats.dataSize());

        memPool.put("k1", new TestPoolable("v1"));
        stats = memPool.stats();
        assertEquals(102, stats.dataSize());

        memPool.put("key2", new TestPoolable("v2"));
        stats = memPool.stats();
        assertEquals(206, stats.dataSize());

        memPool.remove("k1");
        stats = memPool.stats();
        assertEquals(104, stats.dataSize());

        memPool.close();
    }

    // Additional tests for missing coverage

    @Test
    public void testIsEmpty() {
        assertTrue(pool.isEmpty());
        pool.put("key1", new TestPoolable("value1"));
        assertFalse(pool.isEmpty());
        pool.remove("key1");
        assertTrue(pool.isEmpty());
    }

    @Test
    public void testRemoveUpdatesAccessStats() {
        TestPoolable poolable = new TestPoolable("value1");
        pool.put("key1", poolable);

        TestPoolable removed = pool.remove("key1");
        assertNotNull(removed);
        assertEquals(1, removed.activityPrint().getAccessCount());
    }

    @Test
    public void testRemoveOnClosedPool() {
        pool.close();
        assertThrows(IllegalStateException.class, () -> pool.remove("key"));
    }

    @Test
    public void testPeekDoesNotUpdateAccessStats() {
        TestPoolable poolable = new TestPoolable("value1");
        pool.put("key1", poolable);

        pool.peek("key1");
        pool.peek("key1");
        pool.peek("key1");

        assertEquals(0, poolable.activityPrint().getAccessCount());
    }

    @Test
    public void testPeek() {
        TestPoolable poolable = new TestPoolable("value1");
        pool.put("key1", poolable);

        TestPoolable peeked = pool.peek("key1");
        assertNotNull(peeked);
        assertEquals(poolable, peeked);
        assertEquals(0, peeked.activityPrint().getAccessCount());
    }

    @Test
    public void testPeekNonExistent() {
        assertNull(pool.peek("nonexistent"));
    }

    // --- Additional tests for missing coverage ---

    @Test
    public void testPeekKeepsElementInPool() {
        TestPoolable poolable = new TestPoolable("value1");
        pool.put("key1", poolable);

        TestPoolable peeked = pool.peek("key1");
        assertNotNull(peeked);
        assertEquals(poolable, peeked);

        // Element should still be in the pool
        assertEquals(1, pool.size());
        assertTrue(pool.containsKey("key1"));
    }

    @Test
    public void testPeekExpired() throws InterruptedException {
        TestPoolable expired = new TestPoolable("expired", 50, 50);
        pool.put("key", expired);

        Thread.sleep(60);

        assertNull(pool.peek("key"));
        assertTrue(expired.isDestroyed());
        assertEquals(Poolable.Caller.EVICT, expired.getDestroyedByCaller());
    }

    @Test
    public void testPeekOnClosedPool() {
        pool.close();
        assertThrows(IllegalStateException.class, () -> pool.peek("key"));
    }

    @Test
    public void testContainsKey() {
        pool.put("key1", new TestPoolable("value1"));
        pool.put("key2", new TestPoolable("value2"));

        assertTrue(pool.containsKey("key1"));
        assertTrue(pool.containsKey("key2"));
        assertFalse(pool.containsKey("key3"));
    }

    @Test
    public void testContainsKeyOnClosedPool() {
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
        assertTrue(keys.contains("key1"));
        assertTrue(keys.contains("key2"));
        assertTrue(keys.contains("key3"));

        keys.clear();
        assertEquals(3, pool.size());
    }

    @Test
    public void testKeySetOnClosedPool() {
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
        assertTrue(values.contains(p1));
        assertTrue(values.contains(p2));

        values.clear();
        assertEquals(2, pool.size());
    }

    @Test
    public void testValuesOnClosedPool() {
        pool.close();
        assertThrows(IllegalStateException.class, () -> pool.values());
    }

    @Test
    public void testClear() {
        List<TestPoolable> poolables = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            TestPoolable p = new TestPoolable("value" + i);
            poolables.add(p);
            pool.put("key" + i, p);
        }

        pool.clear();
        assertEquals(0, pool.size());

        for (TestPoolable p : poolables) {
            assertTrue(p.isDestroyed());
            assertEquals(Poolable.Caller.REMOVE_REPLACE_CLEAR, p.getDestroyedByCaller());
        }
    }

    @Test
    public void testClearEmptyPool() {
        pool.clear();
        assertEquals(0, pool.size());
        assertTrue(pool.isEmpty());
    }

    @Test
    public void testClearOnClosedPool() {
        pool.close();
        assertThrows(IllegalStateException.class, () -> pool.clear());
    }

    @Test
    public void testConstructorWithMemory() {
        KeyedObjectPool.MemoryMeasure<String, TestPoolable> measure = (k, v) -> k.length() + 100;
        GenericKeyedObjectPool<String, TestPoolable> memPool = new GenericKeyedObjectPool<>(20, 2000, EvictionPolicy.EXPIRATION_TIME, 1024, measure);
        assertEquals(20, memPool.capacity());
        memPool.close();
    }

    @Test
    public void testConstructorWithAutoBalance() {
        GenericKeyedObjectPool<String, TestPoolable> balancePool = new GenericKeyedObjectPool<>(30, 1000, EvictionPolicy.LAST_ACCESS_TIME, false, 0.3f);
        assertEquals(30, balancePool.capacity());
        balancePool.close();
    }

    @Test
    public void testConstructorFullConfig() {
        KeyedObjectPool.MemoryMeasure<String, TestPoolable> measure = (k, v) -> k.length() + 200;
        GenericKeyedObjectPool<String, TestPoolable> fullPool = new GenericKeyedObjectPool<>(40, 4000, EvictionPolicy.ACCESS_COUNT, true, 0.4f, 2048, measure);
        assertEquals(40, fullPool.capacity());
        fullPool.close();
    }

    @Test
    public void testSerializationWithEvictionEnabled() throws Exception {
        GenericKeyedObjectPool<String, TestPoolable> evictPool = new GenericKeyedObjectPool<>(10, 100, EvictionPolicy.LAST_ACCESS_TIME);

        GenericKeyedObjectPool<String, TestPoolable> deserialized = deserialize(serialize(evictPool));
        assertNotNull(deserialized);
        assertFalse(deserialized.isClosed());

        evictPool.close();
        deserialized.close();
    }

    @Test
    public void testSerializationWithEvictionEnabledRestoresShutdownHook() throws Exception {
        GenericKeyedObjectPool<String, TestPoolable> evictPool = new GenericKeyedObjectPool<>(10, 100, EvictionPolicy.LAST_ACCESS_TIME);

        GenericKeyedObjectPool<String, TestPoolable> deserialized = deserialize(serialize(evictPool));

        try {
            assertNotNull(shutdownHookOf(deserialized));
        } finally {
            evictPool.close();
            deserialized.close();
        }
    }

    private static Object shutdownHookOf(final Object pool) throws Exception {
        final Field field = AbstractPool.class.getDeclaredField("shutdownHook");
        field.setAccessible(true);
        return field.get(pool);
    }

    @Test
    public void testClose() {
        TestPoolable p1 = new TestPoolable("value1");
        TestPoolable p2 = new TestPoolable("value2");

        pool.put("key1", p1);
        pool.put("key2", p2);

        assertFalse(pool.isClosed());
        pool.close();
        assertTrue(pool.isClosed());

        assertTrue(p1.isDestroyed());
        assertTrue(p2.isDestroyed());
        assertEquals(Poolable.Caller.CLOSE, p1.getDestroyedByCaller());
        assertEquals(Poolable.Caller.CLOSE, p2.getDestroyedByCaller());

        assertThrows(IllegalStateException.class, () -> pool.put("key3", new TestPoolable("value3")));
        assertThrows(IllegalStateException.class, () -> pool.get("key1"));
    }

    @Test
    public void testCloseIdempotent() {
        pool.put("key1", new TestPoolable("value1"));
        pool.close();
        assertTrue(pool.isClosed());
        pool.close(); // Should not throw
        assertTrue(pool.isClosed());
    }

    @Test
    public void test_evict() {
        for (int i = 0; i < 10; i++) {
            pool.put("key" + i, new TestPoolable("value" + i));
        }

        pool.evict();
        assertEquals(8, pool.size());
    }

    @Test
    public void test_evictWithCustomBalanceFactor() {
        GenericKeyedObjectPool<String, TestPoolable> customPool = new GenericKeyedObjectPool<>(10, 0, EvictionPolicy.LAST_ACCESS_TIME, true, 0.5f);

        for (int i = 0; i < 10; i++) {
            customPool.put("key" + i, new TestPoolable("value" + i));
        }

        customPool.evict();
        assertEquals(5, customPool.size());

        customPool.close();
    }

    @Test
    public void testEvictWithExpirationTimePolicy() {
        GenericKeyedObjectPool<String, TestPoolable> expPool = new GenericKeyedObjectPool<>(10, 0, EvictionPolicy.EXPIRATION_TIME, true, 0.5f);

        for (int i = 0; i < 10; i++) {
            expPool.put("key" + i, new TestPoolable("value" + i));
        }

        expPool.evict();
        assertEquals(5, expPool.size());

        expPool.close();
    }

    @Test
    public void testMemoryTrackingDuringEviction() {
        KeyedObjectPool.MemoryMeasure<String, TestPoolable> measure = (k, v) -> k.length() + 100;
        GenericKeyedObjectPool<String, TestPoolable> memPool = new GenericKeyedObjectPool<>(10, 0, EvictionPolicy.LAST_ACCESS_TIME, true, 0.5f, 5000, measure);

        for (int i = 0; i < 10; i++) {
            memPool.put("key" + i, new TestPoolable("value" + i));
        }

        long dataSizeBefore = memPool.stats().dataSize();
        assertTrue(dataSizeBefore > 0);

        memPool.evict();
        long dataSizeAfter = memPool.stats().dataSize();
        assertTrue(dataSizeAfter < dataSizeBefore);

        memPool.close();
    }

    @Test
    public void testEvictMoreThanSize() {
        // When evict count >= pool size, entire pool should be cleared
        GenericKeyedObjectPool<String, TestPoolable> smallPool = new GenericKeyedObjectPool<>(5, 0, EvictionPolicy.LAST_ACCESS_TIME, true, 1.0f);
        for (int i = 0; i < 3; i++) {
            smallPool.put("key" + i, new TestPoolable("value" + i));
        }
        // balanceFactor=1.0 means evict all
        smallPool.evict();
        assertEquals(0, smallPool.size());
        smallPool.close();
    }

    @Test
    public void testEvict_RemovesRequestedNumberOfEntries() {
        TestPoolable v1 = new TestPoolable("v1");
        TestPoolable v2 = new TestPoolable("v2");
        TestPoolable v3 = new TestPoolable("v3");

        pool.put("key1", v1);
        pool.put("key2", v2);
        pool.put("key3", v3);

        pool.vacate(2);

        int destroyedCount = (v1.isDestroyed() ? 1 : 0) + (v2.isDestroyed() ? 1 : 0) + (v3.isDestroyed() ? 1 : 0);
        assertEquals(1, pool.size());
        assertEquals(2, destroyedCount);
    }

    @Test
    public void testEvictWithCreatedTimePolicy() throws InterruptedException {
        GenericKeyedObjectPool<String, TestPoolable> createdPool = new GenericKeyedObjectPool<>(10, 0, EvictionPolicy.CREATED_TIME, true, 0.5f);

        // Distinct creation times (ActivityPrint.createdTime is set at construction).
        TestPoolable v1 = new TestPoolable("v1");
        Thread.sleep(10);
        TestPoolable v2 = new TestPoolable("v2");
        Thread.sleep(10);
        TestPoolable v3 = new TestPoolable("v3");

        createdPool.put("k1", v1);
        createdPool.put("k2", v2);
        createdPool.put("k3", v3);

        // CREATED_TIME (and FIFO) evict the oldest-created first: vacate(2) destroys v1 and v2, keeps v3.
        createdPool.vacate(2);

        assertEquals(1, createdPool.size());
        assertTrue(v1.isDestroyed());
        assertTrue(v2.isDestroyed());
        assertFalse(v3.isDestroyed());

        createdPool.close();
    }

    @Test
    public void testEvictWithFifoPolicy() throws InterruptedException {
        GenericKeyedObjectPool<String, TestPoolable> fifoPool = new GenericKeyedObjectPool<>(10, 0, EvictionPolicy.FIFO, true, 0.5f);

        TestPoolable v1 = new TestPoolable("v1");
        Thread.sleep(10);
        TestPoolable v2 = new TestPoolable("v2");

        fifoPool.put("k1", v1);
        fifoPool.put("k2", v2);

        // FIFO evicts the first-added (oldest-created) first.
        fifoPool.vacate(1);

        assertEquals(1, fifoPool.size());
        assertTrue(v1.isDestroyed());
        assertFalse(v2.isDestroyed());

        fifoPool.close();
    }

    @Test
    public void testFifoEvictsFirstPutNotOldestCreated() throws InterruptedException {
        GenericKeyedObjectPool<String, TestPoolable> fifoPool = new GenericKeyedObjectPool<>(10, 0, EvictionPolicy.FIFO, true, 0.5f);

        TestPoolable older = new TestPoolable("older");
        Thread.sleep(10);
        TestPoolable newer = new TestPoolable("newer");

        fifoPool.put("newer", newer);
        fifoPool.put("older", older);

        fifoPool.vacate(1);

        assertEquals(1, fifoPool.size());
        assertTrue(newer.isDestroyed());
        assertFalse(older.isDestroyed());
        assertFalse(fifoPool.containsKey("newer"));
        assertTrue(fifoPool.containsKey("older"));

        fifoPool.close();
    }

    @Test
    public void testEvictEmptyPool() {
        pool.evict();
        assertEquals(0, pool.size());
    }

    @Test
    public void testEvictOnClosedPool() {
        pool.close();
        assertThrows(IllegalStateException.class, () -> pool.evict());
    }

    @Test
    public void testEvictWithAccessCountPolicy() throws InterruptedException {
        GenericKeyedObjectPool<String, TestPoolable> countPool = new GenericKeyedObjectPool<>(10, 0, EvictionPolicy.ACCESS_COUNT, true, 0.5f);

        for (int i = 0; i < 10; i++) {
            countPool.put("key" + i, new TestPoolable("value" + i));
        }

        // Access some entries multiple times
        for (int i = 0; i < 5; i++) {
            countPool.get("key" + i);
            countPool.get("key" + i);
        }

        countPool.evict();
        assertTrue(countPool.size() < 10);

        // The frequently accessed entries should be retained
        for (int i = 0; i < 5; i++) {
            assertTrue(countPool.containsKey("key" + i));
        }

        countPool.close();
    }

    @Test
    public void testConstructorBasic() {
        GenericKeyedObjectPool<String, TestPoolable> basicPool = new GenericKeyedObjectPool<>(50, 3000, EvictionPolicy.ACCESS_COUNT);
        assertEquals(50, basicPool.capacity());
        assertEquals(0, basicPool.size());
        assertFalse(basicPool.isClosed());
        basicPool.close();
    }

    @Test
    public void testSize() {
        assertEquals(0, pool.size());

        pool.put("key1", new TestPoolable("value1"));
        assertEquals(1, pool.size());

        pool.put("key2", new TestPoolable("value2"));
        assertEquals(2, pool.size());

        pool.remove("key1");
        assertEquals(1, pool.size());

        pool.remove("key2");
        assertEquals(0, pool.size());
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
    }

    @Test
    public void testMultiplePutAndRemove() {
        for (int i = 0; i < 5; i++) {
            pool.put("key" + i, new TestPoolable("value" + i));
        }
        assertEquals(5, pool.size());

        for (int i = 0; i < 5; i++) {
            assertNotNull(pool.remove("key" + i));
        }
        assertEquals(0, pool.size());
        assertNull(pool.remove("nonexistent"));
    }

    @Test
    public void testNegativeMemoryMeasure() {
        KeyedObjectPool.MemoryMeasure<String, TestPoolable> measure = (k, v) -> -1;
        GenericKeyedObjectPool<String, TestPoolable> memPool = new GenericKeyedObjectPool<>(10, 0, EvictionPolicy.LAST_ACCESS_TIME, 1000, measure);
        // Negative memory should cause put to fail
        assertFalse(memPool.put("key1", new TestPoolable("v1")));
        assertEquals(0, memPool.size());
        memPool.close();
    }

    @Test
    public void testEviction() throws InterruptedException {
        GenericKeyedObjectPool<String, TestPoolable> evictPool = new GenericKeyedObjectPool<>(10, 100, EvictionPolicy.LAST_ACCESS_TIME);

        TestPoolable shortLived = new TestPoolable("short", 50, 50);
        TestPoolable longLived = new TestPoolable("long", 10000, 10000);

        evictPool.put("short", shortLived);
        evictPool.put("long", longLived);

        assertEquals(2, evictPool.size());

        Thread.sleep(200);

        assertEquals(1, evictPool.size());
        assertTrue(evictPool.containsKey("long"));
        assertFalse(evictPool.containsKey("short"));
        assertTrue(shortLived.isDestroyed());
        assertEquals(Poolable.Caller.EVICT, shortLived.getDestroyedByCaller());

        evictPool.close();
    }

    @Test
    public void testSizeOnClosedPool() {
        pool.put("key1", new TestPoolable("value1"));
        pool.close();
        assertThrows(IllegalStateException.class, () -> pool.size());
    }

    @Test
    public void testConcurrentOperations() throws InterruptedException {
        int threads = 10;
        int opsPerThread = 100;
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(threads);
        AtomicInteger putCount = new AtomicInteger(0);
        AtomicInteger getCount = new AtomicInteger(0);

        for (int i = 0; i < threads; i++) {
            final int threadId = i;
            new Thread(() -> {
                try {
                    startLatch.await();
                    for (int j = 0; j < opsPerThread; j++) {
                        String key = "key" + (j % 20);
                        if (j % 2 == 0) {
                            if (pool.put(key, new TestPoolable("t" + threadId + "-" + j))) {
                                putCount.incrementAndGet();
                            }
                        } else {
                            if (pool.get(key) != null) {
                                getCount.incrementAndGet();
                            }
                        }
                    }
                } catch (InterruptedException e) {
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
    public void testLockAndUnlock() {
        pool.lock();
        pool.unlock();
        // Should not throw
        assertEquals(0, pool.size());
    }

    @Test
    public void testHashCode() {
        pool.put("key1", new TestPoolable("value1"));
        pool.put("key2", new TestPoolable("value2"));

        int hash1 = pool.hashCode();
        int hash2 = pool.hashCode();
        assertEquals(hash1, hash2);
    }

    @Test
    public void testEquals() {
        GenericKeyedObjectPool<String, TestPoolable> pool1 = new GenericKeyedObjectPool<>(10, 0, EvictionPolicy.LAST_ACCESS_TIME);
        GenericKeyedObjectPool<String, TestPoolable> pool2 = new GenericKeyedObjectPool<>(10, 0, EvictionPolicy.LAST_ACCESS_TIME);

        assertTrue(pool1.equals(pool1));
        assertTrue(pool1.equals(pool2));

        TestPoolable p = new TestPoolable("value1");
        pool1.put("key1", p);
        assertFalse(pool1.equals(pool2));

        pool2.put("key1", p);
        assertTrue(pool1.equals(pool2));

        assertFalse(pool1.equals(null));
        assertFalse(pool1.equals("not a pool"));

        pool1.close();
        pool2.close();
    }

    @Test
    public void testToStringContent() {
        String str = pool.toString();
        assertTrue(str.contains("GenericKeyedObjectPool"));
        assertTrue(str.contains("capacity=10"));
        assertTrue(str.contains("LAST_ACCESS_TIME"));
    }

    @Test
    public void testToString() {
        pool.put("key1", new TestPoolable("value1"));
        String str = pool.toString();
        assertNotNull(str);
        assertFalse(str.contains("key1"));
    }

    @Test
    public void testRemoveExpired_RemovesOnlyExpiredEntries() throws InterruptedException {
        TestPoolable expired = new TestPoolable("expired", 10, 10);
        TestPoolable retained = new TestPoolable("retained");

        pool.put("expired", expired);
        pool.put("retained", retained);
        Thread.sleep(20);

        pool.removeExpired();

        assertTrue(expired.isDestroyed());
        assertEquals(Poolable.Caller.EVICT, expired.getDestroyedByCaller());
        assertNotNull(pool.get("retained"));
        assertEquals(1, pool.size());
    }

    @Test
    public void testCapacity() {
        assertEquals(10, pool.capacity());
    }

    @Test
    public void testHitAndMissCountsOnGet() {
        pool.put("key1", new TestPoolable("value1"));

        pool.get("key1"); // hit
        pool.get("nonexistent"); // miss

        PoolStats stats = pool.stats();
        assertEquals(1, stats.hitCount());
        assertEquals(1, stats.missCount());
    }

    /**
     * Bug 1b: put() for a NEW key fails because pool is at capacity (autoBalance=false).
     * The existing entry under a different key must be completely unaffected.
     */
    @Test
    public void testBug1b_PutFailure_CapacityExceeded_ExistingEntryPreserved() {
        GenericKeyedObjectPool<String, TestPoolable> capacityPool = new GenericKeyedObjectPool<>(1, 0, EvictionPolicy.LAST_ACCESS_TIME, false, 0.2f);
        try {
            TestPoolable v1 = new TestPoolable("value1");
            assertTrue(capacityPool.put("K1", v1));

            boolean result = capacityPool.put("K2", new TestPoolable("value2"));

            assertFalse(result, "Put must fail: pool at capacity and autoBalance=false");
            assertFalse(v1.isDestroyed(), "Bug 1: v1 under K1 must NOT be destroyed by a failed put for K2");
            assertTrue(capacityPool.containsKey("K1"));
            assertEquals(1, capacityPool.size());
        } finally {
            capacityPool.close();
        }
    }

    /**
     * Bug 1c: Successful replacement must still destroy the old value (regression guard).
     * The fix must not break the normal replace path.
     */
    @Test
    public void testBug1c_PutSuccessfulReplacement_OldValueDestroyed() {
        KeyedObjectPool.MemoryMeasure<String, TestPoolable> measure = (k, v) -> 50L;
        GenericKeyedObjectPool<String, TestPoolable> memPool = new GenericKeyedObjectPool<>(10, 0, EvictionPolicy.LAST_ACCESS_TIME, false, 0.2f, 1000, measure);
        try {
            TestPoolable original = new TestPoolable("original");
            assertTrue(memPool.put("K", original));

            TestPoolable replacement = new TestPoolable("replacement");
            assertTrue(memPool.put("K", replacement), "Replacement must succeed");

            assertTrue(original.isDestroyed(), "Old value MUST be destroyed on a successful replacement");
            assertEquals(Poolable.Caller.REMOVE_REPLACE_CLEAR, original.getDestroyedByCaller());
            assertEquals(replacement, memPool.get("K"));
        } finally {
            memPool.close();
        }
    }

    /**
     * Bug 1d: put() with memory limit exceeded for a new key (no existing entry).
     * put() must return false and the pool must remain unchanged.
     */
    @Test
    public void testBug1d_PutFailure_MemoryLimitExceeded_NewKey_PoolUnchanged() {
        KeyedObjectPool.MemoryMeasure<String, TestPoolable> measure = (k, v) -> 100L;
        GenericKeyedObjectPool<String, TestPoolable> memPool = new GenericKeyedObjectPool<>(10, 0, EvictionPolicy.LAST_ACCESS_TIME, false, 0.2f, 150, measure);
        try {
            TestPoolable p1 = new TestPoolable("value1");
            assertTrue(memPool.put("K1", p1), "First put must succeed (100 <= 150)");

            boolean result = memPool.put("K2", new TestPoolable("value2"));
            assertFalse(result, "Second put must fail: 100+100=200 > 150");
            assertFalse(p1.isDestroyed(), "p1 under K1 must not be destroyed");
            assertEquals(1, memPool.size(), "Pool size must remain 1");
        } finally {
            memPool.close();
        }
    }

    /**
     * Bug: get() checks isClosed only BEFORE acquiring the lock. A concurrent close() can
     * set isClosed=true and release the lock before invoking removeAll() to drain the pool.
     * A get() that won the race for the lock between those two steps could read a "live"
     * element out of a pool already marked closed. Fix: re-check assertNotClosed() inside
     * the lock.
     */
    @Test
    public void testGet_raceWithClose_doesNotReturnElementFromClosedPool() throws Exception {
        final TestPoolable v = new TestPoolable("racing");
        pool.put("K", v);

        final java.util.concurrent.locks.ReentrantLock poolLock = pool.lock;
        final CountDownLatch holderHasLock = new CountDownLatch(1);
        final CountDownLatch mayProceed = new CountDownLatch(1);

        Thread holder = new Thread(() -> {
            poolLock.lock();
            try {
                holderHasLock.countDown();
                mayProceed.await();
                pool.isClosed = true;
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            } finally {
                poolLock.unlock();
            }
        });
        holder.start();
        holderHasLock.await();

        java.util.concurrent.atomic.AtomicBoolean threwISE = new java.util.concurrent.atomic.AtomicBoolean(false);
        java.util.concurrent.atomic.AtomicBoolean returned = new java.util.concurrent.atomic.AtomicBoolean(false);
        Thread getter = new Thread(() -> {
            try {
                TestPoolable t = pool.get("K");
                if (t != null) {
                    returned.set(true);
                }
            } catch (IllegalStateException e) {
                threwISE.set(true);
            }
        });
        getter.start();

        Thread.sleep(50);
        mayProceed.countDown();
        holder.join();
        getter.join();

        assertTrue(threwISE.get(), "get() must throw IllegalStateException when isClosed was set under the lock");
        assertFalse(returned.get(), "get() must not return an element from a pool already marked closed");
    }

    /**
     * Bug: remove() has the same race as get() — assertNotClosed runs only outside the lock.
     */
    @Test
    public void testRemove_raceWithClose_throwsAfterCloseUnderLock() throws Exception {
        pool.put("K", new TestPoolable("racing"));

        final java.util.concurrent.locks.ReentrantLock poolLock = pool.lock;
        final CountDownLatch holderHasLock = new CountDownLatch(1);
        final CountDownLatch mayProceed = new CountDownLatch(1);

        Thread holder = new Thread(() -> {
            poolLock.lock();
            try {
                holderHasLock.countDown();
                mayProceed.await();
                pool.isClosed = true;
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            } finally {
                poolLock.unlock();
            }
        });
        holder.start();
        holderHasLock.await();

        java.util.concurrent.atomic.AtomicBoolean threwISE = new java.util.concurrent.atomic.AtomicBoolean(false);
        java.util.concurrent.atomic.AtomicBoolean returned = new java.util.concurrent.atomic.AtomicBoolean(false);
        Thread remover = new Thread(() -> {
            try {
                TestPoolable t = pool.remove("K");
                if (t != null) {
                    returned.set(true);
                }
            } catch (IllegalStateException e) {
                threwISE.set(true);
            }
        });
        remover.start();

        Thread.sleep(50);
        mayProceed.countDown();
        holder.join();
        remover.join();

        assertTrue(threwISE.get(), "remove() must throw IllegalStateException when isClosed was set under the lock");
        assertFalse(returned.get(), "remove() must not return an element from a pool already marked closed");
    }

    /**
     * Bug: peek() has the same race as get() — assertNotClosed runs only outside the lock.
     */
    @Test
    public void testPeek_raceWithClose_throwsAfterCloseUnderLock() throws Exception {
        pool.put("K", new TestPoolable("racing"));

        final java.util.concurrent.locks.ReentrantLock poolLock = pool.lock;
        final CountDownLatch holderHasLock = new CountDownLatch(1);
        final CountDownLatch mayProceed = new CountDownLatch(1);

        Thread holder = new Thread(() -> {
            poolLock.lock();
            try {
                holderHasLock.countDown();
                mayProceed.await();
                pool.isClosed = true;
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            } finally {
                poolLock.unlock();
            }
        });
        holder.start();
        holderHasLock.await();

        java.util.concurrent.atomic.AtomicBoolean threwISE = new java.util.concurrent.atomic.AtomicBoolean(false);
        java.util.concurrent.atomic.AtomicBoolean returned = new java.util.concurrent.atomic.AtomicBoolean(false);
        Thread peeker = new Thread(() -> {
            try {
                TestPoolable t = pool.peek("K");
                if (t != null) {
                    returned.set(true);
                }
            } catch (IllegalStateException e) {
                threwISE.set(true);
            }
        });
        peeker.start();

        Thread.sleep(50);
        mayProceed.countDown();
        holder.join();
        peeker.join();

        assertTrue(threwISE.get(), "peek() must throw IllegalStateException when isClosed was set under the lock");
        assertFalse(returned.get(), "peek() must not return an element from a pool already marked closed");
    }

    /**
     * Regression test for remove() leaking the entry when memoryMeasure.sizeOf() throws.
     *
     * <p>Before the fix, an unchecked exception thrown by a user-supplied MemoryMeasure during
     * remove() propagated out of the method while the entry had already been removed from the
     * internal map — never returned to the caller, never destroyed, leaked. The fix wraps
     * sizeOf() in a try/catch so the removed entry is still returned and the caller remains
     * responsible for it.
     */
    @Test
    public void testRemoveDoesNotLeakElementWhenMemoryMeasureThrows() {
        final java.util.concurrent.atomic.AtomicBoolean throwOnNext = new java.util.concurrent.atomic.AtomicBoolean(false);
        final KeyedObjectPool.MemoryMeasure<String, TestPoolable> conditionalMeasure = (k, v) -> {
            if (throwOnNext.get()) {
                throw new RuntimeException("simulated sizeOf failure on remove");
            }
            return 0L;
        };

        GenericKeyedObjectPool<String, TestPoolable> p = new GenericKeyedObjectPool<>(10, 0, EvictionPolicy.LAST_ACCESS_TIME, true, 0.2f, 1024L * 1024L,
                conditionalMeasure);
        try {
            TestPoolable original = new TestPoolable("memMeasureRemove");
            assertTrue(p.put("k1", original));
            assertEquals(1, p.size());

            throwOnNext.set(true);

            // Pre-fix: remove() throws RuntimeException and the entry is leaked (gone from map,
            // never returned, never destroyed). Post-fix: remove() catches and returns the entry.
            TestPoolable removed = p.remove("k1");
            assertNotNull(removed, "remove() must return the entry even if memoryMeasure.sizeOf() throws");
            assertEquals("memMeasureRemove", removed.getValue());
            assertFalse(removed.isDestroyed(), "remove() must not destroy the returned element (caller owns it)");
            assertEquals(0, p.size());
        } finally {
            p.close();
        }
    }

    /**
     * Regression test for equals() reading the other pool's internal map without holding its
     * lock. Before the fix, GenericKeyedObjectPool.equals(other) iterated other.pool while only
     * locking this.lock — a thread mutating `other` concurrently could trigger
     * ConcurrentModificationException or a torn comparison. The fix snapshots both pools under
     * their own locks before comparing.
     */
    @Test
    public void testEqualsIsSafeUnderConcurrentMutationOfOtherPool() throws InterruptedException {
        final GenericKeyedObjectPool<String, TestPoolable> p1 = new GenericKeyedObjectPool<>(1000, 0, EvictionPolicy.LAST_ACCESS_TIME);
        final GenericKeyedObjectPool<String, TestPoolable> p2 = new GenericKeyedObjectPool<>(1000, 0, EvictionPolicy.LAST_ACCESS_TIME);

        // Seed both pools so equals() actually has to iterate.
        for (int i = 0; i < 200; i++) {
            p1.put("k" + i, new TestPoolable("v" + i));
            p2.put("k" + i, new TestPoolable("v" + i));
        }

        final java.util.concurrent.atomic.AtomicBoolean stop = new java.util.concurrent.atomic.AtomicBoolean(false);
        final java.util.concurrent.atomic.AtomicReference<Throwable> mutatorFailure = new java.util.concurrent.atomic.AtomicReference<>();
        Thread mutator = new Thread(() -> {
            int n = 0;
            try {
                while (!stop.get()) {
                    final String key = "k" + (n++ % 200);
                    p2.remove(key);
                    p2.put(key, new TestPoolable("vv" + n));
                }
            } catch (Throwable t) {
                mutatorFailure.set(t);
            }
        });
        mutator.start();

        final java.util.concurrent.atomic.AtomicReference<Throwable> equalsFailure = new java.util.concurrent.atomic.AtomicReference<>();
        try {
            // Many equals() invocations against the concurrently-mutating p2.
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

        assertNull(equalsFailure.get(), "equals() must not throw when the other pool is concurrently mutated; got: " + equalsFailure.get());
        assertNull(mutatorFailure.get(), "concurrent mutator must not have crashed; got: " + mutatorFailure.get());
        p1.close();
        p2.close();
    }

    /**
     * Regression test for put() propagating exception when memoryMeasure.sizeOf() throws
     * during value replacement.
     *
     * <p>By design, put() removes and destroys the old value before measuring the new one.
     * Before the fix, an unchecked exception thrown by a user-supplied MemoryMeasure during
     * put() for an already-existing key propagated out of the method while the old value had
     * already been removed and destroyed — losing the entry. The fix wraps sizeOf() in a
     * try/catch so the put returns {@code false} cleanly instead of throwing.
     */
    @Test
    public void testPutDoesNotLeakOldValueWhenMemoryMeasureThrows() {
        final java.util.concurrent.atomic.AtomicBoolean throwOnNext = new java.util.concurrent.atomic.AtomicBoolean(false);
        final KeyedObjectPool.MemoryMeasure<String, TestPoolable> conditionalMeasure = (k, v) -> {
            if (throwOnNext.get()) {
                throw new RuntimeException("simulated sizeOf failure on put");
            }
            return 100L;
        };

        GenericKeyedObjectPool<String, TestPoolable> p = new GenericKeyedObjectPool<>(10, 0, EvictionPolicy.LAST_ACCESS_TIME, true, 0.2f, 1024L * 1024L,
                conditionalMeasure);
        try {
            TestPoolable original = new TestPoolable("memMeasurePut");
            assertTrue(p.put("k1", original));
            assertEquals(1, p.size());

            throwOnNext.set(true);

            // Pre-fix: put() removes the old value (lines 254-258), then sizeOf() throws,
            // the exception propagates to the caller — old value already destroyed.
            // Post-fix: put() catches the exception, logs it, and returns false.
            boolean putResult = p.put("k1", new TestPoolable("shouldNotBePut"));
            assertFalse(putResult, "put() must return false when memoryMeasure throws");

            // The old value was already destroyed per design, and the new value was not added.
            // Pool should be empty; more importantly, no exception propagated.
            assertEquals(0, p.size(), "pool should be empty after failed put");
        } finally {
            p.close();
        }
    }

    /**
     * Regression for the get() hit/miss accounting refactor: moving the counter update out
     * of the {@code finally} block must preserve correct accounting on the normal path
     * (present-key hit, absent-key miss, expired-key miss) and must not record a miss when
     * get() aborts on a closed pool.
     */
    @Test
    public void testGet_HitMissAccountingAfterRefactor() throws InterruptedException {
        GenericKeyedObjectPool<String, TestPoolable> p = new GenericKeyedObjectPool<>(10, 0, EvictionPolicy.LAST_ACCESS_TIME);

        p.put("k1", new TestPoolable("v1"));
        assertNotNull(p.get("k1")); // hit
        assertNull(p.get("absent")); // miss

        p.put("short", new TestPoolable("s", 1, 1));
        Thread.sleep(20);
        assertNull(p.get("short")); // expired -> miss

        assertEquals(1, p.hitCount.get(), "exactly one hit");
        assertEquals(2, p.missCount.get(), "exactly two misses");

        p.close();
        // Already-closed get() throws before acquiring the lock; counters must be untouched.
        assertThrows(IllegalStateException.class, () -> p.get("k1"));
        assertEquals(1, p.hitCount.get(), "closed get() must not change hitCount");
        assertEquals(2, p.missCount.get(), "closed get() must not change missCount");
    }

    // --- regression tests for 2026-06-10 deep-review fixes ---

    @Test
    public void testPutBackSameInstanceDoesNotDestroyIt() {
        // regression: put(key, value) destroyed the old mapping without an identity check, so the
        // documented "put it back" pattern (get() is non-removing) destroyed the live resource
        // and re-pooled the corpse
        GenericKeyedObjectPool<String, TestPoolable> p = new GenericKeyedObjectPool<>(10, 0, EvictionPolicy.LAST_ACCESS_TIME);
        TestPoolable resource = new TestPoolable("res");

        assertTrue(p.put("k", resource));

        TestPoolable borrowed = p.get("k");
        org.junit.jupiter.api.Assertions.assertSame(resource, borrowed);

        assertTrue(p.put("k", borrowed)); // documented "put it back" pattern

        assertFalse(resource.isDestroyed(), "re-putting the same instance must not destroy it");
        org.junit.jupiter.api.Assertions.assertSame(resource, p.get("k"));

        // replacing with a DIFFERENT instance still destroys the old one
        TestPoolable replacement = new TestPoolable("res2");
        assertTrue(p.put("k", replacement));
        assertTrue(resource.isDestroyed());
        assertFalse(replacement.isDestroyed());
    }

    /**
     * Regression: re-pooling the SAME instance under the same key with a memory measure configured
     * must not double-count its memory in totalDataSize. Previously put() removed the old mapping
     * (without adjusting totalDataSize) and, because the same-instance identity guard skipped
     * destroy() (which is what otherwise subtracts the memory), the success path's unconditional
     * addAndGet() counted the instance's memory twice.
     */
    @Test
    public void testPutBackSameInstanceWithMemoryMeasureDoesNotDoubleCount() {
        KeyedObjectPool.MemoryMeasure<String, TestPoolable> measure = (k, v) -> k.length() + 100;
        GenericKeyedObjectPool<String, TestPoolable> memPool = new GenericKeyedObjectPool<>(10, 0, EvictionPolicy.LAST_ACCESS_TIME, 10000, measure);
        try {
            TestPoolable resource = new TestPoolable("res");
            assertTrue(memPool.put("k1", resource)); // "k1".length()(2) + 100 = 102
            assertEquals(102, memPool.stats().dataSize());

            // documented non-removing get() leaves the mapping in place
            TestPoolable borrowed = memPool.get("k1");
            org.junit.jupiter.api.Assertions.assertSame(resource, borrowed);

            // "put it back" pattern: re-pool the same instance; memory must stay 102, not 204
            assertTrue(memPool.put("k1", borrowed));
            assertEquals(102, memPool.stats().dataSize(), "re-pooling the same instance must not double-count memory");
            assertEquals(1, memPool.size());
            assertFalse(resource.isDestroyed());

            // re-pooling once more must still keep the accounting stable
            assertTrue(memPool.put("k1", memPool.get("k1")));
            assertEquals(102, memPool.stats().dataSize(), "repeated re-pooling must keep memory accounting stable");

            // removing the entry must bring memory back to 0 (would be negative/positive-leftover if double-counted)
            memPool.remove("k1");
            assertEquals(0, memPool.stats().dataSize(), "memory must return to 0 after removing the only entry");
        } finally {
            memPool.close();
        }
    }

    // ============================ Timed put/get (M29) ============================

    @Test
    public void testTimedPut_SucceedsImmediatelyWhenSpaceAvailable() throws InterruptedException {
        TestPoolable v = new TestPoolable("v");
        assertTrue(pool.put("k", v, 1, TimeUnit.SECONDS));
        assertEquals(1, pool.size());
        assertSameValue(v, pool.get("k"));
    }

    @Test
    public void testTimedPut_ReturnsFalseOnTimeoutWhenFullNoAutoBalance() throws InterruptedException {
        GenericKeyedObjectPool<String, TestPoolable> p = new GenericKeyedObjectPool<>(1, 0, EvictionPolicy.LAST_ACCESS_TIME, false, 0.2f);
        try {
            assertTrue(p.put("k1", new TestPoolable("v1")));
            assertEquals(1, p.size());

            long start = System.nanoTime();
            // Pool is full and auto-balance is off; nobody removes an entry, so this must time out.
            assertFalse(p.put("k2", new TestPoolable("v2"), 100, TimeUnit.MILLISECONDS));
            long elapsedMs = (System.nanoTime() - start) / 1_000_000;
            assertTrue(elapsedMs >= 50, "should have waited roughly the timeout, waited " + elapsedMs + "ms");
            assertEquals(1, p.size());
        } finally {
            p.close();
        }
    }

    @Test
    public void testTimedPut_WaitsThenSucceedsWhenSlotFreed() throws InterruptedException {
        final GenericKeyedObjectPool<String, TestPoolable> p = new GenericKeyedObjectPool<>(1, 0, EvictionPolicy.LAST_ACCESS_TIME, false, 0.2f);
        try {
            assertTrue(p.put("k1", new TestPoolable("v1")));

            // Free the slot shortly after the timed put begins waiting.
            Thread freer = new Thread(() -> {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException ignored) {
                    Thread.currentThread().interrupt();
                }
                p.remove("k1");
            });
            freer.start();

            assertTrue(p.put("k2", new TestPoolable("v2"), 5, TimeUnit.SECONDS));
            freer.join();
            assertTrue(p.containsKey("k2"));
        } finally {
            p.close();
        }
    }

    @Test
    public void testTimedPut_ExpiredValueReturnsFalse() throws InterruptedException {
        TestPoolable expired = new TestPoolable("e", 1, 1);
        Thread.sleep(10);
        assertFalse(pool.put("k", expired, 1, TimeUnit.SECONDS));
        assertEquals(0, pool.size());
    }

    @Test
    public void testTimedPut_NullKeyOrValueThrows() {
        assertThrows(IllegalArgumentException.class, () -> pool.put(null, new TestPoolable("v"), 1, TimeUnit.SECONDS));
        assertThrows(IllegalArgumentException.class, () -> pool.put("k", null, 1, TimeUnit.SECONDS));
    }

    @Test
    public void testTimedPut_ClosedPoolThrows() {
        pool.close();
        assertThrows(IllegalStateException.class, () -> pool.put("k", new TestPoolable("v"), 1, TimeUnit.SECONDS));
    }

    @Test
    public void testTimedPutWithAutoDestroy_DestroysOnTimeout() throws InterruptedException {
        GenericKeyedObjectPool<String, TestPoolable> p = new GenericKeyedObjectPool<>(1, 0, EvictionPolicy.LAST_ACCESS_TIME, false, 0.2f);
        try {
            assertTrue(p.put("k1", new TestPoolable("v1")));

            TestPoolable v2 = new TestPoolable("v2");
            assertFalse(p.put("k2", v2, 100, TimeUnit.MILLISECONDS, true));
            assertTrue(v2.isDestroyed());
            assertEquals(Poolable.Caller.PUT_ADD_FAILURE, v2.getDestroyedByCaller());
        } finally {
            p.close();
        }
    }

    @Test
    public void testTimedGet_ReturnsImmediatelyWhenPresent() throws InterruptedException {
        TestPoolable v = new TestPoolable("v");
        pool.put("k", v);
        TestPoolable got = pool.get("k", 1, TimeUnit.SECONDS);
        assertSameValue(v, got);
        // get() does not remove: the element stays in the pool.
        assertEquals(1, pool.size());
    }

    @Test
    public void testTimedGet_ReturnsNullOnTimeoutWhenAbsent() throws InterruptedException {
        long start = System.nanoTime();
        TestPoolable got = pool.get("missing", 100, TimeUnit.MILLISECONDS);
        long elapsedMs = (System.nanoTime() - start) / 1_000_000;
        assertNull(got);
        assertTrue(elapsedMs >= 50, "should have waited roughly the timeout, waited " + elapsedMs + "ms");
    }

    @Test
    public void testTimedGet_WaitsThenSucceedsWhenKeyPopulated() throws InterruptedException {
        final CountDownLatch ready = new CountDownLatch(1);
        Thread producer = new Thread(() -> {
            try {
                ready.await();
                Thread.sleep(50);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
            pool.put("k", new TestPoolable("v"));
        });
        producer.start();

        ready.countDown();
        TestPoolable got = pool.get("k", 5, TimeUnit.SECONDS);
        producer.join();
        assertNotNull(got);
        assertEquals("v", got.getValue());
    }

    @Test
    public void testTimedGet_ClosedPoolThrows() {
        pool.close();
        assertThrows(IllegalStateException.class, () -> pool.get("k", 1, TimeUnit.SECONDS));
    }

    /**
     * Regression for pool NEW-1 (heterogeneous key-waiter wakeup). A waiter blocked in
     * get(KEY_A, ...) must not be starved when a put for an unrelated key (KEY_B) and a
     * separate waiter (on KEY_C) are involved: under the old single-waiter notEmpty.signal(),
     * the put(KEY_A) signal could be consumed by the KEY_C waiter (which re-checks, misses C,
     * and re-awaits), leaving the KEY_A waiter to spuriously time out even though A is present.
     * notEmpty.signalAll() wakes every key-waiter so each re-checks its own key.
     */
    @Test
    public void testTimedGet_HeterogeneousWaiters_NotStarvedByForeignSignal() throws InterruptedException {
        final GenericKeyedObjectPool<String, TestPoolable> p = new GenericKeyedObjectPool<>(8, 0, EvictionPolicy.LAST_ACCESS_TIME);
        try {
            final long timeoutMs = 10_000; // generous so the test is not flaky
            final CountDownLatch waitersStarted = new CountDownLatch(2);
            final AtomicReference<TestPoolable> resultA = new AtomicReference<>();
            final AtomicReference<TestPoolable> resultC = new AtomicReference<>();
            final AtomicReference<Throwable> failure = new AtomicReference<>();

            // Waiter on KEY_A: this is the thread that must NOT be starved.
            final Thread waiterA = new Thread(() -> {
                waitersStarted.countDown();
                try {
                    resultA.set(p.get("A", timeoutMs, TimeUnit.MILLISECONDS));
                } catch (final Throwable t) {
                    failure.set(t);
                }
            }, "waiter-A");

            // A second waiter on a different key (KEY_C) so a foreign signal has somewhere to go.
            final Thread waiterC = new Thread(() -> {
                waitersStarted.countDown();
                try {
                    resultC.set(p.get("C", timeoutMs, TimeUnit.MILLISECONDS));
                } catch (final Throwable t) {
                    failure.set(t);
                }
            }, "waiter-C");

            waiterA.start();
            waiterC.start();

            // Wait for both threads to launch, then give them a moment to actually park on notEmpty.
            assertTrue(waitersStarted.await(2, TimeUnit.SECONDS), "waiter threads did not start");
            Thread.sleep(150);

            // put(KEY_B) must NOT satisfy the KEY_A (or KEY_C) waiter; it only fires a notEmpty wakeup.
            p.put("B", new TestPoolable("vB"));
            Thread.sleep(50);
            // Now make KEY_A available. The KEY_A waiter must observe it well within the timeout.
            p.put("A", new TestPoolable("vA"));
            // And satisfy KEY_C too so its thread can finish cleanly.
            p.put("C", new TestPoolable("vC"));

            // Join with a bound far below the get() timeout: if the A-waiter were starved it would
            // still be blocked on its 10s timeout here and the join would expire.
            waiterA.join(4_000);
            waiterC.join(4_000);

            assertNull(failure.get(), "waiter threads threw: " + failure.get());
            assertFalse(waiterA.isAlive(), "KEY_A waiter did not return well within its timeout (starved)");
            assertNotNull(resultA.get(), "KEY_A waiter returned null despite A being put before timeout");
            assertEquals("vA", resultA.get().getValue());
            assertNotNull(resultC.get(), "KEY_C waiter returned null despite C being put before timeout");
            assertEquals("vC", resultC.get().getValue());
        } finally {
            p.close();
        }
    }

    private static void assertSameValue(TestPoolable expected, TestPoolable actual) {
        assertNotNull(actual);
        assertEquals(expected.getValue(), actual.getValue());
    }

    @Test
    public void testPut_failedReplaceWakesNotFullWaiters() throws Exception {
        // regression: non-timed put removed the existing same-key entry then returned false
        // without signaling notFull, stranding timed put waiters.
        // Use a memory measure that rejects the replacement AFTER the up-front remove.
        final KeyedObjectPool.MemoryMeasure<String, TestPoolable> measure = (k, v) -> {
            if ("reject".equals(v.getValue())) {
                return -1L; // triggers fail path after remove
            }
            return 10L;
        };
        final GenericKeyedObjectPool<String, TestPoolable> p = new GenericKeyedObjectPool<>(1, 0, EvictionPolicy.LAST_ACCESS_TIME, false, 0.2f, 10_000L,
                measure);
        try {
            assertTrue(p.put("A", new TestPoolable("oldA")));

            final CountDownLatch waiterStarted = new CountDownLatch(1);
            final AtomicReference<Boolean> putResult = new AtomicReference<>();
            final AtomicReference<Throwable> failure = new AtomicReference<>();

            final Thread waiter = new Thread(() -> {
                try {
                    waiterStarted.countDown();
                    // Blocks until capacity frees (notFull). Must succeed after failed replace of A.
                    final boolean ok = p.put("B", new TestPoolable("vB"), 5, TimeUnit.SECONDS);
                    putResult.set(ok);
                } catch (final Throwable t) {
                    failure.set(t);
                }
            }, "notFull-waiter");
            waiter.start();

            assertTrue(waiterStarted.await(2, TimeUnit.SECONDS));
            Thread.sleep(150); // allow waiter to park on notFull

            // Replace key A: remove succeeds, measure rejects, value not stored → must wake notFull.
            assertFalse(p.put("A", new TestPoolable("reject")));

            waiter.join(4_000);
            assertNull(failure.get(), "waiter threw: " + failure.get());
            assertFalse(waiter.isAlive(), "notFull waiter did not return after failed replace freed a slot");
            assertEquals(Boolean.TRUE, putResult.get(), "timed put(B) should succeed after A was removed and not re-stored");
            assertEquals(1, p.size());
            assertNotNull(p.get("B"));
        } finally {
            p.close();
        }
    }
}

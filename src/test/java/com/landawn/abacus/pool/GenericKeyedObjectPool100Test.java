package com.landawn.abacus.pool;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

@Tag("new-test")
public class GenericKeyedObjectPool100Test extends TestBase {

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

    @Test
    public void testConstructorBasic() {
        GenericKeyedObjectPool<String, TestPoolable> basicPool = new GenericKeyedObjectPool<>(50, 3000, EvictionPolicy.ACCESS_COUNT);
        assertEquals(50, basicPool.capacity());
        assertEquals(0, basicPool.size());
        assertFalse(basicPool.isClosed());
        basicPool.close();
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
    public void testPut() {
        TestPoolable poolable = new TestPoolable("value1");
        assertTrue(pool.put("key1", poolable));
        assertEquals(1, pool.size());
        assertTrue(pool.containsKey("key1"));
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
        assertFalse(memPool.put("k3", new TestPoolable("v3")));

        memPool.close();
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
    public void testPeek() {
        TestPoolable poolable = new TestPoolable("value1");
        pool.put("key1", poolable);

        TestPoolable peeked = pool.peek("key1");
        assertNotNull(peeked);
        assertEquals(poolable, peeked);
        assertEquals(0, peeked.activityPrint().getAccessCount());
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
    public void testContainsKey() {
        pool.put("key1", new TestPoolable("value1"));
        pool.put("key2", new TestPoolable("value2"));

        assertTrue(pool.containsKey("key1"));
        assertTrue(pool.containsKey("key2"));
        assertFalse(pool.containsKey("key3"));
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
    public void testVacate() {
        for (int i = 0; i < 10; i++) {
            pool.put("key" + i, new TestPoolable("value" + i));
        }

        pool.vacate();
        assertEquals(8, pool.size());
    }

    @Test
    public void testVacateWithCustomBalanceFactor() {
        GenericKeyedObjectPool<String, TestPoolable> customPool = new GenericKeyedObjectPool<>(10, 0, EvictionPolicy.LAST_ACCESS_TIME, true, 0.5f);

        for (int i = 0; i < 10; i++) {
            customPool.put("key" + i, new TestPoolable("value" + i));
        }

        customPool.vacate();
        assertEquals(5, customPool.size());

        customPool.close();
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
    public void testToString() {
        pool.put("key1", new TestPoolable("value1"));
        String str = pool.toString();
        assertNotNull(str);
        assertFalse(str.contains("key1"));
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
}

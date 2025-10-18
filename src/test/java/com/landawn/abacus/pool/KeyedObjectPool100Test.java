package com.landawn.abacus.pool;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

@Tag("new-test")
public class KeyedObjectPool100Test extends TestBase {

    private TestKeyedObjectPool pool;

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

    private static class TestKeyedObjectPool implements KeyedObjectPool<String, TestPoolable> {
        private final Map<String, TestPoolable> map = new HashMap<>();
        private final int capacity;
        private boolean closed = false;
        private final Object lock = new Object();
        private long totalMemorySize = 0;
        private final KeyedObjectPool.MemoryMeasure<String, TestPoolable> memoryMeasure;

        TestKeyedObjectPool(int capacity) {
            this(capacity, null);
        }

        TestKeyedObjectPool(int capacity, KeyedObjectPool.MemoryMeasure<String, TestPoolable> memoryMeasure) {
            this.capacity = capacity;
            this.memoryMeasure = memoryMeasure;
        }

        @Override
        public boolean put(String key, TestPoolable e) {
            if (closed)
                throw new IllegalStateException("Pool is closed");
            if (key == null || e == null)
                throw new IllegalArgumentException("Key and value cannot be null");
            if (e.activityPrint().isExpired())
                return false;

            synchronized (lock) {
                if (map.size() >= capacity)
                    return false;

                TestPoolable old = map.put(key, e);
                if (old != null) {
                    old.destroy(Poolable.Caller.REMOVE_REPLACE_CLEAR);
                    if (memoryMeasure != null) {
                        totalMemorySize -= memoryMeasure.sizeOf(key, old);
                    }
                }

                if (memoryMeasure != null) {
                    totalMemorySize += memoryMeasure.sizeOf(key, e);
                }
                return true;
            }
        }

        @Override
        public boolean put(String key, TestPoolable e, boolean autoDestroyOnFailedToPut) {
            boolean success = false;
            try {
                success = put(key, e);
            } finally {
                if (!success && autoDestroyOnFailedToPut && e != null) {
                    e.destroy(Poolable.Caller.PUT_ADD_FAILURE);
                }
            }
            return success;
        }

        @Override
        public TestPoolable get(String key) {
            if (closed)
                throw new IllegalStateException("Pool is closed");

            synchronized (lock) {
                TestPoolable e = map.get(key);
                if (e != null) {
                    if (e.activityPrint().isExpired()) {
                        map.remove(key);
                        e.destroy(Poolable.Caller.EVICT);
                        if (memoryMeasure != null) {
                            totalMemorySize -= memoryMeasure.sizeOf(key, e);
                        }
                        return null;
                    }
                    e.activityPrint().updateLastAccessTime();
                    e.activityPrint().updateAccessCount();
                }
                return e;
            }
        }

        @Override
        public TestPoolable remove(String key) {
            if (closed)
                throw new IllegalStateException("Pool is closed");

            synchronized (lock) {
                TestPoolable e = map.remove(key);
                if (e != null) {
                    e.activityPrint().updateLastAccessTime();
                    e.activityPrint().updateAccessCount();
                    if (memoryMeasure != null) {
                        totalMemorySize -= memoryMeasure.sizeOf(key, e);
                    }
                }
                return e;
            }
        }

        @Override
        public TestPoolable peek(String key) {
            if (closed)
                throw new IllegalStateException("Pool is closed");

            synchronized (lock) {
                TestPoolable e = map.get(key);
                if (e != null && e.activityPrint().isExpired()) {
                    map.remove(key);
                    e.destroy(Poolable.Caller.EVICT);
                    if (memoryMeasure != null) {
                        totalMemorySize -= memoryMeasure.sizeOf(key, e);
                    }
                    return null;
                }
                return e;
            }
        }

        @Override
        public Set<String> keySet() {
            if (closed)
                throw new IllegalStateException("Pool is closed");
            synchronized (lock) {
                return new HashSet<>(map.keySet());
            }
        }

        @Override
        public Collection<TestPoolable> values() {
            if (closed)
                throw new IllegalStateException("Pool is closed");
            synchronized (lock) {
                return new ArrayList<>(map.values());
            }
        }

        @Override
        public boolean containsKey(String key) {
            if (closed)
                throw new IllegalStateException("Pool is closed");
            synchronized (lock) {
                return map.containsKey(key);
            }
        }

        @Override
        public void lock() {
        }

        @Override
        public void unlock() {
        }

        @Override
        public int capacity() {
            return capacity;
        }

        @Override
        public int size() {
            synchronized (lock) {
                return map.size();
            }
        }

        @Override
        public boolean isEmpty() {
            return size() == 0;
        }

        @Override
        public void vacate() {
            if (closed)
                throw new IllegalStateException("Pool is closed");
            synchronized (lock) {
                int toRemove = Math.max(1, map.size() / 5);
                Iterator<Map.Entry<String, TestPoolable>> it = map.entrySet().iterator();
                for (int i = 0; i < toRemove && it.hasNext(); i++) {
                    Map.Entry<String, TestPoolable> entry = it.next();
                    if (memoryMeasure != null) {
                        totalMemorySize -= memoryMeasure.sizeOf(entry.getKey(), entry.getValue());
                    }
                    it.remove();
                }
            }
        }

        @Override
        public void clear() {
            if (closed)
                throw new IllegalStateException("Pool is closed");
            synchronized (lock) {
                for (Map.Entry<String, TestPoolable> entry : map.entrySet()) {
                    entry.getValue().destroy(Poolable.Caller.REMOVE_REPLACE_CLEAR);
                }
                map.clear();
                totalMemorySize = 0;
            }
        }

        @Override
        public PoolStats stats() {
            return new PoolStats(capacity, size(), 0, 0, 0, 0, 0, -1, -1);
        }

        @Override
        public void close() {
            closed = true;
            synchronized (lock) {
                for (Map.Entry<String, TestPoolable> entry : map.entrySet()) {
                    entry.getValue().destroy(Poolable.Caller.CLOSE);
                }
                map.clear();
                totalMemorySize = 0;
            }
        }

        @Override
        public boolean isClosed() {
            return closed;
        }

        long getTotalMemorySize() {
            return totalMemorySize;
        }
    }

    @BeforeEach
    public void setUp() {
        pool = new TestKeyedObjectPool(5);
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
    public void testPutExpired() {
        TestPoolable expired = new TestPoolable("expired", 1, 1);
        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
        }
        assertFalse(pool.put("key", expired));
    }

    @Test
    public void testPutToFullPool() {
        for (int i = 0; i < 5; i++) {
            assertTrue(pool.put("key" + i, new TestPoolable("value" + i)));
        }
        assertEquals(5, pool.size());

        TestPoolable extra = new TestPoolable("extra");
        assertFalse(pool.put("key5", extra));
        assertEquals(5, pool.size());
    }

    @Test
    public void testPutReplace() {
        TestPoolable poolable1 = new TestPoolable("value1");
        TestPoolable poolable2 = new TestPoolable("value2");

        assertTrue(pool.put("key1", poolable1));
        assertEquals(1, pool.size());

        assertTrue(pool.put("key1", poolable2));
        assertEquals(1, pool.size());

        assertTrue(poolable1.isDestroyed());
        assertEquals(Poolable.Caller.REMOVE_REPLACE_CLEAR, poolable1.getDestroyedByCaller());

        assertEquals(poolable2, pool.get("key1"));
    }

    @Test
    public void testPutToClosedPool() {
        pool.close();
        assertThrows(IllegalStateException.class, () -> pool.put("key", new TestPoolable("value")));
    }

    @Test
    public void testPutWithAutoDestroy() {
        for (int i = 0; i < 5; i++) {
            assertTrue(pool.put("key" + i, new TestPoolable("value" + i)));
        }

        TestPoolable extra = new TestPoolable("extra");
        assertFalse(pool.put("key5", extra, true));
        assertTrue(extra.isDestroyed());
        assertEquals(Poolable.Caller.PUT_ADD_FAILURE, extra.getDestroyedByCaller());
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

        TestPoolable retrieved = pool.get("key");
        assertNull(retrieved);
        assertTrue(expired.isDestroyed());
        assertEquals(Poolable.Caller.EVICT, expired.getDestroyedByCaller());
        assertFalse(pool.containsKey("key"));
    }

    @Test
    public void testGetFromClosedPool() {
        pool.close();
        assertThrows(IllegalStateException.class, () -> pool.get("key"));
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
    public void testRemoveFromClosedPool() {
        pool.close();
        assertThrows(IllegalStateException.class, () -> pool.remove("key"));
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

        TestPoolable peeked = pool.peek("key");
        assertNull(peeked);
        assertTrue(expired.isDestroyed());
        assertEquals(Poolable.Caller.EVICT, expired.getDestroyedByCaller());
    }

    @Test
    public void testPeekFromClosedPool() {
        pool.close();
        assertThrows(IllegalStateException.class, () -> pool.peek("key"));
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
    public void testKeySetFromClosedPool() {
        pool.close();
        assertThrows(IllegalStateException.class, () -> pool.keySet());
    }

    @Test
    public void testValues() {
        TestPoolable poolable1 = new TestPoolable("value1");
        TestPoolable poolable2 = new TestPoolable("value2");

        pool.put("key1", poolable1);
        pool.put("key2", poolable2);

        Collection<TestPoolable> values = pool.values();
        assertEquals(2, values.size());
        assertTrue(values.contains(poolable1));
        assertTrue(values.contains(poolable2));

        values.clear();
        assertEquals(2, pool.size());
    }

    @Test
    public void testValuesFromClosedPool() {
        pool.close();
        assertThrows(IllegalStateException.class, () -> pool.values());
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
    public void testContainsKeyFromClosedPool() {
        pool.close();
        assertThrows(IllegalStateException.class, () -> pool.containsKey("key"));
    }

    @Test
    public void testMemoryMeasure() {
        KeyedObjectPool.MemoryMeasure<String, TestPoolable> measure = (key, value) -> key.length() + 100;

        TestKeyedObjectPool memPool = new TestKeyedObjectPool(5, measure);

        memPool.put("k1", new TestPoolable("value1"));
        assertEquals(102, memPool.getTotalMemorySize());

        memPool.put("key2", new TestPoolable("value2"));
        assertEquals(206, memPool.getTotalMemorySize());

        memPool.put("k1", new TestPoolable("value3"));
        assertEquals(206, memPool.getTotalMemorySize());

        memPool.remove("key2");
        assertEquals(102, memPool.getTotalMemorySize());

        memPool.clear();
        assertEquals(0, memPool.getTotalMemorySize());
    }

    @Test
    public void testMemoryMeasureInterface() {
        KeyedObjectPool.MemoryMeasure<Integer, String> measure = (key, value) -> 4 + value.length() * 2;

        assertEquals(14, measure.sizeOf(1, "hello"));
        assertEquals(4, measure.sizeOf(100, ""));
    }

    @Test
    public void testVacate() {
        for (int i = 0; i < 5; i++) {
            pool.put("key" + i, new TestPoolable("value" + i));
        }
        assertEquals(5, pool.size());

        pool.vacate();
        assertTrue(pool.size() < 5);
        assertTrue(pool.size() >= 0);
    }

    @Test
    public void testClear() {
        TestPoolable poolable1 = new TestPoolable("value1");
        TestPoolable poolable2 = new TestPoolable("value2");

        pool.put("key1", poolable1);
        pool.put("key2", poolable2);
        assertEquals(2, pool.size());

        pool.clear();
        assertEquals(0, pool.size());
        assertTrue(pool.isEmpty());

        assertTrue(poolable1.isDestroyed());
        assertTrue(poolable2.isDestroyed());
        assertEquals(Poolable.Caller.REMOVE_REPLACE_CLEAR, poolable1.getDestroyedByCaller());
        assertEquals(Poolable.Caller.REMOVE_REPLACE_CLEAR, poolable2.getDestroyedByCaller());
    }

    @Test
    public void testClose() {
        TestPoolable poolable1 = new TestPoolable("value1");
        TestPoolable poolable2 = new TestPoolable("value2");

        pool.put("key1", poolable1);
        pool.put("key2", poolable2);

        assertFalse(pool.isClosed());
        pool.close();
        assertTrue(pool.isClosed());

        assertTrue(poolable1.isDestroyed());
        assertTrue(poolable2.isDestroyed());
        assertEquals(Poolable.Caller.CLOSE, poolable1.getDestroyedByCaller());
        assertEquals(Poolable.Caller.CLOSE, poolable2.getDestroyedByCaller());
    }
}

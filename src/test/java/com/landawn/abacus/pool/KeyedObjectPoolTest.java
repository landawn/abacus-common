package com.landawn.abacus.pool;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collection;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

public class KeyedObjectPoolTest extends TestBase {

    private KeyedObjectPool<String, TestPoolable> pool;

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
    }

    @BeforeEach
    public void setUp() {
        pool = new GenericKeyedObjectPool<>(5, 0, EvictionPolicy.LAST_ACCESS_TIME, false, 0.2f);
    }

    @AfterEach
    public void tearDown() {
        if (pool != null && !pool.isClosed()) {
            pool.close();
        }
    }

    private void fill(int n) {
        for (int i = 0; i < n; i++) {
            assertTrue(pool.put("key" + i, new TestPoolable("value" + i)));
        }
    }

    @Test
    public void testPut() {
        fill(5);
        assertEquals(5, pool.size());
        TestPoolable extra = new TestPoolable("extra");
        assertFalse(pool.put("key5", extra));
        assertFalse(pool.put("key5", extra, false));
        assertFalse(extra.destroyed);
        TestPoolable destroy = new TestPoolable("destroy");
        assertFalse(pool.put("key5", destroy, true));
        assertTrue(destroy.destroyed);
        assertEquals(Poolable.Caller.PUT_ADD_FAILURE, destroy.destroyedByCaller);

        TestPoolable original = new TestPoolable("value1");
        TestPoolable replacement = new TestPoolable("value2");
        KeyedObjectPool<String, TestPoolable> replacePool = new GenericKeyedObjectPool<>(5, 0, EvictionPolicy.LAST_ACCESS_TIME, false, 0.2f);
        try {
            assertTrue(replacePool.put("key1", original));
            assertTrue(replacePool.put("key1", replacement));
            assertEquals(1, replacePool.size());
            assertTrue(original.destroyed);
            assertEquals(Poolable.Caller.REMOVE_REPLACE_CLEAR, original.destroyedByCaller);
            assertEquals(replacement, replacePool.get("key1"));
        } finally {
            replacePool.close();
        }
    }

    @Test
    public void testPut_EdgeCase() throws InterruptedException {
        assertThrows(IllegalArgumentException.class, () -> pool.put(null, new TestPoolable("value")));
        assertThrows(IllegalArgumentException.class, () -> pool.put("key", null));
        TestPoolable expired = new TestPoolable("expired", 1, 1);
        Thread.sleep(10);
        assertFalse(pool.put("key", expired));
    }

    @Test
    public void testPut_Closed() {
        pool.close();
        assertThrows(IllegalStateException.class, () -> pool.put("key", new TestPoolable("value")));
    }

    @Test
    public void testGet() throws InterruptedException {
        TestPoolable poolable = new TestPoolable("value1");
        pool.put("key1", poolable);
        TestPoolable retrieved = pool.get("key1");
        assertNotNull(retrieved);
        assertEquals("value1", retrieved.value);
        assertEquals(1, retrieved.activityPrint().getAccessCount());
        assertNull(pool.get("nonexistent"));

        TestPoolable expired = new TestPoolable("expired", 50, 50);
        pool.put("expired", expired);
        Thread.sleep(60);
        assertNull(pool.get("expired"));
        assertTrue(expired.destroyed);
        assertEquals(Poolable.Caller.EVICT, expired.destroyedByCaller);
        assertFalse(pool.containsKey("expired"));
    }

    @Test
    public void testGet_Closed() {
        pool.close();
        assertThrows(IllegalStateException.class, () -> pool.get("key"));
    }

    @Test
    public void testRemove() {
        TestPoolable poolable = new TestPoolable("value1");
        pool.put("key1", poolable);
        TestPoolable removed = pool.remove("key1");
        assertNotNull(removed);
        assertEquals("value1", removed.value);
        assertEquals(0, pool.size());
        assertEquals(1, removed.activityPrint().getAccessCount());
        assertNull(pool.remove("nonexistent"));
    }

    @Test
    public void testRemove_Closed() {
        pool.close();
        assertThrows(IllegalStateException.class, () -> pool.remove("key"));
    }

    @Test
    public void testPeek() throws InterruptedException {
        TestPoolable poolable = new TestPoolable("value1");
        pool.put("key1", poolable);
        TestPoolable peeked = pool.peek("key1");
        assertEquals(poolable, peeked);
        assertEquals(0, peeked.activityPrint().getAccessCount());
        assertEquals(1, pool.size());
        assertNull(pool.peek("nonexistent"));

        TestPoolable expired = new TestPoolable("expired", 50, 50);
        pool.put("expired", expired);
        Thread.sleep(60);
        assertNull(pool.peek("expired"));
        assertTrue(expired.destroyed);
        assertEquals(Poolable.Caller.EVICT, expired.destroyedByCaller);
    }

    @Test
    public void testPeek_Closed() {
        pool.close();
        assertThrows(IllegalStateException.class, () -> pool.peek("key"));
    }

    @Test
    public void testKeySet() {
        fill(3);
        Set<String> keys = pool.keySet();
        assertEquals(3, keys.size());
        assertTrue(keys.contains("key1"));
        keys.clear();
        assertEquals(3, pool.size());
        pool.close();
        assertThrows(IllegalStateException.class, () -> pool.keySet());
    }

    @Test
    public void testValues() {
        TestPoolable p1 = new TestPoolable("value1");
        pool.put("key1", p1);
        pool.put("key2", new TestPoolable("value2"));
        Collection<TestPoolable> values = pool.values();
        assertEquals(2, values.size());
        assertTrue(values.contains(p1));
        values.clear();
        assertEquals(2, pool.size());
        pool.close();
        assertThrows(IllegalStateException.class, () -> pool.values());
    }

    @Test
    public void testContainsKey() {
        pool.put("key1", new TestPoolable("value1"));
        assertTrue(pool.containsKey("key1"));
        assertFalse(pool.containsKey("key3"));
        pool.close();
        assertThrows(IllegalStateException.class, () -> pool.containsKey("key"));
    }

    @Test
    public void testMemoryMeasure() {
        KeyedObjectPool.MemoryMeasure<String, TestPoolable> measure = (key, value) -> key.length() + 100;
        KeyedObjectPool<String, TestPoolable> memPool = new GenericKeyedObjectPool<>(5, 0, EvictionPolicy.LAST_ACCESS_TIME, false, 0.2f, 1000, measure);
        try {
            memPool.put("k1", new TestPoolable("value1"));
            assertEquals(102, memPool.stats().dataSize());
            memPool.put("key2", new TestPoolable("value2"));
            assertEquals(206, memPool.stats().dataSize());
            memPool.put("k1", new TestPoolable("value3"));
            assertEquals(206, memPool.stats().dataSize());
            memPool.remove("key2");
            assertEquals(102, memPool.stats().dataSize());
            memPool.clear();
            assertEquals(0, memPool.stats().dataSize());
        } finally {
            memPool.close();
        }
        KeyedObjectPool.MemoryMeasure<Integer, String> iface = (key, value) -> 4 + value.length() * 2;
        assertEquals(14, iface.sizeOf(1, "hello"));
    }

    @Test
    public void testEvict() {
        fill(5);
        pool.evict();
        assertTrue(pool.size() < 5);
        KeyedObjectPool<String, TestPoolable> empty = new GenericKeyedObjectPool<>(5, 0, EvictionPolicy.LAST_ACCESS_TIME, false, 0.2f);
        try {
            empty.evict();
            assertEquals(0, empty.size());
        } finally {
            empty.close();
        }
        pool.close();
        assertThrows(IllegalStateException.class, () -> pool.evict());
    }

    @Test
    public void testClear() {
        TestPoolable p1 = new TestPoolable("value1");
        TestPoolable p2 = new TestPoolable("value2");
        pool.put("key1", p1);
        pool.put("key2", p2);
        pool.clear();
        assertEquals(0, pool.size());
        assertTrue(p1.destroyed);
        assertEquals(Poolable.Caller.REMOVE_REPLACE_CLEAR, p1.destroyedByCaller);
        pool.close();
        assertThrows(IllegalStateException.class, () -> pool.clear());
    }

    @Test
    public void testClose() {
        TestPoolable p1 = new TestPoolable("value1");
        TestPoolable p2 = new TestPoolable("value2");
        pool.put("key1", p1);
        pool.put("key2", p2);
        pool.close();
        assertTrue(pool.isClosed());
        assertTrue(p1.destroyed);
        assertEquals(Poolable.Caller.CLOSE, p1.destroyedByCaller);
    }

    @Test
    public void testPoolBasics() {
        assertTrue(pool.isEmpty());
        pool.put("key1", new TestPoolable("value1"));
        assertFalse(pool.isEmpty());
        pool.remove("key1");
        assertTrue(pool.isEmpty());
        assertEquals(5, pool.capacity());
        assertEquals(5, pool.stats().capacity());
        AbstractPool abstractPool = (AbstractPool) pool;
        abstractPool.lock();
        abstractPool.unlock();
    }

    @Test
    public void testPut_Timeout() throws InterruptedException {
        fill(5);
        assertFalse(pool.put("key5", new TestPoolable("extra"), 1, TimeUnit.MILLISECONDS));
        TestPoolable destroy = new TestPoolable("destroy");
        assertFalse(pool.put("key5", destroy, 1, TimeUnit.MILLISECONDS, true));
        assertTrue(destroy.destroyed);
        assertEquals(Poolable.Caller.PUT_ADD_FAILURE, destroy.destroyedByCaller);
    }
}

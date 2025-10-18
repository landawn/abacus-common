package com.landawn.abacus.pool;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

@Tag("new-test")
public class PoolFactory100Test extends TestBase {

    private static class TestPoolable extends AbstractPoolable {
        TestPoolable() {
            super(10000, 5000);
        }

        @Override
        public void destroy(Poolable.Caller caller) {
        }
    }

    @Test
    public void testCreateObjectPoolWithCapacityOnly() {
        ObjectPool<TestPoolable> pool = PoolFactory.createObjectPool(100);

        assertNotNull(pool);
        assertEquals(100, pool.capacity());
        assertEquals(0, pool.size());
        assertFalse(pool.isClosed());

        TestPoolable poolable = new TestPoolable();
        assertTrue(pool.add(poolable));
        assertEquals(1, pool.size());

        TestPoolable taken = pool.take();
        assertNotNull(taken);
        assertEquals(poolable, taken);
        assertEquals(0, pool.size());

        pool.close();
    }

    @Test
    public void testCreateObjectPoolWithEvictDelay() {
        ObjectPool<TestPoolable> pool = PoolFactory.createObjectPool(50, 5000);

        assertNotNull(pool);
        assertEquals(50, pool.capacity());

        TestPoolable poolable = new TestPoolable();
        assertTrue(pool.add(poolable));
        assertNotNull(pool.take());

        pool.close();
    }

    @Test
    public void testCreateObjectPoolWithEvictionPolicy() {
        ObjectPool<TestPoolable> pool = PoolFactory.createObjectPool(25, 1000, EvictionPolicy.ACCESS_COUNT);

        assertNotNull(pool);
        assertEquals(25, pool.capacity());

        pool.close();
    }

    @Test
    public void testCreateObjectPoolWithMemoryMeasure() {
        ObjectPool.MemoryMeasure<TestPoolable> measure = e -> 100;

        ObjectPool<TestPoolable> pool = PoolFactory.createObjectPool(10, 2000, EvictionPolicy.LAST_ACCESS_TIME, 1024, measure);

        assertNotNull(pool);
        assertEquals(10, pool.capacity());

        pool.close();
    }

    @Test
    public void testCreateObjectPoolWithAutoBalance() {
        ObjectPool<TestPoolable> pool = PoolFactory.createObjectPool(30, 3000, EvictionPolicy.EXPIRATION_TIME, false, 0.3f);

        assertNotNull(pool);
        assertEquals(30, pool.capacity());

        pool.close();
    }

    @Test
    public void testCreateObjectPoolWithFullConfig() {
        ObjectPool.MemoryMeasure<TestPoolable> measure = e -> 200;

        ObjectPool<TestPoolable> pool = PoolFactory.createObjectPool(40, 4000, EvictionPolicy.ACCESS_COUNT, true, 0.4f, 2048, measure);

        assertNotNull(pool);
        assertEquals(40, pool.capacity());

        pool.close();
    }

    @Test
    public void testCreateKeyedObjectPoolWithCapacityOnly() {
        KeyedObjectPool<String, TestPoolable> pool = PoolFactory.createKeyedObjectPool(100);

        assertNotNull(pool);
        assertEquals(100, pool.capacity());
        assertEquals(0, pool.size());
        assertFalse(pool.isClosed());

        TestPoolable poolable = new TestPoolable();
        assertTrue(pool.put("key1", poolable));
        assertEquals(1, pool.size());

        TestPoolable retrieved = pool.get("key1");
        assertNotNull(retrieved);
        assertEquals(poolable, retrieved);

        pool.close();
    }

    @Test
    public void testCreateKeyedObjectPoolWithEvictDelay() {
        KeyedObjectPool<String, TestPoolable> pool = PoolFactory.createKeyedObjectPool(50, 5000);

        assertNotNull(pool);
        assertEquals(50, pool.capacity());

        TestPoolable poolable = new TestPoolable();
        assertTrue(pool.put("key", poolable));
        assertNotNull(pool.get("key"));

        pool.close();
    }

    @Test
    public void testCreateKeyedObjectPoolWithEvictionPolicy() {
        KeyedObjectPool<String, TestPoolable> pool = PoolFactory.createKeyedObjectPool(25, 1000, EvictionPolicy.ACCESS_COUNT);

        assertNotNull(pool);
        assertEquals(25, pool.capacity());

        pool.close();
    }

    @Test
    public void testCreateKeyedObjectPoolWithMemoryMeasure() {
        KeyedObjectPool.MemoryMeasure<String, TestPoolable> measure = (key, e) -> key.length() + 100;

        KeyedObjectPool<String, TestPoolable> pool = PoolFactory.createKeyedObjectPool(10, 2000, EvictionPolicy.LAST_ACCESS_TIME, 1024, measure);

        assertNotNull(pool);
        assertEquals(10, pool.capacity());

        pool.close();
    }

    @Test
    public void testCreateKeyedObjectPoolWithAutoBalance() {
        KeyedObjectPool<String, TestPoolable> pool = PoolFactory.createKeyedObjectPool(30, 3000, EvictionPolicy.EXPIRATION_TIME, false, 0.3f);

        assertNotNull(pool);
        assertEquals(30, pool.capacity());

        pool.close();
    }

    @Test
    public void testCreateKeyedObjectPoolWithFullConfig() {
        KeyedObjectPool.MemoryMeasure<String, TestPoolable> measure = (key, e) -> key.length() + 200;

        KeyedObjectPool<String, TestPoolable> pool = PoolFactory.createKeyedObjectPool(40, 4000, EvictionPolicy.ACCESS_COUNT, true, 0.4f, 2048, measure);

        assertNotNull(pool);
        assertEquals(40, pool.capacity());

        pool.close();
    }

    @Test
    public void testCreateMultiplePools() {
        ObjectPool<TestPoolable> pool1 = PoolFactory.createObjectPool(10);
        ObjectPool<TestPoolable> pool2 = PoolFactory.createObjectPool(20);
        KeyedObjectPool<String, TestPoolable> pool3 = PoolFactory.createKeyedObjectPool(30);

        assertEquals(10, pool1.capacity());
        assertEquals(20, pool2.capacity());
        assertEquals(30, pool3.capacity());

        pool1.add(new TestPoolable());
        assertEquals(1, pool1.size());
        assertEquals(0, pool2.size());
        assertEquals(0, pool3.size());

        pool1.close();
        pool2.close();
        pool3.close();
    }

    @Test
    public void testCreatePoolsWithDifferentEvictionPolicies() {
        ObjectPool<TestPoolable> lastAccessPool = PoolFactory.createObjectPool(10, 1000, EvictionPolicy.LAST_ACCESS_TIME);
        ObjectPool<TestPoolable> accessCountPool = PoolFactory.createObjectPool(10, 1000, EvictionPolicy.ACCESS_COUNT);
        ObjectPool<TestPoolable> expirationPool = PoolFactory.createObjectPool(10, 1000, EvictionPolicy.EXPIRATION_TIME);

        assertNotNull(lastAccessPool);
        assertNotNull(accessCountPool);
        assertNotNull(expirationPool);

        lastAccessPool.close();
        accessCountPool.close();
        expirationPool.close();
    }

    @Test
    public void testCreatePoolWithZeroEvictDelay() {
        ObjectPool<TestPoolable> pool = PoolFactory.createObjectPool(10, 0);
        assertNotNull(pool);

        TestPoolable poolable = new TestPoolable();
        assertTrue(pool.add(poolable));
        assertEquals(1, pool.size());

        pool.close();
    }

    @Test
    public void testCreatePoolWithDefaultEvictDelay() {
        ObjectPool<TestPoolable> pool = PoolFactory.createObjectPool(10);
        assertNotNull(pool);

        assertTrue(pool.add(new TestPoolable()));

        pool.close();
    }

    @Test
    public void testCreatePoolWithNullMemoryMeasure() {
        ObjectPool<TestPoolable> pool = PoolFactory.createObjectPool(10, 1000, EvictionPolicy.LAST_ACCESS_TIME, 0, null);

        assertNotNull(pool);
        assertTrue(pool.add(new TestPoolable()));

        pool.close();
    }

    @Test
    public void testCreateKeyedPoolWithNullMemoryMeasure() {
        KeyedObjectPool<String, TestPoolable> pool = PoolFactory.createKeyedObjectPool(10, 1000, EvictionPolicy.LAST_ACCESS_TIME, 0, null);

        assertNotNull(pool);
        assertTrue(pool.put("key", new TestPoolable()));

        pool.close();
    }
}

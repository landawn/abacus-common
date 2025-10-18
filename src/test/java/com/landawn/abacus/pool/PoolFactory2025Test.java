package com.landawn.abacus.pool;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

@Tag("2025")
public class PoolFactory2025Test extends TestBase {

    private static class TestPoolable extends AbstractPoolable {
        TestPoolable(long liveTime, long maxIdleTime) {
            super(liveTime, maxIdleTime);
        }

        @Override
        public void destroy(Caller caller) {
            // no-op
        }
    }

    @Test
    public void testCreateObjectPoolWithCapacity() {
        ObjectPool<TestPoolable> pool = PoolFactory.createObjectPool(100);

        assertNotNull(pool);
        assertEquals(100, pool.capacity());
        assertTrue(pool instanceof GenericObjectPool);
        pool.close();
    }

    @Test
    public void testCreateObjectPoolWithCapacityAndEvictDelay() {
        ObjectPool<TestPoolable> pool = PoolFactory.createObjectPool(50, 5000);

        assertNotNull(pool);
        assertEquals(50, pool.capacity());
        assertTrue(pool instanceof GenericObjectPool);
        pool.close();
    }

    @Test
    public void testCreateObjectPoolWithCapacityEvictDelayAndPolicy() {
        ObjectPool<TestPoolable> pool = PoolFactory.createObjectPool(75, 3000, EvictionPolicy.ACCESS_COUNT);

        assertNotNull(pool);
        assertEquals(75, pool.capacity());
        assertTrue(pool instanceof GenericObjectPool);
        pool.close();
    }

    @Test
    public void testCreateObjectPoolWithAllEvictionPolicies() {
        for (EvictionPolicy policy : EvictionPolicy.values()) {
            ObjectPool<TestPoolable> pool = PoolFactory.createObjectPool(100, 3000, policy);

            assertNotNull(pool);
            assertEquals(100, pool.capacity());
            pool.close();
        }
    }

    @Test
    public void testCreateObjectPoolWithMemoryConstraints() {
        ObjectPool.MemoryMeasure<TestPoolable> measure = e -> 1024;
        ObjectPool<TestPoolable> pool = PoolFactory.createObjectPool(100, 3000, EvictionPolicy.LAST_ACCESS_TIME, 1024 * 1024, measure);

        assertNotNull(pool);
        assertEquals(100, pool.capacity());
        pool.close();
    }

    @Test
    public void testCreateObjectPoolWithAutoBalance() {
        ObjectPool<TestPoolable> pool = PoolFactory.createObjectPool(100, 3000, EvictionPolicy.LAST_ACCESS_TIME, true, 0.2f);

        assertNotNull(pool);
        assertEquals(100, pool.capacity());
        pool.close();
    }

    @Test
    public void testCreateObjectPoolWithFullConfiguration() {
        ObjectPool.MemoryMeasure<TestPoolable> measure = e -> 512;
        ObjectPool<TestPoolable> pool = PoolFactory.createObjectPool(100, 3000, EvictionPolicy.EXPIRATION_TIME, true, 0.3f, 1024 * 1024, measure);

        assertNotNull(pool);
        assertEquals(100, pool.capacity());
        pool.close();
    }

    @Test
    public void testCreateObjectPoolWithZeroEvictDelay() {
        ObjectPool<TestPoolable> pool = PoolFactory.createObjectPool(50, 0);

        assertNotNull(pool);
        assertEquals(50, pool.capacity());
        pool.close();
    }

    @Test
    public void testCreateObjectPoolWithSmallCapacity() {
        ObjectPool<TestPoolable> pool = PoolFactory.createObjectPool(1);

        assertNotNull(pool);
        assertEquals(1, pool.capacity());
        pool.close();
    }

    @Test
    public void testCreateObjectPoolWithLargeCapacity() {
        ObjectPool<TestPoolable> pool = PoolFactory.createObjectPool(10000);

        assertNotNull(pool);
        assertEquals(10000, pool.capacity());
        pool.close();
    }

    @Test
    public void testCreateKeyedObjectPoolWithCapacity() {
        KeyedObjectPool<String, TestPoolable> pool = PoolFactory.createKeyedObjectPool(100);

        assertNotNull(pool);
        assertEquals(100, pool.capacity());
        assertTrue(pool instanceof GenericKeyedObjectPool);
        pool.close();
    }

    @Test
    public void testCreateKeyedObjectPoolWithCapacityAndEvictDelay() {
        KeyedObjectPool<String, TestPoolable> pool = PoolFactory.createKeyedObjectPool(50, 5000);

        assertNotNull(pool);
        assertEquals(50, pool.capacity());
        assertTrue(pool instanceof GenericKeyedObjectPool);
        pool.close();
    }

    @Test
    public void testCreateKeyedObjectPoolWithCapacityEvictDelayAndPolicy() {
        KeyedObjectPool<String, TestPoolable> pool = PoolFactory.createKeyedObjectPool(75, 3000, EvictionPolicy.ACCESS_COUNT);

        assertNotNull(pool);
        assertEquals(75, pool.capacity());
        assertTrue(pool instanceof GenericKeyedObjectPool);
        pool.close();
    }

    @Test
    public void testCreateKeyedObjectPoolWithAllEvictionPolicies() {
        for (EvictionPolicy policy : EvictionPolicy.values()) {
            KeyedObjectPool<String, TestPoolable> pool = PoolFactory.createKeyedObjectPool(100, 3000, policy);

            assertNotNull(pool);
            assertEquals(100, pool.capacity());
            pool.close();
        }
    }

    @Test
    public void testCreateKeyedObjectPoolWithMemoryConstraints() {
        KeyedObjectPool.MemoryMeasure<String, TestPoolable> measure = (key, e) -> key.length() + 1024;
        KeyedObjectPool<String, TestPoolable> pool = PoolFactory.createKeyedObjectPool(100, 3000, EvictionPolicy.LAST_ACCESS_TIME, 1024 * 1024, measure);

        assertNotNull(pool);
        assertEquals(100, pool.capacity());
        pool.close();
    }

    @Test
    public void testCreateKeyedObjectPoolWithAutoBalance() {
        KeyedObjectPool<String, TestPoolable> pool = PoolFactory.createKeyedObjectPool(100, 3000, EvictionPolicy.LAST_ACCESS_TIME, true, 0.2f);

        assertNotNull(pool);
        assertEquals(100, pool.capacity());
        pool.close();
    }

    @Test
    public void testCreateKeyedObjectPoolWithFullConfiguration() {
        KeyedObjectPool.MemoryMeasure<String, TestPoolable> measure = (key, e) -> 512;
        KeyedObjectPool<String, TestPoolable> pool = PoolFactory.createKeyedObjectPool(100, 3000, EvictionPolicy.EXPIRATION_TIME, true, 0.3f, 1024 * 1024,
                measure);

        assertNotNull(pool);
        assertEquals(100, pool.capacity());
        pool.close();
    }

    @Test
    public void testCreatedObjectPoolIsUsable() {
        ObjectPool<TestPoolable> pool = PoolFactory.createObjectPool(10);

        TestPoolable obj = new TestPoolable(10000, 5000);
        assertTrue(pool.add(obj));

        TestPoolable retrieved = pool.take();
        assertNotNull(retrieved);

        pool.close();
    }

    @Test
    public void testCreatedKeyedObjectPoolIsUsable() {
        KeyedObjectPool<String, TestPoolable> pool = PoolFactory.createKeyedObjectPool(10);

        TestPoolable obj = new TestPoolable(10000, 5000);
        assertTrue(pool.put("key1", obj));

        TestPoolable retrieved = pool.get("key1");
        assertNotNull(retrieved);

        pool.close();
    }

    @Test
    public void testObjectPoolWithDifferentBalanceFactors() {
        float[] balanceFactors = { 0.1f, 0.2f, 0.5f, 0.9f };

        for (float factor : balanceFactors) {
            ObjectPool<TestPoolable> pool = PoolFactory.createObjectPool(100, 3000, EvictionPolicy.LAST_ACCESS_TIME, true, factor);

            assertNotNull(pool);
            pool.close();
        }
    }

    @Test
    public void testKeyedObjectPoolWithDifferentBalanceFactors() {
        float[] balanceFactors = { 0.1f, 0.2f, 0.5f, 0.9f };

        for (float factor : balanceFactors) {
            KeyedObjectPool<String, TestPoolable> pool = PoolFactory.createKeyedObjectPool(100, 3000, EvictionPolicy.LAST_ACCESS_TIME, true, factor);

            assertNotNull(pool);
            pool.close();
        }
    }

    @Test
    public void testObjectPoolWithoutAutoBalance() {
        ObjectPool<TestPoolable> pool = PoolFactory.createObjectPool(10, 3000, EvictionPolicy.LAST_ACCESS_TIME, false, 0.2f);

        assertNotNull(pool);
        assertEquals(10, pool.capacity());
        pool.close();
    }

    @Test
    public void testKeyedObjectPoolWithoutAutoBalance() {
        KeyedObjectPool<String, TestPoolable> pool = PoolFactory.createKeyedObjectPool(10, 3000, EvictionPolicy.LAST_ACCESS_TIME, false, 0.2f);

        assertNotNull(pool);
        assertEquals(10, pool.capacity());
        pool.close();
    }

    @Test
    public void testObjectPoolWithNullMemoryMeasure() {
        ObjectPool<TestPoolable> pool = PoolFactory.createObjectPool(100, 3000, EvictionPolicy.LAST_ACCESS_TIME, 0, null);

        assertNotNull(pool);
        pool.close();
    }

    @Test
    public void testKeyedObjectPoolWithNullMemoryMeasure() {
        KeyedObjectPool<String, TestPoolable> pool = PoolFactory.createKeyedObjectPool(100, 3000, EvictionPolicy.LAST_ACCESS_TIME, 0, null);

        assertNotNull(pool);
        pool.close();
    }

    @Test
    public void testObjectPoolInitiallyEmpty() {
        ObjectPool<TestPoolable> pool = PoolFactory.createObjectPool(100);

        assertTrue(pool.isEmpty());
        assertEquals(0, pool.size());

        pool.close();
    }

    @Test
    public void testKeyedObjectPoolInitiallyEmpty() {
        KeyedObjectPool<String, TestPoolable> pool = PoolFactory.createKeyedObjectPool(100);

        assertTrue(pool.isEmpty());
        assertEquals(0, pool.size());

        pool.close();
    }

    @Test
    public void testObjectPoolNotClosedAfterCreation() {
        ObjectPool<TestPoolable> pool = PoolFactory.createObjectPool(100);

        assertFalse(pool.isClosed());

        pool.close();
        assertTrue(pool.isClosed());
    }

    @Test
    public void testKeyedObjectPoolNotClosedAfterCreation() {
        KeyedObjectPool<String, TestPoolable> pool = PoolFactory.createKeyedObjectPool(100);

        assertFalse(pool.isClosed());

        pool.close();
        assertTrue(pool.isClosed());
    }

    @Test
    public void testMultiplePoolsCanBeCreated() {
        ObjectPool<TestPoolable> pool1 = PoolFactory.createObjectPool(10);
        ObjectPool<TestPoolable> pool2 = PoolFactory.createObjectPool(20);
        ObjectPool<TestPoolable> pool3 = PoolFactory.createObjectPool(30);

        assertNotNull(pool1);
        assertNotNull(pool2);
        assertNotNull(pool3);
        assertEquals(10, pool1.capacity());
        assertEquals(20, pool2.capacity());
        assertEquals(30, pool3.capacity());

        pool1.close();
        pool2.close();
        pool3.close();
    }

    @Test
    public void testObjectPoolWithLargeMaxMemorySize() {
        ObjectPool.MemoryMeasure<TestPoolable> measure = e -> 1024;
        ObjectPool<TestPoolable> pool = PoolFactory.createObjectPool(100, 3000, EvictionPolicy.LAST_ACCESS_TIME, Long.MAX_VALUE / 2, measure);

        assertNotNull(pool);
        pool.close();
    }

    @Test
    public void testKeyedObjectPoolWithLargeMaxMemorySize() {
        KeyedObjectPool.MemoryMeasure<String, TestPoolable> measure = (k, e) -> 1024;
        KeyedObjectPool<String, TestPoolable> pool = PoolFactory.createKeyedObjectPool(100, 3000, EvictionPolicy.LAST_ACCESS_TIME, Long.MAX_VALUE / 2, measure);

        assertNotNull(pool);
        pool.close();
    }
}

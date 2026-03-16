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
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

@Tag("new-test")
public class GenericObjectPoolTest extends TestBase {

    private GenericObjectPool<TestPoolable> pool;

    private static class TestPoolable extends AbstractPoolable {
        private final String id;
        private boolean destroyed = false;
        private Poolable.Caller destroyedByCaller = null;

        TestPoolable(String id) {
            super(10000, 5000);
            this.id = id;
        }

        TestPoolable(String id, long liveTime, long maxIdleTime) {
            super(liveTime, maxIdleTime);
            this.id = id;
        }

        @Override
        public void destroy(Poolable.Caller caller) {
            destroyed = true;
            destroyedByCaller = caller;
        }

        String getId() {
            return id;
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
        pool = new GenericObjectPool<>(10, 0, EvictionPolicy.LAST_ACCESS_TIME);
    }

    @AfterEach
    public void tearDown() {
        if (pool != null && !pool.isClosed()) {
            pool.close();
        }
    }

    @Test
    public void testConstructorBasic() {
        GenericObjectPool<TestPoolable> basicPool = new GenericObjectPool<>(50, 3000, EvictionPolicy.ACCESS_COUNT);
        assertEquals(50, basicPool.capacity());
        assertEquals(0, basicPool.size());
        assertFalse(basicPool.isClosed());
        basicPool.close();
    }

    @Test
    public void testConstructorWithMemory() {
        ObjectPool.MemoryMeasure<TestPoolable> measure = e -> 100;
        GenericObjectPool<TestPoolable> memPool = new GenericObjectPool<>(20, 2000, EvictionPolicy.EXPIRATION_TIME, 1024, measure);
        assertEquals(20, memPool.capacity());
        memPool.close();
    }

    @Test
    public void testConstructorWithAutoBalance() {
        GenericObjectPool<TestPoolable> balancePool = new GenericObjectPool<>(30, 1000, EvictionPolicy.LAST_ACCESS_TIME, false, 0.3f);
        assertEquals(30, balancePool.capacity());
        balancePool.close();
    }

    @Test
    public void testConstructorFullConfig() {
        ObjectPool.MemoryMeasure<TestPoolable> measure = e -> 200;
        GenericObjectPool<TestPoolable> fullPool = new GenericObjectPool<>(40, 4000, EvictionPolicy.ACCESS_COUNT, true, 0.4f, 2048, measure);
        assertEquals(40, fullPool.capacity());
        fullPool.close();
    }

    @Test
    public void testSerializationWithEvictionEnabled() throws Exception {
        GenericObjectPool<TestPoolable> evictPool = new GenericObjectPool<>(10, 100, EvictionPolicy.LAST_ACCESS_TIME);

        GenericObjectPool<TestPoolable> deserialized = deserialize(serialize(evictPool));
        assertNotNull(deserialized);
        assertFalse(deserialized.isClosed());

        evictPool.close();
        deserialized.close();
    }

    @Test
    public void testAdd() {
        TestPoolable poolable = new TestPoolable("test1");
        assertTrue(pool.add(poolable));
        assertEquals(1, pool.size());
        assertTrue(pool.contains(poolable));
    }

    @Test
    public void testAddNull() {
        assertThrows(IllegalArgumentException.class, () -> pool.add(null));
    }

    @Test
    public void testAddExpired() throws InterruptedException {
        TestPoolable expired = new TestPoolable("expired", 10, 10);
        Thread.sleep(20);
        assertFalse(pool.add(expired));
        assertEquals(0, pool.size());
    }

    @Test
    public void testAddToFullPoolWithoutAutoBalance() {
        GenericObjectPool<TestPoolable> noBalancePool = new GenericObjectPool<>(3, 0, EvictionPolicy.LAST_ACCESS_TIME, false, 0.2f);

        for (int i = 0; i < 3; i++) {
            assertTrue(noBalancePool.add(new TestPoolable("test" + i)));
        }

        assertFalse(noBalancePool.add(new TestPoolable("extra")));
        assertEquals(3, noBalancePool.size());

        noBalancePool.close();
    }

    @Test
    public void testAddToFullPoolWithAutoBalance() {
        GenericObjectPool<TestPoolable> balancePool = new GenericObjectPool<>(3, 0, EvictionPolicy.LAST_ACCESS_TIME, true, 0.4f);

        for (int i = 0; i < 3; i++) {
            assertTrue(balancePool.add(new TestPoolable("test" + i)));
        }

        assertTrue(balancePool.add(new TestPoolable("extra")));
        assertEquals(3, balancePool.size());

        balancePool.close();
    }

    @Test
    public void testAddToSmallFullPoolWithDefaultAutoBalanceFactor() {
        GenericObjectPool<TestPoolable> balancePool = new GenericObjectPool<>(1, 0, EvictionPolicy.LAST_ACCESS_TIME, true, 0.2f);
        TestPoolable first = new TestPoolable("first");

        assertTrue(balancePool.add(first));
        assertTrue(balancePool.add(new TestPoolable("second")));
        assertEquals(1, balancePool.size());
        assertTrue(first.isDestroyed());

        balancePool.close();
    }

    @Test
    public void testAddWithMemoryConstraint() {
        ObjectPool.MemoryMeasure<TestPoolable> measure = e -> 100;
        GenericObjectPool<TestPoolable> memPool = new GenericObjectPool<>(10, 0, EvictionPolicy.LAST_ACCESS_TIME, 250, measure);

        assertTrue(memPool.add(new TestPoolable("test1")));
        assertTrue(memPool.add(new TestPoolable("test2")));
        assertTrue(memPool.add(new TestPoolable("test3")));
        assertEquals(2, memPool.size());

        memPool.close();
    }

    @Test
    public void testAddWithMemoryMeasureAndUnlimitedMemory() {
        ObjectPool.MemoryMeasure<TestPoolable> measure = e -> 100;
        GenericObjectPool<TestPoolable> memPool = new GenericObjectPool<>(10, 0, EvictionPolicy.LAST_ACCESS_TIME, 0, measure);

        try {
            for (int i = 0; i < 5; i++) {
                assertTrue(memPool.add(new TestPoolable("test" + i)));
            }

            assertEquals(5, memPool.size());
        } finally {
            memPool.close();
        }
    }

    @Test
    public void testAddWithAutoDestroy() {
        for (int i = 0; i < 10; i++) {
            pool.add(new TestPoolable("test" + i));
        }

        TestPoolable extra = new TestPoolable("extra");
        assertTrue(pool.add(extra, true));
        assertFalse(extra.isDestroyed());
        assertNull(extra.getDestroyedByCaller());
    }

    @Test
    public void testAddWithTimeout() throws InterruptedException {
        for (int i = 0; i < 10; i++) {
            pool.add(new TestPoolable("test" + i));
        }

        long start = System.currentTimeMillis();
        assertTrue(pool.add(new TestPoolable("extra"), 100, TimeUnit.MILLISECONDS));
        long elapsed = System.currentTimeMillis() - start;
        assertTrue(elapsed < 90);
    }

    @Test
    public void testAddWithTimeoutAndSpace() throws InterruptedException {
        for (int i = 0; i < 9; i++) {
            pool.add(new TestPoolable("test" + i));
        }

        assertTrue(pool.add(new TestPoolable("last"), 100, TimeUnit.MILLISECONDS));
        assertEquals(10, pool.size());
    }

    @Test
    public void testAddWithTimeoutAndAutoDestroy() throws InterruptedException {
        for (int i = 0; i < 10; i++) {
            pool.add(new TestPoolable("test" + i));
        }

        TestPoolable extra = new TestPoolable("extra");
        assertTrue(pool.add(extra, 100, TimeUnit.MILLISECONDS, true));
        assertFalse(extra.isDestroyed());
        assertNull(extra.getDestroyedByCaller());
    }

    @Test
    public void testTimedAddFailureDoesNotIncrementPutCount() throws InterruptedException {
        GenericObjectPool<TestPoolable> noBalancePool = new GenericObjectPool<>(1, 0, EvictionPolicy.LAST_ACCESS_TIME, false, 0.2f);

        try {
            assertTrue(noBalancePool.add(new TestPoolable("first")));
            assertFalse(noBalancePool.add(new TestPoolable("second"), 1, TimeUnit.MILLISECONDS));
            assertEquals(1, noBalancePool.stats().putCount());
        } finally {
            noBalancePool.close();
        }
    }

    @Test
    public void testTake() {
        TestPoolable poolable = new TestPoolable("test1");
        pool.add(poolable);

        TestPoolable taken = pool.take();
        assertNotNull(taken);
        assertEquals("test1", taken.getId());
        assertEquals(0, pool.size());
        assertEquals(1, taken.activityPrint().getAccessCount());
    }

    @Test
    public void testTakeFromEmptyPool() {
        assertNull(pool.take());
    }

    @Test
    public void testTakeExpired() throws InterruptedException {
        TestPoolable expired = new TestPoolable("expired", 50, 50);
        pool.add(expired);

        Thread.sleep(60);

        assertNull(pool.take());
        assertTrue(expired.isDestroyed());
        assertEquals(Poolable.Caller.EVICT, expired.getDestroyedByCaller());
    }

    @Test
    public void testTakeWithTimeout() throws InterruptedException {
        long start = System.currentTimeMillis();
        assertNull(pool.take(100, TimeUnit.MILLISECONDS));
        long elapsed = System.currentTimeMillis() - start;
        assertTrue(elapsed >= 90);
    }

    @Test
    public void testTakeWithTimeoutSuccess() throws InterruptedException {
        CountDownLatch addLatch = new CountDownLatch(1);

        new Thread(() -> {
            try {
                Thread.sleep(50);
                pool.add(new TestPoolable("delayed"));
                addLatch.countDown();
            } catch (InterruptedException e) {
            }
        }).start();

        TestPoolable taken = pool.take(200, TimeUnit.MILLISECONDS);
        assertNotNull(taken);
        assertEquals("delayed", taken.getId());

        addLatch.await();
    }

    @Test
    public void testContains() {
        TestPoolable p1 = new TestPoolable("p1");
        TestPoolable p2 = new TestPoolable("p2");
        TestPoolable p3 = new TestPoolable("p3");

        pool.add(p1);
        pool.add(p2);

        assertTrue(pool.contains(p1));
        assertTrue(pool.contains(p2));
        assertFalse(pool.contains(p3));
    }

    @Test
    public void test_evict() {
        for (int i = 0; i < 10; i++) {
            pool.add(new TestPoolable("test" + i));
        }

        pool.evict();
        assertEquals(8, pool.size());
    }

    @Test
    public void test_evictithCustomBalanceFactor() {
        GenericObjectPool<TestPoolable> customPool = new GenericObjectPool<>(10, 0, EvictionPolicy.LAST_ACCESS_TIME, true, 0.5f);

        for (int i = 0; i < 10; i++) {
            customPool.add(new TestPoolable("test" + i));
        }

        customPool.evict();
        assertEquals(5, customPool.size());

        customPool.close();
    }

    @Test
    public void test_evictWithEvictionPolicy() throws InterruptedException {
        TestPoolable p1 = new TestPoolable("p1");
        TestPoolable p2 = new TestPoolable("p2");
        TestPoolable p3 = new TestPoolable("p3");
        TestPoolable p4 = new TestPoolable("p4");
        TestPoolable p5 = new TestPoolable("p5");

        pool.add(p1);
        Thread.sleep(10);
        pool.add(p2);
        Thread.sleep(10);
        pool.add(p3);
        Thread.sleep(10);
        pool.add(p4);
        Thread.sleep(10);
        pool.add(p5);

        pool.take();
        pool.add(p1);

        pool.evict();

        assertTrue(pool.contains(p1));
        assertTrue(pool.contains(p3));
        assertEquals(4, pool.size());
    }

    @Test
    public void testClear() {
        List<TestPoolable> poolables = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            TestPoolable p = new TestPoolable("test" + i);
            poolables.add(p);
            pool.add(p);
        }

        pool.clear();
        assertEquals(0, pool.size());

        for (TestPoolable p : poolables) {
            assertTrue(p.isDestroyed());
            assertEquals(Poolable.Caller.REMOVE_REPLACE_CLEAR, p.getDestroyedByCaller());
        }
    }

    @Test
    public void testClose() {
        TestPoolable p1 = new TestPoolable("p1");
        TestPoolable p2 = new TestPoolable("p2");

        pool.add(p1);
        pool.add(p2);

        assertFalse(pool.isClosed());
        pool.close();
        assertTrue(pool.isClosed());

        assertTrue(p1.isDestroyed());
        assertTrue(p2.isDestroyed());
        assertEquals(Poolable.Caller.CLOSE, p1.getDestroyedByCaller());
        assertEquals(Poolable.Caller.CLOSE, p2.getDestroyedByCaller());

        assertThrows(IllegalStateException.class, () -> pool.add(new TestPoolable("p3")));
        assertThrows(IllegalStateException.class, () -> pool.take());
    }

    @Test
    public void testEviction() throws InterruptedException {
        GenericObjectPool<TestPoolable> evictPool = new GenericObjectPool<>(10, 100, EvictionPolicy.LAST_ACCESS_TIME);

        TestPoolable shortLived = new TestPoolable("short", 50, 50);
        TestPoolable longLived = new TestPoolable("long", 10000, 10000);

        evictPool.add(shortLived);
        evictPool.add(longLived);

        assertEquals(2, evictPool.size());

        Thread.sleep(300);

        assertEquals(1, evictPool.size());
        assertTrue(evictPool.contains(longLived));
        assertFalse(evictPool.contains(shortLived));
        assertTrue(shortLived.isDestroyed());
        assertEquals(Poolable.Caller.EVICT, shortLived.getDestroyedByCaller());

        evictPool.close();
    }

    @Test
    public void testSize() {
        assertEquals(0, pool.size());

        pool.add(new TestPoolable("p1"));
        assertEquals(1, pool.size());

        pool.add(new TestPoolable("p2"));
        assertEquals(2, pool.size());

        pool.take();
        assertEquals(1, pool.size());

        pool.take();
        assertEquals(0, pool.size());
    }

    @Test
    public void testSizeOnClosedPool() {
        pool.add(new TestPoolable("p1"));
        pool.close();
        assertThrows(IllegalStateException.class, () -> pool.size());
    }

    @Test
    public void testHashCode() {
        pool.add(new TestPoolable("p1"));
        pool.add(new TestPoolable("p2"));

        int hash1 = pool.hashCode();
        int hash2 = pool.hashCode();
        assertEquals(hash1, hash2);
    }

    @Test
    public void testEquals() {
        GenericObjectPool<TestPoolable> pool1 = new GenericObjectPool<>(10, 0, EvictionPolicy.LAST_ACCESS_TIME);
        GenericObjectPool<TestPoolable> pool2 = new GenericObjectPool<>(10, 0, EvictionPolicy.LAST_ACCESS_TIME);

        assertTrue(pool1.equals(pool1));
        assertFalse(pool1.equals(pool2));

        TestPoolable p = new TestPoolable("p1");
        pool1.add(p);
        assertFalse(pool1.equals(pool2));

        pool2.add(p);
        assertFalse(pool1.equals(pool2));

        assertFalse(pool1.equals(null));
        assertFalse(pool1.equals("not a pool"));

        pool1.close();
        pool2.close();
    }

    @Test
    public void testToString() {
        pool.add(new TestPoolable("p1"));
        String str = pool.toString();
        assertNotNull(str);
        assertTrue(str.contains("GenericObjectPool"));
    }

    @Test
    public void testMemoryTracking() {
        ObjectPool.MemoryMeasure<TestPoolable> measure = e -> 100;
        GenericObjectPool<TestPoolable> memPool = new GenericObjectPool<>(10, 0, EvictionPolicy.LAST_ACCESS_TIME, 1000, measure);

        PoolStats stats = memPool.stats();
        assertEquals(1000, stats.maxMemory());
        assertEquals(0, stats.dataSize());

        memPool.add(new TestPoolable("p1"));
        stats = memPool.stats();
        assertEquals(100, stats.dataSize());

        memPool.add(new TestPoolable("p2"));
        stats = memPool.stats();
        assertEquals(200, stats.dataSize());

        memPool.take();
        stats = memPool.stats();
        assertEquals(100, stats.dataSize());

        memPool.close();
    }

    @Test
    public void testConcurrentOperations() throws InterruptedException {
        int threads = 10;
        int opsPerThread = 100;
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(threads);
        AtomicInteger addCount = new AtomicInteger(0);
        AtomicInteger takeCount = new AtomicInteger(0);

        for (int i = 0; i < threads; i++) {
            final int threadId = i;
            new Thread(() -> {
                try {
                    startLatch.await();
                    for (int j = 0; j < opsPerThread; j++) {
                        if (j % 2 == 0) {
                            if (pool.add(new TestPoolable("t" + threadId + "-" + j))) {
                                addCount.incrementAndGet();
                            }
                        } else {
                            if (pool.take() != null) {
                                takeCount.incrementAndGet();
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

        assertEquals(addCount.get() - takeCount.get(), pool.size());
    }

    @Test
    public void testLIFOBehavior() {
        TestPoolable p1 = new TestPoolable("p1");
        TestPoolable p2 = new TestPoolable("p2");
        TestPoolable p3 = new TestPoolable("p3");

        pool.add(p1);
        pool.add(p2);
        pool.add(p3);

        assertEquals(p3, pool.take());
        assertEquals(p2, pool.take());
        assertEquals(p1, pool.take());
    }

    @Test
    public void testStats() {
        pool.add(new TestPoolable("p1"));
        pool.add(new TestPoolable("p2"));
        pool.take();
        pool.take();
        pool.take();

        PoolStats stats = pool.stats();
        assertEquals(10, stats.capacity());
        assertEquals(0, stats.size());
        assertEquals(2, stats.putCount());
        assertEquals(3, stats.getCount());
        assertEquals(2, stats.hitCount());
        assertEquals(1, stats.missCount());
    }

    // Additional tests for missing coverage

    @Test
    public void testIsEmpty() {
        assertTrue(pool.isEmpty());
        pool.add(new TestPoolable("p1"));
        assertFalse(pool.isEmpty());
        pool.take();
        assertTrue(pool.isEmpty());
    }

    @Test
    public void testCapacity() {
        assertEquals(10, pool.capacity());
    }

    @Test
    public void testAddWithAutoDestroyFailure() {
        GenericObjectPool<TestPoolable> noBalancePool = new GenericObjectPool<>(2, 0, EvictionPolicy.LAST_ACCESS_TIME, false, 0.2f);
        try {
            noBalancePool.add(new TestPoolable("p1"));
            noBalancePool.add(new TestPoolable("p2"));

            TestPoolable extra = new TestPoolable("extra");
            assertFalse(noBalancePool.add(extra, true));
            assertTrue(extra.isDestroyed());
            assertEquals(Poolable.Caller.PUT_ADD_FAILURE, extra.getDestroyedByCaller());
        } finally {
            noBalancePool.close();
        }
    }

    @Test
    public void testAddWithTimeoutNoAutoBalanceTimeout() throws InterruptedException {
        GenericObjectPool<TestPoolable> noBalancePool = new GenericObjectPool<>(2, 0, EvictionPolicy.LAST_ACCESS_TIME, false, 0.2f);
        try {
            noBalancePool.add(new TestPoolable("p1"));
            noBalancePool.add(new TestPoolable("p2"));

            long start = System.currentTimeMillis();
            assertFalse(noBalancePool.add(new TestPoolable("extra"), 100, TimeUnit.MILLISECONDS));
            long elapsed = System.currentTimeMillis() - start;
            assertTrue(elapsed >= 90);
        } finally {
            noBalancePool.close();
        }
    }

    @Test
    public void testAddWithTimeoutAndAutoDestroyFailure() throws InterruptedException {
        GenericObjectPool<TestPoolable> noBalancePool = new GenericObjectPool<>(2, 0, EvictionPolicy.LAST_ACCESS_TIME, false, 0.2f);
        try {
            noBalancePool.add(new TestPoolable("p1"));
            noBalancePool.add(new TestPoolable("p2"));

            TestPoolable extra = new TestPoolable("extra");
            assertFalse(noBalancePool.add(extra, 50, TimeUnit.MILLISECONDS, true));
            assertTrue(extra.isDestroyed());
            assertEquals(Poolable.Caller.PUT_ADD_FAILURE, extra.getDestroyedByCaller());
        } finally {
            noBalancePool.close();
        }
    }

    @Test
    public void testAddNullWithTimeout() {
        assertThrows(IllegalArgumentException.class, () -> pool.add(null, 100, TimeUnit.MILLISECONDS));
    }

    @Test
    public void testAddExpiredWithTimeout() throws InterruptedException {
        TestPoolable expired = new TestPoolable("expired", 10, 10);
        Thread.sleep(20);
        assertFalse(pool.add(expired, 100, TimeUnit.MILLISECONDS));
    }

    @Test
    public void testContainsOnClosedPool() {
        pool.add(new TestPoolable("p1"));
        pool.close();
        assertThrows(IllegalStateException.class, () -> pool.contains(new TestPoolable("p1")));
    }

    @Test
    public void testEvictOnClosedPool() {
        pool.close();
        assertThrows(IllegalStateException.class, () -> pool.evict());
    }

    @Test
    public void testClearOnClosedPool() {
        pool.close();
        assertThrows(IllegalStateException.class, () -> pool.clear());
    }

    @Test
    public void testEvictEmptyPool() {
        pool.evict();
        assertEquals(0, pool.size());
    }

    @Test
    public void testCloseIdempotent() {
        pool.add(new TestPoolable("p1"));
        pool.close();
        assertTrue(pool.isClosed());
        pool.close(); // Should not throw
        assertTrue(pool.isClosed());
    }

    @Test
    public void testTakeExpiredWithTimeout() throws InterruptedException {
        TestPoolable expired = new TestPoolable("expired", 30, 30);
        pool.add(expired);

        Thread.sleep(50);

        long start = System.currentTimeMillis();
        assertNull(pool.take(100, TimeUnit.MILLISECONDS));
        long elapsed = System.currentTimeMillis() - start;
        assertTrue(elapsed >= 90);
        assertTrue(expired.isDestroyed());
    }

    @Test
    public void testMemoryTrackingWithEviction() {
        ObjectPool.MemoryMeasure<TestPoolable> measure = e -> 100;
        GenericObjectPool<TestPoolable> memPool = new GenericObjectPool<>(10, 0, EvictionPolicy.LAST_ACCESS_TIME, true, 0.5f, 1000, measure);

        for (int i = 0; i < 10; i++) {
            memPool.add(new TestPoolable("p" + i));
        }

        PoolStats stats = memPool.stats();
        assertEquals(1000, stats.dataSize());

        memPool.evict();
        stats = memPool.stats();
        assertTrue(stats.dataSize() < 1000);

        memPool.close();
    }

    @Test
    public void testAddWithMemoryConstraintNoAutoBalance() {
        ObjectPool.MemoryMeasure<TestPoolable> measure = e -> 100;
        GenericObjectPool<TestPoolable> memPool = new GenericObjectPool<>(10, 0, EvictionPolicy.LAST_ACCESS_TIME, false, 0.2f, 250, measure);

        assertTrue(memPool.add(new TestPoolable("p1")));
        assertTrue(memPool.add(new TestPoolable("p2")));
        assertFalse(memPool.add(new TestPoolable("p3")));
        assertEquals(2, memPool.size());

        memPool.close();
    }

    @Test
    public void testLockAndUnlock() {
        pool.lock();
        pool.unlock();
        // Should not throw - basic lock/unlock test
        assertEquals(0, pool.size());
    }

    @Test
    public void testEvictWithAccessCountPolicy() throws InterruptedException {
        GenericObjectPool<TestPoolable> countPool = new GenericObjectPool<>(10, 0, EvictionPolicy.ACCESS_COUNT, true, 0.5f);

        for (int i = 0; i < 10; i++) {
            countPool.add(new TestPoolable("p" + i));
        }

        // Access some objects multiple times to increase their access count
        for (int i = 0; i < 5; i++) {
            TestPoolable taken = countPool.take();
            if (taken != null) {
                countPool.add(taken);
            }
        }

        countPool.evict();
        assertTrue(countPool.size() < 10);

        countPool.close();
    }

    @Test
    public void testEvictWithExpirationTimePolicy() {
        GenericObjectPool<TestPoolable> expPool = new GenericObjectPool<>(10, 0, EvictionPolicy.EXPIRATION_TIME, true, 0.5f);

        for (int i = 0; i < 10; i++) {
            expPool.add(new TestPoolable("p" + i));
        }

        expPool.evict();
        assertEquals(5, expPool.size());

        expPool.close();
    }

    // --- Additional tests for missing coverage ---

    @Test
    public void testAddOnClosedPool() {
        pool.close();
        assertThrows(IllegalStateException.class, () -> pool.add(new TestPoolable("p1")));
    }

    @Test
    public void testTakeOnClosedPool() {
        pool.close();
        assertThrows(IllegalStateException.class, () -> pool.take());
    }

    @Test
    public void testAddWithTimeoutOnClosedPool() {
        pool.close();
        assertThrows(IllegalStateException.class, () -> pool.add(new TestPoolable("p1"), 100, TimeUnit.MILLISECONDS));
    }

    @Test
    public void testTakeWithTimeoutOnClosedPool() {
        pool.close();
        assertThrows(IllegalStateException.class, () -> pool.take(100, TimeUnit.MILLISECONDS));
    }

    @Test
    public void testEvictMoreThanSize() {
        // When evict count >= pool size, entire pool should be cleared
        GenericObjectPool<TestPoolable> smallPool = new GenericObjectPool<>(5, 0, EvictionPolicy.LAST_ACCESS_TIME, true, 1.0f);
        for (int i = 0; i < 3; i++) {
            smallPool.add(new TestPoolable("p" + i));
        }
        // balanceFactor=1.0 means evict all
        smallPool.evict();
        assertEquals(0, smallPool.size());
        smallPool.close();
    }

    @Test
    public void testNegativeMemoryMeasure() {
        ObjectPool.MemoryMeasure<TestPoolable> measure = e -> -1;
        GenericObjectPool<TestPoolable> memPool = new GenericObjectPool<>(10, 0, EvictionPolicy.LAST_ACCESS_TIME, 1000, measure);
        // Negative memory should cause add to fail
        assertFalse(memPool.add(new TestPoolable("p1")));
        assertEquals(0, memPool.size());
        memPool.close();
    }

    @Test
    public void testToStringContent() {
        String str = pool.toString();
        assertTrue(str.contains("GenericObjectPool"));
        assertTrue(str.contains("capacity=10"));
        assertTrue(str.contains("LAST_ACCESS_TIME"));
    }

    @Test
    public void testMultipleAddAndTake() {
        for (int i = 0; i < 5; i++) {
            pool.add(new TestPoolable("p" + i));
        }
        assertEquals(5, pool.size());

        for (int i = 0; i < 5; i++) {
            assertNotNull(pool.take());
        }
        assertEquals(0, pool.size());
        assertNull(pool.take());
    }

    @Test
    public void testAddWithAutoDestroyFalse() {
        GenericObjectPool<TestPoolable> noBalancePool = new GenericObjectPool<>(2, 0, EvictionPolicy.LAST_ACCESS_TIME, false, 0.2f);
        try {
            noBalancePool.add(new TestPoolable("p1"));
            noBalancePool.add(new TestPoolable("p2"));

            TestPoolable extra = new TestPoolable("extra");
            assertFalse(noBalancePool.add(extra, false));
            assertFalse(extra.isDestroyed());
        } finally {
            noBalancePool.close();
        }
    }

    @Test
    public void testClearEmptyPool() {
        pool.clear();
        assertEquals(0, pool.size());
        assertTrue(pool.isEmpty());
    }

    @Test
    public void testPutCountIncrementsOnAdd() {
        pool.add(new TestPoolable("p1"));
        pool.add(new TestPoolable("p2"));
        pool.add(new TestPoolable("p3"));
        assertEquals(3, pool.stats().putCount());
    }

    @Test
    public void testHitAndMissCountsOnTake() {
        pool.add(new TestPoolable("p1"));

        pool.take(); // hit
        pool.take(); // miss (pool empty)

        PoolStats stats = pool.stats();
        assertEquals(1, stats.hitCount());
        assertEquals(1, stats.missCount());
    }

    @Test
    public void testAddWithTimeout_NegativeMemoryMeasure() throws InterruptedException {
        ObjectPool.MemoryMeasure<TestPoolable> measure = e -> -1;
        GenericObjectPool<TestPoolable> memPool = new GenericObjectPool<>(2, 0, EvictionPolicy.LAST_ACCESS_TIME, 1000, measure);

        try {
            assertFalse(memPool.add(new TestPoolable("bad"), 20, TimeUnit.MILLISECONDS));
            assertEquals(0, memPool.size());
        } finally {
            memPool.close();
        }
    }

    @Test
    public void testTakeWithTimeout_SkipsExpiredEntryAndReturnsNext() throws InterruptedException {
        TestPoolable retained = new TestPoolable("retained");
        TestPoolable expired = new TestPoolable("expired", 10, 10);

        pool.add(retained);
        pool.add(expired);
        Thread.sleep(20);

        TestPoolable result = pool.take(50, TimeUnit.MILLISECONDS);

        assertNotNull(result);
        assertEquals("retained", result.getId());
        assertTrue(expired.isDestroyed());
        assertEquals(Poolable.Caller.EVICT, expired.getDestroyedByCaller());
    }

    @Test
    public void testEvict_RemovesRequestedNumberOfEntries() {
        TestPoolable p1 = new TestPoolable("p1");
        TestPoolable p2 = new TestPoolable("p2");
        TestPoolable p3 = new TestPoolable("p3");

        pool.add(p1);
        pool.add(p2);
        pool.add(p3);

        pool.evict(2);

        int destroyedCount = (p1.isDestroyed() ? 1 : 0) + (p2.isDestroyed() ? 1 : 0) + (p3.isDestroyed() ? 1 : 0);
        assertEquals(1, pool.size());
        assertEquals(2, destroyedCount);
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
}

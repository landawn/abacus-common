package com.landawn.abacus.pool;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
public class GenericObjectPool100Test extends TestBase {

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
    public void testAddWithMemoryConstraint() {
        ObjectPool.MemoryMeasure<TestPoolable> measure = e -> 100;
        GenericObjectPool<TestPoolable> memPool = new GenericObjectPool<>(10, 0, EvictionPolicy.LAST_ACCESS_TIME, 250, measure);

        assertTrue(memPool.add(new TestPoolable("test1")));
        assertTrue(memPool.add(new TestPoolable("test2")));
        assertFalse(memPool.add(new TestPoolable("test3")));

        memPool.close();
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
    public void testVacate() {
        for (int i = 0; i < 10; i++) {
            pool.add(new TestPoolable("test" + i));
        }

        pool.vacate();
        assertEquals(8, pool.size());
    }

    @Test
    public void testVacateWithCustomBalanceFactor() {
        GenericObjectPool<TestPoolable> customPool = new GenericObjectPool<>(10, 0, EvictionPolicy.LAST_ACCESS_TIME, true, 0.5f);

        for (int i = 0; i < 10; i++) {
            customPool.add(new TestPoolable("test" + i));
        }

        customPool.vacate();
        assertEquals(5, customPool.size());

        customPool.close();
    }

    @Test
    public void testVacateWithEvictionPolicy() throws InterruptedException {
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

        pool.vacate();

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
}

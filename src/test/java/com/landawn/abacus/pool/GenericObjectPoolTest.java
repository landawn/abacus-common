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
import java.util.Date;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Condition;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

public class GenericObjectPoolTest extends TestBase {

    private static final class SpuriousWakeupCondition implements Condition {
        private int awaitCount;

        @Override
        public long awaitNanos(final long nanosTimeout) {
            return ++awaitCount <= 10_000 ? 1 : 0;
        }

        @Override
        public void await() throws InterruptedException {
            throw new UnsupportedOperationException();
        }

        @Override
        public void awaitUninterruptibly() {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean await(final long time, final TimeUnit unit) throws InterruptedException {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean awaitUntil(final Date deadline) throws InterruptedException {
            throw new UnsupportedOperationException();
        }

        @Override
        public void signal() {
            // no-op for this deterministic test condition
        }

        @Override
        public void signalAll() {
            // no-op for this deterministic test condition
        }
    }

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
    public void testAdd() {
        TestPoolable poolable = new TestPoolable("test1");
        assertTrue(pool.add(poolable));
        assertEquals(1, pool.size());
        assertTrue(pool.contains(poolable));
    }

    @Test
    public void testAddClosedPoolRejectsExpiredElementWithIllegalState() throws InterruptedException {
        GenericObjectPool<TestPoolable> closedPool = new GenericObjectPool<>(1, 0, EvictionPolicy.LAST_ACCESS_TIME);
        TestPoolable expired = new TestPoolable("expired", 1, 1);

        Thread.sleep(20);
        closedPool.close();

        assertThrows(IllegalStateException.class, () -> closedPool.add(expired));
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
    public void testAddTimedReturnsFalseWhenMemoryMeasureThrows() throws InterruptedException {
        ObjectPool.MemoryMeasure<TestPoolable> measure = e -> {
            throw new IllegalStateException("boom");
        };

        GenericObjectPool<TestPoolable> memPool = new GenericObjectPool<>(10, 0, EvictionPolicy.LAST_ACCESS_TIME, 250, measure);

        try {
            assertFalse(memPool.add(new TestPoolable("timed"), 1, TimeUnit.MILLISECONDS));
            assertEquals(0, memPool.size());
        } finally {
            memPool.close();
        }
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

    // --- Additional tests for missing coverage ---

    @Test
    public void testAddOnClosedPool() {
        pool.close();
        assertThrows(IllegalStateException.class, () -> pool.add(new TestPoolable("p1")));
    }

    @Test
    public void testAddWithTimeoutOnClosedPool() {
        pool.close();
        assertThrows(IllegalStateException.class, () -> pool.add(new TestPoolable("p1"), 100, TimeUnit.MILLISECONDS));
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
    public void testLIFOBehavior() {
        TestPoolable p1 = new TestPoolable("p1");
        TestPoolable p2 = new TestPoolable("p2");
        TestPoolable p3 = new TestPoolable("p3");

        pool.add(p1);
        pool.add(p2);
        pool.add(p3);

        assertEquals(p3, pool.poll());
        assertEquals(p2, pool.poll());
        assertEquals(p1, pool.poll());
    }

    @Test
    public void testStats() {
        pool.add(new TestPoolable("p1"));
        pool.add(new TestPoolable("p2"));
        pool.poll();
        pool.poll();
        pool.poll();

        PoolStats stats = pool.stats();
        assertEquals(10, stats.capacity());
        assertEquals(0, stats.size());
        assertEquals(2, stats.putCount());
        assertEquals(3, stats.getCount());
        assertEquals(2, stats.hitCount());
        assertEquals(1, stats.missCount());
    }

    @Test
    public void testPoll() {
        TestPoolable poolable = new TestPoolable("test1");
        pool.add(poolable);

        TestPoolable taken = pool.poll();
        assertNotNull(taken);
        assertEquals("test1", taken.getId());
        assertEquals(0, pool.size());
        assertEquals(1, taken.activityPrint().getAccessCount());
    }

    @Test
    public void testPollFromEmptyPool() {
        assertNull(pool.poll());
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

        memPool.poll();
        stats = memPool.stats();
        assertEquals(100, stats.dataSize());

        memPool.close();
    }

    // Additional tests for missing coverage

    @Test
    public void testIsEmpty() {
        assertTrue(pool.isEmpty());
        pool.add(new TestPoolable("p1"));
        assertFalse(pool.isEmpty());
        pool.poll();
        assertTrue(pool.isEmpty());
    }

    @Test
    public void testHitAndMissCountsOnPoll() {
        pool.add(new TestPoolable("p1"));

        pool.poll(); // hit
        pool.poll(); // miss (pool empty)

        PoolStats stats = pool.stats();
        assertEquals(1, stats.hitCount());
        assertEquals(1, stats.missCount());
    }

    @Test
    public void testPollExpired() throws InterruptedException {
        TestPoolable expired = new TestPoolable("expired", 50, 50);
        pool.add(expired);

        Thread.sleep(60);

        assertNull(pool.poll());
        assertTrue(expired.isDestroyed());
        assertEquals(Poolable.Caller.EVICT, expired.getDestroyedByCaller());
    }

    @Test
    public void testPollWithTimeout() throws InterruptedException {
        long start = System.currentTimeMillis();
        assertNull(pool.poll(100, TimeUnit.MILLISECONDS));
        long elapsed = System.currentTimeMillis() - start;
        assertTrue(elapsed >= 90);
    }

    @Test
    public void testPollWithTimeoutSuccess() throws InterruptedException {
        CountDownLatch addLatch = new CountDownLatch(1);

        new Thread(() -> {
            try {
                Thread.sleep(50);
                pool.add(new TestPoolable("delayed"));
                addLatch.countDown();
            } catch (InterruptedException e) {
            }
        }).start();

        TestPoolable taken = pool.poll(200, TimeUnit.MILLISECONDS);
        assertNotNull(taken);
        assertEquals("delayed", taken.getId());

        addLatch.await();
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
                            if (pool.poll() != null) {
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
    public void testPollExpiredWithTimeout() throws InterruptedException {
        TestPoolable expired = new TestPoolable("expired", 30, 30);
        pool.add(expired);

        Thread.sleep(50);

        long start = System.currentTimeMillis();
        assertNull(pool.poll(100, TimeUnit.MILLISECONDS));
        long elapsed = System.currentTimeMillis() - start;
        assertTrue(elapsed >= 90);
        assertTrue(expired.isDestroyed());
    }

    @Test
    public void testPollOnClosedPool() {
        pool.close();
        assertThrows(IllegalStateException.class, () -> pool.poll());
    }

    @Test
    public void testPollWithTimeoutOnClosedPool() {
        pool.close();
        assertThrows(IllegalStateException.class, () -> pool.poll(100, TimeUnit.MILLISECONDS));
    }

    @Test
    public void testPollWithTimeout_SkipsExpiredEntryAndReturnsNext() throws InterruptedException {
        TestPoolable retained = new TestPoolable("retained");
        TestPoolable expired = new TestPoolable("expired", 10, 10);

        pool.add(retained);
        pool.add(expired);
        Thread.sleep(20);

        TestPoolable result = pool.poll(50, TimeUnit.MILLISECONDS);

        assertNotNull(result);
        assertEquals("retained", result.getId());
        assertTrue(expired.isDestroyed());
        assertEquals(Poolable.Caller.EVICT, expired.getDestroyedByCaller());
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
    public void testContainsOnClosedPool() {
        pool.add(new TestPoolable("p1"));
        pool.close();
        assertThrows(IllegalStateException.class, () -> pool.contains(new TestPoolable("p1")));
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
    public void testEvictWithExpirationTimePolicy() {
        GenericObjectPool<TestPoolable> expPool = new GenericObjectPool<>(10, 0, EvictionPolicy.EXPIRATION_TIME, true, 0.5f);

        for (int i = 0; i < 10; i++) {
            expPool.add(new TestPoolable("p" + i));
        }

        expPool.evict();
        assertEquals(5, expPool.size());

        expPool.close();
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
    public void testEvict_RemovesRequestedNumberOfEntries() {
        TestPoolable p1 = new TestPoolable("p1");
        TestPoolable p2 = new TestPoolable("p2");
        TestPoolable p3 = new TestPoolable("p3");

        pool.add(p1);
        pool.add(p2);
        pool.add(p3);

        pool.vacate(2);

        int destroyedCount = (p1.isDestroyed() ? 1 : 0) + (p2.isDestroyed() ? 1 : 0) + (p3.isDestroyed() ? 1 : 0);
        assertEquals(1, pool.size());
        assertEquals(2, destroyedCount);
    }

    @Test
    public void testEvictEmptyPool() {
        pool.evict();
        assertEquals(0, pool.size());
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

        pool.poll();
        pool.add(p1);

        pool.evict();

        assertTrue(pool.contains(p1));
        assertTrue(pool.contains(p3));
        assertEquals(4, pool.size());
    }

    @Test
    public void testEvictOnClosedPool() {
        pool.close();
        assertThrows(IllegalStateException.class, () -> pool.evict());
    }

    @Test
    public void testEvictWithAccessCountPolicy() throws InterruptedException {
        GenericObjectPool<TestPoolable> countPool = new GenericObjectPool<>(10, 0, EvictionPolicy.ACCESS_COUNT, true, 0.5f);

        for (int i = 0; i < 10; i++) {
            countPool.add(new TestPoolable("p" + i));
        }

        // Access some objects multiple times to increase their access count
        for (int i = 0; i < 5; i++) {
            TestPoolable taken = countPool.poll();
            if (taken != null) {
                countPool.add(taken);
            }
        }

        countPool.evict();
        assertTrue(countPool.size() < 10);

        countPool.close();
    }

    @Test
    public void testEvictWithCreatedTimePolicy() throws InterruptedException {
        GenericObjectPool<TestPoolable> createdPool = new GenericObjectPool<>(10, 0, EvictionPolicy.CREATED_TIME, true, 0.5f);

        // Distinct creation times (ActivityPrint.createdTime is set at construction).
        TestPoolable p1 = new TestPoolable("p1");
        Thread.sleep(10);
        TestPoolable p2 = new TestPoolable("p2");
        Thread.sleep(10);
        TestPoolable p3 = new TestPoolable("p3");

        createdPool.add(p1);
        createdPool.add(p2);
        createdPool.add(p3);

        // CREATED_TIME evicts the oldest-created first: vacate(2) drops p1 and p2, keeps p3.
        createdPool.vacate(2);

        assertEquals(1, createdPool.size());
        assertTrue(createdPool.contains(p3));
        assertFalse(createdPool.contains(p1));
        assertFalse(createdPool.contains(p2));
        assertTrue(p1.isDestroyed());
        assertTrue(p2.isDestroyed());

        createdPool.close();
    }

    @Test
    public void testEvictWithFifoPolicy() throws InterruptedException {
        GenericObjectPool<TestPoolable> fifoPool = new GenericObjectPool<>(10, 0, EvictionPolicy.FIFO, true, 0.5f);

        TestPoolable p1 = new TestPoolable("p1");
        Thread.sleep(10);
        TestPoolable p2 = new TestPoolable("p2");
        Thread.sleep(10);
        TestPoolable p3 = new TestPoolable("p3");

        fifoPool.add(p1);
        fifoPool.add(p2);
        fifoPool.add(p3);

        // FIFO evicts in insertion (creation) order: the first-added p1 goes first.
        fifoPool.vacate(1);

        assertEquals(2, fifoPool.size());
        assertFalse(fifoPool.contains(p1));
        assertTrue(fifoPool.contains(p2));
        assertTrue(fifoPool.contains(p3));

        fifoPool.close();
    }

    @Test
    public void testFifoEvictsFirstAddedNotOldestCreated() throws InterruptedException {
        GenericObjectPool<TestPoolable> fifoPool = new GenericObjectPool<>(10, 0, EvictionPolicy.FIFO, true, 0.5f);

        TestPoolable older = new TestPoolable("older");
        Thread.sleep(10);
        TestPoolable newer = new TestPoolable("newer");

        fifoPool.add(newer);
        fifoPool.add(older);

        fifoPool.vacate(1);

        assertEquals(1, fifoPool.size());
        assertFalse(fifoPool.contains(newer));
        assertTrue(fifoPool.contains(older));
        assertTrue(newer.isDestroyed());
        assertFalse(older.isDestroyed());

        fifoPool.close();
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
    public void testSerializationWithEvictionEnabledRestoresShutdownHook() throws Exception {
        GenericObjectPool<TestPoolable> evictPool = new GenericObjectPool<>(10, 100, EvictionPolicy.LAST_ACCESS_TIME);

        GenericObjectPool<TestPoolable> deserialized = deserialize(serialize(evictPool));

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
        assertThrows(IllegalStateException.class, () -> pool.poll());
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
    public void testConstructorBasic() {
        GenericObjectPool<TestPoolable> basicPool = new GenericObjectPool<>(50, 3000, EvictionPolicy.ACCESS_COUNT);
        assertEquals(50, basicPool.capacity());
        assertEquals(0, basicPool.size());
        assertFalse(basicPool.isClosed());
        basicPool.close();
    }

    @Test
    public void testSize() {
        assertEquals(0, pool.size());

        pool.add(new TestPoolable("p1"));
        assertEquals(1, pool.size());

        pool.add(new TestPoolable("p2"));
        assertEquals(2, pool.size());

        pool.poll();
        assertEquals(1, pool.size());

        pool.poll();
        assertEquals(0, pool.size());
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
    public void testMultipleAddAndPoll() {
        for (int i = 0; i < 5; i++) {
            pool.add(new TestPoolable("p" + i));
        }
        assertEquals(5, pool.size());

        for (int i = 0; i < 5; i++) {
            assertNotNull(pool.poll());
        }
        assertEquals(0, pool.size());
        assertNull(pool.poll());
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
    public void testSizeOnClosedPool() {
        pool.add(new TestPoolable("p1"));
        pool.close();
        assertThrows(IllegalStateException.class, () -> pool.size());
    }

    @Test
    public void testLockAndUnlock() {
        pool.lock();
        pool.unlock();
        // Should not throw - basic lock/unlock test
        assertEquals(0, pool.size());
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
    public void testHashCodeMatchesEqualPools() {
        GenericObjectPool<TestPoolable> pool1 = new GenericObjectPool<>(10, 0, EvictionPolicy.LAST_ACCESS_TIME);
        GenericObjectPool<TestPoolable> pool2 = new GenericObjectPool<>(10, 0, EvictionPolicy.LAST_ACCESS_TIME);

        try {
            TestPoolable p1 = new TestPoolable("p1");
            TestPoolable p2 = new TestPoolable("p2");

            pool1.add(p1);
            pool1.add(p2);

            pool2.add(p1);
            pool2.add(p2);

            assertEquals(pool1.hashCode(), pool2.hashCode());
        } finally {
            pool1.close();
            pool2.close();
        }
    }

    @Test
    public void testEquals() {
        GenericObjectPool<TestPoolable> pool1 = new GenericObjectPool<>(10, 0, EvictionPolicy.LAST_ACCESS_TIME);
        GenericObjectPool<TestPoolable> pool2 = new GenericObjectPool<>(10, 0, EvictionPolicy.LAST_ACCESS_TIME);

        assertTrue(pool1.equals(pool1));
        assertTrue(pool1.equals(pool2));

        TestPoolable p = new TestPoolable("p1");
        pool1.add(p);
        assertFalse(pool1.equals(pool2));

        pool2.add(p);
        assertTrue(pool1.equals(pool2));

        assertFalse(pool1.equals(null));
        assertFalse(pool1.equals("not a pool"));

        pool1.close();
        pool2.close();
    }

    @Test
    public void testEqualsReturnsFalseForDifferentOrder() {
        GenericObjectPool<TestPoolable> pool1 = new GenericObjectPool<>(10, 0, EvictionPolicy.LAST_ACCESS_TIME);
        GenericObjectPool<TestPoolable> pool2 = new GenericObjectPool<>(10, 0, EvictionPolicy.LAST_ACCESS_TIME);

        try {
            TestPoolable p1 = new TestPoolable("p1");
            TestPoolable p2 = new TestPoolable("p2");

            pool1.add(p1);
            pool1.add(p2);

            pool2.add(p2);
            pool2.add(p1);

            assertFalse(pool1.equals(pool2));
        } finally {
            pool1.close();
            pool2.close();
        }
    }

    @Test
    public void testToStringContent() {
        String str = pool.toString();
        assertTrue(str.contains("GenericObjectPool"));
        assertTrue(str.contains("capacity=10"));
        assertTrue(str.contains("LAST_ACCESS_TIME"));
    }

    @Test
    public void testToString() {
        pool.add(new TestPoolable("p1"));
        String str = pool.toString();
        assertNotNull(str);
        assertTrue(str.contains("GenericObjectPool"));
    }

    @Test
    public void testCapacity() {
        assertEquals(10, pool.capacity());
    }

    @Test
    public void testPutCountIncrementsOnAdd() {
        pool.add(new TestPoolable("p1"));
        pool.add(new TestPoolable("p2"));
        pool.add(new TestPoolable("p3"));
        assertEquals(3, pool.stats().putCount());
    }

    // ===========================================================
    // Bug-fix tests: poll(timeout) spurious missCount (Bug 2)
    // ===========================================================

    /**
     * Bug 2a: Normal timeout on an empty pool must count as exactly one miss.
     */
    @Test
    public void testBug2a_NormalTimeout_ExactlyOneMiss() throws InterruptedException {
        assertNull(pool.poll(20, TimeUnit.MILLISECONDS));

        PoolStats stats = pool.stats();
        assertEquals(0, stats.hitCount(), "No hits on empty pool");
        assertEquals(1, stats.missCount(), "Exactly one miss for timed-out poll");
    }

    /**
     * Bug 2b: Successful poll(timeout) must count as exactly one hit with zero misses.
     */
    @Test
    public void testBug2b_SuccessfulPoll_ExactlyOneHit() throws InterruptedException {
        pool.add(new TestPoolable("p1"));

        assertNotNull(pool.poll(100, TimeUnit.MILLISECONDS));

        PoolStats stats = pool.stats();
        assertEquals(1, stats.hitCount(), "Exactly one hit");
        assertEquals(0, stats.missCount(), "No misses when poll succeeds");
    }

    /**
     * Bug 2d: Pool closed while a thread is blocked in poll(timeout).
     * The resulting IllegalStateException path must NOT be counted as a miss.
     */
    @Test
    public void testBug2d_PoolClosedWhileWaiting_NoSpuriousMiss() throws InterruptedException {
        CountDownLatch takingLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(1);
        AtomicBoolean gotIllegalState = new AtomicBoolean(false);

        Thread taker = new Thread(() -> {
            try {
                takingLatch.countDown();
                pool.poll(10, TimeUnit.SECONDS);
            } catch (IllegalStateException e) {
                gotIllegalState.set(true);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                doneLatch.countDown();
            }
        });

        taker.start();
        assertTrue(takingLatch.await(2, TimeUnit.SECONDS));
        Thread.sleep(50); // let taker reach awaitNanos

        // Close the pool; this signals notEmpty.signalAll() which wakes the taker.
        pool.close();

        assertTrue(doneLatch.await(3, TimeUnit.SECONDS));
        assertTrue(gotIllegalState.get(), "Poller must receive IllegalStateException on close");
        // pool.stats() throws because pool is closed; we verify the exception path was taken
        // which -- after the fix -- does NOT call missCount++.
    }

    /**
     * Bug 2e: Multiple hits and misses accumulate correctly; no off-by-one errors.
     */
    @Test
    public void testBug2e_MultipleHitsAndMisses_CorrectCounts() throws InterruptedException {
        for (int i = 0; i < 3; i++) {
            pool.add(new TestPoolable("p" + i));
        }
        // 3 successful takes -> 3 hits
        for (int i = 0; i < 3; i++) {
            assertNotNull(pool.poll(50, TimeUnit.MILLISECONDS));
        }
        // 2 timed-out polls on empty pool -> 2 misses
        assertNull(pool.poll(20, TimeUnit.MILLISECONDS));
        assertNull(pool.poll(20, TimeUnit.MILLISECONDS));

        PoolStats stats = pool.stats();
        assertEquals(3, stats.hitCount());
        assertEquals(2, stats.missCount());
        assertEquals(5, stats.getCount(), "getCount must equal hitCount + missCount");
    }

    /**
     * Regression: timed poll(timeout) must keep popping expired entries instead of
     * blocking on notEmpty.awaitNanos after each one. Before the fix, the loop
     * would await between expired pops and time out even when valid elements
     * remained behind the expired ones.
     */
    @Test
    public void testPollTimed_skipsExpiredAndReturnsValidImmediately() throws InterruptedException {
        // Already-expired entries: liveTime small enough to be expired by the time we call poll.
        TestPoolable expired1 = new TestPoolable("expired1", 1, 1);
        TestPoolable expired2 = new TestPoolable("expired2", 1, 1);
        TestPoolable valid = new TestPoolable("valid"); // default 10000ms live

        pool.add(valid);
        pool.add(expired1);
        pool.add(expired2);

        // Sleep long enough for the short-lived entries to expire.
        Thread.sleep(20);

        long start = System.nanoTime();
        TestPoolable result = pool.poll(2, TimeUnit.SECONDS);
        long elapsedMillis = (System.nanoTime() - start) / 1_000_000;

        assertNotNull(result, "Should return the valid element");
        assertEquals("valid", result.getId());
        // Should return well under the 2s timeout — the bug fix means we re-check
        // the pool synchronously instead of waiting on notEmpty between expired pops.
        assertTrue(elapsedMillis < 500, "poll(timeout, unit) must not await between expired pops, elapsed=" + elapsedMillis + "ms");

        assertTrue(expired1.isDestroyed() || expired2.isDestroyed(), "expired entries must be destroyed");
    }

    /**
     * Regression: timed poll(timeout) returning null when the pool only contains
     * expired entries should not consume the entire timeout — the expired ones
     * are popped synchronously, then we await briefly for a refill that never comes.
     */
    @Test
    public void testPollTimed_allExpired_returnsNullPromptly() throws InterruptedException {
        for (int i = 0; i < 5; i++) {
            pool.add(new TestPoolable("expired" + i, 1, 1));
        }
        Thread.sleep(20);

        long start = System.nanoTime();
        TestPoolable result = pool.poll(100, TimeUnit.MILLISECONDS);
        long elapsedMillis = (System.nanoTime() - start) / 1_000_000;

        assertNull(result, "Should return null since no valid entry exists");
        // Should be roughly bounded by the timeout (~100ms), not by 5×timeout under the old bug.
        assertTrue(elapsedMillis < 500, "poll(timeout, unit) must not multiply wait time by expired-count, elapsed=" + elapsedMillis + "ms");
    }

    /**
     * Bug: poll() (no timeout) checks isClosed only BEFORE acquiring the lock. A concurrent
     * close() can set isClosed=true and release the lock before invoking removeAll() to drain
     * the pool. A poll() that won the race for the lock between those two steps can pop and
     * return a "live" element from a pool that is already marked closed — and the pre-snapshot
     * taken by removeAll() will not destroy it, leaking the element. The fix is to re-check
     * assertNotClosed() inside the lock (matching what add() and poll(timeout) already do).
     */
    @Test
    public void testPoll_raceWithClose_doesNotReturnElementFromClosedPool() throws Exception {
        final TestPoolable element = new TestPoolable("racing");
        pool.add(element);

        // Manually drive the close() race window by holding the pool's lock from a
        // background thread, then mark isClosed=true while poll() is parked at lock.lock(),
        // then release. This deterministically reproduces the race that close() opens
        // between releasing the lock and calling removeAll().
        final java.util.concurrent.locks.ReentrantLock poolLock = pool.lock;
        final CountDownLatch holderHasLock = new CountDownLatch(1);
        final CountDownLatch takeMayProceed = new CountDownLatch(1);

        Thread holder = new Thread(() -> {
            poolLock.lock();
            try {
                holderHasLock.countDown();
                takeMayProceed.await();
                // Mimic close()'s first phase: set isClosed=true while still holding the lock.
                pool.isClosed = true;
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            } finally {
                poolLock.unlock();
            }
        });
        holder.start();
        holderHasLock.await();

        // poll() will pass its outer assertNotClosed() (still false), then block on lock.lock().
        AtomicBoolean threwISE = new AtomicBoolean(false);
        AtomicBoolean returnedElement = new AtomicBoolean(false);
        Thread taker = new Thread(() -> {
            try {
                TestPoolable t = pool.poll();
                if (t != null) {
                    returnedElement.set(true);
                }
            } catch (IllegalStateException e) {
                threwISE.set(true);
            }
        });
        taker.start();

        // Give taker a moment to reach lock.lock() and park.
        Thread.sleep(50);
        // Now release; holder will set isClosed=true and unlock; taker proceeds with the lock.
        takeMayProceed.countDown();
        holder.join();
        taker.join();

        assertTrue(threwISE.get(), "poll() must throw IllegalStateException when isClosed was set under the lock");
        assertFalse(returnedElement.get(), "poll() must not return an element from a pool already marked closed");
        // Cleanup: pool.isClosed is true; calling close() again is a no-op per idempotency contract.
    }

    /**
     * Regression test for poll() leaking the popped element when memoryMeasure.sizeOf() throws.
     *
     * <p>Before the fix, an unchecked exception thrown by a user-supplied MemoryMeasure during
     * poll() propagated out of the method while the element had already been popped from the
     * internal deque — never returned to the caller, never destroyed, leaked. The fix wraps the
     * sizeOf() call in a try/catch so the popped element is still returned and the caller
     * remains responsible for it.
     */
    @Test
    public void testPollDoesNotLeakElementWhenMemoryMeasureThrows() {
        // Build a measure that returns 0 on add but throws once on poll.
        final AtomicBoolean throwOnNext = new AtomicBoolean(false);
        final ObjectPool.MemoryMeasure<TestPoolable> conditionalMeasure = e -> {
            if (throwOnNext.get()) {
                throw new RuntimeException("simulated sizeOf failure on poll");
            }
            return 0L;
        };

        GenericObjectPool<TestPoolable> p2 = new GenericObjectPool<>(10, 0, EvictionPolicy.LAST_ACCESS_TIME, true, 0.2f, 1024L * 1024L, conditionalMeasure);
        try {
            TestPoolable original = new TestPoolable("memMeasurePoll");
            assertTrue(p2.add(original));
            assertEquals(1, p2.size());

            // Now arm the measure to throw on the next sizeOf invocation (which poll() will make).
            throwOnNext.set(true);

            // Pre-fix: poll() throws RuntimeException and the element is leaked (gone from pool,
            // never returned, never destroyed). Post-fix: poll() catches, returns the element.
            TestPoolable popped = p2.poll();
            assertNotNull(popped, "poll() must return the popped element even if memoryMeasure.sizeOf() throws");
            assertEquals("memMeasurePoll", popped.getId());
            assertFalse(popped.isDestroyed(), "Successfully taken element must not be destroyed");
            assertEquals(0, p2.size());
        } finally {
            p2.close();
        }
    }

    /**
     * Same regression as above, but for poll(timeout, unit).
     */
    @Test
    public void testPollWithTimeoutDoesNotLeakElementWhenMemoryMeasureThrows() throws InterruptedException {
        final AtomicBoolean throwOnNext = new AtomicBoolean(false);
        final ObjectPool.MemoryMeasure<TestPoolable> conditionalMeasure = e -> {
            if (throwOnNext.get()) {
                throw new RuntimeException("simulated sizeOf failure on poll(timeout)");
            }
            return 0L;
        };

        GenericObjectPool<TestPoolable> p = new GenericObjectPool<>(10, 0, EvictionPolicy.LAST_ACCESS_TIME, true, 0.2f, 1024L * 1024L, conditionalMeasure);
        try {
            TestPoolable original = new TestPoolable("memMeasurePollTimeout");
            assertTrue(p.add(original));

            throwOnNext.set(true);

            TestPoolable popped = p.poll(1, TimeUnit.SECONDS);
            assertNotNull(popped, "poll(timeout) must return the popped element even if memoryMeasure.sizeOf() throws");
            assertEquals("memMeasurePollTimeout", popped.getId());
            assertFalse(popped.isDestroyed());
            assertEquals(0, p.size());
        } finally {
            p.close();
        }
    }

    /**
     * Regression: a thread blocked in {@code poll(timeout, unit)} that aborts because the
     * pool is concurrently {@code close()}d (resulting in an IllegalStateException) must NOT
     * be counted as a cache miss. Before the fix the hit/miss accounting lived in the
     * {@code finally} block and ran even when the body unwound exceptionally, recording a
     * spurious miss. {@code missCount} is read directly (package-private field) because
     * {@code stats()} throws on a closed pool.
     */
    @Test
    public void testPollTimed_PoolClosedWhileWaiting_NoSpuriousMissCount() throws InterruptedException {
        final CountDownLatch takingLatch = new CountDownLatch(1);
        final CountDownLatch doneLatch = new CountDownLatch(1);
        final AtomicBoolean gotIllegalState = new AtomicBoolean(false);

        Thread taker = new Thread(() -> {
            try {
                takingLatch.countDown();
                pool.poll(10, TimeUnit.SECONDS);
            } catch (IllegalStateException e) {
                gotIllegalState.set(true);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                doneLatch.countDown();
            }
        });

        taker.start();
        assertTrue(takingLatch.await(2, TimeUnit.SECONDS));
        Thread.sleep(100); // let the taker park in notEmpty.awaitNanos

        pool.close(); // signals notEmpty.signalAll(); taker wakes, assertNotClosed() throws

        assertTrue(doneLatch.await(3, TimeUnit.SECONDS));
        assertTrue(gotIllegalState.get(), "Poller must receive IllegalStateException on close");
        assertEquals(0, pool.missCount.get(), "Closed-pool abort must not be counted as a miss");
        assertEquals(0, pool.hitCount.get(), "Closed-pool abort must not be counted as a hit");
    }

    @Test
    public void testConcurrentCloseWaitsForDestructionCallbacks() throws InterruptedException {
        final GenericObjectPool<TestPoolable> p = new GenericObjectPool<>(1, 0, EvictionPolicy.LAST_ACCESS_TIME);
        final CountDownLatch destroyStarted = new CountDownLatch(1);
        final CountDownLatch allowDestroyToFinish = new CountDownLatch(1);
        final CountDownLatch secondCloseReturned = new CountDownLatch(1);

        final TestPoolable element = new TestPoolable("blocking-destroy") {
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

        assertTrue(p.add(element));

        final Thread firstCloser = new Thread(p::close);
        final Thread secondCloser = new Thread(() -> {
            p.close();
            secondCloseReturned.countDown();
        });

        firstCloser.start();
        assertTrue(destroyStarted.await(2, TimeUnit.SECONDS));

        secondCloser.start();

        try {
            assertFalse(secondCloseReturned.await(200, TimeUnit.MILLISECONDS),
                    "a concurrent close must not return while the first close is still destroying resources");
        } finally {
            allowDestroyToFinish.countDown();
        }

        firstCloser.join(2000);
        secondCloser.join(2000);

        assertFalse(firstCloser.isAlive());
        assertFalse(secondCloser.isAlive());
        assertEquals(0, secondCloseReturned.getCount());
        assertTrue(element.isDestroyed());
    }

    // --- regression tests for 2026-06-10 deep-review fixes ---

    @Test
    public void testAddReChecksExpiryInsideLock() {
        // regression: time spent in sizeOf() inside the lock could expire the element, which was
        // then pushed anyway (the timed add variant already re-checked expiry in the lock)
        GenericObjectPool<TestPoolable> memPool = new GenericObjectPool<>(10, 0, EvictionPolicy.LAST_ACCESS_TIME, 0, e -> {
            try {
                Thread.sleep(50);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
            }
            return 1L;
        });

        TestPoolable shortLived = new TestPoolable("x", 10, 10); // expires while sizeOf sleeps

        assertFalse(memPool.add(shortLived));
        assertEquals(0, memPool.size(), "an element that expired inside the lock must not be pooled");

        memPool.close();
    }

    @Test
    public void testEvictRemovesObjectBeforeDestroyCallbackAndReleasesLock() {
        final GenericObjectPool<TestPoolable> p = new GenericObjectPool<>(1, 0, EvictionPolicy.LAST_ACCESS_TIME);
        final AtomicReference<TestPoolable> observedDuringDestroy = new AtomicReference<>();
        final AtomicReference<Boolean> lockHeldDuringDestroy = new AtomicReference<>();
        final TestPoolable doomed = new TestPoolable("doomed") {
            @Override
            public void destroy(final Poolable.Caller caller) {
                lockHeldDuringDestroy.set(p.lock.isHeldByCurrentThread());
                observedDuringDestroy.set(p.poll());
                super.destroy(caller);
            }
        };

        try {
            assertTrue(p.add(doomed));
            p.evict();

            assertNull(observedDuringDestroy.get(), "a destroy callback must not retrieve an object already selected for eviction");
            assertEquals(Boolean.FALSE, lockHeldDuringDestroy.get(), "explicit eviction must not invoke user callbacks while holding the pool lock");
            assertEquals(0, p.size());
        } finally {
            p.close();
        }
    }

    @Test
    public void testAutoBalanceDestroyCallbackRunsAfterUnlockAndReplacementIsStored() {
        final GenericObjectPool<TestPoolable> p = new GenericObjectPool<>(1, 0, EvictionPolicy.LAST_ACCESS_TIME);
        final AtomicReference<Boolean> lockHeldDuringDestroy = new AtomicReference<>();
        final AtomicReference<Boolean> victimStillPresent = new AtomicReference<>();
        final AtomicReference<Boolean> replacementPresent = new AtomicReference<>();
        final TestPoolable replacement = new TestPoolable("replacement");
        final TestPoolable victim = new TestPoolable("victim") {
            @Override
            public void destroy(final Poolable.Caller caller) {
                lockHeldDuringDestroy.set(p.lock.isHeldByCurrentThread());
                victimStillPresent.set(p.contains(this));
                replacementPresent.set(p.contains(replacement));
                super.destroy(caller);
            }
        };

        try {
            assertTrue(p.add(victim));
            assertTrue(p.add(replacement));

            assertEquals(Boolean.FALSE, lockHeldDuringDestroy.get(), "auto-balance must invoke destroy after releasing the pool lock");
            assertEquals(Boolean.FALSE, victimStillPresent.get(), "the victim must be detached before its destroy callback");
            assertEquals(Boolean.TRUE, replacementPresent.get(), "the successful add must be visible before the victim's destroy callback");
            assertEquals(Poolable.Caller.VACATE, victim.getDestroyedByCaller());
        } finally {
            p.close();
        }
    }

    @Test
    public void testPollExpiredDestroyCallbackRunsAfterUnlockAndDetachment() {
        final GenericObjectPool<TestPoolable> p = new GenericObjectPool<>(1, 0, EvictionPolicy.LAST_ACCESS_TIME);
        final AtomicReference<Boolean> lockHeldDuringDestroy = new AtomicReference<>();
        final AtomicReference<Boolean> victimStillPresent = new AtomicReference<>();
        final TestPoolable expired = new TestPoolable("expired") {
            @Override
            public void destroy(final Poolable.Caller caller) {
                lockHeldDuringDestroy.set(p.lock.isHeldByCurrentThread());
                victimStillPresent.set(p.contains(this));
                super.destroy(caller);
            }
        };

        try {
            assertTrue(p.add(expired));
            expired.activityPrint().setCreatedTime(System.currentTimeMillis() - 20_000);

            assertNull(p.poll());
            assertEquals(Boolean.FALSE, lockHeldDuringDestroy.get(), "poll must destroy an expired object after releasing the pool lock");
            assertEquals(Boolean.FALSE, victimStillPresent.get(), "the expired object must be detached before its destroy callback");
            assertEquals(Poolable.Caller.EVICT, expired.getDestroyedByCaller());
            assertEquals(0, p.size());
        } finally {
            p.close();
        }
    }

    @Test
    public void testRemoveExpiredAccountsBeforeUnlockingForDestroyCallback() throws Exception {
        assertRemovalAccountingVisibleBeforeCallback(false);
    }

    @Test
    public void testClearAccountsBeforeUnlockingForDestroyCallback() throws Exception {
        assertRemovalAccountingVisibleBeforeCallback(true);
    }

    private static void assertRemovalAccountingVisibleBeforeCallback(final boolean clear) throws Exception {
        final CountDownLatch destructionPhaseReached = new CountDownLatch(1);
        final CountDownLatch releaseDestruction = new CountDownLatch(1);
        final AtomicReference<Throwable> cleanupFailure = new AtomicReference<>();
        final AtomicReference<Boolean> lockHeldDuringCallback = new AtomicReference<>();

        class AccountingProbePool extends GenericObjectPool<TestPoolable> {
            AccountingProbePool() {
                super(2, 0, EvictionPolicy.LAST_ACCESS_TIME, false, 0.2f, 10L, value -> 10L);
            }

            @Override
            protected void destroyAll(final Collection<TestPoolable> values, final Poolable.Caller caller) {
                // Legacy removeExpired/removeAll reached this hook after unlocking but before
                // destroy() performed memory accounting. Pausing here makes that stale window
                // deterministic; the fixed paths bypass it because only callbacks remain.
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

        assertTrue(p.add(victim));

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
        }, clear ? "clear-accounting-probe" : "expiry-accounting-probe");

        cleanup.start();

        try {
            assertTrue(destructionPhaseReached.await(2, TimeUnit.SECONDS), "cleanup did not reach its post-unlock destruction phase");
            assertTrue(p.add(new TestPoolable("replacement")), "detached victim memory must be subtracted before another add checks the limit");
            assertEquals(10L, p.totalDataSize.get());
        } finally {
            releaseDestruction.countDown();
            cleanup.join(2_000);

            if (!p.isClosed()) {
                p.close();
            }
        }

        assertFalse(cleanup.isAlive(), "cleanup remained blocked");
        assertNull(cleanupFailure.get(), "cleanup failed: " + cleanupFailure.get());
        assertEquals(Boolean.FALSE, lockHeldDuringCallback.get(), "cleanup callback must run after unlocking");
        assertEquals(0L, p.totalDataSize.get(), "close must account the replacement before completing its callbacks");
    }

    private static void awaitCleanupRelease(final CountDownLatch release) {
        try {
            release.await(5, TimeUnit.SECONDS);
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @Test
    public void testTimedAddHonorsTimeoutAcrossMoreThanTenThousandWakeups() throws Exception {
        final GenericObjectPool<TestPoolable> p = new GenericObjectPool<>(1, 0, EvictionPolicy.LAST_ACCESS_TIME, false, 0.2f);
        assertTrue(p.add(new TestPoolable("existing")));

        final SpuriousWakeupCondition condition = new SpuriousWakeupCondition();
        p.notFull = condition;

        try {
            assertFalse(p.add(new TestPoolable("new"), 1, TimeUnit.MILLISECONDS));
            assertEquals(10_001, condition.awaitCount);
        } finally {
            p.close();
        }
    }

    @Test
    public void testTimedOperationsRejectNullTimeUnit() {
        assertThrows(IllegalArgumentException.class, () -> pool.add(new TestPoolable("value"), 1, null));
        assertThrows(IllegalArgumentException.class, () -> pool.poll(1, null));
    }

    @Test
    public void testCloseContinuesAfterNonFatalDestroyError() {
        final TestPoolable survivor = new TestPoolable("survivor");
        final TestPoolable broken = new TestPoolable("broken") {
            @Override
            public void destroy(final Poolable.Caller caller) {
                throw new AssertionError("simulated callback failure");
            }
        };

        assertTrue(pool.add(survivor));
        assertTrue(pool.add(broken));
        pool.close();

        assertTrue(survivor.isDestroyed(), "one broken callback must not prevent later resources from being destroyed");
    }

    @Test
    public void testEvictSelectsCountAndVictimsInSingleCriticalSection() {
        final AtomicReference<TestPoolable> removedDuringFormerGap = new AtomicReference<>();

        class AtomicEvictPool extends GenericObjectPool<TestPoolable> {
            AtomicEvictPool() {
                super(8, 0, EvictionPolicy.FIFO, true, 0.5f);
            }

            @Override
            protected void vacate(final int numberToEvict) {
                // Legacy evict() computed numberToEvict, unlocked, and then dispatched here.
                // Mutating in that gap made its count stale and removed three of four objects.
                final Thread interloper = new Thread(() -> removedDuringFormerGap.set(poll()), "evict-gap-interloper");
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

        final AtomicEvictPool p = new AtomicEvictPool();

        try {
            assertTrue(p.add(new TestPoolable("one")));
            assertTrue(p.add(new TestPoolable("two")));
            assertTrue(p.add(new TestPoolable("three")));
            assertTrue(p.add(new TestPoolable("four")));

            p.evict();

            assertNull(removedDuringFormerGap.get(), "evict must not expose a mutation gap between counting and detaching victims");
            assertEquals(2, p.size(), "a 0.5 balance factor must atomically remove two of four objects");
        } finally {
            p.close();
        }
    }
}

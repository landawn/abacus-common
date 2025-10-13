package com.landawn.abacus.pool;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import com.landawn.abacus.TestBase;

@Tag("new-test")
public class AbstractPool100Test extends TestBase {

    private TestAbstractPool pool;

    private static class TestAbstractPool extends AbstractPool {
        private int currentSize = 0;

        TestAbstractPool(int capacity, long evictDelay, EvictionPolicy evictionPolicy, boolean autoBalance, float balanceFactor, long maxMemorySize) {
            super(capacity, evictDelay, evictionPolicy, autoBalance, balanceFactor, maxMemorySize);
        }

        @Override
        public int size() {
            return currentSize;
        }

        @Override
        public void vacate() {
            assertNotClosed();
            currentSize = Math.max(0, currentSize - 1);
        }

        @Override
        public void clear() {
            assertNotClosed();
            currentSize = 0;
        }

        @Override
        public void close() {
            if (!isClosed) {
                isClosed = true;
                currentSize = 0;
            }
        }

        public void setSize(int size) {
            this.currentSize = size;
        }

        public void incrementPutCount() {
            putCount.incrementAndGet();
        }

        public void incrementHitCount() {
            hitCount.incrementAndGet();
        }

        public void incrementMissCount() {
            missCount.incrementAndGet();
        }

        public void incrementEvictionCount() {
            evictionCount.incrementAndGet();
        }

        public void setTotalDataSize(long size) {
            totalDataSize.set(size);
        }

        ReentrantLock getLock() {
            return lock;
        }
    }

    @BeforeEach
    public void setUp() {
        pool = new TestAbstractPool(100, 3000, EvictionPolicy.LAST_ACCESS_TIME, true, 0.2f, 0);
    }

    @Test
    public void testConstructor() {
        assertNotNull(pool);
        assertEquals(100, pool.capacity());
        assertEquals(0, pool.size());
        assertFalse(pool.isClosed());
    }

    @Test
    public void testConstructorWithNegativeCapacity() {
        assertThrows(IllegalArgumentException.class, () -> new TestAbstractPool(-1, 3000, EvictionPolicy.LAST_ACCESS_TIME, true, 0.2f, 0));
    }

    @Test
    public void testConstructorWithNegativeEvictDelay() {
        assertThrows(IllegalArgumentException.class, () -> new TestAbstractPool(100, -1, EvictionPolicy.LAST_ACCESS_TIME, true, 0.2f, 0));
    }

    @Test
    public void testConstructorWithNegativeBalanceFactor() {
        assertThrows(IllegalArgumentException.class, () -> new TestAbstractPool(100, 3000, EvictionPolicy.LAST_ACCESS_TIME, true, -0.1f, 0));
    }

    @Test
    public void testConstructorWithNegativeMaxMemorySize() {
        assertThrows(IllegalArgumentException.class, () -> new TestAbstractPool(100, 3000, EvictionPolicy.LAST_ACCESS_TIME, true, 0.2f, -1));
    }

    @Test
    public void testConstructorWithCustomBalanceFactor() {
        TestAbstractPool customPool = new TestAbstractPool(100, 3000, EvictionPolicy.LAST_ACCESS_TIME, true, 0.5f, 0);
        assertEquals(0.5f, customPool.balanceFactor);

        TestAbstractPool defaultPool = new TestAbstractPool(100, 3000, EvictionPolicy.LAST_ACCESS_TIME, true, 0f, 0);
        assertEquals(AbstractPool.DEFAULT_BALANCE_FACTOR, defaultPool.balanceFactor);
    }

    @Test
    public void testConstructorWithNullEvictionPolicy() {
        TestAbstractPool poolWithNull = new TestAbstractPool(100, 3000, null, true, 0.2f, 0);
        assertEquals(EvictionPolicy.LAST_ACCESS_TIME, poolWithNull.evictionPolicy);
    }

    @Test
    public void testLockAndUnlock() throws InterruptedException {
        assertFalse(pool.getLock().isLocked());

        pool.lock();
        assertTrue(pool.getLock().isLocked());
        assertTrue(pool.getLock().isHeldByCurrentThread());

        pool.unlock();
        assertFalse(pool.getLock().isLocked());
    }

    @Test
    public void testUnlockWithoutLock() {
        assertThrows(IllegalMonitorStateException.class, () -> pool.unlock());
    }

    @Test
    public void testLockIsReentrant() {
        pool.lock();
        pool.lock();
        assertTrue(pool.getLock().isLocked());
        assertEquals(2, pool.getLock().getHoldCount());

        pool.unlock();
        assertTrue(pool.getLock().isLocked());
        assertEquals(1, pool.getLock().getHoldCount());

        pool.unlock();
        assertFalse(pool.getLock().isLocked());
    }

    @Test
    public void testConcurrentLock() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(1);

        pool.lock();

        Thread other = new Thread(() -> {
            latch.countDown();
            pool.lock();
            try {
                done.countDown();
            } finally {
                pool.unlock();
            }
        });
        other.start();

        latch.await();
        Thread.sleep(50);
        assertTrue(pool.getLock().hasQueuedThreads());

        pool.unlock();
        assertTrue(done.await(1, TimeUnit.SECONDS));

        other.join();
    }

    @Test
    public void testCapacity() {
        assertEquals(100, pool.capacity());
    }

    @Test
    public void testSize() {
        assertEquals(0, pool.size());
        pool.setSize(50);
        assertEquals(50, pool.size());
    }

    @Test
    public void testIsEmpty() {
        assertTrue(pool.isEmpty());
        pool.setSize(1);
        assertFalse(pool.isEmpty());
        pool.setSize(0);
        assertTrue(pool.isEmpty());
    }

    @Test
    public void testStats() {
        pool.setSize(25);
        pool.incrementPutCount();
        pool.incrementPutCount();
        pool.incrementHitCount();
        pool.incrementMissCount();
        pool.incrementEvictionCount();

        PoolStats stats = pool.stats();
        assertNotNull(stats);
        assertEquals(100, stats.capacity());
        assertEquals(25, stats.size());
        assertEquals(2, stats.putCount());
        assertEquals(2, stats.getCount());
        assertEquals(1, stats.hitCount());
        assertEquals(1, stats.missCount());
        assertEquals(1, stats.evictionCount());
        assertEquals(-1, stats.maxMemory());
        assertEquals(-1, stats.dataSize());
    }

    @Test
    public void testStatsWithMemory() {
        TestAbstractPool memPool = new TestAbstractPool(100, 3000, EvictionPolicy.LAST_ACCESS_TIME, true, 0.2f, 1024 * 1024);
        memPool.setTotalDataSize(512 * 1024);

        PoolStats stats = memPool.stats();
        assertEquals(1024 * 1024, stats.maxMemory());
        assertEquals(512 * 1024, stats.dataSize());
    }

    @Test
    public void testVacate() {
        pool.setSize(10);
        assertEquals(10, pool.size());
        pool.vacate();
        assertEquals(9, pool.size());
    }

    @Test
    public void testVacateOnClosedPool() {
        pool.close();
        assertThrows(IllegalStateException.class, () -> pool.vacate());
    }

    @Test
    public void testClear() {
        pool.setSize(10);
        assertEquals(10, pool.size());
        pool.clear();
        assertEquals(0, pool.size());
    }

    @Test
    public void testClearOnClosedPool() {
        pool.close();
        assertThrows(IllegalStateException.class, () -> pool.clear());
    }

    @Test
    public void testClose() {
        assertFalse(pool.isClosed());
        pool.close();
        assertTrue(pool.isClosed());
        assertEquals(0, pool.size());

        pool.close();
        assertTrue(pool.isClosed());
    }

    @Test
    public void testIsClosed() {
        assertFalse(pool.isClosed());
        pool.close();
        assertTrue(pool.isClosed());
    }

    @Test
    public void testAssertNotClosed() {
        assertDoesNotThrow(() -> pool.assertNotClosed());

        pool.close();
        assertThrows(IllegalStateException.class, () -> pool.assertNotClosed());
    }

    @Test
    public void testDefaultEvictDelay() {
        assertEquals(3000, AbstractPool.DEFAULT_EVICT_DELAY);
    }

    @Test
    public void testDefaultBalanceFactor() {
        assertEquals(0.2f, AbstractPool.DEFAULT_BALANCE_FACTOR);
    }

    @Test
    public void testAtomicCounters() {
        int threads = 10;
        int incrementsPerThread = 1000;
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(threads);

        for (int i = 0; i < threads; i++) {
            new Thread(() -> {
                try {
                    startLatch.await();
                    for (int j = 0; j < incrementsPerThread; j++) {
                        pool.incrementPutCount();
                        pool.incrementHitCount();
                        pool.incrementMissCount();
                        pool.incrementEvictionCount();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    endLatch.countDown();
                }
            }).start();
        }

        startLatch.countDown();

        try {
            assertTrue(endLatch.await(5, TimeUnit.SECONDS));
        } catch (InterruptedException e) {
            fail("Test interrupted");
        }

        PoolStats stats = pool.stats();
        assertEquals(threads * incrementsPerThread, stats.putCount());
        assertEquals(threads * incrementsPerThread, stats.hitCount());
        assertEquals(threads * incrementsPerThread, stats.missCount());
        assertEquals(threads * incrementsPerThread * 2, stats.getCount());
        assertEquals(threads * incrementsPerThread, stats.evictionCount());
    }

    @Test
    public void testEvictionPolicies() {
        TestAbstractPool lastAccessPool = new TestAbstractPool(10, 0, EvictionPolicy.LAST_ACCESS_TIME, false, 0.2f, 0);
        assertEquals(EvictionPolicy.LAST_ACCESS_TIME, lastAccessPool.evictionPolicy);

        TestAbstractPool accessCountPool = new TestAbstractPool(10, 0, EvictionPolicy.ACCESS_COUNT, false, 0.2f, 0);
        assertEquals(EvictionPolicy.ACCESS_COUNT, accessCountPool.evictionPolicy);

        TestAbstractPool expirationPool = new TestAbstractPool(10, 0, EvictionPolicy.EXPIRATION_TIME, false, 0.2f, 0);
        assertEquals(EvictionPolicy.EXPIRATION_TIME, expirationPool.evictionPolicy);
    }
}

package com.landawn.abacus.pool;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

public class ObjectPoolTest extends TestBase {

    private ObjectPool<TestPoolable> pool;

    private static class TestPoolable extends AbstractPoolable {
        private final String id;
        private boolean destroyed;
        private Poolable.Caller destroyedByCaller;

        TestPoolable(String id) {
            this(id, 10000, 5000);
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
    }

    @BeforeEach
    public void setUp() {
        pool = new GenericObjectPool<>(5, 0, EvictionPolicy.LAST_ACCESS_TIME, false, 0.2f);
    }

    @AfterEach
    public void tearDown() {
        if (pool != null && !pool.isClosed()) {
            pool.close();
        }
    }

    private void fill(int n) {
        for (int i = 0; i < n; i++) {
            assertTrue(pool.add(new TestPoolable("test" + i)));
        }
    }

    @Test
    public void testAdd() {
        fill(5);
        assertEquals(5, pool.size());
        TestPoolable extra = new TestPoolable("extra");
        assertFalse(pool.add(extra));
        assertFalse(pool.add(extra, false));
        assertFalse(extra.destroyed);
        TestPoolable destroy = new TestPoolable("destroy");
        assertFalse(pool.add(destroy, true));
        assertTrue(destroy.destroyed);
        assertEquals(Poolable.Caller.PUT_ADD_FAILURE, destroy.destroyedByCaller);

        ObjectPool<TestPoolable> open = new GenericObjectPool<>(2, 0, EvictionPolicy.LAST_ACCESS_TIME, false, 0.2f);
        try {
            TestPoolable ok = new TestPoolable("ok");
            assertTrue(open.add(ok, true));
            assertFalse(ok.destroyed);
        } finally {
            open.close();
        }
    }

    @Test
    public void testAdd_EdgeCase() throws InterruptedException {
        assertThrows(IllegalArgumentException.class, () -> pool.add(null));
        TestPoolable expired = new TestPoolable("expired", 1, 1);
        Thread.sleep(10);
        assertFalse(pool.add(expired));
    }

    @Test
    public void testAdd_Closed() {
        pool.close();
        assertThrows(IllegalStateException.class, () -> pool.add(new TestPoolable("test")));
        assertThrows(IllegalStateException.class, () -> pool.add(new TestPoolable("test"), 100, TimeUnit.MILLISECONDS));
    }

    @Test
    public void testAdd_Timeout() throws InterruptedException {
        fill(5);
        TestPoolable extra = new TestPoolable("extra");
        long start = System.currentTimeMillis();
        assertFalse(pool.add(extra, 100, TimeUnit.MILLISECONDS));
        assertTrue(System.currentTimeMillis() - start >= 90);
        TestPoolable destroy = new TestPoolable("destroy");
        assertFalse(pool.add(destroy, 50, TimeUnit.MILLISECONDS, true));
        assertTrue(destroy.destroyed);
        assertEquals(Poolable.Caller.PUT_ADD_FAILURE, destroy.destroyedByCaller);
        TestPoolable keep = new TestPoolable("keep");
        assertFalse(pool.add(keep, 50, TimeUnit.MILLISECONDS, false));
        assertFalse(keep.destroyed);

        ObjectPool<TestPoolable> open = new GenericObjectPool<>(5, 0, EvictionPolicy.LAST_ACCESS_TIME, false, 0.2f);
        try {
            for (int i = 0; i < 4; i++) {
                assertTrue(open.add(new TestPoolable("test" + i)));
            }
            assertTrue(open.add(new TestPoolable("last"), 100, TimeUnit.MILLISECONDS));
            assertEquals(5, open.size());
        } finally {
            open.close();
        }

        TestPoolable expired = new TestPoolable("expired", 1, 1);
        Thread.sleep(10);
        assertFalse(pool.add(expired, 100, TimeUnit.MILLISECONDS));
    }

    @Test
    public void testPoll() throws InterruptedException {
        TestPoolable poolable = new TestPoolable("test1");
        assertTrue(pool.add(poolable));
        TestPoolable taken = pool.poll();
        assertNotNull(taken);
        assertEquals("test1", taken.id);
        assertEquals(0, pool.size());
        assertEquals(1, taken.activityPrint().getAccessCount());
        assertNull(pool.poll());

        TestPoolable expired = new TestPoolable("expired", 50, 50);
        assertTrue(pool.add(expired));
        Thread.sleep(60);
        assertNull(pool.poll());
        assertTrue(expired.destroyed);
        assertEquals(Poolable.Caller.EVICT, expired.destroyedByCaller);
    }

    @Test
    public void testPoll_Closed() {
        pool.close();
        assertThrows(IllegalStateException.class, () -> pool.poll());
        assertThrows(IllegalStateException.class, () -> pool.poll(100, TimeUnit.MILLISECONDS));
    }

    @Test
    public void testPoll_Timeout() throws InterruptedException {
        long start = System.currentTimeMillis();
        assertNull(pool.poll(100, TimeUnit.MILLISECONDS));
        assertTrue(System.currentTimeMillis() - start >= 90);

        CountDownLatch added = new CountDownLatch(1);
        new Thread(() -> {
            try {
                Thread.sleep(50);
                pool.add(new TestPoolable("delayed"));
                added.countDown();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();
        TestPoolable taken = pool.poll(200, TimeUnit.MILLISECONDS);
        assertNotNull(taken);
        assertEquals("delayed", taken.id);
        added.await(500, TimeUnit.MILLISECONDS);
    }

    @Test
    public void testConcurrent() throws InterruptedException {
        int threads = 10;
        int ops = 100;
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(threads * 2);
        AtomicInteger added = new AtomicInteger();
        AtomicInteger taken = new AtomicInteger();
        for (int i = 0; i < threads; i++) {
            final int threadId = i;
            new Thread(() -> {
                try {
                    startLatch.await();
                    for (int j = 0; j < ops; j++) {
                        if (pool.add(new TestPoolable("t" + threadId + "-" + j))) {
                            added.incrementAndGet();
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    endLatch.countDown();
                }
            }).start();
            new Thread(() -> {
                try {
                    startLatch.await();
                    for (int j = 0; j < ops; j++) {
                        if (pool.poll() != null) {
                            taken.incrementAndGet();
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    endLatch.countDown();
                }
            }).start();
        }
        startLatch.countDown();
        assertTrue(endLatch.await(5, TimeUnit.SECONDS));
        assertEquals(added.get() - taken.get(), pool.size());
    }

    @Test
    public void testContains() {
        TestPoolable p1 = new TestPoolable("test1");
        pool.add(p1);
        assertTrue(pool.contains(p1));
        assertFalse(pool.contains(new TestPoolable("test3")));
        pool.close();
        assertThrows(IllegalStateException.class, () -> pool.contains(new TestPoolable("test")));
    }

    @Test
    public void testMemoryMeasure() {
        ObjectPool.MemoryMeasure<TestPoolable> measure = e -> 100;
        ObjectPool<TestPoolable> memPool = new GenericObjectPool<>(5, 0, EvictionPolicy.LAST_ACCESS_TIME, false, 0.2f, 1000, measure);
        try {
            assertTrue(memPool.add(new TestPoolable("test1")));
            assertEquals(100, memPool.stats().dataSize());
            assertTrue(memPool.add(new TestPoolable("test2")));
            assertEquals(200, memPool.stats().dataSize());
            assertNotNull(memPool.poll());
            assertEquals(100, memPool.stats().dataSize());
            for (int i = 0; i < 3; i++) {
                memPool.add(new TestPoolable("m" + i));
            }
            long before = memPool.stats().dataSize();
            memPool.evict();
            assertTrue(memPool.stats().dataSize() < before);
            memPool.clear();
            assertEquals(0, memPool.stats().dataSize());
        } finally {
            memPool.close();
        }
        ObjectPool.MemoryMeasure<String> stringMeasure = s -> s.length() * 2;
        assertEquals(10, stringMeasure.sizeOf("hello"));
    }

    @Test
    public void testEvict() {
        fill(5);
        pool.evict();
        assertTrue(pool.size() < 5);
        ObjectPool<TestPoolable> empty = new GenericObjectPool<>(5, 0, EvictionPolicy.LAST_ACCESS_TIME, false, 0.2f);
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
        fill(3);
        pool.clear();
        assertEquals(0, pool.size());
        assertTrue(pool.isEmpty());
        pool.close();
        assertThrows(IllegalStateException.class, () -> pool.clear());
    }

    @Test
    public void testClose() {
        TestPoolable p1 = new TestPoolable("test1");
        TestPoolable p2 = new TestPoolable("test2");
        pool.add(p1);
        pool.add(p2);
        pool.close();
        assertTrue(pool.isClosed());
        assertTrue(p1.destroyed);
        assertTrue(p2.destroyed);
        assertEquals(Poolable.Caller.CLOSE, p1.destroyedByCaller);
    }

    @Test
    public void testPoolBasics() {
        assertTrue(pool.isEmpty());
        pool.add(new TestPoolable("test1"));
        assertFalse(pool.isEmpty());
        pool.poll();
        assertTrue(pool.isEmpty());
        assertEquals(5, pool.capacity());
        assertEquals(5, pool.stats().capacity());
        AbstractPool abstractPool = (AbstractPool) pool;
        abstractPool.lock();
        abstractPool.unlock();
    }
}

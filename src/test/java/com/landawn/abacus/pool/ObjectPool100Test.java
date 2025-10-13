package com.landawn.abacus.pool;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import com.landawn.abacus.TestBase;

@Tag("new-test")
public class ObjectPool100Test extends TestBase {

    private TestObjectPool pool;

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

    private static class TestObjectPool implements ObjectPool<TestPoolable> {
        private final java.util.Queue<TestPoolable> queue = new java.util.LinkedList<>();
        private final int capacity;
        private boolean closed = false;
        private final Object lock = new Object();
        private long totalMemorySize = 0;
        private final ObjectPool.MemoryMeasure<TestPoolable> memoryMeasure;

        TestObjectPool(int capacity) {
            this(capacity, null);
        }

        TestObjectPool(int capacity, ObjectPool.MemoryMeasure<TestPoolable> memoryMeasure) {
            this.capacity = capacity;
            this.memoryMeasure = memoryMeasure;
        }

        @Override
        public boolean add(TestPoolable e) {
            if (closed)
                throw new IllegalStateException("Pool is closed");
            if (e == null)
                throw new IllegalArgumentException("Element cannot be null");
            if (e.activityPrint().isExpired())
                return false;

            synchronized (lock) {
                if (queue.size() >= capacity)
                    return false;
                if (memoryMeasure != null) {
                    totalMemorySize += memoryMeasure.sizeOf(e);
                }
                queue.offer(e);
                lock.notifyAll();
                return true;
            }
        }

        @Override
        public boolean add(TestPoolable e, boolean autoDestroyOnFailedToAdd) {
            boolean success = false;
            try {
                success = add(e);
            } finally {
                if (!success && autoDestroyOnFailedToAdd && e != null) {
                    e.destroy(Poolable.Caller.PUT_ADD_FAILURE);
                }
            }
            return success;
        }

        @Override
        public boolean add(TestPoolable e, long timeout, TimeUnit unit) throws InterruptedException {
            if (closed)
                throw new IllegalStateException("Pool is closed");
            if (e == null)
                throw new IllegalArgumentException("Element cannot be null");
            if (e.activityPrint().isExpired())
                return false;

            synchronized (lock) {
                long deadline = System.currentTimeMillis() + unit.toMillis(timeout);
                while (queue.size() >= capacity) {
                    long waitTime = deadline - System.currentTimeMillis();
                    if (waitTime <= 0)
                        return false;
                    lock.wait(waitTime);
                }
                if (memoryMeasure != null) {
                    totalMemorySize += memoryMeasure.sizeOf(e);
                }
                queue.offer(e);
                lock.notifyAll();
                return true;
            }
        }

        @Override
        public boolean add(TestPoolable e, long timeout, TimeUnit unit, boolean autoDestroyOnFailedToAdd) throws InterruptedException {
            boolean success = false;
            try {
                success = add(e, timeout, unit);
            } finally {
                if (!success && autoDestroyOnFailedToAdd && e != null) {
                    e.destroy(Poolable.Caller.PUT_ADD_FAILURE);
                }
            }
            return success;
        }

        @Override
        public TestPoolable take() {
            if (closed)
                throw new IllegalStateException("Pool is closed");

            synchronized (lock) {
                TestPoolable e = queue.poll();
                if (e != null) {
                    if (e.activityPrint().isExpired()) {
                        e.destroy(Poolable.Caller.EVICT);
                        return null;
                    }
                    e.activityPrint().updateLastAccessTime();
                    e.activityPrint().updateAccessCount();
                    if (memoryMeasure != null) {
                        totalMemorySize -= memoryMeasure.sizeOf(e);
                    }
                    lock.notifyAll();
                }
                return e;
            }
        }

        @Override
        public TestPoolable take(long timeout, TimeUnit unit) throws InterruptedException {
            if (closed)
                throw new IllegalStateException("Pool is closed");

            synchronized (lock) {
                long deadline = System.currentTimeMillis() + unit.toMillis(timeout);
                while (queue.isEmpty()) {
                    long waitTime = deadline - System.currentTimeMillis();
                    if (waitTime <= 0)
                        return null;
                    lock.wait(waitTime);
                }
                return take();
            }
        }

        @Override
        public boolean contains(TestPoolable valueToFind) {
            if (closed)
                throw new IllegalStateException("Pool is closed");
            synchronized (lock) {
                return queue.contains(valueToFind);
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
                return queue.size();
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
                int toRemove = Math.max(1, queue.size() / 5);
                for (int i = 0; i < toRemove && !queue.isEmpty(); i++) {
                    TestPoolable e = queue.poll();
                    if (e != null && memoryMeasure != null) {
                        totalMemorySize -= memoryMeasure.sizeOf(e);
                    }
                }
            }
        }

        @Override
        public void clear() {
            if (closed)
                throw new IllegalStateException("Pool is closed");
            synchronized (lock) {
                queue.clear();
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
                for (TestPoolable e : queue) {
                    e.destroy(Poolable.Caller.CLOSE);
                }
                queue.clear();
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
        pool = new TestObjectPool(5);
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
    public void testAddExpired() {
        TestPoolable expired = new TestPoolable("expired", 1, 1);
        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
        }
        assertFalse(pool.add(expired));
    }

    @Test
    public void testAddToFullPool() {
        for (int i = 0; i < 5; i++) {
            assertTrue(pool.add(new TestPoolable("test" + i)));
        }
        assertEquals(5, pool.size());

        TestPoolable extra = new TestPoolable("extra");
        assertFalse(pool.add(extra));
        assertEquals(5, pool.size());
    }

    @Test
    public void testAddToClosedPool() {
        pool.close();
        assertThrows(IllegalStateException.class, () -> pool.add(new TestPoolable("test")));
    }

    @Test
    public void testAddWithAutoDestroy() {
        for (int i = 0; i < 5; i++) {
            assertTrue(pool.add(new TestPoolable("test" + i)));
        }

        TestPoolable extra = new TestPoolable("extra");
        assertFalse(pool.add(extra, true));
        assertTrue(extra.isDestroyed());
        assertEquals(Poolable.Caller.PUT_ADD_FAILURE, extra.getDestroyedByCaller());
    }

    @Test
    public void testAddWithTimeout() throws InterruptedException {
        for (int i = 0; i < 5; i++) {
            assertTrue(pool.add(new TestPoolable("test" + i)));
        }

        TestPoolable extra = new TestPoolable("extra");
        long start = System.currentTimeMillis();
        assertFalse(pool.add(extra, 100, TimeUnit.MILLISECONDS));
        long elapsed = System.currentTimeMillis() - start;
        assertTrue(elapsed >= 90);
    }

    @Test
    public void testAddWithTimeoutSuccess() throws InterruptedException {
        for (int i = 0; i < 4; i++) {
            assertTrue(pool.add(new TestPoolable("test" + i)));
        }

        TestPoolable extra = new TestPoolable("extra");
        assertTrue(pool.add(extra, 100, TimeUnit.MILLISECONDS));
        assertEquals(5, pool.size());
    }

    @Test
    public void testAddWithTimeoutAndAutoDestroy() throws InterruptedException {
        for (int i = 0; i < 5; i++) {
            assertTrue(pool.add(new TestPoolable("test" + i)));
        }

        TestPoolable extra = new TestPoolable("extra");
        assertFalse(pool.add(extra, 100, TimeUnit.MILLISECONDS, true));
        assertTrue(extra.isDestroyed());
        assertEquals(Poolable.Caller.PUT_ADD_FAILURE, extra.getDestroyedByCaller());
    }

    @Test
    public void testTake() {
        TestPoolable poolable = new TestPoolable("test1");
        assertTrue(pool.add(poolable));

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
    public void testTakeExpiredObject() throws InterruptedException {
        TestPoolable expired = new TestPoolable("expired", 50, 50);
        assertTrue(pool.add(expired));

        Thread.sleep(60);

        TestPoolable taken = pool.take();
        assertNull(taken);
        assertTrue(expired.isDestroyed());
        assertEquals(Poolable.Caller.EVICT, expired.getDestroyedByCaller());
    }

    @Test
    public void testTakeFromClosedPool() {
        pool.close();
        assertThrows(IllegalStateException.class, () -> pool.take());
    }

    @Test
    public void testTakeWithTimeout() throws InterruptedException {
        long start = System.currentTimeMillis();
        TestPoolable taken = pool.take(100, TimeUnit.MILLISECONDS);
        long elapsed = System.currentTimeMillis() - start;

        assertNull(taken);
        assertTrue(elapsed >= 90);
    }

    @Test
    public void testTakeWithTimeoutSuccess() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicBoolean success = new AtomicBoolean(false);

        new Thread(() -> {
            try {
                Thread.sleep(50);
                pool.add(new TestPoolable("delayed"));
                latch.countDown();
            } catch (InterruptedException e) {
            }
        }).start();

        TestPoolable taken = pool.take(200, TimeUnit.MILLISECONDS);
        assertNotNull(taken);
        assertEquals("delayed", taken.getId());

        latch.await(500, TimeUnit.MILLISECONDS);
    }

    @Test
    public void testContains() {
        TestPoolable poolable1 = new TestPoolable("test1");
        TestPoolable poolable2 = new TestPoolable("test2");
        TestPoolable poolable3 = new TestPoolable("test3");

        pool.add(poolable1);
        pool.add(poolable2);

        assertTrue(pool.contains(poolable1));
        assertTrue(pool.contains(poolable2));
        assertFalse(pool.contains(poolable3));
    }

    @Test
    public void testContainsOnClosedPool() {
        pool.close();
        assertThrows(IllegalStateException.class, () -> pool.contains(new TestPoolable("test")));
    }

    @Test
    public void testMemoryMeasure() {
        ObjectPool.MemoryMeasure<TestPoolable> measure = e -> 100;
        TestObjectPool memPool = new TestObjectPool(5, measure);

        TestPoolable poolable1 = new TestPoolable("test1");
        TestPoolable poolable2 = new TestPoolable("test2");

        assertTrue(memPool.add(poolable1));
        assertEquals(100, memPool.getTotalMemorySize());

        assertTrue(memPool.add(poolable2));
        assertEquals(200, memPool.getTotalMemorySize());

        TestPoolable taken = memPool.take();
        assertNotNull(taken);
        assertEquals(100, memPool.getTotalMemorySize());

        memPool.clear();
        assertEquals(0, memPool.getTotalMemorySize());
    }

    @Test
    public void testMemoryMeasureInterface() {
        ObjectPool.MemoryMeasure<String> stringMeasure = s -> s.length() * 2;

        assertEquals(10, stringMeasure.sizeOf("hello"));
        assertEquals(0, stringMeasure.sizeOf(""));
        assertEquals(20, stringMeasure.sizeOf("ten chars!"));
    }

    @Test
    public void testConcurrentAddTake() throws InterruptedException {
        int numThreads = 10;
        int operationsPerThread = 100;
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(numThreads * 2);
        AtomicInteger addedCount = new AtomicInteger(0);
        AtomicInteger takenCount = new AtomicInteger(0);

        for (int i = 0; i < numThreads; i++) {
            final int threadId = i;
            new Thread(() -> {
                try {
                    startLatch.await();
                    for (int j = 0; j < operationsPerThread; j++) {
                        if (pool.add(new TestPoolable("t" + threadId + "-" + j))) {
                            addedCount.incrementAndGet();
                        }
                        Thread.yield();
                    }
                } catch (InterruptedException e) {
                } finally {
                    endLatch.countDown();
                }
            }).start();
        }

        for (int i = 0; i < numThreads; i++) {
            new Thread(() -> {
                try {
                    startLatch.await();
                    for (int j = 0; j < operationsPerThread; j++) {
                        if (pool.take() != null) {
                            takenCount.incrementAndGet();
                        }
                        Thread.yield();
                    }
                } catch (InterruptedException e) {
                } finally {
                    endLatch.countDown();
                }
            }).start();
        }

        startLatch.countDown();
        assertTrue(endLatch.await(5, TimeUnit.SECONDS));

        int finalSize = pool.size();
        assertEquals(addedCount.get() - takenCount.get(), finalSize);
        assertTrue(finalSize >= 0 && finalSize <= pool.capacity());
    }

    @Test
    public void testVacate() {
        for (int i = 0; i < 5; i++) {
            assertTrue(pool.add(new TestPoolable("test" + i)));
        }
        assertEquals(5, pool.size());

        pool.vacate();
        assertTrue(pool.size() < 5);
        assertTrue(pool.size() >= 0);
    }

    @Test
    public void testClear() {
        for (int i = 0; i < 3; i++) {
            assertTrue(pool.add(new TestPoolable("test" + i)));
        }
        assertEquals(3, pool.size());

        pool.clear();
        assertEquals(0, pool.size());
        assertTrue(pool.isEmpty());
    }

    @Test
    public void testClose() {
        TestPoolable poolable1 = new TestPoolable("test1");
        TestPoolable poolable2 = new TestPoolable("test2");

        pool.add(poolable1);
        pool.add(poolable2);

        assertFalse(pool.isClosed());
        pool.close();
        assertTrue(pool.isClosed());

        assertTrue(poolable1.isDestroyed());
        assertTrue(poolable2.isDestroyed());
        assertEquals(Poolable.Caller.CLOSE, poolable1.getDestroyedByCaller());
        assertEquals(Poolable.Caller.CLOSE, poolable2.getDestroyedByCaller());
    }
}

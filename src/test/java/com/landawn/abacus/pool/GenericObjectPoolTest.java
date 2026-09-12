package com.landawn.abacus.pool;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.NotSerializableException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

public class GenericObjectPoolTest extends TestBase {

    private static final class SpuriousWakeupCondition implements Condition {
        private final int spuriousWakeups;
        private int awaitCount;

        private SpuriousWakeupCondition(final int spuriousWakeups) {
            this.spuriousWakeups = spuriousWakeups;
        }

        @Override
        public long awaitNanos(final long nanosTimeout) {
            return ++awaitCount <= spuriousWakeups ? 1 : 0;
        }

        @Override
        public void await() {
            throw new UnsupportedOperationException();
        }

        @Override
        public void awaitUninterruptibly() {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean await(final long time, final TimeUnit unit) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean awaitUntil(final Date deadline) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void signal() {
        }

        @Override
        public void signalAll() {
        }
    }

    private static final class AtomicEvictPool extends GenericObjectPool<TestPoolable> {
        private final AtomicBoolean vacateDispatched;

        private AtomicEvictPool(final AtomicBoolean vacateDispatched) {
            super(8, 0, EvictionPolicy.FIFO, true, 0.5f);
            this.vacateDispatched = vacateDispatched;
        }

        @Override
        protected void vacate(final int numberToEvict) {
            vacateDispatched.set(true);
            super.vacate(numberToEvict);
        }
    }

    private static final class DeserializationEvictionProbePool extends GenericObjectPool<TestPoolable> {
        private static final AtomicBoolean observedPartiallyDeserializedState = new AtomicBoolean();
        private static volatile CountDownLatch evictionRan = new CountDownLatch(1);

        private boolean subclassStateInitialized = true;
        private transient volatile boolean deserializationProbeEnabled;

        private DeserializationEvictionProbePool() {
            super(2, 1, EvictionPolicy.LAST_ACCESS_TIME);
        }

        private void readObject(final ObjectInputStream input) throws IOException, ClassNotFoundException {
            deserializationProbeEnabled = true;
            try {
                Thread.sleep(100);
            } catch (final InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IOException(e);
            }
            input.defaultReadObject();
        }

        @Override
        protected void removeExpired() {
            if (deserializationProbeEnabled) {
                if (!subclassStateInitialized) {
                    observedPartiallyDeserializedState.set(true);
                }
                evictionRan.countDown();
            }
            super.removeExpired();
        }

        private static void resetProbe() {
            observedPartiallyDeserializedState.set(false);
            evictionRan = new CountDownLatch(1);
        }
    }

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

    private GenericObjectPool<TestPoolable> pool;

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

    private static GenericObjectPool<TestPoolable> noBalance(int capacity) {
        return new GenericObjectPool<>(capacity, 0, EvictionPolicy.LAST_ACCESS_TIME, false, 0.2f);
    }

    private static byte[] serialize(final Object obj) throws IOException {
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ObjectOutputStream oos = new ObjectOutputStream(baos)) {
            oos.writeObject(obj);
        }
        return baos.toByteArray();
    }

    @SuppressWarnings("unchecked")
    private static <T> T deserialize(final byte[] bytes) throws IOException, ClassNotFoundException {
        try (ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(bytes))) {
            return (T) ois.readObject();
        }
    }

    private static Object shutdownHookOf(final Object target) throws Exception {
        final Field field = AbstractPool.class.getDeclaredField("shutdownHook");
        field.setAccessible(true);
        return field.get(target);
    }

    @SuppressWarnings("unchecked")
    private static void invokeRemoveByIdentity(final GenericObjectPool<?> target, final Collection<?> victims) throws Exception {
        final Method method = GenericObjectPool.class.getDeclaredMethod("removeByIdentity", Collection.class);
        method.setAccessible(true);
        method.invoke(target, victims);
    }

    private static void awaitCleanupRelease(final CountDownLatch release) {
        try {
            release.await(5, TimeUnit.SECONDS);
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @Test
    public void testAdd() {
        TestPoolable poolable = new TestPoolable("test1");
        assertTrue(pool.add(poolable));
        assertEquals(1, pool.size());
        assertTrue(pool.contains(poolable));
        assertEquals(1, pool.stats().putCount());

        GenericObjectPool<TestPoolable> noBalancePool = noBalance(3);
        try {
            for (int i = 0; i < 3; i++) {
                assertTrue(noBalancePool.add(new TestPoolable("test" + i)));
            }
            assertFalse(noBalancePool.add(new TestPoolable("extra")));
            assertEquals(3, noBalancePool.size());
        } finally {
            noBalancePool.close();
        }

        GenericObjectPool<TestPoolable> balancePool = new GenericObjectPool<>(3, 0, EvictionPolicy.LAST_ACCESS_TIME, true, 0.4f);
        try {
            for (int i = 0; i < 3; i++) {
                assertTrue(balancePool.add(new TestPoolable("test" + i)));
            }
            assertTrue(balancePool.add(new TestPoolable("extra")));
            assertEquals(3, balancePool.size());
        } finally {
            balancePool.close();
        }

        GenericObjectPool<TestPoolable> small = new GenericObjectPool<>(1, 0, EvictionPolicy.LAST_ACCESS_TIME, true, 0.2f);
        try {
            TestPoolable first = new TestPoolable("first");
            assertTrue(small.add(first));
            assertTrue(small.add(new TestPoolable("second")));
            assertEquals(1, small.size());
            assertTrue(first.isDestroyed());
        } finally {
            small.close();
        }

        ObjectPool.MemoryMeasure<TestPoolable> measure = e -> 100;
        GenericObjectPool<TestPoolable> memPool = new GenericObjectPool<>(10, 0, EvictionPolicy.LAST_ACCESS_TIME, 250, measure);
        try {
            assertTrue(memPool.add(new TestPoolable("test1")));
            assertTrue(memPool.add(new TestPoolable("test2")));
            assertTrue(memPool.add(new TestPoolable("test3")));
            assertEquals(2, memPool.size());
        } finally {
            memPool.close();
        }

        GenericObjectPool<TestPoolable> unlimited = new GenericObjectPool<>(10, 0, EvictionPolicy.LAST_ACCESS_TIME, 0, measure);
        try {
            for (int i = 0; i < 5; i++) {
                assertTrue(unlimited.add(new TestPoolable("test" + i)));
            }
            assertEquals(5, unlimited.size());
        } finally {
            unlimited.close();
        }

        GenericObjectPool<TestPoolable> memNoBalance = new GenericObjectPool<>(10, 0, EvictionPolicy.LAST_ACCESS_TIME, false, 0.2f, 250, measure);
        try {
            assertTrue(memNoBalance.add(new TestPoolable("p1")));
            assertTrue(memNoBalance.add(new TestPoolable("p2")));
            assertFalse(memNoBalance.add(new TestPoolable("p3")));
            assertEquals(2, memNoBalance.size());
        } finally {
            memNoBalance.close();
        }

        for (int i = 0; i < 10; i++) {
            pool.add(new TestPoolable("full" + i));
        }
        TestPoolable extra = new TestPoolable("extra");
        assertTrue(pool.add(extra, true));
        assertFalse(extra.isDestroyed());
    }

    @Test
    public void testAdd_EdgeCase() throws InterruptedException {
        assertThrows(IllegalArgumentException.class, () -> pool.add(null));
        assertThrows(IllegalArgumentException.class, () -> pool.add(null, 100, TimeUnit.MILLISECONDS));

        TestPoolable expired = new TestPoolable("expired", 10, 10);
        Thread.sleep(20);
        assertFalse(pool.add(expired));
        assertEquals(0, pool.size());
        assertFalse(pool.add(expired, 100, TimeUnit.MILLISECONDS));

        ObjectPool.MemoryMeasure<TestPoolable> negative = e -> -1;
        GenericObjectPool<TestPoolable> memPool = new GenericObjectPool<>(10, 0, EvictionPolicy.LAST_ACCESS_TIME, 1000, negative);
        try {
            assertFalse(memPool.add(new TestPoolable("p1")));
            assertFalse(memPool.add(new TestPoolable("bad"), 20, TimeUnit.MILLISECONDS));
            assertEquals(0, memPool.size());
        } finally {
            memPool.close();
        }
    }

    @Test
    public void testAdd_Closed() throws InterruptedException {
        GenericObjectPool<TestPoolable> closedPool = new GenericObjectPool<>(1, 0, EvictionPolicy.LAST_ACCESS_TIME);
        TestPoolable expired = new TestPoolable("expired", 1, 1);
        Thread.sleep(20);
        closedPool.close();
        assertThrows(IllegalStateException.class, () -> closedPool.add(expired));

        pool.close();
        assertThrows(IllegalStateException.class, () -> pool.add(new TestPoolable("p1")));
        assertThrows(IllegalStateException.class, () -> pool.add(new TestPoolable("p1"), 100, TimeUnit.MILLISECONDS));
    }

    @Test
    public void testAdd_Timeout() throws InterruptedException {
        for (int i = 0; i < 10; i++) {
            pool.add(new TestPoolable("test" + i));
        }
        long start = System.currentTimeMillis();
        assertTrue(pool.add(new TestPoolable("extra"), 100, TimeUnit.MILLISECONDS));
        assertTrue(System.currentTimeMillis() - start < 90);

        GenericObjectPool<TestPoolable> noBalancePool = noBalance(2);
        try {
            noBalancePool.add(new TestPoolable("p1"));
            noBalancePool.add(new TestPoolable("p2"));
            start = System.currentTimeMillis();
            assertFalse(noBalancePool.add(new TestPoolable("extra"), 100, TimeUnit.MILLISECONDS));
            assertTrue(System.currentTimeMillis() - start >= 90);
            assertEquals(2, noBalancePool.stats().putCount());
        } finally {
            noBalancePool.close();
        }

        GenericObjectPool<TestPoolable> auto = new GenericObjectPool<>(1, 0, EvictionPolicy.LAST_ACCESS_TIME);
        try {
            TestPoolable victim = new TestPoolable("v1");
            TestPoolable newcomer = new TestPoolable("v2");
            assertTrue(auto.add(victim));
            long nano = System.nanoTime();
            assertTrue(auto.add(newcomer, 5, TimeUnit.SECONDS));
            assertTrue((System.nanoTime() - nano) / 1_000_000 < 2_000);
            assertTrue(victim.isDestroyed());
            assertEquals(Poolable.Caller.VACATE, victim.getDestroyedByCaller());
            assertSame(newcomer, auto.poll());
        } finally {
            auto.close();
        }

        assertThrows(IllegalArgumentException.class, () -> pool.add(new TestPoolable("value"), 1, null));
        assertThrows(IllegalArgumentException.class, () -> pool.poll(1, null));
    }

    @Test
    public void testAdd_MemoryMeasureThrows() throws InterruptedException {
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
    public void testAdd_AutoDestroyFailure() throws InterruptedException {
        GenericObjectPool<TestPoolable> noBalancePool = noBalance(2);
        try {
            noBalancePool.add(new TestPoolable("p1"));
            noBalancePool.add(new TestPoolable("p2"));

            TestPoolable keep = new TestPoolable("keep");
            assertFalse(noBalancePool.add(keep, false));
            assertFalse(keep.isDestroyed());

            TestPoolable extra = new TestPoolable("extra");
            assertFalse(noBalancePool.add(extra, true));
            assertTrue(extra.isDestroyed());
            assertEquals(Poolable.Caller.PUT_ADD_FAILURE, extra.getDestroyedByCaller());

            TestPoolable timed = new TestPoolable("timed");
            assertFalse(noBalancePool.add(timed, 50, TimeUnit.MILLISECONDS, true));
            assertTrue(timed.isDestroyed());
            assertEquals(Poolable.Caller.PUT_ADD_FAILURE, timed.getDestroyedByCaller());
        } finally {
            noBalancePool.close();
        }
    }

    @Test
    public void testAdd_ReChecksExpiryInsideLock() {
        GenericObjectPool<TestPoolable> memPool = new GenericObjectPool<>(10, 0, EvictionPolicy.LAST_ACCESS_TIME, 0, e -> {
            try {
                Thread.sleep(50);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
            }
            return 1L;
        });
        try {
            assertFalse(memPool.add(new TestPoolable("x", 10, 10)));
            assertEquals(0, memPool.size());
        } finally {
            memPool.close();
        }
    }

    @Test
    public void testLIFO() {
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
    public void testPoll() throws InterruptedException {
        assertNull(pool.poll());
        TestPoolable poolable = new TestPoolable("test1");
        pool.add(poolable);
        TestPoolable taken = pool.poll();
        assertNotNull(taken);
        assertEquals("test1", taken.getId());
        assertEquals(0, pool.size());
        assertEquals(1, taken.activityPrint().getAccessCount());

        for (int i = 0; i < 5; i++) {
            pool.add(new TestPoolable("p" + i));
        }
        for (int i = 0; i < 5; i++) {
            assertNotNull(pool.poll());
        }
        assertNull(pool.poll());

        TestPoolable expired = new TestPoolable("expired", 50, 50);
        pool.add(expired);
        Thread.sleep(60);
        assertNull(pool.poll());
        assertTrue(expired.isDestroyed());
        assertEquals(Poolable.Caller.EVICT, expired.getDestroyedByCaller());
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
        assertEquals("delayed", taken.getId());
        added.await();

        TestPoolable expired = new TestPoolable("expired", 30, 30);
        pool.add(expired);
        Thread.sleep(50);
        start = System.currentTimeMillis();
        assertNull(pool.poll(100, TimeUnit.MILLISECONDS));
        assertTrue(System.currentTimeMillis() - start >= 90);
        assertTrue(expired.isDestroyed());
    }

    @Test
    public void testPoll_SkipExpiredReturnsNext() throws InterruptedException {
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

        TestPoolable valid = new TestPoolable("valid");
        pool.add(valid);
        pool.add(new TestPoolable("expired1", 1, 1));
        pool.add(new TestPoolable("expired2", 1, 1));
        Thread.sleep(20);
        long nano = System.nanoTime();
        TestPoolable immediate = pool.poll(2, TimeUnit.SECONDS);
        assertEquals("valid", immediate.getId());
        assertTrue((System.nanoTime() - nano) / 1_000_000 < 500);

        for (int i = 0; i < 5; i++) {
            pool.add(new TestPoolable("all" + i, 1, 1));
        }
        Thread.sleep(20);
        nano = System.nanoTime();
        assertNull(pool.poll(100, TimeUnit.MILLISECONDS));
        assertTrue((System.nanoTime() - nano) / 1_000_000 < 500);
    }

    @Test
    public void testPoll_HitMissStats() throws InterruptedException {
        pool.add(new TestPoolable("p1"));
        pool.poll();
        pool.poll();
        PoolStats stats = pool.stats();
        assertEquals(1, stats.hitCount());
        assertEquals(1, stats.missCount());

        assertNull(pool.poll(20, TimeUnit.MILLISECONDS));
        stats = pool.stats();
        assertEquals(1, stats.hitCount());
        assertEquals(2, stats.missCount());

        pool.add(new TestPoolable("p2"));
        assertNotNull(pool.poll(100, TimeUnit.MILLISECONDS));
        stats = pool.stats();
        assertEquals(2, stats.hitCount());
        assertEquals(2, stats.missCount());

        for (int i = 0; i < 3; i++) {
            pool.add(new TestPoolable("p" + i));
        }
        for (int i = 0; i < 3; i++) {
            assertNotNull(pool.poll(50, TimeUnit.MILLISECONDS));
        }
        assertNull(pool.poll(20, TimeUnit.MILLISECONDS));
        assertNull(pool.poll(20, TimeUnit.MILLISECONDS));
        stats = pool.stats();
        assertEquals(5, stats.hitCount());
        assertEquals(4, stats.missCount());
        assertEquals(9, stats.getCount());
    }

    @Test
    public void testPoll_ClosedWhileWaiting_NoSpuriousMiss() throws InterruptedException {
        CountDownLatch takingLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(1);
        AtomicBoolean gotIllegalState = new AtomicBoolean();
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
        Thread.sleep(100);
        pool.close();
        assertTrue(doneLatch.await(3, TimeUnit.SECONDS));
        assertTrue(gotIllegalState.get());
        assertEquals(0, pool.missCount.get());
        assertEquals(0, pool.hitCount.get());
    }

    @Test
    public void testPoll_RaceWithClose() throws Exception {
        pool.add(new TestPoolable("racing"));
        ReentrantLock poolLock = pool.lock;
        CountDownLatch holderHasLock = new CountDownLatch(1);
        CountDownLatch takeMayProceed = new CountDownLatch(1);
        Thread holder = new Thread(() -> {
            poolLock.lock();
            try {
                holderHasLock.countDown();
                takeMayProceed.await();
                pool.isClosed = true;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                poolLock.unlock();
            }
        });
        holder.start();
        holderHasLock.await();
        AtomicBoolean threwISE = new AtomicBoolean();
        AtomicBoolean returnedElement = new AtomicBoolean();
        Thread taker = new Thread(() -> {
            try {
                if (pool.poll() != null) {
                    returnedElement.set(true);
                }
            } catch (IllegalStateException e) {
                threwISE.set(true);
            }
        });
        taker.start();
        Thread.sleep(50);
        takeMayProceed.countDown();
        holder.join();
        taker.join();
        assertTrue(threwISE.get());
        assertFalse(returnedElement.get());
    }

    @Test
    public void testPoll_MemoryMeasureThrows() throws InterruptedException {
        AtomicBoolean throwOnNext = new AtomicBoolean();
        ObjectPool.MemoryMeasure<TestPoolable> measure = e -> {
            if (throwOnNext.get()) {
                throw new RuntimeException("sizeOf");
            }
            return 0L;
        };
        GenericObjectPool<TestPoolable> memPool = new GenericObjectPool<>(10, 0, EvictionPolicy.LAST_ACCESS_TIME, true, 0.2f, 1024L * 1024L, measure);
        try {
            TestPoolable original = new TestPoolable("mem");
            assertTrue(memPool.add(original));
            throwOnNext.set(true);
            TestPoolable popped = memPool.poll();
            assertNotNull(popped);
            assertEquals("mem", popped.getId());
            assertFalse(popped.isDestroyed());
            assertEquals(0, memPool.size());

            throwOnNext.set(false);
            TestPoolable timed = new TestPoolable("timed");
            assertTrue(memPool.add(timed));
            throwOnNext.set(true);
            TestPoolable timedPop = memPool.poll(1, TimeUnit.SECONDS);
            assertNotNull(timedPop);
            assertEquals("timed", timedPop.getId());
            assertFalse(timedPop.isDestroyed());
        } finally {
            memPool.close();
        }
    }

    @Test
    public void testPoll_ExpiredDestroyCallback() {
        GenericObjectPool<TestPoolable> p = new GenericObjectPool<>(1, 0, EvictionPolicy.LAST_ACCESS_TIME);
        AtomicReference<Boolean> lockHeld = new AtomicReference<>();
        AtomicReference<Boolean> stillPresent = new AtomicReference<>();
        TestPoolable expired = new TestPoolable("expired") {
            @Override
            public void destroy(final Poolable.Caller caller) {
                lockHeld.set(p.lock.isHeldByCurrentThread());
                stillPresent.set(p.contains(this));
                super.destroy(caller);
            }
        };
        try {
            assertTrue(p.add(expired));
            expired.activityPrint().setCreatedTime(System.currentTimeMillis() - 20_000);
            assertNull(p.poll());
            assertEquals(Boolean.FALSE, lockHeld.get());
            assertEquals(Boolean.FALSE, stillPresent.get());
            assertEquals(Poolable.Caller.EVICT, expired.getDestroyedByCaller());
        } finally {
            p.close();
        }
    }

    @Test
    public void testConcurrent() throws InterruptedException {
        int threads = 10;
        int ops = 100;
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(threads);
        AtomicInteger addCount = new AtomicInteger();
        AtomicInteger takeCount = new AtomicInteger();
        for (int i = 0; i < threads; i++) {
            final int threadId = i;
            new Thread(() -> {
                try {
                    startLatch.await();
                    for (int j = 0; j < ops; j++) {
                        if (j % 2 == 0) {
                            if (pool.add(new TestPoolable("t" + threadId + "-" + j))) {
                                addCount.incrementAndGet();
                            }
                        } else if (pool.poll() != null) {
                            takeCount.incrementAndGet();
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
        assertEquals(addCount.get() - takeCount.get(), pool.size());
    }

    @Test
    public void testContains() {
        TestPoolable p1 = new TestPoolable("p1");
        TestPoolable p2 = new TestPoolable("p2");
        pool.add(p1);
        pool.add(p2);
        assertTrue(pool.contains(p1));
        assertTrue(pool.contains(p2));
        assertFalse(pool.contains(new TestPoolable("p3")));
        pool.close();
        assertThrows(IllegalStateException.class, () -> pool.contains(new TestPoolable("p1")));
    }

    @Test
    public void testEvict() throws InterruptedException {
        for (int i = 0; i < 10; i++) {
            pool.add(new TestPoolable("test" + i));
        }
        pool.evict();
        assertEquals(8, pool.size());
        pool.evict();
        pool.evict();
        assertTrue(pool.size() >= 0);

        GenericObjectPool<TestPoolable> custom = new GenericObjectPool<>(10, 0, EvictionPolicy.LAST_ACCESS_TIME, true, 0.5f);
        try {
            for (int i = 0; i < 10; i++) {
                custom.add(new TestPoolable("test" + i));
            }
            custom.evict();
            assertEquals(5, custom.size());
        } finally {
            custom.close();
        }

        GenericObjectPool<TestPoolable> all = new GenericObjectPool<>(5, 0, EvictionPolicy.LAST_ACCESS_TIME, true, 1.0f);
        try {
            for (int i = 0; i < 3; i++) {
                all.add(new TestPoolable("p" + i));
            }
            all.evict();
            assertEquals(0, all.size());
        } finally {
            all.close();
        }

        TestPoolable v1 = new TestPoolable("p1");
        TestPoolable v2 = new TestPoolable("p2");
        TestPoolable v3 = new TestPoolable("p3");
        pool.clear();
        pool.add(v1);
        pool.add(v2);
        pool.add(v3);
        pool.vacate(2);
        int destroyed = (v1.isDestroyed() ? 1 : 0) + (v2.isDestroyed() ? 1 : 0) + (v3.isDestroyed() ? 1 : 0);
        assertEquals(1, pool.size());
        assertEquals(2, destroyed);

        GenericObjectPool<TestPoolable> created = new GenericObjectPool<>(10, 0, EvictionPolicy.CREATED_TIME, true, 0.5f);
        try {
            TestPoolable c1 = new TestPoolable("p1");
            Thread.sleep(10);
            TestPoolable c2 = new TestPoolable("p2");
            Thread.sleep(10);
            TestPoolable c3 = new TestPoolable("p3");
            created.add(c1);
            created.add(c2);
            created.add(c3);
            created.vacate(2);
            assertEquals(1, created.size());
            assertTrue(created.contains(c3));
            assertTrue(c1.isDestroyed());
            assertTrue(c2.isDestroyed());
        } finally {
            created.close();
        }

        GenericObjectPool<TestPoolable> fifo = new GenericObjectPool<>(10, 0, EvictionPolicy.FIFO, true, 0.5f);
        try {
            TestPoolable older = new TestPoolable("older");
            Thread.sleep(10);
            TestPoolable newer = new TestPoolable("newer");
            fifo.add(newer);
            fifo.add(older);
            fifo.vacate(1);
            assertFalse(fifo.contains(newer));
            assertTrue(fifo.contains(older));
        } finally {
            fifo.close();
        }

        GenericObjectPool<TestPoolable> countPool = new GenericObjectPool<>(10, 0, EvictionPolicy.ACCESS_COUNT, true, 0.5f);
        try {
            for (int i = 0; i < 10; i++) {
                countPool.add(new TestPoolable("p" + i));
            }
            for (int i = 0; i < 5; i++) {
                TestPoolable taken = countPool.poll();
                if (taken != null) {
                    countPool.add(taken);
                }
            }
            countPool.evict();
            assertTrue(countPool.size() < 10);
        } finally {
            countPool.close();
        }

        ObjectPool.MemoryMeasure<TestPoolable> measure = e -> 100;
        GenericObjectPool<TestPoolable> memPool = new GenericObjectPool<>(10, 0, EvictionPolicy.LAST_ACCESS_TIME, true, 0.5f, 1000, measure);
        try {
            for (int i = 0; i < 10; i++) {
                memPool.add(new TestPoolable("p" + i));
            }
            assertEquals(1000, memPool.stats().dataSize());
            memPool.evict();
            assertTrue(memPool.stats().dataSize() < 1000);
        } finally {
            memPool.close();
        }

        GenericObjectPool<TestPoolable> scheduled = new GenericObjectPool<>(10, 100, EvictionPolicy.LAST_ACCESS_TIME);
        try {
            TestPoolable shortLived = new TestPoolable("short", 50, 50);
            TestPoolable longLived = new TestPoolable("long", 10000, 10000);
            scheduled.add(shortLived);
            scheduled.add(longLived);
            Thread.sleep(300);
            assertEquals(1, scheduled.size());
            assertTrue(scheduled.contains(longLived));
            assertTrue(shortLived.isDestroyed());
            assertEquals(Poolable.Caller.EVICT, shortLived.getDestroyedByCaller());
        } finally {
            scheduled.close();
        }

        GenericObjectPool<TestPoolable> empty = new GenericObjectPool<>(5, 0, EvictionPolicy.LAST_ACCESS_TIME);
        try {
            empty.evict();
            assertEquals(0, empty.size());
            empty.close();
            assertThrows(IllegalStateException.class, () -> empty.evict());
        } finally {
            if (!empty.isClosed()) {
                empty.close();
            }
        }
    }

    @Test
    public void testEvict_RemovesBeforeDestroyCallback() {
        GenericObjectPool<TestPoolable> p = new GenericObjectPool<>(1, 0, EvictionPolicy.LAST_ACCESS_TIME);
        AtomicReference<TestPoolable> observed = new AtomicReference<>();
        AtomicReference<Boolean> lockHeld = new AtomicReference<>();
        TestPoolable doomed = new TestPoolable("doomed") {
            @Override
            public void destroy(final Poolable.Caller caller) {
                lockHeld.set(p.lock.isHeldByCurrentThread());
                observed.set(p.poll());
                super.destroy(caller);
            }
        };
        try {
            assertTrue(p.add(doomed));
            p.evict();
            assertNull(observed.get());
            assertEquals(Boolean.FALSE, lockHeld.get());
            assertEquals(0, p.size());
        } finally {
            p.close();
        }
    }

    @Test
    public void testEvict_SingleCriticalSection() {
        AtomicBoolean vacateDispatched = new AtomicBoolean();
        AtomicEvictPool p = new AtomicEvictPool(vacateDispatched);
        try {
            for (int i = 0; i < 4; i++) {
                assertTrue(p.add(new TestPoolable("p" + i)));
            }
            p.evict();
            assertFalse(vacateDispatched.get());
            assertEquals(2, p.size());
        } finally {
            p.close();
        }
    }

    @Test
    public void testClear() {
        List<TestPoolable> items = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            TestPoolable item = new TestPoolable("test" + i);
            items.add(item);
            pool.add(item);
        }
        pool.clear();
        assertEquals(0, pool.size());
        for (TestPoolable item : items) {
            assertTrue(item.isDestroyed());
            assertEquals(Poolable.Caller.REMOVE_REPLACE_CLEAR, item.getDestroyedByCaller());
        }
        pool.clear();
        assertTrue(pool.isEmpty());
        pool.close();
        assertThrows(IllegalStateException.class, () -> pool.clear());
    }

    @Test
    public void testClear_AccountsBeforeUnlock() throws Exception {
        assertRemovalAccountingVisibleBeforeCallback(true);
        assertRemovalAccountingVisibleBeforeCallback(false);
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
        });
        cleanup.start();
        try {
            assertTrue(destructionPhaseReached.await(2, TimeUnit.SECONDS));
            assertTrue(p.add(new TestPoolable("replacement")));
            assertEquals(10L, p.totalDataSize.get());
        } finally {
            releaseDestruction.countDown();
            cleanup.join(2_000);
            if (!p.isClosed()) {
                p.close();
            }
        }
        assertFalse(cleanup.isAlive());
        assertNull(cleanupFailure.get());
        assertEquals(Boolean.FALSE, lockHeldDuringCallback.get());
        assertEquals(0L, p.totalDataSize.get());
    }

    @Test
    public void testConstructor() {
        GenericObjectPool<TestPoolable> basic = new GenericObjectPool<>(50, 3000, EvictionPolicy.ACCESS_COUNT);
        try {
            assertEquals(50, basic.capacity());
            assertEquals(0, basic.size());
            assertFalse(basic.isClosed());
        } finally {
            basic.close();
        }
        ObjectPool.MemoryMeasure<TestPoolable> measure = e -> 100;
        GenericObjectPool<TestPoolable> mem = new GenericObjectPool<>(20, 2000, EvictionPolicy.EXPIRATION_TIME, 1024, measure);
        try {
            assertEquals(20, mem.capacity());
        } finally {
            mem.close();
        }
        GenericObjectPool<TestPoolable> full = new GenericObjectPool<>(40, 4000, EvictionPolicy.ACCESS_COUNT, true, 0.4f, 2048, measure);
        try {
            assertEquals(40, full.capacity());
        } finally {
            full.close();
        }
    }

    @Test
    public void testSerialization() throws Exception {
        GenericObjectPool<TestPoolable> evictPool = new GenericObjectPool<>(10, 100, EvictionPolicy.LAST_ACCESS_TIME);
        GenericObjectPool<TestPoolable> deserialized = deserialize(serialize(evictPool));
        try {
            assertNotNull(deserialized);
            assertFalse(deserialized.isClosed());
            assertNotNull(shutdownHookOf(deserialized));
        } finally {
            evictPool.close();
            deserialized.close();
        }

        TestPoolable first = new TestPoolable("first", 600_000, 60_000);
        TestPoolable second = new TestPoolable("second", 600_000, 60_000);
        second.activityPrint().updateAccessCount();
        second.activityPrint().updateAccessCount();
        assertTrue(pool.add(first));
        assertTrue(pool.add(second));
        GenericObjectPool<TestPoolable> copy = deserialize(serialize(pool));
        try {
            assertEquals(2, copy.size());
            TestPoolable restoredSecond = copy.poll();
            assertEquals("second", restoredSecond.getId());
            assertEquals(3, restoredSecond.activityPrint().getAccessCount());
            assertEquals("first", copy.poll().getId());
        } finally {
            copy.close();
        }

        GenericObjectPool<PoolableAdapter<String>> adapterPool = new GenericObjectPool<>(10, 0, EvictionPolicy.LAST_ACCESS_TIME);
        GenericObjectPool<PoolableAdapter<String>> adapterCopy = null;
        try {
            PoolableAdapter<String> adapter = Poolable.wrap("hello", 600_000, 60_000);
            adapter.activityPrint().updateAccessCount();
            assertTrue(adapterPool.add(adapter));
            adapterCopy = deserialize(serialize(adapterPool));
            PoolableAdapter<String> restored = adapterCopy.poll();
            assertEquals("hello", restored.value());
            assertEquals(2, restored.activityPrint().getAccessCount());
            assertSame(adapter, adapterPool.poll());

            adapterPool.add(Poolable.wrap("汉字 – ñ – 😀"));
            GenericObjectPool<PoolableAdapter<String>> unicode = deserialize(serialize(adapterPool));
            try {
                assertEquals("汉字 – ñ – 😀", unicode.poll().value());
            } finally {
                unicode.close();
            }
        } finally {
            adapterPool.close();
            if (adapterCopy != null) {
                adapterCopy.close();
            }
        }

        GenericObjectPool<PoolableAdapter<Object>> nullPool = new GenericObjectPool<>(10, 0, EvictionPolicy.LAST_ACCESS_TIME);
        try {
            assertTrue(nullPool.add(PoolableAdapter.of(null)));
            GenericObjectPool<PoolableAdapter<Object>> nullCopy = deserialize(serialize(nullPool));
            try {
                assertNull(nullCopy.poll().value());
            } finally {
                nullCopy.close();
            }
            assertTrue(nullPool.add(PoolableAdapter.of(new Object())));
            assertThrows(NotSerializableException.class, () -> serialize(nullPool));
        } finally {
            nullPool.close();
        }

        ObjectPool.MemoryMeasure<PoolableAdapter<String>> measure = (ObjectPool.MemoryMeasure<PoolableAdapter<String>> & Serializable) a -> a.value().length();
        GenericObjectPool<PoolableAdapter<String>> measured = new GenericObjectPool<>(10, 0, EvictionPolicy.LAST_ACCESS_TIME, 1000, measure);
        try {
            assertTrue(measured.add(Poolable.wrap("abc")));
            assertTrue(measured.add(Poolable.wrap("defgh")));
            GenericObjectPool<PoolableAdapter<String>> measuredCopy = deserialize(serialize(measured));
            try {
                assertEquals(8, measuredCopy.stats().dataSize());
                assertEquals("defgh", measuredCopy.poll().value());
                assertEquals(3, measuredCopy.stats().dataSize());
            } finally {
                measuredCopy.close();
            }
        } finally {
            measured.close();
        }

        GenericObjectPool<TestPoolable> lambdaPool = new GenericObjectPool<>(10, 0, EvictionPolicy.LAST_ACCESS_TIME, 1000, e -> 1);
        try {
            assertThrows(NotSerializableException.class, () -> serialize(lambdaPool));
        } finally {
            lambdaPool.close();
        }
    }

    @Test
    public void testDeserializationDefersEviction() throws Exception {
        DeserializationEvictionProbePool original = new DeserializationEvictionProbePool();
        byte[] bytes = serialize(original);
        original.close();
        DeserializationEvictionProbePool.resetProbe();
        DeserializationEvictionProbePool deserialized = deserialize(bytes);
        try {
            assertTrue(DeserializationEvictionProbePool.evictionRan.await(1, TimeUnit.SECONDS));
            assertFalse(DeserializationEvictionProbePool.observedPartiallyDeserializedState.get());
        } finally {
            deserialized.close();
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
        assertThrows(IllegalStateException.class, () -> pool.add(new TestPoolable("p3")));
        pool.close();
        assertTrue(pool.isClosed());
    }

    @Test
    public void testClose_ContinuesAfterDestroyError() {
        TestPoolable survivor = new TestPoolable("survivor");
        TestPoolable broken = new TestPoolable("broken") {
            @Override
            public void destroy(final Poolable.Caller caller) {
                throw new AssertionError("simulated callback failure");
            }
        };
        GenericObjectPool<TestPoolable> p = new GenericObjectPool<>(10, 0, EvictionPolicy.LAST_ACCESS_TIME);
        try {
            assertTrue(p.add(survivor));
            assertTrue(p.add(broken));
            p.close();
            assertTrue(survivor.isDestroyed());
        } finally {
            if (!p.isClosed()) {
                p.close();
            }
        }
    }

    @Test
    public void testConcurrentCloseWaits() throws InterruptedException {
        GenericObjectPool<TestPoolable> p = new GenericObjectPool<>(1, 0, EvictionPolicy.LAST_ACCESS_TIME);
        CountDownLatch destroyStarted = new CountDownLatch(1);
        CountDownLatch allowDestroyToFinish = new CountDownLatch(1);
        CountDownLatch secondCloseReturned = new CountDownLatch(1);
        TestPoolable element = new TestPoolable("blocking") {
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
        Thread first = new Thread(p::close);
        Thread second = new Thread(() -> {
            p.close();
            secondCloseReturned.countDown();
        });
        first.start();
        assertTrue(destroyStarted.await(2, TimeUnit.SECONDS));
        second.start();
        try {
            assertFalse(secondCloseReturned.await(200, TimeUnit.MILLISECONDS));
        } finally {
            allowDestroyToFinish.countDown();
        }
        first.join(2000);
        second.join(2000);
        assertFalse(first.isAlive());
        assertFalse(second.isAlive());
        assertTrue(element.isDestroyed());
    }

    @Test
    public void testSize() {
        assertEquals(0, pool.size());
        assertTrue(pool.isEmpty());
        assertEquals(10, pool.capacity());
        pool.add(new TestPoolable("p1"));
        assertEquals(1, pool.size());
        assertFalse(pool.isEmpty());
        pool.poll();
        assertEquals(0, pool.size());
        pool.add(new TestPoolable("p1"));
        pool.close();
        assertThrows(IllegalStateException.class, () -> pool.size());
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
        assertFalse(pool.isMemoryTracked());
        assertEquals(-1, stats.maxMemory());
        assertEquals(-1, stats.dataSize());

        ObjectPool.MemoryMeasure<TestPoolable> measure = e -> 100;
        GenericObjectPool<TestPoolable> unlimited = new GenericObjectPool<>(10, 0, EvictionPolicy.LAST_ACCESS_TIME, 0, measure);
        try {
            assertTrue(unlimited.isMemoryTracked());
            assertEquals(-1, unlimited.stats().maxMemory());
            assertTrue(unlimited.add(new TestPoolable("a")));
            assertTrue(unlimited.add(new TestPoolable("b")));
            assertEquals(200, unlimited.stats().dataSize());
            unlimited.poll();
            assertEquals(100, unlimited.stats().dataSize());
            unlimited.clear();
            assertEquals(0, unlimited.stats().dataSize());
        } finally {
            unlimited.close();
        }

        GenericObjectPool<TestPoolable> limited = new GenericObjectPool<>(10, 0, EvictionPolicy.LAST_ACCESS_TIME, 1000, measure);
        try {
            limited.add(new TestPoolable("a"));
            limited.add(new TestPoolable("b"));
            assertEquals(1000, limited.stats().maxMemory());
            assertEquals(200, limited.stats().dataSize());
        } finally {
            limited.close();
        }
    }

    @Test
    public void testLockAndUnlock() {
        pool.lock();
        pool.unlock();
        assertEquals(0, pool.size());
    }

    @Test
    public void testEqualsHashCodeToString() {
        pool.add(new TestPoolable("p1"));
        assertEquals(pool.hashCode(), pool.hashCode());
        assertTrue(pool.toString().contains("GenericObjectPool"));
        assertTrue(pool.toString().contains("capacity=10"));

        GenericObjectPool<TestPoolable> pool1 = new GenericObjectPool<>(10, 0, EvictionPolicy.LAST_ACCESS_TIME);
        GenericObjectPool<TestPoolable> pool2 = new GenericObjectPool<>(10, 0, EvictionPolicy.LAST_ACCESS_TIME);
        try {
            assertTrue(pool1.equals(pool1));
            assertTrue(pool1.equals(pool2));
            assertEquals(pool1.hashCode(), pool2.hashCode());
            TestPoolable a = new TestPoolable("p1");
            TestPoolable b = new TestPoolable("p2");
            pool1.add(a);
            assertFalse(pool1.equals(pool2));
            pool2.add(a);
            assertTrue(pool1.equals(pool2));
            pool2.poll();
            pool2.add(b);
            pool2.add(a);
            assertFalse(pool1.equals(pool2));
            assertFalse(pool1.equals(null));
            assertFalse(pool1.equals("not a pool"));
        } finally {
            pool1.close();
            pool2.close();
        }
    }

    @Test
    public void testVacate_DuplicateInstance() {
        GenericObjectPool<TestPoolable> p = new GenericObjectPool<>(10, 0, EvictionPolicy.ACCESS_COUNT, false, 0.5f);
        try {
            TestPoolable dup = new TestPoolable("dup");
            TestPoolable keeper = new TestPoolable("keeper");
            keeper.activityPrint().updateAccessCount();
            keeper.activityPrint().updateAccessCount();
            assertTrue(p.add(dup));
            assertTrue(p.add(dup));
            assertTrue(p.add(dup));
            assertTrue(p.add(keeper));
            p.vacate(2);
            assertEquals(2, p.size());
            assertEquals(2, p.stats().evictionCount());
            assertTrue(dup.isDestroyed());
            assertEquals(Poolable.Caller.VACATE, dup.getDestroyedByCaller());
            assertSame(keeper, p.poll());
            assertSame(dup, p.poll());
        } finally {
            p.close();
        }

        ObjectPool.MemoryMeasure<TestPoolable> measure = e -> 10;
        GenericObjectPool<TestPoolable> mem = new GenericObjectPool<>(10, 0, EvictionPolicy.ACCESS_COUNT, false, 0.5f, 1000, measure);
        try {
            TestPoolable dup = new TestPoolable("dup");
            TestPoolable keeper = new TestPoolable("keeper");
            keeper.activityPrint().updateAccessCount();
            mem.add(dup);
            mem.add(dup);
            mem.add(dup);
            mem.add(keeper);
            mem.vacate(2);
            assertEquals(20, mem.stats().dataSize());
            assertSame(keeper, mem.poll());
            assertSame(dup, mem.poll());
            assertEquals(0, mem.stats().dataSize());
        } finally {
            mem.close();
        }
    }

    @Test
    public void testRemoveByIdentity() throws Exception {
        TestPoolable a = new TestPoolable("a");
        TestPoolable b = new TestPoolable("b");
        pool.add(a);
        pool.add(b);
        invokeRemoveByIdentity(pool, Collections.emptyList());
        invokeRemoveByIdentity(pool, null);
        invokeRemoveByIdentity(pool, List.of(new TestPoolable("zzz"), new TestPoolable("a")));
        assertEquals(2, pool.size());
        invokeRemoveByIdentity(pool, List.of(new TestPoolable("nope"), a));
        assertEquals(1, pool.size());
        assertTrue(pool.contains(b));

        TestPoolable dup = new TestPoolable("dup");
        TestPoolable other = new TestPoolable("other");
        pool.clear();
        pool.add(dup);
        pool.add(other);
        pool.add(dup);
        invokeRemoveByIdentity(pool, List.of(dup, dup, dup));
        assertEquals(1, pool.size());
        assertSame(other, pool.poll());
    }

    @Test
    public void testRemoveExpired_NotQuadratic() throws Exception {
        int expired = 80_000;
        int live = 80_000;
        GenericObjectPool<TestPoolable> big = new GenericObjectPool<>(expired + live, 0, EvictionPolicy.LAST_ACCESS_TIME, false, 0.2f);
        try {
            for (int i = 0; i < expired; i++) {
                assertTrue(big.add(new TestPoolable("e" + i, 250, 250)));
            }
            Thread.sleep(400);
            for (int i = 0; i < live; i++) {
                assertTrue(big.add(new TestPoolable("l" + i, 600_000, 600_000)));
            }
            long start = System.nanoTime();
            big.removeExpired();
            assertEquals(live, big.size());
            assertEquals(expired, big.stats().evictionCount());
            assertTrue((System.nanoTime() - start) / 1_000_000 < 2_000);
        } finally {
            big.close();
        }
    }

    @Test
    public void testTimedAdd_SpuriousWakeups() throws Exception {
        GenericObjectPool<TestPoolable> p = noBalance(1);
        assertTrue(p.add(new TestPoolable("existing")));
        SpuriousWakeupCondition condition = new SpuriousWakeupCondition(3);
        p.notFull = condition;
        try {
            assertFalse(p.add(new TestPoolable("new"), 1, TimeUnit.SECONDS));
            assertEquals(4, condition.awaitCount);
        } finally {
            p.close();
        }
    }

    @Test
    public void testAutoBalanceDestroyCallback() {
        GenericObjectPool<TestPoolable> p = new GenericObjectPool<>(1, 0, EvictionPolicy.LAST_ACCESS_TIME);
        AtomicReference<Boolean> lockHeld = new AtomicReference<>();
        AtomicReference<Boolean> victimPresent = new AtomicReference<>();
        AtomicReference<Boolean> replacementPresent = new AtomicReference<>();
        TestPoolable replacement = new TestPoolable("replacement");
        TestPoolable victim = new TestPoolable("victim") {
            @Override
            public void destroy(final Poolable.Caller caller) {
                lockHeld.set(p.lock.isHeldByCurrentThread());
                victimPresent.set(p.contains(this));
                replacementPresent.set(p.contains(replacement));
                super.destroy(caller);
            }
        };
        try {
            assertTrue(p.add(victim));
            assertTrue(p.add(replacement));
            assertEquals(Boolean.FALSE, lockHeld.get());
            assertEquals(Boolean.FALSE, victimPresent.get());
            assertEquals(Boolean.TRUE, replacementPresent.get());
            assertEquals(Poolable.Caller.VACATE, victim.getDestroyedByCaller());
        } finally {
            p.close();
        }
    }
}

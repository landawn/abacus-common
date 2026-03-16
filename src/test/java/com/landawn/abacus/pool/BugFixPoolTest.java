package com.landawn.abacus.pool;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Tests verifying pool-related bug fixes (thread safety of size/hashCode/equals, evict Map.Entry fix).
 */
@Tag("new-test")
public class BugFixPoolTest {

    // ============================================================
    // Fix: GenericObjectPool size/hashCode/equals thread safety
    // ============================================================

    @Test
    public void testObjectPool_sizeIsThreadSafe() throws InterruptedException {
        final ObjectPool<PoolableAdapter<String>> pool = PoolFactory.createObjectPool(1000, 0);
        final int numThreads = 10;
        final int itemsPerThread = 50;
        final CountDownLatch startLatch = new CountDownLatch(1);
        final CountDownLatch doneLatch = new CountDownLatch(numThreads);
        final AtomicInteger errorCount = new AtomicInteger(0);

        for (int t = 0; t < numThreads; t++) {
            final int threadId = t;
            new Thread(() -> {
                try {
                    startLatch.await();
                    for (int i = 0; i < itemsPerThread; i++) {
                        pool.add(PoolableAdapter.of("item-" + threadId + "-" + i));
                        // Concurrently call size() - should not throw
                        int s = pool.size();
                        if (s < 0) {
                            errorCount.incrementAndGet();
                        }
                    }
                } catch (Exception e) {
                    errorCount.incrementAndGet();
                } finally {
                    doneLatch.countDown();
                }
            }).start();
        }

        startLatch.countDown();
        doneLatch.await();

        assertEquals(0, errorCount.get(), "size() should be thread-safe");
        assertTrue(pool.size() > 0);
        pool.close();
    }

    @Test
    public void testObjectPool_hashCodeIsThreadSafe() throws InterruptedException {
        final ObjectPool<PoolableAdapter<String>> pool = PoolFactory.createObjectPool(100, 0);
        for (int i = 0; i < 10; i++) {
            pool.add(PoolableAdapter.of("item-" + i));
        }

        final int numThreads = 10;
        final int iterations = 100;
        final CountDownLatch startLatch = new CountDownLatch(1);
        final CountDownLatch doneLatch = new CountDownLatch(numThreads);
        final AtomicInteger errorCount = new AtomicInteger(0);

        for (int t = 0; t < numThreads; t++) {
            new Thread(() -> {
                try {
                    startLatch.await();
                    for (int i = 0; i < iterations; i++) {
                        // hashCode() should not throw ConcurrentModificationException
                        pool.hashCode();
                    }
                } catch (Exception e) {
                    errorCount.incrementAndGet();
                } finally {
                    doneLatch.countDown();
                }
            }).start();
        }

        startLatch.countDown();
        doneLatch.await();

        assertEquals(0, errorCount.get(), "hashCode() should be thread-safe");
        pool.close();
    }

    @Test
    public void testObjectPool_equalsIsThreadSafe() throws InterruptedException {
        final ObjectPool<PoolableAdapter<String>> pool1 = PoolFactory.createObjectPool(100, 0);
        final ObjectPool<PoolableAdapter<String>> pool2 = PoolFactory.createObjectPool(100, 0);
        for (int i = 0; i < 10; i++) {
            pool1.add(PoolableAdapter.of("item-" + i));
            pool2.add(PoolableAdapter.of("item-" + i));
        }

        final int numThreads = 10;
        final int iterations = 100;
        final CountDownLatch startLatch = new CountDownLatch(1);
        final CountDownLatch doneLatch = new CountDownLatch(numThreads);
        final AtomicInteger errorCount = new AtomicInteger(0);

        for (int t = 0; t < numThreads; t++) {
            new Thread(() -> {
                try {
                    startLatch.await();
                    for (int i = 0; i < iterations; i++) {
                        // equals() should not throw ConcurrentModificationException
                        pool1.equals(pool2);
                    }
                } catch (Exception e) {
                    errorCount.incrementAndGet();
                } finally {
                    doneLatch.countDown();
                }
            }).start();
        }

        startLatch.countDown();
        doneLatch.await();

        assertEquals(0, errorCount.get(), "equals() should be thread-safe");
        pool1.close();
        pool2.close();
    }

    @Test
    public void testObjectPool_sizeBasicCorrectness() {
        final ObjectPool<PoolableAdapter<String>> pool = PoolFactory.createObjectPool(10, 0);
        assertEquals(0, pool.size());

        pool.add(PoolableAdapter.of("a"));
        assertEquals(1, pool.size());

        pool.add(PoolableAdapter.of("b"));
        assertEquals(2, pool.size());

        pool.take();
        assertEquals(1, pool.size());

        pool.close();
    }

    // ============================================================
    // Fix: GenericKeyedObjectPool size/hashCode/equals thread safety
    // ============================================================

    @Test
    public void testKeyedObjectPool_sizeIsThreadSafe() throws InterruptedException {
        final KeyedObjectPool<String, PoolableAdapter<String>> pool = PoolFactory.createKeyedObjectPool(1000, 0);
        final int numThreads = 10;
        final int itemsPerThread = 50;
        final CountDownLatch startLatch = new CountDownLatch(1);
        final CountDownLatch doneLatch = new CountDownLatch(numThreads);
        final AtomicInteger errorCount = new AtomicInteger(0);

        for (int t = 0; t < numThreads; t++) {
            final int threadId = t;
            new Thread(() -> {
                try {
                    startLatch.await();
                    for (int i = 0; i < itemsPerThread; i++) {
                        String key = "key-" + threadId + "-" + i;
                        pool.put(key, PoolableAdapter.of("val-" + threadId + "-" + i));
                        // Concurrently call size() - should not throw
                        int s = pool.size();
                        if (s < 0) {
                            errorCount.incrementAndGet();
                        }
                    }
                } catch (Exception e) {
                    errorCount.incrementAndGet();
                } finally {
                    doneLatch.countDown();
                }
            }).start();
        }

        startLatch.countDown();
        doneLatch.await();

        assertEquals(0, errorCount.get(), "size() should be thread-safe on keyed pool");
        assertTrue(pool.size() > 0);
        pool.close();
    }

    @Test
    public void testKeyedObjectPool_sizeBasicCorrectness() {
        final KeyedObjectPool<String, PoolableAdapter<String>> pool = PoolFactory.createKeyedObjectPool(10, 0);
        assertEquals(0, pool.size());

        pool.put("k1", PoolableAdapter.of("v1"));
        assertEquals(1, pool.size());

        pool.put("k2", PoolableAdapter.of("v2"));
        assertEquals(2, pool.size());

        pool.remove("k1");
        assertEquals(1, pool.size());

        pool.close();
    }

    // ============================================================
    // Fix: GenericKeyedObjectPool evict() Map.Entry access after remove
    // ============================================================

    @Test
    public void testKeyedObjectPool_evictDoesNotCorruptData() throws InterruptedException {
        // Create pool with auto-balance and a small evict delay
        final KeyedObjectPool<String, PoolableAdapter<String>> pool = PoolFactory.createKeyedObjectPool(100, 0, EvictionPolicy.LAST_ACCESS_TIME, true, 0.5f);

        // Add items
        for (int i = 0; i < 20; i++) {
            pool.put("key-" + i, PoolableAdapter.of("value-" + i));
        }

        assertEquals(20, pool.size());

        // Manually trigger eviction - should not throw due to Map.Entry corruption
        pool.evict();

        // After eviction with balanceFactor 0.5, roughly half should be evicted
        assertTrue(pool.size() < 20, "Some items should have been evicted");
        assertTrue(pool.size() > 0, "Not all items should be evicted");

        pool.close();
    }

    @Test
    public void testKeyedObjectPool_evictWithConcurrentAccess() throws InterruptedException {
        final KeyedObjectPool<String, PoolableAdapter<String>> pool = PoolFactory.createKeyedObjectPool(1000, 0, EvictionPolicy.LAST_ACCESS_TIME, true, 0.3f);

        // Add items
        for (int i = 0; i < 100; i++) {
            pool.put("key-" + i, PoolableAdapter.of("value-" + i));
        }

        final int numThreads = 5;
        final CountDownLatch startLatch = new CountDownLatch(1);
        final CountDownLatch doneLatch = new CountDownLatch(numThreads + 1);
        final AtomicInteger errorCount = new AtomicInteger(0);

        // Eviction thread
        new Thread(() -> {
            try {
                startLatch.await();
                for (int i = 0; i < 10; i++) {
                    pool.evict();
                }
            } catch (Exception e) {
                errorCount.incrementAndGet();
            } finally {
                doneLatch.countDown();
            }
        }).start();

        // Reader threads
        for (int t = 0; t < numThreads; t++) {
            new Thread(() -> {
                try {
                    startLatch.await();
                    for (int i = 0; i < 100; i++) {
                        pool.get("key-" + (i % 100));
                        pool.size();
                    }
                } catch (Exception e) {
                    errorCount.incrementAndGet();
                } finally {
                    doneLatch.countDown();
                }
            }).start();
        }

        startLatch.countDown();
        doneLatch.await();

        assertEquals(0, errorCount.get(), "Concurrent evict + access should not throw");
        pool.close();
    }
}

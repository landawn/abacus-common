package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

public class ConcurrentCacheMapTest extends TestBase {

    @Test
    public void testConstructor() {
        ConcurrentCacheMap<String, Integer> cache = new ConcurrentCacheMap<>(16);
        assertTrue(cache.isEmpty());
        assertEquals(0, cache.size());

        ConcurrentCacheMap<String, Integer> zero = new ConcurrentCacheMap<>(0);
        zero.put("key", 1);
        assertEquals(1, zero.get("key"));
        assertThrows(IllegalArgumentException.class, () -> new ConcurrentCacheMap<>(-1));
    }

    @Test
    public void testGetAndPut() {
        ConcurrentCacheMap<String, Integer> cache = new ConcurrentCacheMap<>(10);
        assertNull(cache.get("two"));
        assertNull(cache.get(null));
        assertNull(cache.put("key", 100));
        assertEquals(100, cache.get("key"));
        assertEquals(100, cache.put("key", 200));
        assertEquals(200, cache.get("key"));

        ConcurrentCacheMap<String, Integer> collisions = new ConcurrentCacheMap<>(4);
        collisions.put("key1", 1);
        collisions.put("key2", 2);
        collisions.put("key3", 3);
        collisions.put("key4", 4);
        assertEquals(1, collisions.get("key1"));
        assertEquals(2, collisions.get("key2"));
        assertEquals(3, collisions.get("key3"));
        assertEquals(4, collisions.get("key4"));

        assertThrows(NullPointerException.class, () -> cache.put(null, 100));
        assertThrows(NullPointerException.class, () -> cache.put("key", null));
        assertThrows(NullPointerException.class, () -> cache.putIfAbsent(null, 100));

        Map<String, Integer> map = new HashMap<>();
        map.put("one", 1);
        map.put("two", 2);
        map.put("three", 3);
        cache.putAll(map);
        assertEquals(1, cache.get("one"));
        assertEquals(2, cache.get("two"));
        assertEquals(3, cache.get("three"));

        Map<String, Integer> withNull = new HashMap<>();
        withNull.put("one", 1);
        withNull.put("two", null);
        withNull.put("three", 3);
        assertThrows(NullPointerException.class, () -> cache.putAll(withNull));
    }

    @Test
    public void testPutIfAbsent() {
        ConcurrentCacheMap<String, Integer> cache = new ConcurrentCacheMap<>(10);
        assertNull(cache.putIfAbsent("key", 100));
        assertEquals(100, cache.get("key"));
        assertEquals(100, cache.putIfAbsent("key", 200));
        assertEquals(100, cache.get("key"));
    }

    @Test
    public void testPutIfAbsentIsAtomicUnderConcurrency() throws InterruptedException {
        final int threadCount = 16;
        ConcurrentCacheMap<String, Integer> cache = new ConcurrentCacheMap<>(16);
        AtomicInteger winners = new AtomicInteger();
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(threadCount);
        Thread[] threads = new Thread[threadCount];
        for (int i = 0; i < threadCount; i++) {
            final int value = i;
            threads[i] = new Thread(() -> {
                try {
                    start.await();
                    if (cache.putIfAbsent("race", value) == null) {
                        winners.incrementAndGet();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    done.countDown();
                }
            });
            threads[i].start();
        }
        start.countDown();
        done.await();
        assertEquals(1, winners.get());
        assertTrue(cache.containsKey("race"));
    }

    @Test
    public void testRemove() {
        ConcurrentCacheMap<String, Integer> cache = new ConcurrentCacheMap<>(10);
        cache.put("key", 100);
        assertEquals(100, cache.remove("key"));
        assertNull(cache.get("key"));
        assertEquals(0, cache.size());
        assertNull(cache.remove("key"));
        assertNull(new ConcurrentCacheMap<String, Integer>(10).remove("nonexistent"));

        assertFalse(cache.remove("key", 200));
        cache.put("key", 100);
        assertFalse(cache.remove("key", 200));
        assertEquals(100, cache.get("key"));
        assertTrue(cache.remove("key", 100));
        assertFalse(cache.containsKey("key"));
        assertFalse(cache.remove(null, 100));
        assertFalse(cache.remove("key", null));
        assertNull(cache.get(null));
        assertFalse(cache.containsKey(null));
        assertNull(cache.remove(null));

        ConcurrentCacheMap<String, Integer> collisions = new ConcurrentCacheMap<>(4);
        collisions.put("key1", 1);
        collisions.put("key2", 2);
        collisions.put("key3", 3);
        assertEquals(2, collisions.remove("key2"));
        assertEquals(1, collisions.get("key1"));
        assertNull(collisions.get("key2"));
        assertEquals(3, collisions.get("key3"));

        ConcurrentCacheMap<String, Integer> chain = new ConcurrentCacheMap<>(4);
        chain.put("a", 1);
        chain.put("b", 2);
        chain.put("c", 3);
        chain.put("d", 4);
        chain.put("e", 5);
        chain.remove("c");
        assertEquals(1, chain.get("a"));
        assertEquals(2, chain.get("b"));
        assertNull(chain.get("c"));
        assertEquals(4, chain.get("d"));
        assertEquals(5, chain.get("e"));
    }

    @Test
    public void testConditionalRemoveDoesNotDeleteConcurrentReplacement() throws Exception {
        CountDownLatch comparisonStarted = new CountDownLatch(1);
        CountDownLatch releaseComparison = new CountDownLatch(1);

        final class BlockingValue {
            private final String id;

            BlockingValue(final String id) {
                this.id = id;
            }

            @Override
            public boolean equals(final Object obj) {
                comparisonStarted.countDown();
                try {
                    releaseComparison.await();
                } catch (final InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new AssertionError(e);
                }
                return obj instanceof BlockingValue other && id.equals(other.id);
            }

            @Override
            public int hashCode() {
                return id.hashCode();
            }
        }

        ConcurrentCacheMap<String, BlockingValue> cache = new ConcurrentCacheMap<>(2);
        BlockingValue original = new BlockingValue("original");
        BlockingValue expected = new BlockingValue("original");
        BlockingValue replacement = new BlockingValue("replacement");
        AtomicBoolean removed = new AtomicBoolean();
        AtomicReference<Throwable> failure = new AtomicReference<>();
        CountDownLatch replacementCompleted = new CountDownLatch(1);
        cache.put("key", original);

        Thread remover = new Thread(() -> {
            try {
                removed.set(cache.remove("key", expected));
            } catch (final Throwable e) {
                failure.compareAndSet(null, e);
            }
        });
        remover.setDaemon(true);
        remover.start();
        assertTrue(comparisonStarted.await(5, TimeUnit.SECONDS));

        Thread replacer = new Thread(() -> {
            try {
                cache.put("key", replacement);
            } catch (final Throwable e) {
                failure.compareAndSet(null, e);
            } finally {
                replacementCompleted.countDown();
            }
        });
        replacer.setDaemon(true);
        replacer.start();

        boolean replacementAttemptObserved = false;
        try {
            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
            while (System.nanoTime() < deadline) {
                if (replacementCompleted.getCount() == 0 || replacer.getState() == Thread.State.BLOCKED) {
                    replacementAttemptObserved = true;
                    break;
                }
                Thread.yield();
            }
        } finally {
            releaseComparison.countDown();
        }

        remover.join(TimeUnit.SECONDS.toMillis(5));
        replacer.join(TimeUnit.SECONDS.toMillis(5));
        assertTrue(replacementAttemptObserved, "replacement thread did not reach the conditional removal");
        assertFalse(remover.isAlive());
        assertFalse(replacer.isAlive());
        assertNull(failure.get());
        assertTrue(removed.get());
        assertSame(replacement, cache.get("key"));
    }

    @Test
    public void testContainsSizeClear() {
        ConcurrentCacheMap<String, Integer> cache = new ConcurrentCacheMap<>(10);
        assertTrue(cache.isEmpty());
        assertEquals(0, cache.size());
        cache.put("key1", 100);
        cache.put("key2", 200);
        assertTrue(cache.containsKey("key1"));
        assertFalse(cache.containsKey("nonexistent"));
        assertFalse(cache.containsKey(null));
        assertTrue(cache.containsValue(100));
        assertTrue(cache.containsValue(200));
        assertFalse(cache.containsValue(300));
        assertFalse(cache.containsValue(null));
        assertFalse(cache.isEmpty());
        assertEquals(2, cache.size());
        cache.remove("key1");
        assertEquals(1, cache.size());
        cache.clear();
        assertEquals(0, cache.size());
        assertTrue(cache.isEmpty());
        assertNull(cache.get("key2"));
        cache.put("newkey", 200);
        assertEquals(200, cache.get("newkey"));
        assertEquals(1, cache.size());

        ConcurrentCacheMap<String, Integer> grow = new ConcurrentCacheMap<>(2);
        grow.put("one", 1);
        grow.put("two", 2);
        grow.put("three", 3);
        assertEquals(3, grow.size());
        assertEquals(1, grow.get("one"));

        ConcurrentCacheMap<String, Integer> sameHash = new ConcurrentCacheMap<>(4);
        for (int i = 0; i < 10; i++) {
            sameHash.put("key" + i, i);
        }
        for (int i = 0; i < 10; i++) {
            assertEquals(i, sameHash.get("key" + i));
        }
        assertEquals(10, sameHash.size());
    }

    @Test
    public void testViews() {
        ConcurrentCacheMap<String, Integer> cache = new ConcurrentCacheMap<>(10);
        cache.put("one", 1);
        cache.put("two", 2);
        cache.put("three", 3);

        Set<String> keys = cache.keySet();
        assertEquals(3, keys.size());
        assertTrue(keys.contains("one") && keys.contains("two") && keys.contains("three"));
        cache.put("key3", 3);
        assertEquals(4, keys.size());
        assertTrue(keys.contains("key3"));
        cache.remove("one");
        assertFalse(keys.contains("one"));
        assertTrue(keys.remove("two"));
        assertNull(cache.get("two"));
        assertThrows(UnsupportedOperationException.class, () -> keys.add("new"));

        ConcurrentCacheMap<String, Integer> valuesCache = new ConcurrentCacheMap<>(10);
        valuesCache.put("one", 1);
        valuesCache.put("two", 2);
        valuesCache.put("three", 3);
        Collection<Integer> values = valuesCache.values();
        assertEquals(3, values.size());
        assertTrue(values.contains(1) && values.contains(2) && values.contains(3));
        assertTrue(values.remove(1));
        assertNull(valuesCache.get("one"));
        assertThrows(UnsupportedOperationException.class, () -> values.add(200));

        ConcurrentCacheMap<String, Integer> entries = new ConcurrentCacheMap<>(10);
        entries.put("one", 1);
        entries.put("two", 2);
        Set<Map.Entry<String, Integer>> entrySet = entries.entrySet();
        assertEquals(2, entrySet.size());
        for (Map.Entry<String, Integer> entry : entrySet) {
            if ("one".equals(entry.getKey())) {
                assertEquals(1, entry.getValue());
            } else if ("two".equals(entry.getKey())) {
                assertEquals(2, entry.getValue());
            } else {
                throw new AssertionError("Unexpected entry: " + entry);
            }
        }
        entrySet.clear();
        assertTrue(entries.isEmpty());
    }

    @Test
    public void testEntry() {
        ConcurrentCacheMap<String, Integer> cache = new ConcurrentCacheMap<>(10);
        cache.put("key", 100);
        Map.Entry<String, Integer> entry = cache.entrySet().iterator().next();
        assertEquals("key", entry.getKey());
        assertEquals(100, entry.getValue());
        assertEquals(entry.hashCode(), entry.hashCode());
        assertEquals(100, entry.setValue(200));
        assertEquals(200, entry.getValue());
        assertEquals(200, cache.get("key"));
        assertTrue(entry.toString().contains("key") && entry.toString().contains("200") && entry.toString().contains("="));

        ConcurrentCacheMap<String, Integer> other = new ConcurrentCacheMap<>(10);
        other.put("key", 200);
        Map.Entry<String, Integer> entry2 = other.entrySet().iterator().next();
        assertEquals(entry, entry2);
        assertEquals(entry, entry);
        assertNotEquals(entry, null);
        assertNotEquals(entry, "not an entry");
    }

    @Test
    public void testEqualsHashCodeToString() {
        ConcurrentCacheMap<String, Integer> cache1 = new ConcurrentCacheMap<>(10);
        cache1.put("a", 1);
        cache1.put("b", 2);
        ConcurrentCacheMap<String, Integer> cache2 = new ConcurrentCacheMap<>(10);
        cache2.put("a", 1);
        cache2.put("b", 2);
        assertEquals(cache1, cache2);
        assertEquals(cache1, cache1);
        assertEquals(cache1.hashCode(), cache2.hashCode());

        ConcurrentCacheMap<String, Integer> cache3 = new ConcurrentCacheMap<>(10);
        cache3.put("a", 1);
        cache3.put("b", 3);
        assertNotEquals(cache1, cache3);
        assertNotEquals(cache1, null);
        assertNotEquals(cache1, "not a map");
        assertFalse(cache1.equals(null));

        Map<String, Integer> map = new HashMap<>();
        map.put("a", 1);
        map.put("b", 2);
        assertEquals(cache1, map);
        assertEquals(new ConcurrentCacheMap<>(10), new ConcurrentCacheMap<>(5));
        assertEquals(new ConcurrentCacheMap<String, Integer>(10).hashCode(), new ConcurrentCacheMap<String, Integer>(10).hashCode());

        ConcurrentCacheMap<String, Integer> named = new ConcurrentCacheMap<>(10);
        named.put("key", 100);
        assertTrue(named.toString().contains("key") && named.toString().contains("100"));
        assertEquals("{}", new ConcurrentCacheMap<String, Integer>(10).toString());
    }

    @Test
    public void testConcurrentAccess() throws InterruptedException {
        ConcurrentCacheMap<String, Integer> cache = new ConcurrentCacheMap<>(100);
        Thread t1 = new Thread(() -> {
            for (int i = 0; i < 50; i++) {
                cache.put("key" + i, i);
            }
        });
        Thread t2 = new Thread(() -> {
            for (int i = 50; i < 100; i++) {
                cache.put("key" + i, i);
            }
        });
        t1.start();
        t2.start();
        t1.join();
        t2.join();
        assertEquals(100, cache.size());
        for (int i = 0; i < 100; i++) {
            assertEquals(i, cache.get("key" + i));
        }
    }

    @Test
    public void testConcurrentReadAndWrite_NoCorruption() throws InterruptedException {
        ConcurrentCacheMap<Integer, Integer> cache = new ConcurrentCacheMap<>(256);
        int writers = 4;
        int writesPerThread = 200;
        Thread[] writerThreads = new Thread[writers];
        for (int t = 0; t < writers; t++) {
            final int offset = t * writesPerThread;
            writerThreads[t] = new Thread(() -> {
                for (int i = 0; i < writesPerThread; i++) {
                    cache.put(offset + i, (offset + i) * 2);
                }
            });
        }
        Thread reader = new Thread(() -> {
            for (int i = 0; i < 1000; i++) {
                cache.size();
                cache.containsKey(i);
                cache.get(i);
            }
        });
        for (Thread w : writerThreads) {
            w.start();
        }
        reader.start();
        for (Thread w : writerThreads) {
            w.join();
        }
        reader.join();
        assertEquals(writers * writesPerThread, cache.size());
        for (int t = 0; t < writers; t++) {
            for (int i = 0; i < writesPerThread; i++) {
                int key = t * writesPerThread + i;
                assertEquals(key * 2, cache.get(key));
            }
        }
    }

    @Test
    public void testConcurrentMapCompoundOperations() {
        ConcurrentCacheMap<String, Integer> cache = new ConcurrentCacheMap<>(8);
        assertTrue(cache instanceof ConcurrentMap);
        assertEquals(1, cache.computeIfAbsent("key", key -> 1));
        assertEquals(2, cache.computeIfPresent("key", (key, value) -> value + 1));
        assertEquals(4, cache.compute("key", (key, value) -> value * 2));
        assertEquals(7, cache.merge("key", 3, Integer::sum));
        assertTrue(cache.replace("key", 7, 8));
        assertEquals(8, cache.replace("key", 9));
        cache.replaceAll((key, value) -> value + 1);
        assertEquals(10, cache.get("key"));
    }

    @Test
    public void testComputeIfAbsentIsAtomicUnderConcurrency() throws InterruptedException {
        final int threadCount = 16;
        ConcurrentCacheMap<String, Object> cache = new ConcurrentCacheMap<>(8);
        AtomicInteger invocationCount = new AtomicInteger();
        CountDownLatch ready = new CountDownLatch(threadCount);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch mappingEntered = new CountDownLatch(1);
        CountDownLatch releaseMapping = new CountDownLatch(1);
        Thread[] threads = new Thread[threadCount];
        for (int i = 0; i < threadCount; i++) {
            threads[i] = new Thread(() -> {
                ready.countDown();
                try {
                    start.await();
                    cache.computeIfAbsent("key", key -> {
                        invocationCount.incrementAndGet();
                        mappingEntered.countDown();
                        try {
                            releaseMapping.await();
                        } catch (final InterruptedException e) {
                            Thread.currentThread().interrupt();
                            throw new AssertionError(e);
                        }
                        return new Object();
                    });
                } catch (final InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new AssertionError(e);
                }
            });
            threads[i].start();
        }
        assertTrue(ready.await(5, TimeUnit.SECONDS));
        start.countDown();
        assertTrue(mappingEntered.await(5, TimeUnit.SECONDS));
        releaseMapping.countDown();
        for (Thread thread : threads) {
            thread.join(TimeUnit.SECONDS.toMillis(5));
            assertFalse(thread.isAlive());
        }
        assertEquals(1, invocationCount.get());
    }

    @Test
    public void testConditionalReplaceIsNotNullTolerant() {
        ConcurrentCacheMap<String, Integer> cache = new ConcurrentCacheMap<>(8);
        cache.put("k", 1);

        assertTrue(cache.replace("k", 1, 2));
        assertEquals(2, cache.get("k"));
        assertFalse(cache.replace("k", 99, 3));
        assertEquals(2, cache.get("k"));
        assertFalse(cache.replace("absent", 1, 2));

        assertEquals(2, cache.replace("k", 7));
        assertEquals(7, cache.get("k"));
        assertNull(cache.replace("absent", 9));
        assertFalse(cache.containsKey("absent"));

        // remove(Object, Object) is the only conditional method that tolerates null; replace does not.
        assertFalse(cache.remove(null, null));
        assertThrows(NullPointerException.class, () -> cache.replace(null, 1, 2));
        assertThrows(NullPointerException.class, () -> cache.replace("k", null, 2));
        assertThrows(NullPointerException.class, () -> cache.replace("k", 7, null));
        assertThrows(NullPointerException.class, () -> cache.replace(null, 2));
        assertThrows(NullPointerException.class, () -> cache.replace("k", null));
        assertEquals(1, cache.size());
    }

    @Test
    public void testCompoundUpdateNullContract() {
        ConcurrentCacheMap<String, Integer> cache = new ConcurrentCacheMap<>(8);
        cache.put("k", 1);

        // A remapping function returning null records no mapping / removes the existing one.
        assertNull(cache.computeIfAbsent("absent", key -> null));
        assertFalse(cache.containsKey("absent"));
        assertEquals(1, cache.computeIfAbsent("k", key -> 100));
        assertNull(cache.computeIfPresent("nope", (key, value) -> 5));
        assertNull(cache.computeIfPresent("k", (key, value) -> null));
        assertFalse(cache.containsKey("k"));
        assertEquals(42, cache.compute("k", (key, value) -> value == null ? 42 : 0));
        assertNull(cache.compute("k", (key, value) -> null));
        assertFalse(cache.containsKey("k"));
        assertEquals(5, cache.merge("m", 5, Integer::sum));
        assertEquals(10, cache.merge("m", 5, Integer::sum));
        assertNull(cache.merge("m", 5, (a, b) -> null));
        assertFalse(cache.containsKey("m"));
        assertEquals(0, cache.size());

        cache.put("k", 1);
        assertThrows(NullPointerException.class, () -> cache.computeIfAbsent(null, key -> 1));
        assertThrows(NullPointerException.class, () -> cache.computeIfAbsent("k", null));
        assertThrows(NullPointerException.class, () -> cache.computeIfPresent(null, (key, value) -> 1));
        assertThrows(NullPointerException.class, () -> cache.computeIfPresent("k", null));
        assertThrows(NullPointerException.class, () -> cache.compute(null, (key, value) -> 1));
        assertThrows(NullPointerException.class, () -> cache.compute("k", null));
        assertThrows(NullPointerException.class, () -> cache.merge(null, 1, (a, b) -> a));
        assertThrows(NullPointerException.class, () -> cache.merge("k", null, (a, b) -> a));
        assertThrows(NullPointerException.class, () -> cache.merge("k", 1, null));
        assertEquals(1, cache.get("k"));
    }

    @Test
    public void testReplaceAllContract() {
        ConcurrentCacheMap<String, Integer> cache = new ConcurrentCacheMap<>(8);
        cache.put("a", 1);
        cache.put("b", 2);

        cache.replaceAll((key, value) -> value * 10);
        assertEquals(10, cache.get("a"));
        assertEquals(20, cache.get("b"));
        assertEquals(2, cache.size());

        assertThrows(NullPointerException.class, () -> cache.replaceAll(null));
        assertThrows(NullPointerException.class, () -> cache.replaceAll((key, value) -> null));
        assertEquals(2, cache.size());
        assertEquals(10, cache.get("a"));
        assertEquals(20, cache.get("b"));
    }
}

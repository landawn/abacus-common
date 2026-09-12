package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

public class LazyInitializerTest extends TestBase {

    @Test
    public void testOf_basic() {
        LazyInitializer<String> lazy = LazyInitializer.of(() -> "hello");
        assertEquals("hello", lazy.get());
    }

    @Test
    public void testOf_nullSupplier() {
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class, () -> LazyInitializer.of(null));
    }

    @Test
    public void testGet_initializesOnlyOnce() {
        AtomicInteger count = new AtomicInteger();
        LazyInitializer<Integer> lazy = LazyInitializer.of(() -> {
            count.incrementAndGet();
            return 42;
        });

        assertEquals(0, count.get(), "Supplier must not be invoked before first get()");
        assertEquals(42, lazy.get());
        assertEquals(1, count.get());
        // Repeated calls must reuse cached value
        assertEquals(42, lazy.get());
        assertEquals(42, lazy.get());
        assertEquals(1, count.get(), "Supplier must be invoked at most once on success");
    }

    @Test
    public void testGet_nullValueIsCached() {
        AtomicInteger count = new AtomicInteger();
        LazyInitializer<String> lazy = LazyInitializer.of(() -> {
            count.incrementAndGet();
            return null;
        });

        assertNull(lazy.get());
        assertNull(lazy.get());
        assertEquals(1, count.get(), "Null is a valid initialized value and must be cached");
    }

    @Test
    public void testGet_supplierThrows_propagatesAndRetries() {
        AtomicInteger count = new AtomicInteger();
        LazyInitializer<String> lazy = LazyInitializer.of(() -> {
            count.incrementAndGet();
            throw new IllegalStateException("boom");
        });

        assertThrows(IllegalStateException.class, lazy::get);
        assertThrows(IllegalStateException.class, lazy::get);
        assertTrue(count.get() >= 2, "On exception, initializer should retry on subsequent calls");
    }

    @Test
    public void testGet_supplierEventuallySucceeds_resultIsCached() {
        AtomicInteger count = new AtomicInteger();
        LazyInitializer<String> lazy = LazyInitializer.of(() -> {
            int n = count.incrementAndGet();
            if (n < 2) {
                throw new RuntimeException("not yet");
            }
            return "ok-" + n;
        });

        assertThrows(RuntimeException.class, lazy::get);
        assertEquals("ok-2", lazy.get());
        assertEquals("ok-2", lazy.get());
        assertEquals(2, count.get(), "After first successful init, no further supplier calls expected");
    }

    @Test
    public void testGet_recursiveInitializationFailsFastAndRetries() {
        final AtomicInteger count = new AtomicInteger();
        @SuppressWarnings("unchecked")
        final LazyInitializer<String>[] holder = new LazyInitializer[1];

        holder[0] = LazyInitializer.of(() -> {
            count.incrementAndGet();
            return holder[0].get();
        });

        final IllegalStateException first = assertThrows(IllegalStateException.class, holder[0]::get);
        final IllegalStateException second = assertThrows(IllegalStateException.class, holder[0]::get);

        assertEquals("Recursive initialization of deferred value", first.getMessage());
        assertEquals("Recursive initialization of deferred value", second.getMessage());
        assertNotSame(first, second);
        assertEquals(2, count.get(), "A failed recursive initialization must remain retryable");
    }

    @Test
    public void testGet_releasesSupplierAfterSuccessfulInitialization() throws Exception {
        LazyInitializer<Object> lazy = LazyInitializer.of(Object::new);
        java.lang.reflect.Field supplierField = LazyInitializer.class.getDeclaredField("supplier");
        supplierField.setAccessible(true);
        assertNotNull(supplierField.get(lazy));

        lazy.get();

        assertNull(supplierField.get(lazy), "Successful initialization must not retain the supplier or its captured state");
    }

    @Test
    public void testOf_doesNotDoubleWrap() {
        LazyInitializer<String> inner = LazyInitializer.of(() -> "x");
        // The inner is itself a Supplier<T>, so of() should return it as-is
        LazyInitializer<String> outer = LazyInitializer.of(inner);
        assertSame(inner, outer);
    }

    @Test
    public void testGet_concurrent_supplierInvokedAtMostOnce() throws Exception {
        final int threads = 16;
        final AtomicInteger count = new AtomicInteger();
        final LazyInitializer<Object> lazy = LazyInitializer.of(() -> {
            count.incrementAndGet();
            // Simulate slow init so all threads pile on the lock
            try {
                Thread.sleep(20);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return new Object();
        });

        final CountDownLatch start = new CountDownLatch(1);
        final CountDownLatch done = new CountDownLatch(threads);
        final AtomicReference<Object> first = new AtomicReference<>();
        final AtomicReference<Throwable> firstError = new AtomicReference<>();

        ExecutorService pool = Executors.newFixedThreadPool(threads);
        try {
            for (int i = 0; i < threads; i++) {
                pool.submit(() -> {
                    try {
                        start.await();
                        Object got = lazy.get();
                        first.compareAndSet(null, got);
                        if (got != first.get()) {
                            firstError.compareAndSet(null, new AssertionError("Different instance returned by concurrent get()"));
                        }
                    } catch (Throwable t) {
                        firstError.compareAndSet(null, t);
                    } finally {
                        done.countDown();
                    }
                });
            }
            start.countDown();
            assertTrue(done.await(10, TimeUnit.SECONDS), "Threads timed out");
        } finally {
            pool.shutdownNow();
        }

        assertNull(firstError.get(), () -> "Concurrent get() returned different instances or threw: " + firstError.get());
        assertEquals(1, count.get(), "Supplier should be invoked exactly once even under concurrent access");
        assertNotNull(first.get());
    }

    // FINDING 28 (same defect as Fnn.memoizeWithExpiration): this object is what Fn.memoize(Supplier) and
    // N.lazyInit(Supplier) hand back, so synchronizing on `this` let caller code holding that monitor block the
    // initialization. Initialization now runs under a private lock.
    @Test
    public void reviewFixes20260908_getDoesNotSynchronizeOnTheReturnedInitializer() throws Exception {
        final AtomicInteger calls = new AtomicInteger();
        final LazyInitializer<Integer> lazy = LazyInitializer.of(calls::incrementAndGet);

        final CountDownLatch done = new CountDownLatch(1);
        final AtomicReference<Object> result = new AtomicReference<>();
        final Thread worker = new Thread(() -> {
            try {
                result.set(lazy.get());
            } catch (final Throwable e) { // NOSONAR - the failure has to reach the assertion below
                result.set(e);
            } finally {
                done.countDown();
            }
        });
        worker.setDaemon(true);

        synchronized (lazy) {
            worker.start();
            assertTrue(done.await(10, TimeUnit.SECONDS), "get() blocked on the caller-visible monitor of the initializer");
        }

        assertEquals(1, result.get());
        assertEquals(1, calls.get());
        assertEquals(1, lazy.get());
    }

}

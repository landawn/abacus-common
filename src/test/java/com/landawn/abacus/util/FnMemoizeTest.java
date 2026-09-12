package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.util.function.Function;
import com.landawn.abacus.util.function.Supplier;

public class FnMemoizeTest extends FnTestSupport {

    @Test
    public void testMemoize_Supplier() {
        final AtomicInteger counter = new AtomicInteger();
        final Supplier<Integer> memoized = Fn.memoize(counter::incrementAndGet);
        assertEquals(1, memoized.get());
        assertEquals(1, memoized.get());
        assertEquals(1, counter.get());
        assertThrows(IllegalArgumentException.class, () -> Fn.memoize((java.util.function.Supplier<Object>) null));
    }

    @Test
    public void testMemoize_Function() {
        final AtomicInteger counter = new AtomicInteger();
        final Function<String, Integer> memoized = Fn.memoize(s -> {
            counter.incrementAndGet();
            return s == null ? -1 : s.length();
        });
        assertEquals(3, memoized.apply("abc"));
        assertEquals(3, memoized.apply("abc"));
        assertEquals(4, memoized.apply("abcd"));
        assertEquals(-1, memoized.apply(null));
        assertEquals(-1, memoized.apply(null));
        assertEquals(3, counter.get());
        assertThrows(IllegalArgumentException.class, () -> Fn.memoize((java.util.function.Function<Object, Object>) null));
    }

    @Test
    public void testMemoize_Function_DoesNotCacheException() {
        final AtomicInteger calls = new AtomicInteger();
        final Function<String, String> f = Fn.memoize(s -> {
            calls.incrementAndGet();
            if ("bad".equals(s)) {
                throw new RuntimeException("boom");
            }
            return s + "!";
        });
        assertThrows(RuntimeException.class, () -> f.apply("bad"));
        assertEquals("ok!", f.apply("ok"));
        assertThrows(RuntimeException.class, () -> f.apply("bad"));
        assertTrue(calls.get() >= 3);
    }

    @Test
    public void testMemoize_Function_NullExceptionNotCached() {
        final AtomicInteger calls = new AtomicInteger();
        final Function<Object, String> f = Fn.memoize(o -> {
            if (calls.incrementAndGet() == 1) {
                throw new RuntimeException("first failure");
            }
            return "ok";
        });
        assertThrows(RuntimeException.class, () -> f.apply(null));
        assertEquals("ok", f.apply(null));
        assertEquals("ok", f.apply(null));
        assertEquals(2, calls.get());
    }

    @Test
    public void testMemoize_Function_RecursiveSameKeyFailsFastAndRetries() {
        final AtomicInteger attempts = new AtomicInteger();
        @SuppressWarnings("unchecked")
        final Function<String, String>[] holder = new Function[1];
        holder[0] = Fn.memoize(key -> {
            attempts.incrementAndGet();
            return holder[0].apply(key);
        });
        assertThrows(IllegalStateException.class, () -> holder[0].apply(null));
        assertThrows(IllegalStateException.class, () -> holder[0].apply(null));
        assertThrows(IllegalStateException.class, () -> holder[0].apply("key"));
        assertThrows(IllegalStateException.class, () -> holder[0].apply("key"));
        assertEquals(4, attempts.get());
    }

    @Test
    public void testMemoize_Function_SwallowedRecursionStillFails() {
        final AtomicInteger attempts = new AtomicInteger();
        @SuppressWarnings("unchecked")
        final Function<String, String>[] holder = new Function[1];
        holder[0] = Fn.memoize(key -> {
            attempts.incrementAndGet();
            try {
                holder[0].apply(key);
            } catch (final IllegalStateException ignored) {
            }
            return "must-not-be-cached";
        });
        assertThrows(IllegalStateException.class, () -> holder[0].apply(null));
        assertThrows(IllegalStateException.class, () -> holder[0].apply("key"));
        assertEquals(2, attempts.get());
    }

    @Test
    public void testMemoize_Function_SwallowedRecursionDoesNotPoisonOtherKeys() {
        final AtomicInteger attempts = new AtomicInteger();
        @SuppressWarnings("unchecked")
        final Function<String, String>[] holder = new Function[1];
        holder[0] = Fn.memoize(key -> {
            attempts.incrementAndGet();
            if (!"outer".equals(key)) {
                return "other-value";
            }
            try {
                holder[0].apply("outer"); // same-key recursion: poisons "outer" only
            } catch (final IllegalStateException ignored) {
                // deliberately swallowed
            }
            // an unrelated key computed inside the poisoned call must still succeed and be cached
            return "inner=" + holder[0].apply("other");
        });

        assertThrows(IllegalStateException.class, () -> holder[0].apply("outer"));
        assertEquals(2, attempts.get());
        assertEquals("other-value", holder[0].apply("other"));
        assertEquals(2, attempts.get());
    }

    @Test
    public void testMemoize_Function_DistinctKeyRecursion() {
        @SuppressWarnings("unchecked")
        final Function<Integer, Integer>[] holder = new Function[1];
        holder[0] = Fn.memoize(key -> key == 0 ? 0 : holder[0].apply(key - 1) + 1);
        assertEquals(4, holder[0].apply(4));
        assertEquals(4, holder[0].apply(4));
    }

    @Test
    public void testMemoizeWithExpiration() throws InterruptedException {
        final AtomicInteger counter = new AtomicInteger();
        assertThrows(IllegalArgumentException.class, () -> Fn.memoizeWithExpiration(counter::incrementAndGet, 0, TimeUnit.MILLISECONDS));
        assertThrows(IllegalArgumentException.class, () -> Fn.memoizeWithExpiration(counter::incrementAndGet, -1, TimeUnit.SECONDS));
        assertThrows(IllegalArgumentException.class, () -> Fn.memoizeWithExpiration(null, 1, TimeUnit.SECONDS));

        final Supplier<Integer> memoized = Fn.memoizeWithExpiration(counter::incrementAndGet, 100, TimeUnit.MILLISECONDS);
        assertEquals(1, memoized.get());
        assertEquals(1, memoized.get());
        Thread.sleep(150);
        assertEquals(2, memoized.get());
        assertEquals(2, counter.get());
    }

    @Test
    public void testMemoizeWithExpiration_Duration() {
        final AtomicInteger callCount = new AtomicInteger();
        final Supplier<Integer> memoized = Fn.memoizeWithExpiration(callCount::incrementAndGet, Duration.ofSeconds(10));
        assertEquals(1, memoized.get());
        assertEquals(1, memoized.get());
        assertEquals(1, callCount.get());
        assertThrows(IllegalArgumentException.class, () -> Fn.memoizeWithExpiration(() -> 1, Duration.ZERO));
    }

    @Test
    public void testMemoizeWithExpiration_Concurrency() {
        final AtomicInteger counter = new AtomicInteger();
        final Supplier<Integer> memoized = Fn.memoizeWithExpiration(() -> {
            try {
                Thread.sleep(40);
            } catch (final InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return counter.incrementAndGet();
        }, 200, TimeUnit.MILLISECONDS);

        final List<CompletableFuture<Integer>> futures = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            futures.add(CompletableFuture.supplyAsync(memoized::get));
        }
        assertTrue(futures.stream().map(CompletableFuture::join).allMatch(r -> r == 1));
        assertEquals(1, counter.get());
    }

    @Test
    public void testMemoizeWithExpiration_RecursiveFailsFastAndRetries() {
        final AtomicInteger attempts = new AtomicInteger();
        final AtomicReference<Supplier<Integer>> holder = new AtomicReference<>();
        holder.set(Fn.memoizeWithExpiration(() -> {
            attempts.incrementAndGet();
            return holder.get().get();
        }, 1, TimeUnit.HOURS));
        assertEquals("Recursive computation of memoized value", assertThrows(IllegalStateException.class, holder.get()::get).getMessage());
        assertEquals("Recursive computation of memoized value", assertThrows(IllegalStateException.class, holder.get()::get).getMessage());
        assertEquals(2, attempts.get());
    }

    @Test
    public void testMemoizeWithExpiration_NanosExpireImmediately() throws InterruptedException {
        final AtomicInteger counter = new AtomicInteger();
        final Supplier<Integer> memoized = Fn.memoizeWithExpiration(counter::incrementAndGet, 1, TimeUnit.NANOSECONDS);
        final int first = memoized.get();
        Thread.sleep(1);
        assertNotEquals(first, memoized.get());
    }

    @Test
    public void testMemoize_Function_NullAndNonNullCrossCallsDoNotDeadlock() throws Exception {
        final CountDownLatch nullEntered = new CountDownLatch(1);
        final CountDownLatch nonNullStarted = new CountDownLatch(1);
        final CountDownLatch nonNullEntered = new CountDownLatch(1);
        final AtomicReference<Thread> dedicated = new AtomicReference<>();
        final AtomicReference<String> nullResult = new AtomicReference<>();
        final AtomicReference<String> nonNullResult = new AtomicReference<>();
        final AtomicReference<Throwable> nullFailure = new AtomicReference<>();
        final AtomicReference<Throwable> nonNullFailure = new AtomicReference<>();
        @SuppressWarnings("unchecked")
        final Function<String, String>[] holder = new Function[1];
        holder[0] = Fn.memoize(key -> {
            if (key == null) {
                nullEntered.countDown();
                try {
                    assertTrue(nonNullStarted.await(5, TimeUnit.SECONDS));
                    nonNullEntered.await(1, TimeUnit.SECONDS);
                } catch (final InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw ExceptionUtil.toRuntimeException(e);
                }
                return "null->" + holder[0].apply("key");
            }
            nonNullEntered.countDown();
            if (Thread.currentThread() == dedicated.get()) {
                return "key->" + holder[0].apply(null);
            }
            return "key";
        });

        final Thread nullWorker = new Thread(() -> {
            try {
                nullResult.set(holder[0].apply(null));
            } catch (final Throwable e) {
                nullFailure.set(e);
            }
        });
        nullWorker.setDaemon(true);
        nullWorker.start();
        assertTrue(nullEntered.await(5, TimeUnit.SECONDS));

        final Thread nonNullWorker = new Thread(() -> {
            nonNullStarted.countDown();
            try {
                nonNullResult.set(holder[0].apply("key"));
            } catch (final Throwable e) {
                nonNullFailure.set(e);
            }
        });
        dedicated.set(nonNullWorker);
        nonNullWorker.setDaemon(true);
        nonNullWorker.start();

        nullWorker.join(TimeUnit.SECONDS.toMillis(3));
        nonNullWorker.join(TimeUnit.SECONDS.toMillis(3));
        assertFalse(nullWorker.isAlive());
        assertFalse(nonNullWorker.isAlive());
        assertNull(nullFailure.get());
        assertNull(nonNullFailure.get());
        assertEquals("null->key", nullResult.get());
        assertEquals("key", nonNullResult.get());
    }

    // FINDING R09-5 (the same defect Fnn.memoizeWithExpiration was fixed for): the returned supplier is handed
    // back to the caller, so refreshing under `synchronized (this)` let caller code holding that monitor block -
    // and interleave with - every refresh. Refreshes now run under a private lock.
    @Test
    public void reviewFixes20260908_memoizeWithExpirationDoesNotSynchronizeOnTheReturnedSupplier() throws Exception {
        final AtomicInteger calls = new AtomicInteger();
        final Supplier<Integer> memo = Fn.memoizeWithExpiration(calls::incrementAndGet, 1, TimeUnit.HOURS);

        assertEquals(1, getWhileHoldingMonitorOf(memo));
        assertEquals(1, calls.get());
        assertEquals(1, memo.get());
    }

    // The Duration overload delegates to the (long, TimeUnit) one, so it hands back the very same object.
    @Test
    public void reviewFixes20260908_memoizeWithExpirationDurationDoesNotSynchronizeOnTheReturnedSupplier() throws Exception {
        final AtomicInteger calls = new AtomicInteger();
        final Supplier<Integer> memo = Fn.memoizeWithExpiration(calls::incrementAndGet, Duration.ofHours(1));

        assertEquals(1, getWhileHoldingMonitorOf(memo));
        assertEquals(1, calls.get());
        assertEquals(1, memo.get());
    }

    /**
     * Calls {@code get()} on another thread while this thread holds the supplier's own monitor, and returns
     * whatever that call produced. Fails if the call did not finish, i.e. if it was blocked by the caller-held
     * monitor.
     */
    private static Object getWhileHoldingMonitorOf(final Supplier<?> supplier) throws Exception {
        final CountDownLatch done = new CountDownLatch(1);
        final AtomicReference<Object> result = new AtomicReference<>();
        final Thread worker = new Thread(() -> {
            try {
                result.set(supplier.get());
            } catch (final Throwable e) { // NOSONAR - the failure has to reach the assertion in the caller
                result.set(e);
            } finally {
                done.countDown();
            }
        });
        worker.setDaemon(true);

        synchronized (supplier) {
            worker.start();
            assertTrue(done.await(10, TimeUnit.SECONDS), "get() blocked on the caller-visible monitor of the returned supplier");
        }

        return result.get();
    }

    // FINDING G20-002 (doc-only contract pins): re-entering get() from inside the delegate throws
    // IllegalStateException, and that failure is not cached - both now stated in the Fn javadocs.
    @Test
    public void testMemoize_SupplierRecursiveFailsFastAndRetries() {
        final AtomicReference<Supplier<String>> holder = new AtomicReference<>();
        final AtomicInteger attempts = new AtomicInteger();
        holder.set(Fn.memoize(() -> attempts.incrementAndGet() == 1 ? holder.get().get() : "v"));

        final IllegalStateException ex = assertThrows(IllegalStateException.class, () -> holder.get().get());
        assertEquals("Recursive initialization of deferred value", ex.getMessage());
        assertEquals("v", holder.get().get());
        assertEquals(2, attempts.get());
    }

    @Test
    public void testMemoizeWithExpiration_DurationRecursiveFailsFastAndRetries() {
        final AtomicReference<Supplier<String>> holder = new AtomicReference<>();
        final AtomicInteger attempts = new AtomicInteger();
        holder.set(Fn.memoizeWithExpiration(() -> attempts.incrementAndGet() == 1 ? holder.get().get() : "v", Duration.ofHours(1)));

        final IllegalStateException ex = assertThrows(IllegalStateException.class, () -> holder.get().get());
        assertEquals("Recursive computation of memoized value", ex.getMessage());
        assertEquals("v", holder.get().get());
        assertEquals(2, attempts.get());
    }
}

package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

public class RateLimiterTest extends TestBase {

    private static final class FakeSleepingStopwatch extends RateLimiter.SleepingStopwatch {
        private long micros;

        @Override
        protected long readMicros() {
            return micros;
        }

        @Override
        protected void sleepMicrosUninterruptibly(final long micros) {
            this.micros += micros;
        }

        public void advanceMicros(final long micros) {
            this.micros += micros;
        }
    }

    @Test
    public void testCreateAndSetRate() {
        RateLimiter limiter = RateLimiter.create(5.0);
        assertEquals(5.0, limiter.getRate(), 0.001);
        assertEquals(1.0, RateLimiter.create(1.0).getRate(), 0.001);
        assertEquals(0.5, RateLimiter.create(0.5).getRate(), 0.001);
        assertEquals(10.0, RateLimiter.create(10.0, 3, TimeUnit.SECONDS).getRate(), 0.001);
        assertEquals(5.0, RateLimiter.create(5.0, 0, TimeUnit.SECONDS).getRate(), 0.001);
        assertNotNull(RateLimiter.create(10.0, 500, TimeUnit.MILLISECONDS));
        assertNotNull(RateLimiter.create(10.0, 1, TimeUnit.MINUTES));

        limiter.setRate(10.0);
        assertEquals(10.0, limiter.getRate(), 0.001);
        limiter.setRate(0.25);
        assertEquals(0.25, limiter.getRate(), 0.001);

        assertThrows(IllegalArgumentException.class, () -> RateLimiter.create(0.0));
        assertThrows(IllegalArgumentException.class, () -> RateLimiter.create(-1.0));
        assertThrows(IllegalArgumentException.class, () -> RateLimiter.create(Double.NaN));
        assertThrows(IllegalArgumentException.class, () -> RateLimiter.create(10.0, -1, TimeUnit.SECONDS));
        assertThrows(IllegalArgumentException.class, () -> RateLimiter.create(0.0, 3, TimeUnit.SECONDS));
        assertThrows(IllegalArgumentException.class, () -> limiter.setRate(0.0));
        assertThrows(IllegalArgumentException.class, () -> limiter.setRate(-1.0));
        assertThrows(IllegalArgumentException.class, () -> limiter.setRate(Double.NaN));
        assertEquals(0.25, limiter.getRate(), 0.001);
    }

    @Test
    public void testCreateWithZeroWarmupDoesNotCorruptPermitsAfterIdle() {
        FakeSleepingStopwatch stopwatch = new FakeSleepingStopwatch();
        RateLimiter limiter = RateLimiter.create(1.0, 0, TimeUnit.SECONDS, 3.0, stopwatch);
        assertTrue(limiter.tryAcquire());
        stopwatch.advanceMicros(2_000_000L);
        assertTrue(limiter.tryAcquire());
        assertTrue(limiter.tryAcquire());
        assertFalse(limiter.tryAcquire());
    }

    @Test
    public void testAcquire() {
        RateLimiter fast = RateLimiter.create(1000.0);
        assertTrue(fast.acquire() >= 0);
        assertTrue(fast.acquire(5) >= 0);

        RateLimiter limiter = RateLimiter.create(5.0);
        limiter.acquire();
        limiter.setRate(10.0);
        assertEquals(10.0, limiter.getRate(), 0.001);
        assertTrue(limiter.acquire() >= 0);
        assertTrue(limiter.acquire(3) >= 0);
        assertThrows(IllegalArgumentException.class, () -> limiter.acquire(0));
        assertThrows(IllegalArgumentException.class, () -> limiter.acquire(-1));

        RateLimiter warmup = RateLimiter.create(10.0, 2, TimeUnit.SECONDS);
        assertTrue(warmup.acquire() >= 0);
        assertTrue(warmup.acquire() >= 0);
        assertEquals(10.0, warmup.getRate(), 0.001);

        RateLimiter burst = RateLimiter.create(2.0);
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        long start = System.nanoTime();
        burst.acquire();
        burst.acquire();
        assertTrue((System.nanoTime() - start) / 1_000_000_000.0 < 0.8);

        RateLimiter paced = RateLimiter.create(2.0);
        start = System.nanoTime();
        paced.acquire();
        paced.acquire();
        paced.acquire();
        paced.acquire();
        assertTrue((System.nanoTime() - start) / 1_000_000_000.0 >= 1.0);
    }

    @Test
    public void testTryAcquire() {
        RateLimiter limiter = RateLimiter.create(10.0);
        assertTrue(limiter.tryAcquire());
        assertTrue(RateLimiter.create(1000.0).tryAcquire());
        assertTrue(RateLimiter.create(1000.0).tryAcquire(5));
        assertTrue(RateLimiter.create(10.0).tryAcquire(5, 500, TimeUnit.MILLISECONDS));
        assertTrue(RateLimiter.create(1.0).tryAcquire(100, TimeUnit.MILLISECONDS));
        assertNotNull(RateLimiter.create(10.0).tryAcquire(-100, TimeUnit.MILLISECONDS));

        RateLimiter slow = RateLimiter.create(0.1);
        slow.acquire();
        assertFalse(slow.tryAcquire(10, TimeUnit.MILLISECONDS));

        assertThrows(IllegalArgumentException.class, () -> limiter.tryAcquire(0));
        assertThrows(IllegalArgumentException.class, () -> limiter.tryAcquire(-1));
        assertThrows(IllegalArgumentException.class, () -> limiter.tryAcquire(0, 100, TimeUnit.MILLISECONDS));
        assertThrows(IllegalArgumentException.class, () -> limiter.tryAcquire(-1, 100, TimeUnit.MILLISECONDS));
    }

    @Test
    public void testConcurrentAcquire() throws InterruptedException {
        RateLimiter limiter = RateLimiter.create(10.0);
        AtomicInteger success = new AtomicInteger();
        Thread[] threads = new Thread[5];
        for (int i = 0; i < threads.length; i++) {
            threads[i] = new Thread(() -> {
                limiter.acquire();
                success.incrementAndGet();
            });
            threads[i].start();
        }
        for (Thread thread : threads) {
            thread.join();
        }
        assertEquals(5, success.get());

        RateLimiter tryLimiter = RateLimiter.create(10.0);
        AtomicInteger trySuccess = new AtomicInteger();
        for (int i = 0; i < threads.length; i++) {
            threads[i] = new Thread(() -> {
                if (tryLimiter.tryAcquire(100, TimeUnit.MILLISECONDS)) {
                    trySuccess.incrementAndGet();
                }
            });
            threads[i].start();
        }
        for (Thread thread : threads) {
            thread.join();
        }
        assertTrue(trySuccess.get() > 0);

        RateLimiter shared = RateLimiter.create(5.0);
        AtomicInteger errors = new AtomicInteger();
        Thread[] readers = new Thread[10];
        for (int i = 0; i < readers.length; i++) {
            final int threadId = i;
            readers[i] = new Thread(() -> {
                for (int j = 0; j < 100; j++) {
                    double rate = shared.getRate();
                    if (rate != 5.0 && rate != 10.0) {
                        errors.incrementAndGet();
                    }
                    if (j == 50 && threadId == 0) {
                        shared.setRate(10.0);
                    }
                }
            });
            readers[i].start();
        }
        for (Thread thread : readers) {
            thread.join();
        }
        assertEquals(0, errors.get());
    }

    @Test
    public void testToStringAndStopwatch() {
        RateLimiter limiter = RateLimiter.create(5.0);
        assertTrue(limiter.toString().contains("RateLimiter"));
        assertTrue(limiter.toString().contains("5."));
        limiter.setRate(10.0);
        assertTrue(limiter.toString().contains("10."));

        RateLimiter.SleepingStopwatch stopwatch = RateLimiter.SleepingStopwatch.createFromSystemTimer();
        assertNotNull(stopwatch);
        long micros1 = stopwatch.readMicros();
        assertTrue(micros1 >= 0);
        stopwatch.sleepMicrosUninterruptibly(1000);
        assertTrue(stopwatch.readMicros() >= micros1);
    }

    /**
     * Pins the exact rendering documented on {@link RateLimiter#toString()}: the rate goes through
     * {@link Double#toString(double)}, so a fractional rate keeps every digit it needs and a large rate
     * comes out in scientific notation. The equivalent assertions in {@code MultiClassRegressionCTest}
     * live in a {@code @Nested public static class}, which JUnit never runs, so they pin nothing.
     */
    @Test
    public void testToStringRendersRateWithDoubleToString() {
        assertEquals("RateLimiter[stableRate=5.0qps]", RateLimiter.create(5.0).toString());
        assertEquals("RateLimiter[stableRate=0.05qps]", RateLimiter.create(0.05).toString());

        final RateLimiter limiter = RateLimiter.create(5.0);
        limiter.setRate(10.0);
        assertEquals("RateLimiter[stableRate=10.0qps]", limiter.toString());

        // Large rates are rendered in scientific notation - documented on toString().
        assertEquals("RateLimiter[stableRate=1.0E7qps]", RateLimiter.create(1e7).toString());
    }

    /**
     * Pins the paragraph now on {@link RateLimiter#getRate()} about the upper extreme: {@code +Infinity} is a
     * positive, non-NaN rate, so both {@code create} and {@code setRate} accept it. It yields a zero permit
     * interval - an effectively unlimited limiter that never throttles - and {@code getRate()} reports
     * {@code Infinity}. Only {@code NaN}, zero and negative rates are rejected.
     */
    @Test
    public void reviewFixes20260911_positiveInfinityIsAnAcceptedRateThatNeverThrottles() {
        assertEquals(Double.POSITIVE_INFINITY, RateLimiter.create(Double.POSITIVE_INFINITY).getRate());

        final RateLimiter limiter = RateLimiter.create(5.0);
        limiter.setRate(Double.POSITIVE_INFINITY);
        assertEquals(Double.POSITIVE_INFINITY, limiter.getRate());

        // A zero permit interval: even an absurd permit count costs nothing and nothing is queued behind it.
        assertEquals(0.0, limiter.acquire(1_000_000));
        assertTrue(limiter.tryAcquire(1_000_000));
        assertEquals(0.0, limiter.acquire());

        // The rejected values are unchanged.
        assertThrows(IllegalArgumentException.class, () -> RateLimiter.create(Double.NaN));
        assertThrows(IllegalArgumentException.class, () -> RateLimiter.create(0.0));
        assertThrows(IllegalArgumentException.class, () -> RateLimiter.create(Double.NEGATIVE_INFINITY));
        assertThrows(IllegalArgumentException.class, () -> limiter.setRate(Double.NaN));
    }

    /**
     * Pins the one exception to {@code doGetRate()}'s "positive" contract, which the paragraph on
     * {@link RateLimiter#getRate()} documents: below roughly {@code 5.6e-303} permits per second the stored
     * permit interval overflows to {@code +Infinity} and the reported rate is {@code 0.0} - a value
     * {@code setRate} itself rejects, so a {@code 0.0} reading is not proof that no rate was ever set.
     */
    @Test
    public void reviewFixes20260911_aRateWhosePermitIntervalOverflowsIsReportedAsZeroNotAsPositive() {
        assertTrue(RateLimiter.create(5.6e-303).getRate() > 0.0);

        assertEquals(0.0, RateLimiter.create(5.5e-303).getRate());
        assertEquals(0.0, RateLimiter.create(Double.MIN_VALUE).getRate());
        assertEquals(0.0, RateLimiter.create(5.5e-303, 1, TimeUnit.SECONDS).getRate());

        assertThrows(IllegalArgumentException.class, () -> RateLimiter.create(5.5e-303).setRate(0.0));
    }
}

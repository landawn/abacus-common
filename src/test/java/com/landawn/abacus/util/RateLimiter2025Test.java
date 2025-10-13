package com.landawn.abacus.util;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import com.landawn.abacus.TestBase;

@Tag("2025")
public class RateLimiter2025Test extends TestBase {

    @Test
    public void testCreate_withValidRate() {
        RateLimiter limiter = RateLimiter.create(5.0);
        Assertions.assertNotNull(limiter);
        Assertions.assertEquals(5.0, limiter.getRate(), 0.001);

        RateLimiter limiter1 = RateLimiter.create(1.0);
        Assertions.assertNotNull(limiter1);
        Assertions.assertEquals(1.0, limiter1.getRate(), 0.001);

        RateLimiter limiter100 = RateLimiter.create(100.0);
        Assertions.assertNotNull(limiter100);
        Assertions.assertEquals(100.0, limiter100.getRate(), 0.001);

        RateLimiter limiterFrac = RateLimiter.create(0.5);
        Assertions.assertNotNull(limiterFrac);
        Assertions.assertEquals(0.5, limiterFrac.getRate(), 0.001);
    }

    @Test
    public void testCreate_withInvalidRate() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            RateLimiter.create(0.0);
        });

        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            RateLimiter.create(-1.0);
        });

        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            RateLimiter.create(Double.NaN);
        });

        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            RateLimiter.create(-0.5);
        });
    }

    @Test
    public void testCreate_withWarmup() {
        RateLimiter limiter = RateLimiter.create(10.0, 3, TimeUnit.SECONDS);
        Assertions.assertNotNull(limiter);
        Assertions.assertEquals(10.0, limiter.getRate(), 0.001);

        RateLimiter limiterZeroWarmup = RateLimiter.create(5.0, 0, TimeUnit.SECONDS);
        Assertions.assertNotNull(limiterZeroWarmup);
        Assertions.assertEquals(5.0, limiterZeroWarmup.getRate(), 0.001);

        RateLimiter limiterMillis = RateLimiter.create(10.0, 500, TimeUnit.MILLISECONDS);
        Assertions.assertNotNull(limiterMillis);

        RateLimiter limiterMinutes = RateLimiter.create(10.0, 1, TimeUnit.MINUTES);
        Assertions.assertNotNull(limiterMinutes);
    }

    @Test
    public void testCreate_withWarmup_invalidParameters() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            RateLimiter.create(10.0, -1, TimeUnit.SECONDS);
        });

        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            RateLimiter.create(0.0, 3, TimeUnit.SECONDS);
        });

        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            RateLimiter.create(-5.0, 3, TimeUnit.SECONDS);
        });
    }

    @Test
    public void testSetRate_withValidRate() {
        RateLimiter limiter = RateLimiter.create(5.0);
        Assertions.assertEquals(5.0, limiter.getRate(), 0.001);

        limiter.setRate(10.0);
        Assertions.assertEquals(10.0, limiter.getRate(), 0.001);

        limiter.setRate(1.0);
        Assertions.assertEquals(1.0, limiter.getRate(), 0.001);

        limiter.setRate(0.25);
        Assertions.assertEquals(0.25, limiter.getRate(), 0.001);
    }

    @Test
    public void testSetRate_withInvalidRate() {
        RateLimiter limiter = RateLimiter.create(5.0);

        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            limiter.setRate(0.0);
        });

        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            limiter.setRate(-1.0);
        });

        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            limiter.setRate(Double.NaN);
        });

        Assertions.assertEquals(5.0, limiter.getRate(), 0.001);
    }

    @Test
    public void testGetRate() {
        RateLimiter limiter = RateLimiter.create(7.5);
        Assertions.assertEquals(7.5, limiter.getRate(), 0.001);

        limiter.setRate(15.0);
        Assertions.assertEquals(15.0, limiter.getRate(), 0.001);

        RateLimiter warmupLimiter = RateLimiter.create(10.0, 2, TimeUnit.SECONDS);
        Assertions.assertEquals(10.0, warmupLimiter.getRate(), 0.001);
    }

    @Test
    public void testAcquire_singlePermit() {
        RateLimiter limiter = RateLimiter.create(10.0);

        double waitTime = limiter.acquire();
        Assertions.assertTrue(waitTime >= 0.0, "Wait time should be non-negative");

        double waitTime2 = limiter.acquire();
        Assertions.assertTrue(waitTime2 >= 0.0, "Wait time should be non-negative");
    }

    @Test
    public void testAcquire_multiplePermits() {
        RateLimiter limiter = RateLimiter.create(5.0);

        double waitTime = limiter.acquire(3);
        Assertions.assertTrue(waitTime >= 0.0, "Wait time should be non-negative");

        double waitTime2 = limiter.acquire(1);
        Assertions.assertTrue(waitTime2 >= 0.0, "Wait time should be non-negative");
    }

    @Test
    public void testAcquire_withInvalidPermits() {
        RateLimiter limiter = RateLimiter.create(5.0);

        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            limiter.acquire(0);
        });

        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            limiter.acquire(-1);
        });

        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            limiter.acquire(-10);
        });
    }

    @Test
    public void testAcquire_rateLimiting() {
        RateLimiter limiter = RateLimiter.create(2.0);

        long startTime = System.nanoTime();

        limiter.acquire();
        limiter.acquire();
        limiter.acquire();
        limiter.acquire();

        long elapsed = System.nanoTime() - startTime;
        double elapsedSeconds = elapsed / 1_000_000_000.0;

        Assertions.assertTrue(elapsedSeconds >= 1.0, "Rate limiting should enforce delays, elapsed: " + elapsedSeconds);
    }

    @Test
    public void testTryAcquire_noTimeout() {
        RateLimiter limiter = RateLimiter.create(10.0);

        boolean acquired = limiter.tryAcquire();
        Assertions.assertTrue(acquired, "First tryAcquire should succeed");

        boolean acquired2 = limiter.tryAcquire();
    }

    @Test
    public void testTryAcquire_withPermits() {
        RateLimiter limiter = RateLimiter.create(10.0);

        boolean acquired = limiter.tryAcquire(5);
        Assertions.assertTrue(acquired, "First tryAcquire(5) should succeed");

        boolean acquired2 = limiter.tryAcquire(1);
    }

    @Test
    public void testTryAcquire_withPermits_invalid() {
        RateLimiter limiter = RateLimiter.create(5.0);

        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            limiter.tryAcquire(0);
        });

        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            limiter.tryAcquire(-1);
        });
    }

    @Test
    public void testTryAcquire_withTimeout() {
        RateLimiter limiter = RateLimiter.create(5.0);

        boolean acquired = limiter.tryAcquire(1000, TimeUnit.MILLISECONDS);
        Assertions.assertTrue(acquired, "Should acquire within 1 second");

        boolean acquired2 = limiter.tryAcquire(1, TimeUnit.MICROSECONDS);
    }

    @Test
    public void testTryAcquire_withTimeout_failure() {
        RateLimiter limiter = RateLimiter.create(0.1);

        limiter.acquire();

        boolean acquired = limiter.tryAcquire(10, TimeUnit.MILLISECONDS);
        Assertions.assertFalse(acquired, "Should timeout when rate is too slow");
    }

    @Test
    public void testTryAcquire_withPermitsAndTimeout() {
        RateLimiter limiter = RateLimiter.create(10.0);

        boolean acquired = limiter.tryAcquire(5, 500, TimeUnit.MILLISECONDS);
        Assertions.assertTrue(acquired, "Should acquire 5 permits within 500ms");

        boolean acquired2 = limiter.tryAcquire(1, 0, TimeUnit.MILLISECONDS);
    }

    @Test
    public void testTryAcquire_withPermitsAndTimeout_invalid() {
        RateLimiter limiter = RateLimiter.create(5.0);

        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            limiter.tryAcquire(0, 100, TimeUnit.MILLISECONDS);
        });

        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            limiter.tryAcquire(-1, 100, TimeUnit.MILLISECONDS);
        });
    }

    @Test
    public void testTryAcquire_negativeTimeout() {
        RateLimiter limiter = RateLimiter.create(10.0);

        boolean acquired = limiter.tryAcquire(-100, TimeUnit.MILLISECONDS);
    }

    @Test
    public void testToString() {
        RateLimiter limiter = RateLimiter.create(5.0);
        String str = limiter.toString();

        Assertions.assertNotNull(str);
        Assertions.assertTrue(str.contains("RateLimiter"), "toString should contain 'RateLimiter'");
        Assertions.assertTrue(str.contains("5.0") || str.contains("5."), "toString should contain the rate");

        limiter.setRate(10.0);
        String str2 = limiter.toString();
        Assertions.assertTrue(str2.contains("10.0") || str2.contains("10."), "toString should reflect updated rate");
    }

    @Test
    public void testSleepingStopwatch_createFromSystemTimer() {
        RateLimiter.SleepingStopwatch stopwatch = RateLimiter.SleepingStopwatch.createFromSystemTimer();
        Assertions.assertNotNull(stopwatch);
    }

    @Test
    public void testConcurrentAcquire() throws InterruptedException {
        RateLimiter limiter = RateLimiter.create(10.0);
        AtomicInteger successCount = new AtomicInteger(0);
        int numThreads = 5;
        Thread[] threads = new Thread[numThreads];

        for (int i = 0; i < numThreads; i++) {
            threads[i] = new Thread(() -> {
                limiter.acquire();
                successCount.incrementAndGet();
            });
            threads[i].start();
        }

        for (Thread thread : threads) {
            thread.join();
        }

        Assertions.assertEquals(numThreads, successCount.get(), "All threads should successfully acquire permits");
    }

    @Test
    public void testConcurrentTryAcquire() throws InterruptedException {
        RateLimiter limiter = RateLimiter.create(10.0);
        AtomicInteger successCount = new AtomicInteger(0);
        int numThreads = 5;
        Thread[] threads = new Thread[numThreads];

        for (int i = 0; i < numThreads; i++) {
            threads[i] = new Thread(() -> {
                if (limiter.tryAcquire(100, TimeUnit.MILLISECONDS)) {
                    successCount.incrementAndGet();
                }
            });
            threads[i].start();
        }

        for (Thread thread : threads) {
            thread.join();
        }

        Assertions.assertTrue(successCount.get() > 0, "At least some threads should successfully acquire permits");
    }

    @Test
    public void testRateChange_affectsSubsequentAcquires() {
        RateLimiter limiter = RateLimiter.create(5.0);

        limiter.acquire();

        limiter.setRate(10.0);
        Assertions.assertEquals(10.0, limiter.getRate(), 0.001);

        double waitTime = limiter.acquire();
        Assertions.assertTrue(waitTime >= 0.0);
    }

    @Test
    public void testWarmupLimiter_behavior() {
        RateLimiter limiter = RateLimiter.create(10.0, 2, TimeUnit.SECONDS);

        double waitTime1 = limiter.acquire();
        Assertions.assertTrue(waitTime1 >= 0.0);

        double waitTime2 = limiter.acquire();
        Assertions.assertTrue(waitTime2 >= 0.0);

        Assertions.assertEquals(10.0, limiter.getRate(), 0.001);
    }

    @Test
    public void testBurstCapacity() {
        RateLimiter limiter = RateLimiter.create(2.0);

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        long startTime = System.nanoTime();
        limiter.acquire();
        limiter.acquire();
        long elapsed = System.nanoTime() - startTime;
        double elapsedSeconds = elapsed / 1_000_000_000.0;

        Assertions.assertTrue(elapsedSeconds < 0.8, "Burst acquisition should be fast, elapsed: " + elapsedSeconds);
    }

    @Test
    public void testAcquire_largeNumberOfPermits() {
        RateLimiter limiter = RateLimiter.create(100.0);

        double waitTime = limiter.acquire(50);
        Assertions.assertTrue(waitTime >= 0.0);

        double waitTime2 = limiter.acquire();
        Assertions.assertTrue(waitTime2 >= 0.0);
    }

    @Test
    public void testGetRate_threadSafety() throws InterruptedException {
        RateLimiter limiter = RateLimiter.create(5.0);
        AtomicInteger errorCount = new AtomicInteger(0);
        int numThreads = 10;
        Thread[] threads = new Thread[numThreads];

        for (int i = 0; i < numThreads; i++) {
            final int threadId = i;
            threads[i] = new Thread(() -> {
                for (int j = 0; j < 100; j++) {
                    double rate = limiter.getRate();
                    if (rate != 5.0 && rate != 10.0) {
                        errorCount.incrementAndGet();
                    }
                    if (j == 50 && threadId == 0) {
                        limiter.setRate(10.0);
                    }
                }
            });
            threads[i].start();
        }

        for (Thread thread : threads) {
            thread.join();
        }

        Assertions.assertEquals(0, errorCount.get(), "Rate reading should be thread-safe");
    }

    @Test
    public void testTryAcquire_withZeroTimeout() {
        RateLimiter limiter = RateLimiter.create(10.0);

        boolean acquired = limiter.tryAcquire(1, 0, TimeUnit.MILLISECONDS);
        Assertions.assertTrue(acquired);

        boolean acquired2 = limiter.tryAcquire(1, 0, TimeUnit.MILLISECONDS);
    }

    @Test
    public void testAcquire_maintainsRate() {
        RateLimiter limiter = RateLimiter.create(5.0);

        long startTime = System.currentTimeMillis();

        for (int i = 0; i < 10; i++) {
            limiter.acquire();
        }

        long elapsed = System.currentTimeMillis() - startTime;
        double elapsedSeconds = elapsed / 1000.0;

        Assertions.assertTrue(elapsedSeconds >= 1.0, "Should maintain rate limiting, elapsed: " + elapsedSeconds);
    }
}

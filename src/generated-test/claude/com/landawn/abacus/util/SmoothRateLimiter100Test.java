package com.landawn.abacus.util;

import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

public class SmoothRateLimiter100Test extends TestBase {

    @Test
    public void testSmoothBurstyDoSetRate() {
        RateLimiter.SleepingStopwatch stopwatch = RateLimiter.SleepingStopwatch.createFromSystemTimer();
        SmoothRateLimiter.SmoothBursty limiter = new SmoothRateLimiter.SmoothBursty(stopwatch, 1.0);

        limiter.doSetRate(10.0, 100000); // 10 permits per second

        Assertions.assertEquals(10.0, limiter.maxPermits);
        Assertions.assertEquals(0.0, limiter.storedPermits);
    }

    @Test
    public void testSmoothBurstyStoredPermitsToWaitTime() {
        RateLimiter.SleepingStopwatch stopwatch = RateLimiter.SleepingStopwatch.createFromSystemTimer();
        SmoothRateLimiter.SmoothBursty limiter = new SmoothRateLimiter.SmoothBursty(stopwatch, 1.0);

        long waitTime = limiter.storedPermitsToWaitTime(5.0, 2.0);
        Assertions.assertEquals(0L, waitTime); // Bursty limiter returns 0 wait time for stored permits
    }

    @Test
    public void testSmoothBurstyCoolDownIntervalMicros() {
        RateLimiter.SleepingStopwatch stopwatch = RateLimiter.SleepingStopwatch.createFromSystemTimer();
        SmoothRateLimiter.SmoothBursty limiter = new SmoothRateLimiter.SmoothBursty(stopwatch, 1.0);
        limiter.stableIntervalMicros = 100000; // 100ms

        double coolDown = limiter.coolDownIntervalMicros();
        Assertions.assertEquals(100000, coolDown);
    }

    @Test
    public void testSmoothWarmingUpDoSetRate() {
        RateLimiter.SleepingStopwatch stopwatch = RateLimiter.SleepingStopwatch.createFromSystemTimer();
        SmoothRateLimiter.SmoothWarmingUp limiter = new SmoothRateLimiter.SmoothWarmingUp(stopwatch, 1000, TimeUnit.MILLISECONDS, 3.0);

        limiter.doSetRate(5.0, 200000); // 5 permits per second

        Assertions.assertTrue(limiter.maxPermits > 0);
        //    Assertions.assertTrue(limiter.thresholdPermits > 0);
        //    Assertions.assertTrue(limiter.slope != 0);
    }

    @Test
    public void testSmoothWarmingUpStoredPermitsToWaitTime() {
        RateLimiter.SleepingStopwatch stopwatch = RateLimiter.SleepingStopwatch.createFromSystemTimer();
        SmoothRateLimiter.SmoothWarmingUp limiter = new SmoothRateLimiter.SmoothWarmingUp(stopwatch, 1000, TimeUnit.MILLISECONDS, 3.0);
        limiter.doSetRate(5.0, 200000);

        long waitTime = limiter.storedPermitsToWaitTime(limiter.maxPermits, 1.0);
        Assertions.assertTrue(waitTime >= 0);
    }

    @Test
    public void testSmoothWarmingUpCoolDownIntervalMicros() {
        RateLimiter.SleepingStopwatch stopwatch = RateLimiter.SleepingStopwatch.createFromSystemTimer();
        SmoothRateLimiter.SmoothWarmingUp limiter = new SmoothRateLimiter.SmoothWarmingUp(stopwatch, 1000, TimeUnit.MILLISECONDS, 3.0);
        limiter.doSetRate(5.0, 200000);

        double coolDown = limiter.coolDownIntervalMicros();
        Assertions.assertTrue(coolDown > 0);
    }

    @Test
    public void testResync() {
        RateLimiter.SleepingStopwatch stopwatch = RateLimiter.SleepingStopwatch.createFromSystemTimer();
        SmoothRateLimiter.SmoothBursty limiter = new SmoothRateLimiter.SmoothBursty(stopwatch, 1.0);
        limiter.doSetRate(1.0, TimeUnit.SECONDS.toMicros(1));

        long nowMicros = stopwatch.readMicros();
        limiter.resync(nowMicros + TimeUnit.SECONDS.toMicros(2)); // 2 seconds later

        // Should have accumulated permits
        Assertions.assertTrue(limiter.storedPermits > 0);
    }
}
package com.landawn.abacus.util;

import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import com.landawn.abacus.TestBase;

@Tag("new-test")
public class SmoothRateLimiter100Test extends TestBase {

    @Test
    public void testSmoothBurstyDoSetRate() {
        RateLimiter.SleepingStopwatch stopwatch = RateLimiter.SleepingStopwatch.createFromSystemTimer();
        SmoothRateLimiter.SmoothBursty limiter = new SmoothRateLimiter.SmoothBursty(stopwatch, 1.0);

        limiter.doSetRate(10.0, 100000);

        Assertions.assertEquals(10.0, limiter.maxPermits);
        Assertions.assertEquals(0.0, limiter.storedPermits);
    }

    @Test
    public void testSmoothBurstyStoredPermitsToWaitTime() {
        RateLimiter.SleepingStopwatch stopwatch = RateLimiter.SleepingStopwatch.createFromSystemTimer();
        SmoothRateLimiter.SmoothBursty limiter = new SmoothRateLimiter.SmoothBursty(stopwatch, 1.0);

        long waitTime = limiter.storedPermitsToWaitTime(5.0, 2.0);
        Assertions.assertEquals(0L, waitTime);
    }

    @Test
    public void testSmoothBurstyCoolDownIntervalMicros() {
        RateLimiter.SleepingStopwatch stopwatch = RateLimiter.SleepingStopwatch.createFromSystemTimer();
        SmoothRateLimiter.SmoothBursty limiter = new SmoothRateLimiter.SmoothBursty(stopwatch, 1.0);
        limiter.stableIntervalMicros = 100000;

        double coolDown = limiter.coolDownIntervalMicros();
        Assertions.assertEquals(100000, coolDown);
    }

    @Test
    public void testSmoothWarmingUpDoSetRate() {
        RateLimiter.SleepingStopwatch stopwatch = RateLimiter.SleepingStopwatch.createFromSystemTimer();
        SmoothRateLimiter.SmoothWarmingUp limiter = new SmoothRateLimiter.SmoothWarmingUp(stopwatch, 1000, TimeUnit.MILLISECONDS, 3.0);

        limiter.doSetRate(5.0, 200000);

        Assertions.assertTrue(limiter.maxPermits > 0);
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
        limiter.resync(nowMicros + TimeUnit.SECONDS.toMicros(2));

        Assertions.assertTrue(limiter.storedPermits > 0);
    }
}

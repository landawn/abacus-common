package com.landawn.abacus.util;

import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

public class SmoothRateLimiterTest extends TestBase {

    @Test
    public void testSmoothBurstyDoSetRate() {
        RateLimiter.SleepingStopwatch stopwatch = RateLimiter.SleepingStopwatch.createFromSystemTimer();
        SmoothRateLimiter.SmoothBursty limiter = new SmoothRateLimiter.SmoothBursty(stopwatch, 1.0);

        limiter.doSetRate(10.0, 100000);

        Assertions.assertEquals(10.0, limiter.maxPermits);
        Assertions.assertEquals(0.0, limiter.storedPermits);
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

        limiter.addStoredPermitsWaitTime(limiter.maxPermits, 1.0);
        Assertions.assertTrue(limiter.queryEarliestAvailable(0) > 0);
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
    public void testSmoothBurstyStoredPermitsToWaitTime() {
        RateLimiter.SleepingStopwatch stopwatch = RateLimiter.SleepingStopwatch.createFromSystemTimer();
        SmoothRateLimiter.SmoothBursty limiter = new SmoothRateLimiter.SmoothBursty(stopwatch, 1.0);

        limiter.addStoredPermitsWaitTime(5.0, 2.0);
        Assertions.assertEquals(0L, limiter.queryEarliestAvailable(0));
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
    public void testResync() {
        RateLimiter.SleepingStopwatch stopwatch = RateLimiter.SleepingStopwatch.createFromSystemTimer();
        SmoothRateLimiter.SmoothBursty limiter = new SmoothRateLimiter.SmoothBursty(stopwatch, 1.0);
        limiter.doSetRate(1.0, TimeUnit.SECONDS.toMicros(1));

        long nowMicros = stopwatch.readMicros();
        limiter.resync(nowMicros + TimeUnit.SECONDS.toMicros(2));

        Assertions.assertTrue(limiter.storedPermits > 0);
    }

    @Test
    public void testHugeReservationSaturatesInsteadOfWrappingSchedule() {
        SmoothRateLimiter limiter = (SmoothRateLimiter) RateLimiter.create(0.000001, Long.MAX_VALUE, TimeUnit.MICROSECONDS);

        limiter.reserve(Integer.MAX_VALUE);

        Assertions.assertEquals(Long.MAX_VALUE, limiter.queryEarliestAvailable(0));
    }

    @Test
    public void testSmoothWarmingUpRampIsMeasuredAboveThresholdPermits() {
        // permitsToTime(p) evaluates the climbing segment p permits ABOVE thresholdPermits: stableIntervalMicros
        // at 0 and exactly the cold interval at maxPermits - thresholdPermits. With warmupPeriod = 1,000,000us
        // and stableInterval = 200,000us, thresholdPermits = 0.5 * 1,000,000 / 200,000 = 2.5 and maxPermits =
        // 2.5 + 2 * 1,000,000 / (200,000 + 600,000) = 5.0. Each case measures the DEBT ADDED, because
        // doSetRate(rate, nowMicros) resyncs nextFreeTicketMicros to nowMicros first.
        final RateLimiter.SleepingStopwatch stopwatch = RateLimiter.SleepingStopwatch.createFromSystemTimer();

        final SmoothRateLimiter.SmoothWarmingUp full = new SmoothRateLimiter.SmoothWarmingUp(stopwatch, 1000, TimeUnit.MILLISECONDS, 3.0);
        full.doSetRate(5.0, 200000);
        Assertions.assertEquals(5.0, full.maxPermits);
        Assertions.assertEquals(200000.0, full.stableIntervalMicros);

        // draining every stored permit costs the ramp (warmupPeriod) plus the flat part (warmupPeriod / 2)
        long before = full.queryEarliestAvailable(0);
        full.addStoredPermitsWaitTime(full.maxPermits, full.maxPermits);
        Assertions.assertEquals(1500000L, full.queryEarliestAvailable(0) - before);

        // the ramp alone, i.e. maxPermits down to thresholdPermits, is exactly the warmup period - which holds
        // only because the interval at maxPermits - thresholdPermits is the cold interval, not the interval at
        // maxPermits
        final SmoothRateLimiter.SmoothWarmingUp ramp = new SmoothRateLimiter.SmoothWarmingUp(stopwatch, 1000, TimeUnit.MILLISECONDS, 3.0);
        ramp.doSetRate(5.0, 200000);
        before = ramp.queryEarliestAvailable(0);
        ramp.addStoredPermitsWaitTime(ramp.maxPermits, 2.5);
        Assertions.assertEquals(1000000L, ramp.queryEarliestAvailable(0) - before);

        // below thresholdPermits the function is flat at the stable interval
        final SmoothRateLimiter.SmoothWarmingUp flat = new SmoothRateLimiter.SmoothWarmingUp(stopwatch, 1000, TimeUnit.MILLISECONDS, 3.0);
        flat.doSetRate(5.0, 200000);
        before = flat.queryEarliestAvailable(0);
        flat.addStoredPermitsWaitTime(2.5, 2.5);
        Assertions.assertEquals(500000L, flat.queryEarliestAvailable(0) - before);
    }
}

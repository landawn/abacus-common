package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("unit")
public class RateFractionalDebtTest {
    private static class Clock extends RateLimiter.SleepingStopwatch {
        long now;
        long slept;

        @Override
        protected long readMicros() {
            return now;
        }

        @Override
        protected void sleepMicrosUninterruptibly(long micros) {
            assertTrue(micros >= 0);
            now = Numbers.saturatedAdd(now, micros);
            slept = Numbers.saturatedAdd(slept, micros);
        }
    }

    private static SmoothRateLimiter make(double rate, boolean warm, Clock clock) {
        return (SmoothRateLimiter) (warm ? RateLimiter.create(rate, 100, TimeUnit.MICROSECONDS, 3, clock) : RateLimiter.create(rate, clock));
    }

    private static BigDecimal fraction(SmoothRateLimiter limiter) throws Exception {
        BigDecimal result = BigDecimal.ZERO;
        for (String name : new String[] { "fractionalMicros", "fractionalMicrosLow" }) {
            Field field = SmoothRateLimiter.class.getDeclaredField(name);
            field.setAccessible(true);
            result = result.add(new BigDecimal(field.getDouble(limiter)));
        }
        assertTrue(result.signum() >= 0 && result.compareTo(BigDecimal.ONE) < 0, result.toString());
        return result;
    }

    @Test
    void singletonAndBatchCostsIncludeWarmupPieces() throws Exception {
        for (boolean warm : new boolean[] { false, true }) {
            for (double rate : new double[] { 2e6, 6e5, 3e6 }) {
                SmoothRateLimiter singles = make(rate, warm, new Clock());
                SmoothRateLimiter batch = make(rate, warm, new Clock());
                for (int i = 0; i < 200; i++) {
                    singles.reserveEarliestAvailable(1, 0);
                    fraction(singles);
                }
                batch.reserveEarliestAvailable(200, 0);
                assertEquals(batch.queryEarliestAvailable(0) + fraction(batch).doubleValue(),
                        singles.queryEarliestAvailable(0) + fraction(singles).doubleValue(), 1e-8);
            }
        }
        Clock clock = new Clock();
        SmoothRateLimiter limiter = make(2e6, false, clock);
        for (int i = 0; i < 200; i++) {
            limiter.acquire();
        }
        assertEquals(99, clock.slept);
        assertEquals(100, limiter.queryEarliestAvailable(0));
    }

    @Test
    void compensationIdleAndRateChangesPreserveSubMicrosecondDebt() throws Exception {
        SmoothRateLimiter tiny = make(1e6 / Math.nextDown(1d), false, new Clock());
        tiny.reserveEarliestAvailable(1, 0);
        tiny.setRate(1e23);
        for (int i = 0; i < 20; i++) {
            tiny.reserveEarliestAvailable(1, 0);
            fraction(tiny);
        }
        assertEquals(1, tiny.queryEarliestAvailable(0));
        SmoothRateLimiter idle = make(2e6, false, new Clock());
        idle.reserveEarliestAvailable(1, 0);
        idle.resync(1);
        assertEquals(1, idle.storedPermits);
        assertEquals(0, fraction(idle).signum());
        idle.resync(2000000);
        assertEquals(idle.maxPermits, idle.storedPermits);
        SmoothRateLimiter changed = make(2e6, false, new Clock());
        changed.reserveEarliestAvailable(1, 0);
        changed.setRate(4e6);
        changed.reserveEarliestAvailable(2, 0);
        assertEquals(1, changed.queryEarliestAvailable(0));
        assertEquals(0, fraction(changed).signum());
    }

    @Test
    void epochSaturationInfinityAndTimeouts() throws Exception {
        for (long epoch : new long[] { 1L << 54, Long.MAX_VALUE - 2 }) {
            Clock clock = new Clock();
            clock.now = epoch;
            SmoothRateLimiter limiter = make(2e6, false, clock);
            for (int i = 0; i < 200; i++) {
                limiter.reserveEarliestAvailable(1, epoch);
            }
            assertEquals(Numbers.saturatedAdd(epoch, 100), limiter.queryEarliestAvailable(0));
            assertEquals(0, fraction(limiter).signum());
        }
        for (boolean warm : new boolean[] { false, true }) {
            Clock clock = new Clock();
            SmoothRateLimiter unlimited = make(Double.POSITIVE_INFINITY, warm, clock);
            for (int i = 0; i < 200; i++) {
                unlimited.acquire();
            }
            assertEquals(0, clock.slept);
            assertEquals(0, unlimited.queryEarliestAvailable(0));
            SmoothRateLimiter saturated = make(Double.MIN_VALUE, warm, new Clock());
            saturated.reserveEarliestAvailable(1, 0);
            assertEquals(Long.MAX_VALUE, saturated.queryEarliestAvailable(0));
            assertEquals(0, fraction(saturated).signum());
        }
        SmoothRateLimiter timed = make(2e6, false, new Clock());
        assertTrue(timed.tryAcquire());
        assertTrue(timed.tryAcquire());
        assertFalse(timed.tryAcquire());
        assertEquals(1, timed.queryEarliestAvailable(0));
    }
}

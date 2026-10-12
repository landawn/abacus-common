package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import com.landawn.abacus.TestBase;

@Tag("unit")
class RateLimiterFiniteRescaleReview20261003Test extends TestBase {
    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    void repeatedHugeFiniteRateKeepsStoredPermitsFinite(boolean warmup) {
        Clock clock = new Clock();
        SmoothRateLimiter limiter = create(warmup, 1e200, clock);
        clock.now = 1_000_000;
        limiter.setRate(1e200);

        assertEquals(limiter.maxPermits, limiter.storedPermits);
        assertTrue(Double.isFinite(limiter.storedPermits));
    }

    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    void changedHugeFiniteRateKeepsProportionalStoredPermits(boolean warmup) {
        Clock clock = new Clock();
        SmoothRateLimiter limiter = create(warmup, 1e160, clock);
        clock.now = 1_000_000;
        limiter.setRate(1e200);

        assertEquals(limiter.maxPermits, limiter.storedPermits);
        assertTrue(Double.isFinite(limiter.storedPermits));
    }

    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    void restoredOrdinaryRateHasFiniteReservationSchedule(boolean warmup) {
        Clock clock = new Clock();
        RateLimiter limiter = create(warmup, 1e200, clock);
        clock.now = 1_000_000;
        // Consecutive calls may legally observe the same microsecond. Advancing the clock here
        // would resync and cap the corrupted store, masking the intermediate overflow.
        limiter.setRate(1e200);
        limiter.setRate(1);

        assertTrue(limiter.tryAcquire());
        if (warmup) {
            assertTrue(limiter.tryAcquire(1, 2, TimeUnit.SECONDS));
            assertEquals(2_500_000, clock.now);
        } else {
            assertTrue(limiter.tryAcquire());
            assertFalse(limiter.tryAcquire());
        }
    }

    @ParameterizedTest
    @CsvSource({ "false, 1e-200", "true, 1e-200", "false, 1e-160", "true, 1e-160" })
    void tinyFiniteRatePreservesStoreAcrossZeroOrSubnormalProduct(boolean warmup, double rate) {
        Clock clock = new Clock();
        SmoothRateLimiter limiter = create(warmup, rate, clock);
        clock.now = 1_000_000;
        limiter.setRate(rate);

        assertEquals(limiter.maxPermits, limiter.storedPermits);
        limiter.setRate(1);
        assertEquals(1, limiter.storedPermits);
    }

    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    void hugeFiniteRatePreservesPartialStore(boolean warmup) {
        assertPartialStore(warmup, 1e200);
    }

    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    void tinyFiniteRatePreservesPartialStore(boolean warmup) {
        assertPartialStore(warmup, 1e-160);
    }

    private static void assertPartialStore(boolean warmup, double rate) {
        Clock clock = new Clock();
        // Warmup starts cold/full; the documented infinity-to-finite transition gives it an empty
        // store so the subsequent quarter-second idle interval fills exactly a quarter of capacity.
        SmoothRateLimiter limiter = create(warmup, warmup ? Double.POSITIVE_INFINITY : rate, clock);
        if (warmup) {
            limiter.setRate(rate);
        }
        clock.now = 250_000;
        limiter.setRate(rate);

        assertEquals(limiter.maxPermits / 4, limiter.storedPermits);
        limiter.setRate(4);
        assertEquals(1, limiter.storedPermits);
    }

    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    void ordinaryRateChangePreservesPartialStore(boolean warmup) {
        Clock clock = new Clock();
        SmoothRateLimiter limiter = create(warmup, 10, clock);
        clock.now = 1_000_000;
        limiter.setRate(10);
        assertTrue(limiter.tryAcquire(5));
        limiter.setRate(20);

        assertEquals(20, limiter.maxPermits);
        assertEquals(10, limiter.storedPermits);
    }

    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    void directHugeToOrdinaryTransitionRemainsFinite(boolean warmup) {
        Clock clock = new Clock();
        SmoothRateLimiter limiter = create(warmup, 1e200, clock);
        clock.now = 1_000_000;
        limiter.setRate(1);

        assertEquals(1, limiter.maxPermits);
        assertEquals(1, limiter.storedPermits);
    }

    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    void positiveInfinityRemainsAcceptedAndCanReturnToFiniteRate(boolean warmup) {
        Clock clock = new Clock();
        SmoothRateLimiter limiter = create(warmup, Double.POSITIVE_INFINITY, clock);
        assertTrue(limiter.tryAcquire(Integer.MAX_VALUE));
        limiter.setRate(2);

        assertEquals(2, limiter.getRate());
        assertEquals(warmup ? 0 : 2, limiter.storedPermits);
        assertTrue(limiter.tryAcquire());
    }

    private static SmoothRateLimiter create(boolean warmup, double rate, Clock clock) {
        return (SmoothRateLimiter) (warmup ? RateLimiter.create(rate, 1, TimeUnit.SECONDS, 3, clock) : RateLimiter.create(rate, clock));
    }

    private static final class Clock extends RateLimiter.SleepingStopwatch {
        long now;

        @Override
        protected long readMicros() {
            return now;
        }

        @Override
        protected void sleepMicrosUninterruptibly(long micros) {
            now += micros;
        }
    }
}

package com.landawn.abacus.util;

import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

@Tag("new-test")
public class RateLimiter100Test extends TestBase {

    @Test
    public void testCreate() {
        RateLimiter limiter = RateLimiter.create(10.0);
        Assertions.assertNotNull(limiter);
        Assertions.assertEquals(10.0, limiter.getRate(), 0.01);
    }

    @Test
    public void testCreateWithWarmup() {
        RateLimiter limiter = RateLimiter.create(5.0, 1, TimeUnit.SECONDS);
        Assertions.assertNotNull(limiter);
        Assertions.assertEquals(5.0, limiter.getRate(), 0.01);
    }

    @Test
    public void testCreateWithNegativeWarmup() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            RateLimiter.create(5.0, -1, TimeUnit.SECONDS);
        });
    }

    @Test
    public void testSetRate() {
        RateLimiter limiter = RateLimiter.create(10.0);
        limiter.setRate(20.0);
        Assertions.assertEquals(20.0, limiter.getRate(), 0.01);
    }

    @Test
    public void testSetRateInvalid() {
        RateLimiter limiter = RateLimiter.create(10.0);

        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            limiter.setRate(0.0);
        });

        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            limiter.setRate(-1.0);
        });
    }

    @Test
    public void testGetRate() {
        RateLimiter limiter = RateLimiter.create(15.5);
        Assertions.assertEquals(15.5, limiter.getRate(), 0.01);
    }

    @Test
    public void testAcquire() {
        RateLimiter limiter = RateLimiter.create(1000.0);
        double waitTime = limiter.acquire();
        Assertions.assertTrue(waitTime >= 0);
    }

    @Test
    public void testAcquireMultiple() {
        RateLimiter limiter = RateLimiter.create(1000.0);
        double waitTime = limiter.acquire(5);
        Assertions.assertTrue(waitTime >= 0);
    }

    @Test
    public void testTryAcquireWithTimeout() {
        RateLimiter limiter = RateLimiter.create(1.0);
        boolean acquired = limiter.tryAcquire(100, TimeUnit.MILLISECONDS);
        Assertions.assertTrue(acquired);
    }

    @Test
    public void testTryAcquireMultiple() {
        RateLimiter limiter = RateLimiter.create(1000.0);
        boolean acquired = limiter.tryAcquire(5);
        Assertions.assertTrue(acquired);
    }

    @Test
    public void testTryAcquireNoWait() {
        RateLimiter limiter = RateLimiter.create(1000.0);
        boolean acquired = limiter.tryAcquire();
        Assertions.assertTrue(acquired);
    }

    @Test
    public void testTryAcquireMultipleWithTimeout() {
        RateLimiter limiter = RateLimiter.create(1000.0);
        boolean acquired = limiter.tryAcquire(5, 100, TimeUnit.MILLISECONDS);
        Assertions.assertTrue(acquired);
    }

    @Test
    public void testToString() {
        RateLimiter limiter = RateLimiter.create(10.0);
        String str = limiter.toString();
        Assertions.assertTrue(str.contains("10.0"));
        Assertions.assertTrue(str.contains("RateLimiter"));
    }
}

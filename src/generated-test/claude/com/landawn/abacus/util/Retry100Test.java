package com.landawn.abacus.util;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

public class Retry100Test extends TestBase {

    @Test
    public void testRetryOf_WithRetryCondition() {
        Retry<Void> retry = Retry.of(2, 100, e -> e instanceof RuntimeException);
        Assertions.assertNotNull(retry);
    }

    @Test
    public void testRetryOf_WithBiPredicate() {
        Retry<String> retry = Retry.of(2, 100, (result, exception) -> exception != null || "retry".equals(result));
        Assertions.assertNotNull(retry);
    }

    @Test
    public void testRun_SuccessOnFirstTry() throws Exception {
        AtomicInteger counter = new AtomicInteger(0);
        Retry<Void> retry = Retry.of(3, 50, e -> true);
        
        retry.run(() -> {
            counter.incrementAndGet();
        });
        
        Assertions.assertEquals(1, counter.get());
    }

    @Test
    public void testRun_SuccessAfterRetry() throws Exception {
        AtomicInteger counter = new AtomicInteger(0);
        Retry<Void> retry = Retry.of(3, 50, e -> e instanceof RuntimeException);
        
        retry.run(() -> {
            if (counter.incrementAndGet() < 3) {
                throw new RuntimeException("Fail");
            }
        });
        
        Assertions.assertEquals(3, counter.get());
    }

    @Test
    public void testRun_FailureAfterAllRetries() {
        AtomicInteger counter = new AtomicInteger(0);
        Retry<Void> retry = Retry.of(2, 50, e -> e instanceof RuntimeException);
        
        Assertions.assertThrows(RuntimeException.class, () -> {
            retry.run(() -> {
                counter.incrementAndGet();
                throw new RuntimeException("Always fail");
            });
        });
        
        Assertions.assertEquals(3, counter.get()); // 1 initial + 2 retries
    }

    @Test
    public void testCall_SuccessOnFirstTry() throws Exception {
        Retry<String> retry = Retry.of(3, 50, (r, e) -> r == null);
        
        String result = retry.call(() -> "success");
        
        Assertions.assertEquals("success", result);
    }

    @Test
    public void testCall_SuccessAfterRetry() throws Exception {
        AtomicInteger counter = new AtomicInteger(0);
        Retry<String> retry = Retry.of(3, 50, (r, e) -> e instanceof RuntimeException);
        
        String result = retry.call(() -> {
            if (counter.incrementAndGet() < 3) {
                throw new RuntimeException("Fail");
            }
            return "success";
        });
        
        Assertions.assertEquals("success", result);
        Assertions.assertEquals(3, counter.get());
    }

    @Test
    public void testCall_WithBiPredicateRetryCondition() throws Exception {
        AtomicInteger counter = new AtomicInteger(0);
        Retry<String> retry = Retry.of(3, 50, (result, exception) -> 
            exception != null || "retry".equals(result));
        
        String result = retry.call(() -> {
            counter.incrementAndGet();
            if (counter.get() < 3) {
                return "retry";
            }
            return "success";
        });
        
        Assertions.assertEquals("success", result);
        Assertions.assertEquals(3, counter.get());
    }

    @Test
    public void testCall_WithZeroRetries() throws Exception {
        Retry<String> retry = Retry.of(0, 0, (r, e) -> true);
        
        String result = retry.call(() -> "success");
        
        Assertions.assertEquals("success", result);
    }

    @Test
    public void testRun_WithZeroRetries() throws Exception {
        AtomicInteger counter = new AtomicInteger(0);
        Retry<Void> retry = Retry.of(0, 0, e -> true);
        
        retry.run(() -> {
            counter.incrementAndGet();
        });
        
        Assertions.assertEquals(1, counter.get());
    }

    // Tests for Retry.R inner class

    @Test
    public void testNegativeRetryTimes() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            Retry.of(-1, 100, e -> true);
        });
    }

    @Test
    public void testNegativeRetryInterval() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            Retry.of(2, -1, e -> true);
        });
    }

    @Test
    public void testNullRetryCondition() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            Retry.of(2, 100, (Predicate<? super Exception>) null);
        });
    }
}
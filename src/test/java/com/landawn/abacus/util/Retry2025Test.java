package com.landawn.abacus.util;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

@Tag("2025")
public class Retry2025Test extends TestBase {

    @Test
    public void testOf_WithPredicate_ValidParameters() {
        Retry<Void> retry = Retry.of(3, 1000, e -> e instanceof IOException);
        Assertions.assertNotNull(retry);
    }

    @Test
    public void testOf_WithPredicate_ZeroRetryTimes() {
        Retry<Void> retry = Retry.of(0, 1000, e -> e instanceof IOException);
        Assertions.assertNotNull(retry);
    }

    @Test
    public void testOf_WithPredicate_ZeroRetryInterval() {
        Retry<Void> retry = Retry.of(3, 0, e -> e instanceof IOException);
        Assertions.assertNotNull(retry);
    }

    @Test
    public void testOf_WithPredicate_NegativeRetryTimes() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            Retry.of(-1, 1000, e -> e instanceof IOException);
        });
    }

    @Test
    public void testOf_WithPredicate_NegativeRetryInterval() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            Retry.of(3, -1, e -> e instanceof IOException);
        });
    }

    @Test
    public void testOf_WithPredicate_NullRetryCondition() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            Retry.of(3, 1000, (Predicate) null);
        });
    }

    @Test
    public void testOf_WithBiPredicate_ValidParameters() {
        Retry<String> retry = Retry.of(3, 1000, (result, ex) -> result == null || ex instanceof IOException);
        Assertions.assertNotNull(retry);
    }

    @Test
    public void testOf_WithBiPredicate_ZeroRetryTimes() {
        Retry<String> retry = Retry.of(0, 1000, (result, ex) -> result == null);
        Assertions.assertNotNull(retry);
    }

    @Test
    public void testOf_WithBiPredicate_ZeroRetryInterval() {
        Retry<String> retry = Retry.of(3, 0, (result, ex) -> result == null);
        Assertions.assertNotNull(retry);
    }

    @Test
    public void testOf_WithBiPredicate_NegativeRetryTimes() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            Retry.of(-1, 1000, (result, ex) -> result == null);
        });
    }

    @Test
    public void testOf_WithBiPredicate_NegativeRetryInterval() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            Retry.of(3, -1, (result, ex) -> result == null);
        });
    }

    @Test
    public void testOf_WithBiPredicate_NullRetryCondition() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            Retry.of(3, 1000, (java.util.function.BiPredicate<String, Exception>) null);
        });
    }

    @Test
    public void testRun_SuccessOnFirstAttempt() throws Exception {
        Retry<Void> retry = Retry.of(3, 100, e -> e instanceof IOException);
        AtomicInteger counter = new AtomicInteger(0);

        retry.run(() -> {
            counter.incrementAndGet();
        });

        Assertions.assertEquals(1, counter.get());
    }

    @Test
    public void testRun_SuccessOnSecondAttempt() throws Exception {
        Retry<Void> retry = Retry.of(3, 50, e -> e instanceof IOException);
        AtomicInteger counter = new AtomicInteger(0);

        retry.run(() -> {
            int count = counter.incrementAndGet();
            if (count < 2) {
                throw new IOException("Attempt " + count + " failed");
            }
        });

        Assertions.assertEquals(2, counter.get());
    }

    @Test
    public void testRun_SuccessOnThirdAttempt() throws Exception {
        Retry<Void> retry = Retry.of(3, 50, e -> e instanceof IOException);
        AtomicInteger counter = new AtomicInteger(0);

        retry.run(() -> {
            int count = counter.incrementAndGet();
            if (count < 3) {
                throw new IOException("Attempt " + count + " failed");
            }
        });

        Assertions.assertEquals(3, counter.get());
    }

    @Test
    public void testRun_FailAfterAllRetries() {
        Retry<Void> retry = Retry.of(2, 50, e -> e instanceof IOException);
        AtomicInteger counter = new AtomicInteger(0);

        IOException exception = Assertions.assertThrows(IOException.class, () -> {
            retry.run(() -> {
                counter.incrementAndGet();
                throw new IOException("Always fails");
            });
        });

        Assertions.assertEquals("Always fails", exception.getMessage());
        Assertions.assertEquals(3, counter.get());
    }

    @Test
    public void testRun_NoRetryOnNonMatchingException() {
        Retry<Void> retry = Retry.of(3, 50, e -> e instanceof IOException);
        AtomicInteger counter = new AtomicInteger(0);

        RuntimeException exception = Assertions.assertThrows(RuntimeException.class, () -> {
            retry.run(() -> {
                counter.incrementAndGet();
                throw new RuntimeException("Non-matching exception");
            });
        });

        Assertions.assertEquals("Non-matching exception", exception.getMessage());
        Assertions.assertEquals(1, counter.get());
    }

    @Test
    public void testRun_WithZeroRetryTimes() throws Exception {
        Retry<Void> retry = Retry.of(0, 50, e -> e instanceof IOException);
        AtomicInteger counter = new AtomicInteger(0);

        retry.run(() -> {
            counter.incrementAndGet();
        });

        Assertions.assertEquals(1, counter.get());
    }

    @Test
    public void testRun_WithZeroRetryTimes_ThrowsException() {
        Retry<Void> retry = Retry.of(0, 50, e -> e instanceof IOException);

        Assertions.assertThrows(IOException.class, () -> {
            retry.run(() -> {
                throw new IOException("Fails immediately");
            });
        });
    }

    @Test
    public void testRun_WithBiPredicateRetryCondition() throws Exception {
        Retry<Void> retry = Retry.of(3, 50, (result, ex) -> ex instanceof IOException);
        AtomicInteger counter = new AtomicInteger(0);

        retry.run(() -> {
            int count = counter.incrementAndGet();
            if (count < 2) {
                throw new IOException("Attempt " + count + " failed");
            }
        });

        Assertions.assertEquals(2, counter.get());
    }

    @Test
    public void testRun_WithZeroRetryInterval() throws Exception {
        Retry<Void> retry = Retry.of(2, 0, e -> e instanceof IOException);
        AtomicInteger counter = new AtomicInteger(0);

        long startTime = System.currentTimeMillis();
        retry.run(() -> {
            int count = counter.incrementAndGet();
            if (count < 2) {
                throw new IOException("Attempt " + count + " failed");
            }
        });
        long endTime = System.currentTimeMillis();

        Assertions.assertEquals(2, counter.get());
        Assertions.assertTrue(endTime - startTime < 500);
    }

    @Test
    public void testCall_SuccessOnFirstAttempt() throws Exception {
        Retry<String> retry = Retry.of(3, 100, (result, ex) -> result == null || ex instanceof IOException);
        AtomicInteger counter = new AtomicInteger(0);

        String result = retry.call(() -> {
            counter.incrementAndGet();
            return "Success";
        });

        Assertions.assertEquals("Success", result);
        Assertions.assertEquals(1, counter.get());
    }

    @Test
    public void testCall_SuccessOnSecondAttempt() throws Exception {
        Retry<String> retry = Retry.of(3, 50, (result, ex) -> result == null || ex instanceof IOException);
        AtomicInteger counter = new AtomicInteger(0);

        String result = retry.call(() -> {
            int count = counter.incrementAndGet();
            if (count < 2) {
                throw new IOException("Attempt " + count + " failed");
            }
            return "Success";
        });

        Assertions.assertEquals("Success", result);
        Assertions.assertEquals(2, counter.get());
    }

    @Test
    public void testCall_SuccessOnThirdAttempt() throws Exception {
        Retry<String> retry = Retry.of(3, 50, (result, ex) -> result == null || ex instanceof IOException);
        AtomicInteger counter = new AtomicInteger(0);

        String result = retry.call(() -> {
            int count = counter.incrementAndGet();
            if (count < 3) {
                throw new IOException("Attempt " + count + " failed");
            }
            return "Success";
        });

        Assertions.assertEquals("Success", result);
        Assertions.assertEquals(3, counter.get());
    }

    @Test
    public void testCall_FailAfterAllRetries() {
        Retry<String> retry = Retry.of(2, 50, (result, ex) -> result == null || ex instanceof IOException);
        AtomicInteger counter = new AtomicInteger(0);

        IOException exception = Assertions.assertThrows(IOException.class, () -> {
            retry.call(() -> {
                counter.incrementAndGet();
                throw new IOException("Always fails");
            });
        });

        Assertions.assertEquals("Always fails", exception.getMessage());
        Assertions.assertEquals(3, counter.get());
    }

    @Test
    public void testCall_NoRetryOnNonMatchingException() {
        Retry<String> retry = Retry.of(3, 50, (result, ex) -> ex instanceof IOException);
        AtomicInteger counter = new AtomicInteger(0);

        RuntimeException exception = Assertions.assertThrows(RuntimeException.class, () -> {
            retry.call(() -> {
                counter.incrementAndGet();
                throw new RuntimeException("Non-matching exception");
            });
        });

        Assertions.assertEquals("Non-matching exception", exception.getMessage());
        Assertions.assertEquals(1, counter.get());
    }

    @Test
    public void testCall_RetryOnUnsatisfactoryResult() throws Exception {
        Retry<String> retry = Retry.of(3, 50, (result, ex) -> result == null || result.equals("Bad"));
        AtomicInteger counter = new AtomicInteger(0);

        String result = retry.call(() -> {
            int count = counter.incrementAndGet();
            if (count < 3) {
                return "Bad";
            }
            return "Good";
        });

        Assertions.assertEquals("Good", result);
        Assertions.assertEquals(3, counter.get());
    }

    @Test
    public void testCall_RetryOnNullResult() throws Exception {
        Retry<String> retry = Retry.of(3, 50, (result, ex) -> result == null);
        AtomicInteger counter = new AtomicInteger(0);

        String result = retry.call(() -> {
            int count = counter.incrementAndGet();
            if (count < 2) {
                return null;
            }
            return "Success";
        });

        Assertions.assertEquals("Success", result);
        Assertions.assertEquals(2, counter.get());
    }

    @Test
    public void testCall_FailsAfterRetriesWithUnsatisfactoryResult() {
        Retry<String> retry = Retry.of(2, 50, (result, ex) -> result == null);
        AtomicInteger counter = new AtomicInteger(0);

        RuntimeException exception = Assertions.assertThrows(RuntimeException.class, () -> {
            retry.call(() -> {
                counter.incrementAndGet();
                return null;
            });
        });

        Assertions.assertTrue(exception.getMessage().contains("Still failed after retried"));
        Assertions.assertTrue(exception.getMessage().contains("2 times"));
        Assertions.assertEquals(3, counter.get());
    }

    @Test
    public void testCall_WithZeroRetryTimes() throws Exception {
        Retry<String> retry = Retry.of(0, 50, (result, ex) -> result == null);
        AtomicInteger counter = new AtomicInteger(0);

        String result = retry.call(() -> {
            counter.incrementAndGet();
            return "Success";
        });

        Assertions.assertEquals("Success", result);
        Assertions.assertEquals(1, counter.get());
    }

    @Test
    public void testCall_WithZeroRetryTimes_ThrowsException() {
        Retry<String> retry = Retry.of(0, 50, (result, ex) -> ex instanceof IOException);

        Assertions.assertThrows(IOException.class, () -> {
            retry.call(() -> {
                throw new IOException("Fails immediately");
            });
        });
    }

    @Test
    public void testCall_WithPredicateRetryCondition() throws Exception {
        Retry<String> retry = Retry.of(3, 50, (String result, Exception ex) -> ex instanceof IOException);
        AtomicInteger counter = new AtomicInteger(0);

        String result = retry.call(() -> {
            int count = counter.incrementAndGet();
            if (count < 2) {
                throw new IOException("Attempt " + count + " failed");
            }
            return "Success";
        });

        Assertions.assertEquals("Success", result);
        Assertions.assertEquals(2, counter.get());
    }

    @Test
    public void testCall_WithZeroRetryInterval() throws Exception {
        Retry<String> retry = Retry.of(2, 0, (result, ex) -> result == null || ex instanceof IOException);
        AtomicInteger counter = new AtomicInteger(0);

        long startTime = System.currentTimeMillis();
        String result = retry.call(() -> {
            int count = counter.incrementAndGet();
            if (count < 2) {
                throw new IOException("Attempt " + count + " failed");
            }
            return "Success";
        });
        long endTime = System.currentTimeMillis();

        Assertions.assertEquals("Success", result);
        Assertions.assertEquals(2, counter.get());
        Assertions.assertTrue(endTime - startTime < 500);
    }

    @Test
    public void testCall_ExceptionThenSuccessfulResult() throws Exception {
        Retry<Integer> retry = Retry.of(3, 50, (result, ex) -> (result != null && result < 100) || ex instanceof IOException);
        AtomicInteger counter = new AtomicInteger(0);

        Integer result = retry.call(() -> {
            int count = counter.incrementAndGet();
            if (count == 1) {
                throw new IOException("First attempt fails");
            }
            if (count == 2) {
                return 50;
            }
            return 150;
        });

        Assertions.assertEquals(150, result);
        Assertions.assertEquals(3, counter.get());
    }

    @Test
    public void testCall_MixedExceptionAndResultRetries() throws Exception {
        Retry<String> retry = Retry.of(5, 30, (result, ex) -> (result != null && result.startsWith("Retry")) || ex instanceof IOException);
        AtomicInteger counter = new AtomicInteger(0);

        String result = retry.call(() -> {
            int count = counter.incrementAndGet();
            switch (count) {
                case 1:
                    throw new IOException("First fails");
                case 2:
                    return "Retry-1";
                case 3:
                    throw new IOException("Third fails");
                case 4:
                    return "Retry-2";
                default:
                    return "Success";
            }
        });

        Assertions.assertEquals("Success", result);
        Assertions.assertEquals(5, counter.get());
    }

    @Test
    public void testCall_ReturnNullWithNullConditionFalse() throws Exception {
        Retry<String> retry = Retry.of(3, 50, (result, ex) -> false);

        String result = retry.call(() -> null);

        Assertions.assertNull(result);
    }

    @Test
    public void testCall_ExceptionChangesAcrossRetries() throws Exception {
        Retry<String> retry = Retry.of(3, 50, (result, ex) -> ex instanceof IOException || ex instanceof IllegalStateException);
        AtomicInteger counter = new AtomicInteger(0);

        String result = retry.call(() -> {
            int count = counter.incrementAndGet();
            if (count == 1) {
                throw new IOException("IOException");
            }
            if (count == 2) {
                throw new IllegalStateException("IllegalStateException");
            }
            return "Success";
        });

        Assertions.assertEquals("Success", result);
        Assertions.assertEquals(3, counter.get());
    }

    @Test
    public void testRun_MultipleExceptionTypes() throws Exception {
        Retry<Void> retry = Retry.of(3, 50, e -> e instanceof IOException || e instanceof IllegalArgumentException);
        AtomicInteger counter = new AtomicInteger(0);

        retry.run(() -> {
            int count = counter.incrementAndGet();
            if (count == 1) {
                throw new IOException("First attempt");
            }
            if (count == 2) {
                throw new IllegalArgumentException("Second attempt");
            }
        });

        Assertions.assertEquals(3, counter.get());
    }

    @Test
    public void testCall_AllRetriesThrowDifferentExceptions() {
        Retry<String> retry = Retry.of(2, 50, (result, ex) -> ex instanceof Exception);
        AtomicInteger counter = new AtomicInteger(0);

        Exception exception = Assertions.assertThrows(IllegalStateException.class, () -> {
            retry.call(() -> {
                int count = counter.incrementAndGet();
                if (count == 1) {
                    throw new IOException("First");
                } else if (count == 2) {
                    throw new IllegalArgumentException("Second");
                } else {
                    throw new IllegalStateException("Third");
                }
            });
        });

        Assertions.assertEquals("Third", exception.getMessage());
        Assertions.assertEquals(3, counter.get());
    }
}

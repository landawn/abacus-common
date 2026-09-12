package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiPredicate;
import java.util.function.Predicate;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.exception.RetryExhaustedException;

public class RetryTest extends TestBase {

    @Test
    public void testWithFixedDelay() throws Exception {
        Retry<Void> onException = Retry.withFixedDelay(2, 0, e -> e instanceof IOException);
        assertNotNull(onException);
        AtomicInteger runs = new AtomicInteger();
        onException.run(() -> {
            if (runs.incrementAndGet() < 2) {
                throw new IOException("fail");
            }
        });
        assertEquals(2, runs.get());

        Retry<String> onResult = Retry.withFixedDelay(2, 0, (result, ex) -> "bad".equals(result) || ex instanceof IOException);
        AtomicInteger calls = new AtomicInteger();
        assertEquals("good", onResult.call(() -> calls.incrementAndGet() < 2 ? "bad" : "good"));
        assertEquals(2, calls.get());

        assertNotNull(Retry.withFixedDelay(0, 0, e -> e instanceof IOException));
        assertNotNull(Retry.withFixedDelay(0, 1000, (result, ex) -> result == null));
    }

    @Test
    public void testWithFixedDelay_Invalid() {
        assertThrows(IllegalArgumentException.class, () -> Retry.withFixedDelay(-1, 1000, e -> e instanceof IOException));
        assertThrows(IllegalArgumentException.class, () -> Retry.withFixedDelay(3, -1, e -> e instanceof IOException));
        assertThrows(IllegalArgumentException.class, () -> Retry.withFixedDelay(3, 1000, (Predicate<? super Exception>) null));
        assertThrows(IllegalArgumentException.class, () -> Retry.withFixedDelay(-1, 1000, (result, ex) -> result == null));
        assertThrows(IllegalArgumentException.class, () -> Retry.withFixedDelay(3, -1, (result, ex) -> result == null));
        assertThrows(IllegalArgumentException.class, () -> Retry.withFixedDelay(3, 1000, (BiPredicate<String, Exception>) null));
    }

    @Test
    public void testRun() throws Exception {
        AtomicInteger first = new AtomicInteger();
        Retry.withFixedDelay(3, 0, e -> e instanceof IOException).run(first::incrementAndGet);
        assertEquals(1, first.get());

        AtomicInteger retried = new AtomicInteger();
        Retry.withFixedDelay(3, 0, e -> e instanceof IOException).run(() -> {
            if (retried.incrementAndGet() < 3) {
                throw new IOException("Attempt " + retried.get() + " failed");
            }
        });
        assertEquals(3, retried.get());

        AtomicInteger always = new AtomicInteger();
        IOException exhausted = assertThrows(IOException.class, () -> Retry.withFixedDelay(2, 0, e -> e instanceof IOException).run(() -> {
            always.incrementAndGet();
            throw new IOException("Always fails");
        }));
        assertEquals("Always fails", exhausted.getMessage());
        assertEquals(3, always.get());

        AtomicInteger unmatched = new AtomicInteger();
        RuntimeException runtime = assertThrows(RuntimeException.class, () -> Retry.withFixedDelay(3, 0, e -> e instanceof IOException).run(() -> {
            unmatched.incrementAndGet();
            throw new RuntimeException("Non-matching exception");
        }));
        assertEquals("Non-matching exception", runtime.getMessage());
        assertEquals(1, unmatched.get());

        AtomicInteger mixed = new AtomicInteger();
        Retry.withFixedDelay(3, 0, e -> e instanceof IOException || e instanceof IllegalArgumentException).run(() -> {
            int count = mixed.incrementAndGet();
            if (count == 1) {
                throw new IOException("First attempt");
            }
            if (count == 2) {
                throw new IllegalArgumentException("Second attempt");
            }
        });
        assertEquals(3, mixed.get());

        AtomicInteger bi = new AtomicInteger();
        Retry.<Void> withFixedDelay(3, 0, (result, ex) -> ex instanceof IOException).run(() -> {
            if (bi.incrementAndGet() < 2) {
                throw new IOException("fail");
            }
        });
        assertEquals(2, bi.get());
    }

    @Test
    public void testRun_EdgeCase() throws Exception {
        AtomicInteger zero = new AtomicInteger();
        Retry.withFixedDelay(0, 0, e -> e instanceof IOException).run(zero::incrementAndGet);
        assertEquals(1, zero.get());
        assertThrows(IOException.class, () -> Retry.withFixedDelay(0, 0, e -> e instanceof IOException).run(() -> {
            throw new IOException("Fails immediately");
        }));

        AtomicInteger noDelay = new AtomicInteger();
        long start = System.currentTimeMillis();
        Retry.withFixedDelay(2, 0, e -> e instanceof IOException).run(() -> {
            if (noDelay.incrementAndGet() < 2) {
                throw new IOException("fail");
            }
        });
        assertEquals(2, noDelay.get());
        assertTrue(System.currentTimeMillis() - start < 500);

        Retry<String> retry = Retry.withFixedDelay(1, 0, (result, ex) -> false);
        assertThrows(IllegalArgumentException.class, () -> retry.run(null));
        assertThrows(IllegalArgumentException.class, () -> retry.call(null));

        AtomicInteger biFail = new AtomicInteger();
        assertThrows(IOException.class, () -> Retry.<Void> withFixedDelay(2, 0, (result, ex) -> ex instanceof IOException).run(() -> {
            biFail.incrementAndGet();
            throw new IOException("always");
        }));
        assertEquals(3, biFail.get());

        AtomicInteger biUnmatched = new AtomicInteger();
        assertThrows(RuntimeException.class, () -> Retry.<Void> withFixedDelay(3, 0, (result, ex) -> ex instanceof IOException).run(() -> {
            biUnmatched.incrementAndGet();
            throw new RuntimeException("not retryable");
        }));
        assertEquals(1, biUnmatched.get());
    }

    @Test
    public void testCall() throws Exception {
        AtomicInteger first = new AtomicInteger();
        assertEquals("Success", Retry.<String> withFixedDelay(3, 0, (result, ex) -> result == null || ex instanceof IOException).call(() -> {
            first.incrementAndGet();
            return "Success";
        }));
        assertEquals(1, first.get());

        AtomicInteger retried = new AtomicInteger();
        assertEquals("Success", Retry.<String> withFixedDelay(3, 0, (result, ex) -> result == null || ex instanceof IOException).call(() -> {
            if (retried.incrementAndGet() < 3) {
                throw new IOException("Attempt " + retried.get() + " failed");
            }
            return "Success";
        }));
        assertEquals(3, retried.get());

        AtomicInteger always = new AtomicInteger();
        IOException exhausted = assertThrows(IOException.class,
                () -> Retry.<String> withFixedDelay(2, 0, (result, ex) -> result == null || ex instanceof IOException).call(() -> {
                    always.incrementAndGet();
                    throw new IOException("Always fails");
                }));
        assertEquals("Always fails", exhausted.getMessage());
        assertEquals(3, always.get());

        AtomicInteger unmatched = new AtomicInteger();
        RuntimeException runtime = assertThrows(RuntimeException.class,
                () -> Retry.<String> withFixedDelay(3, 0, (result, ex) -> ex instanceof IOException).call(() -> {
                    unmatched.incrementAndGet();
                    throw new RuntimeException("Non-matching exception");
                }));
        assertEquals("Non-matching exception", runtime.getMessage());
        assertEquals(1, unmatched.get());

        AtomicInteger badResult = new AtomicInteger();
        assertEquals("Good", Retry.<String> withFixedDelay(3, 0, (result, ex) -> result == null || "Bad".equals(result)).call(() -> {
            return badResult.incrementAndGet() < 3 ? "Bad" : "Good";
        }));
        assertEquals(3, badResult.get());

        AtomicInteger mixed = new AtomicInteger();
        assertEquals(Integer.valueOf(150),
                Retry.<Integer> withFixedDelay(3, 0, (result, ex) -> (result != null && result < 100) || ex instanceof IOException).call(() -> {
                    int count = mixed.incrementAndGet();
                    if (count == 1) {
                        throw new IOException("First attempt fails");
                    }
                    if (count == 2) {
                        return 50;
                    }
                    return 150;
                }));
        assertEquals(3, mixed.get());
    }

    @Test
    public void testCall_EdgeCase() throws Exception {
        AtomicInteger nullThenValue = new AtomicInteger();
        assertEquals("Success", Retry.<String> withFixedDelay(3, 0, (result, ex) -> result == null).call(() -> {
            return nullThenValue.incrementAndGet() < 2 ? null : "Success";
        }));
        assertEquals(2, nullThenValue.get());
        assertNull(Retry.<String> withFixedDelay(3, 0, (result, ex) -> false).call(() -> null));

        AtomicInteger rejected = new AtomicInteger();
        RuntimeException stillMatched = assertThrows(RuntimeException.class,
                () -> Retry.<String> withFixedDelay(2, 0, (result, ex) -> result == null).call(() -> {
                    rejected.incrementAndGet();
                    return null;
                }));
        assertTrue(stillMatched.getMessage().contains("still matched the retry condition"));
        assertTrue(stillMatched.getMessage().contains("3 attempts"));
        assertTrue(stillMatched.getMessage().contains("2 retries"));
        assertEquals(3, rejected.get());

        AtomicInteger predicateCalls = new AtomicInteger();
        RuntimeException alwaysRejected = assertThrows(RuntimeException.class,
                () -> Retry.<String> withFixedDelay(2, 0, (result, ex) -> predicateCalls.incrementAndGet() <= 3).call(() -> "rejected"));
        assertTrue(alwaysRejected.getMessage().contains("3 attempts"));
        assertEquals(3, predicateCalls.get());

        AtomicInteger stale = new AtomicInteger();
        IOException firstFailure = new IOException("first attempt");
        RuntimeException rejectedAfterException = assertThrows(RuntimeException.class,
                () -> Retry.<String> withFixedDelay(1, 0, (result, ex) -> ex != null || "rejected".equals(result)).call(() -> {
                    if (stale.getAndIncrement() == 0) {
                        throw firstFailure;
                    }
                    return "rejected";
                }));
        assertFalse(rejectedAfterException.getMessage().contains("rejected"));
        assertNull(rejectedAfterException.getCause());
        assertArrayEquals(new Throwable[] { firstFailure }, rejectedAfterException.getSuppressed());
        assertEquals(2, stale.get());

        AtomicInteger callableCalls = new AtomicInteger();
        AtomicInteger throwingPredicateCalls = new AtomicInteger();
        IllegalStateException predicateFailure = new IllegalStateException("predicate failure");
        IllegalStateException thrown = assertThrows(IllegalStateException.class, () -> Retry.<String> withFixedDelay(2, 0, (result, ex) -> {
            throwingPredicateCalls.incrementAndGet();
            if (ex == null) {
                throw predicateFailure;
            }
            return true;
        }).call(() -> {
            callableCalls.incrementAndGet();
            return "result";
        }));
        assertSame(predicateFailure, thrown);
        assertEquals(1, callableCalls.get());
        assertEquals(1, throwingPredicateCalls.get());

        assertThrows(RetryExhaustedException.class, () -> Retry.<String> withFixedDelay(0, 0, (r, e) -> true).call(() -> "success"));
        assertEquals("Success", Retry.<String> withFixedDelay(0, 0, (result, ex) -> result == null).call(() -> "Success"));
        assertThrows(IOException.class, () -> Retry.<String> withFixedDelay(0, 0, (result, ex) -> ex instanceof IOException).call(() -> {
            throw new IOException("Fails immediately");
        }));

        AtomicInteger changing = new AtomicInteger();
        assertEquals("Success",
                Retry.<String> withFixedDelay(3, 0, (result, ex) -> ex instanceof IOException || ex instanceof IllegalStateException).call(() -> {
                    int count = changing.incrementAndGet();
                    if (count == 1) {
                        throw new IOException("IOException");
                    }
                    if (count == 2) {
                        throw new IllegalStateException("IllegalStateException");
                    }
                    return "Success";
                }));
        assertEquals(3, changing.get());

        AtomicInteger lastWins = new AtomicInteger();
        assertEquals("Third",
                assertThrows(IllegalStateException.class, () -> Retry.<String> withFixedDelay(2, 0, (result, ex) -> ex instanceof Exception).call(() -> {
                    int count = lastWins.incrementAndGet();
                    if (count == 1) {
                        throw new IOException("First");
                    }
                    if (count == 2) {
                        throw new IllegalArgumentException("Second");
                    }
                    throw new IllegalStateException("Third");
                })).getMessage());
        assertEquals(3, lastWins.get());
    }
}

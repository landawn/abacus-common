package com.landawn.abacus.util;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.exception.RetryExhaustedException;

/**
 * Covers the {@link Retry} fixes from the 2026-09-01 review:
 *
 * <ul>
 *   <li><b>B1</b> - a retry predicate that throws no longer destroys the operation's own exception.</li>
 *   <li><b>B4</b> - result exhaustion raises a dedicated {@link RetryExhaustedException} carrying the attempt
 *       counts, with an earlier operation failure attached as suppressed history rather than as the cause.</li>
 *   <li><b>D6</b> - {@code withFixedDelay(int, long, Predicate)} is generic, so a failure-only policy can drive a
 *       value-returning {@code call}.</li>
 * </ul>
 */
public class RetryRegressionTest extends TestBase {

    // ------------------------------------------------------------------------------------------------------
    // B1: a throwing retry predicate keeps the operation exception
    // ------------------------------------------------------------------------------------------------------

    @Test
    public void testRun_ThrowingExceptionPredicate_KeepsOperationExceptionAsSuppressed() {
        final IOException operationFailure = new IOException("original operation failure");
        final IllegalStateException predicateFailure = new IllegalStateException("predicate blew up");

        final Retry<Void> retry = Retry.withFixedDelay(2, 0, e -> {
            throw predicateFailure;
        });

        final IllegalStateException thrown = Assertions.assertThrows(IllegalStateException.class, () -> retry.run(() -> {
            throw operationFailure;
        }));

        Assertions.assertSame(predicateFailure, thrown);
        Assertions.assertArrayEquals(new Throwable[] { operationFailure }, thrown.getSuppressed());
    }

    @Test
    public void testCall_ThrowingExceptionPredicate_KeepsOperationExceptionAsSuppressed() {
        final IOException operationFailure = new IOException("original operation failure");

        final Retry<String> retry = Retry.withFixedDelay(2, 0, (final String result, final Exception ex) -> {
            throw new IllegalStateException("predicate blew up");
        });

        final IllegalStateException thrown = Assertions.assertThrows(IllegalStateException.class, () -> retry.call(() -> {
            throw operationFailure;
        }));

        Assertions.assertEquals("predicate blew up", thrown.getMessage());
        Assertions.assertArrayEquals(new Throwable[] { operationFailure }, thrown.getSuppressed());
    }

    @Test
    public void testRun_ThrowingPredicate_PropagatesPredicateFailureAsPrimary() {
        // The documented contract: the predicate failure stays primary because it is the configuration defect that
        // aborted the retry loop. Only the *loss* of the operation exception was the bug.
        final Retry<Void> retry = Retry.withFixedDelay(3, 0, e -> {
            throw new IllegalArgumentException("bad predicate");
        });

        final Throwable thrown = Assertions.assertThrows(IllegalArgumentException.class, () -> retry.run(() -> {
            throw new IOException("op");
        }));

        Assertions.assertNull(thrown.getCause());
        Assertions.assertEquals(1, thrown.getSuppressed().length);
        Assertions.assertInstanceOf(IOException.class, thrown.getSuppressed()[0]);
    }

    @Test
    public void testRun_ThrowingPredicate_ErrorIsAlsoAugmentedAndRethrown() {
        final IOException operationFailure = new IOException("op");
        final Retry<Void> retry = Retry.withFixedDelay(2, 0, e -> {
            throw new StackOverflowError("predicate blew the stack");
        });

        final StackOverflowError thrown = Assertions.assertThrows(StackOverflowError.class, () -> retry.run(() -> {
            throw operationFailure;
        }));

        Assertions.assertArrayEquals(new Throwable[] { operationFailure }, thrown.getSuppressed());
    }

    @Test
    public void testRun_ThrowingPredicate_DoesNotSelfSuppress() {
        // A predicate that rethrows the very exception it was handed must not be self-suppressed
        // (Throwable.addSuppressed throws IllegalArgumentException for that).
        final Retry<Void> retry = Retry.withFixedDelay(2, 0, e -> {
            throw (RuntimeException) e;
        });

        final RuntimeException operationFailure = new RuntimeException("thrown, then rethrown by the predicate");

        final RuntimeException thrown = Assertions.assertThrows(RuntimeException.class, () -> retry.run(() -> {
            throw operationFailure;
        }));

        Assertions.assertSame(operationFailure, thrown);
        Assertions.assertEquals(0, thrown.getSuppressed().length);
    }

    @Test
    public void testRun_ThrowingPredicate_DoesNotRunAnotherAttempt() {
        final AtomicInteger attempts = new AtomicInteger();
        final Retry<Void> retry = Retry.withFixedDelay(5, 0, e -> {
            throw new IllegalStateException("predicate blew up");
        });

        Assertions.assertThrows(IllegalStateException.class, () -> retry.run(() -> {
            attempts.incrementAndGet();
            throw new IOException("op");
        }));

        Assertions.assertEquals(1, attempts.get());
    }

    @Test
    public void testCall_ThrowingResultPredicate_PropagatesWithoutAnotherAttempt() {
        // No attempt throws in this scenario, so there is no EARLIER operation exception and nothing is
        // attached; the predicate's failure must simply propagate and must not trigger another invocation.
        // When an earlier attempt did throw, that exception IS attached - see Retry.shouldRetryResult and
        // testCall_ThrowingResultPredicate_AfterAnEarlierFailure_KeepsItAsSuppressed.
        final AtomicInteger attempts = new AtomicInteger();
        final Retry<String> retry = Retry.withFixedDelay(3, 0, (final String result, final Exception ex) -> {
            throw new IllegalStateException("result predicate blew up");
        });

        final IllegalStateException thrown = Assertions.assertThrows(IllegalStateException.class, () -> retry.call(() -> {
            attempts.incrementAndGet();
            return "v";
        }));

        Assertions.assertEquals("result predicate blew up", thrown.getMessage());
        Assertions.assertEquals(0, thrown.getSuppressed().length);
        Assertions.assertEquals(1, attempts.get());
    }

    @Test
    public void testRun_ThrowingBiPredicate_AlsoKeepsOperationExceptionAsSuppressed() {
        // run() consults retryCondition2 with (null, exception) when the policy was built from a BiPredicate.
        // That branch of shouldRetryAfter needs the same protection as the Predicate branch.
        final IOException operationFailure = new IOException("original operation failure");
        final Retry<Void> retry = Retry.withFixedDelay(2, 0, (final Void result, final Exception ex) -> {
            throw new IllegalStateException("bi-predicate blew up");
        });

        final IllegalStateException thrown = Assertions.assertThrows(IllegalStateException.class, () -> retry.run(() -> {
            throw operationFailure;
        }));

        Assertions.assertEquals("bi-predicate blew up", thrown.getMessage());
        Assertions.assertArrayEquals(new Throwable[] { operationFailure }, thrown.getSuppressed());
    }

    @Test
    public void testRun_BiPredicateSeesNullResultAndTheException() throws Exception {
        // The unchanged contract, re-asserted because shouldRetryAfter now owns this call.
        final List<Object[]> seen = new ArrayList<>();
        final AtomicInteger attempts = new AtomicInteger();
        final Retry<Void> retry = Retry.withFixedDelay(2, 0, (final Void result, final Exception ex) -> {
            seen.add(new Object[] { result, ex });
            return true;
        });

        retry.run(() -> {
            if (attempts.incrementAndGet() < 2) {
                throw new IOException("attempt " + attempts.get());
            }
        });

        Assertions.assertEquals(1, seen.size());
        Assertions.assertNull(seen.get(0)[0]);
        Assertions.assertInstanceOf(IOException.class, seen.get(0)[1]);
    }

    @Test
    public void testRun_PredicateSeesEachAttemptsOwnExceptionAndTheLatestIsRethrown() {
        // The loop reassigns `ex` on every failed retry; shouldRetryAfter must be asked about the *latest*
        // failure, and rejecting it must rethrow that one rather than the first.
        final List<String> asked = new ArrayList<>();
        final AtomicInteger attempts = new AtomicInteger();
        final Retry<Void> retry = Retry.withFixedDelay(5, 0, e -> {
            asked.add(e.getMessage());
            return "retryable".equals(e.getMessage());
        });

        final IOException thrown = Assertions.assertThrows(IOException.class, () -> retry.run(() -> {
            throw new IOException(attempts.incrementAndGet() <= 2 ? "retryable" : "fatal");
        }));

        Assertions.assertEquals("fatal", thrown.getMessage());
        Assertions.assertEquals(List.of("retryable", "retryable", "fatal"), asked);
        Assertions.assertEquals(3, attempts.get());
    }

    @Test
    public void testExceptionPredicateIsNotConsultedOnTheTerminalAttempt() {
        // Unchanged behaviour, re-asserted because both call sites now route through shouldRetryAfter:
        // for an operation that always fails, the predicate is asked at most retryTimes times, not retryTimes + 1.
        final AtomicInteger runPredicateCalls = new AtomicInteger();
        final AtomicInteger runAttempts = new AtomicInteger();
        final Retry<Void> voidRetry = Retry.withFixedDelay(2, 0, e -> {
            runPredicateCalls.incrementAndGet();
            return true;
        });

        Assertions.assertThrows(IOException.class, () -> voidRetry.run(() -> {
            runAttempts.incrementAndGet();
            throw new IOException("always");
        }));

        Assertions.assertEquals(3, runAttempts.get());
        Assertions.assertEquals(2, runPredicateCalls.get());

        final AtomicInteger callPredicateCalls = new AtomicInteger();
        final AtomicInteger callAttempts = new AtomicInteger();
        final Retry<String> valueRetry = Retry.withFixedDelay(2, 0, e -> {
            callPredicateCalls.incrementAndGet();
            return true;
        });

        Assertions.assertThrows(IOException.class, () -> valueRetry.call(() -> {
            callAttempts.incrementAndGet();
            throw new IOException("always");
        }));

        Assertions.assertEquals(3, callAttempts.get());
        Assertions.assertEquals(2, callPredicateCalls.get(), "call() must not ask one more time than run()");
    }

    @Test
    public void testCall_ResultExhaustion_MessageNeverContainsNull() {
        final Retry<String> retry = Retry.withFixedDelay(1, 0, (final String result, final Exception ex) -> true);

        final RetryExhaustedException thrown = Assertions.assertThrows(RetryExhaustedException.class, () -> retry.call(() -> null));

        Assertions.assertFalse(thrown.getMessage().contains("null"), thrown.getMessage());
        Assertions.assertEquals(2, thrown.attempts());
    }

    @Test
    public void testWellBehavedPredicate_IsUnaffected() throws Exception {
        final AtomicInteger attempts = new AtomicInteger();
        final Retry<Void> retry = Retry.withFixedDelay(3, 0, e -> e instanceof IOException);

        retry.run(() -> {
            if (attempts.incrementAndGet() < 3) {
                throw new IOException("attempt " + attempts.get());
            }
        });

        Assertions.assertEquals(3, attempts.get());
    }

    // ------------------------------------------------------------------------------------------------------
    // B4: RetryExhaustedException
    // ------------------------------------------------------------------------------------------------------

    @Test
    public void testCall_ResultExhaustion_ThrowsRetryExhaustedExceptionWithCounts() {
        final AtomicInteger calls = new AtomicInteger();
        final Retry<String> retry = Retry.withFixedDelay(2, 0, (final String result, final Exception ex) -> result == null);

        final RetryExhaustedException thrown = Assertions.assertThrows(RetryExhaustedException.class, () -> retry.call(() -> {
            calls.incrementAndGet();
            return null;
        }));

        Assertions.assertEquals(3, calls.get());
        Assertions.assertEquals(3, thrown.attempts());
        Assertions.assertEquals(2, thrown.retries());
        Assertions.assertTrue(thrown.getMessage().contains("3 attempts"));
        Assertions.assertTrue(thrown.getMessage().contains("2 retries"));
    }

    @Test
    public void testRetryExhaustedException_IsStillARuntimeException() {
        // Existing callers catch RuntimeException; the dedicated type must remain assignable to it.
        final Retry<String> retry = Retry.withFixedDelay(1, 0, (final String result, final Exception ex) -> true);

        final RuntimeException thrown = Assertions.assertThrows(RuntimeException.class, () -> retry.call(() -> "rejected"));

        Assertions.assertInstanceOf(RetryExhaustedException.class, thrown);
        Assertions.assertInstanceOf(IllegalStateException.class, thrown);
    }

    @Test
    public void testCall_ResultExhaustion_DoesNotLeakTheRejectedResult() {
        final String secret = "s3cr3t-token-value";
        final Retry<String> retry = Retry.withFixedDelay(1, 0, (final String result, final Exception ex) -> true);

        final RetryExhaustedException thrown = Assertions.assertThrows(RetryExhaustedException.class, () -> retry.call(() -> secret));

        Assertions.assertFalse(thrown.getMessage().contains(secret));
    }

    @Test
    public void testCall_ResultExhaustion_NoEarlierFailure_HasNoCauseAndNoSuppressed() {
        final Retry<String> retry = Retry.withFixedDelay(2, 0, (final String result, final Exception ex) -> result == null);

        final RetryExhaustedException thrown = Assertions.assertThrows(RetryExhaustedException.class, () -> retry.call(() -> null));

        Assertions.assertNull(thrown.getCause());
        Assertions.assertEquals(0, thrown.getSuppressed().length);
    }

    @Test
    public void testCall_ResultExhaustion_EarlierFailureIsSuppressedNotCause() {
        // Attempt 1 throws (retryable), attempts 2 and 3 return a rejected result. The earlier IOException is
        // history, not the reason the final result was rejected, so it must be suppressed rather than the cause.
        final AtomicInteger attempts = new AtomicInteger();
        final Retry<String> retry = Retry.withFixedDelay(2, 0, (final String result, final Exception ex) -> result == null || ex != null);

        final RetryExhaustedException thrown = Assertions.assertThrows(RetryExhaustedException.class, () -> retry.call(() -> {
            if (attempts.incrementAndGet() == 1) {
                throw new IOException("first attempt failed");
            }
            return null;
        }));

        Assertions.assertEquals(3, attempts.get());
        Assertions.assertNull(thrown.getCause());
        Assertions.assertEquals(1, thrown.getSuppressed().length);
        Assertions.assertInstanceOf(IOException.class, thrown.getSuppressed()[0]);
        Assertions.assertEquals("first attempt failed", thrown.getSuppressed()[0].getMessage());
    }

    @Test
    public void testCall_FinalAttemptThrows_StillRethrowsThatExceptionNotRetryExhausted() {
        final Retry<String> retry = Retry.withFixedDelay(1, 0, (final String result, final Exception ex) -> true);

        final IOException thrown = Assertions.assertThrows(IOException.class, () -> retry.call(() -> {
            throw new IOException("always fails");
        }));

        Assertions.assertEquals("always fails", thrown.getMessage());
    }

    @Test
    public void testRetryExhaustedException_AccessorsAndConstructor() {
        final RetryExhaustedException e = new RetryExhaustedException("msg", 4, 3);

        Assertions.assertEquals("msg", e.getMessage());
        Assertions.assertEquals(4, e.attempts());
        Assertions.assertEquals(3, e.retries());
        Assertions.assertEquals(e.retries() + 1, e.attempts());
    }

    // ------------------------------------------------------------------------------------------------------
    // D6: the exception-only factory is generic
    // ------------------------------------------------------------------------------------------------------

    @Test
    public void testWithFixedDelay_PredicateFactory_IsGeneric() throws Exception {
        final AtomicInteger attempts = new AtomicInteger();
        final Retry<String> retry = Retry.withFixedDelay(3, 0, e -> e instanceof IOException);

        final String result = retry.call(() -> {
            if (attempts.incrementAndGet() < 3) {
                throw new IOException("attempt " + attempts.get());
            }
            return "success";
        });

        Assertions.assertEquals("success", result);
        Assertions.assertEquals(3, attempts.get());
    }

    @Test
    public void testWithFixedDelay_PredicateFactory_StillInfersVoid() throws Exception {
        // The previous signature returned Retry<Void>; that assignment must keep compiling and behaving.
        final Retry<Void> retry = Retry.withFixedDelay(1, 0, e -> e instanceof IOException);
        final AtomicInteger attempts = new AtomicInteger();

        retry.run(attempts::incrementAndGet);

        Assertions.assertEquals(1, attempts.get());
    }

    @Test
    public void testWithFixedDelay_PredicateFactory_ResultIsNeverInspected() throws Exception {
        // A failure-only policy accepts any result, including null, without consulting anything.
        final Retry<String> retry = Retry.withFixedDelay(2, 0, e -> true);

        Assertions.assertNull(retry.call(() -> null));
    }

    @Test
    public void testWithFixedDelay_PredicateFactory_NonMatchingExceptionIsRethrownImmediately() {
        final AtomicInteger attempts = new AtomicInteger();
        final Retry<String> retry = Retry.withFixedDelay(3, 0, e -> e instanceof IOException);

        Assertions.assertThrows(IllegalStateException.class, () -> retry.call(() -> {
            attempts.incrementAndGet();
            throw new IllegalStateException("not retryable");
        }));

        Assertions.assertEquals(1, attempts.get());
    }

    @Test
    public void testWithFixedDelay_PredicateFactory_ExplicitPredicateVariableStillResolves() throws Exception {
        final Predicate<Exception> condition = e -> e instanceof IOException;
        final Retry<Integer> retry = Retry.withFixedDelay(1, 0, condition);

        Assertions.assertEquals(42, retry.call(() -> 42));
    }

    // ------------------------------------------------------------------------------------------------------
    // Argument validation still uses the shared name constants (O2)
    // ------------------------------------------------------------------------------------------------------

    @Test
    public void testValidationMessagesNameTheArguments() {
        Assertions.assertTrue(
                Assertions.assertThrows(IllegalArgumentException.class, () -> Retry.withFixedDelay(-1, 0, e -> true)).getMessage().contains("retryTimes"));

        Assertions.assertTrue(Assertions.assertThrows(IllegalArgumentException.class, () -> Retry.withFixedDelay(1, -1, e -> true))
                .getMessage()
                .contains("retryIntervalInMillis"));

        Assertions.assertTrue(Assertions.assertThrows(IllegalArgumentException.class, () -> Retry.withFixedDelay(1, 0, (Predicate<Exception>) null))
                .getMessage()
                .contains("retryCondition"));
    }

    // ------------------------------------------------------------------------------------------------------
    // Interruption behaviour is unchanged by the above
    // ------------------------------------------------------------------------------------------------------

    @Test
    public void testInterruptionStillAbandonsTheLoopAndPreservesBothExceptions() throws Exception {
        final Retry<Void> retry = Retry.withFixedDelay(5, 2_000, e -> true);
        final IOException operationFailure = new IOException("always");

        Thread.currentThread().interrupt();
        try {
            final IOException thrown = Assertions.assertThrows(IOException.class, () -> retry.run(() -> {
                throw operationFailure;
            }));

            Assertions.assertSame(operationFailure, thrown);
            Assertions.assertEquals(1, thrown.getSuppressed().length);
            Assertions.assertInstanceOf(InterruptedException.class, thrown.getSuppressed()[0]);
            Assertions.assertTrue(Thread.currentThread().isInterrupted());
        } finally {
            // Clear the flag so it cannot leak into whatever runs next on this thread.
            Thread.interrupted();
        }
    }

    @Test
    public void testCall_ThrowingResultPredicate_AfterAnEarlierFailure_KeepsItAsSuppressed() {
        // Attempt 1 throws (retryable); attempt 2 returns a value whose *result* predicate then blows up.
        // The result predicate used to be called inline, outside any catch, so the IOException vanished -
        // even though the neighbouring RetryExhaustedException branch preserves it in exactly this state.
        final IOException operationFailure = new IOException("first attempt failed");
        final AtomicInteger attempts = new AtomicInteger();
        final Retry<String> retry = Retry.withFixedDelay(3, 0, (final String result, final Exception ex) -> {
            if (result != null) {
                throw new IllegalStateException("result predicate blew up");
            }
            return true;
        });

        final IllegalStateException thrown = Assertions.assertThrows(IllegalStateException.class, () -> retry.call(() -> {
            if (attempts.incrementAndGet() == 1) {
                throw operationFailure;
            }
            return "v";
        }));

        Assertions.assertEquals("result predicate blew up", thrown.getMessage());
        Assertions.assertArrayEquals(new Throwable[] { operationFailure }, thrown.getSuppressed());
        Assertions.assertNull(thrown.getCause());
        Assertions.assertEquals(2, attempts.get());
    }

    @Test
    public void testCall_ResultPredicateThrowingAnError_AlsoKeepsTheOperationException() {
        // The guard catches RuntimeException | Error, like shouldRetryAfter does.
        final IOException operationFailure = new IOException("first attempt failed");
        final AtomicInteger attempts = new AtomicInteger();
        final Retry<String> retry = Retry.withFixedDelay(3, 0, (final String result, final Exception ex) -> {
            if (result != null) {
                throw new StackOverflowError("result predicate blew up");
            }
            return true;
        });

        final StackOverflowError thrown = Assertions.assertThrows(StackOverflowError.class, () -> retry.call(() -> {
            if (attempts.incrementAndGet() == 1) {
                throw operationFailure;
            }
            return "v";
        }));

        Assertions.assertArrayEquals(new Throwable[] { operationFailure }, thrown.getSuppressed());
    }

    @Test
    public void testCall_ResultPredicateRethrowingTheOperationException_DoesNotSelfSuppress() {
        // addSuppressed(this) throws IllegalArgumentException, so the identity guard is load-bearing.
        final RuntimeException operationFailure = new IllegalStateException("op");
        final AtomicInteger attempts = new AtomicInteger();
        final Retry<String> retry = Retry.withFixedDelay(3, 0, (final String result, final Exception ex) -> {
            if (result != null) {
                throw operationFailure;
            }
            return true;
        });

        final IllegalStateException thrown = Assertions.assertThrows(IllegalStateException.class, () -> retry.call(() -> {
            if (attempts.incrementAndGet() == 1) {
                throw operationFailure;
            }
            return "v";
        }));

        Assertions.assertSame(operationFailure, thrown);
        Assertions.assertEquals(0, thrown.getSuppressed().length);
    }

    @Test
    public void testCall_ResultExhaustion_MessageWordingIsPinnedIncludingTheUngrammatical1Attempts() {
        // The " attempts" token is how callers (and RetryAttemptCountTest:49) locate the count in the message,
        // so it stays fixed even for a single attempt, where it reads "1 attempts".
        final Retry<String> single = Retry.withFixedDelay(0, 0, (final String result, final Exception ex) -> true);
        final RetryExhaustedException one = Assertions.assertThrows(RetryExhaustedException.class, () -> single.call(() -> "x"));

        Assertions.assertEquals("The final result still matched the retry condition after 1 attempts (0 retries)", one.getMessage());
        Assertions.assertEquals(1, one.attempts());
        Assertions.assertEquals(0, one.retries());

        final Retry<String> twice = Retry.withFixedDelay(2, 0, (final String result, final Exception ex) -> true);
        final RetryExhaustedException three = Assertions.assertThrows(RetryExhaustedException.class, () -> twice.call(() -> "x"));

        Assertions.assertEquals("The final result still matched the retry condition after 3 attempts (2 retries)", three.getMessage());
        Assertions.assertEquals(3, three.attempts());
        Assertions.assertEquals(2, three.retries());
    }

    @Test
    public void testCall_PredicateThrowingACachedException_MutatesThatSharedInstanceOnEveryCall() {
        // Pins the caveat documented on call()'s @throws RuntimeException: the operation's exception is attached
        // to the very instance the predicate threw, so a predicate that throws a shared or cached exception
        // collects one more suppressed entry on every call. That is why a predicate must throw a fresh one.
        final IllegalStateException cachedPredicateFailure = new IllegalStateException("cached predicate failure");
        final Retry<String> retry = Retry.withFixedDelay(3, 0, (final String result, final Exception ex) -> {
            if (result != null) {
                throw cachedPredicateFailure;
            }
            return true;
        });

        for (int call = 1; call <= 3; call++) {
            final AtomicInteger attempts = new AtomicInteger();
            final IllegalStateException thrown = Assertions.assertThrows(IllegalStateException.class, () -> retry.call(() -> {
                if (attempts.incrementAndGet() == 1) {
                    throw new IOException("first attempt failed");
                }
                return "v";
            }));

            Assertions.assertSame(cachedPredicateFailure, thrown);
            Assertions.assertEquals(call, cachedPredicateFailure.getSuppressed().length);
        }
    }
}

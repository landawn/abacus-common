package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.StringWriter;
import java.net.SocketTimeoutException;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Comparator;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.ToIntFunction;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.u.Nullable;
import com.landawn.abacus.util.u.OptionalInt;

/**
 * Covers the fixes applied after the 2026-08-31 review of Index / Median / Wrapper / Indexed / Keyed /
 * Timed / IndexedKeyed / TypeReference / Clazz / If / Try / Synchronized / Retry / Hashing / Hex /
 * DigestUtil.
 *
 * <p>Each test names the finding it locks in, so a future change that reintroduces one of these defects
 * fails with an explanation rather than a bare assertion error.</p>
 */
public class IndexMedianTryRetryFixesTest extends TestBase {

    @BeforeEach
    @AfterEach
    public void clearInterruptFlag() {
        // Several tests here interrupt the current thread on purpose. JUnit reuses threads, so a leaked
        // flag would make an unrelated test fail later in a very confusing way.
        Thread.interrupted();
    }

    // ---------------------------------------------------------------------------------------------
    // B3 - Retry: the delay between attempts must be interruptible
    // ---------------------------------------------------------------------------------------------

    @Test
    public void b3_run_alreadyInterruptedThreadDoesNotSleepThroughTheRetries() {
        final AtomicInteger attempts = new AtomicInteger();
        final Retry<Void> retry = Retry.withFixedDelay(4, 2_000, e -> true);

        Thread.currentThread().interrupt();

        final long startNanos = System.nanoTime();
        final RuntimeException thrown = assertThrows(RuntimeException.class, () -> retry.run(() -> {
            attempts.incrementAndGet();
            throw new RuntimeException("boom");
        }));
        final long elapsedMillis = (System.nanoTime() - startNanos) / 1_000_000;

        // 4 retries x 2s of uninterruptible sleep used to be unavoidable; now nothing is slept at all.
        assertTrue(elapsedMillis < 1_000, "should not have slept, but took " + elapsedMillis + "ms");
        assertEquals(1, attempts.get(), "no further attempt may start once the thread is interrupted");
        assertTrue(Thread.currentThread().isInterrupted(), "the interrupted status must be left set");
        assertEquals("boom", thrown.getMessage());
        assertEquals(1, thrown.getSuppressed().length);
        assertTrue(thrown.getSuppressed()[0] instanceof InterruptedException);
    }

    @Test
    public void b3_call_alreadyInterruptedThreadDoesNotSleepThroughTheRetries() {
        final AtomicInteger attempts = new AtomicInteger();
        final Retry<String> retry = Retry.withFixedDelay(4, 2_000, (result, ex) -> true);

        Thread.currentThread().interrupt();

        final long startNanos = System.nanoTime();
        final RuntimeException thrown = assertThrows(RuntimeException.class, () -> retry.call(() -> {
            attempts.incrementAndGet();
            throw new RuntimeException("boom");
        }));
        final long elapsedMillis = (System.nanoTime() - startNanos) / 1_000_000;

        assertTrue(elapsedMillis < 1_000, "should not have slept, but took " + elapsedMillis + "ms");
        assertEquals(1, attempts.get());
        assertTrue(Thread.currentThread().isInterrupted());
        assertEquals("boom", thrown.getMessage());
        assertEquals(1, thrown.getSuppressed().length);
        assertTrue(thrown.getSuppressed()[0] instanceof InterruptedException);
    }

    @Test
    public void b3_zeroIntervalRetryLoopIsAlsoCancellable() {
        // With no delay there is no Thread.sleep to interrupt, so the status has to be checked explicitly.
        final AtomicInteger attempts = new AtomicInteger();
        final Retry<Void> retry = Retry.withFixedDelay(5, 0, e -> true);

        Thread.currentThread().interrupt();

        assertThrows(RuntimeException.class, () -> retry.run(() -> {
            attempts.incrementAndGet();
            throw new RuntimeException("boom");
        }));

        assertEquals(1, attempts.get());
        assertTrue(Thread.currentThread().isInterrupted());
    }

    @Test
    public void b3_interruptRaisedDuringTheDelayStopsTheLoop() throws Exception {
        final AtomicInteger attempts = new AtomicInteger();
        final Retry<Void> retry = Retry.withFixedDelay(5, 30_000, e -> true);
        final Thread[] worker = new Thread[1];
        final Throwable[] caught = new Throwable[1];
        final boolean[] stillInterrupted = new boolean[1];

        final Thread t = new Thread(() -> {
            try {
                retry.run(() -> {
                    attempts.incrementAndGet();
                    throw new RuntimeException("boom");
                });
            } catch (final Throwable e) {
                caught[0] = e;
            }
            stillInterrupted[0] = Thread.currentThread().isInterrupted();
        });
        worker[0] = t;
        t.start();

        // Wait until the worker is parked in the 30s delay, then cancel it.
        while (attempts.get() < 1 || t.getState() != Thread.State.TIMED_WAITING) {
            Thread.sleep(5);
        }
        t.interrupt();
        t.join(10_000);

        assertFalse(t.isAlive(), "the worker should have abandoned the 30s delay immediately");
        assertEquals(1, attempts.get());
        assertNotNull(caught[0]);
        assertEquals("boom", caught[0].getMessage());
        assertEquals(1, caught[0].getSuppressed().length);
        assertTrue(caught[0].getSuppressed()[0] instanceof InterruptedException);
        assertTrue(stillInterrupted[0], "the worker's interrupted status must survive");
    }

    @Test
    public void b3_uninterruptedRetryStillSleepsAndSucceeds() throws Exception {
        final AtomicInteger attempts = new AtomicInteger();
        final Retry<Void> retry = Retry.withFixedDelay(3, 20, e -> true);

        final long startNanos = System.nanoTime();
        retry.run(() -> {
            if (attempts.incrementAndGet() < 3) {
                throw new RuntimeException("boom");
            }
        });
        final long elapsedMillis = (System.nanoTime() - startNanos) / 1_000_000;

        assertEquals(3, attempts.get());
        assertTrue(elapsedMillis >= 30, "two 20ms delays should have elapsed, but only " + elapsedMillis + "ms did");
        assertFalse(Thread.currentThread().isInterrupted());
    }

    // ---------------------------------------------------------------------------------------------
    // B4 - Retry: run and call must consult the exception predicate the same number of times
    // ---------------------------------------------------------------------------------------------

    @Test
    public void b4_runAndCallEvaluateTheExceptionPredicateTheSameNumberOfTimes() {
        for (final int retryTimes : new int[] { 0, 1, 3, 5 }) {
            final AtomicInteger runPredicateCalls = new AtomicInteger();
            final AtomicInteger runAttempts = new AtomicInteger();
            final Retry<Void> runRetry = Retry.withFixedDelay(retryTimes, 0, e -> {
                runPredicateCalls.incrementAndGet();
                return true;
            });
            assertThrows(RuntimeException.class, () -> runRetry.run(() -> {
                runAttempts.incrementAndGet();
                throw new RuntimeException("boom");
            }));

            final AtomicInteger callPredicateCalls = new AtomicInteger();
            final AtomicInteger callAttempts = new AtomicInteger();
            final Retry<Void> callRetry = Retry.withFixedDelay(retryTimes, 0, e -> {
                callPredicateCalls.incrementAndGet();
                return true;
            });
            assertThrows(RuntimeException.class, () -> callRetry.call(() -> {
                callAttempts.incrementAndGet();
                throw new RuntimeException("boom");
            }));

            assertEquals(retryTimes + 1, runAttempts.get(), "run attempts for retryTimes=" + retryTimes);
            assertEquals(retryTimes + 1, callAttempts.get(), "call attempts for retryTimes=" + retryTimes);
            assertEquals(retryTimes, runPredicateCalls.get(), "run predicate calls for retryTimes=" + retryTimes);
            assertEquals(retryTimes, callPredicateCalls.get(), "call predicate calls for retryTimes=" + retryTimes);
        }
    }

    @Test
    public void b4_predicateThrowingOnTheFinalAttemptCannotReplaceTheOperationException() {
        // The terminal attempt's exception is rethrown without consulting the predicate, so a predicate
        // that blows up "one call too late" can no longer hijack the failure the caller actually got.
        final IOException operationFailure = new IOException("the real failure");
        final AtomicInteger predicateCalls = new AtomicInteger();
        // Explodes only from its SECOND invocation on - i.e. only on the call that the old code made after
        // the terminal attempt and the new code does not make at all.
        // Declared as a variable so the invocation binds to the Predicate factory, not the BiPredicate one.
        final java.util.function.Predicate<Exception> explodingPredicate = e -> {
            if (predicateCalls.incrementAndGet() >= 2) {
                throw new IllegalStateException("predicate exploded");
            }
            return true;
        };
        final Retry<Void> retry = Retry.withFixedDelay(1, 0, explodingPredicate);

        final IOException thrown = assertThrows(IOException.class, () -> retry.call(() -> {
            throw operationFailure;
        }));

        // Before the fix this was an IllegalStateException("predicate exploded") - the predicate's own
        // failure had displaced the operation failure the caller actually needed to see.
        assertSame(operationFailure, thrown);
        assertEquals(1, predicateCalls.get(), "the terminal attempt's exception must not be tested");
    }

    @Test
    public void b4_biPredicateExceptionArmIsAlsoSkippedOnTheTerminalAttempt() {
        final AtomicInteger exceptionArmCalls = new AtomicInteger();
        final Retry<String> retry = Retry.withFixedDelay(2, 0, (result, ex) -> {
            if (ex != null) {
                exceptionArmCalls.incrementAndGet();
            }
            return true;
        });

        assertThrows(RuntimeException.class, () -> retry.call(() -> {
            throw new RuntimeException("boom");
        }));

        assertEquals(2, exceptionArmCalls.get(), "the terminal attempt's exception must not be tested");
    }

    // ---------------------------------------------------------------------------------------------
    // B5 - Retry: the rejected result must not be copied into the exception message
    // ---------------------------------------------------------------------------------------------

    @Test
    public void b5_rejectedResultIsNotLeakedIntoTheExceptionMessage() {
        final Retry<String> retry = Retry.withFixedDelay(2, 0, (result, ex) -> true);

        final RuntimeException thrown = assertThrows(RuntimeException.class, () -> retry.call(() -> "SECRET-PAYLOAD"));

        assertFalse(thrown.getMessage().contains("SECRET-PAYLOAD"), "the result must not reach the message: " + thrown.getMessage());
        assertTrue(thrown.getMessage().contains("3 attempts"));
        assertTrue(thrown.getMessage().contains("2 retries"));
        assertNull(thrown.getCause(), "no operation exception occurred, so there is no cause to attach");
    }

    @Test
    public void b5_mostRecentOperationExceptionIsStillAttachedAsSuppressedHistory() {
        final IOException firstFailure = new IOException("first attempt");
        final AtomicInteger calls = new AtomicInteger();
        final Retry<String> retry = Retry.withFixedDelay(1, 0, (result, ex) -> true);

        final RuntimeException thrown = assertThrows(RuntimeException.class, () -> retry.call(() -> {
            if (calls.getAndIncrement() == 0) {
                throw firstFailure;
            }
            return "rejected";
        }));

        // The earlier failure is still carried, but as suppressed history rather than as the cause: the final
        // attempt returned normally, so an earlier IOException did not cause its result to be rejected.
        assertNull(thrown.getCause());
        assertArrayEquals(new Throwable[] { firstFailure }, thrown.getSuppressed());
        assertFalse(thrown.getMessage().contains("rejected"));
    }

    // ---------------------------------------------------------------------------------------------
    // B6 - Median: the collection overloads must not trust a size() the iterator disagrees with
    // ---------------------------------------------------------------------------------------------

    /** A collection whose {@code size()} over-reports, as a concurrently shrinking one would. */
    private static final class OverReportingList extends AbstractList<String> {
        private final List<String> backing;
        private final int reportedSize;

        OverReportingList(final int reportedSize, final String... actual) {
            this.backing = new ArrayList<>(Arrays.asList(actual));
            this.reportedSize = reportedSize;
        }

        @Override
        public String get(final int index) {
            return backing.get(index);
        }

        @Override
        public int size() {
            return reportedSize;
        }

        @Override
        public Iterator<String> iterator() {
            return backing.iterator();
        }
    }

    @Test
    public void b6_overReportedSizeNoLongerBreaksTheOneElementBranch() {
        // Previously: NoSuchElementException from source.iterator().next().
        assertEquals(Pair.of("a", Nullable.<String> empty()), Median.of(new OverReportingList(1, "a")));
        assertEquals(Pair.of("a", Nullable.<String> empty()), Median.of(new OverReportingList(3, "a")));
    }

    @Test
    public void b6_overReportedSizeNoLongerBreaksTheTwoElementBranch() {
        // Previously: NoSuchElementException from the second iter.next().
        assertEquals(Pair.of("a", Nullable.of("b")), Median.of(new OverReportingList(2, "a", "b")));
        assertEquals(Pair.of("a", Nullable.of("b")), Median.of(new OverReportingList(5, "b", "a")));
    }

    @Test
    public void b6_overReportedSizeNoLongerReturnsASilentlyWrongMedian() {
        // Previously the worst case: size()==3 with two real elements returned (b, empty) - no error at
        // all, just the wrong answer. The right answer for {a, b} is the pair (a, b).
        assertEquals(Pair.of("a", Nullable.of("b")), Median.of(new OverReportingList(3, "a", "b")));
        // ... and size()==5 with three real elements is the single median of those three.
        assertEquals(Pair.of("b", Nullable.<String> empty()), Median.of(new OverReportingList(5, "c", "a", "b")));
    }

    @Test
    public void b6_overReportedSizeNoLongerOverrunsTheCopy() {
        // Previously: IndexOutOfBoundsException from copy.get(len / 2).
        assertEquals(Pair.of("b", Nullable.of("c")), Median.of(new OverReportingList(9, "d", "a", "b", "c")));
    }

    @Test
    public void b6_wellBehavedCollectionsAreUnaffected() {
        assertEquals(Pair.of(15, Nullable.<Integer> empty()), Median.of(Arrays.asList(10, 5, 20, 15, 25)));
        assertEquals(Pair.of(3, Nullable.of(5)), Median.of(Arrays.asList(5, 2, 8, 1, 9, 3)));
        assertEquals(Pair.of("banana", Nullable.of("cherry")), Median.of(Arrays.asList("zebra", "apple", "banana", "cherry")));
        assertEquals(Pair.of(20, Nullable.<Integer> empty()), Median.of(Arrays.asList(30, 10, 20)));
        assertEquals(Pair.of(10, Nullable.of(20)), Median.of(Arrays.asList(20, 10)));
        assertEquals(Pair.of(7, Nullable.<Integer> empty()), Median.of(Arrays.asList(7)));
        assertEquals(Pair.of("bee", Nullable.<String> empty()),
                Median.of(Arrays.asList("elephant", "ant", "bee", "tiger"), 1, 4, Comparator.comparing(String::length)));
        assertThrows(IllegalArgumentException.class, () -> Median.of(new ArrayList<Integer>()));
    }

    @Test
    public void b6_inputCollectionIsNeverModified() {
        final List<Integer> source = new ArrayList<>(Arrays.asList(5, 2, 8, 1));
        Median.of(source);
        assertEquals(Arrays.asList(5, 2, 8, 1), source);

        final List<Integer> three = new ArrayList<>(Arrays.asList(5, 2, 8));
        Median.of(three);
        assertEquals(Arrays.asList(5, 2, 8), three);
    }

    // ---------------------------------------------------------------------------------------------
    // B1 - Index: tolerance == 0 is NOT the exact-match overload, for signed zero
    // ---------------------------------------------------------------------------------------------

    @Test
    public void b1_doubleToleranceZeroMatchesSignedZeroUnlikeTheExactOverload() {
        final double[] negZero = { -0.0d };

        assertFalse(Index.of(negZero, 0.0d).isPresent());
        assertEquals(OptionalInt.of(0), Index.of(negZero, 0.0d, 0, 0.0d));

        assertFalse(Index.last(negZero, 0.0d, 0).isPresent());
        assertEquals(OptionalInt.of(0), Index.last(negZero, 0.0d, 0, 0.0d));

        assertTrue(Index.allOf(negZero, 0.0d, 0).isEmpty());
        assertEquals(1, Index.allOf(negZero, 0.0d, 0, 0.0d).cardinality());
    }

    @Test
    public void b1_floatToleranceZeroMatchesSignedZeroUnlikeTheExactOverload() {
        final float[] negZero = { -0.0f };

        assertFalse(Index.of(negZero, 0.0f).isPresent());
        assertEquals(OptionalInt.of(0), Index.of(negZero, 0.0f, 0, 0.0f));

        assertFalse(Index.last(negZero, 0.0f, 0).isPresent());
        assertEquals(OptionalInt.of(0), Index.last(negZero, 0.0f, 0, 0.0f));

        assertTrue(Index.allOf(negZero, 0.0f, 0).isEmpty());
        assertEquals(1, Index.allOf(negZero, 0.0f, 0, 0.0f).cardinality());
    }

    @Test
    public void b1_naturalZeroAndNaNAreUnaffectedByTheToleranceOverloads() {
        assertEquals(OptionalInt.of(0), Index.of(new double[] { 0.0d }, 0.0d, 0, 0.0d));
        assertEquals(OptionalInt.of(0), Index.of(new double[] { -0.0d }, -0.0d, 0, 0.0d));
        // NaN is equal to NaN under both routes.
        assertEquals(OptionalInt.of(0), Index.of(new double[] { Double.NaN }, Double.NaN));
        assertEquals(OptionalInt.of(0), Index.of(new double[] { Double.NaN }, Double.NaN, 0, 0.0d));
    }

    // ---------------------------------------------------------------------------------------------
    // J4 - Index.of(String, String): an empty source is not automatically a miss
    // ---------------------------------------------------------------------------------------------

    @Test
    public void j4_emptyNeedleIsFoundAtZeroEvenInAnEmptySource() {
        assertEquals(OptionalInt.of(0), Index.of("", ""));
        assertEquals("".indexOf(""), Index.of("", "").orElse(-1));
        assertEquals(OptionalInt.of(0), Index.of("abc", ""));
        assertFalse(Index.of("", "x").isPresent());
        assertFalse(Index.of((String) null, "x").isPresent());
        assertFalse(Index.of("abc", (String) null).isPresent());
    }

    // ---------------------------------------------------------------------------------------------
    // B2 - Try: a close() failure fails the operation, and that is documented on every overload
    // ---------------------------------------------------------------------------------------------

    /** Body succeeds; close() throws. */
    private static final class FailingCloseResource implements AutoCloseable {
        @Override
        public void close() {
            throw new IllegalStateException("close failed");
        }
    }

    @Test
    public void b2_closeFailureAfterASuccessfulBodyRoutesToEveryFallback() {
        assertEquals("FALLBACK", Try.with(new FailingCloseResource()).call(c -> "REAL", "FALLBACK"));
        assertEquals("FALLBACK", Try.with(new FailingCloseResource()).call(c -> "REAL", () -> "FALLBACK"));
        assertEquals("close failed", Try.with(new FailingCloseResource()).call(c -> "REAL", Throwable::getMessage));
        assertEquals("FALLBACK", Try.with(new FailingCloseResource()).call(c -> "REAL", ex -> ex instanceof IllegalStateException, "FALLBACK"));
        assertEquals("FALLBACK", Try.with(new FailingCloseResource()).call(c -> "REAL", ex -> ex instanceof IllegalStateException, () -> "FALLBACK"));

        final IllegalStateException[] seen = new IllegalStateException[1];
        Try.with(new FailingCloseResource()).run(c -> {
        }, ex -> seen[0] = (IllegalStateException) ex);
        assertEquals("close failed", seen[0].getMessage());
    }

    @Test
    public void b2_closeFailureThatDoesNotMatchThePredicateIsStillThrown() {
        final RuntimeException thrown = assertThrows(RuntimeException.class,
                () -> Try.with(new FailingCloseResource()).call(c -> "REAL", ex -> ex instanceof IOException, "FALLBACK"));
        assertEquals("close failed", thrown.getMessage());
    }

    // ---------------------------------------------------------------------------------------------
    // B8 - Try: a null-returning supplier must not be reported as a null 'targetResource'
    // ---------------------------------------------------------------------------------------------

    @Test
    public void b8_nullFromTheSupplierNamesTheSupplier() {
        final Throwables.Supplier<StringWriter, Exception> nullSupplier = () -> null;

        final IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class, () -> Try.with(nullSupplier).run(w -> w.write("x")));

        assertTrue(thrown.getMessage().contains("targetResourceSupplier"), "message was: " + thrown.getMessage());
        assertFalse(thrown.getMessage().contains("'targetResource'"), "message was: " + thrown.getMessage());
    }

    @Test
    public void b8_aRealResourceIsStillAccepted() throws Exception {
        final StringWriter writer = new StringWriter();
        Try.with(writer).run(w -> w.write("ok"));
        assertEquals("ok", writer.toString());

        assertEquals("supplied", Try.with((Throwables.Supplier<StringWriter, Exception>) StringWriter::new).call(w -> "supplied"));
    }

    // ---------------------------------------------------------------------------------------------
    // B7 - Try: only InterruptedException restores the interrupted status
    // ---------------------------------------------------------------------------------------------

    @Test
    public void b7_interruptedExceptionRestoresTheFlagDirectlyAndThroughTheCauseChain() {
        Thread.interrupted();
        Try.run(() -> {
            throw new InterruptedException("direct");
        }, ex -> {
        });
        assertTrue(Thread.interrupted(), "a direct InterruptedException must restore the flag");

        Try.run(() -> {
            throw new IOException("io", new InterruptedException("nested cause"));
        }, ex -> {
        });
        assertTrue(Thread.interrupted(), "an InterruptedException in the cause chain must restore the flag");

        final IOException withSuppressed = new IOException("io");
        withSuppressed.addSuppressed(new InterruptedException("suppressed"));
        Try.run(() -> {
            throw withSuppressed;
        }, ex -> {
        });
        assertTrue(Thread.interrupted(), "an InterruptedException among the suppressed must restore the flag");
    }

    @Test
    public void b7_socketTimeoutMustNotBeMistakenForAnInterrupt() {
        // SocketTimeoutException extends InterruptedIOException, but an ordinary read timeout does not mean
        // the thread was interrupted. Treating the whole InterruptedIOException family as an interrupt would
        // silently set the flag on every timed-out socket read.
        Thread.interrupted();

        Try.run(() -> {
            throw new SocketTimeoutException("read timed out");
        }, ex -> {
        });
        assertFalse(Thread.interrupted(), "a socket timeout must not set the interrupted status");

        Try.run(() -> {
            throw new java.io.InterruptedIOException("io timed out");
        }, ex -> {
        });
        assertFalse(Thread.interrupted(), "a bare InterruptedIOException must not set the interrupted status");
    }

    @Test
    public void b7_ordinaryFailuresLeaveTheFlagAlone() {
        Thread.interrupted();
        assertEquals("d", Try.call(() -> {
            throw new IOException("plain");
        }, "d"));
        assertFalse(Thread.interrupted());

        // A deep cause chain with no InterruptedException in it must also leave the flag clear.
        assertEquals("d", Try.call(() -> {
            throw new IOException("outer", new IllegalStateException("middle", new IllegalArgumentException("inner")));
        }, "d"));
        assertFalse(Thread.interrupted());
    }

    // ---------------------------------------------------------------------------------------------
    // D1 - custom Wrapper.of(value, hash, equals): shared function instances compare across call sites
    // ---------------------------------------------------------------------------------------------

    private static final ToIntFunction<String> BY_LENGTH_HASH = String::length;
    private static final BiPredicate<String, String> BY_LENGTH_EQ = (a, b) -> a.length() == b.length();

    private static Wrapper<String> wrapHereA(final String s) {
        return Wrapper.of(s, BY_LENGTH_HASH, BY_LENGTH_EQ);
    }

    private static Wrapper<String> wrapHereB(final String s) {
        return Wrapper.of(s, BY_LENGTH_HASH, BY_LENGTH_EQ);
    }

    @Test
    public void d1_wrappersFromSharedFunctionInstancesCompareAcrossCallSites() {
        assertEquals(wrapHereA("abc"), wrapHereB("xyz"));
        assertEquals(wrapHereA("abc").hashCode(), wrapHereB("xyz").hashCode());

        final Set<Wrapper<String>> set = new HashSet<>();
        set.add(wrapHereA("abc"));
        assertTrue(set.contains(wrapHereB("xyz")), "a lookup from another call site must hit");
        assertFalse(set.contains(Wrapper.of("abcd", BY_LENGTH_HASH, BY_LENGTH_EQ)));
    }

    @Test
    public void d1_inlineLambdasCannotCompareAcrossCallSites() {
        // Documented behaviour: each call site supplies its own lambda instances.
        final Wrapper<String> a = Wrapper.of("abc", (ToIntFunction<String>) String::length, (x, y) -> x.length() == y.length());
        final Wrapper<String> b = Wrapper.of("xyz", (ToIntFunction<String>) String::length, (x, y) -> x.length() == y.length());
        assertNotEquals(a, b);
    }

    @Test
    public void d1_differentFunctionInstancesNeverCompareEqual() {
        final ToIntFunction<String> hashAgain = String::length;
        final BiPredicate<String, String> eqAgain = (a, b) -> a.length() == b.length();
        assertNotEquals(Wrapper.of("abc", BY_LENGTH_HASH, BY_LENGTH_EQ), Wrapper.of("abc", hashAgain, eqAgain));
        assertNotEquals(Wrapper.of("abc", hashAgain, eqAgain), Wrapper.of("abc", BY_LENGTH_HASH, BY_LENGTH_EQ));
    }

    @Test
    public void d1_customToStringDoesNotAffectEquality() {
        final ToIntFunction<String> hash = s -> s.toLowerCase(java.util.Locale.ROOT).hashCode();
        final BiPredicate<String, String> eq = String::equalsIgnoreCase;
        final Function<String, String> render = s -> "CI(" + s + ")";

        assertEquals(Wrapper.of("Hello", hash, eq, render), Wrapper.of("HELLO", hash, eq, render));
        assertEquals(Wrapper.of("Hello", hash, eq, render).hashCode(), Wrapper.of("HELLO", hash, eq, render).hashCode());
        assertEquals("Wrapper[CI(Hello)]", Wrapper.of("Hello", hash, eq, render).toString());
        assertEquals("Hello", Wrapper.of("Hello", hash, eq, render).value());
    }

    @Test
    public void d1_customOfValidatesItsFunctions() {
        assertThrows(IllegalArgumentException.class, () -> Wrapper.of("x", null, (a, b) -> true));
        assertThrows(IllegalArgumentException.class, () -> Wrapper.of("x", (ToIntFunction<String>) String::length, null));
        assertThrows(IllegalArgumentException.class,
                () -> Wrapper.of("x", (ToIntFunction<String>) String::length, (BiPredicate<String, String>) (a, b) -> true, null));
    }

    @Test
    public void d1_customOfWrapsNullWhenItsFunctionsAcceptNull() {
        final ToIntFunction<String> hash = s -> s == null ? 0 : s.hashCode();
        final BiPredicate<String, String> eq = (a, b) -> a == null ? b == null : a.equals(b);

        assertEquals(Wrapper.of(null, hash, eq), Wrapper.of(null, hash, eq));
        assertNull(Wrapper.of(null, hash, eq).value());
        assertNotEquals(Wrapper.of(null, hash, eq), Wrapper.of("x", hash, eq));
    }

    @Test
    public void d1_customWrapperIsNotEqualToADeepWrapper() {
        assertNotEquals(Wrapper.of("abc", BY_LENGTH_HASH, BY_LENGTH_EQ), Wrapper.of("abc"));
        assertNotEquals(Wrapper.of("abc"), Wrapper.of("abc", BY_LENGTH_HASH, BY_LENGTH_EQ));
    }

    // ---------------------------------------------------------------------------------------------
    // D2 - TypeReference value semantics
    // ---------------------------------------------------------------------------------------------

    @Test
    public void d2_typeReferencesWithTheSameCaptureAreEqual() {
        final TypeReference<List<String>> a = new TypeReference<>() {
        };
        final TypeReference<List<String>> b = new TypeReference<>() {
        };

        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
        assertEquals(a, a);
    }

    @Test
    public void d2_typeReferencesWithDifferentCapturesAreNotEqual() {
        final TypeReference<List<String>> strings = new TypeReference<>() {
        };
        final TypeReference<List<Integer>> integers = new TypeReference<>() {
        };

        assertNotEquals(strings, integers);
        assertNotEquals(strings, null);
        assertNotEquals(strings, "not a type reference");
    }

    @Test
    public void d2_typeTokenAndTypeReferenceForTheSameTypeAreEqual() {
        final TypeReference<Map<String, Integer>> ref = new TypeReference<>() {
        };
        final TypeReference.TypeToken<Map<String, Integer>> token = new TypeReference.TypeToken<>() {
        };

        assertEquals(ref, token);
        assertEquals(token, ref);
        assertEquals(ref.hashCode(), token.hashCode());
    }

    @Test
    public void d2_typeReferenceToStringIsTheTypeName() {
        assertEquals("java.util.List<java.lang.String>", new TypeReference<List<String>>() {
        }.toString());
        assertEquals("java.lang.String", new TypeReference<String>() {
        }.toString());
    }

    @Test
    public void d2_typeReferenceIsUsableAsAMapKey() {
        final Map<TypeReference<?>, String> byType = new LinkedHashMap<>();
        byType.put(new TypeReference<List<String>>() {
        }, "strings");

        assertEquals("strings", byType.get(new TypeReference<List<String>>() {
        }));
        assertNull(byType.get(new TypeReference<List<Integer>>() {
        }));
    }

    // ---------------------------------------------------------------------------------------------
    // D7 / O2 / O6 - Clazz.of null, and the array-key identity that Keyed now documents
    // ---------------------------------------------------------------------------------------------

    @Test
    public void d7_clazzOfReturnsItsArgumentIncludingNull() {
        assertNull(Clazz.of(null));
        assertSame(ArrayList.class, Clazz.of(ArrayList.class));
    }

    @Test
    public void o6_arrayKeysInKeyedCompareByIdentityAndWrapperIsTheFix() {
        final int[] k1 = { 1, 2 };
        final int[] k2 = { 1, 2 };

        assertNotEquals(Keyed.of(k1, "v1"), Keyed.of(k2, "v2"));
        assertEquals(Keyed.of(k1, "v1"), Keyed.of(k1, "v2"));
        assertNotEquals(IndexedKeyed.of(k1, "v1", 0), IndexedKeyed.of(k2, "v2", 0));

        // The documented remedy.
        assertEquals(Keyed.of(Wrapper.of(k1), "v1"), Keyed.of(Wrapper.of(k2), "v2"));
    }

    // ---------------------------------------------------------------------------------------------
    // D9 - Hex.encodeToString now delegates to java.util.HexFormat; output must be unchanged
    // ---------------------------------------------------------------------------------------------

    @Test
    public void d9_hexEncodeToStringMatchesHexFormatAndTheCharArrayRoute() {
        final byte[] data = new byte[512];
        new Random(20260831L).nextBytes(data);

        assertEquals(HexFormat.of().formatHex(data), Hex.encodeToString(data));
        assertEquals(HexFormat.of().withUpperCase().formatHex(data), Hex.encodeToString(data, false));
        assertEquals(String.valueOf(Hex.encode(data)), Hex.encodeToString(data));
        assertEquals(String.valueOf(Hex.encode(data, false)), Hex.encodeToString(data, false));
        assertArrayEqualsBytes(data, Hex.decode(Hex.encodeToString(data)));
        assertArrayEqualsBytes(data, Hex.decode(Hex.encodeToString(data, false)));
    }

    @Test
    public void d9_hexEncodeToStringEdgeCases() {
        assertEquals("", Hex.encodeToString(new byte[0]));
        assertEquals("", Hex.encodeToString(new byte[0], false));
        assertEquals("48656c6c6f", Hex.encodeToString(new byte[] { 0x48, 0x65, 0x6C, 0x6C, 0x6F }));
        assertEquals("48656C6C6F", Hex.encodeToString(new byte[] { 0x48, 0x65, 0x6C, 0x6C, 0x6F }, false));
        assertEquals("ff0042", Hex.encodeToString(new byte[] { (byte) 0xFF, 0x00, 0x42 }, true));
        assertEquals("FF0042", Hex.encodeToString(new byte[] { (byte) 0xFF, 0x00, 0x42 }, false));

        assertThrows(IllegalArgumentException.class, () -> Hex.encodeToString(null));
        assertThrows(IllegalArgumentException.class, () -> Hex.encodeToString(null, true));
        assertThrows(IllegalArgumentException.class, () -> Hex.encodeToString(null, false));
    }

    private static void assertArrayEqualsBytes(final byte[] expected, final byte[] actual) {
        assertTrue(Arrays.equals(expected, actual), "byte arrays differ");
    }

    // ---------------------------------------------------------------------------------------------
    // J2 / J1 - documented values that were simply wrong or misleading
    // ---------------------------------------------------------------------------------------------

    @Test
    public void j2_md2HexOfTheDocumentedSampleMatchesTheDocumentedValue() {
        assertEquals("7db01e9c9419c10d047e45986382487a", DigestUtil.md2Hex(Strings.getBytesUtf8("legacy data")));
        assertEquals("7db01e9c9419c10d047e45986382487a", DigestUtil.md2Hex("legacy data"));
    }

    @Test
    public void j1_concatenatingAFunctionWithItselfOnlyRepeatsItsOutput() {
        // Locks the javadoc warning: 512 output bits, but the second half is a copy of the first.
        final com.landawn.abacus.guava.hash.HashFunction doubled = com.landawn.abacus.guava.hash.Hashing
                .concatenating(com.landawn.abacus.guava.hash.Hashing.sha256(), com.landawn.abacus.guava.hash.Hashing.sha256());

        assertEquals(512, doubled.bits());

        final String hex = doubled.hash("data".getBytes(java.nio.charset.StandardCharsets.UTF_8)).toString();
        final String firstHalf = hex.substring(0, hex.length() / 2);
        final String secondHalf = hex.substring(hex.length() / 2);

        assertEquals(firstHalf, secondHalf);
        assertEquals(com.landawn.abacus.guava.hash.Hashing.sha256().hash("data".getBytes(java.nio.charset.StandardCharsets.UTF_8)).toString(), firstHalf);

        // Two *different* functions genuinely add information.
        final com.landawn.abacus.guava.hash.HashFunction mixed = com.landawn.abacus.guava.hash.Hashing
                .concatenating(com.landawn.abacus.guava.hash.Hashing.sha256(), com.landawn.abacus.guava.hash.Hashing.sha512());
        assertEquals(768, mixed.bits());
        final String mixedHex = mixed.hash("data".getBytes(java.nio.charset.StandardCharsets.UTF_8)).toString();
        assertNotEquals(mixedHex.substring(0, 64), mixedHex.substring(64, 128));
    }

    // ---------------------------------------------------------------------------------------------
    // O2 - the two lint warnings removed without any @SuppressWarnings must not change behaviour
    // ---------------------------------------------------------------------------------------------

    @Test
    public void o2_keyedAndIndexedKeyedEqualityIsUnchanged() {
        assertEquals(Keyed.of("k", 1), Keyed.of("k", 2));
        assertNotEquals(Keyed.of("k", 1), Keyed.of("other", 1));
        assertNotEquals(Keyed.of("k", 1), IndexedKeyed.of("k", 1, 0));
        assertNotEquals(IndexedKeyed.of("k", 1, 0), Keyed.of("k", 1));
        assertNotEquals(Keyed.of("k", 1), "not a Keyed");
        assertNotEquals(Keyed.of(null, 1), Keyed.of("k", 1));
        assertEquals(Keyed.of(null, 1), Keyed.of(null, 2));

        assertEquals(IndexedKeyed.of("k", 1, 3), IndexedKeyed.of("k", 9, 3));
        assertNotEquals(IndexedKeyed.of("k", 1, 3), IndexedKeyed.of("k", 1, 4));
        assertNotEquals(IndexedKeyed.of("k", 1, 3), "not an IndexedKeyed");
        assertEquals(IndexedKeyed.of("k", 1, 3).hashCode(), IndexedKeyed.of("k", 9, 3).hashCode());
    }

    // ---------------------------------------------------------------------------------------------
    // D3 - If.OrElse: the members the javadoc used to call "internal" are usable public API
    // ---------------------------------------------------------------------------------------------

    @Test
    public void d3_orElseDoNothingIsCallableAndIsANoOp() {
        final AtomicInteger thenRuns = new AtomicInteger();

        If.is(true).then(thenRuns::incrementAndGet).orElseDoNothing();
        assertEquals(1, thenRuns.get());

        If.is(false).then(thenRuns::incrementAndGet).orElseDoNothing();
        assertEquals(1, thenRuns.get());

        If.is(true).thenDoNothing().orElseDoNothing();
        assertEquals(1, thenRuns.get());
    }

    // ---------------------------------------------------------------------------------------------
    // Index.allOf still hands the caller its own BitSet
    // ---------------------------------------------------------------------------------------------

    @Test
    public void indexAllOfReturnsAFreshMutableBitSetEachCall() {
        final int[] source = { 1, 2, 1 };
        final BitSet first = Index.allOf(source, 1);
        first.set(99);

        final BitSet second = Index.allOf(source, 1);
        assertFalse(second.get(99), "each call must return a fresh BitSet");
        assertEquals(2, second.cardinality());
    }
}

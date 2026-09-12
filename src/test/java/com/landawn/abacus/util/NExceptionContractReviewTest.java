package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.exception.UncheckedIOException;
import com.landawn.abacus.exception.UncheckedInterruptedException;
import com.landawn.abacus.util.function.BooleanPredicate;
import com.landawn.abacus.util.function.Predicate;

class NExceptionContractReviewTest {
    @Test
    void positiveSleepWrapsInterruptionAndRestoresStatus() {
        try {
            Thread.currentThread().interrupt();
            assertThrows(UncheckedInterruptedException.class, () -> N.sleep(1));
            assertTrue(Thread.currentThread().isInterrupted());
            assertThrows(UncheckedInterruptedException.class, () -> N.sleep(1, TimeUnit.MILLISECONDS));
            assertTrue(Thread.currentThread().isInterrupted());
        } finally {
            Thread.interrupted();
        }
    }

    @Test
    void nonpositiveSleepPreservesNoOpAndUnitValidation() {
        try {
            Thread.currentThread().interrupt();
            N.sleep(0);
            N.sleep(-1, TimeUnit.MILLISECONDS);
            assertTrue(Thread.currentThread().isInterrupted());
            assertThrows(IllegalArgumentException.class, () -> N.sleep(0, null));
            assertTrue(Thread.currentThread().isInterrupted());
        } finally {
            Thread.interrupted();
        }
    }

    @Test
    void parallelTraversalPropagatesErrorsAndWrapsCheckedExceptions() {
        final Executor direct = Runnable::run;
        final AssertionError failure = new AssertionError("consumer failed");
        assertSame(failure, assertThrows(AssertionError.class,
                () -> N.forEachInParallel(List.of(1), value -> { throw failure; }, 1, direct)));
        assertSame(failure, assertThrows(AssertionError.class,
                () -> N.forEachIndexedInParallel(List.of(1), (index, value) -> { throw failure; }, 1, direct)));
        final IOException checked = new IOException("consumer failed");
        final UncheckedIOException wrapped = assertThrows(UncheckedIOException.class,
                () -> N.forEachInParallel(List.of(1), value -> { throw checked; }, 1, direct));
        assertSame(checked, wrapped.getCause());
    }

    @Test
    void asyncSubmissionRejectsBeforeCommandRuns() {
        final RejectedExecutionException failure = new RejectedExecutionException("full");
        final Executor rejecting = command -> { throw failure; };
        final AtomicInteger calls = new AtomicInteger();
        final Callable<Integer> callable = calls::incrementAndGet;
        assertSame(failure, assertThrows(RejectedExecutionException.class, () -> N.asyncExecute(callable, rejecting)));
        assertSame(failure, assertThrows(RejectedExecutionException.class,
                () -> N.asyncExecute((Throwables.Runnable<Exception>) calls::incrementAndGet, rejecting)));
        assertEquals(0, calls.get());
    }

    @Test
    void bulkSubmissionKeepsNullContainerAndNullElementDistinct() {
        final Executor direct = Runnable::run;
        assertTrue(N.asyncExecute((Collection<Callable<Integer>>) null, direct).isEmpty());
        final AtomicInteger calls = new AtomicInteger();
        final Collection<Callable<Integer>> commands = Arrays.asList(calls::incrementAndGet, null);
        assertThrows(IllegalArgumentException.class, () -> N.asyncExecute(commands, direct));
        assertEquals(1, calls.get(), "commands before a null element have already been submitted");
    }

    @Test
    void functionalCompositionPreservesInheritedAndLibraryContracts() {
        final Predicate<String> jdkPredicate = value -> true;
        assertThrows(NullPointerException.class, () -> jdkPredicate.and(null));
        assertThrows(NullPointerException.class, () -> jdkPredicate.or(null));
        final BooleanPredicate primitivePredicate = value -> value;
        assertThrows(IllegalArgumentException.class, () -> primitivePredicate.and(null));
        assertThrows(IllegalArgumentException.class, () -> primitivePredicate.or(null));
        assertFalse(primitivePredicate.and(value -> { throw new AssertionError("must short circuit"); }).test(false));
    }
}

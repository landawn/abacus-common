package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

/**
 * Doc-only regression locks for the cycle-3 review of {@link Throwables}: G1-03 (Iterator.defer delegates count() and
 * advance(long) to the underlying iterator, so a close made by that iterator mid-call does not stop them) and G1-04
 * (the run/call interruption paragraph: Try searches the cause/suppressed chain except beneath an ExecutionException or
 * CompletionException).
 */
public class ThrowablesReview20260924cTest extends TestBase {

    @AfterEach
    public void clearInterrupt() {
        Thread.interrupted();
    }

    /**
     * A source whose {@code hasNext()} closes {@code holder[0]} on the {@code closeOnCall}-th call and which counts its
     * own {@code count()}/{@code advance(long)} calls.
     */
    private static final class Source<T> extends Throwables.Iterator<T, Exception> {
        final AtomicInteger countCalls = new AtomicInteger();
        final AtomicInteger advanceCalls = new AtomicInteger();
        private final java.util.Iterator<T> it;
        private final Object[] holder;
        private final int closeOnCall;
        private int hasNextCalls;

        Source(final List<T> values, final Object[] holder, final int closeOnCall) {
            this.it = values.iterator();
            this.holder = holder;
            this.closeOnCall = closeOnCall;
        }

        @Override
        public boolean hasNext() {
            if (++hasNextCalls == closeOnCall && holder[0] != null) {
                ((Throwables.Iterator<?, ?>) holder[0]).closeResource();
            }
            return it.hasNext();
        }

        @Override
        public T next() {
            return it.next();
        }

        @Override
        public long count() throws Exception {
            countCalls.incrementAndGet();
            return super.count();
        }

        @Override
        public void advance(final long n) throws Exception {
            advanceCalls.incrementAndGet();
            super.advance(n);
        }
    }

    // ---------------------------------------------------------------- G1-03 defer: count()/advance() are delegated

    @Test
    public void g103_deferCount_isDelegatedAndIgnoresACloseMadeDuringTheCall() throws Exception {
        final Object[] holder = new Object[1];
        final Source<String> source = new Source<>(Arrays.asList("a", "中", "😀", "b", "c"), holder, 3);
        final Throwables.Iterator<String, Exception> deferred = Throwables.Iterator.defer(() -> source);
        holder[0] = deferred;

        // the delegated count runs to completion although the source closed the wrapper on its 3rd hasNext()
        assertEquals(5, deferred.count());
        assertEquals(1, source.countCalls.get());
        // afterwards the wrapper reports itself exhausted
        assertFalse(deferred.hasNext());
        assertThrows(NoSuchElementException.class, deferred::next);
        assertEquals(0, deferred.count());
        assertEquals(1, source.countCalls.get());
    }

    @Test
    public void g103_mapCount_incontrast_stopsAtTheClose() throws Exception {
        final Object[] holder = new Object[1];
        final Source<String> source = new Source<>(Arrays.asList("a", "中", "😀", "b", "c"), holder, 3);
        final Throwables.Iterator<String, Exception> mapped = source.map(s -> s);
        holder[0] = mapped;

        assertEquals(2, mapped.count());
        assertEquals(0, source.countCalls.get());
    }

    @Test
    public void g103_deferAdvance_isDelegatedOnlyForPositiveN() throws Exception {
        final Source<Integer> source = new Source<>(Arrays.asList(1, 2, 3, 4), new Object[1], -1);
        final Throwables.Iterator<Integer, Exception> deferred = Throwables.Iterator.defer(() -> source);

        deferred.advance(0);
        deferred.advance(-1);
        assertEquals(0, source.advanceCalls.get());

        deferred.advance(2);
        assertEquals(1, source.advanceCalls.get());
        assertEquals(3, deferred.next());
        assertEquals(1, deferred.count());
        assertEquals(1, source.countCalls.get());
    }

    @Test
    public void g103_deferCount_closedBeforeTheCall_returnsZeroWithoutTouchingTheSource() throws Exception {
        final Source<Integer> source = new Source<>(Arrays.asList(1, 2), new Object[1], -1);
        final Throwables.Iterator<Integer, Exception> deferred = Throwables.Iterator.defer(() -> source);

        assertTrue(deferred.hasNext());
        deferred.closeResource();

        assertEquals(0, deferred.count());
        deferred.advance(5);
        assertEquals(0, source.countCalls.get());
        assertEquals(0, source.advanceCalls.get());
    }

    // ---------------------------------------------------------------- G1-04 interruption wording (Try vs Throwables)

    private static boolean interruptedAfterTryRun(final Exception failure) {
        Thread.interrupted();
        try {
            Try.run(() -> {
                throw failure;
            });
        } catch (final RuntimeException e) {
            // converted; only the interrupt flag matters
        }
        return Thread.interrupted();
    }

    private static boolean interruptedAfterTryCall(final Exception failure) {
        Thread.interrupted();
        assertEquals("v", Try.call(() -> {
            throw failure;
        }, "v"));
        return Thread.interrupted();
    }

    private static boolean interruptedAfterThrowablesCall(final Exception failure) {
        Thread.interrupted();
        assertEquals("v", Throwables.call(() -> {
            throw failure;
        }, "v"));
        return Thread.interrupted();
    }

    @Test
    public void g104_try_searchesTheCauseChainButNotBeneathExecutionOrCompletionException() {
        // searched: a plain cause chain
        assertTrue(interruptedAfterTryRun(new RuntimeException(new IllegalStateException(new InterruptedException()))));
        assertTrue(interruptedAfterTryCall(new RuntimeException(new InterruptedException())));

        // not searched: beneath an ExecutionException / CompletionException (another thread's interruption)
        assertFalse(interruptedAfterTryRun(new ExecutionException(new InterruptedException())));
        assertFalse(interruptedAfterTryCall(new ExecutionException(new InterruptedException())));
        assertFalse(interruptedAfterTryRun(new CompletionException(new InterruptedException())));
        assertFalse(interruptedAfterTryCall(new RuntimeException(new ExecutionException(new InterruptedException()))));

        // exceptions suppressed on such a wrapper are still examined
        final ExecutionException ee = new ExecutionException(new IllegalStateException("task"));
        ee.addSuppressed(new InterruptedException());
        assertTrue(interruptedAfterTryCall(ee));
    }

    @Test
    public void g104_throwables_looksOnlyThroughInvocationTargetAndUndeclaredThrowable() {
        assertTrue(interruptedAfterThrowablesCall(new InterruptedException()));
        assertTrue(interruptedAfterThrowablesCall(new java.lang.reflect.InvocationTargetException(new InterruptedException())));
        assertTrue(interruptedAfterThrowablesCall(new java.lang.reflect.UndeclaredThrowableException(new InterruptedException())));

        assertFalse(interruptedAfterThrowablesCall(new RuntimeException(new InterruptedException())));
        assertFalse(interruptedAfterThrowablesCall(new ExecutionException(new InterruptedException())));
        assertFalse(interruptedAfterThrowablesCall(new CompletionException(new InterruptedException())));
    }
}

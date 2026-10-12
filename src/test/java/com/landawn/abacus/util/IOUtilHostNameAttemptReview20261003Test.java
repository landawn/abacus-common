package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.landawn.abacus.TestBase;

@Tag("unit")
class IOUtilHostNameAttemptReview20261003Test extends TestBase {
    static Stream<Arguments> delayedOutcomes() {
        return Stream.of(Arguments.of(false, false), Arguments.of(false, true), Arguments.of(true, false), Arguments.of(true, true));
    }

    @ParameterizedTest
    @MethodSource("delayedOutcomes")
    void delayedOldOutcomeCannotRetireNewAttemptOrOverrideCachedSuccess(final boolean timeout, final boolean newerSuccess) throws Exception {
        try (HostState state = new HostState()) {
            final DelayedFailure old = new DelayedFailure(timeout);
            state.install(old, null);
            final Caller delayed = state.caller();
            old.delayedCaller = delayed.thread;
            delayed.thread.start();
            assertTrue(old.waiting.await(5, TimeUnit.SECONDS));

            assertEquals("UNKNOWN_HOST_NAME", IOUtil.getHostName());
            assertNull(state.future.get(null));

            final Resolver next = new Resolver();
            synchronized (state.lock) {
                // Simulate the retry deadline passing while the old caller is descheduled.
                state.retry.setLong(null, 0);
                state.resolver.set(null, next);
            }
            final Caller newer = state.caller();
            newer.thread.start();
            assertTrue(next.attempt.waiting.await(5, TimeUnit.SECONDS));
            assertSame(next.attempt, state.future.get(null));
            if (newerSuccess) {
                next.attempt.complete("new-host");
                assertEquals("new-host", newer.result());
            }

            old.release.countDown();
            assertEquals(newerSuccess ? "new-host" : "UNKNOWN_HOST_NAME", delayed.result());
            assertEquals(0L, state.retry.getLong(null), "an old outcome must not start a new backoff");
            if (newerSuccess) {
                assertEquals("new-host", state.name.get(null));
                assertNull(state.future.get(null));
            } else {
                assertSame(next.attempt, state.future.get(null), "the pending retry must remain shared by later callers");
                assertSame(next, state.resolver.get(null));
                assertFalse(next.isShutdown());
                next.attempt.complete("new-host");
                assertEquals("new-host", newer.result());
            }
            assertEquals("new-host", IOUtil.getHostName());
        }
    }

    @Test
    void currentFailureStillRetiresItsResolverAndBacksOff() throws Exception {
        try (HostState state = new HostState()) {
            final Resolver resolver = new Resolver();
            resolver.attempt.completeExceptionally(new UnknownHostException("failure"));
            state.install(resolver.attempt, resolver);
            assertEquals("UNKNOWN_HOST_NAME", IOUtil.getHostName());
            assertNull(state.future.get(null));
            assertNull(state.resolver.get(null));
            assertNull(state.name.get(null));
            assertTrue(resolver.isShutdown());
            assertTrue(state.retry.getLong(null) > System.currentTimeMillis());
            assertEquals("UNKNOWN_HOST_NAME", IOUtil.getHostName());
        }
    }

    @Test
    void callerInterruptionPreservesPendingAttemptAndFlag() throws Exception {
        try (HostState state = new HostState()) {
            final Resolver resolver = new Resolver();
            state.install(resolver.attempt, resolver);
            Thread.currentThread().interrupt();
            try {
                assertEquals("UNKNOWN_HOST_NAME", IOUtil.getHostName());
                assertTrue(Thread.currentThread().isInterrupted());
            } finally {
                Thread.interrupted();
            }
            assertSame(resolver.attempt, state.future.get(null));
            assertFalse(resolver.isShutdown());
            assertEquals(0L, state.retry.getLong(null));
            resolver.attempt.complete("completed-after-interrupt");
            assertEquals("completed-after-interrupt", IOUtil.getHostName());
            assertTrue(resolver.isShutdown());
        }
    }

    // follow-up 2026-10-04: like the timeout branch, an interrupted caller reports a name a peer cached meanwhile.
    @Test
    void interruptedCallerReturnsANameCachedByAPeerWhileItWaited() throws Exception {
        try (HostState state = new HostState()) {
            final CompletableFuture<String> attempt = new CompletableFuture<>() {
                @Override
                public String get(final long wait, final TimeUnit unit) throws InterruptedException {
                    try {
                        state.name.set(null, "peer-host"); // a peer completed the shared attempt and cached its name
                    } catch (final IllegalAccessException e) {
                        throw new AssertionError(e);
                    }
                    throw new InterruptedException("caller interrupted");
                }
            };
            state.install(attempt, new Resolver());
            try {
                assertEquals("peer-host", IOUtil.getHostName());
                assertTrue(Thread.currentThread().isInterrupted(), "interruption must still be restored");
            } finally {
                Thread.interrupted();
            }
            assertSame(attempt, state.future.get(null), "the in-flight attempt is left for later callers");
        }
    }

    private static final class HostState implements AutoCloseable {
        final Field name = field("hostName");
        final Field future = field("hostNameFuture");
        final Field retry = field("hostNameRetryAtMillis");
        final Field resolver = field("hostNameResolver");
        final Object lock = field("hostNameLock").get(null);
        final Object[] saved;
        final List<Caller> callers = new ArrayList<>();

        HostState() throws Exception {
            synchronized (lock) {
                saved = new Object[] { name.get(null), future.get(null), retry.get(null), resolver.get(null) };
                install(null, null);
            }
        }

        void install(final Future<String> attempt, final Resolver executor) throws Exception {
            synchronized (lock) {
                name.set(null, null);
                future.set(null, attempt);
                retry.setLong(null, 0);
                resolver.set(null, executor);
            }
        }

        Caller caller() {
            final Caller caller = new Caller();
            callers.add(caller);
            return caller;
        }

        @Override
        public void close() throws Exception {
            for (final Caller caller : callers) {
                caller.thread.interrupt();
                caller.thread.join(5000);
                assertFalse(caller.thread.isAlive(), "test caller must terminate before shared state is restored");
            }
            synchronized (lock) {
                name.set(null, saved[0]);
                future.set(null, saved[1]);
                retry.set(null, saved[2]);
                resolver.set(null, saved[3]);
            }
        }
    }

    private static final class Caller {
        final AtomicReference<String> value = new AtomicReference<>();
        final AtomicReference<Throwable> failure = new AtomicReference<>();
        final Thread thread = new Thread(() -> {
            try {
                value.set(IOUtil.getHostName());
            } catch (final Throwable e) {
                failure.set(e);
            }
        });

        String result() throws InterruptedException {
            thread.join(5000);
            assertFalse(thread.isAlive());
            assertNull(failure.get());
            return value.get();
        }
    }

    private static final class DelayedFailure extends CompletableFuture<String> {
        final CountDownLatch waiting = new CountDownLatch(1);
        final CountDownLatch release = new CountDownLatch(1);
        final boolean timeout;
        Thread delayedCaller;

        DelayedFailure(final boolean timeout) {
            this.timeout = timeout;
            completeExceptionally(new UnknownHostException("old lookup"));
        }

        @Override
        public String get(final long wait, final TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
            if (Thread.currentThread() == delayedCaller) {
                waiting.countDown();
                if (!release.await(5, TimeUnit.SECONDS)) {
                    throw new AssertionError("test release timed out");
                }
                if (timeout) {
                    // The caller timed out before its peer saw the failure, then was descheduled.
                    throw new TimeoutException("old wait");
                }
            }
            return super.get(wait, unit);
        }
    }

    private static final class PendingAttempt extends CompletableFuture<String> {
        final CountDownLatch waiting = new CountDownLatch(1);

        @Override
        public String get(final long wait, final TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
            waiting.countDown();
            return super.get(wait, unit);
        }
    }

    private static final class Resolver extends AbstractExecutorService {
        final PendingAttempt attempt = new PendingAttempt();
        boolean shutdown;

        @Override
        public void shutdown() {
            shutdown = true;
        }

        @Override
        public List<Runnable> shutdownNow() {
            shutdown = true;
            return List.of();
        }

        @Override
        public boolean isShutdown() {
            return shutdown;
        }

        @Override
        public boolean isTerminated() {
            return shutdown && attempt.isDone();
        }

        @Override
        public boolean awaitTermination(final long timeout, final TimeUnit unit) {
            return isTerminated();
        }

        @Override
        public void execute(final Runnable command) {
            throw new AssertionError("unexpected execution");
        }

        @SuppressWarnings("unchecked")
        @Override
        public <T> Future<T> submit(final Callable<T> task) {
            return (Future<T>) attempt;
        }
    }

    private static Field field(final String name) throws Exception {
        final Field field = IOUtil.class.getDeclaredField(name);
        field.setAccessible(true);
        return field;
    }
}

package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.landawn.abacus.TestBase;

@Tag("unit")
class FnShutdownReentryReview20261003Test extends TestBase {
    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    void terminationHookDoesNotRepeatShutdownOrWait(final boolean timed) {
        final HookExecutor executor = new HookExecutor();
        final java.lang.Runnable task = shutdownTask(executor, timed);
        executor.hook = task;

        task.run();
        task.run();

        assertTrue(executor.isTerminated());
        assertEquals(1, executor.shutdownCalls);
        assertEquals(timed ? 1 : 0, executor.awaitCalls);
    }

    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    void reentryBeforeFailureDoesNotPreventALaterRetry(final boolean timed) {
        final HookExecutor executor = new HookExecutor();
        final java.lang.Runnable task = shutdownTask(executor, timed);
        executor.hook = task;
        executor.failFirst = true;
        executor.reenterBeforeFailure = true;

        try {
            assertThrows(SecurityException.class, task::run);
            assertEquals(1, executor.shutdownCalls);
            assertEquals(0, executor.awaitCalls);
            assertFalse(executor.isShutdown());

            task.run();
            task.run();
            assertTrue(executor.isTerminated());
            assertEquals(2, executor.shutdownCalls);
            assertEquals(timed ? 1 : 0, executor.awaitCalls);
        } finally {
            executor.shutdownNow();
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    void ordinaryFailureStillRetriesAndSuccessfulShutdownIsRemembered(final boolean timed) {
        final HookExecutor executor = new HookExecutor();
        executor.failFirst = true;
        final java.lang.Runnable task = shutdownTask(executor, timed);

        try {
            assertThrows(SecurityException.class, task::run);
            assertEquals(0, executor.awaitCalls);
            task.run();
            task.run();
            assertTrue(executor.isTerminated());
            assertEquals(2, executor.shutdownCalls);
            assertEquals(timed ? 1 : 0, executor.awaitCalls);
        } finally {
            executor.shutdownNow();
        }
    }

    private static java.lang.Runnable shutdownTask(final HookExecutor executor, final boolean timed) {
        return timed ? Fn.shutdown(executor, 0, TimeUnit.MILLISECONDS) : Fn.shutdown(executor);
    }

    private static final class HookExecutor extends ThreadPoolExecutor {
        private java.lang.Runnable hook;
        private boolean failFirst;
        private boolean reenterBeforeFailure;
        private int shutdownCalls;
        private int awaitCalls;

        HookExecutor() {
            super(0, 1, 1, TimeUnit.SECONDS, new LinkedBlockingQueue<>());
        }

        @Override
        public void shutdown() {
            if (++shutdownCalls == 1 && failFirst) {
                if (reenterBeforeFailure) {
                    hook.run();
                }
                throw new SecurityException("first shutdown denied");
            }
            super.shutdown();
        }

        @Override
        protected void terminated() {
            if (hook != null) {
                hook.run();
            }
        }

        @Override
        public boolean awaitTermination(final long timeout, final TimeUnit unit) throws InterruptedException {
            awaitCalls++;
            return super.awaitTermination(timeout, unit);
        }
    }
}

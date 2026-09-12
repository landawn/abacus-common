package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("unit")
public class AsyncAdmissionTest {
    private static void await(CountDownLatch latch) {
        try {
            assertTrue(latch.await(5, TimeUnit.SECONDS));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AssertionError(e);
        }
    }

    private static void until(BooleanSupplier condition) {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
        while (!condition.getAsBoolean()) {
            assertTrue(System.nanoTime() - deadline < 0, "condition timed out");
            Thread.onSpinWait();
        }
    }

    private static class Exposed extends AsyncExecutor {
        Exposed(Executor executor) {
            super(executor);
        }

        void submit(FutureTask<?> task) {
            execute(task);
        }
    }

    private static class Paused extends AsyncExecutor {
        final CountDownLatch selected = new CountDownLatch(1);
        final CountDownLatch resume = new CountDownLatch(1);

        Paused(Executor executor) {
            super(executor);
        }

        @Override
        public Executor getExecutor() {
            Executor result = super.getExecutor();
            selected.countDown();
            await(resume);
            return result;
        }
    }

    @Test
    void shutdownCannotMissSelectedBorrowedWork() throws Exception {
        ExecutorService pool = Executors.newSingleThreadExecutor();
        Paused executor = new Paused(pool);
        AtomicInteger calls = new AtomicInteger();
        AtomicReference<Throwable> failure = new AtomicReference<>();
        Thread submit = new Thread(() -> {
            try {
                executor.execute((Callable<Integer>) calls::incrementAndGet).get(5, TimeUnit.SECONDS);
            } catch (Throwable e) {
                failure.set(e);
            }
        });
        Thread shutdown = new Thread(() -> executor.shutdownAndAwait(5, TimeUnit.SECONDS));
        try {
            submit.start();
            await(executor.selected);
            shutdown.start();
            // Hold selection before the reservation. Shutdown must block on that same admission lock.
            until(() -> shutdown.getState() == Thread.State.BLOCKED || !shutdown.isAlive());
            assertTrue(shutdown.isAlive());
            executor.resume.countDown();
            submit.join(6000);
            shutdown.join(6000);
            assertFalse(submit.isAlive());
            assertFalse(shutdown.isAlive());
            assertNull(failure.get());
            assertEquals(1, calls.get());
            assertTrue(executor.isTerminated());
            assertFalse(pool.isShutdown());
            assertThrows(IllegalStateException.class, () -> executor.execute(() -> {
            }));
        } finally {
            executor.resume.countDown();
            pool.shutdownNow();
            assertTrue(pool.awaitTermination(5, TimeUnit.SECONDS));
        }
    }

    @Test
    void inlineDoneErrorAndDispatchRejectionReleaseExactlyOnce() throws Exception {
        Exposed direct = new Exposed(Runnable::run);
        Error marker = new AssertionError("done");
        FutureTask<Integer> task = new FutureTask<>(() -> 7) {
            @Override
            protected void done() {
                throw marker;
            }
        };
        assertSame(marker, assertThrows(Error.class, () -> direct.submit(task)));
        direct.shutdown();
        assertTrue(direct.isTerminated());
        assertEquals(7, task.get());
        Exposed rejected = new Exposed(command -> {
            throw new RejectedExecutionException();
        });
        assertThrows(RejectedExecutionException.class, () -> rejected.submit(new FutureTask<>(() -> 1)));
        rejected.shutdown();
        assertTrue(rejected.isTerminated());
    }

    @Test
    void ownedReservationRemainsVisibleUntilLateDispatchRejects() throws Exception {
        CountDownLatch dispatch = new CountDownLatch(1);
        CountDownLatch resume = new CountDownLatch(1);
        AtomicReference<ExecutorService> ownedPool = new AtomicReference<>();
        AsyncExecutor executor = new AsyncExecutor() {
            @Override
            public Executor getExecutor() {
                Executor real = super.getExecutor();
                ownedPool.set((ExecutorService) real);
                return command -> {
                    dispatch.countDown();
                    await(resume);
                    real.execute(command);
                };
            }
        };
        AtomicReference<Throwable> failure = new AtomicReference<>();
        Thread submit = new Thread(() -> {
            try {
                executor.execute(() -> {
                });
            } catch (Throwable e) {
                failure.set(e);
            }
        });
        try {
            submit.start();
            await(dispatch);
            executor.shutdown();
            // Once the owned pool has terminated, only the held admission can prevent wrapper termination.
            assertTrue(ownedPool.get().awaitTermination(5, TimeUnit.SECONDS));
            assertFalse(executor.isTerminated());
            resume.countDown();
            submit.join(6000);
            assertFalse(submit.isAlive());
            executor.shutdownAndAwait(5, TimeUnit.SECONDS);
            assertInstanceOf(RejectedExecutionException.class, failure.get());
            assertTrue(executor.isTerminated());
        } finally {
            resume.countDown();
            executor.shutdownAndAwait(5, TimeUnit.SECONDS);
        }
    }
}

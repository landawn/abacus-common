package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

public class ObserverLifecycleTest extends TestBase {
    @Test
    void timedLimitWakesEmptyQueueAndPreservesLaterItems() throws Exception {
        PollQueue<Integer> queue = new PollQueue<>();
        Observer<List<Integer>> observer = Observer.of(queue).buffer(1, TimeUnit.DAYS).limit(1);
        Signals<List<Integer>> signals = new Signals<>();
        signals.subscribe(observer);
        await(queue.waiting);
        List<ScheduledFuture<?>> tasks = new ArrayList<>(owner(observer).scheduledFutures.keySet());
        fire(observer, "emitPeriodically");
        signals.normal(observer, List.of(List.of()));
        await(queue.exited);
        assertTrue(tasks.stream().allMatch(Future::isCancelled));
        queue.add(99);
        assertEquals(99, queue.peek());
        assertEquals(1, queue.polls.get());
        fire(observer, "emitPeriodically");
        assertEquals(1, signals.completions.get());
        assertEquals(1, signals.values.size());
    }

    @Test
    void timedFailuresCancelPendingOperatorsAndDeliverOneError() throws Exception {
        for (String kind : List.of("buffer", "debounce", "throttle")) {
            PollQueue<String> queue = new PollQueue<>();
            queue.add("日本語🙂");
            Observer<?> observer = switch (kind) {
                case "buffer" -> Observer.of(queue).debounce(1, TimeUnit.DAYS).buffer(1, TimeUnit.DAYS);
                case "debounce" -> Observer.of(queue).debounce(1, TimeUnit.DAYS);
                default -> Observer.of(queue).throttleLast(1, TimeUnit.DAYS);
            };
            RuntimeException failure = new IllegalStateException(kind);
            List<Exception> errors = new CopyOnWriteArrayList<>();
            AtomicInteger completed = new AtomicInteger();
            CountDownLatch done = new CountDownLatch(1);
            observer.observe(value -> {
                throw failure;
            }, error -> {
                errors.add(error);
                done.countDown();
            }, completed::incrementAndGet);
            await(queue.waiting);
            Object pendingOperator = operator(observer, "emitPending");
            ScheduledFuture<?> pending = (ScheduledFuture<?>) field(pendingOperator, "future");
            fire(observer, kind.equals("buffer") ? "emitPeriodically" : "emitPending");
            await(done);
            settled(observer);
            await(queue.exited);
            assertEquals(List.of(failure), errors);
            assertEquals(0, completed.get());
            if (kind.equals("buffer")) {
                assertTrue(pending.isCancelled());
                assertNotEquals("日本語🙂", ((Observer.Dispatcher<?>) pendingOperator).holder.value());
            }
            finish(observer);
            assertEquals(0, completed.get());
        }
    }

    @Test
    void timersCancelImmediatelyAndZeroLimitStartsNoSourceWork() throws Exception {
        for (boolean interval : List.of(false, true)) {
            Observer<List<Long>> observer = (interval ? Observer.interval(1, 1, TimeUnit.DAYS) : Observer.timer(1, TimeUnit.DAYS)).buffer(1, TimeUnit.DAYS)
                    .limit(1);
            Signals<List<Long>> signals = new Signals<>();
            signals.subscribe(observer);
            ScheduledFuture<?> source = (ScheduledFuture<?>) observerField(observer, "sourceFuture");
            fire(observer, "emitPeriodically");
            signals.normal(observer, List.of(List.of()));
            assertTrue(source.isCancelled());
        }
        PollQueue<Integer> queue = new PollQueue<>();
        Observer<List<Integer>> zero = Observer.of(queue).buffer(1, TimeUnit.DAYS).limit(0);
        Signals<List<Integer>> signals = new Signals<>();
        signals.subscribe(zero);
        signals.normal(zero, List.of());
        assertEquals(0, queue.polls.get());
    }

    @Test
    void normalCompletionFlushesDownstreamAndHonorsOperatorPolicies() throws Exception {
        Signals<List<String>> limited = new Signals<>();
        Observer<List<String>> observer = Observer.of(List.of("é🙂", "remaining")).limit(1).buffer(1, TimeUnit.DAYS);
        limited.subscribe(observer);
        limited.normal(observer, List.of(List.of("é🙂")));
        Signals<String> debounce = new Signals<>();
        Observer<String> debounced = Observer.of(List.of("é🙂")).debounce(1, TimeUnit.DAYS);
        debounce.subscribe(debounced);
        debounce.normal(debounced, List.of("é🙂"));
        Signals<String> throttle = new Signals<>();
        Observer<String> throttled = Observer.of(List.of("é🙂")).throttleLast(1, TimeUnit.DAYS);
        throttle.subscribe(throttled);
        throttle.normal(throttled, List.of());
        Signals<List<Integer>> window = new Signals<>();
        Observer<List<Integer>> windows = Observer.of(List.of(1)).buffer(2, 1, TimeUnit.DAYS);
        window.subscribe(windows);
        window.normal(windows, List.of(List.of(1)));
    }

    @Test
    void externalInterruptRemainsAnErrorAndBlockedIteratorStopsCooperatively() throws Exception {
        PollQueue<Integer> queue = new PollQueue<>();
        Observer<Integer> observer = Observer.of(queue);
        Signals<Integer> signals = new Signals<>();
        signals.subscribe(observer);
        await(queue.waiting);
        queue.worker.interrupt();
        await(signals.done);
        settled(observer);
        assertInstanceOf(InterruptedException.class, signals.errors.get(0));
        assertEquals(0, signals.completions.get());

        CountDownLatch entered = new CountDownLatch(1), release = new CountDownLatch(1), returned = new CountDownLatch(1);
        AtomicInteger next = new AtomicInteger();
        Iterator<Integer> iterator = new Iterator<>() {
            @Override
            public boolean hasNext() {
                entered.countDown();
                await(release);
                returned.countDown();
                return true;
            }

            @Override
            public Integer next() {
                next.incrementAndGet();
                return 1;
            }
        };
        Observer<List<Integer>> blocked = Observer.of(iterator).buffer(1, TimeUnit.DAYS).limit(1);
        Signals<List<Integer>> stopped = new Signals<>();
        stopped.subscribe(blocked);
        await(entered);
        try {
            fire(blocked, "emitPeriodically");
            stopped.normal(blocked, List.of(List.of()));
        } finally {
            release.countDown();
        }
        await(returned);
        finish(blocked);
        assertEquals(0, next.get());
    }

    @Test
    void scheduledErrorWinsAgainstCompetingCompletionWithoutInterruptingCallback() throws Exception {
        PollQueue<Integer> queue = new PollQueue<>();
        Observer<List<Integer>> observer = Observer.of(queue).buffer(1, TimeUnit.DAYS);
        CountDownLatch entered = new CountDownLatch(1), release = new CountDownLatch(1), done = new CountDownLatch(1);
        AtomicInteger errors = new AtomicInteger(), completed = new AtomicInteger();
        List<Throwable> threadErrors = new CopyOnWriteArrayList<>();
        observer.observe(value -> {
            entered.countDown();
            await(release);
            assertFalse(Thread.currentThread().isInterrupted());
            throw new IllegalArgumentException("scheduled failure");
        }, error -> {
            errors.incrementAndGet();
            done.countDown();
        }, () -> {
            completed.incrementAndGet();
            done.countDown();
        });
        await(queue.waiting);
        Thread callback = new Thread(() -> {
            try {
                fire(observer, "emitPeriodically");
            } catch (Throwable error) {
                threadErrors.add(error);
            }
        });
        Thread completion = new Thread(() -> {
            try {
                finish(observer);
            } catch (Throwable error) {
                threadErrors.add(error);
            }
        });
        callback.start();
        await(entered);
        completion.start();
        release.countDown();
        await(done);
        callback.join(5000);
        completion.join(5000);
        assertFalse(callback.isAlive());
        assertFalse(completion.isAlive());
        assertTrue(threadErrors.isEmpty(), threadErrors.toString());
        settled(observer);
        await(queue.exited);
        assertEquals(1, errors.get());
        assertEquals(0, completed.get());
    }

    @Test
    void failingCompletionCallbackStillCleansUpWithoutSecondTerminal() throws Exception {
        PollQueue<Integer> queue = new PollQueue<>();
        Observer<Integer> observer = Observer.of(queue).debounce(1, TimeUnit.DAYS);
        RuntimeException failure = new IllegalStateException("completion");
        AtomicInteger errors = new AtomicInteger(), completed = new AtomicInteger();
        observer.observe(value -> {
        }, error -> errors.incrementAndGet(), () -> {
            completed.incrementAndGet();
            throw failure;
        });
        await(queue.waiting);
        InvocationTargetException actual = assertThrows(InvocationTargetException.class, () -> finish(observer));
        assertSame(failure, actual.getCause());
        settled(observer);
        await(queue.exited);
        finish(observer);
        assertEquals(1, completed.get());
        assertEquals(0, errors.get());
    }

    @Test
    void realScheduledWindowAndStartupRacesHaveOneTerminal() throws Exception {
        for (int i = 0; i < 30; i++) {
            Observer<Long> observer = Observer.interval(0, 1, TimeUnit.DAYS).limit(1);
            Signals<Long> signals = new Signals<>();
            signals.subscribe(observer);
            signals.normal(observer, List.of(0L));
            assertTrue(((Future<?>) observerField(observer, "sourceFuture")).isCancelled());
        }
        PollQueue<Integer> queue = new PollQueue<>();
        Observer<List<Integer>> timed = Observer.of(queue).buffer(20, 10, TimeUnit.MILLISECONDS).limit(1);
        Signals<List<Integer>> signals = new Signals<>();
        signals.subscribe(timed);
        signals.normal(timed, List.of(List.of()));
    }

    @Test
    void fatalSourceCallbacksAndIteratorReadsStopWithoutTerminalSignals() throws Exception {
        for (String mode : List.of("map", "action", "hasNext", "next")) {
            AssertionError fatal = new AssertionError(mode);
            CountDownLatch escaped = new CountDownLatch(1);
            List<Throwable> uncaught = new CopyOnWriteArrayList<>();
            Runnable fail = () -> {
                Thread.currentThread().setUncaughtExceptionHandler((thread, error) -> {
                    uncaught.add(error);
                    escaped.countDown();
                });
                throw fatal;
            };
            Iterator<Integer> source = new Iterator<>() {
                @Override
                public boolean hasNext() {
                    if (mode.equals("hasNext")) {
                        fail.run();
                    }
                    return true;
                }

                @Override
                public Integer next() {
                    if (mode.equals("next")) {
                        fail.run();
                    }
                    return 1;
                }
            };
            Observer<Integer> observer = Observer.of(source);
            if (mode.equals("map")) {
                observer = observer.map(value -> {
                    fail.run();
                    return value;
                });
            }
            Observer<List<Integer>> buffered = observer.buffer(1, TimeUnit.DAYS, 1);
            AtomicInteger terminals = new AtomicInteger();
            buffered.observe(value -> {
                if (mode.equals("action")) {
                    fail.run();
                }
            }, error -> terminals.incrementAndGet(), terminals::incrementAndGet);
            await(escaped);
            assertEquals(List.of(fatal), uncaught);
            settled(buffered);
            assertTrue(((List<?>) observerField(buffered, "terminationActions")).isEmpty());
            finish(buffered);
            assertEquals(0, terminals.get());
        }
    }

    @Test
    void fatalTimedCallbacksWakeQueueAndDiscardPendingState() throws Exception {
        for (String kind : List.of("buffer", "debounce", "throttle")) {
            PollQueue<String> queue = new PollQueue<>();
            queue.add("\u65e5\u672c\u8a9e\ud83d\ude42");
            Observer<?> observer = switch (kind) {
                case "buffer" -> Observer.of(queue).debounce(1, TimeUnit.DAYS).buffer(1, TimeUnit.DAYS);
                case "debounce" -> Observer.of(queue).debounce(1, TimeUnit.DAYS);
                default -> Observer.of(queue).throttleLast(1, TimeUnit.DAYS);
            };
            AssertionError fatal = new AssertionError(kind);
            AtomicInteger terminals = new AtomicInteger();
            observer.observe(value -> {
                throw fatal;
            }, error -> terminals.incrementAndGet(), terminals::incrementAndGet);
            await(queue.waiting);
            Object pending = operator(observer, "emitPending");
            Future<?> future = (Future<?>) field(pending, "future");
            InvocationTargetException thrown = assertThrows(InvocationTargetException.class,
                    () -> fire(observer, kind.equals("buffer") ? "emitPeriodically" : "emitPending"));
            assertSame(fatal, thrown.getCause());
            settled(observer);
            await(queue.exited);
            assertTrue(future.isCancelled());
            assertTrue(((List<?>) observerField(observer, "terminationActions")).isEmpty());
            finish(observer);
            assertEquals(0, terminals.get());
        }
    }

    @Test
    void fatalTerminalCallbacksCleanUpWithoutAnotherSignal() throws Exception {
        for (boolean errorSignal : List.of(false, true)) {
            PollQueue<Integer> queue = new PollQueue<>();
            Observer<List<Integer>> observer = Observer.of(queue).buffer(1, TimeUnit.DAYS);
            AssertionError fatal = new AssertionError("terminal");
            AtomicInteger terminals = new AtomicInteger();
            Runnable callback = () -> {
                terminals.incrementAndGet();
                throw fatal;
            };
            observer.observe(value -> {
            }, error -> callback.run(), callback);
            await(queue.waiting);
            Method finish = Observer.class.getDeclaredMethod("finishSubscription", Exception.class);
            finish.setAccessible(true);
            InvocationTargetException thrown = assertThrows(InvocationTargetException.class,
                    () -> finish.invoke(owner(observer), new Object[] { errorSignal ? new IllegalArgumentException("source") : null }));
            assertSame(fatal, thrown.getCause());
            settled(observer);
            await(queue.exited);
            finish(observer);
            assertEquals(1, terminals.get());
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    void failedStartupAndSubmissionReleaseAllStateAndPreserveOriginalFailure() throws Exception {
        for (boolean fatalFailure : List.of(false, true)) {
            Throwable failure = fatalFailure ? new AssertionError("startup") : new IllegalStateException("startup");
            Runnable fail = () -> {
                if (failure instanceof Error error) {
                    throw error;
                }
                throw (RuntimeException) failure;
            };
            Observer<List<Integer>> starting = Observer.of(new LinkedBlockingQueue<Integer>()).buffer(1, TimeUnit.DAYS);
            // Inject failure after the buffer's deferred initializer on the lifecycle owner.
            ((List<Runnable>) observerField(starting, "subscriptionActions")).add(fail);
            AtomicInteger terminals = new AtomicInteger();
            assertSame(failure, assertThrows(Throwable.class, () -> starting.observe(value -> {
            }, error -> terminals.incrementAndGet(), terminals::incrementAndGet)));
            settled(starting);

            PollQueue<Integer> queue = new PollQueue<>();
            Observer<List<Integer>> active = Observer.of(queue).buffer(1, TimeUnit.DAYS);
            active.observe(value -> {
            }, error -> terminals.incrementAndGet(), terminals::incrementAndGet);
            await(queue.waiting);
            AssertionError secondary = new AssertionError("cleanup");
            AtomicInteger lastCleanup = new AtomicInteger();
            synchronized (observerField(active, "eventGate")) {
                List<Runnable> cleanups = (List<Runnable>) observerField(active, "terminationActions");
                cleanups.add(0, () -> {
                    throw secondary;
                });
                cleanups.add(lastCleanup::incrementAndGet);
            }
            Method submit = Observer.class.getDeclaredMethod("startSource", Runnable.class);
            submit.setAccessible(true);
            InvocationTargetException thrown = assertThrows(InvocationTargetException.class, () -> submit.invoke(owner(active), fail));
            assertSame(failure, thrown.getCause());
            assertArrayEquals(new Throwable[] { secondary }, failure.getSuppressed());
            assertEquals(1, lastCleanup.get());
            settled(active);
            await(queue.exited);
            assertTrue(((List<?>) observerField(active, "terminationActions")).isEmpty());
            assertEquals(0, terminals.get());
        }
    }

    private static void await(CountDownLatch latch) {
        try {
            assertTrue(latch.await(5, TimeUnit.SECONDS), "Timed out waiting for a lifecycle transition");
        } catch (InterruptedException error) {
            Thread.currentThread().interrupt();
            throw new AssertionError(error);
        }
    }

    private static Object field(Object target, String name) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return field.get(target);
    }

    private static Object observerField(Observer<?> observer, String name) throws Exception {
        Field field = Observer.class.getDeclaredField(name);
        field.setAccessible(true);
        return field.get(owner(observer));
    }

    // Typed handles share a pipeline but intentionally do not own its lifecycle fields.
    private static Observer<?> owner(Observer<?> observer) throws Exception {
        Field field = Observer.class.getDeclaredField("pipelineOwner");
        field.setAccessible(true);
        return (Observer<?>) field.get(observer);
    }

    private static void settled(Observer<?> observer) throws Exception {
        synchronized (observerField(observer, "eventGate")) {
            assertFalse(owner(observer).hasMore);
            assertTrue(owner(observer).scheduledFutures.isEmpty());
        }
    }

    private static Object operator(Observer<?> observer, String methodName) throws Exception {
        Object node = observer.dispatcher;
        Field downstream = Observer.Dispatcher.class.getDeclaredField("downDispatcher");
        downstream.setAccessible(true);
        while (node != null) {
            for (Method method : node.getClass().getDeclaredMethods()) {
                if (method.getName().equals(methodName)) {
                    return node;
                }
            }
            node = downstream.get(node);
        }
        throw new AssertionError("Missing operator " + methodName);
    }

    // Fire the scheduled body through the production event gate; no sleeps or scheduler replacement.
    private static void fire(Observer<?> observer, String methodName) throws Exception {
        Object node = operator(observer, methodName);
        Method callback = java.util.Arrays.stream(node.getClass().getDeclaredMethods()).filter(m -> m.getName().equals(methodName)).findFirst().orElseThrow();
        callback.setAccessible(true);
        Method event = Observer.class.getDeclaredMethod("runEvent", Runnable.class);
        event.setAccessible(true);
        if (methodName.equals("emitPending")) {
            // This test runs a one-shot callback early; remove the real scheduled invocation it replaces.
            ((Future<?>) field(node, "future")).cancel(false);
        }
        event.invoke(owner(observer), (Runnable) () -> {
            try {
                if (callback.getParameterCount() == 0) {
                    callback.invoke(node);
                } else {
                    callback.invoke(node, field(node, "generation"));
                }
            } catch (InvocationTargetException error) {
                if (error.getCause() instanceof Error fatal) {
                    throw fatal;
                }
                if (error.getCause() instanceof RuntimeException failure) {
                    throw failure;
                }
                throw new AssertionError(error.getCause());
            } catch (Exception error) {
                throw new AssertionError(error);
            }
        });
    }

    @Test
    void jvmExitIsNotStalledByASubscriptionParkedOnAnEmptyQueue() throws Exception {
        // Measured in a child JVM because JVM exit time is the only place this shows: the observer pools used
        // to be stopped by a hook that calls shutdown() and then awaits termination for 120 seconds, and
        // shutdown() never interrupts a running emission, so a subscription parked in poll(Long.MAX_VALUE)
        // held the JVM open for the hook's full two minutes after main returned.
        final String javaCommand = ProcessHandle.current()
                .info()
                .command()
                .orElse(System.getProperty("java.home") + File.separator + "bin" + File.separator + "java");
        final ProcessBuilder builder = new ProcessBuilder(javaCommand, "-Xmx256m", "-cp", System.getProperty("java.class.path"), JvmExitProbe.class.getName());
        builder.redirectErrorStream(true);

        final StringBuilder out = new StringBuilder();
        final long started = System.currentTimeMillis();
        final Process child = builder.start();
        // The child's output must be drained while it runs: reading it to EOF would itself wait for the exit
        // this test is timing.
        final Thread drain = new Thread(() -> {
            try (java.io.BufferedReader lines = new java.io.BufferedReader(new InputStreamReader(child.getInputStream(), StandardCharsets.UTF_8))) {
                for (String line = lines.readLine(); line != null; line = lines.readLine()) {
                    synchronized (out) {
                        out.append(line).append('\n');
                    }
                }
            } catch (IOException ignored) {
                // The child is gone; whatever was read before that is enough to judge the run.
            }
        });
        drain.setDaemon(true);
        drain.start();

        final boolean exited;

        try {
            exited = child.waitFor(40, TimeUnit.SECONDS);
        } finally {
            if (child.isAlive()) {
                child.destroyForcibly();
                child.waitFor(20, TimeUnit.SECONDS);
            }
        }

        final long elapsed = System.currentTimeMillis() - started;
        drain.join(2_000);

        final String output;

        synchronized (out) {
            output = out.toString();
        }

        // There is nothing to measure if this JVM's class path cannot launch the probe at all.
        assumeTrue(output.contains(JvmExitProbe.PARKED), () -> "the probe JVM did not start: " + output);
        assertTrue(exited, () -> "JVM exit was stalled by the observer shutdown hooks: still running " + elapsed + " ms after launch; output=" + output);
        assertFalse(output.contains("Exception in thread"),
                () -> "the shutdown interrupt escaped as a source failure instead of stopping the subscription: " + output);
    }

    public static final class JvmExitProbe {
        static final String PARKED = "OBSERVER-PARKED-AND-MAIN-RETURNED";

        public static void main(final String[] args) throws Exception {
            final LinkedBlockingQueue<Integer> queue = new LinkedBlockingQueue<>();
            final CountDownLatch received = new CountDownLatch(1);
            // No onError on purpose: an interrupt reported as a source failure reaches ON_ERROR_MISSING, which
            // rethrows it out of a pool thread and prints an uncaught stack trace as the JVM exits.
            Observer.of(queue).observe(item -> received.countDown());
            queue.offer(1);
            received.await(10, TimeUnit.SECONDS);
            // The emission loop is now parked in poll(Long.MAX_VALUE) and nothing will ever complete it.
            System.out.println(PARKED);
            System.out.flush();
        }
    }

    private static void finish(Observer<?> observer) throws Exception {
        Method method = Observer.class.getDeclaredMethod("finishSubscription", Exception.class);
        method.setAccessible(true);
        method.invoke(owner(observer), new Object[] { null });
    }

    private static final class PollQueue<E> extends LinkedBlockingQueue<E> {
        final CountDownLatch waiting = new CountDownLatch(1), exited = new CountDownLatch(1);
        final AtomicInteger polls = new AtomicInteger();
        volatile Thread worker;

        @Override
        public E poll(long timeout, TimeUnit unit) throws InterruptedException {
            polls.incrementAndGet();
            if (!isEmpty()) {
                return super.poll(timeout, unit);
            }
            worker = Thread.currentThread();
            waiting.countDown();
            try {
                return super.poll(timeout, unit);
            } finally {
                exited.countDown();
            }
        }
    }

    private static final class Signals<T> {
        final List<T> values = new CopyOnWriteArrayList<>();
        final List<Exception> errors = new CopyOnWriteArrayList<>();
        final AtomicInteger completions = new AtomicInteger();
        final CountDownLatch done = new CountDownLatch(1);

        void subscribe(Observer<T> observer) {
            observer.observe(values::add, error -> {
                errors.add(error);
                done.countDown();
            }, () -> {
                completions.incrementAndGet();
                done.countDown();
            });
        }

        void normal(Observer<?> observer, List<T> expected) throws Exception {
            await(done);
            settled(observer);
            assertEquals(List.of(), errors);
            assertEquals(1, completions.get());
            assertEquals(expected, values);
        }
    }
}

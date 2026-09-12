package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.StringWriter;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import com.landawn.abacus.TestBase;

/**
 * Regression tests for the {@code Profiler} findings of the second review pass.
 *
 * <p>{@code Profiler.suspend()} is a process-wide switch, so each test that relies on a real
 * thread/loop count restores the previous setting itself.</p>
 */
public class ProfilerRegressionTest extends TestBase {

    private boolean wasSuspended;

    @BeforeEach
    public void rememberSuspension() {
        wasSuspended = Profiler.isSuspended();
        Profiler.resume();
    }

    @AfterEach
    public void restoreSuspension() {
        if (wasSuspended) {
            Profiler.suspend();
        } else {
            Profiler.resume();
        }
    }

    // ---------------------------------------------------------------------------------------------
    // B8 / D6 - a Throwables.Runnable whose run() is INHERITED used to be rejected outright
    // ---------------------------------------------------------------------------------------------

    /** Declares {@code run()}; {@link InheritedRunCommand} inherits it without redeclaring it. */
    public abstract static class BaseCommand implements Throwables.Runnable<Exception> {
        final AtomicInteger invocations = new AtomicInteger();

        @Override
        public void run() {
            invocations.incrementAndGet();
        }
    }

    public static class InheritedRunCommand extends BaseCommand {
    }

    @Test
    @Timeout(120)
    public void testB8_runAcceptsACommandThatInheritsRun() {
        final InheritedRunCommand command = new InheritedRunCommand();

        // Pre-fix: IllegalArgumentException("No method found by name: run"), because the reflective
        // lookup only considered methods DECLARED by InheritedRunCommand.
        final Profiler.MultiLoopsStatistics stats = Profiler.run(1, 3, 1, command);

        assertNotNull(stats);
        assertEquals(3, command.invocations.get());
        assertEquals(List.of("run"), stats.getMethodNameList());
        assertEquals(3, stats.getMethodInvocationCount("run"));
    }

    @Test
    @Timeout(120)
    public void testB8_lambdaAndAnonymousCommandsStillWork() {
        final AtomicInteger lambdaCalls = new AtomicInteger();
        assertEquals(2, Profiler.run(1, 2, 1, () -> lambdaCalls.incrementAndGet()).getMethodInvocationCount("run"));
        assertEquals(2, lambdaCalls.get());

        final AtomicInteger anonCalls = new AtomicInteger();
        final Profiler.MultiLoopsStatistics stats = Profiler.run(1, 2, 1, new Throwables.Runnable<Exception>() {
            @Override
            public void run() {
                anonCalls.incrementAndGet();
            }
        });
        assertEquals(2, anonCalls.get());
        assertEquals(2, stats.getMethodInvocationCount("run"));
    }

    @Test
    @Timeout(120)
    public void testB8_labelIsStillUsedAsTheMethodName() {
        final Profiler.MultiLoopsStatistics stats = Profiler.run(1, 1, 1, "myLabel", () -> {
        });
        assertEquals(List.of("myLabel"), stats.getMethodNameList());

        // A null label is still normalized to the string "null" rather than blowing up.
        assertEquals(List.of("null"), Profiler.run(1, 1, 1, (String) null, () -> {
        }).getMethodNameList());
    }

    @Test
    @Timeout(120)
    public void testB8_thrownExceptionIsRecordedNotPropagated() {
        final IllegalStateException boom = new IllegalStateException("boom");
        final Profiler.MultiLoopsStatistics stats = Profiler.run(1, 1, 1, "throwing", () -> {
            throw boom;
        });

        final List<Profiler.MethodStatistics> failures = stats.getAllFailedMethodStatisticsList();
        assertEquals(1, failures.size());
        assertTrue(failures.get(0).isFailed());
        // The exception the profiled code threw is recorded, not a reflective wrapper around it.
        assertEquals(boom, failures.get(0).getResult());
    }

    @Test
    @Timeout(120)
    public void testB8_thrownErrorIsRecordedNotPropagated() {
        // The reflective path wrapped an Error in InvocationTargetException, so it was captured. Direct
        // invocation must not let it escape and kill the worker thread instead.
        final StackOverflowError boom = new StackOverflowError("deep");
        final Profiler.MultiLoopsStatistics stats = Profiler.run(1, 1, 1, "throwingError", () -> {
            throw boom;
        });

        assertEquals(1, stats.getAllFailedMethodStatisticsList().size());
        assertEquals(boom, stats.getAllFailedMethodStatisticsList().get(0).getResult());
    }

    // ---------------------------------------------------------------------------------------------
    // Reflective overloads must be unaffected by the Invocation refactor
    // ---------------------------------------------------------------------------------------------

    final AtomicInteger namedCalls = new AtomicInteger();

    public void namedTarget() {
        namedCalls.incrementAndGet();
    }

    @Test
    @Timeout(120)
    public void testReflectiveOverloadStillWorks() {
        namedCalls.set(0);
        final Profiler.MultiLoopsStatistics stats = Profiler.run(this, "namedTarget", 1, 3, 1);

        assertEquals(3, namedCalls.get());
        assertEquals(List.of("namedTarget"), stats.getMethodNameList());
        assertEquals(3, stats.getMethodInvocationCount("namedTarget"));
    }

    @Test
    @Timeout(120)
    public void testReflectiveOverloadRejectsAnUnknownMethod() {
        assertEquals("No method found by name: noSuchMethod",
                org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class, () -> Profiler.run(this, "noSuchMethod", 1, 1, 1)).getMessage());
    }

    // ---------------------------------------------------------------------------------------------
    // D7 / report rendering - the three writers now share one percentile computation
    // ---------------------------------------------------------------------------------------------

    @Test
    @Timeout(120)
    public void testReportRenderersAgreeAndKeepTheirColumns() {
        final Profiler.MultiLoopsStatistics stats = Profiler.run(2, 5, 1, "work", () -> N.sleep(1));

        final StringWriter text = new StringWriter();
        stats.writeResult(text);
        final StringWriter html = new StringWriter();
        stats.writeHtmlResult(html);
        final StringWriter xml = new StringWriter();
        stats.writeXmlResult(xml);

        // 11 percentile columns plus avg/min/max in every format.
        assertEquals(11, Strings.countMatches(html.toString(), "&gt;=</th>"));
        assertEquals(3, Strings.countMatches(text.toString(), "time|"));

        // The XML element names must keep their historical spelling: String.valueOf(0.0001d) is "1.0E-4",
        // which would have renamed the first element had the tags been derived from the values.
        for (final String tag : new String[] { "_0.0001", "_0.001", "_0.01", "_0.1", "_0.2", "_0.5", "_0.8", "_0.9", "_0.99", "_0.999", "_0.9999" }) {
            assertTrue(xml.toString().contains("<" + tag + ">"), "missing <" + tag + "> in:\n" + xml);
            assertTrue(xml.toString().contains("</" + tag + ">"), "missing </" + tag + "> in:\n" + xml);
        }
        assertFalse(xml.toString().contains("1.0E-4"), xml.toString());

        assertTrue(xml.toString().contains("<avgTime>"));
        assertTrue(xml.toString().contains("<minTime>"));
        assertTrue(xml.toString().contains("<maxTime>"));
        assertTrue(xml.toString().contains("<threadNum>2</threadNum>"));
        assertTrue(xml.toString().contains("<loops>5</loops>"), xml.toString());
    }

    @Test
    @Timeout(120)
    public void testFailureRateIsFormattedNotRaw() {
        // 3 invocations, 1 failing -> 33.333...%, which used to be printed at full double precision.
        final AtomicInteger n = new AtomicInteger();
        final Profiler.MultiLoopsStatistics stats = Profiler.run(1, 3, 1, "sometimesFails", () -> {
            if (n.incrementAndGet() == 1) {
                throw new IllegalStateException("first call fails");
            }
        });

        final StringWriter text = new StringWriter();
        stats.writeResult(text);

        assertTrue(text.toString().contains("Errors:1 (33.33%)"), text.toString());
        assertFalse(text.toString().contains("33.33333"), text.toString());
    }

    @Test
    @Timeout(120)
    public void testHtmlReportUsesAHorizontalRuleNotAnAsciiSeparator() {
        final Profiler.MultiLoopsStatistics stats = Profiler.run(1, 2, 1, "work", () -> {
        });

        final StringWriter html = new StringWriter();
        stats.writeHtmlResult(html);

        assertEquals(2, Strings.countMatches(html.toString(), "<hr/>"));
        assertFalse(html.toString().contains("========"), "the 120-char ASCII rule does not belong in HTML");

        // The plain-text report keeps its ASCII separator.
        final StringWriter text = new StringWriter();
        stats.writeResult(text);
        assertEquals(2, Strings.countMatches(text.toString(),
                "========================================================================================================================"));
    }

    @Test
    @Timeout(120)
    public void testRunProducesMutableLoopStatistics() {
        // runLoop returns a single MethodStatistics now and runLoops wraps it. The wrapper must stay a
        // mutable ArrayList: SingleLoopStatistics publishes it through getMethodStatisticsList() and appends
        // to it in addMethodStatistics(), so an immutable list would make both throw.
        final Profiler.MultiLoopsStatistics stats = Profiler.run(1, 2, 1, "work", () -> {
        });

        final List<Profiler.LoopStatistics> loops = stats.getLoopStatisticsList();
        assertEquals(2, loops.size());

        final Profiler.SingleLoopStatistics loop = (Profiler.SingleLoopStatistics) loops.get(0);
        assertEquals(1, loop.getMethodStatisticsList().size());

        loop.addMethodStatistics(new Profiler.MethodStatistics("extra", 0, 1, 0, 1_000_000));
        assertEquals(2, loop.getMethodStatisticsList().size());
        assertEquals(1, loop.getMethodInvocationCount("extra"));
    }

    @Test
    @Timeout(120)
    public void testArgumentThatIsItselfAnArrayIsPassedAsOneArgument() {
        // Method.invoke is varargs: invoking with the raw argument would SPREAD an Object[] across the
        // parameters instead of passing it as the single argument the profiled method declares.
        arrayArg = null;
        final Object[] arg = { "a", "b" };
        Profiler.run(this, getArrayTarget(), arg, 1, 1, 1);

        assertSame(arg, arrayArg);
    }

    Object arrayArg;

    public void arrayTarget(final Object value) {
        arrayArg = value;
    }

    private java.lang.reflect.Method getArrayTarget() {
        return ClassUtil.getDeclaredMethod(ProfilerRegressionTest.class, "arrayTarget", Object.class);
    }

    @Test
    @Timeout(120)
    public void testSuspendedRunStillExecutesOnceAndReportsRealCounts() {
        Profiler.suspend();

        try {
            final AtomicInteger calls = new AtomicInteger();
            final Profiler.MultiLoopsStatistics stats = Profiler.run(8, 100, 3, "suspended", () -> calls.incrementAndGet());

            assertEquals(1, calls.get(), "suspension is a minimal-execution mode, not a no-op");
            assertEquals(1, stats.getThreadNum());
            assertEquals(1, stats.getMethodInvocationCount("suspended"));
        } finally {
            Profiler.resume();
        }
    }
    //
    // ==================== review fixes 2026-09-06 ====================
    // These live here rather than in ProfilerTest because ProfilerTest is @Tag("slow-test"), which
    // AbacusCoreTestSuite excludes - a test added there would never run.
    //

    @Test
    public void reviewFixes20260906_writeResultOutputStreamNullThrowsIAE() {
        final Profiler.MultiLoopsStatistics stats = Profiler.run(1, 1, 1, "x", () -> {
        });

        // All six overloads validate the destination with N.checkArgNotNull, so a null output is IAE.
        assertThrows(IllegalArgumentException.class, () -> stats.writeResult((java.io.OutputStream) null));
        assertThrows(IllegalArgumentException.class, () -> stats.writeHtmlResult((java.io.OutputStream) null));
        assertThrows(IllegalArgumentException.class, () -> stats.writeXmlResult((java.io.OutputStream) null));

        assertThrows(IllegalArgumentException.class, () -> stats.writeResult((java.io.Writer) null));
        assertThrows(IllegalArgumentException.class, () -> stats.writeHtmlResult((java.io.Writer) null));
        assertThrows(IllegalArgumentException.class, () -> stats.writeXmlResult((java.io.Writer) null));
    }

    @Test
    public void reviewFixes20260906_workerInterruptedInLoopDelayDoesNotEscape() throws Exception {
        // The command restores its own interrupt flag - the standard idiom - so the inter-loop N.sleep throws.
        // That exception used to escape runLoops and kill the worker thread, reaching the JVM's default
        // uncaught-exception handler; now it is caught, logged, and simply ends that thread's remaining loops.
        //
        // The truncated loop count is NOT what distinguishes the two: the thread stops either way. What the
        // fix changes is whether the failure escapes as an uncaught exception, so that is what this asserts.
        final java.util.concurrent.atomic.AtomicInteger invocations = new java.util.concurrent.atomic.AtomicInteger();
        final java.util.List<Throwable> uncaught = java.util.Collections.synchronizedList(new java.util.ArrayList<>());
        final Thread.UncaughtExceptionHandler previous = Thread.getDefaultUncaughtExceptionHandler();

        final Profiler.MultiLoopsStatistics stats;

        try {
            Thread.setDefaultUncaughtExceptionHandler((t, e) -> uncaught.add(e));

            stats = Profiler.run(1, 0L, 10, 5L, 1, "interrupting", () -> {
                invocations.incrementAndGet();
                Thread.currentThread().interrupt();
            });

            // The handler runs on the dying worker, which Profiler.run has already joined; poll briefly anyway
            // so a slow hand-off cannot make this pass for the wrong reason.
            for (int i = 0; i < 50 && uncaught.isEmpty(); i++) {
                Thread.sleep(10);
            }
        } finally {
            Thread.setDefaultUncaughtExceptionHandler(previous);
        }

        assertTrue(uncaught.isEmpty(), "the interrupted delay must not escape the worker: " + uncaught);

        assertNotNull(stats);
        assertEquals(1, invocations.get(), "the interrupted delay ends this thread's loops");
        assertEquals(1, stats.getMethodInvocationCount("interrupting"));

        // The statistics object must still be usable - the worker did not die mid-write.
        final StringWriter out = new StringWriter();
        stats.writeResult(out);
        assertTrue(out.toString().contains("interrupting"));

        // Control: an uninterrupted run with the same delay completes every loop.
        final java.util.concurrent.atomic.AtomicInteger clean = new java.util.concurrent.atomic.AtomicInteger();
        final Profiler.MultiLoopsStatistics ok = Profiler.run(1, 0L, 3, 1L, 1, "clean", clean::incrementAndGet);
        assertEquals(3, clean.get());
        assertEquals(3, ok.getMethodInvocationCount("clean"));
    }

    @Test
    public void reviewFixes20260906_methodMaxElapsedTimeMirrorsTheMinSibling() {
        // A recorded-but-negative elapsed time is only constructible by hand; System.nanoTime() is monotonic.
        // getMethodMinElapsedTimeInMillis has always carried a `found` flag, getMethodMaxElapsedTimeInMillis
        // seeded with 0 - so the four ways of asking for the maximum disagreed.
        final Profiler.SingleLoopStatistics loop = new Profiler.SingleLoopStatistics();
        loop.addMethodStatistics(new Profiler.MethodStatistics("m", 0, 0, 5_000_000L, 3_000_000L));

        assertEquals(-2.0d, loop.getMethodMaxElapsedTimeInMillis("m"), 1e-9);
        assertEquals(-2.0d, loop.getMethodMinElapsedTimeInMillis("m"), 1e-9);
        assertEquals(0.0d, loop.getMethodMaxElapsedTimeInMillis("absent"), 1e-9);
        assertEquals(0.0d, loop.getMethodMinElapsedTimeInMillis("absent"), 1e-9);

        final List<Profiler.LoopStatistics> loops = new java.util.ArrayList<>();
        loops.add(loop);
        final Profiler.MultiLoopsStatistics stats = new Profiler.MultiLoopsStatistics(0, 0, 0, 0, 1, loops);

        assertEquals(-2.0d, stats.getMethodMaxElapsedTimeInMillis("m"), 1e-9);
        assertEquals(-2.0d, stats.getMethodMinElapsedTimeInMillis("m"), 1e-9);
        assertEquals(-2.0d, stats.getMaxElapsedTimeMethod().getElapsedTimeInMillis(), 1e-9);
        assertEquals(0.0d, stats.getMethodMaxElapsedTimeInMillis("absent"), 1e-9);

        // A normal, positive-only run must be unaffected.
        final Profiler.MultiLoopsStatistics real = Profiler.run(1, 2, 1, "ok", () -> N.sleep(1));
        assertTrue(real.getMethodMaxElapsedTimeInMillis("ok") >= real.getMethodMinElapsedTimeInMillis("ok"));
        assertEquals(0.0d, real.getMethodMaxElapsedTimeInMillis("never-run"), 1e-9);
    }
    //
    // ==================== review fixes 2026-09-11 ====================
    //

    @Test
    public void reviewFixes20260911_cancellingTheCallerEndsTheProfilingRun() throws Exception {
        // Every public entry point but the 7-arg run(..) passes loopDelay == 0, and the interrupt that
        // shutdownNow() raises was only ever observable through the inter-loop sleep - which N.sleep skips
        // for a zero delay. A cancelled run therefore kept executing every remaining iteration, on a
        // non-daemon thread. The worker now tests a cancellation token at the top of each loop.
        assertTimeoutPreemptively(Duration.ofSeconds(60), () -> {
            final int loopNum = 1200;
            final AtomicInteger invocations = new AtomicInteger();
            final CountDownLatch running = new CountDownLatch(1);
            final AtomicReference<Throwable> callerFailure = new AtomicReference<>();

            final Thread caller = new Thread(() -> {
                try {
                    Profiler.run(1, loopNum, 1, "cancelMe", () -> {
                        invocations.incrementAndGet();
                        running.countDown();

                        // CPU-bound on purpose: this command neither observes nor clears the interrupt flag,
                        // so only the cancellation token can end the run.
                        final long until = System.nanoTime() + 2_000_000L;

                        while (System.nanoTime() < until) {
                            Thread.onSpinWait();
                        }
                    });
                } catch (final Throwable e) { // NOSONAR - the caller's failure is the assertion
                    callerFailure.set(e);
                }
            }, "profiler-cancellation-caller");

            caller.setDaemon(true);
            caller.start();

            assertTrue(running.await(30, TimeUnit.SECONDS), "the profiling worker never started");
            caller.interrupt();
            caller.join(30_000);

            assertFalse(caller.isAlive(), "interrupting the caller must unblock Profiler.run");
            assertNotNull(callerFailure.get(), "interrupting the caller must abort Profiler.run");

            // Let the iteration that was already in flight finish before sampling.
            Thread.sleep(250);
            final int atCancellation = invocations.get();
            Thread.sleep(600);

            assertEquals(atCancellation, invocations.get(), "the cancelled worker must not start further loops");
            assertTrue(atCancellation < loopNum, "the cancelled run must be truncated, but ran " + atCancellation + " of " + loopNum + " loops");
        });
    }

    @Test
    public void reviewFixes20260911_profilerWorkersAreDaemonThreads() {
        // An orphaned profiling worker must not hold the JVM open after the caller has abandoned the run.
        final AtomicBoolean daemon = new AtomicBoolean();
        final AtomicReference<String> threadName = new AtomicReference<>();

        Profiler.run(1, 1, 1, "daemonCheck", () -> {
            daemon.set(Thread.currentThread().isDaemon());
            threadName.set(Thread.currentThread().getName());
        });

        assertTrue(daemon.get(), "profiler workers must be daemon threads, but ran on " + threadName.get());
        assertTrue(threadName.get().startsWith("abacus-profiler-"), "unexpected worker thread name: " + threadName.get());
    }

    @Test
    public void reviewFixes20260911_reportsRenderAMethodNameThatHasNoSamples() {
        // LoopStatistics is a public extension point reachable through addLoopStatistics(..), so an
        // implementation may name a method it holds no samples for. summarize() then indexed the empty
        // sample list with get(0) and get(size - 1) == get(-1), and every report writer died on a bare
        // IndexOutOfBoundsException.
        final Profiler.SingleLoopStatistics ghost = new Profiler.SingleLoopStatistics() {
            @Override
            public List<String> getMethodNameList() {
                return N.asList("ghost");
            }
        };

        final List<Profiler.LoopStatistics> loops = new ArrayList<>();
        loops.add(ghost);

        final Profiler.MultiLoopsStatistics stats = new Profiler.MultiLoopsStatistics(0, 0, 0, 0, 1, loops);

        assertEquals(0, stats.getMethodInvocationCount("ghost"));
        assertTrue(stats.getMethodStatisticsList("ghost").isEmpty());

        final StringWriter text = new StringWriter();
        stats.writeResult(text);
        assertTrue(text.toString().contains("ghost"), text.toString());

        final StringWriter html = new StringWriter();
        stats.writeHtmlResult(html);
        assertTrue(html.toString().contains("ghost"), html.toString());

        final StringWriter xml = new StringWriter();
        stats.writeXmlResult(xml);
        assertTrue(xml.toString().contains("ghost"), xml.toString());
    }

    @Test
    public void reviewFixes20260911_textReportDataRowsCarryATrailingSeparator() {
        // Doc pin for the "Example output format" block: writeResult appends the "," separator after EVERY
        // value including the last, so a data row carries one more comma than the header.
        final Profiler.MultiLoopsStatistics stats = Profiler.run(1, 3, 1, "rowShape", () -> {
        });

        final StringWriter out = new StringWriter();
        stats.writeResult(out);

        String header = null;
        String row = null;

        for (final String line : out.toString().lines().toList()) {
            if (line.startsWith("<method name>,")) {
                header = line.stripTrailing();
            } else if (line.startsWith("rowShape,")) {
                row = line.stripTrailing();
            }
        }

        assertNotNull(header, out.toString());
        assertNotNull(row, out.toString());
        assertTrue(header.endsWith("|99.99% >=|"), header);
        assertTrue(row.endsWith(","), row);
        assertEquals(header.split(",", -1).length + 1, row.split(",", -1).length, header + " || " + row);
    }
}

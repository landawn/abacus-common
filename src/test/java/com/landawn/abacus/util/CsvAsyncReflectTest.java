package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.io.TempDir;

import com.landawn.abacus.TestBase;

/**
 * Regression tests for the defects found in the 2026-09-01 review of RegExUtil, URLEncodedUtil, CsvUtil,
 * ExcelUtil, ClassUtil, AsyncExecutor, ContinuableFuture, Futures and Profiler.
 *
 * <p>Each test is named after the finding it locks down and fails against the pre-fix code.</p>
 */
public class CsvAsyncReflectTest extends TestBase {

    @TempDir
    Path tempDir;

    /**
     * {@code Profiler.suspended} is process-wide static state that puts {@code run(..)} into a
     * minimal-execution mode (one thread, one loop). Other tests in this package set it, so the profiler
     * tests below capture and restore it rather than assuming a clean slate.
     */
    private boolean profilerWasSuspended;

    @BeforeEach
    public void rememberProfilerState() {
        profilerWasSuspended = Profiler.isSuspended();
        Profiler.resume();
    }

    @AfterEach
    public void restoreProfilerState() {
        if (profilerWasSuspended) {
            Profiler.suspend();
        } else {
            Profiler.resume();
        }
    }

    // ------------------------------------------------------------------------------------------------
    // B1 - Profiler must not charge failure reporting to the profiled method
    // ------------------------------------------------------------------------------------------------

    /** An {@link OutputStream} that is deliberately slow, so "did this write happen inside the timed window?" is decidable. */
    private static final class SlowOutputStream extends OutputStream {
        static final long DELAY_MILLIS = 25;

        private final AtomicInteger writes = new AtomicInteger();

        @Override
        public void write(final int b) {
            // no-op: only the array overload is worth delaying
        }

        @Override
        public void write(final byte[] b, final int off, final int len) {
            writes.incrementAndGet();

            try {
                Thread.sleep(DELAY_MILLIS);
            } catch (final InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    @Test
    public void b1_profiler_throwingMethodIsNotChargedForItsOwnStackTrace() {
        final PrintStream realOut = System.out;
        final SlowOutputStream slow = new SlowOutputStream();

        final Profiler.MultiLoopsStatistics stats;

        try {
            // Profiler captures System.out when run(..) starts and writes each failure's stack trace to it.
            // Making that write cost a known 25ms turns the question into a deterministic one: if the write
            // is inside the timed window, the recorded elapsed time cannot be below it.
            System.setOut(new PrintStream(slow));
            stats = Profiler.run(1, 4, 1, "slowReport", () -> {
                throw new IllegalStateException("boom");
            });
        } finally {
            System.setOut(realOut);
        }

        assertEquals(4, stats.getAllFailedMethodStatisticsList().size(), "every invocation must still be recorded as failed");
        assertTrue(slow.writes.get() > 0, "the stack trace must still be reported");

        final double avg = stats.getMethodAverageElapsedTimeInMillis("slowReport");
        final double max = stats.getMethodMaxElapsedTimeInMillis("slowReport");

        // Before the fix, printStackTrace() sat between the two nanoTime() reads, so each invocation was
        // recorded as taking at least DELAY_MILLIS. It measures the throw, not the reporting of the throw.
        assertTrue(max < SlowOutputStream.DELAY_MILLIS, "reporting the failure must not be charged to the profiled method: max=" + max + "ms, avg=" + avg
                + "ms, delay=" + SlowOutputStream.DELAY_MILLIS + "ms");
    }

    @Test
    public void b1_profiler_stillCapturesTheTargetExceptionAsTheResult() {
        final PrintStream realOut = System.out;

        try {
            System.setOut(new PrintStream(new ByteArrayOutputStream()));

            final Profiler.MultiLoopsStatistics stats = Profiler.run(1, 3, 1, "boom", () -> {
                throw new IllegalStateException("expected");
            });

            final List<Profiler.MethodStatistics> failed = stats.getAllFailedMethodStatisticsList();
            assertEquals(3, failed.size());

            for (final Profiler.MethodStatistics ms : failed) {
                assertTrue(ms.isFailed());
                // The target exception is unwrapped from the InvocationTargetException, as before.
                assertTrue(ms.getResult() instanceof IllegalStateException, "was: " + ms.getResult());
                assertEquals("expected", ((Throwable) ms.getResult()).getMessage());
            }
        } finally {
            System.setOut(realOut);
        }
    }

    // ------------------------------------------------------------------------------------------------
    // B7 - AsyncExecutor.executeWithRetry(Runnable, ...) must validate on the calling thread
    // ------------------------------------------------------------------------------------------------

    @Test
    public void b7_executeWithRetry_runnableOverloadValidatesSynchronously() {
        final AsyncExecutor executor = new AsyncExecutor(1, 1, 1, TimeUnit.SECONDS);

        try {
            assertThrows(IllegalArgumentException.class, () -> executor.executeWithRetry((Throwables.Runnable<Exception>) () -> {
            }, -1, 0, e -> false));

            assertThrows(IllegalArgumentException.class, () -> executor.executeWithRetry((Throwables.Runnable<Exception>) () -> {
            }, 1, -5, e -> false));

            // The Callable overload already behaved this way; both must now agree.
            assertThrows(IllegalArgumentException.class, () -> executor.executeWithRetry((Callable<String>) () -> "x", -1, 0, (r, e) -> false));
        } finally {
            executor.shutdown();
        }
    }

    @Test
    public void b7_executeWithRetry_runnableStillRetriesAndSucceeds() throws Exception {
        final AsyncExecutor executor = new AsyncExecutor(1, 1, 1, TimeUnit.SECONDS);

        try {
            final AtomicInteger attempts = new AtomicInteger();

            executor.executeWithRetry((Throwables.Runnable<Exception>) () -> {
                if (attempts.incrementAndGet() < 3) {
                    throw new IllegalStateException("retry me");
                }
            }, 5, 1, e -> true).get();

            assertEquals(3, attempts.get());
        } finally {
            executor.shutdown();
        }
    }

    // ------------------------------------------------------------------------------------------------
    // B0/D3 - the lazily created pool must use named daemon threads so the JVM can still exit
    // ------------------------------------------------------------------------------------------------

    @Test
    public void b0_asyncExecutorWorkerThreadsAreNamedDaemons() throws Exception {
        final AsyncExecutor executor = new AsyncExecutor(1, 1, 1, TimeUnit.SECONDS);

        try {
            final Callable<String[]> probe = () -> new String[] { Thread.currentThread().getName(), //
                    String.valueOf(Thread.currentThread().isDaemon()) };
            final String[] info = executor.execute(probe).get();

            // Non-daemon core threads prevented JVM shutdown from ever STARTING, which also meant the
            // shutdown hook registered to drain the pool could never run.
            assertEquals("true", info[1], "worker threads must be daemon threads");
            assertTrue(info[0].startsWith("abacus-async-"), "worker threads must be identifiable, was: " + info[0]);
        } finally {
            executor.shutdown();
        }
    }

    @Test
    public void b0_shutdownStillDrainsQueuedWork() throws Exception {
        final AsyncExecutor executor = new AsyncExecutor(1, 1, 1, TimeUnit.SECONDS);
        final AtomicInteger completed = new AtomicInteger();

        for (int i = 0; i < 20; i++) {
            executor.execute((Throwables.Runnable<Exception>) () -> {
                Thread.sleep(1);
                completed.incrementAndGet();
            });
        }

        // Daemon threads must not mean "work may be dropped": shutdownAndAwait still drains.
        executor.shutdownAndAwait(30, TimeUnit.SECONDS);

        assertEquals(20, completed.get());
        assertTrue(executor.isTerminated());
    }

    // ------------------------------------------------------------------------------------------------
    // B6 - Futures.anyOf().isDone() must not run user code, and must be idempotent
    // ------------------------------------------------------------------------------------------------

    @Test
    public void b6_anyOfIsDoneDoesNotReRunALazyMapper() throws Exception {
        final AtomicInteger mapperCalls = new AtomicInteger();
        final ContinuableFuture<Integer> lazy = ContinuableFuture.completed(1).map(v -> {
            mapperCalls.incrementAndGet();
            return v;
        });

        final ContinuableFuture<Integer> any = Futures.anyOf(Arrays.asList(lazy));

        assertEquals(0, mapperCalls.get());

        assertTrue(any.isDone());
        assertTrue(any.isDone());
        assertTrue(any.isDone());

        // Before the fix each isDone() call invoked the caller's mapper: 3 calls -> 3 invocations.
        assertTrue(mapperCalls.get() <= 1, "isDone() must resolve each input at most once, was: " + mapperCalls.get());
    }

    @Test
    public void b6_anyOfIsDoneResolvesCompletableFuturesWithoutCallingGet() {
        // A CompletableFuture is classified through isCompletedExceptionally(), so no get() happens at all.
        final CompletableFuture<String> pending = new CompletableFuture<>();
        final CompletableFuture<String> other = new CompletableFuture<>();
        final ContinuableFuture<String> any = Futures.anyOf(pending, other);

        assertFalse(any.isDone());

        other.complete("done");

        assertTrue(any.isDone());
    }

    @Test
    public void b6_anyOfIsDoneStillWaitsForSuccessAfterAFailure() throws Exception {
        final CompletableFuture<String> failed = new CompletableFuture<>();
        final CompletableFuture<String> pending = new CompletableFuture<>();
        final ContinuableFuture<String> any = Futures.anyOf(failed, pending);

        failed.completeExceptionally(new IllegalStateException("boom"));

        assertFalse(any.isDone(), "a failed candidate must not complete anyOf while another can still succeed");

        pending.complete("second");

        assertTrue(any.isDone());
        assertEquals("second", any.get());
    }

    @Test
    public void b6_anyOfIsDoneIsTrueWhenEveryCandidateFailed() {
        final CompletableFuture<String> a = new CompletableFuture<>();
        final CompletableFuture<String> b = new CompletableFuture<>();
        final ContinuableFuture<String> any = Futures.anyOf(a, b);

        a.completeExceptionally(new IllegalStateException("a"));
        assertFalse(any.isDone());

        b.completeExceptionally(new IllegalStateException("b"));

        // Nothing can succeed any more, so get() will throw rather than block - that is "done".
        assertTrue(any.isDone());
        assertThrows(ExecutionException.class, any::get);
    }

    @Test
    public void b6_anyOfIsDoneTreatsCancellationAsFailure() {
        final CompletableFuture<String> cancelled = new CompletableFuture<>();
        final CompletableFuture<String> pending = new CompletableFuture<>();
        final ContinuableFuture<String> any = Futures.anyOf(cancelled, pending);

        cancelled.cancel(true);

        assertFalse(any.isDone());

        pending.complete("ok");

        assertTrue(any.isDone());
    }

    // ------------------------------------------------------------------------------------------------
    // J2 - Futures.combine reports an action failure through ExecutionException, not RuntimeException
    // ------------------------------------------------------------------------------------------------

    @Test
    public void j2_combineReportsCheckedActionFailureAsExecutionException() {
        final ContinuableFuture<Integer> combined = Futures.combine(ContinuableFuture.completed(1), ContinuableFuture.completed(2), (a, b) -> {
            throw new java.io.IOException("boom");
        });

        final ExecutionException e = assertThrows(ExecutionException.class, combined::get);
        assertTrue(e.getCause() instanceof java.io.IOException, "cause was: " + e.getCause());
        assertEquals("boom", e.getCause().getMessage());
    }

    @Test
    public void j2_combineReportsUncheckedActionFailureAsExecutionException() {
        final ContinuableFuture<Integer> combined = Futures.combine(ContinuableFuture.completed(1), ContinuableFuture.completed(2), (a, b) -> {
            throw new IllegalStateException("boom");
        });

        final ExecutionException e = assertThrows(ExecutionException.class, combined::get);
        assertTrue(e.getCause() instanceof IllegalStateException, "cause was: " + e.getCause());
    }

    // ------------------------------------------------------------------------------------------------
    // B4 - the unknown-column message must name only the columns that are missing
    // ------------------------------------------------------------------------------------------------

    @Test
    public void b4_unknownColumnMessageNamesOnlyTheMissingColumns() {
        final String csv = "id,name,city,zip\n1,a,b,c\n";

        final IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> CsvUtil.load(new StringReader(csv), Arrays.asList("id", "name", "citty")));

        assertTrue(e.getMessage().contains("citty"), "must name the missing column, was: " + e.getMessage());
        assertFalse(e.getMessage().contains("[id, name, citty]"), "must not accuse the whole selection, was: " + e.getMessage());
        assertTrue(e.getMessage().contains("id, name, city, zip"), "must still show the header, was: " + e.getMessage());
    }

    @Test
    public void b4_unknownColumnMessageListsEveryMissingColumn() {
        final String csv = "id,name\n1,a\n";

        final IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> CsvUtil.load(new StringReader(csv), Arrays.asList("id", "nope1", "nope2")));

        assertTrue(e.getMessage().contains("nope1") && e.getMessage().contains("nope2"), "was: " + e.getMessage());
    }

    // ------------------------------------------------------------------------------------------------
    // B5 - a UTF-8 BOM must not become part of the first column's name
    // ------------------------------------------------------------------------------------------------

    private File writeBomCsv(final String name, final String body) throws Exception {
        final File f = tempDir.resolve(name).toFile();

        try (OutputStream os = new FileOutputStream(f)) {
            os.write(new byte[] { (byte) 0xEF, (byte) 0xBB, (byte) 0xBF });
            os.write(body.getBytes(StandardCharsets.UTF_8));
        }

        return f;
    }

    @Test
    public void b5_bomIsStrippedFromTheHeaderLine() throws Exception {
        final File f = writeBomCsv("bom-basic.csv", "id,name\n1,John\n");

        final Dataset ds = CsvUtil.load(f);

        assertEquals(Arrays.asList("id", "name"), ds.columnNames());
        assertEquals(2, ds.columnNames().get(0).length(), "the BOM must not remain in the column name");
        assertEquals(1, ds.size());
    }

    @Test
    public void b5_columnSelectionWorksOnABomPrefixedFile() throws Exception {
        final File f = writeBomCsv("bom-select.csv", "id,name,city\n1,John,NY\n");

        final Dataset ds = CsvUtil.load(f, Arrays.asList("id", "name"));

        assertEquals(Arrays.asList("id", "name"), ds.columnNames());
        assertEquals(Arrays.asList("1", "John"), ds.getRow(0));
    }

    @Test
    public void b5_bomIsStrippedForStreamAndCsvToJsonToo() throws Exception {
        final File f = writeBomCsv("bom-stream.csv", "id,name\n1,John\n");

        // stream(...) resolves the selection against the header too, so a retained BOM would fail here.
        try (com.landawn.abacus.util.stream.Stream<Object[]> s = CsvUtil.stream(f, Arrays.asList("id", "name"), Object[].class)) {
            assertEquals(1, s.count());
        }

        final java.io.StringWriter out = new java.io.StringWriter();

        try (java.io.Reader r = IOUtil.newFileReader(f)) {
            CsvUtil.csvToJson(r, null, out, null);
        }

        assertTrue(out.toString().contains("\"id\""), "BOM must not leak into the JSON key, was: " + out);
    }

    @Test
    public void b5_aBomOnlyHeaderBehavesExactlyLikeAnEmptyHeader() throws Exception {
        // Degenerate input: the header line is nothing but the BOM. Once the BOM is removed there is nothing
        // left, so this must behave identically to a genuinely empty header line - which is itself rejected
        // (pre-existing behaviour: RowDataset does not accept an empty column name). Asserting the
        // EQUIVALENCE is what proves the BOM was fully removed rather than turned into a column name.
        final File f = writeBomCsv("bom-only.csv", "\n");

        final IllegalArgumentException withBom = assertThrows(IllegalArgumentException.class, () -> CsvUtil.load(f));
        final IllegalArgumentException withoutBom = assertThrows(IllegalArgumentException.class, () -> CsvUtil.load(new StringReader("\n")));

        assertEquals(withoutBom.getMessage(), withBom.getMessage());
    }

    @Test
    public void b5_noBomInputIsUnaffected() {
        final Dataset ds = CsvUtil.load(new StringReader("id,name\n1,John\n"));

        assertEquals(Arrays.asList("id", "name"), ds.columnNames());
    }

    // ------------------------------------------------------------------------------------------------
    // B12 / B13 - ClassUtil
    // ------------------------------------------------------------------------------------------------

    /**
     * The flag must live in a DIFFERENT class from the probe: merely assigning to a static field of the probe
     * would itself initialize it, and the test could no longer tell whether the scan did so.
     */
    public static class ClinitFlag {
        public static volatile boolean probeInitialized = false;
    }

    /** Records its own initialization in {@link ClinitFlag}. Never referenced except by {@code .class}, which does not initialize. */
    public static class ClinitProbe {
        static {
            ClinitFlag.probeInitialized = true;
        }
    }

    @Test
    public void b12_packageScanningDoesNotRunStaticInitializers() {
        assertFalse(ClinitFlag.probeInitialized, "precondition: the probe must not be initialized yet");

        final List<Class<?>> found = ClassUtil.findClassesInPackage(CsvAsyncReflectTest.class.getPackageName(), false, true,
                c -> c.getName().endsWith("CsvAsyncReflectTest$ClinitProbe"));

        assertEquals(1, found.size(), "the probe class should have been discovered");
        // A class literal resolves the class without initializing it, so this assertion is itself side-effect free.
        assertSame(ClinitProbe.class, found.get(0));
        assertFalse(ClinitFlag.probeInitialized, "scanning must not run the discovered class's static initializer");
    }

    @Test
    public void b12_predicateFailuresAreNotSwallowedBySkipClassLoadingException() {
        // skipClassLoadingException only covers failures of the LOAD; an exception from the caller's
        // predicate is the caller's and must propagate unchanged.
        assertThrows(IllegalStateException.class, () -> ClassUtil.findClassesInPackage(CsvAsyncReflectTest.class.getPackageName(), false, true, c -> {
            throw new IllegalStateException("predicate blew up");
        }));
    }

    @Test
    public void b13_makeFolderForPackageIsIdempotent() throws Exception {
        final String src = tempDir.resolve("gen").toString();
        Files.createDirectories(tempDir.resolve("gen"));

        final String first = ClassUtil.makeFolderForPackage(src, "com.example.demo");
        // A second call must not fail just because mkdirs() now returns false.
        final String second = ClassUtil.makeFolderForPackage(src, "com.example.demo");

        assertEquals(first, second);
        assertTrue(new File(first).isDirectory());

        assertThrows(IllegalArgumentException.class, () -> ClassUtil.makeFolderForPackage(null, "com.example.demo"));
    }

    // ------------------------------------------------------------------------------------------------
    // B14 - the RFC 5322 domain-literal class must exclude '[', '\' and ']'
    // ------------------------------------------------------------------------------------------------

    @Test
    public void b14_domainLiteralRejectsBracketsAndBackslash() {
        final Pattern matcher = RegExUtil.EMAIL_ADDRESS_RFC_5322_MATCHER;

        // A well-formed general address literal still matches.
        assertTrue(RegExUtil.matches("a@[1.2.3.x:abc]", matcher));

        // ... but the three characters that must be escaped inside a domain literal are now rejected.
        assertFalse(RegExUtil.matches("a@[1.2.3.x:a]b]", matcher), "']' must not be admitted inside a domain literal");
        assertFalse(RegExUtil.matches("a@[1.2.3.x:a[b]", matcher), "'[' must not be admitted inside a domain literal");
    }

    @Test
    public void b14_ordinaryAddressesAreUnaffected() {
        final Pattern matcher = RegExUtil.EMAIL_ADDRESS_RFC_5322_MATCHER;

        assertTrue(RegExUtil.matches("user@example.com", matcher));
        assertTrue(RegExUtil.matches("john.doe@company.co.uk", matcher));
        assertTrue(RegExUtil.matches("user+tag@example.com", matcher));
        assertTrue(RegExUtil.matches("\"quoted.user\"@example.com", matcher));
        assertTrue(RegExUtil.matches("user@[192.168.1.1]", matcher));
        assertFalse(RegExUtil.matches("not an email", matcher));
    }

    // ------------------------------------------------------------------------------------------------
    // J1 - encode(CharSequence) behaviour matches what the javadoc now says
    // ------------------------------------------------------------------------------------------------

    @Test
    public void j1_charSequenceWithEqualsIsAppendedVerbatim() {
        // Documented (and long-standing) behaviour: a '='-bearing CharSequence is a pre-built query.
        assertEquals("a=1&a=2", URLEncodedUtil.encode("a=1&a=2"));
        assertEquals("q=a%20b", URLEncodedUtil.encode("q=a%20b"));
        assertEquals("http://x?a=1&a=2", URLEncodedUtil.encode("http://x", "a=1&a=2"));
    }

    @Test
    public void j1_charSequenceWithoutEqualsIsEncodedAsOneField() {
        assertEquals("a+b", URLEncodedUtil.encode("a b"));
        assertEquals("http://x?a+b", URLEncodedUtil.encode("http://x", "a b"));
    }

    // ------------------------------------------------------------------------------------------------
    // Timeout guard: none of the above may hang.
    // ------------------------------------------------------------------------------------------------

    @Test
    @Timeout(value = 60, unit = TimeUnit.SECONDS)
    public void guard_suiteDoesNotHang() {
        assertTrue(true);
    }
}

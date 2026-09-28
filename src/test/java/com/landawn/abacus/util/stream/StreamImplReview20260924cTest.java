package com.landawn.abacus.util.stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

import javax.sql.DataSource;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.IOUtil;

/**
 * Cycle-3 regression tests (2026-09-24) for the object-stream implementation classes. Ledger:
 * {@code scripts/cross_review/StreamFamily_Fn_MoreCollectors_Seq_Fnn_Throwables_ledger_2026-09-24.md}, table
 * "Cycle 3 findings" (C-118, C-121/C-123, nit G3-06). The parallel min/max part of the failure-precedence rule (C-119)
 * is in {@link CloseFailurePrecedenceReview20260924bTest}.
 */
public class StreamImplReview20260924cTest extends TestBase {

    private static final class Boom extends RuntimeException {
        private static final long serialVersionUID = 1L;

        Boom(final String message) {
            super(message);
        }
    }

    @FunctionalInterface
    private interface Terminal {
        Object apply(Stream<Integer> s) throws Exception;
    }

    private File dir;

    @BeforeEach
    public void createDir() throws IOException {
        dir = Files.createTempDirectory("persist-c118").toFile();
    }

    @AfterEach
    public void deleteDir() {
        IOUtil.deleteRecursivelyIfExists(dir);
    }

    private static List<Function<List<Integer>, Stream<Integer>>> sourceKinds() {
        return Arrays.asList(list -> Stream.of(list.toArray(new Integer[0])), list -> Stream.of(list.iterator()),
                list -> Stream.of(list.toArray(new Integer[0])).parallel(3), list -> Stream.of(list.iterator()).parallel(3));
    }

    /**
     * Runs {@code terminal} on a stream of every source kind whose close handler fails, and checks that the terminal's
     * own failure (of type {@code expected}) propagates with the close failure suppressed, the handler having run once.
     */
    private static <X extends Throwable> void assertOpenFailureWins(final Class<X> expected, final Terminal terminal, final String label) {
        int kind = 0;

        for (final Function<List<Integer>, Stream<Integer>> source : sourceKinds()) {
            final Boom closeFailure = new Boom("closeX");
            final AtomicInteger closes = new AtomicInteger();
            final Stream<Integer> s = source.apply(Arrays.asList(1, 2, 3)).onClose(() -> {
                closes.incrementAndGet();
                throw closeFailure;
            });

            final String msg = label + " / source kind " + kind++;
            final X thrown = assertThrows(expected, () -> terminal.apply(s), msg);
            assertEquals(1, thrown.getSuppressed().length, msg);
            assertSame(closeFailure, thrown.getSuppressed()[0], msg);
            assertEquals(1, closes.get(), msg);
        }
    }

    // ---------------------------------------------------------------------------------------------------------
    // C-118 persist*(File) / persist(Connection|DataSource): a failure to open the target wins over a failing close
    // handler (the C-114 catch had landed on the inner cleanup try).
    // ---------------------------------------------------------------------------------------------------------

    @Test
    public void testC118_persistToDirectoryKeepsTheOpenFailure() {
        // Every File overload; the output is a directory, so opening the writer fails before the Writer overload runs.
        assertOpenFailureWins(IllegalArgumentException.class, s -> s.persist(dir), "persist(File)");
        assertOpenFailureWins(IllegalArgumentException.class, s -> s.persist("h", "t", dir), "persist(h, t, File)");
        assertOpenFailureWins(IllegalArgumentException.class, s -> s.persist(String::valueOf, dir), "persist(toLine, File)");
        assertOpenFailureWins(IllegalArgumentException.class, s -> s.persist("h", "t", String::valueOf, dir), "persist(h, t, toLine, File)");
        assertOpenFailureWins(IllegalArgumentException.class, s -> s.persist((x, w) -> w.write(String.valueOf(x)), dir), "persist(write, File)");
        assertOpenFailureWins(IllegalArgumentException.class, s -> s.persist("h", "t", (x, w) -> w.write(String.valueOf(x)), dir),
                "persist(h, t, write, File)");
        assertOpenFailureWins(IllegalArgumentException.class, s -> s.persistToCsv(dir), "persistToCsv(File)");
        assertOpenFailureWins(IllegalArgumentException.class, s -> s.persistToCsv(Arrays.asList("a"), dir), "persistToCsv(headers, File)");
        assertOpenFailureWins(IllegalArgumentException.class, s -> s.persistToJson(dir), "persistToJson(File)");
    }

    @Test
    public void testC118_persistToFileOperationFailureKeepsThePrimaryFailure() throws IOException {
        // The writer opens; the operation itself fails. The written prefix is flushed and the file writer closed.
        final File out = new File(dir, "out.txt");
        assertOpenFailureWins(Boom.class, s -> s.persist(x -> {
            if (x == 2) {
                throw new Boom("line boom");
            }
            return "v" + x;
        }, out), "persist(toLine, File)");
        assertEquals("v1", Files.readAllLines(out.toPath(), StandardCharsets.UTF_8).get(0));
        assertTrue(out.delete(), "the writer must be closed so that the file can be deleted");

        assertOpenFailureWins(Boom.class, s -> s.persist((x, w) -> {
            if (x == 2) {
                throw new Boom("write boom");
            }
            w.write("w" + x);
        }, out), "persist(write, File)");
        assertEquals("w1", Files.readAllLines(out.toPath(), StandardCharsets.UTF_8).get(0));
        assertTrue(out.delete());
    }

    @Test
    public void testC118_persistToFileSuccessUnchanged() throws IOException {
        final File out = new File(dir, "ok.txt");
        final AtomicInteger closes = new AtomicInteger();
        assertEquals(3, Stream.of(1, 2, 3).onClose(closes::incrementAndGet).persist("h", "t", (x, w) -> w.write("#" + x), out));
        assertEquals(Arrays.asList("h", "#1", "#2", "#3", "t"), Files.readAllLines(out.toPath(), StandardCharsets.UTF_8));
        assertEquals(1, closes.get());

        assertEquals(1, Stream.of(Arrays.asList("x").iterator()).onClose(closes::incrementAndGet).persistToJson(out));
        assertEquals(2, closes.get());

        // A close handler failing after a successful persist is still reported.
        final Boom e = assertThrows(Boom.class, () -> Stream.of(1).onClose(() -> {
            throw new Boom("closeX");
        }).persist(out));
        assertEquals("closeX", e.getMessage());
    }

    /** A Connection whose prepareStatement fails; close() is counted. */
    private static Connection connectionFailingOnPrepare(final AtomicInteger connCloses) {
        return (Connection) Proxy.newProxyInstance(StreamImplReview20260924cTest.class.getClassLoader(), new Class<?>[] { Connection.class },
                (proxy, method, args) -> {
                    switch (method.getName()) {
                        case "prepareStatement":
                            throw new SQLException("prepareFailed");
                        case "close":
                            connCloses.incrementAndGet();
                            return null;
                        case "isClosed":
                        case "getAutoCommit":
                            return false;
                        case "hashCode":
                            return System.identityHashCode(proxy);
                        case "equals":
                            return proxy == args[0];
                        case "toString":
                            return "failing connection";
                        default:
                            throw new UnsupportedOperationException(method.getName());
                    }
                });
    }

    /** A DataSource returning {@code conn}, or failing with a new SQLException("noConnection") when {@code conn} is null. */
    private static DataSource dataSource(final Connection conn) {
        return (DataSource) Proxy.newProxyInstance(StreamImplReview20260924cTest.class.getClassLoader(), new Class<?>[] { DataSource.class },
                (proxy, method, args) -> {
                    switch (method.getName()) {
                        case "getConnection":
                            if (conn == null) {
                                throw new SQLException("noConnection");
                            }
                            return conn;
                        case "hashCode":
                            return System.identityHashCode(proxy);
                        case "equals":
                            return proxy == args[0];
                        case "toString":
                            return "test data source";
                        default:
                            throw new UnsupportedOperationException(method.getName());
                    }
                });
    }

    @Test
    public void testC118_persistToConnectionKeepsThePrepareFailure() {
        final AtomicInteger connCloses = new AtomicInteger();
        final Connection conn = connectionFailingOnPrepare(connCloses);
        final SQLException[] seen = new SQLException[1];

        assertOpenFailureWins(SQLException.class, s -> {
            try {
                return s.persist(conn, "insert into t values (?)", 10, 0, (x, stmt) -> stmt.setInt(1, x));
            } catch (final SQLException e) {
                seen[0] = e;
                throw e;
            }
        }, "persist(Connection)");

        assertEquals("prepareFailed", seen[0].getMessage());
        assertEquals(0, connCloses.get(), "the caller's connection must not be closed");
    }

    @Test
    public void testC118_persistToDataSourceKeepsTheConnectFailure() {
        final SQLException[] seen = new SQLException[1];

        assertOpenFailureWins(SQLException.class, s -> {
            try {
                return s.persist(dataSource(null), "insert into t values (?)", 10, 0, (x, stmt) -> stmt.setInt(1, x));
            } catch (final SQLException e) {
                seen[0] = e;
                throw e;
            }
        }, "persist(DataSource) getConnection failure");
        assertEquals("noConnection", seen[0].getMessage());

        final AtomicInteger connCloses = new AtomicInteger();
        assertOpenFailureWins(SQLException.class, s -> {
            try {
                return s.persist(dataSource(connectionFailingOnPrepare(connCloses)), "insert into t values (?)", 10, 0,
                        (x, stmt) -> stmt.setInt(1, x));
            } catch (final SQLException e) {
                seen[0] = e;
                throw e;
            }
        }, "persist(DataSource) prepareStatement failure");
        assertEquals("prepareFailed", seen[0].getMessage());
        assertEquals(4, connCloses.get(), "the connection obtained from the DataSource is released every time (4 source kinds)");
    }

    @Test
    public void testC118_closeAfterFailureWithResourceSuppressesBothCloseFailures() throws Exception {
        final Boom primary = new Boom("primary");
        final IOException resourceFailure = new IOException("writer close failed");
        final Boom handlerFailure = new Boom("closeX");
        final AtomicInteger resourceCloses = new AtomicInteger();
        final AtomicInteger handlerRuns = new AtomicInteger();

        final Stream<Integer> s = Stream.of(1, 2).onClose(() -> {
            handlerRuns.incrementAndGet();
            throw handlerFailure;
        });

        s.closeAfterFailure(() -> {
            resourceCloses.incrementAndGet();
            throw resourceFailure;
        }, primary);

        assertEquals(1, resourceCloses.get());
        assertEquals(1, handlerRuns.get());
        assertTrue(s.isClosed());
        assertEquals(Arrays.asList(resourceFailure, handlerFailure), Arrays.asList(primary.getSuppressed()));

        // A null resource (failed to open) is skipped; the primary failure is never suppressed onto itself.
        final Boom primary2 = new Boom("primary2");
        final Stream<Integer> s2 = Stream.of(1).onClose(() -> {
            throw primary2;
        });
        s2.closeAfterFailure(null, primary2);
        assertEquals(0, primary2.getSuppressed().length);
        assertTrue(s2.isClosed());
    }

    @Test
    public void testC118_closeAfterFailureWithResourceRestoresTheInterruptStatus() {
        final Boom primary = new Boom("primary");
        final Stream<Integer> s = Stream.of(1);

        try {
            s.closeAfterFailure(() -> {
                throw new InterruptedException("interrupted close");
            }, primary);

            assertTrue(Thread.currentThread().isInterrupted());
            assertEquals(1, primary.getSuppressed().length);
            assertTrue(primary.getSuppressed()[0] instanceof InterruptedException);
        } finally {
            Thread.interrupted(); // clear for the following tests
        }
    }

    // ---------------------------------------------------------------------------------------------------------
    // C-121 / C-123 IteratorStream split/sliding (default list factory): a SHORT chunk or window (the last one, or a
    // source shorter than the requested size) is trimmed; a full chunk keeps its exact pre-size.
    // ---------------------------------------------------------------------------------------------------------

    /** Capacity of an ArrayList, or -1 when java.util is not opened to this module (the Maven suite). */
    private static int capacity(final Object list) {
        try {
            final Field f = ArrayList.class.getDeclaredField("elementData");
            f.setAccessible(true);
            return ((Object[]) f.get(list)).length;
        } catch (final Exception | Error e) { // InaccessibleObjectException without --add-opens java.base/java.util
            return -1;
        }
    }

    private static List<Integer> range(final int from, final int to) {
        final List<Integer> list = new ArrayList<>(to - from);

        for (int i = from; i < to; i++) {
            list.add(i);
        }

        return list;
    }

    private static void assumeCapacityObservable() {
        Assumptions.assumeTrue(capacity(new ArrayList<>()) >= 0, "ArrayList capacity is not observable without --add-opens java.base/java.util");
    }

    @Test
    public void testC121_shortChunksCarryNoSpareCapacity() {
        // Content checks run everywhere.
        assertEquals(Arrays.asList(Arrays.asList(1, 2, 3)), Stream.of(Arrays.asList(1, 2, 3).iterator()).split(60_000).toList());
        final List<List<Integer>> ragged = Stream.of(range(0, 100_003).iterator()).split(50_000).toList();
        assertEquals(3, ragged.size());
        assertEquals(range(100_000, 100_003), ragged.get(2));

        assumeCapacityObservable();

        // c2final: 60,000 / 65,536 / 10,000 / 1 (capacity = the requested chunk size)
        assertEquals(3, capacity(Stream.of(Arrays.asList(1, 2, 3).iterator()).split(60_000).first().orElseThrow()));
        assertEquals(3, capacity(Stream.of(Arrays.asList(1, 2, 3).iterator()).split(65_536).first().orElseThrow()));
        assertEquals(3, capacity(Stream.of(Arrays.asList(1, 2, 3).iterator()).split(10_000).first().orElseThrow()));
        assertEquals(1, capacity(Stream.of(Arrays.asList("x").iterator()).split(2).first().orElseThrow()));

        // Full chunks keep the exact pre-size (no copy); only the final short chunk is trimmed (c2final: 50,000).
        assertEquals(50_000, capacity(ragged.get(0)));
        assertEquals(50_000, capacity(ragged.get(1)));
        assertEquals(3, capacity(ragged.get(2)));

        // Above the threshold (unchanged): trimmed.
        assertEquals(3, capacity(Stream.of(Arrays.asList(1, 2, 3).iterator()).split(100_000).first().orElseThrow()));

        // The IntFunctions.ofList() overload is the same default factory.
        assertEquals(2, capacity(Stream.of(Arrays.asList(1, 2).iterator()).split(1000, com.landawn.abacus.util.IntFunctions.ofList()).first().orElseThrow()));

        // Parallel iterator source: the chunking is sequential, same rule.
        assertEquals(3, capacity(Stream.of(Arrays.asList(1, 2, 3).iterator()).parallel(2).split(60_000).first().orElseThrow()));
    }

    @Test
    public void testC121_shortWindowsCarryNoSpareCapacity() {
        final List<List<Integer>> windows = Stream.of(range(0, 10).iterator()).sliding(4, 3).toList();
        assertEquals(Arrays.asList(range(0, 4), range(3, 7), range(6, 10)), windows);
        final List<List<Integer>> stepped = Stream.of(range(0, 10).iterator()).sliding(4, 4).toList();
        assertEquals(Arrays.asList(range(0, 4), range(4, 8), range(8, 10)), stepped);

        assumeCapacityObservable();

        // c2final: 60,000 / 65,536 (capacity = the requested window size)
        assertEquals(3, capacity(Stream.of(Arrays.asList(1, 2, 3).iterator()).sliding(60_000).first().orElseThrow()));
        assertEquals(3, capacity(Stream.of(Arrays.asList(1, 2, 3).iterator()).sliding(100_000, 100_000).first().orElseThrow()));

        // Full windows exact (also the ones that start with the overlap), the short last window trimmed (c2final: 4).
        assertEquals(4, capacity(windows.get(0)));
        assertEquals(4, capacity(windows.get(2)));
        assertEquals(4, capacity(stepped.get(1)));
        assertEquals(2, capacity(stepped.get(2)));

        // Overlapping windows over a short source: every emitted window is exactly sized.
        for (final List<Integer> w : Stream.of(range(0, 5).iterator()).sliding(1000, 2).toList()) {
            assertEquals(w.size(), capacity(w));
        }
    }

    @Test
    public void testC121_emittedChunksAreIndependentAndNotReused() {
        final List<List<Integer>> chunks = Stream.of(range(0, 7).iterator()).split(3).toList();
        assertEquals(Arrays.asList(range(0, 3), range(3, 6), range(6, 7)), chunks);
        assertNotSame(chunks.get(0), chunks.get(1));

        // A trimmed chunk is still a mutable ArrayList owned by the caller.
        chunks.get(2).add(99);
        assertEquals(Arrays.asList(6, 99), chunks.get(2));
        assertTrue(chunks.get(2) instanceof ArrayList);
    }

    // ---------------------------------------------------------------------------------------------------------
    // G3-06 collectingCombiner message: anonymous/local container classes have no canonical name.
    // ---------------------------------------------------------------------------------------------------------

    @Test
    public void testG3_06_combinerMessageNamesAnonymousContainers() {
        @SuppressWarnings("unchecked")
        final java.util.function.BiConsumer<Object, Object> combiner = StreamBase.collectingCombiner;
        final Object anon = new Object() {
        };

        final IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> combiner.accept(anon, anon));
        assertFalse(e.getMessage().startsWith("null"), e.getMessage()); // c2final: "null cannot be combined by default..."
        assertTrue(e.getMessage().startsWith(anon.getClass().getName() + " cannot be combined"), e.getMessage());

        // Canonical names are still used where they exist.
        final IllegalArgumentException e2 = assertThrows(IllegalArgumentException.class,
                () -> combiner.accept(new AtomicInteger(), new AtomicInteger()));
        assertTrue(e2.getMessage().startsWith("java.util.concurrent.atomic.AtomicInteger cannot be combined"), e2.getMessage());

        // Through a parallel collect with an ad-hoc container.
        final IllegalArgumentException e3 = assertThrows(IllegalArgumentException.class, () -> Stream.of(range(0, 1000).toArray(new Integer[0]))
                .parallel(4)
                .collect(() -> new Object() {
                    int sum;
                }, (a, x) -> a.sum += x));
        assertFalse(e3.getMessage().startsWith("null"), e3.getMessage());
        assertTrue(e3.getMessage().contains("$"), e3.getMessage());
    }

    // ---------------------------------------------------------------------------------------------------------
    // C-133 parallel skip(n, action): the skip stage runs sequentially (like rateLimited/delay) and no longer
    // reorders the elements it passes on; the parallel settings are kept for the stages after it.
    // ---------------------------------------------------------------------------------------------------------

    @Test
    public void testC133_parallelSkipWithActionKeepsItsInputOrder() {
        final List<Integer> input = range(0, 20_000);
        int outOfOrder = 0;

        for (int round = 0; round < 50; round++) {
            final List<Integer> skipped = java.util.Collections.synchronizedList(new ArrayList<>());
            final List<Integer> rest = (round % 2 == 0 ? Stream.of(input.toArray(new Integer[0])) : Stream.of(input.iterator())).parallel(4)
                    .skip(3, skipped::add)
                    .toList();

            assertEquals(Arrays.asList(0, 1, 2), skipped, "round " + round);
            assertEquals(input.size() - 3, rest.size(), "round " + round);

            if (!rest.equals(input.subList(3, input.size()))) {
                outOfOrder++;
            }
        }

        assertEquals(0, outOfOrder, "rounds out of order"); // c2final: 50/50 (parallel dropWhile emitted in completion order)
    }

    @Test
    public void testC133_parallelEntryStreamSkipWithActionKeepsItsInputOrder() {
        final java.util.LinkedHashMap<Integer, Integer> map = new java.util.LinkedHashMap<>();
        for (int i = 0; i < 20_000; i++) {
            map.put(i, -i);
        }

        for (int round = 0; round < 20; round++) {
            final List<Integer> skipped = java.util.Collections.synchronizedList(new ArrayList<>());
            final List<Integer> keys = EntryStream.of(map).parallel(4).skip(3, e -> skipped.add(e.getKey())).keys().toList();
            assertEquals(Arrays.asList(0, 1, 2), skipped);
            assertEquals(range(3, 20_000), keys, "round " + round); // c2final: out of order
        }
    }

    @Test
    public void testC133_parallelSkipWithActionSeesExactlyTheSkippedElements() {
        final AtomicInteger calls = new AtomicInteger();
        final List<Integer> skipped = java.util.Collections.synchronizedList(new ArrayList<>());

        // n larger than the stream: every element is skipped, the result is empty.
        assertEquals(0, Stream.of(1, 2, 3).parallel(2).skip(10, x -> {
            calls.incrementAndGet();
            skipped.add(x);
        }).count());
        assertEquals(3, calls.get());
        assertEquals(Arrays.asList(1, 2, 3), skipped);

        // n == 0: the action is never called.
        calls.set(0);
        assertEquals(Arrays.asList(1, 2, 3), Stream.of(1, 2, 3).parallel(2).skip(0, x -> calls.incrementAndGet()).toList());
        assertEquals(0, calls.get());

        // A parallel stage after the skip still runs in parallel (order then unspecified) and sees every kept element.
        skipped.clear();
        final List<Integer> mapped = Stream.of(range(0, 1000).iterator()).parallel(4).skip(5, skipped::add).map(x -> x * 2).sortedBy(x -> x).toList();
        assertEquals(Arrays.asList(0, 1, 2, 3, 4), skipped);
        assertEquals(995, mapped.size());
        assertEquals(10, mapped.get(0));

        // Invalid arguments still fail and close the stream.
        final AtomicInteger closed = new AtomicInteger();
        assertThrows(IllegalArgumentException.class, () -> Stream.of(1).parallel(2).onClose(closed::incrementAndGet).skip(-1, x -> {
        }));
        assertEquals(1, closed.get());
    }

    @Test
    public void testC133_parallelSkipWithActionKeepsTheParallelSettingsAndCloseHandlers() {
        final AtomicInteger closed = new AtomicInteger();
        final Stream<Integer> source = Stream.of(range(0, 100).toArray(new Integer[0])).parallel(3).onClose(closed::incrementAndGet);
        final Stream<Integer> result = source.skip(2, x -> {
        });

        assertTrue(result.isParallel());
        assertEquals(3, ((StreamBase<?, ?, ?, ?, ?, ?, ?, ?>) result).maxThreadNum());
        assertEquals(((StreamBase<?, ?, ?, ?, ?, ?, ?, ?>) source).splitStrategy(), ((StreamBase<?, ?, ?, ?, ?, ?, ?, ?>) result).splitStrategy());
        assertEquals(98, result.count());
        assertEquals(1, closed.get());

        // Sequential streams are unchanged.
        final Stream<Integer> seq = Stream.of(1, 2, 3).skip(1, x -> {
        });
        assertFalse(seq.isParallel());
        assertEquals(Arrays.asList(2, 3), seq.toList());
    }

    // ---------------------------------------------------------------------------------------------------------
    // C-131 (doc-only) delay/rateLimited waits are uninterruptible; the documented takeWhile idiom stops a consumer.
    // ---------------------------------------------------------------------------------------------------------

    @Test
    public void testC131_delayWaitIsUninterruptibleAndKeepsTheInterruptStatus() {
        try {
            Thread.currentThread().interrupt();
            final long start = System.nanoTime();
            assertEquals(Arrays.asList(1, 2, 3), Stream.of(1, 2, 3).delay(java.time.Duration.ofMillis(60)).toList());
            assertTrue(System.nanoTime() - start >= java.util.concurrent.TimeUnit.MILLISECONDS.toNanos(100), "the waits were not cut short");
            assertTrue(Thread.currentThread().isInterrupted(), "interrupt status kept");
        } finally {
            Thread.interrupted();
        }
    }

    @Test
    public void testC131_takeWhileNotInterruptedStopsAPacedConsumer() throws InterruptedException {
        for (int variant = 0; variant < 2; variant++) {
            final int v = variant;
            final AtomicInteger emitted = new AtomicInteger();
            final boolean[] flagAfter = new boolean[1];
            final Thread consumer = new Thread(() -> {
                final Stream<Integer> paced = v == 0 ? Stream.generate(() -> 1).delay(java.time.Duration.ofMillis(20)) : Stream.generate(() -> 1).rateLimited(50);
                paced.takeWhile(x -> !Thread.currentThread().isInterrupted()).forEach(x -> emitted.incrementAndGet());
                flagAfter[0] = Thread.currentThread().isInterrupted();
            });

            consumer.setDaemon(true);
            consumer.start();

            // Interrupt only once the consumer is running (the first element can take a while on a cold JVM).
            final long deadline = System.currentTimeMillis() + 10_000;
            while (emitted.get() < 2 && System.currentTimeMillis() < deadline) {
                Thread.sleep(10);
            }

            consumer.interrupt();
            consumer.join(3000);

            assertFalse(consumer.isAlive(), "variant " + v + ": the consumer must stop after the interrupt");
            assertTrue(flagAfter[0], "variant " + v + ": interrupt status kept");
            assertTrue(emitted.get() >= 2, "variant " + v);
        }
    }
}

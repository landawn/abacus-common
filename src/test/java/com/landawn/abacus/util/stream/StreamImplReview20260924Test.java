package com.landawn.abacus.util.stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.function.Supplier;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.Dataset;
import com.landawn.abacus.util.IntFunctions;
import com.landawn.abacus.util.ListMultimap;
import com.landawn.abacus.util.Multiset;
import com.landawn.abacus.util.Pair;
import com.landawn.abacus.util.Tuple.Tuple2;
import com.landawn.abacus.util.Tuple.Tuple3;
import com.landawn.abacus.util.u.Optional;
import com.landawn.abacus.util.u.OptionalDouble;

/**
 * Cycle-1 regression tests (2026-09-24) for the object-stream implementation classes ({@code AbstractStream},
 * {@code ArrayStream}, {@code IteratorStream}, {@code Parallel*Stream}), {@code StreamBase}, {@code BaseStream} and
 * {@code Collectors}. Ledger: {@code scripts/cross_review/StreamFamily_Fn_MoreCollectors_Seq_Fnn_Throwables_ledger_2026-09-24.md}.
 */
public class StreamImplReview20260924Test extends TestBase {

    private static final Comparator<String> BY_LEN = Comparator.comparingInt(String::length);

    private static List<Integer> range(final int from, final int to) {
        final List<Integer> list = new ArrayList<>();

        for (int i = from; i < to; i++) {
            list.add(i);
        }

        return list;
    }

    // ---------------------------------------------------------------------------------------------------------
    // C-015 linkCloseToThis: a transfer that returns an already-closed stream closes the receiver too.
    // ---------------------------------------------------------------------------------------------------------

    @Test
    public void testC015_transformReturningClosedStreamClosesUpstream() {
        final AtomicInteger closed = new AtomicInteger();
        final Stream<Integer> src = Stream.of(1, 2).onClose(closed::incrementAndGet);
        final Stream<Integer> other = Stream.of(9);
        other.close();

        assertThrows(IllegalStateException.class, () -> src.transform(s -> other));
        assertEquals(1, closed.get());
        assertTrue(src.isClosed());
    }

    @Test
    public void testC015_transformReturningClosedStreamClosesUpstream_iteratorAndPrimitive() {
        final AtomicInteger closed = new AtomicInteger();
        final Stream<Integer> src = Stream.of(Arrays.asList(1, 2).iterator()).onClose(closed::incrementAndGet);
        final IntStream other = IntStream.of(9);
        other.close();

        assertThrows(IllegalStateException.class, () -> src.transform(s -> other));
        assertEquals(1, closed.get());

        final AtomicInteger closed2 = new AtomicInteger();
        final IntStream src2 = IntStream.of(1, 2).onClose(closed2::incrementAndGet);
        final Stream<String> other2 = Stream.of("x");
        other2.close();

        assertThrows(IllegalStateException.class, () -> src2.transform(s -> other2));
        assertEquals(1, closed2.get());
    }

    // ---------------------------------------------------------------------------------------------------------
    // C-016 a factory/supplier returning null -> NullPointerException, stream closed, empty and non-empty.
    // ---------------------------------------------------------------------------------------------------------

    private static void assertRejectsNullFactory(final Function<Stream<Integer>, Object> op, final Supplier<Stream<Integer>> source) {
        final AtomicInteger closed = new AtomicInteger();
        final Stream<Integer> s = source.get().onClose(closed::incrementAndGet);
        final NullPointerException e = assertThrows(NullPointerException.class, () -> op.apply(s));
        assertTrue(e.getMessage().contains("returned null"), e.getMessage());
        assertEquals(1, closed.get(), "the stream must be closed");
    }

    private static List<Supplier<Stream<Integer>>> sources() {
        final List<Supplier<Stream<Integer>>> list = new ArrayList<>();
        list.add(Stream::empty); // full, empty array: used to NPE at ArrayStream.toCollection
        list.add(() -> Stream.of(new Integer[0]));
        list.add(() -> Stream.of(1, 2).skip(2)); // empty sub-range: used to return null
        list.add(() -> Stream.of(Arrays.<Integer> asList().iterator()));
        list.add(() -> Stream.of(1, 2, 3));
        list.add(() -> Stream.of(new Integer[] { 0, 1, 2, 3, 4 }, 1, 4));
        list.add(() -> Stream.of(Arrays.asList(1, 2, 3).iterator()));
        return list;
    }

    @Test
    public void testC016_toCollectionAndToMultisetNullSupplier() {
        for (final Supplier<Stream<Integer>> src : sources()) {
            assertRejectsNullFactory(s -> s.toCollection(() -> null), src);
            assertRejectsNullFactory(s -> s.toMultiset(() -> null), src);
        }
    }

    @Test
    public void testC016_mapFactoryReturningNull() {
        for (final Supplier<Stream<Integer>> src : sources()) {
            assertRejectsNullFactory(s -> s.toMap(x -> x, x -> x, (a, b) -> a, () -> null), src);
            assertRejectsNullFactory(s -> s.toMap(x -> x, x -> x, () -> (Map<Integer, Integer>) null), src);
            assertRejectsNullFactory(s -> s.toMultimap(x -> x, () -> (ListMultimap<Integer, Integer>) null), src);
            assertRejectsNullFactory(s -> s.groupTo(x -> x, () -> (Map<Integer, List<Integer>>) null), src);
            assertRejectsNullFactory(s -> s.flatGroupTo(x -> Arrays.asList(x), () -> (Map<Integer, List<Integer>>) null), src);
        }
    }

    @Test
    public void testC016_parallelMapFactoryReturningNull() {
        final Integer[] a = range(0, 1000).toArray(new Integer[0]);

        // U23-01 (2026-09-25): the SplitStrategy.ARRAY branches of ParallelArrayStream carry their own copy of the
        // factory-null checks (toMap/groupTo/flatGroupTo/toMultimap/collect); DEFAULT_SPLIT_STRATEGY is ITERATOR, so
        // they are reached only through an explicit ARRAY ParallelSettings source.
        for (final Supplier<Stream<Integer>> src : Arrays.<Supplier<Stream<Integer>>> asList(() -> Stream.of(a).parallel(4),
                () -> Stream.of(a).parallel(new BaseStream.ParallelSettings(4, BaseStream.SplitStrategy.ITERATOR, null)),
                () -> Stream.of(a).parallel(new BaseStream.ParallelSettings(4, BaseStream.SplitStrategy.ARRAY, null)),
                () -> Stream.of(Arrays.asList(a).iterator()).parallel(4))) {
            assertRejectsNullFactory(s -> s.toMap(x -> x, x -> x, (x, y) -> x, () -> null), src);
            assertRejectsNullFactory(s -> s.toMultimap(x -> x, x -> x, () -> (ListMultimap<Integer, Integer>) null), src);
            assertRejectsNullFactory(s -> s.groupTo(x -> x % 7, () -> (Map<Integer, List<Integer>>) null), src);
            assertRejectsNullFactory(s -> s.flatGroupTo(x -> Arrays.asList(x % 7), () -> (Map<Integer, List<Integer>>) null), src);
            assertRejectsNullFactory(s -> s.collect(() -> (List<Integer>) null, List::add), src);
            assertRejectsNullFactory(s -> s.collect(() -> (List<Integer>) null, List::add, List::addAll), src);
        }

        // The ARRAY strategy's valid paths are unchanged (slice merge keeps the encounter order of collect).
        final Supplier<Stream<Integer>> arrayStrategy = () -> Stream.of(a).parallel(new BaseStream.ParallelSettings(4, BaseStream.SplitStrategy.ARRAY, null));
        assertTrue(arrayStrategy.get().isParallel());
        assertEquals(1000, arrayStrategy.get().toMap(x -> x, x -> x, (x, y) -> x, HashMap::new).size());
        assertEquals(7, arrayStrategy.get().groupTo(x -> x % 7, () -> new HashMap<>()).size());
        assertEquals(7, arrayStrategy.get().flatGroupTo(x -> Arrays.asList(x % 7), () -> new HashMap<>()).size());
        assertEquals(1000, arrayStrategy.get().toMultimap(x -> x, x -> x).size());
        assertEquals(range(0, 1000), arrayStrategy.get().collect(ArrayList::new, List::add, List::addAll));
    }

    @Test
    public void testC016_splitSlidingCollapseNullCollectionSupplier() {
        for (final Supplier<Stream<Integer>> src : Arrays.<Supplier<Stream<Integer>>> asList(() -> Stream.of(1, 2, 3),
                () -> Stream.of(Arrays.asList(1, 2, 3).iterator()))) {
            assertRejectsNullFactory(s -> s.split(2, n -> (List<Integer>) null).toList(), src);
            assertRejectsNullFactory(s -> s.split(x -> x > 1, () -> (List<Integer>) null).toList(), src);
            assertRejectsNullFactory(s -> s.sliding(2, 1, n -> (List<Integer>) null).toList(), src);
            assertRejectsNullFactory(s -> s.collapse((x, y) -> true, () -> (List<Integer>) null).toList(), src);
        }

        // Nothing is produced for an empty source, so the supplier is never asked.
        assertEquals(0, Stream.<Integer> empty().split(2, n -> (List<Integer>) null).count());
    }

    @Test
    public void testC016_validSuppliersStillWork() {
        assertEquals(Arrays.asList(1, 2, 3), Stream.of(1, 2, 3).toCollection(ArrayList::new));
        assertEquals(new ArrayList<>(), Stream.<Integer> empty().toCollection(ArrayList::new));
        assertEquals(2, Stream.of("é", "é").toMultiset(Multiset::new).count("é"));
        assertEquals(Map.of(1, 1), Stream.of(1).toMap(x -> x, x -> x, (a, b) -> a, HashMap::new));
    }

    // ---------------------------------------------------------------------------------------------------------
    // C-065 persist(PreparedStatement, batchSize >= 2) clears the pending batch on failure.
    // ---------------------------------------------------------------------------------------------------------

    private static PreparedStatement recordingStatement(final List<String> calls, final boolean failClearBatch) {
        return (PreparedStatement) Proxy.newProxyInstance(StreamImplReview20260924Test.class.getClassLoader(), new Class<?>[] { PreparedStatement.class },
                (proxy, method, args) -> {
                    calls.add(method.getName());

                    if (failClearBatch && "clearBatch".equals(method.getName())) {
                        throw new SQLException("clearBatch failed");
                    }

                    switch (method.getName()) {
                        case "executeBatch":
                            return new int[0];
                        case "execute":
                            return false;
                        case "hashCode":
                            return System.identityHashCode(proxy);
                        case "equals":
                            return proxy == args[0];
                        case "toString":
                            return "stmt";
                        default:
                            return null;
                    }
                });
    }

    @Test
    public void testC065_persistClearsPendingBatchOnFailure() {
        final List<String> calls = new ArrayList<>();
        final PreparedStatement stmt = recordingStatement(calls, false);

        final SQLException e = assertThrows(SQLException.class, () -> Stream.of(1, 2, 3, 4).persist(stmt, 5, 0, (x, st) -> {
            if (x == 3) {
                throw new SQLException("boom");
            }

            st.setInt(1, x);
        }));

        assertEquals("boom", e.getMessage());
        assertEquals(Arrays.asList("setInt", "addBatch", "setInt", "addBatch", "clearBatch"), calls);
    }

    @Test
    public void testC065_persistClearBatchFailureIsSuppressed() {
        final List<String> calls = new ArrayList<>();
        final PreparedStatement stmt = recordingStatement(calls, true);

        final RuntimeException e = assertThrows(RuntimeException.class,
                () -> Stream.of(Arrays.asList(1, 2).iterator()).map(x -> x == 2 ? 1 / 0 : x).persist(stmt, 5, 0, (x, st) -> st.setInt(1, x)));

        assertTrue(e instanceof ArithmeticException, e.toString());
        assertEquals(1, e.getSuppressed().length);
        assertEquals("clearBatch failed", e.getSuppressed()[0].getMessage());
    }

    @Test
    public void testC065_persistSuccessAndNoBatchPathUnchanged() throws SQLException {
        final List<String> calls = new ArrayList<>();
        assertEquals(3, Stream.of(1, 2, 3).persist(recordingStatement(calls, false), 2, 0, (x, st) -> st.setInt(1, x)));
        // Only the clearBatch() that follows each executeBatch() (DataSourceUtil.executeBatch) - no extra one.
        assertEquals(2, calls.stream().filter("executeBatch"::equals).count());
        assertEquals(2, calls.stream().filter("clearBatch"::equals).count());

        final List<String> calls2 = new ArrayList<>();
        assertThrows(SQLException.class, () -> Stream.of(1, 2).persist(recordingStatement(calls2, false), 1, 0, (x, st) -> {
            throw new SQLException("x");
        }));
        assertFalse(calls2.contains("clearBatch"), "batchSize 1 does not use batches");
    }

    // ---------------------------------------------------------------------------------------------------------
    // C-066 persistToCsv validates every row against the header count / the first row's kind.
    // ---------------------------------------------------------------------------------------------------------

    private static IllegalArgumentException csvFailure(final Stream<?> s) {
        return assertThrows(IllegalArgumentException.class, () -> s.persistToCsv(Arrays.asList("h1", "h2"), new java.io.StringWriter()));
    }

    @Test
    public void testC066_persistToCsvRowSizeAndTypeValidated() {
        IllegalArgumentException e = csvFailure(Stream.of(Arrays.asList("a", "b"), Arrays.asList("c")));
        assertTrue(e.getMessage().contains("row 2") && e.getMessage().contains("1 field"), e.getMessage());

        e = csvFailure(Stream.of(Arrays.asList("a", "b"), Arrays.asList("c", "d", "e")));
        assertTrue(e.getMessage().contains("row 2") && e.getMessage().contains("3 field"), e.getMessage());

        e = csvFailure(Stream.of(java.util.Collections.singletonList(Arrays.asList("a"))));
        assertTrue(e.getMessage().contains("row 1"), e.getMessage());

        e = csvFailure(Stream.<Object[]> of(new Object[] { "a", "b" }, new Object[] { "c" }));
        assertTrue(e.getMessage().contains("row 2"), e.getMessage());

        e = csvFailure(Stream.<Object[]> of(new Object[] { "a", "b", "c" }));
        assertTrue(e.getMessage().contains("row 1"), e.getMessage());

        e = csvFailure(Stream.<Object> of(Arrays.asList("a", "b"), new Object[] { "c", "d" }));
        assertTrue(e.getMessage().contains("row 2"), e.getMessage());

        e = csvFailure(Stream.<Object> of(Map.of("h1", 1, "h2", 2), "not a map"));
        assertTrue(e.getMessage().contains("row 2"), e.getMessage());

        e = csvFailure(Stream.of(Arrays.asList("a", "b"), Arrays.asList("c", "d"), null));
        assertTrue(e.getMessage().contains("row 3") && e.getMessage().contains("null"), e.getMessage());
    }

    @Test
    public void testC066_persistToCsvWellFormedRowsUnchanged() throws Exception {
        final java.io.StringWriter w = new java.io.StringWriter();
        assertEquals(2, Stream.of(Arrays.asList("a", "é"), Arrays.asList("c", "d")).persistToCsv(Arrays.asList("h1", "h2"), w));
        assertEquals("\"h1\",\"h2\"\n\"a\",\"é\"\n\"c\",\"d\"", w.toString().trim());

        final java.io.StringWriter w2 = new java.io.StringWriter();
        final Map<String, Object> row1 = new LinkedHashMap<>();
        row1.put("h1", 1);
        row1.put("h2", 2);
        final Map<String, Object> row2 = new LinkedHashMap<>();
        row2.put("h1", 3);
        assertEquals(2, Stream.of(row1, row2).persistToCsv(w2));
        assertEquals("\"h1\",\"h2\"\n1,2\n3,null", w2.toString().trim());
    }

    // ---------------------------------------------------------------------------------------------------------
    // C-067 onEachSave(File) creates/truncates the file when traversal starts, even for an empty stream.
    // ---------------------------------------------------------------------------------------------------------

    @Test
    public void testC067_onEachSaveFileTruncatedOnEmptyTraversal() throws Exception {
        final File f = File.createTempFile("onEachSave", ".txt");

        try {
            Files.write(f.toPath(), "STALE\n".getBytes(StandardCharsets.UTF_8));
            assertEquals(0, Stream.empty().onEachSave(f).count());
            assertEquals(0, f.length());

            Files.write(f.toPath(), "STALE\n".getBytes(StandardCharsets.UTF_8));
            assertEquals(0, Stream.<String> empty().onEachSave((e, w) -> w.write(e), f).count());
            assertEquals(0, f.length());

            Files.write(f.toPath(), "STALE\n".getBytes(StandardCharsets.UTF_8));
            assertEquals(Arrays.asList("é", "b"), Stream.of("é", "b").onEachSave(f).toList());
            assertEquals("é\nb\n", new String(Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8));

            // Never traversed: the file is left alone.
            Files.write(f.toPath(), "KEEP\n".getBytes(StandardCharsets.UTF_8));
            Stream.of("x").onEachSave(f).close();
            assertEquals("KEEP\n", new String(Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8));
        } finally {
            f.delete();
        }
    }

    // ---------------------------------------------------------------------------------------------------------
    // C-075 skipRange message names both values.
    // ---------------------------------------------------------------------------------------------------------

    @Test
    public void testC075_skipRangeMessageIncludesBothValues() {
        final IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> Stream.of(1, 2, 3).skipRange(5, 2));
        assertEquals("'startInclusive' (5) must be <= 'endExclusive' (2)", e.getMessage());
        assertEquals(Arrays.asList(1, 3), Stream.of(1, 2, 3).skipRange(1, 2).toList());
        assertEquals(Arrays.asList(1, 2, 3), Stream.of(1, 2, 3).skipRange(2, 2).toList());
    }

    // ---------------------------------------------------------------------------------------------------------
    // C-077 delay(java.time.Duration) overflow closes the stream (keeps ArithmeticException).
    // ---------------------------------------------------------------------------------------------------------

    @Test
    public void testC077_delayJdkDurationOverflowClosesStream() {
        final AtomicInteger closed = new AtomicInteger();
        final Stream<Integer> s = Stream.of(1, 2).onClose(closed::incrementAndGet);
        assertThrows(ArithmeticException.class, () -> s.delay(java.time.Duration.ofSeconds(Long.MAX_VALUE)));
        assertEquals(1, closed.get());
        assertTrue(s.isClosed());

        final AtomicInteger closed2 = new AtomicInteger();
        final IntStream is = IntStream.of(1).onClose(closed2::incrementAndGet);
        assertThrows(ArithmeticException.class, () -> is.delay(java.time.Duration.ofSeconds(Long.MIN_VALUE)));
        assertEquals(1, closed2.get());

        assertEquals(Arrays.asList(1, 2), Stream.of(1, 2).delay(java.time.Duration.ofMillis(1)).toList());
    }

    // ---------------------------------------------------------------------------------------------------------
    // C-078 sps/psp reject a null ops result with NPE (stream closed); transform keeps returning null.
    // ---------------------------------------------------------------------------------------------------------

    @Test
    public void testC078_spsPspNullResultRejected() {
        final List<Function<Stream<Integer>, Object>> ops = Arrays.asList(s -> s.sps(t -> null), s -> s.sps(2, t -> null),
                s -> s.sps(2, java.util.concurrent.Executors.newCachedThreadPool(), t -> null), s -> s.psp(t -> null), s -> s.parallel().sps(t -> null),
                s -> s.parallel(3).sps(3, t -> null), s -> s.parallel().psp(t -> null));

        for (final Function<Stream<Integer>, Object> op : ops) {
            final AtomicInteger closed = new AtomicInteger();
            final Stream<Integer> s = Stream.of(1, 2).onClose(closed::incrementAndGet);
            final NullPointerException e = assertThrows(NullPointerException.class, () -> op.apply(s));
            assertEquals("ops returned null", e.getMessage());
            assertEquals(1, closed.get());
        }

        final AtomicInteger closed = new AtomicInteger();
        final IntStream is = IntStream.of(1).onClose(closed::incrementAndGet);
        assertThrows(NullPointerException.class, () -> is.sps(t -> null));
        assertEquals(1, closed.get());
    }

    @Test
    public void testC078_transformNullStillReturnsNullAndCloses() {
        final AtomicInteger closed = new AtomicInteger();
        assertNull(Stream.of(1).onClose(closed::incrementAndGet).transform(s -> null));
        assertEquals(1, closed.get());
        assertEquals(Arrays.asList(2, 4), Stream.of(1, 2).sps(s -> s.map(x -> x * 2)).sorted().toList());
    }

    // ---------------------------------------------------------------------------------------------------------
    // C-001 step skips the gap lazily.
    // ---------------------------------------------------------------------------------------------------------

    @Test
    public void testC001_stepPullsGapLazily() {
        final AtomicInteger pulls = new AtomicInteger();
        assertEquals(Arrays.asList(0, 1000), Stream.of(range(0, 10000).iterator()).peek(x -> pulls.incrementAndGet()).step(1000).limit(2).toList());
        assertEquals(1001, pulls.get());

        final AtomicInteger gen = new AtomicInteger();
        assertEquals(Arrays.asList(0), Stream.generate(gen::getAndIncrement).step(1_000_000).limit(1).toList());
        assertEquals(1, gen.get());

        assertEquals(Optional.of(1), Stream.of(1, 2, 3).map(x -> x == 2 ? 1 / 0 : x).step(2).first());

        final AtomicInteger pulls2 = new AtomicInteger();
        assertEquals(Optional.of(0), Stream.of(range(0, 10).iterator()).peek(x -> pulls2.incrementAndGet()).step(3).first());
        assertEquals(1, pulls2.get());
    }

    @Test
    public void testC001_stepValuesUnchanged() {
        assertEquals(Arrays.asList(0, 3, 6, 9), Stream.of(range(0, 10).iterator()).step(3).toList());
        assertEquals(Arrays.asList(0, 3, 6, 9), Stream.of(range(0, 10)).map(x -> x).step(3).toList());
        assertEquals(4, Stream.of(range(0, 10)).map(x -> x).step(3).count());
        assertEquals(Arrays.asList(3, 6, 9), Stream.of(range(0, 10)).map(x -> x).step(3).skip(1).toList());
        assertEquals(Arrays.asList(), Stream.<Integer> empty().map(x -> x).step(3).toList());
        assertEquals(Arrays.asList(0), Stream.of(range(0, 1).iterator()).step(Long.MAX_VALUE).toList());

        final List<Integer> par = new ArrayList<>(Stream.of(range(0, 100).iterator()).parallel(4).step(10).toList());
        par.sort(null);
        assertEquals(Arrays.asList(0, 10, 20, 30, 40, 50, 60, 70, 80, 90), par);
    }

    // ---------------------------------------------------------------------------------------------------------
    // C-002 iterator split/sliding size the container from the actual element count.
    // ---------------------------------------------------------------------------------------------------------

    @Test
    public void testC002_hugeChunkOrWindowSizeOnSmallIteratorSource() {
        assertEquals(Arrays.asList(Arrays.asList(1, 2, 3)), Stream.of(Arrays.asList(1, 2, 3).iterator()).split(Integer.MAX_VALUE).toList());
        assertEquals(Arrays.asList(Arrays.asList(1, 2, 3)), Stream.of(Arrays.asList(1, 2, 3).iterator()).sliding(Integer.MAX_VALUE).toList());
        assertEquals(Arrays.asList(Arrays.asList(1, 2, 3)), Stream.of(1, 2, 3).map(x -> x).split(Integer.MAX_VALUE - 8).toList());
        assertEquals(Arrays.asList(Arrays.asList(1, 2, 3)), Stream.of(1, 2, 3).map(x -> x).sliding(Integer.MAX_VALUE - 8, 1).toList());
    }

    @Test
    public void testC002_supplierSeesExactCounts() {
        final List<Integer> seen = new ArrayList<>();
        assertEquals(Arrays.asList(Arrays.asList(1, 2), Arrays.asList(3, 4), Arrays.asList(5)), Stream.of(Arrays.asList(1, 2, 3, 4, 5).iterator()).split(2, n -> {
            seen.add(n);
            return new ArrayList<>(n);
        }).toList());
        assertEquals(Arrays.asList(2, 2, 1), seen);

        seen.clear();
        assertEquals(Arrays.asList(Arrays.asList(1, 2, 3), Arrays.asList(3, 4, 5), Arrays.asList(5, 6)),
                Stream.of(Arrays.asList(1, 2, 3, 4, 5, 6).iterator()).sliding(3, 2, n -> {
                    seen.add(n);
                    return new ArrayList<>(n);
                }).toList());
        assertEquals(Arrays.asList(3, 3, 2), seen);
    }

    @Test
    public void testC002_boundedCapacitySupplierStillWorks() {
        final List<Integer> sizes = Stream.of(range(0, 100).iterator())
                .split(40, IntFunctions.<Integer> ofArrayBlockingQueue())
                .map(Collection::size)
                .toList();
        assertEquals(Arrays.asList(40, 40, 20), sizes);

        final List<Integer> windowSizes = Stream.of(range(0, 10).iterator())
                .sliding(4, 3, IntFunctions.<Integer> ofArrayBlockingQueue())
                .map(Collection::size)
                .toList();
        assertEquals(Arrays.asList(4, 4, 4), windowSizes);

        assertEquals(Arrays.asList(Arrays.asList(1, 2), Arrays.asList(3)), Stream.of(Arrays.asList(1, 2, 3).iterator()).split(2).toList());
        assertEquals(Arrays.asList(), Stream.of(Arrays.<Integer> asList().iterator()).sliding(2).toList());
    }

    // ---------------------------------------------------------------------------------------------------------
    // C-004 Stream.averageLong is exact (no long wrap).
    // ---------------------------------------------------------------------------------------------------------

    @Test
    public void testC004_averageLongDoesNotWrap() {
        assertEquals(OptionalDouble.of(9.223372036854776E18), Stream.of(Long.MAX_VALUE, Long.MAX_VALUE).averageLong(x -> x));
        assertEquals(OptionalDouble.of(9.223372036854776E18), Stream.of(Long.MAX_VALUE, Long.MAX_VALUE).parallel(2).averageLong(x -> x));
        assertEquals(-3.0744573456182584E18, Stream.of(Long.MIN_VALUE, Long.MIN_VALUE, Long.MAX_VALUE).averageLong(x -> x).orElseThrow(), 1e3);
        assertEquals(OptionalDouble.empty(), Stream.<Long> empty().averageLong(x -> x));
        assertEquals(OptionalDouble.of(2.0), Stream.of(1L, 3L).averageLong(x -> x));
    }

    // ---------------------------------------------------------------------------------------------------------
    // C-011 groupJoin: a null right key matches a null left key.
    // ---------------------------------------------------------------------------------------------------------

    private static Integer key(final String s) {
        return s == null ? null : 1;
    }

    @Test
    public void testC011_groupJoinNullRightKey() {
        final List<String> right = Arrays.asList("x", null);

        assertEquals(Arrays.asList(Pair.of("a", Arrays.asList("x")), Pair.of(null, Arrays.asList((String) null))),
                Stream.of("a", null).groupJoin(right, StreamImplReview20260924Test::key, StreamImplReview20260924Test::key).toList());

        assertEquals(Arrays.asList(Pair.of("a", Arrays.asList("x")), Pair.of(null, Arrays.asList((String) null))),
                Stream.of("a", null).groupJoin(right, StreamImplReview20260924Test::key).toList());

        assertEquals(Arrays.asList(Pair.of("a", 1L), Pair.of(null, 1L)), Stream.of("a", null)
                .groupJoin(right, StreamImplReview20260924Test::key, StreamImplReview20260924Test::key, java.util.stream.Collectors.counting())
                .toList());

        final AtomicInteger bClosed = new AtomicInteger();
        assertEquals(Arrays.asList("a=[x]", "null=[null]"),
                Stream.of("a", null)
                        .groupJoin(Stream.of(right).onClose(bClosed::incrementAndGet), StreamImplReview20260924Test::key, StreamImplReview20260924Test::key,
                                (l, r) -> l + "=" + r)
                        .toList());
        assertEquals(1, bClosed.get());

        final AtomicInteger bClosed2 = new AtomicInteger();
        assertEquals(Arrays.asList("a=1", "null=1"),
                Stream.of("a", null)
                        .groupJoin(Stream.of(right).onClose(bClosed2::incrementAndGet), StreamImplReview20260924Test::key,
                                StreamImplReview20260924Test::key, java.util.stream.Collectors.counting(), (l, r) -> l + "=" + r)
                        .toList());
        assertEquals(1, bClosed2.get());

        final List<Pair<String, List<String>>> par = new ArrayList<>(
                Stream.of("a", null, "b").parallel(2).groupJoin(right, StreamImplReview20260924Test::key, StreamImplReview20260924Test::key).toList());
        assertEquals(3, par.size());
        assertTrue(par.contains(Pair.of((String) null, Arrays.asList((String) null))));
    }

    @Test
    public void testC011_groupJoinUnchangedForNonNullKeysAndMissingKeys() {
        assertEquals(Arrays.asList(Pair.of(1, Arrays.asList(1, 1)), Pair.of(2, Arrays.asList())),
                Stream.of(1, 2).groupJoin(Arrays.asList(1, 1, 3), Function.identity(), Function.identity()).toList());
    }

    // ---------------------------------------------------------------------------------------------------------
    // C-012 parallel splitAt(.., Collector) keeps [head, tail] and in-order contents.
    // ---------------------------------------------------------------------------------------------------------

    @Test
    public void testC012_parallelSplitAtCollectorKeepsOrder() {
        final Integer[] a = range(0, 20_000).toArray(new Integer[0]);
        final List<Integer> head = range(0, 10_000);
        final List<Integer> tail = range(10_000, 20_000);

        for (int i = 0; i < 30; i++) {
            assertEquals(Arrays.asList(head, tail), Stream.of(a).parallel(4).splitAt(10_000, java.util.stream.Collectors.toList()).toList());
            assertEquals(Arrays.asList(head, tail), Stream.of(a).parallel(4).splitAt(x -> x == 10_000, java.util.stream.Collectors.toList()).toList());
            assertEquals(Arrays.asList(head, tail),
                    Stream.of(Arrays.asList(a).iterator()).parallel(4).splitAt(10_000, java.util.stream.Collectors.toList()).toList());
        }

        assertEquals(Arrays.asList(6, 15), Stream.of(1, 2, 3, 4, 5, 6).parallel(3).splitAt(x -> x == 4, Collectors.summingInt(x -> x)).toList());
        assertEquals(Arrays.asList(6, 15), Stream.of(1, 2, 3, 4, 5, 6).splitAt(3, Collectors.summingInt(x -> x)).toList());
    }

    // ---------------------------------------------------------------------------------------------------------
    // C-017 max resolves ties to the FIRST maximal element, also after sorted() with the same comparator.
    // ---------------------------------------------------------------------------------------------------------

    @Test
    public void testC017_maxAfterSortedReturnsFirstTie() {
        assertEquals("banana", Stream.of("banana", "cherry").max(BY_LEN).get());
        assertEquals("banana", Stream.of("banana", "cherry").sorted(BY_LEN).max(BY_LEN).get());
        // The sort is stable: [a, dd, bb, cc] - "dd" is the first of the three maximal elements.
        assertEquals("dd", Stream.of("dd", "a", "bb", "cc").sorted(BY_LEN).max(BY_LEN).get());
        assertEquals("dd", Stream.of(Arrays.asList("dd", "a", "bb", "cc").iterator()).sorted(BY_LEN).filter(x -> true).max(BY_LEN).get());
        for (int i = 0; i < 50; i++) {
            // On a parallel stream the documented rule is weaker: WHICH of the maximal elements is returned is
            // unspecified (parallel sort and the parallel max scan do not preserve tie order), but it is maximal.
            final Stream<String> par = Stream.of("dd", "a", "bb", "cc").parallel(2).sorted(BY_LEN);
            assertTrue(par.isParallel());
            assertTrue(Arrays.asList("dd", "bb", "cc").contains(par.max(BY_LEN).get()));
        }
        assertEquals("dd", Stream.of(new String[] { "x", "dd", "a", "bb", "cc" }, 1, 5).sorted(BY_LEN).max(BY_LEN).get());
        assertEquals("dd", Stream.of("dd", "a", "bb", "cc").max(BY_LEN).get());
        assertEquals("a", Stream.of("a").sorted(BY_LEN).max(BY_LEN).get());
        assertEquals("ccc", Stream.of("a", "ccc", "bb").sorted(BY_LEN).max(BY_LEN).get());
        assertFalse(Stream.<String> empty().sorted(BY_LEN).max(BY_LEN).isPresent());
        assertEquals("dd", Stream.of("dd", "a", "bb", "cc").sorted(BY_LEN).min(Comparator.comparingInt((String s) -> -s.length())).get());
    }

    // ---------------------------------------------------------------------------------------------------------
    // C-038 shuffled() still permutes (now a fresh non-secure Random per call).
    // ---------------------------------------------------------------------------------------------------------

    @Test
    public void testC038_shuffledIsPermutation() {
        final List<Integer> shuffled = new ArrayList<>(Stream.of(range(0, 1000)).shuffled().toList());
        assertEquals(1000, shuffled.size());
        shuffled.sort(null);
        assertEquals(range(0, 1000), shuffled);
        assertEquals(Arrays.asList(), Stream.empty().shuffled().toList());
    }

    // ---------------------------------------------------------------------------------------------------------
    // C-063 mapPartial*: a null Optional from the mapper is an NPE with a message.
    // ---------------------------------------------------------------------------------------------------------

    @Test
    public void testC063_mapPartialNullOptionalMessage() {
        // Cycle 2 (D5-07): the message names the optional type the mapper should have returned.
        final String msg = "mapper returned a null %1$s; return %1$s.empty() for no result";
        assertEquals(String.format(msg, "Optional"),
                assertThrows(NullPointerException.class, () -> Stream.of(1).mapPartial(x -> null).toList()).getMessage());
        assertEquals(String.format(msg, "OptionalInt"),
                assertThrows(NullPointerException.class, () -> Stream.of(1).mapPartialToInt(x -> null).toArray()).getMessage());
        assertEquals(String.format(msg, "OptionalLong"),
                assertThrows(NullPointerException.class, () -> Stream.of(1).mapPartialToLong(x -> null).toArray()).getMessage());
        assertEquals(String.format(msg, "OptionalDouble"),
                assertThrows(NullPointerException.class, () -> Stream.of(1).mapPartialToDouble(x -> null).toArray()).getMessage());
        assertEquals(String.format(msg, "java.util.Optional"),
                assertThrows(NullPointerException.class, () -> Stream.of(1).mapPartialJdk(x -> null).toList()).getMessage());
        assertEquals(String.format(msg, "java.util.OptionalInt"),
                assertThrows(NullPointerException.class, () -> Stream.of(1).mapPartialToIntJdk(x -> null).toArray()).getMessage());
        assertEquals(String.format(msg, "java.util.OptionalLong"),
                assertThrows(NullPointerException.class, () -> Stream.of(1).mapPartialToLongJdk(x -> null).toArray()).getMessage());
        assertEquals(String.format(msg, "java.util.OptionalDouble"),
                assertThrows(NullPointerException.class, () -> Stream.of(1).mapPartialToDoubleJdk(x -> null).toArray()).getMessage());

        assertEquals(Arrays.asList(2), Stream.of(1, 2).mapPartial(x -> x == 2 ? Optional.of(x) : Optional.empty()).toList());
    }

    // ---------------------------------------------------------------------------------------------------------
    // Q3-06 partitionTo: LinkedHashMap, false-then-true.
    // ---------------------------------------------------------------------------------------------------------

    @Test
    public void testQ306_partitionToOrder() {
        final Map<Boolean, List<Integer>> m = Stream.of(1, 2, 3).partitionTo(x -> x % 2 == 1);
        assertTrue(m instanceof LinkedHashMap, m.getClass().toString());
        assertEquals(Arrays.asList(false, true), new ArrayList<>(m.keySet()));
        assertEquals(Arrays.asList(2), m.get(false));
        assertEquals(Arrays.asList(1, 3), m.get(true));

        final Map<Boolean, Long> empty = Stream.<Integer> empty().partitionTo(x -> true, Collectors.counting());
        assertEquals(Arrays.asList(false, true), new ArrayList<>(empty.keySet()));
        assertEquals(Arrays.asList(0L, 0L), new ArrayList<>(empty.values()));
    }

    // ---------------------------------------------------------------------------------------------------------
    // S2-12 combinations(len == count) does not close the source during the intermediate call.
    // ---------------------------------------------------------------------------------------------------------

    @Test
    public void testS212_combinationsFullLengthDoesNotCloseEarly() {
        final List<String> events = new ArrayList<>();
        final Stream<List<Integer>> s = Stream.of(1, 2, 3).onClose(() -> events.add("closed")).combinations(3);
        events.add("after-call");
        assertEquals(Arrays.asList(Arrays.asList(1, 2, 3)), s.toList());
        assertEquals(Arrays.asList("after-call", "closed"), events);
    }

    // ---------------------------------------------------------------------------------------------------------
    // S2-13 joins lock a private object, not the caller's key extractor.
    // ---------------------------------------------------------------------------------------------------------

    @Test
    public void testS213_joinDoesNotLockCallerKeyExtractor() throws Exception {
        final Function<Integer, Integer> keyFn = x -> x;
        final CountDownLatch locked = new CountDownLatch(1);
        final CountDownLatch release = new CountDownLatch(1);
        final Thread holder = new Thread(() -> {
            synchronized (keyFn) {
                locked.countDown();

                try {
                    release.await();
                } catch (final InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        });
        holder.setDaemon(true);
        holder.start();
        locked.await();

        try {
            assertTimeoutPreemptively(java.time.Duration.ofSeconds(5),
                    () -> assertEquals(Arrays.asList(Pair.of(1, 1)), Stream.of(1, 2).innerJoin(Arrays.asList(1, 3), keyFn, keyFn).toList()));
            assertTimeoutPreemptively(java.time.Duration.ofSeconds(5),
                    () -> assertEquals(2, Stream.of(1, 2).leftJoin(Arrays.asList(1, 3), keyFn, keyFn).count()));
        } finally {
            release.countDown();
        }
    }

    // ---------------------------------------------------------------------------------------------------------
    // E2-05 duplicate-key message names the key.
    // ---------------------------------------------------------------------------------------------------------

    @Test
    public void testE205_duplicateKeyMessageIncludesKey() {
        IllegalStateException e = assertThrows(IllegalStateException.class, () -> Stream.of("é", "é").toMap(x -> x, x -> 1));
        assertEquals("Duplicate key é (attempted merging values 1 and 1)", e.getMessage());

        e = assertThrows(IllegalStateException.class, () -> Stream.of(Arrays.asList("a", "a").iterator()).toMap(x -> x, x -> 1));
        assertEquals("Duplicate key a (attempted merging values 1 and 1)", e.getMessage());

        e = assertThrows(IllegalStateException.class, () -> Stream.of("a", "b", "a").collect(Collectors.toMap(x -> x, String::length)));
        assertEquals("Duplicate key a (attempted merging values 1 and 1)", e.getMessage());

        e = assertThrows(IllegalStateException.class, () -> Stream.of(new String[] { "k", null }).toMap(x -> "k", x -> x));
        assertEquals("Duplicate key k (attempted merging values k and null)", e.getMessage());

        // Other merge functions are untouched.
        assertEquals(Map.of("a", 2), Stream.of("a", "a").toMap(x -> x, x -> 1, Integer::sum));
    }

    // ---------------------------------------------------------------------------------------------------------
    // N1-04 toDataset(columnNames) snapshots the list; N1-05 multi-column averages unchanged.
    // ---------------------------------------------------------------------------------------------------------

    @Test
    public void testN104_toDatasetSnapshotsColumnNames() {
        final List<String> cols = new ArrayList<>(Arrays.asList("a", "b"));
        final java.util.stream.Collector<Object[], ?, Dataset> c = Collectors.MoreCollectors.toDataset(cols);
        cols.add("c");
        final Dataset ds = Stream.<Object[]> of(new Object[] { 1, 2 }).collect(c);
        assertEquals(Arrays.asList("a", "b"), ds.columnNames());

        cols.clear();
        assertEquals(Arrays.asList("a", "b"), Stream.<Object[]> of(new Object[] { 3, 4 }).collect(c).columnNames());
    }

    @Test
    public void testN105_multiColumnAveragesUnchanged() {
        final List<int[]> rows = Arrays.asList(new int[] { 1, 10, 100 }, new int[] { 2, 20, 200 }, new int[] { 4, 40, 400 });
        final double a0 = 7 / 3.0;

        final Tuple2<Double, Double> t2 = Stream.of(rows).collect(Collectors.MoreCollectors.averagingInt(r -> r[0], r -> r[1]));
        assertEquals(a0, t2._1, 1e-12);
        assertEquals(a0 * 10, t2._2, 1e-12);

        final Tuple3<Double, Double, Double> t3 = Stream.of(rows).parallel(3).collect(Collectors.MoreCollectors.averagingLong(r -> r[0], r -> r[1], r -> r[2]));
        assertEquals(a0, t3._1, 1e-12);
        assertEquals(a0 * 10, t3._2, 1e-12);
        assertEquals(a0 * 100, t3._3, 1e-12);

        final Tuple2<java.math.BigDecimal, java.math.BigDecimal> bd = Stream.of(rows)
                .collect(Collectors.MoreCollectors.averagingBigInteger(r -> java.math.BigInteger.valueOf(r[0]), r -> java.math.BigInteger.valueOf(r[1])));
        assertEquals(0, new java.math.BigDecimal("2.333333333333333333333333333333333").compareTo(bd._1));

        final Tuple3<Double, Double, Double> empty = Stream.<int[]> empty().collect(Collectors.MoreCollectors.averagingInt(r -> r[0], r -> r[1], r -> r[2]));
        assertEquals(0.0, empty._1);
        assertEquals(0.0, empty._3);
    }
}

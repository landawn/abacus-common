package com.landawn.abacus.util.stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;
import java.util.function.Supplier;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.exception.UncheckedIOException;
import com.landawn.abacus.util.Duration;
import com.landawn.abacus.util.MergeResult;
import com.landawn.abacus.util.ObjIterator;
import com.landawn.abacus.util.u.Optional;
import com.landawn.abacus.util.stream.Stream.WindowHandler;

/**
 * Regression tests for the cycle-1 fixes to {@code Stream.java} of the 2026-09-24 review of the stream family (ledger
 * {@code scripts/cross_review/StreamFamily_Fn_MoreCollectors_Seq_Fnn_Throwables_ledger_2026-09-24.md}).
 *
 * <ul>
 *   <li><b>C-008</b> - the time-window start was advanced one increment per loop iteration.</li>
 *   <li><b>C-009</b> - a bounded window closed on count reported in-order elements of the next window as late data.</li>
 *   <li><b>C-010</b> - {@code delayForLateData} released results before late data could no longer change them.</li>
 *   <li><b>C-014</b> - {@code transformViaJdkStream}/{@code sjps} did not close the source when the function threw.</li>
 *   <li><b>C-031</b> - {@code parallelZipIterators} read its collection (and {@code zipIterators} its defaults) lazily.</li>
 *   <li><b>C-032</b> - {@code parallelMerge*} of more than three sources returned ties in a timing-dependent order.</li>
 *   <li><b>C-033</b> - the primitive N-ary zips with defaults closed exhausted sources from inside {@code hasNext()}.</li>
 *   <li><b>C-071</b> - the late-data cache was pre-sized to {@code cacheSizeForLateData}.</li>
 *   <li><b>C-073</b> - {@code ofReversed(List)} was O(n^2) on a non-RandomAccess list.</li>
 *   <li>Locks for the documented behaviour behind the doc-only fixes (C-017, C-020, C-028, C-029, C-052, C-063, C-068,
 *       C-070, C-072, C-074, S1-11, S2-16, S3-12).</li>
 * </ul>
 */
@Tag("unit")
public class StreamReview20260924Test extends TestBase {

    // ============================================================== C-014: transformViaJdkStream / sjps close on failure

    @Test
    public void testC014_transformViaJdkStream_closesSourceWhenFunctionThrows() {
        final AtomicInteger closed = new AtomicInteger();
        final Stream<Integer> s = Stream.of(1, 2).onClose(closed::incrementAndGet);

        assertThrows(IllegalStateException.class, () -> s.transformViaJdkStream(js -> {
            throw new IllegalStateException("boom");
        }));
        assertEquals(1, closed.get());
    }

    @Test
    public void testC014_transformViaJdkStream_notDeferred_closesSourceWhenFunctionThrows() {
        final AtomicInteger closed = new AtomicInteger();
        final Stream<Integer> s = Stream.of(1, 2).onClose(closed::incrementAndGet);

        assertThrows(IllegalStateException.class, () -> s.transformViaJdkStream(js -> {
            throw new IllegalStateException("boom");
        }, false));
        assertEquals(1, closed.get());
    }

    @Test
    public void testC014_sjps_closesSourceWhenFunctionThrows_sequentialAndParallel() {
        final AtomicInteger closed = new AtomicInteger();

        assertThrows(IllegalStateException.class, () -> Stream.of(1, 2).onClose(closed::incrementAndGet).sjps(js -> {
            throw new IllegalStateException("boom");
        }));
        assertEquals(1, closed.get());

        assertThrows(IllegalStateException.class, () -> Stream.of(1, 2).onClose(closed::incrementAndGet).parallel(2).sjps(js -> {
            throw new IllegalStateException("boom");
        }));
        assertEquals(2, closed.get());
    }

    @Test
    public void testC014_successPath_resultClosesSourceOnce() {
        final AtomicInteger closed = new AtomicInteger();

        assertEquals(List.of(2, 4), Stream.of(1, 2).onClose(closed::incrementAndGet).transformViaJdkStream(js -> js.map(x -> x * 2)).toList());
        assertEquals(1, closed.get());

        // A function that ignores its input: closing the result still releases the source.
        assertEquals(List.of("x"), Stream.of(1, 2).onClose(closed::incrementAndGet).transformViaJdkStream(js -> java.util.stream.Stream.of("x")).toList());
        assertEquals(2, closed.get());

        assertEquals(List.of(4, 16), Stream.of(1, 2, 3, 4).onClose(closed::incrementAndGet).sjps(js -> js.filter(x -> x % 2 == 0).map(x -> x * x)).toList());
        assertEquals(3, closed.get());

        // Empty source and a null result (treated as empty, unchanged).
        assertEquals(List.of(), Stream.<Integer> empty().transformViaJdkStream(js -> js.map(x -> x)).toList());
        assertEquals(List.of(), Stream.of(1).onClose(closed::incrementAndGet).transformViaJdkStream(js -> null).toList());
        assertEquals(4, closed.get());
    }

    // ============================================================== C-020 / C-029 / C-028: merge-function docs (locks)

    @Test
    public void testC020_distinctMerge_nullResultRemovesGroup_laterDuplicateReinserts() {
        assertEquals(Arrays.asList(1, null), Stream.of(null, 1, null, null).distinct((a, b) -> a).toList());
        assertEquals(Arrays.asList(1), Stream.of(null, 1, null).distinct((a, b) -> a).toList());
        assertEquals(List.of("b", "a"), Stream.of("a", "b", "a", "a").distinct((x, y) -> null).toList());
        assertEquals(List.of("b"), Stream.of("a", "b", "a").distinct((x, y) -> null).toList());

        // distinctBy: a key that occurs three times survives with its last element, in that position.
        assertEquals(List.of("b1", "a3"), Stream.of("a1", "b1", "a2", "a3").distinctBy(s -> s.charAt(0), (x, y) -> null).toList());

        // Arrays keep identity equality in the merge overloads (documented).
        final int[] a1 = { 1 };
        final int[] a2 = { 1 };
        assertEquals(2, Stream.of(a1, a2).distinct((x, y) -> x).count());
    }

    @Test
    public void testC029_toMapMerge_calledWithNullCurrentValue_unlikeMapMerge() {
        final List<Integer> firstArgs = new ArrayList<>();
        final Map<String, Integer> m = Stream.of(1, 2).toMap(x -> "k", x -> x == 1 ? null : x, (a, b) -> {
            firstArgs.add(a);
            return b;
        });

        assertEquals(Collections.singletonList(null), firstArgs);
        assertEquals(Map.of("k", 2), m);

        // A null merge result removes the key.
        assertEquals(Map.of("b", 1), Stream.of("a", "a", "b").toMap(s -> s, s -> 1, (x, y) -> null));
    }

    @Test
    public void testC028_groupByMerge_isToMapBacked_otherGroupByRejectNullKey() {
        final List<Map.Entry<String, Integer>> entries = Stream.of("a", null).groupBy(s -> s, s -> 1, Integer::sum).toList();
        assertEquals(2, entries.size());
        assertTrue(entries.stream().anyMatch(e -> e.getKey() == null && e.getValue() == 1));

        assertEquals(List.of("b"), Stream.of("a", "a", "b").groupBy(s -> s, s -> 1, (x, y) -> null).map(Map.Entry::getKey).toList());

        assertThrows(NullPointerException.class, () -> Stream.of("a", null).groupBy(s -> s).toList());
        assertThrows(NullPointerException.class, () -> Stream.of("a", null).groupTo(s -> s));
        assertThrows(NullPointerException.class, () -> Stream.of("a", null).countBy(s -> s).toList());
        assertThrows(NullPointerException.class, () -> Stream.of("a").flatGroupTo(s -> Arrays.asList(s, null)));
        // A null key collection is treated as empty.
        assertEquals(Map.of(), Stream.of("a").flatGroupTo(s -> (List<String>) null));
    }

    // ============================================================== C-031: snapshots

    private static List<Iterator<Integer>> iterators(final List<?>... lists) {
        final List<Iterator<Integer>> result = new ArrayList<>();

        for (final List<?> l : lists) {
            result.add((Iterator<Integer>) l.iterator());
        }

        return result;
    }

    private static int sum(final List<Integer> l) {
        int s = 0;

        for (final Integer i : l) {
            s += i;
        }

        return s;
    }

    @Test
    public void testC031_parallelZipIterators_clearedCollection_usesSnapshot() {
        final List<Iterator<Integer>> its = iterators(List.of(1, 2), List.of(3, 4));
        final Stream<Integer> s = Stream.parallelZipIterators(its, StreamReview20260924Test::sum, 2);
        its.clear();

        // Before the fix the emptied collection produced an infinite stream of sum([]) == 0.
        assertEquals(List.of(4, 6), s.limit(10).sorted().toList());
    }

    @Test
    public void testC031_parallelZipIterators_laterAddition_isIgnored() {
        final List<Iterator<Integer>> its = iterators(List.of(1, 2), List.of(3, 4));
        final Stream<Integer> s = Stream.parallelZipIterators(its, StreamReview20260924Test::sum, 2);
        its.add(List.of(9).iterator());

        assertEquals(List.of(4, 6), s.sorted().toList());
    }

    @Test
    public void testC031_parallelZipIteratorsWithDefaults_snapshotsCollectionAndDefaults() {
        final List<Iterator<Integer>> its = iterators(List.of(1, 2), List.of(10));
        final List<Integer> defaults = new ArrayList<>(List.of(0, 0));
        final Stream<Integer> s = Stream.parallelZipIterators(its, defaults, StreamReview20260924Test::sum, 2);
        defaults.set(1, 100);
        its.clear();

        assertEquals(List.of(2, 11), s.sorted().toList());

        final List<Integer> defaults2 = new ArrayList<>(List.of(0, 0));
        final Stream<Integer> s2 = Stream.parallelZipIterators(iterators(List.of(1, 2), List.of(10)), defaults2, StreamReview20260924Test::sum, 2);
        defaults2.clear(); // was IndexOutOfBoundsException on traversal

        assertEquals(List.of(2, 11), s2.sorted().toList());
    }

    @Test
    public void testC031_zipIteratorsWithDefaults_snapshotsDefaults() {
        final List<Integer> defaults = new ArrayList<>(List.of(0, 0));
        final Stream<Integer> s = Stream.zipIterators(iterators(List.of(1, 2), List.of(10)), defaults, StreamReview20260924Test::sum);
        defaults.set(1, 100);

        assertEquals(List.of(11, 2), s.toList());

        // Stream-collection variant goes through zipIterators too.
        final List<Integer> defaults3 = new ArrayList<>(List.of(-1, -2));
        final Stream<Integer> s3 = Stream.zip(List.of(Stream.of(1, 2), Stream.of(10)), defaults3, StreamReview20260924Test::sum);
        defaults3.set(1, 100);
        assertEquals(List.of(11, 0), s3.toList());
    }

    @Test
    public void testC031_emptyAndNullInputs() {
        assertEquals(List.of(), Stream.parallelZipIterators((List<Iterator<Integer>>) null, StreamReview20260924Test::sum, 2).toList());
        assertEquals(List.of(), Stream.parallelZipIterators(new ArrayList<Iterator<Integer>>(), new ArrayList<Integer>(), StreamReview20260924Test::sum, 2)
                .toList());
        assertThrows(IllegalArgumentException.class, () -> Stream.parallelZipIterators(iterators(List.of(1)), List.of(), StreamReview20260924Test::sum, 2));

        // A null iterator is treated as empty.
        final List<Iterator<Integer>> withNull = iterators(List.of(1, 2));
        withNull.add(null);
        assertEquals(List.of(), Stream.parallelZipIterators(withNull, StreamReview20260924Test::sum, 2).toList());
        assertEquals(List.of(1, 2), Stream.parallelZipIterators(withNull, Arrays.asList(0, 0), StreamReview20260924Test::sum, 2).sorted().toList());
    }

    // ============================================================== C-032: parallelMerge tie order

    private static final BiFunction<String, String, MergeResult> BY_FIRST_CHAR = (x, y) -> x.charAt(0) <= y.charAt(0) ? MergeResult.TAKE_FIRST
            : MergeResult.TAKE_SECOND;

    @Test
    public void testC032_parallelMergeIterables_tiesKeepSourceOrder() {
        final List<List<String>> sources = new ArrayList<>();

        for (int i = 1; i <= 8; i++) {
            sources.add(List.of("a" + i));
        }

        final List<String> expected = Stream.mergeIterables(sources, BY_FIRST_CHAR).toList();
        assertEquals(List.of("a1", "a2", "a3", "a4", "a5", "a6", "a7", "a8"), expected);

        for (int run = 0; run < 40; run++) {
            assertEquals(expected, Stream.parallelMergeIterables(sources, BY_FIRST_CHAR, 4).toList(), "run " + run);
        }
    }

    @Test
    public void testC032_parallelMergeStreams_tiesKeepSourceOrder() {
        final Supplier<List<Stream<String>>> sources = () -> List.of(Stream.of("a1", "b1"), Stream.of("a2", "b2"), Stream.of("a3", "b3"),
                Stream.of("a4", "b4"));
        final List<String> expected = List.of("a1", "a2", "a3", "a4", "b1", "b2", "b3", "b4");

        assertEquals(expected, Stream.merge(sources.get(), BY_FIRST_CHAR).toList());

        for (int run = 0; run < 40; run++) {
            assertEquals(expected, Stream.parallelMerge(sources.get(), BY_FIRST_CHAR, 4).toList(), "run " + run);
        }
    }

    @Test
    public void testC032_parallelMerge_matchesSequentialMerge_forManySourceCounts() {
        final Random rnd = new Random(20260924);
        final BiFunction<int[], int[], MergeResult> byValue = (x, y) -> x[0] <= y[0] ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND;

        for (int n = 0; n <= 13; n++) {
            final List<List<int[]>> sources = new ArrayList<>();

            for (int i = 0; i < n; i++) {
                final int len = rnd.nextInt(6);
                final List<int[]> l = new ArrayList<>();
                int v = 0;

                for (int j = 0; j < len; j++) {
                    v += rnd.nextInt(3); // ties are frequent
                    l.add(new int[] { v, i, j });
                }

                sources.add(l);
            }

            final List<String> expected = Stream.mergeIterables(sources, byValue).map(Arrays::toString).toList();

            for (final int threads : new int[] { 2, 3, 8 }) {
                assertEquals(expected, Stream.parallelMergeIterables(sources, byValue, threads).map(Arrays::toString).toList(), n + "/" + threads);

                final List<Iterator<int[]>> iters = new ArrayList<>();
                sources.forEach(l -> iters.add(l.iterator()));
                assertEquals(expected, Stream.parallelMergeIterators(iters, byValue, threads).map(Arrays::toString).toList(), n + "/" + threads);

                final List<Stream<int[]>> streams = new ArrayList<>();
                sources.forEach(l -> streams.add(Stream.of(l)));
                assertEquals(expected, Stream.parallelMerge(streams, byValue, threads).map(Arrays::toString).toList(), n + "/" + threads);
            }
        }
    }

    @Test
    public void testC032_parallelMerge_selectorFailure_propagatesAndClosesSources() {
        final AtomicInteger closed = new AtomicInteger();
        final List<Stream<Integer>> streams = new ArrayList<>();

        for (int i = 0; i < 8; i++) {
            streams.add(Stream.of(i, i + 10).onClose(closed::incrementAndGet));
        }

        final BiFunction<Integer, Integer, MergeResult> failing = (x, y) -> {
            throw new IllegalStateException("selector");
        };

        final IllegalStateException e = assertThrows(IllegalStateException.class, () -> Stream.parallelMerge(streams, failing, 4).toList());
        assertEquals("selector", e.getMessage());
        assertEquals(8, closed.get());
    }

    // ============================================================== C-033: primitive N-ary zip with defaults

    private static void assertNoEarlyClose(final Stream<?> zipped, final List<String> closed) {
        final ObjIterator<?> it = zipped.iterator();
        it.next();
        it.hasNext(); // the first source is exhausted now

        assertEquals(List.of(), closed, "a source was closed during iteration");

        while (it.hasNext()) {
            it.next();
        }

        assertEquals(List.of(), closed, "a source was closed during iteration");
        zipped.close();
        assertEquals(List.of("a", "b"), closed.stream().sorted().toList());
    }

    @Test
    public void testC033_primitiveZipWithDefaults_closesSourcesOnlyWithTheResult() {
        List<String> closed = new ArrayList<>();
        final List<String> c1 = closed;
        assertNoEarlyClose(Stream.zip(List.of(IntStream.of(1).onClose(() -> c1.add("a")), IntStream.of(1, 2, 3).onClose(() -> c1.add("b"))),
                new int[] { 0, 0 }, args -> args[0] + args[1]), closed);

        closed = new ArrayList<>();
        final List<String> c2 = closed;
        assertNoEarlyClose(Stream.zip(List.of(CharStream.of('x').onClose(() -> c2.add("a")), CharStream.of('y', 'z').onClose(() -> c2.add("b"))),
                new char[] { '-', '-' }, args -> "" + args[0] + args[1]), closed);

        closed = new ArrayList<>();
        final List<String> c3 = closed;
        assertNoEarlyClose(Stream.zip(List.of(ByteStream.of((byte) 1).onClose(() -> c3.add("a")), ByteStream.of((byte) 1, (byte) 2).onClose(() -> c3.add("b"))),
                new byte[] { 0, 0 }, args -> args[0] + args[1]), closed);

        closed = new ArrayList<>();
        final List<String> c4 = closed;
        assertNoEarlyClose(
                Stream.zip(List.of(ShortStream.of((short) 1).onClose(() -> c4.add("a")), ShortStream.of((short) 1, (short) 2).onClose(() -> c4.add("b"))),
                        new short[] { 0, 0 }, args -> args[0] + args[1]),
                closed);

        closed = new ArrayList<>();
        final List<String> c5 = closed;
        assertNoEarlyClose(Stream.zip(List.of(LongStream.of(1).onClose(() -> c5.add("a")), LongStream.of(1, 2).onClose(() -> c5.add("b"))), new long[] { 0, 0 },
                args -> args[0] + args[1]), closed);

        closed = new ArrayList<>();
        final List<String> c6 = closed;
        assertNoEarlyClose(Stream.zip(List.of(FloatStream.of(1).onClose(() -> c6.add("a")), FloatStream.of(1, 2).onClose(() -> c6.add("b"))), new float[] { 0, 0 },
                args -> args[0] + args[1]), closed);

        closed = new ArrayList<>();
        final List<String> c7 = closed;
        assertNoEarlyClose(Stream.zip(List.of(DoubleStream.of(1).onClose(() -> c7.add("a")), DoubleStream.of(1, 2).onClose(() -> c7.add("b"))),
                new double[] { 0, 0 }, args -> args[0] + args[1]), closed);
    }

    @Test
    public void testC033_primitiveZipWithDefaults_values() {
        assertEquals(List.of(2, 2, 3), Stream.zip(List.of(IntStream.of(1), IntStream.of(1, 2, 3)), new int[] { 0, 0 }, args -> args[0] + args[1]).toList());
        assertEquals(List.of("xy", "-z"),
                Stream.zip(List.of(CharStream.of('x'), CharStream.of('y', 'z')), new char[] { '-', '-' }, args -> "" + args[0] + args[1]).toList());
        assertEquals(List.of(), Stream.zip(new ArrayList<IntStream>(), new int[0], args -> args.length).toList());
    }

    // ============================================================== C-052 / C-017 / C-063 / S1-11: documented behaviour (locks)

    @Test
    public void testC052_intersectionAndDifference_stopMappingOnceNothingIsLeftToMatch() {
        final AtomicInteger calls = new AtomicInteger();

        assertEquals(List.of(1), Stream.of(1, 2, 3, 4, 5).intersection(x -> {
            calls.incrementAndGet();
            return x;
        }, List.of(1)).toList());
        assertEquals(1, calls.get());

        calls.set(0);
        assertEquals(List.of(2, 3, 4, 5), Stream.of(1, 2, 3, 4, 5).difference(x -> {
            calls.incrementAndGet();
            return x;
        }, List.of(1)).toList());
        assertEquals(1, calls.get());

        calls.set(0);
        assertEquals(List.of(), Stream.of(1, 2).intersection(x -> calls.incrementAndGet(), List.of()).toList());
        assertEquals(0, calls.get());
    }

    @Test
    public void testC017_max_tiesReturnFirstMaximalElement() {
        final Comparator<String> byLen = Comparator.comparingInt(String::length);

        assertEquals("banana", Stream.of("apple", "banana", "cherry").max(byLen).get());
        assertEquals("banana", Stream.of("apple", "banana", "cherry").maxBy(String::length).get());
        assertEquals("banana", Stream.of(List.of("apple", "banana", "cherry").iterator()).max(byLen).get());
        assertEquals(Optional.empty(), Stream.<String> empty().max(byLen));
    }

    @Test
    public void testC063_mapPartial_nullOptional_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> Stream.of("a").mapPartial(s -> (Optional<String>) null).toList());
        assertThrows(NullPointerException.class, () -> Stream.of("a").mapPartialJdk(s -> (java.util.Optional<String>) null).toList());
    }

    @Test
    public void testS111_top_nullsAreSmallest() {
        assertEquals(List.of(3, 5), Stream.of(5, null, 3).top(2).sorted().toList());
        assertEquals(List.of(3, 5), Stream.of(List.of(5, 3).iterator()).top(2).sorted().toList());
        assertEquals(3, Stream.of(5, null, 3).top(3).count());
        assertTrue(Stream.of(5, null, 3).top(3).anyMatch(x -> x == null));
    }

    // ============================================================== C-068: collect(Supplier, BiConsumer) in parallel

    @Test
    public void testC068_parallelCollect_unsupportedContainer_failsOnlyWhenContainersAreCombined() {
        // One element: only one container is created, so nothing has to be combined.
        assertEquals(5, Stream.of(5).parallel(2).collect(() -> new int[1], (a, x) -> a[0] += x)[0]);

        final AtomicInteger accumulated = new AtomicInteger();
        final RuntimeException e = assertThrows(IllegalArgumentException.class, () -> Stream.range(0, 1000).parallel(4).collect(() -> new int[1], (a, x) -> {
            accumulated.incrementAndGet();
            a[0] += x;
        }));

        assertTrue(e.getMessage().contains("cannot be combined"), e.getMessage());
        assertEquals(1000, accumulated.get()); // the failure comes after the accumulation

        // Sequential streams never combine.
        assertEquals(499500, Stream.range(0, 1000).collect(() -> new int[1], (a, x) -> a[0] += x)[0]);
    }

    // ============================================================== C-070: abacus Duration rules

    @Test
    public void testC070_durationMustBePositive_noArithmeticException() {
        assertEquals(List.of(1, 2, 3), Stream.of(1, 2, 3).maxWait(Duration.ofMillis(Long.MAX_VALUE), 0).toList());
        // A huge duration is accepted; only a boundary that leaves the long range fails, and that is on traversal.
        assertEquals(List.of(List.of(1, 2, 3)), Stream.of(1, 2, 3).window(Duration.ofMillis(Long.MAX_VALUE / 4), () -> 0L, Collectors.toList()).limit(1).toList());

        assertThrows(IllegalArgumentException.class, () -> Stream.of(1).window(Duration.ofMillis(0)));
        assertThrows(IllegalArgumentException.class, () -> Stream.of(1).window(Duration.ofMillis(-1)));
        assertThrows(IllegalArgumentException.class, () -> Stream.of(1).maxWait(Duration.ofMillis(0), 0));
        assertThrows(IllegalArgumentException.class, () -> Stream.of(1).window(Duration.ofMillis(10), Duration.ofMillis(0)));
    }

    // ============================================================== C-073: ofReversed(List)

    private static final class CountingLinkedList<E> extends LinkedList<E> {
        private static final long serialVersionUID = 1L;
        int getCalls = 0;

        CountingLinkedList(final List<E> c) {
            super(c);
        }

        @Override
        public E get(final int index) {
            getCalls++;
            return super.get(index);
        }
    }

    @Test
    public void testC073_ofReversed_nonRandomAccessList_walksBackwards() {
        final CountingLinkedList<Integer> list = new CountingLinkedList<>(List.of(1, 2, 3, 4, 5));

        assertEquals(List.of(5, 4, 3, 2, 1), Stream.ofReversed(list).toList());
        assertEquals(0, list.getCalls, "get(i) is O(n) on a LinkedList");

        assertEquals(List.of(3, 2), Stream.ofReversed(list).skip(2).limit(2).toList());
        assertEquals(5, Stream.ofReversed(list).count());
    }

    @Test
    public void testC073_ofReversed_edgeCases() {
        assertEquals(List.of(), Stream.ofReversed((List<Integer>) null).toList());
        assertEquals(List.of(), Stream.ofReversed(new LinkedList<Integer>()).toList());
        assertEquals(List.of("😀"), Stream.ofReversed(new LinkedList<>(List.of("😀"))).toList());
        assertEquals(Arrays.asList(null, "b", null), Stream.ofReversed(new LinkedList<>(Arrays.asList(null, "b", null))).toList());
        assertEquals(List.of(3, 2, 1), Stream.ofReversed(new ArrayList<>(List.of(1, 2, 3))).toList());

        final LinkedList<Integer> big = new LinkedList<>();

        for (int i = 0; i < 20_000; i++) {
            big.add(i);
        }

        final List<Integer> reversed = Stream.ofReversed(big).toList();
        assertEquals(20_000, reversed.size());
        assertEquals(19_999, reversed.get(0));
        assertEquals(0, reversed.get(19_999));
    }

    // ============================================================== C-074: splitToLines

    @Test
    public void testC074_splitToLines_terminatorsAndEdges() {
        assertEquals(List.of("a", "b", "c", "d", "e", "f", "g", "h", "i"),
                Stream.splitToLines("a\u000Bb\u000Cc\u0085d e f\r\ng\rh\ni").toList());
        assertEquals(List.of("a", "", "b"), Stream.splitToLines("a\r\n\nb").toList());
        assertEquals(List.of(), Stream.splitToLines(null).toList());
        assertEquals(List.of(""), Stream.splitToLines("").toList());
        assertEquals(List.of("a", "b", ""), Stream.splitToLines("a\nb\n").toList());

        assertEquals(List.of(), Stream.splitToLines(null, true, true).toList());
        assertEquals(List.of(), Stream.splitToLines("", false, true).toList());
        assertEquals(List.of("a", "b"), Stream.splitToLines(" a \n\n b \n", true, true).toList());
        assertEquals(List.of("a", "", "b", ""), Stream.splitToLines(" a \n\n b \n", true, false).toList());
    }

    // ============================================================== C-008: O(1) window catch-up

    @Test
    public void testC008_epochAlignedStart_isFastAndKeepsSimultaneousElementsTogether() {
        // Warm up the window machinery so that class loading does not count against the time limit below.
        Stream.of(0).window(Duration.ofMillis(1000), System::currentTimeMillis, Collectors.toList()).toList();

        long start = System.currentTimeMillis();
        assertEquals(List.of(List.of(1, 2, 3)), Stream.of(1, 2, 3).window(Duration.ofMillis(1000), () -> 0L, Collectors.toList()).toList());
        assertTrue(System.currentTimeMillis() - start < 2000, (System.currentTimeMillis() - start) + " ms");

        start = System.currentTimeMillis();
        assertEquals(List.of(List.of(1, 2, 3)), Stream.of(1, 2, 3).window(Duration.ofMillis(100), () -> 0L, Collectors.toList()).toList());
        assertTrue(System.currentTimeMillis() - start < 2000, (System.currentTimeMillis() - start) + " ms");

        start = System.currentTimeMillis();
        assertEquals(List.of(List.of(1, 2, 3)), Stream.of(1, 2, 3).window(Duration.ofMillis(1000), 10, () -> 0L, Collectors.toList()).toList());
        assertTrue(System.currentTimeMillis() - start < 2000, (System.currentTimeMillis() - start) + " ms");
    }

    @Test
    public void testC008_eventTime_longGaps_smallWindows() {
        final long day = 86_400_000L;
        final List<Long> evs = List.of(0L, 1L, 10 * day, 10 * day + 1, 20 * day);
        final WindowHandler<Long, List<Long>> handler = WindowHandler.<Long, List<Long>> builder().timeExtractor(Long::longValue).build();

        final long start = System.currentTimeMillis();
        final List<List<Long>> windows = Stream.of(evs).window(Duration.ofMillis(2), Duration.ofMillis(2), () -> 0L, handler, Collectors.toList()).toList();
        assertEquals(List.of(List.of(0L, 1L), List.of(10 * day, 10 * day + 1), List.of(20 * day)), windows);

        final List<List<Long>> bounded = Stream.of(evs).window(Duration.ofMillis(2), 10, () -> 0L, handler, Collectors.toList()).toList();
        assertEquals(List.of(List.of(0L, 1L), List.of(10 * day, 10 * day + 1), List.of(20 * day)), bounded);
        assertTrue(System.currentTimeMillis() - start < 2000, (System.currentTimeMillis() - start) + " ms");
    }

    private static long loopWindowStart(long fromTime, long endTime, final long time, final long increment, final long duration) {
        do {
            fromTime = Math.addExact(fromTime, increment);
            endTime = Math.addExact(fromTime, duration);
        } while (time >= endTime);

        return fromTime;
    }

    @Test
    public void testC008_advanceWindowStart_matchesTheLoop() throws Exception {
        final Method m = Stream.class.getDeclaredMethod("advanceWindowStart", long.class, long.class, long.class, long.class);
        m.setAccessible(true);

        final Random rnd = new Random(7);
        int checked = 0;

        for (int i = 0; i < 100_000; i++) {
            final long duration = 1 + (rnd.nextBoolean() ? rnd.nextInt(50) : (rnd.nextLong() >>> (1 + rnd.nextInt(62))));
            final long increment = 1 + (rnd.nextBoolean() ? rnd.nextInt(50) : (rnd.nextLong() >>> (1 + rnd.nextInt(62))));
            final long fromTime;

            switch (rnd.nextInt(3)) {
                case 0:
                    fromTime = Long.MIN_VALUE + rnd.nextInt(1000);
                    break;
                case 1:
                    fromTime = Long.MAX_VALUE - duration - rnd.nextInt(1000);
                    break;
                default:
                    fromTime = rnd.nextLong() >> rnd.nextInt(63);
            }

            final long endTime;

            try {
                endTime = Math.addExact(fromTime, duration);
            } catch (final ArithmeticException e) {
                continue;
            }

            final long gap = rnd.nextInt(4) == 0 ? rnd.nextInt(3 * (int) Math.min(increment, 1000)) : rnd.nextLong() >>> rnd.nextInt(64);
            final long time = endTime + gap;

            if (time < endTime) {
                continue; // overflowed
            }

            // Only compare against the loop when it terminates quickly.
            if (Long.compareUnsigned(Long.divideUnsigned(time - endTime, increment), 1_000) > 0) {
                continue;
            }

            Long expected;

            try {
                expected = loopWindowStart(fromTime, endTime, time, increment, duration);
                Math.addExact(expected, duration);
            } catch (final ArithmeticException e) {
                expected = null;
            }

            Long actual;

            try {
                actual = (Long) m.invoke(null, fromTime, endTime, time, increment);
                Math.addExact(actual, duration);
            } catch (final InvocationTargetException e) {
                assertTrue(e.getCause() instanceof ArithmeticException, String.valueOf(e.getCause()));
                actual = null;
            } catch (final ArithmeticException e) {
                actual = null;
            }

            assertEquals(expected, actual, fromTime + "/" + endTime + "/" + time + "/" + increment);
            checked++;
        }

        assertTrue(checked > 20_000, "checked " + checked);

        // Far past starts are O(1) now.
        // (the loop would take 2^64 - 11 iterations here).
        assertEquals(Long.MAX_VALUE - 10, (long) (Long) m.invoke(null, Long.MIN_VALUE, Long.MIN_VALUE + 10, Long.MAX_VALUE - 1, 1L));
    }

    // ============================================================== C-009: bounded window + late data

    private static WindowHandler<Long, List<Long>> addingLateHandler(final List<String> late) {
        return WindowHandler.<Long, List<Long>> builder().timeExtractor(Long::longValue).onLateData((start, end, t, e, result) -> {
            late.add(e + "->[" + start + "," + end + ")");
            result.add(e);
        }).build();
    }

    @Test
    public void testC009_boundedWindow_countClose_inOrderElementsAreNotLate() {
        final List<String> late = new ArrayList<>();

        assertEquals(List.of(List.of(1L, 2L), List.of(3L, 4L), List.of(12L)),
                Stream.of(1L, 2L, 3L, 4L, 12L).window(Duration.ofMillis(10), 2, () -> 0L, addingLateHandler(late), Collectors.toList()).toList());
        assertEquals(List.of(), late);

        assertEquals(List.of(List.of(1L, 2L), List.of(3L, 4L), List.of(5L, 6L)),
                Stream.of(1L, 2L, 3L, 4L, 5L, 6L).window(Duration.ofMillis(10), 2, () -> 0L, addingLateHandler(late), Collectors.toList()).toList());
        assertEquals(List.of(), late);

        // Iterator (async) source.
        assertEquals(List.of(List.of(1L, 2L), List.of(3L, 4L), List.of(12L)), Stream.of(List.of(1L, 2L, 3L, 4L, 12L).iterator())
                .window(Duration.ofMillis(10), 2, () -> 0L, addingLateHandler(late), Collectors.toList())
                .toList());
        assertEquals(List.of(), late);
    }

    @Test
    public void testC009_boundedWindow_processingTime_neverLate() {
        final AtomicInteger late = new AtomicInteger();
        final WindowHandler<Integer, List<Integer>> handler = WindowHandler.<Integer, List<Integer>> builder()
                .onLateData((e, result) -> late.incrementAndGet())
                .build();

        final List<List<Integer>> windows = Stream.of(1, 2, 3, 4, 5, 6)
                .window(Duration.ofSeconds(10), 2, System::currentTimeMillis, handler, Collectors.toList())
                .toList();

        assertEquals(List.of(List.of(1, 2), List.of(3, 4), List.of(5, 6)), windows);
        assertEquals(0, late.get());
    }

    @Test
    public void testC009_boundedWindow_trulyLateElement_isStillReportedOnce() {
        final List<String> late = new ArrayList<>();

        // 1 and 5 close the first window on count; 3 is before the next window's start (5).
        assertEquals(List.of(List.of(1L, 5L, 3L)),
                Stream.of(1L, 5L, 3L).window(Duration.ofMillis(10), 2, () -> 0L, addingLateHandler(late), Collectors.toList()).toList());
        assertEquals(List.of("3->[0,10)"), late);

        late.clear();

        // 4 is late for the second window only (it was also dispatched to the first one before the fix).
        assertEquals(List.of(List.of(1L, 2L), List.of(3L, 4L), List.of(15L, 16L)),
                Stream.of(1L, 2L, 3L, 15L, 4L, 16L).window(Duration.ofMillis(10), 2, () -> 0L, addingLateHandler(late), Collectors.toList()).toList());
        assertEquals(List.of("4->[2,12)"), late);
    }

    @Test
    public void testC009_slidingWindow_lateDataUnchanged() {
        final List<String> late = new ArrayList<>();

        assertEquals(List.of(List.of(1L, 2L, 3L, 4L), List.of(12L)),
                Stream.of(1L, 2L, 3L, 4L, 12L).window(Duration.ofMillis(10), Duration.ofMillis(10), () -> 0L, addingLateHandler(late), Collectors.toList())
                        .toList());
        assertEquals(List.of(), late);

        assertEquals(List.of(List.of(1L, 5L), List.of(12L)),
                Stream.of(1L, 12L, 5L).window(Duration.ofMillis(10), Duration.ofMillis(10), () -> 0L, addingLateHandler(late), Collectors.toList())
                        .toList());
        assertEquals(List.of("5->[0,10)"), late);
    }

    // ============================================================== C-010: delayForLateData

    private static WindowHandler<Long, List<Long>> delayingHandler(final int cacheSize) {
        return WindowHandler.<Long, List<Long>> builder()
                .timeExtractor(Long::longValue)
                .cacheSizeForLateData(cacheSize)
                .delayForLateData(true)
                .onLateData((e, result) -> result.add(e))
                .build();
    }

    private static final List<Long> LATE_INPUT = List.of(1L, 11L, 21L, 15L, 31L, 25L, 41L, 35L, 51L);
    private static final List<List<Long>> LATE_FINAL = List.of(List.of(1L), List.of(11L, 15L), List.of(21L, 25L), List.of(31L, 35L), List.of(41L),
            List.of(51L));

    @Test
    public void testC010_delayForLateData_consumerSeesLateUpdates_slidingCore() {
        for (final int cacheSize : new int[] { 1, 2, 3, 100 }) {
            final List<List<Long>> seen = Stream.of(LATE_INPUT)
                    .window(Duration.ofMillis(10), Duration.ofMillis(10), () -> 0L, delayingHandler(cacheSize), Collectors.toList())
                    .map(ArrayList::new) // snapshot at emission
                    .map(l -> (List<Long>) l)
                    .toList();

            assertEquals(LATE_FINAL, seen, "cacheSize " + cacheSize);
        }
    }

    @Test
    public void testC010_delayForLateData_consumerSeesLateUpdates_boundedCore() {
        for (final int cacheSize : new int[] { 1, 2, 5 }) {
            final List<List<Long>> seen = Stream.of(LATE_INPUT)
                    .window(Duration.ofMillis(10), 100, () -> 0L, delayingHandler(cacheSize), Collectors.toList())
                    .map(ArrayList::new)
                    .map(l -> (List<Long>) l)
                    .toList();

            assertEquals(LATE_FINAL, seen, "cacheSize " + cacheSize);
        }
    }

    @Test
    public void testC010_delayForLateData_emptyAndNullResults() {
        assertEquals(List.of(), Stream.<Long> empty().window(Duration.ofMillis(10), Duration.ofMillis(10), () -> 0L, delayingHandler(2), Collectors.toList())
                .toList());

        final WindowHandler<Long, Long> nullResults = WindowHandler.<Long, Long> builder()
                .timeExtractor(Long::longValue)
                .cacheSizeForLateData(1)
                .delayForLateData(true)
                .onLateData((e, result) -> {
                })
                .build();

        final java.util.stream.Collector<Long, ?, Long> alwaysNull = java.util.stream.Collector.of(() -> new long[1], (a, x) -> a[0] += x, (a, b) -> a,
                a -> null);
        assertEquals(Arrays.asList(null, null, null),
                Stream.of(1L, 11L, 21L).window(Duration.ofMillis(10), Duration.ofMillis(10), () -> 0L, nullResults, alwaysNull).toList());
    }

    @Test
    public void testC010_delayForLateData_closeReleasesSource() {
        final AtomicInteger closed = new AtomicInteger();
        final Stream<List<Long>> s = Stream.of(LATE_INPUT)
                .onClose(closed::incrementAndGet)
                .window(Duration.ofMillis(10), Duration.ofMillis(10), () -> 0L, delayingHandler(2), Collectors.toList());

        assertEquals(List.of(1L), s.first().get());
        assertEquals(1, closed.get());
    }

    // ============================================================== C-071: no pre-sizing of the late-data cache

    @Test
    public void testC071_hugeCacheSize_doesNotPreallocate() {
        final WindowHandler<Long, List<Long>> handler = WindowHandler.<Long, List<Long>> builder()
                .timeExtractor(Long::longValue)
                .cacheSizeForLateData(Integer.MAX_VALUE)
                .onLateData((e, result) -> result.add(e))
                .build();

        assertEquals(List.of(List.of(1L, 2L, 5L), List.of(11L)),
                Stream.of(1L, 2L, 11L, 5L).window(Duration.ofMillis(10), Duration.ofMillis(10), () -> 0L, handler, Collectors.toList()).toList());
        assertEquals(List.of(List.of(1L, 2L, 5L), List.of(11L)),
                Stream.of(1L, 2L, 11L, 5L).window(Duration.ofMillis(10), 100, () -> 0L, handler, Collectors.toList()).toList());

        final WindowHandler<Long, List<Long>> delaying = WindowHandler.<Long, List<Long>> builder()
                .timeExtractor(Long::longValue)
                .cacheSizeForLateData(Integer.MAX_VALUE)
                .delayForLateData(true)
                .onLateData((e, result) -> result.add(e))
                .build();

        assertEquals(List.of(List.of(1L, 2L, 5L), List.of(11L)), Stream.of(1L, 2L, 11L, 5L)
                .window(Duration.ofMillis(10), Duration.ofMillis(10), () -> 0L, delaying, Collectors.toList())
                .map(ArrayList::new)
                .map(l -> (List<Long>) l)
                .toList());
    }

    // ============================================================== C-072: maxWait window on array sources (doc lock)

    @Test
    public void testC072_arraySource_maxWaitFunctionNotInvoked() {
        final AtomicInteger calls = new AtomicInteger();

        final List<List<Integer>> windows = Stream.of(1, 2, 3, 4).window((first, current, count) -> {
            calls.incrementAndGet();
            return 1000L;
        }, (first, current, next, count) -> count < 2).toList();

        assertEquals(List.of(List.of(1, 2), List.of(3, 4)), windows);
        assertEquals(0, calls.get());
    }

    // ============================================================== S2-16 / S3-12: documented exceptions (locks)

    @Test
    public void testS216_persistToCsv_collectionRowsWithoutHeaders_throwIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> Stream.of(List.of(1, 2)).persistToCsv(new StringWriter()));
    }

    @Test
    public void testS312_ofLines_lazyOpen_andNullCharsetIsUtf8() throws IOException {
        final File missing = new File("target/StreamReview20260924Test-does-not-exist-" + System.nanoTime() + ".txt");
        final Stream<String> lines = Stream.ofLines(missing); // no exception yet
        assertThrows(UncheckedIOException.class, lines::toList);

        final File f = Files.createTempFile("StreamReview20260924Test", ".txt").toFile();

        try {
            Files.write(f.toPath(), "café\n中文".getBytes(StandardCharsets.UTF_8));
            assertEquals(List.of("café", "中文"), Stream.ofLines(f, null).toList());
            assertEquals(List.of("café", "中文"), Stream.ofLines(f.toPath(), null).toList());
        } finally {
            f.delete();
        }
    }

    // ============================================================== S2-10 / S3-07 / S2-09

    @Test
    public void testS210_chunkedSps_zeroMaxThreadNumSelectsDefault() {
        assertEquals(List.of(2, 4, 6), Stream.of(1, 2, 3).spsMap(0, 1, x -> x * 2).sorted().toList());
        assertEquals(List.of(2), Stream.of(1, 2, 3).spsFilter(0, 2, x -> x == 2).toList());
        assertEquals(List.of(1, 1, 2, 2), Stream.of(1, 2).spsFlatMap(0, 1, x -> Stream.of(x, x)).sorted().toList());
        assertEquals(List.of(1, 1, 2, 2), Stream.of(1, 2).spsFlatmap(0, 1, x -> List.of(x, x)).sorted().toList());
        assertEquals(List.of(1, 2, 3), Stream.of(1, 2, 3).sps(0, 2, chunk -> Stream.of(chunk)).sorted().toList());
        // A null stream returned for a chunk is treated as empty.
        assertEquals(List.of(), Stream.of(1, 2, 3).sps(2, 2, chunk -> (Stream<Integer>) null).toList());

        assertThrows(IllegalArgumentException.class, () -> Stream.of(1).spsMap(-1, 1, x -> x));
        assertThrows(IllegalArgumentException.class, () -> Stream.of(1).spsMap(1, 0, x -> x));
    }

    private static final class CountingList<E> extends java.util.AbstractList<E> {
        private final List<E> delegate;
        int getCalls = 0;

        CountingList(final List<E> delegate) {
            this.delegate = delegate;
        }

        @Override
        public E get(final int index) {
            getCalls++;
            return delegate.get(index);
        }

        @Override
        public int size() {
            return delegate.size();
        }
    }

    @Test
    public void testS307_ofCollectionRange_listIteratesOnlyTheRange() {
        final List<Integer> data = new ArrayList<>();

        for (int i = 0; i < 1000; i++) {
            data.add(i);
        }

        final CountingList<Integer> list = new CountingList<>(data);

        assertEquals(List.of(990, 991), Stream.of(list, 990, 992).toList());
        assertEquals(2, list.getCalls);

        assertEquals(2, Stream.of(list, 990, 992).count());
        assertEquals(List.of(), Stream.of(list, 5, 5).toList());
        assertEquals(List.of(3, 4), Stream.of(new LinkedList<>(List.of(1, 2, 3, 4)), 2, 4).toList());
        // Non-list collections still work (walk and skip).
        assertEquals(List.of(2, 3), Stream.of(new java.util.LinkedHashSet<>(List.of(1, 2, 3, 4)), 1, 3).toList());
        assertThrows(IndexOutOfBoundsException.class, () -> Stream.of(list, 5, 1001));
    }

    @Test
    public void testS209_mergeWithNull_isEmpty_cartesianProductNullFactor_isEmpty() {
        final BiFunction<Integer, Integer, MergeResult> sel = (a, b) -> a <= b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND;

        assertEquals(List.of(1, 2), Stream.of(1, 2).mergeWith((Stream<Integer>) null, sel).toList());
        assertEquals(List.of(1, 2), Stream.of(1, 2).mergeWith((List<Integer>) null, sel).toList());
        assertEquals(List.of(), Stream.of(1, 2).cartesianProduct(Arrays.asList(List.of(3), null)).toList());
        assertThrows(IllegalArgumentException.class, () -> Stream.of(1).zipWith((Stream<Integer>) null, (a, b) -> a));
    }
}

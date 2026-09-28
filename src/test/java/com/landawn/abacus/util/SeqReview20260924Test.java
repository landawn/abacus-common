package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.IntFunction;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

/**
 * Regression tests for the 2026-09-24 stream/function review, Seq side (findings C-002, C-015, C-016, C-017, C-018,
 * C-019, C-020, C-038, C-050, C-051, C-053 and the Q1/Q2/Q3 nits).
 */
public class SeqReview20260924Test extends TestBase {

    private static <T> Seq<T, Exception> tracked(final AtomicInteger closeCount, final T... elements) {
        return Seq.<T, Exception> of(elements).onClose(closeCount::incrementAndGet);
    }

    // ------------------------------------------------------------------------------------------------------
    // C-015 transform: a closed result must not strand the receiver
    // ------------------------------------------------------------------------------------------------------

    @Test
    public void testTransform_closedResult_closesReceiver() {
        final AtomicInteger closed = new AtomicInteger();
        final Seq<Integer, Exception> src = tracked(closed, 1, 2);

        assertThrows(IllegalStateException.class, () -> src.transform(s -> {
            final Seq<Integer, Exception> other = Seq.of(9);
            other.close();
            return other;
        }));

        assertEquals(1, closed.get());
        assertThrows(IllegalStateException.class, src::toList);
    }

    @Test
    public void testTransform_openResult_closesReceiverExactlyOnce() throws Exception {
        final AtomicInteger closed = new AtomicInteger();
        final Seq<Integer, Exception> src = tracked(closed, 1, 2, 3);

        assertEquals(Arrays.asList(2, 4, 6), src.transform(s -> s.map(x -> x * 2)).toList());
        assertEquals(1, closed.get());

        final AtomicInteger closed2 = new AtomicInteger();
        assertEquals(Collections.emptyList(), tracked(closed2, 1).transform(s -> null).toList());
        assertEquals(1, closed2.get());
    }

    // ------------------------------------------------------------------------------------------------------
    // C-016 a factory/supplier that returns null -> NullPointerException, sequence closed
    // ------------------------------------------------------------------------------------------------------

    @Test
    public void testToCollection_supplierReturnsNull_nonEmpty() {
        final AtomicInteger closed = new AtomicInteger();
        final NullPointerException e = assertThrows(NullPointerException.class,
                () -> tracked(closed, "a", "b").toCollection(() -> (List<String>) null));
        assertEquals("supplier returned null", e.getMessage());
        assertEquals(1, closed.get());
    }

    @Test
    public void testToCollection_supplierReturnsNull_empty() {
        final AtomicInteger closed = new AtomicInteger();
        assertThrows(NullPointerException.class, () -> tracked(closed).toCollection(() -> (List<Object>) null));
        assertEquals(1, closed.get());
    }

    @Test
    public void testToCollectionThenApplyAndAccept_supplierReturnsNull() {
        assertThrows(NullPointerException.class, () -> Seq.of("a").toCollectionThenApply(() -> (List<String>) null, c -> c));
        assertThrows(NullPointerException.class, () -> Seq.of("a").toCollectionThenAccept(() -> (List<String>) null, c -> {
        }));
    }

    @Test
    public void testToMap_mapFactoryReturnsNull() {
        final Supplier<Map<String, String>> nullFactory = () -> null;

        for (final boolean empty : new boolean[] { false, true }) {
            final AtomicInteger closed = new AtomicInteger();
            final Seq<String, Exception> seq = empty ? tracked(closed) : tracked(closed, "a", "b");
            final NullPointerException e = assertThrows(NullPointerException.class,
                    () -> seq.toMap(x -> x, x -> x, Fnn.throwingMerger(), nullFactory));
            assertEquals("mapFactory returned null", e.getMessage());
            assertEquals(1, closed.get());
        }

        assertThrows(NullPointerException.class, () -> Seq.of("a").toMap(x -> x, x -> x, nullFactory));
        assertThrows(NullPointerException.class, () -> Seq.<String, Exception> empty().toMap(x -> x, x -> x, nullFactory));
    }

    @Test
    public void testGroupTo_mapFactoryReturnsNull() {
        final Supplier<Map<String, List<String>>> nullListMap = () -> null;
        final Supplier<Map<String, Long>> nullCountMap = () -> null;

        final AtomicInteger closed = new AtomicInteger();
        assertThrows(NullPointerException.class, () -> tracked(closed, "a").groupTo(x -> x, nullListMap));
        assertEquals(1, closed.get());

        assertThrows(NullPointerException.class, () -> Seq.<String, Exception> empty().groupTo(x -> x, nullListMap));
        assertThrows(NullPointerException.class, () -> Seq.of("a").groupTo(x -> x, x -> x, nullListMap));
        assertThrows(NullPointerException.class, () -> Seq.of("a").groupTo(x -> x, Collectors.counting(), nullCountMap));
        assertThrows(NullPointerException.class, () -> Seq.<String, Exception> empty().groupTo(x -> x, x -> x, Collectors.counting(), nullCountMap));
    }

    @Test
    public void testGroupBy_intermediate_mapFactoryReturnsNull_throwsOnTraversal() throws Exception {
        final Supplier<Map<String, List<String>>> nullListMap = () -> null;
        final AtomicInteger closed = new AtomicInteger();

        final Seq<Map.Entry<String, List<String>>, Exception> grouped = tracked(closed, "a", "b").groupBy(x -> x, nullListMap);
        assertThrows(NullPointerException.class, grouped::toList);
        assertEquals(1, closed.get());
    }

    @Test
    public void testToMultimapAndToMultiset_factoryReturnsNull() {
        final Supplier<ListMultimap<String, String>> nullMultimap = () -> null;
        final Supplier<Multiset<String>> nullMultiset = () -> null;

        assertThrows(NullPointerException.class, () -> Seq.of("a").toMultimap(x -> x, nullMultimap));
        assertThrows(NullPointerException.class, () -> Seq.<String, Exception> empty().toMultimap(x -> x, nullMultimap));
        assertThrows(NullPointerException.class, () -> Seq.of("a").toMultimap(x -> x, x -> x, nullMultimap));

        final AtomicInteger closed = new AtomicInteger();
        final NullPointerException e = assertThrows(NullPointerException.class, () -> tracked(closed, "a").toMultiset(nullMultiset));
        assertEquals("supplier returned null", e.getMessage());
        assertEquals(1, closed.get());
        assertThrows(NullPointerException.class, () -> Seq.<String, Exception> empty().toMultiset(nullMultiset));
    }

    @Test
    public void testSplitAndSliding_collectionSupplierReturnsNull() {
        final AtomicInteger closed = new AtomicInteger();
        final NullPointerException e = assertThrows(NullPointerException.class,
                () -> tracked(closed, 1, 2, 3).split(2, n -> (List<Integer>) null).toList());
        assertEquals("collectionSupplier returned null", e.getMessage());
        assertEquals(1, closed.get());

        assertThrows(NullPointerException.class, () -> Seq.of(1, 2, 3).split(x -> x > 1, () -> (List<Integer>) null).toList());
        assertThrows(NullPointerException.class, () -> Seq.of(1, 2, 3).sliding(2, 1, n -> (List<Integer>) null).toList());
        assertThrows(NullPointerException.class, () -> Seq.of(1, 2, 3).sliding(2, n -> (List<Integer>) null).toList());
    }

    @Test
    public void testSplitAndSliding_collectionSupplierReturnsNull_emptySourceNeverAsks() throws Exception {
        assertEquals(Collections.emptyList(), Seq.<Integer, Exception> empty().split(2, n -> (List<Integer>) null).toList());
        assertEquals(Collections.emptyList(), Seq.<Integer, Exception> empty().sliding(2, 1, n -> (List<Integer>) null).toList());
    }

    // ------------------------------------------------------------------------------------------------------
    // C-018 defer: a supplier failure is final (doc-only; regression lock)
    // ------------------------------------------------------------------------------------------------------

    @Test
    public void testDefer_supplierFailureIsFinal() {
        final AtomicInteger calls = new AtomicInteger();
        final Seq<Integer, Exception> d = Seq.defer(() -> {
            if (calls.incrementAndGet() == 1) {
                throw new IllegalStateException("boom");
            }
            return Seq.of(1, 2, 3);
        });

        final Iterator<Integer> it = d.stream().iterator();
        assertThrows(IllegalStateException.class, it::hasNext);
        assertFalse(it.hasNext());
        assertEquals(1, calls.get());
    }

    // ------------------------------------------------------------------------------------------------------
    // C-019 callbacks accept a narrower checked exception (compile-level; the calls did not compile on the base)
    // ------------------------------------------------------------------------------------------------------

    // C019-BEGIN
    @Test
    public void testNarrowerCallbackExceptionTypesCompileAndRun() throws Exception {
        final Throwables.Function<String, String, FileNotFoundException> upper = String::toUpperCase;
        final Throwables.Function<String, u.Optional<String>, FileNotFoundException> partial = s -> s.isEmpty() ? u.Optional.empty() : u.Optional.of(s);
        final Throwables.Function<String, u.OptionalInt, FileNotFoundException> partialInt = s -> u.OptionalInt.of(s.length());
        final Throwables.Function<String, u.OptionalLong, FileNotFoundException> partialLong = s -> u.OptionalLong.of(s.length());
        final Throwables.Function<String, u.OptionalDouble, FileNotFoundException> partialDouble = s -> u.OptionalDouble.of(s.length());
        final Throwables.Predicate<String, FileNotFoundException> isA = "a"::equals;
        final Throwables.BiPredicate<String, Long, FileNotFoundException> firstOnly = (s, i) -> i == 1;
        final Throwables.Consumer<String, FileNotFoundException> noop = s -> {
        };
        final Throwables.BiFunction<String, String, MergeResult, FileNotFoundException> selector = (a, b) -> a.compareTo(b) <= 0 ? MergeResult.TAKE_FIRST
                : MergeResult.TAKE_SECOND;

        assertEquals(Arrays.asList("A", "b"), Seq.<String, IOException> of("a", "b").mapFirstOrElse(upper, s -> s).toList());
        assertEquals(Arrays.asList("A", "B"), Seq.<String, IOException> of("a", "b").mapFirstOrElse(upper, upper).toList());
        assertEquals(Arrays.asList("A", "B"), Seq.<String, IOException> of("a", "b").mapLastOrElse(upper, upper).toList());
        assertEquals(Arrays.asList("a"), Seq.<String, IOException> of("", "a").mapPartial(partial).toList());
        assertEquals(Arrays.asList(0, 1), Seq.<String, IOException> of("", "a").mapPartialToInt(partialInt).toList());
        assertEquals(Arrays.asList(0L, 1L), Seq.<String, IOException> of("", "a").mapPartialToLong(partialLong).toList());
        assertEquals(Arrays.asList(0d, 1d), Seq.<String, IOException> of("", "a").mapPartialToDouble(partialDouble).toList());
        assertEquals(2, Seq.<String, IOException> of("a", "b").partitionBy(isA).count());
        assertEquals(2, Seq.<String, IOException> of("a", "b").partitionBy(isA, Collectors.counting()).count());
        assertEquals(Arrays.asList("a"), Seq.<String, IOException> of("a", "b").intersection(upper, Arrays.asList("A")).toList());
        assertEquals(Arrays.asList("b"), Seq.<String, IOException> of("a", "b").difference(upper, Arrays.asList("A")).toList());
        assertEquals(Arrays.asList("a", "b"), Seq.<String, IOException> of("a", "b").peekIf(isA, noop).toList());
        assertEquals(Arrays.asList("a", "b"), Seq.<String, IOException> of("a", "b").peekIf(firstOnly, noop).toList());
        assertEquals(Arrays.asList("a", "b", "c"), Seq.<String, IOException> of("a", "c").mergeWith(Arrays.asList("b"), selector).toList());
        assertEquals(Arrays.asList("a", "b", "c"), Seq.<String, IOException> of("a", "c").mergeWith(Seq.<String, IOException> of("b"), selector).toList());

        final Seq<String, IOException> merged = Seq.merge(Seq.<String, IOException> of("a", "c"), Seq.<String, IOException> of("b"), selector);
        assertEquals(Arrays.asList("a", "b", "c"), merged.toList());
        final Seq<String, IOException> merged3 = Seq.merge(Seq.<String, IOException> of("a"), Seq.<String, IOException> of("c"),
                Seq.<String, IOException> of("b"), selector);
        assertEquals(Arrays.asList("a", "b", "c"), merged3.toList());
        final Seq<String, IOException> mergedArrays = Seq.merge(new String[] { "a", "c" }, new String[] { "b" }, selector);
        assertEquals(Arrays.asList("a", "b", "c"), mergedArrays.toList());
        final Seq<String, IOException> mergedArrays3 = Seq.merge(new String[] { "a" }, new String[] { "c" }, new String[] { "b" }, selector);
        assertEquals(Arrays.asList("a", "b", "c"), mergedArrays3.toList());
        final Seq<String, IOException> mergedIterables = Seq.merge(Arrays.asList("a", "c"), Arrays.asList("b"), selector);
        assertEquals(Arrays.asList("a", "b", "c"), mergedIterables.toList());
        final Seq<String, IOException> mergedIterators = Seq.merge(Arrays.asList("a", "c").iterator(), Arrays.asList("b").iterator(), selector);
        assertEquals(Arrays.asList("a", "b", "c"), mergedIterators.toList());
        // U10-03 (2026-09-25): the two three-way overloads (Iterable x3, Iterator x3) were not exercised
        final Seq<String, IOException> mergedIterables3 = Seq.merge(Arrays.asList("a"), Arrays.asList("c"), Arrays.asList("b"), selector);
        assertEquals(Arrays.asList("a", "b", "c"), mergedIterables3.toList());
        final Seq<String, IOException> mergedIterators3 = Seq.merge(Arrays.asList("a").iterator(), Arrays.asList("c").iterator(),
                Arrays.asList("b").iterator(), selector);
        assertEquals(Arrays.asList("a", "b", "c"), mergedIterators3.toList());

        // A lambda selector still infers E from the target / its body, as before.
        assertEquals(Arrays.asList(1, 2, 3),
                Seq.merge(new Integer[] { 1, 3 }, new Integer[] { 2 }, (a, b) -> a <= b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).toList());
    }
    // C019-END

    // ------------------------------------------------------------------------------------------------------
    // C-020 distinct(merge)/distinctBy(merge): a null merge result re-inserts on odd counts (doc-only; lock)
    // ------------------------------------------------------------------------------------------------------

    @Test
    public void testDistinctMerge_nullResultRemovesGroupAndLaterDuplicateStartsANewOne() throws Exception {
        assertEquals(Arrays.asList(1, null), Seq.of(null, 1, null, null).distinct((a, b) -> a).toList());
        assertEquals(Arrays.asList(1), Seq.of(null, 1, null).distinct((a, b) -> a).toList());
        assertEquals(Arrays.asList("b", "a"), Seq.of("a", "b", "a", "a").distinct((x, y) -> null).toList());
        assertEquals(Arrays.asList("b"), Seq.of("a", "b", "a").distinct((x, y) -> null).toList());
        assertEquals(Arrays.asList("bb", "a3"), Seq.of("a1", "bb", "a2", "a3").distinctBy(s -> s.charAt(0), (x, y) -> null).toList());
        assertEquals(Collections.emptyList(), Seq.<String, Exception> empty().distinct((x, y) -> null).toList());
    }

    // ------------------------------------------------------------------------------------------------------
    // C-050 cycled(0)/cycled(1) short-circuit
    // ------------------------------------------------------------------------------------------------------

    @Test
    public void testCycledOne_keepsSortedFlag() throws Exception {
        final AtomicInteger comparisons = new AtomicInteger();
        final Comparator<Integer> counting = (a, b) -> {
            comparisons.incrementAndGet();
            return Integer.compare(a, b);
        };

        // Comparisons made by the (lazy) sort alone.
        Seq.of(3, 1, 2).sorted(counting).toList();
        final int sortOnly = comparisons.get();

        comparisons.set(0);
        assertEquals(u.Nullable.of(1), Seq.of(3, 1, 2).sorted(counting).cycled(1).min(counting));
        assertEquals(sortOnly, comparisons.get(), "cycled(1) must keep the sorted flag, so min(..) needs no comparison");
    }

    @Test
    public void testCycledZeroAndOne_valuesAndClose() throws Exception {
        final AtomicInteger pulls = new AtomicInteger();
        final Iterator<Integer> counting = new Iterator<>() {
            private int i = 0;

            @Override
            public boolean hasNext() {
                return i < 3;
            }

            @Override
            public Integer next() {
                pulls.incrementAndGet();
                return i++;
            }
        };

        assertEquals(Collections.emptyList(), Seq.<Integer, Exception> of(counting).cycled(0).toList());
        assertEquals(0, pulls.get());

        final AtomicInteger closed = new AtomicInteger();
        assertEquals(Arrays.asList(1, 2, 3), tracked(closed, 1, 2, 3).cycled(1).toList());
        assertEquals(1, closed.get());

        final AtomicInteger closed0 = new AtomicInteger();
        assertEquals(0, tracked(closed0, 1, 2, 3).cycled(0).count());
        assertEquals(1, closed0.get());

        assertEquals(Collections.emptyList(), Seq.<Integer, Exception> empty().cycled(1).toList());
        assertEquals(Arrays.asList(1, 2, 1, 2), Seq.of(1, 2).cycled(2).toList());
        assertThrows(IllegalArgumentException.class, () -> Seq.of(1).cycled(-1));
    }

    // ------------------------------------------------------------------------------------------------------
    // C-051 delay(java.time.Duration) overflow closes the sequence
    // ------------------------------------------------------------------------------------------------------

    @Test
    public void testDelayJavaTimeDuration_overflowClosesSequence() {
        final AtomicInteger closed = new AtomicInteger();
        final Seq<Integer, Exception> seq = tracked(closed, 1, 2);

        assertThrows(ArithmeticException.class, () -> seq.delay(java.time.Duration.ofSeconds(Long.MAX_VALUE)));
        assertEquals(1, closed.get());
    }

    @Test
    public void testDelayJavaTimeDuration_largestRepresentableAndZero() throws Exception {
        final AtomicInteger closed = new AtomicInteger();
        final Seq<Integer, Exception> delayed = tracked(closed, 1).delay(java.time.Duration.ofMillis(Long.MAX_VALUE));
        // A single element is never delayed.
        assertEquals(Arrays.asList(1), delayed.toList());
        assertEquals(1, closed.get());
        assertEquals(Arrays.asList(1, 2), Seq.of(1, 2).delay(java.time.Duration.ZERO).toList());
    }

    // ------------------------------------------------------------------------------------------------------
    // C-053 groupBy(k, v, merge, mapFactory) accepts a constructor reference (doc-only; lock)
    // ------------------------------------------------------------------------------------------------------

    @Test
    public void testGroupByFourArg_acceptsTreeMapConstructorReference() throws Exception {
        final List<Map.Entry<String, Integer>> result = Seq.of("b2", "a1", "a3", "b1")
                .groupBy(s -> s.substring(0, 1), s -> Integer.parseInt(s.substring(1)), Fnn.<Integer, Exception> max(Comparator.naturalOrder()), TreeMap::new)
                .toList();

        assertEquals("[a=3, b=2]", result.toString());
    }

    // ------------------------------------------------------------------------------------------------------
    // C-017 max keeps the first of tied maxima even on a sorted sequence
    // ------------------------------------------------------------------------------------------------------

    @Test
    public void testMax_sortedSequence_keepsFirstOfTiedMaxima() throws Exception {
        final Comparator<String> byLen = Comparator.comparingInt(String::length);

        assertEquals(u.Nullable.of("bb"), Seq.of("bb", "a", "cc").sorted(byLen).max(byLen));
        assertEquals(u.Nullable.of("bb"), Seq.of("a", "bb", "cc").max(byLen));
        assertEquals(u.Nullable.of("bb"), Seq.of("a", "bb", "cc").maxBy(String::length));
        assertEquals(u.Nullable.of("a"), Seq.of("a", "b").sorted(byLen).min(byLen));
        assertEquals(u.Nullable.empty(), Seq.<String, Exception> empty().sorted(byLen).max(byLen));
        assertEquals(u.Nullable.of(null), Seq.of((String) null).sorted(Comparators.nullsFirst()).max(Comparators.nullsFirst()));
    }

    @Test
    public void testMinByMaxBy_closeOnKeyMapperFailure() {
        final AtomicInteger closed = new AtomicInteger();
        assertThrows(NullPointerException.class, () -> tracked(closed, "a", null, "b").minBy(String::length));
        assertEquals(1, closed.get());

        final AtomicInteger closed2 = new AtomicInteger();
        assertThrows(NullPointerException.class, () -> tracked(closed2, "a", null, "b").maxBy(String::length));
        assertEquals(1, closed2.get());
    }

    // ------------------------------------------------------------------------------------------------------
    // C-038 shuffled() still yields a permutation (fast non-secure source now)
    // ------------------------------------------------------------------------------------------------------

    @Test
    public void testShuffled_isAPermutationAndCloses() throws Exception {
        final AtomicInteger closed = new AtomicInteger();
        final List<Integer> shuffled = tracked(closed, 1, 2, 3, 4, 5, 6, 7, 8).shuffled().toList();
        final List<Integer> sorted = new ArrayList<>(shuffled);
        Collections.sort(sorted);
        assertEquals(Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8), sorted);
        assertEquals(1, closed.get());
        assertEquals(Collections.emptyList(), Seq.<Integer, Exception> empty().shuffled().toList());
    }

    // ------------------------------------------------------------------------------------------------------
    // C-002 split/sliding no longer pre-size from the caller's limit; the supplier gets the exact count
    // ------------------------------------------------------------------------------------------------------

    @Test
    public void testSplitAndSliding_hugeChunkSizeOnTinySource() throws Exception {
        assertEquals(Arrays.asList(Arrays.asList(1, 2, 3)), Seq.of(1, 2, 3).split(Integer.MAX_VALUE - 8).toList());
        assertEquals(Arrays.asList(Arrays.asList(1, 2, 3)), Seq.of(1, 2, 3).split(Integer.MAX_VALUE).toList());
        assertEquals(Arrays.asList(Arrays.asList(1, 2, 3)), Seq.of(1, 2, 3).sliding(Integer.MAX_VALUE - 8).toList());
        assertEquals(Arrays.asList(Arrays.asList(1, 2, 3)), Seq.of(1, 2, 3).sliding(Integer.MAX_VALUE - 8, 1).toList());
        assertEquals(Arrays.asList(Arrays.asList(1, 2, 3)), Seq.of(1, 2, 3).sliding(Integer.MAX_VALUE, Integer.MAX_VALUE).toList());
    }

    @Test
    public void testSplit_supplierReceivesExactCounts() throws Exception {
        final List<Integer> requested = new ArrayList<>();
        final IntFunction<List<Integer>> recording = n -> {
            requested.add(n);
            return new ArrayList<>(n);
        };

        assertEquals(Arrays.asList(Arrays.asList(1, 2), Arrays.asList(3, 4), Arrays.asList(5)), Seq.of(1, 2, 3, 4, 5).split(2, recording).toList());
        assertEquals(Arrays.asList(2, 2, 1), requested);

        requested.clear();
        assertEquals(Arrays.asList(Arrays.asList(1, 2, 3)), Seq.of(1, 2, 3).split(Integer.MAX_VALUE, recording).toList());
        assertEquals(Arrays.asList(3), requested);
    }

    @Test
    public void testSliding_supplierReceivesExactCounts() throws Exception {
        final List<Integer> requested = new ArrayList<>();
        final IntFunction<List<Integer>> recording = n -> {
            requested.add(n);
            return new ArrayList<>(n);
        };

        assertEquals(Arrays.asList(Arrays.asList(1, 2)), Seq.of(1, 2).sliding(3, recording).toList());
        assertEquals(Arrays.asList(2), requested);

        requested.clear();
        assertEquals(Arrays.asList(Arrays.asList(1, 2, 3), Arrays.asList(3, 4)), Seq.of(1, 2, 3, 4).sliding(3, 2, recording).toList());
        assertEquals(Arrays.asList(3, 2), requested);
    }

    @Test
    public void testSplitAndSliding_boundedCollectionSupplier() throws Exception {
        final List<Integer> hundred = Seq.range(0, 100).toList();
        final List<ArrayBlockingQueue<Integer>> chunks = Seq.of(hundred).split(40, IntFunctions.<Integer> ofArrayBlockingQueue()).toList();
        assertEquals(Arrays.asList(40, 40, 20), Seq.of(chunks).map(Collection::size).toList());
        assertEquals(hundred, Seq.of(chunks).flatmap(q -> new ArrayList<>(q)).toList());

        final List<ArrayBlockingQueue<Integer>> windows = Seq.of(1, 2, 3, 4, 5).sliding(3, 1, IntFunctions.<Integer> ofArrayBlockingQueue()).toList();
        assertEquals("[[1, 2, 3], [2, 3, 4], [3, 4, 5]]", windows.toString());
    }

    @Test
    public void testSplitAndSliding_customSupplierResultsAreIndependent() throws Exception {
        final List<LinkedHashSet<String>> chunks = Seq.of("a", "b", "c", "d", "e").split(2, n -> new LinkedHashSet<>()).toList();
        assertEquals("[[a, b], [c, d], [e]]", chunks.toString());
        assertNotSame(chunks.get(0), chunks.get(1));

        final List<LinkedList<Integer>> gapped = Seq.of(1, 2, 3, 4, 5, 6, 7).sliding(2, 3, n -> new LinkedList<>()).toList();
        assertEquals("[[1, 2], [4, 5], [7]]", gapped.toString());

        final List<List<Integer>> defaults = Seq.of(1, 2, 3, 4, 5).split(2).toList();
        defaults.get(0).add(99);
        assertEquals("[[1, 2, 99], [3, 4], [5]]", defaults.toString());
        assertInstanceOf(ArrayList.class, defaults.get(1));

        final List<List<Integer>> windows = Seq.of(1, 2, 3, 4).sliding(2).toList();
        assertEquals("[[1, 2], [2, 3], [3, 4]]", windows.toString());
        windows.get(0).set(1, 42);
        assertEquals("[[1, 42], [2, 3], [3, 4]]", windows.toString());

        final List<HashSet<String>> unicode = Seq.of("😀", "é", "😀").split(3, n -> new HashSet<>()).toList();
        assertEquals(1, unicode.size());
        assertEquals(2, unicode.get(0).size());
    }

    // ------------------------------------------------------------------------------------------------------
    // Q2-07 split(predicate) evaluates the predicate once per element
    // ------------------------------------------------------------------------------------------------------

    @Test
    public void testSplitByPredicate_predicateCalledOncePerElement() throws Exception {
        final AtomicInteger calls = new AtomicInteger();
        final List<List<Integer>> groups = Seq.of(1, 3, 5, 2, 4, 6, 7, 9).split(x -> {
            calls.incrementAndGet();
            return x % 2 == 0;
        }).toList();

        assertEquals("[[1, 3, 5], [2, 4, 6], [7, 9]]", groups.toString());
        assertEquals(8, calls.get());

        calls.set(0);
        final List<Long> counts = Seq.of(1, 2, 3, 4).split(x -> {
            calls.incrementAndGet();
            return x % 2 == 0;
        }, Collectors.counting()).toList();
        assertEquals(Arrays.asList(1L, 1L, 1L, 1L), counts);
        assertEquals(4, calls.get());

        calls.set(0);
        final List<LinkedHashSet<Integer>> sets = Seq.of(1, 1, 2, 2, 1).split(x -> {
            calls.incrementAndGet();
            return x == 2;
        }, LinkedHashSet::new).toList();
        assertEquals("[[1], [2], [1]]", sets.toString());
        assertEquals(5, calls.get());

        assertEquals(Collections.emptyList(), Seq.<Integer, Exception> empty().split(x -> true).toList());
        assertEquals("[[5]]", Seq.of(5).split(x -> true).toList().toString());
    }

    // ------------------------------------------------------------------------------------------------------
    // Q2-09 throwIfEmpty reports the caller's exception, not a closed-state ISE of the upstream
    // ------------------------------------------------------------------------------------------------------

    @Test
    public void testThrowIfEmpty_upstreamClosedAfterDerive_reportsCallersException() {
        final Seq<Integer, Exception> upstream = Seq.empty();
        final Seq<Integer, Exception> derived = upstream.throwIfEmpty(() -> new IllegalArgumentException("custom"));
        upstream.close();

        final IllegalArgumentException e = assertThrows(IllegalArgumentException.class, derived::toList);
        assertEquals("custom", e.getMessage());
    }

    // ------------------------------------------------------------------------------------------------------
    // Q1-06 / Q1-07 / Q1-08 / Q2-11 / Q3-06 regression locks for doc-only or refactor-only changes
    // ------------------------------------------------------------------------------------------------------

    @Test
    public void testOfLines_readsAndReleasesTheFile() throws Exception {
        final Path tmp = Files.createTempFile("seq-review-20260924", ".txt");

        try {
            Files.write(tmp, Arrays.asList("x", "é😀", "z"), StandardCharsets.UTF_8);
            final File file = tmp.toFile();

            assertEquals(Arrays.asList("x", "é😀", "z"), Seq.ofLines(file, StandardCharsets.UTF_8).toList());
            assertEquals(Arrays.asList("x", "é😀", "z"), Seq.ofLines(tmp, StandardCharsets.UTF_8).toList());
            assertEquals("x", Seq.ofLines(file, StandardCharsets.UTF_8).first().orElseThrow());

            final Seq<String, IOException> untraversed = Seq.ofLines(tmp, StandardCharsets.UTF_8);
            untraversed.close();
            assertThrows(IllegalStateException.class, untraversed::toList);
        } finally {
            Files.deleteIfExists(tmp);
        }

        assertFalse(Files.exists(tmp));
    }

    @Test
    public void testConcatEmptyExamples() throws Exception {
        final Seq<Integer, Exception> emptyArrays = Seq.concat(new Integer[0]);
        assertEquals(0, emptyArrays.count());
        final Seq<String, Exception> emptyIterators = Seq.concat(Collections.<String> emptyIterator());
        assertEquals(0, emptyIterators.count());
        final Seq<String, Exception> emptySeqs = Seq.concat(Seq.<String, Exception> empty());
        assertEquals(0, emptySeqs.count());
    }

    @Test
    public void testNullArgumentsTreatedAsEmpty() throws Exception {
        assertEquals(Arrays.asList(1, 2), Seq.of(1, 2).prepend((Seq<Integer, Exception>) null).toList());
        assertEquals(Arrays.asList(1, 2), Seq.of(1, 2).append((Seq<Integer, Exception>) null).toList());
        assertEquals(Arrays.asList(1, 2), Seq.of(1, 2).prepend((Collection<Integer>) null).toList());
        assertEquals(Arrays.asList(1, 2), Seq.of(1, 2).append((Collection<Integer>) null).toList());
        assertEquals(Arrays.asList(1, 2),
                Seq.of(1, 2).mergeWith((Collection<Integer>) null, (a, b) -> MergeResult.TAKE_FIRST).toList());
        assertEquals(Arrays.asList(1, 2),
                Seq.of(1, 2).mergeWith((Seq<Integer, Exception>) null, (a, b) -> MergeResult.TAKE_FIRST).toList());
        assertEquals(Arrays.asList(null, "a", "bb"), Seq.of(null, "a", "bb").sortedByInt(s -> s == null ? 0 : s.length()).toList());
    }

    @Test
    public void testPartitionTo_bothEntriesFalseFirstMutable() throws Exception {
        final Map<Boolean, Long> empty = Seq.<Integer, Exception> empty().partitionTo(x -> x > 0, Collectors.counting());
        assertEquals("{false=0, true=0}", empty.toString());
        empty.put(Boolean.TRUE, 5L);
        assertEquals(5L, empty.get(Boolean.TRUE));

        final Map<Boolean, List<Integer>> parts = Seq.of(1, -1, 2).partitionTo(x -> x > 0);
        assertEquals("{false=[-1], true=[1, 2]}", parts.toString());
    }

    @Test
    public void testSpsAndTransformViaStream_stillUnwrapCheckedExceptions() {
        final IOException io = new IOException("io");
        final IOException thrown = assertThrows(IOException.class, () -> Seq.<Integer, IOException> of(1, 2).transformViaStream(s -> s.map(x -> {
            throw new java.io.UncheckedIOException(io);
        })).toList());
        assertEquals("io", thrown.getMessage());
    }
}

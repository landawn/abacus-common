package com.landawn.abacus.bugfix;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;
import java.util.stream.Collector.Characteristics;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.stream.Collectors;
import com.landawn.abacus.util.stream.IntStream;
import com.landawn.abacus.util.stream.ObjIteratorEx;
import com.landawn.abacus.util.stream.Stream;

/**
 * Tests verifying bug fixes from the second multi-agent code review pass
 * (focused on com.landawn.abacus.util.stream).
 */
public class StreamBugFixVerificationTest extends TestBase {

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private static <T, R> R combineDirectly(final java.util.stream.Collector<T, ?, R> collector, final List<T> left, final List<T> right) {
        final Supplier supplier = collector.supplier();
        final java.util.function.BiConsumer accumulator = collector.accumulator();
        final java.util.function.BinaryOperator combiner = collector.combiner();
        final java.util.function.Function finisher = collector.finisher();
        final Object la = supplier.get();
        for (T t : left) {
            accumulator.accept(la, t);
        }
        final Object ra = supplier.get();
        for (T t : right) {
            accumulator.accept(ra, t);
        }
        return (R) finisher.apply(combiner.apply(la, ra));
    }

    @Test
    public void collectorsToList_combinerPreservesOrder() {
        // The combiner must append the right (later) segment AFTER the left (earlier) one regardless
        // of segment sizes; a bigger-wins merge reorders JDK parallel stream results.
        final List<Integer> result = combineDirectly(Collectors.toList(), Arrays.asList(1, 2), Arrays.asList(3, 4, 5, 6));
        assertEquals(Arrays.asList(1, 2, 3, 4, 5, 6), result);
    }

    @Test
    public void collectorsToLinkedHashSet_isNotMarkedUnordered() {
        // The LinkedHashSet collector must not advertise UNORDERED — that would tell parallel
        // pipelines they may merge bucket partitions in arbitrary order.
        final java.util.stream.Collector<Integer, ?, Set<Integer>> c = Collectors.toLinkedHashSet();
        assertFalse(c.characteristics().contains(java.util.stream.Collector.Characteristics.UNORDERED),
                "toLinkedHashSet must not be UNORDERED — it stores elements in insertion order");
    }

    @Test
    public void collectorsToLinkedHashMap_isNotMarkedUnordered() {
        final java.util.stream.Collector<Map.Entry<String, Integer>, ?, Map<String, Integer>> c = Collectors.toLinkedHashMap(Map.Entry::getKey,
                Map.Entry::getValue);
        assertFalse(c.characteristics().contains(java.util.stream.Collector.Characteristics.UNORDERED),
                "toLinkedHashMap must not be UNORDERED — LinkedHashMap maintains insertion order");
    }

    @Test
    public void collectorsToMap_supportsNullValues() {
        // Pre-fix: Map.merge() in the accumulator threw NPE when valueMapper returned null,
        // contradicting the toMap(...) javadoc that null values are supported when the underlying
        // map permits them.
        final List<String> input = Arrays.asList("a", "b", "c");
        final Map<String, String> result = Stream.of(input).collect(Collectors.toMap(s -> s, s -> s.equals("b") ? null : s));
        assertEquals("a", result.get("a"));
        assertNull(result.get("b"));
        assertTrue(result.containsKey("b"));
        assertEquals("c", result.get("c"));
    }

    // -----------------------------------------------------------------
    // Bug: AbstractStream.cycled(rounds) on an empty source returned an
    // iterator whose hasNext()=true but next() throws NoSuchElementException
    // when rounds >= 3. Now correctly returns empty.
    // -----------------------------------------------------------------

    @Test
    public void cycledRounds_onEmptySource_doesNotInfinite() {
        // Before fix: this would loop "forever" — hasNext returns true but next() throws.
        // After fix: returns empty stream.
        assertEquals(0L, Stream.<Integer> empty().cycled(3).count());
        assertEquals(0L, Stream.<Integer> empty().cycled(5).count());
        assertEquals(0L, Stream.<Integer> empty().cycled(100).count());
    }

    @Test
    public void cycledRounds_normalSourceStillWorks() {
        // Sanity: don't break the happy path.
        assertEquals(Arrays.asList(1, 2, 1, 2, 1, 2), Stream.of(1, 2).cycled(3).toList());
        assertEquals(0, Stream.of(1).cycled(0).count());
        assertEquals(Arrays.asList(1), Stream.of(1).cycled(1).toList());
    }

    // -----------------------------------------------------------------
    // Bug: IntStream.splitByChunkCount anonymous IntIteratorEx.count() returned
    // count - cnt without resetting cnt or advancing cursor, so a subsequent
    // hasNext() / nextInt() would resume producing elements after a count() call.
    // -----------------------------------------------------------------

    @Test
    public void splitByChunkCount_count_drainsIterator() {
        // After count() the iterator must be exhausted — otherwise consumers that call
        // count() then iterate get duplicate output. Verify the user-visible invariant
        // that count() == toArray().length on a separately-built stream.
        assertEquals(3L, IntStream.splitByChunkCount(10, 3, (from, to) -> to - from).count());
        assertEquals(3, IntStream.splitByChunkCount(10, 3, (from, to) -> to - from).toArray().length);
        // sizeSmallerFirst variant (the second anonymous IntIteratorEx)
        assertEquals(3L, IntStream.splitByChunkCount(10, 3, true, (from, to) -> to - from).count());
        assertEquals(3, IntStream.splitByChunkCount(10, 3, true, (from, to) -> to - from).toArray().length);
    }

    // -----------------------------------------------------------------
    // Bug: ObjIteratorEx.defer().closeResource() forced init() (running the
    // iteratorSupplier) even when the deferred iterator was never iterated.
    // That defeats the whole point of defer (lazy resource opening).
    // -----------------------------------------------------------------

    @Test
    public void deferIteratorEx_closeResource_doesNotForceInitialization() {
        final AtomicInteger supplierCalls = new AtomicInteger();
        final ObjIteratorEx<Integer> deferred = ObjIteratorEx.defer(() -> {
            supplierCalls.incrementAndGet();
            return ObjIteratorEx.of(new Integer[] { 1, 2, 3 });
        });

        // Close without ever iterating — pre-fix this incremented supplierCalls.
        deferred.closeResource();

        assertEquals(0, supplierCalls.get(), "closeResource() on an unused deferred iterator must not initialize the supplier");
    }

    @Test
    public void deferIteratorEx_closeResource_forwardsToAutoCloseable() {
        // When the supplier returns a plain Iterator that is also AutoCloseable,
        // closeResource() must propagate to it (otherwise the resource leaks).
        final AtomicInteger closeCount = new AtomicInteger();
        class CloseableIter implements Iterator<Integer>, AutoCloseable {
            private int i = 0;

            @Override
            public boolean hasNext() {
                return i < 2;
            }

            @Override
            public Integer next() {
                return ++i;
            }

            @Override
            public void close() {
                closeCount.incrementAndGet();
            }
        }

        final ObjIteratorEx<Integer> deferred = ObjIteratorEx.defer((Supplier<Iterator<Integer>>) CloseableIter::new);
        // Force initialization
        assertTrue(deferred.hasNext());
        deferred.closeResource();
        assertEquals(1, closeCount.get(), "closeResource() must forward to the underlying AutoCloseable iterator");
    }

    // -----------------------------------------------------------------
    // Bug: AbstractStream.reversed/rotated/rollup/combinations/cartesianProduct
    // count() returned the remaining count without advancing cursor,
    // violating the IteratorEx.count() drain contract.
    // -----------------------------------------------------------------

    @Test
    public void reversedCount_drainsIterator() {
        final ObjIteratorEx<Integer> iter = (ObjIteratorEx<Integer>) Stream.of(1, 2, 3, 4, 5).reversed().iterator();
        assertEquals(5L, iter.count());
        assertFalse(iter.hasNext(), "reversed().iterator() must be drained after count()");
    }

    @Test
    public void rotatedCount_drainsIterator() {
        final ObjIteratorEx<Integer> iter = (ObjIteratorEx<Integer>) Stream.of(1, 2, 3, 4, 5).rotated(2).iterator();
        assertEquals(5L, iter.count());
        assertFalse(iter.hasNext(), "rotated().iterator() must be drained after count()");
    }

    // -----------------------------------------------------------------
    // Bug: AbstractStream.sliding (& IteratorStream.sliding) count() formula
    // overcounted when increment >= windowSize. Fixed via shared
    // countSlidingWindows helper in StreamBase.
    // -----------------------------------------------------------------

    @Test
    public void slidingCount_correctWhenIncrementGreaterThanWindowSize() {
        // 12 elements, windowSize=3, increment=4 → windows start at 0, 4, 8 = 3 windows.
        // Pre-fix formula returned 4 (incorrectly counted an extra trailing partial).
        assertEquals(3L, Stream.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12).sliding(3, 4).count());
        // Same data through the iterator-backed stream:
        assertEquals(3L, Stream.of(N.toList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12).iterator()).sliding(3, 4).count());
    }

    @Test
    public void slidingCount_matchesActualEmittedWindows() {
        // count() must equal toList().size() for every (windowSize, increment) combo.
        for (int windowSize = 1; windowSize <= 4; windowSize++) {
            for (int increment = 1; increment <= 5; increment++) {
                final int ws = windowSize;
                final int inc = increment;
                final long counted = Stream.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10).sliding(ws, inc).count();
                final long materialized = Stream.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10).sliding(ws, inc).toList().size();
                assertEquals(materialized, counted, "sliding(" + ws + ", " + inc + "): count=" + counted + " but toList().size=" + materialized);
            }
        }
    }

    // -----------------------------------------------------------------
    // Bug: AbstractIntStream.summaryStatisticsAndPercentiles wrapped sum
    // through Numbers.toIntExact, throwing on overflow even though
    // IntSummaryStatistics's 4th arg is a long (and summaryStatistics() handles
    // overflow cleanly). Now uses a long-typed local sum.
    // -----------------------------------------------------------------

    @Test
    public void summaryStatisticsAndPercentiles_doesNotOverflowOnLargeSum() {
        // Sum of [Integer.MAX_VALUE, Integer.MAX_VALUE, 1] overflows int.
        // Pre-fix this threw ArithmeticException; now it should produce a valid statistic.
        final int[] big = { Integer.MAX_VALUE, Integer.MAX_VALUE, 1 };
        final var pair = IntStream.of(big).summaryStatisticsAndPercentiles();
        assertNotNull(pair);
        assertEquals(3L, pair.left().getCount());
        assertEquals(Integer.MAX_VALUE * 2L + 1L, pair.left().getSum());
    }

    @Test
    public void combineThreeCollectors_characteristics_intersectsAllThree() {
        // downstream1 and downstream2 both advertise UNORDERED
        final java.util.stream.Collector<Integer, AtomicLong, Long> unorderedSummer = java.util.stream.Collector.of(AtomicLong::new,
                (acc, e) -> acc.addAndGet(e), (a, b) -> {
                    a.addAndGet(b.get());
                    return a;
                }, AtomicLong::get, Characteristics.UNORDERED);

        // downstream3 does NOT advertise UNORDERED (ordered list collector)
        final java.util.stream.Collector<Integer, ArrayList<Integer>, List<Integer>> orderedList = java.util.stream.Collector.of(ArrayList::new, ArrayList::add,
                (a, b) -> {
                    a.addAll(b);
                    return a;
                }, a -> (List<Integer>) a);

        final java.util.stream.Collector<Integer, ?, ?> combined = Collectors.MoreCollectors.combine(unorderedSummer, unorderedSummer, orderedList,
                (sum1, sum2, list) -> Arrays.asList(sum1, sum2, list));

        // Post-fix: combined must NOT advertise UNORDERED because downstream3 lacks it
        assertFalse(combined.characteristics().contains(Characteristics.UNORDERED),
                "3-way combine must intersect all three downstreams' characteristics; " + "UNORDERED must be excluded because downstream3 is ordered");
    }

    @Test
    public void combineThreeCollectors_characteristics_preservesSharedOnes() {
        // All three have UNORDERED — the combined collector should still have it
        final java.util.stream.Collector<Integer, AtomicLong, Long> unorderedSummer = java.util.stream.Collector.of(AtomicLong::new,
                (acc, e) -> acc.addAndGet(e), (a, b) -> {
                    a.addAndGet(b.get());
                    return a;
                }, AtomicLong::get, Characteristics.UNORDERED);

        final java.util.stream.Collector<Integer, ?, ?> combined = Collectors.MoreCollectors.combine(unorderedSummer, unorderedSummer, unorderedSummer,
                (a, b, c) -> a + b + c);

        assertTrue(combined.characteristics().contains(Characteristics.UNORDERED), "3-way combine must preserve UNORDERED when all three downstreams share it");
    }

    @Test
    public void combineThreeCollectors_functionalCorrectness() {
        // Verify the combined collector still produces correct results after the fix
        final List<?> result = Stream.of(1, 2, 3, 4, 5)
                .collect(Collectors.MoreCollectors.combine(Collectors.counting(), Collectors.summingInt(x -> (int) x), Collectors.toList(),
                        (count, sum, list) -> Arrays.asList(count, sum, list)));

        assertEquals(3, result.size());
        assertEquals(5L, result.get(0)); // count
        assertEquals(15, result.get(1)); // sum
        assertEquals(Arrays.asList(1, 2, 3, 4, 5), result.get(2)); // list
    }

    // ---------- Multi-agent review pass (round 4) ----------

    /**
     * AbstractStream.splitAt(int) / splitAt(Predicate) count() previously drained the inner
     * iterator but never advanced the local cursor, so a subsequent hasNext() returned true
     * and the stream produced extra elements after count(). Same root cause as the earlier
     * reversed/rotated/rollup fix.
     */
    @SuppressWarnings("unchecked")
    @Test
    public void splitAt_count_drainsIterator() {
        ObjIteratorEx<Stream<Integer>> iter = (ObjIteratorEx<Stream<Integer>>) Stream.of(1, 2, 3, 4, 5).splitAt(2).iterator();
        assertEquals(2L, iter.count());
        assertFalse(iter.hasNext(), "splitAt(int).iterator() must be drained after count()");

        ObjIteratorEx<Stream<Integer>> iter2 = (ObjIteratorEx<Stream<Integer>>) Stream.of(1, 2, 3, 4, 5).splitAt((Integer i) -> i >= 3).iterator();
        assertEquals(2L, iter2.count());
        assertFalse(iter2.hasNext(), "splitAt(predicate).iterator() must be drained after count()");
    }

    /**
     * AbstractByteStream/CharStream/ShortStream/IntStream/LongStream/FloatStream/DoubleStream
     * cycled(rounds) on an empty source previously infinite-looped (or threw
     * NoSuchElementException) with rounds &gt;= 3. Same bug pattern that was fixed earlier for
     * the Object-typed stream.
     */
    @Test
    public void primitiveStream_cycledRounds_emptySource_terminates() {
        assertEquals(0L, com.landawn.abacus.util.stream.IntStream.empty().cycled(5).count());
        assertEquals(0L, com.landawn.abacus.util.stream.LongStream.empty().cycled(5).count());
        assertEquals(0L, com.landawn.abacus.util.stream.ByteStream.empty().cycled(5).count());
        assertEquals(0L, com.landawn.abacus.util.stream.CharStream.empty().cycled(5).count());
        assertEquals(0L, com.landawn.abacus.util.stream.ShortStream.empty().cycled(5).count());
        assertEquals(0L, com.landawn.abacus.util.stream.FloatStream.empty().cycled(5).count());
        assertEquals(0L, com.landawn.abacus.util.stream.DoubleStream.empty().cycled(5).count());
        assertEquals(6, com.landawn.abacus.util.stream.IntStream.of(1, 2).cycled(3).toArray().length);
    }

    /**
     * AbstractByteStream/CharStream/ShortStream.summaryStatisticsAndPercentiles wrapped the
     * 4th-argument sum through Numbers.toIntExact, throwing ArithmeticException on overflow
     * even though {Byte,Char,Short}SummaryStatistics's 4th arg is a long and the peer
     * summaryStatistics() API handles overflow cleanly. Now uses a long-typed local sum.
     */
    @Test
    public void primitiveSummaryStatisticsAndPercentiles_doesNotOverflow() {
        byte[] bytes = { (byte) 1, (byte) 2, (byte) 3, (byte) 4 };
        var pb = com.landawn.abacus.util.stream.ByteStream.of(bytes).summaryStatisticsAndPercentiles();
        assertEquals(4L, pb.left().getCount());
        assertEquals(10L, pb.left().getSum());

        char[] chars = { 'a', 'b', 'c', 'd' };
        var pc = com.landawn.abacus.util.stream.CharStream.of(chars).summaryStatisticsAndPercentiles();
        assertEquals(4L, pc.left().getCount());
        assertEquals((long) 'a' + 'b' + 'c' + 'd', pc.left().getSum());

        short[] shorts = { (short) 100, (short) 200, (short) 300 };
        var ps = com.landawn.abacus.util.stream.ShortStream.of(shorts).summaryStatisticsAndPercentiles();
        assertEquals(3L, ps.left().getCount());
        assertEquals(600L, ps.left().getSum());
    }

    /**
     * MoreCollectors.combine(Collection, merger) previously filtered out empty
     * characteristic sets before intersecting, so a combined collector could advertise
     * characteristics that one of its constituents lacked. Same root cause as the
     * 3-arg combine fix; the empty filter is gone.
     */
    @Test
    public void combineCollectionCollectors_intersectsEmptyCharacteristics() {
        final java.util.stream.Collector<Integer, AtomicLong, Long> unorderedSummer = java.util.stream.Collector.of(AtomicLong::new,
                (acc, e) -> acc.addAndGet(e), (a, b) -> {
                    a.addAndGet(b.get());
                    return a;
                }, AtomicLong::get, Characteristics.UNORDERED);

        final java.util.stream.Collector<Integer, ArrayList<Integer>, List<Integer>> orderedList = java.util.stream.Collector.of(ArrayList::new, ArrayList::add,
                (a, b) -> {
                    a.addAll(b);
                    return a;
                }, a -> (List<Integer>) a);

        final List<java.util.stream.Collector<? super Integer, ?, ?>> downstreams = Arrays.asList(unorderedSummer, unorderedSummer, orderedList);

        final java.util.stream.Collector<Integer, ?, ?> combined = Collectors.MoreCollectors.combine(downstreams, results -> results);

        assertFalse(combined.characteristics().contains(Characteristics.UNORDERED),
                "N-way combine must intersect ALL downstreams' characteristics; UNORDERED must be "
                        + "excluded because the ordered-list downstream's characteristics set is empty");
    }

    // -----------------------------------------------------------------
    // Bug: parallel flatMap* did not link this::close, so parent stayed open after child terminal
    // -----------------------------------------------------------------

    @Test
    public void parallelFlatMap_closesParentStream() {
        final AtomicInteger closed = new AtomicInteger();
        final Stream<Integer> parent = Stream.of(1, 2, 3).onClose(closed::incrementAndGet).parallel(4);
        parent.flatMap(i -> Stream.of(i, i)).count();
        assertEquals(1, closed.get(), "closing the flatMap child must close the parallel parent");
        assertThrows(IllegalStateException.class, parent::count);
    }

    // -----------------------------------------------------------------
    // Bug: sorted()/lazyLoad dropped isSorted on the outer deferred stream
    // -----------------------------------------------------------------

    @Test
    public void sorted_preservesSortedFlagForRedundantSortedElision() {
        final Stream<Integer> sorted = Stream.of(3, 1, 2).sorted();
        // If isSorted()/comparator were dropped, a second sorted() would re-materialize and re-sort.
        // The already-sorted path must return the same instance.
        assertTrue(sorted == sorted.sorted(), "already-sorted stream must elide a second sorted()");
        assertEquals(Arrays.asList(1, 2, 3), sorted.toList());
    }

    // -----------------------------------------------------------------
    // Bug: groupTo/groupBy accepted null downstream and then NPE'd opaquely
    // -----------------------------------------------------------------

    @Test
    public void groupTo_rejectsNullDownstream() {
        assertThrows(IllegalArgumentException.class, () -> Stream.of("a", "b").groupTo(s -> s, (java.util.stream.Collector<? super String, ?, ?>) null));
        assertThrows(IllegalArgumentException.class,
                () -> Stream.of("a", "b").groupBy(s -> s, (java.util.stream.Collector<? super String, ?, ?>) null).count());
    }

    // -----------------------------------------------------------------
    // Optional-returning terminals reject null selected elements and final reduction results.
    // Empty input still returns Optional.empty(); collectors follow the same strict contract.
    // -----------------------------------------------------------------

    @Test
    public void firstLastOnlyOne_nullElement_throwsNpe() {
        assertThrows(NullPointerException.class, () -> Stream.of((String) null).first());
        assertThrows(NullPointerException.class, () -> Stream.of("a", null).last());
        assertThrows(NullPointerException.class, () -> Stream.of((String) null).onlyOne());
        assertThrows(NullPointerException.class, () -> Stream.of((String) null).reduce((a, b) -> a));
        assertThrows(NullPointerException.class, () -> Stream.of(null, "x").findFirst(java.util.Objects::isNull));

        assertTrue(Stream.<String>empty().first().isEmpty());
        assertTrue(Stream.<String>empty().last().isEmpty());
        assertTrue(Stream.<String>empty().onlyOne().isEmpty());
        assertTrue(Stream.<String>empty().reduce((a, b) -> a).isEmpty());
        assertTrue(Stream.of((String) null).findFirst(value -> false).isEmpty());

        // onlyOne must still throw when there are two elements even if the first is null
        assertThrows(com.landawn.abacus.exception.TooManyElementsException.class, () -> Stream.of(null, "x").onlyOne());

        // Both operations reject null; assertEquals cannot compare throwing expressions.
        assertThrows(NullPointerException.class, () -> Stream.of((String) null).collect(Collectors.first()));
        assertThrows(NullPointerException.class, () -> Stream.of((String) null).first());
    }

    @Test
    public void arrayStream_nullElement_terminals_throwNpe() {
        final String[] withNull = { null };
        assertThrows(NullPointerException.class, () -> Stream.of(withNull).first());
        assertThrows(NullPointerException.class, () -> Stream.of(withNull).last());
        assertThrows(NullPointerException.class, () -> Stream.of(withNull).onlyOne());
        assertThrows(NullPointerException.class, () -> Stream.of(withNull).elementAt(0));
        assertTrue(Stream.of(withNull).elementAt(1).isEmpty());
    }

    @Test
    public void parallelReduce_nullElement_throwsNpe() {
        assertThrows(NullPointerException.class, () -> Stream.of((Integer) null).parallel(4).reduce((a, b) -> a));
        assertThrows(NullPointerException.class, () -> Stream.<Integer>of(null, null, null, null).parallel(4).reduce((a, b) -> a));
        assertThrows(NullPointerException.class, () -> Stream.of(null, null).parallel(4).findAny(x -> true));
        assertTrue(Stream.<Integer>empty().parallel(4).reduce((a, b) -> a).isEmpty());
        assertTrue(Stream.of(null, null).parallel(4).findAny(x -> false).isEmpty());
    }
}

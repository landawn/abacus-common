package com.landawn.abacus.util.stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.Joiner;
import com.landawn.abacus.util.Tuple.Tuple2;
import com.landawn.abacus.util.Tuple.Tuple3;
import com.landawn.abacus.util.u.Optional;

/**
 * Test-gap closures (2026-09-25) for the object-stream family. Every test here pins CURRENT, already-verified
 * behaviour that the cycle-1..3 pinning tests left uncovered (review findings U17-02, U17-03, U18-02, U23-02):
 * <ul>
 * <li>U17-02: the parallel combiner path of the multi-column averaging collectors (shared element count, N1-05);</li>
 * <li>U17-03: LST/C-114 failure precedence at {@code EntryStream.join(4-arg)} and {@code joinTo};</li>
 * <li>U18-02: LST/C-001 lazy {@code step(n)} skips the gap with ONE bulk {@code advance()} - never element by element;</li>
 * <li>U23-02: LST/C-017 sorted-shortcut tie walk of {@code ArrayStream}/{@code ParallelArrayStream.max} returns the FIRST
 * element of the trailing equivalence run, also on a sub-range and when the run reaches {@code fromIndex}.</li>
 * </ul>
 */
public class StreamTestGapsReview20260925Test extends TestBase {

    // ---------------------------------------------------------------------------------------------------------
    // U17-02: N1-05 rewrote the 8 multi-column averaging combiners (one shared count). averagingInt(3),
    // averagingBigInteger(2/3) and averagingBigDecimal(2/3) were pinned sequentially only.
    // ---------------------------------------------------------------------------------------------------------

    private static final MathContext MC = MathContext.DECIMAL128;

    /** 12 rows whose column averages are 7/3, 70/3 and 700/3 - enough elements for every parallel worker. */
    private static List<int[]> rows() {
        final List<int[]> rows = new ArrayList<>();

        for (int i = 0; i < 4; i++) {
            rows.add(new int[] { 1, 10, 100 });
            rows.add(new int[] { 2, 20, 200 });
            rows.add(new int[] { 4, 40, 400 });
        }

        return rows;
    }

    private static BigDecimal expectedAverage(final int sumOfThreeDistinctRows) {
        return new BigDecimal(sumOfThreeDistinctRows).divide(new BigDecimal(3), MC);
    }

    private static void assertBigAverage(final int sumOfThreeDistinctRows, final BigDecimal actual) {
        final BigDecimal expected = expectedAverage(sumOfThreeDistinctRows);
        assertTrue(expected.subtract(actual).abs().compareTo(new BigDecimal("1e-30")) < 0, "expected " + expected + " but was " + actual);
    }

    @Test
    public void testU1702_averagingIntThreeColumnsParallelCombiner() {
        final List<int[]> rows = rows();
        final double a0 = 7 / 3.0;

        final Tuple3<Double, Double, Double> seq = Stream.of(rows).collect(Collectors.MoreCollectors.averagingInt(r -> r[0], r -> r[1], r -> r[2]));
        final Stream<int[]> parallel = Stream.of(rows).parallel(4);
        assertTrue(parallel.isParallel());
        final Tuple3<Double, Double, Double> par = parallel.collect(Collectors.MoreCollectors.averagingInt(r -> r[0], r -> r[1], r -> r[2]));

        assertEquals(a0, seq._1, 1e-12);
        assertEquals(a0 * 10, seq._2, 1e-12);
        assertEquals(a0 * 100, seq._3, 1e-12);
        // Integer sums are exact, so the parallel result must be bit-identical to the sequential one.
        assertEquals(seq, par);

        // Parallel over an iterator-backed source (ParallelIteratorStream) too.
        assertEquals(seq, Stream.of(rows.iterator()).parallel(4).collect(Collectors.MoreCollectors.averagingInt(r -> r[0], r -> r[1], r -> r[2])));

        // Empty parallel stream: the combiner sees only empty containers.
        final Tuple3<Double, Double, Double> empty = Stream.<int[]> empty().parallel(4).collect(Collectors.MoreCollectors.averagingInt(r -> r[0], r -> r[1], r -> r[2]));
        assertEquals(Tuple3.of(0.0, 0.0, 0.0), empty);
    }

    @Test
    public void testU1702_averagingBigIntegerParallelCombiner() {
        final List<int[]> rows = rows();

        final Tuple2<BigDecimal, BigDecimal> seq2 = Stream.of(rows)
                .collect(Collectors.MoreCollectors.averagingBigInteger(r -> BigInteger.valueOf(r[0]), r -> BigInteger.valueOf(r[1])));
        final Tuple2<BigDecimal, BigDecimal> par2 = Stream.of(rows)
                .parallel(4)
                .collect(Collectors.MoreCollectors.averagingBigInteger(r -> BigInteger.valueOf(r[0]), r -> BigInteger.valueOf(r[1])));
        assertBigAverage(7, seq2._1);
        assertBigAverage(70, seq2._2);
        assertEquals(seq2, par2);

        final Tuple3<BigDecimal, BigDecimal, BigDecimal> seq3 = Stream.of(rows)
                .collect(Collectors.MoreCollectors.averagingBigInteger(r -> BigInteger.valueOf(r[0]), r -> BigInteger.valueOf(r[1]),
                        r -> BigInteger.valueOf(r[2])));
        final Tuple3<BigDecimal, BigDecimal, BigDecimal> par3 = Stream.of(rows)
                .parallel(4)
                .collect(Collectors.MoreCollectors.averagingBigInteger(r -> BigInteger.valueOf(r[0]), r -> BigInteger.valueOf(r[1]),
                        r -> BigInteger.valueOf(r[2])));
        assertBigAverage(7, seq3._1);
        assertBigAverage(70, seq3._2);
        assertBigAverage(700, seq3._3);
        assertEquals(seq3, par3);

        assertEquals(seq3, Stream.of(rows.iterator())
                .parallel(4)
                .collect(Collectors.MoreCollectors.averagingBigInteger(r -> BigInteger.valueOf(r[0]), r -> BigInteger.valueOf(r[1]),
                        r -> BigInteger.valueOf(r[2]))));

        assertEquals(Tuple2.of(BigDecimal.ZERO, BigDecimal.ZERO), Stream.<int[]> empty()
                .parallel(4)
                .collect(Collectors.MoreCollectors.averagingBigInteger(r -> BigInteger.valueOf(r[0]), r -> BigInteger.valueOf(r[1]))));
    }

    @Test
    public void testU1702_averagingBigDecimalParallelCombiner() {
        final List<int[]> rows = rows();

        final Tuple2<BigDecimal, BigDecimal> seq2 = Stream.of(rows)
                .collect(Collectors.MoreCollectors.averagingBigDecimal(r -> BigDecimal.valueOf(r[0]), r -> BigDecimal.valueOf(r[1])));
        final Tuple2<BigDecimal, BigDecimal> par2 = Stream.of(rows)
                .parallel(4)
                .collect(Collectors.MoreCollectors.averagingBigDecimal(r -> BigDecimal.valueOf(r[0]), r -> BigDecimal.valueOf(r[1])));
        assertBigAverage(7, seq2._1);
        assertBigAverage(70, seq2._2);
        assertEquals(seq2, par2);

        final Tuple3<BigDecimal, BigDecimal, BigDecimal> seq3 = Stream.of(rows)
                .collect(Collectors.MoreCollectors.averagingBigDecimal(r -> BigDecimal.valueOf(r[0]), r -> BigDecimal.valueOf(r[1]),
                        r -> BigDecimal.valueOf(r[2])));
        final Tuple3<BigDecimal, BigDecimal, BigDecimal> par3 = Stream.of(rows)
                .parallel(4)
                .collect(Collectors.MoreCollectors.averagingBigDecimal(r -> BigDecimal.valueOf(r[0]), r -> BigDecimal.valueOf(r[1]),
                        r -> BigDecimal.valueOf(r[2])));
        assertBigAverage(7, seq3._1);
        assertBigAverage(70, seq3._2);
        assertBigAverage(700, seq3._3);
        assertEquals(seq3, par3);

        assertEquals(seq3, Stream.of(rows.iterator())
                .parallel(4)
                .collect(Collectors.MoreCollectors.averagingBigDecimal(r -> BigDecimal.valueOf(r[0]), r -> BigDecimal.valueOf(r[1]),
                        r -> BigDecimal.valueOf(r[2]))));

        assertEquals(Tuple3.of(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO), Stream.<int[]> empty()
                .parallel(4)
                .collect(Collectors.MoreCollectors.averagingBigDecimal(r -> BigDecimal.valueOf(r[0]), r -> BigDecimal.valueOf(r[1]),
                        r -> BigDecimal.valueOf(r[2]))));
    }

    // ---------------------------------------------------------------------------------------------------------
    // U17-03: LST/C-114 at the two try/finally terminals of EntryStream itself - join(delimiter, keyValueDelimiter,
    // prefix, suffix) and joinTo(Joiner). The primary failure wins, the close failure is its single suppressed
    // exception and the close handler runs exactly once.
    // ---------------------------------------------------------------------------------------------------------

    private static final class Boom extends RuntimeException {
        private static final long serialVersionUID = 1L;

        Boom(final String message) {
            super(message);
        }
    }

    private static EntryStream<String, Integer> failingOnB(final AtomicInteger closed, final Boom closeFailure) {
        return EntryStream.of("a", 1, "b", 2).onClose(() -> {
            closed.incrementAndGet();
            throw closeFailure;
        }).mapValue(v -> {
            if (v == 2) {
                throw new ArithmeticException("boom");
            }

            return v;
        });
    }

    private static void assertEntryTerminalKeepsThePrimaryFailure(final Consumer<EntryStream<String, Integer>> terminal, final String label) {
        final AtomicInteger closed = new AtomicInteger();
        final Boom closeFailure = new Boom("closeX");
        final EntryStream<String, Integer> es = failingOnB(closed, closeFailure);

        final ArithmeticException e = assertThrows(ArithmeticException.class, () -> terminal.accept(es), label);
        assertEquals("boom", e.getMessage(), label);
        assertEquals(1, e.getSuppressed().length, label);
        assertSame(closeFailure, e.getSuppressed()[0], label);
        assertEquals(1, closed.get(), label);

        // A later explicit close() must not run the (failing) handler again.
        es.close();
        assertEquals(1, closed.get(), label);
    }

    @Test
    public void testU1703_entryStreamJoinAndJoinToKeepThePrimaryFailure() {
        assertEntryTerminalKeepsThePrimaryFailure(es -> es.join(",", "=", "{", "}"), "join(4-arg)");
        assertEntryTerminalKeepsThePrimaryFailure(es -> es.joinTo(Joiner.with(",")), "joinTo");
        // The 1/2/3-arg joins delegate to the 4-arg one.
        assertEntryTerminalKeepsThePrimaryFailure(es -> es.join(","), "join(1-arg)");
        assertEntryTerminalKeepsThePrimaryFailure(es -> es.join(",", ":"), "join(2-arg)");
        assertEntryTerminalKeepsThePrimaryFailure(es -> es.join(",", "[", "]"), "join(3-arg)");
    }

    @Test
    public void testU1703_entryStreamJoinAndJoinToStillReportTheCloseFailureOnSuccess() {
        for (int variant = 0; variant < 2; variant++) {
            final AtomicInteger closed = new AtomicInteger();
            final Boom closeFailure = new Boom("closeX");
            final EntryStream<String, Integer> es = EntryStream.of("a", 1, "b", 2).onClose(() -> {
                closed.incrementAndGet();
                throw closeFailure;
            });

            final Boom e = variant == 0 ? assertThrows(Boom.class, () -> es.join(",", "=", "{", "}")) : assertThrows(Boom.class, () -> es.joinTo(Joiner.with(",")));
            assertSame(closeFailure, e);
            assertEquals(0, e.getSuppressed().length);
            assertEquals(1, closed.get());
        }
    }

    @Test
    public void testU1703_entryStreamJoinAndJoinToSuccessUnchanged() {
        final AtomicInteger closed = new AtomicInteger();
        assertEquals("{a=1,b=2}", EntryStream.of("a", 1, "b", 2).onClose(closed::incrementAndGet).join(",", "=", "{", "}"));
        assertEquals(1, closed.get());

        final Joiner joiner = Joiner.with(",");
        assertSame(joiner, EntryStream.of("a", 1, "b", 2).onClose(closed::incrementAndGet).joinTo(joiner));
        assertEquals("a=1,b=2", joiner.toString());
        assertEquals(2, closed.get());

        assertEquals("{}", EntryStream.<String, Integer> empty().onClose(closed::incrementAndGet).join(",", "=", "{", "}"));
        assertEquals("", EntryStream.<String, Integer> empty().onClose(closed::incrementAndGet).joinTo(Joiner.with(",")).toString());
        assertEquals(4, closed.get());
    }

    // ---------------------------------------------------------------------------------------------------------
    // U18-02: LST/C-001 lazy step(n) - the gap is skipped with ONE bulk advance() per taken element, never element
    // by element (the "lazy-step trap" of the ledger's integration log). A counting ObjIteratorEx source shows it.
    // ---------------------------------------------------------------------------------------------------------

    /** A source that counts every next() and every advance(n) call it receives. */
    private static final class CountingSource extends ObjIteratorEx<Integer> {
        final AtomicInteger advances = new AtomicInteger();
        final AtomicInteger nexts = new AtomicInteger();
        private final int size;
        private int cursor = 0;

        CountingSource(final int size) {
            this.size = size;
        }

        @Override
        public boolean hasNext() {
            return cursor < size;
        }

        @Override
        public Integer next() {
            if (cursor >= size) {
                throw new NoSuchElementException();
            }

            nexts.incrementAndGet();
            return cursor++;
        }

        @Override
        public void advance(final long n) {
            advances.incrementAndGet();
            cursor = (int) Math.min(size, cursor + n);
        }
    }

    @Test
    public void testU1802_stepSkipsTheGapWithOneBulkAdvance() {
        // step(1000).limit(3): 3 elements read, 2 gaps skipped - one advance(999) each, none after the last element.
        final CountingSource src = new CountingSource(100_000);
        assertEquals(Arrays.asList(0, 1000, 2000), Stream.of(src).step(1000).limit(3).toList());
        assertEquals(2, src.advances.get(), "one bulk advance per gap");
        assertEquals(3, src.nexts.get(), "only the taken elements are read");

        // step(1000).first(): the gap after the first element is never pulled at all.
        final CountingSource src2 = new CountingSource(100_000);
        assertEquals(Optional.of(0), Stream.of(src2).step(1000).first());
        assertEquals(0, src2.advances.get());
        assertEquals(1, src2.nexts.get());

        // Full traversal of a short source: the trailing gap is also one bulk advance (which exhausts the source).
        final CountingSource src3 = new CountingSource(10);
        assertEquals(Arrays.asList(0, 4, 8), Stream.of(src3).step(4).toList());
        assertEquals(3, src3.advances.get());
        assertEquals(3, src3.nexts.get());

        // skip() on the stepped stream re-applies the gap per skipped element (the wrapper's inherited advance
        // loops next()), but every gap is still one bulk advance on the source.
        final CountingSource src4 = new CountingSource(100_000);
        assertEquals(Arrays.asList(2000), Stream.of(src4).step(1000).skip(2).limit(1).toList());
        assertEquals(2, src4.advances.get());
        assertEquals(3, src4.nexts.get());

        // step(1) is the identity: no advance at all.
        final CountingSource src5 = new CountingSource(5);
        assertEquals(Arrays.asList(0, 1, 2, 3, 4), Stream.of(src5).step(1).toList());
        assertEquals(0, src5.advances.get());
        assertEquals(5, src5.nexts.get());

        // A gap larger than the rest of the source: one advance, then exhaustion - no extra read.
        final CountingSource src6 = new CountingSource(3);
        assertEquals(Arrays.asList(0), Stream.of(src6).step(Long.MAX_VALUE).toList());
        assertEquals(1, src6.advances.get());
        assertEquals(1, src6.nexts.get());
    }

    // ---------------------------------------------------------------------------------------------------------
    // U23-02: LST/C-017 sorted-shortcut tie walk - deterministic RESULT for ParallelArrayStream and for an
    // ArrayStream sub-range whose trailing equivalence run reaches fromIndex (the `idx > fromIndex` guard).
    // ---------------------------------------------------------------------------------------------------------

    private static final Comparator<String> BY_LEN = Comparator.comparingInt(String::length);

    @Test
    public void testU2302_parallelArrayStreamSortedMaxReturnsFirstOfTrailingRun() {
        // Flagged sorted by length on the sub-range [1, 5): a, bb, cc, dd - "bb" is the first of the maximal run.
        final String[] sorted = { "zzzzz", "a", "bb", "cc", "dd" };

        for (int i = 0; i < 20; i++) {
            final Stream<String> par = new ArrayStream<>(sorted, 1, 5, true, BY_LEN, null).parallel(2);
            assertTrue(par instanceof ParallelArrayStream, par.getClass().getName());
            assertEquals("bb", par.max(BY_LEN).get());
        }

        // Sequential control on the same sub-range, and min stays the first element.
        assertEquals("bb", new ArrayStream<>(sorted, 1, 5, true, BY_LEN, null).max(BY_LEN).get());
        assertEquals("a", new ArrayStream<>(sorted, 1, 5, true, BY_LEN, null).min(BY_LEN).get());
        assertEquals("a", new ArrayStream<>(sorted, 1, 5, true, BY_LEN, null).parallel(2).min(BY_LEN).get());

        // A different comparator does not take the shortcut: still the first maximal element ("zzzzz" excluded by the range).
        final Comparator<String> byLenCopy = Comparator.comparingInt(String::length);
        assertEquals("bb", new ArrayStream<>(sorted, 1, 5, true, BY_LEN, null).max(byLenCopy).get());
    }

    @Test
    public void testU2302_arrayStreamSortedMaxAllEqualRunReachesFromIndex() {
        // Distinct instances, so the RETURNED element (not merely an equal one) is asserted.
        final String[] allEqual = { "zz", new String(new char[] { 'a' }), new String(new char[] { 'a' }), new String(new char[] { 'a' }) };
        final AtomicInteger cmpCalls = new AtomicInteger();
        final Comparator<String> counting = (a, b) -> {
            cmpCalls.incrementAndGet();
            return Integer.compare(a.length(), b.length());
        };

        // Sub-range [1, 4) is an all-equal run: the walk stops at fromIndex, exactly n-1 comparisons.
        final Optional<String> seq = new ArrayStream<>(allEqual, 1, 4, true, counting, null).max(counting);
        assertSame(allEqual[1], seq.get());
        assertEquals(2, cmpCalls.get());

        cmpCalls.set(0);
        final Stream<String> par = new ArrayStream<>(allEqual, 1, 4, true, counting, null).parallel(2);
        assertTrue(par instanceof ParallelArrayStream, par.getClass().getName());
        final Optional<String> parMax = par.max(counting);
        assertSame(allEqual[1], parMax.get());
        assertEquals(2, cmpCalls.get());

        // Full array with a single maximal element at the end: one comparison, last element returned.
        cmpCalls.set(0);
        final String[] distinct = { "a", "bb", "ccc" };
        assertSame(distinct[2], new ArrayStream<>(distinct, 0, 3, true, counting, null).max(counting).get());
        assertEquals(1, cmpCalls.get());

        // Single-element and empty sub-ranges never compare.
        cmpCalls.set(0);
        assertSame(allEqual[3], new ArrayStream<>(allEqual, 3, 4, true, counting, null).max(counting).get());
        assertFalse(new ArrayStream<>(allEqual, 2, 2, true, counting, null).max(counting).isPresent());
        assertEquals(0, cmpCalls.get());
    }
}

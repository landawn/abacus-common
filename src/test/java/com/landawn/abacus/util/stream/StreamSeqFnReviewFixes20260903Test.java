package com.landawn.abacus.util.stream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collector;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.Duration;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.Seq;
import com.landawn.abacus.util.u.Optional;
import com.landawn.abacus.util.stream.Stream.WindowHandler;

/**
 * Cycle-2 regression tests of the iterative review of the stream family, {@code Seq}, {@code Collectors},
 * {@code Fn} and {@code Fnn} (ledger {@code scripts/cross_review/StreamFamily_Seq_Collectors_Fn_Fnn_ledger_2026-09-02.md}).
 *
 * <ul>
 *   <li><b>C-026</b> — {@code LongStream.range/rangeClosed} on spans of {@code 2^63} elements or more were a three-way
 *       {@code concat} whose iterator walked element by element on {@code skip}/{@code count}.</li>
 *   <li><b>C-027</b> — {@code EntryStream.prepend/append((EntryStream) null)} and a {@code null} result from an
 *       {@code appendIfEmpty} supplier threw {@code NullPointerException}.</li>
 *   <li><b>C-029</b> — a failing {@code Seq} close handler replaced the primary failure of the terminal operation.</li>
 *   <li><b>C-030</b> — {@code Seq.cycled(rounds)} over an empty source answered {@code hasNext() == true} after
 *       exhaustion and then threw {@code NoSuchElementException}.</li>
 *   <li><b>C-031</b> — {@code buffered()} dropped the elements the producer had already queued when it failed later.</li>
 *   <li><b>C-033</b> — the sliding event-time window checked only the timestamp of the LAST queued element, so with
 *       out-of-order arrivals whole windows made of queued elements were skipped.</li>
 *   <li><b>C-034</b> — iterator-backed windows waited out the full wall-clock window (or {@code maxWaitForNext}) after
 *       the source was exhausted, because the raw queue poll could not see the end of the source.</li>
 *   <li><b>C-036</b> — {@code toArray(IntFunction)} on an iterator-backed stream threw from {@code arraycopy} when the
 *       generator returned a short array.</li>
 *   <li><b>C-038</b> — {@code Stream.prepend/append((Optional) null)} threw a raw {@code NullPointerException}.</li>
 * </ul>
 */
@Tag("unit")
public class StreamSeqFnReviewFixes20260903Test extends TestBase {

    // ---------------------------------------------------------------- C-026: LongStream huge ranges

    private static final long MAX = Long.MAX_VALUE;
    private static final long MIN = Long.MIN_VALUE;

    /** The k-th element (0-based) of the arithmetic progression start + k * by, computed exactly. */
    private static long nth(final long start, final long by, final BigInteger k) {
        return BigInteger.valueOf(start).add(BigInteger.valueOf(by).multiply(k)).longValueExact();
    }

    @Test
    @org.junit.jupiter.api.Timeout(120) // pre-fix these walked 2^63 elements
    public void testC026_range_hugeSpan_skipIsConstantTime() {
        // range(MIN, MAX) has 2^64 - 1 elements. Two skips of MAX elements each leave exactly one: MAX - 1.
        assertArrayEquals(new long[] { MAX - 1 }, LongStream.range(MIN, MAX).skip(MAX).skip(MAX).toArray());
        assertArrayEquals(new long[] { MIN, MIN + 1, MIN + 2 }, LongStream.range(MIN, MAX).limit(3).toArray());

        final BigInteger k = BigInteger.valueOf(MAX).add(BigInteger.valueOf(5));
        assertEquals(nth(MIN, 1, k), LongStream.range(MIN, MAX).skip(MAX).skip(5).first().getAsLong());

        // rangeClosed(MIN, MAX) has 2^64 elements: two skips of MAX leave [MAX - 1, MAX].
        assertArrayEquals(new long[] { MAX - 1, MAX }, LongStream.rangeClosed(MIN, MAX).skip(MAX).skip(MAX).toArray());
        assertArrayEquals(new long[] { MAX - 2, MAX - 1, MAX }, LongStream.rangeClosed(0, MAX).skip(MAX - 2).toArray());
        assertArrayEquals(new long[] { MAX - 2, MAX - 1 }, LongStream.range(-1, MAX).skip(MAX - 1).toArray());
        assertArrayEquals(new long[] { MAX }, LongStream.rangeClosed(0, MAX).skip(MAX).toArray());
        assertEquals(0, LongStream.rangeClosed(0, MAX).skip(MAX).skip(1).count());

        // Descending, 2^64 - 1 and 2^64 elements.
        assertArrayEquals(new long[] { MIN + 1 }, LongStream.range(MAX, MIN, -1).skip(MAX).skip(MAX).toArray());
        assertArrayEquals(new long[] { MIN + 1, MIN }, LongStream.rangeClosed(MAX, MIN, -1).skip(MAX).skip(MAX).toArray());
        assertArrayEquals(new long[] { MAX, MAX - 1, MAX - 2 }, LongStream.range(MAX, MIN, -1).limit(3).toArray());
    }

    @Test
    @org.junit.jupiter.api.Timeout(120) // pre-fix these walked 2^63 elements
    public void testC026_range_hugeSpan_withStep_matchesBigIntegerReference() {
        final long[][] cases = { { MIN, MAX, 3 }, { MIN, MAX, 7 }, { MAX, MIN, -3 }, { MAX, MIN, -7 }, { MIN, MAX, 1L << 62 }, { MAX, MIN, -(1L << 62) },
                { MIN, MAX, MAX }, { MAX, MIN, MIN }, { -5, MAX, MAX }, { 5, MIN, MIN }, { 0, MIN, MIN }, { 0, MAX, MAX } };

        for (final long[] c : cases) {
            final long start = c[0], end = c[1], by = c[2];
            final BigInteger span = BigInteger.valueOf(end).subtract(BigInteger.valueOf(start)).abs();
            final BigInteger absBy = BigInteger.valueOf(by).abs();
            final BigInteger openCount = span.subtract(BigInteger.ONE).divide(absBy).add(BigInteger.ONE); // ceil(span / |by|)
            final BigInteger closedCount = span.divide(absBy).add(BigInteger.ONE);

            // first three elements
            final long[] first3 = LongStream.range(start, end, by).limit(3).toArray();
            for (int i = 0; i < first3.length; i++) {
                assertEquals(nth(start, by, BigInteger.valueOf(i)), first3[i], "range" + N.toString(c) + " #" + i);
            }

            // last element of the open and closed ranges, reached by skipping (never by walking)
            final long lastOpen = nth(start, by, openCount.subtract(BigInteger.ONE));
            final long lastClosed = nth(start, by, closedCount.subtract(BigInteger.ONE));

            assertEquals(lastOpen, skipAll(LongStream.range(start, end, by), openCount.subtract(BigInteger.ONE)).first().getAsLong(), "range" + N.toString(c));
            assertEquals(lastClosed, skipAll(LongStream.rangeClosed(start, end, by), closedCount.subtract(BigInteger.ONE)).first().getAsLong(),
                    "rangeClosed" + N.toString(c));
            assertEquals(0, skipAll(LongStream.range(start, end, by), openCount).count(), "range" + N.toString(c) + " exhausted");
            assertEquals(0, skipAll(LongStream.rangeClosed(start, end, by), closedCount).count(), "rangeClosed" + N.toString(c) + " exhausted");

            // count() is exact when it fits, and refuses to lie when it does not
            if (openCount.bitLength() < 64) {
                assertEquals(openCount.longValueExact(), LongStream.range(start, end, by).count(), "range" + N.toString(c) + " count");
            } else {
                assertThrows(ArithmeticException.class, () -> LongStream.range(start, end, by).count());
            }

            if (closedCount.bitLength() < 64) {
                assertEquals(closedCount.longValueExact(), LongStream.rangeClosed(start, end, by).count(), "rangeClosed" + N.toString(c) + " count");
            } else {
                assertThrows(ArithmeticException.class, () -> LongStream.rangeClosed(start, end, by).count());
            }
        }
    }

    private static LongStream skipAll(LongStream s, final BigInteger n) {
        BigInteger left = n;

        while (left.signum() > 0) {
            final long step = left.min(BigInteger.valueOf(MAX)).longValueExact();
            s = s.skip(step);
            left = left.subtract(BigInteger.valueOf(step));
        }

        return s;
    }

    @Test
    @org.junit.jupiter.api.Timeout(120) // pre-fix these walked 2^63 elements
    public void testC026_range_count_toArray_contracts() {
        assertEquals(MAX, LongStream.range(0, MAX).count());
        assertEquals(MAX, LongStream.rangeClosed(1, MAX).count());
        assertThrows(ArithmeticException.class, () -> LongStream.rangeClosed(0, MAX).count());
        assertThrows(ArithmeticException.class, () -> LongStream.range(-1, MAX).count());
        assertThrows(ArithmeticException.class, () -> LongStream.range(MIN, MAX).count());
        assertThrows(ArithmeticException.class, () -> LongStream.rangeClosed(MIN, MAX).count());
        assertThrows(IllegalStateException.class, () -> LongStream.range(MIN, MAX).toArray());
        assertThrows(IllegalStateException.class, () -> LongStream.rangeClosed(MIN, MAX).toArray());
        assertThrows(IllegalStateException.class, () -> LongStream.range(0, 1L << 40).toArray());
    }

    @Test
    public void testC026_range_smallSpans_unchanged() {
        assertArrayEquals(new long[] {}, LongStream.range(5, 5).toArray());
        assertArrayEquals(new long[] {}, LongStream.range(5, 1).toArray());
        assertArrayEquals(new long[] { 1, 2, 3, 4 }, LongStream.range(1, 5).toArray());
        assertArrayEquals(new long[] { 1, 2, 3, 4, 5 }, LongStream.rangeClosed(1, 5).toArray());
        assertArrayEquals(new long[] { 5 }, LongStream.rangeClosed(5, 5).toArray());
        assertArrayEquals(new long[] {}, LongStream.rangeClosed(5, 1).toArray());
        assertArrayEquals(new long[] { 0, 3, 6, 9 }, LongStream.range(0, 10, 3).toArray());
        assertArrayEquals(new long[] { 0, 3, 6, 9 }, LongStream.range(0, 12, 3).toArray());
        assertArrayEquals(new long[] { 0, 3, 6, 9 }, LongStream.rangeClosed(0, 9, 3).toArray());
        assertArrayEquals(new long[] { 0, 3, 6, 9 }, LongStream.rangeClosed(0, 10, 3).toArray());
        assertArrayEquals(new long[] { 10, 7, 4, 1 }, LongStream.range(10, 0, -3).toArray());
        assertArrayEquals(new long[] { 10, 7, 4, 1 }, LongStream.rangeClosed(10, 1, -3).toArray());
        assertArrayEquals(new long[] { 10, 7, 4, 1 }, LongStream.rangeClosed(10, 0, -3).toArray());
        assertArrayEquals(new long[] {}, LongStream.range(0, 10, -1).toArray());
        assertArrayEquals(new long[] {}, LongStream.rangeClosed(0, 10, -1).toArray());
        assertArrayEquals(new long[] { 5 }, LongStream.rangeClosed(5, 5, -1).toArray());
        assertArrayEquals(new long[] { 0 }, LongStream.range(0, MIN, MIN).toArray());
        assertArrayEquals(new long[] { 0, MIN }, LongStream.rangeClosed(0, MIN, MIN).toArray());
        assertArrayEquals(new long[] { 0, MAX }, LongStream.rangeClosed(0, MAX, MAX).toArray());
        assertArrayEquals(new long[] { 0 }, LongStream.range(0, MAX, MAX).toArray());
        assertThrows(IllegalArgumentException.class, () -> LongStream.range(0, 10, 0));
        assertThrows(IllegalArgumentException.class, () -> LongStream.rangeClosed(0, 10, 0));

        // skip / count / limit inside a small range
        assertArrayEquals(new long[] { 7, 9 }, LongStream.range(1, 10, 2).skip(3).toArray());
        assertEquals(2, LongStream.range(1, 10, 2).skip(3).count());
        assertEquals(0, LongStream.range(1, 10, 2).skip(99).count());
        assertArrayEquals(new long[] { 4, 5 }, LongStream.rangeClosed(1, 5).skip(3).toArray());
        assertEquals(5, LongStream.rangeClosed(1, 5).skip(0).count());
    }

    // ---------------------------------------------------------------- C-027: EntryStream null argument / null supplier result

    @Test
    public void testC027_entryStream_prependAppendNullStream_isNoOp() {
        assertEquals(List.of("a"), EntryStream.of("a", 1).prepend((EntryStream<String, Integer>) null).keys().toList());
        assertEquals(List.of("a"), EntryStream.of("a", 1).append((EntryStream<String, Integer>) null).keys().toList());
        assertEquals(List.of("x", "a"), EntryStream.of("a", 1).prepend(EntryStream.of("x", 0)).keys().toList());
        assertEquals(List.of("a", "x"), EntryStream.of("a", 1).append(EntryStream.of("x", 0)).keys().toList());
        assertEquals(List.of(1, 2), Stream.of(1, 2).prepend((Stream<Integer>) null).toList());
    }

    @Test
    public void testC027_appendIfEmpty_nullSupplierResult_isEmpty() {
        final java.util.function.Supplier<EntryStream<String, Integer>> nullSupplier = () -> null;
        final java.util.function.Supplier<EntryStream<String, Integer>> xSupplier = () -> EntryStream.of("x", 0);
        assertEquals(0, EntryStream.<String, Integer> empty().appendIfEmpty(nullSupplier).count());
        assertEquals(Map.of("a", 1), EntryStream.of("a", 1).appendIfEmpty(nullSupplier).toMap());
        assertEquals(Map.of("x", 0), EntryStream.<String, Integer> empty().appendIfEmpty(xSupplier).toMap());
        assertEquals(0, Stream.<Integer> empty().appendIfEmpty(() -> null).count()); // IteratorStream
        assertEquals(0, Stream.of(new Integer[0]).appendIfEmpty(() -> null).count()); // ArrayStream
        assertEquals(List.of(1), Stream.of(1).appendIfEmpty(() -> null).toList());
        assertEquals(List.of(9), Stream.of(new Integer[0]).appendIfEmpty(() -> Stream.of(9)).toList());
    }

    // ---------------------------------------------------------------- C-029: Seq close-handler failure must not mask the primary failure

    @Test
    public void testC029_seqTerminalFailure_closeHandlerFailureIsSuppressed() {
        final AtomicInteger closed = new AtomicInteger();

        final IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> Seq.<Integer, Exception> of(1, 2).map(x -> {
            throw new IllegalArgumentException("elem-fail");
        }).onClose(() -> {
            closed.incrementAndGet();
            throw new IllegalStateException("close-fail");
        }).toList());

        assertEquals("elem-fail", e.getMessage());
        assertEquals(1, e.getSuppressed().length);
        assertEquals("close-fail", e.getSuppressed()[0].getMessage());
        assertEquals(1, closed.get(), "close handlers run exactly once");
    }

    @Test
    public void testC029_seqCheckedTerminalFailure_closeHandlerFailureIsSuppressed() {
        final IOException e = assertThrows(IOException.class, () -> Seq.<Integer, IOException> of(1, 2).map(x -> {
            throw new IOException("io-fail");
        }).onClose(() -> {
            throw new IllegalStateException("close-fail");
        }).forEach(x -> {
        }));

        assertEquals("io-fail", e.getMessage());
        assertEquals(1, e.getSuppressed().length);
        assertEquals("close-fail", e.getSuppressed()[0].getMessage());
    }

    @Test
    public void testC029_seqArgumentCheckFailure_closeHandlerFailureIsSuppressed() {
        final IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> Seq.<Integer, Exception> of(1, 2).onClose(() -> {
            throw new IllegalStateException("close-fail");
        }).filter(null));

        assertTrue(e.getMessage().contains("predicate"), e.getMessage());
        assertEquals(1, e.getSuppressed().length);
        assertEquals("close-fail", e.getSuppressed()[0].getMessage());
    }

    @Test
    public void testC029_seqBufferingIntermediateFailure_closeHandlerFailureIsSuppressed() {
        // takeLast() drains the source inside an intermediate iterator that closes the Seq itself.
        final IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> Seq.<Integer, Exception> of(1, 2, 3).map(x -> {
            if (x == 3) {
                throw new IllegalArgumentException("elem-fail");
            }
            return x;
        }).onClose(() -> {
            throw new IllegalStateException("close-fail");
        }).takeLast(1).toList());

        assertEquals("elem-fail", e.getMessage());
        assertEquals(1, e.getSuppressed().length);
    }

    @Test
    public void testC029_seqSuccessfulTerminal_closeHandlerFailurePropagatesAlone() {
        final IllegalStateException e = assertThrows(IllegalStateException.class, () -> Seq.<Integer, Exception> of(1, 2).onClose(() -> {
            throw new IllegalStateException("close-fail");
        }).toList());

        assertEquals("close-fail", e.getMessage());
        assertEquals(0, e.getSuppressed().length);
    }

    @Test
    public void testC029_seqTwoFailingCloseHandlers_bothReachTheCaller() {
        final IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> Seq.<Integer, Exception> of(1).map(x -> {
            throw new IllegalArgumentException("elem-fail");
        }).onClose(() -> {
            throw new IllegalStateException("close-1");
        }).onClose(() -> {
            throw new IllegalStateException("close-2");
        }).count());

        assertEquals("elem-fail", e.getMessage());
        assertEquals(1, e.getSuppressed().length);
        assertEquals("close-1", e.getSuppressed()[0].getMessage());
        assertEquals(1, e.getSuppressed()[0].getSuppressed().length);
        assertEquals("close-2", e.getSuppressed()[0].getSuppressed()[0].getMessage());
    }

    // ---------------------------------------------------------------- C-030: Seq.cycled(rounds) over an empty source

    @Test
    public void testC030_seqCycled_emptySource() throws Exception {
        for (int rounds = 0; rounds <= 4; rounds++) {
            assertEquals(List.of(), Seq.<Integer, Exception> empty().cycled(rounds).skip(1).toList(), "rounds=" + rounds);
            assertEquals(List.of(), Seq.<Integer, Exception> empty().cycled(rounds).toList(), "rounds=" + rounds);
            assertEquals(List.of(), Seq.<Integer, Exception> of(1, 2).filter(x -> false).cycled(rounds).skip(1).toList(), "rounds=" + rounds);
            assertEquals(0, Seq.<Integer, Exception> empty().cycled(rounds).count(), "rounds=" + rounds);
        }

        // Operators that ask hasNext() repeatedly after exhaustion (merge, skip) see a consistent answer.
        assertEquals(List.of(1),
                Seq.merge(Seq.<Integer, Exception> empty().cycled(3), Seq.<Integer, Exception> of(1), (a, b) -> com.landawn.abacus.util.MergeResult.TAKE_FIRST)
                        .toList());
        assertEquals(List.of(), Seq.<Integer, Exception> empty().cycled(3).skip(2).skip(1).toList());

        // Non-empty sources are unchanged.
        assertEquals(List.of(1, 2, 1, 2, 1, 2), Seq.<Integer, Exception> of(1, 2).cycled(3).toList());
        assertEquals(List.of(2, 1, 2, 1, 2), Seq.<Integer, Exception> of(1, 2).cycled(3).skip(1).toList());
        assertEquals(List.of(), Seq.<Integer, Exception> of(1, 2).cycled(0).toList());
        assertEquals(List.of(1, 2), Seq.<Integer, Exception> of(1, 2).cycled(1).toList());
        assertEquals(List.of(), Stream.<Integer> empty().cycled(3).skip(1).toList());
    }

    // ---------------------------------------------------------------- C-031: buffered() keeps what the producer queued before failing

    @Test
    public void testC031_seqBuffered_elementsQueuedBeforeProducerFailureAreDelivered() throws Exception {
        final List<Integer> consumed = Collections.synchronizedList(new ArrayList<>());

        final IllegalStateException e = assertThrows(IllegalStateException.class, () -> {
            Seq.<Integer, Exception> of(1, 2, 3, 4, 5).map(x -> {
                if (x == 5) {
                    throw new IllegalStateException("boom");
                }
                return x;
            }).buffered(64).forEach(x -> {
                if (x == 1) {
                    Thread.sleep(300); // let the producer run ahead and fail while the queue still holds 2, 3, 4
                }

                consumed.add(x);
            });
        });

        assertEquals("boom", e.getMessage());
        assertEquals(List.of(1, 2, 3, 4), consumed);
    }

    @Test
    public void testC031_streamBuffered_elementsQueuedBeforeProducerFailureAreDelivered() throws Exception {
        final List<Integer> consumed = Collections.synchronizedList(new ArrayList<>());

        final IllegalStateException e = assertThrows(IllegalStateException.class, () -> {
            Stream.of(List.of(1, 2, 3, 4, 5)).map(x -> {
                if (x == 5) {
                    throw new IllegalStateException("boom");
                }
                return x;
            }).buffered(64).forEach(x -> {
                if (x == 1) {
                    N.sleep(300); // the producer only starts with the first pull: let it run ahead and fail now
                }

                consumed.add(x);
            });
        });

        assertEquals("boom", e.getMessage());
        assertEquals(List.of(1, 2, 3, 4), consumed);
    }

    // ---------------------------------------------------------------- C-033: out-of-order event times in the sliding window

    private record Ev(int id, long ts) {
    }

    private static Collector<Ev, ?, List<Integer>> ids() {
        return Collectors.mapping(Ev::id, Collectors.toList());
    }

    @Test
    public void testC033_slidingWindow_outOfOrderEventTimes_noWindowSkipped() {
        final long base = System.currentTimeMillis() - 3_600_000L;
        // arrival order 0 s, 2.5 s, 1.5 s: the last queued element does not carry the greatest timestamp
        final List<Ev> evs = List.of(new Ev(0, base), new Ev(25, base + 2_500), new Ev(15, base + 1_500));
        final WindowHandler<Ev, List<Integer>> handler = WindowHandler.<Ev, List<Integer>> builder().timeExtractor(Ev::ts).build();

        // 3 s / 1 s: [0,3) -> 0,25,15 ; [1,4) -> 25,15 ; [2,5) -> 25
        final List<List<Integer>> windows = Stream.of(evs).window(Duration.ofMillis(3_000), Duration.ofMillis(1_000), () -> base, handler, ids()).toList();
        assertEquals(List.of(List.of(0, 25, 15), List.of(25, 15), List.of(25)), windows);

        // In-order arrival of the same timestamps gives the same windows (modulo intra-window order).
        final List<Ev> ordered = List.of(new Ev(0, base), new Ev(15, base + 1_500), new Ev(25, base + 2_500));
        assertEquals(List.of(List.of(0, 15, 25), List.of(15, 25), List.of(25)),
                Stream.of(ordered).window(Duration.ofMillis(3_000), Duration.ofMillis(1_000), () -> base, handler, ids()).toList());

        // With a trailing element far ahead, the [2,5) window made only of the queued 2.5 s element is still emitted.
        final List<Ev> withTail = List.of(new Ev(0, base), new Ev(25, base + 2_500), new Ev(15, base + 1_500), new Ev(100, base + 10_000));
        final List<List<Integer>> windows2 = Stream.of(withTail)
                .window(Duration.ofMillis(3_000), Duration.ofMillis(1_000), () -> base, handler, ids())
                .toList();
        assertEquals(List.of(List.of(0, 25, 15), List.of(25, 15), List.of(25), List.of(100), List.of(100), List.of(100)), windows2);

        // Same through the iterator (async) path.
        assertEquals(windows2, Stream.of(withTail.iterator()).window(Duration.ofMillis(3_000), Duration.ofMillis(1_000), () -> base, handler, ids()).toList());
    }

    // ---------------------------------------------------------------- C-034: windows end promptly once the source is exhausted

    @Test
    public void testC034_iteratorBackedWindows_returnPromptlyAfterSourceExhausted() {
        final List<Integer> list = List.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);

        long t0 = System.currentTimeMillis();
        List<List<Integer>> w = Stream.of(list).window(Duration.ofSeconds(30), 3).toList();
        long elapsed = System.currentTimeMillis() - t0;
        assertEquals(List.of(List.of(1, 2, 3), List.of(4, 5, 6), List.of(7, 8, 9), List.of(10)), w);
        assertTrue(elapsed < 5_000, "bounded window took " + elapsed + " ms");

        t0 = System.currentTimeMillis();
        w = Stream.of(list.iterator()).window(Duration.ofSeconds(30)).toList();
        elapsed = System.currentTimeMillis() - t0;
        assertEquals(List.of(list), w);
        assertTrue(elapsed < 5_000, "tumbling window took " + elapsed + " ms");

        t0 = System.currentTimeMillis();
        w = Stream.of(list.iterator()).window(Duration.ofSeconds(30), Duration.ofSeconds(10), Collectors.toList()).toList();
        elapsed = System.currentTimeMillis() - t0;
        assertEquals(List.of(list), w);
        assertTrue(elapsed < 5_000, "sliding window took " + elapsed + " ms");

        t0 = System.currentTimeMillis();
        final List<List<Integer>> w2 = Stream.of(list.iterator()).window((first, last, cnt) -> 30_000L, (first, last, next, cnt) -> cnt < 4).toList();
        elapsed = System.currentTimeMillis() - t0;
        assertEquals(List.of(List.of(1, 2, 3, 4), List.of(5, 6, 7, 8), List.of(9, 10)), w2);
        assertTrue(elapsed < 5_000, "splitter window took " + elapsed + " ms");

        // A failing source surfaces its exception promptly too.
        t0 = System.currentTimeMillis();
        assertThrows(IllegalStateException.class, () -> Stream.of(list.iterator()).map(x -> {
            if (x == 3) {
                throw new IllegalStateException("boom");
            }
            return x;
        }).window(Duration.ofSeconds(30), 100).toList());
        elapsed = System.currentTimeMillis() - t0;
        assertTrue(elapsed < 5_000, "failure took " + elapsed + " ms");
    }

    @Test
    public void testC034_earlyClose_stopsTheBufferingThread() {
        // An infinite iterator source; the window stream is closed after the first window. The buffering thread
        // must stop pulling shortly afterwards (it is released through closeResource()).
        final AtomicInteger pulled = new AtomicInteger();
        final List<List<Integer>> w = Stream.iterate(0, x -> x + 1).onEach(x -> pulled.incrementAndGet()).window(Duration.ofSeconds(30), 3).limit(1).toList();

        assertEquals(List.of(List.of(0, 1, 2)), w);
        N.sleep(200);
        final int afterClose = pulled.get();
        N.sleep(300);
        assertEquals(afterClose, pulled.get(), "the buffering thread kept pulling after the window stream was closed");
    }

    @Test
    public void testC034_processingTimeWindow_stillClosesOnTheClock() {
        // A slow live source: elements 250 ms apart, 400 ms windows -> at least two windows, none empty.
        final AtomicInteger n = new AtomicInteger();
        final List<List<Integer>> w = Stream.generate(() -> {
            N.sleep(250);
            return n.incrementAndGet();
        }).limit(4).window(Duration.ofMillis(400)).toList();

        final List<Integer> flat = new ArrayList<>();
        w.forEach(flat::addAll);
        assertEquals(List.of(1, 2, 3, 4), flat);
        assertTrue(w.size() >= 2, w.toString());
        assertTrue(w.stream().noneMatch(List::isEmpty), w.toString());
    }

    // ---------------------------------------------------------------- C-036: toArray(IntFunction) with a short generator array

    @Test
    public void testC036_toArrayWithGenerator_shortArrayIsGrown() {
        assertArrayEquals(new Integer[] { 1, 2, 3 }, Stream.of(1, 2, 3).toArray(len -> new Integer[1]));
        assertArrayEquals(new Integer[] { 1, 2, 3 }, Stream.of(List.of(1, 2, 3).iterator()).toArray(len -> new Integer[1]));
        assertArrayEquals(new String[] { "a", "b" }, Stream.from(java.util.stream.Stream.of("a", "b")).toArray(len -> new String[0]));
        final Integer[] exact = Stream.of(List.of(1, 2, 3).iterator()).toArray(Integer[]::new);
        assertArrayEquals(new Integer[] { 1, 2, 3 }, exact);
        assertEquals(Integer[].class, Stream.of(List.of(1).iterator()).toArray(len -> new Integer[0]).getClass());
        assertArrayEquals(new Integer[0], Stream.<Integer> empty().toArray(len -> new Integer[0]));
    }

    // ---------------------------------------------------------------- C-038: Stream.prepend/append((Optional) null)

    @Test
    public void testC038_streamPrependAppendNullOptional_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> Stream.of(1, 2).prepend((Optional<Integer>) null));
        assertThrows(IllegalArgumentException.class, () -> Stream.of(1, 2).append((Optional<Integer>) null));
        assertThrows(IllegalArgumentException.class, () -> Stream.of(List.of(1, 2).iterator()).prepend((Optional<Integer>) null));
        assertEquals(List.of(0, 1, 2), Stream.of(1, 2).prepend(Optional.of(0)).toList());
        assertEquals(List.of(1, 2, 3), Stream.of(1, 2).append(Optional.of(3)).toList());
        assertEquals(List.of(1, 2), Stream.of(1, 2).prepend(Optional.<Integer> empty()).toList());
        assertEquals(List.of(1, 2), Stream.of(1, 2).append(Optional.<Integer> empty()).toList());
    }
}

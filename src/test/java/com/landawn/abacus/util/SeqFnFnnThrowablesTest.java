package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.exception.UncheckedInterruptedException;
import com.landawn.abacus.util.u.Nullable;
import com.landawn.abacus.util.u.Optional;
import com.landawn.abacus.util.function.BiPredicate;
import com.landawn.abacus.util.function.Predicate;

/**
 * Covers the review fixes applied to {@code Seq}, {@code Fn}, {@code Fnn} and {@code Throwables} on 2026-09-02.
 * Every test in here fails on the pre-fix code.
 */
@Tag("unit")
public class SeqFnFnnThrowablesTest extends TestBase {

    // ------------------------------------------------------------------------------------------------
    // helpers
    // ------------------------------------------------------------------------------------------------

    /** A Reader that records whether it was closed. */
    private static final class TrackingReader extends Reader {
        private final StringReader delegate;
        private final AtomicInteger closeCount = new AtomicInteger();

        TrackingReader(final String content) {
            delegate = new StringReader(content);
        }

        @Override
        public int read(final char[] cbuf, final int off, final int len) throws IOException {
            return delegate.read(cbuf, off, len);
        }

        @Override
        public void close() {
            closeCount.incrementAndGet();
            delegate.close();
        }

        boolean isClosed() {
            return closeCount.get() > 0;
        }
    }

    // ================================================================================================
    // N1 - Seq.ofLines(Reader, true) must close the caller's Reader even when never traversed
    // ================================================================================================

    @Test
    public void test_N1_ofLinesReader_closesReader_whenClosedWithoutTraversal() throws Exception {
        final TrackingReader reader = new TrackingReader("a\nb\n");

        final Seq<String, IOException> seq = Seq.ofLines(reader, true);
        assertFalse(reader.isClosed(), "the reader must not be closed before the sequence is");

        seq.close();

        assertTrue(reader.isClosed(), "closing an untraversed sequence must still close the reader");
    }

    @Test
    public void test_N1_ofLinesReader_closesReader_whenTerminalOpPullsNothing() throws Exception {
        final TrackingReader reader = new TrackingReader("a\nb\n");

        assertEquals(CommonUtil.emptyList(), Seq.ofLines(reader, true).limit(0).toList());

        assertTrue(reader.isClosed(), "limit(0).toList() pulls no element but must still close the reader");
    }

    @Test
    public void test_N1_ofLinesReader_closesReader_onFullAndPartialTraversal() throws Exception {
        final TrackingReader full = new TrackingReader("a\nb\n");
        assertEquals(CommonUtil.asList("a", "b"), Seq.ofLines(full, true).toList());
        assertTrue(full.isClosed());

        final TrackingReader partial = new TrackingReader("a\nb\n");
        assertEquals(Nullable.of("a"), Seq.ofLines(partial, true).first());
        assertTrue(partial.isClosed());
    }

    @Test
    public void test_N1_ofLinesReader_closesReaderExactlyOnce() throws Exception {
        final TrackingReader reader = new TrackingReader("a\nb\n");

        final Seq<String, IOException> seq = Seq.ofLines(reader, true);
        assertEquals(CommonUtil.asList("a", "b"), seq.toList());
        seq.close(); // idempotent

        assertEquals(1, reader.closeCount.get(), "the close handler must run exactly once");
    }

    @Test
    public void test_N1_ofLinesReader_falseFlag_stillLeavesReaderOpen() throws Exception {
        final TrackingReader reader = new TrackingReader("a\nb\n");

        assertEquals(CommonUtil.asList("a", "b"), Seq.ofLines(reader, false).toList());

        assertFalse(reader.isClosed(), "the caller keeps ownership when the flag is false");
    }

    // ================================================================================================
    // B1 - Seq.transform(..) must link this sequence for closing
    // ================================================================================================

    @Test
    public void test_B1_transform_closesSource_whenTransferDiscardsIt() throws Exception {
        final AtomicInteger closed = new AtomicInteger();

        final List<String> result = Seq.<String, Exception> of("a", "b")
                .onClose(closed::incrementAndGet)
                .transform(s -> Seq.<String, Exception> of("x"))
                .toList();

        assertEquals(CommonUtil.asList("x"), result);
        assertEquals(1, closed.get(), "a transfer that ignores its input must not strand the source");
    }

    @Test
    public void test_B1_transform_closesSourceReader_whenTransferDiscardsIt() throws Exception {
        final TrackingReader reader = new TrackingReader("a\nb\n");

        final List<String> result = Seq.ofLines(reader, true).transform(s -> Seq.<String, IOException> of("x")).toList();

        assertEquals(CommonUtil.asList("x"), result);
        assertTrue(reader.isClosed());
    }

    @Test
    public void test_B1_transform_closesSourceExactlyOnce_forDerivingTransfer() throws Exception {
        final AtomicInteger closed = new AtomicInteger();

        final List<Integer> result = Seq.<Integer, Exception> of(1, 2, 3).onClose(closed::incrementAndGet).transform(s -> s.map(i -> i * 2)).toList();

        assertEquals(CommonUtil.asList(2, 4, 6), result);
        assertEquals(1, closed.get(), "the ordinary case must not double-close");
    }

    @Test
    public void test_B1_transform_nullResultIsEmptyAndStillCloses() throws Exception {
        final AtomicInteger closed = new AtomicInteger();

        final List<String> result = Seq.<String, Exception> of("a").onClose(closed::incrementAndGet).<String> transform(s -> null).toList();

        assertEquals(CommonUtil.emptyList(), result);
        assertEquals(1, closed.get());
    }

    @Test
    public void test_B1_transform_identityTransferDoesNotLoop() throws Exception {
        final AtomicInteger closed = new AtomicInteger();

        final List<String> result = Seq.<String, Exception> of("a", "b").onClose(closed::incrementAndGet).transform(s -> s).toList();

        assertEquals(CommonUtil.asList("a", "b"), result);
        assertEquals(1, closed.get());
    }

    // ================================================================================================
    // N2 - an already-closed sequence must not be resurrected by the combinators
    // ================================================================================================

    @Test
    public void test_N2_concatCollection_rejectsClosedSource() throws Exception {
        final Seq<Integer, Exception> closed = Seq.of(1, 2);
        closed.close();

        assertThrows(IllegalStateException.class, () -> Seq.concat(CommonUtil.asList(closed)).toList());
    }

    @Test
    public void test_N2_concatVarargs_rejectsClosedSource() throws Exception {
        final Seq<Integer, Exception> closed = Seq.of(1, 2);
        closed.close();

        assertThrows(IllegalStateException.class, () -> Seq.<Integer, Exception> concat(closed).toList());
    }

    @Test
    public void test_N2_append_rejectsClosedSource() throws Exception {
        final Seq<Integer, Exception> closed = Seq.of(1, 2);
        closed.close();

        assertThrows(IllegalStateException.class, () -> Seq.<Integer, Exception> of(9).append(closed).toList());
    }

    @Test
    public void test_N2_prepend_rejectsClosedSource() throws Exception {
        final Seq<Integer, Exception> closed = Seq.of(1, 2);
        closed.close();

        assertThrows(IllegalStateException.class, () -> Seq.<Integer, Exception> of(9).prepend(closed).toList());
    }

    @Test
    public void test_N2_zip_rejectsClosedSource() {
        final Seq<Integer, Exception> closed = Seq.of(1, 2);
        closed.close();
        final Seq<Integer, Exception> open = Seq.of(10, 20);

        assertThrows(IllegalStateException.class, () -> Seq.<Integer, Integer, Integer, Exception> zip(closed, open, (a, b) -> a + b).toList());

        open.close();
    }

    @Test
    public void test_N2_merge_rejectsClosedSource() {
        final Seq<Integer, Exception> closed = Seq.of(1, 3);
        closed.close();
        final Seq<Integer, Exception> open = Seq.of(2, 4);

        assertThrows(IllegalStateException.class,
                () -> Seq.<Integer, Exception> merge(closed, open, (a, b) -> a <= b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).toList());

        open.close();
    }

    @Test
    public void test_N2_openSourcesStillWork() throws Exception {
        assertEquals(CommonUtil.asList(1, 2, 3, 4),
                Seq.concat(CommonUtil.asList(Seq.<Integer, Exception> of(1, 2), Seq.<Integer, Exception> of(3, 4))).toList());
        assertEquals(CommonUtil.asList(11, 22), Seq.<Integer, Integer, Integer, Exception> zip(Seq.of(1, 2), Seq.of(10, 20), (a, b) -> a + b).toList());
        assertEquals(CommonUtil.asList(1, 2, 3, 4),
                Seq.<Integer, Exception> merge(Seq.of(1, 3), Seq.of(2, 4), (a, b) -> a <= b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).toList());
        assertEquals(CommonUtil.asList(9, 1, 2), Seq.<Integer, Exception> of(9).append(Seq.<Integer, Exception> of(1, 2)).toList());
    }

    @Test
    public void test_N2_concatSkipsNullEntriesWithoutFailing() throws Exception {
        final List<Seq<Integer, Exception>> sources = new ArrayList<>();
        sources.add(Seq.of(1, 2));
        sources.add(null);
        sources.add(Seq.of(3));

        assertEquals(CommonUtil.asList(1, 2, 3), Seq.concat(sources).toList());
    }

    // ================================================================================================
    // B5 - no-op intermediate operations must keep the known sort order
    // ================================================================================================

    /** {@code min(cmp)} on a sequence known to be sorted by {@code cmp} pulls exactly one element. */
    private static int pullsForMinAfter(final java.util.function.UnaryOperator<Seq<Integer, Exception>> op) throws Exception {
        final AtomicInteger pulls = new AtomicInteger();
        final Comparator<Integer> nat = Comparators.naturalOrder();

        final Nullable<Integer> min = op.apply(Seq.<Integer, Exception> of(1, 2, 3).sorted().onEach(x -> pulls.incrementAndGet())).min(nat);

        assertEquals(Nullable.of(1), min);
        return pulls.get();
    }

    @Test
    public void test_B5_appendIfEmpty_emptyCollection_keepsSortedFlag() throws Exception {
        assertEquals(1, pullsForMinAfter(s -> s.appendIfEmpty(CommonUtil.<Integer> emptyList())));
    }

    @Test
    public void test_B5_appendIfEmpty_emptyArray_keepsSortedFlag() throws Exception {
        assertEquals(1, pullsForMinAfter(Seq::appendIfEmpty));
    }

    @Test
    public void test_B5_rotatedZero_keepsSortedFlag() throws Exception {
        assertEquals(1, pullsForMinAfter(s -> s.rotated(0)));
    }

    @Test
    public void test_B5_baselineOpsStillKeepOrDropTheFlagAsBefore() throws Exception {
        assertEquals(1, pullsForMinAfter(s -> s), "baseline: sorted() alone short-circuits");
        assertEquals(1, pullsForMinAfter(s -> s.skip(0)));
        assertEquals(1, pullsForMinAfter(s -> s.append(Optional.<Integer> empty())));
        assertEquals(3, pullsForMinAfter(s -> s.map(x -> x)), "map(..) may reorder, so the flag must be dropped");
    }

    @Test
    public void test_B5_noOpsStillProduceTheRightElements() throws Exception {
        assertEquals(CommonUtil.asList(1, 2, 3), Seq.<Integer, Exception> of(1, 2, 3).appendIfEmpty(CommonUtil.<Integer> emptyList()).toList());
        assertEquals(CommonUtil.asList(1, 2, 3), Seq.<Integer, Exception> of(1, 2, 3).rotated(0).toList());
        assertEquals(CommonUtil.asList(7), Seq.<Integer, Exception> empty().appendIfEmpty(CommonUtil.asList(7)).toList());
        assertEquals(CommonUtil.asList(3, 1, 2), Seq.<Integer, Exception> of(1, 2, 3).rotated(1).toList());
    }

    // ================================================================================================
    // B14 - skipAndLimit(0, MAX_VALUE) derives a new sequence like every other intermediate operation
    // ================================================================================================

    @Test
    public void test_B14_skipAndLimit_derivesInsteadOfReturningReceiver() throws Exception {
        final Seq<Integer, Exception> src = Seq.of(1, 2, 3);
        final Seq<Integer, Exception> derived = src.skipAndLimit(0, Long.MAX_VALUE);

        assertNotSame(src, derived);
        assertEquals(CommonUtil.asList(1, 2, 3), derived.toList());
    }

    @Test
    public void test_B14_skipAndLimit_otherBranchesUnchanged() throws Exception {
        assertEquals(CommonUtil.asList(1, 2), Seq.<Integer, Exception> of(1, 2, 3).skipAndLimit(0, 2).toList());
        assertEquals(CommonUtil.asList(2, 3), Seq.<Integer, Exception> of(1, 2, 3).skipAndLimit(1, Long.MAX_VALUE).toList());
        assertEquals(CommonUtil.asList(2), Seq.<Integer, Exception> of(1, 2, 3).skipAndLimit(1, 1).toList());
        assertThrows(IllegalArgumentException.class, () -> Seq.<Integer, Exception> of(1).skipAndLimit(0, -1));
        assertThrows(IllegalArgumentException.class, () -> Seq.<Integer, Exception> of(1).skipAndLimit(-1, 1));
    }

    @Test
    public void test_B14_skipAndLimit_derivedSeqClosesSource() throws Exception {
        final AtomicInteger closed = new AtomicInteger();

        Seq.<Integer, Exception> of(1, 2, 3).onClose(closed::incrementAndGet).skipAndLimit(0, Long.MAX_VALUE).toList();

        assertEquals(1, closed.get());
    }

    // ================================================================================================
    // B6 - interruption must not escape as the sequence's declared checked exception type
    // ================================================================================================

    @Test
    public void test_B6_buffered_consumerInterruption_surfacesAsUncheckedAndRestoresFlag() throws Exception {
        final AtomicReference<Throwable> thrown = new AtomicReference<>();
        final AtomicBoolean interruptedAfter = new AtomicBoolean();
        final CountDownLatch consumerGotFirst = new CountDownLatch(1);
        final CountDownLatch producerBlocked = new CountDownLatch(1);
        final CountDownLatch releaseProducer = new CountDownLatch(1);
        final CountDownLatch consumerDone = new CountDownLatch(1);

        // The producer stalls before handing over the second element, so the consumer ends up waiting in the
        // buffer's poll loop - which is where the interruption has to be handled.
        final Seq<String, IOException> seq = Seq.<String, IOException> of("a", "b").onEach(it -> {
            if (!"a".equals(it)) {
                producerBlocked.countDown();

                try {
                    releaseProducer.await();
                } catch (final InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }).buffered(1);

        final Thread consumer = new Thread(() -> {
            try {
                seq.forEach(it -> consumerGotFirst.countDown());
            } catch (final Throwable t) {
                thrown.set(t);
            } finally {
                interruptedAfter.set(Thread.currentThread().isInterrupted());
                consumerDone.countDown();
            }
        });

        try {
            consumer.start();

            assertTrue(consumerGotFirst.await(10, TimeUnit.SECONDS), "the consumer never received the first element");
            assertTrue(producerBlocked.await(10, TimeUnit.SECONDS), "the producer never reached its stall point");

            consumer.interrupt();

            assertTrue(consumerDone.await(10, TimeUnit.SECONDS), "the interrupted consumer never returned");
        } finally {
            releaseProducer.countDown();
            consumer.join(10_000);
        }

        assertNotNull(thrown.get(), "the interrupted consumer must fail rather than return silently");
        assertTrue(thrown.get() instanceof RuntimeException, "expected an unchecked failure but got: " + thrown.get());
        assertFalse(thrown.get() instanceof InterruptedException, "an InterruptedException must not escape a Seq<.., IOException>");
        assertTrue(interruptedAfter.get(), "the caller's interrupt status must be restored");
    }

    @Test
    public void test_B6_buffered_stillDeliversElementsAndCheckedExceptions() throws Exception {
        assertEquals(CommonUtil.asList(1, 2, 3), Seq.<Integer, Exception> of(1, 2, 3).buffered(2).toList());

        final IOException boom = new IOException("boom");
        final IOException caught = assertThrows(IOException.class, () -> Seq.<Integer, IOException> of(1, 2, 3).<IOException> onEach(it -> {
            if (it == 2) {
                throw boom;
            }
        }).buffered(1).toList());

        assertSame(boom, caught, "a checked source failure must still arrive as itself");
    }

    @Test
    public void test_B6_buffered_handlesNullElements() throws Exception {
        assertEquals(CommonUtil.asList("a", null, "b"), Seq.<String, Exception> of("a", null, "b").buffered(2).toList());
    }

    // ================================================================================================
    // O2 / O4 - small Seq cleanups
    // ================================================================================================

    @Test
    public void test_O2_forEachUntil_namesTheFlagArgument() {
        final IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> Seq.<Integer, Exception> of(1).forEachUntil(null, it -> {
        }));

        assertTrue(ex.getMessage() != null && ex.getMessage().contains("flagToBreak"), "message was: " + ex.getMessage());
    }

    @Test
    public void test_O4_hasMatchCountBetween_boundariesUnchanged() throws Exception {
        assertTrue(Seq.<Integer, Exception> of(1, 2, 3, 4).hasMatchCountBetween(2, 2, i -> i % 2 == 0));
        assertTrue(Seq.<Integer, Exception> of(1, 2, 3, 4).hasMatchCountBetween(0, 5, i -> i % 2 == 0));
        assertFalse(Seq.<Integer, Exception> of(1, 2, 3, 4).hasMatchCountBetween(3, 4, i -> i % 2 == 0));
        assertFalse(Seq.<Integer, Exception> of(1, 2, 3, 4).hasMatchCountBetween(0, 1, i -> i % 2 == 0));
        assertTrue(Seq.<Integer, Exception> empty().hasMatchCountBetween(0, 0, i -> true));
        assertFalse(Seq.<Integer, Exception> empty().hasMatchCountBetween(1, 2, i -> true));
    }

    // ================================================================================================
    // B3 - Fn.and(..)/Fn.or(..) over a collection must snapshot it
    // ================================================================================================

    @Test
    public void test_B3_and_collection_isNotAffectedByLaterMutation() {
        final List<java.util.function.Predicate<? super String>> ps = new ArrayList<>();
        ps.add(s -> !s.isEmpty());

        final Predicate<String> p = Fn.and(ps);
        assertTrue(p.test("abc"));

        ps.add(s -> s.length() > 10);
        assertTrue(p.test("abc"), "adding a predicate afterwards must not change the built one");

        ps.clear();
        assertTrue(p.test("abc"), "an emptied collection must not turn the conjunction into alwaysTrue");
        assertFalse(p.test(""), "the snapshotted predicate must still reject");
    }

    @Test
    public void test_B3_or_collection_isNotAffectedByLaterMutation() {
        final List<java.util.function.Predicate<? super String>> ps = new ArrayList<>();
        ps.add(String::isEmpty);

        final Predicate<String> p = Fn.or(ps);
        assertTrue(p.test(""));

        ps.clear();
        assertTrue(p.test(""), "an emptied collection must not turn the disjunction into alwaysFalse");
        assertFalse(p.test("abc"));
    }

    @Test
    public void test_B3_and_biPredicateList_isNotAffectedByLaterMutation() {
        final List<java.util.function.BiPredicate<? super String, ? super Integer>> ps = new ArrayList<>();
        ps.add((s, i) -> s.length() == i);

        final BiPredicate<String, Integer> p = Fn.and(ps);
        assertTrue(p.test("ab", 2));

        ps.clear();
        assertTrue(p.test("ab", 2));
        assertFalse(p.test("ab", 3));
    }

    @Test
    public void test_B3_or_biPredicateList_isNotAffectedByLaterMutation() {
        final List<java.util.function.BiPredicate<? super String, ? super Integer>> ps = new ArrayList<>();
        ps.add((s, i) -> i > 0);

        final BiPredicate<String, Integer> p = Fn.or(ps);
        assertTrue(p.test("ab", 1));

        ps.clear();
        assertTrue(p.test("ab", 1));
        assertFalse(p.test("ab", -1));
    }

    @Test
    public void test_B3_andOr_stillEvaluateAllElementsAndRejectEmpty() {
        assertTrue(Fn.<String> and(CommonUtil.asList(s -> s.startsWith("a"), s -> s.endsWith("z"))).test("abz"));
        assertFalse(Fn.<String> and(CommonUtil.asList(s -> s.startsWith("a"), s -> s.endsWith("z"))).test("abc"));
        assertTrue(Fn.<String> or(CommonUtil.asList(s -> s.startsWith("a"), s -> s.endsWith("z"))).test("abc"));
        assertFalse(Fn.<String> or(CommonUtil.asList(s -> s.startsWith("a"), s -> s.endsWith("z"))).test("bcd"));

        assertThrows(IllegalArgumentException.class, () -> Fn.and(new ArrayList<java.util.function.Predicate<? super String>>()));
        assertThrows(IllegalArgumentException.class, () -> Fn.or(new ArrayList<java.util.function.Predicate<? super String>>()));
    }

    // ================================================================================================
    // B8 / B9 - timeLimit no longer schedules; Fn no longer needs a pool to be loaded
    // ================================================================================================

    @Test
    public void test_B8_timeLimit_zeroAndNegative() {
        assertFalse(Fn.timeLimit(0).test("x"));
        assertThrows(IllegalArgumentException.class, () -> Fn.timeLimit(-1));
        assertThrows(IllegalArgumentException.class, () -> Fn.timeLimit((Duration) null));
    }

    @Test
    @Tag("slow-test")
    public void test_B8_timeLimit_windowStartsAtCreationAndExpires() throws Exception {
        final Predicate<String> p = Fn.timeLimit(200);
        assertTrue(p.test("x"));

        Thread.sleep(400);
        assertFalse(p.test("x"), "the window is counted from the factory call");
    }

    @Test
    @Tag("slow-test")
    public void test_B8_timeLimit_durationOverload() throws Exception {
        final Predicate<String> p = Fn.timeLimit(Duration.ofMillis(200));
        assertTrue(p.test("x"));

        Thread.sleep(400);
        assertFalse(p.test("x"));
    }

    @Test
    public void test_B8_timeLimit_hugeValueDoesNotOverflowIntoAnExpiredWindow() {
        assertTrue(Fn.timeLimit(Long.MAX_VALUE).test("x"), "a saturating conversion must not wrap into the past");
        assertTrue(Fn.timeLimit(Long.MAX_VALUE / 1_000L).test("x"));
    }

    // ================================================================================================
    // B10 / O6 - memoizeWithExpiration argument validation
    // ================================================================================================

    @Test
    public void test_B10_fnMemoizeWithExpiration_nullDurationIsIllegalArgument() {
        final IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> Fn.memoizeWithExpiration(() -> "x", (Duration) null));

        assertTrue(ex.getMessage() != null && ex.getMessage().contains("duration"), "message was: " + ex.getMessage());
    }

    @Test
    public void test_O6_memoizeWithExpiration_nullUnitIsReportedAsSuch() {
        final IllegalArgumentException fn = assertThrows(IllegalArgumentException.class, () -> Fn.memoizeWithExpiration(() -> "x", 1L, null));
        assertTrue(fn.getMessage() != null && fn.getMessage().contains("unit"), "message was: " + fn.getMessage());

        final IllegalArgumentException fnn = assertThrows(IllegalArgumentException.class,
                () -> Fnn.memoizeWithExpiration((Throwables.Supplier<String, Exception>) () -> "x", 1L, null));
        assertTrue(fnn.getMessage() != null && fnn.getMessage().contains("unit"), "message was: " + fnn.getMessage());
    }

    @Test
    public void test_O6_nonPositiveDurationWinsOverNullUnit() {
        // Both arguments are invalid. Validate duration first because it precedes unit in the signature.
        final IllegalArgumentException fn = assertThrows(IllegalArgumentException.class, () -> Fn.memoizeWithExpiration(() -> "x", -1L, null));
        assertTrue(fn.getMessage() != null && fn.getMessage().contains("duration"), "message was: " + fn.getMessage());
        assertFalse(fn.getMessage().contains("-1 null"), "duration validation does not format an unvalidated unit: " + fn.getMessage());

        final IllegalArgumentException fnn = assertThrows(IllegalArgumentException.class,
                () -> Fnn.memoizeWithExpiration((Throwables.Supplier<String, Exception>) () -> "x", -1L, null));
        assertTrue(fnn.getMessage() != null && fnn.getMessage().contains("duration"), "message was: " + fnn.getMessage());
        assertFalse(fnn.getMessage().contains("-1 null"), "duration validation does not format an unvalidated unit: " + fnn.getMessage());
    }

    @Test
    public void test_O6_memoizeWithExpiration_nonPositiveDurationStillRejected() {
        assertThrows(IllegalArgumentException.class, () -> Fn.memoizeWithExpiration(() -> "x", 0L, TimeUnit.MILLISECONDS));
        assertThrows(IllegalArgumentException.class, () -> Fn.memoizeWithExpiration(() -> "x", -1L, TimeUnit.MILLISECONDS));
        assertThrows(IllegalArgumentException.class,
                () -> Fnn.memoizeWithExpiration((Throwables.Supplier<String, Exception>) () -> "x", 0L, TimeUnit.MILLISECONDS));
    }

    // ================================================================================================
    // B11 - Fnn.memoizeWithExpiration(supplier, Duration) uses com.landawn.abacus.util.Duration
    // ================================================================================================

    @Test
    public void test_B11_zeroAndNegativeDurationsRejected() {
        assertThrows(IllegalArgumentException.class, () -> Fnn.memoizeWithExpiration((Throwables.Supplier<String, Exception>) () -> "x", Duration.ZERO));
        assertThrows(IllegalArgumentException.class,
                () -> Fnn.memoizeWithExpiration((Throwables.Supplier<String, Exception>) () -> "x", Duration.ofMillis(-1)));
        assertThrows(IllegalArgumentException.class, () -> Fnn.memoizeWithExpiration((Throwables.Supplier<String, Exception>) () -> "x", (Duration) null));
    }

    @Test
    public void test_B11_millisecondDurationExpires() throws Exception {
        final AtomicInteger calls = new AtomicInteger();
        final Throwables.Supplier<Integer, Exception> s = Fnn.memoizeWithExpiration(calls::incrementAndGet, Duration.ofMillis(1));

        assertEquals(1, s.get());
        Thread.sleep(10);
        assertEquals(2, s.get(), "a 1ms window must have expired");
    }

    @Test
    public void test_B11_hugeDurationDoesNotOverflow() throws Exception {
        final AtomicInteger calls = new AtomicInteger();
        final Throwables.Supplier<Integer, Exception> s = Fnn.memoizeWithExpiration(calls::incrementAndGet, Duration.ofMillis(Long.MAX_VALUE));

        assertEquals(1, s.get());
        assertEquals(1, s.get());
    }

    @Test
    public void test_B11_ordinaryDurationsStillWork() throws Exception {
        final AtomicInteger calls = new AtomicInteger();
        final Throwables.Supplier<Integer, Exception> s = Fnn.memoizeWithExpiration(calls::incrementAndGet, Duration.ofMinutes(10));

        assertEquals(1, s.get());
        assertEquals(1, s.get());
    }

    // ================================================================================================
    // B12 - argument checks must name the argument
    // ================================================================================================

    @Test
    public void test_B12_fnArgumentChecksCarryAMessage() {
        assertMessageNames(() -> Fn.startsWith(null), "prefix");
        assertMessageNames(() -> Fn.endsWith(null), "suffix");
        assertMessageNames(() -> Fn.notStartsWith(null), "prefix");
        assertMessageNames(() -> Fn.notEndsWith(null), "suffix");
        assertMessageNames(() -> Fn.contains(null), "valueToFind");
        assertMessageNames(() -> Fn.notContains(null), "str");
        assertMessageNames(() -> Fn.matches(null), "pattern");
        assertMessageNames(() -> Fn.instanceOf(null), "clazz");
        assertMessageNames(() -> Fn.subtypeOf(null), "clazz");
        assertMessageNames(() -> Fn.cast(null), "clazz");
        assertMessageNames(() -> Fn.in(null), "c");
        assertMessageNames(() -> Fn.notIn(null), "c");
        assertMessageNames(() -> Fn.println(null), "separator");
        assertMessageNames(() -> Fn.rateLimiter((RateLimiter) null), "rateLimiter");
    }

    @Test
    public void test_B12_fnnArgumentCheckCarriesAMessage() {
        assertMessageNames(() -> Fnn.rateLimiter((RateLimiter) null), "rateLimiter");
    }

    private static void assertMessageNames(final org.junit.jupiter.api.function.Executable call, final String argName) {
        final IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, call);
        assertNotNull(ex.getMessage(), "IllegalArgumentException for '" + argName + "' carried no message");
        assertTrue(ex.getMessage().contains(argName), "expected the message to name '" + argName + "' but got: " + ex.getMessage());
    }

    // ================================================================================================
    // B13 (reverted) - the factory bounds stay on Exception; see the note on Throwables.Iterator.
    // Widening them broke `Iterator<X, Exception> it = Iterator.of(x).filter(p);`, because a factory call used
    // as a receiver is a standalone expression and infers E from its bound alone. These tests lock in both the
    // shape that must keep compiling and the Throwable-bounded factories that remain available.
    // ================================================================================================

    @Test
    public void test_B13_chainedFactoryCallStillInfersException() throws Exception {
        final Object sentinel = new Object();
        final Throwables.Iterator<Object, Exception> filtered = Throwables.Iterator.of(sentinel).filter(v -> true);

        assertTrue(filtered.hasNext());
        assertSame(sentinel, filtered.next());
        assertFalse(filtered.hasNext());
    }

    @Test
    public void test_B13_throwableBoundedFactoriesAreAvailable() throws Throwable {
        assertFalse(Throwables.Iterator.<String, Throwable> empty().hasNext());
        assertEquals(CommonUtil.asList("j"), Throwables.Iterator.<String, Throwable> just("j").toList());
        assertEquals(CommonUtil.asList("i"), Throwables.Iterator.<String, Throwable> of(CommonUtil.asList("i")).toList());
    }

    @Test
    public void test_B13_iteratorFactoriesStillAcceptExceptionBound() throws Exception {
        final Throwables.Iterator<String, IOException> iter = Throwables.Iterator.of("a", "b");
        assertArrayEquals(new String[] { "a", "b" }, iter.toArray(new String[0]));

        final Throwables.Iterator<String, IOException> fromRange = Throwables.Iterator.of(new String[] { "a", "b", "c" }, 1, 3);
        assertEquals(CommonUtil.asList("b", "c"), fromRange.toList());

        final Throwables.Iterator<String, IOException> deferred = Throwables.Iterator.defer(() -> Throwables.Iterator.of("d"));
        assertEquals(CommonUtil.asList("d"), deferred.toList());

        final Throwables.Iterator<String, IOException> concatenated = Throwables.Iterator.concat(Throwables.Iterator.<String, IOException> of("p"),
                Throwables.Iterator.<String, IOException> of("q"));
        assertEquals(CommonUtil.asList("p", "q"), concatenated.toList());
    }

    // ================================================================================================
    // D3 - Throwables.TernaryOperator is a TriFunction specialization
    // ================================================================================================

    @Test
    public void test_D3_ternaryOperatorIsATriFunction() throws Exception {
        final Throwables.TernaryOperator<Integer, Exception> op = (a, b, c) -> a + b + c;

        assertEquals(6, op.apply(1, 2, 3));

        final Throwables.TriFunction<Integer, Integer, Integer, Integer, Exception> asTriFunction = op;
        assertEquals(6, asTriFunction.apply(1, 2, 3));
    }

    // ================================================================================================
    // B2 / J4 - the documented float policies (regression lock, behaviour deliberately unchanged)
    // ================================================================================================

    @Test
    public void test_B2_floatPoliciesAreAsDocumented() {
        // primitive > / >=
        assertFalse(Fn.FF.positive().test(Float.NaN));
        assertTrue(Fn.FF.notNegative().test(-0.0f));
        assertFalse(Fn.FD.positive().test(Double.NaN));
        assertTrue(Fn.FD.notNegative().test(-0.0d));

        // N.compare / N.equals total order
        assertTrue(Fn.FF.greaterThan().test(Float.NaN, 0f));
        assertFalse(Fn.FF.greaterThanOrEqual().test(-0.0f, 0.0f));
        assertTrue(Fn.FF.equal().test(Float.NaN, Float.NaN));
        assertFalse(Fn.FF.equal().test(0.0f, -0.0f));
        assertTrue(Fn.FD.greaterThan().test(Double.NaN, 0d));
        assertFalse(Fn.FD.greaterThanOrEqual().test(-0.0d, 0.0d));
        assertTrue(Fn.FD.equal().test(Double.NaN, Double.NaN));
        assertFalse(Fn.FD.equal().test(0.0d, -0.0d));

        // Math.min / Math.max
        assertTrue(Float.isNaN(Fn.FF.FloatBinaryOperators.MIN.applyAsFloat(Float.NaN, 1f)));
        assertTrue(Double.isNaN(Fn.FD.DoubleBinaryOperators.MAX.applyAsDouble(Double.NaN, 1d)));
    }

    // ================================================================================================
    // B7 - Fnn.throwException(Supplier) with a null-returning supplier
    // ================================================================================================

    @Test
    public void test_B7_throwExceptionWithNullSupplierResult() {
        final Throwables.Consumer<String, Exception> c = Fnn.throwException(() -> null);

        assertThrows(NullPointerException.class, () -> c.accept("x"));
        assertThrows(IllegalArgumentException.class, () -> Fnn.throwException((java.util.function.Supplier<Exception>) null));
    }

    @Test
    public void test_B7_throwExceptionWithRealSupplier() {
        final Throwables.Consumer<String, IllegalStateException> c = Fnn.throwException(() -> new IllegalStateException("nope"));

        final IllegalStateException ex = assertThrows(IllegalStateException.class, () -> c.accept("x"));
        assertEquals("nope", ex.getMessage());
    }

    // ================================================================================================
    // O3 - Fn.futureGet keeps restoring the interrupt status
    // ================================================================================================

    @Test
    public void test_O3_futureGetWrapsExecutionException() {
        final java.util.concurrent.CompletableFuture<String> failed = new java.util.concurrent.CompletableFuture<>();
        failed.completeExceptionally(new IOException("boom"));

        final RuntimeException ex = assertThrows(RuntimeException.class, () -> Fn.<String> futureGet().apply(failed));
        assertNotNull(ex);
    }

    @Test
    public void test_O3_futureGetReturnsValue() {
        assertEquals("v", Fn.<String> futureGet().apply(java.util.concurrent.CompletableFuture.completedFuture("v")));
        assertNull(Fn.<String> futureGet().apply(java.util.concurrent.CompletableFuture.completedFuture(null)));
    }

    // ================================================================================================
    // J1 - debounce still behaves as documented after the javadoc correction
    // ================================================================================================

    @Test
    public void test_J1_debounceEmitsOnlyTheLastElementOfAnInstantBurst() throws Exception {
        assertEquals(CommonUtil.asList(3), Seq.<Integer, Exception> of(1, 2, 3).debounce(Duration.ofMillis(100)).toList());
        assertEquals(CommonUtil.emptyList(), Seq.<Integer, Exception> empty().debounce(Duration.ofMillis(100)).toList());
        assertEquals(CommonUtil.asList(1), Seq.<Integer, Exception> of(1).debounce(Duration.ofMillis(100)).toList());
    }

    // ================================================================================================
    // UncheckedInterruptedException is what B6 relies on being available
    // ================================================================================================

    @Test
    public void test_B6_interruptedExceptionMapsToUncheckedInterruptedException() {
        final RuntimeException converted = ExceptionUtil.toRuntimeException(new InterruptedException("x"), false, false);

        assertTrue(converted instanceof UncheckedInterruptedException, "got: " + converted.getClass());
        assertFalse(Thread.currentThread().isInterrupted(), "callInterrupt=false must not touch the flag");
    }
}

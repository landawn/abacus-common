package com.landawn.abacus.util.stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.Duration;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.Seq;

/**
 * Stream-family counterparts of the {@code Seq} defects fixed on 2026-09-02.
 *
 * <ul>
 *   <li><b>S1</b> — {@code Stream.ofLines(Reader, true)} did not close the caller's Reader unless the
 *       stream was traversed.</li>
 *   <li><b>S2</b> — {@code transform}/{@code sps}/{@code psp}/{@code transformViaJdkStream}/
 *       {@code EntryStream.transformViaStream} did not link the source for closing, so a function that
 *       ignored its input stranded it.</li>
 *   <li><b>S3</b> — parallel {@code dropWhile} took an element under the shared lock but tested the
 *       predicate outside it, so a thread that had not yet observed the boundary consumed and silently
 *       discarded an element belonging to the output.</li>
 * </ul>
 */
@Tag("unit")
public class StreamFamilyReviewFixes20260902Test extends TestBase {

    /** A Reader that records how many times it was closed. */
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
    // S1 - Stream.ofLines(Reader, true)
    // ================================================================================================

    @Test
    public void test_S1_ofLinesReader_closesReader_whenClosedWithoutTraversal() {
        final TrackingReader reader = new TrackingReader("a\nb\n");

        final Stream<String> s = Stream.ofLines(reader, true);
        assertFalse(reader.isClosed(), "must not be closed before the stream is");

        s.close();

        assertTrue(reader.isClosed(), "closing an untraversed stream must still close the reader");
    }

    @Test
    public void test_S1_ofLinesReader_closesReader_whenTerminalOpPullsNothing() {
        final TrackingReader reader = new TrackingReader("a\nb\n");

        assertEquals(N.emptyList(), Stream.ofLines(reader, true).limit(0).toList());

        assertTrue(reader.isClosed(), "limit(0).toList() pulls no element but must still close the reader");
    }

    @Test
    public void test_S1_ofLinesReader_closesReader_onFullAndPartialTraversal() {
        final TrackingReader full = new TrackingReader("a\nb\n");
        assertEquals(N.asList("a", "b"), Stream.ofLines(full, true).toList());
        assertTrue(full.isClosed());

        final TrackingReader partial = new TrackingReader("a\nb\n");
        assertEquals("a", Stream.ofLines(partial, true).first().orElseThrow());
        assertTrue(partial.isClosed());
    }

    @Test
    public void test_S1_ofLinesReader_closesReaderExactlyOnce() {
        final TrackingReader reader = new TrackingReader("a\nb\n");

        final Stream<String> s = Stream.ofLines(reader, true);
        assertEquals(N.asList("a", "b"), s.toList());
        s.close(); // idempotent

        assertEquals(1, reader.closeCount.get(), "the reader must be closed exactly once");
    }

    @Test
    public void test_S1_ofLinesReader_falseFlag_leavesReaderOpen() {
        final TrackingReader reader = new TrackingReader("a\nb\n");

        assertEquals(N.asList("a", "b"), Stream.ofLines(reader, false).toList());

        assertFalse(reader.isClosed(), "the caller keeps ownership when the flag is false");
    }

    @Test
    public void test_S1_ofLinesReader_emptyReader() {
        final TrackingReader reader = new TrackingReader("");

        assertEquals(N.emptyList(), Stream.ofLines(reader, true).toList());
        assertTrue(reader.isClosed());
    }

    // ================================================================================================
    // S2 - the "hand the pipeline to a function" operations must link the source for closing
    // ================================================================================================

    @Test
    public void test_S2_transform_closesSource_whenTransferDiscardsIt() {
        final AtomicInteger closed = new AtomicInteger();

        final List<String> r = Stream.of("a", "b").onClose(closed::incrementAndGet).transform(s -> Stream.of("x")).toList();

        assertEquals(N.asList("x"), r);
        assertEquals(1, closed.get());
    }

    @Test
    public void test_S2_sps_and_psp_closeSource_whenOpsDiscardsIt() {
        final AtomicInteger a = new AtomicInteger();
        assertEquals(N.asList("x"), Stream.of("a", "b").onClose(a::incrementAndGet).sps(s -> Stream.of("x")).toList());
        assertEquals(1, a.get());

        final AtomicInteger b = new AtomicInteger();
        assertEquals(N.asList("x"), Stream.of("a", "b").onClose(b::incrementAndGet).sps(2, s -> Stream.of("x")).toList());
        assertEquals(1, b.get());

        final AtomicInteger c = new AtomicInteger();
        assertEquals(N.asList("x"), Stream.of("a", "b").onClose(c::incrementAndGet).psp(s -> Stream.of("x")).toList());
        assertEquals(1, c.get());
    }

    @Test
    public void test_S2_transformViaJdkStream_closesSource_whenTransferDiscardsIt() {
        final AtomicInteger a = new AtomicInteger();
        assertEquals(N.asList("x"), Stream.of("a", "b").onClose(a::incrementAndGet).transformViaJdkStream(s -> java.util.stream.Stream.of("x")).toList());
        assertEquals(1, a.get());

        final AtomicInteger b = new AtomicInteger();
        assertEquals(N.asList("x"), Stream.of("a", "b").onClose(b::incrementAndGet).transformViaJdkStream(s -> java.util.stream.Stream.of("x"), true).toList());
        assertEquals(1, b.get());
    }

    @Test
    public void test_S2_primitiveTransformViaJdkStream_closesSource() {
        final AtomicInteger i = new AtomicInteger();
        assertEquals(N.asList(9), IntStream.of(1, 2).onClose(i::incrementAndGet).transformViaJdkStream(s -> java.util.stream.IntStream.of(9)).toList());
        assertEquals(1, i.get());

        final AtomicInteger l = new AtomicInteger();
        assertEquals(N.asList(9L), LongStream.of(1L, 2L).onClose(l::incrementAndGet).transformViaJdkStream(s -> java.util.stream.LongStream.of(9L)).toList());
        assertEquals(1, l.get());

        final AtomicInteger d = new AtomicInteger();
        assertEquals(N.asList(9d),
                DoubleStream.of(1d, 2d).onClose(d::incrementAndGet).transformViaJdkStream(s -> java.util.stream.DoubleStream.of(9d)).toList());
        assertEquals(1, d.get());
    }

    @Test
    public void test_S2_entryStreamTransformViaStream_closesSource() {
        final AtomicInteger a = new AtomicInteger();
        assertEquals(N.asMap("z", 9),
                EntryStream.of(N.asMap("a", 1)).onClose(a::incrementAndGet).transformViaStream(s -> Stream.of(N.newEntry("z", 9))).toMap());
        assertEquals(1, a.get());

        final AtomicInteger b = new AtomicInteger();
        assertEquals(N.asMap("z", 9),
                EntryStream.of(N.asMap("a", 1)).onClose(b::incrementAndGet).transformViaStream(s -> Stream.of(N.newEntry("z", 9)), true).toMap());
        assertEquals(1, b.get());
    }

    @Test
    public void test_S2_wellBehavedTransferStillClosesExactlyOnce() {
        final AtomicInteger t = new AtomicInteger();
        assertEquals(N.asList("A", "B"), Stream.of("a", "b").onClose(t::incrementAndGet).transform(s -> s.map(String::toUpperCase)).toList());
        assertEquals(1, t.get(), "linking must not cause a double close");

        // sps/psp run the ops in parallel, so only the element SET is defined, not the order.
        final AtomicInteger s1 = new AtomicInteger();
        assertHaveSameElements(N.asList("A", "B"), Stream.of("a", "b").onClose(s1::incrementAndGet).sps(s -> s.map(String::toUpperCase)).toList());
        assertEquals(1, s1.get());

        final AtomicInteger p = new AtomicInteger();
        assertHaveSameElements(N.asList("A", "B"), Stream.of("a", "b").onClose(p::incrementAndGet).psp(s -> s.map(String::toUpperCase)).toList());
        assertEquals(1, p.get());

        final AtomicInteger j = new AtomicInteger();
        assertEquals(N.asList("A", "B"), Stream.of("a", "b").onClose(j::incrementAndGet).transformViaJdkStream(s -> s.map(String::toUpperCase)).toList());
        assertEquals(1, j.get());
    }

    @Test
    public void test_S2_transformWithRealFileHandle() {
        final TrackingReader reader = new TrackingReader("a\nb\n");

        assertEquals(N.asList("x"), Stream.ofLines(reader, true).transform(s -> Stream.of("x")).toList());

        assertTrue(reader.isClosed(), "a transfer that ignores its input must not strand the reader");
    }

    @Test
    public void test_S2_identityTransferDoesNotLoopOrDoubleClose() {
        final AtomicInteger closed = new AtomicInteger();

        assertEquals(N.asList("a", "b"), Stream.of("a", "b").onClose(closed::incrementAndGet).transform(s -> s).toList());

        assertEquals(1, closed.get());
    }

    // ================================================================================================
    // S3 - parallel dropWhile must drop only the LEADING prefix
    // ================================================================================================

    @Test
    public void test_S3_parallelDropWhile_dropsOnlyTheLeadingPrefix() {
        for (int run = 0; run < 300; run++) {
            final List<Integer> got = Stream.of(N.asList(1, 2, 3, 4, 1, 2)).parallel(4).dropWhile(n -> n < 3).toList();

            assertEquals(4, got.size(), "run " + run + " lost elements: " + got);
            assertHaveSameElements(N.asList(3, 4, 1, 2), got);
        }
    }

    @Test
    public void test_S3_parallelDropWhile_longSourceLosesNothing() {
        final List<Integer> source = new ArrayList<>();
        source.add(1);
        source.add(2);
        for (int i = 0; i < 200; i++) {
            source.add(3); // survives
            source.add(1); // < 3, but past the prefix, so it must survive too
        }
        final int expected = source.size() - 2;

        for (int run = 0; run < 60; run++) {
            final List<Integer> got = Stream.of(source).parallel(4).dropWhile(n -> n < 3).toList();
            assertEquals(expected, got.size(), "run " + run + " lost elements");
        }
    }

    @Test
    public void test_S3_parallelDropWhile_edgeCases() {
        // nothing to drop
        assertHaveSameElements(N.asList(3, 4), Stream.of(N.asList(3, 4)).parallel(4).dropWhile(n -> n < 3).toList());
        // everything dropped
        assertEquals(N.emptyList(), Stream.of(N.asList(1, 2, 1)).parallel(4).dropWhile(n -> n < 3).toList());
        // empty source
        assertEquals(N.emptyList(), Stream.of(new ArrayList<Integer>()).parallel(4).dropWhile(n -> n < 3).toList());
        // single element kept / dropped
        assertHaveSameElements(N.asList(5), Stream.of(N.asList(5)).parallel(4).dropWhile(n -> n < 3).toList());
        assertEquals(N.emptyList(), Stream.of(N.asList(1)).parallel(4).dropWhile(n -> n < 3).toList());
        // null elements past the prefix must survive
        final List<Integer> withNull = new ArrayList<>(N.asList(1, 2, 3));
        withNull.add(null);
        assertEquals(2, Stream.of(withNull).parallel(4).dropWhile(n -> n != null && n < 3).toList().size());
    }

    @Test
    public void test_S3_sequentialDropWhileUnchanged() {
        assertEquals(N.asList(3, 4, 1, 2), Stream.of(N.asList(1, 2, 3, 4, 1, 2)).dropWhile(n -> n < 3).toList());
        assertEquals(N.asList(3, 4, 1, 2), Stream.of(N.asList(1, 2, 3, 4, 1, 2)).parallel(1).dropWhile(n -> n < 3).toList());
    }

    // ================================================================================================
    // S4 - Seq.debounce and Stream.debounce must agree; both now measure with currentTimeMillis
    // ================================================================================================

    @Test
    public void test_S4_seqAndStreamDebounceAgree_onInstantSources() throws Exception {
        // Everything arrives with a zero gap, so only the last element of the burst survives - in both.
        assertEquals(N.asList(3), Stream.of(N.asList(1, 2, 3)).debounce(Duration.ofMillis(100)).toList());
        assertEquals(N.asList(3), Seq.<Integer, Exception> of(1, 2, 3).debounce(Duration.ofMillis(100)).toList());

        assertEquals(N.asList(1), Stream.of(N.asList(1)).debounce(Duration.ofMillis(100)).toList());
        assertEquals(N.asList(1), Seq.<Integer, Exception> of(1).debounce(Duration.ofMillis(100)).toList());

        assertEquals(N.emptyList(), Stream.of(new ArrayList<Integer>()).debounce(Duration.ofMillis(100)).toList());
        assertEquals(N.emptyList(), Seq.<Integer, Exception> empty().debounce(Duration.ofMillis(100)).toList());
    }

    @Test
    public void test_S4_seqAndStreamDebounceAgree_whenAQuietGapIsObserved() throws Exception {
        final long window = 40;
        final long gap = 250; // comfortably longer than the window

        final List<Integer> viaStream = Stream.of(N.asList(1, 2)).onEach(it -> N.sleepUninterruptibly(gap)).debounce(Duration.ofMillis(window)).toList();
        final List<Integer> viaSeq = Seq.<Integer, Exception> of(1, 2).onEach(it -> N.sleepUninterruptibly(gap)).debounce(Duration.ofMillis(window)).toList();

        // element 1 is followed by a gap >= window, so it survives; element 2 is the final pending one
        assertEquals(N.asList(1, 2), viaStream);
        assertEquals(viaStream, viaSeq, "Seq.debounce and Stream.debounce must produce the same result");
    }

    @Test
    public void test_S4_bothRejectNonPositiveDurations() {
        assertThrows(IllegalArgumentException.class, () -> Stream.of(N.asList(1)).debounce(Duration.ofMillis(0)));
        assertThrows(IllegalArgumentException.class, () -> Seq.<Integer, Exception> of(1).debounce(Duration.ofMillis(0)));
        assertThrows(IllegalArgumentException.class, () -> Stream.of(N.asList(1)).debounce(null));
        assertThrows(IllegalArgumentException.class, () -> Seq.<Integer, Exception> of(1).debounce(null));
    }
}

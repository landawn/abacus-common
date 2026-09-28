package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.exception.UncheckedIOException;
import com.landawn.abacus.exception.UncheckedInterruptedException;
import com.landawn.abacus.exception.UncheckedSQLException;
import com.landawn.abacus.util.stream.Stream;

/**
 * Regression tests for cycle 3 of the 2026-09-24 stream/function review, Seq side (findings C-121, C-127, C-128 and
 * the nits H2-04, H2-06, H2-07) plus the verified C-126 fix and the C-131 documentation.
 */
public class SeqReview20260924cTest extends TestBase {

    // ------------------------------------------------------------------------------------------------------
    // C-121 split/sliding default ArrayList: a SHORT chunk (last chunk, small source) keeps no spare capacity
    // ------------------------------------------------------------------------------------------------------

    private static Field arrayListElementData() {
        try {
            final Field f = ArrayList.class.getDeclaredField("elementData");
            f.setAccessible(true);
            return f;
        } catch (final Exception | Error e) { // InaccessibleObjectException without --add-opens java.base/java.util
            return null;
        }
    }

    private static int capacityOf(final Field elementData, final List<?> list) throws IllegalAccessException {
        return ((Object[]) elementData.get(list)).length;
    }

    private static void assertNoSpareCapacity(final Field elementData, final List<? extends List<?>> chunks, final String what) throws Exception {
        for (int i = 0; i < chunks.size(); i++) {
            assertEquals(chunks.get(i).size(), capacityOf(elementData, chunks.get(i)), what + " chunk " + i);
        }
    }

    @Test
    public void testSplitAndSliding_shortChunksHaveNoSpareCapacity() throws Exception {
        final Field elementData = arrayListElementData();
        Assumptions.assumeTrue(elementData != null, "ArrayList.elementData is not accessible (needs --add-opens java.base/java.util)");

        // a small source with a large chunk size: the only chunk is short (below, at and above the pre-size bound)
        for (final int chunkSize : new int[] { 4, 1000, 60_000, 1 << 16, (1 << 16) + 1, 100_000 }) {
            final List<List<Integer>> split = Seq.<Exception> range(0, 3).split(chunkSize).toList();
            assertEquals(Arrays.asList(Arrays.asList(0, 1, 2)), split);
            assertNoSpareCapacity(elementData, split, "split(" + chunkSize + ")");

            final List<List<Integer>> tumbling = Seq.<Exception> range(0, 3).sliding(chunkSize, chunkSize).toList();
            assertEquals(Arrays.asList(Arrays.asList(0, 1, 2)), tumbling);
            assertNoSpareCapacity(elementData, tumbling, "sliding(" + chunkSize + ", " + chunkSize + ")");

            final List<List<Integer>> overlapping = Seq.<Exception> range(0, 3).sliding(chunkSize, 1).toList();
            assertEquals(Arrays.asList(Arrays.asList(0, 1, 2)), overlapping);
            assertNoSpareCapacity(elementData, overlapping, "sliding(" + chunkSize + ", 1)");
        }

        // the last chunk of a longer source is short; the full chunks stay exact
        final List<List<Integer>> split = Seq.<Exception> range(0, 100_003).split(50_000).toList();
        assertEquals(Arrays.asList(50_000, 50_000, 3), N.map(split, List::size));
        assertEquals(Arrays.asList(100_000, 100_001, 100_002), split.get(2));
        assertNoSpareCapacity(elementData, split, "split(50000) over 100003");

        final List<List<Integer>> windows = Seq.<Exception> range(0, 100_003).sliding(50_000, 50_000).toList();
        assertEquals(Arrays.asList(50_000, 50_000, 3), N.map(windows, List::size));
        assertNoSpareCapacity(elementData, windows, "sliding(50000, 50000) over 100003");

        // overlapping windows: the final window [2..5] is short
        final List<List<String>> overlapping = Seq.<String, Exception> of("é", "中", "😀", "a", "b", "c").sliding(5, 2).toList();
        assertEquals(Arrays.asList(Arrays.asList("é", "中", "😀", "a", "b"), Arrays.asList("😀", "a", "b", "c")), overlapping);
        assertNoSpareCapacity(elementData, overlapping, "sliding(5, 2)");

        // single-element chunks and an empty source
        assertNoSpareCapacity(elementData, Seq.<Exception> range(0, 5).split(1).toList(), "split(1)");
        assertEquals(Collections.emptyList(), Seq.<Integer, Exception> empty().split(100_000).toList());
    }

    // ------------------------------------------------------------------------------------------------------
    // C-127 runAsync/callAsync: a future cancelled before its task starts still closes the sequence
    // ------------------------------------------------------------------------------------------------------

    /** Occupies the single thread of {@code executor} until {@code release} is counted down. */
    private static CountDownLatch block(final ExecutorService executor) throws InterruptedException {
        final CountDownLatch started = new CountDownLatch(1);
        final CountDownLatch release = new CountDownLatch(1);

        executor.execute(() -> {
            started.countDown();

            try {
                release.await();
            } catch (final InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        assertTrue(started.await(10, TimeUnit.SECONDS));
        return release;
    }

    private static void drain(final ExecutorService executor, final CountDownLatch release) throws InterruptedException {
        release.countDown();
        executor.shutdown();
        assertTrue(executor.awaitTermination(10, TimeUnit.SECONDS));
    }

    @Test
    public void testRunAsync_cancelledBeforeStart_closesSequenceWithoutRunningAction() throws Exception {
        for (final boolean mayInterrupt : new boolean[] { false, true }) {
            final ExecutorService single = Executors.newSingleThreadExecutor();

            try {
                final CountDownLatch release = block(single);
                final AtomicInteger closed = new AtomicInteger();
                final AtomicBoolean ran = new AtomicBoolean();
                final Seq<Integer, Exception> seq = Seq.<Integer, Exception> of(1, 2, 3).onClose(closed::incrementAndGet);

                final ContinuableFuture<Void> future = seq.runAsync(s -> {
                    ran.set(true);
                    s.forEach(Fn.emptyConsumer());
                }, single);

                assertTrue(future.cancel(mayInterrupt));
                assertTrue(future.isCancelled());
                assertEquals(1, closed.get(), "closed by the cancellation itself");
                assertThrows(IllegalStateException.class, seq::count);

                drain(single, release);

                assertFalse(ran.get());
                assertEquals(1, closed.get(), "closed exactly once");
            } finally {
                single.shutdownNow();
            }
        }
    }

    @Test
    public void testCallAsync_cancelledBeforeStart_closesSequenceWithoutRunningAction() throws Exception {
        for (final boolean mayInterrupt : new boolean[] { false, true }) {
            final ExecutorService single = Executors.newSingleThreadExecutor();

            try {
                final CountDownLatch release = block(single);
                final AtomicInteger closed = new AtomicInteger();
                final AtomicBoolean ran = new AtomicBoolean();
                final Seq<String, Exception> seq = Seq.<String, Exception> of("é", "中", "😀").onClose(closed::incrementAndGet);

                final ContinuableFuture<List<String>> future = seq.callAsync(s -> {
                    ran.set(true);
                    return s.toList();
                }, single);

                assertTrue(future.cancel(mayInterrupt));
                assertEquals(1, closed.get());
                assertThrows(java.util.concurrent.CancellationException.class, future::get);

                drain(single, release);

                assertFalse(ran.get());
                assertEquals(1, closed.get());
            } finally {
                single.shutdownNow();
            }
        }
    }

    @Test
    public void testAsync_cancelledBeforeStart_failingCloseHandlerDoesNotBreakCancel() throws Exception {
        final ExecutorService single = Executors.newSingleThreadExecutor();

        try {
            final CountDownLatch release = block(single);
            final AtomicBoolean handlerRan = new AtomicBoolean();
            final Seq<Integer, Exception> seq = Seq.<Integer, Exception> of(1).onClose(() -> {
                handlerRan.set(true);
                throw new IllegalStateException("close boom");
            });

            final ContinuableFuture<Long> future = seq.callAsync(Seq::count, single);

            assertTrue(future.cancel(false)); // the close failure is logged, not thrown out of cancel()
            assertTrue(handlerRan.get());
            assertTrue(future.isCancelled());

            drain(single, release);
        } finally {
            single.shutdownNow();
        }
    }

    @Test
    public void testAsync_cancelWhileRunning_closesWhenTheTaskEnds() throws Exception {
        final ExecutorService single = Executors.newSingleThreadExecutor();

        try {
            final CountDownLatch inAction = new CountDownLatch(1);
            final CountDownLatch closedLatch = new CountDownLatch(1);
            final AtomicInteger closed = new AtomicInteger();
            final Seq<Integer, Exception> seq = Seq.<Integer, Exception> of(1, 2, 3).onClose(() -> {
                closed.incrementAndGet();
                closedLatch.countDown();
            });

            final ContinuableFuture<Void> future = seq.runAsync(s -> {
                inAction.countDown();
                Thread.sleep(10_000); // interrupted by cancel(true)
            }, single);

            assertTrue(inAction.await(10, TimeUnit.SECONDS));
            assertTrue(future.cancel(true));
            assertTrue(closedLatch.await(10, TimeUnit.SECONDS), "closed by the task's own finally");

            single.shutdown();
            assertTrue(single.awaitTermination(10, TimeUnit.SECONDS));
            assertEquals(1, closed.get());
        } finally {
            single.shutdownNow();
        }
    }

    @Test
    public void testAsync_normalCompletionAndFailure_stillCloseOnce() throws Exception {
        final AtomicInteger closed = new AtomicInteger();

        assertEquals(Arrays.asList(1, 2, 3), Seq.<Integer, Exception> of(1, 2, 3).onClose(closed::incrementAndGet).callAsync(Seq::toList).get());
        assertEquals(1, closed.get());

        Seq.<Integer, Exception> of(1, 2, 3).onClose(closed::incrementAndGet).runAsync(s -> s.forEach(Fn.emptyConsumer())).get();
        assertEquals(2, closed.get());

        final ExecutorService single = Executors.newSingleThreadExecutor();

        try {
            final ContinuableFuture<Object> failed = Seq.<Integer, Exception> of(1).onClose(() -> {
                closed.incrementAndGet();
                throw new IllegalArgumentException("close boom");
            }).callAsync(s -> {
                throw new IOException("action boom");
            }, single);

            final ExecutionException e = assertThrows(ExecutionException.class, failed::get);
            assertEquals("action boom", e.getCause().getMessage());
            assertEquals("close boom", e.getCause().getSuppressed()[0].getMessage());
            assertEquals(3, closed.get());
        } finally {
            single.shutdownNow();
        }
    }

    // ------------------------------------------------------------------------------------------------------
    // C-128 Seq.ofReversed(List): list iterator created at the call; fail-fast like Seq.of(list); snapshots kept
    // ------------------------------------------------------------------------------------------------------

    @Test
    public void testOfReversed_structuralChangeAfterCreation_failsFast() {
        final ArrayList<Integer> inserted = new ArrayList<>(Arrays.asList(1, 2, 3));
        final Seq<Integer, Exception> s1 = Seq.ofReversed(inserted);
        inserted.add(0, 0);
        assertThrows(ConcurrentModificationException.class, s1::toList); // was [2, 1, 0]

        final ArrayList<Integer> removed = new ArrayList<>(Arrays.asList(1, 2, 3));
        final Seq<Integer, Exception> s2 = Seq.ofReversed(removed);
        removed.remove(0);
        assertThrows(ConcurrentModificationException.class, s2::toList); // was IndexOutOfBoundsException

        final LinkedList<Integer> linkedRemoved = new LinkedList<>(Arrays.asList(1, 2, 3));
        final Seq<Integer, Exception> s3 = Seq.ofReversed(linkedRemoved);
        linkedRemoved.remove(0);
        linkedRemoved.remove(0);
        assertThrows(ConcurrentModificationException.class, s3::toList); // was IndexOutOfBoundsException

        final LinkedList<Integer> linkedAdded = new LinkedList<>(Arrays.asList(1, 2, 3));
        final Seq<Integer, Exception> s4 = Seq.ofReversed(linkedAdded);
        linkedAdded.add(4);
        assertThrows(ConcurrentModificationException.class, s4::toList); // was [3, 2, 1]

        // the same as Seq.of(list)
        final ArrayList<Integer> forward = new ArrayList<>(Arrays.asList(1, 2, 3));
        final Seq<Integer, Exception> s5 = Seq.of(forward);
        forward.add(0, 0);
        assertThrows(ConcurrentModificationException.class, s5::toList);
    }

    @Test
    public void testOfReversed_modificationDuringTraversal_failsFast() {
        final ArrayList<Integer> list = new ArrayList<>(Arrays.asList(1, 2, 3));

        assertThrows(ConcurrentModificationException.class, () -> Seq.<Integer, Exception> ofReversed(list).forEach(x -> {
            if (x == 3) {
                list.add(99);
            }
        }));
    }

    @Test
    public void testOfReversed_skipAndCountAfterShrink_failFastNotIndexOutOfBounds() throws Exception {
        final List<Integer> ten = Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);

        final ArrayList<Integer> cleared = new ArrayList<>(ten);
        final Seq<Integer, Exception> skipped = Seq.<Integer, Exception> ofReversed(cleared).skip(3);
        cleared.clear();
        assertThrows(ConcurrentModificationException.class, skipped::toList);

        final ArrayList<Integer> shrunk = new ArrayList<>(ten);
        final Seq<Integer, Exception> skipped2 = Seq.<Integer, Exception> ofReversed(shrunk).skip(3);
        shrunk.subList(5, 10).clear();
        assertThrows(ConcurrentModificationException.class, skipped2::toList);

        final ArrayList<Integer> counted = new ArrayList<>(ten);
        final Seq<Integer, Exception> toCount = Seq.ofReversed(counted);
        counted.remove(0);
        assertThrows(ConcurrentModificationException.class, toCount::count);

        // unmodified: skip/count/limit are unchanged
        assertEquals(Arrays.asList(7, 6), Seq.<Integer, Exception> ofReversed(new ArrayList<>(ten)).skip(3).limit(2).toList());
        assertEquals(Arrays.asList(7, 6), Seq.<Integer, Exception> ofReversed(new LinkedList<>(ten)).skip(3).limit(2).toList());
        assertEquals(10, Seq.<Integer, Exception> ofReversed(new ArrayList<>(ten)).count());
        assertEquals(7, Seq.<Integer, Exception> ofReversed(new ArrayList<>(ten)).skip(3).count());
        assertEquals(0, Seq.<Integer, Exception> ofReversed(new ArrayList<>(ten)).skip(Long.MAX_VALUE).count());
    }

    @Test
    public void testOfReversed_snapshotList_keepsItsSnapshotEvenWithSkip() throws Exception {
        final CopyOnWriteArrayList<Integer> cow = new CopyOnWriteArrayList<>(Arrays.asList(1, 2, 3));
        final Seq<Integer, Exception> s1 = Seq.ofReversed(cow);
        cow.add(0, 0);
        assertEquals(Arrays.asList(3, 2, 1), s1.toList());

        final CopyOnWriteArrayList<Integer> cow2 = new CopyOnWriteArrayList<>(Arrays.asList(1, 2, 3));
        final Seq<Integer, Exception> s2 = Seq.<Integer, Exception> ofReversed(cow2).skip(1);
        cow2.add(0, 0);
        assertEquals(Arrays.asList(2, 1), s2.toList());

        final CopyOnWriteArrayList<Integer> cow3 = new CopyOnWriteArrayList<>(Arrays.asList(1, 2, 3));
        final Seq<Integer, Exception> s3 = Seq.<Integer, Exception> ofReversed(cow3).skip(1);
        cow3.clear();
        assertEquals(Arrays.asList(2, 1), s3.toList());

        final CopyOnWriteArrayList<Integer> cow4 = new CopyOnWriteArrayList<>(Arrays.asList(1, 2, 3, 4, 5));
        final Seq<Integer, Exception> s4 = Seq.ofReversed(cow4);
        cow4.set(0, 100);
        cow4.remove(1);
        assertEquals(5, s4.count()); // the snapshot, not the current 4 elements

        final CopyOnWriteArrayList<Integer> cow5 = new CopyOnWriteArrayList<>(Arrays.asList(1, 2, 3, 4, 5));
        final Seq<Integer, Exception> s5 = Seq.<Integer, Exception> ofReversed(cow5).skip(1);
        cow5.set(0, 100);
        cow5.remove(1);
        assertEquals(Arrays.asList(4, 3, 2, 1), s5.toList());
    }

    @Test
    public void testOfReversed_nullEmptyAndValues() throws Exception {
        assertEquals(Collections.emptyList(), Seq.<String, Exception> ofReversed((List<String>) null).toList());
        assertEquals(Collections.emptyList(), Seq.<String, Exception> ofReversed(new ArrayList<>()).toList());
        assertEquals(Arrays.asList("😀", null, "中", "é"), Seq.<String, Exception> ofReversed(Arrays.asList("é", "中", null, "😀")).toList());
        assertEquals(Arrays.asList("😀", null, "中", "é"), Seq.<String, Exception> ofReversed(new LinkedList<>(Arrays.asList("é", "中", null, "😀"))).toList());
        assertEquals(Arrays.asList(1), Seq.<Integer, Exception> ofReversed(Collections.singletonList(1)).toList());

        // a non-structural set() is read lazily, as by the list iterator
        final List<String> fixed = Arrays.asList("a", "b", "c");
        final Seq<String, Exception> s = Seq.ofReversed(fixed);
        fixed.set(0, "z");
        assertEquals(Arrays.asList("c", "b", "z"), s.toList());

        // a long linked list is walked in linear time (the old get(i) walk was quadratic)
        final LinkedList<Integer> big = new LinkedList<>();

        for (int i = 0; i < 200_000; i++) {
            big.add(i);
        }

        assertEquals(199_999, Seq.<Integer, Exception> ofReversed(big).first().orElseThrow());
        assertEquals(0, Seq.<Integer, Exception> ofReversed(big).last().orElseThrow());
    }

    // ------------------------------------------------------------------------------------------------------
    // H2-04 sorted*/reversed/shuffled/rotated: a retry after a failed materialization fails again
    // ------------------------------------------------------------------------------------------------------

    private static Seq<Integer, Exception> failingAt2() {
        return Seq.<Integer, Exception> of(3, 1, 2, 5).map(x -> {
            if (x == 2) {
                throw new IllegalStateException("source boom");
            }

            return x;
        });
    }

    @SuppressWarnings("deprecation")
    @Test
    public void testMaterializingOps_retryAfterFailedReadFailsAgain() {
        final List<Function<Seq<Integer, Exception>, Seq<Integer, Exception>>> ops = Arrays.asList(Seq::sorted, s -> s.sorted(Comparators.reverseOrder()),
                s -> s.sortedBy(x -> x), Seq::reverseSorted, s -> s.reverseSortedBy(x -> x), Seq::reversed, Seq::shuffled, s -> s.rotated(1),
                s -> s.sortedByInt(x -> x));

        for (int i = 0; i < ops.size(); i++) {
            final Iterator<Integer> iter = ops.get(i).apply(failingAt2()).stream().iterator();

            assertEquals("source boom", assertThrows(IllegalStateException.class, iter::hasNext).getMessage(), "op " + i);
            // used to report a clean, empty exhaustion
            final IllegalStateException retry = assertThrows(IllegalStateException.class, iter::hasNext, "op " + i);
            assertTrue(retry.getMessage().contains("closed"), retry.getMessage());
        }
    }

    @SuppressWarnings("deprecation")
    @Test
    public void testSorted_retryAfterComparatorFailureFailsAgain() {
        final Iterator<Object> iter = Seq.<Object, Exception> of("a", 1, "b").sorted().stream().iterator();

        assertThrows(ClassCastException.class, iter::hasNext);
        assertThrows(IllegalStateException.class, iter::hasNext);
    }

    @Test
    public void testMaterializingOps_successPathUnchanged() throws Exception {
        assertEquals(Arrays.asList(1, 2, 3, 5), Seq.<Integer, Exception> of(3, 1, 2, 5).sorted().toList());
        assertEquals(Arrays.asList(5, 2, 1, 3), Seq.<Integer, Exception> of(3, 1, 2, 5).reversed().toList());
        assertEquals(Arrays.asList(5, 3, 1, 2), Seq.<Integer, Exception> of(3, 1, 2, 5).rotated(1).toList());
        assertEquals(4, Seq.<Integer, Exception> of(3, 1, 2, 5).shuffled().count());
        assertEquals(Collections.emptyList(), Seq.<Integer, Exception> empty().reversed().toList());

        // repeated hasNext() after success does not re-read
        final AtomicInteger reads = new AtomicInteger();
        final Seq<Integer, Exception> sorted = Seq.<Integer, Exception> of(2, 1).onEach(x -> reads.incrementAndGet()).sorted();
        assertEquals(Arrays.asList(1, 2), sorted.toList());
        assertEquals(2, reads.get());
    }

    // ------------------------------------------------------------------------------------------------------
    // H2-06 stream(): how the sequence's checked exception surfaces (doc lock)
    // ------------------------------------------------------------------------------------------------------

    private static <X extends Exception> Seq<Integer, X> throwingAt2(final X ex) {
        return Seq.<Integer, X> of(1, 2, 3).map(x -> {
            if (x == 2) {
                throw ex;
            }

            return x;
        });
    }

    @Test
    public void testStream_convertsCheckedExceptionsAsDocumented() {
        final IOException io = new IOException("io");
        final UncheckedIOException uio = assertThrows(UncheckedIOException.class, () -> throwingAt2(io).stream().toList());
        assertSame(io, uio.getCause());

        final SQLException sql = new SQLException("sql");
        assertSame(sql, assertThrows(UncheckedSQLException.class, () -> throwingAt2(sql).stream().toList()).getCause());

        final IOException inner = new IOException("inner");
        assertSame(inner, assertThrows(UncheckedIOException.class, () -> throwingAt2(new ExecutionException(inner)).stream().toList()).getCause());

        final IllegalArgumentException iae = new IllegalArgumentException("iae");
        assertSame(iae, assertThrows(IllegalArgumentException.class, () -> throwingAt2(new ExecutionException(iae)).stream().toList()));

        try {
            assertThrows(UncheckedInterruptedException.class, () -> throwingAt2(new InterruptedException("ie")).stream().toList());
            assertTrue(Thread.interrupted(), "interrupt status restored (and cleared here)");
        } finally {
            Thread.interrupted();
        }

        final IllegalStateException ise = new IllegalStateException("ise");
        assertSame(ise, assertThrows(IllegalStateException.class,
                () -> Seq.<Integer, Exception> of(1, 2).map(x -> { throw ise; }).stream().toList()));

        final AssertionError error = new AssertionError("error");
        assertSame(error, assertThrows(AssertionError.class, () -> Seq.<Integer, Exception> of(1, 2).map(x -> { throw error; }).stream().toList()));
    }

    // ------------------------------------------------------------------------------------------------------
    // H2-07 listFiles: eager (non-recursive) vs lazy (recursive) listing; a non-directory yields empty (doc lock)
    // ------------------------------------------------------------------------------------------------------

    @Test
    public void testListFiles_eagerVersusLazyListing() throws Exception {
        final Path dir = Files.createTempDirectory("seqReviewC");

        try {
            final File root = dir.toFile();
            final File first = new File(root, "first-é中.txt");
            assertTrue(first.createNewFile());

            final Seq<File, IOException> eager = Seq.listFiles(root);
            final Seq<File, IOException> eager2 = Seq.listFiles(root, false);
            final Seq<File, IOException> lazy = Seq.listFiles(root, true);

            final File second = new File(root, "second.txt");
            assertTrue(second.createNewFile());

            assertEquals(Arrays.asList(first.getName()), eager.map(File::getName).toList());
            assertEquals(Arrays.asList(first.getName()), eager2.map(File::getName).toList());
            assertEquals(N.asSet(first.getName(), second.getName()), lazy.map(File::getName).toSet());

            // a regular file (not a directory) yields an empty sequence, never an IOException
            assertEquals(0, Seq.listFiles(first).count());
            assertEquals(0, Seq.listFiles(first, true).count());
            assertEquals(0, Seq.listFiles(new File(root, "missing")).count());
        } finally {
            Seq.listFiles(dir.toFile(), true).forEach(File::delete);
            Files.deleteIfExists(dir);
        }
    }

    // ------------------------------------------------------------------------------------------------------
    // C-126 buffered / sps / transformViaStream: a failure thrown by the sequence keeps its exact type
    // ------------------------------------------------------------------------------------------------------

    private static <X extends Exception> Seq<Integer, X> failingAt(final int n, final int failAt, final RuntimeException unchecked, final X checked) {
        return Seq.<X> range(0, n).map(x -> {
            if (x == failAt) {
                if (unchecked != null) {
                    throw unchecked;
                }

                throw checked;
            }

            return x;
        });
    }

    /** Every path that must be transparent for a failure raised by the source sequence itself. */
    private static <X extends Exception> List<Throwables.Function<Seq<Integer, X>, List<Integer>, X>> transparentPaths(final ExecutorService executor) {
        return Arrays.asList(Seq::toList, s -> s.buffered().toList(), s -> s.buffered(1).toList(), s -> s.buffered(100_000).toList(),
                s -> s.sps(st -> st.map(x -> x)).toList(), s -> s.sps(3, st -> st.map(x -> x)).toList(),
                s -> s.sps(3, executor, st -> st.map(x -> x)).toList(), s -> s.transformViaStream(st -> st.map(x -> x)).toList(),
                s -> s.transformViaStream(st -> st.map(x -> x), true).toList(), s -> s.transformViaStream(st -> st.filter(x -> true).skip(0)).toList());
    }

    private static <X extends Exception> void assertSameFailureOnEveryPath(final java.util.function.Supplier<Seq<Integer, X>> seqs, final Throwable expected,
            final ExecutorService executor) {
        final List<Throwables.Function<Seq<Integer, X>, List<Integer>, X>> paths = SeqReview20260924cTest.<X> transparentPaths(executor);

        for (int i = 0; i < paths.size(); i++) {
            final Throwables.Function<Seq<Integer, X>, List<Integer>, X> path = paths.get(i);
            final Throwable actual = assertThrows(Throwable.class, () -> path.apply(seqs.get()), "path " + i);
            assertSame(expected, actual, "path " + i + " threw " + actual);
        }
    }

    @Test
    public void testSeqFailure_keepsItsExactTypeThroughBufferedSpsAndTransformViaStream() throws Exception {
        final ExecutorService executor = Executors.newFixedThreadPool(3);

        try {
            // an unchecked wrapper thrown on purpose must not become an undeclared checked IOException
            final java.io.UncheckedIOException jdkUio = new java.io.UncheckedIOException(new IOException("jdk io"));
            SeqReview20260924cTest.<SQLException> assertSameFailureOnEveryPath(() -> failingAt(10, 2, jdkUio, null), jdkUio, executor);

            final UncheckedIOException abacusUio = new UncheckedIOException(new IOException("abacus io é中"));
            SeqReview20260924cTest.<SQLException> assertSameFailureOnEveryPath(() -> failingAt(10, 2, abacusUio, null), abacusUio, executor);

            // a declared E of a wrapper type must not be replaced by its cause
            final ExecutionException exe = new ExecutionException(new IOException("inner"));
            SeqReview20260924cTest.<ExecutionException> assertSameFailureOnEveryPath(() -> failingAt(10, 2, null, exe), exe, executor);

            final ExecutionException exeIse = new ExecutionException(new IllegalStateException("inner ise"));
            SeqReview20260924cTest.<ExecutionException> assertSameFailureOnEveryPath(() -> failingAt(10, 2, null, exeIse), exeIse, executor);

            // unchanged: a plain declared E and a plain unchecked exception
            final SQLException sql = new SQLException("sql");
            SeqReview20260924cTest.<SQLException> assertSameFailureOnEveryPath(() -> failingAt(10, 2, null, sql), sql, executor);

            final IllegalArgumentException iae = new IllegalArgumentException("iae");
            SeqReview20260924cTest.<SQLException> assertSameFailureOnEveryPath(() -> failingAt(10, 2, iae, null), iae, executor);

            // failing late in a long source: the parallel stage may wrap it again, the original is still found
            SeqReview20260924cTest.<ExecutionException> assertSameFailureOnEveryPath(() -> failingAt(5_000, 4_321, null, exe), exe, executor);
            SeqReview20260924cTest.<SQLException> assertSameFailureOnEveryPath(() -> failingAt(5_000, 4_321, abacusUio, null), abacusUio, executor);
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    public void testStreamStageFailure_stillUnwrappedAsBefore() throws Exception {
        // raised INSIDE the stream stage (not by the source sequence): today's unwrap is kept (SeqTransformTest pins it)
        final Exception viaSps = assertThrows(Exception.class, () -> Seq.<Integer, SQLException> of(1, 2, 3).sps(st -> st.map(x -> {
            if (x == 2) {
                throw new java.io.UncheckedIOException(new IOException("stage"));
            }

            return x;
        })).toList());
        assertEquals(IOException.class, viaSps.getClass());
        assertEquals("stage", viaSps.getMessage());

        final Exception viaTransform = assertThrows(Exception.class,
                () -> Seq.<Integer, SQLException> of(1, 2, 3).transformViaStream(st -> st.map(x -> {
                    if (x == 2) {
                        throw new UncheckedSQLException(new SQLException("stage sql"));
                    }

                    return x;
                })).toList());
        assertEquals(SQLException.class, viaTransform.getClass());

        // the stage itself still sees the documented stream() conversion of a source failure
        final IOException io = new IOException("src");
        final AtomicReference<Throwable> seenByStage = new AtomicReference<>();
        final Exception rethrown = assertThrows(Exception.class, () -> SeqReview20260924cTest.<IOException> failingAt(5, 1, null, io).transformViaStream(st -> {
            try {
                return Stream.of(st.toList());
            } catch (final RuntimeException e) {
                seenByStage.set(e);
                throw e;
            }
        }).toList());
        assertEquals(UncheckedIOException.class, seenByStage.get().getClass());
        assertSame(io, seenByStage.get().getCause());
        // the eager callback itself failed, so its own exception propagates from transformViaStream(..) unchanged
        assertSame(seenByStage.get(), rethrown);

        // success paths are untouched
        assertEquals(Arrays.asList(2, 4, 6), Seq.<Integer, SQLException> of(1, 2, 3).sps(st -> st.map(x -> x * 2)).sorted().toList());
        assertEquals(Arrays.asList(1, 2, 3), Seq.<Integer, SQLException> of(1, 2, 3).buffered(2).toList());
    }

    // ------------------------------------------------------------------------------------------------------
    // C-131 delay / rateLimited: uninterruptible waits keep the interrupt flag; the takeWhile stop idiom works
    // ------------------------------------------------------------------------------------------------------

    @Test
    public void testDelayAndRateLimited_interruptFlagIsKeptAndDoesNotAbort() throws Exception {
        try {
            Thread.currentThread().interrupt();
            assertEquals(Arrays.asList(1, 2, 3), Seq.<Integer, Exception> of(1, 2, 3).delay(Duration.ofMillis(5)).toList());
            assertTrue(Thread.currentThread().isInterrupted());

            assertEquals(Arrays.asList(1, 2, 3), Seq.<Integer, Exception> of(1, 2, 3).delay(java.time.Duration.ofMillis(5)).toList());
            assertTrue(Thread.currentThread().isInterrupted());

            assertEquals(Arrays.asList(1, 2, 3), Seq.<Integer, Exception> of(1, 2, 3).rateLimited(1000).toList());
            assertTrue(Thread.currentThread().isInterrupted());
        } finally {
            Thread.interrupted();
        }
    }

    @Test
    public void testDelay_takeWhileNotInterruptedIdiomStopsAnInfiniteTraversal() throws Exception {
        for (final boolean useRateLimit : new boolean[] { false, true }) {
            final AtomicInteger emitted = new AtomicInteger();
            final AtomicBoolean flagAtEnd = new AtomicBoolean();
            final CountDownLatch started = new CountDownLatch(1);

            final Thread consumer = new Thread(() -> {
                try {
                    final Seq<Integer, Exception> infinite = Seq.<Integer, Exception> of(1).cycled();
                    (useRateLimit ? infinite.rateLimited(50) : infinite.delay(Duration.ofMillis(20))) //
                            .takeWhile(x -> !Thread.currentThread().isInterrupted())
                            .forEach(x -> {
                                emitted.incrementAndGet();
                                started.countDown();
                            });
                    flagAtEnd.set(Thread.currentThread().isInterrupted());
                } catch (final Exception e) {
                    throw new RuntimeException(e);
                }
            });
            consumer.setDaemon(true);
            consumer.start();

            assertTrue(started.await(10, TimeUnit.SECONDS));
            consumer.interrupt();
            consumer.join(10_000);

            assertFalse(consumer.isAlive(), "the idiom must end the traversal");
            assertTrue(flagAtEnd.get(), "interrupt status kept");
            assertTrue(emitted.get() >= 1);
        }
    }
}

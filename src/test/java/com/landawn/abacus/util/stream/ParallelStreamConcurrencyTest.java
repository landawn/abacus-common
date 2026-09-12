package com.landawn.abacus.util.stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.stream.BaseStream.ParallelSettings;
import com.landawn.abacus.util.stream.BaseStream.SplitStrategy;

/**
 * Regression tests for concurrency hazards in parallel stream base classes.
 *
 * <p>Covers fixes such as:
 * <ul>
 *   <li>{@code sequential} cache field made {@code volatile} with proper
 *       double-checked locking in {@link ParallelArrayStream#sequential()} and
 *       {@link ParallelIteratorStream#sequential()}.</li>
 * </ul>
 */
public class ParallelStreamConcurrencyTest extends TestBase {

    /**
     * The {@code sequential} cache field on {@link ParallelArrayStream} must be
     * {@code volatile}; otherwise concurrent calls to {@code sequential()} may
     * observe partially-published instances or create duplicates that share the
     * same backing data.
     */
    @Test
    public void parallelArrayStream_sequentialField_isVolatile() throws Exception {
        Field f = ParallelArrayStream.class.getDeclaredField("sequential");
        assertTrue(Modifier.isVolatile(f.getModifiers()), "ParallelArrayStream.sequential must be declared volatile");
    }

    /**
     * Same volatile contract for {@link ParallelIteratorStream#sequential()}.
     * A duplicate {@code IteratorStream} wrapping the same underlying iterator
     * would be a real correctness bug because two consumers of the same
     * iterator would interleave their reads.
     */
    @Test
    public void parallelIteratorStream_sequentialField_isVolatile() throws Exception {
        Field f = ParallelIteratorStream.class.getDeclaredField("sequential");
        assertTrue(Modifier.isVolatile(f.getModifiers()), "ParallelIteratorStream.sequential must be declared volatile");
    }

    /**
     * Hammer {@code sequential()} from many threads to verify that:
     * <ol>
     *   <li>It always returns a non-null instance.</li>
     *   <li>All concurrent callers observe the same cached instance (no
     *       duplicates leaked through the broken double-checked locking that
     *       existed before the volatile fix).</li>
     * </ol>
     */
    @Test
    public void parallelArrayStream_sequential_concurrentCallsReturnSameInstance() throws Exception {
        final Integer[] data = new Integer[1024];
        for (int i = 0; i < data.length; i++) {
            data[i] = i;
        }

        // Build a parallel array stream
        Stream<Integer> parallel = Stream.of(data).parallel(ParallelSettings.builder().splitStrategy(SplitStrategy.ARRAY).maxThreadNum(8).build());
        try {
            final int callerCount = 32;
            final ExecutorService pool = Executors.newFixedThreadPool(callerCount);
            try {
                final CountDownLatch start = new CountDownLatch(1);
                final CountDownLatch done = new CountDownLatch(callerCount);
                final List<Stream<Integer>> observed = Collections.synchronizedList(new ArrayList<>(callerCount));
                final AtomicReference<Throwable> err = new AtomicReference<>();

                for (int i = 0; i < callerCount; i++) {
                    pool.submit(() -> {
                        try {
                            start.await();
                            // Race many threads on the same parallel stream's sequential() cache.
                            Stream<Integer> s = parallel.sequential();
                            assertNotNull(s);
                            observed.add(s);
                        } catch (Throwable t) {
                            err.compareAndSet(null, t);
                        } finally {
                            done.countDown();
                        }
                    });
                }

                start.countDown();
                assertTrue(done.await(30, TimeUnit.SECONDS), "Timed out waiting for sequential() callers");
                if (err.get() != null) {
                    fail("Concurrent sequential() call threw: " + err.get());
                }

                assertEquals(callerCount, observed.size());

                // All callers must observe the same cached sequential view.
                Stream<Integer> first = observed.get(0);
                for (Stream<Integer> s : observed) {
                    assertSame(first, s, "sequential() must return a single cached instance");
                }
            } finally {
                pool.shutdownNow();
            }
        } finally {
            parallel.close();
        }
    }

    /**
     * For {@link ParallelIteratorStream}, the consequences of a duplicated
     * cache are more visible: two wrappers around the same underlying iterator
     * will interleave their reads and the sequential consumer may miss
     * elements. Verify that under concurrent {@code sequential()} access the
     * cached instance is shared and that a subsequent sequential terminal
     * observes every element exactly once.
     */
    @Test
    public void parallelIteratorStream_sequential_concurrentCallsReturnSameInstance() throws Exception {
        final List<Integer> data = new ArrayList<>(2048);
        for (int i = 0; i < 2048; i++) {
            data.add(i);
        }

        // .iterator() forces an IteratorStream-backed parallel stream
        Stream<Integer> parallel = Stream.of(data.iterator())
                .parallel(ParallelSettings.builder().splitStrategy(SplitStrategy.ITERATOR).maxThreadNum(8).build());
        try {
            final int callerCount = 32;
            final ExecutorService pool = Executors.newFixedThreadPool(callerCount);
            try {
                final CountDownLatch start = new CountDownLatch(1);
                final CountDownLatch done = new CountDownLatch(callerCount);
                final List<Stream<Integer>> observed = Collections.synchronizedList(new ArrayList<>(callerCount));
                final AtomicReference<Throwable> err = new AtomicReference<>();

                for (int i = 0; i < callerCount; i++) {
                    pool.submit(() -> {
                        try {
                            start.await();
                            Stream<Integer> s = parallel.sequential();
                            assertNotNull(s);
                            observed.add(s);
                        } catch (Throwable t) {
                            err.compareAndSet(null, t);
                        } finally {
                            done.countDown();
                        }
                    });
                }

                start.countDown();
                assertTrue(done.await(30, TimeUnit.SECONDS), "Timed out waiting for sequential() callers");
                if (err.get() != null) {
                    fail("Concurrent sequential() call threw: " + err.get());
                }

                Stream<Integer> first = observed.get(0);
                for (Stream<Integer> s : observed) {
                    assertSame(first, s, "sequential() must return a single cached instance even for iterator-backed streams");
                }

                // Drain the cached sequential view: since it's the *only* cached wrapper
                // around the underlying iterator, we must see all elements exactly once.
                List<Integer> drained = first.toList();
                assertEquals(data.size(), drained.size(), "Cached sequential view must drain entire iterator");
                Set<Integer> distinct = new HashSet<>(drained);
                assertEquals(data.size(), distinct.size(), "Each element must appear exactly once");
            } finally {
                pool.shutdownNow();
            }
        } finally {
            // parallel was already closed via first.toList() (which closes its source); ignore
        }
    }

    /**
     * Smoke test that exercises a real parallel pipeline end-to-end (filter +
     * map + reduce on an ARRAY-splitStrategy stream), to make sure the volatile fix
     * did not regress regular parallel execution.
     */
    @Test
    public void parallelArrayStream_basicPipelineStillWorks() {
        Integer[] data = new Integer[1000];
        for (int i = 0; i < data.length; i++) {
            data[i] = i + 1;
        }

        long sum = Stream.of(data)
                .parallel(ParallelSettings.builder().splitStrategy(SplitStrategy.ARRAY).maxThreadNum(8).build())
                .filter(v -> v % 2 == 0)
                .mapToLong(Integer::longValue)
                .sum();

        // Even numbers from 1..1000 sum to 2 + 4 + ... + 1000 = 250500
        assertEquals(250500L, sum);
    }

    /**
     * Same smoke test for ITERATOR splitStrategy.
     */
    @Test
    public void parallelIteratorStream_basicPipelineStillWorks() {
        List<Integer> data = new ArrayList<>(1000);
        for (int i = 1; i <= 1000; i++) {
            data.add(i);
        }

        long sum = Stream.of(data.iterator())
                .parallel(ParallelSettings.builder().splitStrategy(SplitStrategy.ITERATOR).maxThreadNum(8).build())
                .filter(v -> v % 2 == 0)
                .mapToLong(Integer::longValue)
                .sum();

        assertEquals(250500L, sum);
    }

    /**
     * Sanity: sequential() on a fresh parallel stream should return a working
     * sequential view that produces the same elements as the original.
     */
    @Test
    public void parallelArrayStream_sequentialView_matchesOriginalElements() {
        Integer[] data = { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10 };
        Stream<Integer> parallel = Stream.of(data).parallel(ParallelSettings.builder().splitStrategy(SplitStrategy.ARRAY).maxThreadNum(4).build());
        List<Integer> sequentialResult = parallel.sequential().toList();
        assertEquals(Arrays.asList(data), sequentialResult);
    }

    // -----------------------------------------------------------------------
    // Primitive parallel stream volatile fixes (all 14 specialisations)
    // -----------------------------------------------------------------------

    /** Helper: hammer sequential() from many threads and assert single cached instance. */
    private static <S extends BaseStream<?, ?, ?, ?, ?, ?, ?, ?>> void assertSingleCachedSequential(S parallel, int callerCount) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(callerCount);
        try {
            CountDownLatch start = new CountDownLatch(1);
            CountDownLatch done = new CountDownLatch(callerCount);
            List<Object> observed = Collections.synchronizedList(new ArrayList<>(callerCount));
            AtomicReference<Throwable> err = new AtomicReference<>();

            for (int i = 0; i < callerCount; i++) {
                pool.submit(() -> {
                    try {
                        start.await();
                        observed.add(parallel.sequential());
                    } catch (Throwable t) {
                        err.compareAndSet(null, t);
                    } finally {
                        done.countDown();
                    }
                });
            }

            start.countDown();
            assertTrue(done.await(30, TimeUnit.SECONDS), "Timed out");
            if (err.get() != null) {
                fail("Concurrent sequential() threw: " + err.get());
            }
            assertEquals(callerCount, observed.size());
            Object first = observed.get(0);
            for (Object s : observed) {
                assertSame(first, s, "All threads must observe the same cached sequential instance");
            }
        } finally {
            pool.shutdownNow();
        }
    }

    // --- ParallelArrayByteStream ---

    @Test
    public void parallelArrayByteStream_sequentialField_isVolatile() throws Exception {
        Field f = ParallelArrayByteStream.class.getDeclaredField("sequential");
        assertTrue(Modifier.isVolatile(f.getModifiers()), "ParallelArrayByteStream.sequential must be declared volatile");
    }

    @Test
    public void parallelArrayByteStream_sequential_concurrentCallsReturnSameInstance() throws Exception {
        ByteStream parallel = ByteStream.of(new byte[] { 1, 2, 3, 4, 5 })
                .parallel(ParallelSettings.builder().splitStrategy(SplitStrategy.ARRAY).maxThreadNum(4).build());
        assertSingleCachedSequential(parallel, 16);
        parallel.close();
    }

    // --- ParallelArrayCharStream ---

    @Test
    public void parallelArrayCharStream_sequentialField_isVolatile() throws Exception {
        Field f = ParallelArrayCharStream.class.getDeclaredField("sequential");
        assertTrue(Modifier.isVolatile(f.getModifiers()), "ParallelArrayCharStream.sequential must be declared volatile");
    }

    @Test
    public void parallelArrayCharStream_sequential_concurrentCallsReturnSameInstance() throws Exception {
        CharStream parallel = CharStream.of(new char[] { 'a', 'b', 'c', 'd', 'e' })
                .parallel(ParallelSettings.builder().splitStrategy(SplitStrategy.ARRAY).maxThreadNum(4).build());
        assertSingleCachedSequential(parallel, 16);
        parallel.close();
    }

    // --- ParallelArrayShortStream ---

    @Test
    public void parallelArrayShortStream_sequentialField_isVolatile() throws Exception {
        Field f = ParallelArrayShortStream.class.getDeclaredField("sequential");
        assertTrue(Modifier.isVolatile(f.getModifiers()), "ParallelArrayShortStream.sequential must be declared volatile");
    }

    @Test
    public void parallelArrayShortStream_sequential_concurrentCallsReturnSameInstance() throws Exception {
        ShortStream parallel = ShortStream.of(new short[] { 1, 2, 3, 4, 5 })
                .parallel(ParallelSettings.builder().splitStrategy(SplitStrategy.ARRAY).maxThreadNum(4).build());
        assertSingleCachedSequential(parallel, 16);
        parallel.close();
    }

    // --- ParallelArrayIntStream ---

    @Test
    public void parallelArrayIntStream_sequentialField_isVolatile() throws Exception {
        Field f = ParallelArrayIntStream.class.getDeclaredField("sequential");
        assertTrue(Modifier.isVolatile(f.getModifiers()), "ParallelArrayIntStream.sequential must be declared volatile");
    }

    @Test
    public void parallelArrayIntStream_sequential_concurrentCallsReturnSameInstance() throws Exception {
        IntStream parallel = IntStream.of(1, 2, 3, 4, 5).parallel(ParallelSettings.builder().splitStrategy(SplitStrategy.ARRAY).maxThreadNum(4).build());
        assertSingleCachedSequential(parallel, 16);
        parallel.close();
    }

    // --- ParallelArrayLongStream ---

    @Test
    public void parallelArrayLongStream_sequentialField_isVolatile() throws Exception {
        Field f = ParallelArrayLongStream.class.getDeclaredField("sequential");
        assertTrue(Modifier.isVolatile(f.getModifiers()), "ParallelArrayLongStream.sequential must be declared volatile");
    }

    @Test
    public void parallelArrayLongStream_sequential_concurrentCallsReturnSameInstance() throws Exception {
        LongStream parallel = LongStream.of(1L, 2L, 3L, 4L, 5L).parallel(ParallelSettings.builder().splitStrategy(SplitStrategy.ARRAY).maxThreadNum(4).build());
        assertSingleCachedSequential(parallel, 16);
        parallel.close();
    }

    // --- ParallelArrayFloatStream ---

    @Test
    public void parallelArrayFloatStream_sequentialField_isVolatile() throws Exception {
        Field f = ParallelArrayFloatStream.class.getDeclaredField("sequential");
        assertTrue(Modifier.isVolatile(f.getModifiers()), "ParallelArrayFloatStream.sequential must be declared volatile");
    }

    @Test
    public void parallelArrayFloatStream_sequential_concurrentCallsReturnSameInstance() throws Exception {
        FloatStream parallel = FloatStream.of(1f, 2f, 3f, 4f, 5f)
                .parallel(ParallelSettings.builder().splitStrategy(SplitStrategy.ARRAY).maxThreadNum(4).build());
        assertSingleCachedSequential(parallel, 16);
        parallel.close();
    }

    // --- ParallelArrayDoubleStream ---

    @Test
    public void parallelArrayDoubleStream_sequentialField_isVolatile() throws Exception {
        Field f = ParallelArrayDoubleStream.class.getDeclaredField("sequential");
        assertTrue(Modifier.isVolatile(f.getModifiers()), "ParallelArrayDoubleStream.sequential must be declared volatile");
    }

    @Test
    public void parallelArrayDoubleStream_sequential_concurrentCallsReturnSameInstance() throws Exception {
        DoubleStream parallel = DoubleStream.of(1.0, 2.0, 3.0, 4.0, 5.0)
                .parallel(ParallelSettings.builder().splitStrategy(SplitStrategy.ARRAY).maxThreadNum(4).build());
        assertSingleCachedSequential(parallel, 16);
        parallel.close();
    }

    // --- ParallelIteratorByteStream ---

    @Test
    public void parallelIteratorByteStream_sequentialField_isVolatile() throws Exception {
        Field f = ParallelIteratorByteStream.class.getDeclaredField("sequential");
        assertTrue(Modifier.isVolatile(f.getModifiers()), "ParallelIteratorByteStream.sequential must be declared volatile");
    }

    @Test
    public void parallelIteratorByteStream_sequential_concurrentCallsReturnSameInstance() throws Exception {
        // map forces an iterator-backed parallel stream
        ByteStream parallel = ByteStream.of(new byte[] { 1, 2, 3, 4, 5 })
                .map(b -> b)
                .parallel(ParallelSettings.builder().splitStrategy(SplitStrategy.ITERATOR).maxThreadNum(4).build());
        assertSingleCachedSequential(parallel, 16);
        parallel.close();
    }

    // --- ParallelIteratorCharStream ---

    @Test
    public void parallelIteratorCharStream_sequentialField_isVolatile() throws Exception {
        Field f = ParallelIteratorCharStream.class.getDeclaredField("sequential");
        assertTrue(Modifier.isVolatile(f.getModifiers()), "ParallelIteratorCharStream.sequential must be declared volatile");
    }

    @Test
    public void parallelIteratorCharStream_sequential_concurrentCallsReturnSameInstance() throws Exception {
        CharStream parallel = CharStream.of(new char[] { 'a', 'b', 'c', 'd', 'e' })
                .map(c -> c)
                .parallel(ParallelSettings.builder().splitStrategy(SplitStrategy.ITERATOR).maxThreadNum(4).build());
        assertSingleCachedSequential(parallel, 16);
        parallel.close();
    }

    // --- ParallelIteratorShortStream ---

    @Test
    public void parallelIteratorShortStream_sequentialField_isVolatile() throws Exception {
        Field f = ParallelIteratorShortStream.class.getDeclaredField("sequential");
        assertTrue(Modifier.isVolatile(f.getModifiers()), "ParallelIteratorShortStream.sequential must be declared volatile");
    }

    @Test
    public void parallelIteratorShortStream_sequential_concurrentCallsReturnSameInstance() throws Exception {
        ShortStream parallel = ShortStream.of(new short[] { 1, 2, 3, 4, 5 })
                .map(s -> s)
                .parallel(ParallelSettings.builder().splitStrategy(SplitStrategy.ITERATOR).maxThreadNum(4).build());
        assertSingleCachedSequential(parallel, 16);
        parallel.close();
    }

    // --- ParallelIteratorIntStream ---

    @Test
    public void parallelIteratorIntStream_sequentialField_isVolatile() throws Exception {
        Field f = ParallelIteratorIntStream.class.getDeclaredField("sequential");
        assertTrue(Modifier.isVolatile(f.getModifiers()), "ParallelIteratorIntStream.sequential must be declared volatile");
    }

    @Test
    public void parallelIteratorIntStream_sequential_concurrentCallsReturnSameInstance() throws Exception {
        IntStream parallel = IntStream.of(1, 2, 3, 4, 5)
                .map(i -> i)
                .parallel(ParallelSettings.builder().splitStrategy(SplitStrategy.ITERATOR).maxThreadNum(4).build());
        assertSingleCachedSequential(parallel, 16);
        parallel.close();
    }

    // --- ParallelIteratorLongStream ---

    @Test
    public void parallelIteratorLongStream_sequentialField_isVolatile() throws Exception {
        Field f = ParallelIteratorLongStream.class.getDeclaredField("sequential");
        assertTrue(Modifier.isVolatile(f.getModifiers()), "ParallelIteratorLongStream.sequential must be declared volatile");
    }

    @Test
    public void parallelIteratorLongStream_sequential_concurrentCallsReturnSameInstance() throws Exception {
        LongStream parallel = LongStream.of(1L, 2L, 3L, 4L, 5L)
                .map(l -> l)
                .parallel(ParallelSettings.builder().splitStrategy(SplitStrategy.ITERATOR).maxThreadNum(4).build());
        assertSingleCachedSequential(parallel, 16);
        parallel.close();
    }

    // --- ParallelIteratorFloatStream ---

    @Test
    public void parallelIteratorFloatStream_sequentialField_isVolatile() throws Exception {
        Field f = ParallelIteratorFloatStream.class.getDeclaredField("sequential");
        assertTrue(Modifier.isVolatile(f.getModifiers()), "ParallelIteratorFloatStream.sequential must be declared volatile");
    }

    @Test
    public void parallelIteratorFloatStream_sequential_concurrentCallsReturnSameInstance() throws Exception {
        FloatStream parallel = FloatStream.of(1f, 2f, 3f, 4f, 5f)
                .map(f -> f)
                .parallel(ParallelSettings.builder().splitStrategy(SplitStrategy.ITERATOR).maxThreadNum(4).build());
        assertSingleCachedSequential(parallel, 16);
        parallel.close();
    }

    // --- ParallelIteratorDoubleStream ---

    @Test
    public void parallelIteratorDoubleStream_sequentialField_isVolatile() throws Exception {
        Field f = ParallelIteratorDoubleStream.class.getDeclaredField("sequential");
        assertTrue(Modifier.isVolatile(f.getModifiers()), "ParallelIteratorDoubleStream.sequential must be declared volatile");
    }

    @Test
    public void parallelIteratorDoubleStream_sequential_concurrentCallsReturnSameInstance() throws Exception {
        DoubleStream parallel = DoubleStream.of(1.0, 2.0, 3.0, 4.0, 5.0)
                .map(d -> d)
                .parallel(ParallelSettings.builder().splitStrategy(SplitStrategy.ITERATOR).maxThreadNum(4).build());
        assertSingleCachedSequential(parallel, 16);
        parallel.close();
    }
    // ------------------------------------------------------------------------------------------------------
    // Stream review 2026-09-09 (pass B) - parallel flatMap leaked the in-flight mapped sub-stream on early close
    // ------------------------------------------------------------------------------------------------------

    /**
     * A worker's iterator holds the sub-stream it is draining in one field, clearing it before mapping the next
     * element. The pipeline's close handler clears and closes that same field, from whichever thread called
     * {@code close()}. A worker sitting inside {@code mapper.apply(...)} - past the clear, before the store -
     * therefore adopted a freshly opened sub-stream <i>after</i> the close handler had already run, and nothing
     * ever closed it.
     *
     * <p>The race window is exactly as wide as the mapper call, which is why an earlier attempt to pin this
     * could not reproduce it: with a trivial {@code Stream.of(x)} mapper it fires a couple of rounds in thirty.
     * A mapper that does a little work - as any mapper opening a real resource would - makes it near-certain,
     * so these rounds use one. Only the early-close path is affected; full consumption was always clean, and
     * the control below pins that.
     *
     * <p>Note the asymmetry: the fixed implementation can never fail this assertion, because "every opened
     * sub-stream is eventually closed" is an invariant, not a timing artefact. Timing only affects how
     * reliably the <i>unfixed</i> implementation is caught.
     */
    @Test
    public void testParallelFlatMap_earlyCloseClosesTheInFlightMappedStream() throws Exception {
        for (int round = 0; round < 12; round++) {
            for (final boolean iteratorBacked : new boolean[] { false, true }) {
                final AtomicInteger opened = new AtomicInteger();
                final AtomicInteger closed = new AtomicInteger();

                final Integer[] source = new Integer[512];
                Arrays.setAll(source, i -> i);

                final Stream<Integer> base = iteratorBacked ? Stream.of(Arrays.asList(source).iterator()) : Stream.of(source);

                base.parallel(8).flatMap(x -> {
                    // widen the window the way a mapper that opens something real would
                    final long until = System.nanoTime() + 200_000L;
                    while (System.nanoTime() < until) {
                        Thread.onSpinWait();
                    }

                    opened.incrementAndGet();
                    return Stream.of(x, x).onClose(closed::incrementAndGet);
                }).first(); // short-circuits, then closes the pipeline

                // let any worker still unwinding finish; a leak is permanent, so waiting only removes noise
                Thread.sleep(300);

                assertEquals(opened.get(), closed.get(),
                        "round " + round + (iteratorBacked ? " (iterator)" : " (array)") + ": mapped sub-streams left open");
            }
        }
    }

    /** Control: full consumption was never affected, and must stay that way. */
    @Test
    public void testParallelFlatMap_fullConsumptionClosesEveryMappedStream() throws Exception {
        for (final boolean iteratorBacked : new boolean[] { false, true }) {
            final AtomicInteger opened = new AtomicInteger();
            final AtomicInteger closed = new AtomicInteger();

            final Integer[] source = new Integer[512];
            Arrays.setAll(source, i -> i);

            final Stream<Integer> base = iteratorBacked ? Stream.of(Arrays.asList(source).iterator()) : Stream.of(source);

            final long count = base.parallel(8).flatMap(x -> {
                opened.incrementAndGet();
                return Stream.of(x, x).onClose(closed::incrementAndGet);
            }).count();

            Thread.sleep(200);

            assertEquals(1024, count);
            assertEquals(512, opened.get());
            assertEquals(opened.get(), closed.get(), iteratorBacked ? "(iterator)" : "(array)");
        }
    }

    /**
     * Same early-close leak, on the two shapes the sibling test does not reach: {@code SplitStrategy.ARRAY}
     * (a separate set of eight anonymous iterators in {@code ParallelArrayStream}) and a {@code flatMapToX}
     * site. The bodies are identical to the ones the sibling covers, so this is coverage rather than a
     * distinct defect - but 22 of the 24 rewritten sites were otherwise unpinned.
     */
    @Test
    @SuppressWarnings("deprecation")
    public void testParallelFlatMap_earlyCloseAlsoCoversArraySplitAndFlatMapToInt() throws Exception {
        for (int round = 0; round < 12; round++) {
            final Integer[] source = new Integer[512];
            Arrays.setAll(source, i -> i);

            // (a) SplitStrategy.ARRAY - the other eight iterator classes in ParallelArrayStream
            final AtomicInteger openedA = new AtomicInteger();
            final AtomicInteger closedA = new AtomicInteger();

            Stream.of(source)
                    .parallel(ParallelSettings.builder().splitStrategy(SplitStrategy.ARRAY).maxThreadNum(8).build())
                    .flatMap(x -> {
                        final long until = System.nanoTime() + 200_000L;
                        while (System.nanoTime() < until) {
                            Thread.onSpinWait();
                        }

                        openedA.incrementAndGet();
                        return Stream.of(x, x).onClose(closedA::incrementAndGet);
                    })
                    .first();

            // (b) flatMapToInt - a primitive mapped sub-stream rather than an object one
            final AtomicInteger openedB = new AtomicInteger();
            final AtomicInteger closedB = new AtomicInteger();

            Stream.of(source).parallel(8).flatMapToInt(x -> {
                final long until = System.nanoTime() + 200_000L;
                while (System.nanoTime() < until) {
                    Thread.onSpinWait();
                }

                openedB.incrementAndGet();
                return IntStream.of(x, x).onClose(closedB::incrementAndGet);
            }).first();

            Thread.sleep(300);

            assertEquals(openedA.get(), closedA.get(), "round " + round + " (ARRAY split): mapped sub-streams left open");
            assertEquals(openedB.get(), closedB.get(), "round " + round + " (flatMapToInt): mapped sub-streams left open");
        }
    }

}

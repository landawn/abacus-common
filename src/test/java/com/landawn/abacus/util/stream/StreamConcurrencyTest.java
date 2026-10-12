package com.landawn.abacus.util.stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import com.landawn.abacus.util.Duration;
import com.landawn.abacus.util.Fnn;
import com.landawn.abacus.util.IOUtil;
import com.landawn.abacus.util.MergeResult;
import com.landawn.abacus.util.MutableBoolean;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.NoCachingNoUpdating;
import com.landawn.abacus.util.function.BiFunction;
import com.landawn.abacus.util.function.BooleanSupplier;
import com.landawn.abacus.util.function.Function;

/** Stream concurrency contracts. */
public class StreamConcurrencyTest extends StreamTestSupport {

    // Async execution

    @Test
    public void test_asyncExecute() throws Exception {
        try {
            N.asyncExecute(() -> Stream.of(1, 2, 3).forEach(Fnn.throwIOException("oooh"))).get();

            fail("Should throw ExecutionException");
        } catch (final ExecutionException e) {
            e.printStackTrace();
        }

        try {
            Stream.repeat("a", 100).parallel(10).forEach(Fnn.throwIOException("oooh"));

            fail("Should throw IOException");
        } catch (final IOException e) {
            e.printStackTrace();
        }
    }

    // debounce

    @Test
    public void testDebounce_coldStreamEmitsOnlyLastElement() {
        // A cold in-memory stream yields all elements with ~0 inter-arrival gap, so only the last survives.
        assertEquals(Arrays.asList(5), Stream.of(1, 2, 3, 4, 5).debounce(com.landawn.abacus.util.Duration.ofSeconds(1)).toList());
        assertEquals(Arrays.asList(5), Stream.of(1, 2, 3, 4, 5).parallel().debounce(com.landawn.abacus.util.Duration.ofSeconds(1)).toList());
    }

    @Test
    public void testDebounce_emptyEmitsNothing() {
        assertTrue(Stream.<Integer> empty().debounce(com.landawn.abacus.util.Duration.ofSeconds(1)).toList().isEmpty());
        assertTrue(Stream.<Integer> empty().parallel().debounce(com.landawn.abacus.util.Duration.ofSeconds(1)).toList().isEmpty());
    }

    @Test
    public void testDebounce_invalidDurationThrows() {
        assertThrows(IllegalArgumentException.class, () -> Stream.of(1, 2, 3).debounce((com.landawn.abacus.util.Duration) null).toList());
        assertThrows(IllegalArgumentException.class, () -> Stream.of(1, 2, 3).debounce(com.landawn.abacus.util.Duration.ofMillis(0)).toList());
        assertThrows(IllegalArgumentException.class, () -> Stream.of(1, 2, 3).debounce(com.landawn.abacus.util.Duration.ofMillis(-100)).toList());
    }

    @Test
    public void testDebounce_singleElementSurvives() {
        assertEquals(Arrays.asList(42), Stream.of(42).debounce(com.landawn.abacus.util.Duration.ofSeconds(1)).toList());
    }

    @Test
    public void testDebounce_slowSourceAllSurviveWhenGapAtLeastDuration() {
        // Each element arrives >= 60ms after the previous (delay); with a 20ms quiet window all survive.
        assertEquals(Arrays.asList(1, 2, 3),
                Stream.of(1, 2, 3).delay(com.landawn.abacus.util.Duration.ofMillis(60)).debounce(com.landawn.abacus.util.Duration.ofMillis(20)).toList());
    }

    // interval

    @Test
    public void testInterval() {
        AtomicInteger counter = new AtomicInteger(0);
        List<Integer> result = Stream.interval(5, () -> counter.incrementAndGet()).limit(3).toList();
        assertEquals(Arrays.asList(1, 2, 3), result);
    }

    @Test
    public void testInterval_WithDelayAndSupplier() throws InterruptedException {
        List<String> results = new ArrayList<>();

        Stream.interval(100, 50, () -> "tick").limit(3).forEach(results::add);

        assertEquals(Arrays.asList("tick", "tick", "tick"), results);
    }

    @Test
    public void testInterval_WithLongFunction() throws InterruptedException {
        List<Long> values = new ArrayList<>();

        Stream.interval(50, timeMillis -> timeMillis).limit(3).forEach(values::add);

        assertEquals(3, values.size());

        for (Long value : values) {
            assertTrue(value > 0);
        }
    }

    @Test
    public void testInterval_WithSupplier() throws InterruptedException {
        List<Long> timestamps = new ArrayList<>();
        long startTime = System.currentTimeMillis();

        Stream.interval(50, s -> s).limit(3).forEach(timestamps::add);

        assertEquals(3, timestamps.size());

        for (int i = 1; i < timestamps.size(); i++) {
            long interval = timestamps.get(i) - timestamps.get(i - 1);
            assertTrue(interval >= 40 && interval <= 70, "Interval should be around 50ms");
        }
    }

    @Test
    public void testIntervalWithDelay() {
        AtomicInteger counter = new AtomicInteger(0);
        List<Integer> result = Stream.interval(1, 5, () -> counter.incrementAndGet()).limit(3).toList();
        assertEquals(Arrays.asList(1, 2, 3), result);
    }

    @Test
    public void testIntervalWithDelayAndSupplier() throws InterruptedException {
        final int[] counter = { 0 };
        long startTime = System.currentTimeMillis();

        List<Integer> result = Stream.interval(50, 10, () -> counter[0]++).limit(3).toList();

        long elapsedTime = System.currentTimeMillis() - startTime;

        assertEquals(Arrays.asList(0, 1, 2), result);
        assertTrue(elapsedTime >= 50);
    }

    @Test
    public void testIntervalWithLongFunction() throws InterruptedException {
        List<String> result = Stream.interval(10, time -> "Time: " + time).limit(2).toList();

        assertEquals(2, result.size());
        assertTrue(result.get(0).startsWith("Time: "));
        assertTrue(result.get(1).startsWith("Time: "));
    }

    @Test
    public void testIntervalWithSupplier() throws InterruptedException {
        final int[] counter = { 0 };
        List<Integer> result = Stream.interval(10, () -> counter[0]++).limit(3).toList();

        assertEquals(Arrays.asList(0, 1, 2), result);
        assertTrue(counter[0] >= 3);
    }

    @Test
    public void testIntervalWithTimeUnit() throws InterruptedException {
        final int[] counter = { 0 };

        List<Integer> result = Stream.interval(0, 10, TimeUnit.MILLISECONDS, () -> counter[0]++).limit(3).toList();

        assertEquals(Arrays.asList(0, 1, 2), result);
    }

    // observe

    @Test
    public void testObserve_BlockingQueueWithDuration() throws InterruptedException {
        BlockingQueue<String> queue = new ArrayBlockingQueue<>(10);
        queue.add("item1");
        queue.add("item2");

        List<String> results = Stream.observe(queue, Duration.ofMillis(100)).toList();

        assertTrue(results.contains("item1"));
        assertTrue(results.contains("item2"));
    }

    @Test
    public void testObserve_BlockingQueueWithHasMore() throws InterruptedException {
        BlockingQueue<Integer> queue = new ArrayBlockingQueue<>(10);
        BooleanSupplier hasMore = () -> true;

        CompletableFuture.runAsync(() -> {
            try {
                Thread.sleep(50);
                queue.add(1);
                Thread.sleep(50);
                queue.add(2);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        List<Integer> results = Stream.observe(queue, hasMore, 30).limit(2).toList();

        assertEquals(Arrays.asList(1, 2), results);
    }

    @Test
    public void testObserve_EmptyQueue() throws InterruptedException {
        BlockingQueue<String> queue = new ArrayBlockingQueue<>(10);

        List<String> results = Stream.observe(queue, Duration.ofMillis(50)).toList();

        assertTrue(results.isEmpty());
    }

    @Test
    public void testObserve_QueueWithTimeout() throws InterruptedException {
        BlockingQueue<Integer> queue = new ArrayBlockingQueue<>(10);

        CompletableFuture.runAsync(() -> {
            try {
                Thread.sleep(30);
                queue.add(1);
                Thread.sleep(30);
                queue.add(2);
                Thread.sleep(100);
                queue.add(3);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        List<Integer> results = Stream.observe(queue, Duration.ofMillis(100)).toList();

        assertEquals(2, results.size());
        assertTrue(results.contains(1));
        assertTrue(results.contains(2));
    }

    @Test
    public void testObserveWithBooleanSupplier() throws InterruptedException {
        BlockingQueue<String> queue = new ArrayBlockingQueue<>(10);
        MutableBoolean hasMore = MutableBoolean.of(true);

        queue.offer("item1");
        queue.offer("item2");

        CompletableFuture.runAsync(() -> {
            try {
                Thread.sleep(50);
                queue.offer("item3");
                Thread.sleep(50);
                hasMore.setFalse();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        List<String> result = Stream.observe(queue, hasMore::value, 20).toList();
        assertEquals(3, result.size());
        assertEquals(Arrays.asList("item1", "item2", "item3"), result);
    }

    @Test
    public void testObserveWithDuration() throws InterruptedException {
        BlockingQueue<String> queue = new ArrayBlockingQueue<>(10);
        queue.offer("item1");
        queue.offer("item2");

        CompletableFuture.runAsync(() -> {
            try {
                Thread.sleep(50);
                queue.offer("item3");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        List<String> result = Stream.observe(queue, Duration.ofMillis(100)).toList();
        assertTrue(result.size() >= 2);
        assertTrue(result.contains("item1"));
        assertTrue(result.contains("item2"));
    }

    // parallel

    @Test
    public void testParallel() {
        Stream<Integer> stream = Stream.of(1, 2, 3).parallel();
        assertTrue(stream.isParallel());
    }

    @Test
    public void testParallelStreamOperations() {
        List<Integer> result = Stream.range(1, 1000).parallel().filter(x -> x % 7 == 0).sorted().limit(10).toList();

        assertEquals(Arrays.asList(7, 14, 21, 28, 35, 42, 49, 56, 63, 70), result);
    }

    @Test
    public void testParallelWithMaxThreadNum() {
        Stream<Integer> stream = Stream.of(1, 2, 3).parallel(2);
        assertTrue(stream.isParallel());
    }

    // parallelConcat

    @Test
    public void testParallelConcat() {
        Stream<Integer> s1 = Stream.of(1, 2, 3);
        Stream<Integer> s2 = Stream.of(4, 5, 6);
        List<Integer> result = Stream.parallelConcat(Arrays.asList(s1, s2), 2).toList();
        assertEquals(6, result.size());
        assertTrue(result.containsAll(Arrays.asList(1, 2, 3, 4, 5, 6)));
    }

    @Test
    public void testParallelConcat_IteratorArray() throws Exception {
        @SuppressWarnings("unchecked")
        Iterator<Integer>[] iters = new Iterator[] { Arrays.asList(1, 2).iterator(), Arrays.asList(3, 4).iterator() };
        List<Integer> result = Stream.parallelConcat(iters).sorted().toList();
        assertEquals(Arrays.asList(1, 2, 3, 4), result);
    }

    @Test
    public void testParallelConcat_StreamArray() throws Exception {
        @SuppressWarnings("unchecked")
        Stream<Integer>[] streams = new Stream[] { Stream.of(1, 2), Stream.of(3, 4) };
        List<Integer> result = Stream.parallelConcat(streams).sorted().toList();
        assertEquals(Arrays.asList(1, 2, 3, 4), result);
    }

    @Test
    public void testParallelConcatCollectionsSnapshotMembership() {
        final List<Stream<Integer>> streamSources = new ArrayList<>();
        streamSources.add(Stream.of(1, 2));
        final Stream<Integer> streamConcat = Stream.parallelConcat(streamSources, 1, 8);
        streamSources.clear();
        assertEquals(Arrays.asList(1, 2), streamConcat.toList());

        final List<Iterator<Integer>> iteratorSources = new ArrayList<>();
        iteratorSources.add(Arrays.asList(3, 4).iterator());
        final Stream<Integer> iteratorConcat = Stream.parallelConcatIterators(iteratorSources, 1, 8);
        iteratorSources.clear();
        assertEquals(Arrays.asList(3, 4), iteratorConcat.toList());
    }

    @Test
    public void testParallelConcatEmpty() {
        Stream<Integer> s1 = Stream.empty();
        Stream<Integer> s2 = Stream.empty();
        List<Integer> result = Stream.parallelConcat(Arrays.asList(s1, s2), 2).toList();
        assertTrue(result.isEmpty());
    }

    @Test
    public void testParallelConcatIteratorVarargsSkipsNullSourcesAndRetainsNullElements() {
        for (int nullIndex = 0; nullIndex < 3; nullIndex++) {
            final List<Iterator<Integer>> sources = new ArrayList<>();
            sources.add(Arrays.asList(1, (Integer) null).iterator());
            sources.add(Arrays.asList(2, 3).iterator());
            sources.add(nullIndex, null);

            final List<Integer> result = Stream.parallelConcat(sources.get(0), sources.get(1), sources.get(2)).toList();

            assertEquals(4, result.size());
            assertTrue(result.containsAll(Arrays.asList(1, null, 2, 3)));
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testParallelConcatNullVarargsAreEmpty() {
        assertTrue(Stream.parallelConcat((Iterator<Integer>[]) null).toList().isEmpty());
        assertTrue(Stream.parallelConcat((Stream<Integer>[]) null).toList().isEmpty());
    }

    @Test
    public void testParallelConcatRejectsDisposableAfterRegularElement() {
        NoCachingNoUpdating.DisposableArray<Integer> disposable = NoCachingNoUpdating.DisposableArray.wrap(new Integer[] { 1 });
        Stream<Object> source = Stream.of("safe", disposable);

        assertThrows(IllegalStateException.class, () -> Stream.parallelConcat(Arrays.asList(source), 1, 8).count());
    }

    @Test
    public void testParallelConcatValidatesBufferSizeBeforeEmptyShortcut() {
        assertThrows(IllegalArgumentException.class, () -> Stream.parallelConcat(Collections.emptyList(), 1, 0));
        assertThrows(IllegalArgumentException.class, () -> Stream.parallelConcatIterators(Collections.emptyList(), 1, 0));
    }

    @Test
    public void testParallelConcatValidatesReadThreadNum() {
        // regression: readThreadNum <= 0 started zero reader threads and silently produced an EMPTY stream
        assertThrows(IllegalArgumentException.class, () -> Stream.parallelConcat(N.asList(Stream.of(1), Stream.of(2)), 0));
        assertThrows(IllegalArgumentException.class, () -> Stream.parallelConcatIterators(N.asList(N.asList(1).iterator(), N.asList(2).iterator()), -1));
        assertThrows(IllegalArgumentException.class, () -> Stream.parallelConcat(N.asList(Stream.of(1)), 1, 0));

        // null collections return an empty stream, matching concat(Collection) and the 3-arg overloads
        assertTrue(Stream.parallelConcat((Collection<Stream<Integer>>) null, 2).toList().isEmpty());
        assertTrue(Stream.parallelConcatIterators((Collection<Iterator<Integer>>) null, 2).toList().isEmpty());

        // normal behavior unchanged
        assertEquals(N.asSet(1, 2, 3), Stream.parallelConcat(N.asList(Stream.of(1, 2), Stream.of(3)), 2).toSet());
    }

    // parallelConcatIterators

    @Test
    public void testParallelConcatIterators() {
        Iterator<Integer> it1 = Arrays.asList(1, 2).iterator();
        Iterator<Integer> it2 = Arrays.asList(3, 4).iterator();
        List<Integer> result = Stream.parallelConcatIterators(Arrays.asList(it1, it2), 2).toList();
        assertEquals(4, result.size());
        assertTrue(result.containsAll(Arrays.asList(1, 2, 3, 4)));
    }

    @Test
    public void testParallelConcatIteratorsRejectsDisposableAfterRegularElement() {
        NoCachingNoUpdating.DisposableArray<Integer> disposable = NoCachingNoUpdating.DisposableArray.wrap(new Integer[] { 1 });
        Iterator<Object> source = Arrays.<Object> asList("safe", disposable).iterator();

        assertThrows(IllegalStateException.class, () -> Stream.parallelConcatIterators(Arrays.asList(source), 1, 8).count());
    }

    @Test
    public void testParallelConcatIteratorsSkipsNullSourcesAndRetainsNullElements() {
        for (int nullIndex = 0; nullIndex < 3; nullIndex++) {
            for (boolean explicitBufferSize : new boolean[] { false, true }) {
                final List<Iterator<Integer>> sources = new ArrayList<>();
                sources.add(Arrays.asList(1, (Integer) null).iterator());
                sources.add(Arrays.asList(2, 3).iterator());
                sources.add(nullIndex, null);

                final Stream<Integer> stream = explicitBufferSize ? Stream.parallelConcatIterators(sources, 1, 1) : Stream.parallelConcatIterators(sources, 1);

                assertEquals(Arrays.asList(1, null, 2, 3), stream.toList());
            }
        }
    }

    @Test
    public void testParallelConcatIteratorsWithOnlyNullSources() {
        final List<Iterator<Integer>> sources = Arrays.asList(null, null);

        assertTrue(Stream.parallelConcatIterators(sources).toList().isEmpty());
        assertTrue(Stream.parallelConcatIterators(sources, 1).toList().isEmpty());
        assertTrue(Stream.parallelConcatIterators(sources, 1, 1).toList().isEmpty());
        assertTrue(Stream.<Integer> parallelConcat((Iterator<Integer>) null, (Iterator<Integer>) null).toList().isEmpty());
    }

    // parallelMerge

    @Test
    public void testParallelMerge_Basic() {
        List<Stream<Integer>> streams = new ArrayList<>();
        streams.add(Stream.of(1, 4, 7));
        streams.add(Stream.of(2, 5, 8));
        streams.add(Stream.of(3, 6, 9));

        List<Integer> result = Stream.parallelMerge(streams, (a, b) -> a <= b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).toList();
        assertEquals(9, result.size());
        assertTrue(result.containsAll(Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9)));
    }

    @Test
    public void test_parallelMerge_streams() {
        Integer[] a = { 1, 3, 5 };
        Integer[] b = { 2, 4, 6 };
        Integer[] c = { 7, 8, 9 };

        assertEquals(N.toList(1, 2, 3, 4, 5, 6),
                Stream.parallelMerge(N.toList(Stream.of(a), Stream.of(b)), (e1, e2) -> e1 <= e2 ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).toList());

        assertEquals(N.toList(1, 2, 3, 4, 5, 6, 7, 8, 9), Stream
                .parallelMerge(N.toList(Stream.of(a), Stream.of(b), Stream.of(c)), (e1, e2) -> e1 <= e2 ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND)
                .toList());
        assertEquals(
                N.toList(1, 1, 2, 3, 3, 4, 5, 5, 6, 7, 8, 9), Stream
                        .parallelMerge(N.toList(Stream.of(a), Stream.of(b), Stream.of(c), Stream.of(a)),
                                (e1, e2) -> e1 <= e2 ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND)
                        .toList());

    }

    @Test
    public void testParallelMerge_WithMaxThreadNum() {
        List<Stream<Integer>> streams = new ArrayList<>();
        streams.add(Stream.of(1, 3, 5));
        streams.add(Stream.of(2, 4, 6));
        streams.add(Stream.of(7, 8, 9));
        streams.add(Stream.of(10, 11));

        List<Integer> result = Stream.parallelMerge(streams, (a, b) -> a <= b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND, 2).toList();
        assertEquals(11, result.size());
    }

    @Test
    public void testParallelMerge_WithMaxThreadNum_v2() {
        // Covers parallelMerge(Collection, BiFunction, int) - additional coverage
        List<Stream<Integer>> streams = Arrays.asList(Stream.of(1, 4, 7, 10, 13), Stream.of(2, 5, 8, 11, 14), Stream.of(3, 6, 9, 12, 15));
        List<Integer> result = Stream.parallelMerge(streams, (a, b) -> a <= b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND, 3).toList();
        assertEquals(15, result.size());
    }

    @Test
    public void testParallelMergeNullSelectorThrowsOnConsumption() {
        final List<Stream<Integer>> sources = Arrays.asList(Stream.of(1), Stream.of(2), Stream.of(3), Stream.of(4));

        assertThrows(IllegalArgumentException.class, () -> Stream.parallelMerge(sources, null, 2).toList());
        assertThrows(IllegalArgumentException.class, () -> Stream.merge(Stream.of(1), Stream.of(2), (BiFunction<Integer, Integer, MergeResult>) null).toList());
        assertThrows(IllegalArgumentException.class,
                () -> Stream.merge(N.asList(1).iterator(), N.asList(2).iterator(), (BiFunction<Integer, Integer, MergeResult>) null).toList());
        assertThrows(IllegalArgumentException.class,
                () -> Stream.parallelMergeIterators(N.asList(N.asList(1).iterator(), N.asList(2).iterator()), null, 2).toList());
        assertThrows(IllegalArgumentException.class, () -> Stream.parallelMergeIterables(N.asList(N.asList(1), N.asList(2)), null, 2).toList());
        assertThrows(IllegalArgumentException.class, () -> Stream.mergeIterables(N.asList(N.asList(1), N.asList(2)), null).toList());
    }

    @Test
    public void testParallelMergeSnapshotsSourceCollection() {
        final List<Stream<Integer>> sources = new ArrayList<>(Arrays.asList(Stream.of(1, 5), Stream.of(2, 6), Stream.of(3, 7), Stream.of(4, 8)));
        final Stream<Integer> merged = Stream.parallelMerge(sources, (x, y) -> x <= y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND, 2);

        sources.clear();

        assertEquals(Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8), merged.toList());
    }

    @Test
    public void testParallelMergeIterators() {
        List<Integer> a = N.toList(1, 3, 5);
        List<Integer> b = N.toList(2, 4, 6);
        List<Integer> c = N.toList(7, 8, 9);
        List<Integer> d = N.toList(7, 9, 10);

        assertEquals(N.toList(1, 2, 3, 4, 5, 6),
                Stream.parallelMergeIterables(N.toList(a, b), (e1, e2) -> e1 <= e2 ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).toList());

        assertEquals(N.toList(1, 2, 3, 4, 5, 6, 7, 8, 9),
                Stream.parallelMergeIterables(N.toList(a, b, c), (e1, e2) -> e1 <= e2 ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).toList());

        assertEquals(N.toList(1, 2, 3, 4, 5, 6, 7, 7, 8, 9, 9, 10),
                Stream.parallelMergeIterables(N.toList(a, b, c, d), (e1, e2) -> e1 <= e2 ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).toList());
    }

    // parallelMergeIterators

    @Test
    public void testParallelMergeIterators_Basic() {
        List<Iterator<Integer>> iterators = new ArrayList<>();
        iterators.add(Arrays.asList(1, 4, 7).iterator());
        iterators.add(Arrays.asList(2, 5, 8).iterator());
        iterators.add(Arrays.asList(3, 6, 9).iterator());

        List<Integer> result = Stream.parallelMergeIterators(iterators, (a, b) -> a <= b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).toList();
        assertEquals(9, result.size());
    }

    @Test
    public void testParallelMergeIterators_NewTest() {
        List<Integer> a = Arrays.asList(1, 3, 5);
        List<Integer> b = Arrays.asList(2, 4, 6);
        Collection<Iterator<Integer>> iterators = Arrays.asList(a.iterator(), b.iterator());
        List<Integer> result = Stream
                .parallelMergeIterators(iterators,
                        (x, y) -> x <= y ? com.landawn.abacus.util.MergeResult.TAKE_FIRST : com.landawn.abacus.util.MergeResult.TAKE_SECOND)
                .sorted()
                .toList();
        assertEquals(Arrays.asList(1, 2, 3, 4, 5, 6), result);
    }

    @Test
    public void testParallelMergeIteratorsSnapshotsSourceCollection() {
        final List<Iterator<Integer>> sources = new ArrayList<>(
                Arrays.asList(Arrays.asList(1, 5).iterator(), Arrays.asList(2, 6).iterator(), Arrays.asList(3, 7).iterator(), Arrays.asList(4, 8).iterator()));
        final Stream<Integer> merged = Stream.parallelMergeIterators(sources, (x, y) -> x <= y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND, 2);

        sources.clear();

        assertEquals(Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8), merged.toList());
    }

    @Test
    public void testParallelMergeIteratorsThreeSources() {
        // pins the size == 3 fast path, where the buffered stream's close handler is now kept reachable
        // so the background buffering thread stops when the result stream is closed
        final List<Iterator<Integer>> iterators = N.asList(N.asList(1, 4, 7).iterator(), N.asList(2, 5, 8).iterator(), N.asList(3, 6, 9).iterator());

        final List<Integer> merged = Stream.parallelMergeIterators(iterators, (x, y) -> x <= y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND, 2).toList();

        assertEquals(N.asList(1, 2, 3, 4, 5, 6, 7, 8, 9), merged);
    }

    // parallelZip

    @Test
    public void testParallelZip_nonPositiveThreads_uncovered() {
        assertThrows(IllegalArgumentException.class, () -> Stream.parallelZip(N.asList(1), N.asList(2), (x, y) -> x + y, 0));
    }

    @Test
    public void testParallelZip_ThreeIterables_WithDefaultValues() {
        // Covers Stream.parallelZip(Iterable, Iterable, Iterable, valueA, valueB, valueC, TriFunction, threads)
        List<Integer> a = Arrays.asList(1, 2, 3);
        List<String> b = Arrays.asList("a", "b");
        List<Double> c = Arrays.asList(0.1, 0.2, 0.3, 0.4);
        List<String> result = Stream.parallelZip(a, b, c, 0, "z", 0.0, (i, s, d) -> i + s + d, 2).toList();
        // parallel zip order is non-deterministic across all positions
        assertEquals(4, result.size());
        assertTrue(result.contains("1a0.1"));
        assertTrue(result.contains("2b0.2"));
        assertTrue(result.contains("3z0.3"));
        assertTrue(result.contains("0z0.4"));
    }

    @Test
    @Timeout(10)
    public void testParallelZipDoesNotAdvanceInputsForIncompleteTuple() {
        final AtomicInteger pairLeftNextCalls = new AtomicInteger();
        final CountDownLatch pairRightExhausted = new CountDownLatch(1);
        final Iterator<Integer> pairLeft = countingIterator(Arrays.asList(1, 2), pairLeftNextCalls);
        final Iterator<Integer> pairRight = signallingIterator(Collections.singletonList(10), pairRightExhausted);

        assertEquals(Collections.singletonList(11), Stream.parallelZip(pairLeft, pairRight, (a, b) -> {
            await(pairRightExhausted);
            return a + b;
        }, 2).toList());
        assertEquals(1, pairLeftNextCalls.get());
        assertEquals(2, pairLeft.next());

        final AtomicInteger triFirstNextCalls = new AtomicInteger();
        final AtomicInteger triSecondNextCalls = new AtomicInteger();
        final CountDownLatch triThirdExhausted = new CountDownLatch(1);
        final Iterator<Integer> triFirst = countingIterator(Arrays.asList(1, 2), triFirstNextCalls);
        final Iterator<Integer> triSecond = countingIterator(Arrays.asList(10, 20), triSecondNextCalls);
        final Iterator<Integer> triThird = signallingIterator(Collections.singletonList(100), triThirdExhausted);

        assertEquals(Collections.singletonList(111), Stream.parallelZip(triFirst, triSecond, triThird, (a, b, c) -> {
            await(triThirdExhausted);
            return a + b + c;
        }, 2).toList());
        assertEquals(1, triFirstNextCalls.get());
        assertEquals(1, triSecondNextCalls.get());
        assertEquals(2, triFirst.next());
        assertEquals(20, triSecond.next());

        final AtomicInteger collectionFirstNextCalls = new AtomicInteger();
        final AtomicInteger collectionSecondNextCalls = new AtomicInteger();
        final CountDownLatch collectionThirdExhausted = new CountDownLatch(1);
        final Iterator<Integer> collectionFirst = countingIterator(Arrays.asList(1, 2), collectionFirstNextCalls);
        final Iterator<Integer> collectionSecond = countingIterator(Arrays.asList(10, 20), collectionSecondNextCalls);
        final Iterator<Integer> collectionThird = signallingIterator(Collections.singletonList(100), collectionThirdExhausted);

        assertEquals(Collections.singletonList(111), Stream.parallelZipIterators(Arrays.asList(collectionFirst, collectionSecond, collectionThird), values -> {
            await(collectionThirdExhausted);
            return values.get(0) + values.get(1) + values.get(2);
        }, 2).toList());
        assertEquals(1, collectionFirstNextCalls.get());
        assertEquals(1, collectionSecondNextCalls.get());
        assertEquals(2, collectionFirst.next());
        assertEquals(20, collectionSecond.next());
    }



    // parallelZip(Iterable, Iterable, fn, 1) -> delegates to sequential zip (line 24956 -> 24993).
    @Test
    public void testParallelZipIterables_singleThreadFallback_uncovered() {
        final List<Integer> a = N.asList(1, 2, 3);
        final List<Integer> b = N.asList(10, 20, 30);
        assertEquals(N.asList(11, 22, 33), Stream.parallelZip(a, b, (x, y) -> x + y, 1).toList());
    }



    // parallelZip(Iterable, Iterable, valueForNoneA, valueForNoneB, fn, 1) -> sequential zip (line 25308 -> 25348).
    @Test
    public void testParallelZipIterablesWithDefaults_singleThreadFallback_uncovered() {
        final List<Integer> a = N.asList(1, 2, 3);
        final List<Integer> b = N.asList(10, 20);
        assertEquals(N.asList(11, 22, 3), Stream.parallelZip(a, b, 0, 0, (x, y) -> x + y, 1).toList());
    }



    // parallelZip(Iterator, Iterator, fn, 1) -> sequential zip (line 24993).
    @Test
    public void testParallelZipIterators_singleThreadFallback_uncovered() {
        final Iterator<Integer> a = N.asList(1, 2, 3, 4).iterator();
        final Iterator<Integer> b = N.asList(10, 20, 30).iterator();
        assertEquals(N.asList(11, 22, 33), Stream.parallelZip(a, b, (x, y) -> x + y, 1).toList());
    }



    // parallelZip(Iterator, Iterator, valueForNoneA, valueForNoneB, fn, 1) -> sequential zip (line 25348).
    @Test
    public void testParallelZipIteratorsWithDefaults_singleThreadFallback_uncovered() {
        final Iterator<Integer> a = N.asList(1, 2).iterator();
        final Iterator<Integer> b = N.asList(10, 20, 30).iterator();
        assertEquals(N.asList(11, 22, 30), Stream.parallelZip(a, b, 0, 0, (x, y) -> x + y, 1).toList());
    }

    @Test
    public void testParallelZipThreeStreamsNonPositiveThreadCountLeavesSourcesOpen() {
        final AtomicInteger closeCount = new AtomicInteger();
        final Stream<Integer> a = Stream.of(1, 2).onClose(closeCount::incrementAndGet);
        final Stream<Integer> b = Stream.of(10, 20).onClose(closeCount::incrementAndGet);
        final Stream<Integer> c = Stream.of(100, 200).onClose(closeCount::incrementAndGet);

        assertThrows(IllegalArgumentException.class, () -> Stream.parallelZip(a, b, c, (x, y, z) -> x + y + z, -1));
        assertEquals(0, closeCount.get());

        assertEquals(Arrays.asList(111, 222), Stream.zip(a, b, c, (x, y, z) -> x + y + z).toList());
        assertEquals(3, closeCount.get());
    }

    @Test
    public void testParallelZipThreeStreamsWithDefaultsNonPositiveThreadCountLeavesSourcesOpen() {
        final AtomicInteger closeCount = new AtomicInteger();
        final Stream<Integer> a = Stream.of(1, 2).onClose(closeCount::incrementAndGet);
        final Stream<Integer> b = Stream.of(10).onClose(closeCount::incrementAndGet);
        final Stream<Integer> c = Stream.of(100, 200, 300).onClose(closeCount::incrementAndGet);

        assertThrows(IllegalArgumentException.class, () -> Stream.parallelZip(a, b, c, 0, 0, 0, (x, y, z) -> x + y + z, 0));
        assertEquals(0, closeCount.get());

        assertEquals(Arrays.asList(111, 202, 300), Stream.zip(a, b, c, 0, 0, 0, (x, y, z) -> x + y + z).toList());
        assertEquals(3, closeCount.get());
    }

    @Test
    public void testParallelZipTwoStreamsNonPositiveThreadCountLeavesSourcesOpen() {
        final AtomicInteger closeCount = new AtomicInteger();
        final Stream<Integer> a = Stream.of(1, 2).onClose(closeCount::incrementAndGet);
        final Stream<Integer> b = Stream.of(10, 20).onClose(closeCount::incrementAndGet);

        assertThrows(IllegalArgumentException.class, () -> Stream.parallelZip(a, b, (x, y) -> x + y, 0));
        assertEquals(0, closeCount.get());

        assertEquals(Arrays.asList(11, 22), Stream.zip(a, b, (x, y) -> x + y).toList());
        assertEquals(2, closeCount.get());
    }

    @Test
    public void testParallelZipTwoStreamsWithDefaultsNonPositiveThreadCountLeavesSourcesOpen() {
        final AtomicInteger closeCount = new AtomicInteger();
        final Stream<Integer> a = Stream.of(1, 2).onClose(closeCount::incrementAndGet);
        final Stream<Integer> b = Stream.of(10).onClose(closeCount::incrementAndGet);

        assertThrows(IllegalArgumentException.class, () -> Stream.parallelZip(a, b, 0, 0, (x, y) -> x + y, 0));
        assertEquals(0, closeCount.get());

        assertEquals(Arrays.asList(11, 2), Stream.zip(a, b, 0, 0, (x, y) -> x + y).toList());
        assertEquals(2, closeCount.get());
    }

    @Test
    public void testParallelZipIterables() {
        List<List<Integer>> iterables = Arrays.asList(Arrays.asList(1, 2), Arrays.asList(10, 20));

        List<Integer> result = Stream.parallelZipIterables(iterables, values -> values.get(0) + values.get(1), 2).toList();
        Collections.sort(result);

        assertEquals(Arrays.asList(11, 22), result);
    }

    @Test
    public void testParallelZipIterables_WithDefaults() {
        List<List<Integer>> iterables = Arrays.asList(Arrays.asList(1, 2), Arrays.asList(10));

        List<Integer> result = Stream.parallelZipIterables(iterables, Arrays.asList(0, 99), values -> values.get(0) + values.get(1), 2).toList();
        Collections.sort(result);

        assertEquals(Arrays.asList(11, 101), result);
    }

    @Test
    public void testParallelZipIterators() {
        List<Iterator<Integer>> iterators = Arrays.asList(Arrays.asList(1, 2).iterator(), Arrays.asList(10, 20).iterator());

        List<Integer> result = Stream.parallelZipIterators(iterators, values -> values.get(0) + values.get(1), 2).toList();
        Collections.sort(result);

        assertEquals(Arrays.asList(11, 22), result);
    }

    @Test
    public void testParallelZipIterators_WithDefaults() {
        List<Iterator<Integer>> iterators = Arrays.asList(Arrays.asList(1, 2).iterator(), Arrays.asList(10).iterator());

        List<Integer> result = Stream.parallelZipIterators(iterators, Arrays.asList(0, 99), values -> values.get(0) + values.get(1), 2).toList();
        Collections.sort(result);

        assertEquals(Arrays.asList(11, 101), result);
    }

    @Test
    public void testPersist() throws IOException {
        File tempFile = new File(tempDir.toFile(), "persist_test.txt");
        List<String> data = Arrays.asList("line1", "line2", "line3");
        Stream.of(data).persist(tempFile);

        List<String> readBack = IOUtil.readAllLines(tempFile);
        assertEquals(data, readBack);
    }

    @Test
    public void testPersistToCSV() throws IOException {
        File tempFile = new File(tempDir.toFile(), "test.csv");
        List<List<String>> rows = Arrays.asList(Arrays.asList("a", "b", "c"), Arrays.asList("1", "2", "3"));
        Stream.of(rows).persistToCsv(N.toList("Column1", "Column2", "Column3"), tempFile);

        assertTrue(tempFile.exists());
        assertTrue(tempFile.length() > 0);
    }

    @Test
    public void testPersistToCsv_WithHeadersAndOutputStream() throws IOException {
        java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
        Map<String, Object> row1 = new LinkedHashMap<>();
        row1.put("name", "Alice");
        row1.put("age", 30);
        Map<String, Object> row2 = new LinkedHashMap<>();
        row2.put("name", "Bob");
        row2.put("age", 25);
        long count = Stream.of(row1, row2).persistToCsv(Arrays.asList("name", "age"), baos);
        assertEquals(2, count);
        String output = baos.toString();
        assertFalse(output.isEmpty());
    }

    @Test
    public void testPersistToJSON() throws IOException {
        File tempFile = new File(tempDir.toFile(), "test.json");
        List<Map<String, Object>> data = Arrays.asList(new LinkedHashMap<String, Object>() {
            {
                put("name", "Alice");
                put("age", 30);
            }
        });
        Stream.of(data).persistToJson(tempFile);

        assertTrue(tempFile.exists());
        assertTrue(tempFile.length() > 0);
    }

    @Test
    public void testSequential() {
        Stream<Integer> stream = Stream.of(1, 2, 3).parallel().sequential();
        assertFalse(stream.isParallel());
    }

    // sps

    @Test
    public void testSps() {
        List<Integer> result = Stream.of(1, 2, 3, 4, 5).sps(s -> s.filter(n -> n % 2 == 0)).toList();
        assertHaveSameElements(Arrays.asList(2, 4), result);
    }

    @Test
    public void testSps_MaxThreadAndChunkSize_AlreadyParallel() {
        // covers the isParallel() && maxThreadNum == maxThreadNum() branch in sps(int, int, Function)
        List<Integer> result = Stream.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10).parallel(2).sps(2, 3, chunk -> Stream.of(chunk).map(i -> i * 2)).sorted().toList();
        assertEquals(Arrays.asList(2, 4, 6, 8, 10, 12, 14, 16, 18, 20), result);
    }

    @Test
    public void testSps_MaxThreadAndChunkSize_DifferentThreadCount() {
        // covers the else branch: split(chunkSize).parallel(maxThreadNum, ...)
        List<Integer> result = Stream.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10).parallel(4).sps(2, 3, chunk -> Stream.of(chunk).map(i -> i * 2)).sorted().toList();
        assertEquals(Arrays.asList(2, 4, 6, 8, 10, 12, 14, 16, 18, 20), result);
    }

    @Test
    public void testSps_WithMaxThreadAndChunkSize() {
        List<Integer> result = Stream.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10).sps(2, 3, chunk -> Stream.of(chunk).map(i -> i * 2)).sorted().toList();
        assertEquals(Arrays.asList(2, 4, 6, 8, 10, 12, 14, 16, 18, 20), result);
    }

    // spsFilter

    @Test
    public void testSpsFilter() {
        Stream<Integer> stream = Stream.of(1, 2, 3, 4, 5);
        Stream<Integer> filtered = stream.spsFilter(n -> n % 2 == 0);
        assertNotNull(filtered);
        assertHaveSameElements(Arrays.asList(2, 4), filtered.toList());
    }

    @Test
    public void test_spsFilter() {
        Stream<String> strings = createStream01("a", "bb", "ccc", "dd", "e");
        List<String> result = strings.spsFilter(it -> it.length() <= 1).toList();
        assertHaveSameElements(Arrays.asList("a", "e"), result);
    }

    @Test
    public void testSpsFilter_WithMaxThreadNumAndChunkSize() {
        List<Integer> result = Stream.of(1, 2, 3, 4, 5).spsFilter(2, 2, n -> n % 2 == 0).sorted().toList();
        assertEquals(Arrays.asList(2, 4), result);
    }

    @Test
    public void testSpsFilterEmptyStream() {
        Stream<Integer> filtered = Stream.<Integer> empty().spsFilter(n -> n > 0);
        assertTrue(filtered.toList().isEmpty());
    }

    @Test
    public void testSpsFilterWithMaxThreadNum() {
        List<Integer> result = Stream.of(1, 2, 3, 4, 5, 6, 7, 8).spsFilter(2, x -> x % 2 == 0).toList();
        assertHaveSameElements(Arrays.asList(2, 4, 6, 8), result);
    }

    @Test
    public void testSpsFilterE() {
        Stream<Integer> stream = Stream.of(1, 2, 3, 4, 5);
        Stream<Integer> filtered = stream.spsFilterE(n -> {
            if (n == null) {
                throw new IllegalArgumentException("null value");
            }
            return n % 2 == 0;
        });
        assertHaveSameElements(Arrays.asList(2, 4), filtered.toList());
    }

    @Test
    public void testSpsFilterE_ExplicitThreadCount() {
        final List<Integer> result = Stream.of(1, 2, 3, 4, 5).spsFilterE(2, i -> i % 2 == 1).sorted().toList();

        assertEquals(Arrays.asList(1, 3, 5), result);
    }

    // spsFlatMap

    @Test
    public void testSpsFlatMap() {
        Stream<Integer> stream = Stream.of(1, 2, 3);
        Stream<Integer> flatMapped = stream.spsFlatMap(n -> Stream.of(n, n * 10));
        assertNotNull(flatMapped);
        List<Integer> result = flatMapped.toList();
        assertEquals(6, result.size());
        assertTrue(result.containsAll(Arrays.asList(1, 10, 2, 20, 3, 30)));
    }

    @Test
    public void testSpsFlatMap_WithMaxThreadNum() {
        List<String> result = Stream.of("a", "bb").spsFlatMap(2, s -> Stream.of(s, s.toUpperCase())).sorted().toList();
        assertEquals(4, result.size());
        assertTrue(result.contains("A"));
        assertTrue(result.contains("bb"));
    }

    @Test
    public void testSpsFlatMap_WithMaxThreadNumAndChunkSize() {
        List<Integer> result = Stream.of(1, 2, 3).spsFlatMap(2, 2, n -> Stream.of(n, n * 10)).sorted().toList();
        assertEquals(6, result.size());
        assertTrue(result.contains(10));
        assertTrue(result.contains(30));
    }

    // spsFlatmap

    @Test
    public void testSpsFlatmap_Collection() {
        List<String> result = Stream.of("a", "bb", "ccc").spsFlatmap(s -> Arrays.asList(s, s.toUpperCase())).sorted().toList();
        assertEquals(6, result.size());
        assertTrue(result.contains("A"));
        assertTrue(result.contains("ccc"));
    }

    @Test
    public void testSpsFlatmap_WithMaxThreadNum() {
        List<String> result = Stream.of("a", "bb").spsFlatmap(2, s -> Arrays.asList(s, s.toUpperCase())).sorted().toList();
        assertEquals(4, result.size());
        assertTrue(result.contains("A"));
    }

    @Test
    public void testSpsFlatmap_WithMaxThreadNumAndChunkSize() {
        List<Integer> result = Stream.of(1, 2, 3).spsFlatmap(2, 2, n -> Arrays.asList(n, n * 10)).sorted().toList();
        assertEquals(6, result.size());
        assertTrue(result.contains(10));
    }

    @Test
    public void testSpsFlatMapE() {
        Stream<Integer> stream = Stream.of(1, 2, 3);
        Stream<Integer> flatMapped = stream.spsFlatMapE(n -> {
            if (n == null) {
                throw new IllegalArgumentException("null value");
            }
            return Stream.of(n, n * 10);
        });
        List<Integer> result = flatMapped.toList();
        assertEquals(6, result.size());
    }

    @Test
    public void testSpsFlatMapE_WithMaxThreadNum() throws Exception {
        List<String> result = Stream.of("a", "bb").spsFlatMapE(2, s -> Stream.of(s, s.toUpperCase())).sorted().toList();
        assertEquals(4, result.size());
        assertTrue(result.contains("A"));
    }

    @Test
    public void testSpsFlatmapE_Collection() throws Exception {
        List<String> result = Stream.of("a", "bb", "ccc").spsFlatmapE(s -> Arrays.asList(s, s.toUpperCase())).sorted().toList();
        assertEquals(6, result.size());
        assertTrue(result.contains("A"));
    }

    @Test
    public void testSpsFlatmapE_WithMaxThreadNum() throws Exception {
        List<String> result = Stream.of("a", "bb").spsFlatmapE(2, s -> Arrays.asList(s, s.toUpperCase())).sorted().toList();
        assertEquals(4, result.size());
        assertTrue(result.contains("A"));
    }

    // spsMap

    @Test
    public void testSpsMap() {
        Stream<Integer> stream = Stream.of(1, 2, 3);
        Stream<String> mapped = stream.spsMap(n -> String.valueOf(n * 2));
        assertNotNull(mapped);
        assertHaveSameElements(Arrays.asList("2", "4", "6"), mapped.toList());
    }

    @Test
    public void testSpsMap_WithMaxThreadNum() {
        List<String> result = Stream.of("a", "bb", "ccc").spsMap(2, s -> s.toUpperCase()).sorted().toList();
        assertEquals(3, result.size());
        assertTrue(result.contains("A"));
        assertTrue(result.contains("CCC"));
    }

    @Test
    public void testSpsMap_WithMaxThreadNumAndChunkSize() {
        List<String> result = Stream.of(1, 2, 3).spsMap(2, 2, n -> n + "x").sorted().toList();
        assertEquals(3, result.size());
        assertTrue(result.contains("1x"));
        assertTrue(result.contains("3x"));
    }

    @Test
    public void testSpsMapEmptyStream() {
        Stream<String> mapped = Stream.<Integer> empty().spsMap(String::valueOf);
        assertTrue(mapped.toList().isEmpty());
    }

    @Test
    public void testSpsMapE() {
        Stream<Integer> stream = Stream.of(1, 2, 3);
        Stream<String> mapped = stream.spsMapE(n -> {
            if (n == null) {
                throw new IllegalArgumentException("null value");
            }
            return String.valueOf(n * 2);
        });
        assertHaveSameElements(Arrays.asList("2", "4", "6"), mapped.toList());
    }

    @Test
    public void testSpsMapE_WithMaxThreadNum() throws Exception {
        List<String> result = Stream.of("a", "bb", "ccc").spsMapE(2, s -> s.toUpperCase()).sorted().toList();
        assertEquals(3, result.size());
        assertTrue(result.contains("A"));
    }

}

package com.landawn.abacus.util.stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.RandomAccess;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.MergeResult;

/**
 * Regression tests for the cycle-2 Stream.java fixes of the 2026-09-24 stream review.
 * <ul>
 *   <li><b>C-089</b> - {@code Stream.split(CharSequence, ..)} read a mutable sequence lazily with its length fixed at the call.</li>
 *   <li><b>C-090</b> - caller-sized {@code ArrayBlockingQueue}s (parallelConcat bufferSize, addSubscriber queueSize) allocated
 *       the whole capacity up front: {@code OutOfMemoryError} for a large bound, sources left open.</li>
 *   <li><b>C-104</b> - {@code merge(Collection)}/{@code mergeIterators} folded left: O(n*k) selector calls and a
 *       {@code StackOverflowError} (sources never closed) at about 20,000 sources.</li>
 *   <li><b>C-105</b> - {@code ofReversed(List)} captured the size at creation but read the list lazily: silently wrong
 *       results instead of a {@code ConcurrentModificationException}.</li>
 *   <li><b>C-107</b> - the 7 primitive {@code zip(Collection, x[] valuesForNone, ..)} read the defaults array live.</li>
 *   <li><b>C-082</b> - an upstream failure under addSubscriber/filterWhile/takeWhile/dropWhileAddSubscriber was replaced
 *       by a cause-less {@code IllegalStateException}.</li>
 *   <li><b>C-081</b> - a bounded event-time window closed on the wall clock restarted at the last pulled element's time
 *       (overlapping windows, late data absorbed silently, or the emitted range reopened).</li>
 *   <li><b>C-106</b> - a {@code WindowHandler.timeWrapper} that changed the element or timestamp gave mixed windows or an
 *       empty stream; it now fails fast.</li>
 *   <li><b>R1-05</b> - {@code joinByRange} unjoined mapper: not called when nothing remains; {@code null} result = empty.</li>
 * </ul>
 */
@Tag("unit")
public class StreamReview20260924bTest extends TestBase {

    private static final BiFunction<Integer, Integer, MergeResult> TAKE_SMALLER = (a, b) -> a <= b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND;

    // ============================================================== C-089: split(CharSequence, ..)

    @Test
    public void testC089_split_char_snapshotsMutableSequence() {
        final StringBuilder sb = new StringBuilder("aa,bb,cc");
        final Stream<String> s = Stream.split(sb, ',');
        sb.setLength(2); // used to throw StringIndexOutOfBoundsException during traversal

        assertEquals(List.of("aa", "bb", "cc"), s.toList());

        final StringBuilder sb2 = new StringBuilder("x,y");
        final Stream<String> s2 = Stream.split(sb2, ',');
        sb2.append(",z");
        assertEquals(List.of("x", "y"), s2.toList());
    }

    @Test
    public void testC089_split_charSequenceDelimiter_snapshotsMutableSequence() {
        final StringBuilder sb = new StringBuilder("aa::bb::cc");
        final Stream<String> s = Stream.split(sb, "::");
        sb.setLength(3);

        assertEquals(List.of("aa", "bb", "cc"), s.toList());
    }

    @Test
    public void testC089_split_pattern_snapshotsMutableSequence() {
        final StringBuilder sb = new StringBuilder("aa  bb cc");
        final Stream<String> s = Stream.split(sb, Pattern.compile("\\s+"));
        sb.setLength(1);

        assertEquals(List.of("aa", "bb", "cc"), s.toList());
    }

    @Test
    public void testC089_split_nullEmptyAndUnicode() {
        assertEquals(List.of(), Stream.split((CharSequence) null, ',').toList());
        assertEquals(List.of(), Stream.split((CharSequence) null, "::").toList());
        assertEquals(List.of(), Stream.split((CharSequence) null, Pattern.compile(",")).toList());
        assertEquals(List.of(""), Stream.split(new StringBuilder(), ',').toList());

        final StringBuilder sb = new StringBuilder("😀,é,中");
        final Stream<String> s = Stream.split(sb, ',');
        sb.setLength(0);
        assertEquals(List.of("😀", "é", "中"), s.toList());
    }

    // ============================================================== C-090: bounded queues sized by the caller

    @Test
    public void testC090_newBoundedQueue_smallArrayBackedLargeLinked() {
        final BlockingQueue<Integer> small = Stream.newBoundedQueue(64);
        assertTrue(small instanceof ArrayBlockingQueue);
        assertEquals(64, small.remainingCapacity());

        final BlockingQueue<Integer> atThreshold = Stream.newBoundedQueue(StreamBase.MAX_BUFFERED_SIZE);
        assertTrue(atThreshold instanceof ArrayBlockingQueue);

        final BlockingQueue<Integer> large = Stream.newBoundedQueue(StreamBase.MAX_BUFFERED_SIZE + 1);
        assertTrue(large instanceof LinkedBlockingQueue);
        assertEquals(StreamBase.MAX_BUFFERED_SIZE + 1, large.remainingCapacity());

        final BlockingQueue<Integer> huge = Stream.newBoundedQueue(Integer.MAX_VALUE);
        assertEquals(Integer.MAX_VALUE, huge.remainingCapacity());

        // still bounded
        final BlockingQueue<Integer> bounded = Stream.newBoundedQueue(StreamBase.MAX_BUFFERED_SIZE + 2);
        for (int i = 0; i < StreamBase.MAX_BUFFERED_SIZE + 2; i++) {
            assertTrue(bounded.offer(i));
        }
        assertTrue(!bounded.offer(-1));
    }

    @Test
    public void testC090_parallelConcat_hugeBufferSize() {
        final AtomicInteger closed = new AtomicInteger();
        final List<Stream<Integer>> streams = List.of(Stream.of(List.of(1, 2).iterator()).onClose(closed::incrementAndGet),
                Stream.of(List.of(3).iterator()).onClose(closed::incrementAndGet), Stream.of(List.of(4, 5).iterator()).onClose(closed::incrementAndGet));

        final List<Integer> result;

        try (Stream<Integer> s = Stream.parallelConcat(streams, 2, Integer.MAX_VALUE)) {
            result = new ArrayList<>(s.toList());
        }

        result.sort(null);
        assertEquals(List.of(1, 2, 3, 4, 5), result);
        assertEquals(3, closed.get());
    }

    @Test
    public void testC090_parallelConcatIterators_hugeBufferSize() {
        final List<Iterator<Integer>> iterators = List.of(List.of(1, 2).iterator(), List.of(3).iterator(), List.of(4, 5).iterator());
        final List<Integer> result = new ArrayList<>(Stream.parallelConcatIterators(iterators, 2, Integer.MAX_VALUE).toList());

        result.sort(null);
        assertEquals(List.of(1, 2, 3, 4, 5), result);
    }

    @Test
    public void testC090_addSubscriberFamily_hugeQueueSize() {
        final ExecutorService executor = Executors.newCachedThreadPool();

        try {
            for (int op = 0; op < 3; op++) {
                final AtomicInteger closed = new AtomicInteger();
                final List<Integer> sub = new CopyOnWriteArrayList<>();
                final Stream<Integer> source = Stream.of(List.of(1, 2, 3).iterator()).onClose(closed::incrementAndGet);
                final Stream<Integer> main = switch (op) {
                    case 0 -> source.addSubscriber(s -> s.forEach(sub::add), Integer.MAX_VALUE, 30_000, executor);
                    case 1 -> source.filterWhileAddSubscriber(x -> x != 2, s -> s.forEach(sub::add), Integer.MAX_VALUE, 30_000, executor);
                    default -> source.dropWhileAddSubscriber(x -> x < 2, s -> s.forEach(sub::add), Integer.MAX_VALUE, 30_000, executor);
                };

                final List<Integer> result = main.toList();

                switch (op) {
                    case 0 -> {
                        assertEquals(List.of(1, 2, 3), result);
                        assertEquals(List.of(1, 2, 3), sub);
                    }
                    case 1 -> {
                        assertEquals(List.of(1, 3), result);
                        assertEquals(List.of(2), sub);
                    }
                    default -> {
                        assertEquals(List.of(2, 3), result);
                        assertEquals(List.of(1), sub);
                    }
                }

                assertEquals(1, closed.get(), "op " + op);
            }
        } finally {
            executor.shutdownNow();
        }
    }

    // ============================================================== C-104: merge(Collection) / mergeIterators

    @Test
    public void testC104_merge_20000Streams_noStackOverflow_allClosed() {
        final int k = 20_000;
        final AtomicInteger closed = new AtomicInteger();
        final List<Stream<Integer>> streams = new ArrayList<>(k);

        for (int i = k - 1; i >= 0; i--) {
            streams.add(Stream.of(i).onClose(closed::incrementAndGet));
        }

        final List<Integer> result;

        try (Stream<Integer> merged = Stream.merge(streams, TAKE_SMALLER)) {
            result = merged.toList();
        }

        assertEquals(k, result.size());

        for (int i = 0; i < k; i++) {
            assertEquals(i, result.get(i));
        }

        assertEquals(k, closed.get());
    }

    @Test
    public void testC104_merge_20000Streams_closeWithoutTraversal() {
        final int k = 20_000;
        final AtomicInteger closed = new AtomicInteger();
        final List<Stream<Integer>> streams = new ArrayList<>(k);

        for (int i = 0; i < k; i++) {
            streams.add(Stream.of(i).onClose(closed::incrementAndGet));
        }

        Stream.merge(streams, TAKE_SMALLER).close();
        assertEquals(k, closed.get());
    }

    @Test
    public void testC104_mergeIterators_and_parallelMergeSingleThread_20000Sources() {
        final int k = 20_000;
        final List<Iterator<Integer>> iterators = new ArrayList<>(k);
        final List<Stream<Integer>> streams = new ArrayList<>(k);

        for (int i = k - 1; i >= 0; i--) {
            iterators.add(List.of(i).iterator());
            streams.add(Stream.of(i));
        }

        final List<Integer> expected = new ArrayList<>(k);

        for (int i = 0; i < k; i++) {
            expected.add(i);
        }

        assertEquals(expected, Stream.mergeIterators(iterators, TAKE_SMALLER).toList());
        assertEquals(expected, Stream.parallelMerge(streams, TAKE_SMALLER, 1).toList());
    }

    @Test
    public void testC104_selectorCallCount_isLogarithmicInSourceCount() {
        final int k = 64;
        final int perSource = 100;
        final Random rnd = new Random(42);
        final List<List<Integer>> data = new ArrayList<>();

        for (int i = 0; i < k; i++) {
            final List<Integer> list = new ArrayList<>();

            for (int j = 0; j < perSource; j++) {
                list.add(rnd.nextInt(10_000));
            }

            list.sort(null);
            data.add(list);
        }

        final AtomicInteger calls = new AtomicInteger();
        final BiFunction<Integer, Integer, MergeResult> counting = (a, b) -> {
            calls.incrementAndGet();
            return a <= b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND;
        };

        final List<Integer> expected = new ArrayList<>();
        data.forEach(expected::addAll);
        expected.sort(null);

        final List<Stream<Integer>> streams = new ArrayList<>();
        data.forEach(list -> streams.add(Stream.of(list)));
        assertEquals(expected, Stream.merge(streams, counting).toList());
        // log2(64) = 6 merge levels, at most one selector call per element per level (the left fold made ~207,000)
        assertTrue(calls.get() <= k * perSource * 6, "selector calls: " + calls.get());

        calls.set(0);
        final List<Iterator<Integer>> iterators = new ArrayList<>();
        data.forEach(list -> iterators.add(list.iterator()));
        assertEquals(expected, Stream.mergeIterators(iterators, counting).toList());
        assertTrue(calls.get() <= k * perSource * 6, "selector calls: " + calls.get());

        calls.set(0);
        assertEquals(expected, Stream.mergeIterables(data, counting).toList());
        assertTrue(calls.get() <= k * perSource * 6, "selector calls: " + calls.get());
    }

    /**
     * For a selector that takes the first on ties, the balanced tree gives exactly the left fold's result:
     * elements with equal keys keep their source order.
     */
    @Test
    public void testC104_tieOrder_sameAsLeftFold() {
        final Random rnd = new Random(7);
        final BiFunction<int[], int[], MergeResult> byKey = (a, b) -> a[0] <= b[0] ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND;

        for (int k = 1; k <= 17; k++) {
            for (int round = 0; round < 5; round++) {
                final List<List<int[]>> data = new ArrayList<>();

                for (int src = 0; src < k; src++) {
                    final int n = rnd.nextInt(6);
                    final List<int[]> list = new ArrayList<>();

                    for (int pos = 0; pos < n; pos++) {
                        list.add(new int[] { rnd.nextInt(4), src, pos });
                    }

                    list.sort((x, y) -> Integer.compare(x[0], y[0]));
                    data.add(list);
                }

                // Reference: the former left fold, built from the two-source merge.
                Stream<int[]> fold = Stream.of(data.get(0));

                for (int src = 1; src < k; src++) {
                    fold = Stream.merge(fold, Stream.of(data.get(src)), byKey);
                }

                final List<String> expected = fold.map(Arrays::toString).toList();

                final List<Stream<int[]>> streams = new ArrayList<>();
                data.forEach(list -> streams.add(Stream.of(list)));
                assertEquals(expected, Stream.merge(streams, byKey).map(Arrays::toString).toList(), "k=" + k);

                final List<Iterator<int[]>> iterators = new ArrayList<>();
                data.forEach(list -> iterators.add(list.iterator()));
                assertEquals(expected, Stream.mergeIterators(iterators, byKey).map(Arrays::toString).toList(), "k=" + k);
            }
        }
    }

    @Test
    public void testC104_merge_nullAndEmptySources() {
        final List<Stream<Integer>> streams = new ArrayList<>();
        streams.add(null);
        streams.add(Stream.of(3, 5));
        streams.add(Stream.empty());
        streams.add(null);
        streams.add(Stream.of(1, 4));
        assertEquals(List.of(1, 3, 4, 5), Stream.merge(streams, TAKE_SMALLER).toList());

        final List<Iterator<Integer>> iterators = new ArrayList<>();
        iterators.add(null);
        iterators.add(List.of(2).iterator());
        iterators.add(null);
        assertEquals(List.of(2), Stream.mergeIterators(iterators, TAKE_SMALLER).toList());
    }

    /**
     * A closed input fails the call; the inputs before it are closed, the ones after it are left untouched (as the left
     * fold did).
     */
    @Test
    public void testC104_merge_closedInput_closesPrecedingOnly() {
        for (int closedIndex = 0; closedIndex < 7; closedIndex++) {
            final int[] closeCounts = new int[7];
            final List<Stream<Integer>> streams = new ArrayList<>();

            for (int i = 0; i < 7; i++) {
                final int idx = i;
                streams.add(Stream.of(i).onClose(() -> closeCounts[idx]++));
            }

            streams.get(closedIndex).close();
            closeCounts[closedIndex] = 0;

            assertThrows(IllegalStateException.class, () -> Stream.merge(streams, TAKE_SMALLER));

            for (int i = 0; i < 7; i++) {
                if (i < closedIndex) {
                    assertEquals(1, closeCounts[i], "closedIndex=" + closedIndex + ", i=" + i);
                } else {
                    assertEquals(0, closeCounts[i], "closedIndex=" + closedIndex + ", i=" + i);
                }
            }
        }
    }

    // ============================================================== C-105: ofReversed(List)

    @Test
    public void testC105_ofReversed_modificationAfterCreation_failsFast() {
        final List<Integer> list = new ArrayList<>(List.of(1, 2, 3));
        final Stream<Integer> added = Stream.ofReversed(list);
        list.add(4); // used to yield [3, 2, 1] silently
        assertThrows(ConcurrentModificationException.class, added::toList);

        final List<Integer> list2 = new ArrayList<>(List.of(1, 2, 3));
        final Stream<Integer> insertedAtHead = Stream.ofReversed(list2);
        list2.add(0, 0); // used to yield [2, 1, 0]
        assertThrows(ConcurrentModificationException.class, insertedAtHead::toList);

        final List<Integer> list3 = new ArrayList<>(List.of(1, 2, 3));
        final Stream<Integer> cleared = Stream.ofReversed(list3);
        list3.clear(); // used to throw IndexOutOfBoundsException
        assertThrows(ConcurrentModificationException.class, cleared::toList);

        final List<Integer> linked = new LinkedList<>(List.of(1, 2, 3));
        final Stream<Integer> linkedAdded = Stream.ofReversed(linked);
        linked.add(4);
        assertThrows(ConcurrentModificationException.class, linkedAdded::toList);

        final List<Integer> linked2 = new LinkedList<>(List.of(1, 2, 3));
        final Stream<Integer> linkedRemoved = Stream.ofReversed(linked2);
        linked2.remove(0); // used to throw IndexOutOfBoundsException
        assertThrows(ConcurrentModificationException.class, linkedRemoved::toList);
    }

    @Test
    public void testC105_ofReversed_modificationDuringTraversal_failsFast() {
        final List<Integer> list = new ArrayList<>(List.of(1, 2, 3));
        assertThrows(ConcurrentModificationException.class, () -> Stream.ofReversed(list).peek(x -> {
            if (x == 3) {
                list.add(0, 9);
            }
        }).toList());
    }

    @Test
    public void testC105_ofReversed_skipAndCount_failFast() {
        final List<Integer> list = new ArrayList<>(List.of(1, 2, 3));
        final Stream<Integer> skipped = Stream.ofReversed(list).skip(1);
        list.add(4);
        assertThrows(ConcurrentModificationException.class, skipped::toList);

        final List<Integer> list2 = new ArrayList<>(List.of(1, 2, 3));
        final Stream<Integer> counted = Stream.ofReversed(list2);
        list2.remove(0);
        assertThrows(ConcurrentModificationException.class, counted::count);
    }

    @Test
    public void testC105_ofReversed_snapshotListAndEdges() {
        final List<Integer> cow = new CopyOnWriteArrayList<>(List.of(1, 2, 3));
        final Stream<Integer> s = Stream.ofReversed(cow);
        cow.add(4);
        assertEquals(List.of(3, 2, 1), s.toList());

        final List<Integer> empty = new ArrayList<>();
        final Stream<Integer> emptyStream = Stream.ofReversed(empty);
        empty.add(1);
        assertEquals(List.of(), emptyStream.toList());

        assertEquals(List.of(), Stream.ofReversed((List<Integer>) null).toList());
        assertEquals(List.of("中", "😀"), Stream.ofReversed(new ArrayList<>(List.of("😀", "中"))).toList());
        assertEquals(Arrays.asList(null, "b", null), Stream.ofReversed(Arrays.asList(null, "b", null)).toList());
    }

    @Test
    public void testC105_ofReversed_skipCountLimit_values() {
        for (final List<Integer> list : List.of(new ArrayList<>(List.of(1, 2, 3, 4, 5)), new LinkedList<>(List.of(1, 2, 3, 4, 5)))) {
            assertEquals(List.of(4, 3, 2, 1), Stream.ofReversed(list).skip(1).toList());
            assertEquals(List.of(3, 2), Stream.ofReversed(list).skip(2).limit(2).toList());
            assertEquals(List.of(1), Stream.ofReversed(list).skip(4).toList());
            assertEquals(List.of(), Stream.ofReversed(list).skip(5).toList());
            assertEquals(List.of(), Stream.ofReversed(list).skip(Long.MAX_VALUE).toList());
            assertEquals(5, Stream.ofReversed(list).count());
            assertEquals(3, Stream.ofReversed(list).skip(2).count());
            assertEquals(0, Stream.ofReversed(list).skip(7).count());
            assertEquals(List.of(5, 3, 1), Stream.ofReversed(list).step(2).toList());
            assertEquals(List.of(5, 4, 3, 2, 1), Stream.ofReversed(list).toList());
        }
    }

    /** A RandomAccess list is still skipped in O(1): the skipped elements are never read. */
    @Test
    public void testC105_ofReversed_randomAccessSkipDoesNotReadSkippedElements() {
        final int size = 1_000_000;
        final AtomicInteger gets = new AtomicInteger();
        final List<Integer> list = new CountingRandomAccessList(size, gets);

        assertEquals(List.of(9, 8, 7, 6, 5, 4, 3, 2, 1, 0), Stream.ofReversed(list).skip(size - 10).toList());
        assertTrue(gets.get() <= 12, "get calls: " + gets.get());

        gets.set(0);
        assertEquals(size, Stream.ofReversed(list).count());
        assertTrue(gets.get() <= 2, "get calls: " + gets.get());
    }

    private static final class CountingRandomAccessList extends java.util.AbstractList<Integer> implements RandomAccess {
        private final int size;
        private final AtomicInteger gets;

        CountingRandomAccessList(final int size, final AtomicInteger gets) {
            this.size = size;
            this.gets = gets;
        }

        @Override
        public Integer get(final int index) {
            gets.incrementAndGet();
            return index;
        }

        @Override
        public int size() {
            return size;
        }
    }

    // ============================================================== C-107: primitive zip(Collection, x[] valuesForNone, ..)

    @Test
    public void testC107_primitiveZip_defaultsArraySnapshotted() {
        final char[] cd = { 'a', 'a' };
        final Stream<String> cs = Stream.zip(List.of(CharStream.of('x', 'y'), CharStream.of('z')), cd, Arrays::toString);
        cd[1] = '!';
        assertEquals(List.of("[x, z]", "[y, a]"), cs.toList());

        final byte[] bd = { 0, 0 };
        final Stream<String> bs = Stream.zip(List.of(ByteStream.of((byte) 1, (byte) 2), ByteStream.of((byte) 10)), bd, Arrays::toString);
        bd[1] = 99;
        assertEquals(List.of("[1, 10]", "[2, 0]"), bs.toList());

        final short[] sd = { 0, 0 };
        final Stream<String> ss = Stream.zip(List.of(ShortStream.of((short) 1, (short) 2), ShortStream.of((short) 10)), sd, Arrays::toString);
        sd[1] = 99;
        assertEquals(List.of("[1, 10]", "[2, 0]"), ss.toList());

        final int[] id = { 0, 0 };
        final Stream<String> is = Stream.zip(List.of(IntStream.of(1, 2), IntStream.of(10)), id, Arrays::toString);
        id[1] = 99;
        assertEquals(List.of("[1, 10]", "[2, 0]"), is.toList());

        final long[] ld = { 0, 0 };
        final Stream<String> ls = Stream.zip(List.of(LongStream.of(1, 2), LongStream.of(10)), ld, Arrays::toString);
        ld[1] = 99;
        assertEquals(List.of("[1, 10]", "[2, 0]"), ls.toList());

        final float[] fd = { 0, 0 };
        final Stream<String> fs = Stream.zip(List.of(FloatStream.of(1, 2), FloatStream.of(10)), fd, Arrays::toString);
        fd[1] = 99;
        assertEquals(List.of("[1.0, 10.0]", "[2.0, 0.0]"), fs.toList());

        final double[] dd = { 0, -0.0 };
        final Stream<String> ds = Stream.zip(List.of(DoubleStream.of(1, 2), DoubleStream.of(10)), dd, Arrays::toString);
        dd[1] = Double.NaN;
        assertEquals(List.of("[1.0, 10.0]", "[2.0, -0.0]"), ds.toList());
    }

    @Test
    public void testC107_primitiveZip_sizeCheckStillEager() {
        assertThrows(IllegalArgumentException.class, () -> Stream.zip(List.of(IntStream.of(1)), new int[0], Arrays::toString));
        assertThrows(IllegalArgumentException.class, () -> Stream.zip(List.of(IntStream.of(1)), (int[]) null, Arrays::toString));
        assertEquals(List.of(), Stream.zip(new ArrayList<IntStream>(), new int[0], Arrays::toString).toList());
        assertEquals(List.of(), Stream.zip((List<IntStream>) null, (int[]) null, Arrays::toString).toList());
    }

    // ============================================================== R1-05: joinByRange unjoined mapper (documented behaviour lock)

    @Test
    public void testR105_joinByRange_unjoinedMapper_calledOnlyWhenSomethingRemains_nullIsEmpty() {
        final AtomicInteger mapperCalls = new AtomicInteger();

        final List<String> allJoined = Stream.of(10, 30)
                .joinByRange(List.of(5, 25).iterator(), (slot, t) -> t < slot, java.util.stream.Collectors.toList(), (slot, ts) -> slot + ":" + ts,
                        remaining -> {
                            mapperCalls.incrementAndGet();
                            return Stream.of("rest");
                        })
                .toList();

        assertEquals(List.of("10:[5]", "30:[25]"), allJoined);
        assertEquals(0, mapperCalls.get());

        final List<String> withRest = Stream.of(10, 30)
                .joinByRange(Stream.of(5, 25, 40), (slot, t) -> t < slot, java.util.stream.Collectors.toList(), (slot, ts) -> slot + ":" + ts,
                        remaining -> {
                            mapperCalls.incrementAndGet();
                            return Stream.of("rest:" + com.landawn.abacus.util.Iterators.count(remaining));
                        })
                .toList();

        assertEquals(List.of("10:[5]", "30:[25]", "rest:1"), withRest);
        assertEquals(1, mapperCalls.get());

        final List<String> nullResult = Stream.of(10)
                .joinByRange(List.of(5, 25).iterator(), (slot, t) -> t < slot, java.util.stream.Collectors.toList(), (slot, ts) -> slot + ":" + ts,
                        remaining -> null)
                .toList();

        assertEquals(List.of("10:[5]"), nullResult);
    }

    // ============================================================== C-082: addSubscriber family keeps the upstream failure

    private static Stream<Integer> failingAt(final int failAt, final RuntimeException boom) {
        return Stream.of(1, 2, 3, 4).map(x -> {
            if (x == failAt) {
                throw boom;
            }

            return x;
        });
    }

    private static Stream<Integer> attach(final int op, final Stream<Integer> upstream,
            final com.landawn.abacus.util.Throwables.Consumer<? super Stream<Integer>, ? extends Exception> subscriber) {
        return switch (op) {
            case 0 -> upstream.addSubscriber(subscriber);
            case 1 -> upstream.filterWhileAddSubscriber(x -> x % 2 == 1, subscriber);
            case 2 -> upstream.takeWhileAddSubscriber(x -> x < 10, subscriber);
            default -> upstream.dropWhileAddSubscriber(x -> x < 10, subscriber); // still in the dropping phase at the failure
        };
    }

    private static Throwable thrownBy(final Runnable action) {
        try {
            action.run();
        } catch (final Throwable t) {
            return t;
        }

        throw new AssertionError("nothing thrown");
    }

    @Test
    public void testC082_upstreamFailure_reachesCallerUnchanged_allFourMethods() {
        for (int op = 0; op < 4; op++) {
            final IllegalArgumentException boom = new IllegalArgumentException("up boom");
            final List<Integer> sub = new CopyOnWriteArrayList<>();
            final Stream<Integer> main = attach(op, failingAt(2, boom), s -> s.forEach(sub::add));

            final Throwable thrown = thrownBy(main::toList);
            assertTrue(thrown == boom, "op " + op + ": " + thrown);
            assertEquals(0, boom.getSuppressed().length, "op " + op);
        }
    }

    @Test
    public void testC082_upstreamFailureAtFirstElement_andOtherTerminals() {
        for (int op = 0; op < 4; op++) {
            final int fop = op;
            final IllegalArgumentException boom = new IllegalArgumentException("first");
            final Throwable thrown = thrownBy(() -> attach(fop, failingAt(1, boom), s -> s.count()).forEach(x -> {
            }));
            assertTrue(thrown == boom, "op " + op + ": " + thrown);

            final java.io.UncheckedIOException ioBoom = new java.io.UncheckedIOException(new java.io.IOException("io"));
            final Throwable thrown2 = thrownBy(() -> attach(fop, failingAt(2, ioBoom), s -> s.toList()).count());
            assertTrue(thrown2 == ioBoom, "op " + op + ": " + thrown2);
        }
    }

    @Test
    public void testC082_subscriberIgnoringItsStream_upstreamFailureReachesCaller() {
        for (int op = 0; op < 4; op++) {
            final IllegalArgumentException boom = new IllegalArgumentException("up boom");
            final Throwable thrown = thrownBy(attach(op, failingAt(2, boom), s -> {
            })::toList);
            assertTrue(thrown == boom, "op " + op + ": " + thrown);
            assertEquals(0, boom.getSuppressed().length, "op " + op);
        }
    }

    @Test
    public void testC082_subscriberSeesIllegalStateExceptionCausedByUpstreamFailure() {
        for (int op = 0; op < 4; op++) {
            final IllegalArgumentException boom = new IllegalArgumentException("up boom");
            final java.util.concurrent.atomic.AtomicReference<Throwable> seen = new java.util.concurrent.atomic.AtomicReference<>();
            final Stream<Integer> main = attach(op, failingAt(3, boom), s -> {
                try {
                    s.forEach(x -> {
                    });
                } catch (final IllegalStateException e) {
                    seen.set(e);
                }
            });

            assertTrue(thrownBy(main::toList) == boom, "op " + op);
            assertTrue(seen.get() instanceof IllegalStateException, "op " + op + ": " + seen.get());
            assertTrue(seen.get().getCause() == boom, "op " + op + ": cause " + seen.get().getCause());
            assertEquals(0, boom.getSuppressed().length, "op " + op);
        }
    }

    @Test
    public void testC082_independentSubscriberFailure_isSuppressedOntoUpstreamFailure() {
        for (int op = 0; op < 4; op++) {
            final IllegalArgumentException boom = new IllegalArgumentException("up boom");
            final IllegalStateException subOwn = new IllegalStateException("sub own");
            final java.util.concurrent.CountDownLatch subscriberFailed = new java.util.concurrent.CountDownLatch(1);

            // The upstream fails only after the subscriber has failed on its own (and closed its stream).
            final Stream<Integer> upstream = Stream.of(1, 2, 3, 4).map(x -> {
                if (x == 2) {
                    try {
                        subscriberFailed.await(3, java.util.concurrent.TimeUnit.SECONDS); // filterWhile: the subscriber starts only on close
                        Thread.sleep(300);
                    } catch (final InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }

                    throw boom;
                }

                return x;
            });

            final Stream<Integer> main = attach(op, upstream, s -> {
                try {
                    throw subOwn;
                } finally {
                    subscriberFailed.countDown();
                }
            });

            assertTrue(thrownBy(main::toList) == boom, "op " + op);
            assertEquals(List.of(subOwn), Arrays.asList(boom.getSuppressed()), "op " + op);
        }
    }

    @Test
    public void testC082_noUpstreamFailure_subscriberFailureStillPropagates() {
        for (int op = 0; op < 4; op++) {
            final IllegalStateException subOwn = new IllegalStateException("sub own");
            final Throwable thrown = thrownBy(attach(op, Stream.of(1, 2, 3), s -> {
                throw subOwn;
            })::toList);

            boolean found = false;

            for (Throwable t = thrown; t != null; t = t.getCause()) {
                found |= t == subOwn;
            }

            assertTrue(found, "op " + op + ": " + thrown);
        }
    }

    @Test
    public void testC082_dropWhile_failureAfterDroppingPhase() {
        final IllegalArgumentException boom = new IllegalArgumentException("late boom");
        final List<Integer> sub = new CopyOnWriteArrayList<>();
        final Stream<Integer> main = failingAt(3, boom).dropWhileAddSubscriber(x -> x < 2, s -> s.forEach(sub::add));

        assertTrue(thrownBy(main::toList) == boom);
        assertEquals(List.of(1), sub);

        // an independent subscriber failure is suppressed onto a failure after the dropping phase too
        final IllegalArgumentException boom2 = new IllegalArgumentException("late boom 2");
        final IllegalStateException subOwn = new IllegalStateException("sub own");
        final Stream<Integer> main2 = failingAt(3, boom2).dropWhileAddSubscriber(x -> x < 2, s -> {
            s.toList();
            throw subOwn;
        });

        assertTrue(thrownBy(main2::toList) == boom2);
        assertEquals(List.of(subOwn), Arrays.asList(boom2.getSuppressed()));
    }

    // ============================================================== C-081: bounded event-time window restart after a wall-clock close

    /** Script tokens: {@code "e:<ms>"} = an element whose event time is {@code start + ms}; {@code "s:<ms>"} = the source sleeps. */
    private static java.util.Iterator<String> sleepingSource(final String... script) {
        return new java.util.Iterator<>() {
            private int i = 0;
            private String pending;

            @Override
            public boolean hasNext() {
                while (pending == null && i < script.length) {
                    final String token = script[i++];

                    if (token.startsWith("s:")) {
                        try {
                            Thread.sleep(Long.parseLong(token.substring(2)));
                        } catch (final InterruptedException e) {
                            Thread.currentThread().interrupt();
                            throw new IllegalStateException(e);
                        }
                    } else {
                        pending = token.substring(2);
                    }
                }

                return pending != null;
            }

            @Override
            public String next() {
                if (!hasNext()) {
                    throw new java.util.NoSuchElementException();
                }

                final String result = pending;
                pending = null;
                return result;
            }
        };
    }

    /**
     * Runs a live event-time window over a sleeping source (500 ms windows starting now) and returns
     * "windows | late" where each late entry is "element->[start,end)" relative to the start.
     * mode 0 = public bounded (asynchronous pulling), 1 = bounded with synchronous pulling, 2 = sliding core (control).
     */
    private static String runLiveWindow(final int mode, final int maxWindowSize, final String... script) {
        final long start = System.currentTimeMillis();
        final List<String> late = new CopyOnWriteArrayList<>();
        final Stream.WindowHandler<String, List<String>> handler = Stream.WindowHandler.<String, List<String>> builder()
                .timeExtractor(s -> start + Long.parseLong(s))
                .onLateData((ws, we, et, e, r) -> late.add(e + "->[" + (ws - start) + "," + (we - start) + ")"))
                .build();
        final Stream<String> source = Stream.of(sleepingSource(script));
        final com.landawn.abacus.util.Duration d500 = com.landawn.abacus.util.Duration.ofMillis(500);
        final List<List<String>> windows = switch (mode) {
            case 0 -> source.window(d500, maxWindowSize, () -> start, handler, Collectors.<String> toList()).toList();
            case 1 -> source.window(d500, maxWindowSize, () -> start, handler, Collectors.<String> toList(), false).toList();
            default -> source.window(d500, d500, () -> start, handler, Collectors.<String> toList()).toList();
        };

        return windows + " | " + late;
    }

    /** Live timing: retry a few times so a slow (cold) JVM cannot fail the test; any matching run passes. */
    private static void assertLiveWindow(final String expected, final int mode, final int maxWindowSize, final String... script) {
        runLiveWindow(mode, maxWindowSize, "e:1", "e:2"); // warm-up
        String actual = null;

        for (int attempt = 0; attempt < 3; attempt++) {
            actual = runLiveWindow(mode, maxWindowSize, script);

            if (expected.equals(actual)) {
                return;
            }
        }

        assertEquals(expected, actual, "mode " + mode + " " + Arrays.toString(script));
    }

    @Test
    public void testC081_boundedEventTime_wallClockClose_nextWindowStartsAtEnd_async() {
        // 200 arrives after [0,500) closed on the wall clock: it is late for [0,500), not the start of an overlapping window
        assertLiveWindow("[[100], [700]] | [200->[0,500)]", 0, 10, "e:100", "s:800", "e:200", "e:700");
        // the last pulled element (-50) was ignored as late: it must not reopen the range just emitted
        assertLiveWindow("[[100], [700]] | [200->[0,500), 150->[0,500)]", 0, 10, "e:100", "e:-50", "s:800", "e:200", "e:150", "e:700");
        assertLiveWindow("[[100], [700]] | [50->[0,500)]", 0, 10, "e:100", "s:100", "e:-50", "s:700", "e:50", "e:700");
    }

    @Test
    public void testC081_boundedEventTime_wallClockClose_nextWindowStartsAtEnd_sync() {
        // synchronous pulling: 200 is pulled (by the blocking hasNext) into [0,500); the window then closes on the wall clock
        assertLiveWindow("[[100, 200], [700]] | [300->[0,500)]", 1, 10, "e:100", "s:800", "e:200", "e:300", "e:700");
        // the ignored late element -50 was the last one pulled: the next window must not restart at the closed window's start
        assertLiveWindow("[[100], [700]] | [200->[0,500)]", 1, 10, "e:100", "s:800", "e:-50", "e:200", "e:700");
    }

    @Test
    public void testC081_boundedMatchesSlidingCore() {
        assertLiveWindow("[[100], [700]] | [200->[0,500)]", 2, 10, "e:100", "s:800", "e:200", "e:700");
        assertLiveWindow("[[100], [700]] | [200->[0,500), 300->[0,500)]", 0, 10, "e:100", "s:800", "e:200", "e:300", "e:700");
        assertLiveWindow("[[100], [700]] | [200->[0,500), 300->[0,500)]", 2, 10, "e:100", "s:800", "e:200", "e:300", "e:700");
    }

    @Test
    public void testC081_boundedEventTime_countClose_unchanged() {
        // a count close continues from the last accumulated event time (StreamTest pins the in-order case)
        assertLiveWindow("[[100, 200], [300], [900]] | []", 0, 2, "e:100", "e:200", "e:300", "s:800", "e:900");
        assertLiveWindow("[[100, 200], [300], [900]] | []", 1, 2, "e:100", "e:200", "e:300", "s:800", "e:900");
        assertLiveWindow("[[300, 100], [250, 260], [900]] | []", 0, 2, "e:300", "e:100", "e:250", "e:260", "s:800", "e:900");
    }

    @Test
    public void testC081_boundedProcessingTime_unchanged() {
        // processing time: a count close restarts at the wall clock, a duration close at the window end
        final List<List<String>> windows = Stream.of(sleepingSource("e:a", "e:b", "e:c", "s:700", "e:d"))
                .window(com.landawn.abacus.util.Duration.ofMillis(500), 2)
                .toList();

        assertEquals(List.of(List.of("a", "b"), List.of("c"), List.of("d")), windows);
    }

    // ============================================================== C-106: WindowHandler.timeWrapper must not change element or time

    private static Stream.WindowHandler<String, List<String>> handlerWithWrapper(
            final com.landawn.abacus.util.function.ObjLongFunction<? super String, com.landawn.abacus.util.Timed<String>> wrapper) {
        return Stream.WindowHandler.<String, List<String>> builder().timeExtractor(Long::parseLong).timeWrapper(wrapper).build();
    }

    private static List<List<String>> tumbling(final Stream.WindowHandler<String, List<String>> handler, final String... values) {
        final com.landawn.abacus.util.Duration d10 = com.landawn.abacus.util.Duration.ofMillis(10);
        return Stream.of(values).window(d10, d10, () -> 0L, handler, Collectors.<String> toList()).toList();
    }

    private static List<List<String>> sliding(final Stream.WindowHandler<String, List<String>> handler, final String... values) {
        return Stream.of(values)
                .window(com.landawn.abacus.util.Duration.ofMillis(10), com.landawn.abacus.util.Duration.ofMillis(5), () -> 0L, handler,
                        Collectors.<String> toList())
                .toList();
    }

    private static List<List<String>> bounded(final Stream.WindowHandler<String, List<String>> handler, final String... values) {
        return Stream.of(values).window(com.landawn.abacus.util.Duration.ofMillis(10), 100, () -> 0L, handler, Collectors.<String> toList()).toList();
    }

    @Test
    public void testC106_identityWrapper_works() {
        final AtomicInteger calls = new AtomicInteger();
        final Stream.WindowHandler<String, List<String>> identity = handlerWithWrapper((v, t) -> {
            calls.incrementAndGet();
            return com.landawn.abacus.util.Timed.of(v, t);
        });

        assertEquals(List.of(List.of("1", "2", "3"), List.of("12", "13")), tumbling(identity, "1", "2", "3", "12", "13"));
        assertEquals(List.of(List.of("1", "2", "7"), List.of("7", "12", "13"), List.of("12", "13")), sliding(identity, "1", "2", "7", "12", "13"));
        assertEquals(List.of(List.of("1", "2", "3"), List.of("12", "13")), bounded(identity, "1", "2", "3", "12", "13"));
        assertTrue(calls.get() > 0);

        final Stream.WindowHandler<String, List<String>> methodRef = handlerWithWrapper(com.landawn.abacus.util.Timed::of);
        assertEquals(List.of(List.of("1", "2", "3"), List.of("12", "13")), tumbling(methodRef, "1", "2", "3", "12", "13"));
    }

    @Test
    public void testC106_valueChangingWrapper_failsFast() {
        final Stream.WindowHandler<String, List<String>> tagging = handlerWithWrapper((v, t) -> com.landawn.abacus.util.Timed.of("W" + v, t));

        // used to give [[W1, 2, 3], [W12, 13]] (wrapped and unwrapped elements mixed)
        assertThrows(IllegalArgumentException.class, () -> tumbling(tagging, "1", "2", "3", "12", "13"));
        assertThrows(IllegalArgumentException.class, () -> sliding(tagging, "1", "2", "7", "12", "13"));
        assertThrows(IllegalArgumentException.class, () -> bounded(tagging, "1", "2", "3", "12", "13"));
    }

    @Test
    public void testC106_timeShiftingOrNullWrapper_failsFast() {
        final Stream.WindowHandler<String, List<String>> shifting = handlerWithWrapper((v, t) -> com.landawn.abacus.util.Timed.of(v, t + 100));

        // used to end the stream silently: []
        assertThrows(IllegalArgumentException.class, () -> tumbling(shifting, "1", "2", "3", "12", "13"));
        assertThrows(IllegalArgumentException.class, () -> sliding(shifting, "1", "2", "7", "12", "13"));
        assertThrows(IllegalArgumentException.class, () -> bounded(shifting, "1", "2", "3", "12", "13"));

        final Stream.WindowHandler<String, List<String>> nulls = handlerWithWrapper((v, t) -> null);
        // a null Timed from the wrapper is a broken callback postcondition: NPE (a shifted one stays IAE)
        assertThrows(NullPointerException.class, () -> tumbling(nulls, "1", "2"));
        assertThrows(NullPointerException.class, () -> bounded(nulls, "1", "2"));

        // an empty source never calls the wrapper
        assertEquals(List.of(), tumbling(shifting));
    }
}

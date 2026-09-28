package com.landawn.abacus.util.stream;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.management.ManagementFactory;
import java.lang.reflect.Field;
import java.lang.reflect.Array;
import java.util.Iterator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.util.IntIterator;
import com.landawn.abacus.util.Seq;

@Tag("unit")
public class PerformanceStreamRegressionTest {
    private static Object field(final Object object, final String name) throws Exception {
        final Field field = object.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return field.get(object);
    }

    @Test
    public void inheritedCloseStorageAndTraversalScaleLinearly() throws Exception {
        final Field handlers = StreamBase.class.getDeclaredField("closeHandlers");
        handlers.setAccessible(true);
        final IntStream first = IntStream.of(new int[5_001]);
        IntStream current = first;
        for (int i = 0; i < 5_000; i++) {
            current = current.skip(1);
            assertTrue(((Deque<?>) handlers.get(current)).size() <= 2);
        }
        current.close();
        assertTrue(((Deque<?>) handlers.get(current)).isEmpty());
        assertThrows(IllegalStateException.class, first::count);

        final Field seqHandlers = Seq.class.getDeclaredField("closeHandlers");
        seqHandlers.setAccessible(true);
        final Seq<Integer, RuntimeException> firstSeq = Seq.of(new Integer[5_001]);
        Seq<Integer, RuntimeException> seq = firstSeq;
        for (int i = 0; i < 5_000; i++) {
            seq = seq.skip(1);
            assertTrue(((Deque<?>) seqHandlers.get(seq)).size() <= 2);
        }
        seq.close();
        assertThrows(IllegalStateException.class, firstSeq::count);
    }

    @Test
    public void closeOrderLateUpstreamHandlersAndFlatSuppressionArePreserved() {
        final List<String> order = new ArrayList<>();
        final RuntimeException a = new RuntimeException("a"), b = new RuntimeException("b"), c = new RuntimeException("c");
        final Stream<Integer> root = Stream.of(1).onClose(() -> { order.add("a"); throw a; });
        final Stream<Integer> middle = root.map(v -> v).onClose(() -> { order.add("b"); throw b; });
        final Stream<Integer> last = middle.map(v -> v).onClose(() -> { order.add("c"); throw c; });
        assertSame(a, assertThrows(RuntimeException.class, last::close));
        assertArrayEquals(new Throwable[] { b, c }, a.getSuppressed());
        assertEquals(List.of("a", "b", "c"), order);
        last.close(); root.close(); middle.close();
        assertEquals(3, order.size());

        order.clear();
        final Stream<Integer> upstream = Stream.of(1).onClose(() -> order.add("original"));
        final Stream<Integer> derived = upstream.map(v -> v).onClose(() -> order.add("derived"));
        upstream.onClose(() -> order.add("late"));
        derived.close();
        assertEquals(List.of("original", "late", "derived"), order);
    }

    @Test
    public void directUpstreamAndReentrantClosureKeepOnceOnlyOrder() {
        final List<String> order = new ArrayList<>();
        final Stream<Integer> upstream = Stream.of(1);
        upstream.onClose(() -> { order.add("start"); upstream.close(); order.add("end"); });
        upstream.onClose(() -> order.add("second"));
        final Stream<Integer> derived = upstream.map(v -> v).onClose(() -> order.add("derived"));
        derived.close();
        assertEquals(List.of("start", "second", "end", "derived"), order);

        order.clear();
        final Stream<Integer> another = Stream.of(1).onClose(() -> order.add("upstream"));
        final Stream<Integer> child = another.map(v -> v).onClose(() -> order.add("child"));
        another.close();
        child.close();
        assertEquals(List.of("upstream", "child"), order);
    }

    @Test
    public void seqCloseGroupsPreserveFailuresAndLateHandlers() {
        final List<String> order = new ArrayList<>();
        final RuntimeException first = new RuntimeException("first"), second = new RuntimeException("second"), third = new RuntimeException("third");
        final Seq<Integer, RuntimeException> root = Seq.<Integer, RuntimeException> of(1).onClose(() -> { order.add("first"); throw first; });
        final Seq<Integer, RuntimeException> middle = root.map(v -> v).onClose(() -> { order.add("second"); throw second; });
        final Seq<Integer, RuntimeException> end = middle.map(v -> v).onClose(() -> { order.add("third"); throw third; });
        assertSame(first, assertThrows(RuntimeException.class, end::close));
        assertArrayEquals(new Throwable[] { second, third }, first.getSuppressed());
        assertEquals(List.of("first", "second", "third"), order);
        assertThrows(IllegalStateException.class, root::count);
    }

    @Test
    public void cycleDropsTheAccumulationBufferAfterTheFirstPass() throws Exception {
        final IntStream stream = IntStream.range(0, 100).filter(v -> true).cycled(3);
        final IntIteratorEx iterator = stream.iteratorEx();
        for (int i = 0; i <= 100; i++) assertEquals(i % 100, iterator.nextInt());
        final Object underlying = field(iterator, "val$values");
        assertNull(field(underlying, "list"));
        assertEquals(100, ((int[]) field(underlying, "a")).length);
        stream.close();

        final Stream<Integer> objects = Stream.of(Arrays.asList(1, null, 3)).cycled(2);
        final ObjIteratorEx<Integer> objectIterator = objects.iteratorEx();
        assertEquals(Arrays.asList(1, null, 3, 1), Arrays.asList(objectIterator.next(), objectIterator.next(), objectIterator.next(), objectIterator.next()));
        assertNull(field(objectIterator, "list"));
        objects.close();
        assertEquals(List.of(1, 2, 1, 2), Seq.of(1, 2).cycled(2).toList());
        assertEquals(0, IntStream.empty().cycled().count());
        assertEquals(0, IntStream.of(1).cycled(0).count());
    }

    @Test
    public void sortedTopUsesBoundedWindowsAndPreservesSuffixOrder() throws Exception {
        assertArrayEquals(new int[] { 7, 8, 9 }, IntStream.range(0, 10).top(3).toArray());
        assertArrayEquals(new long[] { 7, 8, 9 }, LongStream.range(0, 10).top(3).toArray());
        assertArrayEquals(new short[] { 3, 4 }, ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4).filter(v -> true).sorted().top(2).toArray());
        assertArrayEquals(new float[] { 0.0f, Float.NaN }, FloatStream.of(-0.0f, 0.0f, Float.NaN).filter(v -> true).sorted().top(2).toArray());
        assertArrayEquals(new double[] { 0.0, Double.NaN }, DoubleStream.of(-0.0, 0.0, Double.NaN).filter(v -> true).sorted().top(2).toArray());
        assertArrayEquals(new int[] { 1, 2, 3 }, IntStream.range(1, 4).top(Integer.MAX_VALUE).toArray());
        assertArrayEquals(new int[0], IntStream.range(1, 4).top(0).toArray());
        final Comparator<Integer> comparator = Comparator.nullsLast(Comparator.naturalOrder());
        assertEquals(Arrays.asList(3, null), Stream.of(1, 3, null).sorted(comparator).top(2, comparator).toList());
        assertEquals(Arrays.asList(3, null), Seq.of(1, 3, null).sorted(comparator).top(2, comparator).toList());
        assertEquals(Integer.valueOf(3), Seq.of(1, 3, null).sorted(comparator).kthLargest(2, comparator).get());
        assertTrue(Seq.of(1, 3).sorted().kthLargest(Integer.MAX_VALUE, Comparator.naturalOrder()).isEmpty());
    }

    @Test
    public void sortedTopRetainsCandidatesWhenTheLastPullFailsInEveryChangedFamily() {
        // The failed pull exhausts the source, so later values cannot hide a discarded candidate.
        final FailedFinalPull shorts = new FailedFinalPull();
        try (ShortStream stream = new IteratorShortStream(new com.landawn.abacus.util.ShortIterator() {
            @Override public boolean hasNext() { return shorts.hasNext(); }
            @Override public short nextShort() { return (short) shorts.next(); }
        }, true, null).top(3)) {
            assertRetainedAfterFailure(stream.iteratorEx(), shorts, List.of((short) 0, (short) 1, (short) 2));
        }
        final FailedFinalPull ints = new FailedFinalPull();
        try (IntStream stream = new IteratorIntStream(new IntIterator() {
            @Override public boolean hasNext() { return ints.hasNext(); }
            @Override public int nextInt() { return ints.next(); }
        }, true, null).top(3)) {
            assertRetainedAfterFailure(stream.iteratorEx(), ints, List.of(0, 1, 2));
        }
        final FailedFinalPull longs = new FailedFinalPull();
        try (LongStream stream = new IteratorLongStream(new com.landawn.abacus.util.LongIterator() {
            @Override public boolean hasNext() { return longs.hasNext(); }
            @Override public long nextLong() { return longs.next(); }
        }, true, null).top(3)) {
            assertRetainedAfterFailure(stream.iteratorEx(), longs, List.of(0L, 1L, 2L));
        }
        final FailedFinalPull floats = new FailedFinalPull();
        try (FloatStream stream = new IteratorFloatStream(new com.landawn.abacus.util.FloatIterator() {
            @Override public boolean hasNext() { return floats.hasNext(); }
            @Override public float nextFloat() { return floats.next(); }
        }, true, null).top(3)) {
            assertRetainedAfterFailure(stream.iteratorEx(), floats, List.of(0f, 1f, 2f));
        }
        final FailedFinalPull doubles = new FailedFinalPull();
        try (DoubleStream stream = new IteratorDoubleStream(new com.landawn.abacus.util.DoubleIterator() {
            @Override public boolean hasNext() { return doubles.hasNext(); }
            @Override public double nextDouble() { return doubles.next(); }
        }, true, null).top(3)) {
            assertRetainedAfterFailure(stream.iteratorEx(), doubles, List.of(0d, 1d, 2d));
        }
        final FailedFinalPull objects = new FailedFinalPull();
        final Comparator<Integer> comparator = Comparator.naturalOrder();
        try (Stream<Integer> stream = new IteratorStream<Integer>(new Iterator<>() {
            @Override public boolean hasNext() { return objects.hasNext(); }
            @Override public Integer next() { return objects.next(); }
        }, true, comparator, null).top(3, comparator)) {
            assertRetainedAfterFailure(stream.iteratorEx(), objects, List.of(0, 1, 2));
        }
    }

    private static final class FailedFinalPull {
        private int value;
        private boolean failed;
        private final RuntimeException failure = new RuntimeException("last pull");
        private boolean hasNext() { return !failed; }
        private int next() {
            if (value == 3) { failed = true; throw failure; }
            return value++;
        }
    }

    private static void assertRetainedAfterFailure(final Iterator<?> iterator, final FailedFinalPull source, final List<?> expected) {
        assertSame(source.failure, assertThrows(RuntimeException.class, iterator::hasNext));
        final List<Object> actual = new ArrayList<>();
        iterator.forEachRemaining(actual::add);
        assertEquals(expected, actual);
    }

    @Test
    public void sortedPrimitiveTopDoesNotAllocatePerSourceElement() {
        Assumptions.assumeTrue(ManagementFactory.getThreadMXBean() instanceof com.sun.management.ThreadMXBean);
        final com.sun.management.ThreadMXBean bean = (com.sun.management.ThreadMXBean) ManagementFactory.getThreadMXBean();
        Assumptions.assumeTrue(bean.isThreadAllocatedMemorySupported());
        bean.setThreadAllocatedMemoryEnabled(true);
        for (int i = 0; i < 10; i++) IntStream.range(1000, 101000).top(10).toArray();
        final long thread = Thread.currentThread().threadId();
        final long before = bean.getThreadAllocatedBytes(thread);
        final int[] result = IntStream.range(1000, 1001000).top(10).toArray();
        final long allocated = bean.getThreadAllocatedBytes(thread) - before;
        assertArrayEquals(new int[] { 1000990, 1000991, 1000992, 1000993, 1000994, 1000995, 1000996, 1000997, 1000998, 1000999 }, result);
        assertTrue(allocated < 1_000_000, "allocated " + allocated + " bytes for a ten-element result");
    }

    @Test
    public void stringAdvanceUsesSaturatingRandomAccess() {
        final CharStream stream = CharStream.of("0123456789", 2, 8);
        final CharIteratorEx iterator = stream.iteratorEx();
        assertTrue(iterator.supportsFailureAtomicAdvance());
        iterator.advance(-1);
        iterator.advance(0);
        assertEquals('2', iterator.nextChar());
        iterator.advance(3);
        assertEquals('6', iterator.nextChar());
        iterator.advance(Long.MAX_VALUE);
        assertFalse(iterator.hasNext());
        stream.close();
        assertEquals('5', CharStream.of("0123456789").skip(5).first().get());
        assertEquals(0, CharStream.of("0123").skip(Long.MAX_VALUE).count());
    }

    @Test
    public void deferredExtendedIteratorRetainsCloseOwnershipButNotFactory() throws Exception {
        final AtomicInteger factoryCalls = new AtomicInteger(), closes = new AtomicInteger();
        final java.util.function.Supplier<ObjIteratorEx<Integer>> factory = () -> {
            factoryCalls.incrementAndGet();
            return new ObjIteratorEx<>() {
                @Override public boolean hasNext() { return false; }
                @Override public Integer next() { throw new java.util.NoSuchElementException(); }
                @Override public void closeResource() { closes.incrementAndGet(); }
            };
        };
        final ObjIteratorEx<Integer> iterator = ObjIteratorEx.defer(factory);
        iterator.closeResource();
        assertEquals(0, factoryCalls.get());
        assertFalse(iterator.hasNext());
        for (final Field f : iterator.getClass().getDeclaredFields()) {
            f.setAccessible(true);
            final Object value = f.get(iterator);
            assertNotSame(factory, value);
            if (value instanceof com.landawn.abacus.util.Holder<?> holder) assertNull(holder.value());
        }
        iterator.closeResource();
        assertEquals(1, factoryCalls.get());
        assertEquals(1, closes.get());
    }

    @Test
    public void everyPrimitiveCycleHandlesSnapshotsRoundsAndEmptySources() throws Exception {
        final String[] names = { "Byte", "Char", "Short", "Int", "Long", "Float", "Double" };
        final Object[] arrays = { new byte[] { 1, 2 }, new char[] { 'a', 'b' }, new short[] { 1, 2 }, new int[] { 1, 2 },
                new long[] { 1, 2 }, new float[] { -0f, Float.NaN }, new double[] { -0d, Double.NaN } };
        for (int i = 0; i < names.length; i++) {
            final Class<?> iteratorType = Class.forName("com.landawn.abacus.util." + names[i] + "Iterator");
            final Class<?> streamType = Class.forName("com.landawn.abacus.util.stream." + names[i] + "Stream");
            for (final long rounds : new long[] { 0, 1, 3, -1 }) {
                final Object source = iteratorType.getMethod("of", arrays[i].getClass()).invoke(null, arrays[i]);
                final Object upstream = streamType.getMethod("of", iteratorType).invoke(null, source);
                final Object cycle = rounds < 0 ? streamType.getMethod("cycled").invoke(upstream)
                        : streamType.getMethod("cycled", long.class).invoke(upstream, rounds);
                final Object bounded = rounds < 0 ? streamType.getMethod("limit", long.class).invoke(cycle, 5L) : cycle;
                final Object result = streamType.getMethod("toArray").invoke(bounded);
                final int expectedLength = rounds < 0 ? 5 : (int) rounds * 2;
                assertEquals(expectedLength, Array.getLength(result), names[i]);
                for (int j = 0; j < expectedLength; j++) assertEquals(Array.get(arrays[i], j % 2), Array.get(result, j), names[i]);
            }
            final Object empty = iteratorType.getMethod("empty").invoke(null);
            final Object upstream = streamType.getMethod("of", iteratorType).invoke(null, empty);
            final Object cycle = streamType.getMethod("cycled").invoke(upstream);
            assertEquals(0, Array.getLength(streamType.getMethod("toArray").invoke(cycle)), names[i]);
        }
        assertEquals(Arrays.asList(1, null, 1, null, 1), Stream.of(Arrays.asList(1, null).iterator()).cycled().limit(5).toList());
        assertEquals(Arrays.asList(1, null, 1, null, 1), Seq.of(Arrays.asList(1, null)).cycled().limit(5).toList());
    }

    @Test
    public void everySortedPrimitiveWindowHandlesZeroOneWrapAndOversizedLimits() throws Exception {
        final String[] names = { "Short", "Int", "Long", "Float", "Double" };
        final Object[] arrays = { new short[] { 0, 1, 2, 3, 4 }, new int[] { 0, 1, 2, 3, 4 }, new long[] { 0, 1, 2, 3, 4 },
                new float[] { 0, 1, 2, 3, 4 }, new double[] { 0, 1, 2, 3, 4 } };
        for (int i = 0; i < names.length; i++) {
            final Class<?> iteratorType = Class.forName("com.landawn.abacus.util." + names[i] + "Iterator");
            final Class<?> streamType = Class.forName("com.landawn.abacus.util.stream." + names[i] + "Stream");
            for (final int n : new int[] { 0, 1, 2, 3, 5, 6, Integer.MAX_VALUE }) {
                final Object source = iteratorType.getMethod("of", arrays[i].getClass()).invoke(null, arrays[i]);
                final Object upstream = streamType.getMethod("of", iteratorType).invoke(null, source);
                final Object sorted = streamType.getMethod("sorted").invoke(upstream);
                final Object top = streamType.getMethod("top", int.class).invoke(sorted, n);
                final Object result = streamType.getMethod("toArray").invoke(top);
                final int expectedLength = Math.min(n, 5);
                assertEquals(expectedLength, Array.getLength(result), names[i]);
                for (int j = 0; j < expectedLength; j++) assertEquals(Array.get(arrays[i], 5 - expectedLength + j), Array.get(result, j), names[i]);
            }
        }
        final Comparator<Integer> comparator = Comparator.nullsFirst(Comparator.naturalOrder());
        final List<Integer> values = Arrays.asList(null, 1, 2, 3, 4);
        for (final int n : new int[] { 0, 1, 2, 3, 5, 6, Integer.MAX_VALUE }) {
            final List<Integer> expected = values.subList(5 - Math.min(n, 5), 5);
            assertEquals(expected, Stream.of(values.iterator()).sorted(comparator).top(n, comparator).toList());
            assertEquals(expected, Seq.of(values).sorted(comparator).top(n, comparator).toList());
            if (n > 0 && n <= 5) assertEquals(values.get(5 - n), Seq.of(values).sorted(comparator).kthLargest(n, comparator).get());
            if (n > 5) assertTrue(Seq.of(values).sorted(comparator).kthLargest(n, comparator).isEmpty());
        }
    }

    @Test
    public void sequenceCloseGroupsKeepReentrantOrderAndLateHandlerFailures() {
        final List<String> order = new ArrayList<>();
        final Seq<Integer, RuntimeException> parent = Seq.of(1);
        parent.onClose(() -> { order.add("start"); parent.close(); order.add("end"); });
        parent.onClose(() -> order.add("second"));
        final Seq<Integer, RuntimeException> child = parent.map(v -> v).onClose(() -> order.add("child"));
        parent.onClose(() -> order.add("late"));
        child.close();
        assertEquals(List.of("start", "second", "late", "end", "child"), order);
        parent.close(); child.close();
        assertEquals(5, order.size());
    }

    @Test
    public void extendedDeferredIteratorCachesErrorAndReleasesFailingFactory() throws Exception {
        final AtomicInteger calls = new AtomicInteger();
        final AssertionError failure = new AssertionError("factory");
        final java.util.function.Supplier<Iterator<Integer>> factory = () -> { calls.incrementAndGet(); throw failure; };
        final ObjIteratorEx<Integer> iterator = ObjIteratorEx.defer(factory);
        assertSame(failure, assertThrows(AssertionError.class, iterator::hasNext));
        assertSame(failure, assertThrows(AssertionError.class, iterator::next));
        iterator.closeResource();
        assertEquals(1, calls.get());
        for (final Field f : iterator.getClass().getDeclaredFields()) {
            f.setAccessible(true);
            final Object value = f.get(iterator);
            assertNotSame(factory, value);
            if (value instanceof com.landawn.abacus.util.Holder<?> holder) assertNull(holder.value());
        }
    }
}

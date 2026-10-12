package com.landawn.abacus.util.stream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BooleanSupplier;
import java.util.function.Function;
import java.util.stream.Collector;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.Duration;
import com.landawn.abacus.util.IntIterator;
import com.landawn.abacus.util.MergeResult;
import com.landawn.abacus.util.MutableBoolean;
import com.landawn.abacus.util.Pair;

// Extends TestBase and is tagged "slow-test": performance/memory-tuning regressions stay out of
// the regular suite (AbacusCoreTestSuite excludes "slow-test"). Run this class directly when changing that code.
@Tag("slow-test")
class UtilStreamPerformanceFixes20261007Test extends TestBase {
    @Test
    void mergeReleasesConsumedHeadsAndRetriesSelectorFailures() throws Exception {
        final RuntimeException failure = new RuntimeException("selector");
        final AtomicInteger calls = new AtomicInteger();
        final List<String> compared = new ArrayList<>();
        try (Stream<Integer> merged = Stream.merge(Arrays.asList(1, 3).iterator(), Arrays.asList(2, 4).iterator(), (a, b) -> {
            compared.add(a + ":" + b);
            if (calls.getAndIncrement() == 0) {
                throw failure;
            }
            return a < b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND;
        })) {
            final ObjIteratorEx<Integer> iter = merged.iteratorEx();
            assertSame(failure, assertThrows(RuntimeException.class, iter::next));
            for (int expected = 1; expected <= 4; expected++) {
                assertEquals(expected, iter.next());
                assertReleasedHeads(iter);
            }
            assertFalse(iter.hasNext());
            assertEquals(List.of("1:2", "1:2", "3:2", "3:4"), compared);
        }
    }

    private static void assertReleasedHeads(final Object iterator) throws Exception {
        for (final String suffix : List.of("A", "B")) {
            final Field flag = iterator.getClass().getDeclaredField("hasNext" + suffix);
            final Field slot = iterator.getClass().getDeclaredField("next" + suffix);
            flag.setAccessible(true);
            slot.setAccessible(true);
            if (!flag.getBoolean(iterator)) {
                assertNull(slot.get(iterator), "consumed head " + suffix);
            }
        }
    }

    @Test
    void closeClearsOwnedQueuesEvenWhenAnOfferCompletesAfterClose() throws Exception {
        for (final boolean timed : new boolean[] { false, true }) {
            for (final boolean ownsQueue : new boolean[] { true, false }) {
                final GatedQueue<Object> queue = new GatedQueue<>(timed);
                final Object payload = new Object();
                final MutableBoolean hasMore = MutableBoolean.of(true);
                final var iter = Stream.buffered(List.of(payload).iterator(), queue, hasMore, ownsQueue);
                try {
                    assertTrue(queue.entered.await(5, TimeUnit.SECONDS));
                    iter.closeResource();
                } finally {
                    queue.release.countDown();
                }
                await(() -> !hasMore.value());
                assertEquals(ownsQueue ? 0 : 1, queue.size());
                if (!ownsQueue) {
                    assertSame(payload, queue.peek());
                }
                assertFalse(iter.hasNext());
                iter.closeResource();
            }
        }
    }

    @Test
    void producerFailureStillDeliversTheQueuedPrefixIncludingNull() throws Exception {
        final RuntimeException failure = new RuntimeException("producer");
        final MutableBoolean hasMore = MutableBoolean.of(true);
        final ArrayBlockingQueue<Integer> queue = new ArrayBlockingQueue<>(8);
        final Iterator<Integer> source = new Iterator<>() {
            private int cursor;

            public boolean hasNext() {
                if (cursor == 3) {
                    throw failure;
                }
                return true;
            }

            public Integer next() {
                return cursor++ == 0 ? null : cursor;
            }
        };
        final var iter = Stream.buffered(source, queue, hasMore, true);
        try {
            await(() -> !hasMore.value());
            assertEquals(3, queue.size());
            assertNull(iter.next());
            assertEquals(2, iter.next());
            assertEquals(3, iter.next());
            assertSame(failure, assertThrows(RuntimeException.class, iter::hasNext));
        } finally {
            iter.closeResource();
        }
    }

    @Test
    void closeReleasesTheConsumerPrefetchedSlot() throws Exception {
        final ArrayBlockingQueue<Object> queue = new ArrayBlockingQueue<>(4);
        final var iter = Stream.buffered(List.of(new Object()).iterator(), queue, null, true);
        assertTrue(iter.hasNext());
        iter.closeResource();
        final Field next = iter.getClass().getDeclaredField("next");
        next.setAccessible(true);
        assertNull(next.get(iter));
        assertTrue(queue.isEmpty());
    }

    @Test
    void timedOperatorsReleaseTheirReadAheadQueueOnClose() throws Exception {
        // maxWait and the time-based windows read the source ahead into a 64-element queue they create themselves, so
        // they own it: closing them must clear it, even while the closed pipeline is still referenced.
        final Map<String, Function<Stream<Object>, Stream<?>>> operators = new LinkedHashMap<>();
        operators.put("maxWait(Duration, T)", s -> s.maxWait(Duration.ofMinutes(1), "default"));
        operators.put("maxWait(Duration, Supplier)", s -> s.maxWait(Duration.ofMinutes(1), () -> "default"));
        operators.put("window(Duration, Collector)", s -> s.window(Duration.ofMillis(5), Collectors.counting()));
        operators.put("window(Duration, int, Collector)", s -> s.window(Duration.ofMinutes(1), 1, Collectors.counting()));
        operators.put("window(ToLongTriFunction, QuadPredicate, Collector)",
                s -> s.window((first, last, count) -> 60_000L, (first, last, next, count) -> false, Collectors.counting()));
        final List<String> failures = new ArrayList<>();

        for (final Map.Entry<String, Function<Stream<Object>, Stream<?>>> operator : operators.entrySet()) {
            final Queue<WeakReference<Object>> produced = new ConcurrentLinkedQueue<>();
            final Stream<?> stream = operator.getValue().apply(Stream.of(trackedSource(produced)));

            try {
                stream.iteratorEx().next();

                // The consumer stopped, so the reader fills the queue and waits for room.
                int readAhead = 0;

                for (int attempt = 0; attempt < 50 && readAhead < 64; attempt++) {
                    Thread.sleep(20);
                    readAhead = reachableAfterGc(produced, Integer.MAX_VALUE);
                }

                if (readAhead < 64) {
                    failures.add(operator.getKey() + ": only " + readAhead + " elements were read ahead");
                }
            } finally {
                stream.close();
            }

            // The operator may still hold the element it looked at last; the queued ones must be released.
            final int retained = reachableAfterGc(produced, 2);

            if (retained > 2) {
                failures.add(operator.getKey() + ": the closed pipeline still holds " + retained + " elements");
            }

            Reference.reachabilityFence(stream);
        }

        assertEquals(List.of(), failures);
    }

    /**
     * An endless source whose elements are tracked through weak references only.
     */
    private static Iterator<Object> trackedSource(final Queue<WeakReference<Object>> produced) {
        return new Iterator<>() {
            @Override
            public boolean hasNext() {
                return true;
            }

            @Override
            public Object next() {
                final Object element = new Object();
                produced.add(new WeakReference<>(element));
                return element;
            }
        };
    }

    /**
     * Collects garbage until at most {@code maxReachable} tracked elements remain reachable, for up to two seconds,
     * and returns how many remain.
     */
    private static int reachableAfterGc(final Queue<WeakReference<Object>> produced, final int maxReachable) throws InterruptedException {
        int reachable = Integer.MAX_VALUE;

        for (int round = 0; round < 100 && reachable > maxReachable; round++) {
            if (round > 0) {
                Thread.sleep(20);
            }

            System.gc();
            reachable = 0;

            for (final WeakReference<Object> reference : produced) {
                if (!reference.refersTo(null)) {
                    reachable++;
                }
            }
        }

        return reachable;
    }

    @Test
    void zeroCommonAffixesReleaseSourcesAfterPreservingCharSequenceCalls() {
        for (final boolean prefix : new boolean[] { true, false }) {
            final Collector<CharSequence, Pair<CharSequence, Integer>, String> collector = affixCollector(prefix);
            final Pair<CharSequence, Integer> state = collector.supplier().get();
            final CountingSequence left = new CountingSequence("a", 0, null);
            final CountingSequence right = new CountingSequence("b", 0, null);
            collector.accumulator().accept(state, left);
            collector.accumulator().accept(state, right);
            assertEquals(prefix ? 3 : 4, left.lengthCalls);
            assertEquals(prefix ? 2 : 3, right.lengthCalls);
            assertEquals(0, state.right());
            assertNull(state.left());
            assertEquals("", collector.finisher().apply(state));
            final CountingSequence untouched = new CountingSequence("ignored", 1, new RuntimeException());
            collector.accumulator().accept(state, untouched);
            assertEquals(0, untouched.lengthCalls);

            final Pair<CharSequence, Integer> other = collector.supplier().get();
            collector.accumulator().accept(other, "anything");
            assertEquals("", collector.finisher().apply(collector.combiner().apply(other, state)));
            assertNull(other.left());
        }
    }

    @Test
    void zeroAffixStillPropagatesTheFinalLengthCallFailure() {
        for (final boolean prefix : new boolean[] { true, false }) {
            final RuntimeException failure = new RuntimeException("length");
            final Collector<CharSequence, Pair<CharSequence, Integer>, String> collector = affixCollector(prefix);
            final Pair<CharSequence, Integer> state = collector.supplier().get();
            collector.accumulator().accept(state, new CountingSequence("a", 0, null));
            final CountingSequence right = new CountingSequence("b", prefix ? 2 : 3, failure);
            assertSame(failure, assertThrows(RuntimeException.class, () -> collector.accumulator().accept(state, right)));
        }
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private static Collector<CharSequence, Pair<CharSequence, Integer>, String> affixCollector(final boolean prefix) {
        return (Collector) (prefix ? Collectors.commonPrefix() : Collectors.commonSuffix());
    }

    @Test
    void allSixSequentialMapMultiPathsGrowDrainAndPreserveOrder() {
        final int[] expected = new int[67];
        Arrays.fill(expected, 0, 65, 1);
        Arrays.fill(expected, 65, 67, 2);
        assertArrayEquals(expected, IntStream.of(0, 1, 2).mapMulti((v, out) -> {
            for (int i = 0; i < (v == 1 ? 65 : v); i++) {
                out.accept(v);
            }
        }).toArray());
        assertArrayEquals(Arrays.stream(expected).asLongStream().toArray(), LongStream.of(0, 1, 2).mapMulti((v, out) -> {
            for (int i = 0; i < (v == 1 ? 65 : v); i++) {
                out.accept(v);
            }
        }).toArray());
        assertArrayEquals(Arrays.stream(expected).asDoubleStream().toArray(), DoubleStream.of(0, 1, 2).mapMulti((v, out) -> {
            for (int i = 0; i < (v == 1 ? 65 : v); i++) {
                out.accept(v);
            }
        }).toArray());
        assertArrayEquals(expected, Stream.of(new Integer[] { null, 1, 2 }).mapMultiToInt((v, out) -> {
            if (v != null) {
                for (int i = 0; i < (v == 1 ? 65 : v); i++) {
                    out.accept(v);
                }
            }
        }).toArray());
        assertArrayEquals(Arrays.stream(expected).asLongStream().toArray(), Stream.of(new Integer[] { null, 1, 2 }).mapMultiToLong((v, out) -> {
            if (v != null) {
                for (int i = 0; i < (v == 1 ? 65 : v); i++) {
                    out.accept(v);
                }
            }
        }).toArray());
        assertArrayEquals(Arrays.stream(expected).asDoubleStream().toArray(), Stream.of(new Integer[] { null, 1, 2 }).mapMultiToDouble((v, out) -> {
            if (v != null) {
                for (int i = 0; i < (v == 1 ? 65 : v); i++) {
                    out.accept(v);
                }
            }
        }).toArray());
    }

    @Test
    void mapMultiIsLazyCompletesOneMapperBeforeLimitAndClosesOnce() {
        final AtomicInteger calls = new AtomicInteger();
        final AtomicInteger emitted = new AtomicInteger();
        final AtomicInteger closes = new AtomicInteger();
        final IntStream result = IntStream.of(1, 2).onClose(closes::incrementAndGet).mapMulti((v, out) -> {
            calls.incrementAndGet();
            for (int i = 0; i < 70_000; i++) {
                out.accept(v + i);
                emitted.incrementAndGet();
            }
        });
        assertEquals(0, calls.get());
        assertArrayEquals(new int[] { 1 }, result.limit(1).toArray());
        assertEquals(1, calls.get());
        assertEquals(70_000, emitted.get());
        assertEquals(1, closes.get());
        result.close();
        assertEquals(1, closes.get());
    }

    @Test
    void failedMappersNeverPublishTheirPartialOutputWhenIterationRetries() {
        final RuntimeException failure = new RuntimeException("mapper");
        try (IntStream s = IntStream.of(1, 2).mapMulti((v, out) -> {
            out.accept(v);
            if (v == 1) {
                throw failure;
            }
        })) {
            assertFailedThenTwo(s.iterator(), failure);
        }
        try (LongStream s = LongStream.of(1, 2).mapMulti((v, out) -> {
            out.accept(v);
            if (v == 1) {
                throw failure;
            }
        })) {
            final var it = s.iterator();
            assertSame(failure, assertThrows(RuntimeException.class, it::hasNext));
            assertEquals(2, it.nextLong());
            assertFalse(it.hasNext());
        }
        try (DoubleStream s = DoubleStream.of(1, 2).mapMulti((v, out) -> {
            out.accept(v);
            if (v == 1) {
                throw failure;
            }
        })) {
            final var it = s.iterator();
            assertSame(failure, assertThrows(RuntimeException.class, it::hasNext));
            assertEquals(2, it.nextDouble());
            assertFalse(it.hasNext());
        }
        try (IntStream s = Stream.of(1, 2).mapMultiToInt((v, out) -> {
            out.accept(v);
            if (v == 1) {
                throw failure;
            }
        })) {
            assertFailedThenTwo(s.iterator(), failure);
        }
        try (LongStream s = Stream.of(1, 2).mapMultiToLong((v, out) -> {
            out.accept(v);
            if (v == 1) {
                throw failure;
            }
        })) {
            final var it = s.iterator();
            assertSame(failure, assertThrows(RuntimeException.class, it::hasNext));
            assertEquals(2, it.nextLong());
            assertFalse(it.hasNext());
        }
        try (DoubleStream s = Stream.of(1, 2).mapMultiToDouble((v, out) -> {
            out.accept(v);
            if (v == 1) {
                throw failure;
            }
        })) {
            final var it = s.iterator();
            assertSame(failure, assertThrows(RuntimeException.class, it::hasNext));
            assertEquals(2, it.nextDouble());
            assertFalse(it.hasNext());
        }
    }

    private static void assertFailedThenTwo(final IntIterator it, final RuntimeException failure) {
        assertSame(failure, assertThrows(RuntimeException.class, it::hasNext));
        assertTrue(it.hasNext());
        assertTrue(it.hasNext());
        assertEquals(2, it.nextInt());
        assertFalse(it.hasNext());
    }

    @Test
    void objectMapMultiDiscardsAFailedMappersPartialOutputLikeThePrimitivePaths() {
        final RuntimeException failure = new RuntimeException("mapper");
        try (Stream<Integer> s = Stream.of(1, 2).<Integer> mapMulti((v, out) -> {
            out.accept(v * 10);
            if (v == 1) {
                throw failure;
            }
            out.accept(v * 100);
        })) {
            final Iterator<Integer> it = s.iterator();
            assertSame(failure, assertThrows(RuntimeException.class, it::hasNext));
            assertTrue(it.hasNext());
            assertEquals(20, it.next());
            assertEquals(200, it.next());
            assertFalse(it.hasNext());
        }
        final IOException checked = new IOException("sneaky mapper");
        try (Stream<Integer> s = Stream.of(1, 2).<Integer> mapMulti((v, out) -> {
            out.accept(v);
            if (v == 1) {
                UtilStreamPerformanceFixes20261007Test.<RuntimeException> throwUnchecked(checked);
            }
        })) {
            final Iterator<Integer> it = s.iterator();
            assertSame(checked, assertThrows(IOException.class, it::hasNext));
            assertEquals(2, it.next());
            assertFalse(it.hasNext());
        }
        // A null element is still a value, and a large expansion followed by small ones keeps exact contents.
        final List<Integer> expected = new ArrayList<>();
        for (int i = 0; i < 20_000; i++) {
            expected.add(i);
        }
        expected.add(null);
        expected.add(7);
        assertEquals(expected, Stream.of(0, 1, 2).<Integer> mapMulti((v, out) -> {
            if (v == 0) {
                for (int i = 0; i < 20_000; i++) {
                    out.accept(i);
                }
            } else if (v == 1) {
                out.accept(null);
            } else {
                out.accept(7);
            }
        }).toList());
    }

    @Test
    void objectMapMultiReleasesItsUndeliveredElementsOnClose() throws Exception {
        // One input element expands to three; after the first is read, closing the stream must drop the other two even
        // though the closed stream itself is still referenced.
        final Queue<WeakReference<Object>> emitted = new ConcurrentLinkedQueue<>();
        final Stream<Object> stream = Stream.of(1).mapMulti((v, out) -> {
            for (int i = 0; i < 3; i++) {
                final Object element = new Object();
                emitted.add(new WeakReference<>(element));
                out.accept(element);
            }
        });

        assertNotNull(stream.iteratorEx().next());
        assertEquals(2, reachableAfterGc(emitted, 2), "the two undelivered elements wait in the buffer");
        stream.close();
        assertEquals(0, reachableAfterGc(emitted, 0), "closing released the buffer");
        Reference.reachabilityFence(stream);
    }

    @Test
    void reusedPrimitiveBuffersExposeExactlyTheCurrentElementsValues() {
        // A large expansion (beyond MAX_BUFFERED_SIZE) is released; smaller ones reuse the array without clearing it,
        // so a later, shorter emission must never expose values left over from an earlier, longer one.
        final int[] counts = { 20_000, 5, 1, 0, 3, 12_000, 2 };
        final List<Integer> expected = new ArrayList<>();
        for (int v = 0; v < counts.length; v++) {
            for (int i = 0; i < counts[v]; i++) {
                expected.add(v * 100_000 + i);
            }
        }
        final int[] ints = IntStream.range(0, counts.length).mapMulti((v, out) -> {
            for (int i = 0; i < counts[v]; i++) {
                out.accept(v * 100_000 + i);
            }
        }).toArray();
        assertEquals(expected, Arrays.stream(ints).boxed().toList());
        final long[] longs = Stream.of(0, 1, 2, 3, 4, 5, 6).mapMultiToLong((v, out) -> {
            for (int i = 0; i < counts[v]; i++) {
                out.accept(v * 100_000L + i);
            }
        }).toArray();
        assertEquals(expected, Arrays.stream(longs).mapToObj(l -> (int) l).toList());
        final double[] doubles = DoubleStream.of(0, 1, 2, 3, 4, 5, 6).mapMulti((v, out) -> {
            for (int i = 0; i < counts[(int) v]; i++) {
                out.accept(v * 100_000 + i);
            }
        }).toArray();
        assertEquals(expected, Arrays.stream(doubles).mapToObj(d -> (int) d).toList());
    }

    @Test
    void sneakyCheckedMapperFailureAlsoDiscardsPartialOutput() {
        final IOException failure = new IOException("sneaky mapper");
        try (IntStream s = Stream.of(1, 2).mapMultiToInt((v, out) -> {
            out.accept(v);
            if (v == 1) {
                UtilStreamPerformanceFixes20261007Test.<RuntimeException> throwUnchecked(failure);
            }
        })) {
            final var it = s.iterator();
            assertSame(failure, assertThrows(IOException.class, it::hasNext));
            assertEquals(2, it.nextInt());
            assertFalse(it.hasNext());
        }
        try (DoubleStream s = DoubleStream.of(1, 2).mapMulti((v, out) -> {
            out.accept(v);
            if (v == 1) {
                UtilStreamPerformanceFixes20261007Test.<RuntimeException> throwUnchecked(failure);
            }
        })) {
            final var it = s.iterator();
            assertSame(failure, assertThrows(IOException.class, it::hasNext));
            assertEquals(2, it.nextDouble());
            assertFalse(it.hasNext());
        }
    }

    @SuppressWarnings("unchecked")
    private static <E extends Throwable> void throwUnchecked(final Throwable failure) throws E {
        throw (E) failure;
    }

    @Test
    void parallelMappersKeepIndependentConsumers() {
        final Set<Object> consumers = ConcurrentHashMap.newKeySet();
        final int[] actual = IntStream.range(0, 100).parallel(4).mapMulti((v, out) -> {
            assertTrue(consumers.add(out));
            out.accept(v);
            out.accept(v);
        }).toArray();
        Arrays.sort(actual);
        assertEquals(200, actual.length);
        for (int i = 0; i < actual.length; i++) {
            assertEquals(i / 2, actual[i]);
        }
        consumers.clear();
        assertEquals(100, Stream.of(Arrays.asList(new Integer[100])).parallel(4).mapMultiToLong((v, out) -> {
            assertTrue(consumers.add(out));
            out.accept(1);
        }).count());
    }

    @Test
    void doubleOutputsPreserveRawBitsAndEmptyInputsValidateMapper() {
        final double unusualNaN = Double.longBitsToDouble(0x7ff800000000002aL);
        for (final double[] actual : List.of(DoubleStream.of(1).mapMulti((v, out) -> {
            out.accept(-0.0);
            out.accept(unusualNaN);
        }).toArray(), Stream.of(1).mapMultiToDouble((v, out) -> {
            out.accept(-0.0);
            out.accept(unusualNaN);
        }).toArray())) {
            assertEquals(Double.doubleToRawLongBits(-0.0), Double.doubleToRawLongBits(actual[0]));
            assertEquals(Double.doubleToRawLongBits(unusualNaN), Double.doubleToRawLongBits(actual[1]));
        }
        assertEquals(0, IntStream.empty().mapMulti((v, out) -> fail("empty mapper")).count());
        assertEquals(0, Stream.empty().mapMultiToDouble((v, out) -> fail("empty mapper")).count());
        assertThrows(IllegalArgumentException.class, () -> IntStream.empty().mapMulti(null));
        assertThrows(IllegalArgumentException.class, () -> Stream.empty().mapMultiToDouble(null));
    }

    @Test
    void mapperFailureKeepsIdentityAndSuppressesCloseFailure() {
        final RuntimeException failure = new RuntimeException("mapper");
        final RuntimeException closeFailure = new RuntimeException("close");
        final AtomicInteger closes = new AtomicInteger();
        final RuntimeException actual = assertThrows(RuntimeException.class, () -> Stream.of(1).onClose(() -> {
            closes.incrementAndGet();
            throw closeFailure;
        }).mapMultiToInt((v, out) -> {
            out.accept(v);
            throw failure;
        }).toArray());
        assertSame(failure, actual);
        assertArrayEquals(new Throwable[] { closeFailure }, actual.getSuppressed());
        assertEquals(1, closes.get());
    }

    private static void await(final BooleanSupplier condition) throws InterruptedException {
        final long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
        while (!condition.getAsBoolean() && System.nanoTime() < deadline) {
            Thread.sleep(1);
        }
        assertTrue(condition.getAsBoolean(), "producer did not finish");
    }

    private static final class GatedQueue<T> extends ArrayBlockingQueue<T> {
        final CountDownLatch entered = new CountDownLatch(1);
        final CountDownLatch release = new CountDownLatch(1);
        private final boolean timed;

        GatedQueue(final boolean timed) {
            super(4);
            this.timed = timed;
        }

        @Override
        public boolean offer(final T value) {
            if (timed) {
                return false;
            }
            gate();
            return super.offer(value);
        }

        @Override
        public boolean offer(final T value, final long timeout, final TimeUnit unit) throws InterruptedException {
            gate();
            return super.offer(value, timeout, unit);
        }

        private void gate() {
            entered.countDown();
            try {
                if (!release.await(5, TimeUnit.SECONDS)) {
                    throw new AssertionError("offer was not released");
                }
            } catch (final InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new AssertionError(e);
            }
        }
    }

    private static final class CountingSequence implements CharSequence {
        private final String value;
        private final int throwOnLengthCall;
        private final RuntimeException failure;
        private int lengthCalls;

        CountingSequence(final String value, final int throwOnLengthCall, final RuntimeException failure) {
            this.value = value;
            this.throwOnLengthCall = throwOnLengthCall;
            this.failure = failure;
        }

        @Override
        public int length() {
            if (++lengthCalls == throwOnLengthCall) {
                throw failure;
            }
            return value.length();
        }

        @Override
        public char charAt(final int index) {
            return value.charAt(index);
        }

        @Override
        public CharSequence subSequence(final int start, final int end) {
            return value.subSequence(start, end);
        }
    }
}

package com.landawn.abacus.util.stream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

/** Stream performance and memory regression contracts. */
@Tag("slow-test")
public class StreamPerformanceMemoryTest extends TestBase {

    // ---- perf review 2026-10-08 G085 end ----
    // ---- perf review 2026-10-08 G086 begin ----
    @Nested
    class G086 {

        /**
         * A value with a sort key and an identity, so that a test can see which of several equal-key elements a
         * percentile picks (the sorts are stable).
         */
        private record Item(int key, int id) implements Comparable<Item> {
            @Override
            public int compareTo(final Item other) {
                return Integer.compare(key, other.key);
            }
        }

        private final java.util.Comparator<Item> byKey = java.util.Comparator.comparingInt(Item::key);

        private Item[] items(final int... keys) {
            final Item[] a = new Item[keys.length];

            for (int i = 0; i < keys.length; i++) {
                a[i] = new Item(keys[i], i);
            }

            return a;
        }

        private Item[] shapedItems(final int n, final int shape, final Random rnd) {
            final Item[] a = new Item[n];

            for (int i = 0; i < n; i++) {
                final int key = switch (shape) {
                    case 0 -> rnd.nextInt(Math.max(1, n / 3)); // random with duplicates
                    case 1 -> i; // ascending
                    case 2 -> n - i; // descending
                    default -> 5; // all equal
                };

                a[i] = new Item(key, i);
            }

            return a;
        }

        /**
         * The expected result: a stable sort of a copy, then N.percentilesOfSorted.
         */
        private <T> Map<com.landawn.abacus.util.Percentage, T> oracle(final T[] a, final java.util.Comparator<? super T> cmp) {
            final T[] copy = a.clone();
            Arrays.sort(copy, cmp);
            return com.landawn.abacus.util.N.percentilesOfSorted(copy);
        }

        private <T> void assertSameValues(final Map<com.landawn.abacus.util.Percentage, T> expected,
                final com.landawn.abacus.util.u.Optional<Map<com.landawn.abacus.util.Percentage, T>> actual, final String context) {
            assertTrue(actual.isPresent(), context);
            assertEquals(new ArrayList<>(expected.keySet()), new ArrayList<>(actual.get().keySet()), context);

            for (final Map.Entry<com.landawn.abacus.util.Percentage, T> e : expected.entrySet()) {
                assertSame(e.getValue(), actual.get().get(e.getKey()), context + " " + e.getKey());
            }
        }

        // G086-01: percentiles()/percentiles(Comparator) sort a private snapshot in place instead of re-copying sorted() output.
        @Test
        public void testPercentiles_matchesStableSortOracleForEverySourceShape() {
            final Random rnd = new Random(20261008L);
            final java.util.Comparator<Item> natural = com.landawn.abacus.util.Comparators.naturalOrder();
            final java.util.Comparator<Item> reversedByKey = byKey.reversed();

            for (final int n : new int[] { 1, 2, 3, 7, 10, 100, 1000 }) {
                for (int shape = 0; shape < 4; shape++) {
                    final Item[] a = shapedItems(n, shape, rnd);
                    final Item[] padded = new Item[n + 4];
                    padded[0] = new Item(-100, -1);
                    padded[1] = new Item(1000, -2);
                    System.arraycopy(a, 0, padded, 2, n);
                    padded[n + 2] = new Item(-100, -3);
                    padded[n + 3] = new Item(1000, -4);

                    final String context = "n=" + n + " shape=" + shape;
                    final Map<com.landawn.abacus.util.Percentage, Item> expected = oracle(a, natural);
                    final Map<com.landawn.abacus.util.Percentage, Item> expectedReversed = oracle(a, reversedByKey);

                    assertSameValues(expected, Stream.of(a).percentiles(), context + " array");
                    assertSameValues(expected, Stream.of(padded, 2, n + 2).percentiles(), context + " array range");
                    assertSameValues(expected, Stream.of(Arrays.asList(a).iterator()).percentiles(), context + " iterator");
                    assertSameValues(expected, Stream.of(a).map(x -> x).percentiles(), context + " mapped");
                    assertSameValues(expected, Stream.of(a).parallel(3).percentiles(), context + " parallel array");
                    assertSameValues(expected, Stream.of(Arrays.asList(a).iterator()).parallel(3).percentiles(), context + " parallel iterator");
                    assertSameValues(expected, Stream.of(a).sorted().percentiles(), context + " already sorted");
                    assertSameValues(expected, Stream.of(a).sorted(reversedByKey).percentiles(), context + " sorted by another comparator");

                    assertSameValues(expected, Stream.of(a).percentiles(byKey), context + " cmp array");
                    assertSameValues(expected, Stream.of(Arrays.asList(a).iterator()).percentiles(byKey), context + " cmp iterator");
                    assertSameValues(expected, Stream.of(a).parallel(3).percentiles(byKey), context + " cmp parallel");
                    assertSameValues(expected, Stream.of(a).sorted(byKey).percentiles(byKey), context + " cmp already sorted");
                    assertSameValues(expected, Stream.of(a).sorted(reversedByKey).percentiles(byKey), context + " cmp sorted by another comparator");
                    assertSameValues(expectedReversed, Stream.of(a).percentiles(reversedByKey), context + " cmp reversed");
                    assertSameValues(expectedReversed, Stream.of(a).sorted().percentiles(reversedByKey), context + " cmp reversed of sorted");
                }
            }

            // Natural order puts null first; equal Integers are compared by value.
            final Integer[] withNulls = { 7, null, 3, 3, null, 9, 1 };
            assertEquals(oracle(withNulls, com.landawn.abacus.util.Comparators.naturalOrder()), Stream.of(withNulls).percentiles().get());
            assertEquals(oracle(withNulls, java.util.Comparator.nullsLast(java.util.Comparator.<Integer> reverseOrder())),
                    Stream.of(Arrays.asList(withNulls).iterator())
                            .percentiles(java.util.Comparator.nullsLast(java.util.Comparator.<Integer> reverseOrder()))
                            .get());
        }

        // G086-01: the source is snapshotted and closed before the first comparison, exactly once, for every source kind.
        @Test
        public void testPercentiles_closesTheSourceOnceBeforeTheFirstComparison() {
            for (int source = 0; source < 5; source++) {
                final List<String> log = new ArrayList<>();
                final java.util.Comparator<Item> logging = (x, y) -> {
                    log.add("compare");
                    return byKey.compare(x, y);
                };

                final Item[] a = items(5, 3, 8, 1, 3);
                final Stream<Item> s = switch (source) {
                    case 0 -> Stream.of(a);
                    case 1 -> Stream.of(Arrays.asList(a).iterator());
                    case 2 -> Stream.of(a).parallel(2);
                    case 3 -> Stream.of(Arrays.asList(a).iterator()).parallel(2);
                    default -> Stream.of(a).map(x -> x);
                };

                final Map<com.landawn.abacus.util.Percentage, Item> result = s.onClose(() -> log.add("close")).percentiles(logging).get();

                assertEquals("close", log.get(0), "source " + source);
                assertEquals(1, Collections.frequency(log, "close"), "source " + source);
                assertTrue(log.size() > 1, "source " + source);
                assertSameValues(oracle(a, byKey), com.landawn.abacus.util.u.Optional.of(result), "source " + source);
            }

            // Already sorted by the same comparator: the sorted stream's own (upstream) sort is the only one.
            final AtomicInteger compares = new AtomicInteger();
            final java.util.Comparator<Item> counting = (x, y) -> {
                compares.incrementAndGet();
                return byKey.compare(x, y);
            };
            final Item[] a = shapedItems(200, 0, new Random(7));

            Stream.of(a).sorted(counting).toArray();
            final int sortOnly = compares.getAndSet(0);
            final AtomicInteger closes = new AtomicInteger();
            final Map<com.landawn.abacus.util.Percentage, Item> result = Stream.of(a)
                    .sorted(counting)
                    .onClose(closes::incrementAndGet)
                    .percentiles(counting)
                    .get();

            assertEquals(sortOnly, compares.get());
            assertEquals(1, closes.get());
            assertSameValues(oracle(a, byKey), com.landawn.abacus.util.u.Optional.of(result), "already sorted");
        }

        // G086-01: failure paths keep the primary failure, run close handlers once and never sort after a failed snapshot.
        @Test
        public void testPercentiles_failuresKeepTheirOrderAndCloseOnce() {
            // A failing source wins over a failing close handler, which is suppressed once.
            for (int parallel = 0; parallel < 2; parallel++) {
                final RuntimeException sourceFailure = new IllegalStateException("source");
                final RuntimeException closeFailure = new IllegalArgumentException("close");
                final AtomicInteger closes = new AtomicInteger();
                final AtomicInteger compares = new AtomicInteger();
                final Iterator<Item> failing = new Iterator<>() {
                    private int i = 0;

                    @Override
                    public boolean hasNext() {
                        return true;
                    }

                    @Override
                    public Item next() {
                        if (++i == 3) {
                            throw sourceFailure;
                        }

                        return new Item(i, i);
                    }
                };

                final Stream<Item> s = parallel == 0 ? Stream.of(failing) : Stream.of(failing).parallel(2);
                final RuntimeException thrown = assertThrows(RuntimeException.class, () -> s.onClose(() -> {
                    closes.incrementAndGet();
                    throw closeFailure;
                }).percentiles((x, y) -> {
                    compares.incrementAndGet();
                    return byKey.compare(x, y);
                }));

                assertSame(sourceFailure, thrown);
                assertEquals(1, closes.get());
                assertEquals(0, compares.get());
                assertEquals(1, thrown.getSuppressed().length);
                assertSame(closeFailure, thrown.getSuppressed()[0]);
            }

            // A comparator failure comes after the close.
            {
                final RuntimeException compareFailure = new UnsupportedOperationException("compare");
                final List<String> log = new ArrayList<>();
                final RuntimeException thrown = assertThrows(RuntimeException.class,
                        () -> Stream.of(items(2, 1, 3)).onClose(() -> log.add("close")).percentiles((x, y) -> {
                            log.add("compare");
                            throw compareFailure;
                        }));

                assertSame(compareFailure, thrown);
                assertEquals(Arrays.asList("close", "compare"), log);
                assertEquals(0, thrown.getSuppressed().length);
            }

            // A close handler failing after a successful snapshot is thrown, and nothing is sorted.
            {
                final RuntimeException closeFailure = new IllegalStateException("close");
                final AtomicInteger compares = new AtomicInteger();
                final RuntimeException thrown = assertThrows(RuntimeException.class, () -> Stream.of(Arrays.asList(items(2, 1, 3)).iterator()).onClose(() -> {
                    throw closeFailure;
                }).percentiles((x, y) -> {
                    compares.incrementAndGet();
                    return byKey.compare(x, y);
                }));

                assertSame(closeFailure, thrown);
                assertEquals(0, compares.get());
            }

            // Elements that are not Comparable fail in the natural-order sort, after the close.
            {
                final AtomicInteger closes = new AtomicInteger();
                assertThrows(ClassCastException.class, () -> Stream.of(new Object(), new Object()).onClose(closes::incrementAndGet).percentiles());
                assertEquals(1, closes.get());
            }
        }

        // G086-01: empty, closed, null comparator, and the caller's array is never reordered.
        @Test
        public void testPercentiles_emptyClosedNullComparatorAndSourceArrayUntouched() {
            final AtomicInteger closes = new AtomicInteger();

            assertFalse(Stream.<Item> empty().onClose(closes::incrementAndGet).percentiles().isPresent());
            assertFalse(Stream.of(new ArrayList<Item>().iterator()).onClose(closes::incrementAndGet).percentiles(byKey).isPresent());
            assertFalse(Stream.of(new Item[0]).parallel(2).onClose(closes::incrementAndGet).percentiles(byKey).isPresent());
            assertEquals(3, closes.get());

            final Stream<Item> closed = Stream.of(items(1, 2));
            closed.close();
            assertThrows(IllegalStateException.class, closed::percentiles);
            assertThrows(IllegalStateException.class, () -> closed.percentiles(byKey));

            // A rejected argument closes the stream, as every argument check of the stream does.
            final Stream<Item> open = Stream.of(items(1, 2)).onClose(closes::incrementAndGet);
            assertThrows(IllegalArgumentException.class, () -> open.percentiles(null));
            assertEquals(4, closes.get());
            open.close();
            assertEquals(4, closes.get());

            final Integer[] source = { 5, 3, 9, 1, 7, 3 };
            assertEquals(Integer.valueOf(3), Stream.of(source).percentiles().get().get(com.landawn.abacus.util.Percentage._20));
            Stream.of(source, 1, 5).percentiles();
            Stream.of(source).parallel(2).percentiles(java.util.Comparator.reverseOrder());
            assertArrayEquals(new Integer[] { 5, 3, 9, 1, 7, 3 }, source);
        }
    }

    // ---- perf review 2026-10-08 G119 end ----
    // ---- perf review 2026-10-08 G121 begin ----
    @Nested
    class G121 {
        private final int size = 1 << 18;
        private WeakReference<Object> payloadRef;

        private boolean awaitCleared(final WeakReference<?> ref) {
            for (int i = 0; i < 50 && ref.get() != null; i++) {
                System.gc();

                try {
                    Thread.sleep(10);
                } catch (final InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }

            return ref.get() == null;
        }

        private long expectedPayloadSum() {
            long expected = 0;

            for (int i = 0; i < size; i++) {
                expected += i % 100;
            }

            return expected;
        }

        private void assertSameElements(final List<?> expected, final List<?> actual) {
            assertEquals(expected.size(), actual.size());

            for (int i = 0; i < expected.size(); i++) {
                assertSame(expected.get(i), actual.get(i));
            }
        }

        // ---- builders: each runs in its own frame so that only the returned stream references the payload ----

        private IntStream intConcatOverPrivateArray() {
            final int[] payload = new int[size];

            for (int i = 0; i < size; i++) {
                payload[i] = i % 100;
            }

            payloadRef = new WeakReference<>(payload);

            return IntStream.concat(Arrays.asList(IntStream.of(payload), null, IntStream.of(-1, -2)));
        }

        private IntStream intSortedThenAppend() {
            final int[] values = new int[size];

            for (int i = 0; i < size; i++) {
                values[i] = 99 - i % 100;
            }

            final IntStream sorted = IntStream.of(values).sorted();
            payloadRef = new WeakReference<>(sorted);

            return sorted.append(IntStream.of(-1, -2));
        }

        private LongStream longConcatOverPrivateArray() {
            final long[] payload = new long[size];

            for (int i = 0; i < size; i++) {
                payload[i] = i % 100;
            }

            payloadRef = new WeakReference<>(payload);

            return LongStream.concat(Arrays.asList(LongStream.of(payload), null, LongStream.of(-1L, -2L)));
        }

        private LongStream longSortedThenAppend() {
            final long[] values = new long[size];

            for (int i = 0; i < size; i++) {
                values[i] = 99 - i % 100;
            }

            final LongStream sorted = LongStream.of(values).sorted();
            payloadRef = new WeakReference<>(sorted);

            return sorted.append(LongStream.of(-1L, -2L));
        }

        private ShortStream shortConcatOverPrivateArray() {
            final short[] payload = new short[size];

            for (int i = 0; i < size; i++) {
                payload[i] = (short) (i % 100);
            }

            payloadRef = new WeakReference<>(payload);

            return ShortStream.concat(Arrays.asList(ShortStream.of(payload), null, ShortStream.of((short) -1, (short) -2)));
        }

        private ShortStream shortSortedThenAppend() {
            final short[] values = new short[size];

            for (int i = 0; i < size; i++) {
                values[i] = (short) (99 - i % 100);
            }

            final ShortStream sorted = ShortStream.of(values).sorted();
            payloadRef = new WeakReference<>(sorted);

            return sorted.append(ShortStream.of((short) -1, (short) -2));
        }

        private Stream<Integer> objConcatOverPrivateArray() {
            final Integer[] payload = new Integer[size];

            for (int i = 0; i < size; i++) {
                payload[i] = i % 100;
            }

            payloadRef = new WeakReference<>(payload);

            return Stream.concat(Arrays.asList(Stream.of(payload), null, Stream.of(-1, -2)));
        }

        private Stream<Integer> objSortedThenAppend() {
            final Integer[] values = new Integer[size];

            for (int i = 0; i < size; i++) {
                values[i] = 99 - i % 100;
            }

            final Stream<Integer> sorted = Stream.of(values).sorted();
            payloadRef = new WeakReference<>(sorted);

            return sorted.append(Stream.of(-1, -2));
        }

        private Stream<Integer> objPrependPrivateArray() {
            final Integer[] payload = new Integer[size];

            for (int i = 0; i < size; i++) {
                payload[i] = i % 100;
            }

            payloadRef = new WeakReference<>(payload);

            return Stream.of(-1, -2).prepend(Stream.of(payload));
        }

        // ---- retention (RED before the fix) ----

        // G121-01: IntStream.concat(Collection) no longer keeps an exhausted (closed) input reachable while later inputs run
        @Test
        void testIntStreamConcat_releasesExhaustedInputWhileLaterInputsRun() {
            final IntStream stream = intConcatOverPrivateArray();
            final boolean[] released = { false };
            final long[] sum = { 0 };
            final List<Integer> tail = new ArrayList<>();

            stream.forEach(x -> {
                if (x < 0) {
                    if (x == -1) {
                        released[0] = awaitCleared(payloadRef);
                    }

                    tail.add(x);
                } else {
                    sum[0] += x;
                }
            });

            assertEquals(expectedPayloadSum(), sum[0]);
            assertEquals(Arrays.asList(-1, -2), tail);
            assertTrue(released[0], "the exhausted first input (and its array) is still reachable from the concatenation");
        }

        // G121-01: IntStream.append delegates to concat(this, other): the exhausted sorted() receiver (and its sorted copy) is released
        @Test
        void testIntStreamAppend_releasesExhaustedSortedReceiverWhileAppendedStreamRuns() {
            final IntStream stream = intSortedThenAppend();
            final boolean[] released = { false };
            final List<Integer> seen = new ArrayList<>();

            stream.forEach(x -> {
                if (x == -1 && seen.size() == 3) {
                    released[0] = awaitCleared(payloadRef);
                }

                if (x < 0 || seen.size() < 3) {
                    seen.add(x);
                }
            });

            assertEquals(Arrays.asList(0, 0, 0, -1, -2), seen);
            assertTrue(released[0], "the exhausted sorted receiver (and its sorted copy) is still reachable from the appended stream");
        }

        // G121-02: LongStream.concat(Collection) no longer keeps an exhausted (closed) input reachable while later inputs run
        @Test
        void testLongStreamConcat_releasesExhaustedInputWhileLaterInputsRun() {
            final LongStream stream = longConcatOverPrivateArray();
            final boolean[] released = { false };
            final long[] sum = { 0 };
            final List<Long> tail = new ArrayList<>();

            stream.forEach(x -> {
                if (x < 0) {
                    if (x == -1) {
                        released[0] = awaitCleared(payloadRef);
                    }

                    tail.add(x);
                } else {
                    sum[0] += x;
                }
            });

            assertEquals(expectedPayloadSum(), sum[0]);
            assertEquals(Arrays.asList(-1L, -2L), tail);
            assertTrue(released[0], "the exhausted first input (and its array) is still reachable from the concatenation");
        }

        // G121-02: LongStream.append delegates to concat(this, other): the exhausted sorted() receiver (and its sorted copy) is released
        @Test
        void testLongStreamAppend_releasesExhaustedSortedReceiverWhileAppendedStreamRuns() {
            final LongStream stream = longSortedThenAppend();
            final boolean[] released = { false };
            final List<Long> seen = new ArrayList<>();

            stream.forEach(x -> {
                if (x == -1 && seen.size() == 3) {
                    released[0] = awaitCleared(payloadRef);
                }

                if (x < 0 || seen.size() < 3) {
                    seen.add(x);
                }
            });

            assertEquals(Arrays.asList(0L, 0L, 0L, -1L, -2L), seen);
            assertTrue(released[0], "the exhausted sorted receiver (and its sorted copy) is still reachable from the appended stream");
        }

        // G121-03: ShortStream.concat(Collection) no longer keeps an exhausted (closed) input reachable while later inputs run
        @Test
        void testShortStreamConcat_releasesExhaustedInputWhileLaterInputsRun() {
            final ShortStream stream = shortConcatOverPrivateArray();
            final boolean[] released = { false };
            final long[] sum = { 0 };
            final List<Short> tail = new ArrayList<>();

            stream.forEach(x -> {
                if (x < 0) {
                    if (x == -1) {
                        released[0] = awaitCleared(payloadRef);
                    }

                    tail.add(x);
                } else {
                    sum[0] += x;
                }
            });

            assertEquals(expectedPayloadSum(), sum[0]);
            assertEquals(Arrays.asList((short) -1, (short) -2), tail);
            assertTrue(released[0], "the exhausted first input (and its array) is still reachable from the concatenation");
        }

        // G121-03: ShortStream.append delegates to concat(this, other): the exhausted sorted() receiver (and its sorted copy) is released
        @Test
        void testShortStreamAppend_releasesExhaustedSortedReceiverWhileAppendedStreamRuns() {
            final ShortStream stream = shortSortedThenAppend();
            final boolean[] released = { false };
            final List<Short> seen = new ArrayList<>();

            stream.forEach(x -> {
                if (x == -1 && seen.size() == 3) {
                    released[0] = awaitCleared(payloadRef);
                }

                if (x < 0 || seen.size() < 3) {
                    seen.add(x);
                }
            });

            assertEquals(Arrays.asList((short) 0, (short) 0, (short) 0, (short) -1, (short) -2), seen);
            assertTrue(released[0], "the exhausted sorted receiver (and its sorted copy) is still reachable from the appended stream");
        }

        // G121-04: Stream.concat(Collection) no longer keeps an exhausted (closed) input reachable while later inputs run
        @Test
        void testStreamConcat_releasesExhaustedInputWhileLaterInputsRun() {
            final Stream<Integer> stream = objConcatOverPrivateArray();
            final boolean[] released = { false };
            final long[] sum = { 0 };
            final List<Integer> tail = new ArrayList<>();

            stream.forEach(x -> {
                if (x < 0) {
                    if (x == -1) {
                        released[0] = awaitCleared(payloadRef);
                    }

                    tail.add(x);
                } else {
                    sum[0] += x;
                }
            });

            assertEquals(expectedPayloadSum(), sum[0]);
            assertEquals(Arrays.asList(-1, -2), tail);
            assertTrue(released[0], "the exhausted first input (and its array) is still reachable from the concatenation");
        }

        // G121-04: Stream.append delegates to concat(this, other): the exhausted sorted() receiver (and its sorted copy) is released
        @Test
        void testStreamAppend_releasesExhaustedSortedReceiverWhileAppendedStreamRuns() {
            final Stream<Integer> stream = objSortedThenAppend();
            final boolean[] released = { false };
            final List<Integer> seen = new ArrayList<>();

            stream.forEach(x -> {
                if (x == -1 && seen.size() == 3) {
                    released[0] = awaitCleared(payloadRef);
                }

                if (x < 0 || seen.size() < 3) {
                    seen.add(x);
                }
            });

            assertEquals(Arrays.asList(0, 0, 0, -1, -2), seen);
            assertTrue(released[0], "the exhausted sorted receiver (and its sorted copy) is still reachable from the appended stream");
        }

        // G121-04: Stream.prepend delegates to concat(other, this): the exhausted prepended stream (and its array) is released
        @Test
        void testStreamPrepend_releasesExhaustedPrependedStreamWhileReceiverRuns() {
            final Stream<Integer> stream = objPrependPrivateArray();
            final boolean[] released = { false };
            final long[] sum = { 0 };
            final List<Integer> tail = new ArrayList<>();

            stream.forEach(x -> {
                if (x < 0) {
                    if (x == -1) {
                        released[0] = awaitCleared(payloadRef);
                    }

                    tail.add(x);
                } else {
                    sum[0] += x;
                }
            });

            assertEquals(expectedPayloadSum(), sum[0]);
            assertEquals(Arrays.asList(-1, -2), tail);
            assertTrue(released[0], "the exhausted prepended stream (and its array) is still reachable from the concatenation");
        }

        // ---- behaviour pins (GREEN before and after the fix) ----

        // G121-01: IntStream.concat - element order, null inputs, close order (each exhausted input before the next one is opened, the
        // rest on close); the caller's collection / varargs array are never modified
        @Test
        void testIntStreamConcat_orderNullInputsAndCloseOrder() {
            final List<String> log = new ArrayList<>();
            final IntStream a = IntStream.of(1, 2).onClose(() -> log.add("closeA"));
            final IntStream b = IntStream.of(3).onClose(() -> log.add("closeB"));
            final IntStream c = IntStream.of(4, 5).onClose(() -> log.add("closeC"));
            final IntStream d = IntStream.of(6).onClose(() -> log.add("closeD"));
            final List<IntStream> input = new ArrayList<>(Arrays.asList(a, null, b, null, c, d));
            final IntStream stream = IntStream.concat(input);
            final IntIteratorEx iter = stream.iteratorEx();
            final List<Integer> seen = new ArrayList<>();

            while (seen.size() < 4 && iter.hasNext()) {
                final int x = iter.nextInt();
                seen.add(x);
                log.add("next" + x);
            }

            stream.close();

            assertEquals(Arrays.asList(1, 2, 3, 4), seen);
            assertEquals(Arrays.asList("next1", "next2", "closeA", "next3", "closeB", "next4", "closeC", "closeD"), log);
            assertSameElements(Arrays.asList(a, null, b, null, c, d), input);
            assertThrows(IllegalStateException.class, a::toArray);
            assertThrows(IllegalStateException.class, d::toArray);

            final IntStream e = IntStream.of(7, 8);
            final IntStream f = IntStream.of(9);
            final IntStream[] array = { e, null, f };
            assertArrayEquals(new int[] { 7, 8, 9 }, IntStream.concat(array).toArray());
            assertSameElements(Arrays.asList(e, null, f), Arrays.asList(array));

            assertEquals(0, IntStream.concat(Arrays.asList((IntStream) null, null)).count());
            assertEquals(0, IntStream.concat(new ArrayList<IntStream>()).count());
            assertArrayEquals(new int[] { 1, 2, 3, 4 }, IntStream.of(1, 2).append(IntStream.of(3, 4)).toArray());
            assertArrayEquals(new int[] { 3, 4, 1, 2 }, IntStream.of(1, 2).prepend(IntStream.of(3, 4)).toArray());
            assertEquals(5, IntStream.concat(IntStream.of(1, 2), IntStream.empty(), IntStream.of(3, 4, 5)).count());
            assertArrayEquals(new int[] { 2, 3 }, IntStream.concat(Arrays.asList(IntStream.of(1, 2), IntStream.of(3, 4))).skip(1).limit(2).toArray());
        }

        // G121-01: IntStream.concat - a failing close of an exhausted input and an already-closed input propagate from hasNext(); a retry
        // continues; the remaining inputs are closed with the concatenation
        @Test
        void testIntStreamConcat_failingCloseAndClosedInputThenRetry() {
            final List<String> log = new ArrayList<>();
            final IntStream a = IntStream.of(1).onClose(() -> {
                log.add("closeA");
                throw new IllegalStateException("closeA failed");
            });
            final IntStream closed = IntStream.of(99);
            closed.close();
            final IntStream c = IntStream.of(3).onClose(() -> log.add("closeC"));
            final IntStream stream = IntStream.concat(Arrays.asList(a, closed, c));
            final IntIteratorEx iter = stream.iteratorEx();

            assertEquals(1, iter.nextInt());
            assertEquals("closeA failed", assertThrows(IllegalStateException.class, iter::hasNext).getMessage());
            assertEquals(Arrays.asList("closeA"), log);
            assertEquals("This stream is already terminated.", assertThrows(IllegalStateException.class, iter::hasNext).getMessage());
            assertEquals(Arrays.asList("closeA"), log);
            assertTrue(iter.hasNext());
            assertEquals(3, iter.nextInt());
            assertFalse(iter.hasNext());
            assertThrows(java.util.NoSuchElementException.class, iter::nextInt);
            stream.close();
            assertEquals(Arrays.asList("closeA", "closeC"), log);

            final IntStream twice = IntStream.of(1, 2);
            final List<Integer> collected = new ArrayList<>();
            assertEquals("This stream is already terminated.",
                    assertThrows(IllegalStateException.class, () -> IntStream.concat(twice, twice).forEach(collected::add)).getMessage());
            assertEquals(Arrays.asList(1, 2), collected);

            final List<String> log2 = new ArrayList<>();
            final IntStream x = IntStream.of(1).onClose(() -> log2.add("closeX"));
            final IntStream closed2 = IntStream.of(2);
            closed2.close();
            final IntStream z = IntStream.of(3).onClose(() -> log2.add("closeZ"));
            assertThrows(IllegalStateException.class, () -> IntStream.concat(x, closed2, z).toArray());
            assertEquals(Arrays.asList("closeX", "closeZ"), log2);

            final List<String> log3 = new ArrayList<>();
            final IntStream p = IntStream.of(1, 2).onClose(() -> log3.add("closeP"));
            final IntStream q = IntStream.of(3).onClose(() -> {
                log3.add("closeQ");
                throw new IllegalStateException("closeQ failed");
            });
            final IntStream r = IntStream.of(4).onClose(() -> log3.add("closeR"));
            final IntStream pqr = IntStream.concat(Arrays.asList(p, q, r));
            assertEquals(1, pqr.iteratorEx().nextInt());
            assertEquals("closeQ failed", assertThrows(IllegalStateException.class, pqr::close).getMessage());
            assertEquals(Arrays.asList("closeP", "closeQ", "closeR"), log3);
        }

        // G121-02: LongStream.concat - element order, null inputs, close order; the caller's collection / varargs array are never modified
        @Test
        void testLongStreamConcat_orderNullInputsAndCloseOrder() {
            final List<String> log = new ArrayList<>();
            final LongStream a = LongStream.of(1L, 2L).onClose(() -> log.add("closeA"));
            final LongStream b = LongStream.of(3L).onClose(() -> log.add("closeB"));
            final LongStream c = LongStream.of(4L, 5L).onClose(() -> log.add("closeC"));
            final LongStream d = LongStream.of(6L).onClose(() -> log.add("closeD"));
            final List<LongStream> input = new ArrayList<>(Arrays.asList(a, null, b, null, c, d));
            final LongStream stream = LongStream.concat(input);
            final LongIteratorEx iter = stream.iteratorEx();
            final List<Long> seen = new ArrayList<>();

            while (seen.size() < 4 && iter.hasNext()) {
                final long x = iter.nextLong();
                seen.add(x);
                log.add("next" + x);
            }

            stream.close();

            assertEquals(Arrays.asList(1L, 2L, 3L, 4L), seen);
            assertEquals(Arrays.asList("next1", "next2", "closeA", "next3", "closeB", "next4", "closeC", "closeD"), log);
            assertSameElements(Arrays.asList(a, null, b, null, c, d), input);
            assertThrows(IllegalStateException.class, a::toArray);
            assertThrows(IllegalStateException.class, d::toArray);

            final LongStream e = LongStream.of(7L, 8L);
            final LongStream f = LongStream.of(9L);
            final LongStream[] array = { e, null, f };
            assertArrayEquals(new long[] { 7, 8, 9 }, LongStream.concat(array).toArray());
            assertSameElements(Arrays.asList(e, null, f), Arrays.asList(array));

            assertEquals(0, LongStream.concat(Arrays.asList((LongStream) null, null)).count());
            assertEquals(0, LongStream.concat(new ArrayList<LongStream>()).count());
            assertArrayEquals(new long[] { 1, 2, 3, 4 }, LongStream.of(1L, 2L).append(LongStream.of(3L, 4L)).toArray());
            assertArrayEquals(new long[] { 3, 4, 1, 2 }, LongStream.of(1L, 2L).prepend(LongStream.of(3L, 4L)).toArray());
            assertEquals(5, LongStream.concat(LongStream.of(1L, 2L), LongStream.empty(), LongStream.of(3L, 4L, 5L)).count());
            assertArrayEquals(new long[] { 2, 3 }, LongStream.concat(Arrays.asList(LongStream.of(1L, 2L), LongStream.of(3L, 4L))).skip(1).limit(2).toArray());
        }

        // G121-02: LongStream.concat - failing close / already-closed input propagate from hasNext(); a retry continues; the rest close
        @Test
        void testLongStreamConcat_failingCloseAndClosedInputThenRetry() {
            final List<String> log = new ArrayList<>();
            final LongStream a = LongStream.of(1L).onClose(() -> {
                log.add("closeA");
                throw new IllegalStateException("closeA failed");
            });
            final LongStream closed = LongStream.of(99L);
            closed.close();
            final LongStream c = LongStream.of(3L).onClose(() -> log.add("closeC"));
            final LongStream stream = LongStream.concat(Arrays.asList(a, closed, c));
            final LongIteratorEx iter = stream.iteratorEx();

            assertEquals(1L, iter.nextLong());
            assertEquals("closeA failed", assertThrows(IllegalStateException.class, iter::hasNext).getMessage());
            assertEquals(Arrays.asList("closeA"), log);
            assertEquals("This stream is already terminated.", assertThrows(IllegalStateException.class, iter::hasNext).getMessage());
            assertEquals(Arrays.asList("closeA"), log);
            assertTrue(iter.hasNext());
            assertEquals(3L, iter.nextLong());
            assertFalse(iter.hasNext());
            assertThrows(java.util.NoSuchElementException.class, iter::nextLong);
            stream.close();
            assertEquals(Arrays.asList("closeA", "closeC"), log);

            final LongStream twice = LongStream.of(1L, 2L);
            final List<Long> collected = new ArrayList<>();
            assertEquals("This stream is already terminated.",
                    assertThrows(IllegalStateException.class, () -> LongStream.concat(twice, twice).forEach(collected::add)).getMessage());
            assertEquals(Arrays.asList(1L, 2L), collected);

            final List<String> log2 = new ArrayList<>();
            final LongStream x = LongStream.of(1L).onClose(() -> log2.add("closeX"));
            final LongStream closed2 = LongStream.of(2L);
            closed2.close();
            final LongStream z = LongStream.of(3L).onClose(() -> log2.add("closeZ"));
            assertThrows(IllegalStateException.class, () -> LongStream.concat(x, closed2, z).toArray());
            assertEquals(Arrays.asList("closeX", "closeZ"), log2);

            final List<String> log3 = new ArrayList<>();
            final LongStream p = LongStream.of(1L, 2L).onClose(() -> log3.add("closeP"));
            final LongStream q = LongStream.of(3L).onClose(() -> {
                log3.add("closeQ");
                throw new IllegalStateException("closeQ failed");
            });
            final LongStream r = LongStream.of(4L).onClose(() -> log3.add("closeR"));
            final LongStream pqr = LongStream.concat(Arrays.asList(p, q, r));
            assertEquals(1L, pqr.iteratorEx().nextLong());
            assertEquals("closeQ failed", assertThrows(IllegalStateException.class, pqr::close).getMessage());
            assertEquals(Arrays.asList("closeP", "closeQ", "closeR"), log3);
        }

        // G121-03: ShortStream.concat - element order, null inputs, close order; the caller's collection / varargs array are never modified
        @Test
        void testShortStreamConcat_orderNullInputsAndCloseOrder() {
            final List<String> log = new ArrayList<>();
            final ShortStream a = ShortStream.of((short) 1, (short) 2).onClose(() -> log.add("closeA"));
            final ShortStream b = ShortStream.of((short) 3).onClose(() -> log.add("closeB"));
            final ShortStream c = ShortStream.of((short) 4, (short) 5).onClose(() -> log.add("closeC"));
            final ShortStream d = ShortStream.of((short) 6).onClose(() -> log.add("closeD"));
            final List<ShortStream> input = new ArrayList<>(Arrays.asList(a, null, b, null, c, d));
            final ShortStream stream = ShortStream.concat(input);
            final ShortIteratorEx iter = stream.iteratorEx();
            final List<Short> seen = new ArrayList<>();

            while (seen.size() < 4 && iter.hasNext()) {
                final short x = iter.nextShort();
                seen.add(x);
                log.add("next" + x);
            }

            stream.close();

            assertEquals(Arrays.asList((short) 1, (short) 2, (short) 3, (short) 4), seen);
            assertEquals(Arrays.asList("next1", "next2", "closeA", "next3", "closeB", "next4", "closeC", "closeD"), log);
            assertSameElements(Arrays.asList(a, null, b, null, c, d), input);
            assertThrows(IllegalStateException.class, a::toArray);
            assertThrows(IllegalStateException.class, d::toArray);

            final ShortStream e = ShortStream.of((short) 7, (short) 8);
            final ShortStream f = ShortStream.of((short) 9);
            final ShortStream[] array = { e, null, f };
            assertArrayEquals(new short[] { 7, 8, 9 }, ShortStream.concat(array).toArray());
            assertSameElements(Arrays.asList(e, null, f), Arrays.asList(array));

            assertEquals(0, ShortStream.concat(Arrays.asList((ShortStream) null, null)).count());
            assertEquals(0, ShortStream.concat(new ArrayList<ShortStream>()).count());
            assertArrayEquals(new short[] { 1, 2, 3, 4 }, ShortStream.of((short) 1, (short) 2).append(ShortStream.of((short) 3, (short) 4)).toArray());
            assertArrayEquals(new short[] { 3, 4, 1, 2 }, ShortStream.of((short) 1, (short) 2).prepend(ShortStream.of((short) 3, (short) 4)).toArray());
            assertEquals(5,
                    ShortStream.concat(ShortStream.of((short) 1, (short) 2), ShortStream.empty(), ShortStream.of((short) 3, (short) 4, (short) 5)).count());
            assertArrayEquals(new short[] { 2, 3 },
                    ShortStream.concat(Arrays.asList(ShortStream.of((short) 1, (short) 2), ShortStream.of((short) 3, (short) 4))).skip(1).limit(2).toArray());
        }

        // G121-03: ShortStream.concat - failing close / already-closed input propagate from hasNext(); a retry continues; the rest close
        @Test
        void testShortStreamConcat_failingCloseAndClosedInputThenRetry() {
            final List<String> log = new ArrayList<>();
            final ShortStream a = ShortStream.of((short) 1).onClose(() -> {
                log.add("closeA");
                throw new IllegalStateException("closeA failed");
            });
            final ShortStream closed = ShortStream.of((short) 99);
            closed.close();
            final ShortStream c = ShortStream.of((short) 3).onClose(() -> log.add("closeC"));
            final ShortStream stream = ShortStream.concat(Arrays.asList(a, closed, c));
            final ShortIteratorEx iter = stream.iteratorEx();

            assertEquals((short) 1, iter.nextShort());
            assertEquals("closeA failed", assertThrows(IllegalStateException.class, iter::hasNext).getMessage());
            assertEquals(Arrays.asList("closeA"), log);
            assertEquals("This stream is already terminated.", assertThrows(IllegalStateException.class, iter::hasNext).getMessage());
            assertEquals(Arrays.asList("closeA"), log);
            assertTrue(iter.hasNext());
            assertEquals((short) 3, iter.nextShort());
            assertFalse(iter.hasNext());
            assertThrows(java.util.NoSuchElementException.class, iter::nextShort);
            stream.close();
            assertEquals(Arrays.asList("closeA", "closeC"), log);

            final ShortStream twice = ShortStream.of((short) 1, (short) 2);
            final List<Short> collected = new ArrayList<>();
            assertEquals("This stream is already terminated.",
                    assertThrows(IllegalStateException.class, () -> ShortStream.concat(twice, twice).forEach(collected::add)).getMessage());
            assertEquals(Arrays.asList((short) 1, (short) 2), collected);

            final List<String> log2 = new ArrayList<>();
            final ShortStream x = ShortStream.of((short) 1).onClose(() -> log2.add("closeX"));
            final ShortStream closed2 = ShortStream.of((short) 2);
            closed2.close();
            final ShortStream z = ShortStream.of((short) 3).onClose(() -> log2.add("closeZ"));
            assertThrows(IllegalStateException.class, () -> ShortStream.concat(x, closed2, z).toArray());
            assertEquals(Arrays.asList("closeX", "closeZ"), log2);

            final List<String> log3 = new ArrayList<>();
            final ShortStream p = ShortStream.of((short) 1, (short) 2).onClose(() -> log3.add("closeP"));
            final ShortStream q = ShortStream.of((short) 3).onClose(() -> {
                log3.add("closeQ");
                throw new IllegalStateException("closeQ failed");
            });
            final ShortStream r = ShortStream.of((short) 4).onClose(() -> log3.add("closeR"));
            final ShortStream pqr = ShortStream.concat(Arrays.asList(p, q, r));
            assertEquals((short) 1, pqr.iteratorEx().nextShort());
            assertEquals("closeQ failed", assertThrows(IllegalStateException.class, pqr::close).getMessage());
            assertEquals(Arrays.asList("closeP", "closeQ", "closeR"), log3);
        }

        // G121-04: Stream.concat - element order (incl. null elements), null inputs, close order; the caller's collection / varargs array are
        // never modified
        @Test
        @SuppressWarnings("unchecked")
        void testStreamConcat_orderNullInputsAndCloseOrder() {
            final List<String> log = new ArrayList<>();
            final Stream<String> a = Stream.of("1", null).onClose(() -> log.add("closeA"));
            final Stream<String> b = Stream.of("3").onClose(() -> log.add("closeB"));
            final Stream<String> c = Stream.of("4", "5").onClose(() -> log.add("closeC"));
            final Stream<String> d = Stream.of("6").onClose(() -> log.add("closeD"));
            final List<Stream<String>> input = new ArrayList<>(Arrays.asList(a, null, b, null, c, d));
            final Stream<String> stream = Stream.concat(input);
            final ObjIteratorEx<String> iter = stream.iteratorEx();
            final List<String> seen = new ArrayList<>();

            while (seen.size() < 4 && iter.hasNext()) {
                final String x = iter.next();
                seen.add(x);
                log.add("next" + x);
            }

            stream.close();

            assertEquals(Arrays.asList("1", null, "3", "4"), seen);
            assertEquals(Arrays.asList("next1", "nextnull", "closeA", "next3", "closeB", "next4", "closeC", "closeD"), log);
            assertSameElements(Arrays.asList(a, null, b, null, c, d), input);
            assertThrows(IllegalStateException.class, a::toList);
            assertThrows(IllegalStateException.class, d::toList);

            final Stream<String> e = Stream.of("7", "8");
            final Stream<String> f = Stream.of("9");
            final Stream<String>[] array = new Stream[] { e, null, f };
            assertEquals(Arrays.asList("7", "8", "9"), Stream.concat(array).toList());
            assertSameElements(Arrays.asList(e, null, f), Arrays.asList(array));

            assertEquals(0, Stream.concat(Arrays.asList((Stream<String>) null, null)).count());
            assertEquals(0, Stream.concat(new ArrayList<Stream<String>>()).count());
            assertEquals(Arrays.asList(1, 2, 3, 4), Stream.of(1, 2).append(Stream.of(3, 4)).toList());
            assertEquals(Arrays.asList(3, 4, 1, 2), Stream.of(1, 2).prepend(Stream.of(3, 4)).toList());
            assertEquals(Arrays.asList(1, 2, 3), Stream.of(1, 2).append(Arrays.asList(3)).toList());
            assertEquals(5, Stream.concat(Stream.of(1, 2), Stream.<Integer> empty(), Stream.of(3, 4, 5)).count());
            assertEquals(Arrays.asList(2, 3), Stream.concat(Arrays.asList(Stream.of(1, 2), Stream.of(3, 4))).skip(1).limit(2).toList());
        }

        // G121-04: Stream.concat - failing close / already-closed input propagate from hasNext(); a retry continues; the rest close
        @Test
        void testStreamConcat_failingCloseAndClosedInputThenRetry() {
            final List<String> log = new ArrayList<>();
            final Stream<String> a = Stream.of("1").onClose(() -> {
                log.add("closeA");
                throw new IllegalStateException("closeA failed");
            });
            final Stream<String> closed = Stream.of("99");
            closed.close();
            final Stream<String> c = Stream.of("3").onClose(() -> log.add("closeC"));
            final Stream<String> stream = Stream.concat(Arrays.asList(a, closed, c));
            final ObjIteratorEx<String> iter = stream.iteratorEx();

            assertEquals("1", iter.next());
            assertEquals("closeA failed", assertThrows(IllegalStateException.class, iter::hasNext).getMessage());
            assertEquals(Arrays.asList("closeA"), log);
            assertEquals("This stream is already terminated.", assertThrows(IllegalStateException.class, iter::hasNext).getMessage());
            assertEquals(Arrays.asList("closeA"), log);
            assertTrue(iter.hasNext());
            assertEquals("3", iter.next());
            assertFalse(iter.hasNext());
            assertThrows(java.util.NoSuchElementException.class, iter::next);
            stream.close();
            assertEquals(Arrays.asList("closeA", "closeC"), log);

            final Stream<String> twice = Stream.of("1", "2");
            final List<String> collected = new ArrayList<>();
            assertEquals("This stream is already terminated.",
                    assertThrows(IllegalStateException.class, () -> Stream.concat(twice, twice).forEach(collected::add)).getMessage());
            assertEquals(Arrays.asList("1", "2"), collected);

            final List<String> log2 = new ArrayList<>();
            final Stream<String> x = Stream.of("1").onClose(() -> log2.add("closeX"));
            final Stream<String> closed2 = Stream.of("2");
            closed2.close();
            final Stream<String> z = Stream.of("3").onClose(() -> log2.add("closeZ"));
            assertThrows(IllegalStateException.class, () -> Stream.concat(x, closed2, z).toList());
            assertEquals(Arrays.asList("closeX", "closeZ"), log2);

            final List<String> log3 = new ArrayList<>();
            final Stream<String> p = Stream.of("1", "2").onClose(() -> log3.add("closeP"));
            final Stream<String> q = Stream.of("3").onClose(() -> {
                log3.add("closeQ");
                throw new IllegalStateException("closeQ failed");
            });
            final Stream<String> r = Stream.of("4").onClose(() -> log3.add("closeR"));
            final Stream<String> pqr = Stream.concat(Arrays.asList(p, q, r));
            assertEquals("1", pqr.iteratorEx().next());
            assertEquals("closeQ failed", assertThrows(IllegalStateException.class, pqr::close).getMessage());
            assertEquals(Arrays.asList("closeP", "closeQ", "closeR"), log3);
        }

        // G121-01..04: parallel concat/append (the concatenation's iterator is shared by the workers under the parallel stream's lock)
        // delivers every element and closes every input
        @Test
        void testConcat_parallelDeliversAllElementsAndClosesInputs() {
            for (int round = 0; round < 10; round++) {
                final AtomicInteger closes = new AtomicInteger();
                final List<IntStream> ints = new ArrayList<>();
                final List<LongStream> longs = new ArrayList<>();
                final List<ShortStream> shorts = new ArrayList<>();
                final List<Stream<Integer>> objs = new ArrayList<>();
                final List<Integer> expected = new ArrayList<>();

                for (int i = 0; i < 50; i++) {
                    final int len = i % 7;
                    final int[] values = new int[len];

                    for (int j = 0; j < len; j++) {
                        values[j] = i + j;
                    }

                    if (i % 10 == 3) {
                        ints.add(null);
                        longs.add(null);
                        shorts.add(null);
                        objs.add(null);
                    } else {
                        final long[] longValues = new long[len];
                        final short[] shortValues = new short[len];
                        final Integer[] boxed = new Integer[len];

                        for (int j = 0; j < len; j++) {
                            longValues[j] = values[j];
                            shortValues[j] = (short) values[j];
                            boxed[j] = values[j];
                            expected.add(values[j] + 1);
                        }

                        ints.add(IntStream.of(values).onClose(closes::incrementAndGet));
                        longs.add(LongStream.of(longValues).onClose(closes::incrementAndGet));
                        shorts.add(ShortStream.of(shortValues).onClose(closes::incrementAndGet));
                        objs.add(Stream.of(boxed).onClose(closes::incrementAndGet));
                    }
                }

                final int[] sortedExpected = new int[expected.size()];

                for (int i = 0; i < sortedExpected.length; i++) {
                    sortedExpected[i] = expected.get(i);
                }

                Arrays.sort(sortedExpected);

                final int[] intResult = IntStream.concat(ints).parallel(4).map(v -> v + 1).toArray();
                Arrays.sort(intResult);
                assertArrayEquals(sortedExpected, intResult);

                final int[] longResult = LongStream.concat(longs).parallel(4).map(v -> v + 1).mapToInt(v -> (int) v).toArray();
                Arrays.sort(longResult);
                assertArrayEquals(sortedExpected, longResult);

                final int[] shortResult = ShortStream.concat(shorts).parallel(4).map(v -> (short) (v + 1)).mapToInt(v -> v).toArray();
                Arrays.sort(shortResult);
                assertArrayEquals(sortedExpected, shortResult);

                final int[] objResult = Stream.concat(objs).parallel(4).map(v -> v + 1).mapToInt(v -> v).toArray();
                Arrays.sort(objResult);
                assertArrayEquals(sortedExpected, objResult);

                assertEquals(4 * 45, closes.get());

                final AtomicInteger closes2 = new AtomicInteger();
                final int[] intAppended = IntStream.of(1, 2, 3)
                        .onClose(closes2::incrementAndGet)
                        .parallel(3)
                        .append(IntStream.of(4, 5).onClose(closes2::incrementAndGet))
                        .toArray();
                Arrays.sort(intAppended);
                assertArrayEquals(new int[] { 1, 2, 3, 4, 5 }, intAppended);

                final long[] longAppended = LongStream.of(1L, 2L, 3L)
                        .onClose(closes2::incrementAndGet)
                        .parallel(3)
                        .append(LongStream.of(4L, 5L).onClose(closes2::incrementAndGet))
                        .toArray();
                Arrays.sort(longAppended);
                assertArrayEquals(new long[] { 1, 2, 3, 4, 5 }, longAppended);

                final short[] shortAppended = ShortStream.of((short) 1, (short) 2, (short) 3)
                        .onClose(closes2::incrementAndGet)
                        .parallel(3)
                        .append(ShortStream.of((short) 4, (short) 5).onClose(closes2::incrementAndGet))
                        .toArray();
                Arrays.sort(shortAppended);
                assertArrayEquals(new short[] { 1, 2, 3, 4, 5 }, shortAppended);

                final List<Integer> objAppended = new ArrayList<>(
                        Stream.of(1, 2, 3).onClose(closes2::incrementAndGet).parallel(3).append(Stream.of(4, 5).onClose(closes2::incrementAndGet)).toList());
                Collections.sort(objAppended);
                assertEquals(Arrays.asList(1, 2, 3, 4, 5), objAppended);

                final List<Integer> objPrepended = new ArrayList<>(
                        Stream.of(1, 2, 3).onClose(closes2::incrementAndGet).parallel(3).prepend(Stream.of(4, 5).onClose(closes2::incrementAndGet)).toList());
                Collections.sort(objPrepended);
                assertEquals(Arrays.asList(1, 2, 3, 4, 5), objPrepended);

                assertEquals(10, closes2.get());
            }
        }
    }
}

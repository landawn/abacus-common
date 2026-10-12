package com.landawn.abacus.util.stream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

/**
 * Behaviour-pinning and memory-retention tests written by the line-by-line performance and memory review of
 * {@code com.landawn.abacus.util.stream} on 2026-10-08. Each review group appends one {@code @Nested} class named after its group id.
 * <p>
 * Extends {@code TestBase} and is tagged {@code slow-test}: performance-review tests stay out of the
 * regular unit suite ({@code AbacusCoreTestSuite} excludes {@code slow-test}); run this class by name.
 */
@Tag("slow-test")
class StreamPerformanceMemoryReview20261008Test extends TestBase {

    // ---- perf review 2026-10-08 G084 begin ----
    @Nested
    class G084 {
        private static final int SIZE = 13;
        private static final int OFFSET = 3;
        private final String[] shared = { "s1", "s2" };
        private final String[] kinds = { "byte", "char", "double", "float" };
        private final String[] sequentialShapes = { "array", "range", "iterator" };

        /**
         * Mapped arrays by value % 5: null, empty, one element, three elements, a shared array.
         */
        private String[] mapped(final int value) {
            switch (value % 5) {
                case 0:
                    return null;
                case 1:
                    return new String[0];
                case 2:
                    return new String[] { "a" + value };
                case 3:
                    return new String[] { "a" + value, "b" + value, "c" + value };
                default:
                    return shared;
            }
        }

        private List<String> oracle(final int fromValue, final int toValue) {
            final List<String> result = new ArrayList<>();

            for (int value = fromValue; value < toValue; value++) {
                final String[] a = mapped(value);

                if (a != null) {
                    result.addAll(Arrays.asList(a));
                }
            }

            return result;
        }

        private List<Integer> values(final int fromValue, final int toValue) {
            final List<Integer> result = new ArrayList<>();

            for (int value = fromValue; value < toValue; value++) {
                result.add(value);
            }

            return result;
        }

        private String[] record(final int value, final List<Integer> calls, final int failOnValue1, final int failOnValue2, final RuntimeException failure) {
            calls.add(value);

            if (value == failOnValue1 || value == failOnValue2) {
                throw failure;
            }

            return mapped(value);
        }

        /**
         * Source shapes: "array" (whole array), "range" (array sub-range), "iterator" (iterator-backed stream), "parallel".
         */
        private ByteStream byteSource(final String shape, final int fromValue, final int toValue) {
            final int length = toValue - fromValue;
            final byte[] a = new byte[length + 2 * OFFSET];

            for (int i = 0; i < length; i++) {
                a[OFFSET + i] = (byte) (fromValue + i);
            }

            switch (shape) {
                case "array":
                    return ByteStream.of(Arrays.copyOfRange(a, OFFSET, OFFSET + length));
                case "range":
                    return ByteStream.of(a, OFFSET, OFFSET + length);
                case "iterator":
                    return ByteStream.of(a, OFFSET, OFFSET + length).map(e -> e);
                default:
                    return ByteStream.of(a, OFFSET, OFFSET + length).parallel(2);
            }
        }

        private CharStream charSource(final String shape, final int fromValue, final int toValue) {
            final int length = toValue - fromValue;
            final char[] a = new char[length + 2 * OFFSET];

            for (int i = 0; i < length; i++) {
                a[OFFSET + i] = (char) (fromValue + i);
            }

            switch (shape) {
                case "array":
                    return CharStream.of(Arrays.copyOfRange(a, OFFSET, OFFSET + length));
                case "range":
                    return CharStream.of(a, OFFSET, OFFSET + length);
                case "iterator":
                    return CharStream.of(a, OFFSET, OFFSET + length).map(e -> e);
                default:
                    return CharStream.of(a, OFFSET, OFFSET + length).parallel(2);
            }
        }

        private DoubleStream doubleSource(final String shape, final int fromValue, final int toValue) {
            final int length = toValue - fromValue;
            final double[] a = new double[length + 2 * OFFSET];

            for (int i = 0; i < length; i++) {
                a[OFFSET + i] = fromValue + i;
            }

            switch (shape) {
                case "array":
                    return DoubleStream.of(Arrays.copyOfRange(a, OFFSET, OFFSET + length));
                case "range":
                    return DoubleStream.of(a, OFFSET, OFFSET + length);
                case "iterator":
                    return DoubleStream.of(a, OFFSET, OFFSET + length).map(e -> e);
                default:
                    return DoubleStream.of(a, OFFSET, OFFSET + length).parallel(2);
            }
        }

        private FloatStream floatSource(final String shape, final int fromValue, final int toValue) {
            final int length = toValue - fromValue;
            final float[] a = new float[length + 2 * OFFSET];

            for (int i = 0; i < length; i++) {
                a[OFFSET + i] = fromValue + i;
            }

            switch (shape) {
                case "array":
                    return FloatStream.of(Arrays.copyOfRange(a, OFFSET, OFFSET + length));
                case "range":
                    return FloatStream.of(a, OFFSET, OFFSET + length);
                case "iterator":
                    return FloatStream.of(a, OFFSET, OFFSET + length).map(e -> e);
                default:
                    return FloatStream.of(a, OFFSET, OFFSET + length).parallel(2);
            }
        }

        /**
         * {@code flatMapArrayToObj} over the values {@code [fromValue, toValue)}; the mapper records its argument in {@code calls}
         * and throws {@code failure} for the two given values. A non-null {@code closeLog} gets "source" when the source closes.
         */
        private Stream<String> flat(final String kind, final String shape, final int fromValue, final int toValue, final List<Integer> calls,
                final int failOnValue1, final int failOnValue2, final RuntimeException failure, final List<String> closeLog) {
            final Runnable onSourceClose = () -> closeLog.add("source");

            switch (kind) {
                case "byte": {
                    final ByteStream source = byteSource(shape, fromValue, toValue);
                    return (closeLog == null ? source : source.onClose(onSourceClose))
                            .flatMapArrayToObj(e -> record(e, calls, failOnValue1, failOnValue2, failure));
                }

                case "char": {
                    final CharStream source = charSource(shape, fromValue, toValue);
                    return (closeLog == null ? source : source.onClose(onSourceClose))
                            .flatMapArrayToObj(e -> record(e, calls, failOnValue1, failOnValue2, failure));
                }

                case "double": {
                    final DoubleStream source = doubleSource(shape, fromValue, toValue);
                    return (closeLog == null ? source : source.onClose(onSourceClose))
                            .flatMapArrayToObj(e -> record((int) e, calls, failOnValue1, failOnValue2, failure));
                }

                default: {
                    final FloatStream source = floatSource(shape, fromValue, toValue);
                    return (closeLog == null ? source : source.onClose(onSourceClose))
                            .flatMapArrayToObj(e -> record((int) e, calls, failOnValue1, failOnValue2, failure));
                }
            }
        }

        private Stream<String> flat(final String kind, final String shape, final int fromValue, final int toValue, final List<Integer> calls) {
            return flat(kind, shape, fromValue, toValue, calls, -1, -1, null, null);
        }

        /**
         * A stream over the values {@code [0, SIZE)} from a user iterator that counts its hasNext() calls in counts[0] and its
         * next calls in counts[1].
         */
        private Stream<String> countingSource(final String kind, final int[] counts) {
            switch (kind) {
                case "byte":
                    return ByteStream.of(new com.landawn.abacus.util.ByteIterator() {
                        private int cursor = 0;

                        @Override
                        public boolean hasNext() {
                            counts[0]++;
                            return cursor < SIZE;
                        }

                        @Override
                        public byte nextByte() {
                            counts[1]++;
                            return (byte) cursor++;
                        }
                    }).flatMapArrayToObj(e -> mapped(e));

                case "char":
                    return CharStream.of(new com.landawn.abacus.util.CharIterator() {
                        private int cursor = 0;

                        @Override
                        public boolean hasNext() {
                            counts[0]++;
                            return cursor < SIZE;
                        }

                        @Override
                        public char nextChar() {
                            counts[1]++;
                            return (char) cursor++;
                        }
                    }).flatMapArrayToObj(e -> mapped(e));

                case "double":
                    return DoubleStream.of(new com.landawn.abacus.util.DoubleIterator() {
                        private int cursor = 0;

                        @Override
                        public boolean hasNext() {
                            counts[0]++;
                            return cursor < SIZE;
                        }

                        @Override
                        public double nextDouble() {
                            counts[1]++;
                            return cursor++;
                        }
                    }).flatMapArrayToObj(e -> mapped((int) e));

                default:
                    return FloatStream.of(new com.landawn.abacus.util.FloatIterator() {
                        private int cursor = 0;

                        @Override
                        public boolean hasNext() {
                            counts[0]++;
                            return cursor < SIZE;
                        }

                        @Override
                        public float nextFloat() {
                            counts[1]++;
                            return cursor++;
                        }
                    }).flatMapArrayToObj(e -> mapped((int) e));
            }
        }

        // G084-01: sequential flatMapArrayToObj reads mapped arrays in place; results, mapper calls and laziness are unchanged.
        @Test
        public void testFlatMapArrayToObj_sequentialMatchesOracle() {
            final List<String> all = oracle(0, SIZE);
            assertEquals(13, all.size());

            for (final String kind : kinds) {
                for (final String shape : sequentialShapes) {
                    final String label = kind + "/" + shape;
                    List<Integer> calls = new ArrayList<>();

                    assertEquals(all, flat(kind, shape, 0, SIZE, calls).toList(), label);
                    assertEquals(values(0, SIZE), calls, label);

                    assertEquals(all.size(), flat(kind, shape, 0, SIZE, new ArrayList<>()).count(), label);
                    assertArrayEquals(all.subList(4, all.size()).toArray(), flat(kind, shape, 0, SIZE, new ArrayList<>()).skip(4).toArray(), label);
                    assertEquals(oracle(5, 11), flat(kind, shape, 5, 11, new ArrayList<>()).toList(), label);
                    assertEquals(oracle(5, 7), flat(kind, shape, 5, 7, new ArrayList<>()).toList(), label);
                    assertTrue(flat(kind, shape, 4, 4, new ArrayList<>()).toList().isEmpty(), label);
                    assertFalse(flat(kind, shape, 0, SIZE, new ArrayList<>()).isParallel(), label);

                    // Lazy: only the source elements needed for the first 4 outputs (a2, a3, b3, c3) are mapped.
                    calls = new ArrayList<>();
                    assertEquals(all.subList(0, 4), flat(kind, shape, 0, SIZE, calls).limit(4).toList(), label);
                    assertEquals(Arrays.asList(0, 1, 2, 3), calls, label);

                    calls = new ArrayList<>();
                    assertEquals(all.subList(0, 5), flat(kind, shape, 0, SIZE, calls).limit(5).toList(), label);
                    assertEquals(Arrays.asList(0, 1, 2, 3, 4), calls, label);

                    // The shared array is read again for every element that maps to it.
                    calls = new ArrayList<>();
                    assertEquals(Arrays.asList("s1", "s2", "s1", "s2"), flat(kind, shape, 0, SIZE, calls).filter(s -> s.startsWith("s")).toList(), label);
                    assertEquals(values(0, SIZE), calls, label);
                }
            }
        }

        // G084-01: iterator protocol - next() without hasNext(), a throwing mapper (same exception, next source element on retry), exhaustion.
        @Test
        public void testFlatMapArrayToObj_iteratorProtocolAndMapperFailure() {
            final IllegalStateException failure = new IllegalStateException("boom");

            for (final String kind : kinds) {
                for (final String shape : sequentialShapes) {
                    final String label = kind + "/" + shape;

                    List<Integer> calls = new ArrayList<>();
                    Iterator<String> iter = flat(kind, shape, 0, SIZE, calls).iterator();
                    final List<String> viaNext = new ArrayList<>();

                    while (true) {
                        try {
                            viaNext.add(iter.next());
                        } catch (final java.util.NoSuchElementException e) {
                            break;
                        }
                    }

                    assertEquals(oracle(0, SIZE), viaNext, label);
                    assertEquals(values(0, SIZE), calls, label);
                    assertFalse(iter.hasNext(), label);
                    assertFalse(iter.hasNext(), label);

                    calls = new ArrayList<>();
                    iter = flat(kind, shape, 0, SIZE, calls, 3, 8, failure, null).iterator();
                    final List<String> seen = new ArrayList<>();

                    for (int guard = 0; guard < 100; guard++) {
                        try {
                            if (!iter.hasNext()) {
                                break;
                            }

                            seen.add(iter.next());
                        } catch (final IllegalStateException e) {
                            assertSame(failure, e, label);
                            seen.add("!");
                        }
                    }

                    assertEquals(Arrays.asList("a2", "!", "s1", "s2", "a7", "!", "s1", "s2", "a12"), seen, label);
                    assertEquals(values(0, SIZE), calls, label);
                    assertFalse(iter.hasNext(), label);
                    assertThrows(java.util.NoSuchElementException.class, iter::next, label);

                    // A failing mapper during a terminal operation propagates the same exception and maps nothing further.
                    final List<Integer> failCalls = new ArrayList<>();
                    assertSame(failure, assertThrows(IllegalStateException.class, () -> flat(kind, shape, 0, SIZE, failCalls, 3, 8, failure, null).toList()),
                            label);
                    assertEquals(Arrays.asList(0, 1, 2, 3), failCalls, label);
                }
            }
        }

        // G084-01: the source iterator sees exactly the same hasNext()/next calls (one hasNext per mapped element, one per exhausted check).
        @Test
        public void testFlatMapArrayToObj_sourceIteratorCallCounts() {
            for (final String kind : kinds) {
                int[] counts = new int[2];
                assertEquals(oracle(0, SIZE), countingSource(kind, counts).toList(), kind);
                assertEquals(SIZE + 1, counts[0], kind);
                assertEquals(SIZE, counts[1], kind);

                counts = new int[2];
                final Iterator<String> iter = countingSource(kind, counts).iterator();
                int n = 0;

                while (iter.hasNext()) {
                    iter.next();
                    n++;
                }

                assertFalse(iter.hasNext(), kind);
                assertFalse(iter.hasNext(), kind);
                assertEquals(13, n, kind);
                assertEquals(SIZE + 3, counts[0], kind);
                assertEquals(SIZE, counts[1], kind);

                counts = new int[2];
                assertEquals(oracle(0, SIZE).subList(0, 2), countingSource(kind, counts).limit(2).toList(), kind);
                assertEquals(4, counts[0], kind);
                assertEquals(4, counts[1], kind);
            }
        }

        // G084-01: close handlers run as before; closing mid-way drops the rest of the current mapped array.
        @Test
        public void testFlatMapArrayToObj_closeHandlers() {
            for (final String kind : kinds) {
                for (final String shape : sequentialShapes) {
                    final String label = kind + "/" + shape;
                    final List<String> log = new ArrayList<>();
                    final Stream<String> stream = flat(kind, shape, 0, SIZE, new ArrayList<>(), -1, -1, null, log).onClose(() -> log.add("mapped"));

                    assertEquals(oracle(0, SIZE), stream.toList(), label);
                    assertEquals(Arrays.asList("source", "mapped"), log, label);

                    final List<String> log2 = new ArrayList<>();
                    final Stream<String> stream2 = flat(kind, shape, 0, SIZE, new ArrayList<>(), -1, -1, null, log2);
                    final Iterator<String> iter = stream2.iterator();

                    assertEquals("a2", iter.next(), label);
                    assertEquals("a3", iter.next(), label);
                    stream2.close();
                    stream2.close();
                    assertEquals(Arrays.asList("source"), log2, label);

                    final List<String> rest = new ArrayList<>();
                    iter.forEachRemaining(rest::add);
                    assertEquals(Arrays.asList("s1", "s2", "a7", "a8", "b8", "c8", "s1", "s2", "a12"), rest, label);
                }
            }
        }

        // G084-01: the parallel path is untouched (still flatMapToObj over a mapped Stream per element).
        @Test
        public void testFlatMapArrayToObj_parallelUnchanged() {
            for (final String kind : kinds) {
                final List<Integer> calls = Collections.synchronizedList(new ArrayList<>());
                final Stream<String> stream = flat(kind, "parallel", 0, SIZE, calls);
                assertTrue(stream.isParallel(), kind);

                final List<String> result = new ArrayList<>(stream.toList());
                final List<String> expected = oracle(0, SIZE);
                Collections.sort(result);
                Collections.sort(expected);
                assertEquals(expected, result, kind);

                final List<Integer> sortedCalls = new ArrayList<>(calls);
                Collections.sort(sortedCalls);
                assertEquals(values(0, SIZE), sortedCalls, kind);
            }
        }
    }

    // ---- perf review 2026-10-08 G084 end ----
    // ---- perf review 2026-10-08 G085 begin ----
    @Nested
    class G085 {
        private final boolean[] iteratorSources = { false, true };

        /**
         * Maps a value to a null, empty, singleton, null-element or three-element array.
         */
        private String[] arrayFor(final long value) {
            switch ((int) Math.floorMod(value, 5L)) {
                case 0:
                    return null;
                case 1:
                    return new String[0];
                case 2:
                    return new String[] { "s" + value };
                case 3:
                    return new String[] { null, "n" + value };
                default:
                    return new String[] { "a" + value, "b" + value, "c" + value };
            }
        }

        private List<String> expectedFor(final long... values) {
            final List<String> expected = new ArrayList<>();

            for (final long value : values) {
                final String[] a = arrayFor(value);

                if (a != null) {
                    expected.addAll(Arrays.asList(a));
                }
            }

            return expected;
        }

        private List<String> sortedNullsFirst(final Collection<String> c) {
            final List<String> list = new ArrayList<>(c);
            list.sort(java.util.Comparator.nullsFirst(java.util.Comparator.naturalOrder()));
            return list;
        }

        private IntStream intSource(final boolean iteratorSource, final int... a) {
            return iteratorSource ? IntStream.of(a).map(x -> x) : IntStream.of(a);
        }

        private LongStream longSource(final boolean iteratorSource, final long... a) {
            return iteratorSource ? LongStream.of(a).map(x -> x) : LongStream.of(a);
        }

        private ShortStream shortSource(final boolean iteratorSource, final short... a) {
            return iteratorSource ? ShortStream.of(a).map(x -> x) : ShortStream.of(a);
        }

        // G085-01: sequential IntStream.flatMapArrayToObj reads each mapped array in place; contents, order and mapper calls pinned.
        @Test
        public void testFlatMapArrayToObj_intMixedArraysAndMapperCalls() {
            final int[] source = { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, -1, -2, -3, -4, -5, Integer.MIN_VALUE, Integer.MAX_VALUE };
            final long[] sourceAsLong = new long[source.length];
            final List<Integer> boxedSource = new ArrayList<>();

            for (int i = 0; i < source.length; i++) {
                sourceAsLong[i] = source[i];
                boxedSource.add(source[i]);
            }

            for (final boolean iteratorSource : iteratorSources) {
                final List<Integer> calls = new ArrayList<>();

                final List<String> result = intSource(iteratorSource, source).flatMapArrayToObj(v -> {
                    calls.add(v);
                    return arrayFor(v);
                }).toList();

                assertEquals(expectedFor(sourceAsLong), result);
                assertEquals(boxedSource, calls);
            }

            // sub-range array source
            assertEquals(expectedFor(2, 3, 4, 5, 6, 7, 8), IntStream.of(source, 2, 9).flatMapArrayToObj(this::arrayFor).toList());

            // empty source: the mapper is never called
            final AtomicInteger emptyCalls = new AtomicInteger();
            assertEquals(Collections.emptyList(), IntStream.empty().flatMapArrayToObj(v -> {
                emptyCalls.incrementAndGet();
                return new String[] { "x" };
            }).toList());
            assertEquals(0, emptyCalls.get());

            // only null and empty arrays
            assertEquals(Collections.emptyList(), IntStream.of(0, 1, 5, 6, 10).flatMapArrayToObj(this::arrayFor).toList());
            assertEquals(Collections.emptyList(), IntStream.of(0, 1, 5, 6, 10).map(x -> x).flatMapArrayToObj(this::arrayFor).toList());
        }

        // G085-01: terminal and skip/limit operations on the flattened IntStream result.
        @Test
        public void testFlatMapArrayToObj_intTerminalOps() {
            final int[] source = { 4, 0, 3, 1, 2, 4, 3 };
            final List<String> expected = expectedFor(4, 0, 3, 1, 2, 4, 3);

            for (final boolean iteratorSource : iteratorSources) {
                assertEquals(expected.size(), intSource(iteratorSource, source).flatMapArrayToObj(this::arrayFor).count());
                assertArrayEquals(expected.toArray(), intSource(iteratorSource, source).flatMapArrayToObj(this::arrayFor).toArray());

                for (int k = 0; k <= expected.size() + 1; k++) {
                    final int cut = Math.min(k, expected.size());

                    assertEquals(expected.subList(cut, expected.size()), intSource(iteratorSource, source).flatMapArrayToObj(this::arrayFor).skip(k).toList());
                    assertEquals(expected.subList(0, cut), intSource(iteratorSource, source).flatMapArrayToObj(this::arrayFor).limit(k).toList());
                }
            }
        }

        // G085-01: the mapper still runs lazily, only when the previous array is exhausted.
        @Test
        public void testFlatMapArrayToObj_lazyMapperCalls() {
            for (final boolean iteratorSource : iteratorSources) {
                final List<Integer> calls = new ArrayList<>();
                final int[] source = new int[100];

                for (int i = 0; i < source.length; i++) {
                    source[i] = i;
                }

                final List<String> result = intSource(iteratorSource, source).flatMapArrayToObj(v -> {
                    calls.add(v);
                    return v % 2 == 0 ? new String[0] : new String[] { "x" + v, "y" + v };
                }).limit(3).toList();

                assertEquals(Arrays.asList("x1", "y1", "x3"), result);
                assertEquals(Arrays.asList(0, 1, 2, 3), calls);
            }
        }

        // G085-01: iterator protocol; a failing mapper call consumes its source element and iteration can continue.
        @Test
        public void testFlatMapArrayToObj_iteratorProtocolAndMapperFailure() {
            for (final boolean iteratorSource : iteratorSources) {
                final List<Integer> calls = new ArrayList<>();

                final Iterator<String> iter = intSource(iteratorSource, 1, 2, 3, 4).flatMapArrayToObj(v -> {
                    calls.add(v);

                    if (v == 2) {
                        throw new IllegalStateException("boom" + v);
                    }

                    return v == 4 ? null : new String[] { "a" + v, "b" + v };
                }).iterator();

                assertTrue(iter.hasNext());
                assertTrue(iter.hasNext());
                assertEquals(Arrays.asList(1), calls);
                assertEquals("a1", iter.next());
                assertEquals("b1", iter.next());

                final IllegalStateException e = assertThrows(IllegalStateException.class, iter::hasNext);
                assertEquals("boom2", e.getMessage());

                assertEquals("a3", iter.next());
                assertEquals("b3", iter.next());
                assertFalse(iter.hasNext());
                assertFalse(iter.hasNext());

                final java.util.NoSuchElementException e2 = assertThrows(java.util.NoSuchElementException.class, iter::next);
                assertEquals(StreamBase.ERROR_MSG_FOR_NO_SUCH_EX, e2.getMessage());
                assertEquals(Arrays.asList(1, 2, 3, 4), calls);
            }
        }

        // G085-01: elements of a mapped array are read when they are reached, not when the array is returned.
        @Test
        public void testFlatMapArrayToObj_arrayElementsReadWhenReached() {
            for (final boolean iteratorSource : iteratorSources) {
                final String[] shared = { "p", "q", "r" };
                final Iterator<String> iter = intSource(iteratorSource, 7).flatMapArrayToObj(v -> shared).iterator();

                assertEquals("p", iter.next());
                shared[1] = "Q";
                shared[2] = null;
                assertEquals("Q", iter.next());
                assertNull(iter.next());
                assertFalse(iter.hasNext());
            }
        }

        // G085-01: close handlers of the source stream and of the result stream still run, once each and in order.
        @Test
        public void testFlatMapArrayToObj_closeHandlers() {
            for (final boolean iteratorSource : iteratorSources) {
                final List<String> log = new ArrayList<>();

                final List<String> result = intSource(iteratorSource, 1, 2).onClose(() -> log.add("source"))
                        .flatMapArrayToObj(v -> new String[] { "v" + v })
                        .onClose(() -> log.add("result"))
                        .toList();

                assertEquals(Arrays.asList("v1", "v2"), result);
                assertEquals(Arrays.asList("source", "result"), log);

                final List<String> log2 = new ArrayList<>();
                final Stream<String> unconsumed = intSource(iteratorSource, 1, 2).onClose(() -> log2.add("source"))
                        .flatMapArrayToObj(v -> new String[] { "v" + v });
                unconsumed.close();
                unconsumed.close();
                assertEquals(Arrays.asList("source"), log2);
            }
        }

        // G085-01: the parallel path is unchanged (flatMapToObj); pin its contents.
        @Test
        public void testFlatMapArrayToObj_parallelContents() {
            final int[] source = new int[300];
            final long[] sourceAsLong = new long[source.length];

            for (int i = 0; i < source.length; i++) {
                source[i] = i - 7;
                sourceAsLong[i] = source[i];
            }

            final List<String> expected = sortedNullsFirst(expectedFor(sourceAsLong));

            for (final boolean iteratorSource : iteratorSources) {
                assertEquals(expected, sortedNullsFirst(intSource(iteratorSource, source).parallel(4).flatMapArrayToObj(this::arrayFor).toList()));
                assertEquals(expected, sortedNullsFirst(longSource(iteratorSource, sourceAsLong).parallel(4).flatMapArrayToObj(this::arrayFor).toList()));
            }
        }

        // G085-01: sequential LongStream.flatMapArrayToObj - contents, mapper calls, laziness and failure handling.
        @Test
        public void testFlatMapArrayToObj_long() {
            final long[] source = { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, -1, -2, -3, -4, -5, Long.MIN_VALUE, Long.MAX_VALUE };
            final List<Long> boxedSource = new ArrayList<>();

            for (final long v : source) {
                boxedSource.add(v);
            }

            for (final boolean iteratorSource : iteratorSources) {
                final List<Long> calls = new ArrayList<>();

                final List<String> result = longSource(iteratorSource, source).flatMapArrayToObj(v -> {
                    calls.add(v);
                    return arrayFor(v);
                }).toList();

                assertEquals(expectedFor(source), result);
                assertEquals(boxedSource, calls);

                final List<Long> lazyCalls = new ArrayList<>();
                assertEquals(Arrays.asList("x1", "y1", "x3"), longSource(iteratorSource, 0, 1, 2, 3, 4, 5).flatMapArrayToObj(v -> {
                    lazyCalls.add(v);
                    return v % 2 == 0 ? null : new String[] { "x" + v, "y" + v };
                }).limit(3).toList());
                assertEquals(Arrays.asList(0L, 1L, 2L, 3L), lazyCalls);

                final Iterator<String> iter = longSource(iteratorSource, 1, 2, 3).flatMapArrayToObj(v -> {
                    if (v == 2) {
                        throw new IllegalStateException("boom" + v);
                    }

                    return new String[] { "a" + v };
                }).iterator();

                assertEquals("a1", iter.next());
                assertEquals("boom2", assertThrows(IllegalStateException.class, iter::next).getMessage());
                assertEquals("a3", iter.next());
                assertFalse(iter.hasNext());
                assertEquals(StreamBase.ERROR_MSG_FOR_NO_SUCH_EX, assertThrows(java.util.NoSuchElementException.class, iter::next).getMessage());

                assertEquals(expectedFor(4, 0, 3).size(), longSource(iteratorSource, 4, 0, 3).flatMapArrayToObj(this::arrayFor).count());
            }

            assertEquals(expectedFor(2, 3, 4), LongStream.of(source, 2, 5).flatMapArrayToObj(this::arrayFor).toList());
            assertEquals(Collections.emptyList(), LongStream.empty().flatMapArrayToObj(this::arrayFor).toList());
        }

        // G085-01: sequential ShortStream.flatMapArrayToObj - contents, mapper calls, laziness, failure handling and parallel contents.
        @Test
        public void testFlatMapArrayToObj_short() {
            final short[] source = { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, -1, -2, -3, -4, -5, Short.MIN_VALUE, Short.MAX_VALUE };
            final long[] sourceAsLong = new long[source.length];
            final List<Short> boxedSource = new ArrayList<>();

            for (int i = 0; i < source.length; i++) {
                sourceAsLong[i] = source[i];
                boxedSource.add(source[i]);
            }

            for (final boolean iteratorSource : iteratorSources) {
                final List<Short> calls = new ArrayList<>();

                final List<String> result = shortSource(iteratorSource, source).flatMapArrayToObj(v -> {
                    calls.add(v);
                    return arrayFor(v);
                }).toList();

                assertEquals(expectedFor(sourceAsLong), result);
                assertEquals(boxedSource, calls);

                final List<Short> lazyCalls = new ArrayList<>();
                assertEquals(Arrays.asList("x1", "y1", "x3"),
                        shortSource(iteratorSource, (short) 0, (short) 1, (short) 2, (short) 3, (short) 4).flatMapArrayToObj(v -> {
                            lazyCalls.add(v);
                            return v % 2 == 0 ? new String[0] : new String[] { "x" + v, "y" + v };
                        }).limit(3).toList());
                assertEquals(Arrays.asList((short) 0, (short) 1, (short) 2, (short) 3), lazyCalls);

                final Iterator<String> iter = shortSource(iteratorSource, (short) 1, (short) 2, (short) 3).flatMapArrayToObj(v -> {
                    if (v == 2) {
                        throw new IllegalStateException("boom" + v);
                    }

                    return new String[] { "a" + v };
                }).iterator();

                assertEquals("a1", iter.next());
                assertEquals("boom2", assertThrows(IllegalStateException.class, iter::next).getMessage());
                assertEquals("a3", iter.next());
                assertFalse(iter.hasNext());
                assertEquals(StreamBase.ERROR_MSG_FOR_NO_SUCH_EX, assertThrows(java.util.NoSuchElementException.class, iter::next).getMessage());

                assertEquals(sortedNullsFirst(expectedFor(sourceAsLong)),
                        sortedNullsFirst(shortSource(iteratorSource, source).parallel(3).flatMapArrayToObj(this::arrayFor).toList()));
            }

            assertEquals(expectedFor(2, 3, 4), ShortStream.of(source, 2, 5).flatMapArrayToObj(this::arrayFor).toList());
            assertEquals(Collections.emptyList(), ShortStream.empty().flatMapArrayToObj(this::arrayFor).toList());
        }
    }

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

    // ---- perf review 2026-10-08 G086 end ----
    // ---- perf review 2026-10-08 G087 begin ----
    @Nested
    class G087 {
        private static final int OP_STEP = 0;
        private static final int OP_SKIP = 1;
        private static final int OP_LIMIT = 2;

        private final long[] interestingCounts = { 0, 1, 2, 3, 5, 7, 31, 64, Integer.MAX_VALUE, Integer.MAX_VALUE + 1L, Long.MAX_VALUE };

        private long randomCount(final Random random, final int size) {
            return random.nextInt(4) == 0 ? interestingCounts[random.nextInt(interestingCounts.length)] : random.nextInt(size + 4);
        }

        private List<long[]> randomTail(final Random random, final int size) {
            final List<long[]> ops = new ArrayList<>();
            final int opCount = random.nextInt(4);

            for (int i = 0; i < opCount; i++) {
                final int kind = random.nextInt(3);
                final long arg = kind == OP_STEP ? 1 + random.nextInt(5) : randomCount(random, size);
                ops.add(new long[] { kind, arg });
            }

            return ops;
        }

        private List<Integer> applyOracle(final List<Integer> positions, final List<long[]> ops) {
            List<Integer> cur = positions;

            for (final long[] op : ops) {
                final List<Integer> next = new ArrayList<>();

                if (op[0] == OP_STEP) {
                    for (long i = 0; i < cur.size(); i += op[1]) {
                        next.add(cur.get((int) i));
                    }
                } else if (op[0] == OP_SKIP) {
                    next.addAll(cur.subList((int) Math.min(op[1], cur.size()), cur.size()));
                } else {
                    next.addAll(cur.subList(0, (int) Math.min(op[1], cur.size())));
                }

                cur = next;
            }

            return cur;
        }

        private List<Integer> range(final int fromIndex, final int toIndex) {
            final List<Integer> result = new ArrayList<>();

            for (int i = fromIndex; i < toIndex; i++) {
                result.add(i);
            }

            return result;
        }

        private ByteStream applyByte(ByteStream s, final List<long[]> ops) {
            for (final long[] op : ops) {
                s = op[0] == OP_STEP ? s.step(op[1]) : op[0] == OP_SKIP ? s.skip(op[1]) : s.limit(op[1]);
            }

            return s;
        }

        private CharStream applyChar(CharStream s, final List<long[]> ops) {
            for (final long[] op : ops) {
                s = op[0] == OP_STEP ? s.step(op[1]) : op[0] == OP_SKIP ? s.skip(op[1]) : s.limit(op[1]);
            }

            return s;
        }

        private DoubleStream applyDouble(DoubleStream s, final List<long[]> ops) {
            for (final long[] op : ops) {
                s = op[0] == OP_STEP ? s.step(op[1]) : op[0] == OP_SKIP ? s.skip(op[1]) : s.limit(op[1]);
            }

            return s;
        }

        private IntStream applyInt(IntStream s, final List<long[]> ops) {
            for (final long[] op : ops) {
                s = op[0] == OP_STEP ? s.step(op[1]) : op[0] == OP_SKIP ? s.skip(op[1]) : s.limit(op[1]);
            }

            return s;
        }

        private String describe(final int length, final int fromIndex, final int toIndex, final String head, final List<long[]> ops) {
            final StringBuilder sb = new StringBuilder("len=" + length + " range=[" + fromIndex + "," + toIndex + ") " + head);

            for (final long[] op : ops) {
                sb.append(op[0] == OP_STEP ? ".step(" : op[0] == OP_SKIP ? ".skip(" : ".limit(").append(op[1]).append(')');
            }

            return sb.toString();
        }

        // G087-01: step(k) on byte/char/double array streams followed by random skip/limit/step chains matches an index oracle
        @Test
        void testStepSkip_matchesOracle() {
            final Random random = new Random(20261008L);

            for (int round = 0; round < 2500; round++) {
                final int length = random.nextInt(4) == 0 ? 0 : random.nextInt(40);
                final int fromIndex = length == 0 ? 0 : random.nextInt(length + 1);
                final int toIndex = fromIndex + (length == fromIndex ? 0 : random.nextInt(length - fromIndex + 1));
                final long step = random.nextInt(8) == 0 ? (random.nextBoolean() ? Integer.MAX_VALUE + 7L : Long.MAX_VALUE) : 2 + random.nextInt(6);
                final List<long[]> ops = new ArrayList<>();
                ops.add(new long[] { OP_STEP, step });
                ops.addAll(randomTail(random, length));
                ops.add(1, new long[] { OP_SKIP, randomCount(random, length) });

                final List<Integer> expected = applyOracle(range(fromIndex, toIndex), ops);
                final String msg = describe(length, fromIndex, toIndex, "step-first", ops);

                final byte[] bytes = new byte[length];
                final char[] chars = new char[length];
                final double[] doubles = new double[length];

                for (int i = 0; i < length; i++) {
                    bytes[i] = (byte) random.nextInt();
                    chars[i] = (char) random.nextInt();
                    doubles[i] = random.nextInt(10) == 0 ? (random.nextBoolean() ? Double.NaN : -0.0d) : random.nextGaussian();
                }

                final byte[] expectedBytes = new byte[expected.size()];
                final char[] expectedChars = new char[expected.size()];
                final double[] expectedDoubles = new double[expected.size()];

                for (int i = 0; i < expected.size(); i++) {
                    expectedBytes[i] = bytes[expected.get(i)];
                    expectedChars[i] = chars[expected.get(i)];
                    expectedDoubles[i] = doubles[expected.get(i)];
                }

                assertArrayEquals(expectedBytes, applyByte(ByteStream.of(bytes, fromIndex, toIndex), ops).toArray(), msg);
                assertEquals(expected.size(), applyByte(ByteStream.of(bytes, fromIndex, toIndex), ops).count(), msg);
                assertArrayEquals(expectedChars, applyChar(CharStream.of(chars, fromIndex, toIndex), ops).toArray(), msg);
                assertEquals(expected.size(), applyChar(CharStream.of(chars, fromIndex, toIndex), ops).count(), msg);
                assertArrayEquals(expectedDoubles, applyDouble(DoubleStream.of(doubles, fromIndex, toIndex), ops).toArray(), msg);
                assertEquals(expected.size(), applyDouble(DoubleStream.of(doubles, fromIndex, toIndex), ops).count(), msg);

                // iteratorEx() is what iterator() returns, minus its close-handler warning
                final ByteIteratorEx byteIter = applyByte(ByteStream.of(bytes, fromIndex, toIndex), ops).iteratorEx();
                final CharIteratorEx charIter = applyChar(CharStream.of(chars, fromIndex, toIndex), ops).iteratorEx();
                final DoubleIteratorEx doubleIter = applyDouble(DoubleStream.of(doubles, fromIndex, toIndex), ops).iteratorEx();

                for (int i = 0; i < expected.size(); i++) {
                    assertTrue(byteIter.hasNext(), msg);
                    assertTrue(byteIter.hasNext(), msg);
                    assertEquals(expectedBytes[i], byteIter.nextByte(), msg);
                    assertEquals(expectedChars[i], charIter.nextChar(), msg);
                    assertEquals(Double.doubleToLongBits(expectedDoubles[i]), Double.doubleToLongBits(doubleIter.nextDouble()), msg);
                }

                assertFalse(byteIter.hasNext(), msg);
                assertFalse(charIter.hasNext(), msg);
                assertFalse(doubleIter.hasNext(), msg);
                assertThrows(java.util.NoSuchElementException.class, byteIter::nextByte, msg);
                assertThrows(java.util.NoSuchElementException.class, charIter::nextChar, msg);
                assertThrows(java.util.NoSuchElementException.class, doubleIter::nextDouble, msg);
            }
        }

        // G087-01: asIntStream() of byte/char array streams followed by random skip/limit/step chains matches an index oracle
        @Test
        void testAsIntStreamSkip_matchesOracle() {
            final Random random = new Random(8102601L);

            for (int round = 0; round < 2500; round++) {
                final int length = random.nextInt(4) == 0 ? 0 : random.nextInt(40);
                final int fromIndex = length == 0 ? 0 : random.nextInt(length + 1);
                final int toIndex = fromIndex + (length == fromIndex ? 0 : random.nextInt(length - fromIndex + 1));
                final List<long[]> ops = new ArrayList<>();
                ops.add(new long[] { OP_SKIP, randomCount(random, length) });
                ops.addAll(randomTail(random, length));

                if (random.nextBoolean()) {
                    ops.add(random.nextInt(ops.size() + 1), new long[] { OP_SKIP, randomCount(random, length) });
                }

                final List<Integer> expected = applyOracle(range(fromIndex, toIndex), ops);
                final String msg = describe(length, fromIndex, toIndex, "asIntStream()", ops);

                final byte[] bytes = new byte[length];
                final char[] chars = new char[length];

                for (int i = 0; i < length; i++) {
                    bytes[i] = (byte) random.nextInt();
                    chars[i] = (char) random.nextInt();
                }

                final int[] expectedFromBytes = new int[expected.size()];
                final int[] expectedFromChars = new int[expected.size()];
                long expectedByteSum = 0;
                long expectedCharSum = 0;

                for (int i = 0; i < expected.size(); i++) {
                    expectedFromBytes[i] = bytes[expected.get(i)];
                    expectedFromChars[i] = chars[expected.get(i)];
                    expectedByteSum += expectedFromBytes[i];
                    expectedCharSum += expectedFromChars[i];
                }

                assertArrayEquals(expectedFromBytes, applyInt(ByteStream.of(bytes, fromIndex, toIndex).asIntStream(), ops).toArray(), msg);
                assertEquals(expected.size(), applyInt(ByteStream.of(bytes, fromIndex, toIndex).asIntStream(), ops).count(), msg);
                assertEquals(expectedByteSum, applyInt(ByteStream.of(bytes, fromIndex, toIndex).asIntStream(), ops).sum(), msg);
                assertArrayEquals(expectedFromChars, applyInt(CharStream.of(chars, fromIndex, toIndex).asIntStream(), ops).toArray(), msg);
                assertEquals(expected.size(), applyInt(CharStream.of(chars, fromIndex, toIndex).asIntStream(), ops).count(), msg);
                assertEquals(expectedCharSum, applyInt(CharStream.of(chars, fromIndex, toIndex).asIntStream(), ops).sum(), msg);

                // iteratorEx() is what iterator() returns, minus its close-handler warning
                final IntIteratorEx byteIter = applyInt(ByteStream.of(bytes, fromIndex, toIndex).asIntStream(), ops).iteratorEx();
                final IntIteratorEx charIter = applyInt(CharStream.of(chars, fromIndex, toIndex).asIntStream(), ops).iteratorEx();

                for (int i = 0; i < expected.size(); i++) {
                    assertTrue(byteIter.hasNext(), msg);
                    assertEquals(expectedFromBytes[i], byteIter.nextInt(), msg);
                    assertTrue(charIter.hasNext(), msg);
                    assertTrue(charIter.hasNext(), msg);
                    assertEquals(expectedFromChars[i], charIter.nextInt(), msg);
                }

                assertFalse(byteIter.hasNext(), msg);
                assertFalse(charIter.hasNext(), msg);
                assertThrows(java.util.NoSuchElementException.class, byteIter::nextInt, msg);
                assertThrows(java.util.NoSuchElementException.class, charIter::nextInt, msg);
            }
        }

        // G087-01: hand-picked boundaries of skip after step()/asIntStream(): exact remaining count, one past it, huge counts, sub-ranges
        @Test
        void testStepSkip_boundaries() {
            final byte[] bytes = { 10, -20, 30, -40, 50, -60, 70 };
            final char[] chars = { 'a', '\uffff', 'c', '\u0080', 'e', 'f', 'g' };
            final double[] doubles = { 1.5, Double.NaN, -0.0d, 0.0d, Double.NEGATIVE_INFINITY, 6.5, 7.5 };

            assertArrayEquals(new byte[] { 50, 70 }, ByteStream.of(bytes).step(2).skip(2).toArray());
            assertArrayEquals(new byte[] { 70 }, ByteStream.of(bytes).step(2).skip(3).toArray());
            assertArrayEquals(new byte[0], ByteStream.of(bytes).step(2).skip(4).toArray());
            assertArrayEquals(new byte[0], ByteStream.of(bytes).step(2).skip(Long.MAX_VALUE).toArray());
            assertArrayEquals(new byte[] { -40 }, ByteStream.of(bytes, 1, 6).step(2).skip(1).limit(1).toArray());
            assertArrayEquals(new byte[] { -20, 50 }, ByteStream.of(bytes, 1, 7).step(3).toArray());
            assertArrayEquals(new byte[] { 50 }, ByteStream.of(bytes, 1, 7).step(3).skip(1).toArray());
            assertArrayEquals(new byte[] { 10 }, ByteStream.of(bytes).step(Long.MAX_VALUE).toArray());
            assertArrayEquals(new byte[0], ByteStream.of(bytes).step(Long.MAX_VALUE).skip(1).toArray());
            assertArrayEquals(new byte[] { 70 }, ByteStream.of(bytes).step(3).skip(1).skip(1).toArray());
            assertEquals(1, ByteStream.of(bytes).step(2).skip(1).limit(2).skip(1).count());
            assertArrayEquals(new byte[0], ByteStream.empty().step(2).skip(1).toArray());

            assertArrayEquals(new int[] { 50, -60, 70 }, ByteStream.of(bytes).asIntStream().skip(4).toArray());
            assertArrayEquals(new int[] { 70 }, ByteStream.of(bytes).asIntStream().skip(6).toArray());
            assertArrayEquals(new int[0], ByteStream.of(bytes).asIntStream().skip(7).toArray());
            assertArrayEquals(new int[0], ByteStream.of(bytes).asIntStream().skip(Long.MAX_VALUE).toArray());
            assertArrayEquals(new int[] { -40, 50 }, ByteStream.of(bytes, 2, 5).asIntStream().skip(1).toArray());
            assertArrayEquals(new int[] { -20, -60 }, ByteStream.of(bytes, 1, 7).asIntStream().step(4).toArray());
            assertArrayEquals(new int[] { 50 }, ByteStream.of(bytes).asIntStream().skip(1).step(3).skip(1).limit(1).toArray());
            assertEquals(0, ByteStream.of(bytes).asIntStream().limit(3).skip(5).count());
            assertEquals(-40 + 50 - 60 + 70, ByteStream.of(bytes).asIntStream().skip(3).sum());

            assertArrayEquals(new char[] { 'e', 'g' }, CharStream.of(chars).step(2).skip(2).toArray());
            assertArrayEquals(new char[0], CharStream.of(chars).step(2).skip(4).toArray());
            assertArrayEquals(new char[] { '\uffff', 'e' }, CharStream.of(chars, 1, 7).step(3).toArray());
            assertArrayEquals(new int[] { 0xffff, 'c', 0x80 }, CharStream.of(chars).asIntStream().skip(1).limit(3).toArray());
            assertArrayEquals(new int[] { 'g' }, CharStream.of(chars).asIntStream().skip(6).toArray());
            assertArrayEquals(new int[0], CharStream.of(chars, 3, 3).asIntStream().skip(1).toArray());
            assertEquals(2, CharStream.of(chars).asIntStream().skip(5).count());

            assertArrayEquals(new double[] { Double.NEGATIVE_INFINITY, 7.5 }, DoubleStream.of(doubles).step(2).skip(2).toArray());
            assertArrayEquals(new double[] { -0.0d, Double.NEGATIVE_INFINITY }, DoubleStream.of(doubles).step(2).skip(1).limit(2).toArray());
            assertArrayEquals(new double[0], DoubleStream.of(doubles).step(2).skip(4).toArray());
            assertArrayEquals(new double[] { Double.NaN, Double.NEGATIVE_INFINITY }, DoubleStream.of(doubles, 1, 7).step(3).toArray());
            assertArrayEquals(new double[] { 0.0d }, DoubleStream.of(doubles).step(3).skip(1).limit(1).toArray());
            assertEquals(2, DoubleStream.of(doubles).step(3).skip(1).count());
            assertEquals(0, DoubleStream.of(doubles).step(3).skip(Long.MAX_VALUE).count());
        }

        // G087-01: parallel array streams inherit step()/asIntStream(); skip after them still yields the same elements
        @Test
        void testAsIntStreamSkip_parallel() {
            final byte[] bytes = new byte[2000];
            final char[] chars = new char[2000];
            final double[] doubles = new double[2000];

            for (int i = 0; i < bytes.length; i++) {
                bytes[i] = (byte) (i * 7);
                chars[i] = (char) (i * 37);
                doubles[i] = i * 0.25;
            }

            final int[] expectedFromBytes = new int[1990];
            final int[] expectedFromChars = new int[1990];

            for (int i = 0; i < 1990; i++) {
                expectedFromBytes[i] = bytes[i + 10];
                expectedFromChars[i] = chars[i + 10];
            }

            final int[] actualFromBytes = ByteStream.of(bytes).parallel(3).asIntStream().skip(10).toArray();
            final int[] actualFromChars = CharStream.of(chars).parallel(3).asIntStream().skip(10).toArray();
            Arrays.sort(expectedFromBytes);
            Arrays.sort(expectedFromChars);
            Arrays.sort(actualFromBytes);
            Arrays.sort(actualFromChars);
            assertArrayEquals(expectedFromBytes, actualFromBytes);
            assertArrayEquals(expectedFromChars, actualFromChars);

            assertEquals(1990, ByteStream.of(bytes).parallel(3).asIntStream().skip(10).count());
            assertEquals(332, ByteStream.of(bytes).parallel(3).step(3).skip(335).count());
            assertEquals(332, CharStream.of(chars).parallel(3).step(3).skip(335).count());

            final double[] expectedDoubles = new double[400];

            for (int i = 0; i < 400; i++) {
                expectedDoubles[i] = doubles[1200 + 2 * i];
            }

            final double[] actualDoubles = DoubleStream.of(doubles).parallel(3).step(2).skip(600).toArray();
            Arrays.sort(actualDoubles);
            assertArrayEquals(expectedDoubles, actualDoubles);
            assertEquals(400, DoubleStream.of(doubles).parallel(3).step(2).skip(600).count());
        }

        // G087-01 (guard, RED on base): the step()/asIntStream() iterators opt into bulk advance, so skip()/limit() skip in O(1)
        @Test
        void testStepAndAsIntStream_optIntoBulkAdvance() {
            final byte[] bytes = { 1, 2, 3, 4, 5 };
            final char[] chars = { 'a', 'b', 'c', 'd', 'e' };
            final double[] doubles = { 1, 2, 3, 4, 5 };

            assertTrue(ByteStream.of(bytes).step(2).iteratorEx().supportsFailureAtomicAdvance());
            assertTrue(ByteStream.of(bytes).asIntStream().iteratorEx().supportsFailureAtomicAdvance());
            assertTrue(ByteStream.of(bytes).asIntStream().skip(1).limit(2).iteratorEx().supportsFailureAtomicAdvance());
            assertTrue(CharStream.of(chars).step(2).iteratorEx().supportsFailureAtomicAdvance());
            assertTrue(CharStream.of(chars).asIntStream().iteratorEx().supportsFailureAtomicAdvance());
            assertTrue(DoubleStream.of(doubles).step(2).iteratorEx().supportsFailureAtomicAdvance());
            assertTrue(DoubleStream.of(doubles).step(2).skip(1).iteratorEx().supportsFailureAtomicAdvance());
        }

        private List<double[]> sumInputs() {
            final List<double[]> inputs = new ArrayList<>();
            inputs.add(new double[0]);
            inputs.add(new double[] { -0.0d });
            inputs.add(new double[] { 0.0d, -0.0d });
            inputs.add(new double[] { Double.NaN });
            inputs.add(new double[] { 1.0d, Double.NaN, 3.0d });
            inputs.add(new double[] { Double.POSITIVE_INFINITY, 1.0d });
            inputs.add(new double[] { Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY });
            inputs.add(new double[] { Double.MAX_VALUE, Double.MAX_VALUE, -Double.MAX_VALUE });
            inputs.add(new double[] { Double.MAX_VALUE, -Double.MAX_VALUE, Double.MAX_VALUE });
            inputs.add(new double[] { Double.MAX_VALUE, Double.MAX_VALUE, 1.0d, Double.NaN });
            inputs.add(new double[] { Double.MIN_VALUE, Double.MIN_VALUE, -Double.MIN_VALUE });
            inputs.add(new double[] { 1.0e16, 1.0d, -1.0e16, 0.1d, 0.2d, 0.3d });

            final Random random = new Random(20261008L);

            for (int i = 0; i < 300; i++) {
                final double[] a = new double[random.nextInt(300)];

                for (int j = 0; j < a.length; j++) {
                    final int kind = random.nextInt(10);

                    if (kind < 4) {
                        a[j] = random.nextDouble();
                    } else if (kind < 6) {
                        a[j] = (random.nextDouble() - 0.5d) * 1.0e300;
                    } else if (kind < 7) {
                        a[j] = (random.nextDouble() - 0.5d) * Double.MAX_VALUE;
                    } else if (kind < 9) {
                        a[j] = random.nextInt(100) - 50;
                    } else {
                        a[j] = Double.longBitsToDouble(random.nextLong()); // any bit pattern: subnormal, huge, infinite, NaN
                    }
                }

                inputs.add(a);
            }

            final double[] big = new double[100_000];

            for (int j = 0; j < big.length; j++) {
                big[j] = (random.nextDouble() - 0.5d) * 1000d;
            }

            inputs.add(big);

            return inputs;
        }

        private com.landawn.abacus.util.KahanSummation reference(final double[] a, final int fromIndex, final int toIndex) {
            final com.landawn.abacus.util.KahanSummation summation = new com.landawn.abacus.util.KahanSummation();

            for (int i = fromIndex; i < toIndex; i++) {
                summation.add(a[i]);
            }

            return summation;
        }

        // doubleToLongBits (not raw): the sign/payload of a NaN produced by arithmetic (e.g. Inf + -Inf) depends on whether the code
        // ran interpreted or JIT-compiled (x86 default NaN is 0xFFF8...), even for the pristine classes; -0.0 vs 0.0 is still checked
        private void assertSameSum(final double expected, final double actual, final String message) {
            assertEquals(Double.doubleToLongBits(expected), Double.doubleToLongBits(actual), message);
        }

        private void assertSameAverage(final com.landawn.abacus.util.u.OptionalDouble expected, final com.landawn.abacus.util.u.OptionalDouble actual,
                final String message) {
            assertEquals(expected.isPresent(), actual.isPresent(), message);

            if (expected.isPresent()) {
                assertEquals(Double.doubleToLongBits(expected.getAsDouble()), Double.doubleToLongBits(actual.getAsDouble()), message);
            }
        }

        // G087-07: ArrayDoubleStream.sum()/average() read the backing array directly; results stay bit-for-bit those of the
        // iterator-based Kahan summation (same KahanSummation adds in the same order) for whole arrays, sub-ranges and parallel
        @Test
        void testSumAverage_arrayBackedMatchesIteratorPathBitForBit() {
            int index = 0;

            for (final double[] a : sumInputs()) {
                final String message = "input #" + index++ + " length " + a.length;
                final com.landawn.abacus.util.KahanSummation whole = reference(a, 0, a.length);

                assertSameSum(whole.sum(), DoubleStream.of(a).sum(), message);
                assertSameAverage(whole.average(), DoubleStream.of(a).average(), message);

                // iterator-based implementation (IteratorDoubleStream inherits AbstractDoubleStream.sum()/average())
                assertSameSum(whole.sum(), DoubleStream.of(a).map(x -> x).sum(), message);
                assertSameAverage(whole.average(), DoubleStream.of(a).map(x -> x).average(), message);

                // ParallelArrayDoubleStream inherits the array-backed implementation; sum()/average() stay sequential
                assertSameSum(whole.sum(), DoubleStream.of(a).parallel().sum(), message);
                assertSameAverage(whole.average(), DoubleStream.of(a).parallel().average(), message);

                if (a.length >= 2) {
                    final int fromIndex = 1;
                    final int toIndex = a.length - 1;
                    final com.landawn.abacus.util.KahanSummation range = reference(a, fromIndex, toIndex);

                    assertSameSum(range.sum(), DoubleStream.of(a, fromIndex, toIndex).sum(), message);
                    assertSameAverage(range.average(), DoubleStream.of(a, fromIndex, toIndex).average(), message);
                    assertSameSum(range.sum(), DoubleStream.of(a).skip(fromIndex).limit(toIndex - fromIndex).sum(), message);
                    assertSameAverage(range.average(), DoubleStream.of(a).skip(fromIndex).limit(toIndex - fromIndex).average(), message);
                    assertSameSum(range.sum(), DoubleStream.of(a, fromIndex, toIndex).parallel(2).sum(), message);
                }
            }

            assertSameSum(0.0d, DoubleStream.empty().sum(), "empty");
            assertFalse(DoubleStream.empty().average().isPresent());
            assertSameSum(0.0d, DoubleStream.of(new double[] { 1d, 2d, 3d }, 2, 2).sum(), "empty range");
            assertFalse(DoubleStream.of(new double[] { 1d, 2d, 3d }, 1, 1).average().isPresent());
            assertTrue(Double.isNaN(DoubleStream.of(1.0d, Double.NaN, 3.0d).sum()));
            assertEquals(Double.POSITIVE_INFINITY, DoubleStream.of(1.0d, Double.POSITIVE_INFINITY).average().getAsDouble());
            assertEquals(12.0d, DoubleStream.of(1.5d, 2.5d, 3.5d, 4.5d).sum());
            assertEquals(2.5d, DoubleStream.of(new double[] { 9d, 1.5d, 2.5d, 3.5d, 9d }, 1, 4).average().getAsDouble());
        }

        // G087-07: close behaviour of the overridden terminal operations is unchanged: the stream is closed exactly once, cannot be
        // reused, and a failing close handler still surfaces from the terminal operation
        @Test
        void testSumAverage_closeSemanticsUnchanged() {
            final AtomicInteger closed = new AtomicInteger();

            final DoubleStream s1 = DoubleStream.of(1d, 2d, 3d).onClose(closed::incrementAndGet);
            assertEquals(6.0d, s1.sum());
            assertEquals(1, closed.get());
            assertThrows(IllegalStateException.class, s1::sum);
            assertThrows(IllegalStateException.class, s1::average);
            assertEquals(1, closed.get());

            final DoubleStream s2 = DoubleStream.of(new double[] { 1d, 2d, 3d, 4d }, 1, 3).onClose(closed::incrementAndGet);
            final com.landawn.abacus.util.u.OptionalDouble average = s2.average();
            assertTrue(average.isPresent());
            assertEquals(2.5d, average.getAsDouble());
            assertEquals(2, closed.get());
            assertThrows(IllegalStateException.class, s2::average);
            assertThrows(IllegalStateException.class, s2::sum);

            final DoubleStream s3 = DoubleStream.empty().onClose(closed::incrementAndGet);
            assertFalse(s3.average().isPresent());
            assertEquals(3, closed.get());

            final DoubleStream s4 = DoubleStream.of(5d).parallel().onClose(closed::incrementAndGet);
            assertEquals(5.0d, s4.sum());
            assertEquals(4, closed.get());
            assertThrows(IllegalStateException.class, s4::sum);

            final DoubleStream parent = DoubleStream.of(1d, 2d, 3d, 4d).onClose(closed::incrementAndGet);
            assertEquals(5.0d, parent.skip(1).limit(2).sum());
            assertEquals(5, closed.get());
            assertThrows(IllegalStateException.class, parent::sum);

            final RuntimeException failure = new RuntimeException("close failure");
            final DoubleStream s5 = DoubleStream.of(1d, 2d).onClose(() -> {
                throw failure;
            });
            assertSame(failure, assertThrows(RuntimeException.class, s5::sum));
            assertThrows(IllegalStateException.class, s5::sum);

            final DoubleStream s6 = DoubleStream.of(1d, 2d).parallel().onClose(() -> {
                throw failure;
            });
            assertSame(failure, assertThrows(RuntimeException.class, s6::average));
        }
    }

    // ---- perf review 2026-10-08 G087 end ----
    // ---- perf review 2026-10-08 G088 begin ----
    @Nested
    class G088 {

        private List<float[]> sumInputs() {
            final List<float[]> inputs = new ArrayList<>();
            inputs.add(new float[0]);
            inputs.add(new float[] { 1.5f });
            inputs.add(new float[] { -0.0f });
            inputs.add(new float[] { -0.0f, -0.0f });
            inputs.add(new float[] { 0.0f, -0.0f });
            inputs.add(new float[] { -0.0f, 0.0f });
            inputs.add(new float[] { Float.NaN });
            inputs.add(new float[] { 1.0f, Float.NaN, 3.0f });
            inputs.add(new float[] { Float.POSITIVE_INFINITY, 1.0f });
            inputs.add(new float[] { Float.NEGATIVE_INFINITY, 2.0f });
            inputs.add(new float[] { Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY });
            inputs.add(new float[] { Float.MAX_VALUE, Float.MAX_VALUE, -Float.MAX_VALUE, Float.MAX_VALUE });
            inputs.add(new float[] { Float.MIN_VALUE, Float.MIN_VALUE, -Float.MIN_NORMAL });
            inputs.add(new float[] { 1.0e8f, 1.0f, -1.0e8f, 1.0f });
            inputs.add(new float[] { 16777216f, 1f, 1f, 1f, -16777216f });
            inputs.add(new float[] { 0.1f, 0.2f, 0.3f });

            final Random rnd = new Random(20261008L);

            for (int i = 0; i < 300; i++) {
                final float[] a = new float[rnd.nextInt(300)];

                for (int j = 0; j < a.length; j++) {
                    final int kind = rnd.nextInt(10);

                    if (kind < 4) {
                        a[j] = rnd.nextFloat();
                    } else if (kind < 7) {
                        a[j] = (rnd.nextFloat() - 0.5f) * 1.0e30f;
                    } else if (kind < 9) {
                        a[j] = rnd.nextInt(100) - 50;
                    } else {
                        a[j] = Float.intBitsToFloat(rnd.nextInt()); // any bit pattern: subnormal, huge, infinite, NaN
                    }
                }

                inputs.add(a);
            }

            final float[] big = new float[100_000];

            for (int j = 0; j < big.length; j++) {
                big[j] = (rnd.nextFloat() - 0.5f) * 1000f;
            }

            inputs.add(big);

            return inputs;
        }

        private com.landawn.abacus.util.KahanSummation reference(final float[] a, final int fromIndex, final int toIndex) {
            final com.landawn.abacus.util.KahanSummation summation = new com.landawn.abacus.util.KahanSummation();

            for (int i = fromIndex; i < toIndex; i++) {
                summation.add(a[i]);
            }

            return summation;
        }

        private void assertSameSum(final double expected, final double actual, final String message) {
            assertEquals(Double.doubleToLongBits(expected), Double.doubleToLongBits(actual), message);
        }

        private void assertSameAverage(final com.landawn.abacus.util.u.OptionalDouble expected, final com.landawn.abacus.util.u.OptionalDouble actual,
                final String message) {
            assertEquals(expected.isPresent(), actual.isPresent(), message);

            if (expected.isPresent()) {
                assertEquals(Double.doubleToLongBits(expected.getAsDouble()), Double.doubleToLongBits(actual.getAsDouble()), message);
            }
        }

        // G088-01: ArrayFloatStream.sum()/average() read the backing array directly; results must stay bit-for-bit those of the
        // iterator-based Kahan summation (same KahanSummation adds in the same order), for whole arrays, sub-ranges and parallel.
        @Test
        public void testSumAverage_arrayBackedMatchesIteratorPathBitForBit() {
            int index = 0;

            for (final float[] a : sumInputs()) {
                final String message = "input #" + index++ + " length " + a.length;
                final com.landawn.abacus.util.KahanSummation whole = reference(a, 0, a.length);

                assertSameSum(whole.sum(), FloatStream.of(a).sum(), message);
                assertSameAverage(whole.average(), FloatStream.of(a).average(), message);

                // iterator-based implementation (IteratorFloatStream inherits AbstractFloatStream.sum()/average())
                assertSameSum(whole.sum(), FloatStream.of(a).map(x -> x).sum(), message);
                assertSameAverage(whole.average(), FloatStream.of(a).map(x -> x).average(), message);

                // ParallelArrayFloatStream inherits the array-backed implementation; sum()/average() are sequential-only
                assertSameSum(whole.sum(), FloatStream.of(a).parallel().sum(), message);
                assertSameAverage(whole.average(), FloatStream.of(a).parallel().average(), message);

                if (a.length >= 2) {
                    final int fromIndex = 1;
                    final int toIndex = a.length - 1;
                    final com.landawn.abacus.util.KahanSummation range = reference(a, fromIndex, toIndex);

                    assertSameSum(range.sum(), FloatStream.of(a, fromIndex, toIndex).sum(), message);
                    assertSameAverage(range.average(), FloatStream.of(a, fromIndex, toIndex).average(), message);
                    assertSameSum(range.sum(), FloatStream.of(a).skip(fromIndex).limit(toIndex - fromIndex).sum(), message);
                    assertSameAverage(range.average(), FloatStream.of(a).skip(fromIndex).limit(toIndex - fromIndex).average(), message);
                }
            }

            assertSameSum(0.0d, FloatStream.empty().sum(), "empty");
            assertFalse(FloatStream.empty().average().isPresent());
            assertSameSum(0.0d, FloatStream.of(new float[] { 1f, 2f, 3f }, 2, 2).sum(), "empty range");
            assertSameSum(0.0d, FloatStream.of(-0.0f).sum(), "-0.0 sums to +0.0");
            assertEquals(Double.NaN, FloatStream.of(1.0f, Float.NaN, 3.0f).sum());
            assertEquals(Double.POSITIVE_INFINITY, FloatStream.of(1.0f, Float.POSITIVE_INFINITY).average().getAsDouble());
            assertEquals(12.0d, FloatStream.of(1.5f, 2.5f, 3.5f, 4.5f).sum());
        }

        // G088-01: close behaviour of the overridden terminal operations is unchanged: the stream is closed exactly once, cannot be
        // reused, and a failing close handler still surfaces from the terminal operation.
        @Test
        public void testSumAverage_closeSemanticsUnchanged() {
            final AtomicInteger closed = new AtomicInteger();

            final FloatStream s1 = FloatStream.of(1f, 2f, 3f).onClose(closed::incrementAndGet);
            assertEquals(6.0d, s1.sum());
            assertEquals(1, closed.get());
            assertThrows(IllegalStateException.class, s1::sum);
            assertThrows(IllegalStateException.class, s1::average);
            assertEquals(1, closed.get());

            final FloatStream s2 = FloatStream.of(new float[] { 1f, 2f, 3f, 4f }, 1, 3).onClose(closed::incrementAndGet);
            final com.landawn.abacus.util.u.OptionalDouble average = s2.average();
            assertTrue(average.isPresent());
            assertEquals(2.5d, average.getAsDouble());
            assertEquals(2, closed.get());
            assertThrows(IllegalStateException.class, s2::average);
            assertThrows(IllegalStateException.class, s2::sum);

            final FloatStream s3 = FloatStream.empty().onClose(closed::incrementAndGet);
            assertFalse(s3.average().isPresent());
            assertEquals(3, closed.get());

            final FloatStream s4 = FloatStream.of(5f).parallel().onClose(closed::incrementAndGet);
            assertEquals(5.0d, s4.sum());
            assertEquals(4, closed.get());
            assertThrows(IllegalStateException.class, s4::sum);

            final RuntimeException failure = new RuntimeException("close failure");
            final FloatStream s5 = FloatStream.of(1f, 2f).onClose(() -> {
                throw failure;
            });
            assertSame(failure, assertThrows(RuntimeException.class, s5::sum));
            assertThrows(IllegalStateException.class, s5::sum);

            final FloatStream s6 = FloatStream.of(1f, 2f).parallel().onClose(() -> {
                throw failure;
            });
            assertSame(failure, assertThrows(RuntimeException.class, s6::average));
        }

        private static final int OP_STEP = 0;
        private static final int OP_SKIP = 1;
        private static final int OP_LIMIT = 2;

        private final long[] interestingCounts = { 0, 1, 2, 3, 5, 7, 31, 64, Integer.MAX_VALUE, Integer.MAX_VALUE + 1L, Long.MAX_VALUE };

        private long randomCount(final Random random, final int size) {
            return random.nextInt(4) == 0 ? interestingCounts[random.nextInt(interestingCounts.length)] : random.nextInt(size + 4);
        }

        /** A skip, then 0-3 random step/skip/limit operations, with an extra skip inserted at a random position half of the time. */
        private List<long[]> randomOps(final Random random, final int size) {
            final List<long[]> ops = new ArrayList<>();
            ops.add(new long[] { OP_SKIP, randomCount(random, size) });
            final int opCount = random.nextInt(4);

            for (int i = 0; i < opCount; i++) {
                final int kind = random.nextInt(3);
                ops.add(new long[] { kind, kind == OP_STEP ? 1 + random.nextInt(5) : randomCount(random, size) });
            }

            if (random.nextBoolean()) {
                ops.add(random.nextInt(ops.size() + 1), new long[] { OP_SKIP, randomCount(random, size) });
            }

            return ops;
        }

        private List<Integer> oracle(final int fromIndex, final int toIndex, final List<long[]> ops) {
            List<Integer> cur = new ArrayList<>();

            for (int i = fromIndex; i < toIndex; i++) {
                cur.add(i);
            }

            for (final long[] op : ops) {
                final List<Integer> next = new ArrayList<>();

                if (op[0] == OP_STEP) {
                    for (long i = 0; i < cur.size(); i += op[1]) {
                        next.add(cur.get((int) i));
                    }
                } else if (op[0] == OP_SKIP) {
                    next.addAll(cur.subList((int) Math.min(op[1], cur.size()), cur.size()));
                } else {
                    next.addAll(cur.subList(0, (int) Math.min(op[1], cur.size())));
                }

                cur = next;
            }

            return cur;
        }

        private IntStream applyInt(IntStream s, final List<long[]> ops) {
            for (final long[] op : ops) {
                s = op[0] == OP_STEP ? s.step(op[1]) : op[0] == OP_SKIP ? s.skip(op[1]) : s.limit(op[1]);
            }

            return s;
        }

        private LongStream applyLong(LongStream s, final List<long[]> ops) {
            for (final long[] op : ops) {
                s = op[0] == OP_STEP ? s.step(op[1]) : op[0] == OP_SKIP ? s.skip(op[1]) : s.limit(op[1]);
            }

            return s;
        }

        private FloatStream applyFloat(FloatStream s, final List<long[]> ops) {
            for (final long[] op : ops) {
                s = op[0] == OP_STEP ? s.step(op[1]) : op[0] == OP_SKIP ? s.skip(op[1]) : s.limit(op[1]);
            }

            return s;
        }

        private DoubleStream applyDouble(DoubleStream s, final List<long[]> ops) {
            for (final long[] op : ops) {
                s = op[0] == OP_STEP ? s.step(op[1]) : op[0] == OP_SKIP ? s.skip(op[1]) : s.limit(op[1]);
            }

            return s;
        }

        private String describe(final int length, final int fromIndex, final int toIndex, final String head, final List<long[]> ops) {
            final StringBuilder sb = new StringBuilder("len=" + length + " range=[" + fromIndex + "," + toIndex + ") " + head);

            for (final long[] op : ops) {
                sb.append(op[0] == OP_STEP ? ".step(" : op[0] == OP_SKIP ? ".skip(" : ".limit(").append(op[1]).append(')');
            }

            return sb.toString();
        }

        // G088-02: step(k) on int/long/float array streams followed by random skip/limit/step chains matches an index oracle
        // (toArray, count, elementAt and the iterator protocol incl. a repeated hasNext() and NoSuchElementException at the end).
        @Test
        public void testStepSkip_matchesOracle() {
            final Random random = new Random(20261008L);

            for (int round = 0; round < 2500; round++) {
                final int length = random.nextInt(4) == 0 ? 0 : random.nextInt(40);
                final int fromIndex = length == 0 ? 0 : random.nextInt(length + 1);
                final int toIndex = fromIndex + (length == fromIndex ? 0 : random.nextInt(length - fromIndex + 1));
                final long step = random.nextInt(8) == 0 ? (random.nextBoolean() ? Integer.MAX_VALUE + 7L : Long.MAX_VALUE) : 2 + random.nextInt(6);
                final List<long[]> ops = randomOps(random, length);
                ops.add(0, new long[] { OP_STEP, step });

                final List<Integer> expected = oracle(fromIndex, toIndex, ops);
                final String msg = describe(length, fromIndex, toIndex, "step-first", ops);

                final int[] ints = new int[length];
                final long[] longs = new long[length];
                final float[] floats = new float[length];

                for (int i = 0; i < length; i++) {
                    ints[i] = random.nextInt();
                    longs[i] = random.nextLong();
                    floats[i] = random.nextInt(10) == 0 ? (random.nextBoolean() ? Float.NaN : -0.0f) : (float) random.nextGaussian();
                }

                final int[] expectedInts = new int[expected.size()];
                final long[] expectedLongs = new long[expected.size()];
                final float[] expectedFloats = new float[expected.size()];

                for (int i = 0; i < expected.size(); i++) {
                    expectedInts[i] = ints[expected.get(i)];
                    expectedLongs[i] = longs[expected.get(i)];
                    expectedFloats[i] = floats[expected.get(i)];
                }

                assertArrayEquals(expectedInts, applyInt(IntStream.of(ints, fromIndex, toIndex), ops).toArray(), msg);
                assertEquals(expected.size(), applyInt(IntStream.of(ints, fromIndex, toIndex), ops).count(), msg);
                assertArrayEquals(expectedLongs, applyLong(LongStream.of(longs, fromIndex, toIndex), ops).toArray(), msg);
                assertEquals(expected.size(), applyLong(LongStream.of(longs, fromIndex, toIndex), ops).count(), msg);
                assertArrayEquals(expectedFloats, applyFloat(FloatStream.of(floats, fromIndex, toIndex), ops).toArray(), msg);
                assertEquals(expected.size(), applyFloat(FloatStream.of(floats, fromIndex, toIndex), ops).count(), msg);

                final int position = random.nextInt(expected.size() + 2);
                final com.landawn.abacus.util.u.OptionalInt intAt = applyInt(IntStream.of(ints, fromIndex, toIndex), ops).elementAt(position);
                final com.landawn.abacus.util.u.OptionalFloat floatAt = applyFloat(FloatStream.of(floats, fromIndex, toIndex), ops).elementAt(position);
                assertEquals(position < expected.size(), intAt.isPresent(), msg);
                assertEquals(position < expected.size(), floatAt.isPresent(), msg);

                if (position < expected.size()) {
                    assertEquals(expectedInts[position], intAt.get(), msg);
                    assertEquals(Float.floatToIntBits(expectedFloats[position]), Float.floatToIntBits(floatAt.get()), msg);
                }

                final IntIteratorEx intIter = applyInt(IntStream.of(ints, fromIndex, toIndex), ops).iteratorEx();
                final LongIteratorEx longIter = applyLong(LongStream.of(longs, fromIndex, toIndex), ops).iteratorEx();
                final FloatIteratorEx floatIter = applyFloat(FloatStream.of(floats, fromIndex, toIndex), ops).iteratorEx();

                for (int i = 0; i < expected.size(); i++) {
                    assertTrue(intIter.hasNext(), msg);
                    assertTrue(intIter.hasNext(), msg);
                    assertEquals(expectedInts[i], intIter.nextInt(), msg);
                    assertEquals(expectedLongs[i], longIter.nextLong(), msg);
                    assertEquals(Float.floatToRawIntBits(expectedFloats[i]), Float.floatToRawIntBits(floatIter.nextFloat()), msg);
                }

                assertFalse(intIter.hasNext(), msg);
                assertFalse(longIter.hasNext(), msg);
                assertFalse(floatIter.hasNext(), msg);
                assertThrows(java.util.NoSuchElementException.class, intIter::nextInt, msg);
                assertThrows(java.util.NoSuchElementException.class, longIter::nextLong, msg);
                assertThrows(java.util.NoSuchElementException.class, floatIter::nextFloat, msg);
            }
        }

        // G088-02: the widening asXxxStream() views of int/long/float array streams followed by random skip/limit/step chains match an
        // index oracle (toArray, count, sum, elementAt and the iterator protocol).
        @Test
        public void testAsXxxStreamSkip_matchesOracle() {
            final Random random = new Random(8102601L);

            for (int round = 0; round < 2500; round++) {
                final int length = random.nextInt(4) == 0 ? 0 : random.nextInt(40);
                final int fromIndex = length == 0 ? 0 : random.nextInt(length + 1);
                final int toIndex = fromIndex + (length == fromIndex ? 0 : random.nextInt(length - fromIndex + 1));
                final List<long[]> ops = randomOps(random, length);

                final List<Integer> expected = oracle(fromIndex, toIndex, ops);
                final String msg = describe(length, fromIndex, toIndex, "asXxxStream()", ops);

                final int[] ints = new int[length];
                final long[] longs = new long[length];
                final float[] floats = new float[length];

                for (int i = 0; i < length; i++) {
                    ints[i] = random.nextInt();
                    longs[i] = random.nextLong();
                    floats[i] = random.nextInt(10) == 0 ? (random.nextBoolean() ? Float.NaN : -0.0f) : (float) random.nextGaussian();
                }

                final int size = expected.size();
                final long[] intToLong = new long[size];
                final float[] intToFloat = new float[size];
                final double[] intToDouble = new double[size];
                final float[] longToFloat = new float[size];
                final double[] longToDouble = new double[size];
                final double[] floatToDouble = new double[size];
                long intToLongSum = 0;

                for (int i = 0; i < size; i++) {
                    final int k = expected.get(i);
                    intToLong[i] = ints[k];
                    intToFloat[i] = ints[k];
                    intToDouble[i] = ints[k];
                    longToFloat[i] = longs[k];
                    longToDouble[i] = longs[k];
                    floatToDouble[i] = floats[k];
                    intToLongSum += ints[k];
                }

                assertArrayEquals(intToLong, applyLong(IntStream.of(ints, fromIndex, toIndex).asLongStream(), ops).toArray(), msg);
                assertEquals(size, applyLong(IntStream.of(ints, fromIndex, toIndex).asLongStream(), ops).count(), msg);
                assertEquals(intToLongSum, applyLong(IntStream.of(ints, fromIndex, toIndex).asLongStream(), ops).sum(), msg);
                assertArrayEquals(intToFloat, applyFloat(IntStream.of(ints, fromIndex, toIndex).asFloatStream(), ops).toArray(), msg);
                assertEquals(size, applyFloat(IntStream.of(ints, fromIndex, toIndex).asFloatStream(), ops).count(), msg);
                assertArrayEquals(intToDouble, applyDouble(IntStream.of(ints, fromIndex, toIndex).asDoubleStream(), ops).toArray(), msg);
                assertEquals(size, applyDouble(IntStream.of(ints, fromIndex, toIndex).asDoubleStream(), ops).count(), msg);
                assertArrayEquals(longToFloat, applyFloat(LongStream.of(longs, fromIndex, toIndex).asFloatStream(), ops).toArray(), msg);
                assertEquals(size, applyFloat(LongStream.of(longs, fromIndex, toIndex).asFloatStream(), ops).count(), msg);
                assertArrayEquals(longToDouble, applyDouble(LongStream.of(longs, fromIndex, toIndex).asDoubleStream(), ops).toArray(), msg);
                assertEquals(size, applyDouble(LongStream.of(longs, fromIndex, toIndex).asDoubleStream(), ops).count(), msg);
                assertArrayEquals(floatToDouble, applyDouble(FloatStream.of(floats, fromIndex, toIndex).asDoubleStream(), ops).toArray(), msg);
                assertEquals(size, applyDouble(FloatStream.of(floats, fromIndex, toIndex).asDoubleStream(), ops).count(), msg);

                final int position = random.nextInt(size + 2);
                final com.landawn.abacus.util.u.OptionalLong longAt = applyLong(IntStream.of(ints, fromIndex, toIndex).asLongStream(), ops).elementAt(position);
                final com.landawn.abacus.util.u.OptionalDouble doubleAt = applyDouble(LongStream.of(longs, fromIndex, toIndex).asDoubleStream(), ops)
                        .elementAt(position);
                assertEquals(position < size, longAt.isPresent(), msg);
                assertEquals(position < size, doubleAt.isPresent(), msg);

                if (position < size) {
                    assertEquals(intToLong[position], longAt.get(), msg);
                    assertEquals(Double.doubleToLongBits(longToDouble[position]), Double.doubleToLongBits(doubleAt.get()), msg);
                }

                final LongIteratorEx longIter = applyLong(IntStream.of(ints, fromIndex, toIndex).asLongStream(), ops).iteratorEx();
                final FloatIteratorEx floatIter = applyFloat(LongStream.of(longs, fromIndex, toIndex).asFloatStream(), ops).iteratorEx();
                final DoubleIteratorEx doubleIter = applyDouble(FloatStream.of(floats, fromIndex, toIndex).asDoubleStream(), ops).iteratorEx();

                for (int i = 0; i < size; i++) {
                    assertTrue(longIter.hasNext(), msg);
                    assertTrue(longIter.hasNext(), msg);
                    assertEquals(intToLong[i], longIter.nextLong(), msg);
                    assertEquals(Float.floatToIntBits(longToFloat[i]), Float.floatToIntBits(floatIter.nextFloat()), msg);
                    assertTrue(doubleIter.hasNext(), msg);
                    assertEquals(Double.doubleToLongBits(floatToDouble[i]), Double.doubleToLongBits(doubleIter.nextDouble()), msg);
                }

                assertFalse(longIter.hasNext(), msg);
                assertFalse(floatIter.hasNext(), msg);
                assertFalse(doubleIter.hasNext(), msg);
                assertThrows(java.util.NoSuchElementException.class, longIter::nextLong, msg);
                assertThrows(java.util.NoSuchElementException.class, floatIter::nextFloat, msg);
                assertThrows(java.util.NoSuchElementException.class, doubleIter::nextDouble, msg);
            }
        }

        // G088-02: hand-picked boundaries of skip/limit/elementAt after step()/asXxxStream(): exact remaining count, one past it, huge
        // counts, sub-ranges, empty streams.
        @Test
        public void testStepAsXxxStreamSkip_boundaries() {
            final int[] ints = { 10, -20, 30, -40, 50, -60, 70 };
            final long[] longs = { 10L, Long.MIN_VALUE, 30L, Long.MAX_VALUE, 50L, -60L, 70L };
            final float[] floats = { 1.5f, Float.NaN, -0.0f, 0.0f, Float.NEGATIVE_INFINITY, 6.5f, 7.5f };

            assertArrayEquals(new int[] { 50, 70 }, IntStream.of(ints).step(2).skip(2).toArray());
            assertArrayEquals(new int[] { 70 }, IntStream.of(ints).step(2).skip(3).toArray());
            assertArrayEquals(new int[0], IntStream.of(ints).step(2).skip(4).toArray());
            assertArrayEquals(new int[0], IntStream.of(ints).step(2).skip(Long.MAX_VALUE).toArray());
            assertArrayEquals(new int[] { -40 }, IntStream.of(ints, 1, 6).step(2).skip(1).limit(1).toArray());
            assertArrayEquals(new int[] { -20, 50 }, IntStream.of(ints, 1, 7).step(3).toArray());
            assertArrayEquals(new int[] { 50 }, IntStream.of(ints, 1, 7).step(3).skip(1).toArray());
            assertArrayEquals(new int[] { 10 }, IntStream.of(ints).step(Long.MAX_VALUE).toArray());
            assertArrayEquals(new int[0], IntStream.of(ints).step(Long.MAX_VALUE).skip(1).toArray());
            assertArrayEquals(new int[] { 70 }, IntStream.of(ints).step(3).skip(1).skip(1).toArray());
            assertEquals(1, IntStream.of(ints).step(2).skip(1).limit(2).skip(1).count());
            assertEquals(70, IntStream.of(ints).step(2).elementAt(3).get());
            assertFalse(IntStream.of(ints).step(2).elementAt(4).isPresent());
            assertArrayEquals(new int[0], IntStream.empty().step(2).skip(1).toArray());

            assertArrayEquals(new long[] { 50L, 70L }, LongStream.of(longs).step(2).skip(2).toArray());
            assertArrayEquals(new long[0], LongStream.of(longs).step(2).skip(4).toArray());
            assertArrayEquals(new long[] { Long.MIN_VALUE, 50L }, LongStream.of(longs, 1, 7).step(3).toArray());
            assertEquals(2, LongStream.of(longs).step(3).skip(1).count());
            assertEquals(70L, LongStream.of(longs).step(3).elementAt(2).get());

            assertArrayEquals(new float[] { Float.NEGATIVE_INFINITY, 7.5f }, FloatStream.of(floats).step(2).skip(2).toArray());
            assertArrayEquals(new float[] { -0.0f, Float.NEGATIVE_INFINITY }, FloatStream.of(floats).step(2).skip(1).limit(2).toArray());
            assertArrayEquals(new float[0], FloatStream.of(floats).step(2).skip(4).toArray());
            assertArrayEquals(new float[] { Float.NaN, Float.NEGATIVE_INFINITY }, FloatStream.of(floats, 1, 7).step(3).toArray());
            assertEquals(0, FloatStream.of(floats).step(3).skip(Long.MAX_VALUE).count());

            assertArrayEquals(new long[] { 50L, -60L, 70L }, IntStream.of(ints).asLongStream().skip(4).toArray());
            assertArrayEquals(new long[] { 70L }, IntStream.of(ints).asLongStream().skip(6).toArray());
            assertArrayEquals(new long[0], IntStream.of(ints).asLongStream().skip(7).toArray());
            assertArrayEquals(new long[0], IntStream.of(ints).asLongStream().skip(Long.MAX_VALUE).toArray());
            assertArrayEquals(new float[] { -40f, 50f }, IntStream.of(ints, 2, 5).asFloatStream().skip(1).toArray());
            assertArrayEquals(new double[] { -20d, -60d }, IntStream.of(ints, 1, 7).asDoubleStream().step(4).toArray());
            assertArrayEquals(new long[] { 50L }, IntStream.of(ints).asLongStream().skip(1).step(3).skip(1).limit(1).toArray());
            assertEquals(0, IntStream.of(ints).asDoubleStream().limit(3).skip(5).count());
            assertEquals(-40L + 50 - 60 + 70, IntStream.of(ints).asLongStream().skip(3).sum());
            assertEquals(-60d, IntStream.of(ints).asDoubleStream().elementAt(5).get());
            assertFalse(IntStream.of(ints).asFloatStream().elementAt(7).isPresent());

            assertArrayEquals(new float[] { (float) Long.MAX_VALUE, 50f }, LongStream.of(longs).asFloatStream().skip(3).limit(2).toArray());
            assertArrayEquals(new double[] { 70d }, LongStream.of(longs).asDoubleStream().skip(6).toArray());
            assertArrayEquals(new double[0], LongStream.of(longs, 3, 3).asDoubleStream().skip(1).toArray());
            assertEquals(2, LongStream.of(longs).asFloatStream().skip(5).count());

            assertArrayEquals(new double[] { 0.0d, Double.NEGATIVE_INFINITY, 6.5d, 7.5d }, FloatStream.of(floats).asDoubleStream().skip(3).toArray());
            assertArrayEquals(new double[] { Double.NaN }, FloatStream.of(floats).asDoubleStream().skip(1).limit(1).toArray());
            assertEquals(Double.doubleToLongBits(-0.0d), Double.doubleToLongBits(FloatStream.of(floats).asDoubleStream().elementAt(2).get()));
            assertEquals(0, FloatStream.of(floats).asDoubleStream().skip(8).count());
        }

        // G088-02: parallel array streams inherit step()/asXxxStream(); skip after them still yields the same elements.
        @Test
        public void testStepAsXxxStreamSkip_parallel() {
            final int[] ints = new int[2000];
            final long[] longs = new long[2000];
            final float[] floats = new float[2000];

            for (int i = 0; i < ints.length; i++) {
                ints[i] = i * 7;
                longs[i] = i * 37L;
                floats[i] = i * 0.25f;
            }

            final long[] expectedLongs = new long[1990];

            for (int i = 0; i < 1990; i++) {
                expectedLongs[i] = ints[i + 10];
            }

            final long[] actualLongs = IntStream.of(ints).parallel(3).asLongStream().skip(10).toArray();
            Arrays.sort(actualLongs);
            assertArrayEquals(expectedLongs, actualLongs);
            assertEquals(1990, IntStream.of(ints).parallel(3).asLongStream().skip(10).count());
            assertEquals(1990, LongStream.of(longs).parallel(3).asDoubleStream().skip(10).count());
            assertEquals(1990, FloatStream.of(floats).parallel(3).asDoubleStream().skip(10).count());
            assertEquals(332, IntStream.of(ints).parallel(3).step(3).skip(335).count());
            assertEquals(332, LongStream.of(longs).parallel(3).step(3).skip(335).count());

            final float[] expectedFloats = new float[400];

            for (int i = 0; i < 400; i++) {
                expectedFloats[i] = floats[1200 + 2 * i];
            }

            final float[] actualFloats = FloatStream.of(floats).parallel(3).step(2).skip(600).toArray();
            Arrays.sort(actualFloats);
            assertArrayEquals(expectedFloats, actualFloats);
            assertEquals(400, FloatStream.of(floats).parallel(3).step(2).skip(600).count());
        }

        // G088-02 (guard, RED on base by design): the step()/asXxxStream() iterators of int/long/float array streams opt into bulk
        // advance, so a downstream skip()/limit()/elementAt() skips in O(1) instead of reading every skipped element.
        @Test
        public void testStepAndAsXxxStream_optIntoBulkAdvance() {
            final int[] ints = { 1, 2, 3, 4, 5 };
            final long[] longs = { 1, 2, 3, 4, 5 };
            final float[] floats = { 1, 2, 3, 4, 5 };

            assertTrue(IntStream.of(ints).step(2).iteratorEx().supportsFailureAtomicAdvance());
            assertTrue(IntStream.of(ints).asLongStream().iteratorEx().supportsFailureAtomicAdvance());
            assertTrue(IntStream.of(ints).asFloatStream().iteratorEx().supportsFailureAtomicAdvance());
            assertTrue(IntStream.of(ints).asDoubleStream().iteratorEx().supportsFailureAtomicAdvance());
            assertTrue(IntStream.of(ints).asLongStream().skip(1).limit(2).iteratorEx().supportsFailureAtomicAdvance());
            assertTrue(LongStream.of(longs).step(2).iteratorEx().supportsFailureAtomicAdvance());
            assertTrue(LongStream.of(longs).asFloatStream().iteratorEx().supportsFailureAtomicAdvance());
            assertTrue(LongStream.of(longs).asDoubleStream().iteratorEx().supportsFailureAtomicAdvance());
            assertTrue(FloatStream.of(floats).step(2).iteratorEx().supportsFailureAtomicAdvance());
            assertTrue(FloatStream.of(floats).asDoubleStream().iteratorEx().supportsFailureAtomicAdvance());
            assertTrue(FloatStream.of(floats).step(2).skip(1).iteratorEx().supportsFailureAtomicAdvance());
        }
    }

    // ---- perf review 2026-10-08 G088 end ----
    // ---- perf review 2026-10-08 G091 begin ----
    @Nested
    class G091 {
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

        // Built in a separate frame so that only the returned stream references the payload array.
        private ByteStream concatOverPrivateArray(final int n) {
            final byte[] payload = new byte[n];

            for (int i = 0; i < n; i++) {
                payload[i] = (byte) (i % 100);
            }

            payloadRef = new WeakReference<>(payload);

            return ByteStream.concat(Arrays.asList(ByteStream.of(payload), null, ByteStream.of((byte) -1, (byte) -2)));
        }

        // Built in a separate frame so that only the returned stream references the sorted() stream (and its sorted copy).
        private ByteStream sortedThenAppend(final int n) {
            final byte[] values = new byte[n];

            for (int i = 0; i < n; i++) {
                values[i] = (byte) (99 - i % 100);
            }

            final ByteStream sorted = ByteStream.of(values).sorted();
            payloadRef = new WeakReference<>(sorted);

            return sorted.append(ByteStream.of((byte) -1, (byte) -2));
        }

        // G091-01: an exhausted (and closed) input of concat(Collection) is no longer reachable from the concatenation while later inputs run
        @Test
        void testConcat_releasesExhaustedInputWhileLaterInputsRun() {
            final ByteStream stream = concatOverPrivateArray(1 << 20);
            final boolean[] released = { false };
            final long[] sum = { 0 };
            final List<Byte> tail = new ArrayList<>();

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

            long expected = 0;

            for (int i = 0; i < 1 << 20; i++) {
                expected += i % 100;
            }

            assertEquals(expected, sum[0]);
            assertEquals(Arrays.asList((byte) -1, (byte) -2), tail);
            assertTrue(released[0], "the exhausted first input (and its array) is still reachable from the concatenation");
        }

        // G091-01: append(ByteStream) delegates to concat(this, other): the exhausted sorted() receiver (and its sorted copy) is released too
        @Test
        void testAppend_releasesExhaustedSortedReceiverWhileAppendedStreamRuns() {
            final ByteStream stream = sortedThenAppend(1 << 20);
            final boolean[] released = { false };
            final List<Byte> seen = new ArrayList<>();

            stream.forEach(x -> {
                if (x == -1 && seen.size() == 3) {
                    released[0] = awaitCleared(payloadRef);
                }

                if (x < 0 || seen.size() < 3) {
                    seen.add(x);
                }
            });

            assertEquals(Arrays.asList((byte) 0, (byte) 0, (byte) 0, (byte) -1, (byte) -2), seen);
            assertTrue(released[0], "the exhausted sorted receiver (and its sorted copy) is still reachable from the appended stream");
        }

        // G091-01: element order, null inputs, close order (each exhausted input before the next one is opened, the rest on close),
        // and the caller's collection / varargs array are never modified
        @Test
        void testConcat_orderNullInputsAndCloseOrder() {
            final List<String> log = new ArrayList<>();
            final ByteStream a = ByteStream.of((byte) 1, (byte) 2).onClose(() -> log.add("closeA"));
            final ByteStream b = ByteStream.of((byte) 3).onClose(() -> log.add("closeB"));
            final ByteStream c = ByteStream.of((byte) 4, (byte) 5).onClose(() -> log.add("closeC"));
            final ByteStream d = ByteStream.of((byte) 6).onClose(() -> log.add("closeD"));
            final List<ByteStream> input = new ArrayList<>(Arrays.asList(a, null, b, null, c, d));
            final ByteStream stream = ByteStream.concat(input);
            final ByteIteratorEx iter = stream.iteratorEx();
            final List<Byte> seen = new ArrayList<>();

            while (seen.size() < 4 && iter.hasNext()) {
                final byte x = iter.nextByte();
                seen.add(x);
                log.add("next" + x);
            }

            stream.close();

            assertEquals(Arrays.asList((byte) 1, (byte) 2, (byte) 3, (byte) 4), seen);
            assertEquals(Arrays.asList("next1", "next2", "closeA", "next3", "closeB", "next4", "closeC", "closeD"), log);
            assertEquals(6, input.size());
            assertSame(a, input.get(0));
            assertNull(input.get(1));
            assertSame(b, input.get(2));
            assertNull(input.get(3));
            assertSame(c, input.get(4));
            assertSame(d, input.get(5));
            assertThrows(IllegalStateException.class, a::toArray);
            assertThrows(IllegalStateException.class, d::toArray);

            final ByteStream e = ByteStream.of((byte) 7, (byte) 8);
            final ByteStream f = ByteStream.of((byte) 9);
            final ByteStream[] array = { e, null, f };
            assertArrayEquals(new byte[] { 7, 8, 9 }, ByteStream.concat(array).toArray());
            assertSame(e, array[0]);
            assertNull(array[1]);
            assertSame(f, array[2]);

            assertEquals(0, ByteStream.concat(Arrays.asList((ByteStream) null, null)).count());
            assertEquals(0, ByteStream.concat(new ArrayList<ByteStream>()).count());
            assertArrayEquals(new byte[] { 1, 2, 3, 4 }, ByteStream.of((byte) 1, (byte) 2).append(ByteStream.of((byte) 3, (byte) 4)).toArray());
            assertArrayEquals(new byte[] { 3, 4, 1, 2 }, ByteStream.of((byte) 1, (byte) 2).prepend(ByteStream.of((byte) 3, (byte) 4)).toArray());
            assertEquals(5, ByteStream.concat(ByteStream.of((byte) 1, (byte) 2), ByteStream.empty(), ByteStream.of((byte) 3, (byte) 4, (byte) 5)).count());
            assertEquals(com.landawn.abacus.util.ByteList.of((byte) 1, (byte) 3, (byte) 4),
                    ByteStream.concat(Arrays.asList(ByteStream.of((byte) 1, (byte) 2), ByteStream.of((byte) 3, (byte) 4))).filter(x -> x != 2).toByteList());
            assertArrayEquals(new byte[] { 2, 3 },
                    ByteStream.concat(Arrays.asList(ByteStream.of((byte) 1, (byte) 2), ByteStream.of((byte) 3, (byte) 4))).skip(1).limit(2).toArray());
        }

        // G091-01: a failing close of an exhausted input and an already-closed input propagate from hasNext(); a retry continues;
        // the remaining inputs are closed with the concatenation
        @Test
        void testConcat_failingCloseAndClosedInputThenRetry() {
            final List<String> log = new ArrayList<>();
            final ByteStream a = ByteStream.of((byte) 1).onClose(() -> {
                log.add("closeA");
                throw new IllegalStateException("closeA failed");
            });
            final ByteStream closed = ByteStream.of((byte) 99);
            closed.close();
            final ByteStream c = ByteStream.of((byte) 3).onClose(() -> log.add("closeC"));
            final ByteStream stream = ByteStream.concat(Arrays.asList(a, closed, c));
            final ByteIteratorEx iter = stream.iteratorEx();

            assertEquals(1, iter.nextByte());
            final IllegalStateException closeFailure = assertThrows(IllegalStateException.class, iter::hasNext);
            assertEquals("closeA failed", closeFailure.getMessage());
            assertEquals(Arrays.asList("closeA"), log);

            // retry: `a` is closed already (no second close call); the next input is closed and is rejected when reached
            final IllegalStateException rejected = assertThrows(IllegalStateException.class, iter::hasNext);
            assertEquals("This stream is already terminated.", rejected.getMessage());
            assertEquals(Arrays.asList("closeA"), log);

            // retry again: traversal continues with the input after the rejected one
            assertTrue(iter.hasNext());
            assertEquals(3, iter.nextByte());
            assertFalse(iter.hasNext());
            assertThrows(java.util.NoSuchElementException.class, iter::nextByte);

            stream.close();
            assertEquals(Arrays.asList("closeA", "closeC"), log);

            // the same stream twice: the second occurrence is rejected when it is reached
            final ByteStream twice = ByteStream.of((byte) 1, (byte) 2);
            final List<Byte> collected = new ArrayList<>();
            final IllegalStateException repeated = assertThrows(IllegalStateException.class, () -> ByteStream.concat(twice, twice).forEach(collected::add));
            assertEquals("This stream is already terminated.", repeated.getMessage());
            assertEquals(Arrays.asList((byte) 1, (byte) 2), collected);

            // a terminal operation that fails on a closed input closes every other input
            final List<String> log2 = new ArrayList<>();
            final ByteStream x = ByteStream.of((byte) 1).onClose(() -> log2.add("closeX"));
            final ByteStream closed2 = ByteStream.of((byte) 2);
            closed2.close();
            final ByteStream z = ByteStream.of((byte) 3).onClose(() -> log2.add("closeZ"));
            assertThrows(IllegalStateException.class, () -> ByteStream.concat(x, closed2, z).toArray());
            assertEquals(Arrays.asList("closeX", "closeZ"), log2);

            // a failing close handler of a not yet exhausted input still runs when the concatenation is closed
            final List<String> log3 = new ArrayList<>();
            final ByteStream p = ByteStream.of((byte) 1, (byte) 2).onClose(() -> log3.add("closeP"));
            final ByteStream q = ByteStream.of((byte) 3).onClose(() -> {
                log3.add("closeQ");
                throw new IllegalStateException("closeQ failed");
            });
            final ByteStream r = ByteStream.of((byte) 4).onClose(() -> log3.add("closeR"));
            final ByteStream pqr = ByteStream.concat(Arrays.asList(p, q, r));
            assertEquals(1, pqr.iteratorEx().nextByte());
            final IllegalStateException onClose = assertThrows(IllegalStateException.class, pqr::close);
            assertEquals("closeQ failed", onClose.getMessage());
            assertEquals(Arrays.asList("closeP", "closeQ", "closeR"), log3);
        }

        // G091-01: parallel append/concat (the concatenation's iterator is shared by the workers) delivers every element and closes every input
        @Test
        void testConcat_parallelDeliversAllElementsAndClosesInputs() {
            for (int round = 0; round < 20; round++) {
                final AtomicInteger closes = new AtomicInteger();
                final List<ByteStream> inputs = new ArrayList<>();
                final List<Byte> expected = new ArrayList<>();

                for (int i = 0; i < 50; i++) {
                    final byte[] values = new byte[i % 7];

                    for (int j = 0; j < values.length; j++) {
                        values[j] = (byte) (i + j);
                        expected.add(values[j]);
                    }

                    inputs.add(i % 10 == 3 ? null : ByteStream.of(values).onClose(closes::incrementAndGet));

                    if (i % 10 == 3) {
                        for (int j = 0; j < values.length; j++) {
                            expected.remove(expected.size() - 1);
                        }
                    }
                }

                final byte[] actual = ByteStream.concat(inputs).parallel(4).map(v -> (byte) (v + 1)).toArray();
                final byte[] sortedExpected = new byte[expected.size()];

                for (int i = 0; i < sortedExpected.length; i++) {
                    sortedExpected[i] = (byte) (expected.get(i) + 1);
                }

                Arrays.sort(sortedExpected);
                Arrays.sort(actual);
                assertArrayEquals(sortedExpected, actual);
                assertEquals(45, closes.get());

                final AtomicInteger closes2 = new AtomicInteger();
                final byte[] appended = ByteStream.of((byte) 1, (byte) 2, (byte) 3)
                        .onClose(closes2::incrementAndGet)
                        .parallel(3)
                        .append(ByteStream.of((byte) 4, (byte) 5).onClose(closes2::incrementAndGet))
                        .toArray();
                Arrays.sort(appended);
                assertArrayEquals(new byte[] { 1, 2, 3, 4, 5 }, appended);
                assertEquals(2, closes2.get());
            }
        }
    }

    // ---- perf review 2026-10-08 G091 end ----
    // ---- perf review 2026-10-08 G092 begin ----
    @Nested
    class G092 {

        /**
         * Mostly ASCII (a Latin-1 compact String); with {@code utf16} also chars above 0xFF and lone surrogates (a UTF-16 String).
         */
        private String randomString(final Random random, final int length, final boolean utf16) {
            final StringBuilder sb = new StringBuilder(length);

            for (int i = 0; i < length; i++) {
                final int kind = utf16 ? random.nextInt(10) : 0;

                if (kind < 7) {
                    sb.append((char) ('a' + random.nextInt(26)));
                } else if (kind < 9) {
                    sb.append((char) (0x0100 + random.nextInt(0x3000)));
                } else {
                    sb.append((char) (0xD800 + random.nextInt(0x800)));
                }
            }

            return sb.toString();
        }

        private char[] reversedCopy(final char[] a) {
            final char[] result = new char[a.length];

            for (int i = 0; i < a.length; i++) {
                result[i] = a[a.length - 1 - i];
            }

            return result;
        }

        // G092-01: the String-backed iterator's bulk toList() (also behind toArray(), sorted(), reversed(), skip(n).toArray())
        // returns exactly the chars of the range, for Latin-1 and UTF-16 strings, any range and any skip.
        @Test
        void testOfString_bulkToArrayAndToCharListMatchSubstringReference() {
            final Random random = new Random(92);

            for (int round = 0; round < 400; round++) {
                final int length = round < 40 ? round : random.nextInt(round < 380 ? 300 : 5000);
                final String str = randomString(random, length, round % 3 == 0);
                final int fromIndex = random.nextInt(length + 1);
                final int toIndex = fromIndex + random.nextInt(length - fromIndex + 1);
                final char[] expected = str.substring(fromIndex, toIndex).toCharArray();
                final String message = "round " + round + ": [" + fromIndex + ", " + toIndex + ") of length " + length;

                assertArrayEquals(expected, CharStream.of(str, fromIndex, toIndex).toArray(), message);
                assertArrayEquals(str.toCharArray(), CharStream.of(str).toArray(), message);

                final com.landawn.abacus.util.CharList list = CharStream.of(str, fromIndex, toIndex).toCharList();
                assertArrayEquals(expected, list.toArray(), message);
                list.add('#');
                assertEquals(expected.length + 1, list.size(), message);
                assertEquals('#', list.get(expected.length), message);

                final int skip = random.nextInt(expected.length + 2);
                assertArrayEquals(Arrays.copyOfRange(expected, Math.min(skip, expected.length), expected.length),
                        CharStream.of(str, fromIndex, toIndex).skip(skip).toArray(), message + ", skip " + skip);

                final char[] sorted = expected.clone();
                Arrays.sort(sorted);
                assertArrayEquals(sorted, CharStream.of(str, fromIndex, toIndex).sorted().toArray(), message);
                assertArrayEquals(reversedCopy(expected), CharStream.of(str, fromIndex, toIndex).reversed().toArray(), message);
                assertEquals(expected.length, CharStream.of(str, fromIndex, toIndex).count(), message);
            }
        }

        // G092-01: an empty remainder still takes the inherited path: the shared empty array, and a fresh empty mutable list.
        @Test
        void testOfString_emptyRemainderKeepsSharedEmptyArrayAndFreshList() {
            assertSame(com.landawn.abacus.util.N.EMPTY_CHAR_ARRAY, CharStream.of("abc", 1, 1).toArray());
            assertSame(com.landawn.abacus.util.N.EMPTY_CHAR_ARRAY, CharStream.of("abc").skip(3).toArray());
            assertSame(com.landawn.abacus.util.N.EMPTY_CHAR_ARRAY, CharStream.of("abc").skip(7).toArray());

            final com.landawn.abacus.util.CharList first = CharStream.of("abc", 3, 3).toCharList();
            final com.landawn.abacus.util.CharList second = CharStream.of("abc", 3, 3).toCharList();
            assertNotSame(first, second);
            assertEquals(0, first.size());
            first.add('x');
            assertEquals(1, first.size());
            assertEquals(0, second.size());

            // a non-empty result is a fresh array each time
            final char[] a1 = CharStream.of("abc").toArray();
            final char[] a2 = CharStream.of("abc").toArray();
            assertNotSame(a1, a2);
            assertArrayEquals(new char[] { 'a', 'b', 'c' }, a1);
        }

        // G092-01: toList() copies only what was not consumed yet and leaves the iterator exhausted, like the element-wise path.
        @Test
        void testOfString_iteratorToListCopiesRemainderAndExhausts() {
            final String latin1 = "hello, world";
            final String utf16 = "ab" + (char) 0x20AC + "cd" + (char) 0xD83D + (char) 0xDE00 + "ef";

            for (final String str : new String[] { latin1, utf16 }) {
                final CharIteratorEx iter = CharStream.of(str, 1, str.length() - 1).iteratorEx();
                assertEquals(str.charAt(1), iter.nextChar());
                iter.advance(2);

                final com.landawn.abacus.util.CharList rest = iter.toList();
                assertArrayEquals(str.substring(4, str.length() - 1).toCharArray(), rest.toArray());
                assertFalse(iter.hasNext());
                assertThrows(java.util.NoSuchElementException.class, iter::nextChar);
                assertEquals(0, iter.toList().size());
                assertSame(com.landawn.abacus.util.N.EMPTY_CHAR_ARRAY, iter.toArray());
                assertEquals(0, iter.count());

                final CharIteratorEx iter2 = CharStream.of(str).iteratorEx();
                assertArrayEquals(str.toCharArray(), iter2.toArray());
                assertFalse(iter2.hasNext());
                assertSame(com.landawn.abacus.util.N.EMPTY_CHAR_ARRAY, iter2.toArray());
            }
        }

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

        /**
         * Built in a separate frame so that only the returned stream references the payload array.
         */
        private CharStream concatOverPrivateArray(final int n) {
            final char[] payload = new char[n];

            for (int i = 0; i < n; i++) {
                payload[i] = (char) ('a' + i % 20);
            }

            payloadRef = new WeakReference<>(payload);

            return CharStream.concat(Arrays.asList(CharStream.of(payload), null, CharStream.of('Y', 'Z')));
        }

        /**
         * Built in a separate frame so that only the returned stream references the sorted() stream (and its sorted copy).
         */
        private CharStream sortedThenAppend(final int n) {
            final char[] values = new char[n];

            for (int i = 0; i < n; i++) {
                values[i] = (char) ('a' + 99 - i % 100);
            }

            final CharStream sorted = CharStream.of(values).sorted();
            payloadRef = new WeakReference<>(sorted);

            return sorted.append(CharStream.of('Y', 'Z'));
        }

        // G092-07: an exhausted (and closed) input of concat(Collection) is no longer reachable from the concatenation while later inputs run
        @Test
        void testConcat_releasesExhaustedInputWhileLaterInputsRun() {
            final CharStream stream = concatOverPrivateArray(1 << 20);
            final boolean[] released = { false };
            final long[] sum = { 0 };
            final List<Character> tail = new ArrayList<>();

            stream.forEach(x -> {
                if (x < 'a') {
                    if (x == 'Y') {
                        released[0] = awaitCleared(payloadRef);
                    }

                    tail.add(x);
                } else {
                    sum[0] += x;
                }
            });

            long expected = 0;

            for (int i = 0; i < 1 << 20; i++) {
                expected += 'a' + i % 20;
            }

            assertEquals(expected, sum[0]);
            assertEquals(Arrays.asList('Y', 'Z'), tail);
            assertTrue(released[0], "the exhausted first input (and its array) is still reachable from the concatenation");
        }

        // G092-07: append(CharStream) delegates to concat(this, other): the exhausted sorted() receiver (and its sorted copy) is released too
        @Test
        void testAppend_releasesExhaustedSortedReceiverWhileAppendedStreamRuns() {
            final CharStream stream = sortedThenAppend(1 << 20);
            final boolean[] released = { false };
            final List<Character> seen = new ArrayList<>();

            stream.forEach(x -> {
                if (x == 'Y' && seen.size() == 3) {
                    released[0] = awaitCleared(payloadRef);
                }

                if (x < 'a' || seen.size() < 3) {
                    seen.add(x);
                }
            });

            assertEquals(Arrays.asList('a', 'a', 'a', 'Y', 'Z'), seen);
            assertTrue(released[0], "the exhausted sorted receiver (and its sorted copy) is still reachable from the appended stream");
        }

        // G092-07: element order, null inputs, close order (each exhausted input before the next one is opened, the rest on close),
        // and the caller's collection / varargs array are never modified
        @Test
        void testConcat_orderNullInputsAndCloseOrder() {
            final List<String> log = new ArrayList<>();
            final CharStream a = CharStream.of('1', '2').onClose(() -> log.add("closeA"));
            final CharStream b = CharStream.of('3').onClose(() -> log.add("closeB"));
            final CharStream c = CharStream.of('4', '5').onClose(() -> log.add("closeC"));
            final CharStream d = CharStream.of('6').onClose(() -> log.add("closeD"));
            final List<CharStream> input = new ArrayList<>(Arrays.asList(a, null, b, null, c, d));
            final CharStream stream = CharStream.concat(input);
            final CharIteratorEx iter = stream.iteratorEx();
            final List<Character> seen = new ArrayList<>();

            while (seen.size() < 4 && iter.hasNext()) {
                final char x = iter.nextChar();
                seen.add(x);
                log.add("next" + x);
            }

            stream.close();

            assertEquals(Arrays.asList('1', '2', '3', '4'), seen);
            assertEquals(Arrays.asList("next1", "next2", "closeA", "next3", "closeB", "next4", "closeC", "closeD"), log);
            assertEquals(6, input.size());
            assertSame(a, input.get(0));
            assertNull(input.get(1));
            assertSame(b, input.get(2));
            assertNull(input.get(3));
            assertSame(c, input.get(4));
            assertSame(d, input.get(5));
            assertThrows(IllegalStateException.class, a::toArray);
            assertThrows(IllegalStateException.class, d::toArray);

            final CharStream e = CharStream.of('7', '8');
            final CharStream f = CharStream.of('9');
            final CharStream[] array = { e, null, f };
            assertArrayEquals(new char[] { '7', '8', '9' }, CharStream.concat(array).toArray());
            assertSame(e, array[0]);
            assertNull(array[1]);
            assertSame(f, array[2]);

            assertEquals(0, CharStream.concat(Arrays.asList((CharStream) null, null)).count());
            assertEquals(0, CharStream.concat(new ArrayList<CharStream>()).count());
            assertArrayEquals(new char[] { '1', '2', '3', '4' }, CharStream.of('1', '2').append(CharStream.of('3', '4')).toArray());
            assertArrayEquals(new char[] { '3', '4', '1', '2' }, CharStream.of('1', '2').prepend(CharStream.of('3', '4')).toArray());
            assertEquals(5, CharStream.concat(CharStream.of('1', '2'), CharStream.empty(), CharStream.of('3', '4', '5')).count());
            assertEquals(com.landawn.abacus.util.CharList.of('1', '3', '4'),
                    CharStream.concat(Arrays.asList(CharStream.of('1', '2'), CharStream.of('3', '4'))).filter(x -> x != '2').toCharList());
            assertArrayEquals(new char[] { '2', '3' },
                    CharStream.concat(Arrays.asList(CharStream.of('1', '2'), CharStream.of('3', '4'))).skip(1).limit(2).toArray());
        }

        // G092-07: a failing close of an exhausted input and an already-closed input propagate from hasNext(); a retry continues;
        // the remaining inputs are closed with the concatenation
        @Test
        void testConcat_failingCloseAndClosedInputThenRetry() {
            final List<String> log = new ArrayList<>();
            final CharStream a = CharStream.of('1').onClose(() -> {
                log.add("closeA");
                throw new IllegalStateException("closeA failed");
            });
            final CharStream closed = CharStream.of('x');
            closed.close();
            final CharStream c = CharStream.of('3').onClose(() -> log.add("closeC"));
            final CharStream stream = CharStream.concat(Arrays.asList(a, closed, c));
            final CharIteratorEx iter = stream.iteratorEx();

            assertEquals('1', iter.nextChar());
            final IllegalStateException closeFailure = assertThrows(IllegalStateException.class, iter::hasNext);
            assertEquals("closeA failed", closeFailure.getMessage());
            assertEquals(Arrays.asList("closeA"), log);

            // retry: `a` is closed already (no second close call); the next input is closed and is rejected when reached
            final IllegalStateException rejected = assertThrows(IllegalStateException.class, iter::hasNext);
            assertEquals("This stream is already terminated.", rejected.getMessage());
            assertEquals(Arrays.asList("closeA"), log);

            // retry again: traversal continues with the input after the rejected one
            assertTrue(iter.hasNext());
            assertEquals('3', iter.nextChar());
            assertFalse(iter.hasNext());
            assertThrows(java.util.NoSuchElementException.class, iter::nextChar);

            stream.close();
            assertEquals(Arrays.asList("closeA", "closeC"), log);

            // the same stream twice: the second occurrence is rejected when it is reached
            final CharStream twice = CharStream.of('1', '2');
            final List<Character> collected = new ArrayList<>();
            final IllegalStateException repeated = assertThrows(IllegalStateException.class, () -> CharStream.concat(twice, twice).forEach(collected::add));
            assertEquals("This stream is already terminated.", repeated.getMessage());
            assertEquals(Arrays.asList('1', '2'), collected);

            // a terminal operation that fails on a closed input closes every other input
            final List<String> log2 = new ArrayList<>();
            final CharStream x = CharStream.of('1').onClose(() -> log2.add("closeX"));
            final CharStream closed2 = CharStream.of('2');
            closed2.close();
            final CharStream z = CharStream.of('3').onClose(() -> log2.add("closeZ"));
            assertThrows(IllegalStateException.class, () -> CharStream.concat(x, closed2, z).toArray());
            assertEquals(Arrays.asList("closeX", "closeZ"), log2);

            // a failing close handler of a not yet exhausted input still runs when the concatenation is closed
            final List<String> log3 = new ArrayList<>();
            final CharStream p = CharStream.of('1', '2').onClose(() -> log3.add("closeP"));
            final CharStream q = CharStream.of('3').onClose(() -> {
                log3.add("closeQ");
                throw new IllegalStateException("closeQ failed");
            });
            final CharStream r = CharStream.of('4').onClose(() -> log3.add("closeR"));
            final CharStream pqr = CharStream.concat(Arrays.asList(p, q, r));
            assertEquals('1', pqr.iteratorEx().nextChar());
            final IllegalStateException onClose = assertThrows(IllegalStateException.class, pqr::close);
            assertEquals("closeQ failed", onClose.getMessage());
            assertEquals(Arrays.asList("closeP", "closeQ", "closeR"), log3);
        }

        // G092-07: parallel append/concat (the concatenation's iterator is shared by the workers) delivers every element and closes every input
        @Test
        void testConcat_parallelDeliversAllElementsAndClosesInputs() {
            for (int round = 0; round < 20; round++) {
                final AtomicInteger closes = new AtomicInteger();
                final List<CharStream> inputs = new ArrayList<>();
                final List<Character> expected = new ArrayList<>();

                for (int i = 0; i < 50; i++) {
                    final char[] values = new char[i % 7];

                    for (int j = 0; j < values.length; j++) {
                        values[j] = (char) ('A' + i + j);
                    }

                    if (i % 10 == 3) {
                        inputs.add(null);
                    } else {
                        inputs.add(CharStream.of(values).onClose(closes::incrementAndGet));

                        for (final char v : values) {
                            expected.add(v);
                        }
                    }
                }

                final char[] actual = CharStream.concat(inputs).parallel(4).map(v -> (char) (v + 1)).toArray();
                final char[] sortedExpected = new char[expected.size()];

                for (int i = 0; i < sortedExpected.length; i++) {
                    sortedExpected[i] = (char) (expected.get(i) + 1);
                }

                Arrays.sort(sortedExpected);
                Arrays.sort(actual);
                assertArrayEquals(sortedExpected, actual);
                assertEquals(45, closes.get());

                final AtomicInteger closes2 = new AtomicInteger();
                final char[] appended = CharStream.of('1', '2', '3')
                        .onClose(closes2::incrementAndGet)
                        .parallel(3)
                        .append(CharStream.of('4', '5').onClose(closes2::incrementAndGet))
                        .toArray();
                Arrays.sort(appended);
                assertArrayEquals(new char[] { '1', '2', '3', '4', '5' }, appended);
                assertEquals(2, closes2.get());
            }
        }
    }

    // ---- perf review 2026-10-08 G092 end ----
    // ---- perf review 2026-10-08 G093 begin ----
    @Nested
    class G093 {
        private final java.util.stream.Collector.Characteristics concurrent = java.util.stream.Collector.Characteristics.CONCURRENT;
        private final java.util.stream.Collector.Characteristics unordered = java.util.stream.Collector.Characteristics.UNORDERED;
        private final java.util.stream.Collector.Characteristics identityFinish = java.util.stream.Collector.Characteristics.IDENTITY_FINISH;

        private final java.util.function.Supplier<List<String>> listSupplier = ArrayList::new;
        private final java.util.function.BiConsumer<List<String>, String> listAccumulator = List::add;
        private final java.util.function.BinaryOperator<List<String>> listCombiner = (a, b) -> {
            a.addAll(b);
            return a;
        };
        private final java.util.function.Function<List<String>, String> listFinisher = String::valueOf;

        private java.util.stream.Collector.Characteristics[] characteristicsWithNullAt(final int length, final int nullIndex) {
            final java.util.stream.Collector.Characteristics[] values = java.util.stream.Collector.Characteristics.values();
            final java.util.stream.Collector.Characteristics[] result = new java.util.stream.Collector.Characteristics[length];

            for (int i = 0; i < length; i++) {
                result[i] = i == nullIndex ? null : values[i % values.length];
            }

            return result;
        }

        private String createMessage(final org.junit.jupiter.api.function.Executable executable) {
            return assertThrows(IllegalArgumentException.class, executable).getMessage();
        }

        // G093-01: checkCharacteristics builds the indexed message only for a null element; pins the exact message, the first null index, all four create overloads and the check order.
        @Test
        public void testCreate_nullCharacteristicMessages() {
            final int[][] lengthAndNullIndex = { { 1, 0 }, { 2, 0 }, { 2, 1 }, { 3, 2 }, { 11, 10 }, { 12, 10 }, { 13, 0 } };

            for (final int[] c : lengthAndNullIndex) {
                final java.util.stream.Collector.Characteristics[] array = characteristicsWithNullAt(c[0], c[1]);
                final List<java.util.stream.Collector.Characteristics> list = Arrays.asList(array);
                final String expected = "'characteristics[" + c[1] + "]' cannot be null";

                assertEquals(expected, createMessage(() -> Collectors.create(listSupplier, listAccumulator, listCombiner, array)));
                assertEquals(expected, createMessage(() -> Collectors.create(listSupplier, listAccumulator, listCombiner, listFinisher, array)));
                assertEquals(expected, createMessage(() -> Collectors.create(listSupplier, listAccumulator, listCombiner, list)));
                assertEquals(expected, createMessage(() -> Collectors.create(listSupplier, listAccumulator, listCombiner, listFinisher, list)));
            }

            final java.util.stream.Collector.Characteristics[] twoNulls = { unordered, null, concurrent, null };

            assertEquals("'characteristics[1]' cannot be null", createMessage(() -> Collectors.create(listSupplier, listAccumulator, listCombiner, twoNulls)));
            assertEquals("'characteristics[1]' cannot be null",
                    createMessage(() -> Collectors.create(listSupplier, listAccumulator, listCombiner, listFinisher, Arrays.asList(twoNulls))));
            assertEquals("'characteristics' cannot be null",
                    createMessage(() -> Collectors.create(listSupplier, listAccumulator, listCombiner, (java.util.stream.Collector.Characteristics[]) null)));
            assertEquals("'characteristics' cannot be null", createMessage(
                    () -> Collectors.create(listSupplier, listAccumulator, listCombiner, listFinisher, (java.util.stream.Collector.Characteristics[]) null)));

            // the functional arguments are still checked first
            assertEquals("'supplier' cannot be null",
                    createMessage(() -> Collectors.create((java.util.function.Supplier<List<String>>) null, listAccumulator, listCombiner, twoNulls)));
            assertEquals("'finisher' cannot be null", createMessage(
                    () -> Collectors.create(listSupplier, listAccumulator, listCombiner, (java.util.function.Function<List<String>, String>) null, twoNulls)));
        }

        // G093-01: valid characteristics pass through unchanged (same sets as Collector.of builds) and the collectors still work.
        @Test
        public void testCreate_validCharacteristicsUnchanged() {
            final java.util.stream.Collector.Characteristics[][] arrays = { {}, java.util.stream.Collector.Characteristics.values(),
                    characteristicsWithNullAt(12, -1), { unordered }, { identityFinish, identityFinish }, { concurrent, unordered } };

            for (final java.util.stream.Collector.Characteristics[] array : arrays) {
                final Set<java.util.stream.Collector.Characteristics> withoutFinisher = java.util.stream.Collector
                        .of(listSupplier, listAccumulator, listCombiner, array)
                        .characteristics();
                final Set<java.util.stream.Collector.Characteristics> withFinisher = java.util.stream.Collector
                        .of(listSupplier, listAccumulator, listCombiner, listFinisher, array)
                        .characteristics();

                assertEquals(withoutFinisher, Collectors.create(listSupplier, listAccumulator, listCombiner, array).characteristics());
                assertEquals(withFinisher, Collectors.create(listSupplier, listAccumulator, listCombiner, listFinisher, array).characteristics());
                assertEquals(withoutFinisher, Collectors.create(listSupplier, listAccumulator, listCombiner, Arrays.asList(array)).characteristics());
                assertEquals(withFinisher,
                        Collectors.create(listSupplier, listAccumulator, listCombiner, listFinisher, Arrays.asList(array)).characteristics());

                assertEquals(Arrays.asList("a", "b", "c"),
                        java.util.stream.Stream.of("a", "b", "c").collect(Collectors.create(listSupplier, listAccumulator, listCombiner, array)));
                assertEquals(Arrays.asList("a", "b", "c"),
                        Stream.of("a", "b", "c").collect(Collectors.create(listSupplier, listAccumulator, listCombiner, array)));
            }

            assertEquals("[a, b]", Stream.of("a", "b").collect(Collectors.create(listSupplier, listAccumulator, listCombiner, listFinisher)));
            assertEquals("[a, b]", Stream.of("a", "b")
                    .collect(Collectors.create(listSupplier, listAccumulator, listCombiner, listFinisher, Collections.singleton(unordered))));

            // factories in the reviewed range keep their characteristics
            assertEquals(java.util.EnumSet.of(identityFinish), Collectors.toList().characteristics());
            assertEquals(java.util.EnumSet.of(unordered, identityFinish), Collectors.toSet().characteristics());
            assertEquals(java.util.EnumSet.of(identityFinish), Collectors.toLinkedHashSet().characteristics());
            assertEquals(java.util.EnumSet.noneOf(java.util.stream.Collector.Characteristics.class), Collectors.joining().characteristics());
            assertEquals(java.util.EnumSet.of(unordered), Collectors.counting().characteristics());
            assertEquals(java.util.EnumSet.of(unordered), Collectors.countingToInt().characteristics());
            assertEquals(java.util.EnumSet.of(unordered), Collectors.summingInt((Integer x) -> x).characteristics());
            assertEquals(java.util.EnumSet.of(unordered, identityFinish), Collectors.mapping(String::valueOf, Collectors.toSet()).characteristics());
            assertEquals(java.util.EnumSet.of(identityFinish), Collectors.filtering((Integer x) -> x > 0, Collectors.toList()).characteristics());
            assertEquals(java.util.EnumSet.noneOf(java.util.stream.Collector.Characteristics.class), Collectors.toImmutableList().characteristics());
            assertEquals(java.util.EnumSet.of(unordered), Collectors.collectingOrEmpty(Collectors.toSet()).characteristics());
        }

        // G093-02: counting()/countingToInt() increment the summingLong/summingInt containers directly; pins results, result types, nulls, sizes, sequential/parallel (combiner) paths.
        @Test
        public void testCounting_resultsTypesAndParallel() {
            for (final int n : new int[] { 0, 1, 2, 3, 17, 1000, 5000 }) {
                final List<String> input = new ArrayList<>();

                for (int i = 0; i < n; i++) {
                    input.add(i % 3 == 0 ? null : "e" + i);
                }

                final Long expectedLong = (long) n;
                final Integer expectedInt = n;

                assertEquals(expectedLong, Stream.of(input).collect(Collectors.counting()));
                assertEquals(expectedInt, Stream.of(input).collect(Collectors.countingToInt()));
                assertEquals(expectedLong, input.stream().collect(Collectors.counting()));
                assertEquals(expectedInt, input.stream().collect(Collectors.countingToInt()));
                assertEquals(expectedLong, input.parallelStream().collect(Collectors.counting()));
                assertEquals(expectedInt, input.parallelStream().collect(Collectors.countingToInt()));
                assertEquals(expectedLong, Stream.of(input).parallel(4).collect(Collectors.counting()));
                assertEquals(expectedInt, Stream.of(input).parallel(4).collect(Collectors.countingToInt()));
                assertEquals(expectedLong, Stream.of(input).collect(Collectors.collectingOrDefaultIfEmpty(Collectors.counting(), 0L)));
            }

            assertNotSame(Collectors.counting(), Collectors.counting());
            assertNotSame(Collectors.countingToInt(), Collectors.countingToInt());
        }

        // G093-02: the accumulation containers, combiners, finishers and overflow wrap-around are unchanged.
        @SuppressWarnings({ "rawtypes", "unchecked" })
        @Test
        public void testCounting_containersCombinerAndOverflow() {
            final java.util.stream.Collector longCounting = Collectors.counting();
            final Object first = longCounting.supplier().get();
            final Object second = longCounting.supplier().get();

            assertTrue(first instanceof long[]);
            assertNotSame(first, second);
            assertEquals(1, ((long[]) first).length);

            for (int i = 0; i < 3; i++) {
                longCounting.accumulator().accept(first, i == 1 ? null : "x");
            }

            for (int i = 0; i < 4; i++) {
                longCounting.accumulator().accept(second, i);
            }

            assertSame(first, longCounting.combiner().apply(first, second));
            assertEquals(Long.valueOf(7), longCounting.finisher().apply(first));
            assertEquals(Long.valueOf(4), longCounting.finisher().apply(second));

            ((long[]) first)[0] = Long.MAX_VALUE;
            longCounting.accumulator().accept(first, "overflow");
            assertEquals(Long.valueOf(Long.MIN_VALUE), longCounting.finisher().apply(first));

            final java.util.stream.Collector intCounting = Collectors.countingToInt();
            final Object intFirst = intCounting.supplier().get();
            final Object intSecond = intCounting.supplier().get();

            assertTrue(intFirst instanceof int[]);
            assertNotSame(intFirst, intSecond);
            assertEquals(1, ((int[]) intFirst).length);

            intCounting.accumulator().accept(intFirst, null);
            intCounting.accumulator().accept(intSecond, "a");
            intCounting.accumulator().accept(intSecond, "b");

            assertSame(intFirst, intCounting.combiner().apply(intFirst, intSecond));
            assertEquals(Integer.valueOf(3), intCounting.finisher().apply(intFirst));

            ((int[]) intFirst)[0] = Integer.MAX_VALUE;
            intCounting.accumulator().accept(intFirst, "overflow");
            assertEquals(Integer.valueOf(Integer.MIN_VALUE), intCounting.finisher().apply(intFirst));
        }

        // G093-02: grouped counting through the library paths that use counting()/countingToInt() as their downstream.
        @Test
        public void testCounting_groupedCallers() {
            final List<String> words = Arrays.asList("apple", "avocado", "banana", null, "blueberry", "cherry", "apple", null);
            final java.util.function.Function<String, String> firstLetter = s -> s == null ? "-" : s.substring(0, 1);

            final Map<String, Long> expectedLong = new LinkedHashMap<>();
            final Map<String, Integer> expectedInt = new LinkedHashMap<>();

            for (final String w : words) {
                expectedLong.merge(firstLetter.apply(w), 1L, Long::sum);
                expectedInt.merge(firstLetter.apply(w), 1, Integer::sum);
            }

            assertEquals(expectedLong, Stream.of(words).collect(Collectors.groupingBy(firstLetter, Collectors.counting())));
            assertEquals(expectedInt, Stream.of(words).collect(Collectors.groupingBy(firstLetter, Collectors.countingToInt())));
            assertEquals(expectedLong, Stream.of(words).collect(Collectors.countingBy(firstLetter)));
            assertEquals(expectedInt, Stream.of(words).collect(Collectors.countingToIntBy(firstLetter)));
            assertEquals(expectedLong, Stream.of(words).collect(Collectors.countingBy(firstLetter, TreeMap::new)));
            assertEquals(expectedInt, Stream.of(words).countBy(firstLetter).toMap(Map.Entry::getKey, Map.Entry::getValue));
            assertEquals(new ArrayList<>(new TreeMap<>(expectedInt).entrySet()), new ArrayList<>(Stream.of(words).countBy(firstLetter, TreeMap::new).toList()));
            assertEquals(expectedInt, words.parallelStream().collect(java.util.stream.Collectors.groupingBy(firstLetter, Collectors.countingToInt())));
        }
    }

    // ---- perf review 2026-10-08 G093 end ----
    // ---- perf review 2026-10-08 G095 begin ----
    @Nested
    class G095 {
        private final double[] samples = { 0.0, -0.0, 1.5, -2.25, Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY, Double.MIN_VALUE, 1e300 };

        private double[] randomArray(final Random rnd, final int maxLength) {
            final double[] a = new double[rnd.nextInt(maxLength + 1)];

            for (int i = 0; i < a.length; i++) {
                a[i] = rnd.nextInt(4) == 0 ? samples[rnd.nextInt(samples.length)] : rnd.nextInt(1000) / 8.0;
            }

            return a;
        }

        private double[] drain(final com.landawn.abacus.util.DoubleIterator iterator) {
            final com.landawn.abacus.util.DoubleList list = new com.landawn.abacus.util.DoubleList();

            while (iterator.hasNext()) {
                list.add(iterator.nextDouble());
            }

            return list.toArray();
        }

        private double[] suffix(final double[] a, final int from) {
            return from >= a.length ? new double[0] : Arrays.copyOfRange(a, from, a.length);
        }

        private double f2(final double x, final double y) {
            return x * 3 + y;
        }

        private double f3(final double x, final double y, final double z) {
            return x * 3 + y - z;
        }

        // variant 0: zip(a, b), 1: zip(a, b, c), 2: zip(a, b, -0.0, NaN), 3: zip(a, b, c, -0.0, NaN, 7.5); every call is recorded
        private DoubleStream zipVariant(final int variant, final double[] a, final double[] b, final double[] c, final List<String> calls) {
            switch (variant) {
                case 0:
                    return DoubleStream.zip(a, b, (x, y) -> {
                        calls.add(x + "," + y);
                        return f2(x, y);
                    });
                case 1:
                    return DoubleStream.zip(a, b, c, (x, y, z) -> {
                        calls.add(x + "," + y + "," + z);
                        return f3(x, y, z);
                    });
                case 2:
                    return DoubleStream.zip(a, b, -0.0, Double.NaN, (x, y) -> {
                        calls.add(x + "," + y);
                        return f2(x, y);
                    });
                default:
                    return DoubleStream.zip(a, b, c, -0.0, Double.NaN, 7.5, (x, y, z) -> {
                        calls.add(x + "," + y + "," + z);
                        return f3(x, y, z);
                    });
            }
        }

        private double[] zipReference(final int variant, final double[] a, final double[] b, final double[] c) {
            final int len = variant == 0 ? Math.min(a.length, b.length)
                    : variant == 1 ? Math.min(a.length, Math.min(b.length, c.length))
                            : variant == 2 ? Math.max(a.length, b.length) : Math.max(a.length, Math.max(b.length, c.length));
            final double[] result = new double[len];

            for (int i = 0; i < len; i++) {
                final double x = i < a.length ? a[i] : -0.0;
                final double y = i < b.length ? b[i] : Double.NaN;
                final double z = i < c.length ? c[i] : 7.5;
                result[i] = variant == 0 || variant == 2 ? f2(x, y) : f3(x, y, z);
            }

            return result;
        }

        // G095-01: zip(double[]...) toArray()/toDoubleList()/skip(k).toArray()/iterator().toArray() fill an exactly sized array;
        // values and the zipFunction call sequence equal the element-by-element traversal
        @Test
        public void testZipArrays_bulkTerminalsMatchElementwiseTraversal() {
            final Random rnd = new Random(95);

            for (int round = 0; round < 400; round++) {
                final double[] a = randomArray(rnd, 12);
                final double[] b = randomArray(rnd, 12);
                final double[] c = randomArray(rnd, 12);

                for (int variant = 0; variant < 4; variant++) {
                    final double[] expected = zipReference(variant, a, b, c);
                    final List<String> expectedCalls = new ArrayList<>();
                    assertArrayEquals(expected, drain(zipVariant(variant, a, b, c, expectedCalls).iterator()));
                    assertEquals(expected.length, expectedCalls.size());

                    final List<String> calls = new ArrayList<>();
                    assertArrayEquals(expected, zipVariant(variant, a, b, c, calls).toArray());
                    assertEquals(expectedCalls, calls);

                    calls.clear();
                    final com.landawn.abacus.util.DoubleList list = zipVariant(variant, a, b, c, calls).toDoubleList();
                    assertArrayEquals(expected, list.toArray());
                    assertEquals(expectedCalls, calls);
                    list.add(42.0);
                    assertEquals(expected.length + 1, list.size());

                    final int k = rnd.nextInt(expected.length + 2);
                    calls.clear();
                    assertArrayEquals(suffix(expected, k), zipVariant(variant, a, b, c, calls).skip(k).toArray());
                    assertEquals(expectedCalls, calls);

                    calls.clear();
                    final com.landawn.abacus.util.DoubleIterator iter = zipVariant(variant, a, b, c, calls).iterator();
                    final int consumed = Math.min(k, expected.length);

                    for (int i = 0; i < consumed; i++) {
                        iter.nextDouble();
                    }

                    assertArrayEquals(suffix(expected, consumed), rnd.nextBoolean() ? iter.toArray() : iter.toList().toArray());
                    assertEquals(expectedCalls, calls);
                    assertFalse(iter.hasNext());
                    assertSame(com.landawn.abacus.util.N.EMPTY_DOUBLE_ARRAY, iter.toArray());
                    assertEquals(0, iter.toList().size());

                    calls.clear();
                    final double[] sorted = expected.clone();
                    Arrays.sort(sorted);
                    assertArrayEquals(sorted, zipVariant(variant, a, b, c, calls).sorted().toArray());
                    assertEquals(expectedCalls, calls);
                }
            }
        }

        // G095-01/03: a failing zipFunction or nextSelector inside toArray()/toList() leaves the iterator exactly where element-by-element
        // traversal leaves it (zip(a, b)/zip(a, b, c) advance before the call, the padded zips and merge do not), and the stream rethrows it
        @Test
        public void testZipAndMergeArrays_failingCallbackKeepsIteratorState() {
            final double[] a = { 1, 2, 3, 4, 5 };
            final double[] b = { 10, 20, 30, 40, 50, 60 };
            final double[] c = { 100, 200, 300, 400, 500, 600, 700 };

            for (int viaList = 0; viaList < 2; viaList++) {
                final boolean useList = viaList == 1;
                final int[] calls = { 0 };

                com.landawn.abacus.util.DoubleIterator iter = DoubleStream.zip(a, b, (x, y) -> {
                    if (++calls[0] == 3) {
                        throw new IllegalStateException("zip2");
                    }

                    return x + y;
                }).iterator();
                final com.landawn.abacus.util.DoubleIterator zip2 = iter;
                assertEquals("zip2", assertThrows(IllegalStateException.class, () -> bulk(zip2, useList)).getMessage());
                assertEquals(3, calls[0]);
                assertArrayEquals(new double[] { 44, 55 }, drain(iter));
                assertEquals(5, calls[0]);

                calls[0] = 0;
                iter = DoubleStream.zip(a, b, c, (x, y, z) -> {
                    if (++calls[0] == 2) {
                        throw new IllegalStateException("zip3");
                    }

                    return x + y + z;
                }).iterator();
                final com.landawn.abacus.util.DoubleIterator zip3 = iter;
                assertEquals("zip3", assertThrows(IllegalStateException.class, () -> bulk(zip3, useList)).getMessage());
                assertArrayEquals(new double[] { 333, 444, 555 }, drain(iter));
                assertEquals(5, calls[0]);

                calls[0] = 0;
                iter = DoubleStream.zip(a, b, 0.5, 0.25, (x, y) -> {
                    if (++calls[0] == 3) {
                        throw new IllegalStateException("pad2");
                    }

                    return x + y;
                }).iterator();
                final com.landawn.abacus.util.DoubleIterator pad2 = iter;
                assertEquals("pad2", assertThrows(IllegalStateException.class, () -> bulk(pad2, useList)).getMessage());
                assertArrayEquals(new double[] { 33, 44, 55, 60.5 }, drain(iter));
                assertEquals(7, calls[0]);

                calls[0] = 0;
                iter = DoubleStream.zip(a, b, c, 0.5, 0.25, 0.125, (x, y, z) -> {
                    if (++calls[0] == 6) {
                        throw new IllegalStateException("pad3");
                    }

                    return x + y + z;
                }).iterator();
                final com.landawn.abacus.util.DoubleIterator pad3 = iter;
                assertEquals("pad3", assertThrows(IllegalStateException.class, () -> bulk(pad3, useList)).getMessage());
                assertArrayEquals(new double[] { 660.5, 700.75 }, drain(iter));
                assertEquals(8, calls[0]);

                calls[0] = 0;
                iter = DoubleStream.merge(new double[] { 1, 3, 5 }, new double[] { 2, 4, 6 }, (x, y) -> {
                    if (++calls[0] == 3) {
                        throw new IllegalStateException("merge");
                    }

                    return x <= y ? com.landawn.abacus.util.MergeResult.TAKE_FIRST : com.landawn.abacus.util.MergeResult.TAKE_SECOND;
                }).iterator();
                final com.landawn.abacus.util.DoubleIterator merge = iter;
                assertEquals("merge", assertThrows(IllegalStateException.class, () -> bulk(merge, useList)).getMessage());
                assertArrayEquals(new double[] { 3, 4, 5, 6 }, drain(iter));
                assertEquals(6, calls[0]);
            }

            final IllegalStateException e = assertThrows(IllegalStateException.class, () -> DoubleStream.zip(a, b, (x, y) -> {
                if (x == 4) {
                    throw new IllegalStateException("stream");
                }

                return x + y;
            }).toArray());
            assertEquals("stream", e.getMessage());
        }

        private Object bulk(final com.landawn.abacus.util.DoubleIterator iterator, final boolean useList) {
            return useList ? iterator.toList() : iterator.toArray();
        }

        // G095-03: merge(double[], double[]) toArray()/toDoubleList() fill an exactly sized array; values and the order and number of
        // nextSelector calls (a stateful selector makes the result depend on them) equal the element-by-element traversal
        @Test
        public void testMergeArrays_bulkTerminalsMatchElementwiseTraversal() {
            final Random rnd = new Random(9503);

            for (int round = 0; round < 400; round++) {
                final double[] a = randomArray(rnd, 10);
                final double[] b = randomArray(rnd, 10);

                if (rnd.nextBoolean()) {
                    Arrays.sort(a);
                    Arrays.sort(b);
                }

                final boolean stateful = rnd.nextBoolean();

                // reference: the merge loop written out, with the same selector rule
                final com.landawn.abacus.util.DoubleList expectedList = new com.landawn.abacus.util.DoubleList();
                final List<String> expectedCalls = new ArrayList<>();
                int i = 0;
                int j = 0;

                while (i < a.length || j < b.length) {
                    if (i < a.length && (j >= b.length || takeFirst(stateful, a[i], b[j], expectedCalls))) {
                        expectedList.add(a[i++]);
                    } else {
                        expectedList.add(b[j++]);
                    }
                }

                final double[] expected = expectedList.toArray();

                final List<String> calls = new ArrayList<>();
                assertArrayEquals(expected, drain(mergeOf(a, b, stateful, calls).iterator()));
                assertEquals(expectedCalls, calls);

                calls.clear();
                assertArrayEquals(expected, mergeOf(a, b, stateful, calls).toArray());
                assertEquals(expectedCalls, calls);

                calls.clear();
                final com.landawn.abacus.util.DoubleList list = mergeOf(a, b, stateful, calls).toDoubleList();
                assertArrayEquals(expected, list.toArray());
                assertEquals(expectedCalls, calls);
                list.add(1.0);
                assertEquals(expected.length + 1, list.size());

                final int k = rnd.nextInt(expected.length + 2);
                calls.clear();
                assertArrayEquals(suffix(expected, k), mergeOf(a, b, stateful, calls).skip(k).toArray());
                assertEquals(expectedCalls, calls);

                calls.clear();
                final com.landawn.abacus.util.DoubleIterator iter = mergeOf(a, b, stateful, calls).iterator();
                final int consumed = Math.min(k, expected.length);

                for (int n = 0; n < consumed; n++) {
                    iter.nextDouble();
                }

                assertArrayEquals(suffix(expected, consumed), rnd.nextBoolean() ? iter.toArray() : iter.toList().toArray());
                assertEquals(expectedCalls, calls);
                assertFalse(iter.hasNext());
                assertEquals(0, iter.toArray().length);
            }
        }

        private boolean takeFirst(final boolean stateful, final double x, final double y, final List<String> calls) {
            calls.add(x + "," + y);
            return stateful ? calls.size() % 3 != 0 : Double.compare(x, y) <= 0;
        }

        private DoubleStream mergeOf(final double[] a, final double[] b, final boolean stateful, final List<String> calls) {
            return DoubleStream.merge(a, b, (x, y) -> takeFirst(stateful, x, y, calls) ? com.landawn.abacus.util.MergeResult.TAKE_FIRST
                    : com.landawn.abacus.util.MergeResult.TAKE_SECOND);
        }

        // G095-02: flatten(double[][], true) (rectangular/mildly jagged walk and the sparse jagged walk) and
        // flatten(double[][], value, vertically) toArray()/toDoubleList() fill an exactly sized array; pinned against plain
        // column-major / row-major references, also after skip(k) and partial iteration
        @Test
        public void testFlatten_bulkTerminalsMatchReference() {
            final Random rnd = new Random(9502);

            for (int round = 0; round < 600; round++) {
                final int shape = rnd.nextInt(3);
                final int rows = shape == 2 ? 5 + rnd.nextInt(6) : rnd.nextInt(7);
                final double[][] a = new double[rows][];
                final int width = 1 + rnd.nextInt(6);

                for (int r = 0; r < rows; r++) {
                    if (shape == 1) {
                        a[r] = new double[width];
                        Arrays.fill(a[r], r + 0.5);
                    } else if (shape == 2) {
                        // one long row among null, empty and one-element rows: rows * maxLen > 4 * n selects the jagged walk
                        a[r] = r == rows / 2 ? randomArray(rnd, 40) : rnd.nextInt(3) == 0 ? null : new double[rnd.nextInt(2)];
                    } else {
                        a[r] = rnd.nextInt(5) == 0 ? null : randomArray(rnd, 6);
                    }
                }

                int maxLen = 0;

                for (final double[] row : a) {
                    maxLen = Math.max(maxLen, row == null ? 0 : row.length);
                }

                final double pad = samples[rnd.nextInt(samples.length)];
                final com.landawn.abacus.util.DoubleList vertical = new com.landawn.abacus.util.DoubleList();
                final com.landawn.abacus.util.DoubleList alignedVertical = new com.landawn.abacus.util.DoubleList();
                final com.landawn.abacus.util.DoubleList alignedHorizontal = new com.landawn.abacus.util.DoubleList();

                for (int col = 0; col < maxLen; col++) {
                    for (final double[] row : a) {
                        if (row != null && col < row.length) {
                            vertical.add(row[col]);
                            alignedVertical.add(row[col]);
                        } else {
                            alignedVertical.add(pad);
                        }
                    }
                }

                for (final double[] row : a) {
                    for (int col = 0; col < maxLen; col++) {
                        alignedHorizontal.add(row != null && col < row.length ? row[col] : pad);
                    }
                }

                for (int mode = 0; mode < 3; mode++) {
                    final double[] expected = (mode == 0 ? vertical : mode == 1 ? alignedVertical : alignedHorizontal).toArray();

                    assertArrayEquals(expected, flattenOf(a, mode, pad).toArray());
                    assertArrayEquals(expected, flattenOf(a, mode, pad).toDoubleList().toArray());
                    assertArrayEquals(expected, drain(flattenOf(a, mode, pad).iterator()));

                    final int k = rnd.nextInt(expected.length + 2);
                    assertArrayEquals(suffix(expected, k), flattenOf(a, mode, pad).skip(k).toArray());

                    final com.landawn.abacus.util.DoubleIterator iter = flattenOf(a, mode, pad).iterator();
                    final int consumed = Math.min(k, expected.length);

                    for (int n = 0; n < consumed; n++) {
                        iter.nextDouble();
                    }

                    assertArrayEquals(suffix(expected, consumed), rnd.nextBoolean() ? iter.toArray() : iter.toList().toArray());
                    assertFalse(iter.hasNext());
                    assertEquals(0, iter.toList().size());
                }
            }
        }

        private DoubleStream flattenOf(final double[][] a, final int mode, final double pad) {
            return mode == 0 ? DoubleStream.flatten(a, true) : DoubleStream.flatten(a, pad, mode == 1);
        }

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

        // Built in a separate frame so that only the returned stream references the payload array.
        private DoubleStream concatOverPrivateArray(final int n) {
            final double[] payload = new double[n];

            for (int i = 0; i < n; i++) {
                payload[i] = i % 100;
            }

            payloadRef = new WeakReference<>(payload);

            return DoubleStream.concat(Arrays.asList(DoubleStream.of(payload), null, DoubleStream.of(-1, -2)));
        }

        // Built in a separate frame so that only the returned stream references the sorted() stream (and its sorted copy).
        private DoubleStream sortedThenAppend(final int n) {
            final double[] values = new double[n];

            for (int i = 0; i < n; i++) {
                values[i] = 99 - i % 100;
            }

            final DoubleStream sorted = DoubleStream.of(values).sorted();
            payloadRef = new WeakReference<>(sorted);

            return sorted.append(DoubleStream.of(-1, -2));
        }

        // G095-04: an exhausted (and closed) input of concat(Collection) is no longer reachable from the concatenation while later inputs run
        @Test
        public void testConcat_releasesExhaustedInputWhileLaterInputsRun() {
            final DoubleStream stream = concatOverPrivateArray(1 << 20);
            final boolean[] released = { false };
            final double[] sum = { 0 };
            final List<Double> tail = new ArrayList<>();

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

            double expected = 0;

            for (int i = 0; i < 1 << 20; i++) {
                expected += i % 100;
            }

            assertEquals(expected, sum[0]);
            assertEquals(Arrays.asList(-1.0, -2.0), tail);
            assertTrue(released[0], "the exhausted first input (and its array) is still reachable from the concatenation");
        }

        // G095-04: append(DoubleStream) delegates to concat(this, other): the exhausted sorted() receiver (and its sorted copy) is released too
        @Test
        public void testAppend_releasesExhaustedSortedReceiverWhileAppendedStreamRuns() {
            final DoubleStream stream = sortedThenAppend(1 << 20);
            final boolean[] released = { false };
            final List<Double> seen = new ArrayList<>();

            stream.forEach(x -> {
                if (x == -1 && seen.size() == 3) {
                    released[0] = awaitCleared(payloadRef);
                }

                if (x < 0 || seen.size() < 3) {
                    seen.add(x);
                }
            });

            assertEquals(Arrays.asList(0.0, 0.0, 0.0, -1.0, -2.0), seen);
            assertTrue(released[0], "the exhausted sorted receiver (and its sorted copy) is still reachable from the appended stream");
        }

        // G095-04: element order, null inputs, close order (each exhausted input before the next one is opened, the rest on close),
        // and the caller's collection / varargs array are never modified
        @Test
        public void testConcat_orderNullInputsAndCloseOrder() {
            final List<String> log = new ArrayList<>();
            final DoubleStream a = DoubleStream.of(1, 2).onClose(() -> log.add("closeA"));
            final DoubleStream b = DoubleStream.of(3).onClose(() -> log.add("closeB"));
            final DoubleStream c = DoubleStream.of(4, 5).onClose(() -> log.add("closeC"));
            final DoubleStream d = DoubleStream.of(6).onClose(() -> log.add("closeD"));
            final List<DoubleStream> input = new ArrayList<>(Arrays.asList(a, null, b, null, c, d));
            final DoubleStream stream = DoubleStream.concat(input);
            final DoubleIteratorEx iter = stream.iteratorEx();
            final List<Double> seen = new ArrayList<>();

            while (seen.size() < 4 && iter.hasNext()) {
                final double x = iter.nextDouble();
                seen.add(x);
                log.add("next" + x);
            }

            stream.close();

            assertEquals(Arrays.asList(1.0, 2.0, 3.0, 4.0), seen);
            assertEquals(Arrays.asList("next1.0", "next2.0", "closeA", "next3.0", "closeB", "next4.0", "closeC", "closeD"), log);
            assertEquals(6, input.size());
            assertSame(a, input.get(0));
            assertNull(input.get(1));
            assertSame(b, input.get(2));
            assertNull(input.get(3));
            assertSame(c, input.get(4));
            assertSame(d, input.get(5));
            assertThrows(IllegalStateException.class, a::toArray);
            assertThrows(IllegalStateException.class, d::toArray);

            final DoubleStream e = DoubleStream.of(7, 8);
            final DoubleStream f = DoubleStream.of(9);
            final DoubleStream[] array = { e, null, f };
            assertArrayEquals(new double[] { 7, 8, 9 }, DoubleStream.concat(array).toArray());
            assertSame(e, array[0]);
            assertNull(array[1]);
            assertSame(f, array[2]);

            assertEquals(0, DoubleStream.concat(Arrays.asList((DoubleStream) null, null)).count());
            assertEquals(0, DoubleStream.concat(new ArrayList<DoubleStream>()).count());
            assertArrayEquals(new double[] { 1, 2, 3, 4 }, DoubleStream.of(1, 2).append(DoubleStream.of(3, 4)).toArray());
            assertArrayEquals(new double[] { 3, 4, 1, 2 }, DoubleStream.of(1, 2).prepend(DoubleStream.of(3, 4)).toArray());
            assertEquals(5, DoubleStream.concat(DoubleStream.of(1, 2), DoubleStream.empty(), DoubleStream.of(3, 4, 5)).count());
            assertEquals(com.landawn.abacus.util.DoubleList.of(1, 3, 4),
                    DoubleStream.concat(Arrays.asList(DoubleStream.of(1, 2), DoubleStream.of(3, 4))).filter(x -> x != 2).toDoubleList());
            assertArrayEquals(new double[] { 2, 3 },
                    DoubleStream.concat(Arrays.asList(DoubleStream.of(1, 2), DoubleStream.of(3, 4))).skip(1).limit(2).toArray());
        }

        // G095-04: a failing close of an exhausted input and an already-closed input propagate from hasNext(); a retry continues;
        // the remaining inputs are closed with the concatenation
        @Test
        public void testConcat_failingCloseAndClosedInputThenRetry() {
            final List<String> log = new ArrayList<>();
            final DoubleStream a = DoubleStream.of(1).onClose(() -> {
                log.add("closeA");
                throw new IllegalStateException("closeA failed");
            });
            final DoubleStream closed = DoubleStream.of(99);
            closed.close();
            final DoubleStream c = DoubleStream.of(3).onClose(() -> log.add("closeC"));
            final DoubleStream stream = DoubleStream.concat(Arrays.asList(a, closed, c));
            final DoubleIteratorEx iter = stream.iteratorEx();

            assertEquals(1, iter.nextDouble());
            final IllegalStateException closeFailure = assertThrows(IllegalStateException.class, iter::hasNext);
            assertEquals("closeA failed", closeFailure.getMessage());
            assertEquals(Arrays.asList("closeA"), log);

            // retry: `a` is closed already (no second close call); the next input is closed and is rejected when reached
            final IllegalStateException rejected = assertThrows(IllegalStateException.class, iter::hasNext);
            assertEquals("This stream is already terminated.", rejected.getMessage());
            assertEquals(Arrays.asList("closeA"), log);

            // retry again: traversal continues with the input after the rejected one
            assertTrue(iter.hasNext());
            assertEquals(3, iter.nextDouble());
            assertFalse(iter.hasNext());
            assertThrows(java.util.NoSuchElementException.class, iter::nextDouble);

            stream.close();
            assertEquals(Arrays.asList("closeA", "closeC"), log);

            // the same stream twice: the second occurrence is rejected when it is reached
            final DoubleStream twice = DoubleStream.of(1, 2);
            final List<Double> collected = new ArrayList<>();
            final IllegalStateException repeated = assertThrows(IllegalStateException.class, () -> DoubleStream.concat(twice, twice).forEach(collected::add));
            assertEquals("This stream is already terminated.", repeated.getMessage());
            assertEquals(Arrays.asList(1.0, 2.0), collected);

            // a terminal operation that fails on a closed input closes every other input
            final List<String> log2 = new ArrayList<>();
            final DoubleStream x = DoubleStream.of(1).onClose(() -> log2.add("closeX"));
            final DoubleStream closed2 = DoubleStream.of(2);
            closed2.close();
            final DoubleStream z = DoubleStream.of(3).onClose(() -> log2.add("closeZ"));
            assertThrows(IllegalStateException.class, () -> DoubleStream.concat(x, closed2, z).toArray());
            assertEquals(Arrays.asList("closeX", "closeZ"), log2);

            // a failing close handler of a not yet exhausted input still runs when the concatenation is closed
            final List<String> log3 = new ArrayList<>();
            final DoubleStream p = DoubleStream.of(1, 2).onClose(() -> log3.add("closeP"));
            final DoubleStream q = DoubleStream.of(3).onClose(() -> {
                log3.add("closeQ");
                throw new IllegalStateException("closeQ failed");
            });
            final DoubleStream r = DoubleStream.of(4).onClose(() -> log3.add("closeR"));
            final DoubleStream pqr = DoubleStream.concat(Arrays.asList(p, q, r));
            assertEquals(1, pqr.iteratorEx().nextDouble());
            final IllegalStateException onClose = assertThrows(IllegalStateException.class, pqr::close);
            assertEquals("closeQ failed", onClose.getMessage());
            assertEquals(Arrays.asList("closeP", "closeQ", "closeR"), log3);
        }

        // G095-04: parallel append/concat (the concatenation's iterator is shared by the workers) delivers every element and closes every input
        @Test
        public void testConcat_parallelDeliversAllElementsAndClosesInputs() {
            for (int round = 0; round < 20; round++) {
                final AtomicInteger closes = new AtomicInteger();
                final List<DoubleStream> inputs = new ArrayList<>();
                final com.landawn.abacus.util.DoubleList expected = new com.landawn.abacus.util.DoubleList();

                for (int i = 0; i < 50; i++) {
                    final double[] values = new double[i % 7];

                    for (int j = 0; j < values.length; j++) {
                        values[j] = i + j;
                    }

                    if (i % 10 == 3) {
                        inputs.add(null);
                    } else {
                        inputs.add(DoubleStream.of(values).onClose(closes::incrementAndGet));
                        expected.addAll(values);
                    }
                }

                final double[] actual = DoubleStream.concat(inputs).parallel(4).map(v -> v + 1).toArray();
                final double[] sortedExpected = expected.toArray();

                for (int i = 0; i < sortedExpected.length; i++) {
                    sortedExpected[i] += 1;
                }

                Arrays.sort(sortedExpected);
                Arrays.sort(actual);
                assertArrayEquals(sortedExpected, actual);
                assertEquals(45, closes.get());

                final AtomicInteger closes2 = new AtomicInteger();
                final double[] appended = DoubleStream.of(1, 2, 3)
                        .onClose(closes2::incrementAndGet)
                        .parallel(3)
                        .append(DoubleStream.of(4, 5).onClose(closes2::incrementAndGet))
                        .toArray();
                Arrays.sort(appended);
                assertArrayEquals(new double[] { 1, 2, 3, 4, 5 }, appended);
                assertEquals(2, closes2.get());
            }
        }
    }

    // ---- perf review 2026-10-08 G095 end ----
    // ---- perf review 2026-10-08 G096 begin ----
    @Nested
    class G096 {

        private Map<String, Integer> source() {
            final Map<String, Integer> map = new LinkedHashMap<>();
            map.put("a", 1);
            map.put("b", 2);
            map.put("c", 3);
            return map;
        }

        private Map<String, Integer> bigSource() {
            final Map<String, Integer> map = new LinkedHashMap<>();

            for (int i = 0; i < 60; i++) {
                map.put("k" + i, i + 1);
            }

            return map;
        }

        private Map.Entry<String, Integer> entry(final String key, final Integer value) {
            return new java.util.AbstractMap.SimpleImmutableEntry<>(key, value);
        }

        private <T> Stream<T> track(final Stream<T> stream, final AtomicInteger closeCount, final List<Stream<?>> returned) {
            final Stream<T> result = stream.onClose(closeCount::incrementAndGet);
            returned.add(result);
            return result;
        }

        private List<String> sortedStrings(final List<Map.Entry<String, Integer>> entries) {
            final List<String> result = new ArrayList<>();

            for (final Map.Entry<String, Integer> e : entries) {
                result.add(e.getKey() + "=" + e.getValue());
            }

            Collections.sort(result);
            return result;
        }

        // G096-01: flatMapKey (Function and BiFunction forms): order of mapper calls, inner elements, inner close and outer close
        @Test
        public void testFlatMapKey_sequentialCallAndCloseOrder() {
            final List<String> expected = Arrays.asList("map:a", "elem:a1", "out:a1=1", "elem:a2", "out:a2=1", "close:a", "map:b", "elem:b1", "out:b1=2",
                    "elem:b2", "out:b2=2", "close:b", "map:c", "elem:c1", "out:c1=3", "elem:c2", "out:c2=3", "close:c", "outer-close");

            final List<String> log = new ArrayList<>();
            EntryStream.of(source()).onClose(() -> log.add("outer-close")).flatMapKey(k -> {
                log.add("map:" + k);
                return Stream.of(k + "1", k + "2").peek(x -> log.add("elem:" + x)).onClose(() -> log.add("close:" + k));
            }).forEach((k, v) -> log.add("out:" + k + "=" + v));
            assertEquals(expected, log);

            final List<String> log2 = new ArrayList<>();
            EntryStream.of(source()).onClose(() -> log2.add("outer-close")).flatMapKey((k, v) -> {
                log2.add("map:" + k);
                return Stream.of(k + "1", k + "2").peek(x -> log2.add("elem:" + x)).onClose(() -> log2.add("close:" + k));
            }).forEach((k, v) -> log2.add("out:" + k + "=" + v));
            assertEquals(expected, log2);
        }

        // G096-01: flatMapValue (Function and BiFunction forms): order of mapper calls, inner elements, inner close and outer close
        @Test
        public void testFlatMapValue_sequentialCallAndCloseOrder() {
            final List<String> expected = Arrays.asList("map:1", "elem:10", "out:a=10", "elem:11", "out:a=11", "close:1", "map:2", "elem:20", "out:b=20",
                    "elem:21", "out:b=21", "close:2", "map:3", "elem:30", "out:c=30", "elem:31", "out:c=31", "close:3", "outer-close");

            final List<String> log = new ArrayList<>();
            EntryStream.of(source()).onClose(() -> log.add("outer-close")).flatMapValue(v -> {
                log.add("map:" + v);
                return Stream.of(v * 10, v * 10 + 1).peek(x -> log.add("elem:" + x)).onClose(() -> log.add("close:" + v));
            }).forEach((k, v) -> log.add("out:" + k + "=" + v));
            assertEquals(expected, log);

            final List<String> log2 = new ArrayList<>();
            EntryStream.of(source()).onClose(() -> log2.add("outer-close")).flatMapValue((k, v) -> {
                log2.add("map:" + v);
                return Stream.of(v * 10, v * 10 + 1).peek(x -> log2.add("elem:" + x)).onClose(() -> log2.add("close:" + v));
            }).forEach((k, v) -> log2.add("out:" + k + "=" + v));
            assertEquals(expected, log2);
        }

        // G096-01: every stream returned by the mapper is closed exactly once and cannot be reused (all 4 forms, sequential and parallel)
        @Test
        public void testFlatMapKeyValue_returnedStreamsClosedOnce() {
            for (final boolean parallel : new boolean[] { false, true }) {
                for (int form = 0; form < 4; form++) {
                    final Map<String, Integer> src = bigSource();
                    final AtomicInteger closeCount = new AtomicInteger();
                    final List<Stream<?>> returned = Collections.synchronizedList(new ArrayList<>());
                    final EntryStream<String, Integer> s = parallel ? EntryStream.of(src).parallel(3) : EntryStream.of(src);
                    final List<Map.Entry<String, Integer>> result;
                    final List<String> expected = new ArrayList<>();

                    switch (form) {
                        case 0:
                            result = s.flatMapKey(k -> track(Stream.of(k, k + "'"), closeCount, returned)).toList();
                            break;
                        case 1:
                            result = s.flatMapKey((k, v) -> track(Stream.of(k, k + "'"), closeCount, returned)).toList();
                            break;
                        case 2:
                            result = s.flatMapValue(v -> track(Stream.of(v, -v), closeCount, returned)).toList();
                            break;
                        default:
                            result = s.flatMapValue((k, v) -> track(Stream.of(v, -v), closeCount, returned)).toList();
                            break;
                    }

                    for (final Map.Entry<String, Integer> e : src.entrySet()) {
                        if (form < 2) {
                            expected.add(e.getKey() + "=" + e.getValue());
                            expected.add(e.getKey() + "'=" + e.getValue());
                        } else {
                            expected.add(e.getKey() + "=" + e.getValue());
                            expected.add(e.getKey() + "=" + (-e.getValue()));
                        }
                    }

                    Collections.sort(expected);
                    final String label = "parallel=" + parallel + ", form=" + form;
                    assertEquals(expected, sortedStrings(result), label);
                    assertEquals(src.size(), closeCount.get(), label);
                    assertEquals(src.size(), returned.size(), label);

                    for (final Stream<?> r : returned) {
                        assertThrows(IllegalStateException.class, r::count, label);
                    }

                    assertEquals(src.size(), closeCount.get(), label);
                }
            }
        }

        // G096-01: a short-circuit closes only the inner stream that was opened, exactly once
        @Test
        public void testFlatMapKeyValue_shortCircuitClosesOpenedInnerStreamOnce() {
            final List<String> log = new ArrayList<>();
            assertEquals(entry("a1", 1), EntryStream.of(source()).onClose(() -> log.add("outer-close")).flatMapKey(k -> {
                log.add("map:" + k);
                return Stream.of(k + "1", k + "2").onClose(() -> log.add("close:" + k));
            }).first().get());
            assertEquals(Arrays.asList("map:a", "outer-close", "close:a"), log);

            log.clear();
            assertEquals(Arrays.asList(entry("a", 10)), EntryStream.of(source()).onClose(() -> log.add("outer-close")).flatMapValue((k, v) -> {
                log.add("map:" + k);
                return Stream.of(v * 10, v * 10 + 1).onClose(() -> log.add("close:" + k));
            }).limit(1).toList());
            assertEquals(Arrays.asList("map:a", "outer-close", "close:a"), log);

            // closing the returned EntryStream while an inner stream is being consumed closes that inner stream once
            log.clear();
            final List<Stream<?>> returned = new ArrayList<>();
            final EntryStream<String, Integer> s = EntryStream.of(source()).flatMapKey(k -> {
                log.add("map:" + k);
                final Stream<String> inner = Stream.of(k + "1", k + "2").onClose(() -> log.add("close:" + k));
                returned.add(inner);
                return inner;
            });
            final Iterator<Map.Entry<String, Integer>> iter = s.iterator();
            assertEquals(entry("a1", 1), iter.next());
            s.close();
            s.close();
            assertEquals(Arrays.asList("map:a", "close:a"), log);
            assertEquals(1, returned.size());
            assertThrows(IllegalStateException.class, () -> returned.get(0).count());
        }

        // G096-01: a failing close handler of a returned stream propagates unchanged; it runs once and the outer stream is closed once
        @Test
        public void testFlatMapKeyValue_innerCloseFailurePropagates() {
            final AtomicInteger outerClosed = new AtomicInteger();
            final AtomicInteger innerClosed = new AtomicInteger();
            final IllegalStateException failure = new IllegalStateException("inner-close-failure");
            final List<Stream<?>> returned = new ArrayList<>();

            final IllegalStateException thrown = assertThrows(IllegalStateException.class,
                    () -> EntryStream.of(source()).onClose(outerClosed::incrementAndGet).flatMapKey(k -> {
                        final Stream<String> inner = Stream.of(k).onClose(() -> {
                            innerClosed.incrementAndGet();
                            throw failure;
                        });
                        returned.add(inner);
                        return inner;
                    }).toList());

            assertSame(failure, thrown);
            assertEquals(1, innerClosed.get());
            assertEquals(1, outerClosed.get());
            assertEquals(1, returned.size());
            assertThrows(IllegalStateException.class, () -> returned.get(0).count());
            assertEquals(1, innerClosed.get());
        }

        // G096-01: an already-closed returned stream, or the same stream returned twice, fails as before and closes the outer stream
        @Test
        public void testFlatMapKeyValue_closedOrReusedReturnedStreamFails() {
            final Stream<String> closedStream = Stream.of("x");
            closedStream.close();
            final AtomicInteger outerClosed = new AtomicInteger();
            final IllegalStateException e1 = assertThrows(IllegalStateException.class,
                    () -> EntryStream.of(source()).onClose(outerClosed::incrementAndGet).flatMapKey(k -> closedStream).toList());
            assertEquals("This stream is already terminated.", e1.getMessage());
            assertEquals(1, outerClosed.get());

            final Stream<Integer> shared = Stream.of(7, 8);
            final List<Map.Entry<String, Integer>> seen = new ArrayList<>();
            final AtomicInteger outerClosed2 = new AtomicInteger();
            final IllegalStateException e2 = assertThrows(IllegalStateException.class,
                    () -> EntryStream.of(source()).onClose(outerClosed2::incrementAndGet).flatMapValue((k, v) -> shared).forEach(e -> seen.add(e)));
            assertEquals("This stream is already terminated.", e2.getMessage());
            assertEquals(Arrays.asList(entry("a", 7), entry("a", 8)), seen);
            assertEquals(1, outerClosed2.get());
        }

        // G096-01: handlers of a returned derived stream (and of its parent) run once each, parent first
        @Test
        public void testFlatMapValue_derivedReturnedStreamHandlerOrder() {
            final List<String> log = new ArrayList<>();
            final Stream<Integer> parent = Stream.of(1, 2).onClose(() -> log.add("h1"));
            final Stream<Integer> derived = parent.map(x -> x * 10).onClose(() -> log.add("h2"));

            assertEquals(Arrays.asList(entry("a", 10), entry("a", 20)), EntryStream.of("a", 1).flatMapValue(v -> derived).toList());
            assertEquals(Arrays.asList("h1", "h2"), log);
            assertThrows(IllegalStateException.class, () -> parent.count());
            assertThrows(IllegalStateException.class, () -> derived.count());
            assertEquals(Arrays.asList("h1", "h2"), log);

            // a null result is still an empty expansion, for both the key and the value forms
            assertEquals(Arrays.asList(entry("b", 2)), EntryStream.of(source()).flatMapKey(k -> "b".equals(k) ? Stream.of(k) : (Stream<String>) null).toList());
            assertEquals(Arrays.asList(entry("c", 3)),
                    EntryStream.of(source()).flatMapValue((k, v) -> "c".equals(k) ? Stream.of(v) : (Stream<Integer>) null).toList());
        }
    }

    // ---- perf review 2026-10-08 G096 end ----
    // ---- perf review 2026-10-08 G098 begin ----
    @Nested
    class G098 {
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

        // Built in a separate frame so that only the returned stream references the payload array.
        private FloatStream concatOverPrivateArray(final int n) {
            final float[] payload = new float[n];

            for (int i = 0; i < n; i++) {
                payload[i] = i % 100;
            }

            payloadRef = new WeakReference<>(payload);

            return FloatStream.concat(Arrays.asList(FloatStream.of(payload), null, FloatStream.of(-1f, -2f)));
        }

        // Built in a separate frame so that only the returned stream references the sorted() stream (and its sorted copy).
        private FloatStream sortedThenAppend(final int n) {
            final float[] values = new float[n];

            for (int i = 0; i < n; i++) {
                values[i] = 99 - i % 100;
            }

            final FloatStream sorted = FloatStream.of(values).sorted();
            payloadRef = new WeakReference<>(sorted);

            return sorted.append(FloatStream.of(-1f, -2f));
        }

        // G098-03: an exhausted (and closed) input of concat(Collection) is no longer reachable from the concatenation while later inputs run
        @Test
        void testConcat_releasesExhaustedInputWhileLaterInputsRun() {
            final FloatStream stream = concatOverPrivateArray(1 << 20);
            final boolean[] released = { false };
            final double[] sum = { 0 };
            final List<Float> tail = new ArrayList<>();

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

            double expected = 0;

            for (int i = 0; i < 1 << 20; i++) {
                expected += i % 100;
            }

            assertEquals(expected, sum[0]);
            assertEquals(Arrays.asList(-1f, -2f), tail);
            assertTrue(released[0], "the exhausted first input (and its array) is still reachable from the concatenation");
        }

        // G098-03: append(FloatStream) delegates to concat(this, other): the exhausted sorted() receiver (and its sorted copy) is released too
        @Test
        void testAppend_releasesExhaustedSortedReceiverWhileAppendedStreamRuns() {
            final FloatStream stream = sortedThenAppend(1 << 20);
            final boolean[] released = { false };
            final List<Float> seen = new ArrayList<>();

            stream.forEach(x -> {
                if (x == -1 && seen.size() == 3) {
                    released[0] = awaitCleared(payloadRef);
                }

                if (x < 0 || seen.size() < 3) {
                    seen.add(x);
                }
            });

            assertEquals(Arrays.asList(0f, 0f, 0f, -1f, -2f), seen);
            assertTrue(released[0], "the exhausted sorted receiver (and its sorted copy) is still reachable from the appended stream");
        }

        // G098-03: element order, null inputs, close order (each exhausted input before the next one is opened, the rest on close),
        // and the caller's collection / varargs array are never modified
        @Test
        void testConcat_orderNullInputsAndCloseOrder() {
            final List<String> log = new ArrayList<>();
            final FloatStream a = FloatStream.of(1f, 2f).onClose(() -> log.add("closeA"));
            final FloatStream b = FloatStream.of(3f).onClose(() -> log.add("closeB"));
            final FloatStream c = FloatStream.of(4f, 5f).onClose(() -> log.add("closeC"));
            final FloatStream d = FloatStream.of(6f).onClose(() -> log.add("closeD"));
            final List<FloatStream> input = new ArrayList<>(Arrays.asList(a, null, b, null, c, d));
            final FloatStream stream = FloatStream.concat(input);
            final FloatIteratorEx iter = stream.iteratorEx();
            final List<Float> seen = new ArrayList<>();

            while (seen.size() < 4 && iter.hasNext()) {
                final float x = iter.nextFloat();
                seen.add(x);
                log.add("next" + (int) x);
            }

            stream.close();

            assertEquals(Arrays.asList(1f, 2f, 3f, 4f), seen);
            assertEquals(Arrays.asList("next1", "next2", "closeA", "next3", "closeB", "next4", "closeC", "closeD"), log);
            assertEquals(6, input.size());
            assertSame(a, input.get(0));
            assertNull(input.get(1));
            assertSame(b, input.get(2));
            assertNull(input.get(3));
            assertSame(c, input.get(4));
            assertSame(d, input.get(5));
            assertThrows(IllegalStateException.class, a::toArray);
            assertThrows(IllegalStateException.class, d::toArray);

            final FloatStream e = FloatStream.of(7f, 8f);
            final FloatStream f = FloatStream.of(9f);
            final FloatStream[] array = { e, null, f };
            assertArrayEquals(new float[] { 7, 8, 9 }, FloatStream.concat(array).toArray());
            assertSame(e, array[0]);
            assertNull(array[1]);
            assertSame(f, array[2]);

            assertEquals(0, FloatStream.concat(Arrays.asList((FloatStream) null, null)).count());
            assertEquals(0, FloatStream.concat(new ArrayList<FloatStream>()).count());
            assertArrayEquals(new float[] { 1, 2, 3, 4 }, FloatStream.of(1f, 2f).append(FloatStream.of(3f, 4f)).toArray());
            assertArrayEquals(new float[] { 3, 4, 1, 2 }, FloatStream.of(1f, 2f).prepend(FloatStream.of(3f, 4f)).toArray());
            assertEquals(5, FloatStream.concat(FloatStream.of(1f, 2f), FloatStream.empty(), FloatStream.of(3f, 4f, 5f)).count());
            assertEquals(com.landawn.abacus.util.FloatList.of(1f, 3f, 4f),
                    FloatStream.concat(Arrays.asList(FloatStream.of(1f, 2f), FloatStream.of(3f, 4f))).filter(x -> x != 2).toFloatList());
            assertArrayEquals(new float[] { 2, 3 },
                    FloatStream.concat(Arrays.asList(FloatStream.of(1f, 2f), FloatStream.of(3f, 4f))).skip(1).limit(2).toArray());
            final float[] special = FloatStream.concat(Arrays.asList(FloatStream.of(Float.NaN, -0.0f), null, FloatStream.of(0.0f))).toArray();
            assertEquals(Float.floatToRawIntBits(Float.NaN), Float.floatToRawIntBits(special[0]));
            assertEquals(Float.floatToRawIntBits(-0.0f), Float.floatToRawIntBits(special[1]));
            assertEquals(Float.floatToRawIntBits(0.0f), Float.floatToRawIntBits(special[2]));
        }

        // G098-03: a failing close of an exhausted input and an already-closed input propagate from hasNext(); a retry continues;
        // the remaining inputs are closed with the concatenation
        @Test
        void testConcat_failingCloseAndClosedInputThenRetry() {
            final List<String> log = new ArrayList<>();
            final FloatStream a = FloatStream.of(1f).onClose(() -> {
                log.add("closeA");
                throw new IllegalStateException("closeA failed");
            });
            final FloatStream closed = FloatStream.of(99f);
            closed.close();
            final FloatStream c = FloatStream.of(3f).onClose(() -> log.add("closeC"));
            final FloatStream stream = FloatStream.concat(Arrays.asList(a, closed, c));
            final FloatIteratorEx iter = stream.iteratorEx();

            assertEquals(1f, iter.nextFloat());
            final IllegalStateException closeFailure = assertThrows(IllegalStateException.class, iter::hasNext);
            assertEquals("closeA failed", closeFailure.getMessage());
            assertEquals(Arrays.asList("closeA"), log);

            // retry: `a` is closed already (no second close call); the next input is closed and is rejected when reached
            final IllegalStateException rejected = assertThrows(IllegalStateException.class, iter::hasNext);
            assertEquals("This stream is already terminated.", rejected.getMessage());
            assertEquals(Arrays.asList("closeA"), log);

            // retry again: traversal continues with the input after the rejected one
            assertTrue(iter.hasNext());
            assertEquals(3f, iter.nextFloat());
            assertFalse(iter.hasNext());
            assertThrows(java.util.NoSuchElementException.class, iter::nextFloat);

            stream.close();
            assertEquals(Arrays.asList("closeA", "closeC"), log);

            // the same stream twice: the second occurrence is rejected when it is reached
            final FloatStream twice = FloatStream.of(1f, 2f);
            final List<Float> collected = new ArrayList<>();
            final IllegalStateException repeated = assertThrows(IllegalStateException.class, () -> FloatStream.concat(twice, twice).forEach(collected::add));
            assertEquals("This stream is already terminated.", repeated.getMessage());
            assertEquals(Arrays.asList(1f, 2f), collected);

            // a terminal operation that fails on a closed input closes every other input
            final List<String> log2 = new ArrayList<>();
            final FloatStream x = FloatStream.of(1f).onClose(() -> log2.add("closeX"));
            final FloatStream closed2 = FloatStream.of(2f);
            closed2.close();
            final FloatStream z = FloatStream.of(3f).onClose(() -> log2.add("closeZ"));
            assertThrows(IllegalStateException.class, () -> FloatStream.concat(x, closed2, z).toArray());
            assertEquals(Arrays.asList("closeX", "closeZ"), log2);

            // a failing close handler of a not yet exhausted input still runs when the concatenation is closed
            final List<String> log3 = new ArrayList<>();
            final FloatStream p = FloatStream.of(1f, 2f).onClose(() -> log3.add("closeP"));
            final FloatStream q = FloatStream.of(3f).onClose(() -> {
                log3.add("closeQ");
                throw new IllegalStateException("closeQ failed");
            });
            final FloatStream r = FloatStream.of(4f).onClose(() -> log3.add("closeR"));
            final FloatStream pqr = FloatStream.concat(Arrays.asList(p, q, r));
            assertEquals(1f, pqr.iteratorEx().nextFloat());
            final IllegalStateException onClose = assertThrows(IllegalStateException.class, pqr::close);
            assertEquals("closeQ failed", onClose.getMessage());
            assertEquals(Arrays.asList("closeP", "closeQ", "closeR"), log3);
        }

        // G098-03: parallel append/concat (the concatenation's iterator is shared by the workers) delivers every element and closes every input
        @Test
        void testConcat_parallelDeliversAllElementsAndClosesInputs() {
            for (int round = 0; round < 20; round++) {
                final AtomicInteger closes = new AtomicInteger();
                final List<FloatStream> inputs = new ArrayList<>();
                final List<Float> expected = new ArrayList<>();

                for (int i = 0; i < 50; i++) {
                    final float[] values = new float[i % 7];

                    for (int j = 0; j < values.length; j++) {
                        values[j] = i * 10 + j;
                    }

                    if (i % 10 == 3) {
                        inputs.add(null);
                    } else {
                        inputs.add(FloatStream.of(values).onClose(closes::incrementAndGet));

                        for (final float v : values) {
                            expected.add(v);
                        }
                    }
                }

                final float[] actual = FloatStream.concat(inputs).parallel(4).map(v -> v + 1).toArray();
                final float[] sortedExpected = new float[expected.size()];

                for (int i = 0; i < sortedExpected.length; i++) {
                    sortedExpected[i] = expected.get(i) + 1;
                }

                Arrays.sort(sortedExpected);
                Arrays.sort(actual);
                assertArrayEquals(sortedExpected, actual);
                assertEquals(45, closes.get());

                final AtomicInteger closes2 = new AtomicInteger();
                final float[] appended = FloatStream.of(1f, 2f, 3f)
                        .onClose(closes2::incrementAndGet)
                        .parallel(3)
                        .append(FloatStream.of(4f, 5f).onClose(closes2::incrementAndGet))
                        .toArray();
                Arrays.sort(appended);
                assertArrayEquals(new float[] { 1, 2, 3, 4, 5 }, appended);
                assertEquals(2, closes2.get());
            }
        }
    }

    // ---- perf review 2026-10-08 G098 end ----
    // ---- perf review 2026-10-08 G119 begin ----
    @Nested
    class G119 {
        private static final int OP_STEP = 0;
        private static final int OP_SKIP = 1;
        private static final int OP_LIMIT = 2;

        private final long[] interestingCounts = { 0, 1, 2, 3, 5, 7, 31, 64, Integer.MAX_VALUE, Integer.MAX_VALUE + 1L, Long.MAX_VALUE };

        private long randomCount(final Random random, final int size) {
            return random.nextInt(4) == 0 ? interestingCounts[random.nextInt(interestingCounts.length)] : random.nextInt(size + 4);
        }

        private List<long[]> randomTail(final Random random, final int size) {
            final List<long[]> ops = new ArrayList<>();
            final int opCount = random.nextInt(4);

            for (int i = 0; i < opCount; i++) {
                final int kind = random.nextInt(3);
                final long arg = kind == OP_STEP ? 1 + random.nextInt(5) : randomCount(random, size);
                ops.add(new long[] { kind, arg });
            }

            return ops;
        }

        private List<Integer> applyOracle(final List<Integer> positions, final List<long[]> ops) {
            List<Integer> cur = positions;

            for (final long[] op : ops) {
                final List<Integer> next = new ArrayList<>();

                if (op[0] == OP_STEP) {
                    for (long i = 0; i < cur.size(); i += op[1]) {
                        next.add(cur.get((int) i));
                    }
                } else if (op[0] == OP_SKIP) {
                    next.addAll(cur.subList((int) Math.min(op[1], cur.size()), cur.size()));
                } else {
                    next.addAll(cur.subList(0, (int) Math.min(op[1], cur.size())));
                }

                cur = next;
            }

            return cur;
        }

        private List<Integer> range(final int fromIndex, final int toIndex) {
            final List<Integer> result = new ArrayList<>();

            for (int i = fromIndex; i < toIndex; i++) {
                result.add(i);
            }

            return result;
        }

        private ShortStream applyShort(ShortStream s, final List<long[]> ops) {
            for (final long[] op : ops) {
                s = op[0] == OP_STEP ? s.step(op[1]) : op[0] == OP_SKIP ? s.skip(op[1]) : s.limit(op[1]);
            }

            return s;
        }

        private IntStream applyInt(IntStream s, final List<long[]> ops) {
            for (final long[] op : ops) {
                s = op[0] == OP_STEP ? s.step(op[1]) : op[0] == OP_SKIP ? s.skip(op[1]) : s.limit(op[1]);
            }

            return s;
        }

        private String describe(final int length, final int fromIndex, final int toIndex, final String head, final List<long[]> ops) {
            final StringBuilder sb = new StringBuilder("len=" + length + " range=[" + fromIndex + "," + toIndex + ") " + head);

            for (final long[] op : ops) {
                sb.append(op[0] == OP_STEP ? ".step(" : op[0] == OP_SKIP ? ".skip(" : ".limit(").append(op[1]).append(')');
            }

            return sb.toString();
        }

        private short[] randomShorts(final Random random, final int length) {
            final short[] a = new short[length];

            for (int i = 0; i < length; i++) {
                a[i] = random.nextInt(8) == 0 ? (random.nextBoolean() ? Short.MIN_VALUE : Short.MAX_VALUE) : (short) random.nextInt();
            }

            return a;
        }

        // G119-01: step(k) on short array streams followed by random skip/limit/step chains matches an index oracle
        @Test
        void testStepSkip_matchesOracle() {
            final Random random = new Random(20261008119L);

            for (int round = 0; round < 2500; round++) {
                final int length = random.nextInt(4) == 0 ? 0 : random.nextInt(40);
                final int fromIndex = length == 0 ? 0 : random.nextInt(length + 1);
                final int toIndex = fromIndex + (length == fromIndex ? 0 : random.nextInt(length - fromIndex + 1));
                final long step = random.nextInt(8) == 0 ? (random.nextBoolean() ? Integer.MAX_VALUE + 7L : Long.MAX_VALUE) : 2 + random.nextInt(6);
                final List<long[]> ops = new ArrayList<>();
                ops.add(new long[] { OP_STEP, step });
                ops.addAll(randomTail(random, length));
                ops.add(1, new long[] { OP_SKIP, randomCount(random, length) });

                final List<Integer> expected = applyOracle(range(fromIndex, toIndex), ops);
                final String msg = describe(length, fromIndex, toIndex, "step-first", ops);
                final short[] shorts = randomShorts(random, length);
                final short[] expectedShorts = new short[expected.size()];
                int expectedSum = 0;

                for (int i = 0; i < expected.size(); i++) {
                    expectedShorts[i] = shorts[expected.get(i)];
                    expectedSum += expectedShorts[i];
                }

                assertArrayEquals(expectedShorts, applyShort(ShortStream.of(shorts, fromIndex, toIndex), ops).toArray(), msg);
                assertEquals(expected.size(), applyShort(ShortStream.of(shorts, fromIndex, toIndex), ops).count(), msg);
                assertEquals(expectedSum, applyShort(ShortStream.of(shorts, fromIndex, toIndex), ops).sum(), msg);

                final int position = random.nextInt(expected.size() + 3);
                final com.landawn.abacus.util.u.OptionalShort element = applyShort(ShortStream.of(shorts, fromIndex, toIndex), ops).elementAt(position);
                assertEquals(position < expected.size(), element.isPresent(), msg + " elementAt(" + position + ")");

                if (position < expected.size()) {
                    assertEquals(expectedShorts[position], element.get(), msg + " elementAt(" + position + ")");
                }

                // iteratorEx() is what iterator() returns, minus its close-handler warning
                final ShortIteratorEx iter = applyShort(ShortStream.of(shorts, fromIndex, toIndex), ops).iteratorEx();

                for (int i = 0; i < expected.size(); i++) {
                    assertTrue(iter.hasNext(), msg);
                    assertTrue(iter.hasNext(), msg);
                    assertEquals(expectedShorts[i], iter.nextShort(), msg);
                }

                assertFalse(iter.hasNext(), msg);
                assertThrows(java.util.NoSuchElementException.class, iter::nextShort, msg);
            }
        }

        // G119-01: asIntStream() of short array streams followed by random skip/limit/step chains matches an index oracle
        @Test
        void testAsIntStreamSkip_matchesOracle() {
            final Random random = new Random(1190081026L);

            for (int round = 0; round < 2500; round++) {
                final int length = random.nextInt(4) == 0 ? 0 : random.nextInt(40);
                final int fromIndex = length == 0 ? 0 : random.nextInt(length + 1);
                final int toIndex = fromIndex + (length == fromIndex ? 0 : random.nextInt(length - fromIndex + 1));
                final List<long[]> ops = new ArrayList<>();
                ops.add(new long[] { OP_SKIP, randomCount(random, length) });
                ops.addAll(randomTail(random, length));

                if (random.nextBoolean()) {
                    ops.add(random.nextInt(ops.size() + 1), new long[] { OP_SKIP, randomCount(random, length) });
                }

                final List<Integer> expected = applyOracle(range(fromIndex, toIndex), ops);
                final String msg = describe(length, fromIndex, toIndex, "asIntStream()", ops);
                final short[] shorts = randomShorts(random, length);
                final int[] expectedInts = new int[expected.size()];
                long expectedSum = 0;

                for (int i = 0; i < expected.size(); i++) {
                    expectedInts[i] = shorts[expected.get(i)];
                    expectedSum += expectedInts[i];
                }

                assertArrayEquals(expectedInts, applyInt(ShortStream.of(shorts, fromIndex, toIndex).asIntStream(), ops).toArray(), msg);
                assertEquals(expected.size(), applyInt(ShortStream.of(shorts, fromIndex, toIndex).asIntStream(), ops).count(), msg);
                assertEquals(expectedSum, applyInt(ShortStream.of(shorts, fromIndex, toIndex).asIntStream(), ops).sum(), msg);

                final int position = random.nextInt(expected.size() + 3);
                final com.landawn.abacus.util.u.OptionalInt element = applyInt(ShortStream.of(shorts, fromIndex, toIndex).asIntStream(), ops)
                        .elementAt(position);
                assertEquals(position < expected.size(), element.isPresent(), msg + " elementAt(" + position + ")");

                if (position < expected.size()) {
                    assertEquals(expectedInts[position], element.get(), msg + " elementAt(" + position + ")");
                }

                // iteratorEx() is what iterator() returns, minus its close-handler warning
                final IntIteratorEx iter = applyInt(ShortStream.of(shorts, fromIndex, toIndex).asIntStream(), ops).iteratorEx();

                for (int i = 0; i < expected.size(); i++) {
                    assertTrue(iter.hasNext(), msg);
                    assertTrue(iter.hasNext(), msg);
                    assertEquals(expectedInts[i], iter.nextInt(), msg);
                }

                assertFalse(iter.hasNext(), msg);
                assertThrows(java.util.NoSuchElementException.class, iter::nextInt, msg);
            }
        }

        // G119-01: hand-picked boundaries of skip/limit/elementAt after step()/asIntStream(): exact remaining count, one past it, huge counts
        @Test
        void testStepSkip_boundaries() {
            final short[] shorts = { 10, -20, 30, Short.MIN_VALUE, 50, Short.MAX_VALUE, 70 };

            assertArrayEquals(new short[] { 50, 70 }, ShortStream.of(shorts).step(2).skip(2).toArray());
            assertArrayEquals(new short[] { 70 }, ShortStream.of(shorts).step(2).skip(3).toArray());
            assertArrayEquals(new short[0], ShortStream.of(shorts).step(2).skip(4).toArray());
            assertArrayEquals(new short[0], ShortStream.of(shorts).step(2).skip(Long.MAX_VALUE).toArray());
            assertArrayEquals(new short[] { Short.MIN_VALUE }, ShortStream.of(shorts, 1, 6).step(2).skip(1).limit(1).toArray());
            assertArrayEquals(new short[] { -20, 50 }, ShortStream.of(shorts, 1, 7).step(3).toArray());
            assertArrayEquals(new short[] { 50 }, ShortStream.of(shorts, 1, 7).step(3).skip(1).toArray());
            assertArrayEquals(new short[] { 10 }, ShortStream.of(shorts).step(Long.MAX_VALUE).toArray());
            assertArrayEquals(new short[0], ShortStream.of(shorts).step(Long.MAX_VALUE).skip(1).toArray());
            assertArrayEquals(new short[] { 70 }, ShortStream.of(shorts).step(3).skip(1).skip(1).toArray());
            assertEquals(1, ShortStream.of(shorts).step(2).skip(1).limit(2).skip(1).count());
            assertEquals(0, ShortStream.of(shorts).step(2).skip(1).limit(5).skip(Integer.MAX_VALUE + 1L).sum());
            assertEquals(50 + 70, ShortStream.of(shorts).step(2).skip(1).limit(5).skip(1).sum());
            assertArrayEquals(new short[0], ShortStream.empty().step(2).skip(1).toArray());
            assertEquals(50, ShortStream.of(shorts).step(2).elementAt(2).get());
            assertEquals(70, ShortStream.of(shorts).step(2).elementAt(3).get());
            assertFalse(ShortStream.of(shorts).step(2).elementAt(4).isPresent());
            assertFalse(ShortStream.of(shorts).step(2).elementAt(Long.MAX_VALUE).isPresent());
            assertEquals(Short.MAX_VALUE, ShortStream.of(shorts).step(5).elementAt(1).get());

            assertArrayEquals(new int[] { 50, Short.MAX_VALUE, 70 }, ShortStream.of(shorts).asIntStream().skip(4).toArray());
            assertArrayEquals(new int[] { 70 }, ShortStream.of(shorts).asIntStream().skip(6).toArray());
            assertArrayEquals(new int[0], ShortStream.of(shorts).asIntStream().skip(7).toArray());
            assertArrayEquals(new int[0], ShortStream.of(shorts).asIntStream().skip(Long.MAX_VALUE).toArray());
            assertArrayEquals(new int[] { Short.MIN_VALUE, 50 }, ShortStream.of(shorts, 2, 5).asIntStream().skip(1).toArray());
            assertArrayEquals(new int[] { -20, Short.MAX_VALUE }, ShortStream.of(shorts, 1, 7).asIntStream().step(4).toArray());
            assertArrayEquals(new int[] { 50 }, ShortStream.of(shorts).asIntStream().skip(1).step(3).skip(1).limit(1).toArray());
            assertArrayEquals(new int[0], ShortStream.of(shorts, 3, 3).asIntStream().skip(1).toArray());
            assertEquals(0, ShortStream.of(shorts).asIntStream().limit(3).skip(5).count());
            assertEquals(2, ShortStream.of(shorts).asIntStream().skip(5).count());
            assertEquals(Short.MIN_VALUE + 50 + Short.MAX_VALUE + 70, ShortStream.of(shorts).asIntStream().skip(3).sum());
            assertEquals(Short.MIN_VALUE, ShortStream.of(shorts).asIntStream().elementAt(3).get());
            assertEquals(70, ShortStream.of(shorts).asIntStream().elementAt(6).get());
            assertFalse(ShortStream.of(shorts).asIntStream().elementAt(7).isPresent());
            assertFalse(ShortStream.of(shorts).asIntStream().elementAt(Long.MAX_VALUE).isPresent());
        }

        // G119-01: a skip() over a partially consumed limit() keeps the same position; the source is read lazily, on first access
        @Test
        void testStepSkip_iteratorProtocol() {
            final short[] shorts = { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11 };

            final ShortIteratorEx stepIter = ShortStream.of(shorts).step(2).iteratorEx();
            assertEquals(1, stepIter.nextShort());
            stepIter.advance(2);
            assertEquals(7, stepIter.nextShort());
            stepIter.advance(Long.MAX_VALUE);
            assertFalse(stepIter.hasNext());
            assertThrows(java.util.NoSuchElementException.class, stepIter::nextShort);

            final IntIteratorEx intIter = ShortStream.of(shorts, 2, 9).asIntStream().skip(2).limit(3).iteratorEx();
            assertEquals(5, intIter.nextInt());
            intIter.advance(1);
            assertEquals(7, intIter.nextInt());
            assertFalse(intIter.hasNext());
            intIter.advance(5);
            assertFalse(intIter.hasNext());
            assertThrows(java.util.NoSuchElementException.class, intIter::nextInt);

            final ShortIteratorEx limitIter = ShortStream.of(shorts).step(3).limit(3).iteratorEx();
            assertEquals(1, limitIter.nextShort());
            limitIter.advance(10);
            assertFalse(limitIter.hasNext());
            assertEquals(0, limitIter.count());

            final AtomicInteger closed = new AtomicInteger();
            assertEquals(3, ShortStream.of(shorts).onClose(closed::incrementAndGet).step(2).skip(3).count());
            assertEquals(1, closed.get());
            assertEquals(3, ShortStream.of(shorts).onClose(closed::incrementAndGet).asIntStream().skip(8).count());
            assertEquals(2, closed.get());
        }

        // G119-01: parallel array streams inherit step()/asIntStream(); skip after them still yields the same elements
        @Test
        void testAsIntStreamSkip_parallel() {
            final short[] shorts = new short[2000];

            for (int i = 0; i < shorts.length; i++) {
                shorts[i] = (short) (i * 37);
            }

            final int[] expectedInts = new int[1990];

            for (int i = 0; i < 1990; i++) {
                expectedInts[i] = shorts[i + 10];
            }

            final int[] actualInts = ShortStream.of(shorts).parallel(3).asIntStream().skip(10).toArray();
            Arrays.sort(expectedInts);
            Arrays.sort(actualInts);
            assertArrayEquals(expectedInts, actualInts);
            assertEquals(1990, ShortStream.of(shorts).parallel(3).asIntStream().skip(10).count());

            final short[] expectedShorts = new short[400];

            for (int i = 0; i < 400; i++) {
                expectedShorts[i] = shorts[1200 + 2 * i];
            }

            final short[] actualShorts = ShortStream.of(shorts).parallel(3).step(2).skip(600).toArray();
            Arrays.sort(expectedShorts);
            Arrays.sort(actualShorts);
            assertArrayEquals(expectedShorts, actualShorts);
            assertEquals(400, ShortStream.of(shorts).parallel(3).step(2).skip(600).count());
            assertEquals(332, ShortStream.of(shorts).parallel(3).step(3).skip(335).count());
        }

        // G119-01 (guard, RED on base): the step()/asIntStream() iterators opt into bulk advance, so skip()/limit() skip in O(1)
        @Test
        void testStepAndAsIntStream_optIntoBulkAdvance() {
            final short[] shorts = { 1, 2, 3, 4, 5 };

            assertTrue(ShortStream.of(shorts).step(2).iteratorEx().supportsFailureAtomicAdvance());
            assertTrue(ShortStream.of(shorts).step(2).skip(1).iteratorEx().supportsFailureAtomicAdvance());
            assertTrue(ShortStream.of(shorts).step(2).limit(2).iteratorEx().supportsFailureAtomicAdvance());
            assertTrue(ShortStream.of(shorts).asIntStream().iteratorEx().supportsFailureAtomicAdvance());
            assertTrue(ShortStream.of(shorts).asIntStream().skip(1).limit(2).iteratorEx().supportsFailureAtomicAdvance());
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
    // ---- perf review 2026-10-08 G121 end ----

    // ---- perf review 2026-10-08 V6 begin ----
    @Nested
    class V6 {
        private Stream<String> flat(final String kind, final boolean iteratorSource, final List<String> log) {
            final Runnable sourceClose = () -> log.add("source");

            switch (kind) {
                case "short": {
                    final ShortStream s = ShortStream.of((short) 1, (short) 2, (short) 3);
                    return (iteratorSource ? s.map(x -> x) : s).onClose(sourceClose).flatMapArrayToObj(v -> {
                        log.add("map" + v);
                        return new String[] { "a" + v, "b" + v };
                    });
                }
                case "int": {
                    final IntStream s = IntStream.of(1, 2, 3);
                    return (iteratorSource ? s.map(x -> x) : s).onClose(sourceClose).flatMapArrayToObj(v -> {
                        log.add("map" + v);
                        return new String[] { "a" + v, "b" + v };
                    });
                }
                case "long": {
                    final LongStream s = LongStream.of(1, 2, 3);
                    return (iteratorSource ? s.map(x -> x) : s).onClose(sourceClose).flatMapArrayToObj(v -> {
                        log.add("map" + v);
                        return new String[] { "a" + v, "b" + v };
                    });
                }
                default: {
                    final ByteStream s = ByteStream.of((byte) 1, (byte) 2, (byte) 3);
                    return (iteratorSource ? s.map(x -> x) : s).onClose(sourceClose).flatMapArrayToObj(v -> {
                        log.add("map" + v);
                        return new String[] { "a" + v, "b" + v };
                    });
                }
            }
        }

        // V6 (G085-01 amendment): closing the result stream mid-way drops the rest of the current mapped array, as
        // flatMapToObj(t -> Stream.of(mapper.apply(t))) did (its close handler closed the current mapped stream) and as
        // the byte/char/float/double siblings (G084-01) still do. An iterator taken before close() continues with the
        // next source element.
        @Test
        public void testFlatMapArrayToObj_closeMidwayDropsTheRestOfTheCurrentArray() {
            for (final String kind : new String[] { "short", "int", "long", "byte" }) {
                for (final boolean iteratorSource : new boolean[] { false, true }) {
                    final String label = kind + "/" + (iteratorSource ? "iterator" : "array");
                    final List<String> log = new ArrayList<>();
                    final Stream<String> stream = flat(kind, iteratorSource, log);
                    final Iterator<String> iter = stream.iterator();

                    assertEquals("a1", iter.next(), label);
                    stream.close();
                    assertEquals(Arrays.asList("map1", "source"), log, label);

                    final List<String> rest = new ArrayList<>();
                    iter.forEachRemaining(rest::add);
                    assertEquals(Arrays.asList("a2", "b2", "a3", "b3"), rest, label);
                    assertEquals(Arrays.asList("map1", "source", "map2", "map3"), log, label);
                }
            }
        }
    }
    // ---- perf review 2026-10-08 V6 end ----

    // ---- perf review 2026-10-08 V9 begin ----
    @Nested
    class V9 {
        private final List<String> log = new ArrayList<>();
        private final Runnable[] reenter = new Runnable[1];

        private Runnable closeFirst() {
            return () -> {
                log.add("closeA");
                reenter[0].run();
                log.add("reentered");
            };
        }

        private Runnable closeLog(final String name) {
            return () -> log.add(name);
        }

        private List<String> expected(final String first, final String third) {
            return Arrays.asList("closeA", "reentered", "got=[" + first + ", " + third + "]", "closeB", "closeC", "closeOuter");
        }

        /**
         * The close handler of the first input re-enters the iterator of the concatenation (hasNext() moves it on), so the second
         * input is skipped. It must still be closed when the concatenation is closed, as before the retention fix.
         */
        @Test
        public void testConcat_closeHandlerReenteringIteratorStillClosesEveryInput() {
            {
                log.clear();
                final Stream<Integer> s = Stream
                        .concat(Arrays.asList(Stream.of(1).onClose(closeFirst()), Stream.of(2).onClose(closeLog("closeB")),
                                Stream.of(3).onClose(closeLog("closeC"))))
                        .onClose(closeLog("closeOuter"));
                final Iterator<Integer> it = s.iterator();
                reenter[0] = it::hasNext;
                final List<Object> got = new ArrayList<>();
                while (it.hasNext()) {
                    got.add(it.next());
                }
                log.add("got=" + got);
                s.close();
                assertEquals(expected("1", "3"), log);
            }
            {
                log.clear();
                final IntStream s = IntStream.concat(Arrays.asList(IntStream.of(1).onClose(closeFirst()), IntStream.of(2).onClose(closeLog("closeB")),
                        IntStream.of(3).onClose(closeLog("closeC")))).onClose(closeLog("closeOuter"));
                final var it = s.iterator();
                reenter[0] = it::hasNext;
                final List<Object> got = new ArrayList<>();
                while (it.hasNext()) {
                    got.add(it.nextInt());
                }
                log.add("got=" + got);
                s.close();
                assertEquals(expected("1", "3"), log);
            }
            {
                log.clear();
                final LongStream s = LongStream.concat(Arrays.asList(LongStream.of(1L).onClose(closeFirst()), LongStream.of(2L).onClose(closeLog("closeB")),
                        LongStream.of(3L).onClose(closeLog("closeC")))).onClose(closeLog("closeOuter"));
                final var it = s.iterator();
                reenter[0] = it::hasNext;
                final List<Object> got = new ArrayList<>();
                while (it.hasNext()) {
                    got.add(it.nextLong());
                }
                log.add("got=" + got);
                s.close();
                assertEquals(expected("1", "3"), log);
            }
            {
                log.clear();
                final ShortStream s = ShortStream.concat(Arrays.asList(ShortStream.of((short) 1).onClose(closeFirst()),
                        ShortStream.of((short) 2).onClose(closeLog("closeB")), ShortStream.of((short) 3).onClose(closeLog("closeC"))))
                        .onClose(closeLog("closeOuter"));
                final var it = s.iterator();
                reenter[0] = it::hasNext;
                final List<Object> got = new ArrayList<>();
                while (it.hasNext()) {
                    got.add(it.nextShort());
                }
                log.add("got=" + got);
                s.close();
                assertEquals(expected("1", "3"), log);
            }
            {
                log.clear();
                final ByteStream s = ByteStream.concat(Arrays.asList(ByteStream.of((byte) 1).onClose(closeFirst()),
                        ByteStream.of((byte) 2).onClose(closeLog("closeB")), ByteStream.of((byte) 3).onClose(closeLog("closeC"))))
                        .onClose(closeLog("closeOuter"));
                final var it = s.iterator();
                reenter[0] = it::hasNext;
                final List<Object> got = new ArrayList<>();
                while (it.hasNext()) {
                    got.add(it.nextByte());
                }
                log.add("got=" + got);
                s.close();
                assertEquals(expected("1", "3"), log);
            }
            {
                log.clear();
                final CharStream s = CharStream.concat(Arrays.asList(CharStream.of('a').onClose(closeFirst()), CharStream.of('b').onClose(closeLog("closeB")),
                        CharStream.of('c').onClose(closeLog("closeC")))).onClose(closeLog("closeOuter"));
                final var it = s.iterator();
                reenter[0] = it::hasNext;
                final List<Object> got = new ArrayList<>();
                while (it.hasNext()) {
                    got.add(it.nextChar());
                }
                log.add("got=" + got);
                s.close();
                assertEquals(expected("a", "c"), log);
            }
            {
                log.clear();
                final FloatStream s = FloatStream.concat(Arrays.asList(FloatStream.of(1f).onClose(closeFirst()), FloatStream.of(2f).onClose(closeLog("closeB")),
                        FloatStream.of(3f).onClose(closeLog("closeC")))).onClose(closeLog("closeOuter"));
                final var it = s.iterator();
                reenter[0] = it::hasNext;
                final List<Object> got = new ArrayList<>();
                while (it.hasNext()) {
                    got.add(it.nextFloat());
                }
                log.add("got=" + got);
                s.close();
                assertEquals(expected("1.0", "3.0"), log);
            }
            {
                log.clear();
                final DoubleStream s = DoubleStream.concat(Arrays.asList(DoubleStream.of(1d).onClose(closeFirst()),
                        DoubleStream.of(2d).onClose(closeLog("closeB")), DoubleStream.of(3d).onClose(closeLog("closeC")))).onClose(closeLog("closeOuter"));
                final var it = s.iterator();
                reenter[0] = it::hasNext;
                final List<Object> got = new ArrayList<>();
                while (it.hasNext()) {
                    got.add(it.nextDouble());
                }
                log.add("got=" + got);
                s.close();
                assertEquals(expected("1.0", "3.0"), log);
            }
        }

        /**
         * A zipFunction or nextSelector that consumes the iterator of its own stream re-entrantly (once, on its first call): the bulk
         * toList()/toArray() must end where the inherited hasNext()-driven loop ends instead of failing with NoSuchElementException.
         */
        @Test
        public void testZipAndMergeArrays_reentrantCallbackEndsBulkTerminalEarly() {
            final double[] a = { 1, 2, 3, 4, 5 };
            final double[] b = { 10, 20, 30, 40, 50 };
            final double[] c = { 100, 200, 300, 400, 500 };
            final Runnable[] consume = new Runnable[1];
            final boolean[] once = new boolean[1];

            once[0] = true;
            DoubleStream s = DoubleStream.zip(a, b, (x, y) -> {
                if (once[0]) {
                    once[0] = false;
                    consume[0].run();
                }
                return x + y;
            });
            var it = s.iterator();
            consume[0] = it::nextDouble;
            assertArrayEquals(new double[] { 11, 33, 44, 55 }, it.toArray());
            assertFalse(it.hasNext());

            once[0] = true;
            s = DoubleStream.zip(a, b, (x, y) -> {
                if (once[0]) {
                    once[0] = false;
                    consume[0].run();
                }
                return x + y;
            });
            it = s.iterator();
            consume[0] = it::nextDouble;
            assertArrayEquals(new double[] { 11, 33, 44, 55 }, s.toDoubleList().toArray());

            once[0] = true;
            s = DoubleStream.zip(a, b, c, (x, y, z) -> {
                if (once[0]) {
                    once[0] = false;
                    consume[0].run();
                }
                return x + y + z;
            });
            it = s.iterator();
            consume[0] = it::nextDouble;
            assertArrayEquals(new double[] { 111, 333, 444, 555 }, it.toArray());

            once[0] = true;
            s = DoubleStream.zip(a, new double[] { 7 }, -1, -2, (x, y) -> {
                if (once[0]) {
                    once[0] = false;
                    consume[0].run();
                }
                return x + y;
            });
            it = s.iterator();
            consume[0] = it::nextDouble;
            assertArrayEquals(new double[] { 8, 1, 2, 3 }, it.toArray());

            once[0] = true;
            s = DoubleStream.zip(a, new double[] { 7 }, new double[0], -1, -2, -3, (x, y, z) -> {
                if (once[0]) {
                    once[0] = false;
                    consume[0].run();
                }
                return x + y + z;
            });
            it = s.iterator();
            consume[0] = it::nextDouble;
            assertArrayEquals(new double[] { 5, -2, -1, 0 }, it.toArray());

            once[0] = true;
            s = DoubleStream.merge(new double[] { 1, 3, 5 }, new double[] { 2, 4, 6 }, (x, y) -> {
                if (once[0]) {
                    once[0] = false;
                    consume[0].run();
                }
                return x <= y ? com.landawn.abacus.util.MergeResult.TAKE_FIRST : com.landawn.abacus.util.MergeResult.TAKE_SECOND;
            });
            it = s.iterator();
            consume[0] = it::nextDouble;
            assertArrayEquals(new double[] { 3, 2, 4, 5, 6 }, it.toArray());

            // A nested bulk terminal drains the rest; the outer one then ends after its first element.
            final List<double[]> inner = new ArrayList<>();
            once[0] = true;
            s = DoubleStream.zip(a, b, (x, y) -> {
                if (once[0]) {
                    once[0] = false;
                    consume[0].run();
                }
                return x + y;
            });
            final var it2 = s.iterator();
            consume[0] = () -> inner.add(it2.toList().toArray());
            assertArrayEquals(new double[] { 11 }, it2.toList().toArray());
            assertEquals(1, inner.size());
            assertArrayEquals(new double[] { 22, 33, 44, 55 }, inner.get(0));
        }
    }
    // ---- perf review 2026-10-08 V9 end ----
}

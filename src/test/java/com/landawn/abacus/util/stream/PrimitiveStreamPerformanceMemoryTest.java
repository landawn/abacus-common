package com.landawn.abacus.util.stream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

/** Stream performance and memory regression contracts. */
@Tag("slow-test")
public class PrimitiveStreamPerformanceMemoryTest extends TestBase {

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
}

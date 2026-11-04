package com.landawn.abacus.util.stream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.NoSuchElementException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.util.LongIterator;

/** LongStream general contracts. */
public class LongStreamTest extends LongStreamTestSupport {

    @Test
    public void testChainedOperations() {
        long result = LongStream.range(1L, 101L).filter(n -> n % 2 == 0).map(n -> n * 2).limit(10).sum();

        assertEquals(220L, result);
    }

    @Test
    public void testDefaultIfEmpty() {
        assertArrayEquals(new long[] { 1L, 2L, 3L }, createLongStream(1L, 2L, 3L).defaultIfEmpty(() -> createLongStream(10L, 20L)).toArray());
        assertArrayEquals(new long[] { 10L, 20L }, LongStream.empty().defaultIfEmpty(() -> createLongStream(10L, 20L)).toArray());
    }

    @Test
    public void testNullHandlingInFilter() {
        LongStream stream = LongStream.of(1L, 2L, 3L);
        assertEquals(3, stream.filter(n -> n != 0).count());
    }

    @Test
    public void testIfEmpty() {
        AtomicBoolean called = new AtomicBoolean(false);
        LongStream.empty().ifEmpty(() -> called.set(true)).count();
        assertTrue(called.get());

        called.set(false);
        LongStream.of(1L).ifEmpty(() -> called.set(true)).count();
        assertFalse(called.get());
    }

    @Test
    public void testIsParallel() {
        LongStream stream = LongStream.of(1L, 2L, 3L);
        assertFalse(stream.isParallel());
    }

    @Test
    public void testIsParallel_HappyPath2() {
        assertFalse(LongStream.of(1, 2, 3).isParallel());
        assertTrue(LongStream.of(1, 2, 3).parallel().isParallel());
    }

    // iterator

    @Test
    public void testBooleanSupplierIteratorsStayExhausted() {
        final AtomicInteger conditionCalls = new AtomicInteger();
        LongIterator iter = LongStream.iterate(() -> conditionCalls.getAndIncrement() > 0, () -> 1L).iterator();

        assertFalse(iter.hasNext());
        assertFalse(iter.hasNext());
        assertThrows(NoSuchElementException.class, iter::nextLong);
        assertEquals(1, conditionCalls.get());

        conditionCalls.set(0);
        iter = LongStream.iterate(1L, () -> conditionCalls.getAndIncrement() > 0, value -> value + 1).iterator();

        assertFalse(iter.hasNext());
        assertFalse(iter.hasNext());
        assertThrows(NoSuchElementException.class, iter::nextLong);
        assertEquals(1, conditionCalls.get());
    }

    @Test
    public void testIterator() {
        LongStream stream = LongStream.of(1L, 2L, 3L);
        LongIterator iter = stream.iterator();

        assertTrue(iter.hasNext());
        assertEquals(1L, iter.nextLong());
        assertTrue(iter.hasNext());
        assertEquals(2L, iter.nextLong());
        assertTrue(iter.hasNext());
        assertEquals(3L, iter.nextLong());
        assertFalse(iter.hasNext());
    }

    @Test
    public void testNextOnExhaustedIterator() {
        LongStream s = LongStream.of(1);
        LongIterator it = s.iterator();
        it.nextLong();
        assertThrows(NoSuchElementException.class, it::nextLong);
    }

    @Test
    public void testLargeStream() {
        LongStream stream = LongStream.range(0L, 10000L);
        assertEquals(10000, stream.count());
    }

    @Test
    public void testInvalidLimit() {
        assertThrows(IllegalArgumentException.class, () -> LongStream.of(1, 2, 3).limit(-1));
    }

    @Test
    public void testMultipleTerminalOpsThrowsException() {
        LongStream stream = LongStream.of(1L, 2L, 3L);
        stream.sum();

        assertThrows(IllegalStateException.class, () -> stream.count());
    }

    @Test
    public void testPrintln() {
        assertDoesNotThrow(() -> {
            LongStream.of(1L, 2L, 3L).println();
        });
    }

    @Test
    public void testPsp() {
        long[] result = LongStream.range(1, 100).parallel().psp(s -> s.filter(i -> i > 10).limit(5)).map(i -> i * 10).sorted().toArray();

        assertArrayEquals(new long[] { 110, 120, 130, 140, 150 }, result);
    }

    @Test
    public void testPsp_HappyPath() {
        long[] result = LongStream.of(1, 2, 3, 4, 5).psp(s -> s.filter(x -> x % 2 != 0)).toArray();
        Arrays.sort(result);
        assertArrayEquals(new long[] { 1, 3, 5 }, result);
    }

    @Test
    public void testInvalidSkip() {
        assertThrows(IllegalArgumentException.class, () -> LongStream.of(1, 2, 3).skip(-1));
    }

    @Test
    public void testInvalidStep() {
        assertThrows(IllegalArgumentException.class, () -> LongStream.of(1, 2, 3).step(0));
        assertThrows(IllegalArgumentException.class, () -> LongStream.of(1, 2, 3).step(-1));
    }

    @Test
    public void testLargeStreamSum() {
        long sum = LongStream.range(1L, 101L).sum();
        assertEquals(5050L, sum);
    }

    // throwIfEmpty

    @Test
    public void testThrowIfEmpty() {
        LongStream stream = LongStream.of(1L, 2L, 3L);
        assertNotNull(stream.throwIfEmpty());

        assertThrows(NoSuchElementException.class, () -> LongStream.empty().throwIfEmpty().count());
    }

    @Test
    public void testThrowIfEmpty_EmptyThrows() {
        assertThrows(java.util.NoSuchElementException.class, () -> LongStream.empty().throwIfEmpty().toArray());
    }

    @Test
    public void testThrowIfEmpty_NonEmpty() {
        long[] result = LongStream.of(1, 2, 3).throwIfEmpty().toArray();
        assertArrayEquals(new long[] { 1, 2, 3 }, result);
    }

    @Test
    public void testThrowIfEmptyWithCustomException() {
        assertThrows(IllegalStateException.class, () -> LongStream.empty().throwIfEmpty(() -> new IllegalStateException("Stream is empty")).count());
    }

    // ---- deep review 2026-11-02 G121 begin ----
    private static final long[] G121_SAMPLES = { 0L, -1L, 1L, Long.MIN_VALUE, Long.MAX_VALUE, Integer.MAX_VALUE + 1L };
    private static final long G121_PAD_A = (long) -1;
    private static final long G121_PAD_B = (long) 9;
    private static final long G121_PAD_C = (long) 7;

    private static long[] g121Of(final int... values) {
        final long[] result = new long[values.length];

        for (int i = 0; i < values.length; i++) {
            result[i] = (long) values[i];
        }

        return result;
    }

    private static long[] g121RandomArray(final java.util.Random rnd, final int maxLength) {
        final long[] a = new long[rnd.nextInt(maxLength + 1)];

        for (int i = 0; i < a.length; i++) {
            a[i] = rnd.nextInt(4) == 0 ? G121_SAMPLES[rnd.nextInt(G121_SAMPLES.length)] : (long) rnd.nextInt(1000);
        }

        return a;
    }

    private static long[] g121Drain(final com.landawn.abacus.util.LongIterator iterator) {
        final com.landawn.abacus.util.LongList list = new com.landawn.abacus.util.LongList();

        while (iterator.hasNext()) {
            list.add(iterator.nextLong());
        }

        return list.toArray();
    }

    private static long[] g121Suffix(final long[] a, final int from) {
        return from >= a.length ? new long[0] : java.util.Arrays.copyOfRange(a, from, a.length);
    }

    // variant 0: zip(a, b), 1: zip(a, b, c), 2: zip(a, b, padA, padB), 3: zip(a, b, c, padA, padB, padC); every call is recorded
    private static LongStream g121Zip(final int variant, final long[] a, final long[] b, final long[] c, final java.util.List<String> calls) {
        switch (variant) {
            case 0:
                return LongStream.zip(a, b, (x, y) -> {
                    calls.add(x + "," + y);
                    return (long) (x * 3 + y);
                });
            case 1:
                return LongStream.zip(a, b, c, (x, y, z) -> {
                    calls.add(x + "," + y + "," + z);
                    return (long) (x * 3 + y - z);
                });
            case 2:
                return LongStream.zip(a, b, G121_PAD_A, G121_PAD_B, (x, y) -> {
                    calls.add(x + "," + y);
                    return (long) (x * 3 + y);
                });
            default:
                return LongStream.zip(a, b, c, G121_PAD_A, G121_PAD_B, G121_PAD_C, (x, y, z) -> {
                    calls.add(x + "," + y + "," + z);
                    return (long) (x * 3 + y - z);
                });
        }
    }

    private static long[] g121ZipReference(final int variant, final long[] a, final long[] b, final long[] c) {
        final int len = variant == 0 ? Math.min(a.length, b.length)
                : variant == 1 ? Math.min(a.length, Math.min(b.length, c.length))
                        : variant == 2 ? Math.max(a.length, b.length) : Math.max(a.length, Math.max(b.length, c.length));
        final long[] result = new long[len];

        for (int i = 0; i < len; i++) {
            final long x = i < a.length ? a[i] : G121_PAD_A;
            final long y = i < b.length ? b[i] : G121_PAD_B;
            final long z = i < c.length ? c[i] : G121_PAD_C;
            result[i] = variant == 0 || variant == 2 ? (long) (x * 3 + y) : (long) (x * 3 + y - z);
        }

        return result;
    }

    // G121-01: zip(long[]...) toArray()/toLongList()/skip(k).toArray()/iterator().toArray()|toList()/sorted() fill an exactly sized
    // array; values and the zipFunction call sequence equal the element-by-element traversal (pin: GREEN on base and fix)
    @org.junit.jupiter.api.Test
    public void testZipArrays_bulkTerminalsMatchElementwiseTraversal() {
        final java.util.Random rnd = new java.util.Random(121);

        for (int round = 0; round < 200; round++) {
            final long[] a = g121RandomArray(rnd, 12);
            final long[] b = g121RandomArray(rnd, 12);
            final long[] c = g121RandomArray(rnd, 12);

            for (int variant = 0; variant < 4; variant++) {
                final long[] expected = g121ZipReference(variant, a, b, c);
                final java.util.List<String> expectedCalls = new java.util.ArrayList<>();
                org.junit.jupiter.api.Assertions.assertArrayEquals(expected, g121Drain(g121Zip(variant, a, b, c, expectedCalls).iterator()));
                org.junit.jupiter.api.Assertions.assertEquals(expected.length, expectedCalls.size());

                final java.util.List<String> calls = new java.util.ArrayList<>();
                org.junit.jupiter.api.Assertions.assertArrayEquals(expected, g121Zip(variant, a, b, c, calls).toArray());
                org.junit.jupiter.api.Assertions.assertEquals(expectedCalls, calls);

                calls.clear();
                final com.landawn.abacus.util.LongList list = g121Zip(variant, a, b, c, calls).toLongList();
                org.junit.jupiter.api.Assertions.assertArrayEquals(expected, list.toArray());
                org.junit.jupiter.api.Assertions.assertEquals(expectedCalls, calls);
                list.add((long) 42);
                org.junit.jupiter.api.Assertions.assertEquals(expected.length + 1, list.size());

                final int k = rnd.nextInt(expected.length + 2);
                calls.clear();
                org.junit.jupiter.api.Assertions.assertArrayEquals(g121Suffix(expected, k), g121Zip(variant, a, b, c, calls).skip(k).toArray());
                org.junit.jupiter.api.Assertions.assertEquals(expectedCalls, calls);

                calls.clear();
                final com.landawn.abacus.util.LongIterator iter = g121Zip(variant, a, b, c, calls).iterator();
                final int consumed = Math.min(k, expected.length);

                for (int i = 0; i < consumed; i++) {
                    iter.nextLong();
                }

                org.junit.jupiter.api.Assertions.assertArrayEquals(g121Suffix(expected, consumed),
                        rnd.nextBoolean() ? iter.toArray() : iter.toList().toArray());
                org.junit.jupiter.api.Assertions.assertEquals(expectedCalls, calls);
                org.junit.jupiter.api.Assertions.assertFalse(iter.hasNext());
                org.junit.jupiter.api.Assertions.assertSame(com.landawn.abacus.util.N.EMPTY_LONG_ARRAY, iter.toArray());
                org.junit.jupiter.api.Assertions.assertEquals(0, iter.toList().size());

                calls.clear();
                final long[] sorted = expected.clone();
                java.util.Arrays.sort(sorted);
                org.junit.jupiter.api.Assertions.assertArrayEquals(sorted, g121Zip(variant, a, b, c, calls).sorted().toArray());
                org.junit.jupiter.api.Assertions.assertEquals(expectedCalls, calls);
            }
        }
    }

    private static boolean g121TakeFirst(final boolean stateful, final long x, final long y, final java.util.List<String> calls) {
        calls.add(x + "," + y);
        return stateful ? calls.size() % 3 != 0 : x <= y;
    }

    private static LongStream g121Merge(final long[] a, final long[] b, final boolean stateful, final java.util.List<String> calls) {
        return LongStream.merge(a, b, (x, y) -> g121TakeFirst(stateful, x, y, calls) ? com.landawn.abacus.util.MergeResult.TAKE_FIRST
                : com.landawn.abacus.util.MergeResult.TAKE_SECOND);
    }

    // G121-01: merge(long[], long[]) toArray()/toLongList() fill an exactly sized array; values and the order and number of
    // nextSelector calls (a stateful selector makes the result depend on them) equal the element-by-element traversal (pin)
    @org.junit.jupiter.api.Test
    public void testMergeArrays_bulkTerminalsMatchElementwiseTraversal() {
        final java.util.Random rnd = new java.util.Random(12103);

        for (int round = 0; round < 200; round++) {
            final long[] a = g121RandomArray(rnd, 10);
            final long[] b = g121RandomArray(rnd, 10);

            if (rnd.nextBoolean()) {
                java.util.Arrays.sort(a);
                java.util.Arrays.sort(b);
            }

            final boolean stateful = rnd.nextBoolean();
            final com.landawn.abacus.util.LongList expectedList = new com.landawn.abacus.util.LongList();
            final java.util.List<String> expectedCalls = new java.util.ArrayList<>();
            int i = 0;
            int j = 0;

            while (i < a.length || j < b.length) {
                if (i < a.length && (j >= b.length || g121TakeFirst(stateful, a[i], b[j], expectedCalls))) {
                    expectedList.add(a[i++]);
                } else {
                    expectedList.add(b[j++]);
                }
            }

            final long[] expected = expectedList.toArray();
            final java.util.List<String> calls = new java.util.ArrayList<>();
            org.junit.jupiter.api.Assertions.assertArrayEquals(expected, g121Drain(g121Merge(a, b, stateful, calls).iterator()));
            org.junit.jupiter.api.Assertions.assertEquals(expectedCalls, calls);

            calls.clear();
            org.junit.jupiter.api.Assertions.assertArrayEquals(expected, g121Merge(a, b, stateful, calls).toArray());
            org.junit.jupiter.api.Assertions.assertEquals(expectedCalls, calls);

            calls.clear();
            final com.landawn.abacus.util.LongList list = g121Merge(a, b, stateful, calls).toLongList();
            org.junit.jupiter.api.Assertions.assertArrayEquals(expected, list.toArray());
            org.junit.jupiter.api.Assertions.assertEquals(expectedCalls, calls);
            list.add((long) 1);
            org.junit.jupiter.api.Assertions.assertEquals(expected.length + 1, list.size());

            final int k = rnd.nextInt(expected.length + 2);
            calls.clear();
            org.junit.jupiter.api.Assertions.assertArrayEquals(g121Suffix(expected, k), g121Merge(a, b, stateful, calls).skip(k).toArray());
            org.junit.jupiter.api.Assertions.assertEquals(expectedCalls, calls);

            calls.clear();
            final com.landawn.abacus.util.LongIterator iter = g121Merge(a, b, stateful, calls).iterator();
            final int consumed = Math.min(k, expected.length);

            for (int n = 0; n < consumed; n++) {
                iter.nextLong();
            }

            org.junit.jupiter.api.Assertions.assertArrayEquals(g121Suffix(expected, consumed), rnd.nextBoolean() ? iter.toArray() : iter.toList().toArray());
            org.junit.jupiter.api.Assertions.assertEquals(expectedCalls, calls);
            org.junit.jupiter.api.Assertions.assertFalse(iter.hasNext());
            org.junit.jupiter.api.Assertions.assertEquals(0, iter.toArray().length);
        }
    }

    private static Object g121Bulk(final com.landawn.abacus.util.LongIterator iterator, final boolean useList) {
        return useList ? iterator.toList() : iterator.toArray();
    }

    // G121-01: a failing zipFunction or nextSelector inside toArray()/toList() leaves the iterator exactly where element-by-element
    // traversal leaves it (zip(a, b)/zip(a, b, c) advance before the call, the padded zips and merge do not) (pin)
    @org.junit.jupiter.api.Test
    public void testZipAndMergeArrays_failingCallbackKeepsIteratorState() {
        final long[] a = g121Of(1, 2, 3, 4, 5);
        final long[] b = g121Of(10, 20, 30, 40, 50, 60);
        final long[] c = g121Of(100, 200, 300, 400, 500, 600, 700);

        for (int viaList = 0; viaList < 2; viaList++) {
            final boolean useList = viaList == 1;
            final int[] calls = { 0 };

            final com.landawn.abacus.util.LongIterator zip2 = LongStream.zip(a, b, (x, y) -> {
                if (++calls[0] == 3) {
                    throw new IllegalStateException("zip2");
                }

                return (long) (x + y);
            }).iterator();
            org.junit.jupiter.api.Assertions.assertEquals("zip2",
                    org.junit.jupiter.api.Assertions.assertThrows(IllegalStateException.class, () -> g121Bulk(zip2, useList)).getMessage());
            org.junit.jupiter.api.Assertions.assertEquals(3, calls[0]);
            org.junit.jupiter.api.Assertions.assertArrayEquals(g121Of(44, 55), g121Drain(zip2));
            org.junit.jupiter.api.Assertions.assertEquals(5, calls[0]);

            calls[0] = 0;
            final com.landawn.abacus.util.LongIterator zip3 = LongStream.zip(a, b, c, (x, y, z) -> {
                if (++calls[0] == 2) {
                    throw new IllegalStateException("zip3");
                }

                return (long) (x + y + z);
            }).iterator();
            org.junit.jupiter.api.Assertions.assertEquals("zip3",
                    org.junit.jupiter.api.Assertions.assertThrows(IllegalStateException.class, () -> g121Bulk(zip3, useList)).getMessage());
            org.junit.jupiter.api.Assertions.assertArrayEquals(g121Of(333, 444, 555), g121Drain(zip3));
            org.junit.jupiter.api.Assertions.assertEquals(5, calls[0]);

            calls[0] = 0;
            final com.landawn.abacus.util.LongIterator pad2 = LongStream.zip(a, b, (long) -1, (long) -2, (x, y) -> {
                if (++calls[0] == 3) {
                    throw new IllegalStateException("pad2");
                }

                return (long) (x + y);
            }).iterator();
            org.junit.jupiter.api.Assertions.assertEquals("pad2",
                    org.junit.jupiter.api.Assertions.assertThrows(IllegalStateException.class, () -> g121Bulk(pad2, useList)).getMessage());
            org.junit.jupiter.api.Assertions.assertArrayEquals(g121Of(33, 44, 55, 59), g121Drain(pad2));
            org.junit.jupiter.api.Assertions.assertEquals(7, calls[0]);

            calls[0] = 0;
            final com.landawn.abacus.util.LongIterator pad3 = LongStream.zip(a, b, c, (long) -1, (long) -2, (long) -3, (x, y, z) -> {
                if (++calls[0] == 6) {
                    throw new IllegalStateException("pad3");
                }

                return (long) (x + y + z);
            }).iterator();
            org.junit.jupiter.api.Assertions.assertEquals("pad3",
                    org.junit.jupiter.api.Assertions.assertThrows(IllegalStateException.class, () -> g121Bulk(pad3, useList)).getMessage());
            org.junit.jupiter.api.Assertions.assertArrayEquals(g121Of(659, 697), g121Drain(pad3));
            org.junit.jupiter.api.Assertions.assertEquals(8, calls[0]);

            calls[0] = 0;
            final com.landawn.abacus.util.LongIterator merge = LongStream.merge(g121Of(1, 3, 5), g121Of(2, 4, 6), (x, y) -> {
                if (++calls[0] == 3) {
                    throw new IllegalStateException("merge");
                }

                return x <= y ? com.landawn.abacus.util.MergeResult.TAKE_FIRST : com.landawn.abacus.util.MergeResult.TAKE_SECOND;
            }).iterator();
            org.junit.jupiter.api.Assertions.assertEquals("merge",
                    org.junit.jupiter.api.Assertions.assertThrows(IllegalStateException.class, () -> g121Bulk(merge, useList)).getMessage());
            org.junit.jupiter.api.Assertions.assertArrayEquals(g121Of(3, 4, 5, 6), g121Drain(merge));
            org.junit.jupiter.api.Assertions.assertEquals(6, calls[0]);
        }
    }

    // G121-01: a zipFunction that consumes its own stream's iterator re-entrantly (once) ends toArray()/toLongList() where the
    // hasNext()-driven loop ends, instead of failing with NoSuchElementException (pin)
    @org.junit.jupiter.api.Test
    public void testZipArrays_reentrantCallbackEndsBulkTerminalEarly() {
        final long[] a = g121Of(1, 2, 3, 4, 5);
        final long[] b = g121Of(10, 20, 30, 40, 50);
        final long[] c = g121Of(100, 200, 300, 400, 500);
        final Runnable[] consume = new Runnable[1];
        final boolean[] once = { true };

        LongStream s = LongStream.zip(a, b, (x, y) -> {
            if (once[0]) {
                once[0] = false;
                consume[0].run();
            }

            return (long) (x + y);
        });
        com.landawn.abacus.util.LongIterator it = s.iterator();
        consume[0] = it::nextLong;
        org.junit.jupiter.api.Assertions.assertArrayEquals(g121Of(11, 33, 44, 55), it.toArray());
        org.junit.jupiter.api.Assertions.assertFalse(it.hasNext());

        once[0] = true;
        s = LongStream.zip(a, b, (x, y) -> {
            if (once[0]) {
                once[0] = false;
                consume[0].run();
            }

            return (long) (x + y);
        });
        it = s.iterator();
        consume[0] = it::nextLong;
        org.junit.jupiter.api.Assertions.assertArrayEquals(g121Of(11, 33, 44, 55), s.toLongList().toArray());

        once[0] = true;
        s = LongStream.zip(a, b, c, (x, y, z) -> {
            if (once[0]) {
                once[0] = false;
                consume[0].run();
            }

            return (long) (x + y + z);
        });
        it = s.iterator();
        consume[0] = it::nextLong;
        org.junit.jupiter.api.Assertions.assertArrayEquals(g121Of(111, 333, 444, 555), it.toArray());

        once[0] = true;
        s = LongStream.zip(a, g121Of(7), (long) -1, (long) -2, (x, y) -> {
            if (once[0]) {
                once[0] = false;
                consume[0].run();
            }

            return (long) (x + y);
        });
        it = s.iterator();
        consume[0] = it::nextLong;
        org.junit.jupiter.api.Assertions.assertArrayEquals(g121Of(8, 1, 2, 3), it.toArray());

        once[0] = true;
        s = LongStream.zip(a, g121Of(7), new long[0], (long) -1, (long) -2, (long) -3, (x, y, z) -> {
            if (once[0]) {
                once[0] = false;
                consume[0].run();
            }

            return (long) (x + y + z);
        });
        it = s.iterator();
        consume[0] = it::nextLong;
        org.junit.jupiter.api.Assertions.assertArrayEquals(g121Of(5, -2, -1, 0), it.toArray());
    }

    private static LongStream g121Flatten(final long[][] a, final int mode, final long pad) {
        return mode == 0 ? LongStream.flatten(a, true) : LongStream.flatten(a, pad, mode == 1);
    }

    // G121-01: flatten(long[][], true) (rectangular walk and the sparse jagged walk) and flatten(long[][], value, vertically)
    // toArray()/toLongList() fill an exactly sized array; pinned against plain column-major / row-major references, also after
    // skip(k) and partial iteration (pin)
    @org.junit.jupiter.api.Test
    public void testFlattenVerticalAndAligned_bulkTerminalsMatchReference() {
        final java.util.Random rnd = new java.util.Random(12102);

        for (int round = 0; round < 300; round++) {
            final int shape = rnd.nextInt(3);
            final int rows = shape == 2 ? 5 + rnd.nextInt(6) : rnd.nextInt(7);
            final long[][] a = new long[rows][];
            final int width = 1 + rnd.nextInt(6);

            for (int r = 0; r < rows; r++) {
                if (shape == 1) {
                    a[r] = new long[width];
                    java.util.Arrays.fill(a[r], (long) (r + 1));
                } else if (shape == 2) {
                    // one long row among null, empty and one-element rows: rows * maxLen > 4 * n selects the jagged walk
                    a[r] = r == rows / 2 ? g121RandomArray(rnd, 40) : rnd.nextInt(3) == 0 ? null : new long[rnd.nextInt(2)];
                } else {
                    a[r] = rnd.nextInt(5) == 0 ? null : g121RandomArray(rnd, 6);
                }
            }

            int maxLen = 0;

            for (final long[] row : a) {
                maxLen = Math.max(maxLen, row == null ? 0 : row.length);
            }

            final long pad = G121_SAMPLES[rnd.nextInt(G121_SAMPLES.length)];
            final com.landawn.abacus.util.LongList vertical = new com.landawn.abacus.util.LongList();
            final com.landawn.abacus.util.LongList alignedVertical = new com.landawn.abacus.util.LongList();
            final com.landawn.abacus.util.LongList alignedHorizontal = new com.landawn.abacus.util.LongList();

            for (int col = 0; col < maxLen; col++) {
                for (final long[] row : a) {
                    if (row != null && col < row.length) {
                        vertical.add(row[col]);
                        alignedVertical.add(row[col]);
                    } else {
                        alignedVertical.add(pad);
                    }
                }
            }

            for (final long[] row : a) {
                for (int col = 0; col < maxLen; col++) {
                    alignedHorizontal.add(row != null && col < row.length ? row[col] : pad);
                }
            }

            for (int mode = 0; mode < 3; mode++) {
                // a single row is returned as-is (no padding) by both flatten overloads
                final long[] expected = rows == 1 ? (a[0] == null ? new long[0] : a[0].clone())
                        : (mode == 0 ? vertical : mode == 1 ? alignedVertical : alignedHorizontal).toArray();

                org.junit.jupiter.api.Assertions.assertArrayEquals(expected, g121Flatten(a, mode, pad).toArray());
                org.junit.jupiter.api.Assertions.assertArrayEquals(expected, g121Flatten(a, mode, pad).toLongList().toArray());
                org.junit.jupiter.api.Assertions.assertArrayEquals(expected, g121Drain(g121Flatten(a, mode, pad).iterator()));

                final int k = rnd.nextInt(expected.length + 2);
                org.junit.jupiter.api.Assertions.assertArrayEquals(g121Suffix(expected, k), g121Flatten(a, mode, pad).skip(k).toArray());

                final com.landawn.abacus.util.LongIterator iter = g121Flatten(a, mode, pad).iterator();
                final int consumed = Math.min(k, expected.length);

                for (int n = 0; n < consumed; n++) {
                    iter.nextLong();
                }

                org.junit.jupiter.api.Assertions.assertArrayEquals(g121Suffix(expected, consumed),
                        rnd.nextBoolean() ? iter.toArray() : iter.toList().toArray());
                org.junit.jupiter.api.Assertions.assertFalse(iter.hasNext());
                org.junit.jupiter.api.Assertions.assertEquals(0, iter.toList().size());
                org.junit.jupiter.api.Assertions.assertThrows(java.util.NoSuchElementException.class, iter::nextLong);
            }
        }
    }
    // ---- deep review 2026-11-02 G121 end ----

    // ---- deep review 2026-11-02b G106 begin ----
    // G106-01: the range/rangeClosed/repeat iterators build toList() (used by toLongList()) on their exactly sized
    // toArray(). Pin: the result equals an element-by-element drain, also after partial consumption and advance(n).
    @Test
    public void testRangeAndRepeatToLongList_matchesElementwiseDrain_r116() {
        // { start, end, by, mask }: bit k of mask = factory kind k (see g106Range) is small enough to drain
        final long[][] specs = { { 0, 0, 1, 15 }, { 0, 1, 1, 15 }, { 0, 10, 1, 15 }, { 0, 1000, 1, 15 }, { -5, 5, 3, 15 }, { 10, 0, -2, 15 },
                { 10, -1, -3, 15 }, { 3, 3, -1, 15 }, { Long.MAX_VALUE - 20, Long.MAX_VALUE, 1, 15 }, { Long.MIN_VALUE, Long.MIN_VALUE + 20, 7, 15 },
                { Long.MAX_VALUE, Long.MIN_VALUE, Long.MIN_VALUE, 15 }, { Long.MIN_VALUE, Long.MAX_VALUE, Long.MAX_VALUE, 10 },
                { Long.MAX_VALUE, Long.MIN_VALUE, -Long.MAX_VALUE, 15 } };

        for (final long[] spec : specs) {
            for (int kind = 0; kind < 4; kind++) {
                if ((spec[3] & (1 << kind)) != 0) {
                    final int k = kind;
                    final long[] expected = g106Drain(g106Range(spec, k).iteratorEx());

                    assertArrayEquals(expected, g106Range(spec, k).toLongList().toArray());
                    g106CheckToList(expected, () -> g106Range(spec, k).iteratorEx());
                }
            }
        }

        for (final long n : new long[] { 1, 9, 10, 11, 17, 1000 }) {
            final long[] expected = new long[(int) n];
            Arrays.fill(expected, -7L);

            assertArrayEquals(expected, LongStream.repeat(-7L, n).toLongList().toArray());
            g106CheckToList(expected, () -> LongStream.repeat(-7L, n).iteratorEx());
        }
    }

    // G106-01: rangeClosed(MIN_VALUE, MAX_VALUE) has 2^64 elements (unsigned count plus the extra end element); after
    // advance(n) only a few remain, and toList() must return exactly those (pin).
    @Test
    public void testRangeAndRepeatToList_hugeSourceAfterAdvance_r116() {
        LongIteratorEx iter = LongStream.rangeClosed(Long.MIN_VALUE, Long.MAX_VALUE).iteratorEx();
        iter.advance(Long.MAX_VALUE);
        iter.advance(Long.MAX_VALUE);
        assertArrayEquals(new long[] { Long.MAX_VALUE - 1, Long.MAX_VALUE }, iter.toList().toArray());
        assertFalse(iter.hasNext());

        iter = LongStream.rangeClosed(Long.MIN_VALUE, Long.MAX_VALUE).iteratorEx();
        iter.advance(Long.MAX_VALUE);
        iter.advance(Long.MAX_VALUE - 3);
        assertArrayEquals(new long[] { Long.MAX_VALUE - 4, Long.MAX_VALUE - 3, Long.MAX_VALUE - 2, Long.MAX_VALUE - 1, Long.MAX_VALUE },
                iter.toList().toArray());
        assertEquals(0, iter.toList().size());

        iter = LongStream.rangeClosed(Long.MIN_VALUE, Long.MAX_VALUE).iteratorEx();
        iter.advance(Long.MAX_VALUE);
        iter.advance(Long.MAX_VALUE);
        assertEquals(Long.MAX_VALUE - 1, iter.nextLong());
        assertArrayEquals(new long[] { Long.MAX_VALUE }, iter.toList().toArray());

        iter = LongStream.range(Long.MIN_VALUE, Long.MAX_VALUE).iteratorEx();
        iter.advance(Long.MAX_VALUE);
        iter.advance(Long.MAX_VALUE);
        assertArrayEquals(new long[] { Long.MAX_VALUE - 1 }, iter.toList().toArray());
        assertThrows(NoSuchElementException.class, iter::nextLong);

        iter = LongStream.rangeClosed(Long.MIN_VALUE, Long.MAX_VALUE, 2).iteratorEx();
        iter.advance(Long.MAX_VALUE - 2);
        assertArrayEquals(new long[] { Long.MAX_VALUE - 5, Long.MAX_VALUE - 3, Long.MAX_VALUE - 1 }, iter.toList().toArray());

        iter = LongStream.repeat(5L, Long.MAX_VALUE).iteratorEx();
        iter.advance(Long.MAX_VALUE - 3);
        assertArrayEquals(new long[] { 5L, 5L, 5L }, iter.toList().toArray());
        assertFalse(iter.hasNext());
    }

    private static LongStream g106Range(final long[] spec, final int kind) {
        switch (kind) {
            case 0:
                return LongStream.range(spec[0], spec[1]);
            case 1:
                return LongStream.range(spec[0], spec[1], spec[2]);
            case 2:
                return LongStream.rangeClosed(spec[0], spec[1]);
            default:
                return LongStream.rangeClosed(spec[0], spec[1], spec[2]);
        }
    }

    private static long[] g106Drain(final LongIterator iter) {
        final com.landawn.abacus.util.LongList list = new com.landawn.abacus.util.LongList();

        while (iter.hasNext()) {
            list.add(iter.nextLong());
        }

        return list.toArray();
    }

    private static void g106CheckToList(final long[] expected, final java.util.function.Supplier<LongIteratorEx> iterSupplier) {
        for (int k = 0; k <= expected.length + 1; k = k < 12 ? k + 1 : k * 3) {
            for (int mode = 0; mode < 2; mode++) {
                final LongIteratorEx iter = iterSupplier.get();
                final int consumed = Math.min(k, expected.length);

                if (mode == 0) {
                    for (int i = 0; i < consumed; i++) {
                        iter.nextLong();
                    }
                } else {
                    iter.advance(k);
                }

                final long[] suffix = Arrays.copyOfRange(expected, consumed, expected.length);
                final com.landawn.abacus.util.LongList list = iter.toList();

                assertArrayEquals(suffix, list.toArray());
                assertFalse(iter.hasNext());
                assertEquals(0, iter.toList().size());
                assertEquals(0, iter.toArray().length);
                assertThrows(NoSuchElementException.class, iter::nextLong);

                list.add(42L); // still a plain mutable list
                assertEquals(suffix.length + 1, list.size());
                assertEquals(42L, list.get(suffix.length));
            }
        }
    }

    // ---- deep review 2026-11-02b G106 end ----
    // ---- deep review 2026-11-02b G120 begin ----
    // G120-04 (pin): LongStream.of(LongBuffer) for a buffer without an accessible array (direct, read-only, sliced, view of a heap
    // ByteBuffer) reads the window [position, limit) through a dedicated iterator; toArray()/toList() fill an exact-size
    // array with the same get(i) reads. Results, laziness (live view), window snapshot and iterator states must match element-wise traversal.
    @Test
    public void testOfLongBuffer_nonArrayBufferTerminalsMatchElementwise_r116() {
        final long[] content = { 10, Long.MIN_VALUE, Long.MAX_VALUE, 0, -1, 5, 5, 3, 9, -7, 2, 1 };

        for (int kind = 0; kind < 5; kind++) {
            for (int from = 0; from <= content.length; from++) {
                for (int to = from; to <= content.length; to++) {
                    final long[] expected = Arrays.copyOfRange(content, from, to);
                    final java.nio.LongBuffer buf = g120LongBuffer(kind, content, from, to);

                    assertFalse(buf.hasArray());
                    assertArrayEquals(expected, LongStream.of(buf).toArray());
                    assertArrayEquals(expected, LongStream.of(buf).toLongList().toArray());
                    assertEquals(expected.length, LongStream.of(buf).count());
                    assertArrayEquals(g120LongSuffix(expected, 3), LongStream.of(buf).skip(3).toArray());
                    assertArrayEquals(g120LongSorted(expected), LongStream.of(buf).sorted().toArray());

                    final com.landawn.abacus.util.LongList list = LongStream.of(buf).toLongList();
                    list.add((long) 42); // a fresh, mutable list
                    assertEquals(expected.length + 1, list.size());

                    for (int consumed = 0; consumed <= expected.length; consumed++) {
                        final LongIterator iter = LongStream.of(buf).iterator();

                        for (int n = 0; n < consumed; n++) {
                            assertEquals(expected[n], iter.nextLong());
                        }

                        final boolean viaArray = (consumed + from + kind) % 2 == 0;
                        final long[] rest = viaArray ? iter.toArray() : iter.toList().toArray();
                        assertArrayEquals(g120LongSuffix(expected, consumed), rest);
                        assertFalse(iter.hasNext());
                        assertEquals(0, iter.toList().size());
                        assertThrows(NoSuchElementException.class, iter::nextLong);

                        if (expected.length > 0) {
                            // an exhausted iterator returns the shared empty array, as the element-wise toArray() did
                            org.junit.jupiter.api.Assertions.assertSame(com.landawn.abacus.util.N.EMPTY_LONG_ARRAY, iter.toArray());
                        } else {
                            assertEquals(0, iter.toArray().length);
                        }
                    }

                    final LongIteratorEx iterEx = (LongIteratorEx) LongStream.of(buf).iterator();
                    iterEx.advance(2);
                    assertArrayEquals(g120LongSuffix(expected, 2), iterEx.toArray());
                }
            }
        }

        // live view of the window taken at the call: later writes are seen, later position/limit changes are not
        final java.nio.LongBuffer direct = java.nio.ByteBuffer.allocateDirect(6 * 8).asLongBuffer();
        direct.put(new long[] { 1, 2, 3, 4, 5, 6 });
        direct.position(1);
        direct.limit(5);

        final LongStream stream = LongStream.of(direct);
        direct.put(4, (long) 40);
        direct.clear();
        direct.limit(0);

        assertArrayEquals(new long[] { 2, 3, 4, 40 }, stream.toArray());
        assertEquals(0, direct.position());
        assertEquals(0, direct.limit());
    }

    private static java.nio.LongBuffer g120LongBuffer(final int kind, final long[] content, final int from, final int to) {
        final java.nio.LongBuffer buf;

        if (kind == 0) {
            buf = java.nio.ByteBuffer.allocateDirect(content.length * 8).asLongBuffer().put(content);
        } else if (kind == 1) {
            buf = java.nio.LongBuffer.wrap(content.clone()).asReadOnlyBuffer();
        } else if (kind == 2) {
            final java.nio.LongBuffer outer = java.nio.ByteBuffer.allocateDirect((content.length + 3) * 8).asLongBuffer();
            outer.position(2);
            outer.put(content);
            outer.position(2);
            outer.limit(2 + content.length);
            buf = outer.slice();
        } else if (kind == 3) {
            buf = java.nio.ByteBuffer.allocateDirect(content.length * 8).asLongBuffer().put(content).asReadOnlyBuffer();
        } else {
            buf = java.nio.ByteBuffer.allocate(content.length * 8).order(java.nio.ByteOrder.LITTLE_ENDIAN).asLongBuffer().put(content);
        }

        buf.limit(to);
        buf.position(from);

        return buf;
    }

    private static long[] g120LongSuffix(final long[] a, final int k) {
        return Arrays.copyOfRange(a, Math.min(k, a.length), a.length);
    }

    private static long[] g120LongSorted(final long[] a) {
        final long[] copy = a.clone();
        Arrays.sort(copy);
        return copy;
    }

    // ---- deep review 2026-11-02b G120 end ----
    // ---- deep review 2026-11-02b G124 begin ----
    // G124: a buffer without an accessible array keeps the close-handler shape of the former
    // IntStream.range(position, limit).mapToLong(view::get) stream: exactly one (run-once, no-op) close handler.
    @Test
    public void testOfLongBuffer_nonArrayBufferKeepsRangeCloseHandler_r116() {
        final java.nio.LongBuffer direct = java.nio.ByteBuffer.allocateDirect(4 * 8).asLongBuffer();
        direct.put(new long[] { (long) 1, (long) 2, (long) 3, (long) 4 });
        direct.position(1);
        final java.nio.LongBuffer emptyWindow = direct.duplicate();
        emptyWindow.position(4);

        for (final java.nio.LongBuffer buf : java.util.List.of(direct, emptyWindow)) {
            // closing the JDK view closes the source stream, as for every stream that has close handlers
            final LongStream source = LongStream.of(buf);
            source.toJdkStream().close();
            org.junit.jupiter.api.Assertions.assertThrows(IllegalStateException.class, source::count);

            // what iterator()'s "Remember to close" warning checks
            final LongStream stream = LongStream.of(buf);
            org.junit.jupiter.api.Assertions.assertFalse(StreamBase.isEmptyCloseHandlers(stream.closeHandlers()));
            org.junit.jupiter.api.Assertions.assertEquals(1, stream.closeHandlers().size());

            // handlers registered later run once, in order; a failing one does not stop the others
            final java.util.List<String> calls = new java.util.ArrayList<>();
            final LongStream withHandlers = LongStream.of(buf).onClose(() -> calls.add("a")).onClose(() -> {
                calls.add("b");
                throw new IllegalStateException("b");
            }).onClose(() -> calls.add("c"));
            org.junit.jupiter.api.Assertions.assertEquals("b",
                    org.junit.jupiter.api.Assertions.assertThrows(IllegalStateException.class, withHandlers::close).getMessage());
            withHandlers.close();
            org.junit.jupiter.api.Assertions.assertEquals(java.util.List.of("a", "b", "c"), calls);
            org.junit.jupiter.api.Assertions.assertThrows(IllegalStateException.class, withHandlers::count);
        }

        org.junit.jupiter.api.Assertions.assertArrayEquals(new long[] { (long) 2, (long) 3, (long) 4 }, LongStream.of(direct).toArray());
    }
    // ---- deep review 2026-11-02b G124 end ----

    // ---- re-review 2026-11-03 R04 begin ----
    // R04 (pin): LongStream.of(LongBuffer) for a buffer without an accessible array counts and skips by moving the cursor of its
    // window iterator instead of reading every skipped element. Pinned for every window, read position and skip length:
    // count(), advance(n) (negative, zero, inside, exactly to and past the end, Long.MAX_VALUE), the elements read after
    // it, the stream-level skip/step/count, the failure-atomic advance flag, and the live view of elements not yet read.
    @Test
    public void testOfLongBuffer_nonArrayBufferCountAndAdvanceMoveCursor_r116() {
        final long[] content = { 10L, Long.MIN_VALUE, Long.MAX_VALUE, 0L, -1L, 5L, 5L, 3L, 9L, -7L, 2L, 1L };

        for (int kind = 0; kind < 2; kind++) {
            for (int from = 0; from <= content.length; from++) {
                for (int to = from; to <= content.length; to++) {
                    final long[] expected = java.util.Arrays.copyOfRange(content, from, to);
                    final java.nio.LongBuffer buf = r04Buffer(kind, content, from, to);
                    org.junit.jupiter.api.Assertions.assertFalse(buf.hasArray());

                    org.junit.jupiter.api.Assertions.assertEquals(expected.length, LongStream.of(buf).count());

                    for (int k = 0; k <= expected.length + 1; k++) {
                        final int start = Math.min(k, expected.length);
                        final long[] rest = java.util.Arrays.copyOfRange(expected, start, expected.length);
                        org.junit.jupiter.api.Assertions.assertEquals(rest.length, LongStream.of(buf).skip(k).count());
                        org.junit.jupiter.api.Assertions.assertArrayEquals(rest, LongStream.of(buf).skip(k).toArray());
                    }

                    org.junit.jupiter.api.Assertions.assertArrayEquals(r04Step(expected, 3), LongStream.of(buf).step(3).toArray());

                    for (int consumed = 0; consumed <= expected.length; consumed++) {
                        final int remaining = expected.length - consumed;

                        for (final long n : new long[] { -1, 0, 1, 2, remaining, remaining + 1L, Long.MAX_VALUE }) {
                            final int next = consumed + (int) Math.min(Math.max(n, 0), remaining);
                            final LongIteratorEx iter = (LongIteratorEx) LongStream.of(buf).iterator();

                            if (expected.length > 0) {
                                org.junit.jupiter.api.Assertions.assertTrue(iter.supportsFailureAtomicAdvance());
                            }

                            for (int i = 0; i < consumed; i++) {
                                org.junit.jupiter.api.Assertions.assertEquals(expected[i], iter.nextLong());
                            }

                            iter.advance(n);

                            for (int i = next; i < expected.length; i++) {
                                org.junit.jupiter.api.Assertions.assertTrue(iter.hasNext());
                                org.junit.jupiter.api.Assertions.assertEquals(expected[i], iter.nextLong());
                            }

                            org.junit.jupiter.api.Assertions.assertFalse(iter.hasNext());
                            org.junit.jupiter.api.Assertions.assertEquals(0, iter.count());
                            org.junit.jupiter.api.Assertions.assertThrows(java.util.NoSuchElementException.class, iter::nextLong);

                            final LongIteratorEx counted = (LongIteratorEx) LongStream.of(buf).iterator();

                            for (int i = 0; i < consumed; i++) {
                                counted.nextLong();
                            }

                            counted.advance(n);
                            org.junit.jupiter.api.Assertions.assertEquals(expected.length - next, counted.count());
                            org.junit.jupiter.api.Assertions.assertFalse(counted.hasNext());
                            counted.advance(1);
                            org.junit.jupiter.api.Assertions.assertEquals(0, counted.count());
                        }
                    }
                }
            }
        }

        // A skip moves the cursor only: an element written after the skip is read live, a skipped one is never read.
        final java.nio.LongBuffer live = r04Buffer(1, content, 0, 6);
        final LongIteratorEx liveIter = (LongIteratorEx) LongStream.of(live).iterator();
        liveIter.advance(2);
        live.put(1, 777L);
        live.put(3, 888L);
        org.junit.jupiter.api.Assertions.assertEquals(content[2], liveIter.nextLong());
        org.junit.jupiter.api.Assertions.assertEquals(888L, liveIter.nextLong());
        org.junit.jupiter.api.Assertions.assertEquals(2, liveIter.count());
        org.junit.jupiter.api.Assertions.assertFalse(liveIter.hasNext());
    }

    private static java.nio.LongBuffer r04Buffer(final int kind, final long[] content, final int from, final int to) {
        // kind 0: read-only view of a heap array; kind 1: writable view of direct memory. Neither exposes an array.
        final java.nio.LongBuffer buf = kind == 0 ? java.nio.LongBuffer.wrap(content.clone()).asReadOnlyBuffer()
                : java.nio.ByteBuffer.allocateDirect(content.length * 8).asLongBuffer().put(content).clear();
        buf.limit(to);
        buf.position(from);
        return buf;
    }

    private static long[] r04Step(final long[] a, final int step) {
        final long[] result = new long[(a.length + step - 1) / step];

        for (int i = 0; i < result.length; i++) {
            result[i] = a[i * step];
        }

        return result;
    }
    // ---- re-review 2026-11-03 R04 end ----
}

package com.landawn.abacus.util.stream;

import static org.assertj.core.api.Assertions.fail;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.util.ByteIterator;

/** ByteStream general contracts. */
public class ByteStreamTest extends ByteStreamTestSupport {

    // defaultIfEmpty

    @Test
    public void testDefaultIfEmpty() {
        ByteStream empty = ByteStream.empty();
        ByteStream result1 = empty.defaultIfEmpty(() -> createByteStream((byte) 1, (byte) 2));
        assertArrayEquals(new byte[] { 1, 2 }, result1.toArray());

        ByteStream nonEmpty = createByteStream((byte) 3);
        ByteStream result2 = nonEmpty.defaultIfEmpty(() -> createByteStream((byte) 1, (byte) 2));
        assertArrayEquals(new byte[] { 3 }, result2.toArray());
    }

    @Test
    public void testDefaultIfEmpty_OnEmpty() {
        ByteStream stream = ByteStream.empty();
        byte[] result = stream.defaultIfEmpty(() -> ByteStream.of((byte) 99)).toArray();
        assertArrayEquals(new byte[] { 99 }, result);
    }

    @Test
    public void testDefaultIfEmpty_OnNonEmpty() {
        ByteStream stream = createByteStream((byte) 1, (byte) 2);
        byte[] result = stream.defaultIfEmpty(() -> ByteStream.of((byte) 99)).toArray();
        assertArrayEquals(new byte[] { 1, 2 }, result);
    }

    @Test
    public void testDefaultIfEmptyOnEmpty() {
        byte[] result = ByteStream.empty().defaultIfEmpty(() -> ByteStream.of((byte) 99)).toArray();
        assertArrayEquals(new byte[] { 99 }, result);
    }

    @Test
    public void testDefaultIfEmptyOnNonEmpty() {
        byte[] result = createByteStream((byte) 1, (byte) 2).defaultIfEmpty(() -> ByteStream.of((byte) 99)).toArray();
        assertArrayEquals(new byte[] { 1, 2 }, result);
    }

    @Test
    public void testFiniteFactoryIteratorsRemainExhaustedAfterFailedNext() {
        final ByteIteratorEx[] iterators = { ByteStream.range((byte) 1, (byte) 3).iteratorEx(), ByteStream.range((byte) 1, (byte) 5, (byte) 2).iteratorEx(),
                ByteStream.rangeClosed((byte) 1, (byte) 3).iteratorEx(), ByteStream.rangeClosed((byte) 1, (byte) 5, (byte) 2).iteratorEx(),
                ByteStream.repeat((byte) 7, 10).iteratorEx() };

        for (final ByteIteratorEx iter : iterators) {
            iter.advance(Long.MAX_VALUE);
            assertThrows(NoSuchElementException.class, iter::nextByte);
            assertEquals(0, iter.count());
            assertArrayEquals(new byte[0], iter.toArray());
        }
    }

    // ifEmpty

    @Test
    public void testIfEmpty() {
        AtomicBoolean actionExecuted = new AtomicBoolean(false);
        byteStream = createByteStream(new byte[] { 1, 2 });
        byteStream.ifEmpty(() -> actionExecuted.set(true)).toList();
        assertFalse(actionExecuted.get());

        actionExecuted.set(false);
        byteStream = createByteStream(new byte[] {});
        byteStream.ifEmpty(() -> actionExecuted.set(true)).toList();
        assertTrue(actionExecuted.get());
    }

    @Test
    public void testIfEmpty_Empty() {
        ByteStream stream = ByteStream.empty();
        AtomicBoolean called = new AtomicBoolean(false);
        ByteStream result = stream.ifEmpty(() -> called.set(true));
        result.count(); // terminal op to trigger
        assertTrue(called.get());
    }

    @Test
    public void testIfEmpty_NonEmpty() {
        ByteStream stream = createByteStream((byte) 1, (byte) 2);
        AtomicBoolean called = new AtomicBoolean(false);
        byte[] result = stream.ifEmpty(() -> called.set(true)).toArray();
        assertFalse(called.get());
        assertArrayEquals(new byte[] { 1, 2 }, result);
    }

    @Test
    public void testIsParallel() {
        byteStream = createByteStream(new byte[] { 1, 2, 3 });
        assertFalse(byteStream.isParallel());
        assertTrue(byteStream.parallel().isParallel());
    }

    @Test
    public void testBooleanSupplierIteratorsStayExhausted() {
        final AtomicInteger conditionCalls = new AtomicInteger();
        ByteIterator iter = ByteStream.iterate(() -> conditionCalls.getAndIncrement() > 0, () -> (byte) 1).iterator();

        assertFalse(iter.hasNext());
        assertFalse(iter.hasNext());
        assertThrows(NoSuchElementException.class, iter::nextByte);
        assertEquals(1, conditionCalls.get());

        conditionCalls.set(0);
        iter = ByteStream.iterate((byte) 1, () -> conditionCalls.getAndIncrement() > 0, value -> (byte) (value + 1)).iterator();

        assertFalse(iter.hasNext());
        assertFalse(iter.hasNext());
        assertThrows(NoSuchElementException.class, iter::nextByte);
        assertEquals(1, conditionCalls.get());
    }

    @Test
    public void testIterator() {
        ByteStream stream = ByteStream.of((byte) 1, (byte) 2, (byte) 3);
        ByteIterator iter = stream.iterator();
        assertTrue(iter.hasNext());
        assertEquals(1, iter.nextByte());
        assertTrue(iter.hasNext());
        assertEquals(2, iter.nextByte());
        assertTrue(iter.hasNext());
        assertEquals(3, iter.nextByte());
        assertFalse(iter.hasNext());
    }

    @Test
    public void testPrintln() {
        assertDoesNotThrow(() -> {
            byteStream = createByteStream(new byte[] { 1, 2, 3 });
            byteStream.println();
        });
    }

    @Test
    public void testPrintln_EmptyStream() {
        assertDoesNotThrow(() -> ByteStream.empty().println());
    }

    @Test
    public void testPsp() {
        ByteStream stream = createByteStream((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5);

        ByteStream result = stream.parallel().psp(s -> s.filter(b -> b % 2 == 0));

        assertTrue(result.isParallel());
        assertArrayEquals(new byte[] { 2, 4 }, result.sorted().toArray());
    }

    @Test
    public void testPspFunction() {
        byteStream = createByteStream(new byte[] { 3, 1, 2 }).parallel();
        List<Byte> result = byteStream.psp(s -> s.sorted()).toList();
        assertEquals(Arrays.asList((byte) 1, (byte) 2, (byte) 3), result);
        assertTrue(byteStream.isParallel());
    }

    @Test
    public void testStreamReuseThrowsException() {
        ByteStream stream = createByteStream((byte) 1, (byte) 2, (byte) 3);
        stream.toArray(); // consume
        assertThrows(IllegalStateException.class, () -> stream.toArray());
    }

    // throwIfEmpty

    @Test
    public void testThrowIfEmpty() {
        ByteStream stream = ByteStream.of((byte) 1, (byte) 2, (byte) 3);
        assertArrayEquals(new byte[] { 1, 2, 3 }, stream.throwIfEmpty().toArray());

        ByteStream emptyStream = ByteStream.empty();
        assertThrows(NoSuchElementException.class, () -> emptyStream.throwIfEmpty().toArray());
    }

    @Test
    public void testThrowIfEmpty_Empty() {
        ByteStream stream = ByteStream.empty();
        assertThrows(NoSuchElementException.class, () -> stream.throwIfEmpty().toArray());
    }

    @Test
    public void testThrowIfEmpty_NonEmpty() {
        ByteStream stream = createByteStream((byte) 1, (byte) 2);
        byte[] result = stream.throwIfEmpty().toArray();
        assertArrayEquals(new byte[] { 1, 2 }, result);
    }

    @Test
    public void testThrowIfEmptyWithCustomException() {
        byteStream = createByteStream(new byte[] {});
        RuntimeException customException = new RuntimeException("Stream is empty!");
        try {
            byteStream.throwIfEmpty(() -> customException).toList();
            fail("Should throw custom exception");
        } catch (RuntimeException e) {
            assertSame(customException, e);
        }
    }

    @Test
    public void testThrowIfEmptyWithSupplier() {
        ByteStream stream = createByteStream((byte) 1, (byte) 2);
        assertArrayEquals(new byte[] { 1, 2 }, stream.throwIfEmpty(() -> new RuntimeException("Should not throw")).toArray());

        ByteStream empty = ByteStream.empty();
        assertThrows(IllegalStateException.class, () -> empty.throwIfEmpty(() -> new IllegalStateException("Empty stream")).toArray());
    }

    // ---- deep review 2026-11-02 G121 begin ----
    private static final byte[] G121_SAMPLES = { 0, -1, 1, Byte.MIN_VALUE, Byte.MAX_VALUE };
    private static final byte G121_PAD_A = (byte) -1;
    private static final byte G121_PAD_B = (byte) 9;
    private static final byte G121_PAD_C = (byte) 7;

    private static byte[] g121Of(final int... values) {
        final byte[] result = new byte[values.length];

        for (int i = 0; i < values.length; i++) {
            result[i] = (byte) values[i];
        }

        return result;
    }

    private static byte[] g121RandomArray(final java.util.Random rnd, final int maxLength) {
        final byte[] a = new byte[rnd.nextInt(maxLength + 1)];

        for (int i = 0; i < a.length; i++) {
            a[i] = rnd.nextInt(4) == 0 ? G121_SAMPLES[rnd.nextInt(G121_SAMPLES.length)] : (byte) rnd.nextInt(256);
        }

        return a;
    }

    private static byte[] g121Drain(final com.landawn.abacus.util.ByteIterator iterator) {
        final com.landawn.abacus.util.ByteList list = new com.landawn.abacus.util.ByteList();

        while (iterator.hasNext()) {
            list.add(iterator.nextByte());
        }

        return list.toArray();
    }

    private static byte[] g121Suffix(final byte[] a, final int from) {
        return from >= a.length ? new byte[0] : java.util.Arrays.copyOfRange(a, from, a.length);
    }

    // variant 0: zip(a, b), 1: zip(a, b, c), 2: zip(a, b, padA, padB), 3: zip(a, b, c, padA, padB, padC); every call is recorded
    private static ByteStream g121Zip(final int variant, final byte[] a, final byte[] b, final byte[] c, final java.util.List<String> calls) {
        switch (variant) {
            case 0:
                return ByteStream.zip(a, b, (x, y) -> {
                    calls.add(x + "," + y);
                    return (byte) (x * 3 + y);
                });
            case 1:
                return ByteStream.zip(a, b, c, (x, y, z) -> {
                    calls.add(x + "," + y + "," + z);
                    return (byte) (x * 3 + y - z);
                });
            case 2:
                return ByteStream.zip(a, b, G121_PAD_A, G121_PAD_B, (x, y) -> {
                    calls.add(x + "," + y);
                    return (byte) (x * 3 + y);
                });
            default:
                return ByteStream.zip(a, b, c, G121_PAD_A, G121_PAD_B, G121_PAD_C, (x, y, z) -> {
                    calls.add(x + "," + y + "," + z);
                    return (byte) (x * 3 + y - z);
                });
        }
    }

    private static byte[] g121ZipReference(final int variant, final byte[] a, final byte[] b, final byte[] c) {
        final int len = variant == 0 ? Math.min(a.length, b.length)
                : variant == 1 ? Math.min(a.length, Math.min(b.length, c.length))
                        : variant == 2 ? Math.max(a.length, b.length) : Math.max(a.length, Math.max(b.length, c.length));
        final byte[] result = new byte[len];

        for (int i = 0; i < len; i++) {
            final byte x = i < a.length ? a[i] : G121_PAD_A;
            final byte y = i < b.length ? b[i] : G121_PAD_B;
            final byte z = i < c.length ? c[i] : G121_PAD_C;
            result[i] = variant == 0 || variant == 2 ? (byte) (x * 3 + y) : (byte) (x * 3 + y - z);
        }

        return result;
    }

    // G121-01: zip(byte[]...) toArray()/toByteList()/skip(k).toArray()/iterator().toArray()|toList()/sorted() fill an exactly sized
    // array; values and the zipFunction call sequence equal the element-by-element traversal (pin: GREEN on base and fix)
    @org.junit.jupiter.api.Test
    public void testZipArrays_bulkTerminalsMatchElementwiseTraversal() {
        final java.util.Random rnd = new java.util.Random(121);

        for (int round = 0; round < 200; round++) {
            final byte[] a = g121RandomArray(rnd, 12);
            final byte[] b = g121RandomArray(rnd, 12);
            final byte[] c = g121RandomArray(rnd, 12);

            for (int variant = 0; variant < 4; variant++) {
                final byte[] expected = g121ZipReference(variant, a, b, c);
                final java.util.List<String> expectedCalls = new java.util.ArrayList<>();
                org.junit.jupiter.api.Assertions.assertArrayEquals(expected, g121Drain(g121Zip(variant, a, b, c, expectedCalls).iterator()));
                org.junit.jupiter.api.Assertions.assertEquals(expected.length, expectedCalls.size());

                final java.util.List<String> calls = new java.util.ArrayList<>();
                org.junit.jupiter.api.Assertions.assertArrayEquals(expected, g121Zip(variant, a, b, c, calls).toArray());
                org.junit.jupiter.api.Assertions.assertEquals(expectedCalls, calls);

                calls.clear();
                final com.landawn.abacus.util.ByteList list = g121Zip(variant, a, b, c, calls).toByteList();
                org.junit.jupiter.api.Assertions.assertArrayEquals(expected, list.toArray());
                org.junit.jupiter.api.Assertions.assertEquals(expectedCalls, calls);
                list.add((byte) 42);
                org.junit.jupiter.api.Assertions.assertEquals(expected.length + 1, list.size());

                final int k = rnd.nextInt(expected.length + 2);
                calls.clear();
                org.junit.jupiter.api.Assertions.assertArrayEquals(g121Suffix(expected, k), g121Zip(variant, a, b, c, calls).skip(k).toArray());
                org.junit.jupiter.api.Assertions.assertEquals(expectedCalls, calls);

                calls.clear();
                final com.landawn.abacus.util.ByteIterator iter = g121Zip(variant, a, b, c, calls).iterator();
                final int consumed = Math.min(k, expected.length);

                for (int i = 0; i < consumed; i++) {
                    iter.nextByte();
                }

                org.junit.jupiter.api.Assertions.assertArrayEquals(g121Suffix(expected, consumed),
                        rnd.nextBoolean() ? iter.toArray() : iter.toList().toArray());
                org.junit.jupiter.api.Assertions.assertEquals(expectedCalls, calls);
                org.junit.jupiter.api.Assertions.assertFalse(iter.hasNext());
                org.junit.jupiter.api.Assertions.assertSame(com.landawn.abacus.util.N.EMPTY_BYTE_ARRAY, iter.toArray());
                org.junit.jupiter.api.Assertions.assertEquals(0, iter.toList().size());

                calls.clear();
                final byte[] sorted = expected.clone();
                java.util.Arrays.sort(sorted);
                org.junit.jupiter.api.Assertions.assertArrayEquals(sorted, g121Zip(variant, a, b, c, calls).sorted().toArray());
                org.junit.jupiter.api.Assertions.assertEquals(expectedCalls, calls);
            }
        }
    }

    private static boolean g121TakeFirst(final boolean stateful, final byte x, final byte y, final java.util.List<String> calls) {
        calls.add(x + "," + y);
        return stateful ? calls.size() % 3 != 0 : x <= y;
    }

    private static ByteStream g121Merge(final byte[] a, final byte[] b, final boolean stateful, final java.util.List<String> calls) {
        return ByteStream.merge(a, b, (x, y) -> g121TakeFirst(stateful, x, y, calls) ? com.landawn.abacus.util.MergeResult.TAKE_FIRST
                : com.landawn.abacus.util.MergeResult.TAKE_SECOND);
    }

    // G121-01: merge(byte[], byte[]) toArray()/toByteList() fill an exactly sized array; values and the order and number of
    // nextSelector calls (a stateful selector makes the result depend on them) equal the element-by-element traversal (pin)
    @org.junit.jupiter.api.Test
    public void testMergeArrays_bulkTerminalsMatchElementwiseTraversal() {
        final java.util.Random rnd = new java.util.Random(12103);

        for (int round = 0; round < 200; round++) {
            final byte[] a = g121RandomArray(rnd, 10);
            final byte[] b = g121RandomArray(rnd, 10);

            if (rnd.nextBoolean()) {
                java.util.Arrays.sort(a);
                java.util.Arrays.sort(b);
            }

            final boolean stateful = rnd.nextBoolean();
            final com.landawn.abacus.util.ByteList expectedList = new com.landawn.abacus.util.ByteList();
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

            final byte[] expected = expectedList.toArray();
            final java.util.List<String> calls = new java.util.ArrayList<>();
            org.junit.jupiter.api.Assertions.assertArrayEquals(expected, g121Drain(g121Merge(a, b, stateful, calls).iterator()));
            org.junit.jupiter.api.Assertions.assertEquals(expectedCalls, calls);

            calls.clear();
            org.junit.jupiter.api.Assertions.assertArrayEquals(expected, g121Merge(a, b, stateful, calls).toArray());
            org.junit.jupiter.api.Assertions.assertEquals(expectedCalls, calls);

            calls.clear();
            final com.landawn.abacus.util.ByteList list = g121Merge(a, b, stateful, calls).toByteList();
            org.junit.jupiter.api.Assertions.assertArrayEquals(expected, list.toArray());
            org.junit.jupiter.api.Assertions.assertEquals(expectedCalls, calls);
            list.add((byte) 1);
            org.junit.jupiter.api.Assertions.assertEquals(expected.length + 1, list.size());

            final int k = rnd.nextInt(expected.length + 2);
            calls.clear();
            org.junit.jupiter.api.Assertions.assertArrayEquals(g121Suffix(expected, k), g121Merge(a, b, stateful, calls).skip(k).toArray());
            org.junit.jupiter.api.Assertions.assertEquals(expectedCalls, calls);

            calls.clear();
            final com.landawn.abacus.util.ByteIterator iter = g121Merge(a, b, stateful, calls).iterator();
            final int consumed = Math.min(k, expected.length);

            for (int n = 0; n < consumed; n++) {
                iter.nextByte();
            }

            org.junit.jupiter.api.Assertions.assertArrayEquals(g121Suffix(expected, consumed), rnd.nextBoolean() ? iter.toArray() : iter.toList().toArray());
            org.junit.jupiter.api.Assertions.assertEquals(expectedCalls, calls);
            org.junit.jupiter.api.Assertions.assertFalse(iter.hasNext());
            org.junit.jupiter.api.Assertions.assertEquals(0, iter.toArray().length);
        }
    }

    private static Object g121Bulk(final com.landawn.abacus.util.ByteIterator iterator, final boolean useList) {
        return useList ? iterator.toList() : iterator.toArray();
    }

    // G121-01: a failing zipFunction or nextSelector inside toArray()/toList() leaves the iterator exactly where element-by-element
    // traversal leaves it (zip(a, b)/zip(a, b, c) advance before the call, the padded zips and merge do not) (pin)
    @org.junit.jupiter.api.Test
    public void testZipAndMergeArrays_failingCallbackKeepsIteratorState() {
        final byte[] a = g121Of(1, 2, 3, 4, 5);
        final byte[] b = g121Of(10, 20, 30, 40, 50, 60);
        final byte[] c = g121Of(100, 200, 300, 400, 500, 600, 700);

        for (int viaList = 0; viaList < 2; viaList++) {
            final boolean useList = viaList == 1;
            final int[] calls = { 0 };

            final com.landawn.abacus.util.ByteIterator zip2 = ByteStream.zip(a, b, (x, y) -> {
                if (++calls[0] == 3) {
                    throw new IllegalStateException("zip2");
                }

                return (byte) (x + y);
            }).iterator();
            org.junit.jupiter.api.Assertions.assertEquals("zip2",
                    org.junit.jupiter.api.Assertions.assertThrows(IllegalStateException.class, () -> g121Bulk(zip2, useList)).getMessage());
            org.junit.jupiter.api.Assertions.assertEquals(3, calls[0]);
            org.junit.jupiter.api.Assertions.assertArrayEquals(g121Of(44, 55), g121Drain(zip2));
            org.junit.jupiter.api.Assertions.assertEquals(5, calls[0]);

            calls[0] = 0;
            final com.landawn.abacus.util.ByteIterator zip3 = ByteStream.zip(a, b, c, (x, y, z) -> {
                if (++calls[0] == 2) {
                    throw new IllegalStateException("zip3");
                }

                return (byte) (x + y + z);
            }).iterator();
            org.junit.jupiter.api.Assertions.assertEquals("zip3",
                    org.junit.jupiter.api.Assertions.assertThrows(IllegalStateException.class, () -> g121Bulk(zip3, useList)).getMessage());
            org.junit.jupiter.api.Assertions.assertArrayEquals(g121Of(333, 444, 555), g121Drain(zip3));
            org.junit.jupiter.api.Assertions.assertEquals(5, calls[0]);

            calls[0] = 0;
            final com.landawn.abacus.util.ByteIterator pad2 = ByteStream.zip(a, b, (byte) -1, (byte) -2, (x, y) -> {
                if (++calls[0] == 3) {
                    throw new IllegalStateException("pad2");
                }

                return (byte) (x + y);
            }).iterator();
            org.junit.jupiter.api.Assertions.assertEquals("pad2",
                    org.junit.jupiter.api.Assertions.assertThrows(IllegalStateException.class, () -> g121Bulk(pad2, useList)).getMessage());
            org.junit.jupiter.api.Assertions.assertArrayEquals(g121Of(33, 44, 55, 59), g121Drain(pad2));
            org.junit.jupiter.api.Assertions.assertEquals(7, calls[0]);

            calls[0] = 0;
            final com.landawn.abacus.util.ByteIterator pad3 = ByteStream.zip(a, b, c, (byte) -1, (byte) -2, (byte) -3, (x, y, z) -> {
                if (++calls[0] == 6) {
                    throw new IllegalStateException("pad3");
                }

                return (byte) (x + y + z);
            }).iterator();
            org.junit.jupiter.api.Assertions.assertEquals("pad3",
                    org.junit.jupiter.api.Assertions.assertThrows(IllegalStateException.class, () -> g121Bulk(pad3, useList)).getMessage());
            org.junit.jupiter.api.Assertions.assertArrayEquals(g121Of(659, 697), g121Drain(pad3));
            org.junit.jupiter.api.Assertions.assertEquals(8, calls[0]);

            calls[0] = 0;
            final com.landawn.abacus.util.ByteIterator merge = ByteStream.merge(g121Of(1, 3, 5), g121Of(2, 4, 6), (x, y) -> {
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

    // G121-01: a zipFunction that consumes its own stream's iterator re-entrantly (once) ends toArray()/toByteList() where the
    // hasNext()-driven loop ends, instead of failing with NoSuchElementException (pin)
    @org.junit.jupiter.api.Test
    public void testZipArrays_reentrantCallbackEndsBulkTerminalEarly() {
        final byte[] a = g121Of(1, 2, 3, 4, 5);
        final byte[] b = g121Of(10, 20, 30, 40, 50);
        final byte[] c = g121Of(100, 200, 300, 400, 500);
        final Runnable[] consume = new Runnable[1];
        final boolean[] once = { true };

        ByteStream s = ByteStream.zip(a, b, (x, y) -> {
            if (once[0]) {
                once[0] = false;
                consume[0].run();
            }

            return (byte) (x + y);
        });
        com.landawn.abacus.util.ByteIterator it = s.iterator();
        consume[0] = it::nextByte;
        org.junit.jupiter.api.Assertions.assertArrayEquals(g121Of(11, 33, 44, 55), it.toArray());
        org.junit.jupiter.api.Assertions.assertFalse(it.hasNext());

        once[0] = true;
        s = ByteStream.zip(a, b, (x, y) -> {
            if (once[0]) {
                once[0] = false;
                consume[0].run();
            }

            return (byte) (x + y);
        });
        it = s.iterator();
        consume[0] = it::nextByte;
        org.junit.jupiter.api.Assertions.assertArrayEquals(g121Of(11, 33, 44, 55), s.toByteList().toArray());

        once[0] = true;
        s = ByteStream.zip(a, b, c, (x, y, z) -> {
            if (once[0]) {
                once[0] = false;
                consume[0].run();
            }

            return (byte) (x + y + z);
        });
        it = s.iterator();
        consume[0] = it::nextByte;
        org.junit.jupiter.api.Assertions.assertArrayEquals(g121Of(111, 333, 444, 555), it.toArray());

        once[0] = true;
        s = ByteStream.zip(a, g121Of(7), (byte) -1, (byte) -2, (x, y) -> {
            if (once[0]) {
                once[0] = false;
                consume[0].run();
            }

            return (byte) (x + y);
        });
        it = s.iterator();
        consume[0] = it::nextByte;
        org.junit.jupiter.api.Assertions.assertArrayEquals(g121Of(8, 1, 2, 3), it.toArray());

        once[0] = true;
        s = ByteStream.zip(a, g121Of(7), new byte[0], (byte) -1, (byte) -2, (byte) -3, (x, y, z) -> {
            if (once[0]) {
                once[0] = false;
                consume[0].run();
            }

            return (byte) (x + y + z);
        });
        it = s.iterator();
        consume[0] = it::nextByte;
        org.junit.jupiter.api.Assertions.assertArrayEquals(g121Of(5, -2, -1, 0), it.toArray());
    }

    private static ByteStream g121Flatten(final byte[][] a, final int mode, final byte pad) {
        return mode == 0 ? ByteStream.flatten(a, true) : ByteStream.flatten(a, pad, mode == 1);
    }

    // G121-01: flatten(byte[][], true) (rectangular walk and the sparse jagged walk) and flatten(byte[][], value, vertically)
    // toArray()/toByteList() fill an exactly sized array; pinned against plain column-major / row-major references, also after
    // skip(k) and partial iteration (pin)
    @org.junit.jupiter.api.Test
    public void testFlattenVerticalAndAligned_bulkTerminalsMatchReference() {
        final java.util.Random rnd = new java.util.Random(12102);

        for (int round = 0; round < 300; round++) {
            final int shape = rnd.nextInt(3);
            final int rows = shape == 2 ? 5 + rnd.nextInt(6) : rnd.nextInt(7);
            final byte[][] a = new byte[rows][];
            final int width = 1 + rnd.nextInt(6);

            for (int r = 0; r < rows; r++) {
                if (shape == 1) {
                    a[r] = new byte[width];
                    java.util.Arrays.fill(a[r], (byte) (r + 1));
                } else if (shape == 2) {
                    // one long row among null, empty and one-element rows: rows * maxLen > 4 * n selects the jagged walk
                    a[r] = r == rows / 2 ? g121RandomArray(rnd, 40) : rnd.nextInt(3) == 0 ? null : new byte[rnd.nextInt(2)];
                } else {
                    a[r] = rnd.nextInt(5) == 0 ? null : g121RandomArray(rnd, 6);
                }
            }

            int maxLen = 0;

            for (final byte[] row : a) {
                maxLen = Math.max(maxLen, row == null ? 0 : row.length);
            }

            final byte pad = G121_SAMPLES[rnd.nextInt(G121_SAMPLES.length)];
            final com.landawn.abacus.util.ByteList vertical = new com.landawn.abacus.util.ByteList();
            final com.landawn.abacus.util.ByteList alignedVertical = new com.landawn.abacus.util.ByteList();
            final com.landawn.abacus.util.ByteList alignedHorizontal = new com.landawn.abacus.util.ByteList();

            for (int col = 0; col < maxLen; col++) {
                for (final byte[] row : a) {
                    if (row != null && col < row.length) {
                        vertical.add(row[col]);
                        alignedVertical.add(row[col]);
                    } else {
                        alignedVertical.add(pad);
                    }
                }
            }

            for (final byte[] row : a) {
                for (int col = 0; col < maxLen; col++) {
                    alignedHorizontal.add(row != null && col < row.length ? row[col] : pad);
                }
            }

            for (int mode = 0; mode < 3; mode++) {
                // a single row is returned as-is (no padding) by both flatten overloads
                final byte[] expected = rows == 1 ? (a[0] == null ? new byte[0] : a[0].clone())
                        : (mode == 0 ? vertical : mode == 1 ? alignedVertical : alignedHorizontal).toArray();

                org.junit.jupiter.api.Assertions.assertArrayEquals(expected, g121Flatten(a, mode, pad).toArray());
                org.junit.jupiter.api.Assertions.assertArrayEquals(expected, g121Flatten(a, mode, pad).toByteList().toArray());
                org.junit.jupiter.api.Assertions.assertArrayEquals(expected, g121Drain(g121Flatten(a, mode, pad).iterator()));

                final int k = rnd.nextInt(expected.length + 2);
                org.junit.jupiter.api.Assertions.assertArrayEquals(g121Suffix(expected, k), g121Flatten(a, mode, pad).skip(k).toArray());

                final com.landawn.abacus.util.ByteIterator iter = g121Flatten(a, mode, pad).iterator();
                final int consumed = Math.min(k, expected.length);

                for (int n = 0; n < consumed; n++) {
                    iter.nextByte();
                }

                org.junit.jupiter.api.Assertions.assertArrayEquals(g121Suffix(expected, consumed),
                        rnd.nextBoolean() ? iter.toArray() : iter.toList().toArray());
                org.junit.jupiter.api.Assertions.assertFalse(iter.hasNext());
                org.junit.jupiter.api.Assertions.assertEquals(0, iter.toList().size());
                org.junit.jupiter.api.Assertions.assertThrows(java.util.NoSuchElementException.class, iter::nextByte);
            }
        }
    }

    // ---- deep review 2026-11-02 G121 end ----
    // ---- deep review 2026-11-02b G096 begin ----
    // G096-01 (pin): ByteStream.of(ByteBuffer) for a buffer without an accessible array (direct, read-only, sliced) reads
    // the window [position, limit) through a dedicated iterator; toArray()/toList() fill an exact-size array with the same
    // get(i) reads. Results, laziness (live view), window snapshot and iterator states must match element-wise traversal.
    @Test
    public void testOfByteBuffer_nonArrayBufferTerminalsMatchElementwise_r116() {
        final byte[] content = { 10, -128, 127, 0, -1, 5, 5, 3, 9, -7, 2, 1 };

        for (int kind = 0; kind < 4; kind++) {
            for (int from = 0; from <= content.length; from++) {
                for (int to = from; to <= content.length; to++) {
                    final byte[] expected = Arrays.copyOfRange(content, from, to);
                    final java.nio.ByteBuffer buf = g096Buffer(kind, content, from, to);

                    org.junit.jupiter.api.Assertions.assertFalse(buf.hasArray());
                    assertArrayEquals(expected, ByteStream.of(buf).toArray());
                    assertArrayEquals(expected, ByteStream.of(buf).toByteList().toArray());
                    assertEquals(expected.length, ByteStream.of(buf).count());
                    assertArrayEquals(g096Suffix(expected, 3), ByteStream.of(buf).skip(3).toArray());
                    assertArrayEquals(g096Sorted(expected), ByteStream.of(buf).sorted().toArray());

                    final com.landawn.abacus.util.ByteList list = ByteStream.of(buf).toByteList();
                    list.add((byte) 42); // a fresh, mutable list
                    assertEquals(expected.length + 1, list.size());

                    for (int consumed = 0; consumed <= expected.length; consumed++) {
                        final ByteIterator iter = ByteStream.of(buf).iterator();

                        for (int n = 0; n < consumed; n++) {
                            assertEquals(expected[n], iter.nextByte());
                        }

                        final boolean viaArray = (consumed + from + kind) % 2 == 0;
                        final byte[] rest = viaArray ? iter.toArray() : iter.toList().toArray();
                        assertArrayEquals(g096Suffix(expected, consumed), rest);
                        assertFalse(iter.hasNext());
                        assertEquals(0, iter.toList().size());
                        assertThrows(NoSuchElementException.class, iter::nextByte);

                        if (expected.length > 0) {
                            // an exhausted iterator returns the shared empty array, as the element-wise toArray() did
                            assertSame(com.landawn.abacus.util.N.EMPTY_BYTE_ARRAY, iter.toArray());
                        } else {
                            assertEquals(0, iter.toArray().length);
                        }
                    }

                    final ByteIteratorEx iterEx = (ByteIteratorEx) ByteStream.of(buf).iterator();
                    iterEx.advance(2);
                    assertArrayEquals(g096Suffix(expected, 2), iterEx.toArray());
                }
            }
        }

        // live view of the window taken at the call: later writes are seen, later position/limit changes are not
        final java.nio.ByteBuffer direct = java.nio.ByteBuffer.allocateDirect(6);
        direct.put(new byte[] { 1, 2, 3, 4, 5, 6 });
        direct.position(1);
        direct.limit(5);

        final ByteStream stream = ByteStream.of(direct);
        direct.put(4, (byte) 40);
        direct.clear();
        direct.limit(0);

        assertArrayEquals(new byte[] { 2, 3, 4, 40 }, stream.toArray());
        assertEquals(0, direct.position());
        assertEquals(0, direct.limit());
    }

    private static java.nio.ByteBuffer g096Buffer(final int kind, final byte[] content, final int from, final int to) {
        final java.nio.ByteBuffer buf;

        if (kind == 0) {
            buf = java.nio.ByteBuffer.allocateDirect(content.length).put(content);
        } else if (kind == 1) {
            buf = java.nio.ByteBuffer.wrap(content.clone()).asReadOnlyBuffer();
        } else if (kind == 2) {
            final java.nio.ByteBuffer outer = java.nio.ByteBuffer.allocateDirect(content.length + 3);
            outer.position(2);
            outer.put(content);
            outer.position(2);
            outer.limit(2 + content.length);
            buf = outer.slice();
        } else {
            buf = java.nio.ByteBuffer.allocateDirect(content.length).put(content).asReadOnlyBuffer();
        }

        buf.limit(to);
        buf.position(from);

        return buf;
    }

    private static byte[] g096Suffix(final byte[] a, final int k) {
        return Arrays.copyOfRange(a, Math.min(k, a.length), a.length);
    }

    private static byte[] g096Sorted(final byte[] a) {
        final byte[] copy = a.clone();
        Arrays.sort(copy);
        return copy;
    }

    // ---- deep review 2026-11-02b G096 end ----
    // ---- deep review 2026-11-02b G119 begin ----
    private static final byte[] G119_BOUNDS = { Byte.MIN_VALUE, -127, -100, -2, -1, 0, 1, 2, 9, 10, 11, 100, 126, Byte.MAX_VALUE };
    private static final byte[] G119_STEPS = { 1, -1, 2, -2, 3, -3, 100, -100, Byte.MAX_VALUE, -127, Byte.MIN_VALUE };

    // kind: 0 range(s, e), 1 range(s, e, by), 2 rangeClosed(s, e), 3 rangeClosed(s, e, by)
    private static ByteStream g119Range(final int kind, final byte start, final byte end, final byte by) {
        switch (kind) {
            case 0:
                return ByteStream.range(start, end);
            case 1:
                return ByteStream.range(start, end, by);
            case 2:
                return ByteStream.rangeClosed(start, end);
            default:
                return ByteStream.rangeClosed(start, end, by);
        }
    }

    // independent oracle: the documented values, computed in int arithmetic
    private static byte[] g119Expected(final int kind, final byte start, final byte end, final byte by) {
        final int step = kind == 0 || kind == 2 ? 1 : by;
        final boolean closed = kind >= 2;
        final com.landawn.abacus.util.ByteList list = new com.landawn.abacus.util.ByteList();

        for (int v = start; step > 0 ? (closed ? v <= end : v < end) : (closed ? v >= end : v > end); v += step) {
            list.add((byte) v);
        }

        return list.toArray();
    }

    private static byte[] g119Drain(final ByteIterator iterator) {
        final com.landawn.abacus.util.ByteList list = new com.landawn.abacus.util.ByteList();

        while (iterator.hasNext()) {
            list.add(iterator.nextByte());
        }

        return list.toArray();
    }

    // G119-01 (pin): range/rangeClosed(+by) iterators answer toList() (used by toByteList()) from their exact-size toArray();
    // the result must equal the documented values and element-wise traversal, be a fresh mutable list, and match after skip(k).
    @Test
    public void testRangeToByteList_matchesElementwiseDrain_r116() {
        int nonEmpty = 0;

        for (final byte start : G119_BOUNDS) {
            for (final byte end : G119_BOUNDS) {
                for (int kind = 0; kind < 4; kind++) {
                    for (final byte by : kind == 0 || kind == 2 ? new byte[] { 1 } : G119_STEPS) {
                        final byte[] expected = g119Expected(kind, start, end, by);
                        final String shape = kind + "/" + start + "/" + end + "/" + by;
                        assertArrayEquals(expected, g119Drain(g119Range(kind, start, end, by).iterator()), shape);

                        final com.landawn.abacus.util.ByteList whole = g119Range(kind, start, end, by).toByteList();
                        assertArrayEquals(expected, whole.toArray(), shape);
                        whole.add((byte) 42); // a fresh mutable list
                        assertEquals(expected.length + 1, whole.size(), shape);
                        assertEquals((byte) 42, whole.get(expected.length), shape);

                        for (final int k : new int[] { 1, 2, expected.length - 1, expected.length, expected.length + 1 }) {
                            if (k >= 0) {
                                assertArrayEquals(Arrays.copyOfRange(expected, Math.min(k, expected.length), expected.length),
                                        g119Range(kind, start, end, by).skip(k).toByteList().toArray(), shape + " skip " + k);
                            }
                        }

                        nonEmpty += expected.length > 0 ? 1 : 0;
                    }
                }
            }
        }

        assertTrue(nonEmpty > 500);
        assertEquals(255, ByteStream.range(Byte.MIN_VALUE, Byte.MAX_VALUE).toByteList().size());
        assertEquals(256, ByteStream.rangeClosed(Byte.MIN_VALUE, Byte.MAX_VALUE).toByteList().size());
        assertArrayEquals(new byte[] { Byte.MAX_VALUE, -1 }, ByteStream.range(Byte.MAX_VALUE, Byte.MIN_VALUE, Byte.MIN_VALUE).toByteList().toArray());
        assertArrayEquals(new byte[] { Byte.MIN_VALUE, -1, 126 },
                ByteStream.rangeClosed(Byte.MIN_VALUE, Byte.MAX_VALUE, Byte.MAX_VALUE).toByteList().toArray());
    }

    // G119-01 (pin): after a partial read (nextByte() or advance(n)), the range iterator's toList() returns exactly the rest
    // and leaves the iterator exhausted (hasNext() false, toList()/toArray()/count() empty, nextByte() throws), as element-wise
    // traversal does; an empty remainder still gives a fresh mutable empty list.
    @Test
    public void testRangeIteratorToList_afterPartialReadLeavesIteratorExhausted_r116() {
        for (final byte start : G119_BOUNDS) {
            for (final byte end : G119_BOUNDS) {
                for (int kind = 0; kind < 4; kind++) {
                    for (final byte by : kind == 0 || kind == 2 ? new byte[] { 1 } : G119_STEPS) {
                        final byte[] expected = g119Expected(kind, start, end, by);
                        final String shape = kind + "/" + start + "/" + end + "/" + by;

                        for (final int k : new int[] { 0, 1, 2, expected.length - 1, expected.length }) {
                            if (k < 0 || k > expected.length) {
                                continue;
                            }

                            for (int mode = 0; mode < 2; mode++) {
                                final ByteIterator it = g119Range(kind, start, end, by).iterator();

                                if (mode == 0) {
                                    for (int i = 0; i < k; i++) {
                                        assertEquals(expected[i], it.nextByte(), shape);
                                    }
                                } else {
                                    ((ByteIteratorEx) it).advance(k);
                                }

                                final com.landawn.abacus.util.ByteList rest = it.toList();
                                assertArrayEquals(Arrays.copyOfRange(expected, k, expected.length), rest.toArray(), shape + " k " + k);
                                assertFalse(it.hasNext(), shape);
                                assertEquals(0, it.toList().size(), shape);
                                assertEquals(0, it.toArray().length, shape);
                                assertEquals(0, ((ByteIteratorEx) it).count(), shape);
                                assertThrows(NoSuchElementException.class, it::nextByte, shape);

                                rest.add((byte) 7); // a fresh mutable list, also when it was empty
                                assertEquals(expected.length - k + 1, rest.size(), shape);
                            }
                        }
                    }
                }
            }
        }
    }

    // ---- deep review 2026-11-02b G119 end ----
    // ---- deep review 2026-11-02b G124 begin ----
    // G124: a buffer without an accessible array keeps the close-handler shape of the former
    // IntStream.range(position, limit).mapToByte(view::get) stream: exactly one (run-once, no-op) close handler.
    @Test
    public void testOfByteBuffer_nonArrayBufferKeepsRangeCloseHandler_r116() {
        final java.nio.ByteBuffer direct = java.nio.ByteBuffer.allocateDirect(4);
        direct.put(new byte[] { (byte) 1, (byte) 2, (byte) 3, (byte) 4 });
        direct.position(1);
        final java.nio.ByteBuffer emptyWindow = direct.duplicate();
        emptyWindow.position(4);

        for (final java.nio.ByteBuffer buf : java.util.List.of(direct, emptyWindow)) {
            // what iterator()'s "Remember to close" warning checks
            final ByteStream stream = ByteStream.of(buf);
            org.junit.jupiter.api.Assertions.assertFalse(StreamBase.isEmptyCloseHandlers(stream.closeHandlers()));
            org.junit.jupiter.api.Assertions.assertEquals(1, stream.closeHandlers().size());

            // handlers registered later run once, in order; a failing one does not stop the others
            final java.util.List<String> calls = new java.util.ArrayList<>();
            final ByteStream withHandlers = ByteStream.of(buf).onClose(() -> calls.add("a")).onClose(() -> {
                calls.add("b");
                throw new IllegalStateException("b");
            }).onClose(() -> calls.add("c"));
            org.junit.jupiter.api.Assertions.assertEquals("b",
                    org.junit.jupiter.api.Assertions.assertThrows(IllegalStateException.class, withHandlers::close).getMessage());
            withHandlers.close();
            org.junit.jupiter.api.Assertions.assertEquals(java.util.List.of("a", "b", "c"), calls);
            org.junit.jupiter.api.Assertions.assertThrows(IllegalStateException.class, withHandlers::count);
        }

        org.junit.jupiter.api.Assertions.assertArrayEquals(new byte[] { (byte) 2, (byte) 3, (byte) 4 }, ByteStream.of(direct).toArray());
    }
    // ---- deep review 2026-11-02b G124 end ----

    // ---- re-review 2026-11-03 R04 begin ----
    // R04 (pin): ByteStream.of(ByteBuffer) for a buffer without an accessible array counts and skips by moving the cursor of its
    // window iterator instead of reading every skipped element. Pinned for every window, read position and skip length:
    // count(), advance(n) (negative, zero, inside, exactly to and past the end, Long.MAX_VALUE), the elements read after
    // it, the stream-level skip/step/count, the failure-atomic advance flag, and the live view of elements not yet read.
    @Test
    public void testOfByteBuffer_nonArrayBufferCountAndAdvanceMoveCursor_r116() {
        final byte[] content = { 10, -128, 127, 0, -1, 5, 5, 3, 9, -7, 2, 1 };

        for (int kind = 0; kind < 2; kind++) {
            for (int from = 0; from <= content.length; from++) {
                for (int to = from; to <= content.length; to++) {
                    final byte[] expected = java.util.Arrays.copyOfRange(content, from, to);
                    final java.nio.ByteBuffer buf = r04Buffer(kind, content, from, to);
                    org.junit.jupiter.api.Assertions.assertFalse(buf.hasArray());

                    org.junit.jupiter.api.Assertions.assertEquals(expected.length, ByteStream.of(buf).count());

                    for (int k = 0; k <= expected.length + 1; k++) {
                        final int start = Math.min(k, expected.length);
                        final byte[] rest = java.util.Arrays.copyOfRange(expected, start, expected.length);
                        org.junit.jupiter.api.Assertions.assertEquals(rest.length, ByteStream.of(buf).skip(k).count());
                        org.junit.jupiter.api.Assertions.assertArrayEquals(rest, ByteStream.of(buf).skip(k).toArray());
                    }

                    org.junit.jupiter.api.Assertions.assertArrayEquals(r04Step(expected, 3), ByteStream.of(buf).step(3).toArray());

                    for (int consumed = 0; consumed <= expected.length; consumed++) {
                        final int remaining = expected.length - consumed;

                        for (final long n : new long[] { -1, 0, 1, 2, remaining, remaining + 1L, Long.MAX_VALUE }) {
                            final int next = consumed + (int) Math.min(Math.max(n, 0), remaining);
                            final ByteIteratorEx iter = (ByteIteratorEx) ByteStream.of(buf).iterator();

                            if (expected.length > 0) {
                                org.junit.jupiter.api.Assertions.assertTrue(iter.supportsFailureAtomicAdvance());
                            }

                            for (int i = 0; i < consumed; i++) {
                                org.junit.jupiter.api.Assertions.assertEquals(expected[i], iter.nextByte());
                            }

                            iter.advance(n);

                            for (int i = next; i < expected.length; i++) {
                                org.junit.jupiter.api.Assertions.assertTrue(iter.hasNext());
                                org.junit.jupiter.api.Assertions.assertEquals(expected[i], iter.nextByte());
                            }

                            org.junit.jupiter.api.Assertions.assertFalse(iter.hasNext());
                            org.junit.jupiter.api.Assertions.assertEquals(0, iter.count());
                            org.junit.jupiter.api.Assertions.assertThrows(java.util.NoSuchElementException.class, iter::nextByte);

                            final ByteIteratorEx counted = (ByteIteratorEx) ByteStream.of(buf).iterator();

                            for (int i = 0; i < consumed; i++) {
                                counted.nextByte();
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
        final java.nio.ByteBuffer live = r04Buffer(1, content, 0, 6);
        final ByteIteratorEx liveIter = (ByteIteratorEx) ByteStream.of(live).iterator();
        liveIter.advance(2);
        live.put(1, (byte) 77);
        live.put(3, (byte) 88);
        org.junit.jupiter.api.Assertions.assertEquals(content[2], liveIter.nextByte());
        org.junit.jupiter.api.Assertions.assertEquals((byte) 88, liveIter.nextByte());
        org.junit.jupiter.api.Assertions.assertEquals(2, liveIter.count());
        org.junit.jupiter.api.Assertions.assertFalse(liveIter.hasNext());
    }

    private static java.nio.ByteBuffer r04Buffer(final int kind, final byte[] content, final int from, final int to) {
        // kind 0: read-only view of a heap array; kind 1: writable view of direct memory. Neither exposes an array.
        final java.nio.ByteBuffer buf = kind == 0 ? java.nio.ByteBuffer.wrap(content.clone()).asReadOnlyBuffer()
                : java.nio.ByteBuffer.allocateDirect(content.length).put(content).clear();
        buf.limit(to);
        buf.position(from);
        return buf;
    }

    private static byte[] r04Step(final byte[] a, final int step) {
        final byte[] result = new byte[(a.length + step - 1) / step];

        for (int i = 0; i < result.length; i++) {
            result[i] = a[i * step];
        }

        return result;
    }
    // ---- re-review 2026-11-03 R04 end ----
}

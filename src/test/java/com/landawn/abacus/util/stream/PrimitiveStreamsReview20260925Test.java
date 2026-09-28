package com.landawn.abacus.util.stream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.ByteIterator;
import com.landawn.abacus.util.CharIterator;
import com.landawn.abacus.util.IntIterator;
import com.landawn.abacus.util.ShortIterator;

/**
 * Review fixes of 2026-09-25 (fixer F11a) for the primitive stream families.
 *
 * <ul>
 *   <li><b>U30-02</b> - {@code Iterator{Int,Short,Byte,Char}Stream.sum()} accumulated with {@code Math.addExact} on the
 *       {@code long} accumulator, so a partial sum leaving the {@code long} range threw "long overflow" even when the exact
 *       total fits the documented {@code int}. Now the same wrap-safe accumulation as {@code average()} (carry counter): the
 *       documented {@code ArithmeticException} ("integer overflow") is thrown only for a total outside the int range.
 *       The long wrap itself needs more than 2^32 (Int) / 2^47 (Char) / 2^48 (Short) / 2^56 (Byte) elements, which no unit
 *       test can afford; it was verified once with a probe (recorded in the F11a fix report). These tests pin everything the
 *       rewrite could have changed at unit-test sizes: normal totals, partial sums leaving the int range in both directions,
 *       the exact int boundaries, the documented overflow (type and message), the primary failure of a failing source, and
 *       close-once on success and on failure.</li>
 *   <li><b>U26-02 / U27-03</b> - {@code Short/Float/DoubleStream.zip(Collection, XNFunction[, valuesForNone])} now use the
 *       shared {@code UNBOX_ZIP_RESULT} constant like Int/Long/Char/Byte (a null result is an NPE).</li>
 * </ul>
 */
@Tag("unit")
public class PrimitiveStreamsReview20260925Test extends TestBase {

    private static final int MAX = Integer.MAX_VALUE;
    private static final int MIN = Integer.MIN_VALUE;

    /** An iterator that yields {@code prefix} and then fails: the source failure must be the primary exception. */
    private static IntIterator failingAfter(final int... prefix) {
        return new IntIterator() {
            private int i = 0;

            @Override
            public boolean hasNext() {
                return true;
            }

            @Override
            public int nextInt() {
                if (i < prefix.length) {
                    return prefix[i++];
                }

                throw new IllegalStateException("boom");
            }
        };
    }

    // ------------------------------------------------------------------------------------------------------------
    // U30-02 IteratorIntStream.sum()
    // ------------------------------------------------------------------------------------------------------------

    @Test
    public void u3002_intSum_partialSumMayLeaveIntRangeAsLongAsTheTotalComesBack() {
        assertEquals(3, IntStream.of(IntIterator.of(MAX, MAX, MIN, MIN, 5)).sum()); // 2 * MAX + 2 * MIN = -2
        assertEquals(-7, IntStream.of(IntIterator.of(MIN, MIN, MAX, MAX, -5)).sum()); // 2 * MIN + 2 * MAX = -2 as well
        assertEquals(-100_000, IntStream.concat(IntStream.repeat(MAX, 100_000), IntStream.repeat(MIN, 100_000)).sum());
        assertEquals(100_000, IntStream.concat(IntStream.repeat(MIN, 100_000), IntStream.repeat(MAX, 100_000), IntStream.repeat(1, 200_000)).sum());
        // sum() and average() agree on the same input
        assertEquals(0.6, IntStream.of(IntIterator.of(MAX, MAX, MIN, MIN, 5)).average().getAsDouble(), 0.0);
    }

    @Test
    public void u3002_intSum_exactIntBoundariesAndDocumentedOverflow() {
        assertEquals(0, IntStream.of(IntIterator.of()).sum());
        assertEquals(15, IntStream.of(IntIterator.of(1, 2, 3, 4, 5)).sum());
        assertEquals(MAX, IntStream.of(IntIterator.of(MAX)).sum());
        assertEquals(MIN, IntStream.of(IntIterator.of(MIN)).sum());
        assertEquals(MAX, IntStream.of(IntIterator.of(MAX, MAX, MIN, 1)).sum()); // partial 2 * MAX, total exactly MAX
        assertEquals(MIN, IntStream.of(IntIterator.of(MIN, MIN, MAX, 1)).sum()); // partial 2 * MIN, total exactly MIN

        ArithmeticException e = assertThrows(ArithmeticException.class, () -> IntStream.of(IntIterator.of(MAX, 1)).sum());
        assertEquals("integer overflow", e.getMessage());
        e = assertThrows(ArithmeticException.class, () -> IntStream.of(IntIterator.of(MIN, -1)).sum());
        assertEquals("integer overflow", e.getMessage());
        e = assertThrows(ArithmeticException.class, () -> IntStream.of(IntIterator.of(MAX, MAX, MIN, 2)).sum()); // total MAX + 1
        assertEquals("integer overflow", e.getMessage());
        e = assertThrows(ArithmeticException.class, () -> IntStream.of(IntIterator.of(MIN, MIN, MAX, 0)).sum()); // total MIN - 1
        assertEquals("integer overflow", e.getMessage());
        e = assertThrows(ArithmeticException.class, () -> IntStream.repeat(MAX, 3).sum());
        assertEquals("integer overflow", e.getMessage());
    }

    @Test
    public void u3002_intSum_closesOnceOnSuccessOnOverflowAndOnSourceFailure() {
        final AtomicInteger closed = new AtomicInteger();

        assertEquals(3, IntStream.of(IntIterator.of(MAX, MAX, MIN, MIN, 5)).onClose(closed::incrementAndGet).sum());
        assertEquals(1, closed.get());

        final ArithmeticException ae = assertThrows(ArithmeticException.class,
                () -> IntStream.of(IntIterator.of(MAX, MAX, MIN, 2)).onClose(closed::incrementAndGet).sum());
        assertEquals("integer overflow", ae.getMessage());
        assertEquals(2, closed.get());

        final IllegalStateException ise = assertThrows(IllegalStateException.class,
                () -> IntStream.of(failingAfter(MAX, MAX)).onClose(closed::incrementAndGet).sum());
        assertEquals("boom", ise.getMessage());
        assertEquals(3, closed.get());
    }

    // ------------------------------------------------------------------------------------------------------------
    // U30-02 IteratorShortStream.sum()
    // ------------------------------------------------------------------------------------------------------------

    @Test
    public void u3002_shortSum_boundariesPartialExcursionAndDocumentedOverflow() {
        assertEquals(0, ShortStream.of(ShortIterator.of()).sum());
        assertEquals(6, ShortStream.of(ShortIterator.of((short) 1, (short) 2, (short) 3)).sum());
        assertEquals(0, ShortStream.of(ShortIterator.of(Short.MAX_VALUE, Short.MIN_VALUE, (short) 1)).sum());
        assertEquals(2147483646, ShortStream.repeat(Short.MAX_VALUE, 65538).sum());
        assertEquals(MIN, ShortStream.repeat(Short.MIN_VALUE, 65536).sum());
        // partial sum 70000 * MAX > Integer.MAX_VALUE, total back in range
        assertEquals(-70_000, ShortStream.concat(ShortStream.repeat(Short.MAX_VALUE, 70_000), ShortStream.repeat(Short.MIN_VALUE, 70_000)).sum());

        ArithmeticException e = assertThrows(ArithmeticException.class, () -> ShortStream.repeat(Short.MAX_VALUE, 65539).sum());
        assertEquals("integer overflow", e.getMessage());
        e = assertThrows(ArithmeticException.class, () -> ShortStream.repeat(Short.MIN_VALUE, 65537).sum());
        assertEquals("integer overflow", e.getMessage());

        final AtomicInteger closed = new AtomicInteger();
        assertEquals(6, ShortStream.of(ShortIterator.of((short) 1, (short) 2, (short) 3)).onClose(closed::incrementAndGet).sum());
        assertEquals(1, closed.get());
        assertThrows(ArithmeticException.class, () -> ShortStream.repeat(Short.MAX_VALUE, 65539).onClose(closed::incrementAndGet).sum());
        assertEquals(2, closed.get());
    }

    // ------------------------------------------------------------------------------------------------------------
    // U30-02 IteratorByteStream.sum()
    // ------------------------------------------------------------------------------------------------------------

    @Test
    public void u3002_byteSum_boundariesPartialExcursionAndDocumentedOverflow() {
        assertEquals(0, ByteStream.of(ByteIterator.of()).sum());
        assertEquals(6, ByteStream.of(ByteIterator.of((byte) 1, (byte) 2, (byte) 3)).sum());
        assertEquals(0, ByteStream.of(ByteIterator.of((byte) 127, (byte) -128, (byte) 1)).sum());
        assertEquals(2_147_483_640, ByteStream.repeat((byte) 127, 16_909_320).sum());
        assertEquals(MIN, ByteStream.repeat((byte) -128, 16_777_216).sum());
        // partial sum 17_000_000 * 127 > Integer.MAX_VALUE, total back in range
        assertEquals(-17_000_000, ByteStream.concat(ByteStream.repeat((byte) 127, 17_000_000), ByteStream.repeat((byte) -128, 17_000_000)).sum());

        ArithmeticException e = assertThrows(ArithmeticException.class, () -> ByteStream.repeat((byte) 127, 16_909_321).sum());
        assertEquals("integer overflow", e.getMessage());
        e = assertThrows(ArithmeticException.class, () -> ByteStream.repeat((byte) -128, 16_777_217).sum());
        assertEquals("integer overflow", e.getMessage());

        final AtomicInteger closed = new AtomicInteger();
        assertEquals(6, ByteStream.of(ByteIterator.of((byte) 1, (byte) 2, (byte) 3)).onClose(closed::incrementAndGet).sum());
        assertEquals(1, closed.get());
        assertThrows(ArithmeticException.class, () -> ByteStream.repeat((byte) 127, 16_909_321).onClose(closed::incrementAndGet).sum());
        assertEquals(2, closed.get());
    }

    // ------------------------------------------------------------------------------------------------------------
    // U30-02 IteratorCharStream.sum() (char values are never negative, so a partial sum cannot leave and come back)
    // ------------------------------------------------------------------------------------------------------------

    @Test
    public void u3002_charSum_boundariesAndDocumentedOverflow() {
        assertEquals(0, CharStream.of(CharIterator.of()).sum());
        assertEquals(131, CharStream.of(CharIterator.of('A', 'B')).sum());
        assertEquals(65535 * 3, CharStream.of(CharIterator.of('\uFFFF', '\uFFFF', '\uFFFF')).sum());
        assertEquals(2_147_450_880, CharStream.repeat('\uFFFF', 32768).sum());

        final ArithmeticException e = assertThrows(ArithmeticException.class, () -> CharStream.repeat('\uFFFF', 32769).sum());
        assertEquals("integer overflow", e.getMessage());

        final AtomicInteger closed = new AtomicInteger();
        assertEquals(131, CharStream.of(CharIterator.of('A', 'B')).onClose(closed::incrementAndGet).sum());
        assertEquals(1, closed.get());
        assertThrows(ArithmeticException.class, () -> CharStream.repeat('\uFFFF', 32769).onClose(closed::incrementAndGet).sum());
        assertEquals(2, closed.get());
    }

    // ------------------------------------------------------------------------------------------------------------
    // U26-02 / U27-03 zip(Collection, XNFunction[, valuesForNone]) through the shared UNBOX_ZIP_RESULT constant
    // ------------------------------------------------------------------------------------------------------------

    @Test
    public void u2602_shortZip_nullResultIsNpeNonNullResultUnboxed() {
        assertArrayEquals(new short[] { 11, 22 },
                ShortStream.zip(Arrays.asList(ShortStream.of((short) 1, (short) 2), ShortStream.of((short) 10, (short) 20)), a -> (short) (a[0] + a[1])).toArray());
        assertArrayEquals(new short[] { 11, 20 },
                ShortStream.zip(Arrays.asList(ShortStream.of((short) 1), ShortStream.of((short) 10, (short) 20)), new short[] { 0, 0 }, a -> (short) (a[0] + a[1]))
                        .toArray());

        NullPointerException e = assertThrows(NullPointerException.class,
                () -> ShortStream.zip(Arrays.asList(ShortStream.of((short) 1), ShortStream.of((short) 2)), a -> null).toArray());
        assertEquals("zipFunction returned null", e.getMessage());
        e = assertThrows(NullPointerException.class,
                () -> ShortStream.zip(Arrays.asList(ShortStream.of((short) 1), ShortStream.of((short) 2)), new short[] { 0, 0 }, a -> null).toArray());
        assertEquals("zipFunction returned null", e.getMessage());
    }

    @Test
    public void u2703_floatZip_nullResultIsNpeNonNullResultUnboxed() {
        assertArrayEquals(new float[] { 11f, 22f }, FloatStream.zip(Arrays.asList(FloatStream.of(1f, 2f), FloatStream.of(10f, 20f)), a -> a[0] + a[1]).toArray(), 0f);
        assertArrayEquals(new float[] { 11f, 20f },
                FloatStream.zip(Arrays.asList(FloatStream.of(1f), FloatStream.of(10f, 20f)), new float[] { 0f, 0f }, a -> a[0] + a[1]).toArray(), 0f);

        NullPointerException e = assertThrows(NullPointerException.class,
                () -> FloatStream.zip(Arrays.asList(FloatStream.of(1f), FloatStream.of(2f)), a -> null).toArray());
        assertEquals("zipFunction returned null", e.getMessage());
        e = assertThrows(NullPointerException.class,
                () -> FloatStream.zip(Arrays.asList(FloatStream.of(1f), FloatStream.of(2f)), new float[] { 0f, 0f }, a -> null).toArray());
        assertEquals("zipFunction returned null", e.getMessage());
    }

    @Test
    public void u2703_doubleZip_nullResultIsNpeNonNullResultUnboxed() {
        assertArrayEquals(new double[] { 11d, 22d }, DoubleStream.zip(Arrays.asList(DoubleStream.of(1d, 2d), DoubleStream.of(10d, 20d)), a -> a[0] + a[1]).toArray(),
                0d);
        assertArrayEquals(new double[] { 11d, 20d },
                DoubleStream.zip(Arrays.asList(DoubleStream.of(1d), DoubleStream.of(10d, 20d)), new double[] { 0d, 0d }, a -> a[0] + a[1]).toArray(), 0d);

        NullPointerException e = assertThrows(NullPointerException.class,
                () -> DoubleStream.zip(Arrays.asList(DoubleStream.of(1d), DoubleStream.of(2d)), a -> null).toArray());
        assertEquals("zipFunction returned null", e.getMessage());
        e = assertThrows(NullPointerException.class,
                () -> DoubleStream.zip(Arrays.asList(DoubleStream.of(1d), DoubleStream.of(2d)), new double[] { 0d, 0d }, a -> null).toArray());
        assertEquals("zipFunction returned null", e.getMessage());
    }
}

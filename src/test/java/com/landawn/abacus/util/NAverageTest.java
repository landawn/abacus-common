package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.LinkedList;

import org.junit.jupiter.api.Test;

public class NAverageTest extends NTestSupport {

    @Test
    public void testAverage_primitiveArrays() {
        assertEquals(('a' + 'b' + 'c') / 3.0, N.average('a', 'b', 'c'), DELTA);
        assertEquals(('b' + 'c') / 2.0, N.average(new char[] { 'a', 'b', 'c', 'd' }, 1, 3), DELTA);
        assertEquals(0.0, N.average((char[]) null), DELTA);
        assertEquals(0.0, N.average(new char[] {}), DELTA);

        assertEquals(3.0, N.average(byteArray), DELTA);
        assertEquals(3.0, N.average(byteArray, 1, 4), DELTA);
        assertEquals(3.0, N.average(shortArray), DELTA);
        assertEquals(3.0, N.average(intArray), DELTA);
        assertEquals(2.5, N.average(new int[] { 1, 2, 3, 4 }, 1, 3), DELTA);
        assertEquals(3.0, N.average(longArray), DELTA);
        assertEquals(3.0, N.average(floatArray), DELTA);
        assertEquals(3.0, N.average(doubleArray), DELTA);

        assertEquals(0.0, N.average((int[]) null), DELTA);
        assertEquals(0.0, N.average(new int[] {}), DELTA);
        assertEquals(0.0, N.average(new byte[] { 1, 2, 3 }, 1, 1), DELTA);
        assertEquals(2.0, N.average(1, 2, 3), DELTA);
        assertEquals((Integer.MAX_VALUE + Integer.MIN_VALUE) / 2.0, N.average(Integer.MAX_VALUE, Integer.MIN_VALUE), DELTA);
        assertEquals(Float.MAX_VALUE, N.average(Float.MAX_VALUE, Float.MAX_VALUE));
        assertEquals(Double.MAX_VALUE, N.average(Double.MAX_VALUE, Double.MAX_VALUE));
    }

    @Test
    public void testAverageInt() {
        assertEquals(2.0, N.averageInt(new Integer[] { 1, 2, 3 }), DELTA);
        assertEquals(2.5, N.averageInt(new Integer[] { 1, 2, 3, 4 }, 1, 3), DELTA);
        assertEquals(0.0, N.averageInt((Integer[]) null), DELTA);
        assertEquals(4.0, N.averageInt(new Integer[] { 1, 2, 3 }, x -> x * 2), DELTA);
        assertEquals(6.0, N.averageInt(integerArray, 1, 4, x -> x * 2), DELTA);
        assertEquals(3.0, N.averageInt(new String[] { "1", "3", "5" }, s -> Integer.parseInt(s)), DELTA);

        assertEquals(2.5, N.averageInt(Arrays.asList(1, 2, 3, 4), 1, 3), DELTA);
        assertEquals(5.0, N.averageInt(CommonUtil.toLinkedHashSet(1, 2, 3, 4), 1, 3, x -> x * 2), DELTA);
        assertEquals(3.0, N.averageInt(new LinkedHashSet<>(Arrays.asList(1, 2, 3, 4, 5)), 1, 4, x -> x), DELTA);
        assertEquals(0.0, N.averageInt(new LinkedHashSet<>(Arrays.asList(1, 2, 3)), 2, 2, x -> x), DELTA);
        assertEquals(2.0, N.averageInt(Arrays.asList(1, 2, 3)), DELTA);
        assertEquals(0.0, N.averageInt(Collections.emptyList()), DELTA);
    }

    @Test
    public void testAverageLong() {
        Long[] values = { 1L, 2L, 3L, 4L, 5L };
        assertEquals(3.0, N.averageLong(values), DELTA);
        assertEquals(3.0, N.averageLong(values, 1, 4), DELTA);
        assertEquals(6.0, N.averageLong(values, x -> x * 2), DELTA);
        assertEquals(2.5, N.averageLong(Arrays.asList(1L, 2L, 3L, 4L), 1, 3), DELTA);
        assertEquals(5.0, N.averageLong(CommonUtil.toLinkedHashSet(1L, 2L, 3L, 4L), 1, 3, x -> x * 2), DELTA);
        assertEquals(2.0, N.averageLong(Arrays.asList(1L, 2L, 3L)), DELTA);
        assertEquals(0.0, N.averageLong(Collections.emptyList()), DELTA);

        final double expectedMax = Long.MAX_VALUE;
        final Long[] maxValues = { Long.MAX_VALUE, Long.MAX_VALUE };
        assertEquals(expectedMax, N.average(new long[] { Long.MAX_VALUE, Long.MAX_VALUE }), 0D);
        assertEquals(expectedMax, N.averageLong(maxValues), 0D);
        assertEquals(expectedMax, N.averageLong(maxValues, value -> value), 0D);
        assertEquals(expectedMax, N.averageLong(Arrays.asList(maxValues), 0, maxValues.length), 0D);
        assertEquals(expectedMax, N.averageLong(new LinkedList<>(Arrays.asList(maxValues)), 0, maxValues.length, value -> value), 0D);
        assertEquals(expectedMax, N.averageLong((Iterable<Long>) () -> Arrays.asList(maxValues).iterator()), 0D);
        assertEquals(Long.MIN_VALUE, N.average(new long[] { Long.MIN_VALUE, Long.MIN_VALUE }), 0D);
    }

    @Test
    public void testAverageDouble() {
        Double[] values = { 1.0, 2.0, 3.0, 4.0, 5.0 };
        assertEquals(3.0, N.averageDouble(values), DELTA);
        assertEquals(3.0, N.averageDouble(values, 1, 4), DELTA);
        assertEquals(6.0, N.averageDouble(values, x -> x * 2), DELTA);
        assertEquals(2.5, N.averageDouble(Arrays.asList(1.0, 2.0, 3.0, 4.0), 1, 3), DELTA);
        assertEquals(0.0, N.averageDouble((Iterable<Double>) null), DELTA);
        assertEquals(0.15, N.averageDouble(Arrays.asList(0.1, 0.2)), DELTA);
        assertEquals(0.2, N.averageDouble(Arrays.asList("0.1", "0.2", "0.3"), s -> Double.parseDouble(s)), DELTA);
        assertEquals(2.0, N.averageDouble(Arrays.asList(1.0, 2.0, 3.0)), DELTA);
        assertEquals(0.0, N.averageDouble(Collections.emptyList()), DELTA);
    }

    @Test
    public void testAverageBigNumber() {
        assertEquals(BigDecimal.ZERO, N.averageBigInteger(null));
        assertEquals(new BigDecimal("2"), N.averageBigInteger(Arrays.asList(BigInteger.ONE, BigInteger.valueOf(2), BigInteger.valueOf(3))));
        assertEquals(BigDecimal.valueOf(4), N.averageBigInteger(Arrays.asList(1, 2, 3), x -> BigInteger.valueOf(x * 2)));

        assertEquals(BigDecimal.ZERO, N.averageBigDecimal(null));
        assertEquals(new BigDecimal("2.1"), N.averageBigDecimal(Arrays.asList(new BigDecimal("1.1"), new BigDecimal("2.2"), new BigDecimal("3.0"))));
        assertEquals(BigDecimal.valueOf(4), N.averageBigDecimal(Arrays.asList(1, 2, 3), x -> BigDecimal.valueOf(x * 2)));
    }

    @Test
    public void testAverage_emptyAndOverflow() {
        assertEquals(0d, N.average(new int[0]), 0.0);
        assertEquals(0d, N.average((int[]) null), 0.0);
        assertEquals(0d, N.average(new long[0]), 0.0);
        assertEquals(0d, N.average(new double[0]), 0.0);

        byte[] allBytes = new byte[256];
        Arrays.fill(allBytes, Byte.MAX_VALUE);
        assertEquals(Byte.MAX_VALUE, N.average(allBytes), 0.0);

        char[] chars = new char[33000];
        Arrays.fill(chars, Character.MAX_VALUE);
        assertEquals(65535.0d, N.average(chars), 0.0d);

        short[] shorts = new short[70000];
        Arrays.fill(shorts, Short.MAX_VALUE);
        assertEquals(32767.0d, N.average(shorts), 0.0d);

        byte[] manyBytes = new byte[17_000_000];
        Arrays.fill(manyBytes, Byte.MAX_VALUE);
        assertEquals(127.0d, N.average(manyBytes), 0.0d);

        assertEquals(20.0d, N.average(new char[] { 10, 20, 30 }, 0, 3), 0.0d);
        assertEquals(20.0d, N.average(new byte[] { 10, 20, 30 }, 0, 3), 0.0d);
        assertEquals(20.0d, N.average(new short[] { 10, 20, 30 }, 0, 3), 0.0d);
    }
}

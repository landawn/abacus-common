package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;

import org.junit.jupiter.api.Test;

public class NSumTest extends NTestSupport {

    @Test
    public void testSum_primitiveArrays() {
        assertEquals(294, N.sum('a', 'b', 'c'));
        assertEquals('b' + 'c', N.sum(new char[] { 'a', 'b', 'c', 'd' }, 1, 3));
        assertEquals(0, N.sum((char[]) null));
        assertEquals(0, N.sum(new char[] {}));
        assertEquals(0, N.sum(new char[] { 'a', 'b' }, 1, 1));
        assertThrows(IndexOutOfBoundsException.class, () -> N.sum(new char[] { 'a' }, 0, 2));

        assertEquals(15, N.sum(byteArray));
        assertEquals(9, N.sum(byteArray, 1, 4));
        assertEquals(0, N.sum(new byte[] {}));

        assertEquals(15, N.sum(shortArray));
        assertEquals(9, N.sum(shortArray, 1, 4));

        assertEquals(15, N.sum(intArray));
        assertEquals(9, N.sum(intArray, 1, 4));
        assertEquals(0, N.sum((int[]) null));
        assertEquals(6, N.sum(1, 2, 3));
        assertEquals(Integer.MAX_VALUE, N.sum(new int[] { Integer.MAX_VALUE }));
        assertThrows(ArithmeticException.class, () -> N.sum(new int[] { Integer.MAX_VALUE, 1 }));

        assertEquals(15L, N.sum(longArray));
        assertEquals(9L, N.sum(longArray, 1, 4));
        assertEquals(0L, N.sum((long[]) null));
        assertEquals(Long.MAX_VALUE, N.sum(Long.MAX_VALUE - 1L, 1L));

        assertEquals(15.0f, N.sum(floatArray), DELTAf);
        assertEquals(9.0f, N.sum(floatArray, 1, 4), DELTAf);
        assertEquals(0f, N.sum((float[]) null), DELTAf);
        assertEquals(0.3f, N.sum(0.1f, 0.2f), DELTAf);

        assertEquals(15.0, N.sum(doubleArray), DELTA);
        assertEquals(9.0, N.sum(doubleArray, 1, 4), DELTA);
        assertEquals(0.0, N.sum((double[]) null), DELTA);
        assertEquals(0.3, N.sum(0.1, 0.2), DELTA);
        assertEquals(Double.POSITIVE_INFINITY, N.sum(Double.MAX_VALUE, Double.MAX_VALUE, Double.MIN_VALUE));
        assertEquals(Double.MAX_VALUE, N.sum(Double.MAX_VALUE, -Double.MAX_VALUE, Double.MAX_VALUE));
    }

    @Test
    public void testSumToLongAndDouble() {
        assertEquals(15L, N.sumToLong(intArray));
        assertEquals(9L, N.sumToLong(intArray, 1, 4));
        assertEquals(0L, N.sumToLong((int[]) null));
        assertEquals((long) Integer.MAX_VALUE + 1, N.sumToLong(new int[] { Integer.MAX_VALUE, 1 }));

        assertEquals(15.0, N.sumToDouble(floatArray), DELTA);
        assertEquals(9.0, N.sumToDouble(floatArray, 1, 4), DELTA);
        assertEquals(0.0, N.sumToDouble((float[]) null), DELTA);
        assertEquals(0.3, N.sumToDouble(0.1f, 0.2f), DELTA);
    }

    @Test
    public void testSumInt() {
        assertEquals(6, N.sumInt(new Integer[] { 1, 2, 3 }));
        assertEquals(5, N.sumInt(new Integer[] { 1, 2, 3, 4 }, 1, 3));
        assertEquals(0, N.sumInt((Integer[]) null));
        assertEquals(12, N.sumInt(new Integer[] { 1, 2, 3 }, x -> x * 2));
        assertEquals(18, N.sumInt(integerArray, 1, 4, x -> x * 2));
        assertEquals(9, N.sumInt(new String[] { "1", "3", "5" }, s -> Integer.parseInt(s)));
        assertThrows(ArithmeticException.class, () -> N.sumInt(new Integer[] { Integer.MAX_VALUE, 1 }));

        assertEquals(5, N.sumInt(Arrays.asList(1, 2, 3, 4), 1, 3));
        assertEquals(10, N.sumInt(Arrays.asList(1, 2, 3, 4), 1, 3, x -> x * 2));
        assertEquals(9, N.sumInt(new LinkedHashSet<>(Arrays.asList(1, 2, 3, 4, 5)), 1, 4, x -> x));
        assertEquals(0, N.sumInt(new LinkedHashSet<>(Arrays.asList(1, 2, 3)), 2, 2, x -> x));
        assertEquals(6, N.sumInt(Arrays.asList(1, 2, 3)));
        assertEquals(0, N.sumInt(Collections.emptyList()));
        assertThrows(ArithmeticException.class, () -> N.sumInt(Arrays.asList(Integer.MAX_VALUE, 1)));
    }

    @Test
    public void testSumIntToLong() {
        assertEquals(0L, N.sumIntToLong((Iterable<Integer>) null));
        assertEquals(6L, N.sumIntToLong(Arrays.asList(1, 2, 3)));
        assertEquals(12L, N.sumIntToLong(Arrays.asList(1, 2, 3), x -> x * 2));
        assertEquals(Integer.MAX_VALUE + 1L, N.sumIntToLong(Arrays.asList(Integer.MAX_VALUE, 1)));
        assertEquals(9L, N.sumIntToLong(Arrays.asList("1", "3", "5"), s -> Integer.parseInt(s)));
    }

    @Test
    public void testSumLong() {
        Long[] values = { 1L, 2L, 3L, 4L, 5L };
        assertEquals(15L, N.sumLong(values));
        assertEquals(9L, N.sumLong(values, 1, 4));
        assertEquals(30L, N.sumLong(values, x -> x * 2));
        assertEquals(18L, N.sumLong(values, 1, 4, x -> x * 2));
        assertEquals(0L, N.sumLong((Long[]) null));
        assertEquals(9L, N.sumLong(new String[] { "1", "3", "5" }, s -> Long.parseLong(s)));
        assertEquals(Long.MAX_VALUE, N.sumLong(new Long[] { Long.MAX_VALUE - 1L, 1L }));

        List<Long> list = Arrays.asList(1L, 2L, 3L, 4L, 5L);
        assertEquals(15L, N.sumLong(list));
        assertEquals(9L, N.sumLong(list, 1, 4));
        assertEquals(18L, N.sumLong(list, 1, 4, x -> x * 2));
        assertEquals(9L, N.sumLong(new LinkedHashSet<>(list), 1, 4, x -> x));
        assertEquals(0L, N.sumLong(Collections.emptyList()));
    }

    @Test
    public void testSumDouble() {
        Double[] values = { 1.0, 2.0, 3.0, 4.0, 5.0 };
        assertEquals(15.0, N.sumDouble(values), DELTA);
        assertEquals(9.0, N.sumDouble(values, 1, 4), DELTA);
        assertEquals(30.0, N.sumDouble(values, x -> x * 2), DELTA);
        assertEquals(0.0, N.sumDouble((Iterable<Double>) null), DELTA);
        assertEquals(0.6, N.sumDouble(Arrays.asList("0.1", "0.2", "0.3"), s -> Double.parseDouble(s)), DELTA);

        List<Double> list = Arrays.asList(1.0, 2.0, 3.0, 4.0, 5.0);
        assertEquals(15.0, N.sumDouble(list), DELTA);
        assertEquals(9.0, N.sumDouble(list, 1, 4), DELTA);
        assertEquals(18.0, N.sumDouble(list, 1, 4, x -> x * 2), DELTA);
        assertEquals(9.0, N.sumDouble(new LinkedHashSet<>(list), 1, 4, x -> x), DELTA);
        assertEquals(0.0, N.sumDouble(Collections.emptyList()), DELTA);
    }

    @Test
    public void testSumBigNumber() {
        assertEquals(BigInteger.ZERO, N.sumBigInteger(null));
        assertEquals(BigInteger.valueOf(6), N.sumBigInteger(Arrays.asList(BigInteger.ONE, BigInteger.valueOf(2), BigInteger.valueOf(3))));
        assertEquals(BigInteger.valueOf(12), N.sumBigInteger(Arrays.asList(1, 2, 3), x -> BigInteger.valueOf(x * 2)));
        assertEquals(BigInteger.valueOf(5),
                N.sumBigInteger(Arrays.asList(BigInteger.ONE, null, BigInteger.valueOf(4)), java.util.function.Function.identity()));

        assertEquals(BigDecimal.ZERO, N.sumBigDecimal(null));
        assertEquals(new BigDecimal("6.3"), N.sumBigDecimal(Arrays.asList(new BigDecimal("1.1"), new BigDecimal("2.2"), new BigDecimal("3.0"))));
        assertEquals(BigDecimal.valueOf(12), N.sumBigDecimal(Arrays.asList(1, 2, 3), x -> BigDecimal.valueOf(x * 2)));
        assertEquals(new BigDecimal("6.3"), N.sumBigDecimal(Arrays.asList("1.1", "2.2", "3.0"), s -> new BigDecimal(s)));
    }
}

package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

public class N105Test extends TestBase {

    // Test data setup
    private static final int[] EMPTY_INT_ARRAY = new int[0];
    private static final long[] EMPTY_LONG_ARRAY = new long[0];
    private static final double[] EMPTY_DOUBLE_ARRAY = new double[0];
    private static final float[] EMPTY_FLOAT_ARRAY = new float[0];
    private static final short[] EMPTY_SHORT_ARRAY = new short[0];
    private static final byte[] EMPTY_BYTE_ARRAY = new byte[0];
    private static final char[] EMPTY_CHAR_ARRAY = new char[0];
    private static final boolean[] EMPTY_BOOLEAN_ARRAY = new boolean[0];
    private static final String[] EMPTY_STRING_ARRAY = new String[0];
    private static final Object[] EMPTY_OBJECT_ARRAY = new Object[0];

    // sum for collections
    @Test
    public void testSumInt_Array() {
        assertEquals(6, N.sumInt(new Integer[] { 1, 2, 3 }));
        assertEquals(5, N.sumInt(new Integer[] { 1, 2, 3, 4 }, 1, 3));
        assertEquals(0, N.sumInt(new Integer[] {}));
        assertEquals(6, N.sumInt(new Integer[] { 1, 2, 3 }, x -> x));
        assertEquals(12, N.sumInt(new Integer[] { 1, 2, 3 }, x -> x * 2));
    }

    @Test
    public void testSumInt_Collection() {
        assertEquals(5, N.sumInt(Arrays.asList(1, 2, 3, 4), 1, 3));
        assertEquals(10, N.sumInt(Arrays.asList(1, 2, 3, 4), 1, 3, x -> x * 2));
    }

    @Test
    public void testSumInt_Iterable() {
        assertEquals(6, N.sumInt(Arrays.asList(1, 2, 3)));
        assertEquals(12, N.sumInt(Arrays.asList(1, 2, 3), x -> x * 2));
        assertEquals(0, N.sumInt(Collections.emptyList()));
    }

    @Test
    public void testSumIntToLong_Iterable() {
        assertEquals(6L, N.sumIntToLong(Arrays.asList(1, 2, 3)));
        assertEquals(12L, N.sumIntToLong(Arrays.asList(1, 2, 3), x -> x * 2));
        assertEquals(0L, N.sumIntToLong(Collections.emptyList()));
    }

    @Test
    public void testSumLong_Array() {
        assertEquals(6L, N.sumLong(new Long[] { 1L, 2L, 3L }));
        assertEquals(5L, N.sumLong(new Long[] { 1L, 2L, 3L, 4L }, 1, 3));
        assertEquals(0L, N.sumLong(new Long[] {}));
        assertEquals(6L, N.sumLong(new Long[] { 1L, 2L, 3L }, x -> x));
        assertEquals(12L, N.sumLong(new Long[] { 1L, 2L, 3L }, x -> x * 2));
    }

    @Test
    public void testSumLong_Collection() {
        assertEquals(5L, N.sumLong(Arrays.asList(1L, 2L, 3L, 4L), 1, 3));
        assertEquals(10L, N.sumLong(Arrays.asList(1L, 2L, 3L, 4L), 1, 3, x -> x * 2));
    }

    @Test
    public void testSumLong_Iterable() {
        assertEquals(6L, N.sumLong(Arrays.asList(1L, 2L, 3L)));
        assertEquals(12L, N.sumLong(Arrays.asList(1L, 2L, 3L), x -> x * 2));
        assertEquals(0L, N.sumLong(Collections.emptyList()));
    }

    @Test
    public void testSumDouble_Array() {
        assertEquals(6.0, N.sumDouble(new Double[] { 1.0, 2.0, 3.0 }), 0.001);
        assertEquals(5.0, N.sumDouble(new Double[] { 1.0, 2.0, 3.0, 4.0 }, 1, 3), 0.001);
        assertEquals(0.0, N.sumDouble(new Double[] {}), 0.001);
        assertEquals(6.0, N.sumDouble(new Double[] { 1.0, 2.0, 3.0 }, x -> x), 0.001);
        assertEquals(12.0, N.sumDouble(new Double[] { 1.0, 2.0, 3.0 }, x -> x * 2), 0.001);
    }

    @Test
    public void testSumDouble_Collection() {
        assertEquals(5.0, N.sumDouble(Arrays.asList(1.0, 2.0, 3.0, 4.0), 1, 3), 0.001);
        assertEquals(10.0, N.sumDouble(Arrays.asList(1.0, 2.0, 3.0, 4.0), 1, 3, x -> x * 2), 0.001);
    }

    @Test
    public void testSumDouble_Iterable() {
        assertEquals(6.0, N.sumDouble(Arrays.asList(1.0, 2.0, 3.0)), 0.001);
        assertEquals(12.0, N.sumDouble(Arrays.asList(1.0, 2.0, 3.0), x -> x * 2), 0.001);
        assertEquals(0.0, N.sumDouble(Collections.emptyList()), 0.001);
    }

    @Test
    public void testSumBigInteger() {
        assertEquals(BigInteger.valueOf(6), N.sumBigInteger(Arrays.asList(BigInteger.ONE, BigInteger.valueOf(2), BigInteger.valueOf(3))));
        assertEquals(BigInteger.valueOf(12), N.sumBigInteger(Arrays.asList(1, 2, 3), x -> BigInteger.valueOf(x * 2)));
        assertEquals(BigInteger.ZERO, N.sumBigInteger(Collections.emptyList()));
    }

    @Test
    public void testSumBigDecimal() {
        assertEquals(BigDecimal.valueOf(6), N.sumBigDecimal(Arrays.asList(BigDecimal.ONE, BigDecimal.valueOf(2), BigDecimal.valueOf(3))));
        assertEquals(BigDecimal.valueOf(12), N.sumBigDecimal(Arrays.asList(1, 2, 3), x -> BigDecimal.valueOf(x * 2)));
        assertEquals(BigDecimal.ZERO, N.sumBigDecimal(Collections.emptyList()));
    }

    // average for collections
    @Test
    public void testAverageInt_Array() {
        assertEquals(2.0, N.averageInt(new Integer[] { 1, 2, 3 }), 0.001);
        assertEquals(2.5, N.averageInt(new Integer[] { 1, 2, 3, 4 }, 1, 3), 0.001);
        assertEquals(0.0, N.averageInt(new Integer[] {}), 0.001);
        assertEquals(2.0, N.averageInt(new Integer[] { 1, 2, 3 }, x -> x), 0.001);
        assertEquals(4.0, N.averageInt(new Integer[] { 1, 2, 3 }, x -> x * 2), 0.001);
    }

    @Test
    public void testAverageInt_Collection() {
        assertEquals(2.5, N.averageInt(Arrays.asList(1, 2, 3, 4), 1, 3), 0.001);
        assertEquals(5.0, N.averageInt(Arrays.asList(1, 2, 3, 4), 1, 3, x -> x * 2), 0.001);
    }

    @Test
    public void testAverageInt_Iterable() {
        assertEquals(2.0, N.averageInt(Arrays.asList(1, 2, 3)), 0.001);
        assertEquals(4.0, N.averageInt(Arrays.asList(1, 2, 3), x -> x * 2), 0.001);
        assertEquals(0.0, N.averageInt(Collections.emptyList()), 0.001);
    }

    @Test
    public void testAverageLong_Array() {
        assertEquals(2.0, N.averageLong(new Long[] { 1L, 2L, 3L }), 0.001);
        assertEquals(2.5, N.averageLong(new Long[] { 1L, 2L, 3L, 4L }, 1, 3), 0.001);
        assertEquals(0.0, N.averageLong(new Long[] {}), 0.001);
        assertEquals(2.0, N.averageLong(new Long[] { 1L, 2L, 3L }, x -> x), 0.001);
        assertEquals(4.0, N.averageLong(new Long[] { 1L, 2L, 3L }, x -> x * 2), 0.001);
    }

    @Test
    public void testAverageLong_Collection() {
        assertEquals(2.5, N.averageLong(Arrays.asList(1L, 2L, 3L, 4L), 1, 3), 0.001);
        assertEquals(5.0, N.averageLong(Arrays.asList(1L, 2L, 3L, 4L), 1, 3, x -> x * 2), 0.001);
    }

    @Test
    public void testAverageLong_Iterable() {
        assertEquals(2.0, N.averageLong(Arrays.asList(1L, 2L, 3L)), 0.001);
        assertEquals(4.0, N.averageLong(Arrays.asList(1L, 2L, 3L), x -> x * 2), 0.001);
        assertEquals(0.0, N.averageLong(Collections.emptyList()), 0.001);
    }

    @Test
    public void testAverageDouble_Array() {
        assertEquals(2.0, N.averageDouble(new Double[] { 1.0, 2.0, 3.0 }), 0.001);
        assertEquals(2.5, N.averageDouble(new Double[] { 1.0, 2.0, 3.0, 4.0 }, 1, 3), 0.001);
        assertEquals(0.0, N.averageDouble(new Double[] {}), 0.001);
        assertEquals(2.0, N.averageDouble(new Double[] { 1.0, 2.0, 3.0 }, x -> x), 0.001);
        assertEquals(4.0, N.averageDouble(new Double[] { 1.0, 2.0, 3.0 }, x -> x * 2), 0.001);
    }

    @Test
    public void testAverageDouble_Collection() {
        assertEquals(2.5, N.averageDouble(Arrays.asList(1.0, 2.0, 3.0, 4.0), 1, 3), 0.001);
        assertEquals(5.0, N.averageDouble(Arrays.asList(1.0, 2.0, 3.0, 4.0), 1, 3, x -> x * 2), 0.001);
    }

    @Test
    public void testAverageDouble_Iterable() {
        assertEquals(2.0, N.averageDouble(Arrays.asList(1.0, 2.0, 3.0)), 0.001);
        assertEquals(4.0, N.averageDouble(Arrays.asList(1.0, 2.0, 3.0), x -> x * 2), 0.001);
        assertEquals(0.0, N.averageDouble(Collections.emptyList()), 0.001);
    }

    @Test
    public void testAverageBigInteger() {
        assertEquals(BigDecimal.valueOf(2), N.averageBigInteger(Arrays.asList(BigInteger.ONE, BigInteger.valueOf(2), BigInteger.valueOf(3))));
        assertEquals(BigDecimal.valueOf(4), N.averageBigInteger(Arrays.asList(1, 2, 3), x -> BigInteger.valueOf(x * 2)));
        assertEquals(BigDecimal.ZERO, N.averageBigInteger(Collections.emptyList()));
    }

    @Test
    public void testAverageBigDecimal() {
        assertEquals(BigDecimal.valueOf(2), N.averageBigDecimal(Arrays.asList(BigDecimal.ONE, BigDecimal.valueOf(2), BigDecimal.valueOf(3))));
        assertEquals(BigDecimal.valueOf(4), N.averageBigDecimal(Arrays.asList(1, 2, 3), x -> BigDecimal.valueOf(x * 2)));
        assertEquals(BigDecimal.ZERO, N.averageBigDecimal(Collections.emptyList()));
    }

    // min tests
    @Test
    public void testMinTwoValues() {
        assertEquals('a', N.min('a', 'b'));
        assertEquals((byte) 1, N.min((byte) 1, (byte) 2));
        assertEquals((short) 1, N.min((short) 1, (short) 2));
        assertEquals(1, N.min(1, 2));
        assertEquals(1L, N.min(1L, 2L));
        assertEquals(1.0f, N.min(1.0f, 2.0f), 0.001f);
        assertEquals(1.0, N.min(1.0, 2.0), 0.001);
        assertEquals(1, N.min(1, 2));
        assertEquals(Integer.valueOf(1), N.min(1, 2, Comparator.naturalOrder()));
        assertEquals("a", N.min("a", "b"));
        assertEquals("a", N.min("a", "b", Comparator.naturalOrder()));
    }

    @Test
    public void testMinThreeValues() {
        assertEquals('a', N.min('a', 'b', 'c'));
        assertEquals((byte) 1, N.min((byte) 1, (byte) 2, (byte) 3));
        assertEquals((short) 1, N.min((short) 1, (short) 2, (short) 3));
        assertEquals(1, N.min(1, 2, 3));
        assertEquals(1L, N.min(1L, 2L, 3L));
        assertEquals(1.0f, N.min(1.0f, 2.0f, 3.0f), 0.001f);
        assertEquals(1.0, N.min(1.0, 2.0, 3.0), 0.001);
        assertEquals(1, N.min(1, 2, 3));
        assertEquals(Integer.valueOf(1), N.min(1, 2, 3, Comparator.naturalOrder()));
        assertEquals("a", N.min("a", "b", "c"));
        assertEquals("a", N.min("a", "b", "c", Comparator.naturalOrder()));
    }

    @Test
    public void testMinArray() {
        assertEquals('a', N.min('a', 'b', 'c', 'd'));
        assertEquals((byte) 1, N.min((byte) 1, (byte) 2, (byte) 3, (byte) 4));
        assertEquals((short) 1, N.min((short) 1, (short) 2, (short) 3, (short) 4));
        assertEquals(1, N.min(1, 2, 3, 4));
        assertEquals(1L, N.min(1L, 2L, 3L, 4L));
        assertEquals(1.0f, N.min(1.0f, 2.0f, 3.0f, 4.0f), 0.001f);
        assertEquals(1.0, N.min(1.0, 2.0, 3.0, 4.0), 0.001);
        assertEquals("a", N.min(new String[] { "a", "b", "c", "d" }));
        assertEquals("a", N.min(new String[] { "a", "b", "c", "d" }, Comparator.naturalOrder()));

        // Test with range
        assertEquals('b', N.min(new char[] { 'a', 'b', 'c', 'd' }, 1, 3));
        assertEquals((byte) 2, N.min(new byte[] { 1, 2, 3, 4 }, 1, 3));
        assertEquals((short) 2, N.min(new short[] { 1, 2, 3, 4 }, 1, 3));
        assertEquals(2, N.min(new int[] { 1, 2, 3, 4 }, 1, 3));
        assertEquals(2L, N.min(new long[] { 1L, 2L, 3L, 4L }, 1, 3));
        assertEquals(2.0f, N.min(new float[] { 1.0f, 2.0f, 3.0f, 4.0f }, 1, 3), 0.001f);
        assertEquals(2.0, N.min(new double[] { 1.0, 2.0, 3.0, 4.0 }, 1, 3), 0.001);
        assertEquals("b", N.min(new String[] { "a", "b", "c", "d" }, 1, 3));
        assertEquals("b", N.min(new String[] { "a", "b", "c", "d" }, 1, 3, Comparator.naturalOrder()));
    }

    @Test
    public void testMinArrayEmpty() {
        assertThrows(IllegalArgumentException.class, () -> N.min(new int[] {}));
    }

    @Test
    public void testMinArrayNull() {
        assertThrows(IllegalArgumentException.class, () -> N.min((int[]) null));
    }

    @Test
    public void testMinCollection() {
        assertEquals(Integer.valueOf(1), N.min(Arrays.asList(1, 2, 3, 4), 0, 4));
        assertEquals(Integer.valueOf(2), N.min(Arrays.asList(1, 2, 3, 4), 1, 3));
        assertEquals(Integer.valueOf(1), N.min(Arrays.asList(1, 2, 3, 4), 0, 4, Comparator.naturalOrder()));
        assertEquals(Integer.valueOf(2), N.min(Arrays.asList(1, 2, 3, 4), 1, 3, Comparator.naturalOrder()));
    }

    @Test
    public void testMinIterable() {
        assertEquals(Integer.valueOf(1), N.min(Arrays.asList(1, 2, 3, 4)));
        assertEquals(Integer.valueOf(1), N.min(Arrays.asList(1, 2, 3, 4), Comparator.naturalOrder()));

        // Test with nulls
        assertEquals(Integer.valueOf(1), N.min(Arrays.asList(null, 1, 2, 3)));
        assertNull(N.min(Arrays.asList(null, null, null)));
    }

    @Test
    public void testMinIterator() {
        assertEquals(Integer.valueOf(1), N.min(Arrays.asList(1, 2, 3, 4).iterator()));
        assertEquals(Integer.valueOf(1), N.min(Arrays.asList(1, 2, 3, 4).iterator(), Comparator.naturalOrder()));
    }

    @Test
    public void testMinIteratorEmpty() {
        assertThrows(IllegalArgumentException.class, () -> N.min(Collections.<Integer> emptyList().iterator()));
    }

    @Test
    public void testMinBy() {
        Person[] people = { new Person("John", 25), new Person("Jane", 22), new Person("Bob", 30) };
        assertEquals("Jane", N.minBy(people, p -> p.age).name);

        List<Person> peopleList = Arrays.asList(people);
        assertEquals("Jane", N.minBy(peopleList, p -> p.age).name);

        assertEquals("Jane", N.minBy(peopleList.iterator(), p -> p.age).name);
    }

    @Test
    public void testMinAll() {
        assertEquals(Arrays.asList(1, 1), N.minAll(new Integer[] { 1, 2, 3, 1, 4 }));
        assertEquals(Arrays.asList(1, 1), N.minAll(new Integer[] { 1, 2, 3, 1, 4 }, Comparator.naturalOrder()));
        assertEquals(Arrays.asList(1, 1), N.minAll(Arrays.asList(1, 2, 3, 1, 4)));
        assertEquals(Arrays.asList(1, 1), N.minAll(Arrays.asList(1, 2, 3, 1, 4), Comparator.naturalOrder()));
        assertEquals(Arrays.asList(1, 1), N.minAll(Arrays.asList(1, 2, 3, 1, 4).iterator()));
        assertEquals(Arrays.asList(1, 1), N.minAll(Arrays.asList(1, 2, 3, 1, 4).iterator(), Comparator.naturalOrder()));

        // Empty cases
        assertEquals(Collections.emptyList(), N.minAll(new Integer[] {}));
        assertEquals(Collections.emptyList(), N.minAll(Collections.emptyList()));
    }

    @Test
    public void testMinOrDefaultIfEmpty() {
        assertEquals(Integer.valueOf(1), N.minOrDefaultIfEmpty(new Integer[] { 1, 2, 3 }, x -> x, 99));
        assertEquals(Integer.valueOf(99), N.minOrDefaultIfEmpty(new Integer[] {}, x -> x, 99));
        assertEquals(Integer.valueOf(1), N.minOrDefaultIfEmpty(Arrays.asList(1, 2, 3), x -> x, 99));
        assertEquals(Integer.valueOf(99), N.minOrDefaultIfEmpty(Collections.<Integer> emptyList(), x -> x, 99));
        assertEquals(Integer.valueOf(1), N.minOrDefaultIfEmpty(Arrays.asList(1, 2, 3).iterator(), x -> x, 99));
        assertEquals(Integer.valueOf(99), N.minOrDefaultIfEmpty(Collections.<Integer> emptyIterator(), x -> x, 99));
    }

    @Test
    public void testMinIntOrDefaultIfEmpty() {
        assertEquals(1, N.minIntOrDefaultIfEmpty(new Integer[] { 1, 2, 3 }, x -> x, 99));
        assertEquals(99, N.minIntOrDefaultIfEmpty(new Integer[] {}, x -> x, 99));
        assertEquals(1, N.minIntOrDefaultIfEmpty(Arrays.asList(1, 2, 3), x -> x, 99));
        assertEquals(99, N.minIntOrDefaultIfEmpty(Collections.<Integer> emptyList(), x -> x, 99));
        assertEquals(1, N.minIntOrDefaultIfEmpty(Arrays.asList(1, 2, 3).iterator(), x -> x, 99));
        assertEquals(99, N.minIntOrDefaultIfEmpty(Collections.<Integer> emptyIterator(), x -> x, 99));
    }

    @Test
    public void testMinLongOrDefaultIfEmpty() {
        assertEquals(1L, N.minLongOrDefaultIfEmpty(new Long[] { 1L, 2L, 3L }, x -> x, 99L));
        assertEquals(99L, N.minLongOrDefaultIfEmpty(new Long[] {}, x -> x, 99L));
        assertEquals(1L, N.minLongOrDefaultIfEmpty(Arrays.asList(1L, 2L, 3L), x -> x, 99L));
        assertEquals(99L, N.minLongOrDefaultIfEmpty(Collections.<Long> emptyList(), x -> x, 99L));
        assertEquals(1L, N.minLongOrDefaultIfEmpty(Arrays.asList(1L, 2L, 3L).iterator(), x -> x, 99L));
        assertEquals(99L, N.minLongOrDefaultIfEmpty(Collections.<Long> emptyIterator(), x -> x, 99L));
    }

    @Test
    public void testMinDoubleOrDefaultIfEmpty() {
        assertEquals(1.0, N.minDoubleOrDefaultIfEmpty(new Double[] { 1.0, 2.0, 3.0 }, x -> x, 99.0), 0.001);
        assertEquals(99.0, N.minDoubleOrDefaultIfEmpty(new Double[] {}, x -> x, 99.0), 0.001);
        assertEquals(1.0, N.minDoubleOrDefaultIfEmpty(Arrays.asList(1.0, 2.0, 3.0), x -> x, 99.0), 0.001);
        assertEquals(99.0, N.minDoubleOrDefaultIfEmpty(Collections.<Double> emptyList(), x -> x, 99.0), 0.001);
        assertEquals(1.0, N.minDoubleOrDefaultIfEmpty(Arrays.asList(1.0, 2.0, 3.0).iterator(), x -> x, 99.0), 0.001);
        assertEquals(99.0, N.minDoubleOrDefaultIfEmpty(Collections.<Double> emptyIterator(), x -> x, 99.0), 0.001);
    }

    @Test
    public void testMinMax() {
        Pair<Integer, Integer> result = N.minMax(new Integer[] { 1, 2, 3, 4, 5 });
        assertEquals(Integer.valueOf(1), result.left());
        assertEquals(Integer.valueOf(5), result.right());

        result = N.minMax(new Integer[] { 1, 2, 3, 4, 5 }, Comparator.naturalOrder());
        assertEquals(Integer.valueOf(1), result.left());
        assertEquals(Integer.valueOf(5), result.right());

        result = N.minMax(Arrays.asList(1, 2, 3, 4, 5));
        assertEquals(Integer.valueOf(1), result.left());
        assertEquals(Integer.valueOf(5), result.right());

        result = N.minMax(Arrays.asList(1, 2, 3, 4, 5), Comparator.naturalOrder());
        assertEquals(Integer.valueOf(1), result.left());
        assertEquals(Integer.valueOf(5), result.right());

        result = N.minMax(Arrays.asList(1, 2, 3, 4, 5).iterator());
        assertEquals(Integer.valueOf(1), result.left());
        assertEquals(Integer.valueOf(5), result.right());

        result = N.minMax(Arrays.asList(1, 2, 3, 4, 5).iterator(), Comparator.naturalOrder());
        assertEquals(Integer.valueOf(1), result.left());
        assertEquals(Integer.valueOf(5), result.right());

        // Single element
        result = N.minMax(new Integer[] { 1 });
        assertEquals(Integer.valueOf(1), result.left());
        assertEquals(Integer.valueOf(1), result.right());
    }

    // max tests
    @Test
    public void testMaxTwoValues() {
        assertEquals('b', N.max('a', 'b'));
        assertEquals((byte) 2, N.max((byte) 1, (byte) 2));
        assertEquals((short) 2, N.max((short) 1, (short) 2));
        assertEquals(2, N.max(1, 2));
        assertEquals(2L, N.max(1L, 2L));
        assertEquals(2.0f, N.max(1.0f, 2.0f), 0.001f);
        assertEquals(2.0, N.max(1.0, 2.0), 0.001);
        assertEquals(2, N.max(1, 2));
        assertEquals(Integer.valueOf(2), N.max(1, 2, Comparator.naturalOrder()));
        assertEquals("b", N.max("a", "b"));
        assertEquals("b", N.max("a", "b", Comparator.naturalOrder()));
    }

    @Test
    public void testMaxThreeValues() {
        assertEquals('c', N.max('a', 'b', 'c'));
        assertEquals((byte) 3, N.max((byte) 1, (byte) 2, (byte) 3));
        assertEquals((short) 3, N.max((short) 1, (short) 2, (short) 3));
        assertEquals(3, N.max(1, 2, 3));
        assertEquals(3L, N.max(1L, 2L, 3L));
        assertEquals(3.0f, N.max(1.0f, 2.0f, 3.0f), 0.001f);
        assertEquals(3.0, N.max(1.0, 2.0, 3.0), 0.001);
        assertEquals(3, N.max(1, 2, 3));
        assertEquals(Integer.valueOf(3), N.max(1, 2, 3, Comparator.naturalOrder()));
        assertEquals("c", N.max("a", "b", "c"));
        assertEquals("c", N.max("a", "b", "c", Comparator.naturalOrder()));
    }

    @Test
    public void testMaxArray() {
        assertEquals('d', N.max('a', 'b', 'c', 'd'));
        assertEquals((byte) 4, N.max((byte) 1, (byte) 2, (byte) 3, (byte) 4));
        assertEquals((short) 4, N.max((short) 1, (short) 2, (short) 3, (short) 4));
        assertEquals(4, N.max(1, 2, 3, 4));
        assertEquals(4L, N.max(1L, 2L, 3L, 4L));
        assertEquals(4.0f, N.max(1.0f, 2.0f, 3.0f, 4.0f), 0.001f);
        assertEquals(4.0, N.max(1.0, 2.0, 3.0, 4.0), 0.001);
        assertEquals("d", N.max(new String[] { "a", "b", "c", "d" }));
        assertEquals("d", N.max(new String[] { "a", "b", "c", "d" }, Comparator.naturalOrder()));

        // Test with range
        assertEquals('c', N.max(new char[] { 'a', 'b', 'c', 'd' }, 1, 3));
        assertEquals((byte) 3, N.max(new byte[] { 1, 2, 3, 4 }, 1, 3));
        assertEquals((short) 3, N.max(new short[] { 1, 2, 3, 4 }, 1, 3));
        assertEquals(3, N.max(new int[] { 1, 2, 3, 4 }, 1, 3));
        assertEquals(3L, N.max(new long[] { 1L, 2L, 3L, 4L }, 1, 3));
        assertEquals(3.0f, N.max(new float[] { 1.0f, 2.0f, 3.0f, 4.0f }, 1, 3), 0.001f);
        assertEquals(3.0, N.max(new double[] { 1.0, 2.0, 3.0, 4.0 }, 1, 3), 0.001);
        assertEquals("c", N.max(new String[] { "a", "b", "c", "d" }, 1, 3));
        assertEquals("c", N.max(new String[] { "a", "b", "c", "d" }, 1, 3, Comparator.naturalOrder()));
    }

    @Test
    public void testMaxArrayEmpty() {
        assertThrows(IllegalArgumentException.class, () -> N.max(new int[] {}));
    }

    @Test
    public void testMaxArrayNull() {
        assertThrows(IllegalArgumentException.class, () -> N.max((int[]) null));
    }

    @Test
    public void testMaxCollection() {
        assertEquals(Integer.valueOf(4), N.max(Arrays.asList(1, 2, 3, 4), 0, 4));
        assertEquals(Integer.valueOf(3), N.max(Arrays.asList(1, 2, 3, 4), 1, 3));
        assertEquals(Integer.valueOf(4), N.max(Arrays.asList(1, 2, 3, 4), 0, 4, Comparator.naturalOrder()));
        assertEquals(Integer.valueOf(3), N.max(Arrays.asList(1, 2, 3, 4), 1, 3, Comparator.naturalOrder()));
    }

    @Test
    public void testMaxIterable() {
        assertEquals(Integer.valueOf(4), N.max(Arrays.asList(1, 2, 3, 4)));
        assertEquals(Integer.valueOf(4), N.max(Arrays.asList(1, 2, 3, 4), Comparator.naturalOrder()));

        // Test with nulls
        assertEquals(Integer.valueOf(3), N.max(Arrays.asList(1, 2, 3, null)));
        assertNull(N.max(Arrays.asList(null, null, null)));
    }

    @Test
    public void testMaxIterator() {
        assertEquals(Integer.valueOf(4), N.max(Arrays.asList(1, 2, 3, 4).iterator()));
        assertEquals(Integer.valueOf(4), N.max(Arrays.asList(1, 2, 3, 4).iterator(), Comparator.naturalOrder()));
    }

    @Test
    public void testMaxIteratorEmpty() {
        assertThrows(IllegalArgumentException.class, () -> N.max(Collections.<Integer> emptyList().iterator()));
    }

    @Test
    public void testMaxBy() {
        Person[] people = { new Person("John", 25), new Person("Jane", 22), new Person("Bob", 30) };
        assertEquals("Bob", N.maxBy(people, p -> p.age).name);

        List<Person> peopleList = Arrays.asList(people);
        assertEquals("Bob", N.maxBy(peopleList, p -> p.age).name);

        assertEquals("Bob", N.maxBy(peopleList.iterator(), p -> p.age).name);
    }

    @Test
    public void testMaxAll() {
        assertEquals(Arrays.asList(4, 4), N.maxAll(new Integer[] { 1, 2, 4, 3, 4 }));
        assertEquals(Arrays.asList(4, 4), N.maxAll(new Integer[] { 1, 2, 4, 3, 4 }, Comparator.naturalOrder()));
        assertEquals(Arrays.asList(4, 4), N.maxAll(Arrays.asList(1, 2, 4, 3, 4)));
        assertEquals(Arrays.asList(4, 4), N.maxAll(Arrays.asList(1, 2, 4, 3, 4), Comparator.naturalOrder()));
        assertEquals(Arrays.asList(4, 4), N.maxAll(Arrays.asList(1, 2, 4, 3, 4).iterator()));
        assertEquals(Arrays.asList(4, 4), N.maxAll(Arrays.asList(1, 2, 4, 3, 4).iterator(), Comparator.naturalOrder()));

        // Empty cases
        assertEquals(Collections.emptyList(), N.maxAll(new Integer[] {}));
        assertEquals(Collections.emptyList(), N.maxAll(Collections.emptyList()));
    }

    @Test
    public void testMaxOrDefaultIfEmpty() {
        assertEquals(Integer.valueOf(3), N.maxOrDefaultIfEmpty(new Integer[] { 1, 2, 3 }, x -> x, 99));
        assertEquals(Integer.valueOf(99), N.maxOrDefaultIfEmpty(new Integer[] {}, x -> x, 99));
        assertEquals(Integer.valueOf(3), N.maxOrDefaultIfEmpty(Arrays.asList(1, 2, 3), x -> x, 99));
        assertEquals(Integer.valueOf(99), N.maxOrDefaultIfEmpty(Collections.<Integer> emptyList(), x -> x, 99));
        assertEquals(Integer.valueOf(3), N.maxOrDefaultIfEmpty(Arrays.asList(1, 2, 3).iterator(), x -> x, 99));
        assertEquals(Integer.valueOf(99), N.maxOrDefaultIfEmpty(Collections.<Integer> emptyIterator(), x -> x, 99));
    }

    @Test
    public void testMaxIntOrDefaultIfEmpty() {
        assertEquals(3, N.maxIntOrDefaultIfEmpty(new Integer[] { 1, 2, 3 }, x -> x, 99));
        assertEquals(99, N.maxIntOrDefaultIfEmpty(new Integer[] {}, x -> x, 99));
        assertEquals(3, N.maxIntOrDefaultIfEmpty(Arrays.asList(1, 2, 3), x -> x, 99));
        assertEquals(99, N.maxIntOrDefaultIfEmpty(Collections.<Integer> emptyList(), x -> x, 99));
        assertEquals(3, N.maxIntOrDefaultIfEmpty(Arrays.asList(1, 2, 3).iterator(), x -> x, 99));
        assertEquals(99, N.maxIntOrDefaultIfEmpty(Collections.<Integer> emptyIterator(), x -> x, 99));
    }

    @Test
    public void testMaxLongOrDefaultIfEmpty() {
        assertEquals(3L, N.maxLongOrDefaultIfEmpty(new Long[] { 1L, 2L, 3L }, x -> x, 99L));
        assertEquals(99L, N.maxLongOrDefaultIfEmpty(new Long[] {}, x -> x, 99L));
        assertEquals(3L, N.maxLongOrDefaultIfEmpty(Arrays.asList(1L, 2L, 3L), x -> x, 99L));
        assertEquals(99L, N.maxLongOrDefaultIfEmpty(Collections.<Long> emptyList(), x -> x, 99L));
        assertEquals(3L, N.maxLongOrDefaultIfEmpty(Arrays.asList(1L, 2L, 3L).iterator(), x -> x, 99L));
        assertEquals(99L, N.maxLongOrDefaultIfEmpty(Collections.<Long> emptyIterator(), x -> x, 99L));
    }

    @Test
    public void testMaxDoubleOrDefaultIfEmpty() {
        assertEquals(3.0, N.maxDoubleOrDefaultIfEmpty(new Double[] { 1.0, 2.0, 3.0 }, x -> x, 99.0), 0.001);
        assertEquals(99.0, N.maxDoubleOrDefaultIfEmpty(new Double[] {}, x -> x, 99.0), 0.001);
        assertEquals(3.0, N.maxDoubleOrDefaultIfEmpty(Arrays.asList(1.0, 2.0, 3.0), x -> x, 99.0), 0.001);
        assertEquals(99.0, N.maxDoubleOrDefaultIfEmpty(Collections.<Double> emptyList(), x -> x, 99.0), 0.001);
        assertEquals(3.0, N.maxDoubleOrDefaultIfEmpty(Arrays.asList(1.0, 2.0, 3.0).iterator(), x -> x, 99.0), 0.001);
        assertEquals(99.0, N.maxDoubleOrDefaultIfEmpty(Collections.<Double> emptyIterator(), x -> x, 99.0), 0.001);
    }

    // median tests
    @Test
    public void testMedianThreeValues() {
        assertEquals('b', N.median('a', 'b', 'c'));
        assertEquals('b', N.median('c', 'b', 'a'));
        assertEquals((byte) 2, N.median((byte) 1, (byte) 2, (byte) 3));
        assertEquals((short) 2, N.median((short) 1, (short) 2, (short) 3));
        assertEquals(2, N.median(1, 2, 3));
        assertEquals(2L, N.median(1L, 2L, 3L));
        assertEquals(2.0f, N.median(1.0f, 2.0f, 3.0f), 0.001f);
        assertEquals(2.0, N.median(1.0, 2.0, 3.0), 0.001);
        assertEquals(2, N.median(1, 2, 3));
        assertEquals((Integer) 2, N.median(1, 2, 3, Comparator.naturalOrder()));
        assertEquals("b", N.median("a", "b", "c"));
        assertEquals("b", N.median("a", "b", "c", Comparator.naturalOrder()));
    }

    @Test
    public void testMedianArray() {
        assertEquals('b', N.median('a', 'b', 'c', 'd'));
        assertEquals((byte) 2, N.median((byte) 1, (byte) 2, (byte) 3, (byte) 4));
        assertEquals((short) 2, N.median((short) 1, (short) 2, (short) 3, (short) 4));
        assertEquals(2, N.median(1, 2, 3, 4));
        assertEquals(2L, N.median(1L, 2L, 3L, 4L));
        assertEquals(2.0f, N.median(1.0f, 2.0f, 3.0f, 4.0f), 0.001f);
        assertEquals(2.0, N.median(1.0, 2.0, 3.0, 4.0), 0.001);
        assertEquals("b", N.median(new String[] { "a", "b", "c", "d" }));
        assertEquals("b", N.median(new String[] { "a", "b", "c", "d" }, Comparator.naturalOrder()));

        // Single element
        assertEquals(1, N.median(1));

        // Two elements
        assertEquals(1, N.median(1, 2));

        // Test with range
        assertEquals('b', N.median(new char[] { 'a', 'b', 'c', 'd' }, 0, 3));
        assertEquals((byte) 2, N.median(new byte[] { 1, 2, 3, 4 }, 0, 3));
        assertEquals((short) 2, N.median(new short[] { 1, 2, 3, 4 }, 0, 3));
        assertEquals(2, N.median(new int[] { 1, 2, 3, 4 }, 0, 3));
        assertEquals(2L, N.median(new long[] { 1L, 2L, 3L, 4L }, 0, 3));
        assertEquals(2.0f, N.median(new float[] { 1.0f, 2.0f, 3.0f, 4.0f }, 0, 3), 0.001f);
        assertEquals(2.0, N.median(new double[] { 1.0, 2.0, 3.0, 4.0 }, 0, 3), 0.001);
        assertEquals("b", N.median(new String[] { "a", "b", "c", "d" }, 0, 3));
        assertEquals("b", N.median(new String[] { "a", "b", "c", "d" }, 0, 3, Comparator.naturalOrder()));
    }

    @Test
    public void testMedianArrayEmpty() {
        assertThrows(IllegalArgumentException.class, () -> N.median(new int[] {}));
    }

    @Test
    public void testMedianArrayNull() {
        assertThrows(IllegalArgumentException.class, () -> N.median((int[]) null));
    }

    @Test
    public void testMedianCollection() {
        assertEquals(Integer.valueOf(2), N.median(Arrays.asList(1, 2, 3, 4)));
        assertEquals(Integer.valueOf(2), N.median(Arrays.asList(1, 2, 3, 4), 0, 4));
        assertEquals(Integer.valueOf(2), N.median(Arrays.asList(1, 2, 3, 4), Comparator.naturalOrder()));
        assertEquals(Integer.valueOf(2), N.median(Arrays.asList(1, 2, 3, 4), 0, 4, Comparator.naturalOrder()));
    }

    // kthLargest tests
    @Test
    public void testKthLargest() {
        assertEquals('d', N.kthLargest(new char[] { 'a', 'b', 'c', 'd', 'e' }, 2));
        assertEquals((byte) 4, N.kthLargest(new byte[] { 1, 2, 3, 4, 5 }, 2));
        assertEquals((short) 4, N.kthLargest(new short[] { 1, 2, 3, 4, 5 }, 2));
        assertEquals(4, N.kthLargest(new int[] { 1, 2, 3, 4, 5 }, 2));
        assertEquals(4L, N.kthLargest(new long[] { 1L, 2L, 3L, 4L, 5L }, 2));
        assertEquals(4.0f, N.kthLargest(new float[] { 1.0f, 2.0f, 3.0f, 4.0f, 5.0f }, 2), 0.001f);
        assertEquals(4.0, N.kthLargest(new double[] { 1.0, 2.0, 3.0, 4.0, 5.0 }, 2), 0.001);
        assertEquals("d", N.kthLargest(new String[] { "a", "b", "c", "d", "e" }, 2));
        assertEquals("d", N.kthLargest(new String[] { "a", "b", "c", "d", "e" }, 2, Comparator.naturalOrder()));

        // Test k = 1 (max)
        assertEquals(5, N.kthLargest(new int[] { 1, 2, 3, 4, 5 }, 1));

        // Test k = length (min)
        assertEquals(1, N.kthLargest(new int[] { 1, 2, 3, 4, 5 }, 5));

        // Test with range
        assertEquals('c', N.kthLargest(new char[] { 'a', 'b', 'c', 'd', 'e' }, 1, 4, 2));
        assertEquals((byte) 3, N.kthLargest(new byte[] { 1, 2, 3, 4, 5 }, 1, 4, 2));
        assertEquals((short) 3, N.kthLargest(new short[] { 1, 2, 3, 4, 5 }, 1, 4, 2));
        assertEquals(3, N.kthLargest(new int[] { 1, 2, 3, 4, 5 }, 1, 4, 2));
        assertEquals(3L, N.kthLargest(new long[] { 1L, 2L, 3L, 4L, 5L }, 1, 4, 2));
        assertEquals(3.0f, N.kthLargest(new float[] { 1.0f, 2.0f, 3.0f, 4.0f, 5.0f }, 1, 4, 2), 0.001f);
        assertEquals(3.0, N.kthLargest(new double[] { 1.0, 2.0, 3.0, 4.0, 5.0 }, 1, 4, 2), 0.001);
        assertEquals("c", N.kthLargest(new String[] { "a", "b", "c", "d", "e" }, 1, 4, 2));
        assertEquals("c", N.kthLargest(new String[] { "a", "b", "c", "d", "e" }, 1, 4, 2, Comparator.naturalOrder()));
    }

    @Test
    public void testKthLargestArrayEmpty() {
        assertThrows(IllegalArgumentException.class, () -> N.kthLargest(new int[] {}, 1));
    }

    @Test
    public void testKthLargestInvalidK() {
        assertThrows(IllegalArgumentException.class, () -> N.kthLargest(new int[] { 1, 2, 3 }, 4));
    }

    @Test
    public void testKthLargestCollection() {
        assertEquals(Integer.valueOf(4), N.kthLargest(Arrays.asList(1, 2, 3, 4, 5), 2));
        assertEquals(Integer.valueOf(4), N.kthLargest(Arrays.asList(1, 2, 3, 4, 5), 0, 5, 2));
        assertEquals(Integer.valueOf(4), N.kthLargest(Arrays.asList(1, 2, 3, 4, 5), 2, Comparator.naturalOrder()));
        assertEquals(Integer.valueOf(4), N.kthLargest(Arrays.asList(1, 2, 3, 4, 5), 0, 5, 2, Comparator.naturalOrder()));
    }

    // top tests
    @Test
    public void testTopShort() {
        assertArrayEquals(new short[] { 4, 5 }, N.top(new short[] { 1, 2, 3, 4, 5 }, 2));
        assertArrayEquals(new short[] { 2, 1 }, N.top(new short[] { 1, 2, 3, 4, 5 }, 2, Comparator.reverseOrder()));
        assertArrayEquals(new short[] { 3, 4 }, N.top(new short[] { 1, 2, 3, 4, 5 }, 1, 4, 2));
        assertArrayEquals(new short[] { 3, 2 }, N.top(new short[] { 1, 2, 3, 4, 5 }, 1, 4, 2, Comparator.reverseOrder()));

        // n >= length
        assertArrayEquals(new short[] { 1, 2, 3 }, N.top(new short[] { 1, 2, 3 }, 5));

        // n = 0
        assertArrayEquals(new short[] {}, N.top(new short[] { 1, 2, 3 }, 0));

        // empty array
        assertArrayEquals(new short[] {}, N.top((short[]) null, 2));
    }

    @Test
    public void testTopInt() {
        assertArrayEquals(new int[] { 4, 5 }, N.top(new int[] { 1, 2, 3, 4, 5 }, 2));
        assertArrayEquals(new int[] { 2, 1 }, N.top(new int[] { 1, 2, 3, 4, 5 }, 2, Comparator.reverseOrder()));
        assertArrayEquals(new int[] { 3, 4 }, N.top(new int[] { 1, 2, 3, 4, 5 }, 1, 4, 2));
        assertArrayEquals(new int[] { 3, 2 }, N.top(new int[] { 1, 2, 3, 4, 5 }, 1, 4, 2, Comparator.reverseOrder()));
    }

    @Test
    public void testTopLong() {
        assertArrayEquals(new long[] { 4L, 5L }, N.top(new long[] { 1L, 2L, 3L, 4L, 5L }, 2));
        assertArrayEquals(new long[] { 2L, 1L }, N.top(new long[] { 1L, 2L, 3L, 4L, 5L }, 2, Comparator.reverseOrder()));
        assertArrayEquals(new long[] { 3L, 4L }, N.top(new long[] { 1L, 2L, 3L, 4L, 5L }, 1, 4, 2));
        assertArrayEquals(new long[] { 3L, 2L }, N.top(new long[] { 1L, 2L, 3L, 4L, 5L }, 1, 4, 2, Comparator.reverseOrder()));
    }

    @Test
    public void testTopFloat() {
        assertArrayEquals(new float[] { 4.0f, 5.0f }, N.top(new float[] { 1.0f, 2.0f, 3.0f, 4.0f, 5.0f }, 2), 0.001f);
        assertArrayEquals(new float[] { 2.0f, 1.0f }, N.top(new float[] { 1.0f, 2.0f, 3.0f, 4.0f, 5.0f }, 2, Comparator.reverseOrder()), 0.001f);
        assertArrayEquals(new float[] { 3.0f, 4.0f }, N.top(new float[] { 1.0f, 2.0f, 3.0f, 4.0f, 5.0f }, 1, 4, 2), 0.001f);
        assertArrayEquals(new float[] { 3.0f, 2.0f }, N.top(new float[] { 1.0f, 2.0f, 3.0f, 4.0f, 5.0f }, 1, 4, 2, Comparator.reverseOrder()), 0.001f);
    }

    @Test
    public void testTopDouble() {
        assertArrayEquals(new double[] { 4.0, 5.0 }, N.top(new double[] { 1.0, 2.0, 3.0, 4.0, 5.0 }, 2), 0.001);
        assertArrayEquals(new double[] { 2.0, 1.0 }, N.top(new double[] { 1.0, 2.0, 3.0, 4.0, 5.0 }, 2, Comparator.reverseOrder()), 0.001);
        assertArrayEquals(new double[] { 3.0, 4.0 }, N.top(new double[] { 1.0, 2.0, 3.0, 4.0, 5.0 }, 1, 4, 2), 0.001);
        assertArrayEquals(new double[] { 3.0, 2.0 }, N.top(new double[] { 1.0, 2.0, 3.0, 4.0, 5.0 }, 1, 4, 2, Comparator.reverseOrder()), 0.001);
    }

    @Test
    public void testTopGeneric() {
        assertEquals(Arrays.asList("d", "e"), N.top(new String[] { "a", "b", "c", "d", "e" }, 2));
        assertEquals(Arrays.asList("b", "a"), N.top(new String[] { "a", "b", "c", "d", "e" }, 2, Comparator.reverseOrder()));
        assertEquals(Arrays.asList("c", "d"), N.top(new String[] { "a", "b", "c", "d", "e" }, 1, 4, 2));
        assertEquals(Arrays.asList("c", "b"), N.top(new String[] { "a", "b", "c", "d", "e" }, 1, 4, 2, Comparator.reverseOrder()));

        // keepEncounterOrder
        assertEquals(Arrays.asList("e", "d"), N.top(new String[] { "a", "e", "c", "d", "b" }, 2, true));
        assertEquals(Arrays.asList("e", "d"), N.top(new String[] { "a", "e", "c", "d", "b" }, 2, Comparator.naturalOrder(), true));
        assertEquals(Arrays.asList("e", "d"), N.top(new String[] { "a", "e", "c", "d", "b" }, 1, 4, 2, true));
        assertEquals(Arrays.asList("e", "d"), N.top(new String[] { "a", "e", "c", "d", "b" }, 1, 4, 2, Comparator.naturalOrder(), true));
    }

    @Test
    public void testTopCollection() {
        assertEquals(Arrays.asList(4, 5), N.top(Arrays.asList(1, 2, 3, 4, 5), 2));
        assertEquals(Arrays.asList(2, 1), N.top(Arrays.asList(1, 2, 3, 4, 5), 2, Comparator.reverseOrder()));
        assertEquals(Arrays.asList(3, 4), N.top(Arrays.asList(1, 2, 3, 4, 5), 1, 4, 2));
        assertEquals(Arrays.asList(3, 2), N.top(Arrays.asList(1, 2, 3, 4, 5), 1, 4, 2, Comparator.reverseOrder()));

        // keepEncounterOrder
        assertEquals(Arrays.asList(5, 4), N.top(Arrays.asList(1, 5, 3, 4, 2), 2, true));
        assertEquals(Arrays.asList(5, 4), N.top(Arrays.asList(1, 5, 3, 4, 2), 2, Comparator.naturalOrder(), true));
        assertEquals(Arrays.asList(5, 4), N.top(Arrays.asList(1, 5, 3, 4, 2), 1, 4, 2, true));
        assertEquals(Arrays.asList(5, 4), N.top(Arrays.asList(1, 5, 3, 4, 2), 1, 4, 2, Comparator.naturalOrder(), true));
    }

    @Test
    public void testTopNegativeN() {
        assertThrows(IllegalArgumentException.class, () -> N.top(new int[] { 1, 2, 3 }, -1));
    }

    // percentiles tests
    @Test
    public void testPercentilesChar() {
        char[] sorted = { 'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j' };
        Map<Percentage, Character> percentiles = N.percentiles(sorted);
        assertNotNull(percentiles);
        assertEquals(Percentage.values().length, percentiles.size());
        assertTrue(percentiles.containsKey(Percentage._0_1));
        assertTrue(percentiles.containsKey(Percentage._1));
        assertTrue(percentiles.containsKey(Percentage._50));
        assertTrue(percentiles.containsKey(Percentage._99));
    }

    @Test
    public void testPercentilesByte() {
        byte[] sorted = { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10 };
        Map<Percentage, Byte> percentiles = N.percentiles(sorted);
        assertNotNull(percentiles);
        assertEquals(Percentage.values().length, percentiles.size());
    }

    @Test
    public void testPercentilesShort() {
        short[] sorted = { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10 };
        Map<Percentage, Short> percentiles = N.percentiles(sorted);
        assertNotNull(percentiles);
        assertEquals(Percentage.values().length, percentiles.size());
    }

    @Test
    public void testPercentilesInt() {
        int[] sorted = new int[100];
        for (int i = 0; i < 100; i++) {
            sorted[i] = i + 1;
        }
        Map<Percentage, Integer> percentiles = N.percentiles(sorted);
        assertNotNull(percentiles);
        assertEquals(Percentage.values().length, percentiles.size());
        assertEquals(Integer.valueOf(2), percentiles.get(Percentage._1));
        assertEquals(Integer.valueOf(51), percentiles.get(Percentage._50));
        assertEquals(Integer.valueOf(100), percentiles.get(Percentage._99));
    }

    @Test
    public void testPercentilesLong() {
        long[] sorted = { 1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L, 10L };
        Map<Percentage, Long> percentiles = N.percentiles(sorted);
        assertNotNull(percentiles);
        assertEquals(Percentage.values().length, percentiles.size());
    }

    @Test
    public void testPercentilesFloat() {
        float[] sorted = { 1.0f, 2.0f, 3.0f, 4.0f, 5.0f, 6.0f, 7.0f, 8.0f, 9.0f, 10.0f };
        Map<Percentage, Float> percentiles = N.percentiles(sorted);
        assertNotNull(percentiles);
        assertEquals(Percentage.values().length, percentiles.size());
    }

    @Test
    public void testPercentilesDouble() {
        double[] sorted = { 1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0, 9.0, 10.0 };
        Map<Percentage, Double> percentiles = N.percentiles(sorted);
        assertNotNull(percentiles);
        assertEquals(Percentage.values().length, percentiles.size());
    }

    @Test
    public void testPercentilesGeneric() {
        String[] sorted = { "a", "b", "c", "d", "e", "f", "g", "h", "i", "j" };
        Map<Percentage, String> percentiles = N.percentiles(sorted);
        assertNotNull(percentiles);
        assertEquals(Percentage.values().length, percentiles.size());
    }

    @Test
    public void testPercentilesList() {
        List<Integer> sorted = Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
        Map<Percentage, Integer> percentiles = N.percentiles(sorted);
        assertNotNull(percentiles);
        assertEquals(Percentage.values().length, percentiles.size());
    }

    @Test
    public void testPercentilesEmptyArray() {
        assertThrows(IllegalArgumentException.class, () -> N.percentiles(new int[] {}));
    }

    @Test
    public void testPercentilesNullArray() {
        assertThrows(IllegalArgumentException.class, () -> N.percentiles((int[]) null));
    }

    @Test
    public void testPercentilesEmptyList() {
        assertThrows(IllegalArgumentException.class, () -> N.percentiles(Collections.emptyList()));
    }

    // Helper class for testing
    public static class Person implements Comparable<Person> {
        String name;
        int age;

        Person(String name, int age) {
            this.name = name;
            this.age = age;
        }

        @Override
        public int compareTo(Person o) {
            return Integer.compare(this.age, o.age);
        }
    }

    // Additional tests for methods from CommonUtil that might be inherited

    // Test for len() method (if available through inheritance)
    @Test
    public void testLen() {
        // Test with arrays
        assertEquals(5, N.len(new int[] { 1, 2, 3, 4, 5 }));
        assertEquals(0, N.len(new int[] {}));
        assertEquals(0, N.len((int[]) null));

        assertEquals(3, N.len(new String[] { "a", "b", "c" }));
        assertEquals(0, N.len(new String[] {}));
        assertEquals(0, N.len((String[]) null));
    }

    // Test for size() method (if available through inheritance)
    @Test
    public void testSize() {
        // Test with collections
        assertEquals(5, N.size(Arrays.asList(1, 2, 3, 4, 5)));
        assertEquals(0, N.size(Collections.emptyList()));
        assertEquals(0, N.size((Collection<?>) null));

        // Test with maps
        Map<String, Integer> map = new HashMap<>();
        map.put("a", 1);
        map.put("b", 2);
        assertEquals(2, N.size(map));
        assertEquals(0, N.size(Collections.emptyMap()));
        assertEquals(0, N.size((Map<?, ?>) null));
    }

    // Test for isEmpty() method
    @Test
    public void testIsEmpty() {
        // Arrays
        assertTrue(N.isEmpty(new int[] {}));
        assertTrue(N.isEmpty((int[]) null));
        assertFalse(N.isEmpty(new int[] { 1 }));

        // Collections
        assertTrue(N.isEmpty(Collections.emptyList()));
        assertTrue(N.isEmpty((Collection<?>) null));
        assertFalse(N.isEmpty(Arrays.asList(1)));

        // Maps
        assertTrue(N.isEmpty(Collections.emptyMap()));
        assertTrue(N.isEmpty((Map<?, ?>) null));
        Map<String, Integer> map = new HashMap<>();
        map.put("a", 1);
        assertFalse(N.isEmpty(map));

        // Strings
        assertTrue(N.isEmpty(""));
        assertTrue(N.isEmpty((String) null));
        assertFalse(N.isEmpty("a"));
    }

    // Test for notEmpty() method
    @Test
    public void testNotEmpty() {
        // Arrays
        assertFalse(N.notEmpty(new int[] {}));
        assertFalse(N.notEmpty((int[]) null));
        assertTrue(N.notEmpty(new int[] { 1 }));

        // Collections
        assertFalse(N.notEmpty(Collections.emptyList()));
        assertFalse(N.notEmpty((Collection<?>) null));
        assertTrue(N.notEmpty(Arrays.asList(1)));

        // Maps
        assertFalse(N.notEmpty(Collections.emptyMap()));
        assertFalse(N.notEmpty((Map<?, ?>) null));
        Map<String, Integer> map = new HashMap<>();
        map.put("a", 1);
        assertTrue(N.notEmpty(map));

        // Strings
        assertFalse(N.notEmpty(""));
        assertFalse(N.notEmpty((String) null));
        assertTrue(N.notEmpty("a"));
    }

    // Test for copy() array methods
    @Test
    public void testCopyArray() {
        // Test copy with all types
        int[] intArr = { 1, 2, 3, 4, 5 };
        int[] intDest = new int[5];
        N.copy(intArr, 0, intDest, 0, 5);
        assertArrayEquals(intArr, intDest);

        // Test partial copy
        intDest = new int[3];
        N.copy(intArr, 1, intDest, 0, 3);
        assertArrayEquals(new int[] { 2, 3, 4 }, intDest);

        // Test with other types
        double[] doubleArr = { 1.0, 2.0, 3.0 };
        double[] doubleDest = new double[3];
        N.copy(doubleArr, 0, doubleDest, 0, 3);
        assertArrayEquals(doubleArr, doubleDest, 0.001);

        String[] strArr = { "a", "b", "c" };
        String[] strDest = new String[3];
        N.copy(strArr, 0, strDest, 0, 3);
        assertArrayEquals(strArr, strDest);
    }

    // Test for copyOfRange() methods
    @Test
    public void testCopyOfRange() {
        // Test with various types
        assertArrayEquals(new int[] { 2, 3, 4 }, N.copyOfRange(new int[] { 1, 2, 3, 4, 5 }, 1, 4));
        assertArrayEquals(new long[] { 2L, 3L, 4L }, N.copyOfRange(new long[] { 1L, 2L, 3L, 4L, 5L }, 1, 4));
        assertArrayEquals(new double[] { 2.0, 3.0, 4.0 }, N.copyOfRange(new double[] { 1.0, 2.0, 3.0, 4.0, 5.0 }, 1, 4), 0.001);
        assertArrayEquals(new String[] { "b", "c", "d" }, N.copyOfRange(new String[] { "a", "b", "c", "d", "e" }, 1, 4));

        // Test edge cases
        assertArrayEquals(new int[] {}, N.copyOfRange(new int[] { 1, 2, 3 }, 1, 1));
        assertArrayEquals(new int[] { 1, 2, 3 }, N.copyOfRange(new int[] { 1, 2, 3 }, 0, 3));
    }

    // Test for clone() methods
    @Test
    public void testClone() {
        // Test with various types
        int[] intArr = { 1, 2, 3 };
        int[] intClone = N.clone(intArr);
        assertArrayEquals(intArr, intClone);
        assertNotSame(intArr, intClone);

        assertNull(N.clone((int[]) null));

        String[] strArr = { "a", "b", "c" };
        String[] strClone = N.clone(strArr);
        assertArrayEquals(strArr, strClone);
        assertNotSame(strArr, strClone);
    }

    // Test for equals() methods
    @Test
    public void testEquals() {
        // Test primitive arrays
        assertTrue(N.equals(new int[] { 1, 2, 3 }, new int[] { 1, 2, 3 }));
        assertFalse(N.equals(new int[] { 1, 2, 3 }, new int[] { 1, 2, 4 }));
        assertFalse(N.equals(new int[] { 1, 2 }, new int[] { 1, 2, 3 }));
        assertTrue(N.equals((int[]) null, (int[]) null));
        assertFalse(N.equals(new int[] { 1 }, null));
        assertFalse(N.equals(null, new int[] { 1 }));

        // Test object arrays
        assertTrue(N.equals(new String[] { "a", "b" }, new String[] { "a", "b" }));
        assertFalse(N.equals(new String[] { "a", "b" }, new String[] { "a", "c" }));

        // Test with nulls in array
        assertTrue(N.equals(new String[] { null, "a" }, new String[] { null, "a" }));
        assertFalse(N.equals(new String[] { null, "a" }, new String[] { "a", null }));

        // Test objects
        assertTrue(N.equals("abc", "abc"));
        assertFalse(N.equals("abc", "def"));
        assertTrue(N.equals((String) null, (String) null));
        assertFalse(N.equals("abc", null));
        assertFalse(N.equals(null, "abc"));

        // Test floating point
        assertTrue(N.equals(1.0, 1.0));
        assertFalse(N.equals(1.0, 1.1));
        assertTrue(N.equals(Double.NaN, Double.NaN));
        assertTrue(N.equals(Float.NaN, Float.NaN));
    }

    // Test for compare() methods
    @Test
    public void testCompare() {
        // Test primitives
        assertEquals(0, N.compare(1, 1));
        assertTrue(N.compare(1, 2) < 0);
        assertTrue(N.compare(2, 1) > 0);

        assertEquals(0, N.compare(1.0, 1.0));
        assertTrue(N.compare(1.0, 2.0) < 0);
        assertTrue(N.compare(2.0, 1.0) > 0);

        // Test objects
        assertEquals(0, N.compare("a", "a"));
        assertTrue(N.compare("a", "b") < 0);
        assertTrue(N.compare("b", "a") > 0);

        // Test with nulls
        assertEquals(0, N.compare((String) null, (String) null));
        assertTrue(N.compare(null, "a") < 0); // null is considered maximum
        assertTrue(N.compare("a", null) > 0);
    }

    // Test for hashCode() methods
    @Test
    public void testHashCode() {
        // Test arrays
        int[] arr1 = { 1, 2, 3 };
        int[] arr2 = { 1, 2, 3 };
        int[] arr3 = { 1, 2, 4 };

        assertEquals(N.hashCode(arr1), N.hashCode(arr2));
        assertNotEquals(N.hashCode(arr1), N.hashCode(arr3));

        // Test objects
        assertEquals(N.hashCode("abc"), N.hashCode("abc"));
        assertNotEquals(N.hashCode("abc"), N.hashCode("def"));

        // Test null
        assertEquals(0, N.hashCode((String) null));
    }

    // Test for toString() methods
    @Test
    public void testToString() {
        // Test arrays
        assertEquals("[1, 2, 3]", N.toString(new int[] { 1, 2, 3 }));
        assertEquals("[a, b, c]", N.toString(new String[] { "a", "b", "c" }));
        assertEquals("[null, a]", N.toString(new String[] { null, "a" }));
        assertEquals("null", N.toString((String[]) null));

        // Test objects
        assertEquals("abc", N.toString("abc"));
        assertEquals("123", N.toString(123));
    }

    // Test for newArray() method
    @Test
    public void testNewArray() {
        // Test creating arrays of different types
        String[] strArray = N.newArray(String.class, 5);
        assertNotNull(strArray);
        assertEquals(5, strArray.length);

        Integer[] intArray = N.newArray(Integer.class, 3);
        assertNotNull(intArray);
        assertEquals(3, intArray.length);

        // Test with 0 length
        Object[] emptyArray = N.newArray(Object.class, 0);
        assertNotNull(emptyArray);
        assertEquals(0, emptyArray.length);
    }

    // Test for toList() method
    @Test
    public void testToList() {
        // Test with arrays
        List<Integer> list = N.toList(new Integer[] { 1, 2, 3 });
        assertEquals(Arrays.asList(1, 2, 3), list);

        // Test with range
        list = N.toList(new Integer[] { 1, 2, 3, 4, 5 }, 1, 4);
        assertEquals(Arrays.asList(2, 3, 4), list);

        // Test with empty array
        list = N.toList(new Integer[] {});
        assertEquals(Collections.emptyList(), list);

        // Test with null
        list = N.toList((Integer[]) null);
        assertEquals(Collections.emptyList(), list);
    }

    // Test for toSet() method
    @Test
    public void testToSet() {
        // Test with arrays
        Set<Integer> set = N.toSet(new Integer[] { 1, 2, 3, 2, 1 });
        assertEquals(new HashSet<>(Arrays.asList(1, 2, 3)), set);

        // Test with empty array
        set = N.toSet(new Integer[] {});
        assertEquals(Collections.emptySet(), set);

        // Test with null
        set = N.toSet((Integer[]) null);
        assertEquals(Collections.emptySet(), set);
    }

    // Test for newHashSet() method
    @Test
    public void testNewHashSet() {
        // Test creating HashSet with initial capacity
        Set<String> set = N.newHashSet(10);
        assertNotNull(set);
        assertTrue(set instanceof HashSet);
        assertEquals(0, set.size());

        // Test with elements
        set = N.newHashSet(Arrays.asList("a", "b", "c"));
        assertEquals(3, set.size());
        assertTrue(set.contains("a"));
        assertTrue(set.contains("b"));
        assertTrue(set.contains("c"));
    }

    // Test for newLinkedHashMap() method
    @Test
    public void testNewLinkedHashMap() {
        // Test creating LinkedHashMap with initial capacity
        Map<String, Integer> map = N.newLinkedHashMap(10);
        assertNotNull(map);
        assertTrue(map instanceof LinkedHashMap);
        assertEquals(0, map.size());
    }

    // Test for checkArgNotNull() method
    @Test
    public void testCheckArgNotNull() {
        assertThrows(IllegalArgumentException.class, () -> N.checkArgNotNull(null, "arg"));
    }

    @Test
    public void testCheckArgNotNullValid() {
        // Should not throw
        N.checkArgNotNull("valid", "arg");
    }

    // Test for checkArgNotEmpty() method
    @Test
    public void testCheckArgNotEmptyNull() {
        assertThrows(IllegalArgumentException.class, () -> N.checkArgNotEmpty((String) null, "arg"));
    }

    @Test
    public void testCheckArgNotEmptyEmpty() {
        assertThrows(IllegalArgumentException.class, () -> N.checkArgNotEmpty("", "arg"));
    }

    @Test
    public void testCheckArgNotEmptyArrayNull() {
        assertThrows(IllegalArgumentException.class, () -> N.checkArgNotEmpty((int[]) null, "arg"));
    }

    @Test
    public void testCheckArgNotEmptyArrayEmpty() {
        assertThrows(IllegalArgumentException.class, () -> N.checkArgNotEmpty(new int[] {}, "arg"));
    }

    @Test
    public void testCheckArgNotEmptyValid() {
        // Should not throw
        N.checkArgNotEmpty("valid", "arg");
        N.checkArgNotEmpty(new int[] { 1 }, "arg");
    }

    // Test for checkArgNotNegative() method
    @Test
    public void testCheckArgNotNegative() {
        assertThrows(IllegalArgumentException.class, () -> N.checkArgNotNegative(-1, "arg"));
    }

    @Test
    public void testCheckArgNotNegativeValid() {
        // Should not throw
        N.checkArgNotNegative(0, "arg");
        N.checkArgNotNegative(1, "arg");
    }

    // Test for checkArgument() method
    @Test
    public void testCheckArgument() {
        assertThrows(IllegalArgumentException.class, () -> N.checkArgument(false, "Invalid argument"));
    }

    @Test
    public void testCheckArgumentValid() {
        // Should not throw
        N.checkArgument(true, "Valid argument");
    }

    // Test for checkFromToIndex() method
    @Test
    public void testCheckFromToIndexInvalidFrom() {
        assertThrows(IndexOutOfBoundsException.class, () -> N.checkFromToIndex(-1, 2, 5));
    }

    @Test
    public void testCheckFromToIndexInvalidTo() {
        assertThrows(IndexOutOfBoundsException.class, () -> N.checkFromToIndex(0, 6, 5));
    }

    @Test
    public void testCheckFromToIndexInvalidRange() {
        assertThrows(IndexOutOfBoundsException.class, () -> N.checkFromToIndex(3, 2, 5));
    }

    @Test
    public void testCheckFromToIndexValid() {
        // Should not throw
        N.checkFromToIndex(0, 5, 5);
        N.checkFromToIndex(1, 3, 5);
        N.checkFromToIndex(2, 2, 5); // empty range is valid
    }

    // Additional edge case tests

    @Test
    public void testNaNHandling() {
        // Test min/max with NaN
        assertEquals(Float.NaN, N.min(1.0f, Float.NaN), 0.001f);
        assertEquals(Float.NaN, N.max(1.0f, Float.NaN), 0.001f);
        assertEquals(Double.NaN, N.min(1.0, Double.NaN), 0.001);
        assertEquals(Double.NaN, N.max(1.0, Double.NaN), 0.001);

        // Test arrays with NaN
        assertEquals(1.0f, N.min(new float[] { 1.0f, Float.NaN, 3.0f }), 0.001f);
        assertEquals(3.0f, N.max(new float[] { 1.0f, Float.NaN, 3.0f }), 0.001f);
    }

    @Test
    public void testComparatorEdgeCases() {
        // Test with reverse comparator
        Comparator<Integer> reverseComp = Comparator.reverseOrder();
        assertEquals(Integer.valueOf(3), N.min(new Integer[] { 1, 2, 3 }, reverseComp));
        assertEquals(Integer.valueOf(1), N.max(new Integer[] { 1, 2, 3 }, reverseComp));

        // Test with null comparator (should use natural ordering)
        assertEquals(Integer.valueOf(1), N.min(new Integer[] { 1, 2, 3 }, null));
        assertEquals(Integer.valueOf(3), N.max(new Integer[] { 1, 2, 3 }, null));
    }

    @Test
    public void testLargeArrayPerformance() {
        // Test with large arrays to ensure algorithms work correctly
        int[] largeArray = new int[10000];
        for (int i = 0; i < largeArray.length; i++) {
            largeArray[i] = i;
        }

        // Test kthLargest with large array
        assertEquals(9999, N.kthLargest(largeArray, 1)); // max
        assertEquals(5000, N.kthLargest(largeArray, 5000)); // median-ish
        assertEquals(0, N.kthLargest(largeArray, 10000)); // min

        // Test top with large array
        int[] top100 = N.top(largeArray, 100);
        assertEquals(100, top100.length);
        assertEquals(9900, top100[0]); // smallest of top 100
    }

    @Test
    public void testThreadSafety() {
        // Basic thread safety test for stateless methods
        final int[] array = { 1, 2, 3, 4, 5 };
        final int iterations = 1000;
        final int threadCount = 10;

        Thread[] threads = new Thread[threadCount];
        final boolean[] errors = new boolean[threadCount];

        for (int i = 0; i < threadCount; i++) {
            final int threadIndex = i;
            threads[i] = new Thread(() -> {
                try {
                    for (int j = 0; j < iterations; j++) {
                        // Test various methods
                        assertEquals(1, N.min(array));
                        assertEquals(5, N.max(array));
                        assertEquals(3, N.median(array));
                        assertEquals(15, N.sum(array));
                        assertEquals(3.0, N.average(array), 0.001);
                    }
                } catch (Exception e) {
                    errors[threadIndex] = true;
                }
            });
            threads[i].start();
        }

        // Wait for all threads to complete
        for (Thread thread : threads) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                fail("Thread interrupted");
            }
        }

        // Check for errors
        for (boolean error : errors) {
            assertFalse(error, "Thread safety issue detected");
        }
    }
}

package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
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
import java.util.TreeMap;
import java.util.function.Predicate;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

@Tag("new-test")
public class N105Test extends TestBase {

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
        assertEquals(10, N.sumInt(CommonUtil.asLinkedHashSet(1, 2, 3, 4), 1, 3, x -> x * 2));
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
        assertEquals(10L, N.sumLong(CommonUtil.asLinkedHashSet(1L, 2L, 3L, 4L), 1, 3, x -> x * 2));
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
        assertEquals(10.0, N.sumDouble(CommonUtil.asLinkedHashSet(1.0, 2.0, 3.0, 4.0), 1, 3, x -> x * 2), 0.001);
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
        assertEquals(5.0, N.averageInt(CommonUtil.asLinkedHashSet(1, 2, 3, 4), 1, 3, x -> x * 2), 0.001);
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
        assertEquals(5.0, N.averageLong(CommonUtil.asLinkedHashSet(1L, 2L, 3L, 4L), 1, 3, x -> x * 2), 0.001);
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

        result = N.minMax(new Integer[] { 1 });
        assertEquals(Integer.valueOf(1), result.left());
        assertEquals(Integer.valueOf(1), result.right());
    }

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

        assertEquals(1, N.median(1));

        assertEquals(1, N.median(1, 2));

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

        assertEquals(5, N.kthLargest(new int[] { 1, 2, 3, 4, 5 }, 1));

        assertEquals(1, N.kthLargest(new int[] { 1, 2, 3, 4, 5 }, 5));

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

    @Test
    public void testTopShort() {
        assertArrayEquals(new short[] { 4, 5 }, N.top(new short[] { 1, 2, 3, 4, 5 }, 2));
        assertArrayEquals(new short[] { 2, 1 }, N.top(new short[] { 1, 2, 3, 4, 5 }, 2, Comparator.reverseOrder()));
        assertArrayEquals(new short[] { 3, 4 }, N.top(new short[] { 1, 2, 3, 4, 5 }, 1, 4, 2));
        assertArrayEquals(new short[] { 3, 2 }, N.top(new short[] { 1, 2, 3, 4, 5 }, 1, 4, 2, Comparator.reverseOrder()));

        assertArrayEquals(new short[] { 1, 2, 3 }, N.top(new short[] { 1, 2, 3 }, 5));

        assertArrayEquals(new short[] {}, N.top(new short[] { 1, 2, 3 }, 0));

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

        assertEquals(Arrays.asList(3, 4, 5), N.top(Arrays.asList(1, 2, 3, null, 4, 5), 3));
        assertEquals(Arrays.asList(3, 4, 5), N.top(CommonUtil.asLinkedHashSet(1, 2, 3, null, 4, 5), 3));

        assertEquals(Arrays.asList(1, 2, 3, 4, 5), N.top(Arrays.asList(1, 2, 3, 4, 5), 0, 5, 6, Comparator.reverseOrder()));
        assertEquals(Arrays.asList(2, 3, 4), N.top(Arrays.asList(1, 2, 3, 4, 5), 1, 4, 6, Comparator.reverseOrder()));

        assertEquals(Arrays.asList(5, 4), N.top(Arrays.asList(1, 5, 3, 4, 2), 2, true));
        assertEquals(Arrays.asList(5, 4), N.top(Arrays.asList(1, 5, 3, 4, 2), 2, Comparator.naturalOrder(), true));
        assertEquals(Arrays.asList(5, 4), N.top(Arrays.asList(1, 5, 3, 4, 2), 1, 4, 2, true));
        assertEquals(Arrays.asList(5, 4), N.top(Arrays.asList(1, 5, 3, 4, 2), 1, 4, 2, Comparator.naturalOrder(), true));

        assertEquals(Arrays.asList(1, 2, 3, 4, 5), N.top(Arrays.asList(1, 2, 3, 4, 5), 0, 5, 6, Comparator.reverseOrder(), true));
        assertEquals(Arrays.asList(2, 3, 4), N.top(Arrays.asList(1, 2, 3, 4, 5), 1, 4, 6, Comparator.reverseOrder(), true));
    }

    @Test
    public void testTopNegativeN() {
        assertThrows(IllegalArgumentException.class, () -> N.top(new int[] { 1, 2, 3 }, -1));
    }

    @Test
    public void testPercentilesChar() {
        char[] sorted = { 'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j' };
        Map<Percentage, Character> percentiles = N.percentilesOfSorted(sorted);
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
        Map<Percentage, Byte> percentiles = N.percentilesOfSorted(sorted);
        assertNotNull(percentiles);
        assertEquals(Percentage.values().length, percentiles.size());
    }

    @Test
    public void testPercentilesShort() {
        short[] sorted = { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10 };
        Map<Percentage, Short> percentiles = N.percentilesOfSorted(sorted);
        assertNotNull(percentiles);
        assertEquals(Percentage.values().length, percentiles.size());
    }

    @Test
    public void testPercentilesInt() {
        int[] sorted = new int[100];
        for (int i = 0; i < 100; i++) {
            sorted[i] = i + 1;
        }
        Map<Percentage, Integer> percentiles = N.percentilesOfSorted(sorted);
        assertNotNull(percentiles);
        assertEquals(Percentage.values().length, percentiles.size());
        assertEquals(Integer.valueOf(2), percentiles.get(Percentage._1));
        assertEquals(Integer.valueOf(51), percentiles.get(Percentage._50));
        assertEquals(Integer.valueOf(100), percentiles.get(Percentage._99));
    }

    @Test
    public void testPercentilesLong() {
        long[] sorted = { 1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L, 10L };
        Map<Percentage, Long> percentiles = N.percentilesOfSorted(sorted);
        assertNotNull(percentiles);
        assertEquals(Percentage.values().length, percentiles.size());
    }

    @Test
    public void testPercentilesFloat() {
        float[] sorted = { 1.0f, 2.0f, 3.0f, 4.0f, 5.0f, 6.0f, 7.0f, 8.0f, 9.0f, 10.0f };
        Map<Percentage, Float> percentiles = N.percentilesOfSorted(sorted);
        assertNotNull(percentiles);
        assertEquals(Percentage.values().length, percentiles.size());
    }

    @Test
    public void testPercentilesDouble() {
        double[] sorted = { 1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0, 9.0, 10.0 };
        Map<Percentage, Double> percentiles = N.percentilesOfSorted(sorted);
        assertNotNull(percentiles);
        assertEquals(Percentage.values().length, percentiles.size());
    }

    @Test
    public void testPercentilesGeneric() {
        String[] sorted = { "a", "b", "c", "d", "e", "f", "g", "h", "i", "j" };
        Map<Percentage, String> percentiles = N.percentilesOfSorted(sorted);
        assertNotNull(percentiles);
        assertEquals(Percentage.values().length, percentiles.size());
    }

    @Test
    public void testPercentilesList() {
        List<Integer> sorted = Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
        Map<Percentage, Integer> percentiles = N.percentilesOfSorted(sorted);
        assertNotNull(percentiles);
        assertEquals(Percentage.values().length, percentiles.size());
    }

    @Test
    public void testPercentilesEmptyArray() {
        assertThrows(IllegalArgumentException.class, () -> N.percentilesOfSorted(new int[] {}));
    }

    @Test
    public void testPercentilesNullArray() {
        assertThrows(IllegalArgumentException.class, () -> N.percentilesOfSorted((int[]) null));
    }

    @Test
    public void testPercentilesEmptyList() {
        assertThrows(IllegalArgumentException.class, () -> N.percentilesOfSorted(Collections.emptyList()));
    }

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

    @Test
    public void testLen() {
        assertEquals(5, CommonUtil.len(new int[] { 1, 2, 3, 4, 5 }));
        assertEquals(0, CommonUtil.len(new int[] {}));
        assertEquals(0, CommonUtil.len((int[]) null));

        assertEquals(3, CommonUtil.len(new String[] { "a", "b", "c" }));
        assertEquals(0, CommonUtil.len(new String[] {}));
        assertEquals(0, CommonUtil.len((String[]) null));
    }

    @Test
    public void testSize() {
        assertEquals(5, CommonUtil.size(Arrays.asList(1, 2, 3, 4, 5)));
        assertEquals(0, CommonUtil.size(Collections.emptyList()));
        assertEquals(0, CommonUtil.size((Collection<?>) null));

        Map<String, Integer> map = new HashMap<>();
        map.put("a", 1);
        map.put("b", 2);
        assertEquals(2, CommonUtil.size(map));
        assertEquals(0, CommonUtil.size(Collections.emptyMap()));
        assertEquals(0, CommonUtil.size((Map<?, ?>) null));
    }

    @Test
    public void testIsEmpty() {
        assertTrue(CommonUtil.isEmpty(new int[] {}));
        assertTrue(CommonUtil.isEmpty((int[]) null));
        assertFalse(CommonUtil.isEmpty(new int[] { 1 }));

        assertTrue(CommonUtil.isEmpty(Collections.emptyList()));
        assertTrue(CommonUtil.isEmpty((Collection<?>) null));
        assertFalse(CommonUtil.isEmpty(Arrays.asList(1)));

        assertTrue(CommonUtil.isEmpty(Collections.emptyMap()));
        assertTrue(CommonUtil.isEmpty((Map<?, ?>) null));
        Map<String, Integer> map = new HashMap<>();
        map.put("a", 1);
        assertFalse(CommonUtil.isEmpty(map));

        assertTrue(CommonUtil.isEmpty(""));
        assertTrue(CommonUtil.isEmpty((String) null));
        assertFalse(CommonUtil.isEmpty("a"));
    }

    @Test
    public void testNotEmpty() {
        assertFalse(CommonUtil.notEmpty(new int[] {}));
        assertFalse(CommonUtil.notEmpty((int[]) null));
        assertTrue(CommonUtil.notEmpty(new int[] { 1 }));

        assertFalse(CommonUtil.notEmpty(Collections.emptyList()));
        assertFalse(CommonUtil.notEmpty((Collection<?>) null));
        assertTrue(CommonUtil.notEmpty(Arrays.asList(1)));

        assertFalse(CommonUtil.notEmpty(Collections.emptyMap()));
        assertFalse(CommonUtil.notEmpty((Map<?, ?>) null));
        Map<String, Integer> map = new HashMap<>();
        map.put("a", 1);
        assertTrue(CommonUtil.notEmpty(map));

        assertFalse(CommonUtil.notEmpty(""));
        assertFalse(CommonUtil.notEmpty((String) null));
        assertTrue(CommonUtil.notEmpty("a"));
    }

    @Test
    public void testCopyArray() {
        int[] intArr = { 1, 2, 3, 4, 5 };
        int[] intDest = new int[5];
        CommonUtil.copy(intArr, 0, intDest, 0, 5);
        assertArrayEquals(intArr, intDest);

        intDest = new int[3];
        CommonUtil.copy(intArr, 1, intDest, 0, 3);
        assertArrayEquals(new int[] { 2, 3, 4 }, intDest);

        double[] doubleArr = { 1.0, 2.0, 3.0 };
        double[] doubleDest = new double[3];
        CommonUtil.copy(doubleArr, 0, doubleDest, 0, 3);
        assertArrayEquals(doubleArr, doubleDest, 0.001);

        String[] strArr = { "a", "b", "c" };
        String[] strDest = new String[3];
        CommonUtil.copy(strArr, 0, strDest, 0, 3);
        assertArrayEquals(strArr, strDest);
    }

    @Test
    public void testCopyOfRange() {
        assertArrayEquals(new int[] { 2, 3, 4 }, CommonUtil.copyOfRange(new int[] { 1, 2, 3, 4, 5 }, 1, 4));
        assertArrayEquals(new long[] { 2L, 3L, 4L }, CommonUtil.copyOfRange(new long[] { 1L, 2L, 3L, 4L, 5L }, 1, 4));
        assertArrayEquals(new double[] { 2.0, 3.0, 4.0 }, CommonUtil.copyOfRange(new double[] { 1.0, 2.0, 3.0, 4.0, 5.0 }, 1, 4), 0.001);
        assertArrayEquals(new String[] { "b", "c", "d" }, CommonUtil.copyOfRange(new String[] { "a", "b", "c", "d", "e" }, 1, 4));

        assertArrayEquals(new int[] {}, CommonUtil.copyOfRange(new int[] { 1, 2, 3 }, 1, 1));
        assertArrayEquals(new int[] { 1, 2, 3 }, CommonUtil.copyOfRange(new int[] { 1, 2, 3 }, 0, 3));
    }

    @Test
    public void testClone() {
        int[] intArr = { 1, 2, 3 };
        int[] intClone = CommonUtil.clone(intArr);
        assertArrayEquals(intArr, intClone);
        assertNotSame(intArr, intClone);

        assertNull(CommonUtil.clone((int[]) null));

        String[] strArr = { "a", "b", "c" };
        String[] strClone = CommonUtil.clone(strArr);
        assertArrayEquals(strArr, strClone);
        assertNotSame(strArr, strClone);
    }

    @Test
    public void testEquals() {
        assertTrue(CommonUtil.equals(new int[] { 1, 2, 3 }, new int[] { 1, 2, 3 }));
        assertFalse(CommonUtil.equals(new int[] { 1, 2, 3 }, new int[] { 1, 2, 4 }));
        assertFalse(CommonUtil.equals(new int[] { 1, 2 }, new int[] { 1, 2, 3 }));
        assertTrue(CommonUtil.equals((int[]) null, (int[]) null));
        assertFalse(CommonUtil.equals(new int[] { 1 }, null));
        assertFalse(CommonUtil.equals(null, new int[] { 1 }));

        assertTrue(CommonUtil.equals(new String[] { "a", "b" }, new String[] { "a", "b" }));
        assertFalse(CommonUtil.equals(new String[] { "a", "b" }, new String[] { "a", "c" }));

        assertTrue(CommonUtil.equals(new String[] { null, "a" }, new String[] { null, "a" }));
        assertFalse(CommonUtil.equals(new String[] { null, "a" }, new String[] { "a", null }));

        assertTrue(CommonUtil.equals("abc", "abc"));
        assertFalse(CommonUtil.equals("abc", "def"));
        assertTrue(CommonUtil.equals((String) null, (String) null));
        assertFalse(CommonUtil.equals("abc", null));
        assertFalse(CommonUtil.equals(null, "abc"));

        assertTrue(CommonUtil.equals(1.0, 1.0));
        assertFalse(CommonUtil.equals(1.0, 1.1));
        assertTrue(CommonUtil.equals(Double.NaN, Double.NaN));
        assertTrue(CommonUtil.equals(Float.NaN, Float.NaN));
    }

    @Test
    public void testCompare() {
        assertEquals(0, CommonUtil.compare(1, 1));
        assertTrue(CommonUtil.compare(1, 2) < 0);
        assertTrue(CommonUtil.compare(2, 1) > 0);

        assertEquals(0, CommonUtil.compare(1.0, 1.0));
        assertTrue(CommonUtil.compare(1.0, 2.0) < 0);
        assertTrue(CommonUtil.compare(2.0, 1.0) > 0);

        assertEquals(0, CommonUtil.compare("a", "a"));
        assertTrue(CommonUtil.compare("a", "b") < 0);
        assertTrue(CommonUtil.compare("b", "a") > 0);

        assertEquals(0, CommonUtil.compare((String) null, (String) null));
        assertTrue(CommonUtil.compare(null, "a") < 0);
        assertTrue(CommonUtil.compare("a", null) > 0);
    }

    @Test
    public void testHashCode() {
        int[] arr1 = { 1, 2, 3 };
        int[] arr2 = { 1, 2, 3 };
        int[] arr3 = { 1, 2, 4 };

        assertEquals(CommonUtil.hashCode(arr1), CommonUtil.hashCode(arr2));
        assertNotEquals(CommonUtil.hashCode(arr1), CommonUtil.hashCode(arr3));

        assertEquals(CommonUtil.hashCode("abc"), CommonUtil.hashCode("abc"));
        assertNotEquals(CommonUtil.hashCode("abc"), CommonUtil.hashCode("def"));

        assertEquals(0, CommonUtil.hashCode((String) null));
    }

    @Test
    public void testToString() {
        assertEquals("[1, 2, 3]", CommonUtil.toString(new int[] { 1, 2, 3 }));
        assertEquals("[a, b, c]", CommonUtil.toString(new String[] { "a", "b", "c" }));
        assertEquals("[null, a]", CommonUtil.toString(new String[] { null, "a" }));
        assertEquals("null", CommonUtil.toString((String[]) null));

        assertEquals("abc", CommonUtil.toString("abc"));
        assertEquals("123", CommonUtil.toString(123));
    }

    @Test
    public void testNewArray() {
        String[] strArray = CommonUtil.newArray(String.class, 5);
        assertNotNull(strArray);
        assertEquals(5, strArray.length);

        Integer[] intArray = CommonUtil.newArray(Integer.class, 3);
        assertNotNull(intArray);
        assertEquals(3, intArray.length);

        Object[] emptyArray = CommonUtil.newArray(Object.class, 0);
        assertNotNull(emptyArray);
        assertEquals(0, emptyArray.length);
    }

    @Test
    public void testToList() {
        List<Integer> list = CommonUtil.toList(new Integer[] { 1, 2, 3 });
        assertEquals(Arrays.asList(1, 2, 3), list);

        list = CommonUtil.toList(new Integer[] { 1, 2, 3, 4, 5 }, 1, 4);
        assertEquals(Arrays.asList(2, 3, 4), list);

        list = CommonUtil.toList(new Integer[] {});
        assertEquals(Collections.emptyList(), list);

        list = CommonUtil.toList((Integer[]) null);
        assertEquals(Collections.emptyList(), list);
    }

    @Test
    public void testToSet() {
        Set<Integer> set = CommonUtil.toSet(new Integer[] { 1, 2, 3, 2, 1 });
        assertEquals(new HashSet<>(Arrays.asList(1, 2, 3)), set);

        set = CommonUtil.toSet(new Integer[] {});
        assertEquals(Collections.emptySet(), set);

        set = CommonUtil.toSet((Integer[]) null);
        assertEquals(Collections.emptySet(), set);
    }

    @Test
    public void testNewHashSet() {
        Set<String> set = CommonUtil.newHashSet(10);
        assertNotNull(set);
        assertTrue(set instanceof HashSet);
        assertEquals(0, set.size());

        set = CommonUtil.newHashSet(Arrays.asList("a", "b", "c"));
        assertEquals(3, set.size());
        assertTrue(set.contains("a"));
        assertTrue(set.contains("b"));
        assertTrue(set.contains("c"));
    }

    @Test
    public void testNewLinkedHashMap() {
        Map<String, Integer> map = CommonUtil.newLinkedHashMap(10);
        assertNotNull(map);
        assertTrue(map instanceof LinkedHashMap);
        assertEquals(0, map.size());
    }

    @Test
    public void testCheckArgNotNull() {
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgNotNull(null, "arg"));
    }

    @Test
    public void testCheckArgNotNullValid() {
        CommonUtil.checkArgNotNull("valid", "arg");
    }

    @Test
    public void testCheckArgNotEmptyNull() {
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgNotEmpty((String) null, "arg"));
    }

    @Test
    public void testCheckArgNotEmptyEmpty() {
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgNotEmpty("", "arg"));
    }

    @Test
    public void testCheckArgNotEmptyArrayNull() {
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgNotEmpty((int[]) null, "arg"));
    }

    @Test
    public void testCheckArgNotEmptyArrayEmpty() {
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgNotEmpty(new int[] {}, "arg"));
    }

    @Test
    public void testCheckArgNotEmptyValid() {
        CommonUtil.checkArgNotEmpty("valid", "arg");
        CommonUtil.checkArgNotEmpty(new int[] { 1 }, "arg");
    }

    @Test
    public void testCheckArgNotNegative() {
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgNotNegative(-1, "arg"));
    }

    @Test
    public void testCheckArgNotNegativeValid() {
        CommonUtil.checkArgNotNegative(0, "arg");
        CommonUtil.checkArgNotNegative(1, "arg");
    }

    @Test
    public void testCheckArgument() {
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgument(false, "Invalid argument"));
    }

    @Test
    public void testCheckArgumentValid() {
        CommonUtil.checkArgument(true, "Valid argument");
    }

    @Test
    public void testCheckFromToIndexInvalidFrom() {
        assertThrows(IndexOutOfBoundsException.class, () -> CommonUtil.checkFromToIndex(-1, 2, 5));
    }

    @Test
    public void testCheckFromToIndexInvalidTo() {
        assertThrows(IndexOutOfBoundsException.class, () -> CommonUtil.checkFromToIndex(0, 6, 5));
    }

    @Test
    public void testCheckFromToIndexInvalidRange() {
        assertThrows(IndexOutOfBoundsException.class, () -> CommonUtil.checkFromToIndex(3, 2, 5));
    }

    @Test
    public void testCheckFromToIndexValid() {
        CommonUtil.checkFromToIndex(0, 5, 5);
        CommonUtil.checkFromToIndex(1, 3, 5);
        CommonUtil.checkFromToIndex(2, 2, 5);
    }

    @Test
    public void testNaNHandling() {
        assertEquals(Float.NaN, N.min(1.0f, Float.NaN), 0.001f);
        assertEquals(Float.NaN, N.max(1.0f, Float.NaN), 0.001f);
        assertEquals(Double.NaN, N.min(1.0, Double.NaN), 0.001);
        assertEquals(Double.NaN, N.max(1.0, Double.NaN), 0.001);

        assertEquals(1.0f, N.min(new float[] { 1.0f, Float.NaN, 3.0f }), 0.001f);
        assertEquals(3.0f, N.max(new float[] { 1.0f, Float.NaN, 3.0f }), 0.001f);
    }

    @Test
    public void testComparatorEdgeCases() {
        Comparator<Integer> reverseComp = Comparator.reverseOrder();
        assertEquals(Integer.valueOf(3), N.min(new Integer[] { 1, 2, 3 }, reverseComp));
        assertEquals(Integer.valueOf(1), N.max(new Integer[] { 1, 2, 3 }, reverseComp));

        assertEquals(Integer.valueOf(1), N.min(new Integer[] { 1, 2, 3 }, null));
        assertEquals(Integer.valueOf(3), N.max(new Integer[] { 1, 2, 3 }, null));
    }

    @Test
    public void testLargeArrayPerformance() {
        int[] largeArray = new int[10000];
        for (int i = 0; i < largeArray.length; i++) {
            largeArray[i] = i;
        }

        assertEquals(9999, N.kthLargest(largeArray, 1));
        assertEquals(5000, N.kthLargest(largeArray, 5000));
        assertEquals(0, N.kthLargest(largeArray, 10000));

        int[] top100 = N.top(largeArray, 100);
        assertEquals(100, top100.length);
        assertEquals(9900, top100[0]);
    }

    @Test
    public void testThreadSafety() {
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

        for (Thread thread : threads) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                fail("Thread interrupted");
            }
        }

        for (boolean error : errors) {
            assertFalse(error, "Thread safety issue detected");
        }
    }

    private static final Predicate<String> STRING_NOT_EMPTY = s -> s != null && !s.isEmpty();
    private static final Predicate<String> STRING_HAS_LETTER_A = s -> s != null && s.contains("a");

    @Test
    public void testFilterCollectionWithRangeAndSupplier() {
        List<String> list = Arrays.asList("a", "b", "aa", "bb", "aaa");
        Set<String> resultSet = N.filter(list, 1, 5, s -> s.contains("a") && s.length() > 1, HashSet::new);
        assertEquals(Set.of("aa", "aaa"), resultSet);

        resultSet = N.filter(CommonUtil.newLinkedHashSet(list), 1, 5, s -> s.contains("a") && s.length() > 1, HashSet::new);
        assertEquals(Set.of("aa", "aaa"), resultSet);
        assertTrue(N.filter(list, 1, 1, STRING_NOT_EMPTY, ArrayList::new).isEmpty());
    }

    @Test
    public void testMapToCollection() {
        {
            List<String> list = Arrays.asList("hello", "world");
            assertArrayEquals(new char[] { 'h', 'w' }, N.mapToChar(list, s -> s.charAt(0)));

            assertArrayEquals(new char[] { 'h', 'w' }, N.mapToChar(CommonUtil.newLinkedHashSet(list), s -> s.charAt(0)));
        }
        {
            List<String> list = Arrays.asList("hello", "world");
            assertArrayEquals(new byte[] { 104, 119 }, N.mapToByte(list, s -> (byte) s.charAt(0)));
            assertArrayEquals(new byte[] { 104, 119 }, N.mapToByte(CommonUtil.newLinkedHashSet(list), s -> (byte) s.charAt(0)));
        }
        {
            List<String> list = Arrays.asList("hello", "world");
            assertArrayEquals(new short[] { 104, 119 }, N.mapToShort(list, s -> (short) s.charAt(0)));
            assertArrayEquals(new short[] { 104, 119 }, N.mapToShort(CommonUtil.newLinkedHashSet(list), s -> (short) s.charAt(0)));
        }
        {
            List<String> list = Arrays.asList("hello", "world");
            assertArrayEquals(new int[] { 104, 119 }, N.mapToInt(list, s -> (int) s.charAt(0)));
            assertArrayEquals(new int[] { 104, 119 }, N.mapToInt(CommonUtil.newLinkedHashSet(list), s -> (int) s.charAt(0)));
        }
        {
            List<String> list = Arrays.asList("hello", "world");
            assertArrayEquals(new long[] { 104L, 119L }, N.mapToLong(list, s -> (long) s.charAt(0)));
            assertArrayEquals(new long[] { 104L, 119L }, N.mapToLong(CommonUtil.newLinkedHashSet(list), s -> (long) s.charAt(0)));
        }
        {
            List<String> list = Arrays.asList("hello", "world");
            assertArrayEquals(new float[] { 104.0f, 119.0f }, N.mapToFloat(list, s -> (float) s.charAt(0)), 0.001f);
            assertArrayEquals(new float[] { 104.0f, 119.0f }, N.mapToFloat(CommonUtil.newLinkedHashSet(list), s -> (float) s.charAt(0)), 0.001f);
        }
        {
            List<String> list = Arrays.asList("hello", "world");
            assertArrayEquals(new double[] { 104.0, 119.0 }, N.mapToDouble(list, s -> (double) s.charAt(0)), 0.001);
            assertArrayEquals(new double[] { 104.0, 119.0 }, N.mapToDouble(CommonUtil.newLinkedHashSet(list), s -> (double) s.charAt(0)), 0.001);
        }
    }

    @Test
    public void testFlatMapCollectionWithRangeAndSupplier() {
        List<String> list = Arrays.asList("key", "lock", "lol");
        Set<Character> expected = CommonUtil.asSet('l', 'o', 'c', 'k');
        assertEquals(expected, N.flatMap(list, 1, 3, s -> CommonUtil.toList(s.toCharArray()), HashSet::new));
        assertEquals(expected, N.flatMap(CommonUtil.newLinkedHashSet(list), 1, 3, s -> CommonUtil.toList(s.toCharArray()), HashSet::new));
    }

    @Test
    public void test_distinct() {
        Iterable<String> iter = createIterable("hello", "world", "hello");
        List<String> expected = CommonUtil.asList("hello", "world");
        assertEquals(expected, N.distinct(iter));
        expected = CommonUtil.asList("hello", "world");
        assertEquals(expected, N.distinctBy(CommonUtil.asLinkedList("hello", "hello", "world", "hello"), 0, 3, Fn.identity()));
    }

    @Test
    public void testZipIterablesWithDefaults() {

        {
            Iterable<String> a = CommonUtil.asList("x", "y", "z");
            Iterable<String> b = CommonUtil.asList("a", "b");
            List<String> result = N.zip(a, b, "X", "A", (s, i) -> s + i);
            assertEquals(List.of("xa", "yb", "zA"), result);
        }

        {
            Iterable<String> a = CommonUtil.asList("x", "y", "z");
            Iterable<String> b = createIterable("a", "b");
            List<String> result = N.zip(a, b, "X", "A", (s, i) -> s + i);
            assertEquals(List.of("xa", "yb", "zA"), result);
        }

        {
            Iterable<String> a = createIterable("x", "y", "z");
            Iterable<String> b = CommonUtil.asList("a", "b");
            List<String> result = N.zip(a, b, "X", "A", (s, i) -> s + i);
            assertEquals(List.of("xa", "yb", "zA"), result);
        }

        {
            Iterable<String> a = createIterable("x", "y", "z");
            Iterable<String> b = createIterable("a", "b");
            List<String> result = N.zip(a, b, "X", "A", (s, i) -> s + i);
            assertEquals(List.of("xa", "yb", "zA"), result);
        }
    }

    @Test
    public void testZipIterablesWithDefaults3() {

        {
            Iterable<String> a = CommonUtil.asList("x", "y", "z");
            Iterable<String> b = CommonUtil.asList("a", "b");
            Iterable<String> c = CommonUtil.asList("1");
            List<String> result = N.zip(a, b, c, "X", "A", "0", (s, i, j) -> s + i + j);
            assertEquals(List.of("xa1", "yb0", "zA0"), result);
        }

        {
            Iterable<String> a = createIterable("x", "y", "z");
            Iterable<String> b = CommonUtil.asList("a", "b");
            Iterable<String> c = CommonUtil.asList("1");
            List<String> result = N.zip(a, b, c, "X", "A", "0", (s, i, j) -> s + i + j);
            assertEquals(List.of("xa1", "yb0", "zA0"), result);
        }

        {
            Iterable<String> a = createIterable("x", "y", "z");
            Iterable<String> b = createIterable("a", "b");
            Iterable<String> c = CommonUtil.asList("1");
            List<String> result = N.zip(a, b, c, "X", "A", "0", (s, i, j) -> s + i + j);
            assertEquals(List.of("xa1", "yb0", "zA0"), result);
        }

        {
            Iterable<String> a = createIterable("x", "y", "z");
            Iterable<String> b = createIterable("a", "b");
            Iterable<String> c = createIterable("1");
            List<String> result = N.zip(a, b, c, "X", "A", "0", (s, i, j) -> s + i + j);
            assertEquals(List.of("xa1", "yb0", "zA0"), result);
        }

        {
            Iterable<String> a = createIterable("x", "y", "z");
            Iterable<String> b = CommonUtil.asList("a", "b");
            Iterable<String> c = createIterable("1");
            List<String> result = N.zip(a, b, c, "X", "A", "0", (s, i, j) -> s + i + j);
            assertEquals(List.of("xa1", "yb0", "zA0"), result);
        }

        {
            Iterable<String> a = CommonUtil.asList("x", "y", "z");
            Iterable<String> b = CommonUtil.asList("a", "b");
            Iterable<String> c = createIterable("1");
            List<String> result = N.zip(a, b, c, "X", "A", "0", (s, i, j) -> s + i + j);
            assertEquals(List.of("xa1", "yb0", "zA0"), result);
        }

    }

    @Test
    public void testGroupByCollectionWithRangeAndMapSupplier() {
        {
            List<String> list = Arrays.asList("one", "two", "three", "four", "five");
            TreeMap<Character, List<String>> result = N.groupBy(list, 1, 4, s -> s.charAt(0), TreeMap::new);
            assertEquals(List.of("four"), result.get('f'));
            assertEquals(List.of("two", "three"), result.get('t'));
        }
        {
            List<String> list = CommonUtil.asLinkedList("one", "two", "three", "four", "five");
            TreeMap<Character, List<String>> result = N.groupBy(list, 1, 4, s -> s.charAt(0), TreeMap::new);
            assertEquals(List.of("four"), result.get('f'));
            assertEquals(List.of("two", "three"), result.get('t'));
        }
    }

    @Test
    public void forEach_iterables_triConsumer_shortCircuit() throws Exception {
        List<String> l1 = Arrays.asList("a", "b", "c");
        List<Integer> l2 = Arrays.asList(1, 2, 3, 4);
        List<Boolean> l3 = Arrays.asList(true, false);
        List<String> result = new ArrayList<>();
        N.forEach(l1, l2, l3, (s, i, bool) -> result.add(s + i + bool));

        assertEquals(Arrays.asList("a1true", "b2false"), result);
    }

    @Test
    public void test_forEach_01() throws Exception {
        {
            String[] a = CommonUtil.asArray("a", "b", "c");
            String[] b = CommonUtil.asArray("1", "2", "3", "4");
            List<String> result = new ArrayList<>();
            N.forEach(a, b, "X", "0", (s, i) -> result.add(s + i));

            assertEquals(Arrays.asList("a1", "b2", "c3", "X4"), result);
        }
        {
            List<String> a = Arrays.asList("a", "b", "c");
            List<String> b = Arrays.asList("1", "2", "3", "4");
            List<String> result = new ArrayList<>();
            N.forEach(a, b, "X", "0", (s, i) -> result.add(s + i));

            assertEquals(Arrays.asList("a1", "b2", "c3", "X4"), result);
        }
        {
            List<String> a = Arrays.asList("a", "b", "c");
            List<String> b = Arrays.asList("1", "2", "3", "4");
            List<String> result = new ArrayList<>();
            N.forEach(a.iterator(), b.iterator(), "X", "0", (s, i) -> result.add(s + i));

            assertEquals(Arrays.asList("a1", "b2", "c3", "X4"), result);
        }

        {
            String[] a = CommonUtil.asArray("a", "b", "c");
            String[] b = CommonUtil.asArray("1", "2", "3", "4");
            Boolean[] c = CommonUtil.asArray(true, false);
            List<String> result = new ArrayList<>();
            N.forEach(a, b, c, "X", "0", false, (s, i, j) -> result.add(s + i + j));

            assertEquals(Arrays.asList("a1true", "b2false", "c3false", "X4false"), result);
        }

        {
            List<String> a = Arrays.asList("a", "b", "c");
            List<String> b = Arrays.asList("1", "2", "3", "4");
            List<Boolean> c = CommonUtil.asList(true, false);
            List<String> result = new ArrayList<>();
            N.forEach(a, b, c, "X", "0", false, (s, i, j) -> result.add(s + i + j));

            assertEquals(Arrays.asList("a1true", "b2false", "c3false", "X4false"), result);
        }

        {
            List<String> a = Arrays.asList("a", "b", "c");
            List<String> b = Arrays.asList("1", "2", "3", "4");
            List<Boolean> c = CommonUtil.asList(true, false);
            List<String> result = new ArrayList<>();
            N.forEach(a.iterator(), b.iterator(), c.iterator(), "X", "0", false, (s, i, j) -> result.add(s + i + j));

            assertEquals(Arrays.asList("a1true", "b2false", "c3false", "X4false"), result);
        }
    }

    @Test
    public void test_forEachNonNull() throws Exception {
        {
            String[] a = CommonUtil.asArray("a", null, "b", null, "c");
            List<String> result = new ArrayList<>();
            N.forEachNonNull(a, e -> result.add(e));

            assertEquals(Arrays.asList("a", "b", "c"), result);
        }
        {
            String[] a = CommonUtil.asArray("a", null, "b", null, "c");
            List<String> result = new ArrayList<>();
            N.forEachNonNull(a, e -> CommonUtil.asList(e, e), (s, i) -> result.add(s + i));

            assertEquals(Arrays.asList("aa", "aa", "bb", "bb", "cc", "cc"), result);
        }
        {
            String[] a = CommonUtil.asArray("a", null, "b", null, "c");
            List<String> result = new ArrayList<>();
            N.forEachNonNull(a, e -> CommonUtil.asList(e, e), e -> CommonUtil.asList(e), (s, i, j) -> result.add(s + i + j));

            assertEquals(Arrays.asList("aaa", "aaa", "bbb", "bbb", "ccc", "ccc"), result);
        }
        {
            List<String> a = CommonUtil.asList("a", null, "b", null, "c");
            List<String> result = new ArrayList<>();
            N.forEachNonNull(a, e -> result.add(e));

            assertEquals(Arrays.asList("a", "b", "c"), result);
        }
        {
            List<String> a = CommonUtil.asList("a", null, "b", null, "c");
            List<String> result = new ArrayList<>();
            N.forEachNonNull(a, e -> CommonUtil.asList(e, e), (s, i) -> result.add(s + i));

            assertEquals(Arrays.asList("aa", "aa", "bb", "bb", "cc", "cc"), result);
        }
        {
            List<String> a = CommonUtil.asList("a", null, "b", null, "c");
            List<String> result = new ArrayList<>();
            N.forEachNonNull(a, e -> CommonUtil.asList(e, e), e -> CommonUtil.asList(e), (s, i, j) -> result.add(s + i + j));

            assertEquals(Arrays.asList("aaa", "aaa", "bbb", "bbb", "ccc", "ccc"), result);
        }
        {
            List<String> a = CommonUtil.asList("a", null, "b", null, "c");
            List<String> result = new ArrayList<>();
            N.forEachNonNull(a.iterator(), e -> result.add(e));

            assertEquals(Arrays.asList("a", "b", "c"), result);
        }
        {
            List<String> a = CommonUtil.asList("a", null, "b", null, "c");
            List<String> result = new ArrayList<>();
            N.forEachNonNull(a.iterator(), e -> CommonUtil.asList(e, e), (s, i) -> result.add(s + i));

            assertEquals(Arrays.asList("aa", "aa", "bb", "bb", "cc", "cc"), result);
        }
        {
            List<String> a = CommonUtil.asList("a", null, "b", null, "c");
            List<String> result = new ArrayList<>();
            N.forEachNonNull(a.iterator(), e -> CommonUtil.asList(e, e), e -> CommonUtil.asList(e), (s, i, j) -> result.add(s + i + j));

            assertEquals(Arrays.asList("aaa", "aaa", "bbb", "bbb", "ccc", "ccc"), result);
        }
    }

}

package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import com.landawn.abacus.TestBase;

public class PercentileTest extends TestBase {

    @Test
    void everyOverloadUsesTheExactSeventiethPercentileBoundary() {
        final char[] chars = new char[90];
        final byte[] bytes = new byte[90];
        final short[] shorts = new short[90];
        final int[] ints = new int[90];
        final long[] longs = new long[90];
        final float[] floats = new float[90];
        final double[] doubles = new double[90];
        final Integer[] objects = new Integer[90];
        for (int i = 0; i < 90; i++) {
            chars[i] = (char) i;
            bytes[i] = (byte) i;
            shorts[i] = (short) i;
            ints[i] = i;
            longs[i] = i;
            floats[i] = i;
            doubles[i] = i;
            objects[i] = i;
        }
        assertEquals((char) 63, N.percentilesOfSorted(chars).get(Percentage._70));
        assertEquals((byte) 63, N.percentilesOfSorted(bytes).get(Percentage._70));
        assertEquals((short) 63, N.percentilesOfSorted(shorts).get(Percentage._70));
        assertEquals(63, N.percentilesOfSorted(ints).get(Percentage._70));
        assertEquals(63L, N.percentilesOfSorted(longs).get(Percentage._70));
        assertEquals(63f, N.percentilesOfSorted(floats).get(Percentage._70));
        assertEquals(63d, N.percentilesOfSorted(doubles).get(Percentage._70));
        assertEquals(63, N.percentilesOfSorted(objects).get(Percentage._70));
        assertEquals(63, N.percentilesOfSorted(Arrays.asList(objects)).get(Percentage._70));
    }

    @Test
    void allPercentagesMatchDecimalArithmeticAcrossLengths() {
        for (int length = 1; length <= 1000; length++) {
            final int[] values = new int[length];
            Arrays.setAll(values, i -> i);
            assertPercentiles(length, N.percentilesOfSorted(values));
        }
        for (final int length : new int[] { 1, 90, 10000, 1000000, Integer.MAX_VALUE - 1, Integer.MAX_VALUE }) {
            // A virtual sorted list checks large index products without allocating gigabytes.
            final List<Integer> values = new AbstractList<>() {
                @Override
                public Integer get(final int index) {
                    if (index < 0 || index >= length) {
                        throw new IndexOutOfBoundsException(index);
                    }
                    return index;
                }

                @Override
                public int size() {
                    return length;
                }
            };
            assertPercentiles(length, N.percentilesOfSorted(values));
        }
    }

    @Test
    void percentilesPreserveSpecialValuesAndUnicodeCodeUnits() {
        final double[] doubles = { Double.NEGATIVE_INFINITY, -0d, 0d, Double.POSITIVE_INFINITY, Double.NaN };
        final float[] floats = { Float.NEGATIVE_INFINITY, -0f, 0f, Float.POSITIVE_INFINITY, Float.NaN };
        final Map<Percentage, Double> doubleResult = N.percentilesOfSorted(doubles);
        final Map<Percentage, Float> floatResult = N.percentilesOfSorted(floats);
        final char[] chars = { 'A', '\uD83D', '\uDE00', '\uFFFF' };
        final Map<Percentage, Character> charResult = N.percentilesOfSorted(chars);
        for (final Percentage p : Percentage.values()) {
            assertEquals(doubles[decimalIndex(doubles.length, p)], doubleResult.get(p));
            assertEquals(floats[decimalIndex(floats.length, p)], floatResult.get(p));
            assertEquals(chars[decimalIndex(chars.length, p)], charResult.get(p));
        }
        assertNull(N.percentilesOfSorted(new String[] { null, "x" }).get(Percentage._1));
        assertEquals("x", N.percentilesOfSorted(new String[] { "x" }).get(Percentage._99_9999));
    }

    @Test
    void everyPercentileOverloadRejectsNullAndEmptyInputs() {
        final List<Executable> invalidInputs = List.of(() -> N.percentilesOfSorted((char[]) null), () -> N.percentilesOfSorted(new char[0]),
                () -> N.percentilesOfSorted((byte[]) null), () -> N.percentilesOfSorted(new byte[0]), () -> N.percentilesOfSorted((short[]) null),
                () -> N.percentilesOfSorted(new short[0]), () -> N.percentilesOfSorted((int[]) null), () -> N.percentilesOfSorted(new int[0]),
                () -> N.percentilesOfSorted((long[]) null), () -> N.percentilesOfSorted(new long[0]), () -> N.percentilesOfSorted((float[]) null),
                () -> N.percentilesOfSorted(new float[0]), () -> N.percentilesOfSorted((double[]) null), () -> N.percentilesOfSorted(new double[0]),
                () -> N.percentilesOfSorted((Object[]) null), () -> N.percentilesOfSorted(new Object[0]), () -> N.percentilesOfSorted((List<Object>) null),
                () -> N.percentilesOfSorted(List.of()));
        for (final Executable invalidInput : invalidInputs) {
            assertThrows(IllegalArgumentException.class, invalidInput);
        }
    }

    private static void assertPercentiles(final int length, final Map<Percentage, Integer> result) {
        assertEquals(Arrays.asList(Percentage.values()), new ArrayList<>(result.keySet()));
        for (final Percentage p : Percentage.values()) {
            assertEquals(decimalIndex(length, p), result.get(p), () -> "length=" + length + ", percentage=" + p);
        }
    }

    private static int decimalIndex(final int length, final Percentage p) {
        final String percent = p.toString();
        final BigDecimal ratio = new BigDecimal(percent.substring(0, percent.length() - 1)).movePointLeft(2);
        return Math.min(ratio.multiply(BigDecimal.valueOf(length)).setScale(0, RoundingMode.FLOOR).intValueExact(), length - 1);
    }
}

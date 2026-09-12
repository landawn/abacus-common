package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

public class ArraySetTest extends ArrayTestSupport {

    @Test
    public void testSet() {
        final String[] a = { "a", "b", "c" };
        Array.set(a, 1, "x");
        assertArrayEquals(new String[] { "a", "x", "c" }, a);

        final int[] ints = { 1, 2, 3 };
        Array.set(ints, 1, 9);
        assertArrayEquals(new int[] { 1, 9, 3 }, ints);

        assertThrows(NullPointerException.class, () -> Array.set(null, 0, "x"));
        assertThrows(IllegalArgumentException.class, () -> Array.set("not-an-array", 0, "x"));
        assertThrows(ArrayIndexOutOfBoundsException.class, () -> Array.set(new int[] { 1 }, 1, 0));
    }

    @Test
    public void testSetBoolean() {
        final boolean[] a = { true, false, true };
        Array.setBoolean(a, 1, true);
        assertArrayEquals(new boolean[] { true, true, true }, a);
        assertThrows(NullPointerException.class, () -> Array.setBoolean(null, 0, true));
        assertThrows(IllegalArgumentException.class, () -> Array.setBoolean(new Boolean[] { true }, 0, true));
        assertThrows(ArrayIndexOutOfBoundsException.class, () -> Array.setBoolean(new boolean[] { true }, 1, false));
    }

    @Test
    public void testSetByte() {
        final byte[] a = { 1, 2, 3 };
        Array.setByte(a, 1, (byte) 9);
        assertArrayEquals(new byte[] { 1, 9, 3 }, a);
        assertThrows(NullPointerException.class, () -> Array.setByte(null, 0, (byte) 1));
        assertThrows(IllegalArgumentException.class, () -> Array.setByte(new Byte[] { 1 }, 0, (byte) 1));
    }

    @Test
    public void testSetChar() {
        final char[] a = { 'a', 'b', 'c' };
        Array.setChar(a, 1, 'z');
        assertArrayEquals(new char[] { 'a', 'z', 'c' }, a);
        assertThrows(NullPointerException.class, () -> Array.setChar(null, 0, 'a'));
        assertThrows(IllegalArgumentException.class, () -> Array.setChar(new Character[] { 'a' }, 0, 'a'));
    }

    @Test
    public void testSetShort() {
        final short[] a = { 1, 2, 3 };
        Array.setShort(a, 1, (short) 9);
        assertArrayEquals(new short[] { 1, 9, 3 }, a);
        final int[] widened = { 1, 2, 3 };
        Array.setShort(widened, 1, (short) 8);
        assertArrayEquals(new int[] { 1, 8, 3 }, widened);
        assertThrows(NullPointerException.class, () -> Array.setShort(null, 0, (short) 1));
        assertThrows(IllegalArgumentException.class, () -> Array.setShort(new byte[] { 1 }, 0, (short) 1));
    }

    @Test
    public void testSetInt() {
        final int[] a = { 1, 2, 3 };
        Array.setInt(a, 1, 9);
        assertArrayEquals(new int[] { 1, 9, 3 }, a);
        final long[] widened = { 1L, 2L, 3L };
        Array.setInt(widened, 1, 8);
        assertArrayEquals(new long[] { 1L, 8L, 3L }, widened);
        assertThrows(NullPointerException.class, () -> Array.setInt(null, 0, 1));
        assertThrows(IllegalArgumentException.class, () -> Array.setInt(new short[] { 1 }, 0, 1));
        assertThrows(ArrayIndexOutOfBoundsException.class, () -> Array.setInt(new int[] { 1 }, 1, 0));
    }

    @Test
    public void testSetLong() {
        final long[] a = { 1L, 2L, 3L };
        Array.setLong(a, 1, 9L);
        assertArrayEquals(new long[] { 1L, 9L, 3L }, a);
        assertThrows(NullPointerException.class, () -> Array.setLong(null, 0, 1L));
        assertThrows(IllegalArgumentException.class, () -> Array.setLong(new int[] { 1 }, 0, 1L));
    }

    @Test
    public void testSetFloat() {
        final float[] a = { 1f, 2f, 3f };
        Array.setFloat(a, 1, 9.5f);
        assertArrayEquals(new float[] { 1f, 9.5f, 3f }, a);
        assertThrows(NullPointerException.class, () -> Array.setFloat(null, 0, 1f));
        assertThrows(IllegalArgumentException.class, () -> Array.setFloat(new Float[] { 1f }, 0, 1f));
    }

    @Test
    public void testSetDouble() {
        final double[] a = { 1d, 2d, 3d };
        Array.setDouble(a, 1, 9.5);
        assertArrayEquals(new double[] { 1d, 9.5, 3d }, a);
        assertThrows(NullPointerException.class, () -> Array.setDouble(null, 0, 1d));
        assertThrows(IllegalArgumentException.class, () -> Array.setDouble(new Double[] { 1d }, 0, 1d));
    }

    @Test
    public void testSet_NonStorableValue() {
        // Contract pin for the corrected javadoc: a value that cannot be stored raises IllegalArgumentException,
        // never the ArrayStoreException an ordinary array assignment raises for the same mistake.
        final String[] strings = { "a" };
        assertThrows(IllegalArgumentException.class, () -> Array.set(strings, 0, Integer.valueOf(1)));
        assertThrows(IllegalArgumentException.class, () -> Array.set(new Number[1], 0, "s"));
        assertThrows(IllegalArgumentException.class, () -> Array.set(new int[1], 0, "x"));
        assertThrows(IllegalArgumentException.class, () -> Array.set(new int[1], 0, null));
        assertThrows(IllegalArgumentException.class, () -> Array.set(new short[1], 0, Integer.valueOf(3)));

        final Object[] erased = strings;
        assertThrows(ArrayStoreException.class, () -> {
            erased[0] = Integer.valueOf(1);
        });

        final long[] widened = { 1L };
        Array.set(widened, 0, Integer.valueOf(7));
        assertArrayEquals(new long[] { 7L }, widened);

        final String[] nullable = { "a" };
        Array.set(nullable, 0, null);
        assertArrayEquals(new String[] { null }, nullable);
    }
}

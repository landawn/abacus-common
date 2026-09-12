package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class ArrayGetTest extends ArrayTestSupport {

    @Test
    public void testGet() {
        assertEquals("b", Array.get(new String[] { "a", "b", "c" }, 1));
        assertEquals(2, (Integer) Array.get(new int[] { 1, 2, 3 }, 1));
        assertNull(Array.get(new String[] { "a", null }, 1));
        assertArrayEquals(new String[] { "x" }, Array.get(new String[][] { { "x" }, { "y" } }, 0));

        assertThrows(NullPointerException.class, () -> Array.get(null, 0));
        assertThrows(IllegalArgumentException.class, () -> Array.get("not-an-array", 0));
        assertThrows(ArrayIndexOutOfBoundsException.class, () -> Array.get(new int[] { 1 }, 1));
        assertThrows(ArrayIndexOutOfBoundsException.class, () -> Array.get(new int[] { 1 }, -1));
    }

    @Test
    public void testGetBoolean() {
        assertFalse(Array.getBoolean(new boolean[] { true, false, true }, 1));
        assertTrue(Array.getBoolean(new boolean[] { true }, 0));
        assertThrows(NullPointerException.class, () -> Array.getBoolean(null, 0));
        assertThrows(IllegalArgumentException.class, () -> Array.getBoolean(new Boolean[] { true }, 0));
        assertThrows(ArrayIndexOutOfBoundsException.class, () -> Array.getBoolean(new boolean[] { true }, 1));
    }

    @Test
    public void testGetByte() {
        assertEquals((byte) 2, Array.getByte(new byte[] { 1, 2, 3 }, 1));
        assertThrows(NullPointerException.class, () -> Array.getByte(null, 0));
        assertThrows(IllegalArgumentException.class, () -> Array.getByte(new Byte[] { 1 }, 0));
        assertThrows(ArrayIndexOutOfBoundsException.class, () -> Array.getByte(new byte[] { 1 }, 1));
    }

    @Test
    public void testGetChar() {
        assertEquals('b', Array.getChar(new char[] { 'a', 'b', 'c' }, 1));
        assertThrows(NullPointerException.class, () -> Array.getChar(null, 0));
        assertThrows(IllegalArgumentException.class, () -> Array.getChar(new Character[] { 'a' }, 0));
        assertThrows(ArrayIndexOutOfBoundsException.class, () -> Array.getChar(new char[] { 'a' }, 1));
    }

    @Test
    public void testGetShort() {
        assertEquals((short) 2, Array.getShort(new short[] { 1, 2, 3 }, 1));
        assertEquals((short) 2, Array.getShort(new byte[] { 1, 2, 3 }, 1));
        assertThrows(NullPointerException.class, () -> Array.getShort(null, 0));
        assertThrows(IllegalArgumentException.class, () -> Array.getShort(new Short[] { 1 }, 0));
        assertThrows(ArrayIndexOutOfBoundsException.class, () -> Array.getShort(new short[] { 1 }, 1));
    }

    @Test
    public void testGetInt() {
        assertEquals(2, Array.getInt(new int[] { 1, 2, 3 }, 1));
        assertEquals(2, Array.getInt(new short[] { 1, 2, 3 }, 1));
        assertThrows(NullPointerException.class, () -> Array.getInt(null, 0));
        assertThrows(IllegalArgumentException.class, () -> Array.getInt(new Integer[] { 1 }, 0));
        assertThrows(ArrayIndexOutOfBoundsException.class, () -> Array.getInt(new int[] { 1 }, 1));
    }

    @Test
    public void testGetLong() {
        assertEquals(2L, Array.getLong(new long[] { 1L, 2L, 3L }, 1));
        assertEquals(2L, Array.getLong(new int[] { 1, 2, 3 }, 1));
        assertThrows(NullPointerException.class, () -> Array.getLong(null, 0));
        assertThrows(IllegalArgumentException.class, () -> Array.getLong(new Long[] { 1L }, 0));
        assertThrows(ArrayIndexOutOfBoundsException.class, () -> Array.getLong(new long[] { 1L }, 1));
    }

    @Test
    public void testGetFloat() {
        assertEquals(2.2f, Array.getFloat(new float[] { 1.1f, 2.2f, 3.3f }, 1));
        assertEquals(2f, Array.getFloat(new int[] { 1, 2, 3 }, 1));
        assertThrows(NullPointerException.class, () -> Array.getFloat(null, 0));
        assertThrows(IllegalArgumentException.class, () -> Array.getFloat(new Float[] { 1f }, 0));
        assertThrows(ArrayIndexOutOfBoundsException.class, () -> Array.getFloat(new float[] { 1f }, 1));
    }

    @Test
    public void testGetDouble() {
        assertEquals(2.2, Array.getDouble(new double[] { 1.1, 2.2, 3.3 }, 1));
        assertEquals(2.0, Array.getDouble(new int[] { 1, 2, 3 }, 1));
        assertThrows(NullPointerException.class, () -> Array.getDouble(null, 0));
        assertThrows(IllegalArgumentException.class, () -> Array.getDouble(new Double[] { 1d }, 0));
        assertThrows(ArrayIndexOutOfBoundsException.class, () -> Array.getDouble(new double[] { 1d }, 1));
    }
}

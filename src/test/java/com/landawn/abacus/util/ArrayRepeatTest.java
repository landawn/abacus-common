package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class ArrayRepeatTest extends ArrayTestSupport {

    @Test
    public void testRepeat_boolean() {
        assertArrayEquals(new boolean[] { true, true, true }, Array.repeat(true, 3));
        assertEquals(0, Array.repeat(true, 0).length);
        assertThrows(IllegalArgumentException.class, () -> Array.repeat(true, -1));

        assertArrayEquals(new boolean[] { true, false, true, false }, Array.repeat(new boolean[] { true, false }, 2));
        assertEquals(0, Array.repeat(new boolean[0], 10).length);
        assertEquals(0, Array.repeat((boolean[]) null, 3).length);
        assertThrows(IllegalArgumentException.class, () -> Array.repeat(new boolean[] { true }, -1));
    }

    @Test
    public void testRepeat_char() {
        assertArrayEquals(new char[] { 'a', 'a', 'a' }, Array.repeat('a', 3));
        assertEquals(0, Array.repeat('a', 0).length);
        assertArrayEquals(new char[] { 'a', 'b', 'a', 'b' }, Array.repeat(new char[] { 'a', 'b' }, 2));
        assertEquals(0, Array.repeat((char[]) null, 2).length);
        assertThrows(IllegalArgumentException.class, () -> Array.repeat('a', -1));
    }

    @Test
    public void testRepeat_byte() {
        assertArrayEquals(new byte[] { 1, 1, 1 }, Array.repeat((byte) 1, 3));
        assertArrayEquals(new byte[] { 1, 2, 1, 2 }, Array.repeat(new byte[] { 1, 2 }, 2));
        assertEquals(0, Array.repeat((byte[]) null, 2).length);
        assertThrows(IllegalArgumentException.class, () -> Array.repeat((byte) 1, -1));
    }

    @Test
    public void testRepeat_short() {
        assertArrayEquals(new short[] { 1, 1, 1 }, Array.repeat((short) 1, 3));
        assertArrayEquals(new short[] { 1, 2, 1, 2 }, Array.repeat(new short[] { 1, 2 }, 2));
        assertThrows(IllegalArgumentException.class, () -> Array.repeat((short) 1, -1));
    }

    @Test
    public void testRepeat_int() {
        assertArrayEquals(new int[] { 7, 7, 7 }, Array.repeat(7, 3));
        assertEquals(0, Array.repeat(7, 0).length);
        assertArrayEquals(new int[] { 1, 2, 1, 2 }, Array.repeat(new int[] { 1, 2 }, 2));
        assertEquals(0, Array.repeat((int[]) null, 3).length);
        assertThrows(IllegalArgumentException.class, () -> Array.repeat(1, -1));
        assertTrue(assertThrows(IllegalArgumentException.class, () -> Array.repeat(new int[] { 1, 2 }, Integer.MAX_VALUE)).getMessage().contains("4294967294"));
    }

    @Test
    public void testRepeat_long() {
        assertArrayEquals(new long[] { 7L, 7L }, Array.repeat(7L, 2));
        assertArrayEquals(new long[] { 1L, 2L, 1L, 2L }, Array.repeat(new long[] { 1L, 2L }, 2));
        assertThrows(IllegalArgumentException.class, () -> Array.repeat(1L, -1));
    }

    @Test
    public void testRepeat_float() {
        assertArrayEquals(new float[] { 1.5f, 1.5f }, Array.repeat(1.5f, 2));
        assertArrayEquals(new float[] { 1f, 2f, 1f, 2f }, Array.repeat(new float[] { 1f, 2f }, 2));
        assertThrows(IllegalArgumentException.class, () -> Array.repeat(1f, -1));
    }

    @Test
    public void testRepeat_double() {
        assertArrayEquals(new double[] { 1.5, 1.5 }, Array.repeat(1.5, 2));
        assertArrayEquals(new double[] { 1d, 2d, 1d, 2d }, Array.repeat(new double[] { 1d, 2d }, 2));
        assertThrows(IllegalArgumentException.class, () -> Array.repeat(1d, -1));
    }

    @Test
    public void testRepeat_String() {
        assertArrayEquals(new String[] { "x", "x", "x" }, Array.repeat("x", 3));
        assertArrayEquals(new String[] { null, null }, Array.repeat((String) null, 2));
        assertArrayEquals(new String[] { "a", "b", "a", "b" }, Array.repeat(new String[] { "a", "b" }, 2));
        assertEquals(0, Array.repeat((String[]) null, 2).length);
        assertThrows(IllegalArgumentException.class, () -> Array.repeat("x", -1));
    }

    @Test
    public void testRepeat_Object() {
        assertArrayEquals(new Integer[] { 1, 1 }, Array.repeat(Integer.valueOf(1), 2, Integer.class));
        assertArrayEquals(new Number[] { 1, 1 }, Array.repeat(Integer.valueOf(1), 2, Number.class));
        assertArrayEquals(new Integer[] { 1, 2, 1, 2 }, Array.repeat(new Integer[] { 1, 2 }, 2, Integer.class));
        assertThrows(IllegalArgumentException.class, () -> Array.repeat("x", -1, String.class));
        assertThrows(IllegalArgumentException.class, () -> Array.repeat("x", 2, (Class<String>) null));
        assertThrows(IllegalArgumentException.class, () -> Array.repeat(new String[] { "a" }, 2, null));
        assertTrue(assertThrows(IllegalArgumentException.class, () -> Array.repeat(new String[] { "a", "b" }, Integer.MAX_VALUE)).getMessage()
                .startsWith("Overflow. Array size is too large"));
    }

    @Test
    public void testRepeatNonNull() {
        assertArrayEquals(new Integer[] { 42, 42, 42 }, Array.repeatNonNull(42, 3));
        assertEquals(0, Array.repeatNonNull("x", 0).length);
        final String s = "same";
        final String[] repeated = Array.repeatNonNull(s, 3);
        assertSame(s, repeated[0]);
        assertSame(s, repeated[2]);
        assertThrows(IllegalArgumentException.class, () -> Array.repeatNonNull(null, 2));
        assertThrows(IllegalArgumentException.class, () -> Array.repeatNonNull("x", -1));
    }

    @Test
    public void testRepeat_MistypedElementClass() {
        // Contract pin for the @throws ArrayStoreException added to both repeat(.., Class) overloads, including
        // the asymmetry the clause calls out: nothing is stored when the result would be empty, so the same
        // mistyped call is fatal for n > 0 and silently accepted for n == 0.
        assertThrows(ArrayStoreException.class, () -> Array.<Number> repeat(Double.valueOf(1.5), 3, Integer.class));
        assertThrows(ArrayStoreException.class, () -> Array.<Number> repeat(new Number[] { Double.valueOf(1.5) }, 2, Integer.class));

        final Number[] fromElement = Array.<Number> repeat(Double.valueOf(1.5), 0, Integer.class);
        assertEquals(0, fromElement.length);
        assertEquals(Integer.class, fromElement.getClass().getComponentType());

        assertEquals(0, Array.<Number> repeat(new Number[] { Double.valueOf(1.5) }, 0, Integer.class).length);
        assertEquals(0, Array.<Number> repeat(new Number[0], 3, Integer.class).length);
        assertEquals(0, Array.<Number> repeat((Number[]) null, 3, Integer.class).length);

        // a null element is storable in any object array
        assertArrayEquals(new Integer[] { null, null }, Array.<Number> repeat((Number) null, 2, Integer.class));
    }
}

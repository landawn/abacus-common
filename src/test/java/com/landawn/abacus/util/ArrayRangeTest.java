package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class ArrayRangeTest extends ArrayTestSupport {

    @Test
    public void testRange_char() {
        assertArrayEquals(new char[] { 'a', 'b', 'c', 'd' }, Array.range('a', 'e'));
        assertArrayEquals(new char[0], Array.range('a', 'a'));
        assertArrayEquals(new char[0], Array.range('z', 'a'));
        assertArrayEquals(new char[] { 'a', 'c', 'e' }, Array.range('a', 'f', 2));
        assertArrayEquals(new char[] { 'g', 'e', 'c' }, Array.range('g', 'a', -2));
        assertArrayEquals(new char[0], Array.range('a', 'e', -1));
        assertThrows(IllegalArgumentException.class, () -> Array.range('a', 'e', 0));
    }

    @Test
    public void testRange_byte() {
        assertArrayEquals(new byte[] { 1, 2, 3, 4 }, Array.range((byte) 1, (byte) 5));
        assertArrayEquals(new byte[0], Array.range((byte) 5, (byte) 5));
        assertArrayEquals(new byte[0], Array.range((byte) 5, (byte) 1));
        assertArrayEquals(new byte[] { 1, 3, 5 }, Array.range((byte) 1, (byte) 6, (byte) 2));
        assertArrayEquals(new byte[] { 5, 3, 1 }, Array.range((byte) 5, (byte) 0, (byte) -2));
        assertThrows(IllegalArgumentException.class, () -> Array.range((byte) 1, (byte) 5, (byte) 0));
    }

    @Test
    public void testRange_short() {
        assertArrayEquals(new short[] { 1, 2, 3, 4 }, Array.range((short) 1, (short) 5));
        assertArrayEquals(new short[0], Array.range((short) 5, (short) 5));
        assertArrayEquals(new short[] { 1, 3, 5 }, Array.range((short) 1, (short) 6, (short) 2));
        assertArrayEquals(new short[0], Array.range((short) 1, (short) 5, (short) -1));
        assertThrows(IllegalArgumentException.class, () -> Array.range((short) 1, (short) 5, (short) 0));
    }

    @Test
    public void testRange_int() {
        assertArrayEquals(new int[] { 0, 1, 2, 3, 4 }, Array.range(0, 5));
        assertArrayEquals(new int[0], Array.range(5, 5));
        assertArrayEquals(new int[0], Array.range(5, 0));
        assertArrayEquals(new int[] { -2, -1, 0, 1 }, Array.range(-2, 2));
        assertArrayEquals(new int[] { 0, 2, 4 }, Array.range(0, 5, 2));
        assertArrayEquals(new int[] { 5, 3, 1 }, Array.range(5, 0, -2));
        assertArrayEquals(new int[0], Array.range(0, 5, -1));
        assertThrows(IllegalArgumentException.class, () -> Array.range(0, 5, 0));
        assertTrue(assertThrows(IllegalArgumentException.class, () -> Array.range(Integer.MIN_VALUE, Integer.MAX_VALUE)).getMessage().contains("4294967295"));
    }

    @Test
    public void testRange_long() {
        assertArrayEquals(new long[] { 0L, 1L, 2L }, Array.range(0L, 3L));
        assertArrayEquals(new long[0], Array.range(3L, 3L));
        assertArrayEquals(new long[] { 0L, 2L, 4L }, Array.range(0L, 5L, 2L));
        assertArrayEquals(new long[] { 5L, 3L, 1L }, Array.range(5L, 0L, -2L));
        assertThrows(IllegalArgumentException.class, () -> Array.range(0L, 5L, 0L));
        assertEquals("Overflow. Array size is too large to allocate: the range [0, 3000000000) exceeds Integer.MAX_VALUE",
                assertThrows(IllegalArgumentException.class, () -> Array.range(0L, 3_000_000_000L)).getMessage());
    }

    @Test
    public void testRangeClosed_char() {
        assertArrayEquals(new char[] { 'a', 'b', 'c' }, Array.rangeClosed('a', 'c'));
        assertArrayEquals(new char[] { 'a' }, Array.rangeClosed('a', 'a'));
        assertArrayEquals(new char[0], Array.rangeClosed('c', 'a'));
        assertArrayEquals(new char[] { 'a', 'c', 'e' }, Array.rangeClosed('a', 'e', 2));
        assertThrows(IllegalArgumentException.class, () -> Array.rangeClosed('a', 'e', 0));
    }

    @Test
    public void testRangeClosed_byte() {
        assertArrayEquals(new byte[] { 1, 2, 3 }, Array.rangeClosed((byte) 1, (byte) 3));
        assertArrayEquals(new byte[] { 7 }, Array.rangeClosed((byte) 7, (byte) 7));
        assertArrayEquals(new byte[0], Array.rangeClosed((byte) 5, (byte) 1));
        assertArrayEquals(new byte[] { 1, 3, 5 }, Array.rangeClosed((byte) 1, (byte) 5, (byte) 2));
        assertThrows(IllegalArgumentException.class, () -> Array.rangeClosed((byte) 1, (byte) 5, (byte) 0));
    }

    @Test
    public void testRangeClosed_short() {
        assertArrayEquals(new short[] { 1, 2, 3 }, Array.rangeClosed((short) 1, (short) 3));
        assertArrayEquals(new short[] { 7 }, Array.rangeClosed((short) 7, (short) 7));
        assertArrayEquals(new short[0], Array.rangeClosed((short) 5, (short) 0));
        assertArrayEquals(new short[] { 1, 3, 5 }, Array.rangeClosed((short) 1, (short) 5, (short) 2));
        assertThrows(IllegalArgumentException.class, () -> Array.rangeClosed((short) 1, (short) 5, (short) 0));
    }

    @Test
    public void testRangeClosed_int() {
        assertArrayEquals(new int[] { 0, 1, 2, 3, 4, 5 }, Array.rangeClosed(0, 5));
        assertArrayEquals(new int[] { 7 }, Array.rangeClosed(7, 7));
        assertArrayEquals(new int[0], Array.rangeClosed(5, 0));
        assertArrayEquals(new int[] { -2, -1, 0, 1, 2 }, Array.rangeClosed(-2, 2));
        assertArrayEquals(new int[] { 0, 2, 4 }, Array.rangeClosed(0, 4, 2));
        assertArrayEquals(new int[] { 5, 3, 1 }, Array.rangeClosed(5, 1, -2));
        assertArrayEquals(new int[] { Integer.MAX_VALUE }, Array.rangeClosed(Integer.MAX_VALUE, Integer.MAX_VALUE));
        assertArrayEquals(new int[] { Integer.MIN_VALUE }, Array.rangeClosed(Integer.MIN_VALUE, Integer.MIN_VALUE));
        assertThrows(IllegalArgumentException.class, () -> Array.rangeClosed(0, 5, 0));
        assertTrue(assertThrows(IllegalArgumentException.class, () -> Array.rangeClosed(0, Integer.MAX_VALUE)).getMessage().contains("2147483648"));
    }

    @Test
    public void testRangeClosed_long() {
        assertArrayEquals(new long[] { 0L, 1L, 2L }, Array.rangeClosed(0L, 2L));
        assertArrayEquals(new long[] { 7L }, Array.rangeClosed(7L, 7L));
        assertArrayEquals(new long[0], Array.rangeClosed(5L, 0L));
        assertArrayEquals(new long[] { 0L, 2L, 4L }, Array.rangeClosed(0L, 4L, 2L));
        assertArrayEquals(new long[] { Long.MAX_VALUE - 2, Long.MAX_VALUE - 1, Long.MAX_VALUE }, Array.rangeClosed(Long.MAX_VALUE - 2, Long.MAX_VALUE, 1L));
        assertThrows(IllegalArgumentException.class, () -> Array.rangeClosed(0L, 5L, 0L));
        assertEquals("Overflow. Array size is too large to allocate: the range [0, 3000000000] exceeds Integer.MAX_VALUE",
                assertThrows(IllegalArgumentException.class, () -> Array.rangeClosed(0L, 3_000_000_000L)).getMessage());
        assertThrows(IllegalArgumentException.class, () -> Array.rangeClosed(0L, Long.MAX_VALUE, 1L));
        assertThrows(IllegalArgumentException.class, () -> Array.rangeClosed(0L, Long.MIN_VALUE + 1, -1L));
        assertThrows(IllegalArgumentException.class, () -> Array.rangeClosed(0L, Long.MAX_VALUE, 2L));
    }
}

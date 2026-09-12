package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.time.LocalDate;
import java.util.Calendar;
import java.util.Date;

import org.junit.jupiter.api.Test;

public class ArrayOfTest extends ArrayTestSupport {

    @Test
    public void testOf_boolean() {
        final boolean[] a = { true, false };
        assertSame(a, Array.of(a));
        assertArrayEquals(new boolean[] { true, false, true }, Array.of(true, false, true));
        assertNull(Array.of((boolean[]) null));
        assertEquals(0, Array.of(new boolean[0]).length);
    }

    @Test
    public void testOf_char() {
        final char[] a = { 'a', 'b' };
        assertSame(a, Array.of(a));
        assertArrayEquals(new char[] { 'a', 'e', 'i' }, Array.of('a', 'e', 'i'));
        assertNull(Array.of((char[]) null));
    }

    @Test
    public void testOf_byte() {
        final byte[] a = { 1, 2 };
        assertSame(a, Array.of(a));
        assertArrayEquals(new byte[] { 1, 2, 3 }, Array.of((byte) 1, (byte) 2, (byte) 3));
        assertNull(Array.of((byte[]) null));
    }

    @Test
    public void testOf_short() {
        final short[] a = { 1, 2 };
        assertSame(a, Array.of(a));
        assertArrayEquals(new short[] { 10, 20 }, Array.of((short) 10, (short) 20));
        assertNull(Array.of((short[]) null));
    }

    @Test
    public void testOf_int() {
        final int[] a = { 1, 2, 3 };
        assertSame(a, Array.of(a));
        assertArrayEquals(new int[] { 2, 3, 5 }, Array.of(2, 3, 5));
        assertNull(Array.of((int[]) null));
    }

    @Test
    public void testOf_long() {
        final long[] a = { 1L, 2L };
        assertSame(a, Array.of(a));
        assertArrayEquals(new long[] { 1L, 2L, 3L }, Array.of(1L, 2L, 3L));
        assertNull(Array.of((long[]) null));
    }

    @Test
    public void testOf_float() {
        final float[] a = { 1.1f, 2.2f };
        assertSame(a, Array.of(a));
        assertArrayEquals(new float[] { 1.1f, 2.2f }, Array.of(1.1f, 2.2f));
        assertNull(Array.of((float[]) null));
    }

    @Test
    public void testOf_double() {
        final double[] a = { 1.1, 2.2 };
        assertSame(a, Array.of(a));
        assertArrayEquals(new double[] { 19.99, 29.99 }, Array.of(19.99, 29.99));
        assertNull(Array.of((double[]) null));
    }

    @Test
    public void testOf_CharSequence() {
        final String[] a = { "a", "b" };
        assertSame(a, Array.of(a));
        assertArrayEquals(new String[] { "apple", "banana" }, Array.of("apple", "banana"));
        assertNull(Array.of((String[]) null));
        final CharSequence[] cs = Array.of((CharSequence) "x", new StringBuilder("y"));
        assertEquals(2, cs.length);
    }

    @Test
    public void testOf_Date() {
        final Date d1 = new Date(1);
        final Date d2 = new Date(2);
        final Date[] a = { d1, d2 };
        assertSame(a, Array.of(a));
        assertArrayEquals(new Date[] { d1, d2 }, Array.of(d1, d2));
        assertNull(Array.of((Date[]) null));
    }

    @Test
    public void testOf_Calendar() {
        final Calendar c1 = Calendar.getInstance();
        final Calendar c2 = Calendar.getInstance();
        final Calendar[] a = { c1, c2 };
        assertSame(a, Array.of(a));
        assertArrayEquals(new Calendar[] { c1, c2 }, Array.of(c1, c2));
        assertNull(Array.of((Calendar[]) null));
    }

    @Test
    public void testOf_Temporal() {
        final LocalDate d1 = LocalDate.of(2020, 1, 1);
        final LocalDate d2 = LocalDate.of(2020, 1, 2);
        final LocalDate[] a = { d1, d2 };
        assertSame(a, Array.of(a));
        assertArrayEquals(new LocalDate[] { d1, d2 }, Array.of(d1, d2));
        assertNull(Array.of((LocalDate[]) null));
    }

    @Test
    public void testOf_Enum() {
        final java.time.DayOfWeek[] a = { java.time.DayOfWeek.MONDAY, java.time.DayOfWeek.TUESDAY };
        assertSame(a, Array.of(a));
        assertArrayEquals(new java.time.DayOfWeek[] { java.time.DayOfWeek.MONDAY, java.time.DayOfWeek.FRIDAY },
                Array.of(java.time.DayOfWeek.MONDAY, java.time.DayOfWeek.FRIDAY));
        assertNull(Array.of((java.time.DayOfWeek[]) null));
    }
}

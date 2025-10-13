package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

@Tag("2025")
public class Array2025Test extends TestBase {

    @Test
    public void test_newInstance_withLength() {
        Integer[] arr = Array.newInstance(Integer.class, 5);
        assertNotNull(arr);
        assertEquals(5, arr.length);

        String[] strArr = Array.newInstance(String.class, 0);
        assertNotNull(strArr);
        assertEquals(0, strArr.length);

        String[] strArr2 = Array.newInstance(String.class, 0);
        assertSame(strArr, strArr2);

        assertThrows(NegativeArraySizeException.class, () -> Array.newInstance(Integer.class, -1));
        assertThrows(Exception.class, () -> Array.newInstance(null, 5));
    }

    @Test
    public void test_newInstance_withDimensions() {
        Integer[][] arr2D = Array.newInstance(Integer.class, 3, 4);
        assertNotNull(arr2D);
        assertEquals(3, arr2D.length);
        assertEquals(4, arr2D[0].length);

        Integer[][][] arr3D = Array.newInstance(Integer.class, 2, 3, 4);
        assertNotNull(arr3D);
        assertEquals(2, arr3D.length);
        assertEquals(3, arr3D[0].length);
        assertEquals(4, arr3D[0][0].length);

        assertThrows(NegativeArraySizeException.class, () -> Array.newInstance(Integer.class, 3, -1));
    }

    @Test
    public void test_getLength() {
        assertEquals(0, Array.getLength(null));
        assertEquals(5, Array.getLength(new int[5]));
        assertEquals(3, Array.getLength(new String[3]));
        assertEquals(0, Array.getLength(new boolean[0]));

        assertThrows(IllegalArgumentException.class, () -> Array.getLength("not an array"));
    }

    @Test
    public void test_get() {
        Integer[] arr = { 1, 2, 3 };
        assertEquals(1, (Integer) Array.get(arr, 0));
        assertEquals(2, (Integer) Array.get(arr, 1));
        assertEquals(3, (Integer) Array.get(arr, 2));

        assertThrows(ArrayIndexOutOfBoundsException.class, () -> Array.get(arr, 3));
        assertThrows(ArrayIndexOutOfBoundsException.class, () -> Array.get(arr, -1));
    }

    @Test
    public void test_getBoolean() {
        boolean[] arr = { true, false, true };
        assertTrue(Array.getBoolean(arr, 0));
        assertFalse(Array.getBoolean(arr, 1));
        assertTrue(Array.getBoolean(arr, 2));

        assertThrows(ArrayIndexOutOfBoundsException.class, () -> Array.getBoolean(arr, 3));
    }

    @Test
    public void test_getByte() {
        byte[] arr = { 1, 2, 3 };
        assertEquals(1, Array.getByte(arr, 0));
        assertEquals(2, Array.getByte(arr, 1));
        assertEquals(3, Array.getByte(arr, 2));

        assertThrows(ArrayIndexOutOfBoundsException.class, () -> Array.getByte(arr, -1));
    }

    @Test
    public void test_getChar() {
        char[] arr = { 'a', 'b', 'c' };
        assertEquals('a', Array.getChar(arr, 0));
        assertEquals('b', Array.getChar(arr, 1));
        assertEquals('c', Array.getChar(arr, 2));

        assertThrows(ArrayIndexOutOfBoundsException.class, () -> Array.getChar(arr, 3));
    }

    @Test
    public void test_getShort() {
        short[] arr = { 10, 20, 30 };
        assertEquals(10, Array.getShort(arr, 0));
        assertEquals(20, Array.getShort(arr, 1));
        assertEquals(30, Array.getShort(arr, 2));

        assertThrows(ArrayIndexOutOfBoundsException.class, () -> Array.getShort(arr, 3));
    }

    @Test
    public void test_getInt() {
        int[] arr = { 100, 200, 300 };
        assertEquals(100, Array.getInt(arr, 0));
        assertEquals(200, Array.getInt(arr, 1));
        assertEquals(300, Array.getInt(arr, 2));

        assertThrows(ArrayIndexOutOfBoundsException.class, () -> Array.getInt(arr, 3));
    }

    @Test
    public void test_getLong() {
        long[] arr = { 1000L, 2000L, 3000L };
        assertEquals(1000L, Array.getLong(arr, 0));
        assertEquals(2000L, Array.getLong(arr, 1));
        assertEquals(3000L, Array.getLong(arr, 2));

        assertThrows(ArrayIndexOutOfBoundsException.class, () -> Array.getLong(arr, 3));
    }

    @Test
    public void test_getFloat() {
        float[] arr = { 1.1f, 2.2f, 3.3f };
        assertEquals(1.1f, Array.getFloat(arr, 0), 0.001);
        assertEquals(2.2f, Array.getFloat(arr, 1), 0.001);
        assertEquals(3.3f, Array.getFloat(arr, 2), 0.001);

        assertThrows(ArrayIndexOutOfBoundsException.class, () -> Array.getFloat(arr, 3));
    }

    @Test
    public void test_getDouble() {
        double[] arr = { 1.1, 2.2, 3.3 };
        assertEquals(1.1, Array.getDouble(arr, 0), 0.001);
        assertEquals(2.2, Array.getDouble(arr, 1), 0.001);
        assertEquals(3.3, Array.getDouble(arr, 2), 0.001);

        assertThrows(ArrayIndexOutOfBoundsException.class, () -> Array.getDouble(arr, 3));
    }

    @Test
    public void test_set() {
        Integer[] arr = { 1, 2, 3 };
        Array.set(arr, 0, 10);
        Array.set(arr, 1, 20);
        Array.set(arr, 2, 30);

        assertEquals(10, arr[0]);
        assertEquals(20, arr[1]);
        assertEquals(30, arr[2]);

        assertThrows(ArrayIndexOutOfBoundsException.class, () -> Array.set(arr, 3, 40));
    }

    @Test
    public void test_setBoolean() {
        boolean[] arr = { true, true, true };
        Array.setBoolean(arr, 0, false);
        Array.setBoolean(arr, 1, false);

        assertFalse(arr[0]);
        assertFalse(arr[1]);
        assertTrue(arr[2]);

        assertThrows(ArrayIndexOutOfBoundsException.class, () -> Array.setBoolean(arr, 3, false));
    }

    @Test
    public void test_setByte() {
        byte[] arr = { 1, 2, 3 };
        Array.setByte(arr, 0, (byte) 10);
        Array.setByte(arr, 1, (byte) 20);

        assertEquals(10, arr[0]);
        assertEquals(20, arr[1]);
        assertEquals(3, arr[2]);

        assertThrows(ArrayIndexOutOfBoundsException.class, () -> Array.setByte(arr, 3, (byte) 40));
    }

    @Test
    public void test_setChar() {
        char[] arr = { 'a', 'b', 'c' };
        Array.setChar(arr, 0, 'x');
        Array.setChar(arr, 1, 'y');

        assertEquals('x', arr[0]);
        assertEquals('y', arr[1]);
        assertEquals('c', arr[2]);

        assertThrows(ArrayIndexOutOfBoundsException.class, () -> Array.setChar(arr, 3, 'z'));
    }

    @Test
    public void test_setShort() {
        short[] arr = { 1, 2, 3 };
        Array.setShort(arr, 0, (short) 10);
        Array.setShort(arr, 1, (short) 20);

        assertEquals(10, arr[0]);
        assertEquals(20, arr[1]);
        assertEquals(3, arr[2]);

        assertThrows(ArrayIndexOutOfBoundsException.class, () -> Array.setShort(arr, 3, (short) 40));
    }

    @Test
    public void test_setInt() {
        int[] arr = { 1, 2, 3 };
        Array.setInt(arr, 0, 10);
        Array.setInt(arr, 1, 20);

        assertEquals(10, arr[0]);
        assertEquals(20, arr[1]);
        assertEquals(3, arr[2]);

        assertThrows(ArrayIndexOutOfBoundsException.class, () -> Array.setInt(arr, 3, 40));
    }

    @Test
    public void test_setLong() {
        long[] arr = { 1L, 2L, 3L };
        Array.setLong(arr, 0, 10L);
        Array.setLong(arr, 1, 20L);

        assertEquals(10L, arr[0]);
        assertEquals(20L, arr[1]);
        assertEquals(3L, arr[2]);

        assertThrows(ArrayIndexOutOfBoundsException.class, () -> Array.setLong(arr, 3, 40L));
    }

    @Test
    public void test_setFloat() {
        float[] arr = { 1.1f, 2.2f, 3.3f };
        Array.setFloat(arr, 0, 10.5f);
        Array.setFloat(arr, 1, 20.5f);

        assertEquals(10.5f, arr[0], 0.001);
        assertEquals(20.5f, arr[1], 0.001);
        assertEquals(3.3f, arr[2], 0.001);

        assertThrows(ArrayIndexOutOfBoundsException.class, () -> Array.setFloat(arr, 3, 40.5f));
    }

    @Test
    public void test_setDouble() {
        double[] arr = { 1.1, 2.2, 3.3 };
        Array.setDouble(arr, 0, 10.5);
        Array.setDouble(arr, 1, 20.5);

        assertEquals(10.5, arr[0], 0.001);
        assertEquals(20.5, arr[1], 0.001);
        assertEquals(3.3, arr[2], 0.001);

        assertThrows(ArrayIndexOutOfBoundsException.class, () -> Array.setDouble(arr, 3, 40.5));
    }

    @Test
    public void test_asList() {
        String[] arr = { "a", "b", "c" };
        List<String> list = Array.asList(arr);
        assertNotNull(list);
        assertEquals(3, list.size());
        assertEquals("a", list.get(0));
        assertEquals("b", list.get(1));
        assertEquals("c", list.get(2));

        List<String> nullList = Array.asList((String[]) null);
        assertNotNull(nullList);
        assertEquals(0, nullList.size());

        List<String> emptyList = Array.asList(new String[0]);
        assertNotNull(emptyList);
        assertEquals(0, emptyList.size());
    }

    @Test
    public void test_of_boolean() {
        boolean[] arr = Array.of(true, false, true);
        assertNotNull(arr);
        assertEquals(3, arr.length);
        assertTrue(arr[0]);
        assertFalse(arr[1]);
        assertTrue(arr[2]);

        boolean[] empty = Array.of(new boolean[0]);
        assertNotNull(empty);
        assertEquals(0, empty.length);
    }

    @Test
    public void test_of_char() {
        char[] arr = Array.of('a', 'b', 'c');
        assertNotNull(arr);
        assertEquals(3, arr.length);
        assertEquals('a', arr[0]);
        assertEquals('b', arr[1]);
        assertEquals('c', arr[2]);
    }

    @Test
    public void test_of_byte() {
        byte[] arr = Array.of((byte) 1, (byte) 2, (byte) 3);
        assertNotNull(arr);
        assertEquals(3, arr.length);
        assertEquals(1, arr[0]);
        assertEquals(2, arr[1]);
        assertEquals(3, arr[2]);
    }

    @Test
    public void test_of_short() {
        short[] arr = Array.of((short) 10, (short) 20, (short) 30);
        assertNotNull(arr);
        assertEquals(3, arr.length);
        assertEquals(10, arr[0]);
        assertEquals(20, arr[1]);
        assertEquals(30, arr[2]);
    }

    @Test
    public void test_of_int() {
        int[] arr = Array.of(100, 200, 300);
        assertNotNull(arr);
        assertEquals(3, arr.length);
        assertEquals(100, arr[0]);
        assertEquals(200, arr[1]);
        assertEquals(300, arr[2]);
    }

    @Test
    public void test_of_long() {
        long[] arr = Array.of(1000L, 2000L, 3000L);
        assertNotNull(arr);
        assertEquals(3, arr.length);
        assertEquals(1000L, arr[0]);
        assertEquals(2000L, arr[1]);
        assertEquals(3000L, arr[2]);
    }

    @Test
    public void test_of_float() {
        float[] arr = Array.of(1.1f, 2.2f, 3.3f);
        assertNotNull(arr);
        assertEquals(3, arr.length);
        assertEquals(1.1f, arr[0], 0.001);
        assertEquals(2.2f, arr[1], 0.001);
        assertEquals(3.3f, arr[2], 0.001);
    }

    @Test
    public void test_of_double() {
        double[] arr = Array.of(1.1, 2.2, 3.3);
        assertNotNull(arr);
        assertEquals(3, arr.length);
        assertEquals(1.1, arr[0], 0.001);
        assertEquals(2.2, arr[1], 0.001);
        assertEquals(3.3, arr[2], 0.001);
    }

    @Test
    public void test_of_String() {
        String[] arr = Array.of("a", "b", "c");
        assertNotNull(arr);
        assertEquals(3, arr.length);
        assertEquals("a", arr[0]);
        assertEquals("b", arr[1]);
        assertEquals("c", arr[2]);
    }

    @Test
    public void test_of_Date() {
        Date date1 = new Date();
        Date date2 = new Date();
        Date[] arr = Array.of(date1, date2);
        assertNotNull(arr);
        assertEquals(2, arr.length);
        assertSame(date1, arr[0]);
        assertSame(date2, arr[1]);
    }

    @Test
    public void test_of_Calendar() {
        Calendar cal1 = Calendar.getInstance();
        Calendar cal2 = Calendar.getInstance();
        Calendar[] arr = Array.of(cal1, cal2);
        assertNotNull(arr);
        assertEquals(2, arr.length);
        assertSame(cal1, arr[0]);
        assertSame(cal2, arr[1]);
    }

    @Test
    public void test_of_Temporal() {
        LocalDate date1 = LocalDate.now();
        LocalDate date2 = LocalDate.now().plusDays(1);
        LocalDate[] arr = Array.of(date1, date2);
        assertNotNull(arr);
        assertEquals(2, arr.length);
        assertSame(date1, arr[0]);
        assertSame(date2, arr[1]);
    }

    @Test
    public void test_of_Enum() {
        Thread.State[] arr = Array.of(Thread.State.NEW, Thread.State.RUNNABLE);
        assertNotNull(arr);
        assertEquals(2, arr.length);
        assertEquals(Thread.State.NEW, arr[0]);
        assertEquals(Thread.State.RUNNABLE, arr[1]);
    }

    @Test
    @SuppressWarnings("deprecation")
    public void test_oF() {
        Integer[] arr = Array.oF(1, 2, 3);
        assertNotNull(arr);
        assertEquals(3, arr.length);
        assertEquals(1, arr[0]);
        assertEquals(2, arr[1]);
        assertEquals(3, arr[2]);
    }

    @Test
    public void test_range_char() {
        char[] arr = Array.range('a', 'e');
        assertArrayEquals(new char[] { 'a', 'b', 'c', 'd' }, arr);

        char[] empty = Array.range('e', 'a');
        assertEquals(0, empty.length);

        char[] single = Array.range('a', 'a');
        assertEquals(0, single.length);
    }

    @Test
    public void test_range_byte() {
        byte[] arr = Array.range((byte) 1, (byte) 5);
        assertArrayEquals(new byte[] { 1, 2, 3, 4 }, arr);

        byte[] empty = Array.range((byte) 5, (byte) 1);
        assertEquals(0, empty.length);
    }

    @Test
    public void test_range_short() {
        short[] arr = Array.range((short) 1, (short) 5);
        assertArrayEquals(new short[] { 1, 2, 3, 4 }, arr);

        short[] empty = Array.range((short) 5, (short) 1);
        assertEquals(0, empty.length);
    }

    @Test
    public void test_range_char_withStep() {
        char[] arr = Array.range('a', 'g', 2);
        assertArrayEquals(new char[] { 'a', 'c', 'e' }, arr);

        char[] reverse = Array.range('g', 'a', -2);
        assertArrayEquals(new char[] { 'g', 'e', 'c' }, reverse);

        assertThrows(IllegalArgumentException.class, () -> Array.range('a', 'e', 0));

        char[] empty = Array.range('a', 'a', 1);
        assertEquals(0, empty.length);
    }

    @Test
    public void test_range_byte_withStep() {
        byte[] arr = Array.range((byte) 0, (byte) 10, (byte) 3);
        assertArrayEquals(new byte[] { 0, 3, 6, 9 }, arr);

        byte[] reverse = Array.range((byte) 10, (byte) 0, (byte) -3);
        assertArrayEquals(new byte[] { 10, 7, 4, 1 }, reverse);

        assertThrows(IllegalArgumentException.class, () -> Array.range((byte) 0, (byte) 10, (byte) 0));
    }

    @Test
    public void test_range_short_withStep() {
        short[] arr = Array.range((short) 0, (short) 10, (short) 3);
        assertArrayEquals(new short[] { 0, 3, 6, 9 }, arr);

        short[] reverse = Array.range((short) 10, (short) 0, (short) -3);
        assertArrayEquals(new short[] { 10, 7, 4, 1 }, reverse);

        assertThrows(IllegalArgumentException.class, () -> Array.range((short) 0, (short) 10, (short) 0));
    }

    @Test
    public void test_range_int_withStep() {
        int[] arr = Array.range(0, 10, 3);
        assertArrayEquals(new int[] { 0, 3, 6, 9 }, arr);

        int[] reverse = Array.range(10, 0, -3);
        assertArrayEquals(new int[] { 10, 7, 4, 1 }, reverse);

        assertThrows(IllegalArgumentException.class, () -> Array.range(0, 10, 0));

        int[] empty = Array.range(0, 0, 1);
        assertEquals(0, empty.length);
    }

    @Test
    public void test_range_long_withStep() {
        long[] arr = Array.range(0L, 10L, 3L);
        assertArrayEquals(new long[] { 0L, 3L, 6L, 9L }, arr);

        long[] reverse = Array.range(10L, 0L, -3L);
        assertArrayEquals(new long[] { 10L, 7L, 4L, 1L }, reverse);

        assertThrows(IllegalArgumentException.class, () -> Array.range(0L, 10L, 0L));
    }

    @Test
    public void test_rangeClosed_char() {
        char[] arr = Array.rangeClosed('a', 'e');
        assertArrayEquals(new char[] { 'a', 'b', 'c', 'd', 'e' }, arr);

        char[] empty = Array.rangeClosed('e', 'a');
        assertEquals(0, empty.length);

        char[] single = Array.rangeClosed('a', 'a');
        assertArrayEquals(new char[] { 'a' }, single);
    }

    @Test
    public void test_rangeClosed_byte() {
        byte[] arr = Array.rangeClosed((byte) 1, (byte) 5);
        assertArrayEquals(new byte[] { 1, 2, 3, 4, 5 }, arr);

        byte[] empty = Array.rangeClosed((byte) 5, (byte) 1);
        assertEquals(0, empty.length);

        byte[] single = Array.rangeClosed((byte) 3, (byte) 3);
        assertArrayEquals(new byte[] { 3 }, single);
    }

    @Test
    public void test_rangeClosed_short() {
        short[] arr = Array.rangeClosed((short) 1, (short) 5);
        assertArrayEquals(new short[] { 1, 2, 3, 4, 5 }, arr);

        short[] empty = Array.rangeClosed((short) 5, (short) 1);
        assertEquals(0, empty.length);

        short[] single = Array.rangeClosed((short) 3, (short) 3);
        assertArrayEquals(new short[] { 3 }, single);
    }

    @Test
    public void test_rangeClosed_int() {
        int[] arr = Array.rangeClosed(1, 5);
        assertArrayEquals(new int[] { 1, 2, 3, 4, 5 }, arr);

        int[] empty = Array.rangeClosed(5, 1);
        assertEquals(0, empty.length);

        int[] single = Array.rangeClosed(3, 3);
        assertArrayEquals(new int[] { 3 }, single);

        assertThrows(IllegalArgumentException.class, () -> Array.rangeClosed(0, Integer.MAX_VALUE));
    }

    @Test
    public void test_rangeClosed_long() {
        long[] arr = Array.rangeClosed(1L, 5L);
        assertArrayEquals(new long[] { 1L, 2L, 3L, 4L, 5L }, arr);

        long[] empty = Array.rangeClosed(5L, 1L);
        assertEquals(0, empty.length);

        long[] single = Array.rangeClosed(3L, 3L);
        assertArrayEquals(new long[] { 3L }, single);

        assertThrows(IllegalArgumentException.class, () -> Array.rangeClosed(0L, Long.MAX_VALUE));
    }

    @Test
    public void test_rangeClosed_char_withStep() {
        char[] arr = Array.rangeClosed('a', 'g', 2);
        assertArrayEquals(new char[] { 'a', 'c', 'e', 'g' }, arr);

        char[] reverse = Array.rangeClosed('g', 'a', -2);
        assertArrayEquals(new char[] { 'g', 'e', 'c', 'a' }, reverse);

        assertThrows(IllegalArgumentException.class, () -> Array.rangeClosed('a', 'e', 0));
    }

    @Test
    public void test_rangeClosed_byte_withStep() {
        byte[] arr = Array.rangeClosed((byte) 0, (byte) 10, (byte) 3);
        assertArrayEquals(new byte[] { 0, 3, 6, 9 }, arr);

        byte[] reverse = Array.rangeClosed((byte) 10, (byte) 0, (byte) -3);
        assertArrayEquals(new byte[] { 10, 7, 4, 1 }, reverse);

        assertThrows(IllegalArgumentException.class, () -> Array.rangeClosed((byte) 0, (byte) 10, (byte) 0));
    }

    @Test
    public void test_rangeClosed_short_withStep() {
        short[] arr = Array.rangeClosed((short) 0, (short) 10, (short) 3);
        assertArrayEquals(new short[] { 0, 3, 6, 9 }, arr);

        short[] reverse = Array.rangeClosed((short) 10, (short) 0, (short) -3);
        assertArrayEquals(new short[] { 10, 7, 4, 1 }, reverse);

        assertThrows(IllegalArgumentException.class, () -> Array.rangeClosed((short) 0, (short) 10, (short) 0));
    }

    @Test
    public void test_rangeClosed_int_withStep() {
        int[] arr = Array.rangeClosed(0, 10, 3);
        assertArrayEquals(new int[] { 0, 3, 6, 9 }, arr);

        int[] reverse = Array.rangeClosed(10, 0, -3);
        assertArrayEquals(new int[] { 10, 7, 4, 1 }, reverse);

        assertThrows(IllegalArgumentException.class, () -> Array.rangeClosed(0, 10, 0));
    }

    @Test
    public void test_rangeClosed_long_withStep() {
        long[] arr = Array.rangeClosed(0L, 10L, 3L);
        assertArrayEquals(new long[] { 0L, 3L, 6L, 9L }, arr);

        long[] reverse = Array.rangeClosed(10L, 0L, -3L);
        assertArrayEquals(new long[] { 10L, 7L, 4L, 1L }, reverse);

        assertThrows(IllegalArgumentException.class, () -> Array.rangeClosed(0L, 10L, 0L));
    }

    @Test
    public void test_repeat_boolean_element() {
        boolean[] arr = Array.repeat(true, 3);
        assertArrayEquals(new boolean[] { true, true, true }, arr);

        boolean[] empty = Array.repeat(false, 0);
        assertEquals(0, empty.length);

        assertThrows(IllegalArgumentException.class, () -> Array.repeat(true, -1));
    }

    @Test
    public void test_repeat_boolean_array() {
        boolean[] arr = Array.repeat(new boolean[] { true, false }, 3);
        assertArrayEquals(new boolean[] { true, false, true, false, true, false }, arr);

        boolean[] empty = Array.repeat(new boolean[] { true, false }, 0);
        assertEquals(0, empty.length);

        assertThrows(IllegalArgumentException.class, () -> Array.repeat(new boolean[] { true }, -1));
    }

    @Test
    public void test_repeat_char_element() {
        char[] arr = Array.repeat('a', 3);
        assertArrayEquals(new char[] { 'a', 'a', 'a' }, arr);

        char[] empty = Array.repeat('x', 0);
        assertEquals(0, empty.length);

        assertThrows(IllegalArgumentException.class, () -> Array.repeat('a', -1));
    }

    @Test
    public void test_repeat_char_array() {
        char[] arr = Array.repeat(new char[] { 'a', 'b' }, 3);
        assertArrayEquals(new char[] { 'a', 'b', 'a', 'b', 'a', 'b' }, arr);

        char[] empty = Array.repeat(new char[] { 'a' }, 0);
        assertEquals(0, empty.length);

        assertThrows(IllegalArgumentException.class, () -> Array.repeat(new char[] { 'a' }, -1));
    }

    @Test
    public void test_repeat_byte_element() {
        byte[] arr = Array.repeat((byte) 5, 3);
        assertArrayEquals(new byte[] { 5, 5, 5 }, arr);

        byte[] empty = Array.repeat((byte) 5, 0);
        assertEquals(0, empty.length);

        assertThrows(IllegalArgumentException.class, () -> Array.repeat((byte) 5, -1));
    }

    @Test
    public void test_repeat_byte_array() {
        byte[] arr = Array.repeat(new byte[] { 1, 2 }, 3);
        assertArrayEquals(new byte[] { 1, 2, 1, 2, 1, 2 }, arr);

        byte[] empty = Array.repeat(new byte[] { 1 }, 0);
        assertEquals(0, empty.length);

        assertThrows(IllegalArgumentException.class, () -> Array.repeat(new byte[] { 1 }, -1));
    }

    @Test
    public void test_repeat_short_element() {
        short[] arr = Array.repeat((short) 5, 3);
        assertArrayEquals(new short[] { 5, 5, 5 }, arr);

        short[] empty = Array.repeat((short) 5, 0);
        assertEquals(0, empty.length);

        assertThrows(IllegalArgumentException.class, () -> Array.repeat((short) 5, -1));
    }

    @Test
    public void test_repeat_short_array() {
        short[] arr = Array.repeat(new short[] { 1, 2 }, 3);
        assertArrayEquals(new short[] { 1, 2, 1, 2, 1, 2 }, arr);

        short[] empty = Array.repeat(new short[] { 1 }, 0);
        assertEquals(0, empty.length);

        assertThrows(IllegalArgumentException.class, () -> Array.repeat(new short[] { 1 }, -1));
    }

    @Test
    public void test_repeat_int_element() {
        int[] arr = Array.repeat(5, 3);
        assertArrayEquals(new int[] { 5, 5, 5 }, arr);

        int[] empty = Array.repeat(5, 0);
        assertEquals(0, empty.length);

        assertThrows(IllegalArgumentException.class, () -> Array.repeat(5, -1));
    }

    @Test
    public void test_repeat_int_array() {
        int[] arr = Array.repeat(new int[] { 1, 2 }, 3);
        assertArrayEquals(new int[] { 1, 2, 1, 2, 1, 2 }, arr);

        int[] empty = Array.repeat(new int[] { 1 }, 0);
        assertEquals(0, empty.length);

        assertThrows(IllegalArgumentException.class, () -> Array.repeat(new int[] { 1 }, -1));
    }

    @Test
    public void test_repeat_long_element() {
        long[] arr = Array.repeat(5L, 3);
        assertArrayEquals(new long[] { 5L, 5L, 5L }, arr);

        long[] empty = Array.repeat(5L, 0);
        assertEquals(0, empty.length);

        assertThrows(IllegalArgumentException.class, () -> Array.repeat(5L, -1));
    }

    @Test
    public void test_repeat_long_array() {
        long[] arr = Array.repeat(new long[] { 1L, 2L }, 3);
        assertArrayEquals(new long[] { 1L, 2L, 1L, 2L, 1L, 2L }, arr);

        long[] empty = Array.repeat(new long[] { 1L }, 0);
        assertEquals(0, empty.length);

        assertThrows(IllegalArgumentException.class, () -> Array.repeat(new long[] { 1L }, -1));
    }

    @Test
    public void test_repeat_float_element() {
        float[] arr = Array.repeat(5.5f, 3);
        assertArrayEquals(new float[] { 5.5f, 5.5f, 5.5f }, arr);

        float[] empty = Array.repeat(5.5f, 0);
        assertEquals(0, empty.length);

        assertThrows(IllegalArgumentException.class, () -> Array.repeat(5.5f, -1));
    }

    @Test
    public void test_repeat_float_array() {
        float[] arr = Array.repeat(new float[] { 1.1f, 2.2f }, 3);
        assertArrayEquals(new float[] { 1.1f, 2.2f, 1.1f, 2.2f, 1.1f, 2.2f }, arr);

        float[] empty = Array.repeat(new float[] { 1.1f }, 0);
        assertEquals(0, empty.length);

        assertThrows(IllegalArgumentException.class, () -> Array.repeat(new float[] { 1.1f }, -1));
    }

    @Test
    public void test_repeat_double_element() {
        double[] arr = Array.repeat(5.5, 3);
        assertArrayEquals(new double[] { 5.5, 5.5, 5.5 }, arr);

        double[] empty = Array.repeat(5.5, 0);
        assertEquals(0, empty.length);

        assertThrows(IllegalArgumentException.class, () -> Array.repeat(5.5, -1));
    }

    @Test
    public void test_repeat_double_array() {
        double[] arr = Array.repeat(new double[] { 1.1, 2.2 }, 3);
        assertArrayEquals(new double[] { 1.1, 2.2, 1.1, 2.2, 1.1, 2.2 }, arr);

        double[] empty = Array.repeat(new double[] { 1.1 }, 0);
        assertEquals(0, empty.length);

        assertThrows(IllegalArgumentException.class, () -> Array.repeat(new double[] { 1.1 }, -1));
    }

    @Test
    public void test_repeat_String_element() {
        String[] arr = Array.repeat("hello", 3);
        assertArrayEquals(new String[] { "hello", "hello", "hello" }, arr);

        String[] empty = Array.repeat("hello", 0);
        assertEquals(0, empty.length);

        assertThrows(IllegalArgumentException.class, () -> Array.repeat("hello", -1));
    }

    @Test
    public void test_repeat_String_array() {
        String[] arr = Array.repeat(new String[] { "a", "b" }, 3);
        assertArrayEquals(new String[] { "a", "b", "a", "b", "a", "b" }, arr);

        String[] empty = Array.repeat(new String[] { "a" }, 0);
        assertEquals(0, empty.length);

        assertThrows(IllegalArgumentException.class, () -> Array.repeat(new String[] { "a" }, -1));
    }

    @Test
    public void test_repeat_generic_element() {
        Integer[] arr = Array.repeat((Integer) 5, 3);
        assertArrayEquals(new Integer[] { 5, 5, 5 }, arr);

        Integer[] empty = Array.repeat((Integer) 5, 0);
        assertEquals(0, empty.length);

        assertThrows(IllegalArgumentException.class, () -> Array.repeat((Integer) 5, -1));
    }

    @Test
    public void test_repeat_generic_element_withClass() {
        Integer[] arr = Array.repeat(5, 3, Integer.class);
        assertArrayEquals(new Integer[] { 5, 5, 5 }, arr);

        Integer[] empty = Array.repeat(5, 0, Integer.class);
        assertEquals(0, empty.length);

        assertThrows(IllegalArgumentException.class, () -> Array.repeat(5, -1, Integer.class));
    }

    @Test
    public void test_repeat_generic_array_withClass() {
        Integer[] arr = Array.repeat(new Integer[] { 1, 2 }, 3, Integer.class);
        assertArrayEquals(new Integer[] { 1, 2, 1, 2, 1, 2 }, arr);

        Integer[] empty = Array.repeat(new Integer[] { 1 }, 0, Integer.class);
        assertEquals(0, empty.length);

        assertThrows(IllegalArgumentException.class, () -> Array.repeat(new Integer[] { 1 }, -1, Integer.class));
    }

    @Test
    public void test_repeatNonNull() {
        Integer[] arr = Array.repeatNonNull(5, 3);
        assertArrayEquals(new Integer[] { 5, 5, 5 }, arr);

        assertThrows(IllegalArgumentException.class, () -> Array.repeatNonNull((Integer) null, 3));
        assertThrows(IllegalArgumentException.class, () -> Array.repeatNonNull(5, -1));
    }

    @Test
    public void test_random_length() {
        int[] arr = Array.random(10);
        assertEquals(10, arr.length);

        int[] empty = Array.random(0);
        assertEquals(0, empty.length);

        assertThrows(NegativeArraySizeException.class, () -> Array.random(-1));
    }

    @Test
    public void test_random_withRange() {
        int[] arr = Array.random(0, 10, 5);
        assertEquals(5, arr.length);
        for (int val : arr) {
            assertTrue(val >= 0 && val < 10);
        }

        int[] empty = Array.random(0, 10, 0);
        assertEquals(0, empty.length);

        assertThrows(NegativeArraySizeException.class, () -> Array.random(0, 10, -1));
    }

    @Test
    public void test_concat_boolean_2D() {
        boolean[][] a = { { true, false }, { false, true } };
        boolean[][] b = { { true, true }, { false, false } };
        boolean[][] result = Array.concat(a, b);

        assertEquals(2, result.length);
        assertArrayEquals(new boolean[] { true, false, true, true }, result[0]);
        assertArrayEquals(new boolean[] { false, true, false, false }, result[1]);

        boolean[][] nullResult = Array.concat((boolean[][]) null, (boolean[][]) null);
        assertNotNull(nullResult);
        assertEquals(0, nullResult.length);
    }

    @Test
    public void test_concat_boolean_3D() {
        boolean[][][] a = { { { true, false } } };
        boolean[][][] b = { { { false, true } } };
        boolean[][][] result = Array.concat(a, b);

        assertEquals(1, result.length);
        assertEquals(1, result[0].length);
        assertArrayEquals(new boolean[] { true, false, false, true }, result[0][0]);
    }

    @Test
    public void test_concat_char_2D() {
        char[][] a = { { 'a', 'b' }, { 'c', 'd' } };
        char[][] b = { { 'e', 'f' } };
        char[][] result = Array.concat(a, b);

        assertEquals(2, result.length);
        assertArrayEquals(new char[] { 'a', 'b', 'e', 'f' }, result[0]);
        assertArrayEquals(new char[] { 'c', 'd' }, result[1]);
    }

    @Test
    public void test_concat_char_3D() {
        char[][][] a = { { { 'a', 'b' } } };
        char[][][] b = { { { 'c', 'd' } } };
        char[][][] result = Array.concat(a, b);

        assertEquals(1, result.length);
        assertArrayEquals(new char[] { 'a', 'b', 'c', 'd' }, result[0][0]);
    }

    @Test
    public void test_concat_byte_2D() {
        byte[][] a = { { 1, 2 }, { 3, 4 } };
        byte[][] b = { { 5, 6 } };
        byte[][] result = Array.concat(a, b);

        assertEquals(2, result.length);
        assertArrayEquals(new byte[] { 1, 2, 5, 6 }, result[0]);
        assertArrayEquals(new byte[] { 3, 4 }, result[1]);
    }

    @Test
    public void test_concat_byte_3D() {
        byte[][][] a = { { { 1, 2 } } };
        byte[][][] b = { { { 3, 4 } } };
        byte[][][] result = Array.concat(a, b);

        assertEquals(1, result.length);
        assertArrayEquals(new byte[] { 1, 2, 3, 4 }, result[0][0]);
    }

    @Test
    public void test_concat_short_2D() {
        short[][] a = { { 1, 2 }, { 3, 4 } };
        short[][] b = { { 5, 6 } };
        short[][] result = Array.concat(a, b);

        assertEquals(2, result.length);
        assertArrayEquals(new short[] { 1, 2, 5, 6 }, result[0]);
        assertArrayEquals(new short[] { 3, 4 }, result[1]);
    }

    @Test
    public void test_concat_short_3D() {
        short[][][] a = { { { 1, 2 } } };
        short[][][] b = { { { 3, 4 } } };
        short[][][] result = Array.concat(a, b);

        assertEquals(1, result.length);
        assertArrayEquals(new short[] { 1, 2, 3, 4 }, result[0][0]);
    }

    @Test
    public void test_concat_int_2D() {
        int[][] a = { { 1, 2 }, { 3, 4 } };
        int[][] b = { { 5, 6 } };
        int[][] result = Array.concat(a, b);

        assertEquals(2, result.length);
        assertArrayEquals(new int[] { 1, 2, 5, 6 }, result[0]);
        assertArrayEquals(new int[] { 3, 4 }, result[1]);
    }

    @Test
    public void test_concat_int_3D() {
        int[][][] a = { { { 1, 2 } } };
        int[][][] b = { { { 3, 4 } } };
        int[][][] result = Array.concat(a, b);

        assertEquals(1, result.length);
        assertArrayEquals(new int[] { 1, 2, 3, 4 }, result[0][0]);
    }

    @Test
    public void test_concat_long_2D() {
        long[][] a = { { 1L, 2L }, { 3L, 4L } };
        long[][] b = { { 5L, 6L } };
        long[][] result = Array.concat(a, b);

        assertEquals(2, result.length);
        assertArrayEquals(new long[] { 1L, 2L, 5L, 6L }, result[0]);
        assertArrayEquals(new long[] { 3L, 4L }, result[1]);
    }

    @Test
    public void test_concat_long_3D() {
        long[][][] a = { { { 1L, 2L } } };
        long[][][] b = { { { 3L, 4L } } };
        long[][][] result = Array.concat(a, b);

        assertEquals(1, result.length);
        assertArrayEquals(new long[] { 1L, 2L, 3L, 4L }, result[0][0]);
    }

    @Test
    public void test_concat_float_2D() {
        float[][] a = { { 1.1f, 2.2f }, { 3.3f, 4.4f } };
        float[][] b = { { 5.5f, 6.6f } };
        float[][] result = Array.concat(a, b);

        assertEquals(2, result.length);
        assertArrayEquals(new float[] { 1.1f, 2.2f, 5.5f, 6.6f }, result[0]);
        assertArrayEquals(new float[] { 3.3f, 4.4f }, result[1]);
    }

    @Test
    public void test_concat_float_3D() {
        float[][][] a = { { { 1.1f, 2.2f } } };
        float[][][] b = { { { 3.3f, 4.4f } } };
        float[][][] result = Array.concat(a, b);

        assertEquals(1, result.length);
        assertArrayEquals(new float[] { 1.1f, 2.2f, 3.3f, 4.4f }, result[0][0]);
    }

    @Test
    public void test_concat_double_2D() {
        double[][] a = { { 1.1, 2.2 }, { 3.3, 4.4 } };
        double[][] b = { { 5.5, 6.6 } };
        double[][] result = Array.concat(a, b);

        assertEquals(2, result.length);
        assertArrayEquals(new double[] { 1.1, 2.2, 5.5, 6.6 }, result[0]);
        assertArrayEquals(new double[] { 3.3, 4.4 }, result[1]);
    }

    @Test
    public void test_concat_double_3D() {
        double[][][] a = { { { 1.1, 2.2 } } };
        double[][][] b = { { { 3.3, 4.4 } } };
        double[][][] result = Array.concat(a, b);

        assertEquals(1, result.length);
        assertArrayEquals(new double[] { 1.1, 2.2, 3.3, 4.4 }, result[0][0]);
    }

    @Test
    public void test_concatt_2D() {
        String[][] a = { { "a", "b" }, { "c", "d" } };
        String[][] b = { { "e", "f" } };
        String[][] result = Array.concatt(a, b);

        assertEquals(2, result.length);
        assertArrayEquals(new String[] { "a", "b", "e", "f" }, result[0]);
        assertArrayEquals(new String[] { "c", "d" }, result[1]);

        String[][] nullResult = Array.concatt((String[][]) null, (String[][]) null);
        assertNull(nullResult);
    }

    @Test
    public void test_concatt_3D() {
        String[][][] a = { { { "a", "b" } } };
        String[][][] b = { { { "c", "d" } } };
        String[][][] result = Array.concatt(a, b);

        assertEquals(1, result.length);
        assertArrayEquals(new String[] { "a", "b", "c", "d" }, result[0][0]);
    }

    @Test
    public void test_box_boolean() {
        Boolean[] boxed = Array.box(true, false, true);
        assertArrayEquals(new Boolean[] { true, false, true }, boxed);

        Boolean[] empty = Array.box(new boolean[0]);
        assertEquals(0, empty.length);

        Boolean[] nullResult = Array.box((boolean[]) null);
        assertNull(nullResult);
    }

    @Test
    public void test_box_boolean_withRange() {
        boolean[] arr = { true, false, true, false };
        Boolean[] boxed = Array.box(arr, 1, 3);
        assertArrayEquals(new Boolean[] { false, true }, boxed);

        assertThrows(IndexOutOfBoundsException.class, () -> Array.box(arr, -1, 2));
        assertThrows(IndexOutOfBoundsException.class, () -> Array.box(arr, 0, 5));
    }

    @Test
    public void test_box_char() {
        Character[] boxed = Array.box('a', 'b', 'c');
        assertArrayEquals(new Character[] { 'a', 'b', 'c' }, boxed);

        Character[] empty = Array.box(new char[0]);
        assertEquals(0, empty.length);

        Character[] nullResult = Array.box((char[]) null);
        assertNull(nullResult);
    }

    @Test
    public void test_box_char_withRange() {
        char[] arr = { 'a', 'b', 'c', 'd' };
        Character[] boxed = Array.box(arr, 1, 3);
        assertArrayEquals(new Character[] { 'b', 'c' }, boxed);

        assertThrows(IndexOutOfBoundsException.class, () -> Array.box(arr, -1, 2));
    }

    @Test
    public void test_box_byte() {
        Byte[] boxed = Array.box((byte) 1, (byte) 2, (byte) 3);
        assertArrayEquals(new Byte[] { 1, 2, 3 }, boxed);

        Byte[] empty = Array.box(new byte[0]);
        assertEquals(0, empty.length);

        Byte[] nullResult = Array.box((byte[]) null);
        assertNull(nullResult);
    }

    @Test
    public void test_box_byte_withRange() {
        byte[] arr = { 1, 2, 3, 4 };
        Byte[] boxed = Array.box(arr, 1, 3);
        assertArrayEquals(new Byte[] { 2, 3 }, boxed);

        assertThrows(IndexOutOfBoundsException.class, () -> Array.box(arr, -1, 2));
    }

    @Test
    public void test_box_short() {
        Short[] boxed = Array.box((short) 1, (short) 2, (short) 3);
        assertArrayEquals(new Short[] { 1, 2, 3 }, boxed);

        Short[] empty = Array.box(new short[0]);
        assertEquals(0, empty.length);

        Short[] nullResult = Array.box((short[]) null);
        assertNull(nullResult);
    }

    @Test
    public void test_box_short_withRange() {
        short[] arr = { 1, 2, 3, 4 };
        Short[] boxed = Array.box(arr, 1, 3);
        assertArrayEquals(new Short[] { 2, 3 }, boxed);

        assertThrows(IndexOutOfBoundsException.class, () -> Array.box(arr, -1, 2));
    }

    @Test
    public void test_box_int() {
        Integer[] boxed = Array.box(1, 2, 3);
        assertArrayEquals(new Integer[] { 1, 2, 3 }, boxed);

        Integer[] empty = Array.box(new int[0]);
        assertEquals(0, empty.length);

        Integer[] nullResult = Array.box((int[]) null);
        assertNull(nullResult);
    }

    @Test
    public void test_box_int_withRange() {
        int[] arr = { 1, 2, 3, 4 };
        Integer[] boxed = Array.box(arr, 1, 3);
        assertArrayEquals(new Integer[] { 2, 3 }, boxed);

        assertThrows(IndexOutOfBoundsException.class, () -> Array.box(arr, -1, 2));
    }

    @Test
    public void test_box_long() {
        Long[] boxed = Array.box(1L, 2L, 3L);
        assertArrayEquals(new Long[] { 1L, 2L, 3L }, boxed);

        Long[] empty = Array.box(new long[0]);
        assertEquals(0, empty.length);

        Long[] nullResult = Array.box((long[]) null);
        assertNull(nullResult);
    }

    @Test
    public void test_box_long_withRange() {
        long[] arr = { 1L, 2L, 3L, 4L };
        Long[] boxed = Array.box(arr, 1, 3);
        assertArrayEquals(new Long[] { 2L, 3L }, boxed);

        assertThrows(IndexOutOfBoundsException.class, () -> Array.box(arr, -1, 2));
    }

    @Test
    public void test_box_float() {
        Float[] boxed = Array.box(1.1f, 2.2f, 3.3f);
        assertArrayEquals(new Float[] { 1.1f, 2.2f, 3.3f }, boxed);

        Float[] empty = Array.box(new float[0]);
        assertEquals(0, empty.length);

        Float[] nullResult = Array.box((float[]) null);
        assertNull(nullResult);
    }

    @Test
    public void test_box_float_withRange() {
        float[] arr = { 1.1f, 2.2f, 3.3f, 4.4f };
        Float[] boxed = Array.box(arr, 1, 3);
        assertArrayEquals(new Float[] { 2.2f, 3.3f }, boxed);

        assertThrows(IndexOutOfBoundsException.class, () -> Array.box(arr, -1, 2));
    }

    @Test
    public void test_box_double() {
        Double[] boxed = Array.box(1.1, 2.2, 3.3);
        assertArrayEquals(new Double[] { 1.1, 2.2, 3.3 }, boxed);

        Double[] empty = Array.box(new double[0]);
        assertEquals(0, empty.length);

        Double[] nullResult = Array.box((double[]) null);
        assertNull(nullResult);
    }

    @Test
    public void test_box_double_withRange() {
        double[] arr = { 1.1, 2.2, 3.3, 4.4 };
        Double[] boxed = Array.box(arr, 1, 3);
        assertArrayEquals(new Double[] { 2.2, 3.3 }, boxed);

        assertThrows(IndexOutOfBoundsException.class, () -> Array.box(arr, -1, 2));
    }

    @Test
    public void test_box_boolean_2D() {
        boolean[][] arr = { { true, false }, { false, true } };
        Boolean[][] boxed = Array.box(arr);

        assertEquals(2, boxed.length);
        assertArrayEquals(new Boolean[] { true, false }, boxed[0]);
        assertArrayEquals(new Boolean[] { false, true }, boxed[1]);

        Boolean[][] nullResult = Array.box((boolean[][]) null);
        assertNull(nullResult);
    }

    @Test
    public void test_box_char_2D() {
        char[][] arr = { { 'a', 'b' }, { 'c', 'd' } };
        Character[][] boxed = Array.box(arr);

        assertEquals(2, boxed.length);
        assertArrayEquals(new Character[] { 'a', 'b' }, boxed[0]);
        assertArrayEquals(new Character[] { 'c', 'd' }, boxed[1]);
    }

    @Test
    public void test_box_byte_2D() {
        byte[][] arr = { { 1, 2 }, { 3, 4 } };
        Byte[][] boxed = Array.box(arr);

        assertEquals(2, boxed.length);
        assertArrayEquals(new Byte[] { 1, 2 }, boxed[0]);
        assertArrayEquals(new Byte[] { 3, 4 }, boxed[1]);
    }

    @Test
    public void test_box_short_2D() {
        short[][] arr = { { 1, 2 }, { 3, 4 } };
        Short[][] boxed = Array.box(arr);

        assertEquals(2, boxed.length);
        assertArrayEquals(new Short[] { 1, 2 }, boxed[0]);
        assertArrayEquals(new Short[] { 3, 4 }, boxed[1]);
    }

    @Test
    public void test_box_int_2D() {
        int[][] arr = { { 1, 2 }, { 3, 4 } };
        Integer[][] boxed = Array.box(arr);

        assertEquals(2, boxed.length);
        assertArrayEquals(new Integer[] { 1, 2 }, boxed[0]);
        assertArrayEquals(new Integer[] { 3, 4 }, boxed[1]);
    }

    @Test
    public void test_box_long_2D() {
        long[][] arr = { { 1L, 2L }, { 3L, 4L } };
        Long[][] boxed = Array.box(arr);

        assertEquals(2, boxed.length);
        assertArrayEquals(new Long[] { 1L, 2L }, boxed[0]);
        assertArrayEquals(new Long[] { 3L, 4L }, boxed[1]);
    }

    @Test
    public void test_box_float_2D() {
        float[][] arr = { { 1.1f, 2.2f }, { 3.3f, 4.4f } };
        Float[][] boxed = Array.box(arr);

        assertEquals(2, boxed.length);
        assertArrayEquals(new Float[] { 1.1f, 2.2f }, boxed[0]);
        assertArrayEquals(new Float[] { 3.3f, 4.4f }, boxed[1]);
    }

    @Test
    public void test_box_double_2D() {
        double[][] arr = { { 1.1, 2.2 }, { 3.3, 4.4 } };
        Double[][] boxed = Array.box(arr);

        assertEquals(2, boxed.length);
        assertArrayEquals(new Double[] { 1.1, 2.2 }, boxed[0]);
        assertArrayEquals(new Double[] { 3.3, 4.4 }, boxed[1]);
    }

    @Test
    public void test_box_boolean_3D() {
        boolean[][][] arr = { { { true, false } } };
        Boolean[][][] boxed = Array.box(arr);

        assertEquals(1, boxed.length);
        assertEquals(1, boxed[0].length);
        assertArrayEquals(new Boolean[] { true, false }, boxed[0][0]);

        Boolean[][][] nullResult = Array.box((boolean[][][]) null);
        assertNull(nullResult);
    }

    @Test
    public void test_box_char_3D() {
        char[][][] arr = { { { 'a', 'b' } } };
        Character[][][] boxed = Array.box(arr);

        assertEquals(1, boxed.length);
        assertArrayEquals(new Character[] { 'a', 'b' }, boxed[0][0]);
    }

    @Test
    public void test_box_byte_3D() {
        byte[][][] arr = { { { 1, 2 } } };
        Byte[][][] boxed = Array.box(arr);

        assertEquals(1, boxed.length);
        assertArrayEquals(new Byte[] { 1, 2 }, boxed[0][0]);
    }

    @Test
    public void test_box_short_3D() {
        short[][][] arr = { { { 1, 2 } } };
        Short[][][] boxed = Array.box(arr);

        assertEquals(1, boxed.length);
        assertArrayEquals(new Short[] { 1, 2 }, boxed[0][0]);
    }

    @Test
    public void test_box_int_3D() {
        int[][][] arr = { { { 1, 2 } } };
        Integer[][][] boxed = Array.box(arr);

        assertEquals(1, boxed.length);
        assertArrayEquals(new Integer[] { 1, 2 }, boxed[0][0]);
    }

    @Test
    public void test_box_long_3D() {
        long[][][] arr = { { { 1L, 2L } } };
        Long[][][] boxed = Array.box(arr);

        assertEquals(1, boxed.length);
        assertArrayEquals(new Long[] { 1L, 2L }, boxed[0][0]);
    }

    @Test
    public void test_box_float_3D() {
        float[][][] arr = { { { 1.1f, 2.2f } } };
        Float[][][] boxed = Array.box(arr);

        assertEquals(1, boxed.length);
        assertArrayEquals(new Float[] { 1.1f, 2.2f }, boxed[0][0]);
    }

    @Test
    public void test_box_double_3D() {
        double[][][] arr = { { { 1.1, 2.2 } } };
        Double[][][] boxed = Array.box(arr);

        assertEquals(1, boxed.length);
        assertArrayEquals(new Double[] { 1.1, 2.2 }, boxed[0][0]);
    }

    @Test
    public void test_unbox_Boolean() {
        boolean[] unboxed = Array.unbox(true, false, true);
        assertArrayEquals(new boolean[] { true, false, true }, unboxed);

        boolean[] withNulls = Array.unbox(new Boolean[] { true, null, false }, false);
        assertArrayEquals(new boolean[] { true, false, false }, withNulls);

        boolean[] empty = Array.unbox(new Boolean[0]);
        assertEquals(0, empty.length);

        boolean[] nullResult = Array.unbox((Boolean[]) null);
        assertNull(nullResult);
    }

    @Test
    public void test_unbox_Boolean_withRange() {
        Boolean[] arr = { true, false, true, false };
        boolean[] unboxed = Array.unbox(arr, 1, 3, true);
        assertArrayEquals(new boolean[] { false, true }, unboxed);

        assertThrows(IndexOutOfBoundsException.class, () -> Array.unbox(arr, -1, 2, false));
    }

    @Test
    public void test_unbox_Character() {
        char[] unboxed = Array.unbox('a', 'b', 'c');
        assertArrayEquals(new char[] { 'a', 'b', 'c' }, unboxed);

        char[] withNulls = Array.unbox(new Character[] { 'a', null, 'c' }, 'x');
        assertArrayEquals(new char[] { 'a', 'x', 'c' }, withNulls);

        char[] empty = Array.unbox(new Character[0]);
        assertEquals(0, empty.length);

        char[] nullResult = Array.unbox((Character[]) null);
        assertNull(nullResult);
    }

    @Test
    public void test_unbox_Character_withRange() {
        Character[] arr = { 'a', 'b', 'c', 'd' };
        char[] unboxed = Array.unbox(arr, 1, 3, 'x');
        assertArrayEquals(new char[] { 'b', 'c' }, unboxed);

        assertThrows(IndexOutOfBoundsException.class, () -> Array.unbox(arr, -1, 2, 'x'));
    }

    @Test
    public void test_unbox_Byte() {
        byte[] unboxed = Array.unbox((Byte) (byte) 1, (Byte) (byte) 2, (Byte) (byte) 3);
        assertArrayEquals(new byte[] { 1, 2, 3 }, unboxed);

        byte[] withNulls = Array.unbox(new Byte[] { 1, null, 3 }, (byte) 0);
        assertArrayEquals(new byte[] { 1, 0, 3 }, withNulls);

        byte[] empty = Array.unbox(new Byte[0]);
        assertEquals(0, empty.length);

        byte[] nullResult = Array.unbox((Byte[]) null);
        assertNull(nullResult);
    }

    @Test
    public void test_unbox_Byte_withRange() {
        Byte[] arr = { 1, 2, 3, 4 };
        byte[] unboxed = Array.unbox(arr, 1, 3, (byte) 0);
        assertArrayEquals(new byte[] { 2, 3 }, unboxed);

        assertThrows(IndexOutOfBoundsException.class, () -> Array.unbox(arr, -1, 2, (byte) 0));
    }

    @Test
    public void test_unbox_Short() {
        short[] unboxed = Array.unbox((Short) (short) 1, (Short) (short) 2, (Short) (short) 3);
        assertArrayEquals(new short[] { 1, 2, 3 }, unboxed);

        short[] withNulls = Array.unbox(new Short[] { 1, null, 3 }, (short) 0);
        assertArrayEquals(new short[] { 1, 0, 3 }, withNulls);

        short[] empty = Array.unbox(new Short[0]);
        assertEquals(0, empty.length);

        short[] nullResult = Array.unbox((Short[]) null);
        assertNull(nullResult);
    }

    @Test
    public void test_unbox_Short_withRange() {
        Short[] arr = { 1, 2, 3, 4 };
        short[] unboxed = Array.unbox(arr, 1, 3, (short) 0);
        assertArrayEquals(new short[] { 2, 3 }, unboxed);

        assertThrows(IndexOutOfBoundsException.class, () -> Array.unbox(arr, -1, 2, (short) 0));
    }

    @Test
    public void test_unbox_Integer() {
        int[] unboxed = Array.unbox(1, 2, 3);
        assertArrayEquals(new int[] { 1, 2, 3 }, unboxed);

        int[] withNulls = Array.unbox(new Integer[] { 1, null, 3 }, 0);
        assertArrayEquals(new int[] { 1, 0, 3 }, withNulls);

        int[] empty = Array.unbox(new Integer[0]);
        assertEquals(0, empty.length);

        int[] nullResult = Array.unbox((Integer[]) null);
        assertNull(nullResult);
    }

    @Test
    public void test_unbox_Integer_withRange() {
        Integer[] arr = { 1, 2, 3, 4 };
        int[] unboxed = Array.unbox(arr, 1, 3, 0);
        assertArrayEquals(new int[] { 2, 3 }, unboxed);

        assertThrows(IndexOutOfBoundsException.class, () -> Array.unbox(arr, -1, 2, 0));
    }

    @Test
    public void test_unbox_Long() {
        long[] unboxed = Array.unbox(1L, 2L, 3L);
        assertArrayEquals(new long[] { 1L, 2L, 3L }, unboxed);

        long[] withNulls = Array.unbox(new Long[] { 1L, null, 3L }, 0L);
        assertArrayEquals(new long[] { 1L, 0L, 3L }, withNulls);

        long[] empty = Array.unbox(new Long[0]);
        assertEquals(0, empty.length);

        long[] nullResult = Array.unbox((Long[]) null);
        assertNull(nullResult);
    }

    @Test
    public void test_unbox_Long_withRange() {
        Long[] arr = { 1L, 2L, 3L, 4L };
        long[] unboxed = Array.unbox(arr, 1, 3, 0L);
        assertArrayEquals(new long[] { 2L, 3L }, unboxed);

        assertThrows(IndexOutOfBoundsException.class, () -> Array.unbox(arr, -1, 2, 0L));
    }

    @Test
    public void test_unbox_Float() {
        float[] unboxed = Array.unbox(1.1f, 2.2f, 3.3f);
        assertArrayEquals(new float[] { 1.1f, 2.2f, 3.3f }, unboxed);

        float[] withNulls = Array.unbox(new Float[] { 1.1f, null, 3.3f }, 0f);
        assertArrayEquals(new float[] { 1.1f, 0f, 3.3f }, withNulls);

        float[] empty = Array.unbox(new Float[0]);
        assertEquals(0, empty.length);

        float[] nullResult = Array.unbox((Float[]) null);
        assertNull(nullResult);
    }

    @Test
    public void test_unbox_Float_withRange() {
        Float[] arr = { 1.1f, 2.2f, 3.3f, 4.4f };
        float[] unboxed = Array.unbox(arr, 1, 3, 0f);
        assertArrayEquals(new float[] { 2.2f, 3.3f }, unboxed);

        assertThrows(IndexOutOfBoundsException.class, () -> Array.unbox(arr, -1, 2, 0f));
    }

    @Test
    public void test_unbox_Double() {
        double[] unboxed = Array.unbox(1.1, 2.2, 3.3);
        assertArrayEquals(new double[] { 1.1, 2.2, 3.3 }, unboxed);

        double[] withNulls = Array.unbox(new Double[] { 1.1, null, 3.3 }, 0.0);
        assertArrayEquals(new double[] { 1.1, 0.0, 3.3 }, withNulls);

        double[] empty = Array.unbox(new Double[0]);
        assertEquals(0, empty.length);

        double[] nullResult = Array.unbox((Double[]) null);
        assertNull(nullResult);
    }

    @Test
    public void test_unbox_Double_withRange() {
        Double[] arr = { 1.1, 2.2, 3.3, 4.4 };
        double[] unboxed = Array.unbox(arr, 1, 3, 0.0);
        assertArrayEquals(new double[] { 2.2, 3.3 }, unboxed);

        assertThrows(IndexOutOfBoundsException.class, () -> Array.unbox(arr, -1, 2, 0.0));
    }

    @Test
    public void test_unbox_Boolean_2D() {
        Boolean[][] arr = { { true, false }, { false, true } };
        boolean[][] unboxed = Array.unbox(arr);

        assertEquals(2, unboxed.length);
        assertArrayEquals(new boolean[] { true, false }, unboxed[0]);
        assertArrayEquals(new boolean[] { false, true }, unboxed[1]);

        Boolean[][] withNulls = { { true, null }, { false, null } };
        boolean[][] unboxedWithNulls = Array.unbox(withNulls, false);
        assertArrayEquals(new boolean[] { true, false }, unboxedWithNulls[0]);
        assertArrayEquals(new boolean[] { false, false }, unboxedWithNulls[1]);

        boolean[][] nullResult = Array.unbox((Boolean[][]) null);
        assertNull(nullResult);
    }

    @Test
    public void test_unbox_Character_2D() {
        Character[][] arr = { { 'a', 'b' }, { 'c', 'd' } };
        char[][] unboxed = Array.unbox(arr);

        assertEquals(2, unboxed.length);
        assertArrayEquals(new char[] { 'a', 'b' }, unboxed[0]);
        assertArrayEquals(new char[] { 'c', 'd' }, unboxed[1]);
    }

    @Test
    public void test_unbox_Byte_2D() {
        Byte[][] arr = { { 1, 2 }, { 3, 4 } };
        byte[][] unboxed = Array.unbox(arr);

        assertEquals(2, unboxed.length);
        assertArrayEquals(new byte[] { 1, 2 }, unboxed[0]);
        assertArrayEquals(new byte[] { 3, 4 }, unboxed[1]);
    }

    @Test
    public void test_unbox_Short_2D() {
        Short[][] arr = { { 1, 2 }, { 3, 4 } };
        short[][] unboxed = Array.unbox(arr);

        assertEquals(2, unboxed.length);
        assertArrayEquals(new short[] { 1, 2 }, unboxed[0]);
        assertArrayEquals(new short[] { 3, 4 }, unboxed[1]);
    }

    @Test
    public void test_unbox_Integer_2D() {
        Integer[][] arr = { { 1, 2 }, { 3, 4 } };
        int[][] unboxed = Array.unbox(arr);

        assertEquals(2, unboxed.length);
        assertArrayEquals(new int[] { 1, 2 }, unboxed[0]);
        assertArrayEquals(new int[] { 3, 4 }, unboxed[1]);
    }

    @Test
    public void test_unbox_Long_2D() {
        Long[][] arr = { { 1L, 2L }, { 3L, 4L } };
        long[][] unboxed = Array.unbox(arr);

        assertEquals(2, unboxed.length);
        assertArrayEquals(new long[] { 1L, 2L }, unboxed[0]);
        assertArrayEquals(new long[] { 3L, 4L }, unboxed[1]);
    }

    @Test
    public void test_unbox_Float_2D() {
        Float[][] arr = { { 1.1f, 2.2f }, { 3.3f, 4.4f } };
        float[][] unboxed = Array.unbox(arr);

        assertEquals(2, unboxed.length);
        assertArrayEquals(new float[] { 1.1f, 2.2f }, unboxed[0]);
        assertArrayEquals(new float[] { 3.3f, 4.4f }, unboxed[1]);
    }

    @Test
    public void test_unbox_Double_2D() {
        Double[][] arr = { { 1.1, 2.2 }, { 3.3, 4.4 } };
        double[][] unboxed = Array.unbox(arr);

        assertEquals(2, unboxed.length);
        assertArrayEquals(new double[] { 1.1, 2.2 }, unboxed[0]);
        assertArrayEquals(new double[] { 3.3, 4.4 }, unboxed[1]);
    }

    @Test
    public void test_unbox_Boolean_3D() {
        Boolean[][][] arr = { { { true, false } } };
        boolean[][][] unboxed = Array.unbox(arr);

        assertEquals(1, unboxed.length);
        assertArrayEquals(new boolean[] { true, false }, unboxed[0][0]);

        boolean[][][] nullResult = Array.unbox((Boolean[][][]) null);
        assertNull(nullResult);
    }

    @Test
    public void test_unbox_Character_3D() {
        Character[][][] arr = { { { 'a', 'b' } } };
        char[][][] unboxed = Array.unbox(arr);

        assertEquals(1, unboxed.length);
        assertArrayEquals(new char[] { 'a', 'b' }, unboxed[0][0]);
    }

    @Test
    public void test_unbox_Byte_3D() {
        Byte[][][] arr = { { { 1, 2 } } };
        byte[][][] unboxed = Array.unbox(arr);

        assertEquals(1, unboxed.length);
        assertArrayEquals(new byte[] { 1, 2 }, unboxed[0][0]);
    }

    @Test
    public void test_unbox_Short_3D() {
        Short[][][] arr = { { { 1, 2 } } };
        short[][][] unboxed = Array.unbox(arr);

        assertEquals(1, unboxed.length);
        assertArrayEquals(new short[] { 1, 2 }, unboxed[0][0]);
    }

    @Test
    public void test_unbox_Integer_3D() {
        Integer[][][] arr = { { { 1, 2 } } };
        int[][][] unboxed = Array.unbox(arr);

        assertEquals(1, unboxed.length);
        assertArrayEquals(new int[] { 1, 2 }, unboxed[0][0]);
    }

    @Test
    public void test_unbox_Long_3D() {
        Long[][][] arr = { { { 1L, 2L } } };
        long[][][] unboxed = Array.unbox(arr);

        assertEquals(1, unboxed.length);
        assertArrayEquals(new long[] { 1L, 2L }, unboxed[0][0]);
    }

    @Test
    public void test_unbox_Float_3D() {
        Float[][][] arr = { { { 1.1f, 2.2f } } };
        float[][][] unboxed = Array.unbox(arr);

        assertEquals(1, unboxed.length);
        assertArrayEquals(new float[] { 1.1f, 2.2f }, unboxed[0][0]);
    }

    @Test
    public void test_unbox_Double_3D() {
        Double[][][] arr = { { { 1.1, 2.2 } } };
        double[][][] unboxed = Array.unbox(arr);

        assertEquals(1, unboxed.length);
        assertArrayEquals(new double[] { 1.1, 2.2 }, unboxed[0][0]);
    }

    @Test
    public void test_transpose_boolean() {
        boolean[][] arr = { { true, false, true }, { false, true, false } };
        boolean[][] transposed = Array.transpose(arr);

        assertEquals(3, transposed.length);
        assertEquals(2, transposed[0].length);
        assertArrayEquals(new boolean[] { true, false }, transposed[0]);
        assertArrayEquals(new boolean[] { false, true }, transposed[1]);
        assertArrayEquals(new boolean[] { true, false }, transposed[2]);

        boolean[][] nullResult = Array.transpose((boolean[][]) null);
        assertNull(nullResult);

        boolean[][] empty = Array.transpose(new boolean[0][]);
        assertEquals(0, empty.length);
    }

    @Test
    public void test_transpose_char() {
        char[][] arr = { { 'a', 'b', 'c' }, { 'd', 'e', 'f' } };
        char[][] transposed = Array.transpose(arr);

        assertEquals(3, transposed.length);
        assertEquals(2, transposed[0].length);
        assertArrayEquals(new char[] { 'a', 'd' }, transposed[0]);
        assertArrayEquals(new char[] { 'b', 'e' }, transposed[1]);
        assertArrayEquals(new char[] { 'c', 'f' }, transposed[2]);
    }

    @Test
    public void test_transpose_byte() {
        byte[][] arr = { { 1, 2, 3 }, { 4, 5, 6 } };
        byte[][] transposed = Array.transpose(arr);

        assertEquals(3, transposed.length);
        assertEquals(2, transposed[0].length);
        assertArrayEquals(new byte[] { 1, 4 }, transposed[0]);
        assertArrayEquals(new byte[] { 2, 5 }, transposed[1]);
        assertArrayEquals(new byte[] { 3, 6 }, transposed[2]);
    }

    @Test
    public void test_transpose_short() {
        short[][] arr = { { 1, 2, 3 }, { 4, 5, 6 } };
        short[][] transposed = Array.transpose(arr);

        assertEquals(3, transposed.length);
        assertEquals(2, transposed[0].length);
        assertArrayEquals(new short[] { 1, 4 }, transposed[0]);
        assertArrayEquals(new short[] { 2, 5 }, transposed[1]);
        assertArrayEquals(new short[] { 3, 6 }, transposed[2]);
    }

    @Test
    public void test_transpose_int() {
        int[][] arr = { { 1, 2, 3 }, { 4, 5, 6 } };
        int[][] transposed = Array.transpose(arr);

        assertEquals(3, transposed.length);
        assertEquals(2, transposed[0].length);
        assertArrayEquals(new int[] { 1, 4 }, transposed[0]);
        assertArrayEquals(new int[] { 2, 5 }, transposed[1]);
        assertArrayEquals(new int[] { 3, 6 }, transposed[2]);
    }

    @Test
    public void test_transpose_long() {
        long[][] arr = { { 1L, 2L, 3L }, { 4L, 5L, 6L } };
        long[][] transposed = Array.transpose(arr);

        assertEquals(3, transposed.length);
        assertEquals(2, transposed[0].length);
        assertArrayEquals(new long[] { 1L, 4L }, transposed[0]);
        assertArrayEquals(new long[] { 2L, 5L }, transposed[1]);
        assertArrayEquals(new long[] { 3L, 6L }, transposed[2]);
    }

    @Test
    public void test_transpose_float() {
        float[][] arr = { { 1.1f, 2.2f, 3.3f }, { 4.4f, 5.5f, 6.6f } };
        float[][] transposed = Array.transpose(arr);

        assertEquals(3, transposed.length);
        assertEquals(2, transposed[0].length);
        assertArrayEquals(new float[] { 1.1f, 4.4f }, transposed[0]);
        assertArrayEquals(new float[] { 2.2f, 5.5f }, transposed[1]);
        assertArrayEquals(new float[] { 3.3f, 6.6f }, transposed[2]);
    }

    @Test
    public void test_transpose_double() {
        double[][] arr = { { 1.1, 2.2, 3.3 }, { 4.4, 5.5, 6.6 } };
        double[][] transposed = Array.transpose(arr);

        assertEquals(3, transposed.length);
        assertEquals(2, transposed[0].length);
        assertArrayEquals(new double[] { 1.1, 4.4 }, transposed[0]);
        assertArrayEquals(new double[] { 2.2, 5.5 }, transposed[1]);
        assertArrayEquals(new double[] { 3.3, 6.6 }, transposed[2]);
    }

    @Test
    public void test_transpose_generic() {
        String[][] arr = { { "a", "b", "c" }, { "d", "e", "f" } };
        String[][] transposed = Array.transpose(arr);

        assertEquals(3, transposed.length);
        assertEquals(2, transposed[0].length);
        assertArrayEquals(new String[] { "a", "d" }, transposed[0]);
        assertArrayEquals(new String[] { "b", "e" }, transposed[1]);
        assertArrayEquals(new String[] { "c", "f" }, transposed[2]);

        String[][] nullResult = Array.transpose((String[][]) null);
        assertNull(nullResult);

        String[][] invalid = { { "a", "b" }, { "c", "d", "e" } };
        assertThrows(IllegalArgumentException.class, () -> Array.transpose(invalid));
    }

    @Test
    public void test_newInstance_primitiveTypes() {
        int[] intArr = Array.newInstance(int.class, 5);
        assertNotNull(intArr);
        assertEquals(5, intArr.length);

        boolean[] boolArr = Array.newInstance(boolean.class, 3);
        assertNotNull(boolArr);
        assertEquals(3, boolArr.length);

        double[] doubleArr = Array.newInstance(double.class, 2);
        assertNotNull(doubleArr);
        assertEquals(2, doubleArr.length);
    }

    @Test
    public void test_newInstance_multidimensional_primitives() {
        int[][] arr2D = Array.newInstance(int.class, 2, 3);
        assertNotNull(arr2D);
        assertEquals(2, arr2D.length);
        assertEquals(3, arr2D[0].length);

        boolean[][][] arr3D = Array.newInstance(boolean.class, 2, 3, 4);
        assertNotNull(arr3D);
        assertEquals(2, arr3D.length);
        assertEquals(3, arr3D[0].length);
        assertEquals(4, arr3D[0][0].length);
    }

    @Test
    public void test_get_primitiveArrays() {
        int[] intArr = { 10, 20, 30 };
        assertEquals(20, (Integer) Array.get(intArr, 1));

        double[] doubleArr = { 1.5, 2.5, 3.5 };
        assertEquals(2.5, Array.get(doubleArr, 1));

        assertThrows(IllegalArgumentException.class, () -> Array.get("not an array", 0));
        assertThrows(NullPointerException.class, () -> Array.get(null, 0));
    }

    @Test
    public void test_set_primitiveArrays() {
        int[] intArr = { 10, 20, 30 };
        Array.set(intArr, 1, 25);
        assertEquals(25, intArr[1]);

        double[] doubleArr = { 1.5, 2.5, 3.5 };
        Array.set(doubleArr, 0, 9.9);
        assertEquals(9.9, doubleArr[0], 0.001);

        assertThrows(IllegalArgumentException.class, () -> Array.set("not an array", 0, 1));
        assertThrows(NullPointerException.class, () -> Array.set(null, 0, 1));
    }

    @Test
    public void test_asList_withNull() {
        List<String> list = Array.asList((String[]) null);
        assertNotNull(list);
        assertEquals(0, list.size());
    }

    @Test
    public void test_asList_singleElement() {
        String[] arr = { "single" };
        List<String> list = Array.asList(arr);
        assertEquals(1, list.size());
        assertEquals("single", list.get(0));
    }

    @Test
    public void test_of_emptyArrays() {
        boolean[] boolArr = Array.of(new boolean[0]);
        assertEquals(0, boolArr.length);

        char[] charArr = Array.of(new char[0]);
        assertEquals(0, charArr.length);

        int[] intArr = Array.of(new int[0]);
        assertEquals(0, intArr.length);

        long[] longArr = Array.of(new long[0]);
        assertEquals(0, longArr.length);

        float[] floatArr = Array.of(new float[0]);
        assertEquals(0, floatArr.length);

        double[] doubleArr = Array.of(new double[0]);
        assertEquals(0, doubleArr.length);
    }

    @Test
    public void test_range_edgeCases() {
        int[] emptyInt = Array.range(5, 5);
        assertEquals(0, emptyInt.length);

        char[] emptyChar = Array.range('z', 'z');
        assertEquals(0, emptyChar.length);

        int[] negativeRange = Array.range(-5, 0);
        assertArrayEquals(new int[] { -5, -4, -3, -2, -1 }, negativeRange);

        byte[] negativeByteRange = Array.range((byte) -3, (byte) 2);
        assertArrayEquals(new byte[] { -3, -2, -1, 0, 1 }, negativeByteRange);
    }

    @Test
    public void test_range_withStep_edgeCases() {
        int[] largeStep = Array.range(0, 100, 25);
        assertArrayEquals(new int[] { 0, 25, 50, 75 }, largeStep);

        int[] negStep = Array.range(10, 0, -2);
        assertArrayEquals(new int[] { 10, 8, 6, 4, 2 }, negStep);

        int[] stepLargerThanRange = Array.range(0, 5, 10);
        assertArrayEquals(new int[] { 0 }, stepLargerThanRange);
    }

    @Test
    public void test_rangeClosed_edgeCases() {
        int[] singleInt = Array.rangeClosed(5, 5);
        assertArrayEquals(new int[] { 5 }, singleInt);

        char[] singleChar = Array.rangeClosed('m', 'm');
        assertArrayEquals(new char[] { 'm' }, singleChar);

        int[] negativeRange = Array.rangeClosed(-5, 0);
        assertArrayEquals(new int[] { -5, -4, -3, -2, -1, 0 }, negativeRange);

        long[] negativeLong = Array.rangeClosed(-3L, 2L);
        assertArrayEquals(new long[] { -3L, -2L, -1L, 0L, 1L, 2L }, negativeLong);
    }

    @Test
    public void test_rangeClosed_withStep_edgeCases() {
        int[] unevenStep = Array.rangeClosed(0, 10, 3);
        assertArrayEquals(new int[] { 0, 3, 6, 9 }, unevenStep);

        int[] exactNegStep = Array.rangeClosed(10, 0, -2);
        assertArrayEquals(new int[] { 10, 8, 6, 4, 2, 0 }, exactNegStep);

        char[] charStep = Array.rangeClosed('a', 'j', 3);
        assertArrayEquals(new char[] { 'a', 'd', 'g', 'j' }, charStep);
    }

    @Test
    public void test_repeat_zeroCount() {
        boolean[] boolRepeat = Array.repeat(true, 0);
        assertEquals(0, boolRepeat.length);

        int[] intRepeat = Array.repeat(5, 0);
        assertEquals(0, intRepeat.length);

        String[] strRepeat = Array.repeat("test", 0);
        assertEquals(0, strRepeat.length);
    }

    @Test
    public void test_repeat_arrays_nullInput() {
        boolean[] result = Array.repeat((boolean[]) null, 3);
        assertNotNull(result);
        assertEquals(0, result.length);

        int[] resultInt = Array.repeat((int[]) null, 3);
        assertNotNull(resultInt);
        assertEquals(0, resultInt.length);

        String[] resultStr = Array.repeat((String[]) null, 3);
        assertNotNull(resultStr);
        assertEquals(0, resultStr.length);
    }

    @Test
    public void test_repeat_largeCount() {
        boolean[] boolArr = Array.repeat(true, 100);
        assertEquals(100, boolArr.length);
        for (boolean val : boolArr) {
            assertTrue(val);
        }

        int[] intArr = Array.repeat(42, 50);
        assertEquals(50, intArr.length);
        for (int val : intArr) {
            assertEquals(42, val);
        }
    }

    @Test
    public void test_repeat_generic_nullElement() {
        assertThrows(IllegalArgumentException.class, () -> Array.repeat((Integer) null, 3));
    }

    @Test
    public void test_repeat_generic_array_emptyArray() {
        Integer[] result = Array.repeat(new Integer[0], 5, Integer.class);
        assertEquals(0, result.length);

        String[] strResult = Array.repeat(new String[0], 3, String.class);
        assertEquals(0, strResult.length);
    }

    @Test
    public void test_random_edgeCases() {
        int[] empty = Array.random(0);
        assertEquals(0, empty.length);

        int[] single = Array.random(1);
        assertEquals(1, single.length);

        // Test with valid range
        int[] rangeArr = Array.random(5, 10, 10);
        assertEquals(10, rangeArr.length);
        for (int val : rangeArr) {
            assertTrue(val >= 5 && val < 10);
        }
    }

    @Test
    public void test_concat_2D_withNullArrays() {
        int[][] a = null;
        int[][] b = { { 1, 2 }, { 3, 4 } };
        int[][] result = Array.concat(a, b);
        assertNotNull(result);
        assertEquals(2, result.length);

        int[][] c = { { 5, 6 } };
        int[][] d = null;
        int[][] result2 = Array.concat(c, d);
        assertNotNull(result2);
        assertEquals(1, result2.length);

        int[][] nullBoth = Array.concat((int[][]) null, (int[][]) null);
        assertNotNull(nullBoth);
        assertEquals(0, nullBoth.length);
    }

    @Test
    public void test_concat_2D_emptyArrays() {
        int[][] empty1 = new int[0][];
        int[][] empty2 = new int[0][];
        int[][] result = Array.concat(empty1, empty2);
        assertEquals(0, result.length);

        int[][] nonEmpty = { { 1, 2 } };
        int[][] result2 = Array.concat(empty1, nonEmpty);
        assertEquals(1, result2.length);
    }

    @Test
    public void test_concat_3D_edgeCases() {
        int[][][] a = { { { 1, 2 } }, { { 3, 4 } } };
        int[][][] b = { { { 5, 6 } } };
        int[][][] result = Array.concat(a, b);
        assertEquals(2, result.length);
        assertArrayEquals(new int[] { 1, 2, 5, 6 }, result[0][0]);
        assertArrayEquals(new int[] { 3, 4 }, result[1][0]);

        int[][][] nullBoth = Array.concat((int[][][]) null, (int[][][]) null);
        assertNotNull(nullBoth);
        assertEquals(0, nullBoth.length);
    }

    @Test
    public void test_concatt_generic_nullHandling() {
        String[][] a = { { "a", "b" } };
        String[][] b = null;
        String[][] result = Array.concatt(a, b);
        assertNotNull(result);
        assertEquals(1, result.length);

        String[][] nullBoth = Array.concatt((String[][]) null, (String[][]) null);
        assertNull(nullBoth);
    }

    @Test
    public void test_box_withRange_edgeCases() {
        int[] arr = { 1, 2, 3, 4, 5 };
        Integer[] boxed = Array.box(arr, 0, 5);
        assertEquals(5, boxed.length);
        assertEquals(1, boxed[0]);
        assertEquals(5, boxed[4]);

        Integer[] empty = Array.box(arr, 2, 2);
        assertEquals(0, empty.length);

        Integer[] partial = Array.box(arr, 2, 4);
        assertArrayEquals(new Integer[] { 3, 4 }, partial);
    }

    @Test
    public void test_box_2D_nullInput() {
        Boolean[][] result = Array.box((boolean[][]) null);
        assertNull(result);

        Integer[][] resultInt = Array.box((int[][]) null);
        assertNull(resultInt);

        Double[][] resultDouble = Array.box((double[][]) null);
        assertNull(resultDouble);
    }

    @Test
    public void test_box_2D_emptyArrays() {
        boolean[][] empty = new boolean[0][];
        Boolean[][] boxed = Array.box(empty);
        assertEquals(0, boxed.length);

        int[][] emptyInt = new int[0][];
        Integer[][] boxedInt = Array.box(emptyInt);
        assertEquals(0, boxedInt.length);
    }

    @Test
    public void test_box_3D_emptyArrays() {
        boolean[][][] empty = new boolean[0][][];
        Boolean[][][] boxed = Array.box(empty);
        assertEquals(0, boxed.length);

        int[][][] emptyInt = new int[0][][];
        Integer[][][] boxedInt = Array.box(emptyInt);
        assertEquals(0, boxedInt.length);
    }

    @Test
    public void test_unbox_withRange_edgeCases() {
        Integer[] arr = { 10, 20, 30, 40, 50 };
        int[] unboxed = Array.unbox(arr, 0, 5, 0);
        assertEquals(5, unboxed.length);
        assertEquals(10, unboxed[0]);
        assertEquals(50, unboxed[4]);

        int[] empty = Array.unbox(arr, 2, 2, 0);
        assertEquals(0, empty.length);

        Integer[] withNulls = { 1, null, 3, null, 5 };
        int[] unboxedWithNulls = Array.unbox(withNulls, 1, 4, -1);
        assertArrayEquals(new int[] { -1, 3, -1 }, unboxedWithNulls);
    }

    @Test
    public void test_unbox_2D_emptyArrays() {
        Boolean[][] empty = new Boolean[0][];
        boolean[][] unboxed = Array.unbox(empty);
        assertEquals(0, unboxed.length);

        Integer[][] emptyInt = new Integer[0][];
        int[][] unboxedInt = Array.unbox(emptyInt);
        assertEquals(0, unboxedInt.length);
    }

    @Test
    public void test_unbox_2D_withNullSubarrays() {
        Integer[][] arr = { { 1, 2 }, null, { 3, 4 } };
        int[][] unboxed = Array.unbox(arr);
        assertEquals(3, unboxed.length);
        assertArrayEquals(new int[] { 1, 2 }, unboxed[0]);
        assertNull(unboxed[1]);
        assertArrayEquals(new int[] { 3, 4 }, unboxed[2]);
    }

    @Test
    public void test_unbox_3D_emptyArrays() {
        Boolean[][][] empty = new Boolean[0][][];
        boolean[][][] unboxed = Array.unbox(empty);
        assertEquals(0, unboxed.length);

        Integer[][][] emptyInt = new Integer[0][][];
        int[][][] unboxedInt = Array.unbox(emptyInt);
        assertEquals(0, unboxedInt.length);
    }

    @Test
    public void test_transpose_singleRow() {
        int[][] singleRow = { { 1, 2, 3 } };
        int[][] transposed = Array.transpose(singleRow);
        assertEquals(3, transposed.length);
        assertEquals(1, transposed[0].length);
        assertArrayEquals(new int[] { 1 }, transposed[0]);
        assertArrayEquals(new int[] { 2 }, transposed[1]);
        assertArrayEquals(new int[] { 3 }, transposed[2]);
    }

    @Test
    public void test_transpose_singleColumn() {
        int[][] singleCol = { { 1 }, { 2 }, { 3 } };
        int[][] transposed = Array.transpose(singleCol);
        assertEquals(1, transposed.length);
        assertEquals(3, transposed[0].length);
        assertArrayEquals(new int[] { 1, 2, 3 }, transposed[0]);
    }

    @Test
    public void test_transpose_square() {
        int[][] square = { { 1, 2, 3 }, { 4, 5, 6 }, { 7, 8, 9 } };
        int[][] transposed = Array.transpose(square);
        assertEquals(3, transposed.length);
        assertEquals(3, transposed[0].length);
        assertArrayEquals(new int[] { 1, 4, 7 }, transposed[0]);
        assertArrayEquals(new int[] { 2, 5, 8 }, transposed[1]);
        assertArrayEquals(new int[] { 3, 6, 9 }, transposed[2]);
    }

    @Test
    public void test_transpose_emptySubarrays() {
        int[][] arr = new int[2][0];
        int[][] transposed = Array.transpose(arr);
        assertEquals(0, transposed.length);

        String[][] strArr = new String[3][0];
        String[][] strTransposed = Array.transpose(strArr);
        assertEquals(0, strTransposed.length);
    }

    @Test
    public void test_getLength_primitiveArrays() {
        assertEquals(5, Array.getLength(new int[5]));
        assertEquals(3, Array.getLength(new boolean[3]));
        assertEquals(10, Array.getLength(new double[10]));
        assertEquals(0, Array.getLength(new byte[0]));

        assertEquals(3, Array.getLength(new int[3][]));
        assertEquals(2, Array.getLength(new String[2]));
    }

    @Test
    public void test_primitiveGetters_wrongType() {
        int[] intArr = { 1, 2, 3 };
        assertThrows(IllegalArgumentException.class, () -> Array.getBoolean(intArr, 0));
        assertThrows(IllegalArgumentException.class, () -> Array.getFloat("not array", 0));
    }

    @Test
    public void test_primitiveSetters_wrongType() {
        int[] intArr = { 1, 2, 3 };
        assertThrows(IllegalArgumentException.class, () -> Array.setBoolean(intArr, 0, true));
        assertThrows(IllegalArgumentException.class, () -> Array.setChar("not array", 0, 'a'));
    }

    @Test
    public void test_primitiveGettersSetters_nullArray() {
        assertThrows(NullPointerException.class, () -> Array.getBoolean(null, 0));
        assertThrows(NullPointerException.class, () -> Array.getInt(null, 0));
        assertThrows(NullPointerException.class, () -> Array.getDouble(null, 0));

        assertThrows(NullPointerException.class, () -> Array.setBoolean(null, 0, true));
        assertThrows(NullPointerException.class, () -> Array.setInt(null, 0, 5));
        assertThrows(NullPointerException.class, () -> Array.setDouble(null, 0, 1.5));
    }

    @Test
    public void test_box_allTypes_emptyArrays() {
        assertEquals(0, Array.box(new boolean[0]).length);
        assertEquals(0, Array.box(new char[0]).length);
        assertEquals(0, Array.box(new byte[0]).length);
        assertEquals(0, Array.box(new short[0]).length);
        assertEquals(0, Array.box(new int[0]).length);
        assertEquals(0, Array.box(new long[0]).length);
        assertEquals(0, Array.box(new float[0]).length);
        assertEquals(0, Array.box(new double[0]).length);
    }

    @Test
    public void test_unbox_allTypes_emptyArrays() {
        assertEquals(0, Array.unbox(new Boolean[0]).length);
        assertEquals(0, Array.unbox(new Character[0]).length);
        assertEquals(0, Array.unbox(new Byte[0]).length);
        assertEquals(0, Array.unbox(new Short[0]).length);
        assertEquals(0, Array.unbox(new Integer[0]).length);
        assertEquals(0, Array.unbox(new Long[0]).length);
        assertEquals(0, Array.unbox(new Float[0]).length);
        assertEquals(0, Array.unbox(new Double[0]).length);
    }

    @Test
    public void test_concat_allPrimitiveTypes_2D() {
        boolean[][] bool1 = { { true } };
        boolean[][] bool2 = { { false } };
        assertEquals(1, Array.concat(bool1, bool2).length);

        char[][] char1 = { { 'a' } };
        char[][] char2 = { { 'b' } };
        assertEquals(1, Array.concat(char1, char2).length);

        byte[][] byte1 = { { 1 } };
        byte[][] byte2 = { { 2 } };
        assertEquals(1, Array.concat(byte1, byte2).length);

        short[][] short1 = { { 1 } };
        short[][] short2 = { { 2 } };
        assertEquals(1, Array.concat(short1, short2).length);

        long[][] long1 = { { 1L } };
        long[][] long2 = { { 2L } };
        assertEquals(1, Array.concat(long1, long2).length);

        float[][] float1 = { { 1.0f } };
        float[][] float2 = { { 2.0f } };
        assertEquals(1, Array.concat(float1, float2).length);

        double[][] double1 = { { 1.0 } };
        double[][] double2 = { { 2.0 } };
        assertEquals(1, Array.concat(double1, double2).length);
    }

    @Test
    public void test_repeat_boundaryValues() {
        byte maxByte = Byte.MAX_VALUE;
        byte[] byteArr = Array.repeat(maxByte, 5);
        for (byte b : byteArr) {
            assertEquals(maxByte, b);
        }

        short maxShort = Short.MAX_VALUE;
        short[] shortArr = Array.repeat(maxShort, 5);
        for (short s : shortArr) {
            assertEquals(maxShort, s);
        }

        int maxInt = Integer.MAX_VALUE;
        int[] intArr = Array.repeat(maxInt, 5);
        for (int i : intArr) {
            assertEquals(maxInt, i);
        }

        long maxLong = Long.MAX_VALUE;
        long[] longArr = Array.repeat(maxLong, 5);
        for (long l : longArr) {
            assertEquals(maxLong, l);
        }

        float maxFloat = Float.MAX_VALUE;
        float[] floatArr = Array.repeat(maxFloat, 5);
        for (float f : floatArr) {
            assertEquals(maxFloat, f, 0.0001);
        }

        double maxDouble = Double.MAX_VALUE;
        double[] doubleArr = Array.repeat(maxDouble, 5);
        for (double d : doubleArr) {
            assertEquals(maxDouble, d, 0.0001);
        }
    }

    @Test
    public void test_newInstance_caching() {
        String[] arr1 = Array.newInstance(String.class, 0);
        String[] arr2 = Array.newInstance(String.class, 0);
        assertSame(arr1, arr2, "Empty arrays of the same type should be cached");

        Integer[] intArr1 = Array.newInstance(Integer.class, 0);
        Integer[] intArr2 = Array.newInstance(Integer.class, 0);
        assertSame(intArr1, intArr2, "Empty arrays of the same type should be cached");

        String[] arr3 = Array.newInstance(String.class, 1);
        String[] arr4 = Array.newInstance(String.class, 1);
        assertNotSame(arr3, arr4, "Non-empty arrays should not be cached");
    }

    @Test
    public void test_boxUnbox_symmetry() {
        int[] original = { 1, 2, 3, 4, 5 };
        Integer[] boxed = Array.box(original);
        int[] unboxed = Array.unbox(boxed);
        assertArrayEquals(original, unboxed);

        boolean[] boolOriginal = { true, false, true };
        Boolean[] boolBoxed = Array.box(boolOriginal);
        boolean[] boolUnboxed = Array.unbox(boolBoxed);
        assertArrayEquals(boolOriginal, boolUnboxed);

        double[] doubleOriginal = { 1.1, 2.2, 3.3 };
        Double[] doubleBoxed = Array.box(doubleOriginal);
        double[] doubleUnboxed = Array.unbox(doubleBoxed);
        assertArrayEquals(doubleOriginal, doubleUnboxed, 0.0001);
    }

    @Test
    public void test_transpose_thenTranspose_identity() {
        int[][] original = { { 1, 2, 3 }, { 4, 5, 6 } };
        int[][] transposed = Array.transpose(original);
        int[][] doubleTransposed = Array.transpose(transposed);

        assertEquals(original.length, doubleTransposed.length);
        assertEquals(original[0].length, doubleTransposed[0].length);
        assertArrayEquals(original[0], doubleTransposed[0]);
        assertArrayEquals(original[1], doubleTransposed[1]);
    }

    @Test
    public void test_repeat_array_singleRepetition() {
        int[] original = { 1, 2, 3 };
        int[] repeated = Array.repeat(original, 1);
        assertArrayEquals(original, repeated);

        String[] strOriginal = { "a", "b" };
        String[] strRepeated = Array.repeat(strOriginal, 1);
        assertArrayEquals(strOriginal, strRepeated);
    }

    @Test
    public void test_range_reverseDirection() {
        int[] empty = Array.range(10, 5);
        assertEquals(0, empty.length);

        long[] emptyLong = Array.range(100L, 50L);
        assertEquals(0, emptyLong.length);

        int[] descending = Array.range(10, 5, -1);
        assertArrayEquals(new int[] { 10, 9, 8, 7, 6 }, descending);
    }
}

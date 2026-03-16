package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.Temporal;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

@Tag("2025")
public class ArrayTest extends TestBase {

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
    public void test_concat_2D() {
        String[][] a = { { "a", "b" }, { "c", "d" } };
        String[][] b = { { "e", "f" } };
        String[][] result = Array.concat2D(a, b);

        assertEquals(2, result.length);
        assertArrayEquals(new String[] { "a", "b", "e", "f" }, result[0]);
        assertArrayEquals(new String[] { "c", "d" }, result[1]);

        String[][] nullResult = Array.concat2D((String[][]) null, (String[][]) null);
        assertNull(nullResult);
    }

    @Test
    public void test_concat_3D() {
        String[][][] a = { { { "a", "b" } } };
        String[][][] b = { { { "c", "d" } } };
        String[][][] result = Array.concat3D(a, b);

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
        byte[] unboxed = Array.unbox((byte) 1, (byte) 2, (byte) 3);
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
        short[] unboxed = Array.unbox((short) 1, (short) 2, (short) 3);
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
    public void test_concat_generic_nullHandling() {
        String[][] a = { { "a", "b" } };
        String[][] b = null;
        String[][] result = Array.concat2D(a, b);
        assertNotNull(result);
        assertEquals(1, result.length);

        String[][] nullBoth = Array.concat2D((String[][]) null, (String[][]) null);
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

    @Test
    public void testNewInstance_withLength() {
        Integer[] intArray = Array.newInstance(Integer.class, 5);
        assertNotNull(intArray);
        assertEquals(5, intArray.length);
        assertEquals(Integer.class, intArray.getClass().getComponentType());

        String[] stringArray = Array.newInstance(String.class, 0);
        assertNotNull(stringArray);
        assertEquals(0, stringArray.length);
        String[] stringArray2 = Array.newInstance(String.class, 0);
        assertSame(stringArray, stringArray2, "Zero-length arrays of the same type should be cached and be the same instance.");

        int[] primitiveIntArray = Array.newInstance(int.class, 3);
        assertNotNull(primitiveIntArray);
        assertEquals(3, primitiveIntArray.length);
        assertEquals(int.class, primitiveIntArray.getClass().getComponentType());

        assertThrows(NegativeArraySizeException.class, () -> Array.newInstance(Integer.class, -1));
    }

    @Test
    public void testNewInstance_withDimensions() {
        Integer[][] matrix = Array.newInstance(Integer.class, 2, 3);
        assertNotNull(matrix);
        assertEquals(2, matrix.length);
        assertNotNull(matrix[0]);
        assertEquals(3, matrix[0].length);
        assertEquals(Integer.class, matrix[0].getClass().getComponentType());

        String[][][] cube = Array.newInstance(String.class, 1, 2, 3);
        assertNotNull(cube);
        assertEquals(1, cube.length);
        assertNotNull(cube[0]);
        assertEquals(2, cube[0].length);
        assertNotNull(cube[0][0]);
        assertEquals(3, cube[0][0].length);

        int[] singleDimension = Array.newInstance(int.class, 5);
        assertEquals(5, singleDimension.length);

        assertThrows(NullPointerException.class, () -> Array.newInstance(null, 1, 2));
        assertThrows(NegativeArraySizeException.class, () -> Array.newInstance(Integer.class, 1, -1, 2));
        assertThrows(NegativeArraySizeException.class, () -> Array.newInstance(Integer.class, -1));

        Integer[][] zeroDimMatrix = Array.newInstance(Integer.class, 0, 5);
        assertEquals(0, zeroDimMatrix.length);

        Integer[][] zeroInnerDimMatrix = Array.newInstance(Integer.class, 5, 0);
        assertEquals(5, zeroInnerDimMatrix.length);
        if (zeroInnerDimMatrix.length > 0) {
            assertEquals(0, zeroInnerDimMatrix[0].length);
        }
    }

    @Test
    public void testGetLength() {
        assertEquals(0, Array.getLength(null));
        assertEquals(0, Array.getLength(new int[0]));
        assertEquals(3, Array.getLength(new int[] { 1, 2, 3 }));
        assertEquals(2, Array.getLength(new String[] { "a", "b" }));

        assertThrows(IllegalArgumentException.class, () -> Array.getLength("not an array"));
        assertThrows(IllegalArgumentException.class, () -> Array.getLength(123));
    }

    @Test
    public void testGet() {
        Integer[] objArray = { 10, 20, 30 };
        assertEquals(20, (Integer) Array.get(objArray, 1));

        int[] primArray = { 1, 2, 3 };
        assertEquals(2, (Integer) Array.get(primArray, 1));

        assertThrows(IllegalArgumentException.class, () -> Array.get("not an array", 0));
        assertThrows(ArrayIndexOutOfBoundsException.class, () -> Array.get(primArray, -1));
        assertThrows(ArrayIndexOutOfBoundsException.class, () -> Array.get(primArray, 3));
        assertThrows(NullPointerException.class, () -> Array.get(null, 0));
    }

    @Test
    public void testGetBoolean() {
        boolean[] array = { true, false, true };
        assertTrue(Array.getBoolean(array, 0));
        assertFalse(Array.getBoolean(array, 1));
        assertThrows(IllegalArgumentException.class, () -> Array.getBoolean("not an array", 0));
        assertThrows(ArrayIndexOutOfBoundsException.class, () -> Array.getBoolean(array, 3));
    }

    @Test
    public void testGetByte() {
        byte[] array = { 10, 20, 30 };
        assertEquals((byte) 20, Array.getByte(array, 1));
        assertThrows(ArrayIndexOutOfBoundsException.class, () -> Array.getByte(array, 3));
    }

    @Test
    public void testGetChar() {
        char[] array = { 'a', 'b', 'c' };
        assertEquals('b', Array.getChar(array, 1));
        assertThrows(ArrayIndexOutOfBoundsException.class, () -> Array.getChar(array, 3));
    }

    @Test
    public void testGetShort() {
        short[] array = { 100, 200, 300 };
        assertEquals((short) 200, Array.getShort(array, 1));
        assertThrows(ArrayIndexOutOfBoundsException.class, () -> Array.getShort(array, 3));
    }

    @Test
    public void testGetInt() {
        int[] array = { 1000, 2000, 3000 };
        assertEquals(2000, Array.getInt(array, 1));
        assertThrows(ArrayIndexOutOfBoundsException.class, () -> Array.getInt(array, 3));
    }

    @Test
    public void testGetLong() {
        long[] array = { 1L, 2L, 3L };
        assertEquals(2L, Array.getLong(array, 1));
        assertThrows(ArrayIndexOutOfBoundsException.class, () -> Array.getLong(array, 3));
    }

    @Test
    public void testGetFloat() {
        float[] array = { 1.1f, 2.2f, 3.3f };
        assertEquals(2.2f, Array.getFloat(array, 1), 0.001f);
        assertThrows(ArrayIndexOutOfBoundsException.class, () -> Array.getFloat(array, 3));
    }

    @Test
    public void testGetDouble() {
        double[] array = { 1.1, 2.2, 3.3 };
        assertEquals(2.2, Array.getDouble(array, 1), 0.001);
        assertThrows(ArrayIndexOutOfBoundsException.class, () -> Array.getDouble(array, 3));
    }

    @Test
    public void testSet() {
        Integer[] objArray = { 10, 20, 30 };
        Array.set(objArray, 1, 25);
        assertEquals(25, objArray[1]);

        int[] primArray = { 1, 2, 3 };
        Array.set(primArray, 1, 5);
        assertEquals(5, primArray[1]);

        assertThrows(IllegalArgumentException.class, () -> Array.set("not an array", 0, 1));
        assertThrows(ArrayIndexOutOfBoundsException.class, () -> Array.set(primArray, 3, 1));
        assertThrows(IllegalArgumentException.class, () -> Array.set(objArray, 0, "string"));
    }

    @Test
    public void testSetBoolean() {
        boolean[] array = { true, false, true };
        Array.setBoolean(array, 1, true);
        assertTrue(array[1]);
        assertThrows(ArrayIndexOutOfBoundsException.class, () -> Array.setBoolean(array, 3, false));
    }

    @Test
    public void testSetByte() {
        byte[] array = { 10, 20, 30 };
        Array.setByte(array, 1, (byte) 25);
        assertEquals((byte) 25, array[1]);
    }

    @Test
    public void testSetChar() {
        char[] array = { 'a', 'b', 'c' };
        Array.setChar(array, 1, 'x');
        assertEquals('x', array[1]);
    }

    @Test
    public void testSetShort() {
        short[] array = { 100, 200, 300 };
        Array.setShort(array, 1, (short) 250);
        assertEquals((short) 250, array[1]);
    }

    @Test
    public void testSetInt() {
        int[] array = { 1000, 2000, 3000 };
        Array.setInt(array, 1, 2500);
        assertEquals(2500, array[1]);
    }

    @Test
    public void testSetLong() {
        long[] array = { 1L, 2L, 3L };
        Array.setLong(array, 1, 4L);
        assertEquals(4L, array[1]);
    }

    @Test
    public void testSetFloat() {
        float[] array = { 1.1f, 2.2f, 3.3f };
        Array.setFloat(array, 1, 4.4f);
        assertEquals(4.4f, array[1], 0.001f);
    }

    @Test
    public void testSetDouble() {
        double[] array = { 1.1, 2.2, 3.3 };
        Array.setDouble(array, 1, 4.4);
        assertEquals(4.4, array[1], 0.001);
    }

    @Test
    public void testAsList() {
        List<String> list1 = Array.asList("a", "b", "c");
        assertEquals(3, list1.size());
        assertEquals("b", list1.get(1));

        List<Integer> list2 = Array.asList();
        assertTrue(list2.isEmpty());

        List<Integer> list3 = Array.asList((Integer[]) null);
        assertTrue(list3.isEmpty());

        String[] originalArray = { "x", "y", "z" };
        List<String> backedList = Array.asList(originalArray);
        backedList.set(0, "X");
        assertEquals("X", originalArray[0]);
        originalArray[1] = "Y";
        assertEquals("Y", backedList.get(1));
    }

    @Test
    public void testOfPrimitives() {
        assertArrayEquals(new boolean[] { true, false }, Array.of(true, false));
        assertArrayEquals(new char[] { 'a', 'b' }, Array.of('a', 'b'));
        assertArrayEquals(new byte[] { 1, 2 }, Array.of((byte) 1, (byte) 2));
        assertArrayEquals(new short[] { 10, 20 }, Array.of((short) 10, (short) 20));
        assertArrayEquals(new int[] { 100, 200 }, Array.of(100, 200));
        assertArrayEquals(new long[] { 1000L, 2000L }, Array.of(1000L, 2000L));
        assertArrayEquals(new float[] { 1.0f, 2.0f }, Array.of(1.0f, 2.0f), 0.001f);
        assertArrayEquals(new double[] { 1.0, 2.0 }, Array.of(1.0, 2.0), 0.001);
        assertArrayEquals(new String[] { "hello", "world" }, Array.of("hello", "world"));

        assertArrayEquals(new int[0], Array.of(CommonUtil.EMPTY_INT_ARRAY));
    }

    @Test
    public void testOfObjects() {
        Date d1 = new Date(1000L);
        Date d2 = new Date(2000L);
        assertArrayEquals(new Date[] { d1, d2 }, Array.of(d1, d2));

        Calendar c1 = Calendar.getInstance();
        Calendar c2 = Calendar.getInstance();
        c2.add(Calendar.DAY_OF_MONTH, 1);
        assertArrayEquals(new Calendar[] { c1, c2 }, Array.of(c1, c2));

        LocalDate t1 = LocalDate.now();
        LocalDateTime t2 = LocalDateTime.now();
        assertArrayEquals(new Temporal[] { t1, t2 }, Array.of(t1, t2));

        enum MyEnum {
            A, B
        }
        assertArrayEquals(new MyEnum[] { MyEnum.A, MyEnum.B }, Array.of(MyEnum.A, MyEnum.B));
    }

    @Test
    @SuppressWarnings("deprecation")
    public void testOF_deprecated() {
        Integer[] nums = { 1, 2, 3 };
        assertArrayEquals(nums, Array.oF(1, 2, 3));
        String[] strs = { "a", "b" };
        assertArrayEquals(strs, Array.oF("a", "b"));
        assertArrayEquals(new Object[0], Array.oF());
    }

    @Test
    public void testRange_char() {
        assertArrayEquals(new char[] { 'a', 'b', 'c' }, Array.range('a', 'd'));
        assertArrayEquals(CommonUtil.EMPTY_CHAR_ARRAY, Array.range('d', 'a'));
        assertArrayEquals(CommonUtil.EMPTY_CHAR_ARRAY, Array.range('a', 'a'));
    }

    @Test
    public void testRange_byte() {
        assertArrayEquals(new byte[] { 10, 11, 12 }, Array.range((byte) 10, (byte) 13));
        assertArrayEquals(CommonUtil.EMPTY_BYTE_ARRAY, Array.range((byte) 13, (byte) 10));
    }

    @Test
    public void testRange_short() {
        assertArrayEquals(new short[] { 5, 6, 7 }, Array.range((short) 5, (short) 8));
        assertArrayEquals(CommonUtil.EMPTY_SHORT_ARRAY, Array.range((short) 8, (short) 5));
    }

    @Test
    public void testRange_int() {
        assertArrayEquals(new int[] { 1, 2, 3 }, Array.range(1, 4));
        assertArrayEquals(CommonUtil.EMPTY_INT_ARRAY, Array.range(4, 1));
    }

    @Test
    public void testRange_int_continued() {
        assertArrayEquals(new int[] { 1, 2, 3 }, Array.range(1, 4));
        assertArrayEquals(CommonUtil.EMPTY_INT_ARRAY, Array.range(4, 1));
        assertArrayEquals(CommonUtil.EMPTY_INT_ARRAY, Array.range(1, 1));
    }

    @Test
    public void testRange_long() {
        assertArrayEquals(new long[] { 1L, 2L, 3L }, Array.range(1L, 4L));
        assertArrayEquals(CommonUtil.EMPTY_LONG_ARRAY, Array.range(4L, 1L));
        assertArrayEquals(CommonUtil.EMPTY_LONG_ARRAY, Array.range(1L, 1L));
    }

    @Test
    public void testRange_withStep_char() {
        assertArrayEquals(new char[] { 'a', 'c', 'e' }, Array.range('a', 'f', 2));
        assertArrayEquals(new char[] { 'e', 'c', 'a' }, Array.range('e', (char) ('a' - 1), -2));
        assertArrayEquals(CommonUtil.EMPTY_CHAR_ARRAY, Array.range('a', 'f', -1));
        assertArrayEquals(CommonUtil.EMPTY_CHAR_ARRAY, Array.range('a', 'a', 1));
        assertThrows(IllegalArgumentException.class, () -> Array.range('a', 'f', 0));
    }

    @Test
    public void testRange_withStep_byte() {
        assertArrayEquals(new byte[] { 10, 12, 14 }, Array.range((byte) 10, (byte) 15, (byte) 2));
        assertArrayEquals(new byte[] { 14, 12, 10 }, Array.range((byte) 14, (byte) 9, (byte) -2));
        assertArrayEquals(CommonUtil.EMPTY_BYTE_ARRAY, Array.range((byte) 10, (byte) 10, (byte) 1));
        assertThrows(IllegalArgumentException.class, () -> Array.range((byte) 10, (byte) 15, (byte) 0));
    }

    @Test
    public void testRange_withStep_short() {
        assertArrayEquals(new short[] { 100, 102, 104 }, Array.range((short) 100, (short) 105, (short) 2));
        assertArrayEquals(new short[] { 104, 102, 100 }, Array.range((short) 104, (short) 99, (short) -2));
        assertThrows(IllegalArgumentException.class, () -> Array.range((short) 100, (short) 105, (short) 0));
    }

    @Test
    public void testRange_withStep_int() {
        assertArrayEquals(new int[] { 1, 3, 5 }, Array.range(1, 6, 2));
        assertArrayEquals(new int[] { 5, 3, 1 }, Array.range(5, 0, -2));
        assertArrayEquals(CommonUtil.EMPTY_INT_ARRAY, Array.range(1, 6, -1));
        assertArrayEquals(CommonUtil.EMPTY_INT_ARRAY, Array.range(1, 1, 1));
        assertThrows(IllegalArgumentException.class, () -> Array.range(1, 10, 0));
        assertArrayEquals(new int[] {}, Array.range(0, (int) ((Integer.MAX_VALUE * 2L)), 1));
    }

    @Test
    public void testRange_withStep_long() {
        assertArrayEquals(new long[] { 1L, 3L, 5L }, Array.range(1L, 6L, 2L));
        assertArrayEquals(new long[] { 5L, 3L, 1L }, Array.range(5L, 0L, -2L));
        assertThrows(IllegalArgumentException.class, () -> Array.range(1L, 10L, 0L));
        assertThrows(IllegalArgumentException.class, () -> Array.range(0L, Long.MAX_VALUE, 1L));
    }

    @Test
    public void testRangeClosed_char() {
        assertArrayEquals(new char[] { 'a', 'b', 'c', 'd' }, Array.rangeClosed('a', 'd'));
        assertArrayEquals(new char[] { 'a' }, Array.rangeClosed('a', 'a'));
        assertArrayEquals(CommonUtil.EMPTY_CHAR_ARRAY, Array.rangeClosed('d', 'a'));
    }

    @Test
    public void testRangeClosed_byte() {
        assertArrayEquals(new byte[] { 10, 11, 12, 13 }, Array.rangeClosed((byte) 10, (byte) 13));
        assertArrayEquals(new byte[] { 10 }, Array.rangeClosed((byte) 10, (byte) 10));
        assertArrayEquals(CommonUtil.EMPTY_BYTE_ARRAY, Array.rangeClosed((byte) 13, (byte) 10));
    }

    @Test
    public void testRangeClosed_short() {
        assertArrayEquals(new short[] { 5, 6, 7, 8 }, Array.rangeClosed((short) 5, (short) 8));
        assertArrayEquals(new short[] { 5 }, Array.rangeClosed((short) 5, (short) 5));
        assertArrayEquals(CommonUtil.EMPTY_SHORT_ARRAY, Array.rangeClosed((short) 8, (short) 5));
    }

    @Test
    public void testRangeClosed_int() {
        assertArrayEquals(new int[] { 1, 2, 3, 4 }, Array.rangeClosed(1, 4));
        assertArrayEquals(new int[] { 1 }, Array.rangeClosed(1, 1));
        assertArrayEquals(CommonUtil.EMPTY_INT_ARRAY, Array.rangeClosed(4, 1));
        assertThrows(IllegalArgumentException.class, () -> Array.rangeClosed(0, Integer.MAX_VALUE));
    }

    @Test
    public void testRangeClosed_long() {
        assertArrayEquals(new long[] { 1L, 2L, 3L, 4L }, Array.rangeClosed(1L, 4L));
        assertArrayEquals(new long[] { 1L }, Array.rangeClosed(1L, 1L));
        assertArrayEquals(CommonUtil.EMPTY_LONG_ARRAY, Array.rangeClosed(4L, 1L));
        assertThrows(IllegalArgumentException.class, () -> Array.rangeClosed(0L, Integer.MAX_VALUE + 1L));
    }

    @Test
    public void testRangeClosed_withStep_char() {
        assertArrayEquals(new char[] { 'a', 'c', 'e' }, Array.rangeClosed('a', 'e', 2));
        assertArrayEquals(new char[] { 'e', 'c', 'a' }, Array.rangeClosed('e', 'a', -2));
        assertArrayEquals(new char[] { 'a' }, Array.rangeClosed('a', 'a', 1));
        assertArrayEquals(new char[] { 'a' }, Array.rangeClosed('a', 'a', -1));
        assertArrayEquals(CommonUtil.EMPTY_CHAR_ARRAY, Array.rangeClosed('a', 'e', -1));
        assertThrows(IllegalArgumentException.class, () -> Array.rangeClosed('a', 'e', 0));
    }

    @Test
    public void testRangeClosed_withStep_byte() {
        assertArrayEquals(new byte[] { 10, 12, 14 }, Array.rangeClosed((byte) 10, (byte) 14, (byte) 2));
        assertArrayEquals(new byte[] { 14, 12, 10 }, Array.rangeClosed((byte) 14, (byte) 10, (byte) -2));
        assertArrayEquals(new byte[] { 10 }, Array.rangeClosed((byte) 10, (byte) 10, (byte) 1));
        assertThrows(IllegalArgumentException.class, () -> Array.rangeClosed((byte) 10, (byte) 14, (byte) 0));
    }

    @Test
    public void testRangeClosed_withStep_short() {
        assertArrayEquals(new short[] { 100, 102, 104 }, Array.rangeClosed((short) 100, (short) 104, (short) 2));
        assertArrayEquals(new short[] { 104, 102, 100 }, Array.rangeClosed((short) 104, (short) 100, (short) -2));
        assertArrayEquals(new short[] { 100 }, Array.rangeClosed((short) 100, (short) 100, (short) 1));
        assertThrows(IllegalArgumentException.class, () -> Array.rangeClosed((short) 100, (short) 104, (short) 0));
    }

    @Test
    public void testRangeClosed_withStep_int() {
        assertArrayEquals(new int[] { 1, 3, 5 }, Array.rangeClosed(1, 5, 2));
        assertArrayEquals(new int[] { 5, 3, 1 }, Array.rangeClosed(5, 1, -2));
        assertArrayEquals(new int[] { 1 }, Array.rangeClosed(1, 1, 1));
        assertThrows(IllegalArgumentException.class, () -> Array.rangeClosed(1, 10, 0));
        assertThrows(IllegalArgumentException.class, () -> Array.rangeClosed(0, Integer.MAX_VALUE, 1));
    }

    @Test
    public void testRangeClosed_withStep_long() {
        assertArrayEquals(new long[] { 1L, 3L, 5L }, Array.rangeClosed(1L, 5L, 2L));
        assertArrayEquals(new long[] { 5L, 3L, 1L }, Array.rangeClosed(5L, 1L, -2L));
        assertThrows(IllegalArgumentException.class, () -> Array.rangeClosed(1L, 10L, 0L));
        assertThrows(IllegalArgumentException.class, () -> Array.rangeClosed(0L, Long.MAX_VALUE, 2L));
    }

    @Test
    public void testRepeat_primitivesAndString() {
        assertArrayEquals(new boolean[] { true, true, true }, Array.repeat(true, 3));
        assertArrayEquals(new char[] { 'a', 'a' }, Array.repeat('a', 2));
        assertArrayEquals(new byte[] { 7, 7, 7, 7 }, Array.repeat((byte) 7, 4));
        assertArrayEquals(new short[] { 42, 42 }, Array.repeat((short) 42, 2));
        assertArrayEquals(new int[] { 10, 10, 10 }, Array.repeat(10, 3));
        assertArrayEquals(new long[] { 5L, 5L }, Array.repeat(5L, 2));
        assertArrayEquals(new float[] { 3.0f, 3.0f, 3.0f }, Array.repeat(3.0f, 3), 0.001f);
        assertArrayEquals(new double[] { 7.0, 7.0 }, Array.repeat(7.0, 2), 0.001);
        assertArrayEquals(new String[] { "hi", "hi", "hi" }, Array.repeat("hi", 3));

        assertArrayEquals(new int[0], Array.repeat(5, 0));
        assertThrows(IllegalArgumentException.class, () -> Array.repeat(5, -1));
    }

    @Test
    @SuppressWarnings("deprecation")
    public void testRepeat_generic_deprecated() {
        int[] expected = { 7, 7, 7 };
        assertArrayEquals(expected, Array.repeat(7, 3));

        String[] strExpected = { "test", "test" };
        assertArrayEquals(strExpected, Array.repeat("test", 2));

        assertArrayEquals(new String[] { null, null, null }, Array.repeat((String) null, 3));
    }

    @Test
    public void testRepeat_generic_withClass() {
        Integer[] intResult = Array.repeat(7, 3, Integer.class);
        assertArrayEquals(new Integer[] { 7, 7, 7 }, intResult);
        assertEquals(Integer.class, intResult.getClass().getComponentType());

        String[] strResult = Array.repeat("test", 2, String.class);
        assertArrayEquals(new String[] { "test", "test" }, strResult);

        Integer[] nullResult = Array.repeat((Integer) null, 3, Integer.class);
        assertArrayEquals(new Integer[] { null, null, null }, nullResult);

        Object[] objResult = Array.repeat("String", 2, Object.class);
        assertArrayEquals(new Object[] { "String", "String" }, objResult);
    }

    @Test
    public void testRepeatNonNull_generic() {
        Integer[] expected = { 7, 7, 7 };
        assertArrayEquals(expected, Array.repeatNonNull(7, 3));

        String[] strExpected = { "test", "test" };
        assertArrayEquals(strExpected, Array.repeatNonNull("test", 2));

        assertThrows(IllegalArgumentException.class, () -> Array.repeatNonNull(null, 3));
    }

    @Test
    public void testRandom_len() {
        int[] r1 = Array.random(5);
        assertEquals(5, r1.length);

        int[] r2 = Array.random(0);
        assertEquals(0, r2.length);

        assertThrows(NegativeArraySizeException.class, () -> Array.random(-1));
        if (r1.length > 0) {
        }
    }

    @Test
    public void testRandom_startEndLen() {
        int[] r1 = Array.random(10, 20, 5);
        assertEquals(5, r1.length);
        for (int val : r1) {
            assertTrue(val >= 10 && val < 20);
        }

        int[] r2 = Array.random(0, 1, 0);
        assertEquals(0, r2.length);

        assertThrows(IllegalArgumentException.class, () -> Array.random(10, 5, 3));
        assertThrows(NegativeArraySizeException.class, () -> Array.random(1, 5, -1));

        int[] rLarge = Array.random(0, (int) (((long) Integer.MAX_VALUE / 2) + 10), 2);
        assertEquals(2, rLarge.length);
        for (int val : rLarge) {
            assertTrue(val >= 0 && val < (int) (((long) Integer.MAX_VALUE / 2) + 10));
        }
    }

    @Test
    public void testConcat_boolean_2D() {
        boolean[][] a = { { true }, { false, true } };
        boolean[][] b = { { false } };
        boolean[][] expected = { { true, false }, { false, true } };

        boolean[][] res1 = Array.concat(new boolean[][] { { true } }, new boolean[][] { { false } });
        assertArrayEquals(new boolean[] { true, false }, res1[0]);

        boolean[][] a2 = { { true }, { false } };
        boolean[][] b2 = { { true } };
        boolean[][] res2 = Array.concat(a2, b2);
        assertArrayEquals(new boolean[] { true, true }, res2[0]);
        assertArrayEquals(new boolean[] { false }, res2[1]);

        boolean[][] a3 = { { true } };
        boolean[][] b3 = { { false }, { true } };
        boolean[][] res3 = Array.concat(a3, b3);
        assertArrayEquals(new boolean[] { true, false }, res3[0]);
        assertArrayEquals(new boolean[] { true }, res3[1]);

        assertArrayEquals(new boolean[0][], Array.concat(new boolean[0][], new boolean[0][]));
        boolean[][] arr = { { true } };
        boolean[][] resClone = Array.concat(arr, new boolean[0][]);
        assertArrayEquals(arr[0], resClone[0]);
        assertNotSame(arr, resClone);
    }

    @Test
    public void testConcat_int_3D() {
        int[][][] a = { { { 1 }, { 2 } }, { { 3 } } };
        int[][][] b = { { { 4 } } };

        int[][][] res = Array.concat(a, b);
        assertEquals(2, res.length);
        assertEquals(2, res[0].length);
        assertArrayEquals(new int[] { 1, 4 }, res[0][0]);
        assertArrayEquals(new int[] { 2 }, res[0][1]);
        assertEquals(1, res[1].length);
        assertArrayEquals(new int[] { 3 }, res[1][0]);
    }

    @Test
    public void testConcatt_generic_2D() {
        String[][] a = { { "a" }, { "b", "c" } };
        String[][] b = { { "d" } };
        String[][] res = Array.concat2D(a, b);

        assertEquals(2, res.length);
        assertArrayEquals(new String[] { "a", "d" }, res[0]);
        assertArrayEquals(new String[] { "b", "c" }, res[1]);

        String[][] c = null;
        String[][] d = { { "x" } };
        String[][] res2 = Array.concat2D(c, d);
        assertNotNull(res2);
        assertEquals(1, res2.length);
        assertArrayEquals(new String[] { "x" }, res2[0]);
        assertNotSame(d, res2);

        String[][] res3 = Array.concat2D(d, c);
        assertNotNull(res3);
        assertArrayEquals(new String[] { "x" }, res3[0]);
    }

    @Test
    public void testBox_boolean() {
        assertNull(Array.box((boolean[]) null));
        assertArrayEquals(CommonUtil.EMPTY_BOOLEAN_OBJ_ARRAY, Array.box(new boolean[0]));
        assertArrayEquals(new Boolean[] { true, false }, Array.box(true, false));

        boolean[] prims = { true, false, true, false };
        assertArrayEquals(new Boolean[] { false, true }, Array.box(prims, 1, 3));
        assertThrows(IndexOutOfBoundsException.class, () -> Array.box(prims, 0, 5));
    }

    @Test
    public void testBox_int_2D_3D() {
        int[][] p2d = { { 1, 2 }, { 3 } };
        Integer[][] b2d = Array.box(p2d);
        assertArrayEquals(new Integer[] { 1, 2 }, b2d[0]);
        assertArrayEquals(new Integer[] { 3 }, b2d[1]);

        int[][][] p3d = { { { 1 } }, { { 2, 3 } } };
        Integer[][][] b3d = Array.box(p3d);
        assertArrayEquals(new Integer[] { 1 }, b3d[0][0]);
        assertArrayEquals(new Integer[] { 2, 3 }, b3d[1][0]);
    }

    @Test
    public void testUnbox_Integer_withDefault() {
        assertNull(Array.unbox((Integer[]) null));
        assertArrayEquals(CommonUtil.EMPTY_INT_ARRAY, Array.unbox(new Integer[0]));

        Integer[] boxed = { 1, null, 3 };
        assertArrayEquals(new int[] { 1, 0, 3 }, Array.unbox(boxed));
        assertArrayEquals(new int[] { 1, 99, 3 }, Array.unbox(boxed, 99));

        Integer[] data = { 10, null, 30, 40, null };
        assertArrayEquals(new int[] { 99, 30 }, Array.unbox(data, 1, 3, 99));
        assertThrows(IndexOutOfBoundsException.class, () -> Array.unbox(data, 0, 6, 0));
    }

    @Test
    public void testUnbox_Double_2D_3D_withDefault() {
        Double[][] b2d = { { 1.0, null }, { 3.0 } };
        double[][] p2d = Array.unbox(b2d, 0.5);
        assertArrayEquals(new double[] { 1.0, 0.5 }, p2d[0], 0.01);
        assertArrayEquals(new double[] { 3.0 }, p2d[1], 0.01);

        Double[][][] b3d = { { { null } }, { { 2.0, 3.0 } } };
        double[][][] p3d = Array.unbox(b3d, -1.0);
        assertArrayEquals(new double[] { -1.0 }, p3d[0][0], 0.01);
        assertArrayEquals(new double[] { 2.0, 3.0 }, p3d[1][0], 0.01);
    }

    @Test
    public void testTranspose_int() {
        assertNull(Array.transpose((int[][]) null));
        assertArrayEquals(new int[0][], Array.transpose(new int[0][]));

        int[][] m1 = { { 1, 2, 3 }, { 4, 5, 6 } };
        int[][] t1 = { { 1, 4 }, { 2, 5 }, { 3, 6 } };
        assertArrayEquals(t1, Array.transpose(m1));

        int[][] m2 = { { 1, 2 }, { 3, 4 } };
        int[][] t2 = { { 1, 3 }, { 2, 4 } };
        assertArrayEquals(t2, Array.transpose(m2));

        int[][] m3 = { { 1 }, { 2 }, { 3 } };
        int[][] t3 = { { 1, 2, 3 } };
        assertArrayEquals(t3, Array.transpose(m3));

        int[][] m4 = { {} };
        int[][] t4 = {};
        assertArrayEquals(new int[0][1], Array.transpose(m4));

        int[][] jagged = { { 1, 2 }, { 3, 4, 5 } };
        assertThrows(IllegalArgumentException.class, () -> Array.transpose(jagged));
    }

    @Test
    public void testTranspose_generic() {
        String[][] m1 = { { "a", "b" }, { "c", "d" } };
        String[][] t1 = { { "a", "c" }, { "b", "d" } };
        assertArrayEquals(t1, Array.transpose(m1));

        Date date = new Date();
        Object[][] m2 = { { 1, "a" }, { date, 3.0 } };
        Object[][] t2 = Array.transpose(m2);
        assertEquals(Object.class, t2.getClass().getComponentType().getComponentType());
        assertArrayEquals(new Object[] { 1, date }, t2[0]);
        assertArrayEquals(new Object[] { "a", 3.0 }, t2[1]);

        String[][] m3 = { {}, {} };
        String[][] t3 = Array.transpose(m3);
        assertEquals(0, t3.length);

    }

    @Test
    public void testNewInstance_withEmptyDimensions() {
        assertThrows(IllegalArgumentException.class, () -> Array.newInstance(String.class, new int[0]));

        assertThrows(IllegalArgumentException.class, () -> {
            String[] arr = Array.newInstance(String.class, new int[0]);
            System.out.println(arr);
        });

        assertThrows(IllegalArgumentException.class, () -> {
            @SuppressWarnings("unused")
            int val = Array.newInstance(int.class, new int[0]);
        });
    }

    @Test
    public void testNewInstance_componentTypeIsArray() {
        Integer[][] arrayOfStringArrays = Array.newInstance(Integer[].class, 3);
        assertNotNull(arrayOfStringArrays);
        assertEquals(3, arrayOfStringArrays.length);
        assertEquals(Integer[].class, arrayOfStringArrays.getClass().getComponentType());
        assertNull(arrayOfStringArrays[0]);
    }

    @Test
    public void testRange_long_withStep_BigIntegerPath_Overflow() {
        assertThrows(IllegalArgumentException.class, () -> Array.range(Long.MIN_VALUE, Long.MAX_VALUE, 1L),
                "Should throw due to calculated length exceeding Integer.MAX_VALUE");

        assertEquals(CommonUtil.EMPTY_LONG_ARRAY, Array.range(Long.MAX_VALUE - 10, Long.MIN_VALUE + 10, 1L));
    }

    @Test
    public void testRange_long_withStep_BigIntegerPath_ValidSmallArray() {
        long[] result = Array.range(0L, Long.MAX_VALUE, Long.MAX_VALUE / 2L);
        assertEquals(3, result.length);
        assertArrayEquals(new long[] { 0L, Long.MAX_VALUE / 2L, Long.MAX_VALUE / 2L * 2L }, result);

        long[] result2 = Array.range(Long.MIN_VALUE, Long.MIN_VALUE + 10, 3L);
        assertArrayEquals(new long[] { Long.MIN_VALUE, Long.MIN_VALUE + 3, Long.MIN_VALUE + 6, Long.MIN_VALUE + 9 }, result2);

        long[] result3 = Array.range(Long.MIN_VALUE, Long.MAX_VALUE, Long.MAX_VALUE);
        assertEquals(3, result3.length);
        assertArrayEquals(new long[] { Long.MIN_VALUE, Long.MIN_VALUE + Long.MAX_VALUE, Long.MIN_VALUE + Long.MAX_VALUE + Long.MAX_VALUE }, result3);
    }

    @Test
    public void testRangeClosed_long_withStep_BigIntegerPath_Overflow() {
        assertThrows(IllegalArgumentException.class, () -> Array.rangeClosed(Long.MIN_VALUE, Long.MAX_VALUE, 1L),
                "Should throw due to calculated length exceeding Integer.MAX_VALUE");

    }

    @Test
    public void testRangeClosed_long_withStep_BigIntegerPath_ValidSmallArray() {
        long[] result = Array.rangeClosed(Long.MIN_VALUE, Long.MAX_VALUE, Long.MAX_VALUE);
        assertEquals(3, result.length);
        assertArrayEquals(new long[] { Long.MIN_VALUE, Long.MIN_VALUE + Long.MAX_VALUE, Long.MIN_VALUE + 2 * Long.MAX_VALUE }, result);

        long[] r = Array.rangeClosed(0L, 0L, 1L);
        assertArrayEquals(new long[] { 0L }, r);

        long[] r2 = Array.rangeClosed(0L, 5L, 2L);
        assertArrayEquals(new long[] { 0L, 2L, 4L }, r2);

        long[] r3 = Array.rangeClosed(0L, Long.MAX_VALUE, Long.MAX_VALUE / 2L);
        assertEquals(3, r3.length);
        assertArrayEquals(new long[] { 0L, Long.MAX_VALUE / 2L, (Long.MAX_VALUE / 2L) * 2L }, r3);
    }

    @Test
    public void testConcat_int_3D_VariedStructures() {
        int[][][] a1 = { { { 1, 2 }, { 3 } } };
        int[][][] b1 = { { { 4 } }, { { 5, 6 }, { 7 } } };
        int[][][] result1 = Array.concat(a1, b1);
        assertEquals(2, result1.length);
        assertNotNull(result1[0]);
        assertEquals(2, result1[0].length);
        assertArrayEquals(new int[] { 1, 2, 4 }, result1[0][0]);
        assertArrayEquals(new int[] { 3 }, result1[0][1]);
        assertNotNull(result1[1]);
        assertEquals(2, result1[1].length);
        assertArrayEquals(new int[] { 5, 6 }, result1[1][0]);
        assertArrayEquals(new int[] { 7 }, result1[1][1]);

        int[][][] a2 = { { { 1 }, null }, { {} } };
        int[][][] b2 = { { null, { 2 } } };

        int[][][] result2 = Array.concat(a2, b2);
        assertEquals(2, result2.length);
        assertEquals(2, result2[0].length);
        assertArrayEquals(new int[] { 1 }, result2[0][0]);
        assertArrayEquals(new int[] { 2 }, result2[0][1]);

        assertEquals(1, result2[1].length);
        assertArrayEquals(CommonUtil.EMPTY_INT_ARRAY, result2[1][0]);
    }

    @Test
    public void testConcatt_String_3D_EmptyAndNulls() {
        String[][][] a = { { { "a" }, null }, { {} } };
        String[][][] b = { { null, { "b" } } };
        String[][][] result = Array.concat3D(a, b);

        assertEquals(2, result.length);
        assertEquals(String[][].class, result.getClass().getComponentType());

        assertEquals(2, result[0].length);
        assertEquals(String[].class, result[0].getClass().getComponentType());
        assertArrayEquals(new String[] { "a" }, result[0][0]);
        assertArrayEquals(new String[] { "b" }, result[0][1]);

        assertEquals(1, result[1].length);
        assertArrayEquals(CommonUtil.EMPTY_STRING_ARRAY, result[1][0]);

        String[][][] c = null;
        String[][][] d = { { { "x" } } };
        String[][][] resNull = Array.concat3D(c, d);
        assertNotNull(resNull);
        assertEquals(1, resNull.length);
        assertArrayEquals(new String[][] { { "x" } }, resNull[0]);
        assertNotSame(d, resNull);
    }

    @Test
    public void testTranspose_singleRowOrColumn_zeroDimension() {
        int[][] m1 = new int[1][0];
        int[][] t1 = Array.transpose(m1);
        assertEquals(0, t1.length, "Transposing a 1x0 matrix should result in a 0x1 matrix (outer length 0)");

        int[][] m2 = new int[0][1];
        int[][] t2 = Array.transpose(m2);
        assertEquals(0, t2.length);

        int[][] m3 = new int[0][0];
        int[][] t3 = Array.transpose(m3);
        assertEquals(0, t3.length);
    }

    @Test
    public void testTranspose_generic_singleRowOrColumn_zeroDimension() {
        String[][] m1 = new String[1][0];
        String[][] t1 = Array.transpose(m1);
        assertEquals(0, t1.length);

        String[][] m2 = new String[0][1];
        String[][] t2 = Array.transpose(m2);
        assertEquals(0, t2.length);

        String[][] m3 = new String[0][0];
        String[][] t3 = Array.transpose(m3);
        assertEquals(0, t3.length);
    }

    @Test
    public void testGet_arrayOfArrays() {
        String[][] arrayOfArrays = { { "a", "b" }, { "c", "d" } };
        String[] innerArray = Array.get(arrayOfArrays, 0);
        assertArrayEquals(new String[] { "a", "b" }, innerArray);

        Object obj = Array.get(arrayOfArrays, 1);
        assertInstanceOf(String[].class, obj);
        assertArrayEquals(new String[] { "c", "d" }, (String[]) obj);

        int[][] primitiveMatrix = { { 1, 2 }, { 3, 4 } };
        Object innerPrimArrayObj = Array.get(primitiveMatrix, 0);
        assertInstanceOf(int[].class, innerPrimArrayObj);
        assertArrayEquals(new int[] { 1, 2 }, (int[]) innerPrimArrayObj);
    }

    @Test
    public void testSet_arrayOfArrays() {
        String[][] arrayOfArrays = { { "a", "b" }, { "c", "d" } };
        String[] newInnerArray = { "x", "y", "z" };
        Array.set(arrayOfArrays, 0, newInnerArray);
        assertArrayEquals(newInnerArray, arrayOfArrays[0]);
        assertSame(newInnerArray, arrayOfArrays[0]);

        Object[][] looselyTypedArray = new Object[2][];
        Array.set(looselyTypedArray, 0, new String[] { "hello" });
        assertThrows(IllegalArgumentException.class, () -> {
            String[][] strictlyTypedArray = new String[1][];
            Array.set(strictlyTypedArray, 0, new Integer[] { 1, 2 });
        });
    }

    @Test
    public void testRange_long_withStep_BigIntegerPath_ExactEnd() {
        long[] result = Array.range(0L, 4L, 2L);
        assertArrayEquals(new long[] { 0L, 2L }, result);

        long step = (Long.MAX_VALUE / 2L) + 100L;
        long[] resultBigLenCalc = Array.range(0L, Long.MAX_VALUE, step);
        assertEquals(2, resultBigLenCalc.length);
        assertArrayEquals(new long[] { 0L, step }, resultBigLenCalc);
    }

    @Test
    public void testRange_long_withStep_BigIntegerPath_NonExactEnd() {
        long[] result = Array.range(0L, 5L, 2L);
        assertArrayEquals(new long[] { 0L, 2L, 4L }, result);

        long step = Long.MAX_VALUE / 3L;
        long endVal = Long.MAX_VALUE;
        long[] resultBigLenCalc = Array.range(0L, endVal, step);
        if (step == 0) {
            if (endVal > 0L)
                assertThrows(IllegalArgumentException.class, () -> Array.range(0L, endVal, step));
            else
                assertEquals(0, resultBigLenCalc.length);
        } else {
            long m = (endVal - 0L) / step;
            long calcLen = (m * step + 0L == endVal) ? m : m + 1;
            if (step < 0 && endVal > 0L)
                calcLen = 0;
            if (step > 0 && endVal < 0L)
                calcLen = 0;
            if (endVal == 0L && step != 0)
                calcLen = 0;

            assertEquals(calcLen, resultBigLenCalc.length, "Length calculation mismatch for large longs");
        }
    }

    @Test
    public void testTranspose_withNullInnerArray() {
        int[][] matrixWithNullRow = { { 1, 2 }, null, { 5, 6 } };
        assertThrows(IllegalArgumentException.class, () -> Array.transpose(matrixWithNullRow));

        String[][] matrixWithNullRowGeneric = { { "a", "b" }, null, { "e", "f" } };
        assertThrows(IllegalArgumentException.class, () -> Array.transpose(matrixWithNullRowGeneric));
    }

    @Test
    public void testTranspose_firstRowNull() {
        int[][] matrixFirstRowNull = { null, { 3, 4 } };
        assertThrows(IllegalArgumentException.class, () -> Array.transpose(matrixFirstRowNull));
    }

    @Test
    public void testTranspose_allRowsNull_consistentLengthZero() {
        int[][] matrixAllNull = { null, null, null };
        assertThrows(IllegalArgumentException.class, () -> Array.transpose(matrixAllNull));
    }

    @Test
    public void testBox_fromIndexEqualsToIndex() {
        boolean[] boolArray = { true, false, true };
        Boolean[] resultBool = Array.box(boolArray, 1, 1);
        assertArrayEquals(CommonUtil.EMPTY_BOOLEAN_OBJ_ARRAY, resultBool);
        assertEquals(0, resultBool.length);

        int[] intArray = { 1, 2, 3, 4 };
        Integer[] resultInt = Array.box(intArray, 3, 3);
        assertArrayEquals(CommonUtil.EMPTY_INT_OBJ_ARRAY, resultInt);
        assertEquals(0, resultInt.length);

        char[] charArray = { 'a', 'b' };
        Character[] resultChar = Array.box(charArray, 0, 0);
        assertArrayEquals(CommonUtil.EMPTY_CHAR_OBJ_ARRAY, resultChar);
    }

    @Test
    public void testUnbox_fromIndexEqualsToIndex() {
        Boolean[] boolArray = { true, false, true };
        boolean[] resultBool = Array.unbox(boolArray, 1, 1, false);
        assertArrayEquals(CommonUtil.EMPTY_BOOLEAN_ARRAY, resultBool);
        assertEquals(0, resultBool.length);

        Integer[] intArray = { 1, 2, 3, 4 };
        int[] resultInt = Array.unbox(intArray, 2, 2, 0);
        assertArrayEquals(CommonUtil.EMPTY_INT_ARRAY, resultInt);
        assertEquals(0, resultInt.length);
    }

    @Test
    public void testOf_variousObjectTypes() {
        Object[] objects = Array.oF(new Object(), "string", 123);
        assertEquals(3, objects.length);
        assertInstanceOf(Object.class, objects[0]);
        assertEquals("string", objects[1]);
        assertEquals(123, objects[2]);

    }

    @Test
    public void testBoxByte3D_NullInput() {
        byte[][][] input = null;
        Byte[][][] result = Array.box(input);
        Assertions.assertNull(result);
    }

    @Test
    public void testBoxByte3D_EmptyArray() {
        byte[][][] input = new byte[0][][];
        Byte[][][] result = Array.box(input);
        Assertions.assertNotNull(result);
        Assertions.assertEquals(0, result.length);
    }

    @Test
    public void testBoxByte3D_ValidArray() {
        byte[][][] input = { { { 1, 2 }, { 3, 4 } }, { { 5, 6 }, { 7, 8 } } };
        Byte[][][] result = Array.box(input);

        Assertions.assertNotNull(result);
        Assertions.assertEquals(2, result.length);
        Assertions.assertEquals(2, result[0].length);
        Assertions.assertEquals(2, result[0][0].length);

        Assertions.assertEquals(Byte.valueOf((byte) 1), result[0][0][0]);
        Assertions.assertEquals(Byte.valueOf((byte) 2), result[0][0][1]);
        Assertions.assertEquals(Byte.valueOf((byte) 3), result[0][1][0]);
        Assertions.assertEquals(Byte.valueOf((byte) 4), result[0][1][1]);
        Assertions.assertEquals(Byte.valueOf((byte) 5), result[1][0][0]);
        Assertions.assertEquals(Byte.valueOf((byte) 6), result[1][0][1]);
        Assertions.assertEquals(Byte.valueOf((byte) 7), result[1][1][0]);
        Assertions.assertEquals(Byte.valueOf((byte) 8), result[1][1][1]);
    }

    @Test
    public void testBoxShort3D_NullInput() {
        short[][][] input = null;
        Short[][][] result = Array.box(input);
        Assertions.assertNull(result);
    }

    @Test
    public void testBoxShort3D_EmptyArray() {
        short[][][] input = new short[0][][];
        Short[][][] result = Array.box(input);
        Assertions.assertNotNull(result);
        Assertions.assertEquals(0, result.length);
    }

    @Test
    public void testBoxShort3D_ValidArray() {
        short[][][] input = { { { 10, 20 }, { 30, 40 } }, { { 50, 60 }, { 70, 80 } } };
        Short[][][] result = Array.box(input);

        Assertions.assertNotNull(result);
        Assertions.assertEquals(2, result.length);
        Assertions.assertEquals(Short.valueOf((short) 10), result[0][0][0]);
        Assertions.assertEquals(Short.valueOf((short) 80), result[1][1][1]);
    }

    @Test
    public void testBoxInt3D_NullInput() {
        int[][][] input = null;
        Integer[][][] result = Array.box(input);
        Assertions.assertNull(result);
    }

    @Test
    public void testBoxInt3D_EmptyArray() {
        int[][][] input = new int[0][][];
        Integer[][][] result = Array.box(input);
        Assertions.assertNotNull(result);
        Assertions.assertEquals(0, result.length);
    }

    @Test
    public void testBoxInt3D_ValidArray() {
        int[][][] input = { { { 100, 200 }, { 300, 400 } }, { { 500, 600 }, { 700, 800 } } };
        Integer[][][] result = Array.box(input);

        Assertions.assertNotNull(result);
        Assertions.assertEquals(2, result.length);
        Assertions.assertEquals(Integer.valueOf(100), result[0][0][0]);
        Assertions.assertEquals(Integer.valueOf(800), result[1][1][1]);
    }

    @Test
    public void testBoxLong3D_NullInput() {
        long[][][] input = null;
        Long[][][] result = Array.box(input);
        Assertions.assertNull(result);
    }

    @Test
    public void testBoxLong3D_EmptyArray() {
        long[][][] input = new long[0][][];
        Long[][][] result = Array.box(input);
        Assertions.assertNotNull(result);
        Assertions.assertEquals(0, result.length);
    }

    @Test
    public void testBoxLong3D_ValidArray() {
        long[][][] input = { { { 1000L, 2000L }, { 3000L, 4000L } }, { { 5000L, 6000L }, { 7000L, 8000L } } };
        Long[][][] result = Array.box(input);

        Assertions.assertNotNull(result);
        Assertions.assertEquals(2, result.length);
        Assertions.assertEquals(Long.valueOf(1000L), result[0][0][0]);
        Assertions.assertEquals(Long.valueOf(8000L), result[1][1][1]);
    }

    @Test
    public void testBoxFloat3D_NullInput() {
        float[][][] input = null;
        Float[][][] result = Array.box(input);
        Assertions.assertNull(result);
    }

    @Test
    public void testBoxFloat3D_EmptyArray() {
        float[][][] input = new float[0][][];
        Float[][][] result = Array.box(input);
        Assertions.assertNotNull(result);
        Assertions.assertEquals(0, result.length);
    }

    @Test
    public void testBoxFloat3D_ValidArray() {
        float[][][] input = { { { 1.1f, 2.2f }, { 3.3f, 4.4f } }, { { 5.5f, 6.6f }, { 7.7f, 8.8f } } };
        Float[][][] result = Array.box(input);

        Assertions.assertNotNull(result);
        Assertions.assertEquals(2, result.length);
        Assertions.assertEquals(Float.valueOf(1.1f), result[0][0][0]);
        Assertions.assertEquals(Float.valueOf(8.8f), result[1][1][1]);
    }

    @Test
    public void testBoxDouble3D_NullInput() {
        double[][][] input = null;
        Double[][][] result = Array.box(input);
        Assertions.assertNull(result);
    }

    @Test
    public void testBoxDouble3D_EmptyArray() {
        double[][][] input = new double[0][][];
        Double[][][] result = Array.box(input);
        Assertions.assertNotNull(result);
        Assertions.assertEquals(0, result.length);
    }

    @Test
    public void testBoxDouble3D_ValidArray() {
        double[][][] input = { { { 1.11, 2.22 }, { 3.33, 4.44 } }, { { 5.55, 6.66 }, { 7.77, 8.88 } } };
        Double[][][] result = Array.box(input);

        Assertions.assertNotNull(result);
        Assertions.assertEquals(2, result.length);
        Assertions.assertEquals(Double.valueOf(1.11), result[0][0][0]);
        Assertions.assertEquals(Double.valueOf(8.88), result[1][1][1]);
    }

    @Test
    public void testUnboxBooleanVarargs_NullInput() {
        Boolean[] input = null;
        boolean[] result = Array.unbox(input);
        Assertions.assertNull(result);
    }

    @Test
    public void testUnboxBooleanVarargs_EmptyArray() {
        Boolean[] input = new Boolean[0];
        boolean[] result = Array.unbox(input);
        Assertions.assertNotNull(result);
        Assertions.assertEquals(0, result.length);
    }

    @Test
    public void testUnboxBooleanVarargs_WithNullValues() {
        Boolean[] input = { true, null, false, null };
        boolean[] result = Array.unbox(input);
        Assertions.assertNotNull(result);
        Assertions.assertEquals(4, result.length);
        Assertions.assertTrue(result[0]);
        Assertions.assertFalse(result[1]);
        Assertions.assertFalse(result[2]);
        Assertions.assertFalse(result[3]);
    }

    @Test
    public void testUnboxBooleanVarargs_ValidArray() {
        Boolean[] input = { true, false, true };
        boolean[] result = Array.unbox(input);
        Assertions.assertNotNull(result);
        Assertions.assertEquals(3, result.length);
        Assertions.assertTrue(result[0]);
        Assertions.assertFalse(result[1]);
        Assertions.assertTrue(result[2]);
    }

    @Test
    public void testUnboxBooleanWithDefault_NullInput() {
        Boolean[] input = null;
        boolean[] result = Array.unbox(input, true);
        Assertions.assertNull(result);
    }

    @Test
    public void testUnboxBooleanWithDefault_WithNullValues() {
        Boolean[] input = { true, null, false, null };
        boolean[] result = Array.unbox(input, true);
        Assertions.assertNotNull(result);
        Assertions.assertEquals(4, result.length);
        Assertions.assertTrue(result[0]);
        Assertions.assertTrue(result[1]);
        Assertions.assertFalse(result[2]);
        Assertions.assertTrue(result[3]);
    }

    @Test
    public void testUnboxBooleanRange_NullInput() {
        Boolean[] input = null;
        boolean[] result = Array.unbox(input, 0, 0, false);
        Assertions.assertNull(result);
    }

    @Test
    public void testUnboxBooleanRange_EmptyRange() {
        Boolean[] input = { true, false, true };
        boolean[] result = Array.unbox(input, 1, 1, false);
        Assertions.assertNotNull(result);
        Assertions.assertEquals(0, result.length);
    }

    @Test
    public void testUnboxBooleanRange_ValidRange() {
        Boolean[] input = { true, null, false, true, null };
        boolean[] result = Array.unbox(input, 1, 4, true);
        Assertions.assertNotNull(result);
        Assertions.assertEquals(3, result.length);
        Assertions.assertTrue(result[0]);
        Assertions.assertFalse(result[1]);
        Assertions.assertTrue(result[2]);
    }

    @Test
    public void testUnboxBooleanRange_InvalidRange() {
        Boolean[] input = { true, false };
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> {
            Array.unbox(input, 0, 3, false);
        });
    }

    @Test
    public void testUnboxCharacterVarargs_NullInput() {
        Character[] input = null;
        char[] result = Array.unbox(input);
        Assertions.assertNull(result);
    }

    @Test
    public void testUnboxCharacterVarargs_WithNullValues() {
        Character[] input = { 'a', null, 'b' };
        char[] result = Array.unbox(input);
        Assertions.assertNotNull(result);
        Assertions.assertEquals(3, result.length);
        Assertions.assertEquals('a', result[0]);
        Assertions.assertEquals((char) 0, result[1]);
        Assertions.assertEquals('b', result[2]);
    }

    @Test
    public void testUnboxCharacterWithDefault_WithNullValues() {
        Character[] input = { 'x', null, 'y' };
        char[] result = Array.unbox(input, '?');
        Assertions.assertNotNull(result);
        Assertions.assertEquals(3, result.length);
        Assertions.assertEquals('x', result[0]);
        Assertions.assertEquals('?', result[1]);
        Assertions.assertEquals('y', result[2]);
    }

    @Test
    public void testUnboxCharacterRange_ValidRange() {
        Character[] input = { 'a', 'b', null, 'd', 'e' };
        char[] result = Array.unbox(input, 1, 4, '*');
        Assertions.assertNotNull(result);
        Assertions.assertEquals(3, result.length);
        Assertions.assertEquals('b', result[0]);
        Assertions.assertEquals('*', result[1]);
        Assertions.assertEquals('d', result[2]);
    }

    @Test
    public void testUnboxByteVarargs_NullInput() {
        Byte[] input = null;
        byte[] result = Array.unbox(input);
        Assertions.assertNull(result);
    }

    @Test
    public void testUnboxByteVarargs_WithNullValues() {
        Byte[] input = { 1, null, 3 };
        byte[] result = Array.unbox(input);
        Assertions.assertNotNull(result);
        Assertions.assertEquals(3, result.length);
        Assertions.assertEquals((byte) 1, result[0]);
        Assertions.assertEquals((byte) 0, result[1]);
        Assertions.assertEquals((byte) 3, result[2]);
    }

    @Test
    public void testUnboxByteWithDefault_WithNullValues() {
        Byte[] input = { 10, null, 30 };
        byte[] result = Array.unbox(input, (byte) 99);
        Assertions.assertNotNull(result);
        Assertions.assertEquals(3, result.length);
        Assertions.assertEquals((byte) 10, result[0]);
        Assertions.assertEquals((byte) 99, result[1]);
        Assertions.assertEquals((byte) 30, result[2]);
    }

    @Test
    public void testUnboxByteRange_ValidRange() {
        Byte[] input = { 1, 2, null, 4, 5 };
        byte[] result = Array.unbox(input, 1, 4, (byte) 77);
        Assertions.assertNotNull(result);
        Assertions.assertEquals(3, result.length);
        Assertions.assertEquals((byte) 2, result[0]);
        Assertions.assertEquals((byte) 77, result[1]);
        Assertions.assertEquals((byte) 4, result[2]);
    }

    @Test
    public void testUnboxShortVarargs_NullInput() {
        Short[] input = null;
        short[] result = Array.unbox(input);
        Assertions.assertNull(result);
    }

    @Test
    public void testUnboxShortVarargs_WithNullValues() {
        Short[] input = { 100, null, 300 };
        short[] result = Array.unbox(input);
        Assertions.assertNotNull(result);
        Assertions.assertEquals(3, result.length);
        Assertions.assertEquals((short) 100, result[0]);
        Assertions.assertEquals((short) 0, result[1]);
        Assertions.assertEquals((short) 300, result[2]);
    }

    @Test
    public void testUnboxShortWithDefault_WithNullValues() {
        Short[] input = { 1000, null, 3000 };
        short[] result = Array.unbox(input, (short) 9999);
        Assertions.assertNotNull(result);
        Assertions.assertEquals(3, result.length);
        Assertions.assertEquals((short) 1000, result[0]);
        Assertions.assertEquals((short) 9999, result[1]);
        Assertions.assertEquals((short) 3000, result[2]);
    }

    @Test
    public void testUnboxShortRange_ValidRange() {
        Short[] input = { 10, 20, null, 40, 50 };
        short[] result = Array.unbox(input, 1, 4, (short) 777);
        Assertions.assertNotNull(result);
        Assertions.assertEquals(3, result.length);
        Assertions.assertEquals((short) 20, result[0]);
        Assertions.assertEquals((short) 777, result[1]);
        Assertions.assertEquals((short) 40, result[2]);
    }

    @Test
    public void testUnboxIntegerVarargs_NullInput() {
        Integer[] input = null;
        int[] result = Array.unbox(input);
        Assertions.assertNull(result);
    }

    @Test
    public void testUnboxIntegerVarargs_WithNullValues() {
        Integer[] input = { 1000, null, 3000 };
        int[] result = Array.unbox(input);
        Assertions.assertNotNull(result);
        Assertions.assertEquals(3, result.length);
        Assertions.assertEquals(1000, result[0]);
        Assertions.assertEquals(0, result[1]);
        Assertions.assertEquals(3000, result[2]);
    }

    @Test
    public void testUnboxIntegerWithDefault_WithNullValues() {
        Integer[] input = { 10000, null, 30000 };
        int[] result = Array.unbox(input, 99999);
        Assertions.assertNotNull(result);
        Assertions.assertEquals(3, result.length);
        Assertions.assertEquals(10000, result[0]);
        Assertions.assertEquals(99999, result[1]);
        Assertions.assertEquals(30000, result[2]);
    }

    @Test
    public void testUnboxIntegerRange_ValidRange() {
        Integer[] input = { 100, 200, null, 400, 500 };
        int[] result = Array.unbox(input, 1, 4, 7777);
        Assertions.assertNotNull(result);
        Assertions.assertEquals(3, result.length);
        Assertions.assertEquals(200, result[0]);
        Assertions.assertEquals(7777, result[1]);
        Assertions.assertEquals(400, result[2]);
    }

    @Test
    public void testUnboxLongVarargs_NullInput() {
        Long[] input = null;
        long[] result = Array.unbox(input);
        Assertions.assertNull(result);
    }

    @Test
    public void testUnboxLongVarargs_WithNullValues() {
        Long[] input = { 100000L, null, 300000L };
        long[] result = Array.unbox(input);
        Assertions.assertNotNull(result);
        Assertions.assertEquals(3, result.length);
        Assertions.assertEquals(100000L, result[0]);
        Assertions.assertEquals(0L, result[1]);
        Assertions.assertEquals(300000L, result[2]);
    }

    @Test
    public void testUnboxLongWithDefault_WithNullValues() {
        Long[] input = { 1000000L, null, 3000000L };
        long[] result = Array.unbox(input, 9999999L);
        Assertions.assertNotNull(result);
        Assertions.assertEquals(3, result.length);
        Assertions.assertEquals(1000000L, result[0]);
        Assertions.assertEquals(9999999L, result[1]);
        Assertions.assertEquals(3000000L, result[2]);
    }

    @Test
    public void testUnboxLongRange_ValidRange() {
        Long[] input = { 1000L, 2000L, null, 4000L, 5000L };
        long[] result = Array.unbox(input, 1, 4, 77777L);
        Assertions.assertNotNull(result);
        Assertions.assertEquals(3, result.length);
        Assertions.assertEquals(2000L, result[0]);
        Assertions.assertEquals(77777L, result[1]);
        Assertions.assertEquals(4000L, result[2]);
    }

    @Test
    public void testUnboxFloatVarargs_NullInput() {
        Float[] input = null;
        float[] result = Array.unbox(input);
        Assertions.assertNull(result);
    }

    @Test
    public void testUnboxFloatVarargs_WithNullValues() {
        Float[] input = { 1.1f, null, 3.3f };
        float[] result = Array.unbox(input);
        Assertions.assertNotNull(result);
        Assertions.assertEquals(3, result.length);
        Assertions.assertEquals(1.1f, result[0]);
        Assertions.assertEquals(0f, result[1]);
        Assertions.assertEquals(3.3f, result[2]);
    }

    @Test
    public void testUnboxFloatWithDefault_WithNullValues() {
        Float[] input = { 11.1f, null, 33.3f };
        float[] result = Array.unbox(input, 99.9f);
        Assertions.assertNotNull(result);
        Assertions.assertEquals(3, result.length);
        Assertions.assertEquals(11.1f, result[0]);
        Assertions.assertEquals(99.9f, result[1]);
        Assertions.assertEquals(33.3f, result[2]);
    }

    @Test
    public void testUnboxFloatRange_ValidRange() {
        Float[] input = { 1.0f, 2.0f, null, 4.0f, 5.0f };
        float[] result = Array.unbox(input, 1, 4, 77.7f);
        Assertions.assertNotNull(result);
        Assertions.assertEquals(3, result.length);
        Assertions.assertEquals(2.0f, result[0]);
        Assertions.assertEquals(77.7f, result[1]);
        Assertions.assertEquals(4.0f, result[2]);
    }

    @Test
    public void testUnboxDoubleVarargs_NullInput() {
        Double[] input = null;
        double[] result = Array.unbox(input);
        Assertions.assertNull(result);
    }

    @Test
    public void testUnboxDoubleVarargs_WithNullValues() {
        Double[] input = { 1.11, null, 3.33 };
        double[] result = Array.unbox(input);
        Assertions.assertNotNull(result);
        Assertions.assertEquals(3, result.length);
        Assertions.assertEquals(1.11, result[0]);
        Assertions.assertEquals(0d, result[1]);
        Assertions.assertEquals(3.33, result[2]);
    }

    @Test
    public void testUnboxDoubleWithDefault_WithNullValues() {
        Double[] input = { 11.11, null, 33.33 };
        double[] result = Array.unbox(input, 99.99);
        Assertions.assertNotNull(result);
        Assertions.assertEquals(3, result.length);
        Assertions.assertEquals(11.11, result[0]);
        Assertions.assertEquals(99.99, result[1]);
        Assertions.assertEquals(33.33, result[2]);
    }

    @Test
    public void testUnboxDoubleRange_ValidRange() {
        Double[] input = { 1.0, 2.0, null, 4.0, 5.0 };
        double[] result = Array.unbox(input, 1, 4, 77.77);
        Assertions.assertNotNull(result);
        Assertions.assertEquals(3, result.length);
        Assertions.assertEquals(2.0, result[0]);
        Assertions.assertEquals(77.77, result[1]);
        Assertions.assertEquals(4.0, result[2]);
    }

    @Test
    public void testUnboxBoolean2D_NullInput() {
        Boolean[][] input = null;
        boolean[][] result = Array.unbox(input);
        Assertions.assertNull(result);
    }

    @Test
    public void testUnboxBoolean2D_EmptyArray() {
        Boolean[][] input = new Boolean[0][];
        boolean[][] result = Array.unbox(input);
        Assertions.assertNotNull(result);
        Assertions.assertEquals(0, result.length);
    }

    @Test
    public void testUnboxBoolean2D_ValidArray() {
        Boolean[][] input = { { true, null, false }, { null, true, true } };
        boolean[][] result = Array.unbox(input);
        Assertions.assertNotNull(result);
        Assertions.assertEquals(2, result.length);
        Assertions.assertEquals(3, result[0].length);
        Assertions.assertTrue(result[0][0]);
        Assertions.assertFalse(result[0][1]);
        Assertions.assertFalse(result[0][2]);
        Assertions.assertFalse(result[1][0]);
        Assertions.assertTrue(result[1][1]);
        Assertions.assertTrue(result[1][2]);
    }

    @Test
    public void testUnboxBoolean2DWithDefault_ValidArray() {
        Boolean[][] input = { { true, null }, { null, false } };
        boolean[][] result = Array.unbox(input, true);
        Assertions.assertNotNull(result);
        Assertions.assertEquals(2, result.length);
        Assertions.assertTrue(result[0][0]);
        Assertions.assertTrue(result[0][1]);
        Assertions.assertTrue(result[1][0]);
        Assertions.assertFalse(result[1][1]);
    }

    @Test
    public void testUnboxCharacter2D_NullInput() {
        Character[][] input = null;
        char[][] result = Array.unbox(input);
        Assertions.assertNull(result);
    }

    @Test
    public void testUnboxCharacter2D_ValidArray() {
        Character[][] input = { { 'a', null, 'c' }, { null, 'e', 'f' } };
        char[][] result = Array.unbox(input);
        Assertions.assertNotNull(result);
        Assertions.assertEquals(2, result.length);
        Assertions.assertEquals('a', result[0][0]);
        Assertions.assertEquals((char) 0, result[0][1]);
        Assertions.assertEquals('c', result[0][2]);
    }

    @Test
    public void testUnboxCharacter2DWithDefault_ValidArray() {
        Character[][] input = { { 'x', null }, { null, 'z' } };
        char[][] result = Array.unbox(input, '?');
        Assertions.assertNotNull(result);
        Assertions.assertEquals(2, result.length);
        Assertions.assertEquals('x', result[0][0]);
        Assertions.assertEquals('?', result[0][1]);
        Assertions.assertEquals('?', result[1][0]);
        Assertions.assertEquals('z', result[1][1]);
    }

    @Test
    public void testUnboxByte2D_NullInput() {
        Byte[][] input = null;
        byte[][] result = Array.unbox(input);
        Assertions.assertNull(result);
    }

    @Test
    public void testUnboxByte2D_ValidArray() {
        Byte[][] input = { { 1, null, 3 }, { null, 5, 6 } };
        byte[][] result = Array.unbox(input);
        Assertions.assertNotNull(result);
        Assertions.assertEquals(2, result.length);
        Assertions.assertEquals((byte) 1, result[0][0]);
        Assertions.assertEquals((byte) 0, result[0][1]);
        Assertions.assertEquals((byte) 3, result[0][2]);
    }

    @Test
    public void testUnboxByte2DWithDefault_ValidArray() {
        Byte[][] input = { { 10, null }, { null, 30 } };
        byte[][] result = Array.unbox(input, (byte) 99);
        Assertions.assertNotNull(result);
        Assertions.assertEquals(2, result.length);
        Assertions.assertEquals((byte) 10, result[0][0]);
        Assertions.assertEquals((byte) 99, result[0][1]);
    }

    @Test
    public void testUnboxShort2D_NullInput() {
        Short[][] input = null;
        short[][] result = Array.unbox(input);
        Assertions.assertNull(result);
    }

    @Test
    public void testUnboxShort2D_ValidArray() {
        Short[][] input = { { 100, null, 300 }, { null, 500, 600 } };
        short[][] result = Array.unbox(input);
        Assertions.assertNotNull(result);
        Assertions.assertEquals(2, result.length);
        Assertions.assertEquals((short) 100, result[0][0]);
        Assertions.assertEquals((short) 0, result[0][1]);
    }

    @Test
    public void testUnboxShort2DWithDefault_ValidArray() {
        Short[][] input = { { 1000, null }, { null, 3000 } };
        short[][] result = Array.unbox(input, (short) 9999);
        Assertions.assertNotNull(result);
        Assertions.assertEquals(2, result.length);
        Assertions.assertEquals((short) 1000, result[0][0]);
        Assertions.assertEquals((short) 9999, result[0][1]);
    }

    @Test
    public void testUnboxInteger2D_NullInput() {
        Integer[][] input = null;
        int[][] result = Array.unbox(input);
        Assertions.assertNull(result);
    }

    @Test
    public void testUnboxInteger2D_ValidArray() {
        Integer[][] input = { { 1000, null, 3000 }, { null, 5000, 6000 } };
        int[][] result = Array.unbox(input);
        Assertions.assertNotNull(result);
        Assertions.assertEquals(2, result.length);
        Assertions.assertEquals(1000, result[0][0]);
        Assertions.assertEquals(0, result[0][1]);
    }

    @Test
    public void testUnboxInteger2DWithDefault_ValidArray() {
        Integer[][] input = { { 10000, null }, { null, 30000 } };
        int[][] result = Array.unbox(input, 99999);
        Assertions.assertNotNull(result);
        Assertions.assertEquals(2, result.length);
        Assertions.assertEquals(10000, result[0][0]);
        Assertions.assertEquals(99999, result[0][1]);
    }

    @Test
    public void testUnboxLong2D_NullInput() {
        Long[][] input = null;
        long[][] result = Array.unbox(input);
        Assertions.assertNull(result);
    }

    @Test
    public void testUnboxLong2D_ValidArray() {
        Long[][] input = { { 100000L, null, 300000L }, { null, 500000L, 600000L } };
        long[][] result = Array.unbox(input);
        Assertions.assertNotNull(result);
        Assertions.assertEquals(2, result.length);
        Assertions.assertEquals(100000L, result[0][0]);
        Assertions.assertEquals(0L, result[0][1]);
    }

    @Test
    public void testUnboxLong2DWithDefault_ValidArray() {
        Long[][] input = { { 1000000L, null }, { null, 3000000L } };
        long[][] result = Array.unbox(input, 9999999L);
        Assertions.assertNotNull(result);
        Assertions.assertEquals(2, result.length);
        Assertions.assertEquals(1000000L, result[0][0]);
        Assertions.assertEquals(9999999L, result[0][1]);
    }

    @Test
    public void testUnboxFloat2D_NullInput() {
        Float[][] input = null;
        float[][] result = Array.unbox(input);
        Assertions.assertNull(result);
    }

    @Test
    public void testUnboxFloat2D_ValidArray() {
        Float[][] input = { { 1.1f, null, 3.3f }, { null, 5.5f, 6.6f } };
        float[][] result = Array.unbox(input);
        Assertions.assertNotNull(result);
        Assertions.assertEquals(2, result.length);
        Assertions.assertEquals(1.1f, result[0][0]);
        Assertions.assertEquals(0f, result[0][1]);
    }

    @Test
    public void testUnboxFloat2DWithDefault_ValidArray() {
        Float[][] input = { { 11.1f, null }, { null, 33.3f } };
        float[][] result = Array.unbox(input, 99.9f);
        Assertions.assertNotNull(result);
        Assertions.assertEquals(2, result.length);
        Assertions.assertEquals(11.1f, result[0][0]);
        Assertions.assertEquals(99.9f, result[0][1]);
    }

    @Test
    public void testUnboxDouble2D_NullInput() {
        Double[][] input = null;
        double[][] result = Array.unbox(input);
        Assertions.assertNull(result);
    }

    @Test
    public void testUnboxDouble2D_ValidArray() {
        Double[][] input = { { 1.11, null, 3.33 }, { null, 5.55, 6.66 } };
        double[][] result = Array.unbox(input);
        Assertions.assertNotNull(result);
        Assertions.assertEquals(2, result.length);
        Assertions.assertEquals(1.11, result[0][0]);
        Assertions.assertEquals(0d, result[0][1]);
    }

    @Test
    public void testUnboxDouble2DWithDefault_ValidArray() {
        Double[][] input = { { 11.11, null }, { null, 33.33 } };
        double[][] result = Array.unbox(input, 99.99);
        Assertions.assertNotNull(result);
        Assertions.assertEquals(2, result.length);
        Assertions.assertEquals(11.11, result[0][0]);
        Assertions.assertEquals(99.99, result[0][1]);
    }

    @Test
    public void testUnboxBoolean3D_NullInput() {
        Boolean[][][] input = null;
        boolean[][][] result = Array.unbox(input);
        Assertions.assertNull(result);
    }

    @Test
    public void testUnboxBoolean3D_EmptyArray() {
        Boolean[][][] input = new Boolean[0][][];
        boolean[][][] result = Array.unbox(input);
        Assertions.assertNotNull(result);
        Assertions.assertEquals(0, result.length);
    }

    @Test
    public void testUnboxBoolean3D_ValidArray() {
        Boolean[][][] input = { { { true, null }, { false, true } }, { { null, false }, { true, null } } };
        boolean[][][] result = Array.unbox(input);
        Assertions.assertNotNull(result);
        Assertions.assertEquals(2, result.length);
        Assertions.assertTrue(result[0][0][0]);
        Assertions.assertFalse(result[0][0][1]);
        Assertions.assertFalse(result[0][1][0]);
        Assertions.assertTrue(result[0][1][1]);
    }

    @Test
    public void testUnboxBoolean3DWithDefault_ValidArray() {
        Boolean[][][] input = { { { true, null }, { null, false } }, { { null, true }, { false, null } } };
        boolean[][][] result = Array.unbox(input, true);
        Assertions.assertNotNull(result);
        Assertions.assertEquals(2, result.length);
        Assertions.assertTrue(result[0][0][0]);
        Assertions.assertTrue(result[0][0][1]);
        Assertions.assertTrue(result[0][1][0]);
        Assertions.assertFalse(result[0][1][1]);
    }

    @Test
    public void testUnboxCharacter3D_NullInput() {
        Character[][][] input = null;
        char[][][] result = Array.unbox(input);
        Assertions.assertNull(result);
    }

    @Test
    public void testUnboxCharacter3D_ValidArray() {
        Character[][][] input = { { { 'a', null }, { 'c', 'd' } }, { { null, 'f' }, { 'g', null } } };
        char[][][] result = Array.unbox(input);
        Assertions.assertNotNull(result);
        Assertions.assertEquals(2, result.length);
        Assertions.assertEquals('a', result[0][0][0]);
        Assertions.assertEquals((char) 0, result[0][0][1]);
    }

    @Test
    public void testUnboxCharacter3DWithDefault_ValidArray() {
        Character[][][] input = { { { 'x', null }, { null, 'z' } }, { { null, 'b' }, { 'c', null } } };
        char[][][] result = Array.unbox(input, '?');
        Assertions.assertNotNull(result);
        Assertions.assertEquals(2, result.length);
        Assertions.assertEquals('x', result[0][0][0]);
        Assertions.assertEquals('?', result[0][0][1]);
    }

    @Test
    public void testUnboxByte3D_NullInput() {
        Byte[][][] input = null;
        byte[][][] result = Array.unbox(input);
        Assertions.assertNull(result);
    }

    @Test
    public void testUnboxByte3D_ValidArray() {
        Byte[][][] input = { { { 1, null }, { 3, 4 } }, { { null, 6 }, { 7, null } } };
        byte[][][] result = Array.unbox(input);
        Assertions.assertNotNull(result);
        Assertions.assertEquals(2, result.length);
        Assertions.assertEquals((byte) 1, result[0][0][0]);
        Assertions.assertEquals((byte) 0, result[0][0][1]);
    }

    @Test
    public void testUnboxByte3DWithDefault_ValidArray() {
        Byte[][][] input = { { { 10, null }, { null, 40 } }, { { null, 60 }, { 70, null } } };
        byte[][][] result = Array.unbox(input, (byte) 99);
        Assertions.assertNotNull(result);
        Assertions.assertEquals(2, result.length);
        Assertions.assertEquals((byte) 10, result[0][0][0]);
        Assertions.assertEquals((byte) 99, result[0][0][1]);
    }

    @Test
    public void testUnboxShort3D_NullInput() {
        Short[][][] input = null;
        short[][][] result = Array.unbox(input);
        Assertions.assertNull(result);
    }

    @Test
    public void testUnboxShort3D_ValidArray() {
        Short[][][] input = { { { 100, null }, { 300, 400 } }, { { null, 600 }, { 700, null } } };
        short[][][] result = Array.unbox(input);
        Assertions.assertNotNull(result);
        Assertions.assertEquals(2, result.length);
        Assertions.assertEquals((short) 100, result[0][0][0]);
        Assertions.assertEquals((short) 0, result[0][0][1]);
    }

    @Test
    public void testUnboxShort3DWithDefault_ValidArray() {
        Short[][][] input = { { { 1000, null }, { null, 4000 } }, { { null, 6000 }, { 7000, null } } };
        short[][][] result = Array.unbox(input, (short) 9999);
        Assertions.assertNotNull(result);
        Assertions.assertEquals(2, result.length);
        Assertions.assertEquals((short) 1000, result[0][0][0]);
        Assertions.assertEquals((short) 9999, result[0][0][1]);
    }

    @Test
    public void testUnboxInteger3D_NullInput() {
        Integer[][][] input = null;
        int[][][] result = Array.unbox(input);
        Assertions.assertNull(result);
    }

    @Test
    public void testUnboxInteger3D_ValidArray() {
        Integer[][][] input = { { { 1000, null }, { 3000, 4000 } }, { { null, 6000 }, { 7000, null } } };
        int[][][] result = Array.unbox(input);
        Assertions.assertNotNull(result);
        Assertions.assertEquals(2, result.length);
        Assertions.assertEquals(1000, result[0][0][0]);
        Assertions.assertEquals(0, result[0][0][1]);
    }

    @Test
    public void testUnboxInteger3DWithDefault_ValidArray() {
        Integer[][][] input = { { { 10000, null }, { null, 40000 } }, { { null, 60000 }, { 70000, null } } };
        int[][][] result = Array.unbox(input, 99999);
        Assertions.assertNotNull(result);
        Assertions.assertEquals(2, result.length);
        Assertions.assertEquals(10000, result[0][0][0]);
        Assertions.assertEquals(99999, result[0][0][1]);
    }

    @Test
    public void testUnboxLong3D_NullInput() {
        Long[][][] input = null;
        long[][][] result = Array.unbox(input);
        Assertions.assertNull(result);
    }

    @Test
    public void testUnboxLong3D_ValidArray() {
        Long[][][] input = { { { 100000L, null }, { 300000L, 400000L } }, { { null, 600000L }, { 700000L, null } } };
        long[][][] result = Array.unbox(input);
        Assertions.assertNotNull(result);
        Assertions.assertEquals(2, result.length);
        Assertions.assertEquals(100000L, result[0][0][0]);
        Assertions.assertEquals(0L, result[0][0][1]);
    }

    @Test
    public void testUnboxLong3DWithDefault_ValidArray() {
        Long[][][] input = { { { 1000000L, null }, { null, 4000000L } }, { { null, 6000000L }, { 7000000L, null } } };
        long[][][] result = Array.unbox(input, 9999999L);
        Assertions.assertNotNull(result);
        Assertions.assertEquals(2, result.length);
        Assertions.assertEquals(1000000L, result[0][0][0]);
        Assertions.assertEquals(9999999L, result[0][0][1]);
    }

    @Test
    public void testUnboxFloat3D_NullInput() {
        Float[][][] input = null;
        float[][][] result = Array.unbox(input);
        Assertions.assertNull(result);
    }

    @Test
    public void testUnboxFloat3D_ValidArray() {
        Float[][][] input = { { { 1.1f, null }, { 3.3f, 4.4f } }, { { null, 6.6f }, { 7.7f, null } } };
        float[][][] result = Array.unbox(input);
        Assertions.assertNotNull(result);
        Assertions.assertEquals(2, result.length);
        Assertions.assertEquals(1.1f, result[0][0][0]);
        Assertions.assertEquals(0f, result[0][0][1]);
    }

    @Test
    public void testUnboxFloat3DWithDefault_ValidArray() {
        Float[][][] input = { { { 11.1f, null }, { null, 44.4f } }, { { null, 66.6f }, { 77.7f, null } } };
        float[][][] result = Array.unbox(input, 99.9f);
        Assertions.assertNotNull(result);
        Assertions.assertEquals(2, result.length);
        Assertions.assertEquals(11.1f, result[0][0][0]);
        Assertions.assertEquals(99.9f, result[0][0][1]);
    }

    @Test
    public void testUnboxDouble3D_NullInput() {
        Double[][][] input = null;
        double[][][] result = Array.unbox(input);
        Assertions.assertNull(result);
    }

    @Test
    public void testUnboxDouble3D_ValidArray() {
        Double[][][] input = { { { 1.11, null }, { 3.33, 4.44 } }, { { null, 6.66 }, { 7.77, null } } };
        double[][][] result = Array.unbox(input);
        Assertions.assertNotNull(result);
        Assertions.assertEquals(2, result.length);
        Assertions.assertEquals(1.11, result[0][0][0]);
        Assertions.assertEquals(0d, result[0][0][1]);
    }

    @Test
    public void testUnboxDouble3DWithDefault_ValidArray() {
        Double[][][] input = { { { 11.11, null }, { null, 44.44 } }, { { null, 66.66 }, { 77.77, null } } };
        double[][][] result = Array.unbox(input, 99.99);
        Assertions.assertNotNull(result);
        Assertions.assertEquals(2, result.length);
        Assertions.assertEquals(11.11, result[0][0][0]);
        Assertions.assertEquals(99.99, result[0][0][1]);
    }

    @Test
    public void testTransposeBoolean_NullInput() {
        boolean[][] input = null;
        boolean[][] result = Array.transpose(input);
        Assertions.assertNull(result);
    }

    @Test
    public void testTransposeBoolean_EmptyArray() {
        boolean[][] input = new boolean[0][];
        boolean[][] result = Array.transpose(input);
        Assertions.assertNotNull(result);
        Assertions.assertEquals(0, result.length);
    }

    @Test
    public void testTransposeBoolean_SquareMatrix() {
        boolean[][] input = { { true, false, true }, { false, true, false }, { true, true, false } };
        boolean[][] result = Array.transpose(input);

        Assertions.assertNotNull(result);
        Assertions.assertEquals(3, result.length);
        Assertions.assertEquals(3, result[0].length);

        Assertions.assertTrue(result[0][0]);
        Assertions.assertFalse(result[0][1]);
        Assertions.assertTrue(result[0][2]);
        Assertions.assertFalse(result[1][0]);
        Assertions.assertTrue(result[1][1]);
        Assertions.assertTrue(result[1][2]);
        Assertions.assertTrue(result[2][0]);
        Assertions.assertFalse(result[2][1]);
        Assertions.assertFalse(result[2][2]);
    }

    @Test
    public void testTransposeBoolean_RectangularMatrix() {
        boolean[][] input = { { true, false }, { false, true }, { true, true } };
        boolean[][] result = Array.transpose(input);

        Assertions.assertNotNull(result);
        Assertions.assertEquals(2, result.length);
        Assertions.assertEquals(3, result[0].length);

        Assertions.assertTrue(result[0][0]);
        Assertions.assertFalse(result[0][1]);
        Assertions.assertTrue(result[0][2]);
        Assertions.assertFalse(result[1][0]);
        Assertions.assertTrue(result[1][1]);
        Assertions.assertTrue(result[1][2]);
    }

    @Test
    public void testTransposeChar_NullInput() {
        char[][] input = null;
        char[][] result = Array.transpose(input);
        Assertions.assertNull(result);
    }

    @Test
    public void testTransposeChar_EmptyArray() {
        char[][] input = new char[0][];
        char[][] result = Array.transpose(input);
        Assertions.assertNotNull(result);
        Assertions.assertEquals(0, result.length);
    }

    @Test
    public void testTransposeChar_SquareMatrix() {
        char[][] input = { { 'a', 'b', 'c' }, { 'd', 'e', 'f' }, { 'g', 'h', 'i' } };
        char[][] result = Array.transpose(input);

        Assertions.assertNotNull(result);
        Assertions.assertEquals(3, result.length);
        Assertions.assertEquals(3, result[0].length);

        Assertions.assertEquals('a', result[0][0]);
        Assertions.assertEquals('d', result[0][1]);
        Assertions.assertEquals('g', result[0][2]);
        Assertions.assertEquals('b', result[1][0]);
        Assertions.assertEquals('e', result[1][1]);
        Assertions.assertEquals('h', result[1][2]);
        Assertions.assertEquals('c', result[2][0]);
        Assertions.assertEquals('f', result[2][1]);
        Assertions.assertEquals('i', result[2][2]);
    }

    @Test
    public void testTransposeByte_NullInput() {
        byte[][] input = null;
        byte[][] result = Array.transpose(input);
        Assertions.assertNull(result);
    }

    @Test
    public void testTransposeByte_RectangularMatrix() {
        byte[][] input = { { 1, 2, 3, 4 }, { 5, 6, 7, 8 } };
        byte[][] result = Array.transpose(input);

        Assertions.assertNotNull(result);
        Assertions.assertEquals(4, result.length);
        Assertions.assertEquals(2, result[0].length);

        Assertions.assertEquals((byte) 1, result[0][0]);
        Assertions.assertEquals((byte) 5, result[0][1]);
        Assertions.assertEquals((byte) 2, result[1][0]);
        Assertions.assertEquals((byte) 6, result[1][1]);
        Assertions.assertEquals((byte) 3, result[2][0]);
        Assertions.assertEquals((byte) 7, result[2][1]);
        Assertions.assertEquals((byte) 4, result[3][0]);
        Assertions.assertEquals((byte) 8, result[3][1]);
    }

    @Test
    public void testTransposeShort_NullInput() {
        short[][] input = null;
        short[][] result = Array.transpose(input);
        Assertions.assertNull(result);
    }

    @Test
    public void testTransposeShort_SingleElement() {
        short[][] input = { { 100 } };
        short[][] result = Array.transpose(input);

        Assertions.assertNotNull(result);
        Assertions.assertEquals(1, result.length);
        Assertions.assertEquals(1, result[0].length);
        Assertions.assertEquals((short) 100, result[0][0]);
    }

    @Test
    public void testTransposeInt_NullInput() {
        int[][] input = null;
        int[][] result = Array.transpose(input);
        Assertions.assertNull(result);
    }

    @Test
    public void testTransposeInt_RectangularMatrix() {
        int[][] input = { { 1, 2, 3 }, { 4, 5, 6 } };
        int[][] result = Array.transpose(input);

        Assertions.assertNotNull(result);
        Assertions.assertEquals(3, result.length);
        Assertions.assertEquals(2, result[0].length);

        Assertions.assertEquals(1, result[0][0]);
        Assertions.assertEquals(4, result[0][1]);
        Assertions.assertEquals(2, result[1][0]);
        Assertions.assertEquals(5, result[1][1]);
        Assertions.assertEquals(3, result[2][0]);
        Assertions.assertEquals(6, result[2][1]);
    }

    @Test
    public void testTransposeLong_NullInput() {
        long[][] input = null;
        long[][] result = Array.transpose(input);
        Assertions.assertNull(result);
    }

    @Test
    public void testTransposeLong_SquareMatrix() {
        long[][] input = { { 1000L, 2000L }, { 3000L, 4000L } };
        long[][] result = Array.transpose(input);

        Assertions.assertNotNull(result);
        Assertions.assertEquals(2, result.length);
        Assertions.assertEquals(2, result[0].length);

        Assertions.assertEquals(1000L, result[0][0]);
        Assertions.assertEquals(3000L, result[0][1]);
        Assertions.assertEquals(2000L, result[1][0]);
        Assertions.assertEquals(4000L, result[1][1]);
    }

    @Test
    public void testTransposeFloat_NullInput() {
        float[][] input = null;
        float[][] result = Array.transpose(input);
        Assertions.assertNull(result);
    }

    @Test
    public void testTransposeFloat_RectangularMatrix() {
        float[][] input = { { 1.1f, 2.2f, 3.3f }, { 4.4f, 5.5f, 6.6f } };
        float[][] result = Array.transpose(input);

        Assertions.assertNotNull(result);
        Assertions.assertEquals(3, result.length);
        Assertions.assertEquals(2, result[0].length);

        Assertions.assertEquals(1.1f, result[0][0]);
        Assertions.assertEquals(4.4f, result[0][1]);
        Assertions.assertEquals(2.2f, result[1][0]);
        Assertions.assertEquals(5.5f, result[1][1]);
        Assertions.assertEquals(3.3f, result[2][0]);
        Assertions.assertEquals(6.6f, result[2][1]);
    }

    @Test
    public void testTransposeDouble_NullInput() {
        double[][] input = null;
        double[][] result = Array.transpose(input);
        Assertions.assertNull(result);
    }

    @Test
    public void testTransposeDouble_SquareMatrix() {
        double[][] input = { { 1.11, 2.22, 3.33 }, { 4.44, 5.55, 6.66 }, { 7.77, 8.88, 9.99 } };
        double[][] result = Array.transpose(input);

        Assertions.assertNotNull(result);
        Assertions.assertEquals(3, result.length);
        Assertions.assertEquals(3, result[0].length);

        Assertions.assertEquals(1.11, result[0][0]);
        Assertions.assertEquals(4.44, result[0][1]);
        Assertions.assertEquals(7.77, result[0][2]);
        Assertions.assertEquals(2.22, result[1][0]);
        Assertions.assertEquals(5.55, result[1][1]);
        Assertions.assertEquals(8.88, result[1][2]);
        Assertions.assertEquals(3.33, result[2][0]);
        Assertions.assertEquals(6.66, result[2][1]);
        Assertions.assertEquals(9.99, result[2][2]);
    }

    @Test
    public void testTransposeGeneric_NullInput() {
        String[][] input = null;
        String[][] result = Array.transpose(input);
        Assertions.assertNull(result);
    }

    @Test
    public void testTransposeGeneric_EmptyArray() {
        String[][] input = new String[0][];
        String[][] result = Array.transpose(input);
        Assertions.assertNotNull(result);
        Assertions.assertEquals(0, result.length);
    }

    @Test
    public void testTransposeGeneric_StringMatrix() {
        String[][] input = { { "a", "b", "c" }, { "d", "e", "f" } };
        String[][] result = Array.transpose(input);

        Assertions.assertNotNull(result);
        Assertions.assertEquals(3, result.length);
        Assertions.assertEquals(2, result[0].length);

        Assertions.assertEquals("a", result[0][0]);
        Assertions.assertEquals("d", result[0][1]);
        Assertions.assertEquals("b", result[1][0]);
        Assertions.assertEquals("e", result[1][1]);
        Assertions.assertEquals("c", result[2][0]);
        Assertions.assertEquals("f", result[2][1]);
    }

    @Test
    public void testTransposeGeneric_IntegerMatrix() {
        Integer[][] input = { { 1, 2 }, { 3, 4 }, { 5, 6 } };
        Integer[][] result = Array.transpose(input);

        Assertions.assertNotNull(result);
        Assertions.assertEquals(2, result.length);
        Assertions.assertEquals(3, result[0].length);

        Assertions.assertEquals(Integer.valueOf(1), result[0][0]);
        Assertions.assertEquals(Integer.valueOf(3), result[0][1]);
        Assertions.assertEquals(Integer.valueOf(5), result[0][2]);
        Assertions.assertEquals(Integer.valueOf(2), result[1][0]);
        Assertions.assertEquals(Integer.valueOf(4), result[1][1]);
        Assertions.assertEquals(Integer.valueOf(6), result[1][2]);
    }

    @Test
    public void testTranspose_InvalidMatrix() {
        int[][] input = { { 1, 2, 3 }, { 4, 5 } };

        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            Array.transpose(input);
        });
    }

    @Test
    public void testTransposeBoolean_SingleRow() {
        boolean[][] input = { { true, false, true, false } };
        boolean[][] result = Array.transpose(input);

        Assertions.assertNotNull(result);
        Assertions.assertEquals(4, result.length);
        Assertions.assertEquals(1, result[0].length);

        Assertions.assertTrue(result[0][0]);
        Assertions.assertFalse(result[1][0]);
        Assertions.assertTrue(result[2][0]);
        Assertions.assertFalse(result[3][0]);
    }

    @Test
    public void testTransposeInt_SingleColumn() {
        int[][] input = { { 1 }, { 2 }, { 3 }, { 4 } };
        int[][] result = Array.transpose(input);

        Assertions.assertNotNull(result);
        Assertions.assertEquals(1, result.length);
        Assertions.assertEquals(4, result[0].length);

        Assertions.assertEquals(1, result[0][0]);
        Assertions.assertEquals(2, result[0][1]);
        Assertions.assertEquals(3, result[0][2]);
        Assertions.assertEquals(4, result[0][3]);
    }

    @Test
    public void testBoxByte3D_WithNullSubArrays() {
        byte[][][] input = { null, { { 1, 2 }, { 3, 4 } }, null };
        Byte[][][] result = Array.box(input);

        Assertions.assertNotNull(result);
        Assertions.assertEquals(3, result.length);
        Assertions.assertNull(result[0]);
        Assertions.assertNotNull(result[1]);
        Assertions.assertNull(result[2]);

        Assertions.assertEquals(Byte.valueOf((byte) 1), result[1][0][0]);
        Assertions.assertEquals(Byte.valueOf((byte) 4), result[1][1][1]);
    }

    @Test
    public void testUnboxBoolean3D_WithNullSubArrays() {
        Boolean[][][] input = { { { true, false }, null }, null, { { null, true }, { false, null } } };
        boolean[][][] result = Array.unbox(input, true);

        Assertions.assertNotNull(result);
        Assertions.assertEquals(3, result.length);
        Assertions.assertNotNull(result[0]);
        Assertions.assertNull(result[1]);
        Assertions.assertNotNull(result[2]);

        Assertions.assertTrue(result[0][0][0]);
        Assertions.assertFalse(result[0][0][1]);
        Assertions.assertNull(result[0][1]);
    }

    @Test
    public void testTransposeDouble_LargeMatrix() {
        int rows = 100;
        int cols = 50;
        double[][] input = new double[rows][cols];

        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                input[i][j] = i * cols + j;
            }
        }

        double[][] result = Array.transpose(input);

        Assertions.assertNotNull(result);
        Assertions.assertEquals(cols, result.length);
        Assertions.assertEquals(rows, result[0].length);

        Assertions.assertEquals(0.0, result[0][0]);
        Assertions.assertEquals(1.0, result[1][0]);
        Assertions.assertEquals(50.0, result[0][1]);
        Assertions.assertEquals(input[rows - 1][cols - 1], result[cols - 1][rows - 1]);
    }

    @Test
    public void testUnboxInteger_AllNullValues() {
        Integer[] input = { null, null, null };
        int[] result = Array.unbox(input, -1);

        Assertions.assertNotNull(result);
        Assertions.assertEquals(3, result.length);
        Assertions.assertEquals(-1, result[0]);
        Assertions.assertEquals(-1, result[1]);
        Assertions.assertEquals(-1, result[2]);
    }

    @Test
    public void testUnboxDouble2D_MixedNullRows() {
        Double[][] input = { { 1.1, 2.2, 3.3 }, null, { 4.4, null, 6.6 } };
        double[][] result = Array.unbox(input, 0.0);

        Assertions.assertNotNull(result);
        Assertions.assertEquals(3, result.length);
        Assertions.assertNotNull(result[0]);
        Assertions.assertNull(result[1]);
        Assertions.assertNotNull(result[2]);

        Assertions.assertEquals(1.1, result[0][0]);
        Assertions.assertEquals(2.2, result[0][1]);
        Assertions.assertEquals(3.3, result[0][2]);
        Assertions.assertEquals(4.4, result[2][0]);
        Assertions.assertEquals(0.0, result[2][1]);
        Assertions.assertEquals(6.6, result[2][2]);
    }

    @Test
    public void testUnboxLongRange_FullArray() {
        Long[] input = { 1L, 2L, 3L, 4L, 5L };
        long[] result = Array.unbox(input, 0, input.length, 0L);

        Assertions.assertNotNull(result);
        Assertions.assertEquals(5, result.length);
        for (int i = 0; i < 5; i++) {
            Assertions.assertEquals(i + 1L, result[i]);
        }
    }

    @Test
    public void testUnboxFloatRange_BoundaryIndices() {
        Float[] input = { 1.0f, 2.0f, 3.0f, 4.0f, 5.0f };

        float[] result1 = Array.unbox(input, 2, 2, 0f);
        Assertions.assertNotNull(result1);
        Assertions.assertEquals(0, result1.length);

        float[] result2 = Array.unbox(input, 2, 3, 0f);
        Assertions.assertNotNull(result2);
        Assertions.assertEquals(1, result2.length);
        Assertions.assertEquals(3.0f, result2[0]);
    }

    @Test
    public void testUnboxByte_MaxMinValues() {
        Byte[] input = { Byte.MAX_VALUE, null, Byte.MIN_VALUE };
        byte[] result = Array.unbox(input, (byte) 0);

        Assertions.assertNotNull(result);
        Assertions.assertEquals(3, result.length);
        Assertions.assertEquals(Byte.MAX_VALUE, result[0]);
        Assertions.assertEquals((byte) 0, result[1]);
        Assertions.assertEquals(Byte.MIN_VALUE, result[2]);
    }

    @Test
    public void testBoxDouble3D_MaxMinValues() {
        double[][][] input = { { { Double.MAX_VALUE, Double.MIN_VALUE } }, { { Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY } },
                { { Double.NaN, 0.0 } } };
        Double[][][] result = Array.box(input);

        Assertions.assertNotNull(result);
        Assertions.assertEquals(Double.MAX_VALUE, result[0][0][0]);
        Assertions.assertEquals(Double.MIN_VALUE, result[0][0][1]);
        Assertions.assertEquals(Double.POSITIVE_INFINITY, result[1][0][0]);
        Assertions.assertEquals(Double.NEGATIVE_INFINITY, result[1][0][1]);
        Assertions.assertTrue(Double.isNaN(result[2][0][0]));
        Assertions.assertEquals(0.0, result[2][0][1]);
    }

    @Test
    public void testTransposeGeneric_SparseMatrix() {
        Integer[][] input = new Integer[10][10];
        for (int i = 0; i < 10; i++) {
            for (int j = 0; j < 10; j++) {
                input[i][j] = (i == j) ? i : null;
            }
        }

        Integer[][] result = Array.transpose(input);

        Assertions.assertNotNull(result);
        Assertions.assertEquals(10, result.length);
        Assertions.assertEquals(10, result[0].length);

        for (int i = 0; i < 10; i++) {
            for (int j = 0; j < 10; j++) {
                if (i == j) {
                    Assertions.assertEquals(Integer.valueOf(i), result[i][j]);
                } else {
                    Assertions.assertNull(result[i][j]);
                }
            }
        }
    }

    @Test
    public void testRepeatBooleanElement() {
        boolean[] result = Array.repeat(true, 5);
        Assertions.assertEquals(5, result.length);
        for (int i = 0; i < 5; i++) {
            Assertions.assertTrue(result[i]);
        }

        result = Array.repeat(false, 3);
        Assertions.assertEquals(3, result.length);
        for (int i = 0; i < 3; i++) {
            Assertions.assertFalse(result[i]);
        }

        result = Array.repeat(true, 0);
        Assertions.assertEquals(0, result.length);

        result = Array.repeat(false, 1);
        Assertions.assertEquals(1, result.length);
        Assertions.assertFalse(result[0]);
    }

    @Test
    public void testRepeatBooleanElementNegativeN() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            Array.repeat(true, -1);
        });
    }

    @Test
    public void testRepeatBooleanArray() {
        boolean[] original = { true, false, true };
        boolean[] result = Array.repeat(original, 3);
        Assertions.assertEquals(9, result.length);
        boolean[] expected = { true, false, true, true, false, true, true, false, true };
        Assertions.assertArrayEquals(expected, result);

        boolean[] single = { false };
        result = Array.repeat(single, 5);
        Assertions.assertEquals(5, result.length);
        for (int i = 0; i < 5; i++) {
            Assertions.assertFalse(result[i]);
        }

        boolean[] empty = {};
        result = Array.repeat(empty, 10);
        Assertions.assertEquals(0, result.length);

        result = Array.repeat(original, 0);
        Assertions.assertEquals(0, result.length);

        result = Array.repeat(original, 1);
        Assertions.assertArrayEquals(original, result);
    }

    @Test
    public void testRepeatBooleanArrayNegativeN() {
        boolean[] array = { true, false };
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            Array.repeat(array, -1);
        });
    }

    @Test
    public void testRepeatCharElement() {
        char[] result = Array.repeat('a', 5);
        Assertions.assertEquals(5, result.length);
        for (int i = 0; i < 5; i++) {
            Assertions.assertEquals('a', result[i]);
        }

        result = Array.repeat('Z', 3);
        Assertions.assertEquals(3, result.length);
        for (int i = 0; i < 3; i++) {
            Assertions.assertEquals('Z', result[i]);
        }

        result = Array.repeat('x', 0);
        Assertions.assertEquals(0, result.length);

        result = Array.repeat('1', 1);
        Assertions.assertEquals(1, result.length);
        Assertions.assertEquals('1', result[0]);
    }

    @Test
    public void testRepeatCharElementNegativeN() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            Array.repeat('a', -1);
        });
    }

    @Test
    public void testRepeatCharArray() {
        char[] original = { 'a', 'b', 'c' };
        char[] result = Array.repeat(original, 3);
        Assertions.assertEquals(9, result.length);
        char[] expected = { 'a', 'b', 'c', 'a', 'b', 'c', 'a', 'b', 'c' };
        Assertions.assertArrayEquals(expected, result);

        char[] single = { 'x' };
        result = Array.repeat(single, 5);
        Assertions.assertEquals(5, result.length);
        for (int i = 0; i < 5; i++) {
            Assertions.assertEquals('x', result[i]);
        }

        char[] empty = {};
        result = Array.repeat(empty, 10);
        Assertions.assertEquals(0, result.length);

        result = Array.repeat(original, 0);
        Assertions.assertEquals(0, result.length);

        result = Array.repeat(original, 1);
        Assertions.assertArrayEquals(original, result);
    }

    @Test
    public void testRepeatCharArrayNegativeN() {
        char[] array = { 'a', 'b' };
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            Array.repeat(array, -1);
        });
    }

    @Test
    public void testRepeatByteElement() {
        byte[] result = Array.repeat((byte) 5, 5);
        Assertions.assertEquals(5, result.length);
        for (int i = 0; i < 5; i++) {
            Assertions.assertEquals((byte) 5, result[i]);
        }

        result = Array.repeat((byte) -128, 3);
        Assertions.assertEquals(3, result.length);
        for (int i = 0; i < 3; i++) {
            Assertions.assertEquals((byte) -128, result[i]);
        }

        result = Array.repeat((byte) 0, 0);
        Assertions.assertEquals(0, result.length);

        result = Array.repeat((byte) 127, 1);
        Assertions.assertEquals(1, result.length);
        Assertions.assertEquals((byte) 127, result[0]);
    }

    @Test
    public void testRepeatByteElementNegativeN() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            Array.repeat((byte) 1, -1);
        });
    }

    @Test
    public void testRepeatByteArray() {
        byte[] original = { 1, 2, 3 };
        byte[] result = Array.repeat(original, 3);
        Assertions.assertEquals(9, result.length);
        byte[] expected = { 1, 2, 3, 1, 2, 3, 1, 2, 3 };
        Assertions.assertArrayEquals(expected, result);

        byte[] single = { 0 };
        result = Array.repeat(single, 5);
        Assertions.assertEquals(5, result.length);
        for (int i = 0; i < 5; i++) {
            Assertions.assertEquals((byte) 0, result[i]);
        }

        byte[] empty = {};
        result = Array.repeat(empty, 10);
        Assertions.assertEquals(0, result.length);

        result = Array.repeat(original, 0);
        Assertions.assertEquals(0, result.length);

        result = Array.repeat(original, 1);
        Assertions.assertArrayEquals(original, result);
    }

    @Test
    public void testRepeatByteArrayNegativeN() {
        byte[] array = { 1, 2 };
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            Array.repeat(array, -1);
        });
    }

    @Test
    public void testRepeatShortElement() {
        short[] result = Array.repeat((short) 100, 5);
        Assertions.assertEquals(5, result.length);
        for (int i = 0; i < 5; i++) {
            Assertions.assertEquals((short) 100, result[i]);
        }

        result = Array.repeat((short) -32768, 3);
        Assertions.assertEquals(3, result.length);
        for (int i = 0; i < 3; i++) {
            Assertions.assertEquals((short) -32768, result[i]);
        }

        result = Array.repeat((short) 0, 0);
        Assertions.assertEquals(0, result.length);

        result = Array.repeat((short) 32767, 1);
        Assertions.assertEquals(1, result.length);
        Assertions.assertEquals((short) 32767, result[0]);
    }

    @Test
    public void testRepeatShortElementNegativeN() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            Array.repeat((short) 1, -1);
        });
    }

    @Test
    public void testRepeatShortArray() {
        short[] original = { 1, 2, 3 };
        short[] result = Array.repeat(original, 3);
        Assertions.assertEquals(9, result.length);
        short[] expected = { 1, 2, 3, 1, 2, 3, 1, 2, 3 };
        Assertions.assertArrayEquals(expected, result);

        short[] single = { 0 };
        result = Array.repeat(single, 5);
        Assertions.assertEquals(5, result.length);
        for (int i = 0; i < 5; i++) {
            Assertions.assertEquals((short) 0, result[i]);
        }

        short[] empty = {};
        result = Array.repeat(empty, 10);
        Assertions.assertEquals(0, result.length);

        result = Array.repeat(original, 0);
        Assertions.assertEquals(0, result.length);

        result = Array.repeat(original, 1);
        Assertions.assertArrayEquals(original, result);
    }

    @Test
    public void testRepeatShortArrayNegativeN() {
        short[] array = { 1, 2 };
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            Array.repeat(array, -1);
        });
    }

    @Test
    public void testRepeatIntElement() {
        int[] result = Array.repeat(42, 5);
        Assertions.assertEquals(5, result.length);
        for (int i = 0; i < 5; i++) {
            Assertions.assertEquals(42, result[i]);
        }

        result = Array.repeat(-2147483648, 3);
        Assertions.assertEquals(3, result.length);
        for (int i = 0; i < 3; i++) {
            Assertions.assertEquals(-2147483648, result[i]);
        }

        result = Array.repeat(0, 0);
        Assertions.assertEquals(0, result.length);

        result = Array.repeat(2147483647, 1);
        Assertions.assertEquals(1, result.length);
        Assertions.assertEquals(2147483647, result[0]);
    }

    @Test
    public void testRepeatIntElementNegativeN() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            Array.repeat(1, -1);
        });
    }

    @Test
    public void testRepeatIntArray() {
        int[] original = { 1, 2, 3 };
        int[] result = Array.repeat(original, 3);
        Assertions.assertEquals(9, result.length);
        int[] expected = { 1, 2, 3, 1, 2, 3, 1, 2, 3 };
        Assertions.assertArrayEquals(expected, result);

        int[] single = { 0 };
        result = Array.repeat(single, 5);
        Assertions.assertEquals(5, result.length);
        for (int i = 0; i < 5; i++) {
            Assertions.assertEquals(0, result[i]);
        }

        int[] empty = {};
        result = Array.repeat(empty, 10);
        Assertions.assertEquals(0, result.length);

        result = Array.repeat(original, 0);
        Assertions.assertEquals(0, result.length);

        result = Array.repeat(original, 1);
        Assertions.assertArrayEquals(original, result);
    }

    @Test
    public void testRepeatIntArrayNegativeN() {
        int[] array = { 1, 2 };
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            Array.repeat(array, -1);
        });
    }

    @Test
    public void testRepeatLongElement() {
        long[] result = Array.repeat(1000000000L, 5);
        Assertions.assertEquals(5, result.length);
        for (int i = 0; i < 5; i++) {
            Assertions.assertEquals(1000000000L, result[i]);
        }

        result = Array.repeat(Long.MIN_VALUE, 3);
        Assertions.assertEquals(3, result.length);
        for (int i = 0; i < 3; i++) {
            Assertions.assertEquals(Long.MIN_VALUE, result[i]);
        }

        result = Array.repeat(0L, 0);
        Assertions.assertEquals(0, result.length);

        result = Array.repeat(Long.MAX_VALUE, 1);
        Assertions.assertEquals(1, result.length);
        Assertions.assertEquals(Long.MAX_VALUE, result[0]);
    }

    @Test
    public void testRepeatLongElementNegativeN() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            Array.repeat(1L, -1);
        });
    }

    @Test
    public void testRepeatLongArray() {
        long[] original = { 1L, 2L, 3L };
        long[] result = Array.repeat(original, 3);
        Assertions.assertEquals(9, result.length);
        long[] expected = { 1L, 2L, 3L, 1L, 2L, 3L, 1L, 2L, 3L };
        Assertions.assertArrayEquals(expected, result);

        long[] single = { 0L };
        result = Array.repeat(single, 5);
        Assertions.assertEquals(5, result.length);
        for (int i = 0; i < 5; i++) {
            Assertions.assertEquals(0L, result[i]);
        }

        long[] empty = {};
        result = Array.repeat(empty, 10);
        Assertions.assertEquals(0, result.length);

        result = Array.repeat(original, 0);
        Assertions.assertEquals(0, result.length);

        result = Array.repeat(original, 1);
        Assertions.assertArrayEquals(original, result);
    }

    @Test
    public void testRepeatLongArrayNegativeN() {
        long[] array = { 1L, 2L };
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            Array.repeat(array, -1);
        });
    }

    @Test
    public void testRepeatFloatElement() {
        float[] result = Array.repeat(3.14f, 5);
        Assertions.assertEquals(5, result.length);
        for (int i = 0; i < 5; i++) {
            Assertions.assertEquals(3.14f, result[i], 0.0001f);
        }

        result = Array.repeat(-Float.MAX_VALUE, 3);
        Assertions.assertEquals(3, result.length);
        for (int i = 0; i < 3; i++) {
            Assertions.assertEquals(-Float.MAX_VALUE, result[i], 0.0001f);
        }

        result = Array.repeat(0.0f, 0);
        Assertions.assertEquals(0, result.length);

        result = Array.repeat(Float.MAX_VALUE, 1);
        Assertions.assertEquals(1, result.length);
        Assertions.assertEquals(Float.MAX_VALUE, result[0], 0.0001f);
    }

    @Test
    public void testRepeatFloatElementNegativeN() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            Array.repeat(1.0f, -1);
        });
    }

    @Test
    public void testRepeatFloatArray() {
        float[] original = { 1.1f, 2.2f, 3.3f };
        float[] result = Array.repeat(original, 3);
        Assertions.assertEquals(9, result.length);
        float[] expected = { 1.1f, 2.2f, 3.3f, 1.1f, 2.2f, 3.3f, 1.1f, 2.2f, 3.3f };
        Assertions.assertArrayEquals(expected, result, 0.0001f);

        float[] single = { 0.0f };
        result = Array.repeat(single, 5);
        Assertions.assertEquals(5, result.length);
        for (int i = 0; i < 5; i++) {
            Assertions.assertEquals(0.0f, result[i], 0.0001f);
        }

        float[] empty = {};
        result = Array.repeat(empty, 10);
        Assertions.assertEquals(0, result.length);

        result = Array.repeat(original, 0);
        Assertions.assertEquals(0, result.length);

        result = Array.repeat(original, 1);
        Assertions.assertArrayEquals(original, result, 0.0001f);
    }

    @Test
    public void testRepeatFloatArrayNegativeN() {
        float[] array = { 1.0f, 2.0f };
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            Array.repeat(array, -1);
        });
    }

    @Test
    public void testRepeatDoubleElement() {
        double[] result = Array.repeat(3.14159, 5);
        Assertions.assertEquals(5, result.length);
        for (int i = 0; i < 5; i++) {
            Assertions.assertEquals(3.14159, result[i], 0.00001);
        }

        result = Array.repeat(-Double.MAX_VALUE, 3);
        Assertions.assertEquals(3, result.length);
        for (int i = 0; i < 3; i++) {
            Assertions.assertEquals(-Double.MAX_VALUE, result[i], 0.00001);
        }

        result = Array.repeat(0.0, 0);
        Assertions.assertEquals(0, result.length);

        result = Array.repeat(Double.MAX_VALUE, 1);
        Assertions.assertEquals(1, result.length);
        Assertions.assertEquals(Double.MAX_VALUE, result[0], 0.00001);
    }

    @Test
    public void testRepeatDoubleElementNegativeN() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            Array.repeat(1.0, -1);
        });
    }

    @Test
    public void testRepeatDoubleArray() {
        double[] original = { 1.1, 2.2, 3.3 };
        double[] result = Array.repeat(original, 3);
        Assertions.assertEquals(9, result.length);
        double[] expected = { 1.1, 2.2, 3.3, 1.1, 2.2, 3.3, 1.1, 2.2, 3.3 };
        Assertions.assertArrayEquals(expected, result, 0.00001);

        double[] single = { 0.0 };
        result = Array.repeat(single, 5);
        Assertions.assertEquals(5, result.length);
        for (int i = 0; i < 5; i++) {
            Assertions.assertEquals(0.0, result[i], 0.00001);
        }

        double[] empty = {};
        result = Array.repeat(empty, 10);
        Assertions.assertEquals(0, result.length);

        result = Array.repeat(original, 0);
        Assertions.assertEquals(0, result.length);

        result = Array.repeat(original, 1);
        Assertions.assertArrayEquals(original, result, 0.00001);
    }

    @Test
    public void testRepeatDoubleArrayNegativeN() {
        double[] array = { 1.0, 2.0 };
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            Array.repeat(array, -1);
        });
    }

    @Test
    public void testRepeatStringElement() {
        String[] result = Array.repeat("hello", 5);
        Assertions.assertEquals(5, result.length);
        for (int i = 0; i < 5; i++) {
            Assertions.assertEquals("hello", result[i]);
        }

        result = Array.repeat("", 3);
        Assertions.assertEquals(3, result.length);
        for (int i = 0; i < 3; i++) {
            Assertions.assertEquals("", result[i]);
        }

        result = Array.repeat("test", 0);
        Assertions.assertEquals(0, result.length);

        result = Array.repeat("single", 1);
        Assertions.assertEquals(1, result.length);
        Assertions.assertEquals("single", result[0]);

        result = Array.repeat((String) null, 3);
        Assertions.assertEquals(3, result.length);
        for (int i = 0; i < 3; i++) {
            Assertions.assertNull(result[i]);
        }
    }

    @Test
    public void testRepeatStringElementNegativeN() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            Array.repeat("test", -1);
        });
    }

    @Test
    public void testRepeatStringArray() {
        String[] original = { "a", "b", "c" };
        String[] result = Array.repeat(original, 3);
        Assertions.assertEquals(9, result.length);
        String[] expected = { "a", "b", "c", "a", "b", "c", "a", "b", "c" };
        Assertions.assertArrayEquals(expected, result);

        String[] single = { "x" };
        result = Array.repeat(single, 5);
        Assertions.assertEquals(5, result.length);
        for (int i = 0; i < 5; i++) {
            Assertions.assertEquals("x", result[i]);
        }

        String[] empty = {};
        result = Array.repeat(empty, 10);
        Assertions.assertEquals(0, result.length);

        result = Array.repeat(original, 0);
        Assertions.assertEquals(0, result.length);

        result = Array.repeat(original, 1);
        Assertions.assertArrayEquals(original, result);

        String[] withNulls = { "a", null, "c" };
        result = Array.repeat(withNulls, 2);
        Assertions.assertEquals(6, result.length);
        String[] expectedWithNulls = { "a", null, "c", "a", null, "c" };
        Assertions.assertArrayEquals(expectedWithNulls, result);
    }

    @Test
    public void testRepeatStringArrayNegativeN() {
        String[] array = { "a", "b" };
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            Array.repeat(array, -1);
        });
    }

    @Test
    public void testRepeatGenericElementNullElement() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            Array.repeat((Integer) null, 5);
        });
    }

    @Test
    public void testRepeatGenericElementNegativeN() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            Array.repeat(42, -1);
        });
    }

    @Test
    public void testRepeatGenericElementWithClassNegativeN() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            Array.repeat(42, -1, Integer.class);
        });
    }

    @Test
    public void testRepeatGenericArrayWithClass() {
        Integer[] original = { 1, 2, 3 };
        Integer[] result = Array.repeat(original, 3, Integer.class);
        Assertions.assertEquals(9, result.length);
        Integer[] expected = { 1, 2, 3, 1, 2, 3, 1, 2, 3 };
        Assertions.assertArrayEquals(expected, result);

        Integer[] single = { 0 };
        result = Array.repeat(single, 5, Integer.class);
        Assertions.assertEquals(5, result.length);
        for (int i = 0; i < 5; i++) {
            Assertions.assertEquals(Integer.valueOf(0), result[i]);
        }

        Integer[] empty = {};
        result = Array.repeat(empty, 10, Integer.class);
        Assertions.assertEquals(0, result.length);

        result = Array.repeat(original, 0, Integer.class);
        Assertions.assertEquals(0, result.length);

        result = Array.repeat(original, 1, Integer.class);
        Assertions.assertArrayEquals(original, result);
    }

    @Test
    public void testRepeatGenericArrayWithClassNegativeN() {
        Integer[] array = { 1, 2 };
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            Array.repeat(array, -1, Integer.class);
        });
    }

    @Test
    public void testRepeatNonNullElementNullElement() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            Array.repeatNonNull(null, 5);
        });
    }

    @Test
    public void testRepeatNonNullElementNegativeN() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            Array.repeatNonNull(42, -1);
        });
    }

    @Test
    public void testNewInstance_SingleDimension() {
        Integer[] zeroArray = Array.newInstance(Integer.class, 0);
        Assertions.assertNotNull(zeroArray);
        Assertions.assertEquals(0, zeroArray.length);

        String[] strArray = Array.newInstance(String.class, 5);
        Assertions.assertNotNull(strArray);
        Assertions.assertEquals(5, strArray.length);

        int[] intArray = Array.newInstance(int.class, 3);
        Assertions.assertNotNull(intArray);
        Assertions.assertEquals(3, intArray.length);
    }

    @Test
    public void testNewInstance_SingleDimension_NegativeLength() {
        Assertions.assertThrows(NegativeArraySizeException.class, () -> {
            Array.newInstance(String.class, -1);
        });
    }

    @Test
    public void testNewInstance_MultiDimension() {
        Integer[][] array2D = Array.newInstance(Integer.class, 3, 4);
        Assertions.assertNotNull(array2D);
        Assertions.assertEquals(3, array2D.length);
        Assertions.assertEquals(4, array2D[0].length);

        String[][][] array3D = Array.newInstance(String.class, 2, 3, 4);
        Assertions.assertNotNull(array3D);
        Assertions.assertEquals(2, array3D.length);
        Assertions.assertEquals(3, array3D[0].length);
        Assertions.assertEquals(4, array3D[0][0].length);
    }

    @Test
    public void testNewInstance_MultiDimension_NegativeDimension() {
        Assertions.assertThrows(NegativeArraySizeException.class, () -> {
            Array.newInstance(String.class, 3, -1);
        });
    }

    @Test
    public void testGetLength_NotArray() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            Array.getLength("not an array");
        });
    }

    @Test
    public void testGet_IndexOutOfBounds() {
        Integer[] array = { 10, 20, 30 };
        Assertions.assertThrows(ArrayIndexOutOfBoundsException.class, () -> {
            Array.get(array, -1);
        });
        Assertions.assertThrows(ArrayIndexOutOfBoundsException.class, () -> {
            Array.get(array, 3);
        });
    }

    @Test
    public void testGet_NotArray() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            Array.get("not an array", 0);
        });
    }

    @Test
    public void testSet_IndexOutOfBounds() {
        Integer[] array = { 10, 20, 30 };
        Assertions.assertThrows(ArrayIndexOutOfBoundsException.class, () -> {
            Array.set(array, -1, 100);
        });
        Assertions.assertThrows(ArrayIndexOutOfBoundsException.class, () -> {
            Array.set(array, 3, 100);
        });
    }

    @Test
    public void testOf_boolean() {
        boolean[] array = Array.of(true, false, true);
        Assertions.assertEquals(3, array.length);
        Assertions.assertTrue(array[0]);
        Assertions.assertFalse(array[1]);
        Assertions.assertTrue(array[2]);
    }

    @Test
    public void testOf_char() {
        char[] array = Array.of('a', 'b', 'c');
        Assertions.assertEquals(3, array.length);
        Assertions.assertEquals('a', array[0]);
        Assertions.assertEquals('b', array[1]);
        Assertions.assertEquals('c', array[2]);
    }

    @Test
    public void testOf_byte() {
        byte[] array = Array.of((byte) 1, (byte) 2, (byte) 3);
        Assertions.assertEquals(3, array.length);
        Assertions.assertEquals((byte) 1, array[0]);
        Assertions.assertEquals((byte) 2, array[1]);
        Assertions.assertEquals((byte) 3, array[2]);
    }

    @Test
    public void testOf_short() {
        short[] array = Array.of((short) 10, (short) 20, (short) 30);
        Assertions.assertEquals(3, array.length);
        Assertions.assertEquals((short) 10, array[0]);
        Assertions.assertEquals((short) 20, array[1]);
        Assertions.assertEquals((short) 30, array[2]);
    }

    @Test
    public void testOf_int() {
        int[] array = Array.of(100, 200, 300);
        Assertions.assertEquals(3, array.length);
        Assertions.assertEquals(100, array[0]);
        Assertions.assertEquals(200, array[1]);
        Assertions.assertEquals(300, array[2]);
    }

    @Test
    public void testOf_long() {
        long[] array = Array.of(1000L, 2000L, 3000L);
        Assertions.assertEquals(3, array.length);
        Assertions.assertEquals(1000L, array[0]);
        Assertions.assertEquals(2000L, array[1]);
        Assertions.assertEquals(3000L, array[2]);
    }

    @Test
    public void testOf_float() {
        float[] array = Array.of(1.1f, 2.2f, 3.3f);
        Assertions.assertEquals(3, array.length);
        Assertions.assertEquals(1.1f, array[0]);
        Assertions.assertEquals(2.2f, array[1]);
        Assertions.assertEquals(3.3f, array[2]);
    }

    @Test
    public void testOf_double() {
        double[] array = Array.of(1.11, 2.22, 3.33);
        Assertions.assertEquals(3, array.length);
        Assertions.assertEquals(1.11, array[0]);
        Assertions.assertEquals(2.22, array[1]);
        Assertions.assertEquals(3.33, array[2]);
    }

    @Test
    public void testOf_String() {
        String[] array = Array.of("a", "b", "c");
        Assertions.assertEquals(3, array.length);
        Assertions.assertEquals("a", array[0]);
        Assertions.assertEquals("b", array[1]);
        Assertions.assertEquals("c", array[2]);
    }

    @Test
    public void testOf_Date() {
        java.util.Date d1 = new java.util.Date();
        java.util.Date d2 = new java.util.Date();
        java.util.Date[] array = Array.of(d1, d2);
        Assertions.assertEquals(2, array.length);
        Assertions.assertEquals(d1, array[0]);
        Assertions.assertEquals(d2, array[1]);
    }

    @Test
    public void testOf_Calendar() {
        java.util.Calendar c1 = java.util.Calendar.getInstance();
        java.util.Calendar c2 = java.util.Calendar.getInstance();
        java.util.Calendar[] array = Array.of(c1, c2);
        Assertions.assertEquals(2, array.length);
        Assertions.assertEquals(c1, array[0]);
        Assertions.assertEquals(c2, array[1]);
    }

    @Test
    public void testOf_Temporal() {
        java.time.LocalDate d1 = java.time.LocalDate.now();
        java.time.LocalDate d2 = java.time.LocalDate.now().plusDays(1);
        java.time.LocalDate[] array = Array.of(d1, d2);
        Assertions.assertEquals(2, array.length);
        Assertions.assertEquals(d1, array[0]);
        Assertions.assertEquals(d2, array[1]);
    }

    @Test
    public void testOf_Enum() {
        Thread.State s1 = Thread.State.NEW;
        Thread.State s2 = Thread.State.RUNNABLE;
        Thread.State[] array = Array.of(s1, s2);
        Assertions.assertEquals(2, array.length);
        Assertions.assertEquals(s1, array[0]);
        Assertions.assertEquals(s2, array[1]);
    }

    @Test
    public void testRange_int_overflow() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            Array.range(Integer.MIN_VALUE, Integer.MAX_VALUE);
        });
    }

    @Test
    public void testRange_long_overflow() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            Array.range(0L, (long) Integer.MAX_VALUE + 1);
        });
    }

    @Test
    public void testRange_char_withStep() {
        char[] array = Array.range('a', 'g', 2);
        Assertions.assertArrayEquals(new char[] { 'a', 'c', 'e' }, array);

        char[] negativeStep = Array.range('z', 'w', -1);
        Assertions.assertArrayEquals(new char[] { 'z', 'y', 'x' }, negativeStep);

        char[] empty = Array.range('a', 'a', 1);
        Assertions.assertEquals(0, empty.length);

        char[] inconsistent = Array.range('a', 'd', -1);
        Assertions.assertEquals(0, inconsistent.length);
    }

    @Test
    public void testRange_char_withStep_zeroStep() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            Array.range('a', 'd', 0);
        });
    }

    @Test
    public void testRange_byte_withStep() {
        byte[] array = Array.range((byte) 1, (byte) 10, (byte) 3);
        Assertions.assertArrayEquals(new byte[] { 1, 4, 7 }, array);

        byte[] negativeStep = Array.range((byte) 10, (byte) 1, (byte) -3);
        Assertions.assertArrayEquals(new byte[] { 10, 7, 4 }, negativeStep);
    }

    @Test
    public void testRange_byte_withStep_zeroStep() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            Array.range((byte) 1, (byte) 10, (byte) 0);
        });
    }

    @Test
    public void testRange_short_withStep() {
        short[] array = Array.range((short) 10, (short) 25, (short) 5);
        Assertions.assertArrayEquals(new short[] { 10, 15, 20 }, array);

        short[] negativeStep = Array.range((short) 20, (short) 5, (short) -5);
        Assertions.assertArrayEquals(new short[] { 20, 15, 10 }, negativeStep);
    }

    @Test
    public void testRange_short_withStep_zeroStep() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            Array.range((short) 10, (short) 20, (short) 0);
        });
    }

    @Test
    public void testRange_int_withStep() {
        int[] array = Array.range(100, 130, 10);
        Assertions.assertArrayEquals(new int[] { 100, 110, 120 }, array);

        int[] negativeStep = Array.range(100, 70, -10);
        Assertions.assertArrayEquals(new int[] { 100, 90, 80 }, negativeStep);
    }

    @Test
    public void testRange_int_withStep_zeroStep() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            Array.range(100, 200, 0);
        });
    }

    @Test
    public void testRange_int_withStep_overflow() {
        Assertions.assertThrows(OutOfMemoryError.class, () -> {
            Array.range(0, Integer.MAX_VALUE, 1);
        });
    }

    @Test
    public void testRange_long_withStep() {
        long[] array = Array.range(1000L, 1030L, 10L);
        Assertions.assertArrayEquals(new long[] { 1000L, 1010L, 1020L }, array);

        long[] negativeStep = Array.range(1000L, 970L, -10L);
        Assertions.assertArrayEquals(new long[] { 1000L, 990L, 980L }, negativeStep);
    }

    @Test
    public void testRange_long_withStep_zeroStep() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            Array.range(1000L, 2000L, 0L);
        });
    }

    @Test
    public void testRange_long_withStep_overflow() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            Array.range(0L, Long.MAX_VALUE, 1L);
        });
    }

    @Test
    public void testRangeClosed_int_overflow() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            Array.rangeClosed(Integer.MIN_VALUE, Integer.MAX_VALUE);
        });
    }

    @Test
    public void testRangeClosed_long_overflow() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            Array.rangeClosed(0L, Integer.MAX_VALUE);
        });
    }

    @Test
    public void testRangeClosed_char_withStep() {
        char[] array = Array.rangeClosed('a', 'e', 2);
        Assertions.assertArrayEquals(new char[] { 'a', 'c', 'e' }, array);

        char[] negativeStep = Array.rangeClosed('z', 'x', -1);
        Assertions.assertArrayEquals(new char[] { 'z', 'y', 'x' }, negativeStep);

        char[] single = Array.rangeClosed('m', 'm', 1);
        Assertions.assertArrayEquals(new char[] { 'm' }, single);
    }

    @Test
    public void testRangeClosed_char_withStep_zeroStep() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            Array.rangeClosed('a', 'e', 0);
        });
    }

    @Test
    public void testRangeClosed_byte_withStep() {
        byte[] array = Array.rangeClosed((byte) 1, (byte) 7, (byte) 3);
        Assertions.assertArrayEquals(new byte[] { 1, 4, 7 }, array);

        byte[] negativeStep = Array.rangeClosed((byte) 10, (byte) 4, (byte) -3);
        Assertions.assertArrayEquals(new byte[] { 10, 7, 4 }, negativeStep);
    }

    @Test
    public void testRangeClosed_short_withStep() {
        short[] array = Array.rangeClosed((short) 10, (short) 20, (short) 5);
        Assertions.assertArrayEquals(new short[] { 10, 15, 20 }, array);

        short[] negativeStep = Array.rangeClosed((short) 20, (short) 10, (short) -5);
        Assertions.assertArrayEquals(new short[] { 20, 15, 10 }, negativeStep);
    }

    @Test
    public void testRangeClosed_int_withStep() {
        int[] array = Array.rangeClosed(100, 120, 10);
        Assertions.assertArrayEquals(new int[] { 100, 110, 120 }, array);

        int[] negativeStep = Array.rangeClosed(100, 80, -10);
        Assertions.assertArrayEquals(new int[] { 100, 90, 80 }, negativeStep);
    }

    @Test
    public void testRangeClosed_long_withStep() {
        long[] array = Array.rangeClosed(1000L, 1020L, 10L);
        Assertions.assertArrayEquals(new long[] { 1000L, 1010L, 1020L }, array);

        long[] negativeStep = Array.rangeClosed(1000L, 980L, -10L);
        Assertions.assertArrayEquals(new long[] { 1000L, 990L, 980L }, negativeStep);
    }

    @Test
    public void testRepeat_boolean() {
        boolean[] array = Array.repeat(true, 3);
        Assertions.assertArrayEquals(new boolean[] { true, true, true }, array);

        boolean[] empty = Array.repeat(false, 0);
        Assertions.assertEquals(0, empty.length);
    }

    @Test
    public void testRepeat_char() {
        char[] array = Array.repeat('x', 4);
        Assertions.assertArrayEquals(new char[] { 'x', 'x', 'x', 'x' }, array);
    }

    @Test
    public void testRepeat_byte() {
        byte[] array = Array.repeat((byte) 5, 3);
        Assertions.assertArrayEquals(new byte[] { 5, 5, 5 }, array);
    }

    @Test
    public void testRepeat_short() {
        short[] array = Array.repeat((short) 10, 2);
        Assertions.assertArrayEquals(new short[] { 10, 10 }, array);
    }

    @Test
    public void testRepeat_int() {
        int[] array = Array.repeat(100, 3);
        Assertions.assertArrayEquals(new int[] { 100, 100, 100 }, array);
    }

    @Test
    public void testRepeat_long() {
        long[] array = Array.repeat(1000L, 2);
        Assertions.assertArrayEquals(new long[] { 1000L, 1000L }, array);
    }

    @Test
    public void testRepeat_float() {
        float[] array = Array.repeat(1.5f, 3);
        Assertions.assertArrayEquals(new float[] { 1.5f, 1.5f, 1.5f }, array);
    }

    @Test
    public void testRepeat_double() {
        double[] array = Array.repeat(2.5, 2);
        Assertions.assertArrayEquals(new double[] { 2.5, 2.5 }, array);
    }

    @Test
    public void testRepeat_String() {
        String[] array = Array.repeat("test", 3);
        Assertions.assertArrayEquals(new String[] { "test", "test", "test" }, array);
    }

    @Test
    public void testRepeat_generic_deprecated_nullElement() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            Array.repeat((Object) null, 2);
        });
    }

    @Test
    public void testRepeat_withClass() {
        String[] array = Array.repeat("world", 3, String.class);
        Assertions.assertArrayEquals(new String[] { "world", "world", "world" }, array);

        String[] nullArray = Array.repeat((String) null, 2, String.class);
        Assertions.assertArrayEquals(new String[] { null, null }, nullArray);
    }

    @Test
    public void testRepeatNonNull() {
        Integer[] array = Array.repeatNonNull(42, 3);
        Assertions.assertArrayEquals(new Integer[] { 42, 42, 42 }, array);
    }

    @Test
    public void testRepeatNonNull_nullElement() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            Array.repeatNonNull(null, 2);
        });
    }

    @Test
    public void testRandom() {
        int[] array = Array.random(10);
        Assertions.assertEquals(10, array.length);

        int[] empty = Array.random(0);
        Assertions.assertEquals(0, empty.length);
    }

    @Test
    public void testRandom_withRange() {
        int[] array = Array.random(0, 10, 20);
        Assertions.assertEquals(20, array.length);

        for (int value : array) {
            Assertions.assertTrue(value >= 0 && value < 10);
        }

        int[] negativeRange = Array.random(-10, 0, 15);
        Assertions.assertEquals(15, negativeRange.length);
        for (int value : negativeRange) {
            Assertions.assertTrue(value >= -10 && value < 0);
        }

        int[] largeRange = Array.random(Integer.MIN_VALUE, Integer.MAX_VALUE, 10);
        Assertions.assertEquals(10, largeRange.length);
    }

    @Test
    public void testRandom_withRange_invalidRange() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            Array.random(10, 10, 5);
        });

        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            Array.random(10, 5, 5);
        });
    }

    @Test
    public void testConcat_boolean2D() {
        boolean[][] a = { { true, false }, { false, true } };
        boolean[][] b = { { true, true }, { false, false } };
        boolean[][] result = Array.concat(a, b);

        Assertions.assertEquals(2, result.length);
        Assertions.assertArrayEquals(new boolean[] { true, false, true, true }, result[0]);
        Assertions.assertArrayEquals(new boolean[] { false, true, false, false }, result[1]);

        boolean[][] empty1 = Array.concat(new boolean[0][], b);
        Assertions.assertArrayEquals(b, empty1);

        boolean[][] empty2 = Array.concat(a, new boolean[0][]);
        Assertions.assertArrayEquals(a, empty2);

        boolean[][] null1 = Array.concat(null, b);
        Assertions.assertArrayEquals(b, null1);

        boolean[][] c = { { true } };
        boolean[][] diffLen = Array.concat(a, c);
        Assertions.assertEquals(2, diffLen.length);
    }

    @Test
    public void testConcat_char2D() {
        char[][] a = { { 'a', 'b' }, { 'c', 'd' } };
        char[][] b = { { 'e', 'f' }, { 'g', 'h' } };
        char[][] result = Array.concat(a, b);

        Assertions.assertEquals(2, result.length);
        Assertions.assertArrayEquals(new char[] { 'a', 'b', 'e', 'f' }, result[0]);
        Assertions.assertArrayEquals(new char[] { 'c', 'd', 'g', 'h' }, result[1]);
    }

    @Test
    public void testConcat_byte2D() {
        byte[][] a = { { 1, 2 }, { 3, 4 } };
        byte[][] b = { { 5, 6 }, { 7, 8 } };
        byte[][] result = Array.concat(a, b);

        Assertions.assertEquals(2, result.length);
        Assertions.assertArrayEquals(new byte[] { 1, 2, 5, 6 }, result[0]);
        Assertions.assertArrayEquals(new byte[] { 3, 4, 7, 8 }, result[1]);
    }

    @Test
    public void testConcat_short2D() {
        short[][] a = { { 10, 20 }, { 30, 40 } };
        short[][] b = { { 50, 60 }, { 70, 80 } };
        short[][] result = Array.concat(a, b);

        Assertions.assertEquals(2, result.length);
        Assertions.assertArrayEquals(new short[] { 10, 20, 50, 60 }, result[0]);
        Assertions.assertArrayEquals(new short[] { 30, 40, 70, 80 }, result[1]);
    }

    @Test
    public void testConcat_int2D() {
        int[][] a = { { 100, 200 }, { 300, 400 } };
        int[][] b = { { 500, 600 }, { 700, 800 } };
        int[][] result = Array.concat(a, b);

        Assertions.assertEquals(2, result.length);
        Assertions.assertArrayEquals(new int[] { 100, 200, 500, 600 }, result[0]);
        Assertions.assertArrayEquals(new int[] { 300, 400, 700, 800 }, result[1]);
    }

    @Test
    public void testConcat_long2D() {
        long[][] a = { { 1000L, 2000L }, { 3000L, 4000L } };
        long[][] b = { { 5000L, 6000L }, { 7000L, 8000L } };
        long[][] result = Array.concat(a, b);

        Assertions.assertEquals(2, result.length);
        Assertions.assertArrayEquals(new long[] { 1000L, 2000L, 5000L, 6000L }, result[0]);
        Assertions.assertArrayEquals(new long[] { 3000L, 4000L, 7000L, 8000L }, result[1]);
    }

    @Test
    public void testConcat_float2D() {
        float[][] a = { { 1.1f, 2.2f }, { 3.3f, 4.4f } };
        float[][] b = { { 5.5f, 6.6f }, { 7.7f, 8.8f } };
        float[][] result = Array.concat(a, b);

        Assertions.assertEquals(2, result.length);
        Assertions.assertArrayEquals(new float[] { 1.1f, 2.2f, 5.5f, 6.6f }, result[0]);
        Assertions.assertArrayEquals(new float[] { 3.3f, 4.4f, 7.7f, 8.8f }, result[1]);
    }

    @Test
    public void testConcat_double2D() {
        double[][] a = { { 1.11, 2.22 }, { 3.33, 4.44 } };
        double[][] b = { { 5.55, 6.66 }, { 7.77, 8.88 } };
        double[][] result = Array.concat(a, b);

        Assertions.assertEquals(2, result.length);
        Assertions.assertArrayEquals(new double[] { 1.11, 2.22, 5.55, 6.66 }, result[0]);
        Assertions.assertArrayEquals(new double[] { 3.33, 4.44, 7.77, 8.88 }, result[1]);
    }

    @Test
    public void testConcat_boolean3D() {
        boolean[][][] a = { { { true, false } }, { { false, true } } };
        boolean[][][] b = { { { true, true } }, { { false, false } } };
        boolean[][][] result = Array.concat(a, b);

        Assertions.assertEquals(2, result.length);
        Assertions.assertEquals(1, result[0].length);

        boolean[][][] empty = Array.concat(new boolean[0][][], b);
        Assertions.assertEquals(b.length, empty.length);
    }

    @Test
    public void testConcat_char3D() {
        char[][][] a = { { { 'a', 'b' } }, { { 'c', 'd' } } };
        char[][][] b = { { { 'e', 'f' } }, { { 'g', 'h' } } };
        char[][][] result = Array.concat(a, b);

        Assertions.assertEquals(2, result.length);
    }

    @Test
    public void testConcat_byte3D() {
        byte[][][] a = { { { 1, 2 } }, { { 3, 4 } } };
        byte[][][] b = { { { 5, 6 } }, { { 7, 8 } } };
        byte[][][] result = Array.concat(a, b);

        Assertions.assertEquals(2, result.length);
    }

    @Test
    public void testConcat_short3D() {
        short[][][] a = { { { 10, 20 } }, { { 30, 40 } } };
        short[][][] b = { { { 50, 60 } }, { { 70, 80 } } };
        short[][][] result = Array.concat(a, b);

        Assertions.assertEquals(2, result.length);
    }

    @Test
    public void testConcat_int3D() {
        int[][][] a = { { { 100, 200 } }, { { 300, 400 } } };
        int[][][] b = { { { 500, 600 } }, { { 700, 800 } } };
        int[][][] result = Array.concat(a, b);

        Assertions.assertEquals(2, result.length);
    }

    @Test
    public void testConcat_long3D() {
        long[][][] a = { { { 1000L, 2000L } }, { { 3000L, 4000L } } };
        long[][][] b = { { { 5000L, 6000L } }, { { 7000L, 8000L } } };
        long[][][] result = Array.concat(a, b);

        Assertions.assertEquals(2, result.length);
    }

    @Test
    public void testConcat_float3D() {
        float[][][] a = { { { 1.1f, 2.2f } }, { { 3.3f, 4.4f } } };
        float[][][] b = { { { 5.5f, 6.6f } }, { { 7.7f, 8.8f } } };
        float[][][] result = Array.concat(a, b);

        Assertions.assertEquals(2, result.length);
    }

    @Test
    public void testConcat_double3D() {
        double[][][] a = { { { 1.11, 2.22 } }, { { 3.33, 4.44 } } };
        double[][][] b = { { { 5.55, 6.66 } }, { { 7.77, 8.88 } } };
        double[][][] result = Array.concat(a, b);

        Assertions.assertEquals(2, result.length);
    }

    @Test
    public void testConcatt_2D() {
        String[][] a = { { "a", "b" }, { "c", "d" } };
        String[][] b = { { "e", "f" }, { "g", "h" } };
        String[][] result = Array.concat2D(a, b);

        Assertions.assertEquals(2, result.length);
        Assertions.assertArrayEquals(new String[] { "a", "b", "e", "f" }, result[0]);
        Assertions.assertArrayEquals(new String[] { "c", "d", "g", "h" }, result[1]);

        String[][] empty1 = Array.concat2D(new String[0][], b);
        Assertions.assertArrayEquals(b, empty1);

        String[][] null1 = Array.concat2D(null, b);
        Assertions.assertArrayEquals(b, null1);
    }

    @Test
    public void testConcatt_3D() {
        Integer[][][] a = { { { 1, 2 } }, { { 3, 4 } } };
        Integer[][][] b = { { { 5, 6 } }, { { 7, 8 } } };
        Integer[][][] result = Array.concat3D(a, b);

        Assertions.assertEquals(2, result.length);
    }

    @Test
    public void testBox_boolean_range() {
        boolean[] primitive = { true, false, true, false, true };
        Boolean[] boxed = Array.box(primitive, 1, 4);
        Assertions.assertArrayEquals(new Boolean[] { false, true, false }, boxed);

        Assertions.assertNull(Array.box((boolean[]) null, 0, 0));

        Boolean[] empty = Array.box(primitive, 2, 2);
        Assertions.assertEquals(0, empty.length);
    }

    @Test
    public void testBox_boolean_range_outOfBounds() {
        boolean[] primitive = { true, false };
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> {
            Array.box(primitive, -1, 1);
        });
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> {
            Array.box(primitive, 0, 3);
        });
    }

    @Test
    public void testBox_char() {
        char[] primitive = { 'a', 'b', 'c' };
        Character[] boxed = Array.box(primitive);
        Assertions.assertArrayEquals(new Character[] { 'a', 'b', 'c' }, boxed);

        Assertions.assertNull(Array.box((char[]) null));
    }

    @Test
    public void testBox_char_range() {
        char[] primitive = { 'a', 'b', 'c', 'd', 'e' };
        Character[] boxed = Array.box(primitive, 1, 4);
        Assertions.assertArrayEquals(new Character[] { 'b', 'c', 'd' }, boxed);
    }

    @Test
    public void testBox_byte() {
        byte[] primitive = { 1, 2, 3 };
        Byte[] boxed = Array.box(primitive);
        Assertions.assertArrayEquals(new Byte[] { 1, 2, 3 }, boxed);

        Assertions.assertNull(Array.box((byte[]) null));
    }

    @Test
    public void testBox_byte_range() {
        byte[] primitive = { 1, 2, 3, 4, 5 };
        Byte[] boxed = Array.box(primitive, 2, 5);
        Assertions.assertArrayEquals(new Byte[] { 3, 4, 5 }, boxed);
    }

    @Test
    public void testBox_short() {
        short[] primitive = { 10, 20, 30 };
        Short[] boxed = Array.box(primitive);
        Assertions.assertArrayEquals(new Short[] { 10, 20, 30 }, boxed);

        Assertions.assertNull(Array.box((short[]) null));
    }

    @Test
    public void testBox_short_range() {
        short[] primitive = { 10, 20, 30, 40, 50 };
        Short[] boxed = Array.box(primitive, 0, 3);
        Assertions.assertArrayEquals(new Short[] { 10, 20, 30 }, boxed);
    }

    @Test
    public void testBox_int() {
        int[] primitive = { 100, 200, 300 };
        Integer[] boxed = Array.box(primitive);
        Assertions.assertArrayEquals(new Integer[] { 100, 200, 300 }, boxed);

        Assertions.assertNull(Array.box((int[]) null));
    }

    @Test
    public void testBox_int_range() {
        int[] primitive = { 100, 200, 300, 400, 500 };
        Integer[] boxed = Array.box(primitive, 1, 3);
        Assertions.assertArrayEquals(new Integer[] { 200, 300 }, boxed);
    }

    @Test
    public void testBox_long() {
        long[] primitive = { 1000L, 2000L, 3000L };
        Long[] boxed = Array.box(primitive);
        Assertions.assertArrayEquals(new Long[] { 1000L, 2000L, 3000L }, boxed);

        Assertions.assertNull(Array.box((long[]) null));
    }

    @Test
    public void testBox_long_range() {
        long[] primitive = { 1000L, 2000L, 3000L, 4000L };
        Long[] boxed = Array.box(primitive, 1, 4);
        Assertions.assertArrayEquals(new Long[] { 2000L, 3000L, 4000L }, boxed);
    }

    @Test
    public void testBox_float() {
        float[] primitive = { 1.1f, 2.2f, 3.3f };
        Float[] boxed = Array.box(primitive);
        Assertions.assertArrayEquals(new Float[] { 1.1f, 2.2f, 3.3f }, boxed);

        Assertions.assertNull(Array.box((float[]) null));
    }

    @Test
    public void testBox_float_range() {
        float[] primitive = { 1.1f, 2.2f, 3.3f, 4.4f };
        Float[] boxed = Array.box(primitive, 0, 2);
        Assertions.assertArrayEquals(new Float[] { 1.1f, 2.2f }, boxed);
    }

    @Test
    public void testBox_double() {
        double[] primitive = { 1.11, 2.22, 3.33 };
        Double[] boxed = Array.box(primitive);
        Assertions.assertArrayEquals(new Double[] { 1.11, 2.22, 3.33 }, boxed);

        Assertions.assertNull(Array.box((double[]) null));
    }

    @Test
    public void testBox_double_range() {
        double[] primitive = { 1.11, 2.22, 3.33, 4.44 };
        Double[] boxed = Array.box(primitive, 2, 4);
        Assertions.assertArrayEquals(new Double[] { 3.33, 4.44 }, boxed);
    }

    @Test
    public void testBox_boolean2D() {
        boolean[][] primitive = { { true, false }, { false, true } };
        Boolean[][] boxed = Array.box(primitive);

        Assertions.assertEquals(2, boxed.length);
        Assertions.assertArrayEquals(new Boolean[] { true, false }, boxed[0]);
        Assertions.assertArrayEquals(new Boolean[] { false, true }, boxed[1]);

        Assertions.assertNull(Array.box((boolean[][]) null));
    }

    @Test
    public void testBox_char2D() {
        char[][] primitive = { { 'a', 'b' }, { 'c', 'd' } };
        Character[][] boxed = Array.box(primitive);

        Assertions.assertEquals(2, boxed.length);
        Assertions.assertArrayEquals(new Character[] { 'a', 'b' }, boxed[0]);
        Assertions.assertArrayEquals(new Character[] { 'c', 'd' }, boxed[1]);

        Assertions.assertNull(Array.box((char[][]) null));
    }

    @Test
    public void testBox_byte2D() {
        byte[][] primitive = { { 1, 2 }, { 3, 4 } };
        Byte[][] boxed = Array.box(primitive);

        Assertions.assertEquals(2, boxed.length);
        Assertions.assertArrayEquals(new Byte[] { 1, 2 }, boxed[0]);
        Assertions.assertArrayEquals(new Byte[] { 3, 4 }, boxed[1]);
    }

    @Test
    public void testBox_short2D() {
        short[][] primitive = { { 10, 20 }, { 30, 40 } };
        Short[][] boxed = Array.box(primitive);

        Assertions.assertEquals(2, boxed.length);
        Assertions.assertArrayEquals(new Short[] { 10, 20 }, boxed[0]);
        Assertions.assertArrayEquals(new Short[] { 30, 40 }, boxed[1]);
    }

    @Test
    public void testBox_int2D() {
        int[][] primitive = { { 100, 200 }, { 300, 400 } };
        Integer[][] boxed = Array.box(primitive);

        Assertions.assertEquals(2, boxed.length);
        Assertions.assertArrayEquals(new Integer[] { 100, 200 }, boxed[0]);
        Assertions.assertArrayEquals(new Integer[] { 300, 400 }, boxed[1]);
    }

    @Test
    public void testBox_long2D() {
        long[][] primitive = { { 1000L, 2000L }, { 3000L, 4000L } };
        Long[][] boxed = Array.box(primitive);

        Assertions.assertEquals(2, boxed.length);
        Assertions.assertArrayEquals(new Long[] { 1000L, 2000L }, boxed[0]);
        Assertions.assertArrayEquals(new Long[] { 3000L, 4000L }, boxed[1]);
    }

    @Test
    public void testBox_float2D() {
        float[][] primitive = { { 1.1f, 2.2f }, { 3.3f, 4.4f } };
        Float[][] boxed = Array.box(primitive);

        Assertions.assertEquals(2, boxed.length);
        Assertions.assertArrayEquals(new Float[] { 1.1f, 2.2f }, boxed[0]);
        Assertions.assertArrayEquals(new Float[] { 3.3f, 4.4f }, boxed[1]);
    }

    @Test
    public void testBox_double2D() {
        double[][] primitive = { { 1.11, 2.22 }, { 3.33, 4.44 } };
        Double[][] boxed = Array.box(primitive);

        Assertions.assertEquals(2, boxed.length);
        Assertions.assertArrayEquals(new Double[] { 1.11, 2.22 }, boxed[0]);
        Assertions.assertArrayEquals(new Double[] { 3.33, 4.44 }, boxed[1]);
    }

    @Test
    public void testBox_boolean3D() {
        boolean[][][] primitive = { { { true, false } }, { { false, true } } };
        Boolean[][][] boxed = Array.box(primitive);

        Assertions.assertEquals(2, boxed.length);
        Assertions.assertEquals(1, boxed[0].length);
        Assertions.assertArrayEquals(new Boolean[] { true, false }, boxed[0][0]);

        Assertions.assertNull(Array.box((boolean[][][]) null));
    }

    @Test
    public void testBox_char3D() {
        char[][][] primitive = { { { 'a', 'b' } }, { { 'c', 'd' } } };
        Character[][][] boxed = Array.box(primitive);

        Assertions.assertEquals(2, boxed.length);
        Assertions.assertEquals(1, boxed[0].length);
        Assertions.assertArrayEquals(new Character[] { 'a', 'b' }, boxed[0][0]);
    }

    @Test
    public void testNewInstance_SingleDimension_NegativeSize() {
        assertThrows(NegativeArraySizeException.class, () -> Array.newInstance(String.class, -1));
    }

    @Test
    public void testNewInstance_MultiDimension_NegativeSize() {
        assertThrows(NegativeArraySizeException.class, () -> Array.newInstance(String.class, 3, -1));
    }

    @Test
    public void testGetLength_NonArray() {
        assertThrows(IllegalArgumentException.class, () -> Array.getLength("not an array"));
    }

    @Test
    public void testOf_Boolean() {
        boolean[] array = Array.of(true, false, true);
        assertArrayEquals(new boolean[] { true, false, true }, array);
    }

    @Test
    public void testOf_Char() {
        char[] array = Array.of('a', 'b', 'c');
        assertArrayEquals(new char[] { 'a', 'b', 'c' }, array);
    }

    @Test
    public void testOf_Byte() {
        byte[] array = Array.of((byte) 1, (byte) 2, (byte) 3);
        assertArrayEquals(new byte[] { 1, 2, 3 }, array);
    }

    @Test
    public void testOf_Short() {
        short[] array = Array.of((short) 10, (short) 20, (short) 30);
        assertArrayEquals(new short[] { 10, 20, 30 }, array);
    }

    @Test
    public void testOf_Int() {
        int[] array = Array.of(100, 200, 300);
        assertArrayEquals(new int[] { 100, 200, 300 }, array);
    }

    @Test
    public void testOf_Long() {
        long[] array = Array.of(1000L, 2000L, 3000L);
        assertArrayEquals(new long[] { 1000L, 2000L, 3000L }, array);
    }

    @Test
    public void testOf_Float() {
        float[] array = Array.of(1.1f, 2.2f, 3.3f);
        assertArrayEquals(new float[] { 1.1f, 2.2f, 3.3f }, array, 0.001f);
    }

    @Test
    public void testOf_Double() {
        double[] array = Array.of(1.11, 2.22, 3.33);
        assertArrayEquals(new double[] { 1.11, 2.22, 3.33 }, array, 0.001);
    }

    @Test
    public void testRange_Char() {
        char[] range = Array.range('a', 'e');
        assertArrayEquals(new char[] { 'a', 'b', 'c', 'd' }, range);

        char[] emptyRange = Array.range('a', 'a');
        assertEquals(0, emptyRange.length);
    }

    @Test
    public void testRange_Byte() {
        byte[] range = Array.range((byte) 1, (byte) 5);
        assertArrayEquals(new byte[] { 1, 2, 3, 4 }, range);
    }

    @Test
    public void testRange_Short() {
        short[] range = Array.range((short) 10, (short) 15);
        assertArrayEquals(new short[] { 10, 11, 12, 13, 14 }, range);
    }

    @Test
    public void testRange_Int() {
        int[] range = Array.range(100, 105);
        assertArrayEquals(new int[] { 100, 101, 102, 103, 104 }, range);
    }

    @Test
    public void testRange_Long() {
        long[] range = Array.range(1000L, 1005L);
        assertArrayEquals(new long[] { 1000L, 1001L, 1002L, 1003L, 1004L }, range);
    }

    @Test
    public void testRange_CharWithStep() {
        char[] range = Array.range('a', 'g', 2);
        assertArrayEquals(new char[] { 'a', 'c', 'e' }, range);

        char[] reverseRange = Array.range('e', 'a', -1);
        assertArrayEquals(new char[] { 'e', 'd', 'c', 'b' }, reverseRange);
    }

    @Test
    public void testRange_CharWithStep_ZeroStep() {
        assertThrows(IllegalArgumentException.class, () -> Array.range('a', 'e', 0));
    }

    @Test
    public void testRange_ByteWithStep() {
        byte[] range = Array.range((byte) 1, (byte) 10, (byte) 3);
        assertArrayEquals(new byte[] { 1, 4, 7 }, range);
    }

    @Test
    public void testRange_ShortWithStep() {
        short[] range = Array.range((short) 10, (short) 20, (short) 3);
        assertArrayEquals(new short[] { 10, 13, 16, 19 }, range);
    }

    @Test
    public void testRange_IntWithStep() {
        int[] range = Array.range(100, 110, 2);
        assertArrayEquals(new int[] { 100, 102, 104, 106, 108 }, range);
    }

    @Test
    public void testRange_LongWithStep() {
        long[] range = Array.range(1000L, 1010L, 3L);
        assertArrayEquals(new long[] { 1000L, 1003L, 1006L, 1009L }, range);
    }

    @Test
    public void testRangeClosed_Char() {
        char[] range = Array.rangeClosed('a', 'd');
        assertArrayEquals(new char[] { 'a', 'b', 'c', 'd' }, range);

        char[] singleElement = Array.rangeClosed('x', 'x');
        assertArrayEquals(new char[] { 'x' }, singleElement);
    }

    @Test
    public void testRangeClosed_Byte() {
        byte[] range = Array.rangeClosed((byte) 1, (byte) 4);
        assertArrayEquals(new byte[] { 1, 2, 3, 4 }, range);
    }

    @Test
    public void testRangeClosed_Short() {
        short[] range = Array.rangeClosed((short) 10, (short) 13);
        assertArrayEquals(new short[] { 10, 11, 12, 13 }, range);
    }

    @Test
    public void testRangeClosed_Int() {
        int[] range = Array.rangeClosed(100, 103);
        assertArrayEquals(new int[] { 100, 101, 102, 103 }, range);
    }

    @Test
    public void testRangeClosed_Long() {
        long[] range = Array.rangeClosed(1000L, 1003L);
        assertArrayEquals(new long[] { 1000L, 1001L, 1002L, 1003L }, range);
    }

    @Test
    public void testRangeClosed_CharWithStep() {
        char[] range = Array.rangeClosed('a', 'g', 2);
        assertArrayEquals(new char[] { 'a', 'c', 'e', 'g' }, range);
    }

    @Test
    public void testRangeClosed_ByteWithStep() {
        byte[] range = Array.rangeClosed((byte) 1, (byte) 10, (byte) 3);
        assertArrayEquals(new byte[] { 1, 4, 7, 10 }, range);
    }

    @Test
    public void testRangeClosed_ShortWithStep() {
        short[] range = Array.rangeClosed((short) 10, (short) 20, (short) 5);
        assertArrayEquals(new short[] { 10, 15, 20 }, range);
    }

    @Test
    public void testRangeClosed_IntWithStep() {
        int[] range = Array.rangeClosed(100, 110, 5);
        assertArrayEquals(new int[] { 100, 105, 110 }, range);
    }

    @Test
    public void testRangeClosed_LongWithStep() {
        long[] range = Array.rangeClosed(1000L, 1009L, 3L);
        assertArrayEquals(new long[] { 1000L, 1003L, 1006L, 1009L }, range);
    }

    @Test
    public void testRepeat_Boolean() {
        boolean[] array = Array.repeat(true, 3);
        assertArrayEquals(new boolean[] { true, true, true }, array);
    }

    @Test
    public void testRepeat_Short() {
        short[] array = Array.repeat((short) 10, 3);
        assertArrayEquals(new short[] { 10, 10, 10 }, array);
    }

    @Test
    public void testRepeat_Long() {
        long[] array = Array.repeat(1000L, 3);
        assertArrayEquals(new long[] { 1000L, 1000L, 1000L }, array);
    }

    @Test
    public void testRepeat_Float() {
        float[] array = Array.repeat(1.5f, 3);
        assertArrayEquals(new float[] { 1.5f, 1.5f, 1.5f }, array, 0.001f);
    }

    @Test
    public void testRepeat_Double() {
        double[] array = Array.repeat(2.5, 3);
        assertArrayEquals(new double[] { 2.5, 2.5, 2.5 }, array, 0.001);
    }

    @Test
    public void testRepeat_ObjectWithClass() {
        String[] array = Array.repeat("hello", 3, String.class);
        assertArrayEquals(new String[] { "hello", "hello", "hello" }, array);
    }

    @Test
    public void testRepeatNonNull_NullElement() {
        assertThrows(IllegalArgumentException.class, () -> Array.repeatNonNull(null, 3));
    }

    @Test
    public void testRandom_WithRange() {
        int[] array = Array.random(0, 10, 100);
        assertNotNull(array);
        assertEquals(100, array.length);

        for (int value : array) {
            assertTrue(value >= 0 && value < 10);
        }
    }

    @Test
    public void testRandom_InvalidRange() {
        assertThrows(IllegalArgumentException.class, () -> Array.random(10, 5, 10));
    }

    @Test
    public void testConcat_Boolean2D() {
        boolean[][] a = { { true, false }, { false, true } };
        boolean[][] b = { { true, true }, { false, false } };
        boolean[][] result = Array.concat(a, b);

        assertEquals(2, result.length);
        assertArrayEquals(new boolean[] { true, false, true, true }, result[0]);
        assertArrayEquals(new boolean[] { false, true, false, false }, result[1]);
    }

    @Test
    public void testConcat_Float2D() {
        float[][] a = { { 1.1f, 2.2f }, { 3.3f, 4.4f } };
        float[][] b = { { 5.5f, 6.6f }, { 7.7f, 8.8f } };
        float[][] result = Array.concat(a, b);

        assertEquals(2, result.length);
        assertArrayEquals(new float[] { 1.1f, 2.2f, 5.5f, 6.6f }, result[0], 0.001f);
        assertArrayEquals(new float[] { 3.3f, 4.4f, 7.7f, 8.8f }, result[1], 0.001f);
    }

    @Test
    public void testConcat_Double2D() {
        double[][] a = { { 1.11, 2.22 }, { 3.33, 4.44 } };
        double[][] b = { { 5.55, 6.66 }, { 7.77, 8.88 } };
        double[][] result = Array.concat(a, b);

        assertEquals(2, result.length);
        assertArrayEquals(new double[] { 1.11, 2.22, 5.55, 6.66 }, result[0], 0.001);
        assertArrayEquals(new double[] { 3.33, 4.44, 7.77, 8.88 }, result[1], 0.001);
    }

    @Test
    public void testConcatt_Generic2D() {
        String[][] a = { { "a", "b" }, { "c", "d" } };
        String[][] b = { { "e", "f" }, { "g", "h" } };
        String[][] result = Array.concat2D(a, b);

        assertEquals(2, result.length);
        assertArrayEquals(new String[] { "a", "b", "e", "f" }, result[0]);
        assertArrayEquals(new String[] { "c", "d", "g", "h" }, result[1]);
    }

    @Test
    public void testConcat_Boolean3D() {
        boolean[][][] a = { { { true, false } }, { { false, true } } };
        boolean[][][] b = { { { true, true } }, { { false, false } } };
        boolean[][][] result = Array.concat(a, b);

        N.println(a);
        N.println(b);
        N.println(result);

        assertEquals(2, result.length);
        assertEquals(1, result[0].length);
        assertArrayEquals(new boolean[] { true, false, true, true }, result[0][0]);
    }

    @Test
    public void testBox_Boolean() {
        boolean[] primitives = { true, false, true };
        Boolean[] boxed = Array.box(primitives);
        assertArrayEquals(new Boolean[] { true, false, true }, boxed);

        assertNull(Array.box((boolean[]) null));
    }

    @Test
    public void testBox_BooleanWithRange() {
        boolean[] primitives = { true, false, true, false, true };
        Boolean[] boxed = Array.box(primitives, 1, 4);
        assertArrayEquals(new Boolean[] { false, true, false }, boxed);
    }

    @Test
    public void testBox_Char() {
        char[] primitives = { 'a', 'b', 'c' };
        Character[] boxed = Array.box(primitives);
        assertArrayEquals(new Character[] { 'a', 'b', 'c' }, boxed);
    }

    @Test
    public void testBox_Byte() {
        byte[] primitives = { 1, 2, 3 };
        Byte[] boxed = Array.box(primitives);
        assertArrayEquals(new Byte[] { 1, 2, 3 }, boxed);
    }

    @Test
    public void testBox_Short() {
        short[] primitives = { 10, 20, 30 };
        Short[] boxed = Array.box(primitives);
        assertArrayEquals(new Short[] { 10, 20, 30 }, boxed);
    }

    @Test
    public void testBox_Int() {
        int[] primitives = { 100, 200, 300 };
        Integer[] boxed = Array.box(primitives);
        assertArrayEquals(new Integer[] { 100, 200, 300 }, boxed);
    }

    @Test
    public void testBox_Long() {
        long[] primitives = { 1000L, 2000L, 3000L };
        Long[] boxed = Array.box(primitives);
        assertArrayEquals(new Long[] { 1000L, 2000L, 3000L }, boxed);
    }

    @Test
    public void testBox_Float() {
        float[] primitives = { 1.1f, 2.2f, 3.3f };
        Float[] boxed = Array.box(primitives);
        assertArrayEquals(new Float[] { 1.1f, 2.2f, 3.3f }, boxed);
    }

    @Test
    public void testBox_Double() {
        double[] primitives = { 1.11, 2.22, 3.33 };
        Double[] boxed = Array.box(primitives);
        assertArrayEquals(new Double[] { 1.11, 2.22, 3.33 }, boxed);
    }

    @Test
    public void testBox_Boolean2D() {
        boolean[][] primitives = { { true, false }, { false, true } };
        Boolean[][] boxed = Array.box(primitives);

        assertEquals(2, boxed.length);
        assertArrayEquals(new Boolean[] { true, false }, boxed[0]);
        assertArrayEquals(new Boolean[] { false, true }, boxed[1]);
    }

    @Test
    public void testBox_Int2D() {
        int[][] primitives = { { 1, 2 }, { 3, 4 } };
        Integer[][] boxed = Array.box(primitives);

        assertEquals(2, boxed.length);
        assertArrayEquals(new Integer[] { 1, 2 }, boxed[0]);
        assertArrayEquals(new Integer[] { 3, 4 }, boxed[1]);
    }

    @Test
    public void testBox_Boolean3D() {
        boolean[][][] primitives = { { { true, false } }, { { false, true } } };
        Boolean[][][] boxed = Array.box(primitives);

        assertEquals(2, boxed.length);
        assertEquals(1, boxed[0].length);
        assertArrayEquals(new Boolean[] { true, false }, boxed[0][0]);
    }

    @Test
    public void testUnbox_Boolean() {
        Boolean[] boxed = { true, false, null, true };
        boolean[] primitives = Array.unbox(boxed);
        assertArrayEquals(new boolean[] { true, false, false, true }, primitives);
    }

    @Test
    public void testUnbox_BooleanWithDefault() {
        Boolean[] boxed = { true, false, null, true };
        boolean[] primitives = Array.unbox(boxed, true);
        assertArrayEquals(new boolean[] { true, false, true, true }, primitives);
    }

    @Test
    public void testUnbox_BooleanWithRange() {
        Boolean[] boxed = { true, false, null, true, false };
        boolean[] primitives = Array.unbox(boxed, 1, 4, true);
        assertArrayEquals(new boolean[] { false, true, true }, primitives);
    }

    @Test
    public void testUnbox_Character() {
        Character[] boxed = { 'a', 'b', null, 'c' };
        char[] primitives = Array.unbox(boxed);
        assertArrayEquals(new char[] { 'a', 'b', '\0', 'c' }, primitives);
    }

    @Test
    public void testUnbox_CharacterWithDefault() {
        Character[] boxed = { 'a', 'b', null, 'c' };
        char[] primitives = Array.unbox(boxed, 'x');
        assertArrayEquals(new char[] { 'a', 'b', 'x', 'c' }, primitives);
    }

    @Test
    public void testUnbox_Byte() {
        Byte[] boxed = { 1, 2, null, 3 };
        byte[] primitives = Array.unbox(boxed);
        assertArrayEquals(new byte[] { 1, 2, 0, 3 }, primitives);
    }

    @Test
    public void testUnbox_Short() {
        Short[] boxed = { 10, 20, null, 30 };
        short[] primitives = Array.unbox(boxed);
        assertArrayEquals(new short[] { 10, 20, 0, 30 }, primitives);
    }

    @Test
    public void testUnbox_Integer() {
        Integer[] boxed = { 100, 200, null, 300 };
        int[] primitives = Array.unbox(boxed);
        assertArrayEquals(new int[] { 100, 200, 0, 300 }, primitives);
    }

    @Test
    public void testUnbox_Long() {
        Long[] boxed = { 1000L, 2000L, null, 3000L };
        long[] primitives = Array.unbox(boxed);
        assertArrayEquals(new long[] { 1000L, 2000L, 0L, 3000L }, primitives);
    }

    @Test
    public void testUnbox_Float() {
        Float[] boxed = { 1.1f, 2.2f, null, 3.3f };
        float[] primitives = Array.unbox(boxed);
        assertArrayEquals(new float[] { 1.1f, 2.2f, 0f, 3.3f }, primitives, 0.001f);
    }

    @Test
    public void testUnbox_Double() {
        Double[] boxed = { 1.11, 2.22, null, 3.33 };
        double[] primitives = Array.unbox(boxed);
        assertArrayEquals(new double[] { 1.11, 2.22, 0d, 3.33 }, primitives, 0.001);
    }

    @Test
    public void testUnbox_Boolean2D() {
        Boolean[][] boxed = { { true, false, null }, { false, true, null } };
        boolean[][] primitives = Array.unbox(boxed);

        assertEquals(2, primitives.length);
        assertArrayEquals(new boolean[] { true, false, false }, primitives[0]);
        assertArrayEquals(new boolean[] { false, true, false }, primitives[1]);
    }

    @Test
    public void testUnbox_Integer2D() {
        Integer[][] boxed = { { 1, 2, null }, { 3, 4, null } };
        int[][] primitives = Array.unbox(boxed);

        assertEquals(2, primitives.length);
        assertArrayEquals(new int[] { 1, 2, 0 }, primitives[0]);
        assertArrayEquals(new int[] { 3, 4, 0 }, primitives[1]);
    }

    @Test
    public void testUnbox_Boolean3D() {
        Boolean[][][] boxed = { { { true, false, null } }, { { false, true, null } } };
        boolean[][][] primitives = Array.unbox(boxed);

        assertEquals(2, primitives.length);
        assertEquals(1, primitives[0].length);
        assertArrayEquals(new boolean[] { true, false, false }, primitives[0][0]);
    }

    @Test
    public void testTranspose_Boolean() {
        boolean[][] matrix = { { true, false, true }, { false, true, false } };
        boolean[][] transposed = Array.transpose(matrix);

        assertEquals(3, transposed.length);
        assertEquals(2, transposed[0].length);
        assertArrayEquals(new boolean[] { true, false }, transposed[0]);
        assertArrayEquals(new boolean[] { false, true }, transposed[1]);
        assertArrayEquals(new boolean[] { true, false }, transposed[2]);
    }

    @Test
    public void testTranspose_Char() {
        char[][] matrix = { { 'a', 'b', 'c' }, { 'd', 'e', 'f' } };
        char[][] transposed = Array.transpose(matrix);

        assertEquals(3, transposed.length);
        assertArrayEquals(new char[] { 'a', 'd' }, transposed[0]);
        assertArrayEquals(new char[] { 'b', 'e' }, transposed[1]);
        assertArrayEquals(new char[] { 'c', 'f' }, transposed[2]);
    }

    @Test
    public void testTranspose_Byte() {
        byte[][] matrix = { { 1, 2, 3 }, { 4, 5, 6 } };
        byte[][] transposed = Array.transpose(matrix);

        assertEquals(3, transposed.length);
        assertArrayEquals(new byte[] { 1, 4 }, transposed[0]);
        assertArrayEquals(new byte[] { 2, 5 }, transposed[1]);
        assertArrayEquals(new byte[] { 3, 6 }, transposed[2]);
    }

    @Test
    public void testTranspose_Short() {
        short[][] matrix = { { 10, 20, 30 }, { 40, 50, 60 } };
        short[][] transposed = Array.transpose(matrix);

        assertEquals(3, transposed.length);
        assertArrayEquals(new short[] { 10, 40 }, transposed[0]);
        assertArrayEquals(new short[] { 20, 50 }, transposed[1]);
        assertArrayEquals(new short[] { 30, 60 }, transposed[2]);
    }

    @Test
    public void testTranspose_Int() {
        int[][] matrix = { { 1, 2, 3 }, { 4, 5, 6 } };
        int[][] transposed = Array.transpose(matrix);

        assertEquals(3, transposed.length);
        assertArrayEquals(new int[] { 1, 4 }, transposed[0]);
        assertArrayEquals(new int[] { 2, 5 }, transposed[1]);
        assertArrayEquals(new int[] { 3, 6 }, transposed[2]);
    }

    @Test
    public void testTranspose_Long() {
        long[][] matrix = { { 100L, 200L, 300L }, { 400L, 500L, 600L } };
        long[][] transposed = Array.transpose(matrix);

        assertEquals(3, transposed.length);
        assertArrayEquals(new long[] { 100L, 400L }, transposed[0]);
        assertArrayEquals(new long[] { 200L, 500L }, transposed[1]);
        assertArrayEquals(new long[] { 300L, 600L }, transposed[2]);
    }

    @Test
    public void testTranspose_Float() {
        float[][] matrix = { { 1.1f, 2.2f, 3.3f }, { 4.4f, 5.5f, 6.6f } };
        float[][] transposed = Array.transpose(matrix);

        assertEquals(3, transposed.length);
        assertArrayEquals(new float[] { 1.1f, 4.4f }, transposed[0], 0.001f);
        assertArrayEquals(new float[] { 2.2f, 5.5f }, transposed[1], 0.001f);
        assertArrayEquals(new float[] { 3.3f, 6.6f }, transposed[2], 0.001f);
    }

    @Test
    public void testTranspose_Double() {
        double[][] matrix = { { 1.11, 2.22, 3.33 }, { 4.44, 5.55, 6.66 } };
        double[][] transposed = Array.transpose(matrix);

        assertEquals(3, transposed.length);
        assertArrayEquals(new double[] { 1.11, 4.44 }, transposed[0], 0.001);
        assertArrayEquals(new double[] { 2.22, 5.55 }, transposed[1], 0.001);
        assertArrayEquals(new double[] { 3.33, 6.66 }, transposed[2], 0.001);
    }

    @Test
    public void testTranspose_Generic() {
        String[][] matrix = { { "a", "b", "c" }, { "d", "e", "f" } };
        String[][] transposed = Array.transpose(matrix);

        assertEquals(3, transposed.length);
        assertArrayEquals(new String[] { "a", "d" }, transposed[0]);
        assertArrayEquals(new String[] { "b", "e" }, transposed[1]);
        assertArrayEquals(new String[] { "c", "f" }, transposed[2]);
    }

    @Test
    public void testTranspose_NullInput() {
        assertNull(Array.transpose((int[][]) null));
    }

    @Test
    public void testTranspose_EmptyArray() {
        int[][] empty = new int[0][];
        int[][] transposed = Array.transpose(empty);
        assertNotNull(transposed);
        assertEquals(0, transposed.length);
    }

    @Test
    public void testTranspose_JaggedArray() {
        int[][] jagged = { { 1, 2, 3 }, { 4, 5 } };
        assertThrows(IllegalArgumentException.class, () -> Array.transpose(jagged));
    }

    @Test
    public void testEdgeCases_NullInputs() {
        assertNull(Array.box((boolean[]) null));
        assertNull(Array.box((char[]) null));
        assertNull(Array.box((byte[]) null));
        assertNull(Array.box((short[]) null));
        assertNull(Array.box((int[]) null));
        assertNull(Array.box((long[]) null));
        assertNull(Array.box((float[]) null));
        assertNull(Array.box((double[]) null));

        assertNull(Array.unbox((Boolean[]) null));
        assertNull(Array.unbox((Character[]) null));
        assertNull(Array.unbox((Byte[]) null));
        assertNull(Array.unbox((Short[]) null));
        assertNull(Array.unbox((Integer[]) null));
        assertNull(Array.unbox((Long[]) null));
        assertNull(Array.unbox((Float[]) null));
        assertNull(Array.unbox((Double[]) null));
    }

    @Test
    public void testEdgeCases_EmptyArrays() {
        assertEquals(0, Array.box(new boolean[0]).length);
        assertEquals(0, Array.box(new char[0]).length);
        assertEquals(0, Array.box(new byte[0]).length);
        assertEquals(0, Array.box(new short[0]).length);
        assertEquals(0, Array.box(new int[0]).length);
        assertEquals(0, Array.box(new long[0]).length);
        assertEquals(0, Array.box(new float[0]).length);
        assertEquals(0, Array.box(new double[0]).length);

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
    public void testWith() {
        String[] strs = Array.with("a", "b", "c");
        assertArrayEquals(new String[] { "a", "b", "c" }, strs);

        Integer[] ints = Array.with(1, 2, 3);
        assertArrayEquals(new Integer[] { 1, 2, 3 }, ints);

        Object[] empty = Array.with();
        assertEquals(0, empty.length);

        String[] single = Array.with("only");
        assertArrayEquals(new String[] { "only" }, single);
    }

    @Test
    public void testConcat2D() {
        String[][] a = { { "a", "b" }, { "c" } };
        String[][] b = { { "d" } };
        String[][] result = Array.concat2D(a, b);
        assertNotNull(result);
        assertEquals(2, result.length);
        assertArrayEquals(new String[] { "a", "b", "d" }, result[0]);
        assertArrayEquals(new String[] { "c" }, result[1]);

        Integer[][] intA = { { 1, 2 } };
        Integer[][] intB = { { 3, 4 } };
        Integer[][] intResult = Array.concat2D(intA, intB);
        assertEquals(1, intResult.length);
        assertArrayEquals(new Integer[] { 1, 2, 3, 4 }, intResult[0]);

        assertNull(Array.concat2D((String[][]) null, (String[][]) null));

        String[][] nonNull = { { "x" } };
        String[][] fromNull = Array.concat2D(null, nonNull);
        assertNotNull(fromNull);
        assertEquals(1, fromNull.length);
    }

    @Test
    public void testConcat3D() {
        String[][][] a = { { { "a" } } };
        String[][][] b = { { { "b" } } };
        String[][][] result = Array.concat3D(a, b);
        assertNotNull(result);
        assertEquals(1, result.length);
        assertEquals(1, result[0].length);
        assertArrayEquals(new String[] { "a", "b" }, result[0][0]);

        assertNull(Array.concat3D((String[][][]) null, (String[][][]) null));

        String[][][] nonNull = { { { "x" } } };
        String[][][] fromNull = Array.concat3D(null, nonNull);
        assertNotNull(fromNull);
        assertEquals(1, fromNull.length);
    }

    @Test
    public void testArrayUtil_CannotInstantiate() {
        assertTrue(java.lang.reflect.Modifier.isFinal(Array.ArrayUtil.class.getModifiers()));
    }

    @Test
    public void test_asList_EmptyArray() {
        List<String> list = Array.asList();
        assertNotNull(list);
        assertEquals(0, list.size());
    }

    @Test
    public void test_asList_NullArray() {
        List<String> list = Array.asList((String[]) null);
        assertNotNull(list);
        assertEquals(0, list.size());
    }

    @Test
    public void test_asList_SingleElement() {
        List<String> list = Array.asList("hello");
        assertEquals(1, list.size());
        assertEquals("hello", list.get(0));
    }

    @Test
    public void test_asList_MultipleElements() {
        List<Integer> list = Array.asList(1, 2, 3, 4, 5);
        assertEquals(5, list.size());
        assertEquals(1, list.get(0));
        assertEquals(5, list.get(4));
    }

    @Test
    public void test_range_int_empty() {
        int[] arr = Array.range(5, 5);
        assertNotNull(arr);
        assertEquals(0, arr.length);
    }

    @Test
    public void test_rangeClosed_int_basic() {
        int[] arr = Array.rangeClosed(1, 5);
        assertNotNull(arr);
        assertEquals(5, arr.length);
        assertArrayEquals(new int[] { 1, 2, 3, 4, 5 }, arr);
    }

    @Test
    public void test_repeat_int_basic() {
        int[] arr = Array.repeat(7, 3);
        assertNotNull(arr);
        assertEquals(3, arr.length);
        assertArrayEquals(new int[] { 7, 7, 7 }, arr);
    }

    @Test
    public void test_repeat_int_zero() {
        int[] arr = Array.repeat(7, 0);
        assertNotNull(arr);
        assertEquals(0, arr.length);
    }

    @Test
    public void test_repeat_booleanArray() {
        boolean[] arr = Array.repeat(new boolean[] { true, false }, 3);
        assertNotNull(arr);
        assertEquals(6, arr.length);
        assertTrue(arr[0]);
        assertFalse(arr[1]);
        assertTrue(arr[2]);
        assertFalse(arr[3]);
    }

    @Test
    public void test_concat2D_bothNull() {
        assertNull(Array.concat2D((String[][]) null, (String[][]) null));
    }

    @Test
    public void test_concat2D_oneNull() {
        String[][] a = { { "x" } };
        String[][] result = Array.concat2D(a, null);
        assertNotNull(result);
        assertEquals(1, result.length);
    }

    @Test
    public void test_concat2D_basic() {
        String[][] a = { { "a", "b" } };
        String[][] b = { { "c", "d" } };
        String[][] result = Array.concat2D(a, b);
        assertEquals(1, result.length);
        assertArrayEquals(new String[] { "a", "b", "c", "d" }, result[0]);
    }

    @Test
    public void test_newInstance_zeroLength() {
        String[] arr1 = Array.newInstance(String.class, 0);
        String[] arr2 = Array.newInstance(String.class, 0);
        // Zero-length arrays should return the same cached instance
        assertSame(arr1, arr2);
    }

    // ========== Additional tests for untested methods ==========

    @Test
    public void test_range_int_basic() {
        int[] arr = Array.range(1, 6);
        assertArrayEquals(new int[] { 1, 2, 3, 4, 5 }, arr);

        int[] empty = Array.range(5, 5);
        assertEquals(0, empty.length);

        int[] reverse = Array.range(10, 5);
        assertEquals(0, reverse.length);
    }

    @Test
    public void test_range_long_basic() {
        long[] arr = Array.range(1L, 6L);
        assertArrayEquals(new long[] { 1L, 2L, 3L, 4L, 5L }, arr);

        long[] empty = Array.range(5L, 5L);
        assertEquals(0, empty.length);

        long[] reverse = Array.range(10L, 5L);
        assertEquals(0, reverse.length);
    }

    @Test
    public void test_range_int_negativeValues() {
        int[] arr = Array.range(-3, 3);
        assertArrayEquals(new int[] { -3, -2, -1, 0, 1, 2 }, arr);
    }

    @Test
    public void test_range_long_negativeValues() {
        long[] arr = Array.range(-3L, 3L);
        assertArrayEquals(new long[] { -3L, -2L, -1L, 0L, 1L, 2L }, arr);
    }

    @Test
    public void test_unbox_Character_2D_withValueForNull() {
        Character[][] arr = { { 'a', null }, { null, 'd' } };
        char[][] unboxed = Array.unbox(arr, 'x');

        assertEquals(2, unboxed.length);
        assertArrayEquals(new char[] { 'a', 'x' }, unboxed[0]);
        assertArrayEquals(new char[] { 'x', 'd' }, unboxed[1]);
    }

    @Test
    public void test_unbox_Byte_2D_withValueForNull() {
        Byte[][] arr = { { 1, null }, { null, 4 } };
        byte[][] unboxed = Array.unbox(arr, (byte) 0);

        assertEquals(2, unboxed.length);
        assertArrayEquals(new byte[] { 1, 0 }, unboxed[0]);
        assertArrayEquals(new byte[] { 0, 4 }, unboxed[1]);
    }

    @Test
    public void test_unbox_Short_2D_withValueForNull() {
        Short[][] arr = { { 1, null }, { null, 4 } };
        short[][] unboxed = Array.unbox(arr, (short) 0);

        assertEquals(2, unboxed.length);
        assertArrayEquals(new short[] { 1, 0 }, unboxed[0]);
        assertArrayEquals(new short[] { 0, 4 }, unboxed[1]);
    }

    @Test
    public void test_unbox_Integer_2D_withValueForNull() {
        Integer[][] arr = { { 1, null }, { null, 4 } };
        int[][] unboxed = Array.unbox(arr, 0);

        assertEquals(2, unboxed.length);
        assertArrayEquals(new int[] { 1, 0 }, unboxed[0]);
        assertArrayEquals(new int[] { 0, 4 }, unboxed[1]);
    }

    @Test
    public void test_unbox_Long_2D_withValueForNull() {
        Long[][] arr = { { 1L, null }, { null, 4L } };
        long[][] unboxed = Array.unbox(arr, 0L);

        assertEquals(2, unboxed.length);
        assertArrayEquals(new long[] { 1L, 0L }, unboxed[0]);
        assertArrayEquals(new long[] { 0L, 4L }, unboxed[1]);
    }

    @Test
    public void test_unbox_Float_2D_withValueForNull() {
        Float[][] arr = { { 1.1f, null }, { null, 4.4f } };
        float[][] unboxed = Array.unbox(arr, 0f);

        assertEquals(2, unboxed.length);
        assertArrayEquals(new float[] { 1.1f, 0f }, unboxed[0]);
        assertArrayEquals(new float[] { 0f, 4.4f }, unboxed[1]);
    }

    @Test
    public void test_unbox_Double_2D_withValueForNull() {
        Double[][] arr = { { 1.1, null }, { null, 4.4 } };
        double[][] unboxed = Array.unbox(arr, 0.0);

        assertEquals(2, unboxed.length);
        assertArrayEquals(new double[] { 1.1, 0.0 }, unboxed[0]);
        assertArrayEquals(new double[] { 0.0, 4.4 }, unboxed[1]);
    }

    @Test
    public void test_unbox_Boolean_3D_withValueForNull() {
        Boolean[][][] arr = { { { true, null }, { null, false } } };
        boolean[][][] unboxed = Array.unbox(arr, false);

        assertEquals(1, unboxed.length);
        assertEquals(2, unboxed[0].length);
        assertArrayEquals(new boolean[] { true, false }, unboxed[0][0]);
        assertArrayEquals(new boolean[] { false, false }, unboxed[0][1]);
    }

    @Test
    public void test_unbox_Character_3D_withValueForNull() {
        Character[][][] arr = { { { 'a', null } } };
        char[][][] unboxed = Array.unbox(arr, 'x');

        assertEquals(1, unboxed.length);
        assertArrayEquals(new char[] { 'a', 'x' }, unboxed[0][0]);
    }

    @Test
    public void test_unbox_Byte_3D_withValueForNull() {
        Byte[][][] arr = { { { 1, null } } };
        byte[][][] unboxed = Array.unbox(arr, (byte) 0);

        assertEquals(1, unboxed.length);
        assertArrayEquals(new byte[] { 1, 0 }, unboxed[0][0]);
    }

    @Test
    public void test_unbox_Short_3D_withValueForNull() {
        Short[][][] arr = { { { 1, null } } };
        short[][][] unboxed = Array.unbox(arr, (short) 0);

        assertEquals(1, unboxed.length);
        assertArrayEquals(new short[] { 1, 0 }, unboxed[0][0]);
    }

    @Test
    public void test_unbox_Integer_3D_withValueForNull() {
        Integer[][][] arr = { { { 1, null } } };
        int[][][] unboxed = Array.unbox(arr, 0);

        assertEquals(1, unboxed.length);
        assertArrayEquals(new int[] { 1, 0 }, unboxed[0][0]);
    }

    @Test
    public void test_unbox_Long_3D_withValueForNull() {
        Long[][][] arr = { { { 1L, null } } };
        long[][][] unboxed = Array.unbox(arr, 0L);

        assertEquals(1, unboxed.length);
        assertArrayEquals(new long[] { 1L, 0L }, unboxed[0][0]);
    }

    @Test
    public void test_unbox_Float_3D_withValueForNull() {
        Float[][][] arr = { { { 1.1f, null } } };
        float[][][] unboxed = Array.unbox(arr, 0f);

        assertEquals(1, unboxed.length);
        assertArrayEquals(new float[] { 1.1f, 0f }, unboxed[0][0]);
    }

    @Test
    public void test_unbox_Double_3D_withValueForNull() {
        Double[][][] arr = { { { 1.1, null } } };
        double[][][] unboxed = Array.unbox(arr, 0.0);

        assertEquals(1, unboxed.length);
        assertArrayEquals(new double[] { 1.1, 0.0 }, unboxed[0][0]);
    }

    @Test
    public void test_concat_char_2D_nullHandling() {
        char[][] result = Array.concat((char[][]) null, (char[][]) null);
        assertNotNull(result);
        assertEquals(0, result.length);

        char[][] a = { { 'a', 'b' } };
        char[][] result2 = Array.concat(a, null);
        assertNotNull(result2);
        assertEquals(1, result2.length);
    }

    @Test
    public void test_concat_byte_2D_nullHandling() {
        byte[][] result = Array.concat((byte[][]) null, (byte[][]) null);
        assertNotNull(result);
        assertEquals(0, result.length);
    }

    @Test
    public void test_concat_short_2D_nullHandling() {
        short[][] result = Array.concat((short[][]) null, (short[][]) null);
        assertNotNull(result);
        assertEquals(0, result.length);
    }

    @Test
    public void test_concat_long_2D_nullHandling() {
        long[][] result = Array.concat((long[][]) null, (long[][]) null);
        assertNotNull(result);
        assertEquals(0, result.length);
    }

    @Test
    public void test_concat_float_2D_nullHandling() {
        float[][] result = Array.concat((float[][]) null, (float[][]) null);
        assertNotNull(result);
        assertEquals(0, result.length);
    }

    @Test
    public void test_concat_double_2D_nullHandling() {
        double[][] result = Array.concat((double[][]) null, (double[][]) null);
        assertNotNull(result);
        assertEquals(0, result.length);
    }

    @Test
    public void test_concat_char_3D_nullHandling() {
        char[][][] result = Array.concat((char[][][]) null, (char[][][]) null);
        assertNotNull(result);
        assertEquals(0, result.length);
    }

    @Test
    public void test_concat_byte_3D_nullHandling() {
        byte[][][] result = Array.concat((byte[][][]) null, (byte[][][]) null);
        assertNotNull(result);
        assertEquals(0, result.length);
    }

    @Test
    public void test_concat_short_3D_nullHandling() {
        short[][][] result = Array.concat((short[][][]) null, (short[][][]) null);
        assertNotNull(result);
        assertEquals(0, result.length);
    }

    @Test
    public void test_concat_long_3D_nullHandling() {
        long[][][] result = Array.concat((long[][][]) null, (long[][][]) null);
        assertNotNull(result);
        assertEquals(0, result.length);
    }

    @Test
    public void test_concat_float_3D_nullHandling() {
        float[][][] result = Array.concat((float[][][]) null, (float[][][]) null);
        assertNotNull(result);
        assertEquals(0, result.length);
    }

    @Test
    public void test_concat_double_3D_nullHandling() {
        double[][][] result = Array.concat((double[][][]) null, (double[][][]) null);
        assertNotNull(result);
        assertEquals(0, result.length);
    }

    @Test
    public void test_concat3D_nullHandling() {
        String[][][] nullBoth = Array.concat3D((String[][][]) null, (String[][][]) null);
        assertNull(nullBoth);

        String[][][] a = { { { "a" } } };
        String[][][] result = Array.concat3D(a, null);
        assertNotNull(result);
        assertEquals(1, result.length);
    }

    @Test
    public void test_concat3D_differentLengths() {
        String[][][] a = { { { "a" } }, { { "b" } } };
        String[][][] b = { { { "c" } } };
        String[][][] result = Array.concat3D(a, b);
        assertEquals(2, result.length);
    }

    @Test
    public void test_transpose_boolean_null() {
        boolean[][] result = Array.transpose((boolean[][]) null);
        assertNull(result);
    }

    @Test
    public void test_transpose_char_null() {
        char[][] result = Array.transpose((char[][]) null);
        assertNull(result);
    }

    @Test
    public void test_transpose_byte_null() {
        byte[][] result = Array.transpose((byte[][]) null);
        assertNull(result);
    }

    @Test
    public void test_transpose_short_null() {
        short[][] result = Array.transpose((short[][]) null);
        assertNull(result);
    }

    @Test
    public void test_transpose_int_null() {
        int[][] result = Array.transpose((int[][]) null);
        assertNull(result);
    }

    @Test
    public void test_transpose_long_null() {
        long[][] result = Array.transpose((long[][]) null);
        assertNull(result);
    }

    @Test
    public void test_transpose_float_null() {
        float[][] result = Array.transpose((float[][]) null);
        assertNull(result);
    }

    @Test
    public void test_transpose_double_null() {
        double[][] result = Array.transpose((double[][]) null);
        assertNull(result);
    }

    @Test
    public void test_transpose_boolean_empty() {
        boolean[][] result = Array.transpose(new boolean[0][]);
        assertEquals(0, result.length);
    }

    @Test
    public void test_transpose_char_empty() {
        char[][] result = Array.transpose(new char[0][]);
        assertEquals(0, result.length);
    }

    @Test
    public void test_transpose_byte_empty() {
        byte[][] result = Array.transpose(new byte[0][]);
        assertEquals(0, result.length);
    }

    @Test
    public void test_transpose_short_empty() {
        short[][] result = Array.transpose(new short[0][]);
        assertEquals(0, result.length);
    }

    @Test
    public void test_transpose_int_empty() {
        int[][] result = Array.transpose(new int[0][]);
        assertEquals(0, result.length);
    }

    @Test
    public void test_transpose_long_empty() {
        long[][] result = Array.transpose(new long[0][]);
        assertEquals(0, result.length);
    }

    @Test
    public void test_transpose_float_empty() {
        float[][] result = Array.transpose(new float[0][]);
        assertEquals(0, result.length);
    }

    @Test
    public void test_transpose_double_empty() {
        double[][] result = Array.transpose(new double[0][]);
        assertEquals(0, result.length);
    }

    @Test
    public void test_box_allTypes_nullInput() {
        assertNull(Array.box((boolean[]) null));
        assertNull(Array.box((char[]) null));
        assertNull(Array.box((byte[]) null));
        assertNull(Array.box((short[]) null));
        assertNull(Array.box((int[]) null));
        assertNull(Array.box((long[]) null));
        assertNull(Array.box((float[]) null));
        assertNull(Array.box((double[]) null));
    }

    @Test
    public void test_unbox_allTypes_nullInput() {
        assertNull(Array.unbox((Boolean[]) null));
        assertNull(Array.unbox((Character[]) null));
        assertNull(Array.unbox((Byte[]) null));
        assertNull(Array.unbox((Short[]) null));
        assertNull(Array.unbox((Integer[]) null));
        assertNull(Array.unbox((Long[]) null));
        assertNull(Array.unbox((Float[]) null));
        assertNull(Array.unbox((Double[]) null));
    }

    @Test
    public void test_unbox_2D_allTypes_nullInput() {
        assertNull(Array.unbox((Boolean[][]) null));
        assertNull(Array.unbox((Character[][]) null));
        assertNull(Array.unbox((Byte[][]) null));
        assertNull(Array.unbox((Short[][]) null));
        assertNull(Array.unbox((Integer[][]) null));
        assertNull(Array.unbox((Long[][]) null));
        assertNull(Array.unbox((Float[][]) null));
        assertNull(Array.unbox((Double[][]) null));
    }

    @Test
    public void test_unbox_3D_allTypes_nullInput() {
        assertNull(Array.unbox((Boolean[][][]) null));
        assertNull(Array.unbox((Character[][][]) null));
        assertNull(Array.unbox((Byte[][][]) null));
        assertNull(Array.unbox((Short[][][]) null));
        assertNull(Array.unbox((Integer[][][]) null));
        assertNull(Array.unbox((Long[][][]) null));
        assertNull(Array.unbox((Float[][][]) null));
        assertNull(Array.unbox((Double[][][]) null));
    }

    @Test
    public void test_box_2D_allTypes_nullInput() {
        assertNull(Array.box((boolean[][]) null));
        assertNull(Array.box((char[][]) null));
        assertNull(Array.box((byte[][]) null));
        assertNull(Array.box((short[][]) null));
        assertNull(Array.box((int[][]) null));
        assertNull(Array.box((long[][]) null));
        assertNull(Array.box((float[][]) null));
        assertNull(Array.box((double[][]) null));
    }

    @Test
    public void test_box_3D_allTypes_nullInput() {
        assertNull(Array.box((boolean[][][]) null));
        assertNull(Array.box((char[][][]) null));
        assertNull(Array.box((byte[][][]) null));
        assertNull(Array.box((short[][][]) null));
        assertNull(Array.box((int[][][]) null));
        assertNull(Array.box((long[][][]) null));
        assertNull(Array.box((float[][][]) null));
        assertNull(Array.box((double[][][]) null));
    }

    @Test
    public void test_repeat_String_array_nullInput() {
        String[] result = Array.repeat((String[]) null, 3);
        assertNotNull(result);
        assertEquals(0, result.length);
    }

    @Test
    public void test_repeat_char_array_nullInput() {
        char[] result = Array.repeat((char[]) null, 3);
        assertNotNull(result);
        assertEquals(0, result.length);
    }

    @Test
    public void test_repeat_byte_array_nullInput() {
        byte[] result = Array.repeat((byte[]) null, 3);
        assertNotNull(result);
        assertEquals(0, result.length);
    }

    @Test
    public void test_repeat_short_array_nullInput() {
        short[] result = Array.repeat((short[]) null, 3);
        assertNotNull(result);
        assertEquals(0, result.length);
    }

    @Test
    public void test_repeat_long_array_nullInput() {
        long[] result = Array.repeat((long[]) null, 3);
        assertNotNull(result);
        assertEquals(0, result.length);
    }

    @Test
    public void test_repeat_float_array_nullInput() {
        float[] result = Array.repeat((float[]) null, 3);
        assertNotNull(result);
        assertEquals(0, result.length);
    }

    @Test
    public void test_repeat_double_array_nullInput() {
        double[] result = Array.repeat((double[]) null, 3);
        assertNotNull(result);
        assertEquals(0, result.length);
    }

    @Test
    public void test_concat_2D_differentLengths() {
        int[][] a = { { 1, 2 }, { 3, 4 }, { 5, 6 } };
        int[][] b = { { 7, 8 } };
        int[][] result = Array.concat(a, b);

        assertEquals(3, result.length);
        assertArrayEquals(new int[] { 1, 2, 7, 8 }, result[0]);
        assertArrayEquals(new int[] { 3, 4 }, result[1]);
        assertArrayEquals(new int[] { 5, 6 }, result[2]);
    }

    @Test
    public void test_random_withRange_edgeCases() {
        int[] arr = Array.random(0, 1, 10);
        assertEquals(10, arr.length);
        for (int val : arr) {
            assertEquals(0, val);
        }

        assertThrows(IllegalArgumentException.class, () -> Array.random(5, 5, 10));
        assertThrows(IllegalArgumentException.class, () -> Array.random(10, 5, 10));
    }

    @Test
    public void test_transpose_generic_singleElement() {
        String[][] arr = { { "only" } };
        String[][] transposed = Array.transpose(arr);
        assertEquals(1, transposed.length);
        assertEquals(1, transposed[0].length);
        assertEquals("only", transposed[0][0]);
    }

    @Test
    public void test_transpose_generic_empty() {
        String[][] empty = new String[0][];
        String[][] transposed = Array.transpose(empty);
        assertEquals(0, transposed.length);
    }

    @Test
    public void test_boxUnbox_symmetry_allTypes() {
        // char
        char[] charOriginal = { 'a', 'b', 'c' };
        Character[] charBoxed = Array.box(charOriginal);
        char[] charUnboxed = Array.unbox(charBoxed);
        assertArrayEquals(charOriginal, charUnboxed);

        // byte
        byte[] byteOriginal = { 1, 2, 3 };
        Byte[] byteBoxed = Array.box(byteOriginal);
        byte[] byteUnboxed = Array.unbox(byteBoxed);
        assertArrayEquals(byteOriginal, byteUnboxed);

        // short
        short[] shortOriginal = { 10, 20, 30 };
        Short[] shortBoxed = Array.box(shortOriginal);
        short[] shortUnboxed = Array.unbox(shortBoxed);
        assertArrayEquals(shortOriginal, shortUnboxed);

        // long
        long[] longOriginal = { 100L, 200L, 300L };
        Long[] longBoxed = Array.box(longOriginal);
        long[] longUnboxed = Array.unbox(longBoxed);
        assertArrayEquals(longOriginal, longUnboxed);

        // float
        float[] floatOriginal = { 1.1f, 2.2f, 3.3f };
        Float[] floatBoxed = Array.box(floatOriginal);
        float[] floatUnboxed = Array.unbox(floatBoxed);
        assertArrayEquals(floatOriginal, floatUnboxed);
    }

    @Test
    public void testRandom_LargeRange() {
        final int startInclusive = -1_500_000_000;
        final int endExclusive = 1_500_000_000;
        final int[] values = Array.random(startInclusive, endExclusive, 16);

        assertEquals(16, values.length);

        for (final int value : values) {
            assertTrue(value >= startInclusive);
            assertTrue(value < endExclusive);
        }
    }

    @Test
    public void testTranspose_RowsGreaterThanColumns() {
        final boolean[][] booleans = { { true, false }, { false, true }, { true, true } };
        final boolean[][] expectedBooleans = { { true, false, true }, { false, true, true } };
        assertArrayEquals(expectedBooleans, Array.transpose(booleans));

        final int[][] ints = { { 1, 2 }, { 3, 4 }, { 5, 6 } };
        final int[][] expectedInts = { { 1, 3, 5 }, { 2, 4, 6 } };
        assertArrayEquals(expectedInts, Array.transpose(ints));

        final double[][] doubles = { { 1.5d, 2.5d }, { 3.5d, 4.5d }, { 5.5d, 6.5d } };
        final double[][] expectedDoubles = { { 1.5d, 3.5d, 5.5d }, { 2.5d, 4.5d, 6.5d } };
        assertArrayEquals(expectedDoubles, Array.transpose(doubles));

        final Object[][] objects = { { "a", 1 }, { "b", 2 }, { "c", 3 } };
        final Object[][] expectedObjects = { { "a", "b", "c" }, { 1, 2, 3 } };
        assertArrayEquals(expectedObjects, Array.transpose(objects));
    }

}

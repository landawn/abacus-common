package com.landawn.abacus.util;

import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import com.landawn.abacus.TestBase;

@Tag("new-test")
public class Array101Test extends TestBase {

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
    public void testGetLength() {
        Assertions.assertEquals(0, Array.getLength(null));
        Assertions.assertEquals(0, Array.getLength(new int[0]));
        Assertions.assertEquals(5, Array.getLength(new String[5]));
        Assertions.assertEquals(3, Array.getLength(new int[] { 1, 2, 3 }));
    }

    @Test
    public void testGetLength_NotArray() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            Array.getLength("not an array");
        });
    }

    @Test
    public void testGet() {
        Integer[] array = { 10, 20, 30 };
        Assertions.assertEquals(10, (Integer) Array.get(array, 0));
        Assertions.assertEquals(20, (Integer) Array.get(array, 1));
        Assertions.assertEquals(30, (Integer) Array.get(array, 2));

        int[] intArray = { 1, 2, 3 };
        Assertions.assertEquals(1, (Integer) Array.get(intArray, 0));
        Assertions.assertEquals(3, (Integer) Array.get(intArray, 2));
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
    public void testGetBoolean() {
        boolean[] array = { true, false, true };
        Assertions.assertTrue(Array.getBoolean(array, 0));
        Assertions.assertFalse(Array.getBoolean(array, 1));
        Assertions.assertTrue(Array.getBoolean(array, 2));
    }

    @Test
    public void testGetByte() {
        byte[] array = { 1, 2, 3 };
        Assertions.assertEquals((byte) 1, Array.getByte(array, 0));
        Assertions.assertEquals((byte) 2, Array.getByte(array, 1));
        Assertions.assertEquals((byte) 3, Array.getByte(array, 2));
    }

    @Test
    public void testGetChar() {
        char[] array = { 'a', 'b', 'c' };
        Assertions.assertEquals('a', Array.getChar(array, 0));
        Assertions.assertEquals('b', Array.getChar(array, 1));
        Assertions.assertEquals('c', Array.getChar(array, 2));
    }

    @Test
    public void testGetShort() {
        short[] array = { 10, 20, 30 };
        Assertions.assertEquals((short) 10, Array.getShort(array, 0));
        Assertions.assertEquals((short) 20, Array.getShort(array, 1));
        Assertions.assertEquals((short) 30, Array.getShort(array, 2));
    }

    @Test
    public void testGetInt() {
        int[] array = { 100, 200, 300 };
        Assertions.assertEquals(100, Array.getInt(array, 0));
        Assertions.assertEquals(200, Array.getInt(array, 1));
        Assertions.assertEquals(300, Array.getInt(array, 2));
    }

    @Test
    public void testGetLong() {
        long[] array = { 1000L, 2000L, 3000L };
        Assertions.assertEquals(1000L, Array.getLong(array, 0));
        Assertions.assertEquals(2000L, Array.getLong(array, 1));
        Assertions.assertEquals(3000L, Array.getLong(array, 2));
    }

    @Test
    public void testGetFloat() {
        float[] array = { 1.1f, 2.2f, 3.3f };
        Assertions.assertEquals(1.1f, Array.getFloat(array, 0));
        Assertions.assertEquals(2.2f, Array.getFloat(array, 1));
        Assertions.assertEquals(3.3f, Array.getFloat(array, 2));
    }

    @Test
    public void testGetDouble() {
        double[] array = { 1.11, 2.22, 3.33 };
        Assertions.assertEquals(1.11, Array.getDouble(array, 0));
        Assertions.assertEquals(2.22, Array.getDouble(array, 1));
        Assertions.assertEquals(3.33, Array.getDouble(array, 2));
    }

    @Test
    public void testSet() {
        Integer[] array = { 10, 20, 30 };
        Array.set(array, 1, 25);
        Assertions.assertEquals(25, array[1]);

        int[] intArray = { 1, 2, 3 };
        Array.set(intArray, 0, 5);
        Assertions.assertEquals(5, intArray[0]);
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
    public void testSetBoolean() {
        boolean[] array = { true, false, true };
        Array.setBoolean(array, 1, true);
        Assertions.assertTrue(array[1]);
    }

    @Test
    public void testSetByte() {
        byte[] array = { 1, 2, 3 };
        Array.setByte(array, 0, (byte) 5);
        Assertions.assertEquals((byte) 5, array[0]);
    }

    @Test
    public void testSetChar() {
        char[] array = { 'a', 'b', 'c' };
        Array.setChar(array, 2, 'z');
        Assertions.assertEquals('z', array[2]);
    }

    @Test
    public void testSetShort() {
        short[] array = { 10, 20, 30 };
        Array.setShort(array, 1, (short) 25);
        Assertions.assertEquals((short) 25, array[1]);
    }

    @Test
    public void testSetInt() {
        int[] array = { 100, 200, 300 };
        Array.setInt(array, 0, 150);
        Assertions.assertEquals(150, array[0]);
    }

    @Test
    public void testSetLong() {
        long[] array = { 1000L, 2000L, 3000L };
        Array.setLong(array, 2, 3500L);
        Assertions.assertEquals(3500L, array[2]);
    }

    @Test
    public void testSetFloat() {
        float[] array = { 1.1f, 2.2f, 3.3f };
        Array.setFloat(array, 1, 2.5f);
        Assertions.assertEquals(2.5f, array[1]);
    }

    @Test
    public void testSetDouble() {
        double[] array = { 1.11, 2.22, 3.33 };
        Array.setDouble(array, 0, 1.55);
        Assertions.assertEquals(1.55, array[0]);
    }

    @Test
    public void testAsList() {
        List<String> emptyList = Array.asList((String[]) null);
        Assertions.assertNotNull(emptyList);
        Assertions.assertTrue(emptyList.isEmpty());

        List<Integer> emptyList2 = Array.asList(new Integer[0]);
        Assertions.assertNotNull(emptyList2);
        Assertions.assertTrue(emptyList2.isEmpty());

        List<String> list = Array.asList("a", "b", "c");
        Assertions.assertEquals(3, list.size());
        Assertions.assertEquals("a", list.get(0));
        Assertions.assertEquals("b", list.get(1));
        Assertions.assertEquals("c", list.get(2));
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
    public void testOF_deprecated() {
        String[] array = Array.oF("x", "y", "z");
        Assertions.assertEquals(3, array.length);
        Assertions.assertEquals("x", array[0]);
        Assertions.assertEquals("y", array[1]);
        Assertions.assertEquals("z", array[2]);
    }

    @Test
    public void testRange_char() {
        char[] array = Array.range('a', 'd');
        Assertions.assertArrayEquals(new char[] { 'a', 'b', 'c' }, array);

        char[] empty = Array.range('d', 'd');
        Assertions.assertEquals(0, empty.length);

        char[] inverted = Array.range('d', 'a');
        Assertions.assertEquals(0, inverted.length);
    }

    @Test
    public void testRange_byte() {
        byte[] array = Array.range((byte) 1, (byte) 4);
        Assertions.assertArrayEquals(new byte[] { 1, 2, 3 }, array);

        byte[] empty = Array.range((byte) 5, (byte) 5);
        Assertions.assertEquals(0, empty.length);

        byte[] inverted = Array.range((byte) 5, (byte) 2);
        Assertions.assertEquals(0, inverted.length);
    }

    @Test
    public void testRange_short() {
        short[] array = Array.range((short) 10, (short) 13);
        Assertions.assertArrayEquals(new short[] { 10, 11, 12 }, array);

        short[] empty = Array.range((short) 10, (short) 10);
        Assertions.assertEquals(0, empty.length);
    }

    @Test
    public void testRange_int() {
        int[] array = Array.range(100, 103);
        Assertions.assertArrayEquals(new int[] { 100, 101, 102 }, array);

        int[] empty = Array.range(100, 100);
        Assertions.assertEquals(0, empty.length);
    }

    @Test
    public void testRange_int_overflow() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            Array.range(Integer.MIN_VALUE, Integer.MAX_VALUE);
        });
    }

    @Test
    public void testRange_long() {
        long[] array = Array.range(1000L, 1003L);
        Assertions.assertArrayEquals(new long[] { 1000L, 1001L, 1002L }, array);

        long[] empty = Array.range(1000L, 1000L);
        Assertions.assertEquals(0, empty.length);
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
    public void testRangeClosed_char() {
        char[] array = Array.rangeClosed('a', 'c');
        Assertions.assertArrayEquals(new char[] { 'a', 'b', 'c' }, array);

        char[] single = Array.rangeClosed('x', 'x');
        Assertions.assertArrayEquals(new char[] { 'x' }, single);

        char[] inverted = Array.rangeClosed('d', 'a');
        Assertions.assertEquals(0, inverted.length);
    }

    @Test
    public void testRangeClosed_byte() {
        byte[] array = Array.rangeClosed((byte) 1, (byte) 3);
        Assertions.assertArrayEquals(new byte[] { 1, 2, 3 }, array);

        byte[] single = Array.rangeClosed((byte) 5, (byte) 5);
        Assertions.assertArrayEquals(new byte[] { 5 }, single);
    }

    @Test
    public void testRangeClosed_short() {
        short[] array = Array.rangeClosed((short) 10, (short) 12);
        Assertions.assertArrayEquals(new short[] { 10, 11, 12 }, array);

        short[] single = Array.rangeClosed((short) 20, (short) 20);
        Assertions.assertArrayEquals(new short[] { 20 }, single);
    }

    @Test
    public void testRangeClosed_int() {
        int[] array = Array.rangeClosed(100, 102);
        Assertions.assertArrayEquals(new int[] { 100, 101, 102 }, array);

        int[] single = Array.rangeClosed(50, 50);
        Assertions.assertArrayEquals(new int[] { 50 }, single);
    }

    @Test
    public void testRangeClosed_int_overflow() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            Array.rangeClosed(Integer.MIN_VALUE, Integer.MAX_VALUE);
        });
    }

    @Test
    public void testRangeClosed_long() {
        long[] array = Array.rangeClosed(1000L, 1002L);
        Assertions.assertArrayEquals(new long[] { 1000L, 1001L, 1002L }, array);

        long[] single = Array.rangeClosed(500L, 500L);
        Assertions.assertArrayEquals(new long[] { 500L }, single);
    }

    @Test
    public void testRangeClosed_long_overflow() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            Array.rangeClosed(0L, (long) Integer.MAX_VALUE);
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
    public void testRepeat_generic_deprecated() {
        String[] array = Array.repeat("hello", 2);
        Assertions.assertArrayEquals(new String[] { "hello", "hello" }, array);
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
        String[][] result = Array.concatt(a, b);

        Assertions.assertEquals(2, result.length);
        Assertions.assertArrayEquals(new String[] { "a", "b", "e", "f" }, result[0]);
        Assertions.assertArrayEquals(new String[] { "c", "d", "g", "h" }, result[1]);

        String[][] empty1 = Array.concatt(new String[0][], b);
        Assertions.assertArrayEquals(b, empty1);

        String[][] null1 = Array.concatt(null, b);
        Assertions.assertArrayEquals(b, null1);
    }

    @Test
    public void testConcatt_3D() {
        Integer[][][] a = { { { 1, 2 } }, { { 3, 4 } } };
        Integer[][][] b = { { { 5, 6 } }, { { 7, 8 } } };
        Integer[][][] result = Array.concatt(a, b);

        Assertions.assertEquals(2, result.length);
    }

    @Test
    public void testBox_boolean() {
        boolean[] primitive = { true, false, true };
        Boolean[] boxed = Array.box(primitive);
        Assertions.assertArrayEquals(new Boolean[] { true, false, true }, boxed);

        Assertions.assertNull(Array.box((boolean[]) null));

        Boolean[] empty = Array.box(new boolean[0]);
        Assertions.assertEquals(0, empty.length);
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
}

package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

public class Array100Test extends TestBase {

    // Test newInstance methods
    @Test
    public void testNewInstance_SingleDimension() {
        // Test with various component types
        Integer[] intArray = Array.newInstance(Integer.class, 5);
        assertNotNull(intArray);
        assertEquals(5, intArray.length);

        String[] strArray = Array.newInstance(String.class, 10);
        assertNotNull(strArray);
        assertEquals(10, strArray.length);

        // Test with zero length
        Double[] emptyArray = Array.newInstance(Double.class, 0);
        assertNotNull(emptyArray);
        assertEquals(0, emptyArray.length);
    }

    @Test
    public void testNewInstance_SingleDimension_NegativeSize() {
        assertThrows(NegativeArraySizeException.class, () -> Array.newInstance(String.class, -1));
    }

    @Test
    public void testNewInstance_MultiDimension() {
        // Test 2D array
        Integer[][] array2D = Array.newInstance(Integer.class, 3, 4);
        assertNotNull(array2D);
        assertEquals(3, array2D.length);
        assertEquals(4, array2D[0].length);

        // Test 3D array
        String[][][] array3D = Array.newInstance(String.class, 2, 3, 4);
        assertNotNull(array3D);
        assertEquals(2, array3D.length);
        assertEquals(3, array3D[0].length);
        assertEquals(4, array3D[0][0].length);
    }

    @Test
    public void testNewInstance_MultiDimension_NegativeSize() {
        assertThrows(NegativeArraySizeException.class, () -> Array.newInstance(String.class, 3, -1));
    }

    // Test getLength method
    @Test
    public void testGetLength() {
        int[] intArray = new int[5];
        assertEquals(5, Array.getLength(intArray));

        String[] strArray = new String[10];
        assertEquals(10, Array.getLength(strArray));

        // Test with null
        assertEquals(0, Array.getLength(null));

        // Test with empty array
        Object[] emptyArray = new Object[0];
        assertEquals(0, Array.getLength(emptyArray));
    }

    @Test
    public void testGetLength_NonArray() {
        assertThrows(IllegalArgumentException.class, () -> Array.getLength("not an array"));
    }

    // Test get methods
    @Test
    public void testGet() {
        Integer[] array = { 1, 2, 3, 4, 5 };
        assertEquals(Integer.valueOf(1), Array.get(array, 0));
        assertEquals(Integer.valueOf(3), Array.get(array, 2));
        assertEquals(Integer.valueOf(5), Array.get(array, 4));
    }

    @Test
    public void testGet_IndexOutOfBounds() {
        Integer[] array = { 1, 2, 3 };
        assertThrows(ArrayIndexOutOfBoundsException.class, () -> Array.get(array, 3));
    }

    @Test
    public void testGetBoolean() {
        boolean[] array = { true, false, true };
        assertTrue(Array.getBoolean(array, 0));
        assertFalse(Array.getBoolean(array, 1));
        assertTrue(Array.getBoolean(array, 2));
    }

    @Test
    public void testGetByte() {
        byte[] array = { 1, 2, 3 };
        assertEquals(1, Array.getByte(array, 0));
        assertEquals(2, Array.getByte(array, 1));
        assertEquals(3, Array.getByte(array, 2));
    }

    @Test
    public void testGetChar() {
        char[] array = { 'a', 'b', 'c' };
        assertEquals('a', Array.getChar(array, 0));
        assertEquals('b', Array.getChar(array, 1));
        assertEquals('c', Array.getChar(array, 2));
    }

    @Test
    public void testGetShort() {
        short[] array = { 10, 20, 30 };
        assertEquals(10, Array.getShort(array, 0));
        assertEquals(20, Array.getShort(array, 1));
        assertEquals(30, Array.getShort(array, 2));
    }

    @Test
    public void testGetInt() {
        int[] array = { 100, 200, 300 };
        assertEquals(100, Array.getInt(array, 0));
        assertEquals(200, Array.getInt(array, 1));
        assertEquals(300, Array.getInt(array, 2));
    }

    @Test
    public void testGetLong() {
        long[] array = { 1000L, 2000L, 3000L };
        assertEquals(1000L, Array.getLong(array, 0));
        assertEquals(2000L, Array.getLong(array, 1));
        assertEquals(3000L, Array.getLong(array, 2));
    }

    @Test
    public void testGetFloat() {
        float[] array = { 1.1f, 2.2f, 3.3f };
        assertEquals(1.1f, Array.getFloat(array, 0), 0.001);
        assertEquals(2.2f, Array.getFloat(array, 1), 0.001);
        assertEquals(3.3f, Array.getFloat(array, 2), 0.001);
    }

    @Test
    public void testGetDouble() {
        double[] array = { 1.11, 2.22, 3.33 };
        assertEquals(1.11, Array.getDouble(array, 0), 0.001);
        assertEquals(2.22, Array.getDouble(array, 1), 0.001);
        assertEquals(3.33, Array.getDouble(array, 2), 0.001);
    }

    // Test set methods
    @Test
    public void testSet() {
        Integer[] array = new Integer[3];
        Array.set(array, 0, 10);
        Array.set(array, 1, 20);
        Array.set(array, 2, 30);

        assertArrayEquals(new Integer[] { 10, 20, 30 }, array);
    }

    @Test
    public void testSetBoolean() {
        boolean[] array = new boolean[3];
        Array.setBoolean(array, 0, true);
        Array.setBoolean(array, 1, false);
        Array.setBoolean(array, 2, true);

        assertArrayEquals(new boolean[] { true, false, true }, array);
    }

    @Test
    public void testSetByte() {
        byte[] array = new byte[3];
        Array.setByte(array, 0, (byte) 1);
        Array.setByte(array, 1, (byte) 2);
        Array.setByte(array, 2, (byte) 3);

        assertArrayEquals(new byte[] { 1, 2, 3 }, array);
    }

    @Test
    public void testSetChar() {
        char[] array = new char[3];
        Array.setChar(array, 0, 'x');
        Array.setChar(array, 1, 'y');
        Array.setChar(array, 2, 'z');

        assertArrayEquals(new char[] { 'x', 'y', 'z' }, array);
    }

    @Test
    public void testSetShort() {
        short[] array = new short[3];
        Array.setShort(array, 0, (short) 10);
        Array.setShort(array, 1, (short) 20);
        Array.setShort(array, 2, (short) 30);

        assertArrayEquals(new short[] { 10, 20, 30 }, array);
    }

    @Test
    public void testSetInt() {
        int[] array = new int[3];
        Array.setInt(array, 0, 100);
        Array.setInt(array, 1, 200);
        Array.setInt(array, 2, 300);

        assertArrayEquals(new int[] { 100, 200, 300 }, array);
    }

    @Test
    public void testSetLong() {
        long[] array = new long[3];
        Array.setLong(array, 0, 1000L);
        Array.setLong(array, 1, 2000L);
        Array.setLong(array, 2, 3000L);

        assertArrayEquals(new long[] { 1000L, 2000L, 3000L }, array);
    }

    @Test
    public void testSetFloat() {
        float[] array = new float[3];
        Array.setFloat(array, 0, 1.1f);
        Array.setFloat(array, 1, 2.2f);
        Array.setFloat(array, 2, 3.3f);

        assertArrayEquals(new float[] { 1.1f, 2.2f, 3.3f }, array, 0.001f);
    }

    @Test
    public void testSetDouble() {
        double[] array = new double[3];
        Array.setDouble(array, 0, 1.11);
        Array.setDouble(array, 1, 2.22);
        Array.setDouble(array, 2, 3.33);

        assertArrayEquals(new double[] { 1.11, 2.22, 3.33 }, array, 0.001);
    }

    // Test asList method
    @Test
    public void testAsList() {
        List<String> list = Array.asList("a", "b", "c");
        assertEquals(3, list.size());
        assertEquals("a", list.get(0));
        assertEquals("b", list.get(1));
        assertEquals("c", list.get(2));

        // Test with null array
        List<String> emptyList = Array.asList((String[]) null);
        assertTrue(emptyList.isEmpty());

        // Test with empty array
        List<Integer> emptyIntList = Array.asList(new Integer[0]);
        assertTrue(emptyIntList.isEmpty());
    }

    // Test of methods
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
    public void testOf_String() {
        String[] array = Array.of("a", "b", "c");
        assertArrayEquals(new String[] { "a", "b", "c" }, array);
    }

    @Test
    public void testOf_Date() {
        java.util.Date date1 = new java.util.Date();
        java.util.Date date2 = new java.util.Date();
        java.util.Date[] array = Array.of(date1, date2);
        assertArrayEquals(new java.util.Date[] { date1, date2 }, array);
    }

    // Test range methods
    @Test
    public void testRange_Char() {
        char[] range = Array.range('a', 'e');
        assertArrayEquals(new char[] { 'a', 'b', 'c', 'd' }, range);

        // Test empty range
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

    // Test range with step methods
    @Test
    public void testRange_CharWithStep() {
        char[] range = Array.range('a', 'g', 2);
        assertArrayEquals(new char[] { 'a', 'c', 'e' }, range);

        // Test negative step
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

    // Test rangeClosed methods
    @Test
    public void testRangeClosed_Char() {
        char[] range = Array.rangeClosed('a', 'd');
        assertArrayEquals(new char[] { 'a', 'b', 'c', 'd' }, range);

        // Test single element
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

    // Test rangeClosed with step methods
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

    // Test repeat methods
    @Test
    public void testRepeat_Boolean() {
        boolean[] array = Array.repeat(true, 3);
        assertArrayEquals(new boolean[] { true, true, true }, array);
    }

    @Test
    public void testRepeat_Char() {
        char[] array = Array.repeat('x', 4);
        assertArrayEquals(new char[] { 'x', 'x', 'x', 'x' }, array);
    }

    @Test
    public void testRepeat_Byte() {
        byte[] array = Array.repeat((byte) 5, 3);
        assertArrayEquals(new byte[] { 5, 5, 5 }, array);
    }

    @Test
    public void testRepeat_Short() {
        short[] array = Array.repeat((short) 10, 3);
        assertArrayEquals(new short[] { 10, 10, 10 }, array);
    }

    @Test
    public void testRepeat_Int() {
        int[] array = Array.repeat(100, 3);
        assertArrayEquals(new int[] { 100, 100, 100 }, array);
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
    public void testRepeat_String() {
        String[] array = Array.repeat("test", 3);
        assertArrayEquals(new String[] { "test", "test", "test" }, array);
    }

    @Test
    public void testRepeat_ObjectWithClass() {
        String[] array = Array.repeat("hello", 3, String.class);
        assertArrayEquals(new String[] { "hello", "hello", "hello" }, array);
    }

    @Test
    public void testRepeatNonNull() {
        String[] array = Array.repeatNonNull("world", 3);
        assertArrayEquals(new String[] { "world", "world", "world" }, array);
    }

    @Test
    public void testRepeatNonNull_NullElement() {
        assertThrows(IllegalArgumentException.class, () -> Array.repeatNonNull(null, 3));
    }

    // Test random methods
    @Test
    public void testRandom() {
        int[] array = Array.random(10);
        assertNotNull(array);
        assertEquals(10, array.length);
        // All elements should be random integers
    }

    @Test
    public void testRandom_WithRange() {
        int[] array = Array.random(0, 10, 100);
        assertNotNull(array);
        assertEquals(100, array.length);

        // Verify all elements are within range
        for (int value : array) {
            assertTrue(value >= 0 && value < 10);
        }
    }

    @Test
    public void testRandom_InvalidRange() {
        assertThrows(IllegalArgumentException.class, () -> Array.random(10, 5, 10));
    }

    // Test concat methods for 2D arrays
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
    public void testConcat_Char2D() {
        char[][] a = { { 'a', 'b' }, { 'c', 'd' } };
        char[][] b = { { 'e', 'f' }, { 'g', 'h' } };
        char[][] result = Array.concat(a, b);

        assertEquals(2, result.length);
        assertArrayEquals(new char[] { 'a', 'b', 'e', 'f' }, result[0]);
        assertArrayEquals(new char[] { 'c', 'd', 'g', 'h' }, result[1]);
    }

    @Test
    public void testConcat_Byte2D() {
        byte[][] a = { { 1, 2 }, { 3, 4 } };
        byte[][] b = { { 5, 6 }, { 7, 8 } };
        byte[][] result = Array.concat(a, b);

        assertEquals(2, result.length);
        assertArrayEquals(new byte[] { 1, 2, 5, 6 }, result[0]);
        assertArrayEquals(new byte[] { 3, 4, 7, 8 }, result[1]);
    }

    @Test
    public void testConcat_Short2D() {
        short[][] a = { { 10, 20 }, { 30, 40 } };
        short[][] b = { { 50, 60 }, { 70, 80 } };
        short[][] result = Array.concat(a, b);

        assertEquals(2, result.length);
        assertArrayEquals(new short[] { 10, 20, 50, 60 }, result[0]);
        assertArrayEquals(new short[] { 30, 40, 70, 80 }, result[1]);
    }

    @Test
    public void testConcat_Int2D() {
        int[][] a = { { 100, 200 }, { 300, 400 } };
        int[][] b = { { 500, 600 }, { 700, 800 } };
        int[][] result = Array.concat(a, b);

        assertEquals(2, result.length);
        assertArrayEquals(new int[] { 100, 200, 500, 600 }, result[0]);
        assertArrayEquals(new int[] { 300, 400, 700, 800 }, result[1]);
    }

    @Test
    public void testConcat_Long2D() {
        long[][] a = { { 1000L, 2000L }, { 3000L, 4000L } };
        long[][] b = { { 5000L, 6000L }, { 7000L, 8000L } };
        long[][] result = Array.concat(a, b);

        assertEquals(2, result.length);
        assertArrayEquals(new long[] { 1000L, 2000L, 5000L, 6000L }, result[0]);
        assertArrayEquals(new long[] { 3000L, 4000L, 7000L, 8000L }, result[1]);
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
        String[][] result = Array.concatt(a, b);

        assertEquals(2, result.length);
        assertArrayEquals(new String[] { "a", "b", "e", "f" }, result[0]);
        assertArrayEquals(new String[] { "c", "d", "g", "h" }, result[1]);
    }

    // Test concat methods for 3D arrays
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

    // Test box methods
    @Test
    public void testBox_Boolean() {
        boolean[] primitives = { true, false, true };
        Boolean[] boxed = Array.box(primitives);
        assertArrayEquals(new Boolean[] { true, false, true }, boxed);

        // Test null input
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

    // Test box methods for 2D arrays
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

    // Test box methods for 3D arrays
    @Test
    public void testBox_Boolean3D() {
        boolean[][][] primitives = { { { true, false } }, { { false, true } } };
        Boolean[][][] boxed = Array.box(primitives);

        assertEquals(2, boxed.length);
        assertEquals(1, boxed[0].length);
        assertArrayEquals(new Boolean[] { true, false }, boxed[0][0]);
    }

    // Test unbox methods
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

    // Test unbox methods for 2D arrays
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

    // Test unbox methods for 3D arrays
    @Test
    public void testUnbox_Boolean3D() {
        Boolean[][][] boxed = { { { true, false, null } }, { { false, true, null } } };
        boolean[][][] primitives = Array.unbox(boxed);

        assertEquals(2, primitives.length);
        assertEquals(1, primitives[0].length);
        assertArrayEquals(new boolean[] { true, false, false }, primitives[0][0]);
    }

    // Test transpose methods
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

    // Test edge cases
    @Test
    public void testEdgeCases_NullInputs() {
        // Test null inputs for various methods
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
        // Test empty arrays
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
    public void testArrayUtil_CannotInstantiate() {
        // Test that ArrayUtil is a utility class
        assertTrue(java.lang.reflect.Modifier.isFinal(Array.ArrayUtil.class.getModifiers()));
    }
}

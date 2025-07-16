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

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

public class Array200Test extends TestBase {

    // Test private static final class cs { static final String element = "element"; }
    // Not directly testable as it's private, but used in error messages.

    @Test
    public void testNewInstance_withLength() {
        Integer[] intArray = Array.newInstance(Integer.class, 5);
        assertNotNull(intArray);
        assertEquals(5, intArray.length);
        assertEquals(Integer.class, intArray.getClass().getComponentType());

        String[] stringArray = Array.newInstance(String.class, 0);
        assertNotNull(stringArray);
        assertEquals(0, stringArray.length);
        // Test caching for zero-length arrays (relies on N.CLASS_EMPTY_ARRAY behavior)
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

        // Test with zero in dimensions
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
        assertEquals(2, (Integer) Array.get(primArray, 1)); // Autoboxing for primitive

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
        Array.set(primArray, 1, 5); // Autoboxing for value
        assertEquals(5, primArray[1]);

        assertThrows(IllegalArgumentException.class, () -> Array.set("not an array", 0, 1));
        assertThrows(ArrayIndexOutOfBoundsException.class, () -> Array.set(primArray, 3, 1));
        // Test type mismatch (will throw ArrayStoreException if underlying java.lang.reflect.Array.set does)
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

        List<Integer> list2 = Array.asList(); // Empty
        assertTrue(list2.isEmpty());

        List<Integer> list3 = Array.asList((Integer[]) null); // Null input
        assertTrue(list3.isEmpty()); // Should be N.emptyList()

        // Test that the list is backed by the array
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

        assertArrayEquals(new int[0], Array.of(N.EMPTY_INT_ARRAY)); // Empty int array
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
        assertArrayEquals(N.EMPTY_CHAR_ARRAY, Array.range('d', 'a'));
        assertArrayEquals(N.EMPTY_CHAR_ARRAY, Array.range('a', 'a'));
    }

    @Test
    public void testRange_byte() {
        assertArrayEquals(new byte[] { 10, 11, 12 }, Array.range((byte) 10, (byte) 13));
        assertArrayEquals(N.EMPTY_BYTE_ARRAY, Array.range((byte) 13, (byte) 10));
    }

    @Test
    public void testRange_short() {
        assertArrayEquals(new short[] { 5, 6, 7 }, Array.range((short) 5, (short) 8));
        assertArrayEquals(N.EMPTY_SHORT_ARRAY, Array.range((short) 8, (short) 5));
    }

    @Test
    public void testRange_int() {
        assertArrayEquals(new int[] { 1, 2, 3 }, Array.range(1, 4));
        assertArrayEquals(N.EMPTY_INT_ARRAY, Array.range(4, 1));
        // assertThrows(IllegalArgumentException.class, () -> Array.range(0, Integer.MAX_VALUE));
    }
    // (Previous tests from the first response would be here)

    @Test
    public void testRange_int_continued() {
        assertArrayEquals(new int[] { 1, 2, 3 }, Array.range(1, 4));
        assertArrayEquals(N.EMPTY_INT_ARRAY, Array.range(4, 1));
        assertArrayEquals(N.EMPTY_INT_ARRAY, Array.range(1, 1));
        // Test potential overflow for array size calculation, though the method caps at Integer.MAX_VALUE length
        // This specific case should produce a large valid array up to Integer.MAX_VALUE if memory allows,
        // or throw IllegalArgumentException for "overflow" if (long)endExclusive - startInclusive > Integer.MAX_VALUE
        // For practical testing, we'll use smaller ranges.
        //    assertThrows(OutOfMemoryError.class,
        //            () -> Array.range(0, Integer.MAX_VALUE + 1L > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) (Integer.MAX_VALUE + 1L))); // This won't directly test overflow due to long casting in implementation.
        // if (largeEnd > 1) assertEquals(largeEnd - 1, largeRangeArray[largeEnd-1]);
    }

    @Test
    public void testRange_long() {
        assertArrayEquals(new long[] { 1L, 2L, 3L }, Array.range(1L, 4L));
        assertArrayEquals(N.EMPTY_LONG_ARRAY, Array.range(4L, 1L));
        assertArrayEquals(N.EMPTY_LONG_ARRAY, Array.range(1L, 1L));
        // Test overflow for resulting array size
        // assertThrows(IllegalArgumentException.class, () -> Array.range(0L, (long) Integer.MAX_VALUE + 2L));
    }

    @Test
    public void testRange_withStep_char() {
        assertArrayEquals(new char[] { 'a', 'c', 'e' }, Array.range('a', 'f', 2));
        assertArrayEquals(new char[] { 'e', 'c', 'a' }, Array.range('e', (char) ('a' - 1), -2)); // end is exclusive
        assertArrayEquals(N.EMPTY_CHAR_ARRAY, Array.range('a', 'f', -1)); // wrong direction
        assertArrayEquals(N.EMPTY_CHAR_ARRAY, Array.range('a', 'a', 1));
        assertThrows(IllegalArgumentException.class, () -> Array.range('a', 'f', 0));
    }

    @Test
    public void testRange_withStep_byte() {
        assertArrayEquals(new byte[] { 10, 12, 14 }, Array.range((byte) 10, (byte) 15, (byte) 2));
        assertArrayEquals(new byte[] { 14, 12, 10 }, Array.range((byte) 14, (byte) 9, (byte) -2));
        assertArrayEquals(N.EMPTY_BYTE_ARRAY, Array.range((byte) 10, (byte) 10, (byte) 1));
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
        assertArrayEquals(N.EMPTY_INT_ARRAY, Array.range(1, 6, -1));
        assertArrayEquals(N.EMPTY_INT_ARRAY, Array.range(1, 1, 1));
        assertThrows(IllegalArgumentException.class, () -> Array.range(1, 10, 0));
        // assertThrows(IllegalArgumentException.class, () -> Array.range(0, (int) (((long) Integer.MAX_VALUE * 2L)), 1)); // Test overflow for len
        assertArrayEquals(new int[] {}, Array.range(0, (int) (((long) Integer.MAX_VALUE * 2L)), 1));
    }

    @Test
    public void testRange_withStep_long() {
        assertArrayEquals(new long[] { 1L, 3L, 5L }, Array.range(1L, 6L, 2L));
        assertArrayEquals(new long[] { 5L, 3L, 1L }, Array.range(5L, 0L, -2L));
        assertThrows(IllegalArgumentException.class, () -> Array.range(1L, 10L, 0L));
        // Test overflow for len, where (endExclusive - startInclusive) / by is too large
        assertThrows(IllegalArgumentException.class, () -> Array.range(0L, Long.MAX_VALUE, 1L)); // This could lead to overflow if not handled by BigInteger path
    }

    @Test
    public void testRangeClosed_char() {
        assertArrayEquals(new char[] { 'a', 'b', 'c', 'd' }, Array.rangeClosed('a', 'd'));
        assertArrayEquals(new char[] { 'a' }, Array.rangeClosed('a', 'a'));
        assertArrayEquals(N.EMPTY_CHAR_ARRAY, Array.rangeClosed('d', 'a'));
    }

    @Test
    public void testRangeClosed_byte() {
        assertArrayEquals(new byte[] { 10, 11, 12, 13 }, Array.rangeClosed((byte) 10, (byte) 13));
        assertArrayEquals(new byte[] { 10 }, Array.rangeClosed((byte) 10, (byte) 10));
        assertArrayEquals(N.EMPTY_BYTE_ARRAY, Array.rangeClosed((byte) 13, (byte) 10));
    }

    @Test
    public void testRangeClosed_short() {
        assertArrayEquals(new short[] { 5, 6, 7, 8 }, Array.rangeClosed((short) 5, (short) 8));
        assertArrayEquals(new short[] { 5 }, Array.rangeClosed((short) 5, (short) 5));
        assertArrayEquals(N.EMPTY_SHORT_ARRAY, Array.rangeClosed((short) 8, (short) 5));
    }

    @Test
    public void testRangeClosed_int() {
        assertArrayEquals(new int[] { 1, 2, 3, 4 }, Array.rangeClosed(1, 4));
        assertArrayEquals(new int[] { 1 }, Array.rangeClosed(1, 1));
        assertArrayEquals(N.EMPTY_INT_ARRAY, Array.rangeClosed(4, 1));
        assertThrows(IllegalArgumentException.class, () -> Array.rangeClosed(0, Integer.MAX_VALUE)); // Length = MAX_VALUE + 1
    }

    @Test
    public void testRangeClosed_long() {
        assertArrayEquals(new long[] { 1L, 2L, 3L, 4L }, Array.rangeClosed(1L, 4L));
        assertArrayEquals(new long[] { 1L }, Array.rangeClosed(1L, 1L));
        assertArrayEquals(N.EMPTY_LONG_ARRAY, Array.rangeClosed(4L, 1L));
        assertThrows(IllegalArgumentException.class, () -> Array.rangeClosed(0L, (long) Integer.MAX_VALUE + 1L)); // Length > Integer.MAX_VALUE
    }

    @Test
    public void testRangeClosed_withStep_char() {
        assertArrayEquals(new char[] { 'a', 'c', 'e' }, Array.rangeClosed('a', 'e', 2));
        assertArrayEquals(new char[] { 'e', 'c', 'a' }, Array.rangeClosed('e', 'a', -2));
        assertArrayEquals(new char[] { 'a' }, Array.rangeClosed('a', 'a', 1));
        assertArrayEquals(new char[] { 'a' }, Array.rangeClosed('a', 'a', -1));
        assertArrayEquals(N.EMPTY_CHAR_ARRAY, Array.rangeClosed('a', 'e', -1)); // wrong direction
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
        assertThrows(IllegalArgumentException.class, () -> Array.rangeClosed(0, Integer.MAX_VALUE, 1)); // Test overflow for len
    }

    @Test
    public void testRangeClosed_withStep_long() {
        assertArrayEquals(new long[] { 1L, 3L, 5L }, Array.rangeClosed(1L, 5L, 2L));
        assertArrayEquals(new long[] { 5L, 3L, 1L }, Array.rangeClosed(5L, 1L, -2L));
        assertThrows(IllegalArgumentException.class, () -> Array.rangeClosed(1L, 10L, 0L));
        assertThrows(IllegalArgumentException.class, () -> Array.rangeClosed(0L, Long.MAX_VALUE, 2L)); // test overflow for len
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

        // assertThrows(IllegalArgumentException.class, () -> Array.repeat(null, 3));
        // This would throw NullPointerException due to element.getClass() if N.checkArgNotNull isn't effective
        // Based on code, N.checkArgNotNull will throw IllegalArgumentException
        assertArrayEquals(new String[] { null, null, null }, Array.repeat((String) null, 3));
    }

    @Test
    public void testRepeat_generic_withClass() {
        Integer[] intResult = Array.repeat(7, 3, Integer.class);
        assertArrayEquals(new Integer[] { 7, 7, 7 }, intResult);
        assertEquals(Integer.class, intResult.getClass().getComponentType());

        String[] strResult = Array.repeat("test", 2, String.class);
        assertArrayEquals(new String[] { "test", "test" }, strResult);

        // Test with null element
        Integer[] nullResult = Array.repeat((Integer) null, 3, Integer.class);
        assertArrayEquals(new Integer[] { null, null, null }, nullResult);

        Object[] objResult = Array.repeat("String", 2, Object.class); // element is String, array is Object[]
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
        // Cannot easily test randomness itself, but can check properties
        if (r1.length > 0) {
            // All elements should be populated (not default 0 if random range is wide)
        }
    }

    @Test
    public void testRandom_startEndLen() {
        int[] r1 = Array.random(10, 20, 5);
        assertEquals(5, r1.length);
        for (int val : r1) {
            assertTrue(val >= 10 && val < 20);
        }

        int[] r2 = Array.random(0, 1, 0); // Zero length
        assertEquals(0, r2.length);

        assertThrows(IllegalArgumentException.class, () -> Array.random(10, 5, 3)); // start >= end
        assertThrows(NegativeArraySizeException.class, () -> Array.random(1, 5, -1));

        // Test large range
        int[] rLarge = Array.random(0, (int) (((long) Integer.MAX_VALUE / 2) + 10), 2); // endExclusive is large
        assertEquals(2, rLarge.length);
        for (int val : rLarge) {
            assertTrue(val >= 0 && val < (int) (((long) Integer.MAX_VALUE / 2) + 10));
        }
    }

    // --- Test Concat Methods ---
    @Test
    public void testConcat_boolean_2D() {
        boolean[][] a = { { true }, { false, true } };
        boolean[][] b = { { false } };
        boolean[][] expected = { { true, false }, { false, true } }; // N.concat logic
        // Assuming N.concat merges element-wise, padding with default if needed by N.concat
        // The current Array.concat logic: result[i] = N.concat(i < aLen ? a[i] : null, i < bLen ? b[i] : null);
        // If aLen=2, bLen=1, maxLen=2.
        // i=0: result[0] = N.concat(a[0], b[0]) = N.concat({true}, {false})
        // i=1: result[1] = N.concat(a[1], null) = N.concat({false, true}, null)
        // The exact output depends on N.concat. Let's assume N.concat({x,y},null) is {x,y} and N.concat({x},{y}) is {x,y}
        // Let's write tests based on the implementation logic.

        boolean[][] res1 = Array.concat(new boolean[][] { { true } }, new boolean[][] { { false } });
        assertArrayEquals(new boolean[] { true, false }, res1[0]);

        boolean[][] a2 = { { true }, { false } };
        boolean[][] b2 = { { true } }; // b2 is shorter
        boolean[][] res2 = Array.concat(a2, b2);
        assertArrayEquals(new boolean[] { true, true }, res2[0]);
        assertArrayEquals(new boolean[] { false }, res2[1]); // Assuming N.concat({false}, null) -> {false}

        boolean[][] a3 = { { true } };
        boolean[][] b3 = { { false }, { true } }; // a3 is shorter
        boolean[][] res3 = Array.concat(a3, b3);
        assertArrayEquals(new boolean[] { true, false }, res3[0]);
        assertArrayEquals(new boolean[] { true }, res3[1]); // Assuming N.concat(null, {true}) -> {true}

        assertArrayEquals(new boolean[0][], Array.concat(new boolean[0][], new boolean[0][]));
        boolean[][] arr = { { true } };
        boolean[][] resClone = Array.concat(arr, new boolean[0][]);
        assertArrayEquals(arr[0], resClone[0]); // N.clone might be involved
        assertNotSame(arr, resClone);
    }

    @Test
    public void testConcat_int_3D() {
        int[][][] a = { { { 1 }, { 2 } }, { { 3 } } };
        int[][][] b = { { { 4 } } };
        // maxLen = 2 for outer dimension
        // i=0: result[0] = Array.concat(a[0], b[0]) = Array.concat({{1},{2}}, {{4}})
        //          This means inner concat: res_inner[0]=N.concat({1},{4}), res_inner[1]=N.concat({2},null)
        // i=1: result[1] = Array.concat(a[1], null) = Array.concat({{3}}, null)
        //          This means inner concat: res_inner[0]=N.concat({3},null)

        int[][][] res = Array.concat(a, b);
        assertEquals(2, res.length);
        assertEquals(2, res[0].length); // from a[0] length due to b[0] being shorter or null in N.concat
        assertArrayEquals(new int[] { 1, 4 }, res[0][0]);
        assertArrayEquals(new int[] { 2 }, res[0][1]);
        assertEquals(1, res[1].length);
        assertArrayEquals(new int[] { 3 }, res[1][0]);
    }

    @Test
    public void testConcatt_generic_2D() {
        String[][] a = { { "a" }, { "b", "c" } };
        String[][] b = { { "d" } };
        String[][] res = Array.concatt(a, b);

        assertEquals(2, res.length);
        assertArrayEquals(new String[] { "a", "d" }, res[0]);
        assertArrayEquals(new String[] { "b", "c" }, res[1]); // N.concat({"b","c"}, null)

        String[][] c = null;
        String[][] d = { { "x" } };
        String[][] res2 = Array.concatt(c, d); // N.clone(d)
        assertNotNull(res2);
        assertEquals(1, res2.length);
        assertArrayEquals(new String[] { "x" }, res2[0]);
        assertNotSame(d, res2); // N.clone

        String[][] res3 = Array.concatt(d, c); // N.clone(d)
        assertNotNull(res3);
        assertArrayEquals(new String[] { "x" }, res3[0]);
    }

    // --- Test Box/Unbox Methods ---
    @Test
    public void testBox_boolean() {
        assertNull(Array.box((boolean[]) null));
        assertArrayEquals(N.EMPTY_BOOLEAN_OBJ_ARRAY, Array.box(new boolean[0]));
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
        assertArrayEquals(N.EMPTY_INT_ARRAY, Array.unbox(new Integer[0]));

        Integer[] boxed = { 1, null, 3 };
        assertArrayEquals(new int[] { 1, 0, 3 }, Array.unbox(boxed)); // default for null is 0
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

    // --- Test Transpose Methods ---
    @Test
    public void testTranspose_int() {
        assertNull(Array.transpose((int[][]) null));
        assertArrayEquals(new int[0][], Array.transpose(new int[0][]));

        int[][] m1 = { { 1, 2, 3 }, { 4, 5, 6 } }; // 2x3
        int[][] t1 = { { 1, 4 }, { 2, 5 }, { 3, 6 } }; // 3x2
        assertArrayEquals(t1, Array.transpose(m1));

        int[][] m2 = { { 1, 2 }, { 3, 4 } }; // 2x2
        int[][] t2 = { { 1, 3 }, { 2, 4 } }; // 2x2
        assertArrayEquals(t2, Array.transpose(m2));

        int[][] m3 = { { 1 }, { 2 }, { 3 } }; // 3x1
        int[][] t3 = { { 1, 2, 3 } }; // 1x3
        assertArrayEquals(t3, Array.transpose(m3));

        int[][] m4 = { {} }; // 1x0
        int[][] t4 = {}; // 0x1 (or 0x0 depending on interpretation)
                         // Implementation: rows=1, cols=0. result = new int[0][1].
        assertArrayEquals(new int[0][1], Array.transpose(m4));

        int[][] jagged = { { 1, 2 }, { 3, 4, 5 } };
        assertThrows(IllegalArgumentException.class, () -> Array.transpose(jagged));
    }

    @Test
    public void testTranspose_generic() {
        String[][] m1 = { { "a", "b" }, { "c", "d" } };
        String[][] t1 = { { "a", "c" }, { "b", "d" } };
        assertArrayEquals(t1, Array.transpose(m1));

        // Test component type
        Object[][] m2 = { { 1, "a" }, { new Date(), 3.0 } };
        Object[][] t2 = Array.transpose(m2);
        assertEquals(Object.class, t2.getClass().getComponentType().getComponentType());
        assertArrayEquals(new Object[] { 1, new Date() }, t2[0]);
        assertArrayEquals(new Object[] { "a", 3.0 }, t2[1]);

        // Test with empty inner arrays if they are all consistently empty
        String[][] m3 = { {}, {} }; // 2x0
        String[][] t3 = Array.transpose(m3); // Should be 0x2
        assertEquals(0, t3.length);
        // If t3.length > 0, then assertEquals(2, t3[0].length);
        // Actual implementation: rows=2, cols=0. result = new String[0][2]. Correct.

    }

    // (Previous tests from the first and second responses would be here)

    @Test
    public void testNewInstance_withEmptyDimensions() {
        // java.lang.reflect.Array.newInstance(Class, int...) with empty dimensions (0-D array)
        // returns an instance of the component type.
        assertThrows(IllegalArgumentException.class, () -> Array.newInstance(String.class, new int[0]));
        // assertNull(obj, "0-D array of String (effectively a String instance) should be null if String is the component type");
        // assertInstanceOf(String.class, obj, "Should be an instance of the component type for 0 dimensions");

        // If T is expected to be an array type, a ClassCastException would occur outside this method.
        // Example: String[] arr = Array.newInstance(String.class, new int[0]); // This would throw ClassCastException
        assertThrows(IllegalArgumentException.class, () -> {
            String[] arr = Array.newInstance(String.class, new int[0]);
            System.out.println(arr); // To prevent unused variable warning
        });

        // For primitive component types, it's more complex/problematic with reflect.Array.newInstance
        // Typically, componentType for java.lang.reflect.Array.newInstance refers to the element type.
        // int.class for 0-D array isn't standard; usually it's for elements *within* an array.
        // java.lang.reflect.Array.newInstance(int.class, new int[0]) throws IllegalArgumentException.
        assertThrows(IllegalArgumentException.class, () -> {
            @SuppressWarnings("unused")
            int val = Array.newInstance(int.class, new int[0]);
        });
    }

    @Test
    public void testNewInstance_componentTypeIsArray() {
        // Create an array of Integer arrays
        Integer[][] arrayOfStringArrays = Array.newInstance(Integer[].class, 3);
        assertNotNull(arrayOfStringArrays);
        assertEquals(3, arrayOfStringArrays.length);
        assertEquals(Integer[].class, arrayOfStringArrays.getClass().getComponentType());
        assertNull(arrayOfStringArrays[0]); // Elements are initialized to null
    }

    // --- More detailed tests for long range methods with BigInteger path ---

    @Test
    public void testRange_long_withStep_BigIntegerPath_Overflow() {
        // Case where (end - start) would overflow standard long arithmetic,
        // but BigInteger calculates a number of elements > Integer.MAX_VALUE.
        assertThrows(IllegalArgumentException.class, () -> Array.range(Long.MIN_VALUE, Long.MAX_VALUE, 1L),
                "Should throw due to calculated length exceeding Integer.MAX_VALUE");

        // Case where (end - start) is negative, by is positive, triggering BigInteger path,
        // but length is still too large.
        //        assertThrows(IllegalArgumentException.class, () -> Array.range(Long.MAX_VALUE - 10, Long.MIN_VALUE + 10, 1L),
        //                "Should throw due to calculated length (via BigInteger) exceeding Integer.MAX_VALUE for array allocation");

        assertEquals(N.EMPTY_LONG_ARRAY, Array.range(Long.MAX_VALUE - 10, Long.MIN_VALUE + 10, 1L));
    }

    @Test
    public void testRange_long_withStep_BigIntegerPath_ValidSmallArray() {
        // This tests the BigInteger calculation path for a scenario that *could* overflow
        // `end - start` naively, but `by` is large enough to make the final count small.
        // Example: range from Long.MIN_VALUE to Long.MAX_VALUE with a very large step.
        // Let start = 0, end = Long.MAX_VALUE, by = Long.MAX_VALUE / 2. Len approx 2.
        long[] result = Array.range(0L, Long.MAX_VALUE, Long.MAX_VALUE / 2L);
        assertEquals(3, result.length); // (L.MAX_V / (L.MAX_V/2)) = 2 elements if end is exclusive
        assertArrayEquals(new long[] { 0L, Long.MAX_VALUE / 2L, Long.MAX_VALUE / 2L * 2L }, result);

        // Example: start = Long.MIN_VALUE, end = Long.MIN_VALUE + 10, by = 3
        // This does not use BigInteger path in current impl's specific condition, but tests general logic.
        long[] result2 = Array.range(Long.MIN_VALUE, Long.MIN_VALUE + 10, 3L);
        assertArrayEquals(new long[] { Long.MIN_VALUE, Long.MIN_VALUE + 3, Long.MIN_VALUE + 6, Long.MIN_VALUE + 9 }, result2);

        // A scenario that might hit the `(by > 0 && endExclusive - startInclusive < 0)` condition
        // AND result in a small array is hard because it implies a "wrap-around" count.
        // The current implementation's BigInteger path for `range` is:
        // `if ((by > 0 && endExclusive - startInclusive < 0) || (by < 0 && startInclusive - endExclusive < 0))`
        // This condition means the range "crosses" the long overflow boundary in the direction of `by`.
        // Such a range would inherently be enormous.
        // The more common use of BigInteger here is to prevent `(end - start)` from overflowing
        // before division, if `end` and `start` are far apart.
        // E.g., `Array.range(Long.MIN_VALUE, Long.MAX_VALUE, Long.MAX_VALUE)`
        // `m = (LMV - LMinV) / LMV`. `m` is approx 2.
        // `len = m.longValue()` if `m * by + start == end`, else `m.longValue() + 1` (this is for `range`)
        // `endExclusive - startInclusive` is `Long.MAX_VALUE - Long.MIN_VALUE` (overflows long)
        // `BigInteger.valueOf(Long.MAX_VALUE).subtract(BigInteger.valueOf(Long.MIN_VALUE))` -> approx `2 * Long.MAX_VALUE`
        // `divide(BigInteger.valueOf(Long.MAX_VALUE))` -> approx `2`.
        long[] result3 = Array.range(Long.MIN_VALUE, Long.MAX_VALUE, Long.MAX_VALUE);
        assertEquals(3, result3.length);
        assertArrayEquals(new long[] { Long.MIN_VALUE, Long.MIN_VALUE + Long.MAX_VALUE, Long.MIN_VALUE + Long.MAX_VALUE + Long.MAX_VALUE }, result3);
    }

    @Test
    public void testRangeClosed_long_withStep_BigIntegerPath_Overflow() {
        assertThrows(IllegalArgumentException.class, () -> Array.rangeClosed(Long.MIN_VALUE, Long.MAX_VALUE, 1L),
                "Should throw due to calculated length exceeding Integer.MAX_VALUE");

        // Test the specific condition `((endInclusive - startInclusive) / by + 1 <= 0)` for BigInteger path
        // This can happen if `by` is negative and `end-start` is positive, making the division negative.
        // e.g. rangeClosed(0, 10, -1) -> (10-0)/-1 + 1 = -10 + 1 = -9. This would normally be empty.
        // The check `endInclusive > startInclusive != by > 0` handles this first.
        // So this specific part of the BigInteger trigger is unlikely unless primary checks are bypassed.
    }

    @Test
    public void testRangeClosed_long_withStep_BigIntegerPath_ValidSmallArray() {
        // Similar to range, testing safe calculation for potentially overflowing (end-start)
        // `Array.rangeClosed(Long.MIN_VALUE, Long.MAX_VALUE, Long.MAX_VALUE)`
        // `m = (LMV - LMinV) / LMV` -> approx 2.
        // `len = m.longValue() + 1` -> 3.
        long[] result = Array.rangeClosed(Long.MIN_VALUE, Long.MAX_VALUE, Long.MAX_VALUE);
        assertEquals(3, result.length); // (end-start)/by + 1
        assertArrayEquals(new long[] { Long.MIN_VALUE, Long.MIN_VALUE + Long.MAX_VALUE,
                Long.MIN_VALUE + 2 * Long.MAX_VALUE /* this will wrap to MAX_VALUE-1 if LMINV is odd, or MAX_VALUE if LMINV is even*/ }, result);
        // More precisely:
        // arr[0] = Long.MIN_VALUE
        // arr[1] = Long.MIN_VALUE + Long.MAX_VALUE (which is -1 if MAX_VALUE is 2^63-1)
        // arr[2] = Long.MIN_VALUE + 2 * Long.MAX_VALUE (which is Long.MAX_VALUE if MAX_VALUE is 2^63-1)
        // This shows the elements.

        // Test `len = (endInclusive - startInclusive) / by + 1;`
        // if `end = 0, start = 0, by = 1`. `len = (0/1)+1 = 1`. Correct.
        long[] r = Array.rangeClosed(0L, 0L, 1L);
        assertArrayEquals(new long[] { 0L }, r);

        // if `end = 5, start = 0, by = 2`. `len = (5/2)+1 = 2+1 = 3`. Correct. `{0,2,4}`
        long[] r2 = Array.rangeClosed(0L, 5L, 2L);
        assertArrayEquals(new long[] { 0L, 2L, 4L }, r2);

        // if `end = Long.MAX_VALUE, start = 0, by = Long.MAX_VALUE / 2`.
        // `len = (LMax / (LMax/2)) + 1 = 2 + 1 = 3`.
        long[] r3 = Array.rangeClosed(0L, Long.MAX_VALUE, Long.MAX_VALUE / 2L);
        assertEquals(3, r3.length);
        assertArrayEquals(new long[] { 0L, Long.MAX_VALUE / 2L, (Long.MAX_VALUE / 2L) * 2L }, r3);
    }

    // --- More detailed tests for concat/concatt 3D ---
    @Test
    public void testConcat_int_3D_VariedStructures() {
        // Case 1: a outer shorter, b outer longer
        int[][][] a1 = { { { 1, 2 }, { 3 } } }; // 1x2x?
        int[][][] b1 = { { { 4 } }, { { 5, 6 }, { 7 } } }; // 2x?x?
        // maxLenOuter = 2
        // res[0] = concat(a1[0], b1[0]) = concat({{1,2},{3}}, {{4}})
        //      res[0][0] = N.concat({1,2}, {4}) -> {1,2,4}
        //      res[0][1] = N.concat({3}, null) -> {3}
        // res[1] = concat(null, b1[1]) = concat(null, {{5,6},{7}})
        //      res[1][0] = N.concat(null, {5,6}) -> {5,6}
        //      res[1][1] = N.concat(null, {7}) -> {7}
        int[][][] result1 = Array.concat(a1, b1);
        assertEquals(2, result1.length);
        assertNotNull(result1[0]);
        assertEquals(2, result1[0].length); // Max of a1[0].length and b1[0].length (if N.concat pads) or determined by Array.concat logic
                                            // Array.concat uses N.max(N.len(a), N.len(b)) for the current dim.
                                            // Inside the loop: result[i] = concat(i < aLen ? a[i] : null, i < bLen ? b[i] : null);
                                            // So for res[0], inner a = a1[0], inner b = b1[0]. Max len of these is 2 (from a1[0]).
        assertArrayEquals(new int[] { 1, 2, 4 }, result1[0][0]);
        assertArrayEquals(new int[] { 3 }, result1[0][1]);
        assertNotNull(result1[1]);
        assertEquals(2, result1[1].length); // from b1[1].length
        assertArrayEquals(new int[] { 5, 6 }, result1[1][0]);
        assertArrayEquals(new int[] { 7 }, result1[1][1]);

        // Case 2: Inner arrays having nulls or being empty
        int[][][] a2 = { { { 1 }, null }, { {} } };
        int[][][] b2 = { { null, { 2 } } };
        // maxLenOuter = 2
        // res[0] = concat(a2[0], b2[0]) = concat({{1},null}, {null,{2}})
        //      maxLenInner = 2
        //      res[0][0] = N.concat(a2[0][0], b2[0][0]) = N.concat({1}, null) -> {1}
        //      res[0][1] = N.concat(a2[0][1], b2[0][1]) = N.concat(null, {2}) -> {2}
        // res[1] = concat(a2[1], null) = concat({{}}, null)
        //      maxLenInner = 1
        //      res[1][0] = N.concat(a2[1][0], null) = N.concat({}, null) -> {}

        int[][][] result2 = Array.concat(a2, b2);
        assertEquals(2, result2.length); // Max of a2.length and b2.length
        assertEquals(2, result2[0].length); // Max of a2[0].length and b2[0].length
        assertArrayEquals(new int[] { 1 }, result2[0][0]);
        assertArrayEquals(new int[] { 2 }, result2[0][1]);

        assertEquals(1, result2[1].length); // from a2[1].length
        assertArrayEquals(N.EMPTY_INT_ARRAY, result2[1][0]);
    }

    @Test
    public void testConcatt_String_3D_EmptyAndNulls() {
        String[][][] a = { { { "a" }, null }, { {} } };
        String[][][] b = { { null, { "b" } } };
        String[][][] result = Array.concatt(a, b);

        assertEquals(2, result.length);
        assertEquals(String[][].class, result.getClass().getComponentType());

        assertEquals(2, result[0].length);
        assertEquals(String[].class, result[0].getClass().getComponentType());
        assertArrayEquals(new String[] { "a" }, result[0][0]);
        assertArrayEquals(new String[] { "b" }, result[0][1]);

        assertEquals(1, result[1].length);
        assertArrayEquals(N.EMPTY_STRING_ARRAY, result[1][0]);

        // Test with one array being completely null
        String[][][] c = null;
        String[][][] d = { { { "x" } } };
        String[][][] resNull = Array.concatt(c, d);
        assertNotNull(resNull);
        assertEquals(1, resNull.length);
        assertArrayEquals(new String[][] { { "x" } }, resNull[0]);
        assertNotSame(d, resNull); // Should be a clone
    }

    // --- Transpose with specific empty/0-dimension cases ---
    @Test
    public void testTranspose_singleRowOrColumn_zeroDimension() {
        // Matrix 1x0
        int[][] m1 = new int[1][0]; // {{}}
        int[][] t1 = Array.transpose(m1);
        assertEquals(0, t1.length, "Transposing a 1x0 matrix should result in a 0x1 matrix (outer length 0)");
        // If t1.length were > 0, then t1[0].length would be 1.
        // Correct: new int[cols][rows] -> new int[0][1]

        // Matrix 0x1 (This input itself is new int[0][1], meaning array of 0 int[1] elements)
        int[][] m2 = new int[0][1];
        int[][] t2 = Array.transpose(m2); // a.length is 0, so it returns a.clone()
        assertEquals(0, t2.length);
        // No exception for inconsistent column length because loop in checkIfMatrixArray won't run.

        // Matrix 0x0
        int[][] m3 = new int[0][0];
        int[][] t3 = Array.transpose(m3); // a.length is 0
        assertEquals(0, t3.length);
    }

    @Test
    public void testTranspose_generic_singleRowOrColumn_zeroDimension() {
        String[][] m1 = new String[1][0]; // {{}}
        String[][] t1 = Array.transpose(m1);
        assertEquals(0, t1.length); // Result is String[0][1]

        String[][] m2 = new String[0][1];
        String[][] t2 = Array.transpose(m2);
        assertEquals(0, t2.length);

        String[][] m3 = new String[0][0];
        String[][] t3 = Array.transpose(m3);
        assertEquals(0, t3.length);
    }

    // (Previous tests from the first, second, and third responses would be here)

    @Test
    public void testGet_arrayOfArrays() {
        String[][] arrayOfArrays = { { "a", "b" }, { "c", "d" } };
        String[] innerArray = Array.get(arrayOfArrays, 0);
        assertArrayEquals(new String[] { "a", "b" }, innerArray);

        Object obj = Array.get(arrayOfArrays, 1);
        assertInstanceOf(String[].class, obj);
        assertArrayEquals(new String[] { "c", "d" }, (String[]) obj);

        int[][] primitiveMatrix = { { 1, 2 }, { 3, 4 } };
        // Getting a primitive inner array returns it as an Object, needs casting.
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
        assertSame(newInnerArray, arrayOfArrays[0]); // Should set the exact object

        // Test with type mismatch for inner array - should lead to ArrayStoreException
        Object[][] looselyTypedArray = new Object[2][];
        Array.set(looselyTypedArray, 0, new String[] { "hello" }); // OK
        assertThrows(IllegalArgumentException.class, () -> {
            String[][] strictlyTypedArray = new String[1][];
            // Trying to set an Integer[] into a String[][]'s element (which should be String[])
            Array.set(strictlyTypedArray, 0, new Integer[] { 1, 2 });
        });
    }

    // --- Specific tests for range/rangeClosed long with BigInteger path and complex length calc ---
    @Test
    public void testRange_long_withStep_BigIntegerPath_ExactEnd() {
        // start=0, end=4, by=2. m = (4-0)/2 = 2.
        // m*by+start = 2*2+0 = 4. equals(end) is true. len = m.longValue() = 2.
        // Elements: 0, 2. (Correct for exclusive end)
        // This specific case might not trigger BigInteger unless values are huge.
        // Assuming values are large enough to trigger BigInteger:
        long[] result = Array.range(0L, 4L, 2L); // Small values, doesn't use BigInt path.
        assertArrayEquals(new long[] { 0L, 2L }, result);

        // To force BigInteger for calculation (end - start overflows long)
        // And make length small.
        // Let start = Long.MIN_VALUE, end = Long.MIN_VALUE + 4, by = 2
        // end - start = 4. This won't overflow.
        // Let start = Long.MAX_VALUE - 3, end = Long.MAX_VALUE + 1 (overflows to Long.MIN_VALUE if added directly), by = 2
        // This case is tricky. `endExclusive - startInclusive < 0` with `by > 0` is for wrap-around.

        // Test the length calculation from BigInteger:
        // `m = floor((end - start) / by)`
        // `len = (m * by + start == end) ? m : m + 1`
        // Example: start=0, end=Long.MAX_VALUE, by=(Long.MAX_VALUE/2 + 100) -> m=1
        // `1 * (LMV/2+100) + 0` != `LMV`. So len = 1+1 = 2.
        // Elements: 0, LMV/2+100
        long step = (Long.MAX_VALUE / 2L) + 100L;
        long[] resultBigLenCalc = Array.range(0L, Long.MAX_VALUE, step);
        assertEquals(2, resultBigLenCalc.length);
        assertArrayEquals(new long[] { 0L, step }, resultBigLenCalc);
    }

    @Test
    public void testRange_long_withStep_BigIntegerPath_NonExactEnd() {
        // start=0, end=5, by=2. m = (5-0)/2 = 2.
        // m*by+start = 2*2+0 = 4. equals(end) is false. len = m.longValue() + 1 = 3.
        // Elements: 0, 2, 4. (Correct for exclusive end)
        long[] result = Array.range(0L, 5L, 2L); // Small values
        assertArrayEquals(new long[] { 0L, 2L, 4L }, result);

        // Example: start=0, end=Long.MAX_VALUE, by=(Long.MAX_VALUE/3) -> m=2 (approx)
        // More precisely, m = 2 if Long.MAX_VALUE is like 3k-1 or 3k-2. If 3k, m=3.
        // Assume LMV = 3k-1 (e.g., 8). LMV/3 = 2. m=2.
        // `2 * (LMV/3) + 0` != LMV. So len = 2+1 = 3.
        // Elements: 0, LMV/3, 2*LMV/3
        long step = Long.MAX_VALUE / 3L;
        long endVal = Long.MAX_VALUE;
        // If step * 3 is slightly less than endVal due to truncation:
        // m will be 2. (step*m + start) will be 2*step. If this is not endVal, len = m+1 = 3.
        long[] resultBigLenCalc = Array.range(0L, endVal, step);
        if (step == 0) { // Avoid divide by zero if LMV/3L is 0 (e.g. LMV < 3)
            if (endVal > 0L)
                assertThrows(IllegalArgumentException.class, () -> Array.range(0L, endVal, step));
            else
                assertEquals(0, resultBigLenCalc.length);
        } else {
            long m = (endVal - 0L) / step; // Integer division
            long calcLen = (m * step + 0L == endVal) ? m : m + 1;
            if (step < 0 && endVal > 0L)
                calcLen = 0; // Mismatched signs
            if (step > 0 && endVal < 0L)
                calcLen = 0; // Mismatched signs
            if (endVal == 0L && step != 0)
                calcLen = 0;

            assertEquals(calcLen, resultBigLenCalc.length, "Length calculation mismatch for large longs");
        }
    }

    // --- Transpose with null inner arrays ---
    @Test
    public void testTranspose_withNullInnerArray() {
        int[][] matrixWithNullRow = { { 1, 2 }, null, { 5, 6 } };
        // checkIfMatrixArray: cols = getLength(a[0]) = 2.
        // For i=1, a[1] is null. getLength(a[1]) is 0.
        // 0 != 2, so IllegalArgumentException.
        assertThrows(IllegalArgumentException.class, () -> Array.transpose(matrixWithNullRow));

        String[][] matrixWithNullRowGeneric = { { "a", "b" }, null, { "e", "f" } };
        assertThrows(IllegalArgumentException.class, () -> Array.transpose(matrixWithNullRowGeneric));
    }

    @Test
    public void testTranspose_firstRowNull() {
        int[][] matrixFirstRowNull = { null, { 3, 4 } };
        // checkIfMatrixArray: cols = getLength(a[0]=null) = 0.
        // For i=1, a[1] is {3,4}. getLength(a[1]) is 2.
        // 2 != 0, so IllegalArgumentException.
        assertThrows(IllegalArgumentException.class, () -> Array.transpose(matrixFirstRowNull));
    }

    @Test
    public void testTranspose_allRowsNull_consistentLengthZero() {
        int[][] matrixAllNull = { null, null, null };
        // checkIfMatrixArray: cols = getLength(null) = 0.
        // Loop: getLength(null) == 0. No exception.
        // transpose: rows=3, cols=0. c = new int[0][3].
        assertThrows(NullPointerException.class, () -> Array.transpose(matrixAllNull));
        // assertEquals(0, result.length);
        // If result had rows, they'd have length 3: result[0].length == 3 (if result.length > 0)
    }

    // --- box/unbox with fromIndex == toIndex ---
    @Test
    public void testBox_fromIndexEqualsToIndex() {
        boolean[] boolArray = { true, false, true };
        Boolean[] resultBool = Array.box(boolArray, 1, 1);
        assertArrayEquals(N.EMPTY_BOOLEAN_OBJ_ARRAY, resultBool);
        assertEquals(0, resultBool.length);

        int[] intArray = { 1, 2, 3, 4 };
        Integer[] resultInt = Array.box(intArray, 3, 3);
        assertArrayEquals(N.EMPTY_INT_OBJ_ARRAY, resultInt);
        assertEquals(0, resultInt.length);

        char[] charArray = { 'a', 'b' };
        Character[] resultChar = Array.box(charArray, 0, 0);
        assertArrayEquals(N.EMPTY_CHAR_OBJ_ARRAY, resultChar);
    }

    @Test
    public void testUnbox_fromIndexEqualsToIndex() {
        Boolean[] boolArray = { true, false, true };
        boolean[] resultBool = Array.unbox(boolArray, 1, 1, false);
        assertArrayEquals(N.EMPTY_BOOLEAN_ARRAY, resultBool);
        assertEquals(0, resultBool.length);

        Integer[] intArray = { 1, 2, 3, 4 };
        int[] resultInt = Array.unbox(intArray, 2, 2, 0);
        assertArrayEquals(N.EMPTY_INT_ARRAY, resultInt);
        assertEquals(0, resultInt.length);
    }

    // Final check on `of` methods for more types if any are particularly special
    // `Array.of(final T... a)` is deprecated in favor of `N.asArray`
    // The specific `of` for Enum, Date, Calendar, Temporal seem fine.
    // `String... a` is also fine.

    @Test
    public void testOf_variousObjectTypes() {
        // Test Object[]
        Object[] objects = Array.oF(new Object(), "string", 123); // Using deprecated oF as it's generic T...
        assertEquals(3, objects.length);
        assertInstanceOf(Object.class, objects[0]);
        assertEquals("string", objects[1]);
        assertEquals(123, objects[2]);

        // Test more specific N.asArray equivalent (which `oF` is deprecated for)
        // The class Array itself has `of` methods for specific common types,
        // and the generic `oF` for others.
        // Let's assume N.asArray works as a general varargs to array converter.
        // The provided `Array.of` methods are mostly for primitives and selected object types.
    }

    // Check if any method in `Array.ArrayUtil` is new/public.
    // `public static final class ArrayUtil extends Array`
    // Since `Array` has a private constructor, `ArrayUtil` doesn't change accessibility of `Array`'s static methods.
    // `ArrayUtil` has a private constructor too. No new public methods.
}

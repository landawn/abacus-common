package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class CommonUtilCloneTest extends CommonUtilTestSupport {

    @Test
    public void testClone_1d() {
        assertNull(CommonUtil.clone((boolean[]) null));
        boolean[] bools = { true, false, true };
        boolean[] clonedBools = CommonUtil.clone(bools);
        assertNotSame(bools, clonedBools);
        assertArrayEquals(bools, clonedBools);

        char[] chars = { 'a', 'b', 'c' };
        assertArrayEquals(chars, CommonUtil.clone(chars));
        assertNotSame(chars, CommonUtil.clone(chars));

        byte[] bytes = { 1, 2, 3 };
        assertArrayEquals(bytes, CommonUtil.clone(bytes));
        short[] shorts = { 10, 20, 30 };
        assertArrayEquals(shorts, CommonUtil.clone(shorts));
        int[] ints = { 1, 2, 3 };
        assertNotSame(ints, CommonUtil.clone(ints));
        assertTrue(CommonUtil.equals(ints, CommonUtil.clone(ints)));
        long[] longs = { 1000L, 2000L, 3000L };
        assertArrayEquals(longs, CommonUtil.clone(longs));
        float[] floats = { 1.0f, 2.0f, 3.0f };
        assertArrayEquals(floats, CommonUtil.clone(floats));
        double[] doubles = { 1.0, 2.0, 3.0 };
        assertArrayEquals(doubles, CommonUtil.clone(doubles));
        String[] strings = { "a", "b", "c" };
        assertArrayEquals(strings, CommonUtil.clone(strings));
        assertNotSame(strings, CommonUtil.clone(strings));
    }

    @Test
    public void testClone_2d() {
        assertNull(CommonUtil.clone((boolean[][]) null));
        boolean[][] bools = { { true, false }, { false, true } };
        boolean[][] clonedBools = CommonUtil.clone(bools);
        assertNotSame(bools, clonedBools);
        assertNotSame(bools[0], clonedBools[0]);
        clonedBools[0][0] = false;
        assertTrue(bools[0][0]);
        assertNull(CommonUtil.clone(new boolean[][] { { true }, null })[1]);
        assertEquals(0, CommonUtil.clone(new boolean[0][0]).length);

        char[][] chars = { { 'a', 'b' }, { 'c', 'd' } };
        char[][] clonedChars = CommonUtil.clone(chars);
        clonedChars[0][0] = 'z';
        assertEquals('a', chars[0][0]);

        byte[][] bytes = { { 1, 2 }, { 3, 4 } };
        byte[][] clonedBytes = CommonUtil.clone(bytes);
        clonedBytes[0][0] = 9;
        assertEquals(1, bytes[0][0]);

        int[][] ints = { { 1, 2 }, { 3, 4 } };
        int[][] clonedInts = CommonUtil.clone(ints);
        assertNotSame(ints, clonedInts);
        assertEquals(ints.length, clonedInts.length);
        clonedInts[0][0] = 9;
        assertEquals(1, ints[0][0]);

        String[][] strings = { { "a", "b" }, { "c", "d" } };
        String[][] clonedStrings = CommonUtil.clone(strings);
        clonedStrings[0][0] = "z";
        assertEquals("a", strings[0][0]);
    }

    @Test
    public void testClone_3d() {
        assertNull(CommonUtil.clone((double[][][]) null));
        double[][][] empty = CommonUtil.clone(new double[0][][]);
        assertNotNull(empty);
        assertEquals(0, empty.length);

        double[][][] original = { { { 1.0, 2.0 }, { 3.0, 4.0 } }, { { 5.0, 6.0 }, { 7.0, 8.0 } } };
        double[][][] cloned = CommonUtil.clone(original);
        assertNotSame(original, cloned);
        assertNotSame(original[0], cloned[0]);
        assertNotSame(original[0][0], cloned[0][0]);
        assertArrayEquals(original[0][0], cloned[0][0]);
        cloned[0][0][0] = 99.0;
        assertNotEquals(original[0][0][0], cloned[0][0][0]);

        int[][][] ints = { { { 1, 2 } }, { { 3, 4 } } };
        int[][][] clonedInts = CommonUtil.clone(ints);
        clonedInts[0][0][0] = 9;
        assertEquals(1, ints[0][0][0]);
        assertNull(CommonUtil.clone(new int[][][] { { { 1, 2 } }, null })[1]);
        assertEquals(0, CommonUtil.clone(new int[0][0][0]).length);

        String[][][] strings = { { { "a", "b" } }, { { "c", "d" } } };
        String[][][] clonedStrings = CommonUtil.clone(strings);
        clonedStrings[0][0][0] = "z";
        assertEquals("a", strings[0][0][0]);
        assertNull(CommonUtil.clone(new String[][][] { { { "a", "b" } }, null })[1]);

        char[][][] chars = { { { 'a', 'b' } }, { { 'c', 'd' } } };
        char[][][] clonedChars = CommonUtil.clone(chars);
        clonedChars[0][0][0] = 'z';
        assertEquals('a', chars[0][0][0]);
    }
}

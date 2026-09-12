package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

public class ArrayBoxTest extends ArrayTestSupport {

    @Test
    public void testBox_boolean() {
        assertArrayEquals(new Boolean[] { true, false, true }, Array.box(true, false, true));
        assertNull(Array.box((boolean[]) null));
        assertSame(CommonUtil.EMPTY_BOOLEAN_OBJ_ARRAY, Array.box(new boolean[0]));
        assertArrayEquals(new Boolean[] { false, true }, Array.box(new boolean[] { true, false, true, false }, 1, 3));
        assertSame(CommonUtil.EMPTY_BOOLEAN_OBJ_ARRAY, Array.box(new boolean[] { true }, 0, 0));
        assertNull(Array.box((boolean[]) null, 0, 4));
        assertThrows(IndexOutOfBoundsException.class, () -> Array.box(new boolean[] { true }, 0, 2));
        assertThrows(IndexOutOfBoundsException.class, () -> Array.box(new boolean[] { true }, -1, 1));
    }

    @Test
    public void testBox_char() {
        assertArrayEquals(new Character[] { 'a', 'b' }, Array.box('a', 'b'));
        assertNull(Array.box((char[]) null));
        assertSame(CommonUtil.EMPTY_CHAR_OBJ_ARRAY, Array.box(new char[0]));
        assertArrayEquals(new Character[] { 'b', 'c' }, Array.box(new char[] { 'a', 'b', 'c', 'd' }, 1, 3));
        assertNull(Array.box((char[]) null, 0, 4));
        assertThrows(IndexOutOfBoundsException.class, () -> Array.box(new char[] { 'a' }, 0, 2));
    }

    @Test
    public void testBox_byte() {
        assertArrayEquals(new Byte[] { 1, 2 }, Array.box((byte) 1, (byte) 2));
        assertNull(Array.box((byte[]) null));
        assertArrayEquals(new Byte[] { 2, 3 }, Array.box(new byte[] { 1, 2, 3, 4 }, 1, 3));
        assertNull(Array.box((byte[]) null, 0, 4));
    }

    @Test
    public void testBox_short() {
        assertArrayEquals(new Short[] { 1, 2 }, Array.box((short) 1, (short) 2));
        assertNull(Array.box((short[]) null));
        assertArrayEquals(new Short[] { 2, 3 }, Array.box(new short[] { 1, 2, 3, 4 }, 1, 3));
        assertNull(Array.box((short[]) null, 0, 4));
    }

    @Test
    public void testBox_int() {
        assertArrayEquals(new Integer[] { 1, 2, 3 }, Array.box(1, 2, 3));
        assertNull(Array.box((int[]) null));
        assertSame(CommonUtil.EMPTY_INT_OBJ_ARRAY, Array.box(new int[0]));
        assertArrayEquals(new Integer[] { 2, 3 }, Array.box(new int[] { 1, 2, 3, 4 }, 1, 3));
        assertNull(Array.box((int[]) null, 0, 4));
        assertThrows(IndexOutOfBoundsException.class, () -> Array.box(new int[] { 1 }, 1, 0));
    }

    @Test
    public void testBox_long() {
        assertArrayEquals(new Long[] { 1L, 2L }, Array.box(1L, 2L));
        assertNull(Array.box((long[]) null));
        assertArrayEquals(new Long[] { 2L, 3L }, Array.box(new long[] { 1L, 2L, 3L, 4L }, 1, 3));
        assertNull(Array.box((long[]) null, 0, 4));
    }

    @Test
    public void testBox_float() {
        assertArrayEquals(new Float[] { 1.1f, 2.2f }, Array.box(1.1f, 2.2f));
        assertNull(Array.box((float[]) null));
        assertArrayEquals(new Float[] { 2.2f, 3.3f }, Array.box(new float[] { 1.1f, 2.2f, 3.3f, 4.4f }, 1, 3));
        assertNull(Array.box((float[]) null, 0, 4));
    }

    @Test
    public void testBox_double() {
        assertArrayEquals(new Double[] { 1.1, 2.2 }, Array.box(1.1, 2.2));
        assertNull(Array.box((double[]) null));
        assertArrayEquals(new Double[] { 2.2, 3.3 }, Array.box(new double[] { 1.1, 2.2, 3.3, 4.4 }, 1, 3));
        assertNull(Array.box((double[]) null, 0, 4));
    }

    @Test
    public void testBox_2D() {
        assertArrayEquals(new Boolean[][] { { true, false }, { true } }, Array.box(new boolean[][] { { true, false }, { true } }));
        assertArrayEquals(new Character[][] { { 'a', 'b' }, { 'c' } }, Array.box(new char[][] { { 'a', 'b' }, { 'c' } }));
        assertArrayEquals(new Byte[][] { { 1, 2 }, { 3 } }, Array.box(new byte[][] { { 1, 2 }, { 3 } }));
        assertArrayEquals(new Short[][] { { 1, 2 }, { 3 } }, Array.box(new short[][] { { 1, 2 }, { 3 } }));
        assertArrayEquals(new Integer[][] { { 1, 2 }, { 3 } }, Array.box(new int[][] { { 1, 2 }, { 3 } }));
        assertArrayEquals(new Long[][] { { 1L, 2L }, { 3L } }, Array.box(new long[][] { { 1L, 2L }, { 3L } }));
        assertArrayEquals(new Float[][] { { 1f, 2f }, { 3f } }, Array.box(new float[][] { { 1f, 2f }, { 3f } }));
        assertArrayEquals(new Double[][] { { 1d, 2d }, { 3d } }, Array.box(new double[][] { { 1d, 2d }, { 3d } }));

        assertNull(Array.box((int[][]) null));
        assertArrayEquals(new Integer[][] { { 1 }, null }, Array.box(new int[][] { { 1 }, null }));
        assertEquals(0, Array.box(new int[0][]).length);
    }

    @Test
    public void testBox_3D() {
        assertArrayEquals(new Boolean[][][] { { { true } } }, Array.box(new boolean[][][] { { { true } } }));
        assertArrayEquals(new Character[][][] { { { 'a', 'b' } } }, Array.box(new char[][][] { { { 'a', 'b' } } }));
        assertArrayEquals(new Byte[][][] { { { 1, 2 } } }, Array.box(new byte[][][] { { { 1, 2 } } }));
        assertArrayEquals(new Short[][][] { { { 1, 2 } } }, Array.box(new short[][][] { { { 1, 2 } } }));
        assertArrayEquals(new Integer[][][] { { { 1, 2 } } }, Array.box(new int[][][] { { { 1, 2 } } }));
        assertArrayEquals(new Long[][][] { { { 1L, 2L } } }, Array.box(new long[][][] { { { 1L, 2L } } }));
        assertArrayEquals(new Float[][][] { { { 1f, 2f } } }, Array.box(new float[][][] { { { 1f, 2f } } }));
        assertArrayEquals(new Double[][][] { { { 1d, 2d } } }, Array.box(new double[][][] { { { 1d, 2d } } }));

        assertNull(Array.box((int[][][]) null));
        assertArrayEquals(new Integer[][][] { { { 1 }, null }, null }, Array.box(new int[][][] { { { 1 }, null }, null }));
    }
}

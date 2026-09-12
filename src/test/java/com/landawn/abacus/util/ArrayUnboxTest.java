package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

public class ArrayUnboxTest extends ArrayTestSupport {

    @Test
    public void testUnbox_boolean() {
        assertArrayEquals(new boolean[] { true, false }, Array.unbox(Boolean.TRUE, Boolean.FALSE));
        assertNull(Array.unbox((Boolean[]) null));
        assertSame(CommonUtil.EMPTY_BOOLEAN_ARRAY, Array.unbox(new Boolean[0]));
        assertArrayEquals(new boolean[] { true, false, false }, Array.unbox(new Boolean[] { true, null, false }, false));
        assertArrayEquals(new boolean[] { false, true }, Array.unbox(new Boolean[] { true, false, true, false }, 1, 3, true));
        assertNull(Array.unbox((Boolean[]) null, false));
        assertNull(Array.unbox((Boolean[]) null, 0, 4, false));
        assertThrows(IndexOutOfBoundsException.class, () -> Array.unbox(new Boolean[] { true }, 0, 2, false));
        assertArrayEquals(new boolean[] { true, false }, Array.unbox(new Boolean[] { true, null }, false));
    }

    @Test
    public void testUnbox_char() {
        assertArrayEquals(new char[] { 'a', 'b' }, Array.unbox('a', 'b'));
        assertNull(Array.unbox((Character[]) null));
        assertArrayEquals(new char[] { 'a', 'x', 'c' }, Array.unbox(new Character[] { 'a', null, 'c' }, 'x'));
        assertArrayEquals(new char[] { 'b', 'c' }, Array.unbox(new Character[] { 'a', 'b', 'c', 'd' }, 1, 3, '?'));
        assertNull(Array.unbox((Character[]) null, 0, 4, '?'));
        assertThrows(IndexOutOfBoundsException.class, () -> Array.unbox(new Character[] { 'a' }, 0, 2, '?'));
    }

    @Test
    public void testUnbox_byte() {
        assertArrayEquals(new byte[] { 1, 2 }, Array.unbox((byte) 1, (byte) 2));
        assertNull(Array.unbox((Byte[]) null));
        assertArrayEquals(new byte[] { 1, 0, 3 }, Array.unbox(new Byte[] { 1, null, 3 }, (byte) 0));
        assertArrayEquals(new byte[] { 2, 3 }, Array.unbox(new Byte[] { 1, 2, 3, 4 }, 1, 3, (byte) 0));
        assertNull(Array.unbox((Byte[]) null, 0, 4, (byte) 0));
    }

    @Test
    public void testUnbox_short() {
        assertArrayEquals(new short[] { 1, 2 }, Array.unbox((short) 1, (short) 2));
        assertNull(Array.unbox((Short[]) null));
        assertArrayEquals(new short[] { 1, -1, 3 }, Array.unbox(new Short[] { 1, null, 3 }, (short) -1));
        assertArrayEquals(new short[] { 2, 3 }, Array.unbox(new Short[] { 1, 2, 3, 4 }, 1, 3, (short) 0));
        assertNull(Array.unbox((Short[]) null, 0, 4, (short) 0));
    }

    @Test
    public void testUnbox_int() {
        assertArrayEquals(new int[] { 1, 2, 3 }, Array.unbox(1, 2, 3));
        assertNull(Array.unbox((Integer[]) null));
        assertSame(CommonUtil.EMPTY_INT_ARRAY, Array.unbox(new Integer[0]));
        assertArrayEquals(new int[] { 1, -1, 3 }, Array.unbox(new Integer[] { 1, null, 3 }, -1));
        assertArrayEquals(new int[] { 2, 3 }, Array.unbox(new Integer[] { 1, 2, 3, 4 }, 1, 3, 0));
        assertNull(Array.unbox((Integer[]) null, 0));
        assertNull(Array.unbox((Integer[]) null, 0, 4, 0));
        assertThrows(IndexOutOfBoundsException.class, () -> Array.unbox(new Integer[] { 1 }, 1, 0, 0));
        assertArrayEquals(new int[] { 1, 0 }, Array.unbox(new Integer[] { 1, null }, 0));
    }

    @Test
    public void testUnbox_long() {
        assertArrayEquals(new long[] { 1L, 2L }, Array.unbox(1L, 2L));
        assertNull(Array.unbox((Long[]) null));
        assertArrayEquals(new long[] { 1L, 0L, 3L }, Array.unbox(new Long[] { 1L, null, 3L }, 0L));
        assertArrayEquals(new long[] { 2L, 3L }, Array.unbox(new Long[] { 1L, 2L, 3L, 4L }, 1, 3, 0L));
        assertNull(Array.unbox((Long[]) null, 0, 4, 0L));
    }

    @Test
    public void testUnbox_float() {
        assertArrayEquals(new float[] { 1.1f, 2.2f }, Array.unbox(1.1f, 2.2f));
        assertNull(Array.unbox((Float[]) null));
        assertArrayEquals(new float[] { 1f, 0f, 3f }, Array.unbox(new Float[] { 1f, null, 3f }, 0f));
        assertArrayEquals(new float[] { 2.2f, 3.3f }, Array.unbox(new Float[] { 1.1f, 2.2f, 3.3f, 4.4f }, 1, 3, 0f));
        assertNull(Array.unbox((Float[]) null, 0, 4, 0f));
    }

    @Test
    public void testUnbox_double() {
        assertArrayEquals(new double[] { 1.1, 2.2 }, Array.unbox(1.1, 2.2));
        assertNull(Array.unbox((Double[]) null));
        assertArrayEquals(new double[] { 1d, 0d, 3d }, Array.unbox(new Double[] { 1d, null, 3d }, 0d));
        assertArrayEquals(new double[] { 2.2, 3.3 }, Array.unbox(new Double[] { 1.1, 2.2, 3.3, 4.4 }, 1, 3, 0d));
        assertNull(Array.unbox((Double[]) null, 0, 4, 0d));
    }

    @Test
    public void testUnbox_2D() {
        assertArrayEquals(new boolean[][] { { true, false }, { true } }, Array.unbox(new Boolean[][] { { true, false }, { true } }));
        assertArrayEquals(new char[][] { { 'a', 'b' }, { 'c' } }, Array.unbox(new Character[][] { { 'a', 'b' }, { 'c' } }));
        assertArrayEquals(new byte[][] { { 1, 2 }, { 3 } }, Array.unbox(new Byte[][] { { 1, 2 }, { 3 } }));
        assertArrayEquals(new short[][] { { 1, 2 }, { 3 } }, Array.unbox(new Short[][] { { 1, 2 }, { 3 } }));
        assertArrayEquals(new int[][] { { 1, 2 }, { 3 } }, Array.unbox(new Integer[][] { { 1, 2 }, { 3 } }));
        assertArrayEquals(new long[][] { { 1L, 2L }, { 3L } }, Array.unbox(new Long[][] { { 1L, 2L }, { 3L } }));
        assertArrayEquals(new float[][] { { 1f, 2f }, { 3f } }, Array.unbox(new Float[][] { { 1f, 2f }, { 3f } }));
        assertArrayEquals(new double[][] { { 1d, 2d }, { 3d } }, Array.unbox(new Double[][] { { 1d, 2d }, { 3d } }));

        assertNull(Array.unbox((Integer[][]) null));
        assertArrayEquals(new int[][] { { 1 }, null }, Array.unbox(new Integer[][] { { 1 }, null }));
        assertArrayEquals(new int[][] { { 1, -1 }, null }, Array.unbox(new Integer[][] { { 1, null }, null }, -1));
        assertEquals(0, Array.unbox(new Integer[0][]).length);
    }

    @Test
    public void testUnbox_3D() {
        assertArrayEquals(new boolean[][][] { { { true } } }, Array.unbox(new Boolean[][][] { { { true } } }));
        assertArrayEquals(new char[][][] { { { 'a', 'b' } } }, Array.unbox(new Character[][][] { { { 'a', 'b' } } }));
        assertArrayEquals(new byte[][][] { { { 1, 2 } } }, Array.unbox(new Byte[][][] { { { 1, 2 } } }));
        assertArrayEquals(new short[][][] { { { 1, 2 } } }, Array.unbox(new Short[][][] { { { 1, 2 } } }));
        assertArrayEquals(new int[][][] { { { 1, 2 } } }, Array.unbox(new Integer[][][] { { { 1, 2 } } }));
        assertArrayEquals(new long[][][] { { { 1L, 2L } } }, Array.unbox(new Long[][][] { { { 1L, 2L } } }));
        assertArrayEquals(new float[][][] { { { 1f, 2f } } }, Array.unbox(new Float[][][] { { { 1f, 2f } } }));
        assertArrayEquals(new double[][][] { { { 1d, 2d } } }, Array.unbox(new Double[][][] { { { 1d, 2d } } }));

        assertNull(Array.unbox((Integer[][][]) null));
        assertArrayEquals(new int[][][] { { { 1 }, null }, null }, Array.unbox(new Integer[][][] { { { 1 }, null }, null }));
        assertArrayEquals(new int[][][] { { { 1, -1 } }, null }, Array.unbox(new Integer[][][] { { { 1, null } }, null }, -1));
    }
}

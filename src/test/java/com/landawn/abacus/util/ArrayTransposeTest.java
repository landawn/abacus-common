package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class ArrayTransposeTest extends ArrayTestSupport {

    @Test
    public void testTranspose_boolean() {
        assertArrayEquals(new boolean[][] { { true, false }, { false, true }, { true, false } },
                Array.transpose(new boolean[][] { { true, false, true }, { false, true, false } }));
        assertNull(Array.transpose((boolean[][]) null));
        assertEquals(0, Array.transpose(new boolean[0][]).length);
        assertThrows(IllegalArgumentException.class, () -> Array.transpose(new boolean[][] { { true }, null }));
        assertThrows(IllegalArgumentException.class, () -> Array.transpose(new boolean[][] { { true, false }, { true } }));
    }

    @Test
    public void testTranspose_char() {
        assertArrayEquals(new char[][] { { 'a', 'd' }, { 'b', 'e' }, { 'c', 'f' } }, Array.transpose(new char[][] { { 'a', 'b', 'c' }, { 'd', 'e', 'f' } }));
        assertNull(Array.transpose((char[][]) null));
        assertThrows(IllegalArgumentException.class, () -> Array.transpose(new char[][] { { 'a' }, null }));
    }

    @Test
    public void testTranspose_byte() {
        assertArrayEquals(new byte[][] { { 1, 4 }, { 2, 5 }, { 3, 6 } }, Array.transpose(new byte[][] { { 1, 2, 3 }, { 4, 5, 6 } }));
        assertNull(Array.transpose((byte[][]) null));
        assertThrows(IllegalArgumentException.class, () -> Array.transpose(new byte[][] { { 1, 2 }, { 3 } }));
    }

    @Test
    public void testTranspose_short() {
        assertArrayEquals(new short[][] { { 1, 4 }, { 2, 5 }, { 3, 6 } }, Array.transpose(new short[][] { { 1, 2, 3 }, { 4, 5, 6 } }));
        assertNull(Array.transpose((short[][]) null));
        assertThrows(IllegalArgumentException.class, () -> Array.transpose(new short[][] { { 1 }, null }));
    }

    @Test
    public void testTranspose_int() {
        final int[][] a = { { 1, 2, 3 }, { 4, 5, 6 } };
        assertArrayEquals(new int[][] { { 1, 4 }, { 2, 5 }, { 3, 6 } }, Array.transpose(a));
        assertArrayEquals(new int[][] { { 1, 2, 3 }, { 4, 5, 6 } }, a);
        assertNull(Array.transpose((int[][]) null));
        assertEquals(0, Array.transpose(new int[0][]).length);

        final IllegalArgumentException nullRow = assertThrows(IllegalArgumentException.class, () -> Array.transpose(new int[][] { { 1 }, null }));
        assertTrue(nullRow.getMessage().contains("index 1"), nullRow.getMessage());
        final IllegalArgumentException firstNull = assertThrows(IllegalArgumentException.class, () -> Array.transpose(new int[][] { null, { 1 } }));
        assertTrue(firstNull.getMessage().contains("index 0"), firstNull.getMessage());
        final IllegalArgumentException ragged = assertThrows(IllegalArgumentException.class, () -> Array.transpose(new int[][] { { 1, 2 }, { 3 } }));
        assertTrue(ragged.getMessage().contains("index 1"), ragged.getMessage());
        assertTrue(ragged.getMessage().contains("length 1"), ragged.getMessage());
    }

    @Test
    public void testTranspose_long() {
        assertArrayEquals(new long[][] { { 1L, 4L }, { 2L, 5L }, { 3L, 6L } }, Array.transpose(new long[][] { { 1L, 2L, 3L }, { 4L, 5L, 6L } }));
        assertNull(Array.transpose((long[][]) null));
        assertThrows(IllegalArgumentException.class, () -> Array.transpose(new long[][] { { 1L, 2L }, { 3L } }));
    }

    @Test
    public void testTranspose_float() {
        assertArrayEquals(new float[][] { { 1f, 4f }, { 2f, 5f } }, Array.transpose(new float[][] { { 1f, 2f }, { 4f, 5f } }));
        assertNull(Array.transpose((float[][]) null));
        assertThrows(IllegalArgumentException.class, () -> Array.transpose(new float[][] { { 1f }, null }));
    }

    @Test
    public void testTranspose_double() {
        assertArrayEquals(new double[][] { { 1d, 4d }, { 2d, 5d }, { 3d, 6d } }, Array.transpose(new double[][] { { 1d, 2d, 3d }, { 4d, 5d, 6d } }));
        assertNull(Array.transpose((double[][]) null));
        assertThrows(IllegalArgumentException.class, () -> Array.transpose(new double[][] { { 1d, 2d }, { 3d } }));
    }

    @Test
    public void testTranspose_Object() {
        assertArrayEquals(new String[][] { { "A", "D" }, { "B", "E" }, { "C", "F" } },
                Array.transpose(new String[][] { { "A", "B", "C" }, { "D", "E", "F" } }));
        assertNull(Array.transpose((String[][]) null));
        assertEquals(0, Array.transpose(new String[0][]).length);
        assertThrows(IllegalArgumentException.class, () -> Array.transpose(new String[][] { { "a" }, null }));
        assertThrows(IllegalArgumentException.class, () -> Array.transpose(new String[][] { { "a", "b" }, { "c" } }));
    }
}

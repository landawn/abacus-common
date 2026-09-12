package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

public class ArrayConcatTest extends ArrayTestSupport {

    @Test
    public void testConcat_boolean_2D() {
        assertArrayEquals(new boolean[][] { { true, false }, { true } }, Array.concat(new boolean[][] { { true } }, new boolean[][] { { false }, { true } }));
        assertArrayEquals(new boolean[][] { { true }, {} }, Array.concat(new boolean[][] { { true }, null }, (boolean[][]) null));
        assertArrayEquals(new boolean[][] { { true }, {} }, Array.concat((boolean[][]) null, new boolean[][] { { true }, null }));
        assertArrayEquals(new boolean[][] { {} }, Array.concat(new boolean[][] { null }, new boolean[0][]));
        assertEquals(0, Array.concat((boolean[][]) null, (boolean[][]) null).length);
    }

    @Test
    public void testConcat_char_2D() {
        assertArrayEquals(new char[][] { { 'a', 'b' }, { 'c' } }, Array.concat(new char[][] { { 'a' } }, new char[][] { { 'b' }, { 'c' } }));
        assertArrayEquals(new char[][] { {} }, Array.concat(new char[][] { null }, new char[0][]));
        assertEquals(0, Array.concat((char[][]) null, (char[][]) null).length);
    }

    @Test
    public void testConcat_byte_2D() {
        assertArrayEquals(new byte[][] { { 1, 2 }, { 3 } }, Array.concat(new byte[][] { { 1 } }, new byte[][] { { 2 }, { 3 } }));
        assertArrayEquals(new byte[][] { {} }, Array.concat(new byte[][] { null }, new byte[0][]));
    }

    @Test
    public void testConcat_short_2D() {
        assertArrayEquals(new short[][] { { 1, 2 }, { 3 } }, Array.concat(new short[][] { { 1 } }, new short[][] { { 2 }, { 3 } }));
        assertArrayEquals(new short[][] { {} }, Array.concat(new short[][] { null }, new short[0][]));
    }

    @Test
    public void testConcat_int_2D() {
        final int[][] a = { { 1 }, { 2 } };
        final int[][] b = { { 3 } };
        final int[][] result = Array.concat(a, b);
        assertArrayEquals(new int[][] { { 1, 3 }, { 2 } }, result);
        a[0][0] = 9;
        assertEquals(1, result[0][0]);
        assertArrayEquals(new int[][] { {} }, Array.concat(new int[][] { null }, new int[0][]));
        assertEquals(0, Array.concat((int[][]) null, (int[][]) null).length);
    }

    @Test
    public void testConcat_long_2D() {
        assertArrayEquals(new long[][] { { 1L, 2L }, { 3L } }, Array.concat(new long[][] { { 1L } }, new long[][] { { 2L }, { 3L } }));
        assertArrayEquals(new long[][] { {} }, Array.concat(new long[][] { null }, new long[0][]));
    }

    @Test
    public void testConcat_float_2D() {
        assertArrayEquals(new float[][] { { 1f, 2f }, { 3f } }, Array.concat(new float[][] { { 1f } }, new float[][] { { 2f }, { 3f } }));
        assertArrayEquals(new float[][] { {} }, Array.concat(new float[][] { null }, new float[0][]));
    }

    @Test
    public void testConcat_double_2D() {
        assertArrayEquals(new double[][] { { 1d, 2d }, { 3d } }, Array.concat(new double[][] { { 1d } }, new double[][] { { 2d }, { 3d } }));
        assertArrayEquals(new double[][] { {} }, Array.concat(new double[][] { null }, new double[0][]));
    }

    @Test
    public void testConcat_boolean_3D() {
        assertArrayEquals(new boolean[][][] { { { true, false } } }, Array.concat(new boolean[][][] { { { true } } }, new boolean[][][] { { { false } } }));
        assertArrayEquals(new boolean[][][] { {} }, Array.concat(new boolean[][][] { null }, new boolean[0][][]));
        assertEquals(0, Array.concat((boolean[][][]) null, (boolean[][][]) null).length);
    }

    @Test
    public void testConcat_char_3D() {
        assertArrayEquals(new char[][][] { { { 'a', 'b' } } }, Array.concat(new char[][][] { { { 'a' } } }, new char[][][] { { { 'b' } } }));
        assertArrayEquals(new char[][][] { {} }, Array.concat(new char[][][] { null }, new char[0][][]));
    }

    @Test
    public void testConcat_byte_3D() {
        assertArrayEquals(new byte[][][] { { { 1, 2 } } }, Array.concat(new byte[][][] { { { 1 } } }, new byte[][][] { { { 2 } } }));
        assertArrayEquals(new byte[][][] { {} }, Array.concat(new byte[][][] { null }, new byte[0][][]));
    }

    @Test
    public void testConcat_short_3D() {
        assertArrayEquals(new short[][][] { { { 1, 2 } } }, Array.concat(new short[][][] { { { 1 } } }, new short[][][] { { { 2 } } }));
        assertArrayEquals(new short[][][] { {} }, Array.concat(new short[][][] { null }, new short[0][][]));
    }

    @Test
    public void testConcat_int_3D() {
        final int[][][] a = { { { 1 } } };
        final int[][][] b = { { { 2 } } };
        final int[][][] result = Array.concat(a, b);
        assertArrayEquals(new int[][][] { { { 1, 2 } } }, result);
        a[0][0][0] = 9;
        assertEquals(1, result[0][0][0]);
        assertArrayEquals(new int[][][] { {} }, Array.concat(new int[][][] { null }, new int[0][][]));
    }

    @Test
    public void testConcat_long_3D() {
        assertArrayEquals(new long[][][] { { { 1L, 2L } } }, Array.concat(new long[][][] { { { 1L } } }, new long[][][] { { { 2L } } }));
        assertArrayEquals(new long[][][] { {} }, Array.concat(new long[][][] { null }, new long[0][][]));
    }

    @Test
    public void testConcat_float_3D() {
        assertArrayEquals(new float[][][] { { { 1f, 2f } } }, Array.concat(new float[][][] { { { 1f } } }, new float[][][] { { { 2f } } }));
        assertArrayEquals(new float[][][] { {} }, Array.concat(new float[][][] { null }, new float[0][][]));
    }

    @Test
    public void testConcat_double_3D() {
        assertArrayEquals(new double[][][] { { { 1d, 2d } } }, Array.concat(new double[][][] { { { 1d } } }, new double[][][] { { { 2d } } }));
        assertArrayEquals(new double[][][] { {} }, Array.concat(new double[][][] { null }, new double[0][][]));
    }

    @Test
    public void testConcat2D() {
        final String[][] a = { { "a" }, { "b" } };
        final String[][] b = { { "c" } };
        assertArrayEquals(new String[][] { { "a", "c" }, { "b" } }, Array.concat2D(a, b));
        assertNull(Array.concat2D((String[][]) null, (String[][]) null));
        assertArrayEquals(new String[][] { { "a" }, null }, Array.concat2D(new String[][] { { "a" }, null }, (String[][]) null));
        assertArrayEquals(new String[][] { { "a" }, null }, Array.concat2D((String[][]) null, new String[][] { { "a" }, null }));
        assertArrayEquals(new String[0][], Array.concat2D(new String[0][], (String[][]) null));
        final String[][] result = Array.concat2D(a, b);
        assertNotSame(a, result);
        a[0][0] = "z";
        assertEquals("a", result[0][0]);
    }

    @Test
    public void testConcat3D() {
        final String[][][] a = { { { "a" } } };
        final String[][][] b = { { { "b" } } };
        assertArrayEquals(new String[][][] { { { "a", "b" } } }, Array.concat3D(a, b));
        assertNull(Array.concat3D((String[][][]) null, (String[][][]) null));
        assertArrayEquals(new String[][][] { { { "a" }, null }, null }, Array.concat3D(new String[][][] { { { "a" }, null }, null }, (String[][][]) null));
        final String[][][] result = Array.concat3D(a, b);
        a[0][0][0] = "z";
        assertEquals("a", result[0][0][0]);
    }

    @Test
    public void testConcat_RowPairLengthBound() {
        // Contract pin for the @throws IllegalArgumentException added to the concat family: the bounded quantity
        // is a corresponding ROW PAIR combined length. Tripping the guard itself needs two rows summing past
        // Integer.MAX_VALUE (~2 GiB of live arrays), which is out of reach for this suite, so only the unit the
        // new clause names is pinned here.
        final byte[][] r2 = Array.concat(new byte[][] { new byte[3], new byte[1] }, new byte[][] { new byte[4], new byte[0] });
        assertEquals(2, r2.length);
        assertEquals(7, r2[0].length);
        assertEquals(1, r2[1].length);

        final byte[][][] r3 = Array.concat(new byte[][][] { { new byte[3] } }, new byte[][][] { { new byte[4] } });
        assertEquals(1, r3.length);
        assertEquals(1, r3[0].length);
        assertEquals(7, r3[0][0].length);

        assertEquals(3, Array.concat2D(new String[][] { { "a", "b" } }, new String[][] { { "c" } })[0].length);
        assertEquals(3, Array.concat3D(new String[][][] { { { "a", "b" } } }, new String[][][] { { { "c" } } })[0][0].length);
    }

    @Test
    public void testConcat2D_NullRowRationale() {
        // Contract pin for the corrected rationale: a result row is null exactly when both (possibly missing)
        // input rows are null - and the row component type IS available at such a row, as the result own
        // runtime type shows, so the null row is a deliberate mirror of the inputs rather than a fallback.
        final String[][] bothNull = Array.concat2D(new String[][] { null }, new String[][] { null });
        assertEquals(1, bothNull.length);
        assertNull(bothNull[0]);
        assertEquals(String[].class, bothNull.getClass().getComponentType());
        assertEquals(String.class, bothNull.getClass().getComponentType().getComponentType());

        assertArrayEquals(new String[][] { { "x" } }, Array.concat2D(new String[][] { null }, new String[][] { { "x" } }));
        assertArrayEquals(new String[][] { { "x" } }, Array.concat2D(new String[][] { { "x" } }, new String[][] { null }));
        assertArrayEquals(new String[][] { { "x" }, null }, Array.concat2D(new String[][] { { "x" } }, new String[][] { new String[0], null }));

        assertNull(N.concat((String[]) null, (String[]) null));

        // the primitive sibling normalises the same row to an empty one
        assertArrayEquals(new boolean[][] { {} }, Array.concat(new boolean[][] { null }, new boolean[][] { null }));
    }

    @Test
    public void testConcat2D_NullToEmptyRemedy() {
        // Contract pin for the remedy the javadoc now recommends: nullToEmpty(row, rowArrayClass) yields a real
        // String[0], while the no-Class overload is an Object[] and so cannot stand in for a T[] row.
        final String[][] withNullRow = Array.concat2D(new String[][] { { "a" }, null }, (String[][]) null);
        assertNull(withNullRow[1]);

        final String[] fixed = N.nullToEmpty(withNullRow[1], String[].class);
        assertEquals(0, fixed.length);
        assertEquals(String.class, fixed.getClass().getComponentType());

        assertEquals(Object.class, N.nullToEmpty((Object[]) null).getClass().getComponentType());
    }
}

package com.landawn.abacus.util;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

public class Array102Test extends TestBase {

    // Tests for box(byte[][][])
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
        byte[][][] input = {
            {{1, 2}, {3, 4}},
            {{5, 6}, {7, 8}}
        };
        Byte[][][] result = Array.box(input);
        
        Assertions.assertNotNull(result);
        Assertions.assertEquals(2, result.length);
        Assertions.assertEquals(2, result[0].length);
        Assertions.assertEquals(2, result[0][0].length);
        
        Assertions.assertEquals(Byte.valueOf((byte)1), result[0][0][0]);
        Assertions.assertEquals(Byte.valueOf((byte)2), result[0][0][1]);
        Assertions.assertEquals(Byte.valueOf((byte)3), result[0][1][0]);
        Assertions.assertEquals(Byte.valueOf((byte)4), result[0][1][1]);
        Assertions.assertEquals(Byte.valueOf((byte)5), result[1][0][0]);
        Assertions.assertEquals(Byte.valueOf((byte)6), result[1][0][1]);
        Assertions.assertEquals(Byte.valueOf((byte)7), result[1][1][0]);
        Assertions.assertEquals(Byte.valueOf((byte)8), result[1][1][1]);
    }

    // Tests for box(short[][][])
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
        short[][][] input = {
            {{10, 20}, {30, 40}},
            {{50, 60}, {70, 80}}
        };
        Short[][][] result = Array.box(input);
        
        Assertions.assertNotNull(result);
        Assertions.assertEquals(2, result.length);
        Assertions.assertEquals(Short.valueOf((short)10), result[0][0][0]);
        Assertions.assertEquals(Short.valueOf((short)80), result[1][1][1]);
    }

    // Tests for box(int[][][])
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
        int[][][] input = {
            {{100, 200}, {300, 400}},
            {{500, 600}, {700, 800}}
        };
        Integer[][][] result = Array.box(input);
        
        Assertions.assertNotNull(result);
        Assertions.assertEquals(2, result.length);
        Assertions.assertEquals(Integer.valueOf(100), result[0][0][0]);
        Assertions.assertEquals(Integer.valueOf(800), result[1][1][1]);
    }

    // Tests for box(long[][][])
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
        long[][][] input = {
            {{1000L, 2000L}, {3000L, 4000L}},
            {{5000L, 6000L}, {7000L, 8000L}}
        };
        Long[][][] result = Array.box(input);
        
        Assertions.assertNotNull(result);
        Assertions.assertEquals(2, result.length);
        Assertions.assertEquals(Long.valueOf(1000L), result[0][0][0]);
        Assertions.assertEquals(Long.valueOf(8000L), result[1][1][1]);
    }

    // Tests for box(float[][][])
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
        float[][][] input = {
            {{1.1f, 2.2f}, {3.3f, 4.4f}},
            {{5.5f, 6.6f}, {7.7f, 8.8f}}
        };
        Float[][][] result = Array.box(input);
        
        Assertions.assertNotNull(result);
        Assertions.assertEquals(2, result.length);
        Assertions.assertEquals(Float.valueOf(1.1f), result[0][0][0]);
        Assertions.assertEquals(Float.valueOf(8.8f), result[1][1][1]);
    }

    // Tests for box(double[][][])
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
        double[][][] input = {
            {{1.11, 2.22}, {3.33, 4.44}},
            {{5.55, 6.66}, {7.77, 8.88}}
        };
        Double[][][] result = Array.box(input);
        
        Assertions.assertNotNull(result);
        Assertions.assertEquals(2, result.length);
        Assertions.assertEquals(Double.valueOf(1.11), result[0][0][0]);
        Assertions.assertEquals(Double.valueOf(8.88), result[1][1][1]);
    }

    // Tests for unbox(Boolean...)
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
        Boolean[] input = {true, null, false, null};
        boolean[] result = Array.unbox(input);
        Assertions.assertNotNull(result);
        Assertions.assertEquals(4, result.length);
        Assertions.assertTrue(result[0]);
        Assertions.assertFalse(result[1]); // null becomes false
        Assertions.assertFalse(result[2]);
        Assertions.assertFalse(result[3]); // null becomes false
    }

    @Test
    public void testUnboxBooleanVarargs_ValidArray() {
        Boolean[] input = {true, false, true};
        boolean[] result = Array.unbox(input);
        Assertions.assertNotNull(result);
        Assertions.assertEquals(3, result.length);
        Assertions.assertTrue(result[0]);
        Assertions.assertFalse(result[1]);
        Assertions.assertTrue(result[2]);
    }

    // Tests for unbox(Boolean[], boolean)
    @Test
    public void testUnboxBooleanWithDefault_NullInput() {
        Boolean[] input = null;
        boolean[] result = Array.unbox(input, true);
        Assertions.assertNull(result);
    }

    @Test
    public void testUnboxBooleanWithDefault_WithNullValues() {
        Boolean[] input = {true, null, false, null};
        boolean[] result = Array.unbox(input, true);
        Assertions.assertNotNull(result);
        Assertions.assertEquals(4, result.length);
        Assertions.assertTrue(result[0]);
        Assertions.assertTrue(result[1]); // null becomes true (valueForNull)
        Assertions.assertFalse(result[2]);
        Assertions.assertTrue(result[3]); // null becomes true (valueForNull)
    }

    // Tests for unbox(Boolean[], int, int, boolean)
    @Test
    public void testUnboxBooleanRange_NullInput() {
        Boolean[] input = null;
        boolean[] result = Array.unbox(input, 0, 0, false);
        Assertions.assertNull(result);
    }

    @Test
    public void testUnboxBooleanRange_EmptyRange() {
        Boolean[] input = {true, false, true};
        boolean[] result = Array.unbox(input, 1, 1, false);
        Assertions.assertNotNull(result);
        Assertions.assertEquals(0, result.length);
    }

    @Test
    public void testUnboxBooleanRange_ValidRange() {
        Boolean[] input = {true, null, false, true, null};
        boolean[] result = Array.unbox(input, 1, 4, true);
        Assertions.assertNotNull(result);
        Assertions.assertEquals(3, result.length);
        Assertions.assertTrue(result[0]); // null becomes true
        Assertions.assertFalse(result[1]);
        Assertions.assertTrue(result[2]);
    }

    @Test
    public void testUnboxBooleanRange_InvalidRange() {
        Boolean[] input = {true, false};
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> {
            Array.unbox(input, 0, 3, false);
        });
    }

    // Tests for unbox(Character...)
    @Test
    public void testUnboxCharacterVarargs_NullInput() {
        Character[] input = null;
        char[] result = Array.unbox(input);
        Assertions.assertNull(result);
    }

    @Test
    public void testUnboxCharacterVarargs_WithNullValues() {
        Character[] input = {'a', null, 'b'};
        char[] result = Array.unbox(input);
        Assertions.assertNotNull(result);
        Assertions.assertEquals(3, result.length);
        Assertions.assertEquals('a', result[0]);
        Assertions.assertEquals((char)0, result[1]); // null becomes 0
        Assertions.assertEquals('b', result[2]);
    }

    // Tests for unbox(Character[], char)
    @Test
    public void testUnboxCharacterWithDefault_WithNullValues() {
        Character[] input = {'x', null, 'y'};
        char[] result = Array.unbox(input, '?');
        Assertions.assertNotNull(result);
        Assertions.assertEquals(3, result.length);
        Assertions.assertEquals('x', result[0]);
        Assertions.assertEquals('?', result[1]); // null becomes '?'
        Assertions.assertEquals('y', result[2]);
    }

    // Tests for unbox(Character[], int, int, char)
    @Test
    public void testUnboxCharacterRange_ValidRange() {
        Character[] input = {'a', 'b', null, 'd', 'e'};
        char[] result = Array.unbox(input, 1, 4, '*');
        Assertions.assertNotNull(result);
        Assertions.assertEquals(3, result.length);
        Assertions.assertEquals('b', result[0]);
        Assertions.assertEquals('*', result[1]); // null becomes '*'
        Assertions.assertEquals('d', result[2]);
    }

    // Tests for unbox(Byte...)
    @Test
    public void testUnboxByteVarargs_NullInput() {
        Byte[] input = null;
        byte[] result = Array.unbox(input);
        Assertions.assertNull(result);
    }

    @Test
    public void testUnboxByteVarargs_WithNullValues() {
        Byte[] input = {1, null, 3};
        byte[] result = Array.unbox(input);
        Assertions.assertNotNull(result);
        Assertions.assertEquals(3, result.length);
        Assertions.assertEquals((byte)1, result[0]);
        Assertions.assertEquals((byte)0, result[1]); // null becomes 0
        Assertions.assertEquals((byte)3, result[2]);
    }

    // Tests for unbox(Byte[], byte)
    @Test
    public void testUnboxByteWithDefault_WithNullValues() {
        Byte[] input = {10, null, 30};
        byte[] result = Array.unbox(input, (byte) 99);
        Assertions.assertNotNull(result);
        Assertions.assertEquals(3, result.length);
        Assertions.assertEquals((byte)10, result[0]);
        Assertions.assertEquals((byte)99, result[1]); // null becomes 99
        Assertions.assertEquals((byte)30, result[2]);
    }

    // Tests for unbox(Byte[], int, int, byte)
    @Test
    public void testUnboxByteRange_ValidRange() {
        Byte[] input = {1, 2, null, 4, 5};
        byte[] result = Array.unbox(input, 1, 4, (byte) 77);
        Assertions.assertNotNull(result);
        Assertions.assertEquals(3, result.length);
        Assertions.assertEquals((byte)2, result[0]);
        Assertions.assertEquals((byte)77, result[1]); // null becomes 77
        Assertions.assertEquals((byte)4, result[2]);
    }

    // Tests for unbox(Short...)
    @Test
    public void testUnboxShortVarargs_NullInput() {
        Short[] input = null;
        short[] result = Array.unbox(input);
        Assertions.assertNull(result);
    }

    @Test
    public void testUnboxShortVarargs_WithNullValues() {
        Short[] input = {100, null, 300};
        short[] result = Array.unbox(input);
        Assertions.assertNotNull(result);
        Assertions.assertEquals(3, result.length);
        Assertions.assertEquals((short)100, result[0]);
        Assertions.assertEquals((short)0, result[1]); // null becomes 0
        Assertions.assertEquals((short)300, result[2]);
    }

    // Tests for unbox(Short[], short)
    @Test
    public void testUnboxShortWithDefault_WithNullValues() {
        Short[] input = {1000, null, 3000};
        short[] result = Array.unbox(input, (short) 9999);
        Assertions.assertNotNull(result);
        Assertions.assertEquals(3, result.length);
        Assertions.assertEquals((short)1000, result[0]);
        Assertions.assertEquals((short)9999, result[1]); // null becomes 9999
        Assertions.assertEquals((short)3000, result[2]);
    }

    // Tests for unbox(Short[], int, int, short)
    @Test
    public void testUnboxShortRange_ValidRange() {
        Short[] input = {10, 20, null, 40, 50};
        short[] result = Array.unbox(input, 1, 4, (short) 777);
        Assertions.assertNotNull(result);
        Assertions.assertEquals(3, result.length);
        Assertions.assertEquals((short)20, result[0]);
        Assertions.assertEquals((short)777, result[1]); // null becomes 777
        Assertions.assertEquals((short)40, result[2]);
    }

    // Tests for unbox(Integer...)
    @Test
    public void testUnboxIntegerVarargs_NullInput() {
        Integer[] input = null;
        int[] result = Array.unbox(input);
        Assertions.assertNull(result);
    }

    @Test
    public void testUnboxIntegerVarargs_WithNullValues() {
        Integer[] input = {1000, null, 3000};
        int[] result = Array.unbox(input);
        Assertions.assertNotNull(result);
        Assertions.assertEquals(3, result.length);
        Assertions.assertEquals(1000, result[0]);
        Assertions.assertEquals(0, result[1]); // null becomes 0
        Assertions.assertEquals(3000, result[2]);
    }

    // Tests for unbox(Integer[], int)
    @Test
    public void testUnboxIntegerWithDefault_WithNullValues() {
        Integer[] input = {10000, null, 30000};
        int[] result = Array.unbox(input, 99999);
        Assertions.assertNotNull(result);
        Assertions.assertEquals(3, result.length);
        Assertions.assertEquals(10000, result[0]);
        Assertions.assertEquals(99999, result[1]); // null becomes 99999
        Assertions.assertEquals(30000, result[2]);
    }

    // Tests for unbox(Integer[], int, int, int)
    @Test
    public void testUnboxIntegerRange_ValidRange() {
        Integer[] input = {100, 200, null, 400, 500};
        int[] result = Array.unbox(input, 1, 4, 7777);
        Assertions.assertNotNull(result);
        Assertions.assertEquals(3, result.length);
        Assertions.assertEquals(200, result[0]);
        Assertions.assertEquals(7777, result[1]); // null becomes 7777
        Assertions.assertEquals(400, result[2]);
    }

    // Tests for unbox(Long...)
    @Test
    public void testUnboxLongVarargs_NullInput() {
        Long[] input = null;
        long[] result = Array.unbox(input);
        Assertions.assertNull(result);
    }

    @Test
    public void testUnboxLongVarargs_WithNullValues() {
        Long[] input = {100000L, null, 300000L};
        long[] result = Array.unbox(input);
        Assertions.assertNotNull(result);
        Assertions.assertEquals(3, result.length);
        Assertions.assertEquals(100000L, result[0]);
        Assertions.assertEquals(0L, result[1]); // null becomes 0L
        Assertions.assertEquals(300000L, result[2]);
    }

    // Tests for unbox(Long[], long)
    @Test
    public void testUnboxLongWithDefault_WithNullValues() {
        Long[] input = {1000000L, null, 3000000L};
        long[] result = Array.unbox(input, 9999999L);
        Assertions.assertNotNull(result);
        Assertions.assertEquals(3, result.length);
        Assertions.assertEquals(1000000L, result[0]);
        Assertions.assertEquals(9999999L, result[1]); // null becomes 9999999L
        Assertions.assertEquals(3000000L, result[2]);
    }

    // Tests for unbox(Long[], int, int, long)
    @Test
    public void testUnboxLongRange_ValidRange() {
        Long[] input = {1000L, 2000L, null, 4000L, 5000L};
        long[] result = Array.unbox(input, 1, 4, 77777L);
        Assertions.assertNotNull(result);
        Assertions.assertEquals(3, result.length);
        Assertions.assertEquals(2000L, result[0]);
        Assertions.assertEquals(77777L, result[1]); // null becomes 77777L
        Assertions.assertEquals(4000L, result[2]);
    }

    // Tests for unbox(Float...)
    @Test
    public void testUnboxFloatVarargs_NullInput() {
        Float[] input = null;
        float[] result = Array.unbox(input);
        Assertions.assertNull(result);
    }

    @Test
    public void testUnboxFloatVarargs_WithNullValues() {
        Float[] input = {1.1f, null, 3.3f};
        float[] result = Array.unbox(input);
        Assertions.assertNotNull(result);
        Assertions.assertEquals(3, result.length);
        Assertions.assertEquals(1.1f, result[0]);
        Assertions.assertEquals(0f, result[1]); // null becomes 0f
        Assertions.assertEquals(3.3f, result[2]);
    }

    // Tests for unbox(Float[], float)
    @Test
    public void testUnboxFloatWithDefault_WithNullValues() {
        Float[] input = {11.1f, null, 33.3f};
        float[] result = Array.unbox(input, 99.9f);
        Assertions.assertNotNull(result);
        Assertions.assertEquals(3, result.length);
        Assertions.assertEquals(11.1f, result[0]);
        Assertions.assertEquals(99.9f, result[1]); // null becomes 99.9f
        Assertions.assertEquals(33.3f, result[2]);
    }

    // Tests for unbox(Float[], int, int, float)
    @Test
    public void testUnboxFloatRange_ValidRange() {
        Float[] input = {1.0f, 2.0f, null, 4.0f, 5.0f};
        float[] result = Array.unbox(input, 1, 4, 77.7f);
        Assertions.assertNotNull(result);
        Assertions.assertEquals(3, result.length);
        Assertions.assertEquals(2.0f, result[0]);
        Assertions.assertEquals(77.7f, result[1]); // null becomes 77.7f
        Assertions.assertEquals(4.0f, result[2]);
    }

    // Tests for unbox(Double...)
    @Test
    public void testUnboxDoubleVarargs_NullInput() {
        Double[] input = null;
        double[] result = Array.unbox(input);
        Assertions.assertNull(result);
    }

    @Test
    public void testUnboxDoubleVarargs_WithNullValues() {
        Double[] input = {1.11, null, 3.33};
        double[] result = Array.unbox(input);
        Assertions.assertNotNull(result);
        Assertions.assertEquals(3, result.length);
        Assertions.assertEquals(1.11, result[0]);
        Assertions.assertEquals(0d, result[1]); // null becomes 0d
        Assertions.assertEquals(3.33, result[2]);
    }

    // Tests for unbox(Double[], double)
    @Test
    public void testUnboxDoubleWithDefault_WithNullValues() {
        Double[] input = {11.11, null, 33.33};
        double[] result = Array.unbox(input, 99.99);
        Assertions.assertNotNull(result);
        Assertions.assertEquals(3, result.length);
        Assertions.assertEquals(11.11, result[0]);
        Assertions.assertEquals(99.99, result[1]); // null becomes 99.99
        Assertions.assertEquals(33.33, result[2]);
    }

    // Tests for unbox(Double[], int, int, double)
    @Test
    public void testUnboxDoubleRange_ValidRange() {
        Double[] input = {1.0, 2.0, null, 4.0, 5.0};
        double[] result = Array.unbox(input, 1, 4, 77.77);
        Assertions.assertNotNull(result);
        Assertions.assertEquals(3, result.length);
        Assertions.assertEquals(2.0, result[0]);
        Assertions.assertEquals(77.77, result[1]); // null becomes 77.77
        Assertions.assertEquals(4.0, result[2]);
    }

    // Tests for 2D unbox methods
    // Tests for unbox(Boolean[][])
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
        Boolean[][] input = {
            {true, null, false},
            {null, true, true}
        };
        boolean[][] result = Array.unbox(input);
        Assertions.assertNotNull(result);
        Assertions.assertEquals(2, result.length);
        Assertions.assertEquals(3, result[0].length);
        Assertions.assertTrue(result[0][0]);
        Assertions.assertFalse(result[0][1]); // null becomes false
        Assertions.assertFalse(result[0][2]);
        Assertions.assertFalse(result[1][0]); // null becomes false
        Assertions.assertTrue(result[1][1]);
        Assertions.assertTrue(result[1][2]);
    }

    // Tests for unbox(Boolean[][], boolean)
    @Test
    public void testUnboxBoolean2DWithDefault_ValidArray() {
        Boolean[][] input = {
            {true, null},
            {null, false}
        };
        boolean[][] result = Array.unbox(input, true);
        Assertions.assertNotNull(result);
        Assertions.assertEquals(2, result.length);
        Assertions.assertTrue(result[0][0]);
        Assertions.assertTrue(result[0][1]); // null becomes true
        Assertions.assertTrue(result[1][0]); // null becomes true
        Assertions.assertFalse(result[1][1]);
    }

    // Tests for unbox(Character[][])
    @Test
    public void testUnboxCharacter2D_NullInput() {
        Character[][] input = null;
        char[][] result = Array.unbox(input);
        Assertions.assertNull(result);
    }

    @Test
    public void testUnboxCharacter2D_ValidArray() {
        Character[][] input = {
            {'a', null, 'c'},
            {null, 'e', 'f'}
        };
        char[][] result = Array.unbox(input);
        Assertions.assertNotNull(result);
        Assertions.assertEquals(2, result.length);
        Assertions.assertEquals('a', result[0][0]);
        Assertions.assertEquals((char)0, result[0][1]); // null becomes 0
        Assertions.assertEquals('c', result[0][2]);
    }

    // Tests for unbox(Character[][], char)
    @Test
    public void testUnboxCharacter2DWithDefault_ValidArray() {
        Character[][] input = {
            {'x', null},
            {null, 'z'}
        };
        char[][] result = Array.unbox(input, '?');
        Assertions.assertNotNull(result);
        Assertions.assertEquals(2, result.length);
        Assertions.assertEquals('x', result[0][0]);
        Assertions.assertEquals('?', result[0][1]); // null becomes '?'
        Assertions.assertEquals('?', result[1][0]); // null becomes '?'
        Assertions.assertEquals('z', result[1][1]);
    }

    // Tests for unbox(Byte[][])
    @Test
    public void testUnboxByte2D_NullInput() {
        Byte[][] input = null;
        byte[][] result = Array.unbox(input);
        Assertions.assertNull(result);
    }

    @Test
    public void testUnboxByte2D_ValidArray() {
        Byte[][] input = {
            {1, null, 3},
            {null, 5, 6}
        };
        byte[][] result = Array.unbox(input);
        Assertions.assertNotNull(result);
        Assertions.assertEquals(2, result.length);
        Assertions.assertEquals((byte)1, result[0][0]);
        Assertions.assertEquals((byte)0, result[0][1]); // null becomes 0
        Assertions.assertEquals((byte)3, result[0][2]);
    }

    // Tests for unbox(Byte[][], byte)
    @Test
    public void testUnboxByte2DWithDefault_ValidArray() {
        Byte[][] input = {
            {10, null},
            {null, 30}
        };
        byte[][] result = Array.unbox(input, (byte) 99);
        Assertions.assertNotNull(result);
        Assertions.assertEquals(2, result.length);
        Assertions.assertEquals((byte)10, result[0][0]);
        Assertions.assertEquals((byte)99, result[0][1]); // null becomes 99
    }

    // Tests for unbox(Short[][])
    @Test
    public void testUnboxShort2D_NullInput() {
        Short[][] input = null;
        short[][] result = Array.unbox(input);
        Assertions.assertNull(result);
    }

    @Test
    public void testUnboxShort2D_ValidArray() {
        Short[][] input = {
            {100, null, 300},
            {null, 500, 600}
        };
        short[][] result = Array.unbox(input);
        Assertions.assertNotNull(result);
        Assertions.assertEquals(2, result.length);
        Assertions.assertEquals((short)100, result[0][0]);
        Assertions.assertEquals((short)0, result[0][1]); // null becomes 0
    }

    // Tests for unbox(Short[][], short)
    @Test
    public void testUnboxShort2DWithDefault_ValidArray() {
        Short[][] input = {
            {1000, null},
            {null, 3000}
        };
        short[][] result = Array.unbox(input, (short) 9999);
        Assertions.assertNotNull(result);
        Assertions.assertEquals(2, result.length);
        Assertions.assertEquals((short)1000, result[0][0]);
        Assertions.assertEquals((short)9999, result[0][1]); // null becomes 9999
    }

    // Tests for unbox(Integer[][])
    @Test
    public void testUnboxInteger2D_NullInput() {
        Integer[][] input = null;
        int[][] result = Array.unbox(input);
        Assertions.assertNull(result);
    }

    @Test
    public void testUnboxInteger2D_ValidArray() {
        Integer[][] input = {
            {1000, null, 3000},
            {null, 5000, 6000}
        };
        int[][] result = Array.unbox(input);
        Assertions.assertNotNull(result);
        Assertions.assertEquals(2, result.length);
        Assertions.assertEquals(1000, result[0][0]);
        Assertions.assertEquals(0, result[0][1]); // null becomes 0
    }

    // Tests for unbox(Integer[][], int)
    @Test
    public void testUnboxInteger2DWithDefault_ValidArray() {
        Integer[][] input = {
            {10000, null},
            {null, 30000}
        };
        int[][] result = Array.unbox(input, 99999);
        Assertions.assertNotNull(result);
        Assertions.assertEquals(2, result.length);
        Assertions.assertEquals(10000, result[0][0]);
        Assertions.assertEquals(99999, result[0][1]); // null becomes 99999
    }

    // Tests for unbox(Long[][])
    @Test
    public void testUnboxLong2D_NullInput() {
        Long[][] input = null;
        long[][] result = Array.unbox(input);
        Assertions.assertNull(result);
    }

    @Test
    public void testUnboxLong2D_ValidArray() {
        Long[][] input = {
            {100000L, null, 300000L},
            {null, 500000L, 600000L}
        };
        long[][] result = Array.unbox(input);
        Assertions.assertNotNull(result);
        Assertions.assertEquals(2, result.length);
        Assertions.assertEquals(100000L, result[0][0]);
        Assertions.assertEquals(0L, result[0][1]); // null becomes 0
    }

    // Tests for unbox(Long[][], long)
    @Test
    public void testUnboxLong2DWithDefault_ValidArray() {
        Long[][] input = {
            {1000000L, null},
            {null, 3000000L}
        };
        long[][] result = Array.unbox(input, 9999999L);
        Assertions.assertNotNull(result);
        Assertions.assertEquals(2, result.length);
        Assertions.assertEquals(1000000L, result[0][0]);
        Assertions.assertEquals(9999999L, result[0][1]); // null becomes 9999999L
    }

    // Tests for unbox(Float[][])
    @Test
    public void testUnboxFloat2D_NullInput() {
        Float[][] input = null;
        float[][] result = Array.unbox(input);
        Assertions.assertNull(result);
    }

    @Test
    public void testUnboxFloat2D_ValidArray() {
        Float[][] input = {
            {1.1f, null, 3.3f},
            {null, 5.5f, 6.6f}
        };
        float[][] result = Array.unbox(input);
        Assertions.assertNotNull(result);
        Assertions.assertEquals(2, result.length);
        Assertions.assertEquals(1.1f, result[0][0]);
        Assertions.assertEquals(0f, result[0][1]); // null becomes 0
    }

    // Tests for unbox(Float[][], float)
    @Test
    public void testUnboxFloat2DWithDefault_ValidArray() {
        Float[][] input = {
            {11.1f, null},
            {null, 33.3f}
        };
        float[][] result = Array.unbox(input, 99.9f);
        Assertions.assertNotNull(result);
        Assertions.assertEquals(2, result.length);
        Assertions.assertEquals(11.1f, result[0][0]);
        Assertions.assertEquals(99.9f, result[0][1]); // null becomes 99.9f
    }

    // Tests for unbox(Double[][])
    @Test
    public void testUnboxDouble2D_NullInput() {
        Double[][] input = null;
        double[][] result = Array.unbox(input);
        Assertions.assertNull(result);
    }

    @Test
    public void testUnboxDouble2D_ValidArray() {
        Double[][] input = {
            {1.11, null, 3.33},
            {null, 5.55, 6.66}
        };
        double[][] result = Array.unbox(input);
        Assertions.assertNotNull(result);
        Assertions.assertEquals(2, result.length);
        Assertions.assertEquals(1.11, result[0][0]);
        Assertions.assertEquals(0d, result[0][1]); // null becomes 0
    }

    // Tests for unbox(Double[][], double)
    @Test
    public void testUnboxDouble2DWithDefault_ValidArray() {
        Double[][] input = {
            {11.11, null},
            {null, 33.33}
        };
        double[][] result = Array.unbox(input, 99.99);
        Assertions.assertNotNull(result);
        Assertions.assertEquals(2, result.length);
        Assertions.assertEquals(11.11, result[0][0]);
        Assertions.assertEquals(99.99, result[0][1]); // null becomes 99.99
    }

    // Tests for 3D unbox methods
    // Tests for unbox(Boolean[][][])
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
        Boolean[][][] input = {
            {{true, null}, {false, true}},
            {{null, false}, {true, null}}
        };
        boolean[][][] result = Array.unbox(input);
        Assertions.assertNotNull(result);
        Assertions.assertEquals(2, result.length);
        Assertions.assertTrue(result[0][0][0]);
        Assertions.assertFalse(result[0][0][1]); // null becomes false
        Assertions.assertFalse(result[0][1][0]);
        Assertions.assertTrue(result[0][1][1]);
    }

    // Tests for unbox(Boolean[][][], boolean)
    @Test
    public void testUnboxBoolean3DWithDefault_ValidArray() {
        Boolean[][][] input = {
            {{true, null}, {null, false}},
            {{null, true}, {false, null}}
        };
        boolean[][][] result = Array.unbox(input, true);
        Assertions.assertNotNull(result);
        Assertions.assertEquals(2, result.length);
        Assertions.assertTrue(result[0][0][0]);
        Assertions.assertTrue(result[0][0][1]); // null becomes true
        Assertions.assertTrue(result[0][1][0]); // null becomes true
        Assertions.assertFalse(result[0][1][1]);
    }

    // Tests for unbox(Character[][][])
    @Test
    public void testUnboxCharacter3D_NullInput() {
        Character[][][] input = null;
        char[][][] result = Array.unbox(input);
        Assertions.assertNull(result);
    }

    @Test
    public void testUnboxCharacter3D_ValidArray() {
        Character[][][] input = {
            {{'a', null}, {'c', 'd'}},
            {{null, 'f'}, {'g', null}}
        };
        char[][][] result = Array.unbox(input);
        Assertions.assertNotNull(result);
        Assertions.assertEquals(2, result.length);
        Assertions.assertEquals('a', result[0][0][0]);
        Assertions.assertEquals((char)0, result[0][0][1]); // null becomes 0
    }

    // Tests for unbox(Character[][][], char)
    @Test
    public void testUnboxCharacter3DWithDefault_ValidArray() {
        Character[][][] input = {
            {{'x', null}, {null, 'z'}},
            {{null, 'b'}, {'c', null}}
        };
        char[][][] result = Array.unbox(input, '?');
        Assertions.assertNotNull(result);
        Assertions.assertEquals(2, result.length);
        Assertions.assertEquals('x', result[0][0][0]);
        Assertions.assertEquals('?', result[0][0][1]); // null becomes '?'
    }

    // Tests for unbox(Byte[][][])
    @Test
    public void testUnboxByte3D_NullInput() {
        Byte[][][] input = null;
        byte[][][] result = Array.unbox(input);
        Assertions.assertNull(result);
    }

    @Test
    public void testUnboxByte3D_ValidArray() {
        Byte[][][] input = {
            {{1, null}, {3, 4}},
            {{null, 6}, {7, null}}
        };
        byte[][][] result = Array.unbox(input);
        Assertions.assertNotNull(result);
        Assertions.assertEquals(2, result.length);
        Assertions.assertEquals((byte)1, result[0][0][0]);
        Assertions.assertEquals((byte)0, result[0][0][1]); // null becomes 0
    }

    // Tests for unbox(Byte[][][], byte)
    @Test
    public void testUnboxByte3DWithDefault_ValidArray() {
        Byte[][][] input = {
            {{10, null}, {null, 40}},
            {{null, 60}, {70, null}}
        };
        byte[][][] result = Array.unbox(input, (byte) 99);
        Assertions.assertNotNull(result);
        Assertions.assertEquals(2, result.length);
        Assertions.assertEquals((byte)10, result[0][0][0]);
        Assertions.assertEquals((byte)99, result[0][0][1]); // null becomes 99
    }

    // Tests for unbox(Short[][][])
    @Test
    public void testUnboxShort3D_NullInput() {
        Short[][][] input = null;
        short[][][] result = Array.unbox(input);
        Assertions.assertNull(result);
    }

    @Test
    public void testUnboxShort3D_ValidArray() {
        Short[][][] input = {
            {{100, null}, {300, 400}},
            {{null, 600}, {700, null}}
        };
        short[][][] result = Array.unbox(input);
        Assertions.assertNotNull(result);
        Assertions.assertEquals(2, result.length);
        Assertions.assertEquals((short)100, result[0][0][0]);
        Assertions.assertEquals((short)0, result[0][0][1]); // null becomes 0
    }

    // Tests for unbox(Short[][][], short)
    @Test
    public void testUnboxShort3DWithDefault_ValidArray() {
        Short[][][] input = {
            {{1000, null}, {null, 4000}},
            {{null, 6000}, {7000, null}}
        };
        short[][][] result = Array.unbox(input, (short) 9999);
        Assertions.assertNotNull(result);
        Assertions.assertEquals(2, result.length);
        Assertions.assertEquals((short)1000, result[0][0][0]);
        Assertions.assertEquals((short)9999, result[0][0][1]); // null becomes 9999
    }

    // Tests for unbox(Integer[][][])
    @Test
    public void testUnboxInteger3D_NullInput() {
        Integer[][][] input = null;
        int[][][] result = Array.unbox(input);
        Assertions.assertNull(result);
    }

    @Test
    public void testUnboxInteger3D_ValidArray() {
        Integer[][][] input = {
            {{1000, null}, {3000, 4000}},
            {{null, 6000}, {7000, null}}
        };
        int[][][] result = Array.unbox(input);
        Assertions.assertNotNull(result);
        Assertions.assertEquals(2, result.length);
        Assertions.assertEquals(1000, result[0][0][0]);
        Assertions.assertEquals(0, result[0][0][1]); // null becomes 0
    }

    // Tests for unbox(Integer[][][], int)
    @Test
    public void testUnboxInteger3DWithDefault_ValidArray() {
        Integer[][][] input = {
            {{10000, null}, {null, 40000}},
            {{null, 60000}, {70000, null}}
        };
        int[][][] result = Array.unbox(input, 99999);
        Assertions.assertNotNull(result);
        Assertions.assertEquals(2, result.length);
        Assertions.assertEquals(10000, result[0][0][0]);
        Assertions.assertEquals(99999, result[0][0][1]); // null becomes 99999
    }

    // Tests for unbox(Long[][][])
    @Test
    public void testUnboxLong3D_NullInput() {
        Long[][][] input = null;
        long[][][] result = Array.unbox(input);
        Assertions.assertNull(result);
    }

    @Test
    public void testUnboxLong3D_ValidArray() {
        Long[][][] input = {
            {{100000L, null}, {300000L, 400000L}},
            {{null, 600000L}, {700000L, null}}
        };
        long[][][] result = Array.unbox(input);
        Assertions.assertNotNull(result);
        Assertions.assertEquals(2, result.length);
        Assertions.assertEquals(100000L, result[0][0][0]);
        Assertions.assertEquals(0L, result[0][0][1]); // null becomes 0
    }

    // Tests for unbox(Long[][][], long)
    @Test
    public void testUnboxLong3DWithDefault_ValidArray() {
        Long[][][] input = {
            {{1000000L, null}, {null, 4000000L}},
            {{null, 6000000L}, {7000000L, null}}
        };
        long[][][] result = Array.unbox(input, 9999999L);
        Assertions.assertNotNull(result);
        Assertions.assertEquals(2, result.length);
        Assertions.assertEquals(1000000L, result[0][0][0]);
        Assertions.assertEquals(9999999L, result[0][0][1]); // null becomes 9999999L
    }

    // Tests for unbox(Float[][][])
    @Test
    public void testUnboxFloat3D_NullInput() {
        Float[][][] input = null;
        float[][][] result = Array.unbox(input);
        Assertions.assertNull(result);
    }

    @Test
    public void testUnboxFloat3D_ValidArray() {
        Float[][][] input = {
            {{1.1f, null}, {3.3f, 4.4f}},
            {{null, 6.6f}, {7.7f, null}}
        };
        float[][][] result = Array.unbox(input);
        Assertions.assertNotNull(result);
        Assertions.assertEquals(2, result.length);
        Assertions.assertEquals(1.1f, result[0][0][0]);
        Assertions.assertEquals(0f, result[0][0][1]); // null becomes 0
    }

    // Tests for unbox(Float[][][], float)
    @Test
    public void testUnboxFloat3DWithDefault_ValidArray() {
        Float[][][] input = {
            {{11.1f, null}, {null, 44.4f}},
            {{null, 66.6f}, {77.7f, null}}
        };
        float[][][] result = Array.unbox(input, 99.9f);
        Assertions.assertNotNull(result);
        Assertions.assertEquals(2, result.length);
        Assertions.assertEquals(11.1f, result[0][0][0]);
        Assertions.assertEquals(99.9f, result[0][0][1]); // null becomes 99.9f
    }

    // Tests for unbox(Double[][][])
    @Test
    public void testUnboxDouble3D_NullInput() {
        Double[][][] input = null;
        double[][][] result = Array.unbox(input);
        Assertions.assertNull(result);
    }

    @Test
    public void testUnboxDouble3D_ValidArray() {
        Double[][][] input = {
            {{1.11, null}, {3.33, 4.44}},
            {{null, 6.66}, {7.77, null}}
        };
        double[][][] result = Array.unbox(input);
        Assertions.assertNotNull(result);
        Assertions.assertEquals(2, result.length);
        Assertions.assertEquals(1.11, result[0][0][0]);
        Assertions.assertEquals(0d, result[0][0][1]); // null becomes 0
    }

    // Tests for unbox(Double[][][], double)
    @Test
    public void testUnboxDouble3DWithDefault_ValidArray() {
        Double[][][] input = {
            {{11.11, null}, {null, 44.44}},
            {{null, 66.66}, {77.77, null}}
        };
        double[][][] result = Array.unbox(input, 99.99);
        Assertions.assertNotNull(result);
        Assertions.assertEquals(2, result.length);
        Assertions.assertEquals(11.11, result[0][0][0]);
        Assertions.assertEquals(99.99, result[0][0][1]); // null becomes 99.99
    }

    // Tests for transpose methods
    // Tests for transpose(boolean[][])
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
        boolean[][] input = {
            {true, false, true},
            {false, true, false},
            {true, true, false}
        };
        boolean[][] result = Array.transpose(input);
        
        Assertions.assertNotNull(result);
        Assertions.assertEquals(3, result.length);
        Assertions.assertEquals(3, result[0].length);
        
        // Check transposed values
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
        boolean[][] input = {
            {true, false},
            {false, true},
            {true, true}
        };
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

    // Tests for transpose(char[][])
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
        char[][] input = {
            {'a', 'b', 'c'},
            {'d', 'e', 'f'},
            {'g', 'h', 'i'}
        };
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

    // Tests for transpose(byte[][])
    @Test
    public void testTransposeByte_NullInput() {
        byte[][] input = null;
        byte[][] result = Array.transpose(input);
        Assertions.assertNull(result);
    }

    @Test
    public void testTransposeByte_RectangularMatrix() {
        byte[][] input = {
            {1, 2, 3, 4},
            {5, 6, 7, 8}
        };
        byte[][] result = Array.transpose(input);
        
        Assertions.assertNotNull(result);
        Assertions.assertEquals(4, result.length);
        Assertions.assertEquals(2, result[0].length);
        
        Assertions.assertEquals((byte)1, result[0][0]);
        Assertions.assertEquals((byte)5, result[0][1]);
        Assertions.assertEquals((byte)2, result[1][0]);
        Assertions.assertEquals((byte)6, result[1][1]);
        Assertions.assertEquals((byte)3, result[2][0]);
        Assertions.assertEquals((byte)7, result[2][1]);
        Assertions.assertEquals((byte)4, result[3][0]);
        Assertions.assertEquals((byte)8, result[3][1]);
    }

    // Tests for transpose(short[][])
    @Test
    public void testTransposeShort_NullInput() {
        short[][] input = null;
        short[][] result = Array.transpose(input);
        Assertions.assertNull(result);
    }

    @Test
    public void testTransposeShort_SingleElement() {
        short[][] input = {{100}};
        short[][] result = Array.transpose(input);
        
        Assertions.assertNotNull(result);
        Assertions.assertEquals(1, result.length);
        Assertions.assertEquals(1, result[0].length);
        Assertions.assertEquals((short)100, result[0][0]);
    }

    // Tests for transpose(int[][])
    @Test
    public void testTransposeInt_NullInput() {
        int[][] input = null;
        int[][] result = Array.transpose(input);
        Assertions.assertNull(result);
    }

    @Test
    public void testTransposeInt_RectangularMatrix() {
        int[][] input = {
            {1, 2, 3},
            {4, 5, 6}
        };
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

    // Tests for transpose(long[][])
    @Test
    public void testTransposeLong_NullInput() {
        long[][] input = null;
        long[][] result = Array.transpose(input);
        Assertions.assertNull(result);
    }

    @Test
    public void testTransposeLong_SquareMatrix() {
        long[][] input = {
            {1000L, 2000L},
            {3000L, 4000L}
        };
        long[][] result = Array.transpose(input);
        
        Assertions.assertNotNull(result);
        Assertions.assertEquals(2, result.length);
        Assertions.assertEquals(2, result[0].length);
        
        Assertions.assertEquals(1000L, result[0][0]);
        Assertions.assertEquals(3000L, result[0][1]);
        Assertions.assertEquals(2000L, result[1][0]);
        Assertions.assertEquals(4000L, result[1][1]);
    }

    // Tests for transpose(float[][])
    @Test
    public void testTransposeFloat_NullInput() {
        float[][] input = null;
        float[][] result = Array.transpose(input);
        Assertions.assertNull(result);
    }

    @Test
    public void testTransposeFloat_RectangularMatrix() {
        float[][] input = {
            {1.1f, 2.2f, 3.3f},
            {4.4f, 5.5f, 6.6f}
        };
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

    // Tests for transpose(double[][])
    @Test
    public void testTransposeDouble_NullInput() {
        double[][] input = null;
        double[][] result = Array.transpose(input);
        Assertions.assertNull(result);
    }

    @Test
    public void testTransposeDouble_SquareMatrix() {
        double[][] input = {
            {1.11, 2.22, 3.33},
            {4.44, 5.55, 6.66},
            {7.77, 8.88, 9.99}
        };
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

    // Tests for transpose(T[][])
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
        String[][] input = {
            {"a", "b", "c"},
            {"d", "e", "f"}
        };
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
        Integer[][] input = {
            {1, 2},
            {3, 4},
            {5, 6}
        };
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
        // Test with jagged array (not a proper matrix)
        int[][] input = {
            {1, 2, 3},
            {4, 5}  // Different length
        };
        
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            Array.transpose(input);
        });
    }

    // Additional edge case tests for transpose
    @Test
    public void testTransposeBoolean_SingleRow() {
        boolean[][] input = {{true, false, true, false}};
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
        int[][] input = {{1}, {2}, {3}, {4}};
        int[][] result = Array.transpose(input);
        
        Assertions.assertNotNull(result);
        Assertions.assertEquals(1, result.length);
        Assertions.assertEquals(4, result[0].length);
        
        Assertions.assertEquals(1, result[0][0]);
        Assertions.assertEquals(2, result[0][1]);
        Assertions.assertEquals(3, result[0][2]);
        Assertions.assertEquals(4, result[0][3]);
    }

    // Edge cases for 3D arrays with null elements
    @Test
    public void testBoxByte3D_WithNullSubArrays() {
        byte[][][] input = {
            null,
            {{1, 2}, {3, 4}},
            null
        };
        Byte[][][] result = Array.box(input);
        
        Assertions.assertNotNull(result);
        Assertions.assertEquals(3, result.length);
        Assertions.assertNull(result[0]);
        Assertions.assertNotNull(result[1]);
        Assertions.assertNull(result[2]);
        
        Assertions.assertEquals(Byte.valueOf((byte)1), result[1][0][0]);
        Assertions.assertEquals(Byte.valueOf((byte)4), result[1][1][1]);
    }

    @Test
    public void testUnboxBoolean3D_WithNullSubArrays() {
        Boolean[][][] input = {
            {{true, false}, null},
            null,
            {{null, true}, {false, null}}
        };
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

    // Large array tests
    @Test
    public void testTransposeDouble_LargeMatrix() {
        int rows = 100;
        int cols = 50;
        double[][] input = new double[rows][cols];
        
        // Fill with test data
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                input[i][j] = i * cols + j;
            }
        }
        
        double[][] result = Array.transpose(input);
        
        Assertions.assertNotNull(result);
        Assertions.assertEquals(cols, result.length);
        Assertions.assertEquals(rows, result[0].length);
        
        // Verify some values
        Assertions.assertEquals(0.0, result[0][0]);
        Assertions.assertEquals(1.0, result[1][0]);
        Assertions.assertEquals(50.0, result[0][1]);
        Assertions.assertEquals(input[rows-1][cols-1], result[cols-1][rows-1]);
    }

    @Test
    public void testUnboxInteger_AllNullValues() {
        Integer[] input = {null, null, null};
        int[] result = Array.unbox(input, -1);
        
        Assertions.assertNotNull(result);
        Assertions.assertEquals(3, result.length);
        Assertions.assertEquals(-1, result[0]);
        Assertions.assertEquals(-1, result[1]);
        Assertions.assertEquals(-1, result[2]);
    }

    @Test
    public void testUnboxDouble2D_MixedNullRows() {
        Double[][] input = {
            {1.1, 2.2, 3.3},
            null,
            {4.4, null, 6.6}
        };
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
        Assertions.assertEquals(0.0, result[2][1]); // null becomes 0.0
        Assertions.assertEquals(6.6, result[2][2]);
    }

    // Boundary tests
    @Test
    public void testUnboxLongRange_FullArray() {
        Long[] input = {1L, 2L, 3L, 4L, 5L};
        long[] result = Array.unbox(input, 0, input.length, 0L);
        
        Assertions.assertNotNull(result);
        Assertions.assertEquals(5, result.length);
        for (int i = 0; i < 5; i++) {
            Assertions.assertEquals(i + 1L, result[i]);
        }
    }

    @Test
    public void testUnboxFloatRange_BoundaryIndices() {
        Float[] input = {1.0f, 2.0f, 3.0f, 4.0f, 5.0f};
        
        // Test fromIndex = toIndex
        float[] result1 = Array.unbox(input, 2, 2, 0f);
        Assertions.assertNotNull(result1);
        Assertions.assertEquals(0, result1.length);
        
        // Test single element range
        float[] result2 = Array.unbox(input, 2, 3, 0f);
        Assertions.assertNotNull(result2);
        Assertions.assertEquals(1, result2.length);
        Assertions.assertEquals(3.0f, result2[0]);
    }

    // Test with maximum primitive values
    @Test
    public void testUnboxByte_MaxMinValues() {
        Byte[] input = {Byte.MAX_VALUE, null, Byte.MIN_VALUE};
        byte[] result = Array.unbox(input, (byte) 0);
        
        Assertions.assertNotNull(result);
        Assertions.assertEquals(3, result.length);
        Assertions.assertEquals(Byte.MAX_VALUE, result[0]);
        Assertions.assertEquals((byte)0, result[1]);
        Assertions.assertEquals(Byte.MIN_VALUE, result[2]);
    }

    @Test
    public void testBoxDouble3D_MaxMinValues() {
        double[][][] input = {
            {{Double.MAX_VALUE, Double.MIN_VALUE}},
            {{Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY}},
            {{Double.NaN, 0.0}}
        };
        Double[][][] result = Array.box(input);
        
        Assertions.assertNotNull(result);
        Assertions.assertEquals(Double.MAX_VALUE, result[0][0][0]);
        Assertions.assertEquals(Double.MIN_VALUE, result[0][0][1]);
        Assertions.assertEquals(Double.POSITIVE_INFINITY, result[1][0][0]);
        Assertions.assertEquals(Double.NEGATIVE_INFINITY, result[1][0][1]);
        Assertions.assertTrue(Double.isNaN(result[2][0][0]));
        Assertions.assertEquals(0.0, result[2][0][1]);
    }

    // Performance-related test (checking handling of sparse arrays)
    @Test
    public void testTransposeGeneric_SparseMatrix() {
        Integer[][] input = new Integer[10][10];
        // Only fill diagonal
        for (int i = 0; i < 10; i++) {
            for (int j = 0; j < 10; j++) {
                input[i][j] = (i == j) ? i : null;
            }
        }
        
        Integer[][] result = Array.transpose(input);
        
        Assertions.assertNotNull(result);
        Assertions.assertEquals(10, result.length);
        Assertions.assertEquals(10, result[0].length);
        
        // Check diagonal remains diagonal
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

    // Tests for repeat(boolean element, int n)
    @Test
    public void testRepeatBooleanElement() {
        // Test normal case
        boolean[] result = Array.repeat(true, 5);
        Assertions.assertEquals(5, result.length);
        for (int i = 0; i < 5; i++) {
            Assertions.assertTrue(result[i]);
        }

        // Test with false
        result = Array.repeat(false, 3);
        Assertions.assertEquals(3, result.length);
        for (int i = 0; i < 3; i++) {
            Assertions.assertFalse(result[i]);
        }

        // Test with n = 0
        result = Array.repeat(true, 0);
        Assertions.assertEquals(0, result.length);

        // Test with n = 1
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

    // Tests for repeat(boolean[] a, int n)
    @Test
    public void testRepeatBooleanArray() {
        // Test normal case
        boolean[] original = { true, false, true };
        boolean[] result = Array.repeat(original, 3);
        Assertions.assertEquals(9, result.length);
        boolean[] expected = { true, false, true, true, false, true, true, false, true };
        Assertions.assertArrayEquals(expected, result);

        // Test single element array
        boolean[] single = { false };
        result = Array.repeat(single, 5);
        Assertions.assertEquals(5, result.length);
        for (int i = 0; i < 5; i++) {
            Assertions.assertFalse(result[i]);
        }

        // Test empty array
        boolean[] empty = {};
        result = Array.repeat(empty, 10);
        Assertions.assertEquals(0, result.length);

        // Test n = 0
        result = Array.repeat(original, 0);
        Assertions.assertEquals(0, result.length);

        // Test n = 1
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

    //    @Test
    //    public void testRepeatBooleanArrayOverflow() {
    //        boolean[] array = new boolean[Integer.MAX_VALUE / 2 + 1];
    //        Assertions.assertThrows(IllegalArgumentException.class, () -> {
    //            Array.repeat(array, 2);
    //        });
    //    }

    // Tests for repeat(char element, int n)
    @Test
    public void testRepeatCharElement() {
        // Test normal case
        char[] result = Array.repeat('a', 5);
        Assertions.assertEquals(5, result.length);
        for (int i = 0; i < 5; i++) {
            Assertions.assertEquals('a', result[i]);
        }

        // Test with different character
        result = Array.repeat('Z', 3);
        Assertions.assertEquals(3, result.length);
        for (int i = 0; i < 3; i++) {
            Assertions.assertEquals('Z', result[i]);
        }

        // Test with n = 0
        result = Array.repeat('x', 0);
        Assertions.assertEquals(0, result.length);

        // Test with n = 1
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

    // Tests for repeat(char[] a, int n)
    @Test
    public void testRepeatCharArray() {
        // Test normal case
        char[] original = { 'a', 'b', 'c' };
        char[] result = Array.repeat(original, 3);
        Assertions.assertEquals(9, result.length);
        char[] expected = { 'a', 'b', 'c', 'a', 'b', 'c', 'a', 'b', 'c' };
        Assertions.assertArrayEquals(expected, result);

        // Test single element array
        char[] single = { 'x' };
        result = Array.repeat(single, 5);
        Assertions.assertEquals(5, result.length);
        for (int i = 0; i < 5; i++) {
            Assertions.assertEquals('x', result[i]);
        }

        // Test empty array
        char[] empty = {};
        result = Array.repeat(empty, 10);
        Assertions.assertEquals(0, result.length);

        // Test n = 0
        result = Array.repeat(original, 0);
        Assertions.assertEquals(0, result.length);

        // Test n = 1
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

    //    @Test
    //    public void testRepeatCharArrayOverflow() {
    //        char[] array = new char[Integer.MAX_VALUE / 2 + 1];
    //        Assertions.assertThrows(IllegalArgumentException.class, () -> {
    //            Array.repeat(array, 2);
    //        });
    //    }

    // Tests for repeat(byte element, int n)
    @Test
    public void testRepeatByteElement() {
        // Test normal case
        byte[] result = Array.repeat((byte) 5, 5);
        Assertions.assertEquals(5, result.length);
        for (int i = 0; i < 5; i++) {
            Assertions.assertEquals((byte) 5, result[i]);
        }

        // Test with negative byte
        result = Array.repeat((byte) -128, 3);
        Assertions.assertEquals(3, result.length);
        for (int i = 0; i < 3; i++) {
            Assertions.assertEquals((byte) -128, result[i]);
        }

        // Test with n = 0
        result = Array.repeat((byte) 0, 0);
        Assertions.assertEquals(0, result.length);

        // Test with n = 1
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

    // Tests for repeat(byte[] a, int n)
    @Test
    public void testRepeatByteArray() {
        // Test normal case
        byte[] original = { 1, 2, 3 };
        byte[] result = Array.repeat(original, 3);
        Assertions.assertEquals(9, result.length);
        byte[] expected = { 1, 2, 3, 1, 2, 3, 1, 2, 3 };
        Assertions.assertArrayEquals(expected, result);

        // Test single element array
        byte[] single = { 0 };
        result = Array.repeat(single, 5);
        Assertions.assertEquals(5, result.length);
        for (int i = 0; i < 5; i++) {
            Assertions.assertEquals((byte) 0, result[i]);
        }

        // Test empty array
        byte[] empty = {};
        result = Array.repeat(empty, 10);
        Assertions.assertEquals(0, result.length);

        // Test n = 0
        result = Array.repeat(original, 0);
        Assertions.assertEquals(0, result.length);

        // Test n = 1
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

    //    @Test
    //    public void testRepeatByteArrayOverflow() {
    //        byte[] array = new byte[Integer.MAX_VALUE / 2 + 1];
    //        Assertions.assertThrows(IllegalArgumentException.class, () -> {
    //            Array.repeat(array, 2);
    //        });
    //    }

    // Tests for repeat(short element, int n)
    @Test
    public void testRepeatShortElement() {
        // Test normal case
        short[] result = Array.repeat((short) 100, 5);
        Assertions.assertEquals(5, result.length);
        for (int i = 0; i < 5; i++) {
            Assertions.assertEquals((short) 100, result[i]);
        }

        // Test with negative short
        result = Array.repeat((short) -32768, 3);
        Assertions.assertEquals(3, result.length);
        for (int i = 0; i < 3; i++) {
            Assertions.assertEquals((short) -32768, result[i]);
        }

        // Test with n = 0
        result = Array.repeat((short) 0, 0);
        Assertions.assertEquals(0, result.length);

        // Test with n = 1
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

    // Tests for repeat(short[] a, int n)
    @Test
    public void testRepeatShortArray() {
        // Test normal case
        short[] original = { 1, 2, 3 };
        short[] result = Array.repeat(original, 3);
        Assertions.assertEquals(9, result.length);
        short[] expected = { 1, 2, 3, 1, 2, 3, 1, 2, 3 };
        Assertions.assertArrayEquals(expected, result);

        // Test single element array
        short[] single = { 0 };
        result = Array.repeat(single, 5);
        Assertions.assertEquals(5, result.length);
        for (int i = 0; i < 5; i++) {
            Assertions.assertEquals((short) 0, result[i]);
        }

        // Test empty array
        short[] empty = {};
        result = Array.repeat(empty, 10);
        Assertions.assertEquals(0, result.length);

        // Test n = 0
        result = Array.repeat(original, 0);
        Assertions.assertEquals(0, result.length);

        // Test n = 1
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

    //    @Test
    //    public void testRepeatShortArrayOverflow() {
    //        short[] array = new short[Integer.MAX_VALUE / 2 + 1];
    //        Assertions.assertThrows(IllegalArgumentException.class, () -> {
    //            Array.repeat(array, 2);
    //        });
    //    }

    // Tests for repeat(int element, int n)
    @Test
    public void testRepeatIntElement() {
        // Test normal case
        int[] result = Array.repeat(42, 5);
        Assertions.assertEquals(5, result.length);
        for (int i = 0; i < 5; i++) {
            Assertions.assertEquals(42, result[i]);
        }

        // Test with negative int
        result = Array.repeat(-2147483648, 3);
        Assertions.assertEquals(3, result.length);
        for (int i = 0; i < 3; i++) {
            Assertions.assertEquals(-2147483648, result[i]);
        }

        // Test with n = 0
        result = Array.repeat(0, 0);
        Assertions.assertEquals(0, result.length);

        // Test with n = 1
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

    // Tests for repeat(int[] a, int n)
    @Test
    public void testRepeatIntArray() {
        // Test normal case
        int[] original = { 1, 2, 3 };
        int[] result = Array.repeat(original, 3);
        Assertions.assertEquals(9, result.length);
        int[] expected = { 1, 2, 3, 1, 2, 3, 1, 2, 3 };
        Assertions.assertArrayEquals(expected, result);

        // Test single element array
        int[] single = { 0 };
        result = Array.repeat(single, 5);
        Assertions.assertEquals(5, result.length);
        for (int i = 0; i < 5; i++) {
            Assertions.assertEquals(0, result[i]);
        }

        // Test empty array
        int[] empty = {};
        result = Array.repeat(empty, 10);
        Assertions.assertEquals(0, result.length);

        // Test n = 0
        result = Array.repeat(original, 0);
        Assertions.assertEquals(0, result.length);

        // Test n = 1
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

    //    @Test
    //    public void testRepeatIntArrayOverflow() {
    //        int[] array = new int[Integer.MAX_VALUE / 2 + 1];
    //        Assertions.assertThrows(IllegalArgumentException.class, () -> {
    //            Array.repeat(array, 2);
    //        });
    //    }

    // Tests for repeat(long element, int n)
    @Test
    public void testRepeatLongElement() {
        // Test normal case
        long[] result = Array.repeat(1000000000L, 5);
        Assertions.assertEquals(5, result.length);
        for (int i = 0; i < 5; i++) {
            Assertions.assertEquals(1000000000L, result[i]);
        }

        // Test with negative long
        result = Array.repeat(Long.MIN_VALUE, 3);
        Assertions.assertEquals(3, result.length);
        for (int i = 0; i < 3; i++) {
            Assertions.assertEquals(Long.MIN_VALUE, result[i]);
        }

        // Test with n = 0
        result = Array.repeat(0L, 0);
        Assertions.assertEquals(0, result.length);

        // Test with n = 1
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

    // Tests for repeat(long[] a, int n)
    @Test
    public void testRepeatLongArray() {
        // Test normal case
        long[] original = { 1L, 2L, 3L };
        long[] result = Array.repeat(original, 3);
        Assertions.assertEquals(9, result.length);
        long[] expected = { 1L, 2L, 3L, 1L, 2L, 3L, 1L, 2L, 3L };
        Assertions.assertArrayEquals(expected, result);

        // Test single element array
        long[] single = { 0L };
        result = Array.repeat(single, 5);
        Assertions.assertEquals(5, result.length);
        for (int i = 0; i < 5; i++) {
            Assertions.assertEquals(0L, result[i]);
        }

        // Test empty array
        long[] empty = {};
        result = Array.repeat(empty, 10);
        Assertions.assertEquals(0, result.length);

        // Test n = 0
        result = Array.repeat(original, 0);
        Assertions.assertEquals(0, result.length);

        // Test n = 1
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

    //    @Test
    //    public void testRepeatLongArrayOverflow() {
    //        long[] array = new long[Integer.MAX_VALUE / 2 + 1];
    //        Assertions.assertThrows(IllegalArgumentException.class, () -> {
    //            Array.repeat(array, 2);
    //        });
    //    }

    // Tests for repeat(float element, int n)
    @Test
    public void testRepeatFloatElement() {
        // Test normal case
        float[] result = Array.repeat(3.14f, 5);
        Assertions.assertEquals(5, result.length);
        for (int i = 0; i < 5; i++) {
            Assertions.assertEquals(3.14f, result[i], 0.0001f);
        }

        // Test with negative float
        result = Array.repeat(-Float.MAX_VALUE, 3);
        Assertions.assertEquals(3, result.length);
        for (int i = 0; i < 3; i++) {
            Assertions.assertEquals(-Float.MAX_VALUE, result[i], 0.0001f);
        }

        // Test with n = 0
        result = Array.repeat(0.0f, 0);
        Assertions.assertEquals(0, result.length);

        // Test with n = 1
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

    // Tests for repeat(float[] a, int n)
    @Test
    public void testRepeatFloatArray() {
        // Test normal case
        float[] original = { 1.1f, 2.2f, 3.3f };
        float[] result = Array.repeat(original, 3);
        Assertions.assertEquals(9, result.length);
        float[] expected = { 1.1f, 2.2f, 3.3f, 1.1f, 2.2f, 3.3f, 1.1f, 2.2f, 3.3f };
        Assertions.assertArrayEquals(expected, result, 0.0001f);

        // Test single element array
        float[] single = { 0.0f };
        result = Array.repeat(single, 5);
        Assertions.assertEquals(5, result.length);
        for (int i = 0; i < 5; i++) {
            Assertions.assertEquals(0.0f, result[i], 0.0001f);
        }

        // Test empty array
        float[] empty = {};
        result = Array.repeat(empty, 10);
        Assertions.assertEquals(0, result.length);

        // Test n = 0
        result = Array.repeat(original, 0);
        Assertions.assertEquals(0, result.length);

        // Test n = 1
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

    //    @Test
    //    public void testRepeatFloatArrayOverflow() {
    //        float[] array = new float[Integer.MAX_VALUE / 2 + 1];
    //        Assertions.assertThrows(IllegalArgumentException.class, () -> {
    //            Array.repeat(array, 2);
    //        });
    //    }

    // Tests for repeat(double element, int n)
    @Test
    public void testRepeatDoubleElement() {
        // Test normal case
        double[] result = Array.repeat(3.14159, 5);
        Assertions.assertEquals(5, result.length);
        for (int i = 0; i < 5; i++) {
            Assertions.assertEquals(3.14159, result[i], 0.00001);
        }

        // Test with negative double
        result = Array.repeat(-Double.MAX_VALUE, 3);
        Assertions.assertEquals(3, result.length);
        for (int i = 0; i < 3; i++) {
            Assertions.assertEquals(-Double.MAX_VALUE, result[i], 0.00001);
        }

        // Test with n = 0
        result = Array.repeat(0.0, 0);
        Assertions.assertEquals(0, result.length);

        // Test with n = 1
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

    // Tests for repeat(double[] a, int n)
    @Test
    public void testRepeatDoubleArray() {
        // Test normal case
        double[] original = { 1.1, 2.2, 3.3 };
        double[] result = Array.repeat(original, 3);
        Assertions.assertEquals(9, result.length);
        double[] expected = { 1.1, 2.2, 3.3, 1.1, 2.2, 3.3, 1.1, 2.2, 3.3 };
        Assertions.assertArrayEquals(expected, result, 0.00001);

        // Test single element array
        double[] single = { 0.0 };
        result = Array.repeat(single, 5);
        Assertions.assertEquals(5, result.length);
        for (int i = 0; i < 5; i++) {
            Assertions.assertEquals(0.0, result[i], 0.00001);
        }

        // Test empty array
        double[] empty = {};
        result = Array.repeat(empty, 10);
        Assertions.assertEquals(0, result.length);

        // Test n = 0
        result = Array.repeat(original, 0);
        Assertions.assertEquals(0, result.length);

        // Test n = 1
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

    //    @Test
    //    public void testRepeatDoubleArrayOverflow() {
    //        double[] array = new double[Integer.MAX_VALUE / 2 + 1];
    //        Assertions.assertThrows(IllegalArgumentException.class, () -> {
    //            Array.repeat(array, 2);
    //        });
    //    }

    // Tests for repeat(String element, int n)
    @Test
    public void testRepeatStringElement() {
        // Test normal case
        String[] result = Array.repeat("hello", 5);
        Assertions.assertEquals(5, result.length);
        for (int i = 0; i < 5; i++) {
            Assertions.assertEquals("hello", result[i]);
        }

        // Test with empty string
        result = Array.repeat("", 3);
        Assertions.assertEquals(3, result.length);
        for (int i = 0; i < 3; i++) {
            Assertions.assertEquals("", result[i]);
        }

        // Test with n = 0
        result = Array.repeat("test", 0);
        Assertions.assertEquals(0, result.length);

        // Test with n = 1
        result = Array.repeat("single", 1);
        Assertions.assertEquals(1, result.length);
        Assertions.assertEquals("single", result[0]);

        // Test with null element
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

    // Tests for repeat(String[] a, int n)
    @Test
    public void testRepeatStringArray() {
        // Test normal case
        String[] original = { "a", "b", "c" };
        String[] result = Array.repeat(original, 3);
        Assertions.assertEquals(9, result.length);
        String[] expected = { "a", "b", "c", "a", "b", "c", "a", "b", "c" };
        Assertions.assertArrayEquals(expected, result);

        // Test single element array
        String[] single = { "x" };
        result = Array.repeat(single, 5);
        Assertions.assertEquals(5, result.length);
        for (int i = 0; i < 5; i++) {
            Assertions.assertEquals("x", result[i]);
        }

        // Test empty array
        String[] empty = {};
        result = Array.repeat(empty, 10);
        Assertions.assertEquals(0, result.length);

        // Test n = 0
        result = Array.repeat(original, 0);
        Assertions.assertEquals(0, result.length);

        // Test n = 1
        result = Array.repeat(original, 1);
        Assertions.assertArrayEquals(original, result);

        // Test array with null elements
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

    //    @Test
    //    public void testRepeatStringArrayOverflow() {
    //        String[] array = new String[Integer.MAX_VALUE / 2 + 1];
    //        Assertions.assertThrows(IllegalArgumentException.class, () -> {
    //            Array.repeat(array, 2);
    //        });
    //    }

    // Tests for repeat(T element, int n) - deprecated generic method
    @Test
    public void testRepeatGenericElement() {
        // Test with Integer
        Integer[] result = Array.repeat(Integer.valueOf(42), 5);
        Assertions.assertEquals(5, result.length);
        for (int i = 0; i < 5; i++) {
            Assertions.assertEquals(Integer.valueOf(42), result[i]);
        }

        // Test with custom object
        TestObject testObj = new TestObject("test");
        TestObject[] objResult = Array.repeat(testObj, 3);
        Assertions.assertEquals(3, objResult.length);
        for (int i = 0; i < 3; i++) {
            Assertions.assertSame(testObj, objResult[i]);
        }

        // Test with n = 0
        result = Array.repeat(Integer.valueOf(100), 0);
        Assertions.assertEquals(0, result.length);

        // Test with n = 1
        result = Array.repeat(Integer.valueOf(200), 1);
        Assertions.assertEquals(1, result.length);
        Assertions.assertEquals(Integer.valueOf(200), result[0]);
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

    // Tests for repeat(T element, int n, Class<? extends T> elementClass)
    @Test
    public void testRepeatGenericElementWithClass() {
        // Test with Integer
        Integer[] result = Array.repeat(42, 5, Integer.class);
        Assertions.assertEquals(5, result.length);
        for (int i = 0; i < 5; i++) {
            Assertions.assertEquals(Integer.valueOf(42), result[i]);
        }

        // Test with null element
        result = Array.repeat((Integer) null, 3, Integer.class);
        Assertions.assertEquals(3, result.length);
        for (int i = 0; i < 3; i++) {
            Assertions.assertNull(result[i]);
        }

        // Test with custom object
        TestObject testObj = new TestObject("test");
        TestObject[] objResult = Array.repeat(testObj, 3, TestObject.class);
        Assertions.assertEquals(3, objResult.length);
        for (int i = 0; i < 3; i++) {
            Assertions.assertSame(testObj, objResult[i]);
        }

        // Test with n = 0
        result = Array.repeat(100, 0, Integer.class);
        Assertions.assertEquals(0, result.length);

        // Test with n = 1
        result = Array.repeat(200, 1, Integer.class);
        Assertions.assertEquals(1, result.length);
        Assertions.assertEquals(Integer.valueOf(200), result[0]);
    }

    @Test
    public void testRepeatGenericElementWithClassNegativeN() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            Array.repeat(42, -1, Integer.class);
        });
    }

    // Tests for repeat(T[] a, int n, Class<? extends T> elementClass)
    @Test
    public void testRepeatGenericArrayWithClass() {
        // Test normal case
        Integer[] original = { 1, 2, 3 };
        Integer[] result = Array.repeat(original, 3, Integer.class);
        Assertions.assertEquals(9, result.length);
        Integer[] expected = { 1, 2, 3, 1, 2, 3, 1, 2, 3 };
        Assertions.assertArrayEquals(expected, result);

        // Test single element array
        Integer[] single = { 0 };
        result = Array.repeat(single, 5, Integer.class);
        Assertions.assertEquals(5, result.length);
        for (int i = 0; i < 5; i++) {
            Assertions.assertEquals(Integer.valueOf(0), result[i]);
        }

        // Test empty array
        Integer[] empty = {};
        result = Array.repeat(empty, 10, Integer.class);
        Assertions.assertEquals(0, result.length);

        // Test n = 0
        result = Array.repeat(original, 0, Integer.class);
        Assertions.assertEquals(0, result.length);

        // Test n = 1
        result = Array.repeat(original, 1, Integer.class);
        Assertions.assertArrayEquals(original, result);

        // Test with custom objects
        TestObject[] objArray = { new TestObject("a"), new TestObject("b") };
        TestObject[] objResult = Array.repeat(objArray, 2, TestObject.class);
        Assertions.assertEquals(4, objResult.length);
        Assertions.assertEquals(objArray[0], objResult[0]);
        Assertions.assertEquals(objArray[1], objResult[1]);
        Assertions.assertEquals(objArray[0], objResult[2]);
        Assertions.assertEquals(objArray[1], objResult[3]);
    }

    @Test
    public void testRepeatGenericArrayWithClassNegativeN() {
        Integer[] array = { 1, 2 };
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            Array.repeat(array, -1, Integer.class);
        });
    }

    //    @Test
    //    public void testRepeatGenericArrayWithClassOverflow() {
    //        Integer[] array = new Integer[Integer.MAX_VALUE / 2 + 1];
    //        Assertions.assertThrows(IllegalArgumentException.class, () -> {
    //            Array.repeat(array, 2, Integer.class);
    //        });
    //    }

    // Tests for repeatNonNull(T element, int n)
    @Test
    public void testRepeatNonNullElement() {
        // Test with Integer
        Integer[] result = Array.repeatNonNull(42, 5);
        Assertions.assertEquals(5, result.length);
        for (int i = 0; i < 5; i++) {
            Assertions.assertEquals(Integer.valueOf(42), result[i]);
        }

        // Test with custom object
        TestObject testObj = new TestObject("test");
        TestObject[] objResult = Array.repeatNonNull(testObj, 3);
        Assertions.assertEquals(3, objResult.length);
        for (int i = 0; i < 3; i++) {
            Assertions.assertSame(testObj, objResult[i]);
        }

        // Test with n = 0
        result = Array.repeatNonNull(100, 0);
        Assertions.assertEquals(0, result.length);

        // Test with n = 1
        result = Array.repeatNonNull(200, 1);
        Assertions.assertEquals(1, result.length);
        Assertions.assertEquals(Integer.valueOf(200), result[0]);
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

    // Helper class for testing
    private static class TestObject {
        private final String value;

        public TestObject(String value) {
            this.value = value;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null || getClass() != obj.getClass())
                return false;
            TestObject that = (TestObject) obj;
            return value.equals(that.value);
        }

        @Override
        public int hashCode() {
            return value.hashCode();
        }
    }
}
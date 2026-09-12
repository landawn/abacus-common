package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.function.UnaryOperator;

import org.junit.jupiter.api.Test;

public class NCopyTest extends NTestSupport {

    @Test
    public void testCopyThenSetAll() throws Exception {
        String[] original = { "a", "b", "c" };
        String[] result = N.copyThenSetAll(original, i -> "Item" + i);
        assertArrayEquals(new String[] { "Item0", "Item1", "Item2" }, result);
        assertArrayEquals(new String[] { "a", "b", "c" }, original);
        assertNull(N.copyThenSetAll((String[]) null, i -> "x"));
        assertEquals(0, N.copyThenSetAll(new String[0], i -> "x").length);

        Integer[] ints = { 1, 2, 3, 4 };
        assertArrayEquals(new Integer[] { 0, 10, 20, 30 }, N.copyThenSetAll(ints, i -> i * 10));
        assertArrayEquals(new Integer[] { 1, 2, 3, 4 }, ints);

        String[] converted = N.copyThenSetAll(original, (Throwables.IntObjFunction<String, String, IOException>) (idx, val) -> val.toUpperCase() + idx);
        assertArrayEquals(new String[] { "A0", "B1", "C2" }, converted);
        assertArrayEquals(new String[] { "a", "b", "c" }, original);
        assertThrows(IOException.class, () -> N.copyThenSetAll(original, (idx, val) -> {
            if (idx == 1) {
                throw new IOException("Test");
            }
            return val;
        }));
        assertNull(N.copyThenSetAll((String[]) null, (idx, val) -> val));
        assertEquals(0, N.copyThenSetAll(new String[0], (idx, val) -> val).length);

        String[] withVal = { "a", "b", "c", "d" };
        assertArrayEquals(new String[] { "a0", "b1", "c2", "d3" }, N.copyThenSetAll(withVal, (i, val) -> val + i));
        assertArrayEquals(new String[] { "a", "b", "c", "d" }, withVal);
    }

    @Test
    public void testCopyThenReplaceAll() {
        String[] original = { "hello", "world", "test" };
        String[] result = N.copyThenReplaceAll(original, String::toUpperCase);
        assertArrayEquals(new String[] { "HELLO", "WORLD", "TEST" }, result);
        assertArrayEquals(new String[] { "hello", "world", "test" }, original);
        assertNull(N.copyThenReplaceAll((String[]) null, String::toUpperCase));
        assertEquals(0, N.copyThenReplaceAll(new String[0], x -> x).length);

        UnaryOperator<String> operator = s -> "a".equals(s) ? "x" : s;
        assertArrayEquals(new String[] { "x", "b", "x" }, N.copyThenReplaceAll(new String[] { "a", "b", "a" }, operator));

        Integer[] ints = { 1, 2, 3, 4 };
        assertArrayEquals(new Integer[] { 2, 4, 6, 8 }, N.copyThenReplaceAll(ints, val -> val * 2));
        assertArrayEquals(new Integer[] { 1, 2, 3, 4 }, ints);
    }

    @Test
    public void testCopyThenUpdateAll() throws Exception {
        String[] original = { "a", "b", "c" };
        String[] result = N.copyThenUpdateAll(original, (Throwables.UnaryOperator<String, IOException>) String::toUpperCase);
        assertArrayEquals(new String[] { "A", "B", "C" }, result);
        assertArrayEquals(new String[] { "a", "b", "c" }, original);
        assertThrows(IOException.class, () -> N.copyThenUpdateAll(original, s -> {
            if ("b".equals(s)) {
                throw new IOException("Test");
            }
            return s;
        }));
        assertNull(N.copyThenUpdateAll((String[]) null, String::toUpperCase));
        assertEquals(0, N.copyThenUpdateAll(new String[0], String::toUpperCase).length);
    }

    @Test
    public void testCopyOfAndCopyOfRange() {
        assertArrayEquals(new boolean[] { true, true }, CommonUtil.copyOfRange(new boolean[] { true, true, false }, 0, 2));
        assertArrayEquals(new char[] { 1, 2 }, CommonUtil.copyOfRange(new char[] { 1, 2, 3 }, 0, 2));
        assertArrayEquals(new byte[] { 1, 2 }, CommonUtil.copyOfRange(new byte[] { 1, 2, 3 }, 0, 2));
        assertArrayEquals(new short[] { 1, 2 }, CommonUtil.copyOfRange(new short[] { 1, 2, 3 }, 0, 2));
        assertArrayEquals(new int[] { 1, 2 }, CommonUtil.copyOfRange(new int[] { 1, 2, 3 }, 0, 2));
        assertArrayEquals(new long[] { 1, 2 }, CommonUtil.copyOfRange(new long[] { 1, 2, 3 }, 0, 2));
        assertArrayEquals(new float[] { 1, 2 }, CommonUtil.copyOfRange(new float[] { 1, 2, 3 }, 0, 2), DELTAf);
        assertArrayEquals(new double[] { 1, 2 }, CommonUtil.copyOfRange(new double[] { 1, 2, 3 }, 0, 2), DELTA);
        assertArrayEquals(new Object[] { 1, 2 }, CommonUtil.copyOfRange(new Object[] { 1, 2, 3 }, 0, 2));
        assertArrayEquals(new Object[] { 1, 2 }, CommonUtil.copyOfRange(new Integer[] { 1, 2, 3 }, 0, 2, Object[].class));

        assertArrayEquals(new int[] { 2, 3, 4 }, CommonUtil.copyOfRange(new int[] { 1, 2, 3, 4, 5 }, 1, 4));
        assertArrayEquals(new int[] {}, CommonUtil.copyOfRange(new int[] { 1, 2, 3 }, 1, 1));
        assertTrue(CommonUtil.equals(CommonUtil.copyOfRange(new int[] { 0, 1, 2, 3, 4, 5, 6 }, 1, 6, 2), Array.of(1, 3, 5)));
        assertTrue(CommonUtil.equals(CommonUtil.copyOfRange(new long[] { 0, 1, 2, 3, 4, 5, 6 }, 1, 6, 2), Array.of(1L, 3, 5)));
        assertTrue(CommonUtil.equals(CommonUtil.copyOfRange(new float[] { 0, 1, 2, 3, 4, 5, 6 }, 1, 6, 2), Array.of(1F, 3, 5)));
        assertTrue(CommonUtil.equals(CommonUtil.copyOfRange(new double[] { 0, 1, 2, 3, 4, 5, 6 }, 1, 6, 2), Array.of(1D, 3, 5)));

        boolean[] copied = CommonUtil.copyOf(new boolean[] { true, true, false }, 9);
        assertEquals(9, copied.length);
        assertTrue(copied[0]);
        assertFalse(copied[8]);
        assertEquals(9, CommonUtil.copyOf(new char[] { 1, 2, 3 }, 9).length);
        assertEquals(9, CommonUtil.copyOf(new byte[] { 1, 2, 3 }, 9).length);
        assertEquals(9, CommonUtil.copyOf(new short[] { 1, 2, 3 }, 9).length);
        assertEquals(9, CommonUtil.copyOf(new int[] { 1, 2, 3 }, 9).length);
        assertEquals(9, CommonUtil.copyOf(new long[] { 1, 2, 3 }, 9).length);
        assertEquals(9, CommonUtil.copyOf(new float[] { 1, 2, 3 }, 9).length);
        assertEquals(9, CommonUtil.copyOf(new double[] { 1, 2, 3 }, 9).length);
        assertEquals(9, CommonUtil.copyOf(new Object[] { 1, 2, 3 }, 9).length);
        Object[] typed = CommonUtil.copyOf(new String[] { "1", "2", "3" }, 9, Object[].class);
        assertEquals(9, typed.length);
        assertEquals("1", typed[0]);
        assertEquals(3, CommonUtil.copyOf(new int[] { 1, 2, 3 }, 3).length);
    }
}

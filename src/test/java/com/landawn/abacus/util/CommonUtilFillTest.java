package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

public class CommonUtilFillTest extends CommonUtilTestSupport {

    @Test
    public void testFill_arrays() {
        boolean[] bools = new boolean[5];
        CommonUtil.fill(bools, true);
        assertArrayEquals(new boolean[] { true, true, true, true, true }, bools);
        CommonUtil.fill(bools, 1, 3, false);
        assertArrayEquals(new boolean[] { true, false, false, true, true }, bools);
        CommonUtil.fill(new boolean[0], true);

        boolean[] boolRange = new boolean[5];
        CommonUtil.fill(boolRange, 1, 4, true);
        assertArrayEquals(new boolean[] { false, true, true, true, false }, boolRange);

        char[] chars = new char[5];
        CommonUtil.fill(chars, 'x');
        assertArrayEquals(new char[] { 'x', 'x', 'x', 'x', 'x' }, chars);
        CommonUtil.fill(chars, 1, 4, 'y');
        assertArrayEquals(new char[] { 'x', 'y', 'y', 'y', 'x' }, chars);

        byte[] bytes = new byte[5];
        CommonUtil.fill(bytes, (byte) 42);
        assertArrayEquals(new byte[] { 42, 42, 42, 42, 42 }, bytes);
        CommonUtil.fill(bytes, 2, 5, (byte) 9);
        assertArrayEquals(new byte[] { 42, 42, 9, 9, 9 }, bytes);

        short[] shorts = new short[5];
        CommonUtil.fill(shorts, (short) 100);
        assertArrayEquals(new short[] { 100, 100, 100, 100, 100 }, shorts);
        CommonUtil.fill(shorts, 0, 3, (short) 1);
        assertArrayEquals(new short[] { 1, 1, 1, 100, 100 }, shorts);

        int[] ints = new int[5];
        CommonUtil.fill(ints, 42);
        assertArrayEquals(new int[] { 42, 42, 42, 42, 42 }, ints);
        CommonUtil.fill(ints, 1, 3, 99);
        assertArrayEquals(new int[] { 42, 99, 99, 42, 42 }, ints);

        long[] longs = new long[5];
        CommonUtil.fill(longs, 1000L);
        assertArrayEquals(new long[] { 1000L, 1000L, 1000L, 1000L, 1000L }, longs);
        CommonUtil.fill(longs, 2, 4, 7L);
        assertArrayEquals(new long[] { 1000L, 1000L, 7L, 7L, 1000L }, longs);

        float[] floats = new float[5];
        CommonUtil.fill(floats, 3.14f);
        assertArrayEquals(new float[] { 3.14f, 3.14f, 3.14f, 3.14f, 3.14f }, floats);
        CommonUtil.fill(floats, 0, 2, 1.0f);
        assertArrayEquals(new float[] { 1.0f, 1.0f, 3.14f, 3.14f, 3.14f }, floats);

        double[] doubles = new double[5];
        CommonUtil.fill(doubles, 2.718);
        assertArrayEquals(new double[] { 2.718, 2.718, 2.718, 2.718, 2.718 }, doubles);
        CommonUtil.fill(doubles, 3, 5, 0.5);
        assertArrayEquals(new double[] { 2.718, 2.718, 2.718, 0.5, 0.5 }, doubles);

        String[] strings = new String[5];
        CommonUtil.fill(strings, "test");
        assertArrayEquals(new String[] { "test", "test", "test", "test", "test" }, strings);
        CommonUtil.fill(strings, 1, 4, "x");
        assertArrayEquals(new String[] { "test", "x", "x", "x", "test" }, strings);
        CommonUtil.fill(new String[0], "test");
    }

    @Test
    public void testFill_list() {
        List<String> list = new ArrayList<>(Arrays.asList("a", "b", "c"));
        CommonUtil.fill(list, "x");
        assertEquals(Arrays.asList("x", "x", "x"), list);

        List<String> ranged = new ArrayList<>(Arrays.asList("a", "b", "c", "d", "e"));
        CommonUtil.fill(ranged, 1, 4, "x");
        assertEquals(Arrays.asList("a", "x", "x", "x", "e"), ranged);

        List<String> extending = new ArrayList<>(Arrays.asList("a", "b"));
        CommonUtil.fill(extending, 0, 5, "x");
        assertEquals(Arrays.asList("x", "x", "x", "x", "x"), extending);

        List<String> gaps = new ArrayList<>();
        CommonUtil.fill(gaps, 2, 5, "x");
        assertEquals(5, gaps.size());
        assertNull(gaps.get(0));
        assertNull(gaps.get(1));
        assertEquals(Arrays.asList(null, null, "x", "x", "x"), gaps);
    }
}

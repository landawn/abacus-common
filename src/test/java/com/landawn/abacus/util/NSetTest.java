package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import org.junit.jupiter.api.Test;

public class NSetTest extends NTestSupport {

    @Test
    public void testSetAll_arrays() {
        boolean[] booleans = new boolean[5];
        N.setAll(booleans, i -> i % 2 == 0);
        assertArrayEquals(new boolean[] { true, false, true, false, true }, booleans);

        char[] chars = new char[3];
        N.setAll(chars, i -> (char) ('a' + i));
        assertArrayEquals(new char[] { 'a', 'b', 'c' }, chars);

        byte[] bytes = new byte[4];
        N.setAll(bytes, i -> (byte) (i * 10));
        assertArrayEquals(new byte[] { 0, 10, 20, 30 }, bytes);

        short[] shorts = new short[3];
        N.setAll(shorts, i -> (short) (i + 100));
        assertArrayEquals(new short[] { 100, 101, 102 }, shorts);

        int[] ints = new int[5];
        N.setAll(ints, i -> i * i);
        assertArrayEquals(new int[] { 0, 1, 4, 9, 16 }, ints);

        long[] longs = new long[4];
        N.setAll(longs, i -> (long) i * 1000);
        assertArrayEquals(new long[] { 0L, 1000L, 2000L, 3000L }, longs);

        float[] floats = new float[3];
        N.setAll(floats, i -> i * 1.5f);
        assertArrayEquals(new float[] { 0.0f, 1.5f, 3.0f }, floats);

        double[] doubles = new double[3];
        N.setAll(doubles, i -> i * 2.5);
        assertArrayEquals(new double[] { 0.0, 2.5, 5.0 }, doubles);

        String[] strings = new String[3];
        N.setAll(strings, i -> "Item" + i);
        assertArrayEquals(new String[] { "Item0", "Item1", "Item2" }, strings);

        assertDoesNotThrow(() -> N.setAll((boolean[]) null, i -> true));
        assertDoesNotThrow(() -> N.setAll((char[]) null, i -> 'x'));
        assertDoesNotThrow(() -> N.setAll((byte[]) null, i -> (byte) i));
        assertDoesNotThrow(() -> N.setAll((short[]) null, i -> (short) i));
        assertDoesNotThrow(() -> N.setAll((int[]) null, i -> i));
        assertDoesNotThrow(() -> N.setAll((long[]) null, i -> (long) i));
        assertDoesNotThrow(() -> N.setAll((float[]) null, i -> (float) i));
        assertDoesNotThrow(() -> N.setAll((double[]) null, i -> (double) i));
        assertDoesNotThrow(() -> N.setAll((String[]) null, i -> "x"));
        N.setAll(new String[0], i -> "x");
    }

    @Test
    public void testSetAll_list() {
        List<String> list = new ArrayList<>(Arrays.asList("", "", ""));
        N.setAll(list, i -> "Value" + i);
        assertEquals(Arrays.asList("Value0", "Value1", "Value2"), list);

        List<Integer> ints = new ArrayList<>(Arrays.asList(0, 0, 0, 0));
        N.setAll(ints, i -> i * 10);
        assertEquals(Arrays.asList(0, 10, 20, 30), ints);

        List<Integer> empty = new ArrayList<>();
        N.setAll(empty, i -> i);
        assertEquals(0, empty.size());
        assertDoesNotThrow(() -> N.setAll((List<String>) null, i -> "x"));

        List<String> linked = new LinkedList<>(Arrays.asList("a", "b", "c"));
        N.setAll(linked, i -> "item" + i);
        assertEquals(Arrays.asList("item0", "item1", "item2"), linked);

        LinkedList<String> large = new LinkedList<>(Arrays.asList("a", "b", "c", "d", "e", "f", "g", "h", "i", "j", "k", "l"));
        N.setAll(large, i -> "v" + i);
        assertEquals("v0", large.get(0));
        assertEquals("v11", large.get(11));

        N.setAll(large, (i, s) -> i + ":" + s.substring(1));
        assertEquals("0:0", large.get(0));
        assertEquals("11:11", large.get(11));
    }

    @Test
    public void testSetAll_intObjFunction() throws Exception {
        String[] arr = { "a", "b", "c", "d" };
        N.setAll(arr, (i, val) -> val + i);
        assertArrayEquals(new String[] { "a0", "b1", "c2", "d3" }, arr);

        List<String> list = new ArrayList<>(Arrays.asList("a", "b", "c", "d"));
        N.setAll(list, (i, val) -> val + i);
        assertEquals(Arrays.asList("a0", "b1", "c2", "d3"), list);

        List<String> linked = new LinkedList<>(Arrays.asList("a", "b", "c"));
        N.setAll(linked, (i, val) -> val.toUpperCase() + i);
        assertEquals(Arrays.asList("A0", "B1", "C2"), linked);

        String[] throwing = { "a", "b", "c" };
        Throwables.IntObjFunction<String, String, IOException> converter = (idx, val) -> {
            if ("b".equals(val)) {
                throw new IOException("Converter Exception");
            }
            return val.toUpperCase() + idx;
        };
        assertThrows(IOException.class, () -> N.setAll(throwing, converter));
        assertEquals("A0", throwing[0]);
        assertEquals("b", throwing[1]);

        String[] arr2 = { "x", "y" };
        Throwables.IntObjFunction<String, String, IOException> noEx = (idx, val) -> val.toUpperCase() + idx;
        N.setAll(arr2, noEx);
        assertArrayEquals(new String[] { "X0", "Y1" }, arr2);
        N.setAll((String[]) null, noEx);
        N.setAll(new String[0], noEx);

        List<String> throwingList = new ArrayList<>(Arrays.asList("a", "b", "c"));
        assertThrows(IOException.class, () -> N.setAll(throwingList, converter));
        assertEquals("A0", throwingList.get(0));
        assertEquals("b", throwingList.get(1));

        N.setAll((List<String>) null, noEx);
        N.setAll(new ArrayList<String>(), noEx);
    }
}

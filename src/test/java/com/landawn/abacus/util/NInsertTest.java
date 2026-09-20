package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

public class NInsertTest extends NTestSupport {

    @Test
    public void testInsertAllShiftsTailByInsertedCount() {
        final List<String> varargsList = new ArrayList<>(List.of("a", "d", "e"));
        assertTrue(N.insertAll(varargsList, 1, "b", "c"));
        assertEquals(List.of("a", "b", "c", "d", "e"), varargsList);

        final List<String> collectionList = new ArrayList<>(List.of("a", "d", "e"));
        assertTrue(N.insertAll(collectionList, 1, List.of("b", "c")));
        assertEquals(List.of("a", "b", "c", "d", "e"), collectionList);
    }

    @Test
    public void testInsert() {
        assertArrayEquals(new boolean[] { true, false, true }, N.insert(new boolean[] { false, true }, 0, true));
        assertArrayEquals(new char[] { 'a', 'x', 'b', 'c' }, N.insert(new char[] { 'a', 'b', 'c' }, 1, 'x'));
        assertArrayEquals(new byte[] { 1, 9, 2, 3 }, N.insert(new byte[] { 1, 2, 3 }, 1, (byte) 9));
        assertArrayEquals(new short[] { 1, 9, 2, 3 }, N.insert(new short[] { 1, 2, 3 }, 1, (short) 9));
        assertArrayEquals(new int[] { 1, 5, 2 }, N.insert(new int[] { 1, 2 }, 1, 5));
        assertArrayEquals(new long[] { 1L, 9L, 2L, 3L }, N.insert(new long[] { 1L, 2L, 3L }, 1, 9L));
        assertArrayEquals(new float[] { 1.0f, 9.0f, 2.0f, 3.0f }, N.insert(new float[] { 1.0f, 2.0f, 3.0f }, 1, 9.0f), DELTAf);
        assertArrayEquals(new double[] { 1.0, 9.0, 2.0, 3.0 }, N.insert(new double[] { 1.0, 2.0, 3.0 }, 1, 9.0), DELTA);
        assertArrayEquals(new Integer[] { 1, 9, 2, 3 }, N.insert(new Integer[] { 1, 2, 3 }, 1, 9));
        assertArrayEquals(new String[] { "hello" }, N.insert((String[]) null, 0, "hello"));
        assertArrayEquals(new String[] { "z", "a", "b" }, N.insert(new String[] { "a", "b" }, 0, "z"));
        assertArrayEquals(new boolean[] { true }, N.insert(new boolean[0], 0, true));
        assertThrows(IndexOutOfBoundsException.class, () -> N.insert(new boolean[] { true }, 2, false));
        assertThrows(IllegalArgumentException.class, () -> N.insert((Integer[]) null, 0, 1));
        assertThrows(IndexOutOfBoundsException.class, () -> N.insert(new int[] { 1, 2, 3 }, 4, 9));

        assertEquals("ab", N.insert("b", 0, "a"));
        assertEquals("hello world", N.insert("helloworld", 5, " "));
        assertEquals("test", N.insert("test", 4, null));
        assertEquals("abc", N.insert("", 0, "abc"));
        assertThrows(IndexOutOfBoundsException.class, () -> N.insert("abc", 4, "d"));
    }

    @Test
    public void testInsertAll() {
        assertArrayEquals(new boolean[] { true, true, false, false }, N.insertAll(new boolean[] { true, false }, 1, true, false));
        assertArrayEquals(new char[] { 'a', 'x', 'y', 'b' }, N.insertAll(new char[] { 'a', 'b' }, 1, 'x', 'y'));
        assertArrayEquals(new byte[] { 1, 8, 9, 2 }, N.insertAll(new byte[] { 1, 2 }, 1, (byte) 8, (byte) 9));
        assertArrayEquals(new short[] { 1, 8, 9, 2 }, N.insertAll(new short[] { 1, 2 }, 1, (short) 8, (short) 9));
        assertArrayEquals(new int[] { 1, 5, 6, 2, 3 }, N.insertAll(new int[] { 1, 2, 3 }, 1, 5, 6));
        assertArrayEquals(new long[] { 1L, 8L, 9L, 2L }, N.insertAll(new long[] { 1L, 2L }, 1, 8L, 9L));
        assertArrayEquals(new float[] { 1.0f, 8.0f, 9.0f, 2.0f }, N.insertAll(new float[] { 1.0f, 2.0f }, 1, 8.0f, 9.0f), DELTAf);
        assertArrayEquals(new double[] { 1.0, 8.0, 9.0, 2.0 }, N.insertAll(new double[] { 1.0, 2.0 }, 1, 8.0, 9.0), DELTA);
        assertArrayEquals(new String[] { "a", "x", "y", "b" }, N.insertAll(new String[] { "a", "b" }, 1, "x", "y"));
        assertArrayEquals(new Integer[] { 5, 6, 1, 2, 3 }, N.insertAll(new Integer[] { 1, 2, 3 }, 0, 5, 6));
        assertArrayEquals(new boolean[] { true, false }, N.insertAll(new boolean[0], 0, true, false));
        assertArrayEquals(new char[] { 'x', 'y' }, N.insertAll(new char[0], 0, 'x', 'y'));
        assertArrayEquals(new byte[] { 3, 4 }, N.insertAll(new byte[0], 0, (byte) 3, (byte) 4));

        boolean[] booleans = { true, false, true };
        boolean[] cloned = N.insertAll(booleans, 1, new boolean[0]);
        assertArrayEquals(booleans, cloned);
        assertNotSame(booleans, cloned);

        assertArrayEquals(new boolean[0], N.insertAll((boolean[]) null, 0, (boolean[]) null));
        assertArrayEquals(new int[0], N.insertAll((int[]) null, 0, (int[]) null));
        assertArrayEquals(new String[0], N.insertAll((String[]) null, 0, (String[]) null));
        assertThrows(IllegalArgumentException.class, () -> N.insertAll((Integer[]) null, 0, 1, 2));

        Number[] source = new Number[0];
        Number[] result = N.insertAll(source, 0, new Integer[] { 1, 2 });
        assertSame(Number[].class, result.getClass());
        assertArrayEquals(new Number[] { 1, 2 }, result);
        Number[] unchanged = N.insertAll(source, 0, (Number[]) null);
        assertSame(Number[].class, unchanged.getClass());
        assertNotSame(source, unchanged);

        List<String> list = new ArrayList<>(Arrays.asList("a", "d"));
        assertTrue(N.insertAll(list, 1, "b", "c"));
        assertEquals(Arrays.asList("a", "b", "c", "d"), list);
        assertFalse(N.insertAll(list, 1));
        assertTrue(N.insertAll(list, 1, Arrays.asList("x")));
        assertFalse(N.insertAll(list, 1, Collections.emptyList()));

        List<String> self = new ArrayList<>(Arrays.asList("a", "b"));
        assertTrue(N.insertAll(self, 1, self));
        assertEquals(Arrays.asList("a", "a", "b", "b"), self);

        List<String> view = new ArrayList<>(Arrays.asList("a", "b", "c"));
        assertTrue(N.insertAll(view, 0, view.subList(1, 3)));
        assertEquals(Arrays.asList("b", "c", "a", "b", "c"), view);

        assertThrows(IllegalArgumentException.class, () -> N.insertAll((List<String>) null, 0, "a"));
        assertThrows(IndexOutOfBoundsException.class, () -> N.insertAll(list, 10, "x"));
    }

    @Test
    public void testInsertAll_nullArrayContract() {
        // documented contract: the fixed-component-type overloads treat a null array as empty, while the
        // generic ones reject null because they take the result's runtime component type from the array.
        assertArrayEquals(new boolean[] { true }, N.insertAll((boolean[]) null, 0, true));
        assertArrayEquals(new char[] { 'x', 'y' }, N.insertAll((char[]) null, 0, 'x', 'y'));
        assertArrayEquals(new byte[] { 1 }, N.insertAll((byte[]) null, 0, (byte) 1));
        assertArrayEquals(new short[] { 1 }, N.insertAll((short[]) null, 0, (short) 1));
        assertArrayEquals(new int[] { 1, 2 }, N.insertAll((int[]) null, 0, 1, 2));
        assertArrayEquals(new long[] { 1L }, N.insertAll((long[]) null, 0, 1L));
        assertArrayEquals(new float[] { 1.0f }, N.insertAll((float[]) null, 0, 1.0f), DELTAf);
        assertArrayEquals(new double[] { 1.0 }, N.insertAll((double[]) null, 0, 1.0), DELTA);
        assertArrayEquals(new String[] { "a" }, N.insertAll((String[]) null, 0, "a"));

        assertArrayEquals(new int[] { 7 }, N.insert((int[]) null, 0, 7));
        assertArrayEquals(new char[] { 'z' }, N.insert((char[]) null, 0, 'z'));
        assertArrayEquals(new double[] { 1.0 }, N.insert((double[]) null, 0, 1.0), DELTA);

        // the position index is still validated against the empty array
        assertThrows(IndexOutOfBoundsException.class, () -> N.insertAll((int[]) null, 1, 1, 2));
        assertThrows(IndexOutOfBoundsException.class, () -> N.insertAll((String[]) null, 1, "a"));
        assertThrows(IndexOutOfBoundsException.class, () -> N.insert((int[]) null, 1, 1));

        // the generic overloads reject a null array outright
        assertThrows(IllegalArgumentException.class, () -> N.insertAll((Integer[]) null, 0, 1));
        assertThrows(IllegalArgumentException.class, () -> N.insert((Integer[]) null, 0, 1));
    }

    @Test
    public void testInsertAll_oversizedResultReportsArithmeticException() {
        org.junit.jupiter.api.Assumptions.assumeTrue(Runtime.getRuntime().maxMemory() > 3L * 1024 * 1024 * 1024, "needs a heap large enough for a 2GB byte[]");

        final byte[] huge = new byte[Integer.MAX_VALUE - 2];
        assertThrows(ArithmeticException.class, () -> N.insertAll(huge, 0, new byte[10]));
    }
}

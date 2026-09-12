package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.DoubleUnaryOperator;
import java.util.function.IntUnaryOperator;
import java.util.function.LongUnaryOperator;
import java.util.function.UnaryOperator;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.util.function.BooleanUnaryOperator;
import com.landawn.abacus.util.function.ByteUnaryOperator;
import com.landawn.abacus.util.function.CharUnaryOperator;
import com.landawn.abacus.util.function.FloatUnaryOperator;
import com.landawn.abacus.util.function.Predicate;
import com.landawn.abacus.util.function.ShortUnaryOperator;

public class NReplaceTest extends NTestSupport {

    @Test
    public void testReplaceIf() {
        boolean[] bools = { true, false, true, false, true };
        assertEquals(3, N.replaceIf(bools, x -> x, false));
        assertArrayEquals(new boolean[] { false, false, false, false, false }, bools);
        assertEquals(0, N.replaceIf(new boolean[] { true, false, true }, x -> false, true));
        assertEquals(0, N.replaceIf((boolean[]) null, x -> true, false));
        assertEquals(0, N.replaceIf(new boolean[0], x -> true, false));

        char[] chars = { 'a', 'b', 'c', 'd', 'e' };
        assertEquals(2, N.replaceIf(chars, x -> x < 'c', 'x'));
        assertArrayEquals(new char[] { 'x', 'x', 'c', 'd', 'e' }, chars);
        assertEquals(0, N.replaceIf((char[]) null, x -> true, 'x'));

        byte[] bytes = { 1, 2, 3, 4, 5 };
        assertEquals(2, N.replaceIf(bytes, x -> x % 2 == 0, (byte) 0));
        assertArrayEquals(new byte[] { 1, 0, 3, 0, 5 }, bytes);
        assertEquals(0, N.replaceIf((byte[]) null, x -> true, (byte) 0));

        short[] shorts = { 10, 20, 30, 40, 50 };
        assertEquals(3, N.replaceIf(shorts, x -> x > 25, (short) 99));
        assertArrayEquals(new short[] { 10, 20, 99, 99, 99 }, shorts);
        assertEquals(0, N.replaceIf((short[]) null, x -> true, (short) 0));

        int[] ints = { 1, 2, 3, 4, 5, 6, 7, 8 };
        assertEquals(2, N.replaceIf(ints, x -> x % 3 == 0, 0));
        assertArrayEquals(new int[] { 1, 2, 0, 4, 5, 0, 7, 8 }, ints);
        assertEquals(0, N.replaceIf((int[]) null, x -> true, 0));

        long[] longs = { 100L, 200L, 300L, 400L };
        assertEquals(2, N.replaceIf(longs, x -> x >= 300L, 0L));
        assertArrayEquals(new long[] { 100L, 200L, 0L, 0L }, longs);
        assertEquals(0, N.replaceIf((long[]) null, x -> true, 0L));

        float[] floats = { 1.5f, 2.5f, 3.5f, 4.5f };
        assertEquals(2, N.replaceIf(floats, x -> x > 3.0f, 0.0f));
        assertArrayEquals(new float[] { 1.5f, 2.5f, 0.0f, 0.0f }, floats);
        assertEquals(0, N.replaceIf((float[]) null, x -> true, 0.0f));

        double[] doubles = { 1.0, 2.0, 3.0, 4.0, 5.0 };
        assertEquals(2, N.replaceIf(doubles, x -> x < 3.0, 99.0));
        assertArrayEquals(new double[] { 99.0, 99.0, 3.0, 4.0, 5.0 }, doubles);
        assertEquals(0, N.replaceIf((double[]) null, x -> true, 0.0));

        String[] words = { "apple", "banana", "cherry", "date" };
        assertEquals(2, N.replaceIf(words, x -> x.startsWith("b") || x.startsWith("c"), "REPLACED"));
        assertArrayEquals(new String[] { "apple", "REPLACED", "REPLACED", "date" }, words);
        assertEquals(0, N.replaceIf((String[]) null, x -> true, "x"));
        assertEquals(0, N.replaceIf(new String[0], x -> true, "x"));

        Integer[] withNull = { 1, null, 1, 3 };
        assertEquals(1, N.replaceIf(withNull, (Predicate<Integer>) Objects::isNull, 0));
        assertArrayEquals(new Integer[] { 1, 0, 1, 3 }, withNull);

        List<String> list = new ArrayList<>(Arrays.asList("apple", "banana", "cherry", "date"));
        assertEquals(2, N.replaceIf(list, x -> x.length() > 5, "LONG"));
        assertEquals(Arrays.asList("apple", "LONG", "LONG", "date"), list);
        assertEquals(0, N.replaceIf((List<String>) null, x -> true, "x"));
        assertEquals(0, N.replaceIf(new ArrayList<String>(), x -> true, "x"));
    }

    @Test
    public void testReplaceAll() {
        boolean[] bools = { true, false, true, false, true };
        assertEquals(3, N.replaceAll(bools, true, false));
        assertArrayEquals(new boolean[] { false, false, false, false, false }, bools);
        assertEquals(0, N.replaceAll(new boolean[] { true, true, true }, false, true));
        assertEquals(0, N.replaceAll((boolean[]) null, true, false));

        char[] chars = { 'a', 'b', 'a', 'c', 'a' };
        assertEquals(3, N.replaceAll(chars, 'a', 'x'));
        assertArrayEquals(new char[] { 'x', 'b', 'x', 'c', 'x' }, chars);
        assertEquals(0, N.replaceAll((char[]) null, 'a', 'x'));

        byte[] bytes = { 1, 2, 1, 3, 1 };
        assertEquals(3, N.replaceAll(bytes, (byte) 1, (byte) 9));
        assertEquals(0, N.replaceAll((byte[]) null, (byte) 1, (byte) 9));

        short[] shorts = { 10, 20, 10, 30, 10 };
        assertEquals(3, N.replaceAll(shorts, (short) 10, (short) 99));
        assertEquals(0, N.replaceAll((short[]) null, (short) 10, (short) 99));

        int[] ints = { 1, 2, 3, 2, 4, 2, 5 };
        assertEquals(3, N.replaceAll(ints, 2, 99));
        assertEquals(0, N.replaceAll((int[]) null, 2, 99));

        long[] longs = { 100L, 200L, 100L, 300L };
        assertEquals(2, N.replaceAll(longs, 100L, 999L));
        assertEquals(0, N.replaceAll((long[]) null, 100L, 999L));

        float[] floats = { 1.5f, 2.5f, 1.5f, 3.5f };
        assertEquals(2, N.replaceAll(floats, 1.5f, 9.9f));
        assertEquals(0, N.replaceAll((float[]) null, 1.5f, 9.9f));
        float[] nan = { 1.0f, Float.NaN, Float.NaN, 2.0f };
        assertEquals(2, N.replaceAll(nan, Float.NaN, 0.0f));
        assertEquals(0.0f, nan[1], 0.0f);

        double[] doubles = { 1.0, 2.0, 1.0, 3.0 };
        assertEquals(2, N.replaceAll(doubles, 1.0, 99.0));
        assertEquals(0, N.replaceAll((double[]) null, 1.0, 99.0));

        String[] words = { "apple", "banana", "apple", "cherry" };
        assertEquals(2, N.replaceAll(words, "apple", "FRUIT"));
        assertArrayEquals(new String[] { "FRUIT", "banana", "FRUIT", "cherry" }, words);
        String[] withNull = { "apple", "banana", "apple", "apple", null };
        assertEquals(1, N.replaceAll(withNull, null, "grape"));
        assertEquals(0, N.replaceAll((String[]) null, "a", "x"));
        assertEquals(0, N.replaceAll(new String[0], "a", "x"));

        List<String> list = new ArrayList<>(Arrays.asList("apple", "banana", "apple", "cherry"));
        assertEquals(2, N.replaceAll(list, "apple", "FRUIT"));
        assertEquals(Arrays.asList("FRUIT", "banana", "FRUIT", "cherry"), list);
        assertEquals(0, N.replaceAll((List<String>) null, "apple", "FRUIT"));
        List<String> withNulls = new ArrayList<>(Arrays.asList("x", null, "y", null));
        assertEquals(2, N.replaceAll(withNulls, null, "n"));
        assertEquals(0, N.replaceAll(new ArrayList<String>(), "a", "b"));

        final List<int[]> nested = new ArrayList<>();
        final int[] first = { 1, 2 };
        nested.add(first);
        nested.add(new int[] { 3 });
        assertEquals(0, N.replaceAll(nested, new int[] { 1, 2 }, new int[] { 9 }));
        assertEquals(1, N.replaceAll(nested, first, new int[] { 9 }));
        assertArrayEquals(new int[] { 9 }, nested.get(0));

        List<Integer> linked = new LinkedList<>(Arrays.asList(1, 0, 2, 0, 3, 0, 4, 0, 5, 0, 6, 0));
        assertEquals(6, N.replaceAll(linked, 0, -1));
        assertEquals(Arrays.asList(1, -1, 2, -1, 3, -1, 4, -1, 5, -1, 6, -1), linked);

        List<String> linkedNulls = new LinkedList<>(Arrays.asList("a", null, "b", null, "c", null, "d", null, "e", null, "f", null));
        assertEquals(6, N.replaceAll(linkedNulls, null, "x"));
        assertEquals("x", linkedNulls.get(1));
    }

    @Test
    public void testReplaceAll_operator() {
        boolean[] bools = { true, false, true };
        N.replaceAll(bools, (BooleanUnaryOperator) b -> !b);
        assertArrayEquals(new boolean[] { false, true, false }, bools);
        N.replaceAll((boolean[]) null, (BooleanUnaryOperator) b -> !b);
        N.replaceAll(new boolean[0], (BooleanUnaryOperator) b -> !b);

        char[] chars = { 'a', 'b', 'c' };
        N.replaceAll(chars, (CharUnaryOperator) c -> (char) (c + 1));
        assertArrayEquals(new char[] { 'b', 'c', 'd' }, chars);
        N.replaceAll((char[]) null, (CharUnaryOperator) c -> c);

        byte[] bytes = { 1, 2, 3 };
        N.replaceAll(bytes, (ByteUnaryOperator) b -> (byte) (b * 2));
        assertArrayEquals(new byte[] { 2, 4, 6 }, bytes);
        N.replaceAll((byte[]) null, (ByteUnaryOperator) b -> b);

        short[] shorts = { 10, 20, 30 };
        N.replaceAll(shorts, (ShortUnaryOperator) s -> (short) (s / 2));
        assertArrayEquals(new short[] { 5, 10, 15 }, shorts);
        N.replaceAll((short[]) null, (ShortUnaryOperator) s -> s);

        int[] ints = { 1, 2, 3 };
        N.replaceAll(ints, (IntUnaryOperator) i -> i * i);
        assertArrayEquals(new int[] { 1, 4, 9 }, ints);
        N.replaceAll((int[]) null, (IntUnaryOperator) i -> i);

        long[] longs = { 10L, 20L, 30L };
        N.replaceAll(longs, (LongUnaryOperator) l -> l - 5L);
        assertArrayEquals(new long[] { 5L, 15L, 25L }, longs);
        N.replaceAll((long[]) null, (LongUnaryOperator) l -> l);

        float[] floats = { 1.0f, 2.0f, 3.0f };
        N.replaceAll(floats, (FloatUnaryOperator) f -> f * 1.5f);
        assertFloatArrayEquals(new float[] { 1.5f, 3.0f, 4.5f }, floats, 0.001f);
        N.replaceAll((float[]) null, (FloatUnaryOperator) f -> f);

        double[] doubles = { 1.0, 2.0, 3.0 };
        N.replaceAll(doubles, (DoubleUnaryOperator) d -> d / 2.0);
        assertDoubleArrayEquals(new double[] { 0.5, 1.0, 1.5 }, doubles, 0.001);
        N.replaceAll((double[]) null, (DoubleUnaryOperator) d -> d);

        String[] words = { "hello", "world", "test" };
        N.replaceAll(words, String::toUpperCase);
        assertArrayEquals(new String[] { "HELLO", "WORLD", "TEST" }, words);
        N.replaceAll((String[]) null, (UnaryOperator<String>) s -> s);

        List<String> list = new ArrayList<>(Arrays.asList("a", "b", "c"));
        N.replaceAll(list, String::toUpperCase);
        assertEquals(Arrays.asList("A", "B", "C"), list);
        N.replaceAll((List<String>) null, (UnaryOperator<String>) s -> s);

        LinkedList<String> large = new LinkedList<>();
        for (int i = 0; i < 15; i++) {
            large.add("item" + i);
        }
        N.replaceAll(large, String::toUpperCase);
        assertEquals("ITEM0", large.get(0));
        assertEquals("ITEM14", large.get(14));
    }

    @Test
    public void testReplaceRange() {
        boolean[] bools = { true, false, true, false, true };
        assertArrayEquals(new boolean[] { true, false, false, false, true }, N.replaceRange(bools, 1, 3, new boolean[] { false, false }));
        assertArrayEquals(new boolean[] { true, false, true }, N.replaceRange(bools, 1, 3, new boolean[0]));
        assertArrayEquals(new boolean[] { true, false }, N.replaceRange(new boolean[] {}, 0, 0, new boolean[] { true, false }));
        assertArrayEquals(new boolean[] {}, N.replaceRange(new boolean[] { true }, 0, 1, new boolean[0]));
        assertArrayEquals(new boolean[] { true, false }, N.replaceRange(null, 0, 0, new boolean[] { true, false }));
        assertArrayEquals(new boolean[] {}, N.replaceRange(new boolean[] { true, true }, 0, 2, null));
        assertThrows(IndexOutOfBoundsException.class, () -> N.replaceRange(new boolean[] { true }, -1, 0, new boolean[0]));

        char[] chars = { 'a', 'b', 'c', 'd' };
        assertArrayEquals(new char[] { 'a', 'X', 'Y', 'd' }, N.replaceRange(chars, 1, 3, new char[] { 'X', 'Y' }));
        assertArrayEquals(chars, new char[] { 'a', 'b', 'c', 'd' });
        assertArrayEquals(new char[] { 'x', 'y' }, N.replaceRange(new char[] {}, 0, 0, new char[] { 'x', 'y' }));
        assertArrayEquals(new char[] { 'a', 'x', 'y', 'z', 'c', 'd' }, N.replaceRange(chars, 1, 2, new char[] { 'x', 'y', 'z' }));
        assertArrayEquals(new char[] { 'q', 'd' }, N.replaceRange(chars, 0, 3, new char[] { 'q' }));

        assertArrayEquals(new byte[] { 1, 10, 20, 4, 5 }, N.replaceRange(byteArray, 1, 3, new byte[] { 10, 20 }));
        assertArrayEquals(new short[] { 1, 10, 20, 4, 5 }, N.replaceRange(shortArray, 1, 3, new short[] { 10, 20 }));
        assertArrayEquals(new int[] { 1, 10, 20, 4, 5 }, N.replaceRange(intArray, 1, 3, new int[] { 10, 20 }));
        assertArrayEquals(new int[] { 1, 3, 4, 5, 6, 7, 8, 9 }, N.replaceRange(new int[] { 1, 2, 3, 4, 5, 6, 7, 8, 9 }, 1, 5, Array.of(3, 4, 5)));
        assertArrayEquals(new long[] { 1L, 10L, 20L, 4L, 5L }, N.replaceRange(longArray, 1, 3, new long[] { 10L, 20L }));
        assertArrayEquals(new float[] { 1.0f, 10.0f, 20.0f, 4.0f, 5.0f }, N.replaceRange(floatArray, 1, 3, new float[] { 10.0f, 20.0f }));
        assertArrayEquals(new double[] { 1.0, 10.0, 20.0, 4.0, 5.0 }, N.replaceRange(doubleArray, 1, 3, new double[] { 10.0, 20.0 }));
        assertArrayEquals(new String[] { "one", "ten", "twenty", "four", "five" }, N.replaceRange(stringArray, 1, 3, new String[] { "ten", "twenty" }));

        Integer[] original = { 1, 2, 3, 4 };
        assertArrayEquals(new Integer[] { 1, 8, 9, 4 }, N.replaceRange(original, 1, 3, new Integer[] { 8, 9 }));
        assertArrayEquals(new Integer[] { 1, 2, 3, 4 }, original);
        assertThrows(IllegalArgumentException.class, () -> N.replaceRange((Integer[]) null, 0, 0, new Integer[] { 10, 20 }));
        assertArrayEquals(new Integer[] { 1, 4 }, N.replaceRange(original, 1, 3, null));
        assertThrows(IndexOutOfBoundsException.class, () -> N.replaceRange(new Integer[] { 1 }, 1, 0, new Integer[0]));

        assertEquals("aXYd", N.replaceRange("abcd", 1, 3, "XY"));
        assertEquals("XYabcd", N.replaceRange("abcd", 0, 0, "XY"));
        assertEquals("hello my world", N.replaceRange("hello world", 5, 6, " my "));
        assertEquals("123456700000", N.replaceRange("123456789", 7, 9, "00000"));
        assertEquals("XY", N.replaceRange("", 0, 0, "XY"));
        assertEquals(null, N.replaceRange(null, 0, 0, "XY"));
        assertEquals("ac", N.replaceRange("abc", 1, 2, null));
        assertThrows(IndexOutOfBoundsException.class, () -> N.replaceRange("a", -1, 0, "b"));

        List<String> list = toMutableList("a", "b", "c", "d");
        assertTrue(N.replaceRange(list, 1, 3, toMutableList("X", "Y")));
        assertEquals(toMutableList("a", "X", "Y", "d"), list);
        List<String> insert = toMutableList("a", "b");
        assertTrue(N.replaceRange(insert, 1, 1, toMutableList("MID")));
        assertEquals(toMutableList("a", "MID", "b"), insert);
        List<String> unchanged = toMutableList("a", "b");
        assertFalse(N.replaceRange(unchanged, 1, 1, Collections.emptyList()));
        List<String> self = toMutableList("a", "b", "c", "d");
        assertTrue(N.replaceRange(self, 1, 3, self));
        assertEquals(Arrays.asList("a", "a", "b", "c", "d", "d"), self);
        List<String> view = toMutableList("a", "b", "c", "d");
        assertTrue(N.replaceRange(view, 1, 3, view.subList(0, 2)));
        assertEquals(Arrays.asList("a", "a", "b", "d"), view);
        List<String> fixed = Arrays.asList("a", "b", "c");
        assertTrue(N.replaceRange(fixed, 1, 2, Collections.singletonList("X")));
        assertEquals(Arrays.asList("a", "X", "c"), fixed);
        List<String> cow = new CopyOnWriteArrayList<>(Arrays.asList("a", "b", "c"));
        assertTrue(N.replaceRange(cow, 1, 2, Collections.singletonList("X")));
        assertEquals(Arrays.asList("a", "X", "c"), cow);
        assertThrows(IllegalArgumentException.class, () -> N.replaceRange((List<String>) null, 0, 0, Arrays.asList("X")));
    }

    @Test
    public void testReplaceRange_nullArrayAndNullReplacementContracts() {
        // A null/empty replacement degenerates into removeRange ...
        assertArrayEquals(new int[] { 1 }, N.replaceRange(new int[] { 1, 2, 3 }, 1, 3, (int[]) null));
        assertArrayEquals(new int[] { 1 }, N.replaceRange(new int[] { 1, 2, 3 }, 1, 3, new int[0]));

        // ... and a null a is treated as an empty array, so the range must be [0, 0).
        assertArrayEquals(new int[] { 7 }, N.replaceRange((int[]) null, 0, 0, new int[] { 7 }));
        assertArrayEquals(new int[] {}, N.replaceRange((int[]) null, 0, 0, (int[]) null));
        assertArrayEquals(new String[] { "z" }, N.replaceRange((String[]) null, 0, 0, new String[] { "z" }));
        assertThrows(IndexOutOfBoundsException.class, () -> N.replaceRange((int[]) null, 0, 1, new int[] { 7 }));
    }

    @Test
    public void testReplaceRange_oversizedResultReportsArithmeticException() {
        org.junit.jupiter.api.Assumptions.assumeTrue(Runtime.getRuntime().maxMemory() > 3L * 1024 * 1024 * 1024,
                "needs a heap large enough for a 2GB byte[]");

        final byte[] huge = new byte[Integer.MAX_VALUE - 2];
        assertThrows(ArithmeticException.class, () -> N.replaceRange(huge, 0, 0, new byte[10]));
    }
}

package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.util.u.OptionalInt;

public class IndexLastTest extends IndexTestSupport {

    @Test
    public void testLast_BooleanArray() {
        final boolean[] array = { true, false, true, false, true };
        assertEquals(OptionalInt.of(4), Index.last(array, true));
        assertEquals(OptionalInt.of(3), Index.last(array, false));
        assertEquals(OptionalInt.of(2), Index.last(array, true, 3));
        assertEquals(OptionalInt.of(4), Index.last(array, true, 10));
        assertFalse(Index.last(array, true, -1).isPresent());
        assertEquals(OptionalInt.empty(), Index.last((boolean[]) null, true));
        assertEquals(OptionalInt.empty(), Index.last(new boolean[0], true));
    }

    @Test
    public void testLast_CharArray() {
        final char[] array = { 'a', 'b', 'c', 'b' };
        assertEquals(OptionalInt.of(3), Index.last(array, 'b'));
        assertEquals(OptionalInt.of(1), Index.last(array, 'b', 2));
        assertEquals(OptionalInt.of(3), Index.last(array, 'b', 10));
    }

    @Test
    public void testLast_ByteArray() {
        final byte[] array = { 1, 2, 3, 2 };
        assertEquals(OptionalInt.of(3), Index.last(array, (byte) 2));
        assertEquals(OptionalInt.of(1), Index.last(array, (byte) 2, 2));
        assertEquals(OptionalInt.of(3), Index.last(array, (byte) 2, 10));
    }

    @Test
    public void testLast_ShortArray() {
        final short[] array = { 1, 2, 3, 2 };
        assertEquals(OptionalInt.of(3), Index.last(array, (short) 2));
        assertEquals(OptionalInt.of(1), Index.last(array, (short) 2, 2));
        assertEquals(OptionalInt.of(3), Index.last(array, (short) 2, 10));
    }

    @Test
    public void testLast_IntArray() {
        final int[] array = { 1, 2, 3, 2, 1 };
        assertEquals(OptionalInt.of(3), Index.last(array, 2));
        assertEquals(OptionalInt.of(1), Index.last(array, 2, 2));
        assertEquals(OptionalInt.of(3), Index.last(array, 2, 100));
        assertEquals(OptionalInt.of(3), Index.last(array, 2, Integer.MAX_VALUE));
    }

    @Test
    public void testLast_LongArray() {
        final long[] array = { 1L, 2L, 3L, 2L };
        assertEquals(OptionalInt.of(3), Index.last(array, 2L));
        assertEquals(OptionalInt.of(1), Index.last(array, 2L, 2));
        assertEquals(OptionalInt.of(3), Index.last(array, 2L, 10));
    }

    @Test
    public void testLast_FloatArray() {
        final float[] array = { 1.0f, 2.0f, 3.0f, 2.0f };
        assertEquals(OptionalInt.of(3), Index.last(array, 2.0f));
        assertEquals(OptionalInt.of(1), Index.last(array, 2.0f, 2));
        assertEquals(OptionalInt.of(3), Index.last(array, 2.0f, 10));
    }

    @Test
    public void testLast_FloatArray_Tolerance() {
        final float[] array = { 1.0f, 2.0f, 3.0f, 2.1f };
        assertEquals(OptionalInt.of(1), Index.last(array, 2.0f, 2, 0.2f));
        assertEquals(OptionalInt.of(3), Index.last(array, 2.0f, 10, 0.2f));
        assertEquals(OptionalInt.of(1), Index.last(array, 2.0f, 2, 0.05f));
    }

    @Test
    public void testLast_DoubleArray() {
        final double[] array = { 1.0, 2.0, 3.0, 2.0 };
        assertEquals(OptionalInt.of(3), Index.last(array, 2.0));
        assertEquals(OptionalInt.of(1), Index.last(array, 2.0, 2));
        assertEquals(OptionalInt.of(3), Index.last(array, 2.0, 10));
    }

    @Test
    public void testLast_DoubleArray_Tolerance() {
        final double[] array = { 1.0, 2.001, 3.0, 2.999, 4.0 };
        assertEquals(OptionalInt.of(3), Index.last(array, 3.0, array.length - 1, 0.01));
        assertEquals(OptionalInt.of(3), Index.last(array, 3.0, 3, 0.01));
        assertEquals(OptionalInt.of(2), Index.last(array, 3.0, 2, 0.01));
        assertEquals(OptionalInt.of(1), Index.last(new double[] { 1.0, 2.0, 3.0, 2.1 }, 2.0, 2, 0.2));
        assertEquals(OptionalInt.of(3), Index.last(new double[] { 1.0, 2.0, 3.0, 2.1 }, 2.0, 10, 0.2));
    }

    @Test
    public void testLast_ObjectArray() {
        final String[] array = { "apple", "banana", "cherry", "banana", "date" };
        assertEquals(OptionalInt.of(3), Index.last(array, "banana"));
        assertEquals(OptionalInt.of(1), Index.last(array, "banana", 2));
        assertEquals(OptionalInt.empty(), Index.last((Object[]) null, "a"));
        assertEquals(OptionalInt.empty(), Index.last(new Object[0], "a"));
    }

    @Test
    public void testLast_Collection() {
        final List<String> list = Arrays.asList("apple", "banana", "cherry", "banana", "date");
        assertEquals(OptionalInt.of(3), Index.last(list, "banana"));
        assertEquals(OptionalInt.of(1), Index.last(list, "banana", 2));
        assertEquals(OptionalInt.of(3), Index.last(new LinkedList<>(list), "banana"));
        assertEquals(OptionalInt.empty(), Index.last((List<String>) null, "a"));
        assertEquals(OptionalInt.empty(), Index.last(Collections.emptyList(), "a"));
    }

    @Test
    public void testLast_String() {
        final String str = "hello world hello";
        assertEquals(OptionalInt.of(15), Index.last(str, 'l'));
        assertEquals(OptionalInt.of(12), Index.last(str, 'h'));
        assertEquals(OptionalInt.empty(), Index.last(str, 'z'));
        assertEquals(OptionalInt.of(12), Index.last(str, "hello"));
        assertEquals(OptionalInt.of(6), Index.last(str, "world"));
        assertEquals(OptionalInt.empty(), Index.last(str, "test"));
        assertEquals(OptionalInt.of(0), Index.last(str, "hello", 10));
        assertEquals(OptionalInt.of(12), Index.last(str, "hello", 20));
        assertEquals(OptionalInt.empty(), Index.last((String) null, 'a'));
        assertEquals(OptionalInt.empty(), Index.last("", 'a'));
        assertEquals(OptionalInt.empty(), Index.last((String) null, "a"));
        assertEquals(OptionalInt.empty(), Index.last("", "a"));
    }

    @Test
    public void testLast_String_EmptySubstring() {
        assertEquals(OptionalInt.of(5), Index.last("hello", ""));
        assertEquals(OptionalInt.of(3), Index.last("hello", "", 3));
        assertEquals(OptionalInt.empty(), Index.last("hello", "", -1));
        final String repeating = "abababab";
        assertEquals(OptionalInt.of(6), Index.last(repeating, "ab"));
        assertEquals(OptionalInt.of(4), Index.last(repeating, "ab", 5));
        assertEquals(OptionalInt.of(2), Index.last(repeating, "ab", 3));
    }

    @Test
    public void testLastOfIgnoreCase() {
        final String str = "Hello World HELLO";
        assertEquals(OptionalInt.of(12), Index.lastOfIgnoreCase(str, "hello"));
        assertEquals(OptionalInt.of(6), Index.lastOfIgnoreCase("AbcDefAbc", "ABC"));
        assertEquals(OptionalInt.of(0), Index.lastOfIgnoreCase("AbcDefAbc", "ABC", 5));
        assertEquals(OptionalInt.empty(), Index.lastOfIgnoreCase("AbcDefAbc", "XYZ"));
        assertEquals(OptionalInt.of(4), Index.lastOfIgnoreCase("aBcAbC", "BC"));
        assertEquals(OptionalInt.of(1), Index.lastOfIgnoreCase("aBcAbC", "BC", 3));
        assertEquals(OptionalInt.of(4), Index.lastOfIgnoreCase("aBcAbC", "bc", 10));
        assertFalse(Index.lastOfIgnoreCase((String) null, "a").isPresent());
    }

    @Test
    public void testLastOfSubArray() {
        assertEquals(OptionalInt.of(4), Index.lastOfSubArray(new boolean[] { true, false, true, false, true, false }, new boolean[] { true, false }));
        assertEquals(OptionalInt.of(2), Index.lastOfSubArray(new boolean[] { true, false, true, false, true, false }, 3, new boolean[] { true, false }));
        assertEquals(OptionalInt.of(6), Index.lastOfSubArray("abcabcabc".toCharArray(), "abc".toCharArray()));
        assertEquals(OptionalInt.of(3), Index.lastOfSubArray("abcabcabc".toCharArray(), 5, "abc".toCharArray()));
        assertEquals(OptionalInt.of(6), Index.lastOfSubArray(new byte[] { 1, 2, 3, 1, 2, 3, 1, 2, 3 }, new byte[] { 1, 2, 3 }));
        assertEquals(OptionalInt.of(3), Index.lastOfSubArray(new short[] { 10, 20, 30, 10, 20, 30 }, new short[] { 10, 20, 30 }));
        assertEquals(OptionalInt.of(5), Index.lastOfSubArray(new int[] { 1, 2, 3, 4, 5, 1, 2, 3 }, new int[] { 1, 2, 3 }));
        assertEquals(OptionalInt.of(5), Index.lastOfSubArray(new int[] { 1, 2, 3, 4, 5, 1, 2, 3 }, 5, new int[] { 1, 2, 3 }));
        assertEquals(OptionalInt.of(0), Index.lastOfSubArray(new int[] { 1, 2, 3, 4, 5, 1, 2, 3 }, 4, new int[] { 1, 2, 3 }));
        assertEquals(OptionalInt.of(3), Index.lastOfSubArray(new long[] { 100L, 200L, 300L, 100L, 200L, 300L }, new long[] { 100L, 200L, 300L }));
        assertEquals(OptionalInt.of(3), Index.lastOfSubArray(new float[] { 1.1f, 2.2f, 3.3f, 1.1f, 2.2f, 3.3f }, new float[] { 1.1f, 2.2f }));
        assertEquals(OptionalInt.of(3), Index.lastOfSubArray(new double[] { 1.1, 2.2, 3.3, 1.1, 2.2, 3.3 }, new double[] { 1.1, 2.2 }));
        assertEquals(OptionalInt.of(2),
                Index.lastOfSubArray(new String[] { "apple", "banana", "apple", "banana", "cherry" }, new String[] { "apple", "banana" }));
        assertEquals(OptionalInt.empty(), Index.lastOfSubArray(new boolean[] { true, false }, new boolean[] { false, false }));
        assertEquals(OptionalInt.empty(), Index.lastOfSubArray((boolean[]) null, new boolean[] { true }));
    }

    @Test
    public void testLastOfSubArray_EmptyAndRange() {
        final int[] source = { 1, 2, 3, 1, 2, 3, 4 };
        assertEquals(OptionalInt.of(7), Index.lastOfSubArray(source, new int[0]));
        assertEquals(OptionalInt.of(5), Index.lastOfSubArray(source, 5, new int[0]));
        assertEquals(OptionalInt.empty(), Index.lastOfSubArray(source, -1, new int[0]));
        assertEquals(OptionalInt.of(7), Index.lastOfSubArray(source, 10, new int[0]));
        assertEquals(OptionalInt.of(5), Index.lastOfSubArray(new int[] { 1, 2, 3, 4, 5 }, 10, new int[0]));
        assertEquals(OptionalInt.empty(), Index.lastOfSubArray(new int[] { 1, 2 }, new int[] { 1, 2, 3 }));
        assertEquals(OptionalInt.empty(), Index.lastOfSubArray(null, new int[] { 1 }));
        assertEquals(OptionalInt.empty(), Index.lastOfSubArray(source, null));

        final double[] doubles = { 1.0, 2.0, 3.0 };
        assertEquals(OptionalInt.of(doubles.length), Index.lastOfSubArray(doubles, 10, new double[0], 0, 0));
        assertEquals(OptionalInt.of(1), Index.lastOfSubArray(doubles, 1, new double[0], 0, 0));
        assertEquals(OptionalInt.empty(), Index.lastOfSubArray(doubles, -1, new double[0], 0, 0));

        final long[] longs = { 1L, 2L, 3L, 2L, 3L };
        final long[] sub = { 2L, 3L };
        assertEquals(OptionalInt.of(3), Index.lastOfSubArray(longs, 5, sub, 0, 2));
        assertEquals(OptionalInt.of(1), Index.lastOfSubArray(longs, 2, sub, 0, 2));
        assertEquals(OptionalInt.empty(), Index.lastOfSubArray(longs, 5, new long[] { 2L, 9L }, 0, 2));
        assertEquals(OptionalInt.empty(), Index.lastOfSubArray((long[]) null, 5, sub, 0, 2));
        assertEquals(OptionalInt.empty(), Index.lastOfSubArray(longs, -1, sub, 0, 2));
        assertEquals(OptionalInt.of(2), Index.lastOfSubArray(longs, 2, sub, 0, 0));

        final String[] objects = { "a", "b", "c", "d", "e" };
        assertEquals(OptionalInt.of(2), Index.lastOfSubArray(objects, 5, new String[] { "x", "c", "d", "y" }, 1, 2));
        assertEquals(OptionalInt.of(5), Index.lastOfSubArray(objects, 5, objects, 0, 0));
        assertFalse(Index.lastOfSubArray((char[]) null, 5, new char[0], 0, 0).isPresent());
        assertFalse(Index.lastOfSubArray(new char[] { 'a' }, -1, new char[0], 0, 0).isPresent());
    }

    @Test
    public void testLastOfSubList() {
        final List<String> list = Arrays.asList("apple", "banana", "apple", "banana", "cherry");
        assertEquals(OptionalInt.of(2), Index.lastOfSubList(list, Arrays.asList("apple", "banana")));
        assertEquals(OptionalInt.of(5), Index.lastOfSubList(list, new ArrayList<>()));
        assertEquals(OptionalInt.of(2), Index.lastOfSubList(new LinkedList<>(list), new LinkedList<>(Arrays.asList("apple", "banana"))));

        final List<Integer> source = Arrays.asList(1, 2, 3, 1, 2, 3, 4);
        assertEquals(OptionalInt.of(3), Index.lastOfSubList(source, Arrays.asList(1, 2, 3)));
        assertEquals(OptionalInt.of(3), Index.lastOfSubList(source, 3, Arrays.asList(1, 2, 3)));
        assertEquals(OptionalInt.of(0), Index.lastOfSubList(source, 2, Arrays.asList(1, 2, 3)));
        assertFalse(Index.lastOfSubList((List<?>) null, 5, new ArrayList<>(), 0, 0).isPresent());
        assertFalse(Index.lastOfSubList(Arrays.asList(1), 5, (List<?>) null, 0, 0).isPresent());
        assertFalse(Index.lastOfSubList(Arrays.asList(1), -1, new ArrayList<>(), 0, 0).isPresent());
    }

    /**
     * G31-001 contract pin: for the indexed {@code lastOfSubArray} forms an empty pattern is a zero-width match at
     * {@code min(startIndexFromBack, length)} only when {@code startIndexFromBack >= 0}; a negative
     * {@code startIndexFromBack} finds nothing, exactly as {@link String#lastIndexOf(String, int)} does. The javadoc
     * used to give the position as {@code min(max(startIndexFromBack, 0), length)}, which promises index 0 instead.
     */
    @Test
    public void testLastOfSubArray_EmptyPatternNegativeStartIndexFromBack() {
        // the JDK behaviour this family follows
        assertEquals(-1, "abc".lastIndexOf("", -1));
        assertEquals(0, "abc".lastIndexOf("", 0));
        assertEquals(3, "abc".lastIndexOf(""));

        // all nine element types: a negative startIndexFromBack finds nothing, it is NOT clamped up to 0
        assertEquals(OptionalInt.empty(), Index.lastOfSubArray(new boolean[] { true, false, true }, -1, new boolean[0]));
        assertEquals(OptionalInt.empty(), Index.lastOfSubArray(new char[] { 'a', 'b', 'c' }, -1, new char[0]));
        assertEquals(OptionalInt.empty(), Index.lastOfSubArray(new byte[] { 1, 2, 3 }, -1, new byte[0]));
        assertEquals(OptionalInt.empty(), Index.lastOfSubArray(new short[] { 1, 2, 3 }, -1, new short[0]));
        assertEquals(OptionalInt.empty(), Index.lastOfSubArray(new int[] { 1, 2, 3 }, -1, new int[0]));
        assertEquals(OptionalInt.empty(), Index.lastOfSubArray(new long[] { 1L, 2L, 3L }, -1, new long[0]));
        assertEquals(OptionalInt.empty(), Index.lastOfSubArray(new float[] { 1f, 2f, 3f }, -1, new float[0]));
        assertEquals(OptionalInt.empty(), Index.lastOfSubArray(new double[] { 1.0, 2.0, 3.0 }, -1, new double[0]));
        assertEquals(OptionalInt.empty(), Index.lastOfSubArray(new String[] { "a", "b", "c" }, -1, new String[0]));
        assertEquals(OptionalInt.empty(), Index.lastOfSubArray(new int[] { 1, 2, 3 }, Integer.MIN_VALUE, new int[0]));
        assertEquals(OptionalInt.empty(), Index.lastOfSubList(Arrays.asList(1, 2, 3), -1, Collections.emptyList()));

        // a non-negative startIndexFromBack really is min(startIndexFromBack, length) - no max(..., 0) anywhere
        final int[] source = { 1, 2, 3 };
        assertEquals(OptionalInt.of(0), Index.lastOfSubArray(source, 0, new int[0]));
        assertEquals(OptionalInt.of(2), Index.lastOfSubArray(source, 2, new int[0]));
        assertEquals(OptionalInt.of(3), Index.lastOfSubArray(source, 3, new int[0]));
        assertEquals(OptionalInt.of(3), Index.lastOfSubArray(source, 99, new int[0]));
        assertEquals(OptionalInt.of(3), Index.lastOfSubArray(source, new int[0]));
    }
}

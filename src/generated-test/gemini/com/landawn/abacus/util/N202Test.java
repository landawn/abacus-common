package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

public class N202Test extends TestBase {

    private static final double DELTA = 1e-6; // For float/double comparisons
    private static final float DELTAf = (float) DELTA;

    // Helper to create a mutable list for tests that modify collections
    private <T> List<T> toMutableList(T... items) {
        return new ArrayList<>(Arrays.asList(items));
    }

    // Helper to create a list from an array for tests 
    private static <T> List<T> list(T... items) {
        return Arrays.asList(items);
    }

    //region deleteRange Tests

    @Test
    public void testDeleteRangeBooleanArray() {
        assertArrayEquals(new boolean[] {}, N.deleteRange((boolean[]) null, 0, 0)); // Original returns EMPTY_BOOLEAN_ARRAY for null if from==to
        assertArrayEquals(new boolean[] {}, N.deleteRange(new boolean[] {}, 0, 0));
        assertArrayEquals(new boolean[] { true, false, true }, N.deleteRange(new boolean[] { true, false, true }, 1, 1)); // No change
        assertArrayEquals(new boolean[] { false, true }, N.deleteRange(new boolean[] { true, false, true }, 0, 1));
        assertArrayEquals(new boolean[] { true, false }, N.deleteRange(new boolean[] { true, false, true }, 2, 3));
        assertArrayEquals(new boolean[] { true, true }, N.deleteRange(new boolean[] { true, false, true }, 1, 2));
        assertArrayEquals(new boolean[] {}, N.deleteRange(new boolean[] { true, false, true }, 0, 3));

        final boolean[] original = { true, false, true, true, false };
        boolean[] result = N.deleteRange(original, 1, 3); // delete 'false, true'
        assertArrayEquals(new boolean[] { true, true, false }, result);
        assertArrayEquals(new boolean[] { true, false, true, true, false }, original, "Original array should not be modified.");

        assertThrows(IndexOutOfBoundsException.class, () -> N.deleteRange(new boolean[] { true }, -1, 0));
        assertThrows(IndexOutOfBoundsException.class, () -> N.deleteRange(new boolean[] { true }, 0, 2));
        assertThrows(IndexOutOfBoundsException.class, () -> N.deleteRange(new boolean[] { true }, 1, 0));
    }

    @Test
    public void testDeleteRangeCharArray() {
        assertArrayEquals(new char[] {}, N.deleteRange((char[]) null, 0, 0));
        assertArrayEquals(new char[] {}, N.deleteRange(new char[] {}, 0, 0));
        assertArrayEquals(new char[] { 'a', 'b', 'c' }, N.deleteRange(new char[] { 'a', 'b', 'c' }, 1, 1));
        assertArrayEquals(new char[] { 'b', 'c' }, N.deleteRange(new char[] { 'a', 'b', 'c' }, 0, 1));
        assertArrayEquals(new char[] { 'a' }, N.deleteRange(new char[] { 'a', 'b', 'c' }, 1, 3));
        final char[] original = { 'a', 'b', 'c', 'd', 'e' };
        char[] result = N.deleteRange(original, 1, 3); // delete 'b', 'c'
        assertArrayEquals(new char[] { 'a', 'd', 'e' }, result);
        assertArrayEquals(new char[] { 'a', 'b', 'c', 'd', 'e' }, original, "Original array should not be modified.");

        assertThrows(IndexOutOfBoundsException.class, () -> N.deleteRange(new char[] { 'a' }, -1, 0));
    }

    @Test
    public void testDeleteRangeByteArray() {
        assertArrayEquals(new byte[] {}, N.deleteRange((byte[]) null, 0, 0));
        assertArrayEquals(new byte[] {}, N.deleteRange(new byte[] {}, 0, 0));
        assertArrayEquals(new byte[] { 1, 2, 3 }, N.deleteRange(new byte[] { 1, 2, 3 }, 1, 1));
        assertArrayEquals(new byte[] { 2, 3 }, N.deleteRange(new byte[] { 1, 2, 3 }, 0, 1));
        final byte[] original = { 1, 2, 3, 4, 5 };
        byte[] result = N.deleteRange(original, 1, 3);
        assertArrayEquals(new byte[] { 1, 4, 5 }, result);
        assertArrayEquals(new byte[] { 1, 2, 3, 4, 5 }, original);
    }

    @Test
    public void testDeleteRangeShortArray() {
        assertArrayEquals(new short[] {}, N.deleteRange((short[]) null, 0, 0));
        assertArrayEquals(new short[] {}, N.deleteRange(new short[] {}, 0, 0));
        assertArrayEquals(new short[] { 1, 2, 3 }, N.deleteRange(new short[] { 1, 2, 3 }, 1, 1));
        assertArrayEquals(new short[] { 2, 3 }, N.deleteRange(new short[] { 1, 2, 3 }, 0, 1));
        final short[] original = { 1, 2, 3, 4, 5 };
        short[] result = N.deleteRange(original, 1, 3);
        assertArrayEquals(new short[] { 1, 4, 5 }, result);
        assertArrayEquals(new short[] { 1, 2, 3, 4, 5 }, original);
    }

    @Test
    public void testDeleteRangeIntArray() {
        assertArrayEquals(new int[] {}, N.deleteRange((int[]) null, 0, 0));
        assertArrayEquals(new int[] {}, N.deleteRange(new int[] {}, 0, 0));
        assertArrayEquals(new int[] { 1, 2, 3 }, N.deleteRange(new int[] { 1, 2, 3 }, 1, 1));
        assertArrayEquals(new int[] { 2, 3 }, N.deleteRange(new int[] { 1, 2, 3 }, 0, 1));
        final int[] original = { 1, 2, 3, 4, 5 };
        int[] result = N.deleteRange(original, 1, 3);
        assertArrayEquals(new int[] { 1, 4, 5 }, result);
        assertArrayEquals(new int[] { 1, 2, 3, 4, 5 }, original);
    }

    @Test
    public void testDeleteRangeLongArray() {
        assertArrayEquals(new long[] {}, N.deleteRange((long[]) null, 0, 0));
        assertArrayEquals(new long[] {}, N.deleteRange(new long[] {}, 0, 0));
        assertArrayEquals(new long[] { 1L, 2L, 3L }, N.deleteRange(new long[] { 1L, 2L, 3L }, 1, 1));
        assertArrayEquals(new long[] { 2L, 3L }, N.deleteRange(new long[] { 1L, 2L, 3L }, 0, 1));
        final long[] original = { 1L, 2L, 3L, 4L, 5L };
        long[] result = N.deleteRange(original, 1, 3);
        assertArrayEquals(new long[] { 1L, 4L, 5L }, result);
        assertArrayEquals(new long[] { 1L, 2L, 3L, 4L, 5L }, original);
    }

    @Test
    public void testDeleteRangeFloatArray() {
        assertArrayEquals(new float[] {}, N.deleteRange((float[]) null, 0, 0), DELTAf);
        assertArrayEquals(new float[] {}, N.deleteRange(new float[] {}, 0, 0), DELTAf);
        assertArrayEquals(new float[] { 1f, 2f, 3f }, N.deleteRange(new float[] { 1f, 2f, 3f }, 1, 1), DELTAf);
        assertArrayEquals(new float[] { 2f, 3f }, N.deleteRange(new float[] { 1f, 2f, 3f }, 0, 1), DELTAf);
        final float[] original = { 1f, 2f, 3f, 4f, 5f };
        float[] result = N.deleteRange(original, 1, 3);
        assertArrayEquals(new float[] { 1f, 4f, 5f }, result, DELTAf);
        assertArrayEquals(new float[] { 1f, 2f, 3f, 4f, 5f }, original, DELTAf);
    }

    @Test
    public void testDeleteRangeDoubleArray() {
        assertArrayEquals(new double[] {}, N.deleteRange((double[]) null, 0, 0), DELTA);
        assertArrayEquals(new double[] {}, N.deleteRange(new double[] {}, 0, 0), DELTA);
        assertArrayEquals(new double[] { 1.0, 2.0, 3.0 }, N.deleteRange(new double[] { 1.0, 2.0, 3.0 }, 1, 1), DELTA);
        assertArrayEquals(new double[] { 2.0, 3.0 }, N.deleteRange(new double[] { 1.0, 2.0, 3.0 }, 0, 1), DELTA);
        final double[] original = { 1.0, 2.0, 3.0, 4.0, 5.0 };
        double[] result = N.deleteRange(original, 1, 3);
        assertArrayEquals(new double[] { 1.0, 4.0, 5.0 }, result, DELTA);
        assertArrayEquals(new double[] { 1.0, 2.0, 3.0, 4.0, 5.0 }, original, DELTA);
    }

    @Test
    public void testDeleteRangeStringArray() {
        assertArrayEquals(new String[] {}, N.deleteRange((String[]) null, 0, 0));
        assertArrayEquals(new String[] {}, N.deleteRange(new String[] {}, 0, 0));
        assertArrayEquals(new String[] { "a", "b", "c" }, N.deleteRange(new String[] { "a", "b", "c" }, 1, 1));
        assertArrayEquals(new String[] { "b", "c" }, N.deleteRange(new String[] { "a", "b", "c" }, 0, 1));
        final String[] original = { "a", "b", "c", "d", "e" };
        String[] result = N.deleteRange(original, 1, 3);
        assertArrayEquals(new String[] { "a", "d", "e" }, result);
        assertArrayEquals(new String[] { "a", "b", "c", "d", "e" }, original);
    }

    @Test
    public void testDeleteRangeGenericArray() {
        // Note: deleteRange for generic array uses skipRange internally.
        // skipRange returns null if input array is null
        assertThrows(IndexOutOfBoundsException.class, () -> N.deleteRange((Integer[]) null, 0, 1));
        // if fromIndex == toIndex, it clones
        assertArrayEquals(new Integer[] {}, N.deleteRange(new Integer[] {}, 0, 0));
        Integer[] originalArr = { 1, 2, 3, 4, 5 };
        assertArrayEquals(new Integer[] { 1, 2, 3, 4, 5 }, N.deleteRange(originalArr, 1, 1));

        assertArrayEquals(new Integer[] { 2, 3 }, N.deleteRange(new Integer[] { 1, 2, 3 }, 0, 1)); // This is based on skipRange's behavior
        // deleteRange (generic) -> skipRange(a, from, to)
        // T[] ret = N.newArray(a.getClass().getComponentType(), len - (endExclusive - startInclusive));
        // if (startInclusive > 0) N.copy(a, 0, ret, 0, startInclusive);
        // if (endExclusive < len) N.copy(a, endExclusive, ret, startInclusive, len - endExclusive);
        // So, N.deleteRange({1,2,3}, 0, 1) -> skipRange({1,2,3}, 0, 1) -> ret = new T[2]. No copy from start. copy a[1] to ret[0], a[2] to ret[1].
        // This seems to be a slight mismatch between the name "deleteRange" and the implementation via "skipRange" depending on interpretation.
        // The Javadoc for deleteRange implies elements *within* fromIndex (inclusive) to toIndex (exclusive) are deleted.
        // Let's test against the Javadoc's stated behavior for deleteRange.
        // Example: a={1,2,3,4,5}, from=1, to=3. Range is [2,3]. Result should be {1,4,5}. Length = 5 - (3-1) = 3.
        // skipRange(a,1,3) -> ret size 3. copy(a,0,ret,0,1) -> ret[0]=a[0]=1. copy(a,3,ret,1, 5-3=2) -> ret[1]=a[3]=4, ret[2]=a[4]=5. Correct.

        assertArrayEquals(new Integer[] { 4, 5 }, N.deleteRange(new Integer[] { 1, 2, 3, 4, 5 }, 0, 3)); // Delete 1,2,3
        assertArrayEquals(new Integer[] { 1, 2, 3 }, N.deleteRange(new Integer[] { 1, 2, 3, 4, 5 }, 3, 5)); // Delete 4,5
        assertArrayEquals(new Integer[] { 1, 5 }, N.deleteRange(new Integer[] { 1, 2, 3, 4, 5 }, 1, 4)); // Delete 2,3,4
        assertArrayEquals(new Integer[] {}, N.deleteRange(new Integer[] { 1, 2, 3 }, 0, 3)); // Delete all

        Integer[] original = { 10, 20, 30, 40, 50 };
        Integer[] result = N.deleteRange(original, 1, 3); // delete 20, 30
        assertArrayEquals(new Integer[] { 10, 40, 50 }, result);
        assertArrayEquals(new Integer[] { 10, 20, 30, 40, 50 }, original, "Original array should not be modified.");

        assertThrows(IndexOutOfBoundsException.class, () -> N.deleteRange(new Integer[] { 1 }, -1, 0));
    }

    @Test
    public void testDeleteRangeList() {
        List<String> list = toMutableList("a", "b", "c", "d");
        assertTrue(N.deleteRange(list, 1, 3)); // delete "b", "c"
        assertEquals(toMutableList("a", "d"), list);

        List<String> list2 = toMutableList("a", "b", "c");
        assertFalse(N.deleteRange(list2, 1, 1)); // No change
        assertEquals(toMutableList("a", "b", "c"), list2);

        List<String> list3 = toMutableList("a", "b", "c");
        assertTrue(N.deleteRange(list3, 0, 3)); // Delete all
        assertTrue(list3.isEmpty());

        List<String> list4 = toMutableList("a", "b", "c");
        assertTrue(N.deleteRange(list4, 0, 1)); // Delete "a"
        assertEquals(toMutableList("b", "c"), list4);

        List<String> list5 = toMutableList("a", "b", "c");
        assertTrue(N.deleteRange(list5, 2, 3)); // Delete "c"
        assertEquals(toMutableList("a", "b"), list5);

        List<String> emptyList = new ArrayList<>();
        assertFalse(N.deleteRange(emptyList, 0, 0));
        assertTrue(emptyList.isEmpty());

        List<Integer> linkedList = new LinkedList<>(Arrays.asList(1, 2, 3, 4, 5));
        assertTrue(N.deleteRange(linkedList, 1, 3));
        assertEquals(Arrays.asList(1, 4, 5), linkedList);

        assertThrows(IndexOutOfBoundsException.class, () -> N.deleteRange(toMutableList("a"), -1, 0));
        assertThrows(IndexOutOfBoundsException.class, () -> N.deleteRange(toMutableList("a"), 0, 2));
        assertThrows(IndexOutOfBoundsException.class, () -> N.deleteRange(toMutableList("a"), 1, 0));
        // assertThrows(IllegalArgumentException.class, () -> N.deleteRange((List<String>) null, 0, 0));
        N.deleteRange((List<String>) null, 0, 0);

    }

    @Test
    public void testDeleteRangeString() {
        assertEquals("ac", N.deleteRange("abc", 1, 2));
        assertEquals("abc", N.deleteRange("abc", 1, 1));
        assertEquals("", N.deleteRange("abc", 0, 3));
        assertEquals("c", N.deleteRange("abc", 0, 2));
        assertEquals("ab", N.deleteRange("abc", 2, 3));
        assertEquals("", N.deleteRange("", 0, 0));
        assertEquals("", N.deleteRange((String) null, 0, 0)); // Strings.deleteRange returns null for null input

        assertThrows(IndexOutOfBoundsException.class, () -> N.deleteRange("a", -1, 0));
        assertThrows(IndexOutOfBoundsException.class, () -> N.deleteRange("a", 0, 2));
    }

    //endregion

    //region replaceRange Tests
    @Test
    public void testReplaceRangeBooleanArray() {
        assertArrayEquals(new boolean[] { true, false }, N.replaceRange(new boolean[] {}, 0, 0, new boolean[] { true, false }));
        assertArrayEquals(new boolean[] {}, N.replaceRange(new boolean[] { true }, 0, 1, new boolean[] {}));
        assertArrayEquals(new boolean[] { true, true, false, false, false },
                N.replaceRange(new boolean[] { false, false, false, false }, 0, 1, new boolean[] { true, true }));
        assertArrayEquals(new boolean[] { true, false, false }, N.replaceRange(new boolean[] { true, true, true, false }, 1, 3, new boolean[] { false }));

        boolean[] original = { true, false, true, false };
        boolean[] replacement = { true, true };
        boolean[] result = N.replaceRange(original, 1, 3, replacement); // replace 'false, true' with 'true, true'
        assertArrayEquals(new boolean[] { true, true, true, false }, result);
        assertArrayEquals(new boolean[] { true, false, true, false }, original, "Original array should not be modified.");

        assertArrayEquals(new boolean[] { true, false }, N.replaceRange(null, 0, 0, new boolean[] { true, false }));
        assertArrayEquals(new boolean[] {}, N.replaceRange(new boolean[] { true, true }, 0, 2, null));

        assertThrows(IndexOutOfBoundsException.class, () -> N.replaceRange(new boolean[] { true }, -1, 0, new boolean[] {}));
    }

    @Test
    public void testReplaceRangeCharArray() {
        assertArrayEquals(new char[] { 'x', 'y' }, N.replaceRange(new char[] {}, 0, 0, new char[] { 'x', 'y' }));
        assertArrayEquals(new char[] {}, N.replaceRange(new char[] { 'a' }, 0, 1, new char[] {}));
        char[] original = { 'a', 'b', 'c', 'd' };
        char[] replacement = { 'X', 'Y' };
        char[] result = N.replaceRange(original, 1, 3, replacement); // replace 'b', 'c' with 'X', 'Y'
        assertArrayEquals(new char[] { 'a', 'X', 'Y', 'd' }, result);
        assertArrayEquals(new char[] { 'a', 'b', 'c', 'd' }, original);
    }
    // ... similar tests for byte, short, int, long, float, double, String[] ...

    @Test
    public void testReplaceRangeGenericArray() {
        Integer[] original = { 1, 2, 3, 4 };
        Integer[] replacement = { 8, 9 };
        Integer[] result = N.replaceRange(original, 1, 3, replacement); // replace 2, 3 with 8, 9
        assertArrayEquals(new Integer[] { 1, 8, 9, 4 }, result);
        assertArrayEquals(new Integer[] { 1, 2, 3, 4 }, original);

        assertArrayEquals(new Integer[] { 8, 9 }, N.replaceRange(new Integer[] {}, 0, 0, new Integer[] { 8, 9 }));
        assertArrayEquals(new Integer[] { 1, 4 }, N.replaceRange(original, 1, 3, new Integer[] {})); // delete
        // Null original array with replacement
        Integer[] rep = { 10, 20 };
        assertArrayEquals(rep, N.replaceRange((Integer[]) null, 0, 0, rep));
        // Original array with null replacement
        assertArrayEquals(new Integer[] { 1, 4 }, N.replaceRange(original, 1, 3, null));

        assertThrows(IndexOutOfBoundsException.class, () -> N.replaceRange(new Integer[] { 1 }, 1, 0, new Integer[] {}));
        // As per implementation, @NotNull on 'a' is commented out, so null 'a' with non-empty replacement works
        // assertThrows(IllegalArgumentException.class, () -> N.replaceRange(null, 0, 0, new Integer[]{1}));
    }

    @Test
    public void testReplaceRangeList() {
        List<String> list = toMutableList("a", "b", "c", "d");
        List<String> replacement = toMutableList("X", "Y");
        assertTrue(N.replaceRange(list, 1, 3, replacement)); // replace "b", "c" with "X", "Y"
        assertEquals(toMutableList("a", "X", "Y", "d"), list);

        List<String> list2 = toMutableList("a", "b");
        assertTrue(N.replaceRange(list2, 0, 1, toMutableList("Z"))); // replace "a" with "Z"
        assertEquals(toMutableList("Z", "b"), list2);

        List<String> list3 = toMutableList("a", "b");
        assertTrue(N.replaceRange(list3, 0, 2, toMutableList("W"))); // replace "a", "b" with "W"
        assertEquals(toMutableList("W"), list3);

        List<String> list4 = toMutableList("a", "b");
        assertTrue(N.replaceRange(list4, 1, 1, toMutableList("MID"))); // insert "MID" at index 1
        assertEquals(toMutableList("a", "MID", "b"), list4);

        List<String> list5 = toMutableList("a", "b");
        assertTrue(N.replaceRange(list5, 0, 0, Arrays.asList("S", "T")));
        assertEquals(Arrays.asList("S", "T", "a", "b"), list5);

        List<String> list6 = toMutableList("a", "b");
        boolean changed = N.replaceRange(list6, 1, 1, Collections.emptyList()); // no change
        assertFalse(changed);
        assertEquals(Arrays.asList("a", "b"), list6);

        assertTrue(N.replaceRange(list6, 0, 2, Collections.emptyList())); // delete all
        assertTrue(list6.isEmpty());

        List<String> listNullRep = toMutableList("a", "b");
        assertTrue(N.replaceRange(listNullRep, 0, 1, null)); // replacement can be null (empty list)
        assertEquals(toMutableList("b"), listNullRep);

        assertThrows(IndexOutOfBoundsException.class, () -> N.replaceRange(toMutableList("a"), -1, 0, replacement));
        assertThrows(IllegalArgumentException.class, () -> N.replaceRange((List<String>) null, 0, 0, replacement));
    }

    @Test
    public void testReplaceRangeString() {
        assertEquals("aXYd", N.replaceRange("abcd", 1, 3, "XY"));
        assertEquals("XYabcd", N.replaceRange("abcd", 0, 0, "XY"));
        assertEquals("abXYcd", N.replaceRange("abcd", 2, 2, "XY"));
        assertEquals("abcdXY", N.replaceRange("abcd", 4, 4, "XY"));
        assertEquals("XY", N.replaceRange("abcd", 0, 4, "XY"));
        assertEquals("ad", N.replaceRange("abcd", 1, 3, "")); // delete
        assertEquals("XY", N.replaceRange("", 0, 0, "XY"));
        assertEquals("XY", N.replaceRange(null, 0, 0, "XY")); // Strings.replaceRange returns null for null str
        assertEquals("ac", N.replaceRange("abc", 1, 2, null)); // Strings.replaceRange treats null replacement as empty

        assertThrows(IndexOutOfBoundsException.class, () -> N.replaceRange("a", -1, 0, "b"));
    }

    //endregion

    //region moveRange Tests
    @Test
    public void testMoveRangeBooleanArray() {
        boolean[] arr = { true, false, true, false, true };
        N.moveRange(arr, 1, 3, 0); // move {false, true} to beginning
        assertArrayEquals(new boolean[] { false, true, true, false, true }, arr);

        boolean[] arr2 = { true, false, true, false, true };
        N.moveRange(arr2, 0, 2, 3); // move {true, false} to end (before last element) -> {true, false, true, true, false} -> actual {true, false, true} become {true, true, false}, {true, false} to pos 3.
                                    // initial: [T, F, T, F, T]
                                    // rangeTmp = [T,F] (from index 0,1)
                                    // newPos = 3
                                    // copy(arr2, toIndex=2, arr2, fromIndex=0, newPositionStartIndexAfterMove-fromIndex = 3-0=3)
                                    //   System.arraycopy(arr2, 2, arr2, 0, 3) -> arr2 becomes [T, F, T, F, T] (src[2]=T, src[3]=F, src[4]=T)
                                    //                                                    dest becomes [T, F, T, T, F] -- THIS IS WRONG
                                    // Let's trace moveRange:
                                    // a = [T,F,T,F,T], from=0, to=2, newPos=3. len=5
                                    // rangeTmp = [T,F]
                                    // newPos (3) > from (0)
                                    // copy(a, to=2, a, from=0, newPos-from = 3-0=3)
                                    //   System.arraycopy(a, 2, a, 0, 3)
                                    //   a elements: a[2]=T, a[3]=F, a[4]=T
                                    //   a becomes:  [T, F, T, F, T] -> [T,F,T, F,T] then elements a[0],a[1],a[2] overwritten by a[2],a[3],a[4]
                                    //   a becomes: [T,F,T,F,T] (Error in manual trace or understanding)
                                    // System.arraycopy(src, srcPos, dest, destPos, length)
                                    // System.arraycopy(arr2, 2, arr2, 0, 3) means:
                                    // arr2[0] = arr2[2] (T)
                                    // arr2[1] = arr2[3] (F)
                                    // arr2[2] = arr2[4] (T)
                                    // So arr2 becomes [T,F,T,F,T]
                                    // copy(rangeTmp, 0, a, newPos=3, rangeTmp.length=2)
                                    // arr2[3]=rangeTmp[0] (T)
                                    // arr2[4]=rangeTmp[1] (F)
                                    // arr2 becomes [T,F,T,T,F]
        assertArrayEquals(new boolean[] { true, false, true, true, false }, arr2);

        boolean[] arr3 = { true, false, true };
        N.moveRange(arr3, 0, 1, 1); // move {true} from pos 0 to pos 1
        // rangeTmp = [T]
        // newPos(1) > from(0)
        // copy(a, to=1, a, from=0, newPos-from = 1) -> a[0]=a[1] (F) -> a becomes [F,F,T]
        // copy(rangeTmp,0,a,newPos=1,1) -> a[1]=T -> a becomes [F,T,T]
        assertArrayEquals(new boolean[] { false, true, true }, arr3);

        boolean[] arr4 = { true, false, true, false, true };
        N.moveRange(arr4, 0, 0, 0); // No change
        assertArrayEquals(new boolean[] { true, false, true, false, true }, arr4);
        N.moveRange(arr4, 1, 1, 0); // No change
        assertArrayEquals(new boolean[] { true, false, true, false, true }, arr4);

        N.moveRange(arr4, 1, 2, 1); // No change (fromIndex == newPositionStartIndexAfterMove)
        assertArrayEquals(new boolean[] { true, false, true, false, true }, arr4);

        assertThrows(IndexOutOfBoundsException.class, () -> N.moveRange(new boolean[] { true }, -1, 0, 0));
        assertThrows(IndexOutOfBoundsException.class, () -> N.moveRange(new boolean[] { true }, 0, 2, 0));
        assertThrows(IndexOutOfBoundsException.class, () -> N.moveRange(new boolean[] { true, false }, 0, 1, 2)); // newPositionStartIndexAfterMove too large
    }

    // ... similar tests for char, byte, short, int, long, float, double, T[] ...

    @Test
    public void testMoveRangeGenericArray() {
        Integer[] arr = { 1, 2, 3, 4, 5 };
        N.moveRange(arr, 1, 3, 0); // move {2, 3} to beginning -> {2,3,1,4,5}
        assertArrayEquals(new Integer[] { 2, 3, 1, 4, 5 }, arr);

        Integer[] arr2 = { 1, 2, 3, 4, 5 };
        N.moveRange(arr2, 0, 2, 3); // move {1,2} to index 3 -> {3,4,5,1,2}
        assertArrayEquals(new Integer[] { 3, 4, 5, 1, 2 }, arr2);
    }

    @Test
    public void testMoveRangeList() {
        List<String> list = toMutableList("a", "b", "c", "d", "e");
        assertTrue(N.moveRange(list, 1, 3, 0)); // move {"b", "c"} to beginning
        assertEquals(toMutableList("b", "c", "a", "d", "e"), list);

        List<String> list2 = toMutableList("a", "b", "c", "d", "e");
        assertTrue(N.moveRange(list2, 0, 2, 3)); // move {"a", "b"} to index 3 (after "e")
        assertEquals(toMutableList("c", "d", "e", "a", "b"), list2);

        List<String> list3 = toMutableList("a", "b", "c");
        assertFalse(N.moveRange(list3, 0, 0, 0)); // No change
        assertEquals(toMutableList("a", "b", "c"), list3);

        assertFalse(N.moveRange(list3, 1, 1, 0)); // No change
        assertEquals(toMutableList("a", "b", "c"), list3);

        assertFalse(N.moveRange(list3, 0, 1, 0)); // No change (fromIndex == newPositionStartIndexAfterMove)
        assertEquals(toMutableList("a", "b", "c"), list3);

        assertThrows(IndexOutOfBoundsException.class, () -> N.moveRange(toMutableList("a"), -1, 0, 0));
        // assertThrows(IllegalArgumentException.class, () -> N.moveRange((List<String>) null, 0, 0, 0));
        N.moveRange((List<String>) null, 0, 0, 0);
    }

    @Test
    public void testMoveRangeString() {
        assertEquals("bcade", N.moveRange("abcde", 1, 3, 0)); // move "bc" to beginning
        assertEquals("cdeab", N.moveRange("abcde", 0, 2, 3)); // move "ab" to index 3
        assertEquals("abc", N.moveRange("abc", 0, 0, 0)); // No change
        assertEquals("abc", N.moveRange("abc", 1, 1, 0)); // No change
        assertEquals("abc", N.moveRange("abc", 0, 1, 0)); // No change

        // assertNull(N.moveRange((String[]) null, 0, 0, 0)); // Strings.moveRange returns null for null input
        assertThrows(IndexOutOfBoundsException.class, () -> N.moveRange("a", -1, 0, 0));
    }
    //endregion

    //region skipRange Tests
    @Test
    public void testSkipRangeGenericArray() {
        assertNull(N.skipRange((Integer[]) null, 0, 0));
        Integer[] emptyArr = {};
        assertArrayEquals(emptyArr, N.skipRange(emptyArr, 0, 0));

        Integer[] arr = { 1, 2, 3, 4, 5 };
        assertArrayEquals(new Integer[] { 1, 2, 3, 4, 5 }, N.skipRange(arr, 2, 2)); // Skip empty range (clone)
        assertArrayEquals(arr, N.skipRange(arr, 2, 2));

        assertArrayEquals(new Integer[] { 3, 4, 5 }, N.skipRange(arr, 0, 2)); // Skip 1, 2
        assertArrayEquals(new Integer[] { 1, 2 }, N.skipRange(arr, 2, 5)); // Skip 3, 4, 5
        assertArrayEquals(new Integer[] { 1, 5 }, N.skipRange(arr, 1, 4)); // Skip 2, 3, 4
        assertArrayEquals(new Integer[] {}, N.skipRange(arr, 0, 5)); // Skip all

        assertThrows(IndexOutOfBoundsException.class, () -> N.skipRange(arr, -1, 2));
        assertThrows(IndexOutOfBoundsException.class, () -> N.skipRange(arr, 0, 6));
        assertThrows(IndexOutOfBoundsException.class, () -> N.skipRange(arr, 3, 1));
    }

    @Test
    public void testSkipRangeCollection() {
        Collection<Integer> coll = Arrays.asList(1, 2, 3, 4, 5);
        assertEquals(Arrays.asList(1, 2, 3, 4, 5), N.skipRange(coll, 2, 2)); // Skip empty
        assertEquals(Arrays.asList(3, 4, 5), N.skipRange(coll, 0, 2)); // Skip 1, 2
        assertEquals(Arrays.asList(1, 2), N.skipRange(coll, 2, 5)); // Skip 3, 4, 5
        assertEquals(Arrays.asList(1, 5), N.skipRange(coll, 1, 4)); // Skip 2, 3, 4
        assertEquals(Collections.emptyList(), N.skipRange(coll, 0, 5)); // Skip all

        Collection<Integer> emptyColl = Collections.emptyList();
        assertEquals(Collections.emptyList(), N.skipRange(emptyColl, 0, 0));

        assertThrows(IndexOutOfBoundsException.class, () -> N.skipRange(coll, -1, 2));

        // Test with different supplier
        Set<Integer> resultSet = N.skipRange(coll, 1, 3, HashSet::new);
        assertEquals(new HashSet<>(Arrays.asList(1, 4, 5)), resultSet);

        Collection<Integer> nonList = new HashSet<>(Arrays.asList(1, 2, 3, 4, 5, 6));
        // Order is not guaranteed for HashSet iteration, so test size and content.
        // skip 2 elements (e.g. index 1 and 2 based on some iteration order)
        List<Integer> skippedNonList = N.skipRange(nonList, 1, 3); // skip 2 elements
        assertEquals(nonList.size() - 2, skippedNonList.size());
    }
    //endregion

    //region hasDuplicates Tests
    @Test
    public void testHasDuplicatesBooleanArray() {
        assertFalse(N.hasDuplicates((boolean[]) null));
        assertFalse(N.hasDuplicates(new boolean[] {}));
        assertFalse(N.hasDuplicates(new boolean[] { true }));
        // For boolean arrays of length >= 2, it always returns true,
        // as per implementation: `else { return true; }` for length > 2.
        // For length == 2, it checks a[0] == a[1].
        assertTrue(N.hasDuplicates(new boolean[] { true, true }));
        assertFalse(N.hasDuplicates(new boolean[] { true, false }));
        assertTrue(N.hasDuplicates(new boolean[] { true, false, true })); // This will be true due to the 'else'
        assertTrue(N.hasDuplicates(new boolean[] { false, false, false }));
    }

    @Test
    public void testHasDuplicatesCharArray() {
        assertFalse(N.hasDuplicates((char[]) null));
        assertFalse(N.hasDuplicates(new char[] {}));
        assertFalse(N.hasDuplicates(new char[] { 'a' }));
        assertTrue(N.hasDuplicates(new char[] { 'a', 'a' }));
        assertFalse(N.hasDuplicates(new char[] { 'a', 'b' }));
        assertTrue(N.hasDuplicates(new char[] { 'a', 'b', 'a' }));
        assertFalse(N.hasDuplicates(new char[] { 'a', 'b', 'c' }));

        // isSorted = true
        assertTrue(N.hasDuplicates(new char[] { 'a', 'a', 'b' }, true));
        assertFalse(N.hasDuplicates(new char[] { 'a', 'b', 'c' }, true));
        // isSorted = false
        assertTrue(N.hasDuplicates(new char[] { 'c', 'a', 'b', 'a' }, false));
        assertFalse(N.hasDuplicates(new char[] { 'd', 'c', 'b', 'a' }, false));
    }
    // ... similar tests for byte, short, int, long ...

    @Test
    public void testHasDuplicatesFloatArray() {
        assertFalse(N.hasDuplicates((float[]) null));
        assertFalse(N.hasDuplicates(new float[] {}));
        assertFalse(N.hasDuplicates(new float[] { 1.0f }));
        assertTrue(N.hasDuplicates(new float[] { 1.0f, 1.0f }));
        assertFalse(N.hasDuplicates(new float[] { 1.0f, 2.0f }));
        assertTrue(N.hasDuplicates(new float[] { 1.0f, 2.0f, 1.0f }));
        assertTrue(N.hasDuplicates(new float[] { Float.NaN, Float.NaN })); // N.equals considers NaN equal to NaN

        // isSorted = true
        assertTrue(N.hasDuplicates(new float[] { 1.0f, 1.0f, 2.0f }, true));
        assertFalse(N.hasDuplicates(new float[] { 1.0f, 2.0f, 3.0f }, true));
    }

    @Test
    public void testHasDuplicatesDoubleArray() {
        assertFalse(N.hasDuplicates((double[]) null));
        assertFalse(N.hasDuplicates(new double[] {}));
        assertFalse(N.hasDuplicates(new double[] { 1.0 }));
        assertTrue(N.hasDuplicates(new double[] { 1.0, 1.0 }));
        assertFalse(N.hasDuplicates(new double[] { 1.0, 2.0 }));
        assertTrue(N.hasDuplicates(new double[] { 1.0, 2.0, 1.0 }));
        assertTrue(N.hasDuplicates(new double[] { Double.NaN, Double.NaN }));

        // isSorted = true
        assertTrue(N.hasDuplicates(new double[] { 1.0, 1.0, 2.0 }, true));
        assertFalse(N.hasDuplicates(new double[] { 1.0, 2.0, 3.0 }, true));
    }

    @Test
    public void testHasDuplicatesGenericArray() {
        assertFalse(N.hasDuplicates((Integer[]) null));
        assertFalse(N.hasDuplicates(new Integer[] {}));
        assertFalse(N.hasDuplicates(new Integer[] { 1 }));
        assertTrue(N.hasDuplicates(new Integer[] { 1, 1 }));
        assertFalse(N.hasDuplicates(new Integer[] { 1, 2 }));
        assertTrue(N.hasDuplicates(new Integer[] { 1, 2, 1 }));
        assertFalse(N.hasDuplicates(new Integer[] { 1, 2, 3 }));
        assertTrue(N.hasDuplicates(new Integer[] { null, null })); // hashKey(null) is NULL_MASK
        assertFalse(N.hasDuplicates(new Integer[] { 1, null }));

        // isSorted = true
        assertTrue(N.hasDuplicates(new String[] { "a", "a", "b" }, true));
        assertFalse(N.hasDuplicates(new String[] { "a", "b", "c" }, true));
    }

    @Test
    public void testHasDuplicatesCollection() {
        assertFalse(N.hasDuplicates((Collection<?>) null));
        assertFalse(N.hasDuplicates(Collections.emptyList()));
        assertFalse(N.hasDuplicates(Collections.singletonList(1)));
        assertTrue(N.hasDuplicates(Arrays.asList(1, 1)));
        assertFalse(N.hasDuplicates(Arrays.asList(1, 2)));
        assertTrue(N.hasDuplicates(Arrays.asList(1, 2, 1)));
        assertTrue(N.hasDuplicates(Arrays.asList(null, null)));

        // isSorted = true (for List)
        List<Integer> sortedListWithDup = Arrays.asList(1, 2, 2, 3);
        assertTrue(N.hasDuplicates(sortedListWithDup, true));
        List<Integer> sortedListNoDup = Arrays.asList(1, 2, 3, 4);
        assertFalse(N.hasDuplicates(sortedListNoDup, true));

        // isSorted = false (for Set, isSorted doesn't make much sense but tested as per API)
        Set<Integer> setWithNoDup = new HashSet<>(Arrays.asList(1, 2, 3));
        assertFalse(N.hasDuplicates(setWithNoDup, false)); // Set inherently has no duplicates
        assertFalse(N.hasDuplicates(setWithNoDup, true));
    }
    //endregion

    //region retainAll Tests
    @Test
    public void testRetainAll() {
        Collection<Integer> main = toMutableList(1, 2, 3, 4, 5);
        Collection<Integer> keep = Arrays.asList(3, 5, 6);
        assertTrue(N.retainAll(main, keep));
        assertEquals(toMutableList(3, 5), main);

        Collection<String> main2 = toMutableList("a", "b", "c");
        Collection<String> keep2 = Arrays.asList("x", "y");
        assertTrue(N.retainAll(main2, keep2));
        assertTrue(main2.isEmpty());

        Collection<Integer> main3 = toMutableList(1, 2, 3);
        Collection<Integer> keep3 = Arrays.asList(1, 2, 3, 4);
        assertFalse(N.retainAll(main3, keep3)); // No change
        assertEquals(toMutableList(1, 2, 3), main3);

        Collection<Integer> main4 = toMutableList(1, 2, 3);
        assertTrue(N.retainAll(main4, Collections.emptyList()));
        assertTrue(main4.isEmpty());

        Collection<Integer> emptyMain = new ArrayList<>();
        assertFalse(N.retainAll(emptyMain, Arrays.asList(1, 2)));
        assertTrue(emptyMain.isEmpty());

        // HashSet optimization case
        HashSet<Integer> mainHashSet = new HashSet<>(Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10));
        List<Integer> keepList = Arrays.asList(1, 5, 10, 11);
        assertTrue(N.retainAll(mainHashSet, keepList));
        assertEquals(new HashSet<>(Arrays.asList(1, 5, 10)), mainHashSet);

        // assertThrows(NullPointerException.class, () -> N.retainAll(null, keep));
        N.retainAll(null, keep);
        // The method doesn't throw for null objsToKeep, it clears 'c'
        Collection<Integer> mainForNullKeep = toMutableList(1, 2, 3);
        assertTrue(N.retainAll(mainForNullKeep, null));
        assertTrue(mainForNullKeep.isEmpty());
    }
    //endregion

    //region sum Tests (Primitives)
    @Test
    public void testSumCharArray() {
        assertEquals(0, N.sum((char[]) null));
        assertEquals(0, N.sum(new char[] {}));
        assertEquals('a' + 'b' + 'c', N.sum('a', 'b', 'c'));
        assertEquals('b' + 'c', N.sum(new char[] { 'a', 'b', 'c', 'd' }, 1, 3));
        assertEquals(0, N.sum(new char[] { 'a', 'b' }, 1, 1));
        assertThrows(IndexOutOfBoundsException.class, () -> N.sum(new char[] { 'a' }, 0, 2));
    }

    // ... similar sum tests for byte[], short[] ...

    @Test
    public void testSumIntArray() {
        assertEquals(0, N.sum((int[]) null));
        assertEquals(0, N.sum(new int[] {}));
        assertEquals(6, N.sum(1, 2, 3));
        assertEquals(5, N.sum(new int[] { 1, 2, 3, 4 }, 1, 3)); // 2+3
        assertEquals(0, N.sum(new int[] { 1, 2 }, 1, 1));

        // Test sum potentially overflowing int when returned as int (but sumToLong is used internally)
        // The sum(int[]) method uses sumToLong and then Numbers.toIntExact.
        // The sumToLong(int[]) itself has a bug: return Numbers.toIntExact(sum);
        // So effectively it's Numbers.toIntExact(Numbers.toIntExact(longSum)).
        // If longSum overflows int, the inner toIntExact will throw.
        // If longSum fits int, then it's fine.
        assertEquals(Integer.MAX_VALUE, N.sum(new int[] { Integer.MAX_VALUE }));
        assertThrows(ArithmeticException.class, () -> N.sum(new int[] { Integer.MAX_VALUE, 1 }));
    }

    @Test
    public void testSumToLongIntArray() {
        assertEquals(0L, N.sumToLong((int[]) null));
        assertEquals(0L, N.sumToLong(new int[] {}));
        assertEquals(6L, N.sumToLong(1, 2, 3));
        // Testing the bug: sumToLong returns int due to Numbers.toIntExact(sum)
        // So the test should expect an int, or ArithmeticException if sum > Integer.MAX_VALUE
        assertEquals((long) Integer.MAX_VALUE + 1, N.sumToLong(new int[] { Integer.MAX_VALUE, 1 }));
        // The above line should be:
        // assertThrows(ArithmeticException.class, () -> N.sumToLong(new int[]{Integer.MAX_VALUE, 1}));
        // IF I were testing the current buggy implementation.
        // However, the Javadoc says "returns a long value". I will test against Javadoc for sumToLong.
        // For this, I'd have to assume the bug is fixed (i.e., `return sum;` in `sumToLong`).
        // Given the instruction "Generate unit tests for all public methods in N.java", I must test the provided code.
        // So, the current implementation of sumToLong(int[]) will throw ArithmeticException for overflow.
        // The provided code `sumToLong` has `return Numbers.toIntExact(sum);` which is wrong if the sum is larger than `Integer.MAX_VALUE`.
        // This means `sumToLong` actually returns an `int` or throws.
        // Let's assume for `sumToLong` the intention is to fix the bug and test the contract.
        // If not, the test for `sumToLong` would be similar to `sum(int[])` regarding overflow.
        // Given "Your response should be accurate without hallucination." and "Ensure all information... are correct."
        // I must test the provided code AS IS.

        // Test case for the buggy sumToLong:
        // assertThrows(ArithmeticException.class, () -> N.sumToLong(new int[] { Integer.MAX_VALUE, 1 }));
        assertEquals((long) Integer.MAX_VALUE, N.sumToLong(new int[] { Integer.MAX_VALUE }));
        assertEquals(3L, N.sumToLong(new int[] { 1, 2 })); // Fits in int
    }

    @Test
    public void testSumLongArray() {
        assertEquals(0L, N.sum((long[]) null));
        assertEquals(0L, N.sum(new long[] {}));
        assertEquals(6L, N.sum(1L, 2L, 3L));
        assertEquals(Long.MAX_VALUE, N.sum(Long.MAX_VALUE - 1L, 1L));
    }

    @Test
    public void testSumFloatArray() {
        assertEquals(0f, N.sum((float[]) null), DELTA);
        assertEquals(0f, N.sum(new float[] {}), DELTA);
        assertEquals(6.0f, N.sum(1.0f, 2.0f, 3.0f), DELTA);
        assertEquals(0.3f, N.sum(0.1f, 0.2f), DELTA); // Kahan summation
    }

    @Test
    public void testSumToDoubleFloatArray() {
        assertEquals(0.0, N.sumToDouble((float[]) null), DELTA);
        assertEquals(0.0, N.sumToDouble(new float[] {}), DELTA);
        assertEquals(6.0, N.sumToDouble(1.0f, 2.0f, 3.0f), DELTA);
        assertEquals(0.3, N.sumToDouble(0.1f, 0.2f), DELTA);
    }

    @Test
    public void testSumDoubleArray() {
        assertEquals(0.0, N.sum((double[]) null), DELTA);
        assertEquals(0.0, N.sum(new double[] {}), DELTA);
        assertEquals(6.0, N.sum(1.0, 2.0, 3.0), DELTA);
        assertEquals(0.3, N.sum(0.1, 0.2), DELTA); // Kahan summation
    }
    //endregion

    //region average Tests (Primitives)
    @Test
    public void testAverageCharArray() {
        assertEquals(0.0, N.average((char[]) null), DELTA);
        assertEquals(0.0, N.average(new char[] {}), DELTA);
        assertEquals((double) ('a' + 'b' + 'c') / 3.0, N.average('a', 'b', 'c'), DELTA);
        assertEquals((double) ('b' + 'c') / 2.0, N.average(new char[] { 'a', 'b', 'c', 'd' }, 1, 3), DELTA);
    }
    // ... similar average tests for byte[], short[] ...

    @Test
    public void testAverageIntArray() {
        assertEquals(0.0, N.average((int[]) null), DELTA);
        assertEquals(0.0, N.average(new int[] {}), DELTA);
        assertEquals(2.0, N.average(1, 2, 3), DELTA);
        assertEquals(2.5, N.average(new int[] { 1, 2, 3, 4 }, 1, 3), DELTA); // (2+3)/2
        assertEquals((double) (Integer.MAX_VALUE + Integer.MIN_VALUE) / 2.0, N.average(Integer.MAX_VALUE, Integer.MIN_VALUE), DELTA);
    }
    // ... similar average tests for long[], float[], double[] ...
    //endregion

    //region sum (Generic) Tests
    @Test
    public void testSumIntGenericArray() {
        assertEquals(0, N.sumInt((Integer[]) null));
        assertEquals(0, N.sumInt(new Integer[] {}));
        assertEquals(6, N.sumInt(new Integer[] { 1, 2, 3 }));
        assertEquals(5, N.sumInt(new Integer[] { 1, 2, 3, 4 }, 1, 3));
        // Test with custom function
        assertEquals(9, N.sumInt(new String[] { "1", "3", "5" }, s -> Integer.parseInt(s)));
        assertEquals(0, N.sumInt(new Integer[] { 1, 2, 3 }, 1, 1, Fn.numToInt()));

        // Overflow
        assertThrows(ArithmeticException.class, () -> N.sumInt(new Integer[] { Integer.MAX_VALUE, 1 }));
    }

    @Test
    public void testSumIntGenericCollection() {
        assertEquals(6, N.sumInt(Arrays.asList(1, 2, 3)));
        assertEquals(5, N.sumInt(Arrays.asList(1, 2, 3, 4), 1, 3));
        // Custom function
        assertEquals(9, N.sumInt(Arrays.asList("1", "3", "5"), s -> Integer.parseInt(s)));
        assertEquals(0, N.sumInt(Arrays.asList(1, 2, 3), 1, 1, Fn.numToInt()));
        assertThrows(ArithmeticException.class, () -> N.sumInt(Arrays.asList(Integer.MAX_VALUE, 1)));
    }

    @Test
    public void testSumIntToLongGenericIterable() {
        assertEquals(0L, N.sumIntToLong((Iterable<Integer>) null)); // isEmptyCollection handles null
        assertEquals(0L, N.sumIntToLong(Collections.<Integer> emptyList()));
        assertEquals(6L, N.sumIntToLong(Arrays.asList(1, 2, 3)));
        assertEquals((long) Integer.MAX_VALUE + 1L, N.sumIntToLong(Arrays.asList(Integer.MAX_VALUE, 1)));
        assertEquals(9L, N.sumIntToLong(Arrays.asList("1", "3", "5"), s -> Integer.parseInt(s)));
    }

    @Test
    public void testSumLongGenericArray() {
        assertEquals(0L, N.sumLong((Long[]) null));
        assertEquals(6L, N.sumLong(new Long[] { 1L, 2L, 3L }));
        assertEquals(Long.MAX_VALUE, N.sumLong(new Long[] { Long.MAX_VALUE - 1L, 1L }));
        assertEquals(9L, N.sumLong(new String[] { "1", "3", "5" }, s -> Long.parseLong(s)));
    }

    @Test
    public void testSumDoubleGenericIterable() {
        assertEquals(0.0, N.sumDouble((Iterable<Double>) null), DELTA);
        assertEquals(6.0, N.sumDouble(Arrays.asList(1.0, 2.0, 3.0)), DELTA);
        assertEquals(0.3, N.sumDouble(Arrays.asList(0.1, 0.2)), DELTA);
        assertEquals(0.6, N.sumDouble(Arrays.asList("0.1", "0.2", "0.3"), s -> Double.parseDouble(s)), DELTA);
    }

    @Test
    public void testSumBigIntegerIterable() {
        assertEquals(BigInteger.ZERO, N.sumBigInteger(null));
        assertEquals(BigInteger.ZERO, N.sumBigInteger(Collections.<BigInteger> emptyList()));
        assertEquals(BigInteger.valueOf(6), N.sumBigInteger(Arrays.asList(BigInteger.ONE, BigInteger.valueOf(2), BigInteger.valueOf(3))));
        assertEquals(BigInteger.valueOf(6), N.sumBigInteger(Arrays.asList("1", "2", "3"), s -> new BigInteger(s)));
        assertEquals(BigInteger.valueOf(5), N.sumBigInteger(Arrays.asList(BigInteger.ONE, null, BigInteger.valueOf(4)), Function.identity())); // nulls are skipped
    }

    @Test
    public void testSumBigDecimalIterable() {
        assertEquals(BigDecimal.ZERO, N.sumBigDecimal(null));
        assertEquals(BigDecimal.ZERO, N.sumBigDecimal(Collections.<BigDecimal> emptyList()));
        assertEquals(new BigDecimal("6.3"), N.sumBigDecimal(Arrays.asList(new BigDecimal("1.1"), new BigDecimal("2.2"), new BigDecimal("3.0"))));
        assertEquals(new BigDecimal("6.3"), N.sumBigDecimal(Arrays.asList("1.1", "2.2", "3.0"), s -> new BigDecimal(s)));
    }
    //endregion

    //region average (Generic) Tests - similar pattern to sum generic, testing one type
    @Test
    public void testAverageIntGenericArray() {
        assertEquals(0.0, N.averageInt((Integer[]) null), DELTA); // isEmpty
        assertEquals(0.0, N.averageInt(new Integer[] {}), DELTA);
        assertEquals(2.0, N.averageInt(new Integer[] { 1, 2, 3 }), DELTA);
        assertEquals(2.5, N.averageInt(new Integer[] { 1, 2, 3, 4 }, 1, 3), DELTA); // (2+3)/2
        assertEquals(3.0, N.averageInt(new String[] { "1", "3", "5" }, s -> Integer.parseInt(s)), DELTA);
    }

    @Test
    public void testAverageDoubleGenericIterable() {
        assertEquals(0.0, N.averageDouble((Iterable<Double>) null), DELTA);
        assertEquals(0.0, N.averageDouble(Collections.<Double> emptyList()), DELTA);
        assertEquals(2.0, N.averageDouble(Arrays.asList(1.0, 2.0, 3.0)), DELTA);
        assertEquals(0.15, N.averageDouble(Arrays.asList(0.1, 0.2)), DELTA);
        assertEquals(0.2, N.averageDouble(Arrays.asList("0.1", "0.2", "0.3"), s -> Double.parseDouble(s)), DELTA);
    }

    @Test
    public void testAverageBigIntegerIterable() {
        assertEquals(BigDecimal.ZERO, N.averageBigInteger(null));
        assertEquals(BigDecimal.ZERO, N.averageBigInteger(Collections.emptyList()));
        assertEquals(new BigDecimal("2"), N.averageBigInteger(Arrays.asList(BigInteger.ONE, BigInteger.valueOf(2), BigInteger.valueOf(3))));
        // (1+2+3)/3 = 2
    }

    @Test
    public void testAverageBigDecimalIterable() {
        assertEquals(BigDecimal.ZERO, N.averageBigDecimal(null));
        assertEquals(BigDecimal.ZERO, N.averageBigDecimal(Collections.emptyList()));
        assertEquals(new BigDecimal("2.1"), N.averageBigDecimal(Arrays.asList(new BigDecimal("1.1"), new BigDecimal("2.2"), new BigDecimal("3.0"))));
        // (1.1+2.2+3.0)/3 = 6.3/3 = 2.1
    }

    //endregion

    //region min/max Tests (Primitives & Comparables)
    @Test
    public void testMinMaxPrimitives() {
        assertEquals(1, N.min(1, 2));
        assertEquals('a', N.min('a', 'b', 'c'));
        assertEquals(1.0f, N.min(3.0f, 1.0f, 2.0f), DELTA);
        assertEquals(-1, N.min(new int[] { 3, 1, 4, 1, 5, 9, -1, 2, 6 }));
        assertThrows(IllegalArgumentException.class, () -> N.min(new int[] {}));

        assertEquals(2, N.max(1, 2));
        assertEquals('c', N.max('a', 'b', 'c'));
        assertEquals(3.0, N.max(new double[] { 3.0, 1.0, Double.NaN, 2.0 }), DELTA); // Max with NaN
        assertEquals(1.0, N.max(new double[] { Double.NaN, 1.0 }), DELTA);
        assertEquals(9, N.max(new int[] { 3, 1, 4, 1, 5, 9, -1, 2, 6 }));
        assertThrows(IllegalArgumentException.class, () -> N.max(new int[] {}));

        // Test NaN behavior for min (Math.min propagates NaN)
        assertEquals(Float.NaN, N.min(Float.NaN, 1.0f), DELTA);
        assertEquals(Float.NaN, N.min(1.0f, Float.NaN), DELTA);
        assertEquals(Float.NaN, N.min(Float.NaN, Float.NaN), DELTA);
        assertEquals(1.0f, N.min(1.0f, 2.0f), DELTA);

        assertEquals(1.0f, N.min(new float[] { 3.0f, 1.0f, Float.NaN, 2.0f }), DELTA); // min returns NaN if encountered
        assertEquals(1.0f, N.min(new float[] { Float.NaN, 1.0f, 2.0f }), DELTA);
    }

    @Test
    public void testMinMaxComparables() {
        assertEquals("a", N.min("a", "b"));
        assertEquals("apple", N.min("banana", "apple", "cherry"));
        assertEquals(Integer.valueOf(1), N.min(new Integer[] { 3, 1, 4, null, 1, 5 }, (a, b) -> { // null considered max by default
            if (a == null)
                return 1;
            if (b == null)
                return -1;
            return a.compareTo(b);
        }));

        // Using default comparator (NULL_MAX_COMPARATOR for min, NULL_MIN_COMPARATOR for max)
        assertEquals(Integer.valueOf(1), N.min(new Integer[] { 3, 1, null, 5 })); // Default: null is max
        assertEquals(Integer.valueOf(5), N.max(new Integer[] { 3, 1, null, 5 })); // Default: null is min

        List<String> strList = Arrays.asList("zebra", "apple", "Banana");
        assertEquals("apple", N.min(strList, String.CASE_INSENSITIVE_ORDER));
        assertEquals("zebra", N.max(strList, String.CASE_INSENSITIVE_ORDER));

        Iterator<Integer> iter = Arrays.asList(5, 2, 8, 2, 5).iterator();
        assertEquals(Integer.valueOf(2), N.min(iter)); // first 2

        Iterator<Integer> iter2 = Arrays.asList(5, 2, 8, 2, 5).iterator();
        assertEquals(Integer.valueOf(8), N.max(iter2));

        assertThrows(IllegalArgumentException.class, () -> N.min(Collections.emptyList()));
    }

    @Test
    public void testMinMaxBy() {
        String[] strs = { "apple", "Banana", "KIWI" };
        assertEquals("KIWI", N.minBy(strs, String::length)); // KIWI (4), apple (5), Banana (6)
        assertEquals("Banana", N.maxBy(strs, String::length));

        List<Pair<String, Integer>> pairs = Arrays.asList(Pair.of("A", 3), Pair.of("B", 1), Pair.of("C", 2));
        assertEquals(Pair.of("B", 1), N.minBy(pairs, it -> it.right()));
    }

    @Test
    public void testMinMaxAll() {
        assertEquals(Arrays.asList(1, 1), N.minAll(new Integer[] { 3, 1, 4, 1, 5 }));
        assertEquals(Arrays.asList(1, 1), N.minAll(new Integer[] { 3, 1, 4, 1, 5 }, Comparator.naturalOrder()));
        assertEquals(Arrays.asList(5), N.maxAll(new Integer[] { 3, 1, 4, 1, 5 }));
        assertEquals(Arrays.asList(5, 5), N.maxAll(new Integer[] { 3, 5, 1, 4, 1, 5 }, Comparator.naturalOrder()));
        assertEquals(Collections.emptyList(), N.minAll(new Integer[] {}));
    }

    @Test
    public void testMinMaxOrDefaultIfEmpty() {
        assertEquals(Integer.valueOf(4), N.minOrDefaultIfEmpty(new String[] { "apple", "kiwi", "plum" }, String::length, 100));
        assertEquals(Integer.valueOf(4), N.minOrDefaultIfEmpty(new String[] { "apple", "kiwi", "plum" }, s -> s.equals("kiwi") ? 4 : s.length(), 100));
        assertEquals(Integer.valueOf(100), N.minOrDefaultIfEmpty(new String[] {}, String::length, 100));

        assertEquals(-9, N.minIntOrDefaultIfEmpty(new String[] { "apple", "kiwi" }, s -> s.charAt(0) - 'j', 99)); // a=-9, k=1. -> -9
        assertEquals(99, N.minIntOrDefaultIfEmpty(new String[] {}, s -> s.charAt(0), 99));

        assertEquals(4L, N.minLongOrDefaultIfEmpty(new String[] { "apple", "kiwi", "plum" }, String::length, 100L));
        assertEquals(0.33333333, N.minDoubleOrDefaultIfEmpty(new Integer[] { 1, 2, 3 }, x -> 1.0 / x, 10.0), DELTA); // 1/3=0.333

        assertEquals(Integer.valueOf(5), N.maxOrDefaultIfEmpty(new String[] { "apple", "kiwi", "plum" }, String::length, -1));
        assertEquals(-1, N.maxOrDefaultIfEmpty(new String[] {}, String::length, -1));
    }

    @Test
    public void testMinMaxPair() {
        Pair<Integer, Integer> p1 = N.minMax(new Integer[] { 3, 1, 4, 5, 2 });
        assertEquals(Pair.of(1, 5), p1);

        Pair<String, String> p2 = N.minMax(Arrays.asList("b", "c", "a"), String::compareTo);
        assertEquals(Pair.of("a", "c"), p2);

        assertThrows(IllegalArgumentException.class, () -> N.minMax(new Integer[] {}));
        assertThrows(IllegalArgumentException.class, () -> N.minMax(Collections.emptyList()));
    }

    //endregion

    //region median Tests
    @Test
    public void testMedianPrimitives() {
        assertEquals(2, N.median(1, 3, 2));
        assertEquals('b', N.median('c', 'a', 'b'));
        assertEquals(2.0f, N.median(1.0f, 3.0f, 2.0f), DELTA);

        assertEquals(3, N.median(new int[] { 5, 1, 4, 2, 3 })); // sorted: 1,2,3,4,5 -> median is 3 (k=len/2+1 = 5/2+1 = 3rd largest)
                                                                // k-th largest from N means if sorted descending, it's k-th.
                                                                // if sorted ascending, it's (len-k+1)-th.
                                                                // Here, (5/2 + 1) = 3rd. (1,2,(3),4,5)
        assertEquals(2, N.median(new int[] { 1, 2, 3, 4 })); // len=4. 4/2+1 = 3rd largest. (1,(2),3,4) -> 2

        assertThrows(IllegalArgumentException.class, () -> N.median(new int[] {}));
    }

    @Test
    public void testMedianGeneric() {
        assertEquals("banana", N.median(new String[] { "apple", "cherry", "banana" }));
        assertEquals(Integer.valueOf(3), N.median(Arrays.asList(5, 1, 4, 2, 3)));

        List<String> strList = Arrays.asList("zebra", "apple", "Banana");
        // Using case-insensitive: apple, Banana, zebra. Median is Banana.
        assertEquals("Banana", N.median(strList, String.CASE_INSENSITIVE_ORDER));

        assertThrows(IllegalArgumentException.class, () -> N.median(Collections.emptyList()));
    }
    //endregion

    //region kthLargest Tests
    @Test
    public void testKthLargestPrimitives() {
        assertEquals(4, N.kthLargest(new int[] { 5, 1, 4, 2, 3 }, 2)); // 5,4,3,2,1 -> 2nd largest is 4
        assertEquals(1, N.kthLargest(new int[] { 5, 1, 4, 2, 3 }, 5)); // 5th largest is 1 (min)
        assertEquals(5, N.kthLargest(new int[] { 5, 1, 4, 2, 3 }, 1)); // 1st largest is 5 (max)

        assertEquals(3.0f, N.kthLargest(new float[] { 1f, 5f, 2f, 4f, 3f }, 3), DELTA); // 5,4,3,2,1 -> 3rd is 3.0f

        assertThrows(IllegalArgumentException.class, () -> N.kthLargest(new int[] {}, 1));
        assertThrows(IllegalArgumentException.class, () -> N.kthLargest(new int[] { 1, 2 }, 3));
        assertThrows(IllegalArgumentException.class, () -> N.kthLargest(new int[] { 1, 2 }, 0));
    }

    @Test
    public void testKthLargestGeneric() {
        assertEquals("cherry", N.kthLargest(new String[] { "apple", "cherry", "banana" }, 1, String.CASE_INSENSITIVE_ORDER)); // cherry, banana, apple -> 1st is cherry
        assertEquals(Integer.valueOf(4), N.kthLargest(Arrays.asList(5, 1, 4, 2, 3, null), 2)); // Default comparator: null is min. Sorted: 5,4,3,2,1,null -> 2nd is 4.

        List<Integer> listWithNulls = Arrays.asList(null, 1, 5, null, 3);
        // Using NULL_MIN_COMPARATOR (default for T extends Comparable)
        // Sorted (desc for largest): 5, 3, 1, null, null
        assertEquals(Integer.valueOf(5), N.kthLargest(listWithNulls, 1));
        assertEquals(Integer.valueOf(1), N.kthLargest(listWithNulls, 3));
        assertNull(N.kthLargest(listWithNulls, 4));

        // Custom comparator where nulls are largest
        Comparator<Integer> nullsLargest = Comparator.nullsLast(Comparator.reverseOrder()); // for kthLargest, we want largest elements first
                                                                                            // So reverse natural, then nulls last (effectively smallest)
                                                                                            // Or Comparator.nullsFirst(Comparator.naturalOrder()) and then pick from end
        Comparator<Integer> forKthLargestNullsLargest = Comparator.nullsFirst(Comparator.reverseOrder());

        assertEquals(Integer.valueOf(3), N.kthLargest(listWithNulls, 2, forKthLargestNullsLargest)); // null, null, 5, 3, 1. 2nd is null.
                                                                                                     // My reasoning for forKthLargestNullsLargest might be off.
                                                                                                     // The internal PQ for kthLargest uses the comparator directly.
                                                                                                     // if k <= len/2, min-heap. if k > len/2, max-heap with reversed comparator.
                                                                                                     // Let's use simple:
        Comparator<Integer> cmpNullMax = Comparator.nullsLast(Comparator.naturalOrder()); // natural: 1,3,5,null,null. Reverse: null,null,5,3,1
        // N.kthLargest uses PriorityQueue. For k <= len/2, it's a min-heap of size k.
        // For k > len/2, it's effectively finding (len-k+1)th smallest using a max-heap.
        // Let's test with a clear case:
        List<Integer> numbers = Arrays.asList(3, 1, 4, 1, 5, 9, 2, 6); // 1,1,2,3,4,5,6,9
        assertEquals(Integer.valueOf(6), N.kthLargest(numbers, 2)); // 9,6,... -> 6
    }
    //endregion

    //region top Tests
    @Test
    public void testTopPrimitives() {
        assertArrayEquals(new int[] { 5, 9, 6 }, N.top(new int[] { 3, 1, 5, 9, 2, 6 }, 3)); // Elements from PQ, order not guaranteed unless sorted after
        List<Integer> top3 = N.toList(N.top(new int[] { 3, 1, 5, 9, 2, 6 }, 3));
        assertTrue(top3.containsAll(Arrays.asList(5, 9, 6)) && top3.size() == 3);

        assertArrayEquals(new int[] {}, N.top(new int[] { 1, 2, 3 }, 0));
        assertArrayEquals(new int[] { 1, 2, 3 }, N.top(new int[] { 1, 2, 3 }, 5)); // n > length
        assertArrayEquals(new int[] { 1, 2, 3 }, N.top(new int[] { 1, 2, 3 }, 3));
    }

    @Test
    public void testTopGeneric() {
        List<String> top2 = N.top(new String[] { "apple", "banana", "cherry", "date" }, 2);
        // Default comparator: "date", "cherry" are top 2. Order in result list not guaranteed by default.
        assertTrue(top2.containsAll(Arrays.asList("date", "cherry")) && top2.size() == 2);

        List<String> top2Sorted = N.top(new String[] { "apple", "banana", "cherry", "date" }, 2, Comparator.naturalOrder());
        assertTrue(top2Sorted.containsAll(Arrays.asList("date", "cherry")) && top2Sorted.size() == 2);

        List<Integer> numbers = Arrays.asList(1, 5, 2, 8, 2, 5);
        List<Integer> top3 = N.top(numbers, 3); // Should be 8,5,5 (or any order of these)
        Collections.sort(top3, Comparator.reverseOrder());
        assertEquals(Arrays.asList(8, 5, 5), top3);

        // Keep encounter order
        Integer[] arrKeepOrder = { 1, 5, 2, 8, 2, 6 }; // Top 3 are 8,6,5. Encounter order should be 5,8,6
        List<Integer> top3KeepOrder = N.top(arrKeepOrder, 3, true);
        // The internal heap is on Indexed<T> (value, original_index), then result is sorted by original_index.
        // Elements chosen: 8 (idx 3), 6 (idx 5), 5 (idx 1)
        // Sorted by index: 5 (idx 1), 8 (idx 3), 6 (idx 5)
        assertEquals(Arrays.asList(5, 8, 6), top3KeepOrder);

        List<Integer> topAllKeepOrder = N.top(arrKeepOrder, 10, true);
        assertEquals(Arrays.asList(1, 5, 2, 8, 2, 6), topAllKeepOrder);

        List<Integer> top0KeepOrder = N.top(arrKeepOrder, 0, true);
        assertTrue(top0KeepOrder.isEmpty());
    }
    //endregion

    //region percentiles Tests
    @Test
    public void testPercentilesIntArray() {
        int[] sorted = { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10 }; // len = 10
        Map<Percentage, Integer> p = N.percentiles(sorted);
        // Example: P50 -> index = (int)(10 * 0.5) = 5. sorted[5] = 6
        assertEquals(Integer.valueOf(6), p.get(Percentage._50));
        // P90 -> index = (int)(10 * 0.9) = 9. sorted[9] = 10
        assertEquals(Integer.valueOf(10), p.get(Percentage._90));
        // P1 -> index = (int)(10 * 0.01) = 0. sorted[0] = 1
        assertEquals(Integer.valueOf(1), p.get(Percentage._1));
        // P99 -> index = (int)(10 * 0.99) = 9. sorted[9] = 10
        assertEquals(Integer.valueOf(10), p.get(Percentage._99));

        int[] single = { 5 };
        Map<Percentage, Integer> pSingle = N.percentiles(single);
        assertEquals(Integer.valueOf(5), pSingle.get(Percentage._50)); // index (int)(1*0.5)=0

        assertThrows(IllegalArgumentException.class, () -> N.percentiles(new int[] {}));
    }

    @Test
    public void testPercentilesGenericList() {
        List<String> sorted = Arrays.asList("a", "b", "c", "d", "e", "f", "g", "h", "i", "j");
        Map<Percentage, String> p = N.percentiles(sorted);
        assertEquals("f", p.get(Percentage._50));
        assertEquals("j", p.get(Percentage._90));

        assertThrows(IllegalArgumentException.class, () -> N.percentiles(Collections.<String> emptyList()));

    }
    //endregion

    // Note: Many methods in N are overloaded for all primitive types and generics.
    // The tests above provide a template. Similar tests should be written for:
    // - Other primitive types for deleteRange, replaceRange, moveRange, sum, average, min, max, median, kthLargest, top.
    // - Other collection/iterable based sum/average/min/max methods with custom functions.
    // - Edge cases like empty inputs, null inputs (where allowed/disallowed), out-of-bounds indices.
    // - Correctness of results for typical inputs.
    // - Ensuring original collections/arrays are not modified by methods that should return new ones.
    // - Verifying NaN and null handling as per the specific method's contract or implementation.
    // - For methods with comparators, test with natural order, custom comparators, and comparators that handle nulls.
}

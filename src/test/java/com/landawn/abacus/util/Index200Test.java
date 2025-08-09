package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.BitSet;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.function.Predicate;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.u.OptionalInt;

public class Index200Test extends TestBase {

    @Test
    public void testOfBooleanArray() {
        boolean[] array = { true, false, true, false };
        assertEquals(OptionalInt.of(0), Index.of(array, true));
        assertEquals(OptionalInt.of(1), Index.of(array, false));
        assertEquals(OptionalInt.of(2), Index.of(array, true, 2));
        assertEquals(OptionalInt.empty(), Index.of(array, true, 3));
    }

    @Test
    public void testOfCharArray() {
        char[] array = { 'a', 'b', 'c', 'a' };
        assertEquals(OptionalInt.of(0), Index.of(array, 'a'));
        assertEquals(OptionalInt.of(1), Index.of(array, 'b'));
        assertEquals(OptionalInt.of(3), Index.of(array, 'a', 1));
        assertEquals(OptionalInt.empty(), Index.of(array, 'd'));
    }

    @Test
    public void testOfByteArray() {
        byte[] array = { 1, 2, 3, 1 };
        assertEquals(OptionalInt.of(0), Index.of(array, (byte) 1));
        assertEquals(OptionalInt.of(1), Index.of(array, (byte) 2));
        assertEquals(OptionalInt.of(3), Index.of(array, (byte) 1, 1));
        assertEquals(OptionalInt.empty(), Index.of(array, (byte) 4));
    }

    @Test
    public void testOfShortArray() {
        short[] array = { 1, 2, 3, 1 };
        assertEquals(OptionalInt.of(0), Index.of(array, (short) 1));
        assertEquals(OptionalInt.of(1), Index.of(array, (short) 2));
        assertEquals(OptionalInt.of(3), Index.of(array, (short) 1, 1));
        assertEquals(OptionalInt.empty(), Index.of(array, (short) 4));
    }

    @Test
    public void testOfIntArray() {
        int[] array = { 1, 2, 3, 1 };
        assertEquals(OptionalInt.of(0), Index.of(array, 1));
        assertEquals(OptionalInt.of(1), Index.of(array, 2));
        assertEquals(OptionalInt.of(3), Index.of(array, 1, 1));
        assertEquals(OptionalInt.empty(), Index.of(array, 4));
    }

    @Test
    public void testOfLongArray() {
        long[] array = { 1L, 2L, 3L, 1L };
        assertEquals(OptionalInt.of(0), Index.of(array, 1L));
        assertEquals(OptionalInt.of(1), Index.of(array, 2L));
        assertEquals(OptionalInt.of(3), Index.of(array, 1L, 1));
        assertEquals(OptionalInt.empty(), Index.of(array, 4L));
    }

    @Test
    public void testOfFloatArray() {
        float[] array = { 1.0f, 2.0f, 3.0f, 1.0f };
        assertEquals(OptionalInt.of(0), Index.of(array, 1.0f));
        assertEquals(OptionalInt.of(1), Index.of(array, 2.0f));
        assertEquals(OptionalInt.of(3), Index.of(array, 1.0f, 1));
        assertEquals(OptionalInt.empty(), Index.of(array, 4.0f));
    }

    @Test
    public void testOfDoubleArray() {
        double[] array = { 1.0, 2.0, 3.0, 1.0 };
        assertEquals(OptionalInt.of(0), Index.of(array, 1.0));
        assertEquals(OptionalInt.of(1), Index.of(array, 2.0));
        assertEquals(OptionalInt.of(3), Index.of(array, 1, 1.0));
        assertEquals(OptionalInt.empty(), Index.of(array, 4.0));
    }

    @Test
    public void testOfDoubleArrayWithTolerance() {
        double[] array = { 1.0, 2.0, 3.0, 1.05 };
        assertEquals(OptionalInt.of(0), Index.of(array, 1.0, 0.1));
        assertEquals(OptionalInt.of(0), Index.of(array, 1.0, 0.01));
        assertEquals(OptionalInt.of(3), Index.of(array, 1.0, 0.1, 1));

    }

    @Test
    public void testOfObjectArray() {
        String[] array = { "a", "b", "c", "a" };
        assertEquals(OptionalInt.of(0), Index.of(array, "a"));
        assertEquals(OptionalInt.of(1), Index.of(array, "b"));
        assertEquals(OptionalInt.of(3), Index.of(array, "a", 1));
        assertEquals(OptionalInt.empty(), Index.of(array, "d"));
        assertEquals(OptionalInt.empty(), Index.of((Object[]) null, "a"));
    }

    @Test
    public void testOfCollection() {
        List<String> list = Arrays.asList("a", "b", "c", "a");
        assertEquals(OptionalInt.of(0), Index.of(list, "a"));
        assertEquals(OptionalInt.of(1), Index.of(list, "b"));
        assertEquals(OptionalInt.of(3), Index.of(list, "a", 1));
        assertEquals(OptionalInt.empty(), Index.of(list, "d"));
    }

    @Test
    public void testOfString() {
        String str = "abcadef";
        assertEquals(OptionalInt.of(0), Index.of(str, 'a'));
        assertEquals(OptionalInt.of(1), Index.of(str, 'b'));
        assertEquals(OptionalInt.of(3), Index.of(str, 'a', 1));
        assertEquals(OptionalInt.empty(), Index.of(str, 'g'));
        assertEquals(OptionalInt.of(0), Index.of(str, "a"));
        assertEquals(OptionalInt.of(3), Index.of(str, "ad"));
        assertEquals(OptionalInt.empty(), Index.of(str, "g"));
    }

    @Test
    public void testOfIgnoreCase() {
        String str = "aBcDeF";
        assertEquals(OptionalInt.of(0), Index.ofIgnoreCase(str, "abc"));
        assertEquals(OptionalInt.of(3), Index.ofIgnoreCase(str, "def"));
        assertEquals(OptionalInt.empty(), Index.ofIgnoreCase(str, "ghi"));
    }

    @Test
    public void testOfSubArray() {
        int[] source = { 1, 2, 3, 4, 5, 1, 2, 3 };
        int[] target = { 1, 2, 3 };
        assertEquals(OptionalInt.of(0), Index.ofSubArray(source, target));
        assertEquals(OptionalInt.of(5), Index.ofSubArray(source, 1, target));
    }

    @Test
    public void testOfSubList() {
        List<Integer> source = Arrays.asList(1, 2, 3, 4, 5, 1, 2, 3);
        List<Integer> target = Arrays.asList(1, 2, 3);
        assertEquals(OptionalInt.of(0), Index.ofSubList(source, target));
        assertEquals(OptionalInt.of(5), Index.ofSubList(source, 1, target));
    }

    @Test
    public void testLast() {
        int[] array = { 1, 2, 3, 1, 2, 3 };
        assertEquals(OptionalInt.of(3), Index.last(array, 1));
        assertEquals(OptionalInt.of(3), Index.last(array, 1, 3));
    }

    @Test
    public void testLastOfSubArray() {
        int[] source = { 1, 2, 3, 4, 5, 1, 2, 3 };
        int[] target = { 1, 2, 3 };
        assertEquals(OptionalInt.of(5), Index.lastOfSubArray(source, target));
        assertEquals(OptionalInt.of(5), Index.lastOfSubArray(source, 5, target));
        assertEquals(OptionalInt.of(0), Index.lastOfSubArray(source, 4, target));
    }

    @Test
    public void testAllOf() {
        int[] array = { 1, 2, 1, 3, 1 };
        BitSet expected = new BitSet();
        expected.set(0);
        expected.set(2);
        expected.set(4);
        assertEquals(expected, Index.allOf(array, 1));
    }

    @Test
    public void testAllOfWithPredicate() {
        String[] array = { "apple", "banana", "avocado", "orange" };
        Predicate<String> startsWithA = s -> s.startsWith("a");
        BitSet expected = new BitSet();
        expected.set(0);
        expected.set(2);
        assertEquals(expected, Index.allOf(array, startsWithA));
    }

    @Test
    public void testOf_withNullAndEmptyInputs() {
        // Primitive arrays
        assertEquals(OptionalInt.empty(), Index.of((boolean[]) null, true));
        assertEquals(OptionalInt.empty(), Index.of(new boolean[0], true));
        assertEquals(OptionalInt.empty(), Index.of((char[]) null, 'a'));
        assertEquals(OptionalInt.empty(), Index.of(new char[0], 'a'));
        assertEquals(OptionalInt.empty(), Index.of((byte[]) null, (byte) 1));
        assertEquals(OptionalInt.empty(), Index.of(new byte[0], (byte) 1));
        assertEquals(OptionalInt.empty(), Index.of((short[]) null, (short) 1));
        assertEquals(OptionalInt.empty(), Index.of(new short[0], (short) 1));
        assertEquals(OptionalInt.empty(), Index.of((int[]) null, 1));
        assertEquals(OptionalInt.empty(), Index.of(new int[0], 1));
        assertEquals(OptionalInt.empty(), Index.of((long[]) null, 1L));
        assertEquals(OptionalInt.empty(), Index.of(new long[0], 1L));
        assertEquals(OptionalInt.empty(), Index.of((float[]) null, 1.0f));
        assertEquals(OptionalInt.empty(), Index.of(new float[0], 1.0f));
        assertEquals(OptionalInt.empty(), Index.of((double[]) null, 1.0));
        assertEquals(OptionalInt.empty(), Index.of(new double[0], 1.0));

        // Object array
        assertEquals(OptionalInt.empty(), Index.of((Object[]) null, "a"));
        assertEquals(OptionalInt.empty(), Index.of(new Object[0], "a"));

        // Collection
        assertEquals(OptionalInt.empty(), Index.of((List<String>) null, "a"));
        assertEquals(OptionalInt.empty(), Index.of(Collections.emptyList(), "a"));

        // Iterator
        assertEquals(OptionalInt.empty(), Index.of((Iterator<String>) null, "a"));
        assertEquals(OptionalInt.empty(), Index.of(Collections.emptyIterator(), "a"));

        // String
        assertEquals(OptionalInt.empty(), Index.of((String) null, 'a'));
        assertEquals(OptionalInt.empty(), Index.of("", 'a'));
        assertEquals(OptionalInt.empty(), Index.of((String) null, "a"));
        assertEquals(OptionalInt.empty(), Index.of("", "a"));
    }

    @Test
    public void testOfIterator() {
        List<String> list = Arrays.asList("a", "b", "c", "a", "b");
        assertEquals(OptionalInt.of(0), Index.of(list.iterator(), "a"));
        assertEquals(OptionalInt.of(1), Index.of(list.iterator(), "b"));
        assertEquals(OptionalInt.of(3), Index.of(list.iterator(), "a", 3));
        assertEquals(OptionalInt.empty(), Index.of(list.iterator(), "d"));
    }

    @Test
    public void testOfDoubleWithTolerance_EdgeCases() {
        double[] array = { 1.0, 1.05, 1.1, 1.15, 1.2 };
        assertEquals(OptionalInt.of(2), Index.of(array, 1.1, 0.0));
        assertEquals(OptionalInt.of(1), Index.of(array, 1.0, 0.051, 1));
        assertEquals(OptionalInt.of(0), Index.of(array, 1.0, 0.04999));
        assertEquals(OptionalInt.empty(), Index.of(array, 0.9, 0.05));
    }
    //</editor-fold>

    //<editor-fold desc="ofSubArray/ofSubList Tests">

    @Test
    public void testOfSubArray_EdgeCases() {
        int[] source = { 1, 2, 3, 4 };
        // Empty target
        assertEquals(OptionalInt.of(0), Index.ofSubArray(source, new int[0]));
        assertEquals(OptionalInt.of(2), Index.ofSubArray(source, 2, new int[0]));
        assertEquals(OptionalInt.of(4), Index.ofSubArray(source, 5, new int[0])); // Clamped to source length
        assertEquals(OptionalInt.of(0), Index.ofSubArray(source, -1, new int[0])); // Clamped to 0

        // Target longer than source
        assertEquals(OptionalInt.empty(), Index.ofSubArray(source, new int[] { 1, 2, 3, 4, 5 }));

        // Null inputs
        assertEquals(OptionalInt.empty(), Index.ofSubArray((int[]) null, new int[] { 1 }));
        assertEquals(OptionalInt.empty(), Index.ofSubArray(source, null));
        assertEquals(OptionalInt.empty(), Index.ofSubArray((int[]) null, null));
        assertEquals(OptionalInt.empty(), Index.ofSubArray(source, 2, null));

        // No match
        assertEquals(OptionalInt.empty(), Index.ofSubArray(source, new int[] { 5, 6 }));
    }

    @Test
    public void testOfSubList_EdgeCases() {
        List<Integer> source = Arrays.asList(1, 2, 3, 4);
        // Empty target
        assertEquals(OptionalInt.of(0), Index.ofSubList(source, Collections.emptyList()));
        assertEquals(OptionalInt.of(2), Index.ofSubList(source, 2, Collections.emptyList()));

        // Target longer than source
        assertEquals(OptionalInt.empty(), Index.ofSubList(source, Arrays.asList(1, 2, 3, 4, 5)));

        // Null inputs
        assertEquals(OptionalInt.empty(), Index.ofSubList(null, Arrays.asList(1)));
        assertEquals(OptionalInt.empty(), Index.ofSubList(source, null));
        assertEquals(OptionalInt.empty(), Index.ofSubList(null, null));
    }
    //</editor-fold>

    //<editor-fold desc="last Tests">

    @Test
    public void testLast_withNullAndEmptyInputs() {
        // Primitives
        assertEquals(OptionalInt.empty(), Index.last((boolean[]) null, true));
        assertEquals(OptionalInt.empty(), Index.last(new boolean[0], true));
        // ... (similar tests for other primitive types)

        // Objects
        assertEquals(OptionalInt.empty(), Index.last((Object[]) null, "a"));
        assertEquals(OptionalInt.empty(), Index.last(new Object[0], "a"));

        // Collections
        assertEquals(OptionalInt.empty(), Index.last((List<String>) null, "a"));
        assertEquals(OptionalInt.empty(), Index.last(Collections.emptyList(), "a"));

        // String
        assertEquals(OptionalInt.empty(), Index.last((String) null, 'a'));
        assertEquals(OptionalInt.empty(), Index.last("", 'a'));
        assertEquals(OptionalInt.empty(), Index.last((String) null, "a"));
        assertEquals(OptionalInt.empty(), Index.last("", "a"));
    }

    @Test
    public void testLastOfIgnoreCase() {
        String str = "AbcDefAbc";
        assertEquals(OptionalInt.of(6), Index.lastOfIgnoreCase(str, "ABC"));
        assertEquals(OptionalInt.of(0), Index.lastOfIgnoreCase(str, "ABC", 5));
        assertEquals(OptionalInt.empty(), Index.lastOfIgnoreCase(str, "XYZ"));
    }
    //</editor-fold>

    //<editor-fold desc="lastOfSubArray/lastOfSubList Tests">

    @Test
    public void testLastOfSubArray_EdgeCases() {
        int[] source = { 1, 2, 3, 1, 2, 3, 4 };
        // Empty target
        assertEquals(OptionalInt.of(7), Index.lastOfSubArray(source, new int[0]));
        assertEquals(OptionalInt.of(5), Index.lastOfSubArray(source, 5, new int[0]));
        assertEquals(OptionalInt.empty(), Index.lastOfSubArray(source, -1, new int[0]));

        // Target longer than source
        assertEquals(OptionalInt.empty(), Index.lastOfSubArray(new int[] { 1, 2 }, new int[] { 1, 2, 3 }));

        // Null inputs
        assertEquals(OptionalInt.empty(), Index.lastOfSubArray(null, new int[] { 1 }));
        assertEquals(OptionalInt.empty(), Index.lastOfSubArray(source, null));

        // No match
        assertEquals(OptionalInt.empty(), Index.lastOfSubArray(source, new int[] { 5, 6 }));
    }

    @Test
    public void testLastOfSubList_EdgeCases() {
        List<Integer> source = Arrays.asList(1, 2, 3, 1, 2, 3, 4);
        List<Integer> target = Arrays.asList(1, 2, 3);
        assertEquals(OptionalInt.of(3), Index.lastOfSubList(source, target));
        assertEquals(OptionalInt.of(3), Index.lastOfSubList(source, 3, target));
        assertEquals(OptionalInt.of(0), Index.lastOfSubList(source, 2, target));
    }
    //</editor-fold>

    //<editor-fold desc="allOf Tests">

    @Test
    public void testAllOf_withNullAndEmpty() {
        // Primitives
        assertTrue(Index.allOf((int[]) null, 1).isEmpty());
        assertTrue(Index.allOf(new int[0], 1).isEmpty());

        // Objects
        assertTrue(Index.allOf((Object[]) null, "a").isEmpty());
        assertTrue(Index.allOf(new Object[0], "a").isEmpty());

        // Collections
        assertTrue(Index.allOf((List<String>) null, "a").isEmpty());
        assertTrue(Index.allOf(Collections.emptyList(), "a").isEmpty());

        // Predicate based
        Predicate<String> p = s -> s.length() > 0;
        assertTrue(Index.allOf((String[]) null, p).isEmpty());
        assertTrue(Index.allOf(new String[0], p).isEmpty());
        assertTrue(Index.allOf((List<String>) null, p).isEmpty());
        assertTrue(Index.allOf(Collections.emptyList(), p).isEmpty());
    }

    @Test
    public void testAllOf_withFromIndex() {
        int[] array = { 1, 2, 1, 3, 1 };
        BitSet expected = new BitSet();
        expected.set(2);
        expected.set(4);
        assertEquals(expected, Index.allOf(array, 1, 1));
        assertEquals(expected, Index.allOf(array, 1, 2));

        BitSet expected2 = new BitSet();
        expected2.set(4);
        assertEquals(expected2, Index.allOf(array, 1, 3));
        assertTrue(Index.allOf(array, 1, 5).isEmpty());
    }

    @Test
    public void testAllOf_collectionWithFromIndex() {
        List<String> list = Arrays.asList("a", "b", "a", "c", "a");
        BitSet expected = new BitSet();
        expected.set(2);
        expected.set(4);
        assertEquals(expected, Index.allOf(list, "a", 1));
    }

    @Test
    public void testAllOf_predicateWithFromIndex() {
        String[] array = { "apple", "banana", "avocado", "orange", "apricot" };
        Predicate<String> startsWithA = s -> s.startsWith("a");
        BitSet expected = new BitSet();
        expected.set(2);
        expected.set(4);
        assertEquals(expected, Index.allOf(array, startsWithA, 1));
    }

    @Test
    public void testAllOf_doubleWithTolerance() {
        double[] array = { 1.0, 1.05, 2.0, 1.1, 0.95 };
        BitSet expected = new BitSet();
        expected.set(0);
        expected.set(1);
        expected.set(3);
        expected.set(4);
        assertEquals(expected, Index.allOf(array, 1.0, 0.1));

        BitSet expectedFromIndex = new BitSet();
        expectedFromIndex.set(1);
        expectedFromIndex.set(3);
        expectedFromIndex.set(4);
        assertEquals(expectedFromIndex, Index.allOf(array, 1.0, 0.1, 1));
    }
}

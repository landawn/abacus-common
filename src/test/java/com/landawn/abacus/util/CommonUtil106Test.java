package com.landawn.abacus.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import com.landawn.abacus.TestBase;

@Tag("new-test")
public class CommonUtil106Test extends TestBase {

    @Test
    public void testFindLastNonNullArray() {
        String[] array = { null, "a", null, "b", "c", null };

        Assertions.assertEquals("b", N.findLastNonNull(array, s -> s.startsWith("b")).orElse(null));
        Assertions.assertEquals("c", N.findLastNonNull(array, s -> s.startsWith("c")).orElse(null));
        Assertions.assertFalse(N.findLastNonNull(array, s -> s.equals("x")).isPresent());
        Assertions.assertFalse(N.findLastNonNull((String[]) null, s -> true).isPresent());

        String[] allNull = { null, null, null };
        Assertions.assertFalse(N.findLastNonNull(allNull, s -> true).isPresent());
    }

    @Test
    public void testFindLastNonNullIterable() {
        List<String> list = Arrays.asList(null, "a", null, "b", "c", null);

        Assertions.assertEquals("b", N.findLastNonNull(list, s -> s.startsWith("b")).orElse(null));
        Assertions.assertEquals("c", N.findLastNonNull(list, s -> s.startsWith("c")).orElse(null));
        Assertions.assertFalse(N.findLastNonNull(list, s -> s.equals("x")).isPresent());
        Assertions.assertFalse(N.findLastNonNull((Iterable<String>) null, s -> true).isPresent());

        List<String> allNull = Arrays.asList(null, null, null);
        Assertions.assertFalse(N.findLastNonNull(allNull, s -> true).isPresent());
    }

    @Test
    public void testHaveSameElementsBoolean() {
        boolean[] a = { true, false, true, false };
        boolean[] b = { false, true, false, true };
        boolean[] c = { true, true, false, false };
        boolean[] d = { true, false, true };

        Assertions.assertTrue(N.haveSameElements(a, b));
        Assertions.assertTrue(N.haveSameElements(a, c));
        Assertions.assertFalse(N.haveSameElements(a, d));
        Assertions.assertTrue(N.haveSameElements((boolean[]) null, (boolean[]) null));
        Assertions.assertTrue(N.haveSameElements(new boolean[0], new boolean[0]));
        Assertions.assertFalse(N.haveSameElements(a, null));
        Assertions.assertFalse(N.haveSameElements(null, a));

        Assertions.assertTrue(N.haveSameElements(a, a));
    }

    @Test
    public void testHaveSameElementsChar() {
        char[] a = { 'a', 'b', 'c', 'b' };
        char[] b = { 'b', 'a', 'c', 'b' };
        char[] c = { 'a', 'b', 'b', 'c' };
        char[] d = { 'a', 'b', 'c' };

        Assertions.assertTrue(N.haveSameElements(a, b));
        Assertions.assertTrue(N.haveSameElements(a, c));
        Assertions.assertFalse(N.haveSameElements(a, d));
        Assertions.assertTrue(N.haveSameElements((char[]) null, (char[]) null));
        Assertions.assertTrue(N.haveSameElements(new char[0], new char[0]));
    }

    @Test
    public void testHaveSameElementsByte() {
        byte[] a = { 1, 2, 3, 2 };
        byte[] b = { 2, 1, 3, 2 };
        byte[] c = { 1, 2, 2, 3 };
        byte[] d = { 1, 2, 3 };

        Assertions.assertTrue(N.haveSameElements(a, b));
        Assertions.assertTrue(N.haveSameElements(a, c));
        Assertions.assertFalse(N.haveSameElements(a, d));
        Assertions.assertTrue(N.haveSameElements((byte[]) null, (byte[]) null));
        Assertions.assertTrue(N.haveSameElements(new byte[0], new byte[0]));
    }

    @Test
    public void testHaveSameElementsShort() {
        short[] a = { 1, 2, 3, 2 };
        short[] b = { 2, 1, 3, 2 };
        short[] c = { 1, 2, 2, 3 };
        short[] d = { 1, 2, 3 };

        Assertions.assertTrue(N.haveSameElements(a, b));
        Assertions.assertTrue(N.haveSameElements(a, c));
        Assertions.assertFalse(N.haveSameElements(a, d));
        Assertions.assertTrue(N.haveSameElements((short[]) null, (short[]) null));
        Assertions.assertTrue(N.haveSameElements(new short[0], new short[0]));
    }

    @Test
    public void testHaveSameElementsInt() {
        int[] a = { 1, 2, 3, 2 };
        int[] b = { 2, 1, 3, 2 };
        int[] c = { 1, 2, 2, 3 };
        int[] d = { 1, 2, 3 };
        int[] e = { 1, 2, 2, 2 };

        Assertions.assertTrue(N.haveSameElements(a, b));
        Assertions.assertTrue(N.haveSameElements(a, c));
        Assertions.assertFalse(N.haveSameElements(a, d));
        Assertions.assertFalse(N.haveSameElements(a, e));
        Assertions.assertTrue(N.haveSameElements((int[]) null, (int[]) null));
        Assertions.assertTrue(N.haveSameElements(new int[0], new int[0]));
    }

    @Test
    public void testHaveSameElementsLong() {
        long[] a = { 1L, 2L, 3L, 2L };
        long[] b = { 2L, 1L, 3L, 2L };
        long[] c = { 1L, 2L, 2L, 3L };
        long[] d = { 1L, 2L, 3L };

        Assertions.assertTrue(N.haveSameElements(a, b));
        Assertions.assertTrue(N.haveSameElements(a, c));
        Assertions.assertFalse(N.haveSameElements(a, d));
        Assertions.assertTrue(N.haveSameElements((long[]) null, (long[]) null));
        Assertions.assertTrue(N.haveSameElements(new long[0], new long[0]));
    }

    @Test
    public void testHaveSameElementsFloat() {
        float[] a = { 1.0f, 2.0f, 3.0f, 2.0f };
        float[] b = { 2.0f, 1.0f, 3.0f, 2.0f };
        float[] c = { 1.0f, 2.0f, 2.0f, 3.0f };
        float[] d = { 1.0f, 2.0f, 3.0f };

        Assertions.assertTrue(N.haveSameElements(a, b));
        Assertions.assertTrue(N.haveSameElements(a, c));
        Assertions.assertFalse(N.haveSameElements(a, d));
        Assertions.assertTrue(N.haveSameElements((float[]) null, (float[]) null));
        Assertions.assertTrue(N.haveSameElements(new float[0], new float[0]));
    }

    @Test
    public void testHaveSameElementsDouble() {
        double[] a = { 1.0, 2.0, 3.0, 2.0 };
        double[] b = { 2.0, 1.0, 3.0, 2.0 };
        double[] c = { 1.0, 2.0, 2.0, 3.0 };
        double[] d = { 1.0, 2.0, 3.0 };

        Assertions.assertTrue(N.haveSameElements(a, b));
        Assertions.assertTrue(N.haveSameElements(a, c));
        Assertions.assertFalse(N.haveSameElements(a, d));
        Assertions.assertTrue(N.haveSameElements((double[]) null, (double[]) null));
        Assertions.assertTrue(N.haveSameElements(new double[0], new double[0]));
    }

    @Test
    public void testHaveSameElementsObject() {
        String[] a = { "a", "b", "c", "b" };
        String[] b = { "b", "a", "c", "b" };
        String[] c = { "a", "b", "b", "c" };
        String[] d = { "a", "b", "c" };

        Assertions.assertTrue(N.haveSameElements(a, b));
        Assertions.assertTrue(N.haveSameElements(a, c));
        Assertions.assertFalse(N.haveSameElements(a, d));
        Assertions.assertTrue(N.haveSameElements((Object[]) null, (Object[]) null));
        Assertions.assertTrue(N.haveSameElements(new Object[0], new Object[0]));

        String[] withNulls1 = { "a", null, "b", null };
        String[] withNulls2 = { null, "a", null, "b" };
        Assertions.assertTrue(N.haveSameElements(withNulls1, withNulls2));
    }

    @Test
    public void testHaveSameElementsCollection() {
        List<String> a = Arrays.asList("a", "b", "c", "b");
        List<String> b = Arrays.asList("b", "a", "c", "b");
        List<String> c = Arrays.asList("a", "b", "b", "c");
        List<String> d = Arrays.asList("a", "b", "c");

        Assertions.assertTrue(N.haveSameElements(a, b));
        Assertions.assertTrue(N.haveSameElements(a, c));
        Assertions.assertFalse(N.haveSameElements(a, d));
        Assertions.assertTrue(N.haveSameElements((Collection<?>) null, (Collection<?>) null));
        Assertions.assertTrue(N.haveSameElements(new ArrayList<>(), new ArrayList<>()));

        Assertions.assertTrue(N.haveSameElements(a, a));
    }

    @Test
    public void testMismatchBoolean() {
        boolean[] a = { true, false, true, false };
        boolean[] b = { true, false, true, false };
        boolean[] c = { true, true, false, false };
        boolean[] d = { true, false, true };

        Assertions.assertEquals(-1, N.mismatch(a, b));
        Assertions.assertEquals(1, N.mismatch(a, c));
        Assertions.assertEquals(3, N.mismatch(a, d));
        Assertions.assertEquals(-1, N.mismatch((boolean[]) null, (boolean[]) null));
        Assertions.assertEquals(-1, N.mismatch(new boolean[0], new boolean[0]));
        Assertions.assertEquals(0, N.mismatch(a, null));
        Assertions.assertEquals(0, N.mismatch(null, a));

        Assertions.assertEquals(-1, N.mismatch(a, a));
    }

    @Test
    public void testMismatchBooleanRange() {
        boolean[] a = { true, false, true, false };
        boolean[] b = { false, false, true, true };

        Assertions.assertEquals(-1, N.mismatch(a, 1, b, 1, 2));
        Assertions.assertEquals(0, N.mismatch(a, 0, b, 0, 2));
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> N.mismatch(a, 0, b, 0, 10));
        Assertions.assertThrows(IllegalArgumentException.class, () -> N.mismatch(a, 0, b, 0, -1));

        Assertions.assertEquals(-1, N.mismatch(a, 1, a, 1, 2));

        Assertions.assertEquals(-1, N.mismatch(a, 0, b, 0, 0));
    }

    @Test
    public void testMismatchChar() {
        char[] a = { 'a', 'b', 'c', 'd' };
        char[] b = { 'a', 'b', 'c', 'd' };
        char[] c = { 'a', 'x', 'c', 'd' };
        char[] d = { 'a', 'b', 'c' };

        Assertions.assertEquals(-1, N.mismatch(a, b));
        Assertions.assertEquals(1, N.mismatch(a, c));
        Assertions.assertEquals(3, N.mismatch(a, d));
        Assertions.assertEquals(-1, N.mismatch((char[]) null, (char[]) null));
        Assertions.assertEquals(-1, N.mismatch(new char[0], new char[0]));
        Assertions.assertEquals(0, N.mismatch(a, null));
        Assertions.assertEquals(0, N.mismatch(null, a));
    }

    @Test
    public void testMismatchCharRange() {
        char[] a = { 'a', 'b', 'c', 'd' };
        char[] b = { 'x', 'b', 'c', 'y' };

        Assertions.assertEquals(-1, N.mismatch(a, 1, b, 1, 2));
        Assertions.assertEquals(0, N.mismatch(a, 0, b, 0, 2));
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> N.mismatch(a, 0, b, 0, 10));
    }

    @Test
    public void testMismatchByte() {
        byte[] a = { 1, 2, 3, 4 };
        byte[] b = { 1, 2, 3, 4 };
        byte[] c = { 1, 5, 3, 4 };
        byte[] d = { 1, 2, 3 };

        Assertions.assertEquals(-1, N.mismatch(a, b));
        Assertions.assertEquals(1, N.mismatch(a, c));
        Assertions.assertEquals(3, N.mismatch(a, d));
        Assertions.assertEquals(-1, N.mismatch((byte[]) null, (byte[]) null));
        Assertions.assertEquals(-1, N.mismatch(new byte[0], new byte[0]));
    }

    @Test
    public void testMismatchByteRange() {
        byte[] a = { 1, 2, 3, 4 };
        byte[] b = { 5, 2, 3, 6 };

        Assertions.assertEquals(-1, N.mismatch(a, 1, b, 1, 2));
        Assertions.assertEquals(0, N.mismatch(a, 0, b, 0, 2));
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> N.mismatch(a, 0, b, 0, 10));
    }

    @Test
    public void testMismatchShort() {
        short[] a = { 1, 2, 3, 4 };
        short[] b = { 1, 2, 3, 4 };
        short[] c = { 1, 5, 3, 4 };
        short[] d = { 1, 2, 3 };

        Assertions.assertEquals(-1, N.mismatch(a, b));
        Assertions.assertEquals(1, N.mismatch(a, c));
        Assertions.assertEquals(3, N.mismatch(a, d));
        Assertions.assertEquals(-1, N.mismatch((short[]) null, (short[]) null));
        Assertions.assertEquals(-1, N.mismatch(new short[0], new short[0]));
    }

    @Test
    public void testMismatchShortRange() {
        short[] a = { 1, 2, 3, 4 };
        short[] b = { 5, 2, 3, 6 };

        Assertions.assertEquals(-1, N.mismatch(a, 1, b, 1, 2));
        Assertions.assertEquals(0, N.mismatch(a, 0, b, 0, 2));
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> N.mismatch(a, 0, b, 0, 10));
    }

    @Test
    public void testMismatchInt() {
        int[] a = { 1, 2, 3, 4 };
        int[] b = { 1, 2, 3, 4 };
        int[] c = { 1, 5, 3, 4 };
        int[] d = { 1, 2, 3 };

        Assertions.assertEquals(-1, N.mismatch(a, b));
        Assertions.assertEquals(1, N.mismatch(a, c));
        Assertions.assertEquals(3, N.mismatch(a, d));
        Assertions.assertEquals(-1, N.mismatch((int[]) null, (int[]) null));
        Assertions.assertEquals(-1, N.mismatch(new int[0], new int[0]));
    }

    @Test
    public void testMismatchIntRange() {
        int[] a = { 1, 2, 3, 4 };
        int[] b = { 5, 2, 3, 6 };

        Assertions.assertEquals(-1, N.mismatch(a, 1, b, 1, 2));
        Assertions.assertEquals(0, N.mismatch(a, 0, b, 0, 2));
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> N.mismatch(a, 0, b, 0, 10));
    }

    @Test
    public void testMismatchLong() {
        long[] a = { 1L, 2L, 3L, 4L };
        long[] b = { 1L, 2L, 3L, 4L };
        long[] c = { 1L, 5L, 3L, 4L };
        long[] d = { 1L, 2L, 3L };

        Assertions.assertEquals(-1, N.mismatch(a, b));
        Assertions.assertEquals(1, N.mismatch(a, c));
        Assertions.assertEquals(3, N.mismatch(a, d));
        Assertions.assertEquals(-1, N.mismatch((long[]) null, (long[]) null));
        Assertions.assertEquals(-1, N.mismatch(new long[0], new long[0]));
    }

    @Test
    public void testMismatchLongRange() {
        long[] a = { 1L, 2L, 3L, 4L };
        long[] b = { 5L, 2L, 3L, 6L };

        Assertions.assertEquals(-1, N.mismatch(a, 1, b, 1, 2));
        Assertions.assertEquals(0, N.mismatch(a, 0, b, 0, 2));
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> N.mismatch(a, 0, b, 0, 10));
    }

    @Test
    public void testMismatchFloat() {
        float[] a = { 1.0f, 2.0f, 3.0f, 4.0f };
        float[] b = { 1.0f, 2.0f, 3.0f, 4.0f };
        float[] c = { 1.0f, 5.0f, 3.0f, 4.0f };
        float[] d = { 1.0f, 2.0f, 3.0f };

        Assertions.assertEquals(-1, N.mismatch(a, b));
        Assertions.assertEquals(1, N.mismatch(a, c));
        Assertions.assertEquals(3, N.mismatch(a, d));
        Assertions.assertEquals(-1, N.mismatch((float[]) null, (float[]) null));
        Assertions.assertEquals(-1, N.mismatch(new float[0], new float[0]));
    }

    @Test
    public void testMismatchFloatRange() {
        float[] a = { 1.0f, 2.0f, 3.0f, 4.0f };
        float[] b = { 5.0f, 2.0f, 3.0f, 6.0f };

        Assertions.assertEquals(-1, N.mismatch(a, 1, b, 1, 2));
        Assertions.assertEquals(0, N.mismatch(a, 0, b, 0, 2));
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> N.mismatch(a, 0, b, 0, 10));
    }

    @Test
    public void testMismatchDouble() {
        double[] a = { 1.0, 2.0, 3.0, 4.0 };
        double[] b = { 1.0, 2.0, 3.0, 4.0 };
        double[] c = { 1.0, 5.0, 3.0, 4.0 };
        double[] d = { 1.0, 2.0, 3.0 };

        Assertions.assertEquals(-1, N.mismatch(a, b));
        Assertions.assertEquals(1, N.mismatch(a, c));
        Assertions.assertEquals(3, N.mismatch(a, d));
        Assertions.assertEquals(-1, N.mismatch((double[]) null, (double[]) null));
        Assertions.assertEquals(-1, N.mismatch(new double[0], new double[0]));
    }

    @Test
    public void testMismatchDoubleRange() {
        double[] a = { 1.0, 2.0, 3.0, 4.0 };
        double[] b = { 5.0, 2.0, 3.0, 6.0 };

        Assertions.assertEquals(-1, N.mismatch(a, 1, b, 1, 2));
        Assertions.assertEquals(0, N.mismatch(a, 0, b, 0, 2));
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> N.mismatch(a, 0, b, 0, 10));
    }

    @Test
    public void testMismatchObjectArrays() {
        String[] a = { "a", "b", "c", "d" };
        String[] b = { "a", "b", "c", "d" };
        String[] c = { "a", "x", "c", "d" };
        String[] d = { "a", "b", "c" };

        Assertions.assertEquals(-1, N.mismatch(a, b));
        Assertions.assertEquals(1, N.mismatch(a, c));
        Assertions.assertEquals(3, N.mismatch(a, d));
        Assertions.assertEquals(-1, N.mismatch((String[]) null, (String[]) null));
        Assertions.assertEquals(-1, N.mismatch(new String[0], new String[0]));
        Assertions.assertEquals(0, N.mismatch(a, null));
        Assertions.assertEquals(0, N.mismatch(null, a));
    }

    @Test
    public void testMismatchObjectArraysRange() {
        String[] a = { "a", "b", "c", "d" };
        String[] b = { "x", "b", "c", "y" };

        Assertions.assertEquals(-1, N.mismatch(a, 1, b, 1, 2));
        Assertions.assertEquals(0, N.mismatch(a, 0, b, 0, 2));
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> N.mismatch(a, 0, b, 0, 10));
    }

    @Test
    public void testMismatchObjectArraysWithComparator() {
        String[] a = { "A", "B", "C", "D" };
        String[] b = { "a", "b", "c", "d" };
        String[] c = { "a", "x", "c", "d" };

        Assertions.assertEquals(-1, N.mismatch(a, b, String.CASE_INSENSITIVE_ORDER));
        Assertions.assertEquals(1, N.mismatch(a, c, String.CASE_INSENSITIVE_ORDER));
        Assertions.assertEquals(-1, N.mismatch(a, a));
    }

    @Test
    public void testMismatchObjectArraysRangeWithComparator() {
        String[] a = { "A", "B", "C", "D" };
        String[] b = { "x", "b", "c", "y" };

        Assertions.assertEquals(-1, N.mismatch(a, 1, b, 1, 2, String.CASE_INSENSITIVE_ORDER));
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> N.mismatch(a, 0, b, 0, 10, String.CASE_INSENSITIVE_ORDER));
    }

    @Test
    public void testMismatchCollections() {
        List<String> a = Arrays.asList("a", "b", "c", "d");
        List<String> b = Arrays.asList("x", "b", "c", "y");

        Assertions.assertEquals(-1, N.mismatch(a, 1, b, 1, 2));
        Assertions.assertEquals(0, N.mismatch(a, 0, b, 0, 2));
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> N.mismatch(a, 0, b, 0, 10));
    }

    @Test
    public void testMismatchCollectionsWithComparator() {
        List<String> a = Arrays.asList("A", "B", "C", "D");
        List<String> b = Arrays.asList("x", "b", "c", "y");

        Assertions.assertEquals(-1, N.mismatch(a, 1, b, 1, 2, String.CASE_INSENSITIVE_ORDER));
        Assertions.assertEquals(-1, N.mismatch(a, 1, a, 1, 2, String.CASE_INSENSITIVE_ORDER));
    }

    @Test
    public void testMismatchIterables() {
        List<String> a = Arrays.asList("a", "b", "c", "d");
        List<String> b = Arrays.asList("a", "b", "c", "d");
        List<String> c = Arrays.asList("a", "x", "c", "d");
        List<String> d = Arrays.asList("a", "b", "c");

        Assertions.assertEquals(-1, N.mismatch((Iterable<String>) a, (Iterable<String>) b));
        Assertions.assertEquals(1, N.mismatch((Iterable<String>) a, (Iterable<String>) c));
        Assertions.assertEquals(3, N.mismatch((Iterable<String>) a, (Iterable<String>) d));
        Assertions.assertEquals(-1, N.mismatch((Iterable<String>) null, (Iterable<String>) null));
        Assertions.assertEquals(-1, N.mismatch(new ArrayList<String>(), new ArrayList<String>()));
        Assertions.assertEquals(0, N.mismatch((Iterable<String>) a, (Iterable<String>) null));
        Assertions.assertEquals(0, N.mismatch((Iterable<String>) null, (Iterable<String>) a));

        Assertions.assertEquals(-1, N.mismatch((Iterable<String>) a, (Iterable<String>) a));
    }

    @Test
    public void testMismatchIterablesWithComparator() {
        List<String> a = Arrays.asList("A", "B", "C", "D");
        List<String> b = Arrays.asList("a", "b", "c", "d");
        List<String> c = Arrays.asList("a", "x", "c", "d");

        Assertions.assertEquals(-1, N.mismatch((Iterable<String>) a, (Iterable<String>) b, String.CASE_INSENSITIVE_ORDER));
        Assertions.assertEquals(1, N.mismatch((Iterable<String>) a, (Iterable<String>) c, String.CASE_INSENSITIVE_ORDER));
    }

    @Test
    public void testMismatchIterators() {
        List<String> a = Arrays.asList("a", "b", "c", "d");
        List<String> b = Arrays.asList("a", "b", "c", "d");
        List<String> c = Arrays.asList("a", "x", "c", "d");
        List<String> d = Arrays.asList("a", "b", "c");
        List<String> e = Arrays.asList("a", "b", "c", "d", "e");

        Assertions.assertEquals(-1, N.mismatch(a.iterator(), b.iterator()));
        Assertions.assertEquals(1, N.mismatch(a.iterator(), c.iterator()));
        Assertions.assertEquals(3, N.mismatch(a.iterator(), d.iterator()));
        Assertions.assertEquals(4, N.mismatch(a.iterator(), e.iterator()));
        Assertions.assertEquals(-1, N.mismatch((Iterator<String>) null, (Iterator<String>) null));

        Iterator<String> iter = a.iterator();
        Assertions.assertEquals(-1, N.mismatch(iter, iter));
    }

    @Test
    public void testMismatchIteratorsWithComparator() {
        List<String> a = Arrays.asList("A", "B", "C", "D");
        List<String> b = Arrays.asList("a", "b", "c", "d");
        List<String> c = Arrays.asList("a", "x", "c", "d");

        Assertions.assertEquals(-1, N.mismatch(a.iterator(), b.iterator(), String.CASE_INSENSITIVE_ORDER));
        Assertions.assertEquals(1, N.mismatch(a.iterator(), c.iterator(), String.CASE_INSENSITIVE_ORDER));
    }

    @Test
    public void testReverseBoolean() {
        boolean[] a = { true, false, true, false };
        boolean[] expected = { false, true, false, true };
        N.reverse(a);
        Assertions.assertArrayEquals(expected, a);

        boolean[] empty = {};
        N.reverse(empty);
        Assertions.assertArrayEquals(new boolean[0], empty);

        N.reverse((boolean[]) null);

        boolean[] single = { true };
        N.reverse(single);
        Assertions.assertArrayEquals(new boolean[] { true }, single);
    }

    @Test
    public void testReverseBooleanRange() {
        boolean[] a = { true, false, true, false, true };
        boolean[] expected = { true, false, true, false, true };
        N.reverse(a, 1, 4);
        expected[1] = false;
        expected[2] = true;
        expected[3] = false;
        Assertions.assertArrayEquals(expected, a);

        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> N.reverse(a, -1, 3));
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> N.reverse(a, 0, 10));
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> N.reverse(a, 3, 2));

        boolean[] b = { true, false, true };
        boolean[] original = b.clone();
        N.reverse(b, 1, 1);
        Assertions.assertArrayEquals(original, b);
    }

    @Test
    public void testReverseChar() {
        char[] a = { 'a', 'b', 'c', 'd' };
        char[] expected = { 'd', 'c', 'b', 'a' };
        N.reverse(a);
        Assertions.assertArrayEquals(expected, a);

        char[] empty = {};
        N.reverse(empty);
        Assertions.assertArrayEquals(new char[0], empty);

        N.reverse((char[]) null);
    }

    @Test
    public void testReverseCharRange() {
        char[] a = { 'a', 'b', 'c', 'd', 'e' };
        char[] expected = { 'a', 'd', 'c', 'b', 'e' };
        N.reverse(a, 1, 4);
        Assertions.assertArrayEquals(expected, a);

        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> N.reverse(a, -1, 3));
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> N.reverse(a, 0, 10));
    }

    @Test
    public void testReverseByte() {
        byte[] a = { 1, 2, 3, 4 };
        byte[] expected = { 4, 3, 2, 1 };
        N.reverse(a);
        Assertions.assertArrayEquals(expected, a);

        byte[] empty = {};
        N.reverse(empty);
        Assertions.assertArrayEquals(new byte[0], empty);

        N.reverse((byte[]) null);
    }

    @Test
    public void testReverseByteRange() {
        byte[] a = { 1, 2, 3, 4, 5 };
        byte[] expected = { 1, 4, 3, 2, 5 };
        N.reverse(a, 1, 4);
        Assertions.assertArrayEquals(expected, a);
    }

    @Test
    public void testReverseShort() {
        short[] a = { 1, 2, 3, 4 };
        short[] expected = { 4, 3, 2, 1 };
        N.reverse(a);
        Assertions.assertArrayEquals(expected, a);

        short[] empty = {};
        N.reverse(empty);
        Assertions.assertArrayEquals(new short[0], empty);

        N.reverse((short[]) null);
    }

    @Test
    public void testReverseShortRange() {
        short[] a = { 1, 2, 3, 4, 5 };
        short[] expected = { 1, 4, 3, 2, 5 };
        N.reverse(a, 1, 4);
        Assertions.assertArrayEquals(expected, a);
    }

    @Test
    public void testReverseInt() {
        int[] a = { 1, 2, 3, 4 };
        int[] expected = { 4, 3, 2, 1 };
        N.reverse(a);
        Assertions.assertArrayEquals(expected, a);

        int[] empty = {};
        N.reverse(empty);
        Assertions.assertArrayEquals(new int[0], empty);

        N.reverse((int[]) null);
    }

    @Test
    public void testReverseIntRange() {
        int[] a = { 1, 2, 3, 4, 5 };
        int[] expected = { 1, 4, 3, 2, 5 };
        N.reverse(a, 1, 4);
        Assertions.assertArrayEquals(expected, a);
    }

    @Test
    public void testReverseLong() {
        long[] a = { 1L, 2L, 3L, 4L };
        long[] expected = { 4L, 3L, 2L, 1L };
        N.reverse(a);
        Assertions.assertArrayEquals(expected, a);

        long[] empty = {};
        N.reverse(empty);
        Assertions.assertArrayEquals(new long[0], empty);

        N.reverse((long[]) null);
    }

    @Test
    public void testReverseLongRange() {
        long[] a = { 1L, 2L, 3L, 4L, 5L };
        long[] expected = { 1L, 4L, 3L, 2L, 5L };
        N.reverse(a, 1, 4);
        Assertions.assertArrayEquals(expected, a);
    }

    @Test
    public void testReverseFloat() {
        float[] a = { 1.0f, 2.0f, 3.0f, 4.0f };
        float[] expected = { 4.0f, 3.0f, 2.0f, 1.0f };
        N.reverse(a);
        Assertions.assertArrayEquals(expected, a, 0.0f);

        float[] empty = {};
        N.reverse(empty);
        Assertions.assertArrayEquals(new float[0], empty, 0.0f);

        N.reverse((float[]) null);
    }

    @Test
    public void testReverseFloatRange() {
        float[] a = { 1.0f, 2.0f, 3.0f, 4.0f, 5.0f };
        float[] expected = { 1.0f, 4.0f, 3.0f, 2.0f, 5.0f };
        N.reverse(a, 1, 4);
        Assertions.assertArrayEquals(expected, a, 0.0f);
    }

    @Test
    public void testReverseDouble() {
        double[] a = { 1.0, 2.0, 3.0, 4.0 };
        double[] expected = { 4.0, 3.0, 2.0, 1.0 };
        N.reverse(a);
        Assertions.assertArrayEquals(expected, a, 0.0);

        double[] empty = {};
        N.reverse(empty);
        Assertions.assertArrayEquals(new double[0], empty, 0.0);

        N.reverse((double[]) null);
    }

    @Test
    public void testReverseDoubleRange() {
        double[] a = { 1.0, 2.0, 3.0, 4.0, 5.0 };
        double[] expected = { 1.0, 4.0, 3.0, 2.0, 5.0 };
        N.reverse(a, 1, 4);
        Assertions.assertArrayEquals(expected, a, 0.0);
    }

    @Test
    public void testReverseObject() {
        String[] a = { "a", "b", "c", "d" };
        String[] expected = { "d", "c", "b", "a" };
        N.reverse(a);
        Assertions.assertArrayEquals(expected, a);

        String[] empty = {};
        N.reverse(empty);
        Assertions.assertArrayEquals(new String[0], empty);

        N.reverse((String[]) null);
    }

    @Test
    public void testReverseObjectRange() {
        String[] a = { "a", "b", "c", "d", "e" };
        String[] expected = { "a", "d", "c", "b", "e" };
        N.reverse(a, 1, 4);
        Assertions.assertArrayEquals(expected, a);
    }

    @Test
    public void testReverseList() {
        List<String> list = new ArrayList<>(Arrays.asList("a", "b", "c", "d"));
        N.reverse(list);
        Assertions.assertEquals(Arrays.asList("d", "c", "b", "a"), list);

        List<String> empty = new ArrayList<>();
        N.reverse(empty);
        Assertions.assertTrue(empty.isEmpty());

        N.reverse((List<?>) null);

        List<String> single = new ArrayList<>(Arrays.asList("a"));
        N.reverse(single);
        Assertions.assertEquals(Arrays.asList("a"), single);
    }

    @Test
    public void testReverseListRange() {
        List<String> list = new ArrayList<>(Arrays.asList("a", "b", "c", "d", "e"));
        N.reverse(list, 1, 4);
        Assertions.assertEquals(Arrays.asList("a", "d", "c", "b", "e"), list);

        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> N.reverse(list, -1, 3));
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> N.reverse(list, 0, 10));

        List<String> list2 = new ArrayList<>(Arrays.asList("a", "b", "c"));
        N.reverse(list2, 1, 1);
        Assertions.assertEquals(Arrays.asList("a", "b", "c"), list2);
    }

    @Test
    public void testReverseCollection() {
        List<String> list = new ArrayList<>(Arrays.asList("a", "b", "c", "d"));
        N.reverse((Collection<?>) list);
        Assertions.assertEquals(Arrays.asList("d", "c", "b", "a"), list);

        Set<String> set = new LinkedHashSet<>(Arrays.asList("a", "b", "c", "d"));
        N.reverse((Collection<?>) set);
        Assertions.assertEquals(4, set.size());

        Collection<String> empty = new ArrayList<>();
        N.reverse(empty);
        Assertions.assertTrue(empty.isEmpty());

        N.reverse((Collection<?>) null);

        Collection<String> single = new ArrayList<>(Arrays.asList("a"));
        N.reverse(single);
        Assertions.assertEquals(1, single.size());
    }

    @Test
    public void testReverseToList() {
        List<String> list = Arrays.asList("a", "b", "c", "d");
        List<String> reversed = N.reverseToList(list);
        Assertions.assertEquals(Arrays.asList("d", "c", "b", "a"), reversed);
        Assertions.assertEquals(Arrays.asList("a", "b", "c", "d"), list);

        List<String> empty = new ArrayList<>();
        List<String> emptyReversed = N.reverseToList(empty);
        Assertions.assertTrue(emptyReversed.isEmpty());

        List<String> nullReversed = N.reverseToList(null);
        Assertions.assertNotNull(nullReversed);
        Assertions.assertTrue(nullReversed.isEmpty());
    }

    @Test
    public void testRotateBoolean() {
        boolean[] a = { true, false, true, false };
        boolean[] expected = { false, true, false, true };
        N.rotate(a, 1);
        Assertions.assertArrayEquals(expected, a);

        boolean[] b = { true, false, true, false };
        boolean[] expected2 = { false, true, false, true };
        N.rotate(b, -1);
        Assertions.assertArrayEquals(expected2, b);

        boolean[] c = { true, false, true, false };
        boolean[] original = c.clone();
        N.rotate(c, 4);
        Assertions.assertArrayEquals(original, c);

        boolean[] empty = {};
        N.rotate(empty, 1);
        Assertions.assertArrayEquals(new boolean[0], empty);

        N.rotate((boolean[]) null, 1);
    }

    @Test
    public void testRotateBooleanRange() {
        boolean[] a = { true, false, true, false, true };
        boolean[] expected = { true, false, true, false, true };
        N.rotate(a, 1, 4, 1);
        expected[1] = false;
        expected[2] = false;
        expected[3] = true;
        Assertions.assertArrayEquals(expected, a);

        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> N.rotate(a, -1, 3, 1));
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> N.rotate(a, 0, 10, 1));

        boolean[] b = { true, false, true };
        boolean[] original = b.clone();
        N.rotate(b, 1, 1, 1);
        Assertions.assertArrayEquals(original, b);
    }

    @Test
    public void testRotateChar() {
        char[] a = { 'a', 'b', 'c', 'd' };
        char[] expected = { 'd', 'a', 'b', 'c' };
        N.rotate(a, 1);
        Assertions.assertArrayEquals(expected, a);

        char[] b = { 'a', 'b', 'c', 'd' };
        char[] expected2 = { 'b', 'c', 'd', 'a' };
        N.rotate(b, -1);
        Assertions.assertArrayEquals(expected2, b);

        char[] c = { 'a', 'b', 'c', 'd' };
        char[] expected3 = { 'b', 'c', 'd', 'a' };
        N.rotate(c, 7);
        Assertions.assertArrayEquals(expected3, c);
    }

    @Test
    public void testRotateCharRange() {
        char[] a = { 'a', 'b', 'c', 'd', 'e' };
        char[] expected = { 'a', 'd', 'b', 'c', 'e' };
        N.rotate(a, 1, 4, 1);
        Assertions.assertArrayEquals(expected, a);
    }

    @Test
    public void testRotateByte() {
        byte[] a = { 1, 2, 3, 4 };
        byte[] expected = { 4, 1, 2, 3 };
        N.rotate(a, 1);
        Assertions.assertArrayEquals(expected, a);
    }

    @Test
    public void testRotateByteRange() {
        byte[] a = { 1, 2, 3, 4, 5 };
        byte[] expected = { 1, 4, 2, 3, 5 };
        N.rotate(a, 1, 4, 1);
        Assertions.assertArrayEquals(expected, a);
    }

    @Test
    public void testRotateShort() {
        short[] a = { 1, 2, 3, 4 };
        short[] expected = { 4, 1, 2, 3 };
        N.rotate(a, 1);
        Assertions.assertArrayEquals(expected, a);
    }

    @Test
    public void testRotateShortRange() {
        short[] a = { 1, 2, 3, 4, 5 };
        short[] expected = { 1, 4, 2, 3, 5 };
        N.rotate(a, 1, 4, 1);
        Assertions.assertArrayEquals(expected, a);
    }

    @Test
    public void testRotateInt() {
        int[] a = { 1, 2, 3, 4, 5 };
        int[] expected = { 4, 5, 1, 2, 3 };
        N.rotate(a, 2);
        Assertions.assertArrayEquals(expected, a);

        int[] b = { 1, 2, 3, 4, 5 };
        int[] expected2 = { 2, 3, 4, 5, 1 };
        N.rotate(b, -1);
        Assertions.assertArrayEquals(expected2, b);

        int[] c = { 1, 2, 3, 4, 5 };
        int[] expected3 = { 4, 5, 1, 2, 3 };
        N.rotate(c, 7);
        Assertions.assertArrayEquals(expected3, c);
    }

    @Test
    public void testRotateIntRange() {

        {
            int[] a = { 1, 2, 3, 4, 5 };
            int[] expected = { 1, 2, 3, 4, 5 };
            N.rotate(a, 0);
            Assertions.assertArrayEquals(expected, a);
        }

        {
            int[] a = { 1, 2, 3, 4, 5 };
            int[] expected = { 5, 1, 2, 3, 4 };
            N.rotate(a, 1);
            Assertions.assertArrayEquals(expected, a);
        }

        {
            int[] a = { 1, 2, 3, 4, 5 };
            int[] expected = { 4, 5, 1, 2, 3 };
            N.rotate(a, 2);
            Assertions.assertArrayEquals(expected, a);
        }

        {
            int[] a = { 1, 2, 3, 4, 5 };
            int[] expected = { 3, 4, 5, 1, 2 };
            N.rotate(a, 3);
            Assertions.assertArrayEquals(expected, a);
        }

        {
            int[] a = { 1, 2, 3, 4, 5 };
            int[] expected = { 2, 3, 4, 5, 1 };
            N.rotate(a, 4);
            Assertions.assertArrayEquals(expected, a);
        }

        {
            int[] a = { 1, 2, 3, 4, 5 };
            int[] expected = { 1, 2, 3, 4, 5 };
            N.rotate(a, 5);
            Assertions.assertArrayEquals(expected, a);
        }

        {
            int[] a = { 1, 2, 3, 4, 5 };
            int[] expected = { 5, 1, 2, 3, 4 };
            N.rotate(a, 6);
            Assertions.assertArrayEquals(expected, a);
        }

        {
            int[] a = { 1, 2, 3, 4, 5 };
            int[] expected = { 2, 3, 4, 5, 1 };
            N.rotate(a, -1);
            Assertions.assertArrayEquals(expected, a);
        }

        {
            int[] a = { 1, 2, 3, 4, 5 };
            int[] expected = { 3, 4, 5, 1, 2 };
            N.rotate(a, -2);
            Assertions.assertArrayEquals(expected, a);
        }

        {
            int[] a = { 1, 2, 3, 4, 5 };
            int[] expected = { 1, 4, 2, 3, 5 };
            N.rotate(a, 1, 4, 1);
            Assertions.assertArrayEquals(expected, a);
        }

        {
            int[] a = { 1, 2, 3, 4, 5 };
            int[] expected = { 1, 3, 4, 2, 5 };
            N.rotate(a, 1, 4, 2);
            Assertions.assertArrayEquals(expected, a);
        }

        {
            int[] a = { 1, 2, 3, 4, 5 };
            int[] expected = { 1, 2, 3, 4, 5 };
            N.rotate(a, 1, 4, 3);
            Assertions.assertArrayEquals(expected, a);
        }

        {
            int[] a = { 1, 2, 3, 4, 5 };
            int[] expected = { 1, 3, 4, 2, 5 };
            N.rotate(a, 1, 4, -1);
            Assertions.assertArrayEquals(expected, a);
        }
    }

    @Test
    public void testRotateLong() {
        long[] a = { 1L, 2L, 3L, 4L };
        long[] expected = { 4L, 1L, 2L, 3L };
        N.rotate(a, 1);
        Assertions.assertArrayEquals(expected, a);
    }

    @Test
    public void testRotateLongRange() {
        long[] a = { 1L, 2L, 3L, 4L, 5L };
        long[] expected = { 1L, 4L, 2L, 3L, 5L };
        N.rotate(a, 1, 4, 1);
        Assertions.assertArrayEquals(expected, a);
    }

    @Test
    public void testRotateFloat() {
        float[] a = { 1.0f, 2.0f, 3.0f, 4.0f };
        float[] expected = { 4.0f, 1.0f, 2.0f, 3.0f };
        N.rotate(a, 1);
        Assertions.assertArrayEquals(expected, a, 0.0f);
    }

    @Test
    public void testRotateFloatRange() {
        float[] a = { 1.0f, 2.0f, 3.0f, 4.0f, 5.0f };
        float[] expected = { 1.0f, 4.0f, 2.0f, 3.0f, 5.0f };
        N.rotate(a, 1, 4, 1);
        Assertions.assertArrayEquals(expected, a, 0.0f);
    }

    @Test
    public void testRotateDouble() {
        double[] a = { 1.0, 2.0, 3.0, 4.0 };
        double[] expected = { 4.0, 1.0, 2.0, 3.0 };
        N.rotate(a, 1);
        Assertions.assertArrayEquals(expected, a, 0.0);
    }

    @Test
    public void testRotateDoubleRange() {
        double[] a = { 1.0, 2.0, 3.0, 4.0, 5.0 };
        double[] expected = { 1.0, 4.0, 2.0, 3.0, 5.0 };
        N.rotate(a, 1, 4, 1);
        Assertions.assertArrayEquals(expected, a, 0.0);
    }

    @Test
    public void testRotateObject() {
        String[] a = { "a", "b", "c", "d" };
        String[] expected = { "d", "a", "b", "c" };
        N.rotate(a, 1);
        Assertions.assertArrayEquals(expected, a);
    }

    @Test
    public void testRotateObjectRange() {
        String[] a = { "a", "b", "c", "d", "e" };
        String[] expected = { "a", "d", "b", "c", "e" };
        N.rotate(a, 1, 4, 1);
        Assertions.assertArrayEquals(expected, a);
    }

    @Test
    public void testRotateList() {
        List<String> list = new ArrayList<>(Arrays.asList("a", "b", "c", "d"));
        N.rotate(list, 1);
        Assertions.assertEquals(Arrays.asList("d", "a", "b", "c"), list);

        N.rotate((List<?>) null, 1);

        List<String> single = new ArrayList<>(Arrays.asList("a"));
        N.rotate(single, 1);
        Assertions.assertEquals(Arrays.asList("a"), single);

        List<String> list2 = new ArrayList<>(Arrays.asList("a", "b", "c", "d"));
        N.rotate(list2, 4);
        Assertions.assertEquals(Arrays.asList("a", "b", "c", "d"), list2);
    }

    @Test
    public void testRotateCollection() {
        List<String> list = new ArrayList<>(Arrays.asList("a", "b", "c", "d"));
        N.rotate((Collection<?>) list, 1);
        Assertions.assertEquals(Arrays.asList("d", "a", "b", "c"), list);

        Set<String> set = new LinkedHashSet<>(Arrays.asList("a", "b", "c", "d"));
        N.rotate((Collection<?>) set, 1);
        Assertions.assertEquals(4, set.size());

        Collection<String> empty = new ArrayList<>();
        N.rotate(empty, 1);
        Assertions.assertTrue(empty.isEmpty());

        N.rotate((Collection<?>) null, 1);

        Collection<String> single = new ArrayList<>(Arrays.asList("a"));
        N.rotate(single, 1);
        Assertions.assertEquals(1, single.size());
    }

    @Test
    public void testShuffleBoolean() {
        boolean[] a = { true, false, true, false, true };
        boolean[] original = a.clone();
        N.shuffle(a);
        Assertions.assertEquals(original.length, a.length);

        boolean[] empty = {};
        N.shuffle(empty);
        Assertions.assertArrayEquals(new boolean[0], empty);

        N.shuffle((boolean[]) null);

        boolean[] single = { true };
        N.shuffle(single);
        Assertions.assertArrayEquals(new boolean[] { true }, single);
    }

    @Test
    public void testShuffleBooleanRange() {
        boolean[] a = { true, false, true, false, true };
        boolean first = a[0];
        boolean last = a[4];
        N.shuffle(a, 1, 4);
        Assertions.assertEquals(first, a[0]);
        Assertions.assertEquals(last, a[4]);

        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> N.shuffle(a, -1, 3));
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> N.shuffle(a, 0, 10));
    }

    @Test
    public void testShuffleBooleanWithRandom() {
        boolean[] a = { true, false, true, false, true };
        Random rnd = new Random(12345);
        N.shuffle(a, rnd);
        Assertions.assertEquals(5, a.length);
    }

    @Test
    public void testShuffleBooleanRangeWithRandom() {
        boolean[] a = { true, false, true, false, true };
        boolean first = a[0];
        boolean last = a[4];
        Random rnd = new Random(12345);
        N.shuffle(a, 1, 4, rnd);
        Assertions.assertEquals(first, a[0]);
        Assertions.assertEquals(last, a[4]);
    }

    @Test
    public void testShuffleChar() {
        char[] a = { 'a', 'b', 'c', 'd', 'e' };
        char[] original = a.clone();
        N.shuffle(a);
        Assertions.assertEquals(original.length, a.length);
        Arrays.sort(original);
        Arrays.sort(a);
        Assertions.assertArrayEquals(original, a);
    }

    @Test
    public void testShuffleCharRange() {
        char[] a = { 'a', 'b', 'c', 'd', 'e' };
        char first = a[0];
        char last = a[4];
        N.shuffle(a, 1, 4);
        Assertions.assertEquals(first, a[0]);
        Assertions.assertEquals(last, a[4]);
    }

    @Test
    public void testShuffleCharWithRandom() {
        char[] a = { 'a', 'b', 'c', 'd', 'e' };
        Random rnd = new Random(12345);
        N.shuffle(a, rnd);
        Assertions.assertEquals(5, a.length);
    }

    @Test
    public void testShuffleCharRangeWithRandom() {
        char[] a = { 'a', 'b', 'c', 'd', 'e' };
        char first = a[0];
        char last = a[4];
        Random rnd = new Random(12345);
        N.shuffle(a, 1, 4, rnd);
        Assertions.assertEquals(first, a[0]);
        Assertions.assertEquals(last, a[4]);
    }

    @Test
    public void testShuffleByte() {
        byte[] a = { 1, 2, 3, 4, 5 };
        byte[] original = a.clone();
        N.shuffle(a);
        Assertions.assertEquals(original.length, a.length);
        Arrays.sort(original);
        Arrays.sort(a);
        Assertions.assertArrayEquals(original, a);
    }

    @Test
    public void testShuffleByteRange() {
        byte[] a = { 1, 2, 3, 4, 5 };
        byte first = a[0];
        byte last = a[4];
        N.shuffle(a, 1, 4);
        Assertions.assertEquals(first, a[0]);
        Assertions.assertEquals(last, a[4]);
    }

    @Test
    public void testShuffleByteWithRandom() {
        byte[] a = { 1, 2, 3, 4, 5 };
        Random rnd = new Random(12345);
        N.shuffle(a, rnd);
        Assertions.assertEquals(5, a.length);
    }

    @Test
    public void testShuffleByteRangeWithRandom() {
        byte[] a = { 1, 2, 3, 4, 5 };
        byte first = a[0];
        byte last = a[4];
        Random rnd = new Random(12345);
        N.shuffle(a, 1, 4, rnd);
        Assertions.assertEquals(first, a[0]);
        Assertions.assertEquals(last, a[4]);
    }

    @Test
    public void testShuffleShort() {
        short[] a = { 1, 2, 3, 4, 5 };
        short[] original = a.clone();
        N.shuffle(a);
        Assertions.assertEquals(original.length, a.length);
        Arrays.sort(original);
        Arrays.sort(a);
        Assertions.assertArrayEquals(original, a);
    }

    @Test
    public void testShuffleShortRange() {
        short[] a = { 1, 2, 3, 4, 5 };
        short first = a[0];
        short last = a[4];
        N.shuffle(a, 1, 4);
        Assertions.assertEquals(first, a[0]);
        Assertions.assertEquals(last, a[4]);
    }

    @Test
    public void testShuffleShortWithRandom() {
        short[] a = { 1, 2, 3, 4, 5 };
        Random rnd = new Random(12345);
        N.shuffle(a, rnd);
        Assertions.assertEquals(5, a.length);
    }

    @Test
    public void testShuffleShortRangeWithRandom() {
        short[] a = { 1, 2, 3, 4, 5 };
        short first = a[0];
        short last = a[4];
        Random rnd = new Random(12345);
        N.shuffle(a, 1, 4, rnd);
        Assertions.assertEquals(first, a[0]);
        Assertions.assertEquals(last, a[4]);
    }

    @Test
    public void testShuffleInt() {
        int[] a = { 1, 2, 3, 4, 5 };
        int[] original = a.clone();
        N.shuffle(a);
        Assertions.assertEquals(original.length, a.length);
        Arrays.sort(original);
        Arrays.sort(a);
        Assertions.assertArrayEquals(original, a);
    }

    @Test
    public void testShuffleIntRange() {
        int[] a = { 1, 2, 3, 4, 5 };
        int first = a[0];
        int last = a[4];
        N.shuffle(a, 1, 4);
        Assertions.assertEquals(first, a[0]);
        Assertions.assertEquals(last, a[4]);
    }

    @Test
    public void testShuffleIntWithRandom() {
        int[] a = { 1, 2, 3, 4, 5 };
        Random rnd = new Random(12345);
        N.shuffle(a, rnd);
        Assertions.assertEquals(5, a.length);
    }

    @Test
    public void testShuffleIntRangeWithRandom() {
        int[] a = { 1, 2, 3, 4, 5 };
        int first = a[0];
        int last = a[4];
        Random rnd = new Random(12345);
        N.shuffle(a, 1, 4, rnd);
        Assertions.assertEquals(first, a[0]);
        Assertions.assertEquals(last, a[4]);
    }

    @Test
    public void testShuffleLong() {
        long[] a = { 1L, 2L, 3L, 4L, 5L };
        long[] original = a.clone();
        N.shuffle(a);
        Assertions.assertEquals(original.length, a.length);
        Arrays.sort(original);
        Arrays.sort(a);
        Assertions.assertArrayEquals(original, a);
    }

    @Test
    public void testShuffleLongRange() {
        long[] a = { 1L, 2L, 3L, 4L, 5L };
        long first = a[0];
        long last = a[4];
        N.shuffle(a, 1, 4);
        Assertions.assertEquals(first, a[0]);
        Assertions.assertEquals(last, a[4]);
    }

    @Test
    public void testShuffleLongWithRandom() {
        long[] a = { 1L, 2L, 3L, 4L, 5L };
        Random rnd = new Random(12345);
        N.shuffle(a, rnd);
        Assertions.assertEquals(5, a.length);
    }

    @Test
    public void testShuffleLongRangeWithRandom() {
        long[] a = { 1L, 2L, 3L, 4L, 5L };
        long first = a[0];
        long last = a[4];
        Random rnd = new Random(12345);
        N.shuffle(a, 1, 4, rnd);
        Assertions.assertEquals(first, a[0]);
        Assertions.assertEquals(last, a[4]);
    }

    @Test
    public void testShuffleFloat() {
        float[] a = { 1.0f, 2.0f, 3.0f, 4.0f, 5.0f };
        float[] original = a.clone();
        N.shuffle(a);
        Assertions.assertEquals(original.length, a.length);
        Arrays.sort(original);
        Arrays.sort(a);
        Assertions.assertArrayEquals(original, a, 0.0f);
    }
}

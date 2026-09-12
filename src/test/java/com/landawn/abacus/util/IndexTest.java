package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Predicate;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.util.u.OptionalInt;

public class IndexTest extends IndexTestSupport {

    @Test
    public void testOfSubArray() {
        final int[] a = { 1, 2, 3, 1, 2, 2, 3, 1, 2, 2, 3 };
        int[] b = { 1, 2, 2, 3 };
        assertEquals(3, Index.ofSubArray(a, 0, b, 0, b.length).orElse(-1));
        assertEquals(7, Index.lastOfSubArray(a, 100, b, 0, b.length).orElse(-1));

        b = new int[] { 1, 2, 2, 2, 3 };
        assertEquals(-1, Index.ofSubArray(a, 0, b, 0, b.length).orElse(-1));
        assertEquals(4, Index.ofSubArray(a, 1, b, 2, 3).orElse(-1));
        assertEquals(-1, Index.ofSubList(CommonUtil.toList(a), 0, CommonUtil.toList(b), 0, b.length).orElse(-1));
        assertEquals(4, Index.ofSubList(CommonUtil.toList(a), 1, CommonUtil.toList(b), 2, 3).orElse(-1));

        assertEquals(-1, Index.lastOfSubArray(a, 0, b, 0, b.length).orElse(-1));
        assertEquals(8, Index.lastOfSubArray(a, a.length - 1, b, 2, 3).orElse(-1));
        assertEquals(8, Index.lastOfSubArray(a, a.length - 2, b, 2, 3).orElse(-1));
        assertEquals(8, Index.lastOfSubArray(a, a.length - 3, b, 2, 3).orElse(-1));
        assertEquals(4, Index.lastOfSubArray(a, a.length - 4, b, 2, 3).orElse(-1));
        assertEquals(8, Index.lastOfSubList(CommonUtil.toList(a), a.length - 1, CommonUtil.toList(b), 2, 3).orElse(-1));
        assertEquals(4, Index.lastOfSubList(CommonUtil.toList(a), a.length - 4, CommonUtil.toList(b), 2, 3).orElse(-1));

        assertEquals(8, Index.last(Strings.join(a, ""), Strings.join(b, 2, 5, ""), a.length - 1).orElse(-1));
        assertEquals(8, Index.last(Strings.join(a, ""), Strings.join(b, 2, 5, ""), a.length - 2).orElse(-1));
        assertEquals(4, Index.last(Strings.join(a, ""), Strings.join(b, 2, 5, ""), a.length - 4).orElse(-1));
    }

    @Test
    public void testOfSubArray_EmptyTarget_FromIndexClamping() {
        final int[] source = { 1, 2, 3 };
        final int[] sub = { 9 };

        assertEquals(OptionalInt.of(0), Index.ofSubArray(new int[0], new int[0]));
        assertEquals(OptionalInt.of(0), Index.lastOfSubArray(new int[0], new int[0]));
        assertEquals(OptionalInt.of(0), Index.ofSubList(Collections.emptyList(), Collections.emptyList()));
        assertEquals(OptionalInt.of(0), Index.lastOfSubList(Collections.emptyList(), Collections.emptyList()));
        assertEquals(OptionalInt.of(0), Index.of("", ""));
        assertEquals(OptionalInt.of(0), Index.last("", ""));
        assertEquals(OptionalInt.of(3), Index.of("abc", "", Integer.MAX_VALUE));
        assertEquals(OptionalInt.of(3), Index.last("abc", "", Integer.MAX_VALUE));
        assertEquals(OptionalInt.of(2), Index.last(source, 3, Integer.MAX_VALUE));
        assertThrows(IndexOutOfBoundsException.class, () -> Index.ofSubArray((int[]) null, 0, sub, 1, 1));

        assertEquals(OptionalInt.of(3), Index.ofSubArray(source, 5, sub, 0, 0));
        assertEquals(OptionalInt.of(3), Index.ofSubArray(source, 3, sub, 0, 0));
        assertEquals(OptionalInt.of(2), Index.ofSubArray(source, 2, sub, 0, 0));
        assertEquals(OptionalInt.of(0), Index.ofSubArray(source, -1, sub, 0, 0));
        assertEquals(OptionalInt.of(0), Index.ofSubArray(source, new int[0]));
        assertFalse(Index.ofSubArray(source, 3, sub, 0, 1).isPresent());
        assertFalse(Index.ofSubArray(source, 5, sub, 0, 1).isPresent());
        assertFalse(Index.ofSubArray((int[]) null, 5, new int[0], 0, 0).isPresent());

        assertEquals(OptionalInt.of(3), Index.lastOfSubArray(source, 5, sub, 0, 0));
        assertEquals(OptionalInt.of(2), Index.lastOfSubArray(source, 2, sub, 0, 0));
        assertEquals(OptionalInt.of(0), Index.lastOfSubArray(source, 0, sub, 0, 0));
        assertFalse(Index.lastOfSubArray(source, -1, sub, 0, 0).isPresent());
        assertEquals(OptionalInt.of(3), Index.lastOfSubArray(source, new int[0]));

        final List<Integer> sourceList = Arrays.asList(1, 2, 3);
        final List<Integer> subList = Arrays.asList(9);
        assertEquals(OptionalInt.of(3), Index.ofSubList(sourceList, 5, subList, 0, 0));
        assertEquals(OptionalInt.of(0), Index.ofSubList(sourceList, -1, subList, 0, 0));
        assertFalse(Index.ofSubList(sourceList, 3, subList, 0, 1).isPresent());
        assertEquals(OptionalInt.of(3), Index.lastOfSubList(sourceList, 5, subList, 0, 0));
        assertFalse(Index.lastOfSubList(sourceList, -1, subList, 0, 0).isPresent());
    }

    @Test
    public void testOfSubList_NonRandomAccess() {
        final List<String> raSource = Arrays.asList("a", "b", "c", "b", "c", "d");
        final List<String> raSub = Arrays.asList("b", "c");
        final List<String> llSource = new LinkedList<>(raSource);
        final List<String> llSub = new LinkedList<>(raSub);

        assertEquals(OptionalInt.of(1), Index.ofSubList(llSource, llSub));
        assertEquals(OptionalInt.of(3), Index.ofSubList(llSource, 2, llSub));
        assertEquals(OptionalInt.of(3), Index.ofSubList(llSource, 2, llSub, 0, 2));
        assertFalse(Index.ofSubList(llSource, 4, llSub, 0, 2).isPresent());

        assertEquals(OptionalInt.of(3), Index.lastOfSubList(llSource, llSub));
        assertEquals(OptionalInt.of(1), Index.lastOfSubList(llSource, 2, llSub));
        assertEquals(OptionalInt.of(1), Index.lastOfSubList(llSource, 2, llSub, 0, 2));
        assertEquals(OptionalInt.of(3), Index.lastOfSubList(llSource, 10, llSub, 0, 2));
        assertFalse(Index.lastOfSubList(llSource, 0, llSub, 0, 2).isPresent());

        assertEquals(Index.ofSubList(raSource, 2, raSub), Index.ofSubList(llSource, 2, llSub));
        assertEquals(Index.lastOfSubList(raSource, 2, raSub), Index.lastOfSubList(llSource, 2, llSub));
    }

    @Test
    public void testOf_NaN() {
        final double[] arr = { 1.0, Double.NaN, 3.0, Double.NaN, 5.0 };
        assertEquals(OptionalInt.of(1), Index.of(arr, Double.NaN));
        assertEquals(OptionalInt.of(3), Index.last(arr, Double.NaN));

        final BitSet result = Index.allOf(arr, Double.NaN);
        assertTrue(result.get(1));
        assertTrue(result.get(3));
        assertFalse(result.get(0));
        assertFalse(result.get(2));
        assertFalse(result.get(4));
    }

    @Test
    public void testOf_EmptyAndNullInputs() {
        assertFalse(Index.of(new boolean[0], true).isPresent());
        assertFalse(Index.of(new char[0], 'a').isPresent());
        assertFalse(Index.of(new byte[0], (byte) 1).isPresent());
        assertFalse(Index.of(new short[0], (short) 1).isPresent());
        assertFalse(Index.of(new int[0], 1).isPresent());
        assertFalse(Index.of(new long[0], 1L).isPresent());
        assertFalse(Index.of(new float[0], 1.0f).isPresent());
        assertFalse(Index.of(new double[0], 1.0d).isPresent());
        assertFalse(Index.of(new Object[0], "x").isPresent());
        assertFalse(Index.of((int[]) null, 1).isPresent());
        assertFalse(Index.last(new int[0], 1).isPresent());
        assertFalse(Index.last((int[]) null, 1).isPresent());
        assertFalse(Index.last(new Object[0], "x").isPresent());
        assertEquals(0, Index.allOf(new int[0], 1).cardinality());
        assertEquals(0, Index.allOf((int[]) null, 1).cardinality());
        assertEquals(0, Index.allOf(new Object[0], "x").cardinality());

        final List<String> emptyList = new ArrayList<>();
        assertFalse(Index.of(emptyList, "a").isPresent());
        assertFalse(Index.last(emptyList, "a").isPresent());
        assertTrue(Index.allOf(emptyList, "a").isEmpty());
    }

    @Test
    public void testOf_FromIndex_OutOfRange() {
        final int[] array = { 1, 2, 3, 4, 5 };
        assertFalse(Index.of(array, 3, 100).isPresent());
        assertEquals(0, Index.allOf(array, 3, 100).cardinality());

        assertEquals(0, Index.of(array, 1, -10).orElse(-1));
        final BitSet bitSet = Index.allOf(array, 1, -10);
        assertEquals(1, bitSet.cardinality());
        assertTrue(bitSet.get(0));
    }

    @Test
    public void testOf_Infinity() {
        final double[] arr = { 1.0, Double.POSITIVE_INFINITY, 3.0, Double.NEGATIVE_INFINITY, 5.0 };
        assertEquals(OptionalInt.of(1), Index.of(arr, Double.POSITIVE_INFINITY));
        assertEquals(OptionalInt.of(3), Index.of(arr, Double.NEGATIVE_INFINITY));
    }

    @Test
    public void testAllOf_MixedNullElements() {
        final Object[] arr = new Object[100];
        for (int i = 0; i < arr.length; i++) {
            arr[i] = (i % 3 == 0) ? null : "value" + (i % 5);
        }
        assertEquals(34, Index.allOf(arr, (Object) null).cardinality());
        assertEquals(13, Index.allOf(arr, "value0").cardinality());
    }

    @Test
    public void testAllOf_LargeArray() {
        final int[] arr = new int[1000];
        Arrays.fill(arr, 42);
        arr[0] = 1;
        arr[500] = 1;
        arr[999] = 1;
        final BitSet result = Index.allOf(arr, 1);
        assertTrue(result.get(0));
        assertTrue(result.get(500));
        assertTrue(result.get(999));
        assertEquals(3, result.cardinality());
    }

    @Test
    public void testOf_ToleranceValidatedBeforeEarlyReturn() {
        assertThrows(IllegalArgumentException.class, () -> Index.of((float[]) null, 1f, 0, -1f));
        assertThrows(IllegalArgumentException.class, () -> Index.last(new float[0], 1f, -1, Float.NaN));
        assertThrows(IllegalArgumentException.class, () -> Index.allOf(new float[0], 1f, 0, -1f));
        assertThrows(IllegalArgumentException.class, () -> Index.of((double[]) null, 1d, 0, Double.NaN));
        assertThrows(IllegalArgumentException.class, () -> Index.last(new double[0], 1d, -1, -1d));
        assertThrows(IllegalArgumentException.class, () -> Index.allOf(new double[0], 1d, 0, Double.NaN));

        // the same fault must be reported the same way whether it surfaces through Index or through N
        assertEquals(assertThrows(IllegalArgumentException.class, () -> N.indexOf((float[]) null, 1f, 0, -1f)).getMessage(),
                assertThrows(IllegalArgumentException.class, () -> Index.of((float[]) null, 1f, 0, -1f)).getMessage());
        assertEquals(assertThrows(IllegalArgumentException.class, () -> N.lastIndexOf(new double[0], 1d, -1, -1d)).getMessage(),
                assertThrows(IllegalArgumentException.class, () -> Index.last(new double[0], 1d, -1, -1d)).getMessage());
        assertEquals(assertThrows(IllegalArgumentException.class, () -> N.indexOf(new float[0], 1f, 0, -1f)).getMessage(),
                assertThrows(IllegalArgumentException.class, () -> Index.allOf(new float[0], 1f, 0, -1f)).getMessage());
        assertEquals(assertThrows(IllegalArgumentException.class, () -> N.indexOf(new double[0], 1d, 0, Double.NaN)).getMessage(),
                assertThrows(IllegalArgumentException.class, () -> Index.allOf(new double[0], 1d, 0, Double.NaN)).getMessage());
    }

    @Test
    public void testLast_NegativeStartIndexFromBack_ElementSearches() {
        final int[] ints = { 1, 2, 3, 2, 1 };
        assertFalse(Index.last(ints, 2, -1).isPresent());
        assertFalse(Index.last(ints, 2, -5).isPresent());
        assertFalse(Index.last(new boolean[] { true }, true, -1).isPresent());
        assertFalse(Index.last(new char[] { 'a' }, 'a', -1).isPresent());
        assertFalse(Index.last(new byte[] { 1 }, (byte) 1, -1).isPresent());
        assertFalse(Index.last(new short[] { 1 }, (short) 1, -1).isPresent());
        assertFalse(Index.last(new long[] { 1L }, 1L, -1).isPresent());
        assertFalse(Index.last(new float[] { 1f }, 1f, -1).isPresent());
        assertFalse(Index.last(new double[] { 1d }, 1d, -1).isPresent());
        assertFalse(Index.last(new String[] { "a" }, "a", -1).isPresent());
        assertFalse(Index.last(Arrays.asList("a"), "a", -1).isPresent());
        assertFalse(Index.last("aaa", 'a', -1).isPresent());
        assertFalse(Index.last("aaa", "a", -1).isPresent());
    }

    @Test
    public void testLast_StartIndexFromBackBeyondEnd() {
        final int[] ints = { 1, 2, 3, 2, 1 };
        assertEquals(3, Index.last(ints, 2, 100).getAsInt());
        assertEquals(3, Index.last(ints, 2, ints.length - 1).getAsInt());
        assertEquals(3, Index.last(ints, 2).getAsInt());
    }

    @Test
    public void testOf_NegativeFromIndexTreatedAsZero() {
        final int[] ints = { 1, 2, 3, 2, 1 };
        assertEquals(1, Index.of(ints, 2, -5).getAsInt());
        assertEquals("{1, 3}", Index.allOf(ints, 2, -5).toString());
        assertEquals(1, Index.of(Arrays.asList("a", "b", "c"), "b", -3).getAsInt());
        assertEquals("{1}", Index.allOf(Arrays.asList("a", "b", "c"), "b", -3).toString());
        assertEquals("{1}", Index.allOf(new LinkedList<>(Arrays.asList("a", "b", "c")), "b", -3).toString());
    }

    @Test
    public void testLastOfSubArray_NegativeStartIndexFromBack() {
        final char[] source = { 'a', 'b', 'c' };
        assertFalse(Index.lastOfSubArray(source, -1, new char[] { 'a' }).isPresent());
        assertFalse(Index.lastOfSubArray(source, -1, new char[0]).isPresent());
        assertFalse(Index.lastOfSubList(Arrays.asList("a", "b"), -1, Arrays.asList("a")).isPresent());
        assertFalse(Index.lastOfSubList(Arrays.asList("a", "b"), -1, Collections.emptyList()).isPresent());
    }

    @Test
    public void testOfSubArray_EmptyPatternMatchesAtLength() {
        final char[] chars = { 'a', 'b', 'c' };
        final String string = "abc";

        assertEquals(string.lastIndexOf(""), Index.lastOfSubArray(chars, new char[0]).getAsInt());
        assertEquals(string.lastIndexOf("", 0), Index.lastOfSubArray(chars, 0, new char[0]).getAsInt());
        assertEquals(string.lastIndexOf("", 9), Index.lastOfSubArray(chars, 9, new char[0]).getAsInt());
        assertEquals(string.indexOf(""), Index.ofSubArray(chars, new char[0]).getAsInt());
        assertEquals(string.indexOf("", 1), Index.ofSubArray(chars, 1, new char[0]).getAsInt());
        assertEquals(string.indexOf("", 9), Index.ofSubArray(chars, 9, new char[0]).getAsInt());
        assertEquals(string.indexOf("", -1), Index.ofSubArray(chars, -1, new char[0]).getAsInt());

        final List<String> list = Arrays.asList("a", "b", "c");
        assertEquals(3, Index.lastOfSubList(list, Collections.emptyList()).getAsInt());
        assertEquals(0, Index.ofSubList(list, Collections.emptyList()).getAsInt());
    }

    @Test
    public void testOfSubArray_MatchesJdkOverRandomInputs() {
        final java.util.Random rnd = new java.util.Random(20260831L);

        for (int it = 0; it < 5000; it++) {
            final int n = rnd.nextInt(8);
            final int m = rnd.nextInt(4);
            final char[] source = new char[n];
            final char[] pattern = new char[m];

            for (int j = 0; j < n; j++) {
                source[j] = (char) ('a' + rnd.nextInt(3));
            }
            for (int j = 0; j < m; j++) {
                pattern[j] = (char) ('a' + rnd.nextInt(3));
            }

            final int from = rnd.nextInt(10) - 2;
            final String s = new String(source);
            final String t = new String(pattern);

            assertEquals(s.indexOf(t, from), Index.ofSubArray(source, from, pattern).orElse(-1), () -> s + " / " + t + " @" + from);
            assertEquals(s.lastIndexOf(t, from), Index.lastOfSubArray(source, from, pattern).orElse(-1), () -> s + " / " + t + " @" + from);
        }
    }

    @Test
    public void testOfSubList_AgreesAcrossRandomAccessAndSequential() {
        final java.util.Random rnd = new java.util.Random(20260831L);

        for (int it = 0; it < 5000; it++) {
            final List<Integer> source = new ArrayList<>();
            final List<Integer> pattern = new ArrayList<>();

            for (int j = 0, n = rnd.nextInt(7); j < n; j++) {
                source.add(rnd.nextInt(3));
            }
            for (int j = 0, m = rnd.nextInt(4); j < m; j++) {
                pattern.add(rnd.nextInt(3));
            }

            final int from = rnd.nextInt(9) - 2;

            assertEquals(Index.ofSubList(source, from, pattern), Index.ofSubList(new LinkedList<>(source), from, new LinkedList<>(pattern)),
                    () -> source + " / " + pattern + " @" + from);
            assertEquals(Index.lastOfSubList(source, from, pattern), Index.lastOfSubList(new LinkedList<>(source), from, new LinkedList<>(pattern)),
                    () -> source + " / " + pattern + " @" + from);
            assertEquals(Collections.indexOfSubList(source, pattern), Index.ofSubList(source, pattern).orElse(-1));
            assertEquals(Collections.lastIndexOfSubList(source, pattern), Index.lastOfSubList(source, pattern).orElse(-1));
        }
    }

    @Test
    public void testAllOf_NullPredicate() {
        assertThrows(IllegalArgumentException.class, () -> Index.allOf(new String[] { "a" }, (Predicate<String>) null));
        assertThrows(IllegalArgumentException.class, () -> Index.allOf(new String[] { "a" }, (Predicate<String>) null, 0));
        assertThrows(IllegalArgumentException.class, () -> Index.allOf(Arrays.asList("a"), (Predicate<String>) null));
        assertThrows(IllegalArgumentException.class, () -> Index.allOf(Arrays.asList("a"), (Predicate<String>) null, 0));
    }

    @Test
    public void testOf_IteratorPastIntegerMaxValue() {
        assertThrows(ArithmeticException.class, () -> Index.of(reviewFixes20260906TargetAfter(Integer.MAX_VALUE + 1L), "target"));
        assertEquals(OptionalInt.of(3), Index.of(reviewFixes20260906TargetAfter(3L), "target"));
        assertFalse(Index.of(Arrays.asList("a", "b").iterator(), "target").isPresent());
    }

    @Test
    public void testLast_EmptySubstring() {
        assertEquals(OptionalInt.of(3), Index.last("abc", ""));
        assertEquals(OptionalInt.of(3), Index.last("abc", "", 10));
        assertEquals(OptionalInt.of(2), Index.last("abc", "", 2));
        assertFalse(Index.last("abc", "", -1).isPresent());
        assertEquals(OptionalInt.of(3), Index.lastOfIgnoreCase("abc", ""));
        assertEquals(OptionalInt.of(3), Index.lastOfIgnoreCase("abc", "", 10));
        assertEquals(OptionalInt.of(0), Index.last("", ""));
        assertEquals("abc".lastIndexOf(""), Index.last("abc", "").orElse(-1));

        assertEquals(OptionalInt.of(2), Index.last("abc", 'c', 10));
        assertEquals(OptionalInt.of(0), Index.last(new String[] { "a" }, "a", 10));

        assertEquals(OptionalInt.of(0), Index.of("abc", ""));
        assertEquals(OptionalInt.of(3), Index.of("abc", "", 10));
        assertEquals(OptionalInt.of(0), Index.ofIgnoreCase("", ""));
        assertEquals(OptionalInt.of(3), Index.ofIgnoreCase("abc", "", 10));
    }

    @Test
    public void testLast_CollectionCopiesWithoutDescendingIterator() {
        final ReviewFixes20260906CountingCollection<String> forLast = new ReviewFixes20260906CountingCollection<>(Arrays.asList("a", "b", "c"));
        assertEquals(OptionalInt.of(1), Index.last(forLast, "b"));
        assertEquals(1, forLast.toArrayCalls);

        final ReviewFixes20260906CountingCollection<String> forOf = new ReviewFixes20260906CountingCollection<>(Arrays.asList("a", "b", "c"));
        assertEquals(OptionalInt.of(1), Index.of(forOf, "b"));
        assertEquals(0, forOf.toArrayCalls);

        final ReviewFixes20260906CountingCollection<String> forAllOf = new ReviewFixes20260906CountingCollection<>(Arrays.asList("a", "b", "c"));
        assertEquals(1, Index.allOf(forAllOf, "b").cardinality());
        assertEquals(0, forAllOf.toArrayCalls);

        final ReviewFixes20260906DescendingCollection<String> descending = new ReviewFixes20260906DescendingCollection<>(Arrays.asList("a", "b", "c"));
        assertEquals(OptionalInt.of(1), Index.last(descending, "b"));
        assertEquals(0, descending.toArrayCalls);
        assertEquals(1, descending.descendingIteratorCalls);

        assertEquals(OptionalInt.of(1), Index.last(new ArrayList<>(Arrays.asList("a", "b", "c")), "b"));
    }

    @Test
    public void ofSubArray_emptyPatternMatchesAtZero() {
        assertEquals(0, Index.ofSubArray(new int[] { 1, 2 }, new int[0]).orElseThrow());
        assertEquals(0, Index.ofSubArray(new int[0], new int[0]).orElseThrow());
        assertEquals(0, Index.ofSubList(List.of("a"), List.of()).orElseThrow());
    }

    @Test
    public void lastOfSubArray_emptyPatternMatchesAtLength() {
        final int[] src = { 1, 2, 3 };
        assertEquals(3, Index.lastOfSubArray(src, new int[0]).orElseThrow());
        assertEquals(3, Index.lastOfSubArray(src, 3, new int[0]).orElseThrow());
        assertEquals(1, Index.lastOfSubArray(src, 1, new int[0]).orElseThrow());
        assertTrue(Index.lastOfSubArray(src, -1, new int[0]).isEmpty());
    }
}

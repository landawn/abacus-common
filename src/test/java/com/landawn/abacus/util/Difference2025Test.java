package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.function.BiPredicate;
import java.util.function.Function;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.Difference.BeanDifference;
import com.landawn.abacus.util.Difference.MapDifference;
import com.landawn.abacus.util.function.TriPredicate;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Tag("2025")
public class Difference2025Test extends TestBase {

    @Test
    public void testOfBooleanArrays() {
        boolean[] a = new boolean[] { true, false, true, false };
        boolean[] b = new boolean[] { false, false, true };

        Difference<BooleanList, BooleanList> diff = Difference.of(a, b);

        assertEquals(BooleanList.of(true, false, false), diff.inCommon());
        assertEquals(BooleanList.of(true), diff.onLeftOnly());
        assertTrue(diff.onRightOnly().isEmpty());
        assertFalse(diff.areEqual());
    }

    @Test
    public void testOfBooleanArraysEmpty() {
        boolean[] a = new boolean[] {};
        boolean[] b = new boolean[] {};

        Difference<BooleanList, BooleanList> diff = Difference.of(a, b);

        assertTrue(diff.inCommon().isEmpty());
        assertTrue(diff.onLeftOnly().isEmpty());
        assertTrue(diff.onRightOnly().isEmpty());
        assertTrue(diff.areEqual());
    }

    @Test
    public void testOfBooleanArraysNull() {
        Difference<BooleanList, BooleanList> diff = Difference.of((boolean[]) null, (boolean[]) null);

        assertTrue(diff.inCommon().isEmpty());
        assertTrue(diff.onLeftOnly().isEmpty());
        assertTrue(diff.onRightOnly().isEmpty());
        assertTrue(diff.areEqual());
    }

    @Test
    public void testOfCharArrays() {
        char[] a = new char[] { 'a', 'b', 'c', 'b' };
        char[] b = new char[] { 'b', 'c', 'd', 'c' };

        Difference<CharList, CharList> diff = Difference.of(a, b);

        assertEquals(CharList.of('b', 'c'), diff.inCommon());
        assertEquals(CharList.of('a', 'b'), diff.onLeftOnly());
        assertEquals(CharList.of('c', 'd'), diff.onRightOnly());
        assertFalse(diff.areEqual());
    }

    @Test
    public void testOfCharArraysEmpty() {
        char[] a = new char[] {};
        char[] b = new char[] {};

        Difference<CharList, CharList> diff = Difference.of(a, b);

        assertTrue(diff.inCommon().isEmpty());
        assertTrue(diff.onLeftOnly().isEmpty());
        assertTrue(diff.onRightOnly().isEmpty());
        assertTrue(diff.areEqual());
    }

    @Test
    public void testOfCharArraysNull() {
        Difference<CharList, CharList> diff = Difference.of((char[]) null, (char[]) null);

        assertTrue(diff.inCommon().isEmpty());
        assertTrue(diff.onLeftOnly().isEmpty());
        assertTrue(diff.onRightOnly().isEmpty());
        assertTrue(diff.areEqual());
    }

    @Test
    public void testOfByteArrays() {
        byte[] a = new byte[] { 1, 2, 3, 2 };
        byte[] b = new byte[] { 2, 3, 4, 3 };

        Difference<ByteList, ByteList> diff = Difference.of(a, b);

        assertEquals(ByteList.of((byte) 2, (byte) 3), diff.inCommon());
        assertEquals(ByteList.of((byte) 1, (byte) 2), diff.onLeftOnly());
        assertEquals(ByteList.of((byte) 3, (byte) 4), diff.onRightOnly());
        assertFalse(diff.areEqual());
    }

    @Test
    public void testOfByteArraysEmpty() {
        byte[] a = new byte[] {};
        byte[] b = new byte[] {};

        Difference<ByteList, ByteList> diff = Difference.of(a, b);

        assertTrue(diff.inCommon().isEmpty());
        assertTrue(diff.onLeftOnly().isEmpty());
        assertTrue(diff.onRightOnly().isEmpty());
        assertTrue(diff.areEqual());
    }

    @Test
    public void testOfByteArraysNull() {
        Difference<ByteList, ByteList> diff = Difference.of((byte[]) null, (byte[]) null);

        assertTrue(diff.inCommon().isEmpty());
        assertTrue(diff.onLeftOnly().isEmpty());
        assertTrue(diff.onRightOnly().isEmpty());
        assertTrue(diff.areEqual());
    }

    @Test
    public void testOfShortArrays() {
        short[] a = new short[] { 1, 2, 3, 2 };
        short[] b = new short[] { 2, 3, 4, 3 };

        Difference<ShortList, ShortList> diff = Difference.of(a, b);

        assertEquals(ShortList.of((short) 2, (short) 3), diff.inCommon());
        assertEquals(ShortList.of((short) 1, (short) 2), diff.onLeftOnly());
        assertEquals(ShortList.of((short) 3, (short) 4), diff.onRightOnly());
        assertFalse(diff.areEqual());
    }

    @Test
    public void testOfShortArraysEmpty() {
        short[] a = new short[] {};
        short[] b = new short[] {};

        Difference<ShortList, ShortList> diff = Difference.of(a, b);

        assertTrue(diff.inCommon().isEmpty());
        assertTrue(diff.onLeftOnly().isEmpty());
        assertTrue(diff.onRightOnly().isEmpty());
        assertTrue(diff.areEqual());
    }

    @Test
    public void testOfShortArraysNull() {
        Difference<ShortList, ShortList> diff = Difference.of((short[]) null, (short[]) null);

        assertTrue(diff.inCommon().isEmpty());
        assertTrue(diff.onLeftOnly().isEmpty());
        assertTrue(diff.onRightOnly().isEmpty());
        assertTrue(diff.areEqual());
    }

    @Test
    public void testOfIntArrays() {
        int[] a = new int[] { 1, 2, 3, 2 };
        int[] b = new int[] { 2, 3, 4, 3 };

        Difference<IntList, IntList> diff = Difference.of(a, b);

        assertEquals(IntList.of(2, 3), diff.inCommon());
        assertEquals(IntList.of(1, 2), diff.onLeftOnly());
        assertEquals(IntList.of(3, 4), diff.onRightOnly());
        assertFalse(diff.areEqual());
    }

    @Test
    public void testOfIntArraysEmpty() {
        int[] a = new int[] {};
        int[] b = new int[] {};

        Difference<IntList, IntList> diff = Difference.of(a, b);

        assertTrue(diff.inCommon().isEmpty());
        assertTrue(diff.onLeftOnly().isEmpty());
        assertTrue(diff.onRightOnly().isEmpty());
        assertTrue(diff.areEqual());
    }

    @Test
    public void testOfIntArraysNull() {
        Difference<IntList, IntList> diff = Difference.of((int[]) null, (int[]) null);

        assertTrue(diff.inCommon().isEmpty());
        assertTrue(diff.onLeftOnly().isEmpty());
        assertTrue(diff.onRightOnly().isEmpty());
        assertTrue(diff.areEqual());
    }

    @Test
    public void testOfLongArrays() {
        long[] a = new long[] { 1L, 2L, 3L, 2L };
        long[] b = new long[] { 2L, 3L, 4L, 3L };

        Difference<LongList, LongList> diff = Difference.of(a, b);

        assertEquals(LongList.of(2L, 3L), diff.inCommon());
        assertEquals(LongList.of(1L, 2L), diff.onLeftOnly());
        assertEquals(LongList.of(3L, 4L), diff.onRightOnly());
        assertFalse(diff.areEqual());
    }

    @Test
    public void testOfLongArraysEmpty() {
        long[] a = new long[] {};
        long[] b = new long[] {};

        Difference<LongList, LongList> diff = Difference.of(a, b);

        assertTrue(diff.inCommon().isEmpty());
        assertTrue(diff.onLeftOnly().isEmpty());
        assertTrue(diff.onRightOnly().isEmpty());
        assertTrue(diff.areEqual());
    }

    @Test
    public void testOfLongArraysNull() {
        Difference<LongList, LongList> diff = Difference.of((long[]) null, (long[]) null);

        assertTrue(diff.inCommon().isEmpty());
        assertTrue(diff.onLeftOnly().isEmpty());
        assertTrue(diff.onRightOnly().isEmpty());
        assertTrue(diff.areEqual());
    }

    @Test
    public void testOfFloatArrays() {
        float[] a = new float[] { 1.0f, 2.0f, 3.0f, 2.0f };
        float[] b = new float[] { 2.0f, 3.0f, 4.0f, 3.0f };

        Difference<FloatList, FloatList> diff = Difference.of(a, b);

        assertEquals(FloatList.of(2.0f, 3.0f), diff.inCommon());
        assertEquals(FloatList.of(1.0f, 2.0f), diff.onLeftOnly());
        assertEquals(FloatList.of(3.0f, 4.0f), diff.onRightOnly());
        assertFalse(diff.areEqual());
    }

    @Test
    public void testOfFloatArraysEmpty() {
        float[] a = new float[] {};
        float[] b = new float[] {};

        Difference<FloatList, FloatList> diff = Difference.of(a, b);

        assertTrue(diff.inCommon().isEmpty());
        assertTrue(diff.onLeftOnly().isEmpty());
        assertTrue(diff.onRightOnly().isEmpty());
        assertTrue(diff.areEqual());
    }

    @Test
    public void testOfFloatArraysNull() {
        Difference<FloatList, FloatList> diff = Difference.of((float[]) null, (float[]) null);

        assertTrue(diff.inCommon().isEmpty());
        assertTrue(diff.onLeftOnly().isEmpty());
        assertTrue(diff.onRightOnly().isEmpty());
        assertTrue(diff.areEqual());
    }

    @Test
    public void testOfDoubleArrays() {
        double[] a = new double[] { 1.0, 2.0, 3.0, 2.0 };
        double[] b = new double[] { 2.0, 3.0, 4.0, 3.0 };

        Difference<DoubleList, DoubleList> diff = Difference.of(a, b);

        assertEquals(DoubleList.of(2.0, 3.0), diff.inCommon());
        assertEquals(DoubleList.of(1.0, 2.0), diff.onLeftOnly());
        assertEquals(DoubleList.of(3.0, 4.0), diff.onRightOnly());
        assertFalse(diff.areEqual());
    }

    @Test
    public void testOfDoubleArraysEmpty() {
        double[] a = new double[] {};
        double[] b = new double[] {};

        Difference<DoubleList, DoubleList> diff = Difference.of(a, b);

        assertTrue(diff.inCommon().isEmpty());
        assertTrue(diff.onLeftOnly().isEmpty());
        assertTrue(diff.onRightOnly().isEmpty());
        assertTrue(diff.areEqual());
    }

    @Test
    public void testOfDoubleArraysNull() {
        Difference<DoubleList, DoubleList> diff = Difference.of((double[]) null, (double[]) null);

        assertTrue(diff.inCommon().isEmpty());
        assertTrue(diff.onLeftOnly().isEmpty());
        assertTrue(diff.onRightOnly().isEmpty());
        assertTrue(diff.areEqual());
    }

    @Test
    public void testOfObjectArrays() {
        String[] a = new String[] { "a", "b", "c", "b" };
        String[] b = new String[] { "b", "c", "d", "c" };

        Difference<List<String>, List<String>> diff = Difference.of(a, b);

        assertEquals(Arrays.asList("b", "c"), diff.inCommon());
        assertEquals(Arrays.asList("a", "b"), diff.onLeftOnly());
        assertEquals(Arrays.asList("c", "d"), diff.onRightOnly());
        assertFalse(diff.areEqual());
    }

    @Test
    public void testOfObjectArraysEmpty() {
        String[] a = new String[] {};
        String[] b = new String[] {};

        Difference<List<String>, List<String>> diff = Difference.of(a, b);

        assertTrue(diff.inCommon().isEmpty());
        assertTrue(diff.onLeftOnly().isEmpty());
        assertTrue(diff.onRightOnly().isEmpty());
        assertTrue(diff.areEqual());
    }

    @Test
    public void testOfObjectArraysWithNull() {
        String[] a = new String[] { "a", null, "b" };
        String[] b = new String[] { null, "b", "c" };

        Difference<List<String>, List<String>> diff = Difference.of(a, b);

        assertEquals(Arrays.asList(null, "b"), diff.inCommon());
        assertEquals(Arrays.asList("a"), diff.onLeftOnly());
        assertEquals(Arrays.asList("c"), diff.onRightOnly());
        assertFalse(diff.areEqual());
    }

    @Test
    public void testOfCollections() {
        List<String> a = Arrays.asList("a", "b", "c", "b");
        List<String> b = Arrays.asList("b", "c", "d", "c");

        Difference<List<String>, List<String>> diff = Difference.of(a, b);

        assertEquals(Arrays.asList("b", "c"), diff.inCommon());
        assertEquals(Arrays.asList("a", "b"), diff.onLeftOnly());
        assertEquals(Arrays.asList("c", "d"), diff.onRightOnly());
        assertFalse(diff.areEqual());
    }

    @Test
    public void testOfCollectionsEmpty() {
        List<String> a = new ArrayList<>();
        List<String> b = new ArrayList<>();

        Difference<List<String>, List<String>> diff = Difference.of(a, b);

        assertTrue(diff.inCommon().isEmpty());
        assertTrue(diff.onLeftOnly().isEmpty());
        assertTrue(diff.onRightOnly().isEmpty());
        assertTrue(diff.areEqual());
    }

    @Test
    public void testOfCollectionsNull() {
        Difference<List<String>, List<String>> diff = Difference.of((Collection<String>) null, (Collection<String>) null);

        assertTrue(diff.inCommon().isEmpty());
        assertTrue(diff.onLeftOnly().isEmpty());
        assertTrue(diff.onRightOnly().isEmpty());
        assertTrue(diff.areEqual());
    }

    @Test
    public void testOfCollectionsOneNull() {
        List<String> a = Arrays.asList("a", "b");

        Difference<List<String>, List<String>> diff1 = Difference.of(a, null);
        assertEquals(Arrays.asList("a", "b"), diff1.onLeftOnly());
        assertTrue(diff1.onRightOnly().isEmpty());
        assertTrue(diff1.inCommon().isEmpty());

        Difference<List<String>, List<String>> diff2 = Difference.of(null, a);
        assertTrue(diff2.onLeftOnly().isEmpty());
        assertEquals(Arrays.asList("a", "b"), diff2.onRightOnly());
        assertTrue(diff2.inCommon().isEmpty());
    }

    @Test
    public void testOfBooleanLists() {
        BooleanList a = BooleanList.of(true, false, true, false);
        BooleanList b = BooleanList.of(false, false, true);

        Difference<BooleanList, BooleanList> diff = Difference.of(a, b);

        assertEquals(BooleanList.of(true, false, false), diff.inCommon());
        assertEquals(BooleanList.of(true), diff.onLeftOnly());
        assertTrue(diff.onRightOnly().isEmpty());
        assertFalse(diff.areEqual());
    }

    @Test
    public void testOfBooleanListsEmpty() {
        BooleanList a = BooleanList.of();
        BooleanList b = BooleanList.of();

        Difference<BooleanList, BooleanList> diff = Difference.of(a, b);

        assertTrue(diff.inCommon().isEmpty());
        assertTrue(diff.onLeftOnly().isEmpty());
        assertTrue(diff.onRightOnly().isEmpty());
        assertTrue(diff.areEqual());
    }

    @Test
    public void testOfBooleanListsNull() {
        Difference<BooleanList, BooleanList> diff = Difference.of((BooleanList) null, (BooleanList) null);

        assertTrue(diff.inCommon().isEmpty());
        assertTrue(diff.onLeftOnly().isEmpty());
        assertTrue(diff.onRightOnly().isEmpty());
        assertTrue(diff.areEqual());
    }

    @Test
    public void testOfCharLists() {
        CharList a = CharList.of('a', 'b', 'c', 'b');
        CharList b = CharList.of('b', 'c', 'd', 'c');

        Difference<CharList, CharList> diff = Difference.of(a, b);

        assertEquals(CharList.of('b', 'c'), diff.inCommon());
        assertEquals(CharList.of('a', 'b'), diff.onLeftOnly());
        assertEquals(CharList.of('c', 'd'), diff.onRightOnly());
        assertFalse(diff.areEqual());
    }

    @Test
    public void testOfCharListsEmpty() {
        CharList a = CharList.of();
        CharList b = CharList.of();

        Difference<CharList, CharList> diff = Difference.of(a, b);

        assertTrue(diff.inCommon().isEmpty());
        assertTrue(diff.onLeftOnly().isEmpty());
        assertTrue(diff.onRightOnly().isEmpty());
        assertTrue(diff.areEqual());
    }

    @Test
    public void testOfCharListsNull() {
        Difference<CharList, CharList> diff = Difference.of((CharList) null, (CharList) null);

        assertTrue(diff.inCommon().isEmpty());
        assertTrue(diff.onLeftOnly().isEmpty());
        assertTrue(diff.onRightOnly().isEmpty());
        assertTrue(diff.areEqual());
    }

    @Test
    public void testOfByteLists() {
        ByteList a = ByteList.of((byte) 1, (byte) 2, (byte) 3, (byte) 2);
        ByteList b = ByteList.of((byte) 2, (byte) 3, (byte) 4, (byte) 3);

        Difference<ByteList, ByteList> diff = Difference.of(a, b);

        assertEquals(ByteList.of((byte) 2, (byte) 3), diff.inCommon());
        assertEquals(ByteList.of((byte) 1, (byte) 2), diff.onLeftOnly());
        assertEquals(ByteList.of((byte) 3, (byte) 4), diff.onRightOnly());
        assertFalse(diff.areEqual());
    }

    @Test
    public void testOfByteListsEmpty() {
        ByteList a = ByteList.of();
        ByteList b = ByteList.of();

        Difference<ByteList, ByteList> diff = Difference.of(a, b);

        assertTrue(diff.inCommon().isEmpty());
        assertTrue(diff.onLeftOnly().isEmpty());
        assertTrue(diff.onRightOnly().isEmpty());
        assertTrue(diff.areEqual());
    }

    @Test
    public void testOfByteListsNull() {
        Difference<ByteList, ByteList> diff = Difference.of((ByteList) null, (ByteList) null);

        assertTrue(diff.inCommon().isEmpty());
        assertTrue(diff.onLeftOnly().isEmpty());
        assertTrue(diff.onRightOnly().isEmpty());
        assertTrue(diff.areEqual());
    }

    @Test
    public void testOfShortLists() {
        ShortList a = ShortList.of((short) 1, (short) 2, (short) 3, (short) 2);
        ShortList b = ShortList.of((short) 2, (short) 3, (short) 4, (short) 3);

        Difference<ShortList, ShortList> diff = Difference.of(a, b);

        assertEquals(ShortList.of((short) 2, (short) 3), diff.inCommon());
        assertEquals(ShortList.of((short) 1, (short) 2), diff.onLeftOnly());
        assertEquals(ShortList.of((short) 3, (short) 4), diff.onRightOnly());
        assertFalse(diff.areEqual());
    }

    @Test
    public void testOfShortListsEmpty() {
        ShortList a = ShortList.of();
        ShortList b = ShortList.of();

        Difference<ShortList, ShortList> diff = Difference.of(a, b);

        assertTrue(diff.inCommon().isEmpty());
        assertTrue(diff.onLeftOnly().isEmpty());
        assertTrue(diff.onRightOnly().isEmpty());
        assertTrue(diff.areEqual());
    }

    @Test
    public void testOfShortListsNull() {
        Difference<ShortList, ShortList> diff = Difference.of((ShortList) null, (ShortList) null);

        assertTrue(diff.inCommon().isEmpty());
        assertTrue(diff.onLeftOnly().isEmpty());
        assertTrue(diff.onRightOnly().isEmpty());
        assertTrue(diff.areEqual());
    }

    @Test
    public void testOfIntLists() {
        IntList a = IntList.of(1, 2, 3, 2);
        IntList b = IntList.of(2, 3, 4, 3);

        Difference<IntList, IntList> diff = Difference.of(a, b);

        assertEquals(IntList.of(2, 3), diff.inCommon());
        assertEquals(IntList.of(1, 2), diff.onLeftOnly());
        assertEquals(IntList.of(3, 4), diff.onRightOnly());
        assertFalse(diff.areEqual());
    }

    @Test
    public void testOfIntListsEmpty() {
        IntList a = IntList.of();
        IntList b = IntList.of();

        Difference<IntList, IntList> diff = Difference.of(a, b);

        assertTrue(diff.inCommon().isEmpty());
        assertTrue(diff.onLeftOnly().isEmpty());
        assertTrue(diff.onRightOnly().isEmpty());
        assertTrue(diff.areEqual());
    }

    @Test
    public void testOfIntListsNull() {
        Difference<IntList, IntList> diff = Difference.of((IntList) null, (IntList) null);

        assertTrue(diff.inCommon().isEmpty());
        assertTrue(diff.onLeftOnly().isEmpty());
        assertTrue(diff.onRightOnly().isEmpty());
        assertTrue(diff.areEqual());
    }

    @Test
    public void testOfLongLists() {
        LongList a = LongList.of(1L, 2L, 3L, 2L);
        LongList b = LongList.of(2L, 3L, 4L, 3L);

        Difference<LongList, LongList> diff = Difference.of(a, b);

        assertEquals(LongList.of(2L, 3L), diff.inCommon());
        assertEquals(LongList.of(1L, 2L), diff.onLeftOnly());
        assertEquals(LongList.of(3L, 4L), diff.onRightOnly());
        assertFalse(diff.areEqual());
    }

    @Test
    public void testOfLongListsEmpty() {
        LongList a = LongList.of();
        LongList b = LongList.of();

        Difference<LongList, LongList> diff = Difference.of(a, b);

        assertTrue(diff.inCommon().isEmpty());
        assertTrue(diff.onLeftOnly().isEmpty());
        assertTrue(diff.onRightOnly().isEmpty());
        assertTrue(diff.areEqual());
    }

    @Test
    public void testOfLongListsNull() {
        Difference<LongList, LongList> diff = Difference.of((LongList) null, (LongList) null);

        assertTrue(diff.inCommon().isEmpty());
        assertTrue(diff.onLeftOnly().isEmpty());
        assertTrue(diff.onRightOnly().isEmpty());
        assertTrue(diff.areEqual());
    }

    @Test
    public void testOfFloatLists() {
        FloatList a = FloatList.of(1.0f, 2.0f, 3.0f, 2.0f);
        FloatList b = FloatList.of(2.0f, 3.0f, 4.0f, 3.0f);

        Difference<FloatList, FloatList> diff = Difference.of(a, b);

        assertEquals(FloatList.of(2.0f, 3.0f), diff.inCommon());
        assertEquals(FloatList.of(1.0f, 2.0f), diff.onLeftOnly());
        assertEquals(FloatList.of(3.0f, 4.0f), diff.onRightOnly());
        assertFalse(diff.areEqual());
    }

    @Test
    public void testOfFloatListsEmpty() {
        FloatList a = FloatList.of();
        FloatList b = FloatList.of();

        Difference<FloatList, FloatList> diff = Difference.of(a, b);

        assertTrue(diff.inCommon().isEmpty());
        assertTrue(diff.onLeftOnly().isEmpty());
        assertTrue(diff.onRightOnly().isEmpty());
        assertTrue(diff.areEqual());
    }

    @Test
    public void testOfFloatListsNull() {
        Difference<FloatList, FloatList> diff = Difference.of((FloatList) null, (FloatList) null);

        assertTrue(diff.inCommon().isEmpty());
        assertTrue(diff.onLeftOnly().isEmpty());
        assertTrue(diff.onRightOnly().isEmpty());
        assertTrue(diff.areEqual());
    }

    @Test
    public void testOfDoubleLists() {
        DoubleList a = DoubleList.of(1.0, 2.0, 3.0, 2.0);
        DoubleList b = DoubleList.of(2.0, 3.0, 4.0, 3.0);

        Difference<DoubleList, DoubleList> diff = Difference.of(a, b);

        assertEquals(DoubleList.of(2.0, 3.0), diff.inCommon());
        assertEquals(DoubleList.of(1.0, 2.0), diff.onLeftOnly());
        assertEquals(DoubleList.of(3.0, 4.0), diff.onRightOnly());
        assertFalse(diff.areEqual());
    }

    @Test
    public void testOfDoubleListsEmpty() {
        DoubleList a = DoubleList.of();
        DoubleList b = DoubleList.of();

        Difference<DoubleList, DoubleList> diff = Difference.of(a, b);

        assertTrue(diff.inCommon().isEmpty());
        assertTrue(diff.onLeftOnly().isEmpty());
        assertTrue(diff.onRightOnly().isEmpty());
        assertTrue(diff.areEqual());
    }

    @Test
    public void testOfDoubleListsNull() {
        Difference<DoubleList, DoubleList> diff = Difference.of((DoubleList) null, (DoubleList) null);

        assertTrue(diff.inCommon().isEmpty());
        assertTrue(diff.onLeftOnly().isEmpty());
        assertTrue(diff.onRightOnly().isEmpty());
        assertTrue(diff.areEqual());
    }

    @Test
    public void testInCommon() {
        List<Integer> a = Arrays.asList(1, 2, 3, 2);
        List<Integer> b = Arrays.asList(2, 3, 4);

        Difference<List<Integer>, List<Integer>> diff = Difference.of(a, b);

        assertEquals(Arrays.asList(2, 3), diff.inCommon());
    }

    @Test
    public void testOnLeftOnly() {
        List<Integer> a = Arrays.asList(1, 2, 3, 2);
        List<Integer> b = Arrays.asList(2, 3, 4);

        Difference<List<Integer>, List<Integer>> diff = Difference.of(a, b);

        assertEquals(Arrays.asList(1, 2), diff.onLeftOnly());
    }

    @Test
    public void testOnRightOnly() {
        List<Integer> a = Arrays.asList(1, 2, 3, 2);
        List<Integer> b = Arrays.asList(2, 3, 4);

        Difference<List<Integer>, List<Integer>> diff = Difference.of(a, b);

        assertEquals(Arrays.asList(4), diff.onRightOnly());
    }

    @Test
    public void testAreEqual() {
        List<Integer> a = Arrays.asList(1, 2, 3);
        List<Integer> b = Arrays.asList(1, 2, 3);

        Difference<List<Integer>, List<Integer>> diff = Difference.of(a, b);

        assertTrue(diff.areEqual());
    }

    @Test
    public void testAreEqualDifferentOrder() {
        List<Integer> a = Arrays.asList(1, 2, 3);
        List<Integer> b = Arrays.asList(3, 2, 1);

        Difference<List<Integer>, List<Integer>> diff = Difference.of(a, b);

        assertTrue(diff.areEqual());
    }

    @Test
    public void testAreNotEqual() {
        List<Integer> a = Arrays.asList(1, 2, 3);
        List<Integer> b = Arrays.asList(1, 2, 4);

        Difference<List<Integer>, List<Integer>> diff = Difference.of(a, b);

        assertFalse(diff.areEqual());
    }

    @Test
    public void testEquals() {
        List<Integer> a = Arrays.asList(1, 2, 3);
        List<Integer> b = Arrays.asList(2, 3, 4);

        Difference<List<Integer>, List<Integer>> diff1 = Difference.of(a, b);
        Difference<List<Integer>, List<Integer>> diff2 = Difference.of(a, b);

        assertEquals(diff1, diff2);
    }

    @Test
    public void testEqualsSameObject() {
        List<Integer> a = Arrays.asList(1, 2, 3);
        List<Integer> b = Arrays.asList(2, 3, 4);

        Difference<List<Integer>, List<Integer>> diff = Difference.of(a, b);

        assertEquals(diff, diff);
    }

    @Test
    public void testNotEquals() {
        List<Integer> a1 = Arrays.asList(1, 2, 3);
        List<Integer> b1 = Arrays.asList(2, 3, 4);

        List<Integer> a2 = Arrays.asList(1, 2, 5);
        List<Integer> b2 = Arrays.asList(2, 3, 4);

        Difference<List<Integer>, List<Integer>> diff1 = Difference.of(a1, b1);
        Difference<List<Integer>, List<Integer>> diff2 = Difference.of(a2, b2);

        assertNotEquals(diff1, diff2);
    }

    @Test
    public void testEqualsWithNull() {
        List<Integer> a = Arrays.asList(1, 2, 3);
        List<Integer> b = Arrays.asList(2, 3, 4);

        Difference<List<Integer>, List<Integer>> diff = Difference.of(a, b);

        assertNotEquals(diff, null);
    }

    @Test
    public void testEqualsWithDifferentClass() {
        List<Integer> a = Arrays.asList(1, 2, 3);
        List<Integer> b = Arrays.asList(2, 3, 4);

        Difference<List<Integer>, List<Integer>> diff = Difference.of(a, b);

        assertNotEquals(diff, "not a difference");
    }

    @Test
    public void testHashCode() {
        List<Integer> a = Arrays.asList(1, 2, 3);
        List<Integer> b = Arrays.asList(2, 3, 4);

        Difference<List<Integer>, List<Integer>> diff1 = Difference.of(a, b);
        Difference<List<Integer>, List<Integer>> diff2 = Difference.of(a, b);

        assertEquals(diff1.hashCode(), diff2.hashCode());
    }

    @Test
    public void testToString() {
        List<Integer> a = Arrays.asList(1, 2, 3);
        List<Integer> b = Arrays.asList(2, 3, 4);

        Difference<List<Integer>, List<Integer>> diff = Difference.of(a, b);
        String str = diff.toString();

        assertTrue(str.contains("areEqual="));
        assertTrue(str.contains("inCommon="));
        assertTrue(str.contains("onLeftOnly="));
        assertTrue(str.contains("onRightOnly="));
    }

    @Test
    public void testMapDifferenceOfBasic() {
        Map<String, Integer> map1 = new HashMap<>();
        map1.put("a", 1);
        map1.put("b", 2);
        map1.put("c", 3);

        Map<String, Integer> map2 = new HashMap<>();
        map2.put("b", 2);
        map2.put("c", 4);
        map2.put("d", 5);

        MapDifference<Map<String, Integer>, Map<String, Integer>, Map<String, Pair<Integer, Integer>>> diff = MapDifference.of(map1, map2);

        assertEquals(1, diff.inCommon().size());
        assertEquals(2, diff.inCommon().get("b"));
        assertEquals(1, diff.onLeftOnly().size());
        assertEquals(1, diff.onLeftOnly().get("a"));
        assertEquals(1, diff.onRightOnly().size());
        assertEquals(5, diff.onRightOnly().get("d"));
        assertEquals(1, diff.withDifferentValues().size());
        assertEquals(Pair.of(3, 4), diff.withDifferentValues().get("c"));
        assertFalse(diff.areEqual());
    }

    @Test
    public void testMapDifferenceOfEmptyMaps() {
        Map<String, Integer> map1 = new HashMap<>();
        Map<String, Integer> map2 = new HashMap<>();

        MapDifference<Map<String, Integer>, Map<String, Integer>, Map<String, Pair<Integer, Integer>>> diff = MapDifference.of(map1, map2);

        assertTrue(diff.inCommon().isEmpty());
        assertTrue(diff.onLeftOnly().isEmpty());
        assertTrue(diff.onRightOnly().isEmpty());
        assertTrue(diff.withDifferentValues().isEmpty());
        assertTrue(diff.areEqual());
    }

    @Test
    public void testMapDifferenceOfNullMaps() {
        MapDifference<Map<String, Integer>, Map<String, Integer>, Map<String, Pair<Integer, Integer>>> diff = MapDifference.of((Map<String, Integer>) null,
                (Map<String, Integer>) null);

        assertTrue(diff.inCommon().isEmpty());
        assertTrue(diff.onLeftOnly().isEmpty());
        assertTrue(diff.onRightOnly().isEmpty());
        assertTrue(diff.withDifferentValues().isEmpty());
        assertTrue(diff.areEqual());
    }

    @Test
    public void testMapDifferenceOfWithNullValues() {
        Map<String, Integer> map1 = new HashMap<>();
        map1.put("a", 1);
        map1.put("b", null);

        Map<String, Integer> map2 = new HashMap<>();
        map2.put("a", 2);
        map2.put("b", null);

        MapDifference<Map<String, Integer>, Map<String, Integer>, Map<String, Pair<Integer, Integer>>> diff = MapDifference.of(map1, map2);

        assertEquals(1, diff.inCommon().size());
        assertNull(diff.inCommon().get("b"));
        assertEquals(1, diff.withDifferentValues().size());
        assertEquals(Pair.of(1, 2), diff.withDifferentValues().get("a"));
    }

    @Test
    public void testMapDifferenceOfWithKeysToCompare() {
        Map<String, Integer> map1 = new HashMap<>();
        map1.put("a", 1);
        map1.put("b", 2);
        map1.put("c", 3);

        Map<String, Integer> map2 = new HashMap<>();
        map2.put("a", 1);
        map2.put("b", 5);
        map2.put("d", 4);

        Collection<String> keysToCompare = Arrays.asList("a", "b");

        MapDifference<Map<String, Integer>, Map<String, Integer>, Map<String, Pair<Integer, Integer>>> diff = MapDifference.of(map1, map2, keysToCompare);

        assertEquals(1, diff.inCommon().size());
        assertEquals(1, diff.inCommon().get("a"));
        assertTrue(diff.onLeftOnly().isEmpty());
        assertTrue(diff.onRightOnly().isEmpty());
        assertEquals(1, diff.withDifferentValues().size());
        assertEquals(Pair.of(2, 5), diff.withDifferentValues().get("b"));
    }

    @Test
    public void testMapDifferenceOfWithCustomEquivalence() {
        Map<String, String> map1 = new HashMap<>();
        map1.put("a", "HELLO");
        map1.put("b", "WORLD");

        Map<String, String> map2 = new HashMap<>();
        map2.put("a", "hello");
        map2.put("b", "world");

        BiPredicate<String, String> caseInsensitive = (v1, v2) -> v1 != null && v2 != null && v1.equalsIgnoreCase(v2);

        MapDifference<Map<String, String>, Map<String, String>, Map<String, Pair<String, String>>> diff = MapDifference.of(map1, map2, null,
                (k, v1, v2) -> caseInsensitive.test(v1, v2));

        assertEquals(2, diff.inCommon().size());
        assertTrue(diff.withDifferentValues().isEmpty());
        assertTrue(diff.areEqual());
    }

    @Test
    public void testMapDifferenceOfCollections() {
        Map<String, Integer> map1a = new HashMap<>();
        map1a.put("id", 1);
        map1a.put("value", 100);

        Map<String, Integer> map1b = new HashMap<>();
        map1b.put("id", 2);
        map1b.put("value", 200);

        List<Map<String, Integer>> col1 = Arrays.asList(map1a, map1b);

        Map<String, Integer> map2a = new HashMap<>();
        map2a.put("id", 1);
        map2a.put("value", 100);

        Map<String, Integer> map2c = new HashMap<>();
        map2c.put("id", 3);
        map2c.put("value", 300);

        List<Map<String, Integer>> col2 = Arrays.asList(map2a, map2c);

        MapDifference<List<Map<String, Integer>>, List<Map<String, Integer>>, Map<Integer, MapDifference<Map<String, Integer>, Map<String, Integer>, Map<String, Pair<Integer, Integer>>>>> diff = MapDifference
                .of(col1, col2, m -> m.get("id"));

        assertEquals(1, diff.inCommon().size());
        assertEquals(1, diff.onLeftOnly().size());
        assertEquals(2, diff.onLeftOnly().get(0).get("id"));
        assertEquals(1, diff.onRightOnly().size());
        assertEquals(3, diff.onRightOnly().get(0).get("id"));
        assertTrue(diff.withDifferentValues().isEmpty());
    }

    @Test
    public void testMapDifferenceOfCollectionsWithKeysToCompare() {
        Map<String, Integer> map1a = new HashMap<>();
        map1a.put("id", 1);
        map1a.put("name", 100);
        map1a.put("age", 25);

        List<Map<String, Integer>> col1 = Arrays.asList(map1a);

        Map<String, Integer> map2a = new HashMap<>();
        map2a.put("id", 1);
        map2a.put("name", 100);
        map2a.put("age", 30);

        List<Map<String, Integer>> col2 = Arrays.asList(map2a);

        Collection<String> keysToCompare = Arrays.asList("name", "age");

        MapDifference<List<Map<String, Integer>>, List<Map<String, Integer>>, Map<Integer, MapDifference<Map<String, Integer>, Map<String, Integer>, Map<String, Pair<Integer, Integer>>>>> diff = MapDifference
                .of(col1, col2, keysToCompare, m -> m.get("id"));

        assertTrue(diff.inCommon().isEmpty());
        assertEquals(1, diff.withDifferentValues().size());
        MapDifference<Map<String, Integer>, Map<String, Integer>, Map<String, Pair<Integer, Integer>>> innerDiff = diff.withDifferentValues().get(1);
        assertEquals(1, innerDiff.inCommon().size());
        assertEquals(100, innerDiff.inCommon().get("name"));
        assertEquals(1, innerDiff.withDifferentValues().size());
        assertEquals(Pair.of(25, 30), innerDiff.withDifferentValues().get("age"));
    }

    @Test
    public void testMapDifferenceOfCollectionsWithDifferentIdExtractors() {
        Map<String, Object> map1a = new HashMap<>();
        map1a.put("id", "A");
        map1a.put("value", 100);

        List<Map<String, Object>> col1 = Arrays.asList(map1a);

        Map<String, Object> map2a = new HashMap<>();
        map2a.put("code", "A");
        map2a.put("value", 100);

        List<Map<String, Object>> col2 = Arrays.asList(map2a);

        Function<Map<? extends String, ? extends Object>, String> idExtractor1 = m -> (String) m.get("id");
        Function<Map<? extends String, ? extends Object>, String> idExtractor2 = m -> (String) m.get("code");

        MapDifference<List<Map<String, Object>>, List<Map<String, Object>>, Map<String, MapDifference<Map<String, Object>, Map<String, Object>, Map<String, Pair<Object, Object>>>>> diff = MapDifference
                .of(col1, col2, idExtractor1, idExtractor2);

        assertTrue(diff.inCommon().isEmpty());
        assertEquals(1, diff.withDifferentValues().size());
        MapDifference<Map<String, Object>, Map<String, Object>, Map<String, Pair<Object, Object>>> innerDiff = diff.withDifferentValues().get("A");
        assertEquals(1, innerDiff.inCommon().size());
        assertEquals(100, innerDiff.inCommon().get("value"));
    }

    @Test
    public void testWithDifferentValues() {
        Map<String, Integer> map1 = new HashMap<>();
        map1.put("a", 1);
        map1.put("b", 2);

        Map<String, Integer> map2 = new HashMap<>();
        map2.put("a", 2);
        map2.put("b", 2);

        MapDifference<Map<String, Integer>, Map<String, Integer>, Map<String, Pair<Integer, Integer>>> diff = MapDifference.of(map1, map2);

        assertEquals(1, diff.withDifferentValues().size());
        assertEquals(Pair.of(1, 2), diff.withDifferentValues().get("a"));
    }

    @Test
    public void testKeyValueDifferenceAreEqual() {
        Map<String, Integer> map1 = new HashMap<>();
        map1.put("a", 1);
        map1.put("b", 2);

        Map<String, Integer> map2 = new HashMap<>();
        map2.put("a", 1);
        map2.put("b", 2);

        MapDifference<Map<String, Integer>, Map<String, Integer>, Map<String, Pair<Integer, Integer>>> diff = MapDifference.of(map1, map2);

        assertTrue(diff.areEqual());
    }

    @Test
    public void testKeyValueDifferenceAreNotEqualWithDifferentValues() {
        Map<String, Integer> map1 = new HashMap<>();
        map1.put("a", 1);
        map1.put("b", 2);

        Map<String, Integer> map2 = new HashMap<>();
        map2.put("a", 2);
        map2.put("b", 2);

        MapDifference<Map<String, Integer>, Map<String, Integer>, Map<String, Pair<Integer, Integer>>> diff = MapDifference.of(map1, map2);

        assertFalse(diff.areEqual());
    }

    @Test
    public void testKeyValueDifferenceEquals() {
        Map<String, Integer> map1 = new HashMap<>();
        map1.put("a", 1);
        map1.put("b", 2);

        Map<String, Integer> map2 = new HashMap<>();
        map2.put("a", 2);
        map2.put("b", 2);

        MapDifference<Map<String, Integer>, Map<String, Integer>, Map<String, Pair<Integer, Integer>>> diff1 = MapDifference.of(map1, map2);
        MapDifference<Map<String, Integer>, Map<String, Integer>, Map<String, Pair<Integer, Integer>>> diff2 = MapDifference.of(map1, map2);

        assertEquals(diff1, diff2);
    }

    @Test
    public void testKeyValueDifferenceHashCode() {
        Map<String, Integer> map1 = new HashMap<>();
        map1.put("a", 1);
        map1.put("b", 2);

        Map<String, Integer> map2 = new HashMap<>();
        map2.put("a", 2);
        map2.put("b", 2);

        MapDifference<Map<String, Integer>, Map<String, Integer>, Map<String, Pair<Integer, Integer>>> diff1 = MapDifference.of(map1, map2);
        MapDifference<Map<String, Integer>, Map<String, Integer>, Map<String, Pair<Integer, Integer>>> diff2 = MapDifference.of(map1, map2);

        assertEquals(diff1.hashCode(), diff2.hashCode());
    }

    @Test
    public void testKeyValueDifferenceToString() {
        Map<String, Integer> map1 = new HashMap<>();
        map1.put("a", 1);
        map1.put("b", 2);

        Map<String, Integer> map2 = new HashMap<>();
        map2.put("a", 2);
        map2.put("b", 2);

        MapDifference<Map<String, Integer>, Map<String, Integer>, Map<String, Pair<Integer, Integer>>> diff = MapDifference.of(map1, map2);
        String str = diff.toString();

        assertTrue(str.contains("areEqual="));
        assertTrue(str.contains("inCommon="));
        assertTrue(str.contains("onLeftOnly="));
        assertTrue(str.contains("onRightOnly="));
        assertTrue(str.contains("withDifferentValues="));
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class TestBean {
        private String name;
        private Integer age;
        private String email;
    }

    @Test
    public void testBeanDifferenceOfBasic() {
        TestBean bean1 = new TestBean("John", 30, "john@example.com");
        TestBean bean2 = new TestBean("John", 31, "john@example.com");

        BeanDifference<Map<String, Object>, Map<String, Object>, Map<String, Pair<Object, Object>>> diff = BeanDifference.of(bean1, bean2);

        assertEquals(2, diff.inCommon().size());
        assertEquals("John", diff.inCommon().get("name"));
        assertEquals("john@example.com", diff.inCommon().get("email"));
        assertEquals(1, diff.withDifferentValues().size());
        assertEquals(Pair.of(30, 31), diff.withDifferentValues().get("age"));
    }

    @Test
    public void testBeanDifferenceOfWithPropNames() {
        TestBean bean1 = new TestBean("John", 30, "john@example.com");
        TestBean bean2 = new TestBean("John", 31, "jane@example.com");

        Collection<String> propNames = Arrays.asList("name", "age");

        BeanDifference<Map<String, Object>, Map<String, Object>, Map<String, Pair<Object, Object>>> diff = BeanDifference.of(bean1, bean2, propNames);

        assertEquals(1, diff.inCommon().size());
        assertEquals("John", diff.inCommon().get("name"));
        assertEquals(1, diff.withDifferentValues().size());
        assertEquals(Pair.of(30, 31), diff.withDifferentValues().get("age"));
    }

    @Test
    public void testBeanDifferenceOfWithValueEquivalence() {
        TestBean bean1 = new TestBean("JOHN", 30, "john@example.com");
        TestBean bean2 = new TestBean("john", 30, "john@example.com");

        BiPredicate<Object, Object> caseInsensitive = (v1, v2) -> {
            if (v1 instanceof String && v2 instanceof String) {
                return ((String) v1).equalsIgnoreCase((String) v2);
            }
            return Objects.equals(v1, v2);
        };

        var diff = BeanDifference.of(bean1, bean2, caseInsensitive);

        assertEquals(3, diff.inCommon().size());
        assertTrue(diff.withDifferentValues().isEmpty());
        assertTrue(diff.areEqual());
    }

    @Test
    public void testBeanDifferenceOfWithPropNamesAndValueEquivalence() {
        TestBean bean1 = new TestBean("JOHN", 30, "john@example.com");
        TestBean bean2 = new TestBean("john", 31, "john@example.com");

        Collection<String> propNames = Arrays.asList("name", "age");

        TriPredicate<String, Object, Object> caseInsensitive = (k, v1, v2) -> {
            if (v1 instanceof String && v2 instanceof String) {
                return ((String) v1).equalsIgnoreCase((String) v2);
            }
            return Objects.equals(v1, v2);
        };

        var diff = BeanDifference.of(bean1, bean2, propNames, caseInsensitive);

        assertEquals(1, diff.inCommon().size());
        assertEquals("JOHN", diff.inCommon().get("name"));
        assertEquals(1, diff.withDifferentValues().size());
        assertEquals(Pair.of(30, 31), diff.withDifferentValues().get("age"));
    }

    @Test
    public void testBeanDifferenceOfCollections() {
        TestBean bean1a = new TestBean("Alice", 25, "alice@example.com");
        TestBean bean1b = new TestBean("Bob", 30, "bob@example.com");
        List<TestBean> list1 = Arrays.asList(bean1a, bean1b);

        TestBean bean2a = new TestBean("Alice", 25, "alice@example.com");
        TestBean bean2c = new TestBean("Charlie", 35, "charlie@example.com");
        List<TestBean> list2 = Arrays.asList(bean2a, bean2c);

        var diff = BeanDifference.of(list1, list2, TestBean::getName);

        assertEquals(1, diff.inCommon().size());
        assertEquals("Alice", diff.inCommon().get(0).getName());
        assertEquals(1, diff.onLeftOnly().size());
        assertEquals("Bob", diff.onLeftOnly().get(0).getName());
        assertEquals(1, diff.onRightOnly().size());
        assertEquals("Charlie", diff.onRightOnly().get(0).getName());
        assertTrue(diff.withDifferentValues().isEmpty());
    }

    @Test
    public void testBeanDifferenceOfCollectionsWithPropNames() {
        TestBean bean1a = new TestBean("Alice", 25, "alice@example.com");
        List<TestBean> list1 = Arrays.asList(bean1a);

        TestBean bean2a = new TestBean("Alice", 26, "alice@example.com");
        List<TestBean> list2 = Arrays.asList(bean2a);

        Collection<String> propNames = Arrays.asList("name", "email");

        var diff = BeanDifference.of(list1, list2, propNames, TestBean::getName);

        assertEquals(1, diff.inCommon().size());
        assertEquals("Alice", diff.inCommon().get(0).getName());
        assertTrue(diff.withDifferentValues().isEmpty());
    }

    @Test
    public void testBeanDifferenceOfCollectionsWithDifferentIdExtractors() {
        TestBean bean1a = new TestBean("alice", 25, "alice@example.com");
        List<TestBean> list1 = Arrays.asList(bean1a);

        TestBean bean2a = new TestBean("ALICE", 25, "alice@example.com");
        List<TestBean> list2 = Arrays.asList(bean2a);

        Function<TestBean, String> idExtractor1 = TestBean::getName;
        Function<TestBean, String> idExtractor2 = b -> b.getName().toLowerCase();

        var diff = BeanDifference.of(list1, list2, idExtractor1, idExtractor2);

        assertTrue(diff.inCommon().isEmpty());
        assertEquals(1, diff.withDifferentValues().size());
        BeanDifference<Map<String, Object>, Map<String, Object>, Map<String, Pair<Object, Object>>> innerDiff = diff.withDifferentValues().get("alice");
        assertEquals(2, innerDiff.inCommon().size());
        assertEquals(1, innerDiff.withDifferentValues().size());
    }

    @Test
    public void testBeanDifferenceOfCollectionsWithPropNamesAndDifferentIdExtractors() {
        TestBean bean1a = new TestBean("alice", 25, "alice@example.com");
        List<TestBean> list1 = Arrays.asList(bean1a);

        TestBean bean2a = new TestBean("ALICE", 26, "alice@example.com");
        List<TestBean> list2 = Arrays.asList(bean2a);

        Collection<String> propNames = Arrays.asList("age", "email");
        Function<TestBean, String> idExtractor1 = TestBean::getName;
        Function<TestBean, String> idExtractor2 = b -> b.getName().toLowerCase();

        var diff = BeanDifference.of(list1, list2, propNames, idExtractor1, idExtractor2);

        assertTrue(diff.inCommon().isEmpty());
        assertEquals(1, diff.withDifferentValues().size());
        var innerDiff = diff.withDifferentValues().get("alice");
        assertEquals(1, innerDiff.inCommon().size());
        assertEquals("alice@example.com", innerDiff.inCommon().get("email"));
        assertEquals(1, innerDiff.withDifferentValues().size());
        assertEquals(Pair.of(25, 26), innerDiff.withDifferentValues().get("age"));
    }

    @Test
    public void testBeanDifferenceWithNullProperties() {
        TestBean bean1 = new TestBean("John", null, "john@example.com");
        TestBean bean2 = new TestBean("John", 30, "john@example.com");

        BeanDifference<Map<String, Object>, Map<String, Object>, Map<String, Pair<Object, Object>>> diff = BeanDifference.of(bean1, bean2);

        assertEquals(2, diff.inCommon().size());
        assertEquals(1, diff.withDifferentValues().size());
        assertEquals(Pair.of(null, 30), diff.withDifferentValues().get("age"));
    }

    @Test
    public void testBeanDifferenceEmptyCollections() {
        List<TestBean> list1 = new ArrayList<>();
        List<TestBean> list2 = new ArrayList<>();

        var diff = BeanDifference.of(list1, list2, TestBean::getName);

        assertTrue(diff.inCommon().isEmpty());
        assertTrue(diff.onLeftOnly().isEmpty());
        assertTrue(diff.onRightOnly().isEmpty());
        assertTrue(diff.withDifferentValues().isEmpty());
        assertTrue(diff.areEqual());
    }

    @Test
    public void testDifferenceWithSingleElement() {
        List<Integer> a = Arrays.asList(1);
        List<Integer> b = Arrays.asList(1);

        Difference<List<Integer>, List<Integer>> diff = Difference.of(a, b);

        assertEquals(Arrays.asList(1), diff.inCommon());
        assertTrue(diff.onLeftOnly().isEmpty());
        assertTrue(diff.onRightOnly().isEmpty());
        assertTrue(diff.areEqual());
    }

    @Test
    public void testDifferenceWithDuplicates() {
        List<String> a = Arrays.asList("a", "a", "a", "b");
        List<String> b = Arrays.asList("a", "b", "b");

        Difference<List<String>, List<String>> diff = Difference.of(a, b);

        assertEquals(Arrays.asList("a", "b"), diff.inCommon());
        assertEquals(Arrays.asList("a", "a"), diff.onLeftOnly());
        assertEquals(Arrays.asList("b"), diff.onRightOnly());
    }

    @Test
    public void testMapDifferenceWithLinkedHashMap() {
        Map<String, Integer> map1 = new LinkedHashMap<>();
        map1.put("a", 1);
        map1.put("b", 2);

        Map<String, Integer> map2 = new LinkedHashMap<>();
        map2.put("b", 3);
        map2.put("c", 4);

        MapDifference<Map<String, Integer>, Map<String, Integer>, Map<String, Pair<Integer, Integer>>> diff = MapDifference.of(map1, map2);

        assertTrue(diff.inCommon() instanceof LinkedHashMap);
        assertTrue(diff.onLeftOnly() instanceof LinkedHashMap);
        assertTrue(diff.onRightOnly() instanceof LinkedHashMap);
        assertTrue(diff.withDifferentValues() instanceof LinkedHashMap);
    }

    @Test
    public void testMapDifferenceWithTreeMap() {
        Map<String, Integer> map1 = new TreeMap<>();
        map1.put("a", 1);
        map1.put("b", 2);

        Map<String, Integer> map2 = new TreeMap<>();
        map2.put("b", 3);
        map2.put("c", 4);

        MapDifference<Map<String, Integer>, Map<String, Integer>, Map<String, Pair<Integer, Integer>>> diff = MapDifference.of(map1, map2);

        assertTrue(diff.inCommon() instanceof LinkedHashMap);
        assertTrue(diff.onLeftOnly() instanceof LinkedHashMap);
        assertTrue(diff.onRightOnly() instanceof LinkedHashMap);
        assertTrue(diff.withDifferentValues() instanceof LinkedHashMap);
    }

    @Test
    public void testFloatListWithNaN() {
        FloatList a = FloatList.of(1.0f, Float.NaN, 3.0f);
        FloatList b = FloatList.of(Float.NaN, 3.0f, 4.0f);

        Difference<FloatList, FloatList> diff = Difference.of(a, b);

        assertEquals(FloatList.of(Float.NaN, 3.0f), diff.inCommon());
        assertEquals(FloatList.of(1.0f), diff.onLeftOnly());
        assertEquals(FloatList.of(4.0f), diff.onRightOnly());
    }

    @Test
    public void testDoubleListWithInfinity() {
        DoubleList a = DoubleList.of(Double.NEGATIVE_INFINITY, 0.0, Double.POSITIVE_INFINITY);
        DoubleList b = DoubleList.of(Double.NEGATIVE_INFINITY, 1.0, Double.POSITIVE_INFINITY);

        Difference<DoubleList, DoubleList> diff = Difference.of(a, b);

        assertEquals(DoubleList.of(Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY), diff.inCommon());
        assertEquals(DoubleList.of(0.0), diff.onLeftOnly());
        assertEquals(DoubleList.of(1.0), diff.onRightOnly());
    }

    @Test
    public void testIntListWithExtremeValues() {
        IntList a = IntList.of(Integer.MIN_VALUE, -1, 0, 1, Integer.MAX_VALUE);
        IntList b = IntList.of(Integer.MIN_VALUE, 0, Integer.MAX_VALUE);

        Difference<IntList, IntList> diff = Difference.of(a, b);

        assertEquals(IntList.of(Integer.MIN_VALUE, 0, Integer.MAX_VALUE), diff.inCommon());
        assertEquals(IntList.of(-1, 1), diff.onLeftOnly());
        assertTrue(diff.onRightOnly().isEmpty());
    }

    @Test
    public void testMapDifferenceAllDifferentValues() {
        Map<String, Integer> map1 = new HashMap<>();
        map1.put("a", 1);
        map1.put("b", 2);
        map1.put("c", 3);

        Map<String, Integer> map2 = new HashMap<>();
        map2.put("a", 10);
        map2.put("b", 20);
        map2.put("c", 30);

        MapDifference<Map<String, Integer>, Map<String, Integer>, Map<String, Pair<Integer, Integer>>> diff = MapDifference.of(map1, map2);

        assertTrue(diff.inCommon().isEmpty());
        assertTrue(diff.onLeftOnly().isEmpty());
        assertTrue(diff.onRightOnly().isEmpty());
        assertEquals(3, diff.withDifferentValues().size());
        assertFalse(diff.areEqual());
    }

    @Test
    public void testBeanDifferenceEqualBeans() {
        TestBean bean1 = new TestBean("John", 30, "john@example.com");
        TestBean bean2 = new TestBean("John", 30, "john@example.com");

        BeanDifference<Map<String, Object>, Map<String, Object>, Map<String, Pair<Object, Object>>> diff = BeanDifference.of(bean1, bean2);

        assertEquals(3, diff.inCommon().size());
        assertTrue(diff.withDifferentValues().isEmpty());
        assertTrue(diff.areEqual());
    }

    @Test
    public void testPairLeftAndRight() {
        Map<String, Integer> map1 = new HashMap<>();
        map1.put("a", 1);

        Map<String, Integer> map2 = new HashMap<>();
        map2.put("a", 2);

        MapDifference<Map<String, Integer>, Map<String, Integer>, Map<String, Pair<Integer, Integer>>> diff = MapDifference.of(map1, map2);

        Pair<Integer, Integer> pair = diff.withDifferentValues().get("a");
        assertEquals(Integer.valueOf(1), pair.left());
        assertEquals(Integer.valueOf(2), pair.right());
    }
}

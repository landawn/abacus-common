package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.AbstractTest;
import com.landawn.abacus.util.Difference.BeanDifference;
import com.landawn.abacus.util.Difference.MapDifference;
import com.landawn.abacus.util.function.BiPredicate;

@Tag("old-test")
public class DifferenceTest extends AbstractTest {

    @Test
    public void test_001() {
        Account a = new Account();
        a.setLastUpdateTime(Dates.currentTimestampPlus(1, TimeUnit.DAYS));
        a.setCreatedTime(Dates.currentTimestampPlus(1, TimeUnit.DAYS));
        Account b = new Account();
        b.setLastUpdateTime(Dates.currentTimestamp());
        b.setCreatedTime(Dates.currentTimestamp());
        var diff = BeanDifference.of(a, b);

        println(diff);

        assertFalse(diff.differentValues().containsKey("lastUpdateTime"));

        diff = BeanDifference.of(a, b, CommonUtil.toList("lastUpdateTime", "createdTime"));
        assertTrue(diff.differentValues().containsKey("lastUpdateTime"));
    }

    @Test
    public void test_002() {
        List<Account> listA = Beans.newRandomList(Account.class, 10);
        List<Account> listB = Beans.newRandomList(Account.class, 10);

        listA.get(0).setGUI(listB.get(3).getGUI());
        listA.get(5).setGUI(listB.get(7).getGUI());
        listA.get(7).setGUI(listB.get(1).getGUI());

        listA.set(4, Beans.copy(listB.get(2)));
        listA.set(6, Beans.copy(listB.get(8)));

        BeanDifference<List<Account>, List<Account>, Map<String, BeanDifference<Map<String, Object>, Map<String, Object>, Map<String, Pair<Object, Object>>>>> diff = BeanDifference
                .of(listA, listB, Account::getGUI);

        println(diff);

        println(diff.differentValues());

        for (int i = 0; i < listA.size(); i++) {
            listA.set(i, Beans.copy(listB.get(i)));
        }

        diff = BeanDifference.of(listA, listB, Account::getGUI);

        assertTrue(diff.areEqual());
    }

    @Test
    public void testOfBooleanArrays() {
        boolean[] a = new boolean[] { true, false, true, false };
        boolean[] b = new boolean[] { false, false, true };

        Difference<BooleanList, BooleanList> diff = Difference.of(a, b);

        assertEquals(BooleanList.of(true, false, false), diff.common());
        assertEquals(BooleanList.of(true), diff.onlyOnLeft());
        assertTrue(diff.onlyOnRight().isEmpty());
        assertFalse(diff.areEqual());
    }

    @Test
    public void testOfBooleanArraysEmpty() {
        boolean[] a = new boolean[] {};
        boolean[] b = new boolean[] {};

        Difference<BooleanList, BooleanList> diff = Difference.of(a, b);

        assertTrue(diff.common().isEmpty());
        assertTrue(diff.onlyOnLeft().isEmpty());
        assertTrue(diff.onlyOnRight().isEmpty());
        assertTrue(diff.areEqual());
    }

    @Test
    public void testOfBooleanArraysNull() {
        Difference<BooleanList, BooleanList> diff = Difference.of((boolean[]) null, (boolean[]) null);

        assertTrue(diff.common().isEmpty());
        assertTrue(diff.onlyOnLeft().isEmpty());
        assertTrue(diff.onlyOnRight().isEmpty());
        assertTrue(diff.areEqual());
    }

    @Test
    public void testOfCharArrays() {
        char[] a = new char[] { 'a', 'b', 'c', 'b' };
        char[] b = new char[] { 'b', 'c', 'd', 'c' };

        Difference<CharList, CharList> diff = Difference.of(a, b);

        assertEquals(CharList.of('b', 'c'), diff.common());
        assertEquals(CharList.of('a', 'b'), diff.onlyOnLeft());
        assertEquals(CharList.of('c', 'd'), diff.onlyOnRight());
        assertFalse(diff.areEqual());
    }

    @Test
    public void testOfCharArraysEmpty() {
        char[] a = new char[] {};
        char[] b = new char[] {};

        Difference<CharList, CharList> diff = Difference.of(a, b);

        assertTrue(diff.common().isEmpty());
        assertTrue(diff.onlyOnLeft().isEmpty());
        assertTrue(diff.onlyOnRight().isEmpty());
        assertTrue(diff.areEqual());
    }

    @Test
    public void testOfCharArraysNull() {
        Difference<CharList, CharList> diff = Difference.of((char[]) null, (char[]) null);

        assertTrue(diff.common().isEmpty());
        assertTrue(diff.onlyOnLeft().isEmpty());
        assertTrue(diff.onlyOnRight().isEmpty());
        assertTrue(diff.areEqual());
    }

    @Test
    public void testOfByteArrays() {
        byte[] a = new byte[] { 1, 2, 3, 2 };
        byte[] b = new byte[] { 2, 3, 4, 3 };

        Difference<ByteList, ByteList> diff = Difference.of(a, b);

        assertEquals(ByteList.of((byte) 2, (byte) 3), diff.common());
        assertEquals(ByteList.of((byte) 1, (byte) 2), diff.onlyOnLeft());
        assertEquals(ByteList.of((byte) 3, (byte) 4), diff.onlyOnRight());
        assertFalse(diff.areEqual());
    }

    @Test
    public void testOfByteArraysEmpty() {
        byte[] a = new byte[] {};
        byte[] b = new byte[] {};

        Difference<ByteList, ByteList> diff = Difference.of(a, b);

        assertTrue(diff.common().isEmpty());
        assertTrue(diff.onlyOnLeft().isEmpty());
        assertTrue(diff.onlyOnRight().isEmpty());
        assertTrue(diff.areEqual());
    }

    @Test
    public void testOfByteArraysNull() {
        Difference<ByteList, ByteList> diff = Difference.of((byte[]) null, (byte[]) null);

        assertTrue(diff.common().isEmpty());
        assertTrue(diff.onlyOnLeft().isEmpty());
        assertTrue(diff.onlyOnRight().isEmpty());
        assertTrue(diff.areEqual());
    }

    @Test
    public void testOfShortArrays() {
        short[] a = new short[] { 1, 2, 3, 2 };
        short[] b = new short[] { 2, 3, 4, 3 };

        Difference<ShortList, ShortList> diff = Difference.of(a, b);

        assertEquals(ShortList.of((short) 2, (short) 3), diff.common());
        assertEquals(ShortList.of((short) 1, (short) 2), diff.onlyOnLeft());
        assertEquals(ShortList.of((short) 3, (short) 4), diff.onlyOnRight());
        assertFalse(diff.areEqual());
    }

    @Test
    public void testOfShortArraysEmpty() {
        short[] a = new short[] {};
        short[] b = new short[] {};

        Difference<ShortList, ShortList> diff = Difference.of(a, b);

        assertTrue(diff.common().isEmpty());
        assertTrue(diff.onlyOnLeft().isEmpty());
        assertTrue(diff.onlyOnRight().isEmpty());
        assertTrue(diff.areEqual());
    }

    @Test
    public void testOfShortArraysNull() {
        Difference<ShortList, ShortList> diff = Difference.of((short[]) null, (short[]) null);

        assertTrue(diff.common().isEmpty());
        assertTrue(diff.onlyOnLeft().isEmpty());
        assertTrue(diff.onlyOnRight().isEmpty());
        assertTrue(diff.areEqual());
    }

    @Test
    public void testOfIntArrays() {
        int[] a = new int[] { 1, 2, 3, 2 };
        int[] b = new int[] { 2, 3, 4, 3 };

        Difference<IntList, IntList> diff = Difference.of(a, b);

        assertEquals(IntList.of(2, 3), diff.common());
        assertEquals(IntList.of(1, 2), diff.onlyOnLeft());
        assertEquals(IntList.of(3, 4), diff.onlyOnRight());
        assertFalse(diff.areEqual());
    }

    @Test
    public void testOfIntArraysEmpty() {
        int[] a = new int[] {};
        int[] b = new int[] {};

        Difference<IntList, IntList> diff = Difference.of(a, b);

        assertTrue(diff.common().isEmpty());
        assertTrue(diff.onlyOnLeft().isEmpty());
        assertTrue(diff.onlyOnRight().isEmpty());
        assertTrue(diff.areEqual());
    }

    @Test
    public void testOfIntArraysNull() {
        Difference<IntList, IntList> diff = Difference.of((int[]) null, (int[]) null);

        assertTrue(diff.common().isEmpty());
        assertTrue(diff.onlyOnLeft().isEmpty());
        assertTrue(diff.onlyOnRight().isEmpty());
        assertTrue(diff.areEqual());
    }

    @Test
    public void testOfLongArrays() {
        long[] a = new long[] { 1L, 2L, 3L, 2L };
        long[] b = new long[] { 2L, 3L, 4L, 3L };

        Difference<LongList, LongList> diff = Difference.of(a, b);

        assertEquals(LongList.of(2L, 3L), diff.common());
        assertEquals(LongList.of(1L, 2L), diff.onlyOnLeft());
        assertEquals(LongList.of(3L, 4L), diff.onlyOnRight());
        assertFalse(diff.areEqual());
    }

    @Test
    public void testOfLongArraysEmpty() {
        long[] a = new long[] {};
        long[] b = new long[] {};

        Difference<LongList, LongList> diff = Difference.of(a, b);

        assertTrue(diff.common().isEmpty());
        assertTrue(diff.onlyOnLeft().isEmpty());
        assertTrue(diff.onlyOnRight().isEmpty());
        assertTrue(diff.areEqual());
    }

    @Test
    public void testOfLongArraysNull() {
        Difference<LongList, LongList> diff = Difference.of((long[]) null, (long[]) null);

        assertTrue(diff.common().isEmpty());
        assertTrue(diff.onlyOnLeft().isEmpty());
        assertTrue(diff.onlyOnRight().isEmpty());
        assertTrue(diff.areEqual());
    }

    @Test
    public void testOfFloatArrays() {
        float[] a = new float[] { 1.0f, 2.0f, 3.0f, 2.0f };
        float[] b = new float[] { 2.0f, 3.0f, 4.0f, 3.0f };

        Difference<FloatList, FloatList> diff = Difference.of(a, b);

        assertEquals(FloatList.of(2.0f, 3.0f), diff.common());
        assertEquals(FloatList.of(1.0f, 2.0f), diff.onlyOnLeft());
        assertEquals(FloatList.of(3.0f, 4.0f), diff.onlyOnRight());
        assertFalse(diff.areEqual());
    }

    @Test
    public void testOfFloatArraysEmpty() {
        float[] a = new float[] {};
        float[] b = new float[] {};

        Difference<FloatList, FloatList> diff = Difference.of(a, b);

        assertTrue(diff.common().isEmpty());
        assertTrue(diff.onlyOnLeft().isEmpty());
        assertTrue(diff.onlyOnRight().isEmpty());
        assertTrue(diff.areEqual());
    }

    @Test
    public void testOfFloatArraysNull() {
        Difference<FloatList, FloatList> diff = Difference.of((float[]) null, (float[]) null);

        assertTrue(diff.common().isEmpty());
        assertTrue(diff.onlyOnLeft().isEmpty());
        assertTrue(diff.onlyOnRight().isEmpty());
        assertTrue(diff.areEqual());
    }

    @Test
    public void testOfDoubleArrays() {
        double[] a = new double[] { 1.0, 2.0, 3.0, 2.0 };
        double[] b = new double[] { 2.0, 3.0, 4.0, 3.0 };

        Difference<DoubleList, DoubleList> diff = Difference.of(a, b);

        assertEquals(DoubleList.of(2.0, 3.0), diff.common());
        assertEquals(DoubleList.of(1.0, 2.0), diff.onlyOnLeft());
        assertEquals(DoubleList.of(3.0, 4.0), diff.onlyOnRight());
        assertFalse(diff.areEqual());
    }

    @Test
    public void testOfDoubleArraysEmpty() {
        double[] a = new double[] {};
        double[] b = new double[] {};

        Difference<DoubleList, DoubleList> diff = Difference.of(a, b);

        assertTrue(diff.common().isEmpty());
        assertTrue(diff.onlyOnLeft().isEmpty());
        assertTrue(diff.onlyOnRight().isEmpty());
        assertTrue(diff.areEqual());
    }

    @Test
    public void testOfDoubleArraysNull() {
        Difference<DoubleList, DoubleList> diff = Difference.of((double[]) null, (double[]) null);

        assertTrue(diff.common().isEmpty());
        assertTrue(diff.onlyOnLeft().isEmpty());
        assertTrue(diff.onlyOnRight().isEmpty());
        assertTrue(diff.areEqual());
    }

    @Test
    public void testOfObjectArrays() {
        String[] a = new String[] { "a", "b", "c", "b" };
        String[] b = new String[] { "b", "c", "d", "c" };

        Difference<List<String>, List<String>> diff = Difference.of(a, b);

        assertEquals(Arrays.asList("b", "c"), diff.common());
        assertEquals(Arrays.asList("a", "b"), diff.onlyOnLeft());
        assertEquals(Arrays.asList("c", "d"), diff.onlyOnRight());
        assertFalse(diff.areEqual());
    }

    @Test
    public void testOfObjectArraysEmpty() {
        String[] a = new String[] {};
        String[] b = new String[] {};

        Difference<List<String>, List<String>> diff = Difference.of(a, b);

        assertTrue(diff.common().isEmpty());
        assertTrue(diff.onlyOnLeft().isEmpty());
        assertTrue(diff.onlyOnRight().isEmpty());
        assertTrue(diff.areEqual());
    }

    @Test
    public void testOfObjectArraysWithNull() {
        String[] a = new String[] { "a", null, "b" };
        String[] b = new String[] { null, "b", "c" };

        Difference<List<String>, List<String>> diff = Difference.of(a, b);

        assertEquals(Arrays.asList(null, "b"), diff.common());
        assertEquals(Arrays.asList("a"), diff.onlyOnLeft());
        assertEquals(Arrays.asList("c"), diff.onlyOnRight());
        assertFalse(diff.areEqual());
    }

    @Test
    public void testOfCollections() {
        List<String> a = Arrays.asList("a", "b", "c", "b");
        List<String> b = Arrays.asList("b", "c", "d", "c");

        Difference<List<String>, List<String>> diff = Difference.of(a, b);

        assertEquals(Arrays.asList("b", "c"), diff.common());
        assertEquals(Arrays.asList("a", "b"), diff.onlyOnLeft());
        assertEquals(Arrays.asList("c", "d"), diff.onlyOnRight());
        assertFalse(diff.areEqual());
    }

    @Test
    public void testOfCollectionsEmpty() {
        List<String> a = new ArrayList<>();
        List<String> b = new ArrayList<>();

        Difference<List<String>, List<String>> diff = Difference.of(a, b);

        assertTrue(diff.common().isEmpty());
        assertTrue(diff.onlyOnLeft().isEmpty());
        assertTrue(diff.onlyOnRight().isEmpty());
        assertTrue(diff.areEqual());
    }

    @Test
    public void testOfCollectionsNull() {
        Difference<List<String>, List<String>> diff = Difference.of((Collection<String>) null, (Collection<String>) null);

        assertTrue(diff.common().isEmpty());
        assertTrue(diff.onlyOnLeft().isEmpty());
        assertTrue(diff.onlyOnRight().isEmpty());
        assertTrue(diff.areEqual());
    }

    @Test
    public void testOfCollectionsOneNull() {
        List<String> a = Arrays.asList("a", "b");

        Difference<List<String>, List<String>> diff1 = Difference.of(a, null);
        assertEquals(Arrays.asList("a", "b"), diff1.onlyOnLeft());
        assertTrue(diff1.onlyOnRight().isEmpty());
        assertTrue(diff1.common().isEmpty());

        Difference<List<String>, List<String>> diff2 = Difference.of(null, a);
        assertTrue(diff2.onlyOnLeft().isEmpty());
        assertEquals(Arrays.asList("a", "b"), diff2.onlyOnRight());
        assertTrue(diff2.common().isEmpty());
    }

    @Test
    public void testOfBooleanLists() {
        BooleanList a = BooleanList.of(true, false, true, false);
        BooleanList b = BooleanList.of(false, false, true);

        Difference<BooleanList, BooleanList> diff = Difference.of(a, b);

        assertEquals(BooleanList.of(true, false, false), diff.common());
        assertEquals(BooleanList.of(true), diff.onlyOnLeft());
        assertTrue(diff.onlyOnRight().isEmpty());
        assertFalse(diff.areEqual());
    }

    @Test
    public void testOfBooleanListsEmpty() {
        BooleanList a = BooleanList.of();
        BooleanList b = BooleanList.of();

        Difference<BooleanList, BooleanList> diff = Difference.of(a, b);

        assertTrue(diff.common().isEmpty());
        assertTrue(diff.onlyOnLeft().isEmpty());
        assertTrue(diff.onlyOnRight().isEmpty());
        assertTrue(diff.areEqual());
    }

    @Test
    public void testOfBooleanListsNull() {
        Difference<BooleanList, BooleanList> diff = Difference.of((BooleanList) null, (BooleanList) null);

        assertTrue(diff.common().isEmpty());
        assertTrue(diff.onlyOnLeft().isEmpty());
        assertTrue(diff.onlyOnRight().isEmpty());
        assertTrue(diff.areEqual());
    }

    @Test
    public void testOfCharLists() {
        CharList a = CharList.of('a', 'b', 'c', 'b');
        CharList b = CharList.of('b', 'c', 'd', 'c');

        Difference<CharList, CharList> diff = Difference.of(a, b);

        assertEquals(CharList.of('b', 'c'), diff.common());
        assertEquals(CharList.of('a', 'b'), diff.onlyOnLeft());
        assertEquals(CharList.of('c', 'd'), diff.onlyOnRight());
        assertFalse(diff.areEqual());
    }

    @Test
    public void testOfCharListsEmpty() {
        CharList a = CharList.of();
        CharList b = CharList.of();

        Difference<CharList, CharList> diff = Difference.of(a, b);

        assertTrue(diff.common().isEmpty());
        assertTrue(diff.onlyOnLeft().isEmpty());
        assertTrue(diff.onlyOnRight().isEmpty());
        assertTrue(diff.areEqual());
    }

    @Test
    public void testOfCharListsNull() {
        Difference<CharList, CharList> diff = Difference.of((CharList) null, (CharList) null);

        assertTrue(diff.common().isEmpty());
        assertTrue(diff.onlyOnLeft().isEmpty());
        assertTrue(diff.onlyOnRight().isEmpty());
        assertTrue(diff.areEqual());
    }

    @Test
    public void testOfByteLists() {
        ByteList a = ByteList.of((byte) 1, (byte) 2, (byte) 3, (byte) 2);
        ByteList b = ByteList.of((byte) 2, (byte) 3, (byte) 4, (byte) 3);

        Difference<ByteList, ByteList> diff = Difference.of(a, b);

        assertEquals(ByteList.of((byte) 2, (byte) 3), diff.common());
        assertEquals(ByteList.of((byte) 1, (byte) 2), diff.onlyOnLeft());
        assertEquals(ByteList.of((byte) 3, (byte) 4), diff.onlyOnRight());
        assertFalse(diff.areEqual());
    }

    @Test
    public void testOfByteListsEmpty() {
        ByteList a = ByteList.of();
        ByteList b = ByteList.of();

        Difference<ByteList, ByteList> diff = Difference.of(a, b);

        assertTrue(diff.common().isEmpty());
        assertTrue(diff.onlyOnLeft().isEmpty());
        assertTrue(diff.onlyOnRight().isEmpty());
        assertTrue(diff.areEqual());
    }

    @Test
    public void testOfByteListsNull() {
        Difference<ByteList, ByteList> diff = Difference.of((ByteList) null, (ByteList) null);

        assertTrue(diff.common().isEmpty());
        assertTrue(diff.onlyOnLeft().isEmpty());
        assertTrue(diff.onlyOnRight().isEmpty());
        assertTrue(diff.areEqual());
    }

    @Test
    public void testOfShortLists() {
        ShortList a = ShortList.of((short) 1, (short) 2, (short) 3, (short) 2);
        ShortList b = ShortList.of((short) 2, (short) 3, (short) 4, (short) 3);

        Difference<ShortList, ShortList> diff = Difference.of(a, b);

        assertEquals(ShortList.of((short) 2, (short) 3), diff.common());
        assertEquals(ShortList.of((short) 1, (short) 2), diff.onlyOnLeft());
        assertEquals(ShortList.of((short) 3, (short) 4), diff.onlyOnRight());
        assertFalse(diff.areEqual());
    }

    @Test
    public void testOfShortListsEmpty() {
        ShortList a = ShortList.of();
        ShortList b = ShortList.of();

        Difference<ShortList, ShortList> diff = Difference.of(a, b);

        assertTrue(diff.common().isEmpty());
        assertTrue(diff.onlyOnLeft().isEmpty());
        assertTrue(diff.onlyOnRight().isEmpty());
        assertTrue(diff.areEqual());
    }

    @Test
    public void testOfShortListsNull() {
        Difference<ShortList, ShortList> diff = Difference.of((ShortList) null, (ShortList) null);

        assertTrue(diff.common().isEmpty());
        assertTrue(diff.onlyOnLeft().isEmpty());
        assertTrue(diff.onlyOnRight().isEmpty());
        assertTrue(diff.areEqual());
    }

    @Test
    public void testOfIntLists() {
        IntList a = IntList.of(1, 2, 3, 2);
        IntList b = IntList.of(2, 3, 4, 3);

        Difference<IntList, IntList> diff = Difference.of(a, b);

        assertEquals(IntList.of(2, 3), diff.common());
        assertEquals(IntList.of(1, 2), diff.onlyOnLeft());
        assertEquals(IntList.of(3, 4), diff.onlyOnRight());
        assertFalse(diff.areEqual());
    }

    @Test
    public void testOfIntListsEmpty() {
        IntList a = IntList.of();
        IntList b = IntList.of();

        Difference<IntList, IntList> diff = Difference.of(a, b);

        assertTrue(diff.common().isEmpty());
        assertTrue(diff.onlyOnLeft().isEmpty());
        assertTrue(diff.onlyOnRight().isEmpty());
        assertTrue(diff.areEqual());
    }

    @Test
    public void testOfIntListsNull() {
        Difference<IntList, IntList> diff = Difference.of((IntList) null, (IntList) null);

        assertTrue(diff.common().isEmpty());
        assertTrue(diff.onlyOnLeft().isEmpty());
        assertTrue(diff.onlyOnRight().isEmpty());
        assertTrue(diff.areEqual());
    }

    @Test
    public void testOfLongLists() {
        LongList a = LongList.of(1L, 2L, 3L, 2L);
        LongList b = LongList.of(2L, 3L, 4L, 3L);

        Difference<LongList, LongList> diff = Difference.of(a, b);

        assertEquals(LongList.of(2L, 3L), diff.common());
        assertEquals(LongList.of(1L, 2L), diff.onlyOnLeft());
        assertEquals(LongList.of(3L, 4L), diff.onlyOnRight());
        assertFalse(diff.areEqual());
    }

    @Test
    public void testOfLongListsEmpty() {
        LongList a = LongList.of();
        LongList b = LongList.of();

        Difference<LongList, LongList> diff = Difference.of(a, b);

        assertTrue(diff.common().isEmpty());
        assertTrue(diff.onlyOnLeft().isEmpty());
        assertTrue(diff.onlyOnRight().isEmpty());
        assertTrue(diff.areEqual());
    }

    @Test
    public void testOfLongListsNull() {
        Difference<LongList, LongList> diff = Difference.of((LongList) null, (LongList) null);

        assertTrue(diff.common().isEmpty());
        assertTrue(diff.onlyOnLeft().isEmpty());
        assertTrue(diff.onlyOnRight().isEmpty());
        assertTrue(diff.areEqual());
    }

    @Test
    public void testOfFloatLists() {
        FloatList a = FloatList.of(1.0f, 2.0f, 3.0f, 2.0f);
        FloatList b = FloatList.of(2.0f, 3.0f, 4.0f, 3.0f);

        Difference<FloatList, FloatList> diff = Difference.of(a, b);

        assertEquals(FloatList.of(2.0f, 3.0f), diff.common());
        assertEquals(FloatList.of(1.0f, 2.0f), diff.onlyOnLeft());
        assertEquals(FloatList.of(3.0f, 4.0f), diff.onlyOnRight());
        assertFalse(diff.areEqual());
    }

    @Test
    public void testOfFloatListsEmpty() {
        FloatList a = FloatList.of();
        FloatList b = FloatList.of();

        Difference<FloatList, FloatList> diff = Difference.of(a, b);

        assertTrue(diff.common().isEmpty());
        assertTrue(diff.onlyOnLeft().isEmpty());
        assertTrue(diff.onlyOnRight().isEmpty());
        assertTrue(diff.areEqual());
    }

    @Test
    public void testOfFloatListsNull() {
        Difference<FloatList, FloatList> diff = Difference.of((FloatList) null, (FloatList) null);

        assertTrue(diff.common().isEmpty());
        assertTrue(diff.onlyOnLeft().isEmpty());
        assertTrue(diff.onlyOnRight().isEmpty());
        assertTrue(diff.areEqual());
    }

    @Test
    public void testOfDoubleLists() {
        DoubleList a = DoubleList.of(1.0, 2.0, 3.0, 2.0);
        DoubleList b = DoubleList.of(2.0, 3.0, 4.0, 3.0);

        Difference<DoubleList, DoubleList> diff = Difference.of(a, b);

        assertEquals(DoubleList.of(2.0, 3.0), diff.common());
        assertEquals(DoubleList.of(1.0, 2.0), diff.onlyOnLeft());
        assertEquals(DoubleList.of(3.0, 4.0), diff.onlyOnRight());
        assertFalse(diff.areEqual());
    }

    @Test
    public void testOfDoubleListsEmpty() {
        DoubleList a = DoubleList.of();
        DoubleList b = DoubleList.of();

        Difference<DoubleList, DoubleList> diff = Difference.of(a, b);

        assertTrue(diff.common().isEmpty());
        assertTrue(diff.onlyOnLeft().isEmpty());
        assertTrue(diff.onlyOnRight().isEmpty());
        assertTrue(diff.areEqual());
    }

    @Test
    public void testOfDoubleListsNull() {
        Difference<DoubleList, DoubleList> diff = Difference.of((DoubleList) null, (DoubleList) null);

        assertTrue(diff.common().isEmpty());
        assertTrue(diff.onlyOnLeft().isEmpty());
        assertTrue(diff.onlyOnRight().isEmpty());
        assertTrue(diff.areEqual());
    }

    @Test
    public void testCommon() {
        List<Integer> a = Arrays.asList(1, 2, 3, 2);
        List<Integer> b = Arrays.asList(2, 3, 4);

        Difference<List<Integer>, List<Integer>> diff = Difference.of(a, b);

        assertEquals(Arrays.asList(2, 3), diff.common());
    }

    @Test
    public void testOnlyOnLeft() {
        List<Integer> a = Arrays.asList(1, 2, 3, 2);
        List<Integer> b = Arrays.asList(2, 3, 4);

        Difference<List<Integer>, List<Integer>> diff = Difference.of(a, b);

        assertEquals(Arrays.asList(1, 2), diff.onlyOnLeft());
    }

    @Test
    public void testOnlyOnRight() {
        List<Integer> a = Arrays.asList(1, 2, 3, 2);
        List<Integer> b = Arrays.asList(2, 3, 4);

        Difference<List<Integer>, List<Integer>> diff = Difference.of(a, b);

        assertEquals(Arrays.asList(4), diff.onlyOnRight());
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
        assertTrue(str.contains("common="));
        assertTrue(str.contains("onlyOnLeft="));
        assertTrue(str.contains("onlyOnRight="));
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

        assertEquals(1, diff.common().size());
        assertEquals(2, diff.common().get("b"));
        assertEquals(1, diff.onlyOnLeft().size());
        assertEquals(1, diff.onlyOnLeft().get("a"));
        assertEquals(1, diff.onlyOnRight().size());
        assertEquals(5, diff.onlyOnRight().get("d"));
        assertEquals(1, diff.differentValues().size());
        assertEquals(Pair.of(3, 4), diff.differentValues().get("c"));
        assertFalse(diff.areEqual());
    }

    @Test
    public void testMapDifferenceOfEmptyMaps() {
        Map<String, Integer> map1 = new HashMap<>();
        Map<String, Integer> map2 = new HashMap<>();

        MapDifference<Map<String, Integer>, Map<String, Integer>, Map<String, Pair<Integer, Integer>>> diff = MapDifference.of(map1, map2);

        assertTrue(diff.common().isEmpty());
        assertTrue(diff.onlyOnLeft().isEmpty());
        assertTrue(diff.onlyOnRight().isEmpty());
        assertTrue(diff.differentValues().isEmpty());
        assertTrue(diff.areEqual());
    }

    @Test
    public void testMapDifferenceOfNullMaps() {
        MapDifference<Map<String, Integer>, Map<String, Integer>, Map<String, Pair<Integer, Integer>>> diff = MapDifference.of((Map<String, Integer>) null,
                (Map<String, Integer>) null);

        assertTrue(diff.common().isEmpty());
        assertTrue(diff.onlyOnLeft().isEmpty());
        assertTrue(diff.onlyOnRight().isEmpty());
        assertTrue(diff.differentValues().isEmpty());
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

        assertEquals(1, diff.common().size());
        assertNull(diff.common().get("b"));
        assertEquals(1, diff.differentValues().size());
        assertEquals(Pair.of(1, 2), diff.differentValues().get("a"));
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

        assertEquals(1, diff.common().size());
        assertEquals(1, diff.common().get("a"));
        assertTrue(diff.onlyOnLeft().isEmpty());
        assertTrue(diff.onlyOnRight().isEmpty());
        assertEquals(1, diff.differentValues().size());
        assertEquals(Pair.of(2, 5), diff.differentValues().get("b"));
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

        assertEquals(2, diff.common().size());
        assertTrue(diff.differentValues().isEmpty());
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

        assertEquals(1, diff.common().size());
        assertEquals(1, diff.onlyOnLeft().size());
        assertEquals(2, diff.onlyOnLeft().get(0).get("id"));
        assertEquals(1, diff.onlyOnRight().size());
        assertEquals(3, diff.onlyOnRight().get(0).get("id"));
        assertTrue(diff.differentValues().isEmpty());
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

        assertTrue(diff.common().isEmpty());
        assertEquals(1, diff.differentValues().size());
        MapDifference<Map<String, Integer>, Map<String, Integer>, Map<String, Pair<Integer, Integer>>> innerDiff = diff.differentValues().get(1);
        assertEquals(1, innerDiff.common().size());
        assertEquals(100, innerDiff.common().get("name"));
        assertEquals(1, innerDiff.differentValues().size());
        assertEquals(Pair.of(25, 30), innerDiff.differentValues().get("age"));
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

        assertTrue(diff.common().isEmpty());
        assertEquals(1, diff.differentValues().size());
        MapDifference<Map<String, Object>, Map<String, Object>, Map<String, Pair<Object, Object>>> innerDiff = diff.differentValues().get("A");
        assertEquals(1, innerDiff.common().size());
        assertEquals(100, innerDiff.common().get("value"));
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

        assertEquals(1, diff.differentValues().size());
        assertEquals(Pair.of(1, 2), diff.differentValues().get("a"));
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
        assertTrue(str.contains("common="));
        assertTrue(str.contains("onlyOnLeft="));
        assertTrue(str.contains("onlyOnRight="));
        assertTrue(str.contains("differentValues="));
    }

    @Test
    public void testDifferenceWithSingleElement() {
        List<Integer> a = Arrays.asList(1);
        List<Integer> b = Arrays.asList(1);

        Difference<List<Integer>, List<Integer>> diff = Difference.of(a, b);

        assertEquals(Arrays.asList(1), diff.common());
        assertTrue(diff.onlyOnLeft().isEmpty());
        assertTrue(diff.onlyOnRight().isEmpty());
        assertTrue(diff.areEqual());
    }

    @Test
    public void testDifferenceWithDuplicates() {
        List<String> a = Arrays.asList("a", "a", "a", "b");
        List<String> b = Arrays.asList("a", "b", "b");

        Difference<List<String>, List<String>> diff = Difference.of(a, b);

        assertEquals(Arrays.asList("a", "b"), diff.common());
        assertEquals(Arrays.asList("a", "a"), diff.onlyOnLeft());
        assertEquals(Arrays.asList("b"), diff.onlyOnRight());
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

        assertTrue(diff.common() instanceof LinkedHashMap);
        assertTrue(diff.onlyOnLeft() instanceof LinkedHashMap);
        assertTrue(diff.onlyOnRight() instanceof LinkedHashMap);
        assertTrue(diff.differentValues() instanceof LinkedHashMap);
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

        assertTrue(diff.common() instanceof LinkedHashMap);
        assertTrue(diff.onlyOnLeft() instanceof LinkedHashMap);
        assertTrue(diff.onlyOnRight() instanceof LinkedHashMap);
        assertTrue(diff.differentValues() instanceof LinkedHashMap);
    }

    @Test
    public void testFloatListWithNaN() {
        FloatList a = FloatList.of(1.0f, Float.NaN, 3.0f);
        FloatList b = FloatList.of(Float.NaN, 3.0f, 4.0f);

        Difference<FloatList, FloatList> diff = Difference.of(a, b);

        assertEquals(FloatList.of(Float.NaN, 3.0f), diff.common());
        assertEquals(FloatList.of(1.0f), diff.onlyOnLeft());
        assertEquals(FloatList.of(4.0f), diff.onlyOnRight());
    }

    @Test
    public void testDoubleListWithInfinity() {
        DoubleList a = DoubleList.of(Double.NEGATIVE_INFINITY, 0.0, Double.POSITIVE_INFINITY);
        DoubleList b = DoubleList.of(Double.NEGATIVE_INFINITY, 1.0, Double.POSITIVE_INFINITY);

        Difference<DoubleList, DoubleList> diff = Difference.of(a, b);

        assertEquals(DoubleList.of(Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY), diff.common());
        assertEquals(DoubleList.of(0.0), diff.onlyOnLeft());
        assertEquals(DoubleList.of(1.0), diff.onlyOnRight());
    }

    @Test
    public void testIntListWithExtremeValues() {
        IntList a = IntList.of(Integer.MIN_VALUE, -1, 0, 1, Integer.MAX_VALUE);
        IntList b = IntList.of(Integer.MIN_VALUE, 0, Integer.MAX_VALUE);

        Difference<IntList, IntList> diff = Difference.of(a, b);

        assertEquals(IntList.of(Integer.MIN_VALUE, 0, Integer.MAX_VALUE), diff.common());
        assertEquals(IntList.of(-1, 1), diff.onlyOnLeft());
        assertTrue(diff.onlyOnRight().isEmpty());
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

        assertTrue(diff.common().isEmpty());
        assertTrue(diff.onlyOnLeft().isEmpty());
        assertTrue(diff.onlyOnRight().isEmpty());
        assertEquals(3, diff.differentValues().size());
        assertFalse(diff.areEqual());
    }

    @Test
    public void testPairLeftAndRight() {
        Map<String, Integer> map1 = new HashMap<>();
        map1.put("a", 1);

        Map<String, Integer> map2 = new HashMap<>();
        map2.put("a", 2);

        MapDifference<Map<String, Integer>, Map<String, Integer>, Map<String, Pair<Integer, Integer>>> diff = MapDifference.of(map1, map2);

        Pair<Integer, Integer> pair = diff.differentValues().get("a");
        assertEquals(Integer.valueOf(1), pair.left());
        assertEquals(Integer.valueOf(2), pair.right());
    }

    @Test
    public void testOf_booleanArrays() {
        Difference<BooleanList, BooleanList> diff = Difference.of(new boolean[] { true, false, true }, new boolean[] { true, true, false, false });
        assertEquals(BooleanList.of(true, false, true), diff.common());
        assertTrue(diff.onlyOnLeft().isEmpty());
        assertEquals(BooleanList.of(false), diff.onlyOnRight());
        assertFalse(diff.areEqual());
    }

    @Test
    public void testOf_charArrays() {
        Difference<CharList, CharList> diff = Difference.of(new char[] { 'a', 'b', 'c' }, new char[] { 'b', 'c', 'd' });
        assertEquals(CharList.of('b', 'c'), diff.common());
        assertEquals(CharList.of('a'), diff.onlyOnLeft());
        assertEquals(CharList.of('d'), diff.onlyOnRight());
    }

    @Test
    public void testOf_byteArrays() {
        Difference<ByteList, ByteList> diff = Difference.of(new byte[] { 1, 2, 3 }, new byte[] { 2, 3, 4 });
        assertEquals(ByteList.of((byte) 2, (byte) 3), diff.common());
        assertEquals(ByteList.of((byte) 1), diff.onlyOnLeft());
        assertEquals(ByteList.of((byte) 4), diff.onlyOnRight());
    }

    @Test
    public void testOf_shortArrays() {
        Difference<ShortList, ShortList> diff = Difference.of(new short[] { 1, 2, 3 }, new short[] { 2, 3, 4 });
        assertEquals(ShortList.of((short) 2, (short) 3), diff.common());
        assertEquals(ShortList.of((short) 1), diff.onlyOnLeft());
        assertEquals(ShortList.of((short) 4), diff.onlyOnRight());
    }

    @Test
    public void testOf_intArrays() {
        Difference<IntList, IntList> diff = Difference.of(new int[] { 1, 2, 3 }, new int[] { 2, 3, 4 });
        assertEquals(IntList.of(2, 3), diff.common());
        assertEquals(IntList.of(1), diff.onlyOnLeft());
        assertEquals(IntList.of(4), diff.onlyOnRight());
    }

    @Test
    public void testOf_longArrays() {
        Difference<LongList, LongList> diff = Difference.of(new long[] { 1L, 2L, 3L }, new long[] { 2L, 3L, 4L });
        assertEquals(LongList.of(2L, 3L), diff.common());
        assertEquals(LongList.of(1L), diff.onlyOnLeft());
        assertEquals(LongList.of(4L), diff.onlyOnRight());
    }

    @Test
    public void testOf_floatArrays() {
        Difference<FloatList, FloatList> diff = Difference.of(new float[] { 1.0f, 2.0f, 3.0f }, new float[] { 2.0f, 3.0f, 4.0f });
        assertEquals(FloatList.of(2.0f, 3.0f), diff.common());
        assertEquals(FloatList.of(1.0f), diff.onlyOnLeft());
        assertEquals(FloatList.of(4.0f), diff.onlyOnRight());
    }

    @Test
    public void testOf_doubleArrays() {
        Difference<DoubleList, DoubleList> diff = Difference.of(new double[] { 1.0, 2.0, 3.0 }, new double[] { 2.0, 3.0, 4.0 });
        assertEquals(DoubleList.of(2.0, 3.0), diff.common());
        assertEquals(DoubleList.of(1.0), diff.onlyOnLeft());
        assertEquals(DoubleList.of(4.0), diff.onlyOnRight());
    }

    @Test
    public void testOf_ObjectArrays() {
        Difference<List<String>, List<String>> diff = Difference.of(new String[] { "a", "b", "c" }, new String[] { "b", "c", "d" });
        assertEquals(Arrays.asList("b", "c"), diff.common());
        assertEquals(Collections.singletonList("a"), diff.onlyOnLeft());
        assertEquals(Collections.singletonList("d"), diff.onlyOnRight());
    }

    @Test
    public void testOf_Collections() {
        Collection<String> a = Arrays.asList("a", "b", "c", "c");
        Collection<String> b = Arrays.asList("b", "c", "d");
        Difference<List<String>, List<String>> diff = Difference.of(a, b);

        assertEquals(Arrays.asList("b", "c"), diff.common());
        assertEquals(Arrays.asList("a", "c"), diff.onlyOnLeft());
        assertEquals(Collections.singletonList("d"), diff.onlyOnRight());
    }

    @Test
    public void testOf_Collections_empty() {
        Difference<List<String>, List<String>> diff1 = Difference.of(new ArrayList<>(), new ArrayList<>());
        assertTrue(diff1.common().isEmpty());
        assertTrue(diff1.onlyOnLeft().isEmpty());
        assertTrue(diff1.onlyOnRight().isEmpty());
        assertTrue(diff1.areEqual());

        Difference<List<String>, List<String>> diff2 = Difference.of(Arrays.asList("a"), new ArrayList<>());
        assertTrue(diff2.common().isEmpty());
        assertEquals(Arrays.asList("a"), diff2.onlyOnLeft());
        assertTrue(diff2.onlyOnRight().isEmpty());

        Difference<List<String>, List<String>> diff3 = Difference.of(new ArrayList<>(), Arrays.asList("b"));
        assertTrue(diff3.common().isEmpty());
        assertTrue(diff3.onlyOnLeft().isEmpty());
        assertEquals(Arrays.asList("b"), diff3.onlyOnRight());
    }

    @Test
    public void testDifference_equalsAndHashCode() {
        Difference<List<String>, List<String>> diff1 = Difference.of(Arrays.asList("a", "b"), Arrays.asList("b", "c"));
        Difference<List<String>, List<String>> diff2 = Difference.of(Arrays.asList("a", "b"), Arrays.asList("b", "c"));
        Difference<List<String>, List<String>> diff3 = Difference.of(Arrays.asList("x", "y"), Arrays.asList("y", "z"));

        assertEquals(diff1, diff2);
        assertEquals(diff1.hashCode(), diff2.hashCode());
        assertNotEquals(diff1, diff3);
        assertNotEquals(diff1, null);
        assertNotEquals(diff1, new Object());
    }

    @Test
    public void testDifference_toString() {
        Difference<List<String>, List<String>> diff = Difference.of(Arrays.asList("a", "b"), Arrays.asList("b", "c"));
        String expected = "{areEqual=false, common=[b], onlyOnLeft=[a], onlyOnRight=[c]}";
        assertEquals(expected, diff.toString());
    }

    @Test
    public void testMapDifference_of() {
        Map<String, Integer> map1 = CommonUtil.asMap("a", 1, "b", 2, "c", 3);
        Map<String, Integer> map2 = CommonUtil.asMap("b", 2, "c", 4, "d", 5);

        MapDifference<Map<String, Integer>, Map<String, Integer>, Map<String, Pair<Integer, Integer>>> diff = MapDifference.of(map1, map2);

        assertEquals(Collections.singletonMap("b", 2), diff.common());
        assertEquals(Collections.singletonMap("a", 1), diff.onlyOnLeft());
        assertEquals(Collections.singletonMap("d", 5), diff.onlyOnRight());
        assertEquals(Collections.singletonMap("c", Pair.of(3, 4)), diff.differentValues());
        assertFalse(diff.areEqual());
    }

    @Test
    public void testMapDifference_of_withKeysToCompare() {
        Map<String, Integer> map1 = CommonUtil.asMap("a", 1, "b", 2, "c", 3);
        Map<String, Integer> map2 = CommonUtil.asMap("b", 2, "c", 4, "d", 5);
        Collection<String> keysToCompare = Arrays.asList("b", "c");

        MapDifference<Map<String, Integer>, Map<String, Integer>, Map<String, Pair<Integer, Integer>>> diff = MapDifference.of(map1, map2, keysToCompare);

        assertEquals(Collections.singletonMap("b", 2), diff.common());
        assertTrue(diff.onlyOnLeft().isEmpty());
        assertTrue(diff.onlyOnRight().isEmpty());
        assertEquals(Collections.singletonMap("c", Pair.of(3, 4)), diff.differentValues());
    }

    @Test
    public void testMapDifference_of_withValueEquivalence() {
        Map<String, String> map1 = CommonUtil.asMap("a", "hello", "b", "WORLD");
        Map<String, String> map2 = CommonUtil.asMap("a", "HELLO", "c", "test");

        MapDifference<Map<String, String>, Map<String, String>, Map<String, Pair<String, String>>> diff = MapDifference.of(map1, map2,
                String::equalsIgnoreCase);

        assertEquals(Collections.singletonMap("a", "hello"), diff.common());
        assertEquals(Collections.singletonMap("b", "WORLD"), diff.onlyOnLeft());
        assertEquals(Collections.singletonMap("c", "test"), diff.onlyOnRight());
        assertTrue(diff.differentValues().isEmpty());
    }

    @Test
    public void testMapDifference_of_collectionOfMaps() {
        Map<String, String> mapA1 = CommonUtil.asMap("id", "1", "value", "A");
        Map<String, String> mapA2 = CommonUtil.asMap("id", "2", "value", "B");
        List<Map<String, String>> listA = Arrays.asList(mapA1, mapA2);

        Map<String, String> mapB1 = CommonUtil.asMap("id", "2", "value", "C");
        Map<String, String> mapB2 = CommonUtil.asMap("id", "3", "value", "D");
        List<Map<String, String>> listB = Arrays.asList(mapB1, mapB2);

        MapDifference<List<Map<String, String>>, List<Map<String, String>>, Map<String, MapDifference<Map<String, String>, Map<String, String>, Map<String, Pair<String, String>>>>> diff = MapDifference
                .of(listA, listB, map -> map.get("id"));

        assertEquals(Collections.singletonList(mapA1), diff.onlyOnLeft());
        assertEquals(Collections.singletonList(mapB2), diff.onlyOnRight());
        assertTrue(diff.common().isEmpty());
        assertEquals(1, diff.differentValues().size());
        assertEquals(Pair.of("B", "C"), diff.differentValues().get("2").differentValues().get("value"));
    }

    @Test
    public void testMapDifference_equalsAndHashCode() {
        Map<String, Integer> map1 = CommonUtil.asMap("a", 1, "b", 2);
        Map<String, Integer> map2 = CommonUtil.asMap("b", 3, "c", 4);
        MapDifference<Map<String, Integer>, Map<String, Integer>, Map<String, Pair<Integer, Integer>>> diff1 = MapDifference.of(map1, map2);
        MapDifference<Map<String, Integer>, Map<String, Integer>, Map<String, Pair<Integer, Integer>>> diff2 = MapDifference.of(map1, map2);

        assertEquals(diff1, diff2);
        assertEquals(diff1.hashCode(), diff2.hashCode());
    }

    @Test
    public void testMapDifference_toString() {
        Map<String, Integer> map1 = CommonUtil.asMap("a", 1, "b", 2);
        Map<String, Integer> map2 = CommonUtil.asMap("b", 3, "c", 4);
        MapDifference<Map<String, Integer>, Map<String, Integer>, Map<String, Pair<Integer, Integer>>> diff = MapDifference.of(map1, map2);
        String expected = "{areEqual=false, common={}, onlyOnLeft={a=1}, onlyOnRight={c=4}, differentValues={b=(2, 3)}}";
        assertEquals(expected, diff.toString());
    }

    @Test
    public void testMapDifferenceCollectionsWithDifferentIdExtractors() {
        Map<String, Object> map1a = new HashMap<>();
        map1a.put("id", "A");
        map1a.put("value", 100);
        map1a.put("type", "X");

        Map<String, Object> map1b = new HashMap<>();
        map1b.put("id", "B");
        map1b.put("value", 200);
        map1b.put("type", "Y");

        List<Map<String, Object>> col1 = Arrays.asList(map1a, map1b);

        Map<String, Object> map2a = new HashMap<>();
        map2a.put("code", "A");
        map2a.put("value", 100);
        map2a.put("category", "Z");

        Map<String, Object> map2c = new HashMap<>();
        map2c.put("code", "C");
        map2c.put("value", 300);
        map2c.put("category", "W");

        List<Map<String, Object>> col2 = Arrays.asList(map2a, map2c);

        Function<Map<? extends String, ? extends Object>, String> idExtractor1 = m -> (String) m.get("id");
        Function<Map<? extends String, ? extends Object>, String> idExtractor2 = m -> (String) m.get("code");

        MapDifference<List<Map<String, Object>>, List<Map<String, Object>>, Map<String, MapDifference<Map<String, Object>, Map<String, Object>, Map<String, Pair<Object, Object>>>>> diff = MapDifference
                .of(col1, col2, idExtractor1, idExtractor2);

        assertTrue(diff.common().isEmpty());
        assertEquals(1, diff.onlyOnLeft().size());
        assertEquals("B", diff.onlyOnLeft().get(0).get("id"));

        assertEquals(1, diff.onlyOnRight().size());
        assertEquals("C", diff.onlyOnRight().get(0).get("code"));

        assertEquals(1, diff.differentValues().size());
        assertTrue(diff.differentValues().containsKey("A"));

        MapDifference<Map<String, Object>, Map<String, Object>, Map<String, Pair<Object, Object>>> innerDiff = diff.differentValues().get("A");
        assertEquals(1, innerDiff.common().size());
        assertEquals(100, innerDiff.common().get("value"));
        assertEquals(2, innerDiff.onlyOnLeft().size());
        assertEquals(2, innerDiff.onlyOnRight().size());
    }

    @Test
    public void testMapDifferenceCollectionsWithKeysToCompareAndDifferentExtractors() {
        Map<String, Object> map1a = new HashMap<>();
        map1a.put("id", 1);
        map1a.put("name", "Alice");
        map1a.put("age", 25);
        map1a.put("city", "NYC");

        Map<String, Object> map1b = new HashMap<>();
        map1b.put("id", 2);
        map1b.put("name", "Bob");
        map1b.put("age", 30);
        map1b.put("city", "LA");

        List<Map<String, Object>> col1 = Arrays.asList(map1a, map1b);

        Map<String, Object> map2a = new HashMap<>();
        map2a.put("userId", 1);
        map2a.put("name", "Alice");
        map2a.put("age", 26);
        map2a.put("country", "USA");

        Map<String, Object> map2c = new HashMap<>();
        map2c.put("userId", 3);
        map2c.put("name", "Charlie");
        map2c.put("age", 35);
        map2c.put("country", "Canada");

        List<Map<String, Object>> col2 = Arrays.asList(map2a, map2c);

        Collection<String> keysToCompare = Arrays.asList("name", "age");

        MapDifference<List<Map<String, Object>>, List<Map<String, Object>>, Map<Integer, MapDifference<Map<String, Object>, Map<String, Object>, Map<String, Pair<Object, Object>>>>> diff = MapDifference
                .of(col1, col2, keysToCompare, m -> (Integer) m.get("id"), m -> (Integer) m.get("userId"));

        assertTrue(diff.common().isEmpty());
        assertEquals(1, diff.onlyOnLeft().size());
        assertEquals("Bob", diff.onlyOnLeft().get(0).get("name"));

        assertEquals(1, diff.onlyOnRight().size());
        assertEquals("Charlie", diff.onlyOnRight().get(0).get("name"));

        assertEquals(1, diff.differentValues().size());
        MapDifference<Map<String, Object>, Map<String, Object>, Map<String, Pair<Object, Object>>> innerDiff = diff.differentValues().get(1);
        assertEquals(1, innerDiff.common().size());
        assertEquals("Alice", innerDiff.common().get("name"));
        assertEquals(1, innerDiff.differentValues().size());
        assertEquals(Pair.of(25, 26), innerDiff.differentValues().get("age"));
    }

    @Test
    public void testMapDifferenceEmptyCollections() {
        List<Map<String, Integer>> emptyList1 = new ArrayList<>();
        List<Map<String, Integer>> emptyList2 = new ArrayList<>();

        MapDifference<List<Map<String, Integer>>, List<Map<String, Integer>>, Map<Integer, MapDifference<Map<String, Integer>, Map<String, Integer>, Map<String, Pair<Integer, Integer>>>>> diff = MapDifference
                .of(emptyList1, emptyList2, m -> m.get("id"));

        assertTrue(diff.common().isEmpty());
        assertTrue(diff.onlyOnLeft().isEmpty());
        assertTrue(diff.onlyOnRight().isEmpty());
        assertTrue(diff.differentValues().isEmpty());
        assertTrue(diff.areEqual());
    }

    @Test
    public void testMapDifferenceCollectionsOneEmpty() {
        Map<String, String> map1 = new HashMap<>();
        map1.put("id", "X");
        map1.put("value", "test");
        List<Map<String, String>> list1 = Arrays.asList(map1);
        List<Map<String, String>> emptyList = new ArrayList<>();

        MapDifference<List<Map<String, String>>, List<Map<String, String>>, Map<String, MapDifference<Map<String, String>, Map<String, String>, Map<String, Pair<String, String>>>>> diff1 = MapDifference
                .of(list1, emptyList, m -> m.get("id"));

        assertEquals(1, diff1.onlyOnLeft().size());
        assertTrue(diff1.onlyOnRight().isEmpty());
        assertFalse(diff1.areEqual());

        MapDifference<List<Map<String, String>>, List<Map<String, String>>, Map<String, MapDifference<Map<String, String>, Map<String, String>, Map<String, Pair<String, String>>>>> diff2 = MapDifference
                .of(emptyList, list1, m -> m.get("id"));

        assertTrue(diff2.onlyOnLeft().isEmpty());
        assertEquals(1, diff2.onlyOnRight().size());
        assertFalse(diff2.areEqual());
    }

    @Test
    public void testBeanDifferenceCollectionWithNonBeanClass() {
        List<String> stringList = Arrays.asList("not a bean");
        List<String> anotherList = Arrays.asList("also not a bean");

        assertThrows(IllegalArgumentException.class, () -> BeanDifference.of(stringList, anotherList, Function.identity()));
    }

    @Test
    public void testComplexMapDifferenceScenario() {
        Map<String, Object> map1 = new HashMap<>();
        map1.put("string", "value1");
        map1.put("integer", 42);
        map1.put("double", 3.14);
        map1.put("boolean", true);
        map1.put("list", Arrays.asList(1, 2, 3));
        map1.put("null", null);
        map1.put("onlyInFirst", "unique");

        Map<String, Object> map2 = new HashMap<>();
        map2.put("string", "value2");
        map2.put("integer", 42);
        map2.put("double", 3.14159);
        map2.put("boolean", true);
        map2.put("list", Arrays.asList(1, 2, 3));
        map2.put("null", null);
        map2.put("onlyInSecond", "unique");

        MapDifference<Map<String, Object>, Map<String, Object>, Map<String, Pair<Object, Object>>> diff = MapDifference.of(map1, map2);

        assertEquals(4, diff.common().size());
        assertEquals(1, diff.onlyOnLeft().size());
        assertTrue(diff.onlyOnLeft().containsKey("onlyInFirst"));
        assertEquals(1, diff.onlyOnRight().size());
        assertTrue(diff.onlyOnRight().containsKey("onlyInSecond"));
        assertEquals(2, diff.differentValues().size());
        assertEquals(Pair.of("value1", "value2"), diff.differentValues().get("string"));
        assertEquals(Pair.of(3.14, 3.14159), diff.differentValues().get("double"));
    }

    @Test
    public void testDifferenceWithLargeCollections() {
        List<Integer> list1 = new ArrayList<>();
        List<Integer> list2 = new ArrayList<>();

        for (int i = 0; i < 10000; i++) {
            list1.add(i);
        }
        for (int i = 5000; i < 15000; i++) {
            list2.add(i);
        }

        Difference<List<Integer>, List<Integer>> diff = Difference.of(list1, list2);

        assertEquals(5000, diff.common().size());
        assertEquals(5000, diff.onlyOnLeft().size());
        assertEquals(5000, diff.onlyOnRight().size());

        assertTrue(diff.common().contains(7500));
        assertTrue(diff.onlyOnLeft().contains(2500));
        assertTrue(diff.onlyOnRight().contains(12500));
    }

    @Test
    public void testDifferenceAllDuplicates() {
        List<String> list1 = Arrays.asList("a", "a", "a", "a");
        List<String> list2 = Arrays.asList("a", "a", "b", "b");

        Difference<List<String>, List<String>> diff = Difference.of(list1, list2);

        assertEquals(Arrays.asList("a", "a"), diff.common());
        assertEquals(Arrays.asList("a", "a"), diff.onlyOnLeft());
        assertEquals(Arrays.asList("b", "b"), diff.onlyOnRight());
    }

    @Test
    public void testPrimitiveListsWithExtremeValues() {
        IntList list1 = IntList.of(Integer.MIN_VALUE, -1, 0, 1, Integer.MAX_VALUE);
        IntList list2 = IntList.of(Integer.MIN_VALUE, 0, Integer.MAX_VALUE);

        Difference<IntList, IntList> diff = Difference.of(list1, list2);

        assertEquals(IntList.of(Integer.MIN_VALUE, 0, Integer.MAX_VALUE), diff.common());
        assertEquals(IntList.of(-1, 1), diff.onlyOnLeft());
        assertTrue(diff.onlyOnRight().isEmpty());
    }

    @Test
    public void testFloatListsWithNaN() {
        FloatList list1 = FloatList.of(1.0f, Float.NaN, 3.0f);
        FloatList list2 = FloatList.of(Float.NaN, 3.0f, 4.0f);

        Difference<FloatList, FloatList> diff = Difference.of(list1, list2);

        assertEquals(FloatList.of(Float.NaN, 3.0f), diff.common());
        assertEquals(FloatList.of(1.0f), diff.onlyOnLeft());
        assertEquals(FloatList.of(4.0f), diff.onlyOnRight());
    }

    @Test
    public void testDoubleListsWithInfinity() {
        DoubleList list1 = DoubleList.of(Double.NEGATIVE_INFINITY, 0.0, Double.POSITIVE_INFINITY);
        DoubleList list2 = DoubleList.of(Double.NEGATIVE_INFINITY, 1.0, Double.POSITIVE_INFINITY);

        Difference<DoubleList, DoubleList> diff = Difference.of(list1, list2);

        assertEquals(DoubleList.of(Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY), diff.common());
        assertEquals(DoubleList.of(0.0), diff.onlyOnLeft());
        assertEquals(DoubleList.of(1.0), diff.onlyOnRight());
    }

    @Test
    public void testOfBooleanArraysEqual() {
        boolean[] a = { true, false, true };
        boolean[] b = { true, false, true };

        Difference<BooleanList, BooleanList> diff = Difference.of(a, b);

        assertEquals(BooleanList.of(true, false, true), diff.common());
        assertTrue(diff.onlyOnLeft().isEmpty());
        assertTrue(diff.onlyOnRight().isEmpty());
        assertTrue(diff.areEqual());
    }

    @Test
    public void testOfGenericArrays() {
        String[] a = { "apple", "banana", "cherry" };
        String[] b = { "banana", "cherry", "date" };

        Difference<List<String>, List<String>> diff = Difference.of(a, b);

        assertEquals(Arrays.asList("banana", "cherry"), diff.common());
        assertEquals(Arrays.asList("apple"), diff.onlyOnLeft());
        assertEquals(Arrays.asList("date"), diff.onlyOnRight());
    }

    @Test
    public void testOfCollectionsWithDuplicates() {
        List<String> a = Arrays.asList("a", "b", "b", "c");
        List<String> b = Arrays.asList("b", "c", "c", "d");

        Difference<List<String>, List<String>> diff = Difference.of(a, b);

        assertEquals(Arrays.asList("b", "c"), diff.common());
        assertEquals(Arrays.asList("a", "b"), diff.onlyOnLeft());
        assertEquals(Arrays.asList("c", "d"), diff.onlyOnRight());
    }

    @Test
    public void testOfOneEmptyCollection() {
        List<String> a = Arrays.asList("a", "b");
        List<String> b = new ArrayList<>();

        Difference<List<String>, List<String>> diff = Difference.of(a, b);

        assertTrue(diff.common().isEmpty());
        assertEquals(Arrays.asList("a", "b"), diff.onlyOnLeft());
        assertTrue(diff.onlyOnRight().isEmpty());
        assertFalse(diff.areEqual());
    }

    @Test
    public void testEqualsAndHashCode() {
        List<String> a1 = Arrays.asList("a", "b", "c");
        List<String> b1 = Arrays.asList("b", "c", "d");
        Difference<List<String>, List<String>> diff1 = Difference.of(a1, b1);

        List<String> a2 = Arrays.asList("a", "b", "c");
        List<String> b2 = Arrays.asList("b", "c", "d");
        Difference<List<String>, List<String>> diff2 = Difference.of(a2, b2);

        assertEquals(diff1, diff2);
        assertEquals(diff1.hashCode(), diff2.hashCode());

        List<String> a3 = Arrays.asList("a", "b");
        List<String> b3 = Arrays.asList("b", "c");
        Difference<List<String>, List<String>> diff3 = Difference.of(a3, b3);

        assertNotEquals(diff1, diff3);
        assertNotEquals(diff1, null);
        assertNotEquals(diff1, "not a difference");
        assertEquals(diff1, diff1);
    }

    @Test
    public void testMapDifferenceBasic() {
        Map<String, Integer> map1 = new HashMap<>();
        map1.put("a", 1);
        map1.put("b", 2);
        map1.put("c", 3);

        Map<String, Integer> map2 = new HashMap<>();
        map2.put("b", 2);
        map2.put("c", 4);
        map2.put("d", 5);

        MapDifference<Map<String, Integer>, Map<String, Integer>, Map<String, Pair<Integer, Integer>>> diff = MapDifference.of(map1, map2);

        assertEquals(1, diff.common().size());
        assertEquals(Integer.valueOf(2), diff.common().get("b"));

        assertEquals(1, diff.onlyOnLeft().size());
        assertEquals(Integer.valueOf(1), diff.onlyOnLeft().get("a"));

        assertEquals(1, diff.onlyOnRight().size());
        assertEquals(Integer.valueOf(5), diff.onlyOnRight().get("d"));

        assertEquals(1, diff.differentValues().size());
        assertEquals(Pair.of(3, 4), diff.differentValues().get("c"));

        assertFalse(diff.areEqual());
    }

    @Test
    public void testMapDifferenceEqual() {
        Map<String, String> map1 = new HashMap<>();
        map1.put("x", "foo");
        map1.put("y", "bar");

        Map<String, String> map2 = new HashMap<>();
        map2.put("x", "foo");
        map2.put("y", "bar");

        MapDifference<Map<String, String>, Map<String, String>, Map<String, Pair<String, String>>> diff = MapDifference.of(map1, map2);

        assertEquals(2, diff.common().size());
        assertTrue(diff.onlyOnLeft().isEmpty());
        assertTrue(diff.onlyOnRight().isEmpty());
        assertTrue(diff.differentValues().isEmpty());
        assertTrue(diff.areEqual());
    }

    @Test
    public void testMapDifferenceWithKeysToCompare() {
        Map<String, Integer> map1 = new HashMap<>();
        map1.put("a", 1);
        map1.put("b", 2);
        map1.put("c", 3);

        Map<String, Integer> map2 = new HashMap<>();
        map2.put("a", 1);
        map2.put("b", 4);
        map2.put("d", 5);

        Collection<String> keysToCompare = Arrays.asList("a", "b");

        MapDifference<Map<String, Integer>, Map<String, Integer>, Map<String, Pair<Integer, Integer>>> diff = MapDifference.of(map1, map2, keysToCompare);

        assertEquals(1, diff.common().size());
        assertEquals(Integer.valueOf(1), diff.common().get("a"));

        assertTrue(diff.onlyOnLeft().isEmpty());
        assertTrue(diff.onlyOnRight().isEmpty());

        assertEquals(1, diff.differentValues().size());
        assertEquals(Pair.of(2, 4), diff.differentValues().get("b"));
    }

    @Test
    public void testMapDifferenceWithCustomEquivalence() {
        Map<String, String> map1 = new HashMap<>();
        map1.put("a", "HELLO");
        map1.put("b", "WORLD");

        Map<String, String> map2 = new HashMap<>();
        map2.put("a", "hello");
        map2.put("b", "world");

        MapDifference<Map<String, String>, Map<String, String>, Map<String, Pair<String, String>>> diff = MapDifference.of(map1, map2,
                (v1, v2) -> v1.equalsIgnoreCase(v2));

        assertEquals(2, diff.common().size());
        assertTrue(diff.onlyOnLeft().isEmpty());
        assertTrue(diff.onlyOnRight().isEmpty());
        assertTrue(diff.differentValues().isEmpty());
        assertTrue(diff.areEqual());
    }

    @Test
    public void testMapDifferenceWithTriPredicateEquivalence() {
        Map<String, Integer> map1 = new HashMap<>();
        map1.put("threshold", 100);
        map1.put("value", 50);

        Map<String, Integer> map2 = new HashMap<>();
        map2.put("threshold", 105);
        map2.put("value", 200);

        MapDifference<Map<String, Integer>, Map<String, Integer>, Map<String, Pair<Integer, Integer>>> diff = MapDifference.of(map1, map2, (k, v1, v2) -> {
            if ("threshold".equals(k)) {
                return Math.abs(v1 - v2) <= 10;
            }
            return v1.equals(v2);
        });

        assertEquals(1, diff.common().size());
        assertTrue(diff.common().containsKey("threshold"));

        assertEquals(1, diff.differentValues().size());
        assertEquals(Pair.of(50, 200), diff.differentValues().get("value"));
    }

    @Test
    public void testMapDifferenceNullValues() {
        Map<String, String> map1 = new HashMap<>();
        map1.put("a", null);
        map1.put("b", "value");

        Map<String, String> map2 = new HashMap<>();
        map2.put("a", null);
        map2.put("b", null);

        MapDifference<Map<String, String>, Map<String, String>, Map<String, Pair<String, String>>> diff = MapDifference.of(map1, map2);

        assertEquals(1, diff.common().size());
        assertNull(diff.common().get("a"));

        assertEquals(1, diff.differentValues().size());
        assertEquals(Pair.of("value", null), diff.differentValues().get("b"));
    }

    @Test
    public void testMapDifferenceLinkedHashMap() {
        Map<String, Integer> map1 = new LinkedHashMap<>();
        map1.put("a", 1);
        map1.put("b", 2);

        Map<String, Integer> map2 = new LinkedHashMap<>();
        map2.put("b", 2);
        map2.put("a", 1);

        MapDifference<Map<String, Integer>, Map<String, Integer>, Map<String, Pair<Integer, Integer>>> diff = MapDifference.of(map1, map2);

        assertTrue(diff.common() instanceof LinkedHashMap);
        assertTrue(diff.onlyOnLeft() instanceof LinkedHashMap);
        assertTrue(diff.onlyOnRight() instanceof LinkedHashMap);
        assertTrue(diff.differentValues() instanceof LinkedHashMap);
    }

    @Test
    public void testMapDifferenceCollections() {
        Map<String, Integer> map1a = new HashMap<>();
        map1a.put("id", 1);
        map1a.put("value", 100);

        Map<String, Integer> map1b = new HashMap<>();
        map1b.put("id", 2);
        map1b.put("value", 200);

        List<Map<String, Integer>> col1 = Arrays.asList(map1a, map1b);

        Map<String, Integer> map2a = new HashMap<>();
        map2a.put("id", 1);
        map2a.put("value", 101);

        Map<String, Integer> map2c = new HashMap<>();
        map2c.put("id", 3);
        map2c.put("value", 300);

        List<Map<String, Integer>> col2 = Arrays.asList(map2a, map2c);

        MapDifference<List<Map<String, Integer>>, List<Map<String, Integer>>, Map<Integer, MapDifference<Map<String, Integer>, Map<String, Integer>, Map<String, Pair<Integer, Integer>>>>> diff = MapDifference
                .of(col1, col2, m -> m.get("id"));

        assertTrue(diff.common().isEmpty());
        assertEquals(1, diff.onlyOnLeft().size());
        assertEquals(Integer.valueOf(200), diff.onlyOnLeft().get(0).get("value"));

        assertEquals(1, diff.onlyOnRight().size());
        assertEquals(Integer.valueOf(300), diff.onlyOnRight().get(0).get("value"));

        assertEquals(1, diff.differentValues().size());
        assertTrue(diff.differentValues().containsKey(1));
    }

    @Test
    public void testMapDifferenceOfWithKeysToCompareAndTriPredicate() {
        Map<String, Integer> map1 = new HashMap<>();
        map1.put("a", 100);
        map1.put("b", 200);
        map1.put("c", 300);

        Map<String, Integer> map2 = new HashMap<>();
        map2.put("a", 105);
        map2.put("b", 250);
        map2.put("c", 310);
        map2.put("d", 400);

        Collection<String> keysToCompare = Arrays.asList("a", "b");

        // TriPredicate: for key "a" allow tolerance of 10, for others require exact match
        MapDifference<Map<String, Integer>, Map<String, Integer>, Map<String, Pair<Integer, Integer>>> diff = MapDifference.of(map1, map2, keysToCompare,
                (k, v1, v2) -> {
                    if ("a".equals(k)) {
                        return Math.abs(v1 - v2) <= 10;
                    }
                    return v1.equals(v2);
                });

        // "a" should be common (within tolerance)
        assertEquals(1, diff.common().size());
        assertTrue(diff.common().containsKey("a"));

        // "b" should have different values (200 vs 250, no tolerance)
        assertEquals(1, diff.differentValues().size());
        assertEquals(Pair.of(200, 250), diff.differentValues().get("b"));

        // "c" and "d" not in keysToCompare, so not counted
        assertTrue(diff.onlyOnLeft().isEmpty());
        assertTrue(diff.onlyOnRight().isEmpty());
    }

    @Test
    public void testMapDifferenceOfWithTriPredicate() {
        Map<String, String> map1 = new HashMap<>();
        map1.put("name", "Alice");
        map1.put("city", "NYC");

        Map<String, String> map2 = new HashMap<>();
        map2.put("name", "alice");
        map2.put("city", "LA");

        // TriPredicate: case-insensitive for "name" key, exact for others
        MapDifference<Map<String, String>, Map<String, String>, Map<String, Pair<String, String>>> diff = MapDifference.of(map1, map2, (k, v1, v2) -> {
            if ("name".equals(k)) {
                return v1.equalsIgnoreCase(v2);
            }
            return v1.equals(v2);
        });

        assertEquals(1, diff.common().size());
        assertTrue(diff.common().containsKey("name"));

        assertEquals(1, diff.differentValues().size());
        assertEquals(Pair.of("NYC", "LA"), diff.differentValues().get("city"));
    }

    @Test
    public void testMapDifferenceNullEquivalence() {
        Map<String, Integer> map1 = new HashMap<>();
        Map<String, Integer> map2 = new HashMap<>();
        assertThrows(IllegalArgumentException.class, () -> MapDifference.of(map1, map2, (BiPredicate<Integer, Integer>) null));
    }

    @Test
    public void testMapDifferenceEqualsHashCodeToString() {
        Map<String, Integer> map1a = new HashMap<>();
        map1a.put("a", 1);
        Map<String, Integer> map2a = new HashMap<>();
        map2a.put("a", 2);

        MapDifference<Map<String, Integer>, Map<String, Integer>, Map<String, Pair<Integer, Integer>>> diff1 = MapDifference.of(map1a, map2a);

        Map<String, Integer> map1b = new HashMap<>();
        map1b.put("a", 1);
        Map<String, Integer> map2b = new HashMap<>();
        map2b.put("a", 2);

        MapDifference<Map<String, Integer>, Map<String, Integer>, Map<String, Pair<Integer, Integer>>> diff2 = MapDifference.of(map1b, map2b);

        assertEquals(diff1, diff2);
        assertEquals(diff1.hashCode(), diff2.hashCode());

        String str = diff1.toString();
        assertTrue(str.contains("differentValues"));
        assertTrue(str.contains("areEqual=false"));
    }

    @Test
    public void testDifferentValues_emptyWhenMapsAreEqual() {
        Map<String, Integer> map1 = new HashMap<>();
        map1.put("a", 1);
        map1.put("b", 2);
        Map<String, Integer> map2 = new HashMap<>();
        map2.put("a", 1);
        map2.put("b", 2);

        MapDifference<Map<String, Integer>, Map<String, Integer>, Map<String, Pair<Integer, Integer>>> diff = MapDifference.of(map1, map2);

        assertTrue(diff.differentValues().isEmpty());
        assertTrue(diff.areEqual());
    }

    @Test
    public void testDifferentValues_multipleEntries() {
        Map<String, Integer> map1 = new HashMap<>();
        map1.put("a", 1);
        map1.put("b", 2);
        map1.put("c", 3);
        Map<String, Integer> map2 = new HashMap<>();
        map2.put("a", 10);
        map2.put("b", 20);
        map2.put("c", 30);

        MapDifference<Map<String, Integer>, Map<String, Integer>, Map<String, Pair<Integer, Integer>>> diff = MapDifference.of(map1, map2);

        assertEquals(3, diff.differentValues().size());
        assertEquals(Pair.of(1, 10), diff.differentValues().get("a"));
        assertEquals(Pair.of(2, 20), diff.differentValues().get("b"));
        assertEquals(Pair.of(3, 30), diff.differentValues().get("c"));
        assertFalse(diff.areEqual());
    }

    @Test
    public void testDifferentValues_withNullValues() {
        Map<String, String> map1 = new HashMap<>();
        map1.put("a", "hello");
        map1.put("b", null);
        Map<String, String> map2 = new HashMap<>();
        map2.put("a", null);
        map2.put("b", "world");

        MapDifference<Map<String, String>, Map<String, String>, Map<String, Pair<String, String>>> diff = MapDifference.of(map1, map2);

        assertEquals(2, diff.differentValues().size());
        assertEquals(Pair.of("hello", null), diff.differentValues().get("a"));
        assertEquals(Pair.of(null, "world"), diff.differentValues().get("b"));
    }

    // --- Additional BeanDifference tests ---

    @Test
    public void testBeanDifferenceOf_Basic() {
        Account a = new Account();
        a.setFirstName("John");
        a.setLastName("Doe");
        a.setStatus(1);

        Account b = new Account();
        b.setFirstName("John");
        b.setLastName("Smith");
        b.setStatus(2);

        BeanDifference<Map<String, Object>, Map<String, Object>, Map<String, Pair<Object, Object>>> diff = BeanDifference.of(a, b);

        assertTrue(diff.common().containsKey("firstName"));
        assertEquals("John", diff.common().get("firstName"));
        assertTrue(diff.differentValues().containsKey("lastName"));
        assertEquals(Pair.of("Doe", "Smith"), diff.differentValues().get("lastName"));
        assertTrue(diff.differentValues().containsKey("status"));
        assertFalse(diff.areEqual());
    }

    @Test
    public void testBeanDifferenceOf_Equal() {
        Account a = new Account();
        a.setFirstName("John");
        a.setLastName("Doe");
        a.setStatus(1);

        Account b = new Account();
        b.setFirstName("John");
        b.setLastName("Doe");
        b.setStatus(1);

        BeanDifference<Map<String, Object>, Map<String, Object>, Map<String, Pair<Object, Object>>> diff = BeanDifference.of(a, b);

        assertTrue(diff.onlyOnLeft().isEmpty());
        assertTrue(diff.onlyOnRight().isEmpty());
        assertTrue(diff.differentValues().isEmpty());
        assertTrue(diff.areEqual());
    }

    @Test
    public void testBeanDifferenceOf_NullBeans() {
        BeanDifference<Map<String, Object>, Map<String, Object>, Map<String, Pair<Object, Object>>> diff = BeanDifference.of((Account) null, (Account) null);

        assertTrue(diff.common().isEmpty());
        assertTrue(diff.onlyOnLeft().isEmpty());
        assertTrue(diff.onlyOnRight().isEmpty());
        assertTrue(diff.differentValues().isEmpty());
        assertTrue(diff.areEqual());
    }

    @Test
    public void testBeanDifferenceOf_OneNullBean() {
        Account a = new Account();
        a.setFirstName("John");
        a.setStatus(1);

        BeanDifference<Map<String, Object>, Map<String, Object>, Map<String, Pair<Object, Object>>> diff1 = BeanDifference.of(a, null);
        assertFalse(diff1.onlyOnLeft().isEmpty());
        assertTrue(diff1.onlyOnRight().isEmpty());
        assertTrue(diff1.common().isEmpty());
        assertFalse(diff1.areEqual());

        BeanDifference<Map<String, Object>, Map<String, Object>, Map<String, Pair<Object, Object>>> diff2 = BeanDifference.of(null, a);
        assertTrue(diff2.onlyOnLeft().isEmpty());
        assertFalse(diff2.onlyOnRight().isEmpty());
        assertTrue(diff2.common().isEmpty());
        assertFalse(diff2.areEqual());
    }

    @Test
    public void testBeanDifferenceOf_WithPropNamesToCompare() {
        Account a = new Account();
        a.setFirstName("John");
        a.setLastName("Doe");
        a.setStatus(1);

        Account b = new Account();
        b.setFirstName("John");
        b.setLastName("Smith");
        b.setStatus(2);

        BeanDifference<Map<String, Object>, Map<String, Object>, Map<String, Pair<Object, Object>>> diff = BeanDifference.of(a, b,
                Arrays.asList("firstName", "lastName"));

        assertEquals("John", diff.common().get("firstName"));
        assertEquals(1, diff.differentValues().size());
        assertEquals(Pair.of("Doe", "Smith"), diff.differentValues().get("lastName"));
        // status not compared
        assertFalse(diff.differentValues().containsKey("status"));
    }

    @Test
    public void testBeanDifferenceOf_WithBiPredicate() {
        Account a = new Account();
        a.setFirstName("JOHN");
        a.setLastName("DOE");

        Account b = new Account();
        b.setFirstName("john");
        b.setLastName("doe");

        // Case-insensitive comparison
        BeanDifference<Map<String, Object>, Map<String, Object>, Map<String, Pair<Object, Object>>> diff = BeanDifference.of(a, b,
                (java.util.function.BiPredicate<Object, Object>) (v1, v2) -> {
                    if (v1 instanceof String && v2 instanceof String) {
                        return ((String) v1).equalsIgnoreCase((String) v2);
                    }
                    return N.equals(v1, v2);
                });

        assertTrue(diff.common().containsKey("firstName"));
        assertTrue(diff.common().containsKey("lastName"));
        assertTrue(diff.differentValues().isEmpty());
    }

    @Test
    public void testBeanDifferenceOf_WithTriPredicate() {
        Account a = new Account();
        a.setFirstName("JOHN");
        a.setLastName("DOE");
        a.setStatus(1);

        Account b = new Account();
        b.setFirstName("john");
        b.setLastName("Smith");
        b.setStatus(1);

        // Case-insensitive for firstName only
        BeanDifference<Map<String, Object>, Map<String, Object>, Map<String, Pair<Object, Object>>> diff = BeanDifference.of(a, b,
                (com.landawn.abacus.util.function.TriPredicate<String, Object, Object>) (propName, v1, v2) -> {
                    if ("firstName".equals(propName) && v1 instanceof String && v2 instanceof String) {
                        return ((String) v1).equalsIgnoreCase((String) v2);
                    }
                    return N.equals(v1, v2);
                });

        assertTrue(diff.common().containsKey("firstName"));
        assertTrue(diff.common().containsKey("status"));
        assertTrue(diff.differentValues().containsKey("lastName"));
    }

    @Test
    public void testBeanDifferenceOf_WithPropNamesAndTriPredicate() {
        Account a = new Account();
        a.setFirstName("JOHN");
        a.setLastName("DOE");
        a.setStatus(1);

        Account b = new Account();
        b.setFirstName("john");
        b.setLastName("Smith");
        b.setStatus(2);

        // Compare only firstName and lastName, with case-insensitive firstName
        BeanDifference<Map<String, Object>, Map<String, Object>, Map<String, Pair<Object, Object>>> diff = BeanDifference.of(a, b,
                Arrays.asList("firstName", "lastName"), (com.landawn.abacus.util.function.TriPredicate<String, Object, Object>) (propName, v1, v2) -> {
                    if ("firstName".equals(propName) && v1 instanceof String && v2 instanceof String) {
                        return ((String) v1).equalsIgnoreCase((String) v2);
                    }
                    return N.equals(v1, v2);
                });

        assertTrue(diff.common().containsKey("firstName"));
        assertTrue(diff.differentValues().containsKey("lastName"));
        // status not compared
        assertFalse(diff.differentValues().containsKey("status"));
        assertFalse(diff.common().containsKey("status"));
    }

    @Test
    public void testBeanDifferenceOf_NonBeanClass() {
        assertThrows(IllegalArgumentException.class, () -> BeanDifference.of("string", "another"));
    }

    @Test
    public void testBeanDifferenceOf_NullBiPredicate() {
        Account a = new Account();
        assertThrows(IllegalArgumentException.class, () -> BeanDifference.of(a, a, (java.util.function.BiPredicate<Object, Object>) null));
    }

    @Test
    public void testBeanDifferenceOf_NullTriPredicate() {
        Account a = new Account();
        assertThrows(IllegalArgumentException.class,
                () -> BeanDifference.of(a, a, (com.landawn.abacus.util.function.TriPredicate<String, Object, Object>) null));
    }

    @Test
    public void testBeanDifferenceOf_DiffIgnoreAnnotation() {
        // Account has @DiffIgnore on lastUpdateTime
        Account a = new Account();
        a.setFirstName("John");
        a.setLastUpdateTime(Dates.currentTimestamp());

        Account b = new Account();
        b.setFirstName("John");
        b.setLastUpdateTime(Dates.currentTimestampPlus(1, TimeUnit.DAYS));

        BeanDifference<Map<String, Object>, Map<String, Object>, Map<String, Pair<Object, Object>>> diff = BeanDifference.of(a, b);

        // lastUpdateTime should be ignored due to @DiffIgnore
        assertFalse(diff.common().containsKey("lastUpdateTime"));
        assertFalse(diff.differentValues().containsKey("lastUpdateTime"));
    }

    @Test
    public void testBeanDifferenceOf_CollectionWithPropNames() {
        Account a1 = new Account();
        a1.setGUI("1");
        a1.setFirstName("John");
        a1.setLastName("Doe");
        a1.setStatus(1);

        Account b1 = new Account();
        b1.setGUI("1");
        b1.setFirstName("John");
        b1.setLastName("Smith");
        b1.setStatus(2);

        List<Account> listA = Arrays.asList(a1);
        List<Account> listB = Arrays.asList(b1);

        // Compare only firstName and lastName
        BeanDifference<List<Account>, List<Account>, Map<String, BeanDifference<Map<String, Object>, Map<String, Object>, Map<String, Pair<Object, Object>>>>> diff = BeanDifference
                .of(listA, listB, Arrays.asList("firstName", "lastName"), Account::getGUI);

        assertTrue(diff.common().isEmpty());
        assertEquals(1, diff.differentValues().size());

        BeanDifference<Map<String, Object>, Map<String, Object>, Map<String, Pair<Object, Object>>> innerDiff = diff.differentValues().get("1");
        assertEquals("John", innerDiff.common().get("firstName"));
        assertEquals(Pair.of("Doe", "Smith"), innerDiff.differentValues().get("lastName"));
        // status should not be compared
        assertFalse(innerDiff.differentValues().containsKey("status"));
    }

    @Test
    public void testBeanDifferenceOf_EmptyCollections() {
        List<Account> emptyList1 = new ArrayList<>();
        List<Account> emptyList2 = new ArrayList<>();

        BeanDifference<List<Account>, List<Account>, Map<String, BeanDifference<Map<String, Object>, Map<String, Object>, Map<String, Pair<Object, Object>>>>> diff = BeanDifference
                .of(emptyList1, emptyList2, Account::getGUI);

        assertTrue(diff.common().isEmpty());
        assertTrue(diff.onlyOnLeft().isEmpty());
        assertTrue(diff.onlyOnRight().isEmpty());
        assertTrue(diff.differentValues().isEmpty());
        assertTrue(diff.areEqual());
    }

    @Test
    public void testBeanDifferenceOf_OneEmptyCollection() {
        Account a1 = new Account();
        a1.setGUI("1");
        a1.setFirstName("John");
        List<Account> listA = Arrays.asList(a1);
        List<Account> emptyList = new ArrayList<>();

        BeanDifference<List<Account>, List<Account>, Map<String, BeanDifference<Map<String, Object>, Map<String, Object>, Map<String, Pair<Object, Object>>>>> diff1 = BeanDifference
                .of(listA, emptyList, Account::getGUI);

        assertEquals(1, diff1.onlyOnLeft().size());
        assertTrue(diff1.onlyOnRight().isEmpty());
        assertFalse(diff1.areEqual());

        BeanDifference<List<Account>, List<Account>, Map<String, BeanDifference<Map<String, Object>, Map<String, Object>, Map<String, Pair<Object, Object>>>>> diff2 = BeanDifference
                .of(emptyList, listA, Account::getGUI);

        assertTrue(diff2.onlyOnLeft().isEmpty());
        assertEquals(1, diff2.onlyOnRight().size());
        assertFalse(diff2.areEqual());
    }

    @Test
    public void testBeanDifferenceEqualsAndHashCode() {
        Account a = new Account();
        a.setFirstName("John");

        Account b = new Account();
        b.setFirstName("Jane");

        BeanDifference<Map<String, Object>, Map<String, Object>, Map<String, Pair<Object, Object>>> diff1 = BeanDifference.of(a, b);
        BeanDifference<Map<String, Object>, Map<String, Object>, Map<String, Pair<Object, Object>>> diff2 = BeanDifference.of(a, b);

        assertEquals(diff1, diff2);
        assertEquals(diff1.hashCode(), diff2.hashCode());
    }

    @Test
    public void testBeanDifferenceToString() {
        Account a = new Account();
        a.setFirstName("John");

        Account b = new Account();
        b.setFirstName("Jane");

        BeanDifference<Map<String, Object>, Map<String, Object>, Map<String, Pair<Object, Object>>> diff = BeanDifference.of(a, b);
        String str = diff.toString();

        assertTrue(str.contains("areEqual="));
        assertTrue(str.contains("common="));
        assertTrue(str.contains("onlyOnLeft="));
        assertTrue(str.contains("onlyOnRight="));
        assertTrue(str.contains("differentValues="));
    }

    @Test
    public void testBeanDifferenceEqualsSameObject() {
        Account a = new Account();
        a.setFirstName("John");

        BeanDifference<Map<String, Object>, Map<String, Object>, Map<String, Pair<Object, Object>>> diff = BeanDifference.of(a, a);
        assertEquals(diff, diff);
    }

    @Test
    public void testBeanDifferenceNotEqualsNull() {
        Account a = new Account();
        a.setFirstName("John");

        BeanDifference<Map<String, Object>, Map<String, Object>, Map<String, Pair<Object, Object>>> diff = BeanDifference.of(a, a);
        assertNotEquals(diff, null);
    }

    @Test
    public void testBeanDifferenceNotEqualsDifferentClass() {
        Account a = new Account();
        a.setFirstName("John");

        BeanDifference<Map<String, Object>, Map<String, Object>, Map<String, Pair<Object, Object>>> diff = BeanDifference.of(a, a);
        assertNotEquals(diff, "not a difference");
    }

    // --- Primitive list one-null tests ---

    @Test
    public void testOfBooleanListsOneNull() {
        BooleanList a = BooleanList.of(true, false);

        Difference<BooleanList, BooleanList> diff1 = Difference.of(a, (BooleanList) null);
        assertEquals(BooleanList.of(true, false), diff1.onlyOnLeft());
        assertTrue(diff1.onlyOnRight().isEmpty());
        assertFalse(diff1.areEqual());

        Difference<BooleanList, BooleanList> diff2 = Difference.of((BooleanList) null, a);
        assertTrue(diff2.onlyOnLeft().isEmpty());
        assertEquals(BooleanList.of(true, false), diff2.onlyOnRight());
        assertFalse(diff2.areEqual());
    }

    @Test
    public void testOfCharListsOneNull() {
        CharList a = CharList.of('a', 'b');

        Difference<CharList, CharList> diff1 = Difference.of(a, (CharList) null);
        assertEquals(CharList.of('a', 'b'), diff1.onlyOnLeft());
        assertTrue(diff1.onlyOnRight().isEmpty());

        Difference<CharList, CharList> diff2 = Difference.of((CharList) null, a);
        assertTrue(diff2.onlyOnLeft().isEmpty());
        assertEquals(CharList.of('a', 'b'), diff2.onlyOnRight());
    }

    @Test
    public void testOfByteListsOneNull() {
        ByteList a = ByteList.of((byte) 1, (byte) 2);

        Difference<ByteList, ByteList> diff1 = Difference.of(a, (ByteList) null);
        assertEquals(ByteList.of((byte) 1, (byte) 2), diff1.onlyOnLeft());
        assertTrue(diff1.onlyOnRight().isEmpty());

        Difference<ByteList, ByteList> diff2 = Difference.of((ByteList) null, a);
        assertTrue(diff2.onlyOnLeft().isEmpty());
        assertEquals(ByteList.of((byte) 1, (byte) 2), diff2.onlyOnRight());
    }

    @Test
    public void testOfShortListsOneNull() {
        ShortList a = ShortList.of((short) 1, (short) 2);

        Difference<ShortList, ShortList> diff1 = Difference.of(a, (ShortList) null);
        assertEquals(ShortList.of((short) 1, (short) 2), diff1.onlyOnLeft());
        assertTrue(diff1.onlyOnRight().isEmpty());

        Difference<ShortList, ShortList> diff2 = Difference.of((ShortList) null, a);
        assertTrue(diff2.onlyOnLeft().isEmpty());
        assertEquals(ShortList.of((short) 1, (short) 2), diff2.onlyOnRight());
    }

    @Test
    public void testOfIntListsOneNull() {
        IntList a = IntList.of(1, 2);

        Difference<IntList, IntList> diff1 = Difference.of(a, (IntList) null);
        assertEquals(IntList.of(1, 2), diff1.onlyOnLeft());
        assertTrue(diff1.onlyOnRight().isEmpty());

        Difference<IntList, IntList> diff2 = Difference.of((IntList) null, a);
        assertTrue(diff2.onlyOnLeft().isEmpty());
        assertEquals(IntList.of(1, 2), diff2.onlyOnRight());
    }

    @Test
    public void testOfLongListsOneNull() {
        LongList a = LongList.of(1L, 2L);

        Difference<LongList, LongList> diff1 = Difference.of(a, (LongList) null);
        assertEquals(LongList.of(1L, 2L), diff1.onlyOnLeft());
        assertTrue(diff1.onlyOnRight().isEmpty());

        Difference<LongList, LongList> diff2 = Difference.of((LongList) null, a);
        assertTrue(diff2.onlyOnLeft().isEmpty());
        assertEquals(LongList.of(1L, 2L), diff2.onlyOnRight());
    }

    @Test
    public void testOfFloatListsOneNull() {
        FloatList a = FloatList.of(1.0f, 2.0f);

        Difference<FloatList, FloatList> diff1 = Difference.of(a, (FloatList) null);
        assertEquals(FloatList.of(1.0f, 2.0f), diff1.onlyOnLeft());
        assertTrue(diff1.onlyOnRight().isEmpty());

        Difference<FloatList, FloatList> diff2 = Difference.of((FloatList) null, a);
        assertTrue(diff2.onlyOnLeft().isEmpty());
        assertEquals(FloatList.of(1.0f, 2.0f), diff2.onlyOnRight());
    }

    @Test
    public void testOfDoubleListsOneNull() {
        DoubleList a = DoubleList.of(1.0, 2.0);

        Difference<DoubleList, DoubleList> diff1 = Difference.of(a, (DoubleList) null);
        assertEquals(DoubleList.of(1.0, 2.0), diff1.onlyOnLeft());
        assertTrue(diff1.onlyOnRight().isEmpty());

        Difference<DoubleList, DoubleList> diff2 = Difference.of((DoubleList) null, a);
        assertTrue(diff2.onlyOnLeft().isEmpty());
        assertEquals(DoubleList.of(1.0, 2.0), diff2.onlyOnRight());
    }

    @Test
    public void testOfObjectArraysOneNull() {
        String[] a = { "a", "b" };

        Difference<List<String>, List<String>> diff1 = Difference.of(a, (String[]) null);
        assertEquals(Arrays.asList("a", "b"), diff1.onlyOnLeft());
        assertTrue(diff1.onlyOnRight().isEmpty());

        Difference<List<String>, List<String>> diff2 = Difference.of((String[]) null, a);
        assertTrue(diff2.onlyOnLeft().isEmpty());
        assertEquals(Arrays.asList("a", "b"), diff2.onlyOnRight());
    }

    @Test
    public void testMapDifferenceOfOneNullMap() {
        Map<String, Integer> map1 = new HashMap<>();
        map1.put("a", 1);
        map1.put("b", 2);

        MapDifference<Map<String, Integer>, Map<String, Integer>, Map<String, Pair<Integer, Integer>>> diff1 = MapDifference.of(map1, null);
        assertEquals(2, diff1.onlyOnLeft().size());
        assertTrue(diff1.onlyOnRight().isEmpty());
        assertTrue(diff1.common().isEmpty());
        assertFalse(diff1.areEqual());

        MapDifference<Map<String, Integer>, Map<String, Integer>, Map<String, Pair<Integer, Integer>>> diff2 = MapDifference.of(null, map1);
        assertTrue(diff2.onlyOnLeft().isEmpty());
        assertEquals(2, diff2.onlyOnRight().size());
        assertTrue(diff2.common().isEmpty());
        assertFalse(diff2.areEqual());
    }

    @Test
    public void testMapDifferenceOfWithKeysToCompare_OneNullMap() {
        Map<String, Integer> map1 = new HashMap<>();
        map1.put("a", 1);
        map1.put("b", 2);

        Collection<String> keys = Arrays.asList("a");

        MapDifference<Map<String, Integer>, Map<String, Integer>, Map<String, Pair<Integer, Integer>>> diff1 = MapDifference.of(map1, null, keys);
        assertEquals(1, diff1.onlyOnLeft().size());
        assertTrue(diff1.onlyOnLeft().containsKey("a"));
        assertFalse(diff1.onlyOnLeft().containsKey("b")); // b not in keysToCompare

        MapDifference<Map<String, Integer>, Map<String, Integer>, Map<String, Pair<Integer, Integer>>> diff2 = MapDifference.of(null, map1, keys);
        assertEquals(1, diff2.onlyOnRight().size());
        assertTrue(diff2.onlyOnRight().containsKey("a"));
    }

    @Test
    public void testMapDifferenceOfWithKeysToCompare_BothNull() {
        MapDifference<Map<String, Integer>, Map<String, Integer>, Map<String, Pair<Integer, Integer>>> diff = MapDifference.of((Map<String, Integer>) null,
                (Map<String, Integer>) null, Arrays.asList("a"));

        assertTrue(diff.common().isEmpty());
        assertTrue(diff.onlyOnLeft().isEmpty());
        assertTrue(diff.onlyOnRight().isEmpty());
        assertTrue(diff.areEqual());
    }

    @Test
    public void testMapDifferenceNullTriPredicateEquivalence() {
        Map<String, Integer> map1 = new HashMap<>();
        Map<String, Integer> map2 = new HashMap<>();
        assertThrows(IllegalArgumentException.class,
                () -> MapDifference.of(map1, map2, (com.landawn.abacus.util.function.TriPredicate<String, Integer, Integer>) null));
    }

    @Test
    public void testDifferenceAreEqual_PrimitiveList() {
        IntList a = IntList.of(1, 2, 3);
        IntList b = IntList.of(3, 2, 1);

        Difference<IntList, IntList> diff = Difference.of(a, b);
        assertTrue(diff.areEqual());
    }

    @Test
    public void testDifferenceAreEqual_BooleanList() {
        BooleanList a = BooleanList.of(true, false);
        BooleanList b = BooleanList.of(false, true);

        Difference<BooleanList, BooleanList> diff = Difference.of(a, b);
        assertTrue(diff.areEqual());
    }

}

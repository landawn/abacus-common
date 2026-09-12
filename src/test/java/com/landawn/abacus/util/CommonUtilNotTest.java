package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.junit.jupiter.api.Test;

public class CommonUtilNotTest extends CommonUtilTestSupport {

    @Test
    public void testNotEmpty() {
        assertFalse(CommonUtil.notEmpty((String) null));
        assertFalse(CommonUtil.notEmpty(""));
        assertTrue(CommonUtil.notEmpty("a"));
        assertTrue(CommonUtil.notEmpty(" "));

        assertFalse(CommonUtil.notEmpty((boolean[]) null));
        assertFalse(CommonUtil.notEmpty(new boolean[0]));
        assertTrue(CommonUtil.notEmpty(new boolean[] { true }));
        assertFalse(CommonUtil.notEmpty((char[]) null));
        assertFalse(CommonUtil.notEmpty(new char[0]));
        assertTrue(CommonUtil.notEmpty(new char[] { 'a' }));
        assertFalse(CommonUtil.notEmpty((byte[]) null));
        assertFalse(CommonUtil.notEmpty(new byte[0]));
        assertTrue(CommonUtil.notEmpty(new byte[] { 1 }));
        assertFalse(CommonUtil.notEmpty((short[]) null));
        assertFalse(CommonUtil.notEmpty(new short[0]));
        assertTrue(CommonUtil.notEmpty(new short[] { 1 }));
        assertFalse(CommonUtil.notEmpty((int[]) null));
        assertFalse(CommonUtil.notEmpty(new int[0]));
        assertTrue(CommonUtil.notEmpty(new int[] { 1 }));
        assertFalse(CommonUtil.notEmpty((long[]) null));
        assertFalse(CommonUtil.notEmpty(new long[0]));
        assertTrue(CommonUtil.notEmpty(new long[] { 1L }));
        assertFalse(CommonUtil.notEmpty((float[]) null));
        assertFalse(CommonUtil.notEmpty(new float[0]));
        assertTrue(CommonUtil.notEmpty(new float[] { 1.0f }));
        assertFalse(CommonUtil.notEmpty((double[]) null));
        assertFalse(CommonUtil.notEmpty(new double[0]));
        assertTrue(CommonUtil.notEmpty(new double[] { 1.0 }));
        assertFalse(CommonUtil.notEmpty((Object[]) null));
        assertFalse(CommonUtil.notEmpty(new Object[0]));
        assertTrue(CommonUtil.notEmpty(new Object[] { "a" }));

        assertFalse(CommonUtil.notEmpty((Collection<?>) null));
        assertFalse(CommonUtil.notEmpty(new ArrayList<>()));
        assertTrue(CommonUtil.notEmpty(Arrays.asList("a")));

        assertFalse(CommonUtil.notEmpty((Map<?, ?>) null));
        assertFalse(CommonUtil.notEmpty(new HashMap<>()));
        Map<String, String> map = new HashMap<>();
        map.put("key", "value");
        assertTrue(CommonUtil.notEmpty(map));

        assertFalse(CommonUtil.notEmpty((Iterable<?>) null));
        assertFalse(CommonUtil.notEmpty(Collections.emptyList()));
        assertTrue(CommonUtil.notEmpty(Arrays.asList(1)));
        assertFalse(CommonUtil.notEmpty((Iterable<Integer>) Collections::emptyIterator));
        assertTrue(CommonUtil.notEmpty((Iterable<Integer>) () -> Arrays.asList(1, 2).iterator()));

        assertFalse(CommonUtil.notEmpty((Iterator<?>) null));
        assertFalse(CommonUtil.notEmpty(Collections.emptyIterator()));
        assertTrue(CommonUtil.notEmpty(Arrays.asList(1).iterator()));

        assertTrue(CommonUtil.notEmpty(IntList.of(1)));
        assertFalse(CommonUtil.notEmpty(IntList.of()));

        Multiset<String> ms = new Multiset<>();
        assertFalse(CommonUtil.notEmpty(ms));
        ms.add("a");
        assertTrue(CommonUtil.notEmpty(ms));

        ListMultimap<String, Integer> mm = CommonUtil.newListMultimap();
        assertFalse(CommonUtil.notEmpty(mm));
        mm.put("key", 1);
        assertTrue(CommonUtil.notEmpty(mm));

        assertFalse(CommonUtil.notEmpty(CommonUtil.newEmptyDataset()));
        assertTrue(CommonUtil.notEmpty(CommonUtil.newDataset(Arrays.asList("col1"), Arrays.asList(Arrays.asList(1)))));
    }

    @Test
    public void testNotBlank() {
        assertFalse(CommonUtil.notBlank(null));
        assertFalse(CommonUtil.notBlank(""));
        assertFalse(CommonUtil.notBlank(" "));
        assertFalse(CommonUtil.notBlank("   "));
        assertFalse(CommonUtil.notBlank("\t"));
        assertTrue(CommonUtil.notBlank("a"));
        assertTrue(CommonUtil.notBlank(" a "));
    }
}

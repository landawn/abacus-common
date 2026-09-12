package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.util.function.Predicate;

public class NIsTest extends NTestSupport {

    @Test
    public void testIsSubCollection() {
        Collection<String> a = Arrays.asList("a", "b", "c");
        Collection<String> b = Arrays.asList("a", "b", "c", "d");
        Collection<String> c = Arrays.asList("a", "b");
        Collection<String> d = Arrays.asList("a", "x");
        Collection<String> e = Arrays.asList("a", "a", "b");
        Collection<String> f = Arrays.asList("a", "b", "a");

        assertTrue(N.isSubCollection(c, a));
        assertTrue(N.isSubCollection(a, b));
        assertTrue(N.isSubCollection(a, a));
        assertTrue(N.isSubCollection(Collections.emptyList(), a));
        assertFalse(N.isSubCollection(a, c));
        assertFalse(N.isSubCollection(d, a));
        assertFalse(N.isSubCollection(a, Collections.emptyList()));
        assertTrue(N.isSubCollection(e, f));
        assertTrue(N.isSubCollection(Arrays.asList("a", "b"), e));
        assertFalse(N.isSubCollection(e, Arrays.asList("a", "b")));
        assertThrows(IllegalArgumentException.class, () -> N.isSubCollection(null, a));
        assertThrows(IllegalArgumentException.class, () -> N.isSubCollection(a, null));

        Collection<?> equalA = CommonUtil.toList("a", "b", "c");
        Collection<?> equalB = CommonUtil.toSet("b", "a", "c");
        assertTrue(N.isEqualCollection(equalA, equalB));
        assertTrue(N.isSubCollection(equalA, equalB));
        assertFalse(N.isProperSubCollection(equalB, equalA));
        assertTrue(N.isProperSubCollection(equalB, CommonUtil.toList("a", "b", "c", "a")));
    }

    @Test
    public void testIsProperSubCollection() {
        Collection<String> a = Arrays.asList("a", "b", "c");
        Collection<String> b = Arrays.asList("a", "b", "c", "d");
        Collection<String> c = Arrays.asList("a", "b");

        assertTrue(N.isProperSubCollection(c, a));
        assertTrue(N.isProperSubCollection(a, b));
        assertFalse(N.isProperSubCollection(a, a));
        assertTrue(N.isProperSubCollection(Collections.emptyList(), a));
        assertFalse(N.isProperSubCollection(a, c));
        assertFalse(N.isProperSubCollection(a, Collections.emptyList()));
        assertTrue(N.isProperSubCollection(Arrays.asList("a", "b"), Arrays.asList("a", "a", "b")));
        assertFalse(N.isProperSubCollection(Arrays.asList("a", "a", "b"), Arrays.asList("a", "b")));
        assertThrows(IllegalArgumentException.class, () -> N.isProperSubCollection(null, a));
        assertThrows(IllegalArgumentException.class, () -> N.isProperSubCollection(a, null));
    }

    @Test
    public void testIsEqualCollection() {
        assertTrue(N.isEqualCollection(Arrays.asList("a", "b", "a"), Arrays.asList("a", "a", "b")));
        assertTrue(N.isEqualCollection(null, null));
        assertTrue(N.isEqualCollection(Collections.emptyList(), Collections.emptySet()));
        assertFalse(N.isEqualCollection(Arrays.asList("a", "b", "a"), Arrays.asList("a", "b")));
        assertFalse(N.isEqualCollection(Arrays.asList("a", "b", "a"), null));
        assertFalse(N.isEqualCollection(null, Arrays.asList("a")));
        assertTrue(N.isEqualCollection(Arrays.asList(1, 2, 2, 3), new LinkedList<>(Arrays.asList(3, 2, 1, 2))));
    }

    @Test
    public void testHasMatchCountBetween() {
        Integer[] arr = { 2, 4, 5, 6, 8 };
        assertTrue(N.hasMatchCountBetween(arr, 2, 5, (Predicate<Integer>) x -> x % 2 == 0));
        assertTrue(N.hasMatchCountBetween(arr, 4, 4, (Predicate<Integer>) x -> x % 2 == 0));
        assertFalse(N.hasMatchCountBetween(arr, 5, 5, (Predicate<Integer>) x -> x % 2 == 0));
        assertFalse(N.hasMatchCountBetween(arr, 0, 2, (Predicate<Integer>) x -> x % 2 == 0));

        List<Integer> list = Arrays.asList(2, 4, 5, 6, 8);
        assertTrue(N.hasMatchCountBetween(list, 3, 5, (Predicate<Integer>) x -> x % 2 == 0));
        assertFalse(N.hasMatchCountBetween(list, 5, 10, (Predicate<Integer>) x -> x % 2 == 0));
        assertTrue(N.hasMatchCountBetween(Arrays.asList(1, 2, 3, 4, 5).iterator(), 2, 3, (Predicate<Integer>) x -> x % 2 == 0));

        Iterator<Integer> iterator = Arrays.asList(2, 4, 99).iterator();
        assertFalse(N.hasMatchCountBetween(iterator, 0, 1, (Predicate<Integer>) x -> x % 2 == 0));
        assertEquals(99, iterator.next());

        assertTrue(N.hasMatchCountBetween(new Integer[] {}, 0, 5, (Predicate<Integer>) x -> true));
        assertFalse(N.hasMatchCountBetween(new Integer[] {}, 1, 5, (Predicate<Integer>) x -> true));
        assertThrows(IllegalArgumentException.class, () -> N.hasMatchCountBetween(arr, -1, 5, (Predicate<Integer>) x -> true));
        assertThrows(IllegalArgumentException.class, () -> N.hasMatchCountBetween(arr, 5, 2, (Predicate<Integer>) x -> true));
    }

    @Test
    public void testIsSorted() {
        assertTrue(CommonUtil.isSorted(CommonUtil.toLinkedHashSet(1, 3, 5)));
        assertTrue(CommonUtil.isSorted(CommonUtil.toLinkedHashSet(1, 3, 5, 5, 7, 9, 10)));
        assertFalse(CommonUtil.isSorted(CommonUtil.toLinkedHashSet(1, 7, 5)));
        assertFalse(CommonUtil.isSorted(CommonUtil.toLinkedHashSet(1, 2, 2, 3, 7, 5)));
        assertTrue(CommonUtil.isSorted(CommonUtil.toLinkedHashSet(1, 2, 2, 3, 7, 5), 1, 3));
        assertTrue(CommonUtil.isSorted(CommonUtil.toLinkedHashSet(1, 2, 3, 7, 5), 0, 4));
        assertFalse(CommonUtil.isSorted(CommonUtil.toLinkedHashSet(1, 2, 3, 7, 5), 2, 5));

        assertFalse(CommonUtil.isSorted(new int[] { 1, 7, 5 }));
        assertTrue(CommonUtil.isSorted(new int[] { 1, 2, 2, 3, 7, 5 }, 1, 3));
        assertTrue(CommonUtil.isSorted(new int[] { 1, 2, 3, 7, 5 }, 0, 4));
        assertFalse(CommonUtil.isSorted(new int[] { 1, 2, 3, 7, 5 }, 2, 5));
    }

    @Test
    public void testIsEmpty() {
        assertTrue(CommonUtil.isEmpty(new boolean[0]));
        assertTrue(CommonUtil.isEmpty(new char[0]));
        assertTrue(CommonUtil.isEmpty(new byte[0]));
        assertTrue(CommonUtil.isEmpty(new short[0]));
        assertTrue(CommonUtil.isEmpty(new int[0]));
        assertTrue(CommonUtil.isEmpty(new long[0]));
        assertTrue(CommonUtil.isEmpty(new float[0]));
        assertTrue(CommonUtil.isEmpty(new double[0]));
        assertTrue(CommonUtil.isEmpty((int[]) null));
        assertTrue(CommonUtil.isEmpty(new String[] {}));
        assertTrue(CommonUtil.isEmpty((String[]) null));
        assertFalse(CommonUtil.isEmpty(new int[] { 1 }));
        assertFalse(CommonUtil.isEmpty(new String[] { null }));
        assertTrue(CommonUtil.notEmpty(new boolean[1]));
        assertTrue(CommonUtil.notEmpty(new int[1]));

        assertTrue(CommonUtil.isEmpty(new ArrayList<>()));
        assertTrue(CommonUtil.isEmpty((java.util.Collection<?>) null));
        assertFalse(CommonUtil.isEmpty(Arrays.asList(1)));
        assertTrue(CommonUtil.isEmpty(new HashMap<>()));
        assertTrue(CommonUtil.isEmpty((Map<?, ?>) null));
        Map<String, Integer> map = new HashMap<>();
        map.put("a", 1);
        assertFalse(CommonUtil.isEmpty(map));
        assertTrue(CommonUtil.isEmpty(""));
        assertTrue(CommonUtil.isEmpty((String) null));
        assertFalse(CommonUtil.isEmpty("a"));
        assertFalse(CommonUtil.isEmpty(" "));
    }
}

package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.function.Supplier;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

/**
 * Regression tests for the Multiset findings of the 2026-09-25 cross review (U22-03, U22-05 - fixer F08).
 */
public class MultisetReview20260925Test extends TestBase {

    // ------------------------------------------------------------------------------------------------
    // U22-05: addAll(Multiset) iterates another source directly and merges each count with one lookup
    // ------------------------------------------------------------------------------------------------

    @Test
    public void testU2205_addAllFromAnotherMultisetMergesCountsAndLeavesTheSourceUntouched() {
        final Multiset<String> dst = new Multiset<>();
        dst.add("a", 2);
        dst.add("c", 5);
        final Multiset<String> src = new Multiset<>();
        src.add("a", 3);
        src.add("b", 4);

        assertTrue(dst.addAll(src));
        assertEquals(5, dst.getCount("a"));   // existing element: in-place count update
        assertEquals(4, dst.getCount("b"));   // new element: inserted
        assertEquals(5, dst.getCount("c"));
        assertEquals(3, src.getCount("a"));
        assertEquals(4, src.getCount("b"));
        assertEquals(2, src.entrySet().size());

        assertTrue(dst.addAll(src, 2));
        assertEquals(11, dst.getCount("a"));
        assertEquals(12, dst.getCount("b"));
        assertEquals(5, dst.getCount("c"));

        assertFalse(dst.addAll(new Multiset<String>()));
        assertFalse(dst.addAll(src, 0));
        assertEquals(11, dst.getCount("a"));
    }

    @Test
    public void testU2205_addAllSelfStillDoubles() {
        final Multiset<String> ms = new Multiset<>();
        ms.add("a", 2);
        ms.add("b", 3);

        assertTrue(ms.addAll(ms));
        assertEquals(4, ms.getCount("a"));
        assertEquals(6, ms.getCount("b"));

        assertTrue(ms.addAll(ms, 3));
        assertEquals(16, ms.getCount("a"));
        assertEquals(24, ms.getCount("b"));
        assertEquals(2, ms.entrySet().size());
    }

    @Test
    public void testU2205_addAllOverflowLeavesTheFailingElementUnchanged() {
        final Multiset<String> big = new Multiset<>();
        big.add("x", Integer.MAX_VALUE - 1);

        final Multiset<String> src = new Multiset<>(LinkedHashMap.class);
        src.add("x", 2);
        src.add("y", 1);

        final IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> big.addAll(src));
        assertTrue(e.getMessage().contains("out of the bound of int"), e.getMessage());
        assertTrue(e.getMessage().contains("current=" + (Integer.MAX_VALUE - 1)), e.getMessage());
        assertEquals(Integer.MAX_VALUE - 1, big.getCount("x"));
        assertEquals(0, big.getCount("y"));   // "x" failed first: nothing after it was added

        // an exact fit is accepted
        final Multiset<String> one = new Multiset<>();
        one.add("x", 1);
        assertTrue(big.addAll(one));
        assertEquals(Integer.MAX_VALUE, big.getCount("x"));

        // self-add at MAX_VALUE: refused, unchanged
        assertThrows(IllegalArgumentException.class, () -> big.addAll(big));
        assertEquals(Integer.MAX_VALUE, big.getCount("x"));

        // count * occurrencesToAdd beyond int, for a new element
        final Multiset<String> huge = new Multiset<>();
        huge.add("x", Integer.MAX_VALUE);
        final Multiset<String> empty = new Multiset<>();
        assertThrows(IllegalArgumentException.class, () -> empty.addAll(huge, 2));
        assertEquals(0, empty.getCount("x"));
        assertTrue(empty.isEmpty());
    }

    // ------------------------------------------------------------------------------------------------
    // U22-03: toMap() of a ConcurrentSkipListMap backing yields a ConcurrentSkipListMap
    // ------------------------------------------------------------------------------------------------

    @Test
    public void testU2203_toMapOfAConcurrentSkipListMapBackingYieldsAConcurrentSkipListMap() {
        final Supplier<Map<String, Object>> concurrentSorted = () -> new ConcurrentSkipListMap<>(String.CASE_INSENSITIVE_ORDER);
        final Multiset<String> ms = new Multiset<>(concurrentSorted);
        ms.add("b", 2);
        ms.add("A", 1);
        ms.add("a", 1);

        final Map<String, Integer> map = ms.toMap();
        assertEquals(ConcurrentSkipListMap.class, map.getClass());
        assertSame(String.CASE_INSENSITIVE_ORDER, ((SortedMap<String, Integer>) map).comparator());
        assertEquals(List.of("A", "b"), new ArrayList<>(map.keySet()));
        assertEquals(Integer.valueOf(2), map.get("a"));
        assertEquals(Integer.valueOf(2), map.get("B"));

        // a TreeMap backing still yields a TreeMap with the same comparator
        final Comparator<String> reverse = Comparator.reverseOrder();
        final Supplier<Map<String, Object>> sorted = () -> new TreeMap<>(reverse);
        final Multiset<String> tree = new Multiset<>(sorted);
        tree.add("a");
        tree.add("b");
        final Map<String, Integer> treeMap = tree.toMap();
        assertEquals(TreeMap.class, treeMap.getClass());
        assertSame(reverse, ((SortedMap<String, Integer>) treeMap).comparator());
        assertEquals(List.of("b", "a"), new ArrayList<>(treeMap.keySet()));
    }
}

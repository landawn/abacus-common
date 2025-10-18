/*
 * Copyright (c) 2015, Haiyang Li. All rights reserved.
 */

package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.AbstractTest;

public class MultisetTest extends AbstractTest {

    @Test
    public void test_01() {
        Multiset<String> set = CommonUtil.asMultiset("a", "b", "c", "C");
        N.println(set);
        set.add("a");

        assertEquals(2, set.getCount("a"));
        N.println(set);
        set.remove("a");

        assertEquals(1, set.getCount("a"));
        set.add("b", 100);
        set.remove("b", 90);

        assertEquals(11, set.getCount("b"));
        N.println(set);
        set.remove("b", 11);
        assertEquals(0, set.getCount("b"));

        set.add("C");
        N.println(set);
        N.println(set.toMapSortedByOccurrences());
        N.println(set.toMapSortedBy((a, b) -> a.getKey().compareTo(b.getKey())));
        N.println(set.maxOccurrences());
        N.println(set.minOccurrences());
        N.println(set.sumOfOccurrences());
        N.println(set.averageOfOccurrences());
    }

    @Test
    public void test_02() {
        Multiset<String> set = CommonUtil.asMultiset("a", "b", "c");
        N.println(set);

        set.setCount("a", 0);
        set.setCount("a", 3);
        assertEquals(3, set.getCount("a"));

        set.entrySet().forEach(Fn.println());

        set.entrySet().forEach(Fn.println());

        try {
            set.setCount("a", -1);
            fail("Should throw IllegalArgumentException");
        } catch (IllegalArgumentException e) {

        }

        assertEquals(3, set.maxOccurrences().get().left().intValue());

        try {
            set.add("a", -1);
            fail("Should throw IllegalArgumentException");
        } catch (IllegalArgumentException e) {

        }

        try {
            set.add("a", Integer.MAX_VALUE);
            fail("Should throw IllegalArgumentException");
        } catch (IllegalArgumentException e) {

        }

        assertEquals(3, set.getCount("a"));

        assertTrue(set.contains("a"));
        assertFalse(set.contains("e"));

        assertTrue(set.containsAll(CommonUtil.asList("a", "b")));
        assertFalse(set.contains(CommonUtil.asList("b", "e")));

        assertTrue(set.containsAll(CommonUtil.asList("a")));
        assertFalse(set.contains(CommonUtil.asList("e")));

        try {
            set.remove("a", -1);
            fail("Should throw IllegalArgumentException");
        } catch (IllegalArgumentException e) {

        }

        set.remove("a", 2);
        assertEquals(1, set.getCount("a"));

        set.remove("a", 2);
        assertEquals(0, set.getCount("a"));

        set.add("a", 3);
        assertEquals(3, set.getCount("a"));

        CommonUtil.asList("a").forEach(e -> set.remove(e));
        assertEquals(2, set.getCount("a"));
        assertEquals(1, set.getCount("b"));

        CommonUtil.asList("a", "b", "e").forEach(e -> set.remove(e, 2));
        assertEquals(0, set.getCount("a"));
        assertEquals(0, set.getCount("b"));

        set.add("a", 3);
        set.add("b", 3);

        set.retainAll(CommonUtil.asList("a", "b", "e"));

        assertEquals(3, set.getCount("a"));
        assertEquals(3, set.getCount("b"));

        Multiset<String> set2 = CommonUtil.asMultiset();
        set2.setCount("a", 3);
        set2.setCount("b", 3);

        assertTrue(CommonUtil.asSet(set).contains(set2));

        set.clear();
        assertTrue(set.isEmpty());
    }
}

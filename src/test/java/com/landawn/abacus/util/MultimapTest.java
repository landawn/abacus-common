/*
 * Copyright (c) 2015, Haiyang Li. All rights reserved.
 */

package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.AbstractTest;

public class MultimapTest extends AbstractTest {

    @Test
    public void test_01() {
        Multimap<String, Integer, List<Integer>> map = CommonUtil.newListMultimap();
        map.put("a", 1);
        map.put("a", 2);
        map.put("a", 3);

        map.put("b", 4);
        map.put("b", 5);
        map.put("b", 6);

        map.put("c", 7);
        map.put("c", 8);
        map.put("c", 9);

        List<Integer> list = map.get("a");

        N.println(list);

        assertEquals(CommonUtil.asList(1, 2, 3), list);

        N.println(map.keySet());
        N.println(map.values());

        assertTrue(map.containsKey("a"));
        assertTrue(map.containsValue(5));

        assertFalse(map.containsKey("e"));
        assertFalse(map.containsValue(0));

        N.println(map.size());
        N.println(map.isEmpty());
        N.println(map.entrySet());

        Multimap<String, Integer, Set<Integer>> map2 = CommonUtil.newSetMultimap();
        map2.put("a", 11);
        map2.put("a", 12);
        map2.put("a", 13);

        map2.put("b", 15);

        map2.put("d", 20);
        map.putMany(map2);

        N.println(map);

        N.println(map.hashCode());
        N.println(map.equals(null));
        N.println(map.toString());

        map.removeAll("a");
        assertNull(map.get("a"));

        map.clear();

        assertEquals(0, map.size());
    }

    @Test
    public void test_02() {
        Map<String, Integer> m = CommonUtil.asMap("abc", 123, "abc", 123, "abc", 456, "a", 1, "b", 2);
        Multimap<String, Integer, List<Integer>> multimap2 = ListMultimap.create(m);
        N.println(multimap2);

    }
}

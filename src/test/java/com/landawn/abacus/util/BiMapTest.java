/*
 * Copyright (c) 2015, Haiyang Li. All rights reserved.
 */

package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.AbstractTest;

public class BiMapTest extends AbstractTest {

    @Test
    public void test_01() {
        BiMap<String, Integer> biMap = new BiMap<>();
        biMap.put("a", 1);
        biMap.put("b", 2);
        N.println(biMap);
        assertEquals(1, biMap.get("a").intValue());
        assertEquals("a", biMap.getByValue(1));

        biMap.put("a", 11);
        N.println(biMap);
        assertNull(biMap.getByValue(1));

        biMap.forcePut("c", 2);
        assertNull(biMap.get("b"));

        try {
            biMap.put(null, 1);
            fail("Should throw IllegalArgumentException");
        } catch (IllegalArgumentException e) {
        }

        try {
            biMap.put("d", null);
            fail("Should throw IllegalArgumentException");
        } catch (IllegalArgumentException e) {
        }

        Map<String, Integer> map = BiMap.of("e", 5);
        biMap.putAll(map);
        N.println(biMap);

        assertEquals(5, biMap.remove("e").intValue());
        assertEquals("c", biMap.removeByValue(2));

        N.println(biMap);

        assertTrue(biMap.containsKey("a"));
        assertTrue(biMap.containsValue(11));
        biMap.clear();

        assertTrue(biMap.isEmpty());
        assertTrue(biMap.size() == 0);

        N.println(biMap.hashCode());
        N.println(biMap.equals(new HashMap<>()));
        N.println(biMap.toString());
        N.println(biMap.keySet());
        N.println(biMap.values());
        assertNull(biMap.remove("a"));
        assertNull(biMap.removeByValue(1));
        BiMap<String, Integer> biMap2 = new BiMap<>(map.size() * 2);
        biMap2.putAll(map);

        N.println(biMap2);
    }
}

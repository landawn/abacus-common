package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.AbstractTest;

@Tag("old-test")
public class JDK8HashMapTest extends AbstractTest {

    @Test
    public void test_HashMap() {
        Map<String, Integer> m = new HashMap<>();
        m.put("a", 1);
        m.put("b", 2);
        N.println(m);
        N.println(m.get("a"));

        m.remove("c");
        m.remove("a");
        N.println(m);
        assertTrue(m.containsKey("b"));
        assertTrue(m.containsValue(2));

        N.println(m.keySet());
        N.println(m.values());
        N.println(m.hashCode());
        N.println(m.equals(m));
        N.println(m.toString());
    }

    @Test
    public void test_LinkedHashMap() {
        Map<String, Integer> m = new LinkedHashMap<>();
        m.put("a", 1);
        m.put("b", 2);
        N.println(m);
        N.println(m.get("a"));

        m.remove("c");
        m.remove("a");
        N.println(m);
        assertTrue(m.containsKey("b"));
        assertTrue(m.containsValue(2));

        N.println(m.keySet());
        N.println(m.values());
        N.println(m.hashCode());
        N.println(m.equals(m));
        N.println(m.toString());
    }
}

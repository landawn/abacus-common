package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.AbstractTest;

@Tag("old-test")
public class QuickHashMapTest extends AbstractTest {

    @Test
    public void test_toString() {
        Map<String, Object> m = new ObjectPool<>(16);
        m.put("a", "b");
        m.put("c", "d");
        N.println(m);

        Map<String, Object> m2 = new ObjectPool<>(8);
        m2.put("c", "d");
        m2.put("a", "b");
        N.println(m);

        assertEquals(m, m2);
        m.clear();
        assertTrue(m.isEmpty());
    }
}

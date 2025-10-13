package com.landawn.abacus.util;

import static org.junit.Assert.assertThrows;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import com.landawn.abacus.TestBase;

@Tag("new-test")
public class ImmutableBiMap100Test extends TestBase {

    @Test
    public void testEmpty() {
        ImmutableBiMap<String, Integer> emptyMap = ImmutableBiMap.empty();
        Assertions.assertTrue(emptyMap.isEmpty());
        Assertions.assertEquals(0, emptyMap.size());
        Assertions.assertNull(emptyMap.getByValue(1));
    }

    @Test
    public void testOf_SingleEntry() {
        ImmutableBiMap<String, Integer> map = ImmutableBiMap.of("one", 1);
        Assertions.assertEquals(1, map.size());
        Assertions.assertEquals(1, map.get("one"));
        Assertions.assertEquals("one", map.getByValue(1));
    }

    @Test
    public void testOf_TwoEntries() {
        ImmutableBiMap<String, Integer> map = ImmutableBiMap.of("one", 1, "two", 2);
        Assertions.assertEquals(2, map.size());
        Assertions.assertEquals(1, map.get("one"));
        Assertions.assertEquals(2, map.get("two"));
        Assertions.assertEquals("one", map.getByValue(1));
        Assertions.assertEquals("two", map.getByValue(2));
    }

    @Test
    public void testOf_ThreeEntries() {
        ImmutableBiMap<String, Integer> map = ImmutableBiMap.of("one", 1, "two", 2, "three", 3);
        Assertions.assertEquals(3, map.size());
        Assertions.assertEquals("three", map.getByValue(3));
    }

    @Test
    public void testOf_FourEntries() {
        ImmutableBiMap<String, Integer> map = ImmutableBiMap.of("one", 1, "two", 2, "three", 3, "four", 4);
        Assertions.assertEquals(4, map.size());
        Assertions.assertEquals("four", map.getByValue(4));
    }

    @Test
    public void testOf_FiveEntries() {
        ImmutableBiMap<String, Integer> map = ImmutableBiMap.of("a", 1, "b", 2, "c", 3, "d", 4, "e", 5);
        Assertions.assertEquals(5, map.size());
        Assertions.assertEquals("e", map.getByValue(5));
    }

    @Test
    public void testOf_SixEntries() {
        ImmutableBiMap<String, Integer> map = ImmutableBiMap.of("a", 1, "b", 2, "c", 3, "d", 4, "e", 5, "f", 6);
        Assertions.assertEquals(6, map.size());
        Assertions.assertEquals("f", map.getByValue(6));
    }

    @Test
    public void testOf_SevenEntries() {
        ImmutableBiMap<String, Integer> map = ImmutableBiMap.of("a", 1, "b", 2, "c", 3, "d", 4, "e", 5, "f", 6, "g", 7);
        Assertions.assertEquals(7, map.size());
        Assertions.assertEquals("g", map.getByValue(7));
    }

    @Test
    public void testOf_EightEntries() {
        ImmutableBiMap<String, Integer> map = ImmutableBiMap.of("a", 1, "b", 2, "c", 3, "d", 4, "e", 5, "f", 6, "g", 7, "h", 8);
        Assertions.assertEquals(8, map.size());
        Assertions.assertEquals("h", map.getByValue(8));
    }

    @Test
    public void testOf_NineEntries() {
        ImmutableBiMap<String, Integer> map = ImmutableBiMap.of("a", 1, "b", 2, "c", 3, "d", 4, "e", 5, "f", 6, "g", 7, "h", 8, "i", 9);
        Assertions.assertEquals(9, map.size());
        Assertions.assertEquals("i", map.getByValue(9));
    }

    @Test
    public void testOf_TenEntries() {
        ImmutableBiMap<String, Integer> map = ImmutableBiMap.of("a", 1, "b", 2, "c", 3, "d", 4, "e", 5, "f", 6, "g", 7, "h", 8, "i", 9, "j", 10);
        Assertions.assertEquals(10, map.size());
        Assertions.assertEquals("j", map.getByValue(10));
    }

    @Test
    public void testCopyOf() {
        BiMap<String, Integer> biMap = new BiMap<>();
        biMap.put("one", 1);
        biMap.put("two", 2);

        ImmutableBiMap<String, Integer> immutableBiMap = ImmutableBiMap.copyOf(biMap);
        Assertions.assertEquals(2, immutableBiMap.size());
        Assertions.assertEquals("one", immutableBiMap.getByValue(1));

        biMap.put("three", 3);
        Assertions.assertEquals(2, immutableBiMap.size());
    }

    @Test
    public void testCopyOf_EmptyMap() {
        BiMap<String, Integer> emptyMap = new BiMap<>();
        ImmutableBiMap<String, Integer> immutableBiMap = ImmutableBiMap.copyOf(emptyMap);
        Assertions.assertTrue(immutableBiMap.isEmpty());
    }

    @Test
    public void testCopyOf_NullMap() {
        ImmutableBiMap<String, Integer> immutableBiMap = ImmutableBiMap.copyOf(null);
        Assertions.assertTrue(immutableBiMap.isEmpty());
    }

    @Test
    public void testCopyOf_Map_Deprecated() {
        Assertions.assertThrows(UnsupportedOperationException.class, () -> {
            ImmutableBiMap.copyOf(N.asMap("key", "value"));
        });
    }

    @Test
    public void testWrap() {
        BiMap<String, Integer> biMap = new BiMap<>();
        biMap.put("one", 1);
        biMap.put("two", 2);

        ImmutableBiMap<String, Integer> wrapped = ImmutableBiMap.wrap(biMap);
        Assertions.assertEquals(2, wrapped.size());

        biMap.put("three", 3);
        Assertions.assertEquals(3, wrapped.size());
        Assertions.assertEquals("three", wrapped.getByValue(3));
    }

    @Test
    public void testWrap_NullMap() {
        ImmutableBiMap<String, Integer> wrapped = ImmutableBiMap.wrap(null);
        Assertions.assertTrue(wrapped.isEmpty());
    }

    @Test
    public void testWrap_AlreadyImmutable() {
        ImmutableBiMap<String, Integer> original = ImmutableBiMap.of("one", 1);
        assertThrows(UnsupportedOperationException.class, () -> ImmutableBiMap.wrap(original));
    }

    @Test
    public void testWrap_Map_Deprecated() {
        Assertions.assertThrows(UnsupportedOperationException.class, () -> {
            ImmutableBiMap.wrap(N.asMap("key", "value"));
        });
    }

    @Test
    public void testGetByValue() {
        ImmutableBiMap<String, Integer> map = ImmutableBiMap.of("one", 1, "two", 2, "three", 3);

        Assertions.assertEquals("one", map.getByValue(1));
        Assertions.assertEquals("two", map.getByValue(2));
        Assertions.assertEquals("three", map.getByValue(3));
        Assertions.assertNull(map.getByValue(4));
        Assertions.assertNull(map.getByValue(null));
    }

    @Test
    public void testGetByValue_WithNull() {
        BiMap<String, Integer> biMap = new BiMap<>();
        Assertions.assertThrows(IllegalArgumentException.class, () -> biMap.put("null", null));
    }
}

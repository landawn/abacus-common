package com.landawn.abacus.util;

import java.util.AbstractMap;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import com.landawn.abacus.TestBase;

@Tag("new-test")
public class ImmutableMap100Test extends TestBase {

    @Test
    public void testEmpty() {
        ImmutableMap<String, Integer> emptyMap = ImmutableMap.empty();
        Assertions.assertTrue(emptyMap.isEmpty());
        Assertions.assertEquals(0, emptyMap.size());
        Assertions.assertNull(emptyMap.get("any"));
    }

    @Test
    public void testOf_SingleEntry() {
        ImmutableMap<String, Integer> map = ImmutableMap.of("one", 1);
        Assertions.assertEquals(1, map.size());
        Assertions.assertEquals(1, map.get("one"));
        Assertions.assertNull(map.get("two"));
    }

    @Test
    public void testOf_TwoEntries() {
        ImmutableMap<String, Integer> map = ImmutableMap.of("one", 1, "two", 2);
        Assertions.assertEquals(2, map.size());
        Assertions.assertEquals(1, map.get("one"));
        Assertions.assertEquals(2, map.get("two"));
    }

    @Test
    public void testOf_ThreeEntries() {
        ImmutableMap<String, String> map = ImmutableMap.of("a", "A", "b", "B", "c", "C");
        Assertions.assertEquals(3, map.size());
        Assertions.assertEquals("C", map.get("c"));
    }

    @Test
    public void testOf_FourEntries() {
        ImmutableMap<Integer, String> map = ImmutableMap.of(1, "one", 2, "two", 3, "three", 4, "four");
        Assertions.assertEquals(4, map.size());
        Assertions.assertEquals("four", map.get(4));
    }

    @Test
    public void testOf_FiveEntries() {
        ImmutableMap<String, Integer> map = ImmutableMap.of("a", 1, "b", 2, "c", 3, "d", 4, "e", 5);
        Assertions.assertEquals(5, map.size());
        Assertions.assertEquals(5, map.get("e"));
    }

    @Test
    public void testOf_SixEntries() {
        ImmutableMap<String, Integer> map = ImmutableMap.of("a", 1, "b", 2, "c", 3, "d", 4, "e", 5, "f", 6);
        Assertions.assertEquals(6, map.size());
        Assertions.assertEquals(6, map.get("f"));
    }

    @Test
    public void testOf_SevenEntries() {
        ImmutableMap<String, Integer> map = ImmutableMap.of("a", 1, "b", 2, "c", 3, "d", 4, "e", 5, "f", 6, "g", 7);
        Assertions.assertEquals(7, map.size());
        Assertions.assertEquals(7, map.get("g"));
    }

    @Test
    public void testOf_EightEntries() {
        ImmutableMap<String, Integer> map = ImmutableMap.of("a", 1, "b", 2, "c", 3, "d", 4, "e", 5, "f", 6, "g", 7, "h", 8);
        Assertions.assertEquals(8, map.size());
        Assertions.assertEquals(8, map.get("h"));
    }

    @Test
    public void testOf_NineEntries() {
        ImmutableMap<String, Integer> map = ImmutableMap.of("a", 1, "b", 2, "c", 3, "d", 4, "e", 5, "f", 6, "g", 7, "h", 8, "i", 9);
        Assertions.assertEquals(9, map.size());
        Assertions.assertEquals(9, map.get("i"));
    }

    @Test
    public void testOf_TenEntries() {
        ImmutableMap<String, Integer> map = ImmutableMap.of("a", 1, "b", 2, "c", 3, "d", 4, "e", 5, "f", 6, "g", 7, "h", 8, "i", 9, "j", 10);
        Assertions.assertEquals(10, map.size());
        Assertions.assertEquals(10, map.get("j"));
    }

    @Test
    public void testCopyOf() {
        Map<String, Integer> mutable = new HashMap<>();
        mutable.put("one", 1);
        mutable.put("two", 2);

        ImmutableMap<String, Integer> immutable = ImmutableMap.copyOf(mutable);
        Assertions.assertEquals(2, immutable.size());

        mutable.put("three", 3);
        Assertions.assertEquals(2, immutable.size());
        Assertions.assertNull(immutable.get("three"));
    }

    @Test
    public void testCopyOf_AlreadyImmutable() {
        ImmutableMap<String, Integer> original = ImmutableMap.of("a", 1);
        ImmutableMap<String, Integer> copy = ImmutableMap.copyOf(original);
        Assertions.assertSame(original, copy);
    }

    @Test
    public void testCopyOf_Empty() {
        ImmutableMap<String, Integer> map = ImmutableMap.copyOf(new HashMap<>());
        Assertions.assertTrue(map.isEmpty());
    }

    @Test
    public void testCopyOf_Null() {
        ImmutableMap<String, Integer> map = ImmutableMap.copyOf(null);
        Assertions.assertTrue(map.isEmpty());
    }

    @Test
    public void testCopyOf_PreservesOrder() {
        LinkedHashMap<String, Integer> linked = new LinkedHashMap<>();
        linked.put("first", 1);
        linked.put("second", 2);
        linked.put("third", 3);

        ImmutableMap<String, Integer> map = ImmutableMap.copyOf(linked);
        Iterator<String> keys = map.keySet().iterator();
        Assertions.assertEquals("first", keys.next());
        Assertions.assertEquals("second", keys.next());
        Assertions.assertEquals("third", keys.next());
    }

    @Test
    public void testWrap() {
        Map<String, Integer> mutable = new HashMap<>();
        mutable.put("initial", 1);

        ImmutableMap<String, Integer> wrapped = ImmutableMap.wrap(mutable);
        Assertions.assertEquals(1, wrapped.size());

        mutable.put("added", 2);
        Assertions.assertEquals(2, wrapped.size());
        Assertions.assertEquals(2, wrapped.get("added"));
    }

    @Test
    public void testWrap_AlreadyImmutable() {
        ImmutableMap<String, Integer> original = ImmutableMap.of("a", 1);
        ImmutableMap<String, Integer> wrapped = ImmutableMap.wrap(original);
        Assertions.assertSame(original, wrapped);
    }

    @Test
    public void testWrap_Null() {
        ImmutableMap<String, Integer> wrapped = ImmutableMap.wrap(null);
        Assertions.assertTrue(wrapped.isEmpty());
    }

    @Test
    public void testGetOrDefault() {
        ImmutableMap<String, Integer> map = ImmutableMap.of("a", 1, "b", 2);

        Assertions.assertEquals(1, map.getOrDefault("a", 0));
        Assertions.assertEquals(2, map.getOrDefault("b", 0));
        Assertions.assertEquals(99, map.getOrDefault("c", 99));
        Assertions.assertEquals(100, map.getOrDefault(null, 100));
    }

    @Test
    public void testGetOrDefault_WithNullValue() {
        Map<String, Integer> mapWithNull = new HashMap<>();
        mapWithNull.put("key", null);
        ImmutableMap<String, Integer> map = ImmutableMap.copyOf(mapWithNull);

        Assertions.assertNull(map.getOrDefault("key", 42));
        Assertions.assertEquals(42, map.getOrDefault("missing", 42));
    }

    @Test
    public void testPut_ThrowsUnsupported() {
        ImmutableMap<String, Integer> map = ImmutableMap.of("a", 1);
        Assertions.assertThrows(UnsupportedOperationException.class, () -> map.put("b", 2));
    }

    @Test
    public void testRemove_ThrowsUnsupported() {
        ImmutableMap<String, Integer> map = ImmutableMap.of("a", 1);
        Assertions.assertThrows(UnsupportedOperationException.class, () -> map.remove("a"));
    }

    @Test
    public void testPutAll_ThrowsUnsupported() {
        ImmutableMap<String, Integer> map = ImmutableMap.of("a", 1);
        Map<String, Integer> other = new HashMap<>();
        other.put("b", 2);
        Assertions.assertThrows(UnsupportedOperationException.class, () -> map.putAll(other));
    }

    @Test
    public void testPutIfAbsent_ThrowsUnsupported() {
        ImmutableMap<String, Integer> map = ImmutableMap.of("a", 1);
        Assertions.assertThrows(UnsupportedOperationException.class, () -> map.putIfAbsent("b", 2));
    }

    @Test
    public void testRemove_KeyValue_ThrowsUnsupported() {
        ImmutableMap<String, Integer> map = ImmutableMap.of("a", 1);
        Assertions.assertThrows(UnsupportedOperationException.class, () -> map.remove("a", 1));
    }

    @Test
    public void testReplace_KeyOldNew_ThrowsUnsupported() {
        ImmutableMap<String, Integer> map = ImmutableMap.of("a", 1);
        Assertions.assertThrows(UnsupportedOperationException.class, () -> map.replace("a", 1, 2));
    }

    @Test
    public void testReplace_KeyValue_ThrowsUnsupported() {
        ImmutableMap<String, Integer> map = ImmutableMap.of("a", 1);
        Assertions.assertThrows(UnsupportedOperationException.class, () -> map.replace("a", 2));
    }

    @Test
    public void testComputeIfAbsent_ThrowsUnsupported() {
        ImmutableMap<String, Integer> map = ImmutableMap.of("a", 1);
        Assertions.assertThrows(UnsupportedOperationException.class, () -> map.computeIfAbsent("b", k -> 2));
    }

    @Test
    public void testComputeIfPresent_ThrowsUnsupported() {
        ImmutableMap<String, Integer> map = ImmutableMap.of("a", 1);
        Assertions.assertThrows(UnsupportedOperationException.class, () -> map.computeIfPresent("a", (k, v) -> v + 1));
    }

    @Test
    public void testCompute_ThrowsUnsupported() {
        ImmutableMap<String, Integer> map = ImmutableMap.of("a", 1);
        Assertions.assertThrows(UnsupportedOperationException.class, () -> map.compute("a", (k, v) -> 2));
    }

    @Test
    public void testMerge_ThrowsUnsupported() {
        ImmutableMap<String, Integer> map = ImmutableMap.of("a", 1);
        Assertions.assertThrows(UnsupportedOperationException.class, () -> map.merge("a", 2, (v1, v2) -> v1 + v2));
    }

    @Test
    public void testClear_ThrowsUnsupported() {
        ImmutableMap<String, Integer> map = ImmutableMap.of("a", 1);
        Assertions.assertThrows(UnsupportedOperationException.class, () -> map.clear());
    }

    @Test
    public void testIsEmpty() {
        Assertions.assertTrue(ImmutableMap.empty().isEmpty());
        Assertions.assertFalse(ImmutableMap.of("a", 1).isEmpty());
    }

    @Test
    public void testContainsKey() {
        ImmutableMap<String, Integer> map = ImmutableMap.of("a", 1, "b", 2);

        Assertions.assertTrue(map.containsKey("a"));
        Assertions.assertTrue(map.containsKey("b"));
        Assertions.assertFalse(map.containsKey("c"));
        Assertions.assertFalse(map.containsKey(null));
    }

    @Test
    public void testContainsValue() {
        ImmutableMap<String, Integer> map = ImmutableMap.of("a", 1, "b", 2, "c", 1);

        Assertions.assertTrue(map.containsValue(1));
        Assertions.assertTrue(map.containsValue(2));
        Assertions.assertFalse(map.containsValue(3));
        Assertions.assertFalse(map.containsValue(null));
    }

    @Test
    public void testGet() {
        ImmutableMap<String, Integer> map = ImmutableMap.of("a", 1, "b", 2);

        Assertions.assertEquals(1, map.get("a"));
        Assertions.assertEquals(2, map.get("b"));
        Assertions.assertNull(map.get("c"));
        Assertions.assertNull(map.get(null));
    }

    @Test
    public void testKeySet() {
        ImmutableMap<String, Integer> map = ImmutableMap.of("a", 1, "b", 2, "c", 3);
        Set<String> keys = map.keySet();

        Assertions.assertEquals(3, keys.size());
        Assertions.assertTrue(keys.contains("a"));
        Assertions.assertTrue(keys.contains("b"));
        Assertions.assertTrue(keys.contains("c"));

        Assertions.assertThrows(UnsupportedOperationException.class, () -> keys.add("d"));
        Assertions.assertThrows(UnsupportedOperationException.class, () -> keys.remove("a"));
    }

    @Test
    public void testValues() {
        ImmutableMap<String, Integer> map = ImmutableMap.of("a", 1, "b", 2, "c", 1);
        Collection<Integer> values = map.values();

        Assertions.assertEquals(3, values.size());
        Assertions.assertTrue(values.contains(1));
        Assertions.assertTrue(values.contains(2));

        Assertions.assertThrows(UnsupportedOperationException.class, () -> values.add(3));
        Assertions.assertThrows(UnsupportedOperationException.class, () -> values.remove(1));
    }

    @Test
    public void testEntrySet() {
        ImmutableMap<String, Integer> map = ImmutableMap.of("a", 1, "b", 2);
        Set<Map.Entry<String, Integer>> entries = map.entrySet();

        Assertions.assertEquals(2, entries.size());

        boolean foundA = false, foundB = false;
        for (Map.Entry<String, Integer> entry : entries) {
            if ("a".equals(entry.getKey()) && 1 == entry.getValue()) {
                foundA = true;
            } else if ("b".equals(entry.getKey()) && 2 == entry.getValue()) {
                foundB = true;
            }
        }
        Assertions.assertTrue(foundA);
        Assertions.assertTrue(foundB);

        Map.Entry<String, Integer> newEntry = new AbstractMap.SimpleEntry<>("c", 3);
        Assertions.assertThrows(UnsupportedOperationException.class, () -> entries.add(newEntry));
    }

    @Test
    public void testSize() {
        Assertions.assertEquals(0, ImmutableMap.empty().size());
        Assertions.assertEquals(1, ImmutableMap.of("a", 1).size());
        Assertions.assertEquals(3, ImmutableMap.of("a", 1, "b", 2, "c", 3).size());
    }

    @Test
    public void testBuilder() {
        ImmutableMap<String, Integer> map = ImmutableMap.<String, Integer> builder().put("one", 1).put("two", 2).putAll(N.asMap("three", 3, "four", 4)).build();

        Assertions.assertEquals(4, map.size());
        Assertions.assertEquals(1, map.get("one"));
        Assertions.assertEquals(4, map.get("four"));
    }

    @Test
    public void testBuilder_EmptyPutAll() {
        ImmutableMap<String, Integer> map = ImmutableMap.<String, Integer> builder().put("a", 1).putAll(null).putAll(new HashMap<>()).build();

        Assertions.assertEquals(1, map.size());
    }

    @Test
    public void testBuilder_WithBackingMap() {
        Map<String, Integer> backing = new LinkedHashMap<>();
        ImmutableMap<String, Integer> map = ImmutableMap.builder(backing).put("a", 1).put("b", 2).build();

        Assertions.assertEquals(2, map.size());
        Assertions.assertEquals(2, backing.size());
    }

    @Test
    public void testBuilder_NullBackingMap() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> ImmutableMap.builder(null));
    }

    @Test
    public void testWithNullKeyValue() {
        Map<String, String> mapWithNulls = new HashMap<>();
        mapWithNulls.put(null, "nullKey");
        mapWithNulls.put("nullValue", null);
        mapWithNulls.put("normal", "value");

        ImmutableMap<String, String> map = ImmutableMap.copyOf(mapWithNulls);

        Assertions.assertEquals(3, map.size());
        Assertions.assertEquals("nullKey", map.get(null));
        Assertions.assertNull(map.get("nullValue"));
        Assertions.assertEquals("value", map.get("normal"));
        Assertions.assertTrue(map.containsKey(null));
        Assertions.assertTrue(map.containsValue(null));
    }
}

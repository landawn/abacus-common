package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;
import java.util.function.Function;

import org.junit.jupiter.api.Test;

public class MapsReplaceTest extends MapsTestSupport {
    @Test
    public void testReplace_withOldValue() {
        Map<String, Integer> map = new HashMap<>(Map.of("a", 1));
        assertTrue(Maps.replace(map, "a", 1, 10));
        assertEquals(10, map.get("a"));
        assertFalse(Maps.replace(map, "a", 1, 20));
        assertFalse(Maps.replace(map, "b", 1, 20));
    }

    @Test
    public void testReplace_withOldValue_mismatch() {
        Map<String, String> map = new HashMap<>(testMap);
        assertFalse(Maps.replace(map, "key1", "wrong", "newVal"));
        assertEquals("value1", map.get("key1"));
    }

    @Test
    public void testReplaceWithOldValue() {
        Map<String, String> map = new HashMap<>(testMap);

        assertTrue(Maps.replace(map, "key1", "value1", "newValue1"));
        assertEquals("newValue1", map.get("key1"));

        assertFalse(Maps.replace(map, "key2", "wrongOldValue", "newValue2"));
        assertEquals("value2", map.get("key2"));

        assertFalse(Maps.replace(map, "missing", "oldValue", "newValue"));

        assertFalse(Maps.replace(new HashMap<>(), "key", "old", "new"));
    }

    @Test
    public void testReplace() {
        Map<String, String> map = new HashMap<>(testMap);

        assertEquals("value1", Maps.replace(map, "key1", "newValue1"));
        assertEquals("newValue1", map.get("key1"));

        assertNull(Maps.replace(map, "missing", "newValue"));
        assertFalse(map.containsKey("missing"));

        map.put("nullKey", null);
        assertNull(Maps.replace(map, "nullKey", "newValue"));
        assertEquals("newValue", map.get("nullKey"));
    }

    @Test
    public void testReplace_newValueOnly() {
        Map<String, Integer> map = new HashMap<>(Map.of("a", 1));
        map.put("b", null);
        assertEquals(Integer.valueOf(1), Maps.replace(map, "a", 10));
        assertEquals(10, map.get("a"));
        assertNull(Maps.replace(map, "b", 20));
        assertEquals(20, map.get("b"));
        assertNull(Maps.replace(map, "c", 30));
        assertNull(map.get("c"));
    }

    @Test
    public void testReplace_keyNotPresent() {
        Map<String, String> map = new HashMap<>(testMap);
        assertNull(Maps.replace(map, "missing", "newVal"));
    }

    @Test
    public void testReplaceKeys_NullValueMerged_NonNullResult() {
        Map<String, Integer> map = new HashMap<>();
        map.put("a", null); // null value
        map.put("b", 10);
        // "a" -> "b" conversion means null value collides with existing key "b"
        Maps.replaceKeys(map, k -> k.equals("a") ? "b" : k, (existing, incoming) -> existing == null ? incoming : existing + (incoming == null ? 0 : incoming));
        assertEquals(Integer.valueOf(10), map.get("b"));
        assertFalse(map.containsKey("a"));
    }

    @Test
    public void testReplaceKeys_NullValueMerged_NullResult_RemovesKey() {
        Map<String, Integer> map = new HashMap<>();
        map.put("a", null); // null value
        map.put("b", 5);
        // merger returns null -> should remove "b"
        Maps.replaceKeys(map, k -> k.equals("a") ? "b" : k, (existing, incoming) -> null);
        assertFalse(map.containsKey("b"));
        assertFalse(map.containsKey("a"));
    }

    @Test
    public void testReplaceKeys_NullValue_NoCollision_PutsNull() {
        Map<String, Integer> map = new HashMap<>();
        map.put("oldKey", null); // null value, no collision
        Maps.replaceKeys(map, k -> "newKey", (existing, incoming) -> existing);
        assertTrue(map.containsKey("newKey"));
        assertNull(map.get("newKey"));
        assertFalse(map.containsKey("oldKey"));
    }

    @Test
    public void testReplace_EmptyMap_ReturnsNull() {
        assertNull(Maps.replace(new HashMap<String, String>(), "key", "newValue"));
    }

    @Test
    public void testReplaceAll() {
        Map<String, String> map = new HashMap<>(testMap);

        Maps.replaceAll(map, (key, value) -> key + "-" + value);
        assertEquals("key1-value1", map.get("key1"));
        assertEquals("key2-value2", map.get("key2"));
        assertEquals("key3-value3", map.get("key3"));

        Map<String, String> emptyMap = new HashMap<>();
        Maps.replaceAll(emptyMap, (k, v) -> "new");
        assertTrue(emptyMap.isEmpty());
    }

    @Test
    public void testReplaceAll_function() {
        Map<String, String> map = new HashMap<>(testMap);
        Maps.replaceAll(map, (k, v) -> v.toUpperCase());
        assertEquals("VALUE1", map.get("key1"));
        assertEquals("VALUE2", map.get("key2"));
    }

    @Test
    public void testReplaceKeys() {
        Map<String, String> map = new HashMap<>();
        map.put("oldKey1", "value1");
        map.put("oldKey2", "value2");

        Maps.replaceKeys(map, key -> key.replace("old", "new"));

        assertEquals(2, map.size());
        assertEquals("value1", map.get("newKey1"));
        assertEquals("value2", map.get("newKey2"));
        assertFalse(map.containsKey("oldKey1"));
        assertFalse(map.containsKey("oldKey2"));
    }

    @Test
    public void testReplaceKeysWithMerger() {
        Map<String, Integer> map = new HashMap<>();
        map.put("a1", 10);
        map.put("a2", 20);
        map.put("b", 30);

        Maps.replaceKeys(map, key -> key.startsWith("a") ? "a" : key, Integer::sum);

        assertEquals(2, map.size());
        assertEquals(Integer.valueOf(30), map.get("a"));
        assertEquals(Integer.valueOf(30), map.get("b"));
    }

    @Test
    public void testReplaceKeys_simple() {
        Map<String, Integer> map = new HashMap<>(Map.of("keyOne", 1, "keyTwo", 2));
        Maps.replaceKeys(map, k -> k.replace("key", "k"));
        assertEquals(Map.of("kOne", 1, "kTwo", 2), map);
    }

    @Test
    public void testReplaceKeys_withMerge() {
        Map<String, Integer> map = new HashMap<>();
        map.put("keyA", 1);
        map.put("keyB", 2);
        map.put("oldC", 3);

        Function<String, String> keyConverter = k -> {
            if (k.equals("keyB") || k.equals("oldC")) {
                return "newKey";
            }
            return k;
        };
        BiFunction<Integer, Integer, Integer> merger = Integer::sum;

        Maps.replaceKeys(map, keyConverter, merger);

        assertEquals(Map.of("keyA", 1, "newKey", 2 + 3), map);
    }

    @Test
    public void testReplaceKeys_WithMerger() {
        Map<String, Integer> map = new LinkedHashMap<>();
        map.put("aaa", 1);
        map.put("bbb", 2);
        map.put("ccc", 3);

        Maps.replaceKeys(map, key -> key.substring(0, 1), Integer::sum);
        assertEquals(3, map.size());
        assertEquals(Integer.valueOf(1), map.get("a"));
        assertEquals(Integer.valueOf(2), map.get("b"));
        assertEquals(Integer.valueOf(3), map.get("c"));
    }

    @Test
    public void testReplaceKeys_emptyMap() {
        Map<String, Integer> map = new HashMap<>();
        Maps.replaceKeys(map, String::toUpperCase);
        assertTrue(map.isEmpty());
    }

    @Test
    public void testReplaceKeys_withMerger_emptyMap() {
        Map<String, Integer> map = new HashMap<>();
        Maps.replaceKeys(map, String::toUpperCase, Integer::sum);
        assertTrue(map.isEmpty());
    }

    @Test
    public void testReplaceKeys_nullMap() {
        assertDoesNotThrow(() -> {
            Maps.replaceKeys((Map<String, Integer>) null, String::toUpperCase);
            // no exception should be thrown
        });
    }

    @Test
    public void testReplaceKeys_withMerger_nullMap() {
        assertDoesNotThrow(() -> {
            Maps.replaceKeys((Map<String, Integer>) null, String::toUpperCase, Integer::sum);
            // no exception should be thrown
        });
    }

    @Test
    public void testReplaceKeys_DuplicateConvertedKeyThrowsIllegalStateException() {
        final Map<String, Integer> map = new LinkedHashMap<>();
        map.put("left", 1);
        map.put("right", 2);

        assertThrows(IllegalStateException.class, () -> Maps.replaceKeys(map, key -> "dup"));
    }

    @Test
    public void testReplaceKeysDuplicateDoesNotRepeatConverter() {
        for (final Map<String, Integer> map : Arrays.<Map<String, Integer>> asList(new LinkedHashMap<>(), new TreeMap<>(String.CASE_INSENSITIVE_ORDER),
                new IdentityHashMap<>(), new CaseInsensitiveIntegerMap())) {
            map.put("first", 1);
            map.put("second", 2);
            final List<String> originalKeys = new ArrayList<>(map.keySet());
            final List<String> converted = new ArrayList<>();

            final IllegalStateException exception = assertThrows(IllegalStateException.class, () -> Maps.replaceKeys(map, key -> {
                if (converted.contains(key)) {
                    throw new AssertionError("Converter called twice for " + key);
                }
                converted.add(key);
                return (map instanceof TreeMap || map instanceof CaseInsensitiveIntegerMap) && key.equals("second") ? "DUPLICATE" : "duplicate";
            }));

            assertEquals(originalKeys, converted);
            assertTrue(exception.getMessage().contains("'first'"), exception.getMessage());
            assertTrue(exception.getMessage().contains("'second'"), exception.getMessage());
            assertEquals(originalKeys, new ArrayList<>(map.keySet()));
            assertEquals(2, map.size());
            assertEquals(Integer.valueOf(1), map.get("first"));
            assertEquals(Integer.valueOf(2), map.get("second"));
        }

        final Map<String, Integer> identityMap = new IdentityHashMap<>();
        identityMap.put("first", 1);
        identityMap.put("second", 2);
        final String firstTarget = new String("equal");
        final String secondTarget = new String("equal");
        final List<String> converted = new ArrayList<>();
        Maps.replaceKeys(identityMap, key -> {
            assertFalse(converted.contains(key), "Converter must be called once per source key");
            converted.add(key);
            return key.equals("first") ? firstTarget : secondTarget;
        });
        assertEquals(2, converted.size());
        assertEquals(2, identityMap.size());
        assertEquals(Integer.valueOf(1), identityMap.get(firstTarget));
        assertEquals(Integer.valueOf(2), identityMap.get(secondTarget));
    }

    @Test
    public void testReplaceKeysRejectedKeyDoesNotMutateMap() {
        final Map<String, Integer> map = new ConcurrentHashMap<>();
        map.put("a", 1);

        assertThrows(NullPointerException.class, () -> Maps.replaceKeys(map, key -> null));
        assertEquals(Map.of("a", 1), map);
    }

    @Test
    public void testReplaceKeysWithMergerHandlesChainedRenames() {
        // regression: iterating a key snapshot while renaming let later iterations see earlier
        // renames - chained renames merged spuriously and results were iteration-order dependent
        final Map<String, Integer> map = new LinkedHashMap<>();
        map.put("a", 1);
        map.put("b", 2);
        Maps.replaceKeys(map, k -> k.equals("a") ? "b" : "c", Integer::sum);
        assertEquals(CommonUtil.asMap("b", 1, "c", 2), map); // distinct targets: no merge at all

        final Map<String, Integer> swapped = new LinkedHashMap<>();
        swapped.put("a", 1);
        swapped.put("b", 2);
        Maps.replaceKeys(swapped, k -> k.equals("a") ? "b" : "a", Integer::sum);
        assertEquals(CommonUtil.asMap("b", 1, "a", 2), swapped); // clean swap

        final Map<String, Integer> dup = new LinkedHashMap<>();
        dup.put("a", 1);
        dup.put("b", 2);
        Maps.replaceKeys(dup, k -> "x", Integer::sum);
        assertEquals(CommonUtil.asMap("x", 3), dup); // true duplicate targets still merge
    }
}

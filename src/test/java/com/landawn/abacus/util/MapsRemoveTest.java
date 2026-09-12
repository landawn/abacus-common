package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiPredicate;
import java.util.function.Predicate;

import org.junit.jupiter.api.Test;

public class MapsRemoveTest extends MapsTestSupport {
    @Test
    public void testRemove_entry() {
        Map<String, Integer> map = new HashMap<>(Map.of("a", 1, "b", 2));
        assertTrue(Maps.removeEntry(map, CommonUtil.newEntry("a", 1)));
        assertEquals(1, map.size());
        assertFalse(Maps.removeEntry(map, CommonUtil.newEntry("b", 3)));
    }

    @Test
    public void testRemove_keyValue() {
        Map<String, Integer> map = new HashMap<>(Map.of("a", 1, "b", 2));
        assertTrue(Maps.removeEntry(map, "a", 1));
        assertFalse(Maps.removeEntry(map, "b", 3));
        assertFalse(Maps.removeEntry(map, "c", 2));
    }

    @Test
    public void testRemoveEntry() {
        Map<String, String> map = new HashMap<>(testMap);

        Map.Entry<String, String> entry = CommonUtil.newEntry("key1", "value1");
        assertTrue(Maps.removeEntry(map, entry));
        assertFalse(map.containsKey("key1"));

        Map.Entry<String, String> missing = CommonUtil.newEntry("missing", "value");
        assertFalse(Maps.removeEntry(map, missing));

        Map.Entry<String, String> wrongValue = CommonUtil.newEntry("key2", "wrongValue");
        assertFalse(Maps.removeEntry(map, wrongValue));
        assertTrue(map.containsKey("key2"));
    }

    @Test
    public void testRemoveKeyValue() {
        Map<String, String> map = new HashMap<>(testMap);

        assertTrue(Maps.removeEntry(map, "key1", "value1"));
        assertFalse(map.containsKey("key1"));

        assertFalse(Maps.removeEntry(map, "key2", "wrongValue"));
        assertTrue(map.containsKey("key2"));

        assertFalse(Maps.removeEntry(map, "missing", "value"));

        assertFalse(Maps.removeEntry(null, "key", "value"));
        assertFalse(Maps.removeEntry(new HashMap<>(), "key", "value"));
    }

    @Test
    public void testRemoveEntries() {
        Map<String, String> map = new HashMap<>(testMap);
        Map<String, String> entriesToRemove = new HashMap<>();
        entriesToRemove.put("key1", "value1");
        entriesToRemove.put("key2", "wrongValue");
        entriesToRemove.put("key3", "value3");

        assertTrue(Maps.removeEntries(map, entriesToRemove));
        assertEquals(1, map.size());
        assertTrue(map.containsKey("key2"));
    }

    @Test
    public void testRemoveEntries_emptyEntries() {
        Map<String, String> map = new HashMap<>(testMap);
        assertFalse(Maps.removeEntries(map, new HashMap<>()));
    }

    @Test
    public void testRemoveEntries_nullMap() {
        assertFalse(Maps.removeEntries(null, testMap));
    }

    @Test
    public void testRemoveEntriesAcceptsSameMapAsRemovalSource() {
        Map<String, String> map = new LinkedHashMap<>(testMap);

        assertTrue(Maps.removeEntries(map, map));
        assertTrue(map.isEmpty());
    }

    @Test
    public void testRemoveKeys() {
        Map<String, String> map = new HashMap<>(testMap);
        List<String> keysToRemove = Arrays.asList("key1", "key3", "missing");

        assertTrue(Maps.removeKeys(map, keysToRemove));
        assertEquals(1, map.size());
        assertTrue(map.containsKey("key2"));

        assertFalse(Maps.removeKeys(map, new ArrayList<>()));
        assertFalse(Maps.removeKeys(new HashMap<>(), keysToRemove));
    }

    @Test
    public void testRemoveKeys_emptyKeys() {
        Map<String, String> map = new HashMap<>(testMap);
        assertFalse(Maps.removeKeys(map, new ArrayList<>()));
    }

    @Test
    public void testRemoveKeys_nullMap() {
        assertFalse(Maps.removeKeys(null, Arrays.asList("key1")));
    }

    @Test
    public void testRemoveKeysAcceptsLiveKeySetView() {
        Map<String, String> map = new LinkedHashMap<>(testMap);

        assertTrue(Maps.removeKeys(map, map.keySet()));
        assertTrue(map.isEmpty());
    }

    @Test
    public void testRemoveIfBiPredicate() {
        Map<String, String> map = new HashMap<>(testMap);

        boolean removed = Maps.removeIf(map, (key, value) -> key.equals("key1") || value.equals("value3"));
        assertTrue(removed);
        assertEquals(1, map.size());
        assertTrue(map.containsKey("key2"));
    }

    @Test
    public void testRemoveIf_entry_noMatch() {
        Map<String, String> map = new HashMap<>(testMap);
        assertFalse(Maps.removeIf(map, e -> false));
    }

    @Test
    public void testRemoveIf_biPred_noMatch() {
        Map<String, String> map = new HashMap<>(testMap);
        assertFalse(Maps.removeIf(map, (k, v) -> false));
    }

    @Test
    public void testRemoveIf_EntryPredicate() {
        Map<String, Integer> map = new LinkedHashMap<>();
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3);

        boolean changed = Maps.removeIf(map, (Map.Entry<String, Integer> e) -> e.getValue() > 1);
        assertTrue(changed);
        assertEquals(1, map.size());
        assertTrue(map.containsKey("a"));
    }

    @Test
    public void testRemoveIf_NothingRemoved() {
        Map<String, Integer> map = new LinkedHashMap<>();
        map.put("a", 1);

        boolean changed = Maps.removeIf(map, (Map.Entry<String, Integer> e) -> e.getValue() > 100);
        assertFalse(changed);
        assertEquals(1, map.size());
    }

    @Test
    public void testRemoveIf() {
        Map<String, String> map = new HashMap<>(testMap);

        boolean removed = Maps.removeIf(map, entry -> entry.getValue().endsWith("1"));
        assertTrue(removed);
        assertEquals(2, map.size());
        assertFalse(map.containsKey("key1"));

        assertFalse(Maps.removeIf(map, entry -> entry.getKey().equals("missing")));

        assertFalse(Maps.removeIf(new HashMap<>(), entry -> true));
    }

    @Test
    public void testRemoveIf_BiPredicate_EmptyMap_ReturnsFalse() {
        assertFalse(Maps.removeIf(new HashMap<String, Integer>(), (k, v) -> true));
    }

    @Test
    public void testRemoveIfKey_EmptyMap_ReturnsFalse() {
        assertFalse(Maps.removeIfKey(new HashMap<String, Integer>(), k -> true));
    }

    @Test
    public void testRemoveIfValue_EmptyMap_ReturnsFalse() {
        assertFalse(Maps.removeIfValue(new HashMap<String, Integer>(), v -> true));
    }

    @Test
    public void testRemoveIf_entryPredicate() {
        Map<String, Integer> map = new HashMap<>(Map.of("a", 1, "b", 20, "c", 3));
        assertTrue(Maps.removeIf(map, entry -> entry.getValue() > 10));
        assertEquals(Map.of("a", 1, "c", 3), map);
        assertFalse(Maps.removeIf(map, entry -> entry.getValue() > 100));
        assertThrows(IllegalArgumentException.class, () -> Maps.removeIf(map, (Predicate<Map.Entry<String, Integer>>) null));
    }

    @Test
    public void testRemoveIf_biPredicate() {
        Map<String, Integer> map = new HashMap<>(Map.of("a", 1, "b", 20, "c", 3));
        assertTrue(Maps.removeIf(map, (k, v) -> k.equals("b")));
        assertEquals(Map.of("a", 1, "c", 3), map);
        assertThrows(IllegalArgumentException.class, () -> Maps.removeIf(map, (BiPredicate<String, Integer>) null));
    }

    @Test
    public void testRemoveIfKey() {
        Map<String, String> map = new HashMap<>(testMap);

        boolean removed = Maps.removeIfKey(map, key -> key.startsWith("key1"));
        assertTrue(removed);
        assertEquals(2, map.size());
        assertFalse(map.containsKey("key1"));
    }

    @Test
    public void testRemoveIfKey_noMatch() {
        Map<String, String> map = new HashMap<>(testMap);
        assertFalse(Maps.removeIfKey(map, k -> false));
    }

    @Test
    public void testRemoveIfValue() {
        Map<String, String> map = new HashMap<>(testMap);

        boolean removed = Maps.removeIfValue(map, value -> value.contains("2"));
        assertTrue(removed);
        assertEquals(2, map.size());
        assertFalse(map.containsKey("key2"));
    }

    @Test
    public void testRemoveIfValue_noMatch() {
        Map<String, String> map = new HashMap<>(testMap);
        assertFalse(Maps.removeIfValue(map, v -> false));
    }

    @Test
    public void testRemoveEntry_nullEntry_returnsFalse() {
        final Map<String, Integer> map = new HashMap<>(Map.of("a", 1, "b", 2));
        final Map.Entry<String, Integer> nullEntry = null;

        assertFalse(Maps.removeEntry(map, nullEntry));
        assertEquals(2, map.size());

        // the query/mutate idiom is now safe for a null entry on both sides
        assertFalse(Maps.containsEntry(map, nullEntry));
        if (Maps.containsEntry(map, nullEntry)) {
            Maps.removeEntry(map, nullEntry);
        }
        assertEquals(2, map.size());

        // a null map combined with a non-null entry is also tolerated
        assertFalse(Maps.removeEntry(null, CommonUtil.newEntry("a", 1)));
    }
}

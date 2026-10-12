package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

public class MapsTest extends MapsTestSupport {

    // containsEntry

    @Test
    public void testContains_entry() {
        Map<String, Integer> map = Map.of("a", 1, "b", 2);
        map = new HashMap<>(map);
        map.put("c", null);
        assertTrue(Maps.containsEntry(map, CommonUtil.newEntry("a", 1)));
        assertFalse(Maps.containsEntry(map, CommonUtil.newEntry("a", 2)));
        assertTrue(Maps.containsEntry(map, CommonUtil.newEntry("c", null)));
        assertFalse(Maps.containsEntry(map, CommonUtil.newEntry("d", null)));
    }

    @Test
    public void testContains_keyValue() {
        Map<String, Integer> map = Map.of("a", 1, "b", 2);
        map = new HashMap<>(map);
        map.put("c", null);
        assertTrue(Maps.containsEntry(map, "a", 1));
        assertFalse(Maps.containsEntry(map, "a", 2));
        assertTrue(Maps.containsEntry(map, "c", null));
        assertFalse(Maps.containsEntry(map, "d", null));
    }

    @Test
    public void testContainsEntry() {
        Map.Entry<String, String> entry = CommonUtil.newEntry("key1", "value1");
        assertTrue(Maps.containsEntry(testMap, entry));

        Map.Entry<String, String> wrongValue = CommonUtil.newEntry("key1", "wrongValue");
        assertFalse(Maps.containsEntry(testMap, wrongValue));

        Map.Entry<String, String> missing = CommonUtil.newEntry("missing", "value");
        assertFalse(Maps.containsEntry(testMap, missing));
    }

    @Test
    public void testContainsEntry_emptyMap() {
        assertFalse(Maps.containsEntry(new HashMap<>(), CommonUtil.newEntry("a", "b")));
    }

    @Test
    public void testContainsEntry_nullMap() {
        assertFalse(Maps.containsEntry((Map<String, String>) null, CommonUtil.newEntry("a", "b")));
    }

    @Test
    public void testContainsEntry_WithMapEntry() {
        Map<String, Integer> map = new HashMap<>();
        map.put("a", 1);

        Map.Entry<String, Integer> entry = Maps.newEntry("a", 1);
        assertTrue(Maps.containsEntry(map, entry));

        Map.Entry<String, Integer> wrongEntry = Maps.newEntry("a", 2);
        assertFalse(Maps.containsEntry(map, wrongEntry));
    }

    @Test
    public void testContainsKeyValue() {
        assertTrue(Maps.containsEntry(testMap, "key1", "value1"));
        assertFalse(Maps.containsEntry(testMap, "key1", "wrongValue"));
        assertFalse(Maps.containsEntry(testMap, "missing", "value1"));

        testMap.put("nullKey", null);
        assertTrue(Maps.containsEntry(testMap, "nullKey", null));

        assertFalse(Maps.containsEntry(new HashMap<>(), "key", "value"));
        assertFalse(Maps.containsEntry(null, "key", "value"));
    }

    @Test
    public void testEntrySet() {
        Set<Map.Entry<String, String>> entries = Maps.entrySet(testMap);
        assertEquals(3, entries.size());

        assertTrue(Maps.entrySet(null).isEmpty());
        assertTrue(Maps.entrySet(new HashMap<>()).isEmpty());
        assertNotNull(Maps.entrySet(null));
    }

    @Test
    public void testKeys() {
        Set<String> keys = Maps.keySet(testMap);
        assertEquals(3, keys.size());
        assertTrue(keys.contains("key1"));
        assertTrue(keys.contains("key2"));
        assertTrue(keys.contains("key3"));

        assertTrue(Maps.keySet(null).isEmpty());
        assertTrue(Maps.keySet(new HashMap<>()).isEmpty());
        assertNotNull(Maps.keySet(null));
    }

    @Test
    public void testKeySetValuesEntrySet_alwaysUnmodifiable() {
        final Map<String, Integer> map = new LinkedHashMap<>();
        map.put("a", 1);
        map.put("b", 2);

        final Set<String> keys = Maps.keySet(map);
        final Collection<Integer> values = Maps.values(map);
        final Set<Map.Entry<String, Integer>> entries = Maps.entrySet(map);

        assertThrows(UnsupportedOperationException.class, () -> keys.remove("a"));
        assertThrows(UnsupportedOperationException.class, values::clear);
        assertThrows(UnsupportedOperationException.class, entries::clear);

        // empty/null inputs also return unmodifiable (empty) views
        assertThrows(UnsupportedOperationException.class, () -> Maps.keySet(null).add("x"));
        assertThrows(UnsupportedOperationException.class, () -> Maps.values(new HashMap<String, Integer>()).add(1));
        assertThrows(UnsupportedOperationException.class, () -> Maps.entrySet(new HashMap<String, Integer>()).clear());

        // the non-empty result is a live read-through view of the backing map
        map.put("c", 3);
        assertTrue(keys.contains("c"));
        assertEquals(3, keys.size());
        assertEquals(3, values.size());
        assertEquals(3, entries.size());
    }

    @Test
    public void testNewEntry() {
        Map.Entry<String, Integer> entry = Maps.newEntry("key", 100);
        assertEquals("key", entry.getKey());
        assertEquals(Integer.valueOf(100), entry.getValue());

        entry.setValue(200);
        assertEquals(Integer.valueOf(200), entry.getValue());
    }

    @Test
    public void testNewEntry_values() {
        Map.Entry<String, Integer> entry = Maps.newEntry("key", 123);
        assertEquals("key", entry.getKey());
        assertEquals(123, entry.getValue().intValue());

        Map.Entry<String, String> nullEntry = Maps.newEntry(null, null);
        assertNull(nullEntry.getKey());
        assertNull(nullEntry.getValue());
    }

    @Test
    public void testNewImmutableEntry() {
        ImmutableEntry<String, Integer> entry = Maps.newImmutableEntry("key", 100);
        assertEquals("key", entry.getKey());
        assertEquals(Integer.valueOf(100), entry.getValue());

        assertThrows(UnsupportedOperationException.class, () -> entry.setValue(200));
    }

    @Test
    public void testNewImmutableEntry_values() {
        ImmutableEntry<String, Integer> entry = Maps.newImmutableEntry("key", 123);
        assertEquals("key", entry.getKey());
        assertEquals(123, entry.getValue().intValue());
    }

    @Test
    public void testValues() {
        Collection<String> values = Maps.values(testMap);
        assertEquals(3, values.size());
        assertTrue(values.contains("value1"));
        assertTrue(values.contains("value2"));
        assertTrue(values.contains("value3"));

        assertTrue(Maps.values(null).isEmpty());
        assertTrue(Maps.values(new HashMap<>()).isEmpty());
        assertNotNull(Maps.values(null));
    }
}

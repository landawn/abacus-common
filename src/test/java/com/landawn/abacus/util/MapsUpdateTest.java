package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.Predicate;
import java.util.function.Supplier;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class MapsUpdateTest extends MapsTestSupport {

    @Test
    public void testMerge() {
        final Map<String, Integer> counts = new HashMap<>();

        // absent key -> stored directly; mergeFunction not invoked
        final int[] calls = { 0 };
        final BiFunction<Integer, Integer, Integer> sum = (a, b) -> {
            calls[0]++;
            return a + b;
        };
        assertEquals(Integer.valueOf(1), Maps.merge(counts, "a", 1, sum));
        assertEquals(0, calls[0]);
        assertEquals(Integer.valueOf(1), counts.get("a"));

        // present key -> mergeFunction combines existing and new value
        assertEquals(Integer.valueOf(6), Maps.merge(counts, "a", 5, sum));
        assertEquals(1, calls[0]);
        assertEquals(Integer.valueOf(6), counts.get("a"));

        // key mapped to null is treated as absent -> stored directly, mergeFunction not invoked
        counts.put("n", null);
        assertEquals(Integer.valueOf(9), Maps.merge(counts, "n", 9, sum));
        assertEquals(1, calls[0]);
        assertEquals(Integer.valueOf(9), counts.get("n"));

        // mergeFunction returning null removes the entry
        assertNull(Maps.merge(counts, "a", 100, (oldV, v) -> null));
        assertFalse(counts.containsKey("a"));

        // empty (but non-null) map is a valid insertion target
        final Map<String, Integer> empty = new HashMap<>();
        assertEquals(Integer.valueOf(7), Maps.merge(empty, "k", 7, sum));
        assertEquals(Integer.valueOf(7), empty.get("k"));
    }

    @Test
    public void testMerge_nullArgs() {
        final Map<String, Integer> map = new HashMap<>();
        // null map / value / mergeFunction all -> IllegalArgumentException
        assertThrows(IllegalArgumentException.class, () -> Maps.merge(null, "a", 1, Integer::sum));
        assertThrows(IllegalArgumentException.class, () -> Maps.merge(map, "a", null, Integer::sum));
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class, () -> Maps.merge(map, "a", 1, null));
    }

    @Test
    public void testPutIfWithBiPredicate() {
        Map<String, Integer> source = new HashMap<>();
        source.put("a", 1);
        source.put("b", 2);
        source.put("c", 3);

        Map<String, Integer> target = new HashMap<>();

        boolean changed = Maps.putAllIf(target, source, (key, value) -> value > 1);
        assertTrue(changed);
        assertEquals(2, target.size());
        assertNull(target.get("a"));
        assertEquals(Integer.valueOf(2), target.get("b"));
        assertEquals(Integer.valueOf(3), target.get("c"));
    }

    @Test
    public void testPutIfWithPredicate() {
        Map<String, String> source = new HashMap<>();
        source.put("a", "1");
        source.put("b", "2");
        source.put("c", "3");

        Map<String, String> target = new HashMap<>();

        boolean changed = Maps.putAllIf(target, source, key -> !key.equals("b"));
        assertTrue(changed);
        assertEquals(2, target.size());
        assertEquals("1", target.get("a"));
        assertNull(target.get("b"));
        assertEquals("3", target.get("c"));

        assertFalse(Maps.putAllIf(target, new HashMap<>(), key -> true));
    }

    // putAllIf

    @Test
    public void testPutAllIf_biPredicate_emptySource() {
        Map<String, Integer> target = new HashMap<>();
        assertFalse(Maps.putAllIf(target, new HashMap<>(), (k, v) -> true));
    }

    @Test
    public void testPutAllIf_keyPredicate_emptySource() {
        Map<String, Integer> target = new HashMap<>();
        assertFalse(Maps.putAllIf(target, new HashMap<>(), k -> true));
    }

    @Test
    public void testPutAllIf_nullTargetMapThrowsRegardlessOfSource() {
        final Map<String, Integer> source = CommonUtil.asMap("a", 1);

        // an empty source used to hide the null target and answer false
        assertThrows(IllegalArgumentException.class, () -> Maps.putAllIf(null, new HashMap<String, Integer>(), k -> true));
        assertThrows(IllegalArgumentException.class, () -> Maps.putAllIf(null, source, k -> true));
        assertThrows(IllegalArgumentException.class, () -> Maps.putAllIf(null, new HashMap<String, Integer>(), (k, v) -> true));
        assertThrows(IllegalArgumentException.class, () -> Maps.putAllIf(null, source, (k, v) -> true));
    }

    @Test
    public void testPutIf_entryFilter() {
        Map<String, Integer> target = new HashMap<>(Map.of("a", 1));
        Map<String, Integer> source = Map.of("b", 20, "c", 3, "d", 40);
        BiPredicate<String, Integer> entryFilter = (k, v) -> v < 10;

        assertTrue(Maps.putAllIf(target, source, entryFilter));
        assertEquals(Map.of("a", 1, "c", 3), target);

        assertFalse(Maps.putAllIf(target, Map.of("e", 50), entryFilter));
    }

    @Test
    public void testPutIf_keyFilter() {
        Map<String, Integer> target = new HashMap<>(Map.of("a", 1));
        Map<String, Integer> source = Map.of("b", 2, "c", 3, "aa", 4);
        Predicate<String> keyFilter = k -> k.length() == 1;

        assertTrue(Maps.putAllIf(target, source, keyFilter));
        assertEquals(Map.of("a", 1, "b", 2, "c", 3), target);

        assertFalse(Maps.putAllIf(target, Map.of("bb", 5), keyFilter));
    }

    // putIfAbsent

    @Test
    public void testMutators_nonNullMapStillBehavesAsBefore() {
        final Map<String, String> map = new LinkedHashMap<>();

        assertNull(Maps.putIfAbsent(map, "a", "1"));
        assertEquals("1", Maps.putIfAbsent(map, "a", "2"));
        assertEquals("1", map.get("a"));
        assertEquals("1", Maps.getOrPutIfAbsent(map, "a", () -> "3"));
        assertEquals("4", Maps.getOrPutIfAbsent(map, "b", () -> "4"));
        assertEquals(CommonUtil.asMap("a", "1", "b", "4"), map);
        assertEquals(CommonUtil.asList("a", "b"), new ArrayList<>(map.keySet()));

        final Map<String, Integer> target = new LinkedHashMap<>();
        assertTrue(Maps.putAllIf(target, CommonUtil.asMap("x", 1, "y", 2), k -> k.equals("x")));
        assertEquals(CommonUtil.asMap("x", 1), target);
    }

    @Test
    public void testMutators_nullMapThrowsIllegalArgumentException() {
        final Supplier<String> supplier = () -> "v";

        assertThrows(IllegalArgumentException.class, () -> Maps.putIfAbsent((Map<String, String>) null, "k", "v"));
        assertThrows(IllegalArgumentException.class, () -> Maps.putIfAbsent((Map<String, String>) null, "k", supplier));
        assertThrows(IllegalArgumentException.class, () -> Maps.getOrPutIfAbsent((Map<String, String>) null, "k", supplier));
        assertThrows(IllegalArgumentException.class, () -> Maps.getOrPutListIfAbsent((Map<String, List<String>>) null, "k"));
        assertThrows(IllegalArgumentException.class, () -> Maps.getOrPutSetIfAbsent((Map<String, Set<String>>) null, "k"));
        assertThrows(IllegalArgumentException.class, () -> Maps.getOrPutLinkedHashSetIfAbsent((Map<String, Set<String>>) null, "k"));
        assertThrows(IllegalArgumentException.class, () -> Maps.getOrPutMapIfAbsent((Map<String, Map<String, String>>) null, "k"));
        assertThrows(IllegalArgumentException.class, () -> Maps.getOrPutLinkedHashMapIfAbsent((Map<String, Map<String, String>>) null, "k"));
        assertThrows(IllegalArgumentException.class, () -> Maps.merge((Map<String, Integer>) null, "k", 1, Integer::sum));
    }

    @Test
    public void testPutIfAbsent() {
        Map<String, String> map = new HashMap<>();
        map.put("existing", "value");

        assertNull(Maps.putIfAbsent(map, "new", "newValue"));
        assertEquals("newValue", map.get("new"));

        assertEquals("value", Maps.putIfAbsent(map, "existing", "anotherValue"));
        assertEquals("value", map.get("existing"));

        assertNull(Maps.putIfAbsent(map, "nullKey", (String) null));
        assertTrue(map.containsKey("nullKey"));
        assertNull(map.get("nullKey"));
    }

    @Test
    @DisplayName("putIfAbsent puts value and returns null when key is absent")
    public void test_putIfAbsent_absentKey() {
        Map<String, String> map = new HashMap<>();

        String result = Maps.putIfAbsent(map, "key3", "value3");

        assertNull(result, "Should return null (no previous mapping)");
        assertEquals("value3", map.get("key3"), "Map should now have the new value");
    }

    @Test
    @DisplayName("putIfAbsent returns existing value when key has non-null mapping")
    public void test_putIfAbsent_existingNonNullValue() {
        Map<String, String> map = new HashMap<>();
        map.put("key1", "value1");

        String result = Maps.putIfAbsent(map, "key1", "newValue");

        assertEquals("value1", result, "Should return the existing non-null value");
        assertEquals("value1", map.get("key1"), "Map should not be modified");
    }

    @Test
    @DisplayName("putIfAbsent puts value and returns null when key has null mapping")
    public void test_putIfAbsent_existingNullValue() {
        Map<String, String> map = new HashMap<>();
        map.put("key2", null);

        String result = Maps.putIfAbsent(map, "key2", "value2");

        assertNull(result, "Should return null (the previous null value)");
        assertEquals("value2", map.get("key2"), "Map should now have the new value");
    }

    @Test
    public void testPutIfAbsent_supplier() {
        Map<String, String> map = new HashMap<>();
        map.put("a", "apple");
        Supplier<String> supplier = () -> "newVal";

        assertEquals("apple", Maps.putIfAbsent(map, "a", supplier));
        assertEquals("apple", map.get("a"));

        assertNull(Maps.putIfAbsent(map, "b", supplier));
        assertEquals("newVal", map.get("b"));
    }

    @Test
    public void testPutIfAbsent_supplier_nullMap() {
        Map<String, String> map = new HashMap<>();
        map.put("key", null);
        assertNull(Maps.putIfAbsent(map, "key", Fn.s(() -> "value")));
        assertEquals("value", map.get("key"));
        assertEquals("value", Maps.putIfAbsent(map, "key", Fn.s(() -> "other")));
    }

    @Test
    public void testPutIfAbsent_value() {
        Map<String, String> map = new HashMap<>();
        map.put("a", "apple");

        assertEquals("apple", Maps.putIfAbsent(map, "a", "newApple"));
        assertEquals("apple", map.get("a"));

        assertNull(Maps.putIfAbsent(map, "b", "banana"));
        assertEquals("banana", map.get("b"));

        map.put("c", null);
        assertNull(Maps.putIfAbsent(map, "c", "cherry"));
        assertEquals("cherry", map.get("c"));
    }

    @Test
    public void testPutIfAbsentWithSupplier() {
        Map<String, String> map = new HashMap<>();
        map.put("existing", "value");

        assertNull(Maps.putIfAbsent(map, "new", Fn.s(() -> "newValue")));
        assertEquals("newValue", map.get("new"));

        final boolean[] supplierCalled = { false };
        assertEquals("value", Maps.putIfAbsent(map, "existing", Fn.s(() -> {
            supplierCalled[0] = true;
            return "anotherValue";
        })));
        assertFalse(supplierCalled[0]);
    }

    @Test
    @DisplayName("putIfAbsent(Supplier) returns the previous value uniformly for a ConcurrentMap")
    public void testPutIfAbsentWithSupplier_concurrentMap() {
        ConcurrentMap<String, String> map = new ConcurrentHashMap<>();
        map.put("existing", "value");

        // Existing non-null key: supplier not called, existing value returned
        final boolean[] supplierCalled = { false };
        assertEquals("value", Maps.putIfAbsent(map, "existing", Fn.s(() -> {
            supplierCalled[0] = true;
            return "other";
        })));
        assertFalse(supplierCalled[0]);
        assertEquals("value", map.get("existing"));

        // Absent key: returns the PREVIOUS value (null), not the newly created one,
        // matching the plain-Map contract (no longer delegates to computeIfAbsent).
        assertNull(Maps.putIfAbsent(map, "fresh", Fn.s(() -> "created")));
        assertEquals("created", map.get("fresh"));
    }

    @Test
    @DisplayName("putIfAbsent(Supplier) invokes the supplier and returns null when the key is mapped to null")
    public void testPutIfAbsentWithSupplier_nullMappedKey() {
        Map<String, String> map = new HashMap<>();
        map.put("nullKey", null);

        final boolean[] supplierCalled = { false };
        String result = Maps.putIfAbsent(map, "nullKey", Fn.s(() -> {
            supplierCalled[0] = true;
            return "supplied";
        }));

        // Returns the previous value (null), not the newly supplied one
        assertNull(result);
        assertTrue(supplierCalled[0]);
        assertEquals("supplied", map.get("nullKey"));
    }
}

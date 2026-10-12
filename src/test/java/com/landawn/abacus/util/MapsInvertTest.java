package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;
import java.util.function.Function;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

public class MapsInvertTest extends MapsTestSupport {

    // C-382 + C-377: one template policy

    /** A case-insensitive map keyed by lower-cased Strings; a non-String key is a ClassCastException. */
    public static class CiMap<V> extends LinkedHashMap<Object, V> {
        private static final long serialVersionUID = 1L;

        public CiMap() {
        }

        public CiMap(final int capacity) {
            super(capacity);
        }

        @Override
        public V put(final Object key, final V value) {
            return super.put(((String) key).toLowerCase(java.util.Locale.ROOT), value);
        }

        @Override
        public V get(final Object key) {
            return super.get(((String) key).toLowerCase(java.util.Locale.ROOT));
        }

        @Override
        public boolean containsKey(final Object key) {
            return super.containsKey(((String) key).toLowerCase(java.util.Locale.ROOT));
        }

        @Override
        public V computeIfAbsent(final Object key, final java.util.function.Function<? super Object, ? extends V> mappingFunction) {
            final V existing = get(key);

            if (existing != null) {
                return existing;
            }

            final V created = mappingFunction.apply(key);
            put(key, created);
            return created;
        }
    }

    // flatInvert

    @Test
    public void testFlatInvert() {
        Map<String, List<Integer>> map = new HashMap<>();
        map.put("even", Arrays.asList(2, 4, 6));
        map.put("odd", Arrays.asList(1, 3, 5));
        map.put("prime", Arrays.asList(2, 3, 5));

        Map<Integer, List<String>> inverted = Maps.flatInvert(map);
        assertEquals(6, inverted.size());

        assertEquals(Arrays.asList("odd"), inverted.get(1));
        assertTrue(inverted.get(2).contains("even"));
        assertTrue(inverted.get(2).contains("prime"));
        assertEquals(2, inverted.get(2).size());
    }

    @Test
    public void testFlatInvert_emptyMap() {
        Map<String, List<String>> result = Maps.flatInvert(new HashMap<>());
        assertTrue(result.isEmpty());
    }

    @Test
    public void testFlatInvert_New() {
        Map<String, List<Integer>> map = new LinkedHashMap<>();
        map.put("Alice", Arrays.asList(1, 2, 3));
        map.put("Bob", Arrays.asList(2, 4));

        Map<Integer, List<String>> result = Maps.flatInvert(map);
        assertEquals(4, result.size());
        assertEquals(1, result.get(1).size());
        assertEquals("Alice", result.get(1).get(0));
        assertEquals(2, result.get(2).size());
    }

    @Test
    public void testFlatInvert_nullMap() {
        Map<String, List<String>> result = Maps.flatInvert(null);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testFlatInvertBiMapResultFindsItsOwnValues() {
        final BiMap<String, List<Integer>> map = new BiMap<>();
        map.put("x", Arrays.asList(1, 2));

        final Map<Integer, List<String>> inverted = Maps.flatInvert(map);

        assertEquals(Arrays.asList("x"), inverted.get(1));
        assertEquals(Arrays.asList("x"), inverted.get(2));
        assertTrue(inverted.containsValue(Arrays.asList("x")));
    }

    @Test
    public void testC382_flatInvertOfCaseInsensitiveMapKeepsBothElements() {
        final CiMap<List<String>> map = new CiMap<>();
        map.put("a", Arrays.asList("X", "x"));

        final Map<String, List<Object>> inverted = Maps.flatInvert(map);

        assertEquals(2, inverted.size());
        assertEquals(Arrays.asList("a"), inverted.get("X"));
        assertEquals(Arrays.asList("a"), inverted.get("x"));
    }

    @Test
    public void testC380_flatInvertRepeatsKeyPerOccurrence() {
        final Map<String, List<Integer>> map = new LinkedHashMap<>();
        map.put("A", Arrays.asList(1, 1, 2));

        final Map<Integer, List<String>> inverted = Maps.flatInvert(map);

        assertEquals(Arrays.asList("A", "A"), inverted.get(1));
        assertEquals(Arrays.asList("A"), inverted.get(2));
    }

    @Test
    public void testC382_nullHostileTemplatesAcceptNullElements() {
        final Map<String, List<Integer>> chm = new ConcurrentHashMap<>();
        chm.put("a", Arrays.asList(1, null));
        chm.put("b", Arrays.asList(2, 3));
        final Map<String, List<Integer>> table = new java.util.Hashtable<>(chm);

        for (final Map<String, List<Integer>> template : Arrays.asList(chm, table)) {
            final Map<Integer, List<String>> inverted = Maps.flatInvert(template);
            assertEquals(Arrays.asList("a"), inverted.get(null), template.getClass().getName());
            assertEquals(Arrays.asList("a"), inverted.get(1));

            final List<Map<String, Integer>> rows = Maps.transpose(template);
            assertEquals(2, rows.size());
            assertTrue(rows.get(1).containsKey("a"));
            assertNull(rows.get(1).get("a"));
            assertEquals(3, rows.get(1).get("b"));
        }
    }

    // invert

    @Test
    @Tag("slow-test")
    public void testFilterAndInvertLargeMaps() {
        Map<String, Integer> veryLargeMap = new HashMap<>();
        for (int i = 0; i < 100000; i++) {
            veryLargeMap.put("key" + i, i);
        }

        Map<String, Integer> filtered = Maps.filterByValue(veryLargeMap, v -> v % 1000 == 0);
        assertEquals(100, filtered.size());
        assertEquals(0, filtered.get("key0"));
        assertEquals(99000, filtered.get("key99000"));

        Map<Integer, String> inverted = Maps.invert(veryLargeMap);
        assertEquals(100000, inverted.size());
        assertEquals("key0", inverted.get(0));
        assertEquals("key99999", inverted.get(99999));
    }

    @Test
    public void testInvert() {
        Map<Integer, String> inverted = Maps.invert(intMap);
        assertEquals(3, inverted.size());
        assertEquals("one", inverted.get(1));
        assertEquals("two", inverted.get(2));
        assertEquals("three", inverted.get(3));

        assertTrue(Maps.invert(null).isEmpty());
    }

    @Test
    public void testInvert_emptyMap() {
        Map<String, String> result = Maps.invert(new HashMap<>());
        assertTrue(result.isEmpty());
    }

    @Test
    public void testInvert_nullMap() {
        Map<String, String> result = Maps.invert(null);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testInvert_withMerge() {
        Map<String, Integer> map = new LinkedHashMap<>();
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 1);
        BiFunction<String, String, String> merger = (oldKey, newKey) -> oldKey + "," + newKey;
        Map<Integer, String> inverted = Maps.invert(map, merger);

        assertEquals("a,c", inverted.get(1));
        assertEquals("b", inverted.get(2));
    }

    @Test
    public void testInvert_withMerge_nullMap() {
        Map<String, String> result = Maps.invert(null, (a, b) -> a);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testInvert_WithMergeOp() {
        Map<String, Integer> map = new LinkedHashMap<>();
        map.put("x", 1);
        map.put("y", 2);
        map.put("z", 1);

        Map<Integer, String> inverted = Maps.invert(map, (k1, k2) -> k1 + "," + k2);
        assertEquals(2, inverted.size());
        assertTrue(inverted.containsKey(1));
        assertTrue(inverted.containsKey(2));
        assertEquals("y", inverted.get(2));
    }

    @Test
    public void testC376_invertNullAndEmptyInput() {
        assertTrue(Maps.invert((Map<String, String>) null, (a, b) -> null).isEmpty());
        assertTrue(Maps.invert(new HashMap<String, String>(), (a, b) -> null).isEmpty());
        assertThrows(IllegalArgumentException.class, () -> Maps.invert(new HashMap<String, String>(), null));
    }

    @Test
    public void testC376_invertNullInputKeyIsStillAGenuinePreviousKey() {
        final Map<String, String> map = new LinkedHashMap<>();
        map.put(null, "v");
        map.put("k", "v");
        final List<String> calls = new ArrayList<>();

        final Map<String, String> inverted = Maps.invert(map, (oldKey, newKey) -> {
            calls.add(oldKey + "|" + newKey);
            return oldKey == null ? "was-null" : oldKey;
        });

        assertEquals(Collections.singletonMap("v", "was-null"), inverted);
        assertEquals(Arrays.asList("null|k"), calls);
    }

    @Test
    public void testC376_invertNullMergeOnConcurrentHashMapDoesNotThrow() {
        final Map<String, String> map = new ConcurrentHashMap<>();
        map.put("a", "x");
        map.put("b", "x");

        final Map<String, String> inverted = Maps.invert(map, (oldKey, newKey) -> null);

        assertTrue(inverted.isEmpty(), inverted.toString());
    }

    // C-376: invert(Map, BiFunction) - a null merge result removes the entry (Map.merge rule)
    @Test
    public void testC376_invertNullMergeResultRemovesEntry() {
        final Map<String, String> map = new LinkedHashMap<>();
        map.put("k1", "v");
        map.put("k2", "v");
        map.put("k3", "v");
        final List<String> calls = new ArrayList<>();

        final Map<String, String> inverted = Maps.invert(map, (oldKey, newKey) -> {
            calls.add(oldKey + "|" + newKey);
            return null;
        });

        // k1 stored, k1+k2 merge to null -> removed, k3 then stored directly without a merge call
        assertEquals(Collections.singletonMap("v", "k3"), inverted);
        assertEquals(Arrays.asList("k1|k2"), calls);
    }

    @Test
    public void testC376_invertNullMergeThenNonNullMerge() {
        final Map<String, String> map = new LinkedHashMap<>();
        map.put("ä", "v");
        map.put("b", "v");
        map.put("c", "v");
        map.put("d", "v");
        map.put("e", "w");

        // null for the first collision, concatenation afterwards
        final int[] n = { 0 };
        final Map<String, String> inverted = Maps.invert(map, (oldKey, newKey) -> n[0]++ == 0 ? null : oldKey + "," + newKey);

        final Map<String, String> expected = new LinkedHashMap<>();
        expected.put("v", "c,d");
        expected.put("w", "e");
        assertEquals(expected, inverted);
    }

    @Test
    public void testC382_invertOfCaseInsensitiveMapKeepsBothValues() {
        final CiMap<Object> map = new CiMap<>();
        map.put("a", "X");
        map.put("b", "x");

        final Map<Object, Object> inverted = Maps.invert(map);

        assertEquals(LinkedHashMap.class, inverted.getClass());
        assertEquals(2, inverted.size());
        assertEquals("a", inverted.get("X"));
        assertEquals("b", inverted.get("x"));

        // non-String values: used to be a ClassCastException from the mirrored map
        final CiMap<Object> numbers = new CiMap<>();
        numbers.put("a", 1);
        assertEquals(Collections.singletonMap(1, "a"), Maps.invert(numbers));
        assertEquals(Collections.singletonMap(1, "a"), Maps.invert(numbers, (x, y) -> x));
    }

    @Test
    public void testC382_invertResultTypes() {
        final Map<String, Integer> chm = new ConcurrentHashMap<>();
        chm.put("a", 1);
        assertEquals(LinkedHashMap.class, Maps.invert(chm).getClass());

        final Map<String, Integer> hash = new HashMap<>();
        hash.put("a", 1);
        assertEquals(HashMap.class, Maps.invert(hash).getClass());

        final Map<String, Integer> weak = new java.util.WeakHashMap<>();
        weak.put("a", 1);
        assertEquals(LinkedHashMap.class, Maps.invert(weak).getClass());

        final BiMap<String, Integer> biMap = new BiMap<>();
        biMap.put("a", 1);
        assertTrue(Maps.invert(biMap) instanceof BiMap);
    }

    @Test
    public void testInvertWithMergeFunction() {
        Map<String, Integer> map = new HashMap<>();
        map.put("one", 1);
        map.put("uno", 1);
        map.put("two", 2);

        Map<Integer, String> inverted = Maps.invert(map, (v1, v2) -> v1 + "," + v2);
        assertEquals(2, inverted.size());
        assertTrue(inverted.get(1).contains("one"));
        assertTrue(inverted.get(1).contains("uno"));
        assertEquals("two", inverted.get(2));
    }

    @Test
    public void testNullHandlingInAllMethods() {
        Map<String, String> nullMap = null;
        Map<String, String> emptyMap = new HashMap<>();

        assertFalse(Maps.getIfExists(nullMap, "key").isPresent());
        assertTrue(Maps.keySet(nullMap).isEmpty());
        assertTrue(Maps.values(nullMap).isEmpty());
        assertTrue(Maps.entrySet(nullMap).isEmpty());
        assertTrue(Maps.filter(nullMap, e -> true).isEmpty());
        assertTrue(Maps.filterByKey(nullMap, k -> true).isEmpty());
        assertTrue(Maps.filterByValue(nullMap, v -> true).isEmpty());
        assertTrue(Maps.invert(nullMap).isEmpty());
        assertTrue(Maps.flatInvert((Map<String, Collection<String>>) null).isEmpty());
        assertTrue(Maps.intersection(nullMap, emptyMap).isEmpty());

        Map<String, String> mapWithNulls = new HashMap<>();
        mapWithNulls.put("null1", null);
        mapWithNulls.put("null2", null);
        mapWithNulls.put("notNull", "value");

        Map<String, String> inverted = Maps.invert(mapWithNulls);
        assertEquals(2, inverted.size());
        assertTrue(inverted.containsKey(null));
        assertEquals("notNull", inverted.get("value"));
    }

    @Test
    public void reviewFixes20260906_invertOfAnIdentityHashMapCollapsesEqualValues() {
        final String a = new String("x");
        final String b = new String("x");
        assertNotSame(a, b);

        final Map<String, String> src = new IdentityHashMap<>();
        src.put("k1", a);
        src.put("k2", b);

        // newOrderingMap used to mirror the template's class. An IdentityHashMap's reference equivalence
        // applies to the TEMPLATE's keys, which become the result's VALUES - so equal values did not collapse
        // and result.get("x") missed an entry the map plainly held.
        final Map<String, String> inv = Maps.invert(src);
        assertEquals(1, inv.size());
        assertNotNull(inv.get("x"));
        assertFalse(inv instanceof IdentityHashMap);

        // ... and the documented merge function is actually invoked.
        final Map<String, String> merged = Maps.invert(src, (k1, k2) -> k1 + "|" + k2);
        assertEquals(1, merged.size());
        assertTrue(merged.get("x").contains("|"));

        // flatInvert groups by value, not by instance.
        final Map<String, List<String>> flatSrc = new IdentityHashMap<>();
        flatSrc.put("k1", CommonUtil.asList(a));
        flatSrc.put("k2", CommonUtil.asList(b));
        final Map<String, List<String>> flat = Maps.flatInvert(flatSrc);
        assertEquals(1, flat.size());
        assertEquals(2, flat.get("x").size());

        // Controls: the ordinary templates are unchanged.
        final Map<String, String> ordered = new LinkedHashMap<>();
        ordered.put("k1", a);
        ordered.put("k2", b);
        assertEquals(1, Maps.invert(ordered).size());
        assertTrue(Maps.invert(ordered) instanceof LinkedHashMap);

        final Map<String, String> sorted = new TreeMap<>();
        sorted.put("k1", "v");
        assertTrue(Maps.invert(sorted) instanceof LinkedHashMap);
        assertTrue(Maps.invert(new HashMap<>(CommonUtil.asMap("k", "v"))) instanceof HashMap);
    }
}

package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.function.BiPredicate;
import java.util.function.IntFunction;
import java.util.function.Predicate;

import org.junit.jupiter.api.Test;

public class MapsFilterTest extends MapsTestSupport {

    static final class G142NoDefaultOuter {
        G142NoDefaultOuter(final int ignored) {
        }

        java.util.Map<String, Integer> doubleBrace() {
            return new java.util.HashMap<>() {
                {
                    put("a", 1);
                }
            };
        }
    }

    // filter

    @Test
    public void testC377_concurrentSkipListTemplateStaysConcurrent() {
        final java.util.concurrent.ConcurrentSkipListMap<String, List<Integer>> map = new java.util.concurrent.ConcurrentSkipListMap<>(
                java.util.Comparator.reverseOrder());
        map.put("a", Arrays.asList(1, null));
        map.put("b", Arrays.asList(2));

        final Map<String, List<Integer>> filtered = Maps.filter(map, (k, v) -> true);
        assertTrue(filtered instanceof java.util.concurrent.ConcurrentSkipListMap);
        assertEquals(Arrays.asList("b", "a"), new ArrayList<>(filtered.keySet()));

        // transpose rows may hold null: a TreeMap with the same comparator
        final List<Map<String, Integer>> rows = Maps.transpose(map);
        assertTrue(rows.get(1) instanceof TreeMap);
        assertEquals(Arrays.asList("b", "a"), new ArrayList<>(rows.get(0).keySet()));
        assertNull(rows.get(1).get("a"));
    }

    @Test
    public void testC377_enumMapTemplateStaysEnumMap() {
        final java.util.EnumMap<java.util.concurrent.TimeUnit, Integer> map = new java.util.EnumMap<>(java.util.concurrent.TimeUnit.class);
        map.put(java.util.concurrent.TimeUnit.SECONDS, 1);
        map.put(java.util.concurrent.TimeUnit.NANOSECONDS, 2);
        map.put(java.util.concurrent.TimeUnit.DAYS, 3);

        final Map<java.util.concurrent.TimeUnit, Integer> filtered = Maps.filter(map, (k, v) -> v > 1);
        assertTrue(filtered instanceof java.util.EnumMap);
        assertEquals(Arrays.asList(java.util.concurrent.TimeUnit.NANOSECONDS, java.util.concurrent.TimeUnit.DAYS), new ArrayList<>(filtered.keySet()));
        assertTrue(Maps.intersection(map, new HashMap<>(map)) instanceof java.util.EnumMap);

        final java.util.EnumMap<java.util.concurrent.TimeUnit, Integer> empty = new java.util.EnumMap<>(java.util.concurrent.TimeUnit.class);
        assertTrue(Maps.newTargetMap(empty) instanceof java.util.EnumMap);
        assertTrue(Maps.filter(empty, (k, v) -> true).isEmpty());
    }

    @Test
    public void testFilter() {
        Map<String, String> result = Maps.filter(testMap, entry -> entry.getValue().endsWith("1") || entry.getValue().endsWith("3"));
        assertEquals(2, result.size());
        assertEquals("value1", result.get("key1"));
        assertEquals("value3", result.get("key3"));
        assertNull(result.get("key2"));

        assertTrue(Maps.filter(null, entry -> true).isEmpty());
    }

    // Anonymous maps use a LinkedHashMap fallback even when their enclosing class cannot be constructed.
    @Test
    public void testFilter_anonymousMapWhoseEnclosingClassCannotBeConstructed() {
        final java.util.Map<String, Integer> m = new G142NoDefaultOuter(0).doubleBrace();
        final java.util.Map<String, Integer> filtered = Maps.filter(m, (k, v) -> true);

        org.junit.jupiter.api.Assertions.assertEquals(java.util.LinkedHashMap.class, filtered.getClass());
        org.junit.jupiter.api.Assertions.assertEquals(java.util.Map.of("a", 1), filtered);
    }

    @Test
    public void testFilter_biPredicate_emptyMap() {
        Map<String, String> result = Maps.filter(new HashMap<>(), (k, v) -> true);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testFilter_biPredicate_withMapSupplier() {
        final Map<String, Integer> map = new LinkedHashMap<>();
        map.put("apple", 5);
        map.put("banana", 2);
        map.put("cherry", 8);

        final LinkedHashMap<String, Integer> filtered = Maps.filter(map, (k, v) -> v > 4, LinkedHashMap::new);
        assertTrue(filtered instanceof LinkedHashMap);
        assertEquals(2, filtered.size());
        assertEquals(Integer.valueOf(5), filtered.get("apple"));
        assertEquals(Integer.valueOf(8), filtered.get("cherry"));
        assertFalse(filtered.containsKey("banana"));

        // null map -> empty map produced by the supplier
        final TreeMap<String, Integer> empty = Maps.filter((Map<String, Integer>) null, (k, v) -> true, size -> new TreeMap<>());
        assertNotNull(empty);
        assertTrue(empty.isEmpty());

        // null predicate -> IllegalArgumentException
        assertThrows(IllegalArgumentException.class, () -> Maps.filter(map, (BiPredicate<String, Integer>) null, HashMap::new));
    }

    @Test
    public void testFilter_emptyMap() {
        Map<String, String> result = Maps.filter(new HashMap<>(), e -> true);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testFilter_entryPredicate() {
        Map<String, Integer> map = Map.of("a", 1, "b", 20, "c", 3);
        Map<String, Integer> filtered = Maps.filter(map, entry -> entry.getValue() < 10);
        assertEquals(Map.of("a", 1, "c", 3), filtered);
        assertNotSame(map, filtered);
        assertEquals(0, Maps.filter(null, e -> true).size());
    }

    @Test
    public void testFilter_NullMap_ReturnsEmptyMap() {
        Map<String, Integer> result = Maps.filter((Map<String, Integer>) null, (k, v) -> true);
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    // G053-01: newTargetMap returned a pre-filled map for a double-brace source; filter results gained "x".
    @Test
    public void testFilter_prefilledAnonymousSourceClass() {
        final Map<String, Integer> m = g053DoubleBraceMapWithXRemoved();

        assertEquals(CommonUtil.asMap("y", 2, "z", 3), Maps.filter(m, (k, v) -> true));
        assertEquals(CommonUtil.asMap("z", 3), Maps.filterByValue(m, v -> v > 2));
        assertEquals(CommonUtil.asMap("y", 2, "z", 3), Maps.intersection(m, new HashMap<>(m)));
        assertEquals(2, Maps.difference(m, new HashMap<>()).size());
    }

    @Test
    public void testFilter_validatesSupplierAndForwardsSizeHint() {
        final Map<String, Integer> map = new LinkedHashMap<>();
        map.put("a", 1);
        map.put("b", 2);
        final int[] sizeHint = { -1 };

        Maps.filter(map, (k, v) -> true, size -> {
            sizeHint[0] = size;
            return new LinkedHashMap<>();
        });

        assertEquals(map.size(), sizeHint[0]);
        assertThrows(IllegalArgumentException.class, () -> Maps.filter(map, (k, v) -> true, (IntFunction<Map<String, Integer>>) null));
        assertThrows(NullPointerException.class, () -> Maps.filter(map, (k, v) -> true, ignored -> (Map<String, Integer>) null));
        assertThrows(NullPointerException.class, () -> Maps.filter((Map<String, Integer>) null, (k, v) -> true, ignored -> (Map<String, Integer>) null));
    }

    @Test
    public void testFilterBiPredicate() {
        Map<String, String> result = Maps.filter(testMap, (key, value) -> key.equals("key1") || value.equals("value3"));
        assertEquals(2, result.size());
        assertEquals("value1", result.get("key1"));
        assertEquals("value3", result.get("key3"));
    }

    @Test
    public void testC375_filterSupplierNullAndFreshMaps() {
        final Map<String, Integer> map = new LinkedHashMap<>();
        map.put("über", 3);
        map.put("b", 1);

        final NullPointerException e = assertThrows(NullPointerException.class, () -> Maps.filter(map, (k, v) -> true, n -> null));
        assertEquals("mapSupplier returned null", e.getMessage());

        final Map<String, Integer> other = new TreeMap<>();
        final Map<String, Integer> result = Maps.filter(map, (k, v) -> v > 2, n -> other);
        assertSame(other, result);
        assertEquals(Collections.singletonMap("über", 3), result);

        assertTrue(Maps.filter((Map<String, Integer>) null, (k, v) -> true, n -> new HashMap<>()).isEmpty());
    }

    @Test
    public void testC375_filterSupplierReturningEmptyInputMapIsRejected() {
        final Map<String, Integer> empty = new HashMap<>();

        assertThrows(IllegalArgumentException.class, () -> Maps.filter(empty, (k, v) -> true, n -> empty));
    }

    // C-375: filter(Map, BiPredicate, IntFunction) rejects a supplier that returns the input map
    @Test
    public void testC375_filterSupplierReturningInputMapIsRejected() {
        final Map<String, Integer> map = new LinkedHashMap<>();
        map.put("a", 1);
        map.put("b", 2);

        final IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> Maps.filter(map, (k, v) -> v > 1, n -> map));

        assertEquals("mapSupplier must create a new map", e.getMessage());
        assertEquals(2, map.size());
    }

    @Test
    public void testFilterWithNullPredicate() {
        assertThrows(IllegalArgumentException.class, () -> Maps.filter(testMap, (java.util.function.Predicate<Map.Entry<String, String>>) null));
    }

    @Test
    public void testFilterWithPredicateExceptions() {
        Map<String, String> map = new HashMap<>();
        map.put("key1", "value1");

        assertThrows(IllegalArgumentException.class, () -> Maps.filter(map, (Predicate<Map.Entry<String, String>>) null));
    }

    @Test
    public void testLinkedHashMapPreservation() {
        LinkedHashMap<String, String> linkedMap = new LinkedHashMap<>();
        linkedMap.put("first", "1");
        linkedMap.put("second", "2");
        linkedMap.put("third", "3");

        Map<String, String> filtered = Maps.filter(linkedMap, e -> !e.getKey().equals("second"));

        java.util.Iterator<Map.Entry<String, String>> entries = filtered.entrySet().iterator();
        assertEquals("first", entries.next().getKey());
        assertEquals("third", entries.next().getKey());
    }

    @Test
    public void testC377_unconstructibleOrderedTemplatesKeepOrder() {
        final Map<String, Integer> source = new LinkedHashMap<>();
        final List<String> keys = Arrays.asList("zeta", "alpha", "mu", "beta", "omega", "über");

        for (int i = 0; i < keys.size(); i++) {
            source.put(keys.get(i), i);
        }

        for (final Map<String, Integer> template : Arrays.asList(Collections.unmodifiableMap(source), Collections.synchronizedMap(source),
                ImmutableMap.copyOf(source))) {
            final Map<String, Integer> filtered = Maps.filter(template, (k, v) -> true);
            assertEquals(LinkedHashMap.class, filtered.getClass(), template.getClass().getName());
            assertEquals(keys, new ArrayList<>(filtered.keySet()));
            assertEquals(keys, new ArrayList<>(Maps.filterByKey(template, k -> true).keySet()));
            assertEquals(keys, new ArrayList<>(Maps.intersection(template, source).keySet()));
        }
    }

    // filterByKey

    @Test
    public void testFilterByKey() {
        Map<String, String> result = Maps.filterByKey(testMap, key -> key.compareTo("key2") <= 0);
        assertEquals(2, result.size());
        assertTrue(result.containsKey("key1"));
        assertTrue(result.containsKey("key2"));
        assertFalse(result.containsKey("key3"));
    }

    @Test
    public void testFilterByKey_emptyMap() {
        Map<String, String> result = Maps.filterByKey(new HashMap<>(), k -> true);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testSortedMapPreservation() {
        TreeMap<String, String> sortedMap = new TreeMap<>();
        sortedMap.put("c", "3");
        sortedMap.put("a", "1");
        sortedMap.put("b", "2");

        Map<String, String> filtered = Maps.filterByKey(sortedMap, k -> !k.equals("b"));
        assertTrue(filtered instanceof TreeMap);

        java.util.Iterator<String> keys = filtered.keySet().iterator();
        assertEquals("a", keys.next());
        assertEquals("c", keys.next());
    }

    @Test
    public void testThreadSafetyConsiderations() {

        final Map<String, String> concurrentMap = new HashMap<>();
        for (int i = 0; i < 100; i++) {
            concurrentMap.put("key" + i, "value" + i);
        }

        Map<String, String> filtered = Maps.filterByKey(concurrentMap, k -> k.contains("5"));
        assertTrue(filtered.size() > 0);
    }

    // filterByValue

    @Test
    public void testFilterByValue() {
        Map<String, String> result = Maps.filterByValue(testMap, value -> value.compareTo("value2") >= 0);
        assertEquals(2, result.size());
        assertTrue(result.containsKey("key2"));
        assertTrue(result.containsKey("key3"));
        assertFalse(result.containsKey("key1"));
    }

    @Test
    public void testFilterByValue_emptyMap() {
        Map<String, String> result = Maps.filterByValue(new HashMap<>(), v -> true);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testLargeMapOperations() {
        Map<String, Integer> largeMap = new HashMap<>();
        for (int i = 0; i < 1000; i++) {
            largeMap.put("key" + i, i);
        }

        Map<String, Integer> filtered = Maps.filterByValue(largeMap, v -> v % 2 == 0);
        assertEquals(500, filtered.size());

        Map<Integer, String> inverted = Maps.invert(largeMap);
        assertEquals(1000, inverted.size());
        assertEquals("key500", inverted.get(500));
    }

    @Test
    public void testTransformations_fallBackCleanlyForUnconstructibleMapTypes() {
        final Map<String, Integer> immutable = Map.of("a", 1, "b", 2, "c", 3);

        assertEquals(3, Maps.filterByValue(immutable, v -> true).size());
        assertEquals(3, Maps.filterByKey(immutable, k -> true).size());
        assertEquals(3, Maps.filter(immutable, (k, v) -> true).size());
        assertEquals(3, Maps.invert(immutable).size());
        assertEquals(3, Maps.difference(immutable, CommonUtil.asMap("a", 9, "b", 9, "c", 9)).size());
        // and the results are ordinary mutable maps
        final Map<String, Integer> filtered = Maps.filterByValue(immutable, v -> true);
        assertDoesNotThrow(() -> filtered.put("d", 4));
    }

    @Test
    public void testTransformations_preserveComparatorBasedKeySemantics() {
        final TreeMap<String, Integer> caseInsensitive = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        caseInsensitive.put("a", 1);
        caseInsensitive.put("B", 2);

        final Map<String, Integer> filtered = Maps.filterByValue(caseInsensitive, v -> true);
        assertTrue(filtered instanceof TreeMap);
        assertEquals(Integer.valueOf(1), filtered.get("A"));
        assertEquals(Integer.valueOf(2), filtered.get("b"));
    }
}

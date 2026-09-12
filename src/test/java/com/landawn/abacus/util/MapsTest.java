package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.IntFunction;
import java.util.function.Predicate;
import java.util.function.Supplier;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.util.Difference.BeanDifference;
import com.landawn.abacus.util.u.Nullable;

import testfixtures.ParameterizedDescriptorFixtures;

public class MapsTest extends MapsTestSupport {
    @Test
    public void testZipDuplicateKeysWithUnequalLengths() {
        final List<String> keys = Arrays.asList("a", "a", "unpaired");
        final List<Integer> values = Arrays.asList(1, 2);

        assertEquals(Map.of("a", 2), Maps.zip(keys, values));
        assertEquals(Map.of("a", 2), Maps.zip(keys, values, LinkedHashMap::new));
        assertEquals(Map.of("a", 3), Maps.zip(keys, values, Integer::sum, HashMap::new));
        assertTrue(Maps.zip(keys, values, (left, right) -> null, HashMap::new).isEmpty());
    }

    @Test
    public void testZipWithMapSupplier() {
        List<String> keys = Arrays.asList("a", "b", "c");
        List<Integer> values = Arrays.asList(1, 2, 3);

        LinkedHashMap<String, Integer> result = Maps.zip(keys, values, LinkedHashMap::new);
        assertEquals(3, result.size());
        assertTrue(result instanceof LinkedHashMap);
    }

    @Test
    public void testZipWithMergeFunction() {
        List<String> keys = Arrays.asList("a", "b", "a");
        List<Integer> values = Arrays.asList(1, 2, 3);

        Map<String, Integer> result = Maps.zip(keys, values, Integer::sum, HashMap::new);
        assertEquals(2, result.size());
        assertEquals(Integer.valueOf(4), result.get("a"));
        assertEquals(Integer.valueOf(2), result.get("b"));
    }

    @Test
    public void testZip_NonCollectionIterables() {
        Iterable<String> keys = () -> Arrays.asList("k1", "k2", "k3").iterator();
        Iterable<Integer> values = () -> Arrays.asList(1, 2).iterator();

        Map<String, Integer> result = Maps.zip(keys, values);

        assertEquals(2, result.size());
        assertEquals(Integer.valueOf(1), result.get("k1"));
        assertEquals(Integer.valueOf(2), result.get("k2"));
        assertFalse(result.containsKey("k3"));
    }

    @Test
    public void testZipWithMapSupplier_NonCollectionIterables() {
        Iterable<String> keys = () -> Arrays.asList("a", "b").iterator();
        Iterable<Integer> values = () -> Arrays.asList(10, 20, 30).iterator();

        LinkedHashMap<String, Integer> result = Maps.zip(keys, values, ignored -> new LinkedHashMap<>());

        assertTrue(result instanceof LinkedHashMap);
        assertEquals(Map.of("a", 10, "b", 20), result);
    }

    @Test
    public void testZipWithMergeFunction_NonCollectionIterables() {
        Iterable<String> keys = () -> Arrays.asList("dup", "dup", "tail").iterator();
        Iterable<Integer> values = () -> Arrays.asList(1, 2, 3).iterator();

        LinkedHashMap<String, Integer> result = Maps.zip(keys, values, Integer::sum, ignored -> new LinkedHashMap<>());

        assertEquals(2, result.size());
        assertEquals(Integer.valueOf(3), result.get("dup"));
        assertEquals(Integer.valueOf(3), result.get("tail"));
    }

    @Test
    public void testZip_iterables_withMergeAndSupplier() {
        List<String> keys = Arrays.asList("a", "b", "a");
        List<Integer> values = Arrays.asList(1, 2, 3);
        BiFunction<Integer, Integer, Integer> merger = Integer::sum;
        IntFunction<HashMap<String, Integer>> supplier = HashMap::new;

        Map<String, Integer> result = Maps.zip(keys, values, merger, supplier);
        Map<String, Integer> expected = new HashMap<>();
        expected.put("a", 1 + 3);
        expected.put("b", 2);
        assertEquals(expected, result);
    }

    @Test
    public void testMapSupplierBehavior() {
        final boolean[] supplierCalled = { false };
        IntFunction<HashMap<String, String>> trackingSupplier = size -> {
            supplierCalled[0] = true;
            return new HashMap<>(size);
        };

        List<String> keys = Arrays.asList("a", "b");
        List<String> values = Arrays.asList("1", "2");

        Maps.zip(keys, values, trackingSupplier);
        assertTrue(supplierCalled[0]);
    }

    @Test
    public void testZip_UnequalSizes() {
        List<String> keys = Arrays.asList("a", "b");
        List<Integer> values = Arrays.asList(1, 2, 3);

        Map<String, Integer> result = Maps.zip(keys, values);
        assertEquals(2, result.size());
        assertEquals(1, result.get("a"));
        assertEquals(2, result.get("b"));
    }

    @Test
    public void testZip_WithMergeAndSupplier() {
        List<String> keys = Arrays.asList("a", "b", "a");
        List<Integer> values = Arrays.asList(1, 2, 3);
        Map<String, Integer> result = Maps.zip(keys, values, (v1, v2) -> v1 + v2, HashMap::new);
        assertEquals(Integer.valueOf(4), result.get("a"));
        assertEquals(Integer.valueOf(2), result.get("b"));
    }

    @Test
    public void testZip() {
        List<String> keys = Arrays.asList("a", "b", "c");
        List<Integer> values = Arrays.asList(1, 2, 3);

        Map<String, Integer> result = Maps.zip(keys, values);
        assertEquals(3, result.size());
        assertEquals(Integer.valueOf(1), result.get("a"));
        assertEquals(Integer.valueOf(2), result.get("b"));
        assertEquals(Integer.valueOf(3), result.get("c"));

        List<String> longerKeys = Arrays.asList("a", "b", "c", "d");
        Map<String, Integer> result2 = Maps.zip(longerKeys, values);
        assertEquals(3, result2.size());

        assertTrue(Maps.zip(null, values).isEmpty());
        assertTrue(Maps.zip(keys, null).isEmpty());
        assertTrue(Maps.zip(new ArrayList<>(), values).isEmpty());
    }

    @Test
    public void testZipWithEmptyCollections() {
        List<String> emptyKeys = new ArrayList<>();
        List<Integer> values = Arrays.asList(1, 2, 3);

        Map<String, Integer> result1 = Maps.zip(emptyKeys, values);
        assertTrue(result1.isEmpty());

        Map<String, Integer> result2 = Maps.zip(emptyKeys, values, (v1, v2) -> v1 + v2, HashMap::new);
        assertTrue(result2.isEmpty());
    }

    @Test
    public void testZip_iterables() {
        List<String> keys = Arrays.asList("a", "b");
        List<Integer> values = Arrays.asList(1, 2);
        Map<String, Integer> expected = new HashMap<>();
        expected.put("a", 1);
        expected.put("b", 2);
        assertEquals(expected, Maps.zip(keys, values));

        assertTrue(Maps.zip(null, values).isEmpty());
        assertTrue(Maps.zip(keys, null).isEmpty());
        assertTrue(Maps.zip(Arrays.asList(), values).isEmpty());

        List<String> shortKeys = Arrays.asList("a");
        assertEquals(Map.of("a", 1), Maps.zip(shortKeys, values));
    }

    @Test
    public void testZip_BasicCase() {
        List<String> keys = Arrays.asList("a", "b", "c");
        List<Integer> values = Arrays.asList(1, 2, 3);

        Map<String, Integer> result = Maps.zip(keys, values);
        assertEquals(3, result.size());
        assertEquals(1, result.get("a"));
        assertEquals(2, result.get("b"));
        assertEquals(3, result.get("c"));
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
    public void testNewImmutableEntry_values() {
        ImmutableEntry<String, Integer> entry = Maps.newImmutableEntry("key", 123);
        assertEquals("key", entry.getKey());
        assertEquals(123, entry.getValue().intValue());
    }

    @Test
    public void testNewImmutableEntry() {
        ImmutableEntry<String, Integer> entry = Maps.newImmutableEntry("key", 100);
        assertEquals("key", entry.getKey());
        assertEquals(Integer.valueOf(100), entry.getValue());

        assertThrows(UnsupportedOperationException.class, () -> entry.setValue(200));
    }

    @Test
    public void testNewTargetMap_PreservesSortedComparator() {
        TreeMap<String, Integer> source = new TreeMap<>(Comparator.reverseOrder());
        source.put("b", 2);
        source.put("a", 1);

        Map<?, ?> target = Maps.newTargetMap(source, 4);

        assertTrue(target instanceof TreeMap);
        assertEquals(source.comparator(), ((TreeMap<?, ?>) target).comparator());
    }

    @Test
    public void testNewTargetMapAndNewOrderingMap_FallbackForNonInstantiableMap() {
        Map<String, Integer> source = new NonInstantiableHashMap<>("blocked");
        source.put("a", 1);

        Map<?, ?> target = Maps.newTargetMap(source, 3);
        Map<?, ?> ordering = Maps.newOrderingMap(source);

        assertEquals(HashMap.class, target.getClass());
        assertTrue(target.isEmpty());
        assertEquals(LinkedHashMap.class, ordering.getClass());
        assertTrue(ordering.isEmpty());
    }

    @Test
    public void testNewOrderingMap_SortedAndUnmodifiableMap() {
        TreeMap<String, Integer> sorted = new TreeMap<>(Comparator.reverseOrder());
        sorted.put("b", 2);
        sorted.put("a", 1);

        Map<?, ?> sortedResult = Maps.newOrderingMap(sorted);
        Map<?, ?> fallbackResult = Maps.newOrderingMap(Collections.unmodifiableMap(new LinkedHashMap<>(Map.of("x", 1))));

        assertTrue(sortedResult instanceof LinkedHashMap);
        assertEquals(LinkedHashMap.class, fallbackResult.getClass());
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

    @Test
    public void testZip_emptyKeys_withMapSupplier() {
        List<String> emptyKeys = new ArrayList<>();
        List<Integer> values = Arrays.asList(1, 2, 3);
        Map<String, Integer> result = Maps.zip(emptyKeys, values, HashMap::new);
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testZip_validatesFunctionsAndSuppliedMapForEmptyInputs() {
        final List<String> emptyKeys = Collections.emptyList();
        final List<Integer> values = Arrays.asList(1, 2, 3);

        assertThrows(IllegalArgumentException.class, () -> Maps.zip(emptyKeys, values, (IntFunction<Map<String, Integer>>) null));
        assertThrows(IllegalArgumentException.class, () -> Maps.zip(emptyKeys, values, ignored -> (Map<String, Integer>) null));
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                () -> Maps.zip(emptyKeys, values, (BiFunction<Integer, Integer, Integer>) null, HashMap::new));
        assertThrows(IllegalArgumentException.class, () -> Maps.zip(emptyKeys, values, Integer::sum, ignored -> (Map<String, Integer>) null));
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
    public void testNullMapHandling() {
        Map<String, String> nullMap = null;

        assertFalse(Maps.getIfExists(nullMap, "key").isPresent());
        assertTrue(Maps.keySet(nullMap).isEmpty());
        assertTrue(Maps.values(nullMap).isEmpty());
        assertTrue(Maps.entrySet(nullMap).isEmpty());
        assertTrue(Maps.filter(nullMap, e -> true).isEmpty());
        assertTrue(Maps.filterByKey(nullMap, k -> true).isEmpty());
        assertTrue(Maps.filterByValue(nullMap, v -> true).isEmpty());
        assertTrue(Maps.invert(nullMap).isEmpty());
        assertTrue(Maps.flatInvert((Map<String, Collection<String>>) null).isEmpty());
        assertTrue(Maps.intersection(nullMap, new HashMap<>()).isEmpty());
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
    public void testComplexPathOperations() {
        Map<String, Object> complexMap = new HashMap<>();

        List<Map<String, Object>> list = new ArrayList<>();
        Map<String, Object> item1 = new HashMap<>();
        item1.put("prop", "value1");
        list.add(item1);

        Map<String, Object> item2 = new HashMap<>();
        List<String> innerList = Arrays.asList("a", "b", "c");
        item2.put("innerList", innerList);
        list.add(item2);

        complexMap.put("list", list);

        assertEquals("value1", Maps.getByPath(complexMap, "list[0].prop"));
        assertEquals("b", Maps.getByPath(complexMap, "list[1].innerList[1]"));

        assertNull(Maps.getByPath(complexMap, "list[5].prop"));
        assertNull(Maps.getByPath(complexMap, "list[1].innerList[10]"));
    }

    @Test
    public void testSpecialCharactersInPaths() {
        Map<String, Object> map = new HashMap<>();
        map.put("key.with.dots", "value1");
        map.put("key[with]brackets", "value2");

        assertEquals("value1", map.get("key.with.dots"));
        assertEquals("value2", map.get("key[with]brackets"));

        Map<String, Object> nested = new HashMap<>();
        nested.put("special.key", "specialValue");
        map.put("normal", nested);

        assertNull(Maps.getByPath(map, "normal.special.key"));
    }

    @Test
    public void testCompleteCodeCoverage() {

        Map<String, Object> pathMap = new HashMap<>();
        pathMap.put("", "emptyKey");
        assertEquals("emptyKey", Maps.getByPath(pathMap, ""));

        Set<String> set = new LinkedHashSet<>();
        set.add("first");
        set.add("second");
        pathMap.put("set", set);
        assertEquals("first", Maps.getByPath(pathMap, "set[0]"));

        TestBean bean = new TestBean();
        bean.setName("Test");

        Map<String, Object> lowerUnderscoreMap = Beans.beanToMap(bean, (Collection<String>) null, NamingPolicy.SNAKE_CASE, IntFunctions.ofMap());
        assertTrue(lowerUnderscoreMap.containsKey("name"));

        Map<String, Object> upperUnderscoreMap = Beans.beanToMap(bean, (Collection<String>) null, NamingPolicy.SCREAMING_SNAKE_CASE, IntFunctions.ofMap());
        assertTrue(upperUnderscoreMap.containsKey("NAME"));
    }

    @Test
    public void testPrimitiveTypeConversions() {
        Map<String, Object> conversionMap = new HashMap<>();
        conversionMap.put("intAsString", "123");
        conversionMap.put("doubleAsString", "45.67");
        conversionMap.put("boolAsString", "true");
        conversionMap.put("boolAsInt", 1);
        conversionMap.put("hexString", "0xFF");

        assertEquals(123, Maps.getAsIntOrDefaultIfAbsent(conversionMap, "intAsString", 0));
        assertEquals(45.67, Maps.getAsDoubleOrDefaultIfAbsent(conversionMap, "doubleAsString", 0.0), 0.001);
        assertTrue(Maps.getAsBooleanOrDefaultIfAbsent(conversionMap, "boolAsString", false));

        conversionMap.put("invalidNumber", "not-a-number");
        try {
            Maps.getAsInt(conversionMap, "invalidNumber");
            fail("Should throw NumberFormatException");
        } catch (Exception e) {
        }
    }

    @Test
    public void testEdgeCasesForTypeConversions() {
        Map<String, Object> edgeCaseMap = new HashMap<>();

        edgeCaseMap.put("maxLong", Long.MAX_VALUE);
        edgeCaseMap.put("minLong", Long.MIN_VALUE);
        edgeCaseMap.put("infinity", Double.POSITIVE_INFINITY);
        edgeCaseMap.put("nan", Double.NaN);

        assertEquals(Long.MAX_VALUE, Maps.getAsLongOrDefaultIfAbsent(edgeCaseMap, "maxLong", 0L));
        assertEquals(Long.MIN_VALUE, Maps.getAsLongOrDefaultIfAbsent(edgeCaseMap, "minLong", 0L));
        assertTrue(Double.isInfinite(Maps.getAsDoubleOrDefaultIfAbsent(edgeCaseMap, "infinity", 0.0)));
        assertTrue(Double.isNaN(Maps.getAsDoubleOrDefaultIfAbsent(edgeCaseMap, "nan", 0.0)));

        edgeCaseMap.put("scientific", "1.23e4");
        assertEquals(12300.0, Maps.getAsDoubleOrDefaultIfAbsent(edgeCaseMap, "scientific", 0.0), 0.001);
    }

    @Test
    public void testIntersection() {
        Map<String, String> map1 = new HashMap<>();
        map1.put("a", "1");
        map1.put("b", "2");
        map1.put("c", "3");

        Map<String, String> map2 = new HashMap<>();
        map2.put("b", "2");
        map2.put("c", "3");
        map2.put("d", "4");

        Map<String, String> result = Maps.intersection(map1, map2);
        assertEquals(2, result.size());
        assertEquals("2", result.get("b"));
        assertEquals("3", result.get("c"));
        assertNull(result.get("a"));
        assertNull(result.get("d"));

        assertTrue(Maps.intersection(null, map2).isEmpty());
        assertTrue(Maps.intersection(map1, null).isEmpty());
        assertTrue(Maps.intersection(map1, new HashMap<>()).isEmpty());
        assertTrue(Maps.intersection(new HashMap<>(), intMap).isEmpty());
        assertTrue(Maps.intersection(new HashMap<String, Integer>(), new HashMap<String, Integer>()).isEmpty());
    }

    @Test
    public void testIntersection_NoCommon() {
        Map<String, Integer> map1 = new HashMap<>();
        map1.put("a", 1);

        Map<String, Integer> map2 = new HashMap<>();
        map2.put("b", 2);

        Map<String, Integer> intersection = Maps.intersection(map1, map2);
        assertTrue(intersection.isEmpty());
    }

    @Test
    public void test_difference() {
        Map<String, Integer> map = CommonUtil.asMap("a", 1, "b", 2, "c", 3);
        Map<String, Integer> map2 = CommonUtil.asMap("a", 1, "b", 3);

        assertEquals(CommonUtil.asMap("a", 1), Maps.intersection(map, map2));

        assertEquals(CommonUtil.asMap("b", Pair.of(2, Nullable.of(3)), "c", Pair.of(3, Nullable.empty())), Maps.difference(map, map2));

        assertEquals(CommonUtil.asMap("b", Pair.of(Nullable.of(2), Nullable.of(3)), "c", Pair.of(Nullable.of(3), Nullable.empty())),
                Maps.symmetricDifference(map, map2));

    }

    @Test
    public void testDifference() {
        Map<String, String> map1 = new HashMap<>();
        map1.put("a", "1");
        map1.put("b", "2");
        map1.put("c", "3");

        Map<String, String> map2 = new HashMap<>();
        map2.put("b", "2");
        map2.put("c", "different");
        map2.put("d", "4");

        Map<String, Pair<String, Nullable<String>>> result = Maps.difference(map1, map2);
        assertEquals(2, result.size());

        Pair<String, Nullable<String>> pairA = result.get("a");
        assertEquals("1", pairA.left());
        assertFalse(pairA.right().isPresent());

        Pair<String, Nullable<String>> pairC = result.get("c");
        assertEquals("3", pairC.left());
        assertTrue(pairC.right().isPresent());
        assertEquals("different", pairC.right().get());

        assertNull(result.get("b"));

        Map<String, Pair<String, Nullable<String>>> result2 = Maps.difference(map1, null);
        assertEquals("1", result2.get("a").left());
        assertEquals("3", result2.get("c").left());

        assertTrue(Maps.difference(null, intMap).isEmpty());
        assertEquals(0, Maps.difference(null, null).size());
    }

    @Test
    public void testSymmetricDifference() {
        Map<String, String> map1 = new HashMap<>();
        map1.put("a", "1");
        map1.put("b", "2");
        map1.put("c", "3");

        Map<String, String> map2 = new HashMap<>();
        map2.put("b", "2");
        map2.put("c", "different");
        map2.put("d", "4");

        Map<String, Pair<Nullable<String>, Nullable<String>>> result = Maps.symmetricDifference(map1, map2);
        assertEquals(3, result.size());

        Pair<Nullable<String>, Nullable<String>> pairA = result.get("a");
        assertTrue(pairA.left().isPresent());
        assertEquals("1", pairA.left().get());
        assertFalse(pairA.right().isPresent());

        Pair<Nullable<String>, Nullable<String>> pairD = result.get("d");
        assertFalse(pairD.left().isPresent());
        assertTrue(pairD.right().isPresent());
        assertEquals("4", pairD.right().get());

        Pair<Nullable<String>, Nullable<String>> pairC = result.get("c");
        assertTrue(pairC.left().isPresent());
        assertEquals("3", pairC.left().get());
        assertTrue(pairC.right().isPresent());
        assertEquals("different", pairC.right().get());

        assertNull(result.get("b"));
    }

    @Test
    public void testMapOperationsWithIdentityHashMap() {
        IdentityHashMap<String, String> identityMap1 = new IdentityHashMap<>();
        String key1 = new String("key");
        String key2 = new String("key");
        identityMap1.put(key1, "value1");

        IdentityHashMap<String, String> identityMap2 = new IdentityHashMap<>();
        identityMap2.put(key2, "value2");

        Map<String, Pair<Nullable<String>, Nullable<String>>> diff = Maps.symmetricDifference(identityMap1, identityMap2);
        assertTrue(diff instanceof IdentityHashMap);
    }

    @Test
    public void testSymmetricDifference_Map2Empty_AllEntriesInResult() {
        Map<String, Integer> map1 = new LinkedHashMap<>();
        map1.put("x", 10);
        map1.put("y", 20);
        Map<String, Pair<Nullable<Integer>, Nullable<Integer>>> result = Maps.symmetricDifference(map1, new HashMap<>());
        assertEquals(2, result.size());
        assertTrue(result.containsKey("x"));
        assertTrue(result.get("x").left().isPresent());
        assertFalse(result.get("x").right().isPresent());
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
    public void testContainsEntry() {
        Map.Entry<String, String> entry = CommonUtil.newEntry("key1", "value1");
        assertTrue(Maps.containsEntry(testMap, entry));

        Map.Entry<String, String> wrongValue = CommonUtil.newEntry("key1", "wrongValue");
        assertFalse(Maps.containsEntry(testMap, wrongValue));

        Map.Entry<String, String> missing = CommonUtil.newEntry("missing", "value");
        assertFalse(Maps.containsEntry(testMap, missing));
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
    public void testContainsEntry_nullMap() {
        assertFalse(Maps.containsEntry((Map<String, String>) null, CommonUtil.newEntry("a", "b")));
    }

    @Test
    public void testContainsEntry_emptyMap() {
        assertFalse(Maps.containsEntry(new HashMap<>(), CommonUtil.newEntry("a", "b")));
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
    @DisplayName("putIfAbsent puts value and returns null when key is absent")
    public void test_putIfAbsent_absentKey() {
        Map<String, String> map = new HashMap<>();

        String result = Maps.putIfAbsent(map, "key3", "value3");

        assertNull(result, "Should return null (no previous mapping)");
        assertEquals("value3", map.get("key3"), "Map should now have the new value");
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
    public void testPutIf_keyFilter() {
        Map<String, Integer> target = new HashMap<>(Map.of("a", 1));
        Map<String, Integer> source = Map.of("b", 2, "c", 3, "aa", 4);
        Predicate<String> keyFilter = k -> k.length() == 1;

        assertTrue(Maps.putAllIf(target, source, keyFilter));
        assertEquals(Map.of("a", 1, "b", 2, "c", 3), target);

        assertFalse(Maps.putAllIf(target, Map.of("bb", 5), keyFilter));
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
    public void testPutAllIf_keyPredicate_emptySource() {
        Map<String, Integer> target = new HashMap<>();
        assertFalse(Maps.putAllIf(target, new HashMap<>(), k -> true));
    }

    @Test
    public void testPutAllIf_biPredicate_emptySource() {
        Map<String, Integer> target = new HashMap<>();
        assertFalse(Maps.putAllIf(target, new HashMap<>(), (k, v) -> true));
    }

    @Test
    public void testConcurrentModification() {
        Map<String, String> map = new HashMap<>(testMap);

        try {
            Maps.removeIf(map, entry -> {
                map.put("newKey", "newValue");
                return true;
            });
            fail("Should throw ConcurrentModificationException");
        } catch (Exception e) {
        }
    }

    @Test
    public void testFilterBiPredicate() {
        Map<String, String> result = Maps.filter(testMap, (key, value) -> key.equals("key1") || value.equals("value3"));
        assertEquals(2, result.size());
        assertEquals("value1", result.get("key1"));
        assertEquals("value3", result.get("key3"));
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
    public void testFilter() {
        Map<String, String> result = Maps.filter(testMap, entry -> entry.getValue().endsWith("1") || entry.getValue().endsWith("3"));
        assertEquals(2, result.size());
        assertEquals("value1", result.get("key1"));
        assertEquals("value3", result.get("key3"));
        assertNull(result.get("key2"));

        assertTrue(Maps.filter(null, entry -> true).isEmpty());
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
    public void testFilter_emptyMap() {
        Map<String, String> result = Maps.filter(new HashMap<>(), e -> true);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testFilter_biPredicate_emptyMap() {
        Map<String, String> result = Maps.filter(new HashMap<>(), (k, v) -> true);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testFilter_NullMap_ReturnsEmptyMap() {
        Map<String, Integer> result = Maps.filter((Map<String, Integer>) null, (k, v) -> true);
        assertNotNull(result);
        assertTrue(result.isEmpty());
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
    public void testFilterByKey() {
        Map<String, String> result = Maps.filterByKey(testMap, key -> key.compareTo("key2") <= 0);
        assertEquals(2, result.size());
        assertTrue(result.containsKey("key1"));
        assertTrue(result.containsKey("key2"));
        assertFalse(result.containsKey("key3"));
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

    @Test
    public void testFilterByKey_emptyMap() {
        Map<String, String> result = Maps.filterByKey(new HashMap<>(), k -> true);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testFilterByValue() {
        Map<String, String> result = Maps.filterByValue(testMap, value -> value.compareTo("value2") >= 0);
        assertEquals(2, result.size());
        assertTrue(result.containsKey("key2"));
        assertTrue(result.containsKey("key3"));
        assertFalse(result.containsKey("key1"));
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
    @Disabled("Performance test - run manually")
    public void testPerformanceWithLargeMaps() {
        Map<String, Integer> veryLargeMap = new HashMap<>();
        for (int i = 0; i < 100000; i++) {
            veryLargeMap.put("key" + i, i);
        }

        long start = System.currentTimeMillis();
        Map<String, Integer> filtered = Maps.filterByValue(veryLargeMap, v -> v % 1000 == 0);
        long duration = System.currentTimeMillis() - start;

        assertEquals(100, filtered.size());
        System.out.println("Filter operation on 100k entries took: " + duration + "ms");

        start = System.currentTimeMillis();
        Map<Integer, String> inverted = Maps.invert(veryLargeMap);
        duration = System.currentTimeMillis() - start;

        assertEquals(100000, inverted.size());
        System.out.println("Invert operation on 100k entries took: " + duration + "ms");
    }

    @Test
    public void testFilterByValue_emptyMap() {
        Map<String, String> result = Maps.filterByValue(new HashMap<>(), v -> true);
        assertTrue(result.isEmpty());
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
    public void testInvert_withMerge_nullMap() {
        Map<String, String> result = Maps.invert(null, (a, b) -> a);
        assertTrue(result.isEmpty());
    }

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
    public void testFlatInvert_emptyMap() {
        Map<String, List<String>> result = Maps.flatInvert(new HashMap<>());
        assertTrue(result.isEmpty());
    }

    @Test
    public void testFlatToMap() {
        Map<String, List<Integer>> map = new HashMap<>();
        map.put("a", Arrays.asList(1, 2, 3));
        map.put("b", Arrays.asList(4, 5, 6));
        map.put("c", Arrays.asList(7, 8));

        List<Map<String, Integer>> result = Maps.transpose(map);
        assertEquals(3, result.size());

        Map<String, Integer> first = result.get(0);
        assertEquals(Integer.valueOf(1), first.get("a"));
        assertEquals(Integer.valueOf(4), first.get("b"));
        assertEquals(Integer.valueOf(7), first.get("c"));

        Map<String, Integer> second = result.get(1);
        assertEquals(Integer.valueOf(2), second.get("a"));
        assertEquals(Integer.valueOf(5), second.get("b"));
        assertEquals(Integer.valueOf(8), second.get("c"));

        Map<String, Integer> third = result.get(2);
        assertEquals(Integer.valueOf(3), third.get("a"));
        assertEquals(Integer.valueOf(6), third.get("b"));
        assertFalse(third.containsKey("c"));
    }

    @Test
    public void testFlatToMap_nullMap() {
        List<Map<String, String>> result = Maps.transpose(null);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testFlatToMap_emptyMap() {
        List<Map<String, String>> result = Maps.transpose(new HashMap<>());
        assertTrue(result.isEmpty());
    }

    @Test
    public void testFlatToMap_Basic() {
        Map<String, List<Integer>> map = new LinkedHashMap<>();
        map.put("a", Arrays.asList(1, 2, 3));
        map.put("b", Arrays.asList(4, 5));

        List<Map<String, Integer>> result = Maps.transpose(map);
        assertEquals(3, result.size());
        assertEquals(Integer.valueOf(1), result.get(0).get("a"));
        assertEquals(Integer.valueOf(4), result.get(0).get("b"));
        assertEquals(Integer.valueOf(2), result.get(1).get("a"));
        assertEquals(Integer.valueOf(5), result.get(1).get("b"));
        assertEquals(Integer.valueOf(3), result.get(2).get("a"));
        assertNull(result.get(2).get("b"));
        assertEquals(0, Maps.transpose(null).size());
    }

    @Test
    public void testFlatToMap_EntryWithEmptyCollection_Skipped() {
        Map<String, List<Integer>> map = new LinkedHashMap<>();
        map.put("a", Arrays.asList(1, 2));
        map.put("b", new ArrayList<>()); // empty collection - should be skipped
        map.put("c", Arrays.asList(3));
        List<Map<String, Integer>> result = Maps.transpose(map);
        assertEquals(2, result.size());
        // "b" not in results since it had empty collection
        assertFalse(result.get(0).containsKey("b"));
    }

    @Test
    public void testFlatten_withSupplier() {
        Map<String, Object> map = new HashMap<>();
        map.put("a", CommonUtil.asMap("b", 1));
        LinkedHashMap<String, Object> result = Maps.flatten(map, LinkedHashMap::new);
        assertEquals(1, result.get("a.b"));
        assertTrue(result instanceof LinkedHashMap);
    }

    @Test
    public void testFlatten() {
        Map<String, Object> map = new HashMap<>();
        map.put("simple", "value");

        Map<String, Object> nested = new HashMap<>();
        nested.put("inner", "innerValue");
        map.put("nested", nested);

        Map<String, Object> deepNested = new HashMap<>();
        deepNested.put("deep", "deepValue");
        nested.put("level2", deepNested);

        Map<String, Object> flattened = Maps.flatten(map);
        assertEquals(3, flattened.size());
        assertEquals("value", flattened.get("simple"));
        assertEquals("innerValue", flattened.get("nested.inner"));
        assertEquals("deepValue", flattened.get("nested.level2.deep"));

        Map<String, Object> deep = CommonUtil.asMap("a", CommonUtil.asMap("b", CommonUtil.asMap("c", CommonUtil.asMap("d", 4), "c2", 3), "b2", 2), "a2", 1);
        assertEquals(deep, Maps.unflatten(Maps.flatten(deep)));
        assertTrue(Maps.flatten(null).isEmpty());
        assertTrue(Maps.flatten(new HashMap<>()).isEmpty());
    }

    @Test
    public void testFlattenWithCustomDelimiter() {
        Map<String, Object> map = new HashMap<>();
        Map<String, Object> nested = new HashMap<>();
        nested.put("inner", "value");
        map.put("outer", nested);

        Map<String, Object> flattened = Maps.flatten(map, "_", HashMap::new);
        assertEquals("value", flattened.get("outer_inner"));
    }

    @Test
    public void testFlattenWithSupplier() {
        Map<String, Object> map = new HashMap<>();
        Map<String, Object> nested = new HashMap<>();
        nested.put("key", "value");
        map.put("outer", nested);

        LinkedHashMap<String, Object> flattened = Maps.flatten(map, size -> new LinkedHashMap<>());
        assertTrue(flattened instanceof LinkedHashMap);
        assertEquals("value", flattened.get("outer.key"));
    }

    @Test
    public void testFlattenAndUnflatten() {
        Map<String, Object> nestedMap = new HashMap<>();
        Map<String, Object> inner = new HashMap<>();
        inner.put("c", 3);
        nestedMap.put("a", 1);
        nestedMap.put("b", inner);

        Map<String, Object> flat = Maps.flatten(nestedMap);
        assertEquals(1, flat.get("a"));
        assertEquals(3, flat.get("b.c"));
        assertEquals(2, flat.size());

        Map<String, Object> unflattened = Maps.unflatten(flat);
        assertEquals(1, unflattened.get("a"));
        assertTrue(unflattened.get("b") instanceof Map);
        assertEquals(3, ((Map<?, ?>) unflattened.get("b")).get("c"));

        Map<String, Object> flatCustom = Maps.flatten(nestedMap, "_", HashMap::new);
        assertEquals(3, flatCustom.get("b_c"));
        Map<String, Object> unflattenedCustom = Maps.unflatten(flatCustom, "_", HashMap::new);
        assertEquals(3, ((Map<?, ?>) unflattenedCustom.get("b")).get("c"));
    }

    @Test
    public void testNestedMapConversions() {
        Map<String, Object> deepMap = new HashMap<>();
        Map<String, Object> level1 = new HashMap<>();
        Map<String, Object> level2 = new HashMap<>();
        Map<String, Object> level3 = new HashMap<>();

        level3.put("deep", "value");
        level2.put("level3", level3);
        level1.put("level2", level2);
        deepMap.put("level1", level1);

        Map<String, Object> flattened = Maps.flatten(deepMap);
        assertEquals("value", flattened.get("level1.level2.level3.deep"));

        Map<String, Object> unflattened = Maps.unflatten(flattened);
        assertEquals(deepMap, unflattened);
    }

    @Test
    public void testUnflatten() {
        Map<String, Object> flat = new HashMap<>();
        flat.put("simple", "value");
        flat.put("nested.inner", "innerValue");
        flat.put("nested.level2.deep", "deepValue");

        Map<String, Object> unflattened = Maps.unflatten(flat);
        assertEquals(2, unflattened.size());
        assertEquals("value", unflattened.get("simple"));

        @SuppressWarnings("unchecked")
        Map<String, Object> nested = (Map<String, Object>) unflattened.get("nested");
        assertNotNull(nested);
        assertEquals("innerValue", nested.get("inner"));

        @SuppressWarnings("unchecked")
        Map<String, Object> level2 = (Map<String, Object>) nested.get("level2");
        assertNotNull(level2);
        assertEquals("deepValue", level2.get("deep"));
        assertTrue(Maps.unflatten(null).isEmpty());
        assertTrue(Maps.unflatten(new HashMap<>()).isEmpty());
    }

    @Test
    public void testUnflattenWithCustomDelimiter() {
        Map<String, Object> flat = new HashMap<>();
        flat.put("outer_inner", "value");

        Map<String, Object> unflattened = Maps.unflatten(flat, "_", HashMap::new);
        @SuppressWarnings("unchecked")
        Map<String, Object> outer = (Map<String, Object>) unflattened.get("outer");
        assertNotNull(outer);
        assertEquals("value", outer.get("inner"));
    }

    @Test
    public void testUnflattenWithSupplier() {
        Map<String, Object> flat = new HashMap<>();
        flat.put("a.b.c", "value");

        Map<String, Object> unflattened = Maps.unflatten(flat, size -> new LinkedHashMap<>());
        assertTrue(unflattened instanceof LinkedHashMap);

        @SuppressWarnings("unchecked")
        Map<String, Object> a = (Map<String, Object>) unflattened.get("a");
        assertNotNull(a);

        @SuppressWarnings("unchecked")
        Map<String, Object> b = (Map<String, Object>) a.get("b");
        assertNotNull(b);
        assertEquals("value", b.get("c"));
    }

    @Test
    public void testUnflatten_withSupplier() {
        Map<String, Object> flat = new HashMap<>();
        flat.put("a.b", 1);
        LinkedHashMap<String, Object> result = Maps.unflatten(flat, LinkedHashMap::new);
        assertNotNull(result.get("a"));
        assertTrue(result instanceof LinkedHashMap);
    }

    @Test
    public void test_beanToMap() {
        Account account1 = Beans.newRandomBean(Account.class);
        Map<String, Object> map1 = Beans.beanToMap(account1);
        assertFalse(map1.isEmpty());

        Map<String, Object> output = new HashMap<>();
        Beans.beanToMap(account1, output);
        assertEquals(map1, output);

        Map<String, Object> map2 = Beans.beanToMap(account1, IntFunctions.ofMap());
        assertEquals(map1, map2);
    }

    @Test
    public void test_MapDifference() {
        Account account1 = Beans.newRandomBean(Account.class);
        Account account2 = Beans.newRandomBean(Account.class);
        account2.setFirstName(account1.getFirstName());

        BeanDifference<Map<String, Object>, Map<String, Object>, Map<String, Pair<Object, Object>>> mapDiff = BeanDifference.of(account1, account2);
        assertEquals(account1.getFirstName(), mapDiff.common().get("firstName"));
        assertNotNull(mapDiff.onlyOnLeft());
        assertNotNull(mapDiff.onlyOnRight());
        assertNotNull(mapDiff.differentValues());

        mapDiff = BeanDifference.of(account1, account2, CommonUtil.toList("id", "firstName", "lastName2"));
        assertEquals(account1.getFirstName(), mapDiff.common().get("firstName"));
        assertFalse(mapDiff.common().containsKey("lastName2"));
    }

    @Test
    public void testBean2Map_simple() {
        SimpleBean bean = new SimpleBean(1, "test");
        Map<String, Object> map = Beans.beanToMap(bean);
        assertEquals(Map.of("id", 1, "value", "test"), map);
    }

    @Test
    public void testMap2Bean_simple() {
        Map<String, Object> map = Map.of("id", 10, "value", "hello", "extra", "ignored");
        SimpleBean bean = Beans.mapToBean(map, SimpleBean.class);
        assertEquals(10, bean.id);
        assertEquals("hello", bean.value);

        Map<String, Object> mapNoMatch = Map.of("id", 20, "value", "world", "extraField", "data");
        assertThrows(IllegalArgumentException.class, () -> Beans.mapToBean(mapNoMatch, false, SimpleBean.class));

    }

    @Test
    public void testDeepBean2Map() {
        Address address = new Address("NY", "10001");
        Person person = new Person("John", 30, address);
        Map<String, Object> map = Beans.deepBeanToMap(person);

        assertEquals("John", map.get("name"));
        assertEquals(30, map.get("age"));
        assertTrue(map.get("address") instanceof Map);
        Map<?, ?> addrMap = (Map<?, ?>) map.get("address");
        assertEquals("NY", addrMap.get("city"));
        assertEquals("10001", addrMap.get("zip"));
    }

    @Test
    public void testBean2FlatMap() {
        Address address = new Address("LA", "90001");
        Person person = new Person("Jane", 25, address);
        Map<String, Object> flatMap = Beans.beanToFlatMap(person);

        assertEquals("Jane", flatMap.get("name"));
        assertEquals(25, flatMap.get("age"));
        assertEquals("LA", flatMap.get("address.city"));
        assertEquals("90001", flatMap.get("address.zip"));
    }

    @Test
    public void testMap2Bean() {
        Map<String, Object> map = new HashMap<>();
        map.put("name", "John");
        map.put("age", 30);
        map.put("active", true);

        TestBean bean = Beans.mapToBean(map, TestBean.class);
        assertNotNull(bean);
        assertEquals("John", bean.getName());
        assertEquals(30, bean.getAge());
        assertTrue(bean.isActive());

        assertNull(Beans.mapToBean((Map) null, TestBean.class));
    }

    @Test
    public void testMap2BeanWithFlags() {
        Map<String, Object> map = new HashMap<>();
        map.put("name", "John");
        map.put("age", null);
        map.put("unknownProperty", "value");

        TestBean bean1 = Beans.mapToBean(map, true, TestBean.class);
        assertEquals("John", bean1.getName());
        assertEquals(0, bean1.getAge());

        try {
            Beans.mapToBean(map, false, TestBean.class);
            fail("Should throw exception for unmatched property");
        } catch (Exception e) {
        }
    }

    @Test
    public void testMap2BeanWithSelectProps() {
        Map<String, Object> map = new HashMap<>();
        map.put("name", "John");
        map.put("age", 30);
        map.put("active", true);

        TestBean bean = Beans.mapToBean(map, Arrays.asList("name", "age"), TestBean.class);
        assertEquals("John", bean.getName());
        assertEquals(30, bean.getAge());
        assertFalse(bean.isActive());
    }

    @Test
    public void testMap2BeanCollection() {
        Map<String, Object> map1 = new HashMap<>();
        map1.put("name", "John");
        map1.put("age", 30);

        Map<String, Object> map2 = new HashMap<>();
        map2.put("name", "Jane");
        map2.put("age", 25);

        List<Map<String, Object>> maps = Arrays.asList(map1, map2);
        List<TestBean> beans = Beans.mapsToBeans(maps, TestBean.class);

        assertEquals(2, beans.size());
        assertEquals("John", beans.get(0).getName());
        assertEquals(30, beans.get(0).getAge());
        assertEquals("Jane", beans.get(1).getName());
        assertEquals(25, beans.get(1).getAge());

        List<TestBean> beans2 = Beans.mapsToBeans(maps, CommonUtil.toList("name"), TestBean.class);

        assertEquals(2, beans2.size());
        assertEquals("John", beans2.get(0).getName());
        assertEquals(0, beans2.get(0).getAge());
        assertEquals("Jane", beans2.get(1).getName());
        assertEquals(0, beans2.get(1).getAge());
    }

    @Test
    public void testBean2Map() {
        TestBean bean = new TestBean();
        bean.setName("John");
        bean.setAge(30);
        bean.setActive(true);

        Map<String, Object> map = Beans.beanToMap(bean);
        assertEquals("John", map.get("name"));
        assertEquals(30, map.get("age"));
        assertEquals(true, map.get("active"));
    }

    @Test
    public void testBean2MapWithMapSupplier() {
        TestBean bean = new TestBean();
        bean.setName("John");

        Map<String, Object> map = Beans.beanToMap(bean, IntFunctions.ofLinkedHashMap());
        assertTrue(map instanceof LinkedHashMap);
        assertEquals("John", map.get("name"));
    }

    @Test
    public void testBean2MapWithSelectProps() {
        TestBean bean = new TestBean();
        bean.setName("John");
        bean.setAge(30);
        bean.setActive(true);

        Map<String, Object> map = Beans.beanToMap(bean, Arrays.asList("name", "age"));
        assertEquals(2, map.size());
        assertEquals("John", map.get("name"));
        assertEquals(30, map.get("age"));
        assertFalse(map.containsKey("active"));
    }

    @Test
    public void testBean2MapWithNamingPolicy() {
        TestBean bean = new TestBean();
        bean.setName("John");
        bean.setAge(30);

        Map<String, Object> map = Beans.beanToMap(bean, null, NamingPolicy.SCREAMING_SNAKE_CASE, IntFunctions.ofMap());
        assertEquals("John", map.get("NAME"));
        assertEquals(30, map.get("AGE"));
    }

    @Test
    public void testBean2MapIgnoreNull() {
        TestBean bean = new TestBean();
        bean.setName("John");
        bean.setNullableField(null);

        Map<String, Object> map1 = Beans.beanToMap(bean, true);
        assertFalse(map1.containsKey("nullableField"));

        Map<String, Object> map2 = Beans.beanToMap(bean, false);
        assertTrue(map2.containsKey("nullableField"));
        assertNull(map2.get("nullableField"));
    }

    @Test
    public void testBean2MapWithIgnoredProps() {
        TestBean bean = new TestBean();
        bean.setName("John");
        bean.setAge(30);
        bean.setActive(true);

        Set<String> ignored = new HashSet<>(Arrays.asList("age", "active"));
        Map<String, Object> map = Beans.beanToMap(bean, false, ignored);

        assertEquals(3, map.size());
        assertEquals("John", map.get("name"));
        assertFalse(map.containsKey("age"));
        assertFalse(map.containsKey("active"));
    }

    @Test
    public void testBean2MapWithComplexTypes() {
        ComplexBean bean = new ComplexBean();
        bean.setDate(new Date());
        bean.setBigDecimal(new BigDecimal("123.456"));
        bean.setStringList(Arrays.asList("a", "b", "c"));
        bean.setIntArray(new int[] { 1, 2, 3 });

        Map<String, Object> map = Beans.beanToMap(bean);
        assertNotNull(map.get("date"));
        assertEquals(bean.getBigDecimal(), map.get("bigDecimal"));
        assertEquals(bean.getStringList(), map.get("stringList"));
        assertArrayEquals(bean.getIntArray(), (int[]) map.get("intArray"));
    }

    @Test
    public void testMap2BeanWithNestedMaps() {
        Map<String, Object> map = new HashMap<>();
        map.put("name", "Parent");

        Map<String, Object> nestedMap = new HashMap<>();
        nestedMap.put("value", "NestedValue");
        map.put("nestedBean", nestedMap);

        TestBean bean = Beans.mapToBean(map, TestBean.class);
        assertEquals("Parent", bean.getName());
        assertNotNull(bean.getNestedBean());
        assertEquals("NestedValue", bean.getNestedBean().getValue());
    }

    @Test
    public void testCircularReferenceHandling() {
        CircularBean bean1 = new CircularBean();
        CircularBean bean2 = new CircularBean();
        bean1.setName("Bean1");
        bean2.setName("Bean2");
        bean1.setReference(bean2);
        bean2.setReference(bean1);

        Map<String, Object> map = Beans.beanToMap(bean1);
        assertEquals("Bean1", map.get("name"));
        assertEquals(bean2, map.get("reference"));

        // A reference cycle is rejected rather than silently truncated (and never blows the stack).
        assertThrows(IllegalArgumentException.class, () -> Beans.deepBeanToMap(bean1));
        assertThrows(IllegalArgumentException.class, () -> Beans.beanToFlatMap(bean1));
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
    public void testDifferenceSymmetricDifference_covariantSecondMap() {
        final Map<String, Number> first = new LinkedHashMap<>();
        first.put("a", 1);
        first.put("b", 2);

        // Integer <: Number — only compiles because the second param was widened to Map<? extends K, ? extends V>
        final Map<String, Integer> second = new LinkedHashMap<>();
        second.put("a", 1);
        second.put("c", 9);

        final Map<String, Pair<Number, Nullable<Number>>> diff = Maps.difference(first, second);
        assertFalse(diff.containsKey("a")); // identical value -> excluded
        assertTrue(diff.containsKey("b")); // only in first
        assertEquals(2, diff.get("b").left().intValue());
        assertFalse(diff.get("b").right().isPresent());

        final Map<String, Pair<Nullable<Number>, Nullable<Number>>> sym = Maps.symmetricDifference(first, second);
        assertFalse(sym.containsKey("a")); // identical
        assertTrue(sym.containsKey("b")); // only in first
        assertTrue(sym.containsKey("c")); // only in second
        assertEquals(9, sym.get("c").right().get().intValue());
        assertFalse(sym.get("c").left().isPresent());
    }

    @Test
    public void testFlattenUnflatten_intFunctionSupplier() {
        final Map<String, Object> map = new LinkedHashMap<>();
        map.put("name", "John");
        final Map<String, Object> address = new LinkedHashMap<>();
        address.put("city", "NYC");
        map.put("address", address);

        final int[] sizeHint = { -1 };
        final IntFunction<LinkedHashMap<String, Object>> supplier = size -> {
            sizeHint[0] = size;
            return new LinkedHashMap<>();
        };

        final LinkedHashMap<String, Object> flat = Maps.flatten(map, supplier);
        assertEquals("John", flat.get("name"));
        assertEquals("NYC", flat.get("address.city"));
        assertEquals(map.size(), sizeHint[0]); // top-level expected-size hint forwarded to the supplier

        @SuppressWarnings("unchecked")
        final Map<String, Object> round = (Map<String, Object>) Maps.unflatten(flat, LinkedHashMap::new).get("address");
        assertEquals("NYC", round.get("city"));
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
        assertThrows(IllegalArgumentException.class, () -> Maps.filter(map, (k, v) -> true, ignored -> (Map<String, Integer>) null));
        assertThrows(IllegalArgumentException.class, () -> Maps.filter((Map<String, Integer>) null, (k, v) -> true, ignored -> (Map<String, Integer>) null));
    }

    @Test
    public void testFlatten_rejectsCyclesAndFlattenedKeyCollisions() {
        final Map<String, Object> cyclic = new HashMap<>();
        cyclic.put("self", cyclic);
        assertThrows(IllegalArgumentException.class, () -> Maps.flatten(cyclic));

        final Map<String, Object> colliding = new LinkedHashMap<>();
        colliding.put("a.b", 1);
        colliding.put("a", CommonUtil.asMap("b", 2));
        assertThrows(IllegalArgumentException.class, () -> Maps.flatten(colliding));

        assertThrows(IllegalArgumentException.class, () -> Maps.flatten(new HashMap<>(), "", HashMap::new));
        assertThrows(IllegalArgumentException.class, () -> Maps.flatten(new HashMap<>(), ".", (IntFunction<Map<String, Object>>) null));
        assertThrows(IllegalArgumentException.class, () -> Maps.flatten(new HashMap<>(), ".", ignored -> (Map<String, Object>) null));
    }

    @Test
    public void testFlatten_preservesEmptyNestedKeyPathSegment() {
        final Map<String, Object> nestedUnderEmptyKey = new LinkedHashMap<>();
        nestedUnderEmptyKey.put("", CommonUtil.asMap("a", 1));

        final Map<String, Object> flattened = Maps.flatten(nestedUnderEmptyKey);
        assertEquals(CommonUtil.asMap(".a", 1), flattened);
        assertEquals(nestedUnderEmptyKey, Maps.unflatten(flattened));
    }

    @Test
    public void testUnflattenRejectsMapValuedLeafConflictsWithoutChangingInput() {
        for (final String delimiter : Arrays.asList(".", "/")) {
            for (final String prefix : Arrays.asList("a", "a" + delimiter + "b")) {
                for (final boolean leafFirst : Arrays.asList(true, false)) {
                    final Map<String, Object> leaf = new LinkedHashMap<>();
                    leaf.put("original", 1);
                    final Map<String, Object> flat = new LinkedHashMap<>();
                    if (leafFirst) {
                        flat.put(prefix, leaf);
                        flat.put(prefix + delimiter + "child", 2);
                    } else {
                        flat.put(prefix + delimiter + "child", 2);
                        flat.put(prefix, leaf);
                    }

                    assertThrows(IllegalArgumentException.class, () -> Maps.unflatten(flat, delimiter, LinkedHashMap::new));
                    assertEquals(Map.of("original", 1), leaf);
                    assertEquals(2, flat.size());
                }
            }
        }

        final Map<String, Object> selfLeaf = new LinkedHashMap<>();
        selfLeaf.put("a", selfLeaf);
        selfLeaf.put("a.child", 2);
        assertThrows(IllegalArgumentException.class, () -> Maps.unflatten(selfLeaf));
        assertEquals(2, selfLeaf.size());
        assertSame(selfLeaf, selfLeaf.get("a"));
    }

    @Test
    public void testUnflattenPreservesNonconflictingMapLeafAliases() {
        final Map<String, Object> leaf = new LinkedHashMap<>();
        leaf.put("value", 1);
        final Map<String, Object> flat = new LinkedHashMap<>();
        flat.put("first.leaf", leaf);
        flat.put("first.sibling", 2);
        flat.put("second", leaf);

        final Map<String, Object> result = Maps.unflatten(flat, ".", LinkedHashMap::new);
        final Map<?, ?> first = (Map<?, ?>) result.get("first");
        assertSame(leaf, first.get("leaf"));
        assertSame(leaf, result.get("second"));
        assertEquals(2, first.get("sibling"));
        assertEquals(Map.of("value", 1), leaf);
        assertEquals(3, flat.size());
    }

    @Test
    public void testUnflatten_rejectsConflictingPathsInEitherOrder() {
        final Map<String, Object> scalarFirst = new LinkedHashMap<>();
        scalarFirst.put("a", null);
        scalarFirst.put("a.b", 2);
        assertThrows(IllegalArgumentException.class, () -> Maps.unflatten(scalarFirst));

        final Map<String, Object> nestedFirst = new LinkedHashMap<>();
        nestedFirst.put("a.b", 2);
        nestedFirst.put("a", 1);
        assertThrows(IllegalArgumentException.class, () -> Maps.unflatten(nestedFirst));

        assertThrows(IllegalArgumentException.class, () -> Maps.unflatten(new HashMap<>(), "", HashMap::new));
        assertThrows(IllegalArgumentException.class, () -> Maps.unflatten(new HashMap<>(), ".", (IntFunction<Map<String, Object>>) null));
        assertThrows(IllegalArgumentException.class, () -> Maps.unflatten(new HashMap<>(), ".", ignored -> (Map<String, Object>) null));
    }

    @Test
    public void testUnflatten_rejectsSupplierMapIdentityReuse() {
        final Map<String, Object> oneLevel = new LinkedHashMap<>();
        oneLevel.put("a.b", 1);

        final Map<String, Object> reusedRoot = new LinkedHashMap<>();
        assertThrows(IllegalArgumentException.class, () -> Maps.unflatten(oneLevel, ignored -> reusedRoot));

        final Map<String, Object> deep = new LinkedHashMap<>();
        deep.put("a.b.c", 1);
        final Map<String, Object> deepRoot = new LinkedHashMap<>();
        final Map<String, Object> reusedNested = new LinkedHashMap<>();
        final int[] deepCalls = { 0 };
        assertThrows(IllegalArgumentException.class, () -> Maps.unflatten(deep, ignored -> deepCalls[0]++ == 0 ? deepRoot : reusedNested));

        final Map<String, Object> siblings = new LinkedHashMap<>();
        siblings.put("a.x", 1);
        siblings.put("b.y", 2);
        final Map<String, Object> siblingRoot = new LinkedHashMap<>();
        final Map<String, Object> reusedSibling = new LinkedHashMap<>();
        final int[] siblingCalls = { 0 };
        assertThrows(IllegalArgumentException.class, () -> Maps.unflatten(siblings, ignored -> siblingCalls[0]++ == 0 ? siblingRoot : reusedSibling));

        final Map<String, Object> inputRoot = new LinkedHashMap<>();
        inputRoot.put("a.b", 1);
        assertThrows(IllegalArgumentException.class, () -> Maps.unflatten(inputRoot, ignored -> inputRoot));
        assertEquals(CommonUtil.asMap("a.b", 1), inputRoot);

        final Map<String, Object> separateRoot = new LinkedHashMap<>();
        final int[] inputCalls = { 0 };
        assertThrows(IllegalArgumentException.class, () -> Maps.unflatten(inputRoot, ignored -> inputCalls[0]++ == 0 ? separateRoot : inputRoot));
        assertEquals(CommonUtil.asMap("a.b", 1), inputRoot);
    }

    @Test
    public void testDifference_arrayValuesAreComparedByContent() {
        final Map<String, Object> map1 = CommonUtil.asMap("id", 1, "data", new byte[] { 1, 2, 3 });
        final Map<String, Object> map2 = CommonUtil.asMap("id", 1, "data", new byte[] { 1, 2, 3 }); // equal content, distinct references

        assertTrue(Maps.difference(map1, map2).isEmpty());

        final Map<String, Object> map3 = CommonUtil.asMap("id", 1, "data", new byte[] { 1, 2, 4 });
        final Map<String, Pair<Object, Nullable<Object>>> diff = Maps.difference(map1, map3);

        assertEquals(1, diff.size());
        assertTrue(diff.containsKey("data"));
    }

    @Test
    public void testDifference_nestedArrayValuesAreComparedByContent() {
        final Map<String, Object> map1 = CommonUtil.asMap("matrix", new int[][] { { 1, 2 }, { 3 } });
        final Map<String, Object> map2 = CommonUtil.asMap("matrix", new int[][] { { 1, 2 }, { 3 } });

        assertTrue(Maps.difference(map1, map2).isEmpty());

        final Map<String, Object> map3 = CommonUtil.asMap("matrix", new int[][] { { 1, 2 }, { 4 } });
        assertEquals(1, Maps.difference(map1, map3).size());
    }

    @Test
    public void testSymmetricDifference_arrayValuesAreComparedByContent() {
        final Map<String, Object> map1 = CommonUtil.asMap("id", 1, "data", new byte[] { 1, 2, 3 });
        final Map<String, Object> map2 = CommonUtil.asMap("id", 1, "data", new byte[] { 1, 2, 3 });

        assertTrue(Maps.symmetricDifference(map1, map2).isEmpty());

        final Map<String, Object> map3 = CommonUtil.asMap("id", 1, "data", new byte[] { 1, 2, 4 });
        final Map<String, Pair<Nullable<Object>, Nullable<Object>>> diff = Maps.symmetricDifference(map1, map3);

        assertEquals(1, diff.size());
        assertTrue(diff.containsKey("data"));
    }

    @Test
    public void testIntersection_arrayValuesAreComparedByContent() {
        final Map<String, Object> map1 = CommonUtil.asMap("id", 1, "data", new byte[] { 1, 2, 3 });
        final Map<String, Object> map2 = CommonUtil.asMap("id", 1, "data", new byte[] { 1, 2, 3 });

        // intersection must agree with difference: an equal-content entry belongs to exactly one of them
        assertEquals(2, Maps.intersection(map1, map2).size());
        assertTrue(Maps.difference(map1, map2).isEmpty());

        final Map<String, Object> map3 = CommonUtil.asMap("id", 1, "data", new byte[] { 1, 2, 4 });
        assertEquals(1, Maps.intersection(map1, map3).size());
        assertEquals(1, Maps.difference(map1, map3).size());
    }

    @Test
    public void testSymmetricDifference_secondMapDoesNotDictateKeySemantics() {
        final Map<String, Integer> hashMap = new HashMap<>();
        hashMap.put(new String("a"), 1);

        final IdentityHashMap<String, Integer> identityMap = new IdentityHashMap<>();
        identityMap.put("b", 2);

        final Map<String, Pair<Nullable<Integer>, Nullable<Integer>>> result = Maps.symmetricDifference(hashMap, identityMap);

        // previously an IdentityHashMap, so the entry keyed by hashMap's own "a" could not be looked up
        assertFalse(result instanceof IdentityHashMap);
        assertEquals(2, result.size());
        assertNotNull(result.get("a"));
        assertEquals(Nullable.of(1), result.get("a").left());
        assertTrue(result.get("a").right().isEmpty());
    }

    @Test
    public void testSymmetricDifference_firstMapStillDictatesKeySemantics() {
        final IdentityHashMap<String, Integer> identityMap = new IdentityHashMap<>();
        identityMap.put("a", 1);
        final Map<String, Integer> hashMap = CommonUtil.asMap("b", 2);

        assertTrue(Maps.symmetricDifference(identityMap, hashMap) instanceof IdentityHashMap);
        // and it agrees with its siblings, which derive the same thing from the first map
        assertTrue(Maps.difference(identityMap, hashMap) instanceof IdentityHashMap);
        assertTrue(Maps.intersection(identityMap, hashMap) instanceof IdentityHashMap);
    }

    @Test
    public void testSymmetricDifference_nullFirstMapMirrorsSecond() {
        final TreeMap<String, Integer> sorted = new TreeMap<>();
        sorted.put("z", 1);
        sorted.put("a", 2);

        // every key comes from map2 here, so map2 is the right template
        final Map<String, Pair<Nullable<Integer>, Nullable<Integer>>> result = Maps.symmetricDifference(null, sorted);
        assertTrue(result instanceof TreeMap);
        assertEquals(CommonUtil.asList("a", "z"), new ArrayList<>(result.keySet()));

        assertNotNull(Maps.symmetricDifference((Map<String, Integer>) null, (Map<String, Integer>) null));
        assertTrue(Maps.symmetricDifference((Map<String, Integer>) null, (Map<String, Integer>) null).isEmpty());
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
    public void testPutAllIf_nullTargetMapThrowsRegardlessOfSource() {
        final Map<String, Integer> source = CommonUtil.asMap("a", 1);

        // an empty source used to hide the null target and answer false
        assertThrows(IllegalArgumentException.class, () -> Maps.putAllIf(null, new HashMap<String, Integer>(), k -> true));
        assertThrows(IllegalArgumentException.class, () -> Maps.putAllIf(null, source, k -> true));
        assertThrows(IllegalArgumentException.class, () -> Maps.putAllIf(null, new HashMap<String, Integer>(), (k, v) -> true));
        assertThrows(IllegalArgumentException.class, () -> Maps.putAllIf(null, source, (k, v) -> true));
    }

    @Test
    public void testNoOpMutators_stillTolerateNullMap() {
        assertFalse(Maps.removeIf((Map<String, Integer>) null, e -> true));
        assertFalse(Maps.removeIf((Map<String, Integer>) null, (k, v) -> true));
        assertFalse(Maps.removeIfKey((Map<String, Integer>) null, k -> true));
        assertFalse(Maps.removeIfValue((Map<String, Integer>) null, v -> true));
        assertFalse(Maps.removeKeys(null, CommonUtil.asList("a")));
        assertFalse(Maps.removeEntries(null, CommonUtil.asMap("a", 1)));
        assertDoesNotThrow(() -> Maps.replaceAll((Map<String, Integer>) null, (k, v) -> v));
        assertDoesNotThrow(() -> Maps.replaceKeys((Map<String, Integer>) null, k -> k));
        assertDoesNotThrow(() -> Maps.replaceKeysWithCamelCase(null));
    }

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
    public void testSupplierReturningNull_hasAnExplanatoryMessage() {
        final Map<String, String> map = new HashMap<>();

        final IllegalArgumentException e1 = assertThrows(IllegalArgumentException.class, () -> Maps.getOrDefaultIfAbsent(map, "x", () -> null));
        assertEquals("defaultValueSupplier returned null", e1.getMessage());

        final IllegalArgumentException e2 = assertThrows(IllegalArgumentException.class, () -> Maps.getOrPutIfAbsent(map, "x", () -> null));
        assertEquals("defaultValueSupplier returned null", e2.getMessage());

        // the map must be left untouched when the supplier misbehaves
        assertTrue(map.isEmpty());
    }

    @Test
    public void testIntersection_stillWorksWithCompatibleMaps() {
        final Map<String, Integer> map1 = CommonUtil.asMap("a", 1, "b", 2);
        final Map<String, Integer> map2 = CommonUtil.asMap("a", 1, "b", 9);

        assertEquals(CommonUtil.asMap("a", 1), Maps.intersection(map1, map2));
        assertTrue(Maps.intersection(map1, null).isEmpty());
        assertTrue(Maps.intersection(null, map2).isEmpty());
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

    @Test
    public void reviewFixes20260906_getAsTypeConvertsContainerElements() {
        final Map<String, Object> map = new HashMap<>();
        map.put("nums", Arrays.asList(1, 2, 3));

        // Type.javaType() erases the parameters, so the old isAssignableFrom fast path handed the stored
        // List<Integer> straight back for a Type<List<String>> - a ClassCastException at first read.
        final com.landawn.abacus.util.u.Optional<List<String>> got = Maps.getAs(map, "nums", new com.landawn.abacus.util.TypeReference<List<String>>() {
        }.type());

        assertTrue(got.isPresent());
        assertEquals(Arrays.asList("1", "2", "3"), got.get());
        assertEquals("1", got.get().get(0));
        assertNotSame(map.get("nums"), got.get());

        // A Map target is converted too.
        final Map<String, Object> m2 = new HashMap<>();
        m2.put("m", CommonUtil.asMap(1, 2));
        final com.landawn.abacus.util.u.Optional<Map<String, String>> gotMap = Maps.getAs(m2, "m",
                new com.landawn.abacus.util.TypeReference<Map<String, String>>() {
                }.type());
        assertEquals(CommonUtil.asMap("1", "2"), gotMap.get());

        // A scalar target still takes the identity fast path.
        final Map<String, Object> m3 = new HashMap<>();
        final String stored = "abc";
        m3.put("s", stored);
        assertSame(stored, Maps.getAs(m3, "s", com.landawn.abacus.type.TypeFactory.getType(String.class)).get());

        // An unconstrained container target is still returned as-is (N.convert short-circuits it).
        final Map<String, Object> m4 = new HashMap<>();
        final List<Object> raw = new ArrayList<>(Arrays.asList(1, 2));
        m4.put("l", raw);
        assertEquals(Arrays.asList(1, 2), Maps.getAs(m4, "l", new com.landawn.abacus.util.TypeReference<List<Object>>() {
        }.type()).get());
    }

    @Test
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public void reviewFixes20260908_getAsTypeSkipsTheIdentityFastPathForEveryParameterizedDescriptor() {
        // The fast path used to be skipped by enumerating collection/map/array, so every OTHER parameterized
        // handler - Optional<T>, Pair<K, V>, a generic bean - still got the erased isAssignableFrom answer and
        // never reached N.convert. The guard now asks the general question: does the descriptor carry type
        // arguments at all?
        final com.landawn.abacus.type.Type<?> parameterized = new com.landawn.abacus.util.TypeReference<ParameterizedDescriptorFixtures.Generic<String>>() {
        }.type();
        final com.landawn.abacus.type.Type<?> erased = com.landawn.abacus.type.TypeFactory.getType(ParameterizedDescriptorFixtures.Generic.class);

        // Precondition: the two descriptors differ only in whether they carry type arguments; both erase to
        // the same class, so isAssignableFrom cannot tell them apart.
        assertFalse(parameterized.parameterTypes().isEmpty());
        assertTrue(erased.parameterTypes().isEmpty());
        assertSame(erased.javaType(), parameterized.javaType());

        final ParameterizedDescriptorFixtures.Generic<Integer> stored = new ParameterizedDescriptorFixtures.Generic<>(7);
        final Map<String, Object> map = new HashMap<>();
        map.put("g", stored);

        // A registered converter is what makes the routing observable: N.convert applies it, the fast path
        // does not. (Registration is global and idempotent; the fixture class exists only for this test.)
        CommonUtil.registerConverter(ParameterizedDescriptorFixtures.Generic.class,
                (final ParameterizedDescriptorFixtures.Generic src, final Class<?> target) -> new ParameterizedDescriptorFixtures.Generic<>("converted"));

        final Object viaParameterized = Maps.getAs(map, "g", (com.landawn.abacus.type.Type) parameterized).get();
        assertNotSame(stored, viaParameterized);
        assertEquals("converted", ((ParameterizedDescriptorFixtures.Generic<?>) viaParameterized).getValue());

        // ... while a descriptor that carries no type arguments is no more informative than a Class token, so
        // the identity fast path still applies there.
        assertSame(stored, Maps.getAs(map, "g", (com.landawn.abacus.type.Type) erased).get());
        assertSame(stored, Maps.getAs(map, "g", ParameterizedDescriptorFixtures.Generic.class).get());
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

    @Test
    public void reviewFixes20260906_aPathOfOnlySeparatorsIsUnresolvable() {
        // Documented, not changed: an empty path is a direct key lookup, but a path made only of separators has
        // no segments at all and is NOT reduced to the empty path. The javadoc now says so.
        final Map<String, Object> map = new HashMap<>();
        map.put("", 1);
        map.put("a", 2);

        assertEquals(Integer.valueOf(1), Maps.<Integer> getByPath(map, ""));
        assertNull(Maps.getByPath(map, "."));
        assertNull(Maps.getByPath(map, ".."));
        assertFalse(Maps.getByPathIfExists(map, ".").isPresent());
        assertTrue(Maps.getByPathIfExists(map, "").isPresent());

        // A separator-delimited real segment still resolves, and empty segments are still dropped.
        assertEquals(Integer.valueOf(2), Maps.<Integer> getByPath(map, "a"));
        assertEquals(Integer.valueOf(2), Maps.<Integer> getByPath(map, ".a"));
        assertEquals(Integer.valueOf(2), Maps.<Integer> getByPath(map, "a."));
    }

    @Test
    public void reviewFixes20260906_replaceKeysWithMergerCanLeaveTheMapEmpty() {
        // Doc-only: the two-argument overload claimed "the map is left untouched" for an
        // UnsupportedOperationException, but the body is clear() + putAll(), so a putAll that fails leaves the
        // map EMPTY. The single-argument sibling already said so; this pins both.
        final Map<String, Integer> withMerger = new NoPutAllLinkedHashMap<>();
        withMerger.put("a1", 1);
        withMerger.put("b1", 2);
        assertThrows(UnsupportedOperationException.class, () -> Maps.replaceKeys(withMerger, k -> k.substring(0, 1), Integer::sum));
        assertTrue(withMerger.isEmpty());

        final Map<String, Integer> noMerger = new NoPutAllLinkedHashMap<>();
        noMerger.put("a", 1);
        noMerger.put("b", 2);
        assertThrows(UnsupportedOperationException.class, () -> Maps.replaceKeys(noMerger, String::toUpperCase));
        assertTrue(noMerger.isEmpty());

        // Control: a failure BEFORE the clear() - during conversion/merging - really does leave the map untouched,
        // which is the half of the sentence that was right.
        final Map<String, Integer> untouched = new LinkedHashMap<>();
        untouched.put("a", 1);
        untouched.put("b", 2);
        assertThrows(IllegalStateException.class, () -> Maps.replaceKeys(untouched, k -> {
            throw new IllegalStateException("boom");
        }, Integer::sum));
        assertEquals(CommonUtil.asMap("a", 1, "b", 2), untouched);

        // Control: an ordinary map is rekeyed and merged as before.
        final Map<String, Integer> ok = new LinkedHashMap<>();
        ok.put("a1", 1);
        ok.put("a2", 2);
        Maps.replaceKeys(ok, k -> k.substring(0, 1), Integer::sum);
        assertEquals(CommonUtil.asMap("a", 3), ok);
    }
}

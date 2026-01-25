package com.landawn.abacus.util;

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
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.Predicate;
import java.util.function.Supplier;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.u.Nullable;
import com.landawn.abacus.util.u.Optional;
import com.landawn.abacus.util.u.OptionalBoolean;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Tag("new-test")
public class Maps200Test extends TestBase {

    @Test
    public void testNewEntry() {
        Map.Entry<String, Integer> entry = Maps.newEntry("key1", 100);
        assertEquals("key1", entry.getKey());
        assertEquals(100, entry.getValue());
        entry.setValue(200);
        assertEquals(200, entry.getValue());
    }

    @Test
    public void testNewImmutableEntry() {
        ImmutableEntry<String, Integer> entry = Maps.newImmutableEntry("key2", 200);
        assertEquals("key2", entry.getKey());
        assertEquals(200, entry.getValue());
        assertThrows(UnsupportedOperationException.class, () -> entry.setValue(300));
    }

    @Test
    public void testKeys() {
        assertTrue(Maps.keys(null).isEmpty());
        assertTrue(Maps.keys(new HashMap<>()).isEmpty());

        Map<String, Integer> map = new HashMap<>();
        map.put("a", 1);
        map.put("b", 2);
        assertEquals(map.keySet(), Maps.keys(map));
        Set<String> emptyKeys = Maps.keys(null);
        assertThrows(UnsupportedOperationException.class, () -> emptyKeys.add("new"));
    }

    @Test
    public void testValues() {
        assertTrue(Maps.values(null).isEmpty());
        assertTrue(Maps.values(new HashMap<>()).isEmpty());

        Map<String, Integer> map = new HashMap<>();
        map.put("a", 1);
        map.put("b", 2);
        Collection<Integer> actualValues = Maps.values(map);
        assertEquals(2, actualValues.size());
        assertTrue(actualValues.containsAll(map.values()));
        assertTrue(map.values().containsAll(actualValues));

        Collection<Object> emptyValues = Maps.values(null);
        assertThrows(UnsupportedOperationException.class, () -> emptyValues.add("new"));
    }

    @Test
    public void testEntrySet() {
        assertTrue(Maps.entrySet(null).isEmpty());
        assertTrue(Maps.entrySet(new HashMap<>()).isEmpty());

        Map<String, Integer> map = new HashMap<>();
        map.put("a", 1);
        assertEquals(map.entrySet(), Maps.entrySet(map));

        Set<Map.Entry<String, String>> emptyEntries = Maps.entrySet(null);
        assertThrows(UnsupportedOperationException.class, () -> emptyEntries.add(CommonUtil.newEntry("k", "v")));
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
    public void testZip_iterables_withSupplier() {
        List<String> keys = Arrays.asList("a", "b");
        List<Integer> values = Arrays.asList(1, 2);
        IntFunction<LinkedHashMap<String, Integer>> supplier = LinkedHashMap::new;
        Map<String, Integer> result = Maps.zip(keys, values, supplier);
        assertTrue(result instanceof LinkedHashMap);
        assertEquals(Map.of("a", 1, "b", 2), result);
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
    public void testZip_iterables_withDefaults() {
        List<String> keys = Arrays.asList("a");
        List<Integer> values = Arrays.asList(1, 2);
        Map<String, Integer> result = Maps.zip(keys, values, "defaultKey", 99);
        Map<String, Integer> expected = new HashMap<>();
        expected.put("a", 1);
        expected.put("defaultKey", 2);
        assertEquals(expected, result);

        List<String> keys2 = Arrays.asList("x", "y");
        List<Integer> values2 = Arrays.asList(10);
        Map<String, Integer> result2 = Maps.zip(keys2, values2, "defaultKey", 99);
        Map<String, Integer> expected2 = new HashMap<>();
        expected2.put("x", 10);
        expected2.put("y", 99);
        assertEquals(expected2, result2);
    }

    @Test
    public void testZip_iterables_withDefaultsAndMerge() {
        List<String> keys = Arrays.asList("a", "defaultKey");
        List<Integer> values = Arrays.asList(1, 2, 3);
        BiFunction<Integer, Integer, Integer> merger = Integer::sum;
        Map<String, Integer> result = Maps.zip(keys, values, "defaultKey", 99, merger, HashMap::new);

        Map<String, Integer> expected = new HashMap<>();
        expected.put("a", 1);
        expected.put("defaultKey", 5);
        assertEquals(expected, result);

        List<String> keys2 = Arrays.asList("k1", "k2", "k3");
        List<Integer> values2 = Arrays.asList(10, 20);
        Map<String, Integer> result2 = Maps.zip(keys2, values2, "defaultKeyX", 99, merger, HashMap::new);
        Map<String, Integer> expected2 = new HashMap<>();
        expected2.put("k1", 10);
        expected2.put("k2", 20);
        expected2.put("k3", 99);
        assertEquals(expected2, result2);
    }

    @Test
    public void testGet_nullable() {
        Map<String, String> map = new HashMap<>();
        map.put("a", "apple");
        map.put("b", null);

        assertEquals(Nullable.of("apple"), Maps.get(map, "a"));
        assertEquals(Nullable.of(null), Maps.get(map, "b"));
        assertEquals(Nullable.empty(), Maps.get(map, "c"));
        assertEquals(Nullable.empty(), Maps.get(null, "a"));
    }

    @Test
    public void testGet_nested_nullable() {
        Map<String, Map<String, Integer>> map = new HashMap<>();
        Map<String, Integer> innerMap = new HashMap<>();
        innerMap.put("x", 10);
        innerMap.put("y", null);
        map.put("outer", innerMap);

        assertEquals(Nullable.of(10), Maps.get(map, "outer", "x"));
        assertEquals(Nullable.of(null), Maps.get(map, "outer", "y"));
        assertEquals(Nullable.empty(), Maps.get(map, "outer", "z"));
        assertEquals(Nullable.empty(), Maps.get(map, "otherOuter", "x"));
        assertEquals(Nullable.empty(), Maps.get(null, "outer", "x"));
    }

    @Test
    public void testGetOrDefaultIfAbsent() {
        Map<String, String> map = new HashMap<>();
        map.put("a", "apple");
        map.put("b", null);
        String defaultVal = "default";

        assertEquals("apple", Maps.getOrDefaultIfAbsent(map, "a", defaultVal));
        assertEquals(defaultVal, Maps.getOrDefaultIfAbsent(map, "b", defaultVal));
        assertEquals(defaultVal, Maps.getOrDefaultIfAbsent(map, "c", defaultVal));
        assertEquals(defaultVal, Maps.getOrDefaultIfAbsent(null, "a", defaultVal));
        assertThrows(IllegalArgumentException.class, () -> Maps.getOrDefaultIfAbsent(map, "a", null));
    }

    @Test
    public void testGetOrDefaultIfAbsent_nested() {
        Map<String, Map<String, Integer>> map = new HashMap<>();
        Map<String, Integer> innerMap = new HashMap<>();
        innerMap.put("x", 10);
        innerMap.put("y", null);
        map.put("outer", innerMap);
        map.put("outerNull", null);
        Integer defaultVal = 99;

        assertEquals(Integer.valueOf(10), Maps.getOrDefaultIfAbsent(map, "outer", "x", defaultVal));
        assertEquals(defaultVal, Maps.getOrDefaultIfAbsent(map, "outer", "y", defaultVal));
        assertEquals(defaultVal, Maps.getOrDefaultIfAbsent(map, "outer", "z", defaultVal));
        assertEquals(defaultVal, Maps.getOrDefaultIfAbsent(map, "otherOuter", "x", defaultVal));
        assertEquals(defaultVal, Maps.getOrDefaultIfAbsent(map, "outerNull", "x", defaultVal));
        assertEquals(defaultVal, Maps.getOrDefaultIfAbsent(null, "outer", "x", defaultVal));
        assertThrows(IllegalArgumentException.class, () -> Maps.getOrDefaultIfAbsent(map, "outer", "x", null));
    }

    @Test
    public void testGetOrEmptyListIfAbsent() {
        Map<String, List<String>> map = new HashMap<>();
        List<String> listA = Arrays.asList("x", "y");
        map.put("a", listA);
        map.put("b", null);

        assertSame(listA, Maps.getOrEmptyListIfAbsent(map, "a"));
        assertTrue(Maps.getOrEmptyListIfAbsent(map, "b").isEmpty());
        assertTrue(Maps.getOrEmptyListIfAbsent(map, "c").isEmpty());
        assertTrue(Maps.getOrEmptyListIfAbsent(null, "a").isEmpty());
    }

    @Test
    public void testGetBoolean() {
        Map<String, Object> map = new HashMap<>();
        map.put("t", true);
        map.put("f", false);
        map.put("s_t", "true");
        map.put("n", null);
        assertEquals(OptionalBoolean.of(true), Maps.getBoolean(map, "t"));
        assertEquals(OptionalBoolean.of(false), Maps.getBoolean(map, "f"));
        assertEquals(OptionalBoolean.of(true), Maps.getBoolean(map, "s_t"));
        assertEquals(OptionalBoolean.empty(), Maps.getBoolean(map, "n"));
        assertEquals(OptionalBoolean.empty(), Maps.getBoolean(map, "missing"));

        assertTrue(Maps.getBoolean(map, "t", false));
        assertFalse(Maps.getBoolean(map, "f", true));
        assertTrue(Maps.getBoolean(map, "missing", true));
        assertFalse(Maps.getBoolean(map, "n", false));
    }

    @Test
    public void testGetString() {
        Map<String, Object> map = new HashMap<>();
        map.put("s", "hello");
        map.put("n", null);
        map.put("i", 123);
        assertEquals(Optional.of("hello"), Maps.getString(map, "s"));
        assertEquals(Optional.empty(), Maps.getString(map, "n"));
        assertEquals(Optional.of("123"), Maps.getString(map, "i"));
        assertEquals(Optional.empty(), Maps.getString(map, "missing"));

        assertEquals("hello", Maps.getString(map, "s", "default"));
        assertEquals("default", Maps.getString(map, "n", "default"));
        assertEquals("123", Maps.getString(map, "i", "default"));
        assertThrows(IllegalArgumentException.class, () -> Maps.getString(map, "s", null));
    }

    @Test
    public void testGetNonNull_class() {
        Map<String, Object> map = new HashMap<>();
        map.put("s", "text");
        map.put("i", 123);
        map.put("d", 123.45);
        map.put("n", null);

        assertEquals(Optional.of("text"), Maps.getNonNull(map, "s", String.class));
        assertEquals(Optional.of(123), Maps.getNonNull(map, "i", Integer.class));
        assertEquals(Optional.of(123.45), Maps.getNonNull(map, "d", Double.class));
        assertEquals(Optional.of("123"), Maps.getNonNull(map, "i", String.class));
        assertEquals(Optional.empty(), Maps.getNonNull(map, "n", String.class));
        assertEquals(Optional.empty(), Maps.getNonNull(map, "missing", String.class));
    }

    @Test
    public void testGetNonNull_default() {
        Map<String, Object> map = new HashMap<>();
        map.put("s", "text");
        map.put("i", 123);
        map.put("n", null);
        String defaultStr = "default";
        Integer defaultInt = 999;

        assertEquals("text", Maps.getNonNull(map, "s", defaultStr));
        assertEquals(Integer.valueOf(123), Maps.getNonNull(map, "i", defaultInt));
        assertEquals("123", Maps.getNonNull(map, "i", defaultStr));
        assertEquals(defaultStr, Maps.getNonNull(map, "n", defaultStr));
        assertEquals(defaultStr, Maps.getNonNull(map, "missing", defaultStr));
        assertThrows(IllegalArgumentException.class, () -> Maps.getNonNull(map, "s", (String) null));
    }

    @Test
    public void testGetAndPutIfAbsent_supplier() {
        Map<String, String> map = new HashMap<>();
        map.put("a", "apple");
        Supplier<String> supplier = () -> "banana";

        assertEquals("apple", Maps.getOrPutIfAbsent(map, "a", supplier));
        assertEquals("apple", map.get("a"));

        assertNotNull(Maps.getOrPutIfAbsent(map, "b", supplier));
        assertEquals("banana", map.get("b"));

        map.put("c", null);
        assertNotNull(Maps.getOrPutIfAbsent(map, "c", supplier));
        assertEquals("banana", map.get("c"));
    }

    @Test
    public void testGetAndPutListIfAbsent() {
        Map<String, List<Integer>> map = new HashMap<>();
        List<Integer> listA = new ArrayList<>(Arrays.asList(1));
        map.put("a", listA);

        assertSame(listA, Maps.getOrPutListIfAbsent(map, "a"));
        assertEquals(Arrays.asList(1), map.get("a"));

        List<Integer> listB = Maps.getOrPutListIfAbsent(map, "b");
        assertNotNull(listB);
        assertNotNull(map.get("b"));
        assertTrue(map.get("b").isEmpty());

        map.put("c", null);
        List<Integer> listC = Maps.getOrPutListIfAbsent(map, "c");
        assertNotNull(listC);
        assertNotNull(map.get("c"));
        assertTrue(map.get("c").isEmpty());
    }

    @Test
    public void testGetIfPresentForEach() {
        Map<String, Integer> map = Map.of("a", 1, "b", 2, "d", 4);
        map = new HashMap<>(map);
        map.put("e", null);
        List<String> keys = Arrays.asList("a", "c", "d", "e");
        List<Integer> result = Maps.getValuesIfPresent(map, keys);
        assertEquals(Arrays.asList(1, 4), result);
    }

    @Test
    public void testGetOrDefaultIfAbsentForEach() {
        Map<String, Integer> map = new HashMap<>();
        map.put("a", 1);
        map.put("b", null);
        List<String> keys = Arrays.asList("a", "b", "c");
        Integer defaultVal = 99;
        List<Integer> result = Maps.getValuesOrDefault(map, keys, defaultVal);
        assertEquals(Arrays.asList(1, defaultVal, defaultVal), result);
    }

    @Test
    public void testGetByPath_simple() {
        Map<String, Object> map = Map.of("key1", "val1");
        assertEquals("val1", Maps.getByPath(map, "key1"));
        assertNull(Maps.getByPath(map, "key2"));
    }

    @Test
    public void testGetByPath_listAccess() {
        Map<String, Object> map = Map.of("key1", Arrays.asList("val1.0", "val1.1"));
        assertEquals("val1.0", Maps.getByPath(map, "key1[0]"));
        assertEquals("val1.1", Maps.getByPath(map, "key1[1]"));
        assertNull(Maps.getByPath(map, "key1[2]"));
    }

    @Test
    public void testGetByPath_nestedMapAndList() {
        Map<String, Object> nestedMap = new HashMap<>();
        nestedMap.put("key3", "val33");
        Map<String, Object> innerMap = new HashMap<>();
        innerMap.put("key2", Arrays.asList("val22.0", nestedMap));
        Map<String, Object> map = Map.of("key1", Arrays.asList(new LinkedHashSet<>(Arrays.asList("val1.0.0", innerMap))));

        assertEquals("val33", Maps.getByPath(map, "key1[0][1].key2[1].key3"));
        assertNull(Maps.getByPath(map, "key1[0][2].key2[1].key3"));
    }

    @Test
    public void testGetByPath_withTargetType() {
        Map<String, Object> map = Map.of("count", "123");
        assertEquals(Integer.valueOf(123), Maps.getByPath(map, "count", Integer.class));
    }

    @Test
    public void testGetByPath_withDefaultValue() {
        Map<String, Object> map = Map.of("name", "Test");
        assertEquals("Test", Maps.getByPath(map, "name", "Default"));
        assertEquals("Default", Maps.getByPath(map, "address", "Default"));
        assertEquals(Integer.valueOf(123), Maps.getByPath(map, "name_no", 123));
    }

    @Test
    public void testGetByPathIfExists() {
        Map<String, Object> map = Map.of("key1", "val1");
        map = new HashMap<>(map);
        map.put("key2", null);

        assertEquals(Nullable.of("val1"), Maps.getByPathIfExists(map, "key1"));
        assertEquals(Nullable.of(null), Maps.getByPathIfExists(map, "key2"));
        assertEquals(Nullable.empty(), Maps.getByPathIfExists(map, "key3"));
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
    public void testIntersection() {
        Map<String, Integer> map1 = Map.of("a", 1, "b", 2, "c", 3);
        Map<String, Integer> map2 = Map.of("b", 2, "c", 4, "d", 5);
        Map<String, Integer> expected = Map.of("b", 2);
        assertEquals(expected, Maps.intersection(map1, map2));
        assertTrue(Maps.intersection(null, map2).isEmpty());
    }

    @Test
    public void testDifference() {
        Map<String, Integer> map1 = Map.of("a", 1, "b", 2, "c", 3);
        Map<String, Integer> map2 = Map.of("b", 20, "d", 4);
        Map<String, Pair<Integer, Nullable<Integer>>> expected = new HashMap<>();
        expected.put("a", Pair.of(1, Nullable.empty()));
        expected.put("b", Pair.of(2, Nullable.of(20)));
        expected.put("c", Pair.of(3, Nullable.empty()));
        assertEquals(expected, Maps.difference(map1, map2));
    }

    @Test
    public void testSymmetricDifference() {
        Map<String, Integer> map1 = Map.of("a", 1, "b", 2, "c", 3);
        Map<String, Integer> map2 = Map.of("b", 20, "d", 4, "c", 3);
        Map<String, Pair<Nullable<Integer>, Nullable<Integer>>> expected = new HashMap<>();
        expected.put("a", Pair.of(Nullable.of(1), Nullable.empty()));
        expected.put("b", Pair.of(Nullable.of(2), Nullable.of(20)));
        expected.put("d", Pair.of(Nullable.empty(), Nullable.of(4)));
        assertEquals(expected, Maps.symmetricDifference(map1, map2));
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
    public void testPutIf_keyFilter() {
        Map<String, Integer> target = new HashMap<>(Map.of("a", 1));
        Map<String, Integer> source = Map.of("b", 2, "c", 3, "aa", 4);
        Predicate<String> keyFilter = k -> k.length() == 1;

        assertTrue(Maps.putIf(target, source, keyFilter));
        assertEquals(Map.of("a", 1, "b", 2, "c", 3), target);

        assertFalse(Maps.putIf(target, Map.of("bb", 5), keyFilter));
    }

    @Test
    public void testPutIf_entryFilter() {
        Map<String, Integer> target = new HashMap<>(Map.of("a", 1));
        Map<String, Integer> source = Map.of("b", 20, "c", 3, "d", 40);
        BiPredicate<String, Integer> entryFilter = (k, v) -> v < 10;

        assertTrue(Maps.putIf(target, source, entryFilter));
        assertEquals(Map.of("a", 1, "c", 3), target);

        assertFalse(Maps.putIf(target, Map.of("e", 50), entryFilter));
    }

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
    public void testRemoveKeys() {
        Map<String, Integer> map = new HashMap<>(Map.of("a", 1, "b", 2, "c", 3));
        assertTrue(Maps.removeKeys(map, Arrays.asList("a", "c", "d")));
        assertEquals(Map.of("b", 2), map);
        assertFalse(Maps.removeKeys(map, Arrays.asList("x")));
    }

    @Test
    public void testRemoveEntries() {
        Map<String, Integer> map = new HashMap<>(Map.of("a", 1, "b", 2, "c", 3));
        Map<String, Integer> toRemove = Map.of("a", 1, "b", 3, "d", 4);
        assertTrue(Maps.removeEntries(map, toRemove));
        assertEquals(Map.of("b", 2, "c", 3), map);
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
        Map<String, Integer> map = new HashMap<>(Map.of("apple", 1, "banana", 2, "apricot", 3));
        assertTrue(Maps.removeIfKey(map, k -> k.startsWith("ap")));
        assertEquals(Map.of("banana", 2), map);
        assertThrows(IllegalArgumentException.class, () -> Maps.removeIfKey(map, (Predicate<String>) null));
    }

    @Test
    public void testRemoveIfValue() {
        Map<String, Integer> map = new HashMap<>(Map.of("a", 10, "b", 2, "c", 30));
        assertTrue(Maps.removeIfValue(map, v -> v > 5));
        assertEquals(Map.of("b", 2), map);
        assertThrows(IllegalArgumentException.class, () -> Maps.removeIfValue(map, (Predicate<Integer>) null));
    }

    @Test
    public void testReplace_withOldValue() {
        Map<String, Integer> map = new HashMap<>(Map.of("a", 1));
        assertTrue(Maps.replace(map, "a", 1, 10));
        assertEquals(10, map.get("a"));
        assertFalse(Maps.replace(map, "a", 1, 20));
        assertFalse(Maps.replace(map, "b", 1, 20));
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
    public void testReplaceAll() {
        Map<String, Integer> map = new LinkedHashMap<>();
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3);
        Maps.replaceAll(map, (k, v) -> v * 10 + (k.equals("a") ? 1 : 0));
        assertEquals(Map.of("a", 11, "b", 20, "c", 30), map);
        assertThrows(IllegalArgumentException.class, () -> Maps.replaceAll(map, null));
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
    public void testInvert() {
        Map<String, Integer> map = Map.of("a", 1, "b", 2, "c", 1);
        Map<Integer, String> inverted = Maps.invert(map);
        assertTrue(inverted.containsKey(1));
        assertTrue(inverted.get(1).equals("a") || inverted.get(1).equals("c"));
        assertEquals("b", inverted.get(2));
        assertEquals(2, inverted.size());

        assertEquals(0, Maps.invert(null).size());
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
    public void testFlatInvert() {
        Map<String, List<String>> map = new HashMap<>();
        map.put("key1", Arrays.asList("valA", "valB"));
        map.put("key2", Arrays.asList("valA", "valC"));
        map.put("key3", null);
        map.put("key4", Arrays.asList());

        Map<String, List<String>> inverted = Maps.flatInvert(map);
        assertEquals(Arrays.asList("key1", "key2"), inverted.get("valA"));
        assertEquals(Arrays.asList("key1"), inverted.get("valB"));
        assertEquals(Arrays.asList("key2"), inverted.get("valC"));
        assertNull(inverted.get("key3"));
        assertEquals(3, inverted.size());
    }

    @Test
    public void testFlatToMap() {
        Map<String, List<Integer>> map = new LinkedHashMap<>();
        map.put("a", Arrays.asList(1, 2, 3));
        map.put("b", Arrays.asList(4, 5, 6));
        map.put("c", Arrays.asList(7, 8));

        List<Map<String, Integer>> result = Maps.flatToMap(map);
        assertEquals(3, result.size());

        assertEquals(Map.of("a", 1, "b", 4, "c", 7), result.get(0));
        assertEquals(Map.of("a", 2, "b", 5, "c", 8), result.get(1));
        assertEquals(Map.of("a", 3, "b", 6), result.get(2));

        assertTrue(Maps.flatToMap(null).isEmpty());
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
        assertThrows(IllegalArgumentException.class, () -> Beans.mapToBean(mapNoMatch, false, false, SimpleBean.class));

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
            if (k.equals("keyB") || k.equals("oldC"))
                return "newKey";
            return k;
        };
        BiFunction<Integer, Integer, Integer> merger = Integer::sum;

        Maps.replaceKeys(map, keyConverter, merger);

        assertEquals(Map.of("keyA", 1, "newKey", 2 + 3), map);
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SimpleBean {
        private int id;
        private String value;

    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Address {
        private String city;
        private String zip;

    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Person {
        private String name;
        private int age;
        private Address address;

    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AnotherBean {
        private String data;
        private Integer number;

    }
}

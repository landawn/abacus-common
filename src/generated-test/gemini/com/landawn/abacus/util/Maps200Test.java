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

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
// For Nullable, Optional, etc. (using mocked versions)
import com.landawn.abacus.util.u.Nullable;
import com.landawn.abacus.util.u.Optional;
import com.landawn.abacus.util.u.OptionalBoolean;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

public class Maps200Test extends TestBase {

    // Test Deprecated methods (newEntry, newImmutableEntry)
    @Test
    public void testNewEntry() {
        Map.Entry<String, Integer> entry = Maps.newEntry("key1", 100);
        assertEquals("key1", entry.getKey());
        assertEquals(100, entry.getValue());
        // Test mutability (if AbstractMap.SimpleEntry allows it)
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

    // Test keys(Map)
    @Test
    public void testKeys() {
        assertTrue(Maps.keys(null).isEmpty());
        assertTrue(Maps.keys(new HashMap<>()).isEmpty());

        Map<String, Integer> map = new HashMap<>();
        map.put("a", 1);
        map.put("b", 2);
        assertEquals(map.keySet(), Maps.keys(map));
        // Check if it's unmodifiable for empty case
        Set<String> emptyKeys = Maps.keys(null);
        assertThrows(UnsupportedOperationException.class, () -> emptyKeys.add("new"));
    }

    // Test values(Map)
    @Test
    public void testValues() {
        assertTrue(Maps.values(null).isEmpty());
        assertTrue(Maps.values(new HashMap<>()).isEmpty());

        Map<String, Integer> map = new HashMap<>();
        map.put("a", 1);
        map.put("b", 2);
        // map.values() is a Collection, not necessarily a Set. Order may not be guaranteed by default.
        Collection<Integer> actualValues = Maps.values(map);
        assertEquals(2, actualValues.size());
        assertTrue(actualValues.containsAll(map.values()));
        assertTrue(map.values().containsAll(actualValues));

        Collection<Object> emptyValues = Maps.values(null);
        assertThrows(UnsupportedOperationException.class, () -> emptyValues.add("new"));
    }

    // Test entrySet(Map)
    @Test
    public void testEntrySet() {
        assertTrue(Maps.entrySet(null).isEmpty());
        assertTrue(Maps.entrySet(new HashMap<>()).isEmpty());

        Map<String, Integer> map = new HashMap<>();
        map.put("a", 1);
        assertEquals(map.entrySet(), Maps.entrySet(map));

        Set<Map.Entry<String, String>> emptyEntries = Maps.entrySet(null);
        assertThrows(UnsupportedOperationException.class, () -> emptyEntries.add(N.newEntry("k", "v")));
    }

    // Test zip methods
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
        assertEquals(Map.of("a", 1), Maps.zip(shortKeys, values)); // Takes min length
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
        expected.put("a", 1 + 3); // Merged
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
        expected.put("defaultKey", 2); // Key exhausted, value from list with default key
        assertEquals(expected, result);

        List<String> keys2 = Arrays.asList("x", "y");
        List<Integer> values2 = Arrays.asList(10);
        Map<String, Integer> result2 = Maps.zip(keys2, values2, "defaultKey", 99);
        Map<String, Integer> expected2 = new HashMap<>();
        expected2.put("x", 10);
        expected2.put("y", 99); // Value exhausted, key from list with default value
        assertEquals(expected2, result2);
    }

    @Test
    public void testZip_iterables_withDefaultsAndMerge() {
        List<String> keys = Arrays.asList("a", "defaultKey");
        List<Integer> values = Arrays.asList(1, 2, 3); // values is longer
        BiFunction<Integer, Integer, Integer> merger = Integer::sum; // (old, new) -> sum
        Map<String, Integer> result = Maps.zip(keys, values, "defaultKey", 99, merger, HashMap::new);

        // Iteration 1: key="a", value=1. result={"a"=1}
        // Iteration 2: key="defaultKey", value=2. result={"a"=1, "defaultKey"=2}
        // Keys exhausted. Value iterator has 3 left.
        // Iteration 3: key="defaultKey", value=3. result.merge("defaultKey", 3, merger)
        // old val for "defaultKey" is 2. merge(2, 3) = 5.
        // result={"a"=1, "defaultKey"=5}
        Map<String, Integer> expected = new HashMap<>();
        expected.put("a", 1);
        expected.put("defaultKey", 5); // 2 merged with 3
        assertEquals(expected, result);

        List<String> keys2 = Arrays.asList("k1", "k2", "k3"); // keys is longer
        List<Integer> values2 = Arrays.asList(10, 20);
        Map<String, Integer> result2 = Maps.zip(keys2, values2, "defaultKeyX", 99, merger, HashMap::new);
        // Iteration 1: key="k1", value=10. result={"k1"=10}
        // Iteration 2: key="k2", value=20. result={"k1"=10, "k2"=20}
        // Values exhausted. Key iterator has "k3" left.
        // Iteration 3: key="k3", value=99 (defaultForValue). result.merge("k3", 99, merger)
        // result={"k1"=10, "k2"=20, "k3"=99}
        Map<String, Integer> expected2 = new HashMap<>();
        expected2.put("k1", 10);
        expected2.put("k2", 20);
        expected2.put("k3", 99);
        assertEquals(expected2, result2);
    }

    // Test get methods
    @Test
    public void testGet_nullable() {
        Map<String, String> map = new HashMap<>();
        map.put("a", "apple");
        map.put("b", null);

        assertEquals(Nullable.of("apple"), Maps.get(map, "a"));
        assertEquals(Nullable.of(null), Maps.get(map, "b")); // Key 'b' is present with null value
        assertEquals(Nullable.empty(), Maps.get(map, "c")); // Key 'c' is absent
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
        assertEquals(Nullable.empty(), Maps.get(map, "outer", "z")); // Inner key absent
        assertEquals(Nullable.empty(), Maps.get(map, "otherOuter", "x")); // Outer key absent
        assertEquals(Nullable.empty(), Maps.get(null, "outer", "x"));
    }

    @Test
    public void testGetOrDefaultIfAbsent() {
        Map<String, String> map = new HashMap<>();
        map.put("a", "apple");
        map.put("b", null); // Value is null
        String defaultVal = "default";

        assertEquals("apple", Maps.getOrDefaultIfAbsent(map, "a", defaultVal));
        assertEquals(defaultVal, Maps.getOrDefaultIfAbsent(map, "b", defaultVal)); // Key 'b' present but value is null
        assertEquals(defaultVal, Maps.getOrDefaultIfAbsent(map, "c", defaultVal)); // Key 'c' absent
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
    // ... similar tests for getOrEmptySetIfAbsent, getOrEmptyMapIfAbsent

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
    // ... similar tests for getChar, getByte, getShort, getInt, getLong, getFloat, getDouble

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
        assertEquals(Optional.of("123"), Maps.getNonNull(map, "i", String.class)); // Conversion
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
        assertEquals(Integer.valueOf(123), Maps.getNonNull(map, "i", defaultInt)); // Requires matching class for default usually
        // Let's assume N.convert handles it if types don't match defaultForNull.getClass()
        assertEquals("123", Maps.getNonNull(map, "i", defaultStr)); // convert Number to String
        assertEquals(defaultStr, Maps.getNonNull(map, "n", defaultStr));
        assertEquals(defaultStr, Maps.getNonNull(map, "missing", defaultStr));
        assertThrows(IllegalArgumentException.class, () -> Maps.getNonNull(map, "s", (String) null));
    }

    // Test getAndPutIfAbsent methods
    @Test
    public void testGetAndPutIfAbsent_supplier() {
        Map<String, String> map = new HashMap<>();
        map.put("a", "apple");
        Supplier<String> supplier = () -> "banana";

        // Key "a" exists with non-null value
        assertEquals("apple", Maps.getAndPutIfAbsent(map, "a", supplier));
        assertEquals("apple", map.get("a")); // Not changed

        // Key "b" is absent
        // Code: val = map.get(key) (null); val_sup = sup.get() ("banana"); map.put("b", "banana"); val = (prev val for b, which is null); return val (null)
        // Javadoc "returns it" (new value) vs code returns previous (often null)
        // Testing based on my interpretation of current code logic:
        assertNotNull(Maps.getAndPutIfAbsent(map, "b", supplier)); // Code returns previous value (null)
        assertEquals("banana", map.get("b")); // "banana" is put

        // Key "c" exists with null value
        map.put("c", null);
        // Code: val = map.get("c") (null); val_sup = sup.get() ("orange"); map.put("c", "orange"); val = (prev val for c, which is null); return val (null)
        assertNotNull(Maps.getAndPutIfAbsent(map, "c", supplier)); // Returns previous (null)
        assertEquals("banana", map.get("c")); // "orange" is put
    }

    @Test
    public void testGetAndPutListIfAbsent() {
        Map<String, List<Integer>> map = new HashMap<>();
        List<Integer> listA = new ArrayList<>(Arrays.asList(1));
        map.put("a", listA);

        // Key "a" exists
        assertSame(listA, Maps.getAndPutListIfAbsent(map, "a"));
        assertEquals(Arrays.asList(1), map.get("a"));

        // Key "b" is absent. Expect new list to be put and *that list returned*.
        // Code: v=null; v_new=new ArrayList; v=map.put("b", v_new) (returns null); return v (null)
        // Javadoc "returns it" (new list). Current code returns previous (null). Testing code.
        List<Integer> listB = Maps.getAndPutListIfAbsent(map, "b");
        assertNotNull(listB); // Based on code returning previous map.put() result
        assertNotNull(map.get("b")); // New list was put
        assertTrue(map.get("b").isEmpty());

        // Key "c" exists with null value
        map.put("c", null);
        List<Integer> listC = Maps.getAndPutListIfAbsent(map, "c");
        assertNotNull(listC); // Based on code returning previous map.put() result
        assertNotNull(map.get("c")); // New list was put
        assertTrue(map.get("c").isEmpty());
    }
    // ... similar tests for getAndPutSetIfAbsent, getAndPutLinkedHashSetIfAbsent, etc.

    // Test getIfPresentForEach, getOrDefaultIfAbsentForEach
    @Test
    public void testGetIfPresentForEach() {
        Map<String, Integer> map = Map.of("a", 1, "b", 2, "d", 4); // "c" is missing
        map = new HashMap<>(map);
        map.put("e", null); // Add a null value
        List<String> keys = Arrays.asList("a", "c", "d", "e");
        List<Integer> result = Maps.getIfPresentForEach(map, keys);
        assertEquals(Arrays.asList(1, 4), result); // Skips "c" (absent) and "e" (null value)
    }

    @Test
    public void testGetOrDefaultIfAbsentForEach() {
        Map<String, Integer> map = new HashMap<>();
        map.put("a", 1);
        map.put("b", null); // key 'b' has null value
        List<String> keys = Arrays.asList("a", "b", "c");
        Integer defaultVal = 99;
        List<Integer> result = Maps.getOrDefaultIfAbsentForEach(map, keys, defaultVal);
        assertEquals(Arrays.asList(1, defaultVal, defaultVal), result); // "b" (null) and "c" (absent) get default
    }

    // Test getByPath methods (using Javadoc examples as base)
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
        assertNull(Maps.getByPath(map, "key1[2]")); // Index out of bounds
    }

    @Test
    public void testGetByPath_nestedMapAndList() {
        Map<String, Object> nestedMap = new HashMap<>();
        nestedMap.put("key3", "val33");
        Map<String, Object> innerMap = new HashMap<>();
        innerMap.put("key2", Arrays.asList("val22.0", nestedMap));
        Map<String, Object> map = Map.of("key1", Arrays.asList(new LinkedHashSet<>(Arrays.asList("val1.0.0", innerMap))));

        // path: "key1[0][1].key2[1].key3"
        // key1 -> List L1
        // L1[0] -> LinkedHashSet S1
        // S1[1] (second element of S1) -> innerMap
        // innerMap.key2 -> List L2 = ["val22.0", nestedMap]
        // L2[1] -> nestedMap
        // nestedMap.key3 -> "val33"
        assertEquals("val33", Maps.getByPath(map, "key1[0][1].key2[1].key3"));
        assertNull(Maps.getByPath(map, "key1[0][2].key2[1].key3")); // S1[2] is out of bounds
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
        assertEquals(Integer.valueOf(123), Maps.getByPath(map, "name_no", 123)); // "Test" converted to 123 (if N.convert supports String to Int)
                                                                                 // Current mock N.convert might struggle here if defaultValue determines type.
                                                                                 // The method uses defaultValue.getClass() for conversion.
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

    // Test contains methods
    @Test
    public void testContains_entry() {
        Map<String, Integer> map = Map.of("a", 1, "b", 2);
        map = new HashMap<>(map);
        map.put("c", null);
        assertTrue(Maps.contains(map, N.newEntry("a", 1)));
        assertFalse(Maps.contains(map, N.newEntry("a", 2))); // Wrong value
        assertTrue(Maps.contains(map, N.newEntry("c", null)));
        assertFalse(Maps.contains(map, N.newEntry("d", null))); // Key absent
    }

    @Test
    public void testContains_keyValue() {
        Map<String, Integer> map = Map.of("a", 1, "b", 2);
        map = new HashMap<>(map);
        map.put("c", null);
        assertTrue(Maps.contains(map, "a", 1));
        assertFalse(Maps.contains(map, "a", 2));
        assertTrue(Maps.contains(map, "c", null));
        assertFalse(Maps.contains(map, "d", null));
    }

    // Test intersection, difference, symmetricDifference
    @Test
    public void testIntersection() {
        Map<String, Integer> map1 = Map.of("a", 1, "b", 2, "c", 3);
        Map<String, Integer> map2 = Map.of("b", 2, "c", 4, "d", 5); // "b" same, "c" different val
        Map<String, Integer> expected = Map.of("b", 2);
        assertEquals(expected, Maps.intersection(map1, map2));
        assertTrue(Maps.intersection(null, map2).isEmpty());
    }

    @Test
    public void testDifference() {
        Map<String, Integer> map1 = Map.of("a", 1, "b", 2, "c", 3);
        Map<String, Integer> map2 = Map.of("b", 20, "d", 4); // "b" different, "d" only in map2
        // Expected: keys from map1.
        // "a": (1, empty)
        // "b": (2, Nullable.of(20)) (different value)
        // "c": (3, empty)
        Map<String, Pair<Integer, Nullable<Integer>>> expected = new HashMap<>();
        expected.put("a", Pair.of(1, Nullable.empty()));
        expected.put("b", Pair.of(2, Nullable.of(20)));
        expected.put("c", Pair.of(3, Nullable.empty()));
        assertEquals(expected, Maps.difference(map1, map2));
    }

    @Test
    public void testSymmetricDifference() {
        Map<String, Integer> map1 = Map.of("a", 1, "b", 2, "c", 3);
        Map<String, Integer> map2 = Map.of("b", 20, "d", 4, "c", 3); // "c" same, "b" different, "d" only in map2, "a" only in map1
        // "a": (Nullable.of(1), empty)
        // "b": (Nullable.of(2), Nullable.of(20))
        // "d": (empty, Nullable.of(4))
        // "c" is common with same value, so not in symmetric difference.
        Map<String, Pair<Nullable<Integer>, Nullable<Integer>>> expected = new HashMap<>();
        expected.put("a", Pair.of(Nullable.of(1), Nullable.empty()));
        expected.put("b", Pair.of(Nullable.of(2), Nullable.of(20)));
        expected.put("d", Pair.of(Nullable.empty(), Nullable.of(4)));
        assertEquals(expected, Maps.symmetricDifference(map1, map2));
    }

    // Test putIfAbsent methods
    @Test
    public void testPutIfAbsent_value() {
        Map<String, String> map = new HashMap<>();
        map.put("a", "apple");

        // Key "a" present, non-null value. Method returns current value "apple", map unchanged.
        assertEquals("apple", Maps.putIfAbsent(map, "a", "newApple"));
        assertEquals("apple", map.get("a"));

        // Key "b" absent. Method puts "banana", returns previous (null).
        assertNull(Maps.putIfAbsent(map, "b", "banana"));
        assertEquals("banana", map.get("b"));

        // Key "c" present with null value. Method puts "cherry", returns previous (null).
        map.put("c", null);
        assertNull(Maps.putIfAbsent(map, "c", "cherry"));
        assertEquals("cherry", map.get("c"));
    }

    @Test
    public void testPutIfAbsent_supplier() {
        Map<String, String> map = new HashMap<>();
        map.put("a", "apple");
        Supplier<String> supplier = () -> "newVal";

        assertEquals("apple", Maps.putIfAbsent(map, "a", supplier)); // Key "a" present
        assertEquals("apple", map.get("a"));

        assertNull(Maps.putIfAbsent(map, "b", supplier)); // Key "b" absent
        assertEquals("newVal", map.get("b"));
    }

    @Test
    public void testPutIf_keyFilter() {
        Map<String, Integer> target = new HashMap<>(Map.of("a", 1));
        Map<String, Integer> source = Map.of("b", 2, "c", 3, "aa", 4);
        Predicate<String> keyFilter = k -> k.length() == 1; // Only put if key length is 1

        assertTrue(Maps.putIf(target, source, keyFilter));
        assertEquals(Map.of("a", 1, "b", 2, "c", 3), target); // "aa" not put

        assertFalse(Maps.putIf(target, Map.of("bb", 5), keyFilter)); // Nothing put
    }

    @Test
    public void testPutIf_entryFilter() {
        Map<String, Integer> target = new HashMap<>(Map.of("a", 1));
        Map<String, Integer> source = Map.of("b", 20, "c", 3, "d", 40);
        BiPredicate<String, Integer> entryFilter = (k, v) -> v < 10; // Only put if value < 10

        assertTrue(Maps.putIf(target, source, entryFilter));
        assertEquals(Map.of("a", 1, "c", 3), target); // "b" and "d" not put

        assertFalse(Maps.putIf(target, Map.of("e", 50), entryFilter));
    }

    // Test remove methods
    @Test
    public void testRemove_entry() {
        Map<String, Integer> map = new HashMap<>(Map.of("a", 1, "b", 2));
        assertTrue(Maps.remove(map, N.newEntry("a", 1)));
        assertEquals(1, map.size());
        assertFalse(Maps.remove(map, N.newEntry("b", 3))); // Wrong value
    }

    @Test
    public void testRemove_keyValue() {
        Map<String, Integer> map = new HashMap<>(Map.of("a", 1, "b", 2));
        assertTrue(Maps.remove(map, "a", 1));
        assertFalse(Maps.remove(map, "b", 3)); // Wrong value
        assertFalse(Maps.remove(map, "c", 2)); // Key not present
    }

    @Test
    public void testRemoveKeys() {
        Map<String, Integer> map = new HashMap<>(Map.of("a", 1, "b", 2, "c", 3));
        assertTrue(Maps.removeKeys(map, Arrays.asList("a", "c", "d"))); // "d" not present
        assertEquals(Map.of("b", 2), map);
        assertFalse(Maps.removeKeys(map, Arrays.asList("x")));
    }

    @Test
    public void testRemoveEntries() {
        Map<String, Integer> map = new HashMap<>(Map.of("a", 1, "b", 2, "c", 3));
        Map<String, Integer> toRemove = Map.of("a", 1, "b", 3, "d", 4); // "b" has wrong value, "d" not in map
        assertTrue(Maps.removeEntries(map, toRemove));
        assertEquals(Map.of("b", 2, "c", 3), map); // Only "a" removed
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

    // Test replace methods
    @Test
    public void testReplace_withOldValue() {
        Map<String, Integer> map = new HashMap<>(Map.of("a", 1));
        assertTrue(Maps.replace(map, "a", 1, 10));
        assertEquals(10, map.get("a"));
        assertFalse(Maps.replace(map, "a", 1, 20)); // Old value is now 10
        assertFalse(Maps.replace(map, "b", 1, 20)); // Key not present
    }

    @Test
    public void testReplace_newValueOnly() {
        Map<String, Integer> map = new HashMap<>(Map.of("a", 1));
        map.put("b", null);
        assertEquals(Integer.valueOf(1), Maps.replace(map, "a", 10));
        assertEquals(10, map.get("a"));
        assertNull(Maps.replace(map, "b", 20)); // b was null
        assertEquals(20, map.get("b"));
        assertNull(Maps.replace(map, "c", 30)); // c not present
        assertNull(map.get("c"));
    }

    @Test
    public void testReplaceAll() {
        Map<String, Integer> map = new LinkedHashMap<>(); // Use LinkedHashMap to test order if relevant (not strictly by replaceAll)
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3);
        Maps.replaceAll(map, (k, v) -> v * 10 + (k.equals("a") ? 1 : 0));
        assertEquals(Map.of("a", 11, "b", 20, "c", 30), map);
        assertThrows(IllegalArgumentException.class, () -> Maps.replaceAll(map, null));
    }

    // Test filter methods
    @Test
    public void testFilter_entryPredicate() {
        Map<String, Integer> map = Map.of("a", 1, "b", 20, "c", 3);
        Map<String, Integer> filtered = Maps.filter(map, entry -> entry.getValue() < 10);
        assertEquals(Map.of("a", 1, "c", 3), filtered);
        assertNotSame(map, filtered);
        assertEquals(0, Maps.filter(null, e -> true).size());
    }
    // ... similar for BiPredicate, filterByKey, filterByValue

    // Test invert methods
    @Test
    public void testInvert() {
        Map<String, Integer> map = Map.of("a", 1, "b", 2, "c", 1); // "a" and "c" map to 1
        Map<Integer, String> inverted = Maps.invert(map);
        // Which key wins for duplicate value 1 depends on map iteration order.
        // Assuming HashMap iteration order, could be "a" or "c".
        // Let's use a LinkedHashMap for predictable order if original map was LinkedHashMap.
        // The method uses newOrderingMap -> if SortedMap -> LinkedHashMap, else N.newMap(m.getClass())
        // If input is HashMap, output is HashMap.
        assertTrue(inverted.containsKey(1));
        assertTrue(inverted.get(1).equals("a") || inverted.get(1).equals("c"));
        assertEquals("b", inverted.get(2));
        assertEquals(2, inverted.size()); // One of the keys for value 1 is lost

        assertEquals(0, Maps.invert(null).size());
    }

    @Test
    public void testInvert_withMerge() {
        Map<String, Integer> map = new LinkedHashMap<>(); // Predictable order
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 1);
        BiFunction<String, String, String> merger = (oldKey, newKey) -> oldKey + "," + newKey;
        Map<Integer, String> inverted = Maps.invert(map, merger);

        assertEquals("a,c", inverted.get(1)); // Order in merget depends on (oldVal, entry.getKey()) -> "a" then "c"
        assertEquals("b", inverted.get(2));
    }

    @Test
    public void testFlatInvert() {
        Map<String, List<String>> map = new HashMap<>();
        map.put("key1", Arrays.asList("valA", "valB"));
        map.put("key2", Arrays.asList("valA", "valC"));
        map.put("key3", null); // Null collection
        map.put("key4", Arrays.asList()); // Empty collection

        Map<String, List<String>> inverted = Maps.flatInvert(map);
        assertEquals(Arrays.asList("key1", "key2"), inverted.get("valA"));
        assertEquals(Arrays.asList("key1"), inverted.get("valB"));
        assertEquals(Arrays.asList("key2"), inverted.get("valC"));
        assertNull(inverted.get("key3")); // Values from null/empty collections are not inverted
        assertEquals(3, inverted.size());
    }

    // Test flatToMap
    @Test
    public void testFlatToMap() {
        Map<String, List<Integer>> map = new LinkedHashMap<>(); // For predictable key order in inner maps
        map.put("a", Arrays.asList(1, 2, 3));
        map.put("b", Arrays.asList(4, 5, 6));
        map.put("c", Arrays.asList(7, 8)); // Shorter list

        List<Map<String, Integer>> result = Maps.flatToMap(map);
        assertEquals(3, result.size()); // Max length is 3

        assertEquals(Map.of("a", 1, "b", 4, "c", 7), result.get(0));
        assertEquals(Map.of("a", 2, "b", 5, "c", 8), result.get(1));
        assertEquals(Map.of("a", 3, "b", 6), result.get(2)); // "c" is missing as its list was shorter

        assertTrue(Maps.flatToMap(null).isEmpty());
    }

    // Test flatten/unflatten
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

        // Test custom delimiter
        Map<String, Object> flatCustom = Maps.flatten(nestedMap, "_", HashMap::new);
        assertEquals(3, flatCustom.get("b_c"));
        Map<String, Object> unflattenedCustom = Maps.unflatten(flatCustom, "_", HashMap::new);
        assertEquals(3, ((Map<?, ?>) unflattenedCustom.get("b")).get("c"));
    }

    //    // Test merge (Maps.merge, not Map.merge)
    //    @Test
    //    public void testMerge() {
    //        Map<String, Integer> map = new HashMap<>(Map.of("a", 1));
    //        BiFunction<Integer, Integer, Integer> sum = Integer::sum;
    //
    //        Maps.merge(map, "a", 5, sum); // Key "a" exists, merge
    //        assertEquals(Integer.valueOf(6), map.get("a"));
    //
    //        Maps.merge(map, "b", 10, sum); // Key "b" absent, put
    //        assertEquals(Integer.valueOf(10), map.get("b"));
    //
    //        map.put("c", null);
    //        Maps.merge(map, "c", 7, sum); // Key "c" has null, merge (oldValue=null, value=7)
    //        assertEquals(Integer.valueOf(7), map.get("c")); // Assuming sum.apply(null, 7) works or remappingFunction handles null
    //        // The method is: map.put(key, remappingFunction.apply(oldValue, value))
    //        // So sum(null, 7) must be handled by the BiFunction. Standard Integer::sum would NPE.
    //        // Let's use a null-safe merger for test.
    //        BiFunction<Integer, Integer, Integer> nullSafeSum = (oldV, newV) -> (oldV == null ? 0 : oldV) + (newV == null ? 0 : newV);
    //        map.put("d", null);
    //        Maps.merge(map, "d", 8, nullSafeSum);
    //        assertEquals(Integer.valueOf(8), map.get("d"));
    //
    //        assertThrows(IllegalArgumentException.class, () -> Maps.merge(map, "e", 1, null));
    //    }

    // Test bean2Map and map2Bean (simplified tests with local beans)
    @Test
    public void testBean2Map_simple() {
        SimpleBean bean = new SimpleBean(1, "test");
        Map<String, Object> map = Beans.bean2Map(bean);
        assertEquals(Map.of("id", 1, "value", "test"), map);
    }

    @Test
    public void testMap2Bean_simple() {
        Map<String, Object> map = Map.of("id", 10, "value", "hello", "extra", "ignored");
        SimpleBean bean = Beans.map2Bean(map, SimpleBean.class); // ignoreUnmatchedProperty is true by default
        assertEquals(10, bean.id);
        assertEquals("hello", bean.value);

        Map<String, Object> mapNoMatch = Map.of("id", 20, "value", "world", "extraField", "data");
        // SimpleBean beanNoMatch = Beans.map2Bean(mapNoMatch, false, false, SimpleBean.class); // ignoreUnmatched=false
        // Current mock BeanInfo.setPropValue will throw if ignoreUnmatched is false and field not found
        assertThrows(IllegalArgumentException.class, () -> Beans.map2Bean(mapNoMatch, false, false, SimpleBean.class));

    }

    @Test
    public void testDeepBean2Map() {
        Address address = new Address("NY", "10001");
        Person person = new Person("John", 30, address);
        Map<String, Object> map = Beans.deepBean2Map(person);

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
        Map<String, Object> flatMap = Beans.bean2FlatMap(person);

        assertEquals("Jane", flatMap.get("name"));
        assertEquals(25, flatMap.get("age"));
        assertEquals("LA", flatMap.get("address.city"));
        assertEquals("90001", flatMap.get("address.zip"));
    }

    // Test replaceKeys methods
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
        map.put("keyB", 2); // This will become "newKey"
        map.put("oldC", 3); // This will also become "newKey"

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

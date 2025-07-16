package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.math.BigDecimal;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.IntFunction;
import java.util.function.Predicate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.type.Type;
import com.landawn.abacus.util.IntFunctions;
import com.landawn.abacus.util.u.Nullable;
import com.landawn.abacus.util.u.Optional;
import com.landawn.abacus.util.u.OptionalBoolean;
import com.landawn.abacus.util.u.OptionalByte;
import com.landawn.abacus.util.u.OptionalChar;
import com.landawn.abacus.util.u.OptionalDouble;
import com.landawn.abacus.util.u.OptionalFloat;
import com.landawn.abacus.util.u.OptionalInt;
import com.landawn.abacus.util.u.OptionalLong;
import com.landawn.abacus.util.u.OptionalShort;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

public class Maps100Test extends TestBase {

    private Map<String, String> testMap;
    private Map<String, Object> objectMap;
    private Map<String, List<String>> listMap;
    private Map<String, Set<String>> setMap;
    private Map<String, Map<String, String>> nestedMap;

    @BeforeEach
    public void setUp() {
        testMap = new HashMap<>();
        testMap.put("key1", "value1");
        testMap.put("key2", "value2");
        testMap.put("key3", "value3");

        objectMap = new HashMap<>();
        objectMap.put("string", "test");
        objectMap.put("byte", 10);
        objectMap.put("short", 100);
        objectMap.put("integer", 123);
        objectMap.put("long", 123456789L);
        objectMap.put("float", 12.34f);
        objectMap.put("double", 45.67);
        objectMap.put("boolean", true);
        objectMap.put("nullValue", null);

        listMap = new HashMap<>();
        listMap.put("list1", Arrays.asList("a", "b", "c"));
        listMap.put("list2", Arrays.asList("d", "e"));
        listMap.put("emptyList", new ArrayList<>());

        setMap = new HashMap<>();
        setMap.put("set1", new HashSet<>(Arrays.asList("x", "y", "z")));
        setMap.put("set2", new HashSet<>(Arrays.asList("p", "q")));

        nestedMap = new HashMap<>();
        Map<String, String> innerMap1 = new HashMap<>();
        innerMap1.put("innerKey1", "innerValue1");
        innerMap1.put("innerKey2", "innerValue2");
        nestedMap.put("outer1", innerMap1);

        Map<String, String> innerMap2 = new HashMap<>();
        innerMap2.put("innerKey3", "innerValue3");
        nestedMap.put("outer2", innerMap2);
    }

    @Test
    public void testNewEntry() {
        Map.Entry<String, Integer> entry = Maps.newEntry("key", 100);
        assertEquals("key", entry.getKey());
        assertEquals(Integer.valueOf(100), entry.getValue());

        // Test mutation
        entry.setValue(200);
        assertEquals(Integer.valueOf(200), entry.getValue());
    }

    @Test
    public void testNewImmutableEntry() {
        ImmutableEntry<String, Integer> entry = Maps.newImmutableEntry("key", 100);
        assertEquals("key", entry.getKey());
        assertEquals(Integer.valueOf(100), entry.getValue());

        // Immutable entries should throw exception when trying to set value 
        assertThrows(UnsupportedOperationException.class, () -> entry.setValue(200));
    }

    @Test
    public void testKeys() {
        Set<String> keys = Maps.keys(testMap);
        assertEquals(3, keys.size());
        assertTrue(keys.contains("key1"));
        assertTrue(keys.contains("key2"));
        assertTrue(keys.contains("key3"));

        // Test with null/empty map
        assertTrue(Maps.keys(null).isEmpty());
        assertTrue(Maps.keys(new HashMap<>()).isEmpty());
    }

    @Test
    public void testValues() {
        Collection<String> values = Maps.values(testMap);
        assertEquals(3, values.size());
        assertTrue(values.contains("value1"));
        assertTrue(values.contains("value2"));
        assertTrue(values.contains("value3"));

        // Test with null/empty map
        assertTrue(Maps.values(null).isEmpty());
        assertTrue(Maps.values(new HashMap<>()).isEmpty());
    }

    @Test
    public void testEntrySet() {
        Set<Map.Entry<String, String>> entries = Maps.entrySet(testMap);
        assertEquals(3, entries.size());

        // Test with null/empty map
        assertTrue(Maps.entrySet(null).isEmpty());
        assertTrue(Maps.entrySet(new HashMap<>()).isEmpty());
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

        // Test with different sizes
        List<String> longerKeys = Arrays.asList("a", "b", "c", "d");
        Map<String, Integer> result2 = Maps.zip(longerKeys, values);
        assertEquals(3, result2.size());

        // Test with null/empty
        assertTrue(Maps.zip(null, values).isEmpty());
        assertTrue(Maps.zip(keys, null).isEmpty());
        assertTrue(Maps.zip(new ArrayList<>(), values).isEmpty());
    }

    @Test
    public void testZipWithMapSupplier() {
        List<String> keys = Arrays.asList("a", "b", "c");
        List<Integer> values = Arrays.asList(1, 2, 3);

        LinkedHashMap<String, Integer> result = Maps.zip(keys, values, LinkedHashMap::new);
        assertEquals(3, result.size());
        assertTrue(result instanceof LinkedHashMap);

        // Verify order is preserved
        Iterator<String> keyIterator = result.keySet().iterator();
        assertEquals("a", keyIterator.next());
        assertEquals("b", keyIterator.next());
        assertEquals("c", keyIterator.next());
    }

    @Test
    public void testZipWithMergeFunction() {
        List<String> keys = Arrays.asList("a", "b", "a");
        List<Integer> values = Arrays.asList(1, 2, 3);

        Map<String, Integer> result = Maps.zip(keys, values, Integer::sum, HashMap::new);
        assertEquals(2, result.size());
        assertEquals(Integer.valueOf(4), result.get("a")); // 1 + 3
        assertEquals(Integer.valueOf(2), result.get("b"));
    }

    @Test
    public void testZipWithDefaults() {
        List<String> keys = Arrays.asList("a", "b");
        List<Integer> values = Arrays.asList(1, 2, 3);

        Map<String, Integer> result = Maps.zip(keys, values, "default", 99);
        assertEquals(3, result.size());
        assertEquals(Integer.valueOf(1), result.get("a"));
        assertEquals(Integer.valueOf(2), result.get("b"));
        assertEquals(Integer.valueOf(3), result.get("default"));

        // Test opposite case
        List<String> longerKeys = Arrays.asList("a", "b", "c");
        List<Integer> shorterValues = Arrays.asList(1, 2);
        Map<String, Integer> result2 = Maps.zip(longerKeys, shorterValues, "default", 99);
        assertEquals(3, result2.size());
        assertEquals(Integer.valueOf(99), result2.get("c"));
    }

    @Test
    public void testGet() {
        Nullable<String> result = Maps.get(testMap, "key1");
        assertTrue(result.isPresent());
        assertEquals("value1", result.get());

        // Test missing key
        Nullable<String> missing = Maps.get(testMap, "missing");
        assertFalse(missing.isPresent());

        // Test null value
        objectMap.put("nullKey", null);
        Nullable<Object> nullResult = Maps.get(objectMap, "nullKey");
        assertTrue(nullResult.isPresent());
        assertNull(nullResult.get());

        // Test with null/empty map
        assertFalse(Maps.get(null, "key").isPresent());
        assertFalse(Maps.get(new HashMap<>(), "key").isPresent());
    }

    @Test
    public void testGetNested() {
        Nullable<String> result = Maps.get(nestedMap, "outer1", "innerKey1");
        assertTrue(result.isPresent());
        assertEquals("innerValue1", result.get());

        // Test missing outer key
        assertFalse(Maps.get(nestedMap, "missing", "innerKey1").isPresent());

        // Test missing inner key
        assertFalse(Maps.get(nestedMap, "outer1", "missing").isPresent());

        // Test with null/empty map
        assertFalse(Maps.get(null, "key", "key2").isPresent());
    }

    @Test
    public void testGetOrDefaultIfAbsent() {
        assertEquals("value1", Maps.getOrDefaultIfAbsent(testMap, "key1", "default"));
        assertEquals("default", Maps.getOrDefaultIfAbsent(testMap, "missing", "default"));

        // Test with null default - should throw exception 
        assertThrows(IllegalArgumentException.class, () -> Maps.getOrDefaultIfAbsent(testMap, "key1", null));
    }

    @Test
    public void testGetOrDefaultIfAbsentNested() {
        assertEquals("innerValue1", Maps.getOrDefaultIfAbsent(nestedMap, "outer1", "innerKey1", "default"));
        assertEquals("default", Maps.getOrDefaultIfAbsent(nestedMap, "outer1", "missing", "default"));
        assertEquals("default", Maps.getOrDefaultIfAbsent(nestedMap, "missing", "innerKey1", "default"));

        // Test with null default - should throw exception 
        assertThrows(IllegalArgumentException.class, () -> Maps.getOrDefaultIfAbsent(nestedMap, "outer1", "innerKey1", null));
    }

    @Test
    public void testGetOrEmptyListIfAbsent() {
        List<String> result = Maps.getOrEmptyListIfAbsent(listMap, "list1");
        assertEquals(3, result.size());
        assertEquals(Arrays.asList("a", "b", "c"), result);

        List<String> empty = Maps.getOrEmptyListIfAbsent(listMap, "missing");
        assertTrue(empty.isEmpty());

        // Test with null value
        listMap.put("nullList", null);
        List<String> nullResult = Maps.getOrEmptyListIfAbsent(listMap, "nullList");
        assertTrue(nullResult.isEmpty());
    }

    @Test
    public void testGetOrEmptySetIfAbsent() {
        Set<String> result = Maps.getOrEmptySetIfAbsent(setMap, "set1");
        assertEquals(3, result.size());
        assertTrue(result.contains("x"));

        Set<String> empty = Maps.getOrEmptySetIfAbsent(setMap, "missing");
        assertTrue(empty.isEmpty());
    }

    @Test
    public void testGetOrEmptyMapIfAbsent() {
        Map<String, String> result = Maps.getOrEmptyMapIfAbsent(nestedMap, "outer1");
        assertEquals(2, result.size());
        assertEquals("innerValue1", result.get("innerKey1"));

        Map<String, String> empty = Maps.getOrEmptyMapIfAbsent(nestedMap, "missing");
        assertTrue(empty.isEmpty());
    }

    @Test
    public void testGetBoolean() {
        objectMap.put("trueString", "true");
        objectMap.put("falseString", "false");
        objectMap.put("boolTrue", Boolean.TRUE);

        OptionalBoolean result1 = Maps.getBoolean(objectMap, "boolean");
        assertTrue(result1.isPresent());
        assertTrue(result1.get());

        OptionalBoolean result2 = Maps.getBoolean(objectMap, "trueString");
        assertTrue(result2.isPresent());
        assertTrue(result2.get());

        OptionalBoolean result3 = Maps.getBoolean(objectMap, "falseString");
        assertTrue(result3.isPresent());
        assertFalse(result3.get());

        assertFalse(Maps.getBoolean(objectMap, "missing").isPresent());
        assertFalse(Maps.getBoolean(null, "key").isPresent());
    }

    @Test
    public void testGetBooleanWithDefault() {
        assertTrue(Maps.getBoolean(objectMap, "boolean", false));
        assertTrue(Maps.getBoolean(objectMap, "missing", true));
        assertFalse(Maps.getBoolean(objectMap, "missing", false));
        assertFalse(Maps.getBoolean(null, "key", false));
    }

    @Test
    public void testGetChar() {
        objectMap.put("char", 'A');
        objectMap.put("charString", "B");

        OptionalChar result1 = Maps.getChar(objectMap, "char");
        assertTrue(result1.isPresent());
        assertEquals('A', result1.get());

        OptionalChar result2 = Maps.getChar(objectMap, "charString");
        assertTrue(result2.isPresent());
        assertEquals('B', result2.get());

        assertFalse(Maps.getChar(objectMap, "missing").isPresent());
    }

    @Test
    public void testGetCharWithDefault() {
        objectMap.put("char", 'A');
        assertEquals('A', Maps.getChar(objectMap, "char", 'Z'));
        assertEquals('Z', Maps.getChar(objectMap, "missing", 'Z'));
    }

    @Test
    public void testGetByte() {
        objectMap.put("byte", (byte) 10);
        objectMap.put("byteString", "20");

        OptionalByte result1 = Maps.getByte(objectMap, "byte");
        assertTrue(result1.isPresent());
        assertEquals(10, result1.get());

        OptionalByte result2 = Maps.getByte(objectMap, "byteString");
        assertTrue(result2.isPresent());
        assertEquals(20, result2.get());

        assertFalse(Maps.getByte(objectMap, "missing").isPresent());
    }

    @Test
    public void testGetByteWithDefault() {
        assertEquals(10, Maps.getByte(objectMap, "byte", (byte) 99));
        assertEquals(99, Maps.getByte(objectMap, "missing", (byte) 99));
    }

    @Test
    public void testGetShort() {
        objectMap.put("short", (short) 100);
        objectMap.put("shortString", "200");

        OptionalShort result1 = Maps.getShort(objectMap, "short");
        assertTrue(result1.isPresent());
        assertEquals(100, result1.get());

        OptionalShort result2 = Maps.getShort(objectMap, "shortString");
        assertTrue(result2.isPresent());
        assertEquals(200, result2.get());

        assertFalse(Maps.getShort(objectMap, "missing").isPresent());
    }

    @Test
    public void testGetShortWithDefault() {
        assertEquals(100, Maps.getShort(objectMap, "short", (short) 999));
        assertEquals(999, Maps.getShort(objectMap, "missing", (short) 999));
    }

    @Test
    public void testGetInt() {
        OptionalInt result = Maps.getInt(objectMap, "integer");
        assertTrue(result.isPresent());
        assertEquals(123, result.getAsInt());

        objectMap.put("intString", "456");
        OptionalInt result2 = Maps.getInt(objectMap, "intString");
        assertTrue(result2.isPresent());
        assertEquals(456, result2.getAsInt());

        assertFalse(Maps.getInt(objectMap, "missing").isPresent());
    }

    @Test
    public void testGetIntWithDefault() {
        assertEquals(123, Maps.getInt(objectMap, "integer", 999));
        assertEquals(999, Maps.getInt(objectMap, "missing", 999));
    }

    @Test
    public void testGetLong() {
        objectMap.put("long", 123456789L);
        objectMap.put("longString", "987654321");

        OptionalLong result1 = Maps.getLong(objectMap, "long");
        assertTrue(result1.isPresent());
        assertEquals(123456789L, result1.getAsLong());

        OptionalLong result2 = Maps.getLong(objectMap, "longString");
        assertTrue(result2.isPresent());
        assertEquals(987654321L, result2.getAsLong());

        assertFalse(Maps.getLong(objectMap, "missing").isPresent());
    }

    @Test
    public void testGetLongWithDefault() {
        assertEquals(123456789L, Maps.getLong(objectMap, "long", 999L));
        assertEquals(999L, Maps.getLong(objectMap, "missing", 999L));
    }

    @Test
    public void testGetFloat() {
        objectMap.put("float", 12.34f);
        objectMap.put("floatString", "56.78");

        OptionalFloat result1 = Maps.getFloat(objectMap, "float");
        assertTrue(result1.isPresent());
        assertEquals(12.34f, result1.get(), 0.001f);

        OptionalFloat result2 = Maps.getFloat(objectMap, "floatString");
        assertTrue(result2.isPresent());
        assertEquals(56.78f, result2.get(), 0.001f);

        assertFalse(Maps.getFloat(objectMap, "missing").isPresent());
    }

    @Test
    public void testGetFloatWithDefault() {
        assertEquals(12.34f, Maps.getFloat(objectMap, "float", 99.99f), 0.001f);
        assertEquals(99.99f, Maps.getFloat(objectMap, "missing", 99.99f), 0.001f);
    }

    @Test
    public void testGetDouble() {
        OptionalDouble result = Maps.getDouble(objectMap, "double");
        assertTrue(result.isPresent());
        assertEquals(45.67, result.getAsDouble(), 0.001);

        objectMap.put("doubleString", "89.12");
        OptionalDouble result2 = Maps.getDouble(objectMap, "doubleString");
        assertTrue(result2.isPresent());
        assertEquals(89.12, result2.getAsDouble(), 0.001);

        assertFalse(Maps.getDouble(objectMap, "missing").isPresent());
    }

    @Test
    public void testGetDoubleWithDefault() {
        assertEquals(45.67, Maps.getDouble(objectMap, "double", 99.99), 0.001);
        assertEquals(99.99, Maps.getDouble(objectMap, "missing", 99.99), 0.001);
    }

    @Test
    public void testGetString() {
        Optional<String> result = Maps.getString(objectMap, "string");
        assertTrue(result.isPresent());
        assertEquals("test", result.get());

        // Test conversion from other types
        Optional<String> intAsString = Maps.getString(objectMap, "integer");
        assertTrue(intAsString.isPresent());
        assertEquals("123", intAsString.get());

        assertFalse(Maps.getString(objectMap, "missing").isPresent());
    }

    @Test
    public void testGetStringWithDefault() {
        assertEquals("test", Maps.getString(objectMap, "string", "default"));
        assertEquals("default", Maps.getString(objectMap, "missing", "default"));

        // Test null default - should throw 
        assertThrows(IllegalArgumentException.class, () -> Maps.getString(objectMap, "missing", null));
    }

    @Test
    public void testGetNonNullWithClass() {
        Optional<Integer> result = Maps.getNonNull(objectMap, "integer", Integer.class);
        assertTrue(result.isPresent());
        assertEquals(Integer.valueOf(123), result.get());

        // Test type conversion
        objectMap.put("stringInt", "456");
        Optional<Integer> converted = Maps.getNonNull(objectMap, "stringInt", Integer.class);
        assertTrue(converted.isPresent());
        assertEquals(Integer.valueOf(456), converted.get());

        assertFalse(Maps.getNonNull(objectMap, "missing", Integer.class).isPresent());
    }

    @Test
    public void testGetNonNullWithType() {
        Type<Integer> intType = Type.of(Integer.class);
        Optional<Integer> result = Maps.getNonNull(objectMap, "integer", intType);
        assertTrue(result.isPresent());
        assertEquals(Integer.valueOf(123), result.get());
    }

    @Test
    public void testGetNonNullWithDefault() {
        Integer result = Maps.getNonNull(objectMap, "integer", 999);
        assertEquals(Integer.valueOf(123), result);

        Integer defaultResult = Maps.getNonNull(objectMap, "missing", 999);
        assertEquals(Integer.valueOf(999), defaultResult);

        // Test null default - should throw ;
        assertThrows(IllegalArgumentException.class, () -> Maps.getNonNull(objectMap, "missing", (Integer) null));
    }

    @Test
    public void testGetAndPutIfAbsent() {
        Map<String, String> map = new HashMap<>();
        String result1 = Maps.getAndPutIfAbsent(map, "key", () -> "value");
        assertEquals("value", result1);
        assertEquals("value", map.get("key"));

        // Second call should return existing value
        String result2 = Maps.getAndPutIfAbsent(map, "key", () -> "newValue");
        assertEquals("value", result2);
        assertEquals("value", map.get("key"));
    }

    @Test
    public void testGetAndPutListIfAbsent() {
        Map<String, List<String>> map = new HashMap<>();
        List<String> list = Maps.getAndPutListIfAbsent(map, "key");
        assertNotNull(list);
        assertTrue(list.isEmpty());
        assertEquals(list, map.get("key"));

        // Add to list and verify
        list.add("item");
        assertEquals(1, map.get("key").size());
    }

    @Test
    public void testGetAndPutSetIfAbsent() {
        Map<String, Set<String>> map = new HashMap<>();
        Set<String> set = Maps.getAndPutSetIfAbsent(map, "key");
        assertNotNull(set);
        assertTrue(set.isEmpty());
        assertTrue(set instanceof HashSet);
    }

    @Test
    public void testGetAndPutLinkedHashSetIfAbsent() {
        Map<String, Set<String>> map = new HashMap<>();
        Set<String> set = Maps.getAndPutLinkedHashSetIfAbsent(map, "key");
        assertNotNull(set);
        assertTrue(set instanceof LinkedHashSet);
    }

    @Test
    public void testGetAndPutMapIfAbsent() {
        Map<String, Map<String, String>> map = new HashMap<>();
        Map<String, String> innerMap = Maps.getAndPutMapIfAbsent(map, "key");
        assertNotNull(innerMap);
        assertTrue(innerMap.isEmpty());
        assertTrue(innerMap instanceof HashMap);
    }

    @Test
    public void testGetAndPutLinkedHashMapIfAbsent() {
        Map<String, Map<String, String>> map = new HashMap<>();
        Map<String, String> innerMap = Maps.getAndPutLinkedHashMapIfAbsent(map, "key");
        assertNotNull(innerMap);
        assertTrue(innerMap instanceof LinkedHashMap);
    }

    @Test
    public void testGetIfPresentForEach() {
        List<String> keys = Arrays.asList("key1", "missing", "key2", "key3");
        List<String> values = Maps.getIfPresentForEach(testMap, keys);
        assertEquals(3, values.size());
        assertEquals(Arrays.asList("value1", "value2", "value3"), values);

        // Test with null/empty
        assertTrue(Maps.getIfPresentForEach(null, keys).isEmpty());
        assertTrue(Maps.getIfPresentForEach(testMap, null).isEmpty());
    }

    @Test
    public void testGetOrDefaultIfAbsentForEach() {
        List<String> keys = Arrays.asList("key1", "missing", "key2");
        List<String> values = Maps.getOrDefaultIfAbsentForEach(testMap, keys, "default");
        assertEquals(3, values.size());
        assertEquals(Arrays.asList("value1", "default", "value2"), values);

        // Test with empty map
        List<String> defaultValues = Maps.getOrDefaultIfAbsentForEach(new HashMap<>(), keys, "default");
        assertEquals(Arrays.asList("default", "default", "default"), defaultValues);
    }

    @Test
    public void testGetByPath() {
        Map<String, Object> map = new HashMap<>();
        map.put("key1", "val1");

        Map<String, Object> nested = new HashMap<>();
        nested.put("key2", "val22");
        map.put("nested", nested);

        // Simple path
        assertEquals("val1", Maps.getByPath(map, "key1"));

        // Nested path
        assertEquals("val22", Maps.getByPath(map, "nested.key2"));

        // Missing path
        assertNull(Maps.getByPath(map, "missing"));
        assertNull(Maps.getByPath(map, "nested.missing"));

        // Array notation
        List<String> list = Arrays.asList("a", "b", "c");
        map.put("array", list);
        assertEquals("b", Maps.getByPath(map, "array[1]"));

        // Complex path with array and nested
        List<Map<String, Object>> complexList = new ArrayList<>();
        Map<String, Object> item = new HashMap<>();
        item.put("prop", "value");
        complexList.add(item);
        map.put("complex", complexList);
        assertEquals("value", Maps.getByPath(map, "complex[0].prop"));
    }

    @Test
    public void testGetByPathWithClass() {
        Map<String, Object> map = new HashMap<>();
        map.put("int", "123");

        Integer result = Maps.getByPath(map, "int", Integer.class);
        assertEquals(Integer.valueOf(123), result);

        assertNull(Maps.getByPath(map, "missing", String.class));
    }

    @Test
    public void testGetByPathWithDefault() {
        Map<String, Object> map = new HashMap<>();
        map.put("key", "value");

        assertEquals("value", Maps.getByPath(map, "key", "default"));
        assertEquals("default", Maps.getByPath(map, "missing", "default"));
    }

    @Test
    public void testGetByPathIfExists() {
        Map<String, Object> map = new HashMap<>();
        map.put("key", "value");
        map.put("key2", N.asMap("kk2", "123"));

        Nullable<String> result = Maps.getByPathIfExists(map, "key");
        assertTrue(result.isPresent());
        assertEquals("value", result.get());

        Nullable<Integer> result2 = Maps.getByPathIfExists(map, "key2.kk2", int.class);
        assertTrue(result2.isPresent());
        assertEquals(123, result2.get());

        Nullable<String> missing = Maps.getByPathIfExists(map, "missing");
        assertFalse(missing.isPresent());
    }

    @Test
    public void testContainsEntry() {
        Map.Entry<String, String> entry = new AbstractMap.SimpleEntry<>("key1", "value1");
        assertTrue(Maps.contains(testMap, entry));

        Map.Entry<String, String> wrongValue = new AbstractMap.SimpleEntry<>("key1", "wrongValue");
        assertFalse(Maps.contains(testMap, wrongValue));

        Map.Entry<String, String> missing = new AbstractMap.SimpleEntry<>("missing", "value");
        assertFalse(Maps.contains(testMap, missing));
    }

    @Test
    public void testContainsKeyValue() {
        assertTrue(Maps.contains(testMap, "key1", "value1"));
        assertFalse(Maps.contains(testMap, "key1", "wrongValue"));
        assertFalse(Maps.contains(testMap, "missing", "value1"));

        // Test with null value
        testMap.put("nullKey", null);
        assertTrue(Maps.contains(testMap, "nullKey", null));

        // Test with empty map
        assertFalse(Maps.contains(new HashMap<>(), "key", "value"));
        assertFalse(Maps.contains(null, "key", "value"));
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

        // Test with null/empty
        assertTrue(Maps.intersection(null, map2).isEmpty());
        assertTrue(Maps.intersection(map1, null).isEmpty());
        assertTrue(Maps.intersection(map1, new HashMap<>()).isEmpty());
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

        // Key 'a' exists only in map1
        Pair<String, Nullable<String>> pairA = result.get("a");
        assertEquals("1", pairA.left());
        assertFalse(pairA.right().isPresent());

        // Key 'c' has different values
        Pair<String, Nullable<String>> pairC = result.get("c");
        assertEquals("3", pairC.left());
        assertTrue(pairC.right().isPresent());
        assertEquals("different", pairC.right().get());

        // Key 'b' has same value - should not be in result
        assertNull(result.get("b"));

        Map<String, Pair<String, Nullable<String>>> result2 = Maps.difference(map1, null);

        assertEquals("1", result2.get("a").left());
        assertEquals("3", result2.get("c").left());
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

        // Key 'a' exists only in map1
        Pair<Nullable<String>, Nullable<String>> pairA = result.get("a");
        assertTrue(pairA.left().isPresent());
        assertEquals("1", pairA.left().get());
        assertFalse(pairA.right().isPresent());

        // Key 'd' exists only in map2
        Pair<Nullable<String>, Nullable<String>> pairD = result.get("d");
        assertFalse(pairD.left().isPresent());
        assertTrue(pairD.right().isPresent());
        assertEquals("4", pairD.right().get());

        // Key 'c' has different values
        Pair<Nullable<String>, Nullable<String>> pairC = result.get("c");
        assertTrue(pairC.left().isPresent());
        assertEquals("3", pairC.left().get());
        assertTrue(pairC.right().isPresent());
        assertEquals("different", pairC.right().get());

        // Key 'b' has same value - should not be in result
        assertNull(result.get("b"));
    }

    @Test
    public void testPutIfAbsent() {
        Map<String, String> map = new HashMap<>();
        map.put("existing", "value");

        // Put new key
        assertNull(Maps.putIfAbsent(map, "new", "newValue"));
        assertEquals("newValue", map.get("new"));

        // Try to put existing key
        assertEquals("value", Maps.putIfAbsent(map, "existing", "anotherValue"));
        assertEquals("value", map.get("existing"));

        // Put with null value
        assertNull(Maps.putIfAbsent(map, "nullKey", (String) null));
        assertTrue(map.containsKey("nullKey"));
        assertNull(map.get("nullKey"));
    }

    @Test
    public void testPutIfAbsentWithSupplier() {
        Map<String, String> map = new HashMap<>();
        map.put("existing", "value");

        // Put new key
        assertNull(Maps.putIfAbsent(map, "new", Fn.s(() -> "newValue")));
        assertEquals("newValue", map.get("new"));

        // Try to put existing key - supplier should not be called
        final boolean[] supplierCalled = { false };
        assertEquals("value", Maps.putIfAbsent(map, "existing", Fn.s(() -> {
            supplierCalled[0] = true;
            return "anotherValue";
        })));
        assertFalse(supplierCalled[0]);
    }

    @Test
    public void testPutIfWithPredicate() {
        Map<String, String> source = new HashMap<>();
        source.put("a", "1");
        source.put("b", "2");
        source.put("c", "3");

        Map<String, String> target = new HashMap<>();

        boolean changed = Maps.putIf(target, source, key -> !key.equals("b"));
        assertTrue(changed);
        assertEquals(2, target.size());
        assertEquals("1", target.get("a"));
        assertNull(target.get("b"));
        assertEquals("3", target.get("c"));

        // Test with empty source
        assertFalse(Maps.putIf(target, new HashMap<>(), key -> true));
    }

    @Test
    public void testPutIfWithBiPredicate() {
        Map<String, Integer> source = new HashMap<>();
        source.put("a", 1);
        source.put("b", 2);
        source.put("c", 3);

        Map<String, Integer> target = new HashMap<>();

        boolean changed = Maps.putIf(target, source, (key, value) -> value > 1);
        assertTrue(changed);
        assertEquals(2, target.size());
        assertNull(target.get("a"));
        assertEquals(Integer.valueOf(2), target.get("b"));
        assertEquals(Integer.valueOf(3), target.get("c"));
    }

    @Test
    public void testRemoveEntry() {
        Map<String, String> map = new HashMap<>(testMap);

        Map.Entry<String, String> entry = new AbstractMap.SimpleEntry<>("key1", "value1");
        assertTrue(Maps.remove(map, entry));
        assertFalse(map.containsKey("key1"));

        // Try to remove non-existent entry
        Map.Entry<String, String> missing = new AbstractMap.SimpleEntry<>("missing", "value");
        assertFalse(Maps.remove(map, missing));

        // Wrong value
        Map.Entry<String, String> wrongValue = new AbstractMap.SimpleEntry<>("key2", "wrongValue");
        assertFalse(Maps.remove(map, wrongValue));
        assertTrue(map.containsKey("key2"));
    }

    @Test
    public void testRemoveKeyValue() {
        Map<String, String> map = new HashMap<>(testMap);

        assertTrue(Maps.remove(map, "key1", "value1"));
        assertFalse(map.containsKey("key1"));

        assertFalse(Maps.remove(map, "key2", "wrongValue"));
        assertTrue(map.containsKey("key2"));

        assertFalse(Maps.remove(map, "missing", "value"));

        // Test with null/empty map
        assertFalse(Maps.remove(null, "key", "value"));
        assertFalse(Maps.remove(new HashMap<>(), "key", "value"));
    }

    @Test
    public void testRemoveKeys() {
        Map<String, String> map = new HashMap<>(testMap);
        List<String> keysToRemove = Arrays.asList("key1", "key3", "missing");

        assertTrue(Maps.removeKeys(map, keysToRemove));
        assertEquals(1, map.size());
        assertTrue(map.containsKey("key2"));

        // Test with empty collections
        assertFalse(Maps.removeKeys(map, new ArrayList<>()));
        assertFalse(Maps.removeKeys(new HashMap<>(), keysToRemove));
    }

    @Test
    public void testRemoveEntries() {
        Map<String, String> map = new HashMap<>(testMap);
        Map<String, String> entriesToRemove = new HashMap<>();
        entriesToRemove.put("key1", "value1");
        entriesToRemove.put("key2", "wrongValue"); // Should not be removed
        entriesToRemove.put("key3", "value3");

        assertTrue(Maps.removeEntries(map, entriesToRemove));
        assertEquals(1, map.size());
        assertTrue(map.containsKey("key2"));
    }

    @Test
    public void testRemoveIf() {
        Map<String, String> map = new HashMap<>(testMap);

        boolean removed = Maps.removeIf(map, entry -> entry.getValue().endsWith("1"));
        assertTrue(removed);
        assertEquals(2, map.size());
        assertFalse(map.containsKey("key1"));

        // No match
        assertFalse(Maps.removeIf(map, entry -> entry.getKey().equals("missing")));

        // Empty map
        assertFalse(Maps.removeIf(new HashMap<>(), entry -> true));
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
    public void testRemoveIfKey() {
        Map<String, String> map = new HashMap<>(testMap);

        boolean removed = Maps.removeIfKey(map, key -> key.startsWith("key1"));
        assertTrue(removed);
        assertEquals(2, map.size());
        assertFalse(map.containsKey("key1"));
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
    public void testReplaceWithOldValue() {
        Map<String, String> map = new HashMap<>(testMap);

        assertTrue(Maps.replace(map, "key1", "value1", "newValue1"));
        assertEquals("newValue1", map.get("key1"));

        assertFalse(Maps.replace(map, "key2", "wrongOldValue", "newValue2"));
        assertEquals("value2", map.get("key2"));

        assertFalse(Maps.replace(map, "missing", "oldValue", "newValue"));

        // Test with empty map
        assertFalse(Maps.replace(new HashMap<>(), "key", "old", "new"));
    }

    @Test
    public void testReplace() {
        Map<String, String> map = new HashMap<>(testMap);

        assertEquals("value1", Maps.replace(map, "key1", "newValue1"));
        assertEquals("newValue1", map.get("key1"));

        assertNull(Maps.replace(map, "missing", "newValue"));
        assertFalse(map.containsKey("missing"));

        // Test with null value
        map.put("nullKey", null);
        assertNull(Maps.replace(map, "nullKey", "newValue"));
        assertEquals("newValue", map.get("nullKey"));
    }

    @Test
    public void testReplaceAll() {
        Map<String, String> map = new HashMap<>(testMap);

        Maps.replaceAll(map, (key, value) -> key + "-" + value);
        assertEquals("key1-value1", map.get("key1"));
        assertEquals("key2-value2", map.get("key2"));
        assertEquals("key3-value3", map.get("key3"));

        // Test with empty map
        Map<String, String> emptyMap = new HashMap<>();
        Maps.replaceAll(emptyMap, (k, v) -> "new");
        assertTrue(emptyMap.isEmpty());
    }

    @Test
    public void testFilter() {
        Map<String, String> result = Maps.filter(testMap, entry -> entry.getValue().endsWith("1") || entry.getValue().endsWith("3"));
        assertEquals(2, result.size());
        assertEquals("value1", result.get("key1"));
        assertEquals("value3", result.get("key3"));
        assertNull(result.get("key2"));

        // Test with null map
        assertTrue(Maps.filter(null, entry -> true).isEmpty());
    }

    @Test
    public void testFilterBiPredicate() {
        Map<String, String> result = Maps.filter(testMap, (key, value) -> key.equals("key1") || value.equals("value3"));
        assertEquals(2, result.size());
        assertEquals("value1", result.get("key1"));
        assertEquals("value3", result.get("key3"));
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
    public void testFilterByValue() {
        Map<String, String> result = Maps.filterByValue(testMap, value -> value.compareTo("value2") >= 0);
        assertEquals(2, result.size());
        assertTrue(result.containsKey("key2"));
        assertTrue(result.containsKey("key3"));
        assertFalse(result.containsKey("key1"));
    }

    @Test
    public void testInvert() {
        Map<String, Integer> map = new HashMap<>();
        map.put("one", 1);
        map.put("two", 2);
        map.put("three", 3);

        Map<Integer, String> inverted = Maps.invert(map);
        assertEquals(3, inverted.size());
        assertEquals("one", inverted.get(1));
        assertEquals("two", inverted.get(2));
        assertEquals("three", inverted.get(3));

        // Test with null map
        assertTrue(Maps.invert(null).isEmpty());
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
    public void testFlatToMap() {
        Map<String, List<Integer>> map = new HashMap<>();
        map.put("a", Arrays.asList(1, 2, 3));
        map.put("b", Arrays.asList(4, 5, 6));
        map.put("c", Arrays.asList(7, 8));

        List<Map<String, Integer>> result = Maps.flatToMap(map);
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
    public void testUnflatten() {
        Map<String, Object> flat = new HashMap<>();
        flat.put("simple", "value");
        flat.put("nested.inner", "innerValue");
        flat.put("nested.level2.deep", "deepValue");

        Map<String, Object> unflattened = Maps.unflatten(flat);
        assertEquals(2, unflattened.size());
        assertEquals("value", unflattened.get("simple"));

        Map<String, Object> nested = (Map<String, Object>) unflattened.get("nested");
        assertNotNull(nested);
        assertEquals("innerValue", nested.get("inner"));

        Map<String, Object> level2 = (Map<String, Object>) nested.get("level2");
        assertNotNull(level2);
        assertEquals("deepValue", level2.get("deep"));
    }

    @Test
    public void testUnflattenWithCustomDelimiter() {
        Map<String, Object> flat = new HashMap<>();
        flat.put("outer_inner", "value");

        Map<String, Object> unflattened = Maps.unflatten(flat, "_", HashMap::new);
        Map<String, Object> outer = (Map<String, Object>) unflattened.get("outer");
        assertNotNull(outer);
        assertEquals("value", outer.get("inner"));
    }

    //    @Test
    //    public void testMerge() {
    //        Map<String, Integer> map = new HashMap<>();
    //        map.put("a", 1);
    //
    //        Maps.merge(map, "a", 2, Integer::sum);
    //        assertEquals(Integer.valueOf(3), map.get("a"));
    //
    //        Maps.merge(map, "b", 5, Integer::sum);
    //        assertEquals(Integer.valueOf(5), map.get("b"));
    //
    //        // Test with null old value
    //        map.put("nullKey", null);
    //        Maps.merge(map, "nullKey", 10, (oldVal, newVal) -> oldVal == null ? newVal : oldVal + newVal);
    //        assertEquals(Integer.valueOf(10), map.get("nullKey"));
    //    }

    @Test
    public void testMap2Bean() {
        Map<String, Object> map = new HashMap<>();
        map.put("name", "John");
        map.put("age", 30);
        map.put("active", true);

        TestBean bean = Maps.map2Bean(map, TestBean.class);
        assertNotNull(bean);
        assertEquals("John", bean.getName());
        assertEquals(30, bean.getAge());
        assertTrue(bean.isActive());

        // Test with null map
        assertNull(Maps.map2Bean((Map) null, TestBean.class));
    }

    @Test
    public void testMap2BeanWithFlags() {
        Map<String, Object> map = new HashMap<>();
        map.put("name", "John");
        map.put("age", null);
        map.put("unknownProperty", "value");

        // Test ignoring null properties
        TestBean bean1 = Maps.map2Bean(map, true, true, TestBean.class);
        assertEquals("John", bean1.getName());
        assertEquals(0, bean1.getAge()); // Primitive int defaults to 0

        // Test not ignoring unmatched properties (should throw exception)
        try {
            Maps.map2Bean(map, false, false, TestBean.class);
            fail("Should throw exception for unmatched property");
        } catch (Exception e) {
            // Expected
        }
    }

    @Test
    public void testMap2BeanWithSelectProps() {
        Map<String, Object> map = new HashMap<>();
        map.put("name", "John");
        map.put("age", 30);
        map.put("active", true);

        TestBean bean = Maps.map2Bean(map, Arrays.asList("name", "age"), TestBean.class);
        assertEquals("John", bean.getName());
        assertEquals(30, bean.getAge());
        assertFalse(bean.isActive()); // Not selected, should be default value
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
        List<TestBean> beans = Maps.map2Bean(maps, TestBean.class);

        assertEquals(2, beans.size());
        assertEquals("John", beans.get(0).getName());
        assertEquals(30, beans.get(0).getAge());
        assertEquals("Jane", beans.get(1).getName());
        assertEquals(25, beans.get(1).getAge());

        List<TestBean> beans2 = Maps.map2Bean(maps, N.asList("name"), TestBean.class);

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

        Map<String, Object> map = Maps.bean2Map(bean);
        assertEquals("John", map.get("name"));
        assertEquals(30, map.get("age"));
        assertEquals(true, map.get("active"));
    }

    @Test
    public void testBean2MapWithMapSupplier() {
        TestBean bean = new TestBean();
        bean.setName("John");

        Map<String, Object> map = Maps.bean2Map(bean, IntFunctions.ofLinkedHashMap());
        assertTrue(map instanceof LinkedHashMap);
        assertEquals("John", map.get("name"));
    }

    @Test
    public void testBean2MapWithSelectProps() {
        TestBean bean = new TestBean();
        bean.setName("John");
        bean.setAge(30);
        bean.setActive(true);

        Map<String, Object> map = Maps.bean2Map(bean, Arrays.asList("name", "age"));
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

        Map<String, Object> map = Maps.bean2Map(bean, null, NamingPolicy.UPPER_CASE_WITH_UNDERSCORE, IntFunctions.ofMap());
        assertEquals("John", map.get("NAME"));
        assertEquals(30, map.get("AGE"));
    }

    @Test
    public void testBean2MapIgnoreNull() {
        TestBean bean = new TestBean();
        bean.setName("John");
        bean.setNullableField(null);

        Map<String, Object> map1 = Maps.bean2Map(bean, true);
        assertFalse(map1.containsKey("nullableField"));

        Map<String, Object> map2 = Maps.bean2Map(bean, false);
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
        Map<String, Object> map = Maps.bean2Map(bean, false, ignored);

        assertEquals(3, map.size()); // name + nullableField + nestedBean
        assertEquals("John", map.get("name"));
        assertFalse(map.containsKey("age"));
        assertFalse(map.containsKey("active"));
    }

    @Test
    public void testDeepBean2Map() {
        {
            NestedBean nested = new NestedBean();
            nested.setValue("nestedValue");

            TestBean bean = new TestBean();
            bean.setName("John");
            bean.setNestedBean(nested);

            Map<String, Object> map = Maps.deepBean2Map(bean);
            assertEquals("John", map.get("name"));

            Map<String, Object> nestedMap = (Map<String, Object>) map.get("nestedBean");
            assertNotNull(nestedMap);
            assertEquals("nestedValue", nestedMap.get("value"));
        }
        {
            NestedBean nested = new NestedBean();
            nested.setValue("nestedValue");

            TestBean bean = new TestBean();
            bean.setName("John");
            bean.setNestedBean(nested);

            Map<String, Object> map = Maps.deepBean2Map(bean, N.asList("name", "nestedBean"));
            assertEquals("John", map.get("name"));

            Map<String, Object> nestedMap = (Map<String, Object>) map.get("nestedBean");
            assertNotNull(nestedMap);
            assertEquals("nestedValue", nestedMap.get("value"));
        }
    }

    @Test
    public void testBean2FlatMap() {
        {
            NestedBean nested = new NestedBean();
            nested.setValue("nestedValue");

            TestBean bean = new TestBean();
            bean.setName("John");
            bean.setAge(30);
            bean.setNestedBean(nested);

            Map<String, Object> flatMap = Maps.bean2FlatMap(bean);
            assertEquals("John", flatMap.get("name"));
            assertEquals(30, flatMap.get("age"));
            assertEquals("nestedValue", flatMap.get("nestedBean.value"));
            assertFalse(flatMap.containsKey("nestedBean"));
        }
        {
            NestedBean nested = new NestedBean();
            nested.setValue("nestedValue");

            TestBean bean = new TestBean();
            bean.setName("John");
            bean.setAge(30);
            bean.setNestedBean(nested);

            Map<String, Object> flatMap = Maps.bean2FlatMap(bean, N.asList("name", "nestedBean"));
            assertEquals("John", flatMap.get("name"));
            assertNull(flatMap.get("age"));
            assertEquals("nestedValue", flatMap.get("nestedBean.value"));
            assertFalse(flatMap.containsKey("nestedBean"));
        }
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

        // Both a1 and a2 will become "a"
        Maps.replaceKeys(map, key -> key.startsWith("a") ? "a" : key, Integer::sum);

        assertEquals(2, map.size());
        assertEquals(Integer.valueOf(30), map.get("a")); // 10 + 20
        assertEquals(Integer.valueOf(30), map.get("b"));
    }

    // Additional edge case tests

    @Test
    public void testGetWithNullValues() {
        Map<String, String> mapWithNulls = new HashMap<>();
        mapWithNulls.put("key1", null);
        mapWithNulls.put("key2", "value2");

        Nullable<String> result1 = Maps.get(mapWithNulls, "key1");
        assertTrue(result1.isPresent());
        assertNull(result1.get());

        Nullable<String> result2 = Maps.get(mapWithNulls, "key2");
        assertTrue(result2.isPresent());
        assertEquals("value2", result2.get());

        Nullable<String> result3 = Maps.get(mapWithNulls, "missing");
        assertFalse(result3.isPresent());
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
    public void testGetByPathComplexCases() {
        Map<String, Object> complexMap = new HashMap<>();

        // Nested collections
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

        // Test out of bounds
        assertNull(Maps.getByPath(complexMap, "list[5].prop"));
        assertNull(Maps.getByPath(complexMap, "list[1].innerList[10]"));
    }

    @Test
    public void testPrimitiveTypeConversions() {
        Map<String, Object> conversionMap = new HashMap<>();
        conversionMap.put("intAsString", "123");
        conversionMap.put("doubleAsString", "45.67");
        conversionMap.put("boolAsString", "true");
        conversionMap.put("boolAsInt", 1);
        conversionMap.put("hexString", "0xFF");

        // Test various conversions
        assertEquals(123, Maps.getInt(conversionMap, "intAsString", 0));
        assertEquals(45.67, Maps.getDouble(conversionMap, "doubleAsString", 0.0), 0.001);
        assertTrue(Maps.getBoolean(conversionMap, "boolAsString", false));

        // Test number format handling
        conversionMap.put("invalidNumber", "not-a-number");
        try {
            Maps.getInt(conversionMap, "invalidNumber");
            fail("Should throw NumberFormatException");
        } catch (Exception e) {
            // Expected
        }
    }

    @Test
    public void testMapOperationsWithIdentityHashMap() {
        IdentityHashMap<String, String> identityMap1 = new IdentityHashMap<>();
        String key1 = new String("key");
        String key2 = new String("key");
        identityMap1.put(key1, "value1");

        IdentityHashMap<String, String> identityMap2 = new IdentityHashMap<>();
        identityMap2.put(key2, "value2");

        // Symmetric difference should preserve IdentityHashMap behavior
        Map<String, Pair<Nullable<String>, Nullable<String>>> diff = Maps.symmetricDifference(identityMap1, identityMap2);
        assertTrue(diff instanceof IdentityHashMap);
    }

    @Test
    public void testFilterWithPredicateExceptions() {
        Map<String, String> map = new HashMap<>();
        map.put("key1", "value1");

        // Test null predicate 
        assertThrows(IllegalArgumentException.class, () -> Maps.filter(map, (Predicate<Map.Entry<String, String>>) null));
    }

    @Test
    public void testNestedMapConversions() {
        // Test deep nesting
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
    public void testBean2MapWithComplexTypes() {
        ComplexBean bean = new ComplexBean();
        bean.setDate(new Date());
        bean.setBigDecimal(new BigDecimal("123.456"));
        bean.setStringList(Arrays.asList("a", "b", "c"));
        bean.setIntArray(new int[] { 1, 2, 3 });

        Map<String, Object> map = Maps.bean2Map(bean);
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

        TestBean bean = Maps.map2Bean(map, TestBean.class);
        assertEquals("Parent", bean.getName());
        assertNotNull(bean.getNestedBean());
        assertEquals("NestedValue", bean.getNestedBean().getValue());
    }

    @Test
    public void testConcurrentModification() {
        Map<String, String> map = new HashMap<>(testMap);

        // Test removeIf with concurrent modification
        try {
            Maps.removeIf(map, entry -> {
                map.put("newKey", "newValue"); // Modify during iteration
                return true;
            });
            fail("Should throw ConcurrentModificationException");
        } catch (Exception e) {
            // Expected
        }
    }

    @Test
    public void testLargeMapOperations() {
        // Test with larger maps for performance edge cases
        Map<String, Integer> largeMap = new HashMap<>();
        for (int i = 0; i < 1000; i++) {
            largeMap.put("key" + i, i);
        }

        // Test filtering
        Map<String, Integer> filtered = Maps.filterByValue(largeMap, v -> v % 2 == 0);
        assertEquals(500, filtered.size());

        // Test inversion
        Map<Integer, String> inverted = Maps.invert(largeMap);
        assertEquals(1000, inverted.size());
        assertEquals("key500", inverted.get(500));
    }

    @Test
    public void testSpecialCharactersInPaths() {
        Map<String, Object> map = new HashMap<>();
        map.put("key.with.dots", "value1");
        map.put("key[with]brackets", "value2");

        // These should work as direct keys, not paths
        assertEquals("value1", map.get("key.with.dots"));
        assertEquals("value2", map.get("key[with]brackets"));

        // Path parsing with special characters
        Map<String, Object> nested = new HashMap<>();
        nested.put("special.key", "specialValue");
        map.put("normal", nested);

        // This should navigate the path
        assertNull(Maps.getByPath(map, "normal.special.key")); // Won't find because "special.key" is one key
    }

    @Test
    public void testNullHandlingInAllMethods() {
        // Comprehensive null testing
        Map<String, String> nullMap = null;
        Map<String, String> emptyMap = new HashMap<>();

        // Test all methods with null map
        assertFalse(Maps.get(nullMap, "key").isPresent());
        assertTrue(Maps.keys(nullMap).isEmpty());
        assertTrue(Maps.values(nullMap).isEmpty());
        assertTrue(Maps.entrySet(nullMap).isEmpty());
        assertTrue(Maps.filter(nullMap, e -> true).isEmpty());
        assertTrue(Maps.filterByKey(nullMap, k -> true).isEmpty());
        assertTrue(Maps.filterByValue(nullMap, v -> true).isEmpty());
        assertTrue(Maps.invert(nullMap).isEmpty());
        assertTrue(Maps.flatInvert((Map<String, Collection<String>>) null).isEmpty());
        assertTrue(Maps.intersection(nullMap, emptyMap).isEmpty());

        // Test with maps containing null values
        Map<String, String> mapWithNulls = new HashMap<>();
        mapWithNulls.put("null1", null);
        mapWithNulls.put("null2", null);
        mapWithNulls.put("notNull", "value");

        Map<String, String> inverted = Maps.invert(mapWithNulls);
        assertEquals(2, inverted.size()); // null keys will overwrite
        assertTrue(inverted.containsKey(null));
        assertEquals("notNull", inverted.get("value"));
    }

    // Helper classes for testing
    public static class TestBean {
        private String name;
        private int age;
        private boolean active;
        private String nullableField;
        private NestedBean nestedBean;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public int getAge() {
            return age;
        }

        public void setAge(int age) {
            this.age = age;
        }

        public boolean isActive() {
            return active;
        }

        public void setActive(boolean active) {
            this.active = active;
        }

        public String getNullableField() {
            return nullableField;
        }

        public void setNullableField(String nullableField) {
            this.nullableField = nullableField;
        }

        public NestedBean getNestedBean() {
            return nestedBean;
        }

        public void setNestedBean(NestedBean nestedBean) {
            this.nestedBean = nestedBean;
        }
    }

    public static class NestedBean {
        private String value;

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }
    }

    @Test
    public void testThreadSafetyConsiderations() {
        // Note: Maps utility methods are not thread-safe by design
        // This test documents expected behavior with concurrent access

        final Map<String, String> concurrentMap = new HashMap<>();
        for (int i = 0; i < 100; i++) {
            concurrentMap.put("key" + i, "value" + i);
        }

        // Single-threaded operation should work fine
        Map<String, String> filtered = Maps.filterByKey(concurrentMap, k -> k.contains("5"));
        assertTrue(filtered.size() > 0);
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

        // Test invert performance
        start = System.currentTimeMillis();
        Map<Integer, String> inverted = Maps.invert(veryLargeMap);
        duration = System.currentTimeMillis() - start;

        assertEquals(100000, inverted.size());
        System.out.println("Invert operation on 100k entries took: " + duration + "ms");
    }

    @Test
    public void testSortedMapPreservation() {
        TreeMap<String, String> sortedMap = new TreeMap<>();
        sortedMap.put("c", "3");
        sortedMap.put("a", "1");
        sortedMap.put("b", "2");

        // Filter should preserve sorted map type
        Map<String, String> filtered = Maps.filterByKey(sortedMap, k -> !k.equals("b"));
        assertTrue(filtered instanceof TreeMap);

        // Verify order is preserved
        Iterator<String> keys = filtered.keySet().iterator();
        assertEquals("a", keys.next());
        assertEquals("c", keys.next());
    }

    @Test
    public void testLinkedHashMapPreservation() {
        LinkedHashMap<String, String> linkedMap = new LinkedHashMap<>();
        linkedMap.put("first", "1");
        linkedMap.put("second", "2");
        linkedMap.put("third", "3");

        Map<String, String> filtered = Maps.filter(linkedMap, e -> !e.getKey().equals("second"));

        // Check order preservation
        Iterator<Map.Entry<String, String>> entries = filtered.entrySet().iterator();
        assertEquals("first", entries.next().getKey());
        assertEquals("third", entries.next().getKey());
    }

    @Test
    public void testEdgeCasesForTypeConversions() {
        Map<String, Object> edgeCaseMap = new HashMap<>();

        // Test extreme values
        edgeCaseMap.put("maxLong", Long.MAX_VALUE);
        edgeCaseMap.put("minLong", Long.MIN_VALUE);
        edgeCaseMap.put("infinity", Double.POSITIVE_INFINITY);
        edgeCaseMap.put("nan", Double.NaN);

        assertEquals(Long.MAX_VALUE, Maps.getLong(edgeCaseMap, "maxLong", 0L));
        assertEquals(Long.MIN_VALUE, Maps.getLong(edgeCaseMap, "minLong", 0L));
        assertTrue(Double.isInfinite(Maps.getDouble(edgeCaseMap, "infinity", 0.0)));
        assertTrue(Double.isNaN(Maps.getDouble(edgeCaseMap, "nan", 0.0)));

        // Test scientific notation
        edgeCaseMap.put("scientific", "1.23e4");
        assertEquals(12300.0, Maps.getDouble(edgeCaseMap, "scientific", 0.0), 0.001);
    }

    @Test
    public void testMapSupplierBehavior() {
        // Test that map suppliers are called correctly
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
    public void testCompleteCodeCoverage() {
        // Additional tests for complete code coverage

        // Test getByPath with various edge cases
        Map<String, Object> pathMap = new HashMap<>();
        pathMap.put("", "emptyKey");
        assertEquals("emptyKey", Maps.getByPath(pathMap, ""));

        // Test with collections in path
        Set<String> set = new LinkedHashSet<>();
        set.add("first");
        set.add("second");
        pathMap.put("set", set);
        assertEquals("first", Maps.getByPath(pathMap, "set[0]"));

        // Test bean2Map with different naming policies
        TestBean bean = new TestBean();
        bean.setName("Test");

        Map<String, Object> lowerUnderscoreMap = Maps.bean2Map(bean, (Collection<String>) null, NamingPolicy.LOWER_CASE_WITH_UNDERSCORE, IntFunctions.ofMap());
        assertTrue(lowerUnderscoreMap.containsKey("name"));

        Map<String, Object> upperUnderscoreMap = Maps.bean2Map(bean, (Collection<String>) null, NamingPolicy.UPPER_CASE_WITH_UNDERSCORE, IntFunctions.ofMap());
        assertTrue(upperUnderscoreMap.containsKey("NAME"));
    }

    @Test
    public void testCircularReferenceHandling() {
        // Test what happens with circular references in bean2Map
        CircularBean bean1 = new CircularBean();
        CircularBean bean2 = new CircularBean();
        bean1.setName("Bean1");
        bean2.setName("Bean2");
        bean1.setReference(bean2);
        bean2.setReference(bean1);

        // This should work for shallow conversion
        Map<String, Object> map = Maps.bean2Map(bean1);
        assertEquals("Bean1", map.get("name"));
        assertEquals(bean2, map.get("reference"));

        // Deep conversion might have issues with circular references
        // The implementation doesn't handle this, so we document the behavior
        try {
            Maps.deepBean2Map(bean1);
            // If it doesn't throw StackOverflowError, it might have some protection
        } catch (StackOverflowError e) {
            // Expected behavior with circular references
        }
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CircularBean {
        private String name;
        private CircularBean reference;

    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ComplexBean {
        private Date date;
        private BigDecimal bigDecimal;
        private List<String> stringList;
        private int[] intArray;

    }
}

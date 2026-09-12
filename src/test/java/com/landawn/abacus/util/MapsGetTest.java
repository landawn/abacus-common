package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

import org.junit.jupiter.api.Test;

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

public class MapsGetTest extends MapsTestSupport {
    @Test
    public void testGet() {
        Nullable<String> result = Maps.getIfExists(testMap, "key1");
        assertTrue(result.isPresent());
        assertEquals("value1", result.get());

        Nullable<String> missing = Maps.getIfExists(testMap, "missing");
        assertFalse(missing.isPresent());

        objectMap.put("nullKey", null);
        Nullable<Object> nullResult = Maps.getIfExists(objectMap, "nullKey");
        assertTrue(nullResult.isPresent());
        assertNull(nullResult.get());

        Map<String, String> mapWithNulls = new HashMap<>();
        mapWithNulls.put("a", "apple");
        mapWithNulls.put("b", null);
        assertEquals(Nullable.of("apple"), Maps.getIfExists(mapWithNulls, "a"));
        assertEquals(Nullable.of(null), Maps.getIfExists(mapWithNulls, "b"));
        assertEquals(Nullable.empty(), Maps.getIfExists(mapWithNulls, "c"));

        assertFalse(Maps.getIfExists(null, "key").isPresent());
        assertFalse(Maps.getIfExists(new HashMap<>(), "key").isPresent());
        assertEquals(Nullable.empty(), Maps.getIfExists((Map<String, String>) null, "a"));
    }

    @Test
    public void testGetNested() {
        Nullable<String> result = Maps.getIfExists(nestedMap, "outer1", "innerKey1");
        assertTrue(result.isPresent());
        assertEquals("innerValue1", result.get());

        assertFalse(Maps.getIfExists(nestedMap, "missing", "innerKey1").isPresent());
        assertFalse(Maps.getIfExists(nestedMap, "outer1", "missing").isPresent());
        assertFalse(Maps.getIfExists(null, "key", "key2").isPresent());

        Map<String, Map<String, Integer>> map = new HashMap<>();
        Map<String, Integer> innerMap = new HashMap<>();
        innerMap.put("x", 10);
        innerMap.put("y", null);
        map.put("outer", innerMap);

        assertEquals(Nullable.of(10), Maps.getIfExists(map, "outer", "x"));
        assertEquals(Nullable.of(null), Maps.getIfExists(map, "outer", "y"));
        assertEquals(Nullable.empty(), Maps.getIfExists(map, "outer", "z"));
        assertEquals(Nullable.empty(), Maps.getIfExists(map, "otherOuter", "x"));
        assertEquals(Nullable.empty(), Maps.getIfExists(null, "outer", "x"));
    }

    @Test
    public void testGetIfExists_threeKeys() {
        Map<String, Map<String, Map<String, Integer>>> tripleNested = new HashMap<>();
        Map<String, Map<String, Integer>> middle = new HashMap<>();
        Map<String, Integer> inner = new HashMap<>();
        inner.put("val", 42);
        inner.put("nullVal", null);
        middle.put("mid", inner);
        tripleNested.put("top", middle);

        Nullable<Integer> result = Maps.getIfExists(tripleNested, "top", "mid", "val");
        assertTrue(result.isPresent());
        assertEquals(42, result.get().intValue());

        Nullable<Integer> nullResult = Maps.getIfExists(tripleNested, "top", "mid", "nullVal");
        assertTrue(nullResult.isPresent());
        assertNull(nullResult.get());

        assertFalse(Maps.getIfExists(tripleNested, "top", "mid", "missing").isPresent());
        assertFalse(Maps.getIfExists(tripleNested, "top", "missing", "val").isPresent());
        assertFalse(Maps.getIfExists(tripleNested, "missing", "mid", "val").isPresent());
        assertFalse(Maps.getIfExists((Map<String, Map<String, Map<String, Integer>>>) null, "top", "mid", "val").isPresent());
    }

    @Test
    public void testGetByPath_withDefaultValue() {
        Map<String, Object> map = Map.of("name", "Test");
        assertEquals("Test", Maps.getByPathAsOrDefaultIfAbsent(map, "name", "Default", String.class));
        assertEquals("Default", Maps.getByPathAsOrDefaultIfAbsent(map, "address", "Default", String.class));
        assertEquals(Integer.valueOf(123), Maps.getByPathAsOrDefaultIfAbsent(map, "name_no", 123, Integer.class));
        assertThrows(IllegalArgumentException.class, () -> Maps.getByPathAsOrDefaultIfAbsent(map, "address", (String) null, String.class));
        assertThrows(IllegalArgumentException.class, () -> Maps.getByPathAsOrDefaultIfAbsent(map, "name", "Default", (Class<String>) null));
        assertEquals("default", Maps.getByPathAsOrDefaultIfAbsent(null, "key", "default", String.class));
    }

    @Test
    public void test_getByPath() {
        Map map = CommonUtil.asMap("key1", "val1");
        assertEquals("val1", Maps.getByPath(map, "key1"));

        map = CommonUtil.asMap("key1", CommonUtil.toList("val1"));
        assertEquals("val1", Maps.getByPath(map, "key1[0]"));

        map = CommonUtil.asMap("key1", CommonUtil.toSet("val1"));
        assertEquals("val1", Maps.getByPath(map, "key1[0]"));

        map = CommonUtil.asMap("key1", CommonUtil.toList(CommonUtil.toLinkedHashSet("val1", "val2")));
        assertEquals("val2", Maps.getByPath(map, "key1[0][1]"));

        map = CommonUtil.asMap("key1", CommonUtil.toSet(CommonUtil.toList(CommonUtil.toSet("val1"))));
        assertEquals("val1", Maps.getByPath(map, "key1[0][0][0]"));

        map = CommonUtil.asMap("key1", CommonUtil.toList(CommonUtil.toLinkedHashSet("val1", CommonUtil.asMap("key2", "val22"))));
        assertEquals("val22", Maps.getByPath(map, "key1[0][1].key2"));

        map = CommonUtil.asMap("key1",
                CommonUtil.toList(CommonUtil.toLinkedHashSet("val1", CommonUtil.asMap("key2", CommonUtil.toList("val22", CommonUtil.asMap("key3", "val33"))))));
        assertEquals("val33", Maps.getByPath(map, "key1[0][1].key2[1].key3"));

        map = CommonUtil.asMap("key1",
                CommonUtil.toList(CommonUtil.toLinkedHashSet("val1", CommonUtil.asMap("key2", CommonUtil.toList("val22", CommonUtil.asMap("key3", "val33"))))));
        assertNull(Maps.getByPath(map, "key1[0][2].key2[1].key3"));

        map = CommonUtil.asMap("key1",
                CommonUtil.toList(CommonUtil.toLinkedHashSet("val1", CommonUtil.asMap("key2", CommonUtil.toList("val22", CommonUtil.asMap("key3", "val33"))))));
        assertNull(Maps.getByPath(map, "key1[0][1].key22[1].key3"));

        map = CommonUtil.asMap("key1", CommonUtil.asMap("key2", null));
        assertNull(Maps.getByPath(map, "key1.key2.key3"));
    }

    @Test
    public void testGetByPath() {
        Map<String, Object> map = new HashMap<>();
        map.put("key1", "val1");

        Map<String, Object> nested = new HashMap<>();
        nested.put("key2", "val22");
        map.put("nested", nested);

        assertEquals("val1", Maps.getByPath(map, "key1"));

        assertEquals("val22", Maps.getByPath(map, "nested.key2"));

        assertNull(Maps.getByPath(map, "missing"));
        assertNull(Maps.getByPath(map, "nested.missing"));

        List<String> list = Arrays.asList("a", "b", "c");
        map.put("array", list);
        assertEquals("b", Maps.getByPath(map, "array[1]"));

        List<Map<String, Object>> complexList = new ArrayList<>();
        Map<String, Object> item = new HashMap<>();
        item.put("prop", "value");
        complexList.add(item);
        map.put("complex", complexList);
        assertEquals("value", Maps.getByPath(map, "complex[0].prop"));

        Map<String, Object> simple = Map.of("key1", "val1");
        assertEquals("val1", Maps.getByPath(simple, "key1"));
        assertNull(Maps.getByPath(simple, "key2"));
        assertFalse(Maps.getByPathIfExists(null, "key").isPresent());
    }

    @Test
    public void testGetByPathWithDefault() {
        Map<String, Object> map = new HashMap<>();
        map.put("key", "value");

        assertEquals("value", Maps.getByPathAsOrDefaultIfAbsent(map, "key", "default", String.class));
        assertEquals("default", Maps.getByPathAsOrDefaultIfAbsent(map, "missing", "default", String.class));
    }

    @Test
    public void testGetByPathAsOrDefaultIfAbsent_appliesDefaultForPresentNull() {
        final Map<String, Object> map = new HashMap<>();
        final Map<String, Object> nested = new HashMap<>();

        map.put("key", null);
        map.put("nested", nested);
        map.put("list", Arrays.asList((Object) null));
        nested.put("value", null);

        assertEquals("default", Maps.getByPathAsOrDefaultIfAbsent(map, "key", "default", String.class));
        assertEquals("default", Maps.getByPathAsOrDefaultIfAbsent(map, "nested.value", "default", String.class));
        assertEquals("default", Maps.getByPathAsOrDefaultIfAbsent(map, "list[0]", "default", String.class));
        assertEquals("default", Maps.getByPathAsOrDefaultIfAbsent(map, "missing", "default", String.class));
        assertEquals("default", Maps.getByPathAsOrDefaultIfAbsent(map, "nested.missing", "default", String.class));
    }

    @Test
    public void testGetByPathAsOrDefaultIfAbsentUsesNullAsAbsentForCustomMaps() {
        final Map<String, Object> map = new HashMap<>() {
            @Override
            public Object getOrDefault(final Object key, final Object defaultValue) {
                final Object val = get(key);
                return val == null ? defaultValue : val;
            }
        };

        final Map<String, Object> nested = new HashMap<>() {
            @Override
            public Object getOrDefault(final Object key, final Object defaultValue) {
                final Object val = get(key);
                return val == null ? defaultValue : val;
            }
        };

        map.put("", null);
        map.put("key", null);
        map.put("nested", nested);
        nested.put("value", null);

        assertEquals("default", Maps.getByPathAsOrDefaultIfAbsent(map, "", "default", String.class));
        assertEquals("default", Maps.getByPathAsOrDefaultIfAbsent(map, "key", "default", String.class));
        assertEquals("default", Maps.getByPathAsOrDefaultIfAbsent(map, "nested.value", "default", String.class));
    }

    @Test
    public void testGetByPath_returnsDefaultForInvalidTraversal() {
        Map<String, Object> map = new HashMap<>();
        map.put("scalar", "value");
        map.put("list", Arrays.asList("first"));
        map.put("nestedLists", Arrays.asList(Arrays.asList("nested")));

        assertNull(Maps.getByPath(map, "scalar.child"));
        assertNull(Maps.getByPath(map, "scalar[0]"));
        assertNull(Maps.getByPath(map, "list[-1]"));
        assertNull(Maps.getByPath(map, "list[abc]"));
        assertNull(Maps.getByPath(map, "list[0].child"));
        assertNull(Maps.getByPath(map, "nestedLists[0][0][0]"));
        assertEquals("fallback", Maps.getByPathAsOrDefaultIfAbsent(map, "list[-1]", "fallback", String.class));
        assertFalse(Maps.getByPathIfExists(map, "scalar.child").isPresent());
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
    public void testGetByPathAsOrDefaultIfAbsent_NestedCollectionIndexDefaultValue() {
        final Map<String, Object> map = new HashMap<>();
        map.put("items", Arrays.asList(Collections.singletonMap("count", "5")));

        assertEquals(Integer.valueOf(5), Maps.getByPathAsOrDefaultIfAbsent(map, "items[0].count", 0, Integer.class));
        assertEquals("fallback", Maps.getByPathAsOrDefaultIfAbsent(map, "items[1].count", "fallback", String.class));
        assertEquals("fallback", Maps.getByPathAsOrDefaultIfAbsent(map, "items[0].missing", "fallback", String.class));
    }

    @Test
    public void testGetByPathAsInt() {
        final Map<String, Object> map = new HashMap<>();
        map.put("user", CommonUtil.asMap("age", "25", "score", 98, "nullValue", null));

        final OptionalInt age = Maps.getByPathAsInt(map, "user.age");
        assertTrue(age.isPresent());
        assertEquals(25, age.getAsInt());

        final OptionalInt score = Maps.getByPathAsInt(map, "user.score");
        assertTrue(score.isPresent());
        assertEquals(98, score.getAsInt());

        assertFalse(Maps.getByPathAsInt(map, "user.nullValue").isPresent());
        assertFalse(Maps.getByPathAsInt(map, "user.missing").isPresent());
        assertFalse(Maps.getByPathAsInt(null, "user.age").isPresent());
    }

    @Test
    public void testGetByPathAsIntOrDefaultIfAbsent() {
        final Map<String, Object> map = new HashMap<>();
        map.put("user", CommonUtil.asMap("age", "25", "nullValue", null));

        assertEquals(25, Maps.getByPathAsIntOrDefaultIfAbsent(map, "user.age", -1));
        assertEquals(-1, Maps.getByPathAsIntOrDefaultIfAbsent(map, "user.nullValue", -1));
        assertEquals(-1, Maps.getByPathAsIntOrDefaultIfAbsent(map, "user.missing", -1));
        assertEquals(-1, Maps.getByPathAsIntOrDefaultIfAbsent(null, "user.age", -1));
    }

    @Test
    public void testGetByPathAsString() {
        final Map<String, Object> map = new HashMap<>();
        map.put("user", CommonUtil.asMap("name", "John", "age", 25, "nullValue", null));

        final Optional<String> name = Maps.getByPathAsString(map, "user.name");
        assertTrue(name.isPresent());
        assertEquals("John", name.get());

        final Optional<String> age = Maps.getByPathAsString(map, "user.age");
        assertTrue(age.isPresent());
        assertEquals("25", age.get());

        assertFalse(Maps.getByPathAsString(map, "user.nullValue").isPresent());
        assertFalse(Maps.getByPathAsString(map, "user.missing").isPresent());
        assertFalse(Maps.getByPathAsString(null, "user.name").isPresent());
    }

    @Test
    public void testGetByPathAsStringOrDefaultIfAbsent() {
        final Map<String, Object> map = new HashMap<>();
        map.put("user", CommonUtil.asMap("name", "John", "age", 25, "nullValue", null));

        assertEquals("John", Maps.getByPathAsStringOrDefaultIfAbsent(map, "user.name", "Unknown"));
        assertEquals("25", Maps.getByPathAsStringOrDefaultIfAbsent(map, "user.age", "Unknown"));
        assertEquals("Unknown", Maps.getByPathAsStringOrDefaultIfAbsent(map, "user.nullValue", "Unknown"));
        assertEquals("Unknown", Maps.getByPathAsStringOrDefaultIfAbsent(map, "user.missing", "Unknown"));
        assertEquals("Unknown", Maps.getByPathAsStringOrDefaultIfAbsent(null, "user.name", "Unknown"));
        assertThrows(IllegalArgumentException.class, () -> Maps.getByPathAsStringOrDefaultIfAbsent(map, "user.name", null));
    }

    @Test
    public void testGetByPathAs() {
        final Map<String, Object> map = new HashMap<>();
        map.put("user", CommonUtil.asMap("age", "25", "active", "true", "nullValue", null));

        final Optional<Integer> age = Maps.getByPathAs(map, "user.age", Integer.class);
        assertTrue(age.isPresent());
        assertEquals(Integer.valueOf(25), age.get());

        final Optional<Boolean> active = Maps.getByPathAs(map, "user.active", Boolean.class);
        assertTrue(active.isPresent());
        assertEquals(Boolean.TRUE, active.get());

        assertFalse(Maps.getByPathAs(map, "user.nullValue", Integer.class).isPresent());
        assertFalse(Maps.getByPathAs(map, "user.missing", Integer.class).isPresent());
        assertFalse(Maps.getByPathAs(null, "user.age", Integer.class).isPresent());
        assertThrows(IllegalArgumentException.class, () -> Maps.getByPathAs(map, "user.age", (Class<Integer>) null));
    }

    @Test
    public void test_getOrDefault() {
        Map<String, Integer> map = CommonUtil.asMap("a", 1, "b", 2, "c", 3);

        assertEquals(1, Maps.getOrDefaultIfAbsent(map, "a", 0).intValue());
        assertEquals(0, Maps.getOrDefaultIfAbsent(map, "d", 0).intValue());

        assertEquals(CommonUtil.toList(1, 0), Maps.getValuesOrDefaultIfAbsent(map, CommonUtil.toList("a", "d"), 0));

        assertEquals(CommonUtil.toList(1), Maps.getValuesIfPresent(map, CommonUtil.toList("a", "d")));
    }

    @Test
    public void testGetOrDefaultIfAbsentForEach() {
        List<String> keys = Arrays.asList("key1", "missing", "key2");
        List<String> values = Maps.getValuesOrDefaultIfAbsent(testMap, keys, "default");
        assertEquals(3, values.size());
        assertEquals(Arrays.asList("value1", "default", "value2"), values);

        List<String> defaultValues = Maps.getValuesOrDefaultIfAbsent(new HashMap<>(), keys, "default");
        assertEquals(Arrays.asList("default", "default", "default"), defaultValues);
    }

    @Test
    public void testGetOrDefaultIfAbsent_withSupplier() {
        Map<String, String> map = new HashMap<>();
        map.put("key1", "value1");

        assertEquals("value1", Maps.getOrDefaultIfAbsent(map, "key1", () -> "default"));
        assertEquals("default", Maps.getOrDefaultIfAbsent(map, "missing", () -> "default"));
        assertEquals("default", Maps.getOrDefaultIfAbsent((Map<String, String>) null, "key", () -> "default"));
        assertEquals("default", Maps.getOrDefaultIfAbsent(new HashMap<>(), "key", () -> "default"));
    }

    @Test
    public void testGetOrDefaultIfAbsent() {
        assertEquals("value1", Maps.getOrDefaultIfAbsent(testMap, "key1", "default"));
        assertEquals("default", Maps.getOrDefaultIfAbsent(testMap, "missing", "default"));

        assertThrows(IllegalArgumentException.class, () -> Maps.getOrDefaultIfAbsent(testMap, "key1", (String) null));
    }

    @Test
    public void testGetOrDefaultIfAbsentNested() {
        assertEquals("innerValue1", Maps.getOrDefaultIfAbsent(nestedMap, "outer1", "innerKey1", "default"));
        assertEquals("default", Maps.getOrDefaultIfAbsent(nestedMap, "outer1", "missing", "default"));
        assertEquals("default", Maps.getOrDefaultIfAbsent(nestedMap, "missing", "innerKey1", "default"));

        assertThrows(IllegalArgumentException.class, () -> Maps.getOrDefaultIfAbsent(nestedMap, "outer1", "innerKey1", null));

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
        List<String> result = Maps.getOrEmptyListIfAbsent(listMap, "list1");
        assertEquals(3, result.size());
        assertEquals(Arrays.asList("a", "b", "c"), result);

        List<String> empty = Maps.getOrEmptyListIfAbsent(listMap, "missing");
        assertTrue(empty.isEmpty());

        listMap.put("nullList", null);
        List<String> nullResult = Maps.getOrEmptyListIfAbsent(listMap, "nullList");
        assertTrue(nullResult.isEmpty());

        List<String> fromNull = Maps.getOrEmptyListIfAbsent(null, "key");
        assertNotNull(fromNull);
        assertTrue(fromNull.isEmpty());
    }

    @Test
    public void testGetOrEmptySetIfAbsent() {
        Set<String> result = Maps.getOrEmptySetIfAbsent(setMap, "set1");
        assertEquals(3, result.size());
        assertTrue(result.contains("x"));

        Set<String> empty = Maps.getOrEmptySetIfAbsent(setMap, "missing");
        assertTrue(empty.isEmpty());

        Set<String> fromNull = Maps.getOrEmptySetIfAbsent(null, "key");
        assertNotNull(fromNull);
        assertTrue(fromNull.isEmpty());
    }

    @Test
    public void testGetOrEmptyMapIfAbsent() {
        Map<String, String> result = Maps.getOrEmptyMapIfAbsent(nestedMap, "outer1");
        assertEquals(2, result.size());
        assertEquals("innerValue1", result.get("innerKey1"));

        Map<String, String> empty = Maps.getOrEmptyMapIfAbsent(nestedMap, "missing");
        assertTrue(empty.isEmpty());

        Map<String, String> fromNull = Maps.getOrEmptyMapIfAbsent(null, "key");
        assertNotNull(fromNull);
        assertTrue(fromNull.isEmpty());
    }

    @Test
    public void testGetAndPutIfAbsent() {
        Map<String, String> map = new HashMap<>();
        String result1 = Maps.getOrPutIfAbsent(map, "key", () -> "value");
        assertEquals("value", result1);
        assertEquals("value", map.get("key"));

        String result2 = Maps.getOrPutIfAbsent(map, "key", () -> "newValue");
        assertEquals("value", result2);
        assertEquals("value", map.get("key"));
    }

    @Test
    public void testGetOrPutIfAbsent_existingKey() {
        Map<String, String> map = new HashMap<>();
        map.put("key", "existing");
        assertEquals("existing", Maps.getOrPutIfAbsent(map, "key", () -> "new"));
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
    public void testGetOrPutListIfAbsent_existingKey() {
        Map<String, List<String>> map = new HashMap<>();
        map.put("key", Arrays.asList("a"));
        assertEquals(Arrays.asList("a"), Maps.getOrPutListIfAbsent(map, "key"));
    }

    @Test
    public void testGetAndPutListIfAbsent() {
        Map<String, List<String>> map = new HashMap<>();
        List<String> list = Maps.getOrPutListIfAbsent(map, "key");
        assertNotNull(list);
        assertTrue(list.isEmpty());
        assertEquals(list, map.get("key"));

        list.add("item");
        assertEquals(1, map.get("key").size());
    }

    @Test
    public void testGetOrPutSetIfAbsent_existingKey() {
        Map<String, Set<String>> map = new HashMap<>();
        Set<String> existing = new HashSet<>(Arrays.asList("a"));
        map.put("key", existing);
        assertEquals(existing, Maps.getOrPutSetIfAbsent(map, "key"));
    }

    @Test
    public void testGetAndPutSetIfAbsent() {
        Map<String, Set<String>> map = new HashMap<>();
        Set<String> set = Maps.getOrPutSetIfAbsent(map, "key");
        assertNotNull(set);
        assertTrue(set.isEmpty());
        assertTrue(set instanceof HashSet);
    }

    @Test
    public void testGetAndPutLinkedHashSetIfAbsent() {
        Map<String, Set<String>> map = new HashMap<>();
        Set<String> set = Maps.getOrPutLinkedHashSetIfAbsent(map, "key");
        assertNotNull(set);
        assertTrue(set instanceof LinkedHashSet);
    }

    @Test
    public void testGetOrPutLinkedHashSetIfAbsent_newKey() {
        Map<String, Set<String>> map = new HashMap<>();
        Set<String> result = Maps.getOrPutLinkedHashSetIfAbsent(map, "newKey");
        assertNotNull(result);
        assertTrue(result.isEmpty());
        assertTrue(result instanceof LinkedHashSet);
    }

    @Test
    public void testGetOrPutLinkedHashSetIfAbsent() {
        Map<String, Set<String>> map = new HashMap<>();
        Set<String> set = Maps.getOrPutLinkedHashSetIfAbsent(map, "key1");
        assertNotNull(set);
        assertTrue(set instanceof LinkedHashSet);
        set.add("val1");

        Set<String> sameSet = Maps.getOrPutLinkedHashSetIfAbsent(map, "key1");
        assertSame(set, sameSet);
        assertEquals(1, sameSet.size());
    }

    @Test
    public void testGetOrPutMapIfAbsent_existingKey() {
        Map<String, Map<String, String>> map = new HashMap<>();
        Map<String, String> existing = new HashMap<>();
        existing.put("a", "b");
        map.put("key", existing);
        assertEquals(existing, Maps.getOrPutMapIfAbsent(map, "key"));
    }

    @Test
    public void testGetAndPutMapIfAbsent() {
        Map<String, Map<String, String>> map = new HashMap<>();
        Map<String, String> innerMap = Maps.getOrPutMapIfAbsent(map, "key");
        assertNotNull(innerMap);
        assertTrue(innerMap.isEmpty());
        assertTrue(innerMap instanceof HashMap);
    }

    @Test
    public void testGetAndPutLinkedHashMapIfAbsent() {
        Map<String, Map<String, String>> map = new HashMap<>();
        Map<String, String> innerMap = Maps.getOrPutLinkedHashMapIfAbsent(map, "key");
        assertNotNull(innerMap);
        assertTrue(innerMap instanceof LinkedHashMap);
    }

    @Test
    public void testGetOrPutLinkedHashMapIfAbsent_newKey() {
        Map<String, Map<String, String>> map = new HashMap<>();
        Map<String, String> result = Maps.getOrPutLinkedHashMapIfAbsent(map, "newKey");
        assertNotNull(result);
        assertTrue(result.isEmpty());
        assertTrue(result instanceof LinkedHashMap);
    }

    @Test
    public void testGetOrPutLinkedHashMapIfAbsent() {
        Map<String, Map<String, Integer>> outer = new HashMap<>();
        Map<String, Integer> inner = Maps.getOrPutLinkedHashMapIfAbsent(outer, "section1");
        assertNotNull(inner);
        assertTrue(inner instanceof LinkedHashMap);
        inner.put("k", 42);

        Map<String, Integer> sameInner = Maps.getOrPutLinkedHashMapIfAbsent(outer, "section1");
        assertSame(inner, sameInner);
        assertEquals(Integer.valueOf(42), sameInner.get("k"));
    }

    @Test
    public void testGetAsBoolean() {
        objectMap.put("trueString", "true");
        objectMap.put("falseString", "false");
        objectMap.put("boolTrue", Boolean.TRUE);

        OptionalBoolean result1 = Maps.getAsBoolean(objectMap, "boolean");
        assertTrue(result1.isPresent());
        assertTrue(result1.get());

        OptionalBoolean result2 = Maps.getAsBoolean(objectMap, "trueString");
        assertTrue(result2.isPresent());
        assertTrue(result2.get());

        OptionalBoolean result3 = Maps.getAsBoolean(objectMap, "falseString");
        assertTrue(result3.isPresent());
        assertFalse(result3.get());

        assertFalse(Maps.getAsBoolean(objectMap, "missing").isPresent());
        assertFalse(Maps.getAsBoolean(null, "key").isPresent());
        assertFalse(Maps.getAsBoolean(new HashMap<>(), "key").isPresent());
    }

    @Test
    public void testGetAsBooleanOrDefaultIfAbsent() {
        assertTrue(Maps.getAsBooleanOrDefaultIfAbsent(objectMap, "boolean", false));
        assertTrue(Maps.getAsBooleanOrDefaultIfAbsent(objectMap, "missing", true));
        assertFalse(Maps.getAsBooleanOrDefaultIfAbsent(objectMap, "missing", false));
        assertFalse(Maps.getAsBooleanOrDefaultIfAbsent(null, "key", false));
        assertTrue(Maps.getAsBooleanOrDefaultIfAbsent(null, "key", true));
        assertFalse(Maps.getAsBooleanOrDefaultIfAbsent(new HashMap<>(), "key", false));
    }

    @Test
    public void testGetAsChar_NumericCodeString() {
        Map<String, Object> map = new HashMap<>();
        map.put("code", "65");
        map.put("empty", "");

        assertEquals('A', Maps.getAsChar(map, "code").orElse('\0'));
        assertEquals('\0', Maps.getAsChar(map, "empty").orElse('Z'));
        assertEquals('A', Maps.getAsCharOrDefaultIfAbsent(map, "code", 'Z'));
        assertThrows(IllegalArgumentException.class, () -> {
            map.put("bad", "-1");
            Maps.getAsChar(map, "bad");
        });
    }

    @Test
    public void testGetAsChar() {
        objectMap.put("charString", "B");

        OptionalChar result1 = Maps.getAsChar(objectMap, "char");
        assertTrue(result1.isPresent());
        assertEquals('A', result1.get());

        OptionalChar result2 = Maps.getAsChar(objectMap, "charString");
        assertTrue(result2.isPresent());
        assertEquals('B', result2.get());

        assertFalse(Maps.getAsChar(objectMap, "missing").isPresent());
        assertFalse(Maps.getAsChar(null, "key").isPresent());
        assertFalse(Maps.getAsChar(new HashMap<>(), "key").isPresent());
    }

    @Test
    public void testGetAsCharOrDefaultIfAbsent() {
        assertEquals('A', Maps.getAsCharOrDefaultIfAbsent(objectMap, "char", 'Z'));
        assertEquals('Z', Maps.getAsCharOrDefaultIfAbsent(objectMap, "missing", 'Z'));
        assertEquals('X', Maps.getAsCharOrDefaultIfAbsent(Map.of("key", "X"), "key", 'Z'));
        assertEquals('x', Maps.getAsCharOrDefaultIfAbsent(null, "key", 'x'));
        assertEquals('x', Maps.getAsCharOrDefaultIfAbsent(new HashMap<>(), "key", 'x'));
    }

    @Test
    public void testGetAsByte() {
        objectMap.put("byteString", "20");

        OptionalByte result1 = Maps.getAsByte(objectMap, "byte");
        assertTrue(result1.isPresent());
        assertEquals(10, result1.get());

        OptionalByte result2 = Maps.getAsByte(objectMap, "byteString");
        assertTrue(result2.isPresent());
        assertEquals(20, result2.get());

        assertFalse(Maps.getAsByte(objectMap, "missing").isPresent());
        assertFalse(Maps.getAsByte(null, "key").isPresent());
        assertFalse(Maps.getAsByte(new HashMap<>(), "key").isPresent());
    }

    @Test
    public void testGetAsByteOrDefaultIfAbsent() {
        assertEquals(10, Maps.getAsByteOrDefaultIfAbsent(objectMap, "byte", (byte) 99));
        assertEquals(99, Maps.getAsByteOrDefaultIfAbsent(objectMap, "missing", (byte) 99));
        assertEquals((byte) 42, Maps.getAsByteOrDefaultIfAbsent(Map.of("key", "42"), "key", (byte) 0));
        assertEquals((byte) 5, Maps.getAsByteOrDefaultIfAbsent(null, "key", (byte) 5));
        assertEquals((byte) 5, Maps.getAsByteOrDefaultIfAbsent(new HashMap<>(), "key", (byte) 5));
    }

    @Test
    public void testGetAsShort() {
        objectMap.put("shortString", "200");

        OptionalShort result1 = Maps.getAsShort(objectMap, "short");
        assertTrue(result1.isPresent());
        assertEquals(100, result1.get());

        OptionalShort result2 = Maps.getAsShort(objectMap, "shortString");
        assertTrue(result2.isPresent());
        assertEquals(200, result2.get());

        assertFalse(Maps.getAsShort(objectMap, "missing").isPresent());
        assertFalse(Maps.getAsShort(null, "key").isPresent());
        assertFalse(Maps.getAsShort(new HashMap<>(), "key").isPresent());
    }

    @Test
    public void testGetAsShortOrDefaultIfAbsent() {
        assertEquals(100, Maps.getAsShortOrDefaultIfAbsent(objectMap, "short", (short) 999));
        assertEquals(999, Maps.getAsShortOrDefaultIfAbsent(objectMap, "missing", (short) 999));
        assertEquals((short) 100, Maps.getAsShortOrDefaultIfAbsent(Map.of("key", "100"), "key", (short) 0));
        assertEquals((short) 10, Maps.getAsShortOrDefaultIfAbsent(null, "key", (short) 10));
        assertEquals((short) 10, Maps.getAsShortOrDefaultIfAbsent(new HashMap<>(), "key", (short) 10));
    }

    @Test
    public void testGetAsInt() {
        OptionalInt result = Maps.getAsInt(objectMap, "integer");
        assertTrue(result.isPresent());
        assertEquals(123, result.getAsInt());

        objectMap.put("intString", "456");
        OptionalInt result2 = Maps.getAsInt(objectMap, "intString");
        assertTrue(result2.isPresent());
        assertEquals(456, result2.getAsInt());

        assertFalse(Maps.getAsInt(objectMap, "missing").isPresent());
        assertFalse(Maps.getAsInt(null, "key").isPresent());
        assertFalse(Maps.getAsInt(new HashMap<>(), "key").isPresent());
    }

    @Test
    public void testGetAsIntOrDefaultIfAbsent() {
        assertEquals(123, Maps.getAsIntOrDefaultIfAbsent(objectMap, "integer", 999));
        assertEquals(999, Maps.getAsIntOrDefaultIfAbsent(objectMap, "missing", 999));
        assertEquals(99, Maps.getAsIntOrDefaultIfAbsent(null, "key", 99));
        assertEquals(99, Maps.getAsIntOrDefaultIfAbsent(new HashMap<>(), "key", 99));
    }

    @Test
    public void testGetAsLong() {
        objectMap.put("longString", "987654321");

        OptionalLong result1 = Maps.getAsLong(objectMap, "long");
        assertTrue(result1.isPresent());
        assertEquals(123456789L, result1.getAsLong());

        OptionalLong result2 = Maps.getAsLong(objectMap, "longString");
        assertTrue(result2.isPresent());
        assertEquals(987654321L, result2.getAsLong());

        assertFalse(Maps.getAsLong(objectMap, "missing").isPresent());
        assertFalse(Maps.getAsLong(null, "key").isPresent());
        assertFalse(Maps.getAsLong(new HashMap<>(), "key").isPresent());
    }

    @Test
    public void testGetAsLongOrDefaultIfAbsent() {
        assertEquals(123456789L, Maps.getAsLongOrDefaultIfAbsent(objectMap, "long", 999L));
        assertEquals(999L, Maps.getAsLongOrDefaultIfAbsent(objectMap, "missing", 999L));
        assertEquals(99999L, Maps.getAsLongOrDefaultIfAbsent(Map.of("key", "99999"), "key", 0L));
        assertEquals(100L, Maps.getAsLongOrDefaultIfAbsent(null, "key", 100L));
        assertEquals(100L, Maps.getAsLongOrDefaultIfAbsent(new HashMap<>(), "key", 100L));
    }

    @Test
    public void testGetAsFloat() {
        objectMap.put("floatString", "56.78");

        OptionalFloat result1 = Maps.getAsFloat(objectMap, "float");
        assertTrue(result1.isPresent());
        assertEquals(12.34f, result1.get(), 0.001f);

        OptionalFloat result2 = Maps.getAsFloat(objectMap, "floatString");
        assertTrue(result2.isPresent());
        assertEquals(56.78f, result2.get(), 0.001f);

        assertFalse(Maps.getAsFloat(objectMap, "missing").isPresent());
        assertFalse(Maps.getAsFloat(null, "key").isPresent());
        assertFalse(Maps.getAsFloat(new HashMap<>(), "key").isPresent());
    }

    @Test
    public void testGetAsFloatOrDefaultIfAbsent() {
        assertEquals(12.34f, Maps.getAsFloatOrDefaultIfAbsent(objectMap, "float", 99.99f), 0.001f);
        assertEquals(99.99f, Maps.getAsFloatOrDefaultIfAbsent(objectMap, "missing", 99.99f), 0.001f);
        assertEquals(1.5f, Maps.getAsFloatOrDefaultIfAbsent(null, "key", 1.5f));
        assertEquals(1.5f, Maps.getAsFloatOrDefaultIfAbsent(new HashMap<>(), "key", 1.5f));
    }

    @Test
    public void testGetAsDouble() {
        OptionalDouble result = Maps.getAsDouble(objectMap, "double");
        assertTrue(result.isPresent());
        assertEquals(45.67, result.getAsDouble(), 0.001);

        objectMap.put("doubleString", "89.12");
        OptionalDouble result2 = Maps.getAsDouble(objectMap, "doubleString");
        assertTrue(result2.isPresent());
        assertEquals(89.12, result2.getAsDouble(), 0.001);

        assertFalse(Maps.getAsDouble(objectMap, "missing").isPresent());
        assertFalse(Maps.getAsDouble(null, "key").isPresent());
        assertFalse(Maps.getAsDouble(new HashMap<>(), "key").isPresent());
    }

    @Test
    public void testGetAsDoubleOrDefaultIfAbsent() {
        assertEquals(45.67, Maps.getAsDoubleOrDefaultIfAbsent(objectMap, "double", 99.99), 0.001);
        assertEquals(99.99, Maps.getAsDoubleOrDefaultIfAbsent(objectMap, "missing", 99.99), 0.001);
        assertEquals(2.5, Maps.getAsDoubleOrDefaultIfAbsent(null, "key", 2.5));
        assertEquals(2.5, Maps.getAsDoubleOrDefaultIfAbsent(new HashMap<>(), "key", 2.5));
    }

    @Test
    public void testGetAsString() {
        Optional<String> result = Maps.getAsString(objectMap, "string");
        assertTrue(result.isPresent());
        assertEquals("test", result.get());

        Optional<String> intAsString = Maps.getAsString(objectMap, "integer");
        assertTrue(intAsString.isPresent());
        assertEquals("123", intAsString.get());

        assertFalse(Maps.getAsString(objectMap, "missing").isPresent());
        assertFalse(Maps.getAsString(null, "key").isPresent());
        assertFalse(Maps.getAsString(new HashMap<>(), "key").isPresent());
    }

    @Test
    public void testGetAsStringOrDefaultIfAbsent() {
        assertEquals("test", Maps.getAsStringOrDefaultIfAbsent(objectMap, "string", "default"));
        assertEquals("default", Maps.getAsStringOrDefaultIfAbsent(objectMap, "missing", "default"));
        assertEquals("123", Maps.getAsStringOrDefaultIfAbsent(Map.of("key", 123), "key", "default"));
        assertEquals("default", Maps.getAsStringOrDefaultIfAbsent(null, "key", "default"));
        assertEquals("default", Maps.getAsStringOrDefaultIfAbsent(new HashMap<>(), "key", "default"));
        assertThrows(IllegalArgumentException.class, () -> Maps.getAsStringOrDefaultIfAbsent(objectMap, "missing", null));
    }

    @Test
    public void testGetNonNullWithClass() {
        Optional<Integer> result = Maps.getAs(objectMap, "integer", Integer.class);
        assertTrue(result.isPresent());
        assertEquals(Integer.valueOf(123), result.get());

        objectMap.put("stringInt", "456");
        Optional<Integer> converted = Maps.getAs(objectMap, "stringInt", Integer.class);
        assertTrue(converted.isPresent());
        assertEquals(Integer.valueOf(456), converted.get());

        assertFalse(Maps.getAs(objectMap, "missing", Integer.class).isPresent());
        assertFalse(Maps.getAs(null, "key", Integer.class).isPresent());
        assertFalse(Maps.getAs(new HashMap<>(), "key", Integer.class).isPresent());
        assertFalse(Maps.getAs(CommonUtil.asMap("val", null), "val", Integer.class).isPresent());
        assertEquals(Optional.of(123), Maps.getAs(Map.of("val", "123"), "val", Integer.class));
    }

    @Test
    public void testGetNonNullWithType() {
        com.landawn.abacus.type.Type<Integer> intType = com.landawn.abacus.type.Type.of(Integer.class);
        Optional<Integer> result = Maps.getAs(objectMap, "integer", intType);
        assertTrue(result.isPresent());
        assertEquals(Integer.valueOf(123), result.get());
        assertFalse(Maps.getAs(new HashMap<>(), "key", com.landawn.abacus.type.TypeFactory.getType(String.class)).isPresent());
    }

    @Test
    public void testGetNonNull_class() {
        Map<String, Object> map = new HashMap<>();
        map.put("s", "text");
        map.put("i", 123);
        map.put("d", 123.45);
        map.put("n", null);

        assertEquals(Optional.of("text"), Maps.getAs(map, "s", String.class));
        assertEquals(Optional.of(123), Maps.getAs(map, "i", Integer.class));
        assertEquals(Optional.of(123.45), Maps.getAs(map, "d", Double.class));
        assertEquals(Optional.of("123"), Maps.getAs(map, "i", String.class));
        assertEquals(Optional.empty(), Maps.getAs(map, "n", String.class));
        assertEquals(Optional.empty(), Maps.getAs(map, "missing", String.class));
    }

    @Test
    public void testGetNonNullWithDefault() {
        Integer result = Maps.getAsOrDefaultIfAbsent(objectMap, "integer", 999, Integer.class);
        assertEquals(Integer.valueOf(123), result);

        Integer defaultResult = Maps.getAsOrDefaultIfAbsent(objectMap, "missing", 999, Integer.class);
        assertEquals(Integer.valueOf(999), defaultResult);

        assertEquals(Integer.valueOf(123), Maps.getAsOrDefaultIfAbsent(Map.of("val", "123"), "val", 0, Integer.class));
        assertEquals(Integer.valueOf(99), Maps.getAsOrDefaultIfAbsent(null, "key", 99, Integer.class));

        assertThrows(IllegalArgumentException.class, () -> Maps.getAsOrDefaultIfAbsent(objectMap, "missing", (Integer) null, Integer.class));
        assertThrows(IllegalArgumentException.class, () -> Maps.getAsOrDefaultIfAbsent(objectMap, "missing", 999, (Class<Integer>) null));
    }

    @Test
    public void testGetNonNull_default() {
        Map<String, Object> map = new HashMap<>();
        map.put("s", "text");
        map.put("i", 123);
        map.put("n", null);
        String defaultStr = "default";
        Integer defaultInt = 999;

        assertEquals("text", Maps.getAsOrDefaultIfAbsent(map, "s", defaultStr, String.class));
        assertEquals(Integer.valueOf(123), Maps.getAsOrDefaultIfAbsent(map, "i", defaultInt, Integer.class));
        assertEquals("123", Maps.getAsOrDefaultIfAbsent(map, "i", defaultStr, String.class));
        assertEquals(defaultStr, Maps.getAsOrDefaultIfAbsent(map, "n", defaultStr, String.class));
        assertEquals(defaultStr, Maps.getAsOrDefaultIfAbsent(map, "missing", defaultStr, String.class));
        assertThrows(IllegalArgumentException.class, () -> Maps.getAsOrDefaultIfAbsent(map, "s", (String) null, String.class));
        assertThrows(IllegalArgumentException.class, () -> Maps.getAsOrDefaultIfAbsent(map, "s", defaultStr, (Class<String>) null));
    }

    @Test
    public void testGetIfPresentForEach() {
        List<String> keys = Arrays.asList("key1", "missing", "key2", "key3");
        List<String> values = Maps.getValuesIfPresent(testMap, keys);
        assertEquals(3, values.size());
        assertEquals(Arrays.asList("value1", "value2", "value3"), values);

        assertTrue(Maps.getValuesIfPresent(null, keys).isEmpty());
        assertTrue(Maps.getValuesIfPresent(testMap, null).isEmpty());
        assertTrue(Maps.getValuesIfPresent(testMap, new ArrayList<>()).isEmpty());
    }

    @Test
    public void testGetValuesOrDefaultIfAbsent() {
        assertTrue(Maps.getValuesOrDefaultIfAbsent(testMap, new ArrayList<>(), "default").isEmpty());

        List<String> fromNull = Maps.getValuesOrDefaultIfAbsent(null, Arrays.asList("a", "b"), "default");
        assertEquals(Arrays.asList("default", "default"), fromNull);
    }

    @Test
    public void testGetByPathEmptyPathTreatsPresentNullAsExisting() {
        // A present null under the "" key is still observable through IfExists, while
        // getByPathAsOrDefaultIfAbsent applies null-as-absent default semantics.
        final Map<String, Object> map = new HashMap<>();
        map.put("", null);

        assertTrue(Maps.getByPathIfExists(map, "").isPresent());
        assertEquals("dflt", Maps.getByPathAsOrDefaultIfAbsent(map, "", "dflt", String.class));

        map.put("", "5");
        assertEquals(Integer.valueOf(5), Maps.getByPathAsOrDefaultIfAbsent(map, "", 0, Integer.class));
    }

    @Test
    public void testGetAs_nullConversionResult_isEmptyNotNpe() {
        final Map<String, Object> map = CommonUtil.asMap("blank", "");

        assertFalse(Maps.getAs(map, "blank", Integer.class).isPresent());
        assertFalse(Maps.getAs(map, "blank", Long.class).isPresent());
        assertFalse(Maps.getAs(map, "blank", Double.class).isPresent());
        assertFalse(Maps.getAs(map, "blank", java.time.LocalDate.class).isPresent());
    }

    @Test
    public void testGetAs_withType_nullConversionResult_isEmptyNotNpe() {
        final Map<String, Object> map = CommonUtil.asMap("blank", "");

        assertFalse(Maps.getAs(map, "blank", com.landawn.abacus.type.TypeFactory.getType(Integer.class)).isPresent());
    }

    @Test
    public void testGetAsOrDefaultIfAbsent_nullConversionResult_returnsDefault() {
        final Map<String, Object> map = CommonUtil.asMap("blank", "");

        // must never leak the null that N.convert answers - this method guarantees a non-null result
        assertEquals(Integer.valueOf(5), Maps.getAsOrDefaultIfAbsent(map, "blank", 5, Integer.class));
        assertEquals(Long.valueOf(7L), Maps.getAsOrDefaultIfAbsent(map, "blank", 7L, Long.class));
    }

    @Test
    public void testGetAs_convertibleValuesAreUnaffected() {
        final Map<String, Object> map = CommonUtil.asMap("num", "25", "text", "");

        assertEquals(Optional.of(25), Maps.getAs(map, "num", Integer.class));
        assertEquals(Integer.valueOf(25), Maps.getAsOrDefaultIfAbsent(map, "num", 0, Integer.class));
        // "" converts to itself for String, so it stays present
        assertEquals(Optional.of(""), Maps.getAs(map, "text", String.class));
        assertEquals("", Maps.getAsOrDefaultIfAbsent(map, "text", "fallback", String.class));
    }

    @Test
    public void testGetAsChar_numberIsACodeUnit_notItsDigits() {
        final Map<String, Object> map = CommonUtil.asMap("five", 5, "nine", 9, "ten", 10, "big", 65);

        // The old string-form conversion branched on string length, so 5 became the glyph '5' (U+0035)
        // while 10 and 65 became code units U+000A and 'A'. Every Number is a code unit now.
        assertEquals((char) 5, Maps.getAsChar(map, "five").get());
        assertEquals((char) 9, Maps.getAsChar(map, "nine").get());
        assertEquals((char) 10, Maps.getAsChar(map, "ten").get());
        assertEquals('A', Maps.getAsChar(map, "big").get());

        assertEquals((char) 5, Maps.getAsCharOrDefaultIfAbsent(map, "five", 'Z'));
        assertEquals('A', Maps.getAsCharOrDefaultIfAbsent(map, "big", 'Z'));
    }

    @Test
    public void testGetAsChar_numberSubtypesAndFractions() {
        final Map<String, Object> map = CommonUtil.asMap("lng", 66L, "dbl", 65.0d, "frac", 65.9d, "flt", 67.0f, "big", java.math.BigInteger.valueOf(68), "dec",
                new BigDecimal("69.9"));

        assertEquals('B', Maps.getAsChar(map, "lng").get());
        assertEquals('A', Maps.getAsChar(map, "dbl").get());
        // fractional values truncate toward zero, matching getAsInt and the other numeric accessors
        assertEquals('A', Maps.getAsChar(map, "frac").get());
        assertEquals('C', Maps.getAsChar(map, "flt").get());
        assertEquals('D', Maps.getAsChar(map, "big").get());
        assertEquals('E', Maps.getAsChar(map, "dec").get());
    }

    @Test
    public void testGetAsChar_numberOutOfCharRange() {
        final Map<String, Object> map = CommonUtil.asMap("neg", -1, "huge", 70000, "vast", Long.MAX_VALUE);

        assertThrows(IllegalArgumentException.class, () -> Maps.getAsChar(map, "neg"));
        assertThrows(IllegalArgumentException.class, () -> Maps.getAsChar(map, "huge"));
        assertThrows(IllegalArgumentException.class, () -> Maps.getAsChar(map, "vast"));
        assertThrows(IllegalArgumentException.class, () -> Maps.getAsCharOrDefaultIfAbsent(map, "neg", 'Z'));
    }

    @Test
    public void testGetAsChar_stringFormIsUnchanged() {
        final Map<String, Object> map = CommonUtil.asMap("code", "65", "one", "X", "empty", "", "junk", "AB", "neg", "-1");

        assertEquals('A', Maps.getAsChar(map, "code").get());
        assertEquals('X', Maps.getAsChar(map, "one").get());
        // deliberate exception to the "empty text is absent" rule, and long-standing behaviour
        assertEquals('\0', Maps.getAsChar(map, "empty").get());
        assertThrows(NumberFormatException.class, () -> Maps.getAsChar(map, "junk"));
        assertThrows(IllegalArgumentException.class, () -> Maps.getAsChar(map, "neg"));
    }

    @Test
    public void testGetAsNumeric_emptyStringValueIsAbsent() {
        final Map<String, Object> map = CommonUtil.asMap("blank", "");

        assertFalse(Maps.getAsByte(map, "blank").isPresent());
        assertFalse(Maps.getAsShort(map, "blank").isPresent());
        assertFalse(Maps.getAsInt(map, "blank").isPresent());
        assertFalse(Maps.getAsLong(map, "blank").isPresent());
        assertFalse(Maps.getAsFloat(map, "blank").isPresent());
        assertFalse(Maps.getAsDouble(map, "blank").isPresent());
    }

    @Test
    public void testGetAsNumericOrDefaultIfAbsent_emptyStringValueYieldsDefault() {
        final Map<String, Object> map = CommonUtil.asMap("blank", "");

        assertEquals((byte) -1, Maps.getAsByteOrDefaultIfAbsent(map, "blank", (byte) -1));
        assertEquals((short) -1, Maps.getAsShortOrDefaultIfAbsent(map, "blank", (short) -1));
        assertEquals(-1, Maps.getAsIntOrDefaultIfAbsent(map, "blank", -1));
        assertEquals(-1L, Maps.getAsLongOrDefaultIfAbsent(map, "blank", -1L));
        assertEquals(-1f, Maps.getAsFloatOrDefaultIfAbsent(map, "blank", -1f));
        assertEquals(-1d, Maps.getAsDoubleOrDefaultIfAbsent(map, "blank", -1d));
    }

    @Test
    public void testGetAsNumeric_anyEmptyCharSequenceIsAbsent() {
        final Map<String, Object> map = CommonUtil.asMap("sb", new StringBuilder());

        assertFalse(Maps.getAsInt(map, "sb").isPresent());
        assertEquals(-1, Maps.getAsIntOrDefaultIfAbsent(map, "sb", -1));
    }

    @Test
    public void testGetAsNumeric_blankButNonEmptyIsStillMalformed() {
        final Map<String, Object> map = CommonUtil.asMap("space", " ");

        // " " is malformed, not absent - it must still fail loudly
        assertThrows(NumberFormatException.class, () -> Maps.getAsInt(map, "space"));
        assertThrows(NumberFormatException.class, () -> Maps.getAsIntOrDefaultIfAbsent(map, "space", -1));
        assertThrows(NumberFormatException.class, () -> Maps.getAsDouble(map, "space"));
    }

    @Test
    public void testGetAsNumeric_realZeroIsStillReported() {
        final Map<String, Object> map = CommonUtil.asMap("num", 0, "text", "0");

        assertEquals(OptionalInt.of(0), Maps.getAsInt(map, "num"));
        assertEquals(OptionalInt.of(0), Maps.getAsInt(map, "text"));
        assertEquals(0, Maps.getAsIntOrDefaultIfAbsent(map, "num", -1));
        assertEquals(0, Maps.getAsIntOrDefaultIfAbsent(map, "text", -1));
    }

    @Test
    public void testGetByPathAsInt_emptyStringValueIsAbsent() {
        final Map<String, Object> map = CommonUtil.asMap("user", CommonUtil.asMap("age", "25", "score", ""));

        assertEquals(OptionalInt.of(25), Maps.getByPathAsInt(map, "user.age"));
        assertFalse(Maps.getByPathAsInt(map, "user.score").isPresent());
        assertEquals(-1, Maps.getByPathAsIntOrDefaultIfAbsent(map, "user.score", -1));
        assertEquals(25, Maps.getByPathAsIntOrDefaultIfAbsent(map, "user.age", -1));
        assertFalse(Maps.getByPathAs(map, "user.score", Integer.class).isPresent());
    }

    @Test
    public void testGetByPath_emptyIndexSegmentIsUnresolvable() {
        final Map<String, Object> map = new LinkedHashMap<>();
        map.put("a", Arrays.asList(CommonUtil.asMap("b", "FIRST"), CommonUtil.asMap("b", "SECOND")));

        assertNull(Maps.getByPath(map, "a[]"));
        assertNull(Maps.getByPath(map, "a[].b"));
        assertNull(Maps.getByPath(map, "a[ ]"));
        assertNull(Maps.getByPath(map, "a[][0]"));
        assertFalse(Maps.getByPathIfExists(map, "a[]").isPresent());
        assertFalse(Maps.getByPathIfExists(map, "a[].b").isPresent());
        assertEquals("dflt", Maps.getByPathAsOrDefaultIfAbsent(map, "a[].b", "dflt", String.class));
    }

    @Test
    public void testGetByPath_wellFormedIndexesStillResolve() {
        final Map<String, Object> map = new LinkedHashMap<>();
        map.put("a", Arrays.asList(CommonUtil.asMap("b", "FIRST"), CommonUtil.asMap("b", "SECOND")));
        map.put("nested", Arrays.asList(Arrays.asList("x", "y")));

        assertEquals("FIRST", Maps.getByPath(map, "a[0].b"));
        assertEquals("SECOND", Maps.getByPath(map, "a[1].b"));
        assertEquals("y", Maps.getByPath(map, "nested[0][1]"));
        assertNull(Maps.getByPath(map, "a[9].b"));
        assertNull(Maps.getByPath(map, "a[-1].b"));
    }

    @Test
    public void testGetByPath_nullValueIsStillDistinctFromMissingPath() {
        final Map<String, Object> map = new LinkedHashMap<>();
        map.put("user", CommonUtil.asMap("age", null));

        assertTrue(Maps.getByPathIfExists(map, "user.age").isPresent());
        assertNull(Maps.getByPathIfExists(map, "user.age").orElseNull());
        assertFalse(Maps.getByPathIfExists(map, "user.missing").isPresent());
        assertNull(Maps.getByPath(map, "user.age"));
        // null-as-absent callers still collapse the two
        assertEquals(Integer.valueOf(7), Maps.getByPathAsOrDefaultIfAbsent(map, "user.age", 7, Integer.class));
        assertEquals(Integer.valueOf(7), Maps.getByPathAsOrDefaultIfAbsent(map, "user.missing", 7, Integer.class));
    }

    @Test
    public void testGetByPathAsOrDefaultIfAbsent_defaultInstanceAlsoStoredInMap() {
        final String shared = new String("shared");
        final Map<String, Object> map = CommonUtil.asMap("k", shared);

        // a found value that happens to be the very default instance must be reported as found
        assertSame(shared, Maps.getByPathAsOrDefaultIfAbsent(map, "k", shared, String.class));
        assertSame(shared, Maps.getByPathAsOrDefaultIfAbsent(map, "missing", shared, String.class));
    }

    @Test
    public void testGetValuesOrDefaultIfAbsent_acceptsASupertypeDefault() {
        final Map<String, Integer> map = CommonUtil.asMap("a", 1);

        // the invariant value type used to reject a Number default for a Map<String, Integer>
        final List<Number> values = Maps.getValuesOrDefaultIfAbsent(map, CommonUtil.asList("a", "missing"), (Number) 0);

        assertEquals(2, values.size());
        assertEquals(Integer.valueOf(1), values.get(0));
        assertEquals(Integer.valueOf(0), values.get(1));
    }

    /**
     * Pins the documented rule for a {@code Character} value: it is read as a one-character string, so
     * {@code 'Y'}, {@code 'y'} and {@code '1'} are {@code true}. The javadoc used to state the opposite
     * ("always false - 'Y', 'y' and '1' included").
     */
    @Test
    public void testGetAsBoolean_CharacterValueFollowsTheStringRule() {
        Map<String, Object> map = new HashMap<>();
        map.put("upperY", 'Y');
        map.put("lowerY", 'y');
        map.put("one", '1');
        map.put("n", 'N');
        map.put("zero", '0');
        map.put("t", 'T');

        assertTrue(Maps.getAsBoolean(map, "upperY").get());
        assertTrue(Maps.getAsBoolean(map, "lowerY").get());
        assertTrue(Maps.getAsBoolean(map, "one").get());
        assertFalse(Maps.getAsBoolean(map, "n").get());
        assertFalse(Maps.getAsBoolean(map, "zero").get());
        assertFalse(Maps.getAsBoolean(map, "t").get());

        // identical to the one-character String form, which is the whole point of the rule
        Map<String, Object> strings = new HashMap<>();
        strings.put("upperY", "Y");
        strings.put("lowerY", "y");
        strings.put("one", "1");
        strings.put("t", "T");

        assertTrue(Maps.getAsBoolean(strings, "upperY").get());
        assertTrue(Maps.getAsBoolean(strings, "lowerY").get());
        assertTrue(Maps.getAsBoolean(strings, "one").get());
        assertFalse(Maps.getAsBoolean(strings, "t").get());
    }

    /**
     * Pins the documented padding rule for a string value: leading and trailing padding (Unicode whitespace, or
     * any character {@code <= ' '}) is stripped before the {@code "true"}/{@code "Y"}/{@code "y"}/{@code "1"}
     * comparison, while interior padding is not stripped.
     */
    @Test
    public void testGetAsBoolean_PaddedStringIsStrippedBeforeTheComparison() {
        Map<String, Object> trues = new HashMap<>();
        trues.put("spacedTrue", " true ");
        trues.put("leadingY", " Y");
        trues.put("trailingY", "Y ");
        trues.put("tabbedOne", "\t1");
        trues.put("controlY", "\u0001Y"); // <= ' ' but not Unicode whitespace
        trues.put("ideographicTrue", "\u3000true"); // Unicode whitespace but > ' '

        for (String key : new String[] { "spacedTrue", "leadingY", "trailingY", "tabbedOne", "controlY", "ideographicTrue" }) {
            assertTrue(Maps.getAsBoolean(trues, key).get(), "should be true: " + trues.get(key));
        }

        Map<String, Object> falses = new HashMap<>();
        falses.put("interior", "tr ue"); // interior padding is not stripped
        falses.put("paddedT", " T ");
        falses.put("paddedYes", " yes ");
        falses.put("onlyPadding", "   ");
        falses.put("empty", "");

        for (String key : new String[] { "interior", "paddedT", "paddedYes", "onlyPadding", "empty" }) {
            assertFalse(Maps.getAsBoolean(falses, key).get(), "should be false: " + falses.get(key));
        }
    }
}

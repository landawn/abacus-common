package com.landawn.abacus.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.type.Type;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

public class JSONUtil100Test extends TestBase {

    @Test
    public void testWrapMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("name", "John");
        map.put("age", 30);
        map.put("active", true);

        JSONObject json = JSONUtil.wrap(map);

        Assertions.assertNotNull(json);
        Assertions.assertEquals("John", json.getString("name"));
        Assertions.assertEquals(30, json.getInt("age"));
        Assertions.assertTrue(json.getBoolean("active"));
    }

    @Test
    public void testWrapMapWithNull() {
        Map<String, Object> map = new HashMap<>();
        map.put("nullValue", null);

        JSONObject json = JSONUtil.wrap(map);

        Assertions.assertNotNull(json);
        Assertions.assertFalse(json.has("nullValue"));
        // Assertions.assertTrue(json.isNull("nullValue"));
    }

    @Test
    public void testWrapObject() {
        TestBean bean = new TestBean("Alice", 25);

        JSONObject json = JSONUtil.wrap(bean);

        Assertions.assertNotNull(json);
        Assertions.assertEquals("Alice", json.getString("name"));
        Assertions.assertEquals(25, json.getInt("age"));
    }

    @Test
    public void testWrapObjectAsMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("id", 123);

        JSONObject json = JSONUtil.wrap((Object) map);

        Assertions.assertNotNull(json);
        Assertions.assertEquals(123, json.getInt("id"));
    }

    @Test
    public void testWrapBooleanArray() {
        boolean[] array = { true, false, true, true, false };

        JSONArray json = JSONUtil.wrap(array);

        Assertions.assertNotNull(json);
        Assertions.assertEquals(5, json.length());
        Assertions.assertTrue(json.getBoolean(0));
        Assertions.assertFalse(json.getBoolean(1));
        Assertions.assertTrue(json.getBoolean(2));
    }

    @Test
    public void testWrapCharArray() {
        char[] array = { 'H', 'e', 'l', 'l', 'o' };

        JSONArray json = JSONUtil.wrap(array);

        Assertions.assertNotNull(json);
        Assertions.assertEquals(5, json.length());
        Assertions.assertEquals('H', json.get(0));
        Assertions.assertEquals('e', json.get(1));
        Assertions.assertEquals('o', json.get(4));
    }

    @Test
    public void testWrapByteArray() {
        byte[] array = { 10, 20, 30, 40, 50 };

        JSONArray json = JSONUtil.wrap(array);

        Assertions.assertNotNull(json);
        Assertions.assertEquals(5, json.length());
        Assertions.assertEquals(10, json.getInt(0));
        Assertions.assertEquals(50, json.getInt(4));
    }

    @Test
    public void testWrapShortArray() {
        short[] array = { 100, 200, 300, 400, 500 };

        JSONArray json = JSONUtil.wrap(array);

        Assertions.assertNotNull(json);
        Assertions.assertEquals(5, json.length());
        Assertions.assertEquals(100, json.getInt(0));
        Assertions.assertEquals(500, json.getInt(4));
    }

    @Test
    public void testWrapIntArray() {
        int[] array = { 1, 2, 3, 4, 5 };

        JSONArray json = JSONUtil.wrap(array);

        Assertions.assertNotNull(json);
        Assertions.assertEquals(5, json.length());
        Assertions.assertEquals(1, json.getInt(0));
        Assertions.assertEquals(5, json.getInt(4));
    }

    @Test
    public void testWrapLongArray() {
        long[] array = { 1609459200000L, 1609545600000L, 1609632000000L };

        JSONArray json = JSONUtil.wrap(array);

        Assertions.assertNotNull(json);
        Assertions.assertEquals(3, json.length());
        Assertions.assertEquals(1609459200000L, json.getLong(0));
    }

    @Test
    public void testWrapFloatArray() {
        float[] array = { 98.6f, 99.1f, 97.8f, 98.2f };

        JSONArray json = JSONUtil.wrap(array);

        Assertions.assertNotNull(json);
        Assertions.assertEquals(4, json.length());
        Assertions.assertEquals(98.6f, json.getFloat(0), 0.001f);
    }

    @Test
    public void testWrapDoubleArray() {
        double[] array = { 19.99, 29.99, 39.99, 49.99 };

        JSONArray json = JSONUtil.wrap(array);

        Assertions.assertNotNull(json);
        Assertions.assertEquals(4, json.length());
        Assertions.assertEquals(19.99, json.getDouble(0), 0.001);
    }

    @Test
    public void testWrapObjectArray() {
        Object[] array = { "text", 123, true, null, Arrays.asList(1, 2, 3) };

        JSONArray json = JSONUtil.wrap(array);

        Assertions.assertNotNull(json);
        Assertions.assertEquals(5, json.length());
        Assertions.assertEquals("text", json.getString(0));
        Assertions.assertEquals(123, json.getInt(1));
        Assertions.assertTrue(json.getBoolean(2));
        Assertions.assertTrue(json.isNull(3));
        Assertions.assertTrue(json.get(4) instanceof JSONArray);
    }

    @Test
    public void testWrapCollection() {
        List<String> names = Arrays.asList("Alice", "Bob", "Charlie");

        JSONArray json = JSONUtil.wrap(names);

        Assertions.assertNotNull(json);
        Assertions.assertEquals(3, json.length());
        Assertions.assertEquals("Alice", json.getString(0));
        Assertions.assertEquals("Bob", json.getString(1));
        Assertions.assertEquals("Charlie", json.getString(2));
    }

    @Test
    public void testWrapSet() {
        Set<Integer> numbers = new LinkedHashSet<>(Arrays.asList(1, 2, 3));

        JSONArray json = JSONUtil.wrap(numbers);

        Assertions.assertNotNull(json);
        Assertions.assertEquals(3, json.length());
    }

    @Test
    public void testUnwrapJSONObject() {
        JSONObject json = new JSONObject();
        json.put("name", "John");
        json.put("age", 30);
        json.put("active", true);

        Map<String, Object> map = JSONUtil.unwrap(json);

        Assertions.assertNotNull(map);
        Assertions.assertEquals("John", map.get("name"));
        Assertions.assertEquals(30, map.get("age"));
        Assertions.assertEquals(true, map.get("active"));
    }

    @Test
    public void testUnwrapJSONObjectToMap() {
        JSONObject json = new JSONObject();
        json.put("z", 1);
        json.put("a", 2);

        TreeMap<String, Object> sorted = JSONUtil.unwrap(json, TreeMap.class);

        Assertions.assertNotNull(sorted);
        Assertions.assertEquals(2, sorted.size());
        List<String> keys = new ArrayList<>(sorted.keySet());
        Assertions.assertEquals("a", keys.get(0));
        Assertions.assertEquals("z", keys.get(1));
    }

    @Test
    public void testUnwrapJSONObjectToBean() {
        JSONObject json = new JSONObject();
        json.put("name", "Alice");
        json.put("age", 25);

        TestBean bean = JSONUtil.unwrap(json, TestBean.class);

        Assertions.assertNotNull(bean);
        Assertions.assertEquals("Alice", bean.getName());
        Assertions.assertEquals(25, bean.getAge());
    }

    @Test
    public void testUnwrapJSONObjectWithType() {
        JSONObject json = new JSONObject();
        json.put("key1", "value1");
        json.put("key2", Arrays.asList("a", "b", "c"));

        Type<Map<String, Object>> type = N.typeOf("Map<String, Object>");
        Map<String, Object> result = JSONUtil.unwrap(json, type);

        Assertions.assertNotNull(result);
        Assertions.assertEquals("value1", result.get("key1"));
        Assertions.assertTrue(result.get("key2") instanceof List);
    }

    @Test
    public void testUnwrapJSONObjectWithNull() {
        JSONObject json = new JSONObject();
        json.put("nullKey", JSONObject.NULL);

        Map<String, Object> map = JSONUtil.unwrap(json);

        Assertions.assertNotNull(map);
        Assertions.assertNull(map.get("nullKey"));
    }

    @Test
    public void testUnwrapJSONArray() {
        JSONArray json = new JSONArray();
        json.put("text");
        json.put(123);
        json.put(true);
        json.put(JSONObject.NULL);

        List<Object> list = JSONUtil.unwrap(json);

        Assertions.assertNotNull(list);
        Assertions.assertEquals(4, list.size());
        Assertions.assertEquals("text", list.get(0));
        Assertions.assertEquals(123, list.get(1));
        Assertions.assertEquals(true, list.get(2));
        Assertions.assertNull(list.get(3));
    }

    @Test
    public void testUnwrapJSONArrayToList() {
        JSONArray json = new JSONArray();
        json.put("Alice");
        json.put("Bob");
        json.put("Charlie");

        List<String> names = JSONUtil.unwrap(json, List.class);

        Assertions.assertNotNull(names);
        Assertions.assertEquals(3, names.size());
        Assertions.assertEquals("Alice", names.get(0));
    }

    @Test
    public void testUnwrapJSONArrayToSet() {
        JSONArray json = new JSONArray();
        json.put(1);
        json.put(2);
        json.put(3);
        json.put(2);
        json.put(1);

        Set<Integer> uniqueNumbers = JSONUtil.unwrap(json, Set.class);

        Assertions.assertNotNull(uniqueNumbers);
        Assertions.assertEquals(3, uniqueNumbers.size());
    }

    @Test
    public void testUnwrapJSONArrayToPrimitiveArray() {
        JSONArray json = new JSONArray();
        json.put(85);
        json.put(90);
        json.put(78);
        json.put(92);
        json.put(88);

        int[] scores = JSONUtil.unwrap(json, int[].class);

        Assertions.assertNotNull(scores);
        Assertions.assertEquals(5, scores.length);
        Assertions.assertEquals(85, scores[0]);
        Assertions.assertEquals(88, scores[4]);
    }

    @Test
    public void testUnwrapJSONArrayToObjectArray() {
        JSONArray json = new JSONArray();
        json.put("Alice");
        json.put("Bob");
        json.put("Charlie");

        String[] names = JSONUtil.unwrap(json, String[].class);

        Assertions.assertNotNull(names);
        Assertions.assertEquals(3, names.length);
        Assertions.assertEquals("Alice", names[0]);
    }

    @Test
    public void testUnwrapJSONArrayWithType() {
        JSONArray json = new JSONArray();
        JSONObject obj1 = new JSONObject();
        obj1.put("name", "Alice");
        obj1.put("age", 25);
        JSONObject obj2 = new JSONObject();
        obj2.put("name", "Bob");
        obj2.put("age", 30);
        json.put(obj1);
        json.put(obj2);

        Type<List<TestBean>> type = N.typeOf("List<com.landawn.abacus.util.JSONUtil100Test.TestBean>");
        List<TestBean> users = JSONUtil.unwrap(json, type);

        Assertions.assertNotNull(users);
        Assertions.assertEquals(2, users.size());
        Assertions.assertEquals("Alice", users.get(0).getName());
        Assertions.assertEquals(30, users.get(1).getAge());
    }

    @Test
    public void testUnwrapJSONArrayWithNullElements() {
        JSONArray json = new JSONArray();
        json.put(1);
        json.put(JSONObject.NULL);
        json.put(3);

        int[] array = JSONUtil.unwrap(json, int[].class);

        Assertions.assertNotNull(array);
        Assertions.assertEquals(3, array.length);
        Assertions.assertEquals(1, array[0]);
        Assertions.assertEquals(0, array[1]); // default value for null
        Assertions.assertEquals(3, array[2]);
    }

    @Test
    public void testToListWithClass() {
        JSONArray json = new JSONArray();
        json.put("apple");
        json.put("banana");
        json.put("orange");

        List<String> fruits = JSONUtil.toList(json, String.class);

        Assertions.assertNotNull(fruits);
        Assertions.assertEquals(3, fruits.size());
        Assertions.assertEquals("apple", fruits.get(0));
        Assertions.assertEquals("banana", fruits.get(1));
        Assertions.assertEquals("orange", fruits.get(2));
    }

    @Test
    public void testToListWithType() {
        JSONArray json = new JSONArray();
        JSONObject map1 = new JSONObject();
        map1.put("id", 1);
        map1.put("name", "Item1");
        JSONObject map2 = new JSONObject();
        map2.put("id", 2);
        map2.put("name", "Item2");
        json.put(map1);
        json.put(map2);

        Type<Map<String, Object>> mapType = N.typeOf("Map<String, Object>");
        List<Map<String, Object>> items = JSONUtil.toList(json, mapType);

        Assertions.assertNotNull(items);
        Assertions.assertEquals(2, items.size());
        Assertions.assertEquals(1, items.get(0).get("id"));
        Assertions.assertEquals("Item2", items.get(1).get("name"));
    }

    @Test
    public void testNestedStructures() {
        // Test nested JSON object
        JSONObject outer = new JSONObject();
        JSONObject inner = new JSONObject();
        inner.put("innerKey", "innerValue");
        outer.put("nested", inner);
        outer.put("simple", "value");

        Map<String, Object> result = JSONUtil.unwrap(outer);

        Assertions.assertNotNull(result);
        Assertions.assertEquals("value", result.get("simple"));
        Assertions.assertTrue(result.get("nested") instanceof Map);
        Map<String, Object> nestedMap = (Map<String, Object>) result.get("nested");
        Assertions.assertEquals("innerValue", nestedMap.get("innerKey"));
    }

    @Test
    public void testComplexNestedArray() {
        // Test nested arrays
        JSONArray outer = new JSONArray();
        JSONArray inner1 = new JSONArray();
        inner1.put(1);
        inner1.put(2);
        inner1.put(3);
        JSONArray inner2 = new JSONArray();
        inner2.put(4);
        inner2.put(5);
        inner2.put(6);
        outer.put(inner1);
        outer.put(inner2);

        Type<List<List<Integer>>> type = N.typeOf("List<List<Integer>>");
        List<List<Integer>> result = JSONUtil.unwrap(outer, type);

        Assertions.assertNotNull(result);
        Assertions.assertEquals(2, result.size());
        Assertions.assertEquals(3, result.get(0).size());
        Assertions.assertEquals(1, result.get(0).get(0));
        Assertions.assertEquals(6, result.get(1).get(2));
    }

    @Test
    public void testExceptionHandling() {
        // Test invalid type for unwrap
        JSONObject json = new JSONObject();
        json.put("key", "value");

        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            JSONUtil.unwrap(json, String.class);
        });

        // Test invalid type for array unwrap
        JSONArray array = new JSONArray();
        array.put(1);

        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            JSONUtil.unwrap(array, String.class);
        });
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TestBean {
        private String name;
        private int age;

    }
}
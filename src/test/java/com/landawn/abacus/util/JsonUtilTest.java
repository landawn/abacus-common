package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
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

public class JsonUtilTest extends TestBase {

    public static class SimpleBean {
        private String name;
        private int age;
        private boolean active;

        public SimpleBean() {
        }

        public SimpleBean(String name, int age, boolean active) {
            this.name = name;
            this.age = age;
            this.active = active;
        }

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
    }

    public static class TestBean {
        private String name;
        private int age;

        public TestBean() {
        }

        public TestBean(String name, int age) {
            this.name = name;
            this.age = age;
        }

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
    }

    public static class NestedBean {
        private String id;
        private SimpleBean simpleBean;
        private List<String> tags;

        public NestedBean() {
        }

        public NestedBean(String id, SimpleBean simpleBean, List<String> tags) {
            this.id = id;
            this.simpleBean = simpleBean;
            this.tags = tags;
        }

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public SimpleBean getSimpleBean() {
            return simpleBean;
        }

        public void setSimpleBean(SimpleBean simpleBean) {
            this.simpleBean = simpleBean;
        }

        public List<String> getTags() {
            return tags;
        }

        public void setTags(List<String> tags) {
            this.tags = tags;
        }
    }

    @Test
    public void testWrapAndUnwrapRoundTrip() {
        SimpleBean original = new SimpleBean("Liam", 27, true);
        JSONObject json = JsonUtil.wrap(original);
        SimpleBean restored = JsonUtil.unwrap(json, SimpleBean.class);

        assertEquals(original.getName(), restored.getName());
        assertEquals(original.getAge(), restored.getAge());
        assertEquals(original.isActive(), restored.isActive());
    }

    @Test
    public void testWrapCollectionAndUnwrapRoundTrip() {
        List<Integer> original = Arrays.asList(5, 10, 15, 20);
        JSONArray json = JsonUtil.wrap(original);
        List<Integer> restored = JsonUtil.toList(json, Integer.class);

        assertEquals(original.size(), restored.size());
        for (int i = 0; i < original.size(); i++) {
            assertEquals(original.get(i), restored.get(i));
        }
    }

    @Test
    public void testWrapMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("name", "Alice");
        map.put("age", 30);
        map.put("active", true);

        JSONObject json = JsonUtil.wrap(map);

        assertNotNull(json);
        assertEquals("Alice", json.getString("name"));
        assertEquals(30, json.getInt("age"));
        assertEquals(true, json.getBoolean("active"));
    }

    @Test
    public void testWrapEmptyMap() {
        Map<String, Object> map = new HashMap<>();
        JSONObject json = JsonUtil.wrap(map);

        assertNotNull(json);
        assertEquals(0, json.length());
    }

    @Test
    public void testWrapMapWithNullValues() {
        Map<String, Object> map = new HashMap<>();
        map.put("key1", "value1");
        map.put("key2", null);

        JSONObject json = JsonUtil.wrap(map);

        assertNotNull(json);
        assertEquals("value1", json.getString("key1"));
        assertTrue(json.isNull("key2"));
    }

    @Test
    public void testWrapMapWithNull() {
        Map<String, Object> map = new HashMap<>();
        map.put("nullValue", null);

        JSONObject json = JsonUtil.wrap(map);

        Assertions.assertNotNull(json);
        Assertions.assertFalse(json.has("nullValue"));
    }

    @Test
    public void testWrapBean() {
        SimpleBean bean = new SimpleBean("Bob", 25, true);
        JSONObject json = JsonUtil.wrap(bean);

        assertNotNull(json);
        assertEquals("Bob", json.getString("name"));
        assertEquals(25, json.getInt("age"));
        assertEquals(true, json.getBoolean("active"));
    }

    @Test
    public void testWrapObjectWithMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("key", "value");

        JSONObject json = JsonUtil.wrap((Object) map);

        assertNotNull(json);
        assertEquals("value", json.getString("key"));
    }

    @Test
    public void testWrapObject() {
        TestBean bean = new TestBean("Alice", 25);

        JSONObject json = JsonUtil.wrap(bean);

        Assertions.assertNotNull(json);
        Assertions.assertEquals("Alice", json.getString("name"));
        Assertions.assertEquals(25, json.getInt("age"));
    }

    @Test
    public void testWrapObjectAsMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("id", 123);

        JSONObject json = JsonUtil.wrap((Object) map);

        Assertions.assertNotNull(json);
        Assertions.assertEquals(123, json.getInt("id"));
    }

    @Test
    public void testWrapNestedBean() {
        SimpleBean simpleBean = new SimpleBean("Charlie", 35, false);
        NestedBean nestedBean = new NestedBean("123", simpleBean, Arrays.asList("tag1", "tag2"));

        JSONObject json = JsonUtil.wrap(nestedBean);

        assertNotNull(json);
        assertEquals("123", json.getString("id"));
        assertNotNull(json.getJSONObject("simpleBean"));
        assertEquals("Charlie", json.getJSONObject("simpleBean").getString("name"));
    }

    @Test
    public void testWrapBooleanArray() {
        boolean[] array = { true, false, true, false };
        JSONArray json = JsonUtil.wrap(array);

        assertNotNull(json);
        assertEquals(4, json.length());
        assertEquals(true, json.getBoolean(0));
        assertEquals(false, json.getBoolean(1));
        assertEquals(true, json.getBoolean(2));
        assertEquals(false, json.getBoolean(3));
    }

    @Test
    public void testWrapEmptyBooleanArray() {
        boolean[] array = {};
        JSONArray json = JsonUtil.wrap(array);

        assertNotNull(json);
        assertEquals(0, json.length());
    }

    @Test
    public void testWrapCharArray() {
        char[] array = { 'H', 'e', 'l', 'l', 'o' };
        JSONArray json = JsonUtil.wrap(array);

        assertNotNull(json);
        assertEquals(5, json.length());
        assertEquals('H', json.get(0));
        assertEquals('e', json.get(1));
        assertEquals('l', json.get(2));
    }

    @Test
    public void testWrapEmptyCharArray() {
        char[] array = {};
        JSONArray json = JsonUtil.wrap(array);

        assertNotNull(json);
        assertEquals(0, json.length());
    }

    @Test
    public void testWrapByteArray() {
        byte[] array = { 10, 20, 30, 40, 50 };
        JSONArray json = JsonUtil.wrap(array);

        assertNotNull(json);
        assertEquals(5, json.length());
        assertEquals(10, json.getInt(0));
        assertEquals(20, json.getInt(1));
        assertEquals(50, json.getInt(4));
    }

    @Test
    public void testWrapEmptyByteArray() {
        byte[] array = {};
        JSONArray json = JsonUtil.wrap(array);

        assertNotNull(json);
        assertEquals(0, json.length());
    }

    @Test
    public void testWrapShortArray() {
        short[] array = { 100, 200, 300, 400 };
        JSONArray json = JsonUtil.wrap(array);

        assertNotNull(json);
        assertEquals(4, json.length());
        assertEquals(100, json.getInt(0));
        assertEquals(200, json.getInt(1));
        assertEquals(400, json.getInt(3));
    }

    @Test
    public void testWrapEmptyShortArray() {
        short[] array = {};
        JSONArray json = JsonUtil.wrap(array);

        assertNotNull(json);
        assertEquals(0, json.length());
    }

    @Test
    public void testWrapIntArray() {
        int[] array = { 1, 2, 3, 4, 5 };
        JSONArray json = JsonUtil.wrap(array);

        assertNotNull(json);
        assertEquals(5, json.length());
        assertEquals(1, json.getInt(0));
        assertEquals(3, json.getInt(2));
        assertEquals(5, json.getInt(4));
    }

    @Test
    public void testWrapEmptyIntArray() {
        int[] array = {};
        JSONArray json = JsonUtil.wrap(array);

        assertNotNull(json);
        assertEquals(0, json.length());
    }

    @Test
    public void testWrapLongArray() {
        long[] array = { 1000L, 2000L, 3000L };
        JSONArray json = JsonUtil.wrap(array);

        assertNotNull(json);
        assertEquals(3, json.length());
        assertEquals(1000L, json.getLong(0));
        assertEquals(2000L, json.getLong(1));
        assertEquals(3000L, json.getLong(2));
    }

    @Test
    public void testWrapEmptyLongArray() {
        long[] array = {};
        JSONArray json = JsonUtil.wrap(array);

        assertNotNull(json);
        assertEquals(0, json.length());
    }

    @Test
    public void testWrapFloatArray() {
        float[] array = { 1.5f, 2.5f, 3.5f };
        JSONArray json = JsonUtil.wrap(array);

        assertNotNull(json);
        assertEquals(3, json.length());
        assertEquals(1.5, json.getDouble(0), 0.001);
        assertEquals(2.5, json.getDouble(1), 0.001);
        assertEquals(3.5, json.getDouble(2), 0.001);
    }

    @Test
    public void testWrapEmptyFloatArray() {
        float[] array = {};
        JSONArray json = JsonUtil.wrap(array);

        assertNotNull(json);
        assertEquals(0, json.length());
    }

    @Test
    public void testWrapDoubleArray() {
        double[] array = { 19.99, 29.99, 39.99 };
        JSONArray json = JsonUtil.wrap(array);

        assertNotNull(json);
        assertEquals(3, json.length());
        assertEquals(19.99, json.getDouble(0), 0.001);
        assertEquals(29.99, json.getDouble(1), 0.001);
        assertEquals(39.99, json.getDouble(2), 0.001);
    }

    @Test
    public void testWrapEmptyDoubleArray() {
        double[] array = {};
        JSONArray json = JsonUtil.wrap(array);

        assertNotNull(json);
        assertEquals(0, json.length());
    }

    @Test
    public void testWrapObjectArray() {
        Object[] array = { "text", 123, true, null };
        JSONArray json = JsonUtil.wrap(array);

        assertNotNull(json);
        assertEquals(4, json.length());
        assertEquals("text", json.getString(0));
        assertEquals(123, json.getInt(1));
        assertEquals(true, json.getBoolean(2));
        assertTrue(json.isNull(3));
    }

    @Test
    public void testWrapEmptyObjectArray() {
        Object[] array = {};
        JSONArray json = JsonUtil.wrap(array);

        assertNotNull(json);
        assertEquals(0, json.length());
    }

    @Test
    public void testWrapObjectArrayWithNestedCollections() {
        Map<String, Object> map = new HashMap<>();
        map.put("key", "value");
        List<Integer> list = Arrays.asList(1, 2, 3);

        Object[] array = { map, list };
        JSONArray json = JsonUtil.wrap(array);

        assertNotNull(json);
        assertEquals(2, json.length());
    }

    @Test
    public void testWrapList() {
        List<String> list = Arrays.asList("Alice", "Bob", "Charlie");
        JSONArray json = JsonUtil.wrap(list);

        assertNotNull(json);
        assertEquals(3, json.length());
        assertEquals("Alice", json.getString(0));
        assertEquals("Bob", json.getString(1));
        assertEquals("Charlie", json.getString(2));
    }

    @Test
    public void testWrapEmptyList() {
        List<String> list = new ArrayList<>();
        JSONArray json = JsonUtil.wrap(list);

        assertNotNull(json);
        assertEquals(0, json.length());
    }

    @Test
    public void testWrapSet() {
        Set<Integer> set = new LinkedHashSet<>(Arrays.asList(1, 2, 3));
        JSONArray json = JsonUtil.wrap(set);

        assertNotNull(json);
        assertEquals(3, json.length());
    }

    @Test
    public void testWrapCollectionWithNullElements() {
        List<Object> list = Arrays.asList("text", null, 123);
        JSONArray json = JsonUtil.wrap(list);

        assertNotNull(json);
        assertEquals(3, json.length());
        assertEquals("text", json.getString(0));
        assertTrue(json.isNull(1));
        assertEquals(123, json.getInt(2));
    }

    @Test
    public void testWrapCollection() {
        List<String> names = Arrays.asList("Alice", "Bob", "Charlie");

        JSONArray json = JsonUtil.wrap(names);

        Assertions.assertNotNull(json);
        Assertions.assertEquals(3, json.length());
        Assertions.assertEquals("Alice", json.getString(0));
        Assertions.assertEquals("Bob", json.getString(1));
        Assertions.assertEquals("Charlie", json.getString(2));
    }

    @Test
    public void testUnwrapJSONObjectToMap() {
        JSONObject json = new JSONObject();
        json.put("name", "David");
        json.put("age", 40);

        Map<String, Object> map = JsonUtil.unwrap(json);

        assertNotNull(map);
        assertEquals("David", map.get("name"));
        assertEquals(40, map.get("age"));
    }

    @Test
    public void testUnwrapEmptyJSONObject() {
        JSONObject json = new JSONObject();
        Map<String, Object> map = JsonUtil.unwrap(json);

        assertNotNull(map);
        assertEquals(0, map.size());
    }

    @Test
    public void testUnwrapJSONObjectWithNull() {
        JSONObject json = new JSONObject();
        json.put("key1", "value1");
        json.put("key2", JSONObject.NULL);

        Map<String, Object> map = JsonUtil.unwrap(json);

        assertNotNull(map);
        assertEquals("value1", map.get("key1"));
        assertNull(map.get("key2"));
    }

    @Test
    public void testUnwrapJSONObject() {
        JSONObject json = new JSONObject();
        json.put("name", "John");
        json.put("age", 30);
        json.put("active", true);

        Map<String, Object> map = JsonUtil.unwrap(json);

        Assertions.assertNotNull(map);
        Assertions.assertEquals("John", map.get("name"));
        Assertions.assertEquals(30, map.get("age"));
        Assertions.assertEquals(true, map.get("active"));
    }

    @Test
    public void testNestedStructures() {
        JSONObject outer = new JSONObject();
        JSONObject inner = new JSONObject();
        inner.put("innerKey", "innerValue");
        outer.put("nested", inner);
        outer.put("simple", "value");

        Map<String, Object> result = JsonUtil.unwrap(outer);

        Assertions.assertNotNull(result);
        Assertions.assertEquals("value", result.get("simple"));
        Assertions.assertTrue(result.get("nested") instanceof Map);
        @SuppressWarnings("unchecked")
        Map<String, Object> nestedMap = (Map<String, Object>) result.get("nested");
        Assertions.assertEquals("innerValue", nestedMap.get("innerKey"));
    }

    @Test
    public void testUnwrapJSONObjectToBean() {
        JSONObject json = new JSONObject();
        json.put("name", "Eve");
        json.put("age", 28);
        json.put("active", true);

        SimpleBean bean = JsonUtil.unwrap(json, SimpleBean.class);

        assertNotNull(bean);
        assertEquals("Eve", bean.getName());
        assertEquals(28, bean.getAge());
        assertEquals(true, bean.isActive());
    }

    @Test
    public void testUnwrapJSONObjectToTreeMap() {
        JSONObject json = new JSONObject();
        json.put("z", 3);
        json.put("a", 1);
        json.put("m", 2);

        TreeMap<String, Object> map = JsonUtil.unwrap(json, TreeMap.class);

        assertNotNull(map);
        assertEquals(3, map.size());
    }

    @Test
    public void testUnwrapJSONObjectToLinkedHashMap() {
        JSONObject json = new JSONObject();
        json.put("first", 1);
        json.put("second", 2);

        LinkedHashMap<String, Object> map = JsonUtil.unwrap(json, LinkedHashMap.class);

        assertNotNull(map);
        assertEquals(2, map.size());
    }

    @Test
    public void testUnwrapJSONObjectToBeanWithMissingProperties() {
        JSONObject json = new JSONObject();
        json.put("name", "Frank");
        json.put("unknownField", "value");

        SimpleBean bean = JsonUtil.unwrap(json, SimpleBean.class);

        assertNotNull(bean);
        assertEquals("Frank", bean.getName());
        assertEquals(0, bean.getAge());
    }

    @Test
    public void testUnwrapJSONObjectWithNestedObjects() {
        JSONObject simpleJson = new JSONObject();
        simpleJson.put("name", "George");
        simpleJson.put("age", 45);
        simpleJson.put("active", false);

        JSONObject nestedJson = new JSONObject();
        nestedJson.put("id", "456");
        nestedJson.put("simpleBean", simpleJson);

        NestedBean bean = JsonUtil.unwrap(nestedJson, NestedBean.class);

        assertNotNull(bean);
        assertEquals("456", bean.getId());
        assertNotNull(bean.getSimpleBean());
        assertEquals("George", bean.getSimpleBean().getName());
    }

    @Test
    public void testUnwrapJSONObjectWithType() {
        JSONObject json = new JSONObject();
        json.put("key", "value");

        Type<Map<String, Object>> type = CommonUtil.typeOf("Map<String, Object>");
        Map<String, Object> map = JsonUtil.unwrap(json, type);

        assertNotNull(map);
        assertEquals("value", map.get("key"));
    }

    @Test
    public void testUnwrapJSONObjectWithObjectType() {
        JSONObject json = new JSONObject();
        json.put("name", "Helen");

        Type<Object> type = CommonUtil.typeOf(Object.class);
        Object result = JsonUtil.unwrap(json, type);

        assertNotNull(result);
        assertTrue(result instanceof Map);
    }

    @Test
    public void testUnwrapJSONObjectReturnsJSONObject() {
        JSONObject json = new JSONObject();
        json.put("test", "value");

        Type<JSONObject> type = CommonUtil.typeOf(JSONObject.class);
        JSONObject result = JsonUtil.unwrap(json, type);

        assertNotNull(result);
        assertEquals(json, result);
    }

    @Test
    public void testUnwrapJSONObjectWithBeanType() {
        JSONObject json = new JSONObject();
        json.put("name", "Ivan");
        json.put("age", 50);
        json.put("active", true);

        Type<SimpleBean> type = CommonUtil.typeOf(SimpleBean.class);
        SimpleBean bean = JsonUtil.unwrap(json, type);

        assertNotNull(bean);
        assertEquals("Ivan", bean.getName());
        assertEquals(50, bean.getAge());
    }

    @Test
    public void testUnwrapJSONArrayToList() {
        JSONArray json = new JSONArray();
        json.put("text");
        json.put(123);
        json.put(true);

        List<Object> list = JsonUtil.unwrap(json);

        assertNotNull(list);
        assertEquals(3, list.size());
        assertEquals("text", list.get(0));
        assertEquals(123, list.get(1));
        assertEquals(true, list.get(2));
    }

    @Test
    public void testUnwrapEmptyJSONArray() {
        JSONArray json = new JSONArray();
        List<Object> list = JsonUtil.unwrap(json);

        assertNotNull(list);
        assertEquals(0, list.size());
    }

    @Test
    public void testUnwrapJSONArrayWithNull() {
        JSONArray json = new JSONArray();
        json.put("value");
        json.put(JSONObject.NULL);

        List<Object> list = JsonUtil.unwrap(json);

        assertNotNull(list);
        assertEquals(2, list.size());
        assertEquals("value", list.get(0));
        assertNull(list.get(1));
    }

    @Test
    public void testUnwrapJSONArray() {
        JSONArray json = new JSONArray();
        json.put("text");
        json.put(123);
        json.put(true);
        json.put(JSONObject.NULL);

        List<Object> list = JsonUtil.unwrap(json);

        Assertions.assertNotNull(list);
        Assertions.assertEquals(4, list.size());
        Assertions.assertEquals("text", list.get(0));
        Assertions.assertEquals(123, list.get(1));
        Assertions.assertEquals(true, list.get(2));
        Assertions.assertNull(list.get(3));
    }

    @Test
    public void testUnwrapJSONArrayToListClass() {
        JSONArray json = new JSONArray();
        json.put("a");
        json.put("b");
        json.put("c");

        List<String> list = JsonUtil.unwrap(json, List.class);

        assertNotNull(list);
        assertEquals(3, list.size());
    }

    @Test
    public void testUnwrapJSONArrayToSet() {
        JSONArray json = new JSONArray();
        json.put(1);
        json.put(2);
        json.put(3);

        Set<Integer> set = JsonUtil.unwrap(json, Set.class);

        assertNotNull(set);
        assertEquals(3, set.size());
    }

    @Test
    public void testUnwrapJSONArrayToIntArray() {
        JSONArray json = new JSONArray();
        json.put(10);
        json.put(20);
        json.put(30);

        int[] array = JsonUtil.unwrap(json, int[].class);

        assertNotNull(array);
        assertEquals(3, array.length);
        assertEquals(10, array[0]);
        assertEquals(20, array[1]);
        assertEquals(30, array[2]);
    }

    @Test
    public void testUnwrapJSONArrayToStringArray() {
        JSONArray json = new JSONArray();
        json.put("apple");
        json.put("banana");

        String[] array = JsonUtil.unwrap(json, String[].class);

        assertNotNull(array);
        assertEquals(2, array.length);
        assertEquals("apple", array[0]);
        assertEquals("banana", array[1]);
    }

    @Test
    public void testUnwrapJSONArrayToBooleanArray() {
        JSONArray json = new JSONArray();
        json.put(true);
        json.put(false);
        json.put(true);

        boolean[] array = JsonUtil.unwrap(json, boolean[].class);

        assertNotNull(array);
        assertEquals(3, array.length);
        assertEquals(true, array[0]);
        assertEquals(false, array[1]);
        assertEquals(true, array[2]);
    }

    @Test
    public void testUnwrapJSONArrayToPrimitiveArray() {
        JSONArray json = new JSONArray();
        json.put(85);
        json.put(90);
        json.put(78);
        json.put(92);
        json.put(88);

        int[] scores = JsonUtil.unwrap(json, int[].class);

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

        String[] names = JsonUtil.unwrap(json, String[].class);

        Assertions.assertNotNull(names);
        Assertions.assertEquals(3, names.length);
        Assertions.assertEquals("Alice", names[0]);
    }

    @Test
    public void testUnwrapJSONArrayWithType() {
        JSONArray json = new JSONArray();
        json.put(1);
        json.put(2);
        json.put(3);

        Type<List<Integer>> type = CommonUtil.typeOf("List<Integer>");
        List<Integer> list = JsonUtil.unwrap(json, type);

        assertNotNull(list);
        assertEquals(3, list.size());
    }

    @Test
    public void testUnwrapJSONArrayWithObjectType() {
        JSONArray json = new JSONArray();
        json.put("item");

        Type<Object> type = CommonUtil.typeOf(Object.class);
        Object result = JsonUtil.unwrap(json, type);

        assertNotNull(result);
        assertTrue(result instanceof List);
    }

    @Test
    public void testUnwrapJSONArrayReturnsJSONArray() {
        JSONArray json = new JSONArray();
        json.put("test");

        Type<JSONArray> type = CommonUtil.typeOf(JSONArray.class);
        JSONArray result = JsonUtil.unwrap(json, type);

        assertNotNull(result);
        assertEquals(json, result);
    }

    @Test
    public void testUnwrapJSONArrayWithSetType() {
        JSONArray json = new JSONArray();
        json.put("x");
        json.put("y");
        json.put("z");

        Type<Set<String>> type = CommonUtil.typeOf("Set<String>");
        Set<String> set = JsonUtil.unwrap(json, type);

        assertNotNull(set);
        assertEquals(3, set.size());
    }

    @Test
    public void testUnwrapJSONArrayWithPrimitiveArrayType() {
        JSONArray json = new JSONArray();
        json.put(5);
        json.put(10);
        json.put(15);

        Type<int[]> type = CommonUtil.typeOf(int[].class);
        int[] array = JsonUtil.unwrap(json, type);

        assertNotNull(array);
        assertEquals(3, array.length);
        assertArrayEquals(new int[] { 5, 10, 15 }, array);
    }

    @Test
    public void testUnwrapJSONArrayWithObjectArrayType() {
        JSONArray json = new JSONArray();
        json.put("a");
        json.put("b");

        Type<String[]> type = CommonUtil.typeOf(String[].class);
        String[] array = JsonUtil.unwrap(json, type);

        assertNotNull(array);
        assertEquals(2, array.length);
        assertArrayEquals(new String[] { "a", "b" }, array);
    }

    @Test
    public void testUnwrapJSONArrayWithNestedObjects() {
        JSONObject obj1 = new JSONObject();
        obj1.put("name", "John");

        JSONObject obj2 = new JSONObject();
        obj2.put("name", "Jane");

        JSONArray json = new JSONArray();
        json.put(obj1);
        json.put(obj2);

        Type<List<SimpleBean>> type = Type.ofList(SimpleBean.class);
        List<SimpleBean> beans = JsonUtil.unwrap(json, type);

        assertNotNull(beans);
        assertEquals(2, beans.size());
        assertEquals("John", beans.get(0).getName());
        assertEquals("Jane", beans.get(1).getName());
    }

    @Test
    public void testUnwrapJSONArrayWithNestedArrays() {
        JSONArray inner1 = new JSONArray();
        inner1.put(1);
        inner1.put(2);

        JSONArray inner2 = new JSONArray();
        inner2.put(3);
        inner2.put(4);

        JSONArray json = new JSONArray();
        json.put(inner1);
        json.put(inner2);

        Type<List<List<Integer>>> type = CommonUtil.typeOf("List<List<Integer>>");
        List<List<Integer>> result = JsonUtil.unwrap(json, type);

        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals(2, result.get(0).size());
        assertEquals(1, result.get(0).get(0));
        assertEquals(4, result.get(1).get(1));
    }

    @Test
    public void testComplexNestedArray() {
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

        Type<List<List<Integer>>> type = CommonUtil.typeOf("List<List<Integer>>");
        List<List<Integer>> result = JsonUtil.unwrap(outer, type);

        Assertions.assertNotNull(result);
        Assertions.assertEquals(2, result.size());
        Assertions.assertEquals(3, result.get(0).size());
        Assertions.assertEquals(1, result.get(0).get(0));
        Assertions.assertEquals(6, result.get(1).get(2));
    }

    @Test
    public void testUnwrapJSONArrayToPrimitiveArraysWithNull() {
        JSONArray json = new JSONArray();
        json.put(1);
        json.put(JSONObject.NULL);
        json.put(3);

        int[] array = JsonUtil.unwrap(json, int[].class);

        assertNotNull(array);
        assertEquals(3, array.length);
        assertEquals(1, array[0]);
        assertEquals(0, array[1]);
        assertEquals(3, array[2]);
    }

    @Test
    public void testUnwrapWithDifferentCollectionTypes() {
        JSONArray json = new JSONArray();
        json.put("a");
        json.put("b");

        Type<ArrayList<String>> arrayListType = CommonUtil.typeOf("ArrayList<String>");
        ArrayList<String> arrayList = JsonUtil.unwrap(json, arrayListType);

        assertNotNull(arrayList);
        assertTrue(arrayList instanceof ArrayList);
        assertEquals(2, arrayList.size());
    }

    @Test
    public void testUnwrapJSONArrayToDoubleArray() {
        JSONArray json = new JSONArray();
        json.put(1.1);
        json.put(2.2);
        json.put(3.3);

        double[] array = JsonUtil.unwrap(json, double[].class);

        assertNotNull(array);
        assertEquals(3, array.length);
        assertEquals(1.1, array[0], 0.001);
        assertEquals(2.2, array[1], 0.001);
        assertEquals(3.3, array[2], 0.001);
    }

    @Test
    public void testUnwrapJSONArrayToLongArray() {
        JSONArray json = new JSONArray();
        json.put(1000000L);
        json.put(2000000L);

        long[] array = JsonUtil.unwrap(json, long[].class);

        assertNotNull(array);
        assertEquals(2, array.length);
        assertEquals(1000000L, array[0]);
        assertEquals(2000000L, array[1]);
    }

    @Test
    public void testUnwrapJSONArrayToFloatArray() {
        JSONArray json = new JSONArray();
        json.put(1.5f);
        json.put(2.5f);

        float[] array = JsonUtil.unwrap(json, float[].class);

        assertNotNull(array);
        assertEquals(2, array.length);
        assertEquals(1.5f, array[0], 0.001f);
        assertEquals(2.5f, array[1], 0.001f);
    }

    @Test
    public void testUnwrapJSONObjectWithInvalidType() {
        JSONObject json = new JSONObject();
        json.put("key", "value");

        Type<String> type = CommonUtil.typeOf(String.class);

        assertThrows(IllegalArgumentException.class, () -> {
            JsonUtil.unwrap(json, type);
        });
    }

    @Test
    public void testExceptionHandling() {
        JSONObject json = new JSONObject();
        json.put("key", "value");

        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            JsonUtil.unwrap(json, String.class);
        });

        JSONArray array = new JSONArray();
        array.put(1);

        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            JsonUtil.unwrap(array, String.class);
        });
    }

    @Test
    public void testUnwrapJSONArrayWithInvalidType() {
        JSONArray json = new JSONArray();
        json.put("value");

        Type<String> type = CommonUtil.typeOf(String.class);

        assertThrows(IllegalArgumentException.class, () -> {
            JsonUtil.unwrap(json, type);
        });
    }

    @Test
    public void testUnwrapJSONArrayToByteArray() {
        JSONArray json = new JSONArray();
        json.put((byte) 10);
        json.put((byte) 20);

        assertThrows(IllegalArgumentException.class, () -> JsonUtil.unwrap(json, byte[].class));
    }

    @Test
    public void testUnwrapJSONArrayToShortArray() {
        JSONArray json = new JSONArray();
        json.put((short) 100);
        json.put((short) 200);

        assertThrows(IllegalArgumentException.class, () -> JsonUtil.unwrap(json, short[].class));
    }

    @Test
    public void testUnwrapJSONArrayToCharArray() {
        JSONArray json = new JSONArray();
        json.put("A");
        json.put("B");

        assertThrows(IllegalArgumentException.class, () -> JsonUtil.unwrap(json, char[].class));
    }

    @Test
    public void testToListWithStringClass() {
        JSONArray json = new JSONArray();
        json.put("apple");
        json.put("banana");
        json.put("cherry");

        List<String> list = JsonUtil.toList(json, String.class);

        assertNotNull(list);
        assertEquals(3, list.size());
        assertEquals("apple", list.get(0));
        assertEquals("banana", list.get(1));
        assertEquals("cherry", list.get(2));
    }

    @Test
    public void testToListWithIntegerClass() {
        JSONArray json = new JSONArray();
        json.put(100);
        json.put(200);

        List<Integer> list = JsonUtil.toList(json, Integer.class);

        assertNotNull(list);
        assertEquals(2, list.size());
        assertEquals(100, list.get(0));
        assertEquals(200, list.get(1));
    }

    @Test
    public void testToListWithObjectClass() {
        JSONArray json = new JSONArray();
        json.put("text");
        json.put(123);
        json.put(true);

        List<Object> list = JsonUtil.toList(json, Object.class);

        assertNotNull(list);
        assertEquals(3, list.size());
    }

    @Test
    public void testToListWithBeanClass() {
        JSONObject obj1 = new JSONObject();
        obj1.put("name", "Kate");
        obj1.put("age", 32);

        JSONArray json = new JSONArray();
        json.put(obj1);

        List<SimpleBean> list = JsonUtil.toList(json, SimpleBean.class);

        assertNotNull(list);
        assertEquals(1, list.size());
        assertEquals("Kate", list.get(0).getName());
        assertEquals(32, list.get(0).getAge());
    }

    @Test
    public void testToListEmptyArray() {
        JSONArray json = new JSONArray();
        List<String> list = JsonUtil.toList(json, String.class);

        assertNotNull(list);
        assertEquals(0, list.size());
    }

    @Test
    public void testToListWithClass() {
        JSONArray json = new JSONArray();
        json.put("apple");
        json.put("banana");
        json.put("orange");

        List<String> fruits = JsonUtil.toList(json, String.class);

        Assertions.assertNotNull(fruits);
        Assertions.assertEquals(3, fruits.size());
        Assertions.assertEquals("apple", fruits.get(0));
        Assertions.assertEquals("banana", fruits.get(1));
        Assertions.assertEquals("orange", fruits.get(2));
    }

    @Test
    public void testToListWithType() {
        JSONArray json = new JSONArray();
        json.put("x");
        json.put("y");

        Type<String> type = CommonUtil.typeOf(String.class);
        List<String> list = JsonUtil.toList(json, type);

        assertNotNull(list);
        assertEquals(2, list.size());
        assertEquals("x", list.get(0));
        assertEquals("y", list.get(1));
    }

    @Test
    public void testToListWithMapType() {
        JSONObject map1 = new JSONObject();
        map1.put("id", 1);

        JSONObject map2 = new JSONObject();
        map2.put("id", 2);

        JSONArray json = new JSONArray();
        json.put(map1);
        json.put(map2);

        Type<Map<String, Object>> type = CommonUtil.typeOf("Map<String, Object>");
        List<Map<String, Object>> list = JsonUtil.toList(json, type);

        assertNotNull(list);
        assertEquals(2, list.size());
        assertEquals(1, list.get(0).get("id"));
        assertEquals(2, list.get(1).get("id"));
    }

    @Test
    public void testToListWithNestedListType() {
        JSONArray inner = new JSONArray();
        inner.put(7);
        inner.put(8);

        JSONArray json = new JSONArray();
        json.put(inner);

        Type<List<Integer>> type = CommonUtil.typeOf("List<Integer>");
        List<List<Integer>> list = JsonUtil.toList(json, type);

        assertNotNull(list);
        assertEquals(1, list.size());
        assertEquals(2, list.get(0).size());
        assertEquals(7, list.get(0).get(0));
    }

    @Test
    public void testToListWithNullElements() {
        JSONArray json = new JSONArray();
        json.put("first");
        json.put(JSONObject.NULL);
        json.put("third");

        Type<String> type = CommonUtil.typeOf(String.class);
        List<String> list = JsonUtil.toList(json, type);

        assertNotNull(list);
        assertEquals(3, list.size());
        assertEquals("first", list.get(0));
        assertNull(list.get(1));
        assertEquals("third", list.get(2));
    }

}

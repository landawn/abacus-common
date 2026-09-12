package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.math.BigInteger;
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

        public SimpleBean(final String name, final int age, final boolean active) {
            this.name = name;
            this.age = age;
            this.active = active;
        }

        public String getName() {
            return name;
        }

        public void setName(final String name) {
            this.name = name;
        }

        public int getAge() {
            return age;
        }

        public void setAge(final int age) {
            this.age = age;
        }

        public boolean isActive() {
            return active;
        }

        public void setActive(final boolean active) {
            this.active = active;
        }
    }

    public static class NestedBean {
        private String id;
        private SimpleBean simpleBean;
        private List<String> tags;

        public NestedBean() {
        }

        public NestedBean(final String id, final SimpleBean simpleBean, final List<String> tags) {
            this.id = id;
            this.simpleBean = simpleBean;
            this.tags = tags;
        }

        public String getId() {
            return id;
        }

        public void setId(final String id) {
            this.id = id;
        }

        public SimpleBean getSimpleBean() {
            return simpleBean;
        }

        public void setSimpleBean(final SimpleBean simpleBean) {
            this.simpleBean = simpleBean;
        }

        public List<String> getTags() {
            return tags;
        }

        public void setTags(final List<String> tags) {
            this.tags = tags;
        }
    }

    @Test
    public void testWrap() {
        final SimpleBean original = new SimpleBean("Liam", 27, true);
        final JSONObject json = JsonUtil.wrap(original);
        assertEquals("Liam", json.getString("name"));
        assertEquals(27, json.getInt("age"));
        assertEquals(true, json.getBoolean("active"));

        final SimpleBean restored = JsonUtil.unwrap(json, SimpleBean.class);
        assertEquals(original.getName(), restored.getName());
        assertEquals(original.getAge(), restored.getAge());
        assertEquals(original.isActive(), restored.isActive());

        final NestedBean nested = new NestedBean("123", new SimpleBean("Charlie", 35, false), Arrays.asList("tag1", "tag2"));
        final JSONObject nestedJson = JsonUtil.wrap(nested);
        assertEquals("123", nestedJson.getString("id"));
        assertEquals("Charlie", nestedJson.getJSONObject("simpleBean").getString("name"));
    }

    @Test
    public void testWrap_Map() {
        final Map<String, Object> map = new HashMap<>();
        map.put("name", "Alice");
        map.put("age", 30);
        map.put("active", true);
        final JSONObject json = JsonUtil.wrap(map);
        assertEquals("Alice", json.getString("name"));
        assertEquals(30, json.getInt("age"));
        assertEquals(true, json.getBoolean("active"));

        assertEquals(0, JsonUtil.wrap(new HashMap<>()).length());

        final Map<String, Object> withNull = new HashMap<>();
        withNull.put("key1", "value1");
        withNull.put("key2", null);
        final JSONObject omitted = JsonUtil.wrap(withNull);
        assertEquals("value1", omitted.getString("key1"));
        assertTrue(omitted.isNull("key2"));
        assertFalse(omitted.has("key2"));

        final Map<String, Object> asObject = new HashMap<>();
        asObject.put("id", 123);
        assertEquals(123, JsonUtil.wrap((Object) asObject).getInt("id"));
    }

    @Test
    public void testWrap_Arrays() {
        final JSONArray booleans = JsonUtil.wrap(new boolean[] { true, false, true });
        assertEquals(3, booleans.length());
        assertEquals(true, booleans.getBoolean(0));
        assertEquals(0, JsonUtil.wrap(new boolean[0]).length());

        final JSONArray chars = JsonUtil.wrap(new char[] { 'H', 'e', 'l' });
        assertEquals(3, chars.length());
        assertEquals('H', chars.get(0));
        assertEquals(0, JsonUtil.wrap(new char[0]).length());

        final JSONArray bytes = JsonUtil.wrap(new byte[] { 10, 20, 50 });
        assertEquals(10, bytes.getInt(0));
        assertEquals(50, bytes.getInt(2));
        assertEquals(0, JsonUtil.wrap(new byte[0]).length());

        final JSONArray shorts = JsonUtil.wrap(new short[] { 100, 400 });
        assertEquals(100, shorts.getInt(0));
        assertEquals(0, JsonUtil.wrap(new short[0]).length());

        final JSONArray ints = JsonUtil.wrap(new int[] { 1, 3, 5 });
        assertEquals(5, ints.getInt(2));
        assertEquals(0, JsonUtil.wrap(new int[0]).length());

        final JSONArray longs = JsonUtil.wrap(new long[] { 1000L, 3000L });
        assertEquals(3000L, longs.getLong(1));
        assertEquals(0, JsonUtil.wrap(new long[0]).length());

        final JSONArray floats = JsonUtil.wrap(new float[] { 1.5f, 3.5f });
        assertEquals(1.5, floats.getDouble(0), 0.001);
        assertEquals(0, JsonUtil.wrap(new float[0]).length());

        final JSONArray doubles = JsonUtil.wrap(new double[] { 19.99, 39.99 });
        assertEquals(19.99, doubles.getDouble(0), 0.001);
        assertEquals(0, JsonUtil.wrap(new double[0]).length());

        final JSONArray objects = JsonUtil.wrap(new Object[] { "text", 123, true, null });
        assertEquals("text", objects.getString(0));
        assertTrue(objects.isNull(3));
        assertEquals(0, JsonUtil.wrap(new Object[0]).length());

        final Map<String, Object> nestedMap = new HashMap<>();
        nestedMap.put("key", "value");
        assertEquals(2, JsonUtil.wrap(new Object[] { nestedMap, Arrays.asList(1, 2, 3) }).length());
    }

    @Test
    public void testWrap_Collection() {
        final List<Integer> original = Arrays.asList(5, 10, 15, 20);
        assertEquals(original, JsonUtil.toList(JsonUtil.wrap(original), Integer.class));

        final JSONArray names = JsonUtil.wrap(Arrays.asList("Alice", "Bob", "Charlie"));
        assertEquals(3, names.length());
        assertEquals("Alice", names.getString(0));
        assertEquals(0, JsonUtil.wrap(new ArrayList<>()).length());

        final JSONArray set = JsonUtil.wrap(new LinkedHashSet<>(Arrays.asList(1, 2, 3)));
        assertEquals(3, set.length());

        final JSONArray withNull = JsonUtil.wrap(Arrays.asList("text", null, 123));
        assertTrue(withNull.isNull(1));
        assertEquals(123, withNull.getInt(2));
    }

    @Test
    public void testUnwrap() {
        final JSONObject json = new JSONObject();
        json.put("name", "David");
        json.put("age", 40);
        json.put("active", true);
        final Map<String, Object> map = JsonUtil.unwrap(json);
        assertEquals("David", map.get("name"));
        assertEquals(40, map.get("age"));
        assertEquals(true, map.get("active"));

        assertEquals(0, JsonUtil.unwrap(new JSONObject()).size());

        final JSONObject withNull = new JSONObject();
        withNull.put("key1", "value1");
        withNull.put("key2", JSONObject.NULL);
        assertEquals("value1", JsonUtil.unwrap(withNull).get("key1"));
        assertNull(JsonUtil.unwrap(withNull).get("key2"));

        final JSONObject inner = new JSONObject();
        inner.put("innerKey", "innerValue");
        final JSONObject outer = new JSONObject();
        outer.put("nested", inner);
        outer.put("simple", "value");
        final Map<String, Object> nested = JsonUtil.unwrap(outer);
        assertEquals("value", nested.get("simple"));
        @SuppressWarnings("unchecked")
        final Map<String, Object> nestedMap = (Map<String, Object>) nested.get("nested");
        assertEquals("innerValue", nestedMap.get("innerKey"));
    }

    @Test
    public void testUnwrap_JSONObjectTarget() {
        final JSONObject json = new JSONObject();
        json.put("name", "Eve");
        json.put("age", 28);
        json.put("active", true);
        final SimpleBean bean = JsonUtil.unwrap(json, SimpleBean.class);
        assertEquals("Eve", bean.getName());
        assertEquals(28, bean.getAge());
        assertTrue(bean.isActive());

        final TreeMap<String, Object> tree = JsonUtil.unwrap(json, TreeMap.class);
        assertEquals(3, tree.size());
        assertEquals(3, JsonUtil.unwrap(json, LinkedHashMap.class).size());

        final JSONObject missing = new JSONObject();
        missing.put("name", "Frank");
        missing.put("unknownField", "value");
        final SimpleBean partial = JsonUtil.unwrap(missing, SimpleBean.class);
        assertEquals("Frank", partial.getName());
        assertEquals(0, partial.getAge());

        final JSONObject simpleJson = new JSONObject();
        simpleJson.put("name", "George");
        simpleJson.put("age", 45);
        simpleJson.put("active", false);
        final JSONObject nestedJson = new JSONObject();
        nestedJson.put("id", "456");
        nestedJson.put("simpleBean", simpleJson);
        final NestedBean nested = JsonUtil.unwrap(nestedJson, NestedBean.class);
        assertEquals("456", nested.getId());
        assertEquals("George", nested.getSimpleBean().getName());

        final Type<Map<String, Object>> mapType = new TypeReference<Map<String, Object>>() {
        }.type();
        assertEquals("value", JsonUtil.unwrap(new JSONObject().put("key", "value"), mapType).get("key"));

        final Object asObject = JsonUtil.unwrap(json, CommonUtil.typeOf(Object.class));
        assertTrue(asObject instanceof Map);
        assertEquals(json, JsonUtil.unwrap(json, CommonUtil.typeOf(JSONObject.class)));

        final Type<SimpleBean> beanType = CommonUtil.typeOf(SimpleBean.class);
        assertEquals("Eve", JsonUtil.unwrap(json, beanType).getName());
    }

    @Test
    public void testUnwrap_JSONObjectConvertsMapTypes() {
        final JSONObject values = new JSONObject();
        values.put("id", 123);
        final Type<Map<String, String>> valueType = new TypeReference<Map<String, String>>() {
        }.type();
        assertEquals("123", JsonUtil.unwrap(values, valueType).get("id"));

        final JSONObject keys = new JSONObject();
        keys.put("123", "value");
        final Type<Map<Integer, String>> keyType = new TypeReference<Map<Integer, String>>() {
        }.type();
        final Map<Integer, String> map = JsonUtil.unwrap(keys, keyType);
        assertEquals("value", map.get(123));
        assertFalse(map.containsKey("123"));
    }

    @Test
    public void testUnwrap_JSONArray() {
        final JSONArray json = new JSONArray();
        json.put("text");
        json.put(123);
        json.put(true);
        json.put(JSONObject.NULL);
        final List<Object> list = JsonUtil.unwrap(json);
        assertEquals(4, list.size());
        assertEquals("text", list.get(0));
        assertNull(list.get(3));
        assertEquals(0, JsonUtil.unwrap(new JSONArray()).size());

        final JSONArray withNull = new JSONArray();
        withNull.put("value");
        withNull.put(JSONObject.NULL);
        assertNull(JsonUtil.unwrap(withNull).get(1));

        assertEquals(3, JsonUtil.unwrap(new JSONArray().put("a").put("b").put("c"), List.class).size());
        assertEquals(3, JsonUtil.unwrap(new JSONArray().put(1).put(2).put(3), Set.class).size());

        final Object asObject = JsonUtil.unwrap(json, CommonUtil.typeOf(Object.class));
        assertTrue(asObject instanceof List);
        assertEquals(json, JsonUtil.unwrap(json, CommonUtil.typeOf(JSONArray.class)));
    }

    @Test
    public void testUnwrap_JSONArrayTargets() {
        assertArrayEquals(new int[] { 10, 20, 30 }, JsonUtil.unwrap(new JSONArray().put(10).put(20).put(30), int[].class));
        assertArrayEquals(new String[] { "apple", "banana" }, JsonUtil.unwrap(new JSONArray().put("apple").put("banana"), String[].class));
        assertArrayEquals(new boolean[] { true, false, true }, JsonUtil.unwrap(new JSONArray().put(true).put(false).put(true), boolean[].class));
        assertArrayEquals(new byte[] { 10, 20 }, JsonUtil.unwrap(new JSONArray().put(10).put(20), byte[].class));
        assertArrayEquals(new short[] { 100, 200 }, JsonUtil.unwrap(new JSONArray().put(100).put(200), short[].class));
        assertArrayEquals(new char[] { 'A', 'B' }, JsonUtil.unwrap(new JSONArray().put("A").put("B"), char[].class));
        assertArrayEquals(new long[] { 1000000L, 2000000L }, JsonUtil.unwrap(new JSONArray().put(1000000L).put(2000000L), long[].class));
        assertEquals(1.1, JsonUtil.unwrap(new JSONArray().put(1.1).put(2.2), double[].class)[0], 0.001);
        assertEquals(1.5f, JsonUtil.unwrap(new JSONArray().put(1.5f).put(2.5f), float[].class)[0], 0.001f);

        final Type<int[]> intType = CommonUtil.typeOf(int[].class);
        assertArrayEquals(new int[] { 5, 10, 15 }, JsonUtil.unwrap(new JSONArray().put(5).put(10).put(15), intType));
        assertArrayEquals(new String[] { "a", "b" }, JsonUtil.unwrap(new JSONArray().put("a").put("b"), CommonUtil.typeOf(String[].class)));

        final int[] withNull = JsonUtil.unwrap(new JSONArray().put(1).put(JSONObject.NULL).put(3), int[].class);
        assertEquals(0, withNull[1]);
        assertEquals(3, withNull[2]);
    }

    @Test
    public void testUnwrap_JSONArrayTypedCollections() {
        final Type<List<Integer>> listType = new TypeReference<List<Integer>>() {
        }.type();
        assertEquals(3, JsonUtil.unwrap(new JSONArray().put(1).put(2).put(3), listType).size());

        final Type<Set<String>> setType = new TypeReference<Set<String>>() {
        }.type();
        assertEquals(3, JsonUtil.unwrap(new JSONArray().put("x").put("y").put("z"), setType).size());

        final Type<ArrayList<String>> arrayListType = new TypeReference<ArrayList<String>>() {
        }.type();
        assertTrue(JsonUtil.unwrap(new JSONArray().put("a").put("b"), arrayListType) instanceof ArrayList);

        assertArrayEquals(new String[] { "1", "true" }, JsonUtil.unwrap(new JSONArray().put(1).put(true), String[].class));
        assertArrayEquals(new Integer[] { 1, 2 }, JsonUtil.unwrap(new JSONArray().put("1").put(2), Integer[].class));

        final Type<List<String>> stringListType = new TypeReference<List<String>>() {
        }.type();
        assertEquals(Arrays.asList("123", "true"), JsonUtil.unwrap(new JSONArray().put(123).put(true), stringListType));

        final JSONObject obj1 = new JSONObject().put("name", "John");
        final JSONObject obj2 = new JSONObject().put("name", "Jane");
        final List<SimpleBean> beans = JsonUtil.unwrap(new JSONArray().put(obj1).put(obj2), Type.ofList(SimpleBean.class));
        assertEquals("John", beans.get(0).getName());
        assertEquals("Jane", beans.get(1).getName());

        final JSONArray nested = new JSONArray().put(new JSONArray().put(1).put(2)).put(new JSONArray().put(3).put(4));
        final Type<List<List<Integer>>> nestedType = new TypeReference<List<List<Integer>>>() {
        }.type();
        final List<List<Integer>> result = JsonUtil.unwrap(nested, nestedType);
        assertEquals(1, result.get(0).get(0));
        assertEquals(4, result.get(1).get(1));
    }

    @Test
    public void testUnwrap_InvalidType() {
        assertThrows(IllegalArgumentException.class, () -> JsonUtil.unwrap(new JSONObject().put("key", "value"), String.class));
        assertThrows(IllegalArgumentException.class, () -> JsonUtil.unwrap(new JSONObject().put("key", "value"), CommonUtil.typeOf(String.class)));
        assertThrows(IllegalArgumentException.class, () -> JsonUtil.unwrap(new JSONArray().put(1), String.class));
        assertThrows(IllegalArgumentException.class, () -> JsonUtil.unwrap(new JSONArray().put("value"), CommonUtil.typeOf(String.class)));
    }

    @Test
    public void testToList() {
        final JSONArray strings = new JSONArray().put("apple").put("banana").put("cherry");
        assertEquals(Arrays.asList("apple", "banana", "cherry"), JsonUtil.toList(strings, String.class));
        assertEquals(Arrays.asList(100, 200), JsonUtil.toList(new JSONArray().put(100).put(200), Integer.class));
        assertEquals("123", JsonUtil.toList(new JSONArray().put(123), String.class).get(0));
        assertEquals(Integer.valueOf(123), JsonUtil.toList(new JSONArray().put("123"), Integer.class).get(0));

        final List<Object> mixed = JsonUtil.toList(new JSONArray().put("text").put(123).put(true), Object.class);
        assertEquals(3, mixed.size());

        final JSONObject obj = new JSONObject().put("name", "Kate").put("age", 32);
        final List<SimpleBean> beans = JsonUtil.toList(new JSONArray().put(obj), SimpleBean.class);
        assertEquals("Kate", beans.get(0).getName());
        assertEquals(32, beans.get(0).getAge());

        assertEquals(0, JsonUtil.toList(new JSONArray(), String.class).size());
        assertEquals(Arrays.asList("x", "y"), JsonUtil.toList(new JSONArray().put("x").put("y"), CommonUtil.typeOf(String.class)));

        final Type<Map<String, Object>> mapType = new TypeReference<Map<String, Object>>() {
        }.type();
        final List<Map<String, Object>> maps = JsonUtil.toList(new JSONArray().put(new JSONObject().put("id", 1)).put(new JSONObject().put("id", 2)), mapType);
        assertEquals(1, maps.get(0).get("id"));
        assertEquals(2, maps.get(1).get("id"));

        final Type<List<Integer>> nestedType = new TypeReference<List<Integer>>() {
        }.type();
        final List<List<Integer>> nested = JsonUtil.toList(new JSONArray().put(new JSONArray().put(7).put(8)), nestedType);
        assertEquals(7, nested.get(0).get(0));

        final List<String> withNull = JsonUtil.toList(new JSONArray().put("first").put(JSONObject.NULL).put("third"), CommonUtil.typeOf(String.class));
        assertEquals("first", withNull.get(0));
        assertNull(withNull.get(1));
        assertEquals("third", withNull.get(2));
    }

    @Test
    public void testRequiredUnwrapArgumentsAreValidated() {
        final JSONObject object = new JSONObject();
        final JSONArray array = new JSONArray();

        assertThrows(IllegalArgumentException.class, () -> JsonUtil.unwrap((JSONObject) null, Map.class));
        assertThrows(IllegalArgumentException.class, () -> JsonUtil.unwrap(object, (Class<?>) null));
        assertThrows(IllegalArgumentException.class, () -> JsonUtil.unwrap(object, (Type<?>) null));
        assertThrows(IllegalArgumentException.class, () -> JsonUtil.unwrap((JSONArray) null, List.class));
        assertThrows(IllegalArgumentException.class, () -> JsonUtil.unwrap(array, (Class<?>) null));
        assertThrows(IllegalArgumentException.class, () -> JsonUtil.unwrap(array, (Type<?>) null));
        assertThrows(IllegalArgumentException.class, () -> JsonUtil.toList(null, String.class));
        assertThrows(IllegalArgumentException.class, () -> JsonUtil.toList(array, (Class<?>) null));
        assertThrows(IllegalArgumentException.class, () -> JsonUtil.toList(array, (Type<Object>) null));
    }

    @Test
    public void testUnwrapNumberBoxTypesMatchTheDocumentedMapping() {
        // A parsed integer literal: Integer / Long / BigInteger by magnitude.
        assertEquals(Integer.class, JsonUtil.unwrap(new JSONObject("{\"k\":1}")).get("k").getClass());
        assertEquals(Long.class, JsonUtil.unwrap(new JSONObject("{\"k\":2147483648}")).get("k").getClass());
        assertEquals(BigInteger.class, JsonUtil.unwrap(new JSONObject("{\"k\":9223372036854775808}")).get("k").getClass());

        // A parsed decimal or exponent literal is a BigDecimal, NOT a Double.
        assertEquals(BigDecimal.class, JsonUtil.unwrap(new JSONObject("{\"score\":95.5}")).get("score").getClass());
        assertEquals(BigDecimal.class, JsonUtil.unwrap(new JSONObject("{\"k\":1e10}")).get("k").getClass());
        assertEquals(BigDecimal.class, JsonUtil.unwrap(new JSONArray("[1.5]")).get(0).getClass());
        assertEquals(BigDecimal.class, ((Map<?, ?>) JsonUtil.unwrap(new JSONObject("{\"a\":{\"b\":2.5}}")).get("a")).get("b").getClass());

        // ... except for the literals BigDecimal itself rejects, which org.json falls back to Double for: a
        // negative literal whose value is zero, and a negative exponent past BigDecimal's int scale. Both are
        // valid RFC-8259 JSON (the grammar puts no bound on the exponent), so a (BigDecimal) cast would break.
        assertEquals(Double.class, JsonUtil.unwrap(new JSONObject("{\"k\":-0}")).get("k").getClass());
        assertEquals(Double.class, JsonUtil.unwrap(new JSONObject("{\"k\":-0.0}")).get("k").getClass());
        assertEquals(Double.class, JsonUtil.unwrap(new JSONArray("[-0e5]")).get(0).getClass());
        assertEquals(Double.class, JsonUtil.unwrap(new JSONObject("{\"k\":1e-2147483648}")).get("k").getClass());
        assertEquals(Double.class, JsonUtil.unwrap(new JSONArray("[1e-2147483648]")).get(0).getClass());
        // the last in-range exponent is still a BigDecimal, so the boundary is BigDecimal's scale, not org.json's
        assertEquals(BigDecimal.class, JsonUtil.unwrap(new JSONObject("{\"k\":1e-2147483647}")).get("k").getClass());

        // A number put in programmatically is returned unchanged, keeping the caller's box type.
        assertEquals(Double.class, JsonUtil.unwrap(JsonUtil.wrap(Map.of("score", 95.5d))).get("score").getClass());
        assertEquals(Float.class, JsonUtil.unwrap(new JSONObject().put("score", 95.5f)).get("score").getClass());

        // An explicit target type is how a caller asks for a specific box type.
        assertEquals(Double.class, JsonUtil.toList(new JSONArray("[1.5]"), Double.class).get(0).getClass());

        // ... but a RAW Map class is not an explicit target type for the values: its value type is Object, so the
        // box types survive untouched. Only a parameterised map type, or a bean property, re-boxes.
        final JSONObject scored = new JSONObject("{\"score\":95.5}");
        assertEquals(BigDecimal.class, JsonUtil.unwrap(scored, Map.class).get("score").getClass());
        assertEquals(BigDecimal.class, JsonUtil.unwrap(scored, HashMap.class).get("score").getClass());
        assertEquals(Double.class, JsonUtil.unwrap(scored, Type.<Map<String, Double>> of("Map<String, Double>")).get("score").getClass());
    }
}

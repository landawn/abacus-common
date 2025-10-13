package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

@Tag("2025")
public class Properties2025Test extends TestBase {

    @Test
    public void testDefaultConstructor() {
        Properties<String, Object> props = new Properties<>();
        assertTrue(props.isEmpty());
        assertEquals(0, props.size());
    }

    @Test
    public void testCreate() {
        Map<String, Object> map = new HashMap<>();
        map.put("name", "John");
        map.put("age", 30);

        Properties<String, Object> props = Properties.create(map);
        assertEquals(2, props.size());
        assertEquals("John", props.get("name"));
        assertEquals(30, props.get("age"));
    }

    @Test
    public void testCreateWithEmptyMap() {
        Map<String, Object> map = new HashMap<>();
        Properties<String, Object> props = Properties.create(map);
        assertTrue(props.isEmpty());
    }

    @Test
    public void testGet() {
        Properties<String, Object> props = new Properties<>();
        props.put("name", "John");

        assertEquals("John", props.get("name"));
        assertNull(props.get("missing"));
    }

    @Test
    public void testGetWithTargetType() {
        Properties<String, Object> props = new Properties<>();
        props.put("age", "25");
        props.put("active", true);
        props.put("count", 10);

        Integer age = props.get("age", Integer.class);
        assertEquals(25, age);

        Boolean active = props.get("active", Boolean.class);
        assertTrue(active);

        Integer count = props.get("count", Integer.class);
        assertEquals(10, count);

        Double missing = props.get("missing", Double.class);
        assertNull(missing);
    }

    @Test
    public void testGetWithTargetTypePrimitive() {
        Properties<String, Object> props = new Properties<>();
        props.put("number", 42);

        int num = props.get("number", int.class);
        assertEquals(42, num);

        int defaultVal = props.get("missing", int.class);
        assertEquals(0, defaultVal);
    }

    @Test
    public void testGetOrDefault() {
        Properties<String, String> props = new Properties<>();
        props.put("host", "localhost");

        String host = props.getOrDefault("host", "0.0.0.0");
        assertEquals("localhost", host);

        String port = props.getOrDefault("port", "8080");
        assertEquals("8080", port);
    }

    @Test
    public void testGetOrDefaultWithNullValue() {
        Properties<String, String> props = new Properties<>();
        props.put("key", null);

        String value = props.getOrDefault("key", "default");
        assertEquals("default", value);
    }

    @Test
    public void testGetOrDefaultWithTargetType() {
        Properties<String, Object> props = new Properties<>();
        props.put("timeout", "30");

        int timeout = props.getOrDefault("timeout", 60, Integer.class);
        assertEquals(30, timeout);

        boolean debug = props.getOrDefault("debug", false, Boolean.class);
        assertFalse(debug);
    }

    @Test
    public void testGetOrDefaultWithTargetTypeNullValue() {
        Properties<String, Object> props = new Properties<>();
        props.put("value", null);

        int result = props.getOrDefault("value", 100, Integer.class);
        assertEquals(100, result);
    }

    @Test
    public void testSet() {
        Properties<String, Object> props = new Properties<>();

        Properties<String, Object> result = props.set("host", "localhost").set("port", 8080).set("debug", true);

        assertEquals(props, result);
        assertEquals(3, props.size());
        assertEquals("localhost", props.get("host"));
        assertEquals(8080, props.get("port"));
        assertEquals(true, props.get("debug"));
    }

    @Test
    public void testPut() {
        Properties<String, Integer> props = new Properties<>();

        assertNull(props.put("count", 10));
        assertEquals(10, props.get("count"));

        Integer old = props.put("count", 20);
        assertEquals(10, old);
        assertEquals(20, props.get("count"));
    }

    @Test
    public void testPutAll() {
        Properties<String, Object> props = new Properties<>();
        props.put("a", 1);

        Map<String, Object> map = new HashMap<>();
        map.put("b", 2);
        map.put("c", 3);

        props.putAll(map);

        assertEquals(3, props.size());
        assertEquals(1, props.get("a"));
        assertEquals(2, props.get("b"));
        assertEquals(3, props.get("c"));
    }

    @Test
    public void testPutIfAbsent() {
        Properties<String, String> props = new Properties<>();
        props.put("name", "John");

        String v1 = props.putIfAbsent("name", "Jane");
        assertEquals("John", v1);
        assertEquals("John", props.get("name"));

        String v2 = props.putIfAbsent("age", "30");
        assertNull(v2);
        assertEquals("30", props.get("age"));
    }

    @Test
    public void testPutIfAbsentWithNullValue() {
        Properties<String, String> props = new Properties<>();
        props.put("key", null);

        String result = props.putIfAbsent("key", "value");
        assertNull(result);
        assertEquals("value", props.get("key"));
    }

    @Test
    public void testRemove() {
        Properties<String, Object> props = new Properties<>();
        props.put("temp", "value");

        Object removed = props.remove("temp");
        assertEquals("value", removed);
        assertNull(props.get("temp"));

        assertNull(props.remove("missing"));
    }

    @Test
    public void testRemoveWithValue() {
        Properties<String, String> props = new Properties<>();
        props.put("status", "active");

        boolean removed1 = props.remove("status", "inactive");
        assertFalse(removed1);
        assertEquals("active", props.get("status"));

        boolean removed2 = props.remove("status", "active");
        assertTrue(removed2);
        assertNull(props.get("status"));
    }

    @Test
    public void testRemoveWithValueNotPresent() {
        Properties<String, String> props = new Properties<>();

        boolean removed = props.remove("missing", "value");
        assertFalse(removed);
    }

    @Test
    public void testRemoveWithValueNullKey() {
        Properties<String, String> props = new Properties<>();
        props.put("key", null);

        boolean removed = props.remove("key", null);
        assertTrue(removed);
        assertFalse(props.containsKey("key"));
    }

    @Test
    public void testReplace() {
        Properties<String, Integer> props = new Properties<>();
        props.put("version", 1);

        Integer old = props.replace("version", 2);
        assertEquals(1, old);
        assertEquals(2, props.get("version"));

        Integer none = props.replace("missing", 3);
        assertNull(none);
        assertNull(props.get("missing"));
    }

    @Test
    public void testReplaceWithNullValue() {
        Properties<String, String> props = new Properties<>();
        props.put("key", null);

        String result = props.replace("key", "value");
        assertNull(result);
        assertEquals("value", props.get("key"));
    }

    @Test
    public void testReplaceWithOldValue() {
        Properties<String, String> props = new Properties<>();
        props.put("status", "draft");

        boolean replaced1 = props.replace("status", "published", "approved");
        assertFalse(replaced1);
        assertEquals("draft", props.get("status"));

        boolean replaced2 = props.replace("status", "draft", "published");
        assertTrue(replaced2);
        assertEquals("published", props.get("status"));
    }

    @Test
    public void testReplaceWithOldValueNull() {
        Properties<String, String> props = new Properties<>();
        props.put("key", null);

        boolean replaced = props.replace("key", null, "value");
        assertTrue(replaced);
        assertEquals("value", props.get("key"));
    }

    @Test
    public void testReplaceWithOldValueNotPresent() {
        Properties<String, String> props = new Properties<>();

        boolean replaced = props.replace("missing", "old", "new");
        assertFalse(replaced);
    }

    @Test
    public void testContainsKey() {
        Properties<String, Object> props = new Properties<>();
        props.put("name", "John");

        assertTrue(props.containsKey("name"));
        assertFalse(props.containsKey("age"));
    }

    @Test
    public void testContainsValue() {
        Properties<String, String> props = new Properties<>();
        props.put("host", "localhost");
        props.put("backup", "localhost");

        assertTrue(props.containsValue("localhost"));
        assertFalse(props.containsValue("127.0.0.1"));
    }

    @Test
    public void testKeySet() {
        Properties<String, Object> props = new Properties<>();
        props.put("name", "John");
        props.put("age", 30);

        Set<String> keys = props.keySet();
        assertEquals(2, keys.size());
        assertTrue(keys.contains("name"));
        assertTrue(keys.contains("age"));
    }

    @Test
    public void testValues() {
        Properties<String, Integer> props = new Properties<>();
        props.put("a", 1);
        props.put("b", 2);

        Collection<Integer> values = props.values();
        assertEquals(2, values.size());
        assertTrue(values.contains(1));
        assertTrue(values.contains(2));
    }

    @Test
    public void testEntrySet() {
        Properties<String, Object> props = new Properties<>();
        props.put("x", 10);
        props.put("y", 20);

        Set<Map.Entry<String, Object>> entries = props.entrySet();
        assertEquals(2, entries.size());

        boolean foundX = false;
        boolean foundY = false;
        for (Map.Entry<String, Object> entry : entries) {
            if ("x".equals(entry.getKey()) && Integer.valueOf(10).equals(entry.getValue())) {
                foundX = true;
            }
            if ("y".equals(entry.getKey()) && Integer.valueOf(20).equals(entry.getValue())) {
                foundY = true;
            }
        }
        assertTrue(foundX);
        assertTrue(foundY);
    }

    @Test
    public void testIsEmpty() {
        Properties<String, Object> props = new Properties<>();
        assertTrue(props.isEmpty());

        props.put("key", "value");
        assertFalse(props.isEmpty());

        props.clear();
        assertTrue(props.isEmpty());
    }

    @Test
    public void testSize() {
        Properties<String, Object> props = new Properties<>();
        assertEquals(0, props.size());

        props.put("a", 1);
        assertEquals(1, props.size());

        props.put("b", 2);
        assertEquals(2, props.size());

        props.remove("a");
        assertEquals(1, props.size());
    }

    @Test
    public void testClear() {
        Properties<String, Object> props = new Properties<>();
        props.put("a", 1);
        props.put("b", 2);

        assertEquals(2, props.size());

        props.clear();
        assertTrue(props.isEmpty());
        assertEquals(0, props.size());
    }

    @Test
    public void testCopy() {
        Properties<String, Object> original = new Properties<>();
        original.put("key", "value");
        original.put("num", 42);

        Properties<String, Object> copy = original.copy();

        assertEquals(original.size(), copy.size());
        assertEquals(original.get("key"), copy.get("key"));
        assertEquals(original.get("num"), copy.get("num"));

        copy.put("key2", "value2");
        assertFalse(original.containsKey("key2"));
    }

    @Test
    public void testHashCode() {
        Properties<String, Object> props1 = new Properties<>();
        props1.put("key", "value");

        Properties<String, Object> props2 = new Properties<>();
        props2.put("key", "value");

        assertEquals(props1.hashCode(), props2.hashCode());
    }

    @Test
    public void testHashCodeDifferent() {
        Properties<String, Object> props1 = new Properties<>();
        props1.put("key1", "value");

        Properties<String, Object> props2 = new Properties<>();
        props2.put("key2", "value");

        assertNotEquals(props1.hashCode(), props2.hashCode());
    }

    @Test
    public void testEquals() {
        Properties<String, Object> props1 = new Properties<>();
        props1.put("key", "value");

        Properties<String, Object> props2 = new Properties<>();
        props2.put("key", "value");

        assertEquals(props1, props2);
        assertEquals(props2, props1);
    }

    @Test
    public void testEqualsSameInstance() {
        Properties<String, Object> props = new Properties<>();
        props.put("key", "value");

        assertEquals(props, props);
    }

    @Test
    public void testEqualsNotEqual() {
        Properties<String, Object> props1 = new Properties<>();
        props1.put("key1", "value");

        Properties<String, Object> props2 = new Properties<>();
        props2.put("key2", "value");

        assertNotEquals(props1, props2);
    }

    @Test
    public void testEqualsNull() {
        Properties<String, Object> props = new Properties<>();
        assertNotEquals(props, null);
    }

    @Test
    public void testEqualsDifferentType() {
        Properties<String, Object> props = new Properties<>();
        assertNotEquals(props, new HashMap<>());
    }

    @Test
    public void testToString() {
        Properties<String, Object> props = new Properties<>();
        props.put("name", "John");
        props.put("age", 30);

        String str = props.toString();
        assertTrue(str.contains("name"));
        assertTrue(str.contains("John"));
        assertTrue(str.contains("age"));
        assertTrue(str.contains("30"));
    }

    @Test
    public void testToStringEmpty() {
        Properties<String, Object> props = new Properties<>();
        String str = props.toString();
        assertEquals("{}", str);
    }

    @Test
    public void testInsertionOrder() {
        Properties<String, Integer> props = new Properties<>();
        props.put("first", 1);
        props.put("second", 2);
        props.put("third", 3);

        Set<String> keys = props.keySet();
        String[] keyArray = keys.toArray(new String[0]);

        assertEquals("first", keyArray[0]);
        assertEquals("second", keyArray[1]);
        assertEquals("third", keyArray[2]);
    }

    @Test
    public void testComplexScenario() {
        Properties<String, Object> props = new Properties<>();

        props.set("host", "localhost").set("port", 8080).set("timeout", 30);

        String host = props.get("host", String.class);
        int port = props.get("port", Integer.class);
        int timeout = props.get("timeout", Integer.class);

        assertEquals("localhost", host);
        assertEquals(8080, port);
        assertEquals(30, timeout);

        props.replace("port", 9090);
        assertEquals(9090, props.get("port"));

        props.remove("timeout");
        assertFalse(props.containsKey("timeout"));

        Properties<String, Object> copy = props.copy();
        assertEquals(props, copy);
    }

    @Test
    public void testNullHandling() {
        Properties<String, String> props = new Properties<>();
        props.put("nullKey", null);

        assertTrue(props.containsKey("nullKey"));
        assertNull(props.get("nullKey"));

        String defaultValue = props.getOrDefault("nullKey", "default");
        assertEquals("default", defaultValue);
    }

    @Test
    public void testGetWithTypeConversion() {
        Properties<String, Object> props = new Properties<>();
        props.put("stringNum", "123");
        props.put("intNum", 456);
        props.put("boolTrue", "true");
        props.put("boolFalse", false);

        int num1 = props.get("stringNum", Integer.class);
        assertEquals(123, num1);

        int num2 = props.get("intNum", Integer.class);
        assertEquals(456, num2);

        boolean bool1 = props.get("boolTrue", Boolean.class);
        assertTrue(bool1);

        boolean bool2 = props.get("boolFalse", Boolean.class);
        assertFalse(bool2);
    }
}

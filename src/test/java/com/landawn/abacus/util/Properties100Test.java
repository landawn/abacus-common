package com.landawn.abacus.util;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import com.landawn.abacus.TestBase;

@Tag("new-test")
public class Properties100Test extends TestBase {

    @Test
    public void testConstructor() {
        Properties<String, Object> props = new Properties<>();
        Assertions.assertNotNull(props);
        Assertions.assertTrue(props.isEmpty());
    }

    @Test
    public void testCreate() {
        Map<String, Object> map = new HashMap<>();
        map.put("key1", "value1");
        map.put("key2", 123);

        Properties<String, Object> props = Properties.create(map);
        Assertions.assertEquals("value1", props.get("key1"));
        Assertions.assertEquals(123, props.get("key2"));
    }

    @Test
    public void testGet() {
        Properties<String, Object> props = new Properties<>();
        props.put("key1", "value1");

        Assertions.assertEquals("value1", props.get("key1"));
        Assertions.assertNull(props.get("nonexistent"));
    }

    @Test
    public void testGetWithTargetType() {
        Properties<String, Object> props = new Properties<>();
        props.put("number", "123");
        props.put("bool", "true");

        Integer num = props.get("number", Integer.class);
        Boolean bool = props.get("bool", Boolean.class);

        Assertions.assertEquals(123, num);
        Assertions.assertTrue(bool);
    }

    @Test
    public void testGetOrDefault() {
        Properties<String, Object> props = new Properties<>();
        props.put("key1", "value1");

        Assertions.assertEquals("value1", props.getOrDefault("key1", "default"));
        Assertions.assertEquals("default", props.getOrDefault("nonexistent", "default"));
    }

    @Test
    public void testGetOrDefaultWithTargetType() {
        Properties<String, Object> props = new Properties<>();
        props.put("number", "123");

        Integer num = props.getOrDefault("number", 0, Integer.class);
        Integer defaultNum = props.getOrDefault("nonexistent", 999, Integer.class);

        Assertions.assertEquals(123, num);
        Assertions.assertEquals(999, defaultNum);
    }

    @Test
    public void testSet() {
        Properties<String, Object> props = new Properties<>();
        Properties<String, Object> result = props.set("key1", "value1");

        Assertions.assertSame(props, result);
        Assertions.assertEquals("value1", props.get("key1"));
    }

    @Test
    public void testPut() {
        Properties<String, Object> props = new Properties<>();
        Object oldValue = props.put("key1", "value1");

        Assertions.assertNull(oldValue);
        Assertions.assertEquals("value1", props.get("key1"));

        oldValue = props.put("key1", "value2");
        Assertions.assertEquals("value1", oldValue);
        Assertions.assertEquals("value2", props.get("key1"));
    }

    @Test
    public void testPutAll() {
        Properties<String, Object> props = new Properties<>();
        Map<String, Object> map = new HashMap<>();
        map.put("key1", "value1");
        map.put("key2", "value2");

        props.putAll(map);

        Assertions.assertEquals("value1", props.get("key1"));
        Assertions.assertEquals("value2", props.get("key2"));
    }

    @Test
    public void testPutIfAbsent() {
        Properties<String, Object> props = new Properties<>();

        Object result = props.putIfAbsent("key1", "value1");
        Assertions.assertNull(result);

        result = props.putIfAbsent("key1", "value2");
        Assertions.assertEquals("value1", result);
        Assertions.assertEquals("value1", props.get("key1"));
    }

    @Test
    public void testRemove() {
        Properties<String, Object> props = new Properties<>();
        props.put("key1", "value1");

        Object removed = props.remove("key1");
        Assertions.assertEquals("value1", removed);
        Assertions.assertNull(props.get("key1"));
    }

    @Test
    public void testRemoveWithValue() {
        Properties<String, Object> props = new Properties<>();
        props.put("key1", "value1");

        boolean removed = props.remove("key1", "wrongValue");
        Assertions.assertFalse(removed);
        Assertions.assertEquals("value1", props.get("key1"));

        removed = props.remove("key1", "value1");
        Assertions.assertTrue(removed);
        Assertions.assertNull(props.get("key1"));
    }

    @Test
    public void testReplace() {
        Properties<String, Object> props = new Properties<>();
        props.put("key1", "value1");

        Object old = props.replace("key1", "value2");
        Assertions.assertEquals("value1", old);
        Assertions.assertEquals("value2", props.get("key1"));

        old = props.replace("nonexistent", "value");
        Assertions.assertNull(old);
    }

    @Test
    public void testReplaceWithOldValue() {
        Properties<String, Object> props = new Properties<>();
        props.put("key1", "value1");

        boolean replaced = props.replace("key1", "wrongValue", "value2");
        Assertions.assertFalse(replaced);
        Assertions.assertEquals("value1", props.get("key1"));

        replaced = props.replace("key1", "value1", "value2");
        Assertions.assertTrue(replaced);
        Assertions.assertEquals("value2", props.get("key1"));
    }

    @Test
    public void testContainsKey() {
        Properties<String, Object> props = new Properties<>();
        props.put("key1", "value1");

        Assertions.assertTrue(props.containsKey("key1"));
        Assertions.assertFalse(props.containsKey("nonexistent"));
    }

    @Test
    public void testContainsValue() {
        Properties<String, Object> props = new Properties<>();
        props.put("key1", "value1");

        Assertions.assertTrue(props.containsValue("value1"));
        Assertions.assertFalse(props.containsValue("nonexistent"));
    }

    @Test
    public void testKeySet() {
        Properties<String, Object> props = new Properties<>();
        props.put("key1", "value1");
        props.put("key2", "value2");

        Assertions.assertEquals(2, props.keySet().size());
        Assertions.assertTrue(props.keySet().contains("key1"));
        Assertions.assertTrue(props.keySet().contains("key2"));
    }

    @Test
    public void testValues() {
        Properties<String, Object> props = new Properties<>();
        props.put("key1", "value1");
        props.put("key2", "value2");

        Assertions.assertEquals(2, props.values().size());
        Assertions.assertTrue(props.values().contains("value1"));
        Assertions.assertTrue(props.values().contains("value2"));
    }

    @Test
    public void testEntrySet() {
        Properties<String, Object> props = new Properties<>();
        props.put("key1", "value1");

        Assertions.assertEquals(1, props.entrySet().size());
    }

    @Test
    public void testIsEmpty() {
        Properties<String, Object> props = new Properties<>();
        Assertions.assertTrue(props.isEmpty());

        props.put("key1", "value1");
        Assertions.assertFalse(props.isEmpty());
    }

    @Test
    public void testSize() {
        Properties<String, Object> props = new Properties<>();
        Assertions.assertEquals(0, props.size());

        props.put("key1", "value1");
        Assertions.assertEquals(1, props.size());
    }

    @Test
    public void testClear() {
        Properties<String, Object> props = new Properties<>();
        props.put("key1", "value1");
        props.put("key2", "value2");

        props.clear();
        Assertions.assertTrue(props.isEmpty());
    }

    @Test
    public void testCopy() {
        Properties<String, Object> props = new Properties<>();
        props.put("key1", "value1");

        Properties<String, Object> copy = props.copy();
        Assertions.assertNotSame(props, copy);
        Assertions.assertEquals(props.get("key1"), copy.get("key1"));
    }

    @Test
    public void testHashCode() {
        Properties<String, Object> props1 = new Properties<>();
        Properties<String, Object> props2 = new Properties<>();

        props1.put("key1", "value1");
        props2.put("key1", "value1");

        Assertions.assertEquals(props1.hashCode(), props2.hashCode());
    }

    @Test
    public void testEquals() {
        Properties<String, Object> props1 = new Properties<>();
        Properties<String, Object> props2 = new Properties<>();

        props1.put("key1", "value1");
        props2.put("key1", "value1");

        Assertions.assertEquals(props1, props2);
        Assertions.assertNotEquals(props1, new Properties<>());
    }

    @Test
    public void testToString() {
        Properties<String, Object> props = new Properties<>();
        props.put("key1", "value1");

        String str = props.toString();
        Assertions.assertTrue(str.contains("key1"));
        Assertions.assertTrue(str.contains("value1"));
    }
}

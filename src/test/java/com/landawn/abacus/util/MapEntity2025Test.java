package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

@Tag("2025")
public class MapEntity2025Test extends TestBase {
    @Test
    public void testSet_simpleName() {
        MapEntity entity = new MapEntity("User");
        MapEntity result = entity.set("name", "John");

        assertNotNull(result);
        assertEquals(entity, result);
        assertEquals("John", entity.get("name"));
    }

    @Test
    public void testSet_canonicalName() {
        MapEntity entity = new MapEntity("User");
        entity.set("User.email", "john@example.com");

        assertEquals("john@example.com", entity.get("email"));
    }

    @Test
    public void testSet_methodChaining() {
        MapEntity entity = new MapEntity("User");
        entity.set("name", "John").set("age", 30).set("email", "john@example.com");

        assertEquals("John", entity.get("name"));
        assertEquals(Integer.valueOf(30), entity.get("age"));
        assertEquals("john@example.com", entity.get("email"));
    }

    @Test
    public void testSet_overwrite() {
        MapEntity entity = new MapEntity("User");
        entity.set("name", "John");
        entity.set("name", "Jane");

        assertEquals("Jane", entity.get("name"));
    }

    @Test
    public void testSet_map() {
        Map<String, Object> props = new HashMap<>();
        props.put("name", "John");
        props.put("age", 30);

        MapEntity entity = new MapEntity("User");
        entity.set(props);

        assertEquals("John", entity.get("name"));
        assertEquals(Integer.valueOf(30), entity.get("age"));
    }

    @Test
    public void testRemove_simpleName() {
        MapEntity entity = new MapEntity("User");
        entity.set("tempData", "temp");

        Object removed = entity.remove("tempData");
        assertEquals("temp", removed);
        assertNull(entity.get("tempData"));
    }

    @Test
    public void testRemove_canonicalName() {
        MapEntity entity = new MapEntity("User");
        entity.set("data", "value");

        Object removed = entity.remove("User.data");
        assertEquals("value", removed);
    }

    @Test
    public void testRemove_nonExistent() {
        MapEntity entity = new MapEntity("User");
        Object removed = entity.remove("nonExistent");
        assertNull(removed);
    }

    @Test
    public void testRemove_emptyEntity() {
        MapEntity entity = new MapEntity("User");
        Object removed = entity.remove("anything");
        assertNull(removed);
    }

    @Test
    public void testRemoveAll() {
        MapEntity entity = new MapEntity("User");
        entity.set("temp1", "value1").set("temp2", "value2").set("keep", "keepValue");

        entity.removeAll(Arrays.asList("temp1", "temp2"));

        assertNull(entity.get("temp1"));
        assertNull(entity.get("temp2"));
        assertEquals("keepValue", entity.get("keep"));
    }

    @Test
    public void testContainsKey_simpleName() {
        MapEntity entity = new MapEntity("User");
        entity.set("email", "john@example.com");

        assertTrue(entity.containsKey("email"));
        assertFalse(entity.containsKey("phone"));
    }

    @Test
    public void testContainsKey_canonicalName() {
        MapEntity entity = new MapEntity("User");
        entity.set("email", "john@example.com");

        assertTrue(entity.containsKey("User.email"));
    }

    @Test
    public void testContainsKey_emptyEntity() {
        MapEntity entity = new MapEntity("User");
        assertFalse(entity.containsKey("anything"));
    }

    @Test
    public void testKeySet() {
        MapEntity entity = new MapEntity("User");
        entity.set("name", "John").set("age", 30);

        Set<String> keys = entity.keySet();
        assertEquals(2, keys.size());
        assertTrue(keys.contains("name"));
        assertTrue(keys.contains("age"));
    }

    @Test
    public void testEntrySet() {
        MapEntity entity = new MapEntity("User");
        entity.set("name", "John").set("age", 30);

        Set<Map.Entry<String, Object>> entries = entity.entrySet();
        assertEquals(2, entries.size());
    }

    @Test
    public void testProps() {
        MapEntity entity = new MapEntity("User");
        entity.set("name", "John");

        Map<String, Object> props = entity.props();
        assertNotNull(props);
        assertEquals("John", props.get("name"));

        props.put("age", 30);
        assertEquals(Integer.valueOf(30), entity.get("age"));
    }

    @Test
    public void testSize() {
        MapEntity entity = new MapEntity("User");
        assertEquals(0, entity.size());

        entity.set("name", "John");
        assertEquals(1, entity.size());

        entity.set("age", 30);
        assertEquals(2, entity.size());
    }

    @Test
    public void testIsEmpty_true() {
        MapEntity entity = new MapEntity("User");
        assertTrue(entity.isEmpty());
    }

    @Test
    public void testIsEmpty_false() {
        MapEntity entity = new MapEntity("User");
        entity.set("name", "John");
        assertFalse(entity.isEmpty());
    }

    @Test
    public void testCopy() {
        MapEntity original = new MapEntity("User");
        original.set("name", "John").set("age", 30);

        MapEntity copy = original.copy();

        assertEquals(original.entityName(), copy.entityName());
        assertEquals((String) original.get("name"), (String) copy.get("name"));
        assertEquals((Object) original.get("age"), (Object) copy.get("age"));

        copy.set("age", 31);
        assertEquals(Integer.valueOf(30), original.get("age"));
        assertEquals(Integer.valueOf(31), copy.get("age"));
    }

    @Test
    public void testHashCode_consistent() {
        MapEntity entity = new MapEntity("User");
        entity.set("name", "John");

        int hash1 = entity.hashCode();
        int hash2 = entity.hashCode();
        assertEquals(hash1, hash2);
    }

    @Test
    public void testHashCode_equal() {
        MapEntity entity1 = new MapEntity("User");
        entity1.set("name", "John");

        MapEntity entity2 = new MapEntity("User");
        entity2.set("name", "John");

        assertEquals(entity1.hashCode(), entity2.hashCode());
    }

    @Test
    public void testEquals_same() {
        MapEntity entity = new MapEntity("User");
        assertEquals(entity, entity);
    }

    @Test
    public void testEquals_equal() {
        MapEntity entity1 = new MapEntity("User");
        entity1.set("name", "John");

        MapEntity entity2 = new MapEntity("User");
        entity2.set("name", "John");

        assertEquals(entity1, entity2);
    }

    @Test
    public void testEquals_differentEntityName() {
        MapEntity entity1 = new MapEntity("User");
        entity1.set("name", "John");

        MapEntity entity2 = new MapEntity("Person");
        entity2.set("name", "John");

        assertNotEquals(entity1, entity2);
    }

    @Test
    public void testEquals_differentValues() {
        MapEntity entity1 = new MapEntity("User");
        entity1.set("name", "John");

        MapEntity entity2 = new MapEntity("User");
        entity2.set("name", "Jane");

        assertNotEquals((Object) entity1, (Object) entity2);
    }

    @Test
    public void testEquals_null() {
        MapEntity entity = new MapEntity("User");
        assertNotEquals(entity, null);
    }

    @Test
    public void testEquals_differentType() {
        MapEntity entity = new MapEntity("User");
        assertNotEquals(entity, "User");
    }

    @Test
    public void testToString() {
        MapEntity entity = new MapEntity("User");
        entity.set("name", "John");

        String str = entity.toString();
        assertNotNull(str);
        assertTrue(str.contains("name"));
        assertTrue(str.contains("John"));
    }

    @Test
    public void testBuilder() {
        MapEntity entity = MapEntity.builder("User").put("name", "John").put("age", 30).put("email", "john@example.com").build();

        assertEquals("User", entity.entityName());
        assertEquals("John", entity.get("name"));
        assertEquals(Integer.valueOf(30), entity.get("age"));
        assertEquals("john@example.com", entity.get("email"));
    }

    @Test
    public void testBuilder_empty() {
        MapEntity entity = MapEntity.builder("Product").build();

        assertEquals("Product", entity.entityName());
        assertTrue(entity.isEmpty());
    }

    @Test
    public void testIntegration_complexScenario() {
        MapEntity user = new MapEntity("User");
        user.set("id", 1).set("name", "John Doe").set("email", "john@example.com").set("age", 30);

        assertEquals(4, user.size());
        assertTrue(user.containsKey("id"));

        user.remove("email");
        assertFalse(user.containsKey("email"));
        assertEquals(3, user.size());

        MapEntity copy = user.copy();
        assertEquals(user, copy);

        copy.set("name", "Jane Doe");
        assertNotEquals((Object) user.get("name"), (Object) copy.get("name"));
    }
}

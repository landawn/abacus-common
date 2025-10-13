package com.landawn.abacus.util;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import com.landawn.abacus.TestBase;

@Tag("new-test")
public class MapEntity100Test extends TestBase {

    @Test
    public void testConstructorWithName() {
        MapEntity entity = new MapEntity("User");
        Assertions.assertEquals("User", entity.entityName());
    }

    @Test
    public void testConstructorWithNameAndProps() {
        Map<String, Object> props = new HashMap<>();
        props.put("name", "John");
        props.put("age", 30);

        MapEntity entity = new MapEntity("User", props);
        Assertions.assertEquals("User", entity.entityName());
        Assertions.assertEquals("John", entity.get("name"));
        Assertions.assertEquals(30, (Integer) entity.get("age"));
    }

    @Test
    public void testEntityName() {
        MapEntity entity = new MapEntity("Product");
        Assertions.assertEquals("Product", entity.entityName());
    }

    @Test
    public void testGet() {
        MapEntity entity = new MapEntity("User");
        entity.set("name", "John");

        Assertions.assertEquals("John", entity.get("name"));
        Assertions.assertNull(entity.get("nonexistent"));
    }

    @Test
    public void testGetWithCanonicalName() {
        MapEntity entity = new MapEntity("User");
        entity.set("name", "John");

        Assertions.assertEquals("John", entity.get("User.name"));
    }

    @Test
    public void testGetWithTargetType() {
        MapEntity entity = new MapEntity("User");
        entity.set("age", "25");
        entity.set("active", "true");

        Integer age = entity.get("age", Integer.class);
        Boolean active = entity.get("active", Boolean.class);

        Assertions.assertEquals(25, age);
        Assertions.assertTrue(active);
    }

    @Test
    public void testSet() {
        MapEntity entity = new MapEntity("User");
        MapEntity result = entity.set("name", "John");

        Assertions.assertSame(entity, result);
        Assertions.assertEquals("John", entity.get("name"));
    }

    @Test
    public void testSetWithCanonicalName() {
        MapEntity entity = new MapEntity("User");
        entity.set("User.name", "John");

        Assertions.assertEquals("John", entity.get("name"));
    }

    @Test
    public void testSetMap() {
        MapEntity entity = new MapEntity("User");
        Map<String, Object> props = new HashMap<>();
        props.put("name", "John");
        props.put("age", 30);

        entity.set(props);

        Assertions.assertEquals("John", entity.get("name"));
        Assertions.assertEquals(30, (Integer) entity.get("age"));
    }

    @Test
    public void testRemove() {
        MapEntity entity = new MapEntity("User");
        entity.set("name", "John");

        Object removed = entity.remove("name");
        Assertions.assertEquals("John", removed);
        Assertions.assertNull(entity.get("name"));
    }

    @Test
    public void testRemoveWithCanonicalName() {
        MapEntity entity = new MapEntity("User");
        entity.set("name", "John");

        Object removed = entity.remove("User.name");
        Assertions.assertEquals("John", removed);
        Assertions.assertNull(entity.get("name"));
    }

    @Test
    public void testRemoveAll() {
        MapEntity entity = new MapEntity("User");
        entity.set("name", "John");
        entity.set("age", 30);
        entity.set("email", "john@example.com");

        entity.removeAll(Arrays.asList("name", "age"));

        Assertions.assertNull(entity.get("name"));
        Assertions.assertNull(entity.get("age"));
        Assertions.assertEquals("john@example.com", entity.get("email"));
    }

    @Test
    public void testContainsKey() {
        MapEntity entity = new MapEntity("User");
        entity.set("name", "John");

        Assertions.assertTrue(entity.containsKey("name"));
        Assertions.assertFalse(entity.containsKey("age"));
    }

    @Test
    public void testContainsKeyWithCanonicalName() {
        MapEntity entity = new MapEntity("User");
        entity.set("name", "John");

        Assertions.assertTrue(entity.containsKey("User.name"));
    }

    @Test
    public void testKeySet() {
        MapEntity entity = new MapEntity("User");
        entity.set("name", "John");
        entity.set("age", 30);

        Assertions.assertEquals(2, entity.keySet().size());
        Assertions.assertTrue(entity.keySet().contains("name"));
        Assertions.assertTrue(entity.keySet().contains("age"));
    }

    @Test
    public void testEntrySet() {
        MapEntity entity = new MapEntity("User");
        entity.set("name", "John");

        Assertions.assertEquals(1, entity.entrySet().size());
    }

    @Test
    public void testProps() {
        MapEntity entity = new MapEntity("User");
        entity.set("name", "John");

        Map<String, Object> props = entity.props();
        Assertions.assertEquals(1, props.size());
        Assertions.assertEquals("John", props.get("name"));
    }

    @Test
    public void testSize() {
        MapEntity entity = new MapEntity("User");
        Assertions.assertEquals(0, entity.size());

        entity.set("name", "John");
        Assertions.assertEquals(1, entity.size());
    }

    @Test
    public void testIsEmpty() {
        MapEntity entity = new MapEntity("User");
        Assertions.assertTrue(entity.isEmpty());

        entity.set("name", "John");
        Assertions.assertFalse(entity.isEmpty());
    }

    @Test
    public void testCopy() {
        MapEntity entity = new MapEntity("User");
        entity.set("name", "John");
        entity.set("age", 30);

        MapEntity copy = entity.copy();

        Assertions.assertNotSame(entity, copy);
        Assertions.assertEquals(entity.entityName(), copy.entityName());
        Assertions.assertEquals((String) entity.get("name"), (String) copy.get("name"));
        Assertions.assertEquals((Integer) entity.get("age"), (Integer) copy.get("age"));
    }

    @Test
    public void testHashCode() {
        MapEntity entity1 = new MapEntity("User");
        entity1.set("name", "John");

        MapEntity entity2 = new MapEntity("User");
        entity2.set("name", "John");

        Assertions.assertEquals(entity1.hashCode(), entity2.hashCode());
    }

    @Test
    public void testEquals() {
        MapEntity entity1 = new MapEntity("User");
        entity1.set("name", "John");

        MapEntity entity2 = new MapEntity("User");
        entity2.set("name", "John");

        Assertions.assertEquals(entity1, entity2);

        entity2.set("age", 30);
        Assertions.assertNotEquals(entity1, entity2);
    }

    @Test
    public void testToString() {
        MapEntity entity = new MapEntity("User");
        entity.set("name", "John");

        String str = entity.toString();
        Assertions.assertTrue(str.contains("name=John"));
    }

    @Test
    public void testBuilder() {
        MapEntity entity = MapEntity.builder("User").put("name", "John").put("age", 30).build();

        Assertions.assertEquals("User", entity.entityName());
        Assertions.assertEquals("John", entity.get("name"));
        Assertions.assertEquals(30, (Integer) entity.get("age"));
    }
}

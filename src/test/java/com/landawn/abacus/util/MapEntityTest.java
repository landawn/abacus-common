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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

public class MapEntityTest extends TestBase {

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
    public void testConstructorWithNullName() {
        MapEntity entity = new MapEntity(null);
        assertEquals("", entity.entityName());
    }

    @Test
    public void testEntityName() {
        MapEntity entity = new MapEntity("Product");
        Assertions.assertEquals("Product", entity.entityName());
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
    public void testGet() {
        MapEntity entity = new MapEntity("User");
        entity.set("name", "John");

        Assertions.assertEquals("John", entity.get("name"));
        Assertions.assertNull(entity.get("nonexistent"));
    }

    @Test
    public void testGetWithTargetType_nullValueReturnsDefault() {
        MapEntity entity = new MapEntity("User");

        int intDefault = entity.get("missing", int.class);
        assertEquals(0, intDefault);

        boolean boolDefault = entity.get("missing", boolean.class);
        assertFalse(boolDefault);
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
    public void testSet_simpleName() {
        MapEntity entity = new MapEntity("User");
        MapEntity result = entity.set("name", "John");

        assertNotNull(result);
        assertEquals(entity, result);
        assertEquals("John", entity.get("name"));
    }

    @Test
    public void testSet() {
        MapEntity entity = new MapEntity("User");
        MapEntity result = entity.set("name", "John");

        Assertions.assertSame(entity, result);
        Assertions.assertEquals("John", entity.get("name"));
    }

    @Test
    public void testRemove_canonicalName() {
        MapEntity entity = new MapEntity("User");
        entity.set("data", "value");

        Object removed = entity.remove("User.data");
        assertEquals("value", removed);
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
        entity.set("temp1", "value1").set("temp2", "value2").set("keep", "keepValue");

        entity.removeAll(Arrays.asList("temp1", "temp2"));

        assertNull(entity.get("temp1"));
        assertNull(entity.get("temp2"));
        assertEquals("keepValue", entity.get("keep"));
    }

    @Test
    public void testRemoveAllAcceptsLiveKeySetView() {
        MapEntity entity = new MapEntity("User");
        entity.set("a", 1).set("b", 2).set("c", 3);

        entity.removeAll(entity.keySet());

        assertTrue(entity.isEmpty());
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
    public void testIsEmpty() {
        MapEntity entity = new MapEntity("User");
        Assertions.assertTrue(entity.isEmpty());

        entity.set("name", "John");
        Assertions.assertFalse(entity.isEmpty());
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

        assertNotEquals(entity1, entity2);
    }

    @Test
    public void testEquals_differentType() {
        MapEntity entity = new MapEntity("User");
        assertNotEquals(entity, "User");
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
    public void testEquals_same() {
        MapEntity entity = new MapEntity("User");
        assertEquals(entity, entity);
    }

    @Test
    public void testEquals_null() {
        MapEntity entity = new MapEntity("User");
        assertNotEquals(entity, null);
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

    @Test
    public void testNullPropNameRejectedWhetherOrNotTheEntityIsEmpty() {
        final MapEntity empty = new MapEntity("User");

        Assertions.assertThrows(IllegalArgumentException.class, () -> empty.containsKey(null));
        Assertions.assertThrows(IllegalArgumentException.class, () -> empty.remove(null));
        Assertions.assertThrows(IllegalArgumentException.class, () -> empty.get(null));
        Assertions.assertThrows(IllegalArgumentException.class, () -> empty.set(null, "x"));

        final MapEntity filled = new MapEntity("User").set("age", "25");

        Assertions.assertThrows(IllegalArgumentException.class, () -> filled.containsKey(null));
        Assertions.assertThrows(IllegalArgumentException.class, () -> filled.remove(null));
        Assertions.assertThrows(IllegalArgumentException.class, () -> filled.get(null));
        Assertions.assertThrows(IllegalArgumentException.class, () -> filled.set(null, "x"));

        // A non-null name on an empty entity still answers rather than throwing.
        assertFalse(empty.containsKey("age"));
        assertNull(empty.remove("age"));
        assertFalse(empty.containsKey("User.age"));
        assertNull(empty.remove("User.age"));
        assertNull(empty.get("age"));
        assertEquals(0, empty.size());

        // ... and the canonical-name handling is unchanged on a non-empty entity.
        assertTrue(filled.containsKey("User.age"));
        assertEquals("25", filled.remove("User.age"));
        assertFalse(filled.containsKey("age"));

        // the four container-taking entry points funnel a property name into the same NameUtil check, so a null
        // element/key raises the same IllegalArgumentException. removeAll on an EMPTY entity reached remove(String)'s
        // isEmpty() short-circuit and used to return quietly.
        final Map<String, Object> nullKeyed = new HashMap<>();
        nullKeyed.put(null, 1);

        Assertions.assertThrows(IllegalArgumentException.class, () -> new MapEntity("User", nullKeyed));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new MapEntity("User").set(nullKeyed));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new MapEntity("User").removeAll(Arrays.asList((String) null)));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new MapEntity("User").set("age", "25").removeAll(Arrays.asList((String) null)));
        Assertions.assertThrows(IllegalArgumentException.class, () -> MapEntity.builder("User").put(null, 1));
    }

    @Test
    public void testGetWithTargetTypeFailureModes() {
        final MapEntity entity = new MapEntity("U").set("k", "abc");

        Assertions.assertThrows(NumberFormatException.class, () -> entity.get("k", Integer.class));
        Assertions.assertThrows(IllegalArgumentException.class, () -> entity.get("k", null));
        // the targetType == null clause holds on the absent-property branch too
        Assertions.assertThrows(IllegalArgumentException.class, () -> new MapEntity("U").get("absent", null));

        // The conversion failure is whatever N.convert raises, which is why the tag is @throws RuntimeException:
        // DateTimeParseException, UnsupportedOperationException, ParsingException and ArithmeticException are all
        // outside the IllegalArgumentException family, so neither tag the pass first wrote covered them.
        Assertions.assertThrows(java.time.format.DateTimeParseException.class, () -> entity.get("k", java.time.LocalDate.class));
        Assertions.assertThrows(UnsupportedOperationException.class, () -> entity.get("k", Number.class));
        Assertions.assertThrows(com.landawn.abacus.exception.ParsingException.class, () -> entity.get("k", Map.class));
        Assertions.assertThrows(com.landawn.abacus.exception.ParsingException.class, () -> entity.get("k", java.io.File.class));
        // an integral overflow is an ArithmeticException even though the stored value IS a String and the target IS numeric
        Assertions.assertThrows(ArithmeticException.class, () -> new MapEntity("U").set("k", "99999999999999999999").get("k", int.class));
        Assertions.assertThrows(ArithmeticException.class, () -> new MapEntity("U").set("k", Long.MAX_VALUE).get("k", Integer.class));

        for (final Class<?> target : new Class<?>[] { java.time.LocalDate.class, Number.class, Map.class, java.io.File.class }) {
            final RuntimeException failure = Assertions.assertThrows(RuntimeException.class, () -> entity.get("k", target));
            assertFalse(failure instanceof IllegalArgumentException, target.getName() + " -> " + failure.getClass().getName());
        }

        // ... while these targets really are IllegalArgumentException
        Assertions.assertThrows(IllegalArgumentException.class, () -> entity.get("k", java.util.UUID.class));
        Assertions.assertThrows(IllegalArgumentException.class, () -> entity.get("k", java.util.Date.class));
        Assertions.assertThrows(IllegalArgumentException.class, () -> entity.get("k", java.time.DayOfWeek.class));

        // An absent property yields the target type's default, which is null for reference types.
        assertEquals(0, (int) (Integer) new MapEntity("U").get("absent", int.class));
        assertNull(new MapEntity("U").get("absent", Integer.class));
        assertNull(new MapEntity("U").get("absent", String.class));
    }

}

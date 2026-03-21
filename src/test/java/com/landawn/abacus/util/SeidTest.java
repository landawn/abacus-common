package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

@SuppressWarnings("deprecation")
public class SeidTest extends TestBase {

    public static class SimpleUser {
        private int id;
        private String name;

        public SimpleUser(int id, String name) {
            this.id = id;
            this.name = name;
        }

        public int getId() {
            return id;
        }

        public void setId(int id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }

    public static class AnnotatedUser {
        @com.landawn.abacus.annotation.Id
        private int id;
        private String name;

        public AnnotatedUser(int id, String name) {
            this.id = id;
            this.name = name;
        }

        public int getId() {
            return id;
        }

        public void setId(int id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }

    // ==================== constructor(String propName, Object propValue) ====================

    @Test
    public void testConstructor_propNameAndValue() {
        Seid seid = new Seid("Order.orderId", 456);
        assertEquals("Order", seid.entityName());
        assertEquals(456, (int) seid.get("orderId"));
    }

    // ==================== constructor(Map) ====================

    @Test
    public void testConstructor_map() {
        Map<String, Object> props = new LinkedHashMap<>();
        props.put("Order.orderId", 100);
        Seid seid = new Seid(props);
        assertEquals("Order", seid.entityName());
        assertEquals(100, (int) seid.get("orderId"));
    }

    // ==================== Seid(String entityName) constructor ====================

    @Test
    public void testConstructor_entityName() {
        Seid seid = new Seid("MyEntity");
        assertEquals("MyEntity", seid.entityName());
        assertTrue(seid.isEmpty());
    }

    @Test
    public void testConstructor_entityName_null() {
        Seid seid = new Seid((String) null);
        assertEquals("", seid.entityName());
    }

    // ==================== of(propName1, propValue1, propName2, propValue2) ====================

    @Test
    public void testOf_twoProps() {
        Seid seid = Seid.of("User.id", 123, "User.name", "John");
        assertEquals("User", seid.entityName());
        assertEquals(123, (int) seid.get("id"));
        assertEquals("John", seid.get("name"));
        assertEquals(2, seid.size());
    }

    // ==================== of(propName1-3, propValue1-3) ====================

    @Test
    public void testOf_threeProps() {
        Seid seid = Seid.of("User.id", 1, "User.name", "John", "User.age", 30);
        assertEquals("User", seid.entityName());
        assertEquals(1, (int) seid.get("id"));
        assertEquals("John", seid.get("name"));
        assertEquals(30, (int) seid.get("age"));
        assertEquals(3, seid.size());
    }

    // ==================== of(String entityName) ====================

    @Test
    public void testOf_entityName() {
        Seid seid = Seid.of("User");
        assertNotNull(seid);
        assertEquals("User", seid.entityName());
        assertTrue(seid.isEmpty());
        assertEquals(0, seid.size());
    }

    @Test
    public void testOf_entityName_null() {
        Seid seid = Seid.of((String) null);
        assertNotNull(seid);
        assertEquals("", seid.entityName());
    }

    @Test
    public void testOf_entityName_empty() {
        Seid seid = Seid.of("");
        assertNotNull(seid);
        assertEquals("", seid.entityName());
    }

    // ==================== of(String propName, Object propValue) ====================

    @Test
    public void testOf_singleProp() {
        Seid seid = Seid.of("User.id", 123);
        assertNotNull(seid);
        assertEquals("User", seid.entityName());
        assertEquals(123, (int) seid.get("id"));
        assertEquals(1, seid.size());
    }

    @Test
    public void testOf_singleProp_simpleName() {
        Seid seid = Seid.of("id", 123);
        assertNotNull(seid);
        assertEquals("", seid.entityName());
        assertEquals(123, (int) seid.get("id"));
    }

    @Test
    public void testOf_singleProp_nullValue() {
        Seid seid = Seid.of("User.id", null);
        assertNull(seid.get("id"));
        assertEquals(1, seid.size());
    }

    // ==================== create(Map) ====================

    @Test
    public void testCreate_map() {
        Map<String, Object> props = new LinkedHashMap<>();
        props.put("User.id", 123);
        props.put("User.version", 1);
        Seid seid = Seid.create(props);

        assertNotNull(seid);
        assertEquals("User", seid.entityName());
        assertEquals(123, (int) seid.get("id"));
        assertEquals(1, (int) seid.get("version"));
    }

    @Test
    public void testCreate_entity_withIdPropNames() {
        SimpleUser user = new SimpleUser(42, "Alice");
        Seid seid = Seid.create(user, java.util.Arrays.asList("id"));
        assertNotNull(seid);
        assertEquals("SimpleUser", seid.entityName());
        assertEquals(Integer.valueOf(42), seid.get("id"));
    }

    @Test
    public void testCreate_entity_withAnnotation() {
        AnnotatedUser user = new AnnotatedUser(99, "Charlie");
        Seid seid = Seid.create(user);
        assertNotNull(seid);
        assertEquals(Integer.valueOf(99), seid.get("id"));
    }

    @Test
    public void testCreate_map_empty() {
        assertThrows(IllegalArgumentException.class, () -> Seid.create(new HashMap<>()));
    }

    @Test
    public void testCreate_entity_withIdPropNames_empty() {
        SimpleUser user = new SimpleUser(1, "Bob");
        assertThrows(IllegalArgumentException.class, () -> Seid.create(user, java.util.Collections.emptyList()));
    }

    // ==================== entityName() ====================

    @Test
    public void testEntityName() {
        Seid seid = Seid.of("User.id", 123);
        assertEquals("User", seid.entityName());
    }

    @Test
    public void testEntityName_noParent() {
        Seid seid = Seid.of("id", 123);
        assertEquals("", seid.entityName());
    }

    // ==================== get(String propName) ====================

    @Test
    public void testGet() {
        Seid seid = Seid.of("User.id", 123);
        assertEquals(123, (int) seid.get("id"));
    }

    @Test
    public void testGet_canonicalName() {
        Seid seid = Seid.of("User.id", 123);
        assertEquals(123, (int) seid.get("User.id"));
    }

    // ==================== get(String propName, Class targetType) ====================

    @Test
    public void testGet_withTargetType() {
        Seid seid = Seid.of("User.active", "true");
        Boolean active = seid.get("active", Boolean.class);
        assertEquals(Boolean.TRUE, active);
    }

    @Test
    public void testGet_withTargetType_intToLong() {
        Seid seid = Seid.of("User.id", 123);
        Long val = seid.get("id", Long.class);
        assertEquals(123L, val);
    }

    @Test
    public void testGet_nonexistent() {
        Seid seid = Seid.of("User.id", 123);
        assertNull(seid.get("nonexistent"));
    }

    @Test
    public void testGet_withTargetType_nullValue() {
        Seid seid = Seid.of("User.id", null);
        int val = seid.get("id", int.class);
        assertEquals(0, val);
    }

    // ==================== getInt(String propName) ====================

    @Test
    public void testGetInt() {
        Seid seid = Seid.of("User.age", 25);
        assertEquals(25, seid.getInt("age"));
    }

    @Test
    public void testGetInt_fromLong() {
        Seid seid = Seid.of("User.age", 25L);
        assertEquals(25, seid.getInt("age"));
    }

    @Test
    public void testGetInt_fromString() {
        Seid seid = Seid.of("User.age", "30");
        assertEquals(30, seid.getInt("age"));
    }

    // ==================== getLong(String propName) ====================

    @Test
    public void testGetLong() {
        Seid seid = Seid.of("User.id", 100L);
        assertEquals(100L, seid.getLong("id"));
    }

    @Test
    public void testGetLong_fromInt() {
        Seid seid = Seid.of("User.id", 42);
        assertEquals(42L, seid.getLong("id"));
    }

    // ==================== set(String propName, Object propValue) ====================

    @Test
    public void testSet() {
        Seid seid = Seid.of("User");
        seid.set("id", 100);
        assertEquals(100, (int) seid.get("id"));
        assertEquals(1, seid.size());
    }

    @Test
    public void testSet_overwrite() {
        Seid seid = Seid.of("User.id", 100);
        seid.set("id", 200);
        assertEquals(200, (int) seid.get("id"));
        assertEquals(1, seid.size());
    }

    // ==================== set(Map) ====================

    @Test
    public void testSet_map() {
        Seid seid = Seid.of("User");
        Map<String, Object> props = new LinkedHashMap<>();
        props.put("id", 1);
        props.put("name", "Alice");
        seid.set(props);
        assertEquals(1, (int) seid.get("id"));
        assertEquals("Alice", seid.get("name"));
    }

    @Test
    public void testSet_multiple() {
        Seid seid = Seid.of("User.id", 100);
        seid.set("name", "John");
        seid.set("age", 30);
        assertEquals(100, (int) seid.get("id"));
        assertEquals("John", seid.get("name"));
        assertEquals(30, (int) seid.get("age"));
        assertEquals(3, seid.size());
    }

    @Test
    public void testSet_chaining() {
        Seid seid = Seid.of("User");
        Seid result = seid.set("id", 100);
        assertNotNull(result);
        assertEquals(seid, result);
    }

    @Test
    public void testSet_map_empty() {
        Seid seid = Seid.of("User.id", 1);
        seid.set(new HashMap<>());
        // should have no effect
        assertEquals(1, (int) seid.get("id"));
    }

    @Test
    public void testSet_map_singleEntry() {
        Seid seid = Seid.of("User");
        Map<String, Object> props = new HashMap<>();
        props.put("id", 42);
        seid.set(props);
        assertEquals(42, (int) seid.get("id"));
    }

    // ==================== containsKey(String propName) ====================

    @Test
    public void testContainsKey() {
        Seid seid = Seid.of("User.id", 123);
        assertTrue(seid.containsKey("id"));
        assertFalse(seid.containsKey("name"));
    }

    @Test
    public void testContainsKey_canonicalName() {
        Seid seid = Seid.of("User.id", 123);
        assertTrue(seid.containsKey("User.id"));
    }

    @Test
    public void testContainsKey_empty() {
        Seid seid = Seid.of("User");
        assertFalse(seid.containsKey("id"));
    }

    // ==================== keySet() ====================

    @Test
    public void testKeySet() {
        Seid seid = Seid.of("User.id", 123, "User.name", "John");
        Set<String> keys = seid.keySet();
        assertTrue(keys.contains("id"));
        assertTrue(keys.contains("name"));
        assertEquals(2, keys.size());
    }

    @Test
    public void testKeySet_empty() {
        Seid seid = Seid.of("User");
        assertTrue(seid.keySet().isEmpty());
    }

    // ==================== entrySet() ====================

    @Test
    public void testEntrySet() {
        Seid seid = Seid.of("User.id", 123);
        Set<Map.Entry<String, Object>> entries = seid.entrySet();
        assertEquals(1, entries.size());
        Map.Entry<String, Object> entry = entries.iterator().next();
        assertEquals("id", entry.getKey());
        assertEquals(123, entry.getValue());
    }

    // ==================== size() ====================

    @Test
    public void testSize() {
        Seid seid = Seid.of("User");
        assertEquals(0, seid.size());

        seid.set("id", 1);
        assertEquals(1, seid.size());
    }

    @Test
    public void testSize_multiple() {
        Seid seid = Seid.of("User.id", 1, "User.name", "John", "User.age", 30);
        assertEquals(3, seid.size());
    }

    // ==================== isEmpty() ====================

    @Test
    public void testIsEmpty() {
        Seid seid = Seid.of("User");
        assertTrue(seid.isEmpty());
    }

    @Test
    public void testIsEmpty_notEmpty() {
        Seid seid = Seid.of("User.id", 1);
        assertFalse(seid.isEmpty());
    }

    // ==================== clear() ====================

    @Test
    public void testClear() {
        Seid seid = Seid.of("User.id", 1, "User.name", "John");
        assertFalse(seid.isEmpty());

        seid.clear();
        assertTrue(seid.isEmpty());
        assertEquals(0, seid.size());
    }

    // ==================== copy() ====================

    @Test
    public void testCopy() {
        Seid original = Seid.of("User.id", 123, "User.name", "John");
        Seid copy = original.copy();

        assertEquals(original.entityName(), copy.entityName());
        assertEquals((Integer) original.get("id"), (Integer) copy.get("id"));
        assertEquals((String) original.get("name"), (String) copy.get("name"));
        assertEquals(original.size(), copy.size());
        assertEquals(original, copy);
    }

    @Test
    public void testCopy_independent() {
        Seid original = Seid.of("User.id", 123);
        Seid copy = original.copy();

        copy.set("id", 999);
        assertEquals(123, (int) original.get("id"));
        assertEquals(999, (int) copy.get("id"));
    }

    @Test
    public void testEquals_equal() {
        Seid seid1 = Seid.of("User.id", 123);
        Seid seid2 = Seid.of("User.id", 123);
        assertTrue(seid1.equals(seid2));
        assertTrue(seid2.equals(seid1));
    }

    @Test
    public void testEquals_differentValue() {
        Seid seid1 = Seid.of("User.id", 123);
        Seid seid2 = Seid.of("User.id", 456);
        assertFalse(seid1.equals(seid2));
    }

    @Test
    public void testEquals_differentProp() {
        Seid seid1 = Seid.of("User.id", 123);
        Seid seid2 = Seid.of("User.name", "John");
        assertFalse(seid1.equals(seid2));
    }

    @Test
    public void testEquals_differentType() {
        Seid seid = Seid.of("User.id", 123);
        assertFalse(seid.equals("not a seid"));
    }

    // ==================== equals(Object obj) ====================

    @Test
    public void testEquals_same() {
        Seid seid = Seid.of("User.id", 123);
        assertTrue(seid.equals(seid));
    }

    @Test
    public void testEquals_null() {
        Seid seid = Seid.of("User.id", 123);
        assertFalse(seid.equals(null));
    }

    // ==================== hashCode() ====================

    @Test
    public void testHashCode_consistent() {
        Seid seid = Seid.of("User.id", 123);
        assertEquals(seid.hashCode(), seid.hashCode());
    }

    @Test
    public void testHashCode_equalObjects() {
        Seid seid1 = Seid.of("User.id", 123);
        Seid seid2 = Seid.of("User.id", 123);
        assertEquals(seid1.hashCode(), seid2.hashCode());
    }

    @Test
    public void testHashCode_differentObjects() {
        Seid seid1 = Seid.of("User.id", 123);
        Seid seid2 = Seid.of("User.id", 456);
        assertNotEquals(seid1.hashCode(), seid2.hashCode());
    }

    @Test
    public void testToString_twoProps() {
        Seid seid = Seid.of("User.id", 123, "User.name", "John");
        String str = seid.toString();
        assertTrue(str.startsWith("User: {"));
        assertTrue(str.contains("id=123"));
        assertTrue(str.contains("name=John"));
        assertTrue(str.endsWith("}"));
    }

    @Test
    public void testToString_threeOrMoreProps() {
        Seid seid = Seid.of("User.a", 1, "User.b", 2, "User.c", 3);
        String str = seid.toString();
        assertTrue(str.startsWith("User: {"));
        assertTrue(str.contains("a=1"));
        assertTrue(str.contains("b=2"));
        assertTrue(str.contains("c=3"));
        assertTrue(str.endsWith("}"));
    }

    // ==================== toString() ====================

    @Test
    public void testToString_empty() {
        Seid seid = Seid.of("User");
        assertEquals("User: {}", seid.toString());
    }

    @Test
    public void testToString_singleProp() {
        Seid seid = Seid.of("User.id", 123);
        assertEquals("User: {id=123}", seid.toString());
    }

    @Test
    public void testToString_cached() {
        Seid seid = Seid.of("User.id", 123);
        String str1 = seid.toString();
        String str2 = seid.toString();
        // toString() is cached, so should return same string
        assertEquals(str1, str2);
    }
}

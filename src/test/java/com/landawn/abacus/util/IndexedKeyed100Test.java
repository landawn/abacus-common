package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

@Tag("new-test")
public class IndexedKeyed100Test extends TestBase {

    @Test
    public void testOf() {
        int index = 5;
        String key = "testKey";
        Integer value = 42;

        IndexedKeyed<String, Integer> indexedKeyed = IndexedKeyed.of(key, value, index);

        assertEquals(index, indexedKeyed.index());
        assertEquals(key, indexedKeyed.key());
        assertEquals(value, indexedKeyed.val());
    }

    @Test
    public void testOf_WithNullKey() {
        int index = 5;
        String key = null;
        Integer value = 42;

        IndexedKeyed<String, Integer> indexedKeyed = IndexedKeyed.of(key, value, index);

        assertEquals(index, indexedKeyed.index());
        assertNull(indexedKeyed.key());
        assertEquals(value, indexedKeyed.val());
    }

    @Test
    public void testOf_WithNullValue() {
        int index = 5;
        String key = "testKey";
        Integer value = null;

        IndexedKeyed<String, Integer> indexedKeyed = IndexedKeyed.of(key, value, index);

        assertEquals(index, indexedKeyed.index());
        assertEquals(key, indexedKeyed.key());
        assertNull(indexedKeyed.val());
    }

    @Test
    public void testOf_AllNull() {
        int index = 0;
        String key = null;
        Integer value = null;

        IndexedKeyed<String, Integer> indexedKeyed = IndexedKeyed.of(key, value, index);

        assertEquals(index, indexedKeyed.index());
        assertNull(indexedKeyed.key());
        assertNull(indexedKeyed.val());
    }

    @Test
    public void testIndex() {
        IndexedKeyed<String, Integer> indexedKeyed1 = IndexedKeyed.of("key", 42, 0);
        IndexedKeyed<String, Integer> indexedKeyed2 = IndexedKeyed.of("key", 42, Integer.MAX_VALUE);
        IndexedKeyed<String, Integer> indexedKeyed3 = IndexedKeyed.of("key", 42, Integer.MIN_VALUE);

        assertEquals(0, indexedKeyed1.index());
        assertEquals(Integer.MAX_VALUE, indexedKeyed2.index());
        assertEquals(Integer.MIN_VALUE, indexedKeyed3.index());
    }

    @Test
    public void testHashCode() {
        int index = 5;
        String key = "testKey";
        Integer value = 42;

        IndexedKeyed<String, Integer> indexedKeyed1 = IndexedKeyed.of(key, value, index);
        IndexedKeyed<String, Integer> indexedKeyed2 = IndexedKeyed.of(key, value, index);

        assertEquals(indexedKeyed1.hashCode(), indexedKeyed2.hashCode());
    }

    @Test
    public void testHashCode_DifferentIndices() {
        String key = "testKey";
        Integer value = 42;

        IndexedKeyed<String, Integer> indexedKeyed1 = IndexedKeyed.of(key, value, 5);
        IndexedKeyed<String, Integer> indexedKeyed2 = IndexedKeyed.of(key, value, 6);

        assertNotEquals(indexedKeyed1.hashCode(), indexedKeyed2.hashCode());
    }

    @Test
    public void testHashCode_DifferentKeys() {
        int index = 5;
        Integer value = 42;

        IndexedKeyed<String, Integer> indexedKeyed1 = IndexedKeyed.of("key1", value, index);
        IndexedKeyed<String, Integer> indexedKeyed2 = IndexedKeyed.of("key2", value, index);

        assertNotEquals(indexedKeyed1.hashCode(), indexedKeyed2.hashCode());
    }

    @Test
    public void testHashCode_DifferentValues() {
        int index = 5;
        String key = "testKey";

        IndexedKeyed<String, Integer> indexedKeyed1 = IndexedKeyed.of(key, 42, index);
        IndexedKeyed<String, Integer> indexedKeyed2 = IndexedKeyed.of(key, 43, index);

        assertEquals(indexedKeyed1.hashCode(), indexedKeyed2.hashCode());
    }

    @Test
    public void testHashCode_WithNullKey() {
        int index = 5;
        Integer value = 42;

        IndexedKeyed<String, Integer> indexedKeyed1 = IndexedKeyed.of(null, value, index);
        IndexedKeyed<String, Integer> indexedKeyed2 = IndexedKeyed.of(null, value, index);

        assertEquals(indexedKeyed1.hashCode(), indexedKeyed2.hashCode());
    }

    @Test
    public void testEquals_SameObject() {
        IndexedKeyed<String, Integer> indexedKeyed = IndexedKeyed.of("key", 42, 5);

        assertTrue(indexedKeyed.equals(indexedKeyed));
    }

    @Test
    public void testEquals_EqualObjects() {
        int index = 5;
        String key = "testKey";
        Integer value = 42;

        IndexedKeyed<String, Integer> indexedKeyed1 = IndexedKeyed.of(key, value, index);
        IndexedKeyed<String, Integer> indexedKeyed2 = IndexedKeyed.of(key, value, index);

        assertTrue(indexedKeyed1.equals(indexedKeyed2));
        assertTrue(indexedKeyed2.equals(indexedKeyed1));
    }

    @Test
    public void testEquals_DifferentIndices() {
        String key = "testKey";
        Integer value = 42;

        IndexedKeyed<String, Integer> indexedKeyed1 = IndexedKeyed.of(key, value, 5);
        IndexedKeyed<String, Integer> indexedKeyed2 = IndexedKeyed.of(key, value, 6);

        assertFalse(indexedKeyed1.equals(indexedKeyed2));
        assertFalse(indexedKeyed2.equals(indexedKeyed1));
    }

    @Test
    public void testEquals_DifferentKeys() {
        int index = 5;
        Integer value = 42;

        IndexedKeyed<String, Integer> indexedKeyed1 = IndexedKeyed.of("key1", value, index);
        IndexedKeyed<String, Integer> indexedKeyed2 = IndexedKeyed.of("key2", value, index);

        assertFalse(indexedKeyed1.equals(indexedKeyed2));
        assertFalse(indexedKeyed2.equals(indexedKeyed1));
    }

    @Test
    public void testEquals_DifferentValues() {
        int index = 5;
        String key = "testKey";

        IndexedKeyed<String, Integer> indexedKeyed1 = IndexedKeyed.of(key, 42, index);
        IndexedKeyed<String, Integer> indexedKeyed2 = IndexedKeyed.of(key, 43, index);

        assertTrue(indexedKeyed1.equals(indexedKeyed2));
        assertTrue(indexedKeyed2.equals(indexedKeyed1));
    }

    @Test
    public void testEquals_WithNullKey() {
        int index = 5;
        Integer value = 42;

        IndexedKeyed<String, Integer> indexedKeyed1 = IndexedKeyed.of(null, value, index);
        IndexedKeyed<String, Integer> indexedKeyed2 = IndexedKeyed.of(null, value, index);

        assertTrue(indexedKeyed1.equals(indexedKeyed2));
    }

    @Test
    public void testEquals_OneNullKey() {
        int index = 5;
        Integer value = 42;

        IndexedKeyed<String, Integer> indexedKeyed1 = IndexedKeyed.of("key", value, index);
        IndexedKeyed<String, Integer> indexedKeyed2 = IndexedKeyed.of(null, value, index);

        assertFalse(indexedKeyed1.equals(indexedKeyed2));
        assertFalse(indexedKeyed2.equals(indexedKeyed1));
    }

    @Test
    public void testEquals_Null() {
        IndexedKeyed<String, Integer> indexedKeyed = IndexedKeyed.of("key", 42, 5);

        assertFalse(indexedKeyed.equals(null));
    }

    @Test
    public void testEquals_DifferentClass() {
        IndexedKeyed<String, Integer> indexedKeyed = IndexedKeyed.of("key", 42, 5);

        assertFalse(indexedKeyed.equals("not an IndexedKeyed"));
        assertFalse(indexedKeyed.equals(42));
        assertFalse(indexedKeyed.equals(new Keyed<>("key", 42)));
    }

    @Test
    public void testToString() {
        int index = 5;
        String key = "testKey";
        Integer value = 42;

        IndexedKeyed<String, Integer> indexedKeyed = IndexedKeyed.of(key, value, index);

        assertEquals("{index=5, key=testKey, val=42}", indexedKeyed.toString());
    }

    @Test
    public void testToString_WithNullKey() {
        int index = 5;
        String key = null;
        Integer value = 42;

        IndexedKeyed<String, Integer> indexedKeyed = IndexedKeyed.of(key, value, index);

        assertEquals("{index=5, key=null, val=42}", indexedKeyed.toString());
    }

    @Test
    public void testToString_WithNullValue() {
        int index = 5;
        String key = "testKey";
        Integer value = null;

        IndexedKeyed<String, Integer> indexedKeyed = IndexedKeyed.of(key, value, index);

        assertEquals("{index=5, key=testKey, val=null}", indexedKeyed.toString());
    }

    @Test
    public void testToString_AllNull() {
        int index = 0;
        String key = null;
        Integer value = null;

        IndexedKeyed<String, Integer> indexedKeyed = IndexedKeyed.of(key, value, index);

        assertEquals("{index=0, key=null, val=null}", indexedKeyed.toString());
    }

    @Test
    public void testToString_ComplexTypes() {
        int index = 10;
        Object key = new Object() {
            @Override
            public String toString() {
                return "customKey";
            }
        };
        Object value = new Object() {
            @Override
            public String toString() {
                return "customValue";
            }
        };

        IndexedKeyed<Object, Object> indexedKeyed = IndexedKeyed.of(key, value, index);

        assertEquals("{index=10, key=customKey, val=customValue}", indexedKeyed.toString());
    }

    @Test
    public void testGenericTypes() {
        IndexedKeyed<Integer, String> intKeyStringVal = IndexedKeyed.of(100, "value", 1);
        assertEquals(100, intKeyStringVal.key());
        assertEquals("value", intKeyStringVal.val());

        IndexedKeyed<Long, Double> longKeyDoubleVal = IndexedKeyed.of(1000L, 3.14, 2);
        assertEquals(1000L, longKeyDoubleVal.key());
        assertEquals(3.14, longKeyDoubleVal.val());

        IndexedKeyed<Object, Object> objectKeyVal = IndexedKeyed.of(new Object(), new Object(), 3);
        assertNotNull(objectKeyVal.key());
        assertNotNull(objectKeyVal.val());
    }

    @Test
    public void testInheritedMethods() {
        IndexedKeyed<String, Integer> indexedKeyed = IndexedKeyed.of("key", 42, 5);

        assertEquals("key", indexedKeyed.key());
        assertEquals(42, indexedKeyed.val());
    }
}

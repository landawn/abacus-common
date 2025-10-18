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

        IndexedKeyed<String, Integer> indexedKeyed = IndexedKeyed.of(index, key, value);

        assertEquals(index, indexedKeyed.index());
        assertEquals(key, indexedKeyed.key());
        assertEquals(value, indexedKeyed.val());
    }

    @Test
    public void testOf_WithNullKey() {
        int index = 5;
        String key = null;
        Integer value = 42;

        IndexedKeyed<String, Integer> indexedKeyed = IndexedKeyed.of(index, key, value);

        assertEquals(index, indexedKeyed.index());
        assertNull(indexedKeyed.key());
        assertEquals(value, indexedKeyed.val());
    }

    @Test
    public void testOf_WithNullValue() {
        int index = 5;
        String key = "testKey";
        Integer value = null;

        IndexedKeyed<String, Integer> indexedKeyed = IndexedKeyed.of(index, key, value);

        assertEquals(index, indexedKeyed.index());
        assertEquals(key, indexedKeyed.key());
        assertNull(indexedKeyed.val());
    }

    @Test
    public void testOf_AllNull() {
        int index = 0;
        String key = null;
        Integer value = null;

        IndexedKeyed<String, Integer> indexedKeyed = IndexedKeyed.of(index, key, value);

        assertEquals(index, indexedKeyed.index());
        assertNull(indexedKeyed.key());
        assertNull(indexedKeyed.val());
    }

    @Test
    public void testIndex() {
        IndexedKeyed<String, Integer> indexedKeyed1 = IndexedKeyed.of(0, "key", 42);
        IndexedKeyed<String, Integer> indexedKeyed2 = IndexedKeyed.of(Integer.MAX_VALUE, "key", 42);
        IndexedKeyed<String, Integer> indexedKeyed3 = IndexedKeyed.of(Integer.MIN_VALUE, "key", 42);

        assertEquals(0, indexedKeyed1.index());
        assertEquals(Integer.MAX_VALUE, indexedKeyed2.index());
        assertEquals(Integer.MIN_VALUE, indexedKeyed3.index());
    }

    @Test
    public void testHashCode() {
        int index = 5;
        String key = "testKey";
        Integer value = 42;

        IndexedKeyed<String, Integer> indexedKeyed1 = IndexedKeyed.of(index, key, value);
        IndexedKeyed<String, Integer> indexedKeyed2 = IndexedKeyed.of(index, key, value);

        assertEquals(indexedKeyed1.hashCode(), indexedKeyed2.hashCode());
    }

    @Test
    public void testHashCode_DifferentIndices() {
        String key = "testKey";
        Integer value = 42;

        IndexedKeyed<String, Integer> indexedKeyed1 = IndexedKeyed.of(5, key, value);
        IndexedKeyed<String, Integer> indexedKeyed2 = IndexedKeyed.of(6, key, value);

        assertNotEquals(indexedKeyed1.hashCode(), indexedKeyed2.hashCode());
    }

    @Test
    public void testHashCode_DifferentKeys() {
        int index = 5;
        Integer value = 42;

        IndexedKeyed<String, Integer> indexedKeyed1 = IndexedKeyed.of(index, "key1", value);
        IndexedKeyed<String, Integer> indexedKeyed2 = IndexedKeyed.of(index, "key2", value);

        assertNotEquals(indexedKeyed1.hashCode(), indexedKeyed2.hashCode());
    }

    @Test
    public void testHashCode_DifferentValues() {
        int index = 5;
        String key = "testKey";

        IndexedKeyed<String, Integer> indexedKeyed1 = IndexedKeyed.of(index, key, 42);
        IndexedKeyed<String, Integer> indexedKeyed2 = IndexedKeyed.of(index, key, 43);

        assertEquals(indexedKeyed1.hashCode(), indexedKeyed2.hashCode());
    }

    @Test
    public void testHashCode_WithNullKey() {
        int index = 5;
        Integer value = 42;

        IndexedKeyed<String, Integer> indexedKeyed1 = IndexedKeyed.of(index, null, value);
        IndexedKeyed<String, Integer> indexedKeyed2 = IndexedKeyed.of(index, null, value);

        assertEquals(indexedKeyed1.hashCode(), indexedKeyed2.hashCode());
    }

    @Test
    public void testEquals_SameObject() {
        IndexedKeyed<String, Integer> indexedKeyed = IndexedKeyed.of(5, "key", 42);

        assertTrue(indexedKeyed.equals(indexedKeyed));
    }

    @Test
    public void testEquals_EqualObjects() {
        int index = 5;
        String key = "testKey";
        Integer value = 42;

        IndexedKeyed<String, Integer> indexedKeyed1 = IndexedKeyed.of(index, key, value);
        IndexedKeyed<String, Integer> indexedKeyed2 = IndexedKeyed.of(index, key, value);

        assertTrue(indexedKeyed1.equals(indexedKeyed2));
        assertTrue(indexedKeyed2.equals(indexedKeyed1));
    }

    @Test
    public void testEquals_DifferentIndices() {
        String key = "testKey";
        Integer value = 42;

        IndexedKeyed<String, Integer> indexedKeyed1 = IndexedKeyed.of(5, key, value);
        IndexedKeyed<String, Integer> indexedKeyed2 = IndexedKeyed.of(6, key, value);

        assertFalse(indexedKeyed1.equals(indexedKeyed2));
        assertFalse(indexedKeyed2.equals(indexedKeyed1));
    }

    @Test
    public void testEquals_DifferentKeys() {
        int index = 5;
        Integer value = 42;

        IndexedKeyed<String, Integer> indexedKeyed1 = IndexedKeyed.of(index, "key1", value);
        IndexedKeyed<String, Integer> indexedKeyed2 = IndexedKeyed.of(index, "key2", value);

        assertFalse(indexedKeyed1.equals(indexedKeyed2));
        assertFalse(indexedKeyed2.equals(indexedKeyed1));
    }

    @Test
    public void testEquals_DifferentValues() {
        int index = 5;
        String key = "testKey";

        IndexedKeyed<String, Integer> indexedKeyed1 = IndexedKeyed.of(index, key, 42);
        IndexedKeyed<String, Integer> indexedKeyed2 = IndexedKeyed.of(index, key, 43);

        assertTrue(indexedKeyed1.equals(indexedKeyed2));
        assertTrue(indexedKeyed2.equals(indexedKeyed1));
    }

    @Test
    public void testEquals_WithNullKey() {
        int index = 5;
        Integer value = 42;

        IndexedKeyed<String, Integer> indexedKeyed1 = IndexedKeyed.of(index, null, value);
        IndexedKeyed<String, Integer> indexedKeyed2 = IndexedKeyed.of(index, null, value);

        assertTrue(indexedKeyed1.equals(indexedKeyed2));
    }

    @Test
    public void testEquals_OneNullKey() {
        int index = 5;
        Integer value = 42;

        IndexedKeyed<String, Integer> indexedKeyed1 = IndexedKeyed.of(index, "key", value);
        IndexedKeyed<String, Integer> indexedKeyed2 = IndexedKeyed.of(index, null, value);

        assertFalse(indexedKeyed1.equals(indexedKeyed2));
        assertFalse(indexedKeyed2.equals(indexedKeyed1));
    }

    @Test
    public void testEquals_Null() {
        IndexedKeyed<String, Integer> indexedKeyed = IndexedKeyed.of(5, "key", 42);

        assertFalse(indexedKeyed.equals(null));
    }

    @Test
    public void testEquals_DifferentClass() {
        IndexedKeyed<String, Integer> indexedKeyed = IndexedKeyed.of(5, "key", 42);

        assertFalse(indexedKeyed.equals("not an IndexedKeyed"));
        assertFalse(indexedKeyed.equals(42));
        assertFalse(indexedKeyed.equals(new Keyed<>("key", 42)));
    }

    @Test
    public void testToString() {
        int index = 5;
        String key = "testKey";
        Integer value = 42;

        IndexedKeyed<String, Integer> indexedKeyed = IndexedKeyed.of(index, key, value);

        assertEquals("{index=5, key=testKey, val=42}", indexedKeyed.toString());
    }

    @Test
    public void testToString_WithNullKey() {
        int index = 5;
        String key = null;
        Integer value = 42;

        IndexedKeyed<String, Integer> indexedKeyed = IndexedKeyed.of(index, key, value);

        assertEquals("{index=5, key=null, val=42}", indexedKeyed.toString());
    }

    @Test
    public void testToString_WithNullValue() {
        int index = 5;
        String key = "testKey";
        Integer value = null;

        IndexedKeyed<String, Integer> indexedKeyed = IndexedKeyed.of(index, key, value);

        assertEquals("{index=5, key=testKey, val=null}", indexedKeyed.toString());
    }

    @Test
    public void testToString_AllNull() {
        int index = 0;
        String key = null;
        Integer value = null;

        IndexedKeyed<String, Integer> indexedKeyed = IndexedKeyed.of(index, key, value);

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

        IndexedKeyed<Object, Object> indexedKeyed = IndexedKeyed.of(index, key, value);

        assertEquals("{index=10, key=customKey, val=customValue}", indexedKeyed.toString());
    }

    @Test
    public void testGenericTypes() {
        IndexedKeyed<Integer, String> intKeyStringVal = IndexedKeyed.of(1, 100, "value");
        assertEquals(100, intKeyStringVal.key());
        assertEquals("value", intKeyStringVal.val());

        IndexedKeyed<Long, Double> longKeyDoubleVal = IndexedKeyed.of(2, 1000L, 3.14);
        assertEquals(1000L, longKeyDoubleVal.key());
        assertEquals(3.14, longKeyDoubleVal.val());

        IndexedKeyed<Object, Object> objectKeyVal = IndexedKeyed.of(3, new Object(), new Object());
        assertNotNull(objectKeyVal.key());
        assertNotNull(objectKeyVal.val());
    }

    @Test
    public void testInheritedMethods() {
        IndexedKeyed<String, Integer> indexedKeyed = IndexedKeyed.of(5, "key", 42);

        assertEquals("key", indexedKeyed.key());
        assertEquals(42, indexedKeyed.val());
    }
}

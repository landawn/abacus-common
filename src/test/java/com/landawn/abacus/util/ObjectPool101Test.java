package com.landawn.abacus.util;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import com.landawn.abacus.TestBase;

@Tag("new-test")
public class ObjectPool101Test extends TestBase {

    @Test
    public void testConstructor() {
        ObjectPool<String, Integer> pool = new ObjectPool<>(16);
        Assertions.assertNotNull(pool);
        Assertions.assertTrue(pool.isEmpty());
        Assertions.assertEquals(0, pool.size());
    }

    @Test
    public void testMultipleEntriesWithSameHash() {
        ObjectPool<String, Integer> pool = new ObjectPool<>(4);

        for (int i = 0; i < 10; i++) {
            pool.put("key" + i, i);
        }

        for (int i = 0; i < 10; i++) {
            Assertions.assertEquals(i, pool.get("key" + i));
        }

        Assertions.assertEquals(10, pool.size());
    }

    @Test
    public void testRemoveFromChain() {
        ObjectPool<String, Integer> pool = new ObjectPool<>(4);

        pool.put("a", 1);
        pool.put("b", 2);
        pool.put("c", 3);
        pool.put("d", 4);
        pool.put("e", 5);

        pool.remove("c");

        Assertions.assertEquals(1, pool.get("a"));
        Assertions.assertEquals(2, pool.get("b"));
        Assertions.assertNull(pool.get("c"));
        Assertions.assertEquals(4, pool.get("d"));
        Assertions.assertEquals(5, pool.get("e"));
    }

    @Test
    public void testNullHandling() {
        ObjectPool<String, Integer> pool = new ObjectPool<>(10);

        Assertions.assertNull(pool.get(null));
        Assertions.assertFalse(pool.containsKey(null));
        Assertions.assertNull(pool.remove(null));
    }

    @Test
    public void testCachingBehavior() {
        ObjectPool<String, Integer> pool = new ObjectPool<>(10);
        pool.put("key1", 1);
        pool.put("key2", 2);

        Set<String> keys1 = pool.keySet();
        Set<String> keys2 = pool.keySet();

        Assertions.assertSame(keys1, keys2);

        pool.put("key3", 3);
        Set<String> keys3 = pool.keySet();
        Assertions.assertNotSame(keys1, keys3);
    }

    @Test
    public void testConstructorInvalidCapacity() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> new ObjectPool<>(0));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new ObjectPool<>(-1));
    }

    @Test
    public void testGet() {
        ObjectPool<String, Integer> pool = new ObjectPool<>(10);
        pool.put("one", 1);

        Integer value = pool.get("one");
        Assertions.assertEquals(1, value);

        Integer missing = pool.get("two");
        Assertions.assertNull(missing);
    }

    @Test
    public void testGetNull() {
        ObjectPool<String, Integer> pool = new ObjectPool<>(10);
        Integer value = pool.get(null);
        Assertions.assertNull(value);
    }

    @Test
    public void testPut() {
        ObjectPool<String, Integer> pool = new ObjectPool<>(10);

        Integer previous = pool.put("key", 100);
        Assertions.assertNull(previous);
        Assertions.assertEquals(100, pool.get("key"));

        Integer replaced = pool.put("key", 200);
        Assertions.assertEquals(100, replaced);
        Assertions.assertEquals(200, pool.get("key"));
    }

    @Test
    public void testPutNullKey() {
        ObjectPool<String, Integer> pool = new ObjectPool<>(10);
        Assertions.assertThrows(IllegalArgumentException.class, () -> pool.put(null, 100));
    }

    @Test
    public void testPutNullValue() {
        ObjectPool<String, Integer> pool = new ObjectPool<>(10);
        Assertions.assertThrows(IllegalArgumentException.class, () -> pool.put("key", null));
    }

    @Test
    public void testPutCollision() {
        ObjectPool<String, Integer> pool = new ObjectPool<>(4);

        pool.put("key1", 1);
        pool.put("key2", 2);
        pool.put("key3", 3);
        pool.put("key4", 4);

        Assertions.assertEquals(1, pool.get("key1"));
        Assertions.assertEquals(2, pool.get("key2"));
        Assertions.assertEquals(3, pool.get("key3"));
        Assertions.assertEquals(4, pool.get("key4"));
    }

    @Test
    public void testPutAll() {
        ObjectPool<String, Integer> pool = new ObjectPool<>(10);

        Map<String, Integer> map = new HashMap<>();
        map.put("one", 1);
        map.put("two", 2);
        map.put("three", 3);

        pool.putAll(map);

        Assertions.assertEquals(3, pool.size());
        Assertions.assertEquals(1, pool.get("one"));
        Assertions.assertEquals(2, pool.get("two"));
        Assertions.assertEquals(3, pool.get("three"));
    }

    @Test
    public void testPutAllWithNullValues() {
        ObjectPool<String, Integer> pool = new ObjectPool<>(10);

        Map<String, Integer> map = new HashMap<>();
        map.put("one", 1);
        map.put("two", null);
        map.put("three", 3);

        pool.putAll(map);

        Assertions.assertEquals(2, pool.size());
        Assertions.assertEquals(1, pool.get("one"));
        Assertions.assertNull(pool.get("two"));
        Assertions.assertEquals(3, pool.get("three"));
    }

    @Test
    public void testRemove() {
        ObjectPool<String, Integer> pool = new ObjectPool<>(10);
        pool.put("key", 100);

        Integer removed = pool.remove("key");
        Assertions.assertEquals(100, removed);
        Assertions.assertNull(pool.get("key"));
        Assertions.assertEquals(0, pool.size());

        Integer notFound = pool.remove("key");
        Assertions.assertNull(notFound);
    }

    @Test
    public void testRemoveEmpty() {
        ObjectPool<String, Integer> pool = new ObjectPool<>(10);
        Integer removed = pool.remove("nonexistent");
        Assertions.assertNull(removed);
    }

    @Test
    public void testRemoveWithCollision() {
        ObjectPool<String, Integer> pool = new ObjectPool<>(4);

        pool.put("key1", 1);
        pool.put("key2", 2);
        pool.put("key3", 3);

        Integer removed = pool.remove("key2");
        Assertions.assertEquals(2, removed);
        Assertions.assertEquals(1, pool.get("key1"));
        Assertions.assertNull(pool.get("key2"));
        Assertions.assertEquals(3, pool.get("key3"));
    }

    @Test
    public void testContainsKey() {
        ObjectPool<String, Integer> pool = new ObjectPool<>(10);
        pool.put("key", 100);

        Assertions.assertTrue(pool.containsKey("key"));
        Assertions.assertFalse(pool.containsKey("nonexistent"));
        Assertions.assertFalse(pool.containsKey(null));
    }

    @Test
    public void testContainsValue() {
        ObjectPool<String, Integer> pool = new ObjectPool<>(10);
        pool.put("key1", 100);
        pool.put("key2", 200);

        Assertions.assertTrue(pool.containsValue(100));
        Assertions.assertTrue(pool.containsValue(200));
        Assertions.assertFalse(pool.containsValue(300));
        Assertions.assertFalse(pool.containsValue(null));
    }

    @Test
    public void testKeySet() {
        ObjectPool<String, Integer> pool = new ObjectPool<>(10);
        pool.put("one", 1);
        pool.put("two", 2);
        pool.put("three", 3);

        Set<String> keys = pool.keySet();
        Assertions.assertNotNull(keys);
        Assertions.assertEquals(3, keys.size());
        Assertions.assertTrue(keys.contains("one"));
        Assertions.assertTrue(keys.contains("two"));
        Assertions.assertTrue(keys.contains("three"));
    }

    @Test
    public void testKeySetUnmodifiable() {
        ObjectPool<String, Integer> pool = new ObjectPool<>(10);
        pool.put("key", 100);

        Set<String> keys = pool.keySet();
        Assertions.assertThrows(UnsupportedOperationException.class, () -> keys.add("new"));
        Assertions.assertThrows(UnsupportedOperationException.class, () -> keys.remove("key"));
    }

    @Test
    public void testKeySetReflectsChanges() {
        ObjectPool<String, Integer> pool = new ObjectPool<>(10);
        pool.put("key1", 1);

        Set<String> keys1 = pool.keySet();
        Assertions.assertEquals(1, keys1.size());

        pool.put("key2", 2);
        Set<String> keys2 = pool.keySet();
        Assertions.assertEquals(2, keys2.size());
        Assertions.assertTrue(keys2.contains("key2"));
    }

    @Test
    public void testValues() {
        ObjectPool<String, Integer> pool = new ObjectPool<>(10);
        pool.put("one", 1);
        pool.put("two", 2);
        pool.put("three", 3);

        Collection<Integer> values = pool.values();
        Assertions.assertNotNull(values);
        Assertions.assertEquals(3, values.size());
        Assertions.assertTrue(values.contains(1));
        Assertions.assertTrue(values.contains(2));
        Assertions.assertTrue(values.contains(3));
    }

    @Test
    public void testValuesUnmodifiable() {
        ObjectPool<String, Integer> pool = new ObjectPool<>(10);
        pool.put("key", 100);

        Collection<Integer> values = pool.values();
        Assertions.assertThrows(UnsupportedOperationException.class, () -> values.add(200));
        Assertions.assertThrows(UnsupportedOperationException.class, () -> values.remove(100));
    }

    @Test
    public void testEntrySet() {
        ObjectPool<String, Integer> pool = new ObjectPool<>(10);
        pool.put("one", 1);
        pool.put("two", 2);

        Set<Map.Entry<String, Integer>> entries = pool.entrySet();
        Assertions.assertNotNull(entries);
        Assertions.assertEquals(2, entries.size());

        for (Map.Entry<String, Integer> entry : entries) {
            if ("one".equals(entry.getKey())) {
                Assertions.assertEquals(1, entry.getValue());
            } else if ("two".equals(entry.getKey())) {
                Assertions.assertEquals(2, entry.getValue());
            } else {
                Assertions.fail("Unexpected entry: " + entry);
            }
        }
    }

    @Test
    public void testEntrySetUnmodifiable() {
        ObjectPool<String, Integer> pool = new ObjectPool<>(10);
        pool.put("key", 100);

        Set<Map.Entry<String, Integer>> entries = pool.entrySet();
        Assertions.assertThrows(UnsupportedOperationException.class, () -> entries.clear());
    }

    @Test
    public void testSize() {
        ObjectPool<String, Integer> pool = new ObjectPool<>(10);
        Assertions.assertEquals(0, pool.size());

        pool.put("one", 1);
        Assertions.assertEquals(1, pool.size());

        pool.put("two", 2);
        Assertions.assertEquals(2, pool.size());

        pool.remove("one");
        Assertions.assertEquals(1, pool.size());
    }

    @Test
    public void testIsEmpty() {
        ObjectPool<String, Integer> pool = new ObjectPool<>(10);
        Assertions.assertTrue(pool.isEmpty());

        pool.put("key", 100);
        Assertions.assertFalse(pool.isEmpty());

        pool.remove("key");
        Assertions.assertTrue(pool.isEmpty());
    }

    @Test
    public void testClear() {
        ObjectPool<String, Integer> pool = new ObjectPool<>(10);
        pool.put("one", 1);
        pool.put("two", 2);
        pool.put("three", 3);

        Assertions.assertEquals(3, pool.size());

        pool.clear();

        Assertions.assertEquals(0, pool.size());
        Assertions.assertTrue(pool.isEmpty());
        Assertions.assertNull(pool.get("one"));
        Assertions.assertNull(pool.get("two"));
        Assertions.assertNull(pool.get("three"));
    }

    @Test
    public void testClearAndReuse() {
        ObjectPool<String, Integer> pool = new ObjectPool<>(10);
        pool.put("key", 100);
        pool.clear();

        pool.put("newkey", 200);
        Assertions.assertEquals(200, pool.get("newkey"));
        Assertions.assertEquals(1, pool.size());
    }

    @Test
    public void testHash() {
        int hash1 = ObjectPool.hash("test");
        int hash2 = ObjectPool.hash("test");
        Assertions.assertEquals(hash1, hash2);

        int hashNull = ObjectPool.hash(null);
        Assertions.assertEquals(0, hashNull);
    }

    @Test
    public void testConcurrentAccess() throws InterruptedException {
        ObjectPool<String, Integer> pool = new ObjectPool<>(100);

        Thread t1 = new Thread(() -> {
            for (int i = 0; i < 50; i++) {
                pool.put("key" + i, i);
            }
        });

        Thread t2 = new Thread(() -> {
            for (int i = 50; i < 100; i++) {
                pool.put("key" + i, i);
            }
        });

        t1.start();
        t2.start();
        t1.join();
        t2.join();

        Assertions.assertEquals(100, pool.size());
        for (int i = 0; i < 100; i++) {
            Assertions.assertEquals(i, pool.get("key" + i));
        }
    }

    @Test
    public void testExceedCapacity() {
        ObjectPool<String, Integer> pool = new ObjectPool<>(2);

        pool.put("one", 1);
        pool.put("two", 2);
        pool.put("three", 3);

        Assertions.assertEquals(3, pool.size());
        Assertions.assertEquals(1, pool.get("one"));
        Assertions.assertEquals(2, pool.get("two"));
        Assertions.assertEquals(3, pool.get("three"));
    }

    @Test
    public void testEntryEquals() {
        ObjectPool<String, Integer> pool1 = new ObjectPool<>(10);
        pool1.put("key", 100);

        ObjectPool<String, Integer> pool2 = new ObjectPool<>(10);
        pool2.put("key", 100);

        Map.Entry<String, Integer> entry1 = pool1.entrySet().iterator().next();
        Map.Entry<String, Integer> entry2 = pool2.entrySet().iterator().next();

        Assertions.assertEquals(entry1, entry2);
        Assertions.assertEquals(entry1, entry1);
        Assertions.assertNotEquals(entry1, null);
        Assertions.assertNotEquals(entry1, "not an entry");
    }

    @Test
    public void testEntryHashCode() {
        ObjectPool<String, Integer> pool = new ObjectPool<>(10);
        pool.put("key", 100);

        Map.Entry<String, Integer> entry = pool.entrySet().iterator().next();
        int hashCode = entry.hashCode();

        Assertions.assertEquals(hashCode, entry.hashCode());
    }

    @Test
    public void testEntryToString() {
        ObjectPool<String, Integer> pool = new ObjectPool<>(10);
        pool.put("key", 100);

        Map.Entry<String, Integer> entry = pool.entrySet().iterator().next();
        String str = entry.toString();

        Assertions.assertNotNull(str);
        Assertions.assertTrue(str.contains("key"));
        Assertions.assertTrue(str.contains("100"));
        Assertions.assertTrue(str.contains("="));
    }

    @Test
    public void testEntryGetKey() {
        ObjectPool<String, Integer> pool = new ObjectPool<>(10);
        pool.put("testkey", 100);

        Map.Entry<String, Integer> entry = pool.entrySet().iterator().next();
        Assertions.assertEquals("testkey", entry.getKey());
    }

    @Test
    public void testEntryGetValue() {
        ObjectPool<String, Integer> pool = new ObjectPool<>(10);
        pool.put("key", 200);

        Map.Entry<String, Integer> entry = pool.entrySet().iterator().next();
        Assertions.assertEquals(200, entry.getValue());
    }

    @Test
    public void testEntrySetValue() {
        ObjectPool<String, Integer> pool = new ObjectPool<>(10);
        pool.put("key", 100);

        Map.Entry<String, Integer> entry = pool.entrySet().iterator().next();
        Integer oldValue = entry.setValue(200);

        Assertions.assertEquals(100, oldValue);
        Assertions.assertEquals(200, entry.getValue());
        Assertions.assertEquals(200, pool.get("key"));
    }
}

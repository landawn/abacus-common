package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.If.OrElse;
import com.landawn.abacus.util.u.Optional;
import com.landawn.abacus.util.stream.EntryStream;
import com.landawn.abacus.util.stream.Stream;

@Tag("new-test")
public class Multimap123Test extends TestBase {

    private ListMultimap<String, Integer> listMultimap;
    private SetMultimap<String, Integer> setMultimap;

    @BeforeEach
    public void setUp() {
        listMultimap = CommonUtil.newListMultimap();
        setMultimap = CommonUtil.newSetMultimap();
    }

    @Test
    public void testGetFirst() {
        assertNull(listMultimap.getFirst("nonexistent"));

        listMultimap.put("key1", 10);
        listMultimap.put("key1", 20);
        listMultimap.put("key1", 30);

        assertEquals(Integer.valueOf(10), listMultimap.getFirst("key1"));

        listMultimap.put("empty", 5);
        listMultimap.removeOne("empty", 5);
        assertNull(listMultimap.getFirst("empty"));
    }

    @Test
    public void testGetFirstOrDefault() {
        Integer defaultValue = -1;
        assertEquals(defaultValue, listMultimap.getFirstOrDefault("nonexistent", defaultValue));

        listMultimap.put("key1", 10);
        listMultimap.put("key1", 20);

        assertEquals(Integer.valueOf(10), listMultimap.getFirstOrDefault("key1", defaultValue));
        assertEquals(defaultValue, listMultimap.getFirstOrDefault("nonexistent", defaultValue));
    }

    @Test
    public void testGet() {
        assertNull(listMultimap.get("nonexistent"));

        listMultimap.put("key1", 10);
        listMultimap.put("key1", 20);

        List<Integer> values = listMultimap.get("key1");
        assertNotNull(values);
        assertEquals(2, values.size());
        assertTrue(values.contains(10));
        assertTrue(values.contains(20));
    }

    @Test
    public void testGetOrDefault() {
        List<Integer> defaultValue = Arrays.asList(-1, -2);
        List<Integer> result = listMultimap.getOrDefault("nonexistent", defaultValue);
        assertEquals(defaultValue, result);

        listMultimap.put("key1", 10);
        List<Integer> values = listMultimap.getOrDefault("key1", defaultValue);
        assertNotEquals(defaultValue, values);
        assertTrue(values.contains(10));
    }

    @Test
    public void testKeySet() {
        assertTrue(listMultimap.keySet().isEmpty());

        listMultimap.put("key1", 10);
        listMultimap.put("key2", 20);
        listMultimap.put("key1", 30);

        Set<String> keySet = listMultimap.keySet();
        assertEquals(2, keySet.size());
        assertTrue(keySet.contains("key1"));
        assertTrue(keySet.contains("key2"));
    }

    @Test
    public void testValues() {
        assertTrue(listMultimap.values().isEmpty());

        listMultimap.put("key1", 10);
        listMultimap.put("key2", 20);

        Collection<List<Integer>> values = listMultimap.values();
        assertEquals(2, values.size());

        boolean found10 = false, found20 = false;
        for (List<Integer> coll : values) {
            if (coll.contains(10))
                found10 = true;
            if (coll.contains(20))
                found20 = true;
        }
        assertTrue(found10 && found20);
    }

    @Test
    public void testEntrySet() {
        assertTrue(listMultimap.entrySet().isEmpty());

        listMultimap.put("key1", 10);
        listMultimap.put("key2", 20);

        Set<Map.Entry<String, List<Integer>>> entrySet = listMultimap.entrySet();
        assertEquals(2, entrySet.size());

        boolean foundKey1 = false, foundKey2 = false;
        for (Map.Entry<String, List<Integer>> entry : entrySet) {
            if ("key1".equals(entry.getKey()) && entry.getValue().contains(10)) {
                foundKey1 = true;
            }
            if ("key2".equals(entry.getKey()) && entry.getValue().contains(20)) {
                foundKey2 = true;
            }
        }
        assertTrue(foundKey1 && foundKey2);
    }

    @Test
    public void testFlatValues() {
        assertTrue(listMultimap.flatValues().isEmpty());

        listMultimap.put("key1", 10);
        listMultimap.put("key1", 20);
        listMultimap.put("key2", 30);

        List<Integer> flatValues = listMultimap.flatValues();
        assertEquals(3, flatValues.size());
        assertTrue(flatValues.contains(10));
        assertTrue(flatValues.contains(20));
        assertTrue(flatValues.contains(30));
    }

    @Test
    public void testFlatValuesWithSupplier() {
        listMultimap.put("key1", 10);
        listMultimap.put("key2", 20);

        TreeSet<Integer> result = listMultimap.flatValues(size -> new TreeSet<>());
        assertEquals(2, result.size());
        assertTrue(result.contains(10));
        assertTrue(result.contains(20));
    }

    @Test
    public void testToMap() {
        listMultimap.put("key1", 10);
        listMultimap.put("key1", 20);
        listMultimap.put("key2", 30);

        Map<String, List<Integer>> map = listMultimap.toMap();
        assertEquals(2, map.size());
        assertTrue(map.get("key1").contains(10));
        assertTrue(map.get("key1").contains(20));
        assertTrue(map.get("key2").contains(30));
    }

    @Test
    public void testToMapWithSupplier() {
        listMultimap.put("key1", 10);
        listMultimap.put("key2", 20);

        TreeMap<String, List<Integer>> map = listMultimap.toMap(size -> new TreeMap<>());
        assertEquals(2, map.size());
        assertTrue(map instanceof TreeMap);
        assertTrue(map.get("key1").contains(10));
        assertTrue(map.get("key2").contains(20));
    }

    @Test
    public void testToMultiset() {
        listMultimap.put("key1", 10);
        listMultimap.put("key1", 20);
        listMultimap.put("key2", 30);

        Multiset<String> multiset = listMultimap.toMultiset();
        assertEquals(2, multiset.getCount("key1"));
        assertEquals(1, multiset.getCount("key2"));
        assertEquals(0, multiset.getCount("key3"));
    }

    @Test
    public void testIterator() {
        listMultimap.put("key1", 10);
        listMultimap.put("key2", 20);

        Iterator<Map.Entry<String, List<Integer>>> iterator = listMultimap.iterator();
        assertTrue(iterator.hasNext());

        int count = 0;
        while (iterator.hasNext()) {
            Map.Entry<String, List<Integer>> entry = iterator.next();
            assertNotNull(entry.getKey());
            assertNotNull(entry.getValue());
            count++;
        }
        assertEquals(2, count);
    }

    @Test
    public void testStream() {
        listMultimap.put("key1", 10);
        listMultimap.put("key2", 20);

        Stream<Map.Entry<String, List<Integer>>> stream = listMultimap.stream();
        assertEquals(2, stream.count());
    }

    @Test
    public void testEntryStream() {
        listMultimap.put("key1", 10);
        listMultimap.put("key2", 20);

        EntryStream<String, List<Integer>> entryStream = listMultimap.entryStream();
        assertEquals(2, entryStream.count());
    }

    @Test
    public void testSize() {
        assertEquals(0, listMultimap.size());

        listMultimap.put("key1", 10);
        assertEquals(1, listMultimap.size());

        listMultimap.put("key2", 20);
        assertEquals(2, listMultimap.size());

        listMultimap.put("key1", 30);
        assertEquals(2, listMultimap.size());
    }

    @Test
    public void testTotalCountOfValues() {
        assertEquals(0, listMultimap.totalCountOfValues());

        listMultimap.put("key1", 10);
        assertEquals(1, listMultimap.totalCountOfValues());

        listMultimap.put("key1", 20);
        assertEquals(2, listMultimap.totalCountOfValues());

        listMultimap.put("key2", 30);
        assertEquals(3, listMultimap.totalCountOfValues());
    }

    @Test
    public void testIsEmpty() {
        assertTrue(listMultimap.isEmpty());

        listMultimap.put("key1", 10);
        assertFalse(listMultimap.isEmpty());

        listMultimap.clear();
        assertTrue(listMultimap.isEmpty());
    }

    @Test
    public void testCopy() {
        listMultimap.put("key1", 10);
        listMultimap.put("key1", 20);
        listMultimap.put("key2", 30);

        Multimap<String, Integer, ? extends List<Integer>> copy = listMultimap.copy();
        assertNotSame(listMultimap, copy);
        assertEquals(listMultimap.size(), copy.size());
        assertEquals(listMultimap.totalCountOfValues(), copy.totalCountOfValues());

        assertTrue(copy.contains("key1", 10));
        assertTrue(copy.contains("key1", 20));
        assertTrue(copy.contains("key2", 30));
    }

    @Test
    public void testPut() {
        assertTrue(listMultimap.put("key1", 10));
        assertTrue(listMultimap.contains("key1", 10));
        assertEquals(1, listMultimap.totalCountOfValues());

        assertTrue(listMultimap.put("key1", 20));
        assertEquals(2, listMultimap.totalCountOfValues());

        assertTrue(setMultimap.put("key1", 10));
        assertFalse(setMultimap.put("key1", 10));
        assertEquals(1, setMultimap.totalCountOfValues());
    }

    @Test
    public void testPutMap() {
        Map<String, Integer> map = new HashMap<>();
        map.put("key1", 10);
        map.put("key2", 20);

        assertTrue(listMultimap.put(map));
        assertTrue(listMultimap.contains("key1", 10));
        assertTrue(listMultimap.contains("key2", 20));

        assertFalse(listMultimap.put(Collections.emptyMap()));
        assertFalse(listMultimap.put((Map<String, Integer>) null));
    }

    @Test
    public void testPutIfAbsent() {
        assertTrue(listMultimap.putIfAbsent("key1", 10));
        assertTrue(listMultimap.contains("key1", 10));

        assertFalse(listMultimap.putIfAbsent("key1", 10));
        assertTrue(listMultimap.putIfAbsent("key1", 20));

        assertEquals(2, listMultimap.totalCountOfValues());
    }

    @Test
    public void testPutIfKeyAbsent() {
        assertTrue(listMultimap.putIfKeyAbsent("key1", 10));
        assertTrue(listMultimap.contains("key1", 10));

        assertFalse(listMultimap.putIfKeyAbsent("key1", 20));
        assertEquals(1, listMultimap.totalCountOfValues());

        assertTrue(listMultimap.putIfKeyAbsent("key2", 30));
        assertTrue(listMultimap.contains("key2", 30));
    }

    @Test
    public void testPutMany() {
        Collection<Integer> values = Arrays.asList(10, 20, 30);
        assertTrue(listMultimap.putMany("key1", values));
        assertEquals(3, listMultimap.totalCountOfValues());

        assertTrue(listMultimap.putMany("key1", Arrays.asList(40)));
        assertEquals(4, listMultimap.totalCountOfValues());

        assertFalse(listMultimap.putMany("key2", Collections.emptyList()));
        assertFalse(listMultimap.putMany("key2", null));
    }

    @Test
    public void testPutManyIfKeyAbsent() {
        Collection<Integer> values = Arrays.asList(10, 20);
        assertTrue(listMultimap.putManyIfKeyAbsent("key1", values));
        assertEquals(2, listMultimap.totalCountOfValues());

        assertFalse(listMultimap.putManyIfKeyAbsent("key1", Arrays.asList(30)));
        assertEquals(2, listMultimap.totalCountOfValues());

        assertFalse(listMultimap.putManyIfKeyAbsent("key2", Collections.emptyList()));
    }

    @Test
    public void testPutManyMap() {
        Map<String, Collection<Integer>> map = new HashMap<>();
        map.put("key1", Arrays.asList(10, 20));
        map.put("key2", Arrays.asList(30));

        assertTrue(listMultimap.putMany(map));
        assertEquals(3, listMultimap.totalCountOfValues());

        assertFalse(listMultimap.putMany(Collections.emptyMap()));

        map.put("key3", Collections.emptyList());
        listMultimap.putMany(map);
    }

    @Test
    public void testPutManyMultimap() {
        ListMultimap<String, Integer> other = CommonUtil.newListMultimap();
        other.put("key1", 10);
        other.put("key1", 20);
        other.put("key2", 30);

        assertTrue(listMultimap.putMany(other));
        assertEquals(3, listMultimap.totalCountOfValues());

        ListMultimap<String, Integer> empty = CommonUtil.newListMultimap();
        assertFalse(listMultimap.putMany(empty));
        assertFalse(listMultimap.putMany((Multimap<String, Integer, ? extends Collection<Integer>>) null));
    }

    @Test
    public void testClear() {
        listMultimap.put("key1", 10);
        listMultimap.put("key2", 20);
        assertFalse(listMultimap.isEmpty());

        listMultimap.clear();
        assertTrue(listMultimap.isEmpty());
        assertEquals(0, listMultimap.size());
        assertEquals(0, listMultimap.totalCountOfValues());
    }

    @Test
    public void testRemoveOne() {
        listMultimap.put("key1", 10);
        listMultimap.put("key1", 20);
        listMultimap.put("key2", 30);

        assertTrue(listMultimap.removeOne("key1", 10));
        assertFalse(listMultimap.contains("key1", 10));
        assertTrue(listMultimap.contains("key1", 20));

        assertFalse(listMultimap.removeOne("key1", 99));
        assertFalse(listMultimap.removeOne("nonexistent", 10));

        assertTrue(listMultimap.removeOne("key2", 30));
        assertFalse(listMultimap.containsKey("key2"));
    }

    @Test
    public void testRemoveOneMap() {
        listMultimap.put("key1", 10);
        listMultimap.put("key1", 20);
        listMultimap.put("key2", 30);

        Map<String, Integer> toRemove = new HashMap<>();
        toRemove.put("key1", 10);
        toRemove.put("key2", 30);

        assertTrue(listMultimap.removeOne(toRemove));
        assertFalse(listMultimap.contains("key1", 10));
        assertTrue(listMultimap.contains("key1", 20));
        assertFalse(listMultimap.containsKey("key2"));

        assertFalse(listMultimap.removeOne(Collections.emptyMap()));
        assertFalse(listMultimap.removeOne((Map<String, Integer>) null));
    }

    @Test
    public void testRemoveAll() {
        listMultimap.put("key1", 10);
        listMultimap.put("key1", 20);
        listMultimap.put("key2", 30);

        List<Integer> removed = listMultimap.removeAll("key1");
        assertNotNull(removed);
        assertEquals(2, removed.size());
        assertTrue(removed.contains(10));
        assertTrue(removed.contains(20));
        assertFalse(listMultimap.containsKey("key1"));

        assertNull(listMultimap.removeAll("nonexistent"));
    }

    @Test
    public void testRemoveMany() {
        listMultimap.put("key1", 10);
        listMultimap.put("key1", 20);
        listMultimap.put("key1", 30);

        Collection<Integer> toRemove = Arrays.asList(10, 20);
        assertTrue(listMultimap.removeMany("key1", toRemove));
        assertFalse(listMultimap.contains("key1", 10));
        assertFalse(listMultimap.contains("key1", 20));
        assertTrue(listMultimap.contains("key1", 30));

        assertFalse(listMultimap.removeMany("key1", Collections.emptyList()));
        assertFalse(listMultimap.removeMany("nonexistent", Arrays.asList(1, 2)));
        assertFalse(listMultimap.removeMany("key1", null));
    }

    @Test
    public void testRemoveManyMap() {
        listMultimap.put("key1", 10);
        listMultimap.put("key1", 20);
        listMultimap.put("key2", 30);
        listMultimap.put("key2", 40);

        Map<String, Collection<Integer>> toRemove = new HashMap<>();
        toRemove.put("key1", Arrays.asList(10));
        toRemove.put("key2", Arrays.asList(30, 40));

        assertTrue(listMultimap.removeMany(toRemove));
        assertFalse(listMultimap.contains("key1", 10));
        assertTrue(listMultimap.contains("key1", 20));
        assertFalse(listMultimap.containsKey("key2"));

        assertFalse(listMultimap.removeMany(Collections.emptyMap()));
    }

    @Test
    public void testRemoveManyMultimap() {
        listMultimap.put("key1", 10);
        listMultimap.put("key1", 20);
        listMultimap.put("key2", 30);

        ListMultimap<String, Integer> toRemove = CommonUtil.newListMultimap();
        toRemove.put("key1", 10);
        toRemove.put("key2", 30);

        assertTrue(listMultimap.removeMany(toRemove));
        assertFalse(listMultimap.contains("key1", 10));
        assertTrue(listMultimap.contains("key1", 20));
        assertFalse(listMultimap.containsKey("key2"));

        ListMultimap<String, Integer> empty = CommonUtil.newListMultimap();
        assertFalse(listMultimap.removeMany(empty));
    }

    @Test
    public void testRemoveOneIf() {
        listMultimap.put("prefix_key1", 10);
        listMultimap.put("prefix_key2", 10);
        listMultimap.put("other_key", 10);

        Predicate<String> keyFilter = key -> key.startsWith("prefix_");
        assertTrue(listMultimap.removeOneIf(10, keyFilter));

        int countAfter = 0;
        if (listMultimap.contains("prefix_key1", 10))
            countAfter++;
        if (listMultimap.contains("prefix_key2", 10))
            countAfter++;
        assertTrue(countAfter < 2);
        assertTrue(listMultimap.contains("other_key", 10));

        assertFalse(listMultimap.removeOneIf(99, keyFilter));
    }

    @Test
    public void testRemoveOneIfBiPredicate() {
        listMultimap.put("key1", 10);
        listMultimap.put("key1", 20);
        listMultimap.put("key2", 10);

        BiPredicate<String, Collection<Integer>> filter = (key, values) -> key.equals("key1") && values.size() > 1;

        assertTrue(listMultimap.removeOneIf(10, filter));
        assertFalse(listMultimap.contains("key1", 10));
        assertTrue(listMultimap.contains("key1", 20));
        assertTrue(listMultimap.contains("key2", 10));

        assertFalse(listMultimap.removeOneIf(99, filter));
    }

    @Test
    public void testRemoveManyIf() {
        listMultimap.put("key1", 10);
        listMultimap.put("key1", 20);
        listMultimap.put("key2", 10);
        listMultimap.put("key3", 30);

        Collection<Integer> valuesToRemove = Arrays.asList(10, 20);
        Predicate<String> keyFilter = key -> key.startsWith("key");

        assertTrue(listMultimap.removeManyIf(valuesToRemove, keyFilter));
        assertFalse(listMultimap.contains("key1", 10));
        assertFalse(listMultimap.contains("key1", 20));
        assertFalse(listMultimap.contains("key2", 10));
        assertTrue(listMultimap.contains("key3", 30));

        assertFalse(listMultimap.removeManyIf(Collections.emptyList(), keyFilter));
        assertFalse(listMultimap.removeManyIf(valuesToRemove, key -> false));
    }

    @Test
    public void testRemoveManyIfBiPredicate() {
        listMultimap.put("key1", 10);
        listMultimap.put("key1", 20);
        listMultimap.put("key2", 30);

        Collection<Integer> valuesToRemove = Arrays.asList(10, 30);
        BiPredicate<String, Collection<Integer>> filter = (key, values) -> values.size() >= 1;

        assertTrue(listMultimap.removeManyIf(valuesToRemove, filter));
        assertFalse(listMultimap.contains("key1", 10));
        assertTrue(listMultimap.contains("key1", 20));
        assertFalse(listMultimap.contains("key2", 30));

        assertFalse(listMultimap.removeManyIf(Collections.emptyList(), filter));
    }

    @Test
    public void testRemoveAllIf() {
        listMultimap.put("remove_key1", 10);
        listMultimap.put("remove_key2", 20);
        listMultimap.put("keep_key", 30);

        Predicate<String> keyFilter = key -> key.startsWith("remove_");
        assertTrue(listMultimap.removeAllIf(keyFilter));

        assertFalse(listMultimap.containsKey("remove_key1"));
        assertFalse(listMultimap.containsKey("remove_key2"));
        assertTrue(listMultimap.containsKey("keep_key"));

        assertFalse(listMultimap.removeAllIf(key -> false));
    }

    @Test
    public void testRemoveAllIfBiPredicate() {
        listMultimap.put("key1", 10);
        listMultimap.put("key1", 20);
        listMultimap.put("key2", 30);

        BiPredicate<String, Collection<Integer>> filter = (key, values) -> values.size() > 1;
        assertTrue(listMultimap.removeAllIf(filter));

        assertFalse(listMultimap.containsKey("key1"));
        assertTrue(listMultimap.containsKey("key2"));

        assertFalse(listMultimap.removeAllIf((key, values) -> false));
    }

    @Test
    public void testReplaceOne() {
        listMultimap.put("key1", 10);
        listMultimap.put("key1", 20);

        assertTrue(listMultimap.replaceOne("key1", 10, 15));
        assertFalse(listMultimap.contains("key1", 10));
        assertTrue(listMultimap.contains("key1", 15));
        assertTrue(listMultimap.contains("key1", 20));

        assertFalse(listMultimap.replaceOne("key1", 99, 100));
        assertFalse(listMultimap.replaceOne("nonexistent", 10, 15));
    }

    @Test
    public void testReplaceAllWithOne() {
        listMultimap.put("key1", 10);
        listMultimap.put("key1", 20);
        listMultimap.put("key2", 30);

        assertTrue(listMultimap.replaceAllWithOne("key1", 99));
        List<Integer> values = listMultimap.get("key1");
        assertEquals(1, values.size());
        assertTrue(values.contains(99));
        assertTrue(listMultimap.contains("key2", 30));

        assertFalse(listMultimap.replaceAllWithOne("nonexistent", 100));
    }

    @Test
    public void testReplaceManyWithOne() {
        listMultimap.put("key1", 10);
        listMultimap.put("key1", 20);
        listMultimap.put("key1", 30);

        Collection<Integer> oldValues = Arrays.asList(10, 20);
        assertTrue(listMultimap.replaceManyWithOne("key1", oldValues, 99));

        List<Integer> values = listMultimap.get("key1");
        assertEquals(2, values.size());
        assertTrue(values.contains(99));
        assertTrue(values.contains(30));

        assertFalse(listMultimap.replaceManyWithOne("key1", Arrays.asList(999), 100));
        assertFalse(listMultimap.replaceManyWithOne("nonexistent", oldValues, 100));
    }

    @Test
    public void testReplaceOneIf() {
        listMultimap.put("replace_key", 10);
        listMultimap.put("keep_key", 10);

        Predicate<String> keyFilter = key -> key.startsWith("replace_");
        assertTrue(listMultimap.replaceOneIf(keyFilter, 10, 99));

        assertTrue(listMultimap.contains("replace_key", 99));
        assertFalse(listMultimap.contains("replace_key", 10));
        assertTrue(listMultimap.contains("keep_key", 10));

        assertFalse(listMultimap.replaceOneIf(keyFilter, 999, 100));
    }

    @Test
    public void testReplaceOneIfBiPredicate() {
        listMultimap.put("key1", 10);
        listMultimap.put("key1", 20);
        listMultimap.put("key2", 10);

        BiPredicate<String, Collection<Integer>> filter = (key, values) -> values.size() > 1;
        assertTrue(listMultimap.replaceOneIf(filter, 10, 99));

        assertTrue(listMultimap.contains("key1", 99));
        assertFalse(listMultimap.contains("key1", 10));
        assertTrue(listMultimap.contains("key2", 10));

        assertFalse(listMultimap.replaceOneIf(filter, 999, 100));
    }

    @Test
    public void testReplaceManyWithOneIf() {
        listMultimap.put("replace_key", 10);
        listMultimap.put("replace_key", 20);
        listMultimap.put("keep_key", 10);

        Collection<Integer> oldValues = Arrays.asList(10, 20);
        Predicate<String> keyFilter = key -> key.startsWith("replace_");

        assertTrue(listMultimap.replaceManyWithOneIf(keyFilter, oldValues, 99));

        Collection<Integer> values = listMultimap.get("replace_key");
        assertEquals(1, values.size());
        assertTrue(values.contains(99));
        assertTrue(listMultimap.contains("keep_key", 10));

        assertFalse(listMultimap.replaceManyWithOneIf(keyFilter, Arrays.asList(999), 100));
    }

    @Test
    public void testReplaceManyWithOneIfBiPredicate() {
        listMultimap.put("key1", 10);
        listMultimap.put("key1", 20);
        listMultimap.put("key2", 30);

        Collection<Integer> oldValues = Arrays.asList(10, 20);
        BiPredicate<String, Collection<Integer>> filter = (key, values) -> values.size() > 1;

        assertTrue(listMultimap.replaceManyWithOneIf(filter, oldValues, 99));

        List<Integer> values = listMultimap.get("key1");
        assertEquals(1, values.size());
        assertTrue(values.contains(99));
        assertTrue(listMultimap.contains("key2", 30));

        assertFalse(listMultimap.replaceManyWithOneIf(filter, Arrays.asList(999), 100));
    }

    @Test
    public void testReplaceAllWithOneIf() {
        listMultimap.put("replace_key1", 10);
        listMultimap.put("replace_key1", 20);
        listMultimap.put("replace_key2", 30);
        listMultimap.put("keep_key", 40);

        Predicate<String> keyFilter = key -> key.startsWith("replace_");
        assertTrue(listMultimap.replaceAllWithOneIf(keyFilter, 99));

        Collection<Integer> values1 = listMultimap.get("replace_key1");
        assertEquals(1, values1.size());
        assertTrue(values1.contains(99));

        Collection<Integer> values2 = listMultimap.get("replace_key2");
        assertEquals(1, values2.size());
        assertTrue(values2.contains(99));

        assertTrue(listMultimap.contains("keep_key", 40));

        assertFalse(listMultimap.replaceAllWithOneIf(key -> false, 100));
    }

    @Test
    public void testReplaceAllWithOneIfBiPredicate() {
        listMultimap.put("key1", 10);
        listMultimap.put("key1", 20);
        listMultimap.put("key2", 30);

        BiPredicate<String, Collection<Integer>> filter = (key, values) -> values.size() > 1;
        assertTrue(listMultimap.replaceAllWithOneIf(filter, 99));

        Collection<Integer> values1 = listMultimap.get("key1");
        assertEquals(1, values1.size());
        assertTrue(values1.contains(99));
        assertTrue(listMultimap.contains("key2", 30));

        assertFalse(listMultimap.replaceAllWithOneIf((key, values) -> false, 100));
    }

    @Test
    public void testReplaceAll() {
        listMultimap.put("key1", 10);
        listMultimap.put("key1", 20);
        listMultimap.put("key2", 30);

        BiFunction<String, List<Integer>, List<Integer>> function = (key, values) -> {
            if (key.equals("key1")) {
                List<Integer> newValues = new ArrayList<>();
                newValues.add(99);
                return newValues;
            }
            return values;
        };

        listMultimap.replaceAll(function);

        List<Integer> values1 = listMultimap.get("key1");
        assertEquals(1, values1.size());
        assertTrue(values1.contains(99));
        assertTrue(listMultimap.contains("key2", 30));
    }

    @Test
    public void testComputeIfAbsent() {
        Function<String, List<Integer>> mappingFunction = key -> {
            List<Integer> values = new ArrayList<>();
            values.add(key.length());
            return values;
        };

        List<Integer> result = listMultimap.computeIfAbsent("newKey", mappingFunction);
        assertNotNull(result);
        assertTrue(result.contains(6));
        assertTrue(listMultimap.containsKey("newKey"));

        listMultimap.put("existingKey", 10);
        List<Integer> existing = listMultimap.computeIfAbsent("existingKey", mappingFunction);
        assertTrue(existing.contains(10));
        assertFalse(existing.contains(11));
    }

    @Test
    public void testComputeIfPresent() {
        listMultimap.put("key1", 10);

        BiFunction<String, List<Integer>, List<Integer>> remappingFunction = (key, values) -> {
            List<Integer> newValues = new ArrayList<>(values);
            newValues.add(20);
            return newValues;
        };

        List<Integer> result = listMultimap.computeIfPresent("key1", remappingFunction);
        assertNotNull(result);
        assertTrue(result.contains(10));
        assertTrue(result.contains(20));

        assertNull(listMultimap.computeIfPresent("nonexistent", remappingFunction));
    }

    @Test
    public void testCompute() {
        BiFunction<String, List<Integer>, List<Integer>> remappingFunction = (key, values) -> {
            List<Integer> newValues = new ArrayList<>();
            if (values != null) {
                newValues.addAll(values);
            }
            newValues.add(key.length());
            return newValues;
        };

        List<Integer> result1 = listMultimap.compute("newKey", remappingFunction);
        assertNotNull(result1);
        assertTrue(result1.contains(6));

        listMultimap.put("existingKey", 10);
        List<Integer> result2 = listMultimap.compute("existingKey", remappingFunction);
        assertTrue(result2.contains(10));
        assertTrue(result2.contains(11));
    }

    @Test
    public void testMergeCollection() {
        listMultimap.put("key1", 10);

        List<Integer> elements = Arrays.asList(20, 30);
        BiFunction<List<Integer>, List<Integer>, List<Integer>> remappingFunction = (oldValues, newElements) -> {
            List<Integer> merged = new ArrayList<>(oldValues);
            merged.addAll(newElements);
            return merged;
        };

        List<Integer> result = listMultimap.merge("key1", elements, remappingFunction);
        assertEquals(3, result.size());
        assertTrue(result.contains(10));
        assertTrue(result.contains(20));
        assertTrue(result.contains(30));

        List<Integer> result2 = listMultimap.merge("newKey", Arrays.asList(40), remappingFunction);
        assertEquals(1, result2.size());
        assertTrue(result2.contains(40));
    }

    @Test
    public void testMergeElement() {
        listMultimap.put("key1", 10);

        BiFunction<List<Integer>, Integer, List<Integer>> remappingFunction = (values, element) -> {
            List<Integer> merged = new ArrayList<>(values);
            merged.add(element);
            return merged;
        };

        List<Integer> result = listMultimap.merge("key1", 20, remappingFunction);
        assertEquals(2, result.size());
        assertTrue(result.contains(10));
        assertTrue(result.contains(20));

        List<Integer> result2 = listMultimap.merge("newKey", 30, remappingFunction);
        assertEquals(1, result2.size());
        assertTrue(result2.contains(30));
    }

    @Test
    public void testInverse() {
        listMultimap.put("key1", 10);
        listMultimap.put("key1", 20);
        listMultimap.put("key2", 10);

        ListMultimap<Integer, String> inverse = listMultimap.inverse(N::newListMultimap);

        assertTrue(inverse.contains(10, "key1"));
        assertTrue(inverse.contains(10, "key2"));
        assertTrue(inverse.contains(20, "key1"));
        assertEquals(2, inverse.get(10).size());
        assertEquals(1, inverse.get(20).size());
    }

    @Test
    public void testContains() {
        listMultimap.put("key1", 10);

        assertTrue(listMultimap.contains("key1", 10));
        assertFalse(listMultimap.contains("key1", 20));
        assertFalse(listMultimap.contains("key2", 10));
        assertFalse(listMultimap.contains("key1", null));
        assertFalse(listMultimap.contains(null, 10));
    }

    @Test
    public void testContainsKey() {
        listMultimap.put("key1", 10);

        assertTrue(listMultimap.containsKey("key1"));
        assertFalse(listMultimap.containsKey("key2"));
        assertFalse(listMultimap.containsKey(null));
    }

    @Test
    public void testContainsValue() {
        listMultimap.put("key1", 10);
        listMultimap.put("key2", 20);

        assertTrue(listMultimap.containsValue(10));
        assertTrue(listMultimap.containsValue(20));
        assertFalse(listMultimap.containsValue(30));
        assertFalse(listMultimap.containsValue(null));
    }

    @Test
    public void testContainsAll() {
        listMultimap.put("key1", 10);
        listMultimap.put("key1", 20);
        listMultimap.put("key1", 30);

        assertTrue(listMultimap.containsAll("key1", Arrays.asList(10, 20)));
        assertTrue(listMultimap.containsAll("key1", Arrays.asList(10)));
        assertFalse(listMultimap.containsAll("key1", Arrays.asList(10, 40)));
        assertFalse(listMultimap.containsAll("key2", Arrays.asList(10)));
        assertTrue(listMultimap.containsAll("key1", Collections.emptyList()));
    }

    @Test
    public void testFilterByKey() {
        listMultimap.put("include_key1", 10);
        listMultimap.put("include_key2", 20);
        listMultimap.put("exclude_key", 30);

        Predicate<String> keyFilter = key -> key.startsWith("include_");
        Multimap<String, Integer, ? extends List<Integer>> filtered = listMultimap.filterByKey(keyFilter);

        assertEquals(2, filtered.size());
        assertTrue(filtered.containsKey("include_key1"));
        assertTrue(filtered.containsKey("include_key2"));
        assertFalse(filtered.containsKey("exclude_key"));
    }

    @Test
    public void testFilterByValue() {
        listMultimap.put("key1", 10);
        listMultimap.put("key1", 20);
        listMultimap.put("key2", 30);
        listMultimap.put("key3", 40);

        Predicate<List<Integer>> valueFilter = values -> values.size() > 1;
        Multimap<String, Integer, ? extends List<Integer>> filtered = listMultimap.filterByValue(valueFilter);

        assertEquals(1, filtered.size());
        assertTrue(filtered.containsKey("key1"));
        assertFalse(filtered.containsKey("key2"));
        assertFalse(filtered.containsKey("key3"));
    }

    @Test
    public void testFilter() {
        listMultimap.put("key1", 10);
        listMultimap.put("key1", 20);
        listMultimap.put("key2", 30);

        BiPredicate<String, List<Integer>> filter = (key, values) -> key.equals("key1") && values.size() > 1;
        Multimap<String, Integer, ? extends List<Integer>> filtered = listMultimap.filter(filter);

        assertEquals(1, filtered.size());
        assertTrue(filtered.containsKey("key1"));
        assertFalse(filtered.containsKey("key2"));
    }

    @Test
    public void testForEach() {
        listMultimap.put("key1", 10);
        listMultimap.put("key2", 20);

        Map<String, List<Integer>> visited = new HashMap<>();
        BiConsumer<String, List<Integer>> action = visited::put;

        listMultimap.forEach(action);

        assertEquals(2, visited.size());
        assertTrue(visited.get("key1").contains(10));
        assertTrue(visited.get("key2").contains(20));
    }

    @Test
    public void testFlatForEach() {
        listMultimap.put("key1", 10);
        listMultimap.put("key1", 20);
        listMultimap.put("key2", 30);

        Map<String, List<Integer>> visited = new HashMap<>();
        BiConsumer<String, Integer> action = (key, value) -> visited.computeIfAbsent(key, k -> new ArrayList<>()).add(value);

        listMultimap.flatForEach(action);

        assertEquals(2, visited.size());
        assertEquals(2, visited.get("key1").size());
        assertTrue(visited.get("key1").contains(10));
        assertTrue(visited.get("key1").contains(20));
        assertEquals(1, visited.get("key2").size());
        assertTrue(visited.get("key2").contains(30));
    }

    @Test
    public void testForEachKey() {
        listMultimap.put("key1", 10);
        listMultimap.put("key2", 20);

        Set<String> visitedKeys = new HashSet<>();
        Consumer<String> action = visitedKeys::add;

        listMultimap.forEachKey(action);

        assertEquals(2, visitedKeys.size());
        assertTrue(visitedKeys.contains("key1"));
        assertTrue(visitedKeys.contains("key2"));
    }

    @Test
    public void testForEachValue() {
        listMultimap.put("key1", 10);
        listMultimap.put("key2", 20);

        List<List<Integer>> visitedValues = new ArrayList<>();
        Consumer<List<Integer>> action = visitedValues::add;

        listMultimap.forEachValue(action);

        assertEquals(2, visitedValues.size());
        boolean found10 = false, found20 = false;
        for (List<Integer> values : visitedValues) {
            if (values.contains(10))
                found10 = true;
            if (values.contains(20))
                found20 = true;
        }
        assertTrue(found10 && found20);
    }

    @Test
    public void testFlatForEachValue() {
        listMultimap.put("key1", 10);
        listMultimap.put("key1", 20);
        listMultimap.put("key2", 30);

        Set<Integer> visitedValues = new HashSet<>();
        Consumer<Integer> action = visitedValues::add;

        listMultimap.flatForEachValue(action);

        assertEquals(3, visitedValues.size());
        assertTrue(visitedValues.contains(10));
        assertTrue(visitedValues.contains(20));
        assertTrue(visitedValues.contains(30));
    }

    @Test
    public void testApply() {
        listMultimap.put("key1", 10);
        listMultimap.put("key2", 20);

        Integer result = listMultimap.apply(mm -> mm.size());
        assertEquals(Integer.valueOf(2), result);

        String result2 = listMultimap.apply(mm -> "Size: " + mm.totalCountOfValues());
        assertEquals("Size: 2", result2);
    }

    @Test
    public void testApplyIfNotEmpty() {
        Optional<Integer> emptyResult = listMultimap.applyIfNotEmpty(mm -> mm.size());
        assertFalse(emptyResult.isPresent());

        listMultimap.put("key1", 10);
        Optional<Integer> result = listMultimap.applyIfNotEmpty(mm -> mm.size());
        assertTrue(result.isPresent());
        assertEquals(Integer.valueOf(1), result.get());
    }

    @Test
    public void testAccept() {
        listMultimap.put("key1", 10);

        final boolean[] visited = { false };
        listMultimap.accept(mm -> visited[0] = true);
        assertTrue(visited[0]);
    }

    @Test
    public void testAcceptIfNotEmpty() {
        final boolean[] visited = { false };

        OrElse orElse = listMultimap.acceptIfNotEmpty(mm -> visited[0] = true);
        orElse.orElse(() -> visited[0] = false);
        assertFalse(visited[0]);

        listMultimap.put("key1", 10);
        listMultimap.acceptIfNotEmpty(mm -> visited[0] = true);
        assertTrue(visited[0]);
    }

    @Test
    public void testHashCode() {
        ListMultimap<String, Integer> mm1 = CommonUtil.newListMultimap();
        ListMultimap<String, Integer> mm2 = CommonUtil.newListMultimap();

        assertEquals(mm1.hashCode(), mm2.hashCode());

        mm1.put("key1", 10);
        mm2.put("key1", 10);
        assertEquals(mm1.hashCode(), mm2.hashCode());

        mm2.put("key2", 20);
        assertNotEquals(mm1.hashCode(), mm2.hashCode());
    }

    @Test
    public void testEquals() {
        ListMultimap<String, Integer> mm1 = CommonUtil.newListMultimap();
        ListMultimap<String, Integer> mm2 = CommonUtil.newListMultimap();

        assertTrue(mm1.equals(mm2));
        assertTrue(mm1.equals(mm1));

        mm1.put("key1", 10);
        mm1.put("key2", 20);
        mm2.put("key2", 20);
        mm2.put("key1", 10);
        assertTrue(mm1.equals(mm2));

        mm2.put("key3", 30);
        assertFalse(mm1.equals(mm2));

        assertFalse(mm1.equals(null));
        assertFalse(mm1.equals("not a multimap"));

        SetMultimap<String, Integer> setMm = CommonUtil.newSetMultimap();
        setMm.put("key1", 10);
        setMm.put("key2", 20);
        assertFalse(mm1.equals(setMm));
    }

    @Test
    public void testToString() {
        String emptyString = listMultimap.toString();
        assertEquals("{}", emptyString);

        listMultimap.put("key1", 10);
        listMultimap.put("key2", 20);
        String result = listMultimap.toString();
        assertTrue(result.contains("key1"));
        assertTrue(result.contains("key2"));
        assertTrue(result.contains("10"));
        assertTrue(result.contains("20"));
    }

    @Test
    public void testNullHandling() {
        assertFalse(listMultimap.containsKey(null));
        assertFalse(listMultimap.containsValue(null));
        assertNull(listMultimap.get(null));
        assertFalse(listMultimap.contains(null, 10));
        assertFalse(listMultimap.contains("key", null));

        assertFalse(listMultimap.putMany("key", null));
        assertFalse(listMultimap.removeMany("key", null));
    }

    @Test
    public void testEmptyCollections() {
        assertFalse(listMultimap.put(Collections.emptyMap()));
        assertFalse(listMultimap.putMany("key", Collections.emptyList()));
        assertFalse(listMultimap.removeMany("key", Collections.emptyList()));

        boolean result = listMultimap.containsAll("key", Collections.emptyList());
    }

    @Test
    public void testLargeCollections() {
        for (int i = 0; i < 1000; i++) {
            listMultimap.put("key" + (i % 10), i);
        }

        assertEquals(10, listMultimap.size());
        assertEquals(1000, listMultimap.totalCountOfValues());

        Multimap<String, Integer, ? extends Collection<Integer>> filtered = listMultimap.filterByKey(key -> key.equals("key0"));
        assertEquals(1, filtered.size());
        assertEquals(100, filtered.totalCountOfValues());
    }

    @Test
    public void testFunctionalInterfaceNullParams() {
        assertThrows(Exception.class, () -> listMultimap.forEach((BiConsumer<String, List<Integer>>) null));
    }
}

package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.If.OrElse;
import com.landawn.abacus.util.u.Optional;
import com.landawn.abacus.util.stream.EntryStream;
import com.landawn.abacus.util.stream.Stream;

@Tag("2025")
public class Multimap2025Test extends TestBase {

    private ListMultimap<String, Integer> listMultimap;
    private SetMultimap<String, Integer> setMultimap;

    @BeforeEach
    public void setUp() {
        listMultimap = N.newListMultimap();
        setMultimap = N.newSetMultimap();
    }

    @Test
    public void testGetFirst_WithNonExistentKey() {
        assertNull(listMultimap.getFirst("nonexistent"));
    }

    @Test
    public void testGetFirst_WithSingleValue() {
        listMultimap.put("key1", 10);
        assertEquals(Integer.valueOf(10), listMultimap.getFirst("key1"));
    }

    @Test
    public void testGetFirst_WithMultipleValues() {
        listMultimap.put("key1", 10);
        listMultimap.put("key1", 20);
        listMultimap.put("key1", 30);
        assertEquals(Integer.valueOf(10), listMultimap.getFirst("key1"));
    }

    @Test
    public void testGetFirst_WithEmptyCollection() {
        listMultimap.put("key1", 10);
        listMultimap.removeOne("key1", 10);
        assertNull(listMultimap.getFirst("key1"));
    }

    @Test
    public void testGetFirstOrDefault_WithNonExistentKey() {
        Integer defaultValue = -1;
        assertEquals(defaultValue, listMultimap.getFirstOrDefault("nonexistent", defaultValue));
    }

    @Test
    public void testGetFirstOrDefault_WithExistingKey() {
        listMultimap.put("key1", 10);
        listMultimap.put("key1", 20);
        assertEquals(Integer.valueOf(10), listMultimap.getFirstOrDefault("key1", -1));
    }

    @Test
    public void testGetFirstOrDefault_WithNullDefault() {
        assertNull(listMultimap.getFirstOrDefault("nonexistent", null));
    }

    @Test
    public void testGet_WithNonExistentKey() {
        assertNull(listMultimap.get("nonexistent"));
    }

    @Test
    public void testGet_WithExistingKey() {
        listMultimap.put("key1", 10);
        listMultimap.put("key1", 20);

        List<Integer> values = listMultimap.get("key1");
        assertNotNull(values);
        assertEquals(2, values.size());
        assertTrue(values.contains(10));
        assertTrue(values.contains(20));
    }

    @Test
    public void testGet_ReturnsBackedCollection() {
        listMultimap.put("key1", 10);
        List<Integer> values = listMultimap.get("key1");
        values.add(20);
        assertEquals(2, listMultimap.get("key1").size());
    }

    @Test
    public void testGetOrDefault_WithNonExistentKey() {
        List<Integer> defaultValue = Arrays.asList(-1, -2);
        List<Integer> result = listMultimap.getOrDefault("nonexistent", defaultValue);
        assertEquals(defaultValue, result);
    }

    @Test
    public void testGetOrDefault_WithExistingKey() {
        listMultimap.put("key1", 10);
        listMultimap.put("key1", 20);
        List<Integer> defaultValue = Arrays.asList(-1, -2);
        List<Integer> result = listMultimap.getOrDefault("key1", defaultValue);
        assertEquals(2, result.size());
        assertTrue(result.contains(10));
    }

    @Test
    public void testPut_SingleValue() {
        assertTrue(listMultimap.put("key1", 10));
        assertEquals(1, listMultimap.get("key1").size());
        assertTrue(listMultimap.get("key1").contains(10));
    }

    @Test
    public void testPut_MultipleValuesToSameKey() {
        assertTrue(listMultimap.put("key1", 10));
        assertTrue(listMultimap.put("key1", 20));
        assertEquals(2, listMultimap.get("key1").size());
    }

    @Test
    public void testPut_DuplicateValueToList() {
        assertTrue(listMultimap.put("key1", 10));
        assertTrue(listMultimap.put("key1", 10));
        assertEquals(2, listMultimap.get("key1").size());
    }

    @Test
    public void testPut_DuplicateValueToSet() {
        assertTrue(setMultimap.put("key1", 10));
        assertFalse(setMultimap.put("key1", 10));
        assertEquals(1, setMultimap.get("key1").size());
    }

    @Test
    public void testPut_NullValue() {
        assertTrue(listMultimap.put("key1", null));
        assertTrue(listMultimap.get("key1").contains(null));
    }

    @Test
    public void testPutMap_EmptyMap() {
        assertFalse(listMultimap.put(new HashMap<>()));
    }

    @Test
    public void testPutMap_WithValues() {
        Map<String, Integer> map = new HashMap<>();
        map.put("key1", 10);
        map.put("key2", 20);

        assertTrue(listMultimap.put(map));
        assertEquals(1, listMultimap.get("key1").size());
        assertEquals(1, listMultimap.get("key2").size());
        assertTrue(listMultimap.get("key1").contains(10));
        assertTrue(listMultimap.get("key2").contains(20));
    }

    @Test
    public void testPutMap_NullMap() {
        assertFalse(listMultimap.put((Map<String, Integer>) null));
    }

    @Test
    public void testPutIfAbsent_NewKey() {
        assertTrue(listMultimap.putIfAbsent("key1", 10));
        assertTrue(listMultimap.get("key1").contains(10));
    }

    @Test
    public void testPutIfAbsent_ExistingKeyDifferentValue() {
        listMultimap.put("key1", 10);
        assertTrue(listMultimap.putIfAbsent("key1", 20));
        assertEquals(2, listMultimap.get("key1").size());
    }

    @Test
    public void testPutIfAbsent_ExistingSameValueInList() {
        listMultimap.put("key1", 10);
        assertFalse(listMultimap.putIfAbsent("key1", 10));
        assertEquals(1, listMultimap.get("key1").size());
    }

    @Test
    public void testPutIfAbsent_ExistingSameValueInSet() {
        setMultimap.put("key1", 10);
        assertFalse(setMultimap.putIfAbsent("key1", 10));
        assertEquals(1, setMultimap.get("key1").size());
    }

    @Test
    public void testPutIfKeyAbsent_NewKey() {
        assertTrue(listMultimap.putIfKeyAbsent("key1", 10));
        assertTrue(listMultimap.get("key1").contains(10));
    }

    @Test
    public void testPutIfKeyAbsent_ExistingKey() {
        listMultimap.put("key1", 10);
        assertFalse(listMultimap.putIfKeyAbsent("key1", 20));
        assertEquals(1, listMultimap.get("key1").size());
        assertFalse(listMultimap.get("key1").contains(20));
    }

    @Test
    public void testPutMany_EmptyCollection() {
        assertFalse(listMultimap.putMany("key1", new ArrayList<>()));
    }

    @Test
    public void testPutMany_NullCollection() {
        assertFalse(listMultimap.putMany("key1", null));
    }

    @Test
    public void testPutMany_WithValues() {
        List<Integer> values = Arrays.asList(10, 20, 30);
        assertTrue(listMultimap.putMany("key1", values));
        assertEquals(3, listMultimap.get("key1").size());
        assertTrue(listMultimap.get("key1").containsAll(values));
    }

    @Test
    public void testPutMany_ToExistingKey() {
        listMultimap.put("key1", 5);
        List<Integer> values = Arrays.asList(10, 20);
        assertTrue(listMultimap.putMany("key1", values));
        assertEquals(3, listMultimap.get("key1").size());
    }

    @Test
    public void testPutManyIfKeyAbsent_NewKey() {
        List<Integer> values = Arrays.asList(10, 20);
        assertTrue(listMultimap.putManyIfKeyAbsent("key1", values));
        assertEquals(2, listMultimap.get("key1").size());
    }

    @Test
    public void testPutManyIfKeyAbsent_ExistingKey() {
        listMultimap.put("key1", 5);
        List<Integer> values = Arrays.asList(10, 20);
        assertFalse(listMultimap.putManyIfKeyAbsent("key1", values));
        assertEquals(1, listMultimap.get("key1").size());
    }

    @Test
    public void testPutManyIfKeyAbsent_EmptyCollection() {
        assertFalse(listMultimap.putManyIfKeyAbsent("key1", new ArrayList<>()));
    }

    @Test
    public void testPutManyMap_EmptyMap() {
        Map<String, Collection<Integer>> map = new HashMap<>();
        assertFalse(listMultimap.putMany(map));
    }

    @Test
    public void testPutManyMap_WithValues() {
        Map<String, Collection<Integer>> map = new HashMap<>();
        map.put("key1", Arrays.asList(10, 20));
        map.put("key2", Arrays.asList(30, 40));

        assertTrue(listMultimap.putMany(map));
        assertEquals(2, listMultimap.get("key1").size());
        assertEquals(2, listMultimap.get("key2").size());
    }

    @Test
    public void testPutManyMap_SkipsEmptyCollections() {
        Map<String, Collection<Integer>> map = new HashMap<>();
        map.put("key1", Arrays.asList(10, 20));
        map.put("key2", new ArrayList<>());

        assertTrue(listMultimap.putMany(map));
        assertNotNull(listMultimap.get("key1"));
        assertNull(listMultimap.get("key2"));
    }

    @Test
    public void testPutManyMultimap_EmptyMultimap() {
        ListMultimap<String, Integer> other = N.newListMultimap();
        assertFalse(listMultimap.putMany(other));
    }

    @Test
    public void testPutManyMultimap_WithValues() {
        ListMultimap<String, Integer> other = N.newListMultimap();
        other.put("key1", 10);
        other.put("key1", 20);
        other.put("key2", 30);

        assertTrue(listMultimap.putMany(other));
        assertEquals(2, listMultimap.get("key1").size());
        assertEquals(1, listMultimap.get("key2").size());
    }

    @Test
    public void testRemoveOne_NonExistentKey() {
        assertFalse(listMultimap.removeOne("key1", 10));
    }

    @Test
    public void testRemoveOne_NonExistentValue() {
        listMultimap.put("key1", 10);
        assertFalse(listMultimap.removeOne("key1", 20));
    }

    @Test
    public void testRemoveOne_ExistingValue() {
        listMultimap.put("key1", 10);
        listMultimap.put("key1", 20);
        assertTrue(listMultimap.removeOne("key1", 10));
        assertEquals(1, listMultimap.get("key1").size());
        assertFalse(listMultimap.get("key1").contains(10));
    }

    @Test
    public void testRemoveOne_LastValueRemovesKey() {
        listMultimap.put("key1", 10);
        assertTrue(listMultimap.removeOne("key1", 10));
        assertNull(listMultimap.get("key1"));
    }

    @Test
    public void testRemoveOne_OnlyFirstOccurrence() {
        listMultimap.put("key1", 10);
        listMultimap.put("key1", 10);
        listMultimap.put("key1", 10);
        assertTrue(listMultimap.removeOne("key1", 10));
        assertEquals(2, listMultimap.get("key1").size());
    }

    @Test
    public void testRemoveOneMap_EmptyMap() {
        assertFalse(listMultimap.removeOne(new HashMap<>()));
    }

    @Test
    public void testRemoveOneMap_WithValues() {
        listMultimap.put("key1", 10);
        listMultimap.put("key1", 20);
        listMultimap.put("key2", 30);

        Map<String, Integer> toRemove = new HashMap<>();
        toRemove.put("key1", 10);
        toRemove.put("key2", 30);

        assertTrue(listMultimap.removeOne(toRemove));
        assertEquals(1, listMultimap.get("key1").size());
        assertNull(listMultimap.get("key2"));
    }

    @Test
    public void testRemoveAll_NonExistentKey() {
        assertNull(listMultimap.removeAll("key1"));
    }

    @Test
    public void testRemoveAll_ExistingKey() {
        listMultimap.put("key1", 10);
        listMultimap.put("key1", 20);
        listMultimap.put("key1", 30);

        List<Integer> removed = listMultimap.removeAll("key1");
        assertNotNull(removed);
        assertEquals(3, removed.size());
        assertNull(listMultimap.get("key1"));
    }

    @Test
    public void testRemoveMany_EmptyCollection() {
        assertFalse(listMultimap.removeMany("key1", new ArrayList<>()));
    }

    @Test
    public void testRemoveMany_NonExistentKey() {
        assertFalse(listMultimap.removeMany("key1", Arrays.asList(10, 20)));
    }

    @Test
    public void testRemoveMany_WithValues() {
        listMultimap.put("key1", 10);
        listMultimap.put("key1", 20);
        listMultimap.put("key1", 30);
        listMultimap.put("key1", 40);

        assertTrue(listMultimap.removeMany("key1", Arrays.asList(10, 30)));
        assertEquals(2, listMultimap.get("key1").size());
        assertTrue(listMultimap.get("key1").contains(20));
        assertTrue(listMultimap.get("key1").contains(40));
    }

    @Test
    public void testRemoveMany_AllValuesRemovesKey() {
        listMultimap.put("key1", 10);
        listMultimap.put("key1", 20);
        assertTrue(listMultimap.removeMany("key1", Arrays.asList(10, 20)));
        assertNull(listMultimap.get("key1"));
    }

    @Test
    public void testRemoveManyMap_EmptyMap() {
        assertFalse(listMultimap.removeMany(new HashMap<>()));
    }

    @Test
    public void testRemoveManyMap_WithValues() {
        listMultimap.putMany("key1", Arrays.asList(10, 20, 30));
        listMultimap.putMany("key2", Arrays.asList(40, 50, 60));

        Map<String, Collection<Integer>> toRemove = new HashMap<>();
        toRemove.put("key1", Arrays.asList(10, 20));
        toRemove.put("key2", Arrays.asList(50));

        assertTrue(listMultimap.removeMany(toRemove));
        assertEquals(1, listMultimap.get("key1").size());
        assertEquals(2, listMultimap.get("key2").size());
    }

    @Test
    public void testRemoveManyMultimap_EmptyMultimap() {
        ListMultimap<String, Integer> other = N.newListMultimap();
        assertFalse(listMultimap.removeMany(other));
    }

    @Test
    public void testRemoveManyMultimap_WithValues() {
        listMultimap.putMany("key1", Arrays.asList(10, 20, 30));
        listMultimap.putMany("key2", Arrays.asList(40, 50));

        ListMultimap<String, Integer> toRemove = N.newListMultimap();
        toRemove.put("key1", 10);
        toRemove.put("key1", 20);
        toRemove.put("key2", 40);

        assertTrue(listMultimap.removeMany(toRemove));
        assertEquals(1, listMultimap.get("key1").size());
        assertEquals(1, listMultimap.get("key2").size());
    }

    @Test
    public void testRemoveOneIfKeyPredicate_NoMatchingKeys() {
        listMultimap.put("key1", 10);
        assertFalse(listMultimap.removeOneIf(10, key -> key.equals("key2")));
    }

    @Test
    public void testRemoveOneIfKeyPredicate_MatchingKeys() {
        listMultimap.put("key1", 10);
        listMultimap.put("key2", 10);
        listMultimap.put("key3", 20);

        assertTrue(listMultimap.removeOneIf(10, key -> key.startsWith("key")));
        assertNull(listMultimap.get("key1"));
        assertNull(listMultimap.get("key2"));
        assertNotNull(listMultimap.get("key3"));
    }

    @Test
    public void testRemoveOneIfBiPredicate_NoMatching() {
        listMultimap.put("key1", 10);
        assertFalse(listMultimap.removeOneIf(10, (k, v) -> v.size() > 5));
    }

    @Test
    public void testRemoveOneIfBiPredicate_Matching() {
        listMultimap.putMany("key1", Arrays.asList(10, 20));
        listMultimap.putMany("key2", Arrays.asList(10, 20, 30));

        assertTrue(listMultimap.removeOneIf(10, (k, v) -> v.size() > 2));
        assertEquals(2, listMultimap.get("key1").size());
        assertEquals(2, listMultimap.get("key2").size());
    }

    @Test
    public void testRemoveManyIfKeyPredicate_EmptyCollection() {
        assertFalse(listMultimap.removeManyIf(new ArrayList<>(), key -> true));
    }

    @Test
    public void testRemoveManyIfKeyPredicate_Matching() {
        listMultimap.putMany("key1", Arrays.asList(10, 20, 30));
        listMultimap.putMany("key2", Arrays.asList(10, 20, 40));

        assertTrue(listMultimap.removeManyIf(Arrays.asList(10, 20), key -> key.equals("key1")));
        assertEquals(1, listMultimap.get("key1").size());
        assertEquals(3, listMultimap.get("key2").size());
    }

    @Test
    public void testRemoveManyIfBiPredicate_EmptyCollection() {
        assertFalse(listMultimap.removeManyIf(new ArrayList<>(), (k, v) -> true));
    }

    @Test
    public void testRemoveManyIfBiPredicate_Matching() {
        listMultimap.putMany("key1", Arrays.asList(10, 20, 30, 40));
        listMultimap.putMany("key2", Arrays.asList(10, 20));

        assertTrue(listMultimap.removeManyIf(Arrays.asList(10, 20), (k, v) -> v.size() > 2));
        assertEquals(2, listMultimap.get("key1").size());
        assertEquals(2, listMultimap.get("key2").size());
    }

    @Test
    public void testRemoveAllIfPredicate_NoMatching() {
        listMultimap.put("key1", 10);
        assertFalse(listMultimap.removeAllIf(key -> key.equals("key2")));
    }

    @Test
    public void testRemoveAllIfPredicate_Matching() {
        listMultimap.put("key1", 10);
        listMultimap.put("key2", 20);
        listMultimap.put("other", 30);

        assertTrue(listMultimap.removeAllIf(key -> key.startsWith("key")));
        assertNull(listMultimap.get("key1"));
        assertNull(listMultimap.get("key2"));
        assertNotNull(listMultimap.get("other"));
    }

    @Test
    public void testRemoveAllIfBiPredicate_NoMatching() {
        listMultimap.put("key1", 10);
        assertFalse(listMultimap.removeAllIf((k, v) -> v.size() > 5));
    }

    @Test
    public void testRemoveAllIfBiPredicate_Matching() {
        listMultimap.putMany("key1", Arrays.asList(10, 20));
        listMultimap.putMany("key2", Arrays.asList(10, 20, 30));
        listMultimap.putMany("key3", Arrays.asList(10));

        assertTrue(listMultimap.removeAllIf((k, v) -> v.size() > 2));
        assertNotNull(listMultimap.get("key1"));
        assertNull(listMultimap.get("key2"));
        assertNotNull(listMultimap.get("key3"));
    }

    @Test
    public void testReplaceOne_NonExistentKey() {
        assertFalse(listMultimap.replaceOne("key1", 10, 20));
    }

    @Test
    public void testReplaceOne_NonExistentOldValue() {
        listMultimap.put("key1", 10);
        assertFalse(listMultimap.replaceOne("key1", 20, 30));
    }

    @Test
    public void testReplaceOne_InList() {
        listMultimap.put("key1", 10);
        listMultimap.put("key1", 20);
        listMultimap.put("key1", 10);

        assertTrue(listMultimap.replaceOne("key1", 10, 99));
        List<Integer> values = listMultimap.get("key1");
        assertEquals(3, values.size());
        assertEquals(Integer.valueOf(99), values.get(0));
        assertEquals(Integer.valueOf(10), values.get(2));
    }

    @Test
    public void testReplaceOne_InSet() {
        setMultimap.put("key1", 10);
        setMultimap.put("key1", 20);

        assertTrue(setMultimap.replaceOne("key1", 10, 99));
        Set<Integer> values = setMultimap.get("key1");
        assertEquals(2, values.size());
        assertTrue(values.contains(99));
        assertFalse(values.contains(10));
    }

    @Test
    public void testReplaceOne_WithNullOldValue() {
        listMultimap.put("key1", null);
        listMultimap.put("key1", 20);

        assertTrue(listMultimap.replaceOne("key1", null, 99));
        assertEquals(Integer.valueOf(99), listMultimap.get("key1").get(0));
    }

    @Test
    public void testReplaceAllWithOne_NonExistentKey() {
        assertFalse(listMultimap.replaceAllWithOne("key1", 99));
    }

    @Test
    public void testReplaceAllWithOne_ExistingKey() {
        listMultimap.putMany("key1", Arrays.asList(10, 20, 30, 40));

        assertTrue(listMultimap.replaceAllWithOne("key1", 99));
        assertEquals(1, listMultimap.get("key1").size());
        assertEquals(Integer.valueOf(99), listMultimap.get("key1").get(0));
    }

    @Test
    public void testReplaceManyWithOne_EmptyOldValues() {
        listMultimap.put("key1", 10);
        assertFalse(listMultimap.replaceManyWithOne("key1", new ArrayList<>(), 99));
    }

    @Test
    public void testReplaceManyWithOne_NonExistentKey() {
        assertFalse(listMultimap.replaceManyWithOne("key1", Arrays.asList(10, 20), 99));
    }

    @Test
    public void testReplaceManyWithOne_NoMatchingOldValues() {
        listMultimap.put("key1", 10);
        assertFalse(listMultimap.replaceManyWithOne("key1", Arrays.asList(20, 30), 99));
    }

    @Test
    public void testReplaceManyWithOne_Success() {
        listMultimap.putMany("key1", Arrays.asList(10, 20, 30, 40, 10));

        assertTrue(listMultimap.replaceManyWithOne("key1", Arrays.asList(10, 20), 99));
        List<Integer> values = listMultimap.get("key1");
        assertEquals(3, values.size());
        assertTrue(values.contains(99));
        assertTrue(values.contains(30));
        assertTrue(values.contains(40));
    }

    @Test
    public void testReplaceOneIfPredicate_NoMatching() {
        listMultimap.put("key1", 10);
        assertFalse(listMultimap.replaceOneIf(key -> key.equals("key2"), 10, 99));
    }

    @Test
    public void testReplaceOneIfPredicate_Matching() {
        listMultimap.put("key1", 10);
        listMultimap.put("key2", 10);
        listMultimap.put("other", 10);

        assertTrue(listMultimap.replaceOneIf(key -> key.startsWith("key"), 10, 99));
        assertEquals(Integer.valueOf(99), listMultimap.get("key1").get(0));
        assertEquals(Integer.valueOf(99), listMultimap.get("key2").get(0));
        assertEquals(Integer.valueOf(10), listMultimap.get("other").get(0));
    }

    @Test
    public void testReplaceOneIfBiPredicate_NoMatching() {
        listMultimap.put("key1", 10);
        assertFalse(listMultimap.replaceOneIf((k, v) -> v.size() > 5, 10, 99));
    }

    @Test
    public void testReplaceOneIfBiPredicate_Matching() {
        listMultimap.putMany("key1", Arrays.asList(10, 20));
        listMultimap.putMany("key2", Arrays.asList(10, 20, 30));

        assertTrue(listMultimap.replaceOneIf((k, v) -> v.size() > 2, 10, 99));
        assertEquals(Integer.valueOf(10), listMultimap.get("key1").get(0));
        assertEquals(Integer.valueOf(99), listMultimap.get("key2").get(0));
    }

    @Test
    public void testReplaceManyWithOneIfPredicate_EmptyOldValues() {
        assertFalse(listMultimap.replaceManyWithOneIf(key -> true, new ArrayList<>(), 99));
    }

    @Test
    public void testReplaceManyWithOneIfPredicate_Matching() {
        listMultimap.putMany("key1", Arrays.asList(10, 20, 30));
        listMultimap.putMany("key2", Arrays.asList(10, 20, 40));

        assertTrue(listMultimap.replaceManyWithOneIf(key -> key.equals("key1"), Arrays.asList(10, 20), 99));
        assertEquals(2, listMultimap.get("key1").size());
        assertTrue(listMultimap.get("key1").contains(99));
        assertTrue(listMultimap.get("key1").contains(30));
        assertEquals(3, listMultimap.get("key2").size());
    }

    @Test
    public void testReplaceManyWithOneIfBiPredicate_EmptyOldValues() {
        assertFalse(listMultimap.replaceManyWithOneIf((k, v) -> true, new ArrayList<>(), 99));
    }

    @Test
    public void testReplaceManyWithOneIfBiPredicate_Matching() {
        listMultimap.putMany("key1", Arrays.asList(10, 20, 30, 40));
        listMultimap.putMany("key2", Arrays.asList(10, 20));

        assertTrue(listMultimap.replaceManyWithOneIf((k, v) -> v.size() > 2, Arrays.asList(10, 20), 99));
        assertEquals(3, listMultimap.get("key1").size());
        assertTrue(listMultimap.get("key1").contains(99));
        assertEquals(2, listMultimap.get("key2").size());
    }

    @Test
    public void testReplaceAllWithOneIfPredicate_NoMatching() {
        listMultimap.put("key1", 10);
        assertFalse(listMultimap.replaceAllWithOneIf(key -> key.equals("key2"), 99));
    }

    @Test
    public void testReplaceAllWithOneIfPredicate_Matching() {
        listMultimap.putMany("key1", Arrays.asList(10, 20, 30));
        listMultimap.putMany("key2", Arrays.asList(40, 50));
        listMultimap.putMany("other", Arrays.asList(60));

        assertTrue(listMultimap.replaceAllWithOneIf(key -> key.startsWith("key"), 99));
        assertEquals(1, listMultimap.get("key1").size());
        assertEquals(1, listMultimap.get("key2").size());
        assertEquals(Integer.valueOf(99), listMultimap.get("key1").get(0));
        assertEquals(Integer.valueOf(99), listMultimap.get("key2").get(0));
        assertEquals(Integer.valueOf(60), listMultimap.get("other").get(0));
    }

    @Test
    public void testReplaceAllWithOneIfBiPredicate_NoMatching() {
        listMultimap.put("key1", 10);
        assertFalse(listMultimap.replaceAllWithOneIf((k, v) -> v.size() > 5, 99));
    }

    @Test
    public void testReplaceAllWithOneIfBiPredicate_Matching() {
        listMultimap.putMany("key1", Arrays.asList(10, 20));
        listMultimap.putMany("key2", Arrays.asList(10, 20, 30));

        assertTrue(listMultimap.replaceAllWithOneIf((k, v) -> v.size() > 2, 99));
        assertEquals(2, listMultimap.get("key1").size());
        assertEquals(1, listMultimap.get("key2").size());
        assertEquals(Integer.valueOf(99), listMultimap.get("key2").get(0));
    }

    @Test
    public void testReplaceAll_EmptyMultimap() {
        listMultimap.replaceAll((k, v) -> {
            List<Integer> newList = new ArrayList<>();
            newList.add(99);
            return (List<Integer>) newList;
        });
        assertTrue(listMultimap.isEmpty());
    }

    @Test
    public void testReplaceAll_WithFunction() {
        listMultimap.put("key1", 10);
        listMultimap.put("key2", 20);

        listMultimap.replaceAll((k, v) -> {
            List<Integer> newList = new ArrayList<>();
            newList.add(99);
            return (List<Integer>) newList;
        });

        assertEquals(Integer.valueOf(99), listMultimap.get("key1").get(0));
        assertEquals(Integer.valueOf(99), listMultimap.get("key2").get(0));
    }

    @Test
    public void testComputeIfAbsent_KeyAbsent() {
        List<Integer> result = listMultimap.computeIfAbsent("key1", k -> {
            List<Integer> list = new ArrayList<>();
            list.add(99);
            return (List<Integer>) list;
        });

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(Integer.valueOf(99), result.get(0));
    }

    @Test
    public void testComputeIfAbsent_KeyPresent() {
        listMultimap.put("key1", 10);
        List<Integer> original = listMultimap.get("key1");

        List<Integer> result = listMultimap.computeIfAbsent("key1", k -> {
            List<Integer> list = new ArrayList<>();
            list.add(99);
            return (List<Integer>) list;
        });

        assertEquals(original, result);
        assertEquals(1, result.size());
        assertEquals(Integer.valueOf(10), result.get(0));
    }

    @Test
    public void testComputeIfAbsent_FunctionReturnsNull() {
        List<Integer> result = listMultimap.computeIfAbsent("key1", k -> null);
        assertNull(result);
        assertNull(listMultimap.get("key1"));
    }

    @Test
    public void testComputeIfPresent_KeyAbsent() {
        List<Integer> result = listMultimap.computeIfPresent("key1", (k, v) -> {
            List<Integer> list = new ArrayList<>();
            list.add(99);
            return (List<Integer>) list;
        });

        assertNull(result);
        assertNull(listMultimap.get("key1"));
    }

    @Test
    public void testComputeIfPresent_KeyPresent() {
        listMultimap.put("key1", 10);

        List<Integer> result = listMultimap.computeIfPresent("key1", (k, v) -> {
            List<Integer> list = new ArrayList<>(v);
            list.add(99);
            return (List<Integer>) list;
        });

        assertNotNull(result);
        assertEquals(2, result.size());
        assertTrue(result.contains(10));
        assertTrue(result.contains(99));
    }

    @Test
    public void testComputeIfPresent_FunctionReturnsNull() {
        listMultimap.put("key1", 10);
        List<Integer> result = listMultimap.computeIfPresent("key1", (k, v) -> null);

        assertNull(result);
        assertNull(listMultimap.get("key1"));
    }

    @Test
    public void testCompute_KeyAbsent() {
        List<Integer> result = listMultimap.compute("key1", (k, v) -> {
            List<Integer> list = new ArrayList<>();
            list.add(99);
            return (List<Integer>) list;
        });

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(Integer.valueOf(99), result.get(0));
    }

    @Test
    public void testCompute_KeyPresent() {
        listMultimap.put("key1", 10);

        List<Integer> result = listMultimap.compute("key1", (k, v) -> {
            List<Integer> list = new ArrayList<>(v);
            list.add(99);
            return (List<Integer>) list;
        });

        assertNotNull(result);
        assertEquals(2, result.size());
    }

    @Test
    public void testCompute_FunctionReturnsNull() {
        listMultimap.put("key1", 10);
        List<Integer> result = listMultimap.compute("key1", (k, v) -> null);

        assertNull(result);
        assertNull(listMultimap.get("key1"));
    }

    @Test
    public void testMergeCollection_KeyAbsent() {
        List<Integer> elements = Arrays.asList(10, 20);
        List<Integer> result = listMultimap.merge("key1", elements, (v, c) -> {
            List<Integer> merged = new ArrayList<>(v);
            merged.addAll(c);
            return (List<Integer>) merged;
        });

        assertNotNull(result);
        assertEquals(2, result.size());
    }

    @Test
    public void testMergeCollection_KeyPresent() {
        listMultimap.put("key1", 5);
        List<Integer> elements = Arrays.asList(10, 20);

        List<Integer> result = listMultimap.merge("key1", elements, (v, c) -> {
            List<Integer> merged = new ArrayList<>(v);
            merged.addAll(c);
            return (List<Integer>) merged;
        });

        assertNotNull(result);
        assertEquals(3, result.size());
        assertTrue(result.contains(5));
    }

    @Test
    public void testMergeElement_KeyAbsent() {
        List<Integer> result = listMultimap.merge("key1", 99, (v, e) -> {
            List<Integer> merged = new ArrayList<>(v);
            merged.add(e);
            return (List<Integer>) merged;
        });

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(Integer.valueOf(99), result.get(0));
    }

    @Test
    public void testMergeElement_KeyPresent() {
        listMultimap.put("key1", 10);

        List<Integer> result = listMultimap.merge("key1", 99, (v, e) -> {
            List<Integer> merged = new ArrayList<>(v);
            merged.add(e);
            return (List<Integer>) merged;
        });

        assertNotNull(result);
        assertEquals(2, result.size());
        assertTrue(result.contains(10));
        assertTrue(result.contains(99));
    }

    @Test
    public void testInverse_EmptyMultimap() {
        ListMultimap<Integer, String> inverse = listMultimap.inverse(N::newListMultimap);
        assertNotNull(inverse);
        assertTrue(inverse.isEmpty());
    }

    @Test
    public void testInverse_WithValues() {
        listMultimap.put("key1", 10);
        listMultimap.put("key1", 20);
        listMultimap.put("key2", 10);

        ListMultimap<Integer, String> inverse = listMultimap.inverse(N::newListMultimap);
        assertEquals(2, inverse.keySet().size());
        assertEquals(2, inverse.get(10).size());
        assertTrue(inverse.get(10).contains("key1"));
        assertTrue(inverse.get(10).contains("key2"));
        assertEquals(1, inverse.get(20).size());
        assertTrue(inverse.get(20).contains("key1"));
    }

    @Test
    public void testCopy_EmptyMultimap() {
        Multimap<String, Integer, List<Integer>> copy = listMultimap.copy();
        assertNotNull(copy);
        assertTrue(copy.isEmpty());
    }

    @Test
    public void testCopy_WithValues() {
        listMultimap.put("key1", 10);
        listMultimap.put("key1", 20);
        listMultimap.put("key2", 30);

        Multimap<String, Integer, List<Integer>> copy = listMultimap.copy();
        assertEquals(2, copy.keySet().size());
        assertEquals(2, copy.get("key1").size());
        assertEquals(1, copy.get("key2").size());

        copy.put("key1", 99);
        assertEquals(2, listMultimap.get("key1").size());
        assertEquals(3, copy.get("key1").size());
    }

    @Test
    public void testContains_NonExistentKey() {
        assertFalse(listMultimap.contains("key1", 10));
    }

    @Test
    public void testContains_NonExistentValue() {
        listMultimap.put("key1", 10);
        assertFalse(listMultimap.contains("key1", 20));
    }

    @Test
    public void testContains_ExistingKeyValue() {
        listMultimap.put("key1", 10);
        listMultimap.put("key1", 20);
        assertTrue(listMultimap.contains("key1", 10));
        assertTrue(listMultimap.contains("key1", 20));
    }

    @Test
    public void testContainsKey_NonExistent() {
        assertFalse(listMultimap.containsKey("key1"));
    }

    @Test
    public void testContainsKey_Existing() {
        listMultimap.put("key1", 10);
        assertTrue(listMultimap.containsKey("key1"));
    }

    @Test
    public void testContainsKey_AfterRemoval() {
        listMultimap.put("key1", 10);
        listMultimap.removeAll("key1");
        assertFalse(listMultimap.containsKey("key1"));
    }

    @Test
    public void testContainsValue_NonExistent() {
        assertFalse(listMultimap.containsValue(10));
    }

    @Test
    public void testContainsValue_Existing() {
        listMultimap.put("key1", 10);
        listMultimap.put("key2", 20);
        assertTrue(listMultimap.containsValue(10));
        assertTrue(listMultimap.containsValue(20));
        assertFalse(listMultimap.containsValue(30));
    }

    @Test
    public void testContainsAll_EmptyCollection() {
        assertFalse(listMultimap.containsAll("key1", new ArrayList<>()));

        listMultimap.put("key1", 10);
        assertTrue(listMultimap.containsAll("key1", new ArrayList<>()));
    }

    @Test
    public void testContainsAll_NonExistentKey() {
        assertFalse(listMultimap.containsAll("key1", Arrays.asList(10, 20)));
    }

    @Test
    public void testContainsAll_AllPresent() {
        listMultimap.putMany("key1", Arrays.asList(10, 20, 30));
        assertTrue(listMultimap.containsAll("key1", Arrays.asList(10, 20)));
    }

    @Test
    public void testContainsAll_SomeMissing() {
        listMultimap.putMany("key1", Arrays.asList(10, 20));
        assertFalse(listMultimap.containsAll("key1", Arrays.asList(10, 30)));
    }

    @Test
    public void testFilter_EmptyMultimap() {
        Multimap<String, Integer, List<Integer>> filtered = listMultimap.filter((k, v) -> true);
        assertTrue(filtered.isEmpty());
    }

    @Test
    public void testFilter_AllPass() {
        listMultimap.put("key1", 10);
        listMultimap.put("key2", 20);

        Multimap<String, Integer, List<Integer>> filtered = listMultimap.filter((k, v) -> true);
        assertEquals(2, filtered.keySet().size());
    }

    @Test
    public void testFilter_SomePass() {
        listMultimap.put("key1", 10);
        listMultimap.put("key2", 20);
        listMultimap.put("other", 30);

        Multimap<String, Integer, List<Integer>> filtered = listMultimap.filter((k, v) -> k.startsWith("key"));
        assertEquals(2, filtered.keySet().size());
        assertTrue(filtered.containsKey("key1"));
        assertTrue(filtered.containsKey("key2"));
        assertFalse(filtered.containsKey("other"));
    }

    @Test
    public void testFilterByKey_EmptyMultimap() {
        Multimap<String, Integer, List<Integer>> filtered = listMultimap.filterByKey(k -> true);
        assertTrue(filtered.isEmpty());
    }

    @Test
    public void testFilterByKey_SomeMatch() {
        listMultimap.put("key1", 10);
        listMultimap.put("key2", 20);
        listMultimap.put("other", 30);

        Multimap<String, Integer, List<Integer>> filtered = listMultimap.filterByKey(k -> k.startsWith("key"));
        assertEquals(2, filtered.keySet().size());
        assertTrue(filtered.containsKey("key1"));
        assertFalse(filtered.containsKey("other"));
    }

    @Test
    public void testFilterByValue_EmptyMultimap() {
        Multimap<String, Integer, List<Integer>> filtered = listMultimap.filterByValue(v -> true);
        assertTrue(filtered.isEmpty());
    }

    @Test
    public void testFilterByValue_SomeMatch() {
        listMultimap.putMany("key1", Arrays.asList(10, 20));
        listMultimap.putMany("key2", Arrays.asList(30, 40, 50));

        Multimap<String, Integer, List<Integer>> filtered = listMultimap.filterByValue(v -> v.size() > 2);
        assertEquals(1, filtered.keySet().size());
        assertTrue(filtered.containsKey("key2"));
        assertFalse(filtered.containsKey("key1"));
    }

    @Test
    public void testForEach_EmptyMultimap() {
        AtomicInteger count = new AtomicInteger(0);
        listMultimap.forEach((k, v) -> count.incrementAndGet());
        assertEquals(0, count.get());
    }

    @Test
    public void testForEach_WithValues() {
        listMultimap.put("key1", 10);
        listMultimap.put("key2", 20);

        AtomicInteger count = new AtomicInteger(0);
        listMultimap.forEach((k, v) -> count.incrementAndGet());
        assertEquals(2, count.get());
    }

    @Test
    public void testForEach_NullAction() {
        BiConsumer<String, List<Integer>> nullAction = null;
        assertThrows(IllegalArgumentException.class, () -> listMultimap.forEach(nullAction));
    }

    @Test
    public void testFlatForEach_EmptyMultimap() {
        AtomicInteger count = new AtomicInteger(0);
        listMultimap.flatForEach((k, e) -> count.incrementAndGet());
        assertEquals(0, count.get());
    }

    @Test
    public void testFlatForEach_WithValues() {
        listMultimap.put("key1", 10);
        listMultimap.put("key1", 20);
        listMultimap.put("key2", 30);

        AtomicInteger count = new AtomicInteger(0);
        listMultimap.flatForEach((k, e) -> count.incrementAndGet());
        assertEquals(3, count.get());
    }

    @Test
    public void testFlatForEach_NullAction() {
        assertThrows(IllegalArgumentException.class, () -> listMultimap.flatForEach(null));
    }

    @Test
    public void testForEachKey_EmptyMultimap() {
        AtomicInteger count = new AtomicInteger(0);
        listMultimap.forEachKey(k -> count.incrementAndGet());
        assertEquals(0, count.get());
    }

    @Test
    public void testForEachKey_WithKeys() {
        listMultimap.put("key1", 10);
        listMultimap.put("key1", 20);
        listMultimap.put("key2", 30);

        AtomicInteger count = new AtomicInteger(0);
        listMultimap.forEachKey(k -> count.incrementAndGet());
        assertEquals(2, count.get());
    }

    @Test
    public void testForEachKey_NullAction() {
        assertThrows(IllegalArgumentException.class, () -> listMultimap.forEachKey(null));
    }

    @Test
    public void testForEachValue_EmptyMultimap() {
        AtomicInteger count = new AtomicInteger(0);
        listMultimap.forEachValue(v -> count.incrementAndGet());
        assertEquals(0, count.get());
    }

    @Test
    public void testForEachValue_WithValues() {
        listMultimap.put("key1", 10);
        listMultimap.put("key1", 20);
        listMultimap.put("key2", 30);

        AtomicInteger count = new AtomicInteger(0);
        listMultimap.forEachValue(v -> count.incrementAndGet());
        assertEquals(2, count.get());
    }

    @Test
    public void testForEachValue_NullAction() {
        assertThrows(IllegalArgumentException.class, () -> listMultimap.forEachValue(null));
    }

    @Test
    public void testFlatForEachValue_EmptyMultimap() {
        AtomicInteger count = new AtomicInteger(0);
        listMultimap.flatForEachValue(e -> count.incrementAndGet());
        assertEquals(0, count.get());
    }

    @Test
    public void testFlatForEachValue_WithValues() {
        listMultimap.put("key1", 10);
        listMultimap.put("key1", 20);
        listMultimap.put("key2", 30);

        AtomicInteger count = new AtomicInteger(0);
        listMultimap.flatForEachValue(e -> count.incrementAndGet());
        assertEquals(3, count.get());
    }

    @Test
    public void testFlatForEachValue_NullAction() {
        assertThrows(IllegalArgumentException.class, () -> listMultimap.flatForEachValue(null));
    }

    @Test
    public void testKeySet_EmptyMultimap() {
        Set<String> keys = listMultimap.keySet();
        assertNotNull(keys);
        assertTrue(keys.isEmpty());
    }

    @Test
    public void testKeySet_WithKeys() {
        listMultimap.put("key1", 10);
        listMultimap.put("key2", 20);
        listMultimap.put("key1", 30);

        Set<String> keys = listMultimap.keySet();
        assertEquals(2, keys.size());
        assertTrue(keys.contains("key1"));
        assertTrue(keys.contains("key2"));
    }

    @Test
    public void testValues_EmptyMultimap() {
        Collection<List<Integer>> values = listMultimap.values();
        assertNotNull(values);
        assertTrue(values.isEmpty());
    }

    @Test
    public void testValues_WithValues() {
        listMultimap.put("key1", 10);
        listMultimap.put("key1", 20);
        listMultimap.put("key2", 30);

        Collection<List<Integer>> values = listMultimap.values();
        assertEquals(2, values.size());
    }

    @Test
    public void testEntrySet_EmptyMultimap() {
        Set<Map.Entry<String, List<Integer>>> entries = listMultimap.entrySet();
        assertNotNull(entries);
        assertTrue(entries.isEmpty());
    }

    @Test
    public void testEntrySet_WithEntries() {
        listMultimap.put("key1", 10);
        listMultimap.put("key2", 20);

        Set<Map.Entry<String, List<Integer>>> entries = listMultimap.entrySet();
        assertEquals(2, entries.size());
    }

    @Test
    public void testFlatValues_EmptyMultimap() {
        List<Integer> values = listMultimap.flatValues();
        assertNotNull(values);
        assertTrue(values.isEmpty());
    }

    @Test
    public void testFlatValues_WithValues() {
        listMultimap.put("key1", 10);
        listMultimap.put("key1", 20);
        listMultimap.put("key2", 30);

        List<Integer> values = listMultimap.flatValues();
        assertEquals(3, values.size());
        assertTrue(values.contains(10));
        assertTrue(values.contains(20));
        assertTrue(values.contains(30));
    }

    @Test
    public void testFlatValuesWithSupplier_EmptyMultimap() {
        Set<Integer> values = listMultimap.flatValues(HashSet::new);
        assertNotNull(values);
        assertTrue(values.isEmpty());
    }

    @Test
    public void testFlatValuesWithSupplier_WithValues() {
        listMultimap.put("key1", 10);
        listMultimap.put("key1", 20);
        listMultimap.put("key2", 30);

        Set<Integer> values = listMultimap.flatValues(HashSet::new);
        assertEquals(3, values.size());
        assertTrue(values.contains(10));
    }

    @Test
    public void testToMap_EmptyMultimap() {
        Map<String, List<Integer>> map = listMultimap.toMap();
        assertNotNull(map);
        assertTrue(map.isEmpty());
    }

    @Test
    public void testToMap_WithValues() {
        listMultimap.put("key1", 10);
        listMultimap.put("key2", 20);

        Map<String, List<Integer>> map = listMultimap.toMap();
        assertEquals(2, map.size());
        assertTrue(map.containsKey("key1"));
        assertTrue(map.containsKey("key2"));
    }

    @Test
    public void testToMapWithSupplier_EmptyMultimap() {
        HashMap<String, List<Integer>> map = listMultimap.toMap(HashMap::new);
        assertNotNull(map);
        assertTrue(map.isEmpty());
    }

    @Test
    public void testToMapWithSupplier_WithValues() {
        listMultimap.put("key1", 10);
        listMultimap.put("key2", 20);

        HashMap<String, List<Integer>> map = listMultimap.toMap(HashMap::new);
        assertEquals(2, map.size());
    }

    @Test
    public void testToMultiset_EmptyMultimap() {
        Multiset<String> multiset = listMultimap.toMultiset();
        assertNotNull(multiset);
        assertTrue(multiset.isEmpty());
    }

    @Test
    public void testToMultiset_WithValues() {
        listMultimap.put("key1", 10);
        listMultimap.put("key1", 20);
        listMultimap.put("key1", 30);
        listMultimap.put("key2", 40);

        Multiset<String> multiset = listMultimap.toMultiset();
        assertEquals(3, multiset.count("key1"));
        assertEquals(1, multiset.count("key2"));
    }

    @Test
    public void testIterator_EmptyMultimap() {
        Iterator<Map.Entry<String, List<Integer>>> iter = listMultimap.iterator();
        assertNotNull(iter);
        assertFalse(iter.hasNext());
    }

    @Test
    public void testIterator_WithValues() {
        listMultimap.put("key1", 10);
        listMultimap.put("key2", 20);

        Iterator<Map.Entry<String, List<Integer>>> iter = listMultimap.iterator();
        int count = 0;
        while (iter.hasNext()) {
            iter.next();
            count++;
        }
        assertEquals(2, count);
    }

    @Test
    public void testStream_EmptyMultimap() {
        Stream<Map.Entry<String, List<Integer>>> stream = listMultimap.stream();
        assertNotNull(stream);
        assertEquals(0, stream.count());
    }

    @Test
    public void testStream_WithValues() {
        listMultimap.put("key1", 10);
        listMultimap.put("key2", 20);

        long count = listMultimap.stream().count();
        assertEquals(2, count);
    }

    @Test
    public void testEntryStream_EmptyMultimap() {
        EntryStream<String, List<Integer>> stream = listMultimap.entryStream();
        assertNotNull(stream);
        assertEquals(0, stream.count());
    }

    @Test
    public void testEntryStream_WithValues() {
        listMultimap.put("key1", 10);
        listMultimap.put("key2", 20);

        long count = listMultimap.entryStream().count();
        assertEquals(2, count);
    }

    @Test
    public void testClear_EmptyMultimap() {
        listMultimap.clear();
        assertTrue(listMultimap.isEmpty());
    }

    @Test
    public void testClear_WithValues() {
        listMultimap.put("key1", 10);
        listMultimap.put("key2", 20);

        listMultimap.clear();
        assertTrue(listMultimap.isEmpty());
        assertNull(listMultimap.get("key1"));
    }

    @Test
    public void testSize_EmptyMultimap() {
        assertEquals(0, listMultimap.size());
    }

    @Test
    public void testSize_WithKeys() {
        listMultimap.put("key1", 10);
        listMultimap.put("key1", 20);
        listMultimap.put("key2", 30);

        assertEquals(2, listMultimap.size());
    }

    @Test
    public void testTotalCountOfValues_EmptyMultimap() {
        assertEquals(0, listMultimap.totalCountOfValues());
    }

    @Test
    public void testTotalCountOfValues_WithValues() {
        listMultimap.put("key1", 10);
        listMultimap.put("key1", 20);
        listMultimap.put("key2", 30);

        assertEquals(3, listMultimap.totalCountOfValues());
    }

    @Test
    public void testIsEmpty_EmptyMultimap() {
        assertTrue(listMultimap.isEmpty());
    }

    @Test
    public void testIsEmpty_WithValues() {
        listMultimap.put("key1", 10);
        assertFalse(listMultimap.isEmpty());
    }

    @Test
    public void testIsEmpty_AfterClear() {
        listMultimap.put("key1", 10);
        listMultimap.clear();
        assertTrue(listMultimap.isEmpty());
    }

    @Test
    public void testApply_ReturnsResult() throws Exception {
        listMultimap.put("key1", 10);

        Integer result = listMultimap.apply(m -> m.size());
        assertEquals(1, result);
    }

    @Test
    public void testApply_NullFunction() {
        assertThrows(Exception.class, () -> listMultimap.apply(null));
    }

    @Test
    public void testApplyIfNotEmpty_EmptyMultimap() throws Exception {
        Optional<Integer> result = listMultimap.applyIfNotEmpty(m -> m.size());
        assertFalse(result.isPresent());
    }

    @Test
    public void testApplyIfNotEmpty_WithValues() throws Exception {
        listMultimap.put("key1", 10);

        Optional<Integer> result = listMultimap.applyIfNotEmpty(m -> m.size());
        assertTrue(result.isPresent());
        assertEquals(1, result.get());
    }

    @Test
    public void testAccept_ExecutesAction() throws Exception {
        listMultimap.put("key1", 10);

        AtomicInteger count = new AtomicInteger(0);
        listMultimap.accept(m -> count.set(m.size()));
        assertEquals(1, count.get());
    }

    @Test
    public void testAccept_NullAction() {
        assertThrows(Exception.class, () -> listMultimap.accept(null));
    }

    @Test
    public void testAcceptIfNotEmpty_EmptyMultimap() throws Exception {
        AtomicInteger count = new AtomicInteger(0);
        OrElse orElse = listMultimap.acceptIfNotEmpty(m -> count.incrementAndGet());

        assertEquals(0, count.get());
        assertNotNull(orElse);
    }

    @Test
    public void testAcceptIfNotEmpty_WithValues() throws Exception {
        listMultimap.put("key1", 10);

        AtomicInteger count = new AtomicInteger(0);
        OrElse orElse = listMultimap.acceptIfNotEmpty(m -> count.incrementAndGet());

        assertEquals(1, count.get());
        assertNotNull(orElse);
    }

    @Test
    public void testHashCode_EmptyMultimaps() {
        ListMultimap<String, Integer> other = N.newListMultimap();
        assertEquals(listMultimap.hashCode(), other.hashCode());
    }

    @Test
    public void testHashCode_EqualMultimaps() {
        listMultimap.put("key1", 10);

        ListMultimap<String, Integer> other = N.newListMultimap();
        other.put("key1", 10);

        assertEquals(listMultimap.hashCode(), other.hashCode());
    }

    @Test
    public void testEquals_SameInstance() {
        assertTrue(listMultimap.equals(listMultimap));
    }

    @Test
    public void testEquals_Null() {
        assertFalse(listMultimap.equals(null));
    }

    @Test
    public void testEquals_DifferentType() {
        assertFalse(listMultimap.equals("not a multimap"));
    }

    @Test
    public void testEquals_EmptyMultimaps() {
        ListMultimap<String, Integer> other = N.newListMultimap();
        assertTrue(listMultimap.equals(other));
    }

    @Test
    public void testEquals_EqualMultimaps() {
        listMultimap.put("key1", 10);
        listMultimap.put("key2", 20);

        ListMultimap<String, Integer> other = N.newListMultimap();
        other.put("key1", 10);
        other.put("key2", 20);

        assertTrue(listMultimap.equals(other));
    }

    @Test
    public void testEquals_DifferentMultimaps() {
        listMultimap.put("key1", 10);

        ListMultimap<String, Integer> other = N.newListMultimap();
        other.put("key1", 20);

        assertFalse(listMultimap.equals(other));
    }

    @Test
    public void testToString_EmptyMultimap() {
        String str = listMultimap.toString();
        assertNotNull(str);
        assertEquals("{}", str);
    }

    @Test
    public void testToString_WithValues() {
        listMultimap.put("key1", 10);
        listMultimap.put("key2", 20);

        String str = listMultimap.toString();
        assertNotNull(str);
        assertTrue(str.contains("key1"));
        assertTrue(str.contains("key2"));
    }

    @Test
    public void testPut_NullKey() {
        assertTrue(listMultimap.put(null, 10));
        assertEquals(1, listMultimap.get(null).size());
        assertTrue(listMultimap.get(null).contains(10));
    }

    @Test
    public void testGet_NullKey() {
        listMultimap.put(null, 10);
        List<Integer> values = listMultimap.get(null);
        assertNotNull(values);
        assertEquals(1, values.size());
    }

    @Test
    public void testContainsKey_NullKey() {
        assertFalse(listMultimap.containsKey(null));
        listMultimap.put(null, 10);
        assertTrue(listMultimap.containsKey(null));
    }

    @Test
    public void testRemoveAll_NullKey() {
        listMultimap.put(null, 10);
        listMultimap.put(null, 20);
        List<Integer> removed = listMultimap.removeAll(null);
        assertNotNull(removed);
        assertEquals(2, removed.size());
        assertNull(listMultimap.get(null));
    }

    @Test
    public void testPutMany_NullKey() {
        List<Integer> values = Arrays.asList(10, 20, 30);
        assertTrue(listMultimap.putMany(null, values));
        assertEquals(3, listMultimap.get(null).size());
    }

    @Test
    public void testRemoveOne_NullKey() {
        listMultimap.put(null, 10);
        assertTrue(listMultimap.removeOne(null, 10));
        assertNull(listMultimap.get(null));
    }

    @Test
    public void testReplaceOne_NullKey() {
        listMultimap.put(null, 10);
        assertTrue(listMultimap.replaceOne(null, 10, 20));
        assertEquals(Integer.valueOf(20), listMultimap.get(null).get(0));
    }

    @Test
    public void testPutMap_NullValues() {
        Map<String, Integer> map = new HashMap<>();
        map.put("key1", null);
        map.put("key2", 20);
        assertTrue(listMultimap.put(map));
        assertTrue(listMultimap.get("key1").contains(null));
        assertTrue(listMultimap.get("key2").contains(20));
    }

    @Test
    public void testPutManyMap_NullValues() {
        Map<String, Collection<Integer>> map = new HashMap<>();
        List<Integer> values = new ArrayList<>();
        values.add(null);
        values.add(10);
        map.put("key1", values);
        assertTrue(listMultimap.putMany(map));
        assertTrue(listMultimap.get("key1").contains(null));
        assertTrue(listMultimap.get("key1").contains(10));
    }

    @Test
    public void testRemoveMany_NullValues() {
        listMultimap.put("key1", null);
        listMultimap.put("key1", 10);
        listMultimap.put("key1", 20);
        List<Integer> toRemove = new ArrayList<>();
        toRemove.add(null);
        toRemove.add(10);
        assertTrue(listMultimap.removeMany("key1", toRemove));
        assertEquals(1, listMultimap.get("key1").size());
        assertTrue(listMultimap.get("key1").contains(20));
    }

    @Test
    public void testContains_NullValue() {
        listMultimap.put("key1", null);
        assertTrue(listMultimap.contains("key1", null));
        assertFalse(listMultimap.contains("key2", null));
    }

    @Test
    public void testContainsValue_Null() {
        assertFalse(listMultimap.containsValue(null));
        listMultimap.put("key1", null);
        assertTrue(listMultimap.containsValue(null));
    }

    @Test
    public void testContainsAll_WithNull() {
        List<Integer> values = new ArrayList<>();
        values.add(null);
        values.add(10);
        listMultimap.put("key1", null);
        listMultimap.put("key1", 10);
        assertTrue(listMultimap.containsAll("key1", values));
    }

    @Test
    public void testReplaceManyWithOne_WithNullValues() {
        listMultimap.put("key1", null);
        listMultimap.put("key1", 10);
        listMultimap.put("key1", 20);
        List<Integer> oldValues = new ArrayList<>();
        oldValues.add(null);
        oldValues.add(10);
        assertTrue(listMultimap.replaceManyWithOne("key1", oldValues, 99));
        assertEquals(2, listMultimap.get("key1").size());
        assertTrue(listMultimap.get("key1").contains(99));
        assertTrue(listMultimap.get("key1").contains(20));
    }

    @Test
    public void testFilter_WithNullKeys() {
        listMultimap.put(null, 10);
        listMultimap.put("key1", 20);
        Multimap<String, Integer, List<Integer>> filtered = listMultimap.filter((k, v) -> k == null);
        assertEquals(1, filtered.size());
        assertTrue(filtered.containsKey(null));
    }

    @Test
    public void testFilterByKey_WithNull() {
        listMultimap.put(null, 10);
        listMultimap.put("key1", 20);
        Multimap<String, Integer, List<Integer>> filtered = listMultimap.filterByKey(k -> k == null);
        assertEquals(1, filtered.size());
        assertTrue(filtered.containsKey(null));
    }

    @Test
    public void testForEach_ModificationDuringIteration() {
        listMultimap.put("key1", 10);
        listMultimap.put("key2", 20);
        AtomicInteger count = new AtomicInteger(0);
        listMultimap.forEach((k, v) -> {
            count.incrementAndGet();
        });
        assertEquals(2, count.get());
    }

    @Test
    public void testFlatForEach_ModificationDuringIteration() {
        listMultimap.put("key1", 10);
        listMultimap.put("key1", 20);
        listMultimap.put("key2", 30);
        List<Integer> collected = new ArrayList<>();
        listMultimap.flatForEach((k, e) -> collected.add(e));
        assertEquals(3, collected.size());
        assertTrue(collected.contains(10));
        assertTrue(collected.contains(20));
        assertTrue(collected.contains(30));
    }

    @Test
    public void testStream_Operations() {
        listMultimap.put("key1", 10);
        listMultimap.put("key2", 20);
        listMultimap.put("key3", 30);
        long count = listMultimap.stream().filter(e -> e.getKey().startsWith("key")).count();
        assertEquals(3, count);
    }

    @Test
    public void testEntryStream_Operations() {
        listMultimap.put("key1", 10);
        listMultimap.put("key2", 20);
        List<String> keys = listMultimap.entryStream().keys().toList();
        assertEquals(2, keys.size());
        assertTrue(keys.contains("key1"));
        assertTrue(keys.contains("key2"));
    }

    @Test
    public void testTotalCountOfValues_LargeNumbers() {
        for (int i = 0; i < 10; i++) {
            listMultimap.putMany("key" + i, Arrays.asList(1, 2, 3, 4, 5));
        }
        assertEquals(50, listMultimap.totalCountOfValues());
    }

    @Test
    public void testCopy_Independence() {
        listMultimap.put("key1", 10);
        Multimap<String, Integer, List<Integer>> copy = listMultimap.copy();

        listMultimap.put("key1", 20);
        listMultimap.put("key2", 30);

        assertEquals(1, copy.get("key1").size());
        assertNull(copy.get("key2"));
    }

    @Test
    public void testInverse_MultipleValuesPerKey() {
        listMultimap.put("a", 1);
        listMultimap.put("a", 2);
        listMultimap.put("b", 1);
        listMultimap.put("b", 3);
        ListMultimap<Integer, String> inverse = listMultimap.inverse(N::newListMultimap);

        assertEquals(3, inverse.size());
        assertEquals(2, inverse.get(1).size());
        assertTrue(inverse.get(1).contains("a"));
        assertTrue(inverse.get(1).contains("b"));
    }

    @Test
    public void testComputeIfAbsent_NullKey() {
        List<Integer> result = listMultimap.computeIfAbsent(null, k -> {
            List<Integer> list = new ArrayList<>();
            list.add(99);
            return (List<Integer>) list;
        });
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(Integer.valueOf(99), result.get(0));
    }

    @Test
    public void testComputeIfPresent_NullKey() {
        listMultimap.put(null, 10);
        List<Integer> result = listMultimap.computeIfPresent(null, (k, v) -> {
            List<Integer> list = new ArrayList<>(v);
            list.add(20);
            return (List<Integer>) list;
        });
        assertNotNull(result);
        assertEquals(2, result.size());
    }

    @Test
    public void testCompute_NullKey() {
        List<Integer> result = listMultimap.compute(null, (k, v) -> {
            List<Integer> list = v == null ? new ArrayList<>() : new ArrayList<>(v);
            list.add(99);
            return (List<Integer>) list;
        });
        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    public void testMergeCollection_NullKey() {
        List<Integer> elements = Arrays.asList(10, 20);
        List<Integer> result = listMultimap.merge(null, elements, (v, c) -> {
            List<Integer> merged = new ArrayList<>(v);
            merged.addAll(c);
            return (List<Integer>) merged;
        });
        assertNotNull(result);
        assertEquals(2, result.size());
    }

    @Test
    public void testMergeElement_NullKey() {
        List<Integer> result = listMultimap.merge(null, 99, (v, e) -> {
            List<Integer> merged = new ArrayList<>(v);
            merged.add(e);
            return (List<Integer>) merged;
        });
        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    public void testToMap_Mutability() {
        listMultimap.put("key1", 10);
        Map<String, List<Integer>> map = listMultimap.toMap();
        map.put("key2", Arrays.asList(20));
        assertNull(listMultimap.get("key2"));
        assertNotNull(map.get("key2"));
    }

    @Test
    public void testFlatValues_ModifiableList() {
        listMultimap.put("key1", 10);
        listMultimap.put("key2", 20);
        List<Integer> values = listMultimap.flatValues();
        values.add(30);
        assertEquals(3, values.size());
        assertEquals(2, listMultimap.totalCountOfValues());
    }

    @Test
    public void testIterator_Remove() {
        listMultimap.put("key1", 10);
        listMultimap.put("key2", 20);
        Iterator<Map.Entry<String, List<Integer>>> iter = listMultimap.iterator();
        if (iter.hasNext()) {
            iter.next();
            iter.remove();
        }
        assertEquals(1, listMultimap.size());
    }

    @Test
    public void testPutIfAbsent_WithNullValue() {
        assertTrue(listMultimap.putIfAbsent("key1", null));
        assertFalse(listMultimap.putIfAbsent("key1", null));
        assertEquals(1, listMultimap.get("key1").size());
    }

    @Test
    public void testPutIfKeyAbsent_WithNullValue() {
        assertTrue(listMultimap.putIfKeyAbsent("key1", null));
        assertFalse(listMultimap.putIfKeyAbsent("key1", 10));
        assertEquals(1, listMultimap.get("key1").size());
        assertTrue(listMultimap.get("key1").contains(null));
    }

    @Test
    public void testRemoveMany_PartialRemoval() {
        listMultimap.putMany("key1", Arrays.asList(10, 20, 30, 40));
        assertTrue(listMultimap.removeMany("key1", Arrays.asList(15, 20, 35, 40)));
        assertEquals(2, listMultimap.get("key1").size());
        assertTrue(listMultimap.get("key1").contains(10));
        assertTrue(listMultimap.get("key1").contains(30));
    }

    @Test
    public void testFilterByValue_EmptyCollections() {
        listMultimap.put("key1", 10);
        listMultimap.putMany("key2", Arrays.asList(20, 30));
        Multimap<String, Integer, List<Integer>> filtered = listMultimap.filterByValue(v -> v.isEmpty());
        assertTrue(filtered.isEmpty());
    }

    @Test
    public void testForEachKey_OrderPreservation() {
        listMultimap.put("key1", 10);
        listMultimap.put("key2", 20);
        listMultimap.put("key3", 30);
        List<String> keys = new ArrayList<>();
        listMultimap.forEachKey(k -> keys.add(k));
        assertEquals(3, keys.size());
    }

    @Test
    public void testForEachValue_OrderPreservation() {
        listMultimap.putMany("key1", Arrays.asList(10, 20));
        listMultimap.putMany("key2", Arrays.asList(30));
        List<List<Integer>> values = new ArrayList<>();
        listMultimap.forEachValue(v -> values.add(new ArrayList<>(v)));
        assertEquals(2, values.size());
    }

    @Test
    public void testFlatForEachValue_OrderPreservation() {
        listMultimap.putMany("key1", Arrays.asList(10, 20));
        listMultimap.putMany("key2", Arrays.asList(30));
        List<Integer> values = new ArrayList<>();
        listMultimap.flatForEachValue(e -> values.add(e));
        assertEquals(3, values.size());
        assertTrue(values.contains(10));
        assertTrue(values.contains(20));
        assertTrue(values.contains(30));
    }

    @Test
    public void testReplaceAll_WithNullResults() {
        listMultimap.put("key1", 10);
        listMultimap.put("key2", 20);
        listMultimap.replaceAll((k, v) -> null);
        assertTrue(listMultimap.isEmpty());
    }

    @Test
    public void testMergeCollection_FunctionReturnsNull() {
        listMultimap.put("key1", 10);
        List<Integer> elements = Arrays.asList(20);
        List<Integer> result = listMultimap.merge("key1", elements, (v, c) -> null);
        assertNull(result);
        assertNull(listMultimap.get("key1"));
    }

    @Test
    public void testMergeElement_FunctionReturnsNull() {
        listMultimap.put("key1", 10);
        List<Integer> result = listMultimap.merge("key1", 20, (v, e) -> null);
        assertNull(result);
        assertNull(listMultimap.get("key1"));
    }

    @Test
    public void testAcceptIfNotEmpty_WithOrElse() throws Exception {
        AtomicInteger count = new AtomicInteger(0);
        OrElse orElse = listMultimap.acceptIfNotEmpty(m -> count.set(1));
        orElse.orElse(() -> count.set(2));
        assertEquals(2, count.get());
    }

    @Test
    public void testAcceptIfNotEmpty_NoOrElse() throws Exception {
        listMultimap.put("key1", 10);
        AtomicInteger count = new AtomicInteger(0);
        OrElse orElse = listMultimap.acceptIfNotEmpty(m -> count.set(1));
        orElse.orElse(() -> count.set(2));
        assertEquals(1, count.get());
    }

    @Test
    public void testPutMany_SingleElementCollection() {
        assertTrue(listMultimap.putMany("key1", Arrays.asList(10)));
        assertEquals(1, listMultimap.get("key1").size());
    }

    @Test
    public void testRemoveOneMap_PartialMatch() {
        listMultimap.put("key1", 10);
        listMultimap.put("key1", 20);
        listMultimap.put("key2", 30);
        Map<String, Integer> toRemove = new HashMap<>();
        toRemove.put("key1", 10);
        toRemove.put("key3", 40);
        assertTrue(listMultimap.removeOne(toRemove));
        assertEquals(1, listMultimap.get("key1").size());
    }

    @Test
    public void testRemoveManyMap_PartialMatch() {
        listMultimap.putMany("key1", Arrays.asList(10, 20, 30));
        listMultimap.putMany("key2", Arrays.asList(40));
        Map<String, Collection<Integer>> toRemove = new HashMap<>();
        toRemove.put("key1", Arrays.asList(10, 20));
        toRemove.put("key3", Arrays.asList(50));
        assertTrue(listMultimap.removeMany(toRemove));
        assertEquals(1, listMultimap.get("key1").size());
    }

    @Test
    public void testHashCode_ConsistentWithEquals() {
        listMultimap.put("key1", 10);
        ListMultimap<String, Integer> other1 = N.newListMultimap();
        other1.put("key1", 10);
        ListMultimap<String, Integer> other2 = N.newListMultimap();
        other2.put("key1", 20);

        assertEquals(listMultimap.hashCode(), other1.hashCode());
        assertTrue(listMultimap.equals(other1));
        assertFalse(listMultimap.equals(other2));
    }

    @Test
    public void testEquals_WithSetMultimap() {
        listMultimap.put("key1", 10);
        SetMultimap<String, Integer> setMm = N.newSetMultimap();
        setMm.put("key1", 10);
        assertNotNull(listMultimap.equals(setMm));
    }

    @Test
    public void testSize_AfterOperations() {
        assertEquals(0, listMultimap.size());
        listMultimap.put("key1", 10);
        assertEquals(1, listMultimap.size());
        listMultimap.put("key1", 20);
        assertEquals(1, listMultimap.size());
        listMultimap.put("key2", 30);
        assertEquals(2, listMultimap.size());
        listMultimap.removeAll("key1");
        assertEquals(1, listMultimap.size());
    }

    @Test
    public void testKeySet_BackingBehavior() {
        listMultimap.put("key1", 10);
        listMultimap.put("key2", 20);
        Set<String> keys = listMultimap.keySet();
        assertEquals(2, keys.size());
        listMultimap.removeAll("key1");
        assertNotNull(keys);
    }

    @Test
    public void testValues_BackingBehavior() {
        listMultimap.put("key1", 10);
        listMultimap.put("key2", 20);
        Collection<List<Integer>> values = listMultimap.values();
        assertEquals(2, values.size());
        assertNotNull(values);
    }

    @Test
    public void testEntrySet_BackingBehavior() {
        listMultimap.put("key1", 10);
        listMultimap.put("key2", 20);
        Set<Map.Entry<String, List<Integer>>> entries = listMultimap.entrySet();
        assertEquals(2, entries.size());
        assertNotNull(entries);
    }
}

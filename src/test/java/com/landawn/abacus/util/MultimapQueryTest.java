package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.util.stream.EntryStream;

public class MultimapQueryTest extends MultimapTestSupport {

    // allValues

    @Test
    public void testAllValues() {
        listMultimap.put("a", 1);
        listMultimap.put("a", 2);
        listMultimap.put("b", 3);

        Collection<Integer> values = listMultimap.allValues();
        assertEquals(3, values.size());
        assertTrue(values.contains(1));
        assertTrue(values.contains(2));
        assertTrue(values.contains(3));
    }

    @Test
    public void testAllValues_EmptyMultimap() {
        Collection<Integer> values = listMultimap.allValues();
        assertTrue(values.isEmpty());
    }

    @Test
    public void testAllValues_Unmodifiable() {
        listMultimap.put("key", 1);
        Collection<Integer> values = listMultimap.allValues();
        assertThrows(UnsupportedOperationException.class, () -> values.add(2));
    }

    @Test
    public void testAllValuesSizeSaturatesAndSpliteratorKeepsLongEstimate() {
        final Map<String, Collection<Integer>> backing = new HashMap<>();
        backing.put("a", Collections.nCopies(Integer.MAX_VALUE, 1));
        backing.put("b", Collections.nCopies(Integer.MAX_VALUE, 2));
        final Multimap<String, Integer, Collection<Integer>> mm = new Multimap<>(backing, ArrayList::new);

        assertThrows(ArithmeticException.class, mm::totalValueCount);
        assertEquals(Integer.MAX_VALUE, mm.allValues().size());
        assertEquals(2L * Integer.MAX_VALUE, mm.valueSpliterator().estimateSize());
    }

    @Test
    public void testContains() {
        listMultimap.put("key1", 10);

        assertTrue(listMultimap.containsEntry("key1", 10));
        assertFalse(listMultimap.containsEntry("key1", 20));
        assertFalse(listMultimap.containsEntry("key2", 10));
        assertFalse(listMultimap.containsEntry("key1", null));
        assertFalse(listMultimap.containsEntry(null, 10));
    }

    @Test
    public void testContains_ExistingKeyValue() {
        listMultimap.put("key1", 10);
        listMultimap.put("key1", 20);
        assertTrue(listMultimap.containsEntry("key1", 10));
        assertTrue(listMultimap.containsEntry("key1", 20));
    }

    @Test
    public void testContains_keyValue() {
        Multimap<String, Integer, List<Integer>> mm = getTestMultimap();
        mm.put("a", 1);
        assertTrue(mm.containsEntry("a", 1));
        assertFalse(mm.containsEntry("a", 2));
        assertFalse(mm.containsEntry("b", 1));
    }

    @Test
    public void testContains_NonExistentKey() {
        assertFalse(listMultimap.containsEntry("key1", 10));
    }

    @Test
    public void testContains_NonExistentValue() {
        listMultimap.put("key1", 10);
        assertFalse(listMultimap.containsEntry("key1", 20));
    }

    @Test
    public void testContains_NullValue() {
        listMultimap.put("key1", null);
        assertTrue(listMultimap.containsEntry("key1", null));
        assertFalse(listMultimap.containsEntry("key2", null));
    }

    // containsEntry

    @Test
    public void testContainsEntry() {
        listMultimap.put("key", 1);
        listMultimap.put("key", 2);

        assertTrue(listMultimap.containsEntry("key", 1));
        assertTrue(listMultimap.containsEntry("key", 2));
        assertFalse(listMultimap.containsEntry("key", 3));
        assertFalse(listMultimap.containsEntry("other", 1));
    }

    @Test
    public void testContainsEntry_AfterReplaceEntry() {
        listMultimap.put("key1", 10);
        listMultimap.replaceEntry("key1", 10, 20);
        assertFalse(listMultimap.containsEntry("key1", 10));
        assertTrue(listMultimap.containsEntry("key1", 20));
    }

    @Test
    public void testContainsEntry_NullValue() {
        listMultimap.put("key", null);
        assertTrue(listMultimap.containsEntry("key", null));
    }

    // containsKey

    @Test
    public void testContainsKey() {
        Multimap<String, Integer, List<Integer>> mm = getTestMultimap();
        mm.put("a", 1);
        assertTrue(mm.containsKey("a"));
        assertFalse(mm.containsKey("b"));
    }

    @Test
    public void testContainsKey_AfterRemoval() {
        listMultimap.put("key1", 10);
        listMultimap.removeAll("key1");
        assertFalse(listMultimap.containsKey("key1"));
    }

    @Test
    public void testContainsKey_Existing() {
        listMultimap.put("key1", 10);
        assertTrue(listMultimap.containsKey("key1"));
    }

    @Test
    public void testContainsKey_NonExistent() {
        assertFalse(listMultimap.containsKey("key1"));
    }

    @Test
    public void testContainsKey_NullKey() {
        assertFalse(listMultimap.containsKey(null));
        listMultimap.put(null, 10);
        assertTrue(listMultimap.containsKey(null));
    }

    // containsValue

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
    public void testContainsValue_element() {
        Multimap<String, Integer, List<Integer>> mm = getTestMultimap();
        mm.put("a", 1);
        mm.put("b", 2);
        mm.put("c", 1);
        assertTrue(mm.containsValue(1));
        assertTrue(mm.containsValue(2));
        assertFalse(mm.containsValue(3));
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
    public void testContainsValue_NonExistent() {
        assertFalse(listMultimap.containsValue(10));
    }

    @Test
    public void testContainsValue_Null() {
        assertFalse(listMultimap.containsValue(null));
        listMultimap.put("key1", null);
        assertTrue(listMultimap.containsValue(null));
    }

    // entryStream

    @Test
    public void testEntryStream_CollectsCorrectKeyValuePairs() {
        listMultimap.put("key1", 10);
        listMultimap.put("key1", 20);
        listMultimap.put("key2", 30);

        List<Map.Entry<String, Integer>> collected = new ArrayList<>();
        listMultimap.entryStream().forEach(e -> collected.add(CommonUtil.newEntry(e.getKey(), e.getValue())));
        assertEquals(3, collected.size());
        long key1Count = collected.stream().filter(e -> "key1".equals(e.getKey())).count();
        long key2Count = collected.stream().filter(e -> "key2".equals(e.getKey())).count();
        assertEquals(2, key1Count);
        assertEquals(1, key2Count);
    }

    @Test
    public void testEntryStream_EmptyMultimap() {
        EntryStream<String, Integer> stream = listMultimap.entryStream();
        assertNotNull(stream);
        assertEquals(0, stream.count());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testEntryStream_SetMultimap() {
        setMultimap.put("key1", 10);
        setMultimap.put("key1", 20);
        setMultimap.put("key2", 30);

        EntryStream<String, Integer> stream = setMultimap.entryStream();
        assertEquals(3, stream.count());
    }

    @Test
    public void testEntryStream_WithValues() {
        listMultimap.put("key1", 10);
        listMultimap.put("key1", 20);
        listMultimap.put("key2", 30);

        EntryStream<String, Integer> stream = listMultimap.entryStream();
        assertEquals(3, stream.count());
    }

    // get

    @Test
    public void testGet() {
        Multimap<String, Integer, List<Integer>> mm = getTestMultimap();
        assertNull(mm.get("a"));
        mm.put("a", 1);
        List<Integer> valuesA = mm.get("a");
        assertNotNull(valuesA);
        assertEquals(1, valuesA.size());
        assertEquals(Integer.valueOf(1), valuesA.get(0));

        mm.put("a", 2);
        assertEquals(2, valuesA.size());
        assertTrue(valuesA.containsAll(Arrays.asList(1, 2)));
    }

    @Test
    public void testGet_NullKey() {
        listMultimap.put(null, 10);
        List<Integer> values = listMultimap.get(null);
        assertNotNull(values);
        assertEquals(1, values.size());
    }

    @Test
    public void testGet_ReturnsBackedCollection() {
        listMultimap.put("key1", 10);
        List<Integer> values = listMultimap.get("key1");
        values.add(20);
        assertEquals(2, listMultimap.get("key1").size());
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
    public void testGet_WithNonExistentKey() {
        assertNull(listMultimap.get("nonexistent"));
    }

    // getOrDefault

    @Test
    public void testGetOrDefault() {
        Multimap<String, Integer, List<Integer>> mm = getTestMultimap();
        List<Integer> defaultList = new ArrayList<>(Arrays.asList(99));
        assertEquals(defaultList, mm.getOrDefault("a", defaultList));
        assertFalse(mm.containsKey("a"));

        mm.put("a", 1);
        List<Integer> valuesA = mm.get("a");
        assertEquals(valuesA, mm.getOrDefault("a", defaultList));
        assertNotEquals(defaultList, mm.getOrDefault("a", defaultList));
    }

    @Test
    public void testGetOrDefault_ReturnsDefaultForNullKeyNotPresent() {
        List<Integer> defaultList = Arrays.asList(99);
        assertEquals(defaultList, listMultimap.getOrDefault("nonexistent", defaultList));
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
    public void testGetOrDefault_WithNonExistentKey() {
        List<Integer> defaultValue = Arrays.asList(-1, -2);
        List<Integer> result = listMultimap.getOrDefault("nonexistent", defaultValue);
        assertEquals(defaultValue, result);
    }

    // isEmpty

    @Test
    public void testIsEmpty() {
        Multimap<String, Integer, List<Integer>> mm = getTestMultimap();
        assertTrue(mm.isEmpty());
        mm.put("a", 1);
        assertFalse(mm.isEmpty());
        mm.removeAll("a");
        assertTrue(mm.isEmpty());
    }

    @Test
    public void testIsEmpty_AfterClear() {
        listMultimap.put("key1", 10);
        listMultimap.clear();
        assertTrue(listMultimap.isEmpty());
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

    // keyCount

    @Test
    public void testKeyCount_AfterClear() {
        listMultimap.put("a", 1);
        listMultimap.put("b", 2);
        listMultimap.clear();
        assertEquals(0, listMultimap.keyCount());
    }

    @Test
    public void testKeyCount_AfterRemoval() {
        listMultimap.put("key1", 10);
        listMultimap.put("key2", 20);
        assertEquals(2, listMultimap.keyCount());

        listMultimap.removeAll("key1");
        assertEquals(1, listMultimap.keyCount());
    }

    @Test
    public void testKeyCount_EmptyMultimap() {
        assertEquals(0, listMultimap.keyCount());
    }

    @Test
    public void testKeyCount_WithValues() {
        listMultimap.put("key1", 10);
        listMultimap.put("key1", 20);
        listMultimap.put("key2", 30);

        assertEquals(2, listMultimap.keyCount());
    }

    // keySet

    @Test
    public void testKeySet() {
        Multimap<String, Integer, List<Integer>> mm = getTestMultimap();
        mm.put("a", 1);
        mm.put("b", 2);
        assertEquals(CommonUtil.toSet("a", "b"), mm.keySet());
        mm.keySet().remove("a");
        assertFalse(mm.containsKey("a"));
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

    // size

    @Test
    public void testSize() {
        Multimap<String, Integer, List<Integer>> mm = getTestMultimap();
        assertEquals(0, mm.totalValueCount());
        mm.put("a", 1);
        assertEquals(1, mm.totalValueCount());
        mm.put("b", 2);
        assertEquals(2, mm.totalValueCount());
        mm.put("a", 3);
        assertEquals(3, mm.totalValueCount());
    }

    @SuppressWarnings("deprecation")
    @Test
    public void testSize_Deprecated_EmptyMultimap() {
        assertEquals(0, listMultimap.size());
    }

    @SuppressWarnings("deprecation")
    @Test
    public void testSize_Deprecated_EqualsKeyCount() {
        listMultimap.put("key1", 10);
        listMultimap.put("key2", 20);
        listMultimap.put("key3", 30);

        assertEquals(listMultimap.keyCount(), listMultimap.size());
    }

    @SuppressWarnings("deprecation")
    @Test
    public void testSize_Deprecated_WithValues() {
        listMultimap.put("key1", 10);
        listMultimap.put("key1", 20);
        listMultimap.put("key2", 30);

        // size() returns number of distinct keys, not total values
        assertEquals(2, listMultimap.size());
    }

    @Test
    public void testSize_AfterOperations() {
        assertEquals(0, listMultimap.totalValueCount());
        listMultimap.put("key1", 10);
        assertEquals(1, listMultimap.totalValueCount());
        listMultimap.put("key1", 20);
        assertEquals(2, listMultimap.totalValueCount());
        listMultimap.put("key2", 30);
        assertEquals(3, listMultimap.totalValueCount());
        listMultimap.removeAll("key1");
        assertEquals(1, listMultimap.totalValueCount());
    }

    // valueCollections

    @Test
    public void testValueCollections_EmptyMultimap() {
        Collection<List<Integer>> collections = listMultimap.valueCollections();
        assertNotNull(collections);
        assertTrue(collections.isEmpty());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testValueCollections_SetMultimap() {
        setMultimap.put("key1", 10);
        setMultimap.put("key1", 20);
        setMultimap.put("key2", 30);

        Collection<Set<Integer>> collections = setMultimap.valueCollections();
        assertEquals(2, collections.size());
    }

    @Test
    public void testValueCollections_WithValues() {
        listMultimap.put("key1", 10);
        listMultimap.put("key1", 20);
        listMultimap.put("key2", 30);

        Collection<List<Integer>> collections = listMultimap.valueCollections();
        assertEquals(2, collections.size());
    }

    @Test
    public void testValues_IncludesDuplicatesAcrossKeysForSetMultimap() {
        setMultimap.put("key1", 1);
        setMultimap.put("key2", 1);
        setMultimap.put("key2", 2);

        Collection<Integer> values = setMultimap.allValues();
        assertEquals(3, values.size());
        assertEquals(2, Collections.frequency(values, 1));
    }

    @Test
    public void testValues_IncludesDuplicatesForListMultimap() {
        listMultimap.put("key1", 1);
        listMultimap.put("key1", 1);
        listMultimap.put("key1", 2);

        Collection<Integer> values = listMultimap.allValues();
        assertEquals(3, values.size());
        assertEquals(2, Collections.frequency(values, 1));
        assertEquals(1, Collections.frequency(values, 2));
    }

    @Test
    public void testValues_IsUnmodifiable() {
        listMultimap.put("key1", 1);
        Collection<Integer> values = listMultimap.allValues();

        assertThrows(UnsupportedOperationException.class, () -> values.add(2));
        assertThrows(UnsupportedOperationException.class, () -> values.remove(1));
        assertThrows(UnsupportedOperationException.class, values::clear);
    }

    @Test
    public void testValues_ReflectsLiveChanges() {
        listMultimap.put("key1", 1);
        Collection<Integer> values = listMultimap.allValues();
        assertEquals(1, values.size());

        listMultimap.put("key1", 2);
        listMultimap.put("key2", 3);
        assertEquals(3, values.size());
        assertTrue(values.contains(2));
        assertTrue(values.contains(3));

        listMultimap.removeEntry("key1", 1);
        assertEquals(2, values.size());

        listMultimap.removeAll("key2");
        assertEquals(1, values.size());
        assertTrue(values.contains(2));
    }

    @Test
    public void testValues_ReturnsSameInstance() {
        Collection<Integer> values1 = listMultimap.allValues();
        Collection<Integer> values2 = listMultimap.allValues();
        assertSame(values1, values2);
    }

    @Test
    public void testValues_SupportsNullValues() {
        listMultimap.put("key1", null);
        Collection<Integer> values = listMultimap.allValues();
        assertTrue(values.contains(null));
        assertEquals(1, values.size());
    }
}

package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.Predicate;
import java.util.function.Supplier;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.u.Optional;

@SuppressWarnings({ "unchecked", "rawtypes" })
public class Multimap200Test extends TestBase {

    private ListMultimap<String, Integer> listMultimap;
    private SetMultimap<String, Integer> setMultimap;

    @BeforeEach
    public void setUp() {
        listMultimap = N.newListMultimap();
        setMultimap = N.newSetMultimap();
    }

    // Helper to get a Multimap instance for tests that should be generic
    // For most tests, ListMultimap is suitable to test Multimap's contract.
    private Multimap<String, Integer, List<Integer>> getTestMultimap() {
        return N.newListMultimap();
    }

    private Multimap<String, Integer, Set<Integer>> getSetTestMultimap() {
        return N.newSetMultimap();
    }

    // Test methods for Multimap.java

    @Test
    public void testGetFirst() {
        Multimap<String, Integer, List<Integer>> mm = getTestMultimap();
        assertNull(mm.getFirst("a"));
        mm.put("a", 1);
        assertEquals(Integer.valueOf(1), mm.getFirst("a"));
        mm.put("a", 2); // For ListMultimap, first should be 1
        assertEquals(Integer.valueOf(1), mm.getFirst("a"));

        Multimap<String, Integer, Set<Integer>> smm = getSetTestMultimap();
        smm.put("a", 10);
        assertEquals(Integer.valueOf(10), smm.getFirst("a"));
        smm.put("a", 5); // For SetMultimap, order isn't guaranteed, but there will be one.
        assertNotNull(smm.getFirst("a"));
        assertTrue(smm.get("a").contains(smm.getFirst("a")));
    }

    @Test
    public void testGetFirstOrDefault() {
        Multimap<String, Integer, List<Integer>> mm = getTestMultimap();
        assertEquals(Integer.valueOf(99), mm.getFirstOrDefault("a", 99));
        mm.put("a", 1);
        assertEquals(Integer.valueOf(1), mm.getFirstOrDefault("a", 99));
        mm.put("a", 2);
        assertEquals(Integer.valueOf(1), mm.getFirstOrDefault("a", 99)); // List preserves order
    }

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
        assertEquals(2, valuesA.size()); // Backed collection
        assertTrue(valuesA.containsAll(Arrays.asList(1, 2)));
    }

    @Test
    public void testGetOrDefault() {
        Multimap<String, Integer, List<Integer>> mm = getTestMultimap();
        List<Integer> defaultList = new ArrayList<>(Arrays.asList(99));
        assertEquals(defaultList, mm.getOrDefault("a", defaultList));
        assertFalse(mm.containsKey("a")); // getOrDefault doesn't add if key absent

        mm.put("a", 1);
        List<Integer> valuesA = mm.get("a");
        assertEquals(valuesA, mm.getOrDefault("a", defaultList));
        assertNotEquals(defaultList, mm.getOrDefault("a", defaultList));
    }

    @Test
    public void testPut_singleValue() {
        Multimap<String, Integer, List<Integer>> mm = getTestMultimap();
        assertTrue(mm.put("a", 1));
        assertTrue(mm.contains("a", 1));
        assertEquals(1, mm.get("a").size());

        assertTrue(mm.put("a", 2)); // ListMultimap allows duplicates
        assertEquals(2, mm.get("a").size());
        assertTrue(mm.get("a").containsAll(Arrays.asList(1, 2)));

        assertTrue(mm.put("a", 1)); // For ListMultimap using ArrayList, add always returns true.
                                    // If it were a Set that already contained 1, it would be false.
                                    // The boolean indicates if the underlying *collection* was modified.
                                    // ArrayList.add always returns true.

        Multimap<String, Integer, Set<Integer>> smm = getSetTestMultimap();
        assertTrue(smm.put("b", 10));
        assertTrue(smm.contains("b", 10));
        assertFalse(smm.put("b", 10)); // Set.add returns false if element exists
    }

    @Test
    public void testPut_map() {
        Multimap<String, Integer, List<Integer>> mm = getTestMultimap();
        Map<String, Integer> mapToPut = new HashMap<>();
        mapToPut.put("a", 1);
        mapToPut.put("b", 2);

        assertTrue(mm.put(mapToPut));
        assertTrue(mm.contains("a", 1));
        assertTrue(mm.contains("b", 2));
        assertEquals(1, mm.get("a").size());

        mm.put("a", 0); // a: [0]
        mapToPut.put("a", 3); // Add another value for 'a'
        assertTrue(mm.put(mapToPut)); // "a" collection modified by adding 3
        assertEquals(3, mm.get("a").size());
        assertTrue(mm.get("a").containsAll(Arrays.asList(0, 1, 3)));

        assertFalse(mm.put(Collections.emptyMap()));
    }

    @Test
    public void testPutIfAbsent() {
        // For ListMultimap, putIfAbsent means "add if the specific value is not already present for that key"
        // or if the key is absent.
        Multimap<String, Integer, List<Integer>> mm = getTestMultimap();
        assertTrue(mm.putIfAbsent("a", 1)); // Key 'a' absent, adds 1.
        assertTrue(mm.contains("a", 1));

        assertTrue(mm.putIfAbsent("a", 2)); // Key 'a' present, value 2 absent for 'a', adds 2.
        assertTrue(mm.contains("a", 2));

        mm.put("b", 10);
        assertFalse(mm.putIfAbsent("b", 10)); // Key 'b' present, value 10 present for 'b'.

        // SetMultimap behavior
        Multimap<String, Integer, Set<Integer>> smm = getSetTestMultimap();
        assertTrue(smm.putIfAbsent("x", 100));
        assertTrue(smm.contains("x", 100));
        assertFalse(smm.putIfAbsent("x", 100)); // Key 'x' present, value 100 already in set.
        assertTrue(smm.putIfAbsent("x", 200));
    }

    @Test
    public void testPutIfKeyAbsent() {
        Multimap<String, Integer, List<Integer>> mm = getTestMultimap();
        assertTrue(mm.putIfKeyAbsent("a", 1));
        assertTrue(mm.contains("a", 1));

        assertFalse(mm.putIfKeyAbsent("a", 2)); // Key 'a' is present, so does nothing.
        assertEquals(1, mm.get("a").size()); // Still just [1]
        assertFalse(mm.contains("a", 2));
    }

    @Test
    public void testPutMany_collection() {
        Multimap<String, Integer, List<Integer>> mm = getTestMultimap();
        Collection<Integer> values = Arrays.asList(1, 2, 1);
        assertTrue(mm.putMany("a", values));
        assertEquals(3, mm.get("a").size());
        assertTrue(mm.get("a").containsAll(Arrays.asList(1, 2)));
        assertEquals(2, Collections.frequency(mm.get("a"), 1));

        mm.put("b", 10);
        Collection<Integer> moreValues = Arrays.asList(3, 4);
        assertTrue(mm.putMany("b", moreValues)); // "b" already exists
        assertEquals(3, mm.get("b").size());
        assertTrue(mm.get("b").containsAll(Arrays.asList(10, 3, 4)));

        assertFalse(mm.putMany("c", Collections.emptyList()));
    }

    @Test
    public void testPutManyIfKeyAbsent() {
        Multimap<String, Integer, List<Integer>> mm = getTestMultimap();
        Collection<Integer> values = Arrays.asList(1, 2);
        assertTrue(mm.putManyIfKeyAbsent("a", values));
        assertEquals(2, mm.get("a").size());

        assertFalse(mm.putManyIfKeyAbsent("a", Arrays.asList(3, 4))); // Key 'a' present
        assertEquals(2, mm.get("a").size()); // Unchanged

        assertFalse(mm.putManyIfKeyAbsent("b", Collections.emptyList()));
    }

    @Test
    public void testPutMany_map() {
        Multimap<String, Integer, List<Integer>> mm = getTestMultimap();
        Map<String, Collection<Integer>> mapToPut = new HashMap<>();
        mapToPut.put("a", Arrays.asList(1, 2));
        mapToPut.put("b", Arrays.asList(3));

        assertTrue(mm.putMany(mapToPut));
        assertEquals(2, mm.get("a").size());
        assertEquals(1, mm.get("b").size());

        mapToPut.put("a", Arrays.asList(4)); // Add more to existing "a"
        assertTrue(mm.putMany(mapToPut));
        assertEquals(3, mm.get("a").size()); // 1,2,4
        assertTrue(mm.get("a").containsAll(Arrays.asList(1, 2, 4)));

        Map<String, Collection<Integer>> mapWithEmptyColl = new HashMap<>();
        mapWithEmptyColl.put("c", Collections.emptyList());
        assertFalse(mm.putMany(mapWithEmptyColl)); // If collection is empty, no modification
        assertFalse(mm.containsKey("c"));

        assertFalse(mm.putMany(Collections.emptyMap()));
    }

    @Test
    public void testPutMany_multimap() {
        Multimap<String, Integer, List<Integer>> mm = getTestMultimap();
        ListMultimap<String, Integer> otherMm = N.newListMultimap();
        otherMm.putMany("a", Arrays.asList(1, 2));
        otherMm.put("b", 3);

        assertTrue(mm.putMany(otherMm));
        assertEquals(2, mm.get("a").size());
        assertEquals(1, mm.get("b").size());

        mm.put("a", 0); // mm now a:[0,1,2]
        otherMm.clear();
        otherMm.put("a", 4);
        assertTrue(mm.putMany(otherMm));
        assertEquals(4, mm.get("a").size()); // 0,1,2,4
        assertTrue(mm.get("a").containsAll(Arrays.asList(0, 1, 2, 4)));

        assertFalse(mm.putMany(N.newListMultimap())); // Empty other multimap
    }

    @Test
    public void testRemoveOne_keyValue() {
        Multimap<String, Integer, List<Integer>> mm = getTestMultimap();
        mm.putMany("a", Arrays.asList(1, 2, 1, 3)); // a: [1,2,1,3]
        assertTrue(mm.removeOne("a", 1));
        assertEquals(3, mm.get("a").size()); // a: [2,1,3] (order specific to List.remove)
        assertTrue(mm.get("a").contains(1));

        assertTrue(mm.removeOne("a", 1));
        assertEquals(2, mm.get("a").size()); // a: [2,3]
        assertFalse(mm.get("a").contains(1));

        assertFalse(mm.removeOne("a", 99)); // Value not present for key
        assertFalse(mm.removeOne("b", 1)); // Key not present

        mm.put("c", 10); // c: [10]
        assertTrue(mm.removeOne("c", 10)); // Last element for key 'c'
        assertNull(mm.get("c")); // Key 'c' should be removed
        assertFalse(mm.containsKey("c"));
    }

    @Test
    public void testRemoveOne_map() {
        Multimap<String, Integer, List<Integer>> mm = getTestMultimap();
        mm.putMany("a", Arrays.asList(1, 2, 1)); // a: [1,2,1]
        mm.put("b", 3); // b: [3]

        Map<String, Integer> toRemove = new HashMap<>();
        toRemove.put("a", 1);
        toRemove.put("b", 3);
        toRemove.put("c", 99); // "c" not in mm

        assertTrue(mm.removeOne(toRemove));
        assertEquals(2, mm.get("a").size()); // a: [2,1] or [1,2] depending on List.remove behavior
        assertTrue(mm.get("a").contains(2) && mm.get("a").contains(1));
        assertFalse(mm.containsKey("b")); // b was [3], removed 3 -> empty -> key removed

        assertFalse(mm.removeOne(Collections.emptyMap()));

        // Remove non-existent value for existing key
        mm.clear();
        mm.put("x", 10);
        Map<String, Integer> toRemoveNonExistentVal = N.asMap("x", 99);
        assertFalse(mm.removeOne(toRemoveNonExistentVal));
        assertTrue(mm.contains("x", 10));
    }

    @Test
    public void testRemoveAll_key() {
        Multimap<String, Integer, List<Integer>> mm = getTestMultimap();
        mm.putMany("a", Arrays.asList(1, 2));
        mm.put("b", 3);

        Collection<Integer> removedA = mm.removeAll("a");
        assertNotNull(removedA);
        assertEquals(2, removedA.size());
        assertTrue(removedA.containsAll(Arrays.asList(1, 2)));
        assertFalse(mm.containsKey("a"));
        assertTrue(mm.containsKey("b"));

        assertNull(mm.removeAll("c")); // Key not present
    }

    @Test
    public void testRemoveMany_keyCollection() {
        Multimap<String, Integer, List<Integer>> mm = getTestMultimap();
        mm.putMany("a", Arrays.asList(1, 2, 3, 1, 4)); // a: [1,2,3,1,4]

        assertTrue(mm.removeMany("a", Arrays.asList(1, 3, 99))); // Remove all 1s and 3s. 99 not there.
        assertEquals(2, mm.get("a").size()); // a: [2,4]
        assertFalse(mm.get("a").contains(1));
        assertFalse(mm.get("a").contains(3));

        mm.put("b", 10); // b: [10,2,4] (assuming 'a' became 'b' for this test line)
                         // Let's keep 'a':  a:[2,4]
        mm.put("b", 20); // b: [20]
        assertTrue(mm.removeMany("b", Arrays.asList(20))); // Remove last element for 'b'
        assertTrue(mm.containsKey("b"));

        assertFalse(mm.removeMany("a", Collections.emptyList()));
        assertFalse(mm.removeMany("non_existent_key", Arrays.asList(1)));

        mm.clear();
        mm.put("x", 1);
        mm.put("x", 2);
        assertFalse(mm.removeMany("x", Arrays.asList(3, 4))); // Removing elements not present in value collection
        assertEquals(2, mm.get("x").size());
    }

    @Test
    public void testRemoveMany_map() {
        Multimap<String, Integer, List<Integer>> mm = getTestMultimap();
        mm.putMany("a", Arrays.asList(1, 2, 3, 1)); // a: [1,2,3,1]
        mm.putMany("b", Arrays.asList(4, 5)); // b: [4,5]

        Map<String, Collection<Integer>> toRemove = new HashMap<>();
        toRemove.put("a", Arrays.asList(1, 3)); // Remove 1s and 3 from "a"
        toRemove.put("b", Arrays.asList(4, 5, 6)); // Remove 4, 5 from "b". 6 not there. Key "b" will be removed.
        toRemove.put("c", Arrays.asList(7)); // "c" not in mm.

        assertTrue(mm.removeMany(toRemove));
        assertEquals(1, mm.get("a").size()); // a: [2]
        assertTrue(mm.get("a").contains(2));
        assertFalse(mm.containsKey("b"));

        assertFalse(mm.removeMany(Collections.emptyMap()));
    }

    @Test
    public void testRemoveMany_multimap() {
        Multimap<String, Integer, List<Integer>> mm = getTestMultimap();
        mm.putMany("a", Arrays.asList(1, 2, 3, 1));
        mm.putMany("b", Arrays.asList(4, 5));

        ListMultimap<String, Integer> otherMm = N.newListMultimap();
        otherMm.putMany("a", Arrays.asList(1, 3));
        otherMm.putMany("b", Arrays.asList(4, 5, 6));
        otherMm.put("c", 7);

        assertTrue(mm.removeMany(otherMm));
        assertEquals(1, mm.get("a").size());
        assertTrue(mm.get("a").contains(2));
        assertFalse(mm.containsKey("b"));

        assertFalse(mm.removeMany(N.newListMultimap()));
    }

    @Test
    public void testRemoveOneIf_value_keyPredicate() {
        Multimap<String, Integer, List<Integer>> mm = getTestMultimap();
        mm.putMany("apple", Arrays.asList(1, 2, 1));
        mm.putMany("apricot", Arrays.asList(1, 3));
        mm.putMany("banana", Arrays.asList(1, 4));

        // Remove value 1 if key starts with "ap"
        assertTrue(mm.removeOneIf(1, key -> key.startsWith("ap")));
        assertEquals(Arrays.asList(2, 1), mm.get("apple")); // One '1' removed
        assertEquals(Arrays.asList(3), mm.get("apricot")); // One '1' removed, list becomes empty, key removed.
                                                           // Actually, key should be removed if value list becomes empty.
                                                           // Let's recheck logic of removeOne.
                                                           // removeOne("apricot", 1) -> apricot: [3]
        assertFalse(mm.get("apricot").contains(1));
        assertEquals(Arrays.asList(1, 4), mm.get("banana")); // Unchanged

        // Remove value 1 from "apple" again -> apple: [2]
        assertTrue(mm.removeOneIf(1, key -> key.equals("apple")));
        assertEquals(Arrays.asList(2), mm.get("apple"));

        assertFalse(mm.removeOneIf(99, key -> true)); // Value not present
    }

    @Test
    public void testRemoveOneIf_value_biPredicate() {
        Multimap<String, Integer, List<Integer>> mm = getTestMultimap();
        mm.putMany("a", Arrays.asList(1, 2, 1)); // a: [1,2,1]
        mm.putMany("b", Arrays.asList(1, 3)); // b: [1,3] V contains 1
        mm.putMany("c", Arrays.asList(2, 4)); // c: [2,4] V does not contain 1

        // Remove 1 if key is "a" OR if values contain 3
        assertTrue(mm.removeOneIf(1, (key, values) -> key.equals("a") || values.contains(3)));
        assertEquals(Arrays.asList(2, 1), mm.get("a")); // One 1 removed from "a"
        assertEquals(Arrays.asList(3), mm.get("b")); // One 1 removed from "b" because its values contained 3.
        assertEquals(Arrays.asList(2, 4), mm.get("c")); // Unchanged

        assertFalse(mm.removeOneIf(1, (k, v) -> k.equals("non_existent")));
    }

    @Test
    public void testRemoveManyIf_values_keyPredicate() {
        Multimap<String, Integer, List<Integer>> mm = getTestMultimap();
        mm.putMany("apple", Arrays.asList(1, 2, 1, 5));
        mm.putMany("apricot", Arrays.asList(1, 3, 5));
        mm.putMany("banana", Arrays.asList(1, 4, 5));
        Collection<Integer> valuesToRemove = Arrays.asList(1, 5, 99); // 99 won't be found

        // Remove values {1,5} if key starts with "ap"
        assertTrue(mm.removeManyIf(valuesToRemove, key -> key.startsWith("ap")));
        assertEquals(Arrays.asList(2), mm.get("apple")); // 1,1,5 removed
        assertEquals(Arrays.asList(3), mm.get("apricot")); // 1,5 removed
        assertEquals(Arrays.asList(1, 4, 5), mm.get("banana")); // Unchanged

        assertFalse(mm.removeManyIf(Collections.emptyList(), key -> true));
        assertFalse(mm.removeManyIf(Arrays.asList(100), key -> true)); // Values not present
    }

    @Test
    public void testRemoveManyIf_values_biPredicate() {
        Multimap<String, Integer, List<Integer>> mm = getTestMultimap();
        mm.putMany("a", Arrays.asList(1, 2, 1, 5));
        mm.putMany("b", Arrays.asList(1, 3, 5)); // V contains 3
        mm.putMany("c", Arrays.asList(1, 4, 5)); // V does not contain 3
        Collection<Integer> valuesToRemove = Arrays.asList(1, 5);

        // Remove {1,5} if key is "a" OR if values for that key contain 3
        assertTrue(mm.removeManyIf(valuesToRemove, (key, values) -> key.equals("a") || values.contains(3)));
        assertEquals(Arrays.asList(2), mm.get("a")); // 1,1,5 removed
        assertEquals(Arrays.asList(3), mm.get("b")); // 1,5 removed
        assertEquals(Arrays.asList(1, 4, 5), mm.get("c")); // Unchanged
    }

    @Test
    public void testRemoveAllIf_keyPredicate() {
        Multimap<String, Integer, List<Integer>> mm = getTestMultimap();
        mm.putMany("apple", Arrays.asList(1, 2));
        mm.putMany("apricot", Arrays.asList(3, 4));
        mm.put("banana", 5);

        assertTrue(mm.removeAllIf(key -> key.startsWith("ap")));
        assertFalse(mm.containsKey("apple"));
        assertFalse(mm.containsKey("apricot"));
        assertTrue(mm.containsKey("banana"));

        assertFalse(mm.removeAllIf(key -> key.startsWith("xyz"))); // No keys match
    }

    @Test
    public void testRemoveAllIf_biPredicate() {
        Multimap<String, Integer, List<Integer>> mm = getTestMultimap();
        mm.putMany("a", Arrays.asList(1, 2)); // Sum = 3
        mm.putMany("b", Arrays.asList(3, 4, 5)); // Sum = 12
        mm.put("c", 5); // Sum = 5

        // Remove if key is "a" or sum of values > 10
        assertTrue(mm.removeAllIf((key, values) -> key.equals("a") || values.stream().mapToInt(i -> i).sum() > 10));
        assertFalse(mm.containsKey("a"));
        assertFalse(mm.containsKey("b"));
        assertTrue(mm.containsKey("c"));

        assertFalse(mm.removeAllIf((k, v) -> k.equals("non_existent")));
    }

    // Note: replace methods are complex due to List vs Set behavior for value collection modification.
    // Primarily testing with ListMultimap as it's more complex.
    @Test
    public void testReplaceOne() {
        Multimap<String, Integer, List<Integer>> mm = getTestMultimap();
        mm.putMany("a", Arrays.asList(1, 2, 1, 3)); // a: [1,2,1,3]

        assertTrue(mm.replaceOne("a", 1, 10)); // a: [10,2,1,3] (first 1 replaced)
        assertEquals(Arrays.asList(10, 2, 1, 3), mm.get("a"));

        assertTrue(mm.replaceOne("a", 1, 11)); // a: [10,2,11,3] (second 1 replaced)
        assertEquals(Arrays.asList(10, 2, 11, 3), mm.get("a"));

        assertFalse(mm.replaceOne("a", 99, 100)); // oldValue not found
        assertFalse(mm.replaceOne("b", 1, 10)); // key not found

        // Test with SetMultimap for different replacement characteristic
        Multimap<String, Integer, Set<Integer>> smm = getSetTestMultimap();
        smm.putMany("x", new HashSet<>(Arrays.asList(10, 20, 30)));
        assertTrue(smm.replaceOne("x", 20, 200));
        assertTrue(smm.get("x").containsAll(Arrays.asList(10, 30, 200)));
        assertFalse(smm.get("x").contains(20));

        // IllegalStateException if add fails (hard to simulate with standard collections)
        // This would happen if underlying collection (e.g. a fixed-size one, or one with restrictions)
        // failed on .add(newValue) after .remove(oldValue) for Set-like collections.
    }

    @Test
    public void testReplaceAllWithOne() {
        Multimap<String, Integer, List<Integer>> mm = getTestMultimap();
        mm.putMany("a", Arrays.asList(1, 2, 3));
        assertTrue(mm.replaceAllWithOne("a", 99));
        assertEquals(Arrays.asList(99), mm.get("a"));

        assertFalse(mm.replaceAllWithOne("b", 100)); // Key not found
    }

    @Test
    public void testReplaceManyWithOne() {
        Multimap<String, Integer, List<Integer>> mm = getTestMultimap();
        mm.putMany("a", Arrays.asList(1, 2, 1, 3, 4, 1)); // a: [1,2,1,3,4,1]

        assertTrue(mm.replaceManyWithOne("a", Arrays.asList(1, 3, 99), 100)); // Remove all 1s and 3, then add 100
                                                                              // a: [2,4,100]
        assertEquals(Arrays.asList(2, 4, 100), mm.get("a"));

        assertFalse(mm.replaceManyWithOne("a", Arrays.asList(88, 77), 200)); // oldValues not found
        assertFalse(mm.replaceManyWithOne("b", Arrays.asList(1), 100)); // key not found
        assertFalse(mm.replaceManyWithOne("a", Collections.emptyList(), 300)); // empty oldValues
    }

    @Test
    public void testReplaceOneIf_keyPredicate() {
        Multimap<String, Integer, List<Integer>> mm = getTestMultimap();
        mm.putMany("apple", Arrays.asList(1, 2, 1));
        mm.putMany("apricot", Arrays.asList(1, 3));
        mm.put("banana", 1);

        // Replace 1 with 10 if key starts with "ap"
        assertTrue(mm.replaceOneIf(key -> key.startsWith("ap"), 1, 10));
        assertEquals(Arrays.asList(10, 2, 1), mm.get("apple")); // first 1 replaced
        assertEquals(Arrays.asList(10, 3), mm.get("apricot")); // first 1 replaced
        assertEquals(Arrays.asList(1), mm.get("banana")); // unchanged

        assertFalse(mm.replaceOneIf(key -> true, 99, 100)); // oldValue not found
    }

    @Test
    public void testReplaceOneIf_biPredicate() {
        Multimap<String, Integer, List<Integer>> mm = getTestMultimap();
        mm.putMany("a", Arrays.asList(1, 2, 1)); // values contain 2
        mm.putMany("b", Arrays.asList(1, 3)); // values do not contain 2
        mm.put("c", 1); // key "c"

        // Replace 1 with 10 if key is "c" or values contain 2
        assertTrue(mm.replaceOneIf((k, v) -> k.equals("c") || v.contains(2), 1, 10));
        assertEquals(Arrays.asList(10, 2, 1), mm.get("a"));
        assertEquals(Arrays.asList(1, 3), mm.get("b")); // unchanged
        assertEquals(Arrays.asList(10), mm.get("c"));
    }

    @Test
    public void testReplaceManyWithOneIf_keyPredicate() {
        Multimap<String, Integer, List<Integer>> mm = getTestMultimap();
        mm.putMany("apple", Arrays.asList(1, 2, 1, 5));
        mm.putMany("apricot", Arrays.asList(1, 3, 5));
        mm.put("banana", 1);
        Collection<Integer> oldValues = Arrays.asList(1, 5);

        // Replace {1,5} with 100 if key starts with "ap"
        assertTrue(mm.replaceManyWithOneIf(key -> key.startsWith("ap"), oldValues, 100));
        assertEquals(Arrays.asList(2, 100), mm.get("apple"));
        assertEquals(Arrays.asList(3, 100), mm.get("apricot"));
        assertEquals(Arrays.asList(1), mm.get("banana"));
    }

    @Test
    public void testReplaceManyWithOneIf_biPredicate() {
        Multimap<String, Integer, List<Integer>> mm = getTestMultimap();
        mm.putMany("a", Arrays.asList(1, 2, 1, 5)); // contains 2
        mm.putMany("b", Arrays.asList(1, 3, 5)); // does not contain 2
        mm.put("c", 1); // key "c"
        Collection<Integer> oldValues = Arrays.asList(1, 5);

        // Replace {1,5} with 100 if key "c" or values contain 2
        assertTrue(mm.replaceManyWithOneIf((k, v) -> k.equals("c") || v.contains(2), oldValues, 100));
        assertEquals(Arrays.asList(2, 100), mm.get("a"));
        assertEquals(Arrays.asList(1, 3, 5), mm.get("b")); // unchanged
        // For key "c", values was [1]. oldValues {1,5} remove 1. val becomes []. Then add 100. So c:[100]
        assertEquals(Arrays.asList(100), mm.get("c"));
    }

    @Test
    public void testReplaceAllWithOneIf_keyPredicate() {
        Multimap<String, Integer, List<Integer>> mm = getTestMultimap();
        mm.putMany("apple", Arrays.asList(1, 2, 1, 5));
        mm.putMany("apricot", Arrays.asList(1, 3, 5));
        mm.put("banana", 1);

        assertTrue(mm.replaceAllWithOneIf(key -> key.startsWith("ap"), 100));
        assertEquals(Arrays.asList(100), mm.get("apple"));
        assertEquals(Arrays.asList(100), mm.get("apricot"));
        assertEquals(Arrays.asList(1), mm.get("banana"));
    }

    @Test
    public void testReplaceAllWithOneIf_biPredicate() {
        Multimap<String, Integer, List<Integer>> mm = getTestMultimap();
        mm.putMany("a", Arrays.asList(1, 2, 1, 5)); // contains 2
        mm.putMany("b", Arrays.asList(1, 3, 5)); // does not contain 2
        mm.put("c", 1); // key "c"

        assertTrue(mm.replaceAllWithOneIf((k, v) -> k.equals("c") || v.contains(2), 100));
        assertEquals(Arrays.asList(100), mm.get("a"));
        assertEquals(Arrays.asList(1, 3, 5), mm.get("b")); // unchanged
        assertEquals(Arrays.asList(100), mm.get("c"));
    }

    @Test
    public void testReplaceAll_biFunction() {
        Multimap<String, Integer, List<Integer>> mm = getTestMultimap();
        mm.putMany("a", Arrays.asList(1, 2));
        mm.putMany("b", Arrays.asList(3, 4, 5));

        mm.replaceAll((key, values) -> {
            if (key.equals("a"))
                return new ArrayList<>(Arrays.asList(values.get(0) * 10, values.get(1) * 10));
            if (key.equals("b"))
                return null; // This should remove key "b"
            return values; // Should not happen with current keys
        });

        assertEquals(Arrays.asList(10, 20), mm.get("a"));
        assertFalse(mm.containsKey("b"));

        // Test where function returns empty list
        mm.putMany("c", Arrays.asList(1, 2));
        mm.replaceAll((k, v) -> k.equals("c") ? new ArrayList<>() : v);
        assertFalse(mm.containsKey("c"));
    }

    @Test
    public void testComputeIfAbsent() {
        Multimap<String, Integer, List<Integer>> mm = getTestMultimap();
        Function<String, List<Integer>> mappingFunc = key -> new ArrayList<>(Arrays.asList(key.length()));

        List<Integer> valA = mm.computeIfAbsent("aaa", mappingFunc); // Key absent, computes [3]
        assertEquals(Arrays.asList(3), valA);
        assertEquals(Arrays.asList(3), mm.get("aaa"));

        List<Integer> valAExisting = mm.computeIfAbsent("aaa", mappingFunc); // Key present
        assertEquals(Arrays.asList(3), valAExisting); // Returns existing
        assertSame(valA, valAExisting); // Should return the exact same list object

        // Function returns null or empty
        mm.computeIfAbsent("b", k -> null);
        assertFalse(mm.containsKey("b"));
        mm.computeIfAbsent("c", k -> new ArrayList<>());
        assertFalse(mm.containsKey("c")); // if newValue is empty, key is not added. get(key) will be null.
                                          // The method returns get(key), so if it's not added, returns null.
        assertNull(mm.computeIfAbsent("c", k -> new ArrayList<>()));

        assertThrows(IllegalArgumentException.class, () -> mm.computeIfAbsent("d", null));
    }

    @Test
    public void testComputeIfPresent() {
        Multimap<String, Integer, List<Integer>> mm = getTestMultimap();
        BiFunction<String, List<Integer>, List<Integer>> remappingFunc = (key, oldValues) -> {
            List<Integer> newValues = new ArrayList<>(oldValues);
            newValues.add(key.length());
            return newValues;
        };

        assertNull(mm.computeIfPresent("absent", remappingFunc)); // Key not present

        mm.put("aaa", 1); // aaa: [1]
        List<Integer> valA = mm.computeIfPresent("aaa", remappingFunc); // aaa: [1,3]
        assertEquals(Arrays.asList(1, 3), valA);
        assertEquals(Arrays.asList(1, 3), mm.get("aaa"));

        // Remapping function returns null or empty -> removes key
        mm.put("b", 10);
        assertNull(mm.computeIfPresent("b", (k, v) -> null));
        assertFalse(mm.containsKey("b"));

        mm.put("c", 20);
        assertNull(mm.computeIfPresent("c", (k, v) -> new ArrayList<>()));
        assertFalse(mm.containsKey("c"));

        assertThrows(IllegalArgumentException.class, () -> mm.computeIfPresent("d", null));
    }

    @Test
    public void testCompute() {
        Multimap<String, Integer, List<Integer>> mm = getTestMultimap();
        BiFunction<String, List<Integer>, List<Integer>> remappingFunc = (key, oldValues) -> {
            if (oldValues == null)
                return new ArrayList<>(Arrays.asList(key.length())); // Compute for absent
            List<Integer> newValues = new ArrayList<>(oldValues); // Modify for present
            newValues.add(key.length() * 2);
            return newValues;
        };

        List<Integer> valAbsent = mm.compute("new", remappingFunc); // oldValues is null
        assertEquals(Arrays.asList(3), valAbsent); // "new".length() = 3
        assertEquals(Arrays.asList(3), mm.get("new"));

        List<Integer> valPresent = mm.compute("new", remappingFunc); // oldValues is [3]
        assertEquals(Arrays.asList(3, 6), valPresent); // 3, "new".length()*2=6
        assertEquals(Arrays.asList(3, 6), mm.get("new"));
        assertSame(mm.get("new"), valPresent); // check if it's the same list object

        // Compute returns null or empty
        assertEquals(Arrays.asList(1), mm.compute("presentToNull", (k, v) -> v == null ? Arrays.asList(1) : null)); // first call creates [1]
        assertEquals(Arrays.asList(1), mm.get("presentToNull"));
        assertNull(mm.compute("presentToNull", (k, v) -> null)); // second call removes it
        assertFalse(mm.containsKey("presentToNull"));

        assertThrows(IllegalArgumentException.class, () -> mm.compute("d", null));
    }

    @Test
    public void testMerge_collection() {
        Multimap<String, Integer, List<Integer>> mm = getTestMultimap();
        List<Integer> elementsToMerge = Arrays.asList(10, 20);
        BiFunction<List<Integer>, List<Integer>, List<Integer>> remappingFunc = (oldVal, newElements) -> {
            List<Integer> merged = new ArrayList<>(oldVal);
            merged.addAll(newElements);
            return merged;
        };

        // Key absent
        List<Integer> mergedValAbsent = mm.merge("a", elementsToMerge, remappingFunc);
        assertEquals(elementsToMerge, mergedValAbsent); // putMany is called, then get
        assertEquals(elementsToMerge, mm.get("a"));

        // Key present
        List<Integer> moreElements = Arrays.asList(30);
        List<Integer> mergedValPresent = mm.merge("a", moreElements, remappingFunc); // a: [10,20,30]
        assertEquals(Arrays.asList(10, 20, 30), mergedValPresent);
        assertEquals(Arrays.asList(10, 20, 30), mm.get("a"));

        // Remapping func returns null
        assertNull(mm.merge("a", Arrays.asList(40), (ov, nv) -> null));
        assertFalse(mm.containsKey("a"));

        assertThrows(IllegalArgumentException.class, () -> mm.merge("b", elementsToMerge, null));
        assertThrows(IllegalArgumentException.class, () -> mm.merge("b", null, remappingFunc));
    }

    @Test
    public void testMerge_element() {
        Multimap<String, Integer, List<Integer>> mm = getTestMultimap();
        Integer elementToMerge = 100;
        BiFunction<List<Integer>, Integer, List<Integer>> remappingFunc = (oldVal, newElement) -> {
            List<Integer> merged = new ArrayList<>(oldVal);
            merged.add(newElement);
            return merged;
        };

        // Key absent
        List<Integer> mergedValAbsent = mm.merge("x", elementToMerge, remappingFunc);
        assertEquals(Arrays.asList(100), mergedValAbsent);
        assertEquals(Arrays.asList(100), mm.get("x"));

        // Key present
        Integer moreElement = 200;
        List<Integer> mergedValPresent = mm.merge("x", moreElement, remappingFunc); // x: [100, 200]
        assertEquals(Arrays.asList(100, 200), mergedValPresent);

        assertThrows(IllegalArgumentException.class, () -> mm.merge("y", elementToMerge, null));
        // assertThrows(IllegalArgumentException.class, () -> mm.merge("y", null, remappingFunc)); // 'e' cannot be null here
    }

    @Test
    public void testInverse() {
        Multimap<String, Integer, List<Integer>> mm = getTestMultimap();
        mm.put("a", 1);
        mm.put("a", 2);
        mm.put("b", 1);
        // mm: a -> [1,2], b -> [1]

        // We need a supplier for the inverted Multimap.
        // It will be Multimap<Integer, String, List<String>>
        IntFunction<ListMultimap<Integer, String>> supplier = size -> N.newListMultimap();
        Multimap<Integer, String, List<String>> inverted = mm.inverse(supplier);

        assertTrue(inverted.contains(1, "a"));
        assertTrue(inverted.contains(1, "b"));
        assertTrue(inverted.contains(2, "a"));
        assertEquals(2, inverted.get(1).size());
        assertEquals(1, inverted.get(2).size());

        Multimap<String, Integer, List<Integer>> emptyMm = getTestMultimap();
        Multimap<Integer, String, List<String>> invertedEmpty = emptyMm.inverse(supplier);
        assertTrue(invertedEmpty.isEmpty());
    }

    @Test
    public void testCopy() {
        Multimap<String, Integer, List<Integer>> mm = getTestMultimap();
        mm.put("a", 1);
        mm.putMany("b", Arrays.asList(2, 3));

        Multimap<String, Integer, List<Integer>> copy = mm.copy();
        assertNotSame(mm, copy);
        assertEquals(mm, copy); // checks backingMap.equals

        // Ensure deep enough copy of value collections (new collections, same elements)
        assertNotSame(mm.get("a"), copy.get("a"));
        assertEquals(mm.get("a"), copy.get("a"));

        copy.put("a", 100);
        assertTrue(mm.get("a").contains(1)); // Original unchanged
        assertFalse(mm.get("a").contains(100));
    }

    @Test
    public void testContains_keyValue() {
        Multimap<String, Integer, List<Integer>> mm = getTestMultimap();
        mm.put("a", 1);
        assertTrue(mm.contains("a", 1));
        assertFalse(mm.contains("a", 2));
        assertFalse(mm.contains("b", 1));
    }

    @Test
    public void testContainsKey() {
        Multimap<String, Integer, List<Integer>> mm = getTestMultimap();
        mm.put("a", 1);
        assertTrue(mm.containsKey("a"));
        assertFalse(mm.containsKey("b"));
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
    public void testContainsAll_keyCollection() {
        Multimap<String, Integer, List<Integer>> mm = getTestMultimap();
        mm.putMany("a", Arrays.asList(1, 2, 3, 1));
        assertTrue(mm.containsAll("a", Arrays.asList(1, 2)));
        assertTrue(mm.containsAll("a", Arrays.asList(1, 1, 2))); // ListMultimap specific
        assertFalse(mm.containsAll("a", Arrays.asList(1, 4)));
        assertTrue(mm.containsAll("a", Collections.emptyList()));
        assertFalse(mm.containsAll("b", Arrays.asList(1))); // Key not present

        Multimap<String, Integer, Set<Integer>> smm = getSetTestMultimap();
        smm.putMany("x", new HashSet<>(Arrays.asList(10, 20, 30)));
        assertTrue(smm.containsAll("x", new HashSet<>(Arrays.asList(10, 20))));
        assertFalse(smm.containsAll("x", new HashSet<>(Arrays.asList(10, 40))));
    }

    @Test
    public void testFilterByKey() {
        Multimap<String, Integer, List<Integer>> mm = getTestMultimap();
        mm.put("apple", 1);
        mm.put("apricot", 2);
        mm.put("banana", 3);
        Predicate<String> startsWithAp = k -> k.startsWith("ap");
        Multimap<String, Integer, List<Integer>> filtered = mm.filterByKey(startsWithAp);

        assertEquals(2, filtered.size());
        assertTrue(filtered.containsKey("apple"));
        assertTrue(filtered.containsKey("apricot"));
        assertFalse(filtered.containsKey("banana"));
        // Ensure values are references, not copies if that's the contract
        // The current impl creates new multimap but puts the original value collections.
        assertSame(mm.get("apple"), filtered.get("apple"));
    }

    @Test
    public void testFilterByValue() {
        Multimap<String, Integer, List<Integer>> mm = getTestMultimap();
        mm.putMany("a", Arrays.asList(1, 2));
        mm.putMany("b", Arrays.asList(3, 4, 5));
        mm.putMany("c", Arrays.asList(1));
        Predicate<List<Integer>> hasEvenNumber = v -> v.stream().anyMatch(i -> i % 2 == 0);
        Multimap<String, Integer, List<Integer>> filtered = mm.filterByValue(hasEvenNumber);

        assertEquals(2, filtered.size());
        assertTrue(filtered.containsKey("a")); // 1,2 (has 2)
        assertTrue(filtered.containsKey("b")); // 3,4,5 (has 4)
        assertFalse(filtered.containsKey("c"));// 1 (no even)
        assertSame(mm.get("a"), filtered.get("a"));
    }

    @Test
    public void testFilter_biPredicate() {
        Multimap<String, Integer, List<Integer>> mm = getTestMultimap();
        mm.putMany("a", Arrays.asList(1, 2));
        mm.putMany("b", Arrays.asList(10, 20));
        BiPredicate<String, List<Integer>> filter = (k, v) -> k.equals("a") || v.contains(20);
        Multimap<String, Integer, List<Integer>> filtered = mm.filter(filter);

        assertEquals(2, filtered.size());
        assertTrue(filtered.containsKey("a"));
        assertTrue(filtered.containsKey("b"));
        assertSame(mm.get("a"), filtered.get("a"));
    }

    @Test
    public void testForEach_biConsumer() {
        Multimap<String, Integer, List<Integer>> mm = getTestMultimap();
        mm.putMany("a", Arrays.asList(1, 2));
        mm.put("b", 3);
        Map<String, List<Integer>> result = new HashMap<>();
        mm.forEach(result::put);

        assertEquals(2, result.size());
        assertEquals(Arrays.asList(1, 2), result.get("a"));
        assertEquals(Arrays.asList(3), result.get("b"));
        assertThrows(NullPointerException.class, () -> mm.forEach((Consumer) null));
    }

    @Test
    public void testFlatForEach() {
        Multimap<String, Integer, List<Integer>> mm = getTestMultimap();
        mm.putMany("a", Arrays.asList(1, 2));
        mm.put("b", 3);
        List<Pair<String, Integer>> pairs = new ArrayList<>();
        mm.flatForEach((k, e) -> pairs.add(Pair.of(k, e)));

        assertEquals(3, pairs.size());
        assertTrue(pairs.contains(Pair.of("a", 1)));
        assertTrue(pairs.contains(Pair.of("a", 2)));
        assertTrue(pairs.contains(Pair.of("b", 3)));
        assertThrows(IllegalArgumentException.class, () -> mm.flatForEach(null));
    }

    @Test
    public void testForEachKey() {
        Multimap<String, Integer, List<Integer>> mm = getTestMultimap();
        mm.put("a", 1);
        mm.put("b", 2);
        Set<String> keys = new HashSet<>();
        mm.forEachKey(keys::add);
        assertEquals(N.asSet("a", "b"), keys);
        assertThrows(IllegalArgumentException.class, () -> mm.forEachKey(null));
    }

    @Test
    public void testForEachValue() {
        Multimap<String, Integer, List<Integer>> mm = getTestMultimap();
        mm.putMany("a", Arrays.asList(1, 2));
        mm.putMany("b", Arrays.asList(3));
        List<Collection<Integer>> valuesList = new ArrayList<>();
        mm.forEachValue(valuesList::add);

        assertEquals(2, valuesList.size());
        assertTrue(valuesList.contains(Arrays.asList(1, 2)));
        assertTrue(valuesList.contains(Arrays.asList(3)));
        assertThrows(IllegalArgumentException.class, () -> mm.forEachValue(null));
    }

    @Test
    public void testFlatForEachValue() {
        Multimap<String, Integer, List<Integer>> mm = getTestMultimap();
        mm.putMany("a", Arrays.asList(1, 2));
        mm.put("b", 3);
        List<Integer> elements = new ArrayList<>();
        mm.flatForEachValue(elements::add);

        assertEquals(3, elements.size());
        assertTrue(elements.containsAll(Arrays.asList(1, 2, 3)));
        assertThrows(IllegalArgumentException.class, () -> mm.flatForEachValue(null));
    }

    @Test
    public void testKeySet() {
        Multimap<String, Integer, List<Integer>> mm = getTestMultimap();
        mm.put("a", 1);
        mm.put("b", 2);
        assertEquals(N.asSet("a", "b"), mm.keySet());
        // Test modification through keySet
        mm.keySet().remove("a");
        assertFalse(mm.containsKey("a"));
    }

    @Test
    public void testValues() {
        Multimap<String, Integer, List<Integer>> mm = getTestMultimap();
        List<Integer> listA = Arrays.asList(1, 2);
        List<Integer> listB = Arrays.asList(3);
        mm.putMany("a", listA);
        mm.putMany("b", listB);

        Collection<List<Integer>> valuesCol = mm.values();
        assertEquals(2, valuesCol.size());
        // Content check is tricky due to collection of collections.
        // Ensure the exact list instances are present
        AtomicInteger foundA = new AtomicInteger();
        AtomicInteger foundB = new AtomicInteger();
        valuesCol.forEach(v -> {
            if (v.equals(listA))
                foundA.incrementAndGet();
            if (v.equals(listB))
                foundB.incrementAndGet();
        });
        assertEquals(1, foundA.get());
        assertEquals(1, foundB.get());
    }

    @Test
    public void testEntrySet() {
        Multimap<String, Integer, List<Integer>> mm = getTestMultimap();
        mm.putMany("a", Arrays.asList(1, 2));
        Set<Map.Entry<String, List<Integer>>> entrySet = mm.entrySet();
        assertEquals(1, entrySet.size());
        Map.Entry<String, List<Integer>> entry = entrySet.iterator().next();
        assertEquals("a", entry.getKey());
        assertEquals(Arrays.asList(1, 2), entry.getValue());
    }

    @Test
    public void testFlatValues() {
        Multimap<String, Integer, List<Integer>> mm = getTestMultimap();
        mm.putMany("a", Arrays.asList(1, 2));
        mm.put("b", 3);
        List<Integer> flat = mm.flatValues();
        assertEquals(3, flat.size());
        assertTrue(flat.containsAll(Arrays.asList(1, 2, 3)));
    }

    @Test
    public void testFlatValues_supplier() {
        Multimap<String, Integer, List<Integer>> mm = getTestMultimap();
        mm.putMany("a", Arrays.asList(1, 2));
        mm.put("b", 3);
        HashSet<Integer> flatSet = mm.flatValues(HashSet::new); // size -> new HashSet<>(size) not used by this HashSet constructor
                                                                // HashSet::new is IntFunction<C> where C = HashSet
        assertEquals(3, flatSet.size()); // 1,2,3 unique
        assertTrue(flatSet.containsAll(Arrays.asList(1, 2, 3)));
    }

    @Test
    public void testToMap() {
        Multimap<String, Integer, List<Integer>> mm = getTestMultimap();
        mm.putMany("a", Arrays.asList(1, 2));
        mm.put("b", 3);
        Map<String, List<Integer>> map = mm.toMap();
        assertEquals(2, map.size());
        assertEquals(Arrays.asList(1, 2), map.get("a"));
        assertNotSame(mm.get("a"), map.get("a")); // Should be a copy of the value collection
    }

    @Test
    public void testToMap_supplier() {
        Multimap<String, Integer, List<Integer>> mm = getTestMultimap();
        mm.putMany("a", Arrays.asList(1, 2));
        Map<String, List<Integer>> linkedMap = mm.toMap(LinkedHashMap::new);
        assertTrue(linkedMap instanceof LinkedHashMap);
        assertEquals(Arrays.asList(1, 2), linkedMap.get("a"));
    }

    @Test
    public void testToMultiset() {
        Multimap<String, Integer, List<Integer>> mm = getTestMultimap();
        mm.putMany("a", Arrays.asList(1, 2, 1)); // a has 3 values
        mm.put("b", 3); // b has 1 value
        Multiset<String> ms = mm.toMultiset();
        assertEquals(3, ms.getCount("a"));
        assertEquals(1, ms.getCount("b"));
        assertEquals(2, ms.countOfDistinctElements());
    }

    @Test
    public void testIterator() {
        Multimap<String, Integer, List<Integer>> mm = getTestMultimap();
        mm.putMany("a", Arrays.asList(1, 2));
        mm.put("b", 3);
        Iterator<Map.Entry<String, List<Integer>>> it = mm.iterator();
        int count = 0;
        while (it.hasNext()) {
            it.next();
            count++;
        }
        assertEquals(2, count); // number of keys
    }

    @Test
    public void testStream() {
        Multimap<String, Integer, List<Integer>> mm = getTestMultimap();
        mm.putMany("a", Arrays.asList(1, 2));
        mm.put("b", 3);
        assertEquals(2, mm.stream().count());
    }

    @Test
    public void testEntryStream() {
        Multimap<String, Integer, List<Integer>> mm = getTestMultimap();
        mm.putMany("a", Arrays.asList(1, 2));
        mm.put("b", 3);
        assertEquals(2, mm.entryStream().count());
    }

    @Test
    public void testClear() {
        Multimap<String, Integer, List<Integer>> mm = getTestMultimap();
        mm.put("a", 1);
        mm.clear();
        assertTrue(mm.isEmpty());
        assertEquals(0, mm.size());
    }

    @Test
    public void testSize() {
        Multimap<String, Integer, List<Integer>> mm = getTestMultimap();
        assertEquals(0, mm.size());
        mm.put("a", 1);
        assertEquals(1, mm.size());
        mm.put("b", 2);
        assertEquals(2, mm.size());
        mm.put("a", 3); // same key, size doesn't change
        assertEquals(2, mm.size());
    }

    @Test
    public void testTotalCountOfValues() {
        Multimap<String, Integer, List<Integer>> mm = getTestMultimap();
        assertEquals(0, mm.totalCountOfValues());
        mm.put("a", 1);
        assertEquals(1, mm.totalCountOfValues());
        mm.putMany("b", Arrays.asList(2, 3));
        assertEquals(3, mm.totalCountOfValues());
        mm.put("a", 4); // a: [1,4], b: [2,3]
        assertEquals(4, mm.totalCountOfValues());
    }

    @Test
    public void testIsEmpty() {
        Multimap<String, Integer, List<Integer>> mm = getTestMultimap();
        assertTrue(mm.isEmpty());
        mm.put("a", 1);
        assertFalse(mm.isEmpty());
        mm.removeAll("a");
        assertTrue(mm.isEmpty());
    }

    // Functional methods (apply, accept) - assuming Throwables.* exist and work like standard FI for non-exception cases.
    @Test
    public void testApply() {
        Multimap<String, Integer, List<Integer>> mm = getTestMultimap();
        mm.put("a", 1);
        Integer result = mm.apply(m -> m.totalCountOfValues() + m.size());
        assertEquals(Integer.valueOf(1 + 1), result);
    }

    @Test
    public void testApplyIfNotEmpty() {
        Multimap<String, Integer, List<Integer>> mmEmpty = getTestMultimap();
        Multimap<String, Integer, List<Integer>> mmNonEmpty = getTestMultimap();
        mmNonEmpty.put("a", 1);

        Optional<Integer> emptyResult = mmEmpty.applyIfNotEmpty(m -> m.size());
        assertFalse(emptyResult.isPresent());

        Optional<Integer> nonEmptyResult = mmNonEmpty.applyIfNotEmpty(m -> m.size());
        assertTrue(nonEmptyResult.isPresent());
        assertEquals(Integer.valueOf(1), nonEmptyResult.get());
    }

    @Test
    public void testAccept() {
        Multimap<String, Integer, List<Integer>> mm = getTestMultimap();
        AtomicBoolean accepted = new AtomicBoolean(false);
        mm.accept(m -> accepted.set(true));
        assertTrue(accepted.get());
    }

    @Test
    public void testAcceptIfNotEmpty() {
        Multimap<String, Integer, List<Integer>> mmEmpty = getTestMultimap();
        Multimap<String, Integer, List<Integer>> mmNonEmpty = getTestMultimap();
        mmNonEmpty.put("a", 1);
        AtomicInteger emptyCounter = new AtomicInteger(0);
        AtomicInteger nonEmptyCounter = new AtomicInteger(0);

        mmEmpty.acceptIfNotEmpty(m -> emptyCounter.incrementAndGet());
        assertEquals(0, emptyCounter.get());

        mmNonEmpty.acceptIfNotEmpty(m -> nonEmptyCounter.incrementAndGet());
        assertEquals(1, nonEmptyCounter.get());
    }

    @Test
    public void testHashCodeAndEquals() {
        Multimap<String, Integer, List<Integer>> mm1 = N.newListMultimap();
        mm1.putMany("a", Arrays.asList(1, 2));
        mm1.put("b", 3);

        Multimap<String, Integer, List<Integer>> mm2 = N.newListMultimap();
        mm2.put("b", 3);
        mm2.putMany("a", Arrays.asList(1, 2));

        assertEquals(mm1.hashCode(), mm2.hashCode());
        assertTrue(mm1.equals(mm2));
        assertTrue(mm2.equals(mm1));
        assertTrue(mm1.equals(mm1));

        Multimap<String, Integer, List<Integer>> mm3 = N.newListMultimap();
        mm3.putMany("a", Arrays.asList(1, 2)); // Missing "b"
        assertNotEquals(mm1.hashCode(), mm3.hashCode());
        assertFalse(mm1.equals(mm3));

        Multimap<String, Integer, List<Integer>> mm4 = N.newListMultimap();
        mm4.putMany("a", Arrays.asList(1, 2, 3)); // Different value for "a"
        mm4.put("b", 3);
        assertNotEquals(mm1.hashCode(), mm4.hashCode());
        assertFalse(mm1.equals(mm4));

        assertFalse(mm1.equals(null));
        assertFalse(mm1.equals(new Object()));

        // Test with SetMultimap
        Multimap<String, Integer, Set<Integer>> smm1 = N.newSetMultimap();
        smm1.putMany("a", new HashSet<>(Arrays.asList(1, 2)));
        smm1.put("b", 3);

        Multimap<String, Integer, Set<Integer>> smm2 = N.newSetMultimap();
        smm2.put("b", 3);
        smm2.putMany("a", new HashSet<>(Arrays.asList(2, 1))); // Order in set doesn't matter

        assertEquals(smm1.hashCode(), smm2.hashCode());
        assertTrue(smm1.equals(smm2));

        assertFalse(mm1.equals(smm1)); // Different types of value collections
    }

    @Test
    public void testToString() {
        Multimap<String, Integer, List<Integer>> mm = getTestMultimap();
        mm.putMany("a", Arrays.asList(1, 2));
        mm.put("b", 3);
        // Backing map is HashMap, so order is not guaranteed in toString.
        String str = mm.toString();
        assertTrue(str.startsWith("{") && str.endsWith("}"));
        assertTrue(str.contains("a=[1, 2]") || str.contains("a=[2, 1]")); // List preserves order internally
        assertTrue(str.contains("b=[3]"));
        assertEquals(("{" + "a=[1, 2]" + ", " + "b=[3]" + "}").length(), str.length());

        Multimap<String, Integer, List<Integer>> linkedMm = N.newListMultimap(LinkedHashMap.class, ArrayList.class);
        linkedMm.put("z", 10);
        linkedMm.put("y", 20);
        assertEquals("{z=[10], y=[20]}", linkedMm.toString());
    }

    // Static factory methods (of)
    @Test
    public void testOfFactories() {
        ListMultimap<String, Integer> lm1 = ListMultimap.of("a", 1);
        assertEquals(1, lm1.size());
        assertEquals(Arrays.asList(1), lm1.get("a"));

        ListMultimap<String, Integer> lm2 = ListMultimap.of("a", 1, "b", 2);
        assertEquals(2, lm2.size());
        assertEquals(Arrays.asList(1), lm2.get("a"));
        assertEquals(Arrays.asList(2), lm2.get("b"));

        ListMultimap<String, Integer> lm3 = ListMultimap.of("a", 1, "a", 2);
        assertEquals(1, lm3.size()); // One key "a"
        assertEquals(Arrays.asList(1, 2), lm3.get("a"));

        ListMultimap<String, Integer> lm7 = ListMultimap.of("a", 1, "b", 2, "c", 3, "d", 4, "e", 5, "f", 6, "g", 7);
        assertEquals(7, lm7.size());
        assertEquals(Arrays.asList(7), lm7.get("g"));
    }

    // Static factory methods (create)
    @Test
    public void testCreateFromMap() {
        Map<String, Integer> sourceMap = new HashMap<>();
        sourceMap.put("a", 1);
        sourceMap.put("b", 2);
        ListMultimap<String, Integer> lm = ListMultimap.create(sourceMap);
        assertEquals(2, lm.size());
        assertEquals(Arrays.asList(1), lm.get("a"));
        assertEquals(Arrays.asList(2), lm.get("b"));

        ListMultimap<String, Integer> lmEmpty = ListMultimap.create(Collections.emptyMap());
        assertTrue(lmEmpty.isEmpty());
    }

    @Test
    public void testCreateFromCollectionWithKeyExtractor() {
        List<String> data = Arrays.asList("apple", "apricot", "banana");
        ListMultimap<Character, String> lm = ListMultimap.create(data, s -> s.charAt(0));
        assertEquals(2, lm.size()); // Keys 'a', 'b'
        assertEquals(Arrays.asList("apple", "apricot"), lm.get('a'));
        assertEquals(Arrays.asList("banana"), lm.get('b'));

        ListMultimap<Character, String> lmEmpty = ListMultimap.create(Collections.emptyList(), s -> s.charAt(0));
        assertTrue(lmEmpty.isEmpty());
        assertThrows(IllegalArgumentException.class, () -> ListMultimap.create(data, null));
    }

    @Test
    public void testCreateFromCollectionWithKeyAndValueExtractors() {
        List<String> data = Arrays.asList("apple:fruit", "banana:fruit", "carrot:vegetable");
        ListMultimap<String, String> lm = ListMultimap.create(data, s -> s.split(":")[1], // type as key
                s -> s.split(":")[0]); // name as value
        assertEquals(2, lm.size());
        assertEquals(Arrays.asList("apple", "banana"), lm.get("fruit"));
        assertEquals(Arrays.asList("carrot"), lm.get("vegetable"));

        ListMultimap<String, String> lmEmpty = ListMultimap.create(Collections.<String> emptyList(), s -> s, s -> s);
        assertTrue(lmEmpty.isEmpty());
    }

    // Static factory methods (concat)
    @Test
    public void testConcatTwoMaps() {
        Map<String, Integer> mapA = Collections.singletonMap("a", 1);
        Map<String, Integer> mapB = Collections.singletonMap("b", 2);
        ListMultimap<String, Integer> lm = ListMultimap.concat(mapA, mapB);
        assertEquals(2, lm.size());
        assertEquals(Arrays.asList(1), lm.get("a"));
        assertEquals(Arrays.asList(2), lm.get("b"));

        Map<String, Integer> mapC = Collections.singletonMap("a", 3); // Duplicate key
        ListMultimap<String, Integer> lmDup = ListMultimap.concat(mapA, mapC);
        assertEquals(1, lmDup.size());
        assertEquals(Arrays.asList(1, 3), lmDup.get("a"));

        assertEquals(Arrays.asList(1), ListMultimap.concat(mapA, null).get("a"));
        assertEquals(Arrays.asList(2), ListMultimap.concat(null, mapB).get("b"));
        assertTrue(ListMultimap.concat(null, null).isEmpty());
    }

    @Test
    public void testConcatThreeMaps() {
        Map<String, Integer> mapA = Collections.singletonMap("a", 1);
        Map<String, Integer> mapB = Collections.singletonMap("b", 2);
        Map<String, Integer> mapC = Collections.singletonMap("c", 3);
        ListMultimap<String, Integer> lm = ListMultimap.concat(mapA, mapB, mapC);
        assertEquals(3, lm.size());
        assertEquals(Arrays.asList(3), lm.get("c"));
    }

    @Test
    public void testConcatCollectionOfMaps() {
        Collection<Map<String, Integer>> maps = Arrays.asList(Collections.singletonMap("a", 1), Collections.singletonMap("b", 2),
                Collections.singletonMap("a", 3));
        ListMultimap<String, Integer> lm = ListMultimap.concat(maps);
        assertEquals(2, lm.size());
        assertEquals(Arrays.asList(1, 3), lm.get("a"));
        assertEquals(Arrays.asList(2), lm.get("b"));

        assertTrue(ListMultimap.concat(Collections.emptyList()).isEmpty());
    }

    // Static factory methods (wrap)
    @Test
    public void testWrapMap() {
        Map<String, List<Integer>> sourceMap = new HashMap<>();
        List<Integer> listA = new ArrayList<>(Arrays.asList(1, 2));
        sourceMap.put("a", listA);

        ListMultimap<String, Integer> lm = ListMultimap.wrap(sourceMap);
        assertEquals(Arrays.asList(1, 2), lm.get("a"));
        assertSame(listA, lm.get("a"), "Wrapped map should share the same value collection instance");

        // Modify wrapped map, check original
        lm.put("a", 3); // listA becomes [1,2,3]
        assertEquals(Arrays.asList(1, 2, 3), sourceMap.get("a"));

        // Modify original map, check wrapped
        sourceMap.get("a").add(4); // listA becomes [1,2,3,4]
        assertEquals(Arrays.asList(1, 2, 3, 4), lm.get("a"));

        assertThrows(IllegalArgumentException.class, () -> ListMultimap.wrap(null));
        Map<String, List<Integer>> mapWithNullValue = new HashMap<>();
        mapWithNullValue.put("x", null);
        // N.checkArgument(N.anyNull(map.values()), ...) will throw
        assertThrows(IllegalArgumentException.class, () -> ListMultimap.wrap(mapWithNullValue));
    }

    @Test
    public void testWrapMapWithValueSupplier() {
        Map<String, List<Integer>> sourceMap = new LinkedHashMap<>(); // Keep order for predictable test
        Supplier<List<Integer>> valueSupplier = LinkedList::new;

        ListMultimap<String, Integer> lm = ListMultimap.wrap(sourceMap, valueSupplier);
        lm.put("a", 1); // This will use the valueSupplier to create a LinkedList
        assertTrue(sourceMap.get("a") instanceof LinkedList);
        assertEquals(Arrays.asList(1), sourceMap.get("a"));

        assertThrows(IllegalArgumentException.class, () -> ListMultimap.wrap(null, valueSupplier));
        assertThrows(IllegalArgumentException.class, () -> ListMultimap.wrap(sourceMap, null));
    }

    // Overridden instance methods
    @Test
    public void testGetFirst_ListMultimapSpecific() {
        listMultimap.put("a", 10);
        listMultimap.put("a", 20); // List: [10, 20]
        assertEquals(Integer.valueOf(10), listMultimap.getFirst("a"));
        assertNull(listMultimap.getFirst("nonExistentKey"));
    }

    @Test
    public void testGetFirstOrDefault_ListMultimapSpecific() {
        listMultimap.put("a", 10);
        listMultimap.put("a", 20);
        assertEquals(Integer.valueOf(10), listMultimap.getFirstOrDefault("a", 99));
        assertEquals(Integer.valueOf(99), listMultimap.getFirstOrDefault("nonExistentKey", 99));
    }

    @Test
    public void testInverse_ListMultimapSpecific() {
        listMultimap.put("k1", 100);
        listMultimap.put("k1", 200);
        listMultimap.put("k2", 100);
        // {"k1"=[100, 200], "k2"=[100]}

        ListMultimap<Integer, String> inverted = listMultimap.inverse();
        assertEquals(2, inverted.size()); // Keys are 100, 200
        assertEquals(Arrays.asList("k1", "k2"), inverted.get(100));
        assertEquals(Arrays.asList("k1"), inverted.get(200));
        assertTrue(inverted.get(100) instanceof List);
    }

    @Test
    public void testCopy_ListMultimapSpecific() {
        listMultimap.put("a", 1);
        ListMultimap<String, Integer> copy = listMultimap.copy();
        assertNotSame(listMultimap, copy);
        assertEquals(listMultimap, copy);
        assertTrue(copy instanceof ListMultimap);
        assertEquals(Arrays.asList(1), copy.get("a"));

        // Ensure value collections are also new instances (deep enough copy)
        assertNotSame(listMultimap.get("a"), copy.get("a"));
        assertEquals(listMultimap.get("a"), copy.get("a"));

        copy.get("a").add(2);
        assertEquals(Arrays.asList(1), listMultimap.get("a")); // Original unchanged
    }

    @Test
    public void testFilterByKey_ListMultimapSpecific() {
        listMultimap.put("apple", 1);
        listMultimap.put("banana", 2);
        ListMultimap<String, Integer> filtered = listMultimap.filterByKey(s -> s.startsWith("a"));
        assertTrue(filtered instanceof ListMultimap);
        assertEquals(1, filtered.size());
        assertTrue(filtered.containsKey("apple"));
    }

    @Test
    public void testFilterByValue_ListMultimapSpecific() {
        listMultimap.putMany("a", Arrays.asList(1, 2));
        listMultimap.putMany("b", Arrays.asList(3));
        ListMultimap<String, Integer> filtered = listMultimap.filterByValue(list -> list.contains(2));
        assertTrue(filtered instanceof ListMultimap);
        assertEquals(1, filtered.size());
        assertTrue(filtered.containsKey("a"));
    }

    @Test
    public void testFilter_BiPredicate_ListMultimapSpecific() {
        listMultimap.putMany("a", Arrays.asList(1, 2));
        listMultimap.putMany("b", Arrays.asList(3));
        ListMultimap<String, Integer> filtered = listMultimap.filter((k, v) -> k.equals("a") && v.contains(1));
        assertTrue(filtered instanceof ListMultimap);
        assertEquals(1, filtered.size());
        assertTrue(filtered.containsKey("a"));
    }

    // New instance methods in ListMultimap
    @Test
    public void testToImmutableMap_ListMultimap() {
        listMultimap.putMany("a", Arrays.asList(1, 2));
        listMultimap.put("b", 3);

        ImmutableMap<String, ImmutableList<Integer>> immutable = listMultimap.toImmutableMap();
        assertEquals(2, immutable.size());
        assertTrue(immutable.get("a") instanceof ImmutableList);
        assertEquals(ImmutableList.of(1, 2), immutable.get("a"));
        assertEquals(ImmutableList.of(3), immutable.get("b"));

        assertThrows(UnsupportedOperationException.class, () -> immutable.get("a").add(4));
        assertThrows(UnsupportedOperationException.class, () -> immutable.put("c", ImmutableList.of(5)));
    }

    @Test
    public void testToImmutableMap_WithSupplier_ListMultimap() {
        listMultimap.putMany("a", Arrays.asList(1, 2));
        IntFunction<Map<String, ImmutableList<Integer>>> mapSupplier = LinkedHashMap::new;

        ImmutableMap<String, ImmutableList<Integer>> immutable = listMultimap.toImmutableMap(mapSupplier);
        assertEquals(1, immutable.size());
        assertTrue(immutable.get("a") instanceof ImmutableList);
        // Underlying map of ImmutableMap might be LinkedHashMap if wrap preserves it,
        // but the important part is the content and ImmutableList type.
        assertEquals(ImmutableList.of(1, 2), immutable.get("a"));
    }

    // Static factory methods (of)
    @Test
    public void testOfFactories2() {
        SetMultimap<String, Integer> sm1 = SetMultimap.of("a", 1);
        assertEquals(1, sm1.size());
        assertEquals(Collections.singleton(1), sm1.get("a"));

        SetMultimap<String, Integer> sm2 = SetMultimap.of("a", 1, "b", 2);
        assertEquals(2, sm2.size());
        assertEquals(Collections.singleton(1), sm1.get("a"));
        assertEquals(Collections.singleton(2), sm2.get("b"));

        SetMultimap<String, Integer> sm3 = SetMultimap.of("a", 1, "a", 1); // Duplicate value for same key
        assertEquals(1, sm3.size());
        assertEquals(Collections.singleton(1), sm3.get("a")); // Set behavior

        SetMultimap<String, Integer> sm7 = SetMultimap.of("a", 1, "b", 2, "c", 3, "d", 4, "e", 5, "f", 6, "g", 7);
        assertEquals(7, sm7.size());
        assertEquals(Collections.singleton(7), sm7.get("g"));
    }

    // Static factory methods (create)
    @Test
    public void testCreateFromMap2() {
        Map<String, Integer> sourceMap = new HashMap<>();
        sourceMap.put("a", 1);
        sourceMap.put("b", 2);
        sourceMap.put("c", 1); // Different key, same value as "a"
        SetMultimap<String, Integer> sm = SetMultimap.create(sourceMap);
        assertEquals(3, sm.size());
        assertEquals(Collections.singleton(1), sm.get("a"));
        assertEquals(Collections.singleton(2), sm.get("b"));
        assertEquals(Collections.singleton(1), sm.get("c"));

        Map<String, Integer> mapWithDupKeyForCreate = new HashMap<>();
        mapWithDupKeyForCreate.put("x", 10);
        // The create(Map) method in SetMultimap is:
        // multimap.put(map); <- which means it iterates map.entrySet and calls put(key, value)
        // If source map had multiple entries that resolve to same key in multimap, they'd go to same Set
        // But standard Map cannot have duplicate keys. So this aspect is simple.
    }

    @Test
    public void testCreateFromCollectionWithKeyExtractor2() {
        List<String> data = Arrays.asList("apple", "apricot", "banana", "apple"); // "apple" duplicated
        SetMultimap<Character, String> sm = SetMultimap.create(data, s -> s.charAt(0));
        assertEquals(2, sm.size()); // Keys 'a', 'b'
        assertEquals(N.asSet("apple", "apricot"), sm.get('a')); // Set behavior for values
        assertEquals(N.asSet("banana"), sm.get('b'));
    }

    @Test
    public void testCreateFromCollectionWithKeyAndValueExtractors2() {
        List<Pair<String, String>> data = Arrays.asList(Pair.of("fruit", "apple"), Pair.of("fruit", "banana"), Pair.of("vegetable", "carrot"),
                Pair.of("fruit", "apple") // Duplicate pair
        );
        SetMultimap<String, String> sm = SetMultimap.create(data, Pair::left, // type as key
                Pair::right); // name as value
        assertEquals(2, sm.size());
        assertEquals(N.asSet("apple", "banana"), sm.get("fruit"));
        assertEquals(N.asSet("carrot"), sm.get("vegetable"));
    }

    // Static factory methods (concat)
    @Test
    public void testConcatTwoMaps2() {
        Map<String, Integer> mapA = Collections.singletonMap("a", 1);
        Map<String, Integer> mapB = Collections.singletonMap("b", 2);
        SetMultimap<String, Integer> sm = SetMultimap.concat(mapA, mapB);
        assertEquals(2, sm.size());
        assertEquals(Collections.singleton(1), sm.get("a"));
        assertEquals(Collections.singleton(2), sm.get("b"));

        Map<String, Integer> mapC = Collections.singletonMap("a", 1); // Duplicate key and value
        SetMultimap<String, Integer> smDup = SetMultimap.concat(mapA, mapC);
        assertEquals(1, smDup.size());
        assertEquals(Collections.singleton(1), smDup.get("a")); // Set behavior
    }

    // Static factory methods (wrap)
    @Test
    public void testWrapMap2() {
        Map<String, Set<Integer>> sourceMap = new HashMap<>();
        Set<Integer> setA = new HashSet<>(Arrays.asList(1, 2));
        sourceMap.put("a", setA);

        SetMultimap<String, Integer> sm = SetMultimap.wrap(sourceMap);
        assertEquals(N.asSet(1, 2), sm.get("a"));
        assertSame(setA, sm.get("a"), "Wrapped map should share the same value collection instance");

        sm.put("a", 3); // setA becomes {1,2,3}
        assertEquals(N.asSet(1, 2, 3), sourceMap.get("a"));

        sourceMap.get("a").add(4); // setA becomes {1,2,3,4}
        assertEquals(N.asSet(1, 2, 3, 4), sm.get("a"));

        assertThrows(IllegalArgumentException.class, () -> SetMultimap.wrap(null));
        Map<String, Set<Integer>> mapWithNullValue = new HashMap<>();
        mapWithNullValue.put("x", null);
        assertThrows(IllegalArgumentException.class, () -> SetMultimap.wrap(mapWithNullValue));
    }

    // Overridden instance methods
    @Test
    public void testInverse_SetMultimapSpecific() {
        setMultimap.put("k1", 100);
        setMultimap.put("k1", 200); // k1 -> {100, 200}
        setMultimap.put("k2", 100); // k2 -> {100}
        setMultimap.put("k1", 100); // no change for k1

        SetMultimap<Integer, String> inverted = setMultimap.inverse();
        assertEquals(2, inverted.size()); // Keys are 100, 200
        assertEquals(N.asSet("k1", "k2"), inverted.get(100));
        assertEquals(N.asSet("k1"), inverted.get(200));
        assertTrue(inverted.get(100) instanceof Set);
    }

    @Test
    public void testCopy_SetMultimapSpecific() {
        setMultimap.put("a", 1);
        setMultimap.put("a", 1); // Set handles duplicates
        SetMultimap<String, Integer> copy = setMultimap.copy();
        assertNotSame(setMultimap, copy);
        assertEquals(setMultimap, copy);
        assertTrue(copy instanceof SetMultimap);
        assertEquals(Collections.singleton(1), copy.get("a"));

        assertNotSame(setMultimap.get("a"), copy.get("a"));
        assertEquals(setMultimap.get("a"), copy.get("a"));

        copy.get("a").add(2);
        assertEquals(Collections.singleton(1), setMultimap.get("a")); // Original unchanged
    }

    @Test
    public void testFilterByKey_SetMultimapSpecific() {
        setMultimap.put("apple", 1);
        setMultimap.put("banana", 2);
        SetMultimap<String, Integer> filtered = setMultimap.filterByKey(s -> s.startsWith("a"));
        assertTrue(filtered instanceof SetMultimap);
        assertEquals(1, filtered.size());
        assertTrue(filtered.containsKey("apple"));
    }

    @Test
    public void testFilterByValue_SetMultimapSpecific() {
        setMultimap.putMany("a", Arrays.asList(1, 2));
        setMultimap.putMany("b", Arrays.asList(3));
        SetMultimap<String, Integer> filtered = setMultimap.filterByValue(set -> set.contains(2));
        assertTrue(filtered instanceof SetMultimap);
        assertEquals(1, filtered.size());
        assertTrue(filtered.containsKey("a"));
    }

    @Test
    public void testFilter_BiPredicate_SetMultimapSpecific() {
        setMultimap.putMany("a", Arrays.asList(1, 2));
        setMultimap.putMany("b", Arrays.asList(3));
        SetMultimap<String, Integer> filtered = setMultimap.filter((k, v) -> k.equals("a") && v.contains(1));
        assertTrue(filtered instanceof SetMultimap);
        assertEquals(1, filtered.size());
        assertTrue(filtered.containsKey("a"));
    }

    // New instance methods in SetMultimap
    @Test
    public void testToImmutableMap_SetMultimap() {
        setMultimap.putMany("a", Arrays.asList(1, 2, 1)); // a -> {1,2}
        setMultimap.put("b", 3); // b -> {3}

        ImmutableMap<String, ImmutableSet<Integer>> immutable = setMultimap.toImmutableMap();
        assertEquals(2, immutable.size());
        assertTrue(immutable.get("a") instanceof ImmutableSet);
        assertEquals(ImmutableSet.of(1, 2), immutable.get("a"));
        assertEquals(ImmutableSet.of(3), immutable.get("b"));

        assertThrows(UnsupportedOperationException.class, () -> immutable.get("a").add(4));
        assertThrows(UnsupportedOperationException.class, () -> immutable.put("c", ImmutableSet.of(5)));
    }

    @Test
    public void testToImmutableMap_WithSupplier_SetMultimap() {
        setMultimap.putMany("a", Arrays.asList(1, 2));
        IntFunction<Map<String, ImmutableSet<Integer>>> mapSupplier = LinkedHashMap::new;

        ImmutableMap<String, ImmutableSet<Integer>> immutable = setMultimap.toImmutableMap(mapSupplier);
        assertEquals(1, immutable.size());
        assertTrue(immutable.get("a") instanceof ImmutableSet);
        assertEquals(ImmutableSet.of(1, 2), immutable.get("a"));
    }

    // Test a core Multimap method (e.g., put) to ensure Set behavior is correctly handled
    @Test
    public void testPut_SetBehavior() {
        assertTrue(setMultimap.put("key1", 100));
        assertEquals(Collections.singleton(100), setMultimap.get("key1"));

        // Adding the same value again to a Set should not change the set,
        // and underlying Set.add() returns false.
        assertFalse(setMultimap.put("key1", 100));
        assertEquals(Collections.singleton(100), setMultimap.get("key1"));
        assertEquals(1, setMultimap.get("key1").size());

        assertTrue(setMultimap.put("key1", 200));
        assertEquals(N.asSet(100, 200), setMultimap.get("key1"));
    }
}

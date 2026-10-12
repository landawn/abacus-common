package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.BiPredicate;
import java.util.function.Predicate;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

public class MultimapRemoveTest extends MultimapTestSupport {

    private static Map<String, List<Integer>> g053Backing() {
        final Map<String, List<Integer>> backing = new LinkedHashMap<>();
        backing.put("a", new ArrayList<>(Arrays.asList(1, 2, 1, 3)));
        backing.put("b", new ArrayList<>(Arrays.asList(4, 5)));
        backing.put("d", new ArrayList<>(Arrays.asList(6)));
        backing.put("e", new ArrayList<>(Arrays.asList(7)));
        // a no-match key whose value collection rejects removeIf: it must not be touched
        backing.put("u", Collections.unmodifiableList(Arrays.asList(8, 9)));
        return backing;
    }

    private static boolean removeAllSelected(Multimap<String, Integer, ?> values, boolean entryPredicate, Collection<Integer> removal) {
        return entryPredicate ? values.removeValuesIf((key, collection) -> true, removal) : values.removeValuesIf(key -> true, removal);
    }

    private static Multimap<String, Integer, ?> sharedValues(boolean setValues) {
        if (setValues) {
            final Set<Integer> shared = new LinkedHashSet<>(List.of(1, 2));
            final Map<String, Set<Integer>> backing = new LinkedHashMap<>();
            backing.put("a", shared);
            backing.put("b", shared);
            return SetMultimap.wrap(backing);
        }

        final List<Integer> shared = new ArrayList<>(List.of(1, 2));
        final Map<String, List<Integer>> backing = new LinkedHashMap<>();
        backing.put("a", shared);
        backing.put("b", shared);
        return ListMultimap.wrap(backing);
    }

    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    void alreadyEmptiedUnmodifiableViewNeedsOnlyKeyRemoval(boolean entryPredicate) {
        final List<Integer> shared = new ArrayList<>(List.of(1, 2));
        final Map<String, List<Integer>> backing = new LinkedHashMap<>();
        backing.put("a", shared);
        backing.put("b", Collections.unmodifiableList(shared));
        final ListMultimap<String, Integer> values = ListMultimap.wrap(backing);

        assertTrue(removeAllSelected(values, entryPredicate, List.of(1, 2)));

        assertTrue(backing.isEmpty());
        assertTrue(shared.isEmpty());
    }

    /**
     * A stored {@code null} probed against a null-hostile removal collection (such as {@code List.of(..)})
     * must count as "not a member" - that is what the private {@code containsSafely} helper exists for, and
     * what {@code removeValues(Object, Collection)} has always done. The bulk forms - {@code removeValues(Map)}
     * and the shared body behind both {@code removeValuesIf} overloads - called {@code contains} raw instead,
     * so the same data threw {@code NullPointerException} through them.
     */
    @Test
    public void test_bulkRemoval_storedNullAgainstNullHostileCollection_regression_20260918() {
        // Reference behaviour: the single-key form tolerates the stored null.
        final Map<String, List<String>> control = new HashMap<>();
        control.put("k", new ArrayList<>(Arrays.asList((String) null, "a")));
        assertTrue(ListMultimap.wrap(control).removeValues("k", List.of("a")));
        assertEquals(Collections.singletonList(null), control.get("k"));

        // Before the fix this threw NullPointerException.
        final Map<String, List<String>> viaPredicate = new HashMap<>();
        viaPredicate.put("k", new ArrayList<>(Arrays.asList((String) null, "a")));
        assertTrue(ListMultimap.wrap(viaPredicate).removeValuesIf(k -> true, List.of("a")));
        assertEquals(Collections.singletonList(null), viaPredicate.get("k"));

        // Before the fix this threw NullPointerException too.
        final Map<String, List<String>> viaMap = new HashMap<>();
        viaMap.put("k", new ArrayList<>(Arrays.asList((String) null, "a")));
        assertTrue(ListMultimap.wrap(viaMap).removeValues(Collections.singletonMap("k", List.of("a"))));
        assertEquals(Collections.singletonList(null), viaMap.get("k"));
    }

    // clear

    @Test
    public void testClear() {
        Multimap<String, Integer, List<Integer>> mm = getTestMultimap();
        mm.put("a", 1);
        mm.clear();
        assertTrue(mm.isEmpty());
        assertEquals(0, mm.totalValueCount());
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

    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    void doesNotRemoveUnselectedAlias(boolean entryPredicate) {
        final Multimap<String, Integer, ?> values = sharedValues(false);
        final Collection<?> retained = values.get("b");

        assertTrue(entryPredicate ? values.removeValuesIf((key, collection) -> key.equals("a"), List.of(1, 2))
                : values.removeValuesIf(key -> key.equals("a"), List.of(1, 2)));

        assertNull(values.get("a"));
        assertSame(retained, values.get("b"));
        assertTrue(retained.isEmpty());
        assertEquals(Set.of("b"), values.keySet());
    }

    @Test
    public void testEmptyCollectionRemoval() {
        multimap.put("key", 1);
        multimap.removeEntry("key", 1);

        assertFalse(multimap.containsKey("key"));
        assertNull(multimap.get("key"));
    }

    @ParameterizedTest
    @CsvSource({ "false,false", "false,true", "true,false", "true,true" })
    void partialRemovalRetainsSharedCollectionAndBothKeys(boolean setValues, boolean entryPredicate) {
        final Multimap<String, Integer, ?> values = sharedValues(setValues);
        final Collection<?> retained = values.get("a");

        assertTrue(removeAllSelected(values, entryPredicate, List.of(1)));

        assertSame(retained, values.get("a"));
        assertSame(retained, values.get("b"));
        assertEquals(List.of(2), new ArrayList<>(retained));
        assertEquals(2, values.keyCount());
        assertEquals(2, values.totalValueCount());
        assertFalse(removeAllSelected(values, entryPredicate, List.of(1)));
    }

    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    void preservesPreexistingEmptyKeysWithNoMatchingValues(boolean entryPredicate) {
        final Map<String, List<Integer>> backing = new LinkedHashMap<>();
        final ListMultimap<String, Integer> values = ListMultimap.wrap(backing);
        final List<Integer> empty = Collections.emptyList();
        backing.put("empty", empty);
        values.put("present", 9);

        assertFalse(removeAllSelected(values, entryPredicate, List.of(1, 2)));

        assertSame(empty, values.get("empty"));
        assertEquals(List.of(9), values.get("present"));
        assertEquals(2, values.keyCount());
    }

    // removeAll

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
    public void testRemoveAll_key() {
        Multimap<String, Integer, List<Integer>> mm = getTestMultimap();
        mm.putValues("a", Arrays.asList(1, 2));
        mm.put("b", 3);

        Collection<Integer> removedA = mm.removeAll("a");
        assertNotNull(removedA);
        assertEquals(2, removedA.size());
        assertTrue(removedA.containsAll(Arrays.asList(1, 2)));
        assertFalse(mm.containsKey("a"));
        assertTrue(mm.containsKey("b"));

        assertNull(mm.removeAll("c"));
    }

    @Test
    public void testRemoveAll_NonExistentKey() {
        assertNull(listMultimap.removeAll("key1"));
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
    public void testRemoveAllIf() {
        listMultimap.put("remove_key1", 10);
        listMultimap.put("remove_key2", 20);
        listMultimap.put("keep_key", 30);

        Predicate<String> keyFilter = key -> key.startsWith("remove_");
        assertTrue(listMultimap.removeKeysIf(keyFilter));

        assertFalse(listMultimap.containsKey("remove_key1"));
        assertFalse(listMultimap.containsKey("remove_key2"));
        assertTrue(listMultimap.containsKey("keep_key"));

        assertFalse(listMultimap.removeKeysIf(key -> false));
    }

    @Test
    public void testRemoveAllIf_biPredicate() {
        Multimap<String, Integer, List<Integer>> mm = getTestMultimap();
        mm.putValues("a", Arrays.asList(1, 2));
        mm.putValues("b", Arrays.asList(3, 4, 5));
        mm.put("c", 5);

        assertTrue(mm.removeKeysIf((key, values) -> key.equals("a") || values.stream().mapToInt(i -> i).sum() > 10));
        assertFalse(mm.containsKey("a"));
        assertFalse(mm.containsKey("b"));
        assertTrue(mm.containsKey("c"));

        assertFalse(mm.removeKeysIf((k, v) -> k.equals("non_existent")));
    }

    @Test
    public void testRemoveAllIf_keyPredicate() {
        Multimap<String, Integer, List<Integer>> mm = getTestMultimap();
        mm.putValues("apple", Arrays.asList(1, 2));
        mm.putValues("apricot", Arrays.asList(3, 4));
        mm.put("banana", 5);

        assertTrue(mm.removeKeysIf(key -> key.startsWith("ap")));
        assertFalse(mm.containsKey("apple"));
        assertFalse(mm.containsKey("apricot"));
        assertTrue(mm.containsKey("banana"));

        assertFalse(mm.removeKeysIf(key -> key.startsWith("xyz")));
    }

    @Test
    public void testRemoveAllIfBiPredicate() {
        listMultimap.put("key1", 10);
        listMultimap.put("key1", 20);
        listMultimap.put("key2", 30);

        BiPredicate<String, Collection<Integer>> filter = (key, values) -> values.size() > 1;
        assertTrue(listMultimap.removeKeysIf(filter));

        assertFalse(listMultimap.containsKey("key1"));
        assertTrue(listMultimap.containsKey("key2"));

        assertFalse(listMultimap.removeKeysIf((key, values) -> false));
    }

    @Test
    public void testRemoveAllIfBiPredicate_Matching() {
        listMultimap.putValues("key1", Arrays.asList(10, 20));
        listMultimap.putValues("key2", Arrays.asList(10, 20, 30));
        listMultimap.putValues("key3", Arrays.asList(10));

        assertTrue(listMultimap.removeKeysIf((k, v) -> v.size() > 2));
        assertNotNull(listMultimap.get("key1"));
        assertNull(listMultimap.get("key2"));
        assertNotNull(listMultimap.get("key3"));
    }

    @Test
    public void testRemoveAllIfBiPredicate_NoMatching() {
        listMultimap.put("key1", 10);
        assertFalse(listMultimap.removeKeysIf((k, v) -> v.size() > 5));
    }

    @Test
    public void testRemoveAllIfPredicate_Matching() {
        listMultimap.put("key1", 10);
        listMultimap.put("key2", 20);
        listMultimap.put("other", 30);

        assertTrue(listMultimap.removeKeysIf(key -> key.startsWith("key")));
        assertNull(listMultimap.get("key1"));
        assertNull(listMultimap.get("key2"));
        assertNotNull(listMultimap.get("other"));
    }

    @Test
    public void testRemoveAllIfPredicate_NoMatching() {
        listMultimap.put("key1", 10);
        assertFalse(listMultimap.removeKeysIf(key -> key.equals("key2")));
    }

    @Test
    public void testRemoveEntries() {
        listMultimap.put("a", 1);
        listMultimap.put("a", 2);
        listMultimap.put("b", 3);

        Map<String, Integer> toRemove = new HashMap<>();
        toRemove.put("a", 1);
        toRemove.put("b", 3);

        assertTrue(listMultimap.removeEntries(toRemove));
        assertEquals(1, listMultimap.get("a").size());
        assertNull(listMultimap.get("b"));

        assertFalse(listMultimap.removeEntries(new HashMap<>()));
    }

    @Test
    public void testRemoveEntries_NullMap() {
        Map<String, Integer> nullMap = null;
        assertFalse(listMultimap.removeEntries(nullMap));
    }

    // removeEntriesIf

    @Test
    public void testRemoveEntriesIf() {
        listMultimap.put("a", 10);
        listMultimap.put("b", 10);
        listMultimap.put("c", 20);

        assertTrue(listMultimap.removeEntriesIf(key -> key.compareTo("c") < 0, 10));
        assertNull(listMultimap.get("a"));
        assertNull(listMultimap.get("b"));
        assertNotNull(listMultimap.get("c"));
    }

    @Test
    public void testRemoveEntriesIf_BiPredicate() {
        listMultimap.putValues("a", Arrays.asList(1, 2, 3));
        listMultimap.putValues("b", Arrays.asList(1));

        assertTrue(listMultimap.removeEntriesIf((k, v) -> v.size() > 2, 1));
        assertEquals(2, listMultimap.get("a").size());
        assertFalse(listMultimap.get("a").contains(1));
        assertEquals(1, listMultimap.get("b").size());
    }

    @Test
    public void testRemoveEntriesIf_BiPredicate_EmptyMultimap() {
        assertFalse(listMultimap.removeEntriesIf((BiPredicate<? super String, ? super List<Integer>>) (k, v) -> true, 1));
    }

    @Test
    public void testRemoveEntriesIf_KeyPredicate_EmptyMultimap() {
        assertFalse(listMultimap.removeEntriesIf(k -> true, 1));
    }

    @Test
    public void testRemoveEntriesIf_NoMatch() {
        listMultimap.put("a", 10);
        assertFalse(listMultimap.removeEntriesIf(key -> key.equals("z"), 10));
        assertEquals(1, listMultimap.get("a").size());
    }

    @Test
    public void testRemoveEntry() {
        listMultimap.put("key", 1);
        listMultimap.put("key", 2);
        listMultimap.put("key", 1);

        assertTrue(listMultimap.removeEntry("key", 1));
        assertEquals(2, listMultimap.get("key").size());
        assertEquals(Integer.valueOf(2), listMultimap.get("key").get(0));

        assertFalse(listMultimap.removeEntry("nonexistent", 1));
    }

    @Test
    public void testRemoveEntry_LastValueRemovesKey() {
        listMultimap.put("key", 1);
        assertTrue(listMultimap.removeEntry("key", 1));
        assertNull(listMultimap.get("key"));
        assertFalse(listMultimap.containsKey("key"));
    }

    // removeKeysIf

    @Test
    public void testRemoveKeysIf() {
        listMultimap.put("abc", 1);
        listMultimap.put("def", 2);
        listMultimap.put("abx", 3);

        assertTrue(listMultimap.removeKeysIf(key -> key.startsWith("ab")));
        assertNull(listMultimap.get("abc"));
        assertNull(listMultimap.get("abx"));
        assertNotNull(listMultimap.get("def"));
    }

    @Test
    public void testRemoveKeysIf_AllKeysRemoved() {
        listMultimap.put("a", 1);
        listMultimap.put("b", 2);
        assertTrue(listMultimap.removeKeysIf(k -> true));
        assertTrue(listMultimap.isEmpty());
    }

    @Test
    public void testRemoveKeysIf_BiPredicate() {
        listMultimap.putValues("a", Arrays.asList(1, 2, 3));
        listMultimap.putValues("b", Arrays.asList(4));

        assertTrue(listMultimap.removeKeysIf((k, v) -> v.size() < 2));
        assertNotNull(listMultimap.get("a"));
        assertNull(listMultimap.get("b"));
    }

    @Test
    public void testRemoveKeysIf_BiPredicate_AllKeysRemoved() {
        listMultimap.put("a", 1);
        listMultimap.put("b", 2);
        assertTrue(listMultimap.removeKeysIf((BiPredicate<? super String, ? super List<Integer>>) (k, v) -> true));
        assertTrue(listMultimap.isEmpty());
    }

    @Test
    public void testRemoveKeysIf_BiPredicate_EmptyMultimap() {
        assertFalse(listMultimap.removeKeysIf((BiPredicate<? super String, ? super List<Integer>>) (k, v) -> true));
    }

    @Test
    public void testRemoveKeysIf_EmptyMultimap() {
        assertFalse(listMultimap.removeKeysIf(k -> true));
    }

    @Test
    public void testRemoveMany() {
        listMultimap.put("key1", 10);
        listMultimap.put("key1", 20);
        listMultimap.put("key1", 30);

        Collection<Integer> toRemove = Arrays.asList(10, 20);
        assertTrue(listMultimap.removeValues("key1", toRemove));
        assertFalse(listMultimap.containsEntry("key1", 10));
        assertFalse(listMultimap.containsEntry("key1", 20));
        assertTrue(listMultimap.containsEntry("key1", 30));

        assertFalse(listMultimap.removeValues("key1", Collections.emptyList()));
        assertFalse(listMultimap.removeValues("nonexistent", Arrays.asList(1, 2)));
        assertFalse(listMultimap.removeValues("key1", null));
    }

    @Test
    public void testRemoveMany_AllValuesRemovesKey() {
        listMultimap.put("key1", 10);
        listMultimap.put("key1", 20);
        assertTrue(listMultimap.removeValues("key1", Arrays.asList(10, 20)));
        assertNull(listMultimap.get("key1"));
    }

    @Test
    public void testRemoveMany_EmptyCollection() {
        assertFalse(listMultimap.removeValues("key1", new ArrayList<>()));
    }

    @Test
    public void testRemoveMany_keyCollection() {
        Multimap<String, Integer, List<Integer>> mm = getTestMultimap();
        mm.putValues("a", Arrays.asList(1, 2, 3, 1, 4));

        assertTrue(mm.removeValues("a", Arrays.asList(1, 3, 99)));
        assertEquals(2, mm.get("a").size());
        assertFalse(mm.get("a").contains(1));
        assertFalse(mm.get("a").contains(3));

        mm.put("b", 10);
        mm.put("b", 20);
        assertTrue(mm.removeValues("b", Arrays.asList(20)));
        assertTrue(mm.containsKey("b"));

        assertFalse(mm.removeValues("a", Collections.emptyList()));
        assertFalse(mm.removeValues("non_existent_key", Arrays.asList(1)));

        mm.clear();
        mm.put("x", 1);
        mm.put("x", 2);
        assertFalse(mm.removeValues("x", Arrays.asList(3, 4)));
        assertEquals(2, mm.get("x").size());
    }

    @Test
    public void testRemoveMany_map() {
        Multimap<String, Integer, List<Integer>> mm = getTestMultimap();
        mm.putValues("a", Arrays.asList(1, 2, 3, 1));
        mm.putValues("b", Arrays.asList(4, 5));

        Map<String, Collection<Integer>> toRemove = new HashMap<>();
        toRemove.put("a", Arrays.asList(1, 3));
        toRemove.put("b", Arrays.asList(4, 5, 6));
        toRemove.put("c", Arrays.asList(7));

        assertTrue(mm.removeValues(toRemove));
        assertEquals(1, mm.get("a").size());
        assertTrue(mm.get("a").contains(2));
        assertFalse(mm.containsKey("b"));

        assertFalse(mm.removeValues(Collections.emptyMap()));
    }

    @Test
    public void testRemoveMany_multimap() {
        Multimap<String, Integer, List<Integer>> mm = getTestMultimap();
        mm.putValues("a", Arrays.asList(1, 2, 3, 1));
        mm.putValues("b", Arrays.asList(4, 5));

        ListMultimap<String, Integer> otherMm = CommonUtil.newListMultimap();
        otherMm.putValues("a", Arrays.asList(1, 3));
        otherMm.putValues("b", Arrays.asList(4, 5, 6));
        otherMm.put("c", 7);

        assertTrue(mm.removeValues(otherMm));
        assertEquals(1, mm.get("a").size());
        assertTrue(mm.get("a").contains(2));
        assertFalse(mm.containsKey("b"));

        assertFalse(mm.removeValues(CommonUtil.newListMultimap()));
    }

    @Test
    public void testRemoveMany_NonExistentKey() {
        assertFalse(listMultimap.removeValues("key1", Arrays.asList(10, 20)));
    }

    @Test
    public void testRemoveMany_NullValues() {
        listMultimap.put("key1", null);
        listMultimap.put("key1", 10);
        listMultimap.put("key1", 20);
        List<Integer> toRemove = new ArrayList<>();
        toRemove.add(null);
        toRemove.add(10);
        assertTrue(listMultimap.removeValues("key1", toRemove));
        assertEquals(1, listMultimap.get("key1").size());
        assertTrue(listMultimap.get("key1").contains(20));
    }

    @Test
    public void testRemoveMany_PartialRemoval() {
        listMultimap.putValues("key1", Arrays.asList(10, 20, 30, 40));
        assertTrue(listMultimap.removeValues("key1", Arrays.asList(15, 20, 35, 40)));
        assertEquals(2, listMultimap.get("key1").size());
        assertTrue(listMultimap.get("key1").contains(10));
        assertTrue(listMultimap.get("key1").contains(30));
    }

    @Test
    public void testRemoveMany_WithValues() {
        listMultimap.put("key1", 10);
        listMultimap.put("key1", 20);
        listMultimap.put("key1", 30);
        listMultimap.put("key1", 40);

        assertTrue(listMultimap.removeValues("key1", Arrays.asList(10, 30)));
        assertEquals(2, listMultimap.get("key1").size());
        assertTrue(listMultimap.get("key1").contains(20));
        assertTrue(listMultimap.get("key1").contains(40));
    }

    @Test
    public void testRemoveManyIf() {
        listMultimap.put("key1", 10);
        listMultimap.put("key1", 20);
        listMultimap.put("key2", 10);
        listMultimap.put("key3", 30);

        Collection<Integer> valuesToRemove = Arrays.asList(10, 20);
        Predicate<String> keyFilter = key -> key.startsWith("key");

        assertTrue(listMultimap.removeValuesIf(keyFilter, valuesToRemove));
        assertFalse(listMultimap.containsEntry("key1", 10));
        assertFalse(listMultimap.containsEntry("key1", 20));
        assertFalse(listMultimap.containsEntry("key2", 10));
        assertTrue(listMultimap.containsEntry("key3", 30));

        assertFalse(listMultimap.removeValuesIf(keyFilter, Collections.emptyList()));
        assertFalse(listMultimap.removeValuesIf(key -> false, valuesToRemove));
    }

    @Test
    public void testRemoveManyIf_values_biPredicate() {
        Multimap<String, Integer, List<Integer>> mm = getTestMultimap();
        mm.putValues("a", Arrays.asList(1, 2, 1, 5));
        mm.putValues("b", Arrays.asList(1, 3, 5));
        mm.putValues("c", Arrays.asList(1, 4, 5));
        Collection<Integer> valuesToRemove = Arrays.asList(1, 5);

        assertTrue(mm.removeValuesIf((key, values) -> key.equals("a") || values.contains(3), valuesToRemove));
        assertEquals(Arrays.asList(2), mm.get("a"));
        assertEquals(Arrays.asList(3), mm.get("b"));
        assertEquals(Arrays.asList(1, 4, 5), mm.get("c"));
    }

    @Test
    public void testRemoveManyIf_values_keyPredicate() {
        Multimap<String, Integer, List<Integer>> mm = getTestMultimap();
        mm.putValues("apple", Arrays.asList(1, 2, 1, 5));
        mm.putValues("apricot", Arrays.asList(1, 3, 5));
        mm.putValues("banana", Arrays.asList(1, 4, 5));
        Collection<Integer> valuesToRemove = Arrays.asList(1, 5, 99);

        assertTrue(mm.removeValuesIf(key -> key.startsWith("ap"), valuesToRemove));
        assertEquals(Arrays.asList(2), mm.get("apple"));
        assertEquals(Arrays.asList(3), mm.get("apricot"));
        assertEquals(Arrays.asList(1, 4, 5), mm.get("banana"));

        assertFalse(mm.removeValuesIf(key -> true, Collections.emptyList()));
        assertFalse(mm.removeValuesIf(key -> true, Arrays.asList(100)));
    }

    @Test
    public void testRemoveManyIfBiPredicate() {
        listMultimap.put("key1", 10);
        listMultimap.put("key1", 20);
        listMultimap.put("key2", 30);

        Collection<Integer> valuesToRemove = Arrays.asList(10, 30);
        BiPredicate<String, Collection<Integer>> filter = (key, values) -> values.size() >= 1;

        assertTrue(listMultimap.removeValuesIf(filter, valuesToRemove));
        assertFalse(listMultimap.containsEntry("key1", 10));
        assertTrue(listMultimap.containsEntry("key1", 20));
        assertFalse(listMultimap.containsEntry("key2", 30));

        assertFalse(listMultimap.removeValuesIf(filter, Collections.emptyList()));
    }

    @Test
    public void testRemoveManyIfBiPredicate_EmptyCollection() {
        assertFalse(listMultimap.removeValuesIf((k, v) -> true, new ArrayList<>()));
    }

    @Test
    public void testRemoveManyIfBiPredicate_Matching() {
        listMultimap.putValues("key1", Arrays.asList(10, 20, 30, 40));
        listMultimap.putValues("key2", Arrays.asList(10, 20));

        assertTrue(listMultimap.removeValuesIf((k, v) -> v.size() > 2, Arrays.asList(10, 20)));
        assertEquals(2, listMultimap.get("key1").size());
        assertEquals(2, listMultimap.get("key2").size());
    }

    @Test
    public void testRemoveManyIfKeyPredicate_EmptyCollection() {
        assertFalse(listMultimap.removeValuesIf(key -> true, new ArrayList<>()));
    }

    @Test
    public void testRemoveManyIfKeyPredicate_Matching() {
        listMultimap.putValues("key1", Arrays.asList(10, 20, 30));
        listMultimap.putValues("key2", Arrays.asList(10, 20, 40));

        assertTrue(listMultimap.removeValuesIf(key -> key.equals("key1"), Arrays.asList(10, 20)));
        assertEquals(1, listMultimap.get("key1").size());
        assertEquals(3, listMultimap.get("key2").size());
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

        assertTrue(listMultimap.removeValues(toRemove));
        assertFalse(listMultimap.containsEntry("key1", 10));
        assertTrue(listMultimap.containsEntry("key1", 20));
        assertFalse(listMultimap.containsKey("key2"));

        assertFalse(listMultimap.removeValues(Collections.emptyMap()));
    }

    @Test
    public void testRemoveManyMap_EmptyMap() {
        assertFalse(listMultimap.removeValues(new HashMap<>()));
    }

    @Test
    public void testRemoveManyMap_PartialMatch() {
        listMultimap.putValues("key1", Arrays.asList(10, 20, 30));
        listMultimap.putValues("key2", Arrays.asList(40));
        Map<String, Collection<Integer>> toRemove = new HashMap<>();
        toRemove.put("key1", Arrays.asList(10, 20));
        toRemove.put("key3", Arrays.asList(50));
        assertTrue(listMultimap.removeValues(toRemove));
        assertEquals(1, listMultimap.get("key1").size());
    }

    @Test
    public void testRemoveManyMap_WithValues() {
        listMultimap.putValues("key1", Arrays.asList(10, 20, 30));
        listMultimap.putValues("key2", Arrays.asList(40, 50, 60));

        Map<String, Collection<Integer>> toRemove = new HashMap<>();
        toRemove.put("key1", Arrays.asList(10, 20));
        toRemove.put("key2", Arrays.asList(50));

        assertTrue(listMultimap.removeValues(toRemove));
        assertEquals(1, listMultimap.get("key1").size());
        assertEquals(2, listMultimap.get("key2").size());
    }

    @Test
    public void testRemoveManyMultimap() {
        listMultimap.put("key1", 10);
        listMultimap.put("key1", 20);
        listMultimap.put("key2", 30);

        ListMultimap<String, Integer> toRemove = CommonUtil.newListMultimap();
        toRemove.put("key1", 10);
        toRemove.put("key2", 30);

        assertTrue(listMultimap.removeValues(toRemove));
        assertFalse(listMultimap.containsEntry("key1", 10));
        assertTrue(listMultimap.containsEntry("key1", 20));
        assertFalse(listMultimap.containsKey("key2"));

        ListMultimap<String, Integer> empty = CommonUtil.newListMultimap();
        assertFalse(listMultimap.removeValues(empty));
    }

    @Test
    public void testRemoveManyMultimap_EmptyMultimap() {
        ListMultimap<String, Integer> other = CommonUtil.newListMultimap();
        assertFalse(listMultimap.removeValues(other));
    }

    @Test
    public void testRemoveManyMultimap_WithValues() {
        listMultimap.putValues("key1", Arrays.asList(10, 20, 30));
        listMultimap.putValues("key2", Arrays.asList(40, 50));

        ListMultimap<String, Integer> toRemove = CommonUtil.newListMultimap();
        toRemove.put("key1", 10);
        toRemove.put("key1", 20);
        toRemove.put("key2", 40);

        assertTrue(listMultimap.removeValues(toRemove));
        assertEquals(1, listMultimap.get("key1").size());
        assertEquals(1, listMultimap.get("key2").size());
    }

    @Test
    public void testRemoveManySelfClearsWithoutConcurrentModification() {
        Multimap<String, Integer, List<Integer>> mm = getTestMultimap();
        mm.putValues("a", Arrays.asList(1, 2));
        mm.putValues("b", Arrays.asList(3, 4));

        assertTrue(mm.removeValues(mm));
        assertTrue(mm.isEmpty());
    }

    @Test
    public void testRemoveManySelfFollowsTheDocumentedDelegation() {
        // removeValues(Multimap) delegates to removeValues(Map) for any other multimap and answers m == this
        // directly. Passing this instance used to short-circuit to clear(), which reported true even when nothing
        // was removed and dropped a key that was already mapped to an empty value collection - a state a retained
        // wrapped backing map can produce. Both routes must now give the same answer for such a key.
        final Map<String, List<Integer>> backing = new HashMap<>();
        final ListMultimap<String, Integer> orphanOnly = ListMultimap.wrap(backing);
        backing.put("z", new ArrayList<>());

        assertFalse(orphanOnly.removeValues(orphanOnly));
        assertTrue(backing.containsKey("z"));
        assertEquals(Collections.emptyList(), backing.get("z"));

        // every sibling agrees on that state
        assertFalse(orphanOnly.removeValues(backing));
        assertFalse(orphanOnly.removeValuesIf(key -> true, Arrays.asList(1)));
        assertTrue(backing.containsKey("z"));

        // a key that does hold values is still emptied and dropped
        final Map<String, List<Integer>> mixed = new HashMap<>();
        final ListMultimap<String, Integer> mm = ListMultimap.wrap(mixed);
        mm.putValues("a", Arrays.asList(1, 2));
        mixed.put("z", new ArrayList<>());
        assertTrue(mm.removeValues(mm));
        assertFalse(mixed.containsKey("a"));
        assertTrue(mixed.containsKey("z"));
    }

    @Test
    public void testRemoveOne() {
        listMultimap.put("key1", 10);
        listMultimap.put("key1", 20);
        listMultimap.put("key2", 30);

        assertTrue(listMultimap.removeEntry("key1", 10));
        assertFalse(listMultimap.containsEntry("key1", 10));
        assertTrue(listMultimap.containsEntry("key1", 20));

        assertFalse(listMultimap.removeEntry("key1", 99));
        assertFalse(listMultimap.removeEntry("nonexistent", 10));

        assertTrue(listMultimap.removeEntry("key2", 30));
        assertFalse(listMultimap.containsKey("key2"));
    }

    @Test
    public void testRemoveOne_ExistingValue() {
        listMultimap.put("key1", 10);
        listMultimap.put("key1", 20);
        assertTrue(listMultimap.removeEntry("key1", 10));
        assertEquals(1, listMultimap.get("key1").size());
        assertFalse(listMultimap.get("key1").contains(10));
    }

    @Test
    public void testRemoveOne_keyValue() {
        Multimap<String, Integer, List<Integer>> mm = getTestMultimap();
        mm.putValues("a", Arrays.asList(1, 2, 1, 3));
        assertTrue(mm.removeEntry("a", 1));
        assertEquals(3, mm.get("a").size());
        assertTrue(mm.get("a").contains(1));

        assertTrue(mm.removeEntry("a", 1));
        assertEquals(2, mm.get("a").size());
        assertFalse(mm.get("a").contains(1));

        assertFalse(mm.removeEntry("a", 99));
        assertFalse(mm.removeEntry("b", 1));

        mm.put("c", 10);
        assertTrue(mm.removeEntry("c", 10));
        assertNull(mm.get("c"));
        assertFalse(mm.containsKey("c"));
    }

    @Test
    public void testRemoveOne_LastValueRemovesKey() {
        listMultimap.put("key1", 10);
        assertTrue(listMultimap.removeEntry("key1", 10));
        assertNull(listMultimap.get("key1"));
    }

    @Test
    public void testRemoveOne_map() {
        Multimap<String, Integer, List<Integer>> mm = getTestMultimap();
        mm.putValues("a", Arrays.asList(1, 2, 1));
        mm.put("b", 3);

        Map<String, Integer> toRemove = new HashMap<>();
        toRemove.put("a", 1);
        toRemove.put("b", 3);
        toRemove.put("c", 99);

        assertTrue(mm.removeEntries(toRemove));
        assertEquals(2, mm.get("a").size());
        assertTrue(mm.get("a").contains(2) && mm.get("a").contains(1));
        assertFalse(mm.containsKey("b"));

        assertFalse(mm.removeEntries(Collections.emptyMap()));

        mm.clear();
        mm.put("x", 10);
        Map<String, Integer> toRemoveNonExistentVal = CommonUtil.asMap("x", 99);
        assertFalse(mm.removeEntries(toRemoveNonExistentVal));
        assertTrue(mm.containsEntry("x", 10));
    }

    @Test
    public void testRemoveOne_NonExistentKey() {
        assertFalse(listMultimap.removeEntry("key1", 10));
    }

    @Test
    public void testRemoveOne_NonExistentValue() {
        listMultimap.put("key1", 10);
        assertFalse(listMultimap.removeEntry("key1", 20));
    }

    @Test
    public void testRemoveOne_NullKey() {
        listMultimap.put(null, 10);
        assertTrue(listMultimap.removeEntry(null, 10));
        assertNull(listMultimap.get(null));
    }

    @Test
    public void testRemoveOne_OnlyFirstOccurrence() {
        listMultimap.put("key1", 10);
        listMultimap.put("key1", 10);
        listMultimap.put("key1", 10);
        assertTrue(listMultimap.removeEntry("key1", 10));
        assertEquals(2, listMultimap.get("key1").size());
    }

    @Test
    public void testRemoveOneIf() {
        listMultimap.put("prefix_key1", 10);
        listMultimap.put("prefix_key2", 10);
        listMultimap.put("other_key", 10);

        Predicate<String> keyFilter = key -> key.startsWith("prefix_");
        assertTrue(listMultimap.removeEntriesIf(keyFilter, 10));

        int countAfter = 0;
        if (listMultimap.containsEntry("prefix_key1", 10)) {
            countAfter++;
        }
        if (listMultimap.containsEntry("prefix_key2", 10)) {
            countAfter++;
        }
        assertTrue(countAfter < 2);
        assertTrue(listMultimap.containsEntry("other_key", 10));

        assertFalse(listMultimap.removeEntriesIf(keyFilter, 99));
    }

    @Test
    public void testRemoveOneIf_value_biPredicate() {
        Multimap<String, Integer, List<Integer>> mm = getTestMultimap();
        mm.putValues("a", Arrays.asList(1, 2, 1));
        mm.putValues("b", Arrays.asList(1, 3));
        mm.putValues("c", Arrays.asList(2, 4));

        assertTrue(mm.removeEntriesIf((key, values) -> key.equals("a") || values.contains(3), 1));
        assertEquals(Arrays.asList(2, 1), mm.get("a"));
        assertEquals(Arrays.asList(3), mm.get("b"));
        assertEquals(Arrays.asList(2, 4), mm.get("c"));

        assertFalse(mm.removeEntriesIf((k, v) -> k.equals("non_existent"), 1));
    }

    @Test
    public void testRemoveOneIf_value_keyPredicate() {
        Multimap<String, Integer, List<Integer>> mm = getTestMultimap();
        mm.putValues("apple", Arrays.asList(1, 2, 1));
        mm.putValues("apricot", Arrays.asList(1, 3));
        mm.putValues("banana", Arrays.asList(1, 4));

        assertTrue(mm.removeEntriesIf(key -> key.startsWith("ap"), 1));
        assertEquals(Arrays.asList(2, 1), mm.get("apple"));
        assertEquals(Arrays.asList(3), mm.get("apricot"));
        assertFalse(mm.get("apricot").contains(1));
        assertEquals(Arrays.asList(1, 4), mm.get("banana"));

        assertTrue(mm.removeEntriesIf(key -> key.equals("apple"), 1));
        assertEquals(Arrays.asList(2), mm.get("apple"));

        assertFalse(mm.removeEntriesIf(key -> true, 99));
    }

    @Test
    public void testRemoveOneIfBiPredicate() {
        listMultimap.put("key1", 10);
        listMultimap.put("key1", 20);
        listMultimap.put("key2", 10);

        BiPredicate<String, Collection<Integer>> filter = (key, values) -> key.equals("key1") && values.size() > 1;

        assertTrue(listMultimap.removeEntriesIf(filter, 10));
        assertFalse(listMultimap.containsEntry("key1", 10));
        assertTrue(listMultimap.containsEntry("key1", 20));
        assertTrue(listMultimap.containsEntry("key2", 10));

        assertFalse(listMultimap.removeEntriesIf(filter, 99));
    }

    @Test
    public void testRemoveOneIfBiPredicate_Matching() {
        listMultimap.putValues("key1", Arrays.asList(10, 20));
        listMultimap.putValues("key2", Arrays.asList(10, 20, 30));

        assertTrue(listMultimap.removeEntriesIf((k, v) -> v.size() > 2, 10));
        assertEquals(2, listMultimap.get("key1").size());
        assertEquals(2, listMultimap.get("key2").size());
    }

    @Test
    public void testRemoveOneIfBiPredicate_NoMatching() {
        listMultimap.put("key1", 10);
        assertFalse(listMultimap.removeEntriesIf((k, v) -> v.size() > 5, 10));
    }

    @Test
    public void testRemoveOneIfKeyPredicate_MatchingKeys() {
        listMultimap.put("key1", 10);
        listMultimap.put("key2", 10);
        listMultimap.put("key3", 20);

        assertTrue(listMultimap.removeEntriesIf(key -> key.startsWith("key"), 10));
        assertNull(listMultimap.get("key1"));
        assertNull(listMultimap.get("key2"));
        assertNotNull(listMultimap.get("key3"));
    }

    @Test
    public void testRemoveOneIfKeyPredicate_NoMatchingKeys() {
        listMultimap.put("key1", 10);
        assertFalse(listMultimap.removeEntriesIf(key -> key.equals("key2"), 10));
    }

    @Test
    public void testRemoveOneMap() {
        listMultimap.put("key1", 10);
        listMultimap.put("key1", 20);
        listMultimap.put("key2", 30);

        Map<String, Integer> toRemove = new HashMap<>();
        toRemove.put("key1", 10);
        toRemove.put("key2", 30);

        assertTrue(listMultimap.removeEntries(toRemove));
        assertFalse(listMultimap.containsEntry("key1", 10));
        assertTrue(listMultimap.containsEntry("key1", 20));
        assertFalse(listMultimap.containsKey("key2"));

        assertFalse(listMultimap.removeEntries(Collections.emptyMap()));
        assertFalse(listMultimap.removeEntries((Map<String, Integer>) null));
    }

    @Test
    public void testRemoveOneMap_EmptyMap() {
        assertFalse(listMultimap.removeEntries(new HashMap<>()));
    }

    @Test
    public void testRemoveOneMap_PartialMatch() {
        listMultimap.put("key1", 10);
        listMultimap.put("key1", 20);
        listMultimap.put("key2", 30);
        Map<String, Integer> toRemove = new HashMap<>();
        toRemove.put("key1", 10);
        toRemove.put("key3", 40);
        assertTrue(listMultimap.removeEntries(toRemove));
        assertEquals(1, listMultimap.get("key1").size());
    }

    @Test
    public void testRemoveOneMap_WithValues() {
        listMultimap.put("key1", 10);
        listMultimap.put("key1", 20);
        listMultimap.put("key2", 30);

        Map<String, Integer> toRemove = new HashMap<>();
        toRemove.put("key1", 10);
        toRemove.put("key2", 30);

        assertTrue(listMultimap.removeEntries(toRemove));
        assertEquals(1, listMultimap.get("key1").size());
        assertNull(listMultimap.get("key2"));
    }

    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    void removesDistinctIdentityKeysEvenWhenTheyAreEqual(boolean entryPredicate) {
        final List<Integer> shared = new ArrayList<>(List.of(1, 2));
        final Map<String, List<Integer>> backing = new IdentityHashMap<>();
        final String first = new String("same");
        final String second = new String("same");
        backing.put(first, shared);
        backing.put(second, shared);
        final ListMultimap<String, Integer> values = ListMultimap.wrap(backing);

        assertTrue(removeAllSelected(values, entryPredicate, List.of(1, 2)));

        assertTrue(backing.isEmpty());
        assertFalse(values.containsKey(first));
        assertFalse(values.containsKey(second));
        assertTrue(shared.isEmpty());
    }

    @ParameterizedTest
    @CsvSource({ "false,false", "false,true", "true,false", "true,true" })
    void removesEverySelectedKeyWhoseSharedCollectionBecomesEmpty(boolean setValues, boolean entryPredicate) {
        final Multimap<String, Integer, ?> values = sharedValues(setValues);
        final Collection<?> retained = values.get("a");
        assertSame(retained, values.get("b"));

        assertTrue(removeAllSelected(values, entryPredicate, List.of(1, 2)));

        assertTrue(retained.isEmpty());
        assertNull(values.get("a"));
        assertNull(values.get("b"));
        assertEquals(0, values.keyCount());
        assertEquals(0, values.totalValueCount());
        assertTrue(values.isEmpty());
    }

    // removeValues

    @Test
    public void testRemoveValues() {
        listMultimap.putValues("key", Arrays.asList(1, 2, 3, 4, 5));
        assertTrue(listMultimap.removeValues("key", Arrays.asList(2, 4)));
        assertEquals(3, listMultimap.get("key").size());
        assertFalse(listMultimap.get("key").contains(2));
        assertFalse(listMultimap.get("key").contains(4));
    }

    @Test
    public void testRemoveValues_acceptsAliasedWrappedMultimap() {
        final Map<String, List<Integer>> backing = new LinkedHashMap<>();
        backing.put("a", new ArrayList<>(Arrays.asList(1, 2)));
        backing.put("b", new ArrayList<>(Collections.singletonList(3)));
        final ListMultimap<String, Integer> target = ListMultimap.wrap(backing);
        final ListMultimap<String, Integer> aliasedRemovals = ListMultimap.wrap(backing);

        assertTrue(target.removeValues(aliasedRemovals));

        assertTrue(target.isEmpty());
        assertTrue(aliasedRemovals.isEmpty());
        assertTrue(backing.isEmpty());
    }

    @Test
    public void testRemoveValues_acceptsWrappedBackingMap() {
        final Map<String, List<Integer>> backing = new LinkedHashMap<>();
        backing.put("a", new ArrayList<>(Arrays.asList(1, 2)));
        backing.put("b", new ArrayList<>(Collections.singletonList(3)));
        final ListMultimap<String, Integer> mm = ListMultimap.wrap(backing);

        assertTrue(mm.removeValues(backing));

        assertTrue(mm.isEmpty());
        assertTrue(backing.isEmpty());
    }

    @Test
    public void testRemoveValues_AllValuesRemovesKey() {
        listMultimap.putValues("key", Arrays.asList(1, 2));
        assertTrue(listMultimap.removeValues("key", Arrays.asList(1, 2)));
        assertNull(listMultimap.get("key"));
    }

    @Test
    public void testRemoveValues_Map() {
        listMultimap.putValues("a", Arrays.asList(1, 2, 3));
        listMultimap.putValues("b", Arrays.asList(4, 5));

        Map<String, Collection<Integer>> toRemove = new HashMap<>();
        toRemove.put("a", Arrays.asList(1, 3));
        toRemove.put("b", Arrays.asList(4, 5));

        assertTrue(listMultimap.removeValues(toRemove));
        assertEquals(1, listMultimap.get("a").size());
        assertTrue(listMultimap.get("a").contains(2));
        assertNull(listMultimap.get("b"));
    }

    @Test
    public void testRemoveValues_Map_NullMap() {
        Map<String, Collection<Integer>> nullMap = null;
        assertFalse(listMultimap.removeValues(nullMap));
    }

    @Test
    public void testRemoveValues_Multimap() {
        listMultimap.putValues("a", Arrays.asList(1, 2, 3));
        listMultimap.putValues("b", Arrays.asList(4, 5));

        ListMultimap<String, Integer> toRemove = CommonUtil.newListMultimap();
        toRemove.putValues("a", Arrays.asList(2, 3));
        toRemove.put("b", 5);

        assertTrue(listMultimap.removeValues(toRemove));
        assertEquals(1, listMultimap.get("a").size());
        assertEquals(1, listMultimap.get("b").size());
    }

    @Test
    public void testRemoveValues_Multimap_NullMultimap() {
        Multimap<String, Integer, List<Integer>> nullMm = null;
        assertFalse(listMultimap.removeValues(nullMm));
    }

    @Test
    public void testRemoveValuesAnswersTheSameWayWhateverTheRelativeSizes() {
        // The two shapes r9526 could not answer at all, because AbstractSet.removeAll only iterated the argument
        // when the value collection was strictly larger: a List value collection always probed the argument, and a
        // Set value collection no larger than the argument did too. Both threw on a stored null; both now answer.
        final ListMultimap<String, String> listValued = CommonUtil.newListMultimap();
        listValued.putValues("key", Arrays.asList("a", "b", null));

        assertTrue(listValued.removeValues("key", List.of("a")));
        assertEquals(Arrays.asList("b", null), listValued.get("key"));

        final SetMultimap<String, String> equalSizes = CommonUtil.newSetMultimap();
        equalSizes.putValues("key", Arrays.asList("a", null));

        assertTrue(equalSizes.removeValues("key", List.of("a", "q")));
        assertEquals(1, equalSizes.get("key").size());
        assertTrue(equalSizes.containsEntry("key", null));
    }

    @Test
    public void testRemoveValuesForOneKeyStagesALiveValueCollection() {
        // removeValues(key, Collection) documents that the argument may be a live value collection or a sublist
        // backed by this Multimap. That is what the IdentityHashSet staging in the body is for: ArrayList.removeAll
        // compacts in place while a sublist view of that same list keeps reading it, so values the argument never
        // matched used to be dropped silently. Only removeValues(Map)/removeValues(Multimap) and removeValuesIf
        // pinned this shape before.
        ListMultimap<String, Integer> viaSubList = CommonUtil.newListMultimap();
        viaSubList.putValues("a", Arrays.asList(1, 2, 3, 4));
        assertTrue(viaSubList.removeValues("a", viaSubList.get("a").subList(0, 2)));
        assertEquals(List.of(3, 4), viaSubList.get("a"));

        // the whole live collection: every stored value matches, so the key goes with it
        ListMultimap<String, Integer> viaWholeCollection = CommonUtil.newListMultimap();
        viaWholeCollection.putValues("a", Arrays.asList(1, 2, 3, 4));
        viaWholeCollection.put("b", 1);
        assertTrue(viaWholeCollection.removeValues("a", viaWholeCollection.get("a")));
        assertNull(viaWholeCollection.get("a"));
        assertEquals(List.of(1), viaWholeCollection.get("b"));

        // a Set value collection too, where Collection.removeAll would have picked an equivalence by size
        SetMultimap<String, Integer> setValued = CommonUtil.newSetMultimap();
        setValued.putValues("a", Arrays.asList(1, 2, 3));
        assertTrue(setValued.removeValues("a", setValued.get("a")));
        assertNull(setValued.get("a"));
    }

    // G053-01: lazily allocated match sets - mixed matching / non-matching / absent keys
    @Test
    public void testRemoveValuesMap_mixedMatchingAndNonMatchingKeys() {
        final Map<String, List<Integer>> backing = g053Backing();
        final ListMultimap<String, Integer> mm = ListMultimap.wrap(backing);
        final Map<String, List<Integer>> removals = new LinkedHashMap<>();
        removals.put("a", Arrays.asList(1));
        removals.put("b", Arrays.asList(9));
        removals.put("c", Arrays.asList(1));
        removals.put("d", Arrays.asList(6));
        removals.put("e", Collections.emptyList());
        removals.put("u", Arrays.asList(0));

        assertTrue(mm.removeValues(removals));
        assertEquals(Arrays.asList(2, 3), backing.get("a"));
        assertEquals(Arrays.asList(4, 5), backing.get("b"));
        assertFalse(backing.containsKey("c"));
        assertFalse(backing.containsKey("d"));
        assertEquals(Arrays.asList(7), backing.get("e"));
        assertEquals(Arrays.asList(8, 9), backing.get("u"));
        assertEquals(Arrays.asList("a", "b", "e", "u"), new ArrayList<>(backing.keySet()));

        final Map<String, List<Integer>> noMatch = new LinkedHashMap<>();
        noMatch.put("a", Arrays.asList(1));
        noMatch.put("u", Arrays.asList(1));
        assertFalse(mm.removeValues(noMatch));
        assertEquals(Arrays.asList(2, 3), backing.get("a"));
    }

    @Test
    public void testRemoveValuesPreservesOriginalMembershipRules() {
        String selected = new String("same");
        String equalButDistinct = new String("same");
        ListMultimap<String, String> identityTarget = CommonUtil.newListMultimap();
        identityTarget.putValues("a", List.of(selected, equalButDistinct));
        Set<String> identityRemoval = new IdentityHashSet<>();
        identityRemoval.add(selected);
        assertTrue(identityTarget.removeValues(Map.of("a", identityRemoval)));
        assertEquals(1, identityTarget.get("a").size());
        assertSame(equalButDistinct, identityTarget.get("a").get(0));

        ListMultimap<String, String> comparatorTarget = CommonUtil.newListMultimap();
        comparatorTarget.putValues("a", List.of("UPPER", "keep"));
        Set<String> comparatorRemoval = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        comparatorRemoval.add("upper");
        assertTrue(comparatorTarget.removeValues(Map.of("a", comparatorRemoval)));
        assertEquals(List.of("keep"), comparatorTarget.get("a"));
    }

    @Test
    public void testRemoveValuesSelfAcceptsAFixedSizeOrImmutableValueCollection() {
        // The self case drops each non-empty value collection instead of calling removeIf on it, so a wrapped
        // fixed-size or immutable list is never asked to remove anything. Routing m == this through
        // removeValues(Map) threw UnsupportedOperationException for all three of these; r9526 accepted them.
        for (final List<Integer> fixed : Arrays.asList(Arrays.asList(1, 2), List.of(1, 2), Collections.unmodifiableList(Arrays.asList(1, 2)))) {
            final Map<String, List<Integer>> backing = new HashMap<>();
            backing.put("a", fixed);
            final ListMultimap<String, Integer> wrapped = ListMultimap.wrap(backing);

            assertTrue(wrapped.removeValues(wrapped));
            assertTrue(backing.isEmpty());
            assertEquals(Arrays.asList(1, 2), fixed);
        }
    }

    @Test
    public void testRemoveValuesSelfDropsTheValueCollectionsWithoutEmptyingThem() {
        // documented on removeValues(Multimap): for the self case the value collections are dropped rather than
        // emptied, so a caller still holding one - here through the map it handed to wrap - sees it unchanged.
        final List<Integer> retained = new ArrayList<>(Arrays.asList(1, 2, 3));
        final Map<String, List<Integer>> backing = new HashMap<>();
        backing.put("a", retained);
        final ListMultimap<String, Integer> wrapped = ListMultimap.wrap(backing);

        assertTrue(wrapped.removeValues(wrapped));
        assertTrue(backing.isEmpty());
        assertEquals(Arrays.asList(1, 2, 3), retained);
    }

    @Test
    public void testRemoveValuesStagesSharedLiveCollections() {
        for (final boolean useSubList : new boolean[] { false, true }) {
            ListMultimap<String, Integer> target = CommonUtil.newListMultimap();
            target.putValues("a", Arrays.asList(1, 3));
            target.putValues("b", Arrays.asList(1, 2, 3));
            Collection<Integer> live = useSubList ? target.get("a").subList(0, 1) : target.get("a");
            Map<String, Collection<Integer>> removals = new LinkedHashMap<>();
            removals.put("a", live);
            removals.put("b", live);
            assertTrue(target.removeValues(removals));
            assertEquals(useSubList ? List.of(3) : null, target.get("a"));
            assertEquals(useSubList ? List.of(2, 3) : List.of(2), target.get("b"));
        }

        ListMultimap<String, Integer> target = CommonUtil.newListMultimap();
        target.put("a", 1);
        target.putValues("b", List.of(1, 2));
        Map<String, List<Integer>> removalMap = new LinkedHashMap<>();
        removalMap.put("a", target.get("a"));
        removalMap.put("b", target.get("a"));
        assertTrue(target.removeValues(ListMultimap.wrap(removalMap)));
        assertNull(target.get("a"));
        assertEquals(List.of(2), target.get("b"));
    }

    @Test
    public void testRemoveValuesTreatsAValueTheArgumentCannotHoldAsNotAMember() {
        // A stored value the removal collection is not allowed to be asked about must count as "not a member"
        // rather than abort the removal: Collection.contains declares NullPointerException/ClassCastException
        // OPTIONAL, and AbstractSet.removeAll suppressed both whenever it chose to iterate the argument. This is a
        // regression guard for r9526 behaviour, so it is green on r9526 and RED against an unguarded
        // valuesToRemove.contains(storedValue) loop.
        for (final Collection<String> strictArgument : Arrays.<Collection<String>> asList(List.of("a"), Set.of("a"), new TreeSet<>(List.of("a")))) {
            final SetMultimap<String, String> withNull = CommonUtil.newSetMultimap();
            withNull.putValues("key", Arrays.asList("a", "b", null));

            assertTrue(withNull.removeValues("key", strictArgument));
            assertEquals(2, withNull.get("key").size());
            assertTrue(withNull.containsEntry("key", null));
            assertTrue(withNull.containsEntry("key", "b"));
            assertFalse(withNull.containsEntry("key", "a"));
        }

        // a type-restricted argument cannot be asked about a foreign-typed stored value either
        final SetMultimap<String, Object> numbers = CommonUtil.newSetMultimap();
        numbers.putValues("key", Arrays.<Object> asList(1, 2, 3));
        final TreeSet<String> typeRestricted = new TreeSet<>();
        typeRestricted.add("x");

        assertFalse(numbers.removeValues("key", typeRestricted));
        assertEquals(3, numbers.get("key").size());
    }

    @Test
    public void testRemoveValuesUsesArgumentMembershipLikeItsSiblings() {
        // The 2-arg removeValues must answer the same question as removeValues(Map)/removeValuesIf/
        // removeValues(Multimap): membership is valuesToRemove.contains(value). Collection.removeAll picked the
        // target's or the argument's equivalence according to their relative sizes, so it disagreed with all
        // four siblings and even with itself once the value collection grew.
        final SetMultimap<String, String> sorted = CommonUtil.newSetMultimap(HashMap::new, () -> new TreeSet<>(String.CASE_INSENSITIVE_ORDER));
        sorted.putValues("key", Arrays.asList("A", "B"));
        // the documented consequence: the single-value family uses the value set's own equivalence, so it answers
        // this very value the other way round
        assertTrue(sorted.containsEntry("key", "a"));
        assertFalse(sorted.removeValues("key", Arrays.asList("a")));
        assertEquals(new TreeSet<>(Arrays.asList("A", "B")), sorted.get("key"));

        // the same shape holding a single value: the old size heuristic answered it the opposite way FROM the
        // two-value shape above, because it only iterated the argument when the value set was strictly larger.
        // At r9526 this one-value case was therefore already false - it is a contrast, not a changed answer.
        final SetMultimap<String, String> single = CommonUtil.newSetMultimap(HashMap::new, () -> new TreeSet<>(String.CASE_INSENSITIVE_ORDER));
        single.putValues("key", Arrays.asList("A"));
        assertFalse(single.removeValues("key", Arrays.asList("a")));
        assertEquals(new TreeSet<>(Arrays.asList("A")), single.get("key"));

        // a comparator-based ARGUMENT against a default value set: the documented rule says its membership decides
        final SetMultimap<String, String> plain = CommonUtil.newSetMultimap();
        plain.putValues("key", Arrays.asList("abc", "def"));
        final TreeSet<String> caseInsensitive = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        caseInsensitive.add("ABC");
        assertTrue(plain.removeValues("key", caseInsensitive));
        assertEquals(Collections.singleton("def"), plain.get("key"));
        assertFalse(plain.removeValuesIf(key -> true, Collections.singletonList("DEF")));

        // equals-consistent values behave exactly as the method javadoc's own example says
        final ListMultimap<String, Integer> numbers = CommonUtil.newListMultimap();
        numbers.putValues("numbers", Arrays.asList(1, 2, 3, 2, 4, 2));
        assertTrue(numbers.removeValues("numbers", Arrays.asList(2, 3)));
        assertEquals(Arrays.asList(1, 4), numbers.get("numbers"));
    }

    // removeValuesIf

    @Test
    public void testRemoveValuesIf() {
        listMultimap.putValues("a", Arrays.asList(1, 2, 3));
        listMultimap.putValues("b", Arrays.asList(1, 2, 3));

        assertTrue(listMultimap.removeValuesIf(key -> key.equals("a"), Arrays.asList(1, 2)));
        assertEquals(1, listMultimap.get("a").size());
        assertEquals(3, listMultimap.get("b").size());
    }

    @Test
    public void testRemoveValuesIf_BiPredicate() {
        listMultimap.putValues("small", Arrays.asList(1, 2));
        listMultimap.putValues("large", Arrays.asList(1, 2, 3, 4));

        assertTrue(listMultimap.removeValuesIf((k, v) -> v.size() > 3, Arrays.asList(1, 2)));
        assertEquals(2, listMultimap.get("small").size());
        assertEquals(2, listMultimap.get("large").size());
    }

    @Test
    public void testRemoveValuesIf_BiPredicate_EmptyMultimap() {
        assertFalse(listMultimap.removeValuesIf((BiPredicate<? super String, ? super List<Integer>>) (k, v) -> true, Arrays.asList(1)));
    }

    @Test
    public void testRemoveValuesIf_EmptyValues() {
        listMultimap.put("a", 1);
        assertFalse(listMultimap.removeValuesIf(key -> true, new ArrayList<>()));
    }

    @Test
    public void testRemoveValuesIf_KeyPredicate_EmptyMultimap() {
        assertFalse(listMultimap.removeValuesIf(k -> true, Arrays.asList(1)));
    }

    // G053-01: lazily allocated match sets - removeValuesIf over matching and non-matching keys
    @Test
    public void testRemoveValuesIf_mixedMatchingAndNonMatchingKeys() {
        final Map<String, List<Integer>> backing = g053Backing();
        final ListMultimap<String, Integer> mm = ListMultimap.wrap(backing);

        assertTrue(mm.removeValuesIf(key -> !key.equals("e"), Arrays.asList(1, 6, 7)));
        assertEquals(Arrays.asList(2, 3), backing.get("a"));
        assertEquals(Arrays.asList(4, 5), backing.get("b"));
        assertFalse(backing.containsKey("d"));
        assertEquals(Arrays.asList(7), backing.get("e"));
        assertEquals(Arrays.asList(8, 9), backing.get("u"));

        assertFalse(mm.removeValuesIf((key, values) -> true, Arrays.asList(100)));
        assertTrue(mm.removeValuesIf((key, values) -> values.size() == 2 && !key.equals("u"), Arrays.asList(3, 4)));
        assertEquals(Arrays.asList(2), backing.get("a"));
        assertEquals(Arrays.asList(5), backing.get("b"));
        assertEquals(Arrays.asList(8, 9), backing.get("u"));
    }

    /**
     * {@code Collection.removeIf} on an unmodifiable collection throws {@code UnsupportedOperationException}
     * unconditionally - it does not first check whether anything would actually be removed. The shared body
     * behind both {@code removeValuesIf} overloads guarded only on the value collection being non-empty, not
     * on the match set being non-empty, so a removal that matched nothing still reached {@code removeIf} and
     * threw - while the sibling {@code removeValues(Object, Collection)} and {@code removeValues(Map)} forms,
     * which do carry that guard, returned {@code false} on the very same data.
     */
    @Test
    public void test_removeValuesIf_noMatchOnUnmodifiableValues_regression_20260918() {
        final Map<String, List<String>> backing = new HashMap<>();
        backing.put("k", Collections.unmodifiableList(new ArrayList<>(Arrays.asList("a", "b"))));
        final ListMultimap<String, String> wrapped = ListMultimap.wrap(backing);

        // Before the fix this threw UnsupportedOperationException.
        assertFalse(wrapped.removeValuesIf(k -> true, Arrays.asList("no-such-value")));
        assertEquals(Arrays.asList("a", "b"), backing.get("k"));

        // The siblings always behaved this way - they are the reference for the fixed behaviour.
        assertFalse(wrapped.removeValues("k", Arrays.asList("no-such-value")));
        assertFalse(wrapped.removeValues(Collections.singletonMap("k", Arrays.asList("no-such-value"))));
    }

    @Test
    public void testRemoveValuesIfPreservesArgumentMembership() {
        final String selected = new String("value");
        final String retained = new String("value");
        final ListMultimap<String, String> values = CommonUtil.newListMultimap();
        values.putValues("key", Arrays.asList(selected, retained));
        final Set<String> identityRemovals = new IdentityHashSet<>();
        identityRemovals.add(selected);

        assertTrue(values.removeValuesIf(key -> true, identityRemovals));
        assertEquals(1, values.get("key").size());
        assertSame(retained, values.get("key").get(0));

        final Set<String> comparatorRemovals = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        comparatorRemovals.add("VALUE");
        assertTrue(values.removeValuesIf((key, collection) -> true, comparatorRemovals));
        assertTrue(values.isEmpty());
    }

    @Test
    public void testRemoveValuesIfUsesArgumentMembershipForSortedTarget() {
        final SetMultimap<String, String> values = CommonUtil.newSetMultimap(HashMap::new, () -> new TreeSet<>(String.CASE_INSENSITIVE_ORDER));
        values.putValues("key", Arrays.asList("A", "B"));

        assertFalse(values.removeValuesIf(key -> true, Arrays.asList("a")));
        assertEquals(new TreeSet<>(Arrays.asList("A", "B")), values.get("key"));
    }

    @Test
    public void testRemoveValuesIfWithLiveSubList() {
        final ListMultimap<String, Integer> values = CommonUtil.newListMultimap(LinkedHashMap.class);
        values.putValues("a", Arrays.asList(1, 2, 9));
        values.putValues("b", Arrays.asList(1, 2, 3));

        assertTrue(values.removeValuesIf((key, collection) -> true, values.get("a").subList(0, 2)));
        assertEquals(Arrays.asList(9), values.get("a"));
        assertEquals(Arrays.asList(3), values.get("b"));
    }

    @Test
    public void testRemoveValuesIfWithLiveValueCollection() {
        final ListMultimap<String, Integer> values = CommonUtil.newListMultimap(LinkedHashMap.class);
        values.putValues("a", Arrays.asList(1, 2));
        values.putValues("b", Arrays.asList(1, 2, 3));

        assertTrue(values.removeValuesIf(key -> true, values.get("a")));
        assertFalse(values.containsKey("a"));
        assertEquals(Arrays.asList(3), values.get("b"));
    }

    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    void unmatchedUnmodifiableCollectionsRemainUntouched(boolean entryPredicate) {
        final List<Integer> shared = List.of(1, 2);
        final Map<String, List<Integer>> backing = new LinkedHashMap<>();
        backing.put("a", shared);
        backing.put("b", shared);
        final ListMultimap<String, Integer> values = ListMultimap.wrap(backing);

        assertFalse(removeAllSelected(values, entryPredicate, List.of(99)));

        assertSame(shared, values.get("a"));
        assertSame(shared, values.get("b"));
        assertEquals(2, values.keyCount());
    }
}

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
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.Predicate;

import org.junit.jupiter.api.Test;

public class MultimapReplaceTest extends MultimapTestSupport {
    @Test
    public void testReplaceOneIfBiPredicate_Matching() {
        listMultimap.putValues("key1", Arrays.asList(10, 20));
        listMultimap.putValues("key2", Arrays.asList(10, 20, 30));

        assertTrue(listMultimap.replaceEntriesIf((k, v) -> v.size() > 2, 10, 99));
        assertEquals(Integer.valueOf(10), listMultimap.get("key1").get(0));
        assertEquals(Integer.valueOf(99), listMultimap.get("key2").get(0));
    }

    @Test
    public void testReplaceManyWithOneIfPredicate_Matching() {
        listMultimap.putValues("key1", Arrays.asList(10, 20, 30));
        listMultimap.putValues("key2", Arrays.asList(10, 20, 40));

        assertTrue(listMultimap.replaceValuesIf(key -> key.equals("key1"), CommonUtil.toList(99)));
        assertEquals(1, listMultimap.get("key1").size());
        assertTrue(listMultimap.get("key1").contains(99));
        assertFalse(listMultimap.get("key1").contains(30));
        assertEquals(3, listMultimap.get("key2").size());
    }

    @Test
    public void testReplaceManyWithOneIfBiPredicate_Matching() {
        listMultimap.putValues("key1", Arrays.asList(10, 20, 30, 40));
        listMultimap.putValues("key2", Arrays.asList(10, 20));

        assertTrue(listMultimap.replaceValuesIf((k, v) -> v.size() > 2, CommonUtil.toList(99)));
        assertEquals(1, listMultimap.get("key1").size());
        assertTrue(listMultimap.get("key1").contains(99));
        assertEquals(2, listMultimap.get("key2").size());
    }

    @Test
    public void testReplaceOne() {
        Multimap<String, Integer, List<Integer>> mm = getTestMultimap();
        mm.putValues("a", Arrays.asList(1, 2, 1, 3));

        assertTrue(mm.replaceEntry("a", 1, 10));
        assertEquals(Arrays.asList(10, 2, 1, 3), mm.get("a"));

        assertTrue(mm.replaceEntry("a", 1, 11));
        assertEquals(Arrays.asList(10, 2, 11, 3), mm.get("a"));

        assertFalse(mm.replaceEntry("a", 99, 100));
        assertFalse(mm.replaceEntry("b", 1, 10));

        Multimap<String, Integer, Set<Integer>> smm = getSetTestMultimap();
        smm.putValues("x", new HashSet<>(Arrays.asList(10, 20, 30)));
        assertTrue(smm.replaceEntry("x", 20, 200));
        assertTrue(smm.get("x").containsAll(Arrays.asList(10, 30, 200)));
        assertFalse(smm.get("x").contains(20));

    }

    @Test
    public void testReplaceOneIf_keyPredicate() {
        Multimap<String, Integer, List<Integer>> mm = getTestMultimap();
        mm.putValues("apple", Arrays.asList(1, 2, 1));
        mm.putValues("apricot", Arrays.asList(1, 3));
        mm.put("banana", 1);

        assertTrue(mm.replaceEntriesIf(key -> key.startsWith("ap"), 1, 10));
        assertEquals(Arrays.asList(10, 2, 1), mm.get("apple"));
        assertEquals(Arrays.asList(10, 3), mm.get("apricot"));
        assertEquals(Arrays.asList(1), mm.get("banana"));

        assertFalse(mm.replaceEntriesIf(key -> true, 99, 100));
    }

    @Test
    public void testReplaceOneIf_biPredicate() {
        Multimap<String, Integer, List<Integer>> mm = getTestMultimap();
        mm.putValues("a", Arrays.asList(1, 2, 1));
        mm.putValues("b", Arrays.asList(1, 3));
        mm.put("c", 1);

        assertTrue(mm.replaceEntriesIf((k, v) -> k.equals("c") || v.contains(2), 1, 10));
        assertEquals(Arrays.asList(10, 2, 1), mm.get("a"));
        assertEquals(Arrays.asList(1, 3), mm.get("b"));
        assertEquals(Arrays.asList(10), mm.get("c"));
    }

    @Test
    public void testReplaceManyWithOneIfPredicate() {
        multimap.putValues("key1", Arrays.asList(1, 2, 3, 4));
        multimap.putValues("key2", Arrays.asList(2, 3, 4, 5));

        assertTrue(multimap.replaceValuesIf(k -> k.equals("key1"), CommonUtil.toList(99)));
        assertEquals(1, multimap.get("key1").size());
        assertTrue(multimap.get("key1").contains(99));
        assertFalse(multimap.get("key1").contains(2));
        assertFalse(multimap.get("key1").contains(3));
    }

    @Test
    public void testReplaceOne_InList() {
        listMultimap.put("key1", 10);
        listMultimap.put("key1", 20);
        listMultimap.put("key1", 10);

        assertTrue(listMultimap.replaceEntry("key1", 10, 99));
        List<Integer> values = listMultimap.get("key1");
        assertEquals(3, values.size());
        assertEquals(Integer.valueOf(99), values.get(0));
        assertEquals(Integer.valueOf(10), values.get(2));
    }

    @Test
    public void testReplaceOne_InSet() {
        setMultimap.put("key1", 10);
        setMultimap.put("key1", 20);

        assertTrue(setMultimap.replaceEntry("key1", 10, 99));
        @SuppressWarnings("unchecked")
        Set<Integer> values = (Set<Integer>) setMultimap.get("key1");
        assertEquals(2, values.size());
        assertTrue(values.contains(99));
        assertFalse(values.contains(10));
    }

    @Test
    public void testReplaceOne_NonExistentKey() {
        assertFalse(listMultimap.replaceEntry("key1", 10, 20));
    }

    @Test
    public void testReplaceOne_NonExistentOldValue() {
        listMultimap.put("key1", 10);
        assertFalse(listMultimap.replaceEntry("key1", 20, 30));
    }

    @Test
    public void testReplaceOne_WithNullOldValue() {
        listMultimap.put("key1", null);
        listMultimap.put("key1", 20);

        assertTrue(listMultimap.replaceEntry("key1", null, 99));
        assertEquals(Integer.valueOf(99), listMultimap.get("key1").get(0));
    }

    @Test
    public void testReplaceOne_NullKey() {
        listMultimap.put(null, 10);
        assertTrue(listMultimap.replaceEntry(null, 10, 20));
        assertEquals(Integer.valueOf(20), listMultimap.get(null).get(0));
    }

    @Test
    public void testReplaceEdgeCases() {
        assertFalse(multimap.replaceEntry("key", 1, 2));
        assertFalse(multimap.replaceValues("key", CommonUtil.toList(1)));

        multimap.put("key", 1);
        assertTrue(multimap.replaceEntry("key", 1, 1));

        multimap.clear();
        multimap.putValues("key", Arrays.asList(1, 2, 1, 3, 1));
        assertTrue(multimap.replaceEntry("key", 1, 99));
        List<Integer> values = multimap.get("key");
        assertEquals(Integer.valueOf(99), values.get(0));
        assertEquals(Integer.valueOf(1), values.get(2));
    }

    @Test
    public void testReplaceEntry() {
        listMultimap.putValues("key", Arrays.asList(1, 2, 3));

        assertTrue(listMultimap.replaceEntry("key", 2, 99));
        assertEquals(Arrays.asList(1, 99, 3), listMultimap.get("key"));

        assertFalse(listMultimap.replaceEntry("key", 100, 200));
        assertFalse(listMultimap.replaceEntry("nonexistent", 1, 2));
    }

    @Test
    public void testReplaceWithInvalidValue() {
        Multimap<String, Integer, Set<Integer>> customMap = CommonUtil.newMultimap(HashMap::new, () -> new HashSet<Integer>() {
            @Override
            public boolean add(Integer e) {
                if (e != null && e < 0) {
                    return false;
                }
                return super.add(e);
            }
        });

        customMap.put("key", 1);
        assertThrows(IllegalStateException.class, () -> customMap.replaceEntry("key", 1, -1));
    }

    @Test
    public void testReplaceOneIfPredicate_NoMatching() {
        listMultimap.put("key1", 10);
        assertFalse(listMultimap.replaceEntriesIf(key -> key.equals("key2"), 10, 99));
    }

    @Test
    public void testReplaceOneIfPredicate_Matching() {
        listMultimap.put("key1", 10);
        listMultimap.put("key2", 10);
        listMultimap.put("other", 10);

        assertTrue(listMultimap.replaceEntriesIf(key -> key.startsWith("key"), 10, 99));
        assertEquals(Integer.valueOf(99), listMultimap.get("key1").get(0));
        assertEquals(Integer.valueOf(99), listMultimap.get("key2").get(0));
        assertEquals(Integer.valueOf(10), listMultimap.get("other").get(0));
    }

    @Test
    public void testReplaceOneIfBiPredicate_NoMatching() {
        listMultimap.put("key1", 10);
        assertFalse(listMultimap.replaceEntriesIf((k, v) -> v.size() > 5, 10, 99));
    }

    @Test
    public void testReplaceOneIf() {
        listMultimap.put("replace_key", 10);
        listMultimap.put("keep_key", 10);

        Predicate<String> keyFilter = key -> key.startsWith("replace_");
        assertTrue(listMultimap.replaceEntriesIf(keyFilter, 10, 99));

        assertTrue(listMultimap.containsEntry("replace_key", 99));
        assertFalse(listMultimap.containsEntry("replace_key", 10));
        assertTrue(listMultimap.containsEntry("keep_key", 10));

        assertFalse(listMultimap.replaceEntriesIf(keyFilter, 999, 100));
    }

    @Test
    public void testReplaceEntriesIf() {
        listMultimap.put("a", 1);
        listMultimap.put("b", 1);
        listMultimap.put("c", 1);

        assertTrue(listMultimap.replaceEntriesIf(key -> key.compareTo("b") <= 0, 1, 99));
        assertEquals(Integer.valueOf(99), listMultimap.get("a").get(0));
        assertEquals(Integer.valueOf(99), listMultimap.get("b").get(0));
        assertEquals(Integer.valueOf(1), listMultimap.get("c").get(0));
    }

    @Test
    public void testReplaceEntriesIf_BiPredicate() {
        listMultimap.putValues("a", Arrays.asList(1, 2));
        listMultimap.putValues("b", Arrays.asList(1, 2, 3, 4));

        assertTrue(listMultimap.replaceEntriesIf((k, v) -> v.size() > 3, 1, 99));
        assertEquals(Integer.valueOf(1), listMultimap.get("a").get(0));
        assertEquals(Integer.valueOf(99), listMultimap.get("b").get(0));
    }

    @Test
    public void testReplaceEntriesIf_KeyPredicate_EmptyMultimap() {
        assertFalse(listMultimap.replaceEntriesIf(k -> true, 1, 2));
    }

    @Test
    public void testReplaceEntriesIf_BiPredicate_EmptyMultimap() {
        assertFalse(listMultimap.replaceEntriesIf((BiPredicate<? super String, ? super List<Integer>>) (k, v) -> true, 1, 2));
    }

    @Test
    public void testReplaceValues() {
        multimap.putValues("key", Arrays.asList(1, 2, 3));

        assertTrue(multimap.replaceValues("key", CommonUtil.toList(99)));
        assertEquals(1, multimap.get("key").size());
    }

    @Test
    public void testReplaceValuesWithSameCollectionInstance() {
        multimap.putValues("key", Arrays.asList(1, 2, 3));

        List<Integer> sameCollection = multimap.get("key");
        assertTrue(multimap.replaceValues("key", sameCollection));
        assertEquals(Arrays.asList(1, 2, 3), multimap.get("key"));
    }

    @Test
    public void testReplaceValues_NonExistentKey() {
        assertFalse(listMultimap.replaceValues("nokey", Arrays.asList(1, 2)));
        assertTrue(listMultimap.isEmpty());
    }

    @Test
    public void testReplaceValues_EmptyNewValuesRemovesKey() {
        listMultimap.putValues("key1", Arrays.asList(1, 2, 3));
        assertTrue(listMultimap.replaceValues("key1", Collections.emptyList()));
        assertNull(listMultimap.get("key1"));
        assertFalse(listMultimap.containsKey("key1"));
    }

    @Test
    public void testReplaceValues_NullNewValuesRemovesKey() {
        listMultimap.putValues("key1", Arrays.asList(1, 2, 3));
        assertTrue(listMultimap.replaceValues("key1", null));
        assertNull(listMultimap.get("key1"));
    }

    @Test
    public void testReplaceManyWithOneIf() {
        listMultimap.put("replace_key", 10);
        listMultimap.put("replace_key", 20);
        listMultimap.put("keep_key", 10);

        Collection<Integer> oldValues = Arrays.asList(10, 20);
        Predicate<String> keyFilter = key -> key.startsWith("replace_");

        assertTrue(listMultimap.replaceValuesIf(keyFilter, CommonUtil.toList(99)));

        Collection<Integer> values = listMultimap.get("replace_key");
        assertEquals(1, values.size());
        assertTrue(values.contains(99));
        assertTrue(listMultimap.containsEntry("keep_key", 10));

        assertTrue(listMultimap.replaceValuesIf(keyFilter, CommonUtil.toList(99)));
    }

    @Test
    public void testReplaceValuesIf() {
        listMultimap.putValues("a", Arrays.asList(1, 2, 3));
        listMultimap.putValues("b", Arrays.asList(4, 5));

        assertTrue(listMultimap.replaceValuesIf(key -> key.equals("a"), Arrays.asList(99, 100)));
        assertEquals(Arrays.asList(99, 100), listMultimap.get("a"));
        assertEquals(Arrays.asList(4, 5), listMultimap.get("b"));
    }

    @Test
    public void testReplaceValuesIf_BiPredicate() {
        listMultimap.putValues("a", Arrays.asList(1, 2, 3));
        listMultimap.putValues("b", Arrays.asList(4));

        assertTrue(listMultimap.replaceValuesIf((k, v) -> v.size() > 2, Arrays.asList(99)));
        assertEquals(Arrays.asList(99), listMultimap.get("a"));
        assertEquals(Arrays.asList(4), listMultimap.get("b"));
    }

    @Test
    public void testReplaceManyWithOneIfPredicate_EmptyOldValues() {
        assertFalse(listMultimap.replaceValuesIf(key -> true, CommonUtil.toList(99)));
    }

    @Test
    public void testReplaceManyWithOneIfBiPredicate_EmptyOldValues() {
        assertFalse(listMultimap.replaceValuesIf((k, v) -> true, CommonUtil.toList(99)));
    }

    @Test
    public void testReplaceValuesIf_EmptyNewValues() {
        listMultimap.putValues("a", Arrays.asList(1, 2));
        listMultimap.putValues("b", Arrays.asList(3));

        assertTrue(listMultimap.replaceValuesIf(key -> key.equals("a"), new ArrayList<>()));
        assertNull(listMultimap.get("a"));
        assertNotNull(listMultimap.get("b"));
    }

    @Test
    public void testReplaceValuesIf_BiPredicate_EmptyNewValues() {
        listMultimap.putValues("a", Arrays.asList(1, 2, 3));
        listMultimap.putValues("b", Arrays.asList(4));

        assertTrue(listMultimap.replaceValuesIf((k, v) -> v.size() > 2, new ArrayList<>()));
        assertNull(listMultimap.get("a"));
        assertNotNull(listMultimap.get("b"));
    }

    @Test
    public void testReplaceValuesIf_KeyPredicate_EmptyMultimap() {
        assertFalse(listMultimap.replaceValuesIf(k -> true, Arrays.asList(99)));
    }

    @Test
    public void testReplaceValuesIf_BiPredicate_EmptyMultimap() {
        assertFalse(listMultimap.replaceValuesIf((BiPredicate<? super String, ? super List<Integer>>) (k, v) -> true, Arrays.asList(99)));
    }

    @Test
    public void testReplaceAllWithOne_ExistingKey() {
        listMultimap.putValues("key1", Arrays.asList(10, 20, 30, 40));

        assertTrue(listMultimap.replaceValues("key1", CommonUtil.toList(99)));
        assertEquals(1, listMultimap.get("key1").size());
        assertEquals(Integer.valueOf(99), listMultimap.get("key1").get(0));
    }

    @Test
    public void testReplaceAll_WithFunction() {
        listMultimap.put("key1", 10);
        listMultimap.put("key2", 20);

        listMultimap.replaceAll((k, v) -> {
            List<Integer> newList = new ArrayList<>();
            newList.add(99);
            return newList;
        });

        assertEquals(Integer.valueOf(99), listMultimap.get("key1").get(0));
        assertEquals(Integer.valueOf(99), listMultimap.get("key2").get(0));
    }

    @Test
    public void testReplaceAllWithOne() {
        listMultimap.put("key1", 10);
        listMultimap.put("key1", 20);
        listMultimap.put("key2", 30);

        assertTrue(listMultimap.replaceValues("key1", CommonUtil.toList(99)));
        List<Integer> values = listMultimap.get("key1");
        assertEquals(1, values.size());
        assertTrue(values.contains(99));
        assertTrue(listMultimap.containsEntry("key2", 30));
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
        assertTrue(listMultimap.containsEntry("key2", 30));
    }

    @Test
    public void testReplaceAllWithAliasedCollection() {
        multimap.putValues("key", Arrays.asList(1, 2, 3));

        multimap.replaceAll((k, v) -> v.subList(1, v.size()));
        assertEquals(Arrays.asList(2, 3), multimap.get("key"));
    }

    @Test
    public void testReplaceAllWithOne_NonExistentKey() {
        assertFalse(listMultimap.replaceValues("key1", CommonUtil.toList(99)));
    }

    @Test
    public void testReplaceAll_nullFunction_throwsIAE() {
        final BiFunction<String, List<Integer>, List<Integer>> nullFn = null;

        assertThrows(IllegalArgumentException.class, () -> listMultimap.replaceAll(nullFn));

        listMultimap.put("k", 1);
        assertThrows(IllegalArgumentException.class, () -> listMultimap.replaceAll(nullFn));
    }

    @Test
    public void testReplaceAll_EmptyMultimap() {
        listMultimap.replaceAll((k, v) -> {
            List<Integer> newList = new ArrayList<>();
            newList.add(99);
            return newList;
        });
        assertTrue(listMultimap.isEmpty());
    }

    @Test
    public void testReplaceAll_WithNullResults() {
        listMultimap.put("key1", 10);
        listMultimap.put("key2", 20);
        listMultimap.replaceAll((k, v) -> null);
        assertTrue(listMultimap.isEmpty());
    }

    @Test
    public void testReplaceAll_biFunction() {
        Multimap<String, Integer, List<Integer>> mm = getTestMultimap();
        mm.putValues("a", Arrays.asList(1, 2));
        mm.putValues("b", Arrays.asList(3, 4, 5));

        mm.replaceAll((key, values) -> {
            if (key.equals("a")) {
                return new ArrayList<>(Arrays.asList(values.get(0) * 10, values.get(1) * 10));
            }
            if (key.equals("b")) {
                return null;
            }
            return values;
        });

        assertEquals(Arrays.asList(10, 20), mm.get("a"));
        assertFalse(mm.containsKey("b"));

        mm.putValues("c", Arrays.asList(1, 2));
        mm.replaceAll((k, v) -> k.equals("c") ? new ArrayList<>() : v);
        assertFalse(mm.containsKey("c"));
    }

    @Test
    public void testReplaceOneIfBiPredicate() {
        listMultimap.put("key1", 10);
        listMultimap.put("key1", 20);
        listMultimap.put("key2", 10);

        BiPredicate<String, Collection<Integer>> filter = (key, values) -> values.size() > 1;
        assertTrue(listMultimap.replaceEntriesIf(filter, 10, 99));

        assertTrue(listMultimap.containsEntry("key1", 99));
        assertFalse(listMultimap.containsEntry("key1", 10));
        assertTrue(listMultimap.containsEntry("key2", 10));

        assertFalse(listMultimap.replaceEntriesIf(filter, 999, 100));
    }

    @Test
    public void testReplaceManyWithOneIfBiPredicate() {
        listMultimap.put("key1", 10);
        listMultimap.put("key1", 20);
        listMultimap.put("key2", 30);

        Collection<Integer> oldValues = Arrays.asList(10, 20);
        BiPredicate<String, Collection<Integer>> filter = (key, values) -> values.size() > 1;

        assertTrue(listMultimap.replaceValuesIf(filter, CommonUtil.toList(99)));

        List<Integer> values = listMultimap.get("key1");
        assertEquals(1, values.size());
        assertTrue(values.contains(99));
        assertTrue(listMultimap.containsEntry("key2", 30));

        assertFalse(listMultimap.replaceValuesIf(filter, CommonUtil.toList(99)));
    }

    @Test
    public void testReplaceValuesIfDefendsAgainstAliasedInput() {
        // regression: replaceValuesIf cleared the live value collection BEFORE copying from
        // newValues, so passing a live collection (or a view of it) emptied everything
        final ListMultimap<String, Integer> mm = CommonUtil.newListMultimap();
        mm.putValues("a", Arrays.asList(1, 2, 3));
        mm.replaceValuesIf(k -> k.equals("a"), mm.get("a"));
        assertEquals(Arrays.asList(1, 2, 3), mm.get("a"));

        final ListMultimap<String, Integer> mm2 = CommonUtil.newListMultimap();
        mm2.putValues("a", Arrays.asList(1, 2, 3));
        mm2.putValues("b", Arrays.asList(4, 5));
        mm2.replaceValuesIf((k, v) -> true, mm2.get("a"));
        assertEquals(Arrays.asList(1, 2, 3), mm2.get("a"));
        assertEquals(Arrays.asList(1, 2, 3), mm2.get("b"));
    }

    @Test
    public void testReplaceAllEmptiedSameInstanceRemovesMapping() {
        // regression: replaceAll checked the same-instance fast path BEFORE the documented
        // "null or empty result removes the mapping" rule (unlike its already-fixed siblings
        // compute/computeIfPresent/merge), so a function that emptied the live collection in
        // place and returned it left an empty value collection in the backing map
        final ListMultimap<String, Integer> mm = CommonUtil.newListMultimap();
        mm.putValues("a", Arrays.asList(1, 2));
        mm.put("b", 3);

        mm.replaceAll((k, v) -> {
            if (k.equals("a")) {
                v.clear(); // emptied in place, same instance returned
            }
            return v;
        });

        assertFalse(mm.containsKey("a"));
        assertNull(mm.get("a"));
        assertEquals(Arrays.asList(3), mm.get("b")); // untouched same-instance entry left unchanged
        assertEquals(1, mm.totalValueCount());
        assertFalse(mm.isEmpty());
    }

    @Test
    public void testReplaceEntry_setMultimapDuplicateNewValue_restoresOldValue() {
        // regression: Set-backed replace removed oldValue before add(newValue); when newValue was
        // already present, add failed and the old value was permanently lost.
        final SetMultimap<String, Integer> mm = CommonUtil.newSetMultimap();
        mm.putValues("k", Arrays.asList(1, 2));

        final IllegalStateException ex = assertThrows(IllegalStateException.class, () -> mm.replaceEntry("k", 1, 2));
        assertTrue(ex.getMessage().contains("Failed to add"));

        assertTrue(mm.containsKey("k"));
        assertEquals(new HashSet<>(Arrays.asList(1, 2)), mm.get("k"));
    }

    @Test
    public void testReplaceEntry_rejectedNewValueException_restoresOldValue() {
        final Multimap<String, Integer, java.util.NavigableSet<Integer>> mm = CommonUtil.newMultimap(HashMap::new, TreeSet::new);
        mm.put("k", 1);

        final IllegalStateException ex = assertThrows(IllegalStateException.class, () -> mm.replaceEntry("k", 1, null));

        assertTrue(ex.getCause() instanceof NullPointerException);
        assertEquals(Collections.singleton(1), mm.get("k"));
        assertEquals(1, mm.totalValueCount());
    }

    @Test
    public void testReplaceValues_emptyNewValues_removesKey() {
        final SetMultimap<String, Integer> mm = CommonUtil.newSetMultimap();
        mm.put("k", 1);
        mm.put("k", 2);

        assertTrue(mm.replaceValues("k", Collections.emptyList()));
        assertFalse(mm.containsKey("k"));
        assertNull(mm.get("k"));
    }

    @Test
    public void testReplaceValues_noValuesAccepted_reportsFailureWithoutEmptyMapping() {
        // Value collection that accepts only the first add; after clear()+addAll in replaceValues
        // nothing is accepted. Restoration is impossible for this deliberately stateful collection,
        // so the method reports failure and at least preserves the no-empty-mapping invariant.
        final Multimap<String, Integer, Set<Integer>> rejecting = CommonUtil.newMultimap(HashMap::new, () -> new HashSet<Integer>() {
            private boolean allowOne = true;

            @Override
            public boolean add(final Integer e) {
                if (allowOne) {
                    allowOne = false;
                    return super.add(e);
                }
                return false;
            }

            @Override
            public boolean addAll(final Collection<? extends Integer> c) {
                boolean changed = false;
                for (final Integer e : c) {
                    changed |= add(e);
                }
                return changed;
            }
        });
        assertTrue(rejecting.put("k", 1)); // first add allowed
        assertThrows(IllegalStateException.class, () -> rejecting.replaceValues("k", Arrays.asList(2, 3)));
        assertFalse(rejecting.containsKey("k"));
        assertNull(rejecting.get("k"));
    }

    @Test
    public void testReplaceValues_partialFailureRestoresPreviousValues() {
        final Multimap<String, Integer, java.util.NavigableSet<Integer>> mm = CommonUtil.newMultimap(HashMap::new, TreeSet::new);
        mm.putValues("k", Arrays.asList(1, 2));

        final IllegalStateException ex = assertThrows(IllegalStateException.class, () -> mm.replaceValues("k", Arrays.asList(3, null)));

        assertTrue(ex.getCause() instanceof NullPointerException);
        assertEquals(new TreeSet<>(Arrays.asList(1, 2)), mm.get("k"));
    }
}

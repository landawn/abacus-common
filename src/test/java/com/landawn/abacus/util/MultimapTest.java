package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
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
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiPredicate;
import java.util.function.Predicate;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.util.If.OrElse;
import com.landawn.abacus.util.u.Optional;

public class MultimapTest extends MultimapTestSupport {

    @Test
    public void test_01() {
        Multimap<String, Integer, List<Integer>> map = CommonUtil.newListMultimap();
        map.put("a", 1);
        map.put("a", 2);
        map.put("a", 3);

        map.put("b", 4);
        map.put("b", 5);
        map.put("b", 6);

        map.put("c", 7);
        map.put("c", 8);
        map.put("c", 9);

        List<Integer> list = map.get("a");

        assertEquals(CommonUtil.toList(1, 2, 3), list);
        assertEquals(CommonUtil.asSet("a", "b", "c"), map.keySet());
        assertEquals(9, map.totalValueCount());
        assertFalse(map.isEmpty());

        assertTrue(map.containsKey("a"));
        assertTrue(map.containsValue(5));

        assertFalse(map.containsKey("e"));
        assertFalse(map.containsValue(0));

        Multimap<String, Integer, Set<Integer>> map2 = CommonUtil.newSetMultimap();
        map2.put("a", 11);
        map2.put("a", 12);
        map2.put("a", 13);

        map2.put("b", 15);

        map2.put("d", 20);
        map.putValues(map2);
        assertTrue(map.get("a").containsAll(CommonUtil.toList(1, 2, 3, 11, 12, 13)));
        assertTrue(map.containsKey("d"));
        assertNotEquals(0, map.hashCode());
        assertFalse(map.equals(null));
        assertTrue(map.toString().contains("a"));

        map.removeAll("a");
        assertNull(map.get("a"));

        map.clear();

        assertEquals(0, map.totalValueCount());
    }

    @Test
    public void test_02() {
        Map<String, Integer> m = CommonUtil.asMap("abc", 123, "abc", 123, "abc", 456, "a", 1, "b", 2);
        Multimap<String, Integer, List<Integer>> multimap2 = ListMultimap.fromMap(m);
        assertEquals(CommonUtil.asSet("abc", "a", "b"), multimap2.keySet());
        assertEquals(CommonUtil.toList(456), multimap2.get("abc"));
        assertEquals(CommonUtil.toList(1), multimap2.get("a"));
        assertEquals(CommonUtil.toList(2), multimap2.get("b"));
    }

    // accept

    @Test
    public void testAccept() {
        Multimap<String, Integer, List<Integer>> mm = getTestMultimap();
        AtomicBoolean accepted = new AtomicBoolean(false);
        mm.accept(m -> accepted.set(true));
        assertTrue(accepted.get());
    }

    @Test
    public void testAccept_ExecutesAction() throws Exception {
        listMultimap.put("key1", 10);

        AtomicInteger count = new AtomicInteger(0);
        listMultimap.accept(m -> count.set(m.totalValueCount()));
        assertEquals(1, count.get());
    }

    @Test
    public void testAccept_NullAction() {
        assertThrows(Exception.class, () -> listMultimap.accept(null));
    }

    // acceptIfNotEmpty

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
    public void testAcceptIfNotEmpty_EmptyMultimap() throws Exception {
        AtomicInteger count = new AtomicInteger(0);
        OrElse orElse = listMultimap.acceptIfNotEmpty(m -> count.incrementAndGet());

        assertEquals(0, count.get());
        assertNotNull(orElse);
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
    public void testAcceptIfNotEmpty_WithOrElse() throws Exception {
        AtomicInteger count = new AtomicInteger(0);
        OrElse orElse = listMultimap.acceptIfNotEmpty(m -> count.set(1));
        orElse.orElse(() -> count.set(2));
        assertEquals(2, count.get());
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
    public void testAllExceptionScenarios() {
        assertDoesNotThrow(() -> {
            //        try {
            //            multimap.forEachKeyValue((BiConsumer) null);
            //            fail("Should throw IllegalArgumentException");
            //        } catch (IllegalArgumentException e) {
            //        }

            //    try {
            //        multimap.flatForEachValue(null);
            //        fail("Should throw IllegalArgumentException");
            //    } catch (IllegalArgumentException e) {
            //    }
        });
    }

    // apply

    @Test
    public void testApply() {
        Multimap<String, Integer, List<Integer>> mm = getTestMultimap();
        mm.put("a", 1);
        Integer result = mm.apply(m -> m.totalValueCount() + m.totalValueCount());
        assertEquals(Integer.valueOf(1 + 1), result);
    }

    @Test
    public void testApply_NullFunction() {
        assertThrows(Exception.class, () -> listMultimap.apply(null));
    }

    @Test
    public void testApply_ReturnsResult() throws Exception {
        listMultimap.put("key1", 10);

        Integer result = listMultimap.apply(m -> m.totalValueCount());
        assertEquals(1, result);
    }

    // applyIfNotEmpty

    @Test
    public void testApplyIfNotEmpty() {
        Multimap<String, Integer, List<Integer>> mmEmpty = getTestMultimap();
        Multimap<String, Integer, List<Integer>> mmNonEmpty = getTestMultimap();
        mmNonEmpty.put("a", 1);

        Optional<Integer> emptyResult = mmEmpty.applyIfNotEmpty(m -> m.totalValueCount());
        assertFalse(emptyResult.isPresent());

        Optional<Integer> nonEmptyResult = mmNonEmpty.applyIfNotEmpty(m -> m.totalValueCount());
        assertTrue(nonEmptyResult.isPresent());
        assertEquals(Integer.valueOf(1), nonEmptyResult.get());
    }

    @Test
    public void testApplyIfNotEmpty_EmptyMultimap() throws Exception {
        Optional<Integer> result = listMultimap.applyIfNotEmpty(m -> m.totalValueCount());
        assertFalse(result.isPresent());
    }

    @Test
    public void testApplyIfNotEmpty_WithValues() throws Exception {
        listMultimap.put("key1", 10);

        Optional<Integer> result = listMultimap.applyIfNotEmpty(m -> m.totalValueCount());
        assertTrue(result.isPresent());
        assertEquals(1, result.get());
    }

    @Test
    public void testBoundaryConditions() {
        multimap.put("", 0);
        assertTrue(multimap.containsEntry("", 0));

        multimap.put(null, 1);
        assertTrue(multimap.containsEntry(null, 1));

        assertTrue(multimap.removeEntry(null, 1));
        assertFalse(multimap.containsKey(null));
    }

    @Test
    public void testComprehensiveScenario() {
        Multimap<String, Object, List<Object>> complexMap = CommonUtil.newListMultimap();

        complexMap.put("numbers", 1);
        complexMap.put("numbers", 2.5);
        complexMap.put("numbers", 3L);

        complexMap.put("strings", "hello");
        complexMap.put("strings", "world");

        complexMap.put("mixed", 42);
        complexMap.put("mixed", "forty-two");
        complexMap.put("mixed", true);

        assertEquals(8, complexMap.totalValueCount());

        Multimap<String, Object, List<Object>> copy = complexMap.copy();
        copy.removeAll("strings");
        assertTrue(complexMap.containsKey("strings"));
        assertFalse(copy.containsKey("strings"));

        complexMap.compute("computed", (k, v) -> {
            if (v == null) {
                return Arrays.asList("computed", "value");
            }
            return v;
        });

        assertTrue(complexMap.containsKey("computed"));
        assertEquals(2, complexMap.get("computed").size());
    }

    @Test
    public void testConditionalMutatorsRejectNullPredicatesForEmptyMultimap() {
        final Multimap<String, Integer, List<Integer>> empty = CommonUtil.newListMultimap();
        final Predicate<String> keyPredicate = null;
        final BiPredicate<String, List<Integer>> entryPredicate = null;

        assertThrows(IllegalArgumentException.class, () -> empty.removeEntriesIf(keyPredicate, 1));
        assertThrows(IllegalArgumentException.class, () -> empty.removeEntriesIf(entryPredicate, 1));
        assertThrows(IllegalArgumentException.class, () -> empty.removeValuesIf(keyPredicate, Collections.emptyList()));
        assertThrows(IllegalArgumentException.class, () -> empty.removeValuesIf(entryPredicate, Collections.emptyList()));
        assertThrows(IllegalArgumentException.class, () -> empty.removeKeysIf(keyPredicate));
        assertThrows(IllegalArgumentException.class, () -> empty.removeKeysIf(entryPredicate));
        assertThrows(IllegalArgumentException.class, () -> empty.replaceEntriesIf(keyPredicate, 1, 2));
        assertThrows(IllegalArgumentException.class, () -> empty.replaceEntriesIf(entryPredicate, 1, 2));
        assertThrows(IllegalArgumentException.class, () -> empty.replaceValuesIf(keyPredicate, Collections.emptyList()));
        assertThrows(IllegalArgumentException.class, () -> empty.replaceValuesIf(entryPredicate, Collections.emptyList()));
    }

    @Test
    public void testCustomCollectionBehavior() {
        Multimap<String, Integer, List<Integer>> customMultimap = CommonUtil.newMultimap(HashMap::new, () -> new ArrayList<Integer>() {
            @Override
            public boolean add(Integer e) {
                if (e != null && e > 100) {
                    return super.add(e * 2);
                }
                return super.add(e);
            }
        });

        customMultimap.put("key", 50);
        customMultimap.put("key", 150);

        List<Integer> values = customMultimap.get("key");
        assertEquals(Integer.valueOf(50), values.get(0));
        assertEquals(Integer.valueOf(300), values.get(1));
    }

    // equals

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
    public void testEquals_DifferentMultimaps() {
        listMultimap.put("key1", 10);

        ListMultimap<String, Integer> other = CommonUtil.newListMultimap();
        other.put("key1", 20);

        assertFalse(listMultimap.equals(other));
    }

    @Test
    public void testEquals_DifferentType() {
        assertFalse(listMultimap.equals("not a multimap"));
    }

    @Test
    public void testEquals_EmptyMultimaps() {
        ListMultimap<String, Integer> other = CommonUtil.newListMultimap();
        assertTrue(listMultimap.equals(other));
        SetMultimap<String, Integer> emptySetMultimap = CommonUtil.newSetMultimap();
        assertTrue(listMultimap.equals(emptySetMultimap));
        assertTrue(emptySetMultimap.equals(listMultimap));
        assertEquals(listMultimap.hashCode(), emptySetMultimap.hashCode());
    }

    @Test
    public void testEquals_EqualMultimaps() {
        listMultimap.put("key1", 10);
        listMultimap.put("key2", 20);

        ListMultimap<String, Integer> other = CommonUtil.newListMultimap();
        other.put("key1", 10);
        other.put("key2", 20);

        assertTrue(listMultimap.equals(other));
    }

    @Test
    public void testEquals_Null() {
        assertFalse(listMultimap.equals(null));
    }

    @Test
    public void testEquals_SameInstance() {
        assertTrue(listMultimap.equals(listMultimap));
    }

    @Test
    public void testEquals_WithSetMultimap() {
        listMultimap.put("key1", 10);
        SetMultimap<String, Integer> setMm = CommonUtil.newSetMultimap();
        setMm.put("key1", 10);
        assertFalse(listMultimap.equals(setMm));
        assertFalse(setMm.equals(listMultimap));
    }

    // hashCode

    @Test
    public void testCustomEqualsHashCode() {
        class CustomKey {
            private final String value;

            CustomKey(String value) {
                this.value = value;
            }

            @Override
            public boolean equals(Object obj) {
                if (obj instanceof CustomKey) {
                    return value.equalsIgnoreCase(((CustomKey) obj).value);
                }
                return false;
            }

            @Override
            public int hashCode() {
                return value.toLowerCase().hashCode();
            }
        }

        Multimap<CustomKey, Integer, List<Integer>> customMultimap = CommonUtil.newListMultimap();
        CustomKey key1 = new CustomKey("TEST");
        CustomKey key2 = new CustomKey("test");

        customMultimap.put(key1, 1);
        customMultimap.put(key2, 2);

        assertEquals(2, customMultimap.totalValueCount());
        assertEquals(2, customMultimap.get(key1).size());
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
    public void testHashCode_ConsistentWithEquals() {
        listMultimap.put("key1", 10);
        ListMultimap<String, Integer> other1 = CommonUtil.newListMultimap();
        other1.put("key1", 10);
        ListMultimap<String, Integer> other2 = CommonUtil.newListMultimap();
        other2.put("key1", 20);

        assertEquals(listMultimap.hashCode(), other1.hashCode());
        assertTrue(listMultimap.equals(other1));
        assertFalse(listMultimap.equals(other2));
    }

    @Test
    public void testHashCode_EmptyMultimaps() {
        ListMultimap<String, Integer> other = CommonUtil.newListMultimap();
        assertEquals(listMultimap.hashCode(), other.hashCode());
    }

    @Test
    public void testHashCode_EqualMultimaps() {
        listMultimap.put("key1", 10);

        ListMultimap<String, Integer> other = CommonUtil.newListMultimap();
        other.put("key1", 10);

        assertEquals(listMultimap.hashCode(), other.hashCode());
    }

    @Test
    public void testHashCodeAndEquals() {
        Multimap<String, Integer, List<Integer>> mm1 = CommonUtil.newListMultimap();
        mm1.putValues("a", Arrays.asList(1, 2));
        mm1.put("b", 3);

        Multimap<String, Integer, List<Integer>> mm2 = CommonUtil.newListMultimap();
        mm2.put("b", 3);
        mm2.putValues("a", Arrays.asList(1, 2));

        assertEquals(mm1.hashCode(), mm2.hashCode());
        assertTrue(mm1.equals(mm2));
        assertTrue(mm2.equals(mm1));
        assertTrue(mm1.equals(mm1));

        Multimap<String, Integer, List<Integer>> mm3 = CommonUtil.newListMultimap();
        mm3.putValues("a", Arrays.asList(1, 2));
        assertNotEquals(mm1.hashCode(), mm3.hashCode());
        assertFalse(mm1.equals(mm3));

        Multimap<String, Integer, List<Integer>> mm4 = CommonUtil.newListMultimap();
        mm4.putValues("a", Arrays.asList(1, 2, 3));
        mm4.put("b", 3);
        assertNotEquals(mm1.hashCode(), mm4.hashCode());
        assertFalse(mm1.equals(mm4));

        assertFalse(mm1.equals(null));
        assertFalse(mm1.equals(new Object()));

        Multimap<String, Integer, Set<Integer>> smm1 = CommonUtil.newSetMultimap();
        smm1.putValues("a", new HashSet<>(Arrays.asList(1, 2)));
        smm1.put("b", 3);

        Multimap<String, Integer, Set<Integer>> smm2 = CommonUtil.newSetMultimap();
        smm2.put("b", 3);
        smm2.putValues("a", new HashSet<>(Arrays.asList(2, 1)));

        assertEquals(smm1.hashCode(), smm2.hashCode());
        assertTrue(smm1.equals(smm2));

        assertFalse(mm1.equals(smm1));
    }

    @Test
    public void testHashCodeFollowsTheBackingMapRuleNotAnUnconditionalGuarantee() {
        // hashCode() delegates to the backing map, so the equal-implies-same-hash guarantee only holds between
        // multimaps whose backing maps share the same key equivalence - exactly what equals(Object) warns about.
        final String key = new String("k");
        final List<Integer> values = new ArrayList<>(Arrays.asList(1, 2));
        final Map<String, List<Integer>> hashBacked = new HashMap<>();
        hashBacked.put(key, values);
        final Map<String, List<Integer>> identityBacked = new java.util.IdentityHashMap<>();
        identityBacked.put(key, values);

        final ListMultimap<String, Integer> a = ListMultimap.wrap(hashBacked);
        final ListMultimap<String, Integer> b = ListMultimap.wrap(identityBacked);

        // mutually equal ...
        assertTrue(a.equals(b));
        assertTrue(b.equals(a));
        // ... yet hashed by two different rules
        assertEquals(key.hashCode() ^ values.hashCode(), a.hashCode());
        assertEquals(System.identityHashCode(key) ^ System.identityHashCode(values), b.hashCode());

        // compatible backing maps keep the guarantee the javadoc does make
        final ListMultimap<String, Integer> c = CommonUtil.newListMultimap();
        c.putValues("k", Arrays.asList(1, 2));
        assertTrue(a.equals(c));
        assertEquals(a.hashCode(), c.hashCode());

        // "same key equivalence" is NOT enough: these two share one backing-map class and one comparator instance,
        // both implement Map#hashCode() exactly as specified, and they are mutually equal - yet they hash apart,
        // because AbstractMap.hashCode() is built from key.hashCode() while the comparator decides equality.
        final ListMultimap<String, Integer> upperKey = new ListMultimap<>(() -> new TreeMap<>(String.CASE_INSENSITIVE_ORDER), ArrayList::new);
        upperKey.put("A", 1);
        final ListMultimap<String, Integer> lowerKey = new ListMultimap<>(() -> new TreeMap<>(String.CASE_INSENSITIVE_ORDER), ArrayList::new);
        lowerKey.put("a", 1);

        assertTrue(upperKey.equals(lowerKey));
        assertTrue(lowerKey.equals(upperKey));
        assertEquals(97, upperKey.hashCode());
        assertEquals(65, lowerKey.hashCode());

        // the same hole on the value-collection side: these value sets honour Set#hashCode() exactly (88 and 120)
        // and still decide membership by a comparator rather than equals
        final SetMultimap<String, String> upperValue = new SetMultimap<>(HashMap::new, () -> new TreeSet<>(String.CASE_INSENSITIVE_ORDER));
        upperValue.put("k", "X");
        final SetMultimap<String, String> lowerValue = new SetMultimap<>(HashMap::new, () -> new TreeSet<>(String.CASE_INSENSITIVE_ORDER));
        lowerValue.put("k", "x");

        assertTrue(upperValue.equals(lowerValue));
        assertTrue(lowerValue.equals(upperValue));
        assertEquals(51, upperValue.hashCode());
        assertEquals(19, lowerValue.hashCode());
    }

    @Test
    public void testIntegrationScenario1() {
        assertDoesNotThrow(() -> {
            List<String> words = Arrays.asList("apple", "apricot", "banana", "berry", "cherry", "apple");

            Multimap<Character, String, List<String>> grouped = CommonUtil.newListMultimap();
            for (String word : words) {
                grouped.put(word.charAt(0), word);
            }

            grouped.replaceAll((k, v) -> {
                List<String> upper = new ArrayList<>();
                for (String s : v) {
                    upper.add(s.toUpperCase());
                }
                return upper;
            });

        });
    }

    @Test
    public void testIntegrationScenario3() {
        Multimap<String, Long, List<Long>> eventTimestamps = CommonUtil.newListMultimap();

        long baseTime = System.currentTimeMillis();
        eventTimestamps.putValues("login", Arrays.asList(baseTime, baseTime + 1000, baseTime + 2000));
        eventTimestamps.putValues("logout", Arrays.asList(baseTime + 3000, baseTime + 4000));
        eventTimestamps.putValues("error", Arrays.asList(baseTime + 1500));

        List<Long> loginTimes = eventTimestamps.get("login");
        List<Long> logoutTimes = eventTimestamps.get("logout");

        eventTimestamps.merge("session_duration", loginTimes, (old, count) -> {
            List<Long> durations = new ArrayList<>();
            for (int i = 0; i < Math.min(loginTimes.size(), logoutTimes.size()); i++) {
                durations.add(logoutTimes.get(i) - loginTimes.get(i));
            }
            return durations;
        });

        assertTrue(eventTimestamps.containsKey("session_duration"));
        assertEquals(3, eventTimestamps.get("session_duration").size());

        eventTimestamps.merge("session_duration", loginTimes, (old, count) -> {
            List<Long> durations = new ArrayList<>();
            for (int i = 0; i < Math.min(loginTimes.size(), logoutTimes.size()); i++) {
                durations.add(logoutTimes.get(i) - loginTimes.get(i));
            }
            return durations;
        });

        assertTrue(eventTimestamps.containsKey("session_duration"));
        assertEquals(2, eventTimestamps.get("session_duration").size());
    }

    @Test
    public void testInternalMapSupplier() {
        Multimap<String, Integer, List<Integer>> treeMultimap = CommonUtil.newMultimap(TreeMap::new, ArrayList::new);

        treeMultimap.put("c", 3);
        treeMultimap.put("a", 1);
        treeMultimap.put("b", 2);

        Iterator<String> keyIter = treeMultimap.keySet().iterator();
        assertEquals("a", keyIter.next());
        assertEquals("b", keyIter.next());
        assertEquals("c", keyIter.next());
    }

    @Test
    public void testLargeCollections() {
        for (int i = 0; i < 1000; i++) {
            listMultimap.put("key" + (i % 10), i);
        }

        assertEquals(1000, listMultimap.totalValueCount());

        //        Multimap<String, Integer, ? extends Collection<Integer>> filtered = listMultimap.filter((k, v) -> k.equals("key0"));
        //        assertEquals(1, filtered.size());
        //        assertEquals(100, filtered.totalCountOfValues());
    }

    @Test
    public void testLargeDataset() {
        int numKeys = 1000;
        int valuesPerKey = 100;

        for (int i = 0; i < numKeys; i++) {
            for (int j = 0; j < valuesPerKey; j++) {
                multimap.put("key" + i, i * valuesPerKey + j);
            }
        }

        assertEquals(numKeys * valuesPerKey, multimap.totalValueCount());

    }

    @Test
    public void testListBehavior() {
        multimap.put("key", 1);
        multimap.put("key", 1);
        multimap.put("key", 1);

        assertEquals(3, multimap.get("key").size());
    }

    @Test
    public void testMemoryEfficiency() {
        for (int i = 0; i < 100; i++) {
            multimap.put("key" + i, i);
        }

        for (int i = 0; i < 100; i++) {
            multimap.removeEntry("key" + i, i);
        }

        assertEquals(0, multimap.totalValueCount());
        assertTrue(multimap.isEmpty());
    }

    @Test
    public void testNullHandling() {
        assertFalse(listMultimap.containsKey(null));
        assertFalse(listMultimap.containsValue(null));
        assertNull(listMultimap.get(null));
        assertFalse(listMultimap.containsEntry(null, 10));
        assertFalse(listMultimap.containsEntry("key", null));

        assertFalse(listMultimap.putValues("key", null));
        assertFalse(listMultimap.removeValues("key", null));
    }

    @Test
    public void testNullValues() {
        multimap.put("key", null);
        assertTrue(multimap.containsEntry("key", null));
        assertEquals(1, multimap.get("key").size());
    }

    @Test
    public void testPerformanceCharacteristics() {
        int operations = 10000;

        long startTime = System.currentTimeMillis();

        for (int i = 0; i < operations; i++) {
            multimap.put("key" + (i % 100), i);
        }

        for (int i = 0; i < operations; i++) {
            multimap.get("key" + (i % 100));
        }

        for (int i = 0; i < operations / 2; i++) {
            multimap.removeEntry("key" + (i % 100), i);
        }

        long endTime = System.currentTimeMillis();

        assertTrue(endTime - startTime < 5000, "Operations took too long: " + (endTime - startTime) + "ms");
    }

    @Test
    public void testSetBehavior() {
        setMultimap.put("key", "value");
        setMultimap.put("key", "value");
        setMultimap.put("key", "value");

        assertEquals(1, setMultimap.get("key").size());
    }

    @Test
    public void testTypeSafety() {
        Multimap<Integer, String, Set<String>> typedMultimap = CommonUtil.newMultimap(HashMap::new, HashSet::new);

        typedMultimap.put(1, "one");
        typedMultimap.put(2, "two");

        Set<String> values = typedMultimap.get(1);
        assertTrue(values instanceof Set);

        Multimap<String, Object, List<Object>> objectMultimap = CommonUtil.newListMultimap();
        objectMultimap.put("mixed", "string");
        objectMultimap.put("mixed", 123);
        objectMultimap.put("mixed", true);

        List<Object> mixedValues = objectMultimap.get("mixed");
        assertEquals(3, mixedValues.size());
        assertEquals("string", mixedValues.get(0));
        assertEquals(123, mixedValues.get(1));
        assertEquals(true, mixedValues.get(2));
    }
}

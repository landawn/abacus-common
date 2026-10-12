package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

/** Performance and memory regression contracts for SetMultimap. */
@Tag("slow-test")
public class SetMultimapPerformanceMemoryTest extends TestBase {

    // ---- perf review 2026-10-08 G064 end ----
    // ---- perf review 2026-10-08 G065 begin ----
    @Nested
    class G065 {
        private Set<String> identityMembershipSet(final String... elements) {
            final Set<String> set = Collections.newSetFromMap(new java.util.IdentityHashMap<>());
            Collections.addAll(set, elements);
            return set;
        }

        private TreeSet<String> sortedSet(final java.util.Comparator<String> comparator, final String... elements) {
            final TreeSet<String> set = new TreeSet<>(comparator);
            Collections.addAll(set, elements);
            return set;
        }

        // G065-01: sorted and identity value sets (copied without the identity index) keep their comparator / identity membership,
        // their iteration order and their independence; emptied, null and equals-based values beside them are unchanged, and the
        // value supplier is never called.
        @Test
        void testToImmutableMap_sortedAndIdentityValueSetsWithoutSupplier() {
            final String first = new String("x");
            final String second = new String("x");
            final IdentityHashSet<String> identity = new IdentityHashSet<>();
            identity.add(first);
            identity.add(second);
            final TreeSet<String> caseInsensitive = sortedSet(String.CASE_INSENSITIVE_ORDER, "b", "A", "c");
            final TreeSet<String> reversed = sortedSet(java.util.Comparator.reverseOrder(), "x", "z", "y");
            final java.util.concurrent.ConcurrentSkipListSet<String> skipList = new java.util.concurrent.ConcurrentSkipListSet<>(String.CASE_INSENSITIVE_ORDER);
            skipList.add("Q");
            skipList.add("p");
            final java.util.NavigableSet<String> descending = sortedSet(null, "m", "n").descendingSet();

            final Map<String, Set<String>> backing = new LinkedHashMap<>();
            backing.put("sorted", caseInsensitive);
            backing.put("identity", identity);
            backing.put("plain", new HashSet<>(Arrays.asList("k")));
            backing.put("reversed", reversed);
            backing.put("skipList", skipList);
            backing.put("descending", descending);

            final AtomicInteger supplierCalls = new AtomicInteger();
            final SetMultimap<String, String> multimap = SetMultimap.wrap(backing, () -> {
                supplierCalls.incrementAndGet();
                return new HashSet<>();
            });
            // reachable after wrapping: a key whose sorted set was emptied in place, and a null value in the wrapped map
            final TreeSet<String> emptied = sortedSet(String.CASE_INSENSITIVE_ORDER, "gone");
            backing.put("emptied", emptied);
            emptied.clear();
            backing.put("null", null);

            final List<ImmutableMap<String, ImmutableSet<String>>> snapshots = List.of(multimap.toImmutableMap(), multimap.toImmutableMap(LinkedHashMap::new),
                    multimap.toImmutableMap(size -> new TreeMap<>()));

            // the sources change afterwards; no snapshot may follow them
            caseInsensitive.add("D");
            identity.clear();
            reversed.clear();
            skipList.add("r");
            descending.add("o");

            assertEquals(0, supplierCalls.get());

            for (final ImmutableMap<String, ImmutableSet<String>> snapshot : snapshots) {
                assertEquals(Set.of("sorted", "identity", "plain", "reversed", "skipList", "descending", "emptied", "null"), new HashSet<>(snapshot.keySet()));

                final ImmutableSet<String> sorted = snapshot.get("sorted");
                assertEquals(Arrays.asList("A", "b", "c"), new ArrayList<>(sorted));
                assertTrue(sorted.contains("a"));
                assertTrue(sorted.contains("B"));
                assertFalse(sorted.contains("d"));

                final ImmutableSet<String> identityCopy = snapshot.get("identity");
                assertEquals(2, identityCopy.size());
                assertTrue(identityCopy.contains(first));
                assertTrue(identityCopy.contains(second));
                assertFalse(identityCopy.contains(new String("x")));

                assertEquals(Arrays.asList("k"), new ArrayList<>(snapshot.get("plain")));
                assertEquals(Arrays.asList("z", "y", "x"), new ArrayList<>(snapshot.get("reversed")));
                assertEquals(Arrays.asList("p", "Q"), new ArrayList<>(snapshot.get("skipList")));
                assertTrue(snapshot.get("skipList").contains("q"));
                assertEquals(Arrays.asList("n", "m"), new ArrayList<>(snapshot.get("descending")));
                assertTrue(snapshot.get("emptied").isEmpty());
                assertTrue(snapshot.get("null").isEmpty());

                assertThrows(UnsupportedOperationException.class, () -> sorted.add("e"));
                assertThrows(UnsupportedOperationException.class, () -> identityCopy.remove(first));
                assertThrows(UnsupportedOperationException.class, () -> snapshot.remove("sorted"));
            }

            // every snapshot owns its own copies, and keeps the key order of the map it was built in
            assertNotSame(snapshots.get(0).get("sorted"), snapshots.get(1).get("sorted"));
            assertNotSame(snapshots.get(0).get("identity"), snapshots.get(1).get("identity"));
            assertEquals(Arrays.asList("sorted", "identity", "plain", "reversed", "skipList", "descending", "emptied", "null"),
                    new ArrayList<>(snapshots.get(0).keySet()));
            assertEquals(new ArrayList<>(snapshots.get(0).keySet()), new ArrayList<>(snapshots.get(1).keySet()));
            assertEquals(Arrays.asList("descending", "emptied", "identity", "null", "plain", "reversed", "skipList", "sorted"),
                    new ArrayList<>(snapshots.get(2).keySet()));
        }

        // G065-01: the common sorted configuration (TreeMap keys, TreeSet values) snapshots in key and value order, independently.
        @Test
        void testToImmutableMap_treeMapTreeSetMultimap() {
            final SetMultimap<String, Integer> multimap = CommonUtil.newSetMultimap(TreeMap.class, TreeSet.class);
            for (int i = 0; i < 50; i++) {
                multimap.put("k" + (i % 7), 100 - i);
            }

            for (final ImmutableMap<String, ImmutableSet<Integer>> snapshot : List.of(multimap.toImmutableMap(), multimap.toImmutableMap(LinkedHashMap::new))) {
                assertEquals(new ArrayList<>(multimap.keySet()), new ArrayList<>(snapshot.keySet()));
                for (final String key : multimap.keySet()) {
                    assertEquals(new ArrayList<>(multimap.get(key)), new ArrayList<>(snapshot.get(key)));
                }
                multimap.put("k0", -1);
                assertFalse(snapshot.get("k0").contains(-1));
                multimap.removeEntry("k0", -1);
            }
        }

        // G065-01: sorted and identity sets before and after sets that the equals-based copy shrinks. The identity index is now
        // built at the first shrinking set rather than the first sorted set; the value supplier is still called once per
        // shrinking set only, and every set keeps its membership rule.
        @Test
        void testToImmutableMap_sortedSetsMixedWithShrinkingSets() {
            final String first = new String("x");
            final String second = new String("x");
            final String third = new String("y");
            final String fourth = new String("y");
            final Map<String, Set<String>> backing = new LinkedHashMap<>();
            backing.put("t1", sortedSet(String.CASE_INSENSITIVE_ORDER, "b", "A"));
            backing.put("u1", identityMembershipSet(first, second));
            backing.put("t2", sortedSet(java.util.Comparator.reverseOrder(), "c", "d"));
            backing.put("u2", identityMembershipSet(third, fourth));
            backing.put("i1", new IdentityHashSet<>(Arrays.asList(first, second)));

            final AtomicInteger supplierCalls = new AtomicInteger();
            final SetMultimap<String, String> multimap = SetMultimap.wrap(backing, () -> {
                supplierCalls.incrementAndGet();
                return identityMembershipSet();
            });

            for (final ImmutableMap<String, ImmutableSet<String>> snapshot : List.of(multimap.toImmutableMap(), multimap.toImmutableMap(LinkedHashMap::new))) {
                assertEquals(Arrays.asList("t1", "u1", "t2", "u2", "i1"), new ArrayList<>(snapshot.keySet()));
                assertEquals(Arrays.asList("A", "b"), new ArrayList<>(snapshot.get("t1")));
                assertTrue(snapshot.get("t1").contains("a"));
                assertEquals(Arrays.asList("d", "c"), new ArrayList<>(snapshot.get("t2")));
                for (final String key : Arrays.asList("u1", "i1")) {
                    assertEquals(2, snapshot.get(key).size());
                    assertTrue(snapshot.get(key).contains(first));
                    assertTrue(snapshot.get(key).contains(second));
                    assertFalse(snapshot.get(key).contains(new String("x")));
                }
                assertEquals(2, snapshot.get("u2").size());
                assertTrue(snapshot.get("u2").contains(third));
                assertTrue(snapshot.get("u2").contains(fourth));
                assertNotSame(backing.get("u1"), snapshot.get("u1"));
            }

            // u1 and u2, for each of the two snapshots
            assertEquals(4, supplierCalls.get());
            assertEquals(2, backing.get("u1").size());
            assertEquals(2, backing.get("u2").size());
        }

        // G065-01: with sorted sets ahead of the shrinking sets, a value supplier that hands back a source set (sorted, emptied
        // sorted, or the set being copied), the same set twice, or a non-empty set is still rejected with the same exception
        // and message, and so is a supplied set that merges the distinct elements; no source is modified.
        @Test
        void testToImmutableMap_sortedSetsThenRejectedSupplierResults() {
            final TreeSet<String> sortedSource = sortedSet(String.CASE_INSENSITIVE_ORDER, "a");
            final TreeSet<String> emptiedSortedSource = sortedSet(null, "gone");
            final Set<String> shrinking = identityMembershipSet(new String("x"), new String("x"));
            final Set<String> shrinking2 = identityMembershipSet(new String("y"), new String("y"));

            final Map<String, Set<String>> backing = new LinkedHashMap<>();
            backing.put("t1", sortedSource);
            backing.put("t2", emptiedSortedSource);
            backing.put("u1", shrinking);
            backing.put("u2", shrinking2);

            final Set<String> reused = identityMembershipSet();
            final SetMultimap<String, String> returnsSortedSource = SetMultimap.wrap(backing, () -> sortedSource);
            final SetMultimap<String, String> returnsEmptiedSortedSource = SetMultimap.wrap(backing, () -> emptiedSortedSource);
            final SetMultimap<String, String> returnsShrinkingSource = SetMultimap.wrap(backing, () -> shrinking);
            final SetMultimap<String, String> returnsSameSetTwice = SetMultimap.wrap(backing, () -> reused);
            final SetMultimap<String, String> returnsNonEmpty = SetMultimap.wrap(backing, () -> identityMembershipSet("z"));
            // the first value is a TreeSet, so new value sets are natural-order TreeSets, which merge the equal elements of u1
            final SetMultimap<String, String> mergingSupplier = SetMultimap.wrap(backing);
            emptiedSortedSource.clear();

            final String inUse = "The value supplier returned a collection already in use; it must return a new empty collection on every call";
            final String nonEmpty = "The value supplier returned a non-empty collection; it must return a new empty collection on every call";

            for (final SetMultimap<String, String> multimap : List.of(returnsSortedSource, returnsEmptiedSortedSource, returnsShrinkingSource,
                    returnsSameSetTwice)) {
                reused.clear();
                assertEquals(inUse, assertThrows(IllegalArgumentException.class, multimap::toImmutableMap).getMessage());
                reused.clear();
                assertEquals(inUse, assertThrows(IllegalArgumentException.class, () -> multimap.toImmutableMap(LinkedHashMap::new)).getMessage());
            }

            assertEquals(nonEmpty, assertThrows(IllegalArgumentException.class, returnsNonEmpty::toImmutableMap).getMessage());
            assertEquals(nonEmpty, assertThrows(IllegalArgumentException.class, () -> returnsNonEmpty.toImmutableMap(LinkedHashMap::new)).getMessage());

            assertEquals("Set copy loses distinct source elements", assertThrows(IllegalArgumentException.class, mergingSupplier::toImmutableMap).getMessage());

            assertEquals(Arrays.asList("a"), new ArrayList<>(sortedSource));
            assertTrue(emptiedSortedSource.isEmpty());
            assertEquals(2, shrinking.size());
            assertEquals(2, shrinking2.size());
        }
    }
}

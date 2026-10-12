package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

@Tag("unit")
class SetMultimapImmutableSnapshotDeepReviewTest extends TestBase {
    @Test
    void immutableConversionsPreserveDistinctEqualReferences() {
        final String first = new String("equal");
        final String second = new String("equal");
        final Set<String> values = new IdentityHashSet<>();
        values.add(first);
        values.add(second);
        final Map<String, Set<String>> backing = new LinkedHashMap<>();
        backing.put("key", values);
        final SetMultimap<String, String> multimap = SetMultimap.wrap(backing);

        final List<ImmutableMap<String, ImmutableSet<String>>> snapshots = List.of(multimap.toImmutableMap(), multimap.toImmutableMap(LinkedHashMap::new));

        values.clear();
        for (final ImmutableMap<String, ImmutableSet<String>> snapshot : snapshots) {
            final ImmutableSet<String> copied = snapshot.get("key");
            assertEquals(2, copied.size());
            assertTrue(copied.contains(first));
            assertTrue(copied.contains(second));
            assertFalse(copied.contains(new String("equal")));
            assertThrows(UnsupportedOperationException.class, () -> copied.remove(first));
            assertThrows(UnsupportedOperationException.class, () -> snapshot.remove("key"));
        }
    }

    @Test
    void immutableConversionsKeepSortedMembershipRules() {
        final Set<String> values = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        values.add("Alpha");
        final Map<String, Set<String>> backing = new LinkedHashMap<>();
        backing.put("key", values);
        final SetMultimap<String, String> multimap = SetMultimap.wrap(backing);

        final List<ImmutableMap<String, ImmutableSet<String>>> snapshots = List.of(multimap.toImmutableMap(), multimap.toImmutableMap(LinkedHashMap::new));

        values.add("Beta");
        for (final ImmutableMap<String, ImmutableSet<String>> snapshot : snapshots) {
            final ImmutableSet<String> copied = snapshot.get("key");
            assertTrue(copied.contains("alpha"));
            assertFalse(copied.contains("beta"));
            assertEquals(List.of("Alpha"), new ArrayList<>(copied));
            assertThrows(UnsupportedOperationException.class, () -> copied.add("Gamma"));
        }
    }

    @Test
    void immutableConversionsKeepComparatorDistinctEqualValues() {
        final EqualValue first = new EqualValue(1);
        final EqualValue second = new EqualValue(2);
        final Set<EqualValue> values = new TreeSet<>(Comparator.comparingInt(value -> value.order));
        values.add(second);
        values.add(first);
        final Map<String, Set<EqualValue>> backing = new LinkedHashMap<>();
        backing.put("key", values);
        final SetMultimap<String, EqualValue> multimap = SetMultimap.wrap(backing);

        final List<ImmutableMap<String, ImmutableSet<EqualValue>>> snapshots = List.of(multimap.toImmutableMap(), multimap.toImmutableMap(LinkedHashMap::new));

        values.clear();
        for (final ImmutableMap<String, ImmutableSet<EqualValue>> snapshot : snapshots) {
            final ImmutableSet<EqualValue> copied = snapshot.get("key");
            final List<EqualValue> ordered = new ArrayList<>(copied);
            assertEquals(2, ordered.size());
            assertSame(first, ordered.get(0));
            assertSame(second, ordered.get(1));
            assertFalse(copied.contains(new EqualValue(3)));
            assertThrows(UnsupportedOperationException.class, copied::clear);
        }
    }

    private static final class EqualValue {
        private final int order;

        EqualValue(final int order) {
            this.order = order;
        }

        @Override
        public boolean equals(final Object obj) {
            return obj instanceof EqualValue;
        }

        @Override
        public int hashCode() {
            return 1;
        }
    }

    // ---- bug review 2026-10-02 final G130 begin ----

    // toImmutableMap of plain HashSet values keeps each set's own iteration order (a re-hashed HashSet copy could reorder it).
    @Test
    void testToImmutableMap_hashSetValuesKeepIterationOrder() {
        final SetMultimap<String, Integer> multimap = CommonUtil.newSetMultimap();
        for (int i = 0; i < 100; i++) {
            multimap.put("key", i * 31);
        }
        for (int i = 5; i < 100; i++) {
            multimap.removeEntry("key", i * 31);
        }
        final List<Integer> liveOrder = new ArrayList<>(multimap.get("key"));

        for (final ImmutableMap<String, ImmutableSet<Integer>> snapshot : List.of(multimap.toImmutableMap(), multimap.toImmutableMap(LinkedHashMap::new))) {
            assertEquals(liveOrder, new ArrayList<>(snapshot.get("key")));
        }
    }

    // toImmutableMap of an ordered value set of a type the multimap cannot recreate keeps its order and never calls the value supplier.
    @Test
    void testToImmutableMap_wrappedOrderedSetKeepsOrderWithoutValueSupplier() {
        final Map<String, Set<Integer>> backing = new LinkedHashMap<>();
        backing.put("key", java.util.Collections.unmodifiableSet(new java.util.LinkedHashSet<>(List.of(3, 1, 2))));
        final SetMultimap<String, Integer> wrapped = SetMultimap.wrap(backing);

        final int[] supplierCalls = new int[1];
        final SetMultimap<String, Integer> keySetValued = CommonUtil.newSetMultimap(LinkedHashMap::new, () -> {
            supplierCalls[0]++;
            return java.util.concurrent.ConcurrentHashMap.newKeySet();
        });
        keySetValued.put("key", 1);
        keySetValued.put("other", 2);
        supplierCalls[0] = 0;

        for (final ImmutableMap<String, ImmutableSet<Integer>> snapshot : List.of(wrapped.toImmutableMap(), wrapped.toImmutableMap(LinkedHashMap::new))) {
            assertEquals(List.of(3, 1, 2), new ArrayList<>(snapshot.get("key")));
        }

        for (final ImmutableMap<String, ImmutableSet<Integer>> snapshot : List.of(keySetValued.toImmutableMap(),
                keySetValued.toImmutableMap(LinkedHashMap::new))) {
            assertEquals(Set.of(1), snapshot.get("key"));
            assertFalse(snapshot.get("key").contains(null));
        }
        assertEquals(0, supplierCalls[0]);
    }

    // toImmutableMap of an unknown set type that holds equal elements apart: copied through the value supplier when it can keep them,
    // rejected when it cannot (the old equals-based copy silently dropped one).
    @Test
    void testToImmutableMap_unknownSetHoldingEqualElementsApart() {
        final String first = new String("equal");
        final String second = new String("equal");

        final SetMultimap<String, String> identityValued = CommonUtil.newSetMultimap(LinkedHashMap::new,
                () -> java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<>()));
        identityValued.put("key", first);
        identityValued.put("key", second);

        final List<ImmutableMap<String, ImmutableSet<String>>> snapshots = List.of(identityValued.toImmutableMap(),
                identityValued.toImmutableMap(LinkedHashMap::new));
        identityValued.get("key").clear();

        for (final ImmutableMap<String, ImmutableSet<String>> snapshot : snapshots) {
            final ImmutableSet<String> copied = snapshot.get("key");
            assertEquals(2, copied.size());
            assertTrue(copied.contains(first));
            assertTrue(copied.contains(second));
            assertFalse(copied.contains(new String("equal")));
        }

        final Set<String> identitySet = java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<>());
        identitySet.add(first);
        identitySet.add(second);
        final Map<String, Set<String>> backing = new LinkedHashMap<>();
        backing.put("key", identitySet);
        final SetMultimap<String, String> wrapped = SetMultimap.wrap(backing);

        assertThrows(IllegalArgumentException.class, wrapped::toImmutableMap);
        assertThrows(IllegalArgumentException.class, () -> wrapped.toImmutableMap(LinkedHashMap::new));
        assertEquals(2, identitySet.size());
    }

    // ---- bug review 2026-10-02 final G130 end ----
}

package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

/**
 * Regression tests for the 2026-08-31 review of Multiset / Multimap / ListMultimap / SetMultimap / Array.
 * Each nested class corresponds to one finding; the test names carry the finding id.
 *
 * <p>Extends {@link TestBase} (as every test class in this project must): that is where {@code @Tag("unit")}
 * comes from, and {@code AbacusCoreTestSuite} selects on {@code @IncludeTags("unit")} - without it these tests
 * pass in isolation but are silently skipped by the suite.
 */
public class CollectionsRegressionATest extends TestBase {

    // ------------------------------------------------------------------------------------------------
    // B1: Multiset(Iterable) / Multiset.create(Iterable) sized the backing map from the TOTAL occurrence
    //     count instead of the number of distinct elements, so copying a multiset with large counts
    //     allocated a map proportional to those counts (and could exhaust the heap).
    // ------------------------------------------------------------------------------------------------
    @Nested
    public class B1_MultisetCopySizing extends TestBase {

        @Test
        public void copyConstructor_largeCountsFewDistinct_doesNotBlowUpAndCopiesCounts() {
            final Multiset<String> src = new Multiset<>();
            src.add("a", 40_000_000);
            src.add("b", 3);

            // Before the fix this allocated a HashMap sized for size()/2 == 20,000,001 entries.
            final Multiset<String> copy = new Multiset<>(src);

            assertEquals(2, copy.countOfDistinctElements());
            assertEquals(40_000_000, copy.getCount("a"));
            assertEquals(3, copy.getCount("b"));
            assertEquals(src, copy);
            assertEquals(src.hashCode(), copy.hashCode());
        }

        @Test
        public void createIterable_onMultiset_copiesCountsDirectly() {
            final Multiset<String> src = new Multiset<>();
            src.add("z", 5_000_000);

            final Multiset<String> copy = Multiset.create(src);

            assertEquals(1, copy.countOfDistinctElements());
            assertEquals(5_000_000, copy.getCount("z"));
        }

        @Test
        public void copy_isIndependentOfTheSource() {
            final Multiset<String> src = Multiset.of("a", "a", "b");
            final Multiset<String> copy = new Multiset<>(src);

            copy.add("a", 5);
            copy.add("c");

            assertEquals(2, src.getCount("a"));
            assertEquals(0, src.getCount("c"));
            assertEquals(7, copy.getCount("a"));
            assertEquals(1, copy.getCount("c"));
        }

        @Test
        public void copy_ofEmptyMultiset_isEmpty() {
            assertTrue(new Multiset<>(new Multiset<String>()).isEmpty());
            assertTrue(Multiset.create(new Multiset<String>()).isEmpty());
        }

        @Test
        public void copy_preservesNullElementAndItsCount() {
            final Multiset<String> src = new Multiset<>();
            src.add(null, 4);
            src.add("x", 1);

            final Multiset<String> copy = new Multiset<>(src);

            assertEquals(4, copy.getCount(null));
            assertEquals(1, copy.getCount("x"));
            assertEquals(src, copy);
        }

        @Test
        public void nonMultisetIterablesStillBehaveAsBefore() {
            final Multiset<String> fromList = new Multiset<>(Arrays.asList("a", "b", "a"));
            assertEquals(2, fromList.getCount("a"));
            assertEquals(1, fromList.getCount("b"));
            assertEquals(2, fromList.countOfDistinctElements());

            assertEquals(1, new Multiset<>(CommonUtil.asSet("a", "b")).getCount("a"));
            assertTrue(new Multiset<>((Iterable<String>) null).isEmpty());

            // A plain (non-Collection) Iterable still goes through the per-element path.
            final Iterable<String> plain = () -> Arrays.asList("q", "q", "r").iterator();
            final Multiset<String> fromPlain = new Multiset<>(plain);
            assertEquals(2, fromPlain.getCount("q"));
            assertEquals(1, fromPlain.getCount("r"));
        }

        @Test
        public void createIterator_stillCountsCorrectly() {
            // create(Iterator) now delegates to add(e) instead of duplicating the increment/overflow logic.
            final Multiset<String> ms = Multiset.create(Arrays.asList("a", "b", "a").iterator());
            assertEquals(2, ms.getCount("a"));
            assertEquals(1, ms.getCount("b"));
            assertEquals(3, ms.size());

            assertTrue(Multiset.create((java.util.Iterator<String>) null).isEmpty());
            assertTrue(Multiset.create(Collections.<String> emptyIterator()).isEmpty());
        }

        @Test
        public void createIterator_sharesAddsOverflowGuard() {
            // The guard create(Iterator) used to carry inline now lives only in add(E, int); an iterator
            // cannot practically be driven to Integer.MAX_VALUE, so pin the shared guard directly.
            final Multiset<String> ms = new Multiset<>();
            ms.add("k", Integer.MAX_VALUE);

            final IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> ms.add("k"));
            assertTrue(ex.getMessage().contains("out of the bound of int"), ex.getMessage());
            assertEquals(Integer.MAX_VALUE, ms.getCount("k"));
        }
    }

    // ------------------------------------------------------------------------------------------------
    // Multiset: containsAll fast path, entrySet read-only contract, argument-name diagnostics.
    // ------------------------------------------------------------------------------------------------
    @Nested
    public class MultisetMisc extends TestBase {

        /**
         * Like the copy fix, this one is about cost rather than result: {@code containsAll} answered
         * correctly before, it just walked one iteration per OCCURRENCE of the argument. The assertions
         * here therefore pin behaviour only; the cost bound is deliberately not provoked in-suite for the
         * reason given at the top of {@code B1_MultisetCopySizing}.
         */
        @Test
        public void containsAll_withAMultisetArgumentStillAnswersCorrectly() {
            final Multiset<String> ms = new Multiset<>();
            ms.add("a", 10);
            ms.add("b", 10);

            final Multiset<String> probe = new Multiset<>();
            probe.add("a", 5_000_000);

            assertTrue(ms.containsAll(probe));

            probe.add("zzz", 1);
            assertFalse(ms.containsAll(probe));
        }

        @Test
        public void containsAll_stillHandlesPlainCollectionsAndEmpty() {
            final Multiset<String> ms = Multiset.of("a", "b", "b");
            assertTrue(ms.containsAll(Arrays.asList("a", "b")));
            assertFalse(ms.containsAll(Arrays.asList("a", "c")));
            assertTrue(ms.containsAll(new ArrayList<>()));
            assertTrue(ms.containsAll(null));
        }

        @Test
        public void entrySet_rejectsEveryMutationThatWouldActuallyChangeIt() {
            final Multiset<String> ms = Multiset.of("a", "a", "b");
            final Set<Multiset.Entry<String>> entries = ms.entrySet();
            final Multiset.Entry<String> first = entries.iterator().next();

            assertThrows(UnsupportedOperationException.class, () -> entries.add(first));
            assertThrows(UnsupportedOperationException.class, () -> entries.remove(first));
            assertThrows(UnsupportedOperationException.class, () -> entries.removeAll(List.of(first)));
            assertThrows(UnsupportedOperationException.class, () -> entries.retainAll(List.of()));
            assertThrows(UnsupportedOperationException.class, () -> entries.removeIf(e -> true));
            assertThrows(UnsupportedOperationException.class, entries::clear);
            assertThrows(UnsupportedOperationException.class, () -> {
                final java.util.Iterator<Multiset.Entry<String>> it = entries.iterator();
                it.next();
                it.remove();
            });

            // The multiset itself is untouched by every failed attempt.
            assertEquals(2, ms.getCount("a"));
            assertEquals(1, ms.getCount("b"));
        }

        @Test
        public void entrySet_noOpMutationsReportFalseRatherThanThrowing() {
            // Documented explicitly because the blanket claim "they all throw" is NOT true: as with any
            // optional Collection operation, a call that removes nothing need not throw.
            final Multiset<String> ms = Multiset.of("a", "a", "b");
            final Set<Multiset.Entry<String>> entries = ms.entrySet();

            assertFalse(entries.removeAll(List.of()));
            assertFalse(entries.retainAll(new ArrayList<>(entries)));
            assertFalse(entries.removeIf(e -> false));

            assertEquals(2, ms.getCount("a"));
            assertEquals(1, ms.getCount("b"));
        }

        @Test
        public void entrySet_entriesAreSnapshotsWhoseCountNeverChanges() {
            final Multiset<String> ms = Multiset.of("x", "x");
            final Multiset.Entry<String> snapshot = ms.entrySet().iterator().next();
            assertEquals(2, snapshot.count());

            ms.add("x", 5);
            assertEquals(2, snapshot.count(), "the entry is a snapshot, not a live view");
            assertEquals(7, ms.getCount("x"));

            ms.removeAllOccurrencesOf("x");
            assertEquals(2, snapshot.count(), "a snapshot count never becomes zero");
            assertEquals(0, ms.getCount("x"));
        }

        @Test
        public void entrySet_containsIsStillHonoured() {
            final Multiset<String> ms = Multiset.of("a", "a", "b");
            final Multiset<String> other = Multiset.of("b", "a", "a");
            assertTrue(ms.entrySet().containsAll(other.entrySet()));
        }

        @Test
        public void negativeArgumentMessagesNameTheActualParameter() {
            final Multiset<String> ms = new Multiset<>();

            assertEquals("The specified 'occurrencesToAdd' cannot be negative: -1",
                    assertThrows(IllegalArgumentException.class, () -> ms.add("k", -1)).getMessage());
            assertEquals("The specified 'occurrencesToRemove' cannot be negative: -2",
                    assertThrows(IllegalArgumentException.class, () -> ms.remove("k", -2)).getMessage());
            assertEquals("The specified 'occurrences' cannot be negative: -3",
                    assertThrows(IllegalArgumentException.class, () -> ms.setCount("k", -3)).getMessage());
            assertEquals("The specified 'oldOccurrences' cannot be negative: -4",
                    assertThrows(IllegalArgumentException.class, () -> ms.setCount("k", -4, 1)).getMessage());
            assertEquals("The specified 'newOccurrences' cannot be negative: -5",
                    assertThrows(IllegalArgumentException.class, () -> ms.setCount("k", 1, -5)).getMessage());
            assertEquals("The specified 'value' cannot be negative: -6",
                    assertThrows(IllegalArgumentException.class, () -> ms.merge("k", -6, (a, b) -> a + b)).getMessage());
            assertEquals("The specified 'occurrencesToRemove' cannot be negative: -7",
                    assertThrows(IllegalArgumentException.class, () -> ms.removeOccurrences(Arrays.asList("k"), -7)).getMessage());
            assertEquals("The specified 'occurrencesToAdd' cannot be negative: -8",
                    assertThrows(IllegalArgumentException.class, () -> ms.addAll(Arrays.asList("k"), -8)).getMessage());
            assertEquals("The specified 'occurrences' cannot be negative: -9",
                    assertThrows(IllegalArgumentException.class, () -> ms.addAndGetCount("k", -9)).getMessage());
            assertEquals("The specified 'occurrences' cannot be negative: -10",
                    assertThrows(IllegalArgumentException.class, () -> ms.removeAndGetCount("k", -10)).getMessage());

            // Nothing was mutated by any rejected call.
            assertTrue(ms.isEmpty());
        }

        @Test
        public void equalsAcceptsAnyMultisetAndStaysConsistentWithHashCode() {
            final Multiset<String> a = Multiset.of("a", "a", "b");
            final Multiset<String> b = new Multiset<>(new LinkedHashMap<String, MutableInt>());
            b.add("b");
            b.add("a", 2);

            assertEquals(a, b);
            assertEquals(a.hashCode(), b.hashCode());
            assertFalse(a.equals("not a multiset"));
        }
    }

    // ------------------------------------------------------------------------------------------------
    // B2: copy() dropped a key whose value collection had been emptied through a live view, so a copy
    //     was not equal to its source. copy() now reproduces the backing map faithfully.
    // ------------------------------------------------------------------------------------------------
    @Nested
    public class B2_MultimapCopyFidelity extends TestBase {

        @Test
        public void listMultimap_copyEqualsSource_evenAfterAValueListWasEmptiedThroughGet() {
            final ListMultimap<String, Integer> m = ListMultimap.of("a", 1, "b", 2);
            m.get("a").clear();

            final ListMultimap<String, Integer> copy = m.copy();

            assertEquals(m, copy);
            assertEquals(m.hashCode(), copy.hashCode());
            assertTrue(copy.containsKey("a"));
            assertNotNull(copy.get("a"));
            assertTrue(copy.get("a").isEmpty());
            assertEquals(2, copy.keyCount());
            assertEquals(1, copy.totalValueCount());
        }

        @Test
        public void setMultimap_copyEqualsSource_evenAfterAValueSetWasEmptiedThroughGet() {
            final SetMultimap<String, Integer> m = SetMultimap.of("a", 1, "b", 2);
            m.get("a").clear();

            final SetMultimap<String, Integer> copy = m.copy();

            assertEquals(m, copy);
            assertEquals(2, copy.keyCount());
            assertTrue(copy.get("a").isEmpty());
        }

        @Test
        public void baseMultimap_copyEqualsSource_evenAfterAValueCollectionWasEmptied() {
            // N.newMultimap(Supplier, Supplier) yields a genuine base Multimap, so this exercises
            // Multimap.copy() itself rather than a ListMultimap/SetMultimap override - and with a value
            // collection type that is neither a List nor a Set.
            final Multimap<String, Integer, java.util.ArrayDeque<Integer>> m = CommonUtil.newMultimap(LinkedHashMap::new, java.util.ArrayDeque::new);
            m.putValues("a", Arrays.asList(1, 2));
            m.putValues("b", Arrays.asList(3));
            assertEquals(Multimap.class, m.getClass());

            m.get("a").clear();

            final Multimap<String, Integer, java.util.ArrayDeque<Integer>> copy = m.copy();

            assertEquals(Multimap.class, copy.getClass());
            assertEquals(2, copy.keyCount());
            assertEquals(1, copy.totalValueCount());
            assertTrue(copy.containsKey("a"));
            assertTrue(copy.get("a").isEmpty());
            assertEquals(Arrays.asList(3), new ArrayList<>(copy.get("b")));
            assertTrue(copy.get("b") instanceof java.util.ArrayDeque, "the copy must use this multimap's value supplier");

            // ArrayDeque has no value-based equals, so compare structurally rather than via Multimap.equals.
            copy.put("b", 4);
            assertEquals(Arrays.asList(3), new ArrayList<>(m.get("b")), "the copy must be independent");
        }

        @Test
        public void copy_isStillADeepEnoughCopy_valueCollectionsAreNotShared() {
            final ListMultimap<String, Integer> m = ListMultimap.of("a", 1, "b", 2);
            final ListMultimap<String, Integer> copy = m.copy();

            copy.put("a", 99);
            copy.put("c", 3);

            assertEquals(Arrays.asList(1), m.get("a"));
            assertNull(m.get("c"));
            assertEquals(Arrays.asList(1, 99), copy.get("a"));
        }

        @Test
        public void copy_preservesBackingMapOrderingAndValueCollectionType() {
            final SetMultimap<String, Integer> m = CommonUtil.newSetMultimap(LinkedHashMap.class, LinkedHashSet.class);
            m.putValues("z", Arrays.asList(3, 1));
            m.putValues("a", Arrays.asList(2));

            final SetMultimap<String, Integer> copy = m.copy();

            assertEquals(Arrays.asList("z", "a"), new ArrayList<>(copy.keySet()));
            assertEquals(Arrays.asList(3, 1), new ArrayList<>(copy.get("z")));
            assertTrue(copy.get("z") instanceof LinkedHashSet);
        }

        @Test
        public void copy_ofEmptyMultimapIsEmpty() {
            assertTrue(CommonUtil.newListMultimap().copy().isEmpty());
            assertTrue(CommonUtil.newSetMultimap().copy().isEmpty());
        }

        @Test
        public void putValues_stillSkipsEmptySourceCollections() {
            // copy() no longer routes through putValues, but putValues itself must keep its documented
            // "an empty value collection contributes nothing" behaviour.
            final ListMultimap<String, Integer> src = ListMultimap.of("a", 1, "b", 2);
            src.get("a").clear();

            final ListMultimap<String, Integer> target = CommonUtil.newListMultimap();
            assertTrue(target.putValues(src));

            assertFalse(target.containsKey("a"));
            assertEquals(Arrays.asList(2), target.get("b"));
        }
    }

    // ------------------------------------------------------------------------------------------------
    // The key lifecycle that Multimap.get(Object) now documents: mutating a live value collection leaves
    // a key mapped to an empty collection, and these are the consequences that are documented.
    // ------------------------------------------------------------------------------------------------
    @Nested
    public class DocumentedKeyLifecycle extends TestBase {

        private ListMultimap<String, Integer> emptiedThroughGet() {
            final ListMultimap<String, Integer> m = ListMultimap.of("a", 1, "b", 2);
            m.get("a").clear();
            return m;
        }

        @Test
        public void anEmptiedKeyStaysPresent() {
            final ListMultimap<String, Integer> m = emptiedThroughGet();

            assertTrue(m.containsKey("a"));
            assertNotNull(m.get("a"));
            assertTrue(m.get("a").isEmpty());
            assertFalse(m.isEmpty());
        }

        @Test
        public void keyCountAndTotalValueCountDisagreeForAnEmptiedKey() {
            final ListMultimap<String, Integer> m = emptiedThroughGet();

            assertEquals(2, m.keyCount());
            assertEquals(1, m.totalValueCount());
            assertEquals(Arrays.asList(2), new ArrayList<>(m.allValues()));
        }

        @Test
        public void toMultisetOmitsAnEmptiedKeyBecauseAZeroCountIsNotRepresentable() {
            final ListMultimap<String, Integer> m = emptiedThroughGet();
            final Multiset<String> ms = m.toMultiset();

            assertFalse(ms.contains("a"));
            assertEquals(0, ms.getCount("a"));
            assertEquals(1, ms.getCount("b"));
        }

        @Test
        public void everyLiveAccessorDocumentedOnGetBypassesTheLifecycleTheSameWay() {
            // get()'s javadoc says the same applies to getOrDefault / valueCollections / stream.
            final ListMultimap<String, Integer> viaGetOrDefault = ListMultimap.of("a", 1);
            viaGetOrDefault.getOrDefault("a", new ArrayList<>()).clear();
            assertTrue(viaGetOrDefault.containsKey("a"));
            assertTrue(viaGetOrDefault.get("a").isEmpty());

            final ListMultimap<String, Integer> viaValueCollections = ListMultimap.of("a", 1);
            viaValueCollections.valueCollections().iterator().next().clear();
            assertTrue(viaValueCollections.containsKey("a"));
            assertTrue(viaValueCollections.get("a").isEmpty());

            final ListMultimap<String, Integer> viaStream = ListMultimap.of("a", 1);
            viaStream.stream().forEach(e -> e.getValue().clear());
            assertTrue(viaStream.containsKey("a"));
            assertTrue(viaStream.get("a").isEmpty());
        }

        @Test
        public void aWrappedBackingMapMutatedExternallyIsStillCopiedFaithfully() {
            // The class javadoc calls out that a wrap factory keeps using a map the caller may still hold.
            final Map<String, List<Integer>> backing = new HashMap<>();
            backing.put("a", new ArrayList<>(List.of(1)));
            final ListMultimap<String, Integer> mm = ListMultimap.wrap(backing);

            backing.get("a").clear();
            backing.put("zz", new ArrayList<>());

            assertEquals(2, mm.keyCount());
            assertEquals(0, mm.totalValueCount());

            final ListMultimap<String, Integer> copy = mm.copy();
            assertEquals(mm, copy);
            assertTrue(copy.containsKey("a"));
            assertTrue(copy.containsKey("zz"));
        }

        @Test
        public void iteratorRemoveDropsTheWholeMapping() {
            final ListMultimap<String, Integer> m = ListMultimap.of("a", 1, "a", 2, "b", 3);
            final java.util.Iterator<Map.Entry<String, List<Integer>>> it = m.iterator();

            while (it.hasNext()) {
                if ("a".equals(it.next().getKey())) {
                    it.remove();
                }
            }

            assertFalse(m.containsKey("a"));
            assertEquals(1, m.keyCount());
            assertEquals(Arrays.asList(3), m.get("b"));
        }

        @Test
        public void iterationInheritsTheBackingMapsConcurrentModificationBehaviour() {
            // iterator() hands back the backing map's own entry-set iterator, so this is the backing map's
            // property, not the Multimap's - a fail-fast map throws, a weakly consistent one does not.
            for (final Class<?> failFast : new Class<?>[] { HashMap.class, LinkedHashMap.class, TreeMap.class }) {
                @SuppressWarnings({ "unchecked", "rawtypes" })
                final ListMultimap<String, Integer> m = CommonUtil.newListMultimap((Class) failFast, ArrayList.class);
                m.put("a", 1);
                m.put("b", 2);

                assertThrows(java.util.ConcurrentModificationException.class, () -> {
                    for (final Map.Entry<String, List<Integer>> e : m) {
                        m.put("c" + e.getKey(), 9);
                    }
                }, failFast.getSimpleName() + " backing should be fail-fast");
            }

            @SuppressWarnings({ "unchecked", "rawtypes" })
            final ListMultimap<String, Integer> weaklyConsistent = CommonUtil.newListMultimap((Class) java.util.concurrent.ConcurrentHashMap.class,
                    ArrayList.class);
            weaklyConsistent.put("a", 1);
            weaklyConsistent.put("b", 2);

            // Must NOT throw - this is why the javadoc does not promise fail-fast unconditionally.
            for (final Map.Entry<String, List<Integer>> e : weaklyConsistent) {
                weaklyConsistent.put("c" + e.getKey(), 9);
            }
            assertTrue(weaklyConsistent.keyCount() >= 4);
        }
    }

    // ------------------------------------------------------------------------------------------------
    // J4: ListMultimap.getFirst/getFirstOrDefault cannot distinguish "absent" from "first value is null".
    // ------------------------------------------------------------------------------------------------
    @Nested
    public class J4_GetFirstNullSemantics extends TestBase {

        @Test
        public void getFirstReturnsNullForAnAbsentKeyAndForAStoredNull() {
            final ListMultimap<String, Integer> m = CommonUtil.newListMultimap();
            m.put("n", null);
            m.put("v", 7);

            assertNull(m.getFirst("absent"));
            assertNull(m.getFirst("n"));
            assertEquals(Integer.valueOf(7), m.getFirst("v"));

            // containsKey is what actually distinguishes the two null cases.
            assertFalse(m.containsKey("absent"));
            assertTrue(m.containsKey("n"));
        }

        @Test
        public void getFirstOrDefaultDoesNotSubstituteTheDefaultForAStoredNull() {
            final ListMultimap<String, Integer> m = CommonUtil.newListMultimap();
            m.put("n", null);

            assertEquals(Integer.valueOf(99), m.getFirstOrDefault("absent", 99));
            assertNull(m.getFirstOrDefault("n", 99), "a stored null is a value, not an absence");
        }

        @Test
        public void getFirstOnAKeyEmptiedThroughGetFallsBackToTheDefault() {
            final ListMultimap<String, Integer> m = ListMultimap.of("a", 1);
            m.get("a").clear();

            assertNull(m.getFirst("a"));
            assertEquals(Integer.valueOf(99), m.getFirstOrDefault("a", 99));
        }
    }

    // ------------------------------------------------------------------------------------------------
    // B3: SetMultimap.wrap(Map) rejected value sets whose runtime type cannot be instantiated, while
    //     ListMultimap.wrap(Map) fell back to ArrayList for newly created keys.
    // ------------------------------------------------------------------------------------------------
    @Nested
    public class B3_SetMultimapWrapFallback extends TestBase {

        @Test
        public void wrap_acceptsImmutableAndViewValueSets() {
            for (final Set<Integer> value : List.of(Set.of(1, 2), Collections.singleton(1), Collections.unmodifiableSet(new HashSet<>(List.of(1))))) {
                final Map<String, Set<Integer>> map = new HashMap<>();
                map.put("a", value);

                final SetMultimap<String, Integer> mm = SetMultimap.wrap(map);

                assertTrue(mm.containsKey("a"));
                assertEquals(value, mm.get("a"));
            }
        }

        @Test
        public void wrap_newKeysUseHashSetWhenTheExistingTypeCannotBeInstantiated() {
            final Map<String, Set<Integer>> map = new HashMap<>();
            map.put("a", Set.of(1, 2));

            final SetMultimap<String, Integer> mm = SetMultimap.wrap(map);
            assertTrue(mm.put("b", 3));

            assertEquals(Set.of(3), mm.get("b"));
            assertTrue(mm.get("b") instanceof HashSet);
            assertTrue(map.containsKey("b"), "the wrap must still write through to the backing map");
        }

        @Test
        public void wrap_existingImmutableSetsKeepTheirRestrictions() {
            final Map<String, Set<Integer>> map = new HashMap<>();
            map.put("a", Set.of(1, 2));

            final SetMultimap<String, Integer> mm = SetMultimap.wrap(map);

            assertThrows(UnsupportedOperationException.class, () -> mm.put("a", 3));
        }

        @Test
        public void wrap_stillPreservesAnInstantiableValueTypeForNewKeys() {
            final Map<String, Set<Integer>> map = new HashMap<>();
            map.put("a", new TreeSet<>(List.of(1, 2)));

            final SetMultimap<String, Integer> mm = SetMultimap.wrap(map);
            mm.put("b", 5);

            assertTrue(mm.get("b") instanceof TreeSet);
        }

        @Test
        public void wrap_stillRejectsNullAndEmptyValues() {
            final Map<String, Set<Integer>> withEmpty = new HashMap<>();
            withEmpty.put("x", new HashSet<>());
            assertThrows(IllegalArgumentException.class, () -> SetMultimap.wrap(withEmpty));

            final Map<String, Set<Integer>> withNull = new HashMap<>();
            withNull.put("x", null);
            assertThrows(IllegalArgumentException.class, () -> SetMultimap.wrap(withNull));
        }

        @Test
        public void wrappedImmutableSets_copyToAnEqualButMutableMultimap() {
            // Interaction between the wrap fallback and the faithful copy(): a wrapped multimap whose values
            // are immutable must still copy to something equal AND writable.
            final Map<String, Set<Integer>> backing = new LinkedHashMap<>();
            backing.put("a", Set.of(1, 2));
            backing.put("b", Collections.singleton(3));

            final SetMultimap<String, Integer> mm = SetMultimap.wrap(backing);
            final SetMultimap<String, Integer> copy = mm.copy();

            assertEquals(mm, copy);
            assertTrue(copy.get("a") instanceof HashSet);
            assertTrue(copy.put("a", 9));
            assertEquals(Set.of(1, 2, 9), copy.get("a"));
            assertEquals(Set.of(1, 2), mm.get("a"), "the wrapped source must be untouched");
        }

        @Test
        public void wrap_ofAnEmptyMapIsAllowedAndUsesHashSet() {
            final Map<String, Set<Integer>> map = new HashMap<>();
            final SetMultimap<String, Integer> mm = SetMultimap.wrap(map);

            assertTrue(mm.isEmpty());
            mm.put("a", 1);
            assertTrue(mm.get("a") instanceof HashSet);
        }
    }

    // ------------------------------------------------------------------------------------------------
    // B6 / B7: null-argument diagnostics.
    // ------------------------------------------------------------------------------------------------
    @Nested
    public class B6B7_NullDiagnostics extends TestBase {

        @Test
        public void setMultimap_toImmutableMap_rejectsANullSupplierResultLikeItsListSibling() {
            final SetMultimap<String, Integer> s = SetMultimap.of("a", 1);
            assertEquals("mapSupplier returned null", assertThrows(IllegalArgumentException.class, () -> s.toImmutableMap(size -> null)).getMessage());

            final ListMultimap<String, Integer> l = ListMultimap.of("a", 1);
            assertEquals("mapSupplier returned null", assertThrows(IllegalArgumentException.class, () -> l.toImmutableMap(size -> null)).getMessage());
        }

        @Test
        public void setMultimap_toImmutableMap_rejectsANullSupplierResultForAnEmptyMultimap() {
            // The empty case never reaches map.put(...), so it used to slip a null into ImmutableMap.wrap.
            final SetMultimap<String, Integer> s = CommonUtil.newSetMultimap();
            assertThrows(IllegalArgumentException.class, () -> s.toImmutableMap(size -> null));
        }

        @Test
        public void wrap_nullMapMessageNamesTheArgument() {
            assertEquals("'map' cannot be null",
                    assertThrows(IllegalArgumentException.class, () -> ListMultimap.wrap((Map<String, List<Integer>>) null)).getMessage());
            assertEquals("'map' cannot be null",
                    assertThrows(IllegalArgumentException.class, () -> SetMultimap.wrap((Map<String, Set<Integer>>) null)).getMessage());
        }

        @Test
        public void merge_nullValueMessagesNameTheArgument() {
            final ListMultimap<String, Integer> m = ListMultimap.of("k", 1);

            assertEquals("'element' cannot be null",
                    assertThrows(IllegalArgumentException.class, () -> m.merge("k", (Integer) null, (o, n) -> o)).getMessage());
            assertEquals("'elements' cannot be null",
                    assertThrows(IllegalArgumentException.class, () -> m.merge("k", (List<Integer>) null, (o, n) -> o)).getMessage());
        }

        @Test
        public void merge_stillAcceptsAnEmptyCollectionAndPutStillAcceptsNull() {
            final ListMultimap<String, Integer> m = ListMultimap.of("k", 1);
            assertEquals(Arrays.asList(1), m.merge("k", new ArrayList<Integer>(), (o, n) -> o));

            m.put("n", null);
            assertEquals(Collections.singletonList(null), m.get("n"));
        }
    }

    // ------------------------------------------------------------------------------------------------
    // D1: forEach(BiConsumer) renamed to forEachKeyValue so it no longer collides, by lambda arity only,
    //     with Iterable.forEach(Consumer) which iterates at a different granularity.
    // ------------------------------------------------------------------------------------------------
    @Nested
    public class D1_ForEachKeyValueRename extends TestBase {

        @Test
        public void forEachKeyValue_visitsEveryKeyElementPair() {
            final ListMultimap<String, Integer> m = CommonUtil.newListMultimap();
            m.putValues("a", Arrays.asList(1, 2, 3));
            m.putValues("b", Arrays.asList(4));

            final List<String> seen = new ArrayList<>();
            m.forEachKeyValue((k, e) -> seen.add(k + "=" + e));

            assertEquals(4, seen.size());
            assertTrue(seen.containsAll(Arrays.asList("a=1", "a=2", "a=3", "b=4")));
        }

        @Test
        public void inheritedForEach_stillVisitsKeyCollectionPairs() {
            final ListMultimap<String, Integer> m = CommonUtil.newListMultimap();
            m.putValues("a", Arrays.asList(1, 2, 3));

            final List<String> seen = new ArrayList<>();
            m.forEach(entry -> seen.add(entry.getKey() + "=" + entry.getValue()));

            assertEquals(Arrays.asList("a=[1, 2, 3]"), seen);
        }

        @Test
        public void forEachKeyValue_rejectsNullAction() {
            final ListMultimap<String, Integer> m = ListMultimap.of("a", 1);
            assertThrows(IllegalArgumentException.class, () -> m.forEachKeyValue((java.util.function.BiConsumer<? super String, ? super Integer>) null));
        }

        @Test
        public void forEachKeyValue_onEmptyMultimapDoesNothing() {
            final AtomicInteger n = new AtomicInteger();
            CommonUtil.newListMultimap().forEachKeyValue((k, v) -> n.incrementAndGet());
            assertEquals(0, n.get());
        }
    }

    // ------------------------------------------------------------------------------------------------
    // putValues(Multimap) now iterates the source's entries instead of keySet() + get(key).
    // ------------------------------------------------------------------------------------------------
    @Nested
    public class PutValuesFromMultimap extends TestBase {

        @Test
        public void mergesIntoExistingKeysAndAddsNewOnes() {
            final ListMultimap<String, Integer> target = CommonUtil.newListMultimap();
            target.putValues("a", Arrays.asList(1, 2));

            final ListMultimap<String, Integer> src = CommonUtil.newListMultimap();
            src.putValues("a", Arrays.asList(3));
            src.putValues("b", Arrays.asList(4, 5));

            assertTrue(target.putValues(src));
            assertEquals(Arrays.asList(1, 2, 3), target.get("a"));
            assertEquals(Arrays.asList(4, 5), target.get("b"));
        }

        @Test
        public void worksWithASortedBackingMapAndKeepsKeyOrder() {
            final ListMultimap<String, Integer> target = CommonUtil.newListMultimap(TreeMap.class, ArrayList.class);
            final ListMultimap<String, Integer> src = CommonUtil.newListMultimap();
            src.putValues("c", Arrays.asList(3));
            src.putValues("a", Arrays.asList(1));
            src.putValues("b", Arrays.asList(2));

            assertTrue(target.putValues(src));
            assertEquals(Arrays.asList("a", "b", "c"), new ArrayList<>(target.keySet()));
        }

        @Test
        public void emptyOrNullSourceIsANoOp() {
            final ListMultimap<String, Integer> target = ListMultimap.of("a", 1);
            assertFalse(target.putValues((Multimap<String, Integer, List<Integer>>) null));
            assertFalse(target.putValues(CommonUtil.<String, Integer> newListMultimap()));
            assertEquals(Arrays.asList(1), target.get("a"));
        }

        @Test
        public void selfPutValuesStillDoublesEachValueList() {
            final ListMultimap<String, Integer> m = ListMultimap.of("a", 1, "a", 2);
            assertTrue(m.putValues(m));
            assertEquals(Arrays.asList(1, 2, 1, 2), m.get("a"));
        }

        @Test
        public void twoMultimapsSharingOneBackingMapDoNotTripConcurrentModification() {
            // putValues now iterates the SOURCE's entry set, so a source that IS this multimap's own
            // backing map must not cause a structural modification mid-iteration.
            final Map<String, List<Integer>> shared = new HashMap<>();
            shared.put("k", new ArrayList<>(List.of(1, 2)));

            final ListMultimap<String, Integer> p = ListMultimap.wrap(shared);
            final ListMultimap<String, Integer> q = ListMultimap.wrap(shared);

            assertTrue(p.putValues(q));
            assertEquals(Arrays.asList(1, 2, 1, 2), p.get("k"));
        }
    }

    // ------------------------------------------------------------------------------------------------
    // Array: J1 (set* conversion direction), B4 (component type), J2 (rangeClosed step doc).
    // ------------------------------------------------------------------------------------------------
    @Nested
    public class ArrayFixes extends TestBase {

        /** The eight {@code set*} Javadocs used to state the {@code get*} rule; this pins the real one. */
        @Test
        public void J1_setStar_widensTheValueIntoTheComponentType() {
            final long[] longs = new long[2];
            final double[] doubles = new double[2];
            final short[] shorts = new short[2];

            Array.setInt(longs, 0, 5); // int widens into long[]
            Array.setInt(doubles, 0, 5); // int widens into double[]
            assertEquals(5L, longs[0]);
            assertEquals(5.0d, doubles[0]);

            // short[] does NOT accept an int, even though short widens TO int (that is the get* rule).
            assertThrows(IllegalArgumentException.class, () -> Array.setInt(shorts, 0, 5));
        }

        @Test
        public void J1_getStar_widensTheComponentTypeIntoTheResult() {
            final short[] shorts = { 7, 8 };
            final long[] longs = { 9L };

            assertEquals(7, Array.getInt(shorts, 0)); // short widens to int
            assertThrows(IllegalArgumentException.class, () -> Array.getInt(longs, 0));
        }

        enum WithBody {
            A {
                @Override
                public String toString() {
                    return "A!";
                }
            },
            B
        }

        @Test
        public void B4_repeatNonNull_componentTypeIsTheRuntimeClassOfTheElement() {
            final Number[] widened = Array.repeatNonNull((Number) Integer.valueOf(1), 3);
            assertEquals(Integer[].class, widened.getClass());
            assertThrows(ArrayStoreException.class, () -> widened[0] = Double.valueOf(1.5));

            final WithBody[] es = Array.repeatNonNull(WithBody.A, 2);
            assertThrows(ArrayStoreException.class, () -> es[0] = WithBody.B);
        }

        @Test
        public void B4_repeatWithExplicitClass_isTheSafeForm() {
            final Number[] ok = Array.repeat(Integer.valueOf(1), 3, Number.class);
            assertEquals(Number[].class, ok.getClass());
            ok[0] = Double.valueOf(1.5); // no ArrayStoreException
            assertEquals(Double.valueOf(1.5), ok[0]);

            final WithBody[] es = Array.repeat(WithBody.A, 2, WithBody.class);
            es[0] = WithBody.B;
            assertArrayEquals(new WithBody[] { WithBody.B, WithBody.A }, es);
        }

        @Test
        public void B4_repeatWithExplicitClass_acceptsANullElement() {
            final String[] nulls = Array.repeat((String) null, 3, String.class);
            assertEquals(3, nulls.length);
            assertNull(nulls[0]);

            assertThrows(IllegalArgumentException.class, () -> Array.repeatNonNull(null, 3));
        }

        @Test
        public void J2_rangeClosedWithZeroStepThrowsEvenWhenStartEqualsEnd() {
            assertThrows(IllegalArgumentException.class, () -> Array.rangeClosed(5, 5, 0));
            assertThrows(IllegalArgumentException.class, () -> Array.rangeClosed('x', 'x', 0));
            assertThrows(IllegalArgumentException.class, () -> Array.rangeClosed((byte) 5, (byte) 5, (byte) 0));
            assertThrows(IllegalArgumentException.class, () -> Array.rangeClosed((short) 5, (short) 5, (short) 0));
            assertThrows(IllegalArgumentException.class, () -> Array.rangeClosed(5L, 5L, 0L));

            // ... but any non-zero step yields the single-element array.
            assertArrayEquals(new int[] { 5 }, Array.rangeClosed(5, 5, 7));
            assertArrayEquals(new int[] { 5 }, Array.rangeClosed(5, 5, -7));
        }

        // NOTE: the Array.newInstance(cls, 0) caching change (reported here as B5) is implemented and
        // regression-tested by the session that owns Array.java - see ArrayReviewFixes20260831Test and
        // CommonUtilTest.testNullToEmpty_TypedArray_sharesThePreSeededConstantsOnly. Not duplicated here.

        @Test
        public void concat2D_nullPolicyIsUnchangedAndDocumented() {
            assertNull(Array.concat2D((String[][]) null, null));
            assertNull(Array.concat3D((String[][][]) null, null));
            assertArrayEquals(new boolean[0][], Array.concat((boolean[][]) null, null));

            final String[][] onlyA = Array.concat2D(new String[][] { { "x" } }, null);
            assertArrayEquals(new String[][] { { "x" } }, onlyA);

            final String[][] emptyA = Array.concat2D(new String[0][], null);
            assertNotNull(emptyA);
            assertEquals(0, emptyA.length);
        }
    }
}

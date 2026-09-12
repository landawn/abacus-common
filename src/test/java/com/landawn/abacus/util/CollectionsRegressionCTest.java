package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Spliterator;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.function.ObjIntPredicate;

/**
 * Regression tests for the third review of Multiset / Multimap / ListMultimap / SetMultimap (2026-09-01).
 * Each nested class corresponds to one finding; the test names carry the finding id.
 *
 * <p>Extends {@link TestBase} (as every test class in this project must): that is where {@code @Tag("unit")}
 * comes from, and {@code AbacusCoreTestSuite} selects on {@code @IncludeTags("unit")} - without it these tests
 * pass in isolation but are silently skipped by the suite.
 */
public class CollectionsRegressionCTest extends TestBase {

    /** Two distinct String instances that are equal under equals/hashCode but distinct under identity. */
    private static String[] equalButDistinct() {
        return new String[] { new String("dup"), new String("dup") };
    }

    // ------------------------------------------------------------------------------------------------
    // B1 - removeAllOccurrencesIf(ObjIntPredicate) staged matching keys in a HashSet, which collapses two
    //      backing-map keys that are equal under equals/hashCode but distinct under the backing map's own
    //      equivalence, leaving the second one behind. Its own inline comment already said "never a Set".
    // ------------------------------------------------------------------------------------------------
    @Nested
    public class B1_KeyStagingPreservesBackingMapEquivalence {

        private Multiset<String> identityMultiset(final String a, final String b) {
            final Multiset<String> ms = new Multiset<>(IdentityHashMap.class);
            ms.add(a, 2);
            ms.add(b, 3);
            return ms;
        }

        @Test
        public void b1_objIntPredicate_removesEveryMatchingKey() {
            final String[] d = equalButDistinct();
            final Multiset<String> ms = identityMultiset(d[0], d[1]);
            assertEquals(2, ms.countOfDistinctElements());

            assertTrue(ms.removeAllOccurrencesIf((ObjIntPredicate<String>) (e, count) -> count > 0));

            assertEquals(0, ms.countOfDistinctElements());
            assertEquals(0, ms.size());
            assertTrue(ms.isEmpty());
        }

        @Test
        public void b1_objIntPredicate_removesOnlyTheMatchingKeys() {
            final String[] d = equalButDistinct();
            final Multiset<String> ms = identityMultiset(d[0], d[1]);

            // Only the count-2 instance matches; the count-3 one must survive with its own identity.
            assertTrue(ms.removeAllOccurrencesIf((ObjIntPredicate<String>) (e, count) -> count == 2));

            assertEquals(1, ms.countOfDistinctElements());
            assertEquals(3, ms.getCount(d[1]));
            assertEquals(0, ms.getCount(d[0]));
        }

        @Test
        public void b1_objIntPredicate_matchingNothingLeavesTheMultisetAlone() {
            final Multiset<String> ms = Multiset.of("a", "a", "b");

            assertFalse(ms.removeAllOccurrencesIf((ObjIntPredicate<String>) (e, count) -> false));

            assertEquals(2, ms.getCount("a"));
            assertEquals(1, ms.getCount("b"));
        }

        @Test
        public void b1_objIntPredicate_alsoWorksOnAComparatorFinerThanEquals() {
            // A TreeMap whose comparator distinguishes instances that are equal under equals: the same
            // hazard as IdentityHashMap, reached through a sorted backing instead.
            final String[] d = equalButDistinct();
            // Ordered by identity against the two known instances - NOT by System.identityHashCode, which
            // can collide and would make this test flaky.
            final Multiset<String> ms = new Multiset<>(() -> new TreeMap<String, Object>((x, y) -> x == y ? 0 : (x == d[0] ? -1 : 1)));
            ms.add(d[0], 2);
            ms.add(d[1], 3);
            assertEquals(2, ms.countOfDistinctElements());

            assertTrue(ms.removeAllOccurrencesIf((ObjIntPredicate<String>) (e, count) -> true));

            assertEquals(0, ms.countOfDistinctElements());
        }

        @Test
        public void b1_predicateOverloadWasAlreadyCorrectAndStaysCorrect() {
            final String[] d = equalButDistinct();
            final Multiset<String> ms = identityMultiset(d[0], d[1]);

            assertTrue(ms.removeAllOccurrencesIf(e -> true));

            assertEquals(0, ms.countOfDistinctElements());
        }

        @Test
        public void b1_removeIfIsEquivalentAndLazyOnNoMatch() {
            final String[] d = equalButDistinct();
            final Multiset<String> ms = identityMultiset(d[0], d[1]);

            assertFalse(ms.removeIf(e -> false));
            assertEquals(2, ms.countOfDistinctElements());

            assertTrue(ms.removeIf(e -> true));
            assertEquals(0, ms.countOfDistinctElements());
        }
    }

    // ------------------------------------------------------------------------------------------------
    // B2 - iterator()/stream() handed out the backing map's own entries, whose setValue installs a
    //      caller-supplied collection straight into the backing map. Iterator.remove() must stay.
    // ------------------------------------------------------------------------------------------------
    @Nested
    public class B2_EntryViewRefusesSetValue {

        @Test
        public void b2_iteratorEntrySetValueThrows() {
            final ListMultimap<String, Integer> mm = CommonUtil.newListMultimap();
            mm.put("k", 1);

            final Map.Entry<String, List<Integer>> entry = mm.iterator().next();

            assertThrows(UnsupportedOperationException.class, () -> entry.setValue(ImmutableList.of(9)));
            // The mapping is untouched, and the class's own mutators still work.
            assertEquals(Arrays.asList(1), mm.get("k"));
            assertTrue(mm.put("k", 2));
            assertEquals(Arrays.asList(1, 2), mm.get("k"));
        }

        @Test
        public void b2_streamEntrySetValueThrows() {
            final ListMultimap<String, Integer> mm = CommonUtil.newListMultimap();
            mm.put("k", 1);

            final Map.Entry<String, List<Integer>> entry = mm.stream().first().orElseThrow();

            assertThrows(UnsupportedOperationException.class, () -> entry.setValue(ImmutableList.of(9)));
            assertEquals(Arrays.asList(1), mm.get("k"));
        }

        @Test
        public void b2_iteratorRemoveStillRemovesTheWholeMapping() {
            final ListMultimap<String, Integer> mm = CommonUtil.newListMultimap();
            mm.putValues("a", Arrays.asList(1, 2));
            mm.put("b", 3);

            final Iterator<Map.Entry<String, List<Integer>>> iter = mm.iterator();
            iter.next();
            iter.remove();

            assertEquals(1, mm.keyCount());
            assertEquals(1, mm.totalValueCount());
        }

        @Test
        public void b2_entryStillExposesTheLiveValueCollection() {
            final ListMultimap<String, Integer> mm = CommonUtil.newListMultimap();
            mm.put("k", 1);

            final Map.Entry<String, List<Integer>> entry = mm.iterator().next();

            // Only setValue is withdrawn; the value is still the live collection this Multimap stores.
            assertSame(mm.get("k"), entry.getValue());
            entry.getValue().add(7);
            assertEquals(Arrays.asList(1, 7), mm.get("k"));
        }

        @Test
        public void b2_entryKeyValueEqualsAndHashCodeFollowTheMapEntryContract() {
            final ListMultimap<String, Integer> mm = CommonUtil.newListMultimap();
            mm.put("k", 1);

            final Map.Entry<String, List<Integer>> entry = mm.iterator().next();

            assertEquals("k", entry.getKey());
            assertEquals(Arrays.asList(1), entry.getValue());

            final Map.Entry<String, List<Integer>> plain = CommonUtil.newEntry("k", Arrays.asList(1));
            assertEquals(plain, entry);
            assertEquals(entry, plain);
            assertEquals(plain.hashCode(), entry.hashCode());
        }

        @Test
        public void b2_entrySetViewDelegatesStructuralOperations() {
            final ListMultimap<String, Integer> mm = CommonUtil.newListMultimap();
            mm.put("a", 1);
            mm.put("b", 2);

            final Set<Map.Entry<String, List<Integer>>> view = mm.entrySetView();

            assertEquals(2, view.size());
            assertFalse(view.isEmpty());
            assertTrue(view.contains(CommonUtil.newEntry("a", Arrays.asList(1))));
            assertFalse(view.contains(CommonUtil.newEntry("a", Arrays.asList(9))));

            assertTrue(view.remove(CommonUtil.newEntry("a", Arrays.asList(1))));
            assertEquals(1, mm.keyCount());
            assertNull(mm.get("a"));

            view.clear();
            assertTrue(mm.isEmpty());
        }

        @Test
        public void b2_viewIsLiveAndCachedAcrossCalls() {
            final ListMultimap<String, Integer> mm = CommonUtil.newListMultimap();
            final Set<Map.Entry<String, List<Integer>>> view = mm.entrySetView();

            assertSame(view, mm.entrySetView());
            assertTrue(view.isEmpty());

            mm.put("a", 1);
            assertEquals(1, view.size());
        }

        @Test
        public void b2_streamAndEntryStreamStillTraverseEverything() {
            final ListMultimap<String, Integer> mm = CommonUtil.newLinkedListMultimap();
            mm.putValues("a", Arrays.asList(1, 2));
            mm.put("b", 3);

            assertEquals(2, mm.stream().count());
            assertEquals(Arrays.asList("a", "b"), mm.stream().map(Map.Entry::getKey).toList());
            assertEquals(Arrays.asList(1, 2, 3), mm.entryStream().values().toList());
            assertEquals(3, mm.allValues().size());
        }

        @Test
        public void b2_setValueIsRefusedForSetMultimapToo() {
            final SetMultimap<String, Integer> mm = CommonUtil.newSetMultimap();
            mm.put("k", 1);

            final Map.Entry<String, Set<Integer>> entry = mm.iterator().next();

            assertThrows(UnsupportedOperationException.class, () -> entry.setValue(CommonUtil.newHashSet(Arrays.asList(9))));
            assertEquals(CommonUtil.newHashSet(Arrays.asList(1)), mm.get("k"));
        }
    }

    // ------------------------------------------------------------------------------------------------
    // B3 - Collection's default spliterator takes its size from size(), which saturates at
    //      Integer.MAX_VALUE; because it still reports SIZED, stream().count() returned the saturated
    //      value instead of the true total.
    // ------------------------------------------------------------------------------------------------
    @Nested
    public class B3_SpliteratorCarriesTheExactTotal {

        /** Two entries whose counts sum past Integer.MAX_VALUE, with no memory cost. */
        private Multiset<String> oversized() {
            final Multiset<String> ms = new Multiset<>();
            ms.setCount("x", Integer.MAX_VALUE);
            ms.setCount("y", 5);
            return ms;
        }

        @Test
        public void b3_estimateSizeIsExactPastIntegerMaxValue() {
            final Multiset<String> ms = oversized();

            assertEquals(Integer.MAX_VALUE, ms.size());
            assertEquals(2147483652L, ms.sumOfOccurrences());
            assertEquals(2147483652L, ms.spliterator().estimateSize());
            assertEquals(2147483652L, ms.spliterator().getExactSizeIfKnown());
        }

        @Test
        @Timeout(value = 60, unit = TimeUnit.SECONDS)
        public void b3_streamCountIsExactPastIntegerMaxValue() {
            // This is the actual bug: count() short-circuits on a SIZED spliterator, so before the
            // spliterator() override it returned the saturated size() instead of the real total.
            //
            // The @Timeout is deliberate. Short-circuiting is a documented optimisation, not a guarantee;
            // if a JDK ever stops doing it this would try to walk 2.1 billion occurrences and hang the whole
            // suite. Fail this one test instead.
            assertEquals(2147483652L, oversized().stream().count());
        }

        @Test
        public void b3_ordinarySizesAreUnaffected() {
            final Multiset<String> ms = Multiset.of("a", "a", "b");

            assertEquals(3, ms.size());
            assertEquals(3L, ms.spliterator().estimateSize());
            assertEquals(3L, ms.stream().count());
            assertEquals(Arrays.asList("a", "a", "b"), ms.stream().sorted().toList());
        }

        @Test
        public void b3_emptyMultisetSpliterator() {
            final Multiset<String> ms = new Multiset<>();

            assertEquals(0L, ms.spliterator().estimateSize());
            assertEquals(0L, ms.stream().count());
        }

        @Test
        public void b3_reportsSizedAndSubsized() {
            final Spliterator<String> sp = Multiset.of("a", "b").spliterator();

            assertTrue(sp.hasCharacteristics(Spliterator.SIZED));
            assertTrue(sp.hasCharacteristics(Spliterator.SUBSIZED));
        }

        @Test
        public void b3_isEagerBindingSoTheSizeGoesStale() {
            // The documented trade-off for the exact size: unlike Collection's late-binding default, this
            // spliterator captures the total when spliterator() is called.
            final Multiset<String> ms = Multiset.of("a");
            final Spliterator<String> sp = ms.spliterator();

            ms.add("b");

            assertEquals(1L, sp.estimateSize());
            assertEquals(2, ms.size());
        }

        @Test
        public void b3_concurrentModificationBehaviourIsTheBackingMaps() {
            // A change to the set of DISTINCT elements is structural for the backing map -> fail-fast.
            final Multiset<String> ms = Multiset.of("a", "b", "c");
            final Spliterator<String> sp = ms.spliterator();
            ms.add("d");
            assertThrows(java.util.ConcurrentModificationException.class, () -> sp.forEachRemaining(x -> {
            }));

            // A change to an existing element's COUNT is not structural, so it does not throw.
            final Multiset<String> counts = Multiset.of("a", "b");
            final Spliterator<String> countSp = counts.spliterator();
            counts.add("a");
            countSp.forEachRemaining(x -> {
            });

            // A weakly consistent backing map raises nothing at all.
            final Multiset<String> weak = new Multiset<>(java.util.concurrent.ConcurrentHashMap.class);
            weak.add("a");
            weak.add("b");
            final Spliterator<String> weakSp = weak.spliterator();
            weak.add("c");
            weakSp.forEachRemaining(x -> {
            });
        }
    }

    // ------------------------------------------------------------------------------------------------
    // B4 - the six put-family entry points each carried their own copy of the "create, add, and drop an
    //      empty collection that refused the add" block. Behaviour must be identical after the dedupe.
    // ------------------------------------------------------------------------------------------------
    @Nested
    public class B4_PutFamilyIsUnchanged {

        @Test
        public void b4_putCreatesAndAppends() {
            final ListMultimap<String, Integer> mm = CommonUtil.newListMultimap();

            assertTrue(mm.put("a", 1));
            assertTrue(mm.put("a", 1));
            assertEquals(Arrays.asList(1, 1), mm.get("a"));
        }

        @Test
        public void b4_putReportsFalseForARejectedDuplicate() {
            final SetMultimap<String, Integer> mm = CommonUtil.newSetMultimap();

            assertTrue(mm.put("a", 1));
            assertFalse(mm.put("a", 1));
            assertEquals(1, mm.totalValueCount());
            assertTrue(mm.containsKey("a"));
        }

        @Test
        public void b4_putAllAccumulatesAcrossEntries() {
            final ListMultimap<String, Integer> mm = CommonUtil.newListMultimap();
            mm.put("a", 1);

            final Map<String, Integer> src = new LinkedHashMap<>();
            src.put("a", 3);
            src.put("b", 2);
            assertTrue(mm.putAll(src));
            assertEquals(Arrays.asList(1, 3), mm.get("a"));
            assertEquals(Arrays.asList(2), mm.get("b"));

            assertFalse(mm.putAll(null));
            assertFalse(mm.putAll(new HashMap<>()));
        }

        @Test
        public void b4_putAllReportsFalseWhenEverySetAddIsRejected() {
            final SetMultimap<String, Integer> mm = CommonUtil.newSetMultimap();
            mm.put("a", 1);
            mm.put("b", 2);

            final Map<String, Integer> src = new LinkedHashMap<>();
            src.put("a", 1);
            src.put("b", 2);
            assertFalse(mm.putAll(src));
            assertEquals(2, mm.totalValueCount());
        }

        @Test
        public void b4_putIfValueAbsentChecksMembershipFirst() {
            final SetMultimap<String, Integer> mm = CommonUtil.newSetMultimap();

            assertTrue(mm.putIfValueAbsent("a", 1));
            assertFalse(mm.putIfValueAbsent("a", 1));
            assertTrue(mm.putIfValueAbsent("a", 2));
            assertEquals(2, mm.get("a").size());

            final ListMultimap<String, Integer> lm = CommonUtil.newListMultimap();
            lm.put("a", 1);
            assertFalse(lm.putIfValueAbsent("a", 1));
            assertEquals(Arrays.asList(1), lm.get("a"));
        }

        @Test
        public void b4_putIfValueAbsentOnAnExternallyEmptiedKeyRefills() {
            // The refactored path: the key is present but its collection was emptied through get(key), so
            // contains(e) is false and the add must go through and re-establish the mapping.
            final SetMultimap<String, Integer> mm = CommonUtil.newSetMultimap();
            mm.put("k", 1);
            mm.get("k").clear();

            assertTrue(mm.putIfValueAbsent("k", 2));
            assertEquals(CommonUtil.newHashSet(Arrays.asList(2)), mm.get("k"));
            assertEquals(1, mm.totalValueCount());
        }

        @Test
        public void b4_putValuesFamilySkipsNullAndEmptySources() {
            final ListMultimap<String, Integer> mm = CommonUtil.newListMultimap();

            assertFalse(mm.putValues("a", null));
            assertFalse(mm.putValues("a", new ArrayList<>()));
            assertFalse(mm.containsKey("a"));

            assertTrue(mm.putValues("a", Arrays.asList(1, 2)));
            assertEquals(Arrays.asList(1, 2), mm.get("a"));

            final Map<String, List<Integer>> src = new LinkedHashMap<>();
            src.put("a", Arrays.asList(3));
            src.put("skipped", new ArrayList<>());
            assertTrue(mm.putValues(src));
            assertEquals(Arrays.asList(1, 2, 3), mm.get("a"));
            assertFalse(mm.containsKey("skipped"));

            final ListMultimap<String, Integer> other = CommonUtil.newListMultimap();
            other.putValues("b", Arrays.asList(4));
            assertTrue(mm.putValues(other));
            assertEquals(Arrays.asList(4), mm.get("b"));
        }

        @Test
        public void b4_anEmptyCollectionThatSucceedsIsSimplyRefilled() {
            // Reachable only by emptying a live collection from get(key).
            final SetMultimap<String, Integer> mm = CommonUtil.newSetMultimap();
            mm.put("k", 1);
            mm.get("k").clear();
            assertTrue(mm.containsKey("k"));

            // An add that succeeds re-establishes the mapping normally.
            assertTrue(mm.put("k", 2));
            assertEquals(CommonUtil.newHashSet(Arrays.asList(2)), mm.get("k"));
        }

        /**
         * A Set that refuses one sentinel value. No JDK collection rejects an add into an empty instance,
         * so this is the only way to reach the "empty collection refused the add" repair branch that
         * {@code addOne}/{@code addMany} carry.
         */
        private static final class RejectingSet extends java.util.HashSet<String> {
            private static final long serialVersionUID = 1L;

            @Override
            public boolean add(final String e) {
                return !"REJECT".equals(e) && super.add(e);
            }

            @Override
            public boolean addAll(final Collection<? extends String> c) {
                boolean modified = false;

                for (final String e : c) {
                    modified |= add(e);
                }

                return modified;
            }
        }

        private Multimap<String, String, Set<String>> rejectingMultimap() {
            final Multimap<String, String, Set<String>> mm = CommonUtil.newMultimap(Suppliers.ofMap(), RejectingSet::new);
            mm.put("k", "ok");
            mm.get("k").clear(); // empty, but still mapped
            return mm;
        }

        @Test
        public void b4_addOneDropsAnEmptyCollectionThatRefusedTheAdd() {
            final Multimap<String, String, Set<String>> mm = rejectingMultimap();
            assertTrue(mm.containsKey("k"));

            assertFalse(mm.put("k", "REJECT"));

            // Nothing could be added and nothing was there: the key must not survive as an empty mapping.
            assertFalse(mm.containsKey("k"));
            assertNull(mm.get("k"));
            assertTrue(mm.isEmpty());
        }

        @Test
        public void b4_addManyDropsAnEmptyCollectionThatRefusedEveryAdd() {
            final Multimap<String, String, Set<String>> mm = rejectingMultimap();

            assertFalse(mm.putValues("k", Arrays.asList("REJECT", "REJECT")));

            assertFalse(mm.containsKey("k"));
            assertTrue(mm.isEmpty());
        }

        @Test
        public void b4_aNonEmptyCollectionThatRefusesTheAddKeepsItsKey() {
            // The repair applies only when the collection is also empty - a plain rejected duplicate
            // must leave the existing values alone.
            final Multimap<String, String, Set<String>> mm = CommonUtil.newMultimap(Suppliers.ofMap(), RejectingSet::new);
            mm.put("k", "ok");

            assertFalse(mm.put("k", "REJECT"));

            assertTrue(mm.containsKey("k"));
            assertEquals(CommonUtil.newHashSet(Arrays.asList("ok")), mm.get("k"));
        }

        @Test
        public void b4_ifKeyAbsentVariantsKeepAnEmptyButMappedKey() {
            // Deliberately NOT symmetric with put(..): an empty-but-mapped key is still present, so these
            // must report "key already present" and leave it exactly as it was.
            final ListMultimap<String, Integer> mm = CommonUtil.newListMultimap();
            mm.put("k", 1);
            mm.get("k").clear();

            assertFalse(mm.putIfKeyAbsent("k", 2));
            assertFalse(mm.putValuesIfKeyAbsent("k", Arrays.asList(2)));
            assertTrue(mm.containsKey("k"));
            assertTrue(mm.get("k").isEmpty());
        }
    }

    // ------------------------------------------------------------------------------------------------
    // B5 - documented, not changed: retainAll and the Object-keyed queries inherit the argument
    //      collection's / backing map's own null and type behaviour.
    // ------------------------------------------------------------------------------------------------
    @Nested
    public class B5_InheritedNullAndTypeBehaviour {

        @Test
        public void b5_retainAllWithANonEmptyNullHostileCollectionPropagatesNpe() {
            final Multiset<String> ms = new Multiset<>();
            ms.add(null, 1);
            ms.add("k", 1);

            assertThrows(NullPointerException.class, () -> ms.retainAll(new TreeSet<>(Arrays.asList("k"))));
        }

        @Test
        public void b5_retainAllWithAnEmptyCollectionClearsWithoutTestingElements() {
            final Multiset<String> ms = new Multiset<>();
            ms.add(null, 1);
            ms.add("k", 1);

            assertTrue(ms.retainAll(new TreeSet<>()));
            assertTrue(ms.isEmpty());
        }

        @Test
        public void b5_retainAllStillRejectsANullArgument() {
            assertThrows(NullPointerException.class, () -> Multiset.of("a").retainAll(null));
        }

        @Test
        public void b5_foreignKeyLookupFollowsTheBackingMap() {
            assertEquals(0, Multiset.of("a").getCount(1));
            assertFalse(Multiset.of("a").contains(1));

            // An empty sorted backing never compares, so it still reports "absent"...
            final Multiset<String> emptyTree = new Multiset<>(TreeMap.class);
            assertEquals(0, emptyTree.getCount(1));

            // ...while a non-empty one compares and throws, exactly as TreeMap.get would.
            final Multiset<String> tree = new Multiset<>(TreeMap.class);
            tree.add("a");
            assertThrows(ClassCastException.class, () -> tree.getCount(1));
        }
    }

    // ------------------------------------------------------------------------------------------------
    // B6 - Multiset's iterator does not support remove(); now documented.
    // ------------------------------------------------------------------------------------------------
    @Nested
    public class B6_IteratorRemoveIsUnsupported {

        @Test
        public void b6_removeThrowsUnsupportedOperation() {
            final Iterator<String> iter = Multiset.of("a").iterator();
            iter.next();

            assertThrows(UnsupportedOperationException.class, iter::remove);
        }

        @Test
        public void b6_theDocumentedAlternativesWork() {
            final Multiset<String> ms = Multiset.of("a", "a", "b");

            assertTrue(ms.remove("a"));
            assertEquals(2, ms.removeAllOccurrencesOf("b") + ms.getCount("a"));
            assertTrue(ms.removeIf("a"::equals));
            assertTrue(ms.isEmpty());
        }
    }

    // ------------------------------------------------------------------------------------------------
    // B7 - the unused base constructors are gone: the int one hard-coded an ArrayList value supplier
    //      regardless of V, so any subclass reaching it would have failed with a ClassCastException.
    // ------------------------------------------------------------------------------------------------
    @Nested
    public class B7_NoUnsafeBaseConstructors {

        @Test
        public void b7_baseMultimapDeclaresNoIntConstructor() {
            for (final Constructor<?> ctor : Multimap.class.getDeclaredConstructors()) {
                final Class<?>[] params = ctor.getParameterTypes();

                assertFalse(params.length == 1 && params[0] == int.class, "Multimap must not declare a Multimap(int) constructor");
            }
        }

        @Test
        public void b7_theNoArgBaseConstructorIsKeptAndYieldsListValues() {
            // Unlike the int one it has real callers, and it routes through Multimap(Class, Class) so the
            // value supplier matches the ArrayList its javadoc promises.
            final Multimap<String, Integer, List<Integer>> mm = new Multimap<>();
            mm.put("a", 1);

            assertEquals(Multimap.class, mm.getClass());
            assertTrue(mm.get("a") instanceof ArrayList);
            assertEquals(Arrays.asList(1), mm.get("a"));
        }

        @Test
        public void b7_subclassIntConstructorsStillProduceTheRightValueType() {
            final SetMultimap<String, Integer> sm = SetMultimap.of("a", 1);
            assertTrue(sm.get("a") instanceof Set);

            final ListMultimap<String, Integer> lm = ListMultimap.of("a", 1);
            assertTrue(lm.get("a") instanceof List);
        }
    }

    // ------------------------------------------------------------------------------------------------
    // D1 - replaceCollectionContents treated an empty replacement as success, which would have returned
    //      with the target cleared and the key still mapped to it.
    // ------------------------------------------------------------------------------------------------
    @Nested
    public class D1_EmptyReplacementNeverLeavesAnEmptyMapping {

        @Test
        public void d1_replaceValuesWithEmptyRemovesTheKey() {
            final ListMultimap<String, Integer> mm = CommonUtil.newListMultimap();
            mm.putValues("k", Arrays.asList(1, 2));

            assertTrue(mm.replaceValues("k", new ArrayList<>()));
            assertFalse(mm.containsKey("k"));
            assertNull(mm.get("k"));

            assertFalse(mm.replaceValues("absent", new ArrayList<>()));
        }

        @Test
        public void d1_replaceValuesWithNullRemovesTheKey() {
            final ListMultimap<String, Integer> mm = CommonUtil.newListMultimap();
            mm.putValues("k", Arrays.asList(1, 2));

            assertTrue(mm.replaceValues("k", null));
            assertFalse(mm.containsKey("k"));
        }

        @Test
        public void d1_replaceValuesWithContentKeepsTheCollectionIdentity() {
            final ListMultimap<String, Integer> mm = CommonUtil.newListMultimap();
            mm.putValues("k", Arrays.asList(1, 2));
            final List<Integer> live = mm.get("k");

            assertTrue(mm.replaceValues("k", Arrays.asList(7, 8)));
            assertSame(live, mm.get("k"));
            assertEquals(Arrays.asList(7, 8), live);
        }

        @Test
        public void d1_replaceCollectionContentsRejectsAnEmptyReplacementInsteadOfEmptyingTheMapping() throws Exception {
            // White-box: every public caller guards N.isEmpty first, so the old
            // `|| replacement.isEmpty()` success branch was unreachable - which is exactly why removing it
            // needs a direct test. Reached reflectively so a future ninth caller cannot silently
            // reintroduce a key mapped to an empty collection.
            final ListMultimap<String, Integer> mm = CommonUtil.newListMultimap();
            mm.putValues("k", Arrays.asList(1, 2));

            final java.lang.reflect.Method m = Multimap.class.getDeclaredMethod("replaceCollectionContents", Object.class, Collection.class, Collection.class);
            m.setAccessible(true);

            final Throwable cause = assertThrows(java.lang.reflect.InvocationTargetException.class, () -> m.invoke(mm, "k", mm.get("k"), new ArrayList<>()))
                    .getCause();

            assertTrue(cause instanceof IllegalStateException, String.valueOf(cause));
            // The rollback puts the previous contents back, so the key survives - but NOT as an empty
            // mapping, which is the state the removed success branch would have left behind.
            assertEquals(Arrays.asList(1, 2), mm.get("k"));
            assertFalse(mm.valueCollections().stream().anyMatch(Collection::isEmpty), "no key may be left mapped to an empty collection");
        }

        @Test
        public void d1_everyEmptyResultPathRemovesRatherThanEmpties() {
            final ListMultimap<String, Integer> mm = CommonUtil.newListMultimap();

            mm.putValues("a", Arrays.asList(1));
            mm.replaceValuesIf(k -> true, new ArrayList<>());
            assertTrue(mm.isEmpty());

            mm.putValues("b", Arrays.asList(1));
            mm.replaceValuesIf((k, v) -> true, new ArrayList<>());
            assertTrue(mm.isEmpty());

            mm.putValues("c", Arrays.asList(1));
            mm.replaceAll((k, v) -> new ArrayList<>());
            assertTrue(mm.isEmpty());

            mm.putValues("d", Arrays.asList(1));
            assertNull(mm.computeIfPresent("d", (k, v) -> new ArrayList<>()));
            assertTrue(mm.isEmpty());

            mm.putValues("e", Arrays.asList(1));
            assertNull(mm.compute("e", (k, v) -> new ArrayList<>()));
            assertTrue(mm.isEmpty());

            mm.putValues("f", Arrays.asList(1));
            assertNull(mm.merge("f", Arrays.asList(2), (o, n) -> new ArrayList<>()));
            assertTrue(mm.isEmpty());

            mm.putValues("g", Arrays.asList(1));
            assertNull(mm.merge("g", 2, (o, n) -> new ArrayList<>()));
            assertTrue(mm.isEmpty());
        }
    }

    // ------------------------------------------------------------------------------------------------
    // J3 - computeIfAbsent documented that it stored "the newly computed collection"; it copies the
    //      contents into a supplier-made collection instead.
    // ------------------------------------------------------------------------------------------------
    @Nested
    public class J3_ComputeIfAbsentCopiesContents {

        @Test
        public void j3_theFunctionsCollectionIsNotStored() {
            final ListMultimap<String, Integer> mm = CommonUtil.newListMultimap();
            final List<Integer> supplied = new ArrayList<>(Arrays.asList(1, 2));

            final List<Integer> stored = mm.computeIfAbsent("k", k -> supplied);

            assertNotSame(supplied, stored);
            assertEquals(Arrays.asList(1, 2), stored);

            supplied.add(99);
            assertEquals(Arrays.asList(1, 2), mm.get("k"));
        }

        @Test
        public void j3_theStoredCollectionHasTheConfiguredType() {
            final ListMultimap<String, Integer> mm = CommonUtil.newListMultimap();

            mm.computeIfAbsent("k", k -> ImmutableList.of(7));

            // Because the contents were copied, the class's own mutators still work.
            assertTrue(mm.put("k", 8));
            assertEquals(Arrays.asList(7, 8), mm.get("k"));
        }

        @Test
        public void j3_presentKeyReturnsItsLiveCollectionUntouched() {
            final ListMultimap<String, Integer> mm = CommonUtil.newListMultimap();
            mm.put("k", 1);
            final List<Integer> live = mm.get("k");

            assertSame(live, mm.computeIfAbsent("k", k -> Arrays.asList(9)));
            assertEquals(Arrays.asList(1), live);
        }

        @Test
        public void j3_nullOrEmptyResultStoresNothing() {
            final ListMultimap<String, Integer> mm = CommonUtil.newListMultimap();

            assertNull(mm.computeIfAbsent("a", k -> null));
            assertNull(mm.computeIfAbsent("b", k -> new ArrayList<>()));
            assertTrue(mm.isEmpty());
        }
    }

    // ------------------------------------------------------------------------------------------------
    // J4 - valueSpliterator() claimed "no characteristics"; Spliterators.spliterator adds SIZED|SUBSIZED.
    // ------------------------------------------------------------------------------------------------
    @Nested
    public class J4_ValueSpliteratorCharacteristics {

        @Test
        public void j4_reportsSizedAndSubsizedWithTheExactTotal() {
            final ListMultimap<String, Integer> mm = CommonUtil.newListMultimap();
            mm.putValues("a", Arrays.asList(1, 2, 3));

            final Spliterator<Integer> sp = mm.allValues().spliterator();

            assertTrue(sp.hasCharacteristics(Spliterator.SIZED));
            assertTrue(sp.hasCharacteristics(Spliterator.SUBSIZED));
            assertEquals(3L, sp.estimateSize());
            assertEquals(3, mm.allValues().size());
            assertEquals(3, mm.totalValueCount());
        }
    }

    // ------------------------------------------------------------------------------------------------
    // J5 - toMap(IntFunction)'s example passed an expected KEY COUNT straight to a capacity constructor,
    //      the exact anti-pattern flatValues' own example warns against. The documented form must work.
    // ------------------------------------------------------------------------------------------------
    @Nested
    public class J5_SizeHintIsAnExpectedCount {

        @Test
        public void j5_theDocumentedToMapFormWorksAndPreservesOrder() {
            final ListMultimap<String, Integer> mm = CommonUtil.newLinkedListMultimap();
            mm.putValues("a", Arrays.asList(1, 2));
            mm.putValues("b", Arrays.asList(3));

            final LinkedHashMap<String, List<Integer>> copy = mm.toMap(N::newLinkedHashMap);

            assertEquals(Arrays.asList("a", "b"), new ArrayList<>(copy.keySet()));
            assertEquals(Arrays.asList(1, 2), copy.get("a"));
            assertEquals(2, mm.keyCount());
        }

        @Test
        public void j5_theSizeHintIsTheKeyCountAndTheResultIsIndependent() {
            final ListMultimap<String, Integer> mm = CommonUtil.newListMultimap();
            mm.putValues("a", Arrays.asList(1, 2, 3));
            mm.putValues("b", Arrays.asList(4));

            final int[] hint = { -1 };
            final Map<String, List<Integer>> copy = mm.toMap(size -> {
                hint[0] = size;
                return CommonUtil.newLinkedHashMap(size);
            });

            assertEquals(2, hint[0]); // keys, not the 4 values
            copy.get("a").add(99);
            assertEquals(Arrays.asList(1, 2, 3), mm.get("a"));
        }

        @Test
        public void j5_flatValuesHintIsTheTotalValueCount() {
            final ListMultimap<String, Integer> mm = CommonUtil.newListMultimap();
            mm.putValues("a", Arrays.asList(1, 2, 3));
            mm.putValues("b", Arrays.asList(4));

            final int[] hint = { -1 };
            final List<Integer> all = mm.flatValues(size -> {
                hint[0] = size;
                return new ArrayList<>(size);
            });

            assertEquals(4, hint[0]); // values, not the 2 keys
            assertEquals(4, all.size());
        }
    }

    @Nested
    public class C001_C006_SortedConvertersAreLosslessOrLoud {

        private Multiset<String> identityMultiset(final String a, final String b) {
            final Multiset<String> ms = new Multiset<>(IdentityHashMap.class);
            ms.add(a, 2);
            ms.add(b, 3);
            return ms;
        }

        @Test
        public void c001_allThreeSortedConvertersRefuseToCollapseEntries() {
            final String[] d = equalButDistinct();

            // Before the fix each of these returned a 1-entry map whose single count was 3, 2 and 3
            // respectively - three different answers for the same multiset, none of them summing to 5.
            assertThrows(IllegalStateException.class, () -> identityMultiset(d[0], d[1]).toMapSortedByOccurrences());
            assertThrows(IllegalStateException.class, () -> identityMultiset(d[0], d[1]).toMapSortedByOccurrences(Comparator.reverseOrder()));
            assertThrows(IllegalStateException.class, () -> identityMultiset(d[0], d[1]).toMapSortedByKey(Comparator.naturalOrder()));
        }

        @Test
        public void c001_theMessageNamesTheOffendingElementAndTheAlternative() {
            final String[] d = equalButDistinct();
            final String msg = assertThrows(IllegalStateException.class, () -> identityMultiset(d[0], d[1]).toMapSortedByOccurrences()).getMessage();

            assertTrue(msg.contains("dup"), msg);
            assertTrue(msg.contains("toMap()"), msg);
        }

        @Test
        public void c001_theLosslessConvertersStillWorkOnTheSameMultiset() {
            final String[] d = equalButDistinct();
            final Multiset<String> ms = identityMultiset(d[0], d[1]);

            assertEquals(2, ms.toMap().size());
            assertEquals(2, ms.toImmutableMap().size());
            assertEquals(2, ms.entrySet().size());
            assertEquals(5, ms.sumOfOccurrences());
            // and the caller-supplied-map overload can opt into a faithful copy
            assertEquals(2, ms.toMap(size -> new IdentityHashMap<String, Integer>(size)).size());
        }

        @Test
        public void c001_ordinaryBackingsAreUnaffected() {
            final Multiset<String> ms = Multiset.of("a", "b", "b", "c", "c", "c");

            assertEquals(Arrays.asList("a", "b", "c"), new ArrayList<>(ms.toMapSortedByOccurrences().keySet()));
            assertEquals(Arrays.asList("c", "b", "a"), new ArrayList<>(ms.toMapSortedByOccurrences(Comparator.reverseOrder()).keySet()));
            assertEquals(Arrays.asList("a", "b", "c"), new ArrayList<>(ms.toMapSortedByKey(Comparator.naturalOrder()).keySet()));
            assertEquals(LinkedHashMap.class, ms.toMapSortedByOccurrences().getClass());
            assertTrue(new Multiset<String>().toMapSortedByOccurrences().isEmpty());
        }

        @Test
        public void c001_aCoarserBackingIsNotAFalsePositive() {
            // CASE_INSENSITIVE_ORDER folds "a"/"A" in the BACKING map at insertion time, so only survivors
            // reach the converter and nothing collides in the result.
            final Multiset<String> ms = new Multiset<>(() -> new TreeMap<String, Object>(String.CASE_INSENSITIVE_ORDER));
            ms.add("a", 2);
            ms.add("A", 3);
            ms.add("b", 1);

            assertEquals(2, ms.countOfDistinctElements());
            final Map<String, Integer> sorted = ms.toMapSortedByOccurrences();
            assertEquals(2, sorted.size());
            assertEquals(6, sorted.values().stream().mapToInt(Integer::intValue).sum());
            assertEquals(ms.sumOfOccurrences(), sorted.values().stream().mapToInt(Integer::intValue).sum());
        }

        /** A Map whose size() over-reports - stands in for a backing map that shrinks mid-call. */
        static final class OverReportingMap<K, V> extends java.util.AbstractMap<K, V> {
            private final Map<K, V> delegate = new LinkedHashMap<>();

            @Override
            public Set<Map.Entry<K, V>> entrySet() {
                return delegate.entrySet();
            }

            @Override
            public V put(final K k, final V v) {
                return delegate.put(k, v);
            }

            @Override
            public int size() {
                return delegate.size() + 1;
            }

            @Override
            public boolean isEmpty() {
                return delegate.isEmpty();
            }
        }

        @Test
        public void c006_aBackingMapYieldingFewerEntriesThanItReportsNoLongerNpes() {
            final Multiset<String> ms = new Multiset<>(() -> new OverReportingMap<String, Object>());
            ms.add("a", 2);
            ms.add("b", 1);

            // Before the fix: NullPointerException from Arrays.sort dereferencing toArray's null sentinel.
            assertEquals(Arrays.asList("b", "a"), new ArrayList<>(ms.toMapSortedByOccurrences().keySet()));
            assertEquals(Arrays.asList("a", "b"), new ArrayList<>(ms.toMapSortedByKey(Comparator.naturalOrder()).keySet()));
            assertEquals(2, ms.toMapSortedByOccurrences().size());
        }
    }

    // ------------------------------------------------------------------------------------------------
    // C-008 - the Multiset fast path in Multiset(Iterable) used backingMap.put(..), so copying a source whose
    //         backing equivalence is finer than the destination HashMap's OVERWROTE counts instead of
    //         accumulating them - dropping occurrences and disagreeing with the element-wise path.
    // ------------------------------------------------------------------------------------------------
    @Nested
    public class C008_CopyingAMultisetAccumulatesInsteadOfOverwriting {

        private Multiset<String> identitySource(final String a, final String b) {
            final Multiset<String> ms = new Multiset<>(IdentityHashMap.class);
            ms.add(a, 2);
            ms.add(b, 3);
            return ms;
        }

        @Test
        public void c008_theFastPathAgreesWithEveryElementWiseRoute() {
            final String[] d = equalButDistinct();
            final Multiset<String> src = identitySource(d[0], d[1]);
            assertEquals(2, src.countOfDistinctElements());
            assertEquals(5, src.sumOfOccurrences());

            final Multiset<String> fast = new Multiset<>(src); // Multiset fast path
            final Iterable<String> plainIterable = src::iterator; // not a Multiset/Collection
            final Multiset<String> viaIterable = new Multiset<>(plainIterable);
            final Multiset<String> viaCollection = new Multiset<>(new ArrayList<>(src));
            final Multiset<String> viaIterator = Multiset.create(src.iterator());

            // Before the fix the fast path gave 2 or 3 depending on the source's iteration order.
            assertEquals(5, fast.sumOfOccurrences());
            assertEquals(5, viaIterable.sumOfOccurrences());
            assertEquals(5, viaCollection.sumOfOccurrences());
            assertEquals(5, viaIterator.sumOfOccurrences());
            assertEquals(1, fast.countOfDistinctElements()); // merging into a HashMap is expected
            assertEquals(fast, viaIterable);
            assertEquals(fast, viaCollection);
        }

        @Test
        public void c008_createDelegatesToTheSameFixedPath() {
            final String[] d = equalButDistinct();
            final Multiset<String> src = identitySource(d[0], d[1]);

            assertEquals(5, Multiset.create(src).sumOfOccurrences());
            assertEquals(Multiset.create(src), new Multiset<>(src));
        }

        @Test
        public void c008_aThreeWayCollisionSumsAllOfThem() {
            final Multiset<String> src = new Multiset<>(IdentityHashMap.class);
            src.add(new String("z"), 1);
            src.add(new String("z"), 2);
            src.add(new String("z"), 4);
            src.add("other", 7);

            final Multiset<String> copy = new Multiset<>(src);

            assertEquals(14, src.sumOfOccurrences());
            assertEquals(14, copy.sumOfOccurrences()); // was 11 - only the last-visited count survived
            assertEquals(7, copy.getCount("z"));
            assertEquals(7, copy.getCount("other"));
        }

        @Test
        public void c008_theOomFastPathIsPreserved() {
            // The fast path exists because copying {x x 2^31-1} occurrence-by-occurrence is O(total count).
            // It must still be O(distinct): this completes instantly rather than walking 2.1 billion elements.
            final Multiset<String> huge = new Multiset<>();
            huge.setCount("x", Integer.MAX_VALUE);
            huge.setCount("y", Integer.MAX_VALUE);

            final Multiset<String> copy = new Multiset<>(huge);

            assertEquals(2, copy.countOfDistinctElements());
            assertEquals(4294967294L, copy.sumOfOccurrences());
            assertEquals(huge, copy);
        }

        @Test
        public void c008_mergingPastIntegerMaxValueIsRejectedLikeTheElementWisePath() {
            final Multiset<String> src = new Multiset<>(IdentityHashMap.class);
            src.add(new String("k"), Integer.MAX_VALUE - 5);
            src.add(new String("k"), 10);

            // Silently produced a wrong count before the fix; now both routes reject it identically.
            assertThrows(IllegalArgumentException.class, () -> new Multiset<>(src));
            final Iterable<String> plainIterable = src::iterator;
            assertThrows(IllegalArgumentException.class, () -> new Multiset<>(plainIterable));
        }

        @Test
        public void c008_ordinaryCopiesAreUnchangedAndIndependent() {
            final Multiset<String> plain = Multiset.of("x", "x", "y");

            final Multiset<String> copy = new Multiset<>(plain);
            assertEquals(plain, copy);
            assertEquals(plain, Multiset.create(plain));
            assertTrue(new Multiset<>(new Multiset<>()).isEmpty());

            // the copy must not alias the source's count holders
            plain.add("x", 5);
            assertEquals(7, plain.getCount("x"));
            assertEquals(2, copy.getCount("x"));
        }
    }

    // ------------------------------------------------------------------------------------------------
    // C-007 - entrySet() holds one entry per backing-map key, but Entry.equals compares element+count, so
    //         two equal-but-distinct elements at the SAME count yield entries equal to each other.
    // ------------------------------------------------------------------------------------------------
    @Nested
    public class C007_EntrySetCaveatForAFinerBacking {

        @Test
        public void c007_equalCountsMakeTheEntriesEqualToEachOther() {
            final String[] d = equalButDistinct();
            final Multiset<String> ms = new Multiset<>(IdentityHashMap.class);
            ms.add(d[0], 2);
            ms.add(d[1], 2); // same count

            final List<Multiset.Entry<String>> entries = new ArrayList<>(ms.entrySet());

            // The view itself is complete and correctly sized...
            assertEquals(2, ms.entrySet().size());
            assertEquals(2, entries.size());
            assertEquals(4, ms.sumOfOccurrences());

            // ...but the two entries are equal, so an equals-based copy collapses them.
            assertEquals(entries.get(0), entries.get(1));
            assertEquals(entries.get(0).hashCode(), entries.get(1).hashCode());
            assertEquals(1, new java.util.HashSet<>(ms.entrySet()).size());

            // toMap() is the lossless alternative the javadoc points at.
            assertEquals(2, ms.toMap().size());
        }

        @Test
        public void c007_differentCountsKeepTheEntriesDistinct() {
            final String[] d = equalButDistinct();
            final Multiset<String> ms = new Multiset<>(IdentityHashMap.class);
            ms.add(d[0], 2);
            ms.add(d[1], 3);

            assertEquals(2, new java.util.HashSet<>(ms.entrySet()).size());
        }

        @Test
        public void c007_anOrdinaryMultisetHasNoSuchProblem() {
            final Multiset<String> ms = Multiset.of("a", "a", "b", "b");

            assertEquals(2, ms.entrySet().size());
            assertEquals(2, new java.util.HashSet<>(ms.entrySet()).size());
        }
    }

    // ------------------------------------------------------------------------------------------------
    // C-002 - retainAll and removeAllOccurrencesOf resolve membership with DIFFERENT equivalences. Both are
    //         contract-conformant; the class javadoc now says so, and this pins it.
    // ------------------------------------------------------------------------------------------------
    @Nested
    public class C002_BulkMethodsUseDifferentEquivalences {

        private Multiset<String> identityMultiset(final String a, final String b) {
            final Multiset<String> ms = new Multiset<>(IdentityHashMap.class);
            ms.add(a, 2);
            ms.add(b, 3);
            return ms;
        }

        @Test
        public void c002_removeAllOccurrencesOfUsesTheBackingMapsEquivalence() {
            final String[] d = equalButDistinct();
            final Multiset<String> ms = identityMultiset(d[0], d[1]);

            // Looked up IN the multiset -> only the instance actually passed is removed.
            assertTrue(ms.removeAllOccurrencesOf(Arrays.asList(d[0])));

            assertEquals(1, ms.countOfDistinctElements());
            assertEquals(0, ms.getCount(d[0]));
            assertEquals(3, ms.getCount(d[1]));
        }

        @Test
        public void c002_removeOccurrencesAlsoUsesTheBackingMapsEquivalence() {
            final String[] d = equalButDistinct();
            final Multiset<String> ms = identityMultiset(d[0], d[1]);

            // Routes through remove(Object,int) -> backingMap.get/remove, so only the instance passed matches.
            assertTrue(ms.removeOccurrences(Arrays.asList(d[0]), 2));
            assertEquals(0, ms.getCount(d[0]));
            assertEquals(3, ms.getCount(d[1]));

            // A third instance that is merely equals-equal is not a backing key, so it matches nothing.
            final Multiset<String> other = identityMultiset(d[0], d[1]);
            assertFalse(other.removeOccurrences(Arrays.asList(new String("dup")), 2));
            assertEquals(5, other.sumOfOccurrences());
        }

        @Test
        public void c002_retainAllUsesTheArgumentCollectionsEquivalence() {
            final String[] d = equalButDistinct();
            final Multiset<String> ms = identityMultiset(d[0], d[1]);

            // Collection.retainAll is specified in terms of c.contains(element), and List.contains uses
            // equals - so the second instance is "contained" too and both survive.
            assertFalse(ms.retainAll(Arrays.asList(d[0])));

            assertEquals(2, ms.countOfDistinctElements());
            assertEquals(5, ms.size());
        }

        @Test
        public void c002_containsAllUsesTheBackingMapsEquivalenceNotTheArguments() {
            final String[] d = equalButDistinct();
            final Multiset<String> ms = identityMultiset(d[0], d[1]);

            assertTrue(ms.containsAll(Arrays.asList(d[0], d[1])));

            // containsAll delegates to backingMap.keySet().containsAll(c), and Set.containsAll asks
            // THIS set about each argument element - so the backing map's equivalence decides, grouping
            // it with removeAllOccurrencesOf rather than with retainAll.
            final String third = new String("dup");
            assertFalse(ms.contains(third));
            assertFalse(ms.containsAll(Arrays.asList(third)));

            // The contrast that makes the grouping visible: the argument collection WOULD say it contains
            // the third instance, but containsAll does not consult the argument that way.
            assertTrue(Arrays.asList(d[0]).contains(third));
            assertFalse(ms.retainAll(Arrays.asList(third))); // retainAll DOES consult it -> keeps everything
            assertEquals(2, ms.countOfDistinctElements());
        }

        @Test
        public void c002_theDefaultHashMapBackingShowsNoDivergence() {
            final String[] d = equalButDistinct();
            final Multiset<String> hash = new Multiset<>();
            hash.add(d[0], 2);
            hash.add(d[1], 3); // same key under equals -> one element, count 5

            assertEquals(1, hash.countOfDistinctElements());
            assertEquals(5, hash.getCount("dup"));
            assertTrue(hash.removeAllOccurrencesOf(Arrays.asList(d[0])));
            assertTrue(hash.isEmpty());
        }
    }

    // ------------------------------------------------------------------------------------------------
    // C-003 - Multiset(Class) javadoc claimed the type "must have an accessible no-arg constructor"; the
    //         standard Map interfaces are supported too and resolve to their usual implementations.
    // ------------------------------------------------------------------------------------------------
    @Nested
    public class C003_BackingMapTypeAcceptsTheStandardInterfaces {

        @Test
        public void c003_mapInterfaceResolvesToAHashMap() {
            final Multiset<String> ms = new Multiset<>(Map.class);
            ms.add("a", 2);

            assertEquals(2, ms.getCount("a"));
            assertEquals(HashMap.class, ms.toMap().getClass());
        }

        @Test
        public void c003_sortedMapInterfaceResolvesToATreeMap() {
            final Multiset<String> ms = new Multiset<>(java.util.SortedMap.class);
            ms.add("b");
            ms.add("a");

            assertEquals(TreeMap.class, ms.toMap().getClass());
            assertEquals(Arrays.asList("a", "b"), new ArrayList<>(ms.elementSet()));
            // and it inherits TreeMap's null hostility, as the class javadoc says
            assertThrows(NullPointerException.class, () -> ms.add(null));
        }

        @Test
        public void c003_concreteTypesAndNullStillBehaveAsDocumented() {
            assertEquals(LinkedHashMap.class, new Multiset<String>(LinkedHashMap.class).toMap().getClass());
            assertThrows(IllegalArgumentException.class, () -> new Multiset<String>((Class<Map>) null));
        }

        @Test
        public void c003_anUninstantiableTypeStillThrowsIllegalArgument() {
            // Documented @throws: a Map class that genuinely cannot be created must be rejected.
            assertThrows(IllegalArgumentException.class, () -> new Multiset<String>(NoNoArgMap.class));
        }
    }

    /** A concrete Map with no accessible no-arg constructor - used by C-003. */
    public static final class NoNoArgMap<K, V> extends java.util.AbstractMap<K, V> {
        private final Map<K, V> delegate = new HashMap<>();

        public NoNoArgMap(final int required) {
            // no no-arg constructor on purpose
        }

        @Override
        public Set<Map.Entry<K, V>> entrySet() {
            return delegate.entrySet();
        }

        @Override
        public V put(final K k, final V v) {
            return delegate.put(k, v);
        }
    }

    // ------------------------------------------------------------------------------------------------
    // C-004 - Iterable is a functional interface, so a no-arg lambda is ambiguous between
    //         Multiset(Iterable) and Multiset(Supplier). Pins the documented workaround.
    // ------------------------------------------------------------------------------------------------
    @Nested
    public class C004_SupplierConstructorNeedsAnExplicitTargetType {

        @Test
        public void c004_aHoistedSupplierReachesTheDocumentedIllegalArgumentException() {
            // `new Multiset<String>(() -> null)` does NOT compile - "reference to Multiset is ambiguous".
            final java.util.function.Supplier<Map<String, Object>> nullSupplier = () -> null;
            assertThrows(IllegalArgumentException.class, () -> new Multiset<String>(nullSupplier));

            final Map<String, Object> nonEmpty = new HashMap<>();
            nonEmpty.put("pre", "existing");
            final java.util.function.Supplier<Map<String, Object>> nonEmptySupplier = () -> nonEmpty;
            assertThrows(IllegalArgumentException.class, () -> new Multiset<String>(nonEmptySupplier));
        }

        @Test
        public void c004_anUnambiguousLambdaStillResolvesToTheSupplierConstructor() {
            // A TreeMap is not an Iterator, so only the Supplier overload applies - this compiles.
            final Multiset<String> ms = new Multiset<>(() -> new TreeMap<>(String.CASE_INSENSITIVE_ORDER));
            ms.add("Alpha", 2);
            ms.add("ALPHA", 3);

            // The comparator groups them, so the multiset itself sees one element.
            assertEquals(1, ms.countOfDistinctElements());
            assertEquals(5, ms.getCount("alpha"));
        }
    }

    // ------------------------------------------------------------------------------------------------
    // Other - lazy staging in Multiset.removeIf, the self-argument shortcuts, and the accumulator rewrite.
    // ------------------------------------------------------------------------------------------------
    @Nested
    public class Other_MultisetBulkRemovalIsUnchanged {

        @Test
        public void other_removeAllOccurrencesOfSelfClearsAndReportsChange() {
            final Multiset<String> ms = Multiset.of("a", "a", "b");

            assertTrue(ms.removeAllOccurrencesOf(ms));
            assertTrue(ms.isEmpty());

            assertFalse(ms.removeAllOccurrencesOf(ms));
        }

        @Test
        public void other_removeOccurrencesAccumulatesAcrossElements() {
            final Multiset<String> ms = Multiset.of("a", "a", "a", "b", "b");

            // "z" is absent, so only the later elements make this true - the accumulator must not stop early.
            assertTrue(ms.removeOccurrences(Arrays.asList("z", "a"), 2));
            assertEquals(1, ms.getCount("a"));
            assertEquals(2, ms.getCount("b"));

            assertFalse(ms.removeOccurrences(Arrays.asList("z"), 2));
            assertFalse(ms.removeOccurrences(Arrays.asList("a"), 0));
            assertFalse(ms.removeOccurrences(new ArrayList<>(), 2));
        }

        @Test
        public void other_removeOccurrencesOfSelfClears() {
            final Multiset<String> ms = Multiset.of("a", "a", "b");

            assertTrue(ms.removeOccurrences(ms, 1));
            assertTrue(ms.isEmpty());
        }

        @Test
        public void other_removeAllOccurrencesOfACollectionAccumulates() {
            final Multiset<String> ms = Multiset.of("a", "a", "b");

            assertTrue(ms.removeAllOccurrencesOf(Arrays.asList("z", "b")));
            assertEquals(2, ms.getCount("a"));
            assertEquals(0, ms.getCount("b"));

            assertFalse(ms.removeAllOccurrencesOf(Arrays.asList("z")));
            assertFalse(ms.removeAllOccurrencesOf((Collection<?>) null));
        }

        @Test
        public void other_removeIfRemovesEveryMatchAndReportsCorrectly() {
            final Multiset<String> ms = Multiset.of("apple", "apple", "banana", "cherry");

            assertTrue(ms.removeIf(s -> s.startsWith("a")));
            assertEquals(0, ms.getCount("apple"));
            assertEquals(1, ms.getCount("banana"));

            assertFalse(ms.removeIf(s -> s.startsWith("z")));
            assertEquals(2, ms.countOfDistinctElements());
        }
    }
}

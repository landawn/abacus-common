package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
import java.util.IdentityHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Consumer;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.function.ObjIntPredicate;

/**
 * Regression tests for the second 2026-08-31 review of Multiset / Multimap / ListMultimap / SetMultimap / BiMap.
 * Each nested class corresponds to one finding; the test names carry the finding id.
 *
 * <p>Extends {@link TestBase} (as every test class in this project must): that is where {@code @Tag("unit")}
 * comes from, and {@code AbacusCoreTestSuite} selects on {@code @IncludeTags("unit")} - without it these tests
 * pass in isolation but are silently skipped by the suite.
 */
public class CollectionsRegressionBTest extends TestBase {

    /** Two distinct String instances that are equal under equals/hashCode but distinct under identity. */
    private static String[] equalButDistinct() {
        return new String[] { new String("dup"), new String("dup") };
    }

    // ------------------------------------------------------------------------------------------------
    // B1 - matching keys were staged in a HashSet, which collapses backing-map keys that are equal under
    //      equals/hashCode but distinct under the backing map's own equivalence, leaving one behind.
    // ------------------------------------------------------------------------------------------------
    @Nested
    public class B1_KeyStagingPreservesBackingMapEquivalence extends TestBase {

        private Multiset<String> identityMultiset(final String a, final String b) {
            final Multiset<String> ms = new Multiset<>(IdentityHashMap.class);
            ms.add(a, 2);
            ms.add(b, 3);
            return ms;
        }

        @Test
        public void b1_multiset_removeAllOccurrencesIf_predicate_removesEveryMatchingKey() {
            final String[] d = equalButDistinct();
            final Multiset<String> ms = identityMultiset(d[0], d[1]);
            assertEquals(2, ms.countOfDistinctElements());

            assertTrue(ms.removeAllOccurrencesIf(e -> true));

            assertEquals(0, ms.countOfDistinctElements());
            assertEquals(0, ms.size());
        }

        @Test
        public void b1_multiset_removeAllOccurrencesIf_objIntPredicate_removesEveryMatchingKey() {
            final String[] d = equalButDistinct();
            final Multiset<String> ms = identityMultiset(d[0], d[1]);

            assertTrue(ms.removeAllOccurrencesIf((ObjIntPredicate<String>) (e, count) -> count > 0));

            assertEquals(0, ms.countOfDistinctElements());
            assertEquals(0, ms.size());
        }

        @Test
        public void b1_multiset_removeIf_removesEveryMatchingKey() {
            final String[] d = equalButDistinct();
            final Multiset<String> ms = identityMultiset(d[0], d[1]);

            assertTrue(ms.removeIf(e -> true));

            assertEquals(0, ms.countOfDistinctElements());
        }

        @Test
        public void b1_multiset_retainAll_dropsEveryUnretainedKey() {
            final String[] d = equalButDistinct();
            final Multiset<String> ms = identityMultiset(d[0], d[1]);

            assertTrue(ms.retainAll(Collections.singletonList("nothing-matches")));

            assertEquals(0, ms.countOfDistinctElements());
        }

        @Test
        public void b1_multiset_retainAll_keepsOnlyTheIdentityThatMatches() {
            final String[] d = equalButDistinct();
            final Multiset<String> ms = identityMultiset(d[0], d[1]);

            // `contains` on an ArrayList is equals-based, so both identity-distinct keys are retained.
            assertFalse(ms.retainAll(Collections.singletonList("dup")));
            assertEquals(2, ms.countOfDistinctElements());
            assertEquals(5, ms.size());
        }

        private ListMultimap<String, Integer> identityMultimap(final String a, final String b) {
            final ListMultimap<String, Integer> mm = CommonUtil.newListMultimap(IdentityHashMap.class, ArrayList.class);
            mm.put(a, 1);
            mm.put(b, 2);
            return mm;
        }

        @Test
        public void b1_multimap_removeKeysIf_predicate_removesEveryMatchingKey() {
            final String[] d = equalButDistinct();
            final ListMultimap<String, Integer> mm = identityMultimap(d[0], d[1]);
            assertEquals(2, mm.keyCount());

            assertTrue(mm.removeKeysIf(k -> true));

            assertEquals(0, mm.keyCount());
        }

        @Test
        public void b1_multimap_removeKeysIf_biPredicate_removesEveryMatchingKey() {
            final String[] d = equalButDistinct();
            final ListMultimap<String, Integer> mm = identityMultimap(d[0], d[1]);

            assertTrue(mm.removeKeysIf((k, v) -> !v.isEmpty()));

            assertEquals(0, mm.keyCount());
        }

        @Test
        public void b1_multimap_removeEntriesIf_predicate_removesEveryMatchingKey() {
            final String[] d = equalButDistinct();
            final ListMultimap<String, Integer> mm = CommonUtil.newListMultimap(IdentityHashMap.class, ArrayList.class);
            mm.put(d[0], 7);
            mm.put(d[1], 7);

            assertTrue(mm.removeEntriesIf(k -> true, 7));

            assertEquals(0, mm.keyCount());
        }

        @Test
        public void b1_multimap_removeEntriesIf_biPredicate_removesEveryMatchingKey() {
            final String[] d = equalButDistinct();
            final ListMultimap<String, Integer> mm = CommonUtil.newListMultimap(IdentityHashMap.class, ArrayList.class);
            mm.put(d[0], 7);
            mm.put(d[1], 7);

            assertTrue(mm.removeEntriesIf((k, v) -> true, 7));

            assertEquals(0, mm.keyCount());
        }

        @Test
        public void b1_multimap_removeValuesIf_predicate_removesEveryMatchingKey() {
            final String[] d = equalButDistinct();
            final ListMultimap<String, Integer> mm = identityMultimap(d[0], d[1]);

            assertTrue(mm.removeValuesIf(k -> true, Arrays.asList(1, 2)));

            assertEquals(0, mm.keyCount());
        }

        @Test
        public void b1_multimap_removeValuesIf_biPredicate_removesEveryMatchingKey() {
            final String[] d = equalButDistinct();
            final ListMultimap<String, Integer> mm = identityMultimap(d[0], d[1]);

            assertTrue(mm.removeValuesIf((k, v) -> true, Arrays.asList(1, 2)));

            assertEquals(0, mm.keyCount());
        }

        @Test
        public void b1_theSameShapeUnderAComparatorFinerThanEquals() {
            // A TreeMap whose comparator distinguishes objects that equals() calls equal is the other way
            // into this bug, and does not depend on IdentityHashMap.
            final String[] d = equalButDistinct();
            final Multiset<String> ms = new Multiset<>(() -> new TreeMap<String, Object>(java.util.Comparator.comparingInt(System::identityHashCode)));
            ms.add(d[0]);
            ms.add(d[1]);
            assertEquals(2, ms.countOfDistinctElements());

            assertTrue(ms.removeAllOccurrencesIf(e -> true));
            assertEquals(0, ms.countOfDistinctElements());
        }

        @Test
        public void b1_ordinaryBackingMapsAreUnaffected() {
            final Multiset<String> ms = Multiset.of("a", "a", "b", "c");
            assertTrue(ms.removeAllOccurrencesIf(e -> e.equals("a")));
            assertEquals(Multiset.of("b", "c"), ms);

            final ListMultimap<String, Integer> mm = ListMultimap.of("a", 1, "b", 2, "c", 3);
            assertTrue(mm.removeKeysIf(k -> k.equals("b")));
            assertEquals(2, mm.keyCount());
            assertNull(mm.get("b"));
        }
    }

    // ------------------------------------------------------------------------------------------------
    // B2 / B6 - Collection / Iterable null contracts.
    // ------------------------------------------------------------------------------------------------
    @Nested
    public class B2_B6_NullArgumentContracts extends TestBase {

        @Test
        public void b2_retainAll_nullThrowsNpeAndLeavesTheMultisetIntact() {
            final Multiset<String> ms = Multiset.of("a", "a", "b");

            assertThrows(NullPointerException.class, () -> ms.retainAll(null));

            // The old behaviour returned true and silently cleared the multiset.
            assertEquals(3, ms.size());
            assertEquals(2, ms.getCount("a"));
        }

        @Test
        public void b2_retainAll_emptyCollectionStillClears() {
            final Multiset<String> ms = Multiset.of("a", "b");
            assertTrue(ms.retainAll(new ArrayList<>()));
            assertTrue(ms.isEmpty());

            // ... and reports no change when there was nothing to clear.
            assertFalse(new Multiset<String>().retainAll(new ArrayList<>()));
        }

        @Test
        public void b6_forEach_nullActionThrowsNpe() {
            final Multiset<String> ms = Multiset.of("a");
            assertThrows(NullPointerException.class, () -> ms.forEach((Consumer<String>) null));
        }

        @Test
        public void b6_toArray_nullArrayThrowsNpe() {
            final Multiset<String> ms = Multiset.of("a");
            assertThrows(NullPointerException.class, () -> ms.toArray((String[]) null));
        }

        @Test
        public void b6_removeIf_nullFilterStillThrowsNpe() {
            final Multiset<String> ms = Multiset.of("a");
            assertThrows(NullPointerException.class, () -> ms.removeIf(null));
        }

        @Test
        public void b6_theClassOwnMethodsKeepTheirIllegalArgumentException() {
            final Multiset<String> ms = Multiset.of("a");
            assertThrows(IllegalArgumentException.class, () -> ms.removeAllOccurrencesIf((java.util.function.Predicate<String>) null));
            assertThrows(IllegalArgumentException.class, () -> ms.removeAllOccurrencesIf((ObjIntPredicate<String>) null));
            assertThrows(IllegalArgumentException.class, () -> ms.forEach((com.landawn.abacus.util.function.ObjIntConsumer<String>) null));
        }

        @Test
        public void b6_theLenientBulkMethodsStayLenientAsDocumented() {
            // Deliberately kept: a null collection is treated as empty, matching the library-wide
            // N.isEmpty(null) convention. Only retainAll differs, because there null would destroy data.
            final Multiset<String> ms = Multiset.of("a", "b");
            assertFalse(ms.addAll(null));
            assertFalse(ms.removeAll(null));
            assertFalse(ms.removeAllOccurrencesOf((Collection<?>) null));
            assertTrue(ms.containsAll(null));
            assertEquals(2, ms.size());
        }
    }

    // ------------------------------------------------------------------------------------------------
    // O1 - Collection.toArray() must allocate a new array even when the collection is empty.
    // ------------------------------------------------------------------------------------------------
    @Nested
    public class O1_ToArrayAllocatesAFreshArray extends TestBase {

        @Test
        public void o1_emptyMultisetsDoNotShareOneArrayInstance() {
            final Object[] a = new Multiset<String>().toArray();
            final Object[] b = new Multiset<String>().toArray();

            assertEquals(0, a.length);
            assertEquals(0, b.length);
            assertNotSame(a, b);
            assertNotSame(CommonUtil.EMPTY_OBJECT_ARRAY, a);
        }

        @Test
        public void o1_nonEmptyToArrayStillWorks() {
            final Multiset<String> ms = Multiset.of("a", "a", "b");
            final Object[] a = ms.toArray();
            assertEquals(3, a.length);
            assertEquals(2, Collections.frequency(Arrays.asList(a), "a"));
        }

        @Test
        public void o1_typedToArrayKeepsTheEndOfDataSentinel() {
            final Multiset<String> ms = Multiset.of("a");
            final String[] big = new String[4];
            Arrays.fill(big, "x");

            final String[] out = ms.toArray(big);

            assertSame(big, out);
            assertEquals("a", out[0]);
            assertNull(out[1], "Collection.toArray(T[]) must null-terminate at index size()");
            assertEquals("x", out[2], "elements past the sentinel are left untouched");
        }
    }

    // ------------------------------------------------------------------------------------------------
    // D1 / D2 - removeAllOccurrencesIf(Predicate) now delegates to removeIf; the *AndGetCount pair now
    //           delegates to a shared helper. Behaviour and message argument names must be unchanged.
    // ------------------------------------------------------------------------------------------------
    @Nested
    public class D1_D2_DelegationIsBehaviourPreserving extends TestBase {

        @Test
        public void d1_removeAllOccurrencesIfAndRemoveIfAgree() {
            final Multiset<String> a = Multiset.of("a", "a", "b", "c");
            final Multiset<String> b = Multiset.of("a", "a", "b", "c");

            assertEquals(a.removeIf(s -> s.equals("a")), b.removeAllOccurrencesIf(s -> s.equals("a")));
            assertEquals(a, b);

            assertEquals(a.removeIf(s -> false), b.removeAllOccurrencesIf(s -> false));
            assertEquals(a, b);
        }

        @Test
        public void d2_addAndGetCountMatchesAddPlusN() {
            for (final int[] c : new int[][] { { 0, 0 }, { 0, 3 }, { 2, 0 }, { 2, 3 } }) {
                final Multiset<String> viaNew = new Multiset<>();
                final Multiset<String> viaOld = new Multiset<>();

                if (c[0] > 0) {
                    viaNew.add("e", c[0]);
                    viaOld.add("e", c[0]);
                }

                assertEquals(viaOld.add("e", c[1]) + c[1], viaNew.addAndGetCount("e", c[1]));
                assertEquals(viaOld, viaNew);
            }
        }

        @Test
        public void d2_removeAndGetCountMatchesRemoveMinusN() {
            for (final int[] c : new int[][] { { 0, 0 }, { 0, 3 }, { 5, 0 }, { 5, 2 }, { 2, 5 } }) {
                final Multiset<String> viaNew = new Multiset<>();
                final Multiset<String> viaOld = new Multiset<>();

                if (c[0] > 0) {
                    viaNew.add("e", c[0]);
                    viaOld.add("e", c[0]);
                }

                assertEquals(Math.max(viaOld.remove("e", c[1]) - c[1], 0), viaNew.removeAndGetCount("e", c[1]));
                assertEquals(viaOld, viaNew);
            }
        }

        @Test
        public void d2_validationMessagesStillNameTheCallersOwnParameter() {
            final Multiset<String> ms = new Multiset<>();

            assertEquals("The specified 'occurrencesToAdd' cannot be negative: -1",
                    assertThrows(IllegalArgumentException.class, () -> ms.add("k", -1)).getMessage());
            assertEquals("The specified 'occurrences' cannot be negative: -2",
                    assertThrows(IllegalArgumentException.class, () -> ms.addAndGetCount("k", -2)).getMessage());
            assertEquals("The specified 'occurrencesToRemove' cannot be negative: -3",
                    assertThrows(IllegalArgumentException.class, () -> ms.remove("k", -3)).getMessage());
            assertEquals("The specified 'occurrences' cannot be negative: -4",
                    assertThrows(IllegalArgumentException.class, () -> ms.removeAndGetCount("k", -4)).getMessage());
            assertTrue(ms.isEmpty());
        }

        @Test
        public void d2_overflowMessageNamesTheCallersOwnParameter() {
            final Multiset<String> ms = new Multiset<>();
            ms.add("k", Integer.MAX_VALUE);

            assertTrue(assertThrows(IllegalArgumentException.class, () -> ms.add("k", 1)).getMessage().contains("occurrencesToAdd=1"));
            assertTrue(assertThrows(IllegalArgumentException.class, () -> ms.addAndGetCount("k", 1)).getMessage().contains("occurrences=1"));
            assertEquals(Integer.MAX_VALUE, ms.getCount("k"));
        }
    }

    // ------------------------------------------------------------------------------------------------
    // B3 - a supplier that does not hand out a fresh instance must be rejected, not silently aliased.
    // ------------------------------------------------------------------------------------------------
    @Nested
    public class B3_SupplierFreshness extends TestBase {

        @Test
        public void b3_multimap_sharedMapSupplierIsRejectedAtConstruction() {
            final Map<String, List<Integer>> shared = new HashMap<>();
            final Multimap<String, Integer, List<Integer>> mm = CommonUtil.newMultimap(() -> shared, ArrayList::new);
            mm.put("k", 1);

            // The first call was fine (the map was empty); the copy's call is not.
            assertThrows(IllegalArgumentException.class, mm::copy);
            assertEquals(1, mm.get("k").size(), "the rejected copy must not have mutated the original");
        }

        @Test
        public void b3_multimap_sharedMapSupplierIsRejectedEvenWhileEmpty() {
            final Map<String, List<Integer>> shared = new HashMap<>();
            final Multimap<String, Integer, List<Integer>> mm = CommonUtil.newMultimap(() -> shared, ArrayList::new);

            // Empty, so the constructor's own emptiness check passes - copyInto's identity check catches it.
            assertThrows(IllegalArgumentException.class, mm::copy);
        }

        @Test
        public void b3_multimap_nonEmptySuppliedMapIsRejected() {
            final Map<String, List<Integer>> seeded = new HashMap<>();
            seeded.put("pre", new ArrayList<>(Collections.singletonList(9)));

            assertThrows(IllegalArgumentException.class, () -> CommonUtil.newMultimap(() -> new HashMap<>(seeded), ArrayList::new));
        }

        @Test
        public void b3_multimap_sharedValueSupplierIsRejectedByCopy() {
            final List<Integer> sharedList = new ArrayList<>();
            final Multimap<String, Integer, List<Integer>> mm = CommonUtil.newMultimap(HashMap::new, () -> sharedList);
            mm.put("a", 1);

            // Without the check, copyInto asks for a "fresh" collection, is handed the one already stored
            // under "a", and appends it to itself - leaving {a=[1, 1]} in BOTH objects.
            assertThrows(IllegalArgumentException.class, mm::copy);
            assertEquals(Collections.singletonList(1), mm.get("a"));
        }

        @Test
        public void b3_multimap_subclassCopyIsGuardedToo() {
            final List<Integer> sharedList = new ArrayList<>();
            final ListMultimap<String, Integer> lm = CommonUtil.newListMultimap(HashMap::new, () -> sharedList);
            lm.put("a", 1);
            assertThrows(IllegalArgumentException.class, lm::copy);

            // An unknown set implementation needs the supplier fallback; known HashSets can be copied directly.
            final Set<Integer> sharedSet = new java.util.HashSet<>() {
            };
            final SetMultimap<String, Integer> sm = CommonUtil.newSetMultimap(HashMap::new, () -> sharedSet);
            sm.put("a", 1);
            assertThrows(IllegalArgumentException.class, sm::copy);
        }

        @Test
        public void b3_multimap_wellBehavedSuppliersStillCopyIndependently() {
            final Multimap<String, Integer, List<Integer>> mm = CommonUtil.newMultimap(HashMap::new, ArrayList::new);
            mm.put("k", 1);

            final Multimap<String, Integer, List<Integer>> copy = mm.copy();
            copy.put("k", 2);

            assertEquals(Collections.singletonList(1), mm.get("k"));
            assertEquals(Arrays.asList(1, 2), copy.get("k"));
            assertNotSame(mm.get("k"), copy.get("k"));
        }

        @Test
        public void b3_multimap_copyStillReproducesAnEmptiedKey() {
            final ListMultimap<String, Integer> lm = ListMultimap.of("a", 1);
            lm.get("a").clear();

            final ListMultimap<String, Integer> copy = lm.copy();
            assertTrue(copy.containsKey("a"));
            assertEquals(lm, copy);
        }

        @Test
        public void b3_biMap_sharedSuppliersAreRejectedByCopy() {
            final Map<String, Integer> km = new HashMap<>();
            final Map<Integer, String> vm = new HashMap<>();
            final BiMap<String, Integer> bm = new BiMap<>(() -> km, () -> vm);

            // Empty original: the constructor's emptiness/distinctness checks both pass, so copy() has to
            // catch the aliasing itself.
            assertThrows(IllegalArgumentException.class, bm::copy);

            bm.put("y", 9);
            // Once non-empty, the constructor's own emptiness check rejects it first.
            assertThrows(IllegalArgumentException.class, bm::copy);
            assertEquals(1, bm.size());
        }

        @Test
        public void b3_biMap_wellBehavedSuppliersStillCopyIndependently() {
            final BiMap<String, Integer> bm = new BiMap<>(HashMap::new, HashMap::new);
            bm.put("a", 1);

            final BiMap<String, Integer> copy = bm.copy();
            copy.put("b", 2);

            assertEquals(1, bm.size());
            assertEquals(2, copy.size());
        }
    }

    // ------------------------------------------------------------------------------------------------
    // B5 - BiMap.copyOf(BiMap) rebuilt the source through plain HashMaps, losing its key equivalence.
    // ------------------------------------------------------------------------------------------------
    @Nested
    public class B5_CopyOfABiMapPreservesTheSourcesBacking extends TestBase {

        @Test
        public void b5_identityKeyedSourceKeepsBothKeys() {
            final String[] d = equalButDistinct();
            final BiMap<String, Integer> src = new BiMap<>(IdentityHashMap.class, HashMap.class);
            src.put(d[0], 1);
            src.put(d[1], 2);
            assertEquals(2, src.size());

            final BiMap<String, Integer> copy = BiMap.copyOf(src);

            // Previously the copy collapsed to a single entry, silently losing one mapping.
            assertEquals(2, copy.size());
            assertEquals(1, copy.get(d[0]));
            assertEquals(2, copy.get(d[1]));
            assertEquals(src, copy);
        }

        @Test
        public void b5_identityValuedSourceIsNotRejectedAsADuplicate() {
            final String[] d = equalButDistinct();
            final BiMap<String, String> src = new BiMap<>(HashMap.class, IdentityHashMap.class);
            src.put("k1", d[0]);
            src.put("k2", d[1]);
            assertEquals(2, src.size());

            // Previously threw IllegalArgumentException("Value already exists: ...") on a valid source.
            final BiMap<String, String> copy = BiMap.copyOf(src);

            assertEquals(2, copy.size());
            assertSame(d[0], copy.get("k1"));
            assertSame(d[1], copy.get("k2"));
        }

        @Test
        public void b5_copyOfABiMapIsIndependentOfTheSource() {
            final BiMap<String, Integer> src = BiMap.of("a", 1, "b", 2);
            final BiMap<String, Integer> copy = BiMap.copyOf(src);

            copy.put("c", 3);
            assertEquals(2, src.size());
            assertEquals(3, copy.size());

            src.remove("a");
            assertEquals(3, copy.size());
        }

        @Test
        public void b5_copyOfABiMapNoLongerNestsBiMapsAsItsBackingMaps() throws Exception {
            final BiMap<String, Integer> copy = BiMap.copyOf(BiMap.of("a", 1, "b", 2));

            final java.lang.reflect.Field keyMap = BiMap.class.getDeclaredField("keyMap");
            keyMap.setAccessible(true);
            final java.lang.reflect.Field valueMap = BiMap.class.getDeclaredField("valueMap");
            valueMap.setAccessible(true);

            assertFalse(keyMap.get(copy) instanceof BiMap, "backing key map must not itself be a BiMap");
            assertFalse(valueMap.get(copy) instanceof BiMap, "backing value map must not itself be a BiMap");
        }

        @Test
        public void b5_copyOfAPlainMapIsUnchanged() {
            final Map<String, Integer> src = new java.util.LinkedHashMap<>();
            src.put("a", 1);
            src.put("b", 2);

            final BiMap<String, Integer> copy = BiMap.copyOf(src);

            assertEquals(2, copy.size());
            assertEquals(Arrays.asList("a", "b"), new ArrayList<>(copy.keySet()), "a LinkedHashMap source keeps its order");
        }

        @Test
        public void b5_copyOfASortedMapStillPreservesTheComparator() {
            final TreeMap<String, Integer> src = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
            src.put("Alpha", 1);
            src.put("beta", 2);

            final BiMap<String, Integer> copy = BiMap.copyOf(src);

            assertEquals(2, copy.size());
            assertEquals(1, copy.get("ALPHA"), "the source comparator must survive the copy");
            assertEquals(1, copy.copy().get("alpha"), "and survive a further copy()");
        }
    }

    // ------------------------------------------------------------------------------------------------
    // D4 - replaceEntry's List branch collapsed to a single pass; behaviour must be identical.
    // ------------------------------------------------------------------------------------------------
    @Nested
    public class D4_ReplaceEntryListBranch extends TestBase {

        @Test
        public void d4_replacesOnlyTheFirstOccurrenceInARandomAccessList() {
            final ListMultimap<String, String> mm = CommonUtil.newListMultimap();
            mm.putValues("c", Arrays.asList("red", "blue", "red", "green"));

            assertTrue(mm.replaceEntry("c", "red", "orange"));
            assertEquals(Arrays.asList("orange", "blue", "red", "green"), mm.get("c"));
        }

        @Test
        public void d4_replacesOnlyTheFirstOccurrenceInALinkedList() {
            final ListMultimap<String, String> mm = CommonUtil.newListMultimap(HashMap::new, LinkedList::new);
            mm.putValues("c", Arrays.asList("red", "blue", "red", "green"));

            assertTrue(mm.replaceEntry("c", "red", "orange"));
            assertEquals(Arrays.asList("orange", "blue", "red", "green"), mm.get("c"));
        }

        @Test
        public void d4_handlesNullOldAndNewValuesInBothListShapes() {
            final ListMultimap<String, String> arrayBacked = CommonUtil.newListMultimap();
            arrayBacked.putValues("k", Arrays.asList("a", null, "b"));
            assertTrue(arrayBacked.replaceEntry("k", null, "z"));
            assertEquals(Arrays.asList("a", "z", "b"), arrayBacked.get("k"));
            assertTrue(arrayBacked.replaceEntry("k", "b", null));
            assertEquals(Arrays.asList("a", "z", null), arrayBacked.get("k"));

            final ListMultimap<String, String> linked = CommonUtil.newListMultimap(HashMap::new, LinkedList::new);
            linked.putValues("k", Arrays.asList("a", null, "b"));
            assertTrue(linked.replaceEntry("k", null, "z"));
            assertEquals(Arrays.asList("a", "z", "b"), linked.get("k"));
            assertTrue(linked.replaceEntry("k", "b", null));
            assertEquals(Arrays.asList("a", "z", null), linked.get("k"));
        }

        @Test
        public void d4_returnsFalseWhenTheOldValueIsAbsent() {
            final ListMultimap<String, String> mm = ListMultimap.of("k", "a");
            assertFalse(mm.replaceEntry("k", "missing", "z"));
            assertEquals(Collections.singletonList("a"), mm.get("k"));
            assertFalse(mm.replaceEntry("absent", "a", "z"));
        }

        @Test
        public void d4_setBackedMultimapStillRoundTripsThroughRemoveAndAdd() {
            final SetMultimap<String, Integer> sm = CommonUtil.newSetMultimap();
            sm.putValues("n", Arrays.asList(1, 2, 3));

            assertTrue(sm.replaceEntry("n", 2, 20));
            assertEquals(CommonUtil.newLinkedHashSet(Arrays.asList(1, 3, 20)), CommonUtil.newLinkedHashSet(sm.get("n")));
        }
    }

    // ------------------------------------------------------------------------------------------------
    // O2 - wrap() names the offending key instead of dumping the whole map; putAll(null) is an NPE.
    // ------------------------------------------------------------------------------------------------
    @Nested
    public class O2_ErrorMessages extends TestBase {

        @Test
        public void o2_listMultimapWrapNamesTheOffendingKey() {
            final Map<String, List<Integer>> bad = new HashMap<>();
            bad.put("x", new ArrayList<>());

            final String msg = assertThrows(IllegalArgumentException.class, () -> ListMultimap.wrap(bad)).getMessage();
            assertTrue(msg.contains("x"), msg);
        }

        @Test
        public void o2_setMultimapWrapNamesTheOffendingKey() {
            final Map<String, Set<Integer>> bad = new HashMap<>();
            bad.put("x", CommonUtil.newHashSet());

            final String msg = assertThrows(IllegalArgumentException.class, () -> SetMultimap.wrap(bad)).getMessage();
            assertTrue(msg.contains("x"), msg);
        }

        @Test
        public void o2_wrapStillAcceptsAWellFormedMap() {
            final Map<String, List<Integer>> ok = new HashMap<>();
            ok.put("a", new ArrayList<>(Arrays.asList(1, 2)));
            assertEquals(Arrays.asList(1, 2), ListMultimap.wrap(ok).get("a"));

            assertTrue(ListMultimap.wrap(new HashMap<String, List<Integer>>()).isEmpty());
        }

        @Test
        public void o2_biMapPutAllRejectsNullWithANamedNpe() {
            final BiMap<String, Integer> bm = new BiMap<>();

            final String msg = assertThrows(NullPointerException.class, () -> bm.putAll(null)).getMessage();
            assertTrue(msg != null && msg.contains("m"), String.valueOf(msg));

            final String forceMsg = assertThrows(IllegalArgumentException.class, () -> bm.forcePutAll(null)).getMessage();
            assertTrue(forceMsg != null && forceMsg.contains("m"), String.valueOf(forceMsg));
        }
    }

    // ------------------------------------------------------------------------------------------------
    // D6 - BiMap.put dropped two redundant valueMap lookups; every put/forcePut path must be unchanged.
    // ------------------------------------------------------------------------------------------------
    @Nested
    public class D6_BiMapPutIsUnchanged extends TestBase {

        @Test
        public void d6_duplicateValueStillRejected() {
            final BiMap<String, Integer> bm = new BiMap<>();
            bm.put("a", 1);

            assertThrows(IllegalArgumentException.class, () -> bm.put("b", 1));
            assertEquals(1, bm.size());
        }

        @Test
        public void d6_reInsertingTheSameMappingIsANoOp() {
            final BiMap<String, Integer> bm = new BiMap<>(java.util.LinkedHashMap::new, java.util.LinkedHashMap::new);
            bm.put("a", 1);
            bm.put("b", 2);

            assertEquals(1, bm.put("a", 1));
            assertEquals(Arrays.asList("a", "b"), new ArrayList<>(bm.keySet()), "a no-op put must not reorder");
        }

        @Test
        public void d6_replacingAKeysValueKeepsBothDirectionsConsistent() {
            final BiMap<String, Integer> bm = BiMap.of("a", 1);

            assertEquals(1, bm.put("a", 2));
            assertEquals(2, bm.get("a"));
            assertEquals("a", bm.getByValue(2));
            assertNull(bm.getByValue(1));
        }

        @Test
        public void d6_forcePutStillDisplacesAndCanShrink() {
            final BiMap<String, Integer> bm = new BiMap<>();
            bm.put("three", 1);
            bm.put("four", 4);

            assertEquals(1, bm.forcePut("three", 4));
            assertEquals(1, bm.size());
            assertEquals(4, bm.get("three"));
            assertEquals("three", bm.getByValue(4));
            assertNull(bm.get("four"));
        }

        @Test
        public void d6_nullKeyOrValueStillRejected() {
            final BiMap<String, Integer> bm = new BiMap<>();
            assertThrows(IllegalArgumentException.class, () -> bm.put(null, 1));
            assertThrows(IllegalArgumentException.class, () -> bm.put("a", null));
            assertThrows(IllegalArgumentException.class, () -> bm.forcePut(null, 1));
        }

        @Test
        public void d6_replaceAllStillHandlesAFullSwap() {
            final BiMap<String, Integer> bm = new BiMap<>(java.util.LinkedHashMap::new, java.util.LinkedHashMap::new);
            bm.put("a", 1);
            bm.put("b", 2);

            bm.replaceAll((k, v) -> v == 1 ? 2 : 1);

            assertEquals(2, bm.get("a"));
            assertEquals(1, bm.get("b"));
            assertEquals("a", bm.getByValue(2));
            assertEquals("b", bm.getByValue(1));
        }
    }

    // ------------------------------------------------------------------------------------------------
    // D3 - valueCollections() is a modifiable live view; the documented behaviour is pinned here.
    // ------------------------------------------------------------------------------------------------
    @Nested
    public class D3_ValueCollectionsView extends TestBase {

        @Test
        public void d3_removeTakesOutOneMappingWithAnEqualCollection() {
            final ListMultimap<String, Integer> mm = ListMultimap.of("a", 1, "b", 1);

            assertTrue(mm.valueCollections().remove(new ArrayList<>(Collections.singletonList(1))));
            assertEquals(1, mm.keyCount(), "exactly one of the two equal collections is removed");
        }

        @Test
        public void d3_clearEmptiesTheMultimapAndAddIsUnsupported() {
            final ListMultimap<String, Integer> mm = ListMultimap.of("a", 1, "b", 2);

            assertThrows(UnsupportedOperationException.class, () -> mm.valueCollections().add(new ArrayList<>()));

            mm.valueCollections().clear();
            assertTrue(mm.isEmpty());
        }

        @Test
        public void d3_allValuesRemainsUnmodifiable() {
            final ListMultimap<String, Integer> mm = ListMultimap.of("a", 1);
            assertThrows(UnsupportedOperationException.class, () -> mm.allValues().clear());
        }
    }

    // ------------------------------------------------------------------------------------------------
    // Cross-cutting: the fixes must not have changed ordinary behaviour.
    // ------------------------------------------------------------------------------------------------
    @Nested
    public class NoRegressionOnOrdinaryUse extends TestBase {

        @Test
        public void multisetIteratorAndCountsStillBehave() {
            final Multiset<String> ms = Multiset.of("a", "a", "b");
            assertEquals(3, ms.size());
            assertEquals(2, ms.countOfDistinctElements());
            assertEquals(2, ms.getCount("a"));

            final List<String> seen = new ArrayList<>();
            ms.iterator().forEachRemaining(seen::add);
            assertEquals(3, seen.size());
            assertEquals(2, Collections.frequency(seen, "a"));
        }

        @Test
        public void multisetSupportsNullElementsWithAHashMapBacking() {
            final Multiset<String> ms = new Multiset<>();
            assertTrue(ms.add(null));
            assertEquals(1, ms.getCount(null));
            assertTrue(ms.contains(null));
            // Explicit cast: an unqualified null binds to the Collection overload, not the Object one.
            assertEquals(1, ms.removeAllOccurrencesOf((Object) null));
            assertFalse(ms.contains(null));
        }

        @Test
        public void multimapInvertAndCopyStillWork() {
            final ListMultimap<String, Integer> mm = CommonUtil.newListMultimap();
            mm.putValues("g1", Arrays.asList(1, 2));
            mm.put("g2", 1);

            final ListMultimap<Integer, String> inverted = mm.invert();
            assertEquals(2, inverted.get(1).size());
            assertEquals(Collections.singletonList("g1"), inverted.get(2));

            final ListMultimap<String, Integer> copy = mm.copy();
            assertEquals(mm, copy);
            copy.put("g1", 99);
            assertEquals(2, mm.get("g1").size());
        }

        @Test
        public void multimapSuppliedInvertStillWorks() {
            final ListMultimap<String, Integer> mm = ListMultimap.of("a", 1, "b", 1);
            final ListMultimap<Integer, String> inverted = mm.invert(N::newListMultimap);
            assertEquals(2, inverted.get(1).size());
        }

        @Test
        public void biMapViewsAndInverseStillWork() {
            final BiMap<String, Integer> bm = BiMap.of("a", 1, "b", 2);

            assertEquals(2, bm.keySet().size());
            assertEquals(2, bm.values().size());
            assertEquals(2, bm.entrySet().size());
            assertThrows(UnsupportedOperationException.class, () -> bm.keySet().remove("a"));

            assertSame(bm, bm.inverse().inverse());
            assertEquals("a", bm.inverse().get(1));
        }
    }
}

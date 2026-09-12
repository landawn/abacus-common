package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.AbstractMap;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.Builder.ListBuilder;
import com.landawn.abacus.util.Builder.MapBuilder;
import com.landawn.abacus.util.Builder.MultisetBuilder;
import com.landawn.abacus.util.Difference.BeanDifference;
import com.landawn.abacus.util.Difference.MapDifference;
import com.landawn.abacus.util.function.ToBooleanFunction;
import com.landawn.abacus.util.function.ToByteFunction;
import com.landawn.abacus.util.function.ToCharFunction;
import com.landawn.abacus.util.function.ToFloatFunction;
import com.landawn.abacus.util.function.ToShortFunction;

/**
 * Regression tests for the 2026-09-01 {@code Builder} / {@code Difference} / {@code Comparators} review.
 *
 * <p>{@link MapDifferenceInconsistentKeyEquality} and {@link SerializableKeyExtractorComparators} fail on the
 * pre-fix sources; the rest lock behaviour that the review either corrected in the javadoc or renamed, so it
 * cannot drift back.
 */
@SuppressWarnings("unchecked")
public class BuilderDifferenceComparatorsTest extends TestBase {

    private static BigDecimal bd(final String s) {
        return new BigDecimal(s);
    }

    /** An insertion-ordered map; {@code N} has no {@code asLinkedHashMap} of this arity. */
    private static <K, V> LinkedHashMap<K, V> ordered(final Object... kv) {
        final LinkedHashMap<K, V> m = new LinkedHashMap<>();
        for (int i = 0; i < kv.length; i += 2) {
            m.put((K) kv[i], (V) kv[i + 1]);
        }
        return m;
    }

    private static TreeMap<BigDecimal, String> treeOf(final String key, final String value) {
        final TreeMap<BigDecimal, String> m = new TreeMap<>();
        m.put(bd(key), value);
        return m;
    }

    // ---------------------------------------------------------------------------------------------------
    // B1 + B2 - MapDifference must never return a self-contradictory partition.
    // ---------------------------------------------------------------------------------------------------

    @Nested
    public class MapDifferenceInconsistentKeyEquality {

        /**
         * B1: a right-hand map whose key equality is coarser than {@code equals} but whose type does not
         * reveal it. Before the fix this returned {@code common={2.0=x}} AND {@code onlyOnRight={2.00=x}} -
         * the same entry in two mutually exclusive buckets, with {@code areEqual()==false} even though
         * {@code map1.equals(map2)}.
         */
        @Test
        public void unmodifiableMapOverTreeMapIsRejected() {
            final Map<BigDecimal, String> m1 = Collections.unmodifiableMap(treeOf("2.0", "x"));
            final Map<BigDecimal, String> m2 = Collections.unmodifiableMap(treeOf("2.00", "x"));

            assertTrue(m1.equals(m2), "precondition: the two maps are equal");

            final IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> MapDifference.of(m1, m2));
            assertTrue(e.getMessage().contains("Cannot compute a consistent difference"), e.getMessage());
            assertTrue(e.getMessage().contains("UnmodifiableMap"), e.getMessage());
        }

        /** B1, value-difference variant: previously produced differentValues AND onlyOnRight for one entry. */
        @Test
        public void unmodifiableMapOverTreeMapIsRejectedForDifferingValues() {
            final TreeMap<BigDecimal, Integer> t1 = new TreeMap<>();
            t1.put(bd("2.0"), 1);
            final TreeMap<BigDecimal, Integer> t2 = new TreeMap<>();
            t2.put(bd("2.00"), 2);

            assertThrows(IllegalArgumentException.class, () -> MapDifference.of(Collections.unmodifiableMap(t1), Collections.unmodifiableMap(t2)));
        }

        /** B1 through this library's own immutable wrapper, which is likewise not a {@code SortedMap}. */
        @Test
        public void immutableMapWrappingTreeMapIsRejected() {
            assertThrows(IllegalArgumentException.class, () -> MapDifference.of(ImmutableMap.wrap(treeOf("2.0", "x")), ImmutableMap.wrap(treeOf("2.00", "x"))));
        }

        /** The same shape reached through a key selection is rejected too. */
        @Test
        public void unmodifiableMapOverTreeMapIsRejectedWithKeySelection() {
            final Map<BigDecimal, String> m1 = Collections.unmodifiableMap(treeOf("2.0", "x"));
            final Map<BigDecimal, String> m2 = Collections.unmodifiableMap(treeOf("2.00", "x"));

            assertThrows(IllegalArgumentException.class, () -> MapDifference.of(m1, m2, new TreeSet<>(Arrays.asList(bd("2.0")))));
        }

        /**
         * B2: the left-hand map's key equality is <i>finer</i> than the right-hand map's, so two left
         * entries resolve to one right entry. Before the fix {@code areEqual()} returned {@code true} for a
         * two-entry map compared against a one-entry map.
         */
        @Test
        public void finerLeftHandKeyEqualityIsRejected() {
            final Map<BigDecimal, String> m1 = new LinkedHashMap<>();
            m1.put(bd("2.0"), "x");
            m1.put(bd("2.00"), "x");

            assertEquals(2, m1.size());
            final Map<BigDecimal, String> m2 = treeOf("2.0", "x");
            assertEquals(1, m2.size());

            assertThrows(IllegalArgumentException.class, () -> MapDifference.of(m1, m2));
        }

        /** B2 via identity keys on the left: two equal-but-distinct keys claim the same right entry. */
        @Test
        public void identityKeyedLeftMapWithEqualKeysIsRejected() {
            final Map<String, Integer> m1 = new IdentityHashMap<>();
            m1.put(new String("a"), 1); // NOSONAR - a distinct instance is the point of the test
            m1.put(new String("a"), 1); // NOSONAR
            assertEquals(2, m1.size());

            assertThrows(IllegalArgumentException.class, () -> MapDifference.of(m1, CommonUtil.asMap("a", 1)));
        }

        /** B2 restricted to a key selection is rejected as well. */
        @Test
        public void finerLeftHandKeyEqualityIsRejectedWithKeySelection() {
            final Map<BigDecimal, String> m1 = new LinkedHashMap<>();
            m1.put(bd("2.0"), "x");
            m1.put(bd("2.00"), "x");

            assertThrows(IllegalArgumentException.class, () -> MapDifference.of(m1, treeOf("2.0", "x"), new TreeSet<>(Arrays.asList(bd("2.0"), bd("2.00")))));
        }

        // ----- the sorted views that DO expose their relation are still repaired, not rejected -----

        @Test
        public void plainTreeMapPairIsStillRepaired() {
            final MapDifference<Map<BigDecimal, String>, Map<BigDecimal, String>, Map<BigDecimal, Pair<String, String>>> diff = MapDifference
                    .of(treeOf("2.0", "x"), treeOf("2.00", "x"));

            assertTrue(diff.areEqual());
            assertTrue(diff.onlyOnRight().isEmpty());
            assertEquals(1, diff.common().size());
        }

        @Test
        public void unmodifiableSortedMapIsRepaired() {
            final MapDifference<Map<BigDecimal, String>, Map<BigDecimal, String>, Map<BigDecimal, Pair<String, String>>> diff = MapDifference
                    .of(Collections.unmodifiableSortedMap(treeOf("2.0", "x")), Collections.unmodifiableSortedMap(treeOf("2.00", "x")));

            assertTrue(diff.areEqual());
            assertTrue(diff.onlyOnRight().isEmpty());
        }

        @Test
        public void immutableSortedMapIsRepaired() {
            final MapDifference<Map<BigDecimal, String>, Map<BigDecimal, String>, Map<BigDecimal, Pair<String, String>>> diff = MapDifference
                    .of(ImmutableSortedMap.wrap(treeOf("2.0", "x")), ImmutableSortedMap.wrap(treeOf("2.00", "x")));

            assertTrue(diff.areEqual());
            assertTrue(diff.onlyOnRight().isEmpty());
        }

        @Test
        public void identityHashMapPairIsRepaired() {
            final String shared = "k";
            final Map<String, Integer> m1 = new IdentityHashMap<>();
            m1.put(shared, 1);
            final Map<String, Integer> m2 = new IdentityHashMap<>();
            m2.put(shared, 1);

            assertTrue(MapDifference.of(m1, m2).areEqual());
        }

        // ----- no false positives on ordinary inputs -----

        @Test
        public void ordinaryHashMapsAreUnaffected() {
            final Map<String, Integer> m1 = ordered("a", 1, "b", 2, "c", 3);
            final Map<String, Integer> m2 = ordered("b", 2, "c", 9, "d", 4);

            final MapDifference<Map<String, Integer>, Map<String, Integer>, Map<String, Pair<Integer, Integer>>> diff = MapDifference.of(m1, m2);

            assertEquals(CommonUtil.asMap("b", 2), diff.common());
            assertEquals(CommonUtil.asMap("a", 1), diff.onlyOnLeft());
            assertEquals(CommonUtil.asMap("d", 4), diff.onlyOnRight());
            assertEquals(1, diff.differentValues().size());
            assertFalse(diff.areEqual());
        }

        @Test
        public void keySelectionIsUnaffected() {
            final Map<String, Integer> m1 = ordered("a", 1, "b", 2, "c", 3);
            final Map<String, Integer> m2 = ordered("b", 2, "c", 9, "d", 4);

            final MapDifference<Map<String, Integer>, Map<String, Integer>, Map<String, Pair<Integer, Integer>>> diff = MapDifference.of(m1, m2,
                    Arrays.asList("a", "b"));

            assertEquals(CommonUtil.asMap("b", 2), diff.common());
            assertEquals(CommonUtil.asMap("a", 1), diff.onlyOnLeft());
            assertTrue(diff.onlyOnRight().isEmpty());
            assertTrue(diff.differentValues().isEmpty());
        }

        @Test
        public void nullValuesOnBothSidesAreUnaffected() {
            final Map<String, String> m1 = new LinkedHashMap<>();
            m1.put("a", null);
            m1.put("b", null);
            final Map<String, String> m2 = new LinkedHashMap<>();
            m2.put("a", null);
            m2.put("c", null);

            final MapDifference<Map<String, String>, Map<String, String>, Map<String, Pair<String, String>>> diff = MapDifference.of(m1, m2);

            assertEquals(Set.of("a"), diff.common().keySet());
            assertEquals(Set.of("b"), diff.onlyOnLeft().keySet());
            assertEquals(Set.of("c"), diff.onlyOnRight().keySet());
        }

        @Test
        public void emptyAndOneSidedInputsAreUnaffected() {
            assertTrue(MapDifference.of(new HashMap<String, Integer>(), new HashMap<String, Integer>()).areEqual());
            assertEquals(CommonUtil.asMap("a", 1), MapDifference.of(CommonUtil.asMap("a", 1), new HashMap<String, Integer>()).onlyOnLeft());
            assertEquals(CommonUtil.asMap("a", 1), MapDifference.of(new HashMap<String, Integer>(), CommonUtil.asMap("a", 1)).onlyOnRight());
        }

        @Test
        public void customValueEquivalenceIsUnaffected() {
            final Map<String, Integer> m1 = ordered("a", 100, "b", 5);
            final Map<String, Integer> m2 = ordered("a", 105, "b", 99);

            final MapDifference<Map<String, Integer>, Map<String, Integer>, Map<String, Pair<Integer, Integer>>> diff = MapDifference.ofByValues(m1, m2,
                    (v1, v2) -> Math.abs(v1 - v2) <= 10);

            assertEquals(Set.of("a"), diff.common().keySet());
            assertEquals(Set.of("b"), diff.differentValues().keySet());
        }

        /** A concurrent hash map keys by equals/hashCode, so it takes the allocation-free path unharmed. */
        @Test
        public void concurrentHashMapIsUnaffected() {
            final Map<String, Integer> m1 = new ConcurrentHashMap<>(CommonUtil.asMap("a", 1));
            final Map<String, Integer> m2 = new ConcurrentHashMap<>(CommonUtil.asMap("a", 1));

            assertTrue(MapDifference.of(m1, m2).areEqual());
        }

        /**
         * The invariant must not fire for any map type whose keys really are compared by
         * {@code equals}/{@code hashCode}, nor for a sorted/identity pair that agrees with itself. Each of
         * these was checked against the fix; a regression here would break ordinary callers.
         */
        @Test
        public void legitimateMapShapesAreNeverRejected() {
            final Map<String, Integer> shared = ordered("a", 1, "b", 2);
            assertTrue(MapDifference.of(shared, shared).areEqual(), "comparing a map with itself");

            final Map<java.time.DayOfWeek, Integer> e1 = new EnumMap<>(java.time.DayOfWeek.class);
            final Map<java.time.DayOfWeek, Integer> e2 = new EnumMap<>(java.time.DayOfWeek.class);
            e1.put(java.time.DayOfWeek.MONDAY, 1);
            e2.put(java.time.DayOfWeek.MONDAY, 1);
            assertTrue(MapDifference.of(e1, e2).areEqual(), "EnumMap");

            assertTrue(MapDifference.of(new Hashtable<>(CommonUtil.asMap("a", 1)), new Hashtable<>(CommonUtil.asMap("a", 1))).areEqual(), "Hashtable");

            final TreeMap<String, Integer> ci1 = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
            final TreeMap<String, Integer> ci2 = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
            ci1.put("A", 1);
            ci2.put("a", 1);
            assertTrue(MapDifference.of(ci1, ci2).areEqual(), "two case-insensitive TreeMaps");

            // one left key resolving to one right key is fine even when the two relations differ
            assertEquals(CommonUtil.asMap("A", 1), MapDifference.of(CommonUtil.asMap("A", 1), ci2).common(), "HashMap vs case-insensitive TreeMap, 1:1");

            final TreeMap<String, Integer> rev1 = new TreeMap<>(Comparator.reverseOrder());
            final TreeMap<String, Integer> rev2 = new TreeMap<>(Comparator.reverseOrder());
            rev1.put("a", 1);
            rev2.put("a", 1);
            assertTrue(MapDifference.of(rev1, rev2).areEqual(), "TreeMap with a reversing comparator");

            final Map<String, Integer> withNullKey1 = new LinkedHashMap<>();
            final Map<String, Integer> withNullKey2 = new LinkedHashMap<>();
            withNullKey1.put(null, 1);
            withNullKey2.put(null, 1);
            assertTrue(MapDifference.of(withNullKey1, withNullKey2).areEqual(), "null key on both sides");

            // an identity-keyed left map is fine as long as no two of its keys collapse on the right
            final Map<String, Integer> identity = new IdentityHashMap<>();
            identity.put(new String("a"), 1); // NOSONAR
            assertTrue(MapDifference.of(identity, CommonUtil.asMap("a", 1)).areEqual(), "IdentityHashMap vs HashMap, 1:1");
        }

        @Test
        public void legitimateKeySelectionsAreNeverRejected() {
            assertTrue(MapDifference.of(CommonUtil.asMap("a", 1), CommonUtil.asMap("a", 1), Arrays.asList("zzz")).areEqual(), "selection matching nothing");
            assertTrue(MapDifference.of(CommonUtil.asMap("a", 1), CommonUtil.asMap("b", 2), Arrays.asList("c")).areEqual(), "selection in neither map");

            final Map<String, Integer> m1 = new LinkedHashMap<>();
            final Map<String, Integer> m2 = new LinkedHashMap<>();
            m1.put("a", 1);
            m1.put(null, 9);
            m2.put("a", 1);
            m2.put(null, 9);
            assertTrue(MapDifference.of(m1, m2, new ArrayList<>(Arrays.asList("a", "a", null))).areEqual(), "List selection with a duplicate and a null");
        }

        /**
         * The one place the check is knowingly conservative: the right-hand map hides a coarser relation,
         * but the equals-based key selection also hides it, so the unchecked result would have been right by
         * accident. The same pair without a selection genuinely double-files, so this is rejected too.
         */
        @Test
        public void coarseRightMapIsRejectedEvenWhenTheSelectionWouldHaveMaskedIt() {
            final Map<BigDecimal, String> m1 = Collections.unmodifiableMap(treeOf("2.0", "x"));
            final Map<BigDecimal, String> m2 = Collections.unmodifiableMap(treeOf("2.00", "x"));

            assertThrows(IllegalArgumentException.class, () -> MapDifference.of(m1, m2, new HashSet<>(Arrays.asList(bd("2.0")))));

            // and the sorted view, which exposes the relation, is accepted
            assertTrue(
                    MapDifference
                            .of(Collections.unmodifiableSortedMap(treeOf("2.0", "x")), Collections.unmodifiableSortedMap(treeOf("2.00", "x")),
                                    new TreeSet<>(Arrays.asList(bd("2.0"))))
                            .areEqual());
        }

        /** The nested collection-of-maps factories run the per-entry comparison through the same path. */
        @Test
        public void collectionOfMapsVariantIsUnaffected() {
            final List<Map<String, Object>> a = Arrays.asList(CommonUtil.asMap("id", "1", "v", "x"));
            final List<Map<String, Object>> b = Arrays.asList(CommonUtil.asMap("id", "1", "v", "y"));

            assertEquals(1, ((Map<?, ?>) MapDifference.of(a, b, m -> m.get("id")).differentValues()).size());
        }

        /**
         * ... and therefore propagate the rejection: each matched pair goes through
         * {@code MapDifference.of(mapA, mapB, keysToCompare)}, so an inconsistent pair is reported from the
         * collection-level factory too rather than silently producing a bad nested difference.
         */
        @Test
        public void collectionOfMapsVariantPropagatesTheRejection() {
            // "K" and "k" are two distinct keys on the left but one key on the case-insensitive right,
            // so the matched pair cannot be partitioned - exactly the shape rejected above, one level down.
            final Map<String, String> left = new LinkedHashMap<>();
            left.put("id", "1");
            left.put("K", "x");
            left.put("k", "x");

            final Map<String, String> right = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
            right.put("id", "1");
            right.put("k", "x");

            assertThrows(IllegalArgumentException.class, () -> MapDifference.of(Arrays.asList(left), Arrays.asList(right), m -> m.get("id")));

            // the same pair without the collapsing key compares cleanly
            left.remove("K");
            assertTrue(MapDifference.of(Arrays.asList(left), Arrays.asList(right), m -> m.get("id")).areEqual());
        }
    }

    // ---------------------------------------------------------------------------------------------------
    // B3 - a null key/property selection must be expressible, so the predicate overloads were renamed.
    // ---------------------------------------------------------------------------------------------------

    @Nested
    public class NullSelectionIsUnambiguous {

        @Test
        public void mapDifferenceAcceptsNullKeysToCompare() {
            final Map<String, Integer> m1 = ordered("a", 1, "b", 2);
            final Map<String, Integer> m2 = ordered("b", 2, "c", 3);

            // Documented as "null or empty -> all keys"; before the rename this call did not compile.
            assertEquals(MapDifference.of(m1, m2).common(), MapDifference.of(m1, m2, null).common());
            assertEquals(MapDifference.of(m1, m2).onlyOnRight(), MapDifference.of(m1, m2, null).onlyOnRight());
        }

        @Test
        public void beanDifferenceAcceptsNullPropNamesToCompare() {
            final ReviewBean b1 = new ReviewBean("x", 1);
            final ReviewBean b2 = new ReviewBean("x", 2);

            assertEquals(BeanDifference.of(b1, b2).common(), BeanDifference.of(b1, b2, null).common());
            assertEquals(BeanDifference.of(b1, b2).differentValues(), BeanDifference.of(b1, b2, null).differentValues());
        }

        @Test
        public void renamedMapFactoriesKeepTheirBehaviour() {
            final Map<String, String> m1 = ordered("k", "VALUE", "other", "a");
            final Map<String, String> m2 = ordered("k", "value", "other", "b");

            assertEquals(Set.of("k"), MapDifference.ofByValues(m1, m2, (v1, v2) -> v1.equalsIgnoreCase(v2)).common().keySet());
            assertEquals(Set.of("k"), MapDifference.ofByEntries(m1, m2, (k, v1, v2) -> "k".equals(k) || CommonUtil.equals(v1, v2)).common().keySet());
            assertEquals(Set.of("k"), MapDifference.ofByEntries(m1, m2, Arrays.asList("k"), (k, v1, v2) -> v1.equalsIgnoreCase(v2)).common().keySet());
        }

        @Test
        public void renamedBeanFactoriesKeepTheirBehaviour() {
            final ReviewBean b1 = new ReviewBean("NAME", 1);
            final ReviewBean b2 = new ReviewBean("name", 1);

            assertTrue(BeanDifference.ofByValues(b1, b2, (v1, v2) -> CommonUtil.equals(v1, v2) || String.valueOf(v1).equalsIgnoreCase(String.valueOf(v2)))
                    .areEqual());
            assertTrue(BeanDifference.ofByProps(b1, b2, (p, v1, v2) -> "name".equals(p) || CommonUtil.equals(v1, v2)).areEqual());
            assertTrue(BeanDifference.ofByProps(b1, b2, Arrays.asList("value"), (p, v1, v2) -> CommonUtil.equals(v1, v2)).areEqual());
        }

        @Test
        public void renamedFactoriesStillRejectANullPredicate() {
            final Map<String, Integer> m = new HashMap<>();
            assertThrows(IllegalArgumentException.class, () -> MapDifference.ofByValues(m, m, null));
            assertThrows(IllegalArgumentException.class, () -> MapDifference.ofByEntries(m, m, null));
            assertThrows(IllegalArgumentException.class, () -> MapDifference.ofByEntries(m, m, Arrays.asList("a"), null));

            final ReviewBean b = new ReviewBean("x", 1);
            assertThrows(IllegalArgumentException.class, () -> BeanDifference.ofByValues(b, b, null));
            assertThrows(IllegalArgumentException.class, () -> BeanDifference.ofByProps(b, b, null));
            assertThrows(IllegalArgumentException.class, () -> BeanDifference.ofByProps(b, b, Arrays.asList("name"), null));
        }

        /** B4: the inherited primitive-list factories only get in the way of a bare {@code null} literal. */
        @Test
        public void typedNullArgumentsStillWork() {
            assertTrue(MapDifference.of((Map<String, Integer>) null, (Map<String, Integer>) null).areEqual());
            assertTrue(BeanDifference.of((Object) null, (Object) null).areEqual());
        }
    }

    // ---------------------------------------------------------------------------------------------------
    // Builder - the renames and the documented-but-untested behaviour.
    // ---------------------------------------------------------------------------------------------------

    @Nested
    public class BuilderApiFixes {

        /** D5: the two removal forms can no longer collide when the element type is itself a collection. */
        @Test
        public void removeAllOccurrencesOfRemovesTheElementItself() {
            final Multiset<List<String>> ms = new Multiset<>();
            final List<String> element = Arrays.asList("x", "y");
            ms.add(element, 3);
            ms.add(Arrays.asList("x"), 2);

            Builder.of(ms).removeAllOccurrencesOf(element);

            assertEquals(0, ms.getCount(element));
            assertEquals(2, ms.getCount(Arrays.asList("x")));
        }

        @Test
        public void removeAllOccurrencesOfAllRemovesEachElement() {
            final Multiset<String> ms = new Multiset<>();
            ms.add("apple", 2);
            ms.add("banana", 3);
            ms.add("cherry", 1);

            final MultisetBuilder<String> b = Builder.of(ms);
            assertSame(b, b.removeAllOccurrencesOfAll(Arrays.asList("apple", "banana")));

            assertEquals(0, ms.getCount("apple"));
            assertEquals(0, ms.getCount("banana"));
            assertEquals(1, ms.getCount("cherry"));
        }

        @Test
        public void deprecatedRemoveAllStillDelegatesToTheBulkForm() {
            final Multiset<String> ms = new Multiset<>();
            ms.add("a", 2);
            ms.add("b", 1);

            Builder.of(ms).removeAll(Arrays.asList("a"));

            assertEquals(0, ms.getCount("a"));
            assertEquals(1, ms.getCount("b"));
        }

        /** D6: on a {@code Map<String, Object>} the supplier form used to shadow the value form. */
        @Test
        public void putIfAbsentBySupplierNoLongerShadowsTheValueOverload() {
            final Supplier<Object> supplier = () -> "produced";

            final Map<String, Object> viaSupplier = new LinkedHashMap<>();
            Builder.of(viaSupplier).putIfAbsentBySupplier("k", supplier);
            assertEquals("produced", viaSupplier.get("k"));

            // The value overload now stores the supplier itself, with no cast needed at the call site.
            final Map<String, Object> viaValue = new LinkedHashMap<>();
            Builder.of(viaValue).putIfAbsent("k", supplier);
            assertSame(supplier, viaValue.get("k"));
        }

        @Test
        public void putIfAbsentBySupplierSkipsAPresentKeyAndRejectsNull() {
            final Map<String, Integer> m = new LinkedHashMap<>();
            m.put("a", 95);

            final MapBuilder<String, Integer, Map<String, Integer>> b = Builder.of(m);
            assertSame(b, b.putIfAbsentBySupplier("a", () -> {
                throw new AssertionError("supplier must not be invoked for a present key");
            }));
            assertEquals(95, m.get("a"));

            b.putIfAbsentBySupplier("b", () -> 87);
            assertEquals(87, m.get("b"));

            assertThrows(IllegalArgumentException.class, () -> b.putIfAbsentBySupplier("c", null));
        }

        /** D4: dropping the four unreachable interface keys must not change any dispatch. */
        @Test
        public void ofDispatchesToTheSameSpecializedBuilders() {
            assertInstanceOf(ListBuilder.class, Builder.of((Object) new ArrayList<String>()));
            assertInstanceOf(ListBuilder.class, Builder.of((Object) new LinkedList<String>()));
            assertInstanceOf(ListBuilder.class, Builder.of((Object) new Vector<String>()));
            assertInstanceOf(Builder.CollectionBuilder.class, Builder.of((Object) new HashSet<String>()));
            assertInstanceOf(Builder.CollectionBuilder.class, Builder.of((Object) new LinkedHashSet<String>()));
            assertInstanceOf(Builder.CollectionBuilder.class, Builder.of((Object) new TreeSet<String>()));
            assertInstanceOf(Builder.CollectionBuilder.class, Builder.of((Object) new ArrayDeque<String>()));
            assertInstanceOf(MapBuilder.class, Builder.of((Object) new HashMap<String, String>()));
            assertInstanceOf(MapBuilder.class, Builder.of((Object) new LinkedHashMap<String, String>()));
            assertInstanceOf(MapBuilder.class, Builder.of((Object) new TreeMap<String, String>()));
            assertInstanceOf(MapBuilder.class, Builder.of((Object) new ConcurrentHashMap<String, String>()));
            assertInstanceOf(MultisetBuilder.class, Builder.of((Object) new Multiset<String>()));
            assertInstanceOf(Builder.MultimapBuilder.class, Builder.of((Object) CommonUtil.newListMultimap()));
            assertInstanceOf(Builder.MultimapBuilder.class, Builder.of((Object) CommonUtil.newSetMultimap()));
            assertInstanceOf(Builder.MultimapBuilder.class,
                    Builder.of((Object) CommonUtil.newMultimap((Supplier<Map<String, List<Integer>>>) HashMap::new, (Supplier<List<Integer>>) ArrayList::new)));
            assertInstanceOf(Builder.DatasetBuilder.class, Builder.of((Object) Dataset.rows(Arrays.asList("a"), new Object[][] { { 1 } })));
            assertInstanceOf(Builder.class, Builder.of((Object) "plain"));
        }

        /** D7: an {@code int} argument always picks the index-based overload, as on {@link List}. */
        @Test
        public void listBuilderRemoveIntIsIndexBased() {
            final List<Integer> byIndex = new ArrayList<>(Arrays.asList(10, 20, 30));
            Builder.of(byIndex).remove(1);
            assertEquals(Arrays.asList(10, 30), byIndex);

            final List<Integer> byValue = new ArrayList<>(Arrays.asList(10, 20, 30));
            Builder.of(byValue).remove((Object) 20);
            assertEquals(Arrays.asList(10, 30), byValue);
        }

        /** D8: the custom predicate is handed the raw arguments, {@code null}s included. */
        @Test
        public void equivalencePredicateReceivesNullsUnchanged() {
            final List<String> seen = new ArrayList<>();

            final boolean result = Builder.equals((String) null, (String) null, (a, b) -> {
                seen.add(a + "/" + b);
                return a == null ? b == null : a.equalsIgnoreCase(b);
            }).result();

            assertTrue(result);
            assertEquals(Arrays.asList("null/null"), seen);
            assertThrows(NullPointerException.class, () -> Builder.equals((String) null, "x", (a, b) -> a.equalsIgnoreCase(b)).result());
        }

        /** D1: {@code compare} and {@code compareNullLess} are documented as the same comparison. */
        @Test
        public void compareAndCompareNullLessAgreeOnEveryNullCombination() {
            final String[][] pairs = { { null, null }, { null, "a" }, { "a", null }, { "a", "b" }, { "b", "a" }, { "a", "a" } };

            for (final String[] p : pairs) {
                assertEquals(Integer.signum(Builder.compare(p[0], p[1]).result()), Integer.signum(Builder.compareNullLess(p[0], p[1]).result()),
                        Arrays.toString(p));
            }
        }

        /** J4: the primitive builders tolerate a null argument, and validate the index before using it. */
        @Test
        public void primitiveListBuilderNullAndIndexContract() {
            assertEquals(IntList.of(1, 2), Builder.of(IntList.of(1, 2)).addAll((IntList) null).val());
            assertEquals(IntList.of(1, 2), Builder.of(IntList.of(1, 2)).removeAll((IntList) null).val());
            assertEquals(IntList.of(1, 2), Builder.of(IntList.of(1, 2)).addAll(0, (IntList) null).val());

            assertThrows(IndexOutOfBoundsException.class, () -> Builder.of(IntList.of(1, 2)).addAll(99, (IntList) null));
            assertThrows(IndexOutOfBoundsException.class, () -> Builder.of(IntList.of(1, 2)).addAll(99, IntList.of()));

            assertEquals(DoubleList.of(1.0), Builder.of(DoubleList.of(1.0)).addAll((DoubleList) null).val());
            assertEquals(CharList.of('a'), Builder.of(CharList.of('a')).removeAll((CharList) null).val());
        }

        /** J2 + J3: the fuzzy overloads follow {@code Numbers.fuzzyEquals}/{@code fuzzyCompare}, not |a-b|. */
        @Test
        public void fuzzyComparisonSpecialValues() {
            assertTrue(Builder.equals(Double.NaN, Double.NaN, 0.0).result());
            assertTrue(Builder.equals(Float.NaN, Float.NaN, 0.0f).result());
            assertTrue(Builder.equals(Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, 0.0).result());
            assertFalse(Builder.equals(Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY, 1e300).result());
            assertTrue(Builder.equals(0.0, -0.0, 0.0).result());

            assertEquals(0, Builder.compare(Double.NaN, Double.NaN, 0.0).result());
            assertTrue(Builder.compare(Double.NaN, Double.POSITIVE_INFINITY, 1.0).result() > 0, "NaN sorts above +Infinity");
            assertTrue(Builder.compare(Double.POSITIVE_INFINITY, Double.NaN, 1.0).result() < 0);

            // the tolerance is still validated eagerly, before any short-circuit
            assertThrows(IllegalArgumentException.class, () -> Builder.compare(1, 2).compare(1.0, 2.0, -1.0));
            assertThrows(IllegalArgumentException.class, () -> Builder.equals(1, 2).equals(1.0, 2.0, Double.NaN));
        }
    }

    // ---------------------------------------------------------------------------------------------------
    // DatasetBuilder - B5 and J1.
    // ---------------------------------------------------------------------------------------------------

    @Nested
    public class DatasetBuilderContract {

        private Dataset ymd() {
            return Dataset.rows(Arrays.asList("year", "month", "day"), new Object[][] { { 2020, 1, 2 }, { 2021, 3, 4 } });
        }

        /** B5: the published example used {@code LocalDate.class}, which is not a supported row type. */
        @Test
        public void combineColumnsSupportsTheDocumentedRowTypes() {
            final Dataset asList = ymd();
            Builder.of(asList).combineColumns(Arrays.asList("year", "month", "day"), "date", List.class);
            assertEquals(Arrays.asList("date"), new ArrayList<>(asList.columnNames()));
            assertEquals(Arrays.asList(2020, 1, 2), asList.getColumn("date").get(0));

            final Dataset asMap = ymd();
            Builder.of(asMap).combineColumns(Arrays.asList("year", "month", "day"), "date", Map.class);
            assertEquals(CommonUtil.asMap("year", 2020, "month", 1, "day", 2), asMap.getColumn("date").get(0));

            final Dataset asArray = ymd();
            Builder.of(asArray).combineColumns(Arrays.asList("year", "month", "day"), "date", Object[].class);
            assertArrayEquals(new Object[] { 2020, 1, 2 }, (Object[]) asArray.getColumn("date").get(0));
        }

        @Test
        public void combineColumnsRejectsAnUnsupportedRowType() {
            final Dataset ds = ymd();
            assertThrows(IllegalArgumentException.class, () -> Builder.of(ds).combineColumns(Arrays.asList("year", "month", "day"), "date", LocalDate.class));
        }

        /** J1: the four rename methods throw on a frozen Dataset, like every other mutator. */
        @Test
        public void renameOnAFrozenDatasetThrows() {
            final Dataset frozen = ymd();
            frozen.freeze();

            assertThrows(IllegalStateException.class, () -> Builder.of(frozen).renameColumn("year", "y"));
            assertThrows(IllegalStateException.class, () -> Builder.of(frozen).renameColumns(CommonUtil.asMap("year", "y")));
            assertThrows(IllegalStateException.class, () -> Builder.of(frozen).renameColumns(Arrays.asList("year"), String::toUpperCase));
            assertThrows(IllegalStateException.class, () -> Builder.of(frozen).renameColumns(String::toUpperCase));
        }
    }

    // ---------------------------------------------------------------------------------------------------
    // Comparators - D9 serializability, plus the behaviour the corrected javadoc now states.
    // ---------------------------------------------------------------------------------------------------

    /** A serializable key extractor, so the comparator's own serializability is what is under test. */
    interface SerToInt<T> extends java.util.function.ToIntFunction<T>, Serializable {
    }

    interface SerToChar<T> extends ToCharFunction<T>, Serializable {
    }

    interface SerToFloat<T> extends ToFloatFunction<T>, Serializable {
    }

    interface SerToBoolean<T> extends ToBooleanFunction<T>, Serializable {
    }

    interface SerToByte<T> extends ToByteFunction<T>, Serializable {
    }

    interface SerToShort<T> extends ToShortFunction<T>, Serializable {
    }

    interface SerToLong<T> extends java.util.function.ToLongFunction<T>, Serializable {
    }

    interface SerToDouble<T> extends java.util.function.ToDoubleFunction<T>, Serializable {
    }

    @Nested
    public class SerializableKeyExtractorComparators {

        @SuppressWarnings("unchecked")
        private <T> Comparator<T> roundTrip(final Comparator<T> cmp) throws Exception {
            assertInstanceOf(Serializable.class, cmp);

            final java.io.ByteArrayOutputStream bo = new java.io.ByteArrayOutputStream();
            try (ObjectOutputStream oo = new ObjectOutputStream(bo)) {
                oo.writeObject(cmp);
            }
            try (ObjectInputStream oi = new ObjectInputStream(new ByteArrayInputStream(bo.toByteArray()))) {
                return (Comparator<T>) oi.readObject();
            }
        }

        /**
         * Before the fix {@code comparingChar}/{@code comparingByte}/{@code comparingShort} carried the
         * {@code Serializable} marker (inherited from {@code Comparator.comparingInt}) but always threw
         * {@code NotSerializableException}, because the method-reference adapter they captured was not
         * serializable; {@code comparingBoolean}/{@code comparingFloat} carried no marker at all.
         */
        @Test
        public void everyPrimitiveKeyComparatorRoundTrips() throws Exception {
            assertEquals(-1, Integer.signum(roundTrip(Comparators.comparingBoolean((SerToBoolean<Boolean>) x -> x)).compare(false, true)));
            assertEquals(-1, Integer.signum(roundTrip(Comparators.comparingChar((SerToChar<Character>) x -> x)).compare('a', 'b')));
            assertEquals(-1, Integer.signum(roundTrip(Comparators.comparingByte((SerToByte<Byte>) x -> x)).compare((byte) 1, (byte) 2)));
            assertEquals(-1, Integer.signum(roundTrip(Comparators.comparingShort((SerToShort<Short>) x -> x)).compare((short) 1, (short) 2)));
            assertEquals(-1, Integer.signum(roundTrip(Comparators.comparingInt((SerToInt<Integer>) x -> x)).compare(1, 2)));
            assertEquals(-1, Integer.signum(roundTrip(Comparators.comparingLong((SerToLong<Long>) x -> x)).compare(1L, 2L)));
            assertEquals(-1, Integer.signum(roundTrip(Comparators.comparingFloat((SerToFloat<Float>) x -> x)).compare(1f, 2f)));
            assertEquals(-1, Integer.signum(roundTrip(Comparators.comparingDouble((SerToDouble<Double>) x -> x)).compare(1d, 2d)));
        }

        @Test
        public void everyReversedPrimitiveKeyComparatorRoundTrips() throws Exception {
            assertEquals(1, Integer.signum(roundTrip(Comparators.reversedComparingBoolean((SerToBoolean<Boolean>) x -> x)).compare(false, true)));
            assertEquals(1, Integer.signum(roundTrip(Comparators.reversedComparingChar((SerToChar<Character>) x -> x)).compare('a', 'b')));
            assertEquals(1, Integer.signum(roundTrip(Comparators.reversedComparingByte((SerToByte<Byte>) x -> x)).compare((byte) 1, (byte) 2)));
            assertEquals(1, Integer.signum(roundTrip(Comparators.reversedComparingShort((SerToShort<Short>) x -> x)).compare((short) 1, (short) 2)));
            assertEquals(1, Integer.signum(roundTrip(Comparators.reversedComparingInt((SerToInt<Integer>) x -> x)).compare(1, 2)));
            assertEquals(1, Integer.signum(roundTrip(Comparators.reversedComparingLong((SerToLong<Long>) x -> x)).compare(1L, 2L)));
            assertEquals(1, Integer.signum(roundTrip(Comparators.reversedComparingFloat((SerToFloat<Float>) x -> x)).compare(1f, 2f)));
            assertEquals(1, Integer.signum(roundTrip(Comparators.reversedComparingDouble((SerToDouble<Double>) x -> x)).compare(1d, 2d)));
        }

        /**
         * The four metric comparators had the same defect as {@code comparingChar} and friends: they were
         * built with {@code Comparator.comparingInt(lambda)}, so they advertised {@code Serializable} while
         * capturing a non-serializable extractor. They hold nothing of the caller's, so they now always
         * round-trip.
         */
        @Test
        public void metricComparatorsAlwaysRoundTrip() throws Exception {
            assertEquals(0, roundTrip(Comparators.<String> comparingByLength()).compare("ab", "cd"));
            assertTrue(roundTrip(Comparators.<String> comparingByLength()).compare("a", "abc") < 0);
            assertEquals(0, roundTrip(Comparators.<String> comparingByLength()).compare(null, ""));

            assertTrue(roundTrip(Comparators.<Object> comparingByArrayLength()).compare(new int[1], new int[3]) < 0);
            assertEquals(0, roundTrip(Comparators.<Object> comparingByArrayLength()).compare(null, new int[0]));

            assertTrue(roundTrip(Comparators.<Collection<?>> comparingBySize()).compare(Arrays.asList(1), Arrays.asList(1, 2)) < 0);
            assertEquals(0, roundTrip(Comparators.<Collection<?>> comparingBySize()).compare(null, Collections.emptyList()));

            assertTrue(roundTrip(Comparators.<Map<?, ?>> comparingByMapSize()).compare(CommonUtil.asMap("a", 1), ordered("a", 1, "b", 2)) < 0);
            assertEquals(0, roundTrip(Comparators.<Map<?, ?>> comparingByMapSize()).compare(null, Collections.emptyMap()));
        }

        /** The class javadoc's Serialization section names exactly these groups; nothing else may claim it. */
        @Test
        public void nothingElseClaimsToBeSerializable() {
            assertFalse(Comparators.naturalOrder() instanceof Serializable);
            assertFalse(Comparators.reverseOrder() instanceof Serializable);
            assertFalse(Comparators.nullsFirst() instanceof Serializable);
            assertFalse(Comparators.nullsLast() instanceof Serializable);
            assertFalse(Comparators.comparingIgnoreCase() instanceof Serializable);
            assertFalse(Comparators.comparingBy((Integer x) -> x) instanceof Serializable);
            assertFalse(Comparators.comparingByKey() instanceof Serializable);
            assertFalse(Comparators.comparingCollection() instanceof Serializable);
            assertFalse(Comparators.INT_ARRAY_COMPARATOR instanceof Serializable);
            assertFalse(Comparators.OBJECT_ARRAY_COMPARATOR instanceof Serializable);
            assertFalse(Comparators.COLLECTION_COMPARATOR instanceof Serializable);
        }

        /**
         * C-010: {@code nothingElseClaimsToBeSerializable} above asserted the closing sentence of the
         * class javadoc's Serialization section against a hand-picked list, and that list happened to
         * exclude the one counterexample: {@code reverseOrder(Comparator)} returns
         * {@code Collections.reverseOrder(cmp)}, which <i>is</i> {@link Serializable} and round-trips
         * whenever {@code cmp} does. The section now names it as a third group; this locks that.
         */
        @Test
        public void reverseOrderWrappingACallerComparatorIsSerializable() throws Exception {
            final Comparator<String> user = new SerializableUserComparator();
            final Comparator<String> reversed = Comparators.reverseOrder(user);
            assertInstanceOf(Serializable.class, reversed);
            assertTrue(reversed.compare("a", "b") > 0, "reversed order");
            assertEquals(1, Integer.signum(roundTrip(reversed).compare("a", "b")), "and still reversed after a round trip");

            // ... but only when it actually wraps: both short-circuits hand back the shared, non-serializable constants
            assertSame(Comparators.reverseOrder(), Comparators.reverseOrder(Comparators.<String> naturalOrder()));
            assertSame(Comparators.naturalOrder(), Comparators.reverseOrder(Comparators.<String> reverseOrder()));
            assertFalse(Comparators.reverseOrder(Comparators.<String> naturalOrder()) instanceof Serializable);
            assertFalse(Comparators.reverseOrder(Comparators.<String> reverseOrder()) instanceof Serializable);

            // and a non-serializable delegate makes the write fail, exactly as the section says
            final Comparator<String> lambda = (x, y) -> 0;
            final Comparator<String> overLambda = Comparators.reverseOrder(lambda);
            assertInstanceOf(Serializable.class, overLambda);
            assertThrows(java.io.NotSerializableException.class, () -> {
                try (ObjectOutputStream oo = new ObjectOutputStream(new java.io.ByteArrayOutputStream())) {
                    oo.writeObject(overLambda);
                }
            });
        }

        /**
         * C-010, the durable half. Rather than a hand-picked list, walk <i>every</i> public factory that
         * takes a single {@link Comparator} and check its serializability against the documented rule, so a
         * factory that silently inherits a JDK wrapper's {@code Serializable} marker cannot slip through
         * again. {@code reverseOrder} is the only one the class javadoc admits.
         */
        @Test
        public void onlyReverseOrderInheritsASerializableWrapper() throws Exception {
            final Comparator<Object> delegate = new SerializableUserComparator2();
            final List<String> unexpected = new ArrayList<>();
            int visited = 0;

            for (final java.lang.reflect.Method m : Comparators.class.getDeclaredMethods()) {
                if (!java.lang.reflect.Modifier.isPublic(m.getModifiers()) || !java.lang.reflect.Modifier.isStatic(m.getModifiers())
                        || !Comparator.class.isAssignableFrom(m.getReturnType())) {
                    continue;
                }
                if (m.getParameterCount() != 1 || m.getParameterTypes()[0] != Comparator.class) {
                    continue;
                }
                visited++;
                final Object result = m.invoke(null, delegate);
                final boolean serializable = result instanceof Serializable;
                if (serializable != "reverseOrder".equals(m.getName())) {
                    unexpected.add(m.getName() + " -> " + (serializable ? "Serializable" : "not serializable"));
                }
            }

            assertEquals(Collections.emptyList(), unexpected, "serializability disagrees with the documented groups");
            // guard against the sweep going vacuous if the signatures are ever reshaped
            assertTrue(visited >= 12, "only " + visited + " single-Comparator factories were swept");
        }

        /** The rewrite must not have changed any ordering, including the float/double special values. */
        @Test
        public void orderingIsUnchangedForSpecialValues() {
            final float[] floats = { Float.NaN, Float.NEGATIVE_INFINITY, -1f, -0.0f, 0.0f, 1f, Float.POSITIVE_INFINITY };
            final Comparator<Float> cf = Comparators.comparingFloat(x -> x);
            final Comparator<Float> rf = Comparators.reversedComparingFloat(x -> x);
            for (final float x : floats) {
                for (final float y : floats) {
                    assertEquals(Integer.signum(Float.compare(x, y)), Integer.signum(cf.compare(x, y)), x + " vs " + y);
                    assertEquals(Integer.signum(Float.compare(y, x)), Integer.signum(rf.compare(x, y)), x + " vs " + y);
                }
            }

            final double[] doubles = { Double.NaN, Double.NEGATIVE_INFINITY, -1d, -0.0d, 0.0d, 1d, Double.POSITIVE_INFINITY };
            final Comparator<Double> cd = Comparators.comparingDouble(x -> x);
            for (final double x : doubles) {
                for (final double y : doubles) {
                    assertEquals(Integer.signum(Double.compare(x, y)), Integer.signum(cd.compare(x, y)), x + " vs " + y);
                }
            }

            assertEquals(Integer.signum(Character.compare(Character.MIN_VALUE, Character.MAX_VALUE)),
                    Integer.signum(Comparators.comparingChar((Character x) -> x).compare(Character.MIN_VALUE, Character.MAX_VALUE)));
            assertEquals(Integer.signum(Byte.compare(Byte.MIN_VALUE, Byte.MAX_VALUE)),
                    Integer.signum(Comparators.comparingByte((Byte x) -> x).compare(Byte.MIN_VALUE, Byte.MAX_VALUE)));
            assertEquals(Integer.signum(Short.compare(Short.MIN_VALUE, Short.MAX_VALUE)),
                    Integer.signum(Comparators.comparingShort((Short x) -> x).compare(Short.MIN_VALUE, Short.MAX_VALUE)));
            assertEquals(Integer.signum(Integer.compare(Integer.MIN_VALUE, Integer.MAX_VALUE)),
                    Integer.signum(Comparators.comparingInt((Integer x) -> x).compare(Integer.MIN_VALUE, Integer.MAX_VALUE)));
            assertEquals(Integer.signum(Long.compare(Long.MIN_VALUE, Long.MAX_VALUE)),
                    Integer.signum(Comparators.comparingLong((Long x) -> x).compare(Long.MIN_VALUE, Long.MAX_VALUE)));
        }
    }

    @Nested
    public class ComparatorsContract {

        /** B6: the class javadoc claimed a lazy NPE; every factory validates eagerly with an IAE. */
        @Test
        public void keyExtractorsAreValidatedEagerly() {
            assertThrows(IllegalArgumentException.class, () -> Comparators.comparingBy(null));
            assertThrows(IllegalArgumentException.class, () -> Comparators.comparingInt(null));
            assertThrows(IllegalArgumentException.class, () -> Comparators.comparingFloat(null));
            assertThrows(IllegalArgumentException.class, () -> Comparators.comparingBoolean(null));
            assertThrows(IllegalArgumentException.class, () -> Comparators.nullsFirstBy(null));
            assertThrows(IllegalArgumentException.class, () -> Comparators.nullsLastBy(null));
            assertThrows(IllegalArgumentException.class, () -> Comparators.reversedComparingBy(null));
            assertThrows(IllegalArgumentException.class, () -> Comparators.reversedComparingInt(null));
            assertThrows(IllegalArgumentException.class, () -> Comparators.nullsFirst(null));
            assertThrows(IllegalArgumentException.class, () -> Comparators.nullsLast(null));
            assertThrows(IllegalArgumentException.class, () -> Comparators.reverseOrder(null));
        }

        /** D3: a comparator of any supertype is now accepted, matching {@code Comparator.nullsFirst}. */
        @Test
        public void nullPolicyWrappersAcceptASupertypeComparator() {
            final Comparator<Object> byToString = Comparator.comparing(Object::toString);

            final Comparator<String> first = Comparators.nullsFirst(byToString);
            assertTrue(first.compare(null, "a") < 0);
            assertTrue(first.compare("a", "b") < 0);

            final Comparator<String> last = Comparators.nullsLast(byToString);
            assertTrue(last.compare(null, "a") > 0);

            final Comparator<String> reversed = Comparators.reverseOrder(byToString);
            assertTrue(reversed.compare("a", "b") > 0);
        }

        /** The wrappers keep returning the shared constant when handed it, after the variance widening. */
        @Test
        public void nullPolicyWrappersKeepTheirIdentityShortcut() {
            assertSame(Comparators.nullsFirst(), Comparators.nullsFirst(Comparators.nullsFirst()));
            assertSame(Comparators.nullsLast(), Comparators.nullsLast(Comparators.nullsLast()));

            // wrapping the opposite policy still produces a working wrapper, not the shared instance
            final Comparator<String> wrapped = Comparators.nullsFirst(Comparators.<String> nullsLast());
            assertTrue(wrapped.compare(null, "a") < 0);
            assertTrue(wrapped.compare("a", "b") < 0);
        }

        /** The metric comparators were rewritten; their documented non-array behaviour must be unchanged. */
        @Test
        public void comparingByArrayLengthStillRejectsANonArray() {
            final Comparator<Object> cmp = Comparators.comparingByArrayLength();

            assertEquals(0, cmp.compare(new int[2], new String[2]));
            assertTrue(cmp.compare(new int[1], new int[2]) < 0);
            assertThrows(IllegalArgumentException.class, () -> cmp.compare("not an array", new int[0]));
            assertThrows(IllegalArgumentException.class, () -> cmp.compare(new int[0], "not an array"));
        }

        @Test
        public void reverseOrderKeepsItsSharedInstanceShortcuts() {
            assertSame(Comparators.reverseOrder(), Comparators.reverseOrder(Comparators.naturalOrder()));
            assertSame(Comparators.reverseOrder(), Comparators.reverseOrder(Comparators.nullsFirst()));
            assertSame(Comparators.naturalOrder(), Comparators.reverseOrder(Comparators.reverseOrder()));
        }

        /** D2: the two null policies this class deliberately runs. */
        @Test
        public void twoNullPoliciesAreLocked() {
            // metric comparators read null as 0, so null ties with empty
            assertEquals(0, Comparators.<String> comparingByLength().compare(null, ""));
            assertEquals(0, Comparators.<Collection<?>> comparingBySize().compare(null, Collections.emptyList()));
            assertEquals(0, Comparators.<Map<?, ?>> comparingByMapSize().compare(null, Collections.emptyMap()));
            assertEquals(0, Comparators.<Object> comparingByArrayLength().compare(null, new int[0]));

            // lexicographic comparators order null strictly first
            assertTrue(Comparators.INT_ARRAY_COMPARATOR.compare(null, new int[0]) < 0);
            assertTrue(Comparators.OBJECT_ARRAY_COMPARATOR.compare(null, new Object[0]) < 0);
            assertTrue(Comparators.COLLECTION_COMPARATOR.compare(null, Collections.emptyList()) < 0);
            assertTrue(Comparators.<String> comparingArray(Comparators.<String> naturalOrder()).compare(null, new String[0]) < 0);
        }

        /** D1: {@code comparingBy} and {@code nullsFirstBy} are the same comparator. */
        @Test
        public void comparingByAndNullsFirstByAgree() {
            final java.util.function.Function<String, Comparable> key = s -> s;
            final Comparator<String> a = Comparators.comparingBy(key);
            final Comparator<String> b = Comparators.nullsFirstBy(key);

            final String[][] pairs = { { null, null }, { null, "a" }, { "a", null }, { "a", "b" }, { "b", "a" }, { "a", "a" } };
            for (final String[] p : pairs) {
                assertEquals(Integer.signum(a.compare(p[0], p[1])), Integer.signum(b.compare(p[0], p[1])), Arrays.toString(p));
            }
        }

        /**
         * J6: {@code Arrays.compare(T[], T[])} treats a null element as the minimum, exactly like
         * {@link Comparators#OBJECT_ARRAY_COMPARATOR}; it does not throw.
         */
        @Test
        public void objectArrayComparatorAgreesWithArraysCompareOnNullElements() {
            assertEquals(Integer.signum(Arrays.compare(new String[] { null }, new String[] { "x" })),
                    Integer.signum(Comparators.OBJECT_ARRAY_COMPARATOR.compare(new Object[] { null }, new Object[] { "x" })));
            assertEquals(Integer.signum(Arrays.compare(new String[] { "x" }, new String[] { null })),
                    Integer.signum(Comparators.OBJECT_ARRAY_COMPARATOR.compare(new Object[] { "x" }, new Object[] { null })));
            assertEquals(Integer.signum(Arrays.compare(new String[] { null }, new String[] { null })),
                    Integer.signum(Comparators.OBJECT_ARRAY_COMPARATOR.compare(new Object[] { null }, new Object[] { null })));
        }

        /**
         * C-008: nothing distinguished {@code comparingByKey()} from {@code comparingByValue()}. Every
         * existing test used entries whose key order and value order give the <i>same</i> permutation
         * ({@code ("apple",1), ("banana",2), ("cherry",3)}), and the null cases were symmetric too, so
         * swapping the two implementations broke no test. These entries order oppositely by key and by
         * value, so each comparator can only pass by reading the right field.
         */
        @Test
        public void entryComparatorsReadTheFieldTheyName() {
            final List<Map.Entry<String, Integer>> entries = Arrays.asList(new AbstractMap.SimpleEntry<>("a", 3), new AbstractMap.SimpleEntry<>("b", 2),
                    new AbstractMap.SimpleEntry<>("c", 1));

            final List<Map.Entry<String, Integer>> byKey = new ArrayList<>(entries);
            byKey.sort(Comparators.comparingByKey());
            assertEquals(Arrays.asList("a", "b", "c"), byKey.stream().map(Map.Entry::getKey).toList());

            final List<Map.Entry<String, Integer>> byValue = new ArrayList<>(entries);
            byValue.sort(Comparators.comparingByValue());
            assertEquals(Arrays.asList("c", "b", "a"), byValue.stream().map(Map.Entry::getKey).toList(),
                    "value order is the reverse of key order for this data");

            // the null cases must discriminate too: a null key and a null value pull opposite ways
            final Map.Entry<String, Integer> nullKey = new AbstractMap.SimpleEntry<>(null, 2);
            final Map.Entry<String, Integer> nullValue = new AbstractMap.SimpleEntry<>("a", null);
            assertTrue(Comparators.<String, Integer> comparingByKey().compare(nullKey, nullValue) < 0, "null key sorts first");
            assertTrue(Comparators.<String, Integer> comparingByValue().compare(nullKey, nullValue) > 0, "null value sorts first");

            // and the comparator-taking overloads read the field they name as well
            assertTrue(Comparators.<String, Integer> comparingByKey(Comparator.reverseOrder()).compare(entries.get(0), entries.get(2)) > 0);
            assertTrue(Comparators.<String, Integer> comparingByValue(Comparator.reverseOrder()).compare(entries.get(0), entries.get(2)) < 0);
        }

        /** D10: the whole entry-comparator family is stable now, not half {@code @Beta}. */
        @Test
        public void reversedEntryComparatorsBehaveAsDocumented() {
            final Map<String, Integer> scores = ordered("Alice", 95, "Bob", 87, "Carol", 92);
            final List<Map.Entry<String, Integer>> byKey = new ArrayList<>(scores.entrySet());
            byKey.sort(Comparators.reversedComparingByKey());
            assertEquals(Arrays.asList("Carol", "Bob", "Alice"), byKey.stream().map(Map.Entry::getKey).toList());

            final List<Map.Entry<String, Integer>> byValue = new ArrayList<>(scores.entrySet());
            byValue.sort(Comparators.reversedComparingByValue());
            assertEquals(Arrays.asList("Alice", "Carol", "Bob"), byValue.stream().map(Map.Entry::getKey).toList());

            final List<Map.Entry<String, Integer>> byKeyLength = new ArrayList<>(
                    BuilderDifferenceComparatorsTest.<String, Integer> ordered("a", 1, "abc", 2, "ab", 3).entrySet());
            byKeyLength.sort(Comparators.reversedComparingByKey(Comparator.comparingInt(String::length)));
            assertEquals(Arrays.asList("abc", "ab", "a"), byKeyLength.stream().map(Map.Entry::getKey).toList());
        }
    }

    // ---------------------------------------------------------------------------------------------------
    // Cycle 1 of the iterative deep review
    // (ledger: scripts/cross_review/Builder_Difference_Comparators_ledger_2026-09-01.md)
    // ---------------------------------------------------------------------------------------------------

    @Nested
    public class Cycle1 {

        /**
         * C-001: {@code areEqual()} now measures each result container on its own type. It used to decide the
         * kind from {@code onlyOnLeft} and cast {@code onlyOnRight} to it, and answered {@code false} for a
         * container kind it did not recognise - the worst possible answer for an equality predicate.
         */
        @Test
        public void areEqualMeasuresEachContainerIndependently() {
            // every result-container kind the factories produce
            assertTrue(Difference.of(new ArrayList<String>(), new ArrayList<String>()).areEqual(), "Collection results");
            assertTrue(Difference.of(IntList.of(), IntList.of()).areEqual(), "PrimitiveList results");
            assertTrue(MapDifference.of(new HashMap<String, Integer>(), new HashMap<String, Integer>()).areEqual(), "Map results");
            assertTrue(BeanDifference.of((Object) null, (Object) null).areEqual(), "bean results");
            assertTrue(MapDifference.of(new ArrayList<Map<String, Object>>(), new ArrayList<Map<String, Object>>(), m -> m.get("id")).areEqual(),
                    "List-of-maps results");

            // and the negative side of each
            assertFalse(Difference.of(Arrays.asList("a"), new ArrayList<String>()).areEqual());
            assertFalse(Difference.of(IntList.of(1), IntList.of()).areEqual());
            assertFalse(MapDifference.of(CommonUtil.asMap("a", 1), new HashMap<String, Integer>()).areEqual());

            // empty on one side only, both orders
            assertFalse(Difference.of(new ArrayList<String>(), Arrays.asList("a")).areEqual());
            assertFalse(Difference.of(IntList.of(), IntList.of(1)).areEqual());
        }

        /**
         * C-003: occurrence matching boxes, so it follows {@code Float.equals}/{@code Double.equals}, not
         * {@code ==} - two NaNs match, +0.0 and -0.0 do not. Now documented on the four float/double
         * overloads; locked here so it cannot drift.
         */
        @Test
        public void floatAndDoubleDifferenceFollowBoxedEquality() {
            assertTrue(Difference.of(FloatList.of(Float.NaN), FloatList.of(Float.NaN)).areEqual(), "NaN matches NaN (FloatList)");
            assertTrue(Difference.of(DoubleList.of(Double.NaN), DoubleList.of(Double.NaN)).areEqual(), "NaN matches NaN (DoubleList)");
            assertTrue(Difference.of(new float[] { Float.NaN }, new float[] { Float.NaN }).areEqual(), "NaN matches NaN (float[])");
            assertTrue(Difference.of(new double[] { Double.NaN }, new double[] { Double.NaN }).areEqual(), "NaN matches NaN (double[])");
            assertTrue(Difference.of(new Float[] { Float.NaN }, new Float[] { Float.NaN }).areEqual(), "NaN matches NaN (Float[])");

            final Difference<FloatList, FloatList> zeros = Difference.of(FloatList.of(0.0f), FloatList.of(-0.0f));
            assertFalse(zeros.areEqual(), "+0.0f does not match -0.0f");
            assertEquals(FloatList.of(0.0f), zeros.onlyOnLeft());
            assertEquals(FloatList.of(-0.0f), zeros.onlyOnRight());

            assertFalse(Difference.of(DoubleList.of(0.0d), DoubleList.of(-0.0d)).areEqual(), "+0.0 does not match -0.0");

            // infinities and the boundary values behave like any other equal/unequal pair
            assertTrue(Difference.of(DoubleList.of(Double.POSITIVE_INFINITY), DoubleList.of(Double.POSITIVE_INFINITY)).areEqual());
            assertFalse(Difference.of(DoubleList.of(Double.POSITIVE_INFINITY), DoubleList.of(Double.NEGATIVE_INFINITY)).areEqual());
            assertTrue(Difference.of(DoubleList.of(Double.MIN_VALUE, Double.MAX_VALUE), DoubleList.of(Double.MAX_VALUE, Double.MIN_VALUE)).areEqual());

            // empty and null inputs still take the documented empty path
            assertTrue(Difference.of((FloatList) null, (FloatList) null).areEqual());
            assertTrue(Difference.of(FloatList.of(), FloatList.of()).areEqual());

            // the documented equivalence with the primitive list own difference()
            assertEquals(0, FloatList.of(Float.NaN).difference(FloatList.of(Float.NaN)).size());
            assertEquals(FloatList.of(0.0f), FloatList.of(0.0f).difference(FloatList.of(-0.0f)));
        }

        /**
         * C-002: the primitive result comments used Java literal syntax the lists never render. Lock what
         * they actually render, including a supplementary code point.
         */
        @Test
        public void primitiveListsRenderWithoutLiteralSuffixes() {
            assertEquals("[a, b, c]", CharList.of('a', 'b', 'c').toString());
            assertEquals("[1, 3]", ByteList.of((byte) 1, (byte) 3).toString());
            assertEquals("[10, 30]", ShortList.of((short) 10, (short) 30).toString());
            assertEquals("[100, 300]", LongList.of(100L, 300L).toString());
            assertEquals("[1.5, 3.5]", FloatList.of(1.5f, 3.5f).toString());
            assertEquals("[1.0, 3.0]", DoubleList.of(1.0d, 3.0d).toString());
            assertEquals("[true, false]", BooleanList.of(true, false).toString());

            // a supplementary code point is two chars, and is held as its two surrogates
            final char[] surrogates = Character.toChars(0x10400);
            assertEquals(2, surrogates.length);
            assertEquals(2, CharList.of(surrogates).size());

            // the corrected doc examples, executed
            assertEquals("[a, c]", Builder.of(CharList.of('a', 'b', 'c')).remove('b').val().toString());
            assertEquals("[1, 3]", Builder.of(ByteList.of((byte) 1, (byte) 2, (byte) 3)).remove((byte) 2).val().toString());
            assertEquals("[100, 300]", Builder.of(LongList.of(100L, 200L, 300L)).remove(200L).val().toString());
        }

        /** C-004: the extractor is applied to both objects, so a null object reaches it. */
        @Test
        public void comparingIgnoreCaseAppliesTheExtractorToNullObjects() {
            final Comparator<String> byItself = Comparators.comparingIgnoreCase(s -> s);
            assertEquals(0, byItself.compare("ABC", "abc"));
            assertTrue(byItself.compare(null, "a") < 0, "extracted nulls order first");
            assertEquals(0, byItself.compare(null, null));

            // an extractor that dereferences its argument sees the raw null, as the note now says
            assertThrows(NullPointerException.class, () -> Comparators.<String> comparingIgnoreCase(String::trim).compare(null, "a"));

            // wrapping in nullsFirst is the documented remedy
            assertTrue(Comparators.nullsFirst(Comparators.<String> comparingIgnoreCase(String::trim)).compare(null, "a") < 0);
        }

        /**
         * C-006: the {@code IdentityHashMap} branch of the consumed-key tracker had no test that could tell
         * it apart from the equals-based fallback - a mutation deleting the branch killed nothing.
         *
         * <p>It only matters when the fallback would recognise a map2 entry that identity would not, i.e.
         * when map2 holds two keys that are {@code equals}-equal but distinct instances. Without the
         * tracker, {@code common.containsKey(kB)} is {@code true} because {@code kA} is already in the
         * equals-keyed result map, so {@code kB} is silently dropped from {@code onlyOnRight} - and the
         * consumed-entry check then rejects the whole call.
         */
        @Test
        public void identityKeyedRightMapDistinguishesEqualButDistinctKeys() {
            final String kA = new String("dup"); // NOSONAR - distinct instances are the point of the test
            final String kB = new String("dup"); // NOSONAR
            assertEquals(kA, kB);
            assertNotSame(kA, kB);

            final Map<String, Integer> map2 = new IdentityHashMap<>();
            map2.put(kA, 1);
            map2.put(kB, 2);
            assertEquals(2, map2.size(), "an IdentityHashMap keeps both instances");

            final Map<String, Integer> map1 = new LinkedHashMap<>();
            map1.put(kA, 1);

            final MapDifference<Map<String, Integer>, Map<String, Integer>, Map<String, Pair<Integer, Integer>>> diff = MapDifference.of(map1, map2);

            assertEquals(CommonUtil.asMap("dup", 1), diff.common(), "kA matched by identity");
            assertTrue(diff.onlyOnLeft().isEmpty());
            assertEquals(CommonUtil.asMap("dup", 2), diff.onlyOnRight(), "kB is right-only and must not be swallowed");
            assertTrue(diff.differentValues().isEmpty());
            assertFalse(diff.areEqual());

            // the same shape with differing values keeps kB on the right too
            final Map<String, Integer> map1b = new LinkedHashMap<>();
            map1b.put(kA, 99);
            final MapDifference<Map<String, Integer>, Map<String, Integer>, Map<String, Pair<Integer, Integer>>> diff2 = MapDifference.of(map1b, map2);
            assertEquals(Set.of("dup"), diff2.differentValues().keySet());
            assertEquals(CommonUtil.asMap("dup", 2), diff2.onlyOnRight());

            // an empty identity map on both sides still takes the documented empty path
            assertTrue(MapDifference.of(new IdentityHashMap<String, Integer>(), new IdentityHashMap<String, Integer>()).areEqual());
        }

        /**
         * C-007: the mirror of the consumed-entry check. An input whose key equality is <i>finer</i> than
         * {@code equals} - here an {@code IdentityHashMap} holding two equal-but-distinct keys - would have
         * had its second entry silently overwritten in the {@code equals}-keyed result map, so the
         * difference reported fewer entries than the input holds. The one-sided fast paths bypassed the
         * consumed-entry counters entirely, so nothing noticed.
         */
        @Test
        public void finerKeyEqualityThatWouldCollapseResultEntriesIsRejected() {
            final String kA = new String("dup"); // NOSONAR
            final String kB = new String("dup"); // NOSONAR
            final Map<String, Integer> identity = new IdentityHashMap<>();
            identity.put(kA, 1);
            identity.put(kB, 2);
            assertEquals(2, identity.size());

            final Map<String, Integer> empty = new LinkedHashMap<>();
            final Map<String, Integer> unrelated = new LinkedHashMap<>();
            unrelated.put("zzz", 9);

            // one-sided fast paths, both directions
            assertThrows(IllegalArgumentException.class, () -> MapDifference.of(empty, identity), "map1 empty");
            assertThrows(IllegalArgumentException.class, () -> MapDifference.of(identity, empty), "map2 empty");
            // and through the main loop
            assertThrows(IllegalArgumentException.class, () -> MapDifference.of(unrelated, identity), "both non-empty");

            final IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> MapDifference.of(empty, identity));
            assertTrue(e.getMessage().contains("collapsed into"), e.getMessage());
            assertTrue(e.getMessage().contains("IdentityHashMap"), e.getMessage());

            // a SortedMap whose comparator is finer than equals collapses the same way
            final TreeMap<String, Integer> finer = new TreeMap<>(Comparator.comparingInt(System::identityHashCode));
            finer.put(kA, 1);
            finer.put(kB, 2);
            assertEquals(2, finer.size());
            assertThrows(IllegalArgumentException.class, () -> MapDifference.of(empty, finer));

            // an identity map WITHOUT equal-but-distinct keys is unaffected
            final Map<String, Integer> plainIdentity = new IdentityHashMap<>();
            plainIdentity.put(kA, 1);
            plainIdentity.put("other", 2);
            assertEquals(2, MapDifference.of(empty, plainIdentity).onlyOnRight().size());
            assertEquals(2, MapDifference.of(plainIdentity, empty).onlyOnLeft().size());
            assertTrue(MapDifference.of(plainIdentity, plainIdentity).areEqual());

            // An equals-based selection admits both equal keys, so both entries still collide.
            assertThrows(IllegalArgumentException.class, () -> MapDifference.of(empty, identity, Arrays.asList("dup")));

            // An identity-based selection can admit just one key without collapsing entries.
            final Set<String> selected = Collections.newSetFromMap(new IdentityHashMap<>());
            selected.add(kA);
            assertEquals(Map.of(kA, 1), MapDifference.of(empty, identity, selected).onlyOnRight());
        }

        /** C-005: the cheap null checks now run before the O(n) bean scan of both collections. */
        @Test
        public void collectionOfBeansValidatesTheExtractorFirst() {
            final List<Object> notBeans = Arrays.asList("not a bean");

            // a null extractor is reported even when the collections would also fail the bean check
            assertThrows(IllegalArgumentException.class, () -> BeanDifference.of(notBeans, notBeans, (java.util.function.Function<Object, Object>) null));

            // and a non-bean element is still rejected once the extractor is valid
            final IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> BeanDifference.of(notBeans, notBeans, Object::toString));
            assertTrue(e.getMessage().contains("is not a bean class"), e.getMessage());
        }
    }

    /** A serializable delegate, so a wrapper's own marker is what the test measures. */
    static final class SerializableUserComparator implements Comparator<String>, Serializable {
        private static final long serialVersionUID = 1L;

        @Override
        public int compare(final String a, final String b) {
            return a == null ? (b == null ? 0 : -1) : (b == null ? 1 : a.compareTo(b));
        }
    }

    /** The same, typed loosely enough for every single-{@code Comparator} factory to accept it. */
    static final class SerializableUserComparator2 implements Comparator<Object>, Serializable {
        private static final long serialVersionUID = 1L;

        @Override
        public int compare(final Object a, final Object b) {
            return 0;
        }
    }

    public static final class ReviewBean {
        private String name;
        private int value;

        public ReviewBean() {
        }

        public ReviewBean(final String name, final int value) {
            this.name = name;
            this.value = value;
        }

        public String getName() {
            return name;
        }

        public void setName(final String name) {
            this.name = name;
        }

        public int getValue() {
            return value;
        }

        public void setValue(final int value) {
            this.value = value;
        }
    }
}

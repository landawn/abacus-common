package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.Difference.BeanDifference;
import com.landawn.abacus.util.Difference.KeyValueDifference;
import com.landawn.abacus.util.Difference.MapDifference;

/**
 * Regression tests for the 2026-08-31 {@code Builder} / {@code Difference} / {@code Comparators} review.
 *
 * <p>Every test in {@link MapDifferenceKeyEquality}, {@link ComparingBeanByPropsValidation} and
 * {@link AcceptPreservesSpecializedBuilder} fails on the pre-fix sources; the remainder lock behaviour that
 * the review documented so it cannot drift.
 */
public class BuilderDifferenceComparatorsFixesTest extends TestBase {

    // ---------------------------------------------------------------------------------------------------
    // Fix 1 - MapDifference must not file one entry into two mutually exclusive buckets.
    // ---------------------------------------------------------------------------------------------------

    @Nested
    public class MapDifferenceKeyEquality {

        /**
         * The headline defect: a plain natural-ordering {@code TreeMap<BigDecimal>} treats 2.0 and 2.00 as
         * one key, but the hash-based result maps do not, so the entry used to land in BOTH common and
         * onlyOnRight.
         */
        @Test
        public void testTreeMapOfBigDecimal_sameEntryNotReportedTwice() {
            final TreeMap<BigDecimal, String> map1 = new TreeMap<>();
            map1.put(new BigDecimal("2.0"), "x");
            final TreeMap<BigDecimal, String> map2 = new TreeMap<>();
            map2.put(new BigDecimal("2.00"), "x");

            // The two maps really are equal to each other...
            assertTrue(map1.equals(map2));

            final MapDifference<Map<BigDecimal, String>, Map<BigDecimal, String>, Map<BigDecimal, Pair<String, String>>> diff = MapDifference.of(map1, map2);

            assertEquals(CommonUtil.asMap(new BigDecimal("2.0"), "x"), diff.common());
            assertTrue(diff.onlyOnLeft().isEmpty());
            assertTrue(diff.onlyOnRight().isEmpty(), "the matched entry must not also be reported as right-only");
            assertTrue(diff.differentValues().isEmpty());
            // ...so the difference must agree.
            assertTrue(diff.areEqual());
        }

        /** Same shape, but the values differ: the right value must not be reported twice. */
        @Test
        public void testTreeMapOfBigDecimal_differentValuesNotDuplicatedIntoOnlyOnRight() {
            final TreeMap<BigDecimal, Integer> map1 = new TreeMap<>();
            map1.put(new BigDecimal("2.0"), 1);
            final TreeMap<BigDecimal, Integer> map2 = new TreeMap<>();
            map2.put(new BigDecimal("2.00"), 2);

            final MapDifference<Map<BigDecimal, Integer>, Map<BigDecimal, Integer>, Map<BigDecimal, Pair<Integer, Integer>>> diff = MapDifference.of(map1,
                    map2);

            assertEquals(1, diff.differentValues().size());
            assertEquals(Pair.of(1, 2), diff.differentValues().get(new BigDecimal("2.0")));
            assertTrue(diff.onlyOnRight().isEmpty(), "the right value must not also be reported as right-only");
            assertFalse(diff.areEqual());
        }

        @Test
        public void testCaseInsensitiveTreeMaps_onBothSides() {
            final TreeMap<String, Integer> map1 = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
            map1.put("a", 1);
            final TreeMap<String, Integer> map2 = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
            map2.put("A", 1);

            final MapDifference<Map<String, Integer>, Map<String, Integer>, Map<String, Pair<Integer, Integer>>> diff = MapDifference.of(map1, map2);

            assertEquals(CommonUtil.asMap("a", 1), diff.common());
            assertTrue(diff.onlyOnRight().isEmpty());
            assertTrue(diff.areEqual());
        }

        /**
         * The keysToCompare branch carries the same guard and must behave identically.
         *
         * <p>The selection is deliberately a {@code TreeSet} with the same coarse ordering as the maps: a
         * {@code Set} is used as given, so its {@code contains} lets 2.00 through, and only then does the
         * consumed-key guard decide. A {@code List} selection would be copied into a {@code HashSet} and
         * would reject 2.00 on equals alone, masking the defect.
         */
        @Test
        public void testKeysToCompareBranch_honoursMapKeyEquality() {
            final TreeMap<BigDecimal, Integer> map1 = new TreeMap<>();
            map1.put(new BigDecimal("2.0"), 1);
            map1.put(new BigDecimal("9.0"), 9);
            final TreeMap<BigDecimal, Integer> map2 = new TreeMap<>();
            map2.put(new BigDecimal("2.00"), 2);
            map2.put(new BigDecimal("9.00"), 9);

            final Set<BigDecimal> selection = new java.util.TreeSet<>(Arrays.asList(new BigDecimal("2.0")));

            final MapDifference<Map<BigDecimal, Integer>, Map<BigDecimal, Integer>, Map<BigDecimal, Pair<Integer, Integer>>> diff = MapDifference.of(map1, map2,
                    selection);

            assertEquals(1, diff.differentValues().size());
            assertEquals(Pair.of(1, 2), diff.differentValues().get(new BigDecimal("2.0")));
            assertTrue(diff.onlyOnRight().isEmpty(), "the selected right entry must not also be reported as right-only");
            assertTrue(diff.common().isEmpty(), "9.0 was not selected, so it must be ignored entirely");
        }

        /** A common-bucket match in the filtered branch must not be double-filed either. */
        @Test
        public void testKeysToCompareBranch_commonMatchNotDoubleFiled() {
            final TreeMap<BigDecimal, String> map1 = new TreeMap<>();
            map1.put(new BigDecimal("2.0"), "x");
            final TreeMap<BigDecimal, String> map2 = new TreeMap<>();
            map2.put(new BigDecimal("2.00"), "x");

            final Set<BigDecimal> selection = new java.util.TreeSet<>(Arrays.asList(new BigDecimal("2.0")));

            final MapDifference<Map<BigDecimal, String>, Map<BigDecimal, String>, Map<BigDecimal, Pair<String, String>>> diff = MapDifference.of(map1, map2,
                    selection);

            assertEquals(CommonUtil.asMap(new BigDecimal("2.0"), "x"), diff.common());
            assertTrue(diff.onlyOnRight().isEmpty());
            assertTrue(diff.areEqual());
        }

        /**
         * A map1 whose key equality is coarser than map2's is inherently asymmetric. Nothing may be
         * silently dropped: both sides keep reporting their own entry, exactly as before the fix.
         */
        @Test
        public void testMixedKeyEquality_dropsNothing() {
            final TreeMap<String, Integer> ciMap = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
            ciMap.put("a", 1);
            final Map<String, Integer> hashMap = new HashMap<>();
            hashMap.put("A", 1);

            final MapDifference<Map<String, Integer>, Map<String, Integer>, Map<String, Pair<Integer, Integer>>> diff = MapDifference.of(ciMap, hashMap);

            assertEquals(CommonUtil.asMap("a", 1), diff.onlyOnLeft());
            assertEquals(CommonUtil.asMap("A", 1), diff.onlyOnRight());
            assertTrue(diff.common().isEmpty());
        }

        @Test
        public void testIdentityHashMap_distinctButEqualKeys() {
            final String k1 = new String("k");
            final String k2 = new String("k");
            final IdentityHashMap<String, Integer> map1 = new IdentityHashMap<>();
            map1.put(k1, 1);
            final IdentityHashMap<String, Integer> map2 = new IdentityHashMap<>();
            map2.put(k2, 1);

            final MapDifference<Map<String, Integer>, Map<String, Integer>, Map<String, Pair<Integer, Integer>>> diff = MapDifference.of(map1, map2);

            assertTrue(diff.common().isEmpty());
            assertEquals(1, diff.onlyOnLeft().size());
            assertEquals(1, diff.onlyOnRight().size());
        }

        @Test
        public void testIdentityHashMap_sameKeyObject() {
            final String k = new String("k");
            final IdentityHashMap<String, Integer> map1 = new IdentityHashMap<>();
            map1.put(k, 1);
            final IdentityHashMap<String, Integer> map2 = new IdentityHashMap<>();
            map2.put(k, 1);

            assertTrue(MapDifference.of(map1, map2).areEqual());
        }

        /** The ordinary hash-based path must be untouched by the fix. */
        @Test
        public void testPlainHashMaps_unchanged() {
            final MapDifference<Map<String, Integer>, Map<String, Integer>, Map<String, Pair<Integer, Integer>>> diff = MapDifference
                    .of(CommonUtil.asMap("a", 1, "b", 2, "c", 3), CommonUtil.asMap("b", 2, "c", 4, "d", 5));

            assertEquals(CommonUtil.asMap("b", 2), diff.common());
            assertEquals(CommonUtil.asMap("a", 1), diff.onlyOnLeft());
            assertEquals(CommonUtil.asMap("d", 5), diff.onlyOnRight());
            assertEquals(CommonUtil.asMap("c", Pair.of(3, 4)), diff.differentValues());
            assertFalse(diff.areEqual());
        }

        @Test
        public void testNullValueOnBothSidesIsStillACommonEntry() {
            final Map<String, Integer> map1 = new HashMap<>();
            map1.put("k", null);
            final Map<String, Integer> map2 = new HashMap<>();
            map2.put("k", null);

            final MapDifference<Map<String, Integer>, Map<String, Integer>, Map<String, Pair<Integer, Integer>>> diff = MapDifference.of(map1, map2);

            assertTrue(diff.common().containsKey("k"));
            assertNull(diff.common().get("k"));
            assertTrue(diff.areEqual());
        }

        @Test
        public void testNullKeyOnBothSidesInHashMaps() {
            final Map<String, Integer> map1 = new HashMap<>();
            map1.put(null, 1);
            final Map<String, Integer> map2 = new HashMap<>();
            map2.put(null, 1);

            assertTrue(MapDifference.of(map1, map2).areEqual());
        }

        /** Documented on {@code @param map2}: only map2 is probed, so only map2 must tolerate the keys. */
        @Test
        public void testNullKeyRejectedByMap2Throws() {
            final Map<String, Integer> map1 = new HashMap<>();
            map1.put(null, 1);
            map1.put("x", 2);

            assertThrows(NullPointerException.class, () -> MapDifference.of(map1, Map.of("x", 2)));
        }

        /** ...and the reverse direction does not throw, because map1 is never queried. */
        @Test
        public void testNullKeyInMap2IsFine() {
            final Map<String, Integer> map2 = new HashMap<>();
            map2.put(null, 1);
            map2.put("x", 2);

            final MapDifference<Map<String, Integer>, Map<String, Integer>, Map<String, Pair<Integer, Integer>>> diff = MapDifference.of(Map.of("x", 2), map2);

            assertEquals(CommonUtil.asMap("x", 2), diff.common());
            assertTrue(diff.onlyOnRight().containsKey(null));
        }

        /** onlyOnRight is built from map2's iteration, so an ordered map2 keeps its order. */
        @Test
        public void testOnlyOnRightPreservesMap2Order() {
            final Map<String, Integer> map1 = new HashMap<>();
            map1.put("a", 1);
            final LinkedHashMap<String, Integer> map2 = new LinkedHashMap<>();
            map2.put("z", 26);
            map2.put("y", 25);
            map2.put("x", 24);

            assertEquals(new ArrayList<>(Arrays.asList("z", "y", "x")), new ArrayList<>(MapDifference.of(map1, map2).onlyOnRight().keySet()));
        }

        /** The collection-of-maps overload builds its per-pair difference the same way. */
        @Test
        public void testCollectionOfMapsOverloadStillWorks() {
            final List<Map<String, Object>> a = Arrays.asList(CommonUtil.asMap("id", (Object) 1, "name", "John"),
                    CommonUtil.asMap("id", (Object) 2, "name", "Jane"));
            final List<Map<String, Object>> b = Arrays.asList(CommonUtil.asMap("id", (Object) 1, "name", "John"),
                    CommonUtil.asMap("id", (Object) 3, "name", "Bob"));

            final MapDifference<?, ?, ?> diff = MapDifference.of(a, b, m -> m.get("id"));

            assertEquals(1, ((List<?>) diff.common()).size());
            assertEquals(1, ((List<?>) diff.onlyOnLeft()).size());
            assertEquals(1, ((List<?>) diff.onlyOnRight()).size());
            assertTrue(((Map<?, ?>) diff.differentValues()).isEmpty());
        }
    }

    // ---------------------------------------------------------------------------------------------------
    // Fix 6 - KeyValueDifference is the only common supertype of MapDifference and BeanDifference.
    // ---------------------------------------------------------------------------------------------------

    @Test
    public void testKeyValueDifferenceIsNameableAsACommonSupertype() {
        final KeyValueDifference<?, ?, ?> fromMap = MapDifference.of(Map.of("a", 1), Map.of("a", 2));
        final KeyValueDifference<?, ?, ?> fromBean = BeanDifference.of(new ReviewBean("x", 1), new ReviewBean("x", 2));

        for (final KeyValueDifference<?, ?, ?> d : Arrays.asList(fromMap, fromBean)) {
            assertNotNull(d.differentValues());
            assertFalse(d.areEqual());
        }
    }

    // ---------------------------------------------------------------------------------------------------
    // Fix 3 - Comparators.comparingBeanByProps validates eagerly; the comparator's own throws are documented.
    // ---------------------------------------------------------------------------------------------------

    @Nested
    public class ComparingBeanByPropsValidation {

        /** Previously constructed fine and only blew up on the first compare(). */
        @Test
        public void testEmptyPropNamesRejectedByTheFactory() {
            assertThrows(IllegalArgumentException.class, () -> Comparators.comparingBeanByProps(new ArrayList<>()));
        }

        @Test
        public void testNullPropNamesRejectedByTheFactory() {
            assertThrows(IllegalArgumentException.class, () -> Comparators.comparingBeanByProps(null));
        }

        @Test
        public void testComparatorOrdersByThePropertiesInOrder() {
            final Comparator<ReviewBean> cmp = Comparators.comparingBeanByProps(Arrays.asList("name", "value"));

            assertTrue(cmp.compare(new ReviewBean("a", 1), new ReviewBean("b", 1)) < 0);
            assertTrue(cmp.compare(new ReviewBean("a", 2), new ReviewBean("a", 1)) > 0);
            assertEquals(0, cmp.compare(new ReviewBean("a", 1), new ReviewBean("a", 1)));

            final List<ReviewBean> beans = new ArrayList<>(Arrays.asList(new ReviewBean("b", 1), new ReviewBean("a", 2), new ReviewBean("a", 1)));
            beans.sort(cmp);
            assertEquals(Arrays.asList("a", "a", "b"), N.map(beans, ReviewBean::getName));
            assertEquals(Arrays.asList(1, 2, 1), N.map(beans, ReviewBean::getValue));
        }

        /** Documented: the returned comparator is NOT null-tolerant, unlike the rest of the class. */
        @Test
        public void testComparatorRejectsNullOperands() {
            final Comparator<ReviewBean> cmp = Comparators.comparingBeanByProps(Arrays.asList("name"));

            assertThrows(IllegalArgumentException.class, () -> cmp.compare(null, new ReviewBean("a", 1)));
            assertThrows(IllegalArgumentException.class, () -> cmp.compare(new ReviewBean("a", 1), null));
        }

        @Test
        public void testComparatorRejectsAnUnknownProperty() {
            final Comparator<ReviewBean> cmp = Comparators.comparingBeanByProps(Arrays.asList("noSuchProp"));

            assertThrows(IllegalArgumentException.class, () -> cmp.compare(new ReviewBean("a", 1), new ReviewBean("a", 1)));
        }

        @Test
        public void testComparatorThrowsClassCastExceptionForNonComparableValue() {
            final Comparator<BeanWithList> cmp = Comparators.comparingBeanByProps(Arrays.asList("items"));

            assertThrows(ClassCastException.class, () -> cmp.compare(new BeanWithList(), new BeanWithList()));
        }
    }

    // ---------------------------------------------------------------------------------------------------
    // Fix 5 - accept() must keep the specialized builder type so the chain survives.
    // ---------------------------------------------------------------------------------------------------

    @Nested
    public class AcceptPreservesSpecializedBuilder {

        @Test
        public void testListBuilder() {
            final List<String> result = Builder.of(new ArrayList<String>()).add("a").accept(l -> l.add("b")).add("c").val();
            assertEquals(Arrays.asList("a", "b", "c"), result);
        }

        @Test
        public void testCollectionBuilder() {
            final Set<String> result = Builder.of(new LinkedHashSet<String>()).add("a").accept(c -> c.add("b")).add("c").val();
            assertEquals(new LinkedHashSet<>(Arrays.asList("a", "b", "c")), result);
        }

        @Test
        public void testMapBuilder() {
            final Map<String, Integer> result = Builder.of(new LinkedHashMap<String, Integer>()).put("a", 1).accept(m -> m.put("b", 2)).put("c", 3).val();
            assertEquals(CommonUtil.asMap("a", 1, "b", 2, "c", 3), result);
            assertEquals(Arrays.asList("a", "b", "c"), new ArrayList<>(result.keySet()));
        }

        @Test
        public void testMultisetBuilder() {
            final Multiset<String> result = Builder.of(new Multiset<String>()).add("a").accept(m -> m.add("a")).add("b").val();
            assertEquals(2, result.getCount("a"));
            assertEquals(1, result.getCount("b"));
        }

        @Test
        public void testMultimapBuilder() {
            final ListMultimap<String, String> mm = CommonUtil.newListMultimap();
            Builder.of(mm).put("k", "a").accept(m -> m.put("k", "b")).put("k", "c");
            assertEquals(Arrays.asList("a", "b", "c"), mm.get("k"));
        }

        @Test
        public void testDatasetBuilder() {
            final Dataset ds = Dataset.rows(Arrays.asList("name", "age"), new Object[][] { { "Alice", 25 }, { "Bob", 30 } });
            Builder.of(ds).renameColumn("age", "years").accept(d -> d.removeColumn("name")).renameColumn("years", "yrs");
            assertEquals(Arrays.asList("yrs"), new ArrayList<>(ds.columnNames()));
        }

        @Test
        public void testAllEightPrimitiveListBuilders() {
            assertEquals(BooleanList.of(true, false, true), Builder.of(BooleanList.of(true)).accept(l -> l.add(false)).add(true).val());
            assertEquals(CharList.of('a', 'b', 'c'), Builder.of(CharList.of('a')).accept(l -> l.add('b')).add('c').val());
            assertEquals(ByteList.of((byte) 1, (byte) 2, (byte) 3), Builder.of(ByteList.of((byte) 1)).accept(l -> l.add((byte) 2)).add((byte) 3).val());
            assertEquals(ShortList.of((short) 1, (short) 2, (short) 3), Builder.of(ShortList.of((short) 1)).accept(l -> l.add((short) 2)).add((short) 3).val());
            assertEquals(IntList.of(1, 2, 3), Builder.of(IntList.of(1)).accept(l -> l.add(2)).add(3).val());
            assertEquals(LongList.of(1L, 2L, 3L), Builder.of(LongList.of(1L)).accept(l -> l.add(2L)).add(3L).val());
            assertEquals(FloatList.of(1.5f, 2.5f, 3.5f), Builder.of(FloatList.of(1.5f)).accept(l -> l.add(2.5f)).add(3.5f).val());
            assertEquals(DoubleList.of(1.0, 2.0, 3.0), Builder.of(DoubleList.of(1.0)).accept(l -> l.add(2.0)).add(3.0).val());
        }

        @Test
        public void testAcceptStillReturnsTheSameBuilderInstanceAndValidatesItsArgument() {
            final Builder.ListBuilder<String, ArrayList<String>> lb = Builder.of(new ArrayList<String>());

            assertSame(lb, lb.accept(l -> l.add("a")));
            assertThrows(IllegalArgumentException.class, () -> lb.accept(null));
        }
    }

    // ---------------------------------------------------------------------------------------------------
    // Fix 4 - the documented non-transitivity of the tolerance overloads.
    // ---------------------------------------------------------------------------------------------------

    @Nested
    public class FuzzyComparisonIsNotTransitive {

        /** The exact counter-example quoted in the javadoc, so the doc cannot drift from the behaviour. */
        @Test
        public void testDoubleCompareCounterExample() {
            assertEquals(0, Builder.compare(0d, 0.009d, 0.01d).result());
            assertEquals(0, Builder.compare(0.009d, 0.018d, 0.01d).result());
            assertTrue(Builder.compare(0d, 0.018d, 0.01d).result() < 0);
        }

        @Test
        public void testFloatCompareCounterExample() {
            assertEquals(0, Builder.compare(0f, 0.009f, 0.01f).result());
            assertEquals(0, Builder.compare(0.009f, 0.018f, 0.01f).result());
            assertTrue(Builder.compare(0f, 0.018f, 0.01f).result() < 0);
        }

        @Test
        public void testDoubleEqualsCounterExample() {
            assertTrue(Builder.equals(0d, 0.009d, 0.01d).result());
            assertTrue(Builder.equals(0.009d, 0.018d, 0.01d).result());
            assertFalse(Builder.equals(0d, 0.018d, 0.01d).result());
        }

        @Test
        public void testFloatEqualsCounterExample() {
            assertTrue(Builder.equals(0f, 0.009f, 0.01f).result());
            assertTrue(Builder.equals(0.009f, 0.018f, 0.01f).result());
            assertFalse(Builder.equals(0f, 0.018f, 0.01f).result());
        }

        /** Tolerance is still validated before the chain short-circuits (locked by the 2026-08-31 pass). */
        @Test
        public void testToleranceValidatedEvenAfterTheChainHasDecided() {
            assertThrows(IllegalArgumentException.class, () -> Builder.compare(1, 2).compare(1d, 2d, -1d));
            assertThrows(IllegalArgumentException.class, () -> Builder.compare(1, 2).compare(1d, 2d, Double.NaN));
            assertThrows(IllegalArgumentException.class, () -> Builder.equals(1, 2).equals(1d, 2d, -1d));
            assertThrows(IllegalArgumentException.class, () -> Builder.equals(1, 2).equals(1d, 2d, Double.NaN));
        }
    }

    // ---------------------------------------------------------------------------------------------------
    // Fix 7 / 11 - documented example values and the ambiguity work-arounds.
    // ---------------------------------------------------------------------------------------------------

    @Test
    public void testByteAndShortCompareOnlyPromiseASign() {
        assertTrue(Builder.compare((byte) 10, (byte) 20).result() < 0);
        assertTrue(Builder.compare((short) 100, (short) 200).result() < 0);
        assertTrue(Builder.compare((byte) 20, (byte) 10).result() > 0);
        assertEquals(0, Builder.compare((byte) 10, (byte) 10).result());
    }

    /** The casts the class javadoc tells callers to use must actually resolve and agree. */
    @Test
    public void testPrimitiveWrapperAmbiguityWorkArounds() {
        final int count = 7;
        final Integer other = 7;

        assertTrue(Builder.equals(count, (int) other).result());
        assertTrue(Builder.equals((Object) count, other).result());
        assertEquals(0, Builder.compare(count, (int) other).result());
        assertEquals(0, Builder.compare((Integer) count, other).result());
    }

    // ---------------------------------------------------------------------------------------------------
    // Fix 8 / 9 / 14 - Comparators documentation locks.
    // ---------------------------------------------------------------------------------------------------

    @Nested
    public class ComparatorsDocumentedBehaviour {

        @Test
        public void testComparingObjArrayAppliesTheComparatorUnchecked() {
            final Comparator<Object[]> cmp = Comparators.comparingObjArray(String.CASE_INSENSITIVE_ORDER);

            assertTrue(cmp.compare(new Object[] { "apple" }, new Object[] { "BANANA" }) < 0);
            assertThrows(ClassCastException.class, () -> cmp.compare(new Object[] { 1 }, new Object[] { 2 }));
        }

        @Test
        public void testComparingIgnoreCaseIsSimpleFoldingAndLocaleIndependent() {
            // supplementary code points DO fold (Deseret capital / small long I)
            final String desUpper = new String(Character.toChars(0x10400));
            final String desLower = new String(Character.toChars(0x10428));
            assertEquals(0, Comparators.comparingIgnoreCase().compare(desUpper, desLower));

            // ...but folding is simple, not full: sharp s does not match "ss"
            assertTrue(Comparators.comparingIgnoreCase().compare("straße", "STRASSE") != 0);

            // ...and no locale is consulted.
            assertEquals(0, Comparators.comparingIgnoreCase().compare("I", "ı"));

            // nulls first, as documented
            assertTrue(Comparators.comparingIgnoreCase().compare(null, "a") < 0);
            assertEquals(0, Comparators.comparingIgnoreCase().compare(null, null));
        }

        @Test
        public void testComparingByLengthCountsCodeUnitsNotCodePoints() {
            final Comparator<String> cmp = Comparators.comparingByLength();

            // one supplementary code point == two code units, so it ties with "ab"
            assertEquals(0, cmp.compare("𝄞", "ab"));
            assertTrue(cmp.compare("a", "ab") < 0);
            assertEquals(0, cmp.compare(null, ""));
        }

        @Test
        public void testComparingByArrayLengthTreatsNullAsZeroAndRejectsNonArrays() {
            final Comparator<Object> cmp = Comparators.comparingByArrayLength();

            assertEquals(0, cmp.compare(null, new int[0]));
            assertTrue(cmp.compare(new int[] { 1 }, new int[] { 1, 2 }) < 0);
            assertTrue(cmp.compare(new String[] { "a", "b" }, new int[] { 1 }) > 0);
            assertThrows(IllegalArgumentException.class, () -> cmp.compare("not an array", new int[0]));
        }

        @Test
        public void testComparingIteratorIdentityShortCircuitDoesNotConsume() {
            final java.util.Iterator<String> it = Arrays.asList("a", "b").iterator();

            assertEquals(0, Comparators.<String, java.util.Iterator<String>> comparingIterator().compare(it, it));
            assertTrue(it.hasNext(), "the identity short-circuit must not consume the iterator");
        }

        @Test
        public void testComparingIteratorNullHandling() {
            final Comparator<java.util.Iterator<Integer>> cmp = Comparators.comparingIterator();

            assertEquals(0, cmp.compare(null, null));
            assertTrue(cmp.compare(null, Collections.<Integer> emptyList().iterator()) < 0);
            assertTrue(cmp.compare(Collections.<Integer> emptyList().iterator(), null) > 0);
            assertTrue(cmp.compare(Arrays.asList(1, 2).iterator(), Arrays.asList(1, 3).iterator()) < 0);
        }
    }

    // ---------------------------------------------------------------------------------------------------
    // Fix 10 - the MultimapBuilder bulk methods are null/empty tolerant, as now documented.
    // ---------------------------------------------------------------------------------------------------

    @Test
    public void testMultimapBuilderBulkMethodsAreNullAndEmptyTolerant() {
        final ListMultimap<String, String> mm = CommonUtil.newListMultimap();
        mm.put("k", "a");

        final Builder.MultimapBuilder<String, String, List<String>, ListMultimap<String, String>> b = Builder.of(mm);

        b.put((Map<String, String>) null);
        b.putMany("k", (Collection<String>) null);
        b.putMany((Map<String, Collection<String>>) null);
        b.putMany((Multimap<String, String, List<String>>) null);
        b.removeOne((Map<String, String>) null);
        b.removeMany("k", (Collection<String>) null);
        b.removeMany((Map<String, Collection<String>>) null);
        b.removeMany((Multimap<String, String, List<String>>) null);

        b.put(new HashMap<>());
        b.putMany("k", new ArrayList<>());
        b.removeMany("k", new ArrayList<>());

        assertEquals(Arrays.asList("a"), mm.get("k"));
    }

    // ---------------------------------------------------------------------------------------------------
    // Test fixtures
    // ---------------------------------------------------------------------------------------------------

    public static class ReviewBean {
        private String name;
        private Integer value;

        public ReviewBean() {
        }

        public ReviewBean(final String name, final Integer value) {
            this.name = name;
            this.value = value;
        }

        public String getName() {
            return name;
        }

        public void setName(final String name) {
            this.name = name;
        }

        public Integer getValue() {
            return value;
        }

        public void setValue(final Integer value) {
            this.value = value;
        }
    }

    public static class BeanWithList {
        private List<String> items = new ArrayList<>(Arrays.asList("a"));

        public BeanWithList() {
        }

        public List<String> getItems() {
            return items;
        }

        public void setItems(final List<String> items) {
            this.items = items;
        }
    }
}

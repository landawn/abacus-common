package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

/**
 * Regression tests for the 2026-08-31 {@link Builder} review. One nested section per finding.
 */
public class BuilderRegressionTest extends TestBase {

    /**
     * B3: the insertion index is validated even when the collection is {@code null} or empty, so an
     * out-of-range index is reported consistently rather than only when there is something to insert.
     */
    @Nested
    public class ListBuilderAddAllAtIndexValidatesTheIndex extends TestBase {

        @Test
        public void emptyCollectionWithAnOutOfRangeIndexThrows() {
            final List<String> list = new ArrayList<>(Arrays.asList("a"));

            assertThrows(IndexOutOfBoundsException.class, () -> Builder.of(list).addAll(999, new ArrayList<>()));
            assertEquals(Arrays.asList("a"), list);
        }

        @Test
        public void nullCollectionWithAnOutOfRangeIndexThrows() {
            final List<String> list = new ArrayList<>(Arrays.asList("a"));

            assertThrows(IndexOutOfBoundsException.class, () -> Builder.of(list).addAll(999, (List<String>) null));
            assertEquals(Arrays.asList("a"), list);
        }

        @Test
        public void negativeIndexThrowsForEveryCollectionShape() {
            final List<String> list = new ArrayList<>(Arrays.asList("a"));

            assertThrows(IndexOutOfBoundsException.class, () -> Builder.of(list).addAll(-1, new ArrayList<>()));
            assertThrows(IndexOutOfBoundsException.class, () -> Builder.of(list).addAll(-1, Arrays.asList("z")));
        }

        @Test
        public void nonEmptyCollectionWithAnOutOfRangeIndexStillThrows() {
            final List<String> list = new ArrayList<>(Arrays.asList("a"));

            assertThrows(IndexOutOfBoundsException.class, () -> Builder.of(list).addAll(999, Arrays.asList("z")));
        }

        @Test
        public void inRangeIndexWithAnEmptyCollectionIsANoOp() {
            final List<String> list = new ArrayList<>(Arrays.asList("a", "b"));

            Builder.of(list).addAll(1, new ArrayList<>()).addAll(2, (List<String>) null);

            assertEquals(Arrays.asList("a", "b"), list);
        }

        @Test
        public void theEndPositionIsValid() {
            final List<String> list = new ArrayList<>(Arrays.asList("a", "b"));

            Builder.of(list).addAll(2, Arrays.asList("c"));

            assertEquals(Arrays.asList("a", "b", "c"), list);
        }

        @Test
        public void insertionStillWorks() {
            final List<String> list = new ArrayList<>(Arrays.asList("a", "d"));

            Builder.of(list).addAll(1, Arrays.asList("b", "c"));

            assertEquals(Arrays.asList("a", "b", "c", "d"), list);
        }
    }

    /**
     * B5: the tolerance is validated before the chain's short-circuit, so the documented
     * {@link IllegalArgumentException} no longer depends on whether an earlier comparison already decided
     * the result.
     */
    @Nested
    public class FuzzyToleranceIsValidatedEagerly extends TestBase {

        @Test
        public void comparisonBuilderDoubleRejectsNegativeToleranceAfterShortCircuit() {
            assertThrows(IllegalArgumentException.class, () -> Builder.compare(1, 2).compare(1.0, 2.0, -5.0));
        }

        @Test
        public void comparisonBuilderDoubleRejectsNaNToleranceAfterShortCircuit() {
            assertThrows(IllegalArgumentException.class, () -> Builder.compare(1, 2).compare(1.0, 2.0, Double.NaN));
        }

        @Test
        public void comparisonBuilderFloatRejectsNegativeToleranceAfterShortCircuit() {
            assertThrows(IllegalArgumentException.class, () -> Builder.compare(1, 2).compare(1.0f, 2.0f, -5.0f));
        }

        @Test
        public void comparisonBuilderFloatRejectsNaNToleranceAfterShortCircuit() {
            assertThrows(IllegalArgumentException.class, () -> Builder.compare(1, 2).compare(1.0f, 2.0f, Float.NaN));
        }

        @Test
        public void equivalenceBuilderDoubleRejectsBadToleranceAfterShortCircuit() {
            assertThrows(IllegalArgumentException.class, () -> Builder.equals(1, 2).equals(1.0, 2.0, -5.0));
            assertThrows(IllegalArgumentException.class, () -> Builder.equals(1, 2).equals(1.0, 2.0, Double.NaN));
        }

        @Test
        public void equivalenceBuilderFloatRejectsBadToleranceAfterShortCircuit() {
            assertThrows(IllegalArgumentException.class, () -> Builder.equals(1, 2).equals(1.0f, 2.0f, -5.0f));
            assertThrows(IllegalArgumentException.class, () -> Builder.equals(1, 2).equals(1.0f, 2.0f, Float.NaN));
        }

        @Test
        public void theNonShortCircuitedPathStillThrowsTheSameException() {
            final IllegalArgumentException eager = assertThrows(IllegalArgumentException.class, () -> Builder.compare(1, 1).compare(1.0, 2.0, -5.0));
            final IllegalArgumentException direct = assertThrows(IllegalArgumentException.class, () -> Numbers.fuzzyCompare(1.0, 2.0, -5.0));

            assertEquals(direct.getMessage(), eager.getMessage());
        }

        @Test
        public void theStaticFactoriesRejectBadTolerancesToo() {
            assertThrows(IllegalArgumentException.class, () -> Builder.compare(1.0, 2.0, -5.0));
            assertThrows(IllegalArgumentException.class, () -> Builder.compare(1.0f, 2.0f, Float.NaN));
            assertThrows(IllegalArgumentException.class, () -> Builder.equals(1.0, 2.0, -5.0));
            assertThrows(IllegalArgumentException.class, () -> Builder.equals(1.0f, 2.0f, Float.NaN));
        }

        @Test
        public void zeroAndPositiveInfinityAreAcceptedAndStillCompare() {
            assertEquals(0, Builder.compare(1.0, 1.0, 0.0).result());
            assertEquals(0, Builder.compare(1.0, 2.0, Double.POSITIVE_INFINITY).result());
            assertTrue(Builder.equals(1.0, 1.0, 0.0).result());
            assertTrue(Builder.equals(1.0, 2.0, Double.POSITIVE_INFINITY).result());
        }

        @Test
        public void validTolerancesStillBehaveAsBefore() {
            assertEquals(0, Builder.compare(1.00001, 1.00002, 0.0001).result());
            assertTrue(Builder.equals(1.0001f, 1.0002f, 0.001f).result());
            assertTrue(Builder.compare(1.0, 2.0, 0.1).result() < 0);
            assertFalse(Builder.equals(1.0, 2.0, 0.1).result());
        }
    }

    /** D2: a multimap with a different value-collection type can be copied in, matching Multimap.putValues. */
    @Nested
    public class MultimapBuilderPutManyAcceptsAnyValueCollectionType extends TestBase {

        @Test
        public void aSetMultimapCanBeCopiedIntoAListMultimap() {
            final ListMultimap<String, Integer> target = CommonUtil.newListMultimap();
            final SetMultimap<String, Integer> src = CommonUtil.newSetMultimap();
            src.put("k", 1);
            src.put("k", 2);

            Builder.of(target).putMany(src);

            assertEquals(2, target.get("k").size());
            assertTrue(target.get("k").containsAll(Arrays.asList(1, 2)));
        }

        @Test
        public void aListMultimapCanBeCopiedIntoASetMultimap() {
            final SetMultimap<String, Integer> target = CommonUtil.newSetMultimap();
            final ListMultimap<String, Integer> src = CommonUtil.newListMultimap();
            src.put("k", 1);
            src.put("k", 1);

            Builder.of(target).putMany(src);

            assertEquals(1, target.get("k").size()); // the Set collapses the duplicate
        }

        @Test
        public void sameTypeStillWorks() {
            final ListMultimap<String, Integer> target = CommonUtil.newListMultimap();
            final ListMultimap<String, Integer> src = CommonUtil.newListMultimap();
            src.put("k", 1);

            Builder.of(target).putMany(src);

            assertEquals(Arrays.asList(1), target.get("k"));
        }
    }

    /** D3: the Dataset column parameter accepts any Collection, as Dataset.addColumn itself does. */
    @Nested
    public class DatasetBuilderAddColumnAcceptsAnyCollection extends TestBase {

        private Dataset newDataset() {
            return Dataset.rows(Arrays.asList("name"), new Object[][] { { "Alice" }, { "Bob" } });
        }

        @Test
        public void aSetCanBeUsedAsAColumn() {
            final Dataset ds = newDataset();
            final Set<Object> col = new LinkedHashSet<>(Arrays.asList(25, 30));

            Builder.of(ds).addColumn("age", col);

            assertEquals(Arrays.asList(25, 30), ds.getColumn("age"));
        }

        @Test
        public void aSetCanBeUsedAsAColumnAtAnIndex() {
            final Dataset ds = newDataset();
            final Set<Object> col = new LinkedHashSet<>(Arrays.asList(25, 30));

            Builder.of(ds).addColumn(0, "age", col);

            assertEquals(Arrays.asList("age", "name"), ds.columnNames());
            assertEquals(Arrays.asList(25, 30), ds.getColumn("age"));
        }

        @Test
        public void aListStillWorks() {
            final Dataset ds = newDataset();

            Builder.of(ds).addColumn("age", Arrays.asList(25, 30));

            assertEquals(Arrays.asList(25, 30), ds.getColumn("age"));
        }

        /** Validates the newly documented {@code @throws IllegalStateException} on the DatasetBuilder mutators. */
        @Test
        public void mutatingAFrozenDatasetThroughTheBuilderThrowsIllegalStateException() {
            final Dataset ds = newDataset();
            ds.freeze();

            assertThrows(IllegalStateException.class, () -> Builder.of(ds).addColumn("age", Arrays.asList(25, 30)));
            assertThrows(IllegalStateException.class, () -> Builder.of(ds).removeColumn("name"));
            assertThrows(IllegalStateException.class, () -> Builder.of(ds).updateAll(v -> v));
        }
    }

    /** D4: the three invariant generic parameters now accept the wider argument types they always should have. */
    @Nested
    public class GenericWidenings extends TestBase {

        @Test
        public void putIfAbsentAcceptsASupplierOfASubtype() {
            final Map<String, Number> m = new HashMap<>();
            final Supplier<Integer> s = () -> 1;

            Builder.of(m).putIfAbsentBySupplier("k", s);

            assertEquals(1, m.get("k"));
        }

        @Test
        public void putIfAbsentSupplierIsNotInvokedWhenPresent() {
            final Map<String, Number> m = new HashMap<>();
            m.put("k", 99);

            Builder.of(m).putIfAbsentBySupplier("k", () -> {
                throw new AssertionError("supplier must not run");
            });

            assertEquals(99, m.get("k"));
        }

        @Test
        public void putIfAbsentRejectsANullSupplier() {
            final Map<String, Number> m = new HashMap<>();

            assertThrows(IllegalArgumentException.class, () -> Builder.of(m).putIfAbsentBySupplier("k", (Supplier<Number>) null));
        }

        @Test
        public void compareAcceptsASuperTypedComparator() {
            final Comparator<Object> byToString = Comparator.comparing(Object::toString);
            final Comparator<CharSequence> byLength = Comparator.comparingInt(CharSequence::length);

            assertTrue(Builder.compare("a", "b", byToString).result() < 0);
            assertTrue(Builder.compare("a", "bb", byLength).result() < 0);
        }

        @Test
        public void chainedCompareAcceptsASuperTypedComparator() {
            final Comparator<CharSequence> byLength = Comparator.comparingInt(CharSequence::length);

            assertTrue(Builder.compare("a", "a").compare("a", "bb", byLength).result() < 0);
        }

        @Test
        public void compareRejectsANullComparator() {
            assertThrows(IllegalArgumentException.class, () -> Builder.compare("a", "b", (Comparator<String>) null));
            assertThrows(IllegalArgumentException.class, () -> Builder.compare("a", "a").compare("a", "b", (Comparator<String>) null));
        }
    }

    /**
     * B2 (doc-only): the running total starts at 0. These lock the documented consequences so the behaviour is
     * not changed by accident; the seed is deliberate, not a defect.
     */
    @Nested
    public class HashCodeBuilderSeedIsZero extends TestBase {

        @Test
        public void aSingleValueChainIsTheIdentity() {
            assertEquals("test".hashCode(), Builder.hash("test").result());
            assertEquals(CommonUtil.hashCode(42), Builder.hash(42).result());
        }

        @Test
        public void atAFixedChainLengthTheSeedIsOnlyAConstantOffset() {
            // The javadoc's core justification: for a chain of fixed length, seeding at 1 instead of 0 shifts
            // every result by the same constant (31^length), so the distribution is unchanged.
            for (final Object[] pair : new Object[][] { { "a", "b" }, { "b", "a" }, { "a", null }, { null, "a" }, { 0, 0 } }) {
                final int seed0 = Builder.hash(pair[0]).hash(pair[1]).result();
                final int seed1 = (31 * (31 * 1 + CommonUtil.hashCode(pair[0]))) + CommonUtil.hashCode(pair[1]);

                assertEquals(31 * 31, seed1 - seed0);
            }
        }

        @Test
        public void distinctFieldVectorsOfTheSameArityStayDistinct() {
            final int ab = Builder.hash("a").hash("b").result();
            final int ba = Builder.hash("b").hash("a").result();
            final int an = Builder.hash("a").hash((Object) null).result();
            final int na = Builder.hash((Object) null).hash("a").result();

            assertEquals(4, new java.util.HashSet<>(Arrays.asList(ab, ba, an, na)).size());
        }

        @Test
        public void chainsOfDifferentLengthMayCollideOnLeadingZeroHashes() {
            // Documented consequence of the zero seed; asserted so the doc and the code stay in step.
            assertEquals(Builder.hash("a").result(), Builder.hash((Object) null).hash("a").result());
            assertEquals(Builder.hash("a").result(), Builder.hash(0).hash(0).hash("a").result());
        }

        @Test
        public void objectsHashIsTheAlternativeWhenArityVaries() {
            assertTrue(Objects.hash((Object) null, "a") != Objects.hash("a"));
        }
    }

    /** J5 (doc-only): arrays go through N.equals / N.hashCode, i.e. by identity. */
    @Nested
    public class ArraysAreComparedAndHashedByIdentity extends TestBase {

        @Test
        public void twoEqualContentArraysAreNotEqual() {
            final int[] x = { 1, 2 };
            final int[] y = { 1, 2 };

            assertFalse(Builder.equals(x, y).result());
            assertTrue(Builder.equals(x, x).result());
        }

        @Test
        public void twoEqualContentArraysHashDifferently() {
            final int[] x = { 1, 2 };
            final int[] y = { 1, 2 };

            assertTrue(Builder.hash(x).result() != Builder.hash(y).result());
        }

        @Test
        public void contentSemanticsAreAvailableThroughTheDocumentedEscapeHatches() {
            final int[] x = { 1, 2 };
            final int[] y = { 1, 2 };

            assertTrue(Builder.equals(x, y, N::deepEquals).result());
            assertEquals(Builder.hash(x, Arrays::hashCode).result(), Builder.hash(y, Arrays::hashCode).result());
        }
    }
}

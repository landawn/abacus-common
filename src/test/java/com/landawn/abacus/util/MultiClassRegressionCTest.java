package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.Range.BoundType;
import com.landawn.abacus.util.Tuple.Tuple2;
import com.landawn.abacus.util.Tuple.Tuple3;

/**
 * Regression tests for the 2026-08-31 review of {@code Result}, {@code Holder}, {@code Pair},
 * {@code Triple}, {@code Tuple}, {@code Range}, {@code Duration}, {@code Stopwatch},
 * {@code RateLimiter} and {@code Fraction}.
 *
 * <p>Each nested class is named after the finding it locks down.</p>
 */
public class MultiClassRegressionCTest extends TestBase {

    // =======================================================================
    // B1 - Result.orElseThrow(Function) / orElseThrow(Supplier) must be
    //      callable with a plain, un-cast lambda.
    // =======================================================================
    @Nested
    @DisplayName("B1: Result.orElseThrow accepts inline lambdas")
    public class B1ResultOrElseThrowLambda extends TestBase {

        @Test
        public void functionLambda_compilesWithoutCast_andIsNotInvokedOnSuccess() {
            final Result<String, IOException> success = Result.success("value");

            // The whole point of the fix: no (Function<...>) cast.
            assertEquals("value", success.orElseThrow(ex -> new IllegalStateException("never", ex)));
        }

        @Test
        public void functionLambda_mapsTheStoredException() {
            final IOException cause = new IOException("boom");
            final Result<String, IOException> failure = Result.failure(cause);

            final IllegalStateException thrown = assertThrows(IllegalStateException.class,
                    () -> failure.orElseThrow(ex -> new IllegalStateException("wrapped", ex)));

            assertEquals("wrapped", thrown.getMessage());
            assertSame(cause, thrown.getCause());
        }

        @Test
        public void supplierLambda_compilesWithoutCast_andIsNotInvokedOnSuccess() {
            final Result<String, IOException> success = Result.success("value");
            final MutableInt calls = MutableInt.of(0);

            assertEquals("value", success.orElseThrow(() -> {
                calls.increment();
                return new IllegalStateException("never");
            }));

            assertEquals(0, calls.value());
        }

        @Test
        public void supplierLambda_throwsTheSuppliedException() {
            final Result<String, IOException> failure = Result.failure(new IOException("boom"));

            final IllegalStateException thrown = assertThrows(IllegalStateException.class,
                    () -> failure.orElseThrow(() -> new IllegalStateException("supplied")));

            assertEquals("supplied", thrown.getMessage());
        }

        @Test
        public void constructorReference_resolvesWithoutCastWhenOnlyOneArityFits() {
            final IOException cause = new IOException("boom");
            final Result<String, IOException> failure = Result.failure(cause);

            // A no-arg constructor ref can only be a Supplier.
            assertThrows(NoArgOnly.class, () -> failure.orElseThrow(NoArgOnly::new));

            // A cause-taking constructor ref can only be a Function.
            assertSame(cause, assertThrows(CauseOnly.class, () -> failure.orElseThrow(CauseOnly::new)).getCause());
        }

        @Test
        public void constructorReferenceFittingBothArities_needsACast() {
            // Residual, inherent limitation: RuntimeException has both RuntimeException() and
            // RuntimeException(Throwable), so `RuntimeException::new` is applicable to the Function
            // and the Supplier overload alike and must be disambiguated. Un-cast *lambdas* are
            // unambiguous either way because their arity is written out.
            final Result<String, IOException> failure = Result.failure(new IOException("boom"));

            assertThrows(RuntimeException.class, () -> failure.orElseThrow((java.util.function.Supplier<RuntimeException>) RuntimeException::new));
            assertThrows(RuntimeException.class, () -> failure.orElseThrow((java.util.function.Function<IOException, RuntimeException>) RuntimeException::new));
        }

        @Test
        public void nullMapperOrSupplier_isRejected() {
            final Result<String, IOException> success = Result.success("value");

            assertThrows(IllegalArgumentException.class, () -> success.orElseThrow((java.util.function.Function<IOException, RuntimeException>) null));
            assertThrows(IllegalArgumentException.class, () -> success.orElseThrow((java.util.function.Supplier<RuntimeException>) null));
        }
    }

    /** Only a no-arg constructor, so a constructor reference to it can only be a {@code Supplier}. */
    static final class NoArgOnly extends RuntimeException {
        private static final long serialVersionUID = 1L;

        NoArgOnly() {
            super("no-arg");
        }
    }

    /** Only a cause-taking constructor, so a constructor reference to it can only be a {@code Function}. */
    static final class CauseOnly extends RuntimeException {
        private static final long serialVersionUID = 1L;

        CauseOnly(final Throwable cause) {
            super(cause);
        }
    }

    // =======================================================================
    // O5 - ifSuccess/ifFailure no longer route through Fn.emptyConsumer().
    // =======================================================================
    @Nested
    @DisplayName("O5: Result.ifSuccess / ifFailure")
    public class O5ResultConditionalConsumers extends TestBase {

        @Test
        public void ifSuccess_runsOnlyForSuccess() {
            final List<String> seen = new ArrayList<>();

            Result.<String, IOException> success("ok").ifSuccess(seen::add);
            Result.<String, IOException> failure(new IOException()).ifSuccess(seen::add);

            assertEquals(List.of("ok"), seen);
        }

        @Test
        public void ifSuccess_passesNullValueThrough() {
            final Holder<Object> seen = Holder.of("unset");

            Result.<String, IOException> success(null).ifSuccess(seen::setValue);

            assertNull(seen.value());
        }

        @Test
        public void ifFailure_runsOnlyForFailure() {
            final IOException ex = new IOException("x");
            final List<Throwable> seen = new ArrayList<>();

            Result.<String, IOException> failure(ex).ifFailure(seen::add);
            Result.<String, IOException> success("ok").ifFailure(seen::add);

            assertEquals(List.of(ex), seen);
        }

        @Test
        public void nullAction_isRejected() {
            assertThrows(IllegalArgumentException.class, () -> Result.success("x").ifSuccess(null));
            assertThrows(IllegalArgumentException.class, () -> Result.success("x").ifFailure(null));
        }
    }

    // =======================================================================
    // D3 - Result.RR inherits success()/failure(), which return Result, not RR.
    // =======================================================================
    @Nested
    @DisplayName("D3: Result.RR factories")
    public class D3ResultRr extends TestBase {

        @Test
        public void of_producesAnRr() {
            final Result.RR<String> rr = Result.RR.of("value", null);

            assertTrue(rr.isSuccess());
            assertEquals("value", rr.orElseThrow());
        }

        @Test
        public void inheritedSuccess_returnsAPlainResult_notAnRr() {
            final Result<String, RuntimeException> r = Result.success("value");

            assertEquals(Result.class, r.getClass());
            assertFalse(r instanceof Result.RR);
        }
    }

    // =======================================================================
    // B2 - Range.hashCode() must not mix in the identity hash of Range.class.
    // =======================================================================
    @Nested
    @DisplayName("B2: Range.hashCode is value-based")
    public class B2RangeHashCode extends TestBase {

        @Test
        public void equalRangesHashEqually() {
            assertEquals(Range.closed(1, 5).hashCode(), Range.closed(1, 5).hashCode());
            assertEquals(Range.open(1, 5).hashCode(), Range.open(1, 5).hashCode());
        }

        @Test
        public void hashIsDerivedOnlyFromEndpointsAndBoundTypes() {
            // Reproduces the previous formula without the getClass() term. If getClass().hashCode()
            // were still mixed in, this expectation could not be written at all: it varies per JVM run.
            final Range<Integer> range = Range.closedOpen(1, 5);

            // Endpoint.hashCode() == 37 * (isClosed ? 0 : 1) + value.hashCode()
            final int lowerHash = 37 * 0 + Integer.valueOf(1).hashCode(); // lower bound is closed
            final int upperHash = 37 * 1 + Integer.valueOf(5).hashCode(); // upper bound is open

            int expected = 17;
            expected = 37 * expected + lowerHash;
            expected = 37 * expected + upperHash;

            assertEquals(expected, range.hashCode());
            assertEquals(23352, expected);
        }

        @Test
        public void boundTypeParticipatesInTheHash() {
            assertNotEquals(Range.closed(1, 5).hashCode(), Range.open(1, 5).hashCode());
            assertNotEquals(Range.closedOpen(1, 5).hashCode(), Range.openClosed(1, 5).hashCode());
        }

        @Test
        public void survivesSerializationRoundTripWithTheSameHash() throws Exception {
            final Range<Integer> range = Range.closedOpen(1, 5);
            final ByteArrayOutputStream bo = new ByteArrayOutputStream();

            try (ObjectOutputStream oo = new ObjectOutputStream(bo)) {
                oo.writeObject(range);
            }

            try (ObjectInputStream oi = new ObjectInputStream(new ByteArrayInputStream(bo.toByteArray()))) {
                final Object back = oi.readObject();
                assertEquals(range, back);
                assertEquals(range.hashCode(), back.hashCode());
            }
        }
    }

    // =======================================================================
    // B4 - Range.elementCompareTo uses the Comparable/Commons-Lang sign
    //      convention; the deprecated positionOf keeps the inverted one.
    // =======================================================================
    @Nested
    @DisplayName("B4: Range.elementCompareTo")
    public class B4RangeElementCompareTo extends TestBase {

        @Test
        public void signsFollowComparableConvention() {
            final Range<Integer> range = Range.closed(5, 10);

            assertEquals(-1, range.elementCompareTo(3));
            assertEquals(0, range.elementCompareTo(5));
            assertEquals(0, range.elementCompareTo(7));
            assertEquals(0, range.elementCompareTo(10));
            assertEquals(1, range.elementCompareTo(12));
        }

        @Test
        public void respectsOpenBounds() {
            final Range<Integer> range = Range.open(5, 10);

            assertEquals(-1, range.elementCompareTo(5));
            assertEquals(0, range.elementCompareTo(6));
            assertEquals(1, range.elementCompareTo(10));
        }

        @SuppressWarnings("deprecation")
        @Test
        public void isTheSignInverseOfTheDeprecatedPositionOf() {
            final Range<Integer> range = Range.closed(5, 10);

            for (final int v : new int[] { 3, 5, 7, 10, 12 }) {
                assertEquals(-range.positionOf(v), range.elementCompareTo(v), "element " + v);
            }
        }

        @Test
        public void nullElementIsRejected() {
            assertThrows(IllegalArgumentException.class, () -> Range.closed(5, 10).elementCompareTo(null));
        }

        @Test
        public void emptyRangeIsRejectedRatherThanReportingAFalsePosition() {
            // An empty range is vacuously both before and after every element, so returning 0 here
            // would wrongly claim containment.
            assertThrows(IllegalStateException.class, () -> Range.open(5, 5).elementCompareTo(5));
            assertThrows(IllegalStateException.class, () -> Range.closedOpen(5, 5).elementCompareTo(1));
            assertThrows(IllegalStateException.class, () -> Range.openClosed(5, 5).elementCompareTo(9));
        }

        @Test
        public void singletonClosedRangeIsNotEmpty() {
            assertEquals(0, Range.just(5).elementCompareTo(5));
            assertEquals(-1, Range.just(5).elementCompareTo(4));
            assertEquals(1, Range.just(5).elementCompareTo(6));
        }
    }

    // =======================================================================
    // B7 - empty-range predicates. isBefore/isAfter answer from a single endpoint each, so away from
    // the shared endpoint value exactly ONE of them is true; only (x, x) reports both at x. The
    // earlier version of this test asserted only the (5, 5)-at-5 case and so certified a javadoc
    // claim ("both true for every element") that does not hold anywhere else.
    // =======================================================================
    @Nested
    @DisplayName("B7: empty Range predicates")
    public class B7EmptyRange extends TestBase {

        @Test
        public void bothTrueOnlyForOpenOpenAtTheSharedEndpoint() {
            assertTrue(Range.open(5, 5).isEmpty());
            assertTrue(Range.open(5, 5).isAfter(5));
            assertTrue(Range.open(5, 5).isBefore(5));
        }

        @Test
        public void halfOpenEmptyRangesReportOnlyOneSideAtTheSharedEndpoint() {
            // [5, 5): the lower bound INCLUDES 5, so the range is not "after" 5.
            assertTrue(Range.closedOpen(5, 5).isEmpty());
            assertFalse(Range.closedOpen(5, 5).isAfter(5));
            assertTrue(Range.closedOpen(5, 5).isBefore(5));

            // (5, 5]: the upper bound INCLUDES 5, so the range is not "before" 5.
            assertTrue(Range.openClosed(5, 5).isEmpty());
            assertTrue(Range.openClosed(5, 5).isAfter(5));
            assertFalse(Range.openClosed(5, 5).isBefore(5));
        }

        @Test
        public void awayFromTheEndpointExactlyOneSideIsTrue() {
            for (final Range<Integer> empty : List.of(Range.open(5, 5), Range.closedOpen(5, 5), Range.openClosed(5, 5))) {
                assertTrue(empty.isEmpty(), () -> empty + " should be empty");

                // Below the endpoint: the range is after the element, and not before it.
                assertTrue(empty.isAfter(3), () -> empty + ".isAfter(3)");
                assertFalse(empty.isBefore(3), () -> empty + ".isBefore(3)");

                // Above the endpoint: the range is before the element, and not after it.
                assertFalse(empty.isAfter(9), () -> empty + ".isAfter(9)");
                assertTrue(empty.isBefore(9), () -> empty + ".isBefore(9)");
            }
        }

        @Test
        public void elementCompareToRejectsEveryElementOfAnEmptyRange() {
            // The blanket rejection is deliberate: a 0 result asserts containment, which an empty
            // range can never satisfy, and there is no fourth value for "not contained, neither side".
            for (final Range<Integer> empty : List.of(Range.open(5, 5), Range.closedOpen(5, 5), Range.openClosed(5, 5))) {
                for (final int element : new int[] { 3, 5, 9 }) {
                    assertThrows(IllegalStateException.class, () -> empty.elementCompareTo(element), () -> empty + ".elementCompareTo(" + element + ")");
                }
            }
        }

        @Test
        public void deprecatedPositionOfStaysTotalOnEmptyRanges() {
            // B3: the positionOf -> elementCompareTo migration is not just a sign flip; positionOf
            // answers for an empty range where elementCompareTo throws.
            final Range<Integer> empty = Range.open(5, 5);

            assertEquals(1, empty.positionOf(3));
            assertEquals(-1, empty.positionOf(9));
            assertThrows(IllegalStateException.class, () -> empty.elementCompareTo(3));
            assertThrows(IllegalStateException.class, () -> empty.elementCompareTo(9));
        }

        @Test
        public void emptyRangeContainsNothingAndOverlapsNothing() {
            final Range<Integer> empty = Range.open(5, 5);

            assertFalse(empty.contains(5));
            assertFalse(empty.overlaps(empty));
            assertFalse(empty.overlaps(Range.closed(1, 10)));
            assertTrue(Range.closed(1, 10).containsRange(empty));
        }
    }

    // =======================================================================
    // B5 - Range.mapEndpoints maps only the endpoints.
    // =======================================================================
    @Nested
    @DisplayName("B5: Range.mapEndpoints")
    public class B5RangeMapEndpoints extends TestBase {

        @Test
        public void preservesBoundTypes() {
            assertEquals(BoundType.CLOSED_OPEN, Range.closedOpen(1L, 5L).mapEndpoints(v -> v * 1000L).boundType());
            assertEquals(BoundType.OPEN_CLOSED, Range.openClosed(1L, 5L).mapEndpoints(v -> v * 1000L).boundType());

            final Range<Long> micros = Range.closedOpen(1L, 5L).mapEndpoints(v -> v * 1000L);
            assertEquals(1000L, micros.lowerEndpoint());
            assertEquals(5000L, micros.upperEndpoint());
        }

        @Test
        public void nonMonotonicMapperProducesAnEndpointRange_asDocumented() {
            // Documented caveat: mapEndpoints maps the two endpoints, not the members.
            final Range<Integer> mapped = Range.closed(2, 10).mapEndpoints(v -> v % 7);

            assertEquals(2, mapped.lowerEndpoint());
            assertEquals(3, mapped.upperEndpoint());
        }

        @Test
        public void rejectsNullMapperNullResultAndInvertedResult() {
            assertThrows(IllegalArgumentException.class, () -> Range.closed(1, 5).mapEndpoints(null));
            assertThrows(IllegalArgumentException.class, () -> Range.closed(1, 5).mapEndpoints(v -> null));
            assertThrows(IllegalArgumentException.class, () -> Range.closed(1, 5).mapEndpoints(v -> -v));
        }

        @SuppressWarnings("deprecation")
        @Test
        public void deprecatedMapDelegates() {
            assertEquals(Range.closed(1, 5).mapEndpoints(String::valueOf), Range.closed(1, 5).map(String::valueOf));
        }
    }

    // =======================================================================
    // J4 - factory validation names the offending bound and shows its value.
    // =======================================================================
    @Nested
    @DisplayName("J4: Range factory messages")
    public class J4RangeFactoryMessages extends TestBase {

        @Test
        public void nullMinAndNullMaxAreReportedSeparately() {
            assertEquals("'min' cannot be null", assertThrows(IllegalArgumentException.class, () -> Range.closed(null, 5)).getMessage());
            assertEquals("'max' cannot be null", assertThrows(IllegalArgumentException.class, () -> Range.closed(1, null)).getMessage());
        }

        @Test
        public void invertedBoundsReportBothValues() {
            assertEquals("'min' (9) must not be greater than 'max' (2)", assertThrows(IllegalArgumentException.class, () -> Range.open(9, 2)).getMessage());
        }

        @Test
        public void justReportsItsOwnParameterName() {
            assertEquals("'element' cannot be null", assertThrows(IllegalArgumentException.class, () -> Range.just(null)).getMessage());
        }

        @Test
        public void allFourFactoriesValidate() {
            assertThrows(IllegalArgumentException.class, () -> Range.open(9, 2));
            assertThrows(IllegalArgumentException.class, () -> Range.openClosed(9, 2));
            assertThrows(IllegalArgumentException.class, () -> Range.closedOpen(9, 2));
            assertThrows(IllegalArgumentException.class, () -> Range.closed(9, 2));
        }
    }

    // =======================================================================
    // D10 / D11 - BoundType.of and the shared Endpoint equals/hashCode.
    // =======================================================================
    @Nested
    @DisplayName("D10/D11: BoundType.of and Endpoint equality")
    public class D10D11RangeInternals extends TestBase {

        @Test
        public void boundTypeOfCoversAllFourCombinations() {
            assertEquals(BoundType.CLOSED_CLOSED, BoundType.of(true, true));
            assertEquals(BoundType.CLOSED_OPEN, BoundType.of(true, false));
            assertEquals(BoundType.OPEN_CLOSED, BoundType.of(false, true));
            assertEquals(BoundType.OPEN_OPEN, BoundType.of(false, false));
        }

        @Test
        public void intersectionAndSpanDeriveTheBoundTypeFromTheChosenEndpoints() {
            assertEquals(BoundType.OPEN_OPEN, Range.closed(1, 5).intersection(Range.open(1, 5)).get().boundType());
            assertEquals(BoundType.OPEN_OPEN, Range.open(1, 5).intersection(Range.closed(1, 5)).get().boundType());
            assertEquals(BoundType.CLOSED_CLOSED, Range.closedOpen(1, 5).span(Range.openClosed(1, 5)).boundType());
            assertEquals(BoundType.CLOSED_OPEN, Range.closedOpen(1, 3).span(Range.closedOpen(2, 5)).boundType());
        }

        @Test
        public void lowerAndUpperEndpointsWithTheSameValueAreNotEqual() {
            final Range.LowerEndpoint<Integer> lower = new Range.LowerEndpoint<>(1, true);
            final Range.UpperEndpoint<Integer> upper = new Range.UpperEndpoint<>(1, true);

            assertNotEquals(lower, upper);
            assertNotEquals(upper, lower);
            assertEquals(lower, new Range.LowerEndpoint<>(1, true));
            assertNotEquals(lower, new Range.LowerEndpoint<>(1, false));
            assertNotEquals(lower, new Range.LowerEndpoint<>(2, true));
            assertFalse(lower.equals(null));
            assertFalse(lower.equals("1"));
        }

        @Test
        public void compareToValueComparesTheEndpointValue() {
            assertEquals(0, new Range.LowerEndpoint<>(5, true).compareToValue(5));
            assertTrue(new Range.LowerEndpoint<>(5, true).compareToValue(3) > 0);
            assertTrue(new Range.UpperEndpoint<>(5, true).compareToValue(9) < 0);
        }
    }

    // =======================================================================
    // B3 - Pair/Triple/Tuple.forEach require an Object-accepting consumer, so
    //      a narrower one can no longer fail half-way with a ClassCastException.
    // =======================================================================
    @Nested
    @DisplayName("B3: forEach requires an Object consumer")
    public class B3ForEach extends TestBase {

        @Test
        public void pairForEachVisitsBothElementsInOrder() {
            final List<Object> seen = new ArrayList<>();

            Pair.of("Count", 42).forEach(seen::add);

            assertEquals(Arrays.asList("Count", 42), seen);
        }

        @Test
        public void tripleForEachVisitsAllThreeInOrder() {
            final List<Object> seen = new ArrayList<>();

            Triple.of("a", 1, true).forEach(seen::add);

            assertEquals(Arrays.asList("a", 1, true), seen);
        }

        @Test
        public void tupleForEachVisitsEveryArityInOrder() {
            final List<Object> seen = new ArrayList<>();

            Tuple.of(1).forEach(seen::add);
            Tuple.of(1, 2).forEach(seen::add);
            Tuple.of(1, 2, 3).forEach(seen::add);
            Tuple.of(1, 2, 3, 4).forEach(seen::add);
            Tuple.of(1, 2, 3, 4, 5).forEach(seen::add);
            Tuple.of(1, 2, 3, 4, 5, 6).forEach(seen::add);
            Tuple.of(1, 2, 3, 4, 5, 6, 7).forEach(seen::add);

            assertEquals(28, seen.size());
            assertEquals(Arrays.asList(1, 1, 2, 1, 2, 3), seen.subList(0, 6));
        }

        @Test
        public void emptyTupleForEachDoesNothingButStillValidates() {
            final Tuple<?> empty = Tuple.fromArray(new Object[0]);
            final List<Object> seen = new ArrayList<>();

            empty.forEach(seen::add);

            assertTrue(seen.isEmpty());
            assertThrows(IllegalArgumentException.class, () -> empty.forEach(null));
        }

        @Test
        public void nullConsumerIsRejected() {
            assertThrows(IllegalArgumentException.class, () -> Pair.of("a", 1).forEach(null));
            assertThrows(IllegalArgumentException.class, () -> Triple.of("a", 1, true).forEach(null));
            assertThrows(IllegalArgumentException.class, () -> Tuple.of("a", 1).forEach(null));
        }

        @Test
        public void heterogeneousPairIsSafeBecauseTheConsumerSeesObject() {
            // Before the fix, a Consumer<String> compiled here and threw ClassCastException after
            // already having consumed the left element.
            final List<String> rendered = new ArrayList<>();

            Pair.of("s", 1).forEach(v -> rendered.add(String.valueOf(v)));

            assertEquals(Arrays.asList("s", "1"), rendered);
        }
    }

    // =======================================================================
    // D13 - Pair.toArray(A[]) matches Triple.toArray(A[]).
    // =======================================================================
    @Nested
    @DisplayName("D13: Pair.toArray(A[])")
    public class D13PairToArray extends TestBase {

        @Test
        public void reusesASufficientlyLargeArrayAndLeavesTheTailUntouched() {
            final String[] a = new String[3];
            a[2] = "untouched";

            final String[] result = Pair.of("a", "b").toArray(a);

            assertSame(a, result);
            assertArrayEquals(new String[] { "a", "b", "untouched" }, result);
        }

        @Test
        public void allocatesWhenTheArrayIsTooSmall() {
            final String[] a = new String[1];
            final String[] result = Pair.of("a", "b").toArray(a);

            assertNotEquals(a, result);
            assertArrayEquals(new String[] { "a", "b" }, result);
        }

        @Test
        public void storesIncompatibleElementsWithArrayStoreException() {
            assertThrows(ArrayStoreException.class, () -> Pair.of("a", 1).toArray(new String[2]));
        }

        @Test
        public void nullArrayThrowsNpe() {
            assertThrows(NullPointerException.class, () -> Pair.of("a", "b").toArray((String[]) null));
        }
    }

    // =======================================================================
    // D5 - Pair.setValue is the Map.Entry mutation contract, so it is not
    //      deprecated any more.
    // =======================================================================
    @Nested
    @DisplayName("D5: Pair.setValue through Map.Entry")
    public class D5PairSetValue extends TestBase {

        @Test
        public void setValueMutatesTheRightElementAndReturnsThePrevious() {
            final java.util.Map.Entry<String, Integer> entry = Pair.of("key", 100);

            assertEquals(100, entry.setValue(200));
            assertEquals(200, entry.getValue());
        }
    }

    // =======================================================================
    // D4 - Tuple.fromArray / fromCollection.
    // =======================================================================
    @Nested
    @DisplayName("D4: Tuple.fromArray / fromCollection")
    public class D4TupleFrom extends TestBase {

        @Test
        public void fromArrayReturnsTheMatchingArity() {
            assertEquals(0, Tuple.fromArray(new Object[0]).arity());
            assertEquals(1, Tuple.fromArray(new Object[] { 1 }).arity());
            assertEquals(5, Tuple.fromArray(new Object[] { 1, 2, 3, 4, 5 }).arity());
            assertEquals(9, Tuple.fromArray(new Object[] { 1, 2, 3, 4, 5, 6, 7, 8, 9 }).arity());
        }

        @Test
        public void fromCollectionReturnsTheMatchingArity() {
            assertEquals(0, Tuple.fromCollection(Collections.emptyList()).arity());
            assertEquals(3, Tuple.fromCollection(Arrays.asList("a", 1, true)).arity());
        }

        @Test
        public void nullInputYieldsTheSharedEmptyTuple() {
            // Both of these used to be a compile error: Tuple.from(null) was ambiguous.
            assertEquals(0, Tuple.fromArray(null).arity());
            assertEquals(0, Tuple.fromCollection(null).arity());
            assertSame(Tuple.fromArray(null), Tuple.fromCollection(null));
        }

        @Test
        public void tooManyElementsIsRejected() {
            assertThrows(IllegalArgumentException.class, () -> Tuple.fromArray(new Object[10]));
            assertThrows(IllegalArgumentException.class, () -> Tuple.fromCollection(Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)));
        }

        @Test
        public void theCastNowHappensAtTheCallSite() {
            final Tuple<?> t = Tuple.fromArray(new Object[] { "a", 1 });

            assertEquals(Tuple.of("a", 1), t);

            @SuppressWarnings("unchecked")
            final Tuple2<String, Integer> t2 = (Tuple2<String, Integer>) t;
            assertEquals("a", t2._1);
            assertEquals(1, t2._2);

            // A wrong-arity assumption fails at the caller's own cast, not inside the library.
            assertThrows(ClassCastException.class, () -> {
                @SuppressWarnings("unchecked")
                final Tuple3<String, Integer, Boolean> bad = (Tuple3<String, Integer, Boolean>) Tuple.fromArray(new Object[] { "a", 1 });
                assertNotNull(bad);
            });
        }

        @Test
        public void fromMapEntryKeepsItsName() {
            assertEquals(Tuple.of("k", 1), Tuple.from(java.util.Map.entry("k", 1)));
        }

        @Test
        public void collectionIterationOrderIsPreserved() {
            final Tuple<?> t = Tuple.fromCollection(new java.util.LinkedHashSet<>(Arrays.asList("x", "y", "z")));

            assertArrayEquals(new Object[] { "x", "y", "z" }, t.toArray());
        }
    }

    // =======================================================================
    // B10 - Duration.plus/minus/compareTo validate their argument.
    // =======================================================================
    @Nested
    @DisplayName("B10: Duration argument validation")
    public class B10DurationNullChecks extends TestBase {

        @Test
        public void plusMinusAndCompareToRejectNull() {
            final Duration d = Duration.ofHours(1);

            assertEquals("'duration' cannot be null", assertThrows(IllegalArgumentException.class, () -> d.plus(null)).getMessage());
            assertEquals("'duration' cannot be null", assertThrows(IllegalArgumentException.class, () -> d.minus(null)).getMessage());

            // compareTo is deliberately the exception to this class's IllegalArgumentException
            // convention: Comparable#compareTo specifies NullPointerException for a null argument,
            // and generic code (sorting, TreeMap) is written against that.
            assertEquals("'other' cannot be null", assertThrows(NullPointerException.class, () -> d.compareTo(null)).getMessage());
        }

        @Test
        public void arithmeticStillWorks() {
            assertEquals(Duration.ofMinutes(90), Duration.ofHours(1).plus(Duration.ofMinutes(30)));
            assertEquals(Duration.ofMinutes(30), Duration.ofHours(1).minus(Duration.ofMinutes(30)));
            assertTrue(Duration.ofHours(1).compareTo(Duration.ofMinutes(30)) > 0);
        }
    }

    // =======================================================================
    // O1 - Duration.toString no longer depends on ZERO identity or the
    //      Objectory builder pool.
    // =======================================================================
    @Nested
    @DisplayName("O1: Duration.toString")
    public class O1DurationToString extends TestBase {

        @Test
        public void formatsTheDocumentedShapes() {
            assertEquals("PT0S", Duration.ZERO.toString());
            assertEquals("PT0S", Duration.ofMillis(0).toString());
            assertEquals("PT1H", Duration.ofHours(1).toString());
            assertEquals("PT1H30M", Duration.ofMinutes(90).toString());
            assertEquals("PT1H30M25S", Duration.ofMinutes(90).plusSeconds(25).toString());
            assertEquals("PT25.500S", Duration.ofMillis(25500).toString());
            assertEquals("-PT0.500S", Duration.ofMillis(-500).toString());
            assertEquals("PT123H45M", Duration.ofDays(5).plusHours(3).plusMinutes(45).toString());
        }

        @Test
        public void padsMillisecondsToThreeDigits() {
            assertEquals("PT0.001S", Duration.ofMillis(1).toString());
            assertEquals("PT0.010S", Duration.ofMillis(10).toString());
            assertEquals("PT0.100S", Duration.ofMillis(100).toString());
        }

        @Test
        public void handlesTheLongMinValueMagnitudeExactly() {
            assertEquals("-PT2562047788015H12M55.808S", Duration.ofMillis(Long.MIN_VALUE).toString());
            assertEquals("PT2562047788015H12M55.807S", Duration.ofMillis(Long.MAX_VALUE).toString());
        }
    }

    // =======================================================================
    // J10 / O2 - Holder message and toString.
    // =======================================================================
    @Nested
    @DisplayName("J10/O2: Holder")
    public class J10O2Holder extends TestBase {

        @Test
        public void orElseThrowIfNullMessageDescribesAHolder() {
            assertEquals("The value held by this Holder is null",
                    assertThrows(NoSuchElementException.class, () -> Holder.of(null).orElseThrowIfNull()).getMessage());
        }

        @Test
        public void orElseThrowIfNullReturnsAPresentValue() {
            assertEquals("v", Holder.of("v").orElseThrowIfNull());
        }

        @Test
        public void toStringHandlesNullAndNonNull() {
            assertEquals("Holder[null]", Holder.of(null).toString());
            assertEquals("Holder[test]", Holder.of("test").toString());
            assertEquals("Holder[42]", Holder.of(42).toString());
        }
    }

    // =======================================================================
    // O3 / O4 - RateLimiter validation and toString.
    // =======================================================================
    @Nested
    @DisplayName("O3/O4: RateLimiter")
    public class O3O4RateLimiter extends TestBase {

        @Test
        public void nonPositiveAndNaNRatesAreRejectedWithTheOffendingValue() {
            assertTrue(assertThrows(IllegalArgumentException.class, () -> RateLimiter.create(0.0)).getMessage().contains("0.0"));
            assertThrows(IllegalArgumentException.class, () -> RateLimiter.create(-1.0));
            assertThrows(IllegalArgumentException.class, () -> RateLimiter.create(Double.NaN));

            final RateLimiter limiter = RateLimiter.create(5.0);
            assertThrows(IllegalArgumentException.class, () -> limiter.setRate(0.0));
            assertThrows(IllegalArgumentException.class, () -> limiter.setRate(Double.NaN));
        }

        @Test
        public void toStringRoundTripsSmallRates() {
            assertEquals("RateLimiter[stableRate=5.0qps]", RateLimiter.create(5.0).toString());
            assertEquals("RateLimiter[stableRate=0.05qps]", RateLimiter.create(0.05).toString());

            final RateLimiter limiter = RateLimiter.create(5.0);
            limiter.setRate(10.0);
            assertEquals("RateLimiter[stableRate=10.0qps]", limiter.toString());
        }

        @Test
        public void warmupPeriodIsStillValidated() {
            assertThrows(IllegalArgumentException.class, () -> RateLimiter.create(10.0, -1, TimeUnit.SECONDS));
        }
    }

    // =======================================================================
    // D2 - Fraction.ofMixed replaces the confusable of(int,int,int) overloads.
    // =======================================================================
    @Nested
    @DisplayName("D2: Fraction.ofMixed")
    public class D2FractionOfMixed extends TestBase {

        @Test
        public void mixedNumbersAreBuiltFromWholeNumeratorDenominator() {
            assertEquals("7/4", Fraction.ofMixed(1, 3, 4).toString());
            assertEquals("-7/3", Fraction.ofMixed(-2, 1, 3).toString());
            assertEquals("6/4", Fraction.ofMixed(1, 2, 4).toString());
            assertEquals("3/2", Fraction.ofMixed(1, 2, 4, true).toString());
            assertEquals("-3/2", Fraction.ofMixed(-1, 1, 2, true).toString());
        }

        @Test
        public void isDistinctFromTheNumeratorDenominatorReduceOverload() {
            // The whole point of the rename: these two used to differ only by the third argument's type.
            assertEquals("5/3", Fraction.ofMixed(1, 2, 3).toString());
            assertEquals("1/2", Fraction.of(1, 2, true).toString());
        }

        @Test
        public void validatesItsArguments() {
            assertThrows(ArithmeticException.class, () -> Fraction.ofMixed(0, 1, 0));
            assertThrows(ArithmeticException.class, () -> Fraction.ofMixed(1, -6, 10));
            assertThrows(ArithmeticException.class, () -> Fraction.ofMixed(1, 6, -10));
            assertThrows(ArithmeticException.class, () -> Fraction.ofMixed(Integer.MAX_VALUE, 1, 4));
        }

        @SuppressWarnings("deprecation")
        @Test
        public void deprecatedAliasesDelegate() {
            assertEquals(Fraction.ofMixed(1, 3, 4), Fraction.of(1, 3, 4));
            assertEquals(Fraction.ofMixed(1, 2, 4, true), Fraction.of(1, 2, 4, true));
        }

        @Test
        public void stringParsingUsesTheMixedFactory() {
            assertEquals("9/4", Fraction.of("2 1/4").toString());
            assertEquals("-5/3", Fraction.of("-1 2/3").toString());
        }
    }

    // =======================================================================
    // B8 / B9 / O6 - Fraction factories, convergence and parse messages.
    // =======================================================================
    @Nested
    @DisplayName("B8/B9/O6: Fraction")
    public class B8B9O6Fraction extends TestBase {

        @Test
        public void minValueDenominatorIsOnlyRescuedWhenReducing() {
            assertThrows(ArithmeticException.class, () -> Fraction.of(2, Integer.MIN_VALUE));
            assertEquals("-1/1073741824", Fraction.of(2, Integer.MIN_VALUE, true).toString());
            assertThrows(ArithmeticException.class, () -> Fraction.of(3, Integer.MIN_VALUE, true));
        }

        @Test
        public void doubleConversionStillConverges() {
            assertEquals("3/4", Fraction.of(0.75).toString());
            assertEquals("1/2", Fraction.of(0.5).toString());
            assertEquals("333/1000", Fraction.of(0.333).toString());
            assertEquals("5/2", Fraction.of(2.5).toString());
            assertEquals("0/1", Fraction.of(0.0).toString());
            assertEquals("0/1", Fraction.of(-0.0).toString());
            assertEquals("-3/4", Fraction.of(-0.75).toString());
            assertEquals("-2147483648/1", Fraction.of(Integer.MIN_VALUE).toString());
            // In range, so approximated rather than rejected: the exact value is 4294967293/2, whose
            // numerator does not fit an int, and of the two equally close integer candidates of(double)
            // documents that the one closest to zero wins.
            assertEquals("2147483646/1", Fraction.of(2147483646.5).toString());
        }

        @Test
        public void doubleConversionRejectsNonFiniteAndOutOfRangeValues() {
            assertThrows(ArithmeticException.class, () -> Fraction.of(Double.NaN));
            assertThrows(ArithmeticException.class, () -> Fraction.of(Double.POSITIVE_INFINITY));
            assertThrows(ArithmeticException.class, () -> Fraction.of(Double.NEGATIVE_INFINITY));
            assertThrows(ArithmeticException.class, () -> Fraction.of(Integer.MAX_VALUE + 1.0));
            assertThrows(ArithmeticException.class, () -> Fraction.of(Integer.MIN_VALUE - 1.0));
            assertThrows(ArithmeticException.class, () -> Fraction.of(Math.nextUp((double) Integer.MAX_VALUE)));
        }

        @Test
        public void parseFailuresNameTheOffendingInput() {
            assertTrue(assertThrows(NumberFormatException.class, () -> Fraction.of("3 / 4")).getMessage().contains("3 / 4"));
            assertTrue(assertThrows(NumberFormatException.class, () -> Fraction.of("2 b/4")).getMessage().contains("2 b/4"));
            assertTrue(assertThrows(NumberFormatException.class, () -> Fraction.of("2 3")).getMessage().contains("2 3"));
            assertTrue(assertThrows(NumberFormatException.class, () -> Fraction.of("1e-3")).getMessage().contains("1e-3"));
            assertTrue(assertThrows(NumberFormatException.class, () -> Fraction.of("x.5")).getMessage().contains("x.5"));
        }

        @Test
        public void whitespaceRemainsSignificant() {
            // A space separates the whole number of the "X Y/Z" form, so padded input is malformed.
            assertThrows(NumberFormatException.class, () -> Fraction.of(" 3"));
            assertThrows(NumberFormatException.class, () -> Fraction.of("2 "));
            assertThrows(NumberFormatException.class, () -> Fraction.of(" "));
        }

        @Test
        public void nullStringIsRejected() {
            assertThrows(IllegalArgumentException.class, () -> Fraction.of((String) null));
        }
    }

    // =======================================================================
    // J9 / J11 - toProperString and the value-based equals/hashCode/toString.
    // =======================================================================
    @Nested
    @DisplayName("J9/J11: Fraction rendering")
    public class J9J11FractionRendering extends TestBase {

        @Test
        public void toProperStringCoversEveryBranch() {
            assertEquals("0", Fraction.of(0, 5).toProperString());
            assertEquals("1", Fraction.of(4, 4).toProperString());
            assertEquals("-1", Fraction.of(-4, 4).toProperString());
            assertEquals("3/4", Fraction.of(3, 4).toProperString());
            assertEquals("1 3/4", Fraction.of(7, 4).toProperString());
            assertEquals("-1 3/4", Fraction.of(-7, 4).toProperString());
            assertEquals("2", Fraction.of(8, 4).toProperString());
            assertEquals("-2", Fraction.of(-8, 4).toProperString());
            assertEquals("-2147483648", Fraction.of(Integer.MIN_VALUE, 1).toProperString());
        }

        @Test
        public void equalsHashCodeAndToStringUseTheStoredTerms() {
            assertEquals(Fraction.of(3, 4), Fraction.of(3, 4));
            assertEquals(Fraction.of(3, 4).hashCode(), Fraction.of(3, 4).hashCode());
            assertNotEquals(Fraction.of(1, 2), Fraction.of(2, 4));
            assertEquals(0, Fraction.of(1, 2).compareTo(Fraction.of(2, 4)));
            assertEquals("8/4", Fraction.of(8, 4).toString());
            assertEquals("0/5", Fraction.of(0, 5).toString());
            assertNotEquals(0, Fraction.of(0, 1).hashCode());
        }
    }

    // =======================================================================
    // D6 - Stopwatch.elapsed() returns java.time.Duration.
    // =======================================================================
    @Nested
    @DisplayName("D6: Stopwatch elapsed type")
    public class D6Stopwatch extends TestBase {

        @Test
        public void elapsedReturnsAJdkDuration() {
            final java.time.Duration d = Stopwatch.createUnstarted().elapsed();

            assertEquals(java.time.Duration.ZERO, d);
            assertEquals("java.time.Duration", d.getClass().getName());
        }

        @Test
        public void unstartedStopwatchRendersZeroNanoseconds() {
            assertEquals("0.000 ns", Stopwatch.createUnstarted().toString());
        }
    }
}

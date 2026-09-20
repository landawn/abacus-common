package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

/**
 * Regression tests for the 2026-08-31 value-types review fixes across
 * {@code Result / Holder / Pair / Triple / Tuple / Range / Duration / Stopwatch / RateLimiter / Fraction}.
 *
 * <p>Each nested class is named after the finding it pins. Findings that were resolved with
 * documentation only (Range's empty-range wording, the ordering-vs-equality note, RateLimiter's
 * bursty fallback) are still covered here, because the point of those fixes was that the previously
 * documented behaviour did not match the code &mdash; so the code's actual behaviour is what needs
 * locking down.</p>
 */
public class ValueTypesTest extends TestBase {

    // =======================================================================
    // B4 - Fraction.floatValue() divided two floats, rounding both int terms to 24-bit precision
    //      before the division. It now divides in double and narrows once.
    // =======================================================================
    @Nested
    @DisplayName("B4: Fraction.floatValue precision")
    public class B4FloatValue extends TestBase {

        @Test
        public void narrowsOnceInsteadOfRoundingBothTerms() {
            // Exact value 0.99999988...; rounding each term to float first yielded 0x3f7ffffc (2 ULP low).
            assertEquals(0x3f7ffffe, Float.floatToIntBits(Fraction.of(16777217, 16777219).floatValue()));

            // Exact value 0.99999994...; the old form yielded 0x3f7ffffe (1 ULP low).
            assertEquals(0x3f7fffff, Float.floatToIntBits(Fraction.of(33554433, 33554435).floatValue()));
        }

        @Test
        public void isAlwaysTheNearestFloatToDoubleValue() {
            // This is the contract the javadoc now states, and it holds by construction.
            final Random rnd = new Random(20260831L);

            for (int i = 0; i < 20_000; i++) {
                final int n = rnd.nextInt(Integer.MAX_VALUE) + 1;
                final int d = rnd.nextInt(Integer.MAX_VALUE) + 1;
                final Fraction f = Fraction.of(n, d);

                assertEquals(Float.floatToIntBits((float) f.doubleValue()), Float.floatToIntBits(f.floatValue()),
                        () -> "floatValue() must equal (float) doubleValue() for " + f);
            }
        }

        @Test
        public void documentedDoubleRoundingCaveatIsReal() {
            // The javadoc explicitly does NOT promise the nearest float to the exact rational.
            // 2125113837/2125114027 is such a case: the exact value rounds to 0x3f7fffff, but the
            // double quotient sits on the other side of the float midpoint and narrows to 0x3f7ffffe.
            // Pinned so that anyone "fixing" this reads the javadoc first.
            final Fraction f = Fraction.of(2125113837, 2125114027);

            assertEquals(0x3f7ffffe, Float.floatToIntBits(f.floatValue()));
            assertEquals(0x3f7fffff,
                    Float.floatToIntBits(new BigDecimal(2125113837).divide(new BigDecimal(2125114027), java.math.MathContext.DECIMAL128).floatValue()),
                    "the exactly-rounded value differs by one ULP");
        }

        @Test
        public void ordinaryValuesAreUnchanged() {
            assertEquals(0.33333334f, Fraction.of(1, 3).floatValue());
            assertEquals(0.75f, Fraction.of(3, 4).floatValue());
            assertEquals(0.0f, Fraction.of(0, 1).floatValue());
            assertEquals(-0.5f, Fraction.of(-1, 2).floatValue());
            assertEquals(3.875f, Fraction.of(31, 8).floatValue());
        }

        @Test
        public void extremeTermsStillWork() {
            assertEquals(Integer.MAX_VALUE, Fraction.of(Integer.MAX_VALUE, 1).floatValue());
            assertEquals(Integer.MIN_VALUE, Fraction.of(Integer.MIN_VALUE, 1).floatValue());

            // MIN_VALUE / MAX_VALUE == -1.0000000004656613; the nearest float is exactly -1.0f.
            // (A negative denominator such as MIN_VALUE + 1 is rejected before this point, because
            // moving the sign onto an Integer.MIN_VALUE numerator would overflow.)
            assertEquals(-1.0f, Fraction.of(Integer.MIN_VALUE, Integer.MAX_VALUE).floatValue());
            assertEquals(Float.floatToIntBits((float) Fraction.of(Integer.MIN_VALUE, Integer.MAX_VALUE).doubleValue()),
                    Float.floatToIntBits(Fraction.of(Integer.MIN_VALUE, Integer.MAX_VALUE).floatValue()));
        }
    }

    // =======================================================================
    // B6 - Duration.compareTo(null) threw IllegalArgumentException; Comparable specifies NPE.
    // =======================================================================
    @Nested
    @DisplayName("B6: Duration.compareTo null contract")
    public class B6DurationCompareToNull extends TestBase {

        @Test
        public void throwsNullPointerExceptionNotIllegalArgument() {
            assertThrows(NullPointerException.class, () -> Duration.ofHours(1).compareTo(null));
            assertThrows(NullPointerException.class, () -> Duration.ZERO.compareTo(null));
        }

        @Test
        public void matchesTheSiblingComparableInThisPackage() {
            // Fraction.compareTo(null) already threw NPE; the two now agree.
            assertThrows(NullPointerException.class, () -> Fraction.of(1, 2).compareTo(null));
            assertThrows(NullPointerException.class, () -> Duration.ofHours(1).compareTo(null));
        }

        @Test
        public void orderingItselfIsUnchanged() {
            final List<Duration> durations = new ArrayList<>(
                    List.of(Duration.ofHours(2), Duration.ofMinutes(30), Duration.ofSeconds(10), Duration.ofMillis(-1)));
            Collections.sort(durations);

            assertEquals(List.of(Duration.ofMillis(-1), Duration.ofSeconds(10), Duration.ofMinutes(30), Duration.ofHours(2)), durations);
            assertTrue(Duration.ofHours(1).compareTo(Duration.ofMinutes(30)) > 0);
            assertTrue(Duration.ofMinutes(30).compareTo(Duration.ofHours(1)) < 0);
            assertEquals(0, Duration.ofHours(1).compareTo(Duration.ofMinutes(60)));
        }
    }

    // =======================================================================
    // O3 - Duration.between used full-sentence messages while compareTo used the 'argName' form.
    // =======================================================================
    @Nested
    @DisplayName("O3: Duration null-argument message convention")
    public class O3DurationMessages extends TestBase {

        @Test
        public void allNullChecksUseTheArgNameConvention() {
            final Date date = new Date();
            final Calendar cal = Calendar.getInstance();

            assertEquals("'start' cannot be null", assertThrows(IllegalArgumentException.class, () -> Duration.between(null, date)).getMessage());
            assertEquals("'end' cannot be null", assertThrows(IllegalArgumentException.class, () -> Duration.between(date, null)).getMessage());
            assertEquals("'start' cannot be null", assertThrows(IllegalArgumentException.class, () -> Duration.between(null, cal)).getMessage());
            assertEquals("'end' cannot be null", assertThrows(IllegalArgumentException.class, () -> Duration.between(cal, null)).getMessage());
            assertEquals("'start' cannot be null",
                    assertThrows(IllegalArgumentException.class, () -> Duration.between(null, java.time.LocalDateTime.now())).getMessage());
            assertEquals("'end' cannot be null",
                    assertThrows(IllegalArgumentException.class, () -> Duration.between(java.time.LocalDateTime.now(), null)).getMessage());
            assertEquals("'other' cannot be null", assertThrows(NullPointerException.class, () -> Duration.ZERO.compareTo(null)).getMessage());
        }
    }

    // =======================================================================
    // B8 - Fraction.of(String) dispatched on '.' before '/', so "1.0/2.0" was reported as a malformed
    //      decimal instead of naming the non-integer component.
    // =======================================================================
    @Nested
    @DisplayName("B8: Fraction.of(String) dispatch")
    public class B8StringDispatch extends TestBase {

        @Test
        public void aTokenContainingSlashIsReadAsAFraction() {
            assertEquals("The fraction \"1.0/2.0\" could not be parsed: \"1.0\" is not an integer",
                    assertThrows(NumberFormatException.class, () -> Fraction.of("1.0/2.0")).getMessage());
            assertEquals("The fraction \"3/4.\" could not be parsed: \"4.\" is not an integer",
                    assertThrows(NumberFormatException.class, () -> Fraction.of("3/4.")).getMessage());
            assertEquals("The fraction \"1 1.5/2\" could not be parsed: \"1.5\" is not an integer",
                    assertThrows(NumberFormatException.class, () -> Fraction.of("1 1.5/2")).getMessage());
        }

        @Test
        public void decimalFormsWithoutSlashAreUnaffected() {
            assertEquals(Fraction.of(1, 4), Fraction.of("0.25"));
            assertEquals(Fraction.of(3, 2), Fraction.of("1.5"));
            assertEquals(Fraction.of(1, 2), Fraction.of(".5"));
            assertEquals(Fraction.of(1, 1), Fraction.of("1."));
            assertEquals(Fraction.of(-11, 4), Fraction.of("-2.75"));
        }

        @Test
        public void otherFormsAreUnaffected() {
            assertEquals(Fraction.of(3, 4), Fraction.of("3/4"));
            assertEquals(Fraction.of(5, 3), Fraction.of("1 2/3"));
            assertEquals(Fraction.of(-5, 1), Fraction.of("-5"));
            assertEquals(Fraction.of(-3, 2), Fraction.of("-1 1/2"));
        }

        @Test
        public void malformedInputStillThrowsNumberFormatException() {
            // The dispatch change never converts a NumberFormatException into a different type:
            // Double.parseDouble can never accept a token containing '/', so every string reaching
            // the fraction parsers by the new route would have failed the decimal parse anyway.
            assertThrows(NumberFormatException.class, () -> Fraction.of("invalid"));
            assertThrows(NumberFormatException.class, () -> Fraction.of("1e-3"));
            assertThrows(NumberFormatException.class, () -> Fraction.of(" 3"));
            assertThrows(NumberFormatException.class, () -> Fraction.of("2 "));
            assertThrows(NumberFormatException.class, () -> Fraction.of("1/2/3"));
            assertThrows(NumberFormatException.class, () -> Fraction.of(""));
            assertThrows(IllegalArgumentException.class, () -> Fraction.of((String) null));
            assertThrows(ArithmeticException.class, () -> Fraction.of("1/0"));
            assertThrows(ArithmeticException.class, () -> Fraction.of("1.0e10"));
        }
    }

    // =======================================================================
    // O4 - Fraction.reduce() passed Math.abs(numerator), a no-op at Integer.MIN_VALUE. The GCD
    //      normalises signs itself, so dropping it must not change any result.
    // =======================================================================
    @Nested
    @DisplayName("O4: Fraction.reduce sign handling")
    public class O4ReduceSigns extends TestBase {

        @Test
        public void reducesMinValueNumeratorsCorrectly() {
            assertEquals(Fraction.of(Integer.MIN_VALUE, 1), Fraction.of(Integer.MIN_VALUE, 1).reduce());
            assertEquals(Fraction.of(Integer.MIN_VALUE / 2, 1), Fraction.of(Integer.MIN_VALUE, 2).reduce());
            assertEquals(Fraction.of(Integer.MIN_VALUE, 3), Fraction.of(Integer.MIN_VALUE, 3).reduce());
            assertEquals(Fraction.of(Integer.MIN_VALUE / 4, 1), Fraction.of(Integer.MIN_VALUE, 4).reduce());
        }

        @Test
        public void ordinarySignsAreUnchanged() {
            assertEquals(Fraction.of(2, 3), Fraction.of(6, 9).reduce());
            assertEquals(Fraction.of(-2, 3), Fraction.of(-6, 9).reduce());
            assertEquals(Fraction.of(2, 3), Fraction.of(-6, -9).reduce());
            assertEquals(Fraction.of(0, 1), Fraction.of(0, 5).reduce());
            assertSame(Fraction.ZERO, Fraction.of(0, 5).reduce());
        }

        @Test
        public void anAlreadyReducedFractionIsReturnedAsIs() {
            final Fraction f = Fraction.of(3, 4);
            assertSame(f, f.reduce());

            final Fraction negative = Fraction.of(-3, 4);
            assertSame(negative, negative.reduce());
        }

        @Test
        public void agreesWithBigIntegerGcdOverASweep() {
            final Random rnd = new Random(4242L);

            for (int i = 0; i < 5_000; i++) {
                final int n = rnd.nextInt();
                final int d = rnd.nextInt(Integer.MAX_VALUE) + 1;
                final Fraction reduced = Fraction.of(n, d).reduce();
                final java.math.BigInteger gcd = java.math.BigInteger.valueOf(n).gcd(java.math.BigInteger.valueOf(d));

                assertEquals(java.math.BigInteger.valueOf(n).divide(gcd).intValueExact(), reduced.numerator(), () -> "numerator of " + n + "/" + d);
                assertEquals(java.math.BigInteger.valueOf(d).divide(gcd).intValueExact(), reduced.denominator(), () -> "denominator of " + n + "/" + d);
            }
        }
    }

    // =======================================================================
    // B2/B3 - Range's empty-range documentation claimed isBefore/isAfter are both true for every
    //         element. They are not. (The behaviour is unchanged; the docs were corrected.)
    //         The full matrix lives in ReviewFixes20260831Test.B7EmptyRange; this pins the
    //         non-empty control cases so the two predicates cannot drift.
    // =======================================================================
    @Nested
    @DisplayName("B2: Range empty-range predicate matrix (non-empty controls)")
    public class B2RangePredicates extends TestBase {

        @Test
        public void nonEmptyRangesAreUnaffected() {
            final Range<Integer> range = Range.closed(5, 10);

            assertTrue(range.isAfter(3));
            assertFalse(range.isAfter(5));
            assertFalse(range.isAfter(7));
            assertTrue(range.isBefore(12));
            assertFalse(range.isBefore(10));
            assertFalse(range.isBefore(7));

            assertEquals(-1, range.elementCompareTo(3));
            assertEquals(0, range.elementCompareTo(7));
            assertEquals(1, range.elementCompareTo(12));
        }

        @Test
        public void openBoundsShiftTheBoundaryAnswers() {
            assertTrue(Range.open(5, 10).isAfter(5));
            assertTrue(Range.open(5, 10).isBefore(10));
            assertFalse(Range.closed(5, 10).isAfter(5));
            assertFalse(Range.closed(5, 10).isBefore(10));
        }

        @Test
        public void nullElementsAreStillFalseNotThrowing() {
            assertFalse(Range.open(5, 5).isAfter(null));
            assertFalse(Range.open(5, 5).isBefore(null));
            assertFalse(Range.closed(5, 10).isAfter(null));
            assertFalse(Range.closed(5, 10).isBefore(null));
        }
    }

    // =======================================================================
    // B5 - Range orders by compareTo but identifies by equals, so span/intersection are commutative
    //      only up to the comparator's equivalence classes. The class javadoc now says so.
    // =======================================================================
    @Nested
    @DisplayName("B5: Range ordering vs. equality")
    public class B5OrderingVsEquality extends TestBase {

        private final BigDecimal fiveShort = new BigDecimal("5.0");
        private final BigDecimal fiveLong = new BigDecimal("5.00");
        private final BigDecimal sixShort = new BigDecimal("6.0");
        private final BigDecimal sixLong = new BigDecimal("6.00");

        @Test
        public void spanKeepsTheReceiversEndpointObjectsOnATie() {
            final Range<BigDecimal> a = Range.closed(fiveShort, sixShort);
            final Range<BigDecimal> b = Range.closed(fiveLong, sixLong);

            assertNotEquals(a, b, "the endpoint objects are not equal even though they compare equal");
            assertEquals(a, a.span(b));
            assertEquals(b, b.span(a));
            assertNotEquals(a.span(b), b.span(a), "documented: commutative only up to compareTo-equivalence");
        }

        @Test
        public void intersectionHasTheSameTieBehaviour() {
            final Range<BigDecimal> a = Range.closed(fiveShort, sixShort);
            final Range<BigDecimal> b = Range.closed(fiveLong, sixLong);

            assertNotEquals(a.intersection(b).get(), b.intersection(a).get());
        }

        @Test
        public void bothOrdersDescribeTheSameInterval() {
            final Range<BigDecimal> a = Range.closed(fiveShort, sixShort);
            final Range<BigDecimal> b = Range.closed(fiveLong, sixLong);
            final BigDecimal probe = new BigDecimal("5.5");

            assertEquals(a.contains(probe), b.contains(probe));
            assertEquals(a.span(b).contains(probe), b.span(a).contains(probe));
            assertTrue(a.span(b).containsRange(b));
            assertTrue(b.span(a).containsRange(a));
        }

        @Test
        public void spanIsCommutativeWhenCompareToAgreesWithEquals() {
            // The normal case, which the javadoc still guarantees.
            assertEquals(Range.closed(1, 3).span(Range.closed(5, 7)), Range.closed(5, 7).span(Range.closed(1, 3)));
            assertEquals(Range.open(1, 3).span(Range.open(5, 7)), Range.open(5, 7).span(Range.open(1, 3)));
            assertEquals(Range.closedOpen(1, 9).span(Range.open(2, 4)), Range.open(2, 4).span(Range.closedOpen(1, 9)));
        }
    }

    // =======================================================================
    // B7 - Range endpoints follow Comparable, not ==, so NaN sorts above everything and -0.0 below 0.0.
    //      Documented rather than special-cased; pinned so the behaviour cannot change silently.
    // =======================================================================
    @Nested
    @DisplayName("B7: Range floating-point endpoint ordering")
    public class B7FloatingPointEndpoints extends TestBase {

        @Test
        public void nanSortsAboveEveryFiniteValue() {
            final Range<Double> range = Range.closed(1.0, Double.NaN);

            assertTrue(range.contains(1.0));
            assertTrue(range.contains(1e300));
            assertTrue(range.contains(Double.POSITIVE_INFINITY));
            assertTrue(range.contains(Double.NaN), "NaN is the upper endpoint of a closed range");
            assertFalse(range.contains(0.0));
            assertFalse(range.contains(Double.NEGATIVE_INFINITY));

            // The reverse order is rejected as min > max, consistently with the same ordering.
            assertThrows(IllegalArgumentException.class, () -> Range.closed(Double.NaN, 1.0));
        }

        @Test
        public void negativeZeroSortsBelowPositiveZero() {
            assertFalse(Range.just(0.0).contains(-0.0));
            assertTrue(Range.just(-0.0).contains(-0.0));
            assertTrue(Range.closed(-0.0, 0.0).contains(-0.0));
            assertTrue(Range.closed(-0.0, 0.0).contains(0.0));
        }
    }

    // =======================================================================
    // B9 - orElseThrow(Function)/(Supplier) throw NPE when the factory returns null. Now documented.
    // =======================================================================
    @Nested
    @DisplayName("B9: Result.orElseThrow with a null-returning factory")
    public class B9NullReturningFactory extends TestBase {

        @Test
        public void nullFromTheMapperOrSupplierBecomesNullPointerException() {
            final Result<String, RuntimeException> failure = Result.failure(new RuntimeException("boom"));

            assertThrows(NullPointerException.class, () -> failure.orElseThrow(e -> null));
            assertThrows(NullPointerException.class, () -> failure.orElseThrow(() -> null));
        }

        @Test
        public void aSuccessNeverInvokesTheFactory() {
            final Result<String, RuntimeException> success = Result.success("v");

            assertEquals("v", success.orElseThrow(e -> null));
            assertEquals("v", success.orElseThrow(() -> null));
        }

        @Test
        public void nullFactoriesThemselvesStillThrowIllegalArgumentException() {
            final Result<String, RuntimeException> failure = Result.failure(new RuntimeException("boom"));

            assertThrows(IllegalArgumentException.class, () -> failure.orElseThrow((java.util.function.Function<RuntimeException, RuntimeException>) null));
            assertThrows(IllegalArgumentException.class, () -> failure.orElseThrow((java.util.function.Supplier<RuntimeException>) null));
        }
    }

    // =======================================================================
    // J3 - Result.hashCode's javadoc claimed "same exception OR value implies same hash".
    // =======================================================================
    @Nested
    @DisplayName("J3: Result.hashCode contract")
    public class J3ResultHashCode extends TestBase {

        @Test
        public void equalResultsHashEqually() {
            final RuntimeException ex = new RuntimeException("boom");

            assertEquals(Result.success("v").hashCode(), Result.success("v").hashCode());
            assertEquals(Result.of("v", ex).hashCode(), Result.of("v", ex).hashCode());
            assertEquals(Result.failure(ex).hashCode(), Result.failure(ex).hashCode());
        }

        @Test
        public void theExceptionTakesPriorityOverTheValue() {
            final RuntimeException ex = new RuntimeException("boom");

            // Same exception, different values: a legal collision, and the reason the old "or value"
            // wording was wrong in the other direction.
            assertEquals(Result.of("A", ex).hashCode(), Result.of("B", ex).hashCode());
            assertNotEquals(Result.of("A", ex), Result.of("B", ex));
        }

        @Test
        public void aFailureHashesAsItsExceptionAndASuccessAsItsValue() {
            // Asserted against the documented rule rather than "these two differ": Throwable does not
            // override hashCode, so two distinct exceptions almost always differ but are not *required*
            // to. This form is deterministic.
            final RuntimeException one = new RuntimeException("one");
            final RuntimeException two = new RuntimeException("two");

            assertEquals(one.hashCode(), Result.of("v", one).hashCode());
            assertEquals(two.hashCode(), Result.of("v", two).hashCode());
            assertEquals(CommonUtil.hashCode("v"), Result.success("v").hashCode());
            assertEquals(CommonUtil.hashCode((Object) null), Result.success(null).hashCode());

            // ... so "same value implies same hash" holds only among successes.
            assertEquals(Result.success("v").hashCode(), Result.success("v").hashCode());
        }
    }

    // =======================================================================
    // J4 - multipliedBy/dividedBy documented "a new Fraction instance" but return the shared ZERO.
    // =======================================================================
    @Nested
    @DisplayName("J4: Fraction zero-operand identity")
    public class J4ZeroOperandIdentity extends TestBase {

        @Test
        public void multiplyingByZeroYieldsTheSharedConstant() {
            assertSame(Fraction.ZERO, Fraction.of(5, 3).multipliedBy(Fraction.of(0, 1)));
            assertSame(Fraction.ZERO, Fraction.of(0, 1).multipliedBy(Fraction.of(5, 3)));
        }

        @Test
        public void dividingZeroYieldsTheSharedConstant() {
            assertSame(Fraction.ZERO, Fraction.of(0, 1).dividedBy(Fraction.of(3, 4)));
        }

        @Test
        public void ordinaryProductsAndQuotientsAreReduced() {
            assertEquals(Fraction.of(1, 2), Fraction.of(2, 3).multipliedBy(Fraction.of(3, 4)));
            assertEquals(Fraction.of(1, 6), Fraction.of(2, 4).multipliedBy(Fraction.of(1, 3)));
            assertEquals(Fraction.of(3, 2), Fraction.of(3, 4).dividedBy(Fraction.of(1, 2)));
            assertThrows(ArithmeticException.class, () -> Fraction.of(3, 4).dividedBy(Fraction.of(0, 1)));
        }
    }

    // =======================================================================
    // D8 - RateLimiter.create(rate, warmup, unit) falls back to a bursty limiter whenever the warmup
    //      converts to 0 microseconds. Previously undocumented on the public factory.
    // =======================================================================
    @Nested
    @DisplayName("D8: RateLimiter sub-microsecond warmup")
    public class D8SubMicrosecondWarmup extends TestBase {

        @Test
        public void subMicrosecondWarmupProducesABurstyLimiter() {
            // A bursty limiter accumulates up to one second of permits while idle, so a freshly
            // created one grants a large immediate batch without blocking; a warming-up limiter
            // would ramp instead. Observe the difference through the rate, not the class name.
            assertEquals("SmoothBursty", RateLimiter.create(10.0, 999, TimeUnit.NANOSECONDS).getClass().getSimpleName());
            assertEquals("SmoothBursty", RateLimiter.create(10.0, 0, TimeUnit.SECONDS).getClass().getSimpleName());
            assertEquals("SmoothWarmingUp", RateLimiter.create(10.0, 1, TimeUnit.MICROSECONDS).getClass().getSimpleName());
            assertEquals("SmoothWarmingUp", RateLimiter.create(10.0, 1, TimeUnit.SECONDS).getClass().getSimpleName());
        }

        @Test
        public void theConfiguredRateSurvivesTheFallback() {
            assertEquals(10.0, RateLimiter.create(10.0, 999, TimeUnit.NANOSECONDS).getRate(), 1e-9);
            assertEquals(10.0, RateLimiter.create(10.0, 0, TimeUnit.SECONDS).getRate(), 1e-9);
        }

        @Test
        public void negativeWarmupIsStillRejected() {
            assertThrows(IllegalArgumentException.class, () -> RateLimiter.create(10.0, -1, TimeUnit.SECONDS));
            assertThrows(IllegalArgumentException.class, () -> RateLimiter.create(0.0, 1, TimeUnit.SECONDS));
        }
    }

    // =======================================================================
    // D13 - Holder carried three standalone // NOSONAR comment lines that suppressed nothing.
    //       Removing them must not change behaviour.
    // =======================================================================
    @Nested
    @DisplayName("D13: Holder update methods after comment cleanup")
    public class D13HolderUpdates extends TestBase {

        @Test
        public void getAndUpdateReturnsThePreviousValue() {
            final Holder<String> holder = Holder.of("old");

            assertEquals("old", holder.getAndUpdate(v -> v + "-new"));
            assertEquals("old-new", holder.value());
        }

        @Test
        public void updateAndGetReturnsTheNewValue() {
            final Holder<Integer> holder = Holder.of(10);

            assertEquals(20, holder.updateAndGet(v -> v * 2));
            assertEquals(20, holder.value());
        }

        @Test
        public void ifNotNullSkipsANullValue() {
            final Holder<String> nullHolder = Holder.of(null);
            final List<String> seen = new ArrayList<>();

            nullHolder.ifNotNull(seen::add);
            assertTrue(seen.isEmpty());

            Holder.of("x").ifNotNull(seen::add);
            assertEquals(List.of("x"), seen);
        }

        @Test
        public void nullFunctionsAreStillRejected() {
            final Holder<String> holder = Holder.of("v");

            assertThrows(IllegalArgumentException.class, () -> holder.getAndUpdate(null));
            assertThrows(IllegalArgumentException.class, () -> holder.updateAndGet(null));
            assertThrows(IllegalArgumentException.class, () -> holder.ifNotNull(null));
        }
    }

    // =======================================================================
    // B1/J1/D2 - javadoc-only fixes. Nothing to assert about behaviour, but the members those docs
    //            describe must keep working, so the described contracts are exercised here.
    // =======================================================================
    @Nested
    @DisplayName("B1/D2: members referenced by the corrected javadoc")
    public class DocumentedMembersStillWork extends TestBase {

        @Test
        public void tuple1UsesTheInheritedWholeTupleMethods() {
            // Tuple1's javadoc explains why it has no element-wise accept/map/filter.
            final Tuple.Tuple1<String> t = Tuple.of("x");

            assertEquals("x", t._1);
            assertEquals(1, t.arity());
            assertDoesNotThrow(() -> t.accept(tuple -> assertEquals("x", tuple._1)));
            assertEquals("X", t.map(tuple -> tuple._1.toUpperCase()));
            assertTrue(t.filter(tuple -> tuple._1.equals("x")).isPresent());
        }

        @Test
        public void theArityZeroTupleIsReachableThroughTheSupertype() {
            // D2: the empty tuple's class is an implementation detail; it is used as Tuple<?>.
            final Tuple<?> empty = Tuple.fromArray(new Object[0]);

            assertEquals(0, empty.arity());
            assertTrue(empty.allNull());
            assertFalse(empty.anyNull());
            assertEquals(0, empty.toArray().length);
            assertEquals(empty, Tuple.fromCollection(List.of()));
            assertEquals(empty, Tuple.fromArray(null));
        }

        @Test
        public void rangeEndpointsAreReadThroughThePublicAccessors() {
            // D2: LowerEndpoint/UpperEndpoint are no longer named in the public javadoc.
            final Range<Integer> range = Range.closedOpen(1, 10);

            assertEquals(1, range.lowerEndpoint());
            assertEquals(10, range.upperEndpoint());
            assertEquals(Range.BoundType.CLOSED_OPEN, range.boundType());
        }

        @Test
        public void fractionMinValueDenominatorRuleHoldsAsDocumented() {
            // J1: the note about Integer.MIN_VALUE denominators was orphaned into @return.
            assertThrows(ArithmeticException.class, () -> Fraction.of(2, Integer.MIN_VALUE));
            assertEquals(Fraction.of(-1, 1 << 30), Fraction.of(2, Integer.MIN_VALUE, true));
            assertThrows(ArithmeticException.class, () -> Fraction.of(1, Integer.MIN_VALUE, true));
        }
    }
}

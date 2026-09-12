package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.type.Type;

/**
 * Locks the documented behaviour corrected by findings J1-J4 of the 2026-08-30j review pass
 * (fixes applied 2026-08-31). Every assertion here pins a sentence that the Javadoc previously
 * stated wrongly, incompletely, or vacuously; see {@code scripts/cross_review/Numbers_ledger_2026-08-30j.md}.
 */
public class NumbersRegressionDTest extends TestBase {

    // ==================================================================================================
    // J1: a trailing type suffix changes the GRAMMAR, not just the width. The old sentence claimed
    // "createNumber(s) and createNumber(s + \"d\") agree on the result type for every s".
    // ==================================================================================================

    @Test
    public void test_createNumber_typeSuffixAgreesOnlyForAnUnsuffixedFloatingToken() {
        // The claim as corrected: it holds for a non-hexadecimal token carrying a decimal point or an
        // exponent and no type suffix of its own, because the length routing measures the payload
        // without the suffix.
        for (final String s : new String[] { "1.5", "1e5", "1E5", ".5", "1.", "0.9", "01e1", "1.5e3", "-1.5", "+1.5", "0.0", "-0.0",
                "1.0000000000000000000000001", "1e400", "-1e400", "1e-400", "0e999999999" }) {
            assertEquals(Numbers.createNumber(s).getClass(), Numbers.createNumber(s + "d").getClass(), s);
        }
    }

    @Test
    public void test_createNumber_typeSuffixMovesAnIntegralTokenOntoTheDecimalPath() {
        // An integral token: the suffix changes both the type and, for a leading-zero token, the radix.
        assertInstanceOf(Integer.class, Numbers.createNumber("123"));
        assertInstanceOf(Double.class, Numbers.createNumber("123d"));
        assertEquals(123, Numbers.createNumber("123").intValue());
        assertEquals(123.0d, Numbers.createNumber("123d"));

        // "010" is octal 8 unsuffixed, but the suffix selects the decimal path, so it becomes 10.0.
        assertEquals(8, Numbers.createNumber("010").intValue());
        assertEquals(10.0d, Numbers.createNumber("010d"));
        assertEquals(83, Numbers.createNumber("0123").intValue());
        assertEquals(123.0d, Numbers.createNumber("0123d"));
    }

    @Test
    public void test_createNumber_typeSuffixIsAHexDigitOnTheHexadecimalPath() {
        // d/D and f/F are hexadecimal digits, so on the hex path the "suffix" is consumed as magnitude.
        assertEquals(16, Numbers.createNumber("0x10").intValue());
        assertEquals(269, Numbers.createNumber("0x10d").intValue()); // 0x10d, not 0x10 as a double
        assertEquals(16, Numbers.createNumber("#10").intValue());
        assertEquals(269, Numbers.createNumber("#10d").intValue());
        assertEquals(255, Numbers.createNumber("0xFF").intValue());
        assertEquals(4093, Numbers.createNumber("0xFFd").intValue());

        // ... while an 'l'/'L' suffix is NOT a hex digit and is still rejected on that path.
        assertThrows(NumberFormatException.class, () -> Numbers.createNumber("0xFFL"));
    }

    @Test
    public void test_createNumber_typeIsUnaffectedByLengthOrSuffix() {
        // The claim the sentence is actually about: a token sitting either side of
        // MAX_FLOATING_POINT_TOKEN_LENGTH gets the same type with and without a trailing 'd' -- and, since
        // 2026-09-02, the same type on both sides too, because the create family no longer looks at length.
        final String justUnder = "1." + "0".repeat(Numbers.MAX_FLOATING_POINT_TOKEN_LENGTH - 2);
        final String justOver = "1." + "0".repeat(Numbers.MAX_FLOATING_POINT_TOKEN_LENGTH - 1);
        assertEquals(Numbers.MAX_FLOATING_POINT_TOKEN_LENGTH, justUnder.length());
        assertEquals(Numbers.MAX_FLOATING_POINT_TOKEN_LENGTH + 1, justOver.length());

        assertInstanceOf(Double.class, Numbers.createNumber(justUnder));
        assertInstanceOf(Double.class, Numbers.createNumber(justUnder + "d"));
        assertInstanceOf(Double.class, Numbers.createNumber(justOver));
        assertInstanceOf(Double.class, Numbers.createNumber(justOver + "d"));
    }

    // ==================================================================================================
    // J2: the defaultValue of every to*(Object, default) also covers a NULL toString(), not only an
    // empty one. The Javadoc named only "empty" at all 34 sites.
    // ==================================================================================================

    /** A non-{@code Number} whose {@code toString()} returns {@code null}, which is legal. */
    private static final Object NULL_TO_STRING = new Object() {
        @Override
        public String toString() {
            return null;
        }
    };

    /** The same object with an empty rendering, for the side-by-side comparison. */
    private static final Object EMPTY_TO_STRING = new Object() {
        @Override
        public String toString() {
            return "";
        }
    };

    @Test
    public void test_toXxxObject_nullToStringTakesTheDefaultJustLikeAnEmptyOne() {
        assertEquals((byte) 7, Numbers.toByte(NULL_TO_STRING, (byte) 7));
        assertEquals((short) 7, Numbers.toShort(NULL_TO_STRING, (short) 7));
        assertEquals(7, Numbers.toInt(NULL_TO_STRING, 7));
        assertEquals(7L, Numbers.toLong(NULL_TO_STRING, 7L));
        assertEquals(7f, Numbers.toFloat(NULL_TO_STRING, 7f));
        assertEquals(7d, Numbers.toDouble(NULL_TO_STRING, 7d));

        // identical to the empty-toString() case the Javadoc already described
        assertEquals(Numbers.toByte(EMPTY_TO_STRING, (byte) 7), Numbers.toByte(NULL_TO_STRING, (byte) 7));
        assertEquals(Numbers.toShort(EMPTY_TO_STRING, (short) 7), Numbers.toShort(NULL_TO_STRING, (short) 7));
        assertEquals(Numbers.toInt(EMPTY_TO_STRING, 7), Numbers.toInt(NULL_TO_STRING, 7));
        assertEquals(Numbers.toLong(EMPTY_TO_STRING, 7L), Numbers.toLong(NULL_TO_STRING, 7L));
        assertEquals(Numbers.toFloat(EMPTY_TO_STRING, 7f), Numbers.toFloat(NULL_TO_STRING, 7f));
        assertEquals(Numbers.toDouble(EMPTY_TO_STRING, 7d), Numbers.toDouble(NULL_TO_STRING, 7d));
    }

    @Test
    public void test_toXxxObject_nullToStringYieldsZeroOnTheNoDefaultOverloads() {
        assertEquals((byte) 0, Numbers.toByte(NULL_TO_STRING));
        assertEquals((short) 0, Numbers.toShort(NULL_TO_STRING));
        assertEquals(0, Numbers.toInt(NULL_TO_STRING));
        assertEquals(0L, Numbers.toLong(NULL_TO_STRING));
        assertEquals(0f, Numbers.toFloat(NULL_TO_STRING));
        assertEquals(0d, Numbers.toDouble(NULL_TO_STRING));
    }

    @Test
    public void test_aNumberWithANullToStringIsADifferentCaseAndUsesDoubleValue() {
        // A Number is never routed through toByte(String, ...): its null toString() falls back to the
        // finite- and range-checked doubleValue() view, so the default does NOT apply.
        final Number nullText = new Number() {
            private static final long serialVersionUID = 1L;

            @Override
            public String toString() {
                return null;
            }

            @Override
            public int intValue() {
                return 42;
            }

            @Override
            public long longValue() {
                return 42L;
            }

            @Override
            public float floatValue() {
                return 42f;
            }

            @Override
            public double doubleValue() {
                return 42d;
            }
        };

        assertEquals(42, Numbers.toInt(nullText, 7));
        assertEquals(42L, Numbers.toLong(nullText, 7L));
        assertEquals((byte) 42, Numbers.toByte(nullText, (byte) 7));
    }

    // ==================================================================================================
    // J3: convert(...) can also throw ArithmeticException when an arbitrary-precision RESULT exceeds the
    // JDK's supported BigInteger magnitude. The @throws tags named only overflow and NaN/Infinity.
    // ==================================================================================================

    @Test
    public void test_convert_bigIntegerMagnitudeLimitThrowsArithmeticException() {
        final BigDecimal huge = new BigDecimal("1e2147483647");

        assertThrows(ArithmeticException.class, () -> Numbers.convert(huge, BigInteger.class));
        assertThrows(ArithmeticException.class, () -> Numbers.convert(huge, BigInteger.class, BigInteger.ZERO));
        assertThrows(ArithmeticException.class, () -> Numbers.convert(huge, Type.of(BigInteger.class)));

        // the bounded integral targets reject the same value as ordinary overflow, not as a magnitude limit
        final ArithmeticException overflow = assertThrows(ArithmeticException.class, () -> Numbers.convert(huge, Long.class));
        assertTrue(overflow.getMessage().startsWith("long overflow:"), overflow.getMessage());
    }

    @Test
    public void test_convert_bigDecimalTargetHasNoMagnitudeLimit() {
        // The limit is BigInteger's, not BigDecimal's: a BigDecimal keeps an unscaled value plus a scale, so
        // the same input that overflows the BigInteger target converts fine here. The @throws tag names only
        // the BigInteger target for exactly this reason.
        final BigDecimal huge = new BigDecimal("1e2147483647");

        assertEquals(0, huge.compareTo(Numbers.convert(huge, BigDecimal.class)));
        assertEquals(0, huge.compareTo(Numbers.convert(huge, Type.of(BigDecimal.class))));

        // ... including when the source is an unrecognized Number subtype recovered from its own text
        final Number hugeText = new Number() {
            private static final long serialVersionUID = 1L;

            @Override
            public String toString() {
                return "1e2147483647";
            }

            @Override
            public int intValue() {
                return 0;
            }

            @Override
            public long longValue() {
                return 0L;
            }

            @Override
            public float floatValue() {
                return 0f;
            }

            @Override
            public double doubleValue() {
                return 0d;
            }
        };
        assertEquals(0, huge.compareTo(Numbers.convert(hugeText, BigDecimal.class)));
        assertThrows(ArithmeticException.class, () -> Numbers.convert(hugeText, BigInteger.class));
    }

    // ==================================================================================================
    // J4: the biggestBinomials comment now states the invariant in terms of C(n, k). This test is that
    // invariant, observed through the method that actually reports the boundary.
    // ==================================================================================================

    @Test
    public void test_biggestBinomials_boundaryIsObservedByBinomialExactToLong() {
        // biggestBinomials[k] for k = 3..6, from the private table.
        final int[][] boundaries = { { 3, 3810779 }, { 4, 121977 }, { 5, 16175 }, { 6, 4337 } };

        for (final int[] boundary : boundaries) {
            final int k = boundary[0];
            final int n = boundary[1];

            // C(n, k) fits in a long ...
            final long fits = Numbers.binomialExactToLong(n, k);
            assertTrue(fits > 0, "C(" + n + ", " + k + ") should fit in a long");

            // ... but C(n + 1, k) does not.
            assertThrows(ArithmeticException.class, () -> Numbers.binomialExactToLong(n + 1, k), "C(" + (n + 1) + ", " + k + ") should not fit in a long");

            // and that is exactly where the saturating form starts returning Long.MAX_VALUE
            assertEquals(fits, Numbers.saturatedBinomialToLong(n, k));
            assertEquals(Long.MAX_VALUE, Numbers.saturatedBinomialToLong(n + 1, k));

            // the saturating form never throws here, which is why "fits in a long" said nothing about it
            assertNotEquals(0L, Numbers.saturatedBinomialToLong(n + 1, k));
        }
    }

    // ==================================================================================================
    // B1 follow-up: the BigInteger target short-circuits |x| < 1 to ZERO instead of building the exact
    // BigDecimal, which for a subnormal is ~750 significant digits. Must be value-identical.
    // ==================================================================================================

    @Test
    public void test_convert_bigIntegerTargetTruncatesSubOneMagnitudesToZero() {
        for (final double d : new double[] { 0.0d, -0.0d, 0.5d, -0.5d, 0.9d, -0.9d, 1e-300d, Double.MIN_VALUE, -Double.MIN_VALUE, Math.nextDown(1.0d),
                Math.nextUp(-1.0d) }) {
            assertEquals(BigInteger.ZERO, Numbers.convert(d, BigInteger.class), String.valueOf(d));
            // identical to taking the exact value and truncating it, which is what the branch replaces
            assertEquals(new BigDecimal(d).toBigInteger(), Numbers.convert(d, BigInteger.class), String.valueOf(d));
        }
        for (final float f : new float[] { 0.0f, -0.0f, 0.5f, -0.5f, 0.9f, -0.9f, Float.MIN_VALUE, -Float.MIN_VALUE, Math.nextDown(1.0f),
                Math.nextUp(-1.0f) }) {
            assertEquals(BigInteger.ZERO, Numbers.convert(f, BigInteger.class), String.valueOf(f));
            assertEquals(new BigDecimal(f).toBigInteger(), Numbers.convert(f, BigInteger.class), String.valueOf(f));
        }

        // the boundary itself is NOT short-circuited
        assertEquals(BigInteger.ONE, Numbers.convert(1.0d, BigInteger.class));
        assertEquals(BigInteger.valueOf(-1), Numbers.convert(-1.0d, BigInteger.class));
        assertEquals(BigInteger.ONE, Numbers.convert(1.0f, BigInteger.class));
        assertEquals(BigInteger.valueOf(-1), Numbers.convert(-1.0f, BigInteger.class));
        assertEquals(BigInteger.ONE, Numbers.convert(1.9d, BigInteger.class));
        assertEquals(BigInteger.valueOf(-1), Numbers.convert(-1.9d, BigInteger.class));

        // non-finite input is still rejected before the short-circuit can see it
        assertThrows(ArithmeticException.class, () -> Numbers.convert(Double.NaN, BigInteger.class));
        assertThrows(ArithmeticException.class, () -> Numbers.convert(Float.NaN, BigInteger.class));

        // and the BigDecimal target is untouched: it keeps the value, it does not truncate
        assertEquals(0, new BigDecimal("0.9").compareTo(Numbers.convert(0.9d, BigDecimal.class)));
        assertEquals(0, new BigDecimal("0.5").compareTo(Numbers.convert(0.5f, BigDecimal.class)));
        assertNotEquals(BigInteger.ZERO, Numbers.convert(0.9d, BigDecimal.class).unscaledValue());
    }

    // ==================================================================================================
    // B1 follow-up: the same sub-one short-circuit for a BigDecimal source. Before it, the BigInteger
    // target disagreed with every other integral target on a value smaller than one.
    // ==================================================================================================

    @Test
    public void test_convert_bigDecimalSourceBelowOneTruncatesToZeroOnEveryIntegralTarget() {
        // |v| < 1, so every integral target must answer 0. The BigInteger target used to throw
        // ArithmeticException("BigInteger would overflow supported range") here -- for a value smaller
        // than one -- because toBigInteger() rescales by building 10^scale.
        final BigDecimal tiny = new BigDecimal("1e-2147483647");

        assertEquals(Byte.valueOf((byte) 0), Numbers.convert(tiny, Byte.class));
        assertEquals(Short.valueOf((short) 0), Numbers.convert(tiny, Short.class));
        assertEquals(Integer.valueOf(0), Numbers.convert(tiny, Integer.class));
        assertEquals(Long.valueOf(0L), Numbers.convert(tiny, Long.class));
        assertEquals(0L, Numbers.toLong(tiny));
        assertEquals(BigInteger.ZERO, Numbers.convert(tiny, BigInteger.class));

        // ... and the same through the unrecognized-Number recovery, which shares the helper
        final Number unknown = new Number() {
            private static final long serialVersionUID = 1L;

            @Override
            public String toString() {
                return "1e-2147483647";
            }

            @Override
            public int intValue() {
                return 0;
            }

            @Override
            public long longValue() {
                return 0L;
            }

            @Override
            public float floatValue() {
                return 0f;
            }

            @Override
            public double doubleValue() {
                return 0d;
            }
        };
        assertEquals(Long.valueOf(0L), Numbers.convert(unknown, Long.class));
        assertEquals(BigInteger.ZERO, Numbers.convert(unknown, BigInteger.class));

        // a genuinely colossal value is still rejected -- the short-circuit must not swallow that
        assertThrows(ArithmeticException.class, () -> Numbers.convert(new BigDecimal("1e2147483647"), BigInteger.class));
    }

    @Test
    public void test_convert_bigDecimalSourceAgreesWithToBigIntegerAtEveryBoundary() {
        // The predicate is "precision - scale <= 0", i.e. no digits before the decimal point. Check both
        // sides of it, and the signed-zero and negative-scale forms.
        for (final String s : new String[] { "0", "-0", "0.0", "-0.0", "0E+100", "0E-100", "0.5", "-0.5", "0.9999", "-0.9999", "0.1", "1", "-1", "1.0", "-1.0",
                "1.5", "-1.5", "10", "10.5", "1E+2", "1E-2", "1E+0", "9.99999999999999999999", "0.00000000000000000001", "100000000000000000000.5",
                "-100000000000000000000.5" }) {
            final BigDecimal bd = new BigDecimal(s);
            assertEquals(bd.toBigInteger(), Numbers.convert(bd, BigInteger.class), s);
        }
    }

    /**
     * The sub-one short-circuit is gated on a scale threshold so that the {@code O(digits)}
     * {@link BigDecimal#precision()} call is not paid on every conversion. The threshold is a pure
     * performance knob &mdash; correctness must not depend on which side of it a value falls &mdash; so this
     * walks straight across it. Without this the suite only exercised scales of about 20 or of
     * {@code Integer.MAX_VALUE}, never the boundary itself.
     */
    @Test
    public void test_convert_bigDecimalSourceIsCorrectOnBothSidesOfTheScaleGate() {
        for (final int scale : new int[] { 0, 1, 9998, 9999, 10000, 10001, 10002, 12000 }) {
            // |v| < 1: one significant digit, so precision - scale <= 0 for every scale above zero
            final BigDecimal subOne = new BigDecimal(BigInteger.ONE, scale);
            assertEquals(subOne.toBigInteger(), Numbers.convert(subOne, BigInteger.class), "sub-one, scale " + scale);
            assertEquals(BigInteger.valueOf(Numbers.convert(subOne, Long.class)), Numbers.convert(subOne, BigInteger.class),
                    "sub-one agrees with the long target, scale " + scale);
            if (scale > 0) {
                assertEquals(BigInteger.ZERO, Numbers.convert(subOne, BigInteger.class), "sub-one, scale " + scale);
            }

            // |v| >= 1: three more digits than the scale, so the value survives truncation
            final BigDecimal aboveOne = new BigDecimal(BigInteger.TEN.pow(scale + 3), scale);
            assertEquals(aboveOne.toBigInteger(), Numbers.convert(aboveOne, BigInteger.class), "above-one, scale " + scale);
            assertNotEquals(BigInteger.ZERO, Numbers.convert(aboveOne, BigInteger.class), "above-one, scale " + scale);

            // EXACTLY one digit before the point (precision - scale == 1) -- the tightest case the predicate
            // must not swallow, and the one a "<= 1" off-by-one would. The cases above all have four, so
            // they slip past such a mutation; these do not.
            final BigDecimal sevenPointZero = new BigDecimal(BigInteger.TEN.pow(scale).multiply(BigInteger.valueOf(7)), scale);
            assertEquals(BigInteger.valueOf(7), Numbers.convert(sevenPointZero, BigInteger.class), "7.0, scale " + scale);

            final BigDecimal nineNines = new BigDecimal(BigInteger.TEN.pow(scale + 1).subtract(BigInteger.ONE), scale);
            assertEquals(BigInteger.valueOf(9), Numbers.convert(nineNines, BigInteger.class), "9.99..., scale " + scale);
            assertEquals(BigInteger.valueOf(-9), Numbers.convert(nineNines.negate(), BigInteger.class), "-9.99..., scale " + scale);

            // and the negated forms, so the sign cannot be lost by the short-circuit
            assertEquals(subOne.negate().toBigInteger(), Numbers.convert(subOne.negate(), BigInteger.class), "-sub-one, scale " + scale);
            assertEquals(aboveOne.negate().toBigInteger(), Numbers.convert(aboveOne.negate(), BigInteger.class), "-above-one, scale " + scale);
        }

        // a zero is short-circuited by signum() before precision() is ever consulted, at any scale
        for (final int scale : new int[] { 0, 10001, Integer.MAX_VALUE }) {
            assertEquals(BigInteger.ZERO, Numbers.convert(new BigDecimal(BigInteger.ZERO, scale), BigInteger.class), "zero, scale " + scale);
        }

        // a large NEGATIVE scale is a colossal value, not a sub-one one: the guard widens the subtraction to
        // long precisely so this cannot overflow int and be mistaken for one
        assertNotEquals(BigInteger.ZERO, Numbers.convert(new BigDecimal(BigInteger.ONE, -300), BigInteger.class));
        assertEquals(new BigDecimal(BigInteger.ONE, -300).toBigInteger(), Numbers.convert(new BigDecimal(BigInteger.ONE, -300), BigInteger.class));
        assertThrows(ArithmeticException.class, () -> Numbers.convert(new BigDecimal(BigInteger.ONE, Integer.MIN_VALUE + 1), BigInteger.class));
    }

    // ==================================================================================================
    // D1: the extractFirst* family scans a PLAIN decimal token. RegExUtil.INTEGER_FINDER only ever yields
    // an optional sign plus ASCII digits, so no radix prefix can reach the scanner.
    // ==================================================================================================

    @Test
    public void test_extractFirstInt_neverInterpretsARadixPrefix() {
        // The finder stops at 'x', so the match is the leading "0" -- the extract family is decimal-only,
        // unlike the decimal-first to* parsers, which do honour a 0x/# prefix.
        assertEquals(0, Numbers.extractFirstInt("0x1F").getAsInt());
        assertEquals(31, Numbers.toInt("0x1F"));
        assertEquals(0, Numbers.extractFirstLong("0x1F").getAsLong());
        assertEquals(31L, Numbers.toLong("0x1F"));

        // a '#' prefix is not matched at all: there is no digit before it
        assertEquals(255, Numbers.extractFirstIntOrElse("#FF", 255));
        assertEquals(255, Numbers.toInt("#FF"));

        // a leading zero is decimal padding here, exactly as in the to* family (never octal)
        assertEquals(10, Numbers.extractFirstInt("010").getAsInt());
        assertEquals(8, Numbers.extractFirstInt("08").getAsInt());
        assertEquals(10L, Numbers.extractFirstLong("010").getAsLong());

        // the OrElse variants publish the same examples in their Javadoc, so execute those too
        assertEquals(0, Numbers.extractFirstIntOrElse("0x1F", -1));
        assertEquals(10, Numbers.extractFirstIntOrElse("010", -1));
        assertEquals(0L, Numbers.extractFirstLongOrElse("0x1F", -1L));
        assertEquals(10L, Numbers.extractFirstLongOrElse("010", -1L));
    }

    @Test
    public void test_extractFirstLong_stillResolvesTheLongMinValueToken() {
        // Long.MIN_VALUE collides with the scanner's INVALID sentinel and is disambiguated lexically; that
        // path takes the same grammar argument, so it has to keep working after the D1 change.
        assertEquals(Long.MIN_VALUE, Numbers.extractFirstLong("x=-9223372036854775808").getAsLong());
        assertEquals(Long.MIN_VALUE, Numbers.extractFirstLongOrElse("-9223372036854775808", 0L));
        assertEquals(Long.MAX_VALUE, Numbers.extractFirstLong("9223372036854775807").getAsLong());
        assertThrows(NumberFormatException.class, () -> Numbers.extractFirstLong("9223372036854775808"));
        assertThrows(NumberFormatException.class, () -> Numbers.extractFirstLong("-9223372036854775809"));
    }

    // ==================================================================================================
    // D2: the create-family type-suffix detection uses an ASCII digit test, like the rest of the class.
    // ==================================================================================================

    @Test
    public void test_createNumber_typeSuffixDetectionIsAsciiOnly() {
        // A non-ASCII digit is rejected by the grammar, so it can never reach the suffix test.
        assertThrows(NumberFormatException.class, () -> Numbers.createNumber("12٣")); // Arabic-Indic 3
        assertFalse(Numbers.isCreatable("12٣"));

        // and the ASCII cases the test actually governs still route correctly
        assertInstanceOf(Integer.class, Numbers.createNumber("123"));
        assertInstanceOf(Double.class, Numbers.createNumber("123."));
        assertInstanceOf(Double.class, Numbers.createNumber("1.5"));
        assertInstanceOf(Float.class, Numbers.createNumber("1.5f"));
        assertInstanceOf(Double.class, Numbers.createNumber("1.5d"));
        assertInstanceOf(Long.class, Numbers.createNumber("123L"));
    }

    // ==================================================================================================
    // D3: divide(long, long, mode) states its divide-by-zero guard explicitly, as the int overload does.
    // ==================================================================================================

    @Test
    public void test_divide_byZeroReportsTheSameWayForIntAndLong() {
        for (final RoundingMode mode : RoundingMode.values()) {
            final ArithmeticException longEx = assertThrows(ArithmeticException.class, () -> Numbers.divide(1L, 0L, mode));
            final ArithmeticException intEx = assertThrows(ArithmeticException.class, () -> Numbers.divide(1, 0, mode));
            assertEquals("/ by zero", longEx.getMessage(), mode.name());
            assertEquals(intEx.getMessage(), longEx.getMessage(), mode.name());
        }

        // the guard runs before the MIN_VALUE/-1 overflow check, so a zero divisor wins over either
        assertEquals("/ by zero", assertThrows(ArithmeticException.class, () -> Numbers.divide(Long.MIN_VALUE, 0L, RoundingMode.DOWN)).getMessage());
        assertEquals("/ by zero", assertThrows(ArithmeticException.class, () -> Numbers.divide(Integer.MIN_VALUE, 0, RoundingMode.DOWN)).getMessage());

        // ... and a null mode is still rejected first, before either arithmetic guard
        assertThrows(IllegalArgumentException.class, () -> Numbers.divide(1L, 0L, null));
        assertThrows(IllegalArgumentException.class, () -> Numbers.divide(1, 0, null));

        // the MIN_VALUE/-1 overflow guard is unchanged
        assertEquals("long overflow", assertThrows(ArithmeticException.class, () -> Numbers.divide(Long.MIN_VALUE, -1L, RoundingMode.DOWN)).getMessage());
    }

    // ==================================================================================================
    // D5 (by design, not a fix): the extract* family reports an out-of-range token as
    // NumberFormatException where the to* family reports ArithmeticException. Locked so the divergence
    // cannot drift silently.
    // ==================================================================================================

    @Test
    public void test_outOfRangeExceptionTypeDivergesBetweenTheExtractAndToFamilies() {
        // extract*: NumberFormatException, and NOT ArithmeticException
        assertThrows(NumberFormatException.class, () -> Numbers.extractFirstInt("id=99999999999"));
        assertThrows(NumberFormatException.class, () -> Numbers.extractFirstIntOrElse("id=99999999999", 0));
        assertThrows(NumberFormatException.class, () -> Numbers.extractFirstLong("id=9999999999999999999"));
        assertThrows(NumberFormatException.class, () -> Numbers.extractFirstLongOrElse("id=9999999999999999999", 0L));

        // to*: ArithmeticException for the very same magnitude
        assertThrows(ArithmeticException.class, () -> Numbers.toInt("99999999999"));
        assertThrows(ArithmeticException.class, () -> Numbers.toLong("9999999999999999999"));

        // and the two are genuinely different types, not one extending the other
        assertFalse(ArithmeticException.class.isAssignableFrom(NumberFormatException.class));
        assertFalse(NumberFormatException.class.isAssignableFrom(ArithmeticException.class));
    }
}

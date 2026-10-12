/*
 * Copyright (C) 2019 HaiYang Li
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTimeout;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.DoubleAdder;
import java.util.concurrent.atomic.LongAdder;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.type.Type;

public class NumbersConversionTest extends NumbersTestSupport {

    /** A {@code Number} outside the built-in eight whose {@code toString()} is caller-controlled. */
    private static final class LoudNumber extends Number {
        private static final long serialVersionUID = 1L;

        private final String text;
        private final double value;

        LoudNumber(final String text) {
            this(text, 1);
        }

        LoudNumber(final String text, final double value) {
            this.text = text;
            this.value = value;
        }

        @Override
        public int intValue() {
            return (int) value;
        }

        @Override
        public long longValue() {
            return (long) value;
        }

        @Override
        public float floatValue() {
            return (float) value;
        }

        @Override
        public double doubleValue() {
            return value;
        }

        @Override
        public String toString() {
            return text;
        }
    }

    /** Minimal throwing-runnable so a loop can drive several call shapes through one assertion. */
    private interface Executable {
        public void run();
    }

    /** {@code toString()} is a well-formed integer token of arbitrary length; {@code doubleValue()} overflows. */
    private static final class HugeIntegralTextNumber extends Number {
        private static final long serialVersionUID = 1L;

        private final String text;

        HugeIntegralTextNumber(final int digits) {
            text = "9".repeat(digits);
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
            return Float.POSITIVE_INFINITY;
        }

        @Override
        public double doubleValue() {
            return Double.POSITIVE_INFINITY;
        }

        @Override
        public String toString() {
            return text;
        }
    }

    /** {@code toString()} returns {@code null}, which {@link Object#toString()} overrides are free to do. */
    private static final class NullTextNumber extends Number {
        private static final long serialVersionUID = 1L;

        private final double value;

        NullTextNumber(final double value) {
            this.value = value;
        }

        @Override
        public int intValue() {
            return (int) value;
        }

        @Override
        public long longValue() {
            return (long) value;
        }

        @Override
        public float floatValue() {
            return (float) value;
        }

        @Override
        public double doubleValue() {
            return value;
        }

        @Override
        public String toString() {
            return null;
        }
    }

    // J2: the defaultValue of every to*(Object, default) also covers a NULL toString(), not only an
    // empty one. The Javadoc named only "empty" at all 34 sites.

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

    // B1: Double -> Float is the IEEE-754 narrowing cast (ties to even)

    /** The exact midpoint between {@code f} and the next float up, as a double (always exactly representable). */
    private static double midpointAbove(final float f) {
        return ((double) f + (double) Math.nextUp(f)) / 2.0d;
    }

    /**
     * A {@code Number} this class does not recognize, whose {@code toString()} is never usable as a decimal
     * token, so every integral and arbitrary-precision target must fall back to {@code doubleValue()}.
     */
    private static final class OpaqueNumber extends Number {
        private static final long serialVersionUID = 1L;

        private final double value;
        private final String text;

        OpaqueNumber(final double value, final String text) {
            this.value = value;
            this.text = text;
        }

        @Override
        public int intValue() {
            return (int) value;
        }

        @Override
        public long longValue() {
            return (long) value;
        }

        @Override
        public float floatValue() {
            return (float) value;
        }

        @Override
        public double doubleValue() {
            return value;
        }

        @Override
        public String toString() {
            return text;
        }
    }

    /** Every text shape that the recovery must reject as "not a decimal token". */
    private static final String[] UNUSABLE_TEXTS = { null, "", "N/A", "1,000,000", " 12", "12 ms", "0x10", "1e", "--1", "1.2.3" };

    private static final BigInteger BI = new BigInteger("12345678901234567890");

    @SuppressWarnings("serial")
    private static final BigInteger BI_SUB = new BigInteger("12345678901234567890") {
        @Override
        public String toString() {
            return "12,345,678,901,234,567,890";
        }
    };

    @SuppressWarnings("serial")
    private static final BigDecimal BD_SUB = new BigDecimal("1.50") {
        @Override
        public String toString() {
            return "$1.50";
        }
    };

    @SuppressWarnings("serial")
    private static final BigDecimal BD_SUB_PLAIN = new BigDecimal("9007199254740993.75") {
    };

    private static Number customNumber(final String text, final double d) {
        return new Number() {
            @Override
            public int intValue() {
                return (int) d;
            }

            @Override
            public long longValue() {
                return (long) d;
            }

            @Override
            public float floatValue() {
                return (float) d;
            }

            @Override
            public double doubleValue() {
                return d;
            }

            @Override
            public String toString() {
                return text;
            }
        };
    }

    private static Number renderedAs(final String text, final double value) {
        return new Number() {
            private static final long serialVersionUID = 1L;

            @Override
            public int intValue() {
                return (int) value;
            }

            @Override
            public long longValue() {
                return (long) value;
            }

            @Override
            public float floatValue() {
                return (float) value;
            }

            @Override
            public double doubleValue() {
                return value;
            }

            @Override
            public String toString() {
                return text;
            }
        };
    }

    private static void assertSameConverter(final Class<? extends Number> primitive, final Class<? extends Number> wrapper) {
        final Number[] sources = { (byte) 7, (short) 7, 7, 7L, 7.0f, 7.0, BigInteger.valueOf(7), new BigDecimal("7") };

        for (final Number source : sources) {
            final Number viaPrimitive = Numbers.convert(source, primitive);
            final Number viaWrapper = Numbers.convert(source, wrapper);
            assertEquals(wrapper, viaPrimitive.getClass(), primitive + " from " + source.getClass().getSimpleName());
            assertEquals(viaWrapper, viaPrimitive, primitive + " from " + source.getClass().getSimpleName());
            assertEquals(viaWrapper, Numbers.convert(source, primitive, null), primitive + " (default overload)");
        }
    }

    // convert

    @Test
    @DisplayName("C-001: every convert overload agrees, including the Type and defaultValue forms")
    public void test_C001_allOverloadsAgree() {
        final NullTextNumber source = new NullTextNumber(42.0d);
        final Type<AtomicLong> atomicLongType = CommonUtil.typeOf(AtomicLong.class);

        assertThrows(NumberFormatException.class, () -> Numbers.convert(source, atomicLongType));
        assertThrows(NumberFormatException.class, () -> Numbers.convert(source, AtomicLong.class, new AtomicLong(-1)));
        assertThrows(NumberFormatException.class, () -> Numbers.convert(source, atomicLongType, new AtomicLong(-1)));
        assertThrows(IllegalArgumentException.class, () -> Numbers.convert(source, CommonUtil.typeOf(Number.class)));

        // defaultValue applies to a null value only, as before.
        assertEquals(-1L, Numbers.convert(null, AtomicLong.class, new AtomicLong(-1)).get());
        assertEquals(-1L, Numbers.convert(null, atomicLongType, new AtomicLong(-1)).get());
    }

    @Test
    @DisplayName("B2: every built-in target recovers the same value from the same source")
    public void test_B2_allTargetsAgreeOnTheSameSource() {
        for (final String text : new String[] { "42", "-42", "+42", "010", "12.9", "-12.9", "0.0", "-0.0", "1e3", "1.5e3", "1e-3", ".5", "42.", "0", "00000042",
                "9223372036854775807", "-9223372036854775808", "0xFF", "#FF", "999L", "not a number", "", "1,234", " 42 " }) {
            final SplitNumber value = n(text);
            final Long asLong;

            try {
                asLong = Numbers.convert(value, Long.class);
            } catch (final ArithmeticException e) {
                continue; // out of long range for both; nothing to compare
            }

            // BigInteger is the same truncation as Long, just unbounded, so whenever Long succeeds the two
            // must agree. Before this fix they disagreed for every hex or L-suffixed spelling.
            assertEquals(BigInteger.valueOf(asLong), Numbers.convert(value, BigInteger.class), text);
            assertEquals(asLong.longValue(), Numbers.toLong(value), text);

            if (asLong.longValue() >= Integer.MIN_VALUE && asLong.longValue() <= Integer.MAX_VALUE) {
                assertEquals(asLong.intValue(), Numbers.convert(value, Integer.class).intValue(), text);
            } else {
                assertThrows(ArithmeticException.class, () -> Numbers.convert(value, Integer.class), text);
            }
        }
    }

    @Test
    @DisplayName("B1: beyond the long range the BigInteger target is still the exact double, not its spelling")
    public void test_B1_beyondLongRangeIsExact() {
        for (final double d : new double[] { 1e30d, -1e30d, 0x1p63, 1.7976931348623157E308, 1e19d }) {
            for (final String text : UNUSABLE_TEXTS) {
                final OpaqueNumber source = new OpaqueNumber(d, text);
                final String why = d + " / " + text;

                assertEquals(new BigDecimal(d).toBigInteger(), Numbers.convert(source, BigInteger.class), why);
                assertEquals(Numbers.convert(Double.valueOf(d), BigInteger.class), Numbers.convert(source, BigInteger.class), why);
                assertThrows(ArithmeticException.class, () -> Numbers.convert(source, Long.class), why);
            }
        }

        // Named: the double 1e30 is not 10^30.
        assertEquals(new BigInteger("1000000000000000019884624838656"), Numbers.convert(new OpaqueNumber(1e30d, "n/a"), BigInteger.class));
    }

    @Test
    public void c001_bigDecimalSubclassIsConvertedExactlyOnEveryOverload() {
        final BigDecimal expected = new BigDecimal("1.50");

        assertSame(BD_SUB, Numbers.convert(BD_SUB, BigDecimal.class));
        assertEquals(expected, Numbers.convert(BD_SUB, BigDecimal.class)); // scale 2 preserved
        assertEquals(BigInteger.ONE, Numbers.convert(BD_SUB, BigInteger.class));
        assertEquals(1L, Numbers.convert(BD_SUB, Long.class));
        assertEquals(1.5f, Numbers.convert(BD_SUB, Float.class));

        assertSame(BD_SUB, Numbers.convert(BD_SUB, BigDecimal.class, BigDecimal.ZERO));
        assertSame(BD_SUB, Numbers.convert(BD_SUB, Type.of(BigDecimal.class)));
        assertSame(BD_SUB, Numbers.convert(BD_SUB, Type.of(BigDecimal.class), BigDecimal.ZERO));
        assertEquals(BigInteger.ONE, Numbers.convert(BD_SUB, Type.of(BigInteger.class)));
    }

    @Test
    @DisplayName("B1: the BigDecimal target keeps its own rule -- the canonical decimal spelling of the fallback double")
    public void test_B1_bigDecimalTargetStillUsesTheCanonicalSpelling() {
        for (final double d : new double[] { 1000000000000000128.0d, 4611686018427388928.0d, 12.9d, 1e30d, 0.1d, -0.0d }) {
            for (final String text : UNUSABLE_TEXTS) {
                final OpaqueNumber source = new OpaqueNumber(d, text);
                final String why = d + " / " + text;

                assertEquals(BigDecimal.valueOf(d), Numbers.convert(source, BigDecimal.class), why);
                assertEquals(Numbers.convert(Double.valueOf(d), BigDecimal.class), Numbers.convert(source, BigDecimal.class), why);
            }
        }

        // The split is deliberate and documented: above 2^53, spelling.toBigInteger() != exact truncation.
        final OpaqueNumber source = new OpaqueNumber(4611686018427388928.0d, "n/a");
        assertFalse(Numbers.convert(source, BigDecimal.class).toBigInteger().equals(Numbers.convert(source, BigInteger.class)));
    }

    // C-001
    @Test
    public void c001_bigIntegerSubclassIsConvertedExactly() {
        assertEquals(BI, Numbers.convert(BI_SUB, BigInteger.class));
        assertSame(BI_SUB, Numbers.convert(BI_SUB, BigInteger.class));
        assertEquals(0, new BigDecimal(BI).compareTo(Numbers.convert(BI_SUB, BigDecimal.class)));
        assertThrows(ArithmeticException.class, () -> Numbers.convert(BI_SUB, Long.class));
        assertEquals(1.2345678901234567E19, Numbers.convert(BI_SUB, Double.class));
    }

    @Test
    public void testBoundedConversion_HugeBigDecimal() {
        final BigDecimal huge = new BigDecimal("1e2147483647");
        assertTimeout(java.time.Duration.ofSeconds(2), () -> {
            assertThrows(ArithmeticException.class, () -> Numbers.convert(huge, int.class));
            assertThrows(ArithmeticException.class, () -> Numbers.toLong(huge, 0L));
        });
        assertEquals("byte overflow: " + huge, assertThrows(ArithmeticException.class, () -> Numbers.convert(huge, byte.class)).getMessage());
        assertEquals(0L, Numbers.toLong(new BigDecimal("1e-1000000"), 0L));
    }

    // D1: the table's Float/Double -> BigInteger entries share the helper (regression coverage)
    @Test
    @DisplayName("D1: Float and Double sources still convert to BigInteger by exact truncation")
    public void test_D1_builtInSourcesStillExact() {
        assertEquals(new BigInteger("2147483648"), Numbers.convert((float) Integer.MAX_VALUE, BigInteger.class));
        assertEquals(new BigInteger("33554448"), Numbers.convert(33554448f, BigInteger.class));
        assertEquals(new BigInteger("-33554448"), Numbers.convert(-33554448f, BigInteger.class));
        assertEquals(new BigInteger("1000000000000000128"), Numbers.convert(1000000000000000128.0d, BigInteger.class));
        assertEquals(new BigInteger("-9223372036854775808"), Numbers.convert(-0x1p63, BigInteger.class));

        for (final Number belowOne : new Number[] { 0.5f, -0.5f, -0.0f, 0.0f, Float.MIN_VALUE, 0.5d, -0.999d, -0.0d, Double.MIN_VALUE }) {
            assertSame(BigInteger.ZERO, Numbers.convert(belowOne, BigInteger.class), String.valueOf(belowOne));
        }

        for (final Number nonFinite : new Number[] { Float.NaN, Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY, Double.NaN, Double.POSITIVE_INFINITY,
                Double.NEGATIVE_INFINITY }) {
            final ArithmeticException e = assertThrows(ArithmeticException.class, () -> Numbers.convert(nonFinite, BigInteger.class),
                    String.valueOf(nonFinite));
            assertEquals("BigInteger overflow: " + nonFinite, e.getMessage());
        }
    }

    @Test
    @DisplayName("C-001: the built-in targets keep their documented doubleValue() fallback for a null toString()")
    public void test_C001_builtInTargetsUnchanged() {
        final NullTextNumber source = new NullTextNumber(42.7d);

        assertEquals((byte) 42, Numbers.convert(source, Byte.class));
        assertEquals((short) 42, Numbers.convert(source, Short.class));
        assertEquals(42, Numbers.convert(source, Integer.class));
        assertEquals(42L, Numbers.convert(source, Long.class));
        assertEquals(BigInteger.valueOf(42), Numbers.convert(source, BigInteger.class));
        assertEquals(BigDecimal.valueOf(42.7d), Numbers.convert(source, BigDecimal.class));
        assertEquals(42.7d, Numbers.convert(source, Double.class));
        assertEquals(42.7f, Numbers.convert(source, Float.class));
        assertEquals(42L, Numbers.toLong(source));
        assertEquals(42, Numbers.toInt(source));

        assertThrows(ArithmeticException.class, () -> Numbers.convert(new NullTextNumber(Double.NaN), Long.class));
        assertThrows(ArithmeticException.class, () -> Numbers.convert(new NullTextNumber(1e30d), Long.class));
    }

    @Test
    public void test_convert_agreesWithTheToStarFamilyForEveryBuiltInTarget() {
        final DoubleAdder fractional = new DoubleAdder();
        fractional.add(12.9d);
        final Number[] sources = { Byte.valueOf((byte) 12), Short.valueOf((short) 12), Integer.valueOf(12), Long.valueOf(12L), Float.valueOf(12.9f),
                Double.valueOf(12.9d), BigInteger.valueOf(12), new BigDecimal("12.9"), new AtomicInteger(12), fractional, new LoudNumber("12.9 ms", 12.9d) };

        for (final Number source : sources) {
            final String label = source.getClass().getSimpleName() + "(" + source + ")";
            assertEquals(Numbers.toByte(source), Numbers.convert(source, Byte.class).byteValue(), label);
            assertEquals(Numbers.toShort(source), Numbers.convert(source, Short.class).shortValue(), label);
            assertEquals(Numbers.toInt(source), Numbers.convert(source, Integer.class).intValue(), label);
            assertEquals(Numbers.toLong(source), Numbers.convert(source, Long.class).longValue(), label);
            assertEquals(Numbers.toFloat(source), Numbers.convert(source, Float.class).floatValue(), 0.0f, label);
            assertEquals(Numbers.toDouble(source), Numbers.convert(source, Double.class).doubleValue(), 0.0d, label);
        }
    }

    @Test
    public void test_convert_aTargetOutsideTheBuiltInSetStillUsesItsStringParser() {
        // The string route is still what an unknown TARGET gets, and an unusable one is still an IAE.
        assertEquals(5, Numbers.convert(Integer.valueOf(5), AtomicInteger.class).intValue());
        assertThrows(IllegalArgumentException.class, () -> Numbers.convert(Integer.valueOf(5), Number.class));
        assertThrows(IllegalArgumentException.class, () -> Numbers.convert(Integer.valueOf(5), (Class<Integer>) null));
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

    // B1 follow-up: the same sub-one short-circuit for a BigDecimal source. Before it, the BigInteger
    // target disagreed with every other integral target on a value smaller than one.
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

    // J3: convert(...) can also throw ArithmeticException when an arbitrary-precision RESULT exceeds the
    // JDK's supported BigInteger magnitude. The @throws tags named only overflow and NaN/Infinity.
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

    // B1 follow-up: the BigInteger target short-circuits |x| < 1 to ZERO instead of building the exact
    // BigDecimal, which for a subnormal is ~750 significant digits. Must be value-identical.
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

    // C-002: every built-in numeric target obeys the built-in rule, whatever the source's exact class
    @Test
    public void test_convert_bigTargetsNarrowAnUnknownNumberByTheDocumentedRule() {
        final DoubleAdder fractional = new DoubleAdder();
        fractional.add(12.9d);
        final DoubleAdder negative = new DoubleAdder();
        negative.add(-12.9d);

        // "an integral target (byte, short, int, long, or BigInteger) truncates toward zero"
        assertEquals(BigInteger.valueOf(12), Numbers.convert(fractional, BigInteger.class));
        assertEquals(BigInteger.valueOf(-12), Numbers.convert(negative, BigInteger.class));
        assertEquals(0, new BigDecimal("12.9").compareTo(Numbers.convert(fractional, BigDecimal.class)));

        // ... exactly as a directly supported source of the same value does
        assertEquals(Numbers.convert(12.9d, BigInteger.class), Numbers.convert(fractional, BigInteger.class));
        assertEquals(0, Numbers.convert(12.9d, BigDecimal.class).compareTo(Numbers.convert(fractional, BigDecimal.class)));

        // an integer-valued unknown source is unaffected
        assertEquals(BigInteger.valueOf(1234), Numbers.convert(new AtomicInteger(1234), BigInteger.class));
        assertEquals(0, BigDecimal.valueOf(1234).compareTo(Numbers.convert(new AtomicInteger(1234), BigDecimal.class)));
    }

    @Test
    public void test_convert_bigTargetsRejectANonFiniteUnknownNumberWithArithmeticException() {
        for (final double nonFinite : new double[] { Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY }) {
            final DoubleAdder adder = new DoubleAdder();
            adder.add(nonFinite);

            assertThrows(ArithmeticException.class, () -> Numbers.convert(adder, BigInteger.class), String.valueOf(nonFinite));
            assertThrows(ArithmeticException.class, () -> Numbers.convert(adder, BigDecimal.class), String.valueOf(nonFinite));

            // the same answer the directly supported Double source gives
            assertThrows(ArithmeticException.class, () -> Numbers.convert(nonFinite, BigInteger.class), String.valueOf(nonFinite));
        }
    }

    @Test
    public void testConvert_BuiltInTargets() {
        final Byte val = (byte) 10;
        assertEquals(Byte.valueOf((byte) 10), Numbers.convert(val, byte.class));
        assertEquals(Short.valueOf((short) 10), Numbers.convert(val, short.class));
        assertEquals(Integer.valueOf(10), Numbers.convert(val, int.class));
        assertEquals(Long.valueOf(10L), Numbers.convert(val, long.class));
        assertEquals(10.0f, Numbers.convert(val, float.class), DELTA);
        assertEquals(10.0d, Numbers.convert(val, double.class), DELTA);
        assertEquals(BigInteger.valueOf(10), Numbers.convert(val, BigInteger.class));
        assertEquals(BigDecimal.valueOf(10), Numbers.convert(val, BigDecimal.class));

        assertEquals(Byte.valueOf((byte) 100), Numbers.convert(100, byte.class));
        assertEquals(Integer.valueOf(12345), Numbers.convert(12345L, int.class));
        assertEquals(Long.valueOf(123L), Numbers.convert(123.45d, long.class));
        assertEquals(Integer.valueOf(123), Numbers.convert(new BigDecimal("123.789"), Integer.class));
        assertEquals(Integer.valueOf(42), Numbers.convert(42, Integer.class));
    }

    @Test
    public void test_convert_floatAndDoubleTargetsAgreeWithToFloatAndToDouble() {
        // A subtype whose toString() is formatted text: the string route cannot parse it, but the
        // documented contract says convert is consistent with toFloat(Object)/toDouble(Object).
        final LoudNumber formatted = new LoudNumber("12.9 ms", 12.9d);

        assertEquals(Numbers.toFloat(formatted), Numbers.convert(formatted, Float.class), 0.0f);
        assertEquals(Numbers.toDouble(formatted), Numbers.convert(formatted, Double.class), 0.0d);
        assertEquals(12.9f, Numbers.convert(formatted, Float.class), 0.0f);
        assertEquals(12.9d, Numbers.convert(formatted, Double.class), 0.0d);

        // integral targets already used doubleValue() as their last resort; they still do
        assertEquals(Integer.valueOf(12), Numbers.convert(formatted, Integer.class));
        assertEquals(BigInteger.valueOf(12), Numbers.convert(formatted, BigInteger.class));

        // NaN/Infinity are preserved by a float/double target
        final LoudNumber nan = new LoudNumber("not a number", Double.NaN);
        assertTrue(Float.isNaN(Numbers.convert(nan, Float.class)));
        assertTrue(Double.isNaN(Numbers.convert(nan, Double.class)));
    }

    @Test
    public void testConvert_FloatDoubleSpelling() {
        for (final double d : new double[] { 1.1d, 1.21d, 123.45d, -0.1d, 1e-40d, 1e300d, Double.MIN_VALUE }) {
            assertEquals((float) d, Numbers.convert(d, Float.class), 0.0f);
            assertEquals((float) d, Numbers.toFloat(d), 0.0f);
        }
        final float source = 1.21f;
        final double canonicalDecimal = Double.parseDouble(Float.toString(source));
        assertNotEquals(Double.doubleToRawLongBits(source), Double.doubleToRawLongBits(canonicalDecimal));
        assertEquals(canonicalDecimal, Numbers.convert(source, Double.class), 0.0d);

        final float floatValue = 1e20f;
        assertEquals(new BigInteger("100000002004087734272"), Numbers.convert(floatValue, BigInteger.class));
        assertEquals(new BigDecimal("1.0E+20"), Numbers.convert(floatValue, BigDecimal.class));
        assertNotEquals(Numbers.convert(floatValue, BigDecimal.class).toBigInteger(), Numbers.convert(floatValue, BigInteger.class));
        assertEquals(new BigDecimal("1.21"), Numbers.convert(1.21f, BigDecimal.class));
    }

    @Test
    public void testConvert_NaNAndInfinity() {
        assertEquals(Float.NaN, Numbers.convert(Double.NaN, float.class));
        assertEquals(Float.POSITIVE_INFINITY, Numbers.convert(Double.POSITIVE_INFINITY, float.class));
        assertEquals(Float.NEGATIVE_INFINITY, Numbers.convert(Double.NEGATIVE_INFINITY, float.class));
        assertEquals(Double.NaN, Numbers.convert(Float.NaN, double.class));
        assertTrue(Numbers.convert(Double.NaN, float.class).isNaN());
        assertThrows(ArithmeticException.class, () -> Numbers.convert(Double.NaN, BigInteger.class));
        assertThrows(ArithmeticException.class, () -> Numbers.convert(Double.NaN, BigDecimal.class));
        assertThrows(ArithmeticException.class, () -> Numbers.convert(Double.POSITIVE_INFINITY, BigInteger.class));
        assertThrows(ArithmeticException.class, () -> Numbers.convert(Float.NaN, Integer.class));
        assertThrows(ArithmeticException.class, () -> Numbers.convert(Double.NaN, Type.of(BigInteger.class)));
        assertEquals(Float.valueOf(Float.NaN), Numbers.convert(Double.NaN, Float.class));
        assertEquals(Float.valueOf(Float.POSITIVE_INFINITY), Numbers.convert(1e300d, Float.class));
    }

    @Test
    public void testConvert_Narrowing() {
        assertEquals(Byte.valueOf((byte) 127), Numbers.convert(Float.valueOf(127.9f), Byte.class));
        assertEquals(Byte.valueOf((byte) 127), Numbers.convert(new BigDecimal("127.9"), Byte.class));
        assertEquals(Integer.valueOf(0), Numbers.convert(Double.valueOf(-0.9d), Integer.class));
        assertEquals(Long.valueOf(-5L), Numbers.convert(Byte.valueOf((byte) -5), Long.class));
        assertThrows(ArithmeticException.class, () -> Numbers.convert(Float.valueOf(Integer.MAX_VALUE), Integer.class));
        assertEquals(Integer.valueOf(Integer.MIN_VALUE), Numbers.convert(Float.valueOf(Integer.MIN_VALUE), Integer.class));

        final Number[] sources = { Integer.valueOf(1000), Long.valueOf(1000L), Float.valueOf(1000.9f), Double.valueOf(1000.9d), BigInteger.valueOf(1000),
                new BigDecimal("1000.9") };
        for (final Number source : sources) {
            assertThrows(ArithmeticException.class, () -> Numbers.convert(source, Byte.class), source.getClass().getSimpleName());
            assertEquals(Short.valueOf((short) 1000), Numbers.convert(source, Short.class));
            assertEquals(1000, Numbers.toInt(source));
        }
    }

    @Test
    public void testConvert_NullAndDefault() {
        assertNull(Numbers.convert(null, Integer.class));
        assertEquals(Byte.valueOf((byte) 0), Numbers.convert(null, byte.class));
        assertEquals(Integer.valueOf(0), Numbers.convert(null, int.class));
        assertEquals(Byte.valueOf((byte) 5), Numbers.convert(null, byte.class, (byte) 5));
        assertEquals(Integer.valueOf(100), Numbers.convert(null, Integer.class, 100));
        assertNull(Numbers.convert(null, Type.of(Integer.class)));
        assertEquals(Long.valueOf(50L), Numbers.convert(null, Type.of(Long.class), 50L));
        assertEquals(Integer.valueOf(0), Numbers.convert(null, Type.of(int.class)));
    }

    @Test
    public void test_convert_nullAndDefaultsAreUnchanged() {
        assertNull(Numbers.convert(null, Integer.class));
        assertEquals(Integer.valueOf(0), Numbers.convert(null, int.class));
        assertEquals(Integer.valueOf(-1), Numbers.convert(null, Integer.class, -1));
        assertSame(BigInteger.TEN, Numbers.convert(null, BigInteger.class, BigInteger.TEN));
    }

    @Test
    public void testConvert_Overflow() {
        assertThrows(ArithmeticException.class, () -> Numbers.convert(Long.MAX_VALUE + 100.0, long.class));
        assertThrows(ArithmeticException.class, () -> Numbers.convert((double) Long.MAX_VALUE, long.class));
        assertThrows(ArithmeticException.class, () -> Numbers.convert((float) Integer.MAX_VALUE, int.class));
        assertEquals(Long.MIN_VALUE, Numbers.convert((double) Long.MIN_VALUE, long.class).longValue());
        assertEquals(Integer.MIN_VALUE, Numbers.convert((float) Integer.MIN_VALUE, int.class).intValue());
        assertThrows(ArithmeticException.class, () -> Numbers.convert(BigInteger.valueOf(3000000000L), int.class));
        assertThrows(ArithmeticException.class, () -> Numbers.convert(new BigDecimal("100000000000000000000"), long.class));
        assertThrows(ArithmeticException.class, () -> Numbers.convert((short) 300, byte.class));
        assertEquals("int overflow: 1.0E300", assertThrows(ArithmeticException.class, () -> Numbers.convert(1e300, Integer.class)).getMessage());
        assertEquals(assertThrows(ArithmeticException.class, () -> Numbers.toInt(1e300)).getMessage(),
                assertThrows(ArithmeticException.class, () -> Numbers.convert(1e300, Integer.class)).getMessage());
    }

    @Test
    public void testConvert_TypeAgreesWithClass() {
        assertThrows(ArithmeticException.class, () -> Numbers.convert(Double.NaN, BigDecimal.class));
        assertThrows(ArithmeticException.class, () -> Numbers.convert(Double.NaN, Type.of(BigDecimal.class)));
        assertThrows(ArithmeticException.class, () -> Numbers.convert(Float.valueOf(Integer.MAX_VALUE), Integer.class));
        assertThrows(ArithmeticException.class, () -> Numbers.convert(Float.valueOf(Integer.MAX_VALUE), Type.of(Integer.class)));
        assertTrue(Double.isNaN(Numbers.convert(Double.NaN, Double.class)));
        assertTrue(Double.isNaN(Numbers.convert(Double.NaN, Type.of(Double.class))));
    }

    @Test
    public void testConvert_UnknownSourceAndUnsupportedTarget() {
        final DoubleAdder fractional = new DoubleAdder();
        fractional.add(12.9);
        assertEquals(Integer.valueOf(12), Numbers.convert(fractional, Integer.class));
        assertEquals(Byte.valueOf((byte) 12), Numbers.convert(fractional, Byte.class));
        assertThrows(ArithmeticException.class, () -> Numbers.convert(new AtomicInteger(1234), Byte.class));
        assertEquals(BigInteger.valueOf(1234), Numbers.convert(new AtomicInteger(1234), BigInteger.class));

        final IllegalArgumentException fromClass = assertThrows(IllegalArgumentException.class, () -> Numbers.convert(Integer.valueOf(5), Number.class));
        assertTrue(fromClass.getMessage().contains("Unsupported target type"), fromClass.getMessage());
        assertNotNull(fromClass.getCause());
        assertThrows(IllegalArgumentException.class, () -> Numbers.convert(Integer.valueOf(5), (Class<Integer>) null));
        assertNull(Numbers.convert(null, Number.class));
        assertEquals(5, Numbers.convert(Integer.valueOf(5), AtomicInteger.class).intValue());

        final Type<Integer> intType = Type.of(Integer.class);
        assertEquals(Integer.valueOf(100), Numbers.convert(100L, intType));
        assertEquals(Integer.valueOf(100), Numbers.convert(100L, Type.of(int.class)));
    }

    @Test
    public void c086_doubleTargetConvertersFromBigNumbers() {
        assertEquals(Double.valueOf(0x1p70), Numbers.convert(BigInteger.TWO.pow(70), Double.class));
        assertEquals(Double.valueOf(1.5), Numbers.convert(new BigDecimal("1.5"), Double.class));
        assertEquals(Double.valueOf(123.0), Numbers.convert(BigInteger.valueOf(123), double.class));
        assertEquals(Double.valueOf(-0.1), Numbers.convert(new BigDecimal("-0.1"), double.class));
        assertEquals(Double.valueOf(Double.POSITIVE_INFINITY), Numbers.convert(new BigDecimal("1e400"), Double.class));
        assertEquals(Double.valueOf(Double.NEGATIVE_INFINITY), Numbers.convert(new BigDecimal("-1e400"), double.class));
        assertEquals(Double.valueOf(Double.POSITIVE_INFINITY), Numbers.convert(BigInteger.TWO.pow(2000), Double.class));
        assertEquals(Double.valueOf(Double.NEGATIVE_INFINITY), Numbers.convert(BigInteger.TWO.pow(2000).negate(), double.class));
        assertEquals(Double.valueOf(0.0), Numbers.convert(new BigDecimal("1e-400"), Double.class));
        assertInstanceOf(Double.class, Numbers.convert(BigInteger.ONE, Double.class));
        assertInstanceOf(Double.class, Numbers.convert(BigDecimal.ONE, double.class));
    }

    // C-006
    @Test
    public void c006_everyBigIntegerMagnitudeOverflowUsesTheClassIdiom() {
        for (final String text : new String[] { "1e2147483648", "-1e2147483648", "5e2147483648", "1e2147483647", "1.2e2147483648" }) {
            final ArithmeticException e = assertThrows(ArithmeticException.class, () -> Numbers.convert(new BigDecimal(text), BigInteger.class), text);
            assertTrue(e.getMessage().startsWith("BigInteger overflow: "), e.getMessage());
            assertNotNull(e.getCause(), text);
        }

        final Number unknown = new Number() {
            @Override
            public int intValue() {
                return 0;
            }

            @Override
            public long longValue() {
                return 0;
            }

            @Override
            public float floatValue() {
                return Float.POSITIVE_INFINITY;
            }

            @Override
            public double doubleValue() {
                return Double.POSITIVE_INFINITY;
            }

            @Override
            public String toString() {
                return "1e2147483648";
            }
        };
        final ArithmeticException e = assertThrows(ArithmeticException.class, () -> Numbers.convert(unknown, BigInteger.class));
        assertTrue(e.getMessage().startsWith("BigInteger overflow: "), e.getMessage());
    }

    @Test
    public void c001_exactClassSourcesAreUnchanged() {
        assertSame(BI, Numbers.convert(BI, BigInteger.class));
        assertEquals(new BigDecimal("1.50"), Numbers.convert(new BigDecimal("1.50"), BigDecimal.class));
        assertEquals(Integer.valueOf(12), Numbers.convert(12.9d, Integer.class));
        assertNull(Numbers.convert((Number) null, BigInteger.class));
        assertEquals(BigInteger.TEN, Numbers.convert((Number) null, BigInteger.class, BigInteger.TEN));
    }

    @Test
    public void c059_floatTargetConvertersFromBigNumbers() {
        assertEquals(Float.valueOf(1.2345679E19f), Numbers.convert(new BigInteger("12345678901234567890"), Float.class));
        assertEquals(Float.valueOf(1.5f), Numbers.convert(new BigDecimal("1.5"), Float.class));
        assertEquals(Float.valueOf(1.5f), Numbers.convert(new BigDecimal("1.5"), float.class));
        assertEquals(Float.valueOf(Float.POSITIVE_INFINITY), Numbers.convert(new BigDecimal("1e50"), Float.class));
        assertEquals(Float.valueOf(Float.NEGATIVE_INFINITY), Numbers.convert(BigInteger.TEN.pow(50).negate(), Float.class));
        assertEquals(Float.valueOf(0.0f), Numbers.convert(new BigDecimal("1e-50"), Float.class));
        assertInstanceOf(Float.class, Numbers.convert(BigInteger.ONE, Float.class));
        assertInstanceOf(Float.class, Numbers.convert(BigDecimal.ONE, float.class));
    }

    @Test
    @DisplayName("B2: fuzz -- convert(x, Long) and convert(x, BigInteger) never disagree")
    public void test_B2_fuzzTargetsNeverDisagree() {
        final Random rnd = new Random(20260830L);
        final char[] alphabet = "0123456789+-.#xXlLeEabcdefABCDEF".toCharArray();
        final StringBuilder sb = new StringBuilder();
        int compared = 0;

        for (int i = 0; i < 200_000; i++) {
            sb.setLength(0);
            final int len = 1 + rnd.nextInt(8);

            for (int j = 0; j < len; j++) {
                sb.append(alphabet[rnd.nextInt(alphabet.length)]);
            }

            final SplitNumber value = n(sb.toString());
            final Long asLong;

            try {
                asLong = Numbers.convert(value, Long.class);
            } catch (final ArithmeticException e) {
                continue;
            }

            compared++;
            assertEquals(BigInteger.valueOf(asLong), Numbers.convert(value, BigInteger.class), sb.toString());
        }

        assertTrue(compared > 100_000, "expected the fuzz to actually compare something, got " + compared);
    }

    // C-033
    @Test
    public void c033_integralSourceWiderThanTheSignificandRoundsToNearest() {
        assertEquals(1.6777216E7f, Numbers.convert(16777217, Float.class));
        assertEquals(1.6777216E7f, Numbers.convert(16777217L, Float.class));
        assertEquals(9.223372036854776E18, Numbers.convert(Long.MAX_VALUE, Double.class));
        assertEquals(9.007199254740992E15, Numbers.convert(9007199254740993L, Double.class));
    }

    @Test
    @DisplayName("B1: below one the fallback truncates to ZERO without building the exact expansion; -0.0 included")
    public void test_B1_magnitudeBelowOneIsZero() {
        for (final double d : new double[] { 0.0d, -0.0d, 0.5d, -0.999999d, Double.MIN_VALUE, -Double.MIN_VALUE, Math.nextDown(1.0d), -Math.nextDown(1.0d) }) {
            for (final String text : UNUSABLE_TEXTS) {
                final OpaqueNumber source = new OpaqueNumber(d, text);
                assertSame(BigInteger.ZERO, Numbers.convert(source, BigInteger.class), d + " / " + text);
                assertEquals(0L, Numbers.convert(source, Long.class), d + " / " + text);
            }
        }

        // Exactly one is not below one.
        assertEquals(BigInteger.ONE, Numbers.convert(new OpaqueNumber(1.0d, "n/a"), BigInteger.class));
        assertEquals(BigInteger.ONE.negate(), Numbers.convert(new OpaqueNumber(-1.0d, "n/a"), BigInteger.class));
    }

    @Test
    @DisplayName("C-012: the message names the target and previews the text, on both convert entry points")
    public void test_C012_messageIsBoundedAndNamesTheTarget() {
        final NumberFormatException byClass = assertThrows(NumberFormatException.class, () -> Numbers.convert(12.9, AtomicInteger.class));
        assertEquals("12.9 is not a valid AtomicInteger.", byClass.getMessage());
        assertTrue(byClass.getCause() instanceof NumberFormatException, String.valueOf(byClass.getCause()));
        assertTrue(byClass.getCause().getMessage().contains("12.9"), byClass.getCause().getMessage());

        final NumberFormatException byType = assertThrows(NumberFormatException.class, () -> Numbers.convert(12.9, Type.of(AtomicInteger.class)));
        assertEquals("12.9 is not a valid AtomicInteger.", byType.getMessage());

        // A 100 KB text with an embedded line break: previewed, escaped, never echoed.
        final LoudNumber loud = new LoudNumber("1.\n" + "5".repeat(100_000));
        final NumberFormatException huge = assertThrows(NumberFormatException.class, () -> Numbers.convert(loud, AtomicInteger.class));
        assertTrue(huge.getMessage().length() < 300, String.valueOf(huge.getMessage().length()));
        assertTrue(huge.getMessage().contains("...[100003 chars]"), huge.getMessage());
        assertTrue(huge.getMessage().contains("\\u000A"), huge.getMessage());
        assertFalse(huge.getMessage().contains("\n"), "raw line break in message");
        assertTrue(huge.getMessage().endsWith(" is not a valid AtomicInteger."), huge.getMessage());
        // The retained cause is bounded too (the JDK's own message embedded the whole text).
        assertTrue(huge.getCause().getMessage().length() < 300, String.valueOf(huge.getCause().getMessage().length()));
        assertFalse(huge.getCause().getMessage().contains("\n"), "raw line break in cause");
    }

    @Test
    @DisplayName("B1: at an exact float midpoint the result is the even significand, not whichever side the decimal spelling fell on")
    public void test_B1_midpointsRoundHalfToEven() {
        // 1 + 2^-24 lies exactly between 1.0f and 1.0000001f. Its shortest decimal spelling is slightly
        // above the midpoint, so the old Float.parseFloat(Double.toString(d)) route answered 1.0000001f
        // (odd significand); IEEE-754 ties-to-even answers 1.0f.
        final double m = midpointAbove(1.0f);
        assertEquals(1.0d + 0x1p-24, m, 0.0d);
        assertEquals(1.0000001f, Float.parseFloat(Double.toString(m)), 0.0f, "precondition: the decimal route breaks the tie upward");

        assertEquals(1.0f, Numbers.toFloat(m), 0.0f);
        assertEquals(1.0f, Numbers.convert(m, Float.class), 0.0f);
        assertEquals(1.0f, Numbers.convert(m, float.class), 0.0f);

        // Every midpoint, in both directions: the even significand wins, whatever the spelling does.
        final Random rnd = new Random(20260902L);
        int checked = 0, spellingDisagreed = 0;

        while (checked < 20_000) {
            final float f = Float.intBitsToFloat(rnd.nextInt());

            if (!Float.isFinite(f) || Math.abs(f) > 1e30f || Math.abs(f) < 1e-30f) {
                continue;
            }

            final double mid = midpointAbove(f);
            final float expected = (float) mid; // ties to even by JLS 5.1.3
            assertEquals(0, Float.floatToIntBits(expected) & 1, "the cast picks the even significand");
            assertEquals(Float.floatToIntBits(expected), Float.floatToIntBits(Numbers.toFloat(mid)), "toFloat " + mid);
            assertEquals(Float.floatToIntBits(expected), Float.floatToIntBits(Numbers.convert(mid, Float.class)), "convert " + mid);

            if (Float.floatToIntBits(expected) != Float.floatToIntBits(Float.parseFloat(Double.toString(mid)))) {
                spellingDisagreed++;
            }

            checked++;
        }

        assertTrue(spellingDisagreed > 1000, "the decimal route must actually disagree on a large share of midpoints, saw " + spellingDisagreed);
    }

    @Test
    @DisplayName("B1: a non-finite fallback is rejected as BigInteger overflow, with the bounded description")
    public void test_B1_nonFiniteFallbackIsOverflow() {
        for (final double d : new double[] { Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY }) {
            final ArithmeticException named = assertThrows(ArithmeticException.class, () -> Numbers.convert(new OpaqueNumber(d, "n/a"), BigInteger.class));
            assertEquals("BigInteger overflow: n/a", named.getMessage());

            final ArithmeticException nullText = assertThrows(ArithmeticException.class, () -> Numbers.convert(new OpaqueNumber(d, null), BigInteger.class));
            assertTrue(nullText.getMessage().startsWith("BigInteger overflow: "), nullText.getMessage());
            assertTrue(nullText.getMessage().endsWith("(toString() returned null)"), nullText.getMessage());

            // A huge toString() is previewed, never embedded (the message stays bounded and the text is read once).
            final ArithmeticException huge = assertThrows(ArithmeticException.class,
                    () -> Numbers.convert(new OpaqueNumber(d, "Q".repeat(100_000)), BigInteger.class));
            assertTrue(huge.getMessage().length() < 300, String.valueOf(huge.getMessage().length()));
            assertTrue(huge.getMessage().contains("...[100000 chars]"), huge.getMessage());
        }
    }

    // B2 - a Number whose toString() returns null.
    @Test
    @DisplayName("B2: a null toString() falls back to doubleValue() instead of raising NullPointerException")
    public void test_B2_nullToStringCoercesViaDoubleValue() {
        final NullTextNumber value = new NullTextNumber(12.9);

        assertEquals((byte) 12, Numbers.toByte(value));
        assertEquals((short) 12, Numbers.toShort(value));
        assertEquals(12, Numbers.toInt(value));
        assertEquals(12L, Numbers.toLong(value));
        assertEquals(12.9f, Numbers.toFloat(value));
        assertEquals(12.9d, Numbers.toDouble(value));

        assertEquals(Byte.valueOf((byte) 12), Numbers.convert(value, Byte.class));
        assertEquals(Integer.valueOf(12), Numbers.convert(value, Integer.class));
        assertEquals(Long.valueOf(12L), Numbers.convert(value, Long.class));
        assertEquals(Float.valueOf(12.9f), Numbers.convert(value, Float.class));
        assertEquals(Double.valueOf(12.9d), Numbers.convert(value, Double.class));
        assertEquals(BigInteger.valueOf(12), Numbers.convert(value, BigInteger.class));
        assertEquals(BigDecimal.valueOf(12.9d), Numbers.convert(value, BigDecimal.class));
    }

    @Test
    @DisplayName("B2: a null toString() on an unrepresentable value reports overflow, naming the type")
    public void test_B2_nullToStringOverflowNamesTheTypeInsteadOfThrowingNpe() {
        final NullTextNumber notFinite = new NullTextNumber(Double.NaN);
        final NullTextNumber outOfRange = new NullTextNumber(1e30d);

        for (final Number source : new Number[] { notFinite, outOfRange }) {
            final ArithmeticException e = assertThrows(ArithmeticException.class, () -> Numbers.toInt(source));
            assertTrue(e.getMessage().startsWith("int overflow: "), e.getMessage());
            assertTrue(e.getMessage().contains(NullTextNumber.class.getName()), e.getMessage());
            assertTrue(e.getMessage().endsWith("(toString() returned null)"), e.getMessage());
        }

        // The arbitrary-precision targets reject a non-finite value the same way.
        final ArithmeticException big = assertThrows(ArithmeticException.class, () -> Numbers.convert(notFinite, BigDecimal.class));
        assertTrue(big.getMessage().startsWith("BigDecimal overflow: "), big.getMessage());
        assertThrows(ArithmeticException.class, () -> Numbers.convert(notFinite, BigInteger.class));

        // ... and a finite out-of-range one still converts where the target is wide enough. With no text to
        // recover, the value comes from doubleValue(), and each target then applies the rule it applies to a
        // Double source: the BigDecimal target keeps the canonical decimal spelling, the BigInteger target
        // truncates the EXACT value (2026-09-02: it used to truncate the spelling, 10^30, which is not the
        // value of the double 1e30 and differed from what convert(1e30d, BigInteger.class) answers).
        assertEquals(1e30d, Numbers.convert(outOfRange, Double.class));
        assertEquals(BigDecimal.valueOf(1e30d), Numbers.convert(outOfRange, BigDecimal.class));
        assertEquals(new BigDecimal(1e30d).toBigInteger(), Numbers.convert(outOfRange, BigInteger.class));
        assertEquals(new BigInteger("1000000000000000019884624838656"), Numbers.convert(outOfRange, BigInteger.class));
        assertEquals(Numbers.convert(1e30d, BigInteger.class), Numbers.convert(outOfRange, BigInteger.class));
    }

    // C-001: string-routed targets apply the doubleValue() fallback to a null toString()
    @Test
    @DisplayName("C-001: a null-toString source reaching a string-routed target is answered as its doubleValue() would be, never as null")
    public void test_C001_nullToStringSourceIsConvertedAsItsDoubleValue() {
        // 42.0 rendered as a Double is "42.0", which AtomicLong's parser rejects -- exactly what
        // convert(42.0d, AtomicLong.class) does today, so the two can never disagree.
        final NumberFormatException viaNullText = assertThrows(NumberFormatException.class, () -> Numbers.convert(new NullTextNumber(42.0d), AtomicLong.class));
        final NumberFormatException viaDouble = assertThrows(NumberFormatException.class, () -> Numbers.convert(42.0d, AtomicLong.class));
        assertEquals(viaDouble.getMessage(), viaNullText.getMessage());
        assertTrue(viaNullText.getMessage().contains("AtomicLong"), viaNullText.getMessage());

        assertThrows(NumberFormatException.class, () -> Numbers.convert(new NullTextNumber(42.7d), AtomicInteger.class));
        assertThrows(NumberFormatException.class, () -> Numbers.convert(new NullTextNumber(Double.NaN), AtomicLong.class));
        assertThrows(NumberFormatException.class, () -> Numbers.convert(new NullTextNumber(Double.POSITIVE_INFINITY), AtomicLong.class));
        assertThrows(NumberFormatException.class, () -> Numbers.convert(new NullTextNumber(1e20d), AtomicLong.class));
    }

    // B2: a null value takes the default without the target being validated
    @Test
    @DisplayName("B2: convert(null, unusableTarget) is the documented null, convert(1, unusableTarget) still throws")
    public void test_B2_nullValueSkipsTargetValidation() {
        assertNull(Numbers.convert(null, Number.class));
        assertNull(Numbers.convert(null, AtomicInteger.class));
        assertNull(Numbers.convert(null, Type.of(Number.class)));
        assertEquals(Integer.valueOf(-1), Numbers.convert(null, Integer.class, -1));

        assertThrows(IllegalArgumentException.class, () -> Numbers.convert(1, Number.class));
        assertThrows(IllegalArgumentException.class, () -> Numbers.convert(null, (Class<Integer>) null));
    }

    @Test
    @DisplayName("B1: 1.21d narrows to 1.21f, exactly as the cast does - the old rationale never needed a round trip")
    public void test_B1_ordinaryDoublesNarrowLikeTheCast() {
        assertEquals(1.21f, (float) 1.21d, 0.0f, "precondition: the cast alone already gives 1.21f");

        for (final double d : new double[] { 1.21d, 1.1d, 123.45d, 0.1d, -2.5d, 3.141592653589793d, 1e-45d, 1e38d, 1e39d, 1e300d, -1e300d, Double.MIN_VALUE,
                Double.MAX_VALUE, 0.0d, -0.0d }) {
            final float expected = (float) d;
            assertEquals(Float.floatToIntBits(expected), Float.floatToIntBits(Numbers.toFloat(d)), "toFloat(Object) " + d);
            assertEquals(Float.floatToIntBits(expected), Float.floatToIntBits(Numbers.toFloat(d, 7f)), "toFloat(Object, default) " + d);
            assertEquals(Float.floatToIntBits(expected), Float.floatToIntBits(Numbers.convert(d, Float.class)), "convert(Float.class) " + d);
            assertEquals(Float.floatToIntBits(expected), Float.floatToIntBits(Numbers.convert(d, float.class)), "convert(float.class) " + d);
            assertEquals(Float.floatToIntBits(expected), Float.floatToIntBits(Numbers.convert(d, Type.of(Float.class))), "convert(Type) " + d);
        }
    }

    @Test
    @DisplayName("B1: the rejection is still ArithmeticException, with a bounded message naming the value")
    public void test_B1_outOfRangeIntegerTokenStillReportsBoundedArithmeticException() {
        final HugeIntegralTextNumber huge = new HugeIntegralTextNumber(10_000);

        final ArithmeticException e = assertThrows(ArithmeticException.class, () -> Numbers.toInt(huge));
        assertTrue(e.getMessage().startsWith("int overflow: 99999999"), e.getMessage());
        assertTrue(e.getMessage().endsWith("...[10000 chars]"), e.getMessage());
        assertTrue(e.getMessage().length() < 200, e.getMessage());

        // Every integral target reports the same way, through both entry points.
        assertThrows(ArithmeticException.class, () -> Numbers.toByte(huge));
        assertThrows(ArithmeticException.class, () -> Numbers.toShort(huge));
        assertThrows(ArithmeticException.class, () -> Numbers.toLong(huge));
        assertThrows(ArithmeticException.class, () -> Numbers.convert(huge, Byte.class));
        assertThrows(ArithmeticException.class, () -> Numbers.convert(huge, Short.class));
        assertThrows(ArithmeticException.class, () -> Numbers.convert(huge, Integer.class));
        assertThrows(ArithmeticException.class, () -> Numbers.convert(huge, Long.class));
    }

    // C-001: an unknown Number's toString() must be bounded and escaped in the overflow message
    @Test
    public void test_overflowMessage_isBoundedForANumberWithAHugeToString() {
        // A subtype whose toString() is NOT a numeric token: this is the path that reached the raw
        // toString(). A numeric one is re-parsed as a BigDecimal and summarized by the bit-length guard
        // instead (pinned by the next test), so it never exercised the leak.
        final LoudNumber huge = new LoudNumber("Q".repeat(100_000), Double.NaN);

        for (final Executable call : new Executable[] { () -> Numbers.toByte(huge), () -> Numbers.toShort(huge), () -> Numbers.toInt(huge),
                () -> Numbers.toLong(huge), () -> Numbers.convert(huge, Integer.class), () -> Numbers.convert(huge, Long.class),
                () -> Numbers.convert(huge, BigInteger.class), () -> Numbers.convert(huge, BigDecimal.class) }) {
            final ArithmeticException e = assertThrows(ArithmeticException.class, call::run);
            assertBoundedAndEscaped(e);
            assertTrue(e.getMessage().contains("...[100000 chars]"), e.getMessage());
        }
    }

    @Test
    public void c150_primitiveAndWrapperTargetsShareOneConverterEntry() {
        assertSameConverter(byte.class, Byte.class);
        assertSameConverter(short.class, Short.class);
        assertSameConverter(int.class, Integer.class);
        assertSameConverter(long.class, Long.class);
        assertSameConverter(float.class, Float.class);
        assertSameConverter(double.class, Double.class);

        // the aliasing covers the overflow check on both spellings of the target
        assertThrows(ArithmeticException.class, () -> Numbers.convert(1000, byte.class));
        assertThrows(ArithmeticException.class, () -> Numbers.convert(1000, Byte.class));
        assertEquals(Byte.valueOf((byte) 7), Numbers.convert(7.9, byte.class));
    }

    @Test
    @DisplayName("B1: random sweep over (2^53, 2^63), both signs -- Long, BigInteger and Double-source agree")
    public void test_B1_randomSweepAgreesAcrossTargets() {
        final Random random = new Random(20260902L);

        for (int i = 0; i < 20_000; i++) {
            double d = Math.scalb(1.0 + random.nextDouble(), 53 + random.nextInt(10));
            if (random.nextBoolean()) {
                d = -d;
            }

            final OpaqueNumber source = new OpaqueNumber(d, "n/a");
            final BigInteger asBig = Numbers.convert(source, BigInteger.class);

            assertEquals(BigInteger.valueOf(Numbers.convert(source, Long.class)), asBig, String.valueOf(d));
            assertEquals(Numbers.convert(Double.valueOf(d), BigInteger.class), asBig, String.valueOf(d));
        }
    }

    @Test
    @DisplayName("C-001: an integral doubleValue() that the target's parser accepts converts (Integer/Long sources are the reference)")
    public void test_C001_sourceWithUsableTextStillConverts() {
        // A subtype WITH a string form is untouched: its text is what the parser reads.
        final AtomicLong fromText = Numbers.convert(new AtomicInteger(7), AtomicLong.class);
        assertNotNull(fromText);
        assertEquals(7L, fromText.get());
        assertEquals(42L, Numbers.convert(42, AtomicLong.class).get());
        assertEquals(42, Numbers.convert(42L, AtomicInteger.class).get());
    }

    @Test
    public void c148_stringRoutedConversionPreviewsTheRenderingHandedToTheParser() {
        // A source with no string form (null or empty toString) is rendered as its double value; that rendering,
        // never null, is what the failing target parser's message previews, on both overloads.
        for (final String rendering : new String[] { null, "" }) {
            NumberFormatException e = assertThrows(NumberFormatException.class, () -> Numbers.convert(renderedAs(rendering, 1.5), AtomicInteger.class));
            assertEquals("1.5 is not a valid AtomicInteger.", e.getMessage());
            assertEquals("1.5 is not a valid Integer.", e.getCause().getMessage());

            e = assertThrows(NumberFormatException.class, () -> Numbers.convert(renderedAs(rendering, 1.5), CommonUtil.typeOf(AtomicInteger.class)));
            assertEquals("1.5 is not a valid AtomicInteger.", e.getMessage());
            assertEquals("1.5 is not a valid Integer.", e.getCause().getMessage());
        }

        // an ordinary rendering is previewed as is, and a parseable one converts
        assertEquals("2.5 is not a valid AtomicInteger.",
                assertThrows(NumberFormatException.class, () -> Numbers.convert(renderedAs("2.5", 99), AtomicInteger.class)).getMessage());
        assertEquals(7, Numbers.convert(renderedAs("7", 0), AtomicInteger.class).get());
        assertEquals(7, Numbers.convert(renderedAs("7", 0), CommonUtil.typeOf(AtomicInteger.class)).get());
    }

    @Test
    public void c001_targetsAgreeForASubclassAboveTwoToTheFiftyThree() {
        // Without the fix the BigInteger/BigDecimal targets read doubleValue() and answered ...994.
        assertEquals(9007199254740993L, Numbers.convert(BD_SUB_PLAIN, Long.class));
        assertEquals(new BigInteger("9007199254740993"), Numbers.convert(BD_SUB_PLAIN, BigInteger.class));
        assertEquals(0, new BigDecimal("9007199254740993.75").compareTo(Numbers.convert(BD_SUB_PLAIN, BigDecimal.class)));
    }

    @Test
    @DisplayName("B1: the concrete pre-fix failure -- a BigInteger larger than the source it was truncated from")
    public void test_B1_theOldAnswerExceededTheSource() {
        final OpaqueNumber source = new OpaqueNumber(4611686018427388928.0d, "n/a");

        assertEquals(new BigInteger("4611686018427388928"), Numbers.convert(source, BigInteger.class));
        // The value the old spelling route produced; larger than the source, so not a truncation at all.
        assertFalse(new BigInteger("4611686018427389000").equals(Numbers.convert(source, BigInteger.class)));
    }

    @Test
    public void c087_unknownNumberBigIntegerOverflowReportsTheSourceText() {
        final Number unknown = customNumber("1e2147483648", Double.POSITIVE_INFINITY);
        final ArithmeticException e = assertThrows(ArithmeticException.class, () -> Numbers.convert(unknown, BigInteger.class));
        assertEquals("BigInteger overflow: 1e2147483648", e.getMessage());
        assertNotNull(e.getCause());
        assertEquals("BigInteger overflow: 1E+2147483648", e.getCause().getMessage());
        assertNotNull(e.getCause().getCause()); // the JDK's own exception stays at the root

        // the same source to an integral target already reported its own text
        assertEquals("byte overflow: 1e2147483648", assertThrows(ArithmeticException.class, () -> Numbers.convert(unknown, byte.class)).getMessage());
        assertEquals("long overflow: 1e2147483648", assertThrows(ArithmeticException.class, () -> Numbers.toLong(unknown)).getMessage());

        // a BigDecimal source is unchanged: it is its own text
        final ArithmeticException direct = assertThrows(ArithmeticException.class, () -> Numbers.convert(new BigDecimal("1e2147483648"), BigInteger.class));
        assertEquals("BigInteger overflow: 1E+2147483648", direct.getMessage());
        assertNotNull(direct.getCause());
        assertNull(direct.getCause().getCause());

        // in-range unknown sources still convert through the same path
        assertEquals(BigInteger.valueOf(12), Numbers.convert(customNumber("12.9", 12.9), BigInteger.class));
        assertEquals(BigInteger.TEN.pow(30), Numbers.convert(customNumber("1e30", 1e30), BigInteger.class));
        assertEquals(BigInteger.ZERO, Numbers.convert(customNumber("1e-2147483647", 0.0), BigInteger.class));
        assertEquals(BigInteger.valueOf(-7), Numbers.convert(customNumber("seven", -7.5), BigInteger.class));
    }

    @Test
    public void testUnknownNumberRenderedOnce() {
        final CountingNumber20260906 value = new CountingNumber20260906();
        assertThrows(ArithmeticException.class, () -> Numbers.toInt(value));
        assertEquals(1, value.toStringCalls);

        final Number blank = new BlankTextNumber20260906(42.0d);
        assertThrows(NumberFormatException.class, () -> Numbers.convert(blank, AtomicLong.class));
        assertEquals(Integer.valueOf(42), Numbers.convert(blank, Integer.class));
        assertThrows(ArithmeticException.class, () -> Numbers.convert(Double.NaN, Type.of(BigDecimal.class)));
        assertTrue(Double.isNaN(Numbers.convert(Double.NaN, Type.of(Double.class))));
    }

    @Test
    @DisplayName("C-001: an unusable target is still reported as unusable for a null-toString source")
    public void test_C001_unusableTargetStillThrowsForNullToStringSource() {
        final IllegalArgumentException viaNullText = assertThrows(IllegalArgumentException.class,
                () -> Numbers.convert(new NullTextNumber(42.0d), Number.class));
        final IllegalArgumentException viaInt = assertThrows(IllegalArgumentException.class, () -> Numbers.convert(42, Number.class));
        assertEquals(viaInt.getMessage(), viaNullText.getMessage());

        // A null VALUE still takes the target's default without the target being checked (unchanged contract).
        assertNull(Numbers.convert(null, Number.class));
        assertNull(Numbers.convert(null, AtomicLong.class));
    }

    // B1: unknown-Number fallback, BigInteger target == Long target == Double source, above 2^53
    @Test
    @DisplayName("B1: above 2^53 the BigInteger target truncates the exact doubleValue(), as the Long target does")
    public void test_B1_unusableText_bigIntegerTargetTruncatesTheExactDouble() {
        // Each of these is exactly representable as a double and is NOT its own shortest decimal:
        // 1.0000000000000001E18 renders as ...0100, the value is ...0128.
        final double[] values = { 1000000000000000128.0d, 4611686018427388928.0d, -4611686018427388928.0d, 9007199254740994.0d, -1000000000000000128.0d,
                Math.nextDown(0x1p63), -0x1p63 };

        for (final double d : values) {
            for (final String text : UNUSABLE_TEXTS) {
                final OpaqueNumber source = new OpaqueNumber(d, text);
                final String why = d + " / " + text;

                final long asLong = Numbers.convert(source, Long.class);
                final BigInteger asBig = Numbers.convert(source, BigInteger.class);

                assertEquals((long) d, asLong, why);
                assertEquals(BigInteger.valueOf(asLong), asBig, why);
                assertEquals(new BigDecimal(d).toBigInteger(), asBig, why);
                // ... and exactly what a Double source of the same value gives.
                assertEquals(Numbers.convert(Double.valueOf(d), BigInteger.class), asBig, why);
                assertEquals(Numbers.convert(Double.valueOf(d), Long.class), asLong, why);
                assertEquals(Numbers.toLong(source), asLong, why);
            }
        }
    }

    @Test
    @DisplayName("B1: a USABLE text is still exact and still wins over doubleValue(), on both arbitrary-precision targets")
    public void test_B1_usableTextIsUnaffected() {
        // doubleValue() deliberately disagrees with the text so the route taken is observable.
        final OpaqueNumber fractional = new OpaqueNumber(-7654321.0d, "12.9");
        assertEquals(BigInteger.valueOf(12), Numbers.convert(fractional, BigInteger.class));
        assertEquals(new BigDecimal("12.9"), Numbers.convert(fractional, BigDecimal.class));
        assertEquals(12L, Numbers.convert(fractional, Long.class));

        final OpaqueNumber huge = new OpaqueNumber(-7654321.0d, "123456789012345678901234567890.5");
        assertEquals(new BigInteger("123456789012345678901234567890"), Numbers.convert(huge, BigInteger.class));
        assertEquals(new BigDecimal("123456789012345678901234567890.5"), Numbers.convert(huge, BigDecimal.class));

        final OpaqueNumber exponent = new OpaqueNumber(-7654321.0d, "1e-100000000");
        assertSame(BigInteger.ZERO, Numbers.convert(exponent, BigInteger.class));
    }

    @Test
    public void c006_zeroAndSubOneValuesStillTruncateToZero() {
        assertEquals(BigInteger.ZERO, Numbers.convert(new BigDecimal("0e2147483648"), BigInteger.class));
        assertEquals(BigInteger.ZERO, Numbers.convert(new BigDecimal("1e-2147483647"), BigInteger.class));
        assertEquals(BigInteger.ZERO, Numbers.convert(new BigDecimal("-0.999"), BigInteger.class));
        assertEquals(BigInteger.TEN.pow(100), Numbers.convert(new BigDecimal("1e100"), BigInteger.class));
    }

    @Test
    @DisplayName("C-012: a parseable-but-unsupported target still converts, and a fractional source still fails as NumberFormatException")
    public void test_C012_unsupportedTargetBehaviourUnchanged() {
        assertEquals(12, Numbers.convert(12, AtomicInteger.class).get());
        assertEquals(7L, Numbers.convert(7, AtomicLong.class).get());
        assertEquals(12, Numbers.convert(12, Type.of(AtomicInteger.class)).get());

        assertThrows(NumberFormatException.class, () -> Numbers.convert(12.9, AtomicInteger.class));
        assertThrows(NumberFormatException.class, () -> Numbers.convert(12.9, Type.of(AtomicInteger.class)));
        assertThrows(NumberFormatException.class, () -> Numbers.convert(12.9, AtomicInteger.class, new AtomicInteger()));
        assertThrows(NumberFormatException.class, () -> Numbers.convert(12.9, Type.of(AtomicInteger.class), new AtomicInteger()));
        // An abstract target is still IllegalArgumentException, not NumberFormatException.
        assertThrows(IllegalArgumentException.class, () -> Numbers.convert(12.9, Number.class));
    }

    // toByte

    @Test
    public void c032_integerToParsersRejectSurroundingWhitespace() {
        assertThrows(NumberFormatException.class, () -> Numbers.toByte(" 12"));
        assertThrows(NumberFormatException.class, () -> Numbers.toByte("12 ", (byte) 0));
        assertThrows(NumberFormatException.class, () -> Numbers.toShort(" 12"));
        assertThrows(NumberFormatException.class, () -> Numbers.toShort(" 12 ", (short) 0));
        assertThrows(NumberFormatException.class, () -> Numbers.toInt(" 12"));
        assertThrows(NumberFormatException.class, () -> Numbers.toInt("\t12", 0));
        assertThrows(NumberFormatException.class, () -> Numbers.toLong(" 12"));
        assertThrows(NumberFormatException.class, () -> Numbers.toLong("12\n", 0L));

        assertEquals(12.0f, Numbers.toFloat(" 12 "), 0.0f);
        assertEquals(12.0f, Numbers.toFloat(" 12 ", 0.0f), 0.0f);
        assertEquals(12.0d, Numbers.toDouble(" 12 "), 0.0d);
        assertEquals(12.0d, Numbers.toDouble("\t12\n", 0.0d), 0.0d);
    }

    @Test
    @DisplayName("B1: the byte/short ranges narrow by the same lexical rule")
    public void test_B1_narrowRangesUseTheSameRule() {
        assertEquals((byte) 127, Numbers.toByte(n("127.9")));
        assertEquals((byte) -128, Numbers.toByte(n("-128.9")));
        assertThrows(ArithmeticException.class, () -> Numbers.toByte(n("128.0")));
        assertThrows(ArithmeticException.class, () -> Numbers.toByte(n("-128.0000000001e1")));
        assertEquals((short) 32767, Numbers.toShort(n("32767.999")));
        assertThrows(ArithmeticException.class, () -> Numbers.toShort(n("32768")));
        assertEquals((byte) 0, Numbers.toByte(n("0.9")));
        assertEquals((byte) 0, Numbers.toByte(n("-0.9")));
    }

    @Test
    @DisplayName("B1: an out-of-range integer token now reports overflow exactly as the String parser does")
    public void test_B1_objectPathAgreesWithTheStringParserOnOverflow() {
        // Before the fix the object path could not tell "out of range" from "malformed", so an L-suffixed
        // token -- which BigDecimal cannot parse -- fell all the way through to doubleValue() and silently
        // returned a value unrelated to the text. toByte("999L") threw while toByte((Object) x) did not.
        final Number lSuffixed = new Number() {
            private static final long serialVersionUID = 1L;

            @Override
            public int intValue() {
                return 5;
            }

            @Override
            public long longValue() {
                return 5L;
            }

            @Override
            public float floatValue() {
                return 5f;
            }

            @Override
            public double doubleValue() {
                return 5d;
            }

            @Override
            public String toString() {
                return "999L";
            }
        };

        // Superseded by 2026-08-30i for the L-suffixed spelling only. This pass proved that accepting the
        // to*(String) text grammar here made the integral targets answer from the text while the
        // BigInteger/BigDecimal targets answered from doubleValue(), because no BigDecimal can be built from
        // "999L". The recovery grammar is now decimal-only, so an L-suffixed toString() is "non-canonical
        // text" like any other formatted rendering and takes the documented doubleValue() route.
        assertEquals(5, Numbers.toByte(lSuffixed));
        assertEquals(5, Numbers.toInt(lSuffixed));
        assertThrows(ArithmeticException.class, () -> Numbers.toByte("999L"));

        // What B1 was actually about is unchanged: an out-of-range *canonical* integer token reports overflow
        // from the object path with exactly the message the String parser produces, and does not fall through
        // to doubleValue().
        final String viaString = assertThrows(ArithmeticException.class, () -> Numbers.toByte("999")).getMessage();
        final String viaObject = assertThrows(ArithmeticException.class, () -> Numbers.toByte(new DecimalTextNumber("999"))).getMessage();
        assertEquals(viaString, viaObject);
        assertEquals("byte overflow: 999", viaObject);

        // In-range canonical values are still read from the text.
        assertEquals(999, Numbers.toInt(new DecimalTextNumber("999")));

        // Text that is a decimal but not an integer token is still truncated toward zero on both routes.
        assertEquals(12, Numbers.toInt(new DecimalTextNumber("12.9")));
    }

    @Test
    public void c005_ownCauseJustOverTheOldBudgetIsIntact() {
        final NumberFormatException e = assertThrows(NumberFormatException.class, () -> Numbers.toByte("x".repeat(29)));
        assertEquals("invalid character 'x' at index 0 of " + "x".repeat(29), e.getCause().getMessage());

        final NumberFormatException d = assertThrows(NumberFormatException.class, () -> Numbers.decodeInteger("1".repeat(100) + "x"));
        assertTrue(d.getCause().getMessage().endsWith("...[101 chars]"), d.getCause().getMessage());

        final NumberFormatException c = assertThrows(NumberFormatException.class, () -> Numbers.createNumber("1".repeat(100) + "x"));
        assertTrue(c.getCause().getMessage().endsWith("...[101 chars]"), c.getCause().getMessage());
    }

    @Test
    @DisplayName("B1: a small out-of-range integer token keeps its historical message")
    public void test_B1_smallOutOfRangeTokenMessageIsUnchanged() {
        assertEquals("byte overflow: 1234", assertThrows(ArithmeticException.class, () -> Numbers.toByte(new AtomicInteger(1234))).getMessage());
        assertEquals("byte overflow: 1234", assertThrows(ArithmeticException.class, () -> Numbers.convert(new AtomicInteger(1234), Byte.class)).getMessage());
        assertEquals("short overflow: 40000", assertThrows(ArithmeticException.class, () -> Numbers.toShort(new AtomicInteger(40000))).getMessage());

        final LongAdder adder = new LongAdder();
        adder.add(3_000_000_000L);
        assertEquals("int overflow: 3000000000", assertThrows(ArithmeticException.class, () -> Numbers.toInt(adder)).getMessage());
    }

    @Test
    public void testToByte() {
        assertEquals((byte) 10, Numbers.toByte("10"));
        assertEquals((byte) 0, Numbers.toByte((String) null));
        assertEquals((byte) 0, Numbers.toByte(""));
        assertEquals((byte) -127, Numbers.toByte("-127"));
        assertEquals((byte) 42, Numbers.toByte(null, (byte) 42));
        assertEquals((byte) 5, Numbers.toByte("", (byte) 5));
        assertEquals((byte) 10, Numbers.toByte(10));
        assertEquals((byte) 10, Numbers.toByte((Object) "10"));
        assertEquals((byte) 0, Numbers.toByte((Object) null));
        assertEquals((byte) 5, Numbers.toByte((Object) null, (byte) 5));
        assertEquals((byte) 12, Numbers.toByte("12L"));
        assertEquals((byte) 127, Numbers.toByte("127"));
        assertEquals((byte) 127, Numbers.toByte("0x7F"));
        assertEquals((byte) -128, Numbers.toByte("-0x80"));
        assertEquals((byte) 10, Numbers.toByte("010"));
        assertThrows(ArithmeticException.class, () -> Numbers.toByte("128"));
        assertThrows(ArithmeticException.class, () -> Numbers.toByte("200", (byte) 5));
        assertThrows(ArithmeticException.class, () -> Numbers.toByte(200));
        assertThrows(NumberFormatException.class, () -> Numbers.toByte("abc"));
        assertEquals("byte overflow: 200", assertThrows(ArithmeticException.class, () -> Numbers.toByte("200")).getMessage());
        assertEquals("byte overflow: 0x100", assertThrows(ArithmeticException.class, () -> Numbers.toByte("0x100")).getMessage());
    }

    @Test
    public void testToXxx_Object_OverflowAndTruncation() {
        final BigInteger wrapsTo50 = new BigInteger("18446744073709551666");
        assertThrows(ArithmeticException.class, () -> Numbers.toByte(wrapsTo50, (byte) 0));
        assertThrows(ArithmeticException.class, () -> Numbers.toInt(wrapsTo50, 0));
        assertEquals((byte) 100, Numbers.toByte(new BigDecimal("100.9"), (byte) 0));
        assertEquals((byte) 127, Numbers.toByte(new BigDecimal("127.9"), (byte) 0));
        assertEquals((byte) -128, Numbers.toByte(new BigDecimal("-128.9"), (byte) 0));
        assertThrows(ArithmeticException.class, () -> Numbers.toByte(new BigDecimal("128.9"), (byte) 0));

        assertEquals((byte) 127, Numbers.toByte(127.9f, (byte) 0));
        assertEquals((byte) -128, Numbers.toByte(-128.9f, (byte) 0));
        assertEquals(2147483647, Numbers.toInt(2147483647.5d, 0));
        assertThrows(ArithmeticException.class, () -> Numbers.toByte(128.5f, (byte) 0));
        assertThrows(ArithmeticException.class, () -> Numbers.toByte(Double.NaN, (byte) 9));
        assertThrows(ArithmeticException.class, () -> Numbers.toInt(Double.POSITIVE_INFINITY, 0));

        assertEquals(Long.MAX_VALUE, Numbers.toLong(new BigDecimal("9223372036854775807.9"), 0L));
        assertEquals(Long.MIN_VALUE, Numbers.toLong(new BigDecimal("-9223372036854775808.9"), 0L));
        assertEquals(0L, Numbers.toLong(new BigDecimal("-0.9"), 0L));
        assertThrows(ArithmeticException.class, () -> Numbers.toLong(new BigDecimal("9223372036854775808"), 0L));
        assertEquals(9223372036854774784L, Numbers.toLong(9223372036854774784.0, 0L));
        assertThrows(ArithmeticException.class, () -> Numbers.toLong(0x1p63, 0L));
        assertThrows(ArithmeticException.class, () -> Numbers.toLong(1e30, 0L));
    }

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

    // toDouble

    // D2: Float -> Double via the decimal spelling, one helper behind toDouble(Object) and the table
    @Test
    @DisplayName("D2: toDouble(Object) and convert(Float, Double.class) agree on every special float")
    public void test_D2_floatToDoubleSitesAgree() {
        final float[] specials = { Float.NaN, Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY, 0.0f, -0.0f, 1.21f, Float.MIN_VALUE, -Float.MIN_VALUE,
                Float.MAX_VALUE, -Float.MAX_VALUE, 3.4E38f, 1e10f, 0.1f };

        for (final float f : specials) {
            final double viaTable = Numbers.convert(Float.valueOf(f), Double.class);
            final double viaToDouble = Numbers.toDouble(Float.valueOf(f));
            final double expected = Double.parseDouble(Float.toString(f));

            assertEquals(Double.doubleToRawLongBits(expected), Double.doubleToRawLongBits(viaTable), String.valueOf(f));
            assertEquals(Double.doubleToRawLongBits(expected), Double.doubleToRawLongBits(viaToDouble), String.valueOf(f));
            assertEquals(Double.doubleToRawLongBits(viaTable), Double.doubleToRawLongBits(Numbers.toDouble(Float.valueOf(f), 99.0d)), String.valueOf(f));
        }

        // The documented spelling semantics, and the special values by name.
        assertEquals(1.21d, Numbers.convert(Float.valueOf(1.21f), Double.class));
        assertTrue(Double.isNaN(Numbers.convert(Float.valueOf(Float.NaN), Double.class)));
        assertEquals(Double.POSITIVE_INFINITY, Numbers.convert(Float.valueOf(Float.POSITIVE_INFINITY), Double.class));
        assertEquals(Double.NEGATIVE_INFINITY, Numbers.convert(Float.valueOf(Float.NEGATIVE_INFINITY), Double.class));
        assertEquals(Double.doubleToRawLongBits(-0.0d), Double.doubleToRawLongBits(Numbers.convert(Float.valueOf(-0.0f), Double.class)));
        assertEquals(Double.doubleToRawLongBits(-0.0d), Double.doubleToRawLongBits(Numbers.convert(Float.valueOf(-0.0f), double.class)));
    }

    @Test
    @DisplayName("B1: the Float -> Double direction is unchanged and still follows the decimal spelling")
    public void test_B1_floatToDoubleStillUsesTheDecimalSpelling() {
        assertEquals(1.21d, Numbers.toDouble(1.21f), 0.0d);
        assertEquals(1.21d, Numbers.convert(1.21f, Double.class), 0.0d);
        assertNotEquals(1.21d, 1.21f, "precondition: widening does not give 1.21");
        assertEquals(new BigDecimal("1.21"), Numbers.convert(1.21f, BigDecimal.class));
    }

    @Test
    public void testToDouble() {
        assertEquals(3.14159, Numbers.toDouble("3.14159"), 0.00001);
        assertEquals(0.0, Numbers.toDouble((String) null), DELTA);
        assertEquals(0.0, Numbers.toDouble(""), DELTA);
        assertEquals(1.0, Numbers.toDouble("", 1.0), 0.001);
        assertEquals(3.14159, Numbers.toDouble((Object) "3.14159"), 0.00001);
        assertEquals(123.456, Numbers.toDouble(new BigDecimal("123.456")), 0.001);
        assertEquals(0.0, Numbers.toDouble((BigDecimal) null), 0.001);
        assertEquals(42.0, Numbers.toDouble((BigDecimal) null, 42.0), DELTA);
        assertEquals(Double.POSITIVE_INFINITY, Numbers.toDouble((Object) new BigDecimal("1e500")), 0.0);
        assertThrows(NumberFormatException.class, () -> Numbers.toDouble("abc"));
    }


    // G060-01: the integral-float fast path of the Float -> double decimal widening must equal the string round trip bit for bit.
    @Test
    public void testToDouble_floatIntegralFastPathMatchesDecimalRoundTrip() {
        final List<Float> values = new ArrayList<>();
        final float[] specials = { 0.0f, -0.0f, 1.0f, -1.0f, 7.0f, 100.0f, 1e7f, -1e7f, 9999999.0f, 12345678.0f, 0x1p24f - 1, 0x1p24f, -0x1p24f, -(0x1p24f - 1),
                0x1p24f + 2, -(0x1p24f + 2), 0x1p25f, 0x1p31f, -0x1p31f, 0x1p40f, 3.0e9f, Float.MAX_VALUE, -Float.MAX_VALUE, Float.MIN_VALUE, -Float.MIN_VALUE,
                Float.MIN_NORMAL, 0.5f, -0.5f, 1.21f, 0.1f, -2.5f, 16777215.5f, 1234.5f, Float.NaN, Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY };

        for (final float f : specials) {
            values.add(f);
        }

        for (int i = -(1 << 24) - 64; i <= -(1 << 24) + 64; i++) {
            values.add((float) i);
        }

        for (int i = (1 << 24) - 64; i <= (1 << 24) + 64; i++) {
            values.add((float) i);
        }

        final Random random = new Random(20260926L);

        for (int i = 0; i < 20000; i++) {
            values.add((float) (random.nextInt(1 << 26) - (1 << 25)));
            values.add(Float.intBitsToFloat(random.nextInt()));
        }

        for (final Float boxed : values) {
            final float f = boxed;
            final long expected = Double.doubleToRawLongBits(Double.parseDouble(Float.toString(f)));
            final String label = Float.toString(f);

            assertEquals(expected, Double.doubleToRawLongBits(Numbers.toDouble(boxed)), label);
            assertEquals(expected, Double.doubleToRawLongBits(Numbers.toDouble(boxed, 99.0d)), label);
            assertEquals(expected, Double.doubleToRawLongBits(Numbers.convert(boxed, Double.class)), label);
            assertEquals(expected, Double.doubleToRawLongBits(Numbers.convert(boxed, double.class)), label);
            assertEquals(expected, Double.doubleToRawLongBits(Numbers.convert(boxed, Type.of(Double.class))), label);
        }

        // Spot checks with literal expectations.
        assertEquals(Double.doubleToRawLongBits(-0.0d), Double.doubleToRawLongBits(Numbers.toDouble(Float.valueOf(-0.0f))));
        assertEquals(16777216.0d, Numbers.toDouble(Float.valueOf(0x1p24f)), 0.0d);
        assertEquals(1.21d, Numbers.toDouble(Float.valueOf(1.21f)), 0.0d);
        assertEquals(3.4028235E38d, Numbers.toDouble(Float.valueOf(Float.MAX_VALUE)), 0.0d);
        assertTrue(Double.isNaN(Numbers.toDouble(Float.valueOf(Float.NaN))));
    }

    // toFloat

    @Test
    @DisplayName("B1: NaN, infinities and float-range overflow are preserved by the narrowing, as before")
    public void test_B1_nonFiniteAndSaturation() {
        assertTrue(Float.isNaN(Numbers.toFloat(Double.NaN)));
        assertTrue(Float.isNaN(Numbers.convert(Double.NaN, Float.class)));
        assertEquals(Float.POSITIVE_INFINITY, Numbers.toFloat(Double.POSITIVE_INFINITY), 0.0f);
        assertEquals(Float.NEGATIVE_INFINITY, Numbers.convert(Double.NEGATIVE_INFINITY, Float.class), 0.0f);
        assertEquals(Float.POSITIVE_INFINITY, Numbers.toFloat(1e300d), 0.0f);
        assertEquals(Float.NEGATIVE_INFINITY, Numbers.convert(-1e300d, Float.class), 0.0f);
        assertEquals(0.0f, Numbers.toFloat(1e-50d), 0.0f);
        assertEquals(Float.floatToIntBits(-0.0f), Float.floatToIntBits(Numbers.convert(-1e-50d, Float.class)));
    }

    @Test
    @DisplayName("B1: a random sweep - toFloat(Object Double) and convert agree with the cast bit for bit")
    public void test_B1_randomSweepMatchesTheCast() {
        final Random rnd = new Random(1L);

        for (int i = 0; i < 200_000; i++) {
            final double d = Double.longBitsToDouble(rnd.nextLong());
            final int expected = Float.floatToIntBits((float) d);
            assertEquals(expected, Float.floatToIntBits(Numbers.toFloat(d)));
            assertEquals(expected, Float.floatToIntBits(Numbers.convert(d, Float.class)));
        }
    }

    @Test
    public void testToFloat() {
        assertEquals(3.14f, Numbers.toFloat("3.14"), 0.001f);
        assertEquals(0.0f, Numbers.toFloat((String) null), FLOAT_DELTA);
        assertEquals(0.0f, Numbers.toFloat(""), FLOAT_DELTA);
        assertEquals(1.0f, Numbers.toFloat("", 1.0f), 0.001f);
        assertEquals(3.14f, Numbers.toFloat(3.14f), 0.001f);
        assertEquals(3.14f, Numbers.toFloat((Object) "3.14"), 0.001f);
        assertEquals(Float.POSITIVE_INFINITY, Numbers.toFloat("Infinity"));
        assertTrue(Float.isNaN(Numbers.toFloat("NaN")));
        assertEquals(0.0f, Numbers.toFloat((BigDecimal) null), 0.0f);
        assertEquals(8.5f, Numbers.toFloat(BigDecimal.valueOf(8.5)), 0.0f);
        assertTrue(Float.isInfinite(Numbers.toFloat(new BigDecimal("1e40"))));
        assertEquals(Float.POSITIVE_INFINITY, Numbers.toFloat((Object) new BigDecimal("1e500")), 0.0f);
        assertThrows(NumberFormatException.class, () -> Numbers.toFloat("abc"));
    }

    @Test
    public void testToFloatToDouble_BoundedMessage() {
        final String hostile = "9".repeat(4000) + "Z";
        for (final Runnable call : List.<Runnable> of(() -> Numbers.toFloat(hostile), () -> Numbers.toDouble(hostile))) {
            final String message = assertThrows(NumberFormatException.class, call::run).getMessage();
            assertTrue(message.length() < 200, message);
            assertTrue(message.contains("...[" + hostile.length() + " chars]"));
            assertFalse(message.startsWith("For input string"));
        }
        final String nl = assertThrows(NumberFormatException.class, () -> Numbers.toFloat("1\n2")).getMessage();
        assertFalse(nl.contains("\n"));
        assertTrue(nl.contains("1\\u000A2"));

        for (final String s : new String[] { "1.5", "NaN", "Infinity", "0x1.8p1", " 123 " }) {
            assertEquals(Float.parseFloat(s), Numbers.toFloat(s), s);
            assertEquals(Double.parseDouble(s), Numbers.toDouble(s), s);
        }
        for (final String s : new String[] { "abc", "0xFF", "123L" }) {
            assertThrows(NumberFormatException.class, () -> Numbers.toFloat(s), s);
        }
    }

    // toInt

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

    @Test
    public void c115_formatCharactersAreEscapedAndQuotedAtTheEdges() {
        NumberFormatException e = assertThrows(NumberFormatException.class, () -> Numbers.toInt("\uFEFF123"));
        assertEquals("\"\\uFEFF123\" is not a valid Integer.", e.getMessage());
        assertEquals("invalid character '\\uFEFF' at index 0 of \\uFEFF123", e.getCause().getMessage());

        e = assertThrows(NumberFormatException.class, () -> Numbers.parseDouble("\uFEFF123"));
        assertEquals("\"\\uFEFF123\" is not a valid Double.", e.getMessage());
        assertEquals("For input string: \"\\uFEFF123\"", e.getCause().getMessage());

        e = assertThrows(NumberFormatException.class, () -> Numbers.toInt("1\u202E2"));
        assertEquals("1\\u202E2 is not a valid Integer.", e.getMessage());
        assertEquals("invalid character '\\u202E' at index 1 of 1\\u202E2", e.getCause().getMessage());

        assertEquals("\"123\\uFEFF\" is not a valid Integer.", assertThrows(NumberFormatException.class, () -> Numbers.toInt("123\uFEFF")).getMessage());
        assertEquals("\"\\u200B1\" is not a valid Integer.", assertThrows(NumberFormatException.class, () -> Numbers.toInt("\u200B1")).getMessage());
        assertEquals("\"1\\u00AD\" is not a valid Integer.", assertThrows(NumberFormatException.class, () -> Numbers.toInt("1\u00AD")).getMessage());
        assertEquals("1\\u200B2 is not a valid Integer.", assertThrows(NumberFormatException.class, () -> Numbers.toInt("1\u200B2")).getMessage());
        assertEquals("\"\\uFEFF1\" is not a valid BigDecimal.",
                assertThrows(NumberFormatException.class, () -> Numbers.parseBigDecimal("\uFEFF1")).getMessage());
        assertEquals("\"\\uFEFF1\" is not a valid Integer.", assertThrows(NumberFormatException.class, () -> Numbers.decodeInteger("\uFEFF1")).getMessage());
        assertEquals("\"\\uFEFF1\" is not a valid Number.", assertThrows(NumberFormatException.class, () -> Numbers.createNumber("\uFEFF1")).getMessage());
        assertEquals("\"\\u20661\\u2069\" is not a valid Long.", assertThrows(NumberFormatException.class, () -> Numbers.toLong("\u20661\u2069")).getMessage());

        // a supplementary format character (a tag) is escaped as its two units; an emoji still passes through raw
        assertEquals("1\\uDB40\\uDC011 is not a valid Integer.", assertThrows(NumberFormatException.class, () -> Numbers.toInt("1\uDB40\uDC011")).getMessage());
        assertEquals("1\uD83D\uDE002 is not a valid Integer.", assertThrows(NumberFormatException.class, () -> Numbers.toInt("1\uD83D\uDE002")).getMessage());

        // a non-breaking space (a space separator, not a format character) and plain whitespace are unchanged
        assertEquals("\"\u00A01\" is not a valid Integer.", assertThrows(NumberFormatException.class, () -> Numbers.toInt("\u00A01")).getMessage());
        assertEquals("\" 1\" is not a valid Integer.", assertThrows(NumberFormatException.class, () -> Numbers.toInt(" 1")).getMessage());
        assertEquals("1\u00A02 is not a valid Integer.", assertThrows(NumberFormatException.class, () -> Numbers.toInt("1\u00A02")).getMessage());
        assertEquals("\"\\u00091\" is not a valid Integer.", assertThrows(NumberFormatException.class, () -> Numbers.toInt("\t1")).getMessage());

        // no message in the retained chain carries a raw format character
        for (final String token : new String[] { "\uFEFF123", "1\u202E2", "\u200B", "\u00AD1", "12\u2069", "1.5e\u200D3" }) {
            for (Throwable t = assertThrows(NumberFormatException.class, () -> Numbers.createNumber(token), token); t != null; t = t.getCause()) {
                final String message = t.getMessage();

                for (int i = 0; i < message.length(); i++) {
                    assertTrue(Character.getType(message.charAt(i)) != Character.FORMAT, token + ": " + message);
                }
            }
        }
    }

    // B2: one recovery grammar for every target
    @Test
    @DisplayName("B2: a hex or L-suffixed toString() is not a Number's canonical text and takes doubleValue()")
    public void test_B2_hexAndLSuffixedTextAreNotCanonicalNumberText() {
        // toInt(String) accepts these spellings because it is a text parse. Object conversion is a numeric
        // coercion, and no Number renders itself in hex or with a Java long-literal suffix, so the text is
        // treated as non-canonical and the documented doubleValue() fallback applies.
        for (final String text : new String[] { "0xFF", "0XFF", "#FF", "-0xFF", "999L", "999l", "-999L", "007L", "0x10" }) {
            final SplitNumber value = n(text);

            assertEquals((int) SENTINEL, Numbers.toInt(value), text);
            assertEquals((long) SENTINEL, Numbers.toLong(value), text);
            assertEquals(Integer.valueOf((int) SENTINEL), Numbers.convert(value, Integer.class), text);
            assertEquals(BigInteger.valueOf((long) SENTINEL), Numbers.convert(value, BigInteger.class), text);
        }

        // The String parsers are unchanged: they still read the same spellings as text.
        assertEquals(255, Numbers.toInt("0xFF"));
        assertEquals(255, Numbers.toInt("#FF"));
        assertEquals(999, Numbers.toInt("999L"));
    }

    // B1: the integral recovery is lexical, so it is linear and allocation-free at any length
    @Test
    @DisplayName("B1: a huge decimal toString() no longer builds a BigDecimal")
    public void test_B1_hugeDecimalTextIsBoundedInTimeAndAllocation() {
        final String text = "1." + "9".repeat(200_000);
        final SplitNumber value = n(text);

        // warm up so the measurement is not dominated by first-call class loading
        for (int i = 0; i < 2_000; i++) {
            Numbers.toInt(n("1.5"));
        }

        assertEquals(1, Numbers.toInt(value));

        final ThreadMXBean bean = ManagementFactory.getThreadMXBean();

        if (bean instanceof com.sun.management.ThreadMXBean) {
            final com.sun.management.ThreadMXBean sun = (com.sun.management.ThreadMXBean) bean;
            final long id = Thread.currentThread().getId();
            final long before = sun.getThreadAllocatedBytes(id);
            Numbers.toInt(value);
            final long allocated = sun.getThreadAllocatedBytes(id) - before;

            // The BigDecimal this replaced allocated ~13.6 MB for this input. Nothing input-sized is created
            // now: only the DecimalTextScan holder and the exception-free scan.
            assertTrue(allocated < 8_192, "toInt((Object) hugeDecimalText) allocated " + allocated + " bytes");
        }

        // Doubling the digit count must not quadruple the cost. Measured before the fix: 431 ms -> 2128 ms for
        // 200k -> 400k digits; after: about 1 ms at either size. The bound is generous so the assertion is
        // about the complexity class, not the machine.
        final SplitNumber twiceAsLong = n("1." + "9".repeat(400_000));
        final long start = System.nanoTime();

        for (int i = 0; i < 50; i++) {
            Numbers.toInt(twiceAsLong);
        }

        final long millis = (System.nanoTime() - start) / 1_000_000;
        assertTrue(millis < 2_000, "50 calls on a 400k-digit decimal toString() took " + millis + " ms");
    }

    @Test
    @DisplayName("B1: an out-of-range decimal toString() reports overflow without arbitrary precision")
    public void test_B1_hugeOutOfRangeDecimalTextReportsOverflow() {
        final SplitNumber huge = n("9".repeat(100_000) + ".5");
        final ArithmeticException e = assertThrows(ArithmeticException.class, () -> Numbers.toInt(huge));

        assertTrue(e.getMessage().startsWith("int overflow: "), e.getMessage());
        assertTrue(e.getMessage().contains("...[100002 chars]"), e.getMessage());
        assertTrue(e.getMessage().length() < 256, "message length " + e.getMessage().length());
    }

    @Test
    @DisplayName("B1: an in-range integer token is unaffected, including the Long.MIN_VALUE collision")
    public void test_B1_inRangeTokensAreUnaffected() {
        assertEquals(1234, Numbers.toInt(new AtomicInteger(1234)));
        assertEquals(-5L, Numbers.toLong(new AtomicLong(-5L)));
        assertEquals(Long.MIN_VALUE, Numbers.toLong(new DecimalTextNumber("-9223372036854775808")));
        assertEquals(Long.MAX_VALUE, Numbers.toLong(new DecimalTextNumber("9223372036854775807")));
        assertEquals(0, Numbers.toInt(new DecimalTextNumber("0")));
        assertEquals(1, Numbers.toInt(new DecimalTextNumber("000000000000000000000000001")));
    }

    @Test
    @DisplayName("B1: a null, empty or non-numeric toString() still falls back to doubleValue()")
    public void test_B1_nonCanonicalTextStillFallsBack() {
        assertEquals((int) SENTINEL, Numbers.toInt(new SplitNumber(null, SENTINEL)));
        assertEquals((int) SENTINEL, Numbers.toInt(n("")));
        assertEquals((int) SENTINEL, Numbers.toInt(n("42 ms")));
        assertEquals((int) SENTINEL, Numbers.toInt(n("1,234")));
        assertEquals((int) SENTINEL, Numbers.toInt(n(" 42 ")));

        // Non-finite doubleValue() on a non-canonical text is still an overflow, not an NPE or a wrap.
        assertThrows(ArithmeticException.class, () -> Numbers.toInt(new SplitNumber("frob", Double.NaN)));
        assertThrows(ArithmeticException.class, () -> Numbers.toInt(new SplitNumber("frob", Double.POSITIVE_INFINITY)));
        assertThrows(ArithmeticException.class, () -> Numbers.convert(new SplitNumber("frob", Double.NaN), BigInteger.class));
        assertThrows(ArithmeticException.class, () -> Numbers.convert(new SplitNumber(null, Double.NaN), BigDecimal.class));
    }

    @Test
    @DisplayName("B1: the decimal and doubleValue() steps are still reached for text that is not an integer token")
    public void test_B1_nonIntegerTextStillTakesTheLaterSteps() {
        // Fractional text -> the BigDecimal step, truncating toward zero.
        assertEquals(12, Numbers.toInt(new DecimalTextNumber("12.9")));
        assertEquals(-12, Numbers.toInt(new DecimalTextNumber("-12.9")));
        assertEquals((byte) 127, Numbers.toByte(new DecimalTextNumber("127.9")));

        // The boundary the BigDecimal step exists for: the integer part is exactly Long.MAX_VALUE, but
        // doubleValue() rounds the whole value up to 2^63 and would wrongly reject it.
        final DecimalTextNumber edge = new DecimalTextNumber("9223372036854775807.9");
        assertEquals(9.223372036854776E18, edge.doubleValue(), "precondition: doubleValue rounds up to 2^63");
        assertEquals(Long.MAX_VALUE, Numbers.toLong(edge));
        assertEquals(Long.MIN_VALUE, Numbers.toLong(new DecimalTextNumber("-9223372036854775808.9")));

        // Non-canonical text -> the doubleValue() step, not a parse failure.
        assertEquals(12, Numbers.toInt(new FormattedNumber(12.9)));
        assertEquals(12L, Numbers.toLong(new FormattedNumber(12.9)));

        // A fractional value out of range is still an overflow, decided from the exact decimal.
        assertThrows(ArithmeticException.class, () -> Numbers.toByte(new DecimalTextNumber("128.9")));
    }

    @Test
    public void c055_notAValidNumberQuotesWhitespaceEdges() {
        assertEquals("\" 1 \" is not a valid Integer.", assertThrows(NumberFormatException.class, () -> Numbers.toInt(" 1 ")).getMessage());
        assertEquals("\"1 \" is not a valid Integer.", assertThrows(NumberFormatException.class, () -> Numbers.toInt("1 ")).getMessage());
        assertEquals("\" 1\" is not a valid Long.", assertThrows(NumberFormatException.class, () -> Numbers.toLong(" 1")).getMessage());
        assertEquals("\"\u00A01\" is not a valid Integer.", assertThrows(NumberFormatException.class, () -> Numbers.toInt("\u00A01")).getMessage());
        assertEquals("\"1\u00A0\" is not a valid Short.", assertThrows(NumberFormatException.class, () -> Numbers.toShort("1\u00A0")).getMessage());
        assertEquals("\" \" is not a valid Integer.", assertThrows(NumberFormatException.class, () -> Numbers.toInt(" ")).getMessage());
        assertEquals("\"\\u00091\" is not a valid Integer.", assertThrows(NumberFormatException.class, () -> Numbers.toInt("\t1")).getMessage());
        assertEquals("\" 1\" is not a valid BigDecimal.", assertThrows(NumberFormatException.class, () -> Numbers.parseBigDecimal(" 1")).getMessage());
        assertEquals("\" 1\" is not a valid Integer.", assertThrows(NumberFormatException.class, () -> Numbers.decodeInteger(" 1")).getMessage());

        // Interior whitespace and plain junk stay unquoted across the conversion entry points.
        assertEquals("abc is not a valid Integer.", assertThrows(NumberFormatException.class, () -> Numbers.toInt("abc")).getMessage());
        assertEquals("abc is not a valid Integer.", assertThrows(NumberFormatException.class, () -> Numbers.decodeInteger("abc")).getMessage());
        assertEquals("1 2 is not a valid Integer.", assertThrows(NumberFormatException.class, () -> Numbers.toInt("1 2")).getMessage());
        assertEquals("1\u00A02 is not a valid Integer.", assertThrows(NumberFormatException.class, () -> Numbers.toInt("1\u00A02")).getMessage());
        assertEquals("12x is not a valid Byte.", assertThrows(NumberFormatException.class, () -> Numbers.toByte("12x")).getMessage());
    }

    // B1 - an out-of-range integer token must not be routed through BigDecimal.
    @Test
    @DisplayName("B1: an out-of-range integral toString() is rejected without an arbitrary-precision parse")
    public void test_B1_outOfRangeIntegerTokenDoesNotBuildABigDecimal() {
        final java.lang.management.ThreadMXBean threadBean = java.lang.management.ManagementFactory.getThreadMXBean();
        final com.sun.management.ThreadMXBean allocationBean = threadBean instanceof com.sun.management.ThreadMXBean
                ? (com.sun.management.ThreadMXBean) threadBean
                : null;

        if (allocationBean == null || !allocationBean.isThreadAllocatedMemorySupported()) {
            return;
        }

        if (!allocationBean.isThreadAllocatedMemoryEnabled()) {
            allocationBean.setThreadAllocatedMemoryEnabled(true);
        }

        final HugeIntegralTextNumber huge = new HugeIntegralTextNumber(200_000);

        // Warm the path so class loading is not billed to the measured call.
        assertThrows(ArithmeticException.class, () -> Numbers.toInt(huge));

        final long threadId = Thread.currentThread().getId();
        final long before = allocationBean.getThreadAllocatedBytes(threadId);
        assertThrows(ArithmeticException.class, () -> Numbers.toInt(huge));
        final long allocated = allocationBean.getThreadAllocatedBytes(threadId) - before;

        // The scan is in place, so the rejection costs a constant amount of memory regardless of the token
        // length. Before the fix this fell through to new BigDecimal(text): 488 KB for a 100,000-digit token
        // and 967 KB for a 200,000-digit one, growing super-linearly. Measured after the fix: 1,224 bytes.
        Assertions.assertTrue(allocated < 64 * 1024, "rejecting a 200,000-digit integer token allocated " + allocated + " bytes");
    }

    @Test
    public void test_overflowMessage_aNumericToStringIsPreviewedRatherThanSummarized() {
        // Superseded expectation. When this test was written the object path recovered a huge numeric
        // toString() by building a BigDecimal, so the message came out of describeNumberForError's
        // BigDecimal branch ("BigDecimal[signum=1, unscaledBitLength=332193, scale=0]"). The 2026-08-30h
        // pass began reporting the raw token instead, to match what toInt(String) prints, and 2026-08-30i
        // removed the BigDecimal from this path altogether. The subtype is not a BigDecimal, so the message
        // now takes the ordinary preview branch -- still bounded and escaped, which is what C-001 was about.
        final LoudNumber numeric = new LoudNumber("9".repeat(100_000), Double.NaN);
        final ArithmeticException e = assertThrows(ArithmeticException.class, () -> Numbers.toInt(numeric));

        assertBoundedAndEscaped(e);
        assertEquals("int overflow: " + "9".repeat(96) + "...[100000 chars]", e.getMessage());

        // A real BigDecimal argument still takes the bit-length summary, so the two branches remain distinct.
        assertEquals("int overflow: BigDecimal[signum=1, unscaledBitLength=332193, scale=0]",
                assertThrows(ArithmeticException.class, () -> Numbers.toInt(new BigDecimal("9".repeat(100_000)))).getMessage());
    }

    @Test
    public void test_overflowMessage_escapesAControlCharacterInANumbersToString() {
        final LoudNumber withBreak = new LoudNumber("1" + (char) 0x0A + "2", Double.NaN);

        final ArithmeticException e = assertThrows(ArithmeticException.class, () -> Numbers.toLong(withBreak));
        assertBoundedAndEscaped(e);
        assertEquals("long overflow: 1\\u000A2", e.getMessage());

        // an astral pair is printable and must survive intact
        final LoudNumber emoji = new LoudNumber("1" + (char) 0xD83D + (char) 0xDE00 + "2", Double.NaN);
        assertEquals("int overflow: 1" + (char) 0xD83D + (char) 0xDE00 + "2", assertThrows(ArithmeticException.class, () -> Numbers.toInt(emoji)).getMessage());

        // an unpaired surrogate is escaped rather than emitted as broken UTF-16
        final LoudNumber lone = new LoudNumber("1" + (char) 0xD800 + "2", Double.NaN);
        assertEquals("int overflow: 1\\uD8002", assertThrows(ArithmeticException.class, () -> Numbers.toInt(lone)).getMessage());
    }

    @Test
    public void test_overflowMessage_spellingIsUnchangedForEveryTypeThatRenderedInFullBefore() {
        assertEquals("byte overflow: 1000", assertThrows(ArithmeticException.class, () -> Numbers.toByte(1000)).getMessage());
        assertEquals("byte overflow: 1234", assertThrows(ArithmeticException.class, () -> Numbers.toByte(new AtomicInteger(1234))).getMessage());
        assertEquals("int overflow: NaN", assertThrows(ArithmeticException.class, () -> Numbers.toInt(Double.NaN)).getMessage());
        assertEquals("long overflow: 1.0E300", assertThrows(ArithmeticException.class, () -> Numbers.toLong(1e300d)).getMessage());

        // The widest BigDecimal the 192-bit guard admits is 72 characters and must still render in full.
        final BigDecimal widest = new BigDecimal(BigInteger.ONE.shiftLeft(192).subtract(BigInteger.ONE).negate(), Integer.MIN_VALUE + 58);

        // 72 is the number MAX_NUMBER_ERROR_TEXT_LENGTH is derived from; pin it so that a JDK change to
        // BigDecimal.toString() cannot silently invalidate the cap's rationale.
        assertEquals(72, widest.toString().length(), widest.toString());

        final String rendered = assertThrows(ArithmeticException.class, () -> Numbers.toInt(widest)).getMessage();
        assertEquals("int overflow: " + widest, rendered);
        assertTrue(rendered.length() < 256, rendered);

        // Beyond the guard, the existing summary form is unchanged.
        assertTrue(assertThrows(ArithmeticException.class, () -> Numbers.toInt(BigInteger.ONE.shiftLeft(2000))).getMessage()
                .startsWith("int overflow: BigInteger[signum=1, bitLength=2001]"));
    }

    @Test
    @DisplayName("B1: the overflow message reuses the toString() the recovery already read")
    public void test_B1_overflowMessageDoesNotCallToStringTwice() {
        final int[] calls = { 0 };
        final Number counting = new Number() {
            private static final long serialVersionUID = 1L;

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

            @Override
            public String toString() {
                calls[0]++;
                return "99999999999999999999999";
            }
        };

        assertThrows(ArithmeticException.class, () -> Numbers.toInt(counting));
        assertEquals(1, calls[0], "toString() should be read once, not once for the value and once for the message");
    }

    // C-005
    @Test
    public void c005_ownCauseKeepsTheInputLengthMarker() {
        final NumberFormatException e = assertThrows(NumberFormatException.class, () -> Numbers.toInt("1".repeat(100) + "x"));
        assertTrue(e.getMessage().endsWith("...[101 chars] is not a valid Integer."), e.getMessage());
        assertNotNull(e.getCause());
        final String cause = e.getCause().getMessage();
        assertTrue(cause.startsWith("invalid character 'x' at index 100 of " + "1".repeat(64) + "...[101 chars]"), cause);

        final NumberFormatException huge = assertThrows(NumberFormatException.class, () -> Numbers.toByte("x".repeat(100_000)));
        assertTrue(huge.getCause().getMessage().endsWith("...[100000 chars]"), huge.getCause().getMessage());
    }

    @Test
    public void c005_ownProductArrivingThroughTheTypeLayerIsNotCutAgain() {
        // The string-routed AtomicInteger target hands the source's text to the Type layer, which calls
        // Numbers.toInt; that failure comes back as the cause and must not be cut a second time.
        final NumberFormatException e = assertThrows(NumberFormatException.class,
                () -> Numbers.convert(new BigDecimal("1." + "5".repeat(200)), AtomicInteger.class));
        final String cause = e.getCause().getMessage();
        assertTrue(cause.endsWith("...[202 chars] is not a valid Integer."), cause);
    }

    @Test
    @DisplayName("B2: the recognized subtypes and canonical decimal text are unaffected")
    public void test_B2_recognizedSubtypesAreUnaffected() {
        assertEquals(12, Numbers.toInt(new AtomicInteger(12)));
        assertEquals(12L, Numbers.toLong(new AtomicLong(12L)));

        final LongAdder adder = new LongAdder();
        adder.add(1234);
        assertEquals(1234, Numbers.toInt(adder));
        assertEquals(BigInteger.valueOf(1234), Numbers.convert(adder, BigInteger.class));

        final DoubleAdder fractional = new DoubleAdder();
        fractional.add(12.9d);
        assertEquals(12, Numbers.toInt(fractional));
        assertEquals(BigInteger.valueOf(12), Numbers.convert(fractional, BigInteger.class));
        assertEquals(0, new BigDecimal("12.9").compareTo(Numbers.convert(fractional, BigDecimal.class)));
    }

    @Test
    public void c147_supplementaryFormatCharacterAtAnEdgeIsQuoted() {
        final String tag = "\uDB40\uDC01"; // U+E0001 LANGUAGE TAG: a format character outside the BMP
        final String beam = "\uD834\uDD73"; // U+1D173 MUSICAL SYMBOL BEGIN BEAM: likewise

        final NumberFormatException e = assertThrows(NumberFormatException.class, () -> Numbers.toInt(tag + "5"));
        assertEquals("\"\\uDB40\\uDC015\" is not a valid Integer.", e.getMessage());
        assertEquals("invalid character '\\uDB40\\uDC01' at index 0 of \\uDB40\\uDC015", e.getCause().getMessage());
        assertEquals("\"5\\uDB40\\uDC01\" is not a valid Integer.", assertThrows(NumberFormatException.class, () -> Numbers.toInt("5" + tag)).getMessage());
        assertEquals("\"\\uDB40\\uDC01\" is not a valid Integer.", assertThrows(NumberFormatException.class, () -> Numbers.toInt(tag)).getMessage());
        assertEquals("\"\\uDB40\\uDC015\\uDB40\\uDC01\" is not a valid Integer.",
                assertThrows(NumberFormatException.class, () -> Numbers.toInt(tag + "5" + tag)).getMessage());
        assertEquals("\"\\uD834\\uDD735\" is not a valid Integer.", assertThrows(NumberFormatException.class, () -> Numbers.toInt(beam + "5")).getMessage());
        assertEquals("\"5\\uD834\\uDD73\" is not a valid Short.", assertThrows(NumberFormatException.class, () -> Numbers.toShort("5" + beam)).getMessage());
        assertEquals("\"\\uDB40\\uDC015\" is not a valid Long.", assertThrows(NumberFormatException.class, () -> Numbers.toLong(tag + "5")).getMessage());
        assertEquals("\"\\uDB40\\uDC015\" is not a valid Integer.",
                assertThrows(NumberFormatException.class, () -> Numbers.decodeInteger(tag + "5")).getMessage());
        assertEquals("\"\\uDB40\\uDC015\" is not a valid Number.",
                assertThrows(NumberFormatException.class, () -> Numbers.createNumber(tag + "5")).getMessage());
        assertEquals("\"\\uDB40\\uDC015\" is not a valid BigDecimal.",
                assertThrows(NumberFormatException.class, () -> Numbers.parseBigDecimal(tag + "5")).getMessage());
        final NumberFormatException f = assertThrows(NumberFormatException.class, () -> Numbers.toFloat(tag + "5"));
        assertEquals("\"\\uDB40\\uDC015\" is not a valid Float.", f.getMessage());
        assertEquals("For input string: \"\\uDB40\\uDC015\"", f.getCause().getMessage());
        assertEquals("\"5\\uDB40\\uDC01\" is not a valid Double.", assertThrows(NumberFormatException.class, () -> Numbers.toDouble("5" + tag)).getMessage());

        // exactly as a BMP format character at an edge (C-115), which is unchanged
        assertEquals("\"\\uFEFF5\" is not a valid Integer.", assertThrows(NumberFormatException.class, () -> Numbers.toInt("\uFEFF5")).getMessage());
        assertEquals("\"5\\u200B\" is not a valid Integer.", assertThrows(NumberFormatException.class, () -> Numbers.toInt("5\u200B")).getMessage());

        // inside the token the character is escaped but nothing is quoted (C-115 pins the same for the tag)
        assertEquals("1\\uDB40\\uDC012 is not a valid Integer.", assertThrows(NumberFormatException.class, () -> Numbers.toInt("1" + tag + "2")).getMessage());
        assertEquals("1\\uD834\\uDD732 is not a valid Integer.", assertThrows(NumberFormatException.class, () -> Numbers.toInt("1" + beam + "2")).getMessage());

        // a printable supplementary character at an edge (an emoji) is neither escaped nor quoted
        assertEquals("\uD83D\uDE005 is not a valid Integer.", assertThrows(NumberFormatException.class, () -> Numbers.toInt("\uD83D\uDE005")).getMessage());
        assertEquals("5\uD83D\uDE00 is not a valid Integer.", assertThrows(NumberFormatException.class, () -> Numbers.toInt("5\uD83D\uDE00")).getMessage());

        // an unpaired surrogate at an edge is escaped but not quoted: it is not a format character
        assertEquals("\\uD83D5 is not a valid Integer.", assertThrows(NumberFormatException.class, () -> Numbers.toInt("\uD83D5")).getMessage());
        assertEquals("5\\uDE00 is not a valid Integer.", assertThrows(NumberFormatException.class, () -> Numbers.toInt("5\uDE00")).getMessage());
        assertEquals("\\uDC005 is not a valid Integer.", assertThrows(NumberFormatException.class, () -> Numbers.toInt("\uDC005")).getMessage());
        assertEquals("5\\uD83D is not a valid Integer.", assertThrows(NumberFormatException.class, () -> Numbers.toInt("5\uD83D")).getMessage());

        // invisible characters that are not format characters pass through raw and unquoted, as documented
        assertEquals("\u31645 is not a valid Integer.", assertThrows(NumberFormatException.class, () -> Numbers.toInt("\u31645")).getMessage()); // Hangul filler, Lo
        assertEquals("5\uFE0F is not a valid Integer.", assertThrows(NumberFormatException.class, () -> Numbers.toInt("5\uFE0F")).getMessage()); // VS16, Mn
        assertEquals("\u034F5 is not a valid Integer.", assertThrows(NumberFormatException.class, () -> Numbers.toInt("\u034F5")).getMessage()); // CGJ, Mn
        assertEquals("\uDB40\uDD005 is not a valid Integer.", assertThrows(NumberFormatException.class, () -> Numbers.toInt("\uDB40\uDD005")).getMessage()); // U+E0100, Mn

        // whitespace edges (C-055) are unchanged
        assertEquals("\" 5\" is not a valid Integer.", assertThrows(NumberFormatException.class, () -> Numbers.toInt(" 5")).getMessage());
        assertEquals("\"\\u00095\" is not a valid Integer.", assertThrows(NumberFormatException.class, () -> Numbers.toInt("\t5")).getMessage());
    }

    @Test
    public void testToInt() {
        assertEquals(1, Numbers.toInt("1"));
        assertEquals(100000, Numbers.toInt("100000"));
        assertEquals(0, Numbers.toInt((String) null));
        assertEquals(0, Numbers.toInt(""));
        assertEquals(42, Numbers.toInt(null, 42));
        assertEquals(77, Numbers.toInt("", 77));
        assertEquals(1000, Numbers.toInt(1000));
        assertEquals(1000, Numbers.toInt(1000L));
        assertEquals(1000, Numbers.toInt((Object) "1000"));
        assertEquals(0, Numbers.toInt((Object) null));
        assertEquals(500, Numbers.toInt((Object) null, 500));
        assertEquals(123456, Numbers.toInt("123456L"));
        assertEquals(10, Numbers.toInt("010"));
        assertEquals(255, Numbers.toInt("0xFF"));
        assertEquals(255, Numbers.toInt("#FF"));
        assertEquals(2147483647, Numbers.toInt("2147483647"));
        assertThrows(ArithmeticException.class, () -> Numbers.toInt("2147483648"));
        assertThrows(ArithmeticException.class, () -> Numbers.toInt((long) Integer.MAX_VALUE + 1));
        assertThrows(NumberFormatException.class, () -> Numbers.toInt("abc"));
        assertThrows(NumberFormatException.class, () -> Numbers.toInt("abc", 0));
        assertEquals(7, Numbers.toInt((String) null, 7));
        assertEquals("abc is not a valid Integer.", assertThrows(NumberFormatException.class, () -> Numbers.toInt("abc")).getMessage());
    }

    @Test
    public void testToXxx_OverflowAndHex() {
        assertThrows(ArithmeticException.class, () -> Numbers.toByte("128"));
        assertThrows(ArithmeticException.class, () -> Numbers.toByte("-129"));
        assertThrows(ArithmeticException.class, () -> Numbers.toShort("32768"));
        assertThrows(ArithmeticException.class, () -> Numbers.toInt("2147483648"));
        assertThrows(ArithmeticException.class, () -> Numbers.toLong("9223372036854775808"));
        assertThrows(ArithmeticException.class, () -> Numbers.toByte("0xFF"));
        assertThrows(ArithmeticException.class, () -> Numbers.toByte("0xFFF"));
        assertThrows(ArithmeticException.class, () -> Numbers.toInt("0x100000000"));
        assertThrows(ArithmeticException.class, () -> Numbers.toLong("0x10000000000000000"));
        assertThrows(ArithmeticException.class, () -> Numbers.toLong("09999999999999999999"));
        assertThrows(ArithmeticException.class, () -> Numbers.toByte("128L"));
        assertThrows(NumberFormatException.class, () -> Numbers.toInt("L"));
        assertThrows(NumberFormatException.class, () -> Numbers.toInt("1.5"));

        assertEquals(10, Numbers.toInt("010"));
        assertEquals(Integer.valueOf(8), Numbers.decodeInteger("010"));
        assertEquals(123, Numbers.toInt("0123L"));
        assertEquals(Long.valueOf(83L), Numbers.decodeLong("0123L"));
        assertEquals(8, Numbers.toInt("08"));
        assertThrows(NumberFormatException.class, () -> Numbers.decodeInteger("08"));
        assertTrue(Numbers.isParsable("08"));
        assertFalse(Numbers.isCreatable("08"));
    }

    @Test
    public void testToIntExact() {
        assertEquals(123, Numbers.toIntExact(123L));
        assertEquals(Integer.MAX_VALUE, Numbers.toIntExact(Integer.MAX_VALUE));
        assertEquals(Integer.MIN_VALUE, Numbers.toIntExact(Integer.MIN_VALUE));
        assertThrows(ArithmeticException.class, () -> Numbers.toIntExact(Integer.MAX_VALUE + 1L));
        assertThrows(ArithmeticException.class, () -> Numbers.toIntExact(Integer.MIN_VALUE - 1L));
    }

    // toLong

    @Test
    public void testErrorMessages_BoundedAndEscaped() {
        final NumberFormatException ctrl = assertThrows(NumberFormatException.class, () -> Numbers.toLong("12\n34"));
        assertTrue(ctrl.getMessage().contains("\\u000A"), ctrl.getMessage());
        assertFalse(ctrl.getMessage().contains("\n"), ctrl.getMessage());
        final NumberFormatException decode = assertThrows(NumberFormatException.class, () -> Numbers.decodeLong("9".repeat(100_000)));
        assertTrue(decode.getMessage().length() < 128, decode.getMessage());
        final String hugeDigits = "9".repeat(100_000);
        final ArithmeticException overflow = assertThrows(ArithmeticException.class, () -> Numbers.toLong(hugeDigits));
        assertTrue(overflow.getMessage().startsWith("long overflow: 99999999"), overflow.getMessage());
        assertTrue(overflow.getMessage().length() < 128, overflow.getMessage());
        assertEquals(1L, Numbers.toLong("0".repeat(100_000) + "1"));
        assertEquals(Long.MIN_VALUE, Numbers.toLong("-9223372036854775808"));
        assertThrows(NumberFormatException.class, () -> Numbers.toInt("١٢٣"));
        assertThrows(NumberFormatException.class, () -> Numbers.toInt("１２３"));
    }

    @Test
    public void testToLong() {
        assertEquals(1L, Numbers.toLong("1"));
        assertEquals(1L, Numbers.toLong("1l"));
        assertEquals(0L, Numbers.toLong("0L"));
        assertEquals(0L, Numbers.toLong((String) null));
        assertEquals(0L, Numbers.toLong(""));
        assertEquals(42L, Numbers.toLong(null, 42L));
        assertEquals(1000000L, Numbers.toLong(1000000));
        assertEquals(500L, Numbers.toLong(new BigInteger("500")));
        assertEquals(10L, Numbers.toLong("010"));
        assertEquals(255L, Numbers.toLong("0xFF"));
        assertEquals(255L, Numbers.toLong("#ff"));
        assertEquals(9223372036854775807L, Numbers.toLong("9223372036854775807"));
        assertEquals(123456789012345L, Numbers.toLong(BigInteger.valueOf(123456789012345L), 0L));
        assertThrows(ArithmeticException.class, () -> Numbers.toLong("9223372036854775808"));
        assertThrows(ArithmeticException.class, () -> Numbers.toLong(BigInteger.valueOf(Long.MAX_VALUE).add(BigInteger.ONE), 0L));
        assertThrows(NumberFormatException.class, () -> Numbers.toLong("abc"));
        assertEquals("abc is not a valid Long.", assertThrows(NumberFormatException.class, () -> Numbers.toLong("abc")).getMessage());
    }

    @Test
    @DisplayName("J2: toLong(Object) recovers a non-canonical Number subtype exactly like its byte/short/int siblings")
    public void test_J2_toLongMatchesItsSiblingsForANonCanonicalSubtype() {
        final FormattedNumber formatted = new FormattedNumber(12.9);
        assertEquals("12.9 ms", formatted.toString(), "precondition: toString() is not a numeric token");

        assertEquals(12L, Numbers.toLong(formatted));
        assertEquals(12L, Numbers.toLong(formatted, 0L));
        assertEquals((byte) 12, Numbers.toByte(formatted));
        assertEquals((short) 12, Numbers.toShort(formatted));
        assertEquals(12, Numbers.toInt(formatted));
        assertEquals(Long.valueOf(12L), Numbers.convert(formatted, Long.class));

        // The whole family agrees, which is what the shared wording now promises.
        assertEquals(Numbers.toInt(formatted), (int) Numbers.toLong(formatted));

        // A DoubleAdder is the JDK's own instance of the same shape.
        final DoubleAdder adder = new DoubleAdder();
        adder.add(12.9);
        assertEquals(12L, Numbers.toLong(adder));
        assertEquals(12, Numbers.toInt(adder));
    }

    @Test
    public void testToXxx_UnknownNumber() {
        final DoubleAdder fractional = new DoubleAdder();
        fractional.add(12.9);
        assertEquals((byte) 12, Numbers.toByte(fractional));
        assertEquals(12, Numbers.toInt(fractional));
        assertEquals(12L, Numbers.toLong(fractional));

        final LongAdder adder = new LongAdder();
        adder.add(-5);
        assertEquals(1234, Numbers.toInt(new AtomicInteger(1234)));
        assertEquals(-5L, Numbers.toLong(adder));
        assertEquals(Long.MIN_VALUE, Numbers.toLong(new DecimalTextNumber("-9223372036854775808")));
        assertEquals(Long.MAX_VALUE, Numbers.toLong(new DecimalTextNumber("9223372036854775807.9")));
        assertEquals(42L, Numbers.toLong(new FormattedNumber(42.7)));
        assertEquals(-3, Numbers.toInt(new FormattedNumber(-3.9)));
        assertThrows(ArithmeticException.class, () -> Numbers.toByte(new AtomicInteger(1234)));
        assertThrows(ArithmeticException.class, () -> Numbers.toInt(new FormattedNumber(Double.NaN)));
        assertThrows(ArithmeticException.class, () -> Numbers.toLong(new DecimalTextNumber("9223372036854775808")));
    }

    @Test
    @DisplayName("B1: truncation toward zero matches a BigDecimal reference for every shape")
    public void test_B1_truncationMatchesBigDecimal() {
        final String[] texts = { "0", "-0", "0.0", "-0.0", "1", "-1", "+1", "1.9", "-1.9", "0.9", "-0.9", ".9", "-.9", "1.", "-1.", "010", "-010",
                "00000000000000000000001", "1e0", "1e1", "1e2", "1.5e1", "1.5e3", "1.55e1", "-1.55e1", "1e-1", "1.9e-1", "12345.6789e2", "12345.6789e-2",
                "9223372036854775807", "-9223372036854775808", "9223372036854775807.9", "-9223372036854775808.9", "922337203685477580.7e1",
                "0.000000000000000000009223372036854775807e21", "1e18", "-1e18", "9.223372036854775807e18", "-9.223372036854775808e18", "0.0000000001",
                "-0.0000000001", "1e-999999999", "0e999999999", "-0e999999999", "0.0e5" };

        for (final String text : texts) {
            final BigDecimal reference = new BigDecimal(text);
            final SplitNumber value = n(text);

            // long
            if (reference.compareTo(new BigDecimal(BigInteger.valueOf(Long.MIN_VALUE)).subtract(BigDecimal.ONE)) > 0
                    && reference.compareTo(new BigDecimal(BigInteger.valueOf(Long.MAX_VALUE)).add(BigDecimal.ONE)) < 0) {
                assertEquals(reference.longValue(), Numbers.toLong(value), text);
            } else {
                assertThrows(ArithmeticException.class, () -> Numbers.toLong(value), text);
            }

            // int, and the BigInteger target's truncation. Only computed for a scale toBigInteger() can
            // materialize: "0e999999999" scales up to a billion digits, and "1e-999999999" makes setScale(0)
            // divide by 10^999999999. Those extremes are pinned explicitly below instead.
            if (Math.abs((long) reference.scale()) <= 1_000 && reference.precision() - reference.scale() < 40) {
                final BigInteger truncated = reference.toBigInteger();

                if (truncated.bitLength() < 32) {
                    assertEquals(truncated.intValue(), Numbers.toInt(value), text);
                } else {
                    assertThrows(ArithmeticException.class, () -> Numbers.toInt(value), text);
                }

                assertEquals(truncated, Numbers.convert(value, BigInteger.class), text);
            }
        }

        // The extremes the reference cannot be asked about: an exponent far below one truncates to zero, and a
        // zero significand is zero at any exponent. Both are decided lexically, with no BigDecimal built.
        for (final String text : new String[] { "1e-999999999", "-1e-999999999", "0e999999999", "-0e999999999", "0.0e-999999999" }) {
            assertEquals(0, Numbers.toInt(n(text)), text);
            assertEquals(0L, Numbers.toLong(n(text)), text);
            assertEquals((byte) 0, Numbers.toByte(n(text)), text);
        }

        // ... and the mirror image: a magnitude far above every long is an overflow, not a wrap.
        for (final String text : new String[] { "1e999999999", "-1e999999999", "9.9e19", "1" + "0".repeat(30) }) {
            assertThrows(ArithmeticException.class, () -> Numbers.toLong(n(text)), text);
            assertThrows(ArithmeticException.class, () -> Numbers.toInt(n(text)), text);
        }
    }

    @Test
    public void testToShort() {
        assertEquals((short) 1000, Numbers.toShort("1000"));
        assertEquals((short) 0, Numbers.toShort((String) null));
        assertEquals((short) 0, Numbers.toShort(""));
        assertEquals((short) 42, Numbers.toShort(null, (short) 42));
        assertEquals((short) 100, Numbers.toShort(100));
        assertEquals((short) 100, Numbers.toShort((Object) "100"));
        assertEquals((short) 50, Numbers.toShort((Object) null, (short) 50));
        assertEquals((short) 1234, Numbers.toShort("1234l"));
        assertEquals((short) -32768, Numbers.toShort("-32768"));
        assertEquals((short) 4095, Numbers.toShort("0xFFF"));
        assertThrows(ArithmeticException.class, () -> Numbers.toShort("32768"));
        assertThrows(ArithmeticException.class, () -> Numbers.toShort(40000));
        assertThrows(NumberFormatException.class, () -> Numbers.toShort("abc"));
        assertEquals("short overflow: 40000", assertThrows(ArithmeticException.class, () -> Numbers.toShort("40000")).getMessage());
    }

    @Test
    public void c088_toShortCacheHitNeedsNoRangeCheck() {
        assertEquals((short) 9999, Numbers.toShort("9999", (short) 0));
        assertEquals((short) -999, Numbers.toShort("-999", (short) 0));
        assertEquals((short) 0, Numbers.toShort("0", (short) 7));
        assertEquals((short) -1001, Numbers.toShort("-1001", (short) 0)); // five characters: the scan path
        assertEquals((short) 10000, Numbers.toShort("10000", (short) 0));
        assertEquals((short) 32767, Numbers.toShort("32767", (short) 0));
        assertEquals((short) -32768, Numbers.toShort("-32768", (short) 0));
        assertEquals((short) 7, Numbers.toShort("", (short) 7));
        assertEquals((short) 7, Numbers.toShort(null, (short) 7));
        assertEquals("short overflow: 32768", assertThrows(ArithmeticException.class, () -> Numbers.toShort("32768", (short) 0)).getMessage());
        assertEquals("short overflow: -32769", assertThrows(ArithmeticException.class, () -> Numbers.toShort("-32769", (short) 0)).getMessage());
        assertThrows(NumberFormatException.class, () -> Numbers.toShort("12x", (short) 0));

        // toByte keeps its live check on a cache hit
        assertEquals((byte) 127, Numbers.toByte("127", (byte) 0));
        assertEquals("byte overflow: 128", assertThrows(ArithmeticException.class, () -> Numbers.toByte("128", (byte) 0)).getMessage());
        assertEquals("byte overflow: -129", assertThrows(ArithmeticException.class, () -> Numbers.toByte("-129", (byte) 0)).getMessage());
    }
}

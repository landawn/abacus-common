package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Random;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

/** Performance and memory regression contracts for Numbers. */
@Tag("slow-test")
public class NumbersPerformanceMemoryTest extends TestBase {

    // ---- perf review 2026-10-08 G051 end ----
    // ---- perf review 2026-10-08 G054 begin ----
    @Nested
    class G054 {

        /**
         * The exact binary value truncated toward zero, built the slow way (the reference the fast path must match).
         */
        private java.math.BigInteger exactTruncation(final double d) {
            return new java.math.BigDecimal(d).toBigInteger();
        }

        /**
         * A {@code Number} subtype with an unusable {@code toString()}, so an integral or {@code BigInteger} target falls back to its
         * {@code doubleValue()}.
         */
        private Number opaque(final double d) {
            return new Number() {
                private static final long serialVersionUID = 1L;

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
                    return "n/a";
                }
            };
        }

        private void assertDoubleToBigInteger(final double d) {
            final String why = d + " / bits " + Long.toHexString(Double.doubleToRawLongBits(d));
            final com.landawn.abacus.type.Type<java.math.BigInteger> bigIntegerType = com.landawn.abacus.type.Type.of(java.math.BigInteger.class);

            if (Double.isNaN(d) || Double.isInfinite(d)) {
                final ArithmeticException e1 = assertThrows(ArithmeticException.class, () -> Numbers.convert(Double.valueOf(d), java.math.BigInteger.class),
                        why);
                assertEquals("BigInteger overflow: " + Double.valueOf(d), e1.getMessage(), why);
                final ArithmeticException e2 = assertThrows(ArithmeticException.class, () -> Numbers.convert(Double.valueOf(d), bigIntegerType), why);
                assertEquals("BigInteger overflow: " + Double.valueOf(d), e2.getMessage(), why);
                final ArithmeticException e3 = assertThrows(ArithmeticException.class, () -> Numbers.convert(opaque(d), java.math.BigInteger.class), why);
                assertEquals("BigInteger overflow: n/a", e3.getMessage(), why);
                return;
            }

            final java.math.BigInteger expected = exactTruncation(d);

            assertEquals(expected, Numbers.convert(Double.valueOf(d), java.math.BigInteger.class), why);
            assertEquals(expected, Numbers.convert(Double.valueOf(d), bigIntegerType), why);
            assertEquals(expected, Numbers.convert(Double.valueOf(d), java.math.BigInteger.class, java.math.BigInteger.TEN), why);
            assertEquals(expected, Numbers.convert(opaque(d), java.math.BigInteger.class), why);
            assertEquals(expected, CommonUtil.convert(Double.valueOf(d), java.math.BigInteger.class), why);
        }

        private void assertFloatToBigInteger(final float f) {
            final String why = f + "f / bits " + Integer.toHexString(Float.floatToRawIntBits(f));

            if (Float.isNaN(f) || Float.isInfinite(f)) {
                final ArithmeticException e = assertThrows(ArithmeticException.class, () -> Numbers.convert(Float.valueOf(f), java.math.BigInteger.class), why);
                assertEquals("BigInteger overflow: " + Float.valueOf(f), e.getMessage(), why);
                return;
            }

            // A float widens to double exactly, so the reference is the exact expansion of that double.
            final java.math.BigInteger expected = exactTruncation(f);

            assertEquals(expected, Numbers.convert(Float.valueOf(f), java.math.BigInteger.class), why);
            assertEquals(expected, Numbers.convert(Float.valueOf(f), com.landawn.abacus.type.Type.of(java.math.BigInteger.class)), why);
        }

        // G054-01: doubleToBigInteger fast path inside the long range must equal the exact BigDecimal expansion.
        @Test
        public void testConvertDoubleToBigInteger_boundariesMatchExactTruncation() {
            final double twoTo63 = 0x1p63;
            final double[] values = { 0.0d, -0.0d, Double.MIN_VALUE, -Double.MIN_VALUE, Double.MIN_NORMAL, 0.5d, -0.5d, Math.nextDown(1.0d),
                    -Math.nextDown(1.0d), 1.0d, -1.0d, Math.nextUp(1.0d), -Math.nextUp(1.0d), 1.5d, -1.5d, 1.9999999999999998d, 2.5d, -2.5d, 12.9d, -12.9d,
                    123456.75d, -123456.75d, 2147483647.9d, 2147483648.5d, -2147483648.9d, 0x1p52 + 0.5d, -(0x1p52 + 0.5d), 0x1p53, 0x1p53 + 2, -0x1p53,
                    9007199254740994.0d, 1000000000000000128.0d, -1000000000000000128.0d, 4611686018427388928.0d, -4611686018427388928.0d,
                    Math.nextDown(twoTo63), -Math.nextDown(twoTo63), twoTo63, -twoTo63, Math.nextUp(twoTo63), Math.nextDown(-twoTo63), 1e19d, -1e19d, 1e30d,
                    -1e30d, 0x1p64, -0x1p64, Double.MAX_VALUE, -Double.MAX_VALUE, Long.MAX_VALUE, Long.MIN_VALUE, Integer.MAX_VALUE, Integer.MIN_VALUE,
                    Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY, Double.longBitsToDouble(0x7ff8000000000001L) };

            for (final double d : values) {
                assertDoubleToBigInteger(d);
            }

            // Named answers, independent of the reference expansion.
            assertEquals(java.math.BigInteger.valueOf(Long.MIN_VALUE), Numbers.convert(-twoTo63, java.math.BigInteger.class));
            assertEquals(java.math.BigInteger.valueOf(9223372036854774784L), Numbers.convert(Math.nextDown(twoTo63), java.math.BigInteger.class));
            assertEquals(new java.math.BigInteger("9223372036854775808"), Numbers.convert(twoTo63, java.math.BigInteger.class));
            assertEquals(new java.math.BigInteger("-9223372036854777856"), Numbers.convert(Math.nextDown(-twoTo63), java.math.BigInteger.class));
            assertEquals(java.math.BigInteger.valueOf(12), Numbers.convert(12.9d, java.math.BigInteger.class));
            assertEquals(java.math.BigInteger.valueOf(-12), Numbers.convert(-12.9d, java.math.BigInteger.class));
            assertEquals(java.math.BigInteger.valueOf(2147483648L), Numbers.convert(2147483648.5d, java.math.BigInteger.class));
            assertSame(java.math.BigInteger.ZERO, Numbers.convert(-0.0d, java.math.BigInteger.class));
            assertSame(java.math.BigInteger.ZERO, Numbers.convert(-0.999d, java.math.BigInteger.class));
            assertEquals(0, Numbers.convert(-0.5d, java.math.BigInteger.class).signum());
        }

        // G054-01: random doubles (raw bit patterns and magnitudes on both sides of 2^63) match the exact expansion.
        @Test
        public void testConvertDoubleToBigInteger_randomMatchesExactTruncation() {
            final Random random = new Random(20261008L);

            for (int i = 0; i < 20_000; i++) {
                assertDoubleToBigInteger(Double.longBitsToDouble(random.nextLong()));
            }

            for (int i = 0; i < 40_000; i++) {
                // Exponents from 2^-3 to 2^70: below one, the whole long range, the 2^63 edge and beyond it.
                double d = Math.scalb(1.0d + random.nextDouble(), random.nextInt(74) - 3);

                if (random.nextBoolean()) {
                    d = -d;
                }

                assertDoubleToBigInteger(d);
            }

            for (int i = 0; i < 2_000; i++) {
                // The doubles adjacent to +-2^63.
                final double edge = random.nextBoolean() ? 0x1p63 : -0x1p63;
                double d = edge;
                final int steps = random.nextInt(8);

                for (int s = 0; s < steps; s++) {
                    d = random.nextBoolean() ? Math.nextUp(d) : Math.nextDown(d);
                }

                assertDoubleToBigInteger(d);
            }
        }

        // G054-01: Float sources take the same path (widened exactly to double).
        @Test
        public void testConvertFloatToBigInteger_matchesExactTruncation() {
            final float[] values = { 0.0f, -0.0f, Float.MIN_VALUE, -Float.MIN_VALUE, 0.5f, -0.5f, Math.nextDown(1.0f), 1.0f, -1.0f, 1.5f, -1.5f, 12.9f, -12.9f,
                    1.21f, 16777217f, 33554448f, -33554448f, Integer.MAX_VALUE, Integer.MIN_VALUE, 0x1p63f, -0x1p63f, Math.nextDown(0x1p63f),
                    -Math.nextDown(0x1p63f), Math.nextUp(0x1p63f), Math.nextDown(-0x1p63f), 1e19f, -1e19f, 1e30f, Float.MAX_VALUE, -Float.MAX_VALUE, Float.NaN,
                    Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY };

            for (final float f : values) {
                assertFloatToBigInteger(f);
            }

            final Random random = new Random(8100L);

            for (int i = 0; i < 20_000; i++) {
                assertFloatToBigInteger(Float.intBitsToFloat(random.nextInt()));
            }

            for (int i = 0; i < 20_000; i++) {
                float f = Math.scalb(1.0f + random.nextFloat(), random.nextInt(70) - 3);

                if (random.nextBoolean()) {
                    f = -f;
                }

                assertFloatToBigInteger(f);
            }

            assertEquals(java.math.BigInteger.valueOf(2147483648L), Numbers.convert((float) Integer.MAX_VALUE, java.math.BigInteger.class));
            assertEquals(java.math.BigInteger.valueOf(Long.MIN_VALUE), Numbers.convert(-0x1p63f, java.math.BigInteger.class));
            assertEquals(new java.math.BigInteger("9223372036854775808"), Numbers.convert(0x1p63f, java.math.BigInteger.class));
        }
    }

    // ---- perf review 2026-10-08 G054 end ----
    // ---- perf review 2026-10-08 G055 begin ----
    @Nested
    class G055 {

        /**
         * Independent exact reference for C(n, k): after step {@code i} the running value is C(n, i + 1), so every
         * division is exact.
         */
        private java.math.BigInteger referenceBinomial(final int n, final int k) {
            java.math.BigInteger result = java.math.BigInteger.ONE;

            for (int i = 0; i < k; i++) {
                result = result.multiply(java.math.BigInteger.valueOf((long) n - i)).divide(java.math.BigInteger.valueOf(i + 1L));
            }

            return result;
        }

        // G055-01: around the lower edge of the product window (k = 64), both orientations, small to maximal n
        @Test
        void testBinomialToBigInteger_lowerWindowEdgeMatchesReference() {
            final int[] ns = { 116, 126, 127, 128, 129, 130, 131, 140, 200, 1000, 12345, 65536, 1 << 20, Integer.MAX_VALUE - 1, Integer.MAX_VALUE };

            for (final int n : ns) {
                for (int k = 56; k <= 72 && k <= n; k++) {
                    final int kk = k;
                    final java.math.BigInteger expected = referenceBinomial(n, Math.min(k, n - k));

                    assertEquals(expected, Numbers.binomialToBigInteger(n, k), () -> "C(" + n + ", " + kk + ")");
                    assertEquals(expected, Numbers.binomialToBigInteger(n, n - k), () -> "C(" + n + ", " + (n - kk) + ")");
                }
            }
        }

        // G055-01: fixed-seed random shapes inside the window, both orientations
        @Test
        void testBinomialToBigInteger_randomWindowShapesMatchReference() {
            final Random random = new Random(55L);

            for (int i = 0; i < 90; i++) {
                final int k = 64 + random.nextInt(i % 10 == 0 ? 2500 : 600);
                final int n = i % 3 == 0 ? 2 * k + random.nextInt(3 * k)
                        : i % 3 == 1 ? 2 * k + random.nextInt(1 << 22) : Integer.MAX_VALUE - random.nextInt(1 << 16);
                final java.math.BigInteger expected = referenceBinomial(n, k);

                assertEquals(expected, Numbers.binomialToBigInteger(n, k), () -> "C(" + n + ", " + k + ")");
                assertEquals(expected, Numbers.binomialToBigInteger(n, n - k), () -> "C(" + n + ", " + (n - k) + ")");
            }

            assertEquals(referenceBinomial(5000, 2500), Numbers.binomialToBigInteger(5000, 2500));
            assertEquals(referenceBinomial(20001, 10000), Numbers.binomialToBigInteger(20001, 10001));
            assertEquals(referenceBinomial(1_000_000, 3000), Numbers.binomialToBigInteger(1_000_000, 3000));
        }

        // G055-01: both window edges cross-checked against the batched path through exact identities
        @Test
        void testBinomialToBigInteger_windowEdgesAgreeWithBatchedPath() {
            // C(n, 64) * 64 == C(n, 63) * (n - 63): k = 63 stays on the batched loop, k = 64 takes the products.
            for (final int n : new int[] { 128, 129, 130, 1000, 99991, Integer.MAX_VALUE }) {
                assertEquals(Numbers.binomialToBigInteger(n, 63).multiply(java.math.BigInteger.valueOf(n - 63L)),
                        Numbers.binomialToBigInteger(n, 64).multiply(java.math.BigInteger.valueOf(64)), "n = " + n);
            }

            // Pascal's rule across the upper edge: C(131074, 65537) keeps k = 65537 (batched loop), while
            // C(131073, 65536) and C(131073, 65537) both reduce to k = 65536, the last k of the product window.
            assertEquals(Numbers.binomialToBigInteger(131074, 65537),
                    Numbers.binomialToBigInteger(131073, 65536).add(Numbers.binomialToBigInteger(131073, 65537)));

            // Symmetry and argument checks are unchanged.
            assertEquals(Numbers.binomialToBigInteger(131073, 65536), Numbers.binomialToBigInteger(131073, 65537));
            assertThrows(IllegalArgumentException.class, () -> Numbers.binomialToBigInteger(1000, 1001));
            assertThrows(IllegalArgumentException.class, () -> Numbers.binomialToBigInteger(1000, -64));
            assertThrows(IllegalArgumentException.class, () -> Numbers.binomialToBigInteger(-1, 64));
        }
    }
}

/*
 * Copyright (C) 2017 HaiYang Li
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.landawn.abacus.util;

import static java.lang.Double.MAX_EXPONENT;
import static java.lang.Double.MIN_EXPONENT;
import static java.lang.Double.POSITIVE_INFINITY;
import static java.lang.Double.doubleToRawLongBits;
import static java.lang.Double.isNaN;
import static java.lang.Double.longBitsToDouble;
import static java.lang.Math.abs;
import static java.lang.Math.getExponent;
import static java.lang.Math.min;
import static java.math.RoundingMode.CEILING;
import static java.math.RoundingMode.FLOOR;
import static java.math.RoundingMode.HALF_EVEN;
import static java.math.RoundingMode.HALF_UP;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 *  Note: A lot of codes in this classed are copied from Google Guava and Apache Commons Math under under the Apache License, Version 2.0.
 *  The purpose of copying the code is to re-organize the APIs.
 *
 */
public abstract class Matth {
    private Matth() {
        // utility class.
    }

    private static final long ONE_BITS = doubleToRawLongBits(1.0);

    /** The biggest half power of two that can fit in an unsigned int. */
    static final int INT_MAX_POWER_OF_SQRT2_UNSIGNED = 0xB504F333;

    /** The biggest half power of two that fits into an unsigned long */
    static final long MAX_POWER_OF_SQRT2_UNSIGNED = 0xB504F333F9DE6484L;

    static final long MAX_SIGNED_POWER_OF_TWO = 1L << (Long.SIZE - 2);

    static final long FLOOR_SQRT_MAX_LONG = 3037000499L;

    static final int FLOOR_SQRT_MAX_INT = 46340;

    // The mask for the significand, according to the {@link
    // Double#doubleToRawLongBits(double)} spec.
    static final long SIGNIFICAND_MASK = 0x000fffffffffffffL;

    static final int SIGNIFICAND_BITS = 52;

    // The mask for the exponent, according to the {@link
    // Double#doubleToRawLongBits(double)} spec.
    static final long EXPONENT_MASK = 0x7ff0000000000000L;

    // The mask for the sign, according to the {@link
    // Double#doubleToRawLongBits(double)} spec.
    static final long SIGN_MASK = 0x8000000000000000L;

    static final int EXPONENT_BIAS = 1023;

    /**
     * The implicit 1 bit that is omitted in significands of normal doubles.
     */
    static final long IMPLICIT_BIT = SIGNIFICAND_MASK + 1;

    private static final double MIN_INT_AS_DOUBLE = -0x1p31;

    private static final double MAX_INT_AS_DOUBLE = 0x1p31 - 1.0;

    private static final double MIN_LONG_AS_DOUBLE = -0x1p63;

    /*
     * We cannot store Long.MAX_VALUE as a double without losing precision. Instead, we store
     * Long.MAX_VALUE + 1 == -Long.MIN_VALUE, and then offset all comparisons by 1.
     */
    private static final double MAX_LONG_AS_DOUBLE_PLUS_ONE = 0x1p63;

    // maxLog10ForLeadingZeros[i] == floor(log10(2^(Long.SIZE - i)))
    static final byte[] int_maxLog10ForLeadingZeros = { 9, 9, 9, 8, 8, 8, 7, 7, 7, 6, 6, 6, 6, 5, 5, 5, 4, 4, 4, 3, 3, 3, 3, 2, 2, 2, 1, 1, 1, 0, 0, 0, 0 };

    static final int[] int_powersOf10 = { 1, 10, 100, 1000, 10000, 100000, 1000000, 10000000, 100000000, 1000000000 };

    private static final int[] int_factorials = { 1, 1, 1 * 2, 1 * 2 * 3, 1 * 2 * 3 * 4, 1 * 2 * 3 * 4 * 5, 1 * 2 * 3 * 4 * 5 * 6, 1 * 2 * 3 * 4 * 5 * 6 * 7,
            1 * 2 * 3 * 4 * 5 * 6 * 7 * 8, 1 * 2 * 3 * 4 * 5 * 6 * 7 * 8 * 9, 1 * 2 * 3 * 4 * 5 * 6 * 7 * 8 * 9 * 10,
            1 * 2 * 3 * 4 * 5 * 6 * 7 * 8 * 9 * 10 * 11, 1 * 2 * 3 * 4 * 5 * 6 * 7 * 8 * 9 * 10 * 11 * 12 };

    // binomial(biggestBinomials[k], k) fits in an int, but not binomial(biggestBinomials[k]+1,k).
    static int[] int_biggestBinomials = { Integer.MAX_VALUE, Integer.MAX_VALUE, 65536, 2345, 477, 193, 110, 75, 58, 49, 43, 39, 37, 35, 34, 34, 33 };

    // halfPowersOf10[i] = largest int less than 10^(i + 0.5)
    static final int[] int_halfPowersOf10 = { 3, 31, 316, 3162, 31622, 316227, 3162277, 31622776, 316227766, Integer.MAX_VALUE };

    // maxLog10ForLeadingZeros[i] == floor(log10(2^(Long.SIZE - i))) 
    static final byte[] maxLog10ForLeadingZeros = { 19, 18, 18, 18, 18, 17, 17, 17, 16, 16, 16, 15, 15, 15, 15, 14, 14, 14, 13, 13, 13, 12, 12, 12, 12, 11, 11,
            11, 10, 10, 10, 9, 9, 9, 9, 8, 8, 8, 7, 7, 7, 6, 6, 6, 6, 5, 5, 5, 4, 4, 4, 3, 3, 3, 3, 2, 2, 2, 1, 1, 1, 0, 0, 0 };

    static final long[] powersOf10 = { 1L, 10L, 100L, 1000L, 10000L, 100000L, 1000000L, 10000000L, 100000000L, 1000000000L, 10000000000L, 100000000000L,
            1000000000000L, 10000000000000L, 100000000000000L, 1000000000000000L, 10000000000000000L, 100000000000000000L, 1000000000000000000L };

    // halfPowersOf10[i] = largest long less than 10^(i + 0.5)  
    static final long[] halfPowersOf10 = { 3L, 31L, 316L, 3162L, 31622L, 316227L, 3162277L, 31622776L, 316227766L, 3162277660L, 31622776601L, 316227766016L,
            3162277660168L, 31622776601683L, 316227766016837L, 3162277660168379L, 31622776601683793L, 316227766016837933L, 3162277660168379331L };

    static final long[] long_factorials = { 1L, 1L, 1L * 2, 1L * 2 * 3, 1L * 2 * 3 * 4, 1L * 2 * 3 * 4 * 5, 1L * 2 * 3 * 4 * 5 * 6, 1L * 2 * 3 * 4 * 5 * 6 * 7,
            1L * 2 * 3 * 4 * 5 * 6 * 7 * 8, 1L * 2 * 3 * 4 * 5 * 6 * 7 * 8 * 9, 1L * 2 * 3 * 4 * 5 * 6 * 7 * 8 * 9 * 10,
            1L * 2 * 3 * 4 * 5 * 6 * 7 * 8 * 9 * 10 * 11, 1L * 2 * 3 * 4 * 5 * 6 * 7 * 8 * 9 * 10 * 11 * 12,
            1L * 2 * 3 * 4 * 5 * 6 * 7 * 8 * 9 * 10 * 11 * 12 * 13, 1L * 2 * 3 * 4 * 5 * 6 * 7 * 8 * 9 * 10 * 11 * 12 * 13 * 14,
            1L * 2 * 3 * 4 * 5 * 6 * 7 * 8 * 9 * 10 * 11 * 12 * 13 * 14 * 15, 1L * 2 * 3 * 4 * 5 * 6 * 7 * 8 * 9 * 10 * 11 * 12 * 13 * 14 * 15 * 16,
            1L * 2 * 3 * 4 * 5 * 6 * 7 * 8 * 9 * 10 * 11 * 12 * 13 * 14 * 15 * 16 * 17,
            1L * 2 * 3 * 4 * 5 * 6 * 7 * 8 * 9 * 10 * 11 * 12 * 13 * 14 * 15 * 16 * 17 * 18,
            1L * 2 * 3 * 4 * 5 * 6 * 7 * 8 * 9 * 10 * 11 * 12 * 13 * 14 * 15 * 16 * 17 * 18 * 19,
            1L * 2 * 3 * 4 * 5 * 6 * 7 * 8 * 9 * 10 * 11 * 12 * 13 * 14 * 15 * 16 * 17 * 18 * 19 * 20 };

    /*
     * binomial(biggestBinomials[k], k) fits in a long, but not binomial(biggestBinomials[k] + 1, k).
     */
    static final int[] biggestBinomials = { Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE, 3810779, 121977, 16175, 4337, 1733, 887, 534, 361, 265,
            206, 169, 143, 125, 111, 101, 94, 88, 83, 79, 76, 74, 72, 70, 69, 68, 67, 67, 66, 66, 66, 66 };

    /*
     * binomial(biggestSimpleBinomials[k], k) doesn't need to use the slower GCD-based impl, but
     * binomial(biggestSimpleBinomials[k] + 1, k) does.
     */
    static final int[] biggestSimpleBinomials = { Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE, 2642246, 86251, 11724, 3218, 1313, 684, 419, 287,
            214, 169, 139, 119, 105, 95, 87, 81, 76, 73, 70, 68, 66, 64, 63, 62, 62, 61, 61, 61 };

    /*
     * This bitmask is used as an optimization for cheaply testing for divisiblity by 2, 3, or 5.
     * Each bit is set to 1 for all remainders that indicate divisibility by 2, 3, or 5, so
     * 1, 7, 11, 13, 17, 19, 23, 29 are set to 0. 30 and up don't matter because they won't be hit.
     */
    private static final int SIEVE_30 = ~((1 << 1) | (1 << 7) | (1 << 11) | (1 << 13) | (1 << 17) | (1 << 19) | (1 << 23) | (1 << 29));

    /*
     * If n <= millerRabinBases[i][0], then testing n against bases millerRabinBases[i][1..] suffices
     * to prove its primality. Values from miller-rabin.appspot.com.
     *
     * NOTE: We could get slightly better bases that would be treated as unsigned, but benchmarks
     * showed negligible performance improvements.
     */
    private static final long[][] millerRabinBaseSets = { { 291830, 126401071349994536L }, { 885594168, 725270293939359937L, 3569819667048198375L },
            { 273919523040L, 15, 7363882082L, 992620450144556L }, { 47636622961200L, 2, 2570940, 211991001, 3749873356L },
            { 7999252175582850L, 2, 4130806001517L, 149795463772692060L, 186635894390467037L, 3967304179347715805L },
            { 585226005592931976L, 2, 123635709730000L, 9233062284813009L, 43835965440333360L, 761179012939631437L, 1263739024124850375L },
            { Long.MAX_VALUE, 2, 325, 9375, 28178, 450775, 9780504, 1795265022 } };

    /** Constant: {@value}. */
    static final double F_1_3 = 1d / 3d;
    /** Constant: {@value}. */
    static final double F_1_5 = 1d / 5d;
    /** Constant: {@value}. */
    static final double F_1_7 = 1d / 7d;
    /** Constant: {@value}. */
    static final double F_1_9 = 1d / 9d;
    /** Constant: {@value}. */
    static final double F_1_11 = 1d / 11d;
    /** Constant: {@value}. */
    static final double F_1_13 = 1d / 13d;
    /** Constant: {@value}. */
    static final double F_1_15 = 1d / 15d;
    /** Constant: {@value}. */
    static final double F_1_17 = 1d / 17d;
    /** Constant: {@value}. */
    static final double F_3_4 = 3d / 4d;
    /** Constant: {@value}. */
    static final double F_15_16 = 15d / 16d;
    /** Constant: {@value}. */
    static final double F_13_14 = 13d / 14d;
    /** Constant: {@value}. */
    static final double F_11_12 = 11d / 12d;
    /** Constant: {@value}. */
    static final double F_9_10 = 9d / 10d;
    /** Constant: {@value}. */
    static final double F_7_8 = 7d / 8d;
    /** Constant: {@value}. */
    static final double F_5_6 = 5d / 6d;
    /** Constant: {@value}. */
    static final double F_1_2 = 1d / 2d;
    /** Constant: {@value}. */
    static final double F_1_4 = 1d / 4d;

    //    /**
    //     * Primality test: tells if the argument is a (provable) prime or not. 
    //     * 
    //     * @param n number to test.
    //     * @return true if n is prime. (All numbers &lt; 2 return false).
    //     */
    //    public static boolean isPrime(int n) {
    //        if (n < 2) {
    //            return false;
    //        } else if (n < 4) {
    //            return true;
    //        }
    //
    //        for (int i = 2, to = (int) Math.sqrt(n); i <= to; i++) {
    //            if (n % i == 0) {
    //                return false;
    //            }
    //        }
    //
    //        return true;
    //    }

    /**
     * Returns {@code true} if {@code n} is a
     * <a href="http://mathworld.wolfram.com/PrimeNumber.html">prime number</a>: an integer <i>greater
     * than one</i> that cannot be factored into a product of <i>smaller</i> positive integers.
     * Returns {@code false} if {@code n} is zero, one, or a composite number (one which <i>can</i>
     * be factored into smaller positive integers).
     *
     * <p>To test larger numbers, use {@link BigInteger#isProbablePrime}.
     *
     * @throws IllegalArgumentException if {@code n} is negative
     * @since 20.0
     */
    public static boolean isPrime(long n) {
        if (n < 2) {
            checkNonNegative("n", n);
            return false;
        }
        if (n < 14 && (n == 2 || n == 3 || n == 5 || n == 7 || n == 11 || n == 13)) {
            return true;
        }

        if ((SIEVE_30 & (1 << (n % 30))) != 0) {
            return false;
        }
        if (n % 7 == 0 || n % 11 == 0 || n % 13 == 0) {
            return false;
        }
        if (n < 17 * 17) {
            return true;
        }

        for (long[] baseSet : millerRabinBaseSets) {
            if (n <= baseSet[0]) {
                for (int i = 1; i < baseSet.length; i++) {
                    if (!MillerRabinTester.test(baseSet[i], n)) {
                        return false;
                    }
                }
                return true;
            }
        }
        throw new AssertionError();
    }

    public static boolean isPerfectSquare(int n) {
        if (n < 0) {
            return false;
        }

        switch (n & 0xF) {
            case 0:
            case 1:
            case 4:
            case 9:
                long tst = (long) Math.sqrt(n);
                return tst * tst == n;

            default:
                return false;
        }
    }

    public static boolean isPerfectSquare(long n) {
        if (n < 0) {
            return false;
        }

        switch ((int) (n & 0xF)) {
            case 0:
            case 1:
            case 4:
            case 9:
                long tst = (long) Math.sqrt(n);
                return tst * tst == n;

            default:
                return false;
        }
    }

    public static boolean isPowerOfTwo(int x) {
        return x > 0 & (x & (x - 1)) == 0;
    }

    public static boolean isPowerOfTwo(long x) {
        return x > 0 & (x & (x - 1)) == 0;
    }

    public static boolean isPowerOfTwo(double x) {
        return x > 0.0 && isFinite(x) && isPowerOfTwo(getSignificand(x));
    }

    /**
     * Returns {@code true} if {@code x} represents a power of two.
     */
    public static boolean isPowerOfTwo(BigInteger x) {
        N.checkArgNotNull(x);
        return x.signum() > 0 && x.getLowestSetBit() == x.bitLength() - 1;
    }

    //    public static boolean isPowerOfFour(int n) {
    //        return (n > 0) && ((n & (n - 1)) == 0) && ((n & 0x55555555) == n);
    //    }
    //
    //    public static boolean isPowerOfFour(long n) {
    //        return (n > 0) && ((n & (n - 1)) == 0) && ((n & 0x5555555555555555L) == n);
    //    }

    public static double log(double a) {
        return Math.log(a);
    }

    public static int log2(int x, RoundingMode mode) {
        checkPositive("x", x);
        switch (mode) {
            case UNNECESSARY:
                checkRoundingUnnecessary(isPowerOfTwo(x));
                // fall through
            case DOWN:
            case FLOOR:
                return (Integer.SIZE - 1) - Integer.numberOfLeadingZeros(x);

            case UP:
            case CEILING:
                return Integer.SIZE - Integer.numberOfLeadingZeros(x - 1);

            case HALF_DOWN:
            case HALF_UP:
            case HALF_EVEN:
                // Since sqrt(2) is irrational, log2(x) - logFloor cannot be exactly 0.5
                int leadingZeros = Integer.numberOfLeadingZeros(x);
                int cmp = INT_MAX_POWER_OF_SQRT2_UNSIGNED >>> leadingZeros;
                // floor(2^(logFloor + 0.5))
                int logFloor = (Integer.SIZE - 1) - leadingZeros;
                return logFloor + lessThanBranchFree(cmp, x);

            default:
                throw new AssertionError();
        }
    }

    /**
     * Returns the base-2 logarithm of {@code x}, rounded according to the specified rounding mode.
     *
     * @throws IllegalArgumentException if {@code x <= 0}
     * @throws ArithmeticException if {@code mode} is {@link RoundingMode#UNNECESSARY} and {@code x}
     *     is not a power of two
     */
    @SuppressWarnings("fallthrough")
    // TODO(kevinb): remove after this warning is disabled globally
    public static int log2(long x, RoundingMode mode) {
        checkPositive("x", x);
        switch (mode) {
            case UNNECESSARY:
                checkRoundingUnnecessary(isPowerOfTwo(x));
                // fall through
            case DOWN:
            case FLOOR:
                return (Long.SIZE - 1) - Long.numberOfLeadingZeros(x);

            case UP:
            case CEILING:
                return Long.SIZE - Long.numberOfLeadingZeros(x - 1);

            case HALF_DOWN:
            case HALF_UP:
            case HALF_EVEN:
                // Since sqrt(2) is irrational, log2(x) - logFloor cannot be exactly 0.5
                int leadingZeros = Long.numberOfLeadingZeros(x);
                long cmp = MAX_POWER_OF_SQRT2_UNSIGNED >>> leadingZeros;
                // floor(2^(logFloor + 0.5))
                int logFloor = (Long.SIZE - 1) - leadingZeros;
                return logFloor + lessThanBranchFree(cmp, x);

            default:
                throw new AssertionError("impossible");
        }
    }

    /**
     * Returns the base 2 logarithm of a double value.
     *
     * <p>Special cases:
     * <ul>
     * <li>If {@code x} is NaN or less than zero, the result is NaN.
     * <li>If {@code x} is positive infinity, the result is positive infinity.
     * <li>If {@code x} is positive or negative zero, the result is negative infinity.
     * </ul>
     *
     * <p>The computed result is within 1 ulp of the exact result.
     *
     * <p>If the result of this method will be immediately rounded to an {@code int},
     * {@link #log2(double, RoundingMode)} is faster.
     */
    public static double log2(double x) {
        return Math.log(x) / LN_2; // surprisingly within 1 ulp according to tests
    }

    /**
     * Returns the base 2 logarithm of a double value, rounded with the specified rounding mode to an
     * {@code int}.
     *
     * <p>Regardless of the rounding mode, this is faster than {@code (int) log2(x)}.
     *
     * @throws IllegalArgumentException if {@code x <= 0.0}, {@code x} is NaN, or {@code x} is
     *     infinite
     */
    @SuppressWarnings("fallthrough")
    public static int log2(double x, RoundingMode mode) {
        N.checkArgument(x > 0.0 && isFinite(x), "x must be positive and finite");
        int exponent = getExponent(x);
        if (!isNormal(x)) {
            return log2(x * IMPLICIT_BIT, mode) - SIGNIFICAND_BITS;
            // Do the calculation on a normal value.
        }
        // x is positive, finite, and normal
        boolean increment;
        switch (mode) {
            case UNNECESSARY:
                checkRoundingUnnecessary(isPowerOfTwo(x));
                // fall through
            case FLOOR:
                increment = false;
                break;
            case CEILING:
                increment = !isPowerOfTwo(x);
                break;
            case DOWN:
                increment = exponent < 0 & !isPowerOfTwo(x);
                break;
            case UP:
                increment = exponent >= 0 & !isPowerOfTwo(x);
                break;
            case HALF_DOWN:
            case HALF_EVEN:
            case HALF_UP:
                double xScaled = scaleNormalize(x);
                // sqrt(2) is irrational, and the spec is relative to the "exact numerical result,"
                // so log2(x) is never exactly exponent + 0.5.
                increment = (xScaled * xScaled) > 2.0;
                break;
            default:
                throw new AssertionError();
        }
        return increment ? exponent + 1 : exponent;
    }

    /**
     * Returns the base-2 logarithm of {@code x}, rounded according to the specified rounding mode.
     *
     * @throws IllegalArgumentException if {@code x <= 0}
     * @throws ArithmeticException if {@code mode} is {@link RoundingMode#UNNECESSARY} and {@code x}
     *     is not a power of two
     */
    @SuppressWarnings("fallthrough")
    // TODO(kevinb): remove after this warning is disabled globally
    public static int log2(BigInteger x, RoundingMode mode) {
        checkPositive("x", N.checkArgNotNull(x));
        int logFloor = x.bitLength() - 1;
        switch (mode) {
            case UNNECESSARY:
                checkRoundingUnnecessary(isPowerOfTwo(x)); // fall through
            case DOWN:
            case FLOOR:
                return logFloor;

            case UP:
            case CEILING:
                return isPowerOfTwo(x) ? logFloor : logFloor + 1;

            case HALF_DOWN:
            case HALF_UP:
            case HALF_EVEN:
                if (logFloor < SQRT2_PRECOMPUTE_THRESHOLD) {
                    BigInteger halfPower = SQRT2_PRECOMPUTED_BITS.shiftRight(SQRT2_PRECOMPUTE_THRESHOLD - logFloor);
                    if (x.compareTo(halfPower) <= 0) {
                        return logFloor;
                    } else {
                        return logFloor + 1;
                    }
                }
                // Since sqrt(2) is irrational, log2(x) - logFloor cannot be exactly 0.5
                //
                // To determine which side of logFloor.5 the logarithm is,
                // we compare x^2 to 2^(2 * logFloor + 1).
                BigInteger x2 = x.pow(2);
                int logX2Floor = x2.bitLength() - 1;
                return (logX2Floor < 2 * logFloor + 1) ? logFloor : logFloor + 1;

            default:
                throw new AssertionError();
        }
    }

    public static int log10(int x, RoundingMode mode) {
        checkPositive("x", x);
        int logFloor = log10Floor(x);
        int floorPow = int_powersOf10[logFloor];
        switch (mode) {
            case UNNECESSARY:
                checkRoundingUnnecessary(x == floorPow);
                // fall through
            case FLOOR:
            case DOWN:
                return logFloor;
            case CEILING:
            case UP:
                return logFloor + lessThanBranchFree(floorPow, x);
            case HALF_DOWN:
            case HALF_UP:
            case HALF_EVEN:
                // sqrt(10) is irrational, so log10(x) - logFloor is never exactly 0.5
                return logFloor + lessThanBranchFree(int_halfPowersOf10[logFloor], x);
            default:
                throw new AssertionError();
        }
    }

    private static int log10Floor(int x) {
        /*
         * Based on Hacker's Delight Fig. 11-5, the two-table-lookup, branch-free implementation.
         *
         * The key idea is that based on the number of leading zeros (equivalently, floor(log2(x))),
         * we can narrow the possible floor(log10(x)) values to two.  For example, if floor(log2(x))
         * is 6, then 64 <= x < 128, so floor(log10(x)) is either 1 or 2.
         */
        int y = int_maxLog10ForLeadingZeros[Integer.numberOfLeadingZeros(x)];
        /*
         * y is the higher of the two possible values of floor(log10(x)). If x < 10^y, then we want the
         * lower of the two possible values, or y - 1, otherwise, we want y.
         */
        return y - lessThanBranchFree(x, int_powersOf10[y]);
    }

    /**
     * Returns the base-10 logarithm of {@code x}, rounded according to the specified rounding mode.
     *
     * @throws IllegalArgumentException if {@code x <= 0}
     * @throws ArithmeticException if {@code mode} is {@link RoundingMode#UNNECESSARY} and {@code x}
     *     is not a power of ten
     */
    @SuppressWarnings("fallthrough")
    // TODO(kevinb): remove after this warning is disabled globally
    public static int log10(long x, RoundingMode mode) {
        checkPositive("x", x);
        int logFloor = log10Floor(x);
        long floorPow = powersOf10[logFloor];
        switch (mode) {
            case UNNECESSARY:
                checkRoundingUnnecessary(x == floorPow);
                // fall through
            case FLOOR:
            case DOWN:
                return logFloor;
            case CEILING:
            case UP:
                return logFloor + lessThanBranchFree(floorPow, x);
            case HALF_DOWN:
            case HALF_UP:
            case HALF_EVEN:
                // sqrt(10) is irrational, so log10(x)-logFloor is never exactly 0.5
                return logFloor + lessThanBranchFree(halfPowersOf10[logFloor], x);
            default:
                throw new AssertionError();
        }
    }

    public static double log10(double x) {
        return Math.log10(x);
    }

    /*
     * The maximum number of bits in a square root for which we'll precompute an explicit half power
     * of two. This can be any value, but higher values incur more class load time and linearly
     * increasing memory consumption.
     */
    static final int SQRT2_PRECOMPUTE_THRESHOLD = 256;

    static final BigInteger SQRT2_PRECOMPUTED_BITS = new BigInteger("16a09e667f3bcc908b2fb1366ea957d3e3adec17512775099da2f590b0667322a", 16);

    /**
     * Returns the base-10 logarithm of {@code x}, rounded according to the specified rounding mode.
     *
     * @throws IllegalArgumentException if {@code x <= 0}
     * @throws ArithmeticException if {@code mode} is {@link RoundingMode#UNNECESSARY} and {@code x}
     *     is not a power of ten
     */
    @SuppressWarnings("fallthrough")
    public static int log10(BigInteger x, RoundingMode mode) {
        checkPositive("x", x);
        if (fitsInLong(x)) {
            return log10(x.longValue(), mode);
        }

        int approxLog10 = (int) (log2(x, FLOOR) * LN_2 / LN_10);
        BigInteger approxPow = BigInteger.TEN.pow(approxLog10);
        int approxCmp = approxPow.compareTo(x);

        /*
         * We adjust approxLog10 and approxPow until they're equal to floor(log10(x)) and
         * 10^floor(log10(x)).
         */

        if (approxCmp > 0) {
            /*
             * The code is written so that even completely incorrect approximations will still yield the
             * correct answer eventually, but in practice this branch should almost never be entered, and
             * even then the loop should not run more than once.
             */
            do {
                approxLog10--;
                approxPow = approxPow.divide(BigInteger.TEN);
                approxCmp = approxPow.compareTo(x);
            } while (approxCmp > 0);
        } else {
            BigInteger nextPow = BigInteger.TEN.multiply(approxPow);
            int nextCmp = nextPow.compareTo(x);
            while (nextCmp <= 0) {
                approxLog10++;
                approxPow = nextPow;
                approxCmp = nextCmp;
                nextPow = BigInteger.TEN.multiply(approxPow);
                nextCmp = nextPow.compareTo(x);
            }
        }

        int floorLog = approxLog10;
        BigInteger floorPow = approxPow;
        int floorCmp = approxCmp;

        switch (mode) {
            case UNNECESSARY:
                checkRoundingUnnecessary(floorCmp == 0);
                // fall through
            case FLOOR:
            case DOWN:
                return floorLog;

            case CEILING:
            case UP:
                return floorPow.equals(x) ? floorLog : floorLog + 1;

            case HALF_DOWN:
            case HALF_UP:
            case HALF_EVEN:
                // Since sqrt(10) is irrational, log10(x) - floorLog can never be exactly 0.5
                BigInteger x2 = x.pow(2);
                BigInteger halfPowerSquared = floorPow.pow(2).multiply(BigInteger.TEN);
                return (x2.compareTo(halfPowerSquared) <= 0) ? floorLog : floorLog + 1;
            default:
                throw new AssertionError();
        }
    }

    static boolean fitsInLong(BigInteger x) {
        return x.bitLength() <= Long.SIZE - 1;
    }

    private static final double LN_10 = Math.log(10);
    private static final double LN_2 = Math.log(2);

    public static int pow(int b, int k) {
        checkNonNegative("exponent", k);
        switch (b) {
            case 0:
                return (k == 0) ? 1 : 0;
            case 1:
                return 1;
            case (-1):
                return ((k & 1) == 0) ? 1 : -1;
            case 2:
                return (k < Integer.SIZE) ? (1 << k) : 0;
            case (-2):
                if (k < Integer.SIZE) {
                    return ((k & 1) == 0) ? (1 << k) : -(1 << k);
                } else {
                    return 0;
                }
            default:
                // continue below to handle the general case
        }
        for (int accum = 1;; k >>= 1) {
            switch (k) {
                case 0:
                    return accum;
                case 1:
                    return b * accum;
                default:
                    accum *= ((k & 1) == 0) ? 1 : b;
                    b *= b;
            }
        }
    }

    /**
     * Returns {@code b} to the {@code k}th power. Even if the result overflows, it will be equal to
     * {@code BigInteger.valueOf(b).pow(k).longValue()}. This implementation runs in {@code O(log k)}
     * time.
     *
     * @throws IllegalArgumentException if {@code k < 0}
     */
    public static long pow(long b, int k) {
        checkNonNegative("exponent", k);
        if (-2 <= b && b <= 2) {
            switch ((int) b) {
                case 0:
                    return (k == 0) ? 1 : 0;
                case 1:
                    return 1;
                case (-1):
                    return ((k & 1) == 0) ? 1 : -1;
                case 2:
                    return (k < Long.SIZE) ? 1L << k : 0;
                case (-2):
                    if (k < Long.SIZE) {
                        return ((k & 1) == 0) ? 1L << k : -(1L << k);
                    } else {
                        return 0;
                    }
                default:
                    throw new AssertionError();
            }
        }
        for (long accum = 1;; k >>= 1) {
            switch (k) {
                case 0:
                    return accum;
                case 1:
                    return accum * b;
                default:
                    accum *= ((k & 1) == 0) ? 1 : b;
                    b *= b;
            }
        }
    }

    /**
     * Returns the smallest power of two greater than or equal to {@code x}.  This is equivalent to
     * {@code checkedPow(2, log2(x, CEILING))}.
     *
     * @throws IllegalArgumentException if {@code x <= 0}
     * @throws ArithmeticException of the next-higher power of two is not representable as a
     *         {@code long}, i.e. when {@code x > 2^62}
     * @since 20.0
     */
    public static long ceilingPowerOfTwo(long x) {
        checkPositive("x", x);
        if (x > MAX_SIGNED_POWER_OF_TWO) {
            throw new ArithmeticException("ceilingPowerOfTwo(" + x + ") is not representable as a long");
        }
        return 1L << -Long.numberOfLeadingZeros(x - 1);
    }

    public static BigInteger ceilingPowerOfTwo(BigInteger x) {
        return BigInteger.ZERO.setBit(log2(x, RoundingMode.CEILING));
    }

    /**
     * Returns the largest power of two less than or equal to {@code x}.  This is equivalent to
     * {@code checkedPow(2, log2(x, FLOOR))}.
     *
     * @throws IllegalArgumentException if {@code x <= 0}
     * @since 20.0
     */
    public static long floorPowerOfTwo(long x) {
        checkPositive("x", x);

        // Long.highestOneBit was buggy on GWT.  We've fixed it, but I'm not certain when the fix will
        // be released.
        return 1L << ((Long.SIZE - 1) - Long.numberOfLeadingZeros(x));
    }

    public static BigInteger floorPowerOfTwo(BigInteger x) {
        return BigInteger.ZERO.setBit(log2(x, RoundingMode.FLOOR));
    }

    /**
     * Returns the square root of {@code x}, rounded with the specified rounding mode.
     *
     * @throws IllegalArgumentException if {@code x < 0}
     * @throws ArithmeticException if {@code mode} is {@link RoundingMode#UNNECESSARY} and
     *         {@code sqrt(x)} is not an integer
     */
    @SuppressWarnings("fallthrough")
    public static int sqrt(int x, RoundingMode mode) {
        checkNonNegative("x", x);
        int sqrtFloor = sqrtFloor(x);
        switch (mode) {
            case UNNECESSARY:
                checkRoundingUnnecessary(sqrtFloor * sqrtFloor == x); // fall through
            case FLOOR:
            case DOWN:
                return sqrtFloor;
            case CEILING:
            case UP:
                return sqrtFloor + lessThanBranchFree(sqrtFloor * sqrtFloor, x);
            case HALF_DOWN:
            case HALF_UP:
            case HALF_EVEN:
                int halfSquare = sqrtFloor * sqrtFloor + sqrtFloor;
                /*
                 * We wish to test whether or not x <= (sqrtFloor + 0.5)^2 = halfSquare + 0.25. Since both
                 * x and halfSquare are integers, this is equivalent to testing whether or not x <=
                 * halfSquare. (We have to deal with overflow, though.)
                 * 
                 * If we treat halfSquare as an unsigned int, we know that
                 *            sqrtFloor^2 <= x < (sqrtFloor + 1)^2
                 * halfSquare - sqrtFloor <= x < halfSquare + sqrtFloor + 1
                 * so |x - halfSquare| <= sqrtFloor.  Therefore, it's safe to treat x - halfSquare as a
                 * signed int, so lessThanBranchFree is safe for use.
                 */
                return sqrtFloor + lessThanBranchFree(halfSquare, x);
            default:
                throw new AssertionError();
        }
    }

    private static int sqrtFloor(int x) {
        // There is no loss of precision in converting an int to a double, according to
        // http://java.sun.com/docs/books/jls/third_edition/html/conversions.html#5.1.2
        return (int) Math.sqrt(x);
    }

    /**
     * Returns the square root of {@code x}, rounded with the specified rounding mode.
     *
     * @throws IllegalArgumentException if {@code x < 0}
     * @throws ArithmeticException if {@code mode} is {@link RoundingMode#UNNECESSARY} and
     *     {@code sqrt(x)} is not an integer
     */
    @SuppressWarnings("fallthrough")
    public static long sqrt(long x, RoundingMode mode) {
        checkNonNegative("x", x);
        if (fitsInInt(x)) {
            return sqrt((int) x, mode);
        }
        /*
         * Let k be the true value of floor(sqrt(x)), so that
         *
         *            k * k <= x          <  (k + 1) * (k + 1)
         * (double) (k * k) <= (double) x <= (double) ((k + 1) * (k + 1))
         *          since casting to double is nondecreasing.
         *          Note that the right-hand inequality is no longer strict.
         * Math.sqrt(k * k) <= Math.sqrt(x) <= Math.sqrt((k + 1) * (k + 1))
         *          since Math.sqrt is monotonic.
         * (long) Math.sqrt(k * k) <= (long) Math.sqrt(x) <= (long) Math.sqrt((k + 1) * (k + 1))
         *          since casting to long is monotonic
         * k <= (long) Math.sqrt(x) <= k + 1
         *          since (long) Math.sqrt(k * k) == k, as checked exhaustively in
         *          {@link LongMathTest#testSqrtOfPerfectSquareAsDoubleIsPerfect}
         */
        long guess = (long) Math.sqrt(x);
        // Note: guess is always <= FLOOR_SQRT_MAX_LONG.
        long guessSquared = guess * guess;
        // Note (2013-2-26): benchmarks indicate that, inscrutably enough, using if statements is
        // faster here than using lessThanBranchFree.
        switch (mode) {
            case UNNECESSARY:
                checkRoundingUnnecessary(guessSquared == x);
                return guess;
            case FLOOR:
            case DOWN:
                if (x < guessSquared) {
                    return guess - 1;
                }
                return guess;
            case CEILING:
            case UP:
                if (x > guessSquared) {
                    return guess + 1;
                }
                return guess;
            case HALF_DOWN:
            case HALF_UP:
            case HALF_EVEN:
                long sqrtFloor = guess - ((x < guessSquared) ? 1 : 0);
                long halfSquare = sqrtFloor * sqrtFloor + sqrtFloor;
                /*
                 * We wish to test whether or not x <= (sqrtFloor + 0.5)^2 = halfSquare + 0.25. Since both x
                 * and halfSquare are integers, this is equivalent to testing whether or not x <=
                 * halfSquare. (We have to deal with overflow, though.)
                 *
                 * If we treat halfSquare as an unsigned long, we know that
                 *            sqrtFloor^2 <= x < (sqrtFloor + 1)^2
                 * halfSquare - sqrtFloor <= x < halfSquare + sqrtFloor + 1
                 * so |x - halfSquare| <= sqrtFloor.  Therefore, it's safe to treat x - halfSquare as a
                 * signed long, so lessThanBranchFree is safe for use.
                 */
                return sqrtFloor + lessThanBranchFree(halfSquare, x);
            default:
                throw new AssertionError();
        }
    }

    public static BigInteger sqrt(BigInteger x, RoundingMode mode) {
        checkNonNegative("x", x);
        if (fitsInLong(x)) {
            return BigInteger.valueOf(sqrt(x.longValue(), mode));
        }
        BigInteger sqrtFloor = sqrtFloor(x);
        switch (mode) {
            case UNNECESSARY:
                checkRoundingUnnecessary(sqrtFloor.pow(2).equals(x)); // fall through
            case FLOOR:
            case DOWN:
                return sqrtFloor;
            case CEILING:
            case UP:
                int sqrtFloorInt = sqrtFloor.intValue();
                boolean sqrtFloorIsExact = (sqrtFloorInt * sqrtFloorInt == x.intValue()) // fast check mod 2^32
                        && sqrtFloor.pow(2).equals(x); // slow exact check
                return sqrtFloorIsExact ? sqrtFloor : sqrtFloor.add(BigInteger.ONE);
            case HALF_DOWN:
            case HALF_UP:
            case HALF_EVEN:
                BigInteger halfSquare = sqrtFloor.pow(2).add(sqrtFloor);
                /*
                 * We wish to test whether or not x <= (sqrtFloor + 0.5)^2 = halfSquare + 0.25. Since both x
                 * and halfSquare are integers, this is equivalent to testing whether or not x <=
                 * halfSquare.
                 */
                return (halfSquare.compareTo(x) >= 0) ? sqrtFloor : sqrtFloor.add(BigInteger.ONE);
            default:
                throw new AssertionError();
        }
    }

    private static BigInteger sqrtFloor(BigInteger x) {
        /*
         * Adapted from Hacker's Delight, Figure 11-1.
         *
         * Using DoubleUtils.bigToDouble, getting a double approximation of x is extremely fast, and
         * then we can get a double approximation of the square root. Then, we iteratively improve this
         * guess with an application of Newton's method, which sets guess := (guess + (x / guess)) / 2.
         * This iteration has the following two properties:
         *
         * a) every iteration (except potentially the first) has guess >= floor(sqrt(x)). This is
         * because guess' is the arithmetic mean of guess and x / guess, sqrt(x) is the geometric mean,
         * and the arithmetic mean is always higher than the geometric mean.
         *
         * b) this iteration converges to floor(sqrt(x)). In fact, the number of correct digits doubles
         * with each iteration, so this algorithm takes O(log(digits)) iterations.
         *
         * We start out with a double-precision approximation, which may be higher or lower than the
         * true value. Therefore, we perform at least one Newton iteration to get a guess that's
         * definitely >= floor(sqrt(x)), and then continue the iteration until we reach a fixed point.
         */
        BigInteger sqrt0;
        int log2 = log2(x, FLOOR);
        if (log2 < Double.MAX_EXPONENT) {
            sqrt0 = sqrtApproxWithDoubles(x);
        } else {
            int shift = (log2 - SIGNIFICAND_BITS) & ~1; // even!
            /*
             * We have that x / 2^shift < 2^54. Our initial approximation to sqrtFloor(x) will be
             * 2^(shift/2) * sqrtApproxWithDoubles(x / 2^shift).
             */
            sqrt0 = sqrtApproxWithDoubles(x.shiftRight(shift)).shiftLeft(shift >> 1);
        }
        BigInteger sqrt1 = sqrt0.add(x.divide(sqrt0)).shiftRight(1);
        if (sqrt0.equals(sqrt1)) {
            return sqrt0;
        }
        do {
            sqrt0 = sqrt1;
            sqrt1 = sqrt0.add(x.divide(sqrt0)).shiftRight(1);
        } while (sqrt1.compareTo(sqrt0) < 0);
        return sqrt0;
    }

    private static BigInteger sqrtApproxWithDoubles(BigInteger x) {
        return roundToBigInteger(Math.sqrt(bigToDouble(x)), HALF_EVEN);
    }

    /**
     * Returns the result of dividing {@code p} by {@code q}, rounding using the specified
     * {@code RoundingMode}.
     *
     * @throws ArithmeticException if {@code q == 0}, or if {@code mode == UNNECESSARY} and {@code a}
     *         is not an integer multiple of {@code b}
     */
    @SuppressWarnings("fallthrough")
    public static int divide(int p, int q, RoundingMode mode) {
        N.checkArgNotNull(mode);
        if (q == 0) {
            throw new ArithmeticException("/ by zero"); // for GWT
        }
        int div = p / q;
        int rem = p - q * div; // equal to p % q

        if (rem == 0) {
            return div;
        }

        /*
         * Normal Java division rounds towards 0, consistently with RoundingMode.DOWN. We just have to
         * deal with the cases where rounding towards 0 is wrong, which typically depends on the sign of
         * p / q.
         *
         * signum is 1 if p and q are both nonnegative or both negative, and -1 otherwise.
         */
        int signum = 1 | ((p ^ q) >> (Integer.SIZE - 1));
        boolean increment;
        switch (mode) {
            case UNNECESSARY:
                checkRoundingUnnecessary(rem == 0);
                // fall through
            case DOWN:
                increment = false;
                break;
            case UP:
                increment = true;
                break;
            case CEILING:
                increment = signum > 0;
                break;
            case FLOOR:
                increment = signum < 0;
                break;
            case HALF_EVEN:
            case HALF_DOWN:
            case HALF_UP:
                int absRem = abs(rem);
                int cmpRemToHalfDivisor = absRem - (abs(q) - absRem);
                // subtracting two nonnegative ints can't overflow
                // cmpRemToHalfDivisor has the same sign as compare(abs(rem), abs(q) / 2).
                if (cmpRemToHalfDivisor == 0) { // exactly on the half mark
                    increment = (mode == HALF_UP || (mode == HALF_EVEN & (div & 1) != 0));
                } else {
                    increment = cmpRemToHalfDivisor > 0; // closer to the UP value
                }
                break;
            default:
                throw new AssertionError();
        }
        return increment ? div + signum : div;
    }

    /**
     * Returns the result of dividing {@code p} by {@code q}, rounding using the specified
     * {@code RoundingMode}.
     *
     * @throws ArithmeticException if {@code q == 0}, or if {@code mode == UNNECESSARY} and {@code a}
     *     is not an integer multiple of {@code b}
     */
    @SuppressWarnings("fallthrough")
    public static long divide(long p, long q, RoundingMode mode) {
        N.checkArgNotNull(mode);
        long div = p / q; // throws if q == 0
        long rem = p - q * div; // equals p % q

        if (rem == 0) {
            return div;
        }

        /*
         * Normal Java division rounds towards 0, consistently with RoundingMode.DOWN. We just have to
         * deal with the cases where rounding towards 0 is wrong, which typically depends on the sign of
         * p / q.
         *
         * signum is 1 if p and q are both nonnegative or both negative, and -1 otherwise.
         */
        int signum = 1 | (int) ((p ^ q) >> (Long.SIZE - 1));
        boolean increment;
        switch (mode) {
            case UNNECESSARY:
                checkRoundingUnnecessary(rem == 0);
                // fall through
            case DOWN:
                increment = false;
                break;
            case UP:
                increment = true;
                break;
            case CEILING:
                increment = signum > 0;
                break;
            case FLOOR:
                increment = signum < 0;
                break;
            case HALF_EVEN:
            case HALF_DOWN:
            case HALF_UP:
                long absRem = abs(rem);
                long cmpRemToHalfDivisor = absRem - (abs(q) - absRem);
                // subtracting two nonnegative longs can't overflow
                // cmpRemToHalfDivisor has the same sign as compare(abs(rem), abs(q) / 2).
                if (cmpRemToHalfDivisor == 0) { // exactly on the half mark
                    increment = (mode == HALF_UP | (mode == HALF_EVEN & (div & 1) != 0));
                } else {
                    increment = cmpRemToHalfDivisor > 0; // closer to the UP value
                }
                break;
            default:
                throw new AssertionError();
        }
        return increment ? div + signum : div;
    }

    public static BigInteger divide(BigInteger p, BigInteger q, RoundingMode mode) {
        BigDecimal pDec = new BigDecimal(p);
        BigDecimal qDec = new BigDecimal(q);
        return pDec.divide(qDec, 0, mode).toBigIntegerExact();
    }

    /**
     * Returns {@code x mod m}, a non-negative value less than {@code m}.
     * This differs from {@code x % m}, which might be negative.
     *
     * <p>For example:<pre> {@code
     *
     * mod(7, 4) == 3
     * mod(-7, 4) == 1
     * mod(-1, 4) == 3
     * mod(-8, 4) == 0
     * mod(8, 4) == 0}</pre>
     *
     * @throws ArithmeticException if {@code m <= 0}
     * @see <a href="http://docs.oracle.com/javase/specs/jls/se7/html/jls-15.html#jls-15.17.3">
     *      Remainder Operator</a>
     */
    public static int mod(int x, int m) {
        if (m <= 0) {
            throw new ArithmeticException("Modulus " + m + " must be > 0");
        }
        int result = x % m;
        return (result >= 0) ? result : result + m;
    }

    /**
     * Returns {@code x mod m}, a non-negative value less than {@code m}. This differs from
     * {@code x % m}, which might be negative.
     *
     * <p>For example:
     *
     * <pre> {@code
     *
     * mod(7, 4) == 3
     * mod(-7, 4) == 1
     * mod(-1, 4) == 3
     * mod(-8, 4) == 0
     * mod(8, 4) == 0}</pre>
     *
     * @throws ArithmeticException if {@code m <= 0}
     * @see <a href="http://docs.oracle.com/javase/specs/jls/se7/html/jls-15.html#jls-15.17.3">
     *     Remainder Operator</a>
     */
    public static int mod(long x, int m) {
        // Cast is safe because the result is guaranteed in the range [0, m)
        return (int) mod(x, (long) m);
    }

    /**
     * Returns {@code x mod m}, a non-negative value less than {@code m}. This differs from
     * {@code x % m}, which might be negative.
     *
     * <p>For example:
     *
     * <pre> {@code
     *
     * mod(7, 4) == 3
     * mod(-7, 4) == 1
     * mod(-1, 4) == 3
     * mod(-8, 4) == 0
     * mod(8, 4) == 0}</pre>
     *
     * @throws ArithmeticException if {@code m <= 0}
     * @see <a href="http://docs.oracle.com/javase/specs/jls/se7/html/jls-15.html#jls-15.17.3">
     *     Remainder Operator</a>
     */
    public static long mod(long x, long m) {
        if (m <= 0) {
            throw new ArithmeticException("Modulus must be positive");
        }
        long result = x % m;
        return (result >= 0) ? result : result + m;
    }

    /**
     * Returns the greatest common divisor of {@code a, b}. Returns {@code 0} if
     * {@code a == 0 && b == 0}.
     *
     * @throws IllegalArgumentException if {@code a < 0} or {@code b < 0}
     */
    public static int gcd(int a, int b) {
        /*
         * The reason we require both arguments to be >= 0 is because otherwise, what do you return on
         * gcd(0, Integer.MIN_VALUE)? BigInteger.gcd would return positive 2^31, but positive 2^31
         * isn't an int.
         */
        checkNonNegative("a", a);
        checkNonNegative("b", b);
        if (a == 0) {
            // 0 % b == 0, so b divides a, but the converse doesn't hold.
            // BigInteger.gcd is consistent with this decision.
            return b;
        } else if (b == 0) {
            return a; // similar logic
        }
        /*
         * Uses the binary GCD algorithm; see http://en.wikipedia.org/wiki/Binary_GCD_algorithm.
         * This is >40% faster than the Euclidean algorithm in benchmarks.
         */
        int aTwos = Integer.numberOfTrailingZeros(a);
        a >>= aTwos; // divide out all 2s
        int bTwos = Integer.numberOfTrailingZeros(b);
        b >>= bTwos; // divide out all 2s
        while (a != b) { // both a, b are odd
            // The key to the binary GCD algorithm is as follows:
            // Both a and b are odd.  Assume a > b; then gcd(a - b, b) = gcd(a, b).
            // But in gcd(a - b, b), a - b is even and b is odd, so we can divide out powers of two.

            // We bend over backwards to avoid branching, adapting a technique from
            // http://graphics.stanford.edu/~seander/bithacks.html#IntegerMinOrMax

            int delta = a - b; // can't overflow, since a and b are nonnegative

            int minDeltaOrZero = delta & (delta >> (Integer.SIZE - 1));
            // equivalent to Math.min(delta, 0)

            a = delta - minDeltaOrZero - minDeltaOrZero; // sets a to Math.abs(a - b)
            // a is now nonnegative and even

            b += minDeltaOrZero; // sets b to min(old a, b)
            a >>= Integer.numberOfTrailingZeros(a); // divide out all 2s, since 2 doesn't divide b
        }
        return a << min(aTwos, bTwos);
    }

    /**
     * Returns the greatest common divisor of {@code a, b}. Returns {@code 0} if
     * {@code a == 0 && b == 0}.
     *
     * @throws IllegalArgumentException if {@code a < 0} or {@code b < 0}
     */
    public static long gcd(long a, long b) {
        /*
         * The reason we require both arguments to be >= 0 is because otherwise, what do you return on
         * gcd(0, Long.MIN_VALUE)? BigInteger.gcd would return positive 2^63, but positive 2^63 isn't an
         * int.
         */
        checkNonNegative("a", a);
        checkNonNegative("b", b);
        if (a == 0) {
            // 0 % b == 0, so b divides a, but the converse doesn't hold.
            // BigInteger.gcd is consistent with this decision.
            return b;
        } else if (b == 0) {
            return a; // similar logic
        }
        /*
         * Uses the binary GCD algorithm; see http://en.wikipedia.org/wiki/Binary_GCD_algorithm. This is
         * >60% faster than the Euclidean algorithm in benchmarks.
         */
        int aTwos = Long.numberOfTrailingZeros(a);
        a >>= aTwos; // divide out all 2s
        int bTwos = Long.numberOfTrailingZeros(b);
        b >>= bTwos; // divide out all 2s
        while (a != b) { // both a, b are odd
            // The key to the binary GCD algorithm is as follows:
            // Both a and b are odd. Assume a > b; then gcd(a - b, b) = gcd(a, b).
            // But in gcd(a - b, b), a - b is even and b is odd, so we can divide out powers of two.

            // We bend over backwards to avoid branching, adapting a technique from
            // http://graphics.stanford.edu/~seander/bithacks.html#IntegerMinOrMax

            long delta = a - b; // can't overflow, since a and b are nonnegative

            long minDeltaOrZero = delta & (delta >> (Long.SIZE - 1));
            // equivalent to Math.min(delta, 0)

            a = delta - minDeltaOrZero - minDeltaOrZero; // sets a to Math.abs(a - b)
            // a is now nonnegative and even

            b += minDeltaOrZero; // sets b to min(old a, b)
            a >>= Long.numberOfTrailingZeros(a); // divide out all 2s, since 2 doesn't divide b
        }
        return a << min(aTwos, bTwos);
    }

    /**
     * <p>
     * Returns the least common multiple of the absolute value of two numbers,
     * using the formula {@code lcm(a,b) = (a / gcd(a,b)) * b}.
     * </p>
     * Special cases:
     * <ul>
     * <li>The invocations {@code lcm(Integer.MIN_VALUE, n)} and
     * {@code lcm(n, Integer.MIN_VALUE)}, where {@code abs(n)} is a
     * power of 2, throw an {@code ArithmeticException}, because the result
     * would be 2^31, which is too large for an int value.</li>
     * <li>The result of {@code lcm(0, x)} and {@code lcm(x, 0)} is
     * {@code 0} for any {@code x}.
     * </ul>
     *
     * @param a Number.
     * @param b Number.
     * @return the least common multiple, never negative.
     * @throws ArithmeticException if the result cannot be represented as
     * a non-negative {@code int} value.
     * @since 1.1
     */
    public static int lcm(int a, int b) throws ArithmeticException {
        if (a == 0 || b == 0) {
            return 0;
        }
        int lcm = Math.abs(addExact(a / gcd(a, b), b));
        if (lcm == Integer.MIN_VALUE) {
            throw new ArithmeticException();
        }
        return lcm;
    }

    /**
     * <p>
     * Returns the least common multiple of the absolute value of two numbers,
     * using the formula {@code lcm(a,b) = (a / gcd(a,b)) * b}.
     * </p>
     * Special cases:
     * <ul>
     * <li>The invocations {@code lcm(Long.MIN_VALUE, n)} and
     * {@code lcm(n, Long.MIN_VALUE)}, where {@code abs(n)} is a
     * power of 2, throw an {@code ArithmeticException}, because the result
     * would be 2^63, which is too large for an int value.</li>
     * <li>The result of {@code lcm(0L, x)} and {@code lcm(x, 0L)} is
     * {@code 0L} for any {@code x}.
     * </ul>
     *
     * @param a Number.
     * @param b Number.
     * @return the least common multiple, never negative.
     * @throws ArithmeticException if the result cannot be represented
     * as a non-negative {@code long} value.
     * @since 2.1
     */
    public static long lcm(long a, long b) throws ArithmeticException {
        if (a == 0 || b == 0) {
            return 0;
        }
        long lcm = Math.abs(addExact(a / gcd(a, b), b));
        if (lcm == Integer.MIN_VALUE) {
            throw new ArithmeticException();
        }
        return lcm;
    }

    /**
     * Returns the sum of {@code a} and {@code b}, provided it does not overflow.
     *
     * @throws ArithmeticException if {@code a + b} overflows in signed {@code int} arithmetic
     */
    public static int addExact(int a, int b) {
        long result = (long) a + b;
        checkNoOverflow(result == (int) result);
        return (int) result;
    }

    /**
     * Returns the sum of {@code a} and {@code b}, provided it does not overflow.
     *
     * @throws ArithmeticException if {@code a + b} overflows in signed {@code long} arithmetic
     */
    public static long addExact(long a, long b) {
        long result = a + b;
        checkNoOverflow((a ^ b) < 0 | (a ^ result) >= 0);
        return result;
    }

    /**
     * Returns the difference of {@code a} and {@code b}, provided it does not overflow.
     *
     * @throws ArithmeticException if {@code a - b} overflows in signed {@code int} arithmetic
     */
    public static int subtractExact(int a, int b) {
        long result = (long) a - b;
        checkNoOverflow(result == (int) result);
        return (int) result;
    }

    /**
     * Returns the difference of {@code a} and {@code b}, provided it does not overflow.
     *
     * @throws ArithmeticException if {@code a - b} overflows in signed {@code long} arithmetic
     */
    public static long subtractExact(long a, long b) {
        long result = a - b;
        checkNoOverflow((a ^ b) >= 0 | (a ^ result) >= 0);
        return result;
    }

    /**
     * Returns the product of {@code a} and {@code b}, provided it does not overflow.
     *
     * @throws ArithmeticException if {@code a * b} overflows in signed {@code int} arithmetic
     */
    public static int multiplyExact(int a, int b) {
        long result = (long) a * b;
        checkNoOverflow(result == (int) result);
        return (int) result;
    }

    /**
     * Returns the product of {@code a} and {@code b}, provided it does not overflow.
     *
     * @throws ArithmeticException if {@code a * b} overflows in signed {@code long} arithmetic
     */
    public static long multiplyExact(long a, long b) {
        // Hacker's Delight, Section 2-12
        int leadingZeros = Long.numberOfLeadingZeros(a) + Long.numberOfLeadingZeros(~a) + Long.numberOfLeadingZeros(b) + Long.numberOfLeadingZeros(~b);
        /*
         * If leadingZeros > Long.SIZE + 1 it's definitely fine, if it's < Long.SIZE it's definitely
         * bad. We do the leadingZeros check to avoid the division below if at all possible.
         *
         * Otherwise, if b == Long.MIN_VALUE, then the only allowed values of a are 0 and 1. We take
         * care of all a < 0 with their own check, because in particular, the case a == -1 will
         * incorrectly pass the division check below.
         *
         * In all other cases, we check that either a is 0 or the result is consistent with division.
         */
        if (leadingZeros > Long.SIZE + 1) {
            return a * b;
        }
        checkNoOverflow(leadingZeros >= Long.SIZE);
        checkNoOverflow(a >= 0 | b != Long.MIN_VALUE);
        long result = a * b;
        checkNoOverflow(a == 0 || result / a == b);
        return result;
    }

    /**
     * Returns the {@code b} to the {@code k}th power, provided it does not overflow.
     *
     * <p>{@link #pow} may be faster, but does not check for overflow.
     *
     * @throws ArithmeticException if {@code b} to the {@code k}th power overflows in signed
     *         {@code int} arithmetic
     */
    public static int powExact(int b, int k) {
        checkNonNegative("exponent", k);
        switch (b) {
            case 0:
                return (k == 0) ? 1 : 0;
            case 1:
                return 1;
            case (-1):
                return ((k & 1) == 0) ? 1 : -1;
            case 2:
                checkNoOverflow(k < Integer.SIZE - 1);
                return 1 << k;
            case (-2):
                checkNoOverflow(k < Integer.SIZE);
                return ((k & 1) == 0) ? 1 << k : -1 << k;
            default:
                // continue below to handle the general case
        }
        int accum = 1;
        while (true) {
            switch (k) {
                case 0:
                    return accum;
                case 1:
                    return multiplyExact(accum, b);
                default:
                    if ((k & 1) != 0) {
                        accum = multiplyExact(accum, b);
                    }
                    k >>= 1;
                    if (k > 0) {
                        checkNoOverflow(-FLOOR_SQRT_MAX_INT <= b & b <= FLOOR_SQRT_MAX_INT);
                        b *= b;
                    }
            }
        }
    }

    /**
     * Returns the {@code b} to the {@code k}th power, provided it does not overflow.
     *
     * @throws ArithmeticException if {@code b} to the {@code k}th power overflows in signed
     *     {@code long} arithmetic
     */
    public static long powExact(long b, int k) {
        checkNonNegative("exponent", k);
        if (b >= -2 & b <= 2) {
            switch ((int) b) {
                case 0:
                    return (k == 0) ? 1 : 0;
                case 1:
                    return 1;
                case (-1):
                    return ((k & 1) == 0) ? 1 : -1;
                case 2:
                    checkNoOverflow(k < Long.SIZE - 1);
                    return 1L << k;
                case (-2):
                    checkNoOverflow(k < Long.SIZE);
                    return ((k & 1) == 0) ? (1L << k) : (-1L << k);
                default:
                    throw new AssertionError();
            }
        }
        long accum = 1;
        while (true) {
            switch (k) {
                case 0:
                    return accum;
                case 1:
                    return multiplyExact(accum, b);
                default:
                    if ((k & 1) != 0) {
                        accum = multiplyExact(accum, b);
                    }
                    k >>= 1;
                    if (k > 0) {
                        checkNoOverflow(-FLOOR_SQRT_MAX_LONG <= b && b <= FLOOR_SQRT_MAX_LONG);
                        b *= b;
                    }
            }
        }
    }

    /**
     * Returns the {@code int} value that is equal to {@code value}, if possible.
     *
     * @param value any value in the range of the {@code int} type
     * @return the {@code int} value that equals {@code value}
     * @throws IllegalArgumentException if {@code value} is greater than {@link Integer#MAX_VALUE} or
     *     less than {@link Integer#MIN_VALUE}
     */
    public static int castExact(long value) {
        int result = (int) value;
        if (result != value) {
            // don't use checkArgument here, to avoid boxing
            throw new IllegalArgumentException("Out of range: " + value);
        }
        return result;
    }

    /**
     * Returns the sum of {@code a} and {@code b} unless it would overflow or underflow in which case
     * {@code Integer.MAX_VALUE} or {@code Integer.MIN_VALUE} is returned, respectively.
     *
     * @since 20.0
     */
    public static int saturatedAdd(int a, int b) {
        return saturatedCast((long) a + b);
    }

    /**
     * Returns the sum of {@code a} and {@code b} unless it would overflow or underflow in which case
     * {@code Long.MAX_VALUE} or {@code Long.MIN_VALUE} is returned, respectively.
     *
     * @since 20.0
     */
    public static long saturatedAdd(long a, long b) {
        long naiveSum = a + b;
        if ((a ^ b) < 0 | (a ^ naiveSum) >= 0) {
            // If a and b have different signs or a has the same sign as the result then there was no
            // overflow, return.
            return naiveSum;
        }
        // we did over/under flow, if the sign is negative we should return MAX otherwise MIN
        return Long.MAX_VALUE + ((naiveSum >>> (Long.SIZE - 1)) ^ 1);
    }

    /**
     * Returns the difference of {@code a} and {@code b} unless it would overflow or underflow in
     * which case {@code Integer.MAX_VALUE} or {@code Integer.MIN_VALUE} is returned, respectively.
     *
     * @since 20.0
     */
    public static int saturatedSubtract(int a, int b) {
        return saturatedCast((long) a - b);
    }

    /**
     * Returns the difference of {@code a} and {@code b} unless it would overflow or underflow in
     * which case {@code Long.MAX_VALUE} or {@code Long.MIN_VALUE} is returned, respectively.
     *
     * @since 20.0
     */
    public static long saturatedSubtract(long a, long b) {
        long naiveDifference = a - b;
        if ((a ^ b) >= 0 | (a ^ naiveDifference) >= 0) {
            // If a and b have the same signs or a has the same sign as the result then there was no
            // overflow, return.
            return naiveDifference;
        }
        // we did over/under flow
        return Long.MAX_VALUE + ((naiveDifference >>> (Long.SIZE - 1)) ^ 1);
    }

    /**
     * Returns the product of {@code a} and {@code b} unless it would overflow or underflow in which
     * case {@code Integer.MAX_VALUE} or {@code Integer.MIN_VALUE} is returned, respectively.
     *
     * @since 20.0
     */
    public static int saturatedMultiply(int a, int b) {
        return saturatedCast((long) a * b);
    }

    /**
     * Returns the product of {@code a} and {@code b} unless it would overflow or underflow in which
     * case {@code Long.MAX_VALUE} or {@code Long.MIN_VALUE} is returned, respectively.
     *
     * @since 20.0
     */
    public static long saturatedMultiply(long a, long b) {
        // see checkedMultiply for explanation
        int leadingZeros = Long.numberOfLeadingZeros(a) + Long.numberOfLeadingZeros(~a) + Long.numberOfLeadingZeros(b) + Long.numberOfLeadingZeros(~b);
        if (leadingZeros > Long.SIZE + 1) {
            return a * b;
        }
        // the return value if we will overflow (which we calculate by overflowing a long :) )
        long limit = Long.MAX_VALUE + ((a ^ b) >>> (Long.SIZE - 1));
        if (leadingZeros < Long.SIZE | (a < 0 & b == Long.MIN_VALUE)) {
            // overflow
            return limit;
        }
        long result = a * b;
        if (a == 0 || result / a == b) {
            return result;
        }
        return limit;
    }

    /**
     * Returns the {@code b} to the {@code k}th power, unless it would overflow or underflow in which
     * case {@code Integer.MAX_VALUE} or {@code Integer.MIN_VALUE} is returned, respectively.
     *
     * @since 20.0
     */
    public static int saturatedPow(int b, int k) {
        checkNonNegative("exponent", k);
        switch (b) {
            case 0:
                return (k == 0) ? 1 : 0;
            case 1:
                return 1;
            case (-1):
                return ((k & 1) == 0) ? 1 : -1;
            case 2:
                if (k >= Integer.SIZE - 1) {
                    return Integer.MAX_VALUE;
                }
                return 1 << k;
            case (-2):
                if (k >= Integer.SIZE) {
                    return Integer.MAX_VALUE + (k & 1);
                }
                return ((k & 1) == 0) ? 1 << k : -1 << k;
            default:
                // continue below to handle the general case
        }
        int accum = 1;
        // if b is negative and k is odd then the limit is MIN otherwise the limit is MAX
        int limit = Integer.MAX_VALUE + ((b >>> Integer.SIZE - 1) & (k & 1));
        while (true) {
            switch (k) {
                case 0:
                    return accum;
                case 1:
                    return saturatedMultiply(accum, b);
                default:
                    if ((k & 1) != 0) {
                        accum = saturatedMultiply(accum, b);
                    }
                    k >>= 1;
                    if (k > 0) {
                        if (-FLOOR_SQRT_MAX_INT > b | b > FLOOR_SQRT_MAX_INT) {
                            return limit;
                        }
                        b *= b;
                    }
            }
        }
    }

    /**
     * Returns the {@code b} to the {@code k}th power, unless it would overflow or underflow in which
     * case {@code Long.MAX_VALUE} or {@code Long.MIN_VALUE} is returned, respectively.
     *
     * @since 20.0
     */
    public static long saturatedPow(long b, int k) {
        checkNonNegative("exponent", k);
        if (b >= -2 & b <= 2) {
            switch ((int) b) {
                case 0:
                    return (k == 0) ? 1 : 0;
                case 1:
                    return 1;
                case (-1):
                    return ((k & 1) == 0) ? 1 : -1;
                case 2:
                    if (k >= Long.SIZE - 1) {
                        return Long.MAX_VALUE;
                    }
                    return 1L << k;
                case (-2):
                    if (k >= Long.SIZE) {
                        return Long.MAX_VALUE + (k & 1);
                    }
                    return ((k & 1) == 0) ? (1L << k) : (-1L << k);
                default:
                    throw new AssertionError();
            }
        }
        long accum = 1;
        // if b is negative and k is odd then the limit is MIN otherwise the limit is MAX
        long limit = Long.MAX_VALUE + ((b >>> Long.SIZE - 1) & (k & 1));
        while (true) {
            switch (k) {
                case 0:
                    return accum;
                case 1:
                    return saturatedMultiply(accum, b);
                default:
                    if ((k & 1) != 0) {
                        accum = saturatedMultiply(accum, b);
                    }
                    k >>= 1;
                    if (k > 0) {
                        if (-FLOOR_SQRT_MAX_LONG > b | b > FLOOR_SQRT_MAX_LONG) {
                            return limit;
                        }
                        b *= b;
                    }
            }
        }
    }

    /**
     * Returns the {@code int} nearest in value to {@code value}.
     *
     * @param value any {@code long} value
     * @return the same value cast to {@code int} if it is in the range of the {@code int} type,
     *     {@link Integer#MAX_VALUE} if it is too large, or {@link Integer#MIN_VALUE} if it is too
     *     small
     */
    public static int saturatedCast(long value) {
        if (value > Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        }
        if (value < Integer.MIN_VALUE) {
            return Integer.MIN_VALUE;
        }
        return (int) value;
    }

    /**
     * Returns {@code n!}, that is, the product of the first {@code n} positive
     * integers, {@code 1} if {@code n == 0}, or {@link Integer#MAX_VALUE} if the
     * result does not fit in a {@code int}.
     *
     * @throws IllegalArgumentException if {@code n < 0}
     */
    public static int factorial(int n) {
        checkNonNegative("n", n);
        return (n < int_factorials.length) ? int_factorials[n] : Integer.MAX_VALUE;
    }

    /**
     * Returns {@code n!}, that is, the product of the first {@code n} positive integers, {@code 1} if
     * {@code n == 0}, or {@link Long#MAX_VALUE} if the result does not fit in a {@code long}.
     *
     * @throws IllegalArgumentException if {@code n < 0}
     */
    public static long factoriall(int n) {
        checkNonNegative("n", n);
        return (n < long_factorials.length) ? long_factorials[n] : Long.MAX_VALUE;
    }

    /**
     * Returns {@code n!}, that is, the product of the first {@code n} positive integers, {@code 1} if
     * {@code n == 0}, or {@code n!}, or {@link Double#POSITIVE_INFINITY} if
     * {@code n! > Double.MAX_VALUE}.
     *
     * <p>The result is within 1 ulp of the true value.
     *
     * @throws IllegalArgumentException if {@code n < 0}
     */
    public static double factorialll(int n) {
        checkNonNegative("n", n);
        if (n > MAX_FACTORIAL) {
            return Double.POSITIVE_INFINITY;
        } else {
            // Multiplying the last (n & 0xf) values into their own accumulator gives a more accurate
            // result than multiplying by everySixteenthFactorial[n >> 4] directly.
            double accum = 1.0;
            for (int i = 1 + (n & ~0xf); i <= n; i++) {
                accum *= i;
            }
            return accum * everySixteenthFactorial[n >> 4];
        }
    }

    /**
     * Returns {@code n!}, that is, the product of the first {@code n} positive integers, or {@code 1}
     * if {@code n == 0}.
     *
     * <p><b>Warning:</b> the result takes <i>O(n log n)</i> space, so use cautiously.
     *
     * <p>This uses an efficient binary recursive algorithm to compute the factorial with balanced
     * multiplies. It also removes all the 2s from the intermediate products (shifting them back in at
     * the end).
     *
     * @throws IllegalArgumentException if {@code n < 0}
     */
    public static BigInteger facttorial(int n) {
        checkNonNegative("n", n);

        // If the factorial is small enough, just use LongMath to do it.
        if (n < long_factorials.length) {
            return BigInteger.valueOf(long_factorials[n]);
        }

        // Pre-allocate space for our list of intermediate BigIntegers.
        int approxSize = divide(n * log2(n, CEILING), Long.SIZE, CEILING);
        ArrayList<BigInteger> bignums = new ArrayList<>(approxSize);

        // Start from the pre-computed maximum long factorial.
        int startingNumber = long_factorials.length;
        long product = long_factorials[startingNumber - 1];
        // Strip off 2s from this value.
        int shift = Long.numberOfTrailingZeros(product);
        product >>= shift;

        // Use floor(log2(num)) + 1 to prevent overflow of multiplication.
        int productBits = log2(product, FLOOR) + 1;
        int bits = log2(startingNumber, FLOOR) + 1;
        // Check for the next power of two boundary, to save us a CLZ operation.
        int nextPowerOfTwo = 1 << (bits - 1);

        // Iteratively multiply the longs as big as they can go.
        for (long num = startingNumber; num <= n; num++) {
            // Check to see if the floor(log2(num)) + 1 has changed.
            if ((num & nextPowerOfTwo) != 0) {
                nextPowerOfTwo <<= 1;
                bits++;
            }
            // Get rid of the 2s in num.
            int tz = Long.numberOfTrailingZeros(num);
            long normalizedNum = num >> tz;
            shift += tz;
            // Adjust floor(log2(num)) + 1.
            int normalizedBits = bits - tz;
            // If it won't fit in a long, then we store off the intermediate product.
            if (normalizedBits + productBits >= Long.SIZE) {
                bignums.add(BigInteger.valueOf(product));
                product = 1;
                productBits = 0;
            }
            product *= normalizedNum;
            productBits = log2(product, FLOOR) + 1;
        }
        // Check for leftovers.
        if (product > 1) {
            bignums.add(BigInteger.valueOf(product));
        }
        // Efficiently multiply all the intermediate products together.
        return listProduct(bignums).shiftLeft(shift);
    }

    static BigInteger listProduct(List<BigInteger> nums) {
        return listProduct(nums, 0, nums.size());
    }

    static BigInteger listProduct(List<BigInteger> nums, int start, int end) {
        switch (end - start) {
            case 0:
                return BigInteger.ONE;
            case 1:
                return nums.get(start);
            case 2:
                return nums.get(start).multiply(nums.get(start + 1));
            case 3:
                return nums.get(start).multiply(nums.get(start + 1)).multiply(nums.get(start + 2));
            default:
                // Otherwise, split the list in half and recursively do this.
                int m = (end + start) >>> 1;
                return listProduct(nums, start, m).multiply(listProduct(nums, m, end));
        }
    }

    /**
     * Returns {@code n} choose {@code k}, also known as the binomial coefficient of {@code n} and
     * {@code k}, or {@link Integer#MAX_VALUE} if the result does not fit in an {@code int}.
     *
     * @throws IllegalArgumentException if {@code n < 0}, {@code k < 0} or {@code k > n}
     */
    public static int binomial(int n, int k) {
        checkNonNegative("n", n);
        checkNonNegative("k", k);
        N.checkArgument(k <= n, "k (%s) > n (%s)", k, n);
        if (k > (n >> 1)) {
            k = n - k;
        }
        if (k >= int_biggestBinomials.length || n > int_biggestBinomials[k]) {
            return Integer.MAX_VALUE;
        }
        switch (k) {
            case 0:
                return 1;
            case 1:
                return n;
            default:
                long result = 1;
                for (int i = 0; i < k; i++) {
                    result *= n - i;
                    result /= i + 1;
                }
                return (int) result;
        }
    }

    /**
     * Returns {@code n} choose {@code k}, also known as the binomial coefficient of {@code n} and
     * {@code k}, or {@link Long#MAX_VALUE} if the result does not fit in a {@code long}.
     *
     * @throws IllegalArgumentException if {@code n < 0}, {@code k < 0}, or {@code k > n}
     */
    public static long binomiall(int n, int k) {
        checkNonNegative("n", n);
        checkNonNegative("k", k);
        N.checkArgument(k <= n, "k (%s) > n (%s)", k, n);
        if (k > (n >> 1)) {
            k = n - k;
        }
        switch (k) {
            case 0:
                return 1;
            case 1:
                return n;
            default:
                if (n < long_factorials.length) {
                    return long_factorials[n] / (long_factorials[k] * long_factorials[n - k]);
                } else if (k >= biggestBinomials.length || n > biggestBinomials[k]) {
                    return Long.MAX_VALUE;
                } else if (k < biggestSimpleBinomials.length && n <= biggestSimpleBinomials[k]) {
                    // guaranteed not to overflow
                    long result = n--;
                    for (int i = 2; i <= k; n--, i++) {
                        result *= n;
                        result /= i;
                    }
                    return result;
                } else {
                    int nBits = log2(n, RoundingMode.CEILING);

                    long result = 1;
                    long numerator = n--;
                    long denominator = 1;

                    int numeratorBits = nBits;
                    // This is an upper bound on log2(numerator, ceiling).

                    /*
                     * We want to do this in long math for speed, but want to avoid overflow. We adapt the
                     * technique previously used by BigIntegerMath: maintain separate numerator and
                     * denominator accumulators, multiplying the fraction into result when near overflow.
                     */
                    for (int i = 2; i <= k; i++, n--) {
                        if (numeratorBits + nBits < Long.SIZE - 1) {
                            // It's definitely safe to multiply into numerator and denominator.
                            numerator *= n;
                            denominator *= i;
                            numeratorBits += nBits;
                        } else {
                            // It might not be safe to multiply into numerator and denominator,
                            // so multiply (numerator / denominator) into result.
                            result = multiplyFraction(result, numerator, denominator);
                            numerator = n;
                            denominator = i;
                            numeratorBits = nBits;
                        }
                    }
                    return multiplyFraction(result, numerator, denominator);
                }
        }
    }

    /**
     * Returns {@code n} choose {@code k}, also known as the binomial coefficient of {@code n} and
     * {@code k}, that is, {@code n! / (k! (n - k)!)}.
     *
     * <p><b>Warning:</b> the result can take as much as <i>O(k log n)</i> space.
     *
     * @throws IllegalArgumentException if {@code n < 0}, {@code k < 0}, or {@code k > n}
     */
    public static BigInteger binnomial(int n, int k) {
        checkNonNegative("n", n);
        checkNonNegative("k", k);
        N.checkArgument(k <= n, "k (%s) > n (%s)", k, n);
        if (k > (n >> 1)) {
            k = n - k;
        }
        if (k < biggestBinomials.length && n <= biggestBinomials[k]) {
            return BigInteger.valueOf(binomial(n, k));
        }

        BigInteger accum = BigInteger.ONE;

        long numeratorAccum = n;
        long denominatorAccum = 1;

        int bits = log2(n, RoundingMode.CEILING);

        int numeratorBits = bits;

        for (int i = 1; i < k; i++) {
            int p = n - i;
            int q = i + 1;

            // log2(p) >= bits - 1, because p >= n/2

            if (numeratorBits + bits >= Long.SIZE - 1) {
                // The numerator is as big as it can get without risking overflow.
                // Multiply numeratorAccum / denominatorAccum into accum.
                accum = accum.multiply(BigInteger.valueOf(numeratorAccum)).divide(BigInteger.valueOf(denominatorAccum));
                numeratorAccum = p;
                denominatorAccum = q;
                numeratorBits = bits;
            } else {
                // We can definitely multiply into the long accumulators without overflowing them.
                numeratorAccum *= p;
                denominatorAccum *= q;
                numeratorBits += bits;
            }
        }
        return accum.multiply(BigInteger.valueOf(numeratorAccum)).divide(BigInteger.valueOf(denominatorAccum));
    }

    /**
     * Returns the arithmetic mean of {@code x} and {@code y}, rounded towards
     * negative infinity. This method is overflow resilient.
     *
     * @since 14.0
     */
    public static int mean(int x, int y) {
        // Efficient method for computing the arithmetic mean.
        // The alternative (x + y) / 2 fails for large values.
        // The alternative (x + y) >>> 1 fails for negative values.
        return (x & y) + ((x ^ y) >> 1);
    }

    /**
     * Returns the arithmetic mean of {@code x} and {@code y}, rounded toward negative infinity. This
     * method is resilient to overflow.
     *
     * @since 14.0
     */
    public static long mean(long x, long y) {
        // Efficient method for computing the arithmetic mean.
        // The alternative (x + y) / 2 fails for large values.
        // The alternative (x + y) >>> 1 fails for negative values.
        return (x & y) + ((x ^ y) >> 1);
    }

    public static double mean(double x, double y) {
        return checkFinite(x) + (checkFinite(x) - x) / 2;
    }

    /**
     * Returns the <a href="http://en.wikipedia.org/wiki/Arithmetic_mean">arithmetic mean</a> of
     * {@code values}.
     *
     * <p>If these values are a sample drawn from a population, this is also an unbiased estimator of
     * the arithmetic mean of the population.
     *
     * @param values a nonempty series of values
     * @throws IllegalArgumentException if {@code values} is empty 
     */
    @SafeVarargs
    public static double mean(int... values) {
        N.checkArgument(values.length > 0, "Cannot take mean of 0 values");
        // The upper bound on the the length of an array and the bounds on the int values mean that, in
        // this case only, we can compute the sum as a long without risking overflow or loss of
        // precision. So we do that, as it's slightly quicker than the Knuth algorithm.
        long sum = 0;
        for (int index = 0; index < values.length; ++index) {
            sum += values[index];
        }
        return (double) sum / values.length;
    }

    /**
     * Returns the <a href="http://en.wikipedia.org/wiki/Arithmetic_mean">arithmetic mean</a> of
     * {@code values}.
     *
     * <p>If these values are a sample drawn from a population, this is also an unbiased estimator of
     * the arithmetic mean of the population.
     *
     * @param values a nonempty series of values, which will be converted to {@code double} values
     *     (this may cause loss of precision for longs of magnitude over 2^53 (slightly over 9e15))
     * @throws IllegalArgumentException if {@code values} is empty 
     */
    @SafeVarargs
    public static double mean(long... values) {
        N.checkArgument(values.length > 0, "Cannot take mean of 0 values");
        long count = 1;
        double mean = values[0];
        for (int index = 1; index < values.length; ++index) {
            count++;
            // Art of Computer Programming vol. 2, Knuth, 4.2.2, (15)
            mean += (values[index] - mean) / count;
        }
        return mean;
    }

    @SafeVarargs
    public static double mean(double... values) {
        N.checkArgument(values.length > 0, "Cannot take mean of 0 values");
        long count = 1;
        double mean = checkFinite(values[0]);
        for (int index = 1; index < values.length; ++index) {
            checkFinite(values[index]);
            count++;
            // Art of Computer Programming vol. 2, Knuth, 4.2.2, (15)
            mean += (values[index] - mean) / count;
        }
        return mean;
    }

    private static double checkFinite(double argument) {
        N.checkArgument(isFinite(argument));
        return argument;
    }

    static double roundIntermediate(double x, RoundingMode mode) {
        if (!isFinite(x)) {
            throw new ArithmeticException("input is infinite or NaN");
        }
        switch (mode) {
            case UNNECESSARY:
                checkRoundingUnnecessary(isMathematicalInteger(x));
                return x;

            case FLOOR:
                if (x >= 0.0 || isMathematicalInteger(x)) {
                    return x;
                } else {
                    return (long) x - 1;
                }

            case CEILING:
                if (x <= 0.0 || isMathematicalInteger(x)) {
                    return x;
                } else {
                    return (long) x + 1;
                }

            case DOWN:
                return x;

            case UP:
                if (isMathematicalInteger(x)) {
                    return x;
                } else {
                    return (long) x + (x > 0 ? 1 : -1);
                }

            case HALF_EVEN:
                return Math.rint(x);

            case HALF_UP: {
                double z = Math.rint(x);
                if (abs(x - z) == 0.5) {
                    return x + Math.copySign(0.5, x);
                } else {
                    return z;
                }
            }

            case HALF_DOWN: {
                double z = Math.rint(x);
                if (abs(x - z) == 0.5) {
                    return x;
                } else {
                    return z;
                }
            }

            default:
                throw new AssertionError();
        }
    }

    /**
     * Returns the {@code int} value that is equal to {@code x} rounded with the specified rounding
     * mode, if possible.
     *
     * @throws ArithmeticException if
     *     <ul>
     *     <li>{@code x} is infinite or NaN
     *     <li>{@code x}, after being rounded to a mathematical integer using the specified rounding
     *         mode, is either less than {@code Integer.MIN_VALUE} or greater than {@code
     *         Integer.MAX_VALUE}
     *     <li>{@code x} is not a mathematical integer and {@code mode} is
     *         {@link RoundingMode#UNNECESSARY}
     *     </ul>
     */
    public static int roundToInt(double x, RoundingMode mode) {
        double z = roundIntermediate(x, mode);
        checkInRange(z > MIN_INT_AS_DOUBLE - 1.0 & z < MAX_INT_AS_DOUBLE + 1.0);
        return (int) z;
    }

    /**
     * Returns the {@code long} value that is equal to {@code x} rounded with the specified rounding
     * mode, if possible.
     *
     * @throws ArithmeticException if
     *     <ul>
     *     <li>{@code x} is infinite or NaN
     *     <li>{@code x}, after being rounded to a mathematical integer using the specified rounding
     *         mode, is either less than {@code Long.MIN_VALUE} or greater than {@code
     *         Long.MAX_VALUE}
     *     <li>{@code x} is not a mathematical integer and {@code mode} is
     *         {@link RoundingMode#UNNECESSARY}
     *     </ul>
     */
    public static long roundToLong(double x, RoundingMode mode) {
        double z = roundIntermediate(x, mode);
        checkInRange(MIN_LONG_AS_DOUBLE - z < 1.0 & z < MAX_LONG_AS_DOUBLE_PLUS_ONE);
        return (long) z;
    }

    /**
     * Returns the {@code BigInteger} value that is equal to {@code x} rounded with the specified
     * rounding mode, if possible.
     *
     * @throws ArithmeticException if
     *     <ul>
     *     <li>{@code x} is infinite or NaN
     *     <li>{@code x} is not a mathematical integer and {@code mode} is
     *         {@link RoundingMode#UNNECESSARY}
     *     </ul>
     */
    // #roundIntermediate, java.lang.Math.getExponent, com.google.common.math.DoubleUtils
    public static BigInteger roundToBigInteger(double x, RoundingMode mode) {
        x = roundIntermediate(x, mode);
        if (MIN_LONG_AS_DOUBLE - x < 1.0 & x < MAX_LONG_AS_DOUBLE_PLUS_ONE) {
            return BigInteger.valueOf((long) x);
        }
        int exponent = getExponent(x);
        long significand = getSignificand(x);
        BigInteger result = BigInteger.valueOf(significand).shiftLeft(exponent - SIGNIFICAND_BITS);
        return (x < 0) ? result.negate() : result;
    }

    static final int MAX_FACTORIAL = 170;

    static final double[] everySixteenthFactorial = { 0x1.0p0, 0x1.30777758p44, 0x1.956ad0aae33a4p117, 0x1.ee69a78d72cb6p202, 0x1.fe478ee34844ap295,
            0x1.c619094edabffp394, 0x1.3638dd7bd6347p498, 0x1.7cac197cfe503p605, 0x1.1e5dfc140e1e5p716, 0x1.8ce85fadb707ep829, 0x1.95d5f3d928edep945 };

    /**
     * Returns {@code true} if {@code a} and {@code b} are within {@code tolerance} of each other.
     *
     * <p>Technically speaking, this is equivalent to
     * {@code Math.abs(a - b) <= tolerance || Double.valueOf(a).equals(Double.valueOf(b))}.
     *
     * <p>Notable special cases include:
     * <ul>
     * <li>All NaNs are fuzzily equal.
     * <li>If {@code a == b}, then {@code a} and {@code b} are always fuzzily equal.
     * <li>Positive and negative zero are always fuzzily equal.
     * <li>If {@code tolerance} is zero, and neither {@code a} nor {@code b} is NaN, then {@code a}
     *     and {@code b} are fuzzily equal if and only if {@code a == b}.
     * <li>With {@link Double#POSITIVE_INFINITY} tolerance, all non-NaN values are fuzzily equal.
     * <li>With finite tolerance, {@code Double.POSITIVE_INFINITY} and {@code
     *     Double.NEGATIVE_INFINITY} are fuzzily equal only to themselves.
     *
     * <p>This is reflexive and symmetric, but <em>not</em> transitive, so it is <em>not</em> an
     * equivalence relation and <em>not</em> suitable for use in {@link Object#equals}
     * implementations.
     *
     * @throws IllegalArgumentException if {@code tolerance} is {@code < 0} or NaN
     * @since 13.0
     */
    public static boolean fuzzyEquals(double a, double b, double tolerance) {
        checkNonNegative("tolerance", tolerance);
        return Math.copySign(a - b, 1.0) <= tolerance
                // copySign(x, 1.0) is a branch-free version of abs(x), but with different NaN semantics
                || (a == b) // needed to ensure that infinities equal themselves
                || (Double.isNaN(a) && Double.isNaN(b));
    }

    /**
     * Compares {@code a} and {@code b} "fuzzily," with a tolerance for nearly-equal values.
     *
     * <p>This method is equivalent to
     * {@code fuzzyEquals(a, b, tolerance) ? 0 : Double.compare(a, b)}. In particular, like
     * {@link Double#compare(double, double)}, it treats all NaN values as equal and greater than all
     * other values (including {@link Double#POSITIVE_INFINITY}).
     *
     * <p>This is <em>not</em> a total ordering and is <em>not</em> suitable for use in
     * {@link Comparable#compareTo} implementations. In particular, it is not transitive.
     *
     * @throws IllegalArgumentException if {@code tolerance} is {@code < 0} or NaN
     * @since 13.0
     */
    public static int fuzzyCompare(double a, double b, double tolerance) {
        if (fuzzyEquals(a, b, tolerance)) {
            return 0;
        } else if (a < b) {
            return -1;
        } else if (a > b) {
            return 1;
        } else {
            return Boolean.compare(Double.isNaN(a), Double.isNaN(b));
        }
    }

    /**
     * Returns {@code true} if {@code x} represents a mathematical integer.
     *
     * <p>This is equivalent to, but not necessarily implemented as, the expression {@code
     * !Double.isNaN(x) && !Double.isInfinite(x) && x == Math.rint(x)}.
     */
    public static boolean isMathematicalInteger(double x) {
        return isFinite(x) && (x == 0.0 || SIGNIFICAND_BITS - Long.numberOfTrailingZeros(getSignificand(x)) <= getExponent(x));
    }

    /**
     * Returns 1 if {@code x < y} as unsigned longs, and 0 otherwise. Assumes that x - y fits into a
     * signed long. The implementation is branch-free, and benchmarks suggest it is measurably faster
     * than the straightforward ternary expression.
     */
    static int lessThanBranchFree(long x, long y) {
        // Returns the sign bit of x - y.
        return (int) (~~(x - y) >>> (Long.SIZE - 1));
    }

    static int log10Floor(long x) {
        /*
         * Based on Hacker's Delight Fig. 11-5, the two-table-lookup, branch-free implementation.
         *
         * The key idea is that based on the number of leading zeros (equivalently, floor(log2(x))), we
         * can narrow the possible floor(log10(x)) values to two. For example, if floor(log2(x)) is 6,
         * then 64 <= x < 128, so floor(log10(x)) is either 1 or 2.
         */
        int y = maxLog10ForLeadingZeros[Long.numberOfLeadingZeros(x)];
        /*
         * y is the higher of the two possible values of floor(log10(x)). If x < 10^y, then we want the
         * lower of the two possible values, or y - 1, otherwise, we want y.
         */
        return y - lessThanBranchFree(x, powersOf10[y]);
    }

    /**
     * Returns (x * numerator / denominator), which is assumed to come out to an integral value.
     */
    static long multiplyFraction(long x, long numerator, long denominator) {
        if (x == 1) {
            return numerator / denominator;
        }
        long commonDivisor = gcd(x, denominator);
        x /= commonDivisor;
        denominator /= commonDivisor;
        // We know gcd(x, denominator) = 1, and x * numerator / denominator is exact,
        // so denominator must be a divisor of numerator.
        return x * (numerator / denominator);
    }

    static double nextDown(double d) {
        return -Math.nextUp(-d);
    }

    static long getSignificand(double d) {
        N.checkArgument(isFinite(d), "not a normal value");
        int exponent = getExponent(d);
        long bits = doubleToRawLongBits(d);
        bits &= SIGNIFICAND_MASK;
        return (exponent == MIN_EXPONENT - 1) ? bits << 1 : bits | IMPLICIT_BIT;
    }

    // These values were generated by using checkedMultiply to see when the simple multiply/divide
    // algorithm would lead to an overflow.

    static boolean isFinite(double d) {
        return getExponent(d) <= MAX_EXPONENT;
    }

    static boolean isNormal(double d) {
        return getExponent(d) >= MIN_EXPONENT;
    }

    /*
     * Returns x scaled by a power of 2 such that it is in the range [1, 2). Assumes x is positive,
     * normal, and finite.
     */
    static double scaleNormalize(double x) {
        long significand = doubleToRawLongBits(x) & SIGNIFICAND_MASK;
        return longBitsToDouble(significand | ONE_BITS);
    }

    static double bigToDouble(BigInteger x) {
        // This is an extremely fast implementation of BigInteger.doubleValue(). JDK patch pending.
        BigInteger absX = x.abs();
        int exponent = absX.bitLength() - 1;
        // exponent == floor(log2(abs(x)))
        if (exponent < Long.SIZE - 1) {
            return x.longValue();
        } else if (exponent > MAX_EXPONENT) {
            return x.signum() * POSITIVE_INFINITY;
        }

        /*
         * We need the top SIGNIFICAND_BITS + 1 bits, including the "implicit" one bit. To make rounding
         * easier, we pick out the top SIGNIFICAND_BITS + 2 bits, so we have one to help us round up or
         * down. twiceSignifFloor will contain the top SIGNIFICAND_BITS + 2 bits, and signifFloor the
         * top SIGNIFICAND_BITS + 1.
         *
         * It helps to consider the real number signif = absX * 2^(SIGNIFICAND_BITS - exponent).
         */
        int shift = exponent - SIGNIFICAND_BITS - 1;
        long twiceSignifFloor = absX.shiftRight(shift).longValue();
        long signifFloor = twiceSignifFloor >> 1;
        signifFloor &= SIGNIFICAND_MASK; // remove the implied bit

        /*
         * We round up if either the fractional part of signif is strictly greater than 0.5 (which is
         * true if the 0.5 bit is set and any lower bit is set), or if the fractional part of signif is
         * >= 0.5 and signifFloor is odd (which is true if both the 0.5 bit and the 1 bit are set).
         */
        boolean increment = (twiceSignifFloor & 1) != 0 && ((signifFloor & 1) != 0 || absX.getLowestSetBit() < shift);
        long signifRounded = increment ? signifFloor + 1 : signifFloor;
        long bits = (long) ((exponent + EXPONENT_BIAS)) << SIGNIFICAND_BITS;
        bits += signifRounded;
        /*
         * If signifRounded == 2^53, we'd need to set all of the significand bits to zero and add 1 to
         * the exponent. This is exactly the behavior we get from just adding signifRounded to bits
         * directly. If the exponent is MAX_DOUBLE_EXPONENT, we round up (correctly) to
         * Double.POSITIVE_INFINITY.
         */
        bits |= x.signum() & SIGN_MASK;
        return longBitsToDouble(bits);
    }

    /**
     * Returns its argument if it is non-negative, zero if it is negative.
     */
    static double ensureNonNegative(double value) {
        N.checkArgument(!isNaN(value));
        if (value > 0.0) {
            return value;
        } else {
            return 0.0;
        }
    }

    static int lessThanBranchFree(int x, int y) {
        // The double negation is optimized away by normal Java, but is necessary for GWT
        // to make sure bit twiddling works as expected.
        return ~~(x - y) >>> (Integer.SIZE - 1);
    }

    // These values were generated by using checkedMultiply to see when the simple multiply/divide
    // algorithm would lead to an overflow.

    static boolean fitsInInt(long x) {
        return (int) x == x;
    }

    static int checkPositive(String role, int x) {
        if (x <= 0) {
            throw new IllegalArgumentException(role + " (" + x + ") must be > 0");
        }
        return x;
    }

    static long checkPositive(String role, long x) {
        if (x <= 0) {
            throw new IllegalArgumentException(role + " (" + x + ") must be > 0");
        }
        return x;
    }

    static BigInteger checkPositive(String role, BigInteger x) {
        if (x.signum() <= 0) {
            throw new IllegalArgumentException(role + " (" + x + ") must be > 0");
        }
        return x;
    }

    static int checkNonNegative(String role, int x) {
        if (x < 0) {
            throw new IllegalArgumentException(role + " (" + x + ") must be >= 0");
        }
        return x;
    }

    static long checkNonNegative(String role, long x) {
        if (x < 0) {
            throw new IllegalArgumentException(role + " (" + x + ") must be >= 0");
        }
        return x;
    }

    static BigInteger checkNonNegative(String role, BigInteger x) {
        if (x.signum() < 0) {
            throw new IllegalArgumentException(role + " (" + x + ") must be >= 0");
        }
        return x;
    }

    static double checkNonNegative(String role, double x) {
        if (!(x >= 0)) { // not x < 0, to work with NaN.
            throw new IllegalArgumentException(role + " (" + x + ") must be >= 0");
        }
        return x;
    }

    static void checkRoundingUnnecessary(boolean condition) {
        if (!condition) {
            throw new ArithmeticException("mode was UNNECESSARY, but rounding was necessary");
        }
    }

    static void checkInRange(boolean condition) {
        if (!condition) {
            throw new ArithmeticException("not in range");
        }
    }

    static void checkNoOverflow(boolean condition) {
        if (!condition) {
            throw new ArithmeticException("overflow");
        }
    }

    /** Compute the inverse hyperbolic sine of a number.
     * @param a number on which evaluation is done
     * @return inverse hyperbolic sine of a
     */
    public static double asinh(double a) {
        boolean negative = false;
        if (a < 0) {
            negative = true;
            a = -a;
        }

        double absAsinh;
        if (a > 0.167) {
            absAsinh = Math.log(Math.sqrt(a * a + 1) + a);
        } else {
            final double a2 = a * a;
            if (a > 0.097) {
                absAsinh = a * (1 - a2 * (F_1_3 - a2 * (F_1_5 - a2
                        * (F_1_7 - a2 * (F_1_9 - a2 * (F_1_11 - a2 * (F_1_13 - a2 * (F_1_15 - a2 * F_1_17 * F_15_16) * F_13_14) * F_11_12) * F_9_10) * F_7_8)
                        * F_5_6) * F_3_4) * F_1_2);
            } else if (a > 0.036) {
                absAsinh = a * (1
                        - a2 * (F_1_3 - a2 * (F_1_5 - a2 * (F_1_7 - a2 * (F_1_9 - a2 * (F_1_11 - a2 * F_1_13 * F_11_12) * F_9_10) * F_7_8) * F_5_6) * F_3_4)
                                * F_1_2);
            } else if (a > 0.0036) {
                absAsinh = a * (1 - a2 * (F_1_3 - a2 * (F_1_5 - a2 * (F_1_7 - a2 * F_1_9 * F_7_8) * F_5_6) * F_3_4) * F_1_2);
            } else {
                absAsinh = a * (1 - a2 * (F_1_3 - a2 * F_1_5 * F_3_4) * F_1_2);
            }
        }

        return negative ? -absAsinh : absAsinh;
    }

    /** Compute the inverse hyperbolic cosine of a number.
     * @param a number on which evaluation is done
     * @return inverse hyperbolic cosine of a
     */
    public static double acosh(final double a) {
        return Math.log(a + Math.sqrt(a * a - 1));
    }

    /** Compute the inverse hyperbolic tangent of a number.
     * @param a number on which evaluation is done
     * @return inverse hyperbolic tangent of a
     */
    public static double atanh(double a) {
        boolean negative = false;
        if (a < 0) {
            negative = true;
            a = -a;
        }

        double absAtanh;
        if (a > 0.15) {
            absAtanh = 0.5 * Math.log((1 + a) / (1 - a));
        } else {
            final double a2 = a * a;
            if (a > 0.087) {
                absAtanh = a * (1 + a2 * (F_1_3 + a2 * (F_1_5 + a2 * (F_1_7 + a2 * (F_1_9 + a2 * (F_1_11 + a2 * (F_1_13 + a2 * (F_1_15 + a2 * F_1_17))))))));
            } else if (a > 0.031) {
                absAtanh = a * (1 + a2 * (F_1_3 + a2 * (F_1_5 + a2 * (F_1_7 + a2 * (F_1_9 + a2 * (F_1_11 + a2 * F_1_13))))));
            } else if (a > 0.003) {
                absAtanh = a * (1 + a2 * (F_1_3 + a2 * (F_1_5 + a2 * (F_1_7 + a2 * F_1_9))));
            } else {
                absAtanh = a * (1 + a2 * (F_1_3 + a2 * F_1_5));
            }
        }

        return negative ? -absAtanh : absAtanh;
    }

    static final class UnsignedLongs {
        private UnsignedLongs() {
        }

        public static final long MAX_VALUE = -1L; // Equivalent to 2^64 - 1

        /**
         * A (self-inverse) bijection which converts the ordering on unsigned longs to the ordering on
         * longs, that is, {@code a <= b} as unsigned longs if and only if {@code flip(a) <= flip(b)} as
         * signed longs.
         */
        private static long flip(long a) {
            return a ^ Long.MIN_VALUE;
        }

        /**
         * Compares the two specified {@code long} values, treating them as unsigned values between
         * {@code 0} and {@code 2^64 - 1} inclusive.
         *
         * @param a the first unsigned {@code long} to compare
         * @param b the second unsigned {@code long} to compare
         * @return a negative value if {@code a} is less than {@code b}; a positive value if {@code a} is
         *     greater than {@code b}; or zero if they are equal
         */
        static int compare(long a, long b) {
            return Long.compare(flip(a), flip(b));
        }

        /**
         * Returns dividend % divisor, where the dividend and divisor are treated as unsigned 64-bit
         * quantities.
         *
         * @param dividend the dividend (numerator)
         * @param divisor the divisor (denominator)
         * @throws ArithmeticException if divisor is 0
         * @since 11.0
         */
        static long remainder(long dividend, long divisor) {
            if (divisor < 0) { // i.e., divisor >= 2^63:
                if (compare(dividend, divisor) < 0) {
                    return dividend; // dividend < divisor
                } else {
                    return dividend - divisor; // dividend >= divisor
                }
            }

            // Optimization - use signed modulus if dividend < 2^63
            if (dividend >= 0) {
                return dividend % divisor;
            }

            /*
             * Otherwise, approximate the quotient, check, and correct if necessary. Our approximation is
             * guaranteed to be either exact or one less than the correct value. This follows from the fact
             * that floor(floor(x)/i) == floor(x/i) for any real x and integer i != 0. The proof is not
             * quite trivial.
             */
            long quotient = ((dividend >>> 1) / divisor) << 1;
            long rem = dividend - quotient * divisor;
            return rem - (compare(rem, divisor) >= 0 ? divisor : 0);
        }

    }

    private enum MillerRabinTester {
        /**
         * Works for inputs <= FLOOR_SQRT_MAX_LONG.
         */
        SMALL {
            @Override
            long mulMod(long a, long b, long m) {
                /*
                 * NOTE(lowasser, 2015-Feb-12): Benchmarks suggest that changing this to
                 * UnsignedLongs.remainder and increasing the threshold to 2^32 doesn't pay for itself, and
                 * adding another enum constant hurts performance further -- I suspect because bimorphic
                 * implementation is a sweet spot for the JVM.
                 */
                return (a * b) % m;
            }

            @Override
            long squareMod(long a, long m) {
                return (a * a) % m;
            }
        },
        /**
         * Works for all nonnegative signed longs.
         */
        LARGE {
            /**
             * Returns (a + b) mod m. Precondition: 0 <= a, b < m < 2^63.
             */
            private long plusMod(long a, long b, long m) {
                return (a >= m - b) ? (a + b - m) : (a + b);
            }

            /**
             * Returns (a * 2^32) mod m. a may be any unsigned long.
             */
            private long times2ToThe32Mod(long a, long m) {
                int remainingPowersOf2 = 32;
                do {
                    int shift = Math.min(remainingPowersOf2, Long.numberOfLeadingZeros(a));
                    // shift is either the number of powers of 2 left to multiply a by, or the biggest shift
                    // possible while keeping a in an unsigned long.
                    a = UnsignedLongs.remainder(a << shift, m);
                    remainingPowersOf2 -= shift;
                } while (remainingPowersOf2 > 0);
                return a;
            }

            @Override
            long mulMod(long a, long b, long m) {
                long aHi = a >>> 32; // < 2^31
                long bHi = b >>> 32; // < 2^31
                long aLo = a & 0xFFFFFFFFL; // < 2^32
                long bLo = b & 0xFFFFFFFFL; // < 2^32

                /*
                 * a * b == aHi * bHi * 2^64 + (aHi * bLo + aLo * bHi) * 2^32 + aLo * bLo.
                 *       == (aHi * bHi * 2^32 + aHi * bLo + aLo * bHi) * 2^32 + aLo * bLo
                 *
                 * We carry out this computation in modular arithmetic. Since times2ToThe32Mod accepts any
                 * unsigned long, we don't have to do a mod on every operation, only when intermediate
                 * results can exceed 2^63.
                 */
                long result = times2ToThe32Mod(aHi * bHi /* < 2^62 */, m); // < m < 2^63
                result += aHi * bLo; // aHi * bLo < 2^63, result < 2^64
                if (result < 0) {
                    result = UnsignedLongs.remainder(result, m);
                }
                // result < 2^63 again
                result += aLo * bHi; // aLo * bHi < 2^63, result < 2^64
                result = times2ToThe32Mod(result, m); // result < m < 2^63
                return plusMod(result, UnsignedLongs.remainder(aLo * bLo /* < 2^64 */, m), m);
            }

            @Override
            long squareMod(long a, long m) {
                long aHi = a >>> 32; // < 2^31
                long aLo = a & 0xFFFFFFFFL; // < 2^32

                /*
                 * a^2 == aHi^2 * 2^64 + aHi * aLo * 2^33 + aLo^2
                 *     == (aHi^2 * 2^32 + aHi * aLo * 2) * 2^32 + aLo^2
                 * We carry out this computation in modular arithmetic.  Since times2ToThe32Mod accepts any
                 * unsigned long, we don't have to do a mod on every operation, only when intermediate
                 * results can exceed 2^63.
                 */
                long result = times2ToThe32Mod(aHi * aHi /* < 2^62 */, m); // < m < 2^63
                long hiLo = aHi * aLo * 2;
                if (hiLo < 0) {
                    hiLo = UnsignedLongs.remainder(hiLo, m);
                }
                // hiLo < 2^63
                result += hiLo; // result < 2^64
                result = times2ToThe32Mod(result, m); // result < m < 2^63
                return plusMod(result, UnsignedLongs.remainder(aLo * aLo /* < 2^64 */, m), m);
            }
        };

        static boolean test(long base, long n) {
            // Since base will be considered % n, it's okay if base > FLOOR_SQRT_MAX_LONG,
            // so long as n <= FLOOR_SQRT_MAX_LONG.
            return ((n <= FLOOR_SQRT_MAX_LONG) ? SMALL : LARGE).testWitness(base, n);
        }

        /**
         * Returns a * b mod m.
         */
        abstract long mulMod(long a, long b, long m);

        /**
         * Returns a^2 mod m.
         */
        abstract long squareMod(long a, long m);

        /**
         * Returns a^p mod m.
         */
        private long powMod(long a, long p, long m) {
            long res = 1;
            for (; p != 0; p >>= 1) {
                if ((p & 1) != 0) {
                    res = mulMod(res, a, m);
                }
                a = squareMod(a, m);
            }
            return res;
        }

        /**
         * Returns true if n is a strong probable prime relative to the specified base.
         */
        private boolean testWitness(long base, long n) {
            int r = Long.numberOfTrailingZeros(n - 1);
            long d = (n - 1) >> r;
            base %= n;
            if (base == 0) {
                return true;
            }
            // Calculate a := base^d mod n.
            long a = powMod(base, d, n);
            // n passes this test if
            //    base^d = 1 (mod n)
            // or base^(2^j * d) = -1 (mod n) for some 0 <= j < r.
            if (a == 1) {
                return true;
            }
            int j = 0;
            while (a != n - 1) {
                if (++j == r) {
                    return false;
                }
                a = squareMod(a, n);
            }
            return true;
        }
    }

    public static abstract class Mathh extends Matth {
        private Mathh() {
            // utility class.
        }
    }
}

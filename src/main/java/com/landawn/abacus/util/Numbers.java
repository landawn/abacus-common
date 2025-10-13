/*
 * Copyright (C) 2017 HaiYang Li
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
import java.text.DecimalFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.UnaryOperator;
import java.util.regex.Matcher;

import com.landawn.abacus.annotation.MayReturnNull;
import com.landawn.abacus.annotation.SuppressFBWarnings;
import com.landawn.abacus.type.Type;

/**
 * <p>Note: A lot of codes in this classed are copied from Google Guava, Apache Commons Math and Apache Commons Lang under the Apache License, Version 2.0 and may be modified</p>
 * <br>The purpose of copying the code is to re-organize the APIs.</br>
 *
 * When to throw exception? It's designed to avoid throwing any unnecessary
 * exception if the contract defined by method is not broken. For example, if
 * user tries to reverse a {@code null} or empty String. The input String will be
 * returned. But exception will be thrown if try to add an element to a {@code null} Object array or collection.
 * <br />
 * <br />
 * An empty String/Array/Collection/Map/Iterator/Iterable/InputStream/Reader will always be a preferred choice than a {@code null} for the return value of a method.
 * <br />
 *
 * @see DecimalFormat
 * @see com.landawn.abacus.util.IEEE754rUtil
 * @see com.landawn.abacus.util.Strings
 * @see com.landawn.abacus.util.RegExUtil
 */
@SuppressWarnings({ "java:S1192", "java:S2148" })
public final class Numbers {

    /** Reusable Byte constant for zero. */
    public static final Byte BYTE_ZERO = (byte) 0;
    /** Reusable Byte constant for one. */
    public static final Byte BYTE_ONE = (byte) 1;
    /** Reusable Byte constant for minus one. */
    public static final Byte BYTE_MINUS_ONE = (byte) -1;
    /** Reusable Short constant for zero. */
    public static final Short SHORT_ZERO = (short) 0;
    /** Reusable Short constant for one. */
    public static final Short SHORT_ONE = (short) 1;
    /** Reusable Short constant for minus one. */
    public static final Short SHORT_MINUS_ONE = (short) -1;
    /** Reusable Integer constant for zero. */
    public static final Integer INTEGER_ZERO = 0;
    /** Reusable Integer constant for one. */
    public static final Integer INTEGER_ONE = 1;
    /** Reusable Integer constant for two */
    public static final Integer INTEGER_TWO = 2;
    /** Reusable Integer constant for minus one. */
    public static final Integer INTEGER_MINUS_ONE = -1;
    /** Reusable Long constant for zero. */
    public static final Long LONG_ZERO = 0L;
    /** Reusable Long constant for one. */
    public static final Long LONG_ONE = 1L;
    /** Reusable Long constant for minus one. */
    public static final Long LONG_MINUS_ONE = -1L;
    /** Reusable Float constant for zero. */
    public static final Float FLOAT_ZERO = 0.0f;
    /** Reusable Float constant for one. */
    public static final Float FLOAT_ONE = 1.0f;
    /** Reusable Float constant for minus one. */
    public static final Float FLOAT_MINUS_ONE = -1.0f;
    /** Reusable Double constant for zero. */
    public static final Double DOUBLE_ZERO = 0.0d;
    /** Reusable Double constant for one. */
    public static final Double DOUBLE_ONE = 1.0d;
    /** Reusable Double constant for minus one. */
    public static final Double DOUBLE_MINUS_ONE = -1.0d;

    /** Positive zero. */
    private static final double DOUBLE_POSITIVE_ZERO = 0d;

    /** Positive zero. */
    private static final float FLOAT_POSITIVE_ZERO = 0f;

    private Numbers() {
        // utility class.
    }

    private static final long ONE_BITS = doubleToRawLongBits(1.0);

    /** The biggest half-power of two that can fit in an unsigned int. */
    static final int INT_MAX_POWER_OF_SQRT2_UNSIGNED = 0xB504F333;

    /**  The biggest half-power of two that fits into an unsigned long. */
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

    private static final int[] int_factorials = { 1, 1, 2, 2 * 3, 2 * 3 * 4, 2 * 3 * 4 * 5, 2 * 3 * 4 * 5 * 6, 2 * 3 * 4 * 5 * 6 * 7, 2 * 3 * 4 * 5 * 6 * 7 * 8,
            2 * 3 * 4 * 5 * 6 * 7 * 8 * 9, 2 * 3 * 4 * 5 * 6 * 7 * 8 * 9 * 10, 2 * 3 * 4 * 5 * 6 * 7 * 8 * 9 * 10 * 11,
            2 * 3 * 4 * 5 * 6 * 7 * 8 * 9 * 10 * 11 * 12 };

    // binomial(biggestBinomials[k], k) fits in an int, but not binomial(biggestBinomials[k]+1,k).
    static final int[] int_biggestBinomials = { Integer.MAX_VALUE, Integer.MAX_VALUE, 65536, 2345, 477, 193, 110, 75, 58, 49, 43, 39, 37, 35, 34, 34, 33 }; //NOSONAR

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

    static final long[] long_factorials = { 1L, 1L, 2L, (long) 2 * 3, (long) 2 * 3 * 4, (long) 2 * 3 * 4 * 5, (long) 2 * 3 * 4 * 5 * 6,
            (long) 2 * 3 * 4 * 5 * 6 * 7, (long) 2 * 3 * 4 * 5 * 6 * 7 * 8, (long) 2 * 3 * 4 * 5 * 6 * 7 * 8 * 9, (long) 2 * 3 * 4 * 5 * 6 * 7 * 8 * 9 * 10,
            (long) 2 * 3 * 4 * 5 * 6 * 7 * 8 * 9 * 10 * 11, (long) 2 * 3 * 4 * 5 * 6 * 7 * 8 * 9 * 10 * 11 * 12,
            (long) 2 * 3 * 4 * 5 * 6 * 7 * 8 * 9 * 10 * 11 * 12 * 13, (long) 2 * 3 * 4 * 5 * 6 * 7 * 8 * 9 * 10 * 11 * 12 * 13 * 14,
            (long) 2 * 3 * 4 * 5 * 6 * 7 * 8 * 9 * 10 * 11 * 12 * 13 * 14 * 15, (long) 2 * 3 * 4 * 5 * 6 * 7 * 8 * 9 * 10 * 11 * 12 * 13 * 14 * 15 * 16,
            (long) 2 * 3 * 4 * 5 * 6 * 7 * 8 * 9 * 10 * 11 * 12 * 13 * 14 * 15 * 16 * 17,
            (long) 2 * 3 * 4 * 5 * 6 * 7 * 8 * 9 * 10 * 11 * 12 * 13 * 14 * 15 * 16 * 17 * 18,
            (long) 2 * 3 * 4 * 5 * 6 * 7 * 8 * 9 * 10 * 11 * 12 * 13 * 14 * 15 * 16 * 17 * 18 * 19,
            (long) 2 * 3 * 4 * 5 * 6 * 7 * 8 * 9 * 10 * 11 * 12 * 13 * 14 * 15 * 16 * 17 * 18 * 19 * 20 };

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
     * This bitmask is used as an optimization for cheaply testing for divisibility by 2, 3, or 5.
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

    static final BigDecimal BIG_DECIMAL_WITH_MIN_BYTE_VALUE = BigDecimal.valueOf(Byte.MIN_VALUE);
    static final BigDecimal BIG_DECIMAL_WITH_MIN_SHORT_VALUE = BigDecimal.valueOf(Short.MIN_VALUE);
    static final BigDecimal BIG_DECIMAL_WITH_MIN_INT_VALUE = BigDecimal.valueOf(Integer.MIN_VALUE);
    static final BigDecimal BIG_DECIMAL_WITH_MIN_LONG_VALUE = BigDecimal.valueOf(Long.MIN_VALUE);
    static final BigDecimal BIG_DECIMAL_WITH_MIN_FLOAT_VALUE = BigDecimal.valueOf(-Float.MAX_VALUE);
    static final BigDecimal BIG_DECIMAL_WITH_MIN_DOUBLE_VALUE = BigDecimal.valueOf(-Double.MAX_VALUE);

    static final BigDecimal BIG_DECIMAL_WITH_MAX_BYTE_VALUE = BigDecimal.valueOf(Byte.MAX_VALUE);
    static final BigDecimal BIG_DECIMAL_WITH_MAX_SHORT_VALUE = BigDecimal.valueOf(Short.MAX_VALUE);
    static final BigDecimal BIG_DECIMAL_WITH_MAX_INT_VALUE = BigDecimal.valueOf(Integer.MAX_VALUE);
    static final BigDecimal BIG_DECIMAL_WITH_MAX_LONG_VALUE = BigDecimal.valueOf(Long.MAX_VALUE);
    static final BigDecimal BIG_DECIMAL_WITH_MAX_FLOAT_VALUE = BigDecimal.valueOf(Float.MAX_VALUE);
    static final BigDecimal BIG_DECIMAL_WITH_MAX_DOUBLE_VALUE = BigDecimal.valueOf(Double.MAX_VALUE);

    static final BigInteger BIG_INTEGER_WITH_MIN_BYTE_VALUE = BigInteger.valueOf(Byte.MIN_VALUE);
    static final BigInteger BIG_INTEGER_WITH_MIN_SHORT_VALUE = BigInteger.valueOf(Short.MIN_VALUE);
    static final BigInteger BIG_INTEGER_WITH_MIN_INT_VALUE = BigInteger.valueOf(Integer.MIN_VALUE);
    static final BigInteger BIG_INTEGER_WITH_MIN_LONG_VALUE = BigInteger.valueOf(Long.MIN_VALUE);
    static final BigInteger BIG_INTEGER_WITH_MIN_FLOAT_VALUE = BIG_DECIMAL_WITH_MIN_FLOAT_VALUE.toBigInteger();
    static final BigInteger BIG_INTEGER_WITH_MIN_DOUBLE_VALUE = BIG_DECIMAL_WITH_MIN_DOUBLE_VALUE.toBigInteger();

    static final BigInteger BIG_INTEGER_WITH_MAX_BYTE_VALUE = BigInteger.valueOf(Byte.MAX_VALUE);
    static final BigInteger BIG_INTEGER_WITH_MAX_SHORT_VALUE = BigInteger.valueOf(Short.MAX_VALUE);
    static final BigInteger BIG_INTEGER_WITH_MAX_INT_VALUE = BigInteger.valueOf(Integer.MAX_VALUE);
    static final BigInteger BIG_INTEGER_WITH_MAX_LONG_VALUE = BigInteger.valueOf(Long.MAX_VALUE);
    static final BigInteger BIG_INTEGER_WITH_MAX_FLOAT_VALUE = BIG_DECIMAL_WITH_MAX_FLOAT_VALUE.toBigInteger();
    static final BigInteger BIG_INTEGER_WITH_MAX_DOUBLE_VALUE = BIG_DECIMAL_WITH_MAX_DOUBLE_VALUE.toBigInteger();

    private static final Map<Class<?>, Map<Class<?>, UnaryOperator<Number>>> numberConverterFuncMap = new HashMap<>();

    static {
        Map<Class<?>, UnaryOperator<Number>> temp = new HashMap<>();
        temp.put(byte.class, UnaryOperator.identity());

        temp.put(short.class, it -> {
            final short value = it.shortValue();

            if (value > Byte.MAX_VALUE || value < Byte.MIN_VALUE) {
                throw new ArithmeticException("byte overflow: " + it);
            }

            //noinspection UnnecessaryBoxing
            return Byte.valueOf(it.byteValue());
        });

        temp.put(int.class, it -> {
            final int value = it.intValue();

            if (value > Byte.MAX_VALUE || value < Byte.MIN_VALUE) {
                throw new ArithmeticException("byte overflow: " + it);
            }

            //noinspection UnnecessaryBoxing
            return Byte.valueOf(it.byteValue());
        });

        temp.put(long.class, it -> {
            final long value = it.longValue();

            if (value > Byte.MAX_VALUE || value < Byte.MIN_VALUE) {
                throw new ArithmeticException("byte overflow: " + it);
            }

            //noinspection UnnecessaryBoxing
            return Byte.valueOf(it.byteValue());
        });

        temp.put(float.class, it -> {
            if (Float.compare(it.floatValue(), Byte.MAX_VALUE) > 0 || Float.compare(it.floatValue(), Byte.MIN_VALUE) < 0) {
                throw new ArithmeticException("byte overflow: " + it);
            }

            //noinspection UnnecessaryBoxing
            return Byte.valueOf(it.byteValue());
        });

        temp.put(double.class, it -> {
            if (Double.compare(it.doubleValue(), Byte.MAX_VALUE) > 0 || Double.compare(it.doubleValue(), Byte.MIN_VALUE) < 0) {
                throw new ArithmeticException("byte overflow: " + it);
            }

            //noinspection UnnecessaryBoxing
            return Byte.valueOf(it.byteValue());
        });

        temp.put(BigInteger.class, it -> {
            final BigInteger bigInteger = (BigInteger) it;

            if (bigInteger.compareTo(BIG_INTEGER_WITH_MAX_BYTE_VALUE) > 0 || bigInteger.compareTo(BIG_INTEGER_WITH_MIN_BYTE_VALUE) < 0) {
                throw new ArithmeticException("byte overflow: " + it);
            }

            //noinspection UnnecessaryBoxing
            return Byte.valueOf(it.byteValue());
        });

        temp.put(BigDecimal.class, it -> {
            final BigDecimal bigDecimal = (BigDecimal) it;

            if (bigDecimal.compareTo(BIG_DECIMAL_WITH_MAX_BYTE_VALUE) > 0 || bigDecimal.compareTo(BIG_DECIMAL_WITH_MIN_BYTE_VALUE) < 0) {
                throw new ArithmeticException("byte overflow: " + it);
            }

            //noinspection UnnecessaryBoxing
            return Byte.valueOf(it.byteValue());
        });

        numberConverterFuncMap.put(byte.class, temp);

        // ================ for short.class

        temp = new HashMap<>();
        temp.put(byte.class, Number::shortValue);
        temp.put(short.class, UnaryOperator.identity());

        temp.put(int.class, it -> {
            final int value = it.intValue();

            if (value > Short.MAX_VALUE || value < Short.MIN_VALUE) {
                throw new ArithmeticException("short overflow: " + it);
            }

            //noinspection UnnecessaryBoxing
            return Short.valueOf(it.shortValue());
        });

        temp.put(long.class, it -> {
            final long value = it.longValue();

            if (value > Short.MAX_VALUE || value < Short.MIN_VALUE) {
                throw new ArithmeticException("short overflow: " + it);
            }

            //noinspection UnnecessaryBoxing
            return Short.valueOf(it.shortValue());
        });

        temp.put(float.class, it -> {
            if (Float.compare(it.floatValue(), Short.MAX_VALUE) > 0 || Float.compare(it.floatValue(), Short.MIN_VALUE) < 0) {
                throw new ArithmeticException("short overflow: " + it);
            }

            //noinspection UnnecessaryBoxing
            return Short.valueOf(it.shortValue());
        });

        temp.put(double.class, it -> {
            if (Double.compare(it.doubleValue(), Short.MAX_VALUE) > 0 || Double.compare(it.doubleValue(), Short.MIN_VALUE) < 0) {
                throw new ArithmeticException("short overflow: " + it);
            }

            //noinspection UnnecessaryBoxing
            return Short.valueOf(it.shortValue());
        });

        temp.put(BigInteger.class, it -> {
            final BigInteger bigInteger = (BigInteger) it;

            if (bigInteger.compareTo(BIG_INTEGER_WITH_MAX_SHORT_VALUE) > 0 || bigInteger.compareTo(BIG_INTEGER_WITH_MIN_SHORT_VALUE) < 0) {
                throw new ArithmeticException("short overflow: " + it);
            }

            //noinspection UnnecessaryBoxing
            return Short.valueOf(it.shortValue());
        });

        temp.put(BigDecimal.class, it -> {
            final BigDecimal bigDecimal = (BigDecimal) it;

            if (bigDecimal.compareTo(BIG_DECIMAL_WITH_MAX_SHORT_VALUE) > 0 || bigDecimal.compareTo(BIG_DECIMAL_WITH_MIN_SHORT_VALUE) < 0) {
                throw new ArithmeticException("short overflow: " + it);
            }

            //noinspection UnnecessaryBoxing
            return Short.valueOf(it.shortValue());
        });

        numberConverterFuncMap.put(short.class, temp);

        // ================ for int.class
        temp = new HashMap<>();

        temp.put(byte.class, Number::intValue);
        temp.put(short.class, Number::intValue);
        temp.put(int.class, UnaryOperator.identity());

        temp.put(long.class, it -> {
            final long value = it.longValue();

            if (value > Integer.MAX_VALUE || value < Integer.MIN_VALUE) {
                throw new ArithmeticException("integer overflow: " + it);
            }

            //noinspection UnnecessaryBoxing
            return Integer.valueOf(it.intValue());
        });

        temp.put(float.class, it -> {
            if (Float.compare(it.floatValue(), Integer.MAX_VALUE) > 0 || Float.compare(it.floatValue(), Integer.MIN_VALUE) < 0) {
                throw new ArithmeticException("integer overflow: " + it);
            }

            //noinspection UnnecessaryBoxing
            return Integer.valueOf(it.intValue());
        });

        temp.put(double.class, it -> {
            if (Double.compare(it.doubleValue(), Integer.MAX_VALUE) > 0 || Double.compare(it.doubleValue(), Integer.MIN_VALUE) < 0) {
                throw new ArithmeticException("integer overflow: " + it);
            }

            //noinspection UnnecessaryBoxing
            return Integer.valueOf(it.intValue());
        });

        temp.put(BigInteger.class, it -> {
            final BigInteger bigInteger = (BigInteger) it;

            if (bigInteger.compareTo(BIG_INTEGER_WITH_MAX_INT_VALUE) > 0 || bigInteger.compareTo(BIG_INTEGER_WITH_MIN_INT_VALUE) < 0) {
                throw new ArithmeticException("integer overflow: " + it);
            }

            //noinspection UnnecessaryBoxing
            return Integer.valueOf(it.intValue());
        });

        temp.put(BigDecimal.class, it -> {
            final BigDecimal bigDecimal = (BigDecimal) it;

            if (bigDecimal.compareTo(BIG_DECIMAL_WITH_MAX_INT_VALUE) > 0 || bigDecimal.compareTo(BIG_DECIMAL_WITH_MIN_INT_VALUE) < 0) {
                throw new ArithmeticException("integer overflow: " + it);
            }

            //noinspection UnnecessaryBoxing
            return Integer.valueOf(it.intValue());
        });

        numberConverterFuncMap.put(int.class, temp);

        // ============================for long
        temp = new HashMap<>();

        temp.put(byte.class, Number::longValue);
        temp.put(short.class, Number::longValue);
        temp.put(int.class, Number::longValue);
        temp.put(long.class, UnaryOperator.identity());

        temp.put(float.class, it -> {
            if (Float.compare(it.floatValue(), Long.MAX_VALUE) > 0 || Float.compare(it.floatValue(), Long.MIN_VALUE) < 0) {
                throw new ArithmeticException("long overflow: " + it);
            }

            //noinspection UnnecessaryBoxing
            return Long.valueOf(it.longValue());
        });

        temp.put(double.class, it -> {
            if (Double.compare(it.doubleValue(), Long.MAX_VALUE) > 0 || Double.compare(it.doubleValue(), Long.MIN_VALUE) < 0) {
                throw new ArithmeticException("long overflow: " + it);
            }

            //noinspection UnnecessaryBoxing
            return Long.valueOf(it.longValue());
        });

        temp.put(BigInteger.class, it -> {
            final BigInteger bigInteger = (BigInteger) it;

            if (bigInteger.compareTo(BIG_INTEGER_WITH_MAX_LONG_VALUE) > 0 || bigInteger.compareTo(BIG_INTEGER_WITH_MIN_LONG_VALUE) < 0) {
                throw new ArithmeticException("long overflow: " + it);
            }

            //noinspection UnnecessaryBoxing
            return Long.valueOf(it.longValue());
        });

        temp.put(BigDecimal.class, it -> {
            final BigDecimal bigDecimal = (BigDecimal) it;

            if (bigDecimal.compareTo(BIG_DECIMAL_WITH_MAX_LONG_VALUE) > 0 || bigDecimal.compareTo(BIG_DECIMAL_WITH_MIN_LONG_VALUE) < 0) {
                throw new ArithmeticException("long overflow: " + it);
            }

            //noinspection UnnecessaryBoxing
            return Long.valueOf(it.longValue());
        });

        numberConverterFuncMap.put(long.class, temp);

        // ============================for float
        temp = new HashMap<>();

        temp.put(byte.class, Number::floatValue);
        temp.put(short.class, Number::floatValue);
        temp.put(int.class, Number::floatValue);
        temp.put(long.class, Number::floatValue);

        temp.put(float.class, UnaryOperator.identity());

        temp.put(double.class, it -> {
            final Double num = (Double) it;

            if (num.isNaN()) {
                return Float.NaN;
            } else if (num.isInfinite()) {
                return Double.compare(it.doubleValue(), Double.POSITIVE_INFINITY) == 0 ? Float.POSITIVE_INFINITY : Float.NEGATIVE_INFINITY;
            } else if (Double.compare(it.doubleValue(), Float.MAX_VALUE) > 0 || Double.compare(it.doubleValue(), -Float.MAX_VALUE) < 0) {
                throw new ArithmeticException("float overflow: " + it);
            } else {
                // return Float.valueOf(num.floatValue()); // 1.21D May not be 1.21F
                return Float.parseFloat(num.toString());
            }
        });

        temp.put(BigInteger.class, it -> {
            final BigInteger bigInteger = (BigInteger) it;

            if (bigInteger.compareTo(BIG_INTEGER_WITH_MAX_FLOAT_VALUE) > 0 || bigInteger.compareTo(BIG_INTEGER_WITH_MIN_FLOAT_VALUE) < 0) {
                throw new ArithmeticException("float overflow: " + it);
            }

            //noinspection UnnecessaryBoxing
            return Float.valueOf(it.floatValue());
        });

        temp.put(BigDecimal.class, it -> {
            final BigDecimal bigDecimal = (BigDecimal) it;

            if (bigDecimal.compareTo(BIG_DECIMAL_WITH_MAX_FLOAT_VALUE) > 0 || bigDecimal.compareTo(BIG_DECIMAL_WITH_MIN_FLOAT_VALUE) < 0) {
                throw new ArithmeticException("float overflow: " + it);
            }

            //noinspection UnnecessaryBoxing
            return Float.valueOf(it.floatValue());
        });

        numberConverterFuncMap.put(float.class, temp);

        // ============================for double
        temp = new HashMap<>();

        temp.put(byte.class, Number::doubleValue);
        temp.put(short.class, Number::doubleValue);
        temp.put(int.class, Number::doubleValue);
        temp.put(long.class, Number::doubleValue);

        temp.put(float.class, it -> {
            final Float num = (Float) it;

            if (num.isNaN()) {
                return Double.NaN;
            } else if (num.isInfinite()) {
                return Float.compare(num, Float.POSITIVE_INFINITY) == 0 ? Double.POSITIVE_INFINITY : Double.NEGATIVE_INFINITY;
            } else {
                // return Double.valueOf(num.doubleValue()); // 1.21F May not be 1.21D
                return Double.parseDouble(it.toString());
            }
        });

        temp.put(double.class, UnaryOperator.identity());

        temp.put(BigInteger.class, it -> {
            final BigInteger bigInteger = (BigInteger) it;

            if (bigInteger.compareTo(BIG_INTEGER_WITH_MAX_DOUBLE_VALUE) > 0 || bigInteger.compareTo(BIG_INTEGER_WITH_MIN_DOUBLE_VALUE) < 0) {
                throw new ArithmeticException("double overflow: " + it);
            }

            //noinspection UnnecessaryBoxing
            return Double.valueOf(it.doubleValue());
        });

        temp.put(BigDecimal.class, it -> {
            final BigDecimal bigDecimal = (BigDecimal) it;

            if (bigDecimal.compareTo(BIG_DECIMAL_WITH_MAX_DOUBLE_VALUE) > 0 || bigDecimal.compareTo(BIG_DECIMAL_WITH_MIN_DOUBLE_VALUE) < 0) {
                throw new ArithmeticException("double overflow: " + it);
            }

            //noinspection UnnecessaryBoxing
            return Double.valueOf(it.doubleValue());
        });

        numberConverterFuncMap.put(double.class, temp);

        // ============================for BigInteger
        temp = new HashMap<>();

        temp.put(byte.class, it -> BigInteger.valueOf(it.byteValue()));
        temp.put(short.class, it -> BigInteger.valueOf(it.shortValue()));
        temp.put(int.class, it -> BigInteger.valueOf(it.intValue()));
        temp.put(long.class, it -> BigInteger.valueOf(it.longValue()));
        temp.put(float.class, it -> BigDecimal.valueOf(it.floatValue()).toBigInteger());
        temp.put(double.class, it -> BigDecimal.valueOf(it.doubleValue()).toBigInteger());
        temp.put(BigInteger.class, UnaryOperator.identity());
        temp.put(BigDecimal.class, it -> ((BigDecimal) it).toBigInteger());

        numberConverterFuncMap.put(BigInteger.class, temp);

        // ============================for BigDecimal
        temp = new HashMap<>();

        temp.put(byte.class, it -> BigDecimal.valueOf(it.byteValue()));
        temp.put(short.class, it -> BigDecimal.valueOf(it.shortValue()));
        temp.put(int.class, it -> BigDecimal.valueOf(it.intValue()));
        temp.put(long.class, it -> BigDecimal.valueOf(it.longValue()));
        temp.put(float.class, it -> BigDecimal.valueOf(it.floatValue()));
        temp.put(double.class, it -> BigDecimal.valueOf(it.doubleValue()));
        temp.put(BigInteger.class, it -> new BigDecimal((BigInteger) it));
        temp.put(BigDecimal.class, UnaryOperator.identity());

        numberConverterFuncMap.put(BigDecimal.class, temp);

        // =================================================================
        final BiMap<Class<?>, Class<?>> p2w = new BiMap<>();

        p2w.put(byte.class, Byte.class);
        p2w.put(short.class, Short.class);
        p2w.put(int.class, Integer.class);
        p2w.put(long.class, Long.class);
        p2w.put(float.class, Float.class);
        p2w.put(double.class, Double.class);

        final List<Class<?>> keys = new ArrayList<>(numberConverterFuncMap.keySet());

        for (final Class<?> cls : keys) {
            temp = numberConverterFuncMap.get(cls);

            final List<Class<?>> keys2 = new ArrayList<>(temp.keySet());

            for (final Class<?> cls2 : keys2) {
                if (p2w.containsKey(cls2)) {
                    temp.put(p2w.get(cls2), temp.get(cls2));
                }
            }

            if (p2w.containsKey(cls)) {
                numberConverterFuncMap.put(p2w.get(cls), temp);
            }
        }
    }

    /**
     * Converts the given number to the specified target type with overflow checking.
     * 
     * <p>This method supports conversion between all primitive number types (byte, short, int, long, float, double)
     * and their corresponding wrapper classes. It also supports conversion to and from BigInteger and BigDecimal.
     * If the conversion would result in an overflow, an ArithmeticException is thrown.</p>
     * 
     * <p>Example usage:</p>
     * <pre>{@code
     * // Convert a double to an int (throws ArithmeticException if outside int range)
     * Integer result = Numbers.convert(123.45, Integer.class);
     * 
     * // Convert a BigInteger to a long (throws ArithmeticException if too large)
     * Long longValue = Numbers.convert(new BigInteger("9223372036854775807"), Long.class);
     * 
     * // Null values return the default value for the target type
     * Byte byteValue = Numbers.convert(null, Byte.class); // Returns null
     * byte primByteValue = Numbers.convert(null, byte.class); // Returns 0
     * }</pre>
     *
     * @param <T> the target type of the conversion (must extend Number)
     * @param value the number to convert
     * @param targetType the class object representing the target type
     * @return the converted number as an instance of the target type,
     *         or the default value of the target type if the input value is null
     * @throws ArithmeticException if the conversion would result in an overflow
     */
    public static <T extends Number> T convert(final Number value, final Class<? extends T> targetType) throws ArithmeticException {
        if (value == null) {
            return N.defaultValueOf(targetType);
        }

        final Map<Class<?>, UnaryOperator<Number>> temp = numberConverterFuncMap.get(targetType);
        final UnaryOperator<Number> func = temp == null ? null : temp.get(value.getClass());

        if (func != null) {
            return (T) func.apply(value);
        } else {
            return N.valueOf(N.stringOf(value), targetType);
        }
    }

    /**
     * Converts the given number to the specified target type with overflow checking.
     * If the input value is null, returns the provided default value.
     *
     * <p>This method supports conversion between all primitive number types (byte, short, int, long, float, double)
     * and their corresponding wrapper classes. It also supports conversion to and from BigInteger and BigDecimal.
     * If the conversion would result in an overflow, an ArithmeticException is thrown.</p>
     *
     * <p>Example usage:</p>
     * <pre>{@code
     * // Convert a double to an int with a default value for null
     * Integer result = Numbers.convert(null, Integer.class, -1);
     *
     * // Convert a BigInteger to a long with a custom default
     * Long longValue = Numbers.convert(null, Long.class, 0L);
     *
     * // Convert with overflow checking
     * try {
     *     Byte byteValue = Numbers.convert(new BigInteger("1000"), Byte.class, (byte)0);
     * } catch (ArithmeticException e) {
     *     // Handle overflow exception
     * }
     * }</pre>
     *
     * @param <T> the target type of the conversion (must extend Number)
     * @param value the number to convert
     * @param targetType the class object representing the target type
     * @param defaultValueForNull the value to return if the input value is null
     * @return the converted number as an instance of the target type,
     *         or the specified default value if the input value is null
     * @throws ArithmeticException if the conversion would result in an overflow
     * @see #convert(Number, Class)
     */
    public static <T extends Number> T convert(final Number value, final Class<? extends T> targetType, T defaultValueForNull) throws ArithmeticException {
        if (value == null) {
            return defaultValueForNull;
        }

        return convert(value, targetType);
    }

    /**
     * Converts the given number to the specified target type using the provided Type instance.
     *
     * <p>This method supports conversion between all primitive number types (byte, short, int, long, float, double),
     * as well as their corresponding wrapper classes. It also supports conversion to and from BigInteger and BigDecimal.
     * If the conversion would result in an overflow, an ArithmeticException is thrown.</p>
     *
     * <p>Example usage:</p>
     * <pre>{@code
     * // Convert a double to an int (throws ArithmeticException if outside int range)
     * Type<Integer> intType = Type.of(Integer.class);
     * Integer result = Numbers.convert(123.45, intType);
     *
     * // Convert a BigInteger to a long (throws ArithmeticException if too large)
     * Type<Long> longType = Type.of(Long.class);
     * Long longValue = Numbers.convert(new BigInteger("9223372036854775807"), longType);
     *
     * // Null values return the default value for the target type
     * Type<Byte> byteType = Type.of(Byte.class);
     * Byte byteValue = Numbers.convert(null, byteType); // Returns null
     * }</pre>
     *
     * @param <T> the target type of the conversion (must extend Number)
     * @param value the number to convert
     * @param targetType the Type object representing the target type
     * @return the converted number as an instance of the target type,
     *         or the default value of the target type if the input value is null
     * @throws ArithmeticException if the conversion would result in an overflow
     * @see #convert(Number, Class)
     * @see com.landawn.abacus.type.Type
     */
    public static <T extends Number> T convert(final Number value, final Type<? extends T> targetType) throws ArithmeticException {
        if (value == null) {
            return targetType.defaultValue();
        }

        final Map<Class<?>, UnaryOperator<Number>> temp = numberConverterFuncMap.get(targetType.clazz());
        final UnaryOperator<Number> func = temp == null ? null : temp.get(value.getClass());

        if (func != null) {
            return (T) func.apply(value);
        } else {
            return targetType.valueOf(N.stringOf(value));
        }
    }

    /**
     * Converts the given number to the specified target type using the provided Type instance, with a custom default value for null.
     *
     * <p>This method supports conversion between all primitive number types (byte, short, int, long, float, double),
     * as well as their corresponding wrapper classes. It also supports conversion to and from BigInteger and BigDecimal.
     * If the conversion would result in an overflow, an ArithmeticException is thrown.</p>
     *
     * <p>Example usage:</p>
     * <pre>{@code
     * // Convert a double to an int with a default value for null
     * Type<Integer> intType = Type.of(Integer.class);
     * Integer result = Numbers.convert(null, intType, -1);
     *
     * // Convert a BigInteger to a long with a custom default
     * Type<Long> longType = Type.of(Long.class);
     * Long longValue = Numbers.convert(null, longType, 0L);
     *
     * // Convert with overflow checking
     * try {
     *     Type<Byte> byteType = Type.of(Byte.class);
     *     Byte byteValue = Numbers.convert(new BigInteger("1000"), byteType, (byte)0);
     * } catch (ArithmeticException e) {
     *     // Handle overflow exception
     * }
     * }</pre>
     *
     * @param <T> the target type of the conversion (must extend Number)
     * @param value the number to convert
     * @param targetType the Type object representing the target type
     * @param defaultValueForNull the value to return if the input value is null
     * @return the converted number as an instance of the target type,
     *         or the specified default value if the input value is null
     * @throws ArithmeticException if the conversion would result in an overflow
     * @see #convert(Number, Type)
     * @see #convert(Number, Class, Number)
     * @see com.landawn.abacus.type.Type
     */
    public static <T extends Number> T convert(final Number value, final Type<? extends T> targetType, T defaultValueForNull) throws ArithmeticException {
        if (value == null) {
            return defaultValueForNull;
        }

        return convert(value, targetType);
    }

    /**
     * Formats the given int value according to the provided decimal format pattern.
     *
     * <p>This method uses {@link java.text.DecimalFormat} to format the integer value. The format should be a valid pattern
     * for DecimalFormat, such as "0.00" for two decimal places or "#,###" for thousand separators.</p>
     *
     * <p>Example usage:</p>
     * <pre>{@code
     * Numbers.format(1234, "#,###");      // returns "1,234"
     * Numbers.format(1234, "0.00");       // returns "1234.00"
     * Numbers.format(1234, "$#,###.00");  // returns "$1,234.00"
     * }</pre>
     *
     * @param x the int value to be formatted
     * @param decimalFormat the decimal format pattern to be used for formatting (must not be null)
     * @return a string representation of the int value formatted according to the provided decimal format
     * @throws IllegalArgumentException if the decimalFormat is {@code null}
     * @see #format(double, String)
     * @see #format(Integer, String)
     * @see java.text.DecimalFormat#format(long)
     */
    public static String format(final int x, final String decimalFormat) throws IllegalArgumentException {
        N.checkArgNotNull(decimalFormat, cs.decimalFormat);

        DecimalFormat df = decimalFormatPool.get(decimalFormat);

        if (df == null) {
            df = new DecimalFormat(decimalFormat);
        }

        return df.format(x);
    }

    /**
     * Formats the given Integer value according to the provided decimal format pattern.
     *
     * <p>This method uses {@link java.text.DecimalFormat} to format the Integer value. The format should be a valid pattern
     * for DecimalFormat, such as "0.00" for two decimal places or "#,###" for thousand separators.</p>
     *
     * <p>If the Integer value is {@code null}, it will be treated as 0.</p>
     *
     * <p>Example usage:</p>
     * <pre>{@code
     * Numbers.format(1234, "#,###");      // returns "1,234"
     * Numbers.format(1234, "0.00");       // returns "1234.00"
     * Numbers.format(null, "#,###");      // returns "0"
     * Numbers.format(1234, "$#,###.00");  // returns "$1,234.00"
     * }</pre>
     *
     * @param x the Integer value to be formatted (null will be treated as 0)
     * @param decimalFormat the decimal format pattern to be used for formatting (must not be null)
     * @return a string representation of the Integer value formatted according to the provided decimal format
     * @throws IllegalArgumentException if the decimalFormat is {@code null}
     * @see #format(int, String)
     * @see #format(Double, String)
     * @see java.text.DecimalFormat#format(long)
     */
    public static String format(final Integer x, final String decimalFormat) throws IllegalArgumentException {
        N.checkArgNotNull(decimalFormat, cs.decimalFormat);

        DecimalFormat df = decimalFormatPool.get(decimalFormat);

        if (df == null) {
            df = new DecimalFormat(decimalFormat);
        }

        if (x == null) {
            return df.format(0);
        } else {
            return df.format(x);
        }
    }

    /**
     * Formats the given long value according to the provided decimal format pattern.
     *
     * <p>This method uses {@link java.text.DecimalFormat} to format the long value. The format should be a valid pattern
     * for DecimalFormat, such as "0.00" for two decimal places or "#,###" for thousand separators.</p>
     *
     * <p>Example usage:</p>
     * <pre>{@code
     * Numbers.format(123456789L, "#,###");      // returns "123,456,789"
     * Numbers.format(123456789L, "0.00");       // returns "123456789.00"
     * Numbers.format(123456789L, "$#,###.00");  // returns "$123,456,789.00"
     * }</pre>
     *
     * @param x the long value to be formatted
     * @param decimalFormat the decimal format pattern to be used for formatting (must not be null)
     * @return a string representation of the long value formatted according to the provided decimal format
     * @throws IllegalArgumentException if the decimalFormat is {@code null}
     * @see #format(double, String)
     * @see #format(Long, String)
     * @see java.text.DecimalFormat#format(long)
     */
    public static String format(final long x, final String decimalFormat) throws IllegalArgumentException {
        N.checkArgNotNull(decimalFormat, cs.decimalFormat);

        DecimalFormat df = decimalFormatPool.get(decimalFormat);

        if (df == null) {
            df = new DecimalFormat(decimalFormat);
        }

        return df.format(x);
    }

    /**
     * Formats the given Long value according to the provided decimal format pattern.
     *
     * <p>This method uses {@link java.text.DecimalFormat} to format the Long value. The format should be a valid pattern
     * for DecimalFormat, such as "0.00" for two decimal places or "#,###" for thousand separators.</p>
     *
     * <p>If the Long value is {@code null}, it will be treated as 0.</p>
     *
     * <p>Example usage:</p>
     * <pre>{@code
     * Numbers.format(123456789L, "#,###");      // returns "123,456,789"
     * Numbers.format(123456789L, "0.00");       // returns "123456789.00"
     * Numbers.format(null, "#,###");            // returns "0"
     * Numbers.format(123456789L, "$#,###.00");  // returns "$123,456,789.00"
     * }</pre>
     *
     * @param x the Long value to be formatted (null will be treated as 0)
     * @param decimalFormat the decimal format pattern to be used for formatting (must not be null)
     * @return a string representation of the Long value formatted according to the provided decimal format
     * @throws IllegalArgumentException if the decimalFormat is {@code null}
     * @see #format(long, String)
     * @see #format(Double, String)
     * @see java.text.DecimalFormat#format(long)
     */
    public static String format(final Long x, final String decimalFormat) throws IllegalArgumentException {
        N.checkArgNotNull(decimalFormat, cs.decimalFormat);

        DecimalFormat df = decimalFormatPool.get(decimalFormat);

        if (df == null) {
            df = new DecimalFormat(decimalFormat);
        }

        if (x == null) {
            return df.format(0);
        } else {
            return df.format(x);
        }
    }

    /**
     * Formats the given float value according to the provided decimal format.
     *
     * <p>This method uses {@link java.text.DecimalFormat} to format the float value. The format should be a valid pattern
     * for DecimalFormat, such as "0.00" for two decimal places or "#.##" for up to two decimal places.
     *
     * <p>Example usage:
     * <pre>
     * <code>
     * Numbers.format(12.105f, "0.00"); // returns "12.10"
     * Numbers.format(12.105f, "#.##"); // returns "12.1"
     * Numbers.format(0.121, "#.##%"); // returns "12.1%"
     * Numbers.format(0.12156, "#.##%"); // returns "12.16%"
     * </code>
     * </pre>
     *
     * @param x The float value to be formatted.
     * @param decimalFormat The decimal format pattern to be used for formatting.
     * @return A string representation of the float value formatted according to the provided decimal format.
     * @throws IllegalArgumentException if the decimalFormat is {@code null}.
     * @see java.text.DecimalFormat#format(double)
     */
    public static String format(final float x, final String decimalFormat) throws IllegalArgumentException {
        N.checkArgNotNull(decimalFormat, cs.decimalFormat);

        DecimalFormat df = decimalFormatPool.get(decimalFormat);

        if (df == null) {
            df = new DecimalFormat(decimalFormat);
        }

        return df.format(x);
    }

    /**
     * Formats the given Float value according to the provided decimal format.
     *
     * <p>This method uses {@link java.text.DecimalFormat} to format the Float value. The format should be a valid pattern
     * for DecimalFormat, such as "0.00" for two decimal places or "#.##" for up to two decimal places.
     *
     * <p>If the Float value is {@code null}, it will be treated as 0f.
     *
     * <p>Example usage:
     * <pre>
     * <code>
     * Numbers.format(12.105f, "0.00"); // returns "12.10"
     * Numbers.format(12.105f, "#.##"); // returns "12.1"
     * Numbers.format(null, "0.00"); // returns "0.00"
     * Numbers.format(0.121, "#.##%"); // returns "12.1%"
     * Numbers.format(0.12156, "#.##%"); // returns "12.16%"
     * </code>
     * </pre>
     *
     * @param x The Float value to be formatted. If {@code null}, it will be treated as 0f.
     * @param decimalFormat The decimal format pattern to be used for formatting.
     * @return A string representation of the Float value formatted according to the provided decimal format.
     * @throws IllegalArgumentException if the decimalFormat is {@code null}.
     * @see java.text.DecimalFormat#format(double)
     */
    public static String format(final Float x, final String decimalFormat) throws IllegalArgumentException {
        N.checkArgNotNull(decimalFormat, cs.decimalFormat);

        DecimalFormat df = decimalFormatPool.get(decimalFormat);

        if (df == null) {
            df = new DecimalFormat(decimalFormat);
        }

        if (x == null) {
            return df.format(0f);
        } else {
            return df.format(x.doubleValue());
        }
    }

    /**
     * Formats the given double value according to the provided decimal format.
     *
     * <p>This method uses {@link java.text.DecimalFormat} to format the double value. The format should be a valid pattern
     * for DecimalFormat, such as "0.00" for two decimal places or "#.##" for up to two decimal places.
     *
     * <p>Example usage:
     * <pre>
     * <code>
     * Numbers.format(12.105, "0.00"); // returns "12.11"
     * Numbers.format(12.105, "#.##"); // returns "12.11"
     * Numbers.format(0.121, "0.00%"); // returns "12.10%"
     * Numbers.format(0.121, "#.##%"); // returns "12.1%"
     * Numbers.format(0.12156, "0.00%"); // returns "12.16%"
     * Numbers.format(0.12156, "#.##%"); // returns "12.16%"
     * </code>
     * </pre>
     *
     * @param x The double value to be formatted.
     * @param decimalFormat The decimal format pattern to be used for formatting.
     * @return A string representation of the double value formatted according to the provided decimal format.
     * @throws IllegalArgumentException if the decimalFormat is {@code null}.
     * @see java.text.DecimalFormat#format(double)
     */
    public static String format(final double x, final String decimalFormat) throws IllegalArgumentException {
        N.checkArgNotNull(decimalFormat, cs.decimalFormat);

        DecimalFormat df = decimalFormatPool.get(decimalFormat);

        if (df == null) {
            df = new DecimalFormat(decimalFormat);
        }

        return df.format(x);
    }

    /**
     * Formats the given Double value according to the provided decimal format.
     *
     * <p>This method uses {@link java.text.DecimalFormat} to format the Double value. The format should be a valid pattern
     * for DecimalFormat, such as "0.00" for two decimal places or "#.##" for up to two decimal places.
     *
     * <p>If the Double value is {@code null}, it will be treated as 0d.
     *
     * <p>Example usage:
     * <pre>
     * <code>
     * Numbers.format(12.105, "0.00"); // returns "12.11"
     * Numbers.format(12.105, "#.##"); // returns "12.11"
     * Numbers.format(null, "0.00"); // returns "0.00"
     * Numbers.format(0.121, "#.##%"); // returns "12.1%"
     * Numbers.format(0.12156, "#.##%"); // returns "12.16%"
     * </code>
     * </pre>
     *
     * @param x The Double value to be formatted. If {@code null}, it will be treated as 0d.
     * @param decimalFormat The decimal format pattern to be used for formatting.
     * @return A string representation of the Double value formatted according to the provided decimal format.
     * @throws IllegalArgumentException if the decimalFormat is {@code null}.
     * @see java.text.DecimalFormat#format(double)
     */
    public static String format(final Double x, final String decimalFormat) throws IllegalArgumentException {
        N.checkArgNotNull(decimalFormat, cs.decimalFormat);

        DecimalFormat df = decimalFormatPool.get(decimalFormat);

        if (df == null) {
            df = new DecimalFormat(decimalFormat);
        }

        if (x == null) {
            return df.format(0d);
        } else {
            return df.format(x);
        }
    }

    /**
     * Extracts the first integer value from the given string. If no integer value is found, it returns 0. 
     *
     * @param str The string to extract the integer value from.
     * @return The extracted integer value, or 0 if no integer value is found.
     * @see #extractFirstInt(String, int)
     * @see #extractFirstLong(String)
     * @see #extractFirstDouble(String)
     * @see #extractFirstDouble(String, boolean)
     * @see Strings#extractFirstInteger(String)
     */
    public static int extractFirstInt(final String str) {
        return extractFirstInt(str, 0);
    }

    /**
     * Extracts the first integer value from the given string. If no integer value is found, it returns the specified default value.
     *
     * @param str The string to extract the integer value from.
     * @param defaultValue The default value to return if no integer value is found.
     * @return The extracted integer value, or the specified default value if no integer value is found.
     * @see #extractFirstInt(String)
     * @see #extractFirstLong(String)
     * @see Strings#extractFirstInteger(String)
     */
    public static int extractFirstInt(final String str, final int defaultValue) {
        if (Strings.isEmpty(str)) {
            return defaultValue;
        }

        final Matcher matcher = RegExUtil.INTEGER_FINDER.matcher(str);
        if (matcher.find()) {
            return Integer.parseInt(matcher.group(1));
        }

        return defaultValue;
    }

    /**
     * Extracts the first long value from the given string. If no long value is found, it returns 0L.
     *
     * @param str The string to extract the long value from.
     * @return The extracted long value, or 0L if no long value is found.
     * @see #extractFirstLong(String, long) 
     * @see Strings#extractFirstInteger(String) 
     */
    public static long extractFirstLong(final String str) {
        return extractFirstLong(str, 0L);
    }

    /**
     * Extracts the first long value from the given string. If no long value is found, it returns the specified default value.
     *
     * @param str The string to extract the long value from.
     * @param defaultValue The default value to return if no long value is found.
     * @return The extracted long value, or the specified default value if no long value is found.
     * @see #extractFirstLong(String)
     * @see Strings#extractFirstInteger(String)
     */
    public static long extractFirstLong(final String str, final long defaultValue) {
        if (Strings.isEmpty(str)) {
            return defaultValue;
        }

        final Matcher matcher = RegExUtil.INTEGER_FINDER.matcher(str);
        if (matcher.find()) {
            return Long.parseLong(matcher.group(1));
        }

        return defaultValue;
    }

    /**
     * Extracts the first double value from the given string. If no double value is found, it returns 0.0.
     *
     * @param str The string to extract the double value from.
     * @return The extracted double value, or 0.0 if no double value is found.
     * @see #extractFirstDouble(String, double)
     * @see #extractFirstDouble(String, double, boolean) 
     * @see Strings#extractFirstDouble(String)
     * @see Strings#extractFirstDouble(String, boolean)
     * @see Strings#replaceFirstDouble(String, String)
     * @see Strings#replaceFirstDouble(String, String, boolean)
     */
    public static double extractFirstDouble(final String str) {
        return extractFirstDouble(str, 0.0, false);
    }

    /**
     * Extracts the first double value from the given string. If no double value is found, it returns 0.0.
     *
     * <p>This method is a convenience method that allows you to specify whether to include scientific notation in the search.
     * If {@code includingCientificNumber} is {@code true}, it will search for both regular and scientific notation numbers.
     * Otherwise, it will only search for regular numbers.</p>
     *
     * @param str The string to extract the double value from.
     * @param includingCientificNumber Whether to include scientific notation in the search.
     * @return The extracted double value, or 0.0 if no double value is found.
     * @see #extractFirstDouble(String, double)
     * @see #extractFirstDouble(String, double, boolean) 
     * @see Strings#extractFirstDouble(String)
     * @see Strings#extractFirstDouble(String, boolean)
     * @see Strings#replaceFirstDouble(String, String)
     * @see Strings#replaceFirstDouble(String, String, boolean)
     */
    public static double extractFirstDouble(final String str, final boolean includingCientificNumber) {
        return extractFirstDouble(str, 0.0, includingCientificNumber);
    }

    /**
     * Extracts the first double value from the given string. If no double value is found, it returns the specified default value.
     *
     * @param str The string to extract the double value from.
     * @param defaultValue The default value to return if no double value is found.
     * @return The extracted double value, or the specified default value if no double value is found. 
     * @see #extractFirstDouble(String, double, boolean)
     * @see Strings#extractFirstDouble(String)
     * @see Strings#extractFirstDouble(String, boolean)
     * @see Strings#replaceFirstDouble(String, String)
     * @see Strings#replaceFirstDouble(String, String, boolean)
     */
    public static double extractFirstDouble(final String str, final double defaultValue) {
        return extractFirstDouble(str, defaultValue, false);
    }

    /**
     * Extracts the first double value from the given string. If no double value is found, it returns the specified default value.
     *
     * <p>This method allows you to specify whether to include scientific notation in the search for double values.
     * If {@code includingCientificNumber} is {@code true}, it will search for both regular and scientific notation numbers.
     * Otherwise, it will only search for regular numbers.</p>
     *
     * @param str The string to extract the double value from.
     * @param defaultValue The default value to return if no double value is found.
     * @param includingCientificNumber Whether to include scientific notation in the search.
     * @return The extracted double value, or the specified default value if no double value is found.
     * @see Strings#extractFirstDouble(String)
     * @see Strings#extractFirstDouble(String, boolean)
     * @see Strings#replaceFirstDouble(String, String)
     * @see Strings#replaceFirstDouble(String, String, boolean)
     */
    public static double extractFirstDouble(final String str, final double defaultValue, final boolean includingCientificNumber) {
        if (Strings.isEmpty(str)) {
            return defaultValue;
        }

        final Matcher matcher = (includingCientificNumber ? RegExUtil.SCIENTIFIC_NUMBER_FINDER : RegExUtil.NUMBER_FINDER).matcher(str);

        if (matcher.find()) {
            return Double.parseDouble(matcher.group(1));
        }

        return defaultValue;
    }

    /**
     * Converts the given string to a byte value.
     *
     * <p>This method attempts to convert the provided string to a byte. If the string is {@code null} or empty,
     * default value {@code 0} is returned. Otherwise, the method attempts to parse the string as a byte.</p>
     *
     * @param str The string to convert. This can be any instance of String.
     * @return The byte representation of the provided string, or {@code 0} if the object is {@code null} or empty.
     * @throws NumberFormatException If the string cannot be parsed as a byte.
     * @see #toByte(String, byte)
     * @see #isParsable(String)
     * @see Byte#parseByte(String)
     * @see Byte#decode(String)
     */
    public static byte toByte(final String str) throws NumberFormatException {
        return toByte(str, (byte) 0);
    }

    /**
     * Converts the given object to a byte value.
     *
     * <p>This method attempts to convert the provided object to a byte. If the object is {@code null},
     * default value {@code 0} is returned. If the object is a Number, its byte value is returned.
     * Otherwise, the method attempts to parse the object's string representation as a byte.</p>
     *
     * @param obj The object to convert. This can be any instance of Object.
     * @return The byte representation of the provided object, or {@code 0} if the object is {@code null}.
     * @throws NumberFormatException If the object is not a Number and its string representation cannot be parsed as a byte.
     * @see #toByte(Object, byte)
     * @see #isParsable(String)
     * @see Byte#parseByte(String)
     * @see Byte#decode(String)
     */
    public static byte toByte(final Object obj) throws NumberFormatException {
        return toByte(obj, (byte) 0);
    }

    /**
     * Converts the given string to a byte value.
     *
     * <p>This method attempts to convert the provided string to a byte. If the string is {@code null} or empty,
     * the provided default value is returned. Otherwise, the method attempts to parse the string as a byte.</p>
     *
     * @param str The string to convert. This can be any instance of String.
     * @param defaultValueForNull The default value to return if the string is {@code null} or empty.
     * @return The byte representation of the provided string, or the default value if the string is {@code null} or empty.
     * @throws NumberFormatException If the string cannot be parsed as a byte.
     * @see #isParsable(String)
     * @see Byte#parseByte(String)
     * @see Byte#decode(String)
     */
    public static byte toByte(final String str, final byte defaultValueForNull) throws NumberFormatException {
        if (Strings.isEmpty(str)) {
            return defaultValueForNull;
        }

        if (str.length() < 5) {
            final Integer result = N.stringIntCache.get(str);

            if (result != null) {
                if (result < Byte.MIN_VALUE || result > Byte.MAX_VALUE) {
                    throw new NumberFormatException("Value out of range. Value:\"" + str + "\" Radix: 10");
                }

                return result.byteValue();
            }
        }

        return Byte.parseByte(str);
    }

    /**
     * Converts the given object to a byte value.
     *
     * <p>This method attempts to convert the provided object to a byte. If the object is {@code null},
     * the provided default value is returned. If the object is a Number, its byte value is returned.
     * Otherwise, the method attempts to parse the object's string representation as a byte.</p>
     *
     * @param obj The object to convert. This can be any instance of Object.
     * @param defaultValueForNull The default value to return if the object is {@code null}.
     * @return The byte representation of the provided object, or the default value if the object is {@code null}.
     * @throws NumberFormatException If the object is not a Number and its string representation cannot be parsed as a byte.
     * @see #isParsable(String)
     * @see Byte#parseByte(String)
     * @see Byte#decode(String)
     */
    public static byte toByte(final Object obj, final byte defaultValueForNull) throws NumberFormatException {
        if (obj == null) {
            return defaultValueForNull;
        }

        if (obj instanceof Byte) {
            return ((Byte) obj);
        } else if (obj instanceof Number) {
            final long lng = ((Number) obj).longValue();

            if (lng > Byte.MAX_VALUE || lng < Byte.MIN_VALUE) {
                throw new NumberFormatException("Value out of range. Value:\"" + obj + "\"");
            }

            return (byte) lng;
        }

        return toByte(obj.toString(), defaultValueForNull);
    }

    /**
     * Converts the given string to a short value.
     *
     * <p>This method attempts to convert the provided string to a short. If the string is {@code null} or empty,
     * default value {@code 0} is returned. Otherwise, the method attempts to parse the string as a short.</p>
     *
     * @param str The string to convert. This can be any instance of String.
     * @return The short representation of the provided string, or {@code 0} if the object is {@code null} or empty.
     * @throws NumberFormatException If the string cannot be parsed as a short.
     * @see #isParsable(String)
     * @see Short#parseShort(String)
     * @see Short#decode(String)
     */
    public static short toShort(final String str) throws NumberFormatException {
        return toShort(str, (short) 0);
    }

    /**
     * Converts the given object to a short value.
     *
     * <p>This method attempts to convert the provided object to a short. If the object is {@code null},
     * default value {@code 0} is returned. If the object is a Number, its short value is returned.
     * Otherwise, the method attempts to parse the object's string representation as a short.</p>
     *
     * @param obj The object to convert. This can be any instance of Object.
     * @return The short representation of the provided object, or {@code 0} if the object is {@code null}.
     * @throws NumberFormatException If the object is not a Number and its string representation cannot be parsed as a short.
     * @see #isParsable(String)
     * @see Short#parseShort(String)
     * @see Short#decode(String)
     */
    public static short toShort(final Object obj) throws NumberFormatException {
        return toShort(obj, (short) 0);
    }

    /**
     * Converts the given string to a short value.
     *
     * <p>This method attempts to convert the provided string to a short. If the string is {@code null} or empty,
     * the provided default value is returned. Otherwise, the method attempts to parse the string as a short.</p>
     *
     * @param str The string to convert. This can be any instance of String.
     * @param defaultValueForNull The default value to return if the string is {@code null} or empty.
     * @return The short representation of the provided string, or the default value if the string is {@code null} or empty.
     * @throws NumberFormatException If the string cannot be parsed as a short.
     * @see #isParsable(String)
     * @see Short#parseShort(String)
     * @see Short#decode(String)
     */
    public static short toShort(final String str, final short defaultValueForNull) throws NumberFormatException {
        if (Strings.isEmpty(str)) {
            return defaultValueForNull;
        }

        if (str.length() < 5) {
            final Integer result = N.stringIntCache.get(str);

            if (result != null) {
                if (result < Short.MIN_VALUE || result > Short.MAX_VALUE) {
                    throw new NumberFormatException("Value out of range. Value:\"" + str + "\" Radix: 10");
                }

                return result.shortValue();
            }
        }

        return Short.parseShort(str);
    }

    /**
     * Converts the given object to a short value.
     *
     * <p>This method attempts to convert the provided object to a short. If the object is {@code null},
     * the provided default value is returned. If the object is a Number, its short value is returned.
     * Otherwise, the method attempts to parse the object's string representation as a short.</p>
     *
     * @param obj The object to convert. This can be any instance of Object.
     * @param defaultValueForNull The default value to return if the object is {@code null}.
     * @return The short representation of the provided object, or the default value if the object is {@code null}.
     * @throws NumberFormatException If the object is not a Number and its string representation cannot be parsed as a short.
     * @see #isParsable(String)
     * @see Short#parseShort(String)
     * @see Short#decode(String)
     */
    public static short toShort(final Object obj, final short defaultValueForNull) throws NumberFormatException {
        if (obj == null) {
            return defaultValueForNull;
        }

        if (obj instanceof Short) {
            return ((Short) obj);
        } else if (obj instanceof Number) {
            final long lng = ((Number) obj).longValue();

            if (lng > Short.MAX_VALUE || lng < Short.MIN_VALUE) {
                throw new NumberFormatException("Value out of range. Value:\"" + obj + "\"");
            }

            return (short) lng;
        }

        return toShort(obj.toString(), defaultValueForNull);
    }

    /**
     * Converts the given string to an integer value.
     *
     * <p>This method attempts to convert the provided string to an integer. If the string is {@code null} or empty,
     * default value {@code 0} is returned. Otherwise, the method attempts to parse the string as an integer.</p>
     *
     * @param str The string to convert. This can be any instance of String.
     * @return The integer representation of the provided string, or {@code 0} if the object is {@code null} or empty.
     * @throws NumberFormatException If the string cannot be parsed as an integer.
     * @see #toInt(String, int)
     * @see #isParsable(String)
     * @see Integer#parseInt(String)
     * @see Integer#decode(String)
     */
    public static int toInt(final String str) throws NumberFormatException {
        return toInt(str, 0);
    }

    /**
     * Converts the given object to an integer value.
     *
     * <p>This method attempts to convert the provided object to an integer. If the object is {@code null},
     * default value {@code 0} is returned. If the object is a Number, its integer value is returned.
     * Otherwise, the method attempts to parse the object's string representation as an integer.</p>
     *
     * @param obj The object to convert. This can be any instance of Object.
     * @return The integer representation of the provided object, or {@code 0} if the object is {@code null}.
     * @throws NumberFormatException If the object is not a Number and its string representation cannot be parsed as an integer.
     * @see #toInt(Object, int)
     * @see #isParsable(String)
     * @see Integer#parseInt(String)
     * @see Integer#decode(String)
     */
    public static int toInt(final Object obj) throws NumberFormatException {
        return toInt(obj, 0);
    }

    /**
     * Converts the given string to an integer value.
     *
     * <p>This method attempts to convert the provided string to an integer. If the string is {@code null} or empty,
     * the provided default value is returned. Otherwise, the method attempts to parse the string as an integer.</p>
     *
     * @param str The string to convert. This can be any instance of String.
     * @param defaultValueForNull The default value to return if the string is {@code null} or empty.
     * @return The integer representation of the provided string, or the default value if the string is {@code null} or empty.
     * @throws NumberFormatException If the string cannot be parsed as an integer.
     * @see #isParsable(String)
     * @see Integer#parseInt(String)
     * @see Integer#decode(String)
     */
    public static int toInt(final String str, final int defaultValueForNull) throws NumberFormatException {
        if (Strings.isEmpty(str)) {
            return defaultValueForNull;
        }

        if (str.length() < 5) {
            final Integer result = N.stringIntCache.get(str);

            if (result != null) {
                return result;
            }
        }

        return Integer.parseInt(str);
    }

    /**
     * Converts the given object to an integer value.
     *
     * <p>This method attempts to convert the provided object to an integer. If the object is {@code null},
     * the provided default value is returned. If the object is a Number, its integer value is returned.
     * Otherwise, the method attempts to parse the object's string representation as an integer.</p>
     *
     * @param obj The object to convert. This can be any instance of Object.
     * @param defaultValueForNull The default value to return if the object is {@code null}.
     * @return The integer representation of the provided object, or the default value if the object is {@code null}.
     * @throws NumberFormatException If the object is not a Number and its string representation cannot be parsed as an integer.
     * @see #isParsable(String)
     * @see Integer#parseInt(String)
     * @see Integer#decode(String)
     */
    public static int toInt(final Object obj, final int defaultValueForNull) throws NumberFormatException {
        if (obj == null) {
            return defaultValueForNull;
        }

        if (obj instanceof Integer) {
            return ((Integer) obj);
        } else if (obj instanceof Number) {
            final long lng = ((Number) obj).longValue();

            if (lng > Integer.MAX_VALUE || lng < Integer.MIN_VALUE) {
                throw new NumberFormatException("Value out of range. Value:\"" + obj + "\"");
            }

            return (int) lng;
        }

        return toInt(obj.toString(), defaultValueForNull);
    }

    /**
     * Converts the given string to a long value.
     *
     * <p>This method attempts to convert the provided string to a long. If the string is {@code null} or empty,
     * default value {@code 0} is returned. Otherwise, the method attempts to parse the string as a long.</p>
     *
     * @param str The string to convert. This can be any instance of String.
     * @return The long representation of the provided string, or {@code 0} if the object is {@code null} or empty.
     * @throws NumberFormatException If the string cannot be parsed as a long.
     * @see #toLong(String, long)
     * @see #isParsable(String)
     * @see Long#parseLong(String)
     * @see Long#decode(String)
     */
    public static long toLong(final String str) throws NumberFormatException {
        return toLong(str, 0L);
    }

    /**
     * Converts the given object to a long value.
     *
     * <p>This method attempts to convert the provided object to a long. If the object is {@code null},
     * default value {@code 0} is returned. If the object is a Number, its long value is returned.
     * Otherwise, the method attempts to parse the object's string representation as a long.</p>
     *
     * @param obj The object to convert. This can be any instance of Object.
     * @return The long representation of the provided object, or {@code 0} if the object is {@code null}.
     * @throws NumberFormatException If the object is not a Number and its string representation cannot be parsed as a long.
     * @see #toLong(Object, long)
     * @see #isParsable(String)
     * @see Long#parseLong(String)
     * @see Long#decode(String)
     */
    public static long toLong(final Object obj) throws NumberFormatException {
        return toLong(obj, 0);
    }

    /**
     * Converts the given string to a long value.
     *
     * <p>This method attempts to convert the provided string to a long. If the string is {@code null} or empty,
     * the provided default value is returned. Otherwise, the method attempts to parse the string as a long.</p>
     *
     * @param str The string to convert. This can be any instance of String.
     * @param defaultValueForNull The default value to return if the string is {@code null} or empty.
     * @return The long representation of the provided string, or the default value if the string is {@code null} or empty.
     * @throws NumberFormatException If the string cannot be parsed as a long.
     * @see #isParsable(String)
     * @see Long#parseLong(String)
     * @see Long#decode(String)
     */
    public static long toLong(final String str, final long defaultValueForNull) throws NumberFormatException {
        if (Strings.isEmpty(str)) {
            return defaultValueForNull;
        }

        if (str.length() < 5) {
            final Integer result = N.stringIntCache.get(str);
            if (result != null) {
                return result;
            }
        }

        final int len = str.length();
        final char ch = str.charAt(len - 1);

        if (len > 1 && (ch == 'L' || ch == 'l')) {
            try {
                return Long.parseLong(str.substring(0, len - 1));
            } catch (final NumberFormatException e) {
                throw new NumberFormatException("Cannot parse string: " + str + ". " + e.getMessage());
            }
        }

        return Long.parseLong(str);
    }

    /**
     * Converts the given object to a long value.
     *
     * <p>This method attempts to convert the provided object to a long. If the object is {@code null},
     * the provided default value is returned. If the object is a Number, its long value is returned.
     * Otherwise, the method attempts to parse the object's string representation as a long.</p>
     *
     * @param obj The object to convert. This can be any instance of Object.
     * @param defaultValueForNull The default value to return if the object is {@code null}.
     * @return The long representation of the provided object, or the default value if the object is {@code null}.
     * @throws NumberFormatException If the object is not a Number and its string representation cannot be parsed as a long.
     * @see #isParsable(String)
     * @see Long#parseLong(String)
     * @see Long#decode(String)
     */
    public static long toLong(final Object obj, final long defaultValueForNull) throws NumberFormatException {
        if (obj == null) {
            return defaultValueForNull;
        }

        if (obj instanceof Long) {
            return ((Long) obj);
        }

        if (obj instanceof final BigInteger bigInt) {
            if (bigInt.compareTo(BIG_INTEGER_WITH_MIN_LONG_VALUE) < 0 || bigInt.compareTo(BIG_INTEGER_WITH_MAX_LONG_VALUE) > 0) {
                throw new NumberFormatException("Value out of range. Value:\"" + obj + "\"");
            }

            return bigInt.longValue();
        } else if (obj instanceof final BigDecimal bigDecimal) {
            if (bigDecimal.compareTo(BIG_DECIMAL_WITH_MIN_LONG_VALUE) < 0 || bigDecimal.compareTo(BIG_DECIMAL_WITH_MAX_LONG_VALUE) > 0) {
                throw new NumberFormatException("Value out of range. Value:\"" + obj + "\"");
            }

            return bigDecimal.longValue();
        }

        if (obj instanceof Number) {
            return ((Number) obj).longValue();
        }

        return toLong(obj.toString(), defaultValueForNull);
    }

    /**
     * Converts the given string to a float value.
     *
     * <p>This method attempts to convert the provided string to a float. If the string is {@code null},
     * default value {@code 0} is returned. Otherwise, the method attempts to parse the string as a float.</p>
     *
     * @param str The string to convert. This can be any instance of String.
     * @return The float representation of the provided string, or {@code 0} if the object is {@code null} or empty.
     * @throws NumberFormatException If the string cannot be parsed as a float.
     * @see #toFloat(String, float)
     * @see #isParsable(String)
     * @see Float#parseFloat(String)
     */
    public static float toFloat(final String str) throws NumberFormatException {
        return toFloat(str, 0.0f);
    }

    /**
     * Converts the given object to a float value.
     *
     * <p>This method attempts to convert the provided object to a float. If the object is {@code null},
     * default value {@code 0} is returned. If the object is a Number, its float value is returned.
     * Otherwise, the method attempts to parse the object's string representation as a float.</p>
     *
     * @param obj The object to convert. This can be any instance of Object.
     * @return The float representation of the provided object, or {@code 0} if the object is {@code null}.
     * @throws NumberFormatException If the object is not a Number and its string representation cannot be parsed as a float.
     * @see #toFloat(Object, float)
     * @see #isParsable(String)
     * @see Float#parseFloat(String)
     */
    public static float toFloat(final Object obj) throws NumberFormatException {
        return toFloat(obj, 0);
    }

    /**
     * Converts the given string to a float value.
     *
     * <p>This method attempts to convert the provided string to a float. If the string is {@code null} or empty,
     * the provided default value is returned. Otherwise, the method attempts to parse the string as a float.</p>
     *
     * @param str The string to convert. This can be any instance of String.
     * @param defaultValueForNull The default value to return if the provided string is {@code null} or empty.
     * @return The float representation of the provided string, or the default value if the string is {@code null} or empty.
     * @throws NumberFormatException If the string cannot be parsed as a float.
     * @see #isParsable(String)
     * @see Float#parseFloat(String)
     */
    public static float toFloat(final String str, final float defaultValueForNull) throws NumberFormatException {
        if (Strings.isEmpty(str)) {
            return defaultValueForNull;
        }

        return Float.parseFloat(str);
    }

    /**
     * Converts the given object to a float value.
     *
     * <p>This method attempts to convert the provided object to a float. If the object is {@code null},
     * the provided default value is returned. If the object is a Number, its float value is returned.
     * Otherwise, the method attempts to parse the object's string representation as a float.</p>
     *
     * @param obj The object to convert. This can be any instance of Object.
     * @param defaultValueForNull The default value to return if the provided object is {@code null}.
     * @return The float representation of the provided object, or the default value if the object is {@code null}.
     * @throws NumberFormatException If the object is not a Number and its string representation cannot be parsed as a float.
     * @see #isParsable(String)
     * @see Float#parseFloat(String)
     */
    public static float toFloat(final Object obj, final float defaultValueForNull) throws NumberFormatException {
        if (obj == null) {
            return defaultValueForNull;
        }

        if (obj instanceof Number) {
            if (obj instanceof Double) {
                return Float.parseFloat(obj.toString());
            } else {
                return ((Number) obj).floatValue();
            }
        }

        return toFloat(obj.toString(), defaultValueForNull);
    }

    /**
     * Converts the given string to a double value.
     *
     * <p>This method attempts to convert the provided string to a double. If the string is {@code null} or empty,
     * default value {@code 0} is returned. Otherwise, the method attempts to parse the string as a double.</p>
     *
     * @param str The string to convert. This can be any instance of String.
     * @return The double representation of the provided string, or {@code 0} if the object is {@code null} or empty.
     * @throws NumberFormatException If the string cannot be parsed as a double.
     * @see #toDouble(String, double)
     * @see #isParsable(String)
     * @see Double#parseDouble(String)
     */
    public static double toDouble(final String str) throws NumberFormatException {
        return toDouble(str, 0.0d);
    }

    /**
     * Converts the given object to a double value.
     *
     * <p>This method attempts to convert the provided object to a double. If the object is {@code null},
     * default value {@code 0} is returned. If the object is a Number, its double value is returned.
     * Otherwise, the method attempts to parse the object's string representation as a double.</p>
     *
     * @param obj The object to convert. This can be any instance of Object.
     * @return The double representatio of the provided object, or {@code 0} if the object is {@code null}.
     * @throws NumberFormatException If the object is not a Number and its string representation cannot be parsed as a double.
     * @see #toDouble(Object, double)
     * @see #isParsable(String)
     * @see Double#parseDouble(String)
     */
    public static double toDouble(final Object obj) throws NumberFormatException {
        return toDouble(obj, 0);
    }

    /**
     * Converts the given string to a double value.
     *
     * <p>This method attempts to convert the provided string to a double. If the string is {@code null} or empty,
     * the provided default value is returned. Otherwise, the method attempts to parse the string as a double.</p>
     *
     * @param str The string to convert. This can be any instance of String.
     * @param defaultValueForNull The default value to return if the provided string is {@code null} or empty.
     * @return The double representation of the provided string, or the default value if the string is {@code null} or empty.
     * @throws NumberFormatException If the string cannot be parsed as a double.
     * @see #isParsable(String)
     * @see Double#parseDouble(String)
     */
    public static double toDouble(final String str, final double defaultValueForNull) throws NumberFormatException {
        if (Strings.isEmpty(str)) {
            return defaultValueForNull;
        }

        return Double.parseDouble(str);
    }

    /**
     * Converts the given object to a double value.
     *
     * <p>This method attempts to convert the provided object to a double. If the object is {@code null},
     * the provided default value is returned. If the object is a Number, its double value is returned.
     * Otherwise, the method attempts to parse the object's string representation as a double.</p>
     *
     * @param obj The object to convert. This can be any instance of Object.
     * @param defaultValueForNull The default value to return if the provided object is {@code null}.
     * @return The double representation of the provided object, or the default value if the object is {@code null}.
     * @throws NumberFormatException If the object is not a Number and its string representation cannot be parsed as a double.
     * @see #isParsable(String)
     * @see Double#parseDouble(String)
     */
    public static double toDouble(final Object obj, final double defaultValueForNull) throws NumberFormatException {
        if (obj == null) {
            return defaultValueForNull;
        }

        if (obj instanceof Number) {
            if (obj instanceof Float) {
                return Double.parseDouble(obj.toString());
            } else {
                return ((Number) obj).doubleValue();
            }
        }

        return toDouble(obj.toString(), defaultValueForNull);
    }

    /**
     * <p>Convert a {@code BigDecimal} to a {@code double}.</p>
     *
     * <p>If the {@code BigDecimal} {@code value} is
     * {@code null}, then the specified default value is returned.</p>
     *
     * <pre>
     *   NumberUtils.toDouble(null)                     = 0.0d
     *   NumberUtils.toDouble(BigDecimal.valueOf(8.5d)) = 8.5d
     * </pre>
     *
     * @param value the {@code BigDecimal} to convert, may be {@code null}.
     * @return the double represented by the {@code BigDecimal} or
     *  {@code 0.0d} if the {@code BigDecimal} is {@code null}.
     */
    public static double toDouble(final BigDecimal value) {
        return toDouble(value, 0.0d);
    }

    /**
     * <p>Convert a {@code BigDecimal} to a {@code double}.</p>
     *
     * <p>If the {@code BigDecimal} {@code value} is
     * {@code null}, then the specified default value is returned.</p>
     *
     * <pre>
     *   NumberUtils.toDouble(null, 1.1d)                     = 1.1d
     *   NumberUtils.toDouble(BigDecimal.valueOf(8.5d), 1.1d) = 8.5d
     * </pre>
     *
     * @param value the {@code BigDecimal} to convert, may be {@code null}.
     * @param defaultValueForNull
     * @return the double represented by the {@code BigDecimal} or the defaultValue if the {@code BigDecimal} is {@code null}.
     */
    public static double toDouble(final BigDecimal value, final double defaultValueForNull) {
        return value == null ? defaultValueForNull : value.doubleValue();
    }

    /**
     * Convert a {@code BigDecimal} to a {@code BigDecimal} with a scale of
     * two that has been rounded using {@code RoundingMode.HALF_EVEN}. If the supplied
     * {@code value} is {@code null}, then {@code BigDecimal.ZERO} is returned.
     *
     * <p>Note, the scale of a {@code BigDecimal} is the number of digits to the right of the decimal point.</p>
     *
     * @param value the {@code BigDecimal} to convert, may be {@code null}.
     * @return the scaled, with appropriate rounding, {@code BigDecimal}.
     */
    public static BigDecimal toScaledBigDecimal(final BigDecimal value) {
        return toScaledBigDecimal(value, INTEGER_TWO, RoundingMode.HALF_EVEN);
    }

    /**
     * Convert a {@code BigDecimal} to a {@code BigDecimal} whose scale is the
     * specified value with a {@code RoundingMode} applied. If the input {@code value}
     * is {@code null}, we simply return {@code BigDecimal.ZERO}.
     *
     * @param value the {@code BigDecimal} to convert, may be {@code null}.
     * @param scale the number of digits to the right of the decimal point.
     * @param roundingMode a rounding behavior for numerical operations capable of discarding precision.
     * @return the scaled, with appropriate rounding, {@code BigDecimal}.
     */
    public static BigDecimal toScaledBigDecimal(final BigDecimal value, final int scale, final RoundingMode roundingMode) {
        if (value == null) {
            return BigDecimal.ZERO;
        }
        return value.setScale(scale, (roundingMode == null) ? RoundingMode.HALF_EVEN : roundingMode);
    }

    /**
     * Convert a {@code Float} to a {@code BigDecimal} with a scale of
     * two that has been rounded using {@code RoundingMode.HALF_EVEN}. If the supplied
     * {@code value} is {@code null}, then {@code BigDecimal.ZERO} is returned.
     *
     * <p>Note, the scale of a {@code BigDecimal} is the number of digits to the right of the decimal point.</p>
     *
     * @param value the {@code Float} to convert, may be {@code null}.
     * @return the scaled, with appropriate rounding, {@code BigDecimal}.
     */
    public static BigDecimal toScaledBigDecimal(final Float value) {
        return toScaledBigDecimal(value, INTEGER_TWO, RoundingMode.HALF_EVEN);
    }

    /**
     * Convert a {@code Float} to a {@code BigDecimal} whose scale is the
     * specified value with a {@code RoundingMode} applied. If the input {@code value}
     * is {@code null}, we simply return {@code BigDecimal.ZERO}.
     *
     * @param value the {@code Float} to convert, may be {@code null}.
     * @param scale the number of digits to the right of the decimal point.
     * @param roundingMode a rounding behavior for numerical operations capable of discarding precision.
     * @return the scaled, with appropriate rounding, {@code BigDecimal}.
     */
    public static BigDecimal toScaledBigDecimal(final Float value, final int scale, final RoundingMode roundingMode) {
        if (value == null) {
            return BigDecimal.ZERO;
        }
        return toScaledBigDecimal(BigDecimal.valueOf(value), scale, roundingMode);
    }

    /**
     * Convert a {@code Double} to a {@code BigDecimal} with a scale of
     * two that has been rounded using {@code RoundingMode.HALF_EVEN}. If the supplied
     * {@code value} is {@code null}, then {@code BigDecimal.ZERO} is returned.
     *
     * <p>Note, the scale of a {@code BigDecimal} is the number of digits to the right of the decimal point.</p>
     *
     * @param value the {@code Double} to convert, may be {@code null}.
     * @return the scaled, with appropriate rounding, {@code BigDecimal}.
     */
    public static BigDecimal toScaledBigDecimal(final Double value) {
        return toScaledBigDecimal(value, INTEGER_TWO, RoundingMode.HALF_EVEN);
    }

    /**
     * Convert a {@code Double} to a {@code BigDecimal} whose scale is the
     * specified value with a {@code RoundingMode} applied. If the input {@code value}
     * is {@code null}, we simply return {@code BigDecimal.ZERO}.
     *
     * @param value the {@code Double} to convert, may be {@code null}.
     * @param scale the number of digits to the right of the decimal point.
     * @param roundingMode a rounding behavior for numerical operations capable of discarding precision.
     * @return the scaled, with appropriate rounding, {@code BigDecimal}.
     */
    public static BigDecimal toScaledBigDecimal(final Double value, final int scale, final RoundingMode roundingMode) {
        if (value == null) {
            return BigDecimal.ZERO;
        }
        return toScaledBigDecimal(BigDecimal.valueOf(value), scale, roundingMode);
    }

    /**
     * Convert a {@code String} to a {@code BigDecimal} with a scale of
     * two that has been rounded using {@code RoundingMode.HALF_EVEN}. If the supplied
     * {@code value} is {@code null}, then {@code BigDecimal.ZERO} is returned.
     *
     * <p>Note, the scale of a {@code BigDecimal} is the number of digits to the right of the decimal point.</p>
     *
     * @param value the {@code String} to convert, may be {@code null}.
     * @return the scaled, with appropriate rounding, {@code BigDecimal}.
     */
    public static BigDecimal toScaledBigDecimal(final String value) {
        return toScaledBigDecimal(value, INTEGER_TWO, RoundingMode.HALF_EVEN);
    }

    /**
     * Convert a {@code String} to a {@code BigDecimal} whose scale is the
     * specified value with a {@code RoundingMode} applied. If the input {@code value}
     * is {@code null}, we simply return {@code BigDecimal.ZERO}.
     *
     * @param value the {@code String} to convert, may be {@code null}.
     * @param scale the number of digits to the right of the decimal point.
     * @param roundingMode a rounding behavior for numerical operations capable of discarding precision.
     * @return the scaled, with appropriate rounding, {@code BigDecimal}.
     */
    public static BigDecimal toScaledBigDecimal(final String value, final int scale, final RoundingMode roundingMode) {
        if (value == null) {
            return BigDecimal.ZERO;
        }

        final BigDecimal num = createBigDecimal(value);

        return toScaledBigDecimal(num, scale, roundingMode);
    }

    /**
     * Returns the value of the {@code long} argument as an {@code int}, throwing an exception if the value overflows an {@code int}.
     *
     * <p>This method provides overflow checking when converting a long to an int. If the long value is outside
     * the range of int values (Integer.MIN_VALUE to Integer.MAX_VALUE), an ArithmeticException is thrown.</p>
     *
     * <p>Example usage:</p>
     * <pre>{@code
     * int result = Numbers.toIntExact(123L);              // returns 123
     * int overflow = Numbers.toIntExact(Integer.MAX_VALUE + 1L);  // throws ArithmeticException
     * }</pre>
     *
     * @param value the long value to convert to an int
     * @return the int value represented by the long argument
     * @throws ArithmeticException if the {@code value} overflows an int (outside range of Integer.MIN_VALUE to Integer.MAX_VALUE)
     * @see Math#toIntExact(long)
     */
    public static int toIntExact(final long value) {
        //    if (value < Integer.MIN_VALUE || value > Integer.MAX_VALUE) {
        //        throw new ArithmeticException("integer overflow: " + value);
        //    }
        //
        //    return (int) value;

        return Math.toIntExact(value);
    }

    /**
     * Converts a {@code String} to an {@code Integer}, handling hexadecimal (0x or 0X prefix) and octal (0 prefix) notations.
     *
     * <p>This method uses {@link Integer#decode(String)} to parse the string, which supports:</p>
     * <ul>
     * <li>Decimal numbers: "123", "-456"</li>
     * <li>Hexadecimal numbers: "0xFF", "0x10", "#FF"</li>
     * <li>Octal numbers: "0777" (leading zero indicates octal)</li>
     * </ul>
     *
     * <p>Note: Leading zeros indicate octal notation. Spaces are not trimmed from the input string.</p>
     *
     * <p>Example usage:</p>
     * <pre>{@code
     * Integer decimal = Numbers.createInteger("123");     // returns 123
     * Integer hex = Numbers.createInteger("0xFF");        // returns 255
     * Integer octal = Numbers.createInteger("010");       // returns 8
     * Integer nullValue = Numbers.createInteger(null);    // returns null
     * Numbers.createInteger("")                           // NumberFormatException!
     * Numbers.createInteger("   ")                        // NumberFormatException!
     * Numbers.createInteger("abc")                        // NumberFormatException!
     * }</pre>
     *
     * @param str the string to convert; must not be {@code null} or empty
     * @return the Integer value represented by the string, or {@code null} if the input string is {@code null}
     * @throws NumberFormatException if the string is empty or cannot be parsed as a valid integer
     * @see #isCreatable(String)
     * @see Integer#decode(String)
     */
    @MayReturnNull
    public static Integer createInteger(final String str) throws NumberFormatException {
        if (str == null) {
            return null;
        }

        // decode() handles 0xAABD and 0777 (hex and octal) as well.
        return Integer.decode(str);
    }

    /**
     * Converts a {@code String} to a {@code Long}, handling hexadecimal (0x or 0X prefix) and octal (0 prefix) notations.
     *
     * <p>This method uses {@link Long#decode(String)} to parse the string, which supports:</p>
     * <ul>
     * <li>Decimal numbers: "123", "-456"</li>
     * <li>Hexadecimal numbers: "0xFF", "0x10", "#FF"</li>
     * <li>Octal numbers: "0777" (leading zero indicates octal)</li>
     * </ul>
     *
     * <p>The method also handles strings ending with 'l' or 'L' suffix, which will be removed before parsing.</p>
     *
     * <p>Note: Leading zeros indicate octal notation. Spaces are not trimmed from the input string.</p>
     *
     * <p>Example usage:</p>
     * <pre>{@code
     * Long decimal = Numbers.createLong("123");       // returns 123L
     * Long hex = Numbers.createLong("0xFF");          // returns 255L
     * Long octal = Numbers.createLong("010");         // returns 8L
     * Long withSuffix = Numbers.createLong("123L");   // returns 123L
     * Long nullValue = Numbers.createLong(null);      // returns null
     * Numbers.createLong("")                          // NumberFormatException!
     * Numbers.createLong("   ")                       // NumberFormatException!
     * Numbers.createLong("abc")                       // NumberFormatException!
     * }</pre>
     *
     * @param str the string to convert; must not be {@code null} or empty
     * @return the Long value represented by the string, or {@code null} if the input string is {@code null}
     * @throws NumberFormatException if the string is empty or cannot be parsed as a valid long
     * @see #isCreatable(String)
     * @see Long#decode(String)
     */
    @MayReturnNull
    public static Long createLong(final String str) throws NumberFormatException {
        if (str == null) {
            return null;
        }

        if (str.length() > 1) {
            final char ch = str.charAt(str.length() - 1);

            if (ch == 'l' || ch == 'L') {
                return Long.decode(str.substring(0, str.length() - 1));
            }
        }

        return Long.decode(str);
    }

    // -----------------------------------------------------------------------

    /**
     * Converts a {@code String} to a {@code Float}.
     *
     * <p>This method parses a string representation of a floating-point number and returns a Float object.
     * The string can contain standard decimal notation or scientific notation (e.g., "1.23e-4").</p>
     *
     * <p>Example usage:</p>
     * <pre>{@code
     * Float f1 = Numbers.createFloat("123.45");       // returns 123.45f
     * Float f2 = Numbers.createFloat("-1.23e4");      // returns -12300.0f
     * Float f3 = Numbers.createFloat("NaN");          // returns Float.NaN
     * Float f4 = Numbers.createFloat("Infinity");     // returns Float.POSITIVE_INFINITY
     * Float nullValue = Numbers.createFloat(null);    // returns null
     * Numbers.createFloat("")                         // NumberFormatException!
     * Numbers.createFloat("   ")                      // NumberFormatException!
     * Numbers.createFloat("abc")                      // NumberFormatException!
     * }</pre>
     *
     * @param str the string to convert; must not be {@code null} or empty
     * @return the Float value represented by the string, or {@code null} if the input string is {@code null}
     * @throws NumberFormatException if the string is empty or cannot be parsed as a valid float
     * @see #isCreatable(String)
     * @see Float#valueOf(String)
     */
    @MayReturnNull
    public static Float createFloat(final String str) throws NumberFormatException {
        if (str == null) {
            return null;
        }

        return Float.valueOf(str);
    }

    /**
     * Converts a {@code String} to a {@code Double}.
     *
     * <p>This method parses a string representation of a floating-point number and returns a Double object.
     * The string can contain standard decimal notation or scientific notation (e.g., "1.23e-4").</p>
     *
     * <p>Example usage:</p>
     * <pre>{@code
     * Double d1 = Numbers.createDouble("123.45");         // returns 123.45
     * Double d2 = Numbers.createDouble("-1.23e10");       // returns -1.23e10
     * Double d3 = Numbers.createDouble("NaN");            // returns Double.NaN
     * Double d4 = Numbers.createDouble("Infinity");       // returns Double.POSITIVE_INFINITY
     * Double nullValue = Numbers.createDouble(null);      // returns null
     * Numbers.createDouble("")                            // NumberFormatException!
     * Numbers.createDouble("   ")                         // NumberFormatException!
     * Numbers.createDouble("abc")                         // NumberFormatException!
     * }</pre>
     *
     * @param str the string to convert; must not be {@code null} or empty
     * @return the Double value represented by the string, or {@code null} if the input string is {@code null}
     * @throws NumberFormatException if the string is empty or cannot be parsed as a valid double
     * @see #isCreatable(String)
     * @see Double#valueOf(String)
     */
    @MayReturnNull
    public static Double createDouble(final String str) throws NumberFormatException {
        if (str == null) {
            return null;
        }

        return Double.valueOf(str);
    }

    /**
     * Converts a {@code String} to a {@code BigInteger}, handling hexadecimal (0x, 0X, or # prefix) and octal (0 prefix) notations.
     *
     * <p>This method supports multiple number formats:</p>
     * <ul>
     * <li>Decimal numbers: "123", "-456"</li>
     * <li>Hexadecimal numbers: "0xFF", "0x10", "#FF"</li>
     * <li>Octal numbers: "0777" (leading zero indicates octal)</li>
     * </ul>
     *
     * <p>The method automatically detects the radix (base) based on the prefix and parses accordingly.
     * For hexadecimal, both "0x"/"0X" and "#" prefixes are supported. For octal, a leading zero is required.</p>
     *
     * <p>Example usage:</p>
     * <pre>{@code
     * BigInteger decimal = Numbers.createBigInteger("123456789012345");  // decimal
     * BigInteger hex = Numbers.createBigInteger("0xFFFFFFFF");           // returns 4294967295
     * BigInteger octal = Numbers.createBigInteger("0777");               // returns 511
     * BigInteger negative = Numbers.createBigInteger("-0xFF");           // returns -255
     * BigInteger nullValue = Numbers.createBigInteger(null);             // returns null
     * Numbers.createBigInteger("")                                       // NumberFormatException!
     * Numbers.createBigInteger("   ")                                    // NumberFormatException!
     * Numbers.createBigInteger("abc")                                    // NumberFormatException!
     * }</pre>
     *
     * @param str the string to convert; must not be {@code null} or empty
     * @return the BigInteger value represented by the string, or {@code null} if the input string is {@code null}
     * @throws NumberFormatException if the string is empty or not a valid BigInteger representation
     * @see #isCreatable(String)
     * @see BigInteger#BigInteger(String, int)
     */
    @MayReturnNull
    public static BigInteger createBigInteger(final String str) throws NumberFormatException {
        if (str == null) {
            return null;
        }

        if (!quickCheckForIsCreatable(str)) {
            throw new NumberFormatException(str + " is not a valid BigInteger.");
        }

        int pos = 0; // offset within string
        int radix = 10;
        boolean negate = false; // need to negate later?
        final char char0 = str.charAt(0);
        if (char0 == '-') {
            negate = true;
            pos = 1;
        } else if (char0 == '+') {
            pos = 1;
        }
        if (str.startsWith("0x", pos) || str.startsWith("0X", pos)) { // hex
            radix = 16;
            pos += 2;
        } else if (str.startsWith("#", pos)) { // alternative hex (allowed by Long/Integer)
            radix = 16;
            pos++;
        } else if (str.startsWith("0", pos) && str.length() > pos + 1) { // octal; so long as there are additional digits
            radix = 8;
            pos++;
        } // default is to treat as decimal

        final BigInteger value = new BigInteger(str.substring(pos), radix);

        return negate ? value.negate() : value;
    }

    /**
     * Converts a {@code String} to a {@code BigDecimal}.
     *
     * <p>This method parses a string representation of a decimal number and returns a BigDecimal object.
     * BigDecimal provides arbitrary-precision decimal arithmetic, making it suitable for financial calculations
     * and other scenarios requiring exact decimal representation.</p>
     *
     * <p>The string can contain:</p>
     * <ul>
     * <li>Standard decimal notation: "123.45", "-0.001"</li>
     * <li>Scientific notation: "1.23E+10", "-4.56e-8"</li>
     * <li>Leading/trailing zeros: "0.100", "123.000"</li>
     * </ul>
     *
     * <p>Example usage:</p>
     * <pre>{@code
     * BigDecimal bd1 = Numbers.createBigDecimal("123.45");        // exact decimal representation
     * BigDecimal bd2 = Numbers.createBigDecimal("1.23E+10");      // scientific notation
     * BigDecimal bd3 = Numbers.createBigDecimal("-0.001");        // negative small number
     * BigDecimal nullValue = Numbers.createBigDecimal(null);      // returns null
     * Numbers.createBigDecimal("")                                // NumberFormatException!
     * Numbers.createBigDecimal("   ")                             // NumberFormatException!
     * Numbers.createBigDecimal("abc")                             // NumberFormatException!
     * }</pre>
     *
     * @param str the string to convert; must not be {@code null} or empty
     * @return the BigDecimal value represented by the string, or {@code null} if the input string is {@code null}
     * @throws NumberFormatException if the string is empty or not a valid BigDecimal representation
     * @see #isCreatable(String)
     * @see BigDecimal#BigDecimal(String)
     */
    @MayReturnNull
    public static BigDecimal createBigDecimal(final String str) throws NumberFormatException {
        if (str == null) {
            return null;
        }

        // handle JDK1.3.1 bug where "" throws IndexOutOfBoundsException
        if (!quickCheckForIsCreatable(str)) {
            throw new NumberFormatException(str + " is not a valid BigDecimal.");
        }

        return new BigDecimal(str);
    }

    /**
     * <p>
     * Turns a string value into a java.lang.Number.
     * </p>
     *
     * <p>
     * If the string starts with {@code 0x} or {@code -0x} (lower or upper case)
     * or {@code #} or {@code -#}, it will be interpreted as a hexadecimal
     * Integer - or Long, if the number of digits after the prefix is more than
     * 8 - or BigInteger if there are more than 16 digits.
     * </p>
     * <p>
     * Then, the value is examined for a type qualifier on the end, i.e., one of
     * {@code 'f','F','d','D','l','L'}. If it is found, it starts trying to
     * create successively larger types from the type specified until one is
     * found that can represent the value.
     *
     * </p>
     *
     * <p>
     * If a type specifier is not found, it will check for a decimal point and
     * then try successively larger types from {@code Integer} to
     * {@code BigInteger} and from {@code double} to
     * {@code BigDecimal}.
     * </p>
     *
     * <p>
     * Integral values with a leading {@code 0} will be interpreted as octal;
     * the returned number will be Integer, Long or BigDecimal as appropriate.
     * </p>
     *
     * {@code null} is returned if the specified {@code str} is {@code null}.
     * <br />
     *
     * @param str a String containing a number; must not be {@code null} or empty
     * @return the parsed Number value, or {@code null} if the input string is {@code null}
     * @throws NumberFormatException if the string is empty or the value cannot be converted
     * @see #isCreatable(String)
     */
    @SuppressFBWarnings({ "SF_SWITCH_FALLTHROUGH", "SF_SWITCH_NO_DEFAULT" })
    @MayReturnNull
    public static Number createNumber(final String str) throws NumberFormatException {
        if (str == null) {
            return null;
        }

        if (!quickCheckForIsCreatable(str)) {
            throw new NumberFormatException(str + " is not a valid number.");
        }

        final int len = str.length();

        // Need to deal with all possible hex prefixes here
        final String[] hexPrefixes = { "0x", "0X", "#" };
        final int length = str.length();
        final int offset = str.charAt(0) == '+' || str.charAt(0) == '-' ? 1 : 0;
        int pfxLen = 0;
        for (final String pfx : hexPrefixes) {
            if (str.startsWith(pfx, offset)) {
                pfxLen += pfx.length() + offset;
                break;
            }
        }

        if (pfxLen > 0) { // we have a hex number
            char firstSigDigit = 0; // strip leading zeroes

            for (int i = pfxLen; i < len; i++) {
                firstSigDigit = str.charAt(i);
                if (firstSigDigit == '0') { // count leading zeroes
                    pfxLen++;
                } else {
                    break;
                }
            }

            final int hexDigits = len - pfxLen;

            if (hexDigits > 16 || hexDigits == 16 && firstSigDigit > '7') { // too many for Long
                return createBigInteger(str);
            } else if (hexDigits > 8 || hexDigits == 8 && firstSigDigit > '7') { // too many for an int
                return createLong(str);
            } else {
                return createInteger(str);
            }
        }

        final char lastChar = str.charAt(len - 1);
        String mant;
        String dec;
        String exp;
        final int decPos = str.indexOf('.');
        final int expPos = str.indexOf('e') + str.indexOf('E') + 1; // assumes both not present
        // if both e and E are present, this is caught by the checks on expPos (which prevent IOOBE)
        // and the parsing which will detect if e or E appear in a number due to using the wrong offset

        // Detect if the return type has been requested
        final boolean requestType = !Character.isDigit(lastChar) && lastChar != '.';
        if (decPos > -1) { // there is a decimal point
            if (expPos > -1) { // there is an exponent
                if (expPos < decPos || expPos > length) { // prevents double exponent causing IOOBE
                    throw new NumberFormatException(str + " is not a valid number.");
                }
                dec = str.substring(decPos + 1, expPos);
            } else {
                // No exponent, but there may be a type character to remove
                dec = str.substring(decPos + 1, requestType ? length - 1 : length);
            }
            mant = getMantissa(str, decPos);
        } else {
            if (expPos > -1) {
                if (expPos > length) { // prevents double exponent causing IOOBE
                    throw new NumberFormatException(str + " is not a valid number.");
                }
                mant = getMantissa(str, expPos);
            } else {
                // No decimal, no exponent, but there may be a type character to remove
                mant = getMantissa(str, requestType ? length - 1 : length);
            }
            dec = null;
        }

        if (!Character.isDigit(lastChar) && lastChar != '.') {
            if (expPos > -1 && expPos < len - 1) {
                exp = str.substring(expPos + 1, len - 1);
            } else {
                exp = null;
            }

            //Requesting a specific type.
            final String numeric = str.substring(0, len - 1);
            isAllZeros(mant);
            isAllZeros(exp);
            switch (lastChar) {
                case 'l':
                case 'L':
                    if (dec == null && exp == null && (!numeric.isEmpty() && numeric.charAt(0) == '-' && isDigits(numeric.substring(1)) || isDigits(numeric))) {
                        try {
                            return createLong(numeric);
                        } catch (final NumberFormatException ignored) {
                            // Too big for a long
                        }
                        return createBigInteger(numeric);

                    }
                    throw new NumberFormatException(str + " is not a valid number.");
                case 'f':
                case 'F':
                    try {
                        final Float f = createFloat(str);
                        if (!(f.isInfinite() || N.equals(f.floatValue(), 0.0F) && !isZero(mant, dec))) {
                            //If it's too big for a float or the float value = 0 and the string
                            //has non-zeros in it, then float does not have the precision we want
                            return f;
                        }

                    } catch (final NumberFormatException ignored) {
                        // ignore the bad number
                    }
                    //$FALL-THROUGH$
                case 'd':
                case 'D':
                    try {
                        final Double d = createDouble(str);
                        if (!(d.isInfinite() || N.equals(d.doubleValue(), 0.0d) && !isZero(mant, dec))) {
                            return d;
                        }
                    } catch (final NumberFormatException ignored) {
                        // ignore the bad number
                    }
                    try {
                        return createBigDecimal(numeric);
                    } catch (final NumberFormatException ignored) {
                        // ignore the bad number
                    }
                    //$FALL-THROUGH$
                default:
                    throw new NumberFormatException(str + " is not a valid number.");
            }
        }
        //User doesn't have a preference on the return type, so let's start
        //small and go from there...
        if (expPos > -1 && expPos < length - 1) {
            exp = str.substring(expPos + 1);
        } else {
            exp = null;
        }

        if (dec == null && exp == null) { // no decimal point and no exponent
            //Must be an Integer, Long, Biginteger
            try {
                return createInteger(str);
            } catch (final NumberFormatException ignored) {
                // ignore the bad number
            }
            try {
                return createLong(str);
            } catch (final NumberFormatException ignored) {
                // ignore the bad number
            }
            return createBigInteger(str);
        }

        //    //Must be a Float, Double, BigDecimal
        //    try {
        //        final Float f = createFloat(str);
        //        final Double d = createDouble(str);
        //        if (!f.isInfinite()
        //                && !(f.floatValue() == 0.0F && !isZero(mant, dec))
        //                && f.toString().equals(d.toString())) {
        //            return f;
        //        }
        //        if (!d.isInfinite() && !(d.doubleValue() == 0.0D && !isZero(mant, dec))) {
        //            final BigDecimal b = createBigDecimal(str);
        //            if (b.compareTo(BigDecimal.valueOf(d.doubleValue())) == 0) {
        //                return d;
        //            }
        //            return b;
        //        }
        //    } catch (final NumberFormatException ignored) {
        //        // ignore the bad number
        //    }

        try {
            if (len > 308 && (decPos < 0 || decPos > 308)) { // MAX_VALUE = 0x1.fffffffffffffP+1023; // 1.7976931348623157e+308
                return createBigDecimal(str);
            }

            final Double d = Double.valueOf(str);

            if (d.isInfinite() && !str.contains("Infinity")) {
                return createBigDecimal(str);
            }

            return d;
        } catch (final NumberFormatException nfe) {
            // ignore the bad number
        }

        return createBigDecimal(str);
    }

    /**
     * Checks whether the {@link String} contains only digit characters.
     *
     * <p>{@code null} and empty String will return {@code false}.</p>
     *
     * @param str the {@link String} to check
     * @return {@code true} if str contains only Unicode numeric
     * @see Strings#isNumeric(CharSequence)
     */
    public static boolean isDigits(final String str) {
        return Strings.isNumeric(str);
    }

    /**
     * <p>Utility method for {@link Numbers#createNumber(java.lang.String)}.</p>
     *
     * <p>Returns mantissa of the given number.</p>
     *
     * @param str the string representation of the number
     * @param stopPos the position of the exponent or decimal point
     * @return mantissa of the given number
     */
    static String getMantissa(final String str, final int stopPos) {
        final char firstChar = str.charAt(0);
        final boolean hasSign = firstChar == '-' || firstChar == '+';

        return hasSign ? str.length() == 1 ? Strings.EMPTY : str.substring(1, stopPos) : str.substring(0, stopPos);
    }

    /**
     * Utility method for {@link #createNumber(java.lang.String)}.
     *
     * <p>This will check if the magnitude of the number is zero by checking if there
     * are only zeros before and after the decimal place.</p>
     *
     * <p>Note: It is <strong>assumed</strong> that the input string has been converted
     * to either a Float or Double with a value of zero when this method is called.
     * This eliminates invalid input for example {@code ".", ".D", ".e0"}.</p>
     *
     * <p>Thus the method only requires checking if both arguments are {@code null}, empty or contain only zeros.</p>
     *
     * <p>Given {@code s = mant + "." + dec}:</p>
     * <ul>
     * <li>{@code true} if s is {@code "0.0"}
     * <li>{@code true} if s is {@code "0."}
     * <li>{@code true} if s is {@code ".0"}
     * <li>{@code false} otherwise (this assumes {@code "."} is not possible)
     * </ul>
     *
     * @param mant the mantissa decimal digits before the decimal point (sign must be removed; never null)
     * @param dec the decimal digits after the decimal point (exponent and type specifier removed; can be null)
     * @return {@code true} if the magnitude is zero
     */
    private static boolean isZero(final String mant, final String dec) {
        return isAllZeros(mant) && isAllZeros(dec);
    }

    /**
     * Utility method for {@link #createNumber(java.lang.String)}.
     *
     * <p>Returns {@code true} if s is {@code null} or empty.</p>
     *
     * @param str the String to check
     * @return if it is all zeros or {@code null}
     */
    private static boolean isAllZeros(final String str) {
        if (str == null) {
            return true;
        }

        for (int i = str.length() - 1; i >= 0; i--) {
            if (str.charAt(i) != '0') {
                return false;
            }
        }

        return true;
    }

    /**
     * Note: It's copied from NumberUtils in Apache Commons Lang under Apache
     * License 2.0
     *
     * <br />
     * <br />
     *
     * It's same as {@code isCreatable(String)}.
     *
     * @param str
     * @return
     * @see #isCreatable(String)
     * @see #isParsable(String)
     */
    public static boolean isNumber(final String str) {
        return isCreatable(str);
    }

    private static final boolean[] alphanumerics = new boolean[128];

    static {
        alphanumerics['0'] = true;
        alphanumerics['1'] = true;
        alphanumerics['2'] = true;
        alphanumerics['3'] = true;
        alphanumerics['4'] = true;
        alphanumerics['5'] = true;
        alphanumerics['6'] = true;
        alphanumerics['7'] = true;
        alphanumerics['8'] = true;
        alphanumerics['9'] = true;

        alphanumerics['+'] = true;
        alphanumerics['-'] = true;
        alphanumerics['.'] = true;
        alphanumerics['#'] = true;

        alphanumerics['x'] = true;
        alphanumerics['X'] = true;

        //noinspection OverwrittenKey
        alphanumerics['e'] = true;
        //noinspection OverwrittenKey
        alphanumerics['E'] = true;

        alphanumerics['a'] = true;
        alphanumerics['A'] = true;
        alphanumerics['b'] = true;
        alphanumerics['B'] = true;
        alphanumerics['c'] = true;
        alphanumerics['C'] = true;
        //noinspection OverwrittenKey
        alphanumerics['d'] = true;
        alphanumerics['D'] = true;
        //noinspection OverwrittenKey,DataFlowIssue
        alphanumerics['e'] = true;
        //noinspection OverwrittenKey
        alphanumerics['E'] = true;
        //noinspection OverwrittenKey
        alphanumerics['f'] = true;
        //noinspection OverwrittenKey
        alphanumerics['F'] = true;

        alphanumerics['l'] = true;
        alphanumerics['L'] = true;
        //noinspection OverwrittenKey,DataFlowIssue
        alphanumerics['f'] = true;
        //noinspection OverwrittenKey
        alphanumerics['F'] = true;
        //noinspection OverwrittenKey,DataFlowIssue
        alphanumerics['d'] = true;
        //noinspection OverwrittenKey
        alphanumerics['D'] = true;
    }

    static boolean quickCheckForIsCreatable(final String str) {
        if (Strings.isEmpty(str)) {
            return false;
        }

        final int len = str.length();
        char ch = 0;

        return ((ch = str.charAt(0)) < 128 && alphanumerics[ch]) && ((ch = str.charAt(len - 1)) < 128 && alphanumerics[ch])
                && ((ch = str.charAt(len / 2)) < 128 && alphanumerics[ch]);
    }

    /**
     * <p>Checks whether the String a valid Java number.</p>
     *
     * <p>Valid numbers include hexadecimal marked with the {@code 0x} or
     * {@code 0X} qualifier, octal numbers, scientific notation and
     * numbers marked with a type qualifier (e.g., 123L).</p>
     *
     * <p>Non-hexadecimal strings beginning with a leading zero are
     * treated as octal values. Thus the string {@code 09} will return
     * {@code false}, since {@code 9} is not a valid octal value.
     * However, numbers beginning with {@code 0.} are treated as decimal.</p>
     *
     * <p>{@code null} and empty/blank {@code String} will return {@code false}.</p>
     *
     * <p>Note, {@link #createNumber(String)} should return a number for every input resulting in {@code true}.</p>
     *
     * @param str the {@code String} to check
     * @return {@code true} if the string is a correctly formatted number
     * @see #isNumber(String)
     * @see #isParsable(String)
     */
    public static boolean isCreatable(final String str) {
        if (!quickCheckForIsCreatable(str)) {
            return false;
        }

        final char[] chars = str.toCharArray();
        int len = chars.length;

        boolean hasExp = false;
        boolean hasDecPoint = false;
        boolean allowSigns = false;
        boolean foundDigit = false;
        // deal with any possible sign up front
        final int start = chars[0] == '-' || chars[0] == '+' ? 1 : 0;

        if (len > start + 1 && chars[start] == '0' && !Strings.contains(str, '.')) { // leading 0, skip if is a decimal number
            if (chars[start + 1] == 'x' || chars[start + 1] == 'X') { // leading 0x/0X
                int i = start + 2;
                if (i == len) {
                    return false; // str == "0x"
                }
                // checking hex (it can't be anything else)
                for (; i < chars.length; i++) {
                    if ((chars[i] < '0' || chars[i] > '9') && (chars[i] < 'a' || chars[i] > 'f') && (chars[i] < 'A' || chars[i] > 'F')) {
                        return false;
                    }
                }
                return true;
            } else if (Character.isDigit(chars[start + 1])) {
                // leading 0, but not hex, must be octal
                int i = start + 1;
                for (; i < chars.length; i++) {
                    if (chars[i] < '0' || chars[i] > '7') {
                        return false;
                    }
                }
                return true;
            }
        }

        len--; // don't want to loop to the last char, check it afterwords
        // for type qualifiers
        int i = start;
        // loop to the next to last char or to the last char if we need another digit to
        // make a valid number (e.g., chars[0..5] = "1234E")
        while (i < len || i < len + 1 && allowSigns && !foundDigit) {
            if (chars[i] >= '0' && chars[i] <= '9') {
                foundDigit = true;
                allowSigns = false;

            } else if (chars[i] == '.') {
                if (hasDecPoint || hasExp) {
                    // two decimal points or dec in exponent
                    return false;
                }
                hasDecPoint = true;
            } else if (chars[i] == 'e' || chars[i] == 'E') {
                // we've already taken care of hex.
                if (hasExp || !foundDigit) {
                    return false;
                }
                hasExp = true;
                allowSigns = true;
            } else if (chars[i] == '+' || chars[i] == '-') {
                if (!allowSigns) {
                    return false;
                }
                allowSigns = false;
                foundDigit = false; // we need a digit after the E
            } else {
                return false;
            }
            i++;
        }

        if (i < chars.length) {
            if (chars[i] >= '0' && chars[i] <= '9') {
                // no type qualifier, OK
                return true;
            }

            if (chars[i] == 'e' || chars[i] == 'E') {
                // can't have an E at the last byte
                return false;
            }

            if (chars[i] == '.') {
                if (hasDecPoint || hasExp) {
                    // two decimal points or dec in exponent
                    return false;
                }
                // single trailing decimal point after non-exponent is ok
                return foundDigit;
            }

            if (!allowSigns && (chars[i] == 'd' || chars[i] == 'D' || chars[i] == 'f' || chars[i] == 'F')) {
                return foundDigit;
            }

            if (chars[i] == 'l' || chars[i] == 'L') {
                // not allowing L with an exponent or decimal point
                return foundDigit && !hasExp && !hasDecPoint;
            }

            // last character is illegal
            return false;
        }
        // allowSigns is true iff the val ends in 'E'
        // found digit it to make sure weird stuff like '.' and '1E-' doesn't pass
        return !allowSigns && foundDigit;
    }

    /**
     * <p>Checks whether the given String is a parsable number.</p>
     *
     * <p>Parsable numbers include those Strings understood by {@link Integer#parseInt(String)},
     * {@link Long#parseLong(String)}, {@link Float#parseFloat(String)} or
     * {@link Double#parseDouble(String)}. This method can be used instead of catching {@link NumberFormatException} when calling one of those methods.</p>
     *
     * <p>Hexadecimal and scientific notations are <strong>not</strong> considered parsable.
     * See {@link #isCreatable(String)} on those cases.</p>
     *
     * <p>{@code Null} and empty String will return {@code false}.</p>
     *
     * @param str the String to check.
     * @return {@code true} if the string is a parsable number.
     */
    public static boolean isParsable(final String str) {
        if (Strings.isEmpty(str)) {
            return false;
        }

        final int len = str.length();

        if (str.charAt(len - 1) == '.') {
            return false;
        }

        final char ch = str.charAt(0);

        if (ch == '-' || ch == '+') {
            if (str.length() == 1) {
                return false;
            }

            return withDecimalsParsing(str, 1);
        }

        return withDecimalsParsing(str, 0);
    }

    private static boolean withDecimalsParsing(final String str, final int beginIdx) {
        int decimalPoints = 0;
        boolean isDecimalPoint = false;
        char ch = 0;

        for (int i = beginIdx; i < str.length(); i++) {
            ch = str.charAt(i);
            isDecimalPoint = ch == '.';

            if (isDecimalPoint) {
                decimalPoints++;
            }

            if ((decimalPoints > 1) || (!isDecimalPoint && !Character.isDigit(ch))) {
                return false;
            }
        }

        return true;
    }

    //    /**

    /**
     * Returns {@code true} if {@code n} is a <a href="http://mathworld.wolfram.com/PrimeNumber.html">prime number</a>:
     * an integer <i>greater than one</i> that cannot be factored into a product of <i>smaller</i> positive integers.
     *
     * <p>This method uses the Miller-Rabin primality test with deterministic bases for numbers up to {@code Long.MAX_VALUE}.
     * It provides a fast and accurate primality test for 64-bit integers.</p>
     *
     * <p>Returns {@code false} if {@code n} is zero, one, or a composite number (one which <i>can</i> be factored into smaller positive integers).</p>
     *
     * <p>Note: To test larger numbers (beyond long range), use {@link BigInteger#isProbablePrime(int)}.</p>
     *
     * <p>Example usage:</p>
     * <pre>{@code
     * boolean b1 = Numbers.isPrime(2);      // returns true (smallest prime)
     * boolean b2 = Numbers.isPrime(17);     // returns true
     * boolean b3 = Numbers.isPrime(100);    // returns false (composite)
     * boolean b4 = Numbers.isPrime(1);      // returns false (not prime by definition)
     * boolean b5 = Numbers.isPrime(0);      // returns false
     * }</pre>
     *
     * @param n the number to test for primality; must be >= 0
     * @return {@code true} if n is prime, {@code false} otherwise (including for n < 2)
     * @throws IllegalArgumentException if {@code n} is negative
     * @see BigInteger#isProbablePrime(int)
     */
    @SuppressWarnings("ConditionCoveredByFurtherCondition")
    public static boolean isPrime(final long n) {
        if (n < 2) {
            checkNonNegative("n", n);
            return false;
        }
        //noinspection ConditionCoveredByFurtherCondition
        if (n < 14 && (n == 2 || n == 3 || n == 5 || n == 7 || n == 11 || n == 13)) {
            return true;
        }

        if (((SIEVE_30 & (1 << (n % 30))) != 0) || n % 7 == 0 || n % 11 == 0 || n % 13 == 0) {
            return false;
        }
        if (n < 17 * 17) {
            return true;
        }

        for (final long[] baseSet : millerRabinBaseSets) {
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

    /**
     * Checks if the given integer is a perfect square (i.e., the square of an integer).
     *
     * <p>A perfect square is a non-negative integer that can be expressed as n = k * k for some integer k.
     * This method uses an optimized algorithm that first checks the lower 4 bits as a fast filter,
     * then performs a square root check for potential candidates.</p>
     *
     * <p>Example usage:</p>
     * <pre>{@code
     * boolean b1 = Numbers.isPerfectSquare(16);   // returns true (4*4)
     * boolean b2 = Numbers.isPerfectSquare(25);   // returns true (5*5)
     * boolean b3 = Numbers.isPerfectSquare(26);   // returns false
     * boolean b4 = Numbers.isPerfectSquare(0);    // returns true (0*0)
     * boolean b5 = Numbers.isPerfectSquare(-4);   // returns false (negative numbers are not perfect squares)
     * }</pre>
     *
     * @param n the integer to check
     * @return {@code true} if n is a perfect square, {@code false} otherwise (including for negative numbers)
     */
    public static boolean isPerfectSquare(final int n) {
        if (n < 0) {
            return false;
        }

        switch (n & 0xF) {
            case 0:
            case 1:
            case 4:
            case 9:
                final long tst = (long) Math.sqrt(n);
                return tst * tst == n;

            default:
                return false;
        }
    }

    /**
     * Checks if the given long value is a perfect square (i.e., the square of an integer).
     *
     * <p>A perfect square is a non-negative integer that can be expressed as n = k * k for some integer k.
     * This method uses an optimized algorithm that first checks the lower 4 bits as a fast filter,
     * then performs a square root check for potential candidates.</p>
     *
     * <p>Example usage:</p>
     * <pre>{@code
     * boolean b1 = Numbers.isPerfectSquare(16L);          // returns true (4*4)
     * boolean b2 = Numbers.isPerfectSquare(100L);         // returns true (10*10)
     * boolean b3 = Numbers.isPerfectSquare(1000000L);     // returns true (1000*1000)
     * boolean b4 = Numbers.isPerfectSquare(1000001L);     // returns false
     * boolean b5 = Numbers.isPerfectSquare(-100L);        // returns false (negative numbers are not perfect squares)
     * }</pre>
     *
     * @param n the long value to check
     * @return {@code true} if n is a perfect square, {@code false} otherwise (including for negative numbers)
     */
    public static boolean isPerfectSquare(final long n) {
        if (n < 0) {
            return false;
        }

        switch ((int) (n & 0xF)) {
            case 0:
            case 1:
            case 4:
            case 9:
                final long tst = (long) Math.sqrt(n);
                return tst * tst == n;

            default:
                return false;
        }
    }

    /**
     * Checks if the given integer is a power of two (i.e., 2^k for some non-negative integer k).
     *
     * <p>A power of two is a positive integer that can be expressed as 2^k where k is a non-negative integer.
     * This method uses a bitwise trick: a power of two has exactly one bit set in its binary representation.</p>
     *
     * <p>Example usage:</p>
     * <pre>{@code
     * boolean b1 = Numbers.isPowerOfTwo(1);      // returns true (2^0)
     * boolean b2 = Numbers.isPowerOfTwo(16);     // returns true (2^4)
     * boolean b3 = Numbers.isPowerOfTwo(1024);   // returns true (2^10)
     * boolean b4 = Numbers.isPowerOfTwo(100);    // returns false
     * boolean b5 = Numbers.isPowerOfTwo(0);      // returns false
     * boolean b6 = Numbers.isPowerOfTwo(-8);     // returns false (negative numbers cannot be powers of two)
     * }</pre>
     *
     * @param x the integer to check
     * @return {@code true} if x is a power of two, {@code false} otherwise
     */
    public static boolean isPowerOfTwo(final int x) {
        return x > 0 && (x & (x - 1)) == 0;
    }

    /**
     * Checks if the given long value is a power of two (i.e., 2^k for some non-negative integer k).
     *
     * <p>A power of two is a positive integer that can be expressed as 2^k where k is a non-negative integer.
     * This method uses a bitwise trick: a power of two has exactly one bit set in its binary representation.</p>
     *
     * <p>Example usage:</p>
     * <pre>{@code
     * boolean b1 = Numbers.isPowerOfTwo(1L);             // returns true (2^0)
     * boolean b2 = Numbers.isPowerOfTwo(1024L);          // returns true (2^10)
     * boolean b3 = Numbers.isPowerOfTwo(1099511627776L); // returns true (2^40)
     * boolean b4 = Numbers.isPowerOfTwo(1000L);          // returns false
     * boolean b5 = Numbers.isPowerOfTwo(0L);             // returns false
     * }</pre>
     *
     * @param x the long value to check
     * @return {@code true} if x is a power of two, {@code false} otherwise
     */
    public static boolean isPowerOfTwo(final long x) {
        return x > 0 && (x & (x - 1)) == 0;
    }

    /**
     * Checks if the given double value is a power of two (i.e., 2^k for some integer k).
     *
     * <p>Unlike the integer versions, this method can handle fractional powers of two (negative exponents)
     * such as 0.5 (2^-1), 0.25 (2^-2), etc., as well as large powers beyond the long range.</p>
     *
     * <p>The method checks that the value is positive, finite (not infinity or NaN), and has a significand
     * (mantissa) that is itself a power of two.</p>
     *
     * <p>Example usage:</p>
     * <pre>{@code
     * boolean b1 = Numbers.isPowerOfTwo(16.0);           // returns true (2^4)
     * boolean b2 = Numbers.isPowerOfTwo(0.5);            // returns true (2^-1)
     * boolean b3 = Numbers.isPowerOfTwo(0.25);           // returns true (2^-2)
     * boolean b4 = Numbers.isPowerOfTwo(3.0);            // returns false
     * boolean b5 = Numbers.isPowerOfTwo(Double.NaN);     // returns false
     * boolean b6 = Numbers.isPowerOfTwo(Double.POSITIVE_INFINITY);  // returns false
     * }</pre>
     *
     * @param x the double value to check
     * @return {@code true} if x is a power of two, {@code false} otherwise
     */
    public static boolean isPowerOfTwo(final double x) {
        return x > 0.0 && isFinite(x) && isPowerOfTwo(getSignificand(x));
    }

    /**
     * Returns {@code true} if {@code x} represents a power of two.
     *
     * @param x
     * @return {@code true}, if is power of two
     * @throws IllegalArgumentException
     */
    public static boolean isPowerOfTwo(final BigInteger x) throws IllegalArgumentException {
        N.checkArgNotNull(x);
        return x.signum() > 0 && x.getLowestSetBit() == x.bitLength() - 1;
    }

    /**
     * Returns the natural logarithm (base e) of a double value.
     *
     * @param a the value to compute the logarithm of
     * @return the natural logarithm of the specified value
     * @see Math#log(double)
     */
    public static double log(final double a) {
        return Math.log(a);
    }

    /**
     * Returns the base-2 logarithm of an integer value, rounded according to the specified rounding mode.
     *
     * @param x the integer value to compute the logarithm of, must be positive
     * @param mode the rounding mode to apply
     * @return the base-2 logarithm of the specified value, rounded according to the specified rounding mode
     * @throws IllegalArgumentException if x is not positive
     * @see RoundingMode
     */
    @SuppressFBWarnings("SF_SWITCH_FALLTHROUGH")
    public static int log2(final int x, final RoundingMode mode) {
        checkPositive("x", x);

        switch (mode) {
            case UNNECESSARY:
                checkRoundingUnnecessary(isPowerOfTwo(x));
                //$FALL-THROUGH$
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
                final int leadingZeros = Integer.numberOfLeadingZeros(x);
                final int cmp = INT_MAX_POWER_OF_SQRT2_UNSIGNED >>> leadingZeros;
                // floor(2^(logFloor + 0.5))
                final int logFloor = (Integer.SIZE - 1) - leadingZeros;
                return logFloor + lessThanBranchFree(cmp, x);

            default:
                throw new AssertionError();
        }
    }

    /**
     * Returns the base-2 logarithm of {@code x}, rounded according to the specified rounding mode.
     *
     * @param x
     * @param mode
     * @return
     * @throws IllegalArgumentException if {@code x <= 0}
     * @throws ArithmeticException if {@code mode} is {@link RoundingMode#UNNECESSARY} and {@code x}
     *     is not a power of two
     */
    @SuppressFBWarnings("SF_SWITCH_FALLTHROUGH")
    // TODO(kevinb): remove after this warning is disabled globally
    public static int log2(final long x, final RoundingMode mode) {
        checkPositive("x", x);
        switch (mode) {
            case UNNECESSARY:
                checkRoundingUnnecessary(isPowerOfTwo(x));
                //$FALL-THROUGH$
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
                final int leadingZeros = Long.numberOfLeadingZeros(x);
                final long cmp = MAX_POWER_OF_SQRT2_UNSIGNED >>> leadingZeros;
                // floor(2^(logFloor + 0.5))
                final int logFloor = (Long.SIZE - 1) - leadingZeros;
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
     *
     * @param x
     * @return
     */
    public static double log2(final double x) {
        return Math.log(x) / LN_2; // surprisingly within 1 ulp according to tests
    }

    /**
     * Returns the base 2 logarithm of a double value, rounded with the specified rounding mode to an
     * {@code int}.
     *
     * <p>Regardless of the rounding mode, this is faster than {@code (int) log2(x)}.
     *
     * @param x
     * @param mode
     * @return
     * @throws IllegalArgumentException if {@code x <= 0.0}, {@code x} is NaN, or {@code x} is
     *     infinite
     */
    @SuppressFBWarnings("SF_SWITCH_FALLTHROUGH")
    public static int log2(final double x, final RoundingMode mode) throws IllegalArgumentException {
        N.checkArgument(x > 0.0 && isFinite(x), "x must be positive and finite");
        final int exponent = getExponent(x);
        if (!isNormal(x)) {
            return log2(x * IMPLICIT_BIT, mode) - SIGNIFICAND_BITS;
            // Do the calculation on a normal value.
        }
        // x is positive, finite, and normal
        boolean increment;
        switch (mode) {
            case UNNECESSARY:
                checkRoundingUnnecessary(isPowerOfTwo(x));
                //$FALL-THROUGH$
            case FLOOR:
                increment = false;
                break;
            case CEILING:
                increment = !isPowerOfTwo(x);
                break;
            case DOWN:
                increment = exponent < 0 & !isPowerOfTwo(x); //NOSONAR
                break;
            case UP:
                increment = exponent >= 0 & !isPowerOfTwo(x); //NOSONAR
                break;
            case HALF_DOWN:
            case HALF_EVEN:
            case HALF_UP:
                final double xScaled = scaleNormalize(x);
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
     * @param x
     * @param mode
     * @return
     * @throws IllegalArgumentException if {@code x <= 0}
     * @throws ArithmeticException if {@code mode} is {@link RoundingMode#UNNECESSARY} and {@code x}
     *     is not a power of two
     */
    @SuppressFBWarnings("SF_SWITCH_FALLTHROUGH")
    @SuppressWarnings("fallthrough")
    // TODO(kevinb): remove after this warning is disabled globally
    public static int log2(final BigInteger x, final RoundingMode mode) throws IllegalArgumentException {
        checkPositive("x", N.checkArgNotNull(x));
        final int logFloor = x.bitLength() - 1;
        switch (mode) {
            case UNNECESSARY:
                checkRoundingUnnecessary(isPowerOfTwo(x)); // fall through
            case DOWN, FLOOR:
                return logFloor;

            case UP:
            case CEILING:
                return isPowerOfTwo(x) ? logFloor : logFloor + 1;

            case HALF_DOWN, HALF_UP, HALF_EVEN:
                if (logFloor < SQRT2_PRECOMPUTE_THRESHOLD) {
                    final BigInteger halfPower = SQRT2_PRECOMPUTED_BITS.shiftRight(SQRT2_PRECOMPUTE_THRESHOLD - logFloor);
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
                final BigInteger x2 = x.pow(2);
                final int logX2Floor = x2.bitLength() - 1;
                return (logX2Floor < 2 * logFloor + 1) ? logFloor : logFloor + 1;

            default:
                throw new AssertionError();
        }
    }

    /**
     * Returns the base-10 logarithm of an integer value, rounded according to the specified rounding mode.
     *
     * @param x the integer value to compute the logarithm of, must be positive
     * @param mode the rounding mode to apply
     * @return the base-10 logarithm of the specified value, rounded according to the specified rounding mode
     * @throws IllegalArgumentException if x is not positive
     */
    @SuppressFBWarnings("SF_SWITCH_FALLTHROUGH")
    public static int log10(final int x, final RoundingMode mode) {
        checkPositive("x", x);

        final int logFloor = log10Floor(x);
        final int floorPow = int_powersOf10[logFloor];
        switch (mode) {
            case UNNECESSARY:
                checkRoundingUnnecessary(x == floorPow);
                //$FALL-THROUGH$
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

    private static int log10Floor(final int x) {
        /*
         * Based on Hacker's Delight Fig. 11-5, the two-table-lookup, branch-free implementation.
         *
         * The key idea is that based on the number of leading zeros (equivalently, floor(log2(x))),
         * we can narrow the possible floor(log10(x)) values to two.  For example, if floor(log2(x))
         * is 6, then 64 <= x < 128, so floor(log10(x)) is either 1 or 2.
         */
        final int y = int_maxLog10ForLeadingZeros[Integer.numberOfLeadingZeros(x)];
        /*
         * y is the higher of the two possible values of floor(log10(x)). If x < 10^y, then we want the
         * lower of the two possible values, or y - 1, otherwise, we want y.
         */
        return y - lessThanBranchFree(x, int_powersOf10[y]);
    }

    /**
     * Returns the base-10 logarithm of {@code x}, rounded according to the specified rounding mode.
     *
     * @param x
     * @param mode
     * @return
     * @throws IllegalArgumentException if {@code x <= 0}
     * @throws ArithmeticException if {@code mode} is {@link RoundingMode#UNNECESSARY} and {@code x}
     *     is not a power of ten
     */
    @SuppressFBWarnings("SF_SWITCH_FALLTHROUGH")
    // TODO(kevinb): remove after this warning is disabled globally
    public static int log10(final long x, final RoundingMode mode) {
        checkPositive("x", x);

        final int logFloor = log10Floor(x);
        final long floorPow = powersOf10[logFloor];
        switch (mode) {
            case UNNECESSARY:
                checkRoundingUnnecessary(x == floorPow);
                //$FALL-THROUGH$
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

    /**
     * Returns the base-10 logarithm of the given double value.
     *
     * <p>This method is a convenience wrapper around {@link Math#log10(double)}, providing the base-10
     * logarithm calculation. The result is the power to which 10 must be raised to obtain the value x.</p>
     *
     * <p>Special cases:</p>
     * <ul>
     * <li>If the argument is NaN or less than zero, the result is NaN.</li>
     * <li>If the argument is positive infinity, the result is positive infinity.</li>
     * <li>If the argument is positive zero or negative zero, the result is negative infinity.</li>
     * <li>If the argument is 10^n for integer n, the result is n.</li>
     * </ul>
     *
     * <p>Example usage:</p>
     * <pre>{@code
     * double log1 = Numbers.log10(100.0);     // returns 2.0 (10^2 = 100)
     * double log2 = Numbers.log10(1000.0);    // returns 3.0 (10^3 = 1000)
     * double log3 = Numbers.log10(1.0);       // returns 0.0 (10^0 = 1)
     * double log4 = Numbers.log10(0.1);       // returns -1.0 (10^-1 = 0.1)
     * }</pre>
     *
     * @param x the value whose base-10 logarithm is to be computed
     * @return the base-10 logarithm of x
     * @see Math#log10(double)
     * @see #log10(int, RoundingMode)
     * @see #log10(long, RoundingMode)
     */
    public static double log10(final double x) {
        return Math.log10(x);
    }

    /*
     * The maximum number of bits in a square root for which we'll precompute an explicit half-power
     * of two. This can be any value, but higher values incur more class load time and linearly
     * increasing memory consumption.
     */
    static final int SQRT2_PRECOMPUTE_THRESHOLD = 256;

    static final BigInteger SQRT2_PRECOMPUTED_BITS = new BigInteger("16a09e667f3bcc908b2fb1366ea957d3e3adec17512775099da2f590b0667322a", 16);

    /**
     * Returns the base-10 logarithm of {@code x}, rounded according to the specified rounding mode.
     *
     * @param x
     * @param mode
     * @return
     * @throws IllegalArgumentException if {@code x <= 0}
     * @throws ArithmeticException if {@code mode} is {@link RoundingMode#UNNECESSARY} and {@code x}
     *     is not a power of ten
     */
    @SuppressFBWarnings("SF_SWITCH_FALLTHROUGH")
    public static int log10(final BigInteger x, final RoundingMode mode) {
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

        final int floorLog = approxLog10;
        final BigInteger floorPow = approxPow;
        final int floorCmp = approxCmp;

        switch (mode) {
            case UNNECESSARY:
                checkRoundingUnnecessary(floorCmp == 0);
                //$FALL-THROUGH$
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
                final BigInteger x2 = x.pow(2);
                final BigInteger halfPowerSquared = floorPow.pow(2).multiply(BigInteger.TEN);
                return (x2.compareTo(halfPowerSquared) <= 0) ? floorLog : floorLog + 1;
            default:
                throw new AssertionError();
        }
    }

    /**
     * Fits in long.
     *
     * @param x
     * @return
     */
    static boolean fitsInLong(final BigInteger x) {
        return x.bitLength() <= Long.SIZE - 1;
    }

    private static final double LN_10 = Math.log(10);

    private static final double LN_2 = Math.log(2);

    /**
     * Returns {@code b} to the {@code k}th power.
     *
     * <p>This method computes integer exponentiation using an efficient O(log k) algorithm.
     * If the result overflows, the returned value will be equal to the low-order bits of the
     * true result, following standard Java overflow semantics (similar to multiplication overflow).
     *
     * <p>Special cases are optimized for common bases (0, 1, -1, 2, -2).
     *
     * <p>Examples:
     * <pre>{@code
     * Numbers.pow(2, 3) = 8
     * Numbers.pow(3, 4) = 81
     * Numbers.pow(5, 0) = 1        // any number to power 0 is 1
     * Numbers.pow(0, 5) = 0        // 0 to any positive power is 0
     * Numbers.pow(0, 0) = 1        // by convention
     * Numbers.pow(-2, 3) = -8
     * Numbers.pow(-2, 4) = 16
     * Numbers.pow(10, 9) = 1000000000
     * Numbers.pow(10, 10)          // overflows, returns low-order bits
     * }</pre>
     *
     * @param b the base integer
     * @param k the exponent; must be non-negative
     * @return {@code b} raised to the {@code k}th power; if overflow occurs, returns the low-order bits
     * @throws IllegalArgumentException if {@code k < 0}
     * @see #powExact(int, int)
     * @see #saturatedPow(int, int)
     */
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
     * Returns {@code b} to the {@code k}th power.
     *
     * <p>This method computes long integer exponentiation using an efficient O(log k) algorithm.
     * If the result overflows, the returned value will be equal to the low-order bits of the
     * true result, specifically {@code BigInteger.valueOf(b).pow(k).longValue()}, following
     * standard Java overflow semantics.
     *
     * <p>Special cases are optimized for common bases (0, 1, -1, 2, -2).
     *
     * <p>Examples:
     * <pre>{@code
     * Numbers.pow(2L, 3) = 8L
     * Numbers.pow(3L, 4) = 81L
     * Numbers.pow(5L, 0) = 1L        // any number to power 0 is 1
     * Numbers.pow(0L, 5) = 0L        // 0 to any positive power is 0
     * Numbers.pow(0L, 0) = 1L        // by convention
     * Numbers.pow(-2L, 3) = -8L
     * Numbers.pow(-2L, 4) = 16L
     * Numbers.pow(10L, 18) = 1000000000000000000L
     * Numbers.pow(10L, 19)           // overflows, returns low-order bits
     * }</pre>
     *
     * @param b the base long integer
     * @param k the exponent; must be non-negative
     * @return {@code b} raised to the {@code k}th power; if overflow occurs, returns the low-order bits
     * @throws IllegalArgumentException if {@code k < 0}
     * @see #powExact(long, int)
     * @see #saturatedPow(long, int)
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
     * @param x
     * @return
     * @throws IllegalArgumentException if {@code x <= 0}
     * @throws ArithmeticException of the next-higher power of two is not representable as a
     *         {@code long}, i.e., when {@code x > 2^62}
     */
    public static long ceilingPowerOfTwo(final long x) {
        checkPositive("x", x);
        if (x > MAX_SIGNED_POWER_OF_TWO) {
            throw new ArithmeticException("ceilingPowerOfTwo(" + x + ") is not representable as a long");
        }
        return 1L << -Long.numberOfLeadingZeros(x - 1);
    }

    /**
     * Returns the smallest power of two greater than or equal to the given BigInteger value.
     *
     * <p>This method computes the ceiling of x to the nearest power of two. For example, if x is 100,
     * the result will be 128 (2^7). This is equivalent to 2^(ceiling(log2(x))).</p>
     *
     * <p>Example usage:</p>
     * <pre>{@code
     * BigInteger result1 = Numbers.ceilingPowerOfTwo(BigInteger.valueOf(100));  // returns 128
     * BigInteger result2 = Numbers.ceilingPowerOfTwo(BigInteger.valueOf(128));  // returns 128
     * BigInteger result3 = Numbers.ceilingPowerOfTwo(BigInteger.valueOf(129));  // returns 256
     * BigInteger result4 = Numbers.ceilingPowerOfTwo(BigInteger.ONE);           // returns 1
     * }</pre>
     *
     * @param x the BigInteger value (must be positive)
     * @return the smallest power of two greater than or equal to x
     * @throws IllegalArgumentException if x is not positive
     * @see #ceilingPowerOfTwo(long)
     * @see #floorPowerOfTwo(BigInteger)
     */
    public static BigInteger ceilingPowerOfTwo(final BigInteger x) {
        return BigInteger.ZERO.setBit(log2(x, RoundingMode.CEILING));
    }

    /**
     * Returns the largest power of two less than or equal to {@code x}.  This is equivalent to
     * {@code checkedPow(2, log2(x, FLOOR))}.
     *
     * @param x
     * @return
     * @throws IllegalArgumentException if {@code x <= 0}
     */
    public static long floorPowerOfTwo(final long x) {
        checkPositive("x", x);

        // Long.highestOneBit was buggy on GWT.  We've fixed it, but I'm not certain when the fix will
        // be released.
        return 1L << ((Long.SIZE - 1) - Long.numberOfLeadingZeros(x));
    }

    /**
     * Returns the largest power of two less than or equal to the given BigInteger value.
     *
     * <p>This method computes the floor of x to the nearest power of two. For example, if x is 100,
     * the result will be 64 (2^6). This is equivalent to 2^(floor(log2(x))).</p>
     *
     * <p>Example usage:</p>
     * <pre>{@code
     * BigInteger result1 = Numbers.floorPowerOfTwo(BigInteger.valueOf(100));  // returns 64
     * BigInteger result2 = Numbers.floorPowerOfTwo(BigInteger.valueOf(128));  // returns 128
     * BigInteger result3 = Numbers.floorPowerOfTwo(BigInteger.valueOf(129));  // returns 128
     * BigInteger result4 = Numbers.floorPowerOfTwo(BigInteger.ONE);           // returns 1
     * }</pre>
     *
     * @param x the BigInteger value (must be positive)
     * @return the largest power of two less than or equal to x
     * @throws IllegalArgumentException if x is not positive
     * @see #floorPowerOfTwo(long)
     * @see #ceilingPowerOfTwo(BigInteger)
     */
    public static BigInteger floorPowerOfTwo(final BigInteger x) {
        return BigInteger.ZERO.setBit(log2(x, RoundingMode.FLOOR));
    }

    /**
     * Returns the square root of {@code x}, rounded with the specified rounding mode.
     *
     * <p>This method computes the integer square root of a non-negative integer value,
     * applying the specified rounding mode to handle non-perfect squares.
     *
     * <p>Examples with different rounding modes:
     * <pre>{@code
     * Numbers.sqrt(9, RoundingMode.DOWN) = 3        // perfect square
     * Numbers.sqrt(10, RoundingMode.DOWN) = 3       // rounds toward zero
     * Numbers.sqrt(10, RoundingMode.UP) = 4         // rounds away from zero
     * Numbers.sqrt(10, RoundingMode.FLOOR) = 3      // rounds toward negative infinity
     * Numbers.sqrt(10, RoundingMode.CEILING) = 4    // rounds toward positive infinity
     * Numbers.sqrt(10, RoundingMode.HALF_UP) = 3    // rounds to nearest, ties away from zero
     * Numbers.sqrt(11, RoundingMode.HALF_UP) = 3    // 11 is closer to 9 than 16
     * Numbers.sqrt(16, RoundingMode.UNNECESSARY) = 4  // exact square root required
     * }</pre>
     *
     * @param x the value to compute the square root of; must be non-negative
     * @param mode the rounding mode to apply
     * @return the integer square root of {@code x}, rounded according to the specified mode
     * @throws IllegalArgumentException if {@code x < 0}
     * @throws ArithmeticException if {@code mode} is {@link RoundingMode#UNNECESSARY} and
     *         {@code sqrt(x)} is not an integer (i.e., {@code x} is not a perfect square)
     * @see RoundingMode
     * @see #sqrt(long, RoundingMode)
     */
    @SuppressFBWarnings("SF_SWITCH_FALLTHROUGH")
    @SuppressWarnings("fallthrough")
    public static int sqrt(final int x, final RoundingMode mode) {
        checkNonNegative("x", x);
        final int sqrtFloor = sqrtFloor(x);
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
                final int halfSquare = sqrtFloor * sqrtFloor + sqrtFloor;
                /*
                 * We wish to test whether x <= (sqrtFloor + 0.5)^2 = halfSquare + 0.25. Since both
                 * x and halfSquare are integers, this is equivalent to testing whether x <=
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

    /**
     *
     * @param x
     * @return
     */
    private static int sqrtFloor(final int x) {
        // There is no loss of precision in converting an int to a double, according to
        // http://java.sun.com/docs/books/jls/third_edition/html/conversions.html#5.1.2
        return (int) Math.sqrt(x);
    }

    /**
     * Returns the square root of {@code x}, rounded with the specified rounding mode.
     *
     * <p>This method computes the integer square root of a non-negative long value,
     * applying the specified rounding mode to handle non-perfect squares.
     *
     * <p>Examples with different rounding modes:
     * <pre>{@code
     * Numbers.sqrt(9L, RoundingMode.DOWN) = 3L        // perfect square
     * Numbers.sqrt(10L, RoundingMode.DOWN) = 3L       // rounds toward zero
     * Numbers.sqrt(10L, RoundingMode.UP) = 4L         // rounds away from zero
     * Numbers.sqrt(10L, RoundingMode.FLOOR) = 3L      // rounds toward negative infinity
     * Numbers.sqrt(10L, RoundingMode.CEILING) = 4L    // rounds toward positive infinity
     * Numbers.sqrt(10L, RoundingMode.HALF_UP) = 3L    // rounds to nearest, ties away from zero
     * Numbers.sqrt(100000000000L, RoundingMode.DOWN) = 316227L
     * Numbers.sqrt(100000000000L, RoundingMode.UP) = 316228L
     * Numbers.sqrt(10000000000L, RoundingMode.UNNECESSARY) = 100000L  // exact square root
     * }</pre>
     *
     * @param x the value to compute the square root of; must be non-negative
     * @param mode the rounding mode to apply
     * @return the integer square root of {@code x}, rounded according to the specified mode
     * @throws IllegalArgumentException if {@code x < 0}
     * @throws ArithmeticException if {@code mode} is {@link RoundingMode#UNNECESSARY} and
     *         {@code sqrt(x)} is not an integer (i.e., {@code x} is not a perfect square)
     * @see RoundingMode
     * @see #sqrt(int, RoundingMode)
     */
    public static long sqrt(final long x, final RoundingMode mode) {
        checkNonNegative("x", x);

        if (fitsInInt(x)) {
            return sqrt((int) x, mode);
        }
        /*
         * Let k be the {@code true} value of floor(sqrt(x)), so that
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
        final long guess = (long) Math.sqrt(x);
        // Note: guess is always <= FLOOR_SQRT_MAX_LONG.
        final long guessSquared = guess * guess;
        // Note (2013-2-26): benchmarks indicate that, inscrutably enough, using if statements are faster here than using lessThanBranchFree.
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
                final long sqrtFloor = guess - ((x < guessSquared) ? 1 : 0);
                final long halfSquare = sqrtFloor * sqrtFloor + sqrtFloor;
                /*
                 * We wish to test whether x <= (sqrtFloor + 0.5)^2 = halfSquare + 0.25. Since both x
                 * and halfSquare are integers, this is equivalent to testing whether x <=
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

    /**
     * Returns the square root of {@code x}, rounded with the specified rounding mode.
     *
     * <p>This method computes the integer square root of a non-negative BigInteger value,
     * applying the specified rounding mode to handle non-perfect squares. This method supports
     * arbitrary-precision arithmetic for very large values.
     *
     * <p>Examples with different rounding modes:
     * <pre>{@code
     * BigInteger nine = BigInteger.valueOf(9);
     * BigInteger ten = BigInteger.valueOf(10);
     * Numbers.sqrt(nine, RoundingMode.DOWN) = 3        // perfect square
     * Numbers.sqrt(ten, RoundingMode.DOWN) = 3         // rounds toward zero
     * Numbers.sqrt(ten, RoundingMode.UP) = 4           // rounds away from zero
     * Numbers.sqrt(ten, RoundingMode.FLOOR) = 3        // rounds toward negative infinity
     * Numbers.sqrt(ten, RoundingMode.CEILING) = 4      // rounds toward positive infinity
     * Numbers.sqrt(ten, RoundingMode.HALF_UP) = 3      // rounds to nearest, ties away from zero
     *
     * // Large value example
     * BigInteger large = new BigInteger("123456789012345678901234567890");
     * Numbers.sqrt(large, RoundingMode.DOWN) // computes sqrt with arbitrary precision
     * }</pre>
     *
     * @param x the value to compute the square root of; must be non-negative
     * @param mode the rounding mode to apply
     * @return the integer square root of {@code x}, rounded according to the specified mode
     * @throws IllegalArgumentException if {@code x} is negative
     * @throws ArithmeticException if {@code mode} is {@link RoundingMode#UNNECESSARY} and
     *         {@code sqrt(x)} is not an integer (i.e., {@code x} is not a perfect square)
     * @see RoundingMode
     * @see #sqrt(int, RoundingMode)
     * @see #sqrt(long, RoundingMode)
     */
    @SuppressFBWarnings("SF_SWITCH_FALLTHROUGH")
    @SuppressWarnings("fallthrough")
    public static BigInteger sqrt(final BigInteger x, final RoundingMode mode) {
        checkNonNegative("x", x);
        if (fitsInLong(x)) {
            return BigInteger.valueOf(sqrt(x.longValue(), mode));
        }
        final BigInteger sqrtFloor = sqrtFloor(x);
        switch (mode) {
            case UNNECESSARY:
                checkRoundingUnnecessary(sqrtFloor.pow(2).equals(x)); // fall through
            case FLOOR:
            case DOWN:
                return sqrtFloor;
            case CEILING:
            case UP:
                final int sqrtFloorInt = sqrtFloor.intValue();
                final boolean sqrtFloorIsExact = (sqrtFloorInt * sqrtFloorInt == x.intValue()) // fast check mod 2^32
                        && sqrtFloor.pow(2).equals(x); // slow exact check
                return sqrtFloorIsExact ? sqrtFloor : sqrtFloor.add(BigInteger.ONE);
            case HALF_DOWN:
            case HALF_UP:
            case HALF_EVEN:
                final BigInteger halfSquare = sqrtFloor.pow(2).add(sqrtFloor);
                /*
                 * We wish to test whether x <= (sqrtFloor + 0.5)^2 = halfSquare + 0.25. Since both x
                 * and halfSquare are integers, this is equivalent to testing whether x <=
                 * halfSquare.
                 */
                return (halfSquare.compareTo(x) >= 0) ? sqrtFloor : sqrtFloor.add(BigInteger.ONE);
            default:
                throw new AssertionError();
        }
    }

    /**
     *
     * @param x
     * @return
     */
    private static BigInteger sqrtFloor(final BigInteger x) {
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
         * {@code true} value. Therefore, we perform at least one Newton iteration to get a guess that's
         * definitely >= floor(sqrt(x)), and then continue the iteration until we reach a fixed point.
         */
        BigInteger sqrt0;
        final int log2 = log2(x, FLOOR);
        if (log2 < Double.MAX_EXPONENT) {
            sqrt0 = sqrtApproxWithDoubles(x);
        } else {
            final int shift = (log2 - SIGNIFICAND_BITS) & ~1; // even!
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

    /**
     * Sqrt approx with doubles.
     *
     * @param x
     * @return
     */
    private static BigInteger sqrtApproxWithDoubles(final BigInteger x) {
        return roundToBigInteger(Math.sqrt(bigToDouble(x)), HALF_EVEN);
    }

    /**
     * Returns the result of dividing {@code p} by {@code q}, rounding using the specified {@code RoundingMode}.
     *
     * <p>This method provides precise control over rounding behavior for integer division, supporting
     * all standard rounding modes defined in {@link RoundingMode}.
     *
     * <p>Examples with different rounding modes:
     * <pre>{@code
     * Numbers.divide(7, 3, RoundingMode.DOWN) = 2       // rounds toward zero
     * Numbers.divide(7, 3, RoundingMode.UP) = 3         // rounds away from zero
     * Numbers.divide(7, 3, RoundingMode.FLOOR) = 2      // rounds toward negative infinity
     * Numbers.divide(-7, 3, RoundingMode.FLOOR) = -3    // rounds toward negative infinity
     * Numbers.divide(7, 3, RoundingMode.CEILING) = 3    // rounds toward positive infinity
     * Numbers.divide(7, 2, RoundingMode.HALF_UP) = 4    // rounds to nearest, ties away from zero
     * Numbers.divide(8, 3, RoundingMode.HALF_EVEN) = 3  // rounds to nearest, ties to even
     * Numbers.divide(9, 3, RoundingMode.UNNECESSARY) = 3  // exact division required
     * }</pre>
     *
     * @param p the dividend
     * @param q the divisor
     * @param mode the rounding mode to apply
     * @return the result of {@code p / q} rounded according to the specified mode
     * @throws IllegalArgumentException if {@code mode} is {@code null}
     * @throws ArithmeticException if {@code q == 0}, or if {@code mode == UNNECESSARY} and {@code p}
     *         is not an integer multiple of {@code q}
     * @see RoundingMode
     * @see #divide(long, long, RoundingMode)
     */
    @SuppressFBWarnings("SF_SWITCH_FALLTHROUGH")
    public static int divide(final int p, final int q, final RoundingMode mode) throws IllegalArgumentException {
        N.checkArgNotNull(mode);
        if (q == 0) {
            throw new ArithmeticException("/ by zero"); // for GWT
        }
        final int div = p / q;
        final int rem = p - q * div; // equal to p % q

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
        final int signum = 1 | ((p ^ q) >> (Integer.SIZE - 1));
        boolean increment;
        switch (mode) {
            case UNNECESSARY:
                //noinspection ConstantValue,DataFlowIssue
                checkRoundingUnnecessary(rem == 0);
                //$FALL-THROUGH$
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
                final int absRem = abs(rem);
                final int cmpRemToHalfDivisor = absRem - (abs(q) - absRem);
                // subtracting two nonnegative ints can't overflow
                // cmpRemToHalfDivisor has the same sign as compare(abs(rem), abs(q) / 2).
                if (cmpRemToHalfDivisor == 0) { // exactly on the half mark
                    increment = (mode == HALF_UP || (mode == HALF_EVEN && (div & 1) != 0));
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
     * Returns the result of dividing {@code p} by {@code q}, rounding using the specified {@code RoundingMode}.
     *
     * <p>This method provides precise control over rounding behavior for long integer division, supporting
     * all standard rounding modes defined in {@link RoundingMode}.
     *
     * <p>Examples with different rounding modes:
     * <pre>{@code
     * Numbers.divide(7L, 3L, RoundingMode.DOWN) = 2L       // rounds toward zero
     * Numbers.divide(7L, 3L, RoundingMode.UP) = 3L         // rounds away from zero
     * Numbers.divide(7L, 3L, RoundingMode.FLOOR) = 2L      // rounds toward negative infinity
     * Numbers.divide(-7L, 3L, RoundingMode.FLOOR) = -3L    // rounds toward negative infinity
     * Numbers.divide(7L, 3L, RoundingMode.CEILING) = 3L    // rounds toward positive infinity
     * Numbers.divide(7L, 2L, RoundingMode.HALF_UP) = 4L    // rounds to nearest, ties away from zero
     * Numbers.divide(8L, 3L, RoundingMode.HALF_EVEN) = 3L  // rounds to nearest, ties to even
     * Numbers.divide(9L, 3L, RoundingMode.UNNECESSARY) = 3L  // exact division required
     * }</pre>
     *
     * @param p the dividend
     * @param q the divisor
     * @param mode the rounding mode to apply
     * @return the result of {@code p / q} rounded according to the specified mode
     * @throws IllegalArgumentException if {@code mode} is {@code null}
     * @throws ArithmeticException if {@code q == 0}, or if {@code mode == UNNECESSARY} and {@code p}
     *         is not an integer multiple of {@code q}
     * @see RoundingMode
     * @see #divide(int, int, RoundingMode)
     */
    @SuppressFBWarnings("SF_SWITCH_FALLTHROUGH")
    public static long divide(final long p, final long q, final RoundingMode mode) throws IllegalArgumentException {
        N.checkArgNotNull(mode);
        final long div = p / q; // throws if q == 0
        final long rem = p - q * div; // equals p % q

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
        final int signum = 1 | (int) ((p ^ q) >> (Long.SIZE - 1));
        boolean increment;
        switch (mode) {
            case UNNECESSARY:
                //noinspection ConstantValue,DataFlowIssue
                checkRoundingUnnecessary(rem == 0);
                //$FALL-THROUGH$
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
                final long absRem = abs(rem);
                final long cmpRemToHalfDivisor = absRem - (abs(q) - absRem);
                // subtracting two nonnegative longs can't overflow
                // cmpRemToHalfDivisor has the same sign as compare(abs(rem), abs(q) / 2).
                if (cmpRemToHalfDivisor == 0) { // exactly on the half mark
                    increment = (mode == HALF_UP || (mode == HALF_EVEN && (div & 1) != 0)); //NOSONAR
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
     * Returns the result of dividing {@code p} by {@code q}, rounding using the specified {@code RoundingMode}.
     *
     * <p>This method provides precise control over rounding behavior for arbitrary-precision integer division,
     * supporting all standard rounding modes defined in {@link RoundingMode}.
     *
     * <p>Examples with different rounding modes:
     * <pre>{@code
     * BigInteger seven = BigInteger.valueOf(7);
     * BigInteger three = BigInteger.valueOf(3);
     * Numbers.divide(seven, three, RoundingMode.DOWN) = 2       // rounds toward zero
     * Numbers.divide(seven, three, RoundingMode.UP) = 3         // rounds away from zero
     * Numbers.divide(seven, three, RoundingMode.FLOOR) = 2      // rounds toward negative infinity
     * Numbers.divide(seven, three, RoundingMode.CEILING) = 3    // rounds toward positive infinity
     * Numbers.divide(seven, BigInteger.valueOf(2), RoundingMode.HALF_UP) = 4    // rounds to nearest
     * Numbers.divide(BigInteger.valueOf(9), three, RoundingMode.UNNECESSARY) = 3  // exact division
     * }</pre>
     *
     * @param p the dividend
     * @param q the divisor
     * @param mode the rounding mode to apply
     * @return the result of {@code p / q} rounded according to the specified mode as a BigInteger
     * @throws ArithmeticException if {@code q} is zero, or if {@code mode == UNNECESSARY} and {@code p}
     *         is not an integer multiple of {@code q}
     * @see RoundingMode
     * @see #divide(int, int, RoundingMode)
     * @see #divide(long, long, RoundingMode)
     */
    public static BigInteger divide(final BigInteger p, final BigInteger q, final RoundingMode mode) throws ArithmeticException {
        final BigDecimal pDec = new BigDecimal(p);
        final BigDecimal qDec = new BigDecimal(q);
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
     * @param x
     * @param m
     * @return
     * @throws ArithmeticException if {@code m <= 0}
     * @see <a href="http://docs.oracle.com/javase/specs/jls/se7/html/jls-15.html#jls-15.17.3">
     *      Remainder Operator</a>
     */
    public static int mod(final int x, final int m) {
        if (m <= 0) {
            throw new ArithmeticException("Modulus " + m + " must be > 0");
        }
        final int result = x % m;
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
     * @param x
     * @param m
     * @return
     * @throws ArithmeticException if {@code m <= 0}
     * @see <a href="http://docs.oracle.com/javase/specs/jls/se7/html/jls-15.html#jls-15.17.3">
     *     Remainder Operator</a>
     */
    public static int mod(final long x, final int m) {
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
     * @param x
     * @param m
     * @return
     * @throws ArithmeticException if {@code m <= 0}
     * @see <a href="http://docs.oracle.com/javase/specs/jls/se7/html/jls-15.html#jls-15.17.3">
     *     Remainder Operator</a>
     */
    public static long mod(final long x, final long m) {
        if (m <= 0) {
            throw new ArithmeticException("Modulus must be positive");
        }
        final long result = x % m;
        return (result >= 0) ? result : result + m;
    }

    /**
     * Returns the greatest common divisor (GCD) of two integers using the binary GCD algorithm.
     * The GCD is the largest positive integer that divides both numbers without a remainder.
     *
     * <p>This implementation uses the binary GCD algorithm (also known as Stein's algorithm),
     * which is over 40% faster than the Euclidean algorithm. The method handles negative numbers
     * by taking their absolute values before computing the GCD.
     *
     * <p>Examples:
     * <pre>{@code
     * Numbers.gcd(12, 8) = 4
     * Numbers.gcd(17, 19) = 1  // coprime numbers
     * Numbers.gcd(0, 5) = 5
     * Numbers.gcd(0, 0) = 0
     * Numbers.gcd(-12, 8) = 4  // handles negatives
     * }</pre>
     *
     * @param a the first integer
     * @param b the second integer
     * @return the greatest common divisor of {@code a} and {@code b}; returns {@code 0} if both are zero
     * @throws ArithmeticException if {@code (a == 0 && b == Integer.MIN_VALUE) || (b == 0 && a == Integer.MIN_VALUE)}
     *         because the GCD would be 2^31 which cannot be represented as a positive int
     * @see #lcm(int, int)
     */
    public static int gcd(int a, int b) throws ArithmeticException {
        //    /*
        //     * The reason we require both arguments to be >= 0 is because otherwise, what do you return on
        //     * gcd(0, Integer.MIN_VALUE)? BigInteger.gcd would return positive 2^31, but positive 2^31
        //     * isn't an int.
        //     */
        //    checkNonNegative("a", a);
        //    checkNonNegative("b", b);

        if ((a == 0 && b == Integer.MIN_VALUE) || (b == 0 && a == Integer.MIN_VALUE)) {
            throw new ArithmeticException("overflow by 2^31");
        }

        if (a == Integer.MIN_VALUE) {
            a = Math.abs(a / 2);
        }

        if (b == Integer.MIN_VALUE) {
            b = Math.abs(b / 2);
        }

        a = abs(a);
        b = abs(b);

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
        final int aTwos = Integer.numberOfTrailingZeros(a);
        a >>= aTwos; // divide out all 2s
        final int bTwos = Integer.numberOfTrailingZeros(b);
        b >>= bTwos; // divide out all 2s
        while (a != b) { // both a, b are odd
            // The key to the binary GCD algorithm is as follows:
            // Both a and b are odd.  Assume a > b; then gcd(a - b, b) = gcd(a, b).
            // But in gcd(a - b, b), a - b is even and b is odd, so we can divide out powers of two.

            // We bend over backwards to avoid branching, adapting a technique from
            // http://graphics.stanford.edu/~seander/bithacks.html#IntegerMinOrMax

            final int delta = a - b; // can't overflow, since a and b are nonnegative

            final int minDeltaOrZero = delta & (delta >> (Integer.SIZE - 1));
            // equivalent to Math.min(delta, 0)

            a = delta - minDeltaOrZero - minDeltaOrZero; // sets a to Math.abs(a - b)
            // a is now nonnegative and even

            b += minDeltaOrZero; // sets b to min(old a, b)
            a >>= Integer.numberOfTrailingZeros(a); // divide out all 2s, since 2 doesn't divide b
        }
        return a << min(aTwos, bTwos);
    }

    /**
     * Returns the greatest common divisor (GCD) of two long integers using the binary GCD algorithm.
     * The GCD is the largest positive integer that divides both numbers without a remainder.
     *
     * <p>This implementation uses the binary GCD algorithm (also known as Stein's algorithm),
     * which is over 40% faster than the Euclidean algorithm. The method handles negative numbers
     * by taking their absolute values before computing the GCD.
     *
     * <p>Examples:
     * <pre>{@code
     * Numbers.gcd(12L, 8L) = 4L
     * Numbers.gcd(17L, 19L) = 1L  // coprime numbers
     * Numbers.gcd(0L, 5L) = 5L
     * Numbers.gcd(0L, 0L) = 0L
     * Numbers.gcd(-12L, 8L) = 4L  // handles negatives
     * Numbers.gcd(1000000000000L, 500000000000L) = 500000000000L
     * }</pre>
     *
     * @param a the first long integer
     * @param b the second long integer
     * @return the greatest common divisor of {@code a} and {@code b}; returns {@code 0} if both are zero
     * @throws ArithmeticException if {@code (a == 0 && b == Long.MIN_VALUE) || (b == 0 && a == Long.MIN_VALUE)}
     *         because the GCD would be 2^63 which cannot be represented as a positive long
     * @see #lcm(long, long)
     */
    public static long gcd(long a, long b) throws ArithmeticException {
        //    /*
        //     * The reason we require both arguments to be >= 0 is because otherwise, what do you return on
        //     * gcd(0, Long.MIN_VALUE)? BigInteger.gcd would return positive 2^63, but positive 2^63 isn't an
        //     * int.
        //     */
        //    checkNonNegative("a", a);
        //    checkNonNegative("b", b);

        if ((a == 0 && b == Long.MIN_VALUE) || (b == 0 && a == Long.MIN_VALUE)) {
            throw new ArithmeticException("overflow by 2^63");
        }

        if (a == Long.MIN_VALUE) {
            a = Math.abs(a / 2);
        }

        if (b == Long.MIN_VALUE) {
            b = Math.abs(b / 2);
        }

        a = abs(a);
        b = abs(b);

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
        final int aTwos = Long.numberOfTrailingZeros(a);
        a >>= aTwos; // divide out all 2s
        final int bTwos = Long.numberOfTrailingZeros(b);
        b >>= bTwos; // divide out all 2s
        while (a != b) { // both a, b are odd
            // The key to the binary GCD algorithm is as follows:
            // Both a and b are odd. Assume a > b; then gcd(a - b, b) = gcd(a, b).
            // But in gcd(a - b, b), a - b is even and b is odd, so we can divide out powers of two.

            // We bend over backwards to avoid branching, adapting a technique from
            // http://graphics.stanford.edu/~seander/bithacks.html#IntegerMinOrMax

            final long delta = a - b; // can't overflow, since a and b are nonnegative

            final long minDeltaOrZero = delta & (delta >> (Long.SIZE - 1));
            // equivalent to Math.min(delta, 0)

            a = delta - minDeltaOrZero - minDeltaOrZero; // sets a to Math.abs(a - b)
            // a is now nonnegative and even

            b += minDeltaOrZero; // sets b to min(old a, b)
            a >>= Long.numberOfTrailingZeros(a); // divide out all 2s, since 2 doesn't divide b
        }
        return a << min(aTwos, bTwos);
    }

    /**
     * Returns the least common multiple (LCM) of two integers.
     * The LCM is the smallest positive integer that is divisible by both numbers.
     *
     * <p>This method uses the formula {@code lcm(a,b) = (a / gcd(a,b)) * b}, computing
     * the LCM from the absolute values of the inputs. The method handles negative numbers
     * by taking their absolute values before computing the LCM.
     *
     * <p>Examples:
     * <pre>{@code
     * Numbers.lcm(4, 6) = 12
     * Numbers.lcm(3, 7) = 21  // coprime numbers: lcm equals product
     * Numbers.lcm(0, 5) = 0
     * Numbers.lcm(12, 18) = 36
     * Numbers.lcm(-4, 6) = 12  // handles negatives
     * }</pre>
     *
     * <p>Special cases:
     * <ul>
     * <li>The invocations {@code lcm(Integer.MIN_VALUE, n)} and {@code lcm(n, Integer.MIN_VALUE)},
     * where {@code abs(n)} is a power of 2, throw an {@code ArithmeticException}, because the result
     * would be 2^31, which is too large for an int value.</li>
     * <li>The result of {@code lcm(0, x)} and {@code lcm(x, 0)} is {@code 0} for any {@code x}.</li>
     * </ul>
     *
     * @param a the first integer
     * @param b the second integer
     * @return the least common multiple of the absolute values of {@code a} and {@code b};
     *         returns {@code 0} if either is zero
     * @throws ArithmeticException if the result cannot be represented as a non-negative {@code int} value,
     *         or if the computation would overflow
     * @see #gcd(int, int)
     */
    public static int lcm(final int a, final int b) throws ArithmeticException {
        if (a == 0 || b == 0) {
            return 0;
        }

        final int lcm = abs(multiplyExact(a / gcd(a, b), b));

        if (lcm == Integer.MIN_VALUE) {
            throw new ArithmeticException();
        }

        return lcm;
    }

    /**
     * Returns the least common multiple (LCM) of two long integers.
     * The LCM is the smallest positive integer that is divisible by both numbers.
     *
     * <p>This method uses the formula {@code lcm(a,b) = (a / gcd(a,b)) * b}, computing
     * the LCM from the absolute values of the inputs. The method handles negative numbers
     * by taking their absolute values before computing the LCM.
     *
     * <p>Examples:
     * <pre>{@code
     * Numbers.lcm(4L, 6L) = 12L
     * Numbers.lcm(3L, 7L) = 21L  // coprime numbers: lcm equals product
     * Numbers.lcm(0L, 5L) = 0L
     * Numbers.lcm(12L, 18L) = 36L
     * Numbers.lcm(-4L, 6L) = 12L  // handles negatives
     * Numbers.lcm(100000000000L, 150000000000L) = 300000000000L
     * }</pre>
     *
     * <p>Special cases:
     * <ul>
     * <li>The invocations {@code lcm(Long.MIN_VALUE, n)} and {@code lcm(n, Long.MIN_VALUE)},
     * where {@code abs(n)} is a power of 2, throw an {@code ArithmeticException}, because the result
     * would be 2^63, which is too large for a long value.</li>
     * <li>The result of {@code lcm(0L, x)} and {@code lcm(x, 0L)} is {@code 0L} for any {@code x}.</li>
     * </ul>
     *
     * @param a the first long integer
     * @param b the second long integer
     * @return the least common multiple of the absolute values of {@code a} and {@code b};
     *         returns {@code 0} if either is zero
     * @throws ArithmeticException if the result cannot be represented as a non-negative {@code long} value,
     *         or if the computation would overflow
     * @see #gcd(long, long)
     */
    public static long lcm(final long a, final long b) throws ArithmeticException {
        if (a == 0 || b == 0) {
            return 0;
        }

        final long lcm = abs(multiplyExact(a / gcd(a, b), b));

        if (lcm == Long.MIN_VALUE) {
            throw new ArithmeticException();
        }

        return lcm;
    }

    /**
     * Returns the sum of {@code a} and {@code b}, provided it does not overflow.
     *
     * <p>This method performs addition with overflow checking. If the result would exceed the range
     * of int values, an ArithmeticException is thrown instead of silently wrapping around.</p>
     *
     * <p>Example usage:</p>
     * <pre>{@code
     * int sum1 = Numbers.addExact(100, 200);                    // returns 300
     * int sum2 = Numbers.addExact(Integer.MAX_VALUE, 1);        // throws ArithmeticException
     * int sum3 = Numbers.addExact(Integer.MIN_VALUE, -1);       // throws ArithmeticException
     * }</pre>
     *
     * @param a the first int value to add
     * @param b the second int value to add
     * @return the sum of a and b
     * @throws ArithmeticException if {@code a + b} overflows in signed {@code int} arithmetic
     * @see #addExact(long, long)
     * @see #saturatedAdd(int, int)
     */
    public static int addExact(final int a, final int b) {
        final long result = (long) a + b;
        checkNoOverflow(result == (int) result);
        return (int) result;
    }

    /**
     * Returns the sum of {@code a} and {@code b}, provided it does not overflow.
     *
     * <p>This method performs addition with overflow checking. If the result would exceed the range
     * of long values, an ArithmeticException is thrown instead of silently wrapping around.</p>
     *
     * <p>Example usage:</p>
     * <pre>{@code
     * long sum1 = Numbers.addExact(100L, 200L);                 // returns 300L
     * long sum2 = Numbers.addExact(Long.MAX_VALUE, 1L);         // throws ArithmeticException
     * long sum3 = Numbers.addExact(Long.MIN_VALUE, -1L);        // throws ArithmeticException
     * }</pre>
     *
     * @param a the first long value to add
     * @param b the second long value to add
     * @return the sum of a and b
     * @throws ArithmeticException if {@code a + b} overflows in signed {@code long} arithmetic
     * @see #addExact(int, int)
     * @see #saturatedAdd(long, long)
     */
    public static long addExact(final long a, final long b) {
        final long result = a + b;
        checkNoOverflow((a ^ b) < 0 || (a ^ result) >= 0);
        return result;
    }

    /**
     * Returns the difference of {@code a} and {@code b}, provided it does not overflow.
     *
     * <p>This method performs subtraction with overflow checking. If the result would exceed the range
     * of int values, an ArithmeticException is thrown instead of silently wrapping around.</p>
     *
     * <p>Example usage:</p>
     * <pre>{@code
     * int diff1 = Numbers.subtractExact(200, 100);              // returns 100
     * int diff2 = Numbers.subtractExact(Integer.MIN_VALUE, 1);  // throws ArithmeticException
     * int diff3 = Numbers.subtractExact(Integer.MAX_VALUE, -1); // throws ArithmeticException
     * }</pre>
     *
     * @param a the value to subtract from
     * @param b the value to subtract
     * @return the difference of a and b
     * @throws ArithmeticException if {@code a - b} overflows in signed {@code int} arithmetic
     * @see #subtractExact(long, long)
     * @see #saturatedSubtract(int, int)
     */
    public static int subtractExact(final int a, final int b) {
        final long result = (long) a - b;
        checkNoOverflow(result == (int) result);
        return (int) result;
    }

    /**
     * Returns the difference of {@code a} and {@code b}, provided it does not overflow.
     *
     * <p>This method performs subtraction with overflow checking. If the result would exceed the range
     * of long values, an ArithmeticException is thrown instead of silently wrapping around.</p>
     *
     * <p>Example usage:</p>
     * <pre>{@code
     * long diff1 = Numbers.subtractExact(200L, 100L);           // returns 100L
     * long diff2 = Numbers.subtractExact(Long.MIN_VALUE, 1L);   // throws ArithmeticException
     * long diff3 = Numbers.subtractExact(Long.MAX_VALUE, -1L);  // throws ArithmeticException
     * }</pre>
     *
     * @param a the value to subtract from
     * @param b the value to subtract
     * @return the difference of a and b
     * @throws ArithmeticException if {@code a - b} overflows in signed {@code long} arithmetic
     * @see #subtractExact(int, int)
     * @see #saturatedSubtract(long, long)
     */
    public static long subtractExact(final long a, final long b) {
        final long result = a - b;
        checkNoOverflow((a ^ b) >= 0 || (a ^ result) >= 0);
        return result;
    }

    /**
     * Returns the product of {@code a} and {@code b}, provided it does not overflow.
     *
     * <p>This method performs multiplication with overflow checking. If the result would exceed the range
     * of int values, an ArithmeticException is thrown instead of silently wrapping around.</p>
     *
     * <p>Example usage:</p>
     * <pre>{@code
     * int product1 = Numbers.multiplyExact(100, 200);           // returns 20000
     * int product2 = Numbers.multiplyExact(Integer.MAX_VALUE, 2);  // throws ArithmeticException
     * int product3 = Numbers.multiplyExact(100000, 100000);     // throws ArithmeticException
     * }</pre>
     *
     * @param a the first int value to multiply
     * @param b the second int value to multiply
     * @return the product of a and b
     * @throws ArithmeticException if {@code a * b} overflows in signed {@code int} arithmetic
     * @see #multiplyExact(long, long)
     * @see #saturatedMultiply(int, int)
     */
    public static int multiplyExact(final int a, final int b) {
        final long result = (long) a * b;
        checkNoOverflow(result == (int) result);
        return (int) result;
    }

    /**
     * Returns the product of {@code a} and {@code b}, provided it does not overflow.
     *
     * <p>This method performs multiplication with overflow checking. If the result would exceed the range
     * of long values, an ArithmeticException is thrown instead of silently wrapping around.</p>
     *
     * <p>Example usage:</p>
     * <pre>{@code
     * long product1 = Numbers.multiplyExact(100L, 200L);        // returns 20000L
     * long product2 = Numbers.multiplyExact(Long.MAX_VALUE, 2L);   // throws ArithmeticException
     * long product3 = Numbers.multiplyExact(10000000000L, 10000000000L);  // throws ArithmeticException
     * }</pre>
     *
     * @param a the first long value to multiply
     * @param b the second long value to multiply
     * @return the product of a and b
     * @throws ArithmeticException if {@code a * b} overflows in signed {@code long} arithmetic
     * @see #multiplyExact(int, int)
     * @see #saturatedMultiply(long, long)
     */
    public static long multiplyExact(final long a, final long b) {
        // Hacker's Delight, Section 2-12
        final int leadingZeros = Long.numberOfLeadingZeros(a) + Long.numberOfLeadingZeros(~a) + Long.numberOfLeadingZeros(b) + Long.numberOfLeadingZeros(~b);
        /*
         * If leadingZeros > Long.SIZE + 1 it's definitely fine, if it's < Long.SIZE it's definitely
         * bad. We do the leadingZeros check to avoid the division below if at all possible.
         *
         * Otherwise, if b == Long.MIN_VALUE, then the only allowed values are 0 and 1. We take
         * care of all a < 0 with their own check, because in particular, the case a == -1 will
         * incorrectly pass the division check below.
         *
         * In all other cases, we check that either a is 0 or the result is consistent with division.
         */
        if (leadingZeros > Long.SIZE + 1) {
            return a * b;
        }
        checkNoOverflow(leadingZeros >= Long.SIZE);
        checkNoOverflow(a >= 0 || b != Long.MIN_VALUE);
        final long result = a * b;
        checkNoOverflow(a == 0 || result / a == b);
        return result;
    }

    /**
     * Returns {@code b} to the {@code k}th power, throwing an exception if overflow occurs.
     *
     * <p>This method computes integer exponentiation with overflow checking. Unlike {@link #pow(int, int)},
     * which silently allows overflow, this method throws an {@code ArithmeticException} if the result
     * cannot be represented as an {@code int}.
     *
     * <p>Note: {@link #pow(int, int)} may be faster for cases where overflow is acceptable, as it
     * does not perform overflow checking.
     *
     * <p>Examples:
     * <pre>{@code
     * Numbers.powExact(2, 3) = 8
     * Numbers.powExact(3, 4) = 81
     * Numbers.powExact(5, 0) = 1
     * Numbers.powExact(10, 9) = 1000000000
     * Numbers.powExact(10, 10)       // throws ArithmeticException (overflow)
     * Numbers.powExact(2, 31)        // throws ArithmeticException (overflow)
     * Numbers.powExact(-2, 30) = 1073741824
     * Numbers.powExact(-2, 31)       // throws ArithmeticException (overflow)
     * }</pre>
     *
     * @param b the base integer
     * @param k the exponent; must be non-negative
     * @return {@code b} raised to the {@code k}th power
     * @throws IllegalArgumentException if {@code k < 0}
     * @throws ArithmeticException if {@code b} to the {@code k}th power overflows in signed {@code int} arithmetic
     * @see #pow(int, int)
     * @see #saturatedPow(int, int)
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
                        checkNoOverflow(-FLOOR_SQRT_MAX_INT <= b && b <= FLOOR_SQRT_MAX_INT);
                        b *= b;
                    }
            }
        }
    }

    /**
     * Returns {@code b} to the {@code k}th power, throwing an exception if overflow occurs.
     *
     * <p>This method computes long integer exponentiation with overflow checking. Unlike {@link #pow(long, int)},
     * which silently allows overflow, this method throws an {@code ArithmeticException} if the result
     * cannot be represented as a {@code long}.
     *
     * <p>Examples:
     * <pre>{@code
     * Numbers.powExact(2L, 3) = 8L
     * Numbers.powExact(3L, 4) = 81L
     * Numbers.powExact(5L, 0) = 1L
     * Numbers.powExact(10L, 18) = 1000000000000000000L
     * Numbers.powExact(10L, 19)      // throws ArithmeticException (overflow)
     * Numbers.powExact(2L, 63)       // throws ArithmeticException (overflow)
     * Numbers.powExact(-2L, 62) = 4611686018427387904L
     * Numbers.powExact(-2L, 63)      // throws ArithmeticException (overflow)
     * }</pre>
     *
     * @param b the base long integer
     * @param k the exponent; must be non-negative
     * @return {@code b} raised to the {@code k}th power
     * @throws IllegalArgumentException if {@code k < 0}
     * @throws ArithmeticException if {@code b} to the {@code k}th power overflows in signed {@code long} arithmetic
     * @see #pow(long, int)
     * @see #saturatedPow(long, int)
     */
    public static long powExact(long b, int k) {
        checkNonNegative("exponent", k);
        if (b >= -2 && b <= 2) {
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
     * Returns the sum of {@code a} and {@code b}, saturating at the integer bounds instead of overflowing.
     *
     * <p>This method performs addition with saturation arithmetic. If the true sum would exceed
     * {@code Integer.MAX_VALUE}, the method returns {@code Integer.MAX_VALUE}. If the true sum
     * would be less than {@code Integer.MIN_VALUE}, the method returns {@code Integer.MIN_VALUE}.
     *
     * <p>Examples demonstrating saturation:
     * <pre>{@code
     * Numbers.saturatedAdd(100, 200) = 300
     * Numbers.saturatedAdd(Integer.MAX_VALUE, 1) = Integer.MAX_VALUE  // saturates at max
     * Numbers.saturatedAdd(Integer.MAX_VALUE, 100) = Integer.MAX_VALUE  // saturates at max
     * Numbers.saturatedAdd(Integer.MIN_VALUE, -1) = Integer.MIN_VALUE  // saturates at min
     * Numbers.saturatedAdd(Integer.MIN_VALUE, -100) = Integer.MIN_VALUE  // saturates at min
     * Numbers.saturatedAdd(1000000000, 1000000000) = 2000000000
     * Numbers.saturatedAdd(2000000000, 2000000000) = Integer.MAX_VALUE  // saturates
     * }</pre>
     *
     * @param a the first integer
     * @param b the second integer
     * @return the sum of {@code a} and {@code b}, or the appropriate bound if overflow would occur
     * @see #saturatedAdd(long, long)
     * @see #addExact(int, int)
     */
    public static int saturatedAdd(final int a, final int b) {
        return saturatedCast((long) a + b);
    }

    /**
     * Returns the sum of {@code a} and {@code b}, saturating at the long bounds instead of overflowing.
     *
     * <p>This method performs addition with saturation arithmetic. If the true sum would exceed
     * {@code Long.MAX_VALUE}, the method returns {@code Long.MAX_VALUE}. If the true sum
     * would be less than {@code Long.MIN_VALUE}, the method returns {@code Long.MIN_VALUE}.
     *
     * <p>Examples demonstrating saturation:
     * <pre>{@code
     * Numbers.saturatedAdd(100L, 200L) = 300L
     * Numbers.saturatedAdd(Long.MAX_VALUE, 1L) = Long.MAX_VALUE  // saturates at max
     * Numbers.saturatedAdd(Long.MAX_VALUE, 100L) = Long.MAX_VALUE  // saturates at max
     * Numbers.saturatedAdd(Long.MIN_VALUE, -1L) = Long.MIN_VALUE  // saturates at min
     * Numbers.saturatedAdd(Long.MIN_VALUE, -100L) = Long.MIN_VALUE  // saturates at min
     * }</pre>
     *
     * @param a the first long integer
     * @param b the second long integer
     * @return the sum of {@code a} and {@code b}, or the appropriate bound if overflow would occur
     * @see #saturatedAdd(int, int)
     * @see #addExact(long, long)
     */
    public static long saturatedAdd(final long a, final long b) {
        final long naiveSum = a + b;
        if ((a ^ b) < 0 || (a ^ naiveSum) >= 0) {
            // If a and b have different signs or a has the same sign as the result then there was no
            // overflow, return.
            return naiveSum;
        }
        // we did over/under flow, if the sign is negative we should return MAX otherwise MIN
        return Long.MAX_VALUE + ((naiveSum >>> (Long.SIZE - 1)) ^ 1);
    }

    /**
     * Returns the difference of {@code a} and {@code b}, saturating at the integer bounds instead of overflowing.
     *
     * <p>This method performs subtraction with saturation arithmetic. If the true difference would exceed
     * {@code Integer.MAX_VALUE}, the method returns {@code Integer.MAX_VALUE}. If the true difference
     * would be less than {@code Integer.MIN_VALUE}, the method returns {@code Integer.MIN_VALUE}.
     *
     * <p>Examples demonstrating saturation:
     * <pre>{@code
     * Numbers.saturatedSubtract(200, 100) = 100
     * Numbers.saturatedSubtract(Integer.MAX_VALUE, -1) = Integer.MAX_VALUE  // saturates at max
     * Numbers.saturatedSubtract(Integer.MAX_VALUE, -100) = Integer.MAX_VALUE  // saturates at max
     * Numbers.saturatedSubtract(Integer.MIN_VALUE, 1) = Integer.MIN_VALUE  // saturates at min
     * Numbers.saturatedSubtract(Integer.MIN_VALUE, 100) = Integer.MIN_VALUE  // saturates at min
     * Numbers.saturatedSubtract(-1000000000, 1500000000) = Integer.MIN_VALUE  // saturates
     * }</pre>
     *
     * @param a the minuend
     * @param b the subtrahend
     * @return the difference {@code a - b}, or the appropriate bound if overflow would occur
     * @see #saturatedSubtract(long, long)
     * @see #subtractExact(int, int)
     */
    public static int saturatedSubtract(final int a, final int b) {
        return saturatedCast((long) a - b);
    }

    /**
     * Returns the difference of {@code a} and {@code b}, saturating at the long bounds instead of overflowing.
     *
     * <p>This method performs subtraction with saturation arithmetic. If the true difference would exceed
     * {@code Long.MAX_VALUE}, the method returns {@code Long.MAX_VALUE}. If the true difference
     * would be less than {@code Long.MIN_VALUE}, the method returns {@code Long.MIN_VALUE}.
     *
     * <p>Examples demonstrating saturation:
     * <pre>{@code
     * Numbers.saturatedSubtract(200L, 100L) = 100L
     * Numbers.saturatedSubtract(Long.MAX_VALUE, -1L) = Long.MAX_VALUE  // saturates at max
     * Numbers.saturatedSubtract(Long.MAX_VALUE, -100L) = Long.MAX_VALUE  // saturates at max
     * Numbers.saturatedSubtract(Long.MIN_VALUE, 1L) = Long.MIN_VALUE  // saturates at min
     * Numbers.saturatedSubtract(Long.MIN_VALUE, 100L) = Long.MIN_VALUE  // saturates at min
     * }</pre>
     *
     * @param a the minuend
     * @param b the subtrahend
     * @return the difference {@code a - b}, or the appropriate bound if overflow would occur
     * @see #saturatedSubtract(int, int)
     * @see #subtractExact(long, long)
     */
    public static long saturatedSubtract(final long a, final long b) {
        final long naiveDifference = a - b;
        if ((a ^ b) >= 0 || (a ^ naiveDifference) >= 0) {
            // If a and b have the same signs or a has the same sign as the result then there was no
            // overflow, return.
            return naiveDifference;
        }
        // we did over/under flow
        return Long.MAX_VALUE + ((naiveDifference >>> (Long.SIZE - 1)) ^ 1);
    }

    /**
     * Returns the product of {@code a} and {@code b}, saturating at the integer bounds instead of overflowing.
     *
     * <p>This method performs multiplication with saturation arithmetic. If the true product would exceed
     * {@code Integer.MAX_VALUE}, the method returns {@code Integer.MAX_VALUE}. If the true product
     * would be less than {@code Integer.MIN_VALUE}, the method returns {@code Integer.MIN_VALUE}.
     *
     * <p>Examples demonstrating saturation:
     * <pre>{@code
     * Numbers.saturatedMultiply(100, 200) = 20000
     * Numbers.saturatedMultiply(Integer.MAX_VALUE, 2) = Integer.MAX_VALUE  // saturates at max
     * Numbers.saturatedMultiply(100000, 100000) = Integer.MAX_VALUE  // saturates at max
     * Numbers.saturatedMultiply(Integer.MIN_VALUE, 2) = Integer.MIN_VALUE  // saturates at min
     * Numbers.saturatedMultiply(-100000, 100000) = Integer.MIN_VALUE  // saturates at min
     * Numbers.saturatedMultiply(Integer.MAX_VALUE, -1) = -Integer.MAX_VALUE
     * }</pre>
     *
     * @param a the first integer
     * @param b the second integer
     * @return the product {@code a * b}, or the appropriate bound if overflow would occur
     * @see #saturatedMultiply(long, long)
     * @see #multiplyExact(int, int)
     */
    public static int saturatedMultiply(final int a, final int b) {
        return saturatedCast((long) a * b);
    }

    /**
     * Returns the product of {@code a} and {@code b}, saturating at the long bounds instead of overflowing.
     *
     * <p>This method performs multiplication with saturation arithmetic. If the true product would exceed
     * {@code Long.MAX_VALUE}, the method returns {@code Long.MAX_VALUE}. If the true product
     * would be less than {@code Long.MIN_VALUE}, the method returns {@code Long.MIN_VALUE}.
     *
     * <p>Examples demonstrating saturation:
     * <pre>{@code
     * Numbers.saturatedMultiply(100L, 200L) = 20000L
     * Numbers.saturatedMultiply(Long.MAX_VALUE, 2L) = Long.MAX_VALUE  // saturates at max
     * Numbers.saturatedMultiply(10000000000L, 10000000000L) = Long.MAX_VALUE  // saturates at max
     * Numbers.saturatedMultiply(Long.MIN_VALUE, 2L) = Long.MIN_VALUE  // saturates at min
     * Numbers.saturatedMultiply(-10000000000L, 10000000000L) = Long.MIN_VALUE  // saturates at min
     * }</pre>
     *
     * @param a the first long integer
     * @param b the second long integer
     * @return the product {@code a * b}, or the appropriate bound if overflow would occur
     * @see #saturatedMultiply(int, int)
     * @see #multiplyExact(long, long)
     */
    public static long saturatedMultiply(final long a, final long b) {
        // see checkedMultiply for explanation
        final int leadingZeros = Long.numberOfLeadingZeros(a) + Long.numberOfLeadingZeros(~a) + Long.numberOfLeadingZeros(b) + Long.numberOfLeadingZeros(~b);
        if (leadingZeros > Long.SIZE + 1) {
            return a * b;
        }
        // the return value if we will overflow (which we calculate by overflowing a long :) )
        final long limit = Long.MAX_VALUE + ((a ^ b) >>> (Long.SIZE - 1));
        if (leadingZeros < Long.SIZE || (a < 0 && b == Long.MIN_VALUE)) { //NOSONAR
            // overflow
            return limit;
        }
        final long result = a * b;
        if (a == 0 || result / a == b) {
            return result;
        }
        return limit;
    }

    /**
     * Returns {@code b} to the {@code k}th power, saturating at the integer bounds instead of overflowing.
     *
     * <p>This method computes integer exponentiation with saturation arithmetic. If the true result
     * would exceed {@code Integer.MAX_VALUE}, the method returns {@code Integer.MAX_VALUE}. If the
     * true result would be less than {@code Integer.MIN_VALUE}, the method returns {@code Integer.MIN_VALUE}.
     *
     * <p>This is useful when you want to avoid overflow but don't want to throw exceptions or use
     * larger data types.
     *
     * <p>Examples demonstrating saturation:
     * <pre>{@code
     * Numbers.saturatedPow(2, 3) = 8
     * Numbers.saturatedPow(3, 4) = 81
     * Numbers.saturatedPow(10, 9) = 1000000000
     * Numbers.saturatedPow(10, 10) = Integer.MAX_VALUE    // saturates instead of overflowing
     * Numbers.saturatedPow(2, 31) = Integer.MAX_VALUE     // saturates at max value
     * Numbers.saturatedPow(2, 100) = Integer.MAX_VALUE    // saturates at max value
     * Numbers.saturatedPow(-2, 31) = Integer.MIN_VALUE    // saturates at min value (odd exponent)
     * Numbers.saturatedPow(-2, 32) = Integer.MAX_VALUE    // saturates at max value (even exponent)
     * }</pre>
     *
     * @param b the base integer
     * @param k the exponent; must be non-negative
     * @return {@code b} raised to the {@code k}th power, or the appropriate bound if overflow would occur
     * @throws IllegalArgumentException if {@code k < 0}
     * @see #pow(int, int)
     * @see #powExact(int, int)
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
        final int limit = Integer.MAX_VALUE + ((b >>> (Integer.SIZE - 1)) & (k & 1));
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
                        if (-FLOOR_SQRT_MAX_INT > b || b > FLOOR_SQRT_MAX_INT) {
                            return limit;
                        }
                        b *= b;
                    }
            }
        }
    }

    /**
     * Returns {@code b} to the {@code k}th power, saturating at the long bounds instead of overflowing.
     *
     * <p>This method computes long integer exponentiation with saturation arithmetic. If the true result
     * would exceed {@code Long.MAX_VALUE}, the method returns {@code Long.MAX_VALUE}. If the
     * true result would be less than {@code Long.MIN_VALUE}, the method returns {@code Long.MIN_VALUE}.
     *
     * <p>This is useful when you want to avoid overflow but don't want to throw exceptions or use
     * larger data types like BigInteger.
     *
     * <p>Examples demonstrating saturation:
     * <pre>{@code
     * Numbers.saturatedPow(2L, 3) = 8L
     * Numbers.saturatedPow(3L, 4) = 81L
     * Numbers.saturatedPow(10L, 18) = 1000000000000000000L
     * Numbers.saturatedPow(10L, 19) = Long.MAX_VALUE      // saturates instead of overflowing
     * Numbers.saturatedPow(2L, 63) = Long.MAX_VALUE       // saturates at max value
     * Numbers.saturatedPow(2L, 100) = Long.MAX_VALUE      // saturates at max value
     * Numbers.saturatedPow(-2L, 63) = Long.MIN_VALUE      // saturates at min value (odd exponent)
     * Numbers.saturatedPow(-2L, 64) = Long.MAX_VALUE      // saturates at max value (even exponent)
     * }</pre>
     *
     * @param b the base long integer
     * @param k the exponent; must be non-negative
     * @return {@code b} raised to the {@code k}th power, or the appropriate bound if overflow would occur
     * @throws IllegalArgumentException if {@code k < 0}
     * @see #pow(long, int)
     * @see #powExact(long, int)
     */
    public static long saturatedPow(long b, int k) {
        checkNonNegative("exponent", k);
        if (b >= -2 && b <= 2) {
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
        final long limit = Long.MAX_VALUE + ((b >>> Long.SIZE - 1) & (k & 1));
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
                        if (-FLOOR_SQRT_MAX_LONG > b || b > FLOOR_SQRT_MAX_LONG) {
                            return limit;
                        }
                        b *= b;
                    }
            }
        }
    }

    /**
     * Returns the {@code int} value nearest to {@code value}, saturating at the integer bounds.
     *
     * <p>This method casts a {@code long} value to an {@code int} with saturation. If the value
     * exceeds {@code Integer.MAX_VALUE}, the method returns {@code Integer.MAX_VALUE}. If the value
     * is less than {@code Integer.MIN_VALUE}, the method returns {@code Integer.MIN_VALUE}.
     * Otherwise, the method returns the {@code long} value cast to an {@code int}.
     *
     * <p>Examples demonstrating saturation:
     * <pre>{@code
     * Numbers.saturatedCast(100L) = 100
     * Numbers.saturatedCast(2147483647L) = Integer.MAX_VALUE  // exact fit
     * Numbers.saturatedCast(2147483648L) = Integer.MAX_VALUE  // saturates at max
     * Numbers.saturatedCast(10000000000L) = Integer.MAX_VALUE  // saturates at max
     * Numbers.saturatedCast(-2147483648L) = Integer.MIN_VALUE  // exact fit
     * Numbers.saturatedCast(-2147483649L) = Integer.MIN_VALUE  // saturates at min
     * Numbers.saturatedCast(-10000000000L) = Integer.MIN_VALUE  // saturates at min
     * }</pre>
     *
     * @param value any {@code long} value
     * @return the {@code int} value nearest to {@code value}, or {@code Integer.MAX_VALUE} if {@code value}
     *         is too large, or {@code Integer.MIN_VALUE} if {@code value} is too small
     */
    public static int saturatedCast(final long value) {
        if (value > Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        }
        if (value < Integer.MIN_VALUE) {
            return Integer.MIN_VALUE;
        }
        return (int) value;
    }

    /**
     * Returns {@code n!} (n factorial), the product of the first {@code n} positive integers.
     *
     * <p>The factorial function computes {@code n! = 1 * 2 * 3 * ... * n}. By convention,
     * {@code 0! = 1}. If the true result would exceed {@code Integer.MAX_VALUE}, this method
     * returns {@code Integer.MAX_VALUE} instead.
     *
     * <p>The largest value of {@code n} for which {@code n!} fits in an {@code int} is 12.
     *
     * <p>Examples:
     * <pre>{@code
     * Numbers.factorial(0) = 1
     * Numbers.factorial(1) = 1
     * Numbers.factorial(5) = 120
     * Numbers.factorial(10) = 3628800
     * Numbers.factorial(12) = 479001600
     * Numbers.factorial(13) = Integer.MAX_VALUE  // overflow, saturates
     * Numbers.factorial(100) = Integer.MAX_VALUE  // overflow, saturates
     * }</pre>
     *
     * @param n the non-negative integer to compute the factorial of
     * @return {@code n!} if it fits in an {@code int}, otherwise {@code Integer.MAX_VALUE}
     * @throws IllegalArgumentException if {@code n < 0}
     * @see #factorialToLong(int)
     * @see #factorialToBigInteger(int)
     */
    public static int factorial(final int n) {
        checkNonNegative("n", n);
        return (n < int_factorials.length) ? int_factorials[n] : Integer.MAX_VALUE;
    }

    /**
     * Returns {@code n!} (n factorial) as a {@code long}, the product of the first {@code n} positive integers.
     *
     * <p>The factorial function computes {@code n! = 1 * 2 * 3 * ... * n}. By convention,
     * {@code 0! = 1}. If the true result would exceed {@code Long.MAX_VALUE}, this method
     * returns {@code Long.MAX_VALUE} instead.
     *
     * <p>The largest value of {@code n} for which {@code n!} fits in a {@code long} is 20.
     *
     * <p>Examples:
     * <pre>{@code
     * Numbers.factorialToLong(0) = 1L
     * Numbers.factorialToLong(1) = 1L
     * Numbers.factorialToLong(5) = 120L
     * Numbers.factorialToLong(10) = 3628800L
     * Numbers.factorialToLong(15) = 1307674368000L
     * Numbers.factorialToLong(20) = 2432902008176640000L
     * Numbers.factorialToLong(21) = Long.MAX_VALUE  // overflow, saturates
     * Numbers.factorialToLong(100) = Long.MAX_VALUE  // overflow, saturates
     * }</pre>
     *
     * @param n the non-negative integer to compute the factorial of
     * @return {@code n!} if it fits in a {@code long}, otherwise {@code Long.MAX_VALUE}
     * @throws IllegalArgumentException if {@code n < 0}
     * @see #factorial(int)
     * @see #factorialToBigInteger(int)
     */
    public static long factorialToLong(final int n) {
        checkNonNegative("n", n);
        return (n < long_factorials.length) ? long_factorials[n] : Long.MAX_VALUE;
    }

    /**
     * Returns {@code n!}, that is, the product of the first {@code n} positive integers, {@code 1} if
     * {@code n == 0}, or {@code n!}, or {@link Double#POSITIVE_INFINITY} if
     * {@code n! > Double.MAX_VALUE}.
     *
     * <p>The result is within 1 ulp of the {@code true} value.
     *
     * @param n
     * @return
     * @throws IllegalArgumentException if {@code n < 0}
     */
    public static double factorialToDouble(final int n) {
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
     * Returns {@code n!} (n factorial) as a {@code BigInteger}, the product of the first {@code n} positive integers.
     *
     * <p>The factorial function computes {@code n! = 1 * 2 * 3 * ... * n}. By convention,
     * {@code 0! = 1}. This method supports arbitrary-precision computation and can handle
     * very large values of {@code n}.
     *
     * <p><b>Performance Note:</b> This method uses an efficient binary recursive algorithm with
     * balanced multiplies. It removes all factors of 2 from intermediate products and shifts them
     * back in at the end. The result takes <i>O(n log n)</i> space, so use cautiously for very
     * large values of {@code n}.
     *
     * <p>Examples:
     * <pre>{@code
     * Numbers.factorialToBigInteger(0) = 1
     * Numbers.factorialToBigInteger(5) = 120
     * Numbers.factorialToBigInteger(20) = 2432902008176640000
     * Numbers.factorialToBigInteger(100)  // returns a 158-digit number
     * Numbers.factorialToBigInteger(1000) // returns a 2568-digit number (use cautiously)
     * }</pre>
     *
     * @param n the non-negative integer to compute the factorial of
     * @return {@code n!} as a {@code BigInteger}
     * @throws IllegalArgumentException if {@code n < 0}
     * @see #factorial(int)
     * @see #factorialToLong(int)
     */
    public static BigInteger factorialToBigInteger(final int n) {
        checkNonNegative("n", n);

        // If the factorial is small enough, just use LongMath to do it.
        if (n < long_factorials.length) {
            return BigInteger.valueOf(long_factorials[n]);
        }

        // Pre-allocate space for our list of intermediate BigIntegers.
        final int approxSize = divide(n * log2(n, CEILING), Long.SIZE, CEILING);
        final ArrayList<BigInteger> bignums = new ArrayList<>(approxSize);

        // Start from the pre-computed maximum long factorial.
        final int startingNumber = long_factorials.length;
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
            final int tz = Long.numberOfTrailingZeros(num);
            final long normalizedNum = num >> tz;
            shift += tz;
            // Adjust floor(log2(num)) + 1.
            final int normalizedBits = bits - tz;
            // If it doesn't fit in a long, then we store off the intermediate product.
            if (normalizedBits + productBits >= Long.SIZE) {
                bignums.add(BigInteger.valueOf(product));
                product = 1;
                productBits = 0; //NOSONAR
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

    /**
     *
     * @param nums
     * @return
     */
    static BigInteger listProduct(final List<BigInteger> nums) {
        return listProduct(nums, 0, nums.size());
    }

    /**
     *
     * @param nums
     * @param start
     * @param end
     * @return
     */
    static BigInteger listProduct(final List<BigInteger> nums, final int start, final int end) {
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
                final int m = (end + start) >>> 1;
                return listProduct(nums, start, m).multiply(listProduct(nums, m, end));
        }
    }

    /**
     * Returns the binomial coefficient "n choose k", denoted as C(n, k) or (n k).
     *
     * <p>The binomial coefficient represents the number of ways to choose {@code k} items from
     * {@code n} items without regard to order. It is calculated as {@code n! / (k! * (n-k)!)}.
     * If the result would exceed {@code Integer.MAX_VALUE}, this method returns {@code Integer.MAX_VALUE}.
     *
     * <p>Examples:
     * <pre>{@code
     * Numbers.binomial(5, 0) = 1      // only one way to choose nothing
     * Numbers.binomial(5, 1) = 5      // five ways to choose one item
     * Numbers.binomial(5, 2) = 10     // C(5,2) = 5!/(2!*3!) = 10
     * Numbers.binomial(5, 3) = 10     // C(5,3) = C(5,2) by symmetry
     * Numbers.binomial(10, 5) = 252
     * Numbers.binomial(52, 5) = 2598960  // poker hands from a deck
     * Numbers.binomial(100, 50) = Integer.MAX_VALUE  // overflow, saturates
     * }</pre>
     *
     * @param n the total number of items; must be non-negative
     * @param k the number of items to choose; must be non-negative and at most {@code n}
     * @return the binomial coefficient C(n, k) if it fits in an {@code int}, otherwise {@code Integer.MAX_VALUE}
     * @throws IllegalArgumentException if {@code n < 0}, {@code k < 0}, or {@code k > n}
     * @see #binomialToLong(int, int)
     * @see #binomialToBigInteger(int, int)
     */
    public static int binomial(final int n, int k) throws IllegalArgumentException {
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
     * Returns the binomial coefficient "n choose k" as a {@code long}, denoted as C(n, k) or (n k).
     *
     * <p>The binomial coefficient represents the number of ways to choose {@code k} items from
     * {@code n} items without regard to order. It is calculated as {@code n! / (k! * (n-k)!)}.
     * If the result would exceed {@code Long.MAX_VALUE}, this method returns {@code Long.MAX_VALUE}.
     *
     * <p>Examples:
     * <pre>{@code
     * Numbers.binomialToLong(5, 0) = 1L      // only one way to choose nothing
     * Numbers.binomialToLong(5, 1) = 5L      // five ways to choose one item
     * Numbers.binomialToLong(5, 2) = 10L     // C(5,2) = 5!/(2!*3!) = 10
     * Numbers.binomialToLong(10, 5) = 252L
     * Numbers.binomialToLong(52, 5) = 2598960L  // poker hands from a deck
     * Numbers.binomialToLong(60, 30) = 118264581564861424L
     * Numbers.binomialToLong(100, 50) = Long.MAX_VALUE  // overflow, saturates
     * }</pre>
     *
     * @param n the total number of items; must be non-negative
     * @param k the number of items to choose; must be non-negative and at most {@code n}
     * @return the binomial coefficient C(n, k) if it fits in a {@code long}, otherwise {@code Long.MAX_VALUE}
     * @throws IllegalArgumentException if {@code n < 0}, {@code k < 0}, or {@code k > n}
     * @see #binomial(int, int)
     * @see #binomialToBigInteger(int, int)
     */
    public static long binomialToLong(int n, int k) throws IllegalArgumentException {
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
                    final int nBits = log2(n, RoundingMode.CEILING);

                    long result = 1;
                    long numerator = n--;
                    long denominator = 1;

                    int numeratorBits = nBits;
                    // This is an upper bound on log2(numerator, ceiling).

                    /*
                     * We want to do this in long math for speed, but want to avoid overflow. We adapt the
                     * technique previously used by BigIntegerMath: maintain separate numerator and
                     * denominator accumulators, multiplying the fraction into the result when near overflow.
                     */
                    for (int i = 2; i <= k; i++, n--) {
                        if (numeratorBits + nBits < Long.SIZE - 1) {
                            // It's definitely safe to multiply into numerator and denominator.
                            numerator *= n;
                            denominator *= i;
                            numeratorBits += nBits;
                        } else {
                            // It might not be safe to multiply into numerator and denominator,
                            // so multiply (numerator / denominator) into the result.
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
     * Returns the binomial coefficient "n choose k" as a {@code BigInteger}, denoted as C(n, k) or (n k).
     *
     * <p>The binomial coefficient represents the number of ways to choose {@code k} items from
     * {@code n} items without regard to order. It is calculated as {@code n! / (k! * (n-k)!)}.
     * This method supports arbitrary-precision computation and can handle very large values.
     *
     * <p><b>Performance Note:</b> The result can take as much as <i>O(k log n)</i> space. Use
     * cautiously for very large values of {@code k} and {@code n}.
     *
     * <p>Examples:
     * <pre>{@code
     * Numbers.binomialToBigInteger(5, 2) = 10
     * Numbers.binomialToBigInteger(10, 5) = 252
     * Numbers.binomialToBigInteger(52, 5) = 2598960
     * Numbers.binomialToBigInteger(100, 50)  // returns exact value (large number)
     * Numbers.binomialToBigInteger(1000, 500) // computes exact value (very large, use cautiously)
     * }</pre>
     *
     * @param n the total number of items; must be non-negative
     * @param k the number of items to choose; must be non-negative and at most {@code n}
     * @return the binomial coefficient C(n, k) as a {@code BigInteger}
     * @throws IllegalArgumentException if {@code n < 0}, {@code k < 0}, or {@code k > n}
     * @see #binomial(int, int)
     * @see #binomialToLong(int, int)
     */
    public static BigInteger binomialToBigInteger(final int n, int k) throws IllegalArgumentException {
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

        final int bits = log2(n, RoundingMode.CEILING);

        int numeratorBits = bits;

        for (int i = 1; i < k; i++) {
            final int p = n - i;
            final int q = i + 1;

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
     * negative infinity. This method is resilient to integer overflow.
     * 
     * <p>This implementation uses bitwise operations to compute the mean safely:
     * {@code (x & y) + ((x ^ y) >> 1)} which avoids the overflow issues with 
     * the traditional {@code (x + y) / 2} approach.
     * 
     * <p>The method correctly handles:
     * <ul>
     *   <li>Large values where {@code (x + y)} would overflow</li>
     *   <li>Negative values where unsigned shift {@code (x + y) >>> 1} would fail</li>
     * </ul>
     * 
     * @param x first value
     * @param y second value
     * @return the arithmetic mean of {@code x} and {@code y}, rounded towards negative infinity
     * 
     * @see #mean(long, long)
     */
    public static int mean(final int x, final int y) {
        // Efficient method for computing the arithmetic mean.
        // The alternative (x + y) / 2 fails for large values.
        // The alternative (x + y) >>> 1 fails for negative values.
        return (x & y) + ((x ^ y) >> 1);
    }

    /**
     * Returns the arithmetic mean of {@code x} and {@code y}, rounded towards negative infinity.
     * This method is resilient to integer overflow.
     *
     * <p>This implementation uses bitwise operations to compute the mean safely:
     * {@code (x & y) + ((x ^ y) >> 1)} which avoids the overflow issues with
     * the traditional {@code (x + y) / 2} approach.
     *
     * <p>The method correctly handles:
     * <ul>
     *   <li>Large values where {@code (x + y)} would overflow</li>
     *   <li>Negative values where unsigned shift {@code (x + y) >>> 1} would fail</li>
     * </ul>
     *
     * <p>Examples:
     * <pre>{@code
     * Numbers.mean(10L, 20L) = 15L
     * Numbers.mean(Long.MAX_VALUE, Long.MAX_VALUE) = Long.MAX_VALUE  // no overflow
     * Numbers.mean(Long.MAX_VALUE, Long.MAX_VALUE - 1) = Long.MAX_VALUE - 1
     * Numbers.mean(-10L, 10L) = 0L
     * Numbers.mean(7L, 8L) = 7L  // rounds toward negative infinity
     * }</pre>
     *
     * @param x the first long value
     * @param y the second long value
     * @return the arithmetic mean of {@code x} and {@code y}, rounded towards negative infinity
     * @see #mean(int, int)
     */
    public static long mean(final long x, final long y) {
        // Efficient method for computing the arithmetic mean.
        // The alternative (x + y) / 2 fails for large values.
        // The alternative (x + y) >>> 1 fails for negative values.
        return (x & y) + ((x ^ y) >> 1);
    }

    /**
     * Returns the arithmetic mean of {@code x} and {@code y}.
     *
     * <p>This implementation divides each value by 2 before adding to avoid overflow
     * and maintain precision. Both inputs must be finite values.
     *
     * <p>Examples:
     * <pre>{@code
     * Numbers.mean(10.0, 20.0) = 15.0
     * Numbers.mean(1.5, 2.5) = 2.0
     * Numbers.mean(-10.0, 10.0) = 0.0
     * Numbers.mean(Double.MAX_VALUE, Double.MAX_VALUE) = Double.MAX_VALUE
     * }</pre>
     *
     * @param x the first double value; must be finite
     * @param y the second double value; must be finite
     * @return the arithmetic mean of {@code x} and {@code y}
     * @throws IllegalArgumentException if either {@code x} or {@code y} is not finite (NaN or infinite)
     */
    public static double mean(final double x, final double y) {
        return checkFinite(x) / 2 + checkFinite(y) / 2;
    }

    /**
     * Returns the <a href="http://en.wikipedia.org/wiki/Arithmetic_mean">arithmetic mean</a> of
     * {@code values}.
     *
     * <p>If these values are a sample drawn from a population, this is also an unbiased estimator of
     * the arithmetic mean of the population.
     *
     * @param values a nonempty series of values
     * @return
     * @throws IllegalArgumentException if {@code values} is empty
     */
    public static double mean(final int... values) throws IllegalArgumentException {
        N.checkArgument(values.length > 0, "Cannot take mean of 0 values");
        // The upper bound on the length of an array and the bounds on the int values mean that, in
        // this case only, we can compute the sum as a long without risking overflow or loss of
        // precision. So we do that, as it's slightly quicker than the Knuth algorithm.
        long sum = 0;
        for (final int value : values) {
            sum += value;
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
     * @return
     * @throws IllegalArgumentException if {@code values} is empty
     */
    public static double mean(final long... values) throws IllegalArgumentException {
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

    /**
     *
     * @param values
     * @return
     * @throws IllegalArgumentException
     */
    public static double mean(final double... values) throws IllegalArgumentException {
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

    /**
     *
     * @param argument
     * @return
     */
    private static double checkFinite(final double argument) {
        N.checkArgument(isFinite(argument));
        return argument;
    }

    /**
     *
     * @param x
     * @param mode
     * @return
     */
    static double roundIntermediate(final double x, final RoundingMode mode) {
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
                    return (long) x - 1; //NOSONAR
                }

            case CEILING:
                if (x <= 0.0 || isMathematicalInteger(x)) {
                    return x;
                } else {
                    return (long) x + 1; //NOSONAR
                }

            case DOWN:
                return x;

            case UP:
                if (isMathematicalInteger(x)) {
                    return x;
                } else {
                    return (long) x + (x > 0 ? 1 : -1); //NOSONAR
                }

            case HALF_EVEN:
                return Math.rint(x);

            case HALF_UP: {
                final double z = Math.rint(x);
                if (N.equals(abs(x - z), 0.5)) {
                    return x + Math.copySign(0.5, x);
                } else {
                    return z;
                }
            }

            case HALF_DOWN: {
                final double z = Math.rint(x);
                if (N.equals(abs(x - z), 0.5)) {
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
     * Rounds the given float value to the specified number of decimal places.
     *
     * @param x the float value to be rounded
     * @param scale the number of decimal places to round to, must be non-negative
     * @return the rounded float value
     * @throws IllegalArgumentException if the scale is negative
     * @see #round(double, int)
     * @see Math#round(double)
     */
    public static float round(final float x, final int scale) {
        N.checkArgNotNegative(scale, cs.scale);

        if (scale == 0) {
            return Math.round(x);
        } else if (scale <= 6) {
            final long factor = pow(10, scale);
            return ((float) Math.round(x * factor)) / factor; //NOSONAR
        } else {
            return round(x, scale, RoundingMode.HALF_UP);
        }
    }

    /**
     * Rounds the given double value to the specified number of decimal places.
     *
     * @param x the double value to be rounded
     * @param scale the number of decimal places to round to, must be non-negative
     * @return the rounded double value
     * @throws IllegalArgumentException if the scale is negative
     * @see #round(double, int, RoundingMode)
     * @see Math#round(double)
     */
    public static double round(final double x, final int scale) throws IllegalArgumentException {
        N.checkArgNotNegative(scale, cs.scale);

        if (scale == 0) {
            return Math.round(x);
        } else if (scale <= 6) {
            final long factor = pow(10, scale);
            return ((double) Math.round(x * factor)) / factor; //NOSONAR
        } else {
            return round(x, scale, RoundingMode.HALF_UP);
        }
    }

    /**
     * Rounds the given double value to the specified number of decimal places.
     *
     * @param x the double value to be rounded
     * @param scale the number of decimal places to round to, must be non-negative
     * @param roundingMode the rounding mode to use
     * @return the rounded double value
     * @throws IllegalArgumentException if the scale is negative
     * @see #round(double, int, RoundingMode)
     * @see BigDecimal#setScale(int, RoundingMode)
     * @see BigDecimal#doubleValue()
     */
    public static float round(final float x, final int scale, final RoundingMode roundingMode) {
        final BigDecimal bd = new BigDecimal(Float.toString(x)).setScale(scale, roundingMode == null ? RoundingMode.HALF_UP : roundingMode);
        final float rounded = bd.floatValue();
        return N.equals(rounded, FLOAT_POSITIVE_ZERO) ? FLOAT_POSITIVE_ZERO * x : rounded;
    }

    /**
     * Rounds the given double value to the specified number of decimal places.
     *
     * @param x the double value to be rounded
     * @param scale the number of decimal places to round to, must be non-negative
     * @param roundingMode the rounding mode to use
     * @return the rounded double value
     * @throws IllegalArgumentException if the scale is negative
     * @see BigDecimal#setScale(int, RoundingMode)
     * @see BigDecimal#doubleValue()
     */
    public static double round(final double x, final int scale, final RoundingMode roundingMode) {
        final BigDecimal bd = BigDecimal.valueOf(x).setScale(scale, roundingMode == null ? RoundingMode.HALF_UP : roundingMode);
        final double rounded = bd.doubleValue();
        return N.equals(rounded, DOUBLE_POSITIVE_ZERO) ? DOUBLE_POSITIVE_ZERO * x : rounded;
    }

    static final Map<String, DecimalFormat> decimalFormatPool = ImmutableMap.<String, DecimalFormat> builder()
            .put("#", new DecimalFormat("#"))
            .put("#.#", new DecimalFormat("#.#"))
            .put("#.##", new DecimalFormat("#.##"))
            .put("#.###", new DecimalFormat("#.###"))
            .put("#.####", new DecimalFormat("#.####"))
            .put("#.#####", new DecimalFormat("#.#####"))
            .put("#.######", new DecimalFormat("#.######"))
            .put("0", new DecimalFormat("0"))
            .put("0.0", new DecimalFormat("0.0"))
            .put("0.00", new DecimalFormat("0.00"))
            .put("0.000", new DecimalFormat("0.000"))
            .put("0.0000", new DecimalFormat("0.0000"))
            .put("0.00000", new DecimalFormat("0.00000"))
            .put("0.000000", new DecimalFormat("0.000000"))
            .put("#%", new DecimalFormat("#%"))
            .put("#.#%", new DecimalFormat("#.#%"))
            .put("#.##%", new DecimalFormat("#.##%"))
            .put("#.###%", new DecimalFormat("#.###%"))
            .put("#.####%", new DecimalFormat("#.####%"))
            .put("#.#####%", new DecimalFormat("#.#####%"))
            .put("#.######%", new DecimalFormat("#.######%"))
            .put("0%", new DecimalFormat("0%"))
            .put("0.0%", new DecimalFormat("0.0%"))
            .put("0.00%", new DecimalFormat("0.00%"))
            .put("0.000%", new DecimalFormat("0.000%"))
            .put("0.0000%", new DecimalFormat("0.0000%"))
            .put("0.00000%", new DecimalFormat("0.00000%"))
            .put("0.000000%", new DecimalFormat("0.000000%"))
            .build();

    /**
     * Rounds the given float value using the specified DecimalFormat.
     *
     * @param x the float value to be rounded
     * @param decimalFormat the DecimalFormat to use for rounding
     * @return the rounded float value
     * @throws IllegalArgumentException if the decimalFormat is null
     * @see DecimalFormat#format(double)
     * @see #toFloat(String)
     */
    public static float round(final float x, final String decimalFormat) {
        return toFloat(format(x, decimalFormat));
    }

    /**
     * Rounds the given double value using the specified DecimalFormat.
     *
     * @param x the double value to be rounded
     * @param decimalFormat the DecimalFormat to use for rounding
     * @return the rounded double value
     * @throws IllegalArgumentException if the decimalFormat is null
     * @see DecimalFormat#format(double)
     * @see #toDouble(String)
     */
    public static double round(final double x, final String decimalFormat) {
        return toDouble(format(x, decimalFormat));
    }

    /**
     * Rounds the given double value using the specified DecimalFormat.
     *
     * @param x the double value to be rounded
     * @param decimalFormat the DecimalFormat to use for rounding
     * @return the rounded double value
     * @throws IllegalArgumentException if the decimalFormat is null
     * @see DecimalFormat#format(double)
     * @see #toFloat(String)
     */
    public static float round(final float x, final DecimalFormat decimalFormat) throws IllegalArgumentException {
        N.checkArgNotNull(decimalFormat, cs.decimalFormat);

        try {
            return decimalFormat.parse(decimalFormat.format(x)).floatValue();
        } catch (ParseException e) {
            throw new IllegalArgumentException(e);
        }
    }

    /**
     * Rounds the given double value using the specified DecimalFormat.
     *
     * @param x the double value to be rounded
     * @param decimalFormat the DecimalFormat to use for rounding
     * @return the rounded double value
     * @throws IllegalArgumentException if the decimalFormat is null
     * @see DecimalFormat#format(double)
     * @see #toDouble(String)
     */
    public static double round(final double x, final DecimalFormat decimalFormat) throws IllegalArgumentException {
        N.checkArgNotNull(decimalFormat, cs.decimalFormat);

        try {
            return decimalFormat.parse(decimalFormat.format(x)).doubleValue();
        } catch (ParseException e) {
            throw new IllegalArgumentException(e);
        }
    }

    /**
     * Returns the {@code int} value that is equal to {@code x} rounded with the specified rounding mode, if possible.
     *
     * @param x
     * @param mode
     * @return
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
    public static int roundToInt(final double x, final RoundingMode mode) {
        final double z = roundIntermediate(x, mode);
        checkInRange(z > MIN_INT_AS_DOUBLE - 1.0 && z < MAX_INT_AS_DOUBLE + 1.0);
        return (int) z;
    }

    /**
     * Returns the {@code long} value that is equal to {@code x} rounded with the specified rounding mode, if possible.
     *
     * @param x
     * @param mode
     * @return
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
    public static long roundToLong(final double x, final RoundingMode mode) {
        final double z = roundIntermediate(x, mode);
        checkInRange(MIN_LONG_AS_DOUBLE - z < 1.0 && z < MAX_LONG_AS_DOUBLE_PLUS_ONE);
        return (long) z;
    }

    /**
     * Returns the {@code BigInteger} value that is equal to {@code x} rounded with the specified rounding mode, if possible.
     *
     * @param x
     * @param mode
     * @return
     * @throws ArithmeticException if
     *     <ul>
     *     <li>{@code x} is infinite or NaN
     *     <li>{@code x} is not a mathematical integer and {@code mode} is
     *         {@link RoundingMode#UNNECESSARY}
     *     </ul>
     */
    // #roundIntermediate, java.lang.Math.getExponent, com.google.common.math.DoubleUtils
    public static BigInteger roundToBigInteger(double x, final RoundingMode mode) {
        x = roundIntermediate(x, mode);
        if (MIN_LONG_AS_DOUBLE - x < 1.0 && x < MAX_LONG_AS_DOUBLE_PLUS_ONE) {
            return BigInteger.valueOf((long) x);
        }
        final int exponent = getExponent(x);
        final long significand = getSignificand(x);
        final BigInteger result = BigInteger.valueOf(significand).shiftLeft(exponent - SIGNIFICAND_BITS);
        return (x < 0) ? result.negate() : result;
    }

    static final int MAX_FACTORIAL = 170;

    static final double[] everySixteenthFactorial = { 0x1.0p0, 0x1.30777758p44, 0x1.956ad0aae33a4p117, 0x1.ee69a78d72cb6p202, 0x1.fe478ee34844ap295,
            0x1.c619094edabffp394, 0x1.3638dd7bd6347p498, 0x1.7cac197cfe503p605, 0x1.1e5dfc140e1e5p716, 0x1.8ce85fadb707ep829, 0x1.95d5f3d928edep945 };

    /**
     * Compares two float values for approximate equality within a specified tolerance.
     * 
     * <p>Technically speaking, this is equivalent to
     * {@code Math.abs(a - b) <= tolerance || Float.valueOf(a).equals(Float.valueOf(b))}.
     * 
     * <p>
     * <h3>Example:</h3>
     * <pre>{@code
     * // Returns true, the values are within tolerance
     * boolean result = Numbers.fuzzyEquals(1.0001f, 1.0002f, 0.001f);
     * 
     * // Returns false, the values exceed the tolerance
     * boolean result = Numbers.fuzzyEquals(1.0f, 1.1f, 0.01f);
     * }</pre>
     *
     * @param a the first float value to compare
     * @param b the second float value to compare
     * @param tolerance the maximum absolute difference allowed between the two values to consider them equal
     * @return {@code true} if the absolute difference between {@code a} and {@code b} is less than or equal to {@code tolerance},
     *         {@code false} otherwise
     * @see #fuzzyEquals(double, double, double)
     * @see Float#compare(float, float)
     */
    public static boolean fuzzyEquals(final float a, final float b, final float tolerance) {
        // Check that tolerance is valid (non-negative and not NaN)
        if (tolerance < 0.0 || Float.isNaN(tolerance)) {
            throw new IllegalArgumentException("tolerance must be non-negative and not NaN");
        }

        return Math.copySign(a - b, 1.0f) <= tolerance // branch-free version of abs(a - b)
                // copySign(x, 1.0) is a branch-free version of abs(x), but with different NaN semantics
                || (N.equals(a, b)) // needed to ensure that infinities equal themselves
                || (Float.isNaN(a) && Float.isNaN(b));
    }

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
     * @param a the first double value to compare
     * @param b the second double value to compare
     * @param tolerance the maximum absolute difference allowed between the two values to consider them equal
     * @return {@code true} if the absolute difference between {@code a} and {@code b} is less than or equal to {@code tolerance},
     * @throws IllegalArgumentException if {@code tolerance} is {@code < 0} or NaN
     */
    public static boolean fuzzyEquals(final double a, final double b, final double tolerance) {
        // Check that tolerance is valid (non-negative and not NaN)
        if (tolerance < 0.0 || Double.isNaN(tolerance)) {
            throw new IllegalArgumentException("tolerance must be non-negative and not NaN");
        }

        return Math.copySign(a - b, 1.0) <= tolerance // branch-free version of abs(a - b)
                // copySign(x, 1.0) is a branch-free version of abs(x), but with different NaN semantics
                || (N.equals(a, b)) // needed to ensure that infinities equal themselves
                || (Double.isNaN(a) && Double.isNaN(b));
    }

    /**
     * Compares {@code a} and {@code b} "fuzzily," with a tolerance for nearly equal values.
     *
     * <p>This method is equivalent to
     * {@code fuzzyEquals(a, b, tolerance) ? 0 : Float.compare(a, b)}. In particular, like
     * {@link Float#compare(float, float)}, it treats all NaN values as equal and greater than all
     * other values (including {@link Float#POSITIVE_INFINITY}).
     *
     * <p>This is <em>not</em> a total ordering and is <em>not</em> suitable for use in
     * {@link Comparable#compareTo} implementations. In particular, it is not transitive.
     *
     * @param a the first float value to compare
     * @param b the second float value to compare
     * @param tolerance the maximum absolute difference allowed between the two values to consider them equal
     * @return {@code 0} if {@code a} and {@code b} are fuzzily equal, a negative integer if {@code a} is less than {@code b},
     * @throws IllegalArgumentException if {@code tolerance} is {@code < 0} or NaN
     */
    public static int fuzzyCompare(final float a, final float b, final float tolerance) {
        if (fuzzyEquals(a, b, tolerance)) {
            return 0;
        } else if (a < b) {
            return -1;
        } else if (a > b) {
            return 1;
        } else {
            return Boolean.compare(Float.isNaN(a), Float.isNaN(b));
        }
    }

    /**
     * Compares {@code a} and {@code b} "fuzzily," with a tolerance for nearly equal values.
     *
     * <p>This method is equivalent to
     * {@code fuzzyEquals(a, b, tolerance) ? 0 : Double.compare(a, b)}. In particular, like
     * {@link Double#compare(double, double)}, it treats all NaN values as equal and greater than all
     * other values (including {@link Double#POSITIVE_INFINITY}).
     *
     * <p>This is <em>not</em> a total ordering and is <em>not</em> suitable for use in
     * {@link Comparable#compareTo} implementations. In particular, it is not transitive.
     *
     * @param a the first double value to compare
     * @param b the second double value to compare
     * @param tolerance the maximum absolute difference allowed between the two values to consider them equal
     * @return {@code 0} if {@code a} and {@code b} are fuzzily equal, a negative integer if {@code a} is less than {@code b},
     * @throws IllegalArgumentException if {@code tolerance} is {@code < 0} or NaN
     */
    public static int fuzzyCompare(final double a, final double b, final double tolerance) {
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
     *
     * @param x
     * @return {@code true}, if is mathematical integer
     */
    public static boolean isMathematicalInteger(final double x) {
        // return isFinite(x) && (N.equals(x, 0.0) || SIGNIFICAND_BITS - Long.numberOfTrailingZeros(getSignificand(x)) <= getExponent(x));
        return !Double.isNaN(x) && !Double.isInfinite(x) && x == Math.rint(x);
    }

    /**
     * Returns 1 if {@code x < y} as unsigned longs, and 0 otherwise. Assumes that x - y fits into a
     * signed long. The implementation is branch-free, and benchmarks suggest it is measurably faster
     * than the straightforward ternary expression.
     *
     * @param x
     * @param y
     * @return
     */
    static int lessThanBranchFree(final long x, final long y) {
        // Returns the sign bit of x - y.
        return (int) ((x - y) >>> (Long.SIZE - 1));
    }

    static int log10Floor(final long x) {
        /*
         * Based on Hacker's Delight Fig. 11-5, the two-table-lookup, branch-free implementation.
         *
         * The key idea is that based on the number of leading zeros (equivalently, floor(log2(x))), we
         * can narrow the possible floor(log10(x)) values to two. For example, if floor(log2(x)) is 6,
         * then 64 <= x < 128, so floor(log10(x)) is either 1 or 2.
         */
        final int y = maxLog10ForLeadingZeros[Long.numberOfLeadingZeros(x)];
        /*
         * y is the higher of the two possible values of floor(log10(x)). If x < 10^y, then we want the
         * lower of the two possible values, or y - 1, otherwise, we want y.
         */
        return y - lessThanBranchFree(x, powersOf10[y]);
    }

    /**
     * Returns (x * numerator / denominator), which is assumed to come out to an integral value.
     *
     * @param x
     * @param numerator
     * @param denominator
     * @return
     */
    static long multiplyFraction(long x, final long numerator, long denominator) {
        if (x == 1) {
            return numerator / denominator;
        }
        final long commondivisor = gcd(x, denominator);
        x /= commondivisor;
        denominator /= commondivisor; //NOSONAR
        // We know gcd(x, denominator) = 1, and x * numerator / denominator is exact,
        // so denominator must be a divisor of numerator.
        return x * (numerator / denominator); //NOSONAR
    }

    static double nextDown(final double d) {
        return -Math.nextUp(-d);
    }

    static long getSignificand(final double d) {
        N.checkArgument(isFinite(d), "not a normal value");
        final int exponent = getExponent(d);
        long bits = doubleToRawLongBits(d);
        bits &= SIGNIFICAND_MASK;
        return (exponent == MIN_EXPONENT - 1) ? bits << 1 : bits | IMPLICIT_BIT;
    }

    // These values were generated by using checkedMultiply to see when the simple multiply/divide
    // algorithm would lead to an overflow.

    /**
     * Checks if is finite.
     *
     * @param d
     * @return {@code true}, if is finite
     */
    static boolean isFinite(final double d) {
        return getExponent(d) <= MAX_EXPONENT;
    }

    /**
     * Checks if is normal.
     *
     * @param d
     * @return {@code true}, if is normal
     */
    static boolean isNormal(final double d) {
        return getExponent(d) >= MIN_EXPONENT;
    }

    /*
     * Returns x scaled by a power of 2 such that it is in the range [1, 2). Assumes x is positive,
     * normal, and finite.
     */
    static double scaleNormalize(final double x) {
        final long significand = doubleToRawLongBits(x) & SIGNIFICAND_MASK;
        return longBitsToDouble(significand | ONE_BITS);
    }

    /**
     * Big to double.
     *
     * @param x
     * @return
     */
    static double bigToDouble(final BigInteger x) {
        // This is an extremely fast implementation of BigInteger.doubleValue(). JDK patch pending.
        final BigInteger absX = x.abs();
        final int exponent = absX.bitLength() - 1;
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
        final int shift = exponent - SIGNIFICAND_BITS - 1;
        final long twiceSignifFloor = absX.shiftRight(shift).longValue();
        long signifFloor = twiceSignifFloor >> 1;
        signifFloor &= SIGNIFICAND_MASK; // remove the implied bit

        /*
         * We round up if either the fractional part of signify is strictly greater than 0.5 (which is
         * {@code true} if the 0.5 bit is set and any lower bit is set), or if the fractional part of signif is
         * >= 0.5 and signifFloor is odd (which is {@code true} if both the 0.5 bit and the 1 bit are set).
         */
        final boolean increment = (twiceSignifFloor & 1) != 0 && ((signifFloor & 1) != 0 || absX.getLowestSetBit() < shift);
        final long signifRounded = increment ? signifFloor + 1 : signifFloor;
        long bits = (long) (exponent + EXPONENT_BIAS) << SIGNIFICAND_BITS;
        bits += signifRounded;
        /*
         * If signifRounded == 2^53, we'd need to set all the significand bits to zero and add 1 to
         * the exponent. This is exactly the behavior we get from just adding signifRounded to bits
         * directly. If the exponent is MAX_DOUBLE_EXPONENT, we round up (correctly) to
         * Double.POSITIVE_INFINITY.
         */
        bits |= x.signum() & SIGN_MASK;
        return longBitsToDouble(bits);
    }

    /**
     * Returns its argument if it is non-negative, zero if it is negative.
     *
     * @param value
     * @return
     */
    static double ensureNonNegative(final double value) {
        N.checkArgument(!isNaN(value));
        return Math.max(value, 0.0);
    }

    static int lessThanBranchFree(final int x, final int y) {
        // Returns the sign bit of x - y.
        return (x - y) >>> (Integer.SIZE - 1);
    }

    // These values were generated by using checkedMultiply to see when the simple multiply/divide
    // algorithm would lead to an overflow.

    static boolean fitsInInt(final long x) {
        return (int) x == x;
    }

    static int checkPositive(final String role, final int x) {
        if (x <= 0) {
            throw new IllegalArgumentException(role + " (" + x + ") must be > 0");
        }
        return x;
    }

    static long checkPositive(final String role, final long x) {
        if (x <= 0) {
            throw new IllegalArgumentException(role + " (" + x + ") must be > 0");
        }
        return x;
    }

    static BigInteger checkPositive(final String role, final BigInteger x) {
        if (x.signum() <= 0) {
            throw new IllegalArgumentException(role + " (" + x + ") must be > 0");
        }
        return x;
    }

    static int checkNonNegative(final String role, final int x) {
        if (x < 0) {
            throw new IllegalArgumentException(role + " (" + x + ") must be >= 0");
        }
        return x;
    }

    static long checkNonNegative(final String role, final long x) {
        if (x < 0) {
            throw new IllegalArgumentException(role + " (" + x + ") must be >= 0");
        }
        return x;
    }

    static BigInteger checkNonNegative(final String role, final BigInteger x) {
        if (x.signum() < 0) {
            throw new IllegalArgumentException(role + " (" + x + ") must be >= 0");
        }
        return x;
    }

    static double checkNonNegative(final String role, final double x) {
        if (!(x >= 0)) { // not x < 0, to work with NaN. //NOSONAR
            throw new IllegalArgumentException(role + " (" + x + ") must be >= 0");
        }
        return x;
    }

    static void checkRoundingUnnecessary(final boolean condition) {
        if (!condition) {
            throw new ArithmeticException("mode was UNNECESSARY, but rounding was necessary");
        }
    }

    static void checkInRange(final boolean condition) {
        if (!condition) {
            throw new ArithmeticException("not in range");
        }
    }

    static void checkNoOverflow(final boolean condition) {
        if (!condition) {
            throw new ArithmeticException("overflow");
        }
    }

    /**
     * Computes the inverse hyperbolic sine (arcsinh) of a number.
     *
     * <p>The inverse hyperbolic sine is defined as: {@code asinh(x) = ln(x + sqrt(x² + 1))}.
     * This function is the inverse of the hyperbolic sine function {@code sinh}, meaning
     * {@code sinh(asinh(x)) = x} for all real {@code x}.
     *
     * <p>This implementation uses an efficient approximation based on Taylor series expansion
     * for small values, and the logarithmic formula for larger values.
     *
     * <p>Mathematical properties:
     * <ul>
     *   <li>{@code asinh(-x) = -asinh(x)} (odd function)</li>
     *   <li>{@code asinh(0) = 0}</li>
     *   <li>Domain: all real numbers (-∞, +∞)</li>
     *   <li>Range: all real numbers (-∞, +∞)</li>
     * </ul>
     *
     * <p>Examples:
     * <pre>{@code
     * Numbers.asinh(0.0) = 0.0
     * Numbers.asinh(1.0) ≈ 0.88137
     * Numbers.asinh(-1.0) ≈ -0.88137
     * Numbers.asinh(10.0) ≈ 2.99822
     * }</pre>
     *
     * @param a the number on which to compute the inverse hyperbolic sine
     * @return the inverse hyperbolic sine of {@code a}
     * @see #acosh(double)
     * @see #atanh(double)
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

    /**
     * Computes the inverse hyperbolic cosine (arccosh) of a number.
     *
     * <p>The inverse hyperbolic cosine is defined as: {@code acosh(x) = ln(x + sqrt(x² - 1))}.
     * This function is the inverse of the hyperbolic cosine function {@code cosh}, meaning
     * {@code cosh(acosh(x)) = x} for all {@code x >= 1}.
     *
     * <p>Mathematical properties:
     * <ul>
     *   <li>{@code acosh(1) = 0}</li>
     *   <li>Domain: [1, +∞) (requires {@code x >= 1})</li>
     *   <li>Range: [0, +∞)</li>
     *   <li>For {@code x < 1}, the result is NaN</li>
     * </ul>
     *
     * <p>Examples:
     * <pre>{@code
     * Numbers.acosh(1.0) = 0.0
     * Numbers.acosh(2.0) ≈ 1.31696
     * Numbers.acosh(10.0) ≈ 2.99322
     * Numbers.acosh(0.5)  // NaN (outside domain)
     * }</pre>
     *
     * @param a the number on which to compute the inverse hyperbolic cosine; must be >= 1
     * @return the inverse hyperbolic cosine of {@code a}, or NaN if {@code a < 1}
     * @see #asinh(double)
     * @see #atanh(double)
     */
    public static double acosh(final double a) {
        return Math.log(a + Math.sqrt(a * a - 1));
    }

    /**
     * Computes the inverse hyperbolic tangent (arctanh) of a number.
     *
     * <p>The inverse hyperbolic tangent is defined as: {@code atanh(x) = 0.5 * ln((1 + x) / (1 - x))}.
     * This function is the inverse of the hyperbolic tangent function {@code tanh}, meaning
     * {@code tanh(atanh(x)) = x} for all {@code -1 < x < 1}.
     *
     * <p>This implementation uses an efficient approximation based on Taylor series expansion
     * for small values, and the logarithmic formula for larger values.
     *
     * <p>Mathematical properties:
     * <ul>
     *   <li>{@code atanh(-x) = -atanh(x)} (odd function)</li>
     *   <li>{@code atanh(0) = 0}</li>
     *   <li>Domain: (-1, 1) (requires {@code -1 < x < 1})</li>
     *   <li>Range: all real numbers (-∞, +∞)</li>
     *   <li>For {@code |x| >= 1}, the result is NaN or ±∞</li>
     * </ul>
     *
     * <p>Examples:
     * <pre>{@code
     * Numbers.atanh(0.0) = 0.0
     * Numbers.atanh(0.5) ≈ 0.54931
     * Numbers.atanh(-0.5) ≈ -0.54931
     * Numbers.atanh(0.9) ≈ 1.47222
     * Numbers.atanh(1.0)  // ±∞
     * Numbers.atanh(1.5)  // NaN (outside domain)
     * }</pre>
     *
     * @param a the number on which to compute the inverse hyperbolic tangent; should be in (-1, 1)
     * @return the inverse hyperbolic tangent of {@code a}
     * @see #asinh(double)
     * @see #acosh(double)
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

    /**
     * The Class UnsignedLongs.
     */
    static final class UnsignedLongs {

        /**
         * Instantiates a new unsigned longs.
         */
        private UnsignedLongs() {
        }

        /** The Constant MAX_VALUE. */
        public static final long MAX_VALUE = -1L; // Equivalent to 2^64 - 1

        /**
         * A (self-inverse) bijection which converts the ordering on unsigned longs to the ordering on
         * longs, that is, {@code a <= b} as unsigned longs if and only if {@code flip(a) <= flip(b)} as
         * signed longs.
         *
         * @param a
         * @return
         */
        private static long flip(final long a) {
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
        static int compare(final long a, final long b) {
            return Long.compare(flip(a), flip(b));
        }

        /**
         * Returns dividend % divisor, where the dividend and divisor are treated as unsigned 64-bit
         * quantities.
         *
         * @param dividend the dividend (numerator)
         * @param divisor the divisor (denominator)
         * @return
         * @throws ArithmeticException if divisor is 0
         */
        static long remainder(final long dividend, final long divisor) {
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
            final long quotient = ((dividend >>> 1) / divisor) << 1;
            final long rem = dividend - quotient * divisor;
            return rem - (compare(rem, divisor) >= 0 ? divisor : 0);
        }

    }

    private enum MillerRabinTester {
        /**
         * Works for inputs <= FLOOR_SQRT_MAX_LONG.
         */
        SMALL {
            @Override
            long mulMod(final long a, final long b, final long m) {
                /*
                 * NOTE(lowasser, 2015-Feb-12): Benchmarks suggest that changing this to
                 * UnsignedLongs.remainder and increasing the threshold to 2^32 doesn't pay for itself, and
                 * adding another enum constant hurts performance further -- I suspect because bimorphic
                 * implementation is a sweet spot for the JVM.
                 */
                return (a * b) % m;
            }

            @Override
            long squareMod(final long a, final long m) {
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
            private long plusMod(final long a, final long b, final long m) {
                return (a >= m - b) ? (a + b - m) : (a + b);
            }

            /**
             * Returns (a * 2^32) mod m. a may be any unsigned long.
             */
            private long times2ToThe32Mod(long a, final long m) {
                int remainingPowersOf2 = 32;
                do {
                    final int shift = Math.min(remainingPowersOf2, Long.numberOfLeadingZeros(a));
                    // shift is either the number of powers of 2 left to multiply a by, or the biggest shift
                    // possible while keeping a in an unsigned long.
                    a = UnsignedLongs.remainder(a << shift, m);
                    remainingPowersOf2 -= shift;
                } while (remainingPowersOf2 > 0);
                return a;
            }

            @Override
            long mulMod(final long a, final long b, final long m) {
                final long aHi = a >>> 32; // < 2^31
                final long bHi = b >>> 32; // < 2^31
                final long aLo = a & 0xFFFFFFFFL; // < 2^32
                final long bLo = b & 0xFFFFFFFFL; // < 2^32

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
            long squareMod(final long a, final long m) {
                final long aHi = a >>> 32; // < 2^31
                final long aLo = a & 0xFFFFFFFFL; // < 2^32

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

        /**
         *
         * @param base
         * @param n
         * @return
         */
        static boolean test(final long base, final long n) {
            // Since base will be considered % n, it's okay if base > FLOOR_SQRT_MAX_LONG,
            // so long as n <= FLOOR_SQRT_MAX_LONG.
            return ((n <= FLOOR_SQRT_MAX_LONG) ? SMALL : LARGE).testWitness(base, n);
        }

        /**
         * Returns a * b mod m.
         *
         * @param a
         * @param b
         * @param m
         * @return
         */
        abstract long mulMod(long a, long b, long m);

        /**
         * Returns a^2 mod m.
         *
         * @param a
         * @param m
         * @return
         */
        abstract long squareMod(long a, long m);

        /**
         * Returns a^p mod m.
         *
         * @param a
         * @param p
         * @param m
         * @return
         */
        private long powMod(long a, long p, final long m) {
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
         * Returns {@code true} if n is a strong probable prime relative to the specified base.
         *
         * @param base
         * @param n
         * @return
         */
        private boolean testWitness(long base, final long n) {
            final int r = Long.numberOfTrailingZeros(n - 1);
            final long d = (n - 1) >> r;
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

}

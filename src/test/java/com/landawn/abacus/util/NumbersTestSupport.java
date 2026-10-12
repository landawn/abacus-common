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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

public abstract class NumbersTestSupport extends TestBase {

    protected static final double DELTA = 1e-15;

    protected static final float FLOAT_DELTA = 1e-7f;

    protected static String signedRadixToken(final BigInteger value, final int radix, final String prefix, final boolean explicitPlus) {
        final String sign = value.signum() < 0 ? "-" : (explicitPlus ? "+" : "");
        return sign + prefix + value.abs().toString(radix);
    }

    /**
     * Reference implementation of the pre-optimization {@code log10(double, RoundingMode)}:
     * estimate with {@code Math.log10}, then correct with exact BigDecimal power comparisons.
     */
    protected static int legacyLog10Rounded(final double x, final RoundingMode mode) {
        final BigDecimal exactX = new BigDecimal(x);
        int logFloor = (int) Math.floor(Math.log10(x));
        BigDecimal floorPower = BigDecimal.ONE.scaleByPowerOfTen(logFloor);

        while (exactX.compareTo(floorPower) < 0) {
            floorPower = BigDecimal.ONE.scaleByPowerOfTen(--logFloor);
        }

        BigDecimal nextPower = floorPower.scaleByPowerOfTen(1);

        while (exactX.compareTo(nextPower) >= 0) {
            floorPower = nextPower;
            nextPower = floorPower.scaleByPowerOfTen(1);
            logFloor++;
        }

        final boolean isPowerOfTen = exactX.compareTo(floorPower) == 0;

        switch (mode) {
            case UNNECESSARY:
                if (!isPowerOfTen) {
                    throw new ArithmeticException("Rounding necessary");
                }
                return logFloor;
            case FLOOR:
                return logFloor;
            case CEILING:
                return isPowerOfTen ? logFloor : logFloor + 1;
            case DOWN:
                return logFloor < 0 && !isPowerOfTen ? logFloor + 1 : logFloor;
            case UP:
                return logFloor >= 0 && !isPowerOfTen ? logFloor + 1 : logFloor;
            default: // HALF_DOWN, HALF_EVEN, HALF_UP
                final BigDecimal squaredX = exactX.multiply(exactX);
                final BigDecimal squaredHalfPower = BigDecimal.ONE.scaleByPowerOfTen(2 * logFloor + 1);
                return squaredX.compareTo(squaredHalfPower) < 0 ? logFloor : logFloor + 1;
        }
    }

    protected static String causeMessageOfCreateNumber(final String str) {
        final NumberFormatException nfe = assertThrows(NumberFormatException.class, () -> Numbers.createNumber(str));
        assertNotNull(nfe.getCause(), "no cause for " + str);
        return nfe.getCause().getMessage();
    }

    /** A {@code Number} outside the eight built-in types whose {@code toString()} is exact decimal text. */
    protected static final class DecimalTextNumber extends Number {
        protected static final long serialVersionUID = 1L;

        protected final BigDecimal value;

        DecimalTextNumber(final String text) {
            value = new BigDecimal(text);
        }

        @Override
        public int intValue() {
            return value.intValue();
        }

        @Override
        public long longValue() {
            return value.longValue();
        }

        @Override
        public float floatValue() {
            return value.floatValue();
        }

        @Override
        public double doubleValue() {
            return value.doubleValue();
        }

        @Override
        public String toString() {
            return value.toPlainString();
        }
    }

    /** A {@code Number} outside the eight built-in types whose {@code toString()} is not a numeric token. */
    protected static final class FormattedNumber extends Number {
        protected static final long serialVersionUID = 1L;

        protected final double value;

        FormattedNumber(final double value) {
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
            return value + " ms";
        }
    }

    static final class CountingNumber20260906 extends Number {
        protected static final long serialVersionUID = 1L;

        int toStringCalls = 0;

        @Override
        public String toString() {
            toStringCalls++;
            return "n/a";
        }

        @Override
        public double doubleValue() {
            return Double.NaN;
        }

        @Override
        public float floatValue() {
            return Float.NaN;
        }

        @Override
        public long longValue() {
            return 0L;
        }

        @Override
        public int intValue() {
            return 0;
        }
    }

    /** A Number whose toString() is legally empty rather than null. */
    static final class BlankTextNumber20260906 extends Number {
        protected static final long serialVersionUID = 1L;

        protected final double value;

        BlankTextNumber20260906(final double value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return "";
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
    }

    protected static void assertBoundedAndEscaped(final Throwable t) {
        for (Throwable cur = t; cur != null && cur != cur.getCause(); cur = cur.getCause()) {
            final String message = cur.getMessage();

            if (message == null) {
                continue;
            }

            assertTrue(message.length() < 256, "message is " + message.length() + " chars: " + message.substring(0, Math.min(60, message.length())));

            for (int i = 0; i < message.length(); i++) {
                assertTrue(!Character.isISOControl(message.charAt(i)),
                        "raw control U+" + Integer.toHexString(message.charAt(i)) + " at " + i + " of " + cur.getClass().getSimpleName());
            }
        }
    }

    // Test doubles

    /**
     * A {@code Number} outside the eight built-in types whose {@code toString()} and {@code doubleValue()}
     * deliberately disagree, so the recovery route actually taken is observable in the result.
     */
    protected static final class SplitNumber extends Number {
        protected static final long serialVersionUID = 1L;

        protected final String text;
        protected final double value;

        SplitNumber(final String text, final double value) {
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

    /** The sentinel {@code doubleValue()} used throughout: a value no test text denotes. */
    protected static final double SENTINEL = -7654321.0d;

    protected static SplitNumber n(final String text) {
        return new SplitNumber(text, SENTINEL);
    }

    /** {@code BigDecimal.TWO} is JDK 19+; the build targets 17. */
    protected static final BigDecimal TWO = BigDecimal.valueOf(2);

    protected static void assertSameDouble(final double expected, final double actual, final String what) {
        assertEquals(Double.doubleToRawLongBits(expected), Double.doubleToRawLongBits(actual), what + ": expected " + expected + " but was " + actual);
    }

    protected static final String UNNECESSARY_MESSAGE = "mode was UNNECESSARY, but rounding was necessary";
}

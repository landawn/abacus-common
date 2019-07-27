/*
 * Copyright (C) 2011 The Guava Authors
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

package com.landawn.abacus.hash;

import static java.lang.Math.abs;
import static java.math.RoundingMode.HALF_EVEN;
import static java.math.RoundingMode.HALF_UP;

import java.math.RoundingMode;

import com.landawn.abacus.util.N;

/**
 * Note: It's copied from Google Guava under Apache License 2.0
 * 
 *
 */
final class Util {
    private Util() {
        // singleton
    }

    public static final class Chars {
        public static final int BYTES = Character.SIZE / Byte.SIZE;

        private Chars() {
            // singleton
        }
    }

    public static final class Shorts {
        public static final int BYTES = Short.SIZE / Byte.SIZE;

        private Shorts() {
            // singleton
        }
    }

    public static final class Ints {
        public static final int BYTES = Integer.SIZE / Byte.SIZE;

        private Ints() {
            // singleton
        }

        /**
         * Returns the {@code int} value that is equal to {@code value}, if possible.
         *
         * @param value any value in the range of the {@code int} type
         * @return the {@code int} value that equals {@code value}
         * @throws IllegalArgumentException if {@code value} is greater than {@link Integer#MAX_VALUE} or
         *     less than {@link Integer#MIN_VALUE}
         */
        public static int checkedCast(long value) {
            int result = (int) value;
            if (result != value) {
                // don't use checkArgument here, to avoid boxing
                throw new IllegalArgumentException("Out of range: " + value);
            }
            return result;
        }
    }

    public static final class Longs {
        public static final int BYTES = Long.SIZE / Byte.SIZE;

        private Longs() {
            // singleton
        }

        public static long fromBytes(byte b1, byte b2, byte b3, byte b4, byte b5, byte b6, byte b7, byte b8) {
            return (b1 & 0xFFL) << 56 | (b2 & 0xFFL) << 48 | (b3 & 0xFFL) << 40 | (b4 & 0xFFL) << 32 | (b5 & 0xFFL) << 24 | (b6 & 0xFFL) << 16
                    | (b7 & 0xFFL) << 8 | (b8 & 0xFFL);
        }
    }

    public static final class SignedBytes {
        private SignedBytes() {
            // singleton            
        }

        public static byte checkedCast(long value) {
            byte result = (byte) value;
            if (result != value) {
                // don't use checkArgument here, to avoid boxing
                throw new IllegalArgumentException("Out of range: " + value);
            }
            return result;
        }
    }

    public static final class UnsignedBytes {
        private static final int UNSIGNED_MASK = 0xFF;

        private UnsignedBytes() {
            // singleton            
        }

        public static byte checkedCast(long value) {
            if ((value >> Byte.SIZE) != 0) {
                // don't use checkArgument here, to avoid boxing
                throw new IllegalArgumentException("Out of range: " + value);
            }
            return (byte) value;
        }

        /**
         * Returns the value of the given byte as an integer, when treated as unsigned. That is, returns
         * {@code value + 256} if {@code value} is negative; {@code value} itself otherwise.
         *
         * @since 6.0
         */
        public static int toInt(byte value) {
            return value & UNSIGNED_MASK;
        }
    }

    public static final class UnsignedInts {
        static final long INT_MASK = 0xffffffffL;

        private UnsignedInts() {
            // singleton            
        }

        /**
         * Returns the value of the given {@code int} as a {@code long}, when treated as unsigned.
         */
        public static long toLong(int value) {
            return value & INT_MASK;
        }
    }

    public static final class LongMath {
        private LongMath() {
            // singleton         
        }

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

        static void checkRoundingUnnecessary(boolean condition) {
            if (!condition) {
                throw new ArithmeticException("mode was UNNECESSARY, but rounding was necessary");
            }
        }
    }

    public static void checkPositionIndexes(int start, int end, int size) {
        // Carefully optimized for execution by hotspot (explanatory comment above)
        if (start < 0 || end < start || end > size) {
            throw new IndexOutOfBoundsException(badPositionIndexes(start, end, size));
        }
    }

    private static String badPositionIndexes(int start, int end, int size) {
        if (start < 0 || start > size) {
            return badPositionIndex(start, size, "start index");
        }
        if (end < 0 || end > size) {
            return badPositionIndex(end, size, "end index");
        }
        // end < start
        return format("end index (%s) must not be less than start index (%s)", end, start);
    }

    public static int checkPositionIndex(int index, int size, String desc) {
        // Carefully optimized for execution by hotspot (explanatory comment above)
        if (index < 0 || index > size) {
            throw new IndexOutOfBoundsException(badPositionIndex(index, size, desc));
        }
        return index;
    }

    private static String badPositionIndex(int index, int size, String desc) {
        if (index < 0) {
            return format("%s (%s) must not be negative", desc, index);
        } else if (size < 0) {
            throw new IllegalArgumentException("negative size: " + size);
        } else { // index > size
            return format("%s (%s) must not be greater than size (%s)", desc, index, size);
        }
    }

    /**
     * Substitutes each {@code %s} in {@code template} with an argument. These are matched by
     * position: the first {@code %s} gets {@code args[0]}, etc.  If there are more arguments than
     * placeholders, the unmatched arguments will be appended to the end of the formatted message in
     * square braces.
     *
     * @param template a non-null string containing 0 or more {@code %s} placeholders.
     * @param args the arguments to be substituted into the message template. Arguments are converted
     *     to strings using {@link String#valueOf(Object)}. Arguments can be null.
     */
    // Note that this is somewhat-improperly used from Verify.java as well.
    static String format(String template, Object... args) {
        template = String.valueOf(template); // null -> "null"

        // start substituting the arguments into the '%s' placeholders
        StringBuilder builder = new StringBuilder(template.length() + 16 * args.length);
        int templateStart = 0;
        int i = 0;
        while (i < args.length) {
            int placeholderStart = template.indexOf("%s", templateStart);
            if (placeholderStart == -1) {
                break;
            }
            builder.append(template.substring(templateStart, placeholderStart));
            builder.append(args[i++]);
            templateStart = placeholderStart + 2;
        }
        builder.append(template.substring(templateStart));

        // if we run out of placeholders, append the extra args in square braces
        if (i < args.length) {
            builder.append(" [");
            builder.append(args[i++]);
            while (i < args.length) {
                builder.append(", ");
                builder.append(args[i++]);
            }
            builder.append(']');
        }

        return builder.toString();
    }
}

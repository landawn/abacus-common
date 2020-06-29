/*
 * Copyright (c) 2018, Haiyang Li.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.landawn.abacus.util;

import com.landawn.abacus.annotation.Beta;
import com.landawn.abacus.annotation.Internal;

public final class Primitives {

    // ...
    static final BiMap<Class<?>, Class<?>> PRIMITIVE_2_WRAPPER = new BiMap<>();

    static {
        PRIMITIVE_2_WRAPPER.put(boolean.class, Boolean.class);
        PRIMITIVE_2_WRAPPER.put(char.class, Character.class);
        PRIMITIVE_2_WRAPPER.put(byte.class, Byte.class);
        PRIMITIVE_2_WRAPPER.put(short.class, Short.class);
        PRIMITIVE_2_WRAPPER.put(int.class, Integer.class);
        PRIMITIVE_2_WRAPPER.put(long.class, Long.class);
        PRIMITIVE_2_WRAPPER.put(float.class, Float.class);
        PRIMITIVE_2_WRAPPER.put(double.class, Double.class);

        PRIMITIVE_2_WRAPPER.put(boolean[].class, Boolean[].class);
        PRIMITIVE_2_WRAPPER.put(char[].class, Character[].class);
        PRIMITIVE_2_WRAPPER.put(byte[].class, Byte[].class);
        PRIMITIVE_2_WRAPPER.put(short[].class, Short[].class);
        PRIMITIVE_2_WRAPPER.put(int[].class, Integer[].class);
        PRIMITIVE_2_WRAPPER.put(long[].class, Long[].class);
        PRIMITIVE_2_WRAPPER.put(float[].class, Float[].class);
        PRIMITIVE_2_WRAPPER.put(double[].class, Double[].class);
    }

    private Primitives() {
        // utility class.
    }

    /**
     * Checks if is primitive type.
     *
     * @param cls
     * @return true, if is primitive type
     */
    public static boolean isPrimitiveType(final Class<?> cls) {
        N.checkArgNotNull(cls, "cls");

        return N.typeOf(cls).isPrimitiveType();
    }

    /**
     * Checks if is wrapper type.
     *
     * @param cls
     * @return true, if is wrapper type
     */
    public static boolean isWrapperType(final Class<?> cls) {
        N.checkArgNotNull(cls, "cls");

        return N.typeOf(cls).isPrimitiveWrapper();
    }

    /**
     * Checks if is primitive array type.
     *
     * @param cls
     * @return true, if is primitive array type
     */
    public static boolean isPrimitiveArrayType(final Class<?> cls) {
        N.checkArgNotNull(cls, "cls");

        return N.typeOf(cls).isPrimitiveArray();
    }

    /**
     * Returns the corresponding wrapper type of {@code type} if it is a primitive type; otherwise
     * returns {@code type} itself. Idempotent.
     * 
     * <pre>
     *     wrap(int.class) == Integer.class
     *     wrap(Integer.class) == Integer.class
     *     wrap(String.class) == String.class
     * </pre>
     *
     * @param cls
     * @return
     */
    public static Class<?> wrap(final Class<?> cls) {
        N.checkArgNotNull(cls, "cls");

        final Class<?> wrapped = PRIMITIVE_2_WRAPPER.get(cls);

        return wrapped == null ? cls : wrapped;
    }

    /**
     * Returns the corresponding primitive type of {@code type} if it is a wrapper type; otherwise
     * returns {@code type} itself. Idempotent.
     * 
     * <pre>
     *     unwrap(Integer.class) == int.class
     *     unwrap(int.class) == int.class
     *     unwrap(String.class) == String.class
     * </pre>
     *
     * @param cls
     * @return
     */
    public static Class<?> unwrap(final Class<?> cls) {
        N.checkArgNotNull(cls, "cls");

        Class<?> unwrapped = PRIMITIVE_2_WRAPPER.getByValue(cls);

        return unwrapped == null ? cls : unwrapped;
    }

    //    public static boolean unboxOrDefault(Boolean b) {
    //        if (b == null) {
    //            return false;
    //        }
    //
    //        return b.booleanValue();
    //    }
    //
    //    public static boolean unboxOrDefault(Boolean b, boolean defaultForNull) {
    //        if (b == null) {
    //            return defaultForNull;
    //        }
    //
    //        return b.booleanValue();
    //    }
    //
    //    public static char unboxOrDefault(Character c) {
    //        if (c == null) {
    //            return N.CHAR_0;
    //        }
    //
    //        return c.charValue();
    //    }
    //
    //    public static char unboxOrDefault(Character c, char defaultForNull) {
    //        if (c == null) {
    //            return defaultForNull;
    //        }
    //
    //        return c.charValue();
    //    }
    //
    //    public static byte unboxOrDefault(Byte b) {
    //        if (b == null) {
    //            return (byte) 0;
    //        }
    //
    //        return b.byteValue();
    //    }
    //
    //    public static byte unboxOrDefault(Byte b, byte defaultForNull) {
    //        if (b == null) {
    //            return defaultForNull;
    //        }
    //
    //        return b.byteValue();
    //    }
    //
    //    public static short unboxOrDefault(Short b) {
    //        if (b == null) {
    //            return (short) 0;
    //        }
    //
    //        return b.shortValue();
    //    }
    //
    //    public static short unboxOrDefault(Short b, short defaultForNull) {
    //        if (b == null) {
    //            return defaultForNull;
    //        }
    //
    //        return b.shortValue();
    //    }
    //
    //    public static int unboxOrDefault(Integer b) {
    //        if (b == null) {
    //            return 0;
    //        }
    //
    //        return b.intValue();
    //    }
    //
    //    public static int unboxOrDefault(Integer b, int defaultForNull) {
    //        if (b == null) {
    //            return defaultForNull;
    //        }
    //
    //        return b.intValue();
    //    }
    //
    //    public static long unboxOrDefault(Long b) {
    //        if (b == null) {
    //            return 0;
    //        }
    //
    //        return b.longValue();
    //    }
    //
    //    public static long unboxOrDefault(Long b, long defaultForNull) {
    //        if (b == null) {
    //            return defaultForNull;
    //        }
    //
    //        return b.longValue();
    //    }
    //
    //    public static float unboxOrDefault(Float b) {
    //        if (b == null) {
    //            return 0;
    //        }
    //
    //        return b.floatValue();
    //    }
    //
    //    public static float unboxOrDefault(Float b, float defaultForNull) {
    //        if (b == null) {
    //            return defaultForNull;
    //        }
    //
    //        return b.floatValue();
    //    }
    //
    //    public static double unboxOrDefault(Double b) {
    //        if (b == null) {
    //            return 0;
    //        }
    //
    //        return b.doubleValue();
    //    }
    //
    //    public static double unboxOrDefault(Double b, double defaultForNull) {
    //        if (b == null) {
    //            return defaultForNull;
    //        }
    //
    //        return b.doubleValue();
    //    }
    //
    //    public static boolean unboxOrGet(Boolean b, BooleanSupplier supplierForNull) {
    //        if (b == null) {
    //            return supplierForNull.getAsBoolean();
    //        }
    //
    //        return b.booleanValue();
    //    }
    //
    //    public static char unboxOrGet(Character b, CharSupplier supplierForNull) {
    //        if (b == null) {
    //            return supplierForNull.getAsChar();
    //        }
    //
    //        return b.charValue();
    //    }
    //
    //    public static byte unboxOrGet(Byte b, ByteSupplier supplierForNull) {
    //        if (b == null) {
    //            return supplierForNull.getAsByte();
    //        }
    //
    //        return b.byteValue();
    //    }
    //
    //    public static short unboxOrGet(Short b, ShortSupplier supplierForNull) {
    //        if (b == null) {
    //            return supplierForNull.getAsShort();
    //        }
    //
    //        return b.shortValue();
    //    }
    //
    //    public static int unboxOrGet(Integer b, IntSupplier supplierForNull) {
    //        if (b == null) {
    //            return supplierForNull.getAsInt();
    //        }
    //
    //        return b.intValue();
    //    }
    //
    //    public static long unboxOrGet(Long b, LongSupplier supplierForNull) {
    //        if (b == null) {
    //            return supplierForNull.getAsLong();
    //        }
    //
    //        return b.longValue();
    //    }
    //
    //    public static float unboxOrGet(Float b, FloatSupplier supplierForNull) {
    //        if (b == null) {
    //            return supplierForNull.getAsFloat();
    //        }
    //
    //        return b.floatValue();
    //    }
    //
    //    public static double unboxOrGet(Double b, DoubleSupplier supplierForNull) {
    //        if (b == null) {
    //            return supplierForNull.getAsDouble();
    //        }
    //
    //        return b.doubleValue();
    //    }

    /**
     * Checks if is null or default. {@code null} is default value for all reference types, {@code false} is default value for primitive boolean, {@code 0} is the default value for primitive number type.
     *
     * @param s
     * @return true, if is null or default
     */
    @Internal
    @Beta
    static boolean isNullOrDefault(final Object value) {
        return (value == null) || N.equals(value, N.defaultValueOf(value.getClass()));
    }

    /**
     * Checks if it's not null or default. {@code null} is default value for all reference types, {@code false} is default value for primitive boolean, {@code 0} is the default value for primitive number type.
     *
     *
     * @param s
     * @return true, if it's not null or default
     */
    @Internal
    @Beta
    static boolean notNullOrDefault(final Object value) {
        return (value != null) && !N.equals(value, N.defaultValueOf(value.getClass()));
    }

    /**
     * The Class Booleans.
     */
    // TODO
    public static final class Booleans {

        /**
         * Instantiates a new booleans.
         */
        private Booleans() {
            // utility class.
        }

        /**
         * Inverts the element from {@code fromIndex} to {@code toIndex}: set it to {@code true} if it's {@code false}, or set it to {@code false} if it's {@code true}.
         *
         * @param a
         */
        public static void invert(final boolean[] a) {
            if (N.isNullOrEmpty(a)) {
                return;
            }

            invert(a, 0, a.length);
        }

        /**
         * Inverts the element from {@code fromIndex} to {@code toIndex}: set it to {@code true} if it's {@code false}, or set it to {@code false} if it's {@code true}.
         *
         * @param a
         * @param fromIndex
         * @param toIndex
         */
        public static void invert(final boolean[] a, final int fromIndex, final int toIndex) {
            N.checkFromToIndex(fromIndex, toIndex, N.len(a));

            if (fromIndex == toIndex) {
                return;
            }

            for (int i = fromIndex; i < toIndex; i++) {
                a[i] = !a[i];
            }
        }
    }

    /**
     * The Class Chars.
     */
    // TODO
    public static final class Chars {

        /**
         * Instantiates a new chars.
         */
        private Chars() {
            // utility class.
        }
    }

    /**
     * The Class Bytes.
     */
    // TODO
    public static final class Bytes {

        /**
         * Instantiates a new bytes.
         */
        private Bytes() {
            // utility class.
        }
    }

    /**
     * The Class Shorts.
     */
    // TODO
    public static final class Shorts {

        /**
         * Instantiates a new shorts.
         */
        private Shorts() {
            // utility class.
        }
    }

    /**
     * The Class Ints.
     */
    // TODO
    public static final class Ints {

        /**
         * Instantiates a new ints.
         */
        private Ints() {
            // utility class.
        }
    }

    /**
     * The Class Longs.
     */
    // TODO
    public static final class Longs {

        /**
         * Instantiates a new longs.
         */
        private Longs() {
            // utility class.
        }
    }

    /**
     * The Class Floats.
     */
    // TODO
    public static final class Floats {

        /**
         * Instantiates a new floats.
         */
        private Floats() {
            // utility class.
        }
    }

    /**
     * The Class Doubles.
     */
    // TODO
    public static final class Doubles {

        /**
         * Instantiates a new doubles.
         */
        private Doubles() {
            // utility class.
        }
    }
}

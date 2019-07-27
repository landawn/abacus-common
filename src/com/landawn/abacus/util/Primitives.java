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

    public static boolean isPrimitiveType(final Class<?> cls) {
        N.checkArgNotNull(cls, "cls");

        return N.typeOf(cls).isPrimitiveType();
    }

    public static boolean isWrapperType(final Class<?> cls) {
        N.checkArgNotNull(cls, "cls");

        return N.typeOf(cls).isPrimitiveWrapper();
    }

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
     */
    public static Class<?> unwrap(final Class<?> cls) {
        N.checkArgNotNull(cls, "cls");

        Class<?> unwrapped = PRIMITIVE_2_WRAPPER.getByValue(cls);

        return unwrapped == null ? cls : unwrapped;
    }

    /**
     * <p>
     * Converts an array of primitive booleans to objects.
     * </p>
     *
     * <p>
     * This method returns {@code null} for a {@code null} input array.
     * </p>
     *
     * @param a
     *            a {@code boolean} array
     * @return a {@code Boolean} array, {@code null} if null array input
     */
    @SafeVarargs
    public static Boolean[] box(final boolean... a) {
        if (a == null) {
            return null;
        }

        return box(a, 0, a.length);
    }

    public static Boolean[] box(final boolean[] a, final int fromIndex, final int toIndex) {
        if (a == null) {
            return null;
        } else if (toIndex - fromIndex == 0) {
            return N.EMPTY_BOOLEAN_OBJECT_ARRAY;
        }

        final Boolean[] result = new Boolean[toIndex - fromIndex];

        for (int i = 0, j = fromIndex; j < toIndex; i++, j++) {
            result[i] = Boolean.valueOf(a[j]);
        }

        return result;
    }

    /**
     * <p>
     * Converts an array of primitive chars to objects.
     * </p>
     *
     * <p>
     * This method returns {@code null} for a {@code null} input array.
     * </p>
     *
     * @param a
     *            a {@code char} array
     * @return a {@code Character} array, {@code null} if null array input
     */
    @SafeVarargs
    public static Character[] box(final char... a) {
        if (a == null) {
            return null;
        }

        return box(a, 0, a.length);
    }

    public static Character[] box(final char[] a, final int fromIndex, final int toIndex) {
        if (a == null) {
            return null;
        } else if (toIndex - fromIndex == 0) {
            return N.EMPTY_CHARACTER_OBJECT_ARRAY;
        }

        final Character[] result = new Character[toIndex - fromIndex];

        for (int i = 0, j = fromIndex; j < toIndex; i++, j++) {
            result[i] = Character.valueOf(a[j]);
        }

        return result;
    }

    /**
     * <p>
     * Converts an array of primitive bytes to objects.
     * </p>
     *
     * <p>
     * This method returns {@code null} for a {@code null} input array.
     * </p>
     *
     * @param a
     *            a {@code byte} array
     * @return a {@code Byte} array, {@code null} if null array input
     */
    @SafeVarargs
    public static Byte[] box(final byte... a) {
        if (a == null) {
            return null;
        }

        return box(a, 0, a.length);
    }

    public static Byte[] box(final byte[] a, final int fromIndex, final int toIndex) {
        if (a == null) {
            return null;
        } else if (toIndex - fromIndex == 0) {
            return N.EMPTY_BYTE_OBJECT_ARRAY;
        }

        final Byte[] result = new Byte[toIndex - fromIndex];

        for (int i = 0, j = fromIndex; j < toIndex; i++, j++) {
            result[i] = Byte.valueOf(a[j]);
        }

        return result;
    }

    /**
     * <p>
     * Converts an array of primitive shorts to objects.
     * </p>
     *
     * <p>
     * This method returns {@code null} for a {@code null} input array.
     * </p>
     *
     * @param a
     *            a {@code short} array
     * @return a {@code Short} array, {@code null} if null array input
     */
    @SafeVarargs
    public static Short[] box(final short... a) {
        if (a == null) {
            return null;
        }

        return box(a, 0, a.length);
    }

    public static Short[] box(final short[] a, final int fromIndex, final int toIndex) {
        if (a == null) {
            return null;
        } else if (toIndex - fromIndex == 0) {
            return N.EMPTY_SHORT_OBJECT_ARRAY;
        }

        final Short[] result = new Short[toIndex - fromIndex];

        for (int i = 0, j = fromIndex; j < toIndex; i++, j++) {
            result[i] = Short.valueOf(a[j]);
        }

        return result;
    }

    /**
     * <p>
     * Converts an array of primitive ints to objects.
     * </p>
     *
     * <p>
     * This method returns {@code null} for a {@code null} input array.
     * </p>
     *
     * @param a
     *            an {@code int} array
     * @return an {@code Integer} array, {@code null} if null array input
     */
    @SafeVarargs
    public static Integer[] box(final int... a) {
        if (a == null) {
            return null;
        }

        return box(a, 0, a.length);
    }

    public static Integer[] box(final int[] a, final int fromIndex, final int toIndex) {
        if (a == null) {
            return null;
        } else if (toIndex - fromIndex == 0) {
            return N.EMPTY_INTEGER_OBJECT_ARRAY;
        }

        final Integer[] result = new Integer[toIndex - fromIndex];

        for (int i = 0, j = fromIndex; j < toIndex; i++, j++) {
            result[i] = Integer.valueOf(a[j]);
        }

        return result;
    }

    /**
     * <p>
     * Converts an array of primitive longs to objects.
     * </p>
     *
     * <p>
     * This method returns {@code null} for a {@code null} input array.
     * </p>
     *
     * @param a
     *            a {@code long} array
     * @return a {@code Long} array, {@code null} if null array input
     */
    @SafeVarargs
    public static Long[] box(final long... a) {
        if (a == null) {
            return null;
        }

        return box(a, 0, a.length);
    }

    public static Long[] box(final long[] a, final int fromIndex, final int toIndex) {
        if (a == null) {
            return null;
        } else if (toIndex - fromIndex == 0) {
            return N.EMPTY_LONG_OBJECT_ARRAY;
        }

        final Long[] result = new Long[toIndex - fromIndex];

        for (int i = 0, j = fromIndex; j < toIndex; i++, j++) {
            result[i] = Long.valueOf(a[j]);
        }

        return result;
    }

    /**
     * <p>
     * Converts an array of primitive floats to objects.
     * </p>
     *
     * <p>
     * This method returns {@code null} for a {@code null} input array.
     * </p>
     *
     * @param a
     *            a {@code float} array
     * @return a {@code Float} array, {@code null} if null array input
     */
    @SafeVarargs
    public static Float[] box(final float... a) {
        if (a == null) {
            return null;
        }

        return box(a, 0, a.length);
    }

    public static Float[] box(final float[] a, final int fromIndex, final int toIndex) {
        if (a == null) {
            return null;
        } else if (toIndex - fromIndex == 0) {
            return N.EMPTY_FLOAT_OBJECT_ARRAY;
        }

        final Float[] result = new Float[toIndex - fromIndex];

        for (int i = 0, j = fromIndex; j < toIndex; i++, j++) {
            result[i] = Float.valueOf(a[j]);
        }

        return result;
    }

    /**
     * <p>
     * Converts an array of primitive doubles to objects.
     * </p>
     *
     * <p>
     * This method returns {@code null} for a {@code null} input array.
     * </p>
     *
     * @param a
     *            a {@code double} array
     * @return a {@code Double} array, {@code null} if null array input
     */
    @SafeVarargs
    public static Double[] box(final double... a) {
        if (a == null) {
            return null;
        }

        return box(a, 0, a.length);
    }

    public static Double[] box(final double[] a, final int fromIndex, final int toIndex) {
        if (a == null) {
            return null;
        } else if (toIndex - fromIndex == 0) {
            return N.EMPTY_DOUBLE_OBJECT_ARRAY;
        }

        final Double[] result = new Double[toIndex - fromIndex];

        for (int i = 0, j = fromIndex; j < toIndex; i++, j++) {
            result[i] = Double.valueOf(a[j]);
        }

        return result;
    }

    public static Boolean[][] box(boolean[][] a) {
        if (a == null) {
            return null;
        }

        final Boolean[][] result = new Boolean[a.length][];

        for (int i = 0, len = a.length; i < len; i++) {
            result[i] = box(a[i]);
        }

        return result;
    }

    public static Character[][] box(char[][] a) {
        if (a == null) {
            return null;
        }

        final Character[][] result = new Character[a.length][];

        for (int i = 0, len = a.length; i < len; i++) {
            result[i] = box(a[i]);
        }

        return result;
    }

    public static Byte[][] box(byte[][] a) {
        if (a == null) {
            return null;
        }

        final Byte[][] result = new Byte[a.length][];

        for (int i = 0, len = a.length; i < len; i++) {
            result[i] = box(a[i]);
        }

        return result;
    }

    public static Short[][] box(short[][] a) {
        if (a == null) {
            return null;
        }

        final Short[][] result = new Short[a.length][];

        for (int i = 0, len = a.length; i < len; i++) {
            result[i] = box(a[i]);
        }

        return result;
    }

    public static Integer[][] box(int[][] a) {
        if (a == null) {
            return null;
        }

        final Integer[][] result = new Integer[a.length][];

        for (int i = 0, len = a.length; i < len; i++) {
            result[i] = box(a[i]);
        }

        return result;
    }

    public static Long[][] box(long[][] a) {
        if (a == null) {
            return null;
        }

        final Long[][] result = new Long[a.length][];

        for (int i = 0, len = a.length; i < len; i++) {
            result[i] = box(a[i]);
        }

        return result;
    }

    public static Float[][] box(float[][] a) {
        if (a == null) {
            return null;
        }

        final Float[][] result = new Float[a.length][];

        for (int i = 0, len = a.length; i < len; i++) {
            result[i] = box(a[i]);
        }

        return result;
    }

    public static Double[][] box(double[][] a) {
        if (a == null) {
            return null;
        }

        final Double[][] result = new Double[a.length][];

        for (int i = 0, len = a.length; i < len; i++) {
            result[i] = box(a[i]);
        }

        return result;
    }

    public static Boolean[][][] box(boolean[][][] a) {
        if (a == null) {
            return null;
        }

        final Boolean[][][] result = new Boolean[a.length][][];

        for (int i = 0, len = a.length; i < len; i++) {
            result[i] = box(a[i]);
        }

        return result;
    }

    public static Character[][][] box(char[][][] a) {
        if (a == null) {
            return null;
        }

        final Character[][][] result = new Character[a.length][][];

        for (int i = 0, len = a.length; i < len; i++) {
            result[i] = box(a[i]);
        }

        return result;
    }

    public static Byte[][][] box(byte[][][] a) {
        if (a == null) {
            return null;
        }

        final Byte[][][] result = new Byte[a.length][][];

        for (int i = 0, len = a.length; i < len; i++) {
            result[i] = box(a[i]);
        }

        return result;
    }

    public static Short[][][] box(short[][][] a) {
        if (a == null) {
            return null;
        }

        final Short[][][] result = new Short[a.length][][];

        for (int i = 0, len = a.length; i < len; i++) {
            result[i] = box(a[i]);
        }

        return result;
    }

    public static Integer[][][] box(int[][][] a) {
        if (a == null) {
            return null;
        }

        final Integer[][][] result = new Integer[a.length][][];

        for (int i = 0, len = a.length; i < len; i++) {
            result[i] = box(a[i]);
        }

        return result;
    }

    public static Long[][][] box(long[][][] a) {
        if (a == null) {
            return null;
        }

        final Long[][][] result = new Long[a.length][][];

        for (int i = 0, len = a.length; i < len; i++) {
            result[i] = box(a[i]);
        }

        return result;
    }

    public static Float[][][] box(float[][][] a) {
        if (a == null) {
            return null;
        }

        final Float[][][] result = new Float[a.length][][];

        for (int i = 0, len = a.length; i < len; i++) {
            result[i] = box(a[i]);
        }

        return result;
    }

    public static Double[][][] box(double[][][] a) {
        if (a == null) {
            return null;
        }

        final Double[][][] result = new Double[a.length][][];

        for (int i = 0, len = a.length; i < len; i++) {
            result[i] = box(a[i]);
        }

        return result;
    }

    static <T> T box(final Object a) {
        if (a == null) {
            return null;
        }

        return box(a, 0, Array.getLength(a));
    }

    static <T> T box(final Object a, final int fromIndex, final int toIndex) {
        if (a == null) {
            return null;
        }

        final Class<?> cls = a.getClass();
        final Integer enumInt = N.CLASS_TYPE_ENUM.get(cls);

        if (enumInt == null) {
            throw new IllegalArgumentException(ClassUtil.getCanonicalClassName(cls) + " is not a primitive array");
        }

        switch (enumInt) {
            case 11:
                return (T) Primitives.box((boolean[]) a, fromIndex, toIndex);

            case 12:
                return (T) Primitives.box((char[]) a, fromIndex, toIndex);

            case 13:
                return (T) Primitives.box((byte[]) a, fromIndex, toIndex);

            case 14:
                return (T) Primitives.box((short[]) a, fromIndex, toIndex);

            case 15:
                return (T) Primitives.box((int[]) a, fromIndex, toIndex);

            case 16:
                return (T) Primitives.box((long[]) a, fromIndex, toIndex);

            case 17:
                return (T) Primitives.box((float[]) a, fromIndex, toIndex);

            case 18:
                return (T) Primitives.box((double[]) a, fromIndex, toIndex);

            default:
                throw new IllegalArgumentException(ClassUtil.getCanonicalClassName(cls) + " is not a primitive array");
        }
    }

    static <T> T unbox(final Object a) {
        if (a == null) {
            return null;
        }

        return unbox(a, null);
    }

    static <T> T unbox(final Object a, final Object valueForNull) {
        if (a == null) {
            return null;
        }

        return unbox(a, 0, Array.getLength(a), valueForNull);
    }

    static <T> T unbox(final Object a, final int fromIndex, final int toIndex, final Object valueForNull) {
        if (a == null) {
            return null;
        }

        final Class<?> cls = Primitives.unwrap(a.getClass());
        final Object defaultValue = valueForNull == null ? N.defaultValueOf(cls.getComponentType()) : valueForNull;
        final Integer enumInt = N.CLASS_TYPE_ENUM.get(cls);

        if (enumInt == null) {
            throw new IllegalArgumentException(ClassUtil.getCanonicalClassName(a.getClass()) + " is not a wrapper of primitive array");
        }

        switch (enumInt) {
            case 11:
                return (T) Primitives.unbox((Boolean[]) a, fromIndex, toIndex, ((Boolean) defaultValue).booleanValue());

            case 12:
                return (T) Primitives.unbox((Character[]) a, fromIndex, toIndex, ((Character) defaultValue).charValue());

            case 13:
                return (T) Primitives.unbox((Byte[]) a, fromIndex, toIndex, ((Number) defaultValue).byteValue());

            case 14:
                return (T) Primitives.unbox((Short[]) a, fromIndex, toIndex, ((Number) defaultValue).shortValue());

            case 15:
                return (T) Primitives.unbox((Integer[]) a, fromIndex, toIndex, ((Number) defaultValue).intValue());

            case 16:
                return (T) Primitives.unbox((Long[]) a, fromIndex, toIndex, ((Number) defaultValue).longValue());

            case 17:
                return (T) Primitives.unbox((Float[]) a, fromIndex, toIndex, ((Number) defaultValue).floatValue());

            case 18:
                return (T) Primitives.unbox((Double[]) a, fromIndex, toIndex, ((Number) defaultValue).doubleValue());

            default:
                throw new IllegalArgumentException(ClassUtil.getCanonicalClassName(a.getClass()) + " is not a wrapper of primitive array");
        }
    }

    // Boolean array converters
    // ----------------------------------------------------------------------
    /**
     * <p>
     * Converts an array of object Booleans to primitives.
     * </p>
     *
     * <p>
     * This method returns {@code null} for a {@code null} input array.
     * </p>
     *
     * @param a
     *            a {@code Boolean} array, may be {@code null}
     * @return a {@code boolean} array, {@code null} if null array input
     */
    @SafeVarargs
    public static boolean[] unbox(final Boolean... a) {
        return unbox(a, false);
    }

    /**
     * <p>
     * Converts an array of object Booleans to primitives handling {@code null}.
     * </p>
     *
     * <p>
     * This method returns {@code null} for a {@code null} input array.
     * </p>
     *
     * @param a
     *            a {@code Boolean} array, may be {@code null}
     * @param valueForNull
     *            the value to insert if {@code null} found
     * @return a {@code boolean} array, {@code null} if null array input
     */
    public static boolean[] unbox(final Boolean[] a, final boolean valueForNull) {
        if (a == null) {
            return null;
        }

        return unbox(a, 0, a.length, valueForNull);
    }

    public static boolean[] unbox(final Boolean[] a, final int fromIndex, final int toIndex, final boolean valueForNull) {
        if (a == null) {
            return null;
        } else if (toIndex - fromIndex == 0) {
            return N.EMPTY_BOOLEAN_ARRAY;
        }

        final boolean[] result = new boolean[toIndex - fromIndex];

        for (int i = 0, j = fromIndex; j < toIndex; i++, j++) {
            result[i] = (a[j] == null ? valueForNull : a[j].booleanValue());
        }

        return result;
    }

    // Character array converters
    // ----------------------------------------------------------------------
    /**
     * <p>
     * Converts an array of object Characters to primitives.
     * </p>
     *
     * <p>
     * This method returns {@code null} for a {@code null} input array.
     * </p>
     *
     * @param a
     *            a {@code Character} array, may be {@code null}
     * @return a {@code char} array, {@code null} if null array input
     */
    @SafeVarargs
    public static char[] unbox(final Character... a) {
        return unbox(a, (char) 0);
    }

    /**
     * <p>
     * Converts an array of object Character to primitives handling {@code null}
     * .
     * </p>
     *
     * <p>
     * This method returns {@code null} for a {@code null} input array.
     * </p>
     *
     * @param a
     *            a {@code Character} array, may be {@code null}
     * @param valueForNull
     *            the value to insert if {@code null} found
     * @return a {@code char} array, {@code null} if null array input
     */
    public static char[] unbox(final Character[] a, final char valueForNull) {
        if (a == null) {
            return null;
        }

        return unbox(a, 0, a.length, valueForNull);
    }

    public static char[] unbox(final Character[] a, final int fromIndex, final int toIndex, final char valueForNull) {
        if (a == null) {
            return null;
        } else if (toIndex - fromIndex == 0) {
            return N.EMPTY_CHAR_ARRAY;
        }

        final char[] result = new char[toIndex - fromIndex];

        for (int i = 0, j = fromIndex; j < toIndex; i++, j++) {
            result[i] = (a[j] == null ? valueForNull : a[j].charValue());
        }

        return result;
    }

    // Byte array converters
    // ----------------------------------------------------------------------
    /**
     * <p>
     * Converts an array of object Bytes to primitives.
     * </p>
     *
     * <p>
     * This method returns {@code null} for a {@code null} input array.
     * </p>
     *
     * @param a
     *            a {@code Byte} array, may be {@code null}
     * @return a {@code byte} array, {@code null} if null array input
     */
    @SafeVarargs
    public static byte[] unbox(final Byte... a) {
        return unbox(a, (byte) 0);
    }

    /**
     * <p>
     * Converts an array of object Bytes to primitives handling {@code null}.
     * </p>
     *
     * <p>
     * This method returns {@code null} for a {@code null} input array.
     * </p>
     *
     * @param a
     *            a {@code Byte} array, may be {@code null}
     * @param valueForNull
     *            the value to insert if {@code null} found
     * @return a {@code byte} array, {@code null} if null array input
     */
    public static byte[] unbox(final Byte[] a, final byte valueForNull) {
        if (a == null) {
            return null;
        }

        return unbox(a, 0, a.length, valueForNull);
    }

    public static byte[] unbox(final Byte[] a, final int fromIndex, final int toIndex, final byte valueForNull) {
        if (a == null) {
            return null;
        } else if (toIndex - fromIndex == 0) {
            return N.EMPTY_BYTE_ARRAY;
        }

        final byte[] result = new byte[toIndex - fromIndex];

        for (int i = 0, j = fromIndex; j < toIndex; i++, j++) {
            result[i] = (a[j] == null ? valueForNull : a[j].byteValue());
        }

        return result;
    }

    // Short array converters
    // ----------------------------------------------------------------------
    /**
     * <p>
     * Converts an array of object Shorts to primitives.
     * </p>
     *
     * <p>
     * This method returns {@code null} for a {@code null} input array.
     * </p>
     *
     * @param a
     *            a {@code Short} array, may be {@code null}
     * @return a {@code byte} array, {@code null} if null array input
     */
    @SafeVarargs
    public static short[] unbox(final Short... a) {
        return unbox(a, (short) 0);
    }

    /**
     * <p>
     * Converts an array of object Short to primitives handling {@code null}.
     * </p>
     *
     * <p>
     * This method returns {@code null} for a {@code null} input array.
     * </p>
     *
     * @param a
     *            a {@code Short} array, may be {@code null}
     * @param valueForNull
     *            the value to insert if {@code null} found
     * @return a {@code byte} array, {@code null} if null array input
     */
    public static short[] unbox(final Short[] a, final short valueForNull) {
        if (a == null) {
            return null;
        }

        return unbox(a, 0, a.length, valueForNull);
    }

    public static short[] unbox(final Short[] a, final int fromIndex, final int toIndex, final short valueForNull) {
        if (a == null) {
            return null;
        } else if (toIndex - fromIndex == 0) {
            return N.EMPTY_SHORT_ARRAY;
        }

        final short[] result = new short[toIndex - fromIndex];

        for (int i = 0, j = fromIndex; j < toIndex; i++, j++) {
            result[i] = (a[j] == null ? valueForNull : a[j].shortValue());
        }

        return result;
    }

    // Int array converters
    // ----------------------------------------------------------------------
    /**
     * <p>
     * Converts an array of object Integers to primitives.
     * </p>
     *
     * <p>
     * This method returns {@code null} for a {@code null} input array.
     * </p>
     *
     * @param a
     *            a {@code Integer} array, may be {@code null}
     * @return an {@code int} array, {@code null} if null array input
     */
    @SafeVarargs
    public static int[] unbox(final Integer... a) {
        return unbox(a, 0);
    }

    /**
     * <p>
     * Converts an array of object Integer to primitives handling {@code null}.
     * </p>
     *
     * <p>
     * This method returns {@code null} for a {@code null} input array.
     * </p>
     *
     * @param a
     *            a {@code Integer} array, may be {@code null}
     * @param valueForNull
     *            the value to insert if {@code null} found
     * @return an {@code int} array, {@code null} if null array input
     */
    public static int[] unbox(final Integer[] a, final int valueForNull) {
        if (a == null) {
            return null;
        }

        return unbox(a, 0, a.length, valueForNull);
    }

    public static int[] unbox(final Integer[] a, final int fromIndex, final int toIndex, final int valueForNull) {
        if (a == null) {
            return null;
        } else if (toIndex - fromIndex == 0) {
            return N.EMPTY_INT_ARRAY;
        }

        final int[] result = new int[toIndex - fromIndex];

        for (int i = 0, j = fromIndex; j < toIndex; i++, j++) {
            result[i] = (a[j] == null ? valueForNull : a[j].intValue());
        }

        return result;
    }

    // Long array converters
    // ----------------------------------------------------------------------
    /**
     * <p>
     * Converts an array of object Longs to primitives.
     * </p>
     *
     * <p>
     * This method returns {@code null} for a {@code null} input array.
     * </p>
     *
     * @param a
     *            a {@code Long} array, may be {@code null}
     * @return a {@code long} array, {@code null} if null array input
     */
    @SafeVarargs
    public static long[] unbox(final Long... a) {
        return unbox(a, 0L);
    }

    /**
     * <p>
     * Converts an array of object Long to primitives handling {@code null}.
     * </p>
     *
     * <p>
     * This method returns {@code null} for a {@code null} input array.
     * </p>
     *
     * @param a
     *            a {@code Long} array, may be {@code null}
     * @param valueForNull
     *            the value to insert if {@code null} found
     * @return a {@code long} array, {@code null} if null array input
     */
    public static long[] unbox(final Long[] a, final long valueForNull) {
        if (a == null) {
            return null;
        }

        return unbox(a, 0, a.length, valueForNull);
    }

    public static long[] unbox(final Long[] a, final int fromIndex, final int toIndex, final long valueForNull) {
        if (a == null) {
            return null;
        } else if (toIndex - fromIndex == 0) {
            return N.EMPTY_LONG_ARRAY;
        }

        final long[] result = new long[toIndex - fromIndex];

        for (int i = 0, j = fromIndex; j < toIndex; i++, j++) {
            result[i] = (a[j] == null ? valueForNull : a[j].longValue());
        }

        return result;
    }

    // Float array converters
    // ----------------------------------------------------------------------
    /**
     * <p>
     * Converts an array of object Floats to primitives.
     * </p>
     *
     * <p>
     * This method returns {@code null} for a {@code null} input array.
     * </p>
     *
     * @param a
     *            a {@code Float} array, may be {@code null}
     * @return a {@code float} array, {@code null} if null array input
     */
    @SafeVarargs
    public static float[] unbox(final Float... a) {
        return unbox(a, 0f);
    }

    /**
     * <p>
     * Converts an array of object Floats to primitives handling {@code null}.
     * </p>
     *
     * <p>
     * This method returns {@code null} for a {@code null} input array.
     * </p>
     *
     * @param a
     *            a {@code Float} array, may be {@code null}
     * @param valueForNull
     *            the value to insert if {@code null} found
     * @return a {@code float} array, {@code null} if null array input
     */
    public static float[] unbox(final Float[] a, final float valueForNull) {
        if (a == null) {
            return null;
        }

        return unbox(a, 0, a.length, valueForNull);
    }

    public static float[] unbox(final Float[] a, final int fromIndex, final int toIndex, final float valueForNull) {
        if (a == null) {
            return null;
        } else if (toIndex - fromIndex == 0) {
            return N.EMPTY_FLOAT_ARRAY;
        }

        final float[] result = new float[toIndex - fromIndex];

        for (int i = 0, j = fromIndex; j < toIndex; i++, j++) {
            result[i] = (a[j] == null ? valueForNull : a[j].floatValue());
        }

        return result;
    }

    // Double array converters
    // ----------------------------------------------------------------------
    /**
     * <p>
     * Converts an array of object Doubles to primitives.
     * </p>
     *
     * <p>
     * This method returns {@code null} for a {@code null} input array.
     * </p>
     *
     * @param a
     *            a {@code Double} array, may be {@code null}
     * @return a {@code double} array, {@code null} if null array input
     */
    @SafeVarargs
    public static double[] unbox(final Double... a) {
        return unbox(a, 0d);
    }

    /**
     * <p>
     * Converts an array of object Doubles to primitives handling {@code null}.
     * </p>
     *
     * <p>
     * This method returns {@code null} for a {@code null} input array.
     * </p>
     *
     * @param a
     *            a {@code Double} array, may be {@code null}
     * @param valueForNull
     *            the value to insert if {@code null} found
     * @return a {@code double} array, {@code null} if null array input
     */
    public static double[] unbox(final Double[] a, final double valueForNull) {
        if (a == null) {
            return null;
        }

        return unbox(a, 0, a.length, valueForNull);
    }

    public static double[] unbox(final Double[] a, final int fromIndex, final int toIndex, final double valueForNull) {
        if (a == null) {
            return null;
        } else if (toIndex - fromIndex == 0) {
            return N.EMPTY_DOUBLE_ARRAY;
        }

        final double[] result = new double[toIndex - fromIndex];

        for (int i = 0, j = fromIndex; j < toIndex; i++, j++) {
            result[i] = (a[j] == null ? valueForNull : a[j].doubleValue());
        }

        return result;
    }

    public static boolean[][] unbox(Boolean[][] a) {
        return unbox(a, false);
    }

    public static boolean[][] unbox(Boolean[][] a, boolean valueForNull) {
        if (a == null) {
            return null;
        }

        final boolean[][] result = new boolean[a.length][];

        for (int i = 0, len = a.length; i < len; i++) {
            result[i] = unbox(a[i], valueForNull);
        }

        return result;
    }

    public static char[][] unbox(Character[][] a) {
        return unbox(a, (char) 0);
    }

    public static char[][] unbox(Character[][] a, char valueForNull) {
        if (a == null) {
            return null;
        }

        final char[][] result = new char[a.length][];

        for (int i = 0, len = a.length; i < len; i++) {
            result[i] = unbox(a[i], valueForNull);
        }

        return result;
    }

    public static byte[][] unbox(Byte[][] a) {
        return unbox(a, (byte) 0);
    }

    public static byte[][] unbox(Byte[][] a, byte valueForNull) {
        if (a == null) {
            return null;
        }

        final byte[][] result = new byte[a.length][];

        for (int i = 0, len = a.length; i < len; i++) {
            result[i] = unbox(a[i], valueForNull);
        }

        return result;
    }

    public static short[][] unbox(Short[][] a) {
        return unbox(a, (short) 0);
    }

    public static short[][] unbox(Short[][] a, short valueForNull) {
        if (a == null) {
            return null;
        }

        final short[][] result = new short[a.length][];

        for (int i = 0, len = a.length; i < len; i++) {
            result[i] = unbox(a[i], valueForNull);
        }

        return result;
    }

    public static int[][] unbox(Integer[][] a) {
        return unbox(a, 0);
    }

    public static int[][] unbox(Integer[][] a, int valueForNull) {
        if (a == null) {
            return null;
        }

        final int[][] result = new int[a.length][];

        for (int i = 0, len = a.length; i < len; i++) {
            result[i] = unbox(a[i], valueForNull);
        }

        return result;
    }

    public static long[][] unbox(Long[][] a) {
        return unbox(a, 0);
    }

    public static long[][] unbox(Long[][] a, long valueForNull) {
        if (a == null) {
            return null;
        }

        final long[][] result = new long[a.length][];

        for (int i = 0, len = a.length; i < len; i++) {
            result[i] = unbox(a[i], valueForNull);
        }

        return result;
    }

    public static float[][] unbox(Float[][] a) {
        return unbox(a, 0);
    }

    public static float[][] unbox(Float[][] a, float valueForNull) {
        if (a == null) {
            return null;
        }

        final float[][] result = new float[a.length][];

        for (int i = 0, len = a.length; i < len; i++) {
            result[i] = unbox(a[i], valueForNull);
        }

        return result;
    }

    public static double[][] unbox(Double[][] a) {
        return unbox(a, 0);
    }

    public static double[][] unbox(Double[][] a, double valueForNull) {
        if (a == null) {
            return null;
        }

        final double[][] result = new double[a.length][];

        for (int i = 0, len = a.length; i < len; i++) {
            result[i] = unbox(a[i], valueForNull);
        }

        return result;
    }

    public static boolean[][][] unbox(Boolean[][][] a) {
        return unbox(a, false);
    }

    public static boolean[][][] unbox(Boolean[][][] a, boolean valueForNull) {
        if (a == null) {
            return null;
        }

        final boolean[][][] result = new boolean[a.length][][];

        for (int i = 0, len = a.length; i < len; i++) {
            result[i] = unbox(a[i], valueForNull);
        }

        return result;
    }

    public static char[][][] unbox(Character[][][] a) {
        return unbox(a, (char) 0);
    }

    public static char[][][] unbox(Character[][][] a, char valueForNull) {
        if (a == null) {
            return null;
        }

        final char[][][] result = new char[a.length][][];

        for (int i = 0, len = a.length; i < len; i++) {
            result[i] = unbox(a[i], valueForNull);
        }

        return result;
    }

    public static byte[][][] unbox(Byte[][][] a) {
        return unbox(a, (byte) 0);
    }

    public static byte[][][] unbox(Byte[][][] a, byte valueForNull) {
        if (a == null) {
            return null;
        }

        final byte[][][] result = new byte[a.length][][];

        for (int i = 0, len = a.length; i < len; i++) {
            result[i] = unbox(a[i], valueForNull);
        }

        return result;
    }

    public static short[][][] unbox(Short[][][] a) {
        return unbox(a, (short) 0);
    }

    public static short[][][] unbox(Short[][][] a, short valueForNull) {
        if (a == null) {
            return null;
        }

        final short[][][] result = new short[a.length][][];

        for (int i = 0, len = a.length; i < len; i++) {
            result[i] = unbox(a[i], valueForNull);
        }

        return result;
    }

    public static int[][][] unbox(Integer[][][] a) {
        return unbox(a, 0);
    }

    public static int[][][] unbox(Integer[][][] a, int valueForNull) {
        if (a == null) {
            return null;
        }

        final int[][][] result = new int[a.length][][];

        for (int i = 0, len = a.length; i < len; i++) {
            result[i] = unbox(a[i], valueForNull);
        }

        return result;
    }

    public static long[][][] unbox(Long[][][] a) {
        return unbox(a, 0);
    }

    public static long[][][] unbox(Long[][][] a, long valueForNull) {
        if (a == null) {
            return null;
        }

        final long[][][] result = new long[a.length][][];

        for (int i = 0, len = a.length; i < len; i++) {
            result[i] = unbox(a[i], valueForNull);
        }

        return result;
    }

    public static float[][][] unbox(Float[][][] a) {
        return unbox(a, 0);
    }

    public static float[][][] unbox(Float[][][] a, float valueForNull) {
        if (a == null) {
            return null;
        }

        final float[][][] result = new float[a.length][][];

        for (int i = 0, len = a.length; i < len; i++) {
            result[i] = unbox(a[i], valueForNull);
        }

        return result;
    }

    public static double[][][] unbox(Double[][][] a) {
        return unbox(a, 0);
    }

    public static double[][][] unbox(Double[][][] a, double valueForNull) {
        if (a == null) {
            return null;
        }

        final double[][][] result = new double[a.length][][];

        for (int i = 0, len = a.length; i < len; i++) {
            result[i] = unbox(a[i], valueForNull);
        }

        return result;
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

    // TODO
    public static final class Booleans {

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

    // TODO
    public static final class Chars {

        private Chars() {
            // utility class.
        }
    }

    // TODO
    public static final class Bytes {

        private Bytes() {
            // utility class.
        }
    }

    // TODO
    public static final class Shorts {

        private Shorts() {
            // utility class.
        }
    }

    // TODO
    public static final class Ints {

        private Ints() {
            // utility class.
        }
    }

    // TODO
    public static final class Longs {

        private Longs() {
            // utility class.
        }
    }

    // TODO
    public static final class Floats {

        private Floats() {
            // utility class.
        }
    }

    // TODO
    public static final class Doubles {

        private Doubles() {
            // utility class.
        }
    }
}

/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.landawn.abacus.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.landawn.abacus.util.Array;

public final class HashEqualsToStringUtil {

    private HashEqualsToStringUtil() {
        //singleton
    }

    private static final int MAX_ARRAY_SIZE = Integer.MAX_VALUE - 8;
    private static final int MAX_HASH_LENGTH = (int) (MAX_ARRAY_SIZE / 1.25) - 1;
    private static final String NULL_STRING = "null";

    public static boolean equals(final boolean a, final boolean b) {
        return a == b;
    }

    public static boolean equals(final char a, final char b) {
        return a == b;
    }

    public static boolean equals(final byte a, final byte b) {
        return a == b;
    }

    public static boolean equals(final short a, final short b) {
        return a == b;
    }

    public static boolean equals(final int a, final int b) {
        return a == b;
    }

    public static boolean equals(final long a, final long b) {
        return a == b;
    }

    public static boolean equals(final float a, final float b) {
        return Float.compare(a, b) == 0;
    }

    public static boolean equals(final double a, final double b) {
        return Double.compare(a, b) == 0;
    }

    public static boolean equals(final String a, final String b) {
        return (a == null) ? b == null : (b == null ? false : a.length() == b.length() && a.equals(b));
    }

    public static boolean equals(final Object a, final Object b) {
        return (a == null) ? b == null : (b == null ? false : a.equals(b));
    }

    public static boolean equals(final boolean[] a, final boolean[] b) {
        return (a == null || b == null) ? a == b : (a.length == b.length && equals(a, 0, a.length, b));
    }

    public static boolean equals(final boolean[] a, final int fromIndex, final int toIndex, final boolean[] b) {
        if (a == b) {
            return true;
        }

        if ((a == null && b != null) || (a != null && b == null) || a.length < toIndex || b.length < toIndex) {
            return false;
        }

        for (int i = fromIndex; i < toIndex; i++) {
            if (a[i] != b[i]) {
                return false;
            }
        }

        return true;
    }

    public static boolean equals(final char[] a, final char[] b) {
        return (a == null || b == null) ? a == b : (a.length == b.length && equals(a, 0, a.length, b));
    }

    public static boolean equals(final char[] a, final int fromIndex, final int toIndex, final char[] b) {
        if (a == b) {
            return true;
        }

        if ((a == null && b != null) || (a != null && b == null) || a.length < toIndex || b.length < toIndex) {
            return false;
        }

        for (int i = fromIndex; i < toIndex; i++) {
            if (a[i] != b[i]) {
                return false;
            }
        }

        return true;
    }

    public static boolean equals(final byte[] a, final byte[] b) {
        return (a == null || b == null) ? a == b : (a.length == b.length && equals(a, 0, a.length, b));
    }

    public static boolean equals(final byte[] a, final int fromIndex, final int toIndex, final byte[] b) {
        if (a == b) {
            return true;
        }

        if ((a == null && b != null) || (a != null && b == null) || a.length < toIndex || b.length < toIndex) {
            return false;
        }

        for (int i = fromIndex; i < toIndex; i++) {
            if (a[i] != b[i]) {
                return false;
            }
        }

        return true;
    }

    public static boolean equals(final short[] a, final short[] b) {
        return (a == null || b == null) ? a == b : (a.length == b.length && equals(a, 0, a.length, b));
    }

    public static boolean equals(final short[] a, final int fromIndex, final int toIndex, final short[] b) {
        if (a == b) {
            return true;
        }

        if ((a == null && b != null) || (a != null && b == null) || a.length < toIndex || b.length < toIndex) {
            return false;
        }

        for (int i = fromIndex; i < toIndex; i++) {
            if (a[i] != b[i]) {
                return false;
            }
        }

        return true;
    }

    public static boolean equals(final int[] a, final int[] b) {
        return (a == null || b == null) ? a == b : (a.length == b.length && equals(a, 0, a.length, b));
    }

    public static boolean equals(final int[] a, final int fromIndex, final int toIndex, final int[] b) {
        if (a == b) {
            return true;
        }

        if ((a == null && b != null) || (a != null && b == null) || a.length < toIndex || b.length < toIndex) {
            return false;
        }

        for (int i = fromIndex; i < toIndex; i++) {
            if (a[i] != b[i]) {
                return false;
            }
        }

        return true;
    }

    public static boolean equals(final long[] a, final long[] b) {
        return (a == null || b == null) ? a == b : (a.length == b.length && equals(a, 0, a.length, b));
    }

    public static boolean equals(final long[] a, final int fromIndex, final int toIndex, final long[] b) {
        if (a == b) {
            return true;
        }

        if ((a == null && b != null) || (a != null && b == null) || a.length < toIndex || b.length < toIndex) {
            return false;
        }

        for (int i = fromIndex; i < toIndex; i++) {
            if (a[i] != b[i]) {
                return false;
            }
        }

        return true;
    }

    public static boolean equals(final float[] a, final float[] b) {
        return (a == null || b == null) ? a == b : (a.length == b.length && equals(a, 0, a.length, b));
    }

    public static boolean equals(final float[] a, final int fromIndex, final int toIndex, final float[] b) {
        if (a == b) {
            return true;
        }

        if ((a == null && b != null) || (a != null && b == null) || a.length < toIndex || b.length < toIndex) {
            return false;
        }

        for (int i = fromIndex; i < toIndex; i++) {
            if (Float.compare(a[i], b[i]) != 0) {
                return false;
            }
        }

        return true;
    }

    public static boolean equals(final double[] a, final double[] b) {
        return (a == null || b == null) ? a == b : (a.length == b.length && equals(a, 0, a.length, b));
    }

    public static boolean equals(final double[] a, final int fromIndex, final int toIndex, final double[] b) {
        if (a == b) {
            return true;
        }

        if ((a == null && b != null) || (a != null && b == null) || a.length < toIndex || b.length < toIndex) {
            return false;
        }

        for (int i = fromIndex; i < toIndex; i++) {
            if (Double.compare(a[i], b[i]) != 0) {
                return false;
            }
        }

        return true;
    }

    public static boolean equals(final Object[] a, final Object[] b) {
        return (a == null || b == null) ? a == b : (a.length == b.length && equals(a, 0, a.length, b));
    }

    public static boolean equals(final Object[] a, final int fromIndex, final int toIndex, final Object[] b) {
        if (a == b) {
            return true;
        }

        if ((a == null && b != null) || (a != null && b == null) || a.length < toIndex || b.length < toIndex) {
            return false;
        }

        for (int i = fromIndex; i < toIndex; i++) {
            if (!(a[i] == null ? b[i] == null : a[i].equals(b[i]))) {
                return false;
            }
        }

        return true;
    }

    public static int hashCode(final boolean value) {
        return value ? 1231 : 1237;
    }

    public static int hashCode(final char value) {
        return value;
    }

    public static int hashCode(final byte value) {
        return value;
    }

    public static int hashCode(final short value) {
        return value;
    }

    public static int hashCode(final int value) {
        return value;
    }

    public static int hashCode(final long value) {
        return (int) (value ^ (value >>> 32));
    }

    public static int hashCode(final float value) {
        return Float.floatToIntBits(value);
    }

    public static int hashCode(final double value) {
        final long bits = Double.doubleToLongBits(value);

        return (int) (bits ^ (bits >>> 32));
    }

    public static int hashCode(final Object obj) {
        if (obj == null) {
            return 0;
        }

        return obj.hashCode();
    }

    public static int hashCode(final boolean[] a) {
        return a == null ? 0 : hashCode(a, 0, a.length);
    }

    public static int hashCode(final boolean[] a, final int fromIndex, final int toIndex) {
        if (a == null) {
            return 0;
        }

        int result = 1;

        for (int i = fromIndex; i < toIndex; i++) {
            result = 31 * result + (a[i] ? 1231 : 1237);
        }

        return result;
    }

    public static int hashCode(final char[] a) {
        return a == null ? 0 : hashCode(a, 0, a.length);
    }

    public static int hashCode(final char[] a, final int fromIndex, final int toIndex) {
        if (a == null) {
            return 0;
        }

        int result = 1;

        for (int i = fromIndex; i < toIndex; i++) {
            result = 31 * result + a[i];
        }

        return result;
    }

    public static int hashCode(final byte[] a) {
        return a == null ? 0 : hashCode(a, 0, a.length);
    }

    public static int hashCode(final byte[] a, final int fromIndex, final int toIndex) {
        if (a == null) {
            return 0;
        }

        int result = 1;

        for (int i = fromIndex; i < toIndex; i++) {
            result = 31 * result + a[i];
        }

        return result;
    }

    public static int hashCode(final short[] a) {
        return a == null ? 0 : hashCode(a, 0, a.length);
    }

    public static int hashCode(final short[] a, final int fromIndex, final int toIndex) {
        if (a == null) {
            return 0;
        }

        int result = 1;

        for (int i = fromIndex; i < toIndex; i++) {
            result = 31 * result + a[i];
        }

        return result;
    }

    public static int hashCode(final int[] a) {
        return a == null ? 0 : hashCode(a, 0, a.length);
    }

    public static int hashCode(final int[] a, final int fromIndex, final int toIndex) {
        if (a == null) {
            return 0;
        }

        int result = 1;

        for (int i = fromIndex; i < toIndex; i++) {
            result = 31 * result + a[i];
        }

        return result;
    }

    public static int hashCode(final long[] a) {
        return a == null ? 0 : hashCode(a, 0, a.length);
    }

    public static int hashCode(final long[] a, final int fromIndex, final int toIndex) {
        if (a == null) {
            return 0;
        }

        int result = 1;

        for (int i = fromIndex; i < toIndex; i++) {
            result = 31 * result + (int) (a[i] ^ (a[i] >>> 32));
        }

        return result;
    }

    public static int hashCode(final float[] a) {
        return a == null ? 0 : hashCode(a, 0, a.length);
    }

    public static int hashCode(final float[] a, final int fromIndex, final int toIndex) {
        if (a == null) {
            return 0;
        }

        int result = 1;

        for (int i = fromIndex; i < toIndex; i++) {
            result = 31 * result + Float.floatToIntBits(a[i]);
        }

        return result;
    }

    public static int hashCode(final double[] a) {
        return a == null ? 0 : hashCode(a, 0, a.length);
    }

    public static int hashCode(final double[] a, final int fromIndex, final int toIndex) {
        if (a == null) {
            return 0;
        }

        int result = 1;

        for (int i = fromIndex; i < toIndex; i++) {
            final long bits = Double.doubleToLongBits(a[i]);
            result = 31 * result + (int) (bits ^ (bits >>> 32));
        }

        return result;
    }

    public static int hashCode(final Object[] a) {
        return a == null ? 0 : hashCode(a, 0, a.length);
    }

    public static int hashCode(final Object[] a, final int fromIndex, final int toIndex) {
        if (a == null) {
            return 0;
        }

        int result = 1;

        for (int i = fromIndex; i < toIndex; i++) {
            result = 31 * result + (a[i] == null ? 0 : a[i].hashCode());
        }

        return result;
    }

    public static String toString(final boolean value) {
        return String.valueOf(value);
    }

    public static String toString(final char value) {
        return String.valueOf(value);
    }

    public static String toString(final byte value) {
        return String.valueOf(value);
    }

    public static String toString(final short value) {
        return String.valueOf(value);
    }

    public static String toString(final int value) {
        return String.valueOf(value);
    }

    public static String toString(final long value) {
        return String.valueOf(value);
    }

    public static String toString(final float value) {
        return String.valueOf(value);
    }

    public static String toString(final double value) {
        return String.valueOf(value);
    }

    public static String toString(final Object obj) {
        if (obj == null) {
            return NULL_STRING;
        }

        return obj.toString();
    }

    public static String toString(final boolean[] a) {
        if (a == null) {
            return NULL_STRING;
        }

        if (a.length == 0) {
            return "[]";
        }

        return toString(a, 0, a.length);
    }

    public static String toString(final boolean[] a, final int from, final int to) {
        final StringBuilder sb = new StringBuilder();

        toString(sb, a, from, to);

        return sb.toString();
    }

    static void toString(final StringBuilder sb, final boolean[] a) {
        if (a == null) {
            sb.append(NULL_STRING);
        } else if (a.length == 0) {
            sb.append("[]");
        } else {
            toString(sb, a, 0, a.length);
        }
    }

    static void toString(final StringBuilder sb, final boolean[] a, final int from, final int to) {
        sb.append('[');

        for (int i = from; i < to; i++) {
            if (i > from) {
                sb.append(", ");
            }

            sb.append(a[i]);
        }

        sb.append(']');
    }

    public static String toString(final char[] a) {
        if (a == null) {
            return NULL_STRING;
        }

        if (a.length == 0) {
            return "[]";
        }

        return toString(a, 0, a.length);
    }

    public static String toString(final char[] a, final int from, final int to) {
        final StringBuilder sb = new StringBuilder();

        toString(sb, a, from, to);

        return sb.toString();
    }

    static void toString(final StringBuilder sb, final char[] a) {
        if (a == null) {
            sb.append(NULL_STRING);
        } else if (a.length == 0) {
            sb.append("[]");
        } else {
            toString(sb, a, 0, a.length);
        }
    }

    static void toString(final StringBuilder sb, final char[] a, final int from, final int to) {
        sb.append('[');

        for (int i = from; i < to; i++) {
            if (i > from) {
                sb.append(", ");
            }

            sb.append(a[i]);
        }

        sb.append(']');
    }

    public static String toString(final byte[] a) {
        if (a == null) {
            return NULL_STRING;
        }

        if (a.length == 0) {
            return "[]";
        }

        return toString(a, 0, a.length);
    }

    public static String toString(final byte[] a, final int from, final int to) {
        final StringBuilder sb = new StringBuilder();

        toString(sb, a, from, to);

        return sb.toString();
    }

    static void toString(final StringBuilder sb, final byte[] a) {
        if (a == null) {
            sb.append(NULL_STRING);
        } else if (a.length == 0) {
            sb.append("[]");
        } else {
            toString(sb, a, 0, a.length);
        }
    }

    static void toString(final StringBuilder sb, final byte[] a, final int from, final int to) {
        sb.append('[');

        for (int i = from; i < to; i++) {
            if (i > from) {
                sb.append(", ");
            }

            sb.append(a[i]);
        }

        sb.append(']');
    }

    public static String toString(final short[] a) {
        if (a == null) {
            return NULL_STRING;
        }

        if (a.length == 0) {
            return "[]";
        }

        return toString(a, 0, a.length);
    }

    public static String toString(final short[] a, final int from, final int to) {
        final StringBuilder sb = new StringBuilder();

        toString(sb, a, from, to);

        return sb.toString();
    }

    static void toString(final StringBuilder sb, final short[] a) {
        if (a == null) {
            sb.append(NULL_STRING);
        } else if (a.length == 0) {
            sb.append("[]");
        } else {
            toString(sb, a, 0, a.length);
        }
    }

    static void toString(final StringBuilder sb, final short[] a, final int from, final int to) {
        sb.append('[');

        for (int i = from; i < to; i++) {
            if (i > from) {
                sb.append(", ");
            }

            sb.append(a[i]);
        }

        sb.append(']');
    }

    public static String toString(final int[] a) {
        if (a == null) {
            return NULL_STRING;
        }

        if (a.length == 0) {
            return "[]";
        }

        return toString(a, 0, a.length);
    }

    public static String toString(final int[] a, final int from, final int to) {
        final StringBuilder sb = new StringBuilder();

        toString(sb, a, from, to);

        return sb.toString();
    }

    static void toString(final StringBuilder sb, final int[] a) {
        if (a == null) {
            sb.append(NULL_STRING);
        } else if (a.length == 0) {
            sb.append("[]");
        } else {
            toString(sb, a, 0, a.length);
        }
    }

    static void toString(final StringBuilder sb, final int[] a, final int from, final int to) {
        sb.append('[');

        for (int i = from; i < to; i++) {
            if (i > from) {
                sb.append(", ");
            }

            sb.append(a[i]);
        }

        sb.append(']');
    }

    public static String toString(final long[] a) {
        if (a == null) {
            return NULL_STRING;
        }

        if (a.length == 0) {
            return "[]";
        }

        return toString(a, 0, a.length);
    }

    public static String toString(final long[] a, final int from, final int to) {
        final StringBuilder sb = new StringBuilder();

        toString(sb, a, from, to);

        return sb.toString();
    }

    static void toString(final StringBuilder sb, final long[] a) {
        if (a == null) {
            sb.append(NULL_STRING);
        } else if (a.length == 0) {
            sb.append("[]");
        } else {
            toString(sb, a, 0, a.length);
        }
    }

    static void toString(final StringBuilder sb, final long[] a, final int from, final int to) {
        sb.append('[');

        for (int i = from; i < to; i++) {
            if (i > from) {
                sb.append(", ");
            }

            sb.append(a[i]);
        }

        sb.append(']');
    }

    public static String toString(final float[] a) {
        if (a == null) {
            return NULL_STRING;
        }

        if (a.length == 0) {
            return "[]";
        }

        return toString(a, 0, a.length);
    }

    public static String toString(final float[] a, final int from, final int to) {
        final StringBuilder sb = new StringBuilder();

        toString(sb, a, from, to);

        return sb.toString();
    }

    static void toString(final StringBuilder sb, final float[] a) {
        if (a == null) {
            sb.append(NULL_STRING);
        } else if (a.length == 0) {
            sb.append("[]");
        } else {
            toString(sb, a, 0, a.length);
        }
    }

    static void toString(final StringBuilder sb, final float[] a, final int from, final int to) {
        sb.append('[');

        for (int i = from; i < to; i++) {
            if (i > from) {
                sb.append(", ");
            }

            sb.append(a[i]);
        }

        sb.append(']');
    }

    public static String toString(final double[] a) {
        if (a == null) {
            return NULL_STRING;
        }

        if (a.length == 0) {
            return "[]";
        }

        return toString(a, 0, a.length);
    }

    public static String toString(final double[] a, final int from, final int to) {
        final StringBuilder sb = new StringBuilder();

        toString(sb, a, from, to);

        return sb.toString();
    }

    static void toString(final StringBuilder sb, final double[] a) {
        if (a == null) {
            sb.append(NULL_STRING);
        } else if (a.length == 0) {
            sb.append("[]");
        } else {
            toString(sb, a, 0, a.length);
        }
    }

    static void toString(final StringBuilder sb, final double[] a, final int from, final int to) {
        sb.append('[');

        for (int i = from; i < to; i++) {
            if (i > from) {
                sb.append(", ");
            }

            sb.append(a[i]);
        }

        sb.append(']');
    }

    public static String toString(final Object[] a) {
        if (a == null) {
            return NULL_STRING;
        }

        if (a.length == 0) {
            return "[]";
        }

        return toString(a, 0, a.length);
    }

    public static String toString(final Object[] a, final int from, final int to) {
        final StringBuilder sb = new StringBuilder();

        toString(sb, a, from, to);

        return sb.toString();
    }

    static void toString(final StringBuilder sb, final Object[] a) {
        if (a == null) {
            sb.append(NULL_STRING);
        } else if (a.length == 0) {
            sb.append("[]");
        } else {
            toString(sb, a, 0, a.length);
        }
    }

    static void toString(final StringBuilder sb, final Object[] a, final int from, final int to) {
        sb.append('[');

        for (int i = from; i < to; i++) {
            if (i > from) {
                sb.append(", ");
            }

            sb.append(toString(a[i]));
        }

        sb.append(']');
    }

    @SafeVarargs
    public static <T> List<T> asList(final T... a) {
        if (a == null) {
            return new ArrayList<>();
        }

        final List<T> list = new ArrayList<>(a.length);

        list.addAll(Array.asList(a));

        return list;
    }

    @SafeVarargs
    public static <T> LinkedList<T> asLinkedList(final T... a) {
        if (a == null) {
            return new LinkedList<>();
        }

        return new LinkedList<>(Array.asList(a));
    }

    @SafeVarargs
    public static <T> Set<T> asSet(final T... a) {
        if (a == null) {
            return new HashSet<>();
        }

        final Set<T> set = new HashSet<>(initHashCapacity(a.length));

        set.addAll(Array.asList(a));

        return set;
    }

    @SafeVarargs
    public static <T> LinkedHashSet<T> asLinkedHashSet(final T... a) {
        if (a == null) {
            return new LinkedHashSet<>();
        }

        final LinkedHashSet<T> set = new LinkedHashSet<>(initHashCapacity(a.length));

        set.addAll(Array.asList(a));

        return set;
    }

    @SafeVarargs
    @SuppressWarnings("unchecked")
    public static <K, V> Map<K, V> asMap(final Object... a) {
        if (a == null) {
            return new HashMap<>();
        }

        final Map<K, V> m = new HashMap<>(initHashCapacity(a.length / 2));

        for (int i = 0; i < a.length; i++) {
            m.put((K) a[i], (V) a[++i]);
        }

        return m;
    }

    @SafeVarargs
    @SuppressWarnings("unchecked")
    public static <K, V> LinkedHashMap<K, V> asLinkedHashMap(final Object... a) {
        if (a == null) {
            return new LinkedHashMap<>();
        }

        final LinkedHashMap<K, V> m = new LinkedHashMap<>(initHashCapacity(a.length / 2));

        for (int i = 0; i < a.length; i++) {
            m.put((K) a[i], (V) a[++i]);
        }

        return m;
    }

    private static int initHashCapacity(final int size) {
        return size < MAX_HASH_LENGTH ? (int) (size * 1.25) + 1 : MAX_ARRAY_SIZE;
    }
}

/*
 * Copyright (c) 2017, Haiyang Li.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.landawn.abacus.util;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Map;
import java.util.function.Function;
import java.util.function.ToDoubleFunction;
import java.util.function.ToIntFunction;
import java.util.function.ToLongFunction;

import com.landawn.abacus.annotation.Beta;
import com.landawn.abacus.util.Builder.ComparisonBuilder;
import com.landawn.abacus.util.function.ToBooleanFunction;
import com.landawn.abacus.util.function.ToByteFunction;
import com.landawn.abacus.util.function.ToCharFunction;
import com.landawn.abacus.util.function.ToFloatFunction;
import com.landawn.abacus.util.function.ToShortFunction;

/**
 *
 * Factory utility class for Comparator.
 *
 *
 */
public final class Comparators {

    @SuppressWarnings("rawtypes")
    static final Comparator<Comparable> NULL_FIRST_COMPARATOR = (a, b) -> a == null ? (b == null ? 0 : -1) : (b == null ? 1 : a.compareTo(b));

    @SuppressWarnings("rawtypes")
    static final Comparator<Comparable> NULL_FIRST_REVERSED_ORDER = (a, b) -> a == null ? (b == null ? 0 : -1) : (b == null ? 1 : b.compareTo(a));

    @SuppressWarnings("rawtypes")
    static final Comparator<Comparable> NULL_LAST_COMPARATOR = (a, b) -> a == null ? (b == null ? 0 : 1) : (b == null ? -1 : a.compareTo(b));

    @SuppressWarnings("rawtypes")
    static final Comparator<Comparable> NULL_LAST_REVERSED_ORDER = (a, b) -> a == null ? (b == null ? 0 : 1) : (b == null ? -1 : b.compareTo(a));

    @SuppressWarnings("rawtypes")
    static final Comparator NATURAL_ORDER = NULL_FIRST_COMPARATOR;

    // It's reversed order of NATURAL_ORDER and null last
    @SuppressWarnings("rawtypes")
    static final Comparator REVERSED_ORDER = NULL_LAST_REVERSED_ORDER;

    static final Comparator<String> COMPARING_IGNORE_CASE = (a, b) -> a == null ? (b == null ? 0 : -1) : (b == null ? 1 : a.compareToIgnoreCase(b));

    static final Comparator<CharSequence> COMPARING_BY_LENGTH = Comparator.comparingInt(a -> a == null ? 0 : a.length());

    static final Comparator<Object> COMPARING_BY_ARRAY_LENGTH = Comparator.comparingInt(a -> a == null ? 0 : Array.getLength(a));

    @SuppressWarnings("rawtypes")
    static final Comparator<Collection> COMPARING_BY_SIZE = Comparator.comparingInt(a -> a == null ? 0 : a.size());

    @SuppressWarnings("rawtypes")
    static final Comparator<Map> COMPARING_BY_MAP_SIZE = Comparator.comparingInt(a -> a == null ? 0 : a.size());

    public static final Comparator<boolean[]> BOOLEAN_ARRAY_COMPARATOR = (a, b) -> {
        final int lenA = N.len(a);
        final int lenB = N.len(b);

        for (int i = 0, minLen = N.min(lenA, lenB); i < minLen; i++) {
            if (a[i] != b[i]) {
                return a[i] ? 1 : -1;
            }
        }

        return NATURAL_ORDER.compare(lenA, lenB);
    };

    public static final Comparator<char[]> CHAR_ARRAY_COMPARATOR = (a, b) -> {
        final int lenA = N.len(a);
        final int lenB = N.len(b);

        for (int i = 0, minLen = N.min(lenA, lenB); i < minLen; i++) {
            if (a[i] != b[i]) {
                return a[i] > b[i] ? 1 : -1;
            }
        }

        return NATURAL_ORDER.compare(lenA, lenB);
    };

    public static final Comparator<byte[]> BYTE_ARRAY_COMPARATOR = (a, b) -> {
        final int lenA = N.len(a);
        final int lenB = N.len(b);

        for (int i = 0, minLen = N.min(lenA, lenB); i < minLen; i++) {
            if (a[i] != b[i]) {
                return a[i] > b[i] ? 1 : -1;
            }
        }

        return NATURAL_ORDER.compare(lenA, lenB);
    };

    public static final Comparator<short[]> SHORT_ARRAY_COMPARATOR = (a, b) -> {
        final int lenA = N.len(a);
        final int lenB = N.len(b);

        for (int i = 0, minLen = N.min(lenA, lenB); i < minLen; i++) {
            if (a[i] != b[i]) {
                return a[i] > b[i] ? 1 : -1;
            }
        }

        return NATURAL_ORDER.compare(lenA, lenB);
    };

    public static final Comparator<int[]> INT_ARRAY_COMPARATOR = (a, b) -> {
        final int lenA = N.len(a);
        final int lenB = N.len(b);

        for (int i = 0, minLen = N.min(lenA, lenB); i < minLen; i++) {
            if (a[i] != b[i]) {
                return a[i] > b[i] ? 1 : -1;
            }
        }

        return NATURAL_ORDER.compare(lenA, lenB);
    };

    public static final Comparator<long[]> LONG_ARRAY_COMPARATOR = (a, b) -> {
        final int lenA = N.len(a);
        final int lenB = N.len(b);

        for (int i = 0, minLen = N.min(lenA, lenB); i < minLen; i++) {
            if (a[i] != b[i]) {
                return a[i] > b[i] ? 1 : -1;
            }
        }

        return NATURAL_ORDER.compare(lenA, lenB);
    };

    public static final Comparator<float[]> FLOAT_ARRAY_COMPARATOR = (a, b) -> {
        final int lenA = N.len(a);
        final int lenB = N.len(b);
        int result = 0;

        for (int i = 0, minLen = N.min(lenA, lenB); i < minLen; i++) {
            result = Float.compare(a[i], b[i]);

            if (result != 0) {
                return result;
            }
        }

        return NATURAL_ORDER.compare(lenA, lenB);
    };

    public static final Comparator<double[]> DOUBLE_ARRAY_COMPARATOR = (a, b) -> {
        final int lenA = N.len(a);
        final int lenB = N.len(b);
        int result = 0;

        for (int i = 0, minLen = N.min(lenA, lenB); i < minLen; i++) {
            result = Double.compare(a[i], b[i]);

            if (result != 0) {
                return result;
            }
        }

        return NATURAL_ORDER.compare(lenA, lenB);
    };

    public static final Comparator<Object[]> OBJECT_ARRAY_COMPARATOR = (a, b) -> {
        final int lenA = N.len(a);
        final int lenB = N.len(b);
        int result = 0;

        for (int i = 0, minLen = N.min(lenA, lenB); i < minLen; i++) {
            result = NATURAL_ORDER.compare(a[i], b[i]);

            if (result != 0) {
                return result;
            }
        }

        return NATURAL_ORDER.compare(lenA, lenB);
    };

    @SuppressWarnings("rawtypes")
    public static final Comparator<Collection> COLLECTION_COMPARATOR = (a, b) -> {
        if (N.isEmpty(a)) {
            return N.isEmpty(b) ? 0 : -1;
        } else if (N.isEmpty(b)) {
            return 1;
        }

        final Iterator<Object> iterA = a.iterator();
        final Iterator<Object> iterB = b.iterator();

        final int lenA = N.size(a);
        final int lenB = N.size(b);
        int result = 0;

        for (int i = 0, minLen = N.min(lenA, lenB); i < minLen; i++) {
            result = NATURAL_ORDER.compare(iterA.next(), iterB.next());

            if (result != 0) {
                return result;
            }
        }

        return NATURAL_ORDER.compare(lenA, lenB);
    };

    private Comparators() {
        // Utility class.
    }

    /**
     * Returns a comparator that compares {@code Comparable} objects in their natural order (where {@code null} is minimum).
     * <br />
     * Same as {@code nullsFirst()}.
     *
     * @param <T> the type of the objects being compared
     * @return a comparator that compares {@code Comparable} objects in their natural order
     * @see #nullsFirst()
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static <T extends Comparable> Comparator<T> naturalOrder() {
        return NATURAL_ORDER;
    }

    /**
     * Returns a comparator that compares {@code Comparable} objects in their natural order (where {@code null} is minimum).
     * <br />
     * Same as {@code naturalOrder()}.
     *
     * @param <T> the type of the objects being compared
     * @return a comparator that compares {@code Comparable} objects in their natural order
     * @see #naturalOrder()
     */
    @SuppressWarnings("rawtypes")
    public static <T extends Comparable> Comparator<T> nullsFirst() {
        return (Comparator<T>) NULL_FIRST_COMPARATOR;
    }

    /**
     *
     * @param <T>
     * @param cmp
     * @return
     */
    public static <T> Comparator<T> nullsFirst(final Comparator<T> cmp) {
        if (cmp == null || cmp == NULL_FIRST_COMPARATOR) { // NOSONAR
            return (Comparator<T>) NULL_FIRST_COMPARATOR;
        }

        return (a, b) -> a == null ? (b == null ? 0 : -1) : (b == null ? 1 : cmp.compare(a, b));
    }

    /**
     * Comparing the key/value extracted by {@code keyExtractor} by {@code nullsFirst()} comparator.
     *
     * @param <T>
     * @param keyExtractor
     * @return
     * @throws IllegalArgumentException
     * @see #comparingByIfNotNullOrElseNullsFirst(Function)
     * @see #comparingByIfNotNullOrElseNullsLast(Function)
     */
    public static <T> Comparator<T> nullsFirstBy(@SuppressWarnings("rawtypes") final Function<? super T, ? extends Comparable> keyExtractor)
            throws IllegalArgumentException {
        N.checkArgNotNull(keyExtractor);

        return (a, b) -> NULL_FIRST_COMPARATOR.compare(keyExtractor.apply(a), keyExtractor.apply(b));
    }

    @SuppressWarnings("rawtypes")
    private static final Comparator NULLS_FIRST_OR_ELSE_EQUAL = (a, b) -> a == null ? (b == null ? 0 : -1) : (b == null ? 1 : 0);

    /**
     *
     * @param <T>
     * @return
     */
    @Beta
    public static <T> Comparator<T> nullsFirstOrElseEqual() {
        return NULLS_FIRST_OR_ELSE_EQUAL;
    }

    /**
     *
     * @param <T>
     * @return
     */
    @SuppressWarnings("rawtypes")
    public static <T extends Comparable> Comparator<T> nullsLast() {
        return (Comparator<T>) NULL_LAST_COMPARATOR;
    }

    /**
     *
     * @param <T>
     * @param cmp
     * @return
     */
    public static <T> Comparator<T> nullsLast(final Comparator<T> cmp) {
        if (cmp == null || cmp == NULL_LAST_COMPARATOR) { // NOSONAR
            return (Comparator<T>) NULL_LAST_COMPARATOR;
        }

        return (a, b) -> a == null ? (b == null ? 0 : 1) : (b == null ? -1 : cmp.compare(a, b));
    }

    @SuppressWarnings("rawtypes")
    private static final Comparator NULLS_LAST_OR_ELSE_EQUAL = (a, b) -> a == null ? (b == null ? 0 : 1) : (b == null ? -1 : 0);

    /**
     * Comparing the key/value extracted by {@code keyExtractor} by {@code nullsLast()} comparator.
     *
     * @param <T>
     * @param keyExtractor
     * @return
     * @throws IllegalArgumentException
     * @see #comparingByIfNotNullOrElseNullsFirst(Function)
     * @see #comparingByIfNotNullOrElseNullsLast(Function)
     */
    public static <T> Comparator<T> nullsLastBy(@SuppressWarnings("rawtypes") final Function<? super T, ? extends Comparable> keyExtractor)
            throws IllegalArgumentException {
        N.checkArgNotNull(keyExtractor);

        return (a, b) -> NULL_LAST_COMPARATOR.compare(keyExtractor.apply(a), keyExtractor.apply(b));
    }

    /**
     *
     * @param <T>
     * @return
     */
    @Beta
    public static <T> Comparator<T> nullsLastOrElseEqual() {
        return NULLS_LAST_OR_ELSE_EQUAL;
    }

    /**
     *
     * @param <T>
     * @return
     * @throws IllegalArgumentException
     */
    public static <T extends Comparable<? super T>> Comparator<u.Optional<T>> emptiesFirst() throws IllegalArgumentException {
        return emptiesFirst(naturalOrder());
    }

    /**
     *
     * @param <T>
     * @param cmp
     * @return
     * @throws IllegalArgumentException
     */
    public static <T> Comparator<u.Optional<T>> emptiesFirst(final Comparator<? super T> cmp) throws IllegalArgumentException {
        N.checkArgNotNull(cmp);

        // return Comparators.<u.Optional<T>, T> comparingBy(o -> o.orElse(null), Comparator.nullsFirst(cmp));

        return (a, b) -> a == null || a.isEmpty() ? (b == null || b.isEmpty() ? 0 : -1) : (b == null || b.isEmpty() ? 1 : cmp.compare(a.get(), b.get()));
    }

    /**
     *
     * @param <T>
     * @return
     * @throws IllegalArgumentException
     */
    public static <T extends Comparable<? super T>> Comparator<u.Optional<T>> emptiesLast() throws IllegalArgumentException {
        return emptiesLast(naturalOrder());
    }

    /**
     *
     * @param <T>
     * @param cmp
     * @return
     * @throws IllegalArgumentException
     */
    public static <T> Comparator<u.Optional<T>> emptiesLast(final Comparator<? super T> cmp) throws IllegalArgumentException {
        N.checkArgNotNull(cmp);

        // return Comparators.<u.Optional<T>, T> comparingBy(o -> o.orElse(null), Comparator.nullsLast(cmp));

        return (a, b) -> a == null || a.isEmpty() ? (b == null || b.isEmpty() ? 0 : 1) : (b == null || b.isEmpty() ? -1 : cmp.compare(a.get(), b.get()));
    }

    /**
     * Comparing the key/value extracted by {@code keyExtractor} by {@code nullsFirst()} comparator.
     *
     * @param <T>
     * @param keyExtractor
     * @return
     * @throws IllegalArgumentException
     * @see #nullsFirstBy(Function)
     * @see #nullsLastBy(Function)
     * @see #comparingByIfNotNullOrElseNullsFirst(Function)
     * @see #comparingByIfNotNullOrElseNullsLast(Function)
     */
    public static <T> Comparator<T> comparingBy(@SuppressWarnings("rawtypes") final Function<? super T, ? extends Comparable> keyExtractor)
            throws IllegalArgumentException {
        N.checkArgNotNull(keyExtractor);

        return (a, b) -> NATURAL_ORDER.compare(keyExtractor.apply(a), keyExtractor.apply(b));
    }

    /**
     *
     * @param <T>
     * @param keyExtractor
     * @return
     * @throws IllegalArgumentException
     */
    @Beta
    public static <T> Comparator<T> comparingByIfNotNullOrElseNullsFirst(
            @SuppressWarnings("rawtypes") final Function<? super T, ? extends Comparable> keyExtractor) throws IllegalArgumentException {
        N.checkArgNotNull(keyExtractor);

        @SuppressWarnings("rawtypes")
        final Comparator<Comparable> cmp = NULL_FIRST_COMPARATOR;

        return (a, b) -> a == null ? (b == null ? 0 : -1) : (b == null ? 1 : cmp.compare(keyExtractor.apply(a), keyExtractor.apply(b)));
    }

    /**
     *
     * @param <T>
     * @param keyExtractor
     * @return
     * @throws IllegalArgumentException
     */
    @Beta
    public static <T> Comparator<T> comparingByIfNotNullOrElseNullsLast(@SuppressWarnings("rawtypes") final Function<? super T, Comparable> keyExtractor)
            throws IllegalArgumentException {
        N.checkArgNotNull(keyExtractor);

        @SuppressWarnings("rawtypes")
        final Comparator<Comparable> cmp = NULL_LAST_COMPARATOR;

        return (a, b) -> a == null ? (b == null ? 0 : 1) : (b == null ? -1 : cmp.compare(keyExtractor.apply(a), keyExtractor.apply(b)));
    }

    /**
     *
     * @param <T>
     * @param <U>
     * @param keyExtractor
     * @param keyComparator
     * @return
     * @throws IllegalArgumentException
     * @see #comparingByIfNotNullOrElseNullsFirst(Function, Comparator)
     * @see #comparingByIfNotNullOrElseNullsLast(Function, Comparator)
     */
    public static <T, U> Comparator<T> comparingBy(final Function<? super T, ? extends U> keyExtractor, final Comparator<? super U> keyComparator)
            throws IllegalArgumentException {
        N.checkArgNotNull(keyExtractor);
        N.checkArgNotNull(keyComparator);

        return (a, b) -> keyComparator.compare(keyExtractor.apply(a), keyExtractor.apply(b));
    }

    /**
     *
     * @param <T>
     * @param <U>
     * @param keyExtractor
     * @param keyComparator
     * @return
     * @throws IllegalArgumentException
     */
    public static <T, U> Comparator<T> comparingByIfNotNullOrElseNullsFirst(final Function<? super T, ? extends U> keyExtractor,
            final Comparator<? super U> keyComparator) throws IllegalArgumentException {
        N.checkArgNotNull(keyExtractor);
        N.checkArgNotNull(keyComparator);

        return (a, b) -> a == null ? (b == null ? 0 : -1) : (b == null ? 1 : keyComparator.compare(keyExtractor.apply(a), keyExtractor.apply(b)));
    }

    /**
     *
     * @param <T>
     * @param <U>
     * @param keyExtractor
     * @param keyComparator
     * @return
     * @throws IllegalArgumentException
     */
    public static <T, U> Comparator<T> comparingByIfNotNullOrElseNullsLast(final Function<? super T, ? extends U> keyExtractor,
            final Comparator<? super U> keyComparator) throws IllegalArgumentException {
        N.checkArgNotNull(keyExtractor);
        N.checkArgNotNull(keyComparator);

        return (a, b) -> a == null ? (b == null ? 0 : 1) : (b == null ? -1 : keyComparator.compare(keyExtractor.apply(a), keyExtractor.apply(b)));
    }

    /**
     *
     * @param <T>
     * @param keyExtractor
     * @return
     * @throws IllegalArgumentException
     */
    public static <T> Comparator<T> comparingBoolean(final ToBooleanFunction<? super T> keyExtractor) throws IllegalArgumentException {
        N.checkArgNotNull(keyExtractor);

        return (a, b) -> Boolean.compare(keyExtractor.applyAsBoolean(a), keyExtractor.applyAsBoolean(b));
    }

    /**
     *
     * @param <T>
     * @param keyExtractor
     * @return
     * @throws IllegalArgumentException
     */
    public static <T> Comparator<T> comparingChar(final ToCharFunction<? super T> keyExtractor) throws IllegalArgumentException {
        N.checkArgNotNull(keyExtractor);

        return Comparator.comparingInt(keyExtractor::applyAsChar);
    }

    /**
     *
     * @param <T>
     * @param keyExtractor
     * @return
     * @throws IllegalArgumentException
     */
    public static <T> Comparator<T> comparingByte(final ToByteFunction<? super T> keyExtractor) throws IllegalArgumentException {
        N.checkArgNotNull(keyExtractor);

        return Comparator.comparingInt(keyExtractor::applyAsByte);
    }

    /**
     *
     * @param <T>
     * @param keyExtractor
     * @return
     * @throws IllegalArgumentException
     */
    public static <T> Comparator<T> comparingShort(final ToShortFunction<? super T> keyExtractor) throws IllegalArgumentException {
        N.checkArgNotNull(keyExtractor);

        return Comparator.comparingInt(keyExtractor::applyAsShort);
    }

    /**
     *
     * @param <T>
     * @param keyExtractor
     * @return
     * @throws IllegalArgumentException
     */
    public static <T> Comparator<T> comparingInt(final ToIntFunction<? super T> keyExtractor) throws IllegalArgumentException {
        N.checkArgNotNull(keyExtractor);

        return Comparator.comparingInt(keyExtractor);
    }

    /**
     *
     * @param <T>
     * @param keyExtractor
     * @return
     * @throws IllegalArgumentException
     */
    public static <T> Comparator<T> comparingLong(final ToLongFunction<? super T> keyExtractor) throws IllegalArgumentException {
        N.checkArgNotNull(keyExtractor);

        return Comparator.comparingLong(keyExtractor);
    }

    /**
     *
     * @param <T>
     * @param keyExtractor
     * @return
     * @throws IllegalArgumentException
     */
    public static <T> Comparator<T> comparingFloat(final ToFloatFunction<? super T> keyExtractor) throws IllegalArgumentException {
        N.checkArgNotNull(keyExtractor);

        return (a, b) -> Float.compare(keyExtractor.applyAsFloat(a), keyExtractor.applyAsFloat(b));
    }

    /**
     *
     * @param <T>
     * @param keyExtractor
     * @return
     * @throws IllegalArgumentException
     */
    public static <T> Comparator<T> comparingDouble(final ToDoubleFunction<? super T> keyExtractor) throws IllegalArgumentException {
        N.checkArgNotNull(keyExtractor);

        return Comparator.comparingDouble(keyExtractor);
    }

    /**
     * Comparing ignore case.
     *
     * @return
     */
    public static Comparator<String> comparingIgnoreCase() {
        return COMPARING_IGNORE_CASE;
    }

    /**
     * Comparing ignore case.
     *
     * @param <T>
     * @param keyExtractor
     * @return
     * @throws IllegalArgumentException
     */
    public static <T> Comparator<T> comparingIgnoreCase(final Function<? super T, String> keyExtractor) throws IllegalArgumentException {
        N.checkArgNotNull(keyExtractor);

        return (a, b) -> COMPARING_IGNORE_CASE.compare(keyExtractor.apply(a), keyExtractor.apply(b));
    }

    /**
     * Comparing by key.
     *
     * @param <K> the key type
     * @param <V> the value type
     * @return
     */
    public static <K extends Comparable<? super K>, V> Comparator<Map.Entry<K, V>> comparingByKey() {
        return (a, b) -> NATURAL_ORDER.compare(a.getKey(), b.getKey());
    }

    /**
     * Comparing by value.
     *
     * @param <K> the key type
     * @param <V> the value type
     * @return
     */
    public static <K, V extends Comparable<? super V>> Comparator<Map.Entry<K, V>> comparingByValue() {
        return (a, b) -> NATURAL_ORDER.compare(a.getValue(), b.getValue());
    }

    /**
     * Comparing by key.
     *
     * @param <K> the key type
     * @param <V> the value type
     * @param cmp
     * @return
     * @throws IllegalArgumentException
     */
    public static <K, V> Comparator<Map.Entry<K, V>> comparingByKey(final Comparator<? super K> cmp) throws IllegalArgumentException {
        N.checkArgNotNull(cmp);

        return (a, b) -> cmp.compare(a.getKey(), b.getKey());
    }

    /**
     * Comparing by value.
     *
     * @param <K> the key type
     * @param <V> the value type
     * @param cmp
     * @return
     * @throws IllegalArgumentException
     */
    public static <K, V> Comparator<Map.Entry<K, V>> comparingByValue(final Comparator<? super V> cmp) throws IllegalArgumentException {
        N.checkArgNotNull(cmp);

        return (a, b) -> cmp.compare(a.getValue(), b.getValue());
    }

    /**
     * Comparing by length.
     *
     * @param <T>
     * @return
     */
    public static <T extends CharSequence> Comparator<T> comparingByLength() {
        return (Comparator<T>) COMPARING_BY_LENGTH;
    }

    /**
     * Comparing by array length.
     *
     * @param <T>
     * @return
     */
    public static <T> Comparator<T> comparingByArrayLength() {
        return (Comparator<T>) COMPARING_BY_ARRAY_LENGTH;
    }

    /**
     * Comparing by collection size.
     *
     * @param <T>
     * @return
     */
    @SuppressWarnings("rawtypes")
    public static <T extends Collection> Comparator<T> comparingBySize() {
        return (Comparator<T>) COMPARING_BY_SIZE;
    }

    /**
     * Comparing by map size.
     *
     * @param <T>
     * @return
     */
    @SuppressWarnings("rawtypes")
    public static <T extends Map> Comparator<T> comparingByMapSize() {
        return (Comparator<T>) COMPARING_BY_MAP_SIZE;
    }

    //    /**
    //     *
    //     *
    //     * @return
    //     */
    //    public static Comparator<Object[]> comparingObjArray() {
    //        return comparingObjArray(naturalOrder());
    //    }

    /**
     *
     * @param cmp
     * @return
     */
    @SuppressWarnings("rawtypes")
    public static Comparator<Object[]> comparingObjArray(final Comparator<?> cmp) {
        return comparingArray((Comparator) cmp);
    }

    /**
     *
     * @param <T>
     * @return
     */
    @SuppressWarnings("rawtypes")
    public static <T extends Comparable> Comparator<T[]> comparingArray() {
        return comparingArray(NATURAL_ORDER);
    }

    /**
     *
     * @param <T>
     * @param cmp
     * @return
     * @throws IllegalArgumentException
     */
    public static <T> Comparator<T[]> comparingArray(final Comparator<? super T> cmp) throws IllegalArgumentException {
        N.checkArgNotNull(cmp);

        return (a, b) -> {
            if (N.isEmpty(a)) {
                return N.isEmpty(b) ? 0 : -1;
            } else if (N.isEmpty(b)) {
                return 1;
            }

            final int lenA = N.len(a);
            final int lenB = N.len(b);
            int result = 0;

            for (int i = 0, minLen = N.min(lenA, lenB); i < minLen; i++) {
                result = cmp.compare(a[i], b[i]);

                if (result != 0) {
                    return result;
                }
            }

            return Integer.compare(lenA, lenB);
        };
    }

    /**
     *
     * @param <C>
     * @return
     */
    @SuppressWarnings("rawtypes")
    public static <C extends Collection<? extends Comparable>> Comparator<C> comparingCollection() {
        return comparingCollection(NATURAL_ORDER);
    }

    /**
     *
     * @param <T>
     * @param <C>
     * @param cmp
     * @return
     * @throws IllegalArgumentException
     */
    public static <T, C extends Collection<T>> Comparator<C> comparingCollection(final Comparator<? super T> cmp) throws IllegalArgumentException {
        N.checkArgNotNull(cmp);

        return (a, b) -> {
            if (N.isEmpty(a)) {
                return N.isEmpty(b) ? 0 : -1;
            } else if (N.isEmpty(b)) {
                return 1;
            }

            final Iterator<T> iterA = a.iterator();
            final Iterator<T> iterB = b.iterator();

            final int sizeA = N.size(a);
            final int sizeB = N.size(b);
            int result = 0;

            for (int i = 0, minLen = N.min(sizeA, sizeB); i < minLen; i++) {
                result = cmp.compare(iterA.next(), iterB.next());

                if (result != 0) {
                    return result;
                }
            }

            return Integer.compare(sizeA, sizeB);
        };
    }

    /**
     *
     * @param <C>
     * @return
     */
    @SuppressWarnings("rawtypes")
    public static <C extends Iterable<? extends Comparable>> Comparator<C> comparingIterable() {
        return comparingIterable(NATURAL_ORDER);
    }

    /**
     *
     * @param <T>
     * @param <C>
     * @param cmp
     * @return
     * @throws IllegalArgumentException
     */
    public static <T, C extends Iterable<T>> Comparator<C> comparingIterable(final Comparator<? super T> cmp) throws IllegalArgumentException {
        N.checkArgNotNull(cmp);

        return (a, b) -> {
            final Iterator<T> iterA = N.iterate(a);
            final Iterator<T> iterB = N.iterate(b);

            if (N.isEmpty(iterA)) {
                return N.isEmpty(iterB) ? 0 : -1;
            } else if (N.isEmpty(iterB)) {
                return 1;
            }

            int result = 0;

            while (iterA.hasNext() && iterB.hasNext()) {
                result = cmp.compare(iterA.next(), iterB.next());

                if (result != 0) {
                    return result;
                }
            }

            return iterA.hasNext() ? 1 : (iterB.hasNext() ? -1 : 0);
        };
    }

    /**
     *
     * @param <C>
     * @return
     */
    @SuppressWarnings("rawtypes")
    public static <C extends Iterator<? extends Comparable>> Comparator<C> comparingIterator() {
        return comparingIterator(NATURAL_ORDER);
    }

    /**
     *
     * @param <T>
     * @param <C>
     * @param cmp
     * @return
     * @throws IllegalArgumentException
     */
    public static <T, C extends Iterator<T>> Comparator<C> comparingIterator(final Comparator<? super T> cmp) throws IllegalArgumentException {
        N.checkArgNotNull(cmp);

        return (a, b) -> {
            if (N.isEmpty(a)) {
                return N.isEmpty(b) ? 0 : -1;
            } else if (N.isEmpty(b)) {
                return 1;
            }

            int result = 0;

            while (a.hasNext() && b.hasNext()) {
                result = cmp.compare(a.next(), b.next());

                if (result != 0) {
                    return result;
                }
            }

            return a.hasNext() ? 1 : (b.hasNext() ? -1 : 0);
        };
    }

    /**
     *
     * @param <M>
     * @return
     */
    @SuppressWarnings("rawtypes")
    public static <M extends Map<? extends Comparable, ?>> Comparator<M> comparingMapByKey() {
        return comparingMapByKey(NATURAL_ORDER);
    }

    /**
     *
     * @param <K>
     * @param <M>
     * @param cmp
     * @return
     * @throws IllegalArgumentException
     */
    public static <K, M extends Map<K, ?>> Comparator<M> comparingMapByKey(final Comparator<? super K> cmp) throws IllegalArgumentException {
        N.checkArgNotNull(cmp);

        return (a, b) -> {
            if (N.isEmpty(a)) {
                return N.isEmpty(b) ? 0 : -1;
            } else if (N.isEmpty(b)) {
                return 1;
            }

            final Iterator<K> iterA = a.keySet().iterator();
            final Iterator<K> iterB = b.keySet().iterator();

            final int sizeA = N.size(a);
            final int sizeB = N.size(b);
            int result = 0;

            for (int i = 0, minLen = N.min(sizeA, sizeB); i < minLen; i++) {
                result = cmp.compare(iterA.next(), iterB.next());

                if (result != 0) {
                    return result;
                }
            }

            return Integer.compare(sizeA, sizeB);
        };
    }

    /**
     *
     * @param <M>
     * @return
     */
    @SuppressWarnings("rawtypes")
    public static <M extends Map<?, ? extends Comparable>> Comparator<M> comparingMapByValue() {
        return comparingMapByValue(NATURAL_ORDER);
    }

    /**
     *
     * @param <V>
     * @param <M>
     * @param cmp
     * @return
     * @throws IllegalArgumentException
     */
    public static <V, M extends Map<?, V>> Comparator<M> comparingMapByValue(final Comparator<? super V> cmp) throws IllegalArgumentException {
        N.checkArgNotNull(cmp);

        return (a, b) -> {
            if (N.isEmpty(a)) {
                return N.isEmpty(b) ? 0 : -1;
            } else if (N.isEmpty(b)) {
                return 1;
            }

            final Iterator<V> iterA = a.values().iterator();
            final Iterator<V> iterB = b.values().iterator();

            final int sizeA = N.size(a);
            final int sizeB = N.size(b);
            int result = 0;

            for (int i = 0, minLen = N.min(sizeA, sizeB); i < minLen; i++) {
                result = cmp.compare(iterA.next(), iterB.next());

                if (result != 0) {
                    return result;
                }
            }

            return Integer.compare(sizeA, sizeB);
        };
    }

    /**
     *
     * @param <T>
     * @param propNamesToCompare
     * @return
     * @throws IllegalArgumentException
     * @deprecated call {@code getPropValue} by reflection apis during comparing or sorting may have huge impact to performance. Use {@link ComparisonBuilder} instead.
     * @see Builder#compare(Object, Object, Comparator)
     * @see {@link ComparisonBuilder}
     */
    @Deprecated
    public static <T> Comparator<T> comparingBeanByProps(final Collection<String> propNamesToCompare) throws IllegalArgumentException {
        return (a, b) -> N.compareByProps(a, b, propNamesToCompare);
    }

    /**
     *
     * @param <T>
     * @return
     */
    @SuppressWarnings("rawtypes")
    public static <T extends Comparable> Comparator<T> reverseOrder() {
        return REVERSED_ORDER;
    }

    /**
     *
     * @param <T>
     * @param cmp
     * @return
     */
    public static <T> Comparator<T> reverseOrder(final Comparator<T> cmp) {
        if (cmp == null || cmp == NATURAL_ORDER) { // NOSONAR
            return REVERSED_ORDER;
        } else if (cmp == REVERSED_ORDER) { // NOSONAR
            return NATURAL_ORDER;
        }

        return Collections.reverseOrder(cmp);
    }

    /**
     *
     * @param <T>
     * @param keyExtractor
     * @return
     * @throws IllegalArgumentException
     */
    public static <T> Comparator<T> reversedComparingBoolean(final ToBooleanFunction<? super T> keyExtractor) throws IllegalArgumentException {
        N.checkArgNotNull(keyExtractor);

        return (a, b) -> Boolean.compare(keyExtractor.applyAsBoolean(b), keyExtractor.applyAsBoolean(a));
    }

    /**
     *
     * @param <T>
     * @param keyExtractor
     * @return
     * @throws IllegalArgumentException
     */
    public static <T> Comparator<T> reversedComparingChar(final ToCharFunction<? super T> keyExtractor) throws IllegalArgumentException {
        N.checkArgNotNull(keyExtractor);

        return (a, b) -> Character.compare(keyExtractor.applyAsChar(b), keyExtractor.applyAsChar(a));
    }

    /**
     *
     * @param <T>
     * @param keyExtractor
     * @return
     * @throws IllegalArgumentException
     */
    public static <T> Comparator<T> reversedComparingByte(final ToByteFunction<? super T> keyExtractor) throws IllegalArgumentException {
        N.checkArgNotNull(keyExtractor);

        return (a, b) -> Byte.compare(keyExtractor.applyAsByte(b), keyExtractor.applyAsByte(a));
    }

    /**
     *
     * @param <T>
     * @param keyExtractor
     * @return
     * @throws IllegalArgumentException
     */
    public static <T> Comparator<T> reversedComparingShort(final ToShortFunction<? super T> keyExtractor) throws IllegalArgumentException {
        N.checkArgNotNull(keyExtractor);

        return (a, b) -> Short.compare(keyExtractor.applyAsShort(b), keyExtractor.applyAsShort(a));
    }

    /**
     *
     * @param <T>
     * @param keyExtractor
     * @return
     * @throws IllegalArgumentException
     */
    public static <T> Comparator<T> reversedComparingInt(final ToIntFunction<? super T> keyExtractor) throws IllegalArgumentException {
        N.checkArgNotNull(keyExtractor);

        return (a, b) -> Integer.compare(keyExtractor.applyAsInt(b), keyExtractor.applyAsInt(a));
    }

    /**
     *
     * @param <T>
     * @param keyExtractor
     * @return
     * @throws IllegalArgumentException
     */
    public static <T> Comparator<T> reversedComparingLong(final ToLongFunction<? super T> keyExtractor) throws IllegalArgumentException {
        N.checkArgNotNull(keyExtractor);

        return (a, b) -> Long.compare(keyExtractor.applyAsLong(b), keyExtractor.applyAsLong(a));
    }

    /**
     *
     * @param <T>
     * @param keyExtractor
     * @return
     * @throws IllegalArgumentException
     */
    public static <T> Comparator<T> reversedComparingFloat(final ToFloatFunction<? super T> keyExtractor) throws IllegalArgumentException {
        N.checkArgNotNull(keyExtractor);

        return (a, b) -> Float.compare(keyExtractor.applyAsFloat(b), keyExtractor.applyAsFloat(a));
    }

    /**
     *
     * @param <T>
     * @param keyExtractor
     * @return
     * @throws IllegalArgumentException
     */
    public static <T> Comparator<T> reversedComparingDouble(final ToDoubleFunction<? super T> keyExtractor) throws IllegalArgumentException {
        N.checkArgNotNull(keyExtractor);

        return (a, b) -> Double.compare(keyExtractor.applyAsDouble(b), keyExtractor.applyAsDouble(a));
    }

    /**
     * Reversed comparing by.
     *
     * @param <T>
     * @param keyExtractor
     * @return
     * @throws IllegalArgumentException
     * @see #reversedComparingByIfNotNullOrElseNullsFirst(Function)
     * @see #reversedComparingByIfNotNullOrElseNullsLast(Function)
     */
    public static <T> Comparator<T> reversedComparingBy(@SuppressWarnings("rawtypes") final Function<? super T, ? extends Comparable> keyExtractor)
            throws IllegalArgumentException {
        N.checkArgNotNull(keyExtractor);

        return (a, b) -> REVERSED_ORDER.compare(keyExtractor.apply(a), keyExtractor.apply(b));
    }

    /**
     *
     * @param <T>
     * @param keyExtractor
     * @return
     * @throws IllegalArgumentException
     */
    @Beta
    public static <T> Comparator<T> reversedComparingByIfNotNullOrElseNullsFirst(
            @SuppressWarnings("rawtypes") final Function<? super T, ? extends Comparable> keyExtractor) throws IllegalArgumentException {
        N.checkArgNotNull(keyExtractor);

        @SuppressWarnings("rawtypes")
        final Comparator<Comparable> cmp = NULL_FIRST_REVERSED_ORDER;

        return (a, b) -> a == null ? (b == null ? 0 : -1) : (b == null ? 1 : cmp.compare(keyExtractor.apply(a), keyExtractor.apply(b)));
    }

    /**
     *
     * @param <T>
     * @param keyExtractor
     * @return
     * @throws IllegalArgumentException
     */
    @Beta
    public static <T> Comparator<T> reversedComparingByIfNotNullOrElseNullsLast(
            @SuppressWarnings("rawtypes") final Function<? super T, ? extends Comparable> keyExtractor) throws IllegalArgumentException {
        N.checkArgNotNull(keyExtractor);

        @SuppressWarnings("rawtypes")
        final Comparator<Comparable> cmp = NULL_LAST_REVERSED_ORDER;

        return (a, b) -> a == null ? (b == null ? 0 : 1) : (b == null ? -1 : cmp.compare(keyExtractor.apply(a), keyExtractor.apply(b)));
    }

    /**
     * Reversed comparing by key.
     *
     * @param <K> the key type
     * @param <V> the value type
     * @return
     */
    public static <K extends Comparable<? super K>, V> Comparator<Map.Entry<K, V>> reversedComparingByKey() {
        return (a, b) -> REVERSED_ORDER.compare(a.getKey(), b.getKey());
    }

    /**
     * Reversed comparing by value.
     *
     * @param <K> the key type
     * @param <V> the value type
     * @return
     */
    public static <K, V extends Comparable<? super V>> Comparator<Map.Entry<K, V>> reversedComparingByValue() {
        return (a, b) -> REVERSED_ORDER.compare(a.getValue(), b.getValue());
    }

    /**
     * Reversed comparing by key.
     *
     * @param <K> the key type
     * @param <V> the value type
     * @param cmp
     * @return
     * @throws IllegalArgumentException
     */
    @Beta
    public static <K, V> Comparator<Map.Entry<K, V>> reversedComparingByKey(final Comparator<? super K> cmp) throws IllegalArgumentException {
        N.checkArgNotNull(cmp);

        final Comparator<? super K> reversedOrder = reverseOrder(cmp);

        return (a, b) -> reversedOrder.compare(a.getKey(), b.getKey());
    }

    /**
     * Reversed comparing by value.
     *
     * @param <K> the key type
     * @param <V> the value type
     * @param cmp
     * @return
     * @throws IllegalArgumentException
     */
    @Beta
    public static <K, V> Comparator<Map.Entry<K, V>> reversedComparingByValue(final Comparator<? super V> cmp) throws IllegalArgumentException {
        N.checkArgNotNull(cmp);

        final Comparator<? super V> reversedOrder = reverseOrder(cmp);

        return (a, b) -> reversedOrder.compare(a.getValue(), b.getValue());
    }
}

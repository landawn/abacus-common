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

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.AbstractCollection;
import java.util.AbstractList;
import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableSet;
import java.util.NoSuchElementException;
import java.util.RandomAccess;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.ToDoubleFunction;
import java.util.function.ToIntFunction;
import java.util.function.ToLongFunction;

import com.landawn.abacus.annotation.Beta;
import com.landawn.abacus.util.Range.BoundType;
import com.landawn.abacus.util.u.Nullable;
import com.landawn.abacus.util.u.Optional;
import com.landawn.abacus.util.u.OptionalByte;
import com.landawn.abacus.util.u.OptionalChar;
import com.landawn.abacus.util.u.OptionalDouble;
import com.landawn.abacus.util.u.OptionalFloat;
import com.landawn.abacus.util.u.OptionalInt;
import com.landawn.abacus.util.u.OptionalLong;
import com.landawn.abacus.util.u.OptionalShort;

/**
 * <p>
 * Note: This class includes codes copied from Apache Commons Lang, Google Guava and other open source projects under the Apache License 2.0.
 * The methods copied from other libraries/frameworks/projects may be modified in this class.
 * </p>
 *
 * <br />
 * <br />
 * When to throw exception? It's designed to avoid throwing any unnecessary
 * exception if the contract defined by method is not broken. for example, if
 * user tries to reverse a null or empty String. the input String will be
 * returned. But exception will be thrown if try to add element to a null Object array or collection.
 * <br />
 * <br />
 * An empty String/Array/Collection/Map/Iterator/Iterable/InputStream/Reader will always be a preferred choice than a {@code null} for the return value of a method.
 * <br />
 *
 * <p>
 * This is a utility class for iterable data structures, including {@code Collection/Array/Iterator}.
 * </p>
 *
 * <p>
 * The methods in this class should only read the input {@code Collection/Array/Iterator} parameters, not modify them.
 * </p>
 *
 * @see com.landawn.abacus.util.N
 * @see com.landawn.abacus.util.Iterators
 * @see com.landawn.abacus.util.Maps
 * @see com.landawn.abacus.util.Strings
 */
public final class Iterables {

    private Iterables() {
        // Utility class.
    }

    /**
     * Returns {@code OptionalChar.empty()} if the specified {@code Array/Collection} is {@code null} or empty.
     *
     * @param a
     * @return
     */
    @SafeVarargs
    public static OptionalChar min(final char... a) {
        return a == null || a.length == 0 ? OptionalChar.empty() : OptionalChar.of(N.min(a));
    }

    /**
     * Returns {@code OptionalByte.empty()} if the specified {@code Array/Collection} is {@code null} or empty.
     *
     * @param a
     * @return
     */
    @SafeVarargs
    public static OptionalByte min(final byte... a) {
        return a == null || a.length == 0 ? OptionalByte.empty() : OptionalByte.of(N.min(a));
    }

    /**
     * Returns {@code OptionalShort.empty()} if the specified {@code Array/Collection} is {@code null} or empty.
     *
     * @param a
     * @return
     */
    @SafeVarargs
    public static OptionalShort min(final short... a) {
        return a == null || a.length == 0 ? OptionalShort.empty() : OptionalShort.of(N.min(a));
    }

    /**
     * Returns {@code OptionalInt.empty()} if the specified {@code Array/Collection} is {@code null} or empty.
     *
     * @param a
     * @return
     */
    @SafeVarargs
    public static OptionalInt min(final int... a) {
        return a == null || a.length == 0 ? OptionalInt.empty() : OptionalInt.of(N.min(a));
    }

    /**
     * Returns {@code OptionalLong.empty()} if the specified {@code Array/Collection} is {@code null} or empty.
     *
     * @param a
     * @return
     */
    @SafeVarargs
    public static OptionalLong min(final long... a) {
        return a == null || a.length == 0 ? OptionalLong.empty() : OptionalLong.of(N.min(a));
    }

    /**
     * Returns {@code OptionalFloat.empty()} if the specified {@code Array/Collection} is {@code null} or empty.
     *
     * @param a
     * @return
     */
    @SafeVarargs
    public static OptionalFloat min(final float... a) {
        return a == null || a.length == 0 ? OptionalFloat.empty() : OptionalFloat.of(N.min(a));
    }

    /**
     * Returns {@code OptionalDouble.empty()} if the specified {@code Array/Collection} is {@code null} or empty.
     *
     * @param a
     * @return
     */
    @SafeVarargs
    public static OptionalDouble min(final double... a) {
        return a == null || a.length == 0 ? OptionalDouble.empty() : OptionalDouble.of(N.min(a));
    }

    /**
     * Returns {@code Nullable.empty()} if the specified {@code Array/Collection} is {@code null} or empty.
     *
     * @param <T>
     * @param a
     * @return
     */
    public static <T extends Comparable<? super T>> Nullable<T> min(final T[] a) {
        return N.isEmpty(a) ? Nullable.<T> empty() : Nullable.of(N.min(a));
    }

    /**
     * Returns {@code Nullable.empty()} if the specified {@code Array/Collection} is {@code null} or empty.
     *
     * @param <T>
     * @param a
     * @param cmp
     * @return
     */
    public static <T> Nullable<T> min(final T[] a, final Comparator<? super T> cmp) {
        return N.isEmpty(a) ? Nullable.<T> empty() : Nullable.of(N.min(a, cmp));
    }

    /**
     * Returns {@code Nullable.empty()} if the specified {@code Array/Collection} is {@code null} or empty.
     *
     * @param <T>
     * @param c
     * @return
     */
    public static <T extends Comparable<? super T>> Nullable<T> min(final Iterable<? extends T> c) {
        return min(c, N.NULL_MAX_COMPARATOR);
    }

    /**
     * Returns {@code Nullable.empty()} if the specified {@code Array/Collection} is {@code null} or empty.
     *
     * @param <T>
     * @param c
     * @param cmp
     * @return
     */
    public static <T> Nullable<T> min(final Iterable<? extends T> c, final Comparator<? super T> cmp) {
        return c == null ? Nullable.<T> empty() : min(c.iterator(), cmp);
    }

    /**
     * Returns {@code Nullable.empty()} if the specified {@code Array/Collection} is {@code null} or empty.
     *
     * @param <T>
     * @param iter
     * @return
     */
    public static <T extends Comparable<? super T>> Nullable<T> min(final Iterator<? extends T> iter) {
        return min(iter, N.NULL_MAX_COMPARATOR);
    }

    /**
     * Returns {@code Nullable.empty()} if the specified {@code Array/Collection} is {@code null} or empty.
     *
     * @param <T>
     * @param iter
     * @param cmp
     * @return
     */
    public static <T> Nullable<T> min(final Iterator<? extends T> iter, Comparator<? super T> cmp) {
        cmp = cmp == null ? (Comparator<T>) N.NULL_MAX_COMPARATOR : cmp;

        if (iter == null || !iter.hasNext()) {
            return Nullable.<T> empty();
        }

        T candidate = null;
        T next = null;

        do {
            next = iter.next();

            if (next == null && cmp == N.NULL_MIN_COMPARATOR) { // NOSONAR
                return Nullable.of(next);
            } else if (cmp.compare(next, candidate) < 0) {
                candidate = next;
            }
        } while (iter.hasNext());

        return Nullable.of(candidate);
    }

    /**
     * Returns {@code Nullable.empty()} if the specified {@code Array/Collection} is {@code null} or empty.
     *
     * @param <T>
     * @param a
     * @param keyMapper
     * @return
     */
    @SuppressWarnings("rawtypes")
    public static <T> Nullable<T> minBy(final T[] a, final Function<? super T, ? extends Comparable> keyMapper) {
        return min(a, Comparators.nullsLastBy(keyMapper));
    }

    /**
     * Returns {@code Nullable.empty()} if the specified {@code Array/Collection} is {@code null} or empty.
     *
     * @param <T>
     * @param c
     * @param keyMapper
     * @return
     */
    @SuppressWarnings("rawtypes")
    public static <T> Nullable<T> minBy(final Iterable<? extends T> c, final Function<? super T, ? extends Comparable> keyMapper) {
        return min(c, Comparators.nullsLastBy(keyMapper));
    }

    /**
     * Returns {@code Nullable.empty()} if the specified {@code Array/Collection} is {@code null} or empty.
     *
     * @param <T>
     * @param iter
     * @param keyMapper
     * @return
     */
    @SuppressWarnings("rawtypes")
    public static <T> Nullable<T> minBy(final Iterator<? extends T> iter, final Function<? super T, ? extends Comparable> keyMapper) {
        return min(iter, Comparators.nullsLastBy(keyMapper));
    }

    /**
     * Returns {@code OptionalInt.empty()} if the specified {@code Array/Collection} is {@code null} or empty.
     *
     * @param <T>
     * @param a
     * @param valueExtractor
     * @return
     */
    @Beta
    public static <T> OptionalInt minInt(final T[] a, final ToIntFunction<? super T> valueExtractor) {
        if (N.isEmpty(a)) {
            return OptionalInt.empty();
        }

        int candicate = valueExtractor.applyAsInt(a[0]);
        int next = 0;

        for (int i = 1, len = a.length; i < len; i++) {
            next = valueExtractor.applyAsInt(a[i]);

            if (next < candicate) {
                candicate = next;
            }
        }

        return OptionalInt.of(candicate);
    }

    /**
     * Returns {@code OptionalInt.empty()} if the specified {@code Array/Collection} is {@code null} or empty.
     *
     * @param <T>
     * @param c
     * @param valueExtractor
     * @return
     */
    @Beta
    public static <T> OptionalInt minInt(final Iterable<? extends T> c, final ToIntFunction<? super T> valueExtractor) {
        if (c == null) {
            return OptionalInt.empty();
        }

        return minInt(c.iterator(), valueExtractor);
    }

    /**
     * Returns {@code OptionalInt.empty()} if the specified {@code Array/Collection} is {@code null} or empty.
     *
     * @param <T>
     * @param iter
     * @param valueExtractor
     * @return
     */
    @Beta
    public static <T> OptionalInt minInt(final Iterator<? extends T> iter, final ToIntFunction<? super T> valueExtractor) {
        if (iter == null || !iter.hasNext()) {
            return OptionalInt.empty();
        }

        int candicate = valueExtractor.applyAsInt(iter.next());
        int next = 0;

        while (iter.hasNext()) {
            next = valueExtractor.applyAsInt(iter.next());

            if (next < candicate) {
                candicate = next;
            }
        }

        return OptionalInt.of(candicate);
    }

    /**
     * Returns {@code OptionalLong.empty()} if the specified {@code Array/Collection} is {@code null} or empty.
     *
     * @param <T>
     * @param a
     * @param valueExtractor
     * @return
     */
    @Beta
    public static <T> OptionalLong minLong(final T[] a, final ToLongFunction<? super T> valueExtractor) {
        if (N.isEmpty(a)) {
            return OptionalLong.empty();
        }

        long candicate = valueExtractor.applyAsLong(a[0]);
        long next = 0;

        for (int i = 1, len = a.length; i < len; i++) {
            next = valueExtractor.applyAsLong(a[i]);

            if (next < candicate) {
                candicate = next;
            }
        }

        return OptionalLong.of(candicate);
    }

    /**
     * Returns {@code OptionalLong.empty()} if the specified {@code Array/Collection} is {@code null} or empty.
     *
     * @param <T>
     * @param c
     * @param valueExtractor
     * @return
     */
    @Beta
    public static <T> OptionalLong minLong(final Iterable<? extends T> c, final ToLongFunction<? super T> valueExtractor) {
        if (c == null) {
            return OptionalLong.empty();
        }

        return minLong(c.iterator(), valueExtractor);
    }

    /**
     * Returns {@code OptionalLong.empty()} if the specified {@code Array/Collection} is {@code null} or empty.
     *
     * @param <T>
     * @param iter
     * @param valueExtractor
     * @return
     */
    @Beta
    public static <T> OptionalLong minLong(final Iterator<? extends T> iter, final ToLongFunction<? super T> valueExtractor) {
        if (iter == null || !iter.hasNext()) {
            return OptionalLong.empty();
        }

        long candicate = valueExtractor.applyAsLong(iter.next());
        long next = 0;

        while (iter.hasNext()) {
            next = valueExtractor.applyAsLong(iter.next());

            if (next < candicate) {
                candicate = next;
            }
        }

        return OptionalLong.of(candicate);
    }

    /**
     * Returns {@code OptionalDouble.empty()} if the specified {@code Array/Collection} is {@code null} or empty.
     *
     * @param <T>
     * @param a
     * @param valueExtractor
     * @return
     */
    @Beta
    public static <T> OptionalDouble minDouble(final T[] a, final ToDoubleFunction<? super T> valueExtractor) {
        if (N.isEmpty(a)) {
            return OptionalDouble.empty();
        }

        double candicate = valueExtractor.applyAsDouble(a[0]);
        double next = 0;

        for (int i = 1, len = a.length; i < len; i++) {
            next = valueExtractor.applyAsDouble(a[i]);

            if (N.compare(next, candicate) < 0) {
                candicate = next;
            }
        }

        return OptionalDouble.of(candicate);
    }

    /**
     * Returns {@code OptionalDouble.empty()} if the specified {@code Array/Collection} is {@code null} or empty.
     *
     * @param <T>
     * @param c
     * @param valueExtractor
     * @return
     */
    @Beta
    public static <T> OptionalDouble minDouble(final Iterable<? extends T> c, final ToDoubleFunction<? super T> valueExtractor) {
        if (c == null) {
            return OptionalDouble.empty();
        }

        return minDouble(c.iterator(), valueExtractor);
    }

    /**
     * Returns {@code OptionalDouble.empty()} if the specified {@code Array/Collection} is {@code null} or empty.
     *
     * @param <T>
     * @param iter
     * @param valueExtractor
     * @return
     */
    @Beta
    public static <T> OptionalDouble minDouble(final Iterator<? extends T> iter, final ToDoubleFunction<? super T> valueExtractor) {
        if (iter == null || !iter.hasNext()) {
            return OptionalDouble.empty();
        }

        double candicate = valueExtractor.applyAsDouble(iter.next());
        double next = 0;

        while (iter.hasNext()) {
            next = valueExtractor.applyAsDouble(iter.next());

            if (N.compare(next, candicate) < 0) {
                candicate = next;
            }
        }

        return OptionalDouble.of(candicate);
    }

    /**
     * Returns {@code OptionalChar.empty()} if the specified {@code Array/Collection} is {@code null} or empty.
     *
     * @param a
     * @return
     */
    @SafeVarargs
    public static OptionalChar max(final char... a) {
        return a == null || a.length == 0 ? OptionalChar.empty() : OptionalChar.of(N.max(a));
    }

    /**
     * Returns {@code OptionalByte.empty()} if the specified {@code Array/Collection} is {@code null} or empty.
     *
     * @param a
     * @return
     */
    @SafeVarargs
    public static OptionalByte max(final byte... a) {
        return a == null || a.length == 0 ? OptionalByte.empty() : OptionalByte.of(N.max(a));
    }

    /**
     * Returns {@code OptionalShort.empty()} if the specified {@code Array/Collection} is {@code null} or empty.
     *
     * @param a
     * @return
     */
    @SafeVarargs
    public static OptionalShort max(final short... a) {
        return a == null || a.length == 0 ? OptionalShort.empty() : OptionalShort.of(N.max(a));
    }

    /**
     * Returns {@code OptionalInt.empty()} if the specified {@code Array/Collection} is {@code null} or empty.
     *
     * @param a
     * @return
     */
    @SafeVarargs
    public static OptionalInt max(final int... a) {
        return a == null || a.length == 0 ? OptionalInt.empty() : OptionalInt.of(N.max(a));
    }

    /**
     * Returns {@code OptionalLong.empty()} if the specified {@code Array/Collection} is {@code null} or empty.
     *
     * @param a
     * @return
     */
    @SafeVarargs
    public static OptionalLong max(final long... a) {
        return a == null || a.length == 0 ? OptionalLong.empty() : OptionalLong.of(N.max(a));
    }

    /**
     * Returns {@code OptionalFloat.empty()} if the specified {@code Array/Collection} is {@code null} or empty.
     *
     * @param a
     * @return
     */
    @SafeVarargs
    public static OptionalFloat max(final float... a) {
        return a == null || a.length == 0 ? OptionalFloat.empty() : OptionalFloat.of(N.max(a));
    }

    /**
     * Returns {@code OptionalDouble.empty()} if the specified {@code Array/Collection} is {@code null} or empty.
     *
     * @param a
     * @return
     */
    @SafeVarargs
    public static OptionalDouble max(final double... a) {
        return a == null || a.length == 0 ? OptionalDouble.empty() : OptionalDouble.of(N.max(a));
    }

    /**
     * Returns {@code Nullable.empty()} if the specified {@code Array/Collection} is {@code null} or empty.
     *
     * @param <T>
     * @param a
     * @return
     */
    public static <T extends Comparable<? super T>> Nullable<T> max(final T[] a) {
        return N.isEmpty(a) ? Nullable.<T> empty() : Nullable.of(N.max(a));
    }

    /**
     * Returns {@code Nullable.empty()} if the specified {@code Array/Collection} is {@code null} or empty.
     *
     * @param <T>
     * @param a
     * @param cmp
     * @return
     */
    public static <T> Nullable<T> max(final T[] a, final Comparator<? super T> cmp) {
        return N.isEmpty(a) ? Nullable.<T> empty() : Nullable.of(N.max(a, cmp));
    }

    /**
     * Returns {@code Nullable.empty()} if the specified {@code Array/Collection} is {@code null} or empty.
     *
     * @param <T>
     * @param c
     * @return
     */
    public static <T extends Comparable<? super T>> Nullable<T> max(final Iterable<? extends T> c) {
        return max(c, N.NULL_MIN_COMPARATOR);
    }

    /**
     * Returns {@code Nullable.empty()} if the specified {@code Array/Collection} is {@code null} or empty.
     *
     * @param <T>
     * @param c
     * @param cmp
     * @return
     */
    public static <T> Nullable<T> max(final Iterable<? extends T> c, final Comparator<? super T> cmp) {
        return c == null ? Nullable.<T> empty() : max(c.iterator(), cmp);
    }

    /**
     * Returns {@code Nullable.empty()} if the specified {@code Array/Collection} is {@code null} or empty.
     *
     * @param <T>
     * @param iter
     * @return
     */
    public static <T extends Comparable<? super T>> Nullable<T> max(final Iterator<? extends T> iter) {
        return max(iter, N.NULL_MIN_COMPARATOR);
    }

    /**
     * Returns {@code Nullable.empty()} if the specified {@code Array/Collection} is {@code null} or empty.
     *
     * @param <T>
     * @param iter
     * @param cmp
     * @return
     */
    public static <T> Nullable<T> max(final Iterator<? extends T> iter, Comparator<? super T> cmp) {
        cmp = cmp == null ? (Comparator<T>) N.NULL_MIN_COMPARATOR : cmp;

        if (iter == null || !iter.hasNext()) {
            return Nullable.<T> empty();
        }

        T candidate = null;
        T next = null;

        do {
            next = iter.next();

            if (next == null && cmp == N.NULL_MAX_COMPARATOR) { // NOSONAR
                return Nullable.of(next);
            } else if (cmp.compare(next, candidate) > 0) {
                candidate = next;
            }
        } while (iter.hasNext());

        return Nullable.of(candidate);
    }

    /**
     * Returns {@code Nullable.empty()} if the specified {@code Array/Collection} is {@code null} or empty.
     *
     * @param <T>
     * @param a
     * @param keyMapper
     * @return
     */
    @SuppressWarnings("rawtypes")
    public static <T> Nullable<T> maxBy(final T[] a, final Function<? super T, ? extends Comparable> keyMapper) {
        return max(a, Comparators.nullsFirstBy(keyMapper));
    }

    /**
     * Returns {@code Nullable.empty()} if the specified {@code Array/Collection} is {@code null} or empty.
     *
     * @param <T>
     * @param c
     * @param keyMapper
     * @return
     */
    @SuppressWarnings("rawtypes")
    public static <T> Nullable<T> maxBy(final Iterable<? extends T> c, final Function<? super T, ? extends Comparable> keyMapper) {
        return max(c, Comparators.nullsFirstBy(keyMapper));
    }

    /**
     * Returns {@code Nullable.empty()} if the specified {@code Array/Collection} is {@code null} or empty.
     *
     * @param <T>
     * @param iter
     * @param keyMapper
     * @return
     */
    @SuppressWarnings("rawtypes")
    public static <T> Nullable<T> maxBy(final Iterator<? extends T> iter, final Function<? super T, ? extends Comparable> keyMapper) {
        return max(iter, Comparators.nullsFirstBy(keyMapper));
    }

    /**
     * Returns {@code OptionalInt.empty()} if the specified {@code Array/Collection} is {@code null} or empty.
     *
     * @param <T>
     * @param a
     * @param valueExtractor
     * @return
     */
    @Beta
    public static <T> OptionalInt maxInt(final T[] a, final ToIntFunction<? super T> valueExtractor) {
        if (N.isEmpty(a)) {
            return OptionalInt.empty();
        }

        int candicate = valueExtractor.applyAsInt(a[0]);
        int next = 0;

        for (int i = 1, len = a.length; i < len; i++) {
            next = valueExtractor.applyAsInt(a[i]);

            if (next > candicate) {
                candicate = next;
            }
        }

        return OptionalInt.of(candicate);
    }

    /**
     * Returns {@code OptionalInt.empty()} if the specified {@code Array/Collection} is {@code null} or empty.
     *
     * @param <T>
     * @param c
     * @param valueExtractor
     * @return
     */
    @Beta
    public static <T> OptionalInt maxInt(final Iterable<? extends T> c, final ToIntFunction<? super T> valueExtractor) {
        if (c == null) {
            return OptionalInt.empty();
        }

        return maxInt(c.iterator(), valueExtractor);
    }

    /**
     * Returns {@code OptionalInt.empty()} if the specified {@code Array/Collection} is {@code null} or empty.
     *
     * @param <T>
     * @param iter
     * @param valueExtractor
     * @return
     */
    @Beta
    public static <T> OptionalInt maxInt(final Iterator<? extends T> iter, final ToIntFunction<? super T> valueExtractor) {
        if (iter == null || !iter.hasNext()) {
            return OptionalInt.empty();
        }

        int candicate = valueExtractor.applyAsInt(iter.next());
        int next = 0;

        while (iter.hasNext()) {
            next = valueExtractor.applyAsInt(iter.next());

            if (next > candicate) {
                candicate = next;
            }
        }

        return OptionalInt.of(candicate);
    }

    /**
     * Returns {@code OptionalLong.empty()} if the specified {@code Array/Collection} is {@code null} or empty.
     *
     * @param <T>
     * @param a
     * @param valueExtractor
     * @return
     */
    @Beta
    public static <T> OptionalLong maxLong(final T[] a, final ToLongFunction<? super T> valueExtractor) {
        if (N.isEmpty(a)) {
            return OptionalLong.empty();
        }

        long candicate = valueExtractor.applyAsLong(a[0]);
        long next = 0;

        for (int i = 1, len = a.length; i < len; i++) {
            next = valueExtractor.applyAsLong(a[i]);

            if (next > candicate) {
                candicate = next;
            }
        }

        return OptionalLong.of(candicate);
    }

    /**
     * Returns {@code OptionalLong.empty()} if the specified {@code Array/Collection} is {@code null} or empty.
     *
     * @param <T>
     * @param c
     * @param valueExtractor
     * @return
     */
    @Beta
    public static <T> OptionalLong maxLong(final Iterable<? extends T> c, final ToLongFunction<? super T> valueExtractor) {
        if (c == null) {
            return OptionalLong.empty();
        }

        return maxLong(c.iterator(), valueExtractor);
    }

    /**
     * Returns {@code OptionalLong.empty()} if the specified {@code Array/Collection} is {@code null} or empty.
     *
     * @param <T>
     * @param iter
     * @param valueExtractor
     * @return
     */
    @Beta
    public static <T> OptionalLong maxLong(final Iterator<? extends T> iter, final ToLongFunction<? super T> valueExtractor) {
        if (iter == null || !iter.hasNext()) {
            return OptionalLong.empty();
        }

        long candicate = valueExtractor.applyAsLong(iter.next());
        long next = 0;

        while (iter.hasNext()) {
            next = valueExtractor.applyAsLong(iter.next());

            if (next > candicate) {
                candicate = next;
            }
        }

        return OptionalLong.of(candicate);
    }

    /**
     * Returns {@code OptionalDouble.empty()} if the specified {@code Array/Collection} is {@code null} or empty.
     *
     * @param <T>
     * @param a
     * @param valueExtractor
     * @return
     */
    @Beta
    public static <T> OptionalDouble maxDouble(final T[] a, final ToDoubleFunction<? super T> valueExtractor) {
        if (N.isEmpty(a)) {
            return OptionalDouble.empty();
        }

        double candicate = valueExtractor.applyAsDouble(a[0]);
        double next = 0;

        for (int i = 1, len = a.length; i < len; i++) {
            next = valueExtractor.applyAsDouble(a[i]);

            if (N.compare(next, candicate) > 0) {
                candicate = next;
            }
        }

        return OptionalDouble.of(candicate);
    }

    /**
     * Returns {@code OptionalDouble.empty()} if the specified {@code Array/Collection} is {@code null} or empty.
     *
     * @param <T>
     * @param c
     * @param valueExtractor
     * @return
     */
    @Beta
    public static <T> OptionalDouble maxDouble(final Iterable<? extends T> c, final ToDoubleFunction<? super T> valueExtractor) {
        if (c == null) {
            return OptionalDouble.empty();
        }

        return maxDouble(c.iterator(), valueExtractor);
    }

    /**
     * Returns {@code OptionalDouble.empty()} if the specified {@code Array/Collection} is {@code null} or empty.
     *
     * @param <T>
     * @param iter
     * @param valueExtractor
     * @return
     */
    @Beta
    public static <T> OptionalDouble maxDouble(final Iterator<? extends T> iter, final ToDoubleFunction<? super T> valueExtractor) {
        if (iter == null || !iter.hasNext()) {
            return OptionalDouble.empty();
        }

        double candicate = valueExtractor.applyAsDouble(iter.next());
        double next = 0;

        while (iter.hasNext()) {
            next = valueExtractor.applyAsDouble(iter.next());

            if (N.compare(next, candicate) > 0) {
                candicate = next;
            }
        }

        return OptionalDouble.of(candicate);
    }

    /**
     *
     * @param <T>
     * @param a
     * @return
     */
    public static <T extends Comparable<? super T>> Optional<Pair<T, T>> minMax(final T[] a) {
        return N.isEmpty(a) ? Optional.<Pair<T, T>> empty() : Optional.of(N.minMax(a));
    }

    /**
     *
     * @param <T>
     * @param a
     * @param cmp
     * @return
     */
    public static <T> Optional<Pair<T, T>> minMax(final T[] a, final Comparator<? super T> cmp) {
        return N.isEmpty(a) ? Optional.<Pair<T, T>> empty() : Optional.of(N.minMax(a, cmp));
    }

    /**
     *
     * @param <T>
     * @param c
     * @return
     */
    public static <T extends Comparable<? super T>> Optional<Pair<T, T>> minMax(final Iterable<? extends T> c) {
        final Iterator<? extends T> iter = c == null ? null : c.iterator();

        return iter == null || !iter.hasNext() ? Optional.<Pair<T, T>> empty() : Optional.of(N.minMax(iter));
    }

    /**
     *
     * @param <T>
     * @param c
     * @param cmp
     * @return
     */
    public static <T> Optional<Pair<T, T>> minMax(final Iterable<? extends T> c, final Comparator<? super T> cmp) {
        final Iterator<? extends T> iter = c == null ? null : c.iterator();

        return iter == null || !iter.hasNext() ? Optional.<Pair<T, T>> empty() : Optional.of(N.minMax(iter, cmp));
    }

    /**
     *
     * @param <T>
     * @param iter
     * @return
     * @see Iterables#minMax(Iterator)
     */
    public static <T extends Comparable<? super T>> Optional<Pair<T, T>> minMax(final Iterator<? extends T> iter) {
        return iter == null || !iter.hasNext() ? Optional.<Pair<T, T>> empty() : Optional.of(N.minMax(iter));
    }

    /**
     *
     * @param <T>
     * @param iter
     * @param cmp
     * @return
     * @see Iterables#minMax(Iterator, Comparator)
     */
    public static <T> Optional<Pair<T, T>> minMax(final Iterator<? extends T> iter, final Comparator<? super T> cmp) {
        return iter == null || !iter.hasNext() ? Optional.<Pair<T, T>> empty() : Optional.of(N.minMax(iter, cmp));
    }

    /**
     * Returns {@code Nullable.empty()} if the specified {@code Array/Collection} is {@code null} or empty.
     *
     * @param <T>
     * @param a
     * @return
     */
    public static <T extends Comparable<? super T>> Nullable<T> median(final T[] a) {
        return N.isEmpty(a) ? Nullable.<T> empty() : Nullable.of(N.median(a));
    }

    /**
     * Returns {@code Nullable.empty()} if the specified {@code Array/Collection} is {@code null} or empty.
     *
     * @param <T>
     * @param a
     * @param cmp
     * @return
     */
    public static <T> Nullable<T> median(final T[] a, final Comparator<? super T> cmp) {
        return N.isEmpty(a) ? Nullable.<T> empty() : Nullable.of(N.median(a, cmp));
    }

    /**
     * Returns {@code Nullable.empty()} if the specified {@code Array/Collection} is {@code null} or empty.
     *
     * @param <T>
     * @param c
     * @return
     */
    public static <T extends Comparable<? super T>> Nullable<T> median(final Collection<? extends T> c) {
        return N.isEmpty(c) ? Nullable.<T> empty() : Nullable.of(N.median(c));
    }

    /**
     * Returns {@code Nullable.empty()} if the specified {@code Array/Collection} is {@code null} or empty.
     *
     * @param <T>
     * @param c
     * @param cmp
     * @return
     */
    public static <T> Nullable<T> median(final Collection<? extends T> c, final Comparator<? super T> cmp) {
        return N.isEmpty(c) ? Nullable.<T> empty() : Nullable.of(N.median(c, cmp));
    }

    //    /**
    //     * Returns {@code Nullable.empty()} if the specified {@code Array/Collection} is {@code null} or empty.
    //     *
    //     * @param <T>
    //     * @param a
    //     * @param keyMapper
    //     * @return
    //     * @deprecated
    //     * @see Comparators#comparingBy(Function)
    //     */
    //    @Deprecated
    //    @Beta
    //    @SuppressWarnings("rawtypes")
    //    public static <T> Nullable<T> medianBy(final T[] a, final Function<? super T, ? extends Comparable> keyMapper) {
    //        return median(a, Comparators.comparingBy(keyMapper));
    //    }
    //
    //    /**
    //     * Returns {@code Nullable.empty()} if the specified {@code Array/Collection} is {@code null} or empty.
    //     *
    //     * @param <T>
    //     * @param c
    //     * @param keyMapper
    //     * @return
    //     * @deprecated
    //     * @see Comparators#comparingBy(Function)
    //     */
    //    @Deprecated
    //    @Beta
    //    @SuppressWarnings("rawtypes")
    //    public static <T> Nullable<T> medianBy(final Collection<? extends T> c, final Function<? super T, ? extends Comparable> keyMapper) {
    //        return median(c, Comparators.comparingBy(keyMapper));
    //    }

    /**
     * Returns {@code Nullable.empty()} if the specified {@code Array/Collection} is {@code null} or empty, or its length/size is less than {@code k}.
     *
     * @param <T>
     * @param a
     * @param k
     * @return
     */
    public static <T extends Comparable<? super T>> Nullable<T> kthLargest(final T[] a, final int k) {
        return N.isEmpty(a) || a.length < k ? Nullable.<T> empty() : Nullable.of(N.kthLargest(a, k));
    }

    /**
     * Returns {@code Nullable.empty()} if the specified {@code Array/Collection} is {@code null} or empty, or its length/size is less than {@code k}.
     *
     * @param <T>
     * @param a
     * @param k
     * @param cmp
     * @return
     */
    public static <T> Nullable<T> kthLargest(final T[] a, final int k, final Comparator<? super T> cmp) {
        return N.isEmpty(a) || a.length < k ? Nullable.<T> empty() : Nullable.of(N.kthLargest(a, k, cmp));
    }

    /**
     * Returns {@code Nullable.empty()} if the specified {@code Array/Collection} is {@code null} or empty, or its length/size is less than {@code k}.
     *
     * @param <T>
     * @param c
     * @param k
     * @return
     */
    public static <T extends Comparable<? super T>> Nullable<T> kthLargest(final Collection<? extends T> c, final int k) {
        return N.isEmpty(c) || c.size() < k ? Nullable.<T> empty() : Nullable.of(N.kthLargest(c, k));
    }

    /**
     * Returns the kth largest element from the provided collection based on the provided comparator.
     * If the collection is null, empty, or its size is less than k, a Nullable.empty() is returned.
     *
     * @param <T> The type of elements in the collection.
     * @param c The collection from which to find the kth largest element.
     * @param k The position from the end of a sorted list of the collection's elements (1-based index).
     * @param cmp The comparator used to determine the order of the collection's elements.
     * @return A Nullable containing the kth largest element if it exists, otherwise Nullable.empty().
     */
    public static <T> Nullable<T> kthLargest(final Collection<? extends T> c, final int k, final Comparator<? super T> cmp) {
        return N.isEmpty(c) || c.size() < k ? Nullable.<T> empty() : Nullable.of(N.kthLargest(c, k, cmp));
    }

    /**
     * Returns {@code OptionalLong.empty()} if the specified {@code Array/Collection} is {@code null} or empty, or {@code fromIndex == toIndex}.
     *
     * @param <T>
     * @param c
     * @return
     */
    public static <T extends Number> OptionalInt sumInt(final Iterable<? extends T> c) {
        return sumInt(c, Fn.numToInt());
    }

    /**
     * Returns {@code OptionalLong.empty()} if the specified {@code Array/Collection} is {@code null} or empty, or {@code fromIndex == toIndex}.
     *
     * @param <T>
     * @param c
     * @param func
     * @return
     */
    public static <T> OptionalInt sumInt(final Iterable<? extends T> c, final ToIntFunction<? super T> func) {
        final Iterator<? extends T> iter = c == null ? ObjIterator.<T> empty() : c.iterator();

        if (iter.hasNext() == false) {
            return OptionalInt.empty();
        }

        return OptionalInt.of(N.sumInt(c, func));
    }

    /**
     * Returns {@code OptionalLong.empty()} if the specified {@code Array/Collection} is {@code null} or empty, or {@code fromIndex == toIndex}.
     *
     * @param <T>
     * @param c
     * @return
     */
    public static <T extends Number> OptionalLong sumIntToLong(final Iterable<? extends T> c) {
        return sumIntToLong(c, Fn.numToInt());
    }

    /**
     * Returns {@code OptionalLong.empty()} if the specified {@code Array/Collection} is {@code null} or empty, or {@code fromIndex == toIndex}.
     *
     * @param <T>
     * @param c
     * @param func
     * @return
     */
    public static <T> OptionalLong sumIntToLong(final Iterable<? extends T> c, final ToIntFunction<? super T> func) {
        final Iterator<? extends T> iter = c == null ? ObjIterator.<T> empty() : c.iterator();

        if (iter.hasNext() == false) {
            return OptionalLong.empty();
        }

        return OptionalLong.of(N.sumIntToLong(c, func));
    }

    /**
     * Returns {@code OptionalLong.empty()} if the specified {@code Array/Collection} is {@code null} or empty, or {@code fromIndex == toIndex}.
     *
     * @param <T>
     * @param c
     * @return
     */
    public static <T extends Number> OptionalLong sumLong(final Iterable<? extends T> c) {
        return sumLong(c, Fn.numToLong());
    }

    /**
     * Returns {@code OptionalLong.empty()} if the specified {@code Array/Collection} is {@code null} or empty, or {@code fromIndex == toIndex}.
     *
     * @param <T>
     * @param c
     * @param func
     * @return
     */
    public static <T> OptionalLong sumLong(final Iterable<? extends T> c, final ToLongFunction<? super T> func) {
        final Iterator<? extends T> iter = c == null ? ObjIterator.<T> empty() : c.iterator();

        if (iter.hasNext() == false) {
            return OptionalLong.empty();
        }

        return OptionalLong.of(N.sumLong(c, func));
    }

    /**
     * Returns {@code OptionalDouble.empty()} if the specified {@code Array/Collection} is {@code null} or empty, or {@code fromIndex == toIndex}.
     *
     * @param <T>
     * @param c
     * @return
     */
    public static <T extends Number> OptionalDouble sumDouble(final Iterable<? extends T> c) {
        return sumDouble(c, Fn.numToDouble());
    }

    /**
     * Returns {@code OptionalDouble.empty()} if the specified {@code Array/Collection} is {@code null} or empty, or {@code fromIndex == toIndex}.
     *
     * @param <T>
     * @param c
     * @param func
     * @return
     */
    public static <T> OptionalDouble sumDouble(final Iterable<? extends T> c, final ToDoubleFunction<? super T> func) {
        final Iterator<? extends T> iter = c == null ? ObjIterator.<T> empty() : c.iterator();

        if (iter.hasNext() == false) {
            return OptionalDouble.empty();
        }

        return OptionalDouble.of(N.sumDouble(c, func));
    }

    /**
     *
     * @param c
     * @return
     */
    public static Optional<BigInteger> sumBigInteger(final Iterable<? extends BigInteger> c) {
        return sumBigInteger(c, Fn.identity());
    }

    /**
     *
     *
     * @param <T>
     * @param c
     * @param func
     * @return
     */
    public static <T> Optional<BigInteger> sumBigInteger(final Iterable<? extends T> c, final Function<? super T, BigInteger> func) {
        final Iterator<? extends T> iter = c == null ? ObjIterator.<T> empty() : c.iterator();

        if (iter.hasNext() == false) {
            return Optional.empty();
        }

        return Optional.of(N.sumBigInteger(c, func));
    }

    /**
     *
     * @param c
     * @return
     */
    public static Optional<BigDecimal> sumBigDecimal(final Iterable<? extends BigDecimal> c) {
        return sumBigDecimal(c, Fn.identity());
    }

    /**
     *
     *
     * @param <T>
     * @param c
     * @param func
     * @return
     */
    public static <T> Optional<BigDecimal> sumBigDecimal(final Iterable<? extends T> c, final Function<? super T, BigDecimal> func) {
        final Iterator<? extends T> iter = c == null ? ObjIterator.<T> empty() : c.iterator();

        if (iter.hasNext() == false) {
            return Optional.empty();
        }

        return Optional.of(N.sumBigDecimal(c, func));
    }

    /**
     * Returns {@code OptionalDouble.empty()} if the specified {@code Array/Collection} is {@code null} or empty, or {@code fromIndex == toIndex}.
     *
     * @param <T>
     * @param a
     * @return
     */
    public static <T extends Number> OptionalDouble averageInt(final T[] a) {
        return averageInt(a, Fn.numToInt());
    }

    /**
     * Returns {@code OptionalDouble.empty()} if the specified {@code Array/Collection} is {@code null} or empty, or {@code fromIndex == toIndex}.
     *
     * @param <T>
     * @param a
     * @param fromIndex
     * @param toIndex
     * @return
     */
    public static <T extends Number> OptionalDouble averageInt(final T[] a, final int fromIndex, final int toIndex) {
        return averageInt(a, fromIndex, toIndex, Fn.numToInt());
    }

    /**
     * Returns {@code OptionalDouble.empty()} if the specified {@code Array/Collection} is {@code null} or empty, or {@code fromIndex == toIndex}.
     *
     * @param <T>
     * @param a
     * @param func
     * @return
     */
    public static <T> OptionalDouble averageInt(final T[] a, final ToIntFunction<? super T> func) {
        if (N.isEmpty(a)) {
            return OptionalDouble.empty();
        }

        return averageInt(a, 0, a.length, func);
    }

    /**
     * Returns {@code OptionalDouble.empty()} if the specified {@code Array/Collection} is {@code null} or empty, or {@code fromIndex == toIndex}.
     *
     * @param <T>
     * @param a
     * @param fromIndex
     * @param toIndex
     * @param func
     * @return
     * @throws IndexOutOfBoundsException
     */
    public static <T> OptionalDouble averageInt(final T[] a, final int fromIndex, final int toIndex, final ToIntFunction<? super T> func)
            throws IndexOutOfBoundsException {
        N.checkFromToIndex(fromIndex, toIndex, N.len(a));

        if (fromIndex == toIndex) {
            return OptionalDouble.empty();
        }

        return OptionalDouble.of(N.averageInt(a, fromIndex, toIndex, func));
    }

    /**
     * Returns {@code OptionalDouble.empty()} if the specified {@code Array/Collection} is {@code null} or empty, or {@code fromIndex == toIndex}.
     *
     * @param <T>
     * @param c
     * @param fromIndex
     * @param toIndex
     * @return
     */
    public static <T extends Number> OptionalDouble averageInt(final Collection<? extends T> c, final int fromIndex, final int toIndex) {
        return averageInt(c, fromIndex, toIndex, Fn.numToInt());
    }

    /**
     * Returns {@code OptionalDouble.empty()} if the specified {@code Array/Collection} is {@code null} or empty, or {@code fromIndex == toIndex}.
     *
     * @param <T>
     * @param c
     * @param fromIndex
     * @param toIndex
     * @param func
     * @return
     * @throws IndexOutOfBoundsException
     */
    public static <T> OptionalDouble averageInt(final Collection<? extends T> c, final int fromIndex, final int toIndex, final ToIntFunction<? super T> func)
            throws IndexOutOfBoundsException {
        N.checkFromToIndex(fromIndex, toIndex, N.size(c));

        if (fromIndex == toIndex) {
            return OptionalDouble.empty();
        }

        return OptionalDouble.of(N.averageInt(c, fromIndex, toIndex, func));
    }

    /**
     * Returns {@code OptionalDouble.empty()} if the specified {@code Array/Collection} is {@code null} or empty, or {@code fromIndex == toIndex}.
     *
     * @param <T>
     * @param c
     * @return
     */
    public static <T extends Number> OptionalDouble averageInt(final Iterable<? extends T> c) {
        return averageInt(c, Fn.numToInt());
    }

    /**
     * Returns {@code OptionalDouble.empty()} if the specified {@code Array/Collection} is {@code null} or empty, or {@code fromIndex == toIndex}.
     *
     * @param <T>
     * @param c
     * @param func
     * @return
     */
    public static <T> OptionalDouble averageInt(final Iterable<? extends T> c, final ToIntFunction<? super T> func) {
        final Iterator<? extends T> iter = c == null ? ObjIterator.<T> empty() : c.iterator();

        if (iter.hasNext() == false) {
            return OptionalDouble.empty();
        }

        return OptionalDouble.of(N.averageInt(c, func));
    }

    /**
     * Returns {@code OptionalDouble.empty()} if the specified {@code Array/Collection} is {@code null} or empty, or {@code fromIndex == toIndex}.
     *
     * @param <T>
     * @param a
     * @return
     */
    public static <T extends Number> OptionalDouble averageLong(final T[] a) {
        return averageLong(a, Fn.numToLong());
    }

    /**
     * Returns {@code OptionalDouble.empty()} if the specified {@code Array/Collection} is {@code null} or empty, or {@code fromIndex == toIndex}.
     *
     * @param <T>
     * @param a
     * @param fromIndex
     * @param toIndex
     * @return
     */
    public static <T extends Number> OptionalDouble averageLong(final T[] a, final int fromIndex, final int toIndex) {
        return averageLong(a, fromIndex, toIndex, Fn.numToLong());
    }

    /**
     * Returns {@code OptionalDouble.empty()} if the specified {@code Array/Collection} is {@code null} or empty, or {@code fromIndex == toIndex}.
     *
     * @param <T>
     * @param a
     * @param func
     * @return
     */
    public static <T> OptionalDouble averageLong(final T[] a, final ToLongFunction<? super T> func) {
        if (N.isEmpty(a)) {
            return OptionalDouble.empty();
        }

        return averageLong(a, 0, a.length, func);
    }

    /**
     * Returns {@code OptionalDouble.empty()} if the specified {@code Array/Collection} is {@code null} or empty, or {@code fromIndex == toIndex}.
     *
     * @param <T>
     * @param a
     * @param fromIndex
     * @param toIndex
     * @param func
     * @return
     * @throws IndexOutOfBoundsException
     */
    public static <T> OptionalDouble averageLong(final T[] a, final int fromIndex, final int toIndex, final ToLongFunction<? super T> func)
            throws IndexOutOfBoundsException {
        N.checkFromToIndex(fromIndex, toIndex, N.len(a));

        if (fromIndex == toIndex) {
            return OptionalDouble.empty();
        }

        return OptionalDouble.of(N.averageLong(a, fromIndex, toIndex, func));
    }

    /**
     * Returns {@code OptionalDouble.empty()} if the specified {@code Array/Collection} is {@code null} or empty, or {@code fromIndex == toIndex}.
     *
     * @param <T>
     * @param c
     * @param fromIndex
     * @param toIndex
     * @return
     */
    public static <T extends Number> OptionalDouble averageLong(final Collection<? extends T> c, final int fromIndex, final int toIndex) {
        return averageLong(c, fromIndex, toIndex, Fn.numToLong());
    }

    /**
     * Returns {@code OptionalDouble.empty()} if the specified {@code Array/Collection} is {@code null} or empty, or {@code fromIndex == toIndex}.
     *
     * @param <T>
     * @param c
     * @param fromIndex
     * @param toIndex
     * @param func
     * @return
     * @throws IndexOutOfBoundsException
     */
    public static <T> OptionalDouble averageLong(final Collection<? extends T> c, final int fromIndex, final int toIndex, final ToLongFunction<? super T> func)
            throws IndexOutOfBoundsException {
        N.checkFromToIndex(fromIndex, toIndex, N.size(c));

        if (fromIndex == toIndex) {
            return OptionalDouble.empty();
        }

        return OptionalDouble.of(N.averageLong(c, fromIndex, toIndex, func));
    }

    /**
     * Returns {@code OptionalDouble.empty()} if the specified {@code Array/Collection} is {@code null} or empty, or {@code fromIndex == toIndex}.
     *
     * @param <T>
     * @param c
     * @return
     */
    public static <T extends Number> OptionalDouble averageLong(final Iterable<? extends T> c) {
        return averageLong(c, Fn.numToLong());
    }

    /**
     * Returns {@code OptionalDouble.empty()} if the specified {@code Array/Collection} is {@code null} or empty, or {@code fromIndex == toIndex}.
     *
     * @param <T>
     * @param c
     * @param func
     * @return
     */
    public static <T> OptionalDouble averageLong(final Iterable<? extends T> c, final ToLongFunction<? super T> func) {
        final Iterator<? extends T> iter = c == null ? ObjIterator.<T> empty() : c.iterator();

        if (iter.hasNext() == false) {
            return OptionalDouble.empty();
        }

        return OptionalDouble.of(N.averageLong(c, func));
    }

    /**
     * Returns {@code OptionalDouble.empty()} if the specified {@code Array/Collection} is {@code null} or empty, or {@code fromIndex == toIndex}.
     *
     * @param <T>
     * @param a
     * @return
     */
    public static <T extends Number> OptionalDouble averageDouble(final T[] a) {
        return averageDouble(a, Fn.numToDouble());
    }

    /**
     * Returns {@code OptionalDouble.empty()} if the specified {@code Array/Collection} is {@code null} or empty, or {@code fromIndex == toIndex}.
     *
     * @param <T>
     * @param a
     * @param fromIndex
     * @param toIndex
     * @return
     */
    public static <T extends Number> OptionalDouble averageDouble(final T[] a, final int fromIndex, final int toIndex) {
        return averageDouble(a, fromIndex, toIndex, Fn.numToDouble());
    }

    /**
     * Returns {@code OptionalDouble.empty()} if the specified {@code Array/Collection} is {@code null} or empty, or {@code fromIndex == toIndex}.
     *
     * @param <T>
     * @param a
     * @param func
     * @return
     */
    public static <T> OptionalDouble averageDouble(final T[] a, final ToDoubleFunction<? super T> func) {
        if (N.isEmpty(a)) {
            return OptionalDouble.empty();
        }

        return averageDouble(a, 0, a.length, func);
    }

    /**
     * Returns {@code OptionalDouble.empty()} if the specified {@code Array/Collection} is {@code null} or empty, or {@code fromIndex == toIndex}.
     *
     * @param <T>
     * @param a
     * @param fromIndex
     * @param toIndex
     * @param func
     * @return
     * @throws IndexOutOfBoundsException
     */
    public static <T> OptionalDouble averageDouble(final T[] a, final int fromIndex, final int toIndex, final ToDoubleFunction<? super T> func)
            throws IndexOutOfBoundsException {
        N.checkFromToIndex(fromIndex, toIndex, N.len(a));

        if (fromIndex == toIndex) {
            return OptionalDouble.empty();
        }

        final KahanSummation summation = new KahanSummation();

        for (int i = fromIndex; i < toIndex; i++) {
            summation.add(func.applyAsDouble(a[i]));
        }

        return summation.average();
    }

    /**
     * Returns {@code OptionalDouble.empty()} if the specified {@code Array/Collection} is {@code null} or empty, or {@code fromIndex == toIndex}.
     *
     * @param <T>
     * @param c
     * @param fromIndex
     * @param toIndex
     * @return
     */
    public static <T extends Number> OptionalDouble averageDouble(final Collection<? extends T> c, final int fromIndex, final int toIndex) {
        return averageDouble(c, fromIndex, toIndex, Fn.numToDouble());
    }

    /**
     * Returns {@code OptionalDouble.empty()} if the specified {@code Array/Collection} is {@code null} or empty, or {@code fromIndex == toIndex}.
     *
     * @param <T>
     * @param c
     * @param fromIndex
     * @param toIndex
     * @param func
     * @return
     * @throws IndexOutOfBoundsException
     */
    public static <T> OptionalDouble averageDouble(final Collection<? extends T> c, final int fromIndex, final int toIndex,
            final ToDoubleFunction<? super T> func) throws IndexOutOfBoundsException {
        N.checkFromToIndex(fromIndex, toIndex, N.size(c));

        if (fromIndex == toIndex) {
            return OptionalDouble.empty();
        }

        final KahanSummation summation = new KahanSummation();

        if (c instanceof List && c instanceof RandomAccess) {
            final List<T> list = (List<T>) c;

            for (int i = fromIndex; i < toIndex; i++) {
                summation.add(func.applyAsDouble(list.get(i)));
            }
        } else {
            int idx = 0;

            for (final T e : c) {
                if (idx++ < fromIndex) {
                    continue;
                }

                summation.add(func.applyAsDouble(e));

                if (idx >= toIndex) {
                    break;
                }
            }
        }

        return summation.average();
    }

    /**
     * Returns {@code OptionalDouble.empty()} if the specified {@code Array/Collection} is {@code null} or empty, or {@code fromIndex == toIndex}.
     *
     * @param <T>
     * @param c
     * @return
     */
    public static <T extends Number> OptionalDouble averageDouble(final Iterable<? extends T> c) {
        return averageDouble(c, Fn.numToDouble());
    }

    /**
     * Returns {@code OptionalDouble.empty()} if the specified {@code Array/Collection} is {@code null} or empty, or {@code fromIndex == toIndex}.
     *
     * @param <T>
     * @param c
     * @param func
     * @return
     */
    public static <T> OptionalDouble averageDouble(final Iterable<? extends T> c, final ToDoubleFunction<? super T> func) {
        if (c == null) {
            return OptionalDouble.empty();
        }

        final KahanSummation summation = new KahanSummation();

        for (final T e : c) {
            summation.add(func.applyAsDouble(e));
        }

        return summation.average();
    }

    /**
     *
     * @param c
     * @return
     */
    public static Optional<BigDecimal> averageBigInteger(final Iterable<? extends BigInteger> c) {
        return averageBigInteger(c, Fn.identity());
    }

    /**
     *
     *
     * @param <T>
     * @param c
     * @param func
     * @return
     */
    public static <T> Optional<BigDecimal> averageBigInteger(final Iterable<? extends T> c, final Function<? super T, BigInteger> func) {
        final Iterator<? extends T> iter = c == null ? ObjIterator.<T> empty() : c.iterator();

        if (iter.hasNext() == false) {
            return Optional.empty();
        }

        return Optional.of(N.averageBigInteger(c, func));
    }

    /**
     *
     * @param c
     * @return
     */
    public static Optional<BigDecimal> averageBigDecimal(final Iterable<? extends BigDecimal> c) {
        return averageBigDecimal(c, Fn.identity());
    }

    /**
     *
     *
     * @param <T>
     * @param c
     * @param func
     * @return
     */
    public static <T> Optional<BigDecimal> averageBigDecimal(final Iterable<? extends T> c, final Function<? super T, BigDecimal> func) {
        final Iterator<? extends T> iter = c == null ? ObjIterator.<T> empty() : c.iterator();

        if (iter.hasNext() == false) {
            return Optional.empty();
        }

        return Optional.of(N.averageBigDecimal(c, func));
    }

    /**
     *
     *
     * @param a
     * @param valueToFind
     * @return
     * @see Index#of(Object[], Object)
     */
    public static OptionalInt indexOf(final Object[] a, final Object valueToFind) {
        return Index.of(a, valueToFind);
    }

    /**
     *
     *
     * @param c
     * @param valueToFind
     * @return
     * @see Index#of(Collection, Object)
     */
    public static OptionalInt indexOf(final Collection<?> c, final Object valueToFind) {
        return Index.of(c, valueToFind);
    }

    /**
     *
     *
     * @param a
     * @param valueToFind
     * @return
     * @see Index#last(Object[], Object)
     */
    public static OptionalInt lastIndexOf(final Object[] a, final Object valueToFind) {
        return Index.last(a, valueToFind);
    }

    /**
     *
     *
     * @param c
     * @param valueToFind
     * @return
     * @see Index#last(Collection, Object)
     */
    public static OptionalInt lastIndexOf(final Collection<?> c, final Object valueToFind) {
        return Index.last(c, valueToFind);
    }

    /**
     * Find first or last.
     *
     * @param <T>
     * @param a
     * @param predicateForFirst
     * @param predicateForLast
     * @return the nullable
     */
    public static <T> Nullable<T> findFirstOrLast(final T[] a, final Predicate<? super T> predicateForFirst, final Predicate<? super T> predicateForLast) {
        if (N.isEmpty(a)) {
            return Nullable.<T> empty();
        }

        final Nullable<T> res = N.findFirst(a, predicateForFirst);

        return res.isPresent() ? res : N.findLast(a, predicateForLast);
    }

    /**
     * Find first or last.
     *
     * @param <T>
     * @param c
     * @param predicateForFirst
     * @param predicateForLast
     * @return the nullable
     */
    public static <T> Nullable<T> findFirstOrLast(final Collection<? extends T> c, final Predicate<? super T> predicateForFirst,
            final Predicate<? super T> predicateForLast) {
        if (N.isEmpty(c)) {
            return Nullable.<T> empty();
        }

        final Nullable<T> res = N.findFirst(c, predicateForFirst);

        return res.isPresent() ? res : N.findLast(c, predicateForLast);
    }

    /**
     * Find first or last index.
     *
     * @param <T>
     * @param a
     * @param predicateForFirst
     * @param predicateForLast
     * @return the optional int
     */
    public static <T> OptionalInt findFirstOrLastIndex(final T[] a, final Predicate<? super T> predicateForFirst, final Predicate<? super T> predicateForLast) {
        if (N.isEmpty(a)) {
            return OptionalInt.empty();
        }

        final OptionalInt res = N.findFirstIndex(a, predicateForFirst);

        return res.isPresent() ? res : N.findLastIndex(a, predicateForLast);
    }

    /**
     *
     *
     * @param <T>
     * @param c
     * @param predicateForFirst
     * @param predicateForLast
     * @return
     */
    public static <T> OptionalInt findFirstOrLastIndex(final Collection<? extends T> c, final Predicate<? super T> predicateForFirst,
            final Predicate<? super T> predicateForLast) {
        if (N.isEmpty(c)) {
            return OptionalInt.empty();
        }

        final OptionalInt res = N.findFirstIndex(c, predicateForFirst);

        return res.isPresent() ? res : N.findLastIndex(c, predicateForLast);
    }

    /**
     * Find first and last.
     *
     * @param <T>
     * @param a
     * @param predicate
     * @return the pair
     */
    public static <T> Pair<Nullable<T>, Nullable<T>> findFirstAndLast(final T[] a, final Predicate<? super T> predicate) {
        return findFirstAndLast(a, predicate, predicate);
    }

    /**
     * Find first and last.
     *
     * @param <T>
     * @param a
     * @param predicateForFirst
     * @param predicateForLast
     * @return the pair
     */
    public static <T> Pair<Nullable<T>, Nullable<T>> findFirstAndLast(final T[] a, final Predicate<? super T> predicateForFirst,
            final Predicate<? super T> predicateForLast) {
        if (N.isEmpty(a)) {
            return Pair.of(Nullable.<T> empty(), Nullable.<T> empty());
        }

        return Pair.of(N.findFirst(a, predicateForFirst), N.findLast(a, predicateForLast));
    }

    /**
     * Find first and last.
     *
     * @param <T>
     * @param c
     * @param predicate
     * @return the pair
     */
    public static <T> Pair<Nullable<T>, Nullable<T>> findFirstAndLast(final Collection<? extends T> c, final Predicate<? super T> predicate) {
        return findFirstAndLast(c, predicate, predicate);
    }

    /**
     * Find first and last.
     *
     * @param <T>
     * @param c
     * @param predicateForFirst
     * @param predicateForLast
     * @return the pair
     */
    public static <T> Pair<Nullable<T>, Nullable<T>> findFirstAndLast(final Collection<? extends T> c, final Predicate<? super T> predicateForFirst,
            final Predicate<? super T> predicateForLast) {
        if (N.isEmpty(c)) {
            return Pair.of(Nullable.<T> empty(), Nullable.<T> empty());
        }

        return Pair.of(N.findFirst(c, predicateForFirst), N.findLast(c, predicateForLast));
    }

    /**
     * Find first and last index.
     *
     * @param <T>
     * @param a
     * @param predicate
     * @return the pair
     */
    public static <T> Pair<OptionalInt, OptionalInt> findFirstAndLastIndex(final T[] a, final Predicate<? super T> predicate) {
        return findFirstAndLastIndex(a, predicate, predicate);
    }

    /**
     * Find first and last index.
     *
     * @param <T>
     * @param a
     * @param predicateForFirst
     * @param predicateForLast
     * @return the pair
     */
    public static <T> Pair<OptionalInt, OptionalInt> findFirstAndLastIndex(final T[] a, final Predicate<? super T> predicateForFirst,
            final Predicate<? super T> predicateForLast) {
        if (N.isEmpty(a)) {
            return Pair.of(OptionalInt.empty(), OptionalInt.empty());
        }

        return Pair.of(N.findFirstIndex(a, predicateForFirst), N.findLastIndex(a, predicateForLast));
    }

    /**
     * Find first and last index.
     *
     * @param <T>
     * @param c
     * @param predicate
     * @return the pair
     */
    public static <T> Pair<OptionalInt, OptionalInt> findFirstAndLastIndex(final Collection<? extends T> c, final Predicate<? super T> predicate) {
        return findFirstAndLastIndex(c, predicate, predicate);
    }

    /**
     * Find first and last index.
     *
     * @param <T>
     * @param c
     * @param predicateForFirst
     * @param predicateForLast
     * @return the pair
     */
    public static <T> Pair<OptionalInt, OptionalInt> findFirstAndLastIndex(final Collection<? extends T> c, final Predicate<? super T> predicateForFirst,
            final Predicate<? super T> predicateForLast) {
        if (N.isEmpty(c)) {
            return Pair.of(OptionalInt.empty(), OptionalInt.empty());
        }

        return Pair.of(N.findFirstIndex(c, predicateForFirst), N.findLastIndex(c, predicateForLast));
    }

    public abstract static class SetView<E> extends ImmutableSet<E> {
        SetView(final Set<? extends E> set) {
            super(set);
        }

        /**
         *
         *
         * @param <S>
         * @param set
         * @return
         */
        public <S extends Set<? super E>> S copyInto(final S set) {
            set.addAll(this);
            return set;
        }
    }

    /**
     * Returns an unmodifiable <b>view</b> of the union of two sets. The returned set contains all
     * elements that are contained in either backing set. Iterating over the returned set iterates
     * first over all the elements of {@code set1}, then over each element of {@code set2}, in order,
     * that is not contained in {@code set1}.
     *
     * <p>Results are undefined if {@code set1} and {@code set2} are sets based on different
     * equivalence relations (as {@link HashSet}, {@link TreeSet}, and the {@link Map#keySet} of an
     * {@code IdentityHashMap} all are).
     *
     * @param <E>
     * @param set1
     * @param set2
     * @return
     * @throws IllegalArgumentException
     */
    public static <E> SetView<E> union(final Set<? extends E> set1, final Set<? extends E> set2) throws IllegalArgumentException {
        // N.checkArgNotNull(set1, "set1");
        // N.checkArgNotNull(set2, "set2");

        Set<? extends E> tmp = null;

        if (N.isEmpty(set1)) {
            tmp = N.isEmpty(set2) ? N.<E> emptySet() : set2;
        } else if (N.isEmpty(set2)) {
            tmp = set1;
        } else {
            tmp = new AbstractSet<>() {
                @Override
                public ObjIterator<E> iterator() {
                    return new ObjIterator<>() {
                        private final Iterator<? extends E> iter1 = set1.iterator();
                        private final Iterator<? extends E> iter2 = set2.iterator();
                        private final E NONE = (E) N.NULL_MASK; //NOSONAR
                        private E next = NONE;
                        private E tmp = null;

                        @Override
                        public boolean hasNext() {
                            if (iter1.hasNext() || next != NONE) {
                                return true;
                            }

                            while (iter2.hasNext()) {
                                next = iter2.next();

                                if (!set1.contains(next)) {
                                    return true;
                                }
                            }

                            next = NONE;

                            return false;
                        }

                        @Override
                        public E next() {
                            if (!hasNext()) {
                                throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                            }

                            if (iter1.hasNext()) {
                                return iter1.next();
                            } else {
                                tmp = next;
                                next = NONE;
                                return tmp;
                            }
                        }
                    };
                }

                @Override
                public boolean contains(final Object object) {
                    return set1.contains(object) || set2.contains(object);
                }

                @Override
                public int size() {
                    int size = set1.size();

                    for (final E e : set2) {
                        if (!set1.contains(e)) {
                            size++;
                        }
                    }

                    return size;
                }

                @Override
                public boolean isEmpty() {
                    return set1.isEmpty() && set2.isEmpty();
                }
            };
        }

        return new SetView<>(tmp) {
            @Override
            public <S extends Set<? super E>> S copyInto(final S set) {
                set.addAll(set1);
                set.addAll(set2);
                return set;
            }
        };
    }

    /**
     * Returns an unmodifiable <b>view</b> of the intersection of two sets. The returned set contains
     * all elements that are contained by both backing sets. The iteration order of the returned set
     * matches that of {@code set1}.
     *
     * <p>Results are undefined if {@code set1} and {@code set2} are sets based on different
     * equivalence relations (as {@code HashSet}, {@code TreeSet}, and the keySet of an {@code
     * IdentityHashMap} all are).
     *
     * <p><b>Note:</b> The returned view performs slightly better when {@code set1} is the smaller of
     * the two sets. If you have reason to believe one of your sets will generally be smaller than the
     * other, pass it first. Unfortunately, since this method sets the generic type of the returned
     * set based on the type of the first set passed, this could in rare cases force you to make a
     * cast, for example:
     *
     * <pre>{@code
     * Set<Object> aFewBadObjects = ...
     * Set<String> manyBadStrings = ...
     *
     * // impossible for a non-String to be in the intersection
     * SuppressWarnings("unchecked")
     * Set<String> badStrings = (Set) Sets.intersection(
     *     aFewBadObjects, manyBadStrings);
     * }</pre>
     *
     * <p>This is unfortunate, but should come up only very rarely.
     *
     * @param <E>
     * @param set1
     * @param set2
     * @return
     * @throws IllegalArgumentException
     * @see N#intersection(int[], int[])
     * @see N#intersection(Collection, Collection)
     * @see N#commonSet(Collection, Collection)
     * @see Collection#retainAll(Collection)
     */
    public static <E> SetView<E> intersection(final Set<E> set1, final Set<?> set2) throws IllegalArgumentException {
        // N.checkArgNotNull(set1, "set1");
        // N.checkArgNotNull(set2, "set2");

        Set<E> tmp = null;

        if (N.isEmpty(set1) || N.isEmpty(set2)) {
            tmp = N.<E> emptySet();
        } else {
            tmp = new AbstractSet<>() {
                @Override
                public ObjIterator<E> iterator() {
                    return new ObjIterator<>() {
                        private final Iterator<E> iter1 = set1.iterator();
                        private final E NONE = (E) N.NULL_MASK; //NOSONAR
                        private E next = NONE;
                        private E tmp = null;

                        @Override
                        public boolean hasNext() {
                            if (next != NONE) {
                                return true;
                            }

                            while (iter1.hasNext()) {
                                next = iter1.next();

                                if (set2.contains(next)) {
                                    return true;
                                }
                            }

                            next = NONE;

                            return false;
                        }

                        @Override
                        public E next() {
                            if (!hasNext()) {
                                throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                            }

                            tmp = next;
                            next = NONE;
                            return tmp;
                        }
                    };
                }

                @Override
                public boolean contains(final Object object) {
                    return set1.contains(object) && set2.contains(object);
                }

                @Override
                public boolean containsAll(final Collection<?> collection) {
                    return set1.containsAll(collection) && set2.containsAll(collection);
                }

                @Override
                public int size() {
                    int size = 0;

                    for (final E e : set1) {
                        if (set2.contains(e)) {
                            size++;
                        }
                    }

                    return size;
                }

                @Override
                public boolean isEmpty() {
                    return Collections.disjoint(set1, set2);
                }
            };
        }

        return new SetView<>(tmp) {
        };
    }

    /**
     * Returns an unmodifiable <b>view</b> of the difference of two sets. The returned set contains
     * all elements that are contained by {@code set1} and not contained by {@code set2}. {@code set2}
     * may also contain elements not present in {@code set1}; these are simply ignored. The iteration
     * order of the returned set matches that of {@code set1}.
     *
     * <p>Results are undefined if {@code set1} and {@code set2} are sets based on different
     * equivalence relations (as {@code HashSet}, {@code TreeSet}, and the keySet of an {@code
     * IdentityHashMap} all are).
     *
     * @param <E>
     * @param set1
     * @param set2
     * @return
     * @throws IllegalArgumentException
     * @see N#difference(Collection, Collection)
     * @see N#symmetricDifference(Collection, Collection)
     * @see N#excludeAll(Collection, Collection)
     * @see N#excludeAllToSet(Collection, Collection)
     * @see N#removeAll(Collection, Iterable)
     * @see N#intersection(Collection, Collection)
     * @see N#commonSet(Collection, Collection)
     * @see Difference#of(Collection, Collection)
     */
    public static <E> SetView<E> difference(final Set<E> set1, final Set<?> set2) throws IllegalArgumentException {
        // N.checkArgNotNull(set1, "set1");
        // N.checkArgNotNull(set2, "set2");

        Set<E> tmp = null;

        if (N.isEmpty(set1)) {
            tmp = N.<E> emptySet();
        } else if (N.isEmpty(set2)) {
            tmp = set1;
        } else {
            tmp = new AbstractSet<>() {
                @Override
                public ObjIterator<E> iterator() {
                    return new ObjIterator<>() {
                        private final Iterator<E> iter1 = set1.iterator();
                        private final E NONE = (E) N.NULL_MASK; //NOSONAR
                        private E next = NONE;
                        private E tmp = null;

                        @Override
                        public boolean hasNext() {
                            if (next != NONE) {
                                return true;
                            }

                            while (iter1.hasNext()) {
                                next = iter1.next();

                                if (!set2.contains(next)) {
                                    return true;
                                }
                            }

                            next = NONE;

                            return false;
                        }

                        @Override
                        public E next() {
                            if (!hasNext()) {
                                throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                            }

                            tmp = next;
                            next = NONE;
                            return tmp;
                        }
                    };
                }

                @Override
                public boolean contains(final Object object) {
                    return set1.contains(object) && !set2.contains(object);
                }

                @Override
                public int size() {
                    int size = 0;

                    for (final E e : set1) {
                        if (!set2.contains(e)) {
                            size++;
                        }
                    }

                    return size;
                }

                @Override
                public boolean isEmpty() {
                    return set2.containsAll(set1);
                }
            };
        }

        return new SetView<>(tmp) {
        };
    }

    /**
     * Returns an unmodifiable <b>view</b> of the symmetric difference of two sets. The returned set
     * contains all elements that are contained in either {@code set1} or {@code set2} but not in
     * both. The iteration order of the returned set is undefined.
     *
     * <p>Results are undefined if {@code set1} and {@code set2} are sets based on different
     * equivalence relations (as {@code HashSet}, {@code TreeSet}, and the keySet of an {@code
     * IdentityHashMap} all are).
     *
     * @param <E>
     * @param set1
     * @param set2
     * @return
     * @throws IllegalArgumentException
     * @since 3.0
     */
    public static <E> SetView<E> symmetricDifference(final Set<? extends E> set1, final Set<? extends E> set2) throws IllegalArgumentException {
        // N.checkArgNotNull(set1, "set1");
        // N.checkArgNotNull(set2, "set2");

        Set<? extends E> tmp = null;

        if (N.isEmpty(set1)) {
            tmp = N.isEmpty(set2) ? N.<E> emptySet() : set2;
        } else if (N.isEmpty(set2)) {
            tmp = set1;
        } else {
            tmp = new AbstractSet<>() {
                @Override
                public ObjIterator<E> iterator() {
                    return new ObjIterator<>() {
                        private final Iterator<? extends E> iter1 = set1.iterator();
                        private final Iterator<? extends E> iter2 = set2.iterator();
                        private final E NONE = (E) N.NULL_MASK; //NOSONAR
                        private E next = NONE;
                        private E tmp = null;

                        @Override
                        public boolean hasNext() {
                            if (next != NONE) {
                                return true;
                            }

                            while (iter1.hasNext()) {
                                next = iter1.next();

                                if (!set2.contains(next)) {
                                    return true;
                                }
                            }

                            while (iter2.hasNext()) {
                                next = iter2.next();

                                if (!set1.contains(next)) {
                                    return true;
                                }
                            }

                            next = NONE;

                            return false;
                        }

                        @Override
                        public E next() {
                            if (!hasNext()) {
                                throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                            }

                            tmp = next;
                            next = NONE;
                            return tmp;
                        }
                    };
                }

                @Override
                public boolean contains(final Object object) {
                    return set1.contains(object) ^ set2.contains(object);
                }

                @Override
                public int size() {
                    int size = 0;

                    for (final E e : set1) {
                        if (!set2.contains(e)) {
                            size++;
                        }
                    }

                    for (final E e : set2) {
                        if (!set1.contains(e)) {
                            size++;
                        }
                    }

                    return size;
                }

                @Override
                public boolean isEmpty() {
                    return set1.equals(set2);
                }
            };
        }

        return new SetView<>(tmp) {
        };
    }

    /**
     *
     *
     * @param <K>
     * @param set
     * @param range
     * @return
     * @throws IllegalArgumentException
     */
    public static <K extends Comparable<? super K>> NavigableSet<K> subSet(final NavigableSet<K> set, final Range<K> range) throws IllegalArgumentException {
        if (N.isEmpty(set)) {
            return N.emptyNavigableSet();
        }

        if (set.comparator() != null && set.comparator() != N.NATURAL_COMPARATOR) { // NOSONAR
            N.checkArgument(set.comparator().compare(range.lowerEndpoint(), range.upperEndpoint()) <= 0,
                    "set is using a custom comparator which is inconsistent with the natural ordering.");
        }

        return set.subSet(range.lowerEndpoint(), range.boundType() == BoundType.CLOSED_OPEN || range.boundType() == BoundType.CLOSED_CLOSED,
                range.upperEndpoint(), range.boundType() == BoundType.OPEN_CLOSED || range.boundType() == BoundType.CLOSED_CLOSED);
    }

    /**
     *
     * Note: copy from Google Guava under Apache License v2.
     * <br />
     *
     * Returns the set of all possible subsets of {@code set}. For example,
     * {@code powerSet(ImmutableSet.of(1, 2))} returns the set {@code {{},
     * {1}, {2}, {1, 2}}}.
     *
     * <p>Elements appear in these subsets in the same iteration order as they
     * appeared in the input set. The order in which these subsets appear in the
     * outer set is undefined. Note that the power set of the empty set is not the
     * empty set, but a one-element set containing the empty set.
     *
     * <p>The returned set and its constituent sets use {@code equals} to decide
     * whether two elements are identical, even if the input set uses a different
     * concept of equivalence.
     *
     * <p><i>Performance notes:</i> while the power set of a set with size {@code
     * n} is of size {@code 2^n}, its memory usage is only {@code O(n)}. When the
     * power set is constructed, the input set is merely copied. Only as the
     * power set is iterated are the individual subsets created, and these subsets
     * themselves occupy only a small constant amount of memory.
     *
     * @param <E>
     * @param set the set of elements to construct a power set from
     * @return the sets the
     * @throws IllegalArgumentException if {@code set} has more than 30 unique
     *     elements (causing the power set size to exceed the {@code int} range)
     * @throws NullPointerException if {@code set} is or contains {@code null}
     * @see <a href="http://en.wikipedia.org/wiki/Power_set">Power set article at
     *      Wikipedia</a>
     */
    public static <E> Set<Set<E>> powerSet(final Set<E> set) {
        return new PowerSet<>(N.nullToEmpty(set));
    }

    /**
     *
     * @param <T>
     * @param c
     * @return the list
     */
    public static <T> List<List<T>> rollup(final Collection<? extends T> c) {
        final List<List<T>> res = new ArrayList<>();
        res.add(new ArrayList<>());

        if (N.notEmpty(c)) {
            for (final T e : c) {
                final List<T> prev = res.get(res.size() - 1);
                final List<T> cur = new ArrayList<>(prev.size() + 1);
                cur.addAll(prev);
                cur.add(e);
                res.add(cur);
            }
        }

        return res;
    }

    /**
     * Note: copy from Google Guava under Apache License v2.
     * <br />
     *
     * Returns a {@link Collection} of all the permutations of the specified
     * {@link Collection}.
     *
     * <p><i>Notes:</i> This is an implementation of the Plain Changes algorithm
     * for permutations generation, described in Knuth's "The Art of Computer
     * Programming", Volume 4, Chapter 7, Section 7.2.1.2.
     *
     * <p>If the input list contains equal elements, some of the generated
     * permutations will be equal.
     *
     * <p>An empty collection has only one permutation, which is an empty list.
     *
     * @param <E>
     * @param elements the original collection whose elements have to be permuted.
     * @return an immutable {@link Collection} containing all the different
     *     permutations of the original collection.
     * @throws NullPointerException if the specified collection is null or has any
     *     null elements.
     */
    public static <E> Collection<List<E>> permutations(final Collection<E> elements) {
        return new PermutationCollection<>(N.nullToEmpty(elements));
    }

    /**
     * Note: copy from Google Guava under Apache License v2.
     * <br />
     *
     * Returns a {@link Collection} of all the permutations of the specified
     * {@link Iterable}.
     *
     * <p><i>Notes:</i> This is an implementation of the algorithm for
     * Lexicographical Permutations Generation, described in Knuth's "The Art of
     * Computer Programming", Volume 4, Chapter 7, Section 7.2.1.2. The
     * iteration order follows the lexicographical order. This means that
     * the first permutation will be in ascending order, and the last will be in
     * descending order.
     *
     * <p>Duplicate elements are considered equal. For example, the list [1, 1]
     * will have only one permutation, instead of two. This is why the elements
     * have to implement {@link Comparable}.
     *
     * <p>An empty iterable has only one permutation, which is an empty list.
     *
     * <p>This method is equivalent to
     * {@code Collections2.orderedPermutations(list, Ordering.natural())}.
     *
     * @param <E>
     * @param elements the original iterable whose elements have to be permuted.
     * @return an immutable {@link Collection} containing all the different
     *     permutations of the original iterable.
     * @throws NullPointerException if the specified iterable is null or has any
     *     null elements.
     */
    public static <E extends Comparable<? super E>> Collection<List<E>> orderedPermutations(final Collection<E> elements) {
        return orderedPermutations(N.nullToEmpty(elements), N.NATURAL_COMPARATOR);
    }

    /**
     * Note: copy from Google Guava under Apache License v2.
     * <br />
     *
     * Returns a {@link Collection} of all the permutations of the specified
     * {@link Iterable} using the specified {@link Comparator} for establishing
     * the lexicographical ordering.
     *
     * <p>Examples: <pre>   {@code
     *
     *   for (List<String> perm : orderedPermutations(asList("b", "c", "a"))) {
     *     println(perm);
     *   }
     *   // -> ["a", "b", "c"]
     *   // -> ["a", "c", "b"]
     *   // -> ["b", "a", "c"]
     *   // -> ["b", "c", "a"]
     *   // -> ["c", "a", "b"]
     *   // -> ["c", "b", "a"]
     *
     *   for (List<Integer> perm : orderedPermutations(asList(1, 2, 2, 1))) {
     *     println(perm);
     *   }
     *   // -> [1, 1, 2, 2]
     *   // -> [1, 2, 1, 2]
     *   // -> [1, 2, 2, 1]
     *   // -> [2, 1, 1, 2]
     *   // -> [2, 1, 2, 1]
     *   // -> [2, 2, 1, 1]}</pre>
     *
     * <p><i>Notes:</i> This is an implementation of the algorithm for
     * Lexicographical Permutations Generation, described in Knuth's "The Art of
     * Computer Programming", Volume 4, Chapter 7, Section 7.2.1.2. The
     * iteration order follows the lexicographical order. This means that
     * the first permutation will be in ascending order, and the last will be in
     * descending order.
     *
     * <p>Elements that compare equal are considered equal and no new permutations
     * are created by swapping them.
     *
     * <p>An empty iterable has only one permutation, which is an empty list.
     *
     * @param <E>
     * @param elements the original iterable whose elements have to be permuted.
     * @param comparator a comparator for the iterable's elements.
     * @return an immutable {@link Collection} containing all the different
     *     permutations of the original iterable.
     * @throws NullPointerException If the specified iterable is null, has any
     *     null elements, or if the specified comparator is null.
     */
    public static <E> Collection<List<E>> orderedPermutations(final Collection<E> elements, final Comparator<? super E> comparator) {
        return new OrderedPermutationCollection<>(N.nullToEmpty(elements), comparator);
    }

    /**
     * Note: copy from Google Guava under Apache License v2.
     * <br />
     *
     * Returns every possible list that can be formed by choosing one element
     * from each of the given lists in order; the "n-ary
     * <a href="http://en.wikipedia.org/wiki/Cartesian_product">Cartesian
     * product</a>" of the lists. For example: <pre>   {@code
     *
     *   Lists.cartesianProduct(ImmutableList.of(
     *       ImmutableList.of(1, 2),
     *       ImmutableList.of("A", "B", "C")))}</pre>
     *
     * <p>returns a list containing six lists in the following order:
     *
     * <ul>
     * <li>{@code ImmutableList.of(1, "A")}
     * <li>{@code ImmutableList.of(1, "B")}
     * <li>{@code ImmutableList.of(1, "C")}
     * <li>{@code ImmutableList.of(2, "A")}
     * <li>{@code ImmutableList.of(2, "B")}
     * <li>{@code ImmutableList.of(2, "C")}
     * </ul>
     *
     * <p>The result is guaranteed to be in the "traditional", lexicographical
     * order for Cartesian products that you would get from nesting for loops:
     * <pre>   {@code
     *
     *   for (B b0 : lists.get(0)) {
     *     for (B b1 : lists.get(1)) {
     *       ...
     *       ImmutableList<B> tuple = ImmutableList.of(b0, b1, ...);
     *       // operate on tuple
     *     }
     *   }}</pre>
     *
     * <p>Note that if any input list is empty, the Cartesian product will also be
     * empty. If no lists at all are provided (an empty list), the resulting
     * Cartesian product has one element, an empty list (counter-intuitive, but
     * mathematically consistent).
     *
     * <p><i>Performance notes:</i> while the cartesian product of lists of size
     * {@code m, n, p} is a list of size {@code m x n x p}, its actual memory
     * consumption is much smaller. When the cartesian product is constructed, the
     * input lists are merely copied. Only as the resulting list is iterated are
     * the individual lists created, and these are not retained after iteration.
     *
     * @param <E> any common base class shared by all axes (often just {@link
     *     Object})
     * @param cs the lists to choose elements from, in the order that
     *     the elements chosen from those lists should appear in the resulting
     *     lists
     * @return
     *     lists
     * @throws IllegalArgumentException if the size of the cartesian product would
     *     be greater than {@link Integer#MAX_VALUE}
     * @throws NullPointerException if {@code lists}, any one of the
     *     {@code lists}, or any element of a provided list is null
     */
    @SafeVarargs
    public static <E> List<List<E>> cartesianProduct(final Collection<? extends E>... cs) {
        return cartesianProduct(Array.asList(cs));
    }

    /**
     * Note: copy from Google Guava under Apache License v2.
     * <br />
     *
     * Returns every possible list that can be formed by choosing one element
     * from each of the given lists in order; the "n-ary
     * <a href="http://en.wikipedia.org/wiki/Cartesian_product">Cartesian
     * product</a>" of the lists. For example: <pre>   {@code
     *
     *   Lists.cartesianProduct(ImmutableList.of(
     *       ImmutableList.of(1, 2),
     *       ImmutableList.of("A", "B", "C")))}</pre>
     *
     * <p>returns a list containing six lists in the following order:
     *
     * <ul>
     * <li>{@code ImmutableList.of(1, "A")}
     * <li>{@code ImmutableList.of(1, "B")}
     * <li>{@code ImmutableList.of(1, "C")}
     * <li>{@code ImmutableList.of(2, "A")}
     * <li>{@code ImmutableList.of(2, "B")}
     * <li>{@code ImmutableList.of(2, "C")}
     * </ul>
     *
     * <p>The result is guaranteed to be in the "traditional", lexicographical
     * order for Cartesian products that you would get from nesting for loops:
     * <pre>   {@code
     *
     *   for (B b0 : lists.get(0)) {
     *     for (B b1 : lists.get(1)) {
     *       ...
     *       ImmutableList<B> tuple = ImmutableList.of(b0, b1, ...);
     *       // operate on tuple
     *     }
     *   }}</pre>
     *
     * <p>Note that if any input list is empty, the Cartesian product will also be
     * empty. If no lists at all are provided (an empty list), the resulting
     * Cartesian product has one element, an empty list (counter-intuitive, but
     * mathematically consistent).
     *
     * <p><i>Performance notes:</i> while the cartesian product of lists of size
     * {@code m, n, p} is a list of size {@code m x n x p}, its actual memory
     * consumption is much smaller. When the cartesian product is constructed, the
     * input lists are merely copied. Only as the resulting list is iterated are
     * the individual lists created, and these are not retained after iteration.
     *
     * @param <E> any common base class shared by all axes (often just {@link
     *     Object})
     * @param cs the lists to choose elements from, in the order that
     *     the elements chosen from those lists should appear in the resulting
     *     lists
     * @return
     *     lists
     * @throws IllegalArgumentException if the size of the cartesian product would
     *     be greater than {@link Integer#MAX_VALUE}
     * @throws NullPointerException if {@code lists}, any one of the {@code lists},
     *     or any element of a provided list is null
     */
    public static <E> List<List<E>> cartesianProduct(final Collection<? extends Collection<? extends E>> cs) {
        return new CartesianList<>(cs);
    }

    /**
     * Returns {@code true} if the second list is a permutation of the first.
     *
     * @param a
     * @param b
     * @return true, if is permutations
     */
    private static boolean isPermutations(final Collection<?> a, final Collection<?> b) {
        if (a.size() != b.size()) {
            return false;
        }

        return N.difference(a, b).size() == 0;
    }

    /**
     * The Class PowerSet.
     *
     * @param <E>
     */
    private static final class PowerSet<E> extends AbstractSet<Set<E>> {

        /** The input set. */
        final ImmutableMap<E, Integer> inputSet;

        /**
         * Instantiates a new power set.
         *
         * @param input
         */
        PowerSet(final Set<E> input) {
            inputSet = indexMap(input);
            N.checkArgument(inputSet.size() <= 30, "Too many elements to create power set: %s > 30", inputSet.size());
        }

        /**
         *
         * @return the int
         */
        @Override
        public int size() {
            return 1 << inputSet.size();
        }

        /**
         * Checks if is empty.
         *
         * @return true, if is empty
         */
        @Override
        public boolean isEmpty() {
            return false;
        }

        /**
         *
         * @return the iterator
         */
        @Override
        public Iterator<Set<E>> iterator() {
            return new Iterator<>() {
                private final int size = size();
                private int position;

                @Override
                public boolean hasNext() {
                    return position < size;
                }

                @Override
                public Set<E> next() {
                    if (!hasNext()) {
                        throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                    }

                    return new SubSet<>(inputSet, position++);
                }

                @Override
                public void remove() {
                    throw new UnsupportedOperationException();
                }
            };
        }

        /**
         *
         * @param obj
         * @return
         */
        @Override
        public boolean contains(final Object obj) {
            if (obj instanceof Set) {
                final Set<?> set = (Set<?>) obj;
                return inputSet.keySet().containsAll(set);
            }
            return false;
        }

        /**
         *
         * @param obj
         * @return
         */
        @Override
        public boolean equals(final Object obj) {
            if (obj instanceof PowerSet) {
                final PowerSet<?> that = (PowerSet<?>) obj;
                return inputSet.equals(that.inputSet);
            }
            return super.equals(obj);
        }

        /**
         *
         * @return the int
         */
        @Override
        public int hashCode() {
            /*
             * The sum of the sums of the hash codes in each subset is just the sum of
             * each input element's hash code times the number of sets that element
             * appears in. Each element appears in exactly half of the 2^n sets, so:
             */
            return inputSet.keySet().hashCode() << (inputSet.size() - 1);
        }

        /**
         *
         * @return the string
         */
        @Override
        public String toString() {
            return "powerSet(" + inputSet + ")";
        }

        /**
         * Returns a map from the ith element of list to i.
         *
         * @param <E>
         * @param c
         * @return the immutable map
         */
        private static <E> ImmutableMap<E, Integer> indexMap(final Collection<E> c) {
            final Map<E, Integer> map = new LinkedHashMap<>();

            int i = 0;

            for (final E e : c) {
                map.put(e, i++);
            }

            return ImmutableMap.wrap(map);
        }
    }

    /**
     * The Class SubSet.
     *
     * @param <E>
     */
    private static final class SubSet<E> extends AbstractSet<E> { //NOSONAR

        /** The input set. */
        private final ImmutableMap<E, Integer> inputSet;

        /** The elements. */
        private final ImmutableList<E> elements;

        /** The mask. */
        private final int mask;

        /**
         * Instantiates a new sub set.
         *
         * @param inputSet
         * @param mask
         */
        SubSet(final ImmutableMap<E, Integer> inputSet, final int mask) {
            this.inputSet = inputSet;
            elements = ImmutableList.copyOf(inputSet.keySet());
            this.mask = mask;
        }

        /**
         *
         * @return the iterator
         */
        @Override
        public Iterator<E> iterator() {
            return new Iterator<>() {
                int remainingSetBits = mask;

                @Override
                public boolean hasNext() {
                    return remainingSetBits != 0;
                }

                @Override
                public E next() {
                    final int index = Integer.numberOfTrailingZeros(remainingSetBits);
                    if (index == 32) {
                        throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                    }
                    remainingSetBits &= ~(1 << index);
                    return elements.get(index);
                }

                @Override
                public void remove() {
                    throw new UnsupportedOperationException();
                }
            };
        }

        /**
         *
         * @return the int
         */
        @Override
        public int size() {
            return Integer.bitCount(mask);
        }

        /**
         *
         * @param o
         * @return
         */
        @Override
        public boolean contains(final Object o) {
            final Integer index = inputSet.get(o);
            return index != null && (mask & (1 << index)) != 0;
        }
    }

    /**
     * The Class PermutationCollection.
     *
     * @param <E>
     */
    private static final class PermutationCollection<E> extends AbstractCollection<List<E>> {

        /** The input list. */
        final List<E> inputList;

        /**
         * Instantiates a new permutation collection.
         *
         * @param input
         */
        PermutationCollection(final Collection<E> input) {
            inputList = new ArrayList<>(input);
        }

        /**
         *
         * @return the int
         */
        @Override
        public int size() {
            return Numbers.factorial(inputList.size());
        }

        /**
         * Checks if is empty.
         *
         * @return true, if is empty
         */
        @Override
        public boolean isEmpty() {
            return false;
        }

        /**
         *
         * @return the iterator
         */
        @Override
        public Iterator<List<E>> iterator() {
            return PermutationIterator.of(inputList);
        }

        /**
         *
         * @param obj
         * @return
         */
        @Override
        public boolean contains(final Object obj) {
            if (obj instanceof Collection) {
                return isPermutations(inputList, (Collection<?>) obj);
            }

            return false;
        }

        /**
         *
         * @return the string
         */
        @Override
        public String toString() {
            return "permutations(" + inputList + ")";
        }
    }

    /**
     * The Class OrderedPermutationCollection.
     *
     * @param <E>
     */
    private static final class OrderedPermutationCollection<E> extends AbstractCollection<List<E>> {

        /** The input list. */
        final List<E> inputList;

        /** The comparator. */
        final Comparator<? super E> comparator;

        /** The size. */
        final int size;

        /**
         * Instantiates a new ordered permutation collection.
         *
         * @param input
         * @param comparator
         */
        OrderedPermutationCollection(final Collection<E> input, final Comparator<? super E> comparator) {
            inputList = new ArrayList<>(input);
            N.sort(inputList, comparator);
            this.comparator = comparator;
            size = calculateSize(inputList, comparator);
        }

        /**
         *
         * @return the int
         */
        @Override
        public int size() {
            return size;
        }

        /**
         * Checks if is empty.
         *
         * @return true, if is empty
         */
        @Override
        public boolean isEmpty() {
            return false;
        }

        /**
         *
         * @return the iterator
         */
        @Override
        public Iterator<List<E>> iterator() {
            return PermutationIterator.ordered(inputList, comparator);
        }

        /**
         *
         * @param obj
         * @return
         */
        @Override
        public boolean contains(final Object obj) {
            if (obj instanceof Collection) {
                return isPermutations(inputList, (Collection<?>) obj);
            }
            return false;
        }

        /**
         *
         * @return the string
         */
        @Override
        public String toString() {
            return "orderedPermutationCollection(" + inputList + ")";
        }

        /**
         * The number of permutations with repeated elements is calculated as
         * follows:
         * <ul>
         * <li>For an empty list, it is 1 (base case).</li>
         * <li>When r numbers are added to a list of n-r elements, the number of
         * permutations is increased by a factor of (n choose r).</li>
         * </ul>
         *
         * @param <E>
         * @param sortedInputList
         * @param comparator
         * @return the int
         */
        private static <E> int calculateSize(final List<E> sortedInputList, final Comparator<? super E> comparator) {
            long permutations = 1;
            int n = 1;
            int r = 1;
            while (n < sortedInputList.size()) {
                final int comparison = comparator.compare(sortedInputList.get(n - 1), sortedInputList.get(n));

                if (comparison < 0) {
                    // We move to the next non-repeated element.
                    permutations *= Numbers.binomial(n, r);
                    r = 0;
                    if (!isPositiveInt(permutations)) {
                        return Integer.MAX_VALUE;
                    }
                }

                n++;
                r++;
            }

            permutations *= Numbers.binomial(n, r);

            if (!isPositiveInt(permutations)) {
                return Integer.MAX_VALUE;
            }

            return (int) permutations;
        }

        /**
         * Checks if is positive int.
         *
         * @param n
         * @return true, if is positive int
         */
        private static boolean isPositiveInt(final long n) {
            return n >= 0 && n <= Integer.MAX_VALUE;
        }
    }

    /**
     * The Class CartesianList.
     *
     * @param <E>
     */
    private static final class CartesianList<E> extends AbstractList<List<E>> implements RandomAccess { //NOSONAR

        /** The axes. */
        private final transient Object[][] axes; //NOSONAR

        /** The axes size product. */
        private final transient int[] axesSizeProduct; //NOSONAR

        /**
         * Instantiates a new cartesian list.
         *
         * @param cs
         */
        CartesianList(final Collection<? extends Collection<? extends E>> cs) {
            final Iterator<? extends Collection<? extends E>> iter = cs.iterator();
            axes = new Object[cs.size()][];

            for (int i = 0, len = axes.length; i < len; i++) {
                axes[i] = iter.next().toArray();
            }

            axesSizeProduct = new int[axes.length + 1];
            axesSizeProduct[axes.length] = 1;

            try {
                for (int i = axes.length - 1; i >= 0; i--) {
                    axesSizeProduct[i] = Numbers.multiplyExact(axesSizeProduct[i + 1], axes[i].length);
                }
            } catch (final ArithmeticException e) {
                throw new IllegalArgumentException("Cartesian product too large; must have size at most Integer.MAX_VALUE");
            }
        }

        /**
         *
         * @param index
         * @return the list
         */
        @Override
        public List<E> get(final int index) throws IllegalArgumentException {
            N.checkArgument(index < size(), "Invalid index %s. It must be less than the size %s", index, size());

            final List<E> result = new ArrayList<>(axes.length);

            for (int k = 0, len = axes.length; k < len; k++) {
                result.add((E) axes[k][getAxisIndexForProductIndex(index, k)]);
            }

            return result;
        }

        /**
         *
         * @return the int
         */
        @Override
        public int size() {
            return axesSizeProduct[0];
        }

        /**
         *
         * @param obj
         * @return
         */
        @Override
        public boolean contains(final Object obj) {
            if (!(obj instanceof Collection)) {
                return false;
            }

            final Collection<?> c = (Collection<?>) obj;

            if (c.size() != axes.length) {
                return false;
            }

            int idx = 0;
            for (final Object e : c) {
                boolean found = false;

                for (final Object p : axes[idx++]) {
                    if (N.equals(e, p)) {
                        found = true;
                        break;
                    }
                }

                if (!found) {
                    return false;
                }
            }

            return true;
        }

        /**
         * Gets the axis index for product index.
         *
         * @param index
         * @param axis
         * @return the axis index for product index
         */
        private int getAxisIndexForProductIndex(final int index, final int axis) {
            return (index / axesSizeProduct[axis + 1]) % axes[axis].length;
        }
    }

    /**
     * The Class Slice.
     *
     * @param <T>
     */
    static final class Slice<T> extends ImmutableCollection<T> { //NOSONAR

        /** The from index. */
        private final int fromIndex;

        /** The to index. */
        private final int toIndex;

        /**
         * Instantiates a new sub collection.
         *
         * @param a
         * @param fromIndex
         * @param toIndex
         */
        Slice(final T[] a, final int fromIndex, final int toIndex) {
            this(Array.asList(a), fromIndex, toIndex);
        }

        /**
         * Instantiates a new sub collection.
         *
         * @param c
         * @param fromIndex
         * @param toIndex
         */
        Slice(final List<? extends T> c, final int fromIndex, final int toIndex) {
            super(fromIndex == 0 && toIndex == c.size() ? c : c.subList(fromIndex, toIndex));
            this.fromIndex = 0;
            this.toIndex = toIndex - fromIndex;
        }

        /**
         * Instantiates a new sub collection.
         *
         * @param c
         * @param fromIndex
         * @param toIndex
         */
        Slice(final Collection<? extends T> c, final int fromIndex, final int toIndex) {
            super(c instanceof List ? ((List<T>) c).subList(fromIndex, toIndex) : c);
            this.fromIndex = c instanceof List ? 0 : fromIndex;
            this.toIndex = c instanceof List ? toIndex - fromIndex : toIndex;
        }

        /**
         *
         * @param o
         * @return
         */
        @Override
        public boolean contains(final Object o) {
            for (final T element : this) {
                if (N.equals(element, o)) {
                    return true;
                }
            }

            return false;
        }

        /**
         *
         * @param c
         * @return
         */
        @Override
        public boolean containsAll(final Collection<?> c) {
            for (final Object e : c) {
                if (!contains(e)) {
                    return false;
                }
            }

            return true;
        }

        /**
         * Checks if is empty.
         *
         * @return true, if is empty
         */
        @Override
        public boolean isEmpty() {
            return size() == 0;
        }

        /**
         *
         * @return the int
         */
        @Override
        public int size() {
            return toIndex - fromIndex; //NOSONAR
        }

        /**
         *
         * @return the iterator
         */
        @Override
        public ObjIterator<T> iterator() {
            if (coll == null || fromIndex == toIndex) {
                return ObjIterator.<T> empty();
            }

            final Iterator<T> iter = coll.iterator();

            if (fromIndex == 0 && toIndex == coll.size()) {
                return ObjIterator.of(iter);
            } else if (fromIndex == 0) {
                return Iterators.limit(iter, toIndex - fromIndex); //NOSONAR
            } else if (toIndex == coll.size()) {
                return Iterators.skip(iter, fromIndex);
            } else {
                return Iterators.skipAndLimit(iter, fromIndex, toIndex - fromIndex); //NOSONAR
            }
        }

        /**
         *
         * @return the object[]
         */
        @Override
        public Object[] toArray() {
            final Iterator<T> iter = this.iterator();
            final Object[] a = new Object[size()];

            for (int i = 0, len = a.length; i < len; i++) {
                a[i] = iter.next();
            }

            return a;
        }

        /**
         *
         * @param <A>
         * @param a
         * @return the a[]
         */
        @Override
        public <A> A[] toArray(A[] a) {
            if (a.length < size()) {
                a = N.copyOf(a, size());
            }

            final Iterator<T> iter = this.iterator();

            for (int i = 0, len = a.length; i < len; i++) {
                a[i] = (A) iter.next();
            }

            return a;
        }
    }

    /**
     * Checks if the specified {@code arg} is {@code null} or empty, and throws {@code IllegalArgumentException} if it is.
     *
     * @param <T>
     * @param arg
     * @param argNameOrErrorMsg
     * @return
     * @throws IllegalArgumentException if the specified {@code arg} is {@code null} or empty.
     */
    static <T> Iterator<T> iterateNonEmpty(final Iterable<T> arg, final String argNameOrErrorMsg) {
        final Iterator<T> iter = arg == null ? ObjIterator.<T> empty() : arg.iterator();
        final boolean isNullOrEmpty = arg == null || (arg instanceof Collection ? ((Collection<T>) arg).size() == 0 : !iter.hasNext());

        if (isNullOrEmpty) {
            if (argNameOrErrorMsg.indexOf(' ') == N.INDEX_NOT_FOUND) {
                throw new IllegalArgumentException("'" + argNameOrErrorMsg + "' can not be null or empty");
            } else {
                throw new IllegalArgumentException(argNameOrErrorMsg);
            }
        }

        return iter;
    }
}

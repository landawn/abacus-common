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

import java.util.AbstractCollection;
import java.util.AbstractList;
import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Deque;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.RandomAccess;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import com.landawn.abacus.logging.Logger;
import com.landawn.abacus.logging.LoggerFactory;
import com.landawn.abacus.util.u.Holder;
import com.landawn.abacus.util.u.Nullable;
import com.landawn.abacus.util.u.Optional;
import com.landawn.abacus.util.u.OptionalByte;
import com.landawn.abacus.util.u.OptionalChar;
import com.landawn.abacus.util.u.OptionalDouble;
import com.landawn.abacus.util.u.OptionalFloat;
import com.landawn.abacus.util.u.OptionalInt;
import com.landawn.abacus.util.u.OptionalLong;
import com.landawn.abacus.util.u.OptionalShort;
import com.landawn.abacus.util.function.Function;
import com.landawn.abacus.util.stream.Stream;

/**
 * The methods in this class should only read the input {@code Collections/Arrays}, not modify any of them.
 *
 * @author Haiyang Li
 * @since 1.2.7
 */
public class Iterables {

    private static final Logger logger = LoggerFactory.getLogger(Iterables.class);

    Iterables() {
        // singleton.
    }

    @SafeVarargs
    public static OptionalChar min(final char... a) {
        return a == null || a.length == 0 ? OptionalChar.empty() : OptionalChar.of(N.min(a));
    }

    @SafeVarargs
    public static OptionalByte min(final byte... a) {
        return a == null || a.length == 0 ? OptionalByte.empty() : OptionalByte.of(N.min(a));
    }

    @SafeVarargs
    public static OptionalShort min(final short... a) {
        return a == null || a.length == 0 ? OptionalShort.empty() : OptionalShort.of(N.min(a));
    }

    @SafeVarargs
    public static OptionalInt min(final int... a) {
        return a == null || a.length == 0 ? OptionalInt.empty() : OptionalInt.of(N.min(a));
    }

    @SafeVarargs
    public static OptionalLong min(final long... a) {
        return a == null || a.length == 0 ? OptionalLong.empty() : OptionalLong.of(N.min(a));
    }

    @SafeVarargs
    public static OptionalFloat min(final float... a) {
        return a == null || a.length == 0 ? OptionalFloat.empty() : OptionalFloat.of(N.min(a));
    }

    @SafeVarargs
    public static OptionalDouble min(final double... a) {
        return a == null || a.length == 0 ? OptionalDouble.empty() : OptionalDouble.of(N.min(a));
    }

    @SafeVarargs
    public static OptionalChar max(final char... a) {
        return a == null || a.length == 0 ? OptionalChar.empty() : OptionalChar.of(N.max(a));
    }

    @SafeVarargs
    public static OptionalByte max(final byte... a) {
        return a == null || a.length == 0 ? OptionalByte.empty() : OptionalByte.of(N.max(a));
    }

    @SafeVarargs
    public static OptionalShort max(final short... a) {
        return a == null || a.length == 0 ? OptionalShort.empty() : OptionalShort.of(N.max(a));
    }

    @SafeVarargs
    public static OptionalInt max(final int... a) {
        return a == null || a.length == 0 ? OptionalInt.empty() : OptionalInt.of(N.max(a));
    }

    @SafeVarargs
    public static OptionalLong max(final long... a) {
        return a == null || a.length == 0 ? OptionalLong.empty() : OptionalLong.of(N.max(a));
    }

    @SafeVarargs
    public static OptionalFloat max(final float... a) {
        return a == null || a.length == 0 ? OptionalFloat.empty() : OptionalFloat.of(N.max(a));
    }

    @SafeVarargs
    public static OptionalDouble max(final double... a) {
        return a == null || a.length == 0 ? OptionalDouble.empty() : OptionalDouble.of(N.max(a));
    }

    /**
     * Min.
     *
     * @param <T> the generic type
     * @param c the c
     * @return the nullable
     */
    public static <T extends Comparable<? super T>> Nullable<T> min(final Collection<? extends T> c) {
        return N.isNullOrEmpty(c) ? Nullable.<T> empty() : Nullable.of(N.min(c));
    }

    /**
     * Min.
     *
     * @param <T> the generic type
     * @param a the a
     * @return the nullable
     */
    public static <T extends Comparable<? super T>> Nullable<T> min(final T[] a) {
        return N.isNullOrEmpty(a) ? Nullable.<T> empty() : Nullable.of(N.min(a));
    }

    /**
     * Min.
     *
     * @param <T> the generic type
     * @param c the c
     * @param cmp the cmp
     * @return the nullable
     */
    public static <T> Nullable<T> min(final Collection<? extends T> c, final Comparator<? super T> cmp) {
        return N.isNullOrEmpty(c) ? Nullable.<T> empty() : Nullable.of(N.min(c, cmp));
    }

    /**
     * Min.
     *
     * @param <T> the generic type
     * @param a the a
     * @param cmp the cmp
     * @return the nullable
     */
    public static <T> Nullable<T> min(final T[] a, final Comparator<? super T> cmp) {
        return N.isNullOrEmpty(a) ? Nullable.<T> empty() : Nullable.of(N.min(a, cmp));
    }

    /**
     * Min by.
     *
     * @param <T> the generic type
     * @param c the c
     * @param keyMapper the key mapper
     * @return the nullable
     */
    @SuppressWarnings("rawtypes")
    public static <T> Nullable<T> minBy(final Collection<? extends T> c, final Function<? super T, ? extends Comparable> keyMapper) {
        return min(c, Fn.comparingBy(keyMapper));
    }

    /**
     * Min by.
     *
     * @param <T> the generic type
     * @param a the a
     * @param keyMapper the key mapper
     * @return the nullable
     */
    @SuppressWarnings("rawtypes")
    public static <T> Nullable<T> minBy(final T[] a, final Function<? super T, ? extends Comparable> keyMapper) {
        return min(a, Fn.comparingBy(keyMapper));
    }

    /**
     * Max.
     *
     * @param <T> the generic type
     * @param c the c
     * @return the nullable
     */
    public static <T extends Comparable<? super T>> Nullable<T> max(final Collection<? extends T> c) {
        return N.isNullOrEmpty(c) ? Nullable.<T> empty() : Nullable.of(N.max(c));
    }

    /**
     * Max.
     *
     * @param <T> the generic type
     * @param a the a
     * @return the nullable
     */
    public static <T extends Comparable<? super T>> Nullable<T> max(final T[] a) {
        return N.isNullOrEmpty(a) ? Nullable.<T> empty() : Nullable.of(N.max(a));
    }

    /**
     * Max.
     *
     * @param <T> the generic type
     * @param c the c
     * @param cmp the cmp
     * @return the nullable
     */
    public static <T> Nullable<T> max(final Collection<? extends T> c, final Comparator<? super T> cmp) {
        return N.isNullOrEmpty(c) ? Nullable.<T> empty() : Nullable.of(N.max(c, cmp));
    }

    /**
     * Max.
     *
     * @param <T> the generic type
     * @param a the a
     * @param cmp the cmp
     * @return the nullable
     */
    public static <T> Nullable<T> max(final T[] a, final Comparator<? super T> cmp) {
        return N.isNullOrEmpty(a) ? Nullable.<T> empty() : Nullable.of(N.max(a, cmp));
    }

    /**
     * Max by.
     *
     * @param <T> the generic type
     * @param c the c
     * @param keyMapper the key mapper
     * @return the nullable
     */
    @SuppressWarnings("rawtypes")
    public static <T> Nullable<T> maxBy(final Collection<? extends T> c, final Function<? super T, ? extends Comparable> keyMapper) {
        return max(c, Fn.comparingBy(keyMapper));
    }

    /**
     * Max by.
     *
     * @param <T> the generic type
     * @param a the a
     * @param keyMapper the key mapper
     * @return the nullable
     */
    @SuppressWarnings("rawtypes")
    public static <T> Nullable<T> maxBy(final T[] a, final Function<? super T, ? extends Comparable> keyMapper) {
        return max(a, Fn.comparingBy(keyMapper));
    }

    /**
     * Median.
     *
     * @param <T> the generic type
     * @param c the c
     * @return the nullable
     */
    public static <T extends Comparable<? super T>> Nullable<T> median(final Collection<? extends T> c) {
        return N.isNullOrEmpty(c) ? Nullable.<T> empty() : Nullable.of(N.median(c));
    }

    /**
     * Median.
     *
     * @param <T> the generic type
     * @param a the a
     * @return the nullable
     */
    public static <T extends Comparable<? super T>> Nullable<T> median(final T[] a) {
        return N.isNullOrEmpty(a) ? Nullable.<T> empty() : Nullable.of(N.median(a));
    }

    /**
     * Median.
     *
     * @param <T> the generic type
     * @param c the c
     * @param cmp the cmp
     * @return the nullable
     */
    public static <T> Nullable<T> median(final Collection<? extends T> c, final Comparator<? super T> cmp) {
        return N.isNullOrEmpty(c) ? Nullable.<T> empty() : Nullable.of(N.median(c, cmp));
    }

    /**
     * Median.
     *
     * @param <T> the generic type
     * @param a the a
     * @param cmp the cmp
     * @return the nullable
     */
    public static <T> Nullable<T> median(final T[] a, final Comparator<? super T> cmp) {
        return N.isNullOrEmpty(a) ? Nullable.<T> empty() : Nullable.of(N.median(a, cmp));
    }

    /**
     * Median by.
     *
     * @param <T> the generic type
     * @param c the c
     * @param keyMapper the key mapper
     * @return the nullable
     */
    @SuppressWarnings("rawtypes")
    public static <T> Nullable<T> medianBy(final Collection<? extends T> c, final Function<? super T, ? extends Comparable> keyMapper) {
        return median(c, Fn.comparingBy(keyMapper));
    }

    /**
     * Median by.
     *
     * @param <T> the generic type
     * @param a the a
     * @param keyMapper the key mapper
     * @return the nullable
     */
    @SuppressWarnings("rawtypes")
    public static <T> Nullable<T> medianBy(final T[] a, final Function<? super T, ? extends Comparable> keyMapper) {
        return median(a, Fn.comparingBy(keyMapper));
    }

    /**
     * Kth largest.
     *
     * @param <T> the generic type
     * @param c the c
     * @param k the k
     * @return the nullable
     */
    public static <T extends Comparable<? super T>> Nullable<T> kthLargest(final Collection<? extends T> c, final int k) {
        return N.isNullOrEmpty(c) ? Nullable.<T> empty() : Nullable.of(N.kthLargest(c, k));
    }

    /**
     * Kth largest.
     *
     * @param <T> the generic type
     * @param a the a
     * @param k the k
     * @return the nullable
     */
    public static <T extends Comparable<? super T>> Nullable<T> kthLargest(final T[] a, final int k) {
        return N.isNullOrEmpty(a) ? Nullable.<T> empty() : Nullable.of(N.kthLargest(a, k));
    }

    /**
     * Kth largest.
     *
     * @param <T> the generic type
     * @param c the c
     * @param k the k
     * @param cmp the cmp
     * @return the nullable
     */
    public static <T> Nullable<T> kthLargest(final Collection<? extends T> c, final int k, final Comparator<? super T> cmp) {
        return N.isNullOrEmpty(c) ? Nullable.<T> empty() : Nullable.of(N.kthLargest(c, k, cmp));
    }

    /**
     * Kth largest.
     *
     * @param <T> the generic type
     * @param a the a
     * @param k the k
     * @param cmp the cmp
     * @return the nullable
     */
    public static <T> Nullable<T> kthLargest(final T[] a, final int k, final Comparator<? super T> cmp) {
        return N.isNullOrEmpty(a) ? Nullable.<T> empty() : Nullable.of(N.kthLargest(a, k, cmp));
    }

    /**
     * Index of.
     *
     * @param c the c
     * @param objToFind the obj to find
     * @return the optional int
     */
    public static OptionalInt indexOf(final Collection<?> c, final Object objToFind) {
        if (N.isNullOrEmpty(c)) {
            return OptionalInt.empty();
        }

        int idx = 0;

        for (Object e : c) {
            if (N.equals(e, objToFind)) {
                return OptionalInt.of(idx);
            }

            idx++;
        }

        return OptionalInt.empty();
    }

    /**
     * Index of.
     *
     * @param a the a
     * @param objToFind the obj to find
     * @return the optional int
     */
    public static OptionalInt indexOf(final Object[] a, final Object objToFind) {
        if (N.isNullOrEmpty(a)) {
            return OptionalInt.empty();
        }

        for (int i = 0, len = a.length; i < len; i++) {
            if (N.equals(a[i], objToFind)) {
                return OptionalInt.of(i);
            }
        }

        return OptionalInt.empty();
    }

    /**
     * Last index of.
     *
     * @param c the c
     * @param objToFind the obj to find
     * @return the optional int
     */
    public static OptionalInt lastIndexOf(final Collection<?> c, final Object objToFind) {
        if (N.isNullOrEmpty(c)) {
            return OptionalInt.empty();
        }

        final int size = c.size();

        if (c instanceof List) {
            final List<Object> list = (List<Object>) c;

            if (c instanceof RandomAccess) {
                for (int i = size - 1; i >= 0; i--) {
                    if (N.equals(list.get(i), objToFind)) {
                        return OptionalInt.of(i);
                    }
                }
            } else {
                final ListIterator<Object> iter = list.listIterator(list.size());

                for (int i = size - 1; iter.hasPrevious(); i--) {
                    if (N.equals(iter.previous(), objToFind)) {
                        return OptionalInt.of(i);
                    }
                }
            }

            return OptionalInt.empty();
        } else if (c instanceof Deque) {
            final Iterator<Object> iter = ((Deque<Object>) c).descendingIterator();

            for (int i = size - 1; iter.hasNext(); i--) {
                if (N.equals(iter.next(), objToFind)) {
                    return OptionalInt.of(i);
                }
            }

            return OptionalInt.empty();
        } else {
            final Object[] a = c.toArray();

            for (int i = a.length - 1; i >= 0; i--) {
                if (N.equals(a[i], objToFind)) {
                    return OptionalInt.of(i);
                }
            }

            return OptionalInt.empty();
        }
    }

    /**
     * Last index of.
     *
     * @param a the a
     * @param objToFind the obj to find
     * @return the optional int
     */
    public static OptionalInt lastIndexOf(final Object[] a, final Object objToFind) {
        if (N.isNullOrEmpty(a)) {
            return OptionalInt.empty();
        }

        for (int i = a.length - 1; i >= 0; i--) {
            if (N.equals(a[i], objToFind)) {
                return OptionalInt.of(i);
            }
        }

        return OptionalInt.empty();
    }

    /**
     * Find first index.
     *
     * @param <T> the generic type
     * @param <E> the element type
     * @param a the a
     * @param predicate the predicate
     * @return the optional int
     * @throws E the e
     * @deprecated replace by N.findFirstIndex
     * @see N#findFirstIndex(Object[], com.landawn.abacus.util.Throwables.Predicate)
     */
    @Deprecated
    public static <T, E extends Exception> OptionalInt findFirstIndex(final T[] a, final Throwables.Predicate<? super T, E> predicate) throws E {
        if (N.isNullOrEmpty(a)) {
            return OptionalInt.empty();
        }

        for (int len = a.length, i = 0; i < len; i++) {
            if (predicate.test(a[i])) {
                return OptionalInt.of(i);
            }
        }

        return OptionalInt.empty();
    }

    /**
     * Find first index.
     *
     * @param <T> the generic type
     * @param <E> the element type
     * @param c the c
     * @param predicate the predicate
     * @return the optional int
     * @throws E the e
     * @deprecated replace by N.findFirstIndex
     * @see N#findFirstIndex(Collection, com.landawn.abacus.util.Throwables.Predicate)
     */
    @Deprecated
    public static <T, E extends Exception> OptionalInt findFirstIndex(final Collection<? extends T> c, final Throwables.Predicate<? super T, E> predicate)
            throws E {
        if (N.isNullOrEmpty(c)) {
            return OptionalInt.empty();
        }

        int idx = 0;

        for (T e : c) {
            if (predicate.test(e)) {
                return OptionalInt.of(idx);
            }

            idx++;
        }

        return OptionalInt.empty();
    }

    /**
     * Find last index.
     *
     * @param <T> the generic type
     * @param <E> the element type
     * @param a the a
     * @param predicate the predicate
     * @return the optional int
     * @throws E the e
     * @deprecated replace by N.findLastIndex
     * @see N#findLastIndex(Object[], com.landawn.abacus.util.Throwables.Predicate)
     */
    @Deprecated
    public static <T, E extends Exception> OptionalInt findLastIndex(final T[] a, final Throwables.Predicate<? super T, E> predicate) throws E {
        if (N.isNullOrEmpty(a)) {
            return OptionalInt.empty();
        }

        for (int len = a.length, i = len - 1; i >= 0; i--) {
            if (predicate.test(a[i])) {
                return OptionalInt.of(i);
            }
        }

        return OptionalInt.empty();
    }

    /**
     * Find last index.
     *
     * @param <T> the generic type
     * @param <E> the element type
     * @param c the c
     * @param predicate the predicate
     * @return the optional int
     * @throws E the e
     * @deprecated replace by N.findLastIndex
     * @see N#findLastIndex(Collection, com.landawn.abacus.util.Throwables.Predicate)
     */
    @Deprecated
    public static <T, E extends Exception> OptionalInt findLastIndex(final Collection<? extends T> c, final Throwables.Predicate<? super T, E> predicate)
            throws E {
        if (N.isNullOrEmpty(c)) {
            return OptionalInt.empty();
        }

        final int size = c.size();

        if (c instanceof List) {
            final List<T> list = (List<T>) c;

            if (c instanceof RandomAccess) {
                for (int i = size - 1; i >= 0; i--) {
                    if (predicate.test(list.get(i))) {
                        return OptionalInt.of(i);
                    }
                }
            } else {
                final ListIterator<T> iter = list.listIterator(list.size());

                for (int i = size - 1; iter.hasPrevious(); i--) {
                    if (predicate.test(iter.previous())) {
                        return OptionalInt.of(i);
                    }
                }
            }

            return OptionalInt.empty();
        } else if (c instanceof Deque) {
            final Iterator<T> iter = ((Deque<T>) c).descendingIterator();

            for (int i = size - 1; iter.hasNext(); i--) {
                if (predicate.test(iter.next())) {
                    return OptionalInt.of(i);
                }
            }

            return OptionalInt.empty();
        } else {
            final T[] a = (T[]) c.toArray();

            for (int i = a.length - 1; i >= 0; i--) {
                if (predicate.test(a[i])) {
                    return OptionalInt.of(i);
                }
            }

            return OptionalInt.empty();
        }
    }

    /**
     * Find first or last index.
     *
     * @param <T> the generic type
     * @param <E> the element type
     * @param <E2> the generic type
     * @param c the c
     * @param predicateForFirst the predicate for first
     * @param predicateForLast the predicate for last
     * @return the optional int
     * @throws E the e
     * @throws E2 the e2
     */
    public static <T, E extends Exception, E2 extends Exception> OptionalInt findFirstOrLastIndex(final Collection<? extends T> c,
            final Throwables.Predicate<? super T, E> predicateForFirst, final Throwables.Predicate<? super T, E2> predicateForLast) throws E, E2 {
        if (N.isNullOrEmpty(c)) {
            return OptionalInt.empty();
        }

        final OptionalInt res = findFirstIndex(c, predicateForFirst);

        return res.isPresent() ? res : findLastIndex(c, predicateForLast);
    }

    /**
     * Find first or last index.
     *
     * @param <T> the generic type
     * @param <E> the element type
     * @param <E2> the generic type
     * @param a the a
     * @param predicateForFirst the predicate for first
     * @param predicateForLast the predicate for last
     * @return the optional int
     * @throws E the e
     * @throws E2 the e2
     */
    public static <T, E extends Exception, E2 extends Exception> OptionalInt findFirstOrLastIndex(final T[] a,
            final Throwables.Predicate<? super T, E> predicateForFirst, final Throwables.Predicate<? super T, E2> predicateForLast) throws E, E2 {
        if (N.isNullOrEmpty(a)) {
            return OptionalInt.empty();
        }

        final OptionalInt res = findFirstIndex(a, predicateForFirst);

        return res.isPresent() ? res : findLastIndex(a, predicateForLast);
    }

    /**
     * Find first and last index.
     *
     * @param <T> the generic type
     * @param <E> the element type
     * @param c the c
     * @param predicate the predicate
     * @return the pair
     * @throws E the e
     */
    public static <T, E extends Exception> Pair<OptionalInt, OptionalInt> findFirstAndLastIndex(final Collection<? extends T> c,
            final Throwables.Predicate<? super T, E> predicate) throws E {
        return findFirstAndLastIndex(c, predicate, predicate);
    }

    /**
     * Find first and last index.
     *
     * @param <T> the generic type
     * @param <E> the element type
     * @param <E2> the generic type
     * @param c the c
     * @param predicateForFirst the predicate for first
     * @param predicateForLast the predicate for last
     * @return the pair
     * @throws E the e
     * @throws E2 the e2
     */
    public static <T, E extends Exception, E2 extends Exception> Pair<OptionalInt, OptionalInt> findFirstAndLastIndex(final Collection<? extends T> c,
            final Throwables.Predicate<? super T, E> predicateForFirst, final Throwables.Predicate<? super T, E2> predicateForLast) throws E, E2 {
        if (N.isNullOrEmpty(c)) {
            return Pair.of(OptionalInt.empty(), OptionalInt.empty());
        }

        return Pair.of(findFirstIndex(c, predicateForFirst), findLastIndex(c, predicateForLast));
    }

    /**
     * Find first and last index.
     *
     * @param <T> the generic type
     * @param <E> the element type
     * @param a the a
     * @param predicate the predicate
     * @return the pair
     * @throws E the e
     */
    public static <T, E extends Exception> Pair<OptionalInt, OptionalInt> findFirstAndLastIndex(final T[] a, final Throwables.Predicate<? super T, E> predicate)
            throws E {
        return findFirstAndLastIndex(a, predicate, predicate);
    }

    /**
     * Find first and last index.
     *
     * @param <T> the generic type
     * @param <E> the element type
     * @param <E2> the generic type
     * @param a the a
     * @param predicateForFirst the predicate for first
     * @param predicateForLast the predicate for last
     * @return the pair
     * @throws E the e
     * @throws E2 the e2
     */
    public static <T, E extends Exception, E2 extends Exception> Pair<OptionalInt, OptionalInt> findFirstAndLastIndex(final T[] a,
            final Throwables.Predicate<? super T, E> predicateForFirst, final Throwables.Predicate<? super T, E2> predicateForLast) throws E, E2 {
        if (N.isNullOrEmpty(a)) {
            return Pair.of(OptionalInt.empty(), OptionalInt.empty());
        }

        return Pair.of(findFirstIndex(a, predicateForFirst), findLastIndex(a, predicateForLast));
    }

    /**
     * Find first.
     *
     * @param <T> the generic type
     * @param <E> the element type
     * @param a the a
     * @param predicate the predicate
     * @return the nullable
     * @throws E the e
     * @deprecated replace by N.findFirst
     * @see N#findFirst(Object[], com.landawn.abacus.util.Throwables.Predicate)
     */
    @Deprecated
    public static <T, E extends Exception> Nullable<T> findFirst(final T[] a, final Throwables.Predicate<? super T, E> predicate) throws E {
        if (N.isNullOrEmpty(a)) {
            return Nullable.empty();
        }

        for (int len = a.length, i = 0; i < len; i++) {
            if (predicate.test(a[i])) {
                return Nullable.of(a[i]);
            }
        }

        return Nullable.empty();
    }

    /**
     * Find first.
     *
     * @param <T> the generic type
     * @param <E> the element type
     * @param c the c
     * @param predicate the predicate
     * @return the nullable
     * @throws E the e
     * @deprecated replace by N.findFirst
     * @see N#findFirst(Collection, com.landawn.abacus.util.Throwables.Predicate)
     */
    @Deprecated
    public static <T, E extends Exception> Nullable<T> findFirst(final Collection<? extends T> c, Throwables.Predicate<? super T, E> predicate) throws E {
        if (N.isNullOrEmpty(c)) {
            return Nullable.empty();
        }

        for (T e : c) {
            if (predicate.test(e)) {
                return Nullable.of(e);
            }
        }

        return Nullable.empty();
    }

    /**
     * Find last.
     *
     * @param <T> the generic type
     * @param <E> the element type
     * @param a the a
     * @param predicate the predicate
     * @return the nullable
     * @throws E the e
     * @deprecated replace by N.findLast
     * @see N#findLast(Object[], com.landawn.abacus.util.Throwables.Predicate)
     */
    @Deprecated
    public static <T, E extends Exception> Nullable<T> findLast(final T[] a, final Throwables.Predicate<? super T, E> predicate) throws E {
        if (N.isNullOrEmpty(a)) {
            return Nullable.empty();
        }

        for (int len = a.length, i = len - 1; i >= 0; i--) {
            if (predicate.test(a[i])) {
                return Nullable.of(a[i]);
            }
        }

        return Nullable.empty();
    }

    /**
     * Find last.
     *
     * @param <T> the generic type
     * @param <E> the element type
     * @param c the c
     * @param predicate the predicate
     * @return the nullable
     * @throws E the e
     * @deprecated replace by N.findLast
     * @see N#findLast(Collection, com.landawn.abacus.util.Throwables.Predicate)
     */
    @Deprecated
    public static <T, E extends Exception> Nullable<T> findLast(final Collection<? extends T> c, Throwables.Predicate<? super T, E> predicate) throws E {
        return findLast(c, predicate, false);
    }

    /**
     * Find first non null.
     *
     * @param <T> the generic type
     * @param <E> the element type
     * @param a the a
     * @param predicate the predicate
     * @return the optional
     * @throws E the e
     * @deprecated replace by N.findFirstNonNull
     * @see N#findFirstNonNull(Object[], com.landawn.abacus.util.Throwables.Predicate)
     */
    @Deprecated
    public static <T, E extends Exception> Optional<T> findFirstNonNull(final T[] a, final Throwables.Predicate<? super T, E> predicate) throws E {
        if (N.isNullOrEmpty(a)) {
            return Optional.empty();
        }

        for (int len = a.length, i = 0; i < len; i++) {
            if (a[i] != null && predicate.test(a[i])) {
                return Optional.of(a[i]);
            }
        }

        return Optional.empty();
    }

    /**
     * Find first non null.
     *
     * @param <T> the generic type
     * @param <E> the element type
     * @param c the c
     * @param predicate the predicate
     * @return the optional
     * @throws E the e
     * @deprecated replace by N.findFirstNonNull
     * @see N#findFirstNonNull(Collection, com.landawn.abacus.util.Throwables.Predicate)
     */
    @Deprecated
    public static <T, E extends Exception> Optional<T> findFirstNonNull(final Collection<? extends T> c, Throwables.Predicate<? super T, E> predicate)
            throws E {
        if (N.isNullOrEmpty(c)) {
            return Optional.empty();
        }

        for (T e : c) {
            if (e != null && predicate.test(e)) {
                return Optional.of(e);
            }
        }

        return Optional.empty();
    }

    /**
     * Find last non null.
     *
     * @param <T> the generic type
     * @param <E> the element type
     * @param a the a
     * @param predicate the predicate
     * @return the optional
     * @throws E the e
     * @deprecated replace by N.findLastNonNull
     * @see N#findLastNonNull(Object[], com.landawn.abacus.util.Throwables.Predicate)
     */
    @Deprecated
    public static <T, E extends Exception> Optional<T> findLastNonNull(final T[] a, final Throwables.Predicate<? super T, E> predicate) throws E {
        if (N.isNullOrEmpty(a)) {
            return Optional.empty();
        }

        for (int len = a.length, i = len - 1; i >= 0; i--) {
            if (a[i] != null && predicate.test(a[i])) {
                return Optional.of(a[i]);
            }
        }

        return Optional.empty();
    }

    /**
     * Find last non null.
     *
     * @param <T> the generic type
     * @param <E> the element type
     * @param c the c
     * @param predicate the predicate
     * @return the optional
     * @throws E the e
     * @deprecated replace by N.findLastNonNull
     * @see N#findLastNonNull(Collection, com.landawn.abacus.util.Throwables.Predicate)
     */
    @Deprecated
    public static <T, E extends Exception> Optional<T> findLastNonNull(final Collection<? extends T> c, Throwables.Predicate<? super T, E> predicate) throws E {
        return findLast(c, predicate, true);
    }

    /**
     * Find last.
     *
     * @param <T> the generic type
     * @param <R> the generic type
     * @param <E> the element type
     * @param c the c
     * @param predicate the predicate
     * @param isForNonNull the is for non null
     * @return the r
     * @throws E the e
     */
    private static <T, R, E extends Exception> R findLast(final Collection<? extends T> c, Throwables.Predicate<? super T, E> predicate, boolean isForNonNull)
            throws E {
        if (N.isNullOrEmpty(c)) {
            return (R) (isForNonNull ? Optional.empty() : Nullable.empty());
        }

        T e = null;

        if (c instanceof List) {
            final List<T> list = (List<T>) c;

            if (c instanceof RandomAccess) {
                for (int i = c.size() - 1; i >= 0; i--) {
                    e = list.get(i);

                    if ((!isForNonNull || e != null) && predicate.test(e)) {
                        return (R) (isForNonNull ? Optional.of(e) : Nullable.of(e));
                    }
                }
            } else {
                final ListIterator<T> iter = list.listIterator(list.size());

                while (iter.hasPrevious()) {
                    e = iter.previous();

                    if ((!isForNonNull || e != null) && predicate.test(e)) {
                        return (R) (isForNonNull ? Optional.of(e) : Nullable.of(e));
                    }
                }
            }

            return (R) (isForNonNull ? Optional.empty() : Nullable.empty());
        } else if (c instanceof Deque) {
            final Iterator<T> iter = ((Deque<T>) c).descendingIterator();

            while (iter.hasNext()) {
                e = iter.next();

                if ((!isForNonNull || e != null) && predicate.test(e)) {
                    return (R) (isForNonNull ? Optional.of(e) : Nullable.of(e));
                }
            }

            return (R) (isForNonNull ? Optional.empty() : Nullable.empty());
        } else {
            final T[] a = (T[]) c.toArray();

            for (int i = a.length - 1; i >= 0; i--) {
                if ((!isForNonNull || a[i] != null) && predicate.test(a[i])) {
                    return (R) (isForNonNull ? Optional.of(a[i]) : Nullable.of(a[i]));
                }
            }

            return (R) (isForNonNull ? Optional.empty() : Nullable.empty());
        }
    }

    /**
     * Find first or last.
     *
     * @param <T> the generic type
     * @param <E> the element type
     * @param <E2> the generic type
     * @param c the c
     * @param predicateForFirst the predicate for first
     * @param predicateForLast the predicate for last
     * @return the nullable
     * @throws E the e
     * @throws E2 the e2
     */
    public static <T, E extends Exception, E2 extends Exception> Nullable<T> findFirstOrLast(final Collection<? extends T> c,
            final Throwables.Predicate<? super T, E> predicateForFirst, final Throwables.Predicate<? super T, E2> predicateForLast) throws E, E2 {
        if (N.isNullOrEmpty(c)) {
            return Nullable.<T> empty();
        }

        final Nullable<T> res = findFirst(c, predicateForFirst);

        return res.isPresent() ? res : findLast(c, predicateForLast);
    }

    /**
     * Find first or last.
     *
     * @param <T> the generic type
     * @param <E> the element type
     * @param <E2> the generic type
     * @param a the a
     * @param predicateForFirst the predicate for first
     * @param predicateForLast the predicate for last
     * @return the nullable
     * @throws E the e
     * @throws E2 the e2
     */
    public static <T, E extends Exception, E2 extends Exception> Nullable<T> findFirstOrLast(final T[] a,
            final Throwables.Predicate<? super T, E> predicateForFirst, final Throwables.Predicate<? super T, E2> predicateForLast) throws E, E2 {
        if (N.isNullOrEmpty(a)) {
            return Nullable.<T> empty();
        }

        final Nullable<T> res = findFirst(a, predicateForFirst);

        return res.isPresent() ? res : findLast(a, predicateForLast);
    }

    /**
     * Find first and last.
     *
     * @param <T> the generic type
     * @param <E> the element type
     * @param c the c
     * @param predicate the predicate
     * @return the pair
     * @throws E the e
     */
    public static <T, E extends Exception> Pair<Nullable<T>, Nullable<T>> findFirstAndLast(final Collection<? extends T> c,
            final Throwables.Predicate<? super T, E> predicate) throws E {
        return findFirstAndLast(c, predicate, predicate);
    }

    /**
     * Find first and last.
     *
     * @param <T> the generic type
     * @param <E> the element type
     * @param <E2> the generic type
     * @param c the c
     * @param predicateForFirst the predicate for first
     * @param predicateForLast the predicate for last
     * @return the pair
     * @throws E the e
     * @throws E2 the e2
     */
    public static <T, E extends Exception, E2 extends Exception> Pair<Nullable<T>, Nullable<T>> findFirstAndLast(final Collection<? extends T> c,
            final Throwables.Predicate<? super T, E> predicateForFirst, final Throwables.Predicate<? super T, E2> predicateForLast) throws E, E2 {
        if (N.isNullOrEmpty(c)) {
            return Pair.of(Nullable.<T> empty(), Nullable.<T> empty());
        }

        return Pair.of(findFirst(c, predicateForFirst), findLast(c, predicateForLast));
    }

    /**
     * Find first and last.
     *
     * @param <T> the generic type
     * @param <E> the element type
     * @param a the a
     * @param predicate the predicate
     * @return the pair
     * @throws E the e
     */
    public static <T, E extends Exception> Pair<Nullable<T>, Nullable<T>> findFirstAndLast(final T[] a, final Throwables.Predicate<? super T, E> predicate)
            throws E {
        return findFirstAndLast(a, predicate, predicate);
    }

    /**
     * Find first and last.
     *
     * @param <T> the generic type
     * @param <E> the element type
     * @param <E2> the generic type
     * @param a the a
     * @param predicateForFirst the predicate for first
     * @param predicateForLast the predicate for last
     * @return the pair
     * @throws E the e
     * @throws E2 the e2
     */
    public static <T, E extends Exception, E2 extends Exception> Pair<Nullable<T>, Nullable<T>> findFirstAndLast(final T[] a,
            final Throwables.Predicate<? super T, E> predicateForFirst, final Throwables.Predicate<? super T, E2> predicateForLast) throws E, E2 {
        if (N.isNullOrEmpty(a)) {
            return Pair.of(Nullable.<T> empty(), Nullable.<T> empty());
        }

        return Pair.of(findFirst(a, predicateForFirst), findLast(a, predicateForLast));
    }

    /**
     * All match.
     *
     * @param <T> the generic type
     * @param <E> the element type
     * @param c the c
     * @param filter the filter
     * @return true, if successful
     * @throws E the e
     * @deprecated replaced by {@code N#allMatch(Collection, com.landawn.abacus.util.Try.Predicate)}
     */
    @Deprecated
    public static <T, E extends Exception> boolean allMatch(final Collection<? extends T> c, final Throwables.Predicate<? super T, E> filter) throws E {
        if (N.isNullOrEmpty(c)) {
            return true;
        }

        for (T e : c) {
            if (filter.test(e) == false) {
                return false;
            }
        }

        return true;
    }

    /**
     * All match.
     *
     * @param <T> the generic type
     * @param <E> the element type
     * @param a the a
     * @param filter the filter
     * @return true, if successful
     * @throws E the e
     * @deprecated replaced by {@code N#allMatch(Object[], com.landawn.abacus.util.Try.Predicate)}
     */
    @Deprecated
    public static <T, E extends Exception> boolean allMatch(final T[] a, final Throwables.Predicate<? super T, E> filter) throws E {
        if (N.isNullOrEmpty(a)) {
            return true;
        }

        for (T e : a) {
            if (filter.test(e) == false) {
                return false;
            }
        }

        return true;
    }

    /**
     * Any match.
     *
     * @param <T> the generic type
     * @param <E> the element type
     * @param c the c
     * @param filter the filter
     * @return true, if successful
     * @throws E the e
     * @deprecated replaced by {@code N#anyMatch(Collection, com.landawn.abacus.util.Try.Predicate)}
     */
    @Deprecated
    public static <T, E extends Exception> boolean anyMatch(final Collection<? extends T> c, final Throwables.Predicate<? super T, E> filter) throws E {
        if (N.isNullOrEmpty(c)) {
            return false;
        }

        for (T e : c) {
            if (filter.test(e)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Any match.
     *
     * @param <T> the generic type
     * @param <E> the element type
     * @param a the a
     * @param filter the filter
     * @return true, if successful
     * @throws E the e
     * @deprecated replaced by {@code N#anyMatch(Object[], com.landawn.abacus.util.Try.Predicate)}
     */
    @Deprecated
    public static <T, E extends Exception> boolean anyMatch(final T[] a, final Throwables.Predicate<? super T, E> filter) throws E {
        if (N.isNullOrEmpty(a)) {
            return false;
        }

        for (T e : a) {
            if (filter.test(e)) {
                return true;
            }
        }

        return false;
    }

    /**
     * None match.
     *
     * @param <T> the generic type
     * @param <E> the element type
     * @param c the c
     * @param filter the filter
     * @return true, if successful
     * @throws E the e
     * @deprecated replaced by {@code N#noneMatch(Collection, com.landawn.abacus.util.Try.Predicate)}
     */
    @Deprecated
    public static <T, E extends Exception> boolean noneMatch(final Collection<? extends T> c, final Throwables.Predicate<? super T, E> filter) throws E {
        if (N.isNullOrEmpty(c)) {
            return true;
        }

        for (T e : c) {
            if (filter.test(e)) {
                return false;
            }
        }

        return true;
    }

    /**
     * None match.
     *
     * @param <T> the generic type
     * @param <E> the element type
     * @param a the a
     * @param filter the filter
     * @return true, if successful
     * @throws E the e
     * @deprecated replaced by {@code N#noneMatch(Object[], com.landawn.abacus.util.Try.Predicate)}
     */
    @Deprecated
    public static <T, E extends Exception> boolean noneMatch(final T[] a, final Throwables.Predicate<? super T, E> filter) throws E {
        if (N.isNullOrEmpty(a)) {
            return true;
        }

        for (T e : a) {
            if (filter.test(e)) {
                return false;
            }
        }

        return true;
    }

    /**
     * N match.
     *
     * @param <T> the generic type
     * @param <E> the element type
     * @param c the c
     * @param atLeast the at least
     * @param atMost the at most
     * @param filter the filter
     * @return true, if successful
     * @throws E the e
     * @deprecated replaced by {@code N#nMatch(Collection, int, int, com.landawn.abacus.util.Try.Predicate)}
     */
    @Deprecated
    public static <T, E extends Exception> boolean nMatch(final Collection<? extends T> c, final int atLeast, final int atMost,
            final Throwables.Predicate<? super T, E> filter) throws E {
        N.checkArgNotNegative(atLeast, "atLeast");
        N.checkArgNotNegative(atMost, "atMost");
        N.checkArgument(atLeast <= atMost, "'atLeast' must be <= 'atMost'");

        long cnt = 0;

        for (T e : c) {
            if (filter.test(e)) {
                if (++cnt > atMost) {
                    return false;
                }
            }
        }

        return cnt >= atLeast && cnt <= atMost;
    }

    /**
     * N match.
     *
     * @param <T> the generic type
     * @param <E> the element type
     * @param a the a
     * @param atLeast the at least
     * @param atMost the at most
     * @param filter the filter
     * @return true, if successful
     * @throws E the e
     * @deprecated replaced by {@code N#nMatch(Object[], int, int, com.landawn.abacus.util.Try.Predicate)}
     */
    @Deprecated
    public static <T, E extends Exception> boolean nMatch(final T[] a, final int atLeast, final int atMost, final Throwables.Predicate<? super T, E> filter)
            throws E {
        N.checkArgNotNegative(atLeast, "atLeast");
        N.checkArgNotNegative(atMost, "atMost");
        N.checkArgument(atLeast <= atMost, "'atLeast' must be <= 'atMost'");

        long cnt = 0;

        for (T e : a) {
            if (filter.test(e)) {
                if (++cnt > atMost) {
                    return false;
                }
            }
        }

        return cnt >= atLeast && cnt <= atMost;
    }

    /**
     * Take while.
     *
     * @param <T> the generic type
     * @param <E> the element type
     * @param c the c
     * @param filter the filter
     * @return the list
     * @throws E the e
     */
    public static <T, E extends Exception> List<T> takeWhile(final Collection<? extends T> c, final Throwables.Predicate<? super T, E> filter) throws E {
        N.checkArgNotNull(filter);

        final List<T> result = new ArrayList<>(N.min(9, N.size(c)));

        if (N.isNullOrEmpty(c)) {
            return result;
        }

        for (T e : c) {
            if (filter.test(e)) {
                result.add(e);
            } else {
                break;
            }
        }

        return result;
    }

    /**
     * Take while.
     *
     * @param <T> the generic type
     * @param <E> the element type
     * @param a the a
     * @param filter the filter
     * @return the list
     * @throws E the e
     */
    public static <T, E extends Exception> List<T> takeWhile(final T[] a, final Throwables.Predicate<? super T, E> filter) throws E {
        N.checkArgNotNull(filter);

        final List<T> result = new ArrayList<>(N.min(9, N.len(a)));

        if (N.isNullOrEmpty(a)) {
            return result;
        }

        for (T e : a) {
            if (filter.test(e)) {
                result.add(e);
            } else {
                break;
            }
        }

        return result;
    }

    /**
     * Take while inclusive.
     *
     * @param <T> the generic type
     * @param <E> the element type
     * @param c the c
     * @param filter the filter
     * @return the list
     * @throws E the e
     */
    public static <T, E extends Exception> List<T> takeWhileInclusive(final Collection<? extends T> c, final Throwables.Predicate<? super T, E> filter)
            throws E {
        N.checkArgNotNull(filter);

        final List<T> result = new ArrayList<>(N.min(9, N.size(c)));

        if (N.isNullOrEmpty(c)) {
            return result;
        }

        for (T e : c) {
            result.add(e);

            if (filter.test(e) == false) {
                break;
            }
        }

        return result;
    }

    /**
     * Take while inclusive.
     *
     * @param <T> the generic type
     * @param <E> the element type
     * @param a the a
     * @param filter the filter
     * @return the list
     * @throws E the e
     */
    public static <T, E extends Exception> List<T> takeWhileInclusive(final T[] a, final Throwables.Predicate<? super T, E> filter) throws E {
        N.checkArgNotNull(filter);

        final List<T> result = new ArrayList<>(N.min(9, N.len(a)));

        if (N.isNullOrEmpty(a)) {
            return result;
        }

        for (T e : a) {
            result.add(e);

            if (filter.test(e) == false) {
                break;
            }
        }

        return result;
    }

    /**
     * Drop while.
     *
     * @param <T> the generic type
     * @param <E> the element type
     * @param c the c
     * @param filter the filter
     * @return the list
     * @throws E the e
     */
    public static <T, E extends Exception> List<T> dropWhile(final Collection<? extends T> c, final Throwables.Predicate<? super T, E> filter) throws E {
        N.checkArgNotNull(filter);

        final List<T> result = new ArrayList<>(N.min(9, N.size(c)));

        if (N.isNullOrEmpty(c)) {
            return result;
        }

        final Iterator<? extends T> iter = c.iterator();
        T e = null;

        while (iter.hasNext()) {
            e = iter.next();

            if (filter.test(e) == false) {
                result.add(e);
                break;
            }
        }

        while (iter.hasNext()) {
            result.add(iter.next());
        }

        return result;
    }

    /**
     * Drop while.
     *
     * @param <T> the generic type
     * @param <E> the element type
     * @param a the a
     * @param filter the filter
     * @return the list
     * @throws E the e
     */
    public static <T, E extends Exception> List<T> dropWhile(final T[] a, final Throwables.Predicate<? super T, E> filter) throws E {
        N.checkArgNotNull(filter);

        final List<T> result = new ArrayList<>(N.min(9, N.len(a)));

        if (N.isNullOrEmpty(a)) {
            return result;
        }

        final int len = a.length;
        int idx = 0;

        while (idx < len && filter.test(a[idx]) == true) {
            idx++;
        }

        while (idx < len) {
            result.add(a[idx++]);
        }

        return result;
    }

    /**
     * Skip until.
     *
     * @param <T> the generic type
     * @param <E> the element type
     * @param c the c
     * @param filter the filter
     * @return the list
     * @throws E the e
     */
    public static <T, E extends Exception> List<T> skipUntil(final Collection<? extends T> c, final Throwables.Predicate<? super T, E> filter) throws E {
        N.checkArgNotNull(filter);

        final List<T> result = new ArrayList<>(N.min(9, N.size(c)));

        if (N.isNullOrEmpty(c)) {
            return result;
        }

        final Iterator<? extends T> iter = c.iterator();
        T e = null;

        while (iter.hasNext()) {
            e = iter.next();

            if (filter.test(e)) {
                result.add(e);
                break;
            }
        }

        while (iter.hasNext()) {
            result.add(iter.next());
        }

        return result;
    }

    /**
     * Skip until.
     *
     * @param <T> the generic type
     * @param <E> the element type
     * @param a the a
     * @param filter the filter
     * @return the list
     * @throws E the e
     */
    public static <T, E extends Exception> List<T> skipUntil(final T[] a, final Throwables.Predicate<? super T, E> filter) throws E {
        N.checkArgNotNull(filter);

        final List<T> result = new ArrayList<>(N.min(9, N.len(a)));

        if (N.isNullOrEmpty(a)) {
            return result;
        }

        final int len = a.length;
        int idx = 0;

        while (idx < len && filter.test(a[idx]) == false) {
            idx++;
        }

        while (idx < len) {
            result.add(a[idx++]);
        }

        return result;
    }

    /**
     * For each pair.
     *
     * @param <T> the generic type
     * @param <E> the element type
     * @param c the c
     * @param action the action
     * @throws E the e
     */
    public static <T, E extends Exception> void forEachPair(final Collection<? extends T> c, final Throwables.BiConsumer<? super T, ? super T, E> action)
            throws E {
        forEachPair(c, action, 1);
    }

    /**
     * For each pair.
     *
     * @param <T> the generic type
     * @param <E> the element type
     * @param c the c
     * @param action the action
     * @param increment the increment
     * @throws E the e
     */
    public static <T, E extends Exception> void forEachPair(final Collection<? extends T> c, final Throwables.BiConsumer<? super T, ? super T, E> action,
            final int increment) throws E {
        N.checkArgNotNull(action);
        final int windowSize = 2;
        N.checkArgument(windowSize > 0 && increment > 0, "windowSize=%s and increment=%s must be bigger than 0", windowSize, increment);

        if (N.isNullOrEmpty(c)) {
            return;
        }

        final Iterator<? extends T> iter = c.iterator();
        Iterators.forEachPair(iter, action, increment);
    }

    /**
     * For each pair.
     *
     * @param <T> the generic type
     * @param <E> the element type
     * @param a the a
     * @param action the action
     * @throws E the e
     */
    public static <T, E extends Exception> void forEachPair(final T[] a, final Throwables.BiConsumer<? super T, ? super T, E> action) throws E {
        forEachPair(a, action, 1);
    }

    /**
     * For each pair.
     *
     * @param <T> the generic type
     * @param <E> the element type
     * @param a the a
     * @param action the action
     * @param increment the increment
     * @throws E the e
     */
    public static <T, E extends Exception> void forEachPair(final T[] a, final Throwables.BiConsumer<? super T, ? super T, E> action, final int increment)
            throws E {
        N.checkArgNotNull(action);
        final int windowSize = 2;
        N.checkArgument(windowSize > 0 && increment > 0, "windowSize=%s and increment=%s must be bigger than 0", windowSize, increment);

        if (N.isNullOrEmpty(a)) {
            return;
        }

        final Iterator<? extends T> iter = ObjIterator.of(a);
        Iterators.forEachPair(iter, action, increment);
    }

    /**
     * For each triple.
     *
     * @param <T> the generic type
     * @param <E> the element type
     * @param c the c
     * @param action the action
     * @throws E the e
     */
    public static <T, E extends Exception> void forEachTriple(final Collection<? extends T> c,
            final Throwables.TriConsumer<? super T, ? super T, ? super T, E> action) throws E {
        forEachTriple(c, action, 1);
    }

    /**
     * For each triple.
     *
     * @param <T> the generic type
     * @param <E> the element type
     * @param c the c
     * @param action the action
     * @param increment the increment
     * @throws E the e
     */
    public static <T, E extends Exception> void forEachTriple(final Collection<? extends T> c,
            final Throwables.TriConsumer<? super T, ? super T, ? super T, E> action, final int increment) throws E {
        N.checkArgNotNull(action);
        final int windowSize = 3;
        N.checkArgument(windowSize > 0 && increment > 0, "windowSize=%s and increment=%s must be bigger than 0", windowSize, increment);

        if (N.isNullOrEmpty(c)) {
            return;
        }

        final Iterator<? extends T> iter = c.iterator();
        Iterators.forEachTriple(iter, action, increment);
    }

    /**
     * For each triple.
     *
     * @param <T> the generic type
     * @param <E> the element type
     * @param a the a
     * @param action the action
     * @throws E the e
     */
    public static <T, E extends Exception> void forEachTriple(final T[] a, final Throwables.TriConsumer<? super T, ? super T, ? super T, E> action) throws E {
        forEachTriple(a, action, 1);
    }

    /**
     * For each triple.
     *
     * @param <T> the generic type
     * @param <E> the element type
     * @param a the a
     * @param action the action
     * @param increment the increment
     * @throws E the e
     */
    public static <T, E extends Exception> void forEachTriple(final T[] a, final Throwables.TriConsumer<? super T, ? super T, ? super T, E> action,
            final int increment) throws E {
        N.checkArgNotNull(action);
        final int windowSize = 3;
        N.checkArgument(windowSize > 0 && increment > 0, "windowSize=%s and increment=%s must be bigger than 0", windowSize, increment);

        if (N.isNullOrEmpty(a)) {
            return;
        }

        final Iterator<? extends T> iter = ObjIterator.of(a);
        Iterators.forEachTriple(iter, action, increment);
    }

    //    /**
    //     *
    //     * @param <T>
    //     * @param <U>
    //     * @param a
    //     * @param b
    //     * @return
    //     * @see N#crossJoin(Collection, Collection)
    //     * @deprecated replaced by {@code N.crossJoin(Collection, Collection)}
    //     */
    //    @Deprecated
    //    public static <T, U> List<Pair<T, U>> crossJoin(final Collection<T> a, final Collection<U> b) {
    //        return crossJoin(a, b, Fn.<T, U> pair());
    //    }
    //
    //    /**
    //     *
    //     * @param <T>
    //     * @param <U>
    //     * @param <R>
    //     * @param <E>
    //     * @param a
    //     * @param b
    //     * @param func
    //     * @return
    //     * @throws E
    //     * @see N#crossJoin(Collection, Collection, com.landawn.abacus.util.Throwables.BiFunction)
    //     * @deprecated replaced by {@code N.crossJoin(Collection, Collection, com.landawn.abacus.util.Try.BiFunction)}
    //     */
    //    @Deprecated
    //    public static <T, U, R, E extends Exception> List<R> crossJoin(final Collection<T> a, final Collection<U> b,
    //            final Throwables.BiFunction<? super T, ? super U, R, E> func) throws E {
    //        N.checkArgNotNull(func, "func");
    //
    //        final List<R> result = new ArrayList<>(N.size(a) * N.size(b));
    //
    //        if (N.isNullOrEmpty(a) || N.isNullOrEmpty(b)) {
    //            return result;
    //        }
    //
    //        for (T ae : a) {
    //            for (U be : b) {
    //                result.add(func.apply(ae, be));
    //            }
    //        }
    //
    //        return result;
    //    }
    //
    //    /**
    //     * The time complexity is <i>O(n + m)</i> : <i>n</i> is the size of this <code>Seq</code> and <i>m</i> is the size of specified collection <code>b</code>.
    //     *
    //     * @param <T> the generic type
    //     * @param <U> the generic type
    //     * @param <E> the element type
    //     * @param <E2> the generic type
    //     * @param a the a
    //     * @param b the b
    //     * @param leftKeyMapper the left key mapper
    //     * @param rightKeyMapper the right key mapper
    //     * @return the list
    //     * @throws E the e
    //     * @throws E2 the e2
    //     * @see N#innerJoin(Collection, Collection, com.landawn.abacus.util.Throwables.Function, com.landawn.abacus.util.Throwables.Function)
    //     * @see <a href="http://stackoverflow.com/questions/5706437/whats-the-difference-between-inner-join-left-join-right-join-and-full-join">sql join</a>
    //     * @deprecated replaced by {@code N.innerJoin(Collection, Collection, com.landawn.abacus.util.Try.Function, com.landawn.abacus.util.Try.Function)}
    //     */
    //    @Deprecated
    //    public static <T, U, K, E extends Exception, E2 extends Exception> List<Pair<T, U>> innerJoin(final Collection<T> a, final Collection<U> b,
    //            final Throwables.Function<? super T, ? extends K, E> leftKeyMapper, final Throwables.Function<? super U, ? extends K, E2> rightKeyMapper)
    //            throws E, E2 {
    //        final List<Pair<T, U>> result = new ArrayList<>(N.min(9, N.size(a), N.size(b)));
    //
    //        if (N.isNullOrEmpty(a) || N.isNullOrEmpty(b)) {
    //            return result;
    //        }
    //
    //        final ListMultimap<K, U> rightKeyMap = ListMultimap.from(b, rightKeyMapper);
    //
    //        for (T left : a) {
    //            final List<U> rights = rightKeyMap.get(leftKeyMapper.apply(left));
    //
    //            if (N.notNullOrEmpty(rights)) {
    //                for (U right : rights) {
    //                    result.add(Pair.of(left, right));
    //                }
    //            }
    //        }
    //
    //        return result;
    //    }
    //
    //    /**
    //     * The time complexity is <i>O(n * m)</i> : <i>n</i> is the size of this <code>Seq</code> and <i>m</i> is the size of specified collection <code>b</code>.
    //     *
    //     * @param <T> the generic type
    //     * @param <U> the generic type
    //     * @param <E> the element type
    //     * @param a the a
    //     * @param b the b
    //     * @param predicate the predicate
    //     * @return the list
    //     * @throws E the e
    //     * @see N#innerJoin(Collection, Collection, com.landawn.abacus.util.Throwables.BiPredicate)
    //     * @see <a href="http://stackoverflow.com/questions/5706437/whats-the-difference-between-inner-join-left-join-right-join-and-full-join">sql join</a>
    //     * @deprecated replaced by {@code N.innerJoin(Collection, Collection, com.landawn.abacus.util.Try.BiPredicate)}
    //     */
    //    @Deprecated
    //    public static <T, U, E extends Exception> List<Pair<T, U>> innerJoin(final Collection<T> a, final Collection<U> b,
    //            final Throwables.BiPredicate<? super T, ? super U, E> predicate) throws E {
    //        final List<Pair<T, U>> result = new ArrayList<>(N.min(9, N.size(a), N.size(b)));
    //
    //        if (N.isNullOrEmpty(a) || N.isNullOrEmpty(b)) {
    //            return result;
    //        }
    //
    //        for (T left : a) {
    //            for (U right : b) {
    //                if (predicate.test(left, right)) {
    //                    result.add(Pair.of(left, right));
    //                }
    //            }
    //        }
    //
    //        return result;
    //    }
    //
    //    /**
    //     * The time complexity is <i>O(n + m)</i> : <i>n</i> is the size of this <code>Seq</code> and <i>m</i> is the size of specified collection <code>b</code>.
    //     *
    //     * @param <T> the generic type
    //     * @param <U> the generic type
    //     * @param <E> the element type
    //     * @param <E2> the generic type
    //     * @param a the a
    //     * @param b the b
    //     * @param leftKeyMapper the left key mapper
    //     * @param rightKeyMapper the right key mapper
    //     * @return the list
    //     * @throws E the e
    //     * @throws E2 the e2
    //     * @see N#fullJoin(Collection, Collection, com.landawn.abacus.util.Throwables.Function, com.landawn.abacus.util.Throwables.Function)
    //     * @see <a href="http://stackoverflow.com/questions/5706437/whats-the-difference-between-inner-join-left-join-right-join-and-full-join">sql join</a>
    //     * @deprecated replaced by {@code N.fullJoin(Collection, Collection, com.landawn.abacus.util.Try.Function, com.landawn.abacus.util.Try.Function)}
    //     */
    //    @Deprecated
    //    public static <T, U, K, E extends Exception, E2 extends Exception> List<Pair<T, U>> fullJoin(final Collection<T> a, final Collection<U> b,
    //            final Throwables.Function<? super T, ? extends K, E> leftKeyMapper, final Throwables.Function<? super U, ? extends K, E2> rightKeyMapper)
    //            throws E, E2 {
    //        final List<Pair<T, U>> result = new ArrayList<>(N.max(9, N.size(a), N.size(b)));
    //
    //        if (N.isNullOrEmpty(a)) {
    //            for (T left : a) {
    //                result.add(Pair.of(left, (U) null));
    //            }
    //        } else if (N.isNullOrEmpty(b)) {
    //            for (U right : b) {
    //                result.add(Pair.of((T) null, right));
    //            }
    //        } else {
    //            final ListMultimap<K, U> rightKeyMap = ListMultimap.from(b, rightKeyMapper);
    //            final Map<U, U> joinedRights = new IdentityHashMap<>();
    //
    //            for (T left : a) {
    //                final List<U> rights = rightKeyMap.get(leftKeyMapper.apply(left));
    //
    //                if (N.notNullOrEmpty(rights)) {
    //                    for (U right : rights) {
    //                        result.add(Pair.of(left, right));
    //                        joinedRights.put(right, right);
    //                    }
    //                } else {
    //                    result.add(Pair.of(left, (U) null));
    //                }
    //            }
    //
    //            for (U right : b) {
    //                if (joinedRights.containsKey(right) == false) {
    //                    result.add(Pair.of((T) null, right));
    //                }
    //            }
    //        }
    //
    //        return result;
    //    }
    //
    //    /**
    //     * The time complexity is <i>O(n * m)</i> : <i>n</i> is the size of this <code>Seq</code> and <i>m</i> is the size of specified collection <code>b</code>.
    //     *
    //     * @param <T> the generic type
    //     * @param <U> the generic type
    //     * @param <E> the element type
    //     * @param a the a
    //     * @param b the b
    //     * @param predicate the predicate
    //     * @return the list
    //     * @throws E the e
    //     * @see N#fullJoin(Collection, Collection, com.landawn.abacus.util.Throwables.BiPredicate)
    //     * @see <a href="http://stackoverflow.com/questions/5706437/whats-the-difference-between-inner-join-left-join-right-join-and-full-join">sql join</a>
    //     * @deprecated replaced by {@code N.fullJoin(Collection, Collection, com.landawn.abacus.util.Try.BiPredicate)}
    //     */
    //    @Deprecated
    //    public static <T, U, E extends Exception> List<Pair<T, U>> fullJoin(final Collection<T> a, final Collection<U> b,
    //            final Throwables.BiPredicate<? super T, ? super U, E> predicate) throws E {
    //        final List<Pair<T, U>> result = new ArrayList<>(N.max(9, N.size(a), N.size(b)));
    //
    //        if (N.isNullOrEmpty(a)) {
    //            for (T left : a) {
    //                result.add(Pair.of(left, (U) null));
    //            }
    //        } else if (N.isNullOrEmpty(b)) {
    //            for (U right : b) {
    //                result.add(Pair.of((T) null, right));
    //            }
    //        } else {
    //            final Map<U, U> joinedRights = new IdentityHashMap<>();
    //
    //            for (T left : a) {
    //                boolean joined = false;
    //
    //                for (U right : b) {
    //                    if (predicate.test(left, right)) {
    //                        result.add(Pair.of(left, right));
    //                        joinedRights.put(right, right);
    //                        joined = true;
    //                    }
    //                }
    //
    //                if (joined == false) {
    //                    result.add(Pair.of(left, (U) null));
    //                }
    //            }
    //
    //            for (U right : b) {
    //                if (joinedRights.containsKey(right) == false) {
    //                    result.add(Pair.of((T) null, right));
    //                }
    //            }
    //        }
    //
    //        return result;
    //    }
    //
    //    /**
    //     * The time complexity is <i>O(n + m)</i> : <i>n</i> is the size of this <code>Seq</code> and <i>m</i> is the size of specified collection <code>b</code>.
    //     *
    //     * @param <T> the generic type
    //     * @param <U> the generic type
    //     * @param <E> the element type
    //     * @param <E2> the generic type
    //     * @param a the a
    //     * @param b the b
    //     * @param leftKeyMapper the left key mapper
    //     * @param rightKeyMapper the right key mapper
    //     * @return the list
    //     * @throws E the e
    //     * @throws E2 the e2
    //     * @see N#leftJoin(Collection, Collection, com.landawn.abacus.util.Throwables.Function, com.landawn.abacus.util.Throwables.Function)
    //     * @see <a href="http://stackoverflow.com/questions/5706437/whats-the-difference-between-inner-join-left-join-right-join-and-full-join">sql join</a>
    //     * @deprecated replaced by {@code N.leftJoin(Collection, Collection, com.landawn.abacus.util.Try.Function, com.landawn.abacus.util.Try.Function)}
    //     */
    //    @Deprecated
    //    public static <T, U, K, E extends Exception, E2 extends Exception> List<Pair<T, U>> leftJoin(final Collection<T> a, final Collection<U> b,
    //            final Throwables.Function<? super T, ? extends K, E> leftKeyMapper, final Throwables.Function<? super U, ? extends K, E2> rightKeyMapper)
    //            throws E, E2 {
    //        final List<Pair<T, U>> result = new ArrayList<>(N.size(a));
    //
    //        if (N.isNullOrEmpty(a)) {
    //            return result;
    //        } else if (N.isNullOrEmpty(b)) {
    //            for (T left : a) {
    //                result.add(Pair.of(left, (U) null));
    //            }
    //        } else {
    //            final ListMultimap<K, U> rightKeyMap = ListMultimap.from(b, rightKeyMapper);
    //
    //            for (T left : a) {
    //                final List<U> rights = rightKeyMap.get(leftKeyMapper.apply(left));
    //
    //                if (N.notNullOrEmpty(rights)) {
    //                    for (U right : rights) {
    //                        result.add(Pair.of(left, right));
    //                    }
    //                } else {
    //                    result.add(Pair.of(left, (U) null));
    //                }
    //            }
    //        }
    //
    //        return result;
    //    }
    //
    //    /**
    //     * The time complexity is <i>O(n * m)</i> : <i>n</i> is the size of this <code>Seq</code> and <i>m</i> is the size of specified collection <code>b</code>.
    //     *
    //     * @param <T> the generic type
    //     * @param <U> the generic type
    //     * @param <E> the element type
    //     * @param a the a
    //     * @param b the b
    //     * @param predicate the predicate
    //     * @return the list
    //     * @throws E the e
    //     * @see N#leftJoin(Collection, Collection, com.landawn.abacus.util.Throwables.BiPredicate)
    //     * @see <a href="http://stackoverflow.com/questions/5706437/whats-the-difference-between-inner-join-left-join-right-join-and-full-join">sql join</a>
    //     * @deprecated replaced by {@code N.leftJoin(Collection, Collection, com.landawn.abacus.util.Try.BiPredicate)}
    //     */
    //    @Deprecated
    //    public static <T, U, E extends Exception> List<Pair<T, U>> leftJoin(final Collection<T> a, final Collection<U> b,
    //            final Throwables.BiPredicate<? super T, ? super U, E> predicate) throws E {
    //        final List<Pair<T, U>> result = new ArrayList<>(N.size(a));
    //
    //        if (N.isNullOrEmpty(a)) {
    //            return result;
    //        } else if (N.isNullOrEmpty(b)) {
    //            for (T left : a) {
    //                result.add(Pair.of(left, (U) null));
    //            }
    //        } else {
    //            for (T left : a) {
    //                boolean joined = false;
    //
    //                for (U right : b) {
    //                    if (predicate.test(left, right)) {
    //                        result.add(Pair.of(left, right));
    //                        joined = true;
    //                    }
    //                }
    //
    //                if (joined == false) {
    //                    result.add(Pair.of(left, (U) null));
    //                }
    //            }
    //        }
    //
    //        return result;
    //    }
    //
    //    /**
    //     * The time complexity is <i>O(n + m)</i> : <i>n</i> is the size of this <code>Seq</code> and <i>m</i> is the size of specified collection <code>b</code>.
    //     *
    //     * @param <T> the generic type
    //     * @param <U> the generic type
    //     * @param <E> the element type
    //     * @param <E2> the generic type
    //     * @param a the a
    //     * @param b the b
    //     * @param leftKeyMapper the left key mapper
    //     * @param rightKeyMapper the right key mapper
    //     * @return the list
    //     * @throws E the e
    //     * @throws E2 the e2
    //     * @see N#rightJoin(Collection, Collection, com.landawn.abacus.util.Throwables.Function, com.landawn.abacus.util.Throwables.Function)
    //     * @see <a href="http://stackoverflow.com/questions/5706437/whats-the-difference-between-inner-join-left-join-right-join-and-full-join">sql join</a>
    //     * @deprecated replaced by {@code N.rightJoin(Collection, Collection, com.landawn.abacus.util.Try.Function, com.landawn.abacus.util.Try.Function)}
    //     */
    //    @Deprecated
    //    public static <T, U, K, E extends Exception, E2 extends Exception> List<Pair<T, U>> rightJoin(final Collection<T> a, final Collection<U> b,
    //            final Throwables.Function<? super T, ? extends K, E> leftKeyMapper, final Throwables.Function<? super U, ? extends K, E2> rightKeyMapper)
    //            throws E, E2 {
    //        final List<Pair<T, U>> result = new ArrayList<>(N.size(b));
    //
    //        if (N.isNullOrEmpty(b)) {
    //            return result;
    //        } else if (N.isNullOrEmpty(a)) {
    //            for (U right : b) {
    //                result.add(Pair.of((T) null, right));
    //            }
    //        } else {
    //            final ListMultimap<K, T> leftKeyMap = ListMultimap.from(a, leftKeyMapper);
    //
    //            for (U right : b) {
    //                final List<T> lefts = leftKeyMap.get(rightKeyMapper.apply(right));
    //
    //                if (N.notNullOrEmpty(lefts)) {
    //                    for (T left : lefts) {
    //                        result.add(Pair.of(left, right));
    //                    }
    //                } else {
    //                    result.add(Pair.of((T) null, right));
    //                }
    //            }
    //        }
    //
    //        return result;
    //    }
    //
    //    /**
    //     * The time complexity is <i>O(n * m)</i> : <i>n</i> is the size of this <code>Seq</code> and <i>m</i> is the size of specified collection <code>b</code>.
    //     *
    //     * @param <T> the generic type
    //     * @param <U> the generic type
    //     * @param <E> the element type
    //     * @param a the a
    //     * @param b the b
    //     * @param predicate the predicate
    //     * @return the list
    //     * @throws E the e
    //     * @see N#rightJoin(Collection, Collection, com.landawn.abacus.util.Throwables.BiPredicate)
    //     * @see <a href="http://stackoverflow.com/questions/5706437/whats-the-difference-between-inner-join-left-join-right-join-and-full-join">sql join</a>
    //     * @deprecated replaced by {@code N.rightJoin(Collection, Collection, com.landawn.abacus.util.Try.BiPredicate)}
    //     */
    //    @Deprecated
    //    public static <T, U, E extends Exception> List<Pair<T, U>> rightJoin(final Collection<T> a, final Collection<U> b,
    //            final Throwables.BiPredicate<? super T, ? super U, E> predicate) throws E {
    //        final List<Pair<T, U>> result = new ArrayList<>(N.size(b));
    //
    //        if (N.isNullOrEmpty(b)) {
    //            return result;
    //        } else if (N.isNullOrEmpty(a)) {
    //            for (U right : b) {
    //                result.add(Pair.of((T) null, right));
    //            }
    //        } else {
    //            for (U right : b) {
    //                boolean joined = false;
    //
    //                for (T left : a) {
    //                    if (predicate.test(left, right)) {
    //                        result.add(Pair.of(left, right));
    //                        joined = true;
    //                    }
    //                }
    //
    //                if (joined == false) {
    //                    result.add(Pair.of((T) null, right));
    //                }
    //            }
    //        }
    //
    //        return result;
    //    }

    /**
     * Parses the.
     *
     * @param <T> the generic type
     * @param <E> the element type
     * @param iter the iter
     * @param elementParser the element parser
     * @throws E the e
     */
    public static <T, E extends Exception> void forEach(final Iterator<? extends T> iter, final Throwables.Consumer<? super T, E> elementParser) throws E {
        forEach(iter, elementParser, Fn.emptyAction());
    }

    /**
     * Parses the.
     *
     * @param <T> the generic type
     * @param <E> the element type
     * @param <E2> the generic type
     * @param iter the iter
     * @param elementParser the element parser
     * @param onComplete the on complete
     * @throws E the e
     * @throws E2 the e2
     */
    public static <T, E extends Exception, E2 extends Exception> void forEach(final Iterator<? extends T> iter,
            final Throwables.Consumer<? super T, E> elementParser, final Throwables.Runnable<E2> onComplete) throws E, E2 {
        forEach(iter, 0, Long.MAX_VALUE, elementParser, onComplete);
    }

    /**
     * Parses the.
     *
     * @param <T> the generic type
     * @param <E> the element type
     * @param iter the iter
     * @param offset the offset
     * @param count the count
     * @param elementParser the element parser
     * @throws E the e
     */
    public static <T, E extends Exception> void forEach(final Iterator<? extends T> iter, final long offset, final long count,
            final Throwables.Consumer<? super T, E> elementParser) throws E {
        forEach(iter, offset, count, elementParser, Fn.emptyAction());
    }

    /**
     * Parses the.
     *
     * @param <T> the generic type
     * @param <E> the element type
     * @param <E2> the generic type
     * @param iter the iter
     * @param offset the offset
     * @param count the count
     * @param elementParser the element parser
     * @param onComplete the on complete
     * @throws E the e
     * @throws E2 the e2
     */
    public static <T, E extends Exception, E2 extends Exception> void forEach(final Iterator<? extends T> iter, final long offset, final long count,
            final Throwables.Consumer<? super T, E> elementParser, final Throwables.Runnable<E2> onComplete) throws E, E2 {
        forEach(iter, offset, count, 0, 0, elementParser, onComplete);
    }

    /**
     * Parses the.
     *
     * @param <T> the generic type
     * @param <E> the element type
     * @param iter the iter
     * @param offset the offset
     * @param count the count
     * @param processThreadNum the process thread num
     * @param queueSize the queue size
     * @param elementParser the element parser
     * @throws E the e
     */
    public static <T, E extends Exception> void forEach(final Iterator<? extends T> iter, long offset, long count, final int processThreadNum,
            final int queueSize, final Throwables.Consumer<? super T, E> elementParser) throws E {
        forEach(iter, offset, count, processThreadNum, queueSize, elementParser, Fn.emptyAction());
    }

    /**
     * Parse the elements in the specified iterators one by one.
     *
     * @param <T> the generic type
     * @param <E> the element type
     * @param <E2> the generic type
     * @param iter the iter
     * @param offset the offset
     * @param count the count
     * @param processThreadNum new threads started to parse/process the lines/records
     * @param queueSize size of queue to save the processing records/lines loaded from source data. Default size is 1024.
     * @param elementParser the element parser
     * @param onComplete the on complete
     * @throws E the e
     * @throws E2 the e2
     */
    public static <T, E extends Exception, E2 extends Exception> void forEach(final Iterator<? extends T> iter, long offset, long count,
            final int processThreadNum, final int queueSize, final Throwables.Consumer<? super T, E> elementParser, final Throwables.Runnable<E2> onComplete)
            throws E, E2 {
        forEach(Array.asList(iter), offset, count, 0, processThreadNum, queueSize, elementParser, onComplete);
    }

    /**
     * Parses the.
     *
     * @param <T> the generic type
     * @param <E> the element type
     * @param iterators the iterators
     * @param elementParser the element parser
     * @throws E the e
     */
    public static <T, E extends Exception> void forEach(final Collection<? extends Iterator<? extends T>> iterators,
            final Throwables.Consumer<? super T, E> elementParser) throws E {
        forEach(iterators, elementParser, Fn.emptyAction());
    }

    /**
     * Parses the.
     *
     * @param <T> the generic type
     * @param <E> the element type
     * @param <E2> the generic type
     * @param iterators the iterators
     * @param elementParser the element parser
     * @param onComplete the on complete
     * @throws E the e
     * @throws E2 the e2
     */
    public static <T, E extends Exception, E2 extends Exception> void forEach(final Collection<? extends Iterator<? extends T>> iterators,
            final Throwables.Consumer<? super T, E> elementParser, final Throwables.Runnable<E2> onComplete) throws E, E2 {
        forEach(iterators, 0, Long.MAX_VALUE, elementParser, onComplete);
    }

    /**
     * Parses the.
     *
     * @param <T> the generic type
     * @param <E> the element type
     * @param iterators the iterators
     * @param offset the offset
     * @param count the count
     * @param elementParser the element parser
     * @throws E the e
     */
    public static <T, E extends Exception> void forEach(final Collection<? extends Iterator<? extends T>> iterators, final long offset, final long count,
            final Throwables.Consumer<? super T, E> elementParser) throws E {
        forEach(iterators, offset, count, elementParser, Fn.emptyAction());
    }

    /**
     * Parses the.
     *
     * @param <T> the generic type
     * @param <E> the element type
     * @param <E2> the generic type
     * @param iterators the iterators
     * @param offset the offset
     * @param count the count
     * @param elementParser the element parser
     * @param onComplete the on complete
     * @throws E the e
     * @throws E2 the e2
     */
    public static <T, E extends Exception, E2 extends Exception> void forEach(final Collection<? extends Iterator<? extends T>> iterators, final long offset,
            final long count, final Throwables.Consumer<? super T, E> elementParser, final Throwables.Runnable<E2> onComplete) throws E, E2 {
        forEach(iterators, offset, count, 0, 0, 0, elementParser, onComplete);
    }

    /**
     * Parses the.
     *
     * @param <T> the generic type
     * @param <E> the element type
     * @param iterators the iterators
     * @param readThreadNum the read thread num
     * @param processThreadNum the process thread num
     * @param queueSize the queue size
     * @param elementParser the element parser
     * @throws E the e
     */
    public static <T, E extends Exception> void forEach(final Collection<? extends Iterator<? extends T>> iterators, final int readThreadNum,
            final int processThreadNum, final int queueSize, final Throwables.Consumer<? super T, E> elementParser) throws E {
        forEach(iterators, readThreadNum, processThreadNum, queueSize, elementParser, Fn.emptyAction());
    }

    /**
     * Parses the.
     *
     * @param <T> the generic type
     * @param <E> the element type
     * @param <E2> the generic type
     * @param iterators the iterators
     * @param readThreadNum the read thread num
     * @param processThreadNum the process thread num
     * @param queueSize the queue size
     * @param elementParser the element parser
     * @param onComplete the on complete
     * @throws E the e
     */
    public static <T, E extends Exception, E2 extends Exception> void forEach(final Collection<? extends Iterator<? extends T>> iterators,
            final int readThreadNum, final int processThreadNum, final int queueSize, final Throwables.Consumer<? super T, E> elementParser,
            final Throwables.Runnable<E2> onComplete) throws E {
        forEach(iterators, 0, Long.MAX_VALUE, readThreadNum, processThreadNum, queueSize, elementParser);
    }

    /**
     * Parses the.
     *
     * @param <T> the generic type
     * @param <E> the element type
     * @param iterators the iterators
     * @param offset the offset
     * @param count the count
     * @param readThreadNum the read thread num
     * @param processThreadNum the process thread num
     * @param queueSize the queue size
     * @param elementParser the element parser
     * @throws E the e
     */
    public static <T, E extends Exception> void forEach(final Collection<? extends Iterator<? extends T>> iterators, final long offset, final long count,
            final int readThreadNum, final int processThreadNum, final int queueSize, final Throwables.Consumer<? super T, E> elementParser) throws E {
        forEach(iterators, offset, count, readThreadNum, processThreadNum, queueSize, elementParser, Fn.emptyAction());
    }

    /**
     * Parse the elements in the specified iterators one by one.
     *
     * @param <T> the generic type
     * @param <E> the element type
     * @param <E2> the generic type
     * @param iterators the iterators
     * @param offset the offset
     * @param count the count
     * @param readThreadNum new threads started to parse/process the lines/records
     * @param processThreadNum new threads started to parse/process the lines/records
     * @param queueSize size of queue to save the processing records/lines loaded from source data. Default size is 1024.
     * @param elementParser the element parser
     * @param onComplete the on complete
     * @throws E the e
     * @throws E2 the e2
     */
    public static <T, E extends Exception, E2 extends Exception> void forEach(final Collection<? extends Iterator<? extends T>> iterators, final long offset,
            final long count, final int readThreadNum, final int processThreadNum, final int queueSize, final Throwables.Consumer<? super T, E> elementParser,
            final Throwables.Runnable<E2> onComplete) throws E, E2 {
        N.checkArgument(offset >= 0 && count >= 0, "'offset'=%s and 'count'=%s can not be negative", offset, count);

        if (N.isNullOrEmpty(iterators)) {
            return;
        }

        if (logger.isInfoEnabled()) {
            logger.info("### Start to parse");
        }

        try (final Stream<T> stream = ((readThreadNum > 0 || queueSize > 0)
                ? Stream.parallelConcatt(iterators, (readThreadNum == 0 ? 1 : readThreadNum), (queueSize == 0 ? 1024 : queueSize))
                : Stream.concatt(iterators))) {

            final Iterator<? extends T> iteratorII = stream.skip(offset).limit(count).iterator();

            if (processThreadNum == 0) {
                while (iteratorII.hasNext()) {
                    elementParser.accept(iteratorII.next());
                }

                if (onComplete != null) {
                    onComplete.run();
                }
            } else {
                final AtomicInteger activeThreadNum = new AtomicInteger();
                final ExecutorService executorService = Executors.newFixedThreadPool(processThreadNum);
                final Holder<Throwable> errorHolder = new Holder<>();

                for (int i = 0; i < processThreadNum; i++) {
                    activeThreadNum.incrementAndGet();

                    executorService.execute(new Runnable() {
                        @Override
                        public void run() {
                            T element = null;
                            try {
                                while (errorHolder.value() == null) {
                                    synchronized (iteratorII) {
                                        if (iteratorII.hasNext()) {
                                            element = iteratorII.next();
                                        } else {
                                            break;
                                        }
                                    }

                                    elementParser.accept(element);
                                }
                            } catch (Exception e) {
                                synchronized (errorHolder) {
                                    if (errorHolder.value() == null) {
                                        errorHolder.setValue(e);
                                    } else {
                                        errorHolder.value().addSuppressed(e);
                                    }
                                }
                            } finally {
                                activeThreadNum.decrementAndGet();
                            }
                        }
                    });
                }

                while (activeThreadNum.get() > 0) {
                    N.sleep(1);
                }

                if (errorHolder.value() == null && onComplete != null) {
                    try {
                        onComplete.run();
                    } catch (Exception e) {
                        errorHolder.setValue(e);
                    }
                }

                if (errorHolder.value() != null) {
                    throw N.toRuntimeException(errorHolder.value());
                }
            }
        } finally {
            if (logger.isInfoEnabled()) {
                logger.info("### End to parse");
            }
        }
    }

    /**
     * Rollup.
     *
     * @param <T> the generic type
     * @param c the c
     * @return the list
     */
    public static <T> List<List<T>> rollup(final Collection<? extends T> c) {
        final List<List<T>> res = new ArrayList<>();
        res.add(new ArrayList<T>());

        if (N.notNullOrEmpty(c)) {
            for (T e : c) {
                final List<T> prev = res.get(res.size() - 1);
                List<T> cur = new ArrayList<>(prev.size() + 1);
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
     * @param <E> the element type
     * @param set the set of elements to construct a power set from
     * @return the sets the
     * @throws IllegalArgumentException if {@code set} has more than 30 unique
     *     elements (causing the power set size to exceed the {@code int} range)
     * @throws NullPointerException if {@code set} is or contains {@code null}
     * @see <a href="http://en.wikipedia.org/wiki/Power_set">Power set article at
     *      Wikipedia</a>
     */
    public static <E> Set<Set<E>> powerSet(Set<E> set) {
        return new PowerSet<>(set);
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
     * @param <E> the element type
     * @param elements the original collection whose elements have to be permuted.
     * @return an immutable {@link Collection} containing all the different
     *     permutations of the original collection.
     * @throws NullPointerException if the specified collection is null or has any
     *     null elements.
     */
    public static <E> Collection<List<E>> permutations(final Collection<E> elements) {
        return new PermutationCollection<>(elements);
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
     * @param <E> the element type
     * @param elements the original iterable whose elements have to be permuted.
     * @return an immutable {@link Collection} containing all the different
     *     permutations of the original iterable.
     * @throws NullPointerException if the specified iterable is null or has any
     *     null elements.
     */
    public static <E extends Comparable<? super E>> Collection<List<E>> orderedPermutations(final Collection<E> elements) {
        return orderedPermutations(elements, Comparators.naturalOrder());
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
     * @param <E> the element type
     * @param elements the original iterable whose elements have to be permuted.
     * @param comparator a comparator for the iterable's elements.
     * @return an immutable {@link Collection} containing all the different
     *     permutations of the original iterable.
     * @throws NullPointerException If the specified iterable is null, has any
     *     null elements, or if the specified comparator is null.
     */
    public static <E> Collection<List<E>> orderedPermutations(final Collection<E> elements, Comparator<? super E> comparator) {
        return new OrderedPermutationCollection<>(elements, comparator);
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
        return cartesianProduct(Arrays.asList(cs));
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
     * @param a the a
     * @param b the b
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
     * @param <E> the element type
     */
    private static final class PowerSet<E> extends AbstractSet<Set<E>> {

        /** The input set. */
        final ImmutableMap<E, Integer> inputSet;

        /**
         * Instantiates a new power set.
         *
         * @param input the input
         */
        PowerSet(Set<E> input) {
            this.inputSet = indexMap(input);
            N.checkArgument(inputSet.size() <= 30, "Too many elements to create power set: %s > 30", inputSet.size());
        }

        /**
         * Size.
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
         * Iterator.
         *
         * @return the iterator
         */
        @Override
        public Iterator<Set<E>> iterator() {
            return new Iterator<Set<E>>() {
                private final int size = size();
                private int position;

                @Override
                public boolean hasNext() {
                    return position < size;
                }

                @Override
                public Set<E> next() {
                    if (!hasNext()) {
                        throw new NoSuchElementException();
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
         * Contains.
         *
         * @param obj the obj
         * @return true, if successful
         */
        @Override
        public boolean contains(Object obj) {
            if (obj instanceof Set) {
                Set<?> set = (Set<?>) obj;
                return inputSet.keySet().containsAll(set);
            }
            return false;
        }

        /**
         * Equals.
         *
         * @param obj the obj
         * @return true, if successful
         */
        @Override
        public boolean equals(Object obj) {
            if (obj instanceof PowerSet) {
                PowerSet<?> that = (PowerSet<?>) obj;
                return inputSet.equals(that.inputSet);
            }
            return super.equals(obj);
        }

        /**
         * Hash code.
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
         * To string.
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
         * @param <E> the element type
         * @param c the c
         * @return the immutable map
         */
        private static <E> ImmutableMap<E, Integer> indexMap(final Collection<E> c) {
            final Map<E, Integer> map = new LinkedHashMap<>();

            int i = 0;

            for (E e : c) {
                map.put(e, i++);
            }

            return ImmutableMap.of(map);
        }
    }

    /**
     * The Class SubSet.
     *
     * @param <E> the element type
     */
    private static final class SubSet<E> extends AbstractSet<E> {

        /** The input set. */
        private final ImmutableMap<E, Integer> inputSet;

        /** The elements. */
        private final ImmutableList<E> elements;

        /** The mask. */
        private final int mask;

        /**
         * Instantiates a new sub set.
         *
         * @param inputSet the input set
         * @param mask the mask
         */
        SubSet(ImmutableMap<E, Integer> inputSet, int mask) {
            this.inputSet = inputSet;
            this.elements = ImmutableList.of((E[]) inputSet.keySet().toArray());
            this.mask = mask;
        }

        /**
         * Iterator.
         *
         * @return the iterator
         */
        @Override
        public Iterator<E> iterator() {
            return new Iterator<E>() {
                int remainingSetBits = mask;

                @Override
                public boolean hasNext() {
                    return remainingSetBits != 0;
                }

                @Override
                public E next() {
                    int index = Integer.numberOfTrailingZeros(remainingSetBits);
                    if (index == 32) {
                        throw new NoSuchElementException();
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
         * Size.
         *
         * @return the int
         */
        @Override
        public int size() {
            return Integer.bitCount(mask);
        }

        /**
         * Contains.
         *
         * @param o the o
         * @return true, if successful
         */
        @Override
        public boolean contains(Object o) {
            Integer index = inputSet.get(o);
            return index != null && (mask & (1 << index)) != 0;
        }
    }

    /**
     * The Class PermutationCollection.
     *
     * @param <E> the element type
     */
    private static final class PermutationCollection<E> extends AbstractCollection<List<E>> {

        /** The input list. */
        final List<E> inputList;

        /**
         * Instantiates a new permutation collection.
         *
         * @param input the input
         */
        PermutationCollection(final Collection<E> input) {
            this.inputList = new ArrayList<>(input);
        }

        /**
         * Size.
         *
         * @return the int
         */
        @Override
        public int size() {
            return Matth.factorial(inputList.size());
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
         * Iterator.
         *
         * @return the iterator
         */
        @Override
        public Iterator<List<E>> iterator() {
            return PermutationIterator.of(inputList);
        }

        /**
         * Contains.
         *
         * @param obj the obj
         * @return true, if successful
         */
        @Override
        public boolean contains(Object obj) {
            if (obj instanceof Collection) {
                return isPermutations(inputList, (Collection<?>) obj);
            }

            return false;
        }

        /**
         * To string.
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
     * @param <E> the element type
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
         * @param input the input
         * @param comparator the comparator
         */
        OrderedPermutationCollection(final Collection<E> input, Comparator<? super E> comparator) {
            this.inputList = new ArrayList<>(input);
            N.sort(inputList, comparator);
            this.comparator = comparator;
            this.size = calculateSize(inputList, comparator);
        }

        /**
         * Size.
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
         * Iterator.
         *
         * @return the iterator
         */
        @Override
        public Iterator<List<E>> iterator() {
            return PermutationIterator.ordered(inputList, comparator);
        }

        /**
         * Contains.
         *
         * @param obj the obj
         * @return true, if successful
         */
        @Override
        public boolean contains(Object obj) {
            if (obj instanceof Collection) {
                return isPermutations(inputList, (Collection<?>) obj);
            }
            return false;
        }

        /**
         * To string.
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
         * @param <E> the element type
         * @param sortedInputList the sorted input list
         * @param comparator the comparator
         * @return the int
         */
        private static <E> int calculateSize(List<E> sortedInputList, Comparator<? super E> comparator) {
            long permutations = 1;
            int n = 1;
            int r = 1;
            while (n < sortedInputList.size()) {
                int comparison = comparator.compare(sortedInputList.get(n - 1), sortedInputList.get(n));

                if (comparison < 0) {
                    // We move to the next non-repeated element.
                    permutations *= Matth.binomial(n, r);
                    r = 0;
                    if (!isPositiveInt(permutations)) {
                        return Integer.MAX_VALUE;
                    }
                }

                n++;
                r++;
            }

            permutations *= Matth.binomial(n, r);

            if (!isPositiveInt(permutations)) {
                return Integer.MAX_VALUE;
            }

            return (int) permutations;
        }

        /**
         * Checks if is positive int.
         *
         * @param n the n
         * @return true, if is positive int
         */
        private static boolean isPositiveInt(long n) {
            return n >= 0 && n <= Integer.MAX_VALUE;
        }
    }

    /**
     * The Class CartesianList.
     *
     * @param <E> the element type
     */
    private static final class CartesianList<E> extends AbstractList<List<E>> implements RandomAccess {

        /** The axes. */
        private final transient Object[][] axes;

        /** The axes size product. */
        private final transient int[] axesSizeProduct;

        /**
         * Instantiates a new cartesian list.
         *
         * @param cs the cs
         */
        CartesianList(final Collection<? extends Collection<? extends E>> cs) {
            final Iterator<? extends Collection<? extends E>> iter = cs.iterator();
            this.axes = new Object[cs.size()][];

            for (int i = 0, len = this.axes.length; i < len; i++) {
                this.axes[i] = iter.next().toArray();
            }

            this.axesSizeProduct = new int[axes.length + 1];
            axesSizeProduct[axes.length] = 1;

            try {
                for (int i = axes.length - 1; i >= 0; i--) {
                    axesSizeProduct[i] = Matth.multiplyExact(axesSizeProduct[i + 1], axes[i].length);
                }
            } catch (ArithmeticException e) {
                throw new IllegalArgumentException("Cartesian product too large; must have size at most Integer.MAX_VALUE");
            }
        }

        /**
         * Gets the.
         *
         * @param index the index
         * @return the list
         */
        @Override
        public List<E> get(final int index) {
            N.checkArgument(index < size(), "Invalid index %s. It must be less than the size %s", index, size());

            final List<E> result = new ArrayList<>(axes.length);

            for (int k = 0, len = axes.length; k < len; k++) {
                result.add((E) axes[k][getAxisIndexForProductIndex(index, k)]);
            }

            return result;
        }

        /**
         * Size.
         *
         * @return the int
         */
        @Override
        public int size() {
            return axesSizeProduct[0];
        }

        /**
         * Contains.
         *
         * @param obj the obj
         * @return true, if successful
         */
        @Override
        public boolean contains(Object obj) {
            if (!(obj instanceof Collection)) {
                return false;
            }

            final Collection<?> c = (Collection<?>) obj;

            if (c.size() != axes.length) {
                return false;
            }

            int idx = 0;
            for (Object e : c) {
                boolean found = false;

                for (Object p : axes[idx++]) {
                    if (N.equals(e, p)) {
                        found = true;
                        break;
                    }
                }

                if (found == false) {
                    return false;
                }
            }

            return true;
        }

        /**
         * Gets the axis index for product index.
         *
         * @param index the index
         * @param axis the axis
         * @return the axis index for product index
         */
        private int getAxisIndexForProductIndex(int index, int axis) {
            return (index / axesSizeProduct[axis + 1]) % axes[axis].length;
        }
    }

    /**
     * The Class Slice.
     *
     * @param <T> the generic type
     */
    static final class Slice<T> extends ImmutableCollection<T> {

        /** The from index. */
        private final int fromIndex;

        /** The to index. */
        private final int toIndex;

        /**
         * Instantiates a new sub collection.
         *
         * @param a the a
         * @param fromIndex the from index
         * @param toIndex the to index
         */
        Slice(final T[] a, final int fromIndex, final int toIndex) {
            this(Array.asList(a), fromIndex, toIndex);
        }

        /**
         * Instantiates a new sub collection.
         *
         * @param c the c
         * @param fromIndex the from index
         * @param toIndex the to index
         */
        Slice(final Collection<? extends T> c, final int fromIndex, final int toIndex) {
            super(c);
            this.fromIndex = fromIndex;
            this.toIndex = toIndex;
        }

        /**
         * Contains.
         *
         * @param o the o
         * @return true, if successful
         */
        @Override
        public boolean contains(Object o) {
            final Iterator<T> iter = this.iterator();

            while (iter.hasNext()) {
                if (N.equals(iter.next(), o)) {
                    return true;
                }
            }

            return false;
        }

        /**
         * Contains all.
         *
         * @param c the c
         * @return true, if successful
         */
        @Override
        public boolean containsAll(final Collection<?> c) {
            for (Object e : c) {
                if (contains(e) == false) {
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
         * Size.
         *
         * @return the int
         */
        @Override
        public int size() {
            return toIndex - fromIndex;
        }

        /**
         * Iterator.
         *
         * @return the iterator
         */
        @Override
        public Iterator<T> iterator() {
            final Iterator<T> iter = coll == null ? ObjIterator.<T> empty() : coll.iterator();

            if (fromIndex > 0) {
                int offset = 0;

                while (offset++ < fromIndex) {
                    iter.next();
                }
            }

            return new Iterator<T>() {
                private int cursor = fromIndex;

                @Override
                public boolean hasNext() {
                    return cursor < toIndex;
                }

                @Override
                public T next() {
                    if (cursor >= toIndex) {
                        throw new NoSuchElementException();
                    }

                    cursor++;
                    return iter.next();
                }

                @Override
                public void remove() {
                    throw new UnsupportedOperationException();
                }
            };
        }

        /**
         * To array.
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
         * To array.
         *
         * @param <A> the generic type
         * @param a the a
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
}
